package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

/**
 * {@code changeEVandNature(pkmn, changeIV=false)} (367_changeEVandNature.rb:1-68): gives a trainer's Pokemon
 * 252/252 EVs in its two highest base stats (HP excluded), a third pick at 0, 6 HP EVs when HP got none,
 * and a nature that suits the spread. Stat order is the plugin's: HP, Atk, Def, Spe, SpA, SpD.
 *
 * <p>Battle_StartAndEnd:162-171 runs it on the opponents' Pokemon once the player has the first badge, for
 * each Pokemon whose EVs are still a placeholder (all 0, all 85, or all {@code (level*1.5).round}).</p>
 */
public final class ChangeEvAndNature {

    private ChangeEvAndNature() {
    }

    /** Battle_StartAndEnd:164-170: the EV spreads that mark a Pokemon as not hand-tuned. */
    static boolean hasPlaceholderEvs(Pokemon pkmn) {
        int lev = (int) Math.round(pkmn.level * 1.5);                          // :165 (pkmn.level * 1.5).round
        return allEqual(pkmn.evs, 0) || allEqual(pkmn.evs, 85) || allEqual(pkmn.evs, lev);   // :166-167
    }

    private static boolean allEqual(int[] values, int v) {
        for (int x : values) if (x != v) return false;
        return true;
    }

    public static Pokemon apply(Pokemon pkmn, PbsData data, boolean changeIV) {
        pkmn.recalculatingStats(() -> {                                        // :66 calcStats keeps the HP lost
            if (changeIV) java.util.Arrays.fill(pkmn.ivs, 31);                 // :2
            // The plugin only writes three stats (:7-12), so a Pokemon that came in with the all-85 or
            // all-(level*1.5) placeholder kept 85 in the other three - 759 EVs, over the 510 cap. The spread it
            // means is 252/252/6 (what an all-0 Pokemon gets), so the old EVs are cleared first.
            java.util.Arrays.fill(pkmn.evs, 0);
            int[] bvs = new int[6];
            for (int i = 0; i < 6; i++) bvs[i] = pkmn.baseStat(i);             // :3
            bvs[0] = 0;                                                        // :4
            int[] evs = {252, 252, 0};                                         // :5
            int[] max = new int[3];
            for (int k = 0; k < 3; k++) {                                      // :7-12
                int j = 0;
                for (int i = 1; i < 6; i++) if (bvs[i] > bvs[j]) j = i;        // :8 bvs.index(bvs.max) - the first maximum
                pkmn.evs[j] = evs[k];                                          // :9
                bvs[j] = 0;                                                    // :10
                max[k] = j;                                                    // :11
            }
            if (pkmn.evs[0] == 0) pkmn.evs[0] = 6;                             // :13
            String nature = natureFor(max, pkmn.baseStat(4) > pkmn.baseStat(1));
            PbsData.Nature n = data == null ? null : data.naturesByName.get(nature);
            if (n != null) pkmn.nature = n;                                    // :65 setNature
        });
        return pkmn;                                                           // :67
    }

    /** :14-64; {@code spAtkOverAtk} is {@code pkmn.baseStats[4] > pkmn.baseStats[1]}. */
    static String natureFor(int[] max, boolean spAtkOverAtk) {
        switch (max[0]) {
            case 0:                                                            // :16 HP highest
                if (max[1] == 1 && max[2] != 4) return "ADAMANT";              // :17-18
                if (max[1] == 4 && max[2] != 1) return "MODEST";               // :19-20
                return "HARDY";                                                // :14
            case 1:                                                            // :22 Atk highest
                return (max[1] == 2 || max[1] == 3 || max[1] == 5) ? "ADAMANT" : "JOLLY";   // :23-27
            case 2:                                                            // :28 Def highest
                if (max[1] == 1) return "IMPISH";                              // :29-30
                return spAtkOverAtk ? "BOLD" : "IMPISH";                       // :32-36
            case 3:                                                            // :38 Spe highest
                if (max[1] == 4) return "MODEST";                              // :39-40
                return spAtkOverAtk ? "MODEST" : "ADAMANT";                    // :42-46
            case 4:                                                            // :48 SpA highest
                return (max[1] == 2 || max[1] == 3 || max[1] == 5) ? "MODEST" : "TIMID";    // :49-53
            case 5:                                                            // :54 SpD highest
                if (max[1] == 3) return "CALM";                                // :55-56
                return spAtkOverAtk ? "CALM" : "CAREFUL";                      // :58-62
            default:
                return "HARDY";
        }
    }
}
