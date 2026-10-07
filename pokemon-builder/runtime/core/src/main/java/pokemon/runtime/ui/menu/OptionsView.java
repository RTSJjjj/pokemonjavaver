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
 * PScreen_Options: the project's options screen. The 设置 title window at
 * (0,0,672,64), {@code Window_PokemonOption} at (0,64,672,288) with the option
 * rows (name in the left 9/20, values on the right) plus the trailing 退出 row,
 * and the bottom message window showing "对话框样式 N.".
 */
public final class OptionsView {

    public enum Result {
        NONE,
        BACK
    }

    private static final Color NAME_BASE = new Color(192f / 255f, 120f / 255f, 0f, 1f);
    private static final Color NAME_SHADOW = new Color(248f / 255f, 176f / 255f, 80f / 255f, 1f);
    private static final Color SEL_BASE = new Color(248f / 255f, 48f / 255f, 24f / 255f, 1f);
    private static final Color SEL_SHADOW = new Color(248f / 255f, 136f / 255f, 128f / 255f, 1f);

    private static final float TITLE_H = 64f;
    private static final int VISIBLE = 9;

    private final RuntimeContext context;
    private int index;
    private int top;

    public OptionsView(RuntimeContext context) {
        this.context = context;
    }

    public Result update(InputManager input, AudioManager audio) {
        GameSettings.Option[] options = GameSettings.Option.values();
        int rows = options.length;
        if (input.wasPressed(GameAction.UP)) {
            index = Math.floorMod(index - 1, rows);
            MenuSe.cursor(audio);
        }
        if (input.wasPressed(GameAction.DOWN)) {
            index = Math.floorMod(index + 1, rows);
            MenuSe.cursor(audio);
        }
        int delta = 0;
        if (input.wasPressed(GameAction.LEFT)) delta = -1;
        else if (input.wasPressed(GameAction.RIGHT)) delta = 1;
        if (delta != 0 && index < rows) {
            GameSettings.Option option = options[index];
            if (option.kind != GameSettings.Option.Kind.EXIT) {
                int current = context.settings().value(option);
                context.settings().setValue(option, context.settings().step(option, current, delta));
                if (option == GameSettings.Option.SCREEN_SIZE) {
                    applyScreenSize();
                }
                context.settingsChanged();
                MenuSe.cursor(audio);
            }
        }
        keepVisible();
        if (input.wasPressed(GameAction.CONFIRM) && options[index].kind == GameSettings.Option.Kind.EXIT) {
            MenuSe.decision(audio);
            return Result.BACK;
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            MenuSe.buzzer(audio);
            return Result.BACK;
        }
        return Result.NONE;
    }

    private void keepVisible() {
        if (index < top) {
            top = index;
        } else if (index >= top + VISIBLE) {
            top = index - VISIBLE + 1;
        }
        top = Math.max(0, Math.min(top, Math.max(0, GameSettings.Option.values().length - VISIBLE)));
    }

    /** 荧幕大小 小/中/大 -> the window scale; 全屏 -> fullscreen. */
    private void applyScreenSize() {
        int size = context.settings().screensize;
        if (com.badlogic.gdx.Gdx.graphics == null) {
            return;
        }
        try {
            if (size >= 3) {
                com.badlogic.gdx.Gdx.graphics.setFullscreenMode(
                        com.badlogic.gdx.Gdx.graphics.getDisplayMode());
            } else {
                int scale = size + 1;
                com.badlogic.gdx.Gdx.graphics.setWindowedMode(
                        Math.max(320, ScreenMetrics.logicalWidth() * scale),
                        Math.max(240, ScreenMetrics.logicalHeight() * scale));
            }
        } catch (RuntimeException error) {
            com.badlogic.gdx.Gdx.app.log("OptionsView", "cannot resize the window: " + error.getMessage());
        }
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        // title: Window_UnformattedTextPokemon.newWithSize("设置",0,0,width,64).
        MenuPanel.window(b, a, skin, 0f, h - TITLE_H, w, TITLE_H);
        Color[] titleText = MenuPanel.textColors(skin);
        f.drawCentered(b, "设置", w / 2f, h - 40f, titleText[0], titleText[1]);
        // option: Window_PokemonOption at (0, 64) 672x288.
        float optionTop = TITLE_H;
        float optionH = 288f;
        MenuPanel.window(b, a, skin, 0f, h - optionTop - optionH, w, optionH);
        GameSettings.Option[] options = GameSettings.Option.values();
        Color[] text = MenuPanel.textColors(skin);
        float optionWidth = (w - 32f) * 9f / 20f;
        for (int i = top; i < options.length && i < top + VISIBLE; i++) {
            float ty = MenuPanel.rowY(h - optionTop - optionH, optionH, i - top, 32f, f.lineHeight());
            MenuPanel.cursor(b, a, skin, 0f, h - optionTop - optionH, optionH, i - top, index - top, 32f);
            f.draw(b, options[i].label, 32f, ty, NAME_BASE, NAME_SHADOW);
            if (options[i].kind == GameSettings.Option.Kind.EXIT) {
                continue;
            }
            drawValue(b, a, f, options[i], 32f + optionWidth, ty, optionWidth, text);
        }
        // textbox: pbCreateMessageWindow -> "对话框样式 N." (speech skin).
        float msgH = 32f + 64f;
        MenuPanel.window(b, a, speech, 0f, 0f, w, msgH);
        Color[] msgText = MenuPanel.textColors(speech);
        f.draw(b, "对话框样式 " + (1 + context.settings().textskin) + ".", 32f,
                msgH - f.lineHeight() + 4f, msgText[0], msgText[1]);
    }

    /** Window_PokemonOption#drawItem: the value column. */
    private void drawValue(SpriteBatch b, MenuAssets a, MenuFont f, GameSettings.Option option,
                           float x, float y, float width, Color[] text) {
        switch (option.kind) {
            case ENUM: {
                String[] values = option.enumValues();
                if (values.length <= 1) {
                    f.draw(b, option.label, x, y, text[0], text[1]);
                    return;
                }
                float total = 0f;
                for (String value : values) {
                    total += f.width(value);
                }
                float spacing = Math.max(0f, (width - total) / (values.length - 1));
                float xpos = x;
                int selected = context.settings().value(option);
                for (int i = 0; i < values.length; i++) {
                    Color main = i == selected ? SEL_BASE : text[0];
                    Color shadow = i == selected ? SEL_SHADOW : text[1];
                    f.draw(b, values[i], xpos, y, main, shadow);
                    xpos += f.width(values[i]) + spacing;
                }
                break;
            }
            case NUMBER:
                f.draw(b, context.settings().display(option), x, y, SEL_BASE, SEL_SHADOW);
                break;
            case SLIDER: {
                int value = context.settings().value(option);
                String valueText = String.valueOf(value);
                float sliderLength = width - f.width(" " + option.end);
                b.setColor(text[0]);
                b.draw(a.pixel(), x, y + 8f, Math.max(0f, sliderLength), 4f);
                float knob = (sliderLength - 8f) * value / Math.max(1, option.end);
                b.setColor(SEL_BASE);
                b.draw(a.pixel(), x + knob, y, 8f, 16f);
                b.setColor(Color.WHITE);
                f.draw(b, valueText, x + width - f.width(valueText), y, SEL_BASE, SEL_SHADOW);
                break;
            }
            default:
                break;
        }
    }
}
