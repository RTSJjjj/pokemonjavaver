package pokemon.runtime.battle;

/**
 * One entry of the battle scene's {@code @sprites} hash: the mutable sprite
 * state the plugin's {@code PictureEx} animations drive
 * ({@code PokeBattle_Animation:20-31 addSprite} copies x/y/z/visible/tone out of
 * it and {@code PictureEx:454-519 setPictureIconSprite} writes everything back).
 *
 * <p>Coordinates follow the plugin: {@code (x,y)} is where the sprite's origin
 * lands, in RMXP's top-left origin space, and {@code ox}/{@code oy} are derived
 * from {@link #origin} and the source rectangle exactly like
 * {@code setPictureSprite:483-498} does.</p>
 */
final class BattleSprite {

    /** How the renderer draws this entry. */
    enum Kind {
        /** {@code battle_bg} / {@code battle_bg2} (Scene_Initialize:216-220). */
        BACKDROP,
        /** {@code base_0} / {@code base_1} (Scene_Initialize:221-230). */
        BASE,
        /** {@code player_N}: pbCreateTrainerBackSprite (Scene_Initialize:235-251). */
        TRAINER_BACK,
        /** {@code trainer_N}: pbCreateTrainerFrontSprite (Scene_Initialize:253-262). */
        TRAINER_FRONT,
        /** {@code pokemon_N}: PokemonBattlerSprite. */
        POKEMON,
        /** {@code shadow_N}: PokemonBattlerShadowSprite. */
        SHADOW,
        /** {@code dataBox_N}: PokemonDataBox. */
        DATA_BOX,
        /** A plain {@code pbAddSprite}/IconSprite whose bitmap is {@link #name}. */
        IMAGE,
        /**
         * One of the 58 extra {@code Sprite.new(@viewport)} cel sprites of a
         * {@code PBAnimationPlayerX} (PokeBattle_AnimationPlayer:736-740).
         */
        ANIM_CEL,
        /** {@code ColoredPlane} (Planes:165-183): a black bitmap tinted by {@link #color}. */
        PLANE_COLOR,
        /** {@code AnimatedPlane} (Planes:190-232): {@link #name} tiled from ({@link #ox},{@link #oy}). */
        PLANE_BITMAP,
        /** {@code abilityBar_N}: AbilitySplashBar (PokeBattle_SceneElements:405-497), the bar and its two text lines. */
        ABILITY_BAR,
        /** {@code dataBox_0} of a Safari Zone battle: SafariDataBox (160_PokeBattle_SafariZone:~45-74). */
        SAFARI_BOX
    }

    final String key;
    final Kind kind;

    // ---- the fields PictureEx writes (PictureEx:77-93) ----
    float x;
    float y;
    /** A data box's resting x (PokemonDataBox#updatePositions resets {@code self.x = @spriteX}). */
    float homeX = Float.NaN;
    int z;
    float zoomX = 1f;
    float zoomY = 1f;
    float angle;
    float[] tone = { 0f, 0f, 0f, 0f };
    float[] color = { 0f, 0f, 0f, 0f };
    float opacity = 255f;
    boolean visible = true;
    /** PokemonBattlerSprite#update (PokeBattle_SceneElements:623-628): hidden for a blink frame while it is the chosen target. */
    boolean blinkHidden;
    /** 345_Transform_Mosaic: the Pokemon whose picture is drawn instead of the battler's while its sprite pixelates (null = the battler's). */
    pokemon.runtime.pokemon.Pokemon lookOverride;
    /** 345_Transform_Mosaic {@code PokemonBattlerSpriteTransform#mosaic}: the block size in pixels (0 = none). */
    float mosaic;
    int blendType;
    /** The plugin's full logical path, e.g. {@code Graphics/Battle animations/ball_00}. */
    String name = "";
    /** ABILITY_BAR: the two lines of text and the side the bar belongs to. */
    String barLine1 = "";
    String barLine2 = "";
    int barSide;
    int origin = PictureEx.Origin.TOP_LEFT;
    /** {@code src_rect}; a negative width/height means "the whole bitmap". */
    int srcX;
    int srcY;
    int srcWidth = -1;
    int srcHeight = -1;
    int cropBottom = -1;
    /**
     * {@code sprite.ox} / {@code sprite.oy}. RMXP keeps these as plain values:
     * a sprite class sets them once ({@code pbSetOrigin},
     * Scene_Initialize:227-229) and {@code setPictureSprite:483-498} rewrites
     * them whenever an origin process runs. An animation that passes
     * {@code origin = nil} ({@code makeSlideSprite}'s default,
     * PokeBattle_SceneAnimations:50-57) leaves them alone.
     */
    float ox;
    float oy;
    /** The bitmap this sprite draws; -1 while it has none ({@code self.bitmap == nil}). */
    int bitmapWidth = -1;
    int bitmapHeight = -1;

    // ---- the bitmap a PBAnimationPlayerX swaps in (`sprite.bitmap = ...`) ----
    /**
     * {@code sprite.bitmap = @animbitmap} (PokeBattle_AnimationPlayer:815/844):
     * the animation sheet's logical path ({@code Graphics/Animations/<graphic>}),
     * or null when the sprite shows its own bitmap.
     */
    String sheetName;
    /** The sheet's {@code animation.hue} (AnimatedBitmap hue argument, :812-813). */
    int sheetHue;
    /**
     * {@code sprite.bitmap = @userbitmap / @targetbitmap} (:840/:842): the
     * battler whose Pokemon bitmap this sprite shows instead of its own; null =
     * none. An ANIM_CEL has no bitmap of its own, a POKEMON sprite does.
     */
    Battler celBattler;
    boolean celBack;

    // ---- per-kind payload ----
    /** POKEMON / SHADOW / DATA_BOX. */
    Battler battler;
    /** TRAINER_BACK / TRAINER_FRONT: 1-based index as in the plugin's sprite key. */
    int idxTrainer;
    /** POKEMON / TRAINER_BACK: this side is drawn with the back graphic. */
    boolean back;
    /** pbCreateTrainerBackSprite:241 / pbCreateTrainerFrontSprite mirror flag. */
    boolean mirror;
    /** The trainer type id whose file this sprite was created from. */
    String trainerFile;
    /**
     * {@code @spriteYExtra} / {@code @spriteXExtra}
     * (PokeBattle_SceneElements:376-386 for the data box, :605-618 for the
     * Pokemon sprite): the "bobbing" offset a sprite class applies on top of the
     * position an animation set. Added at draw time, exactly like
     * {@code self.y = self.y} re-applying it every frame.
     */
    float bobOffsetY;

    BattleSprite(String key, Kind kind) {
        this.key = key;
        this.kind = kind;
    }

    /** The bitmap's width as {@code self.bitmap.width} would report it. */
    int bitmapWidth() {
        return Math.max(0, bitmapWidth);
    }

    int bitmapHeight() {
        return Math.max(0, bitmapHeight);
    }

    /** The source rectangle's width, falling back to the whole bitmap. */
    int sourceWidth() {
        return srcWidth >= 0 ? srcWidth : bitmapWidth();
    }

    int sourceHeight() {
        return srcHeight >= 0 ? srcHeight : bitmapHeight();
    }

    /**
     * {@code setPictureSprite:483-498}: {@code ox}/{@code oy} from the origin and
     * the sprite's current source rectangle.
     */
    void updateOrigin() {
        switch (origin) {
            case PictureEx.Origin.TOP_LEFT:
            case PictureEx.Origin.LEFT:
            case PictureEx.Origin.BOTTOM_LEFT:
                ox = 0;                                         // :485
                break;
            case PictureEx.Origin.TOP:
            case PictureEx.Origin.CENTER:
            case PictureEx.Origin.BOTTOM:
                ox = sourceWidth() / 2f;                        // :487
                break;
            case PictureEx.Origin.TOP_RIGHT:
            case PictureEx.Origin.RIGHT:
            case PictureEx.Origin.BOTTOM_RIGHT:
                ox = sourceWidth();                             // :489
                break;
            default:
                break;                                          // origin nil: keep ox
        }
        switch (origin) {
            case PictureEx.Origin.TOP_LEFT:
            case PictureEx.Origin.TOP:
            case PictureEx.Origin.TOP_RIGHT:
                oy = 0;                                         // :493
                break;
            case PictureEx.Origin.LEFT:
            case PictureEx.Origin.CENTER:
            case PictureEx.Origin.RIGHT:
                oy = sourceHeight() / 2f;                       // :495
                break;
            case PictureEx.Origin.BOTTOM_LEFT:
            case PictureEx.Origin.BOTTOM:
            case PictureEx.Origin.BOTTOM_RIGHT:
                oy = sourceHeight();                            // :497
                break;
            default:
                break;
        }
    }
}
