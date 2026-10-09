package pokemon.runtime.pokemon;

import java.util.Random;

/**
 * 343_ChainCatching:1-103 {@code ChainCatching} and the {@code PokeBattle_Trainer} methods around it: catching the
 * same species in a row raises the odds of meeting it again, of perfect IVs and of shininess.
 */
public final class ChainCatching {
    /** :4 the species of the chain (internal name), null = none. */
    public String species;
    /** :5 chain_times. */
    public int chainTimes;
    /** :6 shiny_retries: extra shiny rolls. */
    public int shinyRetries;
    /** :7 iv_guaranteed: perfect IVs the next wild Pokemon of the chain gets. */
    public int ivGuaranteed;

    /** :34-52 {@code refreshChain(species)}: a Pokemon of the species was caught. */
    public void refresh(String caught) {
        if (caught != null && caught.equals(species)) {                      // :40
            chainTimes += 1;
        } else {
            species = caught;                                                // :43-45
            chainTimes = 1;
        }
        get(caught);                                                         // :48
    }

    /** :54-103 {@code getChain(species)}: the bonuses of the chain for a wild Pokemon of the species. */
    public void get(String wild) {
        if (wild != null && wild.equals(species)) {                          // :60
            switch (chainTimes / 10) {                                       // :63
                case 0: shinyRetries = 0; ivGuaranteed = 0; break;
                case 1: shinyRetries = 16; ivGuaranteed = 1; break;
                case 2: shinyRetries = 32; ivGuaranteed = 2; break;
                case 3: shinyRetries = 48; ivGuaranteed = 3; break;
                case 4: shinyRetries = 64; ivGuaranteed = 4; break;
                case 5: shinyRetries = 96; ivGuaranteed = 5; break;
                default: shinyRetries = 128; ivGuaranteed = 5; break;        // :93-95 60 and more
            }
        } else {
            shinyRetries = 0;                                                // :99-100
            ivGuaranteed = 0;
        }
    }

    /** :105-117 {@code pbRandomIV(num)}: num perfect IVs among six, the rest {@code rand(31)}, shuffled. */
    public static int[] randomIvs(int num, Random random) {
        int[] ivs = new int[6];
        if (num >= 6) {
            java.util.Arrays.fill(ivs, 31);
            return ivs;
        }
        for (int i = 0; i < 6; i++) {
            ivs[i] = i < num ? 31 : random.nextInt(31);
        }
        for (int i = ivs.length - 1; i > 0; i--) {                           // Array#shuffle
            int j = random.nextInt(i + 1);
            int swap = ivs[i];
            ivs[i] = ivs[j];
            ivs[j] = swap;
        }
        return ivs;
    }

    public void reset() {
        species = null;
        chainTimes = 0;
        shinyRetries = 0;
        ivGuaranteed = 0;
    }
}
