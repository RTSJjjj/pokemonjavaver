package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.ui.WindowSkin;

import java.util.Arrays;
import java.util.List;

/**
 * 355_ES_s_Fast_Catching {@code FastCatch_Scene}: the Poke Ball picker the battle command menu opens on Input::A
 * (156_Scene_Commands:64-76). The balls the player owns sit in a row, the chosen one under an arrow; LEFT/RIGHT move by
 * one, L/R by five, C throws, A/B leave. 登记: the description wraps by the font's own width and is drawn in at most
 * three lines (the plugin's {@code numlines = 2} box clips the third).
 */
public final class FastCatchView {

    public enum Result { OPEN, CANCEL, THROW }

    /** 355:46 {@code @oys}: the arrow's bobbing offsets, one per 40fps frame. */
    private static final int[] OYS = {0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 3, 3, 3, 2, 2, 2, 1, 1, 1};
    private static final Color BASE = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color SHADOW = new Color(64f / 255f, 64f / 255f, 64f / 255f, 1f);

    private final RuntimeContext context;
    private final List<String> balls;
    private final boolean female;
    private final MenuClock clock = new MenuClock();
    private final PbMessage confirm;
    private int index;
    private int bgScroll;
    private int bob;
    private boolean masterBallAsked;
    private Result result = Result.OPEN;
    private boolean indexChanged;

    /**
     * @param balls      the ball items the bag holds, in {@code $BallTypes} order (355:150-153)
     * @param startIndex {@code @battle.esfc_ball_index}: where the last quick catch of this battle left off
     */
    public FastCatchView(RuntimeContext context, List<String> balls, int startIndex, boolean female) {
        this.context = context;
        this.balls = balls;
        this.female = female;
        this.index = Math.max(0, Math.min(startIndex, balls.size() - 1));      // 355:8 [esfc_ball_index, length-1].min
        this.confirm = new PbMessage(context);
    }

    public int index() {
        return index;
    }

    public String ball() {
        return balls.get(index);
    }

    public Result result() {
        return result;
    }

    /** One frame of {@code pbStartScreen}'s loop (355:49-96). */
    public Result update(InputManager input) {
        int ticks = clock.advance();
        if (confirm.active()) {
            confirm.update(input, ticks);
            return result;
        }
        for (int i = 0; i < ticks; i++) {
            bgScroll++;                                                        // :54-55 @sprites["bg"].ox += 1 ... 0 if > 32
            if (bgScroll > 32) bgScroll = 0;
            bob = (bob + 1) % OYS.length;                                      // :56-58
        }
        int before = index;
        if (input.wasPressed(GameAction.LEFT) || input.wasRepeated(GameAction.LEFT)) {
            if (balls.size() > 1) {                                            // :60-63
                index = index - 1 < 0 ? balls.size() - 1 : index - 1;
            }
        } else if (input.wasPressed(GameAction.RIGHT) || input.wasRepeated(GameAction.RIGHT)) {
            if (balls.size() > 1) {                                            // :64-67
                index = index + 1 >= balls.size() ? 0 : index + 1;
            }
        } else if (input.wasPressed(GameAction.SHOULDER_LEFT)) {
            if (balls.size() >= 5) {                                           // :68-71 L: 5 back
                index = Math.max(0, index - 5);
            }
        } else if (input.wasPressed(GameAction.SHOULDER_RIGHT)) {
            if (balls.size() >= 5) {                                           // :72-75 R: 5 forward
                index = index + 5 >= balls.size() ? balls.size() - 1 : index + 5;
            }
        } else if (input.wasPressed(GameAction.CONFIRM)) {                     // :76
            if ("MASTERBALL".equals(balls.get(index)) && !masterBallAsked) {   // :77-80 pbConfirmMessageSerious
                masterBallAsked = true;
                confirm.start("确定要扔出大师球吗？", Arrays.asList("否", "是"), 1, 0, pick -> {
                    masterBallAsked = false;
                    if (pick == 1) {
                        result = Result.THROW;
                    }
                });
            } else {
                result = Result.THROW;                                         // :81-87
            }
        } else if (input.wasPressed(GameAction.SPECIAL) || input.wasPressed(GameAction.CANCEL)
                || input.wasPressed(GameAction.MENU)) {
            result = Result.CANCEL;                                            // :88-89 Input::A or Input::B
        }
        indexChanged = index != before;
        if (indexChanged && context.audioManager() != null) {                  // :91-95
            context.audioManager().playSe("GUI naming tab swap start", 80, 100);
        }
        return result;
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, MenuFont smallFont) {
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        float halfWidth = w / 2f, halfHeight = h / 2f;
        float viewportTop = 48f, viewportHeight = h - 192f;                    // :4 Viewport.new(0, 48, width, height-192)
        Texture bg = a.graphic("Pictures/Battle", female ? "bg_moveinfo_f" : "bg_moveinfo_m");
        if (bg != null) {
            b.setColor(Color.WHITE);
            b.draw(bg, -bgScroll, h - viewportTop - bg.getHeight());          // :11-17 the background scrolls left
        }
        Texture arrow = a.graphic("Pictures/Battle", "Arrow");
        float y = halfHeight / 2f;
        if (arrow != null) {                                                   // :22-26 at (half_width-10, half_height/2), bobbing up
            b.draw(arrow, halfWidth - 10f, h - (y - OYS[bob]) - 12f, 0, 0, 20, 12, 1f, 1f, 0f, 0, 0, 20, 12, false, false);
        }
        // Everything below is clipped to the viewport (the ball sprites) or the overlay width.
        float overlayTop = 48f;
        smallFont.drawCentered(b, "要扔出哪一个精灵球？[" + (index + 1) + "/" + balls.size() + "]",
                halfWidth, h - (overlayTop + y - 86f), BASE, SHADOW);          // :132-133
        for (int i = 0; i < balls.size(); i++) {
            float x = halfWidth + 100f * (i - index);                          // :140
            Texture icon = ItemIcons.of(a, context.pbsData(), balls.get(i));
            float iconWidth = icon == null ? 0f : (icon.getHeight() == 48 ? 48f : icon.getWidth());
            float iconHeight = icon == null ? 0f : icon.getHeight();
            if (icon != null && x + iconWidth / 2f > 0f && x - iconWidth / 2f < w) {
                float cy = viewportTop + (y - 12f);                            // :141 ItemIconSprite.new(x, y-12, ball, @viewport)
                b.draw(icon, x - iconWidth / 2f, h - (cy + iconHeight / 2f), 0, 0, iconWidth, iconHeight,
                        1f, 1f, 0f, 0, 0, (int) iconWidth, (int) iconHeight, false, false);
            }
            PbsData.Item item = context.pbsData() == null ? null : context.pbsData().item(balls.get(i));
            String name = (item == null ? balls.get(i) : item.name) + "×" + context.gameState().inventory().count(balls.get(i));
            if (x > -100f && x < w + 100f) {
                smallFont.drawCentered(b, name, x, h - (overlayTop + y + 12f), BASE, SHADOW);   // :145-147
            }
        }
        drawDescription(b, smallFont, w, h, overlayTop + y + 36f);
        if (confirm.active()) {
            confirm.render(b, a, f, skin, skin, w, h);
        }
        b.setColor(Color.WHITE);
    }

    /** 355:152-175: the item description without spaces, broken after the characters that fit {@code width - 136}. */
    private void drawDescription(SpriteBatch b, MenuFont smallFont, float w, float h, float top) {
        PbsData.Item item = context.pbsData() == null ? null : context.pbsData().item(balls.get(index));
        String desc = item == null || item.description == null ? "道具描述" : item.description.replace(" ", "");
        float charWidth = Math.max(1f, smallFont.width("字"));
        int length = desc.length();
        for (int i = 0; i < desc.length(); i++) {                               // :158-163
            if (charWidth * i >= w - 136f) {
                length = i;
                break;
            }
        }
        length = Math.max(1, length);
        for (int line = 0; line < 3 && line * length < desc.length(); line++) {
            String text = desc.substring(line * length, Math.min(desc.length(), (line + 1) * length));
            smallFont.draw(b, text, 62f, h - (top + 24f * line), BASE, SHADOW);   // :174 drawTextEx(... lineheight 24)
        }
    }
}
