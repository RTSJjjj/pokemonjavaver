package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code PokeBattle_DamageState}
 * (PokeBattle_DamageState.rb:1-56) - the scratch state of one move usage: how
 * much damage was calculated, what the type effectiveness was, whether the move
 * missed, whether a Focus Sash/Sturdy/Disguise/... saved the target, and so on.
 *
 * <p>All 22 {@code attr_accessor}s of the Ruby class are transcribed as public
 * fields, one Ruby line each (PokeBattle_DamageState.rb:2-24); the class keeps
 * the plugin's camelCase names so every use site can be diffed against the Ruby
 * line that produced it. {@code @protected} cannot be spelled {@code protected}
 * in Java, so it becomes {@link #protectedFlag} (see its javadoc).</p>
 *
 * <p>{@code initialize} is {@code reset} (PokeBattle_DamageState.rb:26), and
 * {@code reset} clears the cumulative slots then calls {@code resetPerHit}
 * (:28-39) - the per-hit slots (missed, calcDamage, hpLost, ...) are the ones
 * cleared before every hit of a multi-hit move.</p>
 */
public final class DamageState {

    /** {@code @initialHP} (PokeBattle_DamageState.rb:2). */
    public int initialHP;
    /** {@code @typeMod} - type effectiveness (PokeBattle_DamageState.rb:3). */
    public int typeMod;
    /** {@code @unaffected} (PokeBattle_DamageState.rb:4). */
    public boolean unaffected;
    /**
     * Ruby {@code @protected} (PokeBattle_DamageState.rb:5) - "protected by a
     * protective move". Renamed only because {@code protected} is a Java
     * keyword; every use site maps 1:1 to the Ruby {@code @protected}.
     */
    public boolean protectedFlag;
    /** {@code @magicCoat} (PokeBattle_DamageState.rb:6). */
    public boolean magicCoat;
    /** {@code @magicBounce} (PokeBattle_DamageState.rb:7). */
    public boolean magicBounce;
    /** {@code @totalHPLost} - like hpLost, but cumulative over all hits (PokeBattle_DamageState.rb:8). */
    public int totalHPLost;
    /** {@code @fainted} - whether the battler was knocked out by the move (PokeBattle_DamageState.rb:9). */
    public boolean fainted;

    /** {@code @missed} - whether the move failed the accuracy check (PokeBattle_DamageState.rb:11). */
    public boolean missed;
    /** {@code @calcDamage} - calculated damage (PokeBattle_DamageState.rb:12). */
    public int calcDamage;
    /** {@code @hpLost} - HP lost by opponent, inc. HP lost by a substitute (PokeBattle_DamageState.rb:13). */
    public int hpLost;
    /** {@code @critical} - critical hit flag (PokeBattle_DamageState.rb:14). */
    public boolean critical;
    /** {@code @substitute} - whether a substitute took the damage (PokeBattle_DamageState.rb:15). */
    public boolean substitute;
    /** {@code @focusBand} - Focus Band used (PokeBattle_DamageState.rb:16). */
    public boolean focusBand;
    /** {@code @focusSash} - Focus Sash used (PokeBattle_DamageState.rb:17). */
    public boolean focusSash;
    /** {@code @sturdy} - Sturdy ability used (PokeBattle_DamageState.rb:18). */
    public boolean sturdy;
    /** {@code @disguise} - Disguise ability used (PokeBattle_DamageState.rb:19). */
    public boolean disguise;
    /** {@code @endured} - damage was endured (PokeBattle_DamageState.rb:20). */
    public boolean endured;
    /** {@code @berryWeakened} - whether a type-resisting berry was used (PokeBattle_DamageState.rb:21). */
    public boolean berryWeakened;
    /** {@code @iceface} - Ice Face ability used (PokeBattle_DamageState.rb:22). */
    public boolean iceface;
    /** {@code @flameveil} - FLAMEVEIL (PokeBattle_DamageState.rb:23). */
    public boolean flameveil;
    /** {@code @terashell} - Tera Shell ability used (PokeBattle_DamageState.rb:24). */
    public boolean terashell;

    /**
     * {@code attr_accessor :bossHP} (PokeBattle_BOSS:26): how many BOSS "overflow"
     * stages this battler's damage state has gone through. Ruby's reopened
     * {@code initialize} (PokeBattle_BOSS:28-32) sets it to 0 once, <b>after</b>
     * {@code reset}, so {@link #reset()} deliberately does not touch it.
     */
    public int bossHP;

    /** {@code def initialize; reset; end} (PokeBattle_DamageState.rb:26). */
    public DamageState() {
        reset();
    }

    /** {@code reset} (PokeBattle_DamageState.rb:28-39). */
    public void reset() {
        initialHP = 0;          // :29
        typeMod = 0;            // :30
        unaffected = false;     // :31
        protectedFlag = false;  // :32
        magicCoat = false;      // :33
        magicBounce = false;    // :34
        totalHPLost = 0;        // :35
        fainted = false;        // :36
        terashell = false;      // :37
        resetPerHit();          // :38
    }

    /** {@code resetPerHit} (PokeBattle_DamageState.rb:41-55). */
    public void resetPerHit() {
        missed = false;         // :42
        calcDamage = 0;         // :43
        hpLost = 0;             // :44
        critical = false;       // :45
        substitute = false;     // :46
        focusBand = false;      // :47
        focusSash = false;      // :48
        sturdy = false;         // :49
        disguise = false;       // :50
        endured = false;        // :51
        berryWeakened = false;  // :52
        iceface = false;        // :53
        flameveil = false;      // :54
    }

    /**
     * Stage 4 / M0: {@code PokeBattle_SuccessState}
     * (PokeBattle_DamageState.rb:63-90) - "used for Battle Arena", as the Ruby
     * comment at :61 says. Kept as a nested class of {@link DamageState} because
     * both live in the same Ruby section.
     */
    public static final class SuccessState {

        /** {@code @typeMod} (PokeBattle_DamageState.rb:64). */
        public int typeMod;
        /** {@code @useState} - 0 - not used, 1 - failed, 2 - succeeded (PokeBattle_DamageState.rb:65). */
        public int useState;
        /**
         * Ruby {@code @protected} (PokeBattle_DamageState.rb:66); renamed to
         * {@link #protectedFlag} because {@code protected} is a Java keyword.
         */
        public boolean protectedFlag;
        /** {@code @skill} (PokeBattle_DamageState.rb:67). */
        public int skill;

        /** {@code def initialize; clear; end} (PokeBattle_DamageState.rb:69). */
        public SuccessState() {
            clear();
        }

        /** Ruby's default argument {@code full=true} of {@code clear} (PokeBattle_DamageState.rb:71). */
        public void clear() {
            clear(true);
        }

        /** {@code clear(full=true)} (PokeBattle_DamageState.rb:71-76). */
        public void clear(boolean full) {
            typeMod = PBTypeEffectiveness.NORMAL_EFFECTIVE;   // :72
            useState = 0;                                     // :73
            protectedFlag = false;                            // :74
            if (full) {
                skill = 0;                                    // :75
            }
        }

        /**
         * {@code updateSkill} (PokeBattle_DamageState.rb:78-88).
         *
         * <p>The Ruby calls {@code PBTypes.superEffective?(@typeMod)} etc. with a
         * single argument, i.e. with {@code targetType1} left as nil, so it takes
         * the one-argument branch of each helper
         * (PBTypes_Extra.rb:62/68/80/86). Those helpers do not exist in this
         * runtime yet ({@code PBTypes} has not been ported), so their one-argument
         * semantics are inlined here, branch for branch:</p>
         * <ul>
         * <li>{@code superEffective?(attackType)} - {@code attackType > NORMAL_EFFECTIVE} (PBTypes_Extra.rb:86-87)</li>
         * <li>{@code normalEffective?(attackType)} - {@code attackType == NORMAL_EFFECTIVE} (PBTypes_Extra.rb:80-81)</li>
         * <li>{@code notVeryEffective?(attackType)} - {@code attackType > INEFFECTIVE && attackType < NORMAL_EFFECTIVE} (PBTypes_Extra.rb:68-69)</li>
         * <li>anything else - ineffective (the Ruby {@code else} at :85)</li>
         * </ul>
         */
        public void updateSkill() {
            if (useState == 1) {                    // :79
                if (!protectedFlag) {               // :80
                    skill = -2;
                }
            } else if (useState == 2) {             // :81
                if (typeMod > PBTypeEffectiveness.NORMAL_EFFECTIVE) {          // :82
                    skill = 2;
                } else if (typeMod == PBTypeEffectiveness.NORMAL_EFFECTIVE) {  // :83
                    skill = 1;
                } else if (typeMod > PBTypeEffectiveness.INEFFECTIVE
                        && typeMod < PBTypeEffectiveness.NORMAL_EFFECTIVE) {   // :84
                    skill = -1;
                } else {                                                       // :85 Ineffective
                    skill = -2;
                }
            }
            clear(false);                           // :88
        }
    }
}
