package pokemon.runtime.ui.menu;

import pokemon.runtime.pokemon.BattlerBitmaps;
import pokemon.runtime.pokemon.PokemonStats;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.battle.BattleSpriteShader;
import pokemon.runtime.field.EvolutionWorld;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PBEvolution;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.ui.WindowSkin;

import java.util.ArrayList;
import java.util.List;

/**
 * 226_PScreen_Evolution {@code PokemonEvolutionScene}: the evolution screen (pbStartScreen / pbEvolution /
 * pbEvolutionSuccess / pbEndScreen) and the metafile animation it plays (pbGenerateMetafiles).
 *
 * <p>The Ruby blocks on {@code Graphics.update}; every wait here is counted in the same 40 fps ticks ({@link MenuClock})
 * and the phases follow the source order. The two PokemonSprites are drawn white-blended through
 * {@link BattleSpriteShader} (sprite {@code color}, and the viewport {@code tone} of pbFlashInOut).</p>
 *
 * <p>登记: ①the pause arrow of {@code pbMessageWaitForInput(.., showPause=true)} and of the message window is not drawn;
 * ②the Pokemon's animated sprite ({@code pbUpdateSpriteHash}) stays a still picture, as does the animated plane of the
 * background; ③the move list used for learning on evolution is the species' own ({@code getMoveList}).</p>
 */
public final class EvolutionView {

    private static final int ROW = 32;
    private static final int BORDER = 32;

    private enum Phase { BLACK_IN, SPRITES_IN, MESSAGE, INTRO_WAIT, ANIM, FLASH_UP, FLASH_HOLD, FLASH_DOWN, CRY_WAIT,
        LEARN, SPRITES_OUT, BLACK_OUT, DONE }

    private final RuntimeContext context;
    private final Pokemon pokemon;
    private final PbsData.Species newSpecies;
    private final boolean canCancel;
    private final boolean ownsMusic;
    private final MenuClock clock = new MenuClock();
    private final BattleSpriteShader shader = new BattleSpriteShader();

    private Phase phase = Phase.BLACK_IN;
    private int tick;
    private boolean canceled;
    private boolean finished;
    private boolean assetsLoaded;
    private boolean flashPlayed;

    // sprites
    private Texture oldTexture;
    private Texture newTexture;
    private Texture background;
    private float zoom1 = 1f;
    private float zoom2 = 0f;
    private float alpha1;                      // sprite.color.alpha (white), 0..255
    private float alpha2 = 255f;
    private boolean visible1 = true;
    private boolean visible2 = true;
    private float opacity2;                    // rsprite2.opacity (0 until the metafile starts)
    private float tone;                        // @viewport.tone (r = g = b)
    private int bgY;
    private int bgH = -1;
    private float blackAlpha;                  // the two black fades (@myviewport.color, pbFadeInAndShow/OutAndHide)

    // the metafile frames
    private final List<float[]> frames = new ArrayList<>();
    private int frameIndex;

    // message window (pbCreateMessageWindow)
    private final SceneMessage message;
    private boolean messageWaitsInput;

    // forget screen of pbLearnMove
    private SummaryView forget;
    private java.util.function.IntConsumer forgetThen;

    private AudioManager audio() {
        return context.audioManager();
    }

    public EvolutionView(RuntimeContext context, Pokemon pokemon, PbsData.Species newSpecies, boolean canCancel) {
        this(context, pokemon, newSpecies, canCancel, true);
    }

    /**
     * @param ownsMusic true when this scene is {@code pbFadeOutInWithMusic}'s block itself (the party screen, a stone); the trade
     *                  scene runs it inside its own block and keeps the music state.
     */
    public EvolutionView(RuntimeContext context, Pokemon pokemon, PbsData.Species newSpecies, boolean canCancel,
                         boolean ownsMusic) {
        this.ownsMusic = ownsMusic;
        this.message = new SceneMessage(context);
        this.context = context;
        this.pokemon = pokemon;
        this.newSpecies = newSpecies;
        this.canCancel = canCancel;
        generateMetafiles();
        if (ownsMusic && audio() != null) {
            audio().memorizeBgmAndBgs();                  // pbFadeOutInWithMusic / the party's saved BGM
        }
    }

    /** True once the screen has closed. */
    public boolean finished() {
        return finished;
    }

    /** True while the screen below (the host's) is still what the player sees: the two black fades. */
    public boolean hostVisible() {
        return phase == Phase.BLACK_IN || phase == Phase.BLACK_OUT || phase == Phase.DONE;
    }

    /** True when the Pokemon evolved (false when the player cancelled). */
    public boolean evolved() {
        return finished && !canceled;
    }

    // =====================================================================
    // pbGenerateMetafiles (226:353-410)
    // =====================================================================

    /** One entry per {@code sprite.update}: {zoom of rsprite1, zoom of rsprite2, white alpha of rsprite1}. */
    private void generateMetafiles() {
        int alpha = 0;
        int alphaDiff = 10 * 20 / 40;                                            // :364
        while (true) {                                                           // :365-377
            // the metafile keeps the same Color object across pushes, so an entry shows the alpha of the next iteration
            frames.add(new float[] {1f, 0f, Math.min(255, alpha + alphaDiff)});
            if (alpha >= 255) {
                break;                                                           // :375
            }
            alpha += alphaDiff;                                                  // :376
        }
        int totaltempo = 0;
        int currenttempo = 25;                                                   // :379
        int maxtempo = 7 * 40;                                                   // :380
        while (totaltempo < maxtempo) {                                          // :381
            for (int j = 0; j < currenttempo; j++) {                             // :382
                frames.add(new float[] {Math.min(1.1f * (currenttempo - j - 1) / currenttempo, 1f),
                    Math.min(1.1f * (j + 1) / currenttempo, 1f), 255f});          // :391-392
            }
            totaltempo += currenttempo;                                          // :396
            if (totaltempo + currenttempo < maxtempo) {                          // :397
                for (int j = 0; j < currenttempo; j++) {                         // :398
                    frames.add(new float[] {Math.min(1.1f * (j + 1) / currenttempo, 1f),
                        Math.min(1.1f * (currenttempo - j - 1) / currenttempo, 1f), 255f});   // :399-400
                }
            }
            totaltempo += currenttempo;                                          // :405
            currenttempo = Math.max((int) Math.floor(currenttempo / 1.5), 5);    // :406
        }
    }

    // =====================================================================
    // Update
    // =====================================================================

    /** @return true once the screen has closed. */
    public boolean update(InputManager input) {
        if (forget != null) {
            if (forget.update(input)) {
                int result = forget.forgetResult();
                forget = null;
                java.util.function.IntConsumer then = forgetThen;
                forgetThen = null;
                if (then != null) then.accept(result);
            }
            return finished;
        }
        boolean confirm = input.wasPressed(GameAction.CONFIRM);
        boolean cancel = input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU);
        int ticks = clock.advance();
        // input is read once per rendered frame, like Input.trigger?
        if (phase == Phase.INTRO_WAIT) {                                         // pbMessageWaitForInput breaks on C / B
            if (confirm || cancel) {
                tick = 100;
            }
        } else {
            message.input(confirm, cancel);
        }
        if (phase == Phase.ANIM && cancel && canCancel && !canceled) {            // :573-577
            audio().stopBgm();
            MenuSe.close(audio());
            canceled = true;
            startFlash();
        }
        for (int i = 0; i < ticks && !finished; i++) {
            tick();
        }
        return finished;
    }

    private void tick() {
        switch (phase) {
            case BLACK_IN:                                                       // :496-499 16.times alpha += 16
                blackAlpha = Math.min(255f, blackAlpha + 16f);
                if (++tick >= 16) {
                    startSprites();
                }
                break;
            case SPRITES_IN: {                                                   // pbFadeInAndShow: j in 0..16
                int j = tick;
                blackAlpha = Math.max(0f, Math.min(255f, (16 - j) * 16f));
                if (++tick > 16) {
                    startIntro();
                }
                break;
            }
            case MESSAGE:
                message.tick();
                break;
            case INTRO_WAIT:                                                     // :559 pbMessageWaitForInput(50): 50*40/20 ticks
                message.tick();
                if (++tick >= 100) {
                    endIntroWait();
                }
                break;
            case ANIM:
                animTick();
                break;
            case FLASH_UP:                                                       // :444-454
                expandScreen();
                tone += 20 * 20 / 40;
                if (tone >= 255f) {
                    flashMiddle();
                }
                break;
            case FLASH_HOLD:                                                     // :473-476 (40/4).times
                if (++tick >= 10) {
                    phase = Phase.FLASH_DOWN;
                    tone = 255f;
                }
                break;
            case FLASH_DOWN:                                                     // :477-485
                tone -= 40 * 20 / 40;
                if (tone <= 0f) {
                    tone = 0f;
                    flashDone();
                }
                break;
            case CRY_WAIT:
                if (++tick >= cryFrames) {
                    cryDone();
                }
                break;
            case LEARN:
                message.tick();
                break;
            case SPRITES_OUT: {                                                  // pbFadeOutAndHide: j in 0..16
                int j = tick;
                blackAlpha = Math.max(0f, Math.min(255f, j * 16f));
                if (++tick > 16) {
                    phase = Phase.BLACK_OUT;
                    tick = 0;
                    blackAlpha = 255f;
                }
                break;
            }
            case BLACK_OUT:                                                      // :542-545 16.times alpha -= 16
                blackAlpha = Math.max(0f, blackAlpha - 16f);
                if (++tick >= 16) {
                    phase = Phase.DONE;
                    finished = true;
                    if (ownsMusic && audio() != null) {
                        audio().restoreBgmAndBgs();
                    }
                }
                break;
            default:
                break;
        }
    }

    // ---- phases ---------------------------------------------------------

    private void startSprites() {                                                // :501-530 the sprites exist, then fade in
        phase = Phase.SPRITES_IN;
        tick = 0;
    }

    private void startIntro() {                                                  // :551-558
        audio().stopBgm();                                                       // :555 pbBGMStop
        audio().playCry(pokemon.species == null ? 0 : pokemon.species.id);        // :556 pbPlayCry(@pokemon)
        showText("什么？\n" + pokemon.name + "正在进化！", () -> {
            phase = Phase.INTRO_WAIT;                                            // :559 pbMessageWaitForInput(.., 50, true)
            tick = 0;
            messageWaitsInput = true;
        });
    }

    private void endIntroWait() {
        phase = Phase.ANIM;
        messageWaitsInput = false;                                               // the text stays on the window
        MenuSe.decision(audio());                                                // :560 pbPlayDecisionSE
        audio().playMe("Evolution start");                                       // :563
        audio().playBgm("Evolution");                                            // :564
        frameIndex = 0;
        bgY = 0;
        bgH = (int) ScreenMetrics.logicalHeight();
        opacity2 = 255f;                                                         // metafile 2 pushes opacity 255
    }

    private void animTick() {
        narrowScreen();                                                          // :567
        if (frameIndex < frames.size()) {                                        // :568-569 metaplayer.update
            float[] frame = frames.get(frameIndex++);
            zoom1 = frame[0];
            zoom2 = frame[1];
            alpha1 = frame[2];
            alpha2 = 255f;
        }
        if (frameIndex >= frames.size()) {                                       // :579 while both are playing
            startFlash();
        }
    }

    private void startFlash() {
        phase = Phase.FLASH_UP;                                                  // :580 pbFlashInOut
        tone = 0f;
    }

    private void flashMiddle() {
        bgY = 0;                                                                 // :455-457
        bgH = (int) ScreenMetrics.logicalHeight();
        if (canceled) {                                                          // :458-462
            zoom1 = 1f;
            alpha1 = 0f;
            visible1 = true;
            visible2 = false;
        } else {
            visible1 = false;                                                    // :467-471
            visible2 = true;
            zoom2 = 1f;
            alpha2 = 0f;
        }
        phase = Phase.FLASH_HOLD;
        tick = 0;
    }

    private void flashDone() {
        if (canceled) {                                                          // :581-583
            showText("嗯?\n" + pokemon.name + "停止进化了！", this::close);
            return;
        }
        evolutionSuccess();                                                      // :585
    }

    private int cryFrames;

    private void evolutionSuccess() {                                            // :589-
        int form = pokemon.formIndex();
        String cry = audio().resolveCryFile(newSpecies.id, newSpecies.internalName, form);
        float playtime = cry == null ? 0f : Math.max(0f, audio().sePlayTime(cry));
        cryFrames = (int) Math.ceil(playtime * 40.0) + 4;                        // pbCryFrameLength
        audio().stopBgm();                                                       // :592
        audio().playCry(newSpecies.id);                                          // :593
        phase = Phase.CRY_WAIT;
        tick = 0;
    }

    private void cryDone() {
        audio().playMe("Evolution success");                                     // :599
        String oldName = pokemon.species == null ? "" : pokemon.species.name;     // :601
        showText("恭喜！\n" + pokemon.name + "进化成了" + newSpecies.name + "！", () -> {   // :602-604
            message.clear();                                                     // :605 msgwindow.text = ""
            // :606-607 consumed item / duplicate Pokemon
            PBEvolution.Env env = new EvolutionWorld(context);
            PBEvolution.methodAfterEvolution(pokemon, newSpecies.internalName, env);
            // :609-612 the species change, MOTHIM, calcStats
            pokemon.changeSpecies(context.pbsData(), newSpecies);
            if ("MOTHIM".equals(newSpecies.internalName)) {
                pokemon.setForm(context.pbsData(), 0);
            }
            context.gameState().trainer().registerOwned(pokemon);                // :614-616 seen / owned / pbSeenForm
            learnEvolutionMoves();                                               // :618-622
        });
    }

    // ---- learning moves upon evolution (226:617-622 + 188_PItem_Items:779-821 pbLearnMove) ----

    private void learnEvolutionMoves() {
        phase = Phase.LEARN;
        List<PbsData.Move> list = new ArrayList<>();
        for (PbsData.LearnMove learn : pokemon.species.moves) {
            if (learn.level != 0 && learn.level != pokemon.level) {
                continue;                                                        // :620 0 is "learn upon evolution"
            }
            PbsData.Move move = context.pbsData().move(learn.move);
            if (move != null) {
                list.add(move);
            }
        }
        learnFrom(list, 0);
    }

    private void learnFrom(List<PbsData.Move> list, int index) {
        if (index >= list.size()) {
            close();
            return;
        }
        learnMove(list.get(index), () -> learnFrom(list, index + 1));
    }

    private void learnMove(PbsData.Move move, Runnable done) {
        String movename = move.name;
        String pkmnname = pokemon.name;
        for (Pokemon.MoveSlot known : pokemon.moves) {                            // :791 hasMove?; ignoreifknown
            if (known.move != null && known.move.internalName.equals(move.internalName)) {
                done.run();
                return;
            }
        }
        if (pokemon.moves.size < 4) {                                            // :795-798
            pokemon.learnMoveSilently(move);
            showText("\\se[]" + pkmnname + "学会了" + movename + "！\\se[Pkmn move learnt]", done);
            return;
        }
        showText(pkmnname + "想要学会" + movename + "\n可是它已经学会四个招式了。" + SceneMessage.PAUSE, () ->   // :801
            showText("请选择将被" + movename + "替换的招式。", () -> {                              // :802
                forget = SummaryView.forForget(context, pokemon, move);          // :803 pbForgetMove
                forgetThen = result -> {
                    if (result >= 0) {                                           // :804
                        String oldName = pokemon.moves.get(result).move.name;
                        pokemon.moves.set(result, new Pokemon.MoveSlot(move));   // :807
                        showText("1,\\wt[16] 2,\\wt[16]...\\wt[16] ...\\wt[16] ... 当当！\\se[Battle ball drop]" + SceneMessage.PAUSE, () ->
                            showText(pkmnname + "遗忘了" + oldName + "。\n以及……" + SceneMessage.PAUSE, () ->
                                showText("\\se[]" + pkmnname + "学会了" + movename + "！\\se[Pkmn move learnt]", done)));
                    } else {
                        showText(pkmnname + "没有学习" + movename + "。", done);   // :817
                    }
                };
            }));
    }

    private void close() {
        message.clear();
        phase = Phase.SPRITES_OUT;                                               // :534-547 pbEndScreen
        tick = 0;
    }

    // ---- the background band (226:422-442) -------------------------------

    private void narrowScreen() {
        int half = 8 * 20 / 40;                                                  // :423 halfResizeDiff
        if (bgY < 80) {                                                          // :424
            bgH -= half * 2;                                                     // :425
            if (bgH < (int) ScreenMetrics.logicalHeight() - 64) {                // :426
                bgY += half;                                                     // :427
            }
        }
    }

    private void expandScreen() {
        int half = 8 * 20 / 40;
        if (bgY > 0) {
            bgY -= half;                                                         // :436
        }
        if (bgH < (int) ScreenMetrics.logicalHeight()) {
            bgH += half * 2;                                                     // :440
        }
    }

    // =====================================================================
    // Message window (pbMessageDisplay)
    // =====================================================================

    private void showText(String text, Runnable then) {
        message.show(text, then);
        if (phase != Phase.LEARN) {
            phase = Phase.MESSAGE;
        }
    }

    // =====================================================================
    // Render
    // =====================================================================

    private void loadAssets(MenuAssets a) {
        if (assetsLoaded) {
            return;
        }
        assetsLoaded = true;
        PbsData pbs = context.pbsData();
        List<String> types = new ArrayList<>();
        for (String t : pokemon.types()) types.add(t);
        String first = types.isEmpty() ? "NORMAL" : types.get(0);
        String second = types.size() > 1 ? types.get(1) : null;
        int firstId = typeId(pbs, first);
        int secondId = second == null ? -1 : typeId(pbs, second);
        // :507-512 NORMAL + FLYING (type ids 0 and 2) uses the FLYING background
        String bgType = firstId == 0 && secondId == 2 ? String.valueOf(secondId) : String.valueOf(firstId);
        background = a.graphic("Pictures/Evolution Backs", bgType);
        boolean female = pokemon.effectiveGender() == PokemonStats.FEMALE;
        oldTexture = battler(a, pokemon.species, female, pokemon.shiny, pokemon.superShiny, pokemon.formIndex());
        newTexture = battler(a, newSpecies, female, pokemon.shiny, pokemon.superShiny, pokemon.formIndex());
    }

    private static int typeId(PbsData pbs, String type) {
        PbsData.TypeInfo info = pbs == null ? null : pbs.types.get(type);
        return info == null ? 0 : info.id;
    }

    static Texture battler(MenuAssets a, PbsData.Species s, boolean shiny, int form) {
        return battler(a, s, false, shiny, false, form);
    }

    /** {@code pbLoadPokemonBitmapSpecies}: pbCheckPokemonBitmapFiles' gender / shiny / form fallbacks. */
    static Texture battler(MenuAssets a, PbsData.Species s, boolean female, boolean shiny, boolean superShiny, int form) {
        if (s == null) return null;
        return BattlerBitmaps.find(s, false, female, shiny, superShiny, form,
                name -> a.graphic("Battlers", name));
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        if (forget != null) {
            forget.render(b, a, f, skin);
            return;
        }
        loadAssets(a);
        if (bgH < 0) {
            bgH = (int) h;
        }
        boolean showScene = phase != Phase.BLACK_IN && phase != Phase.BLACK_OUT && phase != Phase.DONE;
        if (showScene) {
            drawBackground(b, a, w, h);
            drawSprites(b, a, w, h);
            message.draw(b, a, f, skin, w, h);
        }
        if (blackAlpha > 0f) {
            b.setColor(0f, 0f, 0f, Math.min(1f, blackAlpha / 255f));
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(Color.WHITE);
        }
    }

    private void drawBackground(SpriteBatch b, MenuAssets a, float w, float h) {
        // the plane sits in @bgviewport (rect y..y+height) with oy = y, so the picture stays where it is
        float top = bgY;
        float height = Math.max(0, Math.min(bgH, h - bgY));
        if (background == null) {
            b.setColor(248f / 255f, 248f / 255f, 248f / 255f, 1f);
            b.draw(a.pixel(), 0f, h - top - height, w, height);
            b.setColor(Color.WHITE);
            return;
        }
        int texW = background.getWidth();
        int texH = background.getHeight();
        int srcY = Math.max(0, Math.min(texH, (int) top));
        int srcH = Math.max(0, Math.min(texH - srcY, (int) height));
        if (srcH <= 0) return;
        TextureRegion region = new TextureRegion(background, 0, srcY, Math.min(texW, (int) w), srcH);
        b.draw(region, 0f, h - top - srcH);
    }

    private void drawSprites(SpriteBatch b, MenuAssets a, float w, float h) {
        float cx = w / 2f;
        float cyTop = (h - 64f) / 2f;
        boolean useShader = shader.isCompiled();
        if (useShader) {
            b.setShader(shader.program());
        }
        if (visible1 && oldTexture != null && zoom1 > 0f) {
            drawSprite(b, oldTexture, cx, h - cyTop, zoom1, alpha1, 255f, useShader);
        }
        if (visible2 && newTexture != null && zoom2 > 0f) {
            drawSprite(b, newTexture, cx, h - cyTop, zoom2, alpha2, opacity2, useShader);
        }
        if (useShader) {
            b.setShader(null);
        }
    }

    private void drawSprite(SpriteBatch b, Texture t, float cx, float cy, float zoom, float whiteAlpha, float opacity,
                            boolean useShader) {
        if (useShader) {
            b.flush();
            shader.setTone(new float[] {tone, tone, tone, 0f});
            shader.setColor(new float[] {255f, 255f, 255f, whiteAlpha});
        }
        float width = t.getWidth() * zoom;
        float height = t.getHeight() * zoom;
        b.setColor(1f, 1f, 1f, opacity / 255f);
        b.draw(t, cx - width / 2f, cy - height / 2f, width, height);
        b.setColor(Color.WHITE);
    }
}
