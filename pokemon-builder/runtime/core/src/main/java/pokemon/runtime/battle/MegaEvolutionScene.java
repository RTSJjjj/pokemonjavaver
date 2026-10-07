package pokemon.runtime.battle;

import pokemon.runtime.pokemon.Pokemon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * {@code BaseMegaEvolutionScene} (Mega evolution:9-343) with {@code SceneMegaEvolution},
 * {@code ScenePrimalred} and {@code ScenePrimalblue} (:345-361): the full Mega Evolution scene,
 * played as {@code megascene.start(...); <form change>; megascene.endScene} (:410-417).
 *
 * <p>The plugin's code is a sequence of {@code pbWait(n)} calls; here it becomes a list of
 * frames (one per 40 fps tick) built once, in the plugin's order. Every sprite is an IMAGE
 * sprite of the battle sprite table with a z above the battle's own (the scene's viewport is
 * z 99999), so the table's renderer draws position, origin, zoom, angle, opacity and the
 * {@code sprite.color} blend exactly as for the battle sprites.</p>
 *
 * <p>登记: {@code restoreColor} (:299-303) only assigns its own locals and restores nothing; the
 * scene is transcribed as written, i.e. the "restored" colours are never applied.</p>
 */
final class MegaEvolutionScene {

    /** The scene's access to the battle screen. */
    interface Host {
        BattleSprite addSprite(String id, float x, float y, String file);

        void removeSprite(String id);

        boolean bitmapExists(String file);

        int[] bitmapSizeOf(String file);

        /** {@code pbLoadPokemonBitmapSpecies(pkmn, species)} of the Pokemon in the given form: the logical file. */
        String pokemonFile(Pokemon pkmn, int form);

        void playCry(Pokemon pkmn);

        /** {@code pbBGMPlay(name)}. */
        void playBgm(String name);

        /** {@code $game_system.getPlayingBGM}. */
        String playingBgm();

        int time();

        String backdrop();
    }

    private static final String DIR = "Graphics/Pictures/Mega Evolution/";
    private static final int Z = 99000;

    private final Host host;
    private final Pokemon pkmn;
    private final int sceneKind;
    private final int oldForm;
    private final int newForm;
    private final List<Runnable> frames = new ArrayList<>();
    private final Random rng = new Random();
    private final List<String> created = new ArrayList<>();
    private int nextFrame;
    private int zOrder;
    private String previousBgm;

    // the plugin's @sprites
    private BattleSprite bg;
    private BattleSprite base;
    private BattleSprite pkmnSprite;
    private BattleSprite circle;
    private BattleSprite light;
    private BattleSprite circle2;
    private BattleSprite scene;
    private BattleSprite light2;
    private BattleSprite icon;
    private final BattleSprite[] bubble = new BattleSprite[16];
    private final float[] bubbleX = new float[16];
    private final float[] bubbleY = new float[16];
    private float middlex;
    private float middley;
    private int circle2Width;

    MegaEvolutionScene(Host host, Pokemon pkmn, int sceneKind, int oldForm, int newForm) {
        this.host = host;
        this.pkmn = pkmn;
        this.sceneKind = sceneKind;
        this.oldForm = oldForm;
        this.newForm = newForm;
        build();
    }

    boolean done() {
        return nextFrame >= frames.size();
    }

    /** One 40 fps tick. */
    void tick() {
        if (nextFrame < frames.size()) {
            frames.get(nextFrame++).run();
        }
        if (nextFrame >= frames.size() && !created.isEmpty()) {
            dispose();
        }
    }

    void dispose() {
        for (String key : created) host.removeSprite(key);
        created.clear();
    }

    // ------------------------------------------------------------------
    // the subclasses' configuration (:18-21, :346-361)
    // ------------------------------------------------------------------

    private String iconFilename() {
        return sceneKind == 1 ? "icon_primal_Groudon" : sceneKind == 2 ? "icon_primal_Kyogre" : "icon_mega";
    }

    private String iconDir() {
        return "Graphics/Pictures/Battle/";
    }

    private String circle2Filename() {
        return sceneKind == 1 ? "Circle 2_Red" : sceneKind == 2 ? "Circle 2_Blue" : "Circle 2";
    }

    private String bgmName() {
        return "Mega evolution";                                                   // :21
    }

    // ------------------------------------------------------------------
    // timeline helpers: each plugin statement becomes a frame action, pbWait(n) is n empty frames
    // ------------------------------------------------------------------

    /** An action that takes no time of its own: it runs on the first frame of the next wait. */
    private final List<Runnable> pending = new ArrayList<>();

    private void now(Runnable action) {
        pending.add(action);
    }

    /** {@code pbWait(n)}: n frames; the actions queued by {@link #now} run on the first of them. */
    private void wait(int n) {
        List<Runnable> actions = new ArrayList<>(pending);
        pending.clear();
        if (n <= 0) {
            if (!actions.isEmpty()) frames.add(() -> { for (Runnable a : actions) a.run(); });
            return;
        }
        frames.add(() -> { for (Runnable a : actions) a.run(); });
        for (int i = 1; i < n; i++) frames.add(() -> { });
    }

    /** {@code N.times { body; pbWait(w) }}. */
    private void times(int n, int w, Runnable body) {
        for (int i = 0; i < n; i++) {
            now(body);
            wait(w);
        }
    }

    private BattleSprite create(String key, String file) {
        BattleSprite s = host.addSprite(key, 0, 0, file);
        s.z = Z + zOrder++;
        created.add(key);
        return s;
    }

    private static void setColor(BattleSprite s, float r, float g, float b, float a) {
        s.color[0] = r;
        s.color[1] = g;
        s.color[2] = b;
        s.color[3] = a;
    }

    // ------------------------------------------------------------------
    // start(time, backdrop, pkmn) :23-198, then megaEvolve :200-229 (called by endScene :339-342)
    // ------------------------------------------------------------------

    private void build() {
        final float w = PictureEx.Graphics.WIDTH;
        final float h = PictureEx.Graphics.HEIGHT;
        middlex = w / 2f;                                                          // :40
        middley = h / 2f;                                                          // :41

        // :24-25
        now(() -> {
            previousBgm = host.playingBgm();
            host.playBgm(bgmName());
        });

        // :28-38 backdrop file
        now(() -> {
            String backdropFilename = host.backdrop();                             // :34
            String time = host.time() == 1 ? "eve" : host.time() == 2 ? "night" : null;   // :29-32
            if (time != null) {                                                    // :35
                String trialName = backdropFilename + "_" + time;                  // :36
                if (host.bitmapExists("Graphics/Battlebacks/" + trialName + "_bg")) backdropFilename = trialName;   // :37
            }
            final String file = "Graphics/Battlebacks/" + backdropFilename + "_bg";
            // 背景
            bg = create("mega_bg", file);                                          // :44
            bg.zoomX = 1f;                                                         // :45
            bg.zoomY = 1.35f;
            // 基础图层
            base = create("mega_base", DIR + "Base");                              // :48
            base.opacity = 0;                                                      // :49
            // :52-55 saveColor; :58-60 white screen
            setColor(bg, 255, 255, 255, 0);
            sceneBackdropFile = file;
        });
        // :61-62 17.times { alpha += 15; pbWait(1) }; pbWait(5)
        times(17, 1, () -> bg.color[3] += 15);
        wait(5);
        // :65-67 restoreColor does nothing (class javadoc); 17.times { alpha -= 15; pbWait(1) }; pbWait(2)
        times(17, 1, () -> bg.color[3] -= 15);
        wait(2);

        // :70-77 宝可梦
        now(() -> {
            pkmnSprite = create("mega_pkmn", host.pokemonFile(pkmn, oldForm));     // :70-71
            pkmnSprite.ox = pkmnSprite.bitmapWidth / 2f;                           // :72-74
            pkmnSprite.oy = pkmnSprite.bitmapHeight / 2f;
            pkmnSprite.x = middlex;                                                // :75
            pkmnSprite.y = middley;
            host.playCry(pkmn);                                                    // :76
        });
        wait(30);                                                                  // :77

        // :80 气泡效果
        now(this::setPositionEffectBubble);

        // :83-93 红色屏幕
        now(() -> setColor(bg, 235, 0, 0, bg.color[3]));
        times(17, 1, () -> {
            bg.color[3] += 12;                                                     // :87
            base.opacity += 15;                                                    // :88
            pkmnSprite.color[3] += 15;                                             // :89
        });
        wait(2);                                                                   // :92

        // :95-100 圆环
        now(() -> {
            circle = create("mega_circle", DIR + "Circle");
            circle.ox = circle.bitmapWidth / 2f;
            circle.oy = circle.bitmapHeight / 2f;
            circle.x = middlex;
            circle.y = middley;
            circle.zoomX = 0f;
            circle.zoomY = 0f;
        });

        // :103-106 气泡移动 (each call blocks)
        int[][] order = {{0, 0}, {4, 1}, {8, 2}, {12, 3}, {1, 0}, {5, 1}, {9, 2}, {13, 3},
                {2, 0}, {6, 1}, {10, 2}, {14, 3}, {3, 0}, {7, 1}, {11, 2}, {15, 3}};
        for (int[] call : order) setEffectBubble(call[0], call[1]);

        // :109-110 圆环缩放动画
        final float[] zoom = {0f};
        times(15, 2, () -> {
            zoom[0] += 0.2f;
            circle.zoomX = zoom[0];
            circle.zoomY = zoom[0];
        });

        // :113-124 设置宝可梦白色
        now(() -> {
            setColor(pkmnSprite, 255, 255, 255, 255);
            circle.visible = false;                                                // :123 set_visible_sprite("circle")
        });
        wait(1);                                                                   // :124

        // :127-132 光线
        now(() -> {
            light = create("mega_light", DIR + "Light");
            light.srcWidth = light.bitmapWidth / 3;                                // :128-130
            light.srcHeight = light.bitmapHeight;
            light.srcX = 0;                                                        // :131
            light.srcY = 0;
            light.visible = false;                                                 // :132
        });

        // :135-154 第二圆环
        now(() -> {
            circle2 = create("mega_circle2", DIR + circle2Filename());             // :135
            circle2Width = circle2.bitmapWidth;
            circle2.srcWidth = circle2.bitmapWidth / 3;                            // :136-138
            circle2.srcHeight = circle2.bitmapHeight;
            circle2.srcX = 0;                                                      // :139
            circle2.srcY = 0;
            circle2.ox = circle2.srcWidth / 2f;                                    // :140
            circle2.oy = circle2.srcHeight / 2f;
            circle2.x = middlex;                                                   // :141
            circle2.y = middley;
            setColor(circle2, 255, 255, 255, 255);                                 // :150-153
        });
        wait(1);                                                                   // :154

        // :157-175 第二圆环动画
        final float[] z2 = {1f};
        final int[] mul = {0};
        final int[] inc = {0};
        for (int i = 0; i < 9; i++) {
            now(() -> {
                mul[0] += 1;                                                       // :161
                z2[0] += 0.1f;                                                     // :162
                circle2.zoomX = z2[0];                                             // :163
                circle2.zoomY = z2[0];
                if (mul[0] >= 7) {                                                 // :164
                    circle2.color[3] = 0;                                          // :165-166 (restoreColor does nothing)
                    circle2.srcX = circle2Width / 3 * inc[0];                      // :167-168
                    light.visible = true;                                          // :169
                    light.srcX = light.bitmapWidth / 3 * inc[0];                   // :170-171
                    inc[0] += 1;                                                   // :172
                }
            });
            wait(5);                                                               // :174
        }

        // :178-188 新场景 / 恢复宝可梦颜色
        now(() -> {
            scene = create("mega_scene", sceneBackdropFile);                       // :178
            scene.zoomX = 1f;                                                      // :179
            scene.zoomY = 1.35f;
            setColor(scene, 255, 255, 255, 255);                                   // :180-183
            pkmnSprite.color[3] = 0;                                               // :186-187 (restoreColor does nothing)
            pkmnSprite.zoomX = 2f;                                                 // :188
            pkmnSprite.zoomY = 2f;
            bg.color[3] = 0;                                                       // :191-192
        });
        wait(10);                                                                  // :194
        now(() -> {
            circle2.visible = false;                                               // :195
            light.visible = false;                                                 // :196
            base.visible = false;                                                  // :197
        });

        // ---- endScene -> megaEvolve (:339-342, :200-229) ----
        now(() -> {
            // :202 重置形态
            BattleSprite fresh = pkmnSprite;
            fresh.name = host.pokemonFile(pkmn, newForm);
            int[] size = host.bitmapSizeOf(fresh.name);
            fresh.bitmapWidth = size == null ? -1 : size[0];
            fresh.bitmapHeight = size == null ? -1 : size[1];
        });
        times(17, 2, () -> scene.opacity -= 15);                                   // :203
        final float[] z3 = {2f};
        times(10, 2, () -> {                                                       // :205
            z3[0] -= 0.1f;
            pkmnSprite.zoomX = z3[0];
            pkmnSprite.zoomY = z3[0];
        });

        now(() -> {
            light2 = create("mega_light2", DIR + "Light 2");                       // :207
            light2.ox = light2.bitmapWidth / 2f;                                   // :208-210
            light2.oy = light2.bitmapHeight / 2f;
            light2.x = middlex;                                                    // :211
            light2.y = middley - 130f;
            icon = create("mega_icon", iconDir() + iconFilename());                // :213
            icon.ox = icon.bitmapWidth / 2f;                                       // :214-216
            icon.oy = icon.bitmapHeight / 2f;
            icon.x = middlex;                                                      // :217
            icon.y = middley - 130f;
            icon.zoomX = 1.5f;                                                     // :218
            icon.zoomY = 1.5f;
        });
        final int[] o = {0};
        times(10, 5, () -> {                                                       // :221
            light2.angle += 36;                                                    // :222
            if (o[0] == 5) host.playCry(pkmn);                                     // :223
            o[0] += 1;                                                             // :224
        });
        now(() -> {
            if (previousBgm != null) host.playBgm(previousBgm);                    // :228
        });
        wait(0);
    }

    private String sceneBackdropFile;

    /** {@code setPositionEffectBuble} (:237-258). */
    private void setPositionEffectBubble() {
        for (int i = 0; i < 16; i++) {
            if (bubble[i] == null) bubble[i] = create("mega_bubble_" + i, DIR + "Buble Pink");   // :239
            float xbb;
            float ybb;
            if (i <= 3) {                                                          // :241
                xbb = middlex / 2 - rng.nextInt(10);                               // :242
                ybb = middley / 2 - rng.nextInt(10);                               // :243
            } else if (i <= 7) {                                                   // :244
                xbb = middlex / 2 - rng.nextInt(10);                               // :245
                ybb = middley * 1.5f + rng.nextInt(10);                            // :246
            } else if (i <= 11) {                                                  // :247
                xbb = middlex * 1.5f + rng.nextInt(10);                            // :248
                ybb = middley / 2 - rng.nextInt(10);                               // :249
            } else {
                xbb = middlex * 1.5f + rng.nextInt(10);                            // :251
                ybb = middley * 1.5f + rng.nextInt(10);                            // :252
            }
            bubbleX[i] = xbb;                                                      // :254
            bubbleY[i] = ybb;
            bubble[i].x = xbb;                                                     // :255
            bubble[i].y = ybb;
            bubble[i].visible = false;                                             // :256
        }
    }

    /** {@code setEffectBuble(number, calc)} (:260-287): five 2-frame moves, then hidden, then pbWait(1). */
    private void setEffectBubble(final int number, final int calc) {
        now(() -> bubble[number].visible = true);                            // :261
        for (int k = 0; k < 5; k++) {                                              // :262
            now(() -> {
                float xch = bubbleX[number];                                       // :263
                float ych = bubbleY[number];                                       // :264
                int xxx = (int) Math.floor((middlex - xch) / 5);                   // :265
                int yyy = (int) Math.floor((middley - ych) / 5);                   // :266
                int xxx2 = (int) Math.floor((xch - middlex) / 5);                  // :267
                int yyy2 = (int) Math.floor((ych - middley) / 5);                  // :268
                BattleSprite b = bubble[number];
                switch (calc) {                                                    // :269
                    case 0:
                        b.x += xxx;                                                // :271
                        b.y += yyy;                                                // :272
                        break;
                    case 1:
                        b.x += xxx;                                                // :275
                        b.y -= yyy2;                                               // :276
                        break;
                    case 2:
                        b.x -= xxx2;                                               // :279
                        b.y += yyy;                                                // :278
                        break;
                    default:
                        b.x -= xxx2;                                               // :280
                        b.y -= yyy2;                                               // :281
                        break;
                }
            });
            wait(2);                                                               // :283
        }
        now(() -> bubble[number].visible = false);                           // :285
        wait(1);                                                                   // :286
    }
}
