package pokemon.runtime.battle;

/**
 * {@code PokeBattle_SceneConstants} (68 lines), transcribed for the battle
 * scene's sprite placement.
 *
 * <p>{@code Graphics.width}/{@code Graphics.height} are 672x448 in this project
 * - the size of {@code Graphics/Battle animations/black_screen.png} and of the
 * backdrop graphics - so {@code PLAYER_BASE_X} is 128, {@code FOE_BASE_X} is
 * 672-128 = 544, {@code PLAYER_BASE_Y} is 448/2+88+4 = 316 and
 * {@code FOE_BASE_Y} is 448/2+88 = 312.</p>
 */
public final class PokeBattle_SceneConstants {

    /**
     * {@code USE_ABILITY_SPLASH} (PokeBattle_SceneConstants.rb:2): {@code true}
     * in this project. Every {@code if PokeBattle_SceneConstants::USE_ABILITY_SPLASH}
     * in the battle code therefore takes the first branch and the
     * {@code else} (the wording that spells out {@code abilityName}) is dead
     * code - 30+ sites across the ability/item handlers. Transcribed so those
     * handlers can be ported line for line; keep the constant, do not inline it.
     */
    public static final boolean USE_ABILITY_SPLASH = true;

    /** {@code NUM_BALLS} (:8): party balls shown in each side's lineup. */
    static final int NUM_BALLS = 6;

    /** {@code PLAYER_BASE_X} (:12). */
    static final float PLAYER_BASE_X = 128;
    /** {@code PLAYER_BASE_Y} = Graphics.height/2 + 88 + 4 (:13). */
    static final float PLAYER_BASE_Y = PictureEx.Graphics.HEIGHT / 2f + 88 + 4;
    /** {@code FOE_BASE_X} = Graphics.width - 128 (:18). */
    static final float FOE_BASE_X = PictureEx.Graphics.WIDTH - 128;
    /** {@code FOE_BASE_Y} = Graphics.height/2 + 88 (:19). */
    static final float FOE_BASE_Y = PictureEx.Graphics.HEIGHT / 2f + 88;

    private PokeBattle_SceneConstants() {
    }

    /** {@code pbBattlerPosition} (:24-40): centre bottom of a battler's sprite. */
    static float[] battlerPosition(int index, int sideSize) {
        // Start at the centre of the base for the appropriate side
        float x;
        float y;
        if ((index & 1) == 0) {                                  // :26
            x = PLAYER_BASE_X;
            y = PLAYER_BASE_Y;
        } else {                                                 // :27
            x = FOE_BASE_X;
            y = FOE_BASE_Y;
        }
        // Shift depending on index (no shifting needed for sideSize of 1)
        switch (sideSize) {                                      // :31
            case 2:
                x += new int[] { 48, -48, -64, 64 }[index];      // :33
                y += new int[] { 8, 8, -8, -8 }[index];          // :34
                break;
            case 3:
                x += new int[] { -88, 88, 8, -8, 104, -104 }[index];   // :36
                y += new int[] { 8, 8, 8, 8, 8, 8 }[index];            // :37
                break;
            default:
                break;
        }
        return new float[] { x, y };                             // :39
    }

    /** {@code pbTrainerPosition} (:44-59): centre bottom of a trainer's sprite. */
    static float[] trainerPosition(int side, int index, int sideSize) {
        float x;
        float y;
        if (side == 0) {                                         // :46
            x = PLAYER_BASE_X;
            y = PLAYER_BASE_Y - 16;
        } else {                                                 // :47
            x = FOE_BASE_X;
            y = FOE_BASE_Y + 6;
        }
        switch (sideSize) {                                      // :50
            case 2:
                x += new int[] { -48, 48, 32, -32 }[2 * index + side];   // :52
                y += new int[] { 0, 0, 0, -16 }[2 * index + side];       // :53
                break;
            case 3:
                x += new int[] { -80, 80, 0, 0, 80, -80 }[2 * index + side];   // :55
                y += new int[] { 0, 0, 0, -8, 0, -16 }[2 * index + side];      // :56
                break;
            default:
                break;
        }
        return new float[] { x, y };                             // :58
    }

    /**
     * Default focal points of the user and the target in animations
     * (PokeBattle_SceneConstants:61-66, "do not change!"): the centre middle of
     * each sprite.
     */
    static final int FOCUSUSER_X = 128;                          // :63
    static final int FOCUSUSER_Y = 224;                          // :64
    static final int FOCUSTARGET_X = 384;                        // :65
    static final int FOCUSTARGET_Y = 96;                         // :66
}
