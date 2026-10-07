package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code module PBNatures} (PBNatures.rb:1-87), transcribed
 * constant by constant and method by method.
 *
 * <h2>What already existed and what did not</h2>
 * The runtime already reads the 25 natures from {@code generated/pbs/natures.json}
 * into {@link pokemon.runtime.pokemon.PbsData#natures} (English names plus
 * {@code statUp}/{@code statDown}, PbsData.java:979-996) and applies them as
 * 1.1/0.9 multipliers through
 * {@link pokemon.runtime.pokemon.PokemonStats#natureMultiplier}. The
 * <b>id-based formulas</b> below are not part of that path and do not exist
 * anywhere in the runtime, and the plugin's handler code needs them by nature id
 * ({@code BattleHandlerHelpers}, which still calls
 * {@code PendingApi.PBNatures_getStatRaised/Lowered}; Ruby:
 * BattleHandlers:561-562). The plugin's Chinese display names also have no
 * runtime equivalent - {@code natures.json} carries English ones - so
 * {@link #getName(int)} is the plugin's table, kept verbatim.
 *
 * <h2>Numbering</h2>
 * The constants are the nature ids the plugin exchanges
 * ({@code PokeBattle_Battler:137 @pokemon ? @pokemon.nature : 0}); the order is
 * also this project's {@code natures.json} order (ids 0..24).
 */
public final class PBNatures {

    private PBNatures() {
    }

    /** {@code PBNatures::HARDY} (PBNatures.rb:2). */
    public static final int HARDY = 0;
    /** {@code PBNatures::LONELY} (PBNatures.rb:3). */
    public static final int LONELY = 1;
    /** {@code PBNatures::BRAVE} (PBNatures.rb:4). */
    public static final int BRAVE = 2;
    /** {@code PBNatures::ADAMANT} (PBNatures.rb:5). */
    public static final int ADAMANT = 3;
    /** {@code PBNatures::NAUGHTY} (PBNatures.rb:6). */
    public static final int NAUGHTY = 4;
    /** {@code PBNatures::BOLD} (PBNatures.rb:7). */
    public static final int BOLD = 5;
    /** {@code PBNatures::DOCILE} (PBNatures.rb:8). */
    public static final int DOCILE = 6;
    /** {@code PBNatures::RELAXED} (PBNatures.rb:9). */
    public static final int RELAXED = 7;
    /** {@code PBNatures::IMPISH} (PBNatures.rb:10). */
    public static final int IMPISH = 8;
    /** {@code PBNatures::LAX} (PBNatures.rb:11). */
    public static final int LAX = 9;
    /** {@code PBNatures::TIMID} (PBNatures.rb:12). */
    public static final int TIMID = 10;
    /** {@code PBNatures::HASTY} (PBNatures.rb:13). */
    public static final int HASTY = 11;
    /** {@code PBNatures::SERIOUS} (PBNatures.rb:14). */
    public static final int SERIOUS = 12;
    /** {@code PBNatures::JOLLY} (PBNatures.rb:15). */
    public static final int JOLLY = 13;
    /** {@code PBNatures::NAIVE} (PBNatures.rb:16). */
    public static final int NAIVE = 14;
    /** {@code PBNatures::MODEST} (PBNatures.rb:17). */
    public static final int MODEST = 15;
    /** {@code PBNatures::MILD} (PBNatures.rb:18). */
    public static final int MILD = 16;
    /** {@code PBNatures::QUIET} (PBNatures.rb:19). */
    public static final int QUIET = 17;
    /** {@code PBNatures::BASHFUL} (PBNatures.rb:20). */
    public static final int BASHFUL = 18;
    /** {@code PBNatures::RASH} (PBNatures.rb:21). */
    public static final int RASH = 19;
    /** {@code PBNatures::CALM} (PBNatures.rb:22). */
    public static final int CALM = 20;
    /** {@code PBNatures::GENTLE} (PBNatures.rb:23). */
    public static final int GENTLE = 21;
    /** {@code PBNatures::SASSY} (PBNatures.rb:24). */
    public static final int SASSY = 22;
    /** {@code PBNatures::CAREFUL} (PBNatures.rb:25). */
    public static final int CAREFUL = 23;
    /** {@code PBNatures::QUIRKY} (PBNatures.rb:26). */
    public static final int QUIRKY = 24;

    /** {@code PBNatures.maxValue} (PBNatures.rb:28): 24, not a count. */
    public static int maxValue() {
        return 24;
    }

    /** {@code PBNatures.getCount} (PBNatures.rb:29): 25. */
    public static int getCount() {
        return 25;
    }

    /**
     * {@code PBNatures.getName(id)} (PBNatures.rb:31-61). The plugin's
     * {@code id = getID(PBNatures,id)} normalisation is the identity here; an
     * out-of-range id returns null, like Ruby's {@code names[id]}.
     */
    public static String getName(int id) {
        String[] names = {
                "勤奋",     // :34
                "怕寂寞",   // :35
                "勇敢",     // :36
                "固执",     // :37
                "顽皮",     // :38
                "大胆",     // :39
                "坦率",     // :40
                "悠闲",     // :41
                "淘气",     // :42
                "乐天",     // :43
                "胆小",     // :44
                "急躁",     // :45
                "认真",     // :46
                "爽朗",     // :47
                "天真",     // :48
                "内敛",     // :49
                "慢吞吞",   // :50
                "冷静",     // :51
                "害羞",     // :52
                "马虎",     // :53
                "温和",     // :54
                "温顺",     // :55
                "自大",     // :56
                "慎重",     // :57
                "浮躁"      // :58
        };
        return id >= 0 && id < names.length ? names[id] : null;   // :60
    }

    /**
     * {@code PBNatures.getStatRaised(id)} (PBNatures.rb:63-67): the stat the
     * nature raises, as {@code [ATTACK,DEFENSE,SPEED,SPATK,SPDEF][(id%25)/5]}.
     *
     * <p>The plugin's own comment (:64) warns that the 25 is <em>not</em>
     * {@code getCount} but the square of the number of stats. {@code Math.floorMod}
     * is used so the remainder matches Ruby's {@code %} (a positive divisor
     * always yields a non-negative remainder; Java's {@code %} can be
     * negative).</p>
     */
    public static int getStatRaised(int id) {
        int m = Math.floorMod(id, 25) / 5;                    // :64
        return new int[] {PBStats.ATTACK, PBStats.DEFENSE, PBStats.SPEED,
                PBStats.SPATK, PBStats.SPDEF}[m];             // :65-66
    }

    /**
     * {@code PBNatures.getStatLowered(id)} (PBNatures.rb:69-73): the stat the
     * nature lowers, as {@code [...][id%5]}. The plugin's comment (:70) notes
     * the missing {@code %25} because 25 is a multiple of 5.
     */
    public static int getStatLowered(int id) {
        int m = Math.floorMod(id, 5);                         // :70
        return new int[] {PBStats.ATTACK, PBStats.DEFENSE, PBStats.SPEED,
                PBStats.SPATK, PBStats.SPDEF}[m];             // :71-72
    }

    /**
     * {@code PBNatures.getStatChanges(id)} (PBNatures.rb:75-86): one
     * percentage per stat (100, +10 for the raised one, -10 for the lowered
     * one), over {@code PBStats.eachStat} (:80-84). The array is sized 6 like
     * Ruby's, whose highest index is {@code PBStats::SPDEF} = 5.
     */
    public static int[] getStatChanges(int id) {
        int up = getStatRaised(id);                           // :77
        int dn = getStatLowered(id);                          // :78
        int[] ret = new int[6];                               // :79-80
        for (int s : PBStats.EACH_STAT) {
            ret[s] = 100;                                     // :81
            if (s == up) {
                ret[s] += 10;                                 // :82
            }
            if (s == dn) {
                ret[s] -= 10;                                 // :83
            }
        }
        return ret;                                           // :85
    }
}
