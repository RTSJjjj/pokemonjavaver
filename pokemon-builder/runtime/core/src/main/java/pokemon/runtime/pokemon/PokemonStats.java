package pokemon.runtime.pokemon;

/**
 * Stage 3 / P0: the Essentials stat, experience and gender formulas, kept pure
 * so battle, party and the menu screens share one implementation.
 *
 * <p>Stat order everywhere is the PBS order: HP, ATTACK, DEFENSE, SPEED,
 * SPATK, SPDEF (indices 0..5).</p>
 */
public final class PokemonStats {

    public static final int HP = 0;
    public static final int ATTACK = 1;
    public static final int DEFENSE = 2;
    public static final int SPEED = 3;
    public static final int SPATK = 4;
    public static final int SPDEF = 5;

    public static final int MALE = 0;
    public static final int FEMALE = 1;
    public static final int GENDERLESS = 2;

    private PokemonStats() {
    }

    /** HP = (2*base + IV + EV/4) * level / 100 + level + 10. */
    public static int maxHp(int baseHp, int iv, int ev, int level) {
        return (int) (((2L * baseHp + iv + ev / 4) * level) / 100) + level + 10;
    }

    /**
     * Non-HP stat = ((2*base + IV + EV/4) * level / 100 + 5) * nature, floored
     * like the project's Ruby ({@code (stat*1.1).floor}).
     */
    public static int stat(int base, int iv, int ev, int level, float natureMultiplier) {
        int raw = (int) (((2L * base + iv + ev / 4) * level) / 100) + 5;
        return (int) Math.floor(raw * natureMultiplier);
    }

    /** 1.1 for the raised stat, 0.9 for the lowered one, 1 otherwise. */
    public static float natureMultiplier(PbsData.Nature nature, int statIndex) {
        if (nature == null || nature.neutral()) {
            return 1f;
        }
        int up = statIndexOf(nature.statUp);
        int down = statIndexOf(nature.statDown);
        if (statIndex == up) {
            return 1.1f;
        }
        if (statIndex == down) {
            return 0.9f;
        }
        return 1f;
    }

    /** "ATTACK" -> ATTACK index; unknown/neutral names -> -1. */
    public static int statIndexOf(String statName) {
        if (statName == null) {
            return -1;
        }
        switch (statName.toUpperCase(java.util.Locale.ROOT)) {
            case "HP":
                return HP;
            case "ATTACK":
                return ATTACK;
            case "DEFENSE":
                return DEFENSE;
            case "SPEED":
                return SPEED;
            case "SPATK":
            case "SPECIAL":
            case "SP. ATK":
                return SPATK;
            case "SPDEF":
            case "SP. DEF":
                return SPDEF;
            default:
                return -1;
        }
    }

    /**
     * Experience needed to reach {@code level} for one PBS growth rate. The
     * formulas and their integer arithmetic match the project's PBExperience.
     */
    public static int experienceForLevel(String growthRate, int level) {
        if (level > 100) {
            return PBExperience.pbGetStartExperience(level, growthRate);   // 087_PBExperience:117-147 above the table
        }
        if (level <= 1) {
            return 0;
        }
        long n = level;
        long cube = n * n * n;
        switch (growthRate == null ? "Medium" : growthRate) {
            case "Fast":
                return (int) (4 * cube / 5);
            case "Slow":
                return (int) (5 * cube / 4);
            case "Parabolic":
                return (int) (6 * cube / 5 - 15 * n * n + 100 * n - 140);
            case "Erratic":
                if (n <= 50) {
                    return (int) (cube * (100 - n) / 50);
                }
                if (n <= 68) {
                    return (int) (cube * (150 - n) / 100);
                }
                if (n <= 98) {
                    return (int) (cube * ((1911 - 10 * n) / 3) / 500);
                }
                return (int) (cube * (160 - n) / 100);
            case "Fluctuating":
                if (n <= 15) {
                    return (int) (cube * ((n + 1) / 3 + 24) / 50);
                }
                if (n <= 35) {
                    return (int) (cube * (n + 14) / 50);
                }
                return (int) (cube * (n / 2 + 32) / 50);
            case "Medium":
            default:
                return (int) cube;
        }
    }

    /**
     * Level for a cumulative experience total, by walking the growth curve up.
     * Used by level-up; the cap is the project's {@code MAXIMUM_LEVEL} guard,
     * kept at 100 here because the runtime does not read Settings yet.
     */
    public static int levelForExperience(String growthRate, int experience) {
        return Math.max(1, PBExperience.pbGetLevelFromExperience(experience, growthRate));
    }

    /**
     * Gender from the PBS rate name and a random roll in [0, 1). The names are
     * the ones this project uses (AlwaysMale/AlwaysFemale/Genderless and the
     * three percentage spellings).
     */
    public static int gender(String genderRate, float roll) {
        if (genderRate == null) {
            return GENDERLESS;
        }
        switch (genderRate) {
            case "AlwaysMale":
                return MALE;
            case "AlwaysFemale":
                return FEMALE;
            case "Genderless":
                return GENDERLESS;
            case "Female25Percent":
                return roll < 0.75f ? MALE : FEMALE;
            case "Female75Percent":
                return roll < 0.25f ? MALE : FEMALE;
            case "Female50Percent":
            default:
                return roll < 0.5f ? MALE : FEMALE;
        }
    }

    /** True when the rate admits only one gender (always-male/female/genderless). */
    public static boolean singleGender(String genderRate) {
        return "AlwaysMale".equals(genderRate) || "AlwaysFemale".equals(genderRate)
                || "Genderless".equals(genderRate);
    }

    /**
     * PBGenderRates.genderByte (PBGenderRates:11-23): the threshold a Pokemon's
     * personal id is compared against to pick a mixed species' gender.
     */
    public static int genderByte(String genderRate) {
        if (genderRate == null) {
            return 255;
        }
        switch (genderRate) {
            case "AlwaysMale": return 0;
            case "FemaleOneEighth": return 32;
            case "Female25Percent": return 64;
            case "Female50Percent": return 128;
            case "Female75Percent": return 192;
            case "FemaleSevenEighths": return 224;
            case "AlwaysFemale": return 254;
            default: return 255;   // Genderless
        }
    }
}
