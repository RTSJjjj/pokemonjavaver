package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.UiSounds;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 363_Dimensionality {@code DimensionalWarpScreen}: the Ethereal Nexus's warp list - a 3x3 grid of places per page (the base
 * list, plus the special places whose switch is on), then a "×取消" cell. C picks a place ({@link #result()} = {@code [map, x, y]}),
 * B or the cancel cell leaves with nothing. The grid navigation is the plugin's own, including its reading of the previous page's
 * visible cells while it changes page (:278-340).
 */
public final class DimensionalWarpView {

    private static final int COLS = 3, ROWS = 3, PER_PAGE = COLS * ROWS;

    /** {@code BASE_WARP_DATA} (363_Dimensionality:8-60): name, map, x, y. */
    private static final Object[][] BASE = {
        {"茶月镇", 2, 36, 7}, {"格诺镇", 7, 29, 11}, {"茸舒镇", 16, 18, 36}, {"苜蓿镇", 8, 14, 27}, {"枫弦镇", 17, 38, 36},
        {"月央市", 12, 29, 43}, {"隐龙市", 15, 50, 44}, {"绯雷市", 9, 33, 34}, {"时风镇", 13, 31, 27}, {"曦寒镇", 11, 44, 22},
        {"柊埃联盟", 59, 26, 11}, {"柊埃联盟（内部）", 158, 15, 10}, {"铃兰市", 69, 32, 12}, {"沃饶镇", 14, 19, 12},
        {"瑞乡镇", 57, 34, 11}, {"培育屋", 38, 35, 14}, {"绯隐牧场", 115, 30, 37}, {"静隐路", 288, 24, 49}, {"森楠岛", 25, 26, 18},
        {"莱法岛", 331, 23, 18}, {"暮煦山", 431, 51, 15},
        {"晴云镇", 67, 21, 16}, {"无名小镇", 168, 21, 24}, {"伊未镇", 126, 26, 17}, {"雾绒镇", 189, 14, 17},
        {"雾绒码头", 191, 17, 32}, {"影辞码头", 211, 21, 17}, {"彼方镇", 214, 23, 19}, {"影辞镇", 204, 25, 20},
        {"叹月镇", 282, 23, 18}, {"清风镇", 301, 28, 13}, {"风影镇", 305, 32, 11}, {"夕墨镇", 303, 18, 32}, {"缘眠镇", 317, 50, 21},
        {"古荷镇", 310, 25, 38}, {"泷岐落花田", 313, 29, 31},
        {"天空城祭坛", 201, 68, 19}, {"幻谕岛", 142, 47, 45}, {"灵诺森岛", 247, 24, 13}, {"厄季斯岛", 242, 35, 14},
        {"绯焰岛", 243, 14, 13}, {"茉克岛", 241, 26, 13}, {"虹兰岛", 244, 41, 16}, {"规盈岛", 269, 15, 12},
        {"落英岛", 264, 13, 44}, {"狱怜岛", 250, 34, 32}, {"翼霄岛", 411, 26, 28}, {"磷火岛", 409, 33, 19}, {"长夜屿", 408, 28, 31},
    };

    /** {@code SPECIAL_WARP_DATA} (:63-76): display name, map, x, y, the switch that opens it. */
    private static final Object[][] SPECIAL = {
        {"伊甸园", 347, 24, 61, 231}, {"清澈湖", 349, 43, 56, 233}, {"裂界石窟", 371, 15, 17, 81}, {"心魂之地", 233, 34, 6, 234},
        {"红枫雪域", 224, 59, 57, 230}, {"尘封山", 140, 18, 34, 236}, {"平衡之森", 376, 30, 27, 237}, {"初心镇", 449, 40, 8, 235},
        {"康特联盟", 499, 11, 10, 241}, {"白龙空间", 478, 25, 29, 238}, {"零区研究所", 468, 15, 20, 239}, {"蛮荒之地", 487, 32, 28, 240},
    };

    private static final Color TITLE_BAR = rgb(20, 20, 60);
    private static final Color SLOT = rgb(40, 40, 80), SLOT_INNER = rgb(20, 20, 50);
    private static final Color SLOT_ON = rgb(60, 140, 220), SLOT_INNER_ON = rgb(30, 70, 120);
    private static final Color CANCEL = rgb(80, 30, 30), CANCEL_INNER = rgb(50, 15, 15);
    private static final Color CANCEL_ON = rgb(220, 60, 60), CANCEL_INNER_ON = rgb(140, 30, 30);

    private final RuntimeContext context;
    private final List<Object[]> data = new ArrayList<>();
    private final int totalPages;
    private final boolean[] visible = new boolean[PER_PAGE];
    private int page, col, row;
    private boolean finished;
    private int[] result;

    public DimensionalWarpView(RuntimeContext context) {
        this.context = context;
        for (Object[] entry : BASE) data.add(entry);
        for (Object[] special : SPECIAL) {                                       // :78-93 build_warp_data
            if (context.gameState().switches().get((Integer) special[4])) data.add(special);
        }
        totalPages = (data.size() + PER_PAGE - 1) / PER_PAGE;                    // :99
        drawPage();
    }

    private static Color rgb(int r, int g, int b) {
        return new Color(r / 255f, g / 255f, b / 255f, 1f);
    }

    /** {@code draw_page}: which of the nine cells show (the places of the page, the cancel cell after the last one). */
    private void drawPage() {
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            visible[i] = start + i <= data.size();                              // :173-210
        }
    }

    private boolean validSlot(int c, int r) {
        return visible[r * COLS + c];                                            // :268-272
    }

    /** @return true when the screen is over (a place picked or cancelled). */
    public boolean update(InputManager input) {
        if (finished) {
            return true;
        }
        int audioCursor = 0;
        if (input.wasPressed(GameAction.LEFT)) {                                 // :283-305
            UiSounds.cursor(context.audioManager());
            col -= 1;
            if (col < 0) {
                if (page > 0) {
                    page -= 1;
                    col = COLS - 1;
                    row = Math.min(row, ROWS - 1);
                    for (int c = col; c >= 0; c--) {
                        if (validSlot(c, row)) {
                            col = c;
                            break;
                        }
                    }
                    drawPage();
                } else {
                    col = 0;
                }
            }
        } else if (input.wasPressed(GameAction.RIGHT)) {                         // :307-325
            UiSounds.cursor(context.audioManager());
            col += 1;
            if (col >= COLS || !validSlot(col, row)) {
                if (page < totalPages - 1) {
                    page += 1;
                    col = 0;
                    row = Math.min(row, ROWS - 1);
                    drawPage();
                } else {
                    col -= 1;
                }
            }
        } else if (input.wasPressed(GameAction.UP)) {                            // :327-346
            UiSounds.cursor(context.audioManager());
            row -= 1;
            if (row < 0) {
                if (page > 0) {
                    page -= 1;
                    row = ROWS - 1;
                    drawPage();
                } else {
                    row = 0;
                }
            }
            if (!validSlot(col, row)) {
                row += 1;
            }
        } else if (input.wasPressed(GameAction.DOWN)) {                          // :348-366
            UiSounds.cursor(context.audioManager());
            row += 1;
            if (row >= ROWS || !validSlot(col, row)) {
                if (page < totalPages - 1) {
                    page += 1;
                    row = 0;
                    drawPage();
                } else {
                    row -= 1;
                }
            }
        } else if (input.wasPressed(GameAction.CONFIRM)) {                       // :368-378
            UiSounds.decision(context.audioManager());
            int index = page * PER_PAGE + row * COLS + col;
            if (index < data.size()) {
                Object[] place = data.get(index);
                result = new int[] {(Integer) place[1], (Integer) place[2], (Integer) place[3]};
                finished = true;
            } else if (index == data.size()) {
                finished = true;
            }
        } else if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {   // :380-383
            UiSounds.cancel(context.audioManager());
            finished = true;
        }
        return finished;
    }

    /** {@code [map, x, y]} of the picked place, or null when the player cancelled. */
    public int[] result() {
        return result;
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f) {
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        MenuPanel.fill(b, a, 0f, 0f, w, h, 0f, 0f, 0f, 200f / 255f);                         // :111-114 the half-dark cover
        fill(b, a, 0f, 0f, w, 50f, TITLE_BAR, h);                                            // :117-120 the title bar
        text(b, f, "次元传送装置", w / 2f, 6f, rgb(200, 220, 255), true, h);                  // :124-125
        text(b, f, "← → 翻页", 10f, 6f, rgb(160, 200, 255), false, h);                       // :128-129
        text(b, f, "C确认 Esc取消", w - 85f, 6f, rgb(160, 200, 255), true, h);                // :132-133
        int slotW = (int) (w / COLS), slotH = (int) ((h - 96f) / ROWS);
        for (int i = 0; i < PER_PAGE; i++) {                                                  // :150-170 / :173-210
            if (!visible[i]) {
                continue;
            }
            int index = page * PER_PAGE + i;
            boolean on = i == row * COLS + col;
            float x = (i % COLS) * slotW + 8f, y = (i / COLS) * slotH + 44f;
            float cw = slotW - 16f, ch = slotH - 16f;
            boolean cancel = index == data.size();
            fill(b, a, x, y, cw, ch, cancel ? (on ? CANCEL_ON : CANCEL) : (on ? SLOT_ON : SLOT), h);
            fill(b, a, x + 2f, y + 2f, cw - 4f, ch - 4f, cancel ? (on ? CANCEL_INNER_ON : CANCEL_INNER) : (on ? SLOT_INNER_ON : SLOT_INNER), h);
            String label = cancel ? "×取消" : (String) data.get(index)[0];
            text(b, f, label, x + 8f + (cw - 16f) / 2f, y + 8f + (ch - 16f) / 2f - 10f,
                    cancel ? rgb(255, 180, 180) : rgb(255, 255, 255), true, h);
        }
        float barY = h - 48f;
        fill(b, a, 0f, barY, w, 48f, TITLE_BAR, h);                                           // :140-146 the bottom bar
        text(b, f, "第 " + (page + 1) + "/" + totalPages + " 页", w / 2f, barY + 8f + 6f, rgb(200, 200, 255), true, h);   // :148-150, :212-216
        b.setColor(Color.WHITE);
    }

    private static void fill(SpriteBatch b, MenuAssets a, float x, float y, float w, float h, Color c, float screenH) {
        MenuPanel.fill(b, a, x, screenH - y - h, w, h, c.r, c.g, c.b, 1f);
    }

    /** {@code pbDrawTextPositions} with no shadow ({@code nil}): plain coloured text, optionally centred on x. */
    private static void text(SpriteBatch b, MenuFont f, String text, float x, float top, Color color, boolean centered, float screenH) {
        if (f.font() == null) {
            return;
        }
        float left = centered ? x - f.width(text) / 2f : x;
        f.font().setColor(color);
        f.font().draw(b, text, left, screenH - top);
        f.font().setColor(Color.WHITE);
    }
}
