package pokemon.runtime.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.data.PinyinTable;
import pokemon.runtime.event.MenuService;
import pokemon.runtime.input.*;
import pokemon.runtime.ui.WindowSkin;
import java.util.*;

/**
 * P4: a port of the project's {@code BWGenderSelector} (Scripts "BWGenderSelector").
 * The original flow is: show {@code Pictures/introbg} + the {@code genderselect}
 * bars + the {@code introBoy}/{@code introGirl} sprites, pick boy/girl with
 * left/right, confirm ("是男生吗？"), {@code pbChangePlayer(0/1)} + variable 52,
 * then the trainer-name entry ({@code pbTrainerName} -> the 字库 pinyin table),
 * confirm the name ("你的名字是{1}？") and finish.
 *
 * <p>Rendering uses the pause menu's logical screen camera (672x448, bottom-left
 * origin); the RMXP script's top-left y is converted with {@code y_gdx = H - y_rmxp}.</p>
 */
public final class GenderSelectorView {

    private enum Phase { SHOW, CHOOSE, CONFIRM, NAME, NAME_CHECK, HIDE }

    private static final float FADE = 0.25f;

    private final RuntimeContext context;
    private final MenuService.Request request;

    private Phase phase = Phase.SHOW;
    private float phaseTime;
    private int selection;            // 0 boy, 1 girl (-1 none yet)
    private int chosen;
    private NameEntryModel name;
    private String notice = "";

    private Texture bg;
    private Texture bar;
    private Texture boySprite;
    private Texture girlSprite;
    private boolean loaded;

    private float boyX = 100f;
    private float girlX;
    private float boyScale = 1f;
    private float girlScale = 1f;
    private float boyTone = 1f;
    private float girlTone = 1f;

    public GenderSelectorView(RuntimeContext context, MenuService.Request request) {
        this.context = context;
        this.request = request;
        girlX = ScreenMetrics.logicalWidth() - 130f;
    }

    public boolean update(InputManager input) {
        if (request.done) {
            return true;
        }
        float delta = Gdx.graphics == null ? 0f : Math.min(0.1f, Gdx.graphics.getDeltaTime());
        phaseTime += delta;
        updateAnimation(delta);
        switch (phase) {
            case SHOW:
                if (phaseTime >= FADE) {
                    next(Phase.CHOOSE);
                    notice = "你是男生还是女生？";
                }
                return false;
            case CHOOSE:
                if (input.wasPressed(GameAction.LEFT) && selection != 0) {
                    selection = 0;
                } else if (input.wasPressed(GameAction.RIGHT) && selection != 1) {
                    selection = 1;
                }
                if (selection < 0 && (input.wasPressed(GameAction.CONFIRM)
                        || input.wasPressed(GameAction.LEFT) || input.wasPressed(GameAction.RIGHT))) {
                    selection = 0;
                }
                if (input.wasPressed(GameAction.CONFIRM)) {
                    next(Phase.CONFIRM);
                }
                return false;
            case CONFIRM:
                if (input.wasPressed(GameAction.CONFIRM)) {
                    applyPlayer();
                    next(Phase.NAME);
                    name = new NameEntryModel(pinyin(), 12, context.gameState().playerName());
                    notice = "还不知道你的名字呢，请把你的名字告诉我。";
                } else if (input.wasPressed(GameAction.CANCEL)) {
                    next(Phase.CHOOSE);
                    notice = "你是男生还是女生？";
                }
                return false;
            case NAME:
                updateName(input);
                if (input.wasPressed(GameAction.CONFIRM)) {
                    next(Phase.NAME_CHECK);
                } else if (input.wasPressed(GameAction.CANCEL)) {
                    if (!name.clearBuffer()) {
                        finish();
                        return true;
                    }
                }
                return false;
            case NAME_CHECK:
                if (input.wasPressed(GameAction.CONFIRM)) {
                    finish();
                    return true;
                }
                if (input.wasPressed(GameAction.CANCEL)) {
                    next(Phase.NAME);
                    notice = "请重新输入名字。";
                }
                return false;
            case HIDE:
            default:
                return phaseTime >= FADE;
        }
    }

    private void updateName(InputManager input) {
        if (name == null || Gdx.input == null) {
            return;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.A + (c - 'a'))) {
                name.type(c);
            }
        }
        for (int number = 0; number <= 9; number++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + number)) {
                name.pick(number);
            }
        }
        if (input.wasPressed(GameAction.LEFT)) {
            name.prevPage();
        }
        if (input.wasPressed(GameAction.RIGHT)) {
            name.nextPage();
        }
        if (input.wasPressed(GameAction.UP)) {
            name.backspace();
        }
    }

    /** The script's pbChangePlayer(id) + $game_variables[52] = id. */
    private void applyPlayer() {
        chosen = selection < 0 ? 0 : selection;
        context.gameState().playerId(chosen);
        context.gameState().trainer().gender = chosen;
        context.gameState().variables().set(52, chosen);
    }

    private void finish() {
        String result = name == null || name.text().isEmpty()
                ? context.gameState().playerName() : name.text();
        context.gameState().playerName(result);
        context.gameState().trainer().name = result;
        request.complete(chosen, result);
    }

    private void next(Phase phase) {
        this.phase = phase;
        phaseTime = 0f;
    }

    private void updateAnimation(float delta) {
        float speed = Math.min(1f, delta * 12f);
        float boyTarget = (phase == Phase.CONFIRM && selection == 0)
                ? ScreenMetrics.logicalWidth() / 2f : 100f;
        float girlTarget = (phase == Phase.CONFIRM && selection == 1)
                ? ScreenMetrics.logicalWidth() / 2f
                : ScreenMetrics.logicalWidth() - 130f;
        boyX += (boyTarget - boyX) * speed;
        girlX += (girlTarget - girlX) * speed;
        float selectedScale = phase == Phase.CHOOSE || phase == Phase.CONFIRM ? 1f : 1f;
        boyScale += ((selection == 0 ? selectedScale : selectedScale * 0.9f) - boyScale) * speed;
        girlScale += ((selection == 1 ? selectedScale : selectedScale * 0.9f) - girlScale) * speed;
        boyTone += ((selection == 0 ? 1f : 0.6f) - boyTone) * speed;
        girlTone += ((selection == 1 ? 1f : 0.6f) - girlTone) * speed;
    }

    private void ensureLoaded(MenuAssets assets) {
        if (loaded) {
            return;
        }
        loaded = true;
        bg = assets.graphic("Pictures", "introbg");
        bar = assets.graphic("Pictures", "genderselect");
        boySprite = assets.graphic("Pictures", "introBoy");
        girlSprite = assets.graphic("Pictures", "introGirl");
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        ensureLoaded(a);
        int width = ScreenMetrics.logicalWidth();
        int height = ScreenMetrics.logicalHeight();
        float alpha = phase == Phase.SHOW ? Math.min(1f, phaseTime / FADE)
                : phase == Phase.HIDE ? Math.max(0f, 1f - phaseTime / FADE) : 1f;

        if (bg != null) {
            b.setColor(1f, 1f, 1f, alpha);
            b.draw(bg, 0, 0, width, height);
        }
        if (bar != null && bar.getHeight() >= 2) {
            int barW = bar.getWidth();
            int barH = bar.getHeight() / 2;
            float originBoy = barW / 2f + barW / 6f;
            float originGirl = barW / 2f - barW / 6f;
            // girl bar (top half), boy bar (bottom half); they sit near the right.
            b.setColor(1f, 1f, 1f, alpha * girlTone);
            b.draw(bar, width - originGirl, height / 2f - barH / 2f, originGirl, barH / 2f,
                    barW, barH, 1f, 1f, 0f, 0, 0, barW, barH, false, false);
            b.setColor(1f, 1f, 1f, alpha * boyTone);
            b.draw(bar, width - originBoy, height / 2f - barH / 2f, originBoy, barH / 2f,
                    barW, barH, 1f, 1f, 0f, 0, barH, barW, barH, false, false);
        }
        drawTrainer(b, girlSprite, girlX, girlScale, girlTone, alpha);
        drawTrainer(b, boySprite, boyX, boyScale, boyTone, alpha);
        b.setColor(1f, 1f, 1f, 1f);

        drawMessages(b, a, f, width, height);
    }

    /** Draws the last animation frame centred on {@code (x, height/2)}. */
    private void drawTrainer(SpriteBatch b, Texture texture, float x, float scale, float tone,
                             float alpha) {
        if (texture == null || texture.getHeight() <= 0) {
            return;
        }
        int frameH = texture.getHeight();
        int frames = Math.max(1, texture.getWidth() / frameH);
        int frameW = texture.getWidth() / frames;
        b.setColor(tone, tone, tone, alpha);
        b.draw(texture, x - frameW / 2f, ScreenMetrics.logicalHeight() / 2f - frameH / 2f,
                frameW / 2f, frameH / 2f, frameW, frameH, scale, scale, 0f,
                (frames - 1) * frameW, 0, frameW, frameH, false, false);
    }

    private void drawMessages(SpriteBatch b, MenuAssets a, MenuFont f, int width, int height) {
        String line;
        switch (phase) {
            case CONFIRM:
                line = (selection == 0 ? "是男生吗？" : "是女生吗？") + "   Z 确认 / X 返回";
                break;
            case NAME:
                line = notice + "   " + (name == null ? "" : "拼音[" + name.buffer() + "] "
                        + "名字[" + name.text() + "]");
                break;
            case NAME_CHECK:
                line = "你的名字是" + (name == null ? "" : name.text()) + "？   Z 确认 / X 重输";
                break;
            default:
                line = notice;
                break;
        }
        if (line == null || line.isEmpty()) {
            return;
        }
        float boxY = height / 2f - 120f;
        MenuPanel.fill(b, a, 12, boxY, width - 24, 44, .07f, .12f, .21f, .9f);
        MenuPanel.text(b, f, line, 28, boxY + 14, width - 56);
        if (phase == Phase.NAME && name != null) {
            String candidates = name.candidates();
            int start = name.page() * NameEntryModel.PAGE_SIZE;
            for (int i = 0; i < NameEntryModel.PAGE_SIZE; i++) {
                int index = start + i;
                if (index >= candidates.length()) {
                    break;
                }
                int row = i / NameEntryModel.COLUMNS;
                int column = i % NameEntryModel.COLUMNS;
                float x = 32 + column * (width - 64) / NameEntryModel.COLUMNS;
                float y = boxY - 24 - row * 36;
                MenuPanel.text(b, f, (i % 10) + "" + candidates.charAt(index), x, y, 46);
            }
        }
    }

    private PinyinTable pinyin() {
        return context.database() == null ? null : context.database().pinyin();
    }
}
