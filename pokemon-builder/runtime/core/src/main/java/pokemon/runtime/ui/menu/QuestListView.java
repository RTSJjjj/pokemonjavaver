package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.data.QuestTable;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.state.QuestLog;
import pokemon.runtime.ui.MessagePalette;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The quest log (283_003_Quest_UI {@code QuestList_Scene}): a list of the active / completed / failed quests with a page
 * marker in the corner, and a two-page detail view of the selected quest (what to do and where; who gave it, where and when,
 * and the reward). The list fades out and the detail fades in on C and back on B (15 frames of 17 opacity each).
 */
public final class QuestListView {

    private enum Phase { LIST, LIST_OUT, DETAIL_IN, DETAIL, DETAIL_OUT, LIST_IN }

    /** 281_001_Quest_Config:7 {@code SHOW_FAILED_QUESTS}. */
    private static final boolean SHOW_FAILED_QUESTS = true;
    private static final String[] TAB_TEXT = {"当前", "已完成", "失败"};
    private static final Color WHITE = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color BLACK = new Color(0f, 0f, 0f, 1f);
    /** {@code @base} / {@code @shadow} (283_003_Quest_UI:57-58). */
    private static final int BASE = 0x505058;
    private static final int SHADOW = 0xA0A0A8;
    private static final int PAGE_ITEMS = 10;
    private static final int FADE_TICKS = 15;
    private static final int FADE_STEP = 17;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");

    private final RuntimeContext context;
    private final QuestLog log;
    private final QuestTable table;
    private final MenuClock clock = new MenuClock();
    private final MenuListModel cursor = new MenuListModel(PAGE_ITEMS);
    private final int tabCount = SHOW_FAILED_QUESTS ? 3 : 2;
    private int current;
    private Phase phase = Phase.LIST;
    private int fadeTicks;
    private int listAlpha = 255;
    private int detailAlpha;
    private QuestLog.Entry quest;
    private int page = 1;

    public QuestListView(RuntimeContext context) {
        this.context = context;
        this.log = context.gameState().quests();
        this.table = context.database() == null ? QuestTable.empty() : context.database().quests();
        refreshList();
    }

    private List<QuestLog.Entry> quests() {
        return log.list(current == 0 ? QuestLog.Status.ACTIVE : current == 1 ? QuestLog.Status.COMPLETED : QuestLog.Status.FAILED);
    }

    private void refreshList() {
        cursor.size(quests().size());
    }

    /** @return true when the player left the log (B on the list). */
    public boolean update(InputManager input) {
        int ticks = clock.advance();
        for (int i = 0; i < ticks; i++) {
            if (tickFade()) {
                break;
            }
        }
        switch (phase) {
            case LIST:
                return updateList(input);
            case DETAIL:
                updateDetail(input);
                return false;
            default:
                return false;
        }
    }

    /** One 40 fps frame of a 15-frame fade; returns true when a phase just changed (the rest of the ticks wait for the next frame). */
    private boolean tickFade() {
        switch (phase) {
            case LIST_OUT:
                listAlpha = Math.max(0, listAlpha - FADE_STEP);              // 283:140-146 fadeContent
                if (++fadeTicks >= FADE_TICKS) { listAlpha = 0; phase = Phase.DETAIL_IN; fadeTicks = 0; return true; }
                return false;
            case DETAIL_IN:
                detailAlpha = Math.min(255, detailAlpha + FADE_STEP);        // :180-183
                if (++fadeTicks >= FADE_TICKS) { detailAlpha = 255; phase = Phase.DETAIL; fadeTicks = 0; return true; }
                return false;
            case DETAIL_OUT:
                detailAlpha = Math.max(0, detailAlpha - FADE_STEP);          // :203-206
                if (++fadeTicks >= FADE_TICKS) { detailAlpha = 0; phase = Phase.LIST_IN; fadeTicks = 0; return true; }
                return false;
            case LIST_IN:
                listAlpha = Math.min(255, listAlpha + FADE_STEP);            // :148-154 showContent
                if (++fadeTicks >= FADE_TICKS) { listAlpha = 255; phase = Phase.LIST; fadeTicks = 0; return true; }
                return false;
            default:
                return false;
        }
    }

    private boolean updateList(InputManager input) {
        pokemon.runtime.app.RuntimeContext c = context;
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.close(c.audioManager());                                    // 283:105 pbPlayCloseMenuSE
            return true;
        }
        int before = cursor.index();
        if (cursor.size() > 0) {                                               // Window_DrawableCommand#update (065:850-905)
            if (input.wasPressed(GameAction.UP)) cursor.move(-1);
            else if (input.wasRepeated(GameAction.UP) && cursor.index() >= 1) cursor.move(-1);
            else if (input.wasPressed(GameAction.DOWN)) cursor.move(1);
            else if (input.wasRepeated(GameAction.DOWN) && cursor.index() < cursor.size() - 1) cursor.move(1);
            else if (input.wasRepeated(GameAction.SHOULDER_LEFT) && cursor.index() > 0) cursor.select(Math.max(0, cursor.index() - PAGE_ITEMS));
            else if (input.wasRepeated(GameAction.SHOULDER_RIGHT) && cursor.index() < cursor.size() - 1) {
                cursor.select(Math.min(cursor.size() - 1, cursor.index() + PAGE_ITEMS));
            }
            if (cursor.index() != before) MenuSe.cursor(c.audioManager());     // pbPlayCursorSE
        }
        if (input.wasPressed(GameAction.CONFIRM)) {                            // :107-117
            if (quests().isEmpty()) {
                MenuSe.buzzer(c.audioManager());                               // pbPlayBuzzerSE
            } else {
                MenuSe.decision(c.audioManager());
                quest = quests().get(cursor.index());
                log.markRead(quest.id);                                        // :167 quest.new = false
                page = 1;
                phase = Phase.LIST_OUT;
                fadeTicks = 0;
            }
        } else if (input.wasPressed(GameAction.RIGHT)) {                       // :118-121
            MenuSe.cursor(c.audioManager());
            current = current + 1 > tabCount - 1 ? 0 : current + 1;
            cursor.select(0);
            refreshList();
        } else if (input.wasPressed(GameAction.LEFT)) {                        // :122-125
            MenuSe.cursor(c.audioManager());
            current = current - 1 < 0 ? tabCount - 1 : current - 1;
            cursor.select(0);
            refreshList();
        }
        return false;
    }

    private void updateDetail(InputManager input) {
        if (input.wasPressed(GameAction.RIGHT) && page == 1) {                 // :171-176
            MenuSe.cursor(context.audioManager());
            page = 2;
        } else if (input.wasPressed(GameAction.LEFT) && page == 2) {           // :177-182
            MenuSe.cursor(context.audioManager());
            page = 1;
        } else if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {   // :183-185
            MenuSe.close(context.audioManager());
            phase = Phase.DETAIL_OUT;
            fadeTicks = 0;
        }
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, MenuFont small) {
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        Texture tile = a.graphic("Pictures/QuestUI", "bg_1");                  // addBackgroundPlane(... "QuestUI/bg_1")
        if (tile != null) {
            b.setColor(Color.WHITE);
            for (float y = 0; y < h; y += tile.getHeight()) {
                for (float x = 0; x < w; x += tile.getWidth()) {
                    b.draw(tile, x, h - y - tile.getHeight());
                }
            }
        }
        Texture base = a.graphic("Pictures/QuestUI", "bg_2");
        if (base != null) {
            b.draw(base, 0f, h - base.getHeight());
        }
        drawList(b, a, f, w, h);
        drawDetail(b, a, f, w, h);
        b.setColor(Color.WHITE);
    }

    private void drawList(SpriteBatch b, MenuAssets a, MenuFont f, float w, float h) {
        float alpha = listAlpha / 255f;
        if (alpha <= 0f) {
            return;
        }
        Texture icon1 = a.graphic("Pictures/QuestUI", SHOW_FAILED_QUESTS ? "page_icon1a" : "page_icon1b");
        float iconX = icon1 == null ? w : w - icon1.getWidth() - 10f;
        b.setColor(1f, 1f, 1f, alpha);
        if (icon1 != null) {
            b.draw(icon1, iconX, h - 4f - icon1.getHeight());                   // :48-56 page_icon1
        }
        Texture marker = a.graphic("Pictures/QuestUI", "pageIcon");
        if (marker != null) {
            b.draw(marker, iconX + 32f * current, h - 4f - marker.getHeight());   // pageIcon
        }
        outline(b, f, TAB_TEXT[current] + "任务", 6f, 2f, h, alpha);             // overlay1
        float originX = 118f + 16f, originY = 24f + 16f;                        // Window_Quest at (54+64, 24), contents offset 16
        List<QuestLog.Entry> list = quests();
        Texture arrow = a.graphic("Pictures", "selarrow");
        Texture newIcon = a.graphic("Pictures/QuestUI", "new");
        float rectWidth = (w - 22f - 32f) - 50f;
        for (int i = cursor.first(); i < cursor.end() && i < list.size(); i++) {
            float rowTop = originY + (i - cursor.first()) * 32f;
            QuestLog.Entry entry = list.get(i);
            String name = table.name(entry.id);
            if (entry.story) {
                name = "<b>" + name + "</b>";                                   // :23
            }
            drawFormatted(b, f, "<c2=" + entry.color + ">" + name + "</c2>", originX + 18f, rowTop + 4f, 436f, 32f, h,
                    BASE, SHADOW, alpha);                                       // :25-26
            if (entry.updated && newIcon != null) {
                b.setColor(1f, 1f, 1f, alpha);
                b.draw(newIcon, originX + rectWidth - 160f, h - (rowTop + 8f) - newIcon.getHeight());   // :27-28
            }
            if (i == cursor.index() && arrow != null) {
                b.setColor(1f, 1f, 1f, alpha);
                b.draw(arrow, originX, h - rowTop - arrow.getHeight());         // drawCursor
            }
        }
        // overlay_control (:79-87)
        drawFormatted(b, f, "<c2=" + QuestLog.RED + ">方向键:</c2> 导航", 134f, 376f, 436f, 32f, h, BASE, SHADOW, alpha);
        drawFormatted(b, f, "<c2=" + QuestLog.RED + ">A/S:</c2> 翻页", 134f, 408f, 436f, 32f, h, BASE, SHADOW, alpha);
        drawFormatted(b, f, "<c2=" + QuestLog.RED + ">新消息:</c2>", 396f, 378f, 436f, 32f, h, BASE, SHADOW, alpha);
        if (newIcon != null) {
            b.setColor(1f, 1f, 1f, alpha);
            b.draw(newIcon, 474f, h - 380f - newIcon.getHeight());
        }
    }

    private void drawDetail(SpriteBatch b, MenuAssets a, MenuFont f, float w, float h) {
        float alpha = detailAlpha / 255f;
        if (alpha <= 0f || quest == null) {
            return;
        }
        Texture icon2 = a.graphic("Pictures/QuestUI", "page_icon2");
        if (icon2 != null) {
            b.setColor(1f, 1f, 1f, alpha);
            float x = w - icon2.getWidth() - 10f;
            if (page == 2) {                                                    // @sprites["page_icon2"].mirror = true
                b.draw(icon2, x, h - 4f - icon2.getHeight(), icon2.getWidth(), icon2.getHeight(), 0, 0,
                        icon2.getWidth(), icon2.getHeight(), true, false);
            } else {
                b.draw(icon2, x, h - 4f - icon2.getHeight());
            }
        }
        outline(b, f, table.name(quest.id), 6f, 2f, h, alpha);                  // drawQuestDesc/drawOtherInfo: the quest name
        if (page == 1) {
            drawFormatted(b, f, "<c2=" + "7DC076EF" + ">任务详情:</c2> " + table.description(quest.id, quest.stage),
                    134f, 44f, 436f, 32f, h, BASE, SHADOW, alpha);              // :217-219 colorQuest("blue")
            String location = table.stageLocation(quest.id, quest.stage);
            if (location.equals("nil") || location.isEmpty()) {
                location = "???";                                              // :226-228
            }
            drawFormatted(b, f, "<c2=0E7F4F3F>任务目标:</c2> " + table.stageDescription(quest.id, quest.stage),
                    134f, 378f, 436f, 32f, h, BASE, SHADOW, alpha);             // :229-230 colorQuest("orange")
            drawFormatted(b, f, "<c2=751272B7>地点:</c2> " + location, 134f, 408f, 436f, 32f, h, BASE, SHADOW, alpha);   // purple
        } else {
            String giver = table.questGiver(quest.id);
            if (giver.equals("nil") || giver.isEmpty()) giver = "???";          // :244-246
            String reward = table.reward(quest.id);
            if (reward.equals("nil") || reward.isEmpty()) reward = "???";       // :264-267
            String time = TIME.format(Instant.ofEpochMilli(quest.time).atZone(ZoneId.systemDefault()));   // :258 %Y年%m月%d日 %H:%M
            drawFormatted(b, f, "<c2=6F697395>交代任务的人：</c2>", 134f, 78f, 436f, 32f, h, BASE, SHADOW, alpha);   // cyan
            drawFormatted(b, f, "<c2=5CFA729D>接收任务的地点：</c2>", 134f, 108f, 436f, 32f, h, BASE, SHADOW, alpha); // magenta
            drawFormatted(b, f, "<c2=26CC4B56>任务接收时间：</c2>", 134f, 138f, 436f, 32f, h, BASE, SHADOW, alpha);   // green
            drawFormatted(b, f, "<c2=" + QuestLog.RED + ">报酬：</c2> " + reward, 134f, h - 60f, 436f, 32f, h, BASE, SHADOW, alpha);
            text(b, f, "任务进行程度：" + quest.stage + "/" + table.maxStages(quest.id), 134f, 50f, h, BASE, SHADOW, alpha);   // :270-274
            text(b, f, giver, 306f, 80f, h, BASE, SHADOW, alpha);
            text(b, f, quest.location == null ? "" : quest.location, 316f, 114f, h, BASE, SHADOW, alpha);
            text(b, f, time, 306f, 142f, h, BASE, SHADOW, alpha);
        }
    }

    // ------------------------------------------------------------------
    // Text helpers: pbDrawTextPositions (with the outline flag) and drawFormattedTextEx
    // ------------------------------------------------------------------

    private static Color color(int rgb, float alpha) {
        return new Color(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, alpha);
    }

    private static void text(SpriteBatch b, MenuFont f, String text, float x, float top, float h, int base, int shadow, float alpha) {
        f.draw(b, text, x, h - top, color(base, alpha), color(shadow, alpha));
    }

    /** The 7th textpos element {@code true}: white text with a black outline. */
    private static void outline(SpriteBatch b, MenuFont f, String text, float x, float top, float h, float alpha) {
        if (f.font() == null) {
            return;
        }
        Color dark = new Color(BLACK.r, BLACK.g, BLACK.b, alpha);
        f.font().setColor(dark);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) f.font().draw(b, text, x + dx, h - top + dy);
            }
        }
        f.font().setColor(new Color(WHITE.r, WHITE.g, WHITE.b, alpha));
        f.font().draw(b, text, x, h - top);
        f.font().setColor(Color.WHITE);
    }

    /** One styled run of {@link #drawFormatted}. */
    private static final class Run {
        final String text;
        final int base, shadow;
        final boolean bold;

        Run(String text, int base, int shadow, boolean bold) {
            this.text = text;
            this.base = base;
            this.shadow = shadow;
            this.bold = bold;
        }
    }

    /**
     * {@code drawFormattedTextEx(bitmap, x, y, width, text, base, shadow, lineheight)}: {@code <c2=RRGGBBSS>} colours, {@code <b>}
     * bold and newlines, wrapped to {@code width}. 登记: the plugin's bold is drawn as a second pass one pixel to the right.
     */
    private void drawFormatted(SpriteBatch b, MenuFont f, String text, float x, float top, float width, float lineHeight,
                               float h, int base, int shadow, float alpha) {
        List<List<Run>> lines = layout(f, text, width, base, shadow);
        for (int line = 0; line < lines.size(); line++) {
            float cursorX = x;
            float y = top + line * lineHeight + 8f;      // the line box is 32 high and the text sits in its middle
            for (Run run : lines.get(line)) {
                f.draw(b, run.text, cursorX, h - y, color(run.base, alpha), color(run.shadow, alpha));
                if (run.bold) {
                    f.draw(b, run.text, cursorX + 1f, h - y, color(run.base, alpha), color(run.shadow, alpha));
                }
                cursorX += f.width(run.text);
            }
        }
    }

    private static List<List<Run>> layout(MenuFont f, String text, float width, int base, int shadow) {
        List<List<Run>> lines = new ArrayList<>();
        List<Run> line = new ArrayList<>();
        float used = 0f;
        int curBase = base, curShadow = shadow;
        boolean bold = false;
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '<') {
                int close = text.indexOf('>', i);
                if (close > i) {
                    String tag = text.substring(i + 1, close).toLowerCase(java.util.Locale.ROOT);
                    if (tag.startsWith("c2=") || tag.equals("/c2") || tag.equals("b") || tag.equals("/b")) {
                        if (chunk.length() > 0) {
                            line.add(new Run(chunk.toString(), curBase, curShadow, bold));
                            chunk.setLength(0);
                        }
                        if (tag.startsWith("c2=")) {
                            int[] pair = MessagePalette.parseRgb16Pair(text.substring(i + 4, close), curBase, curShadow);
                            curBase = pair[0];
                            curShadow = pair[1];
                        } else if (tag.equals("/c2")) {
                            curBase = base;
                            curShadow = shadow;
                        } else {
                            bold = tag.equals("b");
                        }
                        i = close;
                        continue;
                    }
                }
            }
            if (ch == '\n') {
                if (chunk.length() > 0) {
                    line.add(new Run(chunk.toString(), curBase, curShadow, bold));
                    chunk.setLength(0);
                }
                lines.add(line);
                line = new ArrayList<>();
                used = 0f;
                continue;
            }
            float charWidth = f.width(String.valueOf(ch));
            if (used + charWidth > width && used > 0f) {                        // wrap
                if (chunk.length() > 0) {
                    line.add(new Run(chunk.toString(), curBase, curShadow, bold));
                    chunk.setLength(0);
                }
                lines.add(line);
                line = new ArrayList<>();
                used = 0f;
            }
            chunk.append(ch);
            used += charWidth;
        }
        if (chunk.length() > 0) {
            line.add(new Run(chunk.toString(), curBase, curShadow, bold));
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }
}
