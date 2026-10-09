package pokemon.runtime.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.pokemon.PbsData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The loading screen before the title: decodes the graphics the menus draw every frame (the icon of every item, the bag
 * and pause menu pictures) into the shared {@link MenuAssets}, whether the player has the item or not, and keeps them
 * until the game closes. Without it the first opening of the bag decoded dozens of PNGs while drawing. The work runs in
 * slices of a few milliseconds per frame so the window stays responsive and the bar moves.
 */
public final class PreloadScreen extends ScreenAdapter {

    /** Milliseconds of loading per frame. */
    private static final long SLICE_NANOS = 12_000_000L;

    private final RuntimeContext context;
    private final Supplier<Screen> next;
    private final MenuAssets assets;
    private final MenuFont font;
    private final SpriteBatch batch = new SpriteBatch();
    private final FitViewport viewport = new FitViewport(ScreenMetrics.logicalWidth(), ScreenMetrics.logicalHeight());
    private final List<Runnable> tasks = new ArrayList<>();
    private int done;
    private boolean switched;

    public PreloadScreen(RuntimeContext context, Supplier<Screen> next) {
        this.context = context;
        this.next = next;
        GraphicsLocator locator = new GraphicsLocator(context.database());
        this.assets = context.sharedMenuAssets(locator);
        String fontName = context.database() == null || context.database().project() == null
                || context.database().project().runtime == null ? null : context.database().project().runtime.messageFont;
        File fontFile = fontName == null ? null : locator.font(fontName);
        this.font = context.sharedMenuFont(fontFile, 22);
        PbsData pbs = context.pbsData();
        if (pbs != null) {
            for (PbsData.Item item : pbs.items.values()) {
                final String id = item.internalName;
                tasks.add(() -> ItemIcons.of(assets, pbs, id));                 // every item, owned or not
            }
        }
        for (String name : locator.pngNames("Pictures/Bag")) {
            tasks.add(() -> assets.graphic("Pictures/Bag", name));
        }
        for (String name : locator.pngNames("Pictures/MPM")) {
            tasks.add(() -> assets.mpm(name));
        }
    }

    @Override
    public void show() {
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        long start = System.nanoTime();
        while (done < tasks.size() && System.nanoTime() - start < SLICE_NANOS) {
            try {
                tasks.get(done).run();
            } catch (RuntimeException error) {
                Gdx.app.error("PreloadScreen", "preload step failed: " + error.getMessage());   // a missing picture is not fatal
            }
            done++;
        }
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        float width = ScreenMetrics.logicalWidth();
        float height = ScreenMetrics.logicalHeight();
        float fraction = tasks.isEmpty() ? 1f : done / (float) tasks.size();
        float barWidth = width * 0.5f;
        float barX = (width - barWidth) / 2f;
        float barY = height * 0.42f;
        batch.begin();
        batch.setColor(0.25f, 0.25f, 0.25f, 1f);
        batch.draw(assets.pixel(), barX - 2f, barY - 2f, barWidth + 4f, 12f);
        batch.setColor(0f, 0f, 0f, 1f);
        batch.draw(assets.pixel(), barX, barY, barWidth, 8f);
        batch.setColor(0.95f, 0.95f, 0.95f, 1f);
        batch.draw(assets.pixel(), barX, barY, barWidth * fraction, 8f);
        batch.setColor(Color.WHITE);
        font.drawCentered(batch, "加载中… " + (int) (fraction * 100f) + "%", width / 2f, barY - 16f, Color.WHITE, new Color(0.34f, 0.34f, 0.34f, 1f));
        batch.end();
        if (done >= tasks.size() && !switched) {
            switched = true;
            context.game().setScreen(next.get());
        }
    }

    @Override
    public void hide() {
        batch.dispose();
    }
}
