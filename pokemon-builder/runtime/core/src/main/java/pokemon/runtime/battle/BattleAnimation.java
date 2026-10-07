package pokemon.runtime.battle;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code PokeBattle_Animation} plus {@code PokeBattle_BallAnimationMixin},
 * transcribed line by line from the {@code PokeBattle_Animation} section
 * (265 lines).
 *
 * <p>{@code @pictureEx}/{@code @pictureSprites} become one list of
 * {@link PictureEx}, each already bound to the {@link BattleSprite} it drives;
 * {@code @tempSprites} becomes the list of sprite keys this animation created
 * through {@link #addNewSprite} and must remove again in {@link #dispose}
 * (PokeBattle_Animation:12-14).</p>
 *
 * <p>The Ruby reads {@code @ballSprite.bitmap.width/height} in
 * {@code addBallSprite}/{@code createBallTumbling}, so the engine needs the
 * bitmap sizes the renderer would load; {@link BitmapSize} supplies them.</p>
 */
abstract class BattleAnimation {

    /** {@code pbResolveBitmap}'s size of a {@code Graphics/...} path. */
    interface BitmapSize {
        /** {width,height}, or null when the file does not exist. */
        int[] size(String name);
    }

    /**
     * Everything the transcribed animation classes ask of the battle
     * ({@code @battle}'s predicates and party lookups, the sprite table and the
     * two renderer services the Ruby reads straight off its bitmaps).
     */
    interface Scene {
        BattleSprites sprites();

        PictureEx.SePlayer sePlayer();

        BitmapSize bitmapSize();

        /** {@code @battle.trainerBattle?} (PokeBattle_Battle:191). */
        boolean trainerBattle();

        /** {@code @battle.player.length} (one trainer in this runtime). */
        int playerCount();

        /** {@code @battle.opponent.length}. */
        int opponentCount();

        /** {@code @battle.pbParty(side)} (PokeBattle_Battle:307). */
        com.badlogic.gdx.utils.Array<Battler> party(int side);

        /** {@code @battle.pbPartyStarts(side)} (PokeBattle_Battle:319). */
        int[] partyStarts(int side);

        /** {@code @battle.battlers.length}: one entry per field slot. */
        int battlerCount();

        /** {@code @battle.battlers[i]}. */
        Battler battler(int index);

        /** {@code @battle.opposes?(idxBattler)} (PokeBattle_Battle:538). */
        boolean opposes(int idxBattler);

        /** {@code @battle.pbGetOwnerIndexFromBattlerIndex} (PokeBattle_Battle:232). */
        int ownerIndex(int idxBattler);

        /** {@code @battle.pbSideSize(idxBattler)} (PokeBattle_Battle:215). */
        int sideSize(int idxBattler);

        /** {@code $Trainer.trainertype}, read by ballTracksHand (PokeBattle_Animation:109-114). */
        String playerTrainerType();

        /**
         * The runtime has no follower system (the 40 unsupported follower blocks
         * in the handoff), so {@code $PokemonTemp.dependentEvents.refresh_sprite}
         * (Follower_Main:174-193) always returns false here. Reported, not
         * invented.
         */
        boolean followerSprite();

        /** {@code pbCryFile} (PSystem_FileUtilities:509-539) for a Pokemon. */
        String cryFile(pokemon.runtime.pokemon.Pokemon pokemon);

        /**
         * {@code pbCryFrameLength} (PSystem_FileUtilities:448-469): the cry's
         * length in 40fps frames, {@code (playtime*40).ceil+4}, with the pitch
         * the caller plays it at.
         *
         * @return 0 when there is no cry file
         */
        default int cryFrameLength(pokemon.runtime.pokemon.Pokemon pokemon, int pitch) {
            return 0;
        }

        /**
         * {@code pbChangePokemon} (PokeBattle_Scene:318-329) + {@code pbRefresh}
         * (:63-68): points the battler's sprites at its Pokemon.
         */
        void changePokemon(int idxBattler, Battler battler);
    }

    /** {@code PokeBattle_Animation:2-10 initialize}. */
    private final Scene scene;
    private final BattleSprites sprites;
    private final PictureEx.SePlayer sePlayer;
    private final BitmapSize bitmapSize;
    private final List<PictureEx> pictures = new ArrayList<>();
    private final List<String> tempSpriteKeys = new ArrayList<>();
    private boolean animDone;

    BattleAnimation(Scene scene) {
        this.scene = scene;                                     // :3
        this.sprites = scene.sprites();
        this.sePlayer = scene.sePlayer();                       // :4
        this.bitmapSize = scene.bitmapSize();
    }

    /**
     * {@code PokeBattle_Animation:9 createProcesses}. Ruby runs it from
     * {@code initialize}, after the subclass has finished setting up (a
     * {@code LineupAppearAnimation} resets the lineup graphics first,
     * PokeBattle_SceneAnimations:96-97); Java runs subclass field initialisers
     * after {@code super(...)}, so each subclass calls this at the end of its
     * own constructor instead.
     */
    protected final void start() {
        createProcesses();
    }

    protected Scene scene() {
        return scene;
    }

    BattleSprites sprites() {
        return sprites;
    }

    /** {@code dispose} (PokeBattle_Animation:12-14). */
    void dispose() {
        for (String key : tempSpriteKeys) {
            sprites.remove(key);
        }
        tempSpriteKeys.clear();
    }

    /** {@code createProcesses} (PokeBattle_Animation:16). */
    protected abstract void createProcesses();

    /** {@code empty?} (PokeBattle_Animation:17). */
    boolean empty() {
        return pictures.isEmpty();
    }

    /** {@code animDone?} (PokeBattle_Animation:18). */
    boolean animDone() {
        return animDone;
    }

    List<PictureEx> pictures() {
        return pictures;
    }

    /** {@code addSprite} (PokeBattle_Animation:20-31). */
    protected PictureEx addSprite(BattleSprite sprite) {
        return addSprite(sprite, PictureEx.Origin.TOP_LEFT);
    }

    protected PictureEx addSprite(BattleSprite sprite, int origin) {
        PictureEx picture = PictureEx.forSprite(sprite, origin, sePlayer);
        pictures.add(picture);                                  // :28
        return picture;
    }

    /**
     * {@code addNewSprite} (PokeBattle_Animation:33-45). The runtime registers
     * the temporary sprite in the scene's table ({@code IconSprite.new(x,y,
     * @viewport)} draws whatever is in the viewport, whether or not it is in
     * {@code @sprites}) and remembers its key for {@link #dispose}.
     */
    protected PictureEx addNewSprite(float x, float y, String bitmapName, int origin) {
        String key = "temp_" + bitmapName + "_" + sprites.size();
        BattleSprite sprite = sprites.add(key, BattleSprite.Kind.IMAGE);
        sprite.x = x;                                           // IconSprite.new(x,y,@viewport)
        sprite.y = y;
        sprite.name = bitmapName;                                // s.setBitmap(name) (:41)
        // :41 IconSprite#setBitmap (SpriteWrapper:278-291) loads the bitmap, so
        // src_rect is the whole image when :38's origin is applied
        // (PictureEx:487/495 ox = src_rect.width/2, oy = src_rect.height/2).
        int[] size = bitmapSize == null ? null : bitmapSize.size(bitmapName);
        sprite.bitmapWidth = size == null ? -1 : size[0];
        sprite.bitmapHeight = size == null ? -1 : size[1];
        tempSpriteKeys.add(key);                                // :43
        PictureEx picture = PictureEx.forNewSprite(sprite, x, y, bitmapName, origin,
                pictures.size(), sePlayer);
        pictures.add(picture);                                  // :39
        return picture;
    }

    /** {@code @ballSprite} (PokeBattle_Animation:90). */
    private BattleSprite ballSprite;
    /**
     * {@code @ballSprite.bitmap}'s size. The Ruby reads it directly in
     * {@code addBallSprite:91}, {@code createBallTumbling:183} and
     * {@code ballSetOpen:205} / {@code ballSetClosed:212} - all of which run
     * while {@code createProcesses} is building the animation, i.e. before the
     * {@code setName} process has swapped the open ball in.
     */
    private int[] ballBitmapSize;

    protected BattleSprite ballSprite() {
        return ballSprite;
    }

    /** {@code update} (PokeBattle_Animation:47-58). */
    void update() {
        if (animDone) {                                         // :48
            return;
        }
        boolean finished = true;                                // :50
        for (PictureEx picture : pictures) {                    // :51
            if (!picture.running()) {                           // :52
                continue;
            }
            finished = false;                                   // :53
            picture.update();                                   // :54
            picture.applyTo(picture.sprite(), bitmapSize);      // :55 setPictureIconSprite
        }
        if (finished) {
            animDone = true;                                    // :57
        }
    }

    /**
     * {@code PokemonBattlerSprite#pbPlayIntroAnimation}
     * (PokeBattle_SceneElements:596-600): {@code cry = pbCryFile(@pkmn);
     * pbSEPlay(cry) if cry}. Queued as the {@code moveZoom} callback of
     * {@code battlerAppear} (PokeBattle_Animation:231) and as a plain
     * {@code setCallback} in {@code BattleIntroAnimation2:78}.
     */
    protected void playIntroAnimation(BattleSprite sprite) {
        if (sprite == null || sprite.battler == null || sprite.battler.pokemon == null) {
            return;                                              // :597
        }
        String cry = scene().cryFile(sprite.battler.pokemon);     // :598
        if (cry != null && scene().sePlayer() != null) {          // :599
            scene().sePlayer().playSe(cry, 100, 100);             // pbSEPlay defaults (Audio_Play:200)
        }
    }

    // ------------------------------------------------------------------
    // PokeBattle_BallAnimationMixin (PokeBattle_Animation:63-264)
    // ------------------------------------------------------------------

    /**
     * {@code getBattlerColorFromBallType} (PokeBattle_Animation:66-85): the
     * colour a Pokémon turns when it goes into or out of its Poké Ball.
     */
    static float[] getBattlerColorFromBallType(int ballType) {
        switch (ballType) {
            case 1: return rgb(132, 189, 247);    // Great Ball
            case 2: return rgb(189, 247, 165);    // Safari Ball
            case 3: return rgb(255, 255, 123);    // Ultra Ball
            case 4: return rgb(189, 165, 231);    // Master Ball
            case 5: return rgb(173, 255, 206);    // Net Ball
            case 6: return rgb(99, 206, 247);     // Dive Ball
            case 7: return rgb(247, 222, 82);     // Nest Ball
            case 8: return rgb(255, 198, 132);    // Repeat Ball
            case 9: return rgb(239, 247, 247);    // Timer Ball
            case 10: return rgb(255, 140, 82);    // Luxury Ball
            case 11: return rgb(255, 74, 82);     // Premier Ball
            case 12: return rgb(115, 115, 140);   // Dusk Ball
            case 13: return rgb(255, 198, 231);   // Heal Ball
            case 14: return rgb(140, 214, 255);   // Quick Ball
            case 15: return rgb(247, 66, 41);     // Cherish Ball
            default: return rgb(255, 181, 247);   // Poké Ball, Sport Ball, others
        }
    }

    private static float[] rgb(int r, int g, int b) {
        return new float[] { r, g, b, 255f };
    }

    /** {@code addBallSprite} (PokeBattle_Animation:87-96). */
    protected PictureEx addBallSprite(float ballX, float ballY, int ballType) {
        String file = String.format(java.util.Locale.ROOT, "Graphics/Battle animations/ball_%02d",
                ballType);                                      // :88-89
        PictureEx ball = addNewSprite(ballX, ballY, file, PictureEx.Origin.CENTER);
        ballSprite = ball.sprite();                              // :90 (@pictureSprites.last)
        int[] size = bitmapSize == null ? null : bitmapSize.size(file);
        ballBitmapSize = size;
        if (size != null && size[0] >= size[1]) {                // :91
            ballSprite.srcWidth = size[1] / 2;                   // :92
            ball.setSrcSize(0, size[1] / 2, size[1]);            // :93
        }
        return ball;                                            // :95
    }

    /**
     * {@code ballTracksHand} (PokeBattle_Animation:98-137). The trainer sprite's
     * frame width is needed twice, so the caller passes the sprite.
     */
    protected float[] ballTracksHand(PictureEx ball, BattleSprite traSprite) {
        return ballTracksHand(ball, traSprite, false);
    }

    protected float[] ballTracksHand(PictureEx ball, BattleSprite traSprite, boolean safariThrow) {
        int[] traSize = bitmapSize == null || traSprite == null ? null : bitmapSize.size(traSprite.name);
        int bitmapWidth = traSize == null ? 0 : traSize[0];
        int bitmapHeight = traSize == null ? 0 : traSize[1];
        // Back sprite isn't animated, no hand-tracking needed
        if (bitmapWidth < bitmapHeight * 2) {                    // :100
            ball.setVisible(7, true);                            // :101
            float ballStartX = traSprite.x;                      // :102
            if (!safariThrow) {
                ballStartX -= ball.totalDuration() * (PictureEx.Graphics.WIDTH / (2 * 16f));   // :103
            }
            float ballStartY = traSprite.y - bitmapHeight / 2f;  // :104
            return new float[] { ballStartX, ballStartY };       // :105
        }
        // Back sprite is animated, make the Poké Ball track the trainer's hand
        float[][] coordSets = {
            { traSprite.x - 44, traSprite.y - 32 }, { -10, -36 }, { 118, -4 } };   // :108
        if (isTrainerType(trainerType(), "POKEMONTRAINER_Leaf")) {                 // :109-110
            coordSets = new float[][] {
                { traSprite.x - 30, traSprite.y - 30 }, { -18, -36 }, { 118, -6 } };
        } else if (isTrainerType(trainerType(), "POKEMONTRAINER_Brendan")) {       // :111-112
            coordSets = new float[][] {
                { traSprite.x - 46, traSprite.y - 40 }, { -4, -30 }, { 118, -2 } };
        } else if (isTrainerType(trainerType(), "POKEMONTRAINER_May")) {           // :113-114
            coordSets = new float[][] {
                { traSprite.x - 44, traSprite.y - 38 }, { -8, -30 }, { 122, 0 } };
        }
        // Arm stretched out behind player
        ball.setVisible(0, true);                                // :117
        ball.setXY(0, coordSets[0][0], coordSets[0][1]);         // :118
        if (!safariThrow) {
            ball.moveDelta(0, 5, -5 * (PictureEx.Graphics.WIDTH / (2 * 16f)), 0);   // :119
        }
        if (safariThrow) {
            ball.setDelta(0, -12, 0);                            // :120
        }
        // Arm mid throw
        ball.setDelta(5, coordSets[1][0], coordSets[1][1]);      // :122
        if (!safariThrow) {
            ball.moveDelta(5, 2, -2 * (PictureEx.Graphics.WIDTH / (2 * 16f)), 0);   // :123
        }
        if (safariThrow) {
            ball.setDelta(5, 34, 0);                             // :124
        }
        // Start of throw
        ball.setDelta(7, coordSets[2][0], coordSets[2][1]);      // :126
        if (safariThrow) {
            ball.setDelta(7, -14, 0);                            // :127
        }
        // Update Poké Ball trajectory's start position
        float ballStartX = 0;
        float ballStartY = 0;                                    // :129
        for (float[] c : coordSets) {                            // :130-133
            ballStartX += c[0];
            ballStartY += c[1];
        }
        if (!safariThrow) {
            ballStartX -= ball.totalDuration() * (PictureEx.Graphics.WIDTH / (2 * 16f));   // :134
        }
        if (safariThrow) {
            ballStartX += 8;                                     // :135  # -12 + 34 - 14
        }
        return new float[] { ballStartX, ballStartY };           // :136
    }

    /**
     * The {@code @trainer} of a send-out animation
     * ({@code @battler.battle.pbGetOwnerFromBattlerIndex}), used only by
     * {@code ballTracksHand}'s animated-back-sprite branch
     * (PokeBattle_Animation:109-114).
     */
    protected String trainerType() {
        return scene.playerTrainerType();
    }

    /** {@code isConst?(@trainer.trainertype,PBTrainers,:X)} (PokeBattle_Animation:109-114). */
    private static boolean isTrainerType(String type, String name) {
        return type != null && type.equals(name);
    }

    /**
     * {@code createBallTrajectory} (PokeBattle_Animation:161-177): a quadratic
     * through (startX,startY) / (midX,midY) / (endX,endY), one
     * {@code moveXY(...,1,...)} per frame, then the tumble.
     */
    protected void createBallTrajectory(PictureEx ball, int delay, int duration, float startX,
            float startY, float midX, float midY, float endX, float endY) {
        ball.setVisible(delay, true);                            // :166
        float a = 2 * startY - 4 * midY + 2 * endY;              // :167
        float b = 4 * midY - 3 * startY - endY;                  // :168
        float c = startY;                                        // :169
        for (int i = 1; i <= duration; i++) {                    // :170
            float t = (float) i / duration;                      // :171
            float x = startX + (endX - startX) * t;              // :172
            float y = a * t * t + b * t + c;                     // :173
            ball.moveXY(delay + i - 1, 1, x, y);                 // :174
        }
        createBallTumbling(ball, delay, duration);               // :176
    }

    /** {@code createBallTumbling} (PokeBattle_Animation:179-201). */
    protected void createBallTumbling(PictureEx ball, int delay, int duration) {
        int numTumbles = 1;                                      // :181
        int numFrames = 1;                                       // :182
        if (ballBitmapSize != null && ballBitmapSize[0] >= ballBitmapSize[1]) {   // :183
            // 2* because each frame is twice as tall as it is wide
            numFrames = 2 * ballBitmapSize[0] / ballBitmapSize[1];                // :185
        }
        if (numFrames > 1) {                                     // :187
            int curFrame = 0;                                    // :188
            for (int i = 1; i <= duration; i++) {                // :189
                int thisFrame = numFrames * numTumbles * i / duration;   // :190
                if (thisFrame > curFrame) {                      // :191
                    curFrame = thisFrame;                        // :192
                    ball.setSrc(delay + i - 1,
                            (curFrame % numFrames) * ballBitmapSize[1] / 2, 0);   // :193
                }
            }
            ball.setSrc(delay + duration, 0, 0);                 // :196
        }
        // Rotate ball
        ball.moveAngle(delay, duration, 360 * 3);                // :199
        ball.setAngle(delay + duration, 0);                      // :200
    }

    /** {@code ballSetOpen} (PokeBattle_Animation:203-208). */
    protected void ballSetOpen(PictureEx ball, int delay, int ballType) {
        ball.setName(delay, String.format(java.util.Locale.ROOT,
                "Graphics/Battle animations/ball_%02d_open", ballType));   // :204
        if (ballBitmapSize != null && ballBitmapSize[0] >= ballBitmapSize[1]) {   // :205
            ball.setSrcSize(delay, ballBitmapSize[1] / 2, ballBitmapSize[1]);     // :206
        }
    }

    /** {@code ballSetClosed} (PokeBattle_Animation:210-215). */
    protected void ballSetClosed(PictureEx ball, int delay, int ballType) {
        ball.setName(delay, String.format(java.util.Locale.ROOT,
                "Graphics/Battle animations/ball_%02d", ballType));        // :211
        if (ballBitmapSize != null && ballBitmapSize[0] >= ballBitmapSize[1]) {   // :212
            ball.setSrcSize(delay, ballBitmapSize[1] / 2, ballBitmapSize[1]);     // :213
        }
    }

    /**
     * {@code ballOpenUp} (PokeBattle_Animation:217-225). Ruby's {@code delay += 6}
     * only affects the method's own copy, so the caller keeps its original
     * delay - which is what {@code ballBurst(delay,...)} then uses.
     */
    protected void ballOpenUp(PictureEx ball, int delay, int ballType) {
        ballOpenUp(ball, delay, ballType, true, true);
    }

    protected void ballOpenUp(PictureEx ball, int delay, int ballType, boolean showSquish,
            boolean playSE) {
        int d = delay;
        if (showSquish) {                                        // :218
            ball.moveZoomXY(d, 1, 120, 80);                      // :219  Squish
            ball.moveZoom(d + 5, 1, 100);                        // :220  Unsquish
            d += 6;                                              // :221
        }
        if (playSE) {
            ball.setSE(d, "Battle recall");                      // :223
        }
        ballSetOpen(ball, d, ballType);                          // :224
    }

    /**
     * {@code battlerAppear} (PokeBattle_Animation:227-236). The
     * {@code [batSprite,:pbPlayIntroAnimation]} callback is passed in because it
     * belongs to the battler sprite, not the animation.
     */
    protected void battlerAppear(PictureEx battler, int delay, float battlerX, float battlerY,
            BattleSprite batSprite, float[] color, PictureEx.Callback introCallback) {
        battler.setVisible(delay, true);                         // :228
        battler.setOpacity(delay, 255);                          // :229
        battler.moveXY(delay, 5, battlerX, battlerY);            // :230
        battler.moveZoom(delay, 5, 100, introCallback);          // :231
        // As soon as the battler sprite finishes zooming, and just as it starts
        // changing its tone to normal, it plays its intro animation.
        float[] target = color.clone();
        target[3] = 0;                                           // :234
        battler.moveColor(delay + 5, 10, target);                // :235
    }

    /**
     * {@code battlerAbsorb} (PokeBattle_Animation:238-245): the reverse of
     * {@link #battlerAppear} - the battler turns into the ball's colour, then
     * shrinks into the ball.
     */
    protected void battlerAbsorb(PictureEx battler, int delay, float battlerX, float battlerY,
            float[] color) {
        float[] target = color.clone();
        target[3] = 255;                                         // :239
        battler.moveColor(delay, 10, target);                    // :240
        int d = battler.totalDuration();                         // :241
        battler.moveXY(d, 5, battlerX, battlerY);                 // :242
        battler.moveZoom(d, 5, 0);                               // :243
        battler.setVisible(d + 5, false);                        // :244
    }

    /**
     * {@code ballBurst} (PokeBattle_Animation:248-249) is empty in the plugin; this
     * runtime's addition is a sparkle burst assembled from existing sheets
     * ({@code PRAS- Status2}: yellow diamonds and pink hearts; {@code PRAS- Recovery}:
     * star flash), tinted and sized per ball type.
     */
    protected void ballBurst(int delay, float ballX, float ballY, int ballType) {
        burstX = ballX;
        burstY = ballY;
        final int[] diamond = { 768, 768 };
        final int[] heart = { 576, 768 };
        final float[] none = null;
        switch (ballType) {
            case 4: {                                              // Master Ball: lavish
                float[] purple = { 190, 90, 255, 150 };
                float[] gold = { 255, 220, 80, 120 };
                burstRing(diamond, 10, 62, 0f, delay, 16, 70, 15, purple, 0f);
                burstRing(diamond, 14, 96, 12f, delay + 3, 20, 60, 10, gold, 0f);
                burstRing(diamond, 8, 34, 5f, delay, 12, 80, 20, none, 0f);
                for (int i = 0; i < 6; i++) {                      // big rotating stars
                    double a = Math.toRadians(60.0 * i + 15);
                    PictureEx s = addBurstCell("Graphics/Animations/PRAS- Recovery", ballX, ballY,
                            192, 384, 0);
                    s.setVisible(0, false);
                    s.setColor(0, purple);
                    s.setVisible(delay, true);
                    s.setOpacity(delay, 255);
                    s.setZoom(delay, 40);
                    s.moveXY(delay, 20, ballX + (float) Math.cos(a) * 80,
                            ballY + (float) Math.sin(a) * 64);
                    s.moveZoom(delay, 20, 120);
                    s.moveAngle(delay, 20, 120);
                    s.moveOpacity(delay + 10, 10, 0);
                    s.setVisible(delay + 20, false);
                }
                burstFlash(delay, 90, 220, purple);
                return;
            }
            case 20: case 13:                                      // Love / Heal Ball: hearts
                burstRing(heart, 7, 50, 8f, delay, 18, 75, 25,
                        ballType == 13 ? new float[] { 255, 150, 190, 90 } : none, -14f);
                burstRing(heart, 5, 30, 30f, delay + 2, 16, 60, 20, none, -10f);
                burstFlash(delay, 60, 130, new float[] { 255, 170, 210, 140 });
                return;
            default:
                break;
        }
        burstRing(diamond, ballType == 3 ? 12 : 8, ballType == 3 ? 64 : 54, 22.5f, delay, 14, 55, 15,
                tintFor(ballType), 0f);
        burstRing(diamond, 8, 32, 0f, delay, 14, 45, 10, tintFor(ballType), 0f);
        burstFlash(delay, 60, 130, tintFor(ballType));
    }

    /** {@code ballBurstCapture} (PokeBattle_Animation:254-255): the opening of the capturing ball. */
    protected void ballBurstCapture(int delay, float ballX, float ballY, int ballType) {
        ballBurst(delay, ballX, ballY, ballType);
    }

    /** Colour a ball type's sparkles are blended towards ({@code null} = untinted yellow). */
    private static float[] tintFor(int ballType) {
        switch (ballType) {
            case 1: return new float[] { 70, 130, 255, 170 };      // Great
            case 2: return new float[] { 90, 200, 90, 170 };       // Safari
            case 5: return new float[] { 60, 200, 200, 170 };      // Net
            case 6: return new float[] { 40, 90, 230, 170 };       // Dive
            case 7: return new float[] { 150, 210, 60, 150 };      // Nest
            case 8: return new float[] { 255, 130, 40, 150 };      // Repeat
            case 9: return new float[] { 230, 230, 230, 170 };     // Timer
            case 11: return new float[] { 255, 255, 255, 220 };    // Premier
            case 12: return new float[] { 120, 60, 190, 170 };     // Dusk
            case 14: return new float[] { 60, 200, 255, 160 };     // Quick
            case 15: return new float[] { 255, 60, 60, 170 };      // Cherish
            case 16: return new float[] { 255, 140, 40, 150 };     // Fast
            case 17: return new float[] { 255, 150, 50, 150 };     // Level
            case 18: return new float[] { 70, 120, 255, 150 };     // Lure
            case 19: return new float[] { 160, 160, 170, 170 };    // Heavy
            case 21: return new float[] { 110, 220, 110, 160 };    // Friend
            case 22: return new float[] { 120, 140, 255, 160 };    // Moon
            case 23: return new float[] { 255, 80, 60, 150 };      // Sport
            case 24: return new float[] { 255, 160, 230, 160 };    // Dream
            case 25: return new float[] { 80, 180, 255, 160 };     // Beast
            case 26: return new float[] { 255, 150, 200, 140 };    // Pet
            case 27: return new float[] { 90, 170, 80, 160 };      // Wilderness
            default: return null;                                  // Poké / Ultra / Luxury / Safari-less
        }
    }

    /**
     * A ring of {@code count} sparkles flying out from the ball: {@code cell} is the
     * (sx, sy) of one 192px cell of {@code PRAS- Status2}; the second frame of the
     * diamond is used half way for a twinkle.
     */
    private void burstRing(int[] cell, int count, float dist, float angleOffsetDeg, int delay,
            int duration, float zoomStart, float zoomEnd, float[] tint, float yBias) {
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(360.0 * i / count + angleOffsetDeg);
            float d = (i % 2 == 0) ? dist : dist * 0.7f;
            PictureEx p = addBurstCell("Graphics/Animations/PRAS- Status2", burstX, burstY, 192,
                    cell[0], cell[1]);
            if (tint != null) {
                p.setColor(0, tint);
            }
            p.setVisible(0, false);
            p.setZoom(0, zoomStart);
            p.setVisible(delay, true);
            p.setOpacity(delay, 255);
            p.moveXY(delay, duration, burstX + (float) Math.cos(angle) * d,
                    burstY + (float) Math.sin(angle) * d * 0.8f + yBias);
            p.moveZoom(delay, duration, zoomEnd);
            if (cell[0] == 768) {
                p.setSrc(delay + duration / 2, 0, 960);           // twinkle
            }
            p.moveOpacity(delay + duration * 2 / 5, duration * 3 / 5, 0);
            p.setVisible(delay + duration, false);
        }
    }

    /** The burst's centre, set by {@link #ballBurst}. */
    private float burstX;
    private float burstY;

    /** The light flash at the ball's opening. */
    private void burstFlash(int delay, float zoomStart, float zoomEnd, float[] tint) {
        PictureEx flash = addBurstCell("Graphics/Animations/PRAS- Recovery", burstX, burstY, 192,
                384, 0);
        if (tint != null) {
            flash.setColor(0, tint);
        }
        flash.setVisible(0, false);
        flash.setVisible(delay, true);
        flash.setOpacity(delay, 255);
        flash.setZoom(delay, zoomStart);
        flash.moveZoom(delay, 8, zoomEnd);
        flash.moveOpacity(delay + 2, 6, 0);
        flash.setVisible(delay + 8, false);
    }

    /** One cell of an animation sheet as a temporary centred sprite above the battlers. */
    private PictureEx addBurstCell(String sheet, float x, float y, int cell, int sx, int sy) {
        PictureEx p = addNewSprite(x, y, sheet, PictureEx.Origin.CENTER);
        p.sprite().srcWidth = cell;
        p.sprite().srcHeight = cell;
        p.setSrcSize(0, cell, cell);
        p.setSrc(0, sx, sy);
        p.setZ(0, 90);
        return p;
    }


    /** {@code ballCaptureSuccess} (PokeBattle_Animation:257-260). */
    protected void ballCaptureSuccess(PictureEx ball, int delay, float ballX, float ballY) {
        ball.setSE(delay, "Battle catch click");                 // :258
        ball.moveTone(delay, 4, new float[] { -64, -64, -64, 128 });   // :259
    }

    /**
     * {@code @sprites["captureBall"] = @tempSprites[i]; @tempSprites[i] = nil}
     * (PokeballThrowCaptureAnimation:891-892): the temporary sprite outlives
     * {@link #dispose} under a new key.
     */
    protected void keepTempSprite(BattleSprite sprite, String newKey) {
        for (String key : new ArrayList<>(tempSpriteKeys)) {
            if (sprites.get(key) == sprite) {
                tempSpriteKeys.remove(key);
                sprites.remove(key);
                sprites.put(newKey, sprite);
                return;
            }
        }
    }

    /** {@code ballBurstRecall} (PokeBattle_Animation:262-263): empty in this project. */
    protected void ballBurstRecall(int delay, float ballX, float ballY, int ballType) {
    }
}
