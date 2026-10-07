package pokemon.runtime.battle;

import pokemon.runtime.data.BattleAnimationData;
import pokemon.runtime.pokemon.Pokemon;

/**
 * {@code PBAnimationPlayerX} (PokeBattle_AnimationPlayer:704-877) with the
 * helpers it calls: {@code transformPoint} (:37-59), {@code getSpriteCenter}
 * (:61-69), {@code isReversed} (:71-75), {@code pbSpriteSetAnimFrame}
 * (:624-697) and {@code PBAnimation#playTiming} (:500-616), transcribed line by
 * line. The 20-ticks-per-second pacing ({@code @framesPerTick = 2}) is driven
 * by the caller's 40 fps frame loop, one {@link #update()} per frame like
 * {@code Scene_Animations:570}.
 *
 * <p>RGSS {@code Sprite} fields are assigned the way the DLL stores them:
 * {@code x}/{@code y} are Integers (a Float assigned to {@code sprite.x}
 * truncates), {@code opacity} is an Integer clamped to 0..255, and
 * {@code Color}/{@code Tone} clamp their components. Float math is done in
 * {@code double} like Ruby's.</p>
 *
 * <p>What the plugin does with a missing sheet: {@code AnimatedBitmap}
 * (AnimatedBitmap:249-252) falls back to a blank 32x32 bitmap, so a missing
 * {@code Graphics/Animations/...} file draws nothing and never raises;
 * {@link Host#graphicSize} reports the same 32x32.</p>
 */
final class BattleAnimationPlayer {

    /** {@code PBAnimationPlayerX::MAX_SPRITES} (:706). */
    static final int MAX_SPRITES = 60;
    /** {@code @framesPerTick} (:721): "20 ticks per second" at the 40 fps frame loop. */
    private static final int FRAMES_PER_TICK = 2;
    /** {@code animwidth} of {@code pbSpriteSetAnimFrame} (:643). */
    private static final int ANIM_WIDTH = 192;

    /** What the player asks of the battle scene ({@code scene.sprites}, the bitmaps, the SE player). */
    interface Host {
        BattleSprites sprites();

        /** {@code pbSEPlay(name,volume,pitch)} (Audio_Play:200-217). */
        PictureEx.SePlayer sePlayer();

        /** {@code pbCryFile(pokemon)} (PSystem_FileUtilities:509-539). */
        String cryFile(Pokemon pokemon);

        /**
         * {width,height} of a {@code Graphics/...} bitmap as {@code AnimatedBitmap}
         * loads it: the file's size, or 32x32 when the file is missing.
         */
        int[] graphicSize(String path);

        /**
         * {width,height} of a battler sprite's own bitmap, null while it has none
         * ({@code sprite.bitmap == nil}).
         */
        int[] battlerBitmapSize(BattleSprite sprite);
    }

    /** {@code LargePlane} (Planes:15-158) as far as {@code PBAnimationPlayerX} uses it. */
    private final class Plane {
        final BattleSprite sprite;
        /** {@code @__ox}/{@code @__oy}. */
        double ox;
        double oy;
        /** {@code @__bitmap != nil}. */
        boolean loaded;
        int bitmapWidth;
        int bitmapHeight;
        /** {@code AnimatedPlane#@bitmap}: the AnimatedBitmap that {@code update} copies into {@code @__bitmap}. */
        String pendingPath;

        Plane(String key, BattleSprite.Kind kind, int z) {
            sprite = host.sprites().add(key, kind);
            sprite.z = z;
            sprite.opacity = 0f;
            sprite.visible = false;
            if (kind == BattleSprite.Kind.PLANE_COLOR) {
                loaded = true;                        // ColoredPlane:168 self.bitmap=Bitmap.new(32,32)
                bitmapWidth = 32;
                bitmapHeight = 32;
            }
            refresh();
        }

        boolean hasBitmap() {
            return loaded;
        }

        /** {@code LargePlane#refresh} (Planes:112-129), minus the drawing the renderer does. */
        void refresh() {
            sprite.visible = loaded;                                       // :113 (@__visible is always true here)
            if (loaded) {
                if (ox < 0) ox += bitmapWidth;                             // :116 (zoom_x is 1)
                if (oy < 0) oy += bitmapHeight;                            // :117
                if (ox > bitmapWidth) ox -= bitmapWidth;                   // :118
                if (oy > bitmapHeight) oy -= bitmapHeight;                 // :119
                sprite.ox = (float) ox;
                sprite.oy = (float) oy;
            }
        }

        /** {@code LargePlane#ox=} (Planes:49-53). */
        void setOx(double value) {
            if (ox == value) return;                                       // :50
            ox = value;                                                    // :51
            refresh();                                                     // :52
        }

        /** {@code LargePlane#oy=} (Planes:55-59). */
        void setOy(double value) {
            if (oy == value) return;
            oy = value;
            refresh();
        }

        /** {@code AnimatedPlane#setBitmap(file)} (Planes:227-231): clearBitmaps, then a new AnimatedBitmap. */
        void setBitmap(String path) {
            pendingPath = null;                                            // :210 @bitmap = nil
            if (loaded && sprite.kind == BattleSprite.Kind.PLANE_BITMAP) {
                loaded = false;                                            // :212 self.bitmap = nil
                refresh();
            }
            sprite.name = "";
            if (path == null) return;                                      // :229
            pendingPath = path;                                            // :230
        }

        /** {@code AnimatedPlane#update} (Planes:201-207): {@code self.bitmap=@bitmap.bitmap}. */
        void update() {
            if (pendingPath != null && !loaded) {
                int[] size = host.graphicSize(pendingPath);
                loaded = true;
                bitmapWidth = size[0];
                bitmapHeight = size[1];
                sprite.name = pendingPath;
                refresh();                                                 // :73 bitmap= -> refresh
            }
        }

        void setColor(double r, double g, double b, double a) {
            sprite.color[0] = clamp(r, 0, 255);
            sprite.color[1] = clamp(g, 0, 255);
            sprite.color[2] = clamp(b, 0, 255);
            sprite.color[3] = clamp(a, 0, 255);
        }

        void setOpacity(double value) {
            sprite.opacity = opacity(value);
        }
    }

    private static int serial;

    private final Host host;
    private final BattleAnimationData.Animation animation;
    /** {@code @user}: "just used for playing user's cry" (:710). */
    private final Battler user;
    private final BattleSprite userSprite;
    private final BattleSprite targetSprite;
    /** {@code @userbitmap}/{@code @targetbitmap}: the battler bitmaps' sizes, null = nil. */
    private final int[] userBitmapSize;
    private final int[] targetBitmapSize;
    private boolean looping;
    /** {@code @animbitmap}: whether the animation's sheet is loaded. */
    private boolean animBitmapLoaded;
    private int frame = -1;
    private double[] srcLine;
    private double[] dstLine;
    private final double[] userOrig;
    private final double[] targetOrig;
    /** {@code @oldbg}/{@code @oldfo}: ox, oy, opacity, then the colour's r,g,b,a. */
    private final double[] oldBg = new double[7];
    private final double[] oldFo = new double[7];
    private final BattleSprite[] animSprites = new BattleSprite[MAX_SPRITES];
    private Plane bgColor;
    private Plane bgGraphic;
    private Plane foColor;
    private Plane foGraphic;
    private final String keyPrefix;
    private boolean disposed;

    /**
     * {@code PBAnimationPlayerX#initialize} (:708-729).
     *
     * @param user the user battler; null like {@code user=nil}
     * @param target the target battler; null like {@code target=nil}
     */
    BattleAnimationPlayer(BattleAnimationData.Animation animation, Battler user, Battler target,
                          Host host, boolean oppMove) {
        this.animation = animation;
        this.host = host;
        this.user = oppMove ? target : user;                                       // :710
        this.userSprite = user != null ? host.sprites().get("pokemon_" + user.index) : null;     // :711
        this.targetSprite = target != null ? host.sprites().get("pokemon_" + target.index) : null;   // :712
        this.userBitmapSize = userSprite != null ? host.battlerBitmapSize(userSprite) : null;     // :713
        this.targetBitmapSize = targetSprite != null ? host.battlerBitmapSize(targetSprite) : null;   // :714
        this.keyPrefix = "animcel" + (serial++) + "_";
        this.userOrig = getSpriteCenter(userSprite);                               // :724
        this.targetOrig = getSpriteCenter(targetSprite);                           // :725
        initializeSprites();
    }

    /** {@code PBAnimationPlayerX#initializeSprites} (:731-771). */
    private void initializeSprites() {
        animSprites[0] = userSprite;                                               // :734
        animSprites[1] = targetSprite;                                             // :735
        for (int i = 2; i < MAX_SPRITES; i++) {                                    // :736
            BattleSprite cel = host.sprites().add(keyPrefix + i, BattleSprite.Kind.ANIM_CEL);   // :737
            cel.visible = false;                                                   // :739
            animSprites[i] = cel;
        }
        bgColor = new Plane(keyPrefix + "bgColor", BattleSprite.Kind.PLANE_COLOR, 5);          // :742-747
        bgGraphic = new Plane(keyPrefix + "bgGraphic", BattleSprite.Kind.PLANE_BITMAP, 5);     // :749-755
        foColor = new Plane(keyPrefix + "foColor", BattleSprite.Kind.PLANE_COLOR, 85);         // :757-762
        foGraphic = new Plane(keyPrefix + "foGraphic", BattleSprite.Kind.PLANE_BITMAP, 85);    // :764-770
    }

    /** {@code PBAnimationPlayerX#dispose} (:773-782). */
    void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        animBitmapLoaded = false;                                                  // :774
        for (int i = 2; i < MAX_SPRITES; i++) {                                    // :775
            if (animSprites[i] != null) {
                host.sprites().remove(animSprites[i].key);                         // :776
            }
        }
        host.sprites().remove(bgGraphic.sprite.key);                               // :778
        host.sprites().remove(bgColor.sprite.key);                                 // :779
        host.sprites().remove(foGraphic.sprite.key);                               // :780
        host.sprites().remove(foColor.sprite.key);                                 // :781
    }

    /** {@code PBAnimationPlayerX#start} (:784-786). */
    void start() {
        frame = 0;
    }

    /** {@code PBAnimationPlayerX#animDone?} (:788-790). */
    boolean animDone() {
        return frame < 0;
    }

    void setLooping(boolean value) {
        looping = value;
    }

    /** {@code PBAnimationPlayerX#setLineTransform} (:792-795). */
    void setLineTransform(double x1, double y1, double x2, double y2,
                          double x3, double y3, double x4, double y4) {
        srcLine = new double[] { x1, y1, x2, y2 };
        dstLine = new double[] { x3, y3, x4, y4 };
    }

    // ------------------------------------------------------------------
    // Geometry helpers (:37-75)
    // ------------------------------------------------------------------

    /** {@code yaxisIntersect} (:37-43). */
    private static double[] yaxisIntersect(double x1, double y1, double x2, double y2, double px, double py) {
        double dx = x2 - x1;                                                       // :38
        double dy = y2 - y1;                                                       // :39
        double x = dx == 0 ? 0.0 : (px - x1) / dx;                                 // :40
        double y = dy == 0 ? 0.0 : (py - y1) / dy;                                 // :41
        return new double[] { x, y };
    }

    /** {@code repositionY} (:45-51). */
    private static double[] repositionY(double x1, double y1, double x2, double y2, double tx, double ty) {
        double dx = x2 - x1;                                                       // :46
        double dy = y2 - y1;                                                       // :47
        return new double[] { x1 + tx * dx, y1 + ty * dy };                        // :48-50
    }

    /** {@code transformPoint} (:53-59). */
    static double[] transformPoint(double x1, double y1, double x2, double y2,
                                   double x3, double y3, double x4, double y4,
                                   double px, double py) {
        double[] ret = yaxisIntersect(x1, y1, x2, y2, px, py);                     // :56
        return repositionY(x3, y3, x4, y4, ret[0], ret[1]);                        // :57
    }

    /** {@code getSpriteCenter} (:61-69). */
    private double[] getSpriteCenter(BattleSprite sprite) {
        if (sprite == null) {
            return new double[] { 0, 0 };                                          // :62
        }
        if (host.battlerBitmapSize(sprite) == null) {
            return new double[] { sprite.x, sprite.y };                            // :63 !sprite.bitmap
        }
        int centerX = sprite.sourceWidth() / 2;                                    // :64 Integer division
        int centerY = sprite.sourceHeight() / 2;                                   // :65
        double offsetX = (centerX - sprite.ox) * sprite.zoomX;                     // :66
        double offsetY = (centerY - sprite.oy) * sprite.zoomY;                     // :67
        return new double[] { sprite.x + offsetX, sprite.y + offsetY };            // :68
    }

    /** {@code isReversed} (:71-75). */
    static boolean isReversed(double src0, double src1, double dst0, double dst1) {
        if (src0 == src1) return false;                                            // :72
        if (src0 < src1) return dst0 > dst1;                                       // :73
        return dst0 < dst1;                                                        // :74
    }

    // ------------------------------------------------------------------
    // RGSS value semantics
    // ------------------------------------------------------------------

    private static float clamp(double value, double lo, double hi) {
        return (float) Math.max(lo, Math.min(hi, value));
    }

    /** {@code Sprite#opacity=}: an Integer 0..255 (a Float truncates). */
    private static float opacity(double value) {
        return Math.max(0, Math.min(255, (int) value));
    }

    // ------------------------------------------------------------------
    // pbSpriteSetAnimFrame (:624-697)
    // ------------------------------------------------------------------

    private void pbSpriteSetAnimFrame(BattleSprite sprite, BattleAnimationData.Cel frame, int[] bitmapSize) {
        // :626-629 `if !frame` never happens here: update skips nil cels.
        sprite.blendType = frame.blend;                                            // :631
        sprite.angle = frame.angle;                                                // :632
        sprite.mirror = frame.mirror > 0;                                          // :633
        sprite.opacity = opacity(frame.opacity);                                   // :634
        sprite.visible = true;                                                     // :635
        // :636 `if !frame[VISIBLE]==1 && inEditor` parses as (!frame[VISIBLE])==1,
        // which is false, so :639 always runs.
        sprite.visible = frame.visible == 1;                                       // :639
        int pattern = frame.pattern;                                               // :641
        if (pattern >= 0) {                                                        // :642
            sprite.srcX = (pattern % 5) * ANIM_WIDTH;                              // :644
            sprite.srcY = (pattern / 5) * ANIM_WIDTH;
            sprite.srcWidth = ANIM_WIDTH;
            sprite.srcHeight = ANIM_WIDTH;
        } else {                                                                   // :646
            sprite.srcX = 0;                                                       // :647
            sprite.srcY = 0;
            sprite.srcWidth = bitmapSize != null ? bitmapSize[0] : 128;            // :648
            sprite.srcHeight = bitmapSize != null ? bitmapSize[1] : 128;           // :649
        }
        sprite.zoomX = frame.zoomX / 100.0f;                                       // :651
        sprite.zoomY = frame.zoomY / 100.0f;                                       // :652
        for (int i = 0; i < 4; i++) {
            sprite.color[i] = clamp(frame.color[i], 0, 255);                       // :653-658 Color#set
            sprite.tone[i] = i < 3 ? clamp(frame.tone[i], -255, 255)               // :659-664 Tone#set
                    : clamp(frame.tone[i], 0, 255);
        }
        sprite.ox = sprite.srcWidth / 2;                                           // :665 Integer division
        sprite.oy = sprite.srcHeight / 2;                                          // :666
        sprite.x = (int) frame.x;                                                  // :667
        sprite.y = (int) frame.y;                                                  // :668
        if (sprite != userSprite && sprite != targetSprite) {                      // :669
            switch (frame.priority) {                                              // :670
                case 0:                                                            // :671 Behind everything
                    sprite.z = 10;
                    break;
                case 1:                                                            // :673 In front of everything
                    sprite.z = 80;
                    break;
                case 2:                                                            // :675 Just behind focus
                    switch (frame.focus) {
                        case 1:
                            sprite.z = targetSprite != null ? targetSprite.z - 1 : 20;   // :678
                            break;
                        case 2:
                            sprite.z = userSprite != null ? userSprite.z - 1 : 20;       // :680
                            break;
                        default:
                            sprite.z = 20;                                         // :682
                    }
                    break;
                case 3:                                                            // :684 Just in front of focus
                    switch (frame.focus) {
                        case 1:
                            sprite.z = targetSprite != null ? targetSprite.z + 1 : 80;   // :687
                            break;
                        case 2:
                            sprite.z = userSprite != null ? userSprite.z + 1 : 80;       // :689
                            break;
                        default:
                            sprite.z = 80;                                         // :691
                    }
                    break;
                default:
                    sprite.z = 80;                                                 // :694
            }
        }
    }

    // ------------------------------------------------------------------
    // PBAnimationPlayerX#update (:797-876)
    // ------------------------------------------------------------------

    /** {@code sprite.bitmap = @animbitmap} (:815/:844). */
    private void setSheetBitmap(BattleSprite sprite) {
        sprite.sheetName = "Graphics/Animations/" + animation.graphic;
        sprite.sheetHue = animation.hue;
        sprite.celBattler = null;
    }

    /** {@code sprite.bitmap = @userbitmap} / {@code @targetbitmap} (:840/:842). */
    private void setBattlerBitmap(BattleSprite sprite, BattleSprite from, int[] fromSize) {
        sprite.sheetName = null;
        if (fromSize == null || from == null || sprite == from) {
            sprite.celBattler = null;      // the sprite's own bitmap (or nil, which draws nothing)
            return;
        }
        sprite.celBattler = from.battler;
        sprite.celBack = from.back;
    }

    void update() {
        if (frame < 0) return;                                                     // :798
        int animFrame = frame / FRAMES_PER_TICK;                                   // :799

        // Loop or end the animation if the animation has reached the end
        if (animFrame >= animation.length()) {                                     // :802
            frame = looping ? 0 : -1;                                              // :803
            if (frame < 0) {                                                       // :804
                animBitmapLoaded = false;                                          // :805-806
                return;
            }
        }
        // Load the animation's spritesheet and assign it to all the sprites.
        if (!animBitmapLoaded) {                                                   // :811
            animBitmapLoaded = true;                                               // :812-813
            for (int i = 0; i < MAX_SPRITES; i++) {                                // :814
                if (animSprites[i] != null) {
                    setSheetBitmap(animSprites[i]);                                // :815
                }
            }
        }
        // Update background and foreground graphics
        bgGraphic.update();                                                        // :819
        bgColor.update();                                                          // :820
        foGraphic.update();                                                        // :821
        foColor.update();                                                          // :822

        // Update all the sprites to depict the animation's next frame
        if (FRAMES_PER_TICK == 1 || (frame % FRAMES_PER_TICK) == 0) {              // :825
            BattleAnimationData.Cel[] thisframe = animation.frames[animFrame];     // :826
            // Make all cel sprites invisible
            for (int i = 0; i < MAX_SPRITES; i++) {                                // :828
                if (animSprites[i] != null) {
                    animSprites[i].visible = false;                                // :829
                }
            }
            // Set each cel sprite acoordingly
            for (int i = 0; i < thisframe.length; i++) {                           // :832
                BattleAnimationData.Cel cel = thisframe[i];                        // :833
                if (cel == null) continue;                                         // :834
                BattleSprite sprite = i < MAX_SPRITES ? animSprites[i] : null;     // :835
                if (sprite == null) continue;                                      // :836
                // Set cel sprite's graphic
                int[] bitmapSize = null;
                switch (cel.pattern) {                                             // :838
                    case -1:
                        setBattlerBitmap(sprite, userSprite, userBitmapSize);      // :840
                        bitmapSize = userBitmapSize;
                        break;
                    case -2:
                        setBattlerBitmap(sprite, targetSprite, targetBitmapSize);  // :842
                        bitmapSize = targetBitmapSize;
                        break;
                    default:
                        setSheetBitmap(sprite);                                    // :844
                }
                // Apply settings to the cel sprite
                pbSpriteSetAnimFrame(sprite, cel, bitmapSize);                     // :847
                switch (cel.focus) {                                               // :848
                    case 1:                                                        // :849 Focused on target
                        sprite.x = (int) (cel.x + targetOrig[0] - PokeBattle_SceneConstants.FOCUSTARGET_X);   // :850
                        sprite.y = (int) (cel.y + targetOrig[1] - PokeBattle_SceneConstants.FOCUSTARGET_Y);   // :851
                        break;
                    case 2:                                                        // :852 Focused on user
                        sprite.x = (int) (cel.x + userOrig[0] - PokeBattle_SceneConstants.FOCUSUSER_X);       // :853
                        sprite.y = (int) (cel.y + userOrig[1] - PokeBattle_SceneConstants.FOCUSUSER_Y);       // :854
                        break;
                    case 3: {                                                      // :855 Focused on user and target
                        if (srcLine == null || dstLine == null) continue;          // :856
                        double[] point = transformPoint(
                                srcLine[0], srcLine[1], srcLine[2], srcLine[3],
                                dstLine[0], dstLine[1], dstLine[2], dstLine[3],
                                sprite.x, sprite.y);                               // :857-860
                        sprite.x = (int) point[0];                                 // :861
                        sprite.y = (int) point[1];                                 // :862
                        if (isReversed(srcLine[0], srcLine[2], dstLine[0], dstLine[2])
                                && cel.pattern >= 0) {                             // :863-864
                            sprite.mirror = !sprite.mirror;                        // :866 Reverse direction
                        }
                        break;
                    }
                    default:
                        break;
                }
                // :869-870 `sprite.x += 64 if @inEditor` - never in battle.
            }
            // Play timings
            playTiming(animFrame);                                                 // :873
        }
        frame += 1;                                                                // :875
    }

    // ------------------------------------------------------------------
    // PBAnimation#playTiming (:500-616)
    // ------------------------------------------------------------------

    private void playTiming(int frame) {
        for (BattleAnimationData.Timing i : animation.timings) {                   // :501
            if (i.frame != frame) continue;                                        // :502
            switch (i.type) {                                                      // :503
                case 0:                                                            // :504 Play SE
                    if (i.name != null && !i.name.isEmpty()) {                     // :505
                        host.sePlayer().playSe("Anim/" + i.name, i.volume, i.pitch);   // :506
                    } else {
                        // :508-510: the user's cry; pbCryFile(1) raises and is rescued to nil.
                        String name = user != null && user.pokemon != null ? host.cryFile(user.pokemon) : null;
                        if (name != null) {
                            host.sePlayer().playSe(name, i.volume, i.pitch);       // :510
                        }
                    }
                    break;
                case 1:                                                            // :516 Set background graphic
                    setGraphic(bgGraphic, bgColor, i);
                    break;
                case 2:                                                            // :530 Move/recolour background graphic
                    rememberOld(bgGraphic, bgColor, oldBg);
                    break;
                case 3:                                                            // :542 Set foreground graphic
                    setGraphic(foGraphic, foColor, i);
                    break;
                case 4:                                                            // :556 Move/recolour foreground graphic
                    rememberOld(foGraphic, foColor, oldFo);
                    break;
                default:
                    break;
            }
        }
        for (BattleAnimationData.Timing i : animation.timings) {                   // :570
            switch (i.type) {
                case 2:                                                            // :572
                    recolour(bgGraphic, bgColor, oldBg, i, frame);
                    break;
                case 4:                                                            // :593
                    recolour(foGraphic, foColor, oldFo, i, frame);
                    break;
                default:
                    break;
            }
        }
    }

    /** :517-529 (bg) / :543-555 (fg): {@code Set background/foreground graphic (immediate)}. */
    private void setGraphic(Plane graphic, Plane colorPlane, BattleAnimationData.Timing i) {
        double r = i.colorRed != null ? i.colorRed : 0;
        double g = i.colorGreen != null ? i.colorGreen : 0;
        double b = i.colorBlue != null ? i.colorBlue : 0;
        double a = i.colorAlpha != null ? i.colorAlpha : 0;
        double opacity = i.opacity != null ? i.opacity : 0;
        if (i.name != null && !i.name.isEmpty()) {                                 // :517
            graphic.setBitmap("Graphics/Animations/" + i.name);                    // :518
            // :519 `-i.bgX || 0` raises on a nil bgX; the data always has one.
            graphic.setOx(i.bgX != null ? -i.bgX : 0);                             // :519
            graphic.setOy(i.bgY != null ? -i.bgY : 0);                             // :520
            graphic.setColor(r, g, b, a);                                          // :521
            graphic.setOpacity(opacity);                                           // :522
            colorPlane.setOpacity(0);                                              // :523
        } else {
            graphic.setBitmap(null);                                               // :525
            graphic.setOpacity(0);                                                 // :526
            colorPlane.setColor(r, g, b, a);                                       // :527
            colorPlane.setOpacity(opacity);                                        // :528
        }
    }

    /** :531-541 (bg) / :557-567 (fg). */
    private void rememberOld(Plane graphic, Plane colorPlane, double[] old) {
        if (graphic.hasBitmap()) {                                                 // :531
            old[0] = graphic.ox;                                                   // :532
            old[1] = graphic.oy;                                                   // :533
            old[2] = graphic.sprite.opacity;                                       // :534
            copyColor(graphic.sprite.color, old);                                  // :535
        } else {
            old[0] = 0;                                                            // :537
            old[1] = 0;                                                            // :538
            old[2] = colorPlane.sprite.opacity;                                    // :539
            copyColor(colorPlane.sprite.color, old);                               // :540
        }
    }

    private static void copyColor(float[] color, double[] old) {
        for (int c = 0; c < 4; c++) {
            old[3 + c] = color[c];
        }
    }

    /** :572-592 (bg) / :593-614 (fg). */
    private void recolour(Plane graphic, Plane colorPlane, double[] old,
                          BattleAnimationData.Timing i, int frame) {
        if (i.duration <= 0) return;                                               // :573 / :594
        if (frame < i.frame || frame > i.frame + i.duration) return;               // :574 / :595
        double fraction = (double) (frame - i.frame) / i.duration;                 // :575 / :596
        double cr = i.colorRed != null ? old[3] + (i.colorRed - old[3]) * fraction : old[3];       // :580
        double cg = i.colorGreen != null ? old[4] + (i.colorGreen - old[4]) * fraction : old[4];   // :581
        double cb = i.colorBlue != null ? old[5] + (i.colorBlue - old[5]) * fraction : old[5];     // :582
        double ca = i.colorAlpha != null ? old[6] + (i.colorAlpha - old[6]) * fraction : old[6];   // :583
        if (graphic.hasBitmap()) {                                                 // :576
            if (i.bgX != null) graphic.setOx(old[0] - (i.bgX - old[0]) * fraction);       // :577
            if (i.bgY != null) graphic.setOy(old[1] - (i.bgY - old[1]) * fraction);       // :578
            if (i.opacity != null) graphic.setOpacity(old[2] + (i.opacity - old[2]) * fraction);   // :579
            graphic.setColor(cr, cg, cb, ca);                                      // :584
        } else {
            if (i.opacity != null) colorPlane.setOpacity(old[2] + (i.opacity - old[2]) * fraction);   // :586
            colorPlane.setColor(cr, cg, cb, ca);                                   // :591
        }
    }

    // ------------------------------------------------------------------
    // Test / probe access
    // ------------------------------------------------------------------

    BattleSprite celSprite(int index) {
        return animSprites[index];
    }

    BattleSprite bgColorSprite() {
        return bgColor.sprite;
    }

    BattleSprite bgGraphicSprite() {
        return bgGraphic.sprite;
    }

    BattleSprite foColorSprite() {
        return foColor.sprite;
    }

    BattleSprite foGraphicSprite() {
        return foGraphic.sprite;
    }
}
