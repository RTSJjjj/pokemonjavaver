package pokemon.runtime.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.battle.BattleScreen;
import pokemon.runtime.battle.BattleSprites;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.map.MapScreen;
import pokemon.runtime.state.GameState;
import pokemon.runtime.ui.menu.PauseMenuOverlay;
import pokemon.runtime.ui.menu.TitleScreen;

import java.io.File;
import java.nio.file.Files;

/**
 * L1 evidence: renders the production title screen and pause menu in a hidden
 * window and writes PNGs (one per state). Arguments: dataRoot outputDir.
 *
 * <p>The music is disabled and user.home points at a scratch directory, so the
 * capture never touches the player's real saves.</p>
 */
public final class MenuCapture extends ApplicationAdapter {

    private final String[] args;
    private Throwable failure;
    private FrameBuffer buffer;
    private SpriteBatch batch;
    private OrthographicCamera projection;
    private RuntimeContext context;
    private TitleScreen title;
    private PauseMenuOverlay overlay;
    private MapScreen mapScreen;
    private int width = ScreenMetrics.LOGICAL_WIDTH;
    private int height = ScreenMetrics.LOGICAL_HEIGHT;

    /**
     * {@code -Dpokemon.capture.perf=1} (or the {@code POKEMON_CAPTURE_PERF}
     * environment variable, which a Gradle {@code JavaExec} always inherits):
     * print the frame / world-update / draw-order counters while capturing.
     *
     * <p>Off by default - with the flag off every path below is the one the
     * capture always took, and the log is unchanged.</p>
     */
    private static final boolean PERF = System.getProperty("pokemon.capture.perf") != null
            || System.getenv("POKEMON_CAPTURE_PERF") != null;
    /** Frames between two perf lines (60 = roughly one second at 60 fps). */
    private static final int PERF_REPORT_FRAMES = 60;

    private int perfFrames;
    private long perfWorldNanos;
    private long perfMenuDrawCalls;
    private long perfLastOrderCalls;
    private long perfLastOrderSorts;
    private long perfLastOrderReuses;
    private long perfLastMenuDrawCalls;

    private MenuCapture(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("dataRoot outputDir");
        }
        if (args.length > 2 && ("mart".equals(args[2]) || "chapter".equals(args[2]) || "battleinfo".equals(args[2]) || "transform".equals(args[2]) || "illusion".equals(args[2]) || "starter".equals(args[2]) || "items".equals(args[2]) || "storage".equals(args[2]) || "pcitems".equals(args[2]) || "slots".equals(args[2]) || "hall".equals(args[2]) || "credits".equals(args[2]) || "hatch".equals(args[2]) || "relearn".equals(args[2]) || "hatcher".equals(args[2]) || "mining".equals(args[2]) || "voltorb".equals(args[2]) || "triad".equals(args[2]) || "fastcatch".equals(args[2]) || "quests".equals(args[2]) || "habitat".equals(args[2]) || "weather".equals(args[2]) || "ready".equals(args[2]) || "minimap".equals(args[2]))) {
            System.setProperty("pokemon.menu.clockDelta", "0.025");
        }
        // Keep the capture away from the real save directory.
        File homes = Files.createTempDirectory("pb-menu-capture-").toFile();
        System.setProperty("user.home", homes.getAbsolutePath());

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(672, 448);
        config.disableAudio(true);
        MenuCapture capture = new MenuCapture(args);
        new Lwjgl3Application(capture, config);
        if (capture.failure != null) {
            throw new RuntimeException("menu capture failed", capture.failure);
        }
    }

    @Override
    public void create() {
        try {
            if (PERF) {
                BattleSprites.perfReset();
                System.out.println("perf counters enabled (-Dpokemon.capture.perf=1)");
            }
            pokemon.runtime.app.PokemonGame game =
                    new pokemon.runtime.app.PokemonGame(args[0], -1, null);
            context = new RuntimeContext("capture", game, args[0]);
            context.loadRuntimeConfig();
            if (context.database() == null) {
                throw new IllegalStateException("runtime data unavailable: " + args[0]);
            }
            int[] screen = pokemon.runtime.data.ProjectInfo.screenSize(new File(args[0]));
            if (screen != null) {
                ScreenMetrics.configure(screen[0], screen[1]);
            }
            width = ScreenMetrics.logicalWidth();
            height = ScreenMetrics.logicalHeight();

            // A save slot so the load screen shows real content.
            GameState state = context.gameState();
            state.enterMap(2, 9, 22);
            state.playerName("小测");
            state.playerId(0);
            // The Pokedex / PC entries need $Trainer.pokedex / $Trainer.pokepc
            // (Modular Menu:67/82), like a real save after the intro events.
            state.trainer().pokedex = true;
            state.trainer().pokepc = true;
            state.switches().set(1, true);
            state.inventory().add("POTION", 5);
            state.inventory().add("SUPERPOTION", 2);
            state.inventory().add("ANTIDOTE", 3);
            state.inventory().add("POKEBALL", 10);
            state.inventory().add("REPEL", 1);
            // A party + a player id so the start menu shows its walking char and
            // the icon row (004_MSF_UI_Load's pbSetParty).
            pokemon.runtime.pokemon.PbsData pbs = context.pbsData();
            if (pbs != null) {
                for (String name : new String[] { "BULBASAUR", "CHARMANDER", "SQUIRTLE" }) {
                    pokemon.runtime.pokemon.PbsData.Species species = pbs.species(name);
                    if (species != null) {
                        pokemon.runtime.pokemon.Pokemon mon = new pokemon.runtime.pokemon.Pokemon(species, 5, pbs);
                        mon.gender = 0; // male, so the party screen draws the gender icon
                        if (pbs.natures.size > 0) {
                            mon.nature = pbs.natures.get(species.id % pbs.natures.size);
                        }
                        mon.originalTrainer = "小测";
                        mon.publicID = 12345;
                        mon.personalID = 1000 + species.id;
                        mon.otGender = 0;
                        mon.ballused = 4;
                        mon.markings = 0b0101;
                        mon.obtainLevel = 5;
                        mon.obtainMode = 0;
                        mon.exp = mon.experience() + 15; // a visible exp bar
                        state.trainer().addToParty(mon);
                    }
                }
                pokemon.runtime.pokemon.Pokemon lead = state.trainer().party.members().size > 0
                        ? state.trainer().party.members().get(0) : null;
                if (lead != null) {
                    lead.item = "POTION";
                    lead.ribbons.add("1");
                    lead.ribbons.add("48");
                }
                for (pokemon.runtime.pokemon.Pokemon member : state.trainer().party.members()) {
                    if (member != lead) {
                        member.item = "SUPERPOTION";
                        member.ribbons.add("1");
                        member.ribbons.add("48");
                    }
                }
                for (String seenName : new String[] {"PIKACHU", "CATERPIE", "PIDGEY", "RATTATA", "SPEAROW"}) {
                    pokemon.runtime.pokemon.PbsData.Species seen = pbs.species(seenName);
                    if (seen != null) {
                        state.trainer().seen.add(seen.internalName);
                    }
                }
                // A couple of Pokemon in the PC box so the storage screen shows content.
                for (String boxName : new String[] {"PIKACHU", "CATERPIE", "PIDGEY"}) {
                    pokemon.runtime.pokemon.PbsData.Species boxSpecies = pbs.species(boxName);
                    if (boxSpecies != null) {
                        pokemon.runtime.pokemon.Pokemon mon = new pokemon.runtime.pokemon.Pokemon(boxSpecies, 8, pbs);
                        mon.nature = pbs.natures.size > 0 ? pbs.natures.get(boxSpecies.id % pbs.natures.size) : null;
                        mon.originalTrainer = "小测";
                        mon.publicID = 12345;
                        state.trainer().currentStorage().store(mon);
                    }
                }
            }
            context.saveManager().save(context.storage(), "1", state);

            GraphicsLocator locator = new GraphicsLocator(context.database());
            batch = new SpriteBatch();
            projection = new OrthographicCamera(width, height);
            projection.position.set(width / 2f, height / 2f, 0f);
            projection.update();
            buffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);

            if (args.length > 2 && "preload".equals(args[2])) {                 // P4: the loading screen
                pokemon.runtime.ui.menu.PreloadScreen loading = new pokemon.runtime.ui.menu.PreloadScreen(context,
                        () -> new com.badlogic.gdx.ScreenAdapter() { });
                loading.resize(width, height);
                for (int frame = 0; frame < 400; frame++) {
                    loading.render(1f / 40f);
                    if (frame == 0 || frame == 12 || frame == 40 || frame == 120) {
                        buffer.begin();
                        loading.render(0f);
                        writeShot("preload-" + frame);
                    }
                }
                return;
            }
            title = new TitleScreen(context);
            title.resize(width, height); // not driven by Game#setScreen here
            // Splash (no show(): the capture stays silent).
            GraphicsLocator probe = locator;
            System.out.println("title probe: splash="
                    + context.database().title().splashImages.size
                    + " seconds=" + context.database().title().secondsPerSplash
                    + " intro1=" + probe.find("Titles", "intro1.png")
                    + " start=" + probe.find("Titles", "start.png")
                    + " mpmbg=" + probe.find("Pictures/MPM", "bg.png"));
            title.render(0.5f); // let the first slide fade in fully
            shot("l14-title-splash");

            tapTitle(GameAction.CONFIRM); // skip the splash -> the white intro fade
            title.render(0.1f);           // a few frames into the fade
            shot("l14-title-intro");

            tapTitle(GameAction.CONFIRM); // skip the intro -> the full title
            shot("l14-title-main");

            tapTitle(GameAction.CONFIRM); // command window (继续/新的故事/设置/退出)
            shot("l14-title-commands");

            // Continue -> load view (the seeded slot).
            tapTitle(GameAction.UP);      // 继续之前的故事
            tapTitle(GameAction.CONFIRM);
            shot("l14-title-load");

            // Pause menu overlay on its own (no map screen needed for visuals).
            overlay = new PauseMenuOverlay(context, locator);
            if (args.length > 2 && "mart".equals(args[2])) {
                captureMart();       // roadmap stage 6.1: the Poke Mart screens
                Gdx.app.exit();
                return;
            }
            if (args.length > 2 && "pcitems".equals(args[2])) {
                pokemon.runtime.event.MenuService.Request pc =
                        new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.STORAGE);
                context.gameState().inventory().add("ETHER", 7);
                overlay.openRequest(pc);
                idle(150);
                tapAndRender(GameAction.CONFIRM, overlay);        // the first line of the open message
                idle(150);
                tapAndRender(GameAction.DOWN, overlay);           // the trainer's PC
                tapAndRender(GameAction.CONFIRM, overlay);
                idle(200);
                tapAndRender(GameAction.CONFIRM, overlay);        // 访问了电脑
                idle(150);
                renderOverlay();
                shot("pcitems-1-menu");
                tapAndRender(GameAction.CONFIRM, overlay);        // 整理道具
                idle(100);
                renderOverlay();
                shot("pcitems-2-help");
                tapAndRender(GameAction.CONFIRM, overlay);        // 取出道具
                idle(20);
                renderOverlay();
                shot("pcitems-3-withdraw");
                Gdx.app.exit();
                return;
            }
            if (args.length > 2 && "storage".equals(args[2])) {
                overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.STORAGE);
                idle(40);
                renderOverlay();
                shot("storage-1-open");
                tapAndRender(GameAction.CONFIRM, overlay);
                idle(40);
                renderOverlay();
                shot("storage-2-after-confirm");
                Gdx.app.exit();
                return;
            }
            if (args.length > 2 && "items".equals(args[2])) {
                captureItems();      // roadmap stage 5: items used from the bag on the party screen
                Gdx.app.exit();
                return;
            }
            if (args.length > 2 && ("slots".equals(args[2]) || "hall".equals(args[2]) || "credits".equals(args[2])
                    || "hatch".equals(args[2]) || "relearn".equals(args[2]) || "hatcher".equals(args[2])
                    || "mining".equals(args[2]) || "voltorb".equals(args[2]) || "triad".equals(args[2]) || "headnames".equals(args[2]) || "fastcatch".equals(args[2]) || "quests".equals(args[2]) || "fishing".equals(args[2]) || "ropes".equals(args[2]) || "nexus".equals(args[2]) || "eon".equals(args[2]) || "chapter".equals(args[2]) || "battleinfo".equals(args[2]) || "transform".equals(args[2]) || "illusion".equals(args[2]) || "habitat".equals(args[2]) || "weather".equals(args[2]) || "ready".equals(args[2]) || "minimap".equals(args[2]))) {
                captureScenes(args[2]);                    // roadmap stage 8 / 12: the scenes added after the starter
                Gdx.app.exit();
                return;
            }
            if (args.length > 2 && "starter".equals(args[2])) {
                captureStarter();    // roadmap stage 8.4: the starter selection scene
                Gdx.app.exit();
                return;
            }
            overlay.open();
            renderOverlay();
            shot("l1-menu-main");

            overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.SAVE);
            renderOverlay();
            shot("l1-menu-save");
            tapAndRender(GameAction.CANCEL, overlay);

            overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.LOAD);
            renderOverlay();
            shot("l1-menu-load");
            tapAndRender(GameAction.CANCEL, overlay);

            overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.OPTIONS);
            renderOverlay();
            shot("l1-menu-options");
            tapAndRender(GameAction.CANCEL, overlay);

            overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.TRAINER);
            renderOverlay();
            shot("l1-menu-trainer");
            tapAndRender(GameAction.CONFIRM, overlay);
            renderOverlay();
            shot("l1-menu-trainer-badges");
            tapAndRender(GameAction.CANCEL, overlay);

            // 背包 (BW Bag).
            overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.BAG);
            renderOverlay();
            shot("l1-bag");
            tapAndRender(GameAction.DOWN, overlay);
            renderOverlay();
            shot("l1-bag-scrolled");
            tapAndRender(GameAction.RIGHT, overlay); // next pocket
            renderOverlay();
            shot("l1-bag-pocket2");
            tapAndRender(GameAction.CONFIRM, overlay); // open the command window
            renderOverlay();
            shot("l1-bag-action");

            // 宝可梦菜单 (PScreen_Party).
            overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.PARTY);
            renderOverlay();
            shot("l1-party");
            tapAndRender(GameAction.RIGHT, overlay);
            renderOverlay();
            shot("l1-party2");

            // 查看详情 (BW PScreen_Summary).
            tapAndRender(GameAction.CONFIRM, overlay); // command window (查看详情)
            renderOverlay();
            shot("l1-party-action");
            tapAndRender(GameAction.CONFIRM, overlay); // open the summary (page 1)
            renderOverlay();
            shot("l1-summary-1");
            tapAndRender(GameAction.RIGHT, overlay);   // page 2
            renderOverlay();
            shot("l1-summary-2");
            tapAndRender(GameAction.RIGHT, overlay);   // page 3
            renderOverlay();
            shot("l1-summary-3");
            tapAndRender(GameAction.RIGHT, overlay);   // page 4
            renderOverlay();
            shot("l1-summary-4");
            tapAndRender(GameAction.RIGHT, overlay);   // page 5
            renderOverlay();
            shot("l1-summary-5");
            // page 5 Z -> ribbon selection (pbRibbonSelection).
            tapAndRender(GameAction.CONFIRM, overlay);
            renderOverlay();
            shot("l1-summary-ribbon");
            tapAndRender(GameAction.CANCEL, overlay);
            // page 4 Z -> move detail (pbMoveSelection).
            tapAndRender(GameAction.LEFT, overlay);
            tapAndRender(GameAction.CONFIRM, overlay);
            renderOverlay();
            shot("l1-summary-move");
            tapAndRender(GameAction.DOWN, overlay);
            renderOverlay();
            shot("l1-summary-move2");
            tapAndRender(GameAction.CANCEL, overlay);
            // pages 1..3 Z -> pbOptions / pbMarking.
            tapAndRender(GameAction.LEFT, overlay);
            tapAndRender(GameAction.LEFT, overlay);
            tapAndRender(GameAction.LEFT, overlay);
            tapAndRender(GameAction.CONFIRM, overlay);
            renderOverlay();
            shot("l1-summary-options");
            // 携带道具 -> the bag in "choose a held item" mode.
            tapAndRender(GameAction.CONFIRM, overlay);
            renderOverlay();
            shot("l1-summary-hold");
            tapAndRender(GameAction.CONFIRM, overlay); // give the item, back to the summary
            // reopen the options and pick 标记 (index 2).
            tapAndRender(GameAction.CONFIRM, overlay);
            tapAndRender(GameAction.DOWN, overlay);
            tapAndRender(GameAction.DOWN, overlay);
            tapAndRender(GameAction.CONFIRM, overlay);
            renderOverlay();
            shot("l1-summary-marking");

            // 图鉴 (BW PokedexMain + PokedexEntry).
            overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.POKEDEX);
            renderOverlay();
            shot("l1-dex");
            tapAndRender(GameAction.CONFIRM, overlay);   // entry page 1 (0001, owned)
            renderOverlay();
            shot("l1-dex-entry1");
            tapAndRender(GameAction.RIGHT, overlay);     // page 2 Data
            renderOverlay();
            shot("l1-dex-entry2");
            tapAndRender(GameAction.RIGHT, overlay);     // page 3 Evolution
            renderOverlay();
            shot("l1-dex-entry3");
            tapAndRender(GameAction.RIGHT, overlay);     // page 4 Forms
            renderOverlay();
            shot("l1-dex-entry4");
            tapAndRender(GameAction.RIGHT, overlay);     // page 5 Area
            renderOverlay();
            shot("l1-dex-entry5");
            tapAndRender(GameAction.CANCEL, overlay);    // back to the list
            tapAndRender(GameAction.DOWN, overlay);
            tapAndRender(GameAction.DOWN, overlay);
            renderOverlay();
            shot("l1-dex2");

            // PC (BW B2W2 PC).
            overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.STORAGE);
            renderOverlay();
            shot("l1-pc");
            tapAndRender(GameAction.CONFIRM, overlay);   // command window
            renderOverlay();
            shot("l1-pc-command");
            tapAndRender(GameAction.CANCEL, overlay);
            tapAndRender(GameAction.RIGHT, overlay);
            tapAndRender(GameAction.RIGHT, overlay);
            renderOverlay();
            shot("l1-pc-select");

            // P0d: the region map behind pbShowMap (PScreen_RegionMap:431-437):
            // mapbg + the centered region image + the cursor + the player head.
            overlay.openRequest(new pokemon.runtime.event.MenuService.Request(
                    pokemon.runtime.event.MenuService.Kind.SHOW_MAP));
            renderOverlay();
            shot("l1-regionmap");
            tapAndRender(GameAction.SHOULDER_RIGHT, overlay); // next region
            renderOverlay();
            shot("l1-regionmap2");
            tapAndRender(GameAction.CANCEL, overlay);

            // Integration: a real MapScreen - X opens the pause menu; walking to
            // 读档 and confirming swaps screens (the host disposes the old
            // screen from inside the overlay update - the crash-prone path).
            mapScreen = new MapScreen(context, 2);
            context.game().setScreen(mapScreen);
            stepMap(GameAction.MENU);        // open the menu
            shotMap("l1-map-menu");
            stepMap(GameAction.DOWN);        // 保存
            stepMap(GameAction.DOWN);        // 读档
            stepMap(GameAction.CONFIRM);     // open the load view
            shotMap("l1-map-menu-load");
            stepMap(GameAction.CONFIRM);     // load slot 1 -> screen swap
            shotMap("l1-map-after-load");

            // P3: the berry plant sprites on Route 1 (map 10): event 10 is a
            // sprouting CHERIBERRY (wet), event 11 a ripe ORANBERRY (damp),
            // event 12 an empty plot (hidden).
            if (pbs != null && pbs.item("CHERIBERRY") != null && pbs.item("ORANBERRY") != null) {
                int now = (int) pokemon.runtime.field.BerryPlants.now();
                context.gameState().eventVars().set(10, 10,
                        new int[] { 2, pbs.item("CHERIBERRY").id, 0, now, 60, 0, 0, 0 });
                context.gameState().eventVars().set(10, 11,
                        new int[] { 5, pbs.item("ORANBERRY").id, 0, now, 20, 0, 0, 0 });
                context.gameState().eventVars().set(10, 12, new int[8]);
                mapScreen = new MapScreen(context, 10, 30, 24, 8);
                context.game().setScreen(mapScreen);
                shotMap("l1-berry");
                // A stage change (2 -> 3) plays PLANT_SPARKLE_ANIMATION_ID = 7.
                context.gameState().eventVars().set(10, 10,
                        new int[] { 3, pbs.item("CHERIBERRY").id, 0, now, 60, 0, 0, 0 });
                shotMap("l1-berry-sparkle");

                // The whole planting interaction on the empty plot (event 10,
                // page 2): the player plants an ORANBERRY and the sprout must
                // appear right after the message, without leaving the map.
                context.gameState().selfSwitches().set(10, 10, "A", true);
                context.gameState().eventVars().set(10, 10, new int[8]);
                context.gameState().inventory().add("ORANBERRY", 2);
                mapScreen = new MapScreen(context, 10, 29, 23, 8);
                context.game().setScreen(mapScreen);
                shotMap("l1-berry-empty");
                stepMap(GameAction.CONFIRM);   // page 2 -> BERRY_PLANT menu
                shotMap("l1-berry-menu");
                stepMap(GameAction.DOWN);      // 种植
                stepMap(GameAction.CONFIRM);   // open the filtered bag
                shotMap("l1-berry-bag");
                stepMap(GameAction.CONFIRM);   // pick ORANBERRY
                shotMap("l1-berry-planted-msg"); // "橙橙果种在了土里。"
                stepMap(GameAction.CONFIRM);   // close the message -> setVariable + turn
                renderMap();                   // the plant sprite picks up the new variable
                // The sprout sits on the plot right in front of the hero; take
                // the shot from a fresh screen a few tiles back so it is visible.
                mapScreen = new MapScreen(context, 10, 29, 27, 8);
                context.game().setScreen(mapScreen);
                shotMap("l1-berry-planted");   // the sprout must be there now
                if (!"berrytreeplanted".equals(mapScreen.eventSheet(10))) {
                    throw new IllegalStateException("berry planting did not reach the event sheet: "
                            + mapScreen.eventSheet(10));
                }
            }

            // P2d: the real battle scene (Battlebacks + Pictures/Battle HUD).
            // Scene_Initialize:15 pbStartBattle plays the intro first, then
            // Battle_StartAndEnd:194 sends the battlers out (Scene_Animations:85).
            boolean trainerCapture = args.length > 2 && "trainer".equals(args[2]);
            boolean transitionCapture = args.length > 2 && "transition".equals(args[2]);
            boolean transitionTrainerCapture = args.length > 2 && "transition-trainer".equals(args[2]);
            boolean switchCapture = args.length > 2 && "switch".equals(args[2]);
            // B2: a trainer with two Pokemon, so the first faint makes the
            // opponent send out a replacement (Battle_Action_Switching:165-239).
            boolean multiTrainerCapture = args.length > 2 && ("trainer2".equals(args[2]) || "trainer2-switch".equals(args[2]) || "trainer2-faint".equals(args[2]));
            // The opposing trainer voluntarily switches in the first round (what the trainer AI does).
            // The opposing trainer's strong lead knocks the player's 1 HP lead out (trainer form of pbEORSwitch, :205-208).
            boolean trainerFaintCapture = args.length > 2 && "trainer2-faint".equals(args[2]);
            boolean foeSwitchCapture = args.length > 2 && "trainer2-switch".equals(args[2]);
            // B2: the player's lead faints in a wild battle, so pbEORSwitch asks
            // "要更换宝可梦吗？" and then replaces it (:221-231).
            boolean faintCapture = args.length > 2 && "faint".equals(args[2]);
            // B6 (§8.1 / §8.2): a wild battle whose lead is prepared so the data
            // box status icon and the fight menu's 0 PP slot can be inspected.
            boolean fightStatusCapture = args.length > 2 && "fight-status".equals(args[2]);
            boolean fightNoPpCapture = args.length > 2 && "fight-0pp".equals(args[2]);
            // B7: a wild battle played through one move and sampled frame by frame,
            // so the damage flash (PokeBattle_SceneAnimations:610-641) and the HP
            // bar animation (Scene_Animations:239-268) can be measured.
            boolean damageCapture = args.length > 2 && "damage".equals(args[2]);
            // Move animations: a wild battle played through the lead's first move, sampled
            // while PBAnimationPlayerX runs (Scene_Animations:542-586).
            boolean moveAnimCapture = args.length > 2 && "move-anim".equals(args[2]);
            // Doubles: a wild double battle whose two player battlers choose one after the other
            // (Battle_Phase_Command:197-263), with the target menu (Scene_Commands:419-474).
            boolean doubleCapture = args.length > 2 && "double".equals(args[2]);
            // Two opposing trainers and a partner (pbDoubleTrainerBattle + a partner trainer), played to the end with CONFIRM.
            boolean doubleTrainerCapture = args.length > 2 && "double-trainer".equals(args[2]);
            // Three opposing trainers ("triple-trainer") or a wild triple ("triple"), played with CONFIRM.
            boolean tripleTrainerCapture = args.length > 2 && "triple-trainer".equals(args[2]);
            boolean tripleWildCapture = args.length > 2 && "triple".equals(args[2]);
            // The boss battle: the player's three against one boss ("3v1").
            boolean tripleBossCapture = args.length > 2 && "triple-boss".equals(args[2]);
            // A wild double battle in which the first battler switches and the second attacks ("double-switch"),
            // or the first battler uses a Potion and the second a Poke Ball ("double-bag").
            boolean doubleSwitchCapture = args.length > 2 && "double-switch".equals(args[2]);
            boolean doubleBagCapture = args.length > 2 && "double-bag".equals(args[2]);
            // The second battler faints at the end of the round, so pbEORSwitch asks for its replacement.
            boolean doubleFaintCapture = args.length > 2 && "double-faint".equals(args[2]);
            // The Safari Zone: a Safari battle played with Bait, Rock and Balls (160_PokeBattle_SafariZone).
            boolean safariCapture = args.length > 2 && args[2].startsWith("safari");
            if (multiTrainerCapture || faintCapture) {
                pokemon.runtime.pokemon.PbsData.TrainerData opponent = null;
                if (multiTrainerCapture && pbs != null) {
                    for (com.badlogic.gdx.utils.ObjectMap.Entry<String, pokemon.runtime.pokemon.PbsData.TrainerData> entry : pbs.trainers) {
                        if (entry.value.party.size < 2 || entry.value.type == null) continue;
                        pokemon.runtime.pokemon.PbsData.TrainerType type = pbs.trainerTypes.get(entry.value.type);
                        if (type == null) continue;
                        if (locator.find("Trainers", "trainer" + type.internalName + ".png") == null
                                && locator.find("Trainers",
                                        String.format(java.util.Locale.ROOT, "trainer%03d.png", type.id)) == null) continue;
                        // The lead must be hurtable by the player's first move.
                        pokemon.runtime.pokemon.PbsData.Species species =
                                pbs.species(entry.value.party.first().species);
                        if (species == null || "GHOST".equals(species.type(0))
                                || "GHOST".equals(species.type(1))) continue;
                        opponent = entry.value;
                        break;
                    }
                }
                if (multiTrainerCapture && opponent == null) {
                    throw new IllegalStateException("no 2-Pokemon trainer for the capture");
                }
                if (multiTrainerCapture) {
                    // Keep the foes weak so the player's lead knocks the first one
                    // out and the opposing side has to send out its replacement.
                    for (pokemon.runtime.pokemon.PbsData.TrainerPokemon member : opponent.party) {
                        member.level = trainerFaintCapture ? 100 : 5;
                    }
                    // ... and level the player's lead up before the battle starts.
                    if (trainerFaintCapture) {
                        pinFaintCaptureLeadToOneHp();
                    } else if (context.gameState().trainer().partyCount() > 0) {
                        pokemon.runtime.pokemon.Pokemon lead =
                                context.gameState().trainer().party.members().get(0);
                        lead.level = 100;
                        lead.exp = pokemon.runtime.pokemon.PokemonStats.experienceForLevel(
                                lead.growthRate(), 100);
                        lead.hp = lead.maxHp();
                    }
                }
                System.out.println((multiTrainerCapture ? "trainer2" : "faint") + " capture: "
                        + (multiTrainerCapture ? opponent.type + " " + opponent.name + " party=" + opponent.party.size
                                               : "wild level 50"));
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                if (multiTrainerCapture) {
                    context.battlePort().trainerBattle(opponent);
                } else {
                    // PROBE DEVICE, not game logic: the wired wild AI draws its
                    // move at random (AI_Move:153-155), so a level-50 foe can
                    // spend the whole frame budget on a status move (Growl) and
                    // the level-5 lead never faints - the "要更换宝可梦吗？" path
                    // this capture exists for would then only be reached by
                    // chance (measured: it was missed in one of three runs).
                    // Pin that scenario: leave the lead at 1 HP and narrow the
                    // captured foe to its damaging moves, so any foe turn faints
                    // the lead and the branch is deterministic again. Nothing
                    // outside this capture's own state is touched.
                    pinFaintCaptureLeadToOneHp();
                    context.battlePort().wildBattle("BULBASAUR", 50);
                    narrowCapturedFoeToDamagingMoves();
                }
                renderMap();
                advanceMap(1f / 60f, 256);          // through the entry animation
                advanceUntilBattle(s -> "BATTLE".equals(s.debugStage()), 1200);
                shotMap("l1-battle-t2-start");
                if (foeSwitchCapture) {
                    ((pokemon.runtime.battle.InteractiveBattlePort) context.battlePort()).session().battle.registerSwitch(1, 1);
                }
                stepMap(GameAction.CONFIRM);        // 战斗 -> the fight menu
                stepMap(GameAction.CONFIRM);        // use the first move
                // The round plays out: the exp / faint line, then pbEORSwitch.
                advanceUntilBattle(s -> s.debugChoice() != null || s.debugPage() == 3
                        || (s.debugWindow() == 2 && s.debugMessage() == null
                            && "none".equals(s.debugSwitch()) && "none".equals(s.debugEor())),
                        3000);
                shotMap("l1-battle-t2-question");
                BattleScreen live = battleScreen();
                // A foe move that misses leaves the 1 HP lead standing: fight on until it faints.
                for (int again = 0; (faintCapture || trainerFaintCapture) && again < 8 && live != null && live.debugChoice() == null
                        && live.debugPage() != 3; again++) {
                    stepMap(GameAction.CONFIRM);
                    stepMap(GameAction.CONFIRM);
                    advanceUntilBattle(s -> s.debugChoice() != null || s.debugPage() == 3
                            || (s.debugWindow() == 2 && s.debugMessage() == null
                                && "none".equals(s.debugSwitch()) && "none".equals(s.debugEor())), 3000);
                    live = battleScreen();
                }
                if (live != null && live.debugChoice() != null) {
                    // The first entry ("是") is selected (PokeBattle_Scene:211).
                    stepMap(GameAction.CONFIRM);
                    advanceUntilBattle(s -> s.debugPage() == 3, 300);
                    shotMap("l1-battle-t2-party");
                    stepMap(GameAction.RIGHT);
                    stepMap(GameAction.CONFIRM);    // -> pbRecallAndReplace
                    advanceUntilBattle(BattleScreen::debugSendingOut, 900);
                    shotMap("l1-battle-t2-player-sendout");
                    advanceUntilBattle(s -> !s.debugSendingOut(), 900);
                    // ... then the opponent's own send-out.
                    if (multiTrainerCapture) {
                        advanceUntilBattle(BattleScreen::debugSendingOut, 900);
                        shotMap("l1-battle-t2-foe-sendout");
                    }
                } else if (live != null && live.debugPage() == 3) {
                    // No question: the trainer-battle form sends the replacement
                    // out directly (Battle_Action_Switching:205-208).
                    shotMap("l1-battle-t2-party");
                    stepMap(GameAction.RIGHT);
                    stepMap(GameAction.CONFIRM);
                    advanceUntilBattle(BattleScreen::debugSendingOut, 900);
                    shotMap("l1-battle-t2-foe-sendout");
                }
                if (trainerFaintCapture) {
                    // A player presses through the lines: where does the battle end up?
                    for (int press = 0; press < 14; press++) {
                        stepMap(GameAction.CONFIRM);
                        advanceMap(1f / 60f, 90);
                        BattleScreen b = battleScreen();
                        System.out.println("press " + press + ": " + (b == null ? "no screen" : "page=" + b.debugPage() + " window=" + b.debugWindow() + " switch=" + b.debugSwitch() + " eor=" + b.debugEor() + " msg=" + b.debugMessage()));
                    }
                    shotMap("l1-battle-t2-after-presses");
                }
                advanceUntilBattle(s -> "BATTLE".equals(s.debugStage())
                        && !s.debugSendingOut() && s.debugWindow() == 2, 1500);
                shotMap("l1-battle-t2-done");
            } else if (transitionTrainerCapture) {
                System.setProperty("pokemon.daynight.hour", "10");   // pin daytime
                // A foe whose trainer type has no vsTrainer graphic falls back to
                // the named transition (PField_Visuals:145), i.e. TwoBallPass here.
                pokemon.runtime.pokemon.PbsData.TrainerData opponent = null;
                if (pbs != null) {
                    for (com.badlogic.gdx.utils.ObjectMap.Entry<String, pokemon.runtime.pokemon.PbsData.TrainerData> entry : pbs.trainers) {
                        pokemon.runtime.pokemon.PbsData.TrainerData candidate = entry.value;
                        if (candidate.party.size != 1 || candidate.type == null) continue;
                        pokemon.runtime.pokemon.PbsData.TrainerType type = pbs.trainerTypes.get(candidate.type);
                        if (type == null) continue;
                        if (locator.find("Transitions", "vsTrainer" + type.id + ".png") != null) continue;
                        if (locator.find("Trainers", "trainer" + type.internalName + ".png") == null
                                && locator.find("Trainers",
                                        String.format(java.util.Locale.ROOT, "trainer%03d.png", type.id)) == null) continue;
                        opponent = candidate;
                        break;
                    }
                }
                if (opponent == null) throw new IllegalStateException("no non-VS trainer for the capture");
                System.out.println("transition trainer capture: " + opponent.type + " " + opponent.name);
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().trainerBattle(opponent);
                renderMap();
                for (int f = 0; f < 84; f++) {
                    advanceMap(1f / 40f, 1);
                    if (f == 4) shotMap("l1-trainer-flash");
                    if (f == 33) shotMap("l1-trainer-transition0");
                    if (f == 45) shotMap("l1-trainer-transition1");
                    if (f == 60) shotMap("l1-trainer-transition2");
                    if (f == 80) shotMap("l1-trainer-transition3");
                }
            } else if (transitionCapture) {
                // Transitions:640 SnakeSquares: an outdoor wild battle on an
                // outside map picks it for both day and night (PField_Visuals:62).
                System.setProperty("pokemon.daynight.hour", "10");   // pin daytime
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().wildBattle("BULBASAUR", 5);
                renderMap();                       // entry frame 0
                for (int f = 0; f < 84; f++) {
                    advanceMap(1f / 40f, 1);
                    if (f == 4) shotMap("l1-battle-flash");
                    if (f == 33) shotMap("l1-transition0");
                    if (f == 42) shotMap("l1-transition1");
                    if (f == 55) shotMap("l1-transition2");
                    if (f == 70) shotMap("l1-transition3");
                    if (f == 80) shotMap("l1-transition4");
                }
            } else if (switchCapture) {
                // B1 fixed points for the switch lines
                // (Battle_Action_Switching:256-305): the recall line, the lineup
                // coming back, the replace line and the throw.
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().wildBattle("BULBASAUR", 5);
                renderMap();
                advanceMap(1f / 60f, 110);         // entry + intro -> the battle screen
                advanceUntilBattle(s -> "BATTLE".equals(s.debugStage()), 900);
                shotMap("l1-battle-switch-menu");
                stepMap(GameAction.DOWN);           // 战斗 -> 背包
                stepMap(GameAction.DOWN);           // 背包 -> 宝可梦
                stepMap(GameAction.CONFIRM);        // -> pbPartyScreen(...,true)
                advanceMap(1f / 60f, 4);
                shotMap("l1-battle-switch-party");
                stepMap(GameAction.DOWN);           // the second party member
                stepMap(GameAction.CONFIRM);        // pbRecallAndReplace
                advanceUntilBattle(s -> s.debugMessageComplete() && !s.debugSendingOut(), 400);
                shotMap("l1-battle-switch-recall");
                stepMap(GameAction.CONFIRM);        // -> the lineup + the replace line
                advanceUntilBattle(BattleScreen::debugSendingOut, 400);
                shotMap("l1-battle-switch-replace");
                advanceMap(1f / 60f, 34);           // the ball opens (Follower_Main:781-785)
                shotMap("l1-battle-switch-throw");
                // The switch stays inside Stage.BATTLE, so "done" means the
                // send-out finished and the command window is back.
                advanceUntilBattle(s -> !s.debugSendingOut() && s.debugWindow() == 2, 600);
                shotMap("l1-battle-switch-done");
            } else if (trainerCapture) {
                // A single-Pokemon trainer with a sprite, so the intro's slide-in
                // and the send-outs can be checked frame by frame.
                pokemon.runtime.pokemon.PbsData.TrainerData opponent = null;
                if (pbs != null) {
                    for (com.badlogic.gdx.utils.ObjectMap.Entry<String, pokemon.runtime.pokemon.PbsData.TrainerData> entry : pbs.trainers) {
                        pokemon.runtime.pokemon.PbsData.TrainerData candidate = entry.value;
                        if (candidate.party.size != 1 || candidate.type == null) continue;
                        pokemon.runtime.pokemon.PbsData.TrainerType type = pbs.trainerTypes.get(candidate.type);
                        if (type == null) continue;
                        if (locator.find("Trainers", "trainer" + type.internalName + ".png") == null
                                && locator.find("Trainers",
                                        String.format(java.util.Locale.ROOT, "trainer%03d.png", type.id)) == null) continue;
                        opponent = candidate;
                        break;
                    }
                }
                if (opponent == null) throw new IllegalStateException("no single-Pokemon trainer for the capture");
                System.out.println("trainer capture: " + opponent.type + " " + opponent.name
                        + " party=" + opponent.party.size);
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().trainerBattle(opponent);
                renderMap();                       // the entry animation starts
                advanceMap(1f / 60f, 16);          // VS: the bars slide in
                shotMap("l1-battle-entry");
                advanceMap(1f / 60f, 58);          // VS: the logo + the names
                shotMap("l1-battle-entry-vs");
                advanceMap(1f / 60f, 182);         // entry done -> the battle screen
                advanceMap(1f / 60f, 18);          // INTRO frame 12: black screen + slide
                shotMap("l1-battle-trainer-intro");
                advanceMap(1f / 60f, 26);          // INTRO frame ~29: lineups appearing
                shotMap("l1-battle-trainer-lineup");
                // B1 fixed points: wait for the opening line to finish revealing.
                advanceUntilBattle(s -> s.debugMessageComplete(), 1200);
                shotMap("l1-battle-trainer-message");
                // One message window ("{fullname}\n向你发起挑战！"): finish the
                // reveal, then close it.
                stepMap(GameAction.CONFIRM);
                stepMap(GameAction.CONFIRM);       // -> pbStartBattleSendOut's SEND_OUT
                // B1: the opposing side goes out first with "{trainer}派出了\n{X}！".
                advanceUntilBattle(BattleScreen::debugSendingOut, 60);
                shotMap("l1-battle-trainer-foe-msg");
                advanceMap(1f / 60f, 34);          // ball squish -> open (:219/:223)
                shotMap("l1-battle-trainer-foe");
                advanceMap(1f / 60f, 24);          // battler zoom-in + colour fade
                shotMap("l1-battle-trainer-foe-out");
                // Then the player's side ("去吧！\n{X}！").
                advanceUntilBattle(s -> !s.debugSendingOut(), 400);
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(BattleScreen::debugSendingOut, 200);
                shotMap("l1-battle-trainer-player-msg");
                advanceMap(1f / 60f, 24);          // Follower_Main:778-779 flight start
                shotMap("l1-battle-trainer-player");
                advanceMap(1f / 60f, 12);          // battlerAppear's zoom (PokeBattle_Animation:230-231)
                shotMap("l1-battle-trainer-reveal");
                advanceUntilBattle(s -> "BATTLE".equals(s.debugStage()), 600);
                shotMap("l1-battle-trainer");
                // Win it: the lead one-shots the opponent, then pbEndOfBattle's
                // WIN branch plays (victory line, the opponent slides back in,
                // its LoseText, the prize money, the fade).
                if (context.gameState().trainer().partyCount() > 0) {
                    context.gameState().trainer().party.members().get(0).level = 50;
                }
                for (int i = 0; i < 24; i++) {
                    stepMap(GameAction.CONFIRM);
                    advanceMap(1f / 60f, 30);
                    shotMap("l1-battle-trainer-end" + i);
                }
            } else if (safariCapture) {
                context.gameState().fieldGlobals().safari.begin(2, 1, 1, 2, 30);
                if ("safari-faint".equals(args[2])) {                 // no able Pokemon: 174_PField_Battles:351-357 runs the override first
                    for (pokemon.runtime.pokemon.Pokemon member : context.gameState().trainer().party.members()) member.hp = 0;
                }
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().freeWildBattle(new pokemon.runtime.pokemon.Pokemon(pbs.species("PIDGEY"), 10, pbs));
                renderMap();
                advanceMap(1f / 60f, 106);
                advanceUntilBattle(s -> "OPENING".equals(s.debugStage()) && s.debugMessageComplete(), 1200);
                shotMap("safari-appear");
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> "BATTLE".equals(s.debugStage()) && s.debugWindow() == 2, 1500);
                shotMap("safari-menu");
                int[] script = {1, 2, 0, 0, 0, 0, 0, 0, 0, 0};
                int throwShots = 0;
                int next = 0;
                String lastKey = "";
                int shots = 0;
                for (int i = 0; i < 1500 && battleScreen() != null; i++) {
                    BattleScreen live = battleScreen();
                    String key = live.debugStage() + "/" + live.debugWindow() + "/" + live.debugMessage();
                    if ("BALL".equals(live.debugStage()) && throwShots < 30 && i % 2 == 0) {
                        shotMap(String.format(java.util.Locale.ROOT, "safari-throw-%02d", throwShots++));
                    }
                    if (!key.equals(lastKey) && shots < 70) {
                        lastKey = key;
                        shotMap(String.format(java.util.Locale.ROOT, "safari-%02d-%s", shots++,
                                live.debugStage().toLowerCase(java.util.Locale.ROOT)));
                    }
                    if ("BATTLE".equals(live.debugStage()) && live.debugWindow() == 2 && live.debugMessage() == null
                            && live.debugPage() == 0 && next < script.length) {
                        int cmd = script[next++];
                        stepMap(GameAction.LEFT);
                        stepMap(GameAction.UP);
                        if ((cmd & 1) == 1) stepMap(GameAction.RIGHT);
                        if ((cmd & 2) == 2) stepMap(GameAction.DOWN);
                        System.out.println("safari command " + cmd);
                    }
                    stepMap(GameAction.CONFIRM);
                    if ("CAUGHT_STORE".equals(live.debugStage())) stepMap(GameAction.CANCEL);   // the Pokedex page closes with B
                    advanceMap(1f / 60f, 4);
                }
                advanceMap(1f / 60f, 60);
                System.out.println("safari capture: finished, shots=" + shots + " balls=" + context.gameState().fieldGlobals().safari.ballcount);
            } else if (doubleSwitchCapture || doubleBagCapture || doubleFaintCapture) {
                String tag = doubleSwitchCapture ? "dsw" : (doubleBagCapture ? "dbg" : "dfa");
                int foeLevel = doubleFaintCapture ? 50 : 5;
                if (doubleFaintCapture) {
                    context.gameState().trainer().party.members().get(1).hp = 1;   // Charmander, battler 2
                    context.gameState().trainer().party.members().get(0).level = 50;
                    context.gameState().trainer().party.members().get(0).hp =
                            context.gameState().trainer().party.members().get(0).maxHp();
                }
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().setBattleSize("double");
                context.battlePort().freeWildBattle(java.util.Arrays.asList(
                        new pokemon.runtime.pokemon.Pokemon(pbs.species("PIDGEY"), foeLevel, pbs),
                        new pokemon.runtime.pokemon.Pokemon(pbs.species("RATTATA"), foeLevel, pbs)));
                renderMap();
                advanceMap(1f / 60f, 106);
                advanceUntilBattle(s -> "OPENING".equals(s.debugStage()) && s.debugMessageComplete(), 1200);
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> !s.debugSendingOut() && "BATTLE".equals(s.debugStage())
                        && s.debugWindow() == 2 && s.debugCommandBattler() == 0, 1500);
                if (doubleFaintCapture) {
                    // nothing scripted: both battlers attack
                } else if (doubleSwitchCapture) {
                    stepMap(GameAction.DOWN);              // 队伍
                    stepMap(GameAction.CONFIRM);
                    advanceUntilBattle(s -> s.debugPage() == 3, 200);
                    shotMap(tag + "-party");
                    stepMap(GameAction.RIGHT);
                    stepMap(GameAction.RIGHT);             // the third member
                    stepMap(GameAction.CONFIRM);
                    advanceMap(1f / 60f, 8);
                    shotMap(tag + "-party-menu");
                    stepMap(GameAction.CONFIRM);           // 换上
                } else {
                    stepMap(GameAction.RIGHT);             // 背包
                    stepMap(GameAction.CONFIRM);
                    advanceMap(1f / 60f, 20);
                    shotMap(tag + "-bag");
                    stepMap(GameAction.CONFIRM);           // the first item
                    advanceMap(1f / 60f, 10);
                    shotMap(tag + "-bag-item");
                    stepMap(GameAction.CONFIRM);
                    advanceMap(1f / 60f, 10);
                    shotMap(tag + "-bag-use");
                    stepMap(GameAction.CONFIRM);
                    advanceMap(1f / 60f, 10);
                    shotMap(tag + "-bag-target");
                    stepMap(GameAction.CONFIRM);
                }
                String lastStage = "";
                int shots = 0;
                for (int i = 0; i < 160 && battleScreen() != null; i++) {
                    BattleScreen live = battleScreen();
                    String key = live.debugStage() + "/" + live.debugWindow() + "/" + live.debugPage() + "/"
                            + live.debugCommandBattler() + "/" + live.debugSendingOut() + "/" + live.debugSwitch();
                    if (!key.equals(lastStage) && shots < 60) {
                        lastStage = key;
                        shotMap(String.format(java.util.Locale.ROOT, "%s-%02d-%s", tag, shots++,
                                live.debugStage().toLowerCase(java.util.Locale.ROOT)));
                    }
                    if (doubleFaintCapture && live.debugPage() == 3 && live.debugMessage() == null) {
                        stepMap(GameAction.RIGHT);
                        stepMap(GameAction.RIGHT);
                    }
                    stepMap(GameAction.CONFIRM);
                    advanceMap(1f / 60f, 8);
                }
                System.out.println(tag + " capture: finished, shots=" + shots + " " + (battleScreen() == null ? "" : battleScreen().debugBattlers()));
            } else if (tripleWildCapture || tripleBossCapture) {
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                if (tripleBossCapture) {
                    pokemon.runtime.pokemon.Pokemon boss = new pokemon.runtime.pokemon.Pokemon(pbs.species("PIDGEY"), 40, pbs);
                    boss.battleRank = 3;
                    context.battlePort().setBattleSize("3v1");
                    context.battlePort().freeWildBattle(boss);
                } else {
                context.battlePort().setBattleSize("triple");
                context.battlePort().freeWildBattle(java.util.Arrays.asList(
                        new pokemon.runtime.pokemon.Pokemon(pbs.species("PIDGEY"), 5, pbs),
                        new pokemon.runtime.pokemon.Pokemon(pbs.species("RATTATA"), 5, pbs),
                        new pokemon.runtime.pokemon.Pokemon(pbs.species("CATERPIE"), 5, pbs)));
                }
                renderMap();
                advanceMap(1f / 60f, 106);
                advanceUntilBattle(s -> "OPENING".equals(s.debugStage()) && s.debugMessageComplete(), 1200);
                shotMap("tri-wild-message");
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> !s.debugSendingOut() && "BATTLE".equals(s.debugStage())
                        && s.debugWindow() == 2 && s.debugCommandBattler() == 0, 1800);
                shotMap("tri-command-1");
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> s.debugPage() == 1, 200);
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> s.debugPage() == 5, 200);
                shotMap("tri-target-1");
                stepMap(GameAction.LEFT);
                advanceMap(1f / 60f, 6);
                shotMap("tri-target-1-left");
                String lastStage = "";
                int shots = 0;
                for (int i = 0; i < 300 && battleScreen() != null; i++) {
                    BattleScreen live = battleScreen();
                    String key = live.debugStage() + "/" + live.debugWindow() + "/" + live.debugPage() + "/"
                            + live.debugCommandBattler() + "/" + live.debugSendingOut();
                    if (!key.equals(lastStage) && shots < 60) {
                        lastStage = key;
                        shotMap(String.format(java.util.Locale.ROOT, "tri-%02d-%s", shots++,
                                live.debugStage().toLowerCase(java.util.Locale.ROOT)));
                    }
                    stepMap(GameAction.CONFIRM);
                    advanceMap(1f / 60f, 8);
                }
                System.out.println("triple capture: finished, shots=" + shots + " "
                        + (battleScreen() == null ? "" : battleScreen().debugBattlers()));
            } else if (doubleTrainerCapture || tripleTrainerCapture) {
                int foesWanted = tripleTrainerCapture ? 3 : 2;
                java.util.List<pokemon.runtime.pokemon.PbsData.TrainerData> foes = new java.util.ArrayList<>();
                String partnerType = null;
                for (com.badlogic.gdx.utils.ObjectMap.Entry<String, pokemon.runtime.pokemon.PbsData.TrainerData> entry : pbs.trainers) {
                    pokemon.runtime.pokemon.PbsData.TrainerData candidate = entry.value;
                    if (candidate.party.size != 1 || candidate.type == null) continue;
                    pokemon.runtime.pokemon.PbsData.TrainerType type = pbs.trainerTypes.get(candidate.type);
                    if (type == null) continue;
                    if (locator.find("Trainers", "trainer" + type.internalName + ".png") == null
                            && locator.find("Trainers",
                                    String.format(java.util.Locale.ROOT, "trainer%03d.png", type.id)) == null) continue;
                    if (foes.size() < foesWanted) {
                        candidate.party.first().level = 5;
                        foes.add(candidate);
                    } else if (partnerType == null) {
                        partnerType = candidate.type;
                    }
                    if (foes.size() == foesWanted && (partnerType != null || tripleTrainerCapture)) break;
                }
                if (foes.size() < foesWanted || (partnerType == null && !tripleTrainerCapture)) throw new IllegalStateException("no trainers for the capture");
                for (pokemon.runtime.pokemon.Pokemon mon : context.gameState().trainer().party.members()) {
                    mon.level = 50;
                    mon.exp = pokemon.runtime.pokemon.PokemonStats.experienceForLevel(mon.growthRate(), 50);
                    mon.hp = mon.maxHp();
                }
                System.out.println("double-trainer capture: " + foes.get(0).type + " " + foes.get(0).name + " + "
                        + foes.get(1).type + " " + foes.get(1).name + ", partner type " + partnerType);
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                pokemon.runtime.pokemon.Pokemon weakPartner = new pokemon.runtime.pokemon.Pokemon(pbs.species("PIKACHU"), 5, pbs);
                weakPartner.hp = 1;                // faints in the first round: the partner has a reserve to send out
                if (!tripleTrainerCapture) {
                    context.battlePort().setPartner(partnerType, "小伙伴", java.util.Arrays.asList(weakPartner,
                            new pokemon.runtime.pokemon.Pokemon(pbs.species("CATERPIE"), 5, pbs)));
                }
                context.battlePort().setBattleSize(tripleTrainerCapture ? "triple" : "double");
                context.battlePort().trainerBattle(foes);
                renderMap();
                advanceMap(1f / 60f, 16);          // entry
                advanceUntilBattle(s -> true, 1500);
                String lastStage = "";
                int shots = 0;
                for (int i = 0; i < 400 && battleScreen() != null; i++) {
                    BattleScreen live = battleScreen();
                    String key = live.debugStage() + "/" + live.debugWindow() + "/" + live.debugPage() + "/"
                            + live.debugCommandBattler() + "/" + live.debugSendingOut();
                    if (!key.equals(lastStage) && shots < 80) {
                        lastStage = key;
                        shotMap(String.format(java.util.Locale.ROOT, (tripleTrainerCapture ? "tt" : "dt") + "-%02d-%s", shots++,
                                live.debugStage().toLowerCase(java.util.Locale.ROOT)));
                    }
                    stepMap(GameAction.CONFIRM);
                    advanceMap(1f / 60f, 8);
                }
                System.out.println("double-trainer capture: finished, shots=" + shots);
            } else if (doubleCapture) {
                if (pbs == null || pbs.species("PIDGEY") == null || pbs.species("RATTATA") == null) {
                    throw new IllegalStateException("the double capture needs PIDGEY and RATTATA");
                }
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().setBattleSize("double");
                context.battlePort().freeWildBattle(java.util.Arrays.asList(
                        new pokemon.runtime.pokemon.Pokemon(pbs.species("PIDGEY"), 5, pbs),
                        new pokemon.runtime.pokemon.Pokemon(pbs.species("RATTATA"), 5, pbs)));
                renderMap();
                advanceMap(1f / 60f, 106);         // entry + intro
                advanceUntilBattle(s -> "OPENING".equals(s.debugStage()) && s.debugMessageComplete(), 1200);
                shotMap("dbl-wild-message");
                stepMap(GameAction.CONFIRM);       // into the send-out
                advanceUntilBattle(s -> !s.debugSendingOut() && "BATTLE".equals(s.debugStage())
                        && s.debugWindow() == 2 && s.debugCommandBattler() == 0, 1500);
                shotMap("dbl-command-1");
                System.out.println("double capture: battlers=" + battleScreen().debugBattlers());
                stepMap(GameAction.CONFIRM);       // 战斗
                advanceUntilBattle(s -> s.debugPage() == 1, 200);
                shotMap("dbl-fight-1");
                stepMap(GameAction.CONFIRM);       // slot 0 -> the target menu
                advanceUntilBattle(s -> s.debugPage() == 5, 200);
                shotMap("dbl-target-1");
                System.out.println("double capture: target=" + battleScreen().debugTarget());
                stepMap(GameAction.RIGHT);
                advanceMap(1f / 60f, 6);
                shotMap("dbl-target-1-right");
                System.out.println("double capture: target after RIGHT=" + battleScreen().debugTarget());
                stepMap(GameAction.CONFIRM);       // the first battler is done
                advanceUntilBattle(s -> s.debugCommandBattler() == 2 && s.debugWindow() == 2, 200);
                shotMap("dbl-command-2");
                stepMap(GameAction.CANCEL);        // "Cancel": back to the first battler
                advanceUntilBattle(s -> s.debugCommandBattler() == 0 && s.debugWindow() == 2, 200);
                shotMap("dbl-back-to-1");
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> s.debugPage() == 1, 200);
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> s.debugPage() == 5, 200);
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> s.debugCommandBattler() == 2 && s.debugWindow() == 2, 200);
                stepMap(GameAction.CONFIRM);       // 战斗
                advanceUntilBattle(s -> s.debugPage() == 1, 200);
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> s.debugPage() == 5, 200);
                shotMap("dbl-target-2");
                stepMap(GameAction.CONFIRM);       // both chose: the round runs
                for (int i = 0; i < 40; i++) {
                    advanceMap(1f / 60f, 20);
                    shotMap(String.format(java.util.Locale.ROOT, "dbl-round-%02d", i));
                    BattleScreen live = battleScreen();
                    if (live != null && "BATTLE".equals(live.debugStage()) && live.debugWindow() == 2
                            && live.debugCommandBattler() == 0) break;
                    stepMap(GameAction.CONFIRM);
                }
                System.out.println("double capture: after the round " + battleScreen().debugBattlers()
                        + " battler=" + battleScreen().debugCommandBattler());
            } else if (fightStatusCapture || fightNoPpCapture) {
                // B6 evidence (task-2 §8.1 / §8.2), a wild battle with a prepared
                // lead:
                //   fight-status[|<status>] -> the lead carries a status, so
                //       PokemonDataBox's icon (PokeBattle_SceneElements:269-275)
                //       has to appear at (11,41) of the player's data box.
                //   fight-0pp -> the lead's move SLOT 1 has 0 PP, so the fight
                //       menu still has to show all four cells in place
                //       (PokeBattle_SceneMenus:367-383) with a red PP reading.
                if (context.gameState().trainer().partyCount() == 0) {
                    throw new IllegalStateException("the fight capture needs a party member");
                }
                if (pbs == null) {
                    throw new IllegalStateException("the fight capture needs the PBS data");
                }
                pokemon.runtime.pokemon.Pokemon lead =
                        context.gameState().trainer().party.members().get(0);
                // Four real moves out of the project's own move table, so the 2x2
                // grid has four filled slots to look at.
                lead.moves.clear();
                for (com.badlogic.gdx.utils.ObjectMap.Entry<String, pokemon.runtime.pokemon.PbsData.Move> entry
                        : pbs.moves) {
                    if (lead.moves.size >= 4) break;
                    lead.moves.add(new pokemon.runtime.pokemon.Pokemon.MoveSlot(entry.value));
                }
                if (lead.moves.size < 4) {
                    throw new IllegalStateException("the move table gave only "
                            + lead.moves.size + " moves");
                }
                if (fightNoPpCapture) {
                    lead.moves.get(1).pp = 0;      // the slot under test
                } else {
                    // Battler's constructor copies pkmn.status (Battler.java:62).
                    lead.status = args.length > 3 ? args[3] : "POISON";
                }
                System.out.println((fightNoPpCapture ? "fight-0pp" : "fight-status")
                        + " capture: lead=" + (lead.name == null ? lead.species.name : lead.name)
                        + " status=" + lead.status
                        + " pp=[" + lead.moves.get(0).pp + "," + lead.moves.get(1).pp
                        + "," + lead.moves.get(2).pp + "," + lead.moves.get(3).pp + "]");
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().wildBattle("BULBASAUR", 5);
                renderMap();
                advanceMap(1f / 60f, 106);         // entry (16) + intro (90)
                advanceUntilBattle(s -> "OPENING".equals(s.debugStage())
                        && s.debugMessageComplete(), 1200);
                stepMap(GameAction.CONFIRM);       // into the send-out
                // Wait for the send-out to finish and the command window to come
                // back, so the menu below is guaranteed to take input.
                advanceUntilBattle(s -> !s.debugSendingOut() && s.debugWindow() == 2, 900);
                shotMap(fightStatusCapture ? "b6-status-battle" : "b6-0pp-battle");
                stepMap(GameAction.CONFIRM);       // 战斗 -> the fight menu
                advanceUntilBattle(s -> s.debugPage() == 1, 200);
                shotMap(fightStatusCapture ? "b6-status-fight" : "b6-0pp-slot0");
                // The plugin's 2x2 grid walks slot 0 -> 1 with RIGHT and RIGHT is
                // then blocked (Scene_Commands:121-124 only moves when the
                // destination slot holds a move), so reach slot 2 with LEFT then
                // DOWN and slot 3 with RIGHT. Reaching slot 3 at all means
                // moves[3].id>0, i.e. the fourth cell is occupied.
                stepMap(GameAction.RIGHT);
                advanceUntilBattle(s -> s.debugCursor() == 1, 60);
                shotMap(fightStatusCapture ? "b6-status-fight-slot1" : "b6-0pp-slot1");
                if (fightNoPpCapture) {
                    stepMap(GameAction.LEFT);
                    advanceUntilBattle(s -> s.debugCursor() == 0, 60);
                    stepMap(GameAction.DOWN);
                    advanceUntilBattle(s -> s.debugCursor() == 2, 60);
                    shotMap("b6-0pp-slot2");
                    stepMap(GameAction.RIGHT);
                    advanceUntilBattle(s -> s.debugCursor() == 3, 60);
                    shotMap("b6-0pp-slot3");
                }
            } else if (moveAnimCapture) {
                if (context.gameState().trainer().partyCount() == 0) {
                    throw new IllegalStateException("the move-anim capture needs a party member");
                }
                pokemon.runtime.pokemon.Pokemon lead =
                        context.gameState().trainer().party.members().get(0);
                lead.level = 100;
                lead.hp = lead.maxHp();
                // PROBE DEVICE: -Dpokemon.capture.move=<INTERNALNAME> puts that move in slot 0.
                String probeMove = System.getProperty("pokemon.capture.move");
                if (probeMove != null && pbs != null && pbs.move(probeMove) != null && lead.moves.size > 0) {
                    lead.moves.get(0).move = pbs.move(probeMove);
                    lead.moves.get(0).pp = 20;
                }
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().wildBattle("BULBASAUR", 50);
                renderMap();
                advanceMap(1f / 60f, 106);
                advanceUntilBattle(s -> "OPENING".equals(s.debugStage()) && s.debugMessageComplete(), 1200);
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> !s.debugSendingOut() && s.debugWindow() == 2, 900);
                stepMap(GameAction.CONFIRM);       // 战斗 -> the fight menu
                advanceUntilBattle(s -> s.debugPage() == 1, 200);
                stepMap(GameAction.CONFIRM);       // use slot 0
                for (int i = 0; i < 400; i++) {
                    BattleScreen animScreen = battleScreen();
                    if (animScreen != null && "MOVE_ANIM".equals(animScreen.debugStage())) break;
                    stepMap(GameAction.CONFIRM);
                }
                BattleScreen animScreen = battleScreen();
                System.out.println("move-anim capture: stage=" + (animScreen == null ? "none" : animScreen.debugStage()));
                int shots = 0;
                int animFrames = 0;
                for (int f = 0; f < 900 && shots < 30; f++) {
                    animScreen = battleScreen();
                    if (animScreen == null) break;
                    if (!"MOVE_ANIM".equals(animScreen.debugStage())) {
                        stepMap(GameAction.CONFIRM);   // close the round's lines, next animation follows
                        animFrames = 0;
                        continue;
                    }
                    if (animFrames++ % 9 == 0) {
                        shotMap(String.format(java.util.Locale.ROOT, "anim-%02d", shots++));
                    } else {
                        advanceMap(1f / 60f, 1);
                    }
                }
                System.out.println("move-anim capture: shots=" + shots);
            } else if (damageCapture) {
                // B7: play one move and sample the hit stage frame by frame. Both
                // sides are levelled so neither faints: the flash and the HP bars
                // are then the only things moving.
                if (context.gameState().trainer().partyCount() == 0) {
                    throw new IllegalStateException("the damage capture needs a party member");
                }
                pokemon.runtime.pokemon.Pokemon lead =
                        context.gameState().trainer().party.members().get(0);
                lead.level = 100;
                lead.hp = lead.maxHp();
                System.out.println("damage capture: lead=" + lead.level + " hp=" + lead.hp);
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().wildBattle("BULBASAUR", 50);
                renderMap();
                advanceMap(1f / 60f, 106);         // entry (16) + intro (90)
                advanceUntilBattle(s -> "OPENING".equals(s.debugStage())
                        && s.debugMessageComplete(), 1200);
                stepMap(GameAction.CONFIRM);       // into the send-out
                advanceUntilBattle(s -> !s.debugSendingOut() && s.debugWindow() == 2, 900);
                shotMap("b7-damage-before");
                stepMap(GameAction.CONFIRM);       // 战斗 -> the fight menu
                advanceUntilBattle(s -> s.debugPage() == 1, 200);
                shotMap("b7-damage-fightmenu");
                stepMap(GameAction.CONFIRM);       // use slot 0
                // Close the round's messages one rendered frame at a time, so the
                // sampling below starts within a frame of the HIT stage beginning
                // (the flash's first off-period is only 6 render frames long).
                for (int i = 0; i < 400; i++) {
                    BattleScreen hitScreen = battleScreen();
                    if (hitScreen != null && "HIT".equals(hitScreen.debugStage())) break;
                    stepMap(GameAction.CONFIRM);
                }
                BattleScreen hitScreen = battleScreen();
                System.out.println("damage capture: stage before sampling="
                        + (hitScreen == null ? "none" : hitScreen.debugStage()));
                // One shot per rendered frame - shotMap renders exactly one frame
                // itself, so every sample is 1/60 s apart (the flash is 4 x 4 ticks
                // at 40 fps, :630-636, i.e. the battler is hidden for 2 of every 4
                // ticks = 3 of every 6 sampled frames).
                for (int f = 0; f < 80; f++) {
                    shotMap(String.format(java.util.Locale.ROOT, "b7-damage-%02d", f));
                }
                hitScreen = battleScreen();
                System.out.println("damage capture: stage after sampling="
                        + (hitScreen == null ? "none" : hitScreen.debugStage()));
            } else {
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().wildBattle("BULBASAUR", 5);
                renderMap();                       // the entry animation starts
                advanceMap(1f / 60f, 16);          // PField_Visuals:85-94 white flash
                shotMap("l1-battle-entry");
                advanceMap(1f / 60f, 90);          // entry done -> the battle intro
                shotMap("l1-battle-intro");
                // B1 fixed points: the intro's own animations (BattleIntroAnimation,
                // then BattleIntroAnimation2 + DataBoxAppearAnimation) run to
                // completion before the opening line appears (Scene_Animations:5-53).
                advanceUntilBattle(s -> "OPENING".equals(s.debugStage())
                        && s.debugMessageComplete(), 1200);
                shotMap("l1-battle-wild");
                // Closing the line runs pbStartBattleSendOut's brief "去吧！\n{X}！"
                // and then the send-out (Battle_StartAndEnd:253-273).
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(BattleScreen::debugSendingOut, 120);
                shotMap("l1-battle-sendout0");
                advanceMap(1f / 60f, 16);          // the ball in hand -> launch
                shotMap("l1-battle-ball");
                advanceMap(1f / 60f, 16);          // createBallTrajectory's flight
                shotMap("l1-battle-sendout1");
                advanceMap(1f / 60f, 22);          // ballOpenUp + battlerAppear
                shotMap("l1-battle-sendout2");
                advanceUntilBattle(s -> "BATTLE".equals(s.debugStage()), 600);
                shotMap("l1-battle");
                // The selected-side bob (PokeBattle_SceneElements:374-386): four
                // shots six frames apart cover the whole 24-frame cycle.
                shotMap("l1-battle-bob0");
                advanceMap(1f / 60f, 9);
                shotMap("l1-battle-bob1");
                advanceMap(1f / 60f, 9);
                shotMap("l1-battle-bob2");
                advanceMap(1f / 60f, 9);
                shotMap("l1-battle-bob3");
                stepMap(GameAction.CONFIRM);       // 战斗 -> the fight menu
                shotMap("l1-battle-fight");
                // The settlement (pbGainExpOne): level the lead up so one hit
                // faints the wild Pokemon, and park it just below the next level
                // so the award also plays a level-up.
                if (context.gameState().trainer().partyCount() > 0) {
                    pokemon.runtime.pokemon.Pokemon lead = context.gameState().trainer().party.members().get(0);
                    lead.level = 50;
                    lead.exp = pokemon.runtime.pokemon.PokemonStats.experienceForLevel(lead.growthRate(), 51) - 3;
                }
                stepMap(GameAction.CONFIRM);       // use the first move
                for (int i = 0; i < 16; i++) {
                    stepMap(GameAction.CONFIRM);
                    advanceMap(1f / 60f, 24);
                    shotMap("l1-battle-settle" + i);
                }
                // rocket:6-101: an active evil-team switch plays the VS
                // animation instead of the default flash.
                context.gameState().switches().set(211, true);
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().wildBattle("BULBASAUR", 5);
                renderMap();
                advanceMap(1f / 60f, 8);
                shotMap("l1-battle-evil-vs0");
                advanceMap(1f / 60f, 16);
                shotMap("l1-battle-evil-vs1");
                advanceMap(1f / 60f, 30);
                shotMap("l1-battle-evil-vs2");
                advanceMap(1f / 60f, 60);
                shotMap("l1-battle-evil-vs3");
            }

            if (PERF) {
                reportPerfTotals();
            }
            System.out.println("MENU CAPTURE OK outputDir=" + args[1]);
        } catch (Throwable error) {
            failure = error;
            error.printStackTrace();
        } finally {
            if (overlay != null) {
                overlay.dispose();
            }
            if (title != null) {
                title.dispose();
            }
            if (context != null && context.game() != null && context.game().getScreen() != null) {
                context.game().getScreen().dispose();
            }
            if (buffer != null) {
                buffer.dispose();
            }
            if (batch != null) {
                batch.dispose();
            }
            Gdx.app.exit();
        }
    }

    private void renderOverlay() {
        projection.update();
        batch.setProjectionMatrix(projection.combined);
        batch.begin();
        int drawCallsBefore = batch.renderCalls;
        overlay.render(batch);
        if (PERF) {
            perfMenuDrawCalls += batch.renderCalls - drawCallsBefore;
        }
        batch.end();
    }

    /**
     * One {@code Screen#render}, with the perf accounting when
     * {@code -Dpokemon.capture.perf=1} is on. The counters are
     * {@code BattleSprites}' own ({@code drawOrder()} calls / rebuilds / reuses)
     * plus this harness's frame and world-update timings; {@code fps} is
     * libGDX's own figure for the hidden capture window.
     */
    private void renderFrame(Screen current, float delta) {
        if (current == null) {
            return;
        }
        if (!PERF) {
            current.render(delta);
            Gdx.gl.glViewport(0, 0, width, height);
            return;
        }
        long started = System.nanoTime();
        current.render(delta);
        Gdx.gl.glViewport(0, 0, width, height);
        perfFrames++;
        perfWorldNanos += System.nanoTime() - started;
        if ((perfFrames % PERF_REPORT_FRAMES) == 0) {
            reportPerf("frame " + perfFrames);
        }
    }

    /** The run's totals: cumulative counters, so the whole capture is quotable. */
    private void reportPerfTotals() {
        System.out.printf(java.util.Locale.ROOT,
                "perf TOTAL frames=%d worldMs=%.3f avgWorldMs=%.3f drawOrderCalls=%d sorts=%d"
                        + " reuses=%d drawList=%d menuDrawCalls=%d%n",
                perfFrames, perfWorldNanos / 1e6, perfWorldNanos / 1e6 / Math.max(1, perfFrames),
                BattleSprites.perfDrawOrderCalls(), BattleSprites.perfDrawOrderSorts(),
                BattleSprites.perfDrawOrderReuses(), BattleSprites.perfDrawListSize(),
                perfMenuDrawCalls);
    }

    /** One perf line; the counters are printed as deltas since the last line. */
    private void reportPerf(String label) {
        long calls = BattleSprites.perfDrawOrderCalls();
        long sorts = BattleSprites.perfDrawOrderSorts();
        long reuses = BattleSprites.perfDrawOrderReuses();
        System.out.printf(java.util.Locale.ROOT,
                "perf %s frames=%d worldMs=%.3f avgWorldMs=%.3f fps=%d"
                        + " drawOrderCalls=+%d sorts=+%d reuses=+%d drawList=%d menuDrawCalls=+%d%n",
                label, perfFrames, perfWorldNanos / 1e6,
                perfWorldNanos / 1e6 / Math.max(1, perfFrames),
                Gdx.graphics == null ? -1 : Gdx.graphics.getFramesPerSecond(),
                calls - perfLastOrderCalls, sorts - perfLastOrderSorts,
                reuses - perfLastOrderReuses, BattleSprites.perfDrawListSize(),
                perfMenuDrawCalls - perfLastMenuDrawCalls);
        perfLastOrderCalls = calls;
        perfLastOrderSorts = sorts;
        perfLastOrderReuses = reuses;
        perfLastMenuDrawCalls = perfMenuDrawCalls;
    }

    /** A tap the title screen itself processes during its next render. */
    private void tapTitle(GameAction action) {
        InputManager input = context.inputManager();
        input.beginFrame();
        input.press(action);
        title.render(1f / 60f);
        input.endFrame();
        input.release(action);
    }

    /** Roadmap stage 8.4: DiegoWTsStarterSelection.new(152,255,728), from the fade-in to the confirmed choice. */
    /** The screens of stage 8 (egg hatching, relearner, Hall of Fame) and stage 12 (credits, slot machine). */
    private void captureScenes(String mode) {
        pokemon.runtime.state.GameState state = context.gameState();
        pokemon.runtime.event.MenuService.Request request;
        switch (mode) {
            case "eon": {
                mapScreen = new MapScreen(context, 10, 31, 23, 2);
                context.game().setScreen(mapScreen);
                advanceMap(1f / 60f, 6);
                for (int id = 1; id < 600; id++) context.gameState().fieldGlobals().visitedMaps.add(id);   // every place counts as visited
                context.eventInterpreter().startFieldItem("EONFLUTE");
                java.util.List<GameAction> walk = new java.util.ArrayList<>();
                for (int k = 0; k < 4; k++) walk.add(GameAction.CONFIRM);               // the messages
                GameAction[] sweep = {GameAction.DOWN, GameAction.LEFT, GameAction.UP, GameAction.RIGHT};
                for (int round = 0; round < 40; round++) {
                    GameAction dir = sweep[(round / 6) % 4];
                    walk.add(dir);
                    walk.add(GameAction.CONFIRM);
                }
                int step = 0;
                for (int i = 0; i < 3600; i++) {
                    advanceMap(1f / 40f, 1);
                    if (i % 14 == 13 && step < walk.size()) stepMap(walk.get(step++));
                    if (i == 40 || i == 150) shotMap("eon-" + i);
                    if (context.gameState().currentMapId() != 10 && i > 40) {
                        for (int j = 0; j < 4; j++) {
                            shotMap("eon-arrive-" + j);
                            for (int k = 0; k < 20; k++) advanceMap(1f / 40f, 1);
                        }
                        break;
                    }
                }
                System.out.println("eon: map=" + context.gameState().currentMapId());
                break;
            }
            case "nexus": {
                context.gameState().switches().set(231, true);          // Eden opens the special list
                mapScreen = new MapScreen(context, 10, 31, 23, 2);
                context.game().setScreen(mapScreen);
                advanceMap(1f / 60f, 6);
                context.eventInterpreter().startFieldItem("ETHEREALNEXUS");
                for (int i = 0; i < 40; i++) advanceMap(1f / 40f, 1);
                shotMap("nexus-1-list");
                stepMap(GameAction.RIGHT);
                stepMap(GameAction.DOWN);
                for (int i = 0; i < 4; i++) advanceMap(1f / 40f, 1);
                shotMap("nexus-2-moved");
                stepMap(GameAction.CONFIRM);
                for (int i = 0; i < 200; i++) advanceMap(1f / 40f, 1);
                shotMap("nexus-3-arrived");
                System.out.println("nexus: map=" + context.gameState().currentMapId());
                break;
            }
            case "chapter": {                                          // map 37 event 40 page 0: the chapter cards (<ac>\l[2] texts)
                mapScreen = new MapScreen(context, 37, 14, 10, 2);
                context.game().setScreen(mapScreen);
                advanceMap(1f / 60f, 6);
                pokemon.runtime.data.MapData md = context.database().map(37);
                for (pokemon.runtime.data.MapData.EventData ev : md.events) {
                    if (ev.id == 40) context.eventInterpreter().start(ev.pages.get(0).commands, 37, 40);
                }
                int shots = 0;
                for (int i = 0; i < 400 && shots < 12; i++) {
                    advanceMap(1f / 40f, 1);
                    if (context.messageService().visible() && i % 6 == 0) {
                        System.out.println("message lines=" + context.messageService().lines().size + " " + context.messageService().lines());
                        shotMap(String.format(java.util.Locale.ROOT, "chapter-%02d", shots++));
                        stepMap(GameAction.CONFIRM);
                    }
                }
                break;
            }
            case "ropes": {
                mapScreen = new MapScreen(context, 10, 31, 23, 2);
                context.game().setScreen(mapScreen);
                advanceMap(1f / 60f, 6);
                context.gameState().fieldGlobals().escapePoint = new int[] {2, 36, 8, 2};
                context.gameState().inventory().add("INFINITEROPE", 1);
                context.eventInterpreter().startFieldItem("INFINITEROPE");
                for (int i = 0; i < 180; i++) {
                    advanceMap(1f / 40f, 1);                       // one frame per call: a transfer swaps the screen under the loop
                    if (i == 6) shotMap("ropes-1-message");
                    if (i == 12 || i == 24) stepMap(GameAction.CONFIRM);
                    if (i == 60) shotMap("ropes-2-fade");
                }
                shotMap("ropes-3-arrived");
                System.out.println("rope: map=" + context.gameState().currentMapId() + " escape=" + context.gameState().fieldGlobals().escapePoint.length
                        + " rope=" + context.gameState().inventory().count("INFINITEROPE"));
                break;
            }
            case "fishing": {
                mapScreen = new MapScreen(context, 10, 31, 23, 2);
                context.game().setScreen(mapScreen);
                advanceMap(1f / 60f, 6);
                shotMap("fishing-0-ready");
                context.eventInterpreter().startFieldItem("SUPERROD");
                for (int i = 0; i < 40; i++) {
                    advanceMap(1f / 40f, 2);
                    if (i % 3 == 0) shotMap(String.format(java.util.Locale.ROOT, "fishing-%02d", i));
                    if (i == 30 || i == 36) stepMap(GameAction.CONFIRM);
                }
                break;
            }
            case "habitat": {
                pokemon.runtime.pokemon.HabitatLog log = state.trainer().habitats;
                log.attachPbs(context.pbsData());
                log.setup(context.pbsData());
                state.trainer().pokedex = true;
                int[] maps = {44, 343, 483, 10, 43};
                for (int map : maps) state.fieldGlobals().visitedMaps.add(map);
                int shown = 0;
                for (pokemon.runtime.pokemon.HabitatLog.Entry e : log.habitatList(state.fieldGlobals().visitedMaps)) {
                    int n = 0;
                    for (pokemon.runtime.pokemon.HabitatLog.Kind kind : e.habitat.encounters.values()) {
                        for (String species : new java.util.ArrayList<>(kind.list)) {
                            if (shown % 3 == 0 || n % 2 == 0) state.trainer().setSeen(species);
                            if (shown % 3 == 0) state.trainer().setOwned(species);
                            n++;
                        }
                    }
                    shown++;
                }
                overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.HABITAT);
                idle(8);
                renderOverlay();
                shot("habitat-1-list");
                tapAndRender(GameAction.DOWN, overlay);
                idle(2);
                renderOverlay();
                shot("habitat-2-second");
                tapAndRender(GameAction.CONFIRM, overlay);
                for (int k = 0; k < 4; k++) {
                    idle(30);
                    renderOverlay();
                    shot("habitat-3-detail-" + k);
                }
                tapAndRender(GameAction.RIGHT, overlay);
                idle(10);
                renderOverlay();
                shot("habitat-4-page2");
                tapAndRender(GameAction.LEFT, overlay);
                tapAndRender(GameAction.LEFT, overlay);
                idle(10);
                renderOverlay();
                shot("habitat-5-area");
                break;
            }
            case "weather": {
                mapScreen = new MapScreen(context, 10, 31, 23, 2);
                context.game().setScreen(mapScreen);
                advanceMap(1f / 60f, 6);
                int[] types = {1, 2, 3, 4, 5};
                String[] names = {"rain", "storm", "snow", "blizzard", "sandstorm"};
                for (int t = 0; t < types.length; t++) {
                    context.gameState().weather().set(types[t], 4, 0);
                    for (int i = 0; i < 90; i++) advanceMap(1f / 40f, 1);
                    shotMap("weather-" + names[t]);
                }
                break;
            }
            case "ready": {
                pokemon.runtime.pokemon.PbsData pbs = context.pbsData();
                pokemon.runtime.pokemon.Pokemon lead = context.gameState().trainer().party.get(0);
                for (String move : new String[] {"CUT", "FLASH", "SURF"}) {
                    lead.moves.add(new pokemon.runtime.pokemon.Pokemon.MoveSlot(pbs.move(move)));
                }
                for (int badge = 0; badge < 8; badge++) context.gameState().trainer().badges.add(badge);
                for (String item : new String[] {"BICYCLE", "TOWNMAP", "SUPERROD", "REPEL", "COINCASE"}) {
                    context.gameState().inventory().add(item, item.equals("REPEL") ? 120 : 1);
                    context.gameState().inventory().bagMemory().register(item);
                }
                mapScreen = new MapScreen(context, 10, 31, 23, 2);
                context.game().setScreen(mapScreen);
                advanceMap(1f / 60f, 6);
                stepMap(GameAction.F5);
                advanceMap(1f / 40f, 3);
                shotMap("ready-1-items");
                stepMap(GameAction.UP);
                advanceMap(1f / 40f, 3);
                shotMap("ready-2-up");
                stepMap(GameAction.LEFT);
                advanceMap(1f / 40f, 3);
                shotMap("ready-3-moves");
                stepMap(GameAction.CANCEL);
                advanceMap(1f / 40f, 3);
                shotMap("ready-4-closed");
                break;
            }
            case "minimap": {
                context.gameState().inventory().add("TOWNMAP", 1);
                pokemon.runtime.ui.menu.GameSettings settings = context.settings();
                settings.showMiniMap = 0;
                mapScreen = new MapScreen(context, 10, 31, 23, 2);
                context.game().setScreen(mapScreen);
                advanceMap(1f / 60f, 6);
                shotMap("minimap-1-default");
                settings.miniMapPosition = 3;
                settings.miniMapSize = 3;
                settings.miniMapZoom = 2;
                settings.miniMapBorder = 2;
                settings.miniMapOpacity = 70;
                advanceMap(1f / 60f, 2);
                shotMap("minimap-2-big-bottom-right");
                settings.miniMapPosition = 1;
                settings.miniMapSize = 0;
                settings.miniMapZoom = 0;
                advanceMap(1f / 60f, 2);
                shotMap("minimap-3-small-top-right");
                settings.showMiniMap = 1;
                pokemon.runtime.event.MenuService.Request map = new pokemon.runtime.event.MenuService.Request(
                        pokemon.runtime.event.MenuService.Kind.SHOW_MAP);
                map.region = -1;
                map.wallmap = false;
                overlay.openRequest(map);
                idle(8);
                renderOverlay();
                shot("minimap-4-region-map");
                tapAndRender(GameAction.SPECIAL, overlay);
                idle(2);
                renderOverlay();
                shot("minimap-5-options");
                tapAndRender(GameAction.DOWN, overlay);
                tapAndRender(GameAction.RIGHT, overlay);
                tapAndRender(GameAction.RIGHT, overlay);
                idle(2);
                renderOverlay();
                shot("minimap-6-options-slider");
                break;
            }
            case "quests": {
                pokemon.runtime.state.QuestLog log = state.quests();
                log.activateQuest("Quest1", pokemon.runtime.state.QuestLog.DEFAULT_COLOR, true, "茶月镇", 2, 1700000000000L);
                log.advanceQuestToStage("Quest1", 2, null, true, "", 2, 1700000000000L);
                log.activateQuest("Quest3", "7DC076EF", false, "六号道路", 3, 1700100000000L);
                log.completeQuest("Quest2", "26CC4B56", false, "荼蘼镇", 3, 1700200000000L);
                log.failQuest("Quest4", "089D5EBF", false, "", 5, 1700300000000L);
                overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.QUESTS);
                idle(8);
                renderOverlay();
                shot("quests-1-list");
                tapAndRender(GameAction.DOWN, overlay);
                idle(2);
                renderOverlay();
                shot("quests-2-second");
                tapAndRender(GameAction.CONFIRM, overlay);
                idle(40);
                renderOverlay();
                shot("quests-3-detail");
                tapAndRender(GameAction.RIGHT, overlay);
                idle(4);
                renderOverlay();
                shot("quests-4-info");
                tapAndRender(GameAction.CANCEL, overlay);
                idle(40);
                tapAndRender(GameAction.RIGHT, overlay);   // 已完成
                idle(3);
                renderOverlay();
                shot("quests-5-completed");
                tapAndRender(GameAction.RIGHT, overlay);   // 失败
                idle(3);
                renderOverlay();
                shot("quests-6-failed");
                break;
            }
            case "fastcatch":
                context.gameState().inventory().add("GREATBALL", 4);
                context.gameState().inventory().add("ULTRABALL", 2);
                context.gameState().inventory().add("MASTERBALL", 1);
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().wildBattle("BULBASAUR", 5);
                renderMap();
                advanceUntilBattle(s -> "OPENING".equals(s.debugStage()) && s.debugMessageComplete(), 2400);
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> "BATTLE".equals(s.debugStage()), 1200);
                stepMap(GameAction.SPECIAL);                 // Input::A on the command menu: the Poke Ball picker
                advanceMap(1f / 60f, 6);
                shotMap("fastcatch-1");
                stepMap(GameAction.RIGHT);
                advanceMap(1f / 60f, 12);
                shotMap("fastcatch-2");
                stepMap(GameAction.RIGHT);
                stepMap(GameAction.RIGHT);                   // Master Ball
                advanceMap(1f / 60f, 12);
                stepMap(GameAction.CONFIRM);                 // pbConfirmMessageSerious
                advanceMap(1f / 60f, 30);
                shotMap("fastcatch-master-confirm");
                stepMap(GameAction.CANCEL);                  // 否
                stepMap(GameAction.LEFT);
                stepMap(GameAction.LEFT);
                stepMap(GameAction.LEFT);                    // back to the Poke Ball
                stepMap(GameAction.CONFIRM);                 // throw
                for (int i = 0; i < 6; i++) {
                    advanceMap(1f / 60f, 30);
                    shotMap("fastcatch-throw" + i);
                }
                break;
            case "illusion": {                              // Illusion: the lead looks like the last Pokemon of the team until it is hit
                pokemon.runtime.pokemon.Pokemon lead = context.gameState().trainer().party.get(0);
                lead.ability = "ILLUSION";
                pokemon.runtime.pokemon.Pokemon last = new pokemon.runtime.pokemon.Pokemon(context.pbsData().species("PIKACHU"), 12, context.pbsData());
                last.name = "皮皮";
                context.gameState().trainer().party.add(last);
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().wildBattle("CHARMANDER", 5);
                renderMap();
                for (int i = 0; i < 260; i++) {
                    advanceMap(1f / 40f, 2);
                    if (i % 24 == 23) stepMap(GameAction.CONFIRM);
                }
                shotMap("illusion-final");
                break;
            }
            case "transform":                               // Imposter on switch-in: the lead takes the foe's picture (mosaic), stats and moves
                context.gameState().trainer().party.get(0).ability = "IMPOSTER";
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().wildBattle("CHARMANDER", 5);
                renderMap();
                for (int i = 0; i < 260; i++) {
                    advanceMap(1f / 40f, 2);
                    if (i % 6 == 0 && i >= 60) shotMap(String.format(java.util.Locale.ROOT, "transform-%03d", i));
                    if (i % 24 == 23) stepMap(GameAction.CONFIRM);
                }
                shotMap("transform-final");
                stepMap(GameAction.F5);
                advanceMap(1f / 60f, 60);
                shotMap("transform-info");
                break;
            case "battleinfo":
                mapScreen = new MapScreen(context, 2);
                context.game().setScreen(mapScreen);
                context.battlePort().wildBattle("BULBASAUR", 5);
                renderMap();
                advanceUntilBattle(s -> "OPENING".equals(s.debugStage()) && s.debugMessageComplete(), 2400);
                stepMap(GameAction.CONFIRM);
                advanceUntilBattle(s -> "BATTLE".equals(s.debugStage()), 1200);
                stepMap(GameAction.F5);                      // 156_Scene_Commands:60-63 pbBattleInfo
                advanceMap(1f / 60f, 90);
                shotMap("battleinfo-1");
                stepMap(GameAction.UP);                      // the foe
                advanceMap(1f / 60f, 60);
                shotMap("battleinfo-2");
                stepMap(GameAction.F5);                      // back to the command menu
                advanceMap(1f / 60f, 6);
                stepMap(GameAction.CONFIRM);                 // Fight
                advanceMap(1f / 60f, 12);
                stepMap(GameAction.F5);                      // 156_Scene_Commands:149-151 pbMoveInfo
                advanceMap(1f / 60f, 90);
                shotMap("moveinfo-1");
                stepMap(GameAction.F5);
                advanceMap(1f / 60f, 6);
                stepMap(GameAction.RIGHT);
                stepMap(GameAction.F5);
                advanceMap(1f / 60f, 90);
                shotMap("moveinfo-2");
                break;
            case "headnames":
                context.settings().headtopname = 3;
                mapScreen = new MapScreen(context, 3, 18, 14, 8);
                context.game().setScreen(mapScreen);
                for (int i = 0; i < 6; i++) { renderMap(); }
                shotMap("headnames");
                break;
            case "mining":
                request = new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.MINIGAME);
                request.wanted = "mining";
                overlay.openRequest(request);
                idle(60);
                renderOverlay();
                shot("mining-1-intro");
                for (int i = 0; i < 6; i++) { tapAndRender(GameAction.CONFIRM, overlay); idle(8); }
                renderOverlay();
                shot("mining-2-start");
                for (int round = 0; round < 40; round++) {
                    tapAndRender(GameAction.CONFIRM, overlay);
                    idle(30);
                    if (round % 4 == 0) tapAndRender(GameAction.RIGHT, overlay);
                    if (round % 4 == 1) tapAndRender(GameAction.UP, overlay);
                    if (round % 4 == 2) tapAndRender(GameAction.SPECIAL, overlay);
                    if (round % 4 == 3) tapAndRender(GameAction.LEFT, overlay);
                    idle(4);
                    if (round == 1 || round == 5 || round == 20 || round == 39) { renderOverlay(); shot("mining-3-round" + round); }
                }
                break;
            case "voltorb":
                state.fieldGlobals().coins = 120;
                request = new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.MINIGAME);
                request.wanted = "voltorbflip";
                overlay.openRequest(request);
                idle(30);
                renderOverlay();
                shot("voltorb-1-curtain");
                idle(60);
                renderOverlay();
                shot("voltorb-2-board");
                for (int round = 0; round < 60; round++) {
                    tapAndRender(GameAction.CONFIRM, overlay);
                    idle(50);
                    if (round % 7 == 3) tapAndRender(GameAction.TOGGLE_FOLLOWER, overlay);
                    tapAndRender(round % 5 == 4 ? GameAction.DOWN : GameAction.RIGHT, overlay);
                    idle(4);
                    if (round == 1 || round == 4 || round == 10 || round == 20 || round == 40 || round == 59) {
                        renderOverlay();
                        shot("voltorb-3-round" + round);
                    }
                }
                tapAndRender(GameAction.CANCEL, overlay);
                idle(30);
                renderOverlay();
                shot("voltorb-4-quit");
                break;
            case "triad":
                state.trainer().money = 50000;
                for (String owned : new String[] {"BULBASAUR", "CHARMANDER", "PIDGEY", "RATTATA", "PIKACHU", "MEWTWO", "SNORLAX"}) {
                    state.trainer().owned.add(owned);
                }
                request = new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.MINIGAME);
                request.wanted = "triadbuy";
                overlay.openRequest(request);
                idle(30);
                renderOverlay();
                shot("triad-1-scrolling");
                idle(40);
                renderOverlay();
                shot("triad-2-buy-list");
                tapAndRender(GameAction.DOWN, overlay);
                idle(4);
                renderOverlay();
                shot("triad-3-buy-second");
                tapAndRender(GameAction.CONFIRM, overlay);          // choose the card
                idle(60);
                tapAndRender(GameAction.UP, overlay);
                tapAndRender(GameAction.UP, overlay);
                idle(10);
                renderOverlay();
                shot("triad-4-number");
                tapAndRender(GameAction.CONFIRM, overlay);          // 3 cards
                idle(60);
                renderOverlay();
                shot("triad-5-confirm");
                tapAndRender(GameAction.CONFIRM, overlay);          // yes
                idle(60);
                renderOverlay();
                shot("triad-6-bought");
                for (int i = 0; i < 3; i++) { tapAndRender(GameAction.CONFIRM, overlay); idle(40); }
                tapAndRender(GameAction.CANCEL, overlay);
                idle(80);
                renderOverlay();
                shot("triad-7-after");
                request = new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.MINIGAME);
                request.wanted = "triadsell";
                overlay.openRequest(request);
                idle(80);
                renderOverlay();
                shot("triad-8-sell-list");
                tapAndRender(GameAction.CONFIRM, overlay);
                idle(60);
                tapAndRender(GameAction.UP, overlay);
                idle(10);
                renderOverlay();
                shot("triad-9-sell-number");
                tapAndRender(GameAction.CONFIRM, overlay);
                idle(60);
                renderOverlay();
                shot("triad-10-sell-confirm");
                tapAndRender(GameAction.CONFIRM, overlay);
                idle(60);
                renderOverlay();
                shot("triad-11-sold");
                tapAndRender(GameAction.CONFIRM, overlay);
                idle(40);
                renderOverlay();
                shot("triad-12-after-sell");
                System.out.println("triad money=" + state.trainer().money + " cards=" + state.fieldGlobals().triads.length()
                        + " first=" + (state.fieldGlobals().triads.get(0) == null ? "-" : state.fieldGlobals().triads.get(0).count));
                break;
            case "slots":
                state.fieldGlobals().coins = 50;
                request = new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.SLOT_MACHINE);
                request.index = 1;
                overlay.openRequest(request);
                idle(40);
                renderOverlay();
                shot("slots-1-insert");
                for (int i = 0; i < 3; i++) {
                    tapAndRender(GameAction.DOWN, overlay);
                    idle(2);
                }
                renderOverlay();
                shot("slots-2-three-coins");
                tapAndRender(GameAction.CONFIRM, overlay);
                idle(30);
                renderOverlay();
                shot("slots-3-spinning");
                for (int i = 0; i < 3; i++) {
                    tapAndRender(GameAction.CONFIRM, overlay);
                    idle(12);
                }
                idle(60);
                renderOverlay();
                shot("slots-4-stopped");
                idle(200);
                renderOverlay();
                shot("slots-5-after");
                break;
            case "credits":
                request = new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.CREDITS);
                overlay.openRequest(request);
                idle(60);
                renderOverlay();
                shot("credits-1-start");
                idle(400);
                renderOverlay();
                shot("credits-2-scrolling");
                idle(1500);
                renderOverlay();
                shot("credits-3-later");
                break;
            case "hall":
                request = new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.HALL_OF_FAME);
                overlay.openRequest(request);
                idle(20);
                renderOverlay();
                shot("hall-1-start");
                for (int i = 0; i < 6; i++) {
                    idle(150);
                    renderOverlay();
                    shot("hall-" + (i + 2) + "-step");
                }
                break;
            case "hatcher":
                for (int i = 0; i < 3; i++) {
                    pokemon.runtime.pokemon.Pokemon egg = new pokemon.runtime.pokemon.Pokemon(state.trainer().party.get(0).species, 1, context.pbsData());
                    egg.egg = true;
                    egg.stepsToHatch = new int[] {12000, 2000, 900}[i];
                    state.trainer().hatcherEggs[i] = egg;
                }
                overlay.openHatcher();
                idle(40);
                renderOverlay();
                shot("hatcher-1-first");
                tapAndRender(GameAction.RIGHT, overlay);
                idle(10);
                renderOverlay();
                shot("hatcher-2-second");
                tapAndRender(GameAction.RIGHT, overlay);
                idle(10);
                renderOverlay();
                shot("hatcher-3-third");
                tapAndRender(GameAction.DOWN, overlay);
                tapAndRender(GameAction.CONFIRM, overlay);
                idle(20);
                renderOverlay();
                shot("hatcher-4-empty-slot");
                break;
            case "hatch":
                request = new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.HATCH);
                request.pokemon = state.trainer().party.get(0);
                overlay.openRequest(request);
                idle(30);
                renderOverlay();
                shot("hatch-1-huh");
                idle(60);
                tapAndRender(GameAction.CONFIRM, overlay);     // the "Huh?" page
                tapAndRender(GameAction.CONFIRM, overlay);
                for (int i = 0; i < 12; i++) {
                    idle(i < 4 ? 50 : 70);
                    renderOverlay();
                    shot("hatch-" + (i + 2) + "-step");
                }
                break;
            default:
                pokemon.runtime.pokemon.Pokemon lead = state.trainer().party.get(0);
                lead.level = 40;
                request = new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.RELEARN);
                request.pokemon = lead;
                overlay.openRequest(request);
                idle(60);
                renderOverlay();
                shot("relearn-1-list");
                tapAndRender(GameAction.DOWN, overlay);
                renderOverlay();
                shot("relearn-2-down");
                tapAndRender(GameAction.CONFIRM, overlay);
                idle(30);
                renderOverlay();
                shot("relearn-3-confirm");
                break;
        }
    }

    private void captureStarter() {
        pokemon.runtime.event.MenuService.Request request =
                new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.STARTER);
        request.dex = new int[] {152, 255, 728};
        overlay.openRequest(request);
        idle(12);
        renderOverlay();
        shot("starter-1-fade");
        idle(90);
        renderOverlay();
        shot("starter-2-ask");
        tapAndRender(GameAction.RIGHT, overlay);          // first input selects the middle ball
        idle(6);
        renderOverlay();
        shot("starter-3-choose");
        tapAndRender(GameAction.LEFT, overlay);
        idle(8);
        renderOverlay();
        shot("starter-4-left");
        tapAndRender(GameAction.CONFIRM, overlay);        // pbChooseBall
        idle(30);
        renderOverlay();
        shot("starter-5-ball-moving");
        idle(70);
        renderOverlay();
        shot("starter-6-confirm");
        tapAndRender(GameAction.DOWN, overlay);
        idle(2);
        renderOverlay();
        shot("starter-7-cancel-selected");
        tapAndRender(GameAction.CANCEL, overlay);         // back to the choosing screen
        idle(90);
        renderOverlay();
        shot("starter-8-after-cancel");
        tapAndRender(GameAction.CONFIRM, overlay);
        idle(100);
        tapAndRender(GameAction.CONFIRM, overlay);        // yes
        idle(10);
        renderOverlay();
        shot("starter-9-closing");
        idle(40);
        System.out.println("starter capture: result=" + request.result + " done=" + request.done);
    }

    /** Roadmap stage 5: bag -> 使用 -> party screen -> the item handlers (HP, level, PP, number window). */
    private void captureItems() {
        pokemon.runtime.state.GameState state = context.gameState();
        pokemon.runtime.state.Inventory bag = state.inventory();
        bag.clear();
        bag.add("RARECANDY", 4);
        pokemon.runtime.pokemon.Pokemon lead = state.trainer().party.get(0);
        lead.hp = Math.max(1, lead.maxHp() / 3);
        overlay.openAt(pokemon.runtime.ui.menu.PauseMenuModel.Action.BAG);
        tapAndRender(GameAction.RIGHT, overlay);          // pocket 2: 回复道具
        idle(4);
        renderOverlay();
        shot("items-1-bag");
        tapAndRender(GameAction.CONFIRM, overlay);        // the first item of the pocket
        idle(2);
        renderOverlay();
        shot("items-2-commands");
        tapAndRender(GameAction.CONFIRM, overlay);        // 使用
        idle(6);
        renderOverlay();
        shot("items-3-party");
        tapAndRender(GameAction.CONFIRM, overlay);        // the lead
        idle(60);
        renderOverlay();
        shot("items-4-after-pick");
        tapAndRender(GameAction.UP, overlay);
        tapAndRender(GameAction.UP, overlay);
        idle(2);
        renderOverlay();
        shot("items-5-number");
        tapAndRender(GameAction.CONFIRM, overlay);
        idle(80);
        renderOverlay();
        shot("items-6-level-up");
        tapAndRender(GameAction.CONFIRM, overlay);
        idle(4);
        renderOverlay();
        shot("items-7-stats");
        tapAndRender(GameAction.CONFIRM, overlay);
        idle(4);
        renderOverlay();
        shot("items-8-stats2");
        tapAndRender(GameAction.CONFIRM, overlay);
        idle(80);
        renderOverlay();
        shot("items-9-next");
        System.out.println("items capture: lead level=" + lead.level + " hp=" + lead.hp + "/" + lead.maxHp()
                + " rarecandy=" + bag.count("RARECANDY") + " potion=" + bag.count("POTION"));
    }

    /** {@code frames} updates of the overlay with nothing pressed. */
    private void idle(int frames) {
        InputManager input = context.inputManager();
        for (int i = 0; i < frames; i++) {
            input.beginFrame();
            overlay.update(1f / 60f);
            input.endFrame();
        }
    }

    /**
     * Roadmap stage 6.1: pbPokemonMart (230_PScreen_Mart) from the greeting to a purchase and a sale.
     * Needs {@code -Dpokemon.menu.clockDelta=0.025} (one 40 fps tick per update) because the capture
     * runs inside {@code create()}, where libGDX's own delta never advances.
     */
    private void captureMart() {
        pokemon.runtime.event.MenuService.Request request =
                new pokemon.runtime.event.MenuService.Request(pokemon.runtime.event.MenuService.Kind.MART);
        request.items = new java.util.ArrayList<>(java.util.Arrays.asList(
                "POKEBALL", "GREATBALL", "POTION", "SUPERPOTION", "ANTIDOTE", "PARALYZEHEAL", "AWAKENING",
                "BURNHEAL", "ICEHEAL", "REPEL", "MAXREPEL", "FULLHEAL"));
        context.gameState().trainer().money = 12000;
        overlay.openRequest(request);
        idle(40);
        renderOverlay();
        shot("mart-1-greeting");
        idle(120);
        renderOverlay();
        shot("mart-2-commands");
        tapAndRender(GameAction.CONFIRM, overlay);        // 购买
        idle(4);
        renderOverlay();
        shot("mart-3-buy-list");
        tapAndRender(GameAction.DOWN, overlay);
        tapAndRender(GameAction.DOWN, overlay);
        tapAndRender(GameAction.DOWN, overlay);
        idle(2);
        renderOverlay();
        shot("mart-4-buy-list-scrolled");
        tapAndRender(GameAction.CONFIRM, overlay);        // choose Super Potion
        idle(120);
        renderOverlay();
        shot("mart-5-number");
        tapAndRender(GameAction.UP, overlay);
        tapAndRender(GameAction.UP, overlay);
        renderOverlay();
        shot("mart-6-number-3");
        tapAndRender(GameAction.CONFIRM, overlay);
        idle(120);
        renderOverlay();
        shot("mart-7-confirm");
        tapAndRender(GameAction.CONFIRM, overlay);        // 是的
        idle(120);
        renderOverlay();
        shot("mart-8-thanks");
        tapAndRender(GameAction.CONFIRM, overlay);
        idle(4);
        tapAndRender(GameAction.CANCEL, overlay);         // leave the buy list
        idle(80);
        renderOverlay();
        shot("mart-9-anything-else");
        System.out.println("mart capture: money=" + context.gameState().trainer().money
                + " superpotion=" + context.gameState().inventory().count("SUPERPOTION"));
    }

    /** One tap: press, let the target update, clear the edge, release. */
    private void tapAndRender(GameAction action, PauseMenuOverlay target) {
        InputManager input = context.inputManager();
        input.beginFrame();
        input.press(action);
        if (target != null) {
            target.update(1f / 60f);
        }
        input.endFrame();
        input.release(action);
    }

    private void shot(String name) {
        buffer.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (name.startsWith("l1-title") || name.startsWith("l14-title")) {
            title.render(1f / 60f);
            // The title screen applies its own FitViewport; the capture renders
            // several screens into the same framebuffer, so restore the
            // full-size viewport for the read (and any later draw).
            Gdx.gl.glViewport(0, 0, width, height);
        } else {
            renderOverlay();
        }
        writeShot(name);
    }

    /** One tap on the live MapScreen (press, render, clear edge, release). */
    private void stepMap(GameAction action) {
        InputManager input = context.inputManager();
        input.beginFrame();
        input.press(action);
        renderMap();
        input.endFrame();
        input.release(action);
    }

    /**
     * Probe device for the {@code faint} capture (not game logic): leaves the
     * player's lead at 1 HP, so the first damaging hit faints it and
     * {@code pbEORSwitch} asks "要更换宝可梦吗？" deterministically. Only the
     * state this capture starts from is shaped; the battle rules are untouched.
     */
    private void pinFaintCaptureLeadToOneHp() {
        pokemon.runtime.pokemon.TrainerState trainer = context.gameState().trainer();
        if (trainer.partyCount() == 0) {
            throw new IllegalStateException("the faint capture needs a party member");
        }
        pokemon.runtime.pokemon.Pokemon lead = trainer.party.members().get(0);
        lead.hp = 1;
    }

    /**
     * Probe device for the {@code faint} capture (not game logic): drops the
     * status moves from the wild foe that was just created, because the wired
     * wild AI picks among the foe's moves at random ({@code AI_Move:153-155})
     * and a Growl turn does not faint the 1 HP lead. The original moves stay in
     * place when none of them deals damage.
     */
    private void narrowCapturedFoeToDamagingMoves() {
        if (!(context.battlePort() instanceof pokemon.runtime.battle.InteractiveBattlePort)) {
            return;
        }
        pokemon.runtime.battle.InteractiveBattlePort.Session session =
                ((pokemon.runtime.battle.InteractiveBattlePort) context.battlePort()).session();
        pokemon.runtime.battle.Battle battle = session == null ? null : session.battle;
        pokemon.runtime.battle.Battler foe = battle == null ? null : battle.foe();
        if (foe == null || foe.pokemon == null || foe.pokemon.moves.size == 0) {
            return;
        }
        com.badlogic.gdx.utils.Array<pokemon.runtime.pokemon.Pokemon.MoveSlot> damaging =
                new com.badlogic.gdx.utils.Array<>();
        for (pokemon.runtime.pokemon.Pokemon.MoveSlot slot : foe.pokemon.moves) {
            if (slot.move != null && slot.move.power > 0
                    && !"Status".equalsIgnoreCase(slot.move.category)) {
                damaging.add(slot);
            }
        }
        if (damaging.size == 0) {
            return;                                     // keep the original moves
        }
        foe.pokemon.moves.clear();
        foe.pokemon.moves.addAll(damaging);
        System.out.println("faint probe device: foe moves narrowed to " + damaging.size
                + " damaging move(s)");
    }

    private void renderMap() {
        renderFrame(context.game().getScreen(), 1f / 60f);
    }

    /** Renders the live screen for n frames (battle intro / send-out stepping). */
    private void advanceMap(float delta, int frames) {
        Screen current = context.game().getScreen();
        for (int i = 0; i < frames && current != null; i++) {
            renderFrame(current, delta);
        }
    }

    /**
     * B1: renders until the battle screen reports the condition, so the capture
     * lands on a stage rather than on a guessed frame (the battle animations run
     * at 40 fps while the capture renders at 60). Reports the frame count so the
     * log shows how far it ran.
     */
    private BattleScreen battleScreen() {
        Screen current = context.game().getScreen();
        if (!(current instanceof MapScreen)) {
            return null;
        }
        return ((MapScreen) current).activeBattleScreen();
    }

    private void advanceUntilBattle(java.util.function.Predicate<BattleScreen> condition,
            int maxFrames) {
        Screen current = context.game().getScreen();
        if (!(current instanceof MapScreen)) {
            System.out.println("advanceUntilBattle: no map screen ("
                    + (current == null ? "null" : current.getClass().getSimpleName()) + ")");
            return;
        }
        MapScreen map = (MapScreen) current;
        int frames = 0;
        BattleScreen screen = map.activeBattleScreen();
        while (frames < maxFrames && (screen == null || !condition.test(screen))) {
            renderFrame(map, 1f / 60f);
            screen = map.activeBattleScreen();
            frames++;
        }
        System.out.println("advanceUntilBattle: " + frames + " frames, stage="
                + (screen == null ? "no battle yet" : screen.debugStage())
                + (screen == null ? "" : " window=" + screen.debugWindow()
                        + " sendingOut=" + screen.debugSendingOut()
                        + " switch=" + screen.debugSwitch()
                        + " eor=" + screen.debugEor()
                        + " choice=" + screen.debugChoice()
                        + " battlers=[" + screen.debugBattlers() + "]"
                        + " message=" + screen.debugMessage()));
    }

    private void shotMap(String name) {
        buffer.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderMap();
        Screen current = context.game().getScreen();
        System.out.println("map shot " + name + " screen="
                + (current == null ? "null" : current.getClass().getSimpleName())
                + " menuOpen=" + (current instanceof MapScreen && ((MapScreen) current).pauseMenuOpen()));
        if (current instanceof MapScreen && ((MapScreen) current).activeBattleScreen() != null
                && System.getProperty("pokemon.capture.sprites") != null) {
            // -Dpokemon.capture.sprites=1: dump the battle sprite table next to
            // every shot, which is how the data box / faint animation states were
            // checked during the batch.
            System.out.println(((MapScreen) current).activeBattleScreen().debugSprites());
        }
        writeShot(name);
    }

    private void writeShot(String name) {
        buffer.begin(); // re-bind: some screens switch the viewport/framebuffer
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, width, height);
        try {
            File output = new File(args[1], name + ".png");
            if (output.getParentFile() != null) {
                output.getParentFile().mkdirs();
            }
            PixmapIO.writePNG(Gdx.files.absolute(output.getAbsolutePath()), pixels, -1, true);
            System.out.println("wrote " + output.getAbsolutePath());
        } finally {
            pixels.dispose();
        }
        buffer.end();
    }
}
