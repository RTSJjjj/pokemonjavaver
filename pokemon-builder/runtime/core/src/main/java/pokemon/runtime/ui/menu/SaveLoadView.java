package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.app.StoragePort;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.ui.WindowSkin;

/**
 * L1: the save / load slot list (same view inside the pause menu and on the
 * title screen). Save mode writes through SaveManager on PICKED; load mode
 * reports the chosen slot to its host.
 */
public final class SaveLoadView {

    public enum Result {
        NONE,
        PICKED,
        CANCELLED
    }

    private static final float ROW_HEIGHT = 48f;

    private final boolean saveMode;
    private final Array<SaveSlots.Slot> slots = new Array<>();
    private int index;

    public SaveLoadView(boolean saveMode) {
        this.saveMode = saveMode;
    }

    public void refresh(StoragePort storage, GameDatabase database) {
        slots.clear();
        slots.addAll(SaveSlots.list(storage, database));
        index = Math.max(0, Math.min(index, slots.size - 1));
    }

    public boolean saveMode() {
        return saveMode;
    }

    public Result update(InputManager input, AudioManager audio) {
        if (slots.size == 0) {
            return Result.NONE;
        }
        if (input.wasPressed(GameAction.UP)) {
            index = Math.floorMod(index - 1, slots.size);
            MenuSe.cursor(audio);
        }
        if (input.wasPressed(GameAction.DOWN)) {
            index = Math.floorMod(index + 1, slots.size);
            MenuSe.cursor(audio);
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            MenuSe.decision(audio);
            return Result.PICKED;
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.buzzer(audio);
            return Result.CANCELLED;
        }
        return Result.NONE;
    }

    public SaveSlots.Slot selected() {
        return slots.size == 0 ? null : slots.get(index);
    }

    public void render(SpriteBatch batch, MenuAssets assets, MenuFont font, WindowSkin skin) {
        float screenWidth = ScreenMetrics.logicalWidth();
        float screenHeight = ScreenMetrics.logicalHeight();
        float windowWidth = 520f;
        float windowHeight = Math.max(200f, slots.size * ROW_HEIGHT + 84f);
        float x = (screenWidth - windowWidth) / 2f;
        float y = (screenHeight - windowHeight) / 2f;

        if (skin != null) {
            skin.draw(batch, x, y, windowWidth, windowHeight);
        }
        font.drawCentered(batch, saveMode ? "保存游戏" : "读取游戏", screenWidth / 2f,
                y + windowHeight - 42f);
        for (int i = 0; i < slots.size; i++) {
            SaveSlots.Slot slot = slots.get(i);
            float rowY = y + windowHeight - 78f - i * ROW_HEIGHT;
            boolean current = i == index;
            font.draw(batch, current ? "▶" : "  ", x + 18f, rowY);
            font.draw(batch, slot.label, x + 52f, rowY);
            font.draw(batch, slot.describe(), x + 190f, rowY);
        }
        font.drawCentered(batch, "Z 确认    X 返回", screenWidth / 2f, y + 30f);
    }
}
