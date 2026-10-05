package pokemon.runtime.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.battle.WildEncounters;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.AnimationData;
import pokemon.runtime.data.MapData;
import pokemon.runtime.event.EventInterpreter;
import pokemon.runtime.event.MapPort;
import pokemon.runtime.event.MessageService;
import pokemon.runtime.event.MoveRoutePlayer;
import pokemon.runtime.event.ScreenEffects;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.state.GameState;
import pokemon.runtime.ui.MessageWindow;
import pokemon.runtime.ui.PictureLayer;
import pokemon.runtime.ui.menu.PauseMenuOverlay;
import pokemon.runtime.ui.menu.TitleScreen;

/**
 * The first playable screen (project3 section 69, walking demo): renders the
 * map from the Builder output, hosts the player character and runs the tile
 * movement. R5/R6 add switches, variables and the event interpreter.
 */
public final class MapScreen extends ScreenAdapter {

    /**
     * Logical viewport (R11): the game renders at this size and a FitViewport
     * scales it onto the window - any aspect ratio gets black bars instead of a
     * stretched map, and the UI keeps its pixel size relative to the game.
     */
    private final int viewWidth = ScreenMetrics.logicalWidth();
    private final int viewHeight = ScreenMetrics.logicalHeight();
    /** Fitted letterbox viewport; rebound to the real camera in the constructor. */
    private FitViewport viewport = new FitViewport(viewWidth, viewHeight);
    /** Character sheet of the playable hero (Graphics/Characters). */
    // PBS/metadata.txt [000] PlayerA: walking / running sheets for this project.
    public static final String PLAYER_CHARACTER = "trchar000";
    public static final String PLAYER_RUNNING_CHARACTER = "boy_run";
    /** Fallback message font when project.json carries no runtime profile. */
    public static final String DEFAULT_MESSAGE_FONT = "FusionPixelMonoPatched.ttf";
    /**
     * R6.24: the project's {@code autoplayAsCue} cues a map's BGM with
     * {@code pbCueBGM(bgm, 1.0)}; the old track fades for one second while the
     * new one starts at 60 %.
     */
    private static final float MAP_BGM_CUE_SECONDS = 1f;

    private final RuntimeContext context;
    private final GameState gameState;
    private final SpriteBatch batch = new SpriteBatch();
    private final TextureRepository textures = new TextureRepository();
    private final OrthographicCamera camera;

    /** R6.23: a map-connection crossing rebuilds these in place (no fade). */
    private GameDatabase database;
    private MapData mapData;
    private TileMap tileMap;
    private MapCamera mapCamera;
    private MapRenderer renderer;
    private final MapCharacter player;
    private final MovementController movement = new MovementController();
    private final MovementController.StepMover stepMover;
    private final EventInterpreter interpreter;
    private MessageWindow messageWindow;
    /** L1: the pause menu overlay (Modular Pause Menu visuals). */
    private PauseMenuOverlay pauseMenu;

    /** L6b: Trainer(N)/Counter(N) events of this map (line-of-sight triggers). */
    private final com.badlogic.gdx.utils.Array<SightTriggers.Sight> sightEvents =
            new com.badlogic.gdx.utils.Array<>();
    private final GraphicsLocator locator;
    private PictureLayer pictureLayer;
    private EventCharacters eventCharacters;
    private MoveRoutePlayer playerRoute;
    /** True while the running event page is an arrival door page (R6.15). */
    private boolean arrivalDoorPage;
    /**
     * R6.15: arrival door pages keep the hero invisible until their forced
     * walk-out route takes its first step (see {@link DoorShowHold}).
     */
    private final DoorShowHold doorShowHold = new DoorShowHold();
    /** True while a fading transfer has already started its fade-out (R6.6). */
    private boolean transferFading;
    /** R6.22: panorama / fog state of this map (Change Map Settings 204 / 206). */
    private final MapEnvironment environment = new MapEnvironment();
    /** R6.22: Scroll Map (203) camera offset. */
    private final CameraScroll cameraScroll = new CameraScroll();
    /** R6.26: pbCaveEntrance / pbCaveExit band animation. */
    private final CaveTransition caveTransition = new CaveTransition();
    /** R6.27: Show Animation (207) effects playing right now. */
    private final MapAnimations mapAnimations = new MapAnimations();
    private final java.util.Set<Integer> animationWarnings = new java.util.HashSet<>();
    /** Timing side effects (SEs and screen flashes) of the animations. */
    private final MapAnimations.TimingSink animationTimingSink = new MapAnimations.TimingSink() {
        @Override
        public void playSe(String name, int volume, int pitch) {
            context.audioManager().playSe(name, volume, pitch);
        }

        @Override
        public void flash(float red, float green, float blue, float alpha, int durationFrames) {
            context.screenEffects().flash(red, green, blue, alpha, durationFrames);
        }
    };
    private Texture panoramaTexture;
    private String panoramaTextureName;
    private Texture fogTexture;
    private String fogTextureName;
    private final java.util.Set<String> environmentWarnings = new java.util.HashSet<>();
    /** R6.23: Essentials map connections (PBS/connections.txt). */
    private MapLinks links = MapLinks.parse(null);
    /**
     * R6.23: neighbour maps drawn beyond the border, keyed by map id. Insertion
     * order doubles as an LRU so a long walk keeps only the maps around the
     * player; a crossing reuses the loaded view instead of rebuilding it
     * (R6.24: that rebuild was the visible hitch at every border).
     */
    private final java.util.LinkedHashMap<Integer, NeighborView> neighbours =
            new java.util.LinkedHashMap<>();
    private static final int NEIGHBOUR_VIEW_LIMIT = 6;
    private MapLinks.Neighbour[] neighborSpecs = new MapLinks.Neighbour[0];
    private final java.util.Set<Integer> neighborWarnings = new java.util.HashSet<>();
    private static boolean warnedMissingSnapEdges;
    private final com.badlogic.gdx.utils.IntSet touched = new com.badlogic.gdx.utils.IntSet();
    /** Small pause between two autorun starts; RMXP restarts them immediately. */
    private float autorunCooldown;
    /**
     * Running parallel pages (trigger 4), one interpreter per event id. They
     * never block the player; RMXP restarts them while their page holds, so a
     * small cooldown keeps a page without its own exit condition from spinning.
     */
    private final com.badlogic.gdx.utils.IntMap<EventInterpreter> parallel =
            new com.badlogic.gdx.utils.IntMap<>();
    private static final int MAX_PARALLEL_EVENTS = 8;
    private float parallelCooldown;
    private long entityVersion = -1;
    private MapRouteContext routeContext;
    private final Texture pixel;
    /** Project-specific knobs (R6.12); the constants above are the fallbacks. */
    private String playerCharacter = PLAYER_CHARACTER;
    private String playerRunningCharacter = PLAYER_RUNNING_CHARACTER;
    private String messageFontName = DEFAULT_MESSAGE_FONT;

    /**
     * @param mapId the map to show; use the System start map for normal runs,
     *              or an explicit id for debugging / demos
     */
    public MapScreen(RuntimeContext context, int mapId) {
        this(context, mapId, Integer.MIN_VALUE, Integer.MIN_VALUE, 0);
    }

    /**
     * @param spawnX     explicit transfer target, or Integer.MIN_VALUE to use the
     *                   System start position / the nearest walkable debug spawn
     * @param direction  RMXP facing to keep after a transfer, 0 to keep the default
     */
    public MapScreen(RuntimeContext context, int mapId, int spawnX, int spawnY, int direction) {
        this.context = context;
        Pixmap white = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        white.setColor(1f, 1f, 1f, 1f);
        white.fill();
        pixel = new Texture(white);
        white.dispose();
        gameState = context.gameState();
        database = context.database();
        if (database == null) {
            mapData = null;
            tileMap = null;
            mapCamera = null;
            renderer = null;
            player = null;
            stepMover = null;
            interpreter = context.eventInterpreter();
            locator = null;
            camera = null;
            Gdx.app.error("MapScreen", "no runtime data; run builder build-data first");
            return;
        }
        MapData data = database.map(mapId);
        // R6.12: the Builder exports the project's own charsets / font, so a
        // different project does not need runtime edits.
        pokemon.runtime.data.ProjectInfo.RuntimeProfile profile = database.project() == null
                ? null : database.project().runtime;
        if (profile != null) {
            if (profile.hasPlayerCharset()) {
                playerCharacter = profile.playerCharset;
            }
            if (profile.hasRunningCharset()) {
                playerRunningCharacter = profile.runningCharset;
            }
            if (profile.hasMessageFont()) {
                messageFontName = profile.messageFont;
            }
        }
        mapData = data;
        collectSightEvents(data);
        links = database.links();
        tileMap = new TileMap(data, database.tileset(data.tilesetId));
        int[] spawn;
        if (spawnX != Integer.MIN_VALUE) {
            spawn = new int[] {spawnX, spawnY}; // Transfer Player target (R6.3)
        } else if (mapId == database.startMapId()) {
            spawn = new int[] {database.startX(), database.startY()};
        } else {
            spawn = Collision.debugSpawn(gameState, tileMap, mapData, database.startX(), database.startY());
        }
        gameState.enterMap(mapId, spawn[0], spawn[1]);
        // RMXP rebuilds the screen state on map setup: the black tone a door
        // applied before the transfer must not survive into the new map.
        context.screenEffects().clearTone();
        if (direction != 0) {
            gameState.setPlayerPosition(spawn[0], spawn[1], direction);
        }
        mapCamera = new MapCamera(tileMap.width(), tileMap.height(),
                viewWidth, viewHeight, clampFor(mapData));
        refreshNeighbourSpecs();
        camera = new OrthographicCamera(viewWidth, viewHeight);
        viewport = new FitViewport(viewWidth, viewHeight, camera);
        locator = new GraphicsLocator(database);
        renderer = new MapRenderer(tileMap, textures, locator, gameState);
        interpreter = context.eventInterpreter();
        if (interpreter != null) {
            interpreter.stop(); // a transfer or a fresh screen never resumes an old event
        }
        if (interpreter != null) {
            messageWindow = new MessageWindow(context.messageService(),
                    locator.font(messageFontName), textures, locator);
        }
        pauseMenu = new PauseMenuOverlay(context, locator);
        pauseMenu.host(new PauseMenuOverlay.Host() {
            @Override
            public void pauseMenuLoaded(String slot) {
                loadFromSlot(slot);
            }

            @Override
            public void pauseMenuTitle() {
                backToTitle();
            }
        });
        pictureLayer = new PictureLayer(context.pictureService(), textures, locator);
        context.pictureService().clear(); // RMXP clears pictures when the map changes
        eventCharacters = new EventCharacters(mapData, tileMap, gameState);
        autoplayMapAudio();
        context.bindScreenPort(new MapPort() {
            @Override
            public void transfer(int mapId, int x, int y, int direction) {
                // Transfers stay with RuntimeContext; this port only plays routes.
            }

            @Override
            public void setMoveRoute(int eventId, MoveRoute route) {
                if (eventId < 0) {
                    playerRoute = new MoveRoutePlayer(route); // "character" = the player
                } else {
                    eventCharacters.setMoveRoute(eventId, route);
                }
            }

            @Override
            public boolean anyRouteActive() {
                if (playerRoute != null && !playerRoute.finished()) {
                    return true;
                }
                if (player.isMoving()) {
                    return true;
                }
                return eventCharacters != null && eventCharacters.anyRouteActive();
            }

            @Override
            public boolean isMoving(int eventId) {
                if (eventId < 0) {
                    return (playerRoute != null && !playerRoute.finished()) || player.isMoving();
                }
                return eventCharacters.isMoving(eventId);
            }

            @Override
            public void setEventLocation(int eventId, int x, int y, int direction) {
                if (eventId < 0) {
                    player.teleport(x, y);
                    if (direction != 0) {
                        player.face(direction);
                    }
                    gameState.setPlayerPosition(player.x(), player.y(), player.direction());
                } else {
                    eventCharacters.setLocation(eventId, x, y, direction);
                }
            }

            @Override
            public void setTransparent(int eventId, boolean transparent) {
                if (eventId < 0) {
                    // RMXP's Change Transparent Flag (208) hides the hero: the
                    // door scripts step the player into the doorway and hide
                    // them until the transfer.
                    if (transparent) {
                        player.opacity = 0f;
                        if (arrivalDoorPage) {
                            doorShowHold.hide();
                        }
                    } else if (doorShowHold.isHolding()) {
                        // R6.15: the arrival page shows the hero before its
                        // walk-out; keep them hidden until that step starts.
                        doorShowHold.requestShow();
                        if (player.isMoving()) {
                            player.opacity = 1f;
                            doorShowHold.clear();
                        }
                    } else {
                        player.opacity = 1f;
                    }
                } else {
                    eventCharacters.setTransparent(eventId, transparent);
                }
            }

            @Override
            public boolean playerOnCharacter(int characterId) {
                MapCharacter other = characterId < 0 || eventCharacters == null
                        ? player : eventCharacters.character(characterId);
                return other != null && player.x() == other.x() && player.y() == other.y();
            }

            /** R6.22: panorama / fog (Change Map Settings 204). */
            @Override
            public void changeMapSettings(int type, com.badlogic.gdx.utils.JsonValue parameters) {
                if (!environment.apply(type, parameters)) {
                    warnEnvironmentOnce("Change Map Settings type " + type
                            + " (battleback / start position) is not handled yet");
                }
            }

            /** R6.22: Change Fog Opacity (206). */
            @Override
            public void changeFogOpacity(int opacity) {
                environment.changeFogOpacity(opacity);
            }

            /** R6.22: Scroll Map (203). */
            @Override
            public void scrollMap(int direction, int distance, int speed) {
                cameraScroll.start(direction, distance, speed);
            }

            /** R6.26: pbCaveEntrance / pbCaveExit (the project's band animation). */
            @Override
            public float caveEntrance(boolean exiting) {
                caveTransition.start(exiting);
                return caveTransition.durationSeconds();
            }

            /** R6.27: Show Animation (207). */
            @Override
            public void showAnimation(int characterId, int animationId) {
                startAnimation(characterId, animationId);
            }

            /** R6.28: Essentials' addUserAnimation (grass rustle, dust, pbExclaim). */
            @Override
            public void showTileAnimation(int animationId, int x, int y, int height) {
                startTileAnimation(animationId, x, y, height);
            }

            /** R6.30: pbExclaim - the bubble above the addressed character. */
            @Override
            public float exclaim(int characterId, int animationId) {
                return MapScreen.this.exclaim(characterId, animationId);
            }

            /** L6: pbNoticePlayer (facing check + exclaim + walk over). */
            @Override
            public float noticePlayer(int characterId) {
                return MapScreen.this.noticePlayer(characterId);
            }

            /** L6: pbSave - quiet save to the quick slot. */
            @Override
            public boolean saveGame() {
                return MapScreen.this.saveGame();
            }

            /** R6.30: pbPushThisBoulder. */
            @Override
            public float pushBoulder(int eventId) {
                return FieldInteractions.pushBoulder(gameState, tileMap, mapData, eventCharacters,
                        eventId, player.direction());
            }

            /** R6.30: toggle_liefeng_switches - the floating-plate puzzle maps. */
            @Override
            public void togglePlateSwitches() {
                FieldInteractions.toggleFloatPlates(gameState, mapData);
            }
        });
        player = new MapCharacter(spawn[0], spawn[1],
                tileMap.width(), tileMap.height(), playerCharacter);
        player.runningCharacterName = playerRunningCharacter;
        // R6.28: the grass rustle fires on steps, not on spawning in the grass.
        lastPlayerTile = new int[] {player.x(), player.y()};
        if (direction != 0) {
            player.face(direction);
        }
        // R6.32: the route context needs the player instance; it wires the live
        // player tile for codes 10/11/25/26 and autonomous movement.
        routeContext = new MapRouteContext(gameState, tileMap, mapData, player,
                this::playerStep, context.audioManager(), context.game()::log);
        stepMover = (character, stepDirection) -> playerStep(character, stepDirection);
        Array<MapCharacter> entities = new Array<>();
        entities.add(player);
        bindEntities(entities, locator);
        entityVersion = gameState.version();
        followPlayer();
        // R6.15: an arrival door page hides the hero with its very first
        // command; hide before the first frame is drawn so no visible frame can
        // slip in when the autorun start is delayed (user report 2026-10-04).
        if (EventTriggers.isArrivalDoorPage(EventTriggers.pageAt(gameState, mapData, player.x(), player.y(),
                EventTriggers.AUTORUN))) {
            player.opacity = 0f;
            doorShowHold.hide();
        }
        // RMXP only fires Player Touch / Event Touch when the player actually
        // stepped onto the tile (Game_Player#update_event_triggering is gated on
        // @moved_this_frame), so an arrival must NOT trigger the event it landed
        // on - that made stair and door pairs bounce straight back. Arrival
        // animations belong to the destination event's autorun page (trigger 3).
        if (context.screenEffects().fade() > 0f) {
            context.screenEffects().fade(12, false); // arriving through a fade
        }
        applyDayNightTone(0); // R11+ : the ambient tone already fits this map
    }

    /**
     * Ambient day/night tone of the player's real clock (see {@link DayNightTone}).
     * Applied on map load and whenever the wall-clock minute changes, unless a
     * script (223) took the tone over - a cave or a door blackout must win.
     * R6.16: only maps marked {@code Outdoor} in PBS/metadata.txt are shaded,
     * which is what the project's own pbDayNightTint does.
     */
    private void applyDayNightTone(int durationFrames) {
        if (!dayNightToneApplies()) {
            return;
        }
        float[] tone = DayNightTone.now();
        appliedTone = tone;
        context.screenEffects().ambientTint(tone[0], tone[1], tone[2], tone[3], durationFrames);
    }

    /** Re-applies the ambient tone when the wall-clock minute rolls over. */
    private void updateDayNightTone() {
        if (!dayNightToneApplies()) {
            return;
        }
        float[] tone = DayNightTone.now();
        if (appliedTone == null || Math.abs(tone[0] - appliedTone[0]) > 0.01f
                || Math.abs(tone[1] - appliedTone[1]) > 0.01f
                || Math.abs(tone[2] - appliedTone[2]) > 0.01f) {
            applyDayNightTone(DayNightTone.TRANSITION_FRAMES);
        }
    }

    /**
     * R6.16: the project's pbDayNightTint shades a map only when its
     * PBS/metadata.txt says {@code Outdoor = true}; indoor maps keep a neutral
     * tone. Generated data from before this batch carries no flag at all: keep
     * the old "shade everywhere" behaviour for it and mention the rebuild once.
     */
    private boolean dayNightToneApplies() {
        if (!DayNightTone.enabled() || context.screenEffects().toneFromScript()) {
            return false;
        }
        if (mapData.outdoor == null && !warnedMissingOutdoor) {
            warnedMissingOutdoor = true;
            context.game().log("map " + mapData.mapId + " has no Outdoor flag; "
                    + "run builder build-data so only outdoor maps get the day/night tone");
        }
        return DayNightTone.shades(mapData.outdoor);
    }

    /** One hint per session is enough for generated data that predates R6.16. */
    private static boolean warnedMissingOutdoor;

    /** R6.17: RGSS tone (day/night + gray desaturation) for the map layer. */
    private final ToneShader worldTone = new ToneShader();
    private boolean worldToneWarned;

    private float[] appliedTone;

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (tileMap == null) {
            return;
        }
        // R11: the letterboxed viewport of this frame (the clear above already
        // painted the bars black).
        viewport.apply();
        context.sampleInput();
        // L1: the pause menu freezes the world (RMXP Scene_Map stops while the
        // menu is open) and owns the frame; X / Esc opens it on the map.
        boolean menuHandled = false;
        if (pauseMenu != null && pauseMenu.isOpen()) {
            pauseMenu.update(delta);
            if (pauseMenu == null) {
                // A host callback (load / back to title) swapped the screen and
                // disposed this one; nothing is left to draw.
                return;
            }
            if (pauseMenu.isOpen()) {
                renderPauseMenu();
                return;
            }
            // The menu closed on this input: fall through to a normal world
            // frame, but the same MENU press must not reopen it right away.
            menuHandled = true;
        }
        if (!menuHandled && pauseMenu != null && context.inputManager().wasPressed(GameAction.MENU)
                && !context.messageService().visible()
                && !(interpreter != null && interpreter.running())
                && !context.transferPending()) {
            pauseMenu.open();
            renderPauseMenu();
            return;
        }
        context.pictureService().update(delta);
        context.screenEffects().update(delta);
        updateCaveTransition(delta);
        mapAnimations.update(delta, animationTimingSink);
        updateNotice(delta); // L6c: deferred second half of pbNoticePlayer
        if (eventCharacters != null && routeContext != null) {
            eventCharacters.update(delta, routeContext);
        }
        if (eventCharacters != null && gameState.version() != entityVersion) {
            // A switch flipped a page: pick up the new sheet and rebind entities.
            entityVersion = gameState.version();
            eventCharacters.refreshGraphics();
            Array<MapCharacter> base = new Array<>();
            base.add(player);
            bindEntities(base, locator);
        } else if (routeContext != null && routeContext.takeGraphicsDirty()) {
            // R6.18: move route code 41 changed a character sheet (the nurse
            // swapping BW 071 / BW 072, for example); reload its sprites.
            Array<MapCharacter> base = new Array<>();
            base.add(player);
            bindEntities(base, locator);
        }
        if (playerRoute != null && routeContext != null) {
            playerRoute.update(delta, player, routeContext);
            if (playerRoute.finished()) {
                playerRoute = null;
            }
        }
        if (autorunCooldown > 0f) {
            autorunCooldown -= delta;
        }
        updateAutorun();
        updateParallelEvents(delta);
        // A step an event route started has to land even though the interpreter
        // owns the input: RMXP updates every character before it runs commands,
        // and without this the hero is stuck mid-step forever (map353/EV006).
        boolean scriptDriven = playerRoute != null || context.transferPending()
                || (interpreter != null && interpreter.running());
        if (interpreter != null && interpreter.running()) {
            // An event is waiting for input / time: the player stands still (RMXP).
            interpreter.update(delta);
        } else if (context.messageService().visible()) {
            // The running event ended or was cut short while its window was up.
            context.messageService().close();
        } else if (!scriptDriven) {
            // Controller advances interpolation too; never update the player twice per frame.
            int beforeX = player.x();
            int beforeY = player.y();
            movement.update(context.inputManager(), delta, player, stepMover);
            gameState.setPlayerPosition(player.x(), player.y(), player.direction());
            if (interpreter != null && context.inputManager().wasPressed(GameAction.CONFIRM)) {
                startFacingEvent();
            } else if (interpreter != null && !interpreter.running()) {
                if (player.x() != beforeX || player.y() != beforeY) {
                    // RMXP checks Player Touch (1) and Event Touch (2) together,
                    // and only on the frame the player stepped onto the tile.
                    startTouchEvent(player.x(), player.y()); // Player Touch: arrived
                    checkEventTouch();                       // Event Touch: same tile
                    checkSightTriggers();                    // L6b: Trainer(N)/Counter(N) sight
                } else {
                    startBlockedTouchEvent();                // Player Touch: walked into it
                }
            }
        }
        if (scriptDriven && player.isMoving()) {
            player.advance(delta);
            gameState.setPlayerPosition(player.x(), player.y(), player.direction());
        }
        // R6.23: a step that left the map edge lands in the connected
        // neighbour (PokemonMapFactory#setCurrentMap).
        updateMapConnection();
        updateGrassRustle(); // R6.28: Essentials field-movement rustle
        // R6.15/R6.33: the arrival door page must not show the hero before its
        // forced walk-out route really steps. On maps with several doors, every
        // door's arrival page is queued first (their tsOff?("A") conditions are
        // all true on arrival), and the old !pageRunning check released the
        // hold in the gap between two pages: the hero stood visible on the
        // doorstep for ~0.5 s until their own page was reached (reported on
        // the 茶月镇 houses). Keep waiting while an autorun page is pending.
        boolean pageRunning = interpreter != null && interpreter.running();
        if (!pageRunning && doorShowHold.isHolding()) {
            pageRunning = EventTriggers.autorunPage(gameState, mapData) != null;
        }
        if (doorShowHold.update(delta, pageRunning, player.isMoving())) {
            player.opacity = 1f;
        }
        if (interpreter == null || !interpreter.running()) {
            arrivalDoorPage = false;
        }
        // RGSS animates every character each frame: the walk cycle while moving
        // and the "step anime" bob the player character keeps while standing.
        player.updateAnimation(delta);
        if (player.isMoving()) {
            // R6.22: RMXP re-centres the view as soon as the player moves.
            cameraScroll.reset();
        }
        followPlayer();
        updateNeighbourViews(); // R6.23: neighbours in range of the new camera
        warmNeighbourViews();   // R6.24: spread their character sheets out
        updateDayNightTone();

        batch.setProjectionMatrix(camera.combined);
        renderer.update(delta);
        environment.update(delta);  // R6.22: fog drift
        cameraScroll.update(delta); // R6.22: Scroll Map (203)
        renderPanorama();
        applyWorldTone();
        for (NeighborView view : neighbours.values()) {
            view.renderer.update(delta);
            view.renderer.render(batch, mapCamera, view.offsetX, view.offsetY);
        }
        prepareGroundAnimations();
        renderer.render(batch, mapCamera, this::renderGroundAnimationsBefore);
        renderAnimations(); // R6.27: Show Animation (207) sits on the toned map
        batch.setShader(null);
        renderFog();
        renderPictures();
        renderMessageWindow();
        renderScreenEffects();
        renderCaveTransition();
        // Transfer Player (201): the fade option blacks the old map out first.
        // The swap waits for that fade, otherwise a door leaves the player in
        // front of a black window forever (the request must keep being polled).
        if (context.transferPending() && context.pendingTransferFade() != 0
                && !transferFading && context.screenEffects().fade() < 250f) {
            transferFading = true;
            if (caveTransition.active()) {
                // L6d: the cave animation already holds a full black/white
                // cover - blacking out at once keeps the transition seamless
                // (no uncovered map frame, no second visible fade).
                context.screenEffects().fade(0, true);
            } else {
                context.screenEffects().fade(12, true); // fade to black, then switch
            }
        }
        RuntimeContext.Transfer transfer = context.takeReadyTransfer();
        if (transfer != null) {
            transferFading = false;
            switchMap(transfer);
        }
    }

    /**
     * L1: loads a save slot from the pause menu. The saved map / position wins
     * (RMXP's Scene_Load); the current screen swaps itself out like a transfer.
     */
    private void loadFromSlot(String slot) {
        if (pauseMenu != null) {
            pauseMenu.close();
        }
        GameState state = context.gameState();
        try {
            if (!context.saveManager().load(context.storage(), slot, state)) {
                return;
            }
        } catch (RuntimeException error) {
            Gdx.app.error("MapScreen", "load failed: " + error.getMessage());
            return;
        }
        context.audioManager().stopBgm();
        MapScreen next = new MapScreen(context, state.currentMapId(), state.playerX(),
                state.playerY(), state.playerDirection());
        context.game().setScreen(next);
        dispose();
    }

    /** L1: "back to title" from the pause menu. */
    private void backToTitle() {
        context.audioManager().stopBgm();
        context.game().setScreen(new TitleScreen(context));
        dispose();
    }

    /**
     * Transfer Player (201): builds the target screen and swaps it in. The old
     * screen disposes itself because Game.setScreen does not dispose screens.
     */
    private void switchMap(RuntimeContext.Transfer transfer) {
        try {
            if (transfer.fade != 0) {
                context.screenEffects().fade(0, true); // the new map starts black
            }
            MapScreen next = new MapScreen(context, transfer.mapId, transfer.x, transfer.y, transfer.direction);
            context.game().setScreen(next);
            dispose();
        } catch (RuntimeException error) {
            // A broken transfer target must not take the whole game down: keep
            // the current map, release the waiting event and report the reason.
            context.game().log("transfer to map " + transfer.mapId + " failed: " + error.getMessage());
            if (interpreter != null) {
                interpreter.stop();
            }
        }
    }

    /** Starts the action-trigger page (trigger 0) of the event the player faces. */
    private void startFacingEvent() {
        int x = Collision.targetX(player.x(), player.direction());
        int y = Collision.targetY(player.y(), player.direction());
        facePlayerWhenTalkedTo(x, y);
        startEvent(x, y, EventTriggers.ACTION);
    }

    /**
     * An event the player talks to turns around to face them, unless its page
     * checked "direction fix" (R6.10). The page's own move routes still run
     * afterwards, so a cutscene that turns the event itself wins.
     */
    private void facePlayerWhenTalkedTo(int x, int y) {
        if (eventCharacters == null) {
            return;
        }
        MapData.EventPageData page = EventTriggers.pageAt(gameState, mapData, x, y, EventTriggers.ACTION);
        if (page == null || page.movement.directionFix) {
            return;
        }
        MapCharacter character = eventCharacters.character(
                EventTriggers.eventIdAt(gameState, mapData, x, y, EventTriggers.ACTION));
        if (character == null || (character.x() == player.x() && character.y() == player.y())) {
            return;
        }
        int dx = player.x() - character.x();
        int dy = player.y() - character.y();
        if (Math.abs(dx) > Math.abs(dy)) {
            character.face(dx > 0 ? 6 : 4);
        } else if (dy != 0) {
            character.face(dy > 0 ? 2 : 8);
        }
    }

    /** Player Touch (trigger 1): the player just finished a step onto the tile. */
    private void startTouchEvent(int x, int y) {
        startEvent(x, y, EventTriggers.PLAYER_TOUCH);
    }

    /**
     * Player Touch when the destination is blocked by the event itself: RMXP
     * doors are walked into, so a held direction towards the event starts it.
     */
    private void startBlockedTouchEvent() {
        var input = context.inputManager();
        if (input.isDown(GameAction.UP) && startEvent(player.x(), player.y() - 1, EventTriggers.PLAYER_TOUCH)) return;
        if (input.isDown(GameAction.DOWN) && startEvent(player.x(), player.y() + 1, EventTriggers.PLAYER_TOUCH)) return;
        if (input.isDown(GameAction.LEFT) && startEvent(player.x() - 1, player.y(), EventTriggers.PLAYER_TOUCH)) return;
        if (input.isDown(GameAction.RIGHT)) startEvent(player.x() + 1, player.y(), EventTriggers.PLAYER_TOUCH);
    }

    private boolean startEvent(int x, int y, int trigger) {
        MapData.EventPageData page = EventTriggers.pageAt(gameState, mapData, x, y, trigger);
        if (page == null) {
            return false;
        }
        arrivalDoorPage = false;
        interpreter.start(page.commands, mapData.mapId, EventTriggers.eventIdAt(gameState, mapData, x, y, trigger));
        return true;
    }

    /**
     * Autorun (trigger 3): while nothing else runs, start the first autorun
     * page in event order; it restarts after finishing unless the event flips a
     * switch / self switch itself, which is how RMXP works too.
     */
    private void updateAutorun() {
        if (interpreter == null || interpreter.running() || playerRoute != null
                || context.transferPending() || autorunCooldown > 0f) {
            return;
        }
        MapData.EventPageData page = EventTriggers.autorunPage(gameState, mapData);
        if (page == null) {
            return;
        }
        int eventId = EventTriggers.autorunEventId(gameState, mapData);
        arrivalDoorPage = EventTriggers.isArrivalDoorPage(page);
        interpreter.start(page.commands, mapData.mapId, eventId);
        autorunCooldown = 1f / 40f; // one game frame: RMXP starts queued pages back-to-back
    }

    /** Event touch (trigger 2): an event that stands on the player's tile. */
    private void checkEventTouch() {
        if (interpreter == null || interpreter.running() || playerRoute != null
                || context.transferPending()) {
            return;
        }
        MapData.EventPageData page = EventTriggers.eventTouchPage(gameState, mapData, player.x(), player.y());
        if (page == null) {
            touched.clear();
            return;
        }
        int eventId = EventTriggers.eventTouchEventId(gameState, mapData, player.x(), player.y());
        if (eventId >= 0 && touched.add(eventId)) {
            interpreter.start(page.commands, mapData.mapId, eventId);
        }
    }

    /**
     * L6b/L6c: the map's {@code Trainer(N)}/{@code Counter(N)} sight events are
     * parsed once per map (the name never changes). Called by the constructor
     * and again whenever a map connection swaps the map under this screen.
     */
    private void collectSightEvents(MapData data) {
        sightEvents.clear();
        for (MapData.EventData event : data.events) {
            SightTriggers.Sight sight = SightTriggers.parse(event.id, event.name);
            if (sight != null) {
                sightEvents.add(sight);
            }
        }
    }

    /**
     * L6b: Essentials line-of-sight triggers. {@code Trainer(N)} starts when
     * the player stands on the event's facing line within N tiles with every
     * tile between passable ({@code pbEventCanReachPlayer?}); {@code
     * Counter(N)} only needs the facing line ({@code pbEventFacesPlayer?}).
     * Like the original (Game_Player's pbCheckEventTriggerFromDistance([2])),
     * this runs when the player finishes a step; both event kinds start their
     * trigger-2 page.
     */
    private void checkSightTriggers() {
        if (interpreter == null || interpreter.running() || playerRoute != null
                || context.transferPending() || eventCharacters == null
                || sightEvents.size == 0) {
            return;
        }
        for (SightTriggers.Sight sight : sightEvents) {
            MapCharacter character = eventCharacters.character(sight.eventId);
            if (character == null) {
                continue;
            }
            int steps = SightTriggers.lineSteps(character.x(), character.y(), character.direction(),
                    player.x(), player.y(), sight.distance);
            if (steps < 0) {
                continue;
            }
            if (sight.kind == SightTriggers.Kind.TRAINER && !lineOfSightClear(character, steps)) {
                continue;
            }
            if (startEvent(character.x(), character.y(), EventTriggers.EVENT_TOUCH)) {
                // L6c: RMXP starts no further step once the trainer's page is
                // running; without this the step the held key began in the
                // same frame lands during the freeze and the player visibly
                // slides one tile after being spotted.
                if (player != null && player.isMoving()) {
                    player.teleport(player.x(), player.y());
                }
                return; // one page at a time: the shared interpreter owns the frame
            }
        }
    }

    /** pbEventCanReachPlayer? passability: every tile strictly between is open. */
    private boolean lineOfSightClear(MapCharacter character, int steps) {
        int direction = character.direction();
        int dx = direction == 6 ? 1 : direction == 4 ? -1 : 0;
        int dy = direction == 2 ? 1 : direction == 8 ? -1 : 0;
        int x = character.x();
        int y = character.y();
        for (int i = 0; i < steps - 1; i++) {
            if (!Collision.canStepFrom(gameState, tileMap, mapData, character, x, y, direction)) {
                return false;
            }
            x += dx;
            y += dy;
        }
        return true;
    }

    private void updateParallelEvents(float delta) {
        if (parallelCooldown > 0f) {
            parallelCooldown -= delta;
        }
        if (parallelCooldown <= 0f) {
            for (MapData.EventData event : mapData.events) {
                if (parallel.size >= MAX_PARALLEL_EVENTS) {
                    break;
                }
                if (parallel.containsKey(event.id)) {
                    continue;
                }
                MapData.EventPageData page = EventTriggers.parallelPage(gameState, mapData, event);
                if (page == null) {
                    continue;
                }
                EventInterpreter interpreter = context.newEventInterpreter();
                interpreter.start(page.commands, mapData.mapId, event.id);
                parallel.put(event.id, interpreter);
            }
        }
        com.badlogic.gdx.utils.IntArray finished = null;
        for (com.badlogic.gdx.utils.IntMap.Entry<EventInterpreter> entry : parallel) {
            entry.value.update(delta);
            if (!entry.value.running()) {
                if (finished == null) {
                    finished = new com.badlogic.gdx.utils.IntArray();
                }
                finished.add(entry.key);
            }
        }
        if (finished != null) {
            for (int i = 0; i < finished.size; i++) {
                parallel.remove(finished.get(i));
                parallelCooldown = 0.2f;
            }
        }
    }

    /**
     * Character-graphic events join the entity path so NPC routes render with
     * the same interpolation and depth sorting as the player; tile-graphic
     * events (signs, doors drawn from tiles) stay on the static path.
     */
    private void bindEntities(Array<MapCharacter> entities, GraphicsLocator locator) {
        Array<MapCharacter> all = new Array<>();
        all.addAll(entities);
        boolean[] runtime = new boolean[mapData.events.size];
        for (int i = 0; i < mapData.events.size; i++) {
            MapCharacter character = eventCharacters.characters().get(i);
            if (character == null) {
                continue;
            }
            all.add(character);
            // R6.21: an event whose current page has no character sheet goes
            // back to the static path - a blank page draws nothing and a tile
            // page draws its tile, while the entity path would keep painting a
            // stale sheet.
            runtime[i] = character.characterName != null && !character.characterName.isEmpty();
        }
        renderer.setEntities(all, textures, locator);
        renderer.setRuntimeEvents(runtime);
    }

    /**
     * R6.23: Game_Map#display_x= only clamps the camera for maps whose
     * PBS/metadata.txt says {@code SnapEdges = true}; every other map follows
     * the player past its own border, which is what makes the connected maps
     * show and scroll seamlessly. Generated data from before the flag keeps the
     * old clamped behaviour and asks for a rebuild once.
     */
    private boolean clampFor(MapData data) {
        if (data.snapEdges == null) {
            if (!warnedMissingSnapEdges) {
                warnedMissingSnapEdges = true;
                context.game().log("map " + data.mapId + " has no SnapEdges flag; run builder "
                        + "build-data so connected maps scroll like the original");
            }
            return true;
        }
        return data.snapEdges;
    }

    /**
     * R6.23: a player step, including {@code MapFactory#isPassableFromEdge?}:
     * the target may be outside the current map when a connection covers it,
     * and then only the neighbour's landing cell decides.
     */
    private boolean playerStep(MapCharacter character, int direction) {
        if (!tileMap.valid(character.x(), character.y())) {
            // The step out of the map has landed but the crossing has not been
            // applied yet: stand still for that one frame instead of walking
            // further into empty space.
            return false;
        }
        int x = Collision.targetX(character.x(), direction);
        int y = Collision.targetY(character.y(), direction);
        if (!tileMap.valid(x, y)) {
            MapLinks.Crossing crossing = links.crossing(mapData.mapId, x, y);
            if (crossing == null) {
                return false;
            }
            NeighborView view = neighbourFor(crossing.mapId);
            if (view == null || !Collision.canStepIntoNeighbour(gameState, view.tileMap,
                    view.data, character, crossing.x, crossing.y)) {
                return false;
            }
            return character.startMove(x, y, direction, true);
        }
        return Collision.canStep(gameState, tileMap, mapData, character, direction)
                && character.startMove(x, y, direction);
    }

    /** The connection of the current map that touches {@code mapId}. */
    private MapLinks.Link linkTo(int mapId) {
        for (MapLinks.Link link : links.linksOf(mapData.mapId)) {
            if (link.a == mapId || link.b == mapId) {
                return link;
            }
        }
        return null;
    }

    /** Loads (and caches) the neighbour view of {@code mapId}. */
    private NeighborView neighbourFor(int mapId) {
        NeighborView view = neighbours.get(mapId);
        if (view != null) {
            return view;
        }
        if (neighborWarnings.contains(mapId)) {
            return null; // a broken neighbour is not retried every frame
        }
        MapLinks.Link link = linkTo(mapId);
        if (link == null) {
            return null;
        }
        return loadNeighbour(links.neighbour(link, mapData.mapId));
    }

    private NeighborView loadNeighbour(MapLinks.Neighbour spec) {
        long started = System.nanoTime();
        try {
            MapData data = database.map(spec.mapId);
            long dataDone = System.nanoTime();
            TileMap map = new TileMap(data, database.tileset(data.tilesetId));
            NeighborView view = new NeighborView(data, map,
                    new MapRenderer(map, textures, locator, gameState), spec.offsetX, spec.offsetY);
            putNeighbour(view);
            if (data.autoplayBgm && data.bgm != null && !data.bgm.isEmpty()) {
                // R6.24: the crossing then only has to cue, not open the file.
                context.audioManager().warmBgm(data.bgm.name);
            }
            long done = System.nanoTime();
            if ((done - started) / 1_000_000L >= 10L) {
                context.game().log("preloading neighbour map " + spec.mapId + " took "
                        + ((done - started) / 1_000_000L) + " ms (data "
                        + ((dataDone - started) / 1_000_000L) + " ms, render "
                        + ((done - dataDone) / 1_000_000L) + " ms)");
            }
            return view;
        } catch (RuntimeException error) {
            if (neighborWarnings.add(spec.mapId)) {
                context.game().log("map connection to " + spec.mapId + " failed: "
                        + error.getMessage());
            }
            return null;
        }
    }

    /** Adds or replaces a loaded neighbour view, dropping the oldest beyond the limit. */
    private void putNeighbour(NeighborView view) {
        neighbours.put(view.data.mapId, view);
        while (neighbours.size() > NEIGHBOUR_VIEW_LIMIT) {
            java.util.Iterator<Integer> oldest = neighbours.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
    }

    /** Recomputes the offsets of every connected neighbour of the current map. */
    private void refreshNeighbourSpecs() {
        java.util.List<MapLinks.Link> linksOf = links.linksOf(mapData.mapId);
        neighborSpecs = new MapLinks.Neighbour[linksOf.size()];
        for (int i = 0; i < linksOf.size(); i++) {
            neighborSpecs[i] = links.neighbour(linksOf.get(i), mapData.mapId);
        }
    }

    /**
     * R6.23: the project keeps a connection's map loaded once it is within the
     * screen plus six tiles (MapFactoryHelper#mapInRangeById?), so its tiles
     * are already there when the player walks to the border.
     */
    private void updateNeighbourViews() {
        if (neighborSpecs.length == 0 || mapCamera == null) {
            return;
        }
        int margin = 6 * TilesetGeometry.TILE_SIZE;
        int left = mapCamera.originX() - margin;
        int right = mapCamera.originX() + mapCamera.viewPixelWidth() + margin;
        int bottom = mapCamera.originY() - margin;
        int top = mapCamera.originY() + mapCamera.viewPixelHeight() + margin;
        for (MapLinks.Neighbour spec : neighborSpecs) {
            if (neighbours.containsKey(spec.mapId) || neighborWarnings.contains(spec.mapId)) {
                continue;
            }
            int[] dims = links.dims(spec.mapId);
            if (dims == null) {
                continue;
            }
            int mapLeft = spec.offsetX;
            int mapRight = spec.offsetX + dims[0] * TilesetGeometry.TILE_SIZE;
            int mapBottom = spec.offsetY;
            int mapTop = spec.offsetY + dims[1] * TilesetGeometry.TILE_SIZE;
            if (mapRight <= left || mapLeft >= right || mapTop <= bottom || mapBottom >= top) {
                continue;
            }
            loadNeighbour(spec);
            break; // one new neighbour per frame keeps the frame budget
        }
    }

    /**
     * R6.24: give every loaded neighbour a small per-frame budget to finish
     * loading its character sheets, so the crossing itself is instant.
     */
    private void warmNeighbourViews() {
        if (neighbours.isEmpty()) {
            return;
        }
        for (NeighborView view : neighbours.values()) {
            view.renderer.warmCharacters(3_000_000L);
        }
    }

    /**
     * R6.23: {@code PokemonMapFactory#setCurrentMap} - once the step out of the
     * map has landed, the neighbour becomes the current map. Nothing fades and
     * the camera does not jump because the teleport and the drawing offsets are
     * both derived from the connection's border points.
     */
    private void updateMapConnection() {
        if (links.isEmpty() || player == null || tileMap == null || player.isMoving()) {
            return;
        }
        if (tileMap.valid(player.x(), player.y())) {
            return;
        }
        MapLinks.Crossing crossing = links.crossing(mapData.mapId, player.x(), player.y());
        if (crossing != null) {
            crossTo(crossing);
        }
    }

    private void crossTo(MapLinks.Crossing crossing) {
        long started = System.nanoTime();
        try {
            MapLinks.Link link = linkTo(crossing.mapId);
            // The crossing map is almost always already rendered beyond the
            // border (playerStep loaded it), so reuse that view: the step then
            // costs no map parse, no MapRenderer and no texture work.
            NeighborView reused = neighbours.remove(crossing.mapId);
            MapData next = reused != null ? reused.data : database.map(crossing.mapId);
            TileMap nextMap = reused != null ? reused.tileMap
                    : new TileMap(next, database.tileset(next.tilesetId));
            MapRenderer nextRenderer = reused != null ? reused.renderer
                    : new MapRenderer(nextMap, textures, locator, gameState);
            EventCharacters nextCharacters = new EventCharacters(next, nextMap, gameState);
            if (link != null && renderer != null) {
                // The map we leave stays loaded and visible behind us; its
                // renderer goes back to the static event path.
                MapLinks.Neighbour back = links.neighbour(link, crossing.mapId);
                renderer.setEntities(new Array<>(), textures, locator);
                renderer.setRuntimeEvents(new boolean[0]);
                putNeighbour(new NeighborView(mapData, tileMap, renderer,
                        back.offsetX, back.offsetY));
            }
            long setupDone = System.nanoTime();
            MapCamera nextCamera = new MapCamera(nextMap.width(), nextMap.height(),
                    viewWidth, viewHeight, clampFor(next));
            mapData = next;
            tileMap = nextMap;
            renderer = nextRenderer;
            eventCharacters = nextCharacters;
            mapCamera = nextCamera;
            // L6c: crossing a connection reuses this screen, so every piece of
            // map-bound state the constructor derives from the map must be
            // re-derived here too. SightTriggers was missed: trainers/counters
            // on a map entered over a connection never fired (user report
            // 2026-10-05, "all trainers on this map are broken"), and the
            // parallel pages of the map we left kept running on top.
            collectSightEvents(next);
            for (EventInterpreter running : parallel.values()) {
                running.stop();
            }
            parallel.clear();
            parallelCooldown = 0f;
            noticeEventId = -1; // a pending pbNoticePlayer walk dies with the map
            // L6c: the route context captured the tile map / map data it was
            // built with; after the swap it must be rebuilt or every forced
            // route (trainer walk-up included) keeps colliding against the map
            // that was left behind.
            routeContext = new MapRouteContext(gameState, tileMap, mapData, player,
                    this::playerStep, context.audioManager(), context.game()::log);
            refreshNeighbourSpecs();
            player.teleport(crossing.x, crossing.y);
            player.setMapBounds(next.width, next.height);
            lastPlayerTile = new int[] {player.x(), player.y()};
            gameState.walkToMap(next.mapId, crossing.x, crossing.y);
            if (interpreter != null) {
                interpreter.stop();
            }
            arrivalDoorPage = false;
            doorShowHold.clear();
            cameraScroll.reset();
            environment.reset();
            Array<MapCharacter> base = new Array<>();
            base.add(player);
            bindEntities(base, locator);
            long entitiesDone = System.nanoTime();
            entityVersion = gameState.version();
            applyDayNightTone(0);
            followPlayer();
            autoplayMapAudio();
            long done = System.nanoTime();
            if ((done - started) / 1_000_000L >= 10L) {
                context.game().log("map connection to " + crossing.mapId + " took "
                        + ((done - started) / 1_000_000L) + " ms (setup "
                        + ((setupDone - started) / 1_000_000L) + " ms, entities "
                        + ((entitiesDone - setupDone) / 1_000_000L) + " ms)");
            }
        } catch (RuntimeException error) {
            context.game().log("map connection to " + crossing.mapId + " failed: "
                    + error.getMessage());
            // Keep the game playable: fall back to the nearest tile inside.
            int safeX = Math.max(0, Math.min(player.x(), tileMap.width() - 1));
            int safeY = Math.max(0, Math.min(player.y(), tileMap.height() - 1));
            player.teleport(safeX, safeY);
            gameState.setPlayerPosition(safeX, safeY);
        }
    }

    /**
     * Map switches must stay instant; anything long enough to show up as a
     * hitch is named in the log instead of staying silent.
     */
    private void reportSlow(String what, long startedNanos) {
        long millis = (System.nanoTime() - startedNanos) / 1_000_000L;
        if (millis >= 10L) {
            context.game().log(what + " took " + millis + " ms");
        }
    }

    /** One loaded neighbour map; offsets are in this map's y-up pixel space. */
    private static final class NeighborView {
        final MapData data;
        final TileMap tileMap;
        final MapRenderer renderer;
        final int offsetX;
        final int offsetY;

        NeighborView(MapData data, TileMap tileMap, MapRenderer renderer,
                     int offsetX, int offsetY) {
            this.data = data;
            this.tileMap = tileMap;
            this.renderer = renderer;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }

    /** L1 integration aid (capture tools): is the pause menu open right now? */
    public boolean pauseMenuOpen() {
        return pauseMenu != null && pauseMenu.isOpen();
    }

    /** L1: draws the pause menu over the frozen map (logical screen space). */
    private void renderPauseMenu() {
        // This screen's camera follows the player (world space). The menu is
        // drawn in logical screen coordinates, so recenter the camera for this
        // pass and restore it afterwards (the world needs it back next frame).
        float savedX = camera.position.x;
        float savedY = camera.position.y;
        camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 0f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        pauseMenu.render(batch);
        batch.end();
        camera.position.set(savedX, savedY, 0f);
        camera.update();
    }

    /** Draws the message window above the map (R6.2, skinned in R6.31). */
    private void renderMessageWindow() {
        if (messageWindow == null || interpreter == null || !interpreter.messages().visible()) {
            return;
        }
        messageWindow.prepare(); // skin / cursor textures outside the batch
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        messageWindow.render(batch, mapCamera.originX(), mapCamera.originY(),
                mapCamera.viewPixelWidth(), mapCamera.viewPixelHeight());
        batch.end();
    }

    /**
     * Map music comes from the map's own properties (RMXP {@code autoplayBgm} /
     * {@code autoplayBgs}), not from an event command - most maps of this
     * project define their BGM that way.
     */
    private void autoplayMapAudio() {
        if (mapData.autoplayBgm && mapData.bgm != null && !mapData.bgm.isEmpty()) {
            context.audioManager().cueBgm(mapData.bgm.name, mapData.bgm.volume,
                    mapData.bgm.pitch, MAP_BGM_CUE_SECONDS);
        }
        if (mapData.autoplayBgs && mapData.bgs != null && !mapData.bgs.isEmpty()) {
            context.audioManager().playBgs(mapData.bgs.name, mapData.bgs.volume, mapData.bgs.pitch);
        }
    }

    /** Draws Show Picture content between the map and the message window (R6.4). */
    private void renderPictures() {
        if (pictureLayer == null || context.pictureService().isEmpty()) {
            return;
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        pictureLayer.render(batch, mapCamera.originX(), mapCamera.originY(), viewHeight);
        batch.end();
    }

    /** Keeps the player centered like the RMXP scroll (R11 adds the viewport). */
    private void followPlayer() {
        // R6.22: a Scroll Map (203) offsets the camera; it stays there until the
        // player moves, like RMXP (the offset is clamped with the camera).
        mapCamera.centerOnPixels(player.pixelX() + cameraScroll.offsetX(),
                player.pixelY() + cameraScroll.offsetY());
        ScreenEffects effects = context.screenEffects();
        camera.position.set(
                mapCamera.originX() + camera.viewportWidth / 2f + effects.shakeX(),
                mapCamera.originY() + camera.viewportHeight / 2f + effects.shakeY(), 0f);
        camera.update();
    }

    /**
     * R6.22: the panorama sits behind the tilemap (RMXP plane z = -1000),
     * tiled across the viewport. This project's Spriteset_Map draws it with
     * {@code @panorama.ox = display_x / 2}: a half-speed parallax backdrop.
     */
    private void renderPanorama() {
        MapEnvironment.Layer layer = environment.panorama();
        if (!layer.visible()) {
            return;
        }
        Texture texture = environmentTexture(layer, "Panoramas", true);
        if (texture != null) {
            drawEnvironmentLayer(texture, layer, 1f, 0.5f);
        }
    }

    /**
     * R6.22: the fog sits above the characters (RMXP plane z = 3000) and is
     * world-anchored: Spriteset_Map does {@code @fog.ox = display_x +
     * @map.fog_ox}, so the cloud pattern stays pinned to the map while the
     * camera moves and only its own {@code sx}/{@code sy} drift slides it.
     */
    private void renderFog() {
        MapEnvironment.Layer layer = environment.fog();
        if (!layer.visible()) {
            return;
        }
        Texture texture = environmentTexture(layer, "Fogs", false);
        if (texture != null) {
            drawEnvironmentLayer(texture, layer,
                    Math.max(0f, Math.min(1f, layer.opacity / 255f)), 1f);
        }
    }

    /**
     * Draws one tiled environment plane. The batch runs in world coordinates,
     * so the phase pins the pattern to the world: {@link MapEnvironment#planePhase}
     * mirrors RGSS {@code Plane#ox}. The panorama parallaxes at half speed,
     * the fog is fully world-anchored with only its own drift.
     */
    private void drawEnvironmentLayer(Texture texture, MapEnvironment.Layer layer, float alpha,
                                      float parallax) {
        float zoom = Math.max(1, layer.zoom) / 100f;
        float width = texture.getWidth() * zoom;
        float height = texture.getHeight() * zoom;
        if (width < 1f || height < 1f) {
            return;
        }
        float originX = mapCamera.originX();
        float originY = mapCamera.originY();
        float viewWidth = mapCamera.viewPixelWidth();
        float viewHeight = mapCamera.viewPixelHeight();
        float phaseX = MapEnvironment.planePhase(originX, parallax, layer.ox, width);
        float phaseY = MapEnvironment.planePhase(originY, parallax, layer.oy, height);
        batch.begin();
        if (layer.blendType == 1) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        }
        batch.setColor(1f, 1f, 1f, alpha);
        for (float x = originX + phaseX; x < originX + viewWidth; x += width) {
            for (float y = originY + phaseY; y < originY + viewHeight; y += height) {
                batch.draw(texture, x, y, width, height);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.end();
    }

    /**
     * Loads / caches one panorama or fog texture. A missing graphic or a hue
     * rotation (not rendered yet) is reported once, like the rest of R6.
     */
    private Texture environmentTexture(MapEnvironment.Layer layer, String directory,
                                       boolean panorama) {
        String name = layer.name;
        Texture cached = panorama ? panoramaTexture : fogTexture;
        String cachedName = panorama ? panoramaTextureName : fogTextureName;
        if (name.equals(cachedName)) {
            return cached;
        }
        String file = name.endsWith(".png") ? name : name + ".png";
        java.io.File found = locator == null ? null : locator.find(directory, file);
        Texture texture = found == null ? null
                : textures.load(directory.toLowerCase() + ":" + name, found);
        if (panorama) {
            panoramaTexture = texture;
            panoramaTextureName = name;
        } else {
            fogTexture = texture;
            fogTextureName = name;
        }
        if (texture == null) {
            warnEnvironmentOnce(directory + "/" + file + " is missing; the layer stays empty");
        } else if (layer.hue != 0) {
            warnEnvironmentOnce(directory + "/" + file + " hue " + layer.hue
                    + " is not rendered yet");
        }
        return texture;
    }

    /** One warning per distinct environment message (parallel pages repeat them). */
    private void warnEnvironmentOnce(String message) {
        if (environmentWarnings.add(message)) {
            context.game().log(message);
        }
    }

    /**
     * R6.17: the project's pbDayNightTint tones the map view (tilemap +
     * characters) with a full RGSS Tone - channel shifts plus gray
     * desaturation. {@link ToneShader} renders both while the batch draws the
     * map; pictures and the message window stay un-toned, exactly like the
     * original (only the map viewport is tinted there). A script tone (223)
     * still owns the screen and is drawn as an overlay instead.
     */
    private void applyWorldTone() {
        ScreenEffects effects = context.screenEffects();
        if (effects.toneFromScript() || !dayNightToneApplies()) {
            batch.setShader(null);
            return;
        }
        if (!worldTone.isCompiled()) {
            if (!worldToneWarned) {
                worldToneWarned = true;
                context.game().log("day/night tone shader failed to compile: " + worldTone.log());
            }
            batch.setShader(null);
            return;
        }
        worldTone.setTone(effects.toneRed(), effects.toneGreen(), effects.toneBlue(),
                effects.toneGray());
        batch.setShader(worldTone.program());
    }

    /** Fade and flash overlays, drawn above pictures and the message window. */
    private void renderScreenEffects() {
        ScreenEffects effects = context.screenEffects();
        float fade = Math.max(0f, Math.min(1f, effects.fade() / 255f));
        float toneRed = effects.toneRed();
        float toneGreen = effects.toneGreen();
        float toneBlue = effects.toneBlue();
        // R6.17: the ambient day/night tone is drawn by ToneShader on the map
        // layer; this overlay only serves a script tone (223), whose negative
        // channels are how the door / cave blackouts work.
        boolean tinted = effects.toneFromScript()
                && (toneRed < 0f || toneGreen < 0f || toneBlue < 0f);
        if (fade <= 0f && !effects.flashing() && !tinted) {
            return;
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (effects.flashing()) {
            batch.setColor(effects.flashRed(), effects.flashGreen(), effects.flashBlue(),
                    Math.max(0f, Math.min(1f, effects.flashAlpha())));
            batch.draw(pixel, mapCamera.originX(), mapCamera.originY(),
                    mapCamera.viewPixelWidth(), mapCamera.viewPixelHeight());
        }
        if (fade > 0f) {
            batch.setColor(0f, 0f, 0f, fade);
            batch.draw(pixel, mapCamera.originX(), mapCamera.originY(),
                    mapCamera.viewPixelWidth(), mapCamera.viewPixelHeight());
        }
        // Command 223: a negative tone component removes that channel, which is
        // exactly the black screen doors use (-255 on all three channels).
        if (tinted) {
            batch.setColor(1f + Math.min(0f, toneRed) / 255f, 1f + Math.min(0f, toneGreen) / 255f,
                    1f + Math.min(0f, toneBlue) / 255f, 1f);
            batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO);
            batch.draw(pixel, mapCamera.originX(), mapCamera.originY(),
                    mapCamera.viewPixelWidth(), mapCamera.viewPixelHeight());
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    /**
     * R6.26: the cave animation hands the screen tone over when its phases
     * change: black/white right after the bands (0169.rb), reset over 8 frames
     * when the sprite starts holding the screen.
     */
    private void updateCaveTransition(float delta) {
        int before = caveTransition.phase();
        caveTransition.update(delta);
        int now = caveTransition.phase();
        if (now > before && now >= 1) {
            float tone = caveTransition.exiting() ? 255f : -255f;
            context.screenEffects().ambientTint(tone, tone, tone, 0f, 0);
        }
        if (now > before && now >= 2) {
            context.screenEffects().ambientTint(0f, 0f, 0f, 0f, 8);
        }
    }

    /** R6.26: the cave bands sit above every other layer (RMXP z = 100000). */
    private void renderCaveTransition() {
        if (!caveTransition.active()) {
            return;
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        caveTransition.render(batch, pixel, mapCamera.originX(), mapCamera.originY(),
                mapCamera.viewPixelWidth(), mapCamera.viewPixelHeight());
        batch.end();
    }

    private static final float ANIMATION_CELL_SIZE = AnimationLayout.CELL_SIZE;

    private final Array<MapAnimations.Active> groundAnimations = new Array<>();
    private int groundAnimationIndex;

    private void prepareGroundAnimations() {
        groundAnimations.clear();
        for (MapAnimations.Active active : mapAnimations.active()) {
            if (AnimationLayout.groundGrass(active)) groundAnimations.add(active);
        }
        groundAnimations.sort((a, b) -> Integer.compare(a.tileY, b.tileY));
        groundAnimationIndex = 0;
    }

    private void renderGroundAnimationsBefore(int depth) {
        while (groundAnimationIndex < groundAnimations.size) {
            MapAnimations.Active active = groundAnimations.get(groundAnimationIndex);
            if (AnimationLayout.grassDepth(active.tileY) >= depth) break;
            renderAnimation(active);
            groundAnimationIndex++;
        }
    }

    /**
     * R6.27: draws the running Show Animation (207) effects. Cells live in the
     * 640x320 animation canvas, anchored to the target character (above the
     * head / centred / below the feet) or to the screen centre.
     */
    private void renderAnimations() {
        if (mapAnimations.isEmpty()) {
            return;
        }
        batch.begin();
        for (MapAnimations.Active active : mapAnimations.active()) {
            if (!AnimationLayout.groundGrass(active)) renderAnimation(active);
        }
        renderTargetFlashes();
        batch.end();
    }

    /** Draws one effect into the current batch, either in depth order or on top. */
    private void renderAnimation(MapAnimations.Active active) {
        AnimationData.Animation animation = active.animation;
        if (animation.frames.length == 0) {
            return;
        }
        Texture sheet = animationSheet(animation);
        if (sheet == null) {
            return;
        }
        AnimationData.Cell[] cells = animation.frames[
                Math.min(animation.frames.length - 1, active.frameIndex())];
        if (cells == null || cells.length == 0) {
            return;
        }
        float[] anchor = animationAnchor(animation, active);
        if (anchor == null) {
            return;
        }
        for (AnimationData.Cell cell : cells) {
            if (!AnimationLayout.hasCell(cell.cell, sheet.getWidth(), sheet.getHeight())) {
                continue;
            }
            float zoom = Math.max(0, cell.zoom) / 100f;
            // The cell's (x, y) is its centre in the animation's own
            // coordinate space, exactly like Essentials' own battle cels
            // (pbCreateCel); RMXP's y points down while ours points up.
            float centerX = anchor[0] + cell.x;
            float centerY = anchor[1] - cell.y;
            float alpha = Math.max(0f, Math.min(1f, cell.opacity / 255f));
            batch.setColor(1f, 1f, 1f, alpha);
            if (cell.blend == 1) {
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            } else if (cell.blend == 2) {
                batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_COLOR);
            }
            batch.draw(sheet, centerX - ANIMATION_CELL_SIZE / 2f,
                    centerY - ANIMATION_CELL_SIZE / 2f,
                    ANIMATION_CELL_SIZE / 2f, ANIMATION_CELL_SIZE / 2f,
                    ANIMATION_CELL_SIZE, ANIMATION_CELL_SIZE, zoom, zoom, -cell.angle,
                    AnimationLayout.sourceX(cell.cell), AnimationLayout.sourceY(cell.cell),
                    AnimationLayout.CELL_SIZE, AnimationLayout.CELL_SIZE, cell.flip, false);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    /**
     * Target flashes (timing scope 1) redraw the character's own sprite in the
     * flash colour, additively - RMXP tints the sprite, not a box around it.
     */
    private void renderTargetFlashes() {
        for (MapAnimations.TargetFlash flash : mapAnimations.flashes()) {
            float alpha = flash.alpha();
            if (alpha <= 0f) {
                continue;
            }
            MapCharacter character = flash.targetId < 0 ? player
                    : eventCharacters == null ? null : eventCharacters.character(flash.targetId);
            if (character == null || character.characterName == null
                    || character.characterName.isEmpty()) {
                continue;
            }
            java.io.File file = locator == null ? null
                    : locator.find("Characters", character.graphicName());
            if (file == null) {
                continue;
            }
            Texture sheet = textures.load("character:" + character.graphicName(), file);
            if (sheet == null) {
                continue;
            }
            int w = sheet.getWidth() / 4;
            int h = sheet.getHeight() / 4;
            float x = character.pixelX() + (TilesetGeometry.TILE_SIZE - w) / 2f;
            float y = character.pixelY();
            batch.setColor(flash.red, flash.green, flash.blue, Math.min(1f, alpha));
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.draw(sheet, x, y, w, h,
                    Math.floorMod(character.pattern(), 4) * w,
                    (character.direction() == 4 ? 1 : character.direction() == 6 ? 2
                            : character.direction() == 8 ? 3 : 0) * h,
                    w, h, false, false);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    /**
     * The origin of the animation's coordinate space in world coordinates.
     * Grass stays at the tile's feet, bubbles at the character's head; other
     * character effects use 0005.rb's sprite-relative positions. Screen effects
     * are resolved first, including when invoked through the tile API.
     */
    private float[] animationAnchor(AnimationData.Animation animation, MapAnimations.Active active) {
        if (animation.position == 3) {
            return new float[] {
                    mapCamera.originX() + mapCamera.viewPixelWidth() / 2f,
                    mapCamera.originY() + mapCamera.viewPixelHeight() / 2f };
        }
        if (active.tileX >= 0) {
            float tile = TilesetGeometry.TILE_SIZE;
            if (AnimationLayout.bubble(animation)) {
                MapCharacter target = player.x() == active.tileX && player.y() == active.tileY ? player : null;
                if (target == null && eventCharacters != null) {
                    for (MapCharacter candidate : eventCharacters.characters()) {
                        if (candidate.x() == active.tileX && candidate.y() == active.tileY) {
                            target = candidate;
                            break;
                        }
                    }
                }
                return new float[] {active.tileX * tile + tile / 2f,
                        AnimationLayout.characterAnchorY(animation,
                                (tileMap.height() - active.tileY - 1) * tile,
                                animationTargetHeight(target, true))};
            }
            return new float[] {active.tileX * tile + tile / 2f,
                    AnimationLayout.tileAnchorY(animation, tileMap.height(), active.tileY)};
        }
        MapCharacter character = active.targetId < 0 ? player
                : eventCharacters == null ? null : eventCharacters.character(active.targetId);
        if (character == null) {
            return null;
        }
        float tile = TilesetGeometry.TILE_SIZE;
        float centerX = character.pixelX() + tile / 2f;
        float spriteHeight = animationTargetHeight(character, AnimationLayout.bubble(animation));
        return new float[] {centerX,
                AnimationLayout.characterAnchorY(animation, character.pixelY(), spriteHeight)};
    }

    private final java.util.Map<String, float[]> animationTargetHeights = new java.util.HashMap<>();

    /** Charset frames can contain large transparent margins; a bubble uses the visible head. */
    private float animationTargetHeight(MapCharacter character, boolean visibleOnly) {
        String name = character == null ? null : character.graphicName();
        if (locator == null || name == null || name.isEmpty()) return TilesetGeometry.TILE_SIZE;
        float[] heights = animationTargetHeights.get(name);
        if (heights == null) {
            java.io.File file = locator.find("Characters", name.endsWith(".png") ? name : name + ".png");
            heights = new float[] {TilesetGeometry.TILE_SIZE, TilesetGeometry.TILE_SIZE};
            if (file != null) {
                Pixmap pixels = new Pixmap(new com.badlogic.gdx.files.FileHandle(file));
                try {
                    int frameHeight = pixels.getHeight() / 4;
                    int top = frameHeight;
                    if (frameHeight > 0) {
                        for (int y = 0; y < pixels.getHeight(); y++) {
                            if (y % frameHeight >= top) continue;
                            for (int x = 0; x < pixels.getWidth(); x++) {
                                if ((pixels.getPixel(x, y) & 255) != 0) {
                                    top = y % frameHeight;
                                    break;
                                }
                            }
                        }
                        heights[0] = frameHeight;
                        heights[1] = top < frameHeight ? frameHeight - top : TilesetGeometry.TILE_SIZE;
                    }
                } finally {
                    pixels.dispose();
                }
            }
            animationTargetHeights.put(name, heights);
        }
        return heights[visibleOnly ? 1 : 0];
    }

    /** Loads one animation sheet from Graphics/Animations, once. */
    private Texture animationSheet(AnimationData.Animation animation) {
        if (animation.graphic.isEmpty()) {
            return null;
        }
        String cachedName = animation.graphic;
        if (cachedName.equals(animationSheetName)) {
            return animationSheet;
        }
        String file = cachedName.endsWith(".png") ? cachedName : cachedName + ".png";
        java.io.File found = locator == null ? null : locator.find("Animations", file);
        Texture texture = found == null ? null
                : textures.load("animation:" + cachedName, found);
        animationSheet = texture;
        animationSheetName = cachedName;
        if (texture == null && animationWarnings.add(-1)) {
            context.game().log("Graphics/Animations/" + file
                    + " is missing; the animation stays empty");
        }
        return texture;
    }

    private Texture animationSheet;
    private String animationSheetName = "";

    /**
     * R6.27: plays one Show Animation (207) effect on a character (-1 = the
     * player) or, for position-3 animations, on the screen. Public so the
     * headless probes can trigger an animation without an event script.
     */
    public void startAnimation(int characterId, int animationId) {
        AnimationData.Animation animation = database.animations().animation(animationId);
        if (Boolean.getBoolean("pokemon.debug.flow")) {
            context.game().log("play animation " + animationId + " on character " + characterId);
        }
        if (animation == null) {
            if (animationWarnings.add(animationId)) {
                context.game().log("Show Animation 207: animation " + animationId
                        + " is not in generated/animations.json (run builder build-data)");
            }
            return;
        }
        mapAnimations.start(animation, characterId);
    }

    /**
     * R6.28: plays an Essentials {@code $scene.spriteset.addUserAnimation}
     * effect at a map tile (the field hook uses it for grass rustles, the
     * project's {@code pbExclaim} for exclamation bubbles). Public so probes
     * can trigger one without an event script.
     */
    public void startTileAnimation(int animationId, int x, int y, int height) {
        AnimationData.Animation animation = database.animations().animation(animationId);
        if (Boolean.getBoolean("pokemon.debug.flow")) {
            context.game().log("play animation " + animationId + " on tile " + x + "," + y
                    + " height " + height);
        }
        if (animation == null) {
            if (animationWarnings.add(animationId)) {
                context.game().log("tile animation " + animationId
                        + " is not in generated/animations.json (run builder build-data)");
            }
            return;
        }
        mapAnimations.startTile(animation, x, y, height);
    }

    /**
     * R6.30: {@code pbExclaim} - plays the bubble at the addressed character's
     * tile and returns how long the running event has to wait. Public so the
     * headless probes can exercise the whole path without an event script.
     */
    public float exclaim(int characterId, int animationId) {
        MapCharacter character = characterId < 0 || eventCharacters == null
                ? player : eventCharacters.character(characterId);
        if (character == null) {
            return 0f;
        }
        startTileAnimation(animationId, character.x(), character.y(), 2);
        return MapAnimations.durationSeconds(database.animations().animation(animationId));
    }

    /**
     * L6c: {@code pbNoticePlayer} plays the "!" in place first; the walk toward
     * the player starts only after that animation has finished. The interpreter
     * keeps waiting for the whole sequence, so the player cannot act during it.
     */
    private int noticeEventId = -1;
    private int noticePlayerDirection;
    private float noticeDelay;

    /**
     * L6: {@code pbNoticePlayer} (Essentials PSystem_Utilities) - the event
     * notices the player: an exclamation unless they already face each other,
     * the player turns toward the event, and the event walks up to the player.
     * The walk runs as a "move toward player" route (R6.32 code 10); the
     * returned wait covers the exclaim plus one step per tile of distance.
     */
    public float noticePlayer(int characterId) {
        MapCharacter character = characterId < 0 || eventCharacters == null
                ? player : eventCharacters.character(characterId);
        if (character == null || character == player) {
            return 0f;
        }
        int playerDirection = directionToward(player, character);
        float walk = noticeWalkSeconds(character);
        if (facingEachOther(character)) {
            // pbNoticePlayer skips the exclamation when both already face each
            // other; the trainer walks over right away.
            startNoticeWalk(characterId, playerDirection);
            return walk;
        }
        // L6c: play the bubble in place, then walk - the walking half is
        // deferred to update() and starts when the animation has finished.
        float animation = exclaim(characterId, 3); // EXCLAMATION_ANIMATION_ID
        noticeEventId = characterId;
        noticePlayerDirection = playerDirection;
        noticeDelay = animation;
        return animation + walk;
    }

    /** Seconds the "walk toward the player" half of pbNoticePlayer needs. */
    private float noticeWalkSeconds(MapCharacter character) {
        int distance = Math.abs(character.x() - player.x()) + Math.abs(character.y() - player.y());
        return Math.max(0, distance - 1) / Math.max(0.1f, character.speed());
    }

    /** L6c: the walking half of pbNoticePlayer (turns the player, routes the event). */
    private void startNoticeWalk(int characterId, int playerDirection) {
        MapCharacter character = characterId < 0 || eventCharacters == null
                ? null : eventCharacters.character(characterId);
        if (character == null || character == player) {
            return;
        }
        player.turn(playerDirection);
        gameState.setPlayerPosition(player.x(), player.y(), player.direction());
        int distance = Math.abs(character.x() - player.x()) + Math.abs(character.y() - player.y());
        int steps = Math.max(0, distance - 1);
        if (steps <= 0) {
            return;
        }
        MoveRoute route = new MoveRoute();
        for (int i = 0; i < steps; i++) {
            MoveRoute.Command step = new MoveRoute.Command();
            step.code = 10; // "move toward player"
            route.commands.add(step);
        }
        eventCharacters.setMoveRoute(characterId, route);
    }

    /** L6c: starts the deferred pbNoticePlayer walk once the "!" is over. */
    private void updateNotice(float delta) {
        if (noticeEventId < 0) {
            return;
        }
        noticeDelay -= delta;
        if (noticeDelay > 0f) {
            return;
        }
        int eventId = noticeEventId;
        int direction = noticePlayerDirection;
        noticeEventId = -1;
        startNoticeWalk(eventId, direction);
    }

    /** Essentials' {@code pbFacingEachOther}: both face the other's tile. */
    private boolean facingEachOther(MapCharacter event) {
        return Collision.targetX(event.x(), event.direction()) == player.x()
                && Collision.targetY(event.y(), event.direction()) == player.y()
                && Collision.targetX(player.x(), player.direction()) == event.x()
                && Collision.targetY(player.y(), player.direction()) == event.y();
    }

    /** {@code pbTurnTowardEvent}'s rule: the dominant axis wins, ties go vertical. */
    static int directionToward(MapCharacter from, MapCharacter to) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? 6 : 4;
        }
        if (dy != 0) {
            return dy > 0 ? 2 : 8;
        }
        return from.direction();
    }

    /**
     * L6: {@code pbSave} - the project's quiet save writes the quick slot, the
     * same file the F5/F9 shortcuts use (stage 2 has no single-save file).
     */
    public boolean saveGame() {
        try {
            context.saveManager().save(context.storage(), "quick", context.gameState());
            return true;
        } catch (RuntimeException error) {
            context.game().log("pbSave failed: " + error.getMessage());
            return false;
        }
    }

    /** PBTerrain::Grass / SootGrass - the tiles the field hook rustles (0168.rb). */
    private static final int RUSTLE_GRASS = 2;
    private static final int RUSTLE_SOOT_GRASS = 14;
    private static final int GRASS_ANIMATION_ID = 1;
    private final com.badlogic.gdx.utils.IntMap<int[]> lastEventTiles =
            new com.badlogic.gdx.utils.IntMap<>();
    private int[] lastPlayerTile = new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE};
    /** P2: the wild encounter roll for step-based battles. */
    private final WildEncounters wildEncounters = new WildEncounters();
    private final java.util.Random encounterRandom = new java.util.Random();

    /**
     * R6.28: the project's {@code Events.onStepTakenFieldMovement} hook: every
     * character that steps onto a Grass / SootGrass tile gets the grass rustle
     * (bridges count, so walking over a bridge above grass stays quiet).
     */
    private void updateGrassRustle() {
        if (eventCharacters == null) {
            return;
        }
        if (lastPlayerTile[0] != player.x() || lastPlayerTile[1] != player.y()) {
            lastPlayerTile[0] = player.x();
            lastPlayerTile[1] = player.y();
            rustleAt(player.x(), player.y());
            checkStepEncounter();
        }
        Array<MapCharacter> characters = eventCharacters.characters();
        for (int i = 0; i < characters.size; i++) {
            MapCharacter character = characters.get(i);
            if (character == null) {
                continue;
            }
            int eventId = mapData.events.get(i).id;
            int[] last = lastEventTiles.get(eventId);
            if (last == null) {
                lastEventTiles.put(eventId, new int[] {character.x(), character.y()});
                continue;
            }
            if (last[0] != character.x() || last[1] != character.y()) {
                last[0] = character.x();
                last[1] = character.y();
                rustleAt(character.x(), character.y());
            }
        }
    }

    private void rustleAt(int x, int y) {
        int tag = tileMap.terrainTag(x, y, true);
        if ((tag == RUSTLE_GRASS || tag == RUSTLE_SOOT_GRASS) && context.mapPort() != null) {
            if (Boolean.getBoolean("pokemon.debug.flow")) {
                context.game().log("grass rustle at " + x + "," + y);
            }
            context.mapPort().showTileAnimation(GRASS_ANIMATION_ID, x, y, 1);
        }
    }

    /**
     * P2: a wild encounter on the tile the player just stepped on. Never while
     * an event or a message is on screen; the roll itself (method, density,
     * safe steps) lives in {@link WildEncounters} so it stays headless-testable.
     */
    private void checkStepEncounter() {
        if (context.battlePort() == null || context.pbsData() == null) {
            return;
        }
        if (interpreter != null && interpreter.running()) {
            return;
        }
        if (context.messageService() != null && context.messageService().visible()) {
            return;
        }
        wildEncounters.onMap(mapData.mapId);
        int tag = tileMap.terrainTag(player.x(), player.y(), true);
        WildEncounters.WildEncounter encounter =
                wildEncounters.roll(context.pbsData(), mapData.mapId, tag, encounterRandom);
        if (encounter == null) {
            return;
        }
        wildEncounters.reset();
        if (Boolean.getBoolean("pokemon.debug.flow")) {
            context.game().log("wild encounter: " + encounter.species + " L" + encounter.level);
        }
        context.battlePort().wildBattle(encounter.species, encounter.level);
    }

    @Override
    public void resize(int width, int height) {
        // R11: FitViewport fits the logical resolution into the window with
        // letterboxing, so any window size or aspect ratio keeps the pixel art
        // unstretched (the camera position is owned by followPlayer()).
        if (viewport != null) {
            viewport.update(width, height, false);
        }
        if (camera != null) {
            camera.update();
        }
    }

    @Override
    public void dispose() {
        pixel.dispose();
        worldTone.dispose();
        if (messageWindow != null) {
            messageWindow.dispose();
        }
        if (pauseMenu != null) {
            pauseMenu.dispose();
            pauseMenu = null;
        }
        batch.dispose();
        textures.dispose();
    }
}
