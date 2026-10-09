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
import pokemon.runtime.field.FieldSteps;
import pokemon.runtime.field.PokemonEncounters;
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
    /** 182_PField_DependentEvents + 297_Follower_Main: the following Pokemon and the other dependent events. */
    private FollowerController followers;
    /** The route the following Pokemon plays during a talk (297_Follower_Config:604-613 {@code followingMoveRoute}). */
    private pokemon.runtime.event.MoveRoutePlayer followerRoute;
    /** A route of the player that only waits (the talk's {@code pbMoveRoute($game_player,[Wait,n])}). */
    private float playerWait;
    private boolean lastBicycle;
    private boolean lastSurfing;
    private boolean lastDiving;
    private final MovementController movement = new MovementController();
    private final MovementController.StepMover stepMover;
    /**
     * Game_Player:9,61-65: a blocked step plays the "Player bump" SE, at most
     * once per 10 frames ({@code @bump_se = 40/4}); it counts down every frame
     * (Game_Player:340).
     */
    private float bumpSe;
    /** True when this frame's step attempt was blocked by collision. */
    private boolean blockedStep;
    /** P0d: the player was mid-jump last frame (landing detection). */
    private boolean playerWasJumping;
    /** PField_Field:1138: the ledge jump's dust plays on the landing frame. */
    private boolean ledgeDustPending;
    /** 311_BW_SignPosts: the board with the map's name, while it is on screen. */
    private LocationSignpost signpost;
    private float signpostClock;
    /** The map the player came from, until the first frame loads the board's graphics (-1 = nothing pending). */
    private int pendingSignpostFrom = -1;
    /** 0 on foot, 1 cycling, 2 surfing: what the player graphic, the speed and the music were last set for. */
    private int vehicleKey;
    /** 179:728-748 {@code $PokemonTemp.surfJump}: the tile the surf base stays on while the player jumps on / off the water. */
    private int[] surfJump;
    /** 023/025:341-346 {@code $PokemonTemp.endSurf}: the jump onto land is over -> dismount. */
    private boolean endSurfPending;
    private final EventInterpreter interpreter;
    private MessageWindow messageWindow;
    /** L1: the pause menu overlay (Modular Pause Menu visuals). */
    private PauseMenuOverlay pauseMenu;
    private pokemon.runtime.battle.BattleScreen battleScreen;
    /** PField_Visuals:123-125: {@code numFrames = 40*4/10}, alphaDiff = ceil(255/16). */
    private static final int BATTLE_RETURN_FRAMES = 40 * 4 / 10;
    private static final int BATTLE_RETURN_ALPHA_STEP =
            (int) Math.ceil(255.0 / BATTLE_RETURN_FRAMES);
    /** The post-battle fade's current alpha (255..0), or -1 when it is not running. */
    private int battleReturnAlpha = -1;

    /**
     * The battle screen this map is currently hosting, or null (probe/debug:
     * the lwjgl3 capture samples the battle's stages through it).
     */
    public pokemon.runtime.battle.BattleScreen activeBattleScreen() {
        return battleScreen;
    }
    /**
     * PField_Visuals:18 pbBattleAnimation / rocket:19: the battle entry
     * animation (a VS screen or the default flash) runs over the map before the
     * battle scene takes over.
     */
    private pokemon.runtime.battle.BattleEntryAnimation battleEntry;
    private boolean battleEntryPlayed;
    /** Graphics.snap_to_bitmap handed to the entry's KGC transition, once. */
    private boolean entrySnapshotTaken;

    /** L6b: Trainer(N)/Counter(N) events of this map (line-of-sight triggers). */
    private final com.badlogic.gdx.utils.Array<SightTriggers.Sight> sightEvents =
            new com.badlogic.gdx.utils.Array<>();
    private final GraphicsLocator locator;
    private PictureLayer pictureLayer;
    private EventCharacters eventCharacters;
    /** P3: the berry plant / moisture sprites of this map (PField_BerryPlants). */
    private BerryPlantSprites berryPlants;
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
    /** PlayerA..PlayerH metadata; the player id is applied from GameState. */
    private pokemon.runtime.data.ProjectInfo.RuntimeProfile runtimeProfile;
    private int appliedPlayerId = Integer.MIN_VALUE;

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
        runtimeProfile = profile;
        if (profile != null) {
            if (profile.hasMessageFont()) {
                messageFontName = profile.messageFont;
            }
        }
        // The player graphic comes from GameState.playerId (pbChangePlayer); a
        // project with PlayerA..PlayerH metadata starts blank until the gender
        // selector picks one.
        applyPlayerCharset();
        mapData = data;
        if (data.region >= 0) gameState.trainer().region = data.region;
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
        int previousMapId = gameState.currentMapId();
        gameState.enterMap(mapId, spawn[0], spawn[1]);
        pendingSignpostFrom = previousMapId;
        gameState.fieldGlobals().outdoorOf = id -> {
            try {
                return database.map(id).outdoor;
            } catch (RuntimeException error) {
                return null;
            }
        };
        gameState.fieldGlobals().safariMapOf = id -> {
            pokemon.runtime.pokemon.PbsData.Metadata meta = database.pbs() == null ? null : database.pbs().mapMetadata(id);
            return meta != null && meta.safariMap;
        };
        if (context.battlePort() instanceof pokemon.runtime.battle.InteractiveBattlePort) {
            // Events.onWildBattleOverride (242_PBattle_Safari:98-107): inside the Safari Zone a wild battle is a Safari battle
            ((pokemon.runtime.battle.InteractiveBattlePort) context.battlePort()).setSafariSource(() ->
                    gameState.fieldGlobals().inSafari(gameState.currentMapId())
                            ? gameState.fieldGlobals().safari.ballcount : -1);
        }
        new pokemon.runtime.field.Vehicles(database.pbs(), gameState).onMapChange(mapId);   // Events.onMapChange (170:603-608)
        noteMapChange(mapId);
        refreshDarkness(mapId);
        flyArrivalBird = gameState.takeFlyArrivalBird();
        if (gameState.takeFlyArrival()) {
            flyArrivalPending = true;                                              // 179:535 pbFlyAnimation(false) once the map has faded in
        }
        // RMXP rebuilds the screen state on map setup: the black tone a door
        // applied before the transfer must not survive into the new map.
        context.screenEffects().clearTone();
        // Scene_Map#transfer_player:70 calls pbBridgeOff: leaving the map drops
        // $PokemonGlobal.bridge, so the next map starts with bridge tiles in
        // their "covers the character" z until its own bridge script fires.
        gameState.bridge(0);
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
            messageWindow.money = () -> context.gameState().trainer().money;
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

            @Override
            public String[] pauseMenuMapItemCheck(String item) {
                return mapItemRefusal(item);
            }

            @Override
            public void pauseMenuSafariQuit() {
                gameState.fieldGlobals().safari.decision = 1;                  // Modular Menu:203-204
                safariGoToStart();
            }
        });
        pictureLayer = new PictureLayer(context.pictureService(), textures, locator);
        context.pictureService().clear(); // RMXP clears pictures when the map changes
        eventCharacters = new EventCharacters(mapData, tileMap, gameState);
        berryPlants = new BerryPlantSprites(mapData, eventCharacters, gameState,
                database == null ? null : database.pbs(), this::startTileAnimation, textures, locator);
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
            public int[] getEventVariable(int eventId) {
                return eventId < 0 || mapData == null ? null
                        : gameState.eventVars().get(mapData.mapId, eventId);
            }

            @Override
            public void setEventVariable(int eventId, int[] value) {
                if (eventId < 0 || mapData == null) {
                    return;
                }
                gameState.eventVars().set(mapData.mapId, eventId, value);
            }

            @Override
            public void turnEvent(int eventId, int direction) {
                if (eventCharacters == null) {
                    return;
                }
                MapCharacter character = eventCharacters.character(eventId);
                if (character != null) {
                    character.face(direction);
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
                }            }

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
            public void eraseEvent(int eventId) {
                // RMXP Erase Event (116): the event leaves the map until it is
                // loaded again - no sprite, no blocking, no trigger.
                if (eventCharacters != null && eventId >= 0) {
                    eventCharacters.erase(eventId);
                }
            }

            @Override
            public void lockEvents(boolean locked) {
                // pbGlobalLock / pbGlobalUnlock (Messages:223-234).
                if (eventCharacters != null) {
                    eventCharacters.setLocked(locked);
                }
            }

            @Override
            public void eraseRoute(int eventId) {
                // pbTrainerEnd: Game_Event#erase_route (PField_Field:804).
                if (eventCharacters != null) {
                    eventCharacters.eraseRoute(eventId);
                }
            }

            @Override
            public int[] triggeredTrainerEvents() {
                return MapScreen.this.triggeredTrainerEvents();
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

            @Override
            public void startSurfing() {
                beginSurfing();
            }

            // 297_Follower_Main: the following Pokemon / dependent events the scripts address.
            @Override
            public boolean toggleFollower(String forced) {
                if (followers == null || followers.follower() == null) {
                    return false;
                }
                followers.toggle(forced, true);
                rebindEntities();
                return true;
            }

            @Override
            public boolean startFollowing(int eventId) {
                if (followers == null || !followers.startFollowing(eventId)) {
                    return false;
                }
                rebindEntities();
                return true;
            }

            @Override
            public void removeDependencies(boolean exceptFollower) {
                if (followers == null) {
                    return;
                }
                if (exceptFollower) {
                    followers.removeAllButFollower();
                } else {
                    followers.removeAll();
                }
                rebindEntities();
            }

            @Override
            public void removeDependency(String name) {
                if (followers == null) {
                    return;
                }
                followers.removeByName(name);
                rebindEntities();
            }

            @Override
            public boolean addDependency(int eventId, String name, int commonEvent) {
                if (followers == null || followers.addEvent(eventId, name, commonEvent) == null) {
                    return false;
                }
                rebindEntities();
                return true;
            }

            @Override
            public FollowerTalkPlan talkToFollower() {
                return MapScreen.this.talkToFollower();
            }

            // 179_PField_FieldMoves: the field side of the hidden moves.
            @Override
            public String facingEventName() {
                pokemon.runtime.data.MapData.EventData event = findFacingEvent();
                return event == null || event.name == null ? null : event.name.toLowerCase();
            }

            @Override
            public int[] facingEventPosition() {
                pokemon.runtime.data.MapData.EventData event = findFacingEvent();
                if (event == null) {
                    return null;
                }
                MapCharacter character = eventCharacters == null ? null : eventCharacters.character(event.id);
                return character == null ? new int[] {event.x, event.y} : new int[] {character.logicalX(), character.logicalY()};
            }

            @Override
            public int facingTerrainTag() {
                return gameState.fieldGlobals().facingTerrainTag;
            }

            @Override
            public int playerTerrainTag() {
                return gameState.fieldGlobals().playerTerrainTag;
            }

            @Override
            public boolean facingPassable() {
                return facingPassableNow();
            }

            @Override
            public boolean fishingFrame(boolean surfing, int pattern) {
                return MapScreen.this.fishingFrame(surfing, pattern);
            }

            @Override
            public void endFishingFrame(int oldPattern) {
                MapScreen.this.endFishingFrame(oldPattern);
            }

            @Override
            public int playerFullPattern() {
                return fullPattern(player);
            }

            @Override
            public boolean currentMapOutdoor() {
                return mapData != null && Boolean.TRUE.equals(mapData.outdoor);
            }

            @Override
            public float flyAnimation(boolean departure, String birdSpecies) {
                return MapScreen.this.flyAnimation(departure, birdSpecies);
            }

            @Override
            public boolean hasDependentEvents() {
                return followers != null && followers.hasDependentEvents();
            }

            @Override
            public int terrainTagOnMap(int mapId) {
                return MapScreen.this.terrainTagOnMap(mapId);
            }

            @Override
            public float smashFacingEvent() {
                return MapScreen.this.smashFacingEvent();
            }

            @Override
            public String eventName(int eventId) {
                pokemon.runtime.data.MapData.EventData event = eventById(eventId);
                return event == null ? null : event.name;
            }

            @Override
            public float smashEvent(int eventId) {
                return MapScreen.this.smashEvent(eventById(eventId));
            }

            @Override
            public float hiddenMoveAnimation(pokemon.runtime.pokemon.Pokemon pokemon) {
                return MapScreen.this.hiddenMoveAnimation(pokemon);
            }

            @Override
            public float ascendWaterfall() {
                return MapScreen.this.ascendWaterfall();
            }

            @Override
            public float sweetScentFlash() {
                return MapScreen.this.sweetScentFlash();
            }

            @Override
            public float flyAnimation(boolean departure) {
                return MapScreen.this.flyAnimation(departure);
            }

            @Override
            public boolean darknessActive() {
                return darkness != null;
            }

            @Override
            public float flashDarkness() {
                return MapScreen.this.flashDarkness();
            }

            /** R6.30: toggle_liefeng_switches - the floating-plate puzzle maps. */
            @Override
            public void togglePlateSwitches() {
                // The plugin reads $game_player.x/.y, which during a step is the
                // destination tile (Game_Character#move_generic sets @x/@y when
                // the step starts), so the plate's page flips while the player
                // is still walking onto it - that is what makes the landing
                // Player Touch check below fire on the pressed page.
                FieldInteractions.toggleFloatPlates(gameState, mapData,
                        player.logicalX(), player.logicalY());
            }
        });
        player = new MapCharacter(spawn[0], spawn[1],
                tileMap.width(), tileMap.height(), playerCharacter);
        player.isPlayer = true;
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
        followers = new FollowerController(gameState, tileMap, mapData, player, followerHost());
        followers.bind(tileMap, mapData, true);
        bindEntities(baseEntities(), locator);
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
        context.sampleInput();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (tileMap == null) {
            return;
        }
        // R11: the letterboxed viewport of this frame (the clear above already
        // painted the bars black).
        viewport.apply();
        if (context.battlePort() instanceof pokemon.runtime.battle.InteractiveBattlePort
                && context.battlePort().pending()) {
            pokemon.runtime.battle.InteractiveBattlePort port =
                    (pokemon.runtime.battle.InteractiveBattlePort) context.battlePort();
            if (battleScreen == null && battleEntry == null && !battleEntryPlayed) {
                // PField_Visuals:44-57: 0 outside, 1 inside, 2 cave, 3 water. The
                // runtime only knows the metadata Outdoor flag, so it uses 0/1.
                pokemon.runtime.data.MapData map = context.database() == null
                        ? null : context.database().map(gameState.currentMapId());
                int location = map == null || map.outdoor == null || map.outdoor ? 0 : 1;
                entrySnapshotTaken = false;
                beginBattleAudio(port.session());
                battleEntry = new pokemon.runtime.battle.BattleEntryAnimation(context, port.session(), location);
                return; // first frame of the entry
            }
            if (battleEntry != null) {
                battleEntry.update(delta);
                if (battleEntry.finished()) {
                    battleEntry.dispose();
                    battleEntry = null;
                    battleEntryPlayed = true;
                    entrySnapshotTaken = false;
                }
                // The world keeps drawing below the entry overlay this frame.
            } else {
                if (battleScreen == null) battleScreen = new pokemon.runtime.battle.BattleScreen(context,
                        (pokemon.runtime.battle.InteractiveBattlePort) context.battlePort());
                battleScreen.render(delta);
                if (battleScreen.finished()) {
                    battleScreen.dispose();
                    battleScreen = null;
                    battleEntryPlayed = false; // the next battle plays its entry again
                    endBattleAudio();
                    // PField_Visuals:122: viewport.color = Color.new(0,0,0,255) -
                    // the scene's last frame was already black
                    // (PokeBattle_Scene:301 pbFadeOutAndHide), so the map comes
                    // back on black and lifts over the next 16 frames (:123-130).
                    // 174_PField_Battles:329-335 + 656-657: pbAfterBattle (the white-out's pbStartOver) runs inside
                    // pbBattleAnimation, before this fade back - so a white-out keeps the screen black for its lines
                    // and the transfer, and the new map is what fades in.
                    battleReturnAlpha = interpreter != null && interpreter.running() ? 0 : 255;
                    afterSafariBattle(port);
                    if (followers != null) {
                        followers.comeBack(false);                           // 297_Follower_Main:645 callRefresh after a battle
                        rebindEntities();
                    }
                }
                return;
            }
        }
        pokerusDailyCheck();
        startQueuedHatch();
        if (pauseMenu != null && !pauseMenu.isOpen() && context.menuService().pending() != null) {
            capturePauseMap();
            pauseMenu.openRequest(context.menuService().pending());
            renderPauseMenu();
            return;
        }
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
            // frame, but the same press must not reopen it or reach a waiting
            // event message in this frame (RMXP's Input.update runs per scene).
            menuHandled = true;
            pokemon.runtime.pokemon.Pokemon[] hiddenPokemon = new pokemon.runtime.pokemon.Pokemon[1];
            String hiddenMove = pauseMenu.takeHiddenMove(hiddenPokemon);
            if (hiddenMove != null && interpreter != null) {
                interpreter.startHiddenMove(hiddenPokemon[0], hiddenMove);   // 206_PScreen_PauseMenu:183 pbUseHiddenMove
            }
            String fieldItem = pauseMenu.takeFieldItem();
            if (fieldItem != null && interpreter != null) {
                interpreter.startFieldItem(fieldItem);                       // 288_Modular_Menu:102-118 pbUseKeyItemInField
            }
            if (followers != null) {
                followers.comeBack(false);                                   // 297_Follower_Main:579-638 after the party / bag screens
                rebindEntities();
            }
            context.inputManager().consumePressed();
        }
        if (!menuHandled && pauseMenu != null && context.inputManager().wasPressed(GameAction.MENU)
                && !context.messageService().visible()
                && !(interpreter != null && interpreter.running())
                && !context.transferPending()) {
            capturePauseMap();
            pauseMenu.open();
            renderPauseMenu();
            return;
        }
        context.pictureService().update(delta);
        context.screenEffects().update(delta);
        context.gameState().weather().update(delta);
        updateCaveTransition(delta);
        mapAnimations.update(delta, animationTimingSink);
        updateNotice(delta); // L6c: deferred second half of pbNoticePlayer
        if (eventCharacters != null && routeContext != null) {
            eventCharacters.update(delta, routeContext);
        }
        updateFollowers(delta, true);
        if (interpreter != null) {
            interpreter.itemToasts().update(delta);      // 308_ItemFindSimple_Scene:78-88: the boxes expire every frame, event or not
        }
        updateScheduledErase(delta);
        updateFlyBird(delta);
        updateDarkness(delta);
        updateHiddenMove(delta);
        if (eventCharacters != null && gameState.version() != entityVersion) {
            // A switch flipped a page: pick up the new sheet and rebind entities.
            entityVersion = gameState.version();
            eventCharacters.refreshGraphics();
            bindEntities(baseEntities(), locator);
        } else if (routeContext != null && routeContext.takeGraphicsDirty()) {
            // R6.18: move route code 41 changed a character sheet (the nurse
            // swapping BW 071 / BW 072, for example); reload its sprites.
            bindEntities(baseEntities(), locator);
        }
        // P3: BerryPlantSprite#update - runs after the page refresh so a
        // switch-driven page change cannot clobber the plant's sheet.
        if (berryPlants != null && berryPlants.update()) {
            bindEntities(baseEntities(), locator);
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
        boolean scriptDriven = playerRoute != null || context.transferPending() || playerWait > 0f
                || battleEntry != null
                || (interpreter != null && interpreter.running());
        if (safariGoToStartAfterMessages && !(interpreter != null && interpreter.running())
                && !context.messageService().visible()) {
            if (pendingSafariMessages != null && !pendingSafariMessages.isEmpty()) {
                java.util.List<String> lines = pendingSafariMessages;
                pendingSafariMessages = null;
                startMessages(lines);                                         // the messages come first, then the way back
            } else {
                safariGoToStartAfterMessages = false;
                pendingSafariMessages = null;
                safariGoToStart();                                            // pbSafariState.pbGoToStart
            }
        }
        if (deferredWild != null && !(interpreter != null && interpreter.running())
                && !context.messageService().visible()) {
            // The step's messages are over: now the wild battle they preceded (PField_Field:488-516 order).
            java.util.List<PokemonEncounters.Encounter> wild = deferredWild;
            deferredWild = null;
            startWildBattle(wild);
        }
        if (interpreter != null && interpreter.running()) {
            // An event is waiting for input / time: the player stands still (RMXP).
            interpreter.update(delta);
            if (interpreter.consumeTitleRequest()) {     // 049_Scene_Map:171-174 $game_temp.to_title
                backToTitle();
                return;
            }
        } else if (context.messageService().visible()) {
            // The running event ended or was cut short while its window was up.
            context.messageService().close();
        } else if (!sightQueue.isEmpty() && !scriptDriven) {
            startQueuedSight();                         // the next spotted trainer event starts once the last one is done
        } else if (!scriptDriven) {
            // Controller advances interpolation too; never update the player twice per frame.
            int beforeX = player.x();
            int beforeY = player.y();
            blockedStep = false;
            movement.runStyle = context.settings().runstyle;
            movement.update(context.inputManager(), delta, player, stepMover);
            // P0d: a jump's tile changed when it started; its step triggers
            // run on the landing frame (Game_Character:833 requires !jumping?).
            boolean playerLanded = playerWasJumping && !player.isJumping();
            gameState.setPlayerPosition(player.x(), player.y(), player.direction());
            if (interpreter != null && !player.isJumping()
                    && context.inputManager().wasPressed(GameAction.CONFIRM)) {
                startFacingEvent();
            } else if (interpreter != null && !interpreter.running() && !player.isJumping()) {
                if (player.x() != beforeX || player.y() != beforeY || playerLanded) {
                    // RMXP checks Player Touch (1) and Event Touch (2) together,
                    // and only on the frame the player stepped onto the tile.
                    startOwnTileTouch();                     // Player Touch: arrived
                    checkEventTouch();                       // Event Touch: same tile
                    checkSightTriggers();                    // L6b: Trainer(N)/Counter(N) sight
                } else {
                    boolean touched = startBlockedTouchEvent(); // Player Touch: walked into it
                    // Game_Player#move_generic:84-88: only a step blocked by an
                    // obstacle without a touch event plays the bump SE.
                    if (blockedStep && !touched) {
                        playBumpSe();
                    }
                    // A page that turned Player Touch after the landing check
                    // (the floating plates) still has to run - see the method.
                    if (!touched) {
                        startOwnTileTouch();
                    }
                }
            }
        }
        if (scriptDriven && player.isMoving()) {
            player.advance(delta);
            gameState.setPlayerPosition(player.x(), player.y(), player.direction());
        }
        playerWasJumping = player.isJumping();
        updateFollowers(delta, false);
        if (ledgeDustPending && !player.isJumping()) {
            // PField_Field:1138: pbLedge's dust, on the frame the jump landed.
            ledgeDustPending = false;
            if (context.mapPort() != null) {
                context.mapPort().showTileAnimation(DUST_ANIMATION_ID, player.x(), player.y(), 1);
            }
        }
        // R6.23: a step that left the map edge lands in the connected
        // neighbour (PokemonMapFactory#setCurrentMap).
        updateMapConnection();
        if (gameState.playerId() != appliedPlayerId) {
            applyPlayerCharset(); // pbChangePlayer changed the walking graphic
        }
        updateVehicle();
        updateGrassRustle(); // R6.28: Essentials field-movement rustle
        if (bumpSe > 0f) {
            bumpSe = Math.max(0f, bumpSe - delta); // Game_Player:340
        }
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
        if (berryPlants != null) {
            berryPlants.beginRender();
        }
        renderer.render(batch, mapCamera, this::renderWorldDepth);
        renderAnimations(); // R6.27: Show Animation (207) sits on the toned map
        renderHeadNames();
        batch.setShader(null);
        renderFog();
        renderPictures();
        renderDarkness();
        renderSweetScent(delta);
        renderFlyBird();
        renderHiddenMove();
        renderSignpost();
        renderKeyItem();
        // A script tone (223) tints the map viewport only, never the windows above it (the chapter cards of map 37 write white
        // text on a toned-black screen), so the message window is drawn after the overlay then too.
        ScreenEffects toneEffects = context.screenEffects();
        boolean scriptTone = toneEffects.toneFromScript()
                && (toneEffects.toneRed() < 0f || toneEffects.toneGreen() < 0f || toneEffects.toneBlue() < 0f);
        boolean blackBackdrop = toneEffects.fade() >= 250f || scriptTone;
        if (!blackBackdrop) {
            renderMessageWindow();
        }
        renderItemToasts();
        renderScreenEffects();
        if (blackBackdrop) {
            renderMessageWindow();                                                 // the window sits above the black screen (the white-out lines)
        }
        renderBattleReturnFade();
        renderCaveTransition();
        if (battleEntry != null) {
            // PField_Visuals:18 pbBattleAnimation / rocket:19: the entry runs in
            // a screen-space viewport above the map.
            if (battleEntry.needsTransitionSnapshot() && !entrySnapshotTaken) {
                captureEntrySnapshot();
            }
            float savedX = camera.position.x;
            float savedY = camera.position.y;
            camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 0f);
            camera.update();
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            battleEntry.render(batch, viewWidth, viewHeight);
            batch.end();
            camera.position.set(savedX, savedY, 0f);
            camera.update();
            return;
        }
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
     *
     * <p>A transfer that keeps the player on the map they are already standing
     * on must NOT rebuild the screen. Essentials' {@code Scene_Map#transfer_player}
     * only calls {@code $MapFactory.setup} when the map id changes
     * ({@code 0047.rb:71-73}); rebuilding would drop the running event's
     * interpreter and restart the still-active autorun page from its first
     * command, which loops forever on map6/event29 page 2 (user report
     * 2026-10-05).</p>
     */
    private void switchMap(RuntimeContext.Transfer transfer) {
        // Scene_Map#transfer_player: pbCancelVehicles($game_temp.player_new_map_id) - surfing ends, the bicycle stays where allowed.
        if (!transfer.keepVehicles) {
            new pokemon.runtime.field.Vehicles(context.pbsData(), gameState).cancelVehicles(transfer.mapId);
        }
        if (mapData != null && transferStaysInMap(mapData.mapId, transfer.mapId)) {
            transferWithinMap(transfer);
            return;
        }
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

    /**
     * Essentials' {@code Scene_Map#transfer_player} sets the map up only when
     * the map id actually changes ({@code 0047.rb:71-73}); a Transfer Player
     * inside the current map keeps the map and its running event alive.
     */
    public static boolean transferStaysInMap(int currentMapId, int targetMapId) {
        return targetMapId == currentMapId;
    }

    /**
     * In-map Transfer Player (201): the hero jumps to the target tile while the
     * map and its event state stay untouched. RMXP's {@code Game_Player#moveto}
     * is not a step, so the destination must not roll an encounter or rustle the
     * grass, and {@code command_201} leaves the event running - the interpreter
     * resumes with the command after the transfer.
     */
    private void transferWithinMap(RuntimeContext.Transfer transfer) {
        if (transfer.fade != 0) {
            // The frame before the swap already faded the screen to black
            // (update's transfer fade); a rebuilt screen fades back in, so the
            // in-place path has to do the same instead of staying black.
            context.screenEffects().fade(12, false);
        }
        player.teleport(transfer.x, transfer.y);
        if (transfer.direction != 0) {
            player.face(transfer.direction);
        }
        gameState.setPlayerPosition(player.x(), player.y(), player.direction());
        // Scene_Map#transfer_player:70: every transfer (same map included)
        // starts with pbBridgeOff.
        gameState.bridge(0);
        // Not a walked step: suppress the grass rustle / encounter roll that
        // updateGrassRustle would otherwise see on the next frame.
        lastPlayerTile = new int[] {player.x(), player.y()};
        // A teleport is not a walk, so RMXP's player-touch check (which runs
        // only on a stepped frame) must not fire at the destination: the map35
        // portal pairs teleport onto each other and would bounce forever.
        latchOwnTouchTile();
        cameraScroll.reset();
        followPlayer();
        if (interpreter != null) {
            interpreter.resumeAfterTransfer();
        }
    }

    /**
     * Marks the player's current tile as already player-touch checked. A
     * teleport (201 / 202 on the player) is not a step, so the page under the
     * hero must not start until the player walks off and back on.
     */
    private void latchOwnTouchTile() {
        if (player == null || mapData == null || gameState == null) {
            return;
        }
        ownTouchTileX = player.x();
        ownTouchTileY = player.y();
        MapData.EventPageData page = EventTriggers.pageAt(gameState, mapData, ownTouchTileX, ownTouchTileY,
                EventTriggers.PLAYER_TOUCH);
        ownTouchEventId = page == null ? -1
                : EventTriggers.eventIdAt(gameState, mapData, ownTouchTileX, ownTouchTileY,
                        EventTriggers.PLAYER_TOUCH);
        ownTouchPage = page == null ? -1 : page.page;
    }

    /** Starts the action-trigger page (trigger 0) of the event the player faces. */
    private void startFacingEvent() {
        int x = Collision.targetX(player.x(), player.direction());
        int y = Collision.targetY(player.y(), player.direction());
        if (startFacingDependent(x, y)) {
            return;                                                        // the follower / the partner talks first
        }
        facePlayerWhenTalkedTo(x, y);
        if (startEvent(x, y, EventTriggers.ACTION)) {
            return;
        }
        if (offerSurf(x, y)) {
            return;
        }
        if (offerWaterMoves()) {
            return;
        }
        // 025_Game_Player:296-311 check_event_trigger_there: nothing to start on the tile in front and
        // that tile is a counter -> look one tile further (the clerk / nurse behind a counter).
        if (tileMap != null && tileMap.counter(x, y)) {
            int farX = Collision.targetX(x, player.direction());
            int farY = Collision.targetY(y, player.direction());
            facePlayerWhenTalkedTo(farX, farY);
            startEvent(farX, farY, EventTriggers.ACTION);
        }
    }

    /**
     * {@code Events.onAction} for Surf (179_PField_FieldMoves:765-772): pressing the action key at the water's edge.
     * The checks that need the map are here; the question, the badge and the lines are the {@code pbSurf} condition atom.
     * 登记: {@code pbFacingEvent} (a non-triggering event on the water tile) and the dependent events.
     */
    private boolean offerSurf(int x, int y) {
        pokemon.runtime.state.FieldGlobals g = gameState.fieldGlobals();
        if (g.surfing || interpreter == null || interpreter.running() || !tileMap.valid(x, y)) {
            return false;                                                  // :766
        }
        if (new pokemon.runtime.field.Vehicles(context.pbsData(), gameState).bicycleAlways(mapData.mapId)) {
            return false;                                                  // :767
        }
        if (!pokemon.runtime.field.PBTerrain.isSurfable(tileMap.terrainTag(x, y, false, gameState.bridge()))) {
            return false;                                                  // :768
        }
        int bit = player.direction() == 2 ? 1 : player.direction() == 4 ? 2 : player.direction() == 6 ? 4 : 8;
        if (!tileMap.playerPassable(player.x(), player.y(), bit, gameState.bridge(), false, g.bicycle)) {
            return false;                                                  // :769 $game_map.passable?
        }
        Array<pokemon.runtime.data.EventCommand> list = new Array<>();
        list.add(syntheticCommand(0, 111, 12, "pbSurf"));
        list.add(syntheticCommand(1, 412));
        list.add(syntheticCommand(2, 0));
        interpreter.start(list, mapData.mapId, -1);
        return true;
    }

    /**
     * {@code Events.onAction} of the water moves (179_PField_FieldMoves:380-399 Dive / Surfacing, :974-981 Waterfall): the
     * action key on deep water, or facing a waterfall. The questions are the {@code pbDive} / {@code pbSurfacing} /
     * {@code pbWaterfall} atoms of the script conditions.
     */
    private boolean offerWaterMoves() {
        if (interpreter == null || interpreter.running()) {
            return false;
        }
        pokemon.runtime.state.FieldGlobals g = gameState.fieldGlobals();
        String atom = null;
        if (g.diving) {                                                            // :381
            if (pokemon.runtime.field.HiddenMoves.DIVING_SURFACE_ANYWHERE) {
                atom = "pbSurfacing";                                              // :382-383
            } else {
                int divemap = pokemon.runtime.field.HiddenMoves.divemapFor(context.pbsData(), mapData.mapId);   // :385-391
                if (divemap >= 0 && pokemon.runtime.field.PBTerrain.isDeepWater(terrainTagOnMap(divemap))) {
                    atom = "pbSurfacing";                                          // :392-394
                }
            }
        } else if (pokemon.runtime.field.PBTerrain.isDeepWater(g.playerTerrainTag)) {
            atom = "pbDive";                                                       // :397
        }
        if (atom == null) {
            if (g.facingTerrainTag == pokemon.runtime.field.PBTerrain.WATERFALL) {            // :976
                atom = "pbWaterfall";
            } else if (g.facingTerrainTag == pokemon.runtime.field.PBTerrain.WATERFALL_CREST) {   // :978
                Array<pokemon.runtime.data.EventCommand> text = new Array<>();
                text.add(syntheticCommand(0, 101, "伴随着震耳欲聋的轰鸣声，\\n庞大的瀑布倾泻而下。"));
                text.add(syntheticCommand(1, 0));
                interpreter.start(text, mapData.mapId, -1);
                return true;
            }
        }
        if (atom == null) {
            return false;
        }
        Array<pokemon.runtime.data.EventCommand> list = new Array<>();
        list.add(syntheticCommand(0, 111, 12, atom));
        list.add(syntheticCommand(1, 412));
        list.add(syntheticCommand(2, 0));
        interpreter.start(list, mapData.mapId, -1);
        return true;
    }

    private static pokemon.runtime.data.EventCommand syntheticCommand(int index, int code, Object... values) {
        pokemon.runtime.data.EventCommand command = new pokemon.runtime.data.EventCommand();
        command.index = index;
        command.code = code;
        command.parameters = new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.array);
        for (Object value : values) {
            command.parameters.addChild(value instanceof Integer
                    ? new com.badlogic.gdx.utils.JsonValue((long) (Integer) value)
                    : new com.badlogic.gdx.utils.JsonValue(String.valueOf(value)));
        }
        return command;
    }

    /**
     * {@code pbStartSurfing} (179:723-732): the surf flag, the graphic (the frame watcher) and the one-tile jump onto the
     * water, the surf base staying on the water tile meanwhile.
     */
    private void beginSurfing() {
        if (pokemonEncounters != null) {
            pokemonEncounters.clearStepCount();                            // :725
        }
        gameState.fieldGlobals().surfing = true;                           // :726
        int fx = Collision.targetX(player.x(), player.direction());
        int fy = Collision.targetY(player.y(), player.direction());
        surfJump = new int[] {fx, fy};                                     // :728
        applyPlayerCharset();                                              // :727 pbUpdateVehicle
        vehicleKey = 2;
        player.startJump(fx, fy, TilesetGeometry.TILE_SIZE * 3f / 8f);     // :729 pbJumpToward (distance 1, no sound)
        vehicleMusic(0, 2);
    }

    /** {@code pbEndSurf} (179:734-752): leaving the water for a land tile jumps onto it, then the player dismounts. */
    private boolean endSurfStep(MapCharacter character, int direction, int x, int y) {
        pokemon.runtime.state.FieldGlobals g = gameState.fieldGlobals();
        if (!g.surfing) {
            return false;                                                  // :735
        }
        int current = tileMap.terrainTag(character.x(), character.y(), false, gameState.bridge());
        int facing = tileMap.terrainTag(x, y, false, gameState.bridge());
        if (!pokemon.runtime.field.PBTerrain.isSurfable(current) || pokemon.runtime.field.PBTerrain.isSurfable(facing)) {
            return false;                                                  // :740
        }
        surfJump = new int[] {character.x(), character.y()};               // :741
        character.face(direction);
        character.startJump(x, y, TilesetGeometry.TILE_SIZE * 3f / 8f);    // :742 pbJumpToward(1, false, true)
        if (pokemonEncounters != null) {
            pokemonEncounters.clearStepCount();                            // pbJumpToward:1218
        }
        endSurfPending = true;                                             // pbJumpToward:1219
        return true;
    }

    /** The per-frame vehicle bookkeeping: terrain tags for the item handlers, the graphic, the speed, the end of a surf. */
    private void updateVehicle() {
        pokemon.runtime.state.FieldGlobals g = gameState.fieldGlobals();
        g.playerTerrainTag = tileMap.valid(player.x(), player.y())
                ? tileMap.terrainTag(player.x(), player.y(), false, gameState.bridge()) : 0;
        int fx = Collision.targetX(player.x(), player.direction());
        int fy = Collision.targetY(player.y(), player.direction());
        g.facingTerrainTag = tileMap.valid(fx, fy) ? tileMap.terrainTag(fx, fy, false, gameState.bridge()) : 0;
        int key = g.surfing ? 2 : g.bicycle ? 1 : 0;
        if (key != vehicleKey) {
            int old = vehicleKey;
            vehicleKey = key;
            applyPlayerCharset();
            vehicleMusic(old, key);
        }
        movement.vehicleSpeedLevel = key == 1 ? 5 : key == 2 ? 4 : 0;       // 026_Game_Player_Visuals:59-62
        movement.runAllowed = g.runningShoes && !g.diving && !g.surfing && !g.bicycle     // :28-31 pbCanRun?
                && !pokemon.runtime.field.PBTerrain.onlyWalk(g.playerTerrainTag);
        if (endSurfPending && !player.isJumping()) {                        // 025_Game_Player:341-346
            endSurfPending = false;
            surfJump = null;
            new pokemon.runtime.field.Vehicles(context.pbsData(), gameState).cancelVehicles(null);
            autoplayMapAudio();                                            // :744 $game_map.autoplayAsCue
        }
        if (surfJump != null && !endSurfPending && !player.isJumping()) {
            surfJump = null;
        }
        if (renderer != null) {
            renderer.surfJumpTile = surfJump;
        }
    }

    /** pbMountBike / pbStartSurfing cue the vehicle's music; getting off goes back to the map's (pbDismountBike, :744). */
    private void vehicleMusic(int from, int to) {
        pokemon.runtime.pokemon.PbsData.Metadata global = context.pbsData() == null ? null : context.pbsData().globalMetadata();
        String name = global == null ? null : to == 2 ? global.surfBGM : to == 1 ? global.bicycleBGM : null;
        pokemon.runtime.audio.BattleMusic.Track track = pokemon.runtime.audio.BattleMusic.resolve(name);
        if (to != 0 && track != null && track.playable()) {
            context.audioManager().cueBgm(track.name, track.volume, track.pitch, MAP_BGM_CUE_SECONDS);
        } else if (to == 0 && from != 0) {
            autoplayMapAudio();
        }
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

    /**
     * Player Touch (trigger 1) on the tile the player stands on:
     * {@code Game_Player#update_event_triggering}:404
     * {@code check_event_trigger_here([1,2])}.
     *
     * <p>RMXP runs that check on the landing frame and reads
     * {@code $game_player.x}, which already holds the destination while the step
     * runs, so a page that a parallel event flips during the step is active by
     * the time the check runs. This port resolves the player's tile when the
     * step lands, so the same page can turn Player Touch one or more frames
     * later (the floating plates do - see {@code toggle_liefeng_switches}).
     * Re-running the check while the player stands there closes that gap; the
     * latch keeps it to one start per event + page, so a page that stays Player
     * Touch under the player is not restarted every frame.</p>
     *
     * @return true when a page started
     */
    private boolean startOwnTileTouch() {
        if (interpreter == null || interpreter.running()) {
            return false;
        }
        int x = player.x();
        int y = player.y();
        if (x != ownTouchTileX || y != ownTouchTileY) {
            // A new tile: nothing has started here yet. (Reset by tile rather
            // than by the landing branch so a teleport onto a page cannot be
            // suppressed by a latch from the tile the player came from.)
            ownTouchTileX = x;
            ownTouchTileY = y;
            ownTouchEventId = -1;
            ownTouchPage = -1;
        }
        MapData.EventPageData page = EventTriggers.pageAt(gameState, mapData, x, y,
                EventTriggers.PLAYER_TOUCH);
        if (page == null) {
            return false;
        }
        int id = EventTriggers.eventIdAt(gameState, mapData, x, y, EventTriggers.PLAYER_TOUCH);
        if (id == ownTouchEventId && page.page == ownTouchPage) {
            return false;
        }
        ownTouchEventId = id;
        ownTouchPage = page.page;
        arrivalDoorPage = false;
        interpreter.start(page.commands, mapData.mapId, id);
        return true;
    }

    /** Player Touch page already started at the player's current tile. */
    private int ownTouchTileX = Integer.MIN_VALUE;
    private int ownTouchTileY = Integer.MIN_VALUE;
    private int ownTouchEventId = -1;
    private int ownTouchPage = -1;

    /**
     * Player Touch when the destination is blocked by the event itself: RMXP
     * doors are walked into, so a held direction towards the event starts it.
     *
     * @return true when an event started; then the bump SE stays silent
     *         (Game_Player#move_generic:84-88)
     */
    private boolean startBlockedTouchEvent() {
        var input = context.inputManager();
        if (input.isDown(GameAction.UP) && startBlockedTouch(player.x(), player.y() - 1)) return true;
        if (input.isDown(GameAction.DOWN) && startBlockedTouch(player.x(), player.y() + 1)) return true;
        if (input.isDown(GameAction.LEFT) && startBlockedTouch(player.x() - 1, player.y())) return true;
        if (input.isDown(GameAction.RIGHT)) return startBlockedTouch(player.x() + 1, player.y());
        return false;
    }

    /**
     * {@code check_event_trigger_touch} (Game_Player:305-327) starts both
     * Player Touch (1) and Event Touch (2) pages on the blocked tile.
     */
    private boolean startBlockedTouch(int x, int y) {
        return startEvent(x, y, EventTriggers.PLAYER_TOUCH)
                || startEvent(x, y, EventTriggers.EVENT_TOUCH);
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
        // Game_Player:140-148 pbCheckEventTriggerFromDistance starts every spotting event; they run one after another
        // (the first of two trainers can wait for the second one, PField_Battles:534-558).
        sightQueue.clear();
        for (SightTriggers.Sight sight : sightEvents) {
            if (sightSpotsPlayer(sight)) {
                sightQueue.add(sight.eventId);
            }
        }
        startQueuedSight();
    }

    /** The events of {@link #checkSightTriggers()} that have not started yet. */
    private final java.util.ArrayDeque<Integer> sightQueue = new java.util.ArrayDeque<>();

    /** One sight event sees the player now and its current page is an Event Touch page. */
    private boolean sightSpotsPlayer(SightTriggers.Sight sight) {
        MapCharacter character = eventCharacters.character(sight.eventId);
        if (character == null) {
            return false;
        }
        int steps = SightTriggers.lineSteps(character.x(), character.y(), character.direction(),
                player.x(), player.y(), sight.distance);
        if (steps < 0) {
            return false;
        }
        if (sight.kind == SightTriggers.Kind.TRAINER && !lineOfSightClear(character, steps)) {
            return false;
        }
        return EventTriggers.eventIdAt(gameState, mapData, character.x(), character.y(),
                EventTriggers.EVENT_TOUCH) == sight.eventId;
    }

    /** Starts the next queued sight event whose page still starts (the shared interpreter runs one page at a time). */
    private void startQueuedSight() {
        while (!sightQueue.isEmpty()) {
            int id = sightQueue.poll();
            MapCharacter character = eventCharacters == null ? null : eventCharacters.character(id);
            if (character == null) {
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

    /** {@code pbTriggeredTrainerEvents([2],false)} (Game_Player:104-119): the Trainer(N) events that see the player. */
    int[] triggeredTrainerEvents() {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        if (eventCharacters != null) {
            for (SightTriggers.Sight sight : sightEvents) {
                if (sight.kind == SightTriggers.Kind.TRAINER && sightSpotsPlayer(sight)) {
                    ids.add(sight.eventId);
                }
            }
        }
        int[] ret = new int[ids.size()];
        for (int i = 0; i < ret.length; i++) ret[i] = ids.get(i);
        return ret;
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
    // ------------------------------------------------------------------
    // 182_PField_DependentEvents / 297_Follower_Main: the following Pokemon
    // ------------------------------------------------------------------

    private FollowerController.Host followerHost() {
        return new FollowerController.Host() {
            @Override
            public void playAnimation(int animationId, int x, int y) {
                startTileAnimation(animationId, x, y, 3);                          // addUserAnimation(id, x, y)
            }

            @Override
            public String mapName() {
                return mapData == null || mapData.name == null ? "" : mapData.name;
            }

            @Override
            public boolean outdoor() {
                return mapData != null && Boolean.TRUE.equals(mapData.outdoor);
            }

            @Override
            public boolean encounterPossible() {
                return pokemonEncounters != null && pokemonEncounters.isEncounterPossibleHere(playerTerrainTag());
            }

            @Override
            public int playerTerrainTag() {
                return MapScreen.this.playerTerrainTag();
            }

            @Override
            public boolean characterExists(String path) {
                return locator != null && locator.find("Characters", path + ".png") != null;
            }

            @Override
            public void eraseEvent(int eventId) {
                if (eventCharacters != null) {
                    eventCharacters.erase(eventId);
                }
            }

            @Override
            public String eventGraphic(int eventId) {
                MapCharacter character = eventCharacters == null ? null : eventCharacters.character(eventId);
                return character == null ? null : character.characterName;
            }

            @Override
            public int[] eventPosition(int eventId) {
                MapCharacter character = eventCharacters == null ? null : eventCharacters.character(eventId);
                return character == null ? null : new int[] {character.x(), character.y(), character.direction()};
            }
        };
    }

    /**
     * One frame of the dependent events. {@code before}: the followers' steps advance first, so a follower lands in the
     * frame the player does. After the player moved: the follow step, the turn, the walking animation, the follower's
     * route and the Ctrl toggle (297_Follower_Main:1373-1397 {@code Scene_Map#update}).
     */
    private void updateFollowers(float delta, boolean before) {
        if (followers == null) {
            return;
        }
        if (before) {
            followers.advance(delta);
            return;
        }
        followers.afterPlayer(delta);
        pokemon.runtime.state.FieldGlobals g = gameState.fieldGlobals();
        if (g.bicycle != lastBicycle || g.surfing != lastSurfing || g.diving != lastDiving) {
            boolean mounted = g.bicycle && !lastBicycle;
            boolean dismounted = !g.bicycle && lastBicycle;
            lastBicycle = g.bicycle;
            lastSurfing = g.surfing;
            lastDiving = g.diving;
            // pbMountBike: come_back(!BicycleAlways), pbDismountBike: come_back(true), the other vehicles: come_back(false).
            boolean animate = mounted
                    ? !new pokemon.runtime.field.Vehicles(context.pbsData(), gameState).bicycleAlways(mapData.mapId)
                    : dismounted;
            followers.comeBack(animate);
            rebindEntities();
        }
        if (followerRoute != null) {
            MapCharacter character = followers.followerCharacter();
            if (character == null || followerRoute.finished()) {
                followerRoute = null;
            } else {
                followerRoute.update(delta, character, followerRouteContext);
            }
        }
        if (playerWait > 0f) {
            playerWait -= delta;
        }
        boolean idle = interpreter != null && !interpreter.running() && !context.messageService().visible()
                && !context.transferPending() && playerRoute == null;
        if (idle && followerRoute == null && followers.follower() != null && !g.bicycle
                && context.inputManager().wasPressed(GameAction.TOGGLE_FOLLOWER)) {
            followers.toggle(null, true);                                           // :1383-1385 CTRL
            rebindEntities();
        }
    }

    /** Rebinds the drawn characters after the dependent events changed. */
    private void rebindEntities() {
        bindEntities(baseEntities(), locator);
    }

    /** Route context of the follower's talk routes: it only turns, waits and jumps on the spot. */
    private final pokemon.runtime.event.MoveRoutePlayer.Context followerRouteContext =
            new pokemon.runtime.event.MoveRoutePlayer.Context() {
                @Override
                public boolean step(MapCharacter character, int direction) {
                    return false;
                }

                @Override
                public boolean canLand(MapCharacter character, int x, int y) {
                    return true;
                }
            };

    /** {@code followingMoveRoute(commands)} with the DSL of FollowerTalk: TD/TL/TR/TU turn, Wn wait, J jump on the spot. */
    private static pokemon.runtime.data.MoveRoute followerRoute(String dsl) {
        pokemon.runtime.data.MoveRoute route = new pokemon.runtime.data.MoveRoute();
        for (String token : dsl.split(" ")) {
            pokemon.runtime.data.MoveRoute.Command command = new pokemon.runtime.data.MoveRoute.Command();
            command.parameters = new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.array);
            switch (token.charAt(0)) {
                case 'T':
                    command.code = token.charAt(1) == 'D' ? 16 : token.charAt(1) == 'L' ? 17 : token.charAt(1) == 'R' ? 18 : 19;
                    break;
                case 'W':
                    command.code = 15;
                    command.parameters.addChild(new com.badlogic.gdx.utils.JsonValue(Long.parseLong(token.substring(1))));
                    break;
                default:
                    command.code = 14;
                    command.parameters.addChild(new com.badlogic.gdx.utils.JsonValue(0L));
                    command.parameters.addChild(new com.badlogic.gdx.utils.JsonValue(0L));
                    break;
            }
            route.commands.add(command);
        }
        pokemon.runtime.data.MoveRoute.Command end = new pokemon.runtime.data.MoveRoute.Command();
        end.code = 0;
        route.commands.add(end);
        return route;
    }

    /**
     * {@code pbTalkToFollower} (297_Follower_Main:68-82): the cry, the handler's emote and routes; the wait and the line
     * are the plan the event plays.
     */
    private MapPort.FollowerTalkPlan talkToFollower() {
        if (followers == null || !followers.followerShows()) {
            return null;                                                           // :69
        }
        pokemon.runtime.pokemon.Pokemon first = gameState.trainer().party.firstAble();
        MapCharacter follower = followers.followerCharacter();
        if (first == null || follower == null) {
            return null;
        }
        if (first.species != null) {
            context.audioManager().playCry(first.species.id);                       // :77 pbPlayCry
        }
        final int weather = gameState.weather().type();
        pokemon.runtime.field.FollowerTalk.Result result = pokemon.runtime.field.FollowerTalk.choose(first,
                new pokemon.runtime.field.FollowerTalk.Env() {
                    @Override
                    public String mapName() {
                        return mapData.name;
                    }

                    @Override
                    public String trainerName() {
                        return gameState.trainer().name;
                    }

                    @Override
                    public int weather() {
                        return weather;
                    }

                    @Override
                    public boolean holdItem() {
                        return gameState.fieldGlobals().followerHoldItem;
                    }

                    @Override
                    public String itemNameById(int id) {
                        pokemon.runtime.pokemon.PbsData pbs = context.pbsData();
                        pokemon.runtime.pokemon.PbsData.Item item = pbs == null || id <= 0 ? null : pbs.itemById(id);
                        return item == null ? "SITRUSBERRY" : item.internalName;      // :101-103 an unknown id gives a Sitrus Berry
                    }
                }, talkRandom.nextInt(6), talkRandom);                                // :79 rand(6)
        MapPort.FollowerTalkPlan plan = new MapPort.FollowerTalkPlan();
        if (result != null) {
            if (result.animation > 0) {
                startTileAnimation(result.animation, follower.logicalX(), follower.logicalY() - 2, 3);   // :80 event.x, event.y-2
            }
            plan.waitFrames = result.waitFrames;
            if (result.route != null) {
                playerWait = result.playerWait / 20f;
                followerRoute = new pokemon.runtime.event.MoveRoutePlayer(followerRoute(result.route));
            }
            if (result.foundItem) {
                plan.foundItem = result.foundItemName;
                plan.foundQuantity = result.foundQuantity;
                plan.foundMessage = result.foundMessage;
                plan.pokemonName = first.name;
            } else if (result.message != null) {
                plan.messages.add(result.message);
            }
        }
        facePlayer(follower);                                                      // :81 pbTurnTowardEvent(event, $game_player)
        return plan;
    }

    private final java.util.Random talkRandom = new java.util.Random();

    private void facePlayer(MapCharacter event) {
        int sx = event.logicalX() - player.logicalX();
        int sy = event.logicalY() - player.logicalY();
        if (sx == 0 && sy == 0) {
            return;
        }
        if (Math.abs(sx) > Math.abs(sy)) {
            event.turn(sx > 0 ? 4 : 6);
        } else {
            event.turn(sy > 0 ? 8 : 2);
        }
    }

    /** Starts the common event of a dependent event the player faces (182:394-425 updateDependentEvents). */
    private boolean startFacingDependent(int x, int y) {
        if (followers == null || interpreter == null) {
            return false;
        }
        pokemon.runtime.state.Dependent entry = followers.at(x, y);
        if (entry == null || entry.commonEvent < 0) {
            return false;
        }
        return interpreter.startCommonEvent(entry.commonEvent, mapData.mapId);
    }

    // ------------------------------------------------------------------
    // 179_PField_FieldMoves: the field side of the hidden moves
    // ------------------------------------------------------------------

    /** The event the player faces ({@code $game_player.pbFacingEvent}, 025_Game_Player:150-170), or null. */
    private pokemon.runtime.data.MapData.EventData findFacingEvent() {
        if (mapData == null || tileMap == null) {
            return null;
        }
        int dx = Collision.targetX(0, player.direction());
        int dy = Collision.targetY(0, player.direction());
        int x = player.x() + dx;
        int y = player.y() + dy;
        if (!tileMap.valid(x, y)) {
            return null;                                                           // :154
        }
        pokemon.runtime.data.MapData.EventData found = eventFacing(x, y);
        if (found == null && tileMap.counter(x, y)) {                              // :160-168 across a counter
            found = eventFacing(x + dx, y + dy);
        }
        return found;
    }

    private pokemon.runtime.data.MapData.EventData eventFacing(int x, int y) {
        for (pokemon.runtime.data.MapData.EventData event : mapData.events) {
            MapCharacter character = eventCharacters == null ? null : eventCharacters.character(event.id);
            int ex = character == null ? event.x : character.logicalX();
            int ey = character == null ? event.y : character.logicalY();
            if (ex != x || ey != y) {
                continue;                                                          // :156
            }
            pokemon.runtime.data.MapData.EventPageData page = EventPages.resolve(gameState, mapData.mapId, event);
            if (page == null) {
                continue;                                                          // an erased event
            }
            if (character != null && character.isJumping()) {
                continue;                                                          // :157 event.jumping?
            }
            if (overTrigger(event, page, ex, ey)) {
                continue;                                                          // :157 event.over_trigger?
            }
            return event;
        }
        return null;
    }

    /** {@code Game_Event#over_trigger?} (024_Game_Event:114-119): a walk-over event (no sheet, or through) on a passable tile. */
    private boolean overTrigger(pokemon.runtime.data.MapData.EventData event,
                                pokemon.runtime.data.MapData.EventPageData page, int x, int y) {
        boolean hasSheet = page.graphic != null && page.graphic.characterName != null && !page.graphic.characterName.isEmpty();
        if (hasSheet && !EventPages.through(page)) {
            return false;                                                          // :115
        }
        if (event.name != null && event.name.toLowerCase().contains("hiddenitem")) {
            return false;                                                          // :116
        }
        return tileMap.passableAnyDirection(x, y);                                 // :117
    }

    /** {@code pbSmashEvent}'s erase (179:236-247): events waiting to disappear, [event id, seconds left]. */
    private final java.util.List<float[]> scheduledErase = new java.util.ArrayList<>();

    private pokemon.runtime.data.MapData.EventData eventById(int eventId) {
        if (mapData == null) {
            return null;
        }
        for (pokemon.runtime.data.MapData.EventData event : mapData.events) {
            if (event.id == eventId) {
                return event;
            }
        }
        return null;
    }

    private float smashFacingEvent() {
        return smashEvent(findFacingEvent());
    }

    /** {@code pbSmashEvent(event)} (179:231-248) without the sound: the shake route, then the erase 0.4 s later. */
    private float smashEvent(pokemon.runtime.data.MapData.EventData event) {
        if (event == null || eventCharacters == null) {
            return 0f;
        }
        eventCharacters.setMoveRoute(event.id, followerRoute("W2 TL W2 TR W2 TU W2"));          // :236-244
        scheduledErase.add(new float[] {event.id, 40 * 4 / 10 / 40f});                          // :245 pbWait(40*4/10), :246 event.erase
        return 40 * 4 / 10 / 40f;
    }

    private void updateScheduledErase(float delta) {
        for (int i = scheduledErase.size() - 1; i >= 0; i--) {
            float[] entry = scheduledErase.get(i);
            entry[1] -= delta;
            if (entry[1] <= 0f) {
                scheduledErase.remove(i);
                eventCharacters.erase((int) entry[0]);
                if (eventCharacters != null) {
                    rebindEntities();
                }
            }
        }
    }

    private pokemon.runtime.event.MoveRoutePlayer.Context noRouteContext() {
        return routeContext;
    }

    /** {@code pbAscendWaterfall} (179:919-936): up through every waterfall tile, through everything, at move speed 2. */
    private float ascendWaterfall() {
        if (player.direction() != 8) {
            return 0f;                                                             // :922
        }
        int terrain = gameState.fieldGlobals().facingTerrainTag;
        if (!pokemon.runtime.field.PBTerrain.isWaterfall(terrain)) {
            return 0f;                                                             // :926
        }
        int steps = 0;
        int y = player.y();
        do {                                                                       // :929-933 move_up until the tile is no waterfall
            y--;
            steps++;
        } while (tileMap.valid(player.x(), y)
                && pokemon.runtime.field.PBTerrain.isWaterfall(tileMap.terrainTag(player.x(), y, false, gameState.bridge())));
        pokemon.runtime.data.MoveRoute route = new pokemon.runtime.data.MoveRoute();
        route.commands.add(command(37, 0));                                         // through on
        route.commands.add(command(29, 2));                                         // move_speed = 2
        for (int i = 0; i < steps; i++) {
            route.commands.add(command(4, -1));                                     // move_up
        }
        route.commands.add(command(38, 0));                                         // through off
        route.commands.add(command(29, player.moveSpeedLevel));                     // restore the speed
        route.commands.add(command(0, -1));
        playerRoute = new pokemon.runtime.event.MoveRoutePlayer(route);
        return steps / pokemon.runtime.map.MapCharacter.SPEEDS[2] + 0.2f;
    }

    private static pokemon.runtime.data.MoveRoute.Command command(int code, int parameter) {
        pokemon.runtime.data.MoveRoute.Command command = new pokemon.runtime.data.MoveRoute.Command();
        command.code = code;
        command.parameters = new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.array);
        if (parameter >= 0 || code == 29) {
            command.parameters.addChild(new com.badlogic.gdx.utils.JsonValue((long) parameter));
        }
        return command;
    }

    /** {@code $MapFactory.getTerrainTag(mapId, $game_player.x, $game_player.y)}. */
    private int terrainTagOnMap(int mapId) {
        if (database == null || mapId <= 0) {
            return 0;
        }
        pokemon.runtime.data.MapData other = database.map(mapId);
        if (other == null) {
            return 0;
        }
        TileMap otherMap = new TileMap(other, database.tileset(other.tilesetId));
        return otherMap.valid(player.x(), player.y()) ? otherMap.terrainTag(player.x(), player.y(), false, 0) : 0;
    }

    // ---- pbHiddenMoveAnimation (179:78-189) ----

    private HiddenMoveAnimation hiddenMoveBanner;
    private float hiddenMoveClock;
    private int hiddenMoveSpecies;

    private float hiddenMoveAnimation(pokemon.runtime.pokemon.Pokemon pkmn) {
        if (pkmn == null || locator == null) {
            return 0f;                                                             // :79 return false if !pokemon
        }
        java.io.File bgFile = locator.find("Pictures", "hiddenMovebg.png");
        java.io.File strobeFile = locator.find("Pictures", "hiddenMoveStrobes.png");
        Texture bg = bgFile == null ? null : textures.load("picture:hiddenMovebg", bgFile);
        Texture strobes = strobeFile == null ? null : textures.load("picture:hiddenMoveStrobes", strobeFile);
        Texture sprite = null;
        if (pkmn.species != null) {
            String base = String.format("%03d", pkmn.species.id) + (pkmn.shiny ? "s" : "");
            int form = pkmn.formIndex();
            String[] names = {form > 0 ? base + "_" + form : base, base, String.format("%03d", pkmn.species.id),
                    pkmn.species.internalName};
            for (String name : names) {
                java.io.File file = locator.find("Battlers", name + ".png");
                if (file != null) {
                    sprite = textures.load("battler:" + name, file);
                    break;
                }
            }
            hiddenMoveSpecies = pkmn.species.id;
        }
        if (bg == null) {
            return 0f;
        }
        hiddenMoveBanner = new HiddenMoveAnimation(bg, strobes, sprite, (int) ScreenMetrics.logicalWidth(),
                (int) ScreenMetrics.logicalHeight(), talkRandom);
        hiddenMoveClock = 0f;
        return HiddenMoveAnimation.durationSeconds();
    }

    private void updateHiddenMove(float delta) {
        if (hiddenMoveBanner == null) {
            return;
        }
        hiddenMoveClock += delta;
        while (hiddenMoveClock >= 1f / 40f && !hiddenMoveBanner.finished()) {
            hiddenMoveClock -= 1f / 40f;
            hiddenMoveBanner.tick();
            if (hiddenMoveBanner.takeCry() && hiddenMoveSpecies > 0) {
                context.audioManager().playCry(hiddenMoveSpecies);                  // :130 pbPlayCry(pokemon)
            }
        }
        if (hiddenMoveBanner.finished()) {
            hiddenMoveBanner = null;
        }
    }

    private void renderHiddenMove() {
        if (hiddenMoveBanner == null) {
            return;
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        hiddenMoveBanner.render(batch, mapCamera.originX(), mapCamera.originY(), mapCamera.viewPixelHeight());
        batch.end();
    }

    // ---- pbSweetScent's red flash (179:813-839) ----

    private int[] sweetScentAlpha;
    private float sweetScentClock;

    private float sweetScentFlash() {
        java.util.List<Integer> alphas = new java.util.ArrayList<>();
        int alpha = 0;
        int count = 0;
        final int alphaDiff = 12;                                                  // :825 12 * frame_rate / 40
        do {
            if (count == 0 && alpha < 128) {
                alpha += alphaDiff;                                                // :827-828
            } else if (count > 40 / 4) {
                alpha -= alphaDiff;                                                // :829-830
            } else {
                count++;                                                           // :832
            }
            alphas.add(Math.max(0, alpha));
        } while (alpha > 0);                                                       // :837
        sweetScentAlpha = new int[alphas.size()];
        for (int i = 0; i < sweetScentAlpha.length; i++) {
            sweetScentAlpha[i] = alphas.get(i);
        }
        sweetScentClock = 0f;
        return sweetScentAlpha.length / 40f;
    }

    private void renderSweetScent(float delta) {
        if (sweetScentAlpha == null) {
            return;
        }
        sweetScentClock += delta;
        int frame = (int) (sweetScentClock * 40f);
        if (frame >= sweetScentAlpha.length) {
            sweetScentAlpha = null;
            return;
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(1f, 0f, 0f, sweetScentAlpha[frame] / 255f);
        batch.draw(pixel, mapCamera.originX(), mapCamera.originY(), mapCamera.viewPixelWidth(), mapCamera.viewPixelHeight());
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    // ---- pbFlyAnimation (340_Fly_Animation) ----

    private FlyBirdAnimation flyBird;
    private float flyBirdClock;
    private boolean flyBirdDeparture;
    /** The arrival half waits for the fade-in of the new map (the plugin plays it after {@code pbFadeOutIn}). */
    private boolean flyArrivalPending;

    private float flyAnimation(boolean departure) {
        return flyAnimation(departure, null);
    }

    private String flyArrivalBird;

    /** {@code pbFlyAnimation(landing, pokemon, item, flybird_species)}: a preset species picks Latios / Latias / Groudon (340:29-47). */
    private float flyAnimation(boolean departure, String species) {
        if (locator == null) {
            return 0f;
        }
        if (departure) {
            player.turn(4);                                                        // :25 $game_player.turn_left
            context.audioManager().playSe("flybird", 100, 100);                    // :26 pbSEPlay("flybird")
        }
        // :33-57 SHOW_GEN_4_BIRD, and a 10 % Groudon (:50-51)
        String name;
        if (species == null) {
            name = talkRandom.nextInt(100) < 10 ? "flybird_Groudon" : "flybird_gen4";     // 340:48-57 the usual flight
        } else {
            name = "LATIOS".equals(species) ? "flybird_Latios" : "LATIAS".equals(species) ? "flybird_Latias"
                    : "GROUDON".equals(species) ? "flybird_Groudon" : "flybird_gen4";     // 340:29-47
        }
        java.io.File file = locator.find("Pictures", name + ".png");
        if (file == null) {
            file = locator.find("Pictures", "flybird.png");
        }
        Texture bird = file == null ? null : textures.load("picture:" + file.getName(), file);
        flyBird = new FlyBirdAnimation(bird, (int) ScreenMetrics.logicalWidth(), (int) ScreenMetrics.logicalHeight());
        flyBirdClock = 0f;
        flyBirdDeparture = departure;
        if (departure) {
            player.opacity = 0f;                                                   // :80-82 $game_player.setOpacity(0)
        }
        return FlyBirdAnimation.durationSeconds();
    }

    private void updateFlyBird(float delta) {
        if (flyArrivalPending && context.screenEffects().fade() <= 0f) {
            flyArrivalPending = false;
            flyAnimation(false, flyArrivalBird);                                   // 179:535 pbFlyAnimation(false)
            flyArrivalBird = null;
        }
        if (flyBird == null) {
            return;
        }
        flyBirdClock += delta;
        while (flyBirdClock >= 1f / 40f && !flyBird.finished()) {
            flyBirdClock -= 1f / 40f;
            flyBird.tick();
            if (!flyBirdDeparture && flyBird.pastCenter()) {
                player.opacity = 1f;                                               // :95-97 the player appears at the centre
            }
        }
        if (flyBird.finished()) {
            if (flyBirdDeparture) {
                player.opacity = 1f;                                               // :132-134
            }
            flyBird = null;
        }
    }

    private void renderFlyBird() {
        if (flyBird == null) {
            return;
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        flyBird.render(batch, mapCamera.originX(), mapCamera.originY(), mapCamera.viewPixelHeight());
        batch.end();
    }

    /** {@code Events.onMapChange} (170_PField_Field:535-540): the healing spot and the visited maps. */
    private void noteMapChange(int mapId) {
        pokemon.runtime.state.FieldGlobals g = gameState.fieldGlobals();
        pokemon.runtime.pokemon.PbsData pbs = context.pbsData();
        pokemon.runtime.pokemon.PbsData.Metadata meta = pbs == null ? null : pbs.mapMetadata(mapId);
        if (meta != null && meta.healingSpot != null) {
            g.healingSpot = meta.healingSpot.clone();                             // :537
        }
        g.visitedMaps.add(mapId);                                                  // :540
        if (!g.inSafari(mapId)) g.safari.end();                                    // 242_PBattle_Safari:54-56
    }

    // ---- DarknessSprite / Flash (170_PField_Field:566-585, 171_PField_Visuals:364-404, 179:479-495) ----

    private DarknessOverlay darkness;
    private boolean darknessGrowing;
    private float darknessClock;

    /** {@code Events.onMapSceneChange}'s darkness block (170:566-585): a dark map gets the layer, any other drops it. */
    private void refreshDarkness(int mapId) {
        pokemon.runtime.pokemon.PbsData pbs = context.pbsData();
        pokemon.runtime.pokemon.PbsData.Metadata meta = pbs == null ? null : pbs.mapMetadata(mapId);
        pokemon.runtime.state.FieldGlobals g = gameState.fieldGlobals();
        if (meta != null && meta.darkMap) {
            if (darkness != null) {
                darkness.dispose();
            }
            darkness = new DarknessOverlay((int) ScreenMetrics.logicalWidth(), (int) ScreenMetrics.logicalHeight());
            if (g.flashUsed) {
                darkness.radius(DarknessOverlay.RADIUS_MAX);                       // :572-573 radius = radiusMax
            }
        } else {
            g.flashUsed = false;                                                   // :580
            if (darkness != null) {
                darkness.dispose();                                                // :582
                darkness = null;
            }
        }
        darknessGrowing = false;
    }

    /** Flash's light circle grows by 8 a frame until it reaches the maximum (179:486-493). */
    private float flashDarkness() {
        if (darkness == null) {
            return 0f;
        }
        darknessGrowing = true;
        darknessClock = 0f;
        return Math.max(0, DarknessOverlay.RADIUS_MAX - darkness.radius() + 7) / 8 / 40f;
    }

    private void updateDarkness(float delta) {
        if (!darknessGrowing || darkness == null) {
            return;
        }
        darknessClock += delta;
        while (darknessClock >= 1f / 40f && darkness.radius() < DarknessOverlay.RADIUS_MAX) {
            darknessClock -= 1f / 40f;
            darkness.radius(Math.min(DarknessOverlay.RADIUS_MAX, darkness.radius() + 8));   // :486/:491-492
        }
        if (darkness.radius() >= DarknessOverlay.RADIUS_MAX) {
            darknessGrowing = false;
        }
    }

    private void renderDarkness() {
        if (darkness == null) {
            return;
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        darkness.render(batch, mapCamera.originX(), mapCamera.originY());
        batch.end();
    }

    // ---- 375_001_HND_Config / 376_002_HND_Script: Headtop_Name ----

    private static final com.badlogic.gdx.graphics.Color HEAD_BASE = rgb(248, 248, 248);
    private static final com.badlogic.gdx.graphics.Color HEAD_SHADOW = rgb(24, 24, 24);
    /** 375_001_HND_Config:9-18 {@code PREFIX_COLOR}, in the plugin's order (the first prefix the name contains wins). */
    private static final String[] HEAD_PREFIXES = {"#ss", "#s", "#m", "#f", "#r", "#g", "#b", "#y"};
    private static final com.badlogic.gdx.graphics.Color[] HEAD_COLORS = {
        rgb(255, 144, 0), rgb(216, 160, 0), rgb(78, 110, 242), rgb(248, 128, 164),
        rgb(255, 64, 64), rgb(48, 224, 96), rgb(0, 64, 255), rgb(248, 216, 0)};
    /** {@code NAME_OPACITY} / {@code NAME_OFFSET_Y} / {@code NAME_OFFSET_OY}. */
    private static final float HEAD_OPACITY = 224f / 255f;
    private static final float HEAD_OFFSET_Y = -30f;
    private static final float HEAD_OFFSET_OY = 8f;

    private static com.badlogic.gdx.graphics.Color rgb(int r, int g, int b) {
        return new com.badlogic.gdx.graphics.Color(r / 255f, g / 255f, b / 255f, 1f);
    }

    /**
     * The names above the characters ({@code Headtop_Name#update}, 376:72-130), as the option 头顶名称 allows (1 the hero,
     * 2 the NPCs, 3 both). An NPC shows the part of its event name after a {@code #}; a colour prefix picks the shadow.
     */
    private void renderHeadNames() {
        int option = context.settings().headtopname;
        if (option <= 0 || messageWindow == null || mapData == null || mapData.mapId == 1) {
            return;                                                                // :113 $game_map.map_id != 1
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (option == 1 || option == 3) {
            pokemon.runtime.pokemon.TrainerState trainer = gameState.trainer();
            boolean female = trainer.gender == pokemon.runtime.pokemon.PokemonStats.FEMALE;
            drawHeadName(player, trainer.name, female ? HEAD_COLORS[3] : HEAD_COLORS[2]);   // :32-35 @female_color / @male_color
        }
        if ((option == 2 || option == 3) && eventCharacters != null) {
            for (int i = 0; i < mapData.events.size && i < eventCharacters.characters().size; i++) {
                pokemon.runtime.data.MapData.EventData event = mapData.events.get(i);
                MapCharacter character = eventCharacters.characters().get(i);
                if (event.name == null || event.name.indexOf('#') < 0 || character == null) {
                    continue;                                                      // :40-41 return unless @event.name.include?('#')
                }
                String raw = event.name.substring(event.name.indexOf('#'));       // :42 name[/\\s*#(.+)/i]
                if (raw.length() < 2) {
                    continue;
                }
                com.badlogic.gdx.graphics.Color color = null;
                for (int p = 0; p < HEAD_PREFIXES.length; p++) {                   // :44-50
                    if (raw.contains(HEAD_PREFIXES[p])) {
                        color = HEAD_COLORS[p];
                        raw = raw.replace(HEAD_PREFIXES[p], "");
                        break;
                    }
                }
                raw = raw.replace("#", "");                                        // :51
                raw = raw.replaceAll("(?i)\\\\pn", java.util.regex.Matcher.quoteReplacement(gameState.trainer().name));   // :52
                raw = raw.replaceAll("(?i)\\\\rn", "");                            // :53 登记: the rival's name is not modelled
                if ("路比".equals(raw)) {
                    color = color != null ? color : rgb(248, 24, 24);              // :56 SPECIAL_NAME_COLORS
                }
                drawHeadName(character, raw, color != null ? color : HEAD_SHADOW);
            }
        }
        batch.end();
    }

    private void drawHeadName(MapCharacter character, String name, com.badlogic.gdx.graphics.Color shadow) {
        if (character == null || name == null || name.isEmpty() || character.opacity <= 0f
                || character.characterName == null || character.characterName.isEmpty()) {
            return;                                                                // :95 @event.character_name != ''
        }
        float height = renderer.spriteHeight(character);
        float centerX = character.pixelX() + TilesetGeometry.TILE_SIZE / 2f;
        float spriteTop = character.pixelY() + height;
        float lineTop = spriteTop - (HEAD_OFFSET_Y * -1f) - HEAD_OFFSET_OY;         // :89 tsprite.y = top - NAME_OFFSET_Y, text at +NAME_OFFSET_OY
        // :96-98 only what is on screen
        float left = mapCamera.originX();
        float bottom = mapCamera.originY();
        if (centerX < left - 64f || centerX > left + mapCamera.viewPixelWidth() + 64f
                || spriteTop < bottom - 64f || spriteTop > bottom + mapCamera.viewPixelHeight() + 64f) {
            return;
        }
        messageWindow.drawCenteredShadowText(batch, name, centerX, lineTop, HEAD_BASE, shadow, HEAD_OPACITY);
    }

    /** The player and the characters of the dependent events (the following Pokemon), what the renderer draws besides the map events. */
    private Array<MapCharacter> baseEntities() {
        Array<MapCharacter> list = new Array<>();
        list.add(player);
        if (followers != null) {
            for (MapCharacter character : followers.characters()) {
                list.add(character);
            }
        }
        return list;
    }

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
            // stale sheet. P3: a berry plant that owns the sheet stays on the
            // entity path even when it hides the event (characterName null).
            runtime[i] = (character.characterName != null && !character.characterName.isEmpty())
                    || character.sheetOverridden;
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
     * Applies {@link GameState#playerId()} to the player's walking graphic.
     * A project with PlayerA..PlayerH metadata starts blank (id -1) until the
     * intro's gender selector runs pbChangePlayer; a project without the table
     * keeps the default charset.
     */
    private void applyPlayerCharset() {
        pokemon.runtime.data.ProjectInfo.RuntimeProfile profile = runtimeProfile;
        boolean hasSelector = profile != null && profile.players != null && profile.players.size > 0;
        int id = gameState.playerId();
        String charset = null;
        String running = null;
        if (id >= 0 && profile != null) {
            pokemon.runtime.data.ProjectInfo.PlayerGraphic graphic = profile.player(id);
            if (graphic != null) {
                charset = blankToNull(graphic.charset);
                running = blankToNull(graphic.runningCharset);
                // 025_Game_Player:436-446 pbUpdateVehicle: diving 5, surfing 3, bicycle 2, else 1; a blank entry falls back to 1
                // (:pbGetPlayerCharset 'ret = meta[1] if !ret || ret==""'). 026_Game_Player_Visuals:42 keeps the running graphic
                // for walking only. 登记: the diving graphic (Dive is not part of the vehicles yet).
                pokemon.runtime.state.FieldGlobals vehicle = gameState.fieldGlobals();
                String vehicleCharset = vehicle.surfing ? blankToNull(graphic.surfCharset)
                        : vehicle.bicycle ? blankToNull(graphic.bikeCharset) : null;
                if (vehicle.surfing || vehicle.bicycle) {
                    running = null;
                    if (vehicleCharset != null) {
                        charset = vehicleCharset;
                    }
                }
            }
        }
        if (charset == null && !hasSelector) {
            charset = PLAYER_CHARACTER;
            running = PLAYER_RUNNING_CHARACTER;
        }
        playerCharacter = charset;
        playerRunningCharacter = running;
        appliedPlayerId = id;
        if (player != null) {
            player.characterName = charset;
            player.runningCharacterName = running;
        }
    }

    /**
     * {@code ItemHandlers::UseFromBag} of the items that act on the map (189_PItem_ItemEffects): the lines shown when the item
     * cannot be used here, null when it can.
     */
    private String[] mapItemRefusal(String item) {
        pokemon.runtime.state.FieldGlobals g = gameState.fieldGlobals();
        boolean partnered = followers != null && followers.hasDependentEvents();
        switch (item) {
            case "SUPERROD":                                               // :55-63
                return pokemon.runtime.field.ItemHandlers.canFish(pokemon.runtime.field.PBTerrain.isWater(g.facingTerrainTag),
                        facingPassableNow(), g.surfing) ? null : new String[] {"这里不能使用。"};
            case "ESCAPEROPE":                                             // :25-35
            case "INFINITEROPE":                                           // :37-47
                if (partnered) return new String[] {"与他人同行时不能使用。"};
                return g.escapePoint != null && g.escapePoint.length > 0 ? null : new String[] {"这里不能使用。"};
            case "LANTERN":                                                // :1523-1527 next false if !darkness -> "这里不能使用。"
                return darkness != null ? null : new String[] {"这里不能使用。"};
            case "EONFLUTE":                                               // :1554-1559, :1560-1565 then next false -> "这里不能使用。"
                if (partnered) return new String[] {"与他人同行时不能使用。", "这里不能使用。"};
                return null;
            case "ETHEREALNEXUS":                                          // :1649-1660
                if (partnered) return new String[] {"与他人同行时不能使用。", "这里不能使用。"};
                if (pokemon.runtime.field.ItemHandlers.banMap(gameState.currentMapId())) return new String[] {"无法在这里使用"};
                return null;
            default:
                return null;
        }
    }

    /** {@code $game_map.passable?(player.x, player.y, player.direction, player)}. */
    private boolean facingPassableNow() {
        pokemon.runtime.state.FieldGlobals g = gameState.fieldGlobals();
        int bit = Collision.directionBit(player.direction());
        return tileMap.playerPassable(player.x(), player.y(), bit, gameState.bridge(), g.surfing, g.bicycle);
    }

    /** {@code Game_Character#fullPattern}: the direction row (down 0, left 1, right 2, up 3) x 4 + the column. */
    private static int fullPattern(MapCharacter character) {
        int row = character.direction() == 4 ? 1 : character.direction() == 6 ? 2 : character.direction() == 8 ? 3 : 0;
        return row * 4 + character.pattern();
    }

    /**
     * {@code setDefaultCharName(fishSheet, pattern, true)} (026_Game_Player_Visuals:14-20): the player shows one frame of the
     * fishing sheet. The sheet is {@code meta[6]} (on foot) or {@code meta[7]} (surfing) of the player's PBS entry.
     */
    private boolean fishingFrame(boolean surfing, int pattern) {
        pokemon.runtime.data.ProjectInfo.PlayerGraphic graphic = runtimeProfile == null ? null
                : runtimeProfile.player(gameState.playerId());
        if (graphic == null || pattern < 0 || pattern >= 16) {
            return false;
        }
        String sheet = blankToNull(surfing && graphic.surfFishCharset != null ? graphic.surfFishCharset : graphic.fishCharset);
        if (sheet == null) {
            return false;                                                  // 170:1238 meta[num] && meta[num]!=""
        }
        player.characterName = sheet;
        player.face(new int[] {2, 4, 6, 8}[pattern / 4]);
        player.basePattern(pattern % 4);
        return true;
    }

    private void endFishingFrame(int oldPattern) {
        applyPlayerCharset();                                              // setDefaultCharName(nil, ...): the walking sheet
        int row = Math.max(0, Math.min(3, oldPattern / 4));
        player.face(new int[] {2, 4, 6, 8}[row]);
        player.basePattern(oldPattern % 4);
    }

    private static String blankToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
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
                blockedStep = true;
                return false;
            }
            NeighborView view = neighbourFor(crossing.mapId);
            if (view == null || !Collision.canStepIntoNeighbour(gameState, view.tileMap,
                    view.data, character, crossing.x, crossing.y)) {
                blockedStep = true;
                return false;
            }
            return character.startMove(x, y, direction, true);
        }
        if (Collision.isFacingLedge(gameState, tileMap, mapData, character, direction)) {
            // Game_Player#move_generic:74-75: pbLedge consumes the step even
            // when the jump cannot land (PField_Field:1135-1145).
            int[] landing = Collision.ledgeLanding(gameState, tileMap, mapData, character, direction);
            if (landing != null) {
                ledgeJump(character, direction, landing[0], landing[1]);
            }
            return true;
        }
        if (!Collision.canStep(gameState, tileMap, mapData, character, direction)) {
            blockedStep = true;
            return false;
        }
        if (character == player && endSurfStep(character, direction, x, y)) {
            return true;                                                   // Game_Player:76 return if pbEndSurf
        }
        return character.startMove(x, y, direction);
    }

    /**
     * PField_Field#pbLedge:1135-1145 + pbJumpToward:1207-1228: the two-tile
     * ledge jump, its "Player jump" SE (line 1217) and the dust animation at
     * the landing tile (line 1138). The jump blocks further input until it
     * lands ({@code Game_Character#jump}:652-661).
     */
    private void ledgeJump(MapCharacter character, int direction, int landX, int landY) {
        character.face(direction);
        // Game_Character:654: max(1, distance) * TILE * 3/8, distance = 2.
        float peak = Math.max(1f, 2f) * TilesetGeometry.TILE_SIZE * 3f / 8f;
        character.startJump(landX, landY, peak);
        context.audioManager().playSe("Player jump", 100, 100);
        // pbLedge adds the dust only after pbJumpToward waited for the landing
        // (PField_Field:1138); the tile is already the landing tile, so the
        // landing frame renders it there.
        ledgeDustPending = true;
    }

    /**
     * Game_Player#bump_into_object (Game_Player:61-65): a blocked step plays
     * "Player bump" (Audio_Play:200), unless the 10-frame SE cooldown
     * ({@code @bump_se = 40/4}) is still running.
     */
    private void playBumpSe() {
        if (bumpSe > 0f) {
            return;
        }
        bumpSe = 10f / 40f;
        context.audioManager().playSe("Player bump", 100, 100);
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
        int previousMap = mapData.mapId;
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
            if (next.region >= 0) gameState.trainer().region = next.region;
            tileMap = nextMap;
            renderer = nextRenderer;
            eventCharacters = nextCharacters;
            berryPlants = new BerryPlantSprites(mapData, eventCharacters, gameState,
                    database == null ? null : database.pbs(), this::startTileAnimation, textures, locator);
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
            // Events.onMapChange (PField_Field:537-538): the encounter tables and $PokemonMap are rebuilt for the new map.
            // A screen that crossed a connection kept the previous map's tables until it was rebuilt.
            gameState.fieldGlobals().clearMap();
            if (pokemonEncounters != null) {
                pokemonEncounters.setup(next.mapId);
            }
            new pokemon.runtime.field.Vehicles(context.pbsData(), gameState).onMapChange(next.mapId);
            noteMapChange(next.mapId);
            refreshDarkness(next.mapId);
            showSignpost(previousMap);
            if (interpreter != null) {
                interpreter.stop();
            }
            arrivalDoorPage = false;
            doorShowHold.clear();
            cameraScroll.reset();
            environment.reset();
            followers.bind(tileMap, mapData, true);                 // 297_Follower_Main:1400-1410 the follower comes along
            bindEntities(baseEntities(), locator);
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

    /** L1 integration aid (capture tools): live character sheet of one event. */
    public String eventSheet(int eventId) {
        MapCharacter character = eventCharacters == null ? null : eventCharacters.character(eventId);
        return character == null ? null : character.characterName;
    }

    /** L1: draws the pause menu over the frozen map (logical screen space). */
    /**
     * {@code Graphics.snap_to_bitmap} for the battle entry's KGC transition
     * ({@code Transitions:805+}): the frozen world screen, captured from the
     * framebuffer after this frame's world pass and scaled to the logical
     * screen. Mirrors {@code PauseMenuOverlay.captureBackground}.
     */
    private void captureEntrySnapshot() {
        int sx = viewport.getScreenX();
        int sy = viewport.getScreenY();
        int sw = viewport.getScreenWidth();
        int sh = viewport.getScreenHeight();
        if (sw <= 0 || sh <= 0) {
            return;
        }
        Pixmap source = Pixmap.createFromFrameBuffer(sx, sy, sw, sh);
        // glReadPixels rows run bottom-up; flip them into screen order.
        flipVertically(source);
        Pixmap logical = new Pixmap(ScreenMetrics.LOGICAL_WIDTH, ScreenMetrics.LOGICAL_HEIGHT,
                Pixmap.Format.RGBA8888);
        try {
            logical.setFilter(Pixmap.Filter.BiLinear);
            logical.drawPixmap(source, 0, 0, sw, sh, 0, 0, logical.getWidth(),
                    logical.getHeight());
            Texture texture = new Texture(logical);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            battleEntry.setSnapshot(texture);
            entrySnapshotTaken = true;
        } finally {
            source.dispose();
            logical.dispose();
        }
    }

    /** Reverses a pixmap's rows in place (glReadPixels returns them bottom-up). */
    private static void flipVertically(Pixmap pixmap) {
        int height = pixmap.getHeight();
        int stride = pixmap.getWidth() * 4;   // RGBA8888 (createFromFrameBuffer)
        java.nio.ByteBuffer buffer = pixmap.getPixels();
        byte[] top = new byte[stride];
        byte[] bottom = new byte[stride];
        for (int y = 0; y < height / 2; y++) {
            buffer.position(y * stride);
            buffer.get(top, 0, stride);
            buffer.position((height - 1 - y) * stride);
            buffer.get(bottom, 0, stride);
            buffer.position(y * stride);
            buffer.put(bottom, 0, stride);
            buffer.position((height - 1 - y) * stride);
            buffer.put(top, 0, stride);
        }
        buffer.position(0);
    }

    private void capturePauseMap() {
        batch.setProjectionMatrix(camera.combined);
        renderPanorama();
        applyWorldTone();
        for (NeighborView view : neighbours.values()) view.renderer.render(batch, mapCamera, view.offsetX, view.offsetY);
        prepareGroundAnimations();
        if (berryPlants != null) {
            berryPlants.beginRender();
        }
        renderer.render(batch, mapCamera, this::renderWorldDepth);
        renderAnimations();
        batch.setShader(null);
        renderFog(); renderPictures(); renderScreenEffects();
        pauseMenu.captureBackground(viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
    }

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
    /** 308_ItemFindSimple_Scene:2-62: the 184x28 box at the right edge with the item's icon, name and quantity. */
    private void renderItemToasts() {
        if (interpreter == null || messageWindow == null || interpreter.itemToasts().list().isEmpty()) {
            return;
        }
        float screenRight = mapCamera.originX() + mapCamera.viewPixelWidth();
        float screenTop = mapCamera.originY() + mapCamera.viewPixelHeight();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (pokemon.runtime.event.ItemFindToasts.Toast toast : interpreter.itemToasts().list()) {
            float left = screenRight - pokemon.runtime.event.ItemFindToasts.WIDTH;
            float bottom = screenTop - toast.y - pokemon.runtime.event.ItemFindToasts.HEIGHT;
            batch.setColor(0f, 0f, 0f, 64 / 255f);                                 // :25 Color.new(0, 0, 0, 64)
            batch.draw(pixel, left, bottom, pokemon.runtime.event.ItemFindToasts.WIDTH, pokemon.runtime.event.ItemFindToasts.HEIGHT);
            batch.setColor(1f, 1f, 1f, 1f);
            Texture icon = cachedToastIcon(toast.item);
            if (icon != null) {                                                    // :30-33 icon at (x+14, y+14), zoom 0.5
                batch.draw(icon, left + 14f - 12f, bottom + 14f - 12f, 24f, 24f);
            }
            messageWindow.drawToastText(batch, toast.name, left + 28f, bottom + 28f - 3f, false);
            messageWindow.drawToastText(batch, toast.qtyText, screenRight - 2f, bottom + 28f - 3f, true);
        }
        batch.end();
    }

    /**
     * {@code Events.onMapSceneChange} (170_PField_Field:555-600): the map trail moves on, and a map with {@code ShowArea}
     * shows its board unless the player came from a map of the same name ({@code NO_SIGNPOSTS} is empty here).
     */
    private void showSignpost(int previousMapId) {
        int[] trail = gameState.fieldGlobals().mapTrail;
        if (trail[0] != mapData.mapId) {
            if (trail[2] != 0) trail[3] = trail[2];
            if (trail[1] != 0) trail[2] = trail[1];
            if (trail[0] != 0) trail[1] = trail[0];
        }
        trail[0] = mapData.mapId;
        pokemon.runtime.pokemon.PbsData pbs = context.pbsData();
        pokemon.runtime.pokemon.PbsData.Metadata meta = pbs == null ? null : pbs.mapMetadata(mapData.mapId);
        if (previousMapId == mapData.mapId || meta == null || !meta.showArea) {
            return;                                                                  // no map change / :586 MetadataShowArea
        }
        if (trail[1] != 0) {
            for (pokemon.runtime.data.MapInfo info : database.maps()) {
                if (info.mapId == trail[1] && mapData.name != null && mapData.name.equals(info.name)) {
                    return;                                                          // :597-598 the same name as the map before
                }
            }
        }
        java.io.File board = locator.find("Pictures/Location", "town.png");
        Texture boardImage = board == null ? null : textures.load("location:town", board);
        java.io.File seasonFile = locator.find("Pictures/Location", "Spring.png");
        Texture seasonImage = seasonFile == null ? null : textures.load("location:Spring", seasonFile);
        signpost = new LocationSignpost(mapData.name, java.time.LocalDate.now().getMonthValue(),
                boardImage == null ? 66f : boardImage.getHeight(), seasonImage == null ? 50f : seasonImage.getHeight(),
                ScreenMetrics.logicalHeight());
        signpostClock = 0f;
    }

    private Texture locationTexture(String name) {
        java.io.File file = locator == null ? null : locator.find("Pictures/Location", name + ".png");
        return file == null ? null : textures.load("location:" + name, file);
    }

    /** The board (:182-205, :253-285): 40 frames a second, over the map and under the message window. */
    private void renderSignpost() {
        if (pendingSignpostFrom >= 0 && locator != null) {
            int from = pendingSignpostFrom;
            pendingSignpostFrom = -1;
            showSignpost(from);
        }
        if (signpost == null || messageWindow == null) {
            return;
        }
        signpostClock += pokemon.runtime.app.GameSpeed.scale(Gdx.graphics.getDeltaTime());
        while (signpostClock >= 1f / 40f && !signpost.finished()) {
            signpostClock -= 1f / 40f;
            signpost.tick();
        }
        if (signpost.finished()) {
            signpost = null;
            return;
        }
        float left = mapCamera.originX();
        float bottom = mapCamera.originY();
        float height = mapCamera.viewPixelHeight();
        Texture board = locationTexture(signpost.board());
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        String seasonName = signpost.season();
        Texture season = seasonName == null ? null : locationTexture(seasonName);
        if (season != null) {                                                       // :95-109
            batch.draw(season, left, bottom + height - signpost.seasonY() - season.getHeight());
        }
        if (board != null) {
            float top = signpost.windowY();
            batch.draw(board, left, bottom + height - top - board.getHeight());
            com.badlogic.gdx.graphics.Color white = com.badlogic.gdx.graphics.Color.WHITE;
            com.badlogic.gdx.graphics.Color shade = new com.badlogic.gdx.graphics.Color(115 / 255f, 115 / 255f, 115 / 255f, 1f);
            messageWindow.drawShadowText(batch, signpost.name(), left + signpost.textX(),
                    bottom + height - (top + 8f) + 2f, white, shade);               // :189-204
            if (signpost.routeNumber() != null) {                                   // :206-217 the route number graphics
                Texture icons = locationTexture("icon_numbers");
                if (icons != null) {
                    int charWidth = icons.getWidth() / 10;
                    float x = left + signpost.routeNumberX();
                    for (int digit : LocationSignpost.routeDigits(signpost.routeNumber())) {
                        batch.draw(icons, x, bottom + height - (top + 14f) - icons.getHeight(), charWidth,
                                icons.getHeight(), digit * charWidth, 0, charWidth, icons.getHeight(), false, false);
                        x += charWidth;
                    }
                }
            }
        }
        batch.end();
    }

    /** 302_BW_Get_Key_Item:51-80 / :112-190: the white flash, the glowing picture and the icon of pbGetKeyItem. */
    private void renderKeyItem() {
        pokemon.runtime.event.KeyItemAnimation animation = interpreter == null ? null : interpreter.keyItemAnimation();
        if (animation == null) {
            return;
        }
        pokemon.runtime.event.KeyItemAnimation.Frame frame = animation.current();
        float left = mapCamera.originX();
        float bottom = mapCamera.originY();
        float width = mapCamera.viewPixelWidth();
        float height = mapCamera.viewPixelHeight();
        Texture background = pictureTexture("keyitembg");
        Texture icon = keyItemIcon(animation);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (frame.whiteOpacity > 0) {                                         // :30 blackscreen tinted white
            batch.setColor(1f, 1f, 1f, frame.whiteOpacity / 255f);
            batch.draw(pixel, left, bottom, width, height);
        }
        if (background != null && frame.bgOpacity > 0) {                      // :41-47 centred, zoomed
            batch.setColor(1f, 1f, 1f, frame.bgOpacity / 255f);
            float w = background.getWidth(), h = background.getHeight();
            batch.draw(background, left + width / 2f - w / 2f, bottom + height / 2f - h / 2f, w / 2f, h / 2f, w, h,
                    frame.bgZoomX, frame.bgZoomY, 0f, 0, 0, background.getWidth(), background.getHeight(), false, false);
        }
        if (icon != null && frame.itemOpacity > 0) {                          // :60-77 centred, turning
            batch.setColor(1f, 1f, 1f, frame.itemOpacity / 255f);
            float w = icon.getWidth(), h = icon.getHeight();
            batch.draw(icon, left + width / 2f - w / 2f, bottom + height / 2f - h / 2f, w / 2f, h / 2f, w, h,
                    frame.itemZoomX, frame.itemZoomY, frame.itemAngle, 0, 0, icon.getWidth(), icon.getHeight(), false, false);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    private Texture pictureTexture(String name) {
        java.io.File file = locator == null ? null : locator.find("Pictures", name + ".png");
        return file == null ? null : textures.load("pictures:" + name, file);
    }

    /** :54-60 {@code item%03dkey}, else {@code item%03d}; a fake item is the file name itself. */
    private Texture keyItemIcon(pokemon.runtime.event.KeyItemAnimation animation) {
        String name;
        if (animation.fakeIcon != null) {
            name = animation.fakeIcon;
        } else {
            pokemon.runtime.pokemon.PbsData.Item data = context.pbsData() == null ? null : context.pbsData().item(animation.item);
            if (data == null) {
                return null;
            }
            name = String.format("item%03dkey", data.id);
            if (locator == null || locator.find("Icons", name + ".png") == null) {
                name = String.format("item%03d", data.id);
            }
        }
        java.io.File file = locator == null ? null : locator.find("Icons", name + ".png");
        return file == null ? null : textures.load("icons:" + name, file);
    }

    /** Icon lookups touch the disk (File.isFile / listFiles), so each item is resolved once, not every frame. */
    private final java.util.HashMap<String, Texture> toastIcons = new java.util.HashMap<>();

    private Texture cachedToastIcon(String item) {
        if (toastIcons.containsKey(item)) {
            return toastIcons.get(item);
        }
        Texture icon = itemToastIcon(item);
        toastIcons.put(item, icon);
        return icon;
    }

    private Texture itemToastIcon(String item) {
        String name = pokemon.runtime.ui.menu.ItemIcons.name(context.pbsData(), item,
                candidate -> locator != null && locator.find("Icons", candidate + ".png") != null);
        if (name == null) {
            return null;
        }
        java.io.File file = locator.find("Icons", name + ".png");
        return file == null ? null : textures.load("icons:" + name, file);
    }

    private void renderMessageWindow() {
        if (messageWindow == null || interpreter == null || !interpreter.messages().visible()) {
            return;
        }
        warmUpMessagesOnFirstItem();
        messageWindow.prepare(); // skin / cursor textures outside the batch
        messageWindow.textSpeed = context.settings().textspeed;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        messageWindow.render(batch, mapCamera.originX(), mapCamera.originY(),
                mapCamera.viewPixelWidth(), mapCamera.viewPixelHeight());
        batch.end();
    }

    /**
     * PField_Visuals:26-36: the battle start pauses the overworld BGM (so
     * {@code bgm_resume} can bring it back at the same position) and plays the
     * battle music. The BGM itself is chosen by the caller of
     * {@code pbBattleAnimation} - {@code pbGetWildBattleBGM} for a wild battle
     * (PField_Battles:329) and {@code pbGetTrainerBattleBGM} for a trainer one
     * (:503).
     */
    private void beginBattleAudio(pokemon.runtime.battle.InteractiveBattlePort.Session session) {
        pokemon.runtime.audio.AudioManager audio = context.audioManager();
        if (audio == null || session == null) {
            return;
        }
        audio.pauseBgm();
        pokemon.runtime.pokemon.PbsData pbs = context.pbsData();
        int mapId = gameState.currentMapId();
        pokemon.runtime.audio.BattleMusic.Track bgm = session.trainerBattle
                ? pokemon.runtime.audio.BattleMusic.trainerBattleBgm(pbs, mapId,
                        gameState.nextBattleBGM(), session.trainerData)
                : pokemon.runtime.audio.BattleMusic.wildBattleBgm(pbs, mapId,
                        gameState.nextBattleBGM());
        if (bgm != null && bgm.playable()) {
            context.game().log("battle BGM: " + bgm.name);
            audio.playBgm(bgm.name, bgm.volume, bgm.pitch);
        }
    }

    /**
     * PField_Visuals:112-119: when the battle is over the overworld BGM resumes
     * where it stopped, and the pending battle audio is dropped.
     */
    private void endBattleAudio() {
        pokemon.runtime.audio.AudioManager audio = context.audioManager();
        if (audio != null) {
            audio.resumeBgm();
        }
        gameState.clearNextBattleAudio();
    }

    /**
     * Map music comes from the map's own properties (RMXP {@code autoplayBgm} /
     * {@code autoplayBgs}), not from an event command - most maps of this
     * project define their BGM that way.
     */
    private void autoplayMapAudio() {
        if (gameState.fieldGlobals().surfing && context.pbsData() != null) {
            // 170:942-949 pbAutoplayOnTransition: surfing plays the surf music instead of the map's.
            pokemon.runtime.pokemon.PbsData.Metadata global = context.pbsData().globalMetadata();
            pokemon.runtime.audio.BattleMusic.Track surf = global == null ? null
                    : pokemon.runtime.audio.BattleMusic.resolve(global.surfBGM);
            if (surf != null && surf.playable()) {
                context.audioManager().playBgm(surf.name, surf.volume, surf.pitch);
                return;
            }
        }
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
    /**
     * {@code PField_Visuals:110-133} after the battle: {@code viewport.color =
     * Color.new(0,0,0,255)}, then {@code numFrames = 40*4/10} frames of
     * {@code color.alpha -= (255.0/numFrames).ceil}. The battle scene already
     * left the screen black (PokeBattle_Scene:301 pbFadeOutAndHide), so this is
     * the fade back to the overworld.
     */
    private void renderBattleReturnFade() {
        if (battleReturnAlpha < 0) {
            return;
        }
        if (battleReturnAlpha > 0) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            batch.setColor(0f, 0f, 0f, Math.min(1f, battleReturnAlpha / 255f));
            batch.draw(pixel, mapCamera.originX(), mapCamera.originY(),
                    mapCamera.viewPixelWidth(), mapCamera.viewPixelHeight());
            batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            batch.end();
        }
        battleReturnAlpha -= BATTLE_RETURN_ALPHA_STEP;
    }

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

    /** R6.28 grass rustle plus P3 berry moisture, both in map depth order. */
    private void renderWorldDepth(int depth) {
        renderGroundAnimationsBefore(depth);
        if (berryPlants != null) {
            berryPlants.renderMoisture(batch, depth);
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
    /** Settings:350: the dust the ledge jump leaves at its landing tile. */
    private static final int DUST_ANIMATION_ID = 2;
    private final com.badlogic.gdx.utils.IntMap<int[]> lastEventTiles =
            new com.badlogic.gdx.utils.IntMap<>();
    private int[] lastPlayerTile = new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE};
    /** P2: the wild encounter roll for step-based battles. */
    /** {@code $PokemonEncounters} of this map (Events.onMapChange: {@code setup(map_id)}, PField_Field:538). */
    private PokemonEncounters pokemonEncounters;
    private FieldSteps fieldSteps;
    /** The direction the player faced last frame, for {@code Events.onChangeDirection}. */
    private int lastPlayerDirection = -1;
    /** A wild battle waiting for the step's messages to finish. */
    private java.util.List<PokemonEncounters.Encounter> deferredWild;
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
        // The tile changes the moment a step STARTS (MovementController's while
        // loop sets the logical position up front), so this fires once per
        // stepped-on tile - exactly like the plugin's onStepTaken. A jump's
        // step is its landing (Game_Character:833 requires !jumping?), so the
        // trigger waits while the player is airborne.
        if (!player.isJumping()
                && (lastPlayerTile[0] != player.x() || lastPlayerTile[1] != player.y())) {
            lastPlayerTile[0] = player.x();
            lastPlayerTile[1] = player.y();
            rustleAt(player.x(), player.y());
            stepEggs();
            onPlayerStep();
        }
        onPlayerDirection();
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
     * P3: eggs in the party hatch after their species' steps. Called on the same
     * player step as the grass rustle (the project's onStepTaken hook).
     */
    private void stepEggs() {
        if (context.pbsData() != null) {                                      // 181_PField_DayCare:471-519 onStepTaken
            gameState.trainer().dayCare.onStep(context.pbsData(), encounterRandom,
                    gameState.inventory().has("OVALCHARM"), gameState.switches().get(199),
                    j -> gameState.trainer().badges.contains(j));
        }
        stepHatcherEggs();                                                    // 323_Egg_Hatcher:316-340 onStepTaken
        if (gameState.trainer().party.eggCount() == 0) {
            return;
        }
        com.badlogic.gdx.utils.Array<pokemon.runtime.pokemon.Pokemon> hatched =
                gameState.trainer().party.stepEggs();                       // 225_PScreen_EggHatching:218-233
        for (pokemon.runtime.pokemon.Pokemon egg : hatched) {
            pokemon.runtime.pokemon.EggHatching.pbHatch(egg, gameState.trainer(), gameState.currentMapId(),
                    System.currentTimeMillis() / 1000L);                     // :192 pbHatch
            hatchQueue.add(egg);
        }
    }

    /** The eggs that reached zero, hatched one after the other ({@code pbHatchAnimation} blocks the game). */
    private final java.util.ArrayDeque<pokemon.runtime.pokemon.Pokemon> hatchQueue = new java.util.ArrayDeque<>();

    /** 170_PField_Field:180-191 {@code Events.onMapUpdate}: once a day every Pokemon of the party loses a day of Pokerus. */
    private void pokerusDailyCheck() {
        long today = java.time.LocalDate.now().toEpochDay();
        pokemon.runtime.state.FieldGlobals globals = gameState.fieldGlobals();
        if (globals.pokerusDay == today) {
            return;
        }
        for (pokemon.runtime.pokemon.Pokemon pkmn : gameState.trainer().party.members()) {
            if (pkmn != null && !pkmn.egg) {                                  // $Trainer.pokemonParty
                pkmn.lowerPokerusCount();
            }
        }
        globals.pokerusDay = today;
    }

    /** The eggs in the Egg Hatcher lose a step, and one more with Flame Body / Magma Armor in the party (323:316-340). */
    private void stepHatcherEggs() {
        pokemon.runtime.pokemon.TrainerState trainer = gameState.trainer();
        for (int i = 0; i < trainer.hatcherEggs.length; i++) {
            pokemon.runtime.pokemon.Pokemon egg = trainer.hatcherEggs[i];
            if (egg == null || egg.stepsToHatch <= 0) continue;               // :325 next if egg == nil / eggsteps > 0
            egg.stepsToHatch -= 1;
            for (pokemon.runtime.pokemon.Pokemon x : trainer.party.members()) {   // :328 $Trainer.pokemonParty
                if (x == null || x.egg) continue;
                if ("FLAMEBODY".equals(x.ability) || "MAGMAARMOR".equals(x.ability)) {
                    egg.stepsToHatch -= 1;
                    break;
                }
            }
            if (egg.stepsToHatch <= 0) {                                      // :337
                egg.stepsToHatch = 0;
                egg.egg = false;
                pokemon.runtime.pokemon.EggHatching.pbHatch(egg, trainer, gameState.currentMapId(),
                        System.currentTimeMillis() / 1000L);                   // :339 pbHatch(egg)
                hatchQueue.add(egg);
                hatcherSlots.put(egg, i);                                     // then takeEgg(egg, i)
            }
        }
    }

    private final java.util.IdentityHashMap<pokemon.runtime.pokemon.Pokemon, Integer> hatcherSlots = new java.util.IdentityHashMap<>();
    private pokemon.runtime.event.MenuService.Request hatchRequest;
    private pokemon.runtime.pokemon.Pokemon hatchPokemon;

    private void startQueuedHatch() {
        if (hatchRequest != null) {
            if (!hatchRequest.done) {
                return;
            }
            hatchRequest = null;
            Integer slot = hatcherSlots.remove(hatchPokemon);
            if (slot != null && interpreter != null) {                        // the Egg Hatcher's egg: takeEgg(egg, slot)
                interpreter.startTakeEgg(hatchPokemon, slot);
                return;
            }
        }
        if (hatchQueue.isEmpty() || context.menuService().pending() != null
                || (interpreter != null && interpreter.running())) {
            return;
        }
        pokemon.runtime.event.MenuService.Request request =
                new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.HATCH);
        request.pokemon = hatchQueue.poll();
        hatchPokemon = request.pokemon;
        hatchRequest = request;
        context.menuService().submit(request);
    }

    private void ensureFieldSteps() {
        if (pokemonEncounters == null && context.pbsData() != null) {
            pokemonEncounters = new PokemonEncounters(context.pbsData(), gameState, encounterRandom,
                    java.time.LocalTime::now);
            pokemonEncounters.setup(mapData.mapId);                         // Events.onMapChange (PField_Field:538)
            fieldSteps = new FieldSteps(context.pbsData(), gameState, pokemonEncounters, encounterRandom);
        }
    }

    /** {@code pbGetTerrainTag($game_player)}: the bridge only counts while {@code $PokemonGlobal.bridge} is up. */
    private int playerTerrainTag() {
        return tileMap.terrainTag(player.x(), player.y(), false, gameState.bridge());
    }

    /** Whether the plugin would treat the player as busy: a move route forces it, the interpreter or a menu runs. */
    private boolean stepBlocked() {
        return playerRoute != null || (interpreter != null && interpreter.running())
                || (context.messageService() != null && context.messageService().visible());
    }

    /**
     * {@code pbOnStepTaken} (PField_Field:356-377) for the tile the player just stepped on; the encounter roll is
     * {@code pbBattleOnStepTaken} (:488-516) in {@link FieldSteps} / {@link PokemonEncounters}.
     */
    private void onPlayerStep() {
        // 170_PField_Field:361-366: stepping on grass brings the following Pokemon out (the option 自动跟随 = 开)
        int stepTag = playerTerrainTag();
        if (context.settings().autoFollow == 0 && followers != null && followers.follower() != null
                && !gameState.followerToggled()
                && (stepTag == pokemon.runtime.field.PBTerrain.GRASS || stepTag == pokemon.runtime.field.PBTerrain.SOOT_GRASS)) {
            followers.toggle("on", true);
            rebindEntities();
        }
        if (!stepBlocked() && safariStep()) {
            return;                                                           // handled[0] = true
        }
        if (context.battlePort() == null || context.pbsData() == null) {
            return;
        }
        ensureFieldSteps();
        applyStepResult(fieldSteps.onStepTaken(false, stepBlocked(), playerTerrainTag(), false));
    }

    /**
     * {@code Events.onStepTakenTransferPossible} of the Safari Zone (242_PBattle_Safari:77-92): each step in the Zone costs
     * one of its steps; at none left the game is over and the player goes back to the reception.
     *
     * @return true when the step was handled (the encounter roll is skipped)
     */
    private boolean safariStep() {
        pokemon.runtime.state.SafariState safari = gameState.fieldGlobals().safari;
        if (!gameState.fieldGlobals().inSafari(mapData.mapId) || safari.decision != 0
                || pokemon.runtime.state.SafariState.SAFARI_STEPS <= 0) {
            return false;
        }
        safari.steps -= 1;
        if (safari.steps > 0) return false;
        safari.decision = 1;
        startMessagesThenSafariStart("PA:  叮咚！", "PA:  狩猎之旅已结束！");
        return true;
    }

    /**
     * The end of {@code pbSafariBattle} (242_PBattle_Safari:109-134): the balls left, the game over when there are none,
     * and the decision in variable 1.
     */
    private void afterSafariBattle(pokemon.runtime.battle.InteractiveBattlePort port) {
        int balls = port.lastSafariBalls();
        if (balls < 0) return;
        pokemon.runtime.state.SafariState safari = gameState.fieldGlobals().safari;
        safari.ballcount = balls;                                          // :111
        pokemon.runtime.battle.BattleResult result = port.lastResult();
        int decision = result == null ? 0 : result.decision >= 0 ? result.decision
                : result.outcome == pokemon.runtime.battle.BattleResult.Outcome.CAUGHT ? 4
                : result.outcome == pokemon.runtime.battle.BattleResult.Outcome.ESCAPE ? 3 : 0;
        if (balls <= 0) {                                                  // :112
            pendingSafariMessages = new java.util.ArrayList<>();
            if (decision != 2) pendingSafariMessages.add("广播：狩猎球用尽！游戏结束！");   // :113-115
            safari.decision = 1;                                           // :117
            safariGoToStartAfterMessages = true;                           // :118 pbGoToStart (after the messages)
        }
        gameState.variables().set(1, decision);                            // :125 pbSet(1,decision)
    }

    private java.util.List<String> pendingSafariMessages;

    /** {@code pbSafariState.pbGoToStart} (242_PBattle_Safari:25-34): fade out, transfer to the start, facing down. */
    private void safariGoToStart() {
        int[] start = gameState.fieldGlobals().safari.start;
        if (start == null) return;
        context.requestTransfer(start[0], start[1], start[2], 2, 1);
    }

    private void startMessagesThenSafariStart(String... texts) {
        startMessages(java.util.Arrays.asList(texts));
        safariGoToStartAfterMessages = true;
    }

    private boolean safariGoToStartAfterMessages;

    /** {@code Events.onChangeDirection} (PField_Field:381-384): turning on the spot can start an encounter too. */
    private void onPlayerDirection() {
        int direction = player.direction();
        int previous = lastPlayerDirection;
        lastPlayerDirection = direction;
        if (previous < 0 || previous == direction || context.battlePort() == null || context.pbsData() == null) {
            return;
        }
        if (stepBlocked()) {
            return;                                                         // Game_Player:96 !@move_route_forcing && !pbMapInterpreterRunning?
        }
        ensureFieldSteps();
        applyStepResult(fieldSteps.onChangeDirection(playerTerrainTag(), false));
    }

    /** Shows the step's messages, then starts its wild battle. */
    private void applyStepResult(FieldSteps.Result result) {
        if (result.isEmpty()) {
            return;
        }
        if (result.repelRenewal) {
            interpreter.startRepelRenewal();
            if (!result.wild.isEmpty()) {
                deferredWild = result.wild;
            }
            return;
        }
        if (!result.messages.isEmpty()) {
            startMessages(result.messages);
            if (!result.wild.isEmpty()) {
                deferredWild = result.wild;
            }
            return;
        }
        startWildBattle(result.wild);
    }

    private void startMessages(java.util.List<String> texts) {
        Array<pokemon.runtime.data.EventCommand> list = new Array<>();
        int index = 0;
        for (String text : texts) {
            pokemon.runtime.data.EventCommand command = new pokemon.runtime.data.EventCommand();
            command.index = index++;
            command.code = 101;
            command.indent = 0;
            command.parameters = new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.array);
            command.parameters.addChild(new com.badlogic.gdx.utils.JsonValue(text));
            list.add(command);
        }
        interpreter.start(list, mapData.mapId, -1);
    }

    /** {@code pbWildBattle} / {@code pbDoubleWildBattle} (PField_Field:502-504). */
    private void startWildBattle(java.util.List<PokemonEncounters.Encounter> wild) {
        if (wild.isEmpty()) {
            return;
        }
        if (pokemonEncounters != null) {
            pokemonEncounters.clearStepCount();                             // the next steps after a battle are safe
        }
        if (wild.size() > 1 && gameState.fieldGlobals().inSafari(mapData.mapId)) {
            wild = wild.subList(0, 1);                                      // 170_PField_Field:497 !pbInSafari?: never a double battle
        }
        PokemonEncounters.Encounter first = wild.get(0);
        if (Boolean.getBoolean("pokemon.debug.flow")) {
            context.game().log("wild encounter: " + first.species + " L" + first.level
                    + (wild.size() > 1 ? " + " + wild.get(1).species + " L" + wild.get(1).level : ""));
        }
        if (wild.size() > 1) {
            context.battlePort().doubleWildBattle(first.species, first.level, wild.get(1).species, wild.get(1).level);
        } else {
            context.battlePort().wildBattle(first.species, first.level);
        }
        applyBattleEnvironment();
        // The step that triggered this is still sliding (the logical tile moved
        // when the step started). Snap it so the battle opens on the destination
        // tile and the player does not finish the step afterwards.
        player.teleport(player.x(), player.y());
    }

    /**
     * PField_Battles:146-151 + :181-188 + PokeBattle_BattleCommon:104-107: a
     * wild battle opened by walking reads the map's MetadataEnvironment (Dusk
     * Ball time / Dive Ball environment) and the "不可捕捉" switch before the
     * battle scene starts.
     */
    private void applyBattleEnvironment() {
        context.battlePort().setSwitchSource(id -> gameState.switches().get(id));
        context.battlePort().setObtainMap(mapData.mapId);
        context.battlePort().setFatefulEncounter(gameState.switches().get(32));
        pokemon.runtime.pokemon.PbsData.Metadata metadata =
                context.pbsData() == null ? null : context.pbsData().mapMetadata(mapData.mapId);
        context.battlePort().setBattleEnvironment(
                pokemon.runtime.battle.CaptureCalculator.environmentId(
                        metadata == null ? null : metadata.environment));
    }

    @Override
    public void resize(int width, int height) {
        if (battleScreen != null) battleScreen.resize(width, height);
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

    /** The first item ever received asks for the one-off glyph/skin/icon preload; later pickups find everything ready. */
    private void warmUpMessagesOnFirstItem() {
        if (interpreter == null || !interpreter.consumeMessageWarmUpRequest()) {
            return;
        }
        java.util.List<String> names = new java.util.ArrayList<>();
        pokemon.runtime.pokemon.PbsData pbs = context.pbsData();
        if (pbs != null) {
            for (pokemon.runtime.pokemon.PbsData.Item item : pbs.items.values()) {
                names.add(item.name);
                names.add(item.namePlural);
            }
            for (pokemon.runtime.pokemon.PbsData.Move move : pbs.moves.values()) {
                names.add(move.name);
            }
        }
        messageWindow.warmUp(names);
    }

    @Override
    public void dispose() {
        pixel.dispose();
        worldTone.dispose();
        if (darkness != null) {
            darkness.dispose();
            darkness = null;
        }
        if (messageWindow != null) {
            messageWindow.dispose();
        }
        if (pauseMenu != null) {
            pauseMenu.dispose();
            pauseMenu = null;
        }
        if (battleScreen != null) { battleScreen.dispose(); battleScreen = null; }
        batch.dispose();
        textures.dispose();
    }
}
