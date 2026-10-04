package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.state.GameState;
import pokemon.runtime.ui.WindowSkin;

/**
 * L1: the "trainer" entry of the pause menu - the stage-2 information that
 * actually exists (name, current map, item kinds, quest counts). The full
 * Trainer Card (badges, money, play time) needs the stage-3 domain.
 */
public final class TrainerView {

    public enum Result {
        NONE,
        BACK
    }

    private static final float ROW_HEIGHT = 40f;

    private final String name;
    private final String mapName;
    private final int itemKinds;
    private final int activeQuests;
    private final int doneQuests;

    public TrainerView(GameDatabase database, GameState state) {
        this.name = state == null || state.playerName() == null || state.playerName().isEmpty()
                ? "训练家"
                : state.playerName();
        String map = "未知";
        if (database != null && state != null && state.currentMapId() >= 0) {
            try {
                MapData data = database.map(state.currentMapId());
                if (data != null && data.name != null && !data.name.isEmpty()) {
                    map = data.name;
                }
            } catch (RuntimeException error) {
                map = "地图 " + state.currentMapId();
            }
        }
        this.mapName = map;
        this.itemKinds = state == null ? 0 : state.inventory().size();
        int active = 0;
        int done = 0;
        if (state != null) {
            for (pokemon.runtime.state.QuestLog.Entry entry : state.quests().entries()) {
                if (entry.status == pokemon.runtime.state.QuestLog.Status.COMPLETED) {
                    done++;
                } else if (entry.status == pokemon.runtime.state.QuestLog.Status.ACTIVE) {
                    active++;
                }
            }
        }
        this.activeQuests = active;
        this.doneQuests = done;
    }

    public Result update(InputManager input, AudioManager audio) {
        if (input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL)
                || input.wasPressed(GameAction.MENU)) {
            MenuSe.buzzer(audio);
            return Result.BACK;
        }
        return Result.NONE;
    }

    public void render(SpriteBatch batch, MenuAssets assets, MenuFont font, WindowSkin skin) {
        float screenWidth = ScreenMetrics.logicalWidth();
        float screenHeight = ScreenMetrics.logicalHeight();
        String[] lines = {
                "训练家：" + name,
                "当前位置：" + mapName,
                "物品：" + itemKinds + " 种",
                "任务：进行中 " + activeQuests + " / 已完成 " + doneQuests,
        };
        float windowWidth = 360f;
        float windowHeight = lines.length * ROW_HEIGHT + 96f;
        float x = (screenWidth - windowWidth) / 2f;
        float y = (screenHeight - windowHeight) / 2f;
        if (skin != null) {
            skin.draw(batch, x, y, windowWidth, windowHeight);
        }
        font.drawCentered(batch, "训练家", screenWidth / 2f, y + windowHeight - 42f);
        for (int i = 0; i < lines.length; i++) {
            font.draw(batch, lines[i], x + 32f, y + windowHeight - 88f - i * ROW_HEIGHT);
        }
        font.drawCentered(batch, "X 返回", screenWidth / 2f, y + 26f);
    }
}
