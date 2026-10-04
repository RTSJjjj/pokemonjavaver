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
    }

    private static final float ROW_HEIGHT = 56f;
    private static final float PANORAMA_SPEED = 30f;

    private final RuntimeContext context;
    private final GameDatabase database;
    private final MenuAssets assets;
    private final MenuFont font;
    private final WindowSkin skin;
    private final PauseMenuModel menu;
    private final SaveLoadView saveView = new SaveLoadView(true);
    private final SaveLoadView loadView = new SaveLoadView(false);
    private final OptionsView optionsView;

    private Sub sub = Sub.MAIN;
    private boolean open;
    private float panoramaOffset;
    private TrainerView trainerView;
    private Host host;

    public PauseMenuOverlay(RuntimeContext context, GraphicsLocator locator) {
        this.context = context;
        this.database = context.database();
        this.assets = new MenuAssets(locator);
        this.font = new MenuFont(messageFontFile(locator));
        this.skin = assets.skin(windowskinName());
        this.menu = new PauseMenuModel(context.gameState());
        this.optionsView = new OptionsView(context);
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

    public void close() {
        open = false;
    }

    /** Per frame while open; the map screen freezes the world around this. */
    public void update(float delta) {
        if (!open) {
            return;
        }
        panoramaOffset -= PANORAMA_SPEED * delta;
        InputManager input = context.inputManager();
        switch (sub) {
            case MAIN:
                updateMain(input);
                break;
            case SAVE: {
                SaveLoadView.Result result = saveView.update(input, context.audioManager());
                if (result == SaveLoadView.Result.PICKED) {
                    saveSelected();
                } else if (result == SaveLoadView.Result.CANCELLED) {
                    sub = Sub.MAIN;
                }
                break;
            }
            case LOAD: {
                SaveLoadView.Result result = loadView.update(input, context.audioManager());
                if (result == SaveLoadView.Result.PICKED) {
                    SaveSlots.Slot slot = loadView.selected();
                    if (slot != null && host != null) {
                        host.pauseMenuLoaded(slot.id);
                    }
                } else if (result == SaveLoadView.Result.CANCELLED) {
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
        Texture background = assets.mpm("bg");
        if (background != null) {
            batch.draw(background, 0f, 0f, width, height);
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
            case SAVE:
                saveView.render(batch, assets, font, skin);
                break;
            case LOAD:
                loadView.render(batch, assets, font, skin);
                break;
            case OPTIONS:
                optionsView.render(batch, assets, font, skin);
                break;
            case TRAINER:
                trainerView.render(batch, assets, font, skin);
                break;
            default:
                break;
        }
    }

    private void renderMain(SpriteBatch batch, float width, float height) {
        Texture selection = assets.mpm("sel");
        float top = height - 36f;
        for (int i = 0; i < menu.size(); i++) {
            float rowBottom = top - (i + 1) * ROW_HEIGHT + 6f;
            boolean current = i == menu.index();
            if (current && selection != null) {
                batch.draw(selection, 72f, rowBottom, 440f, 56f);
            }
            PauseMenuModel.Entry entry = menu.entryAt(i);
            if (entry != null) {
                Texture icon = assets.mpm(entry.icon);
                if (icon != null) {
                    batch.draw(icon, 84f, rowBottom + 4f, 48f, 54f);
                }
                font.draw(batch, entry.label, 168f, rowBottom + 36f);
            }
        }
        font.drawCentered(batch, "Z 确认    X 返回", width / 2f, 16f);
    }

    @Override
    public void dispose() {
        assets.dispose();
        font.dispose();
    }
}
