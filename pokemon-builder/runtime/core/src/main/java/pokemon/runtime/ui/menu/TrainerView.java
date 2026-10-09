package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.state.GameState;
import pokemon.runtime.ui.WindowSkin;

/**
 * B2W2 Trainer Card: the front (name / ID / money / pokedex / play time / start
 * time, the gender card and trainer sprite, the scrolling card background) and
 * the badge case ([C] toggles), transcribed from
 * {@code PokemonTrainerCard_Scene#pbDrawTrainerCardFront / Badges}.
 *
 * <p>Coordinates are the plugin's top-origin values, converted with
 * {@code screenHeight - y}. Text alignment follows {@code pbDrawTextPositions}:
 * 1 = right, 2 = centre, otherwise left.</p>
 */
public final class TrainerView {

    public enum Result {
        NONE,
        BACK
    }

    private static final Color BASE = new Color(1f, 1f, 1f, 1f);
    private static final Color SHADOW = new Color(181f / 255f, 189f / 255f, 206f / 255f, 1f);

    // PbDrawTrainerCardFront textpos (80+64 etc.).
    private static final float LABEL_X = 80 + 64;
    private static final float VALUE_X = 335 + 64;
    private static final float TIME_X = 496 + 64;

    private final GameState state;
    private final TrainerState trainer;
    private final GameDatabase database;

    private int phase;          // 0 idle, 1 moveUp, 2 effectBadges, 3 effectFront, 4 moveDown
    private int step;
    private float blackY = ScreenMetrics.logicalHeight();
    private int blackOpacity = 255;
    private int scene;         // 0 = front, 1 = badges
    private float bgX;
    private float bgY;

    private final boolean badgesOnly;

    public TrainerView(GameDatabase database, GameState state) {
        this(database, state, false);
    }

    /**
     * @param badgesOnly 300_B2W2_Trainer_Card:1084-1096 {@code pbStartBadgeScreen}: opens on the badge page, and C / B closes.
     */
    public TrainerView(GameDatabase database, GameState state, boolean badgesOnly) {
        this.database = database;
        this.state = state;
        this.trainer = state == null ? new TrainerState() : state.trainer();
        this.badgesOnly = badgesOnly;
        this.scene = badgesOnly ? 1 : 0;
        if (state != null && state.fieldGlobals().startTime == 0L) {
            state.fieldGlobals().startTime = System.currentTimeMillis() / 1000L;     // :838 startTime = pbGetTimeNow if !startTime
        }
    }

    public Result update(InputManager input, AudioManager audio) {
        // update: the background drifts up-left one step per frame (wraps at -64).
        bgX -= 1.4f;
        bgY -= 1.4f;
        if (bgX <= -64f) bgX = 0f;
        if (bgY <= -64f) bgY = 0f;
        if (badgesOnly) {
            if (input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL)) {
                audio.playSe("BW2Cancel", 100, 100);                   // :1089-1090
                return Result.BACK;
            }
            return Result.NONE;
        }
        if (phase != 0) {                                              // the page change runs without input
            stepTransition();
            return Result.NONE;
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            audio.playSe("BW2MenuChoose", 100, 100);                   // 300_B2W2_Trainer_Card:1044/1050
            phase = scene == 0 ? 1 : 3;                                // moveUpEffect / effectFront
            step = 0;
            return Result.NONE;
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            audio.playSe("BW2Cancel", 100, 100);                       // :1060
            return Result.BACK;
        }
        return Result.NONE;
    }

    /**
     * :1004-1040: front to badges is moveUpEffect (the black slides up 46 px a frame), the redraw, effectBadges (10 frames,
     * opacity -25); back is effectFront (10 frames, opacity +25), the redraw, moveDownEffect (slides down).
     */
    private void stepTransition() {
        switch (phase) {
            case 1:
                blackY = Math.max(0f, blackY - 46f);
                if (blackY == 0f) { scene = 1; phase = 2; step = 0; }
                break;
            case 2:
                blackOpacity = Math.max(0, blackOpacity - 25);
                if (++step >= 10) phase = 0;
                break;
            case 3:
                blackOpacity = Math.min(255, blackOpacity + 25);
                if (++step >= 10) { scene = 0; phase = 4; }
                break;
            default:
                blackY = Math.min(ScreenMetrics.logicalHeight(), blackY + 46f);
                if (blackY >= ScreenMetrics.logicalHeight()) { phase = 0; blackOpacity = 255; }
                break;
        }
    }

    public void render(SpriteBatch batch, MenuAssets assets, MenuFont font, WindowSkin skin) {
        renderPage(batch, assets, font, skin);
        float h = ScreenMetrics.logicalHeight();
        if (blackY < h && blackOpacity > 0) {                          // @sprites["blacktran"]
            batch.setColor(0f, 0f, 0f, blackOpacity / 255f);
            batch.draw(assets.pixel(), 0f, 0f, ScreenMetrics.logicalWidth(), h - blackY);
            batch.setColor(Color.WHITE);
        }
    }

    private void renderPage(SpriteBatch batch, MenuAssets assets, MenuFont font, WindowSkin skin) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        if (scene == 0) {
            drawFront(batch, assets, font, w, h);
        } else {
            drawBadges(batch, assets, font, w, h);
        }
    }

    private void drawFront(SpriteBatch b, MenuAssets a, MenuFont f, float w, float h) {
        drawBg(b, a, "trainercardbg", w, h);
        boolean king = state != null && state.switches().get(200);               // :738 $game_switches[200]
        Texture card = a.graphic("Pictures/TrainerCard", king ? "trainercard king"
                : trainer.gender == 1 ? "trainercard1" : "trainercard0");
        if (card != null) {
            b.draw(card, 40f + 64f, h - 32f - card.getHeight(), card.getWidth(), card.getHeight());
        }
        Texture intro = a.graphic("Pictures/TrainerCard", trainer.gender == 1 ? "intro_Girl" : "intro_Boy");
        if (intro != null) {
            b.draw(intro, 362f + 64f, h - 64f - intro.getHeight(), intro.getWidth(), intro.getHeight());
        }
        Texture bar = a.graphic("Pictures/TrainerCard", "normalbar");
        if (bar != null) {
            b.draw(bar, 0f, h - (h - 48f) - bar.getHeight(), bar.getWidth(), bar.getHeight());
        }
        Texture icons = a.graphic("Pictures/TrainerCard", "globalicons");
        if (icons != null) {
            b.draw(icons, w - 96f, h - (h - 74f) - 64f, 64f, 64f, 0, 0, 64, 64, false, false);
        }
        drawIcon(b, a, "trainercardicons", 40f, 316f + 64f, 0, 64);
        // pbDrawTrainerCardFront text.
        f.draw(b, "名字", LABEL_X, h - (67f + 32f), BASE, SHADOW);
        f.drawRight(b, trainer.name == null || trainer.name.isEmpty() ? "训练家" : trainer.name,
                VALUE_X, h - (70f + 32f), BASE, SHADOW);
        f.draw(b, "ID No.", LABEL_X, h - (99f + 32f), BASE, SHADOW);
        f.drawRight(b, publicId(), VALUE_X, h - (99f + 32f), BASE, SHADOW);
        f.draw(b, "零花钱", LABEL_X, h - (131f + 32f), BASE, SHADOW);
        f.drawRight(b, "$" + trainer.money, VALUE_X, h - 131f, BASE, SHADOW);
        if (trainer.owned != null && !trainer.owned.isEmpty()) {
            f.draw(b, "图鉴", LABEL_X, h - (163f + 32f), BASE, SHADOW);
            f.drawRight(b, String.valueOf(trainer.owned.size()), VALUE_X, h - (163f + 32f), BASE, SHADOW);
        }
        f.draw(b, "冒险时间", LABEL_X, h - (245f + 32f), BASE, SHADOW);
        f.drawRight(b, playTime(), TIME_X, h - (242f + 32f), BASE, SHADOW);
        f.draw(b, "起始时间", LABEL_X, h - (275f + 32f), BASE, SHADOW);
        f.drawRight(b, startTime(), TIME_X, h - (278f + 32f), BASE, SHADOW);
        f.draw(b, "[C]打开徽章盒", 96f, h - (348f + 64f), BASE, SHADOW);
    }

    private void drawBadges(SpriteBatch b, MenuAssets a, MenuFont f, float w, float h) {
        drawBg(b, a, "trainercardbg2", w, h);
        int region = Math.max(0, Math.min(trainer.region, 3));
        Texture bgbadge = a.graphic("Pictures/TrainerCard", "trainerbadges" + region);
        if (bgbadge != null) {
            b.draw(bgbadge, 0f, h - bgbadge.getHeight(), bgbadge.getWidth(), bgbadge.getHeight());
        }
        Texture leaderfaces = a.graphic("Pictures/TrainerCard", "leaderfaces" + region);
        if (leaderfaces != null) {
            b.draw(leaderfaces, 32f + 48f, h - 27f - 150f, 512f, 150f, 0, 150, 512, 150, false, false);
        }
        drawIcon(b, a, "trainercardicons", 40f, 316f + 64f, 0, 0);
        // pbDrawTrainerCardBadges: obtained badges + their leader faces.
        Texture badges = a.graphic("Pictures/TrainerCard", "badges" + region);
        for (int i = 0; i < 8; i++) {
            if (!trainer.badges.contains(i + region * 4)) {
                continue;
            }
            if (badges != null) {
                b.draw(badges, 32f + 48f + 65f * i, h - 180f - 147f, 64f, 147f,
                        i * 65, 0, 64, 147, false, false);
            }
            if (leaderfaces != null) {
                b.draw(leaderfaces, 32f + 48f + 64f * i, h - 26f - 150f, 64f, 150f,
                        i * 64, 0, 64, 150, false, false);
            }
        }
        f.draw(b, "[C]返回训练师卡", 96f + 64f, h - (348f + 64f), BASE, SHADOW);
    }

    /** The scrolling card background (update: x/y -= 1.4, wrap at -64). */
    private void drawBg(SpriteBatch b, MenuAssets a, String name, float w, float h) {
        Texture bg = a.graphic("Pictures/TrainerCard", name);
        if (bg == null) {
            return;
        }
        b.draw(bg, bgX, h - bgY - bg.getHeight(), bg.getWidth(), bg.getHeight());
    }

    /** A 64x64 icon from a sheet (src y 0 or 64) at the plugin's position. */
    private void drawIcon(SpriteBatch b, MenuAssets a, String name, float x, float top, int srcX, int srcY) {
        Texture sheet = a.graphic("Pictures/TrainerCard", name);
        if (sheet == null) {
            return;
        }
        float h = ScreenMetrics.logicalHeight();
        b.draw(sheet, x, h - top - 64f, 64f, 64f, srcX, srcY, 64, 64, false, false);
    }

    private String publicId() {
        return String.format("%05d", trainer.publicID());                       // :866
    }

    private String playTime() {
        long seconds = (long) trainer.playSeconds;
        return String.format("%02d:%02d", seconds / 3600, seconds / 60 % 60);
    }

    private String startTime() {
        long start = state == null ? 0L : state.fieldGlobals().startTime;
        java.time.LocalDateTime local = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(
                start == 0L ? System.currentTimeMillis() / 1000L : start), java.time.ZoneId.systemDefault());
        return local.getYear() + "年" + local.getMonthValue() + "月" + local.getDayOfMonth() + "日";   // :886 {年}年{pbGetAbbrevMonthName}{日}日
    }
}
