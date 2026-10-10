package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.ui.WindowSkin;

/**
 * 336_002_ESMM_Setting {@code MiniMapOption_Scene}: the "小地图设置" page (the region map's Z key). The 小地图设置 title window, the
 * option list ({@code Window_MiniMapOption}) with the trailing 完成 row, and the text box with the explanation of the selected row.
 */
public final class MiniMapOptionsView {

    public enum Result {
        NONE,
        BACK
    }

    private enum Kind { ENUM, SLIDER }

    /** The rows in the plugin's order: name, kind, values / slider end, explanation. */
    private static final String[] NAMES = {"显示", "透明度", "位置", "尺寸", "缩放", "边框颜色"};
    private static final Kind[] KINDS = {Kind.ENUM, Kind.SLIDER, Kind.ENUM, Kind.ENUM, Kind.ENUM, Kind.ENUM};
    private static final String[][] VALUES = {
            {"是", "否"}, null, {"左上", "右上", "左下", "右下"}, {"小", "中", "大", "更大"}, {"小", "中", "大", "更大"},
            {"黑", "白", "红", "绿", "蓝", "黄"}};
    /** {@code @descs} (:171-178), in the order of {@code @indexes}. */
    private static final String[] DESCS = {
            "选择是否显示小地图，需要拥有地图道具。\n快捷键: [Ctrl] + [M]",
            "调整小地图的透明度。",
            "选择小地图显示在屏幕中的位置。",
            "选择小地图本身的显示尺寸。\n快捷键: [=+] / [-_]",
            "选择小地图内容的缩放尺寸。\n快捷键: [Ctrl] + [=+] / [Ctrl] + [-_]",
            "调整小地图的边框颜色。",
            "完成小地图设置。"};
    private static final int SLIDER_END = 100;
    private static final int SLIDER_INTERVAL = 10;

    private static final Color NAME_BASE = new Color(192f / 255f, 120f / 255f, 0f, 1f);
    private static final Color NAME_SHADOW = new Color(248f / 255f, 176f / 255f, 80f / 255f, 1f);
    private static final Color SEL_BASE = new Color(248f / 255f, 48f / 255f, 24f / 255f, 1f);
    private static final Color SEL_SHADOW = new Color(248f / 255f, 136f / 255f, 128f / 255f, 1f);
    private static final float TITLE_H = 64f;
    private static final int VISIBLE = 9;

    private final RuntimeContext context;
    private int index;

    public MiniMapOptionsView(RuntimeContext context) {
        this.context = context;
    }

    private int get(int option) {
        GameSettings s = context.settings();
        switch (option) {
            case 0: return s.showMiniMap;
            case 1: return s.miniMapOpacity;
            case 2: return s.miniMapPosition;
            case 3: return s.miniMapSize;
            case 4: return s.miniMapZoom;
            default: return s.miniMapBorder;
        }
    }

    private void set(int option, int value) {
        GameSettings s = context.settings();
        switch (option) {
            case 0: s.showMiniMap = value; break;
            case 1: s.miniMapOpacity = value; break;
            case 2: s.miniMapPosition = value; break;
            case 3: s.miniMapSize = value; break;
            case 4: s.miniMapZoom = value; break;
            default: s.miniMapBorder = value; break;
        }
    }

    public Result update(InputManager input, AudioManager audio) {
        int rows = NAMES.length + 1;
        if (input.wasPressed(GameAction.UP) || input.wasRepeated(GameAction.UP)) {
            index = Math.floorMod(index - 1, rows);
            MenuSe.cursor(audio);
        } else if (input.wasPressed(GameAction.DOWN) || input.wasRepeated(GameAction.DOWN)) {
            index = Math.floorMod(index + 1, rows);
            MenuSe.cursor(audio);
        }
        int delta = 0;
        if (input.wasPressed(GameAction.LEFT) || input.wasRepeated(GameAction.LEFT)) delta = -1;
        else if (input.wasPressed(GameAction.RIGHT) || input.wasRepeated(GameAction.RIGHT)) delta = 1;
        if (delta != 0 && index < NAMES.length) {
            int current = get(index);
            int next;
            if (KINDS[index] == Kind.SLIDER) {                                    // SliderOption#prev / #next
                next = delta < 0 ? Math.max(current - SLIDER_INTERVAL, 0) : Math.min(current + SLIDER_INTERVAL, SLIDER_END);
            } else {                                                              // EnumOption#prev / #next
                next = delta < 0 ? Math.max(current - 1, 0) : Math.min(current + 1, VALUES[index].length - 1);
            }
            if (next != current) {
                set(index, next);
                context.settingsChanged();
            }
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.close(audio);                                                  // pbEndScene: pbPlayCloseMenuSE
            return Result.BACK;
        }
        if (input.wasPressed(GameAction.CONFIRM) && index == NAMES.length) {
            MenuSe.close(audio);
            return Result.BACK;
        }
        return Result.NONE;
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        float msgH = 32f + 64f;                                                   // pbCreateMessageWindow
        MenuPanel.window(b, a, skin, 0f, h - TITLE_H, w, TITLE_H);
        Color[] titleText = MenuPanel.textColors(skin);
        f.drawCentered(b, "小地图设置", w / 2f, h - 40f, titleText[0], titleText[1]);
        float optionH = h - TITLE_H - msgH;
        MenuPanel.window(b, a, skin, 0f, h - TITLE_H - optionH, w, optionH);
        Color[] text = MenuPanel.textColors(skin);
        float optionWidth = (w - 32f) * 9f / 20f;
        int rows = NAMES.length + 1;
        for (int i = 0; i < rows && i < VISIBLE; i++) {
            float ty = MenuPanel.rowY(h - TITLE_H - optionH, optionH, i, 32f, f.lineHeight());
            MenuPanel.cursor(b, a, skin, 0f, h - TITLE_H - optionH, optionH, i, index, 32f);
            f.draw(b, i == NAMES.length ? "完成" : NAMES[i], 32f, ty, NAME_BASE, NAME_SHADOW);
            if (i < NAMES.length) drawValue(b, a, f, i, 32f + optionWidth, ty, optionWidth, text);
        }
        MenuPanel.window(b, a, speech, 0f, 0f, w, msgH);
        Color[] msgText = MenuPanel.textColors(speech);
        String[] lines = DESCS[index].split("\n");
        for (int i = 0; i < lines.length; i++) {
            f.draw(b, lines[i], 32f, msgH - f.lineHeight() + 4f - i * 32f, msgText[0], msgText[1]);
        }
    }

    private void drawValue(SpriteBatch b, MenuAssets a, MenuFont f, int option, float x, float y, float width, Color[] text) {
        if (KINDS[option] == Kind.ENUM) {
            String[] values = VALUES[option];
            float total = 0f;
            for (String value : values) total += f.width(value);
            float spacing = Math.max(0f, (width - total) / (values.length - 1));
            float xpos = x;
            int selected = get(option);
            for (int i = 0; i < values.length; i++) {
                f.draw(b, values[i], xpos, y, i == selected ? SEL_BASE : text[0], i == selected ? SEL_SHADOW : text[1]);
                xpos += f.width(values[i]) + spacing;
            }
            return;
        }
        int value = get(option);                                                  // SliderOption (0..100)
        String valueText = String.valueOf(value);
        float sliderLength = width - f.width(" " + SLIDER_END);
        b.setColor(text[0]);
        float center = y - f.lineHeight() / 2f;                 // fill_rect(xpos, rect.y-2+rect.height/2, ..., 4) / (rect.y-8+rect.height/2, 8, 16)
                b.draw(a.pixel(), x, center - 2f, Math.max(0f, sliderLength), 4f);
        float knob = (sliderLength - 8f) * value / SLIDER_END;
        b.setColor(SEL_BASE);
        b.draw(a.pixel(), x + knob, center - 8f, 8f, 16f);
        b.setColor(Color.WHITE);
        f.draw(b, valueText, x + width - f.width(valueText), y, SEL_BASE, SEL_SHADOW);
    }
}
