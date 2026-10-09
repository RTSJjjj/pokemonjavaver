package pokemon.runtime.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.ui.WindowSkin;

import java.io.File;

/**
 * L1: the pause menu overlay (Modular Pause Menu visuals: background, scrolling
 * panorama, icon column and selection bar). It owns the sub views (save, load,
 * options, trainer) and reports load/title requests to its host (MapScreen).
 */
public final class PauseMenuOverlay implements Disposable {

    /** Host callbacks: the map screen swaps itself for a loaded/title state. */
    public interface Host {
        void pauseMenuLoaded(String slot);

        void pauseMenuTitle();

        /** Modular Menu:198-206: the player quit the Safari Zone (decision = 1, pbGoToStart). */
        void pauseMenuSafariQuit();

        /** {@code ItemHandlers::UseFromBag} of the map items: the refusal lines, or null when the item can be used here. */
        String[] pauseMenuMapItemCheck(String item);
    }

    private enum Sub {
        MAIN,
        SAVE,
        LOAD,
        OPTIONS,
        TRAINER
        , PARTY, BAG, POKEDEX, STORAGE, PC, TRADE_SCENE, FORGET, RELEARN, HATCH, HALL, CREDITS, TRAINER_BADGES, MINIGAME, SLOTS, HATCHER, SETUP, MAP, MART, STARTER, QUESTS
    }

    private static final float ROW_HEIGHT = 56f;
    private static final float PANORAMA_SPEED = 30f;

    private final RuntimeContext context;
    private final GameDatabase database;
    private final MenuAssets assets;
    private final MenuFont font;
    private final MenuFont smallFont;
    private final MenuFont detailFont;
    private WindowSkin skin;
    /** The message/speech frame (MessageConfig::TextSkinName / $SpeechFrames). */
    private WindowSkin speech;
    /** The settings values {@link #skin}/{@link #speech} were loaded from. */
    private int themeFrame = -1;
    private int themeTextskin = -1;
    private PauseMenuModel menu;
    private final PbMessage quitMessage;
    private final MenuClock quitClock = new MenuClock();
    private final OptionsView optionsView;
    private final SaveView saveView;
    private final LoadView loadView;

    private Sub sub = Sub.MAIN;
    private boolean open;
    private float panoramaOffset;
    private TrainerView trainerView;
    private PartyView partyView;
    private BagView bagView;
    private PokedexView pokedexView;
    private QuestListView questView;
    private StorageView storageView;
    private PokeCenterPcView pcView;
    private MartView martView;
    private StarterSelectionView starterView;
    private TradeSceneView tradeView;
    private SummaryView forgetView;
    private RelearnerView relearnView;
    private HatchSceneView hatchView;
    private HallOfFameView hallView;
    private CreditsView creditsView;
    private SlotMachineView slotView;
    private MiniGame miniGame;
    private HatcherView hatcherView;
    private Sub hatcherReturnSub = Sub.MAIN;
    private GenderSelectorView genderView;
    private TownMapView townMapView;
    private Texture snapshot;
    private pokemon.runtime.field.ItemHandlers itemHandlers;
    /** Set while the party screen runs an item of the bag: the view that returns to the bag. */
    private Sub partyReturnSub;

    private pokemon.runtime.field.ItemHandlers itemHandlers() {
        if (itemHandlers == null) {
            itemHandlers = new pokemon.runtime.field.ItemHandlers(context.pbsData(), context.gameState(), java.time.LocalTime::now);
        }
        return itemHandlers;
    }

    /** Opens the Egg Hatcher on top of the map (the screenshots of MenuCapture). */
    public void openHatcher() {
        open();
        hatcherView = new HatcherView(context);
        hatcherReturnSub = Sub.MAIN;
        sub = Sub.HATCHER;
    }

    private String fieldItemId;

    /** The map item the bag chose (the fishing rod), once; null when none. */
    public String takeFieldItem() {
        String item = fieldItemId;
        fieldItemId = null;
        return item;
    }

    private BagView newBagView() {
        BagView view = new BagView(context);
        view.townMapHost(() -> {
            townMapView = new TownMapView(context, assets, -1, false);       // pbShowMap(-1, false)
            mapReturnsToBag = true;
            sub = Sub.MAP;
        });
        view.mapItems(new BagView.MapItems() {
            @Override
            public String[] unusable(String item) {
                return host == null ? new String[] {"这里不能使用。"} : host.pauseMenuMapItemCheck(item);
            }

            @Override
            public void use(String item) {
                fieldItemId = item;
                close();                                                       // UseFromBag answered 2: every screen ends
            }
        });
        view.useHost(this::openPartyForItem);
        view.hatcherHost(() -> {
            hatcherView = new HatcherView(context);                           // openHatcher
            hatcherReturnSub = sub;
            sub = Sub.HATCHER;
        });
        view.endScreen(this::close);
        return view;
    }

    /** pbUseItem: {@code pbFadeOutIn { PokemonParty_Scene ... }} runs the item handler on the party screen. */
    private void openPartyForItem(java.util.function.Consumer<pokemon.runtime.field.ItemScene> body) {
        partyView = PartyView.forItem(context, body, null);
        partyReturnSub = sub;
        sub = Sub.PARTY;
    }

    /** The view a bag opened from it returns to (party / storage / PC). */
    private Sub bagReturnSub;
    private java.util.function.Consumer<String> itemPickCallback;
    private boolean storageReturnsToParty;

    /** The party summary's "携带道具": open the bag to pick a held item. */
    private void openBagForHold(pokemon.runtime.pokemon.Pokemon target) {
        bagView = newBagView();
        bagView.chooseHold(target);
        bagReturnSub = sub;
        sub = Sub.BAG;
    }

    /** B2W2 PC:1286-1294 pbChooseItem: the bag picks one item and hands it back to the storage screen. */
    private void openBagForStorageItem(java.util.function.Predicate<String> filter, java.util.function.Consumer<String> callback) {
        bagView = newBagView();
        bagView.chooseItem(filter);
        itemPickCallback = callback;
        bagReturnSub = sub;
        sub = Sub.BAG;
    }

    private void wire(StorageView view) {
        view.itemHost(this::openBagForStorageItem);
        view.bagHost(this::openBagForHold);
    }

    /** PScreen_Party:925-930: [F] opens the storage system and returns to the party screen. */
    private void openStorageFromParty() {
        storageView = new StorageView(context);
        wire(storageView);
        storageReturnsToParty = true;
        sub = Sub.STORAGE;
    }

    /** Downsample the previous world frame once; linear upscaling gives the MPM blur. */
    public void captureBackground(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        com.badlogic.gdx.graphics.Pixmap source = com.badlogic.gdx.graphics.Pixmap.createFromFrameBuffer(x, y, width, height);
        com.badlogic.gdx.graphics.Pixmap small = new com.badlogic.gdx.graphics.Pixmap(Math.max(1, width / 6), Math.max(1, height / 6), source.getFormat());
        try {
            small.setFilter(com.badlogic.gdx.graphics.Pixmap.Filter.BiLinear);
            small.drawPixmap(source, 0, 0, width, height, 0, 0, small.getWidth(), small.getHeight());
            if (snapshot != null) snapshot.dispose();
            snapshot = new Texture(small);
            snapshot.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } finally { source.dispose(); small.dispose(); }
    }
    private pokemon.runtime.event.MenuService.Request request;

    /** {@code PokemonPartyScreen#pbChooseAblePokemon} (210_PScreen_Party:1244-1269) with {@code proc { |pkmn| !pkmn.egg? }}. */
    private void chooseAble(pokemon.runtime.event.MenuService.Request value, pokemon.runtime.field.ItemScene scene) {
        pokemon.runtime.pokemon.Party party = context.gameState().trainer().party;
        String[] annot = new String[party.size()];
        boolean[] eligibility = new boolean[party.size()];
        for (int i = 0; i < annot.length; i++) {
            pokemon.runtime.pokemon.Pokemon pkmn = party.get(i);
            if ("relearnable".equals(value.ableProc)) {
                eligibility[i] = pokemon.runtime.pokemon.MoveRelearner.hasRelearnableMove(pkmn, context.pbsData());   // proc { |p| pbHasRelearnableMove?(p) }
            } else {
                eligibility[i] = pkmn != null && !pkmn.egg;                                               // 252:269 pbChooseNonEggPokemon
            }
            annot[i] = eligibility[i] ? "可以授予" : "不能被授予";                                            // :1250
        }
        String help = party.size() > 1 ? "请选择宝可梦。" : "选择宝可梦或取消。";                                // :1254
        scene.pbStartScene(help, annot);
        int ret = -1;
        while (true) {
            scene.pbSetHelpText(help);                                                                    // :1256-1257
            int pkmnid = scene.pbChoosePokemon(help);                                                     // :1258
            if (pkmnid < 0) break;                                                                        // :1259
            if (!eligibility[pkmnid] && !value.allowIneligible) {                                         // :1260
                scene.pbDisplay("这个宝可梦不能参加。");                                                     // :1261
            } else {
                ret = pkmnid;                                                                             // :1263
                break;
            }
        }
        value.complete(ret, ret >= 0 ? party.get(ret).name : "");                                          // 252:260-265 pbSet
    }

    /** {@code PokemonPartyScreen#pbChooseTradablePokemon} (210_PScreen_Party:1270-1296) for {@code pbChoosePokemonForTrade}. */
    private void chooseTradable(pokemon.runtime.event.MenuService.Request value, pokemon.runtime.field.ItemScene scene) {
        pokemon.runtime.pokemon.Party party = context.gameState().trainer().party;
        String[] annot = new String[party.size()];
        boolean[] eligibility = new boolean[party.size()];
        for (int i = 0; i < annot.length; i++) {
            pokemon.runtime.pokemon.Pokemon pkmn = party.get(i);
            boolean elig = pkmn != null && pkmn.species != null && pkmn.species.internalName.equals(value.wanted);   // :299-303 pkmn.species==wanted
            if (pkmn == null || pkmn.egg) elig = false;                                                              // :1275
            eligibility[i] = elig;
            annot[i] = elig ? "可以使用" : "无效";                                                                     // :1277
        }
        String help = party.size() > 1 ? "请选择宝可梦。" : "选择宝可梦或取消。";                                          // :1281
        scene.pbStartScene(help, annot);
        int ret = -1;
        while (true) {                                                                                               // :1282
            scene.pbSetHelpText(help);                                                                               // :1283
            int pkmnid = scene.pbChoosePokemon(help);                                                                // :1285
            if (pkmnid < 0) break;                                                                                   // :1286
            if (!eligibility[pkmnid]) {                                                                              // :1287
                scene.pbDisplay("这个宝可梦不能参加。");                                                                 // :1288
            } else {
                ret = pkmnid;                                                                                        // :1290
                break;
            }
        }
        // 252:284-293 pbSet(variable, chosen); pbSet(nameVar, name or "")
        value.complete(ret, ret >= 0 ? party.get(ret).name : "");
    }

    public void openRequest(pokemon.runtime.event.MenuService.Request value) {
        open(); request = value;
        if (value.kind == pokemon.runtime.event.MenuService.Kind.STORAGE) {
            // OPEN_PC = pbPokeCenterPC (PScreen_PC:211-221)
            pcView = new PokeCenterPcView(context);
            pcView.itemHost(this::openBagForStorageItem);
            pcView.bagHost(this::openBagForHold);
            sub = Sub.PC;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.TRAINER_PC) {
            pcView = new PokeCenterPcView(context, true);                         // pbTrainerPC
            pcView.itemHost(this::openBagForStorageItem);
            pcView.bagHost(this::openBagForHold);
            sub = Sub.PC;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.GENDER) { genderView = new GenderSelectorView(context, value); sub = Sub.SETUP; }
        // pbChooseItemScreen(filter): the bag is only there to pick one item.
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.CHOOSE_ITEM) {
            bagView = newBagView();
            bagView.chooseItem(value.filter);
            sub = Sub.BAG;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.TUTOR) {
            // pbMoveTutorChoose: the party screen with the tutor's annotations (253_PSystem_Utilities:982-1018).
            pokemon.runtime.pokemon.PbsData.Move tutorMove = context.pbsData() == null ? null : context.pbsData().move(value.move);
            partyView = PartyView.forItem(context, scene -> {
                int chosen = tutorMove == null ? -1
                        : itemHandlers().pbMoveTutorChoose(tutorMove, value.movelist, value.byMachine, scene);
                value.complete(chosen, "");
            }, null);
            partyReturnSub = null;
            sub = Sub.PARTY;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.STARTER) {
            // DiegoWTsStarterSelection.new(a,b,c) (Starter Selection script, section 333).
            starterView = new StarterSelectionView(context, value.dex);
            sub = Sub.STARTER;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.MART) {
            // pbPokemonMart(stock, speech, cantsell) (PScreen_Mart:807-846).
            martView = new MartView(context, value.items, value.speech, value.cantSell);
            sub = Sub.MART;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.SHOW_MAP) {
            // pbShowMap(region, wallmap) (PScreen_RegionMap:431-437).
            townMapView = new TownMapView(context, assets, value.region, value.wallmap);
            sub = Sub.MAP;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.CHOOSE_NON_EGG) {
            // pbChooseNonEggPokemon -> pbChoosePokemon(.., proc { !egg? }) -> PokemonPartyScreen#pbChooseAblePokemon
            partyView = PartyView.forItem(context, scene -> chooseAble(value, scene), null);
            partyReturnSub = null;
            sub = Sub.PARTY;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.CHOOSE_ABLE) {
            // pbChoosePokemon with an able proc -> PokemonPartyScreen#pbChooseAblePokemon (210_PScreen_Party:1244-1269)
            partyView = PartyView.forItem(context, scene -> chooseAble(value, scene), null);
            partyReturnSub = null;
            sub = Sub.PARTY;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.MINIGAME) {
            miniGame = MiniGames.create(context, value.wanted, value);              // pbMiningGame / pbVoltorbFlip / pbTriad...
            if (miniGame == null) {
                value.complete(-1, "");
                return;
            }
            sub = Sub.MINIGAME;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.SLOT_MACHINE) {
            slotView = new SlotMachineView(context, value.index);                 // SlotMachineScene
            sub = Sub.SLOTS;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.TRAINER_CARD_BADGES) {
            trainerView = new TrainerView(database, context.gameState(), true);   // pbStartBadgeScreen
            sub = Sub.TRAINER_BADGES;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.CREDITS) {
            creditsView = new CreditsView(context);                               // $scene = Scene_Credits.new
            sub = Sub.CREDITS;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.HALL_OF_FAME) {
            hallView = HallOfFameView.entry(context);                             // pbHallOfFameEntry
            sub = Sub.HALL;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.HATCH) {
            hatchView = new HatchSceneView(context, value.pokemon);               // pbHatchAnimation
            sub = Sub.HATCH;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.RELEARN) {
            relearnView = new RelearnerView(context, value.pokemon);          // pbRelearnMoveScreen
            sub = Sub.RELEARN;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.FORGET_MOVE) {
            // pbForgetMove (188_PItem_Items:823-830): the summary screen's forget mode
            forgetView = SummaryView.forForget(context, value.pokemon, value.learnMove);
            sub = Sub.FORGET;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.CHOOSE_TRADE) {
            // pbChoosePokemonForTrade -> pbChooseTradablePokemon (252_PSystem_PokemonUtilities:277-296, 210_PScreen_Party:1270-1296)
            partyView = PartyView.forItem(context, scene -> chooseTradable(value, scene), null);
            partyReturnSub = null;
            sub = Sub.PARTY;
        }
        else { tradeView = new TradeSceneView(context, value.index, value.offered, value.nickname, value.trainerName); sub = Sub.TRADE_SCENE; }
    }
    private void back() {
        if (request == null) sub = Sub.MAIN;
        else { if (!request.done) request.complete(-1, ""); request = null; close(); }
    }
    private Host host;

    public PauseMenuOverlay(RuntimeContext context, GraphicsLocator locator) {
        this.context = context;
        this.database = context.database();
        this.assets = new MenuAssets(locator);
        this.font = new MenuFont(messageFontFile(locator));
        this.smallFont = new MenuFont(messageFontFile(locator), 20); // pbSetSmallFont
        this.detailFont = new MenuFont(messageFontFile(locator), 20);
        this.skin = loadSystemFrame();
        this.speech = loadSpeechFrame();
        themeFrame = context.settings().frame;
        themeTextskin = context.settings().textskin;
        this.menu = new PauseMenuModel(context.gameState(), inSafari());
        this.quitMessage = new PbMessage(context);
        this.optionsView = new OptionsView(context);
        this.saveView = new SaveView(context);
        this.loadView = new LoadView(context);
    }

    private File messageFontFile(GraphicsLocator locator) {
        String name = null;
        if (database != null && database.project() != null && database.project().runtime != null
                && database.project().runtime.hasMessageFont()) {
            name = database.project().runtime.messageFont;
        }
        return name == null || locator == null ? null : locator.font(name);
    }

    private String windowskinName() {
        return database == null || database.system() == null ? "" : database.system().windowskinName;
    }

    /**
     * The system window frame the game's {@code Window_CommandPokemon} uses:
     * {@code MessageConfig.pbGetSystemFrame} = {@code $TextFrames[$PokemonSystem.frame]}
     * whose default (frame 0) is {@code ChoiceSkinName} = "choice 1" (a light
     * 48x48 frame). The System.rxdata windowskin (001-Blue01) is only the
     * fallback.
     */
    private WindowSkin loadSystemFrame() {
        String name = frameName(context.settings().frame);
        WindowSkin choice = assets.skin(name);
        return choice != null ? choice : assets.skin("choice 1");
    }

    /** MessageConfig::pbGetSpeechFrame / $SpeechFrames[$PokemonSystem.textskin]. */
    private WindowSkin loadSpeechFrame() {
        WindowSkin speechSkin = assets.skin(speechName(context.settings().textskin));
        return speechSkin != null ? speechSkin : loadSystemFrame();
    }

    private static String frameName(int index) {
        return index >= 0 && index < GameSettings.TEXT_FRAMES.length
                ? GameSettings.TEXT_FRAMES[index] : "choice 1";
    }

    private static String speechName(int index) {
        return index >= 0 && index < GameSettings.SPEECH_FRAMES.length
                ? GameSettings.SPEECH_FRAMES[index] : "speech bw 1";
    }

    /** Re-loads the window skins when the 菜单栏/对话栏 options changed. */
    private void reloadTheme() {
        int frame = context.settings().frame;
        int textskin = context.settings().textskin;
        if (frame == themeFrame && textskin == themeTextskin) {
            return;
        }
        themeFrame = frame;
        themeTextskin = textskin;
        skin = loadSystemFrame();
        speech = loadSpeechFrame();
    }

    public void host(Host host) {
        this.host = host;
    }

    public boolean isOpen() {
        return open;
    }

    private pokemon.runtime.pokemon.Pokemon hiddenMovePokemon;
    private String hiddenMoveId;

    /** {@code PokemonRegionMapScreen#pbStartFlyScreen} (214_PScreen_RegionMap:415-420). 登记: the fly map is not wired yet. */
    private void openFlyMap(java.util.function.Consumer<int[]> done) {
        townMapView = new TownMapView(context, assets, -1, false);
        townMapView.flyMode(true);
        flyCallback = done;
        sub = Sub.MAP;
    }

    private java.util.function.Consumer<int[]> flyCallback;
    /** The region map was opened from the bag (the Town Map): closing it goes back to the bag. */
    private boolean mapReturnsToBag;

    /** The hidden move chosen in the party screen, once; null when none. */
    public String takeHiddenMove(pokemon.runtime.pokemon.Pokemon[] pokemonOut) {
        String move = hiddenMoveId;
        if (move != null) {
            pokemonOut[0] = hiddenMovePokemon;
        }
        hiddenMoveId = null;
        hiddenMovePokemon = null;
        return move;
    }

    /** 292_Splash_Message: the tagline picked when the menu opens (287_Modular_Pause_Menu:165). */
    private String splashMessage = "";
    private final java.util.Random splashRandom = new java.util.Random();

    private boolean inSafari() {
        return context.gameState().fieldGlobals().inSafari(context.gameState().currentMapId());
    }

    public void open() {
        int keptIndex = menu.index();
        menu = new PauseMenuModel(context.gameState(), inSafari());            // the entries follow pbInSafari?
        menu.index(keptIndex);                                                 // the cursor stays where it was
        splashMessage = SplashMessages.sample(splashRandom);
        open = true;
        sub = Sub.MAIN;
        fade = Fade.NONE;
        MenuSe.open(context.audioManager());
    }

    /** Capture/test hook: open the menu directly on one entry. */
    public void openAt(PauseMenuModel.Action action) {
        open();
        execute(action);
    }

    public void close() {
        if (request != null && !request.done) request.complete(-1, "");
        request = null;
        fade = Fade.NONE;
        open = false;
    }

    // ---- pbFadeOutIn around every menu entry (288_Modular_Menu:42-250, 062_MessageConfig:542-569) ----

    private enum Fade { NONE, OUT, IN }

    /** {@code numFrames = (40*0.4).floor} and {@code alphaDiff = (255.0/numFrames).ceil}. */
    private static final int FADE_FRAMES = 16;
    private static final int FADE_STEP = 16;
    private Fade fade = Fade.NONE;
    private int fadeFrame;
    private Sub fadeTo = Sub.MAIN;
    private final MenuClock fadeClock = new MenuClock();

    /** The entries that run inside {@code pbFadeOutIn(99999) { ... }}: the pause menu to a screen and back. */
    private static boolean fadedEntry(Sub from, Sub to) {
        Sub other = from == Sub.MAIN ? to : to == Sub.MAIN ? from : null;
        if (other == null) {
            return false;
        }
        switch (other) {
            case SAVE: case LOAD: case OPTIONS: case TRAINER: case PARTY: case BAG: case POKEDEX: case STORAGE: case QUESTS:
                return true;
            default:
                return false;
        }
    }

    /** Per frame while open; the map screen freezes the world around this. */
    public void update(float delta) {
        if (!open) {
            return;
        }
        if (fade != Fade.NONE) {
            panoramaOffset -= PANORAMA_SPEED * delta;
            fadeFrame += fadeClock.advance();
            if (fadeFrame > FADE_FRAMES) {                      // for j in 0..numFrames
                if (fade == Fade.OUT) {
                    sub = fadeTo;                                // the block runs behind the black screen
                    fade = Fade.IN;
                    fadeFrame = 0;
                } else {
                    fade = Fade.NONE;
                }
            }
            return;
        }
        Sub before = sub;
        updateInner(delta);
        if (open && request == null && sub != before && fadedEntry(before, sub)) {
            fadeTo = sub;
            sub = before;                                        // keep showing the screen that is being left
            fade = Fade.OUT;
            fadeFrame = 0;
            fadeClock.advance();
        }
    }

    private void updateInner(float delta) {
        if (!open) {
            return;
        }
        reloadTheme();
        panoramaOffset -= PANORAMA_SPEED * delta;
        InputManager input = context.inputManager();
        switch (sub) {
            case MAIN:
                updateMain(input);
                break;
            case SAVE: {
                SaveView.Result result = saveView.update(input, context.audioManager());
                if (result == SaveView.Result.PICKED) {
                    saveSelected();
                } else if (result == SaveView.Result.CANCELLED) {
                    sub = Sub.MAIN;
                }
                break;
            }
            case LOAD: {
                LoadView.Result result = loadView.update(input, context.audioManager());
                if (result == LoadView.Result.LOADED) {
                    SaveSlots.Slot slot = loadView.selectedSlot();
                    if (slot != null && host != null) {
                        host.pauseMenuLoaded(slot.id);
                    }
                } else if (result == LoadView.Result.CANCELLED) {
                    sub = Sub.MAIN;
                }
                break;
            }
            case OPTIONS:
                if (optionsView.update(input, context.audioManager()) == OptionsView.Result.BACK) {
                    sub = Sub.MAIN;
                }
                break;
            case TRAINER:
                if (trainerView.update(input, context.audioManager()) == TrainerView.Result.BACK) {
                    sub = Sub.MAIN;
                }
                break;
            case PARTY:
                if (partyView.update(input)) {
                    if (partyView.hiddenMovePokemon() != null) {
                        // 206_PScreen_PauseMenu:183 pbUseHiddenMove(hiddenmove[0], hiddenmove[1]) once the menu has closed.
                        hiddenMovePokemon = partyView.hiddenMovePokemon();
                        hiddenMoveId = partyView.hiddenMoveId();
                        partyView.clearHiddenMove();
                        partyReturnSub = null;
                        close();
                    } else if (partyReturnSub != null) {
                        sub = partyReturnSub;
                        partyReturnSub = null;
                        if (bagView != null) bagView.afterUse();
                    } else if (request != null) {
                        back();                                          // a request's party screen (the move tutor)
                    } else {
                        sub = Sub.MAIN;
                    }
                }
                break;
            case BAG:
                if (bagView.update(input)) {
                    if (request != null && request.kind == pokemon.runtime.event.MenuService.Kind.CHOOSE_ITEM) {
                        // pbChooseItemScreen: hand the chosen item id back (the
                        // plugin returns PBItems id, 0 when cancelled).
                        String picked = bagView.pickedItem();
                        pokemon.runtime.pokemon.PbsData.Item pickedData = picked == null
                                || context.pbsData() == null ? null : context.pbsData().item(picked);
                        request.complete(pickedData == null ? -1 : pickedData.id, picked == null ? "" : picked);
                        request = null;
                        close();
                    } else if (itemPickCallback != null) {
                        java.util.function.Consumer<String> callback = itemPickCallback;
                        itemPickCallback = null;
                        sub = bagReturnSub == null ? Sub.MAIN : bagReturnSub;
                        bagReturnSub = null;
                        callback.accept(bagView.pickedItem());
                    } else if (bagReturnSub != null) {
                        sub = bagReturnSub;
                        bagReturnSub = null;
                    } else {
                        sub = Sub.MAIN;
                    }
                }
                break;
            case QUESTS:
                if (questView.update(input)) sub = Sub.MAIN;
                break;
            case POKEDEX:
                if (pokedexView.update(input)) sub = Sub.MAIN;
                break;
            case STORAGE:
                if (storageView.update(input)) {
                    storageView.dispose();
                    if (storageReturnsToParty) {
                        storageReturnsToParty = false;
                        sub = Sub.PARTY;
                    } else back();
                }
                break;
            case PC:
                if (pcView.update(input)) back();
                break;
            case MART:
                if (martView.update(input)) { martView.dispose(); back(); }
                break;
            case STARTER:
                if (starterView.update(input)) {
                    if (request != null) request.complete(starterView.chosen(), "");
                    starterView.dispose();
                    back();
                }
                break;
            case HATCHER:
                if (hatcherView.update(input)) {
                    hatcherView = null;
                    sub = hatcherReturnSub;
                }
                break;
            case MINIGAME:
                if (miniGame.update(input)) {
                    request.complete(1, "");
                    miniGame.dispose();
                    miniGame = null;
                    back();
                }
                break;
            case SLOTS:
                if (slotView.update(input)) {
                    request.complete(1, "");
                    slotView = null;
                    back();
                }
                break;
            case TRAINER_BADGES:
                if (trainerView.update(input, context.audioManager()) == TrainerView.Result.BACK) {
                    request.complete(1, "");
                    trainerView = null;
                    back();
                }
                break;
            case CREDITS:
                if (creditsView.update(input)) {
                    request.complete(1, "");
                    creditsView = null;
                    back();
                }
                break;
            case HALL:
                if (hallView.update(input)) {
                    request.complete(1, "");
                    hallView = null;
                    back();
                }
                break;
            case HATCH:
                if (hatchView.update(input)) {
                    request.complete(1, "");
                    hatchView = null;
                    back();
                }
                break;
            case RELEARN:
                if (relearnView.update(input)) {
                    request.complete(relearnView.learned() ? 1 : 0, "");
                    relearnView = null;
                    back();
                }
                break;
            case FORGET:
                if (forgetView.update(input)) {
                    request.complete(forgetView.forgetResult(), "");
                    forgetView = null;
                    back();
                }
                break;
            case TRADE_SCENE:
                if (tradeView.update(input)) { request.complete(1, ""); back(); }
                break;
            case SETUP:
                if (genderView.update(input)) { genderView.dispose(); back(); }
                break;
            case MAP:
                if (townMapView.update(input, delta)) {
                    if (flyCallback != null) {
                        java.util.function.Consumer<int[]> callback = flyCallback;
                        flyCallback = null;
                        sub = Sub.PARTY;                                // the party screen goes on (or ends with the chosen place)
                        callback.accept(townMapView.flyResult());
                    } else if (mapReturnsToBag) {
                        mapReturnsToBag = false;
                        sub = Sub.BAG;
                    } else {
                        back();
                    }
                }
                break;
            default:
                break;
        }
    }

    private void updateMain(InputManager input) {
        if (quitMessage.active()) {
            quitMessage.update(input, quitClock.advance());
            return;
        }
        if (input.wasPressed(GameAction.UP)) {
            menu.move(-1);
            MenuSe.cursor(context.audioManager());
        }
        if (input.wasPressed(GameAction.DOWN)) {
            menu.move(1);
            MenuSe.cursor(context.audioManager());
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            MenuSe.decision(context.audioManager());
            execute(menu.selectedAction());
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.close(context.audioManager());
            close();
        }
    }

    private void execute(PauseMenuModel.Action action) {
        if (action == null) {
            return;
        }
        AudioManager audio = context.audioManager();
        switch (action) {
            case PARTY:
                partyView = new PartyView(context); partyView.bagHost(this::openBagForHold);
                partyView.itemHost(this::openBagForStorageItem, itemHandlers());
                partyView.storageHost(this::openStorageFromParty);
                partyView.flyHost(this::openFlyMap); sub = Sub.PARTY; break;
            case BAG:
                bagView = newBagView(); sub = Sub.BAG; break;
            case POKEDEX:
                pokedexView = new PokedexView(context); sub = Sub.POKEDEX; break;
            case QUESTS:
                questView = new QuestListView(context); sub = Sub.QUESTS; break;      // Modular Menu:141-149 QuestList_Scene
            case STORAGE:
                // Modular Menu:70-82 (the pause menu's 寄存系统): SE + the blocked-map check + pbStartScreen(0)
                storageView = new StorageView(context, 0, true);
                wire(storageView);
                sub = Sub.STORAGE;
                break;
            case TRAINER:
                trainerView = new TrainerView(database, context.gameState());
                sub = Sub.TRAINER;
                break;
            case SAVE:
                saveView.refresh(context.storage(), database);
                sub = Sub.SAVE;
                break;
            case LOAD:
                loadView.refresh(context.storage(), database);
                sub = Sub.LOAD;
                break;
            case OPTIONS:
                sub = Sub.OPTIONS;
                break;
            case TITLE:
                if (inSafari()) {                                                   // Modular Menu:197-206 QUIT
                    quitMessage.start("你想退出狩猎吗？", java.util.Arrays.asList("是", "否"), 2, 0, ret -> {   // pbConfirmMessage
                        if (ret != 0) return;
                        close();
                        if (host != null) host.pauseMenuSafariQuit();
                    });
                    break;
                }
                MenuSe.close(audio);
                close();
                if (host != null) {
                    host.pauseMenuTitle();
                }
                break;
            case EXIT:
                Gdx.app.exit();
                break;
            default:
                break;
        }
    }

    private void saveSelected() {
        SaveSlots.Slot slot = saveView.selected();
        if (slot == null) {
            return;
        }
        try {
            context.saveManager().save(context.storage(), slot.id, context.gameState());
            MenuSe.save(context.audioManager());
            saveView.notice("已将进度保存到 " + slot.label + "。");
        } catch (RuntimeException error) {
            Gdx.app.error("PauseMenu", "save failed: " + error.getMessage());
        }
        saveView.refresh(context.storage(), database);
    }

    /** Draws the menu over the frozen map (batch in logical screen space). */
    public void render(SpriteBatch batch) {
        renderInner(batch);
        if (!open || fade == Fade.NONE) {
            return;
        }
        int frame = Math.min(FADE_FRAMES, fadeFrame);
        int alpha = fade == Fade.OUT ? Math.min(255, frame * FADE_STEP) : Math.max(0, (FADE_FRAMES - frame) * FADE_STEP);
        if (alpha > 0) {
            MenuPanel.fill(batch, assets, 0f, 0f, ScreenMetrics.logicalWidth(), ScreenMetrics.logicalHeight(),
                    0f, 0f, 0f, alpha / 255f);
        }
    }

    private void renderInner(SpriteBatch batch) {
        if (!open) {
            return;
        }
        float width = ScreenMetrics.logicalWidth();
        float height = ScreenMetrics.logicalHeight();
        if (sub == Sub.MAP) {
            // The region map draws its own opaque background (mapbg / black),
            // like the plugin's pbFadeOutIn + scene; skip the snapshot/MPM.
            townMapView.render(batch, font);
            return;
        }
        if (snapshot != null) {
            com.badlogic.gdx.graphics.g2d.TextureRegion region = new com.badlogic.gdx.graphics.g2d.TextureRegion(snapshot);
            region.flip(false, true);
            batch.draw(region, 0, 0, width, height);
        }
        if (sub == Sub.MINIGAME && miniGame != null && miniGame.overMap()) {
            // A shop-like mini-game draws over the map (scrolled by pbScrollMap), not over the pause-menu backdrop.
            if (snapshot != null) {
                float shift = miniGame.mapShift();
                com.badlogic.gdx.graphics.g2d.TextureRegion shifted = new com.badlogic.gdx.graphics.g2d.TextureRegion(snapshot);
                shifted.flip(false, true);
                batch.draw(shifted, shift, 0, width, height);
                if (shift > 0f) {                                               // the strip the scroll brought in: the map's own edge
                    com.badlogic.gdx.graphics.g2d.TextureRegion edge = new com.badlogic.gdx.graphics.g2d.TextureRegion(snapshot,
                            0, 0, 1, snapshot.getHeight());
                    edge.flip(false, true);
                    batch.draw(edge, 0, 0, shift, height);
                }
            }
            miniGame.render(batch, assets, font, skin);
            return;
        }
        if (sub == Sub.SETUP) {
            // pbGenderSelector runs on top of the game screen: no pause-menu background.
            genderView.render(batch, assets, font, skin, speech);
            return;
        }
        Texture background = assets.mpm("bg");
        if (background != null) {
            batch.setColor(1, 1, 1, snapshot == null ? 1 : .70f);
            batch.draw(background, 0f, 0f, width, height);
            batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        }
        Texture panorama = assets.mpm("panorama");
        if (panorama != null) {
            float panoramaWidth = panorama.getWidth();
            float offset = panoramaOffset % panoramaWidth;
            if (offset > 0f) {
                offset -= panoramaWidth;
            }
            batch.draw(panorama, offset, 0f);
            batch.draw(panorama, offset + panoramaWidth, 0f);
        }
        switch (sub) {
            case MAIN:
                renderMain(batch, width, height);
                if (quitMessage.active()) quitMessage.render(batch, assets, font, skin, speech, width, height);
                break;
            case PARTY: partyView.render(batch, assets, font, skin, smallFont); break;
            case BAG: bagView.render(batch, assets, font, skin, speech, detailFont); break;
            case POKEDEX: pokedexView.render(batch, assets, font, skin, detailFont); break;
            case QUESTS: questView.render(batch, assets, font, detailFont); break;
            case STORAGE: storageView.render(batch, assets, font, skin, speech, detailFont); break;
            case PC: pcView.render(batch, assets, font, skin, speech, detailFont); break;
            case MART: martView.render(batch, assets, font, skin, speech, detailFont); break;
            case STARTER: starterView.render(batch, assets, font, skin, speech, detailFont); break;
            case HATCHER: hatcherView.render(batch, assets, font, skin, speech, detailFont); break;
            case MINIGAME: miniGame.render(batch, assets, font, skin); break;
            case SLOTS: slotView.render(batch, assets, font, skin); break;
            case TRAINER_BADGES: trainerView.render(batch, assets, font, skin); break;
            case CREDITS: creditsView.render(batch, assets, font, skin); break;
            case HALL: hallView.render(batch, assets, font, skin); break;
            case HATCH: hatchView.render(batch, assets, font, skin); break;
            case RELEARN: relearnView.render(batch, assets, font, skin); break;
            case FORGET: forgetView.render(batch, assets, font, skin); break;
            case TRADE_SCENE: tradeView.render(batch, assets, font, skin); break;
            case SAVE:
                saveView.render(batch, assets, font, skin, speech, detailFont);
                break;
            case LOAD:
                loadView.render(batch, assets, font, skin, detailFont);
                break;
            case OPTIONS:
                optionsView.render(batch, assets, font, skin, speech);
                break;
            case TRAINER:
                trainerView.render(batch, assets, font, skin);
                break;
            default:
                break;
        }
    }

    private void renderMain(SpriteBatch batch, float width, float height) {
        // Trainer portrait on the left (ModularPauseMenu#pbStartScene:
        // x=0, y=Graphics.height-340, native size).
        Texture portrait = assets.mpm(context.gameState().trainer().gender == 1
                ? "intro_Girl1" : "intro_Boy1");
        if (portrait != null) {
            // The plugin places the sprite's TOP at y = height-340 (top-origin);
            // libGDX draws from the bottom-left, so the bottom is 340 - height.
            batch.draw(portrait, 0f, 340f - portrait.getHeight());
        }
        // Entry panels on the right: Graphics/Pictures/MPM/sel (420x72) split
        // into an unselected (left) and a selected (right) half; the icon sits
        // at +8 and the label at +66 (ModularPauseMenu#refresh).
        Texture sel = assets.mpm("sel");
        float panelWidth = sel == null ? 210f : sel.getWidth() / 2f;
        float panelHeight = sel == null ? 72f : sel.getHeight();
        float panelX = width - panelWidth - 40f;
        int count = menu.size();
        int top = count < 4 ? 0 : Math.max(0, menu.index() - 3);
        for (int i = 0; i < count; i++) {
            float panelTop = -(top) * (panelHeight + 12f) + 49f + i * (panelHeight + 12f);
            if (panelTop + panelHeight < 0f || panelTop > height) {
                continue;
            }
            float y = height - panelTop - panelHeight; // RGSS top -> libGDX
            boolean current = i == menu.index();
            boolean visible = i >= top && i <= top + 3;
            float alpha = visible ? 1f : 0.5f;
            float gray = current ? 1f : 0.55f;
            if (sel != null) {
                int srcX = current ? (int) panelWidth : 0;
                batch.setColor(gray, gray, gray, alpha);
                batch.draw(sel, panelX, y, 0f, 0f, panelWidth, panelHeight, 1f, 1f, 0f,
                        srcX, 0, (int) panelWidth, (int) panelHeight, false, false);
            }
            PauseMenuModel.Entry entry = menu.entryAt(i);
            if (entry != null) {
                Texture icon = assets.mpm(entry.icon);
                if (icon != null) {
                    batch.setColor(gray, gray, gray, alpha);
                    batch.draw(icon, panelX + 8f, y, 64f, panelHeight);
                }
                batch.setColor(1f, 1f, 1f, current ? 1f : 0.75f);
                font.draw(batch, entry.label, panelX + 66f, y + 44f);
            }
            batch.setColor(1f, 1f, 1f, 1f);
        }
        // Scroll bar (MPM/scrollbar_bg + a stretched MPM/scrollbar_kn).
        Texture track = assets.mpm("scrollbar_bg");
        if (track != null) {
            batch.draw(track, width - 26f, (height - track.getHeight()) / 2f);
        }
        Texture knob = assets.mpm("scrollbar_kn");
        if (knob != null && count > 0) {
            int pages = count < 4 ? 1 : count - 3;
            float trackHeight = 204f;
            float knobHeight = trackHeight / pages;
            float k = count < 4 ? 0f : Math.max(0, menu.index() - 3);
            float knobTop = (height - trackHeight) / 2f + k * knobHeight;
            batch.draw(knob, width - 28f, height - knobTop - knobHeight, 16f, knobHeight);
        }
        // The plugin's own overlay text: the date/time (top-left) and the
        // version name (bottom-left, CURRENT_NAME from title.json).
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        com.badlogic.gdx.graphics.Color white = com.badlogic.gdx.graphics.Color.WHITE;
        com.badlogic.gdx.graphics.Color shade = new com.badlogic.gdx.graphics.Color(0f, 0f, 0f, 65 / 255f);
        font.draw(batch, String.format("%d年%d月%d日 %02d:%02d", now.getYear(), now.getMonthValue(),
                now.getDayOfMonth(), now.getHour(), now.getMinute()) + " "
                + pokemon.runtime.field.PBDayNight.name(now.toLocalTime()), 16f, height - 24f, white, shade);   // :157-162
        font.draw(batch, splashMessage, 286f, height - 24f,                                                // :165-169
                new com.badlogic.gdx.graphics.Color(1f, 216 / 255f, 0f, 1f),
                new com.badlogic.gdx.graphics.Color(216 / 255f, 128 / 255f, 0f, 1f));
        // :171-186 the mode the game was started in
        String difficulty;
        if (context.gameState().switches().get(197)) {
            difficulty = "懒狗模式";
        } else {
            int mode = context.gameState().variables().get(100);
            difficulty = mode == 2 ? "平均等级" : mode == 1 ? "最高等级" : "简单难度";
        }
        font.draw(batch, "模式：" + difficulty, 16f, height - 50f, white, shade);
        // :188-200 the catching chain
        pokemon.runtime.pokemon.ChainCatching chain = context.gameState().trainer().chainCatching;
        if (chain.chainTimes > 1 && chain.species != null) {
            pokemon.runtime.pokemon.PbsData.Species chained = context.pbsData() == null ? null : context.pbsData().species(chain.species);
            font.draw(batch, "连锁：" + (chained == null ? chain.species : chained.name) + " " + chain.chainTimes + "次",
                    176f, height - 50f, new com.badlogic.gdx.graphics.Color(1f, 1f, 100 / 255f, 1f), shade);
        }
        if (inSafari()) {                                                       // 287_Modular_Pause_Menu:138-140
            pokemon.runtime.state.SafariState safari = context.gameState().fieldGlobals().safari;
            java.util.List<String> content = new java.util.ArrayList<>();
            if (pokemon.runtime.state.SafariState.SAFARI_STEPS > 0) {
                content.add("剩余步数: " + safari.steps + "/" + pokemon.runtime.state.SafariState.SAFARI_STEPS);
            }
            content.add("狩猎球剩余:" + safari.ballcount);
            Texture bar = assets.mpm("partyBar");
            for (int i = 0; i < content.size(); i++) {                          // :155-158
                font.draw(batch, content.get(i), 16f, height - (60f + i * 50f + 22f), white, shade);
                if (bar != null) batch.draw(bar, -2f, height - (92f + i * 50f) - bar.getHeight());
            }
        }
        if (database != null && database.title() != null && !database.title().footerLeft.isEmpty()) {
            font.draw(batch, "版本号：" + database.title().footerLeft, 16f, font.lineHeight() + 4f);
        }
    }

    @Override
    public void dispose() {
        if (snapshot != null) snapshot.dispose();
        assets.dispose();
        font.dispose();
        smallFont.dispose();
        detailFont.dispose();
    }
}
