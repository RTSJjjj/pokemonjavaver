package pokemon.runtime.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;

/**
 * Screen manager (project3 section 8): the placeholder screen the runtime
 * shows until the map runtime (R3) provides a real MapScreen.
 *
 * <p>Screen switching itself (ScreenManager as a dispatcher) arrives with the
 * event interpreter (R6); this class only proves the render loop works
 * end to end.</p>
 */
public final class ScreenManager extends ScreenAdapter {

    private final RuntimeContext context;
    private final String dataRoot;
    private SpriteBatch batch;
    private BitmapFont font;

    public ScreenManager(RuntimeContext context) {
        this(context, null);
    }

    /**
     * @param dataRoot where the runtime looked for the game data; shown to the player so a missing data pack is not a
     *                 silent black screen (the project's own font is not available without the data, so the text is
     *                 plain Latin)
     */
    public ScreenManager(RuntimeContext context, String dataRoot) {
        this.context = context;
        this.dataRoot = dataRoot;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.06f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (batch == null) {
            batch = new SpriteBatch();
            font = new BitmapFont();
            font.getData().setScale(Math.max(1f, Gdx.graphics.getHeight() / 360f));
        }
        float x = Gdx.graphics.getWidth() * 0.06f;
        float y = Gdx.graphics.getHeight() * 0.7f;
        float line = font.getLineHeight() * 1.3f;
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Game data not found.", x, y);
        font.setColor(0.8f, 0.8f, 0.8f, 1f);
        font.draw(batch, "Install the full APK (the one that includes the game data),", x, y - line * 1.5f);
        font.draw(batch, "or put runtime-data.zip into this folder and start again:", x, y - line * 2.5f);
        font.setColor(0.6f, 0.85f, 1f, 1f);
        font.draw(batch, dataRoot == null ? "(the app's files folder)" : dataRoot, x, y - line * 3.7f,
                Gdx.graphics.getWidth() * 0.88f, com.badlogic.gdx.utils.Align.left, true);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // the batch follows the window through the default projection
        if (batch != null) {
            batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        }
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (font != null) {
            font.dispose();
            font = null;
        }
        Gdx.app.log("ScreenManager", "placeholder screen disposed");
    }

    public RuntimeContext context() {
        return context;
    }
}
