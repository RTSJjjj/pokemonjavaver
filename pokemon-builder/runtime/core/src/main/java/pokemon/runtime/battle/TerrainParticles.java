package pokemon.runtime.battle;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import java.util.Random;

/**
 * The particles of a battle terrain (this runtime's own addition; the terrains have no animation data):
 * drifting leaves for Grassy, white arcs on the ground for Electric, falling petals for Misty, a purple haze
 * for Psychic, fireflies for Bug Lure and ground mist for Cold. Every bitmap is drawn here with {@link Pixmap}s - no image files.
 *
 * <p>Coordinates are RMXP's (y down) inside the battle field above the message bar; {@link #render}
 * flips them for the batch. Drawn between the backdrop and the battlers.</p>
 */
final class TerrainParticles {

    private static final int MAX = 40;
    /** Where the ground starts, as a fraction of the field height (the horizon of the battle backs). */
    private static final float GROUND_TOP = 0.42f;

    private final Random random = new Random();
    private int terrain = PBBattleTerrains.None;
    private float fieldW = PictureEx.Graphics.WIDTH;
    private float fieldH = PictureEx.Graphics.HEIGHT - 96f;
    private float clock;
    private int count;

    // per particle
    private final float[] x = new float[MAX], y = new float[MAX], vx = new float[MAX], vy = new float[MAX];
    private final float[] life = new float[MAX], span = new float[MAX], rot = new float[MAX], spin = new float[MAX];
    private final float[] size = new float[MAX], phase = new float[MAX];
    private final int[] kind = new int[MAX];                 // which bitmap / role inside the terrain

    // bitmaps, made on first use
    private Texture[] bolts;
    private Texture spark, blob, glow, sparkle, mist, leaf, petal;

    void setField(float width, float height) {
        fieldW = width;
        fieldH = height;
    }

    /** Switches to a terrain's particle set (None clears it); the same terrain keeps its particles. */
    void update(float delta, int shownTerrain) {
        if (shownTerrain != terrain) {
            terrain = shownTerrain;
            clock = 0f;
            count = 0;
            int n = initialCount(terrain);
            for (int i = 0; i < n; i++) spawn(i, true);
            count = n;
        }
        if (count == 0) return;
        clock += delta;
        for (int i = 0; i < count; i++) step(i, delta);
    }

    private static int initialCount(int terrain) {
        switch (terrain) {
            case PBBattleTerrains.Grassy:   return 4;
            case PBBattleTerrains.Electric: return 4;
            case PBBattleTerrains.Misty:    return 4;
            case PBBattleTerrains.Psychic:  return 4;              // 2 hazes + 2 motes
            case PBBattleTerrains.BugLure:  return 4;
            case PBBattleTerrains.Cold:     return 4;              // 2 mists + 2 glints
            default: return 0;
        }
    }

    private float rnd(float from, float to) { return from + random.nextFloat() * (to - from); }

    /** (Re)starts particle {@code i}; {@code anywhere} scatters it over the field (the first fill). */
    private void spawn(int i, boolean anywhere) {
        rot[i] = rnd(0f, 360f);
        phase[i] = rnd(0f, MathUtils.PI2);
        life[i] = 0f;
        switch (terrain) {
            case PBBattleTerrains.Grassy:                          // a leaf blowing left to right
                x[i] = anywhere ? rnd(-20f, fieldW) : rnd(-40f, -10f);
                y[i] = rnd(10f, fieldH - 10f);
                vx[i] = rnd(28f, 55f);
                vy[i] = rnd(-4f, 10f);
                spin[i] = rnd(-180f, 180f);
                size[i] = rnd(1.4f, 1.9f);
                break;
            case PBBattleTerrains.Misty:                           // a petal drifting down
                x[i] = rnd(-20f, fieldW + 20f);
                y[i] = anywhere ? rnd(-10f, fieldH) : rnd(-30f, -10f);
                vx[i] = rnd(-8f, 16f);
                vy[i] = rnd(16f, 30f);
                spin[i] = rnd(-150f, 150f);
                size[i] = rnd(1.3f, 1.8f);
                break;
            case PBBattleTerrains.Electric:                        // a white arc lying on the ground, fading in and out
                kind[i] = random.nextInt(4);
                x[i] = rnd(30f, fieldW - 30f);
                y[i] = rnd(fieldH * GROUND_TOP + 10f, fieldH - 10f);
                span[i] = rnd(0.9f, 1.5f);
                life[i] = anywhere ? -rnd(0f, 1.6f) : -rnd(0.2f, 1.2f);   // negative = still waiting
                size[i] = rnd(1.4f, 2.0f);
                rot[i] = rnd(-6f, 6f);
                break;
            case PBBattleTerrains.Psychic:
                if (i < 2) {                                       // a large soft haze, slowly wandering
                    kind[i] = -1;
                    x[i] = rnd(-40f, fieldW + 40f);
                    y[i] = rnd(0f, fieldH);
                    vx[i] = rnd(-10f, 10f);
                    vy[i] = rnd(-6f, 6f);
                    size[i] = rnd(2.4f, 3.8f);
                } else {                                           // a mote rising and twinkling
                    kind[i] = 1;
                    x[i] = rnd(0f, fieldW);
                    y[i] = anywhere ? rnd(0f, fieldH) : fieldH + rnd(0f, 20f);
                    vx[i] = rnd(-6f, 6f);
                    vy[i] = rnd(-22f, -10f);
                    size[i] = rnd(1.4f, 2.2f);
                }
                break;
            case PBBattleTerrains.BugLure:                         // fireflies wandering and blinking
                x[i] = rnd(0f, fieldW);
                y[i] = rnd(20f, fieldH - 10f);
                float heading = rnd(0f, MathUtils.PI2), speed = rnd(10f, 24f);
                vx[i] = MathUtils.cos(heading) * speed;
                vy[i] = MathUtils.sin(heading) * speed;
                spin[i] = rnd(1.5f, 3.5f);                         // blink speed
                size[i] = rnd(1.0f, 1.4f);
                break;
            case PBBattleTerrains.Cold:
                if (i < 2) {                                       // ground mist flowing to the left
                    kind[i] = -1;
                    x[i] = anywhere ? rnd(-100f, fieldW + 60f) : fieldW + rnd(40f, 120f);
                    y[i] = rnd(fieldH * 0.40f, fieldH - 10f);
                    vx[i] = rnd(-22f, -10f);
                    size[i] = rnd(1.8f, 2.8f);
                    rot[i] = 0f;                                   // flat along the ground
                } else {                                           // an ice glint
                    kind[i] = 1;
                    x[i] = rnd(0f, fieldW);
                    y[i] = rnd(10f, fieldH - 10f);
                    span[i] = rnd(0.6f, 1.2f);
                    life[i] = anywhere ? -rnd(0f, 2f) : -rnd(0.2f, 1.5f);
                    size[i] = rnd(1.6f, 2.6f);
                }
                break;
            default:
                break;
        }
    }

    private void step(int i, float dt) {
        switch (terrain) {
            case PBBattleTerrains.Grassy:
                x[i] += vx[i] * dt;
                y[i] += (vy[i] + MathUtils.sin(clock * 2.2f + phase[i]) * 18f) * dt;
                rot[i] += spin[i] * dt;
                if (x[i] > fieldW + 20f || y[i] < -20f || y[i] > fieldH + 20f) spawn(i, false);
                break;
            case PBBattleTerrains.Misty:
                x[i] += (vx[i] + MathUtils.sin(clock * 1.6f + phase[i]) * 16f) * dt;
                y[i] += vy[i] * dt;
                rot[i] += spin[i] * dt;
                if (y[i] > fieldH + 20f) spawn(i, false);
                break;
            case PBBattleTerrains.Electric:
                life[i] += dt;
                if (life[i] > span[i]) spawn(i, false);
                break;
            case PBBattleTerrains.Psychic:
                x[i] += (vx[i] + MathUtils.sin(clock * 0.7f + phase[i]) * 6f) * dt;
                y[i] += vy[i] * dt;
                if (kind[i] < 0) {
                    if (x[i] < -120f) x[i] = fieldW + 100f;
                    if (x[i] > fieldW + 120f) x[i] = -100f;
                    if (y[i] < -60f || y[i] > fieldH + 60f) vy[i] = -vy[i];
                } else if (y[i] < -10f) {
                    spawn(i, false);
                }
                break;
            case PBBattleTerrains.BugLure:
                float turn = MathUtils.sin(clock * 0.9f + phase[i] * 3f) * 1.6f * dt;
                float c = MathUtils.cos(turn), s = MathUtils.sin(turn);
                float nvx = vx[i] * c - vy[i] * s, nvy = vx[i] * s + vy[i] * c;
                vx[i] = nvx;
                vy[i] = nvy;
                x[i] += vx[i] * dt;
                y[i] += vy[i] * dt;
                if (x[i] < 0f || x[i] > fieldW) vx[i] = -vx[i];
                if (y[i] < 10f || y[i] > fieldH - 6f) vy[i] = -vy[i];
                break;
            case PBBattleTerrains.Cold:
                if (kind[i] < 0) {
                    x[i] += vx[i] * dt;
                    if (x[i] < -160f) spawn(i, false);
                } else {
                    life[i] += dt;
                    if (life[i] > span[i]) spawn(i, false);
                }
                break;
            default:
                break;
        }
    }

    /** Draws the particles at {@code level} (0-1, the terrain's fade); leaves the batch in normal blending. */
    void render(SpriteBatch batch, float screenH, float level) {
        if (count == 0 || level <= 0f) return;
        ensureBitmaps();
        additive = false;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < count; i++) {
            switch (terrain) {
                case PBBattleTerrains.Grassy:
                    draw(batch, false, leaf, i, size[i], screenH, 0.72f, 0.92f, 0.42f, 0.95f * level);
                    break;
                case PBBattleTerrains.Misty:
                    draw(batch, false, petal, i, size[i], screenH, 1f, 0.74f, 0.86f, 0.95f * level);
                    break;
                case PBBattleTerrains.Electric:
                    if (life[i] > 0f) {
                        // fades in, holds solid white through the middle, fades out
                        float a = Math.min(1f, MathUtils.sin(life[i] / span[i] * MathUtils.PI) * 1.6f);
                        draw(batch, true, mist, i, size[i] * 0.8f, screenH, 1f, 1f, 1f, 0.28f * a * level);
                        draw(batch, false, bolts[kind[i]], i, size[i], screenH, 1f, 1f, 1f, a * level);
                    }
                    break;
                case PBBattleTerrains.Psychic:
                    if (kind[i] < 0) {
                        float breathe = 0.5f + 0.5f * MathUtils.sin(clock * 0.8f + phase[i]);
                        draw(batch, false, blob, i, size[i], screenH, 0.62f, 0.30f, 0.95f, (0.14f + 0.10f * breathe) * level);
                    } else {
                        float twinkle = 0.3f + 0.7f * Math.max(0f, MathUtils.sin(clock * 3f + phase[i]));
                        draw(batch, true, glow, i, size[i] * 1.6f, screenH, 0.8f, 0.4f, 1f, 0.5f * twinkle * level);
                        draw(batch, false, sparkle, i, size[i], screenH, 0.93f, 0.82f, 1f, twinkle * level);
                    }
                    break;
                case PBBattleTerrains.BugLure:
                    float on = Math.max(0f, MathUtils.sin(clock * spin[i] + phase[i]));
                    float bright = 0.12f + 0.88f * on * on;
                    draw(batch, true, glow, i, size[i] * 3.2f, screenH, 0.7f, 1f, 0.3f, 0.7f * bright * level);
                    draw(batch, false, spark, i, size[i] * 1.6f, screenH, 0.95f, 1f, 0.55f, bright * level);
                    break;
                case PBBattleTerrains.Cold:
                    if (kind[i] < 0) {
                        draw(batch, false, mist, i, size[i], screenH, 0.92f, 0.97f, 1f, 0.40f * level);
                    } else if (life[i] > 0f) {
                        float t = life[i] / span[i];
                        float a = MathUtils.sin(t * MathUtils.PI);
                        draw(batch, true, glow, i, size[i] * 1.8f, screenH, 0.6f, 0.85f, 1f, 0.5f * a * level);
                        draw(batch, false, sparkle, i, size[i], screenH, 0.95f, 0.99f, 1f, a * level);
                    }
                    break;
                default:
                    break;
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /** Whether the batch is in additive blending right now (inside {@link #render}). */
    private boolean additive;

    /** A bitmap centred on the particle at {@code scale}, turned by its rotation; additive for glows. */
    private void draw(SpriteBatch batch, boolean add, Texture texture, int i, float scale, float screenH,
                      float r, float g, float b, float a) {
        if (a <= 0f) return;
        if (add != additive) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, add ? GL20.GL_ONE : GL20.GL_ONE_MINUS_SRC_ALPHA);
            additive = add;
        }
        int tw = texture.getWidth(), th = texture.getHeight();
        batch.setColor(r, g, b, Math.min(1f, a));
        batch.draw(texture, x[i] - tw / 2f, screenH - y[i] - th / 2f, tw / 2f, th / 2f, tw, th,
                scale, scale, rot[i], 0, 0, tw, th, false, false);
    }

    // ------------------------------------------------------------------ bitmaps

    private void ensureBitmaps() {
        if (bolts != null) return;
        Random shapes = new Random(7);                             // the same four bolts every battle
        bolts = new Texture[4];
        for (int i = 0; i < bolts.length; i++) bolts[i] = bolt(shapes);
        spark = texture(plus());
        blob = texture(radial(64, 64, 2f));
        glow = texture(radialCore(12));
        sparkle = texture(star());
        mist = texture(radial(96, 32, 1.6f));
        leaf = texture(leafShape(12, 6));
        petal = texture(petalShape(8, 9));
    }

    /** A solid white leaf - an oval pointed at both ends - with a soft one-pixel edge (tinted when drawn). */
    private static Pixmap leafShape(int w, int h) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int yy = 0; yy < h; yy++)
            for (int xx = 0; xx < w; xx++) {
                float u = (xx + 0.5f) / w * 2f - 1f, v = Math.abs((yy + 0.5f) / h * 2f - 1f);
                float half = (float) Math.pow(Math.max(0f, 1f - u * u), 0.8f);   // half width along the leaf
                p.drawPixel(xx, yy, 0xFFFFFF00 | softEdge((half - v) * h / 2f));
            }
        return p;
    }

    /** A solid white petal - rounder at the tip, narrowing to the base - with a soft one-pixel edge. */
    private static Pixmap petalShape(int w, int h) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int yy = 0; yy < h; yy++)
            for (int xx = 0; xx < w; xx++) {
                float u = Math.abs((xx + 0.5f) / w * 2f - 1f), v = (yy + 0.5f) / h * 2f - 1f;
                float half = (float) Math.sqrt(Math.max(0f, 1f - v * v)) * (0.8f - 0.25f * v);
                p.drawPixel(xx, yy, 0xFFFFFF00 | softEdge((half - u) * w / 2f));
            }
        return p;
    }

    /** Alpha for a pixel {@code inside} pixels within a shape's edge: solid inside, fading over the last ~1.5 px. */
    private static int softEdge(float inside) {
        return Math.round(255f * Math.max(0f, Math.min(1f, (inside + 0.5f) / 1.5f)));
    }

    private static Texture texture(Pixmap pixmap) {
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    /** A ground arc, left to right: solid white, 1 px thick, zigzagging a few pixels up and down. */
    private static Texture bolt(Random shapes) {
        int w = 40, h = 12;
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        int by = h / 2;
        for (int xx = 0; xx < w; xx++) {
            int from = by;
            if (xx % 3 == 0) by = Math.max(1, Math.min(h - 2, by + shapes.nextInt(5) - 2));
            for (int yy = Math.min(from, by); yy <= Math.max(from, by); yy++) p.drawPixel(xx, yy, 0xFFFFFFFF);
        }
        Texture texture = new Texture(p);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);   // crisp, no soft edge
        p.dispose();
        return texture;
    }

    /** A 3x3 plus. */
    private static Pixmap plus() {
        Pixmap p = new Pixmap(3, 3, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        p.drawPixel(1, 0, 0xFFFFFFFF);
        p.drawPixel(0, 1, 0xFFFFFFFF);
        p.drawPixel(1, 1, 0xFFFFFFFF);
        p.drawPixel(2, 1, 0xFFFFFFFF);
        p.drawPixel(1, 2, 0xFFFFFFFF);
        return p;
    }

    /** A 7x7 four-pointed glint. */
    private static Pixmap star() {
        Pixmap p = new Pixmap(7, 7, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int d = 0; d < 7; d++) {
            int alpha = d == 3 ? 255 : (d == 2 || d == 4) ? 190 : (d == 1 || d == 5) ? 110 : 50;
            p.drawPixel(3, d, 0xFFFFFF00 | alpha);
            p.drawPixel(d, 3, 0xFFFFFF00 | alpha);
        }
        return p;
    }

    /** A white ellipse fading out from the centre ({@code power} sharpens the falloff). */
    private static Pixmap radial(int w, int h, float power) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int yy = 0; yy < h; yy++)
            for (int xx = 0; xx < w; xx++) {
                float dx = (xx + 0.5f - w / 2f) / (w / 2f), dy = (yy + 0.5f - h / 2f) / (h / 2f);
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                int alpha = d >= 1f ? 0 : Math.round(255f * (float) Math.pow(1f - d, power));
                p.drawPixel(xx, yy, 0xFFFFFF00 | alpha);
            }
        return p;
    }

    /** A small glow with a solid bright core (fireflies, motes). */
    private static Pixmap radialCore(int s) {
        Pixmap p = radial(s, s, 1.8f);
        int c = s / 2;
        p.drawPixel(c, c, 0xFFFFFFFF);
        p.drawPixel(c - 1, c, 0xFFFFFFFF);
        p.drawPixel(c, c - 1, 0xFFFFFFFF);
        p.drawPixel(c - 1, c - 1, 0xFFFFFFFF);
        return p;
    }

    void dispose() {
        if (bolts == null) return;
        for (Texture t : bolts) t.dispose();
        spark.dispose();
        blob.dispose();
        glow.dispose();
        sparkle.dispose();
        mist.dispose();
        leaf.dispose();
        petal.dispose();
        bolts = null;
    }
}
