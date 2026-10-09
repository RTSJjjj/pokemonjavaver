package pokemon.runtime.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.input.touch.TouchButton;
import pokemon.runtime.input.touch.TouchLayout;
import pokemon.runtime.input.touch.TouchOverlay;
import pokemon.runtime.input.touch.TouchPad;
import pokemon.runtime.ui.menu.MenuFont;

import java.io.File;
import java.util.List;

/**
 * Renders the on-screen key layout over a phone-shaped frame and writes a PNG
 * (release plan P1 evidence). Arguments: dataRoot outPng [gameShotPng] [width height].
 * The optional game shot (a 672x488 capture) is centred at full height, which
 * is where the FitViewport puts the game on a wide phone.
 */
public final class TouchPreview extends ApplicationAdapter {

    private final String[] args;
    private Throwable failure;

    private TouchPreview(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("dataRoot outPng [gameShotPng] [width height]");
        }
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(640, 480);
        config.disableAudio(true);
        TouchPreview preview = new TouchPreview(args);
        new Lwjgl3Application(preview, config);
        if (preview.failure != null) {
            throw new RuntimeException("touch preview failed", preview.failure);
        }
    }

    @Override
    public void create() {
        try {
            int width = args.length > 4 ? Integer.parseInt(args[3]) : 2400;
            int height = args.length > 4 ? Integer.parseInt(args[4]) : 1080;
            pokemon.runtime.app.PokemonGame game = new pokemon.runtime.app.PokemonGame(args[0], -1, null);
            RuntimeContext context = new RuntimeContext("capture", game, args[0]);
            context.loadRuntimeConfig();
            String name = context.database().project().runtime.messageFont;
            File fontFile = name == null ? null
                    : new pokemon.runtime.map.GraphicsLocator(context.database()).font(name);
            MenuFont big = new MenuFont(fontFile, Math.round(height * 0.075f));
            MenuFont small = new MenuFont(fontFile, Math.round(height * 0.032f));
            TouchOverlay overlay = new TouchOverlay(big, small);
            List<TouchButton> keys = TouchLayout.create(width, height, 1f);
            TouchPad pad = new TouchPad(keys, 0f);

            FrameBuffer buffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
            SpriteBatch batch = new SpriteBatch();
            buffer.begin();
            Gdx.gl.glViewport(0, 0, width, height);
            Gdx.gl.glClearColor(0.04f, 0.04f, 0.06f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            if (args.length > 2 && !args[2].isEmpty() && new File(args[2]).isFile()) {
                Texture shot = new Texture(Gdx.files.absolute(args[2]));
                float gw = height * shot.getWidth() / (float) shot.getHeight();
                com.badlogic.gdx.graphics.OrthographicCamera camera = new com.badlogic.gdx.graphics.OrthographicCamera();
                camera.setToOrtho(false, width, height);
                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                batch.draw(shot, (width - gw) / 2f, 0f, gw, height, 0, 0, shot.getWidth(), shot.getHeight(), false, false);
                batch.end();
            }
            overlay.render(batch, width, height, keys, pad);
            Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, width, height);
            PixmapIO.writePNG(Gdx.files.absolute(new File(args[1]).getAbsolutePath()), pixels, -1, true);
            pixels.dispose();
            buffer.end();
            System.out.println("wrote " + args[1]);
        } catch (Throwable error) {
            failure = error;
        } finally {
            Gdx.app.exit();
        }
    }
}
