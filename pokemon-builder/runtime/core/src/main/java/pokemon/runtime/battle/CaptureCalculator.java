package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.TrainerState;

import java.util.Random;

/**
 * The Poké Ball capture calculation of {@code PokeBattle_BattleCommon:170-232}
 * ({@code pbCaptureCalc}) together with the ball handlers of
 * {@code PokeBall_CatchEffects:57-257} ({@code BallHandlers}).
 *
 * <p>The plugin's chain is: the species' rareness, {@code modifyCatchRate}
 * (badge / Pokédex bonus, then the ball's own multiplier), the HP and status
 * factor, then {@code y = 65536 / ((255/x)^0.1875)} with up to four independent
 * {@code rand(65536) < y} shakes - four shakes is the capture.
 * {@code ENABLE_CRITICAL_CAPTURES} is false (Settings:164), so the critical
 * capture branch (:206-224) is not part of this project.</p>
 */
public final class CaptureCalculator {

    /** PokeBall_CatchEffects:113-117: NEWEST_BATTLE_MECHANICS (Settings:160) is true. */
    private static final double NET_BALL_MULTIPLIER = 3.5;
    /** :131-135. */
    private static final double REPEAT_BALL_MULTIPLIER = 3.5;
    /** :143-147 {@code multiplier = (NEWEST) ? 3 : 3.5}. */
    private static final double DUSK_BALL_MULTIPLIER = 3;
    /** :149-153. */
    private static final double QUICK_BALL_MULTIPLIER = 4;
    /** :174-180. */
    private static final double LURE_BALL_MULTIPLIER = 5;
    /** :119-122. */
    private static final double DIVE_BALL_MULTIPLIER = 3.5;
    /** :240-242 / :72-75. */
    private static final double PET_BALL_MULTIPLIER = 1.5;
    private static final double SPORT_BALL_MULTIPLIER = 1.5;

    /** PokeBall_CatchEffects:177-187: the Ultra Beast species of pbCaptureCalc. */
    private static final String[] ULTRA_BEASTS = {
        "NIHILEGO", "BUZZWOLE", "PHEROMOSA", "XURKITREE", "CELESTEELA", "KARTANA",
        "GUZZLORD", "POIPOLE", "NAGANADEL", "STAKATAKA", "BLACEPHALON",
    };

    private CaptureCalculator() {
    }

    /** Everything the calculation asks the battle for. */
    public static final class Context {
        public PbsData pbs;
        public TrainerState trainer;
        /** The wild Pokemon that was targeted. */
        public Pokemon target;
        public int targetHp;
        public int targetMaxHp;
        /** PBStatuses name ("SLEEP"/"FROZEN"/"POISON"/...); empty = none. */
        public String targetStatus = "";
        /** {@code pbGetSpeciesData(...,SpeciesRareness)}. */
        public int rareness;
        /** {@code battle.turnCount}: 0 during the first round (:138/:151). */
        public int turnCount;
        /** {@code battle.time} (PField_Battles:181-188): 2 = night/cave. */
        public int time;
        /** {@code battle.environment} (PBEnvironment::Underwater = 6). */
        public int environment;
        /** {@code $PokemonTemp.encounterType} is a fishing rod (:176-178). */
        public boolean fishingRodEncounter;
        /** {@code pbInSafari?} (Wilderness Ball, :244-247). */
        public boolean safari;
        /** The highest level on the player's side (Level Ball, :162-172). */
        public int playerMaxLevel;
        /**
         * The player's other Pokemon, for the Love Ball's same-species /
         * opposite-gender check (:201-209). Singles battles never trigger it.
         */
        public Pokemon[] sameSideParty = new Pokemon[0];

        public Random random;
    }

    /**
     * {@code BallHandlers.isUnconditional?} (PokeBall_CatchEffects:63-66 +
     * :91-93): only the Master Ball.
     */
    public static boolean isUnconditional(Context context, String ballItem) {
        return "MASTERBALL".equals(ballItem);
    }

    /**
     * {@code pbCaptureCalc} (PokeBattle_BattleCommon:170-232).
     *
     * @return the number of shakes; 4 means the Pokemon is caught
     */
    public static int shakes(Context context, String ballItem) {
        int x = captureRate(context, ballItem);
        if (x >= 255 || isUnconditional(context, ballItem)) {
            return 4;                                   // :202
        }
        long y = (long) Math.floor(65536 / Math.pow(255.0 / x, 0.1875));   // :204
        int numShakes = 0;
        for (int i = 0; i < 4; i++) {                   // :226-230
            if (numShakes < i) {
                break;
            }
            if (context.random.nextInt(65536) < y) {
                numShakes++;
            }
        }
        return numShakes;
    }

    /**
     * The {@code x} of {@code pbCaptureCalc:190-200}: the HP factor times the
     * modified rareness, doubled-and-a-half for sleep / freeze and half again
     * for any other status, floored and never below 1.
     */
    static int captureRate(Context context, String ballItem) {
        int rareness = modifyCatchRate(context, ballItem, context.rareness);
        double x = ((3.0 * context.targetMaxHp - 2.0 * context.targetHp) * rareness)
                / (3.0 * context.targetMaxHp);
        if ("SLEEP".equals(context.targetStatus) || "FROZEN".equals(context.targetStatus)) {
            x *= 2.5;                                   // :194-195
        } else if (context.targetStatus != null && !context.targetStatus.isEmpty()) {
            x *= 1.5;                                   // :196-197
        }
        x = Math.floor(x);
        return x < 1 ? 1 : (int) x;                     // :199-200
    }

    /**
     * {@code BallHandlers.modifyCatchRate} (PokeBall_CatchEffects:68-75): the
     * badge and Pokédex bonuses first, then the ball's own handler
     * ({@code ModifyCatchRate.trigger}, which returns the value unchanged when
     * the ball has no handler).
     */
    public static int modifyCatchRate(Context context, String ballItem, int catchRate) {
        if (catchRate > 0 && context.trainer != null) {
            catchRate += context.trainer.badges.size() * 2;             // :70
            catchRate += context.trainer.owned.size() / 32;             // :71
        }
        return ballMultiplier(context, ballItem, catchRate);
    }

    /** The one {@code ModifyCatchRate} handler per ball (PokeBall_CatchEffects:101-247). */
    private static int ballMultiplier(Context context, String ballItem, int catchRate) {
        if (ballItem == null) {
            return catchRate;
        }
        switch (ballItem) {
            case "GREATBALL":                                       // :101-103
                return scale(catchRate, 1.5);
            case "ULTRABALL":                                       // :105-107
                return scale(catchRate, 2);
            case "SAFARIBALL":                                      // :109-111
                return scale(catchRate, 1.5);
            case "NETBALL":                                         // :113-117
                return hasType(context, "BUG") || hasType(context, "WATER")
                        ? scale(catchRate, NET_BALL_MULTIPLIER) : catchRate;
            case "DIVEBALL":                                        // :119-122
                return context.environment == ENVIRONMENT_UNDERWATER
                        ? scale(catchRate, DIVE_BALL_MULTIPLIER) : catchRate;
            case "NESTBALL": {                                      // :124-129
                if (level(context) <= 29) {
                    return scale(catchRate, Math.max((41 - level(context)) / 10.0, 1));
                }
                return catchRate;
            }
            case "REPEATBALL":                                      // :131-135
                return ownsTarget(context) ? scale(catchRate, REPEAT_BALL_MULTIPLIER) : catchRate;
            case "TIMERBALL":                                       // :137-141
                return scale(catchRate, Math.min(1 + (0.3 * context.turnCount), 4));
            case "DUSKBALL":                                        // :143-147
                return context.time == 2 ? scale(catchRate, DUSK_BALL_MULTIPLIER) : catchRate;
            case "QUICKBALL":                                       // :149-153
                return context.turnCount == 0 ? scale(catchRate, QUICK_BALL_MULTIPLIER) : catchRate;
            case "FASTBALL": {                                      // :155-160
                int baseSpeed = context.target != null ? context.target.baseStat(PokemonStats.SPEED) : 0;
                return baseSpeed >= 100 ? Math.min(scale(catchRate, 4), 255) : Math.min(catchRate, 255);
            }
            case "LEVELBALL": {                                     // :162-172
                int maxLevel = context.playerMaxLevel;
                int target = level(context);
                if (maxLevel >= target * 4) {
                    return Math.min(scale(catchRate, 8), 255);
                }
                if (maxLevel >= target * 2) {
                    return Math.min(scale(catchRate, 4), 255);
                }
                if (maxLevel > target) {
                    return Math.min(scale(catchRate, 2), 255);
                }
                return Math.min(catchRate, 255);
            }
            case "LUREBALL":                                        // :174-180
                return context.fishingRodEncounter
                        ? Math.min(scale(catchRate, LURE_BALL_MULTIPLIER), 255)
                        : Math.min(catchRate, 255);
            case "HEAVYBALL": {                                     // :182-199
                if (catchRate == 0) {
                    return 0;
                }
                // pbWeight is the species' weight in kg (PokeBattle_Pokemon:699),
                // so with this project's data the >=3000 / >=2000 steps never
                // apply and every Heavy Ball takes the <1000 branch. Ported
                // literally - the thresholds are the plugin's own.
                int weight = (int) (context.target == null || context.target.species == null
                        ? 500 : context.target.species.weight);
                if (weight >= 3000) {
                    catchRate += 30;
                } else if (weight >= 2000) {
                    catchRate += 20;
                } else if (weight < 1000) {
                    catchRate -= 20;
                }
                return Math.min(Math.max(catchRate, 1), 255);
            }
            case "LOVEBALL": {                                      // :201-209
                if (context.target == null) {
                    return catchRate;
                }
                for (Pokemon other : context.sameSideParty) {
                    if (other == null || other.species == null || context.target.species == null) {
                        continue;
                    }
                    if (!other.species.internalName.equals(context.target.species.internalName)) {
                        continue;
                    }
                    int otherGender = other.effectiveGender();
                    if (otherGender == context.target.effectiveGender()
                            || otherGender == PokemonStats.GENDERLESS
                            || context.target.effectiveGender() == PokemonStats.GENDERLESS) {
                        continue;
                    }
                    return Math.min(scale(catchRate, 8), 255);
                }
                return Math.min(catchRate, 255);
            }
            case "MOONBALL":                                        // :211-220
                return moonStoneFamily(context) ? Math.min(scale(catchRate, 4), 255)
                        : Math.min(catchRate, 255);
            case "SPORTBALL":                                       // :222-224
                return scale(catchRate, SPORT_BALL_MULTIPLIER);
            case "DREAMBALL":                                       // :226-229
                return "SLEEP".equals(context.targetStatus) ? scale(catchRate, 4) : catchRate;
            case "BEASTBALL": {                                     // :231-238
                if (isUltraBeast(context)) {
                    return scale(catchRate, 5);
                }
                int rank = context.target == null ? 0 : context.target.battleRank;
                return rank < 2 ? catchRate / 10 : catchRate;
            }
            case "PETBALL":                                         // :240-242
                return scale(catchRate, PET_BALL_MULTIPLIER);
            case "WILDERNESSBALL":                                  // :244-247
                return scale(catchRate, context.safari ? 2 : 1);
            default:
                // PREMIERBALL / CHERISHBALL and the rest have no handler.
                return catchRate;
        }
    }

    /**
     * {@code BallHandlers.onCatch} (PokeBall_CatchEffects:77-79, :251-257):
     * the Heal Ball heals, the Friend Ball sets happiness to 200.
     */
    public static void onCatch(PbsData pbs, String ballItem, Pokemon caught) {
        if (caught == null) {
            return;
        }
        if ("HEALBALL".equals(ballItem)) {
            caught.hp = caught.maxHp();
            caught.status = "";                                 // :251-253
        } else if ("FRIENDBALL".equals(ballItem)) {
            caught.happiness = 200;                             // :255-257
        }
    }

    // ------------------------------------------------------------------

    /** PBEnvironment::Underwater (PBEnvironment:9). */
    private static final int ENVIRONMENT_UNDERWATER = 6;
    /** PBEnvironment::Cave (PBEnvironment:10). */
    public static final int ENVIRONMENT_CAVE = 7;

    /** The project's PBEnvironment table (PBEnvironment:3-21), by name. */
    private static final String[] ENVIRONMENT_NAMES = {
        "None", "Grass", "TallGrass", "MovingWater", "StillWater", "Puddle", "Underwater",
        "Cave", "Rock", "Sand", "Forest", "ForestGrass", "Snow", "Ice", "Volcano",
        "Graveyard", "Sky", "Space", "UltraSpace",
    };

    /**
     * PBS/metadata.txt's {@code Environment = <name>} -> PBEnvironment id, or
     * -1 when the map declares none.
     */
    public static int environmentId(String name) {
        if (name == null || name.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < ENVIRONMENT_NAMES.length; i++) {
            if (ENVIRONMENT_NAMES[i].equalsIgnoreCase(name.trim())) {
                return i;
            }
        }
        return -1;
    }

    private static int scale(int catchRate, double multiplier) {
        return (int) (catchRate * multiplier);
    }

    private static int level(Context context) {
        return context.target == null ? 1 : context.target.level;
    }

    private static boolean hasType(Context context, String type) {
        if (context.target == null) {
            return false;
        }
        for (String active : context.target.types()) {
            if (type.equals(active)) {
                return true;
            }
        }
        return false;
    }

    private static boolean ownsTarget(Context context) {
        return context.trainer != null && context.target != null && context.target.species != null
                && context.trainer.owned.contains(context.target.species.internalName);
    }

    private static boolean isUltraBeast(Context context) {
        if (context.target == null || context.target.species == null) {
            return false;
        }
        for (String species : ULTRA_BEASTS) {
            if (species.equals(context.target.species.internalName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code pbCheckEvolutionFamilyForItemMethodItem(species, MOONSTONE)}
     * (PokeBall_CatchEffects:215-218): whether anything in the target's
     * evolutionary family evolves with a Moon Stone. Only the forward chain
     * from the target is walked, which covers every Moon Stone evolution a wild
     * Pokemon can be the base of.
     */
    private static boolean moonStoneFamily(Context context) {
        if (context.pbs == null || context.target == null || context.target.species == null) {
            return false;
        }
        PbsData.Species species = context.target.species;
        for (PbsData.Evolution evolution : species.evolutions) {
            if ("Item".equals(evolution.method) && "MOONSTONE".equals(evolution.parameter)) {
                return true;
            }
        }
        return false;
    }
}
