package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.battle.BattleSpriteShader;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.ui.WindowSkin;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * 225_PScreen_EggHatching: {@code pbHatchAnimation(pokemon)} - the "Huh?" message, {@code pbFadeOutInWithMusic} around
 * {@code PokemonEggHatch_Scene} ({@code pbStartScene}, {@code pbMain}, {@code pbEndScene}). Everything counts in 40 fps ticks
 * ({@code updateScene(frames)}), so the timeline is built once as a list of frames.
 *
 * <p>登记: {@code pbApplyBattlerMetricsToSprite} (the hatched sprite's metric offsets) and the nickname screen's Pokemon icon
 * are not drawn; the "Huh?" message is drawn over whatever the host shows below.</p>
 */
public final class HatchSceneView {
    private enum Phase { PRE_MESSAGE, OUTER_OUT, SPRITES_IN, TIMELINE, MESSAGE, ENTRY, SPRITES_OUT, OUTER_IN, DONE }

    /** One entry of the timeline: runs on its tick; {@code instant} entries cost no time. */
    private static final class Frame {
        final Runnable action;
        final boolean instant;

        Frame(Runnable action, boolean instant) {
            this.action = action;
            this.instant = instant;
        }
    }

    private static final int MAX_POKEMON_NAME_SIZE = 10;                       // 252:StorageView uses the same

    private final RuntimeContext context;
    private final Pokemon pokemon;
    private final MenuClock clock = new MenuClock();
    private final SceneMessage preMessage;
    private final PbMessage pbMessage;
    private final BattleSpriteShader shader = new BattleSpriteShader();
    private final Deque<Frame> timeline = new ArrayDeque<>();
    private Phase phase = Phase.PRE_MESSAGE;
    private int tick;
    private float blackAlpha;
    private boolean finished;
    private boolean nicknamed;
    private boolean assetsLoaded;
    private Texture background;
    private Texture eggTexture;
    private Texture crackTexture;
    private Texture pokemonTexture;
    private TextEntryView entry;

    // the sprites of pbStartScene
    private float spriteX;
    private float spriteY = 264f + 56f;                                         // :21 56 to offset the egg sprite
    private boolean showHatchedSprite;
    private boolean hatchVisible;
    private int hatchIndex;
    private float tone;
    private float overlayAlpha;

    public HatchSceneView(RuntimeContext context, Pokemon pokemon) {
        this.context = context;
        this.pokemon = pokemon;
        this.preMessage = new SceneMessage(context);
        this.pbMessage = new PbMessage(context);
        this.spriteX = ScreenMetrics.logicalWidth() / 2f;                      // :17-18
        preMessage.show("Huh?" + SceneMessage.PAUSE, () -> {                    // :183 pbMessage(_INTL("Huh?\1"))
            phase = Phase.OUTER_OUT;
            tick = 0;
            preMessage.clear();
            if (audio() != null) audio().memorizeBgmAndBgs();                   // pbFadeOutInWithMusic
        });
        buildTimeline();
    }

    private AudioManager audio() {
        return context.audioManager();
    }

    /** True while the screen below (the host's) is what the player sees: the "Huh?" and the two outer fades. */
    public boolean hostVisible() {
        return phase == Phase.PRE_MESSAGE || phase == Phase.OUTER_OUT || phase == Phase.OUTER_IN || phase == Phase.DONE;
    }

    public boolean finished() {
        return finished;
    }

    // =====================================================================
    // The timeline of pbMain (:59-111)
    // =====================================================================

    private void act(Runnable action) {
        timeline.add(new Frame(action, true));
    }

    private void wait(int ticks) {                                              // updateScene(frames)
        for (int i = 0; i < ticks; i++) timeline.add(new Frame(() -> { }, false));
    }

    private void se(String name) {
        act(() -> {
            if (audio() != null) audio().playSe(name, 100, 100);
        });
    }

    private void mask(int index) {                                              // pbPositionHatchMask
        act(() -> hatchIndex = index);
    }

    /** {@code swingEgg(speed, swingTimes)} (:129-149): the x of the egg for each tick. */
    private void swingEgg(float speedArg, int swingTimes) {
        act(() -> hatchVisible = true);
        float speed = speedArg * 40f / 20f;                                     // :131 speed.to_f*Graphics.frame_rate/20
        float amplitude = 8f;
        float center = ScreenMetrics.logicalWidth() / 2f;
        float[] targets = new float[swingTimes * 2 + 1];
        for (int i = 0; i < swingTimes; i++) {                                  // :134-137
            targets[i * 2] = center + amplitude;
            targets[i * 2 + 1] = center - amplitude;
        }
        targets[targets.length - 1] = center;                                   // :138
        float x = center;
        for (int i = 0; i < targets.length; i++) {                              // :139-147
            while (true) {
                if (i % 2 == 0 && x >= targets[i]) break;
                if (i % 2 == 1 && x <= targets[i]) break;
                x += speed;
                final float at = x;
                timeline.add(new Frame(() -> spriteX = at, false));
            }
            speed *= -1;
        }
        final float end = targets[targets.length - 1];
        act(() -> spriteX = end);                                               // :148-149
    }

    private void buildTimeline() {
        act(() -> {
            if (audio() != null) audio().playBgm("Evolution");                  // :60 pbBGMPlay("Evolution")
        });
        wait(40 * 15 / 10);                                                     // :62
        mask(0);
        se("Battle ball shake");
        swingEgg(4, 1);
        wait(40 * 2 / 10);                                                      // :66
        mask(1);
        se("Battle ball shake");
        swingEgg(4, 1);
        wait(40 * 4 / 10);                                                      // :70
        mask(2);
        se("Battle ball shake");
        swingEgg(8, 2);
        wait(40 * 4 / 10);                                                      // :74
        mask(3);
        se("Battle ball shake");
        swingEgg(16, 4);
        wait(40 * 2 / 10);                                                      // :78
        mask(4);
        se("Battle recall");
        int fadeTime = 40 * 4 / 10;                                             // :82
        int toneDiff = (int) Math.ceil(255.0 / fadeTime);                       // :83
        for (int i = 1; i <= fadeTime; i++) {                                   // :84-88
            final int step = i * toneDiff;
            timeline.add(new Frame(() -> {
                tone = step;
                overlayAlpha = step;
            }, false));
        }
        wait(40 * 3 / 4);                                                       // :89
        act(() -> {                                                             // :90-94
            showHatchedSprite = true;
            spriteX = ScreenMetrics.logicalWidth() / 2f;
            spriteY = 264f;
            hatchVisible = false;
        });
        for (int i = 1; i <= fadeTime; i++) {                                   // :95-99
            final int step = 255 - i * toneDiff;
            timeline.add(new Frame(() -> {
                tone = step;
                overlayAlpha = step;
            }, false));
        }
        act(() -> {                                                             // :100-101
            tone = 0f;
            overlayAlpha = 0f;
        });
        act(() -> {                                                             // :103-105
            String cry = pokemon.species == null || audio() == null ? null
                    : audio().resolveCryFile(pokemon.species.id, pokemon.species.internalName, pokemon.formIndex());
            float playtime = cry == null ? 0f : Math.max(0f, audio().sePlayTime(cry));
            int frames = (int) Math.ceil(playtime * 40.0) + 4;                  // pbCryFrameLength
            if (audio() != null) audio().playCry(pokemon.species == null ? 0 : pokemon.species.id);   // pbPlayCry
            for (int i = 0; i < frames; i++) timeline.addFirst(new Frame(() -> { }, false));
        });
        act(() -> {                                                             // :106-107
            if (audio() != null) {
                audio().stopBgm();
                audio().playMe("Evolution success");
            }
            phase = Phase.MESSAGE;
            pbMessage.start("\\se[]" + pokemon.name + "从蛋中孵化出来了！\\wt[80]", null, 0, 0, ignored ->   // :108
                    pbMessage.start("要给孵出来的" + pokemon.name + "\n起名字吗？", Arrays.asList("是", "否"), 2, 0, index -> {   // :109-110
                        if (index == 0) {
                            entry = new TextEntryView(context, pokemon.name + "的昵称是？", 0, MAX_POKEMON_NAME_SIZE, "", null);   // :111-112
                            phase = Phase.ENTRY;
                        } else {
                            endScene();
                        }
                    }));
        });
    }

    // =====================================================================
    // Update
    // =====================================================================

    /** @return true when the whole hatching is over */
    public boolean update(InputManager input) {
        if (phase == Phase.ENTRY) {
            if (entry != null && entry.update(input)) {
                String nickname = entry.result();
                entry.dispose();
                entry = null;
                if (nickname != null && !nickname.isEmpty()) pokemon.name = nickname;   // :113
                nicknamed = true;                                               // :114
                endScene();
            }
            return finished;
        }
        boolean confirm = input.wasPressed(GameAction.CONFIRM);
        boolean cancel = input.wasPressed(GameAction.CANCEL)
                || input.wasPressed(GameAction.MENU);
        int ticks = clock.advance();
        if (pbMessage.active()) {
            pbMessage.update(input, ticks);
            return finished;
        }
        if (phase == Phase.PRE_MESSAGE) {
            preMessage.input(confirm, cancel);
        }
        for (int i = 0; i < ticks && !finished && !pbMessage.active() && phase != Phase.ENTRY; i++) {
            step();
        }
        return finished;
    }

    private void step() {
        switch (phase) {
            case PRE_MESSAGE:
                preMessage.tick();
                break;
            case OUTER_OUT:
                blackAlpha = Math.min(255f, tick * 16f);
                if (++tick > 16) {
                    phase = Phase.SPRITES_IN;                                   // :55 pbFadeInAndShow
                    tick = 0;
                }
                break;
            case SPRITES_IN:
                blackAlpha = Math.max(0f, Math.min(255f, (16 - tick) * 16f));
                if (++tick > 16) {
                    phase = Phase.TIMELINE;
                }
                break;
            case TIMELINE:
                while (!timeline.isEmpty() && phase == Phase.TIMELINE) {
                    Frame frame = timeline.poll();
                    frame.action.run();
                    if (!frame.instant) break;
                }
                break;
            case SPRITES_OUT:                                                   // pbFadeOutAndHide
                blackAlpha = Math.max(0f, Math.min(255f, tick * 16f));
                if (++tick > 16) {
                    phase = Phase.OUTER_IN;
                    tick = 0;
                }
                break;
            case OUTER_IN:
                blackAlpha = Math.max(0f, Math.min(255f, (16 - tick) * 16f));
                if (++tick > 16) {
                    finish();
                }
                break;
            default:
                break;
        }
    }

    /** {@code pbEndScene} (:118-122): the fade out only when the Pokemon was not nicknamed (the name screen faded itself). */
    private void endScene() {
        phase = nicknamed ? Phase.OUTER_IN : Phase.SPRITES_OUT;
        blackAlpha = nicknamed ? 255f : 0f;
        tick = 0;
    }

    private void finish() {
        if (audio() != null) audio().restoreBgmAndBgs();
        shader.dispose();
        phase = Phase.DONE;
        finished = true;
    }

    // =====================================================================
    // Render
    // =====================================================================

    private Texture firstOf(MenuAssets a, String... names) {
        for (String name : names) {
            Texture t = a.graphic("Battlers", name);
            if (t != null) return t;
        }
        return null;
    }

    private void loadAssets(MenuAssets a) {
        if (assetsLoaded) return;
        assetsLoaded = true;
        background = a.graphic("Pictures", "hatchbg");                          // :20-21
        String name = pokemon.species == null ? "" : pokemon.species.internalName;
        int id = pokemon.species == null ? 0 : pokemon.species.id;
        int form = pokemon.formIndex();
        eggTexture = firstOf(a, name + "egg_" + form, String.format("%03degg_%d", id, form), name + "egg",   // 251:49-57
                String.format("%03degg", id), "egg");
        crackTexture = firstOf(a, name + "eggCracks", String.format("%03deggCracks", id), "eggCracks");   // :27-33
        pokemonTexture = EvolutionView.battler(a, pokemon.species, pokemon.shiny, form);
    }

    private WindowSkin speechFrame(MenuAssets a, WindowSkin fallback) {
        int index = context.settings().textskin;
        String name = index >= 0 && index < GameSettings.SPEECH_FRAMES.length ? GameSettings.SPEECH_FRAMES[index] : "speech bw 1";
        WindowSkin speech = a.skin(name);
        return speech != null ? speech : fallback;
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        if (phase == Phase.ENTRY && entry != null) {
            entry.render(b, a, f, skin);
            return;
        }
        if (phase == Phase.PRE_MESSAGE) {
            preMessage.draw(b, a, f, skin, w, h);
            return;
        }
        if (!hostVisible()) {
            loadAssets(a);
            drawScene(b, a, w, h);
            if (pbMessage.active()) pbMessage.render(b, a, f, skin, speechFrame(a, skin), w, h);
        }
        if (blackAlpha > 0f) {
            b.setColor(0f, 0f, 0f, Math.min(1f, blackAlpha / 255f));
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(Color.WHITE);
        }
    }

    private void drawScene(SpriteBatch b, MenuAssets a, float w, float h) {
        if (background != null) {
            b.draw(background, 0f, h - background.getHeight());
        } else {
            b.setColor(248f / 255f, 248f / 255f, 248f / 255f, 1f);
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(Color.WHITE);
        }
        Texture sprite = showHatchedSprite ? pokemonTexture : eggTexture;
        if (sprite != null) {
            boolean useShader = shader.isCompiled() && tone > 0f;
            if (useShader) {
                b.flush();
                b.setShader(shader.program());
                shader.setTone(new float[] {tone, tone, tone, 0f});
                shader.setColor(new float[] {0f, 0f, 0f, 0f});
            }
            b.draw(sprite, spriteX - sprite.getWidth() / 2f, h - spriteY);       // Bottom origin
            if (useShader) {
                b.flush();
                b.setShader(null);
            }
        }
        if (hatchVisible && crackTexture != null && !showHatchedSprite) {
            int part = crackTexture.getWidth() / 5;
            TextureRegion region = new TextureRegion(crackTexture, hatchIndex * part, 0, part, crackTexture.getHeight());
            b.draw(region, spriteX - part / 2f, h - spriteY);
        }
        if (overlayAlpha > 0f) {                                                // :35-40 the white flash overlay
            b.setColor(1f, 1f, 1f, Math.min(1f, overlayAlpha / 255f));
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(Color.WHITE);
        }
    }
}
