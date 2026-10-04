package pokemon.runtime.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.ui.WindowSkin;

/**
 * L1: the options screen (BGM / SE / BGS volume, fullscreen). Every change is
 * applied to the AudioManager and persisted through the storage port.
 */
public final class OptionsView {

    public enum Result {
        NONE,
        BACK
    }

    private static final float ROW_HEIGHT = 44f;

    private final RuntimeContext context;
    private int index;

    public OptionsView(RuntimeContext context) {
        this.context = context;
    }

    public Result update(InputManager input, AudioManager audio) {
        GameSettings.Row[] rows = GameSettings.Row.values();
        if (input.wasPressed(GameAction.UP)) {
            index = Math.floorMod(index - 1, rows.length);
            MenuSe.cursor(audio);
        }
        if (input.wasPressed(GameAction.DOWN)) {
            index = Math.floorMod(index + 1, rows.length);
            MenuSe.cursor(audio);
        }
        int delta = 0;
        if (input.wasPressed(GameAction.LEFT)) {
            delta = -1;
        } else if (input.wasPressed(GameAction.RIGHT)) {
            delta = 1;
        } else if (input.wasPressed(GameAction.CONFIRM) && rows[index] == GameSettings.Row.FULLSCREEN) {
            delta = 1;
        }
        if (delta != 0) {
            GameSettings.Row row = rows[index];
            context.settings().adjust(row, delta);
            if (row == GameSettings.Row.FULLSCREEN) {
                applyFullscreen(context.settings().fullscreen);
            }
            context.settingsChanged();
            MenuSe.cursor(audio);
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.buzzer(audio);
            return Result.BACK;
        }
        return Result.NONE;
    }

    /** Desktop: switches the window between fullscreen and windowed mode. */
    private static void applyFullscreen(boolean fullscreen) {
        try {
            if (fullscreen) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            } else {
                Gdx.graphics.setWindowedMode(
                        Math.max(320, ScreenMetrics.logicalWidth() * 2),
                        Math.max(240, ScreenMetrics.logicalHeight() * 2));
            }
        } catch (RuntimeException error) {
            Gdx.app.log("OptionsView", "cannot change the window mode: " + error.getMessage());
        }
    }

    public void render(SpriteBatch batch, MenuAssets assets, MenuFont font, WindowSkin skin) {
        float screenWidth = ScreenMetrics.logicalWidth();
        float screenHeight = ScreenMetrics.logicalHeight();
        GameSettings.Row[] rows = GameSettings.Row.values();
        float windowWidth = 420f;
        float windowHeight = rows.length * ROW_HEIGHT + 96f;
        float x = (screenWidth - windowWidth) / 2f;
        float y = (screenHeight - windowHeight) / 2f;

        if (skin != null) {
            skin.draw(batch, x, y, windowWidth, windowHeight);
        }
        font.drawCentered(batch, "设置", screenWidth / 2f, y + windowHeight - 42f);
        for (int i = 0; i < rows.length; i++) {
            float rowY = y + windowHeight - 88f - i * ROW_HEIGHT;
            boolean current = i == index;
            font.draw(batch, current ? "▶" : "  ", x + 18f, rowY);
            font.draw(batch, rows[i].label(), x + 52f, rowY);
            font.draw(batch, context.settings().value(rows[i]), x + windowWidth - 110f, rowY);
        }
        font.drawCentered(batch, "←→ 调整    Z 确认    X 返回", screenWidth / 2f, y + 26f);
    }
}
