package pokemon.runtime.pokemon;

/**
 * 087_PBExperience: the experience tables. Up to level 100 the plugin reads its table, which equals the
 * growth-rate formulas of {@link PokemonStats#experienceForLevel} (checked entry by entry for all six rates);
 * above level 100 {@code pbGetExpInternal} (:117-147) uses its own formulas, so levels up to
 * {@link #MAXIMUM_LEVEL} exist (the project's {@code MAX_LEVEL} level locks go up to 200).
 */
public final class PBExperience {
    /** 000_Settings: {@code MAXIMUM_LEVEL}, returned by {@code PBExperience.maxLevel}. */
    public static final int MAXIMUM_LEVEL = 210;
    /** 000_Settings:29 {@code MAX_LEVEL}: the level cap before each badge (and the league, then the cap of the game). */
    public static final int[] MAX_LEVEL = {
            25, 16, 32, 36, 42, 48, 58, 63, 85, 100, 113, 125, 138, 150, 163, 175, 188, 200,
    };

    private PBExperience() {
    }

    /** {@code MAX_LEVEL[index]} with Ruby's negative index ({@code -1} is the last entry). */
    public static int maxLevelAt(int index) {
        return MAX_LEVEL[index < 0 ? MAX_LEVEL.length + index : index];
    }

    /** {@code PBExperience.maxLevel}. */
    public static int maxLevel() {
        return MAXIMUM_LEVEL;
    }

    /** {@code pbGetExpInternal(level, growth)} (:117-147); {@code growth} is the PBS growth rate name. */
    public static int pbGetExpInternal(int level, String growth) {
        if (level <= 0) {
            return -1;                                                             // the table's first entry
        }
        if (level <= 100) {
            return PokemonStats.experienceForLevel(growth, level);                 // :119-121 table lookup
        }
        long n = level;
        switch (growth == null ? "Medium" : growth) {
            case "Erratic":
                return (int) Math.floor(((double) n * n * n * n) * 0.6 / 100);     // :126 ((level ** 4) * 0.6 / 100).floor
            case "Fluctuating": {
                long rate = 82;                                                    // :128
                rate -= (n - 100) / 2;                                             // :130
                if (rate < 40) {
                    rate = 40;                                                     // :131
                }
                return (int) Math.floor((double) (n * n * n) * (n * rate / 100) / 50.0);   // :133
            }
            case "Parabolic":
                return (int) (n * n * n * 6 / 5 - 15 * n * n + 100 * n - 140);     // :135
            case "Fast":
                return (int) (n * n * n * 4 / 5);                                  // :137
            case "Slow":
                return (int) (n * n * n * 5 / 4);                                  // :139
            case "Medium":
            default:
                return (int) (n * n * n);                                          // :123
        }
    }

    /** {@code pbGetMaxExperience(growth)} (:151-156). */
    public static int pbGetMaxExperience(String growth) {
        return pbGetExpInternal(MAXIMUM_LEVEL, growth);
    }

    /** {@code pbGetStartExperience(level, growth)} (:161-170): the level is limited to the maximum level. */
    public static int pbGetStartExperience(int level, String growth) {
        if (level > MAXIMUM_LEVEL) {
            level = MAXIMUM_LEVEL;                                                 // :168
        }
        return pbGetExpInternal(level, growth);
    }

    /** {@code pbAddExperience(currexp, expgain, growth)} (:175-182): the total never exceeds the maximum. */
    public static int pbAddExperience(int currexp, int expgain, String growth) {
        int exp = currexp + expgain;
        int maxexp = pbGetExpInternal(MAXIMUM_LEVEL, growth);
        return Math.min(exp, maxexp);
    }

    /** {@code pbGetLevelFromExperience(exp, growth)} (:186-199). */
    public static int pbGetLevelFromExperience(int exp, String growth) {
        int maxexp = pbGetExpInternal(MAXIMUM_LEVEL, growth);
        if (exp > maxexp) {
            exp = maxexp;
        }
        for (int lvl = 0; lvl <= MAXIMUM_LEVEL; lvl++) {
            int currentExp = pbGetExpInternal(lvl, growth);
            if (exp == currentExp) {
                return lvl;
            }
            if (exp < currentExp) {
                return lvl - 1;
            }
        }
        return MAXIMUM_LEVEL;
    }
}
