package pokemon.runtime.pokemon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * {@code pbCheckPokemonBitmapFiles(params)} (251_PSystem_FileUtilities:72-113): the Battlers file a species / gender / shiny /
 * form asks for. The factors that are asked for (gender, shiny, super shiny, form, species) are dropped one by one - the
 * low ones first - until a file exists, so a female Pokemon without its own picture falls back to the male one, a shiny
 * form without a shiny picture to the plain form, and so on. Each attempt is tried under the internal name, then the number.
 */
public final class BattlerBitmaps {
    private BattlerBitmaps() { }

    /** The file names (no extension) in the plugin's order. */
    public static List<String> names(PbsData.Species species, boolean back, boolean female, boolean shiny, boolean superShiny, int form) {
        // factors: [kind, wanted value, fallback value]: 2 gender, 3 shiny, 6 super shiny, 4 form, 0 species
        List<int[]> factors = new ArrayList<>();
        if (female) factors.add(new int[] {2, 1, 0});
        if (shiny) factors.add(new int[] {3, 1, 0});
        if (superShiny) factors.add(new int[] {6, 1, 0});
        if (form != 0) factors.add(new int[] {4, form, 0});
        factors.add(new int[] {0, 1, 0});
        List<String> names = new ArrayList<>();
        int count = 1 << factors.size();
        for (int i = 0; i < count; i++) {
            boolean tryGender = false, tryShiny = false, trySuper = false, withSpecies = true;
            int tryForm = 0;
            for (int index = 0; index < factors.size(); index++) {
                int[] factor = factors.get(index);
                int value = ((i >> index) & 1) == 0 ? factor[1] : factor[2];
                switch (factor[0]) {
                    case 0: withSpecies = value != 0; break;
                    case 2: tryGender = value != 0; break;
                    case 3: tryShiny = value != 0; break;
                    case 6: trySuper = value != 0; break;
                    case 4: tryForm = value; break;
                    default: break;
                }
            }
            if (!withSpecies) continue;                                              // :100 next if trySpecies==0 && j==0 (and no number either)
            String suffix = (tryGender ? "f" : "") + (tryShiny ? "s" : "") + (trySuper ? "s" : "")
                    + (back ? "b" : "") + (tryForm != 0 ? "_" + tryForm : "");
            if (species.internalName != null) names.add(species.internalName + suffix);   // :101 j == 0
            names.add(String.format("%03d", species.id) + suffix);                        // :101 j == 1
        }
        return names;
    }

    /** The first name {@code lookup} resolves (null when none does). */
    public static <T> T find(PbsData.Species species, boolean back, boolean female, boolean shiny, boolean superShiny, int form,
                             Function<String, T> lookup) {
        for (String name : names(species, back, female, shiny, superShiny, form)) {
            T found = lookup.apply(name);
            if (found != null) return found;
        }
        return null;
    }
}
