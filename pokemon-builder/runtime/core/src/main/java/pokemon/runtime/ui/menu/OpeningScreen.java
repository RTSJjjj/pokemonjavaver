package pokemon.runtime.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.data.ResourceCrypto;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.GraphicsLocator;

import java.io.File;
import java.util.function.Supplier;

/**
 * The opening movie, played at every start before the title screen and skippable with any key or a tap.
 *
 * <p>It is the Java port of the offline compositor ({@code opening/ref/render_opening_v2.py} on top of
 * {@code render_opening.py}): the same timeline in the same 1280x720 design space, drawn into a 16:9 band of the
 * logical screen (black bars above and below, like the approved 672x448 video). Everything that needed an image filter
 * in Python (desaturation, the dimmed portraits, the name plates, the watermarks, the blurred shadows, the title
 * frame) is pre-baked by {@code pokemon-builder/tools/opening/export_assets.py} into {@code Graphics/Opening}, which
 * the builder ships with every build; here only position, scale, rotation, alpha and solid shapes remain.</p>
 *
 * <p>Coordinates in the drawing helpers are the Python ones (design units, y pointing down) so every line can be
 * checked against the script; the helpers flip them for the y-up batch. The clock follows the music position, so a
 * slow frame never lets the picture drift away from the beat.</p>
 */
public final class OpeningScreen extends ScreenAdapter {

    private static final String FOLDER = "Opening";
    private static final float DW = 1280f;
    private static final float DH = 720f;
    /** Python's px(): design units -> pixels of its 1920x1080 render (sizes of the baked 1080p layers). */
    private static final float S = 1.5f;
    private static final Color PLATE = new Color(8f / 255f, 8f / 255f, 10f / 255f, 1f);   // blackplate() average

    private final RuntimeContext context;
    private final Supplier<Screen> next;
    private final GraphicsLocator locator;
    private final JsonValue data;
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final FitViewport viewport =
            new FitViewport(ScreenMetrics.logicalWidth(), ScreenMetrics.logicalHeight());
    private final Matrix4 design = new Matrix4();
    private final ObjectMap<String, Texture> textures = new ObjectMap<>();
    private final TextureRegion region = new TextureRegion();
    private final Texture white;
    private final Color ink;
    private final Array<JsonValue> characters = new Array<>();
    private final Array<JsonValue> montage = new Array<>();
    private final IntMap<Color> flashes = new IntMap<>();
    private final Array<String> pendingShots = new Array<>();

    private float scale;
    private float offsetX;
    private float offsetY;
    private Music music;
    private float musicVolume = 1f;
    private float time;
    private boolean done;

    private OpeningScreen(RuntimeContext context, Supplier<Screen> next, GraphicsLocator locator, JsonValue data) {
        this.context = context;
        this.next = next;
        this.locator = locator;
        this.data = data;
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        white = new Texture(pixel);
        pixel.dispose();
        JsonValue inkValue = data.get("ink");
        ink = new Color(inkValue.getInt(0) / 255f, inkValue.getInt(1) / 255f, inkValue.getInt(2) / 255f, 1f);
        for (JsonValue c = data.get("characters").child; c != null; c = c.next) {
            characters.add(c);
        }
        for (JsonValue m = data.get("montage").child; m != null; m = m.next) {
            montage.add(m);
            pendingShots.add(m.getString("file"));
        }
        for (JsonValue f = data.get("flashes").child; f != null; f = f.next) {
            JsonValue rgb = f.get("color");
            flashes.put(f.getInt("frame"), new Color(rgb.getInt(0) / 255f, rgb.getInt(1) / 255f, rgb.getInt(2) / 255f, 1f));
        }
    }

    /**
     * The opening when {@code Graphics/Opening/opening.json} is there (every builder output has it), otherwise the
     * next screen straight away - a source-project run or a missing pack never blocks the game.
     */
    public static Screen orNext(RuntimeContext context, Supplier<Screen> next) {
        OpeningScreen screen = create(context, next);
        return screen != null ? screen : next.get();
    }

    /** The screen, or null when the opening pack is missing or unreadable. */
    public static OpeningScreen create(RuntimeContext context, Supplier<Screen> next) {
        if (context == null || context.database() == null) {
            return null;
        }
        return create(context, next, new GraphicsLocator(context.database()));
    }

    /** Capture/test entry with an explicit Graphics root (the pack is looked up in its {@code Opening/}). */
    public static OpeningScreen create(RuntimeContext context, Supplier<Screen> next, GraphicsLocator locator) {
        File json = locator.find(FOLDER, "opening.json");
        if (json == null || !json.isFile()) {
            return null;
        }
        try {
            return new OpeningScreen(context, next, locator, new JsonReader().parse(ResourceCrypto.readString(json)));
        } catch (Exception error) {
            if (Gdx.app != null) {
                Gdx.app.error("opening", "cannot read " + json, error);
            }
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Screen
    // ------------------------------------------------------------------

    @Override
    public void show() {
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        loadStatic();
        File track = locator.find(FOLDER, "bgm.mp3");
        if (track != null && Gdx.audio != null) {
            music = context.audioManager().openStandaloneMusic(track);
            if (music != null) {
                musicVolume = context.audioManager().bgmVolumeFactor();
                music.play();
            }
        }
        time = 0f;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        float lw = ScreenMetrics.logicalWidth();
        float lh = ScreenMetrics.logicalHeight();
        scale = Math.min(lw / DW, lh / DH);
        offsetX = (lw - DW * scale) / 2f;
        offsetY = (lh - DH * scale) / 2f;
        design.idt().translate(offsetX, offsetY, 0f).scale(scale, scale, 1f);
    }

    @Override
    public void render(float delta) {
        if (done) {
            return;
        }
        context.sampleInput();
        InputManager input = context.inputManager();
        boolean skip = input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL)
                || input.wasPressed(GameAction.MENU) || Gdx.input.justTouched();
        time += Math.min(delta, 0.1f);
        if (music != null && music.isPlaying()) {
            float position = music.getPosition();
            if (Math.abs(position - time) > 0.08f) {
                time = position;                 // follow the beat, not the frame clock
            }
        }
        float duration = data.getFloat("duration");
        if (music != null) {
            float fadeStart = data.getFloat("musicFadeStart");
            float fade = time > fadeStart ? Math.max(0f, 1f - (time - fadeStart) / (duration - fadeStart)) : 1f;
            music.setVolume(musicVolume * fade);
        }
        loadShots(2);
        draw(time);
        if (skip || time >= duration) {
            finish();
        }
    }

    /** Capture/test entry: one frame at {@code seconds}, with every texture loaded first. */
    public void renderAt(float seconds) {
        loadStatic();
        loadShots(Integer.MAX_VALUE);
        draw(seconds);
    }

    /** The movie clock in seconds (follows the music while it plays). */
    public float clock() {
        return time;
    }

    /** Whether the opening's track is playing (false when audio is off or the file could not open). */
    public boolean musicPlaying() {
        return music != null && music.isPlaying();
    }

    /** Ends the opening now and shows the next screen - what any key or tap does. */
    public void skip() {
        if (!done) {
            finish();
        }
    }

    private void finish() {
        done = true;
        if (music != null) {
            music.stop();
        }
        context.game().setScreen(next.get());
        dispose();
    }

    @Override
    public void dispose() {
        if (music != null) {
            music.dispose();
            music = null;
        }
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
        white.dispose();
        batch.dispose();
        shapes.dispose();
    }

    // ------------------------------------------------------------------
    // Textures
    // ------------------------------------------------------------------

    private void loadStatic() {
        for (String name : new String[] { "film", "intro_bg", "vignette", "memory_a", "memory_b", "title" }) {
            texture(name + ".png", false);
        }
        texture("memory_full.jpg", false);
        for (JsonValue s = data.get("story").child; s != null; s = s.next) {
            texture(s.getString("file"), true);
        }
        for (JsonValue c : characters) {
            String id = c.getString("id");
            for (String part : new String[] { "body_", "ghost_", "back_", "plate_", "mark_", "shadow_" }) {
                texture(part + id + ".png", false);
            }
            texture("pixel_" + id + ".png", true);
        }
    }

    /** The 43 montage pictures decode a couple per frame while the characters play, not all at once. */
    private void loadShots(int count) {
        if (time < data.getFloat("start") && count < Integer.MAX_VALUE) {
            return;
        }
        for (int i = 0; i < count && pendingShots.size > 0; i++) {
            texture(pendingShots.removeIndex(0), false);
        }
    }

    private Texture texture(String name, boolean pixelated) {
        Texture texture = textures.get(name);
        if (texture != null || textures.containsKey(name)) {
            return texture;
        }
        File file = locator.find(FOLDER, name);
        try {
            if (file != null) {
                texture = new Texture(ResourceCrypto.handle(file));
                Texture.TextureFilter filter = pixelated ? Texture.TextureFilter.Nearest : Texture.TextureFilter.Linear;
                texture.setFilter(filter, filter);
            }
        } catch (RuntimeException error) {
            if (Gdx.app != null) {
                Gdx.app.error("opening", "cannot load " + name, error);
            }
            texture = null;
        }
        textures.put(name, texture);
        return texture;
    }

    // ------------------------------------------------------------------
    // Frame
    // ------------------------------------------------------------------

    private void draw(float t) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.setTransformMatrix(design);
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.setTransformMatrix(design);
        // the film strips and the shake overscan must not spill into the black bars
        float sx = viewport.getScreenWidth() / (float) ScreenMetrics.logicalWidth();
        float sy = viewport.getScreenHeight() / (float) ScreenMetrics.logicalHeight();
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        HdpiUtils.glScissor(viewport.getScreenX() + Math.round(offsetX * sx), viewport.getScreenY() + Math.round(offsetY * sy),
                Math.round(DW * scale * sx), Math.round(DH * scale * sy));
        batch.begin();
        frame(t);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        batch.setColor(Color.WHITE);
    }

    private void frame(float t) {
        float start = data.getFloat("start");
        JsonValue memory = data.get("memory");
        if (t < start) {
            intro(t * 21.2f / start);
            return;
        }
        if (t >= memory.getFloat("start") && t < memory.getFloat("end")) {
            memory(t, memory);
            return;
        }
        for (int i = 0; i < characters.size; i++) {
            JsonValue c = characters.get(i);
            if (t >= c.getFloat("start") && t < c.getFloat("end")) {
                character(t, c, i);
                return;
            }
        }
        if (t < data.getFloat("montageEnd")) {
            montage(t);
            return;
        }
        ending(t);
    }

    // render_opening.intro(t), t already rescaled to the reference's 21.2 s
    private void intro(float t) {
        Texture bg = texture("intro_bg.png", false);
        if (bg != null) {
            float sc = Math.max(1920f / bg.getWidth(), 1080f / bg.getHeight()) * 1.13f;
            int aw = (int) (bg.getWidth() * sc);
            int ah = (int) (bg.getHeight() * sc);
            float u = t / 21.2f;
            int x = (int) ((aw - 1920) * (0.5f + 0.38f * (float) Math.sin((u - 0.5f) * 1.5f)));
            int y = (int) ((ah - 1080) * (0.45f + 0.15f * (float) Math.sin(u * 1.6f)));
            cropFull(bg, x / sc, y / sc, 1920f / sc, 1080f / sc, 0.19f);   // 'dark' tone: brightness .19
        }
        film(t, 180, 65, 330, 340, 0.58f, 16);
        film(t, 165, -64, 1010, 380, 0.5f, 13);
        for (JsonValue s = data.get("story").child; s != null; s = s.next) {
            float a = s.getFloat("start");
            float b = s.getFloat("end");
            if (t >= a && t < b) {
                float op = Math.min(smooth((t - a) / 0.35f), smooth((b - t) / 0.35f));
                Texture text = texture(s.getString("file"), true);
                if (text != null) {
                    // whole screen pixels per glyph pixel keep the pixel font crisp
                    int k = Math.max(1, Math.round(s.getInt("k") * 0.35f));
                    pixelCentered(text, 640, 355, k, op);
                }
            }
        }
        if (t < 0.45f) {
            fill(ink, 1f - smooth(t / 0.45f));
        }
        atmosphere(t);
    }

    // render_opening_v2.character(t, c, index)
    private void character(float t, JsonValue c, int index) {
        String id = c.getString("id");
        float start = c.getFloat("start");
        float end = c.getFloat("end");
        float close = c.getFloat("close");
        float card = c.getFloat("card");
        int side = c.getInt("side");
        float local = t - start;
        float u = local / (end - start);
        if (t < close) {
            backdrop(c, u, 1f);
            Texture body = texture("body_" + id + ".png", false);
            float enter = ease(local / 0.2f);
            float zoom = 1f + 0.065f * local / (close - start);
            sprite(body, 640 + side * 125 + side * 500 * (1 - enter), 378, 740 * zoom, 0f, 1f);
            float angle = side * 8;
            film(t, 125, angle, 640, 586, 1f, 45);
            float nx = side == 1 ? 920 : 345;
            float ny = 586 - (float) Math.tan(Math.toRadians(angle)) * (nx - 640);
            layer(texture("plate_" + id + ".png", false), c.get("plateSize"), nx, ny, angle, 1f);
        } else if (t < card) {
            float v = (t - close) / (card - close);
            backdrop(c, 0.4f, 0.55f);
            Texture body = texture("body_" + id + ".png", false);
            if (body != null) {
                float z = 1f + 0.16f * ease(v);
                float fh = 1680 * z;
                float fw = body.getWidth() * fh / body.getHeight();
                float tx = 640 + 24 * (float) Math.sin(v * 3);
                float ty = 320 - 25 * v;
                JsonValue face = c.get("face");
                sprite(body, tx + (0.5f - face.getFloat(0)) * fw, ty + (0.5f - face.getFloat(1)) * fh, fh, 0f, 1f);
            }
        } else {
            backdrop(c, u, 1f);
            float v = (t - card) / (end - card);
            float enter = ease((t - card) / 0.16f);
            float cx = (side == 1 ? 1005 : 265) + side * 470 * (1 - enter);
            float empty = 640 - side * 300;
            layer(texture("mark_" + id + ".png", false), c.get("markSize"), empty - side * 170, 345 + 30 * (v - 0.5f), 0f, 1f);
            sprite(texture("ghost_" + id + ".png", false), empty + side * 20 - side * 30 * v, 400, 820, 0f, 1f);
            film(t, 78, -side * 10, 640, 686, 0.96f, 60);
            film(t, 60, side * 36, side == 1 ? 1150 : 100, 58, 0.6f, 42);
            layer(texture("shadow_" + id + ".png", false), c.get("shadowSize"), cx, 372, 0f, 1f);
            Texture pixel = texture("pixel_" + id + ".png", true);
            if (pixel != null) {
                float h = 600f;
                float w = pixel.getWidth() * h / pixel.getHeight();
                float cy = 372 + 4 * (float) Math.sin(t * 3);
                draw(pixel, snap(cx - w / 2, true), snap(cy - h / 2, false), w, h, 0f, 1f);
            }
        }
        atmosphere(t);
        if (local < 0.22f) {
            shutter(1f - ease(local / 0.22f), index);
        }
        float remain = end - t;
        if (remain < 0.34f) {
            shutter(smooth((0.34f - remain) / 0.28f), index);
        }
    }

    // render_opening_v2.backdrop(c, u): the battleback cover-scaled x1.025 and panned
    private void backdrop(JsonValue c, float u, float brightness) {
        Texture back = texture("back_" + c.getString("id") + ".png", false);
        if (back == null) {
            fill(ink, 1f);
            return;
        }
        float sc = Math.max(1920f / back.getWidth(), 1080f / back.getHeight()) * 1.025f;
        int aw = Math.round(back.getWidth() * sc);
        int ah = Math.round(back.getHeight() * sc);
        int xx = (int) ((aw - 1920) * (0.3f + 0.4f * u));
        int yy = (int) ((ah - 1080) * (c.getFloat("panY") + 0.04f * u));
        cropFull(back, xx / sc, yy / sc, 1920f / sc, 1080f / sc, brightness);
    }

    // render_opening_v2.memory(t)
    private void memory(float t, JsonValue memory) {
        float local = t - memory.getFloat("start");
        fill(ink, 1f);
        film(t, 40, 65, 640, 360, 0.8f, 60);
        float side = 1280f;
        float originY = -230f;
        float[][] parts = { { 0f, -1280f, -200f, memory.getFloat("aWidth") }, { 0.38f, 1280f, 240f, 1f } };
        String[] names = { "memory_a.png", "memory_b.png" };
        for (int i = 0; i < 2; i++) {
            float phase = parts[i][0];
            if (local >= phase) {
                float q = ease((local - phase) / 0.68f);
                topLeft(texture(names[i], false), parts[i][1] * (1 - q), originY + parts[i][2] * (1 - q),
                        side * parts[i][3], side, 1f);
            }
        }
        if (local >= 1.12f) {
            topLeft(texture("memory_full.jpg", false), 0f, originY, side, side, 1f);
        }
        float flash = Math.max(0f, 1f - Math.abs(local - 1.38f) / 0.115f) * 0.86f;
        if (flash > 0f) {
            fill(new Color(1f, 249f / 255f, 236f / 255f, 1f), flash);
        }
        if (local > 2.2f) {
            shutter(smooth((local - 2.2f) / 0.3f), 1);
        }
    }

    // render_opening_v2.montage(t): frame-exact cuts and one-frame colour flashes
    private void montage(float t) {
        int fi = Math.round(t * data.getInt("fps"));
        Color flash = flashes.get(fi);
        if (flash != null) {
            fill(flash, 1f);
            return;
        }
        int i = 0;
        for (int j = 0; j < montage.size; j++) {
            if (montage.get(j).getInt("start") <= fi) {
                i = j;
            }
        }
        JsonValue cut = montage.get(i);
        Texture shot = texture(cut.getString("file"), false);
        if (shot == null) {
            fill(ink, 1f);
            return;
        }
        float u = (fi - cut.getInt("start")) / (float) cut.getInt("frames");
        float k = i / (float) (montage.size - 1);
        float sc = Math.max(1920f / shot.getWidth(), 1080f / shot.getHeight()) * 1.21f;
        int aw = Math.round(shot.getWidth() * sc);
        int ah = Math.round(shot.getHeight() * sc);
        float amp = px(2 + 52 * (float) Math.pow(k, 1.7));
        float dx = amp * (float) Math.sin(fi * 2.399 + i);
        float dy = amp * 0.72f * (float) Math.sin(fi * 1.777 + i * 2);
        float xx = (aw - 1920) / 2f + (u - 0.5f) * px(45) * (i % 2 == 1 ? 1 : -1) + dx;
        float yy = (ah - 1080) * (0.31f + 0.05f * u) + dy;
        xx = Math.round(Math.max(0, Math.min(aw - 1920, xx)));
        yy = Math.round(Math.max(0, Math.min(ah - 1080, yy)));
        float margin = 0f;
        if (k > 0.45f && fi % 2 == 0) {
            margin = Math.round(px(6 + 18 * k));     // impact zoom; the overscan keeps every edge filled
        }
        cropFull(shot, (xx + margin) / sc, (yy + margin) / sc, (1920 - 2 * margin) / sc, (1080 - 2 * margin) / sc, 1f);
    }

    // render_opening_v2.ending(t): the title frame fades in from ink and out to black
    private void ending(float t) {
        fill(ink, 1f);
        Texture title = texture("title.png", false);
        float alpha = t < data.getFloat("montageEnd") + 0.6f ? ease((t - data.getFloat("montageEnd")) / 0.6f) : 1f;
        if (title != null) {
            topLeft(title, 0f, 0f, DW, DH, alpha);
        }
        float fade = data.getFloat("titleFade");
        if (t > fade) {
            fill(Color.BLACK, smooth((t - fade) / (data.getFloat("duration") - fade)));
        }
    }

    // ------------------------------------------------------------------
    // Effects
    // ------------------------------------------------------------------

    /** render_opening_v2.fastfilm: the film strip tiled along a rotated band and slid by its own period. */
    private void film(float t, float height, float angle, float cx, float cy, float opacity, float speed) {
        Texture film = texture("film.png", false);
        if (film == null) {
            return;
        }
        float tile = film.getWidth() * height / film.getHeight();
        float period = Math.round(film.getWidth() * px(height) / film.getHeight()) / S;
        float shift = (t * speed * 12) % period - period / 2;
        double a = Math.toRadians(angle);
        float cos = (float) Math.cos(a);
        float sin = (float) Math.sin(a);
        float bx = cx + shift * cos;
        float by = cy - shift * sin;
        int tiles = (int) Math.ceil(2400f / tile);
        for (int k = 0; k < tiles; k++) {
            float lx = -1200f + k * tile + tile / 2f;
            draw(film, bx + lx * cos - tile / 2f, by - lx * sin - height / 2f, tile, height, angle, opacity);
        }
    }

    /** render_opening.atmosphere: vignette, the rare hairline scratch and the thin top / bottom bars. */
    private void atmosphere(float t) {
        Texture vignette = texture("vignette.png", false);
        if (vignette != null) {
            topLeft(vignette, 0f, 0f, DW, DH, 1f);
        }
        int frame = (int) (t * 30);
        if (frame % 73 < 2) {
            rect((frame * 37) % 1280, 0, 1f / scale / 1.5f, DH, new Color(72f / 255f, 63f / 255f, 53f / 255f, 1f), 1f);
        }
        rect(0, 0, DW, 17.33f, ink, 1f);
        rect(0, DH - 17.33f, DW, 17.33f, ink, 1f);
    }

    /** render_opening_v2.shutter(p, kind): black slide plates in six patterns (kind 5+ = split diagonal). */
    private void shutter(float p, int kind) {
        p = clamp(p);
        if (p <= 0f) {
            return;
        }
        if (p >= 0.995f) {
            rect(0, 0, DW, DH, PLATE, 1f);
            return;
        }
        if (kind == 2 || kind == 3 || kind == 4) {
            if (kind == 2) {
                int j = 0;
                for (int x = 0; x < DW; x += 160, j++) {
                    float hh = DH * clamp(p * 1.35f - (j % 3) * 0.14f);
                    if (j % 2 == 1) {
                        rect(x, 0, 160, hh, PLATE, 1f);
                    } else {
                        rect(x, DH - hh, 160, hh, PLATE, 1f);
                    }
                }
            } else if (kind == 3) {
                float half = 60 * p;
                for (int y = 0; y < DH; y += 120) {
                    for (int x = 0; x < DW; x += 120) {
                        rect(x + 60 - half, y + 60 - half, 2 * half, 2 * half, PLATE, 1f);
                    }
                }
            } else {
                for (int y = 0; y < DH; y += 90) {
                    rect(0, y, DW, 90 * p, PLATE, 1f);
                }
            }
            return;
        }
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PLATE);
        if (kind == 0) {
            float cell = 150f;
            float r = cell * 1.07f * p;
            int row = 0;
            for (float y = -cell; y < DH + cell; y += cell, row++) {
                for (float x0 = -cell; x0 < DW + cell; x0 += cell) {
                    float x = x0 + (row % 2) * 75f;
                    quad(x, y - r, x + r, y, x, y + r, x - r, y);
                }
            }
        } else if (kind == 1) {
            float cell = 170f;
            int j = 0;
            for (float x = -500f; x < DW + 500f; x += cell, j++) {
                float ww = cell * p;
                float s = j % 2 == 1 ? cell - ww : 0f;
                quad(x + s, 0, x + s + ww, 0, x + s + ww + 340, DH, x + s + 340, DH);
            }
        } else {
            float ww = (DW + 450) * p / 2;
            quad(0, 0, ww, 0, ww - 220, DH, 0, DH);
            quad(DW - ww + 220, 0, DW, 0, DW, DH, DW - ww, DH);
        }
        shapes.end();
        batch.begin();
    }

    // ------------------------------------------------------------------
    // Drawing helpers: Python coordinates (design units, y down) -> the y-up batch
    // ------------------------------------------------------------------

    /** A picture of design height {@code height}, centred on (cx, cy), rotated like PIL's rotate(angle). */
    private void sprite(Texture texture, float cx, float cy, float height, float angle, float alpha) {
        if (texture == null) {
            return;
        }
        float w = texture.getWidth() * height / texture.getHeight();
        draw(texture, cx - w / 2, cy - height / 2, w, height, angle, alpha);
    }

    /** A layer baked at the 1080p render's pixel size ({@code size} = [w, h] px), centred on (cx, cy). */
    private void layer(Texture texture, JsonValue size, float cx, float cy, float angle, float alpha) {
        if (texture == null) {
            return;
        }
        float w = size.getFloat(0) / S;
        float h = size.getFloat(1) / S;
        draw(texture, cx - w / 2, cy - h / 2, w, h, angle, alpha);
    }

    private void topLeft(Texture texture, float x, float y, float w, float h, float alpha) {
        if (texture != null) {
            draw(texture, x, y, w, h, 0f, alpha);
        }
    }

    /** Draws with the top-left corner at (x, y) in y-down design units, rotating around the centre. */
    private void draw(Texture texture, float x, float y, float w, float h, float angle, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        region.setRegion(texture);
        batch.setColor(1f, 1f, 1f, Math.min(1f, alpha));
        batch.draw(region, x, DH - y - h, w / 2, h / 2, w, h, 1f, 1f, angle);
    }

    /** The source rectangle (texture pixels, y down) stretched over the whole design frame. */
    private void cropFull(Texture texture, float sx, float sy, float sw, float sh, float brightness) {
        region.setRegion(texture);
        region.setRegion(sx / texture.getWidth(), sy / texture.getHeight(),
                (sx + sw) / texture.getWidth(), (sy + sh) / texture.getHeight());
        batch.setColor(brightness, brightness, brightness, 1f);
        batch.draw(region, 0f, 0f, DW, DH);
    }

    /** A pixel-font picture at k whole screen pixels per glyph pixel, centred on (cx, cy). */
    private void pixelCentered(Texture texture, float cx, float cy, int k, float alpha) {
        float w = texture.getWidth() * k / scale;
        float h = texture.getHeight() * k / scale;
        draw(texture, snap(cx - w / 2, true), snap(cy - h / 2, false), w, h, 0f, alpha);
    }

    /** Rounds a design coordinate to a whole logical pixel (the nearest-filtered pixel art stays even). */
    private float snap(float value, boolean horizontal) {
        float origin = horizontal ? offsetX : offsetY;
        return (Math.round(origin + value * scale) - origin) / scale;
    }

    private void rect(float x, float y, float w, float h, Color color, float alpha) {
        if (alpha <= 0f || w <= 0f || h <= 0f) {
            return;
        }
        batch.setColor(color.r, color.g, color.b, Math.min(1f, alpha));
        batch.draw(white, x, DH - y - h, w, h);
    }

    private void fill(Color color, float alpha) {
        rect(0, 0, DW, DH, color, alpha);
    }

    private void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        shapes.triangle(x1, DH - y1, x2, DH - y2, x3, DH - y3);
        shapes.triangle(x1, DH - y1, x3, DH - y3, x4, DH - y4);
    }

    private static float px(float value) {
        return Math.round(value * S);
    }

    private static float clamp(float x) {
        return Math.min(1f, Math.max(0f, x));
    }

    private static float ease(float x) {
        x = clamp(x);
        return 1f - (1f - x) * (1f - x) * (1f - x);
    }

    private static float smooth(float x) {
        x = clamp(x);
        return x * x * (3f - 2f * x);
    }
}
