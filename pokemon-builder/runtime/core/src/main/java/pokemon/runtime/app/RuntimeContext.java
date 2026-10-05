package pokemon.runtime.app;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.battle.BattlePort;
import pokemon.runtime.battle.HeadlessBattlePort;
import pokemon.runtime.data.CommonEventData;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.data.RuntimeDataLocator;
import pokemon.runtime.event.EventInterpreter;
import pokemon.runtime.event.MapPort;
import pokemon.runtime.event.MessageService;
import pokemon.runtime.event.PictureService;
import pokemon.runtime.event.ScreenEffects;
import pokemon.runtime.event.ScriptIr;
import pokemon.runtime.input.DefaultKeyBindings;
import pokemon.runtime.ui.menu.GameSettings;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.input.InputSampler;
import pokemon.runtime.input.KeyStateSource;
import pokemon.runtime.save.SaveManager;
import pokemon.runtime.state.GameState;
import pokemon.runtime.state.Inventory;

import java.io.File;
import java.io.IOException;

/**
 * Shared runtime context (project3 section 8): one object every system can
 * reach, so screens and the event interpreter never need global state.
 */
public final class RuntimeContext {

    private final String version;
    private final PokemonGame game;
    private final String dataRoot;

    private GameDatabase database;
    /** P2: the battle runtime the event interpreter starts battles through. */
    private BattlePort battlePort;
    private final InputManager inputManager = new InputManager();
    private final DefaultKeyBindings keyBindings = new DefaultKeyBindings();
    private final InputSampler inputSampler = new InputSampler(keyBindings);
    private KeyStateSource keySource;
    private final AudioManager audioManager = new AudioManager();
    private final SaveManager saveManager = new SaveManager();
    private final StoragePort storage = new StoragePort();
    private final GameState gameState = new GameState();
    private GameSettings settings = new GameSettings();
    private final MessageService messageService = new MessageService();
    private final PictureService pictureService = new PictureService();
    private final ScreenEffects screenEffects = new ScreenEffects();
    private EventInterpreter eventInterpreter;
    private ScriptIr scriptIr;
    private MapPort mapPort;
    private MapPort screenPort;
    private Transfer pendingTransfer;
    /** Identical warnings are reported once: parallel pages repeat them. */
    private final java.util.Set<String> reportedWarnings = new java.util.HashSet<>();

    public RuntimeContext(String version, PokemonGame game, String dataRoot) {
        this.version = version;
        this.game = game;
        this.dataRoot = dataRoot;
    }

    public String version() {
        return version;
    }

    public PokemonGame game() {
        return game;
    }

    public GameDatabase database() {
        return database;
    }

    public InputManager inputManager() {
        return inputManager;
    }

    public DefaultKeyBindings keyBindings() {
        return keyBindings;
    }

    public void bindKeySource(KeyStateSource source) {
        this.keySource = source;
    }

    /** Samples the bound key source; no-op in headless runs. */
    public void sampleInput() {
        if (keySource != null) {
            inputSampler.sample(keySource, inputManager);
        }
    }

    public AudioManager audioManager() {
        return audioManager;
    }

    public SaveManager saveManager() {
        return saveManager;
    }

    /** P2: the battle runtime (wild / trainer battles from events and steps). */
    public BattlePort battlePort() {
        return battlePort;
    }

    /** P2: the PBS tables, or null before the runtime data has loaded. */
    public pokemon.runtime.pokemon.PbsData pbsData() {
        return database == null ? null : database.pbs();
    }

    public StoragePort storage() {
        return storage;
    }

    /** L1: user options (volumes / fullscreen) loaded from the storage port. */
    public GameSettings settings() {
        return settings;
    }

    /**
     * L1: applies changed options to the audio manager and persists them. The
     * options screen calls this after every adjustment.
     */
    public void settingsChanged() {
        audioManager.setVolumeFactors(settings.bgmFactor(), settings.seFactor(), settings.bgsFactor());
        settings.save(storage);
    }

    /** Live switches / variables / self switches (R5, project3 section 30). */
    public GameState gameState() {
        return gameState;
    }

    /** Message / choice state shared by the interpreter and the window (R6.2). */
    public MessageService messageService() {
        return messageService;
    }

    /** Pictures shown by Show Picture (231/232/235), drawn by PictureLayer. */
    public PictureService pictureService() {
        return pictureService;
    }

    /** Fade / Flash / Shake state drawn by the map screen (R6.4b). */
    public ScreenEffects screenEffects() {
        return screenEffects;
    }

    /** Item counts; the full bag arrives with the stage 3 Pokemon domain. */
    public Inventory inventory() {
        return gameState.inventory();
    }

    /**
     * Map event interpreter. It exists as soon as the runtime config was
     * loaded; without Builder output its common events simply resolve to
     * nothing.
     */
    public EventInterpreter eventInterpreter() {
        return eventInterpreter;
    }

    /**
     * Warning sink of the interpreters. A parallel event page restarts every
     * few frames, so "not implemented / not rendered yet" lines would flood the
     * log; each distinct one is reported once per run. Genuine state messages
     * (items received, switches flipped) always go through.
     */
    void logWarning(String message) {
        if (message == null) {
            return;
        }
        boolean noisy = message.contains("not implemented") || message.contains("not rendered yet")
                || message.contains("not supported") || message.contains("no IR yet");
        if (noisy && !reportedWarnings.add(message)) {
            return;
        }
        if (game != null) {
            game.log(message);
        }
    }

    /**
     * Queues a Transfer Player (201) request; the visible map screen performs
     * the swap at the end of the frame (RMXP semantics).
     */
    public void requestTransfer(int mapId, int x, int y, int direction, int fade) {
        pendingTransfer = new Transfer(mapId, x, y, direction, fade);
    }

    /**
     * Takes the transfer to perform right now. A request without the fade
     * option is ready straight away; one with it (doors use 1) waits until the
     * fade-out reached black, because RMXP blacks the old map out before the
     * swap and fades the new one in. Callers poll this every frame: the request
     * stays queued until the screen is dark enough.
     *
     * @return the transfer to run, or null while the fade is still running
     */
    public Transfer takeReadyTransfer() {
        if (pendingTransfer == null || !swapReady()) {
            return null;
        }
        Transfer ready = pendingTransfer;
        pendingTransfer = null;
        return ready;
    }

    /** True while a Transfer Player request waits for the map swap. */
    public boolean transferPending() {
        return pendingTransfer != null;
    }

    /** Fade option of the queued transfer: 0 = swap at once, 1 = through black. */
    public int pendingTransferFade() {
        return pendingTransfer == null ? 0 : pendingTransfer.fade;
    }

    /** A fade at least this dark counts as a black screen (fade runs 0..255). */
    static final float TRANSFER_BLACK = 250f;

    private boolean swapReady() {
        return pendingTransfer.fade == 0 || screenEffects.fade() >= TRANSFER_BLACK;
    }

    /**
     * Binds the currently visible map screen so route commands (209/210) reach
     * its runtime event characters. Transfers keep going through this context.
     */
    public void bindScreenPort(MapPort port) {
        this.screenPort = port;
    }

    /**
     * R6.28: the interpreter-facing map port. The map screen also uses it for
     * effects the engine triggers on its own (the grass rustle hook), which
     * keeps the R6.14 forwarding trap exercised in the real game.
     */
    public MapPort mapPort() {
        return mapPort;
    }

    /**
     * Loads the Builder manifest and indexes (R2). The base directory is the
     * working directory: POKEMON_RUNTIME_DATA, the pokemon.runtime.data system
     * property, an explicit root and the default generated/ layout are then
     * resolved by RuntimeDataLocator.
     */
    public void loadRuntimeConfig() {
        File baseDir = new File("").getAbsoluteFile();
        try {
            database = GameDatabase.load(dataRoot, baseDir);
        } catch (RuntimeException error) {
            database = null;
            game.log("runtime data unavailable: " + error.getMessage());
        }
        audioManager.attach(database == null ? null : database.audio(),
                database == null ? null : database.dataRoot());
        if (database != null) {
            // P1: rebuilding the saved party / PC storage needs the PBS tables.
            saveManager.attachPbs(database.pbs());
        }
        // L1: options (volumes) load once; a broken/missing file keeps defaults.
        settings = GameSettings.load(storage);
        audioManager.setVolumeFactors(settings.bgmFactor(), settings.seFactor(), settings.bgsFactor());
        mapPort = new MapPort() {
            @Override
            public void transfer(int mapId, int x, int y, int direction) {
                requestTransfer(mapId, x, y, direction, 0);
            }

            @Override
            public void transfer(int mapId, int x, int y, int direction, int fade) {
                requestTransfer(mapId, x, y, direction, fade);
            }

            @Override
            public void setMoveRoute(int eventId, MoveRoute route) {
                if (screenPort != null) {
                    screenPort.setMoveRoute(eventId, route);
                }
            }

            @Override
            public boolean isMoving(int eventId) {
                return screenPort != null && screenPort.isMoving(eventId);
            }

            @Override
            public boolean anyRouteActive() {
                return screenPort != null && screenPort.anyRouteActive();
            }

            @Override
            public void setEventLocation(int eventId, int x, int y, int direction) {
                if (screenPort != null) {
                    screenPort.setEventLocation(eventId, x, y, direction);
                }
            }

            @Override
            public void setTransparent(int eventId, boolean transparent) {
                if (screenPort != null) {
                    screenPort.setTransparent(eventId, transparent);
                }
            }

            @Override
            public void eraseEvent(int eventId) {
                if (screenPort != null) {
                    screenPort.eraseEvent(eventId);
                }
            }

            @Override
            public boolean playerOnCharacter(int characterId) {
                return screenPort != null && screenPort.playerOnCharacter(characterId);
            }

            /** R6.22: 204 / 206 / 203 have to reach the visible screen too. */
            @Override
            public void changeMapSettings(int type, com.badlogic.gdx.utils.JsonValue parameters) {
                if (screenPort != null) {
                    screenPort.changeMapSettings(type, parameters);
                }
            }

            @Override
            public void changeFogOpacity(int opacity) {
                if (screenPort != null) {
                    screenPort.changeFogOpacity(opacity);
                }
            }

            @Override
            public void scrollMap(int direction, int distance, int speed) {
                if (screenPort != null) {
                    screenPort.scrollMap(direction, distance, speed);
                }
            }

            @Override
            public float caveEntrance(boolean exiting) {
                return screenPort == null ? 0f : screenPort.caveEntrance(exiting);
            }

            @Override
            public void showAnimation(int characterId, int animationId) {
                if (screenPort != null) {
                    screenPort.showAnimation(characterId, animationId);
                }
            }

            @Override
            public void showTileAnimation(int animationId, int x, int y, int height) {
                if (screenPort != null) {
                    screenPort.showTileAnimation(animationId, x, y, height);
                }
            }

            // R6.30: the R6.14 trap again - every new hook has to forward.
            @Override
            public float exclaim(int characterId, int animationId) {
                return screenPort == null ? 0f : screenPort.exclaim(characterId, animationId);
            }

            // L6: pbNoticePlayer (facing check + exclaim + walk over).
            @Override
            public float noticePlayer(int characterId) {
                return screenPort == null ? 0f : screenPort.noticePlayer(characterId);
            }

            // L6: pbSave (quiet save to the quick slot).
            @Override
            public boolean saveGame() {
                return screenPort != null && screenPort.saveGame();
            }

            @Override
            public float pushBoulder(int eventId) {
                return screenPort == null ? 0f : screenPort.pushBoulder(eventId);
            }

            @Override
            public void togglePlateSwitches() {
                if (screenPort != null) {
                    screenPort.togglePlateSwitches();
                }
            }
        };
        eventInterpreter = new EventInterpreter(gameState, messageService, inputManager,
                audioManager, this::commonEventCommands, mapPort, pictureService,
                this::logWarning);
        eventInterpreter.attachScreenEffects(screenEffects);
        eventInterpreter.attachInventory(gameState.inventory());
        if (database != null) {
            // P0c: the Pokemon construction IR commands need the PBS data.
            eventInterpreter.attachPbs(database.pbs());
        }
        battlePort = new HeadlessBattlePort(gameState.trainer(),
                () -> database == null ? null : database.pbs(),
                this::whiteOut, new java.util.Random());
        eventInterpreter.attachBattlePort(battlePort);
        if (database != null && database.system() != null) {
            // Named switches ("s:...") are script expressions, not booleans.
            gameState.switchNames(database.system().switches);
        }
        gameState.switchWarnings(this::logWarning);
        loadScriptIr(database);
    }

    /**
     * Loads the IR the Builder compiled (R7.1/R7.2): without it every script
     * block reports "has no IR yet", so the missing artifact is named here.
     */
    private void loadScriptIr(GameDatabase current) {
        if (current == null) {
            return;
        }
        File file = new File(current.dataRoot(), "scripts" + File.separator + "ir.json");
        if (!file.isFile()) {
            game.log("script IR not found (" + file.getAbsolutePath()
                    + "); compile it with: node builder/src/script-compile.js");
            return;
        }
        try {
            scriptIr = ScriptIr.load(file);
            eventInterpreter.attachScriptIr(scriptIr);
            game.log("script IR loaded: " + scriptIr.size() + " commands");
        } catch (IOException | RuntimeException error) {
            game.log("script IR unreadable: " + error.getMessage());
        }
    }

    /**
     * P2: a battle loss. The party is healed and, when the PokeCenter point is
     * known (pbSetPokemonCenter), the player warps back to it; without a point
     * the player simply keeps the map and walks on.
     */
    private void whiteOut() {
        gameState.trainer().healParty();
        if (gameState.trainer().hasPokemonCenter()) {
            game.log("white-out: warping to the PokeCenter");
            requestTransfer(gameState.trainer().healMapId, gameState.trainer().healX,
                    gameState.trainer().healY, gameState.trainer().healDirection, 0);
        } else {
            game.log("white-out: no PokeCenter set; the party was healed");
        }
    }

    /** Common event lookup for CALL_COMMON_EVENT (project3 section 27). */
    private Array<EventCommand> commonEventCommands(int id) {
        GameDatabase current = database;
        if (current == null) {
            return null;
        }
        CommonEventData commonEvent = current.commonEvent(id);
        return commonEvent == null ? null : commonEvent.commands;
    }

    /**
     * A fresh interpreter for one parallel event page (trigger 4). It shares
     * every service with the main interpreter - state, messages, input, audio,
     * pictures, screen effects, the map port and the compiled script IR - so a
     * parallel page behaves exactly like the same page run by the player.
     */
    public EventInterpreter newEventInterpreter() {
        EventInterpreter interpreter = new EventInterpreter(gameState, messageService, inputManager,
                audioManager, this::commonEventCommands, mapPort, pictureService,
                this::logWarning);
        interpreter.attachScreenEffects(screenEffects);
        interpreter.attachInventory(gameState.inventory());
        interpreter.attachScriptIr(scriptIr);
        if (database != null) {
            interpreter.attachPbs(database.pbs());
        }
        if (battlePort == null) {
            battlePort = new HeadlessBattlePort(gameState.trainer(),
                    () -> database == null ? null : database.pbs(),
                    this::whiteOut, new java.util.Random());
        }
        interpreter.attachBattlePort(battlePort);
        return interpreter;
    }

    /** One pending Transfer Player request. */
    public static final class Transfer {
        public final int mapId;
        public final int x;
        public final int y;
        public final int direction;
        public final int fade;

        Transfer(int mapId, int x, int y, int direction, int fade) {
            this.mapId = mapId;
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.fade = fade;
        }
    }
}
