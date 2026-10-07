package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code PBTypeEffectiveness} (PBTypes_Extra.rb:1-7), transcribed
 * one constant at a time.
 *
 * <p>These are the per-type effectiveness factors the plugin multiplies
 * together: a move that misses the target's type entirely is 0, "not very
 * effective" is 1, normal is 2 and super effective is 4 - each factor is
 * <em>per target type</em>. {@code PBTypes.getCombinedEffectiveness}
 * (PBTypes_Extra.rb:47-59) returns {@code mod1*mod2*mod3}, i.e. the PRODUCT of
 * one factor per type of the defender, so a neutral hit on a single-type target
 * is 2 and the "normal" total is {@code 2**3} = {@link #NORMAL_EFFECTIVE} = 8
 * (PBTypes_Extra.rb:6). That is why every {@code PBTypes.*Effective?} helper
 * compares against 8 and not against 2 (PBTypes_Extra.rb:62-89).</p>
 */
public final class PBTypeEffectiveness {

    private PBTypeEffectiveness() {
    }

    /** {@code PBTypeEffectiveness::INEFFECTIVE} (PBTypes_Extra.rb:2). */
    public static final int INEFFECTIVE = 0;

    /** {@code PBTypeEffectiveness::NOT_EFFECTIVE_ONE} (PBTypes_Extra.rb:3). */
    public static final int NOT_EFFECTIVE_ONE = 1;

    /** {@code PBTypeEffectiveness::NORMAL_EFFECTIVE_ONE} (PBTypes_Extra.rb:4). */
    public static final int NORMAL_EFFECTIVE_ONE = 2;

    /** {@code PBTypeEffectiveness::SUPER_EFFECTIVE_ONE} (PBTypes_Extra.rb:5). */
    public static final int SUPER_EFFECTIVE_ONE = 4;

    /**
     * {@code PBTypeEffectiveness::NORMAL_EFFECTIVE} (PBTypes_Extra.rb:6):
     * Ruby {@code NORMAL_EFFECTIVE_ONE ** 3}. The {@code **} is
     * <b>exponentiation</b>, not a multiplication, so this is 2*2*2 = <b>8</b>
     * and never 6. It is kept as a compile-time constant expression (it is used
     * in {@code switch} labels / constant comparisons elsewhere in the plugin),
     * with the exponentiation spelled out literally here.
     */
    public static final int NORMAL_EFFECTIVE =
            NORMAL_EFFECTIVE_ONE * NORMAL_EFFECTIVE_ONE * NORMAL_EFFECTIVE_ONE;   // PBTypes_Extra.rb:6 ** 3
}
