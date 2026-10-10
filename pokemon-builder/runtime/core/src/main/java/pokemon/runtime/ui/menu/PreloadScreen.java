package pokemon.runtime.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
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
 * P4: the loading screen before the title. It decodes, once and behind a progress strip, what the menus and the field draw
 * every frame - the icon of every item, the Pokemon icons, the menu / bag / party / summary / storage / Pokedex / battle UI
 * pictures, the window frames in use and the menu sound effects - into the shared {@link MenuAssets} / {@link
 * pokemon.runtime.audio.AudioManager}, and keeps them until the game closes. The work runs in slices of a few milliseconds
 * per frame so the window stays responsive and the strip moves.
 *
 * <p>Layout: a long strip along the bottom of the screen (text left, percentage right, the bar under them). The area above
 * is the background: the pictures of {@code Graphics/Titles/Loading/} when the project has any, shown one after the other
 * with a cross-fade (a slide show), otherwise a plain dark gradient. Dropping new pictures in that folder is all it takes.</p>
 */
public final class PreloadScreen extends ScreenAdapter {

    /** Milliseconds of loading per frame. */
    private static final long SLICE_NANOS = 12_000_000L;
    /** The UI picture folders of Graphics/ the menus draw from. */
    private static final String[] FOLDERS = {
            "Pictures/Bag", "Pictures/Party", "Pictures/Summary", "Pictures/Storage", "Pictures/Pokedex", "Pictures/Habitats",
            "Pictures/Ready Menu", "Pictures/QuestUI", "Pictures/TrainerCard", "Pictures/Pokegear", "Pictures/Save",
            "Pictures/Naming", "Pictures/Location", "Pictures/Move Flags", "Pictures/Battle", "Pictures/Starter Selection",
            "Pictures/Evolution Backs", "Pictures/Egg Hatcher", "Pictures/Mega Evolution"};
    /** The sound effects the screens play all the time ("GUI sel cursor" ...). */
    private static final String[] SE_PREFIXES = {"GUI ", "Battle ", "PC ", "Door", "Player"};

    private static final float STRIP_HEIGHT = 44f;
    private static final float MARGIN = 24f;
    private static final float BAR_HEIGHT = 6f;
    private static final float SLIDE_SECONDS = 4f;
    private static final float FADE_SECONDS = 0.8f;

    private final RuntimeContext context;
    private final Supplier<Screen> next;
    private final MenuAssets assets;
    private final MenuFont font;
    private final SpriteBatch batch = new SpriteBatch();
    private final FitViewport viewport = new FitViewport(ScreenMetrics.logicalWidth(), ScreenMetrics.logicalHeight());
    private final List<Runnable> tasks = new ArrayList<>();
    private final List<String> slides;
    private int done;
    private float shown;               // the bar's eased fraction
    private float clock;
    private boolean switched;

    public PreloadScreen(RuntimeContext context, Supplier<Screen> next) {
        this.context = context;
        this.next = next;
        GraphicsLocator locator = new GraphicsLocator(context.database());
        this.assets = context.sharedMenuAssets(locator);
        String fontName = context.database() == null || context.database().project() == null
                || context.database().project().runtime == null ? null : context.database().project().runtime.messageFont;
        File fontFile = fontName == null ? null : locator.font(fontName);
        this.font = context.sharedMenuFont(fontFile, 20);
        this.slides = locator.pngNames("Titles/Loading");
        PbsData pbs = context.pbsData();
        if (pbs != null) {
            for (PbsData.Item item : pbs.items.values()) {
                final String id = item.internalName;
                tasks.add(() -> ItemIcons.of(assets, pbs, id));                 // every item, owned or not
            }
        }
        for (String folder : FOLDERS) {
            for (String name : locator.pngNames(folder)) {
                tasks.add(() -> assets.graphic(folder, name));
            }
        }
        for (String name : locator.pngNames("Pictures/MPM")) {
            tasks.add(() -> assets.mpm(name));
        }
        for (String name : locator.pngNames("Icons")) {                          // icon001 ... and iconEgg: the species' own icons
            if (name.matches("icon\\d{3}") || "iconEgg".equals(name)) {
                tasks.add(() -> assets.icon(name));
            }
        }
        final pokemon.runtime.ui.menu.GameSettings settings = context.settings();
        if (settings != null) {                                                 // the window frames the player picked
            tasks.add(() -> assets.skin(GameSettings.TEXT_FRAMES[Math.floorMod(settings.frame, GameSettings.TEXT_FRAMES.length)]));
            tasks.add(() -> assets.skin(GameSettings.SPEECH_FRAMES[Math.floorMod(settings.textskin, GameSettings.SPEECH_FRAMES.length)]));
        }
        if (context.audioManager() != null) {
            for (String id : context.audioManager().seIdsWithPrefix(SE_PREFIXES)) {
                tasks.add(() -> context.audioManager().preloadSe(id));
            }
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
        clock += Math.max(0f, delta);
        float target = tasks.isEmpty() ? 1f : done / (float) tasks.size();
        shown += (target - shown) * Math.min(1f, delta * 8f);
        if (done >= tasks.size() && target - shown < 0.004f) {
            shown = 1f;
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        float width = ScreenMetrics.logicalWidth();
        float height = ScreenMetrics.logicalHeight();
        batch.begin();
        drawBackground(width, height);
        drawStrip(width);
        batch.setColor(Color.WHITE);
        batch.end();
        if (shown >= 1f && !switched) {
            switched = true;
            context.game().setScreen(next.get());
        }
    }

    /** The slide show of Graphics/Titles/Loading (cross-fading), or a plain dark gradient. */
    private void drawBackground(float width, float height) {
        Texture pixel = assets.pixel();
        // the gradient is always there: it is the backdrop of the slides and the whole background without them
        int bands = 24;
        for (int i = 0; i < bands; i++) {
            float t = i / (float) (bands - 1);                       // 0 bottom .. 1 top
            batch.setColor(0.04f + 0.06f * t, 0.05f + 0.08f * t, 0.09f + 0.14f * t, 1f);
            batch.draw(pixel, 0f, height * i / bands, width, height / bands + 1f);
        }
        if (slides.isEmpty()) {
            return;
        }
        int count = slides.size();
        int index = (int) (clock / SLIDE_SECONDS) % count;
        float into = clock % SLIDE_SECONDS;
        drawSlide(index, Math.min(1f, into / FADE_SECONDS), width, height);   // fades in over the previous one
        if (count > 1 && into < FADE_SECONDS) {
            drawSlide((index + count - 1) % count, 1f - into / FADE_SECONDS, width, height);
        }
    }

    private void drawSlide(int index, float alpha, float width, float height) {
        Texture slide = assets.graphic("Titles/Loading", slides.get(index));
        if (slide == null || alpha <= 0f) {
            return;
        }
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(slide, 0f, 0f, width, height);
    }

    /** The long strip along the bottom: "加载中…" left, the percentage right, the bar below them. */
    private void drawStrip(float width) {
        Texture pixel = assets.pixel();
        batch.setColor(0f, 0f, 0f, 0.62f);
        batch.draw(pixel, 0f, 0f, width, STRIP_HEIGHT);
        batch.setColor(1f, 1f, 1f, 0.16f);
        batch.draw(pixel, 0f, STRIP_HEIGHT, width, 1f);                  // the strip's top edge

        float barX = MARGIN;
        float barWidth = width - 2f * MARGIN;
        float barY = 9f;
        batch.setColor(1f, 1f, 1f, 0.14f);                                // the track
        batch.draw(pixel, barX, barY, barWidth, BAR_HEIGHT);
        float fill = barWidth * Math.max(0f, Math.min(1f, shown));
        if (fill > 0f) {
            batch.setColor(0.36f, 0.66f, 0.98f, 1f);                      // the fill, lighter on top
            batch.draw(pixel, barX, barY, fill, BAR_HEIGHT);
            batch.setColor(0.72f, 0.88f, 1f, 1f);
            batch.draw(pixel, barX, barY + BAR_HEIGHT * 0.5f, fill, BAR_HEIGHT * 0.5f);
            float glow = (clock * 0.9f) % 1.4f - 0.2f;                    // a highlight that runs along the fill
            float glowWidth = Math.min(fill, 56f);
            float glowX = barX + (fill - glowWidth) * Math.max(0f, Math.min(1f, glow));
            batch.setColor(1f, 1f, 1f, 0.55f);
            batch.draw(pixel, glowX, barY, glowWidth, BAR_HEIGHT);
        }
        Color white = Color.WHITE;
        Color shadow = new Color(0.1f, 0.1f, 0.1f, 1f);
        float textY = STRIP_HEIGHT - 8f;
        font.draw(batch, "加载中…", barX, textY, white, shadow);
        font.drawRight(batch, (int) (Math.min(1f, shown) * 100f) + "%", barX + barWidth, textY, white, shadow);
    }

    @Override
    public void hide() {
        batch.dispose();
    }
}
