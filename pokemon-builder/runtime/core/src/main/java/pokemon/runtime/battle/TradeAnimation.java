package pokemon.runtime.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.function.Function;

/**
 * 227_PScreen_Trading {@code PokemonTrade_Scene#pbScene1 / pbScene2}: the two {@code PictureEx} animations of a trade - the
 * outgoing Pokemon is recalled into its Poke Ball and the ball leaves through the top of the screen, then the incoming
 * ball drops, bounces, opens and the Pokemon appears. The timelines are the Ruby's, run by the same {@link PictureEx}
 * port the battle animations use ({@code pbRunPictures} = one {@code update} per 40 fps tick until no picture runs).
 */
public final class TradeAnimation {

    /** {@code pbSEPlay}. */
    public interface Se {
        void play(String name, int volume, int pitch);
    }

    private static final float WIDTH = PictureEx.Graphics.WIDTH;
    private static final float HEIGHT = PictureEx.Graphics.HEIGHT;
    /** {@code Color.new(31*8,22*8,30*8,255)}. */
    private static final float[] PINK = {31 * 8, 22 * 8, 30 * 8, 255};

    private final int ball1;
    private final int ball2;
    private final String cry2;
    private final PictureEx.SePlayer se;
    private final BattleSprite poke1 = new BattleSprite("rsprite1", BattleSprite.Kind.IMAGE);
    private final BattleSprite poke2 = new BattleSprite("rsprite2", BattleSprite.Kind.IMAGE);
    private final BattleSprite ball = new BattleSprite("ball", BattleSprite.Kind.IMAGE);
    private PictureEx pictureBall;
    private PictureEx picturePoke;
    private BattleSprite pokeTarget;
    private boolean ballShown;
    /** Created on the first draw: it needs the GL context. */
    private BattleSpriteShader shader;

    /**
     * @param ball1 {@code @pokemon.ballused}; @param ball2 {@code @pokemon2.ballused}; @param cry2 the logical id of the
     *              incoming Pokemon's cry, or null when it has none ({@code pbResolveAudioSE(cry)})
     */
    public TradeAnimation(int ball1, int ball2, String cry2, Se se) {
        this.ball1 = ball1;
        this.ball2 = ball2;
        this.cry2 = cry2;
        this.se = se::play;
        poke1.x = WIDTH / 2f;                                     // pbStartScreen: rsprite1.x = width/2, y = 264
        poke1.y = 264;
        poke2.x = WIDTH / 2f;
        poke2.y = 264;
        poke2.visible = false;                                    // :52 rsprite2.visible = false
    }

    private static String ballName(int ballType) {
        return String.format("Graphics/Battle animations/ball_%02d", ballType);
    }

    private static String ballOpenName(int ballType) {
        return String.format("Graphics/Battle animations/ball_%02d_open", ballType);
    }

    /** {@code pbScene1} (227:57-95). */
    public void startScene1() {
        resetBall();
        pictureBall = PictureEx.forSprite(ball, PictureEx.Origin.TOP_LEFT, se);
        picturePoke = PictureEx.forSprite(poke1, PictureEx.Origin.TOP_LEFT, se);
        pokeTarget = poke1;
        // Starting position of ball (:62-66)
        pictureBall.setXY(0, WIDTH / 2f, 48);
        pictureBall.setName(0, ballName(ball1));
        pictureBall.setSrcSize(0, 32, 64);
        pictureBall.setOrigin(0, PictureEx.Origin.CENTER);
        pictureBall.setVisible(0, true);
        // Starting position of sprite (:68-70)
        picturePoke.setXY(0, poke1.x, poke1.y);
        picturePoke.setOrigin(0, PictureEx.Origin.BOTTOM);
        picturePoke.setVisible(0, true);
        // Change Pokemon color (:72)
        picturePoke.moveColor(2, 5, PINK.clone());
        // Recall (:74-77)
        int delay = picturePoke.totalDuration();
        picturePoke.setSE(delay, "Battle recall");
        pictureBall.setName(delay, ballOpenName(ball1));
        pictureBall.setSrcSize(delay, 32, 64);
        // Move sprite to ball (:79-82)
        picturePoke.moveZoom(delay, 8, 0);
        picturePoke.moveXY(delay, 8, WIDTH / 2f, 48);
        picturePoke.setSE(delay + 5, "Battle jump to ball");
        picturePoke.setVisible(delay + 8, false);
        delay = picturePoke.totalDuration() + 1;                  // :83
        pictureBall.setName(delay, ballName(ball1));
        pictureBall.setSrcSize(delay, 32, 64);
        // Make Poke Ball go off the top of the screen (:87-88)
        delay = picturePoke.totalDuration() + 10;
        pictureBall.moveXY(delay, 6, WIDTH / 2f, -32);
        ballShown = true;
    }

    /** {@code pbScene2} (227:97-150). */
    public void startScene2() {
        resetBall();
        pictureBall = PictureEx.forSprite(ball, PictureEx.Origin.TOP_LEFT, se);
        picturePoke = PictureEx.forSprite(poke2, PictureEx.Origin.TOP_LEFT, se);
        pokeTarget = poke2;
        float rsprite2Y = poke2.y;
        // Starting position of ball (:102-106)
        pictureBall.setXY(0, WIDTH / 2f, -32);
        pictureBall.setName(0, ballName(ball2));
        pictureBall.setSrcSize(0, 32, 64);
        pictureBall.setOrigin(0, PictureEx.Origin.CENTER);
        pictureBall.setVisible(0, true);
        // Starting position of sprite (:108-111)
        picturePoke.setOrigin(0, PictureEx.Origin.BOTTOM);
        picturePoke.setZoom(0, 0);
        picturePoke.setColor(0, PINK.clone());
        picturePoke.setVisible(0, false);
        // Dropping ball (:113-128)
        float y = HEIGHT - 96 - 16 - 16;                          // end point of Poke Ball
        int delay = picturePoke.totalDuration() + 2;
        for (int i = 0; i < 4; i++) {
            int t = new int[] {4, 4, 3, 2}[i];                     // Time taken to rise or fall for each bounce
            int d = new int[] {1, 2, 4, 8}[i];                     // Fraction of the starting height each bounce rises to
            if (i == 0) {
                delay -= t;
            }
            if (i > 0) {
                pictureBall.setZoomXY(delay, 100 + 5 * (5 - i), 100 - 5 * (5 - i));   // Squish
                pictureBall.moveZoom(delay, 2, 100);                                  // Unsquish
                pictureBall.moveXY(delay, t, WIDTH / 2f, y - 100 / d);
            }
            pictureBall.moveXY(delay + t, t, WIDTH / 2f, y);
            pictureBall.setSE(delay + 2 * t, "Battle ball drop");
            delay = pictureBall.totalDuration();
        }
        picturePoke.setXY(delay, WIDTH / 2f, y);
        // Open Poke Ball (:130-134)
        delay = pictureBall.totalDuration() + 15;
        pictureBall.setSE(delay, "Battle recall");
        pictureBall.setName(delay, ballOpenName(ball2));
        pictureBall.setSrcSize(delay, 32, 64);
        pictureBall.setVisible(delay + 5, false);
        // Pokemon appears and enlarges (:136-138)
        picturePoke.setVisible(delay, true);
        picturePoke.moveZoom(delay, 8, 100);
        picturePoke.moveXY(delay, 8, WIDTH / 2f, rsprite2Y);
        // Return Pokemon's color to normal and play cry (:140-143)
        delay = picturePoke.totalDuration();
        picturePoke.moveColor(delay, 5, new float[] {PINK[0], PINK[1], PINK[2], 0});
        if (cry2 != null) {
            picturePoke.setSE(delay, cry2);
        }
        ballShown = true;
    }

    private void resetBall() {
        ball.visible = true;
        ball.x = 0;
        ball.y = 0;
        ball.zoomX = 1f;
        ball.zoomY = 1f;
    }

    /** One {@code pbRunPictures} pass (:6-26); true while any picture is still running. */
    public boolean tick() {
        if (picturePoke == null) {
            return false;
        }
        picturePoke.update();
        pictureBall.update();
        picturePoke.applyTo(pokeTarget);
        pictureBall.applyTo(ball);
        return picturePoke.running() || pictureBall.running();
    }

    /** The first Pokemon is gone after scene 1 and the second stays after scene 2 (their sprites' visibility). */
    public boolean pokemon1Visible() {
        return poke1.visible;
    }

    public boolean pokemon2Visible() {
        return poke2.visible;
    }

    /**
     * Draws the Poke Ball and the Pokemon of the running scene. {@code bitmaps} loads a {@code Graphics/...} path
     * (without the extension) to a texture; {@code h} is the screen height in pixels.
     */
    public void render(SpriteBatch batch, Function<String, Texture> bitmaps, Texture pokemon1, Texture pokemon2, float h) {
        if (shader == null) {
            shader = new BattleSpriteShader();
        }
        boolean useShader = shader.isCompiled();
        drawPokemon(batch, poke1, pokemon1, h, useShader);
        drawPokemon(batch, poke2, pokemon2, h, useShader);
        if (ballShown && ball.visible && ball.name != null && !ball.name.isEmpty()) {
            Texture texture = bitmaps.apply(ball.name);
            if (texture != null) {
                float srcW = ball.srcWidth >= 0 ? ball.srcWidth : texture.getWidth();
                float srcH = ball.srcHeight >= 0 ? ball.srcHeight : texture.getHeight();
                srcW = Math.min(srcW, texture.getWidth());
                srcH = Math.min(srcH, texture.getHeight());
                float w = srcW * ball.zoomX;
                float height = srcH * ball.zoomY;
                batch.setColor(1f, 1f, 1f, ball.opacity / 255f);
                batch.draw(texture, ball.x - w / 2f, h - (ball.y + height / 2f), w, height,
                        0, 0, (int) srcW, (int) srcH, false, false);          // origin Center
                batch.setColor(Color.WHITE);
            }
        }
    }

    private void drawPokemon(SpriteBatch batch, BattleSprite sprite, Texture texture, float h, boolean useShader) {
        if (texture == null || !sprite.visible || sprite.zoomX <= 0f || sprite.zoomY <= 0f) {
            return;
        }
        boolean tinted = sprite.color[3] > 0f;
        if (useShader && tinted) {
            batch.setShader(shader.program());
            batch.flush();
            shader.setTone(new float[] {0, 0, 0, 0});
            shader.setColor(sprite.color);
        }
        float w = texture.getWidth() * sprite.zoomX;
        float height = texture.getHeight() * sprite.zoomY;
        batch.setColor(1f, 1f, 1f, sprite.opacity / 255f);
        batch.draw(texture, sprite.x - w / 2f, h - sprite.y, w, height);        // origin Bottom: y is the feet
        batch.setColor(Color.WHITE);
        if (useShader && tinted) {
            batch.flush();
            batch.setShader(null);
        }
    }

    public void dispose() {
        if (shader != null) {
            shader.dispose();
        }
    }
}
