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
    }

    private enum Sub {
        MAIN,
        SAVE,
        LOAD,
        OPTIONS,
        TRAINER
        , PARTY, BAG, POKEDEX, STORAGE, PC, TRADE, SETUP, MAP
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
    private final PauseMenuModel menu;
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
    private StorageView storageView;
    private PokeCenterPcView pcView;
    private TradeView tradeView;
    private GenderSelectorView genderView;
    private TownMapView townMapView;
    private Texture snapshot;
    /** The view a bag opened from it returns to (party / storage / PC). */
    private Sub bagReturnSub;
    private java.util.function.Consumer<String> itemPickCallback;
    private boolean storageReturnsToParty;

    /** The party summary's "携带道具": open the bag to pick a held item. */
    private void openBagForHold(pokemon.runtime.pokemon.Pokemon target) {
        bagView = new BagView(context);
        bagView.chooseHold(target);
        bagReturnSub = sub;
        sub = Sub.BAG;
    }

    /** B2W2 PC:1286-1294 pbChooseItem: the bag picks one item and hands it back to the storage screen. */
    private void openBagForStorageItem(java.util.function.Predicate<String> filter, java.util.function.Consumer<String> callback) {
        bagView = new BagView(context);
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

    public void openRequest(pokemon.runtime.event.MenuService.Request value) {
        open(); request = value;
        if (value.kind == pokemon.runtime.event.MenuService.Kind.STORAGE) {
            // OPEN_PC = pbPokeCenterPC (PScreen_PC:211-221)
            pcView = new PokeCenterPcView(context);
            pcView.itemHost(this::openBagForStorageItem);
            pcView.bagHost(this::openBagForHold);
            sub = Sub.PC;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.GENDER) { genderView = new GenderSelectorView(context, value); sub = Sub.SETUP; }
        // pbChooseItemScreen(filter): the bag is only there to pick one item.
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.CHOOSE_ITEM) {
            bagView = new BagView(context);
            bagView.chooseItem(value.filter);
            sub = Sub.BAG;
        }
        else if (value.kind == pokemon.runtime.event.MenuService.Kind.SHOW_MAP) {
            // pbShowMap(region, wallmap) (PScreen_RegionMap:431-437).
            townMapView = new TownMapView(context, assets, value.region, value.wallmap);
            sub = Sub.MAP;
        }
        else { tradeView = new TradeView(context, value); sub = Sub.TRADE; }
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
        this.menu = new PauseMenuModel(context.gameState());
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

    public void open() {
        open = true;
        sub = Sub.MAIN;
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
        open = false;
    }

    /** Per frame while open; the map screen freezes the world around this. */
    public void update(float delta) {
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
                if (partyView.update(input)) sub = Sub.MAIN;
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
            case TRADE:
                if (tradeView.update(input)) back();
                break;
            case SETUP:
                if (genderView.update(input)) { genderView.dispose(); back(); }
                break;
            case MAP:
                if (townMapView.update(input, delta)) back();
                break;
            default:
                break;
        }
    }

    private void updateMain(InputManager input) {
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
                partyView.storageHost(this::openStorageFromParty); sub = Sub.PARTY; break;
            case BAG:
                bagView = new BagView(context); sub = Sub.BAG; break;
            case POKEDEX:
                pokedexView = new PokedexView(context); sub = Sub.POKEDEX; break;
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
                break;
            case PARTY: partyView.render(batch, assets, font, skin, smallFont); break;
            case BAG: bagView.render(batch, assets, font, skin, speech, detailFont); break;
            case POKEDEX: pokedexView.render(batch, assets, font, skin, detailFont); break;
            case STORAGE: storageView.render(batch, assets, font, skin, speech, detailFont); break;
            case PC: pcView.render(batch, assets, font, skin, speech, detailFont); break;
            case TRADE: tradeView.render(batch, assets, font, skin); break;
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
        font.draw(batch, String.format("%d年%d月%d日 %02d:%02d", now.getYear(), now.getMonthValue(),
                now.getDayOfMonth(), now.getHour(), now.getMinute()), 16f, height - 24f);
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
