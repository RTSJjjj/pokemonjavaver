package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Texture;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;

import java.util.ArrayList;
import java.util.List;

/**
 * 251_PSystem_FileUtilities:161-232 {@code pbPokemonIconFile} / {@code pbCheckPokemonIconFiles}: the party icon of a Pokemon.
 * The file name is {@code icon<NAME or 000><f><s><s>_<form>_shadow}; when the exact file does not exist the plugin drops the
 * factors one combination at a time (shadow, gender, shiny, super shiny, form, species - in that bit order), so a shiny without
 * its own icon shows the ordinary icon of the same form, and a form without one shows the base species' icon. Eggs try
 * {@code icon<NAME>egg_<form>}, {@code icon%03degg_<form>}, {@code icon<NAME>egg}, {@code icon%03degg} and {@code iconEgg}.
 */
final class PokemonIcons {

    private PokemonIcons() { }

    static Texture of(MenuAssets assets, Pokemon p) {
        if (p == null || p.species == null) {
            return null;
        }
        return of(assets, p.species, p.gender == PokemonStats.FEMALE, p.shiny, p.superShiny, p.formIndex(), false, p.egg);
    }

    static Texture of(MenuAssets assets, PbsData.Species species, boolean female, boolean shiny, boolean superShiny,
                      int form, boolean shadow, boolean egg) {
        for (String candidate : candidates(species, female, shiny, superShiny, form, shadow, egg)) {
            Texture icon = assets.icon(candidate);
            if (icon != null) {
                return icon;
            }
        }
        return null;
    }

    /** The icon file names (without extension) in the order the plugin tries them. */
    static List<String> candidates(PbsData.Species species, boolean female, boolean shiny, boolean superShiny,
                                   int form, boolean shadow, boolean egg) {
        List<String> names = new ArrayList<>();
        if (species == null) {
            return names;
        }
        String id = String.format("%03d", species.id);
        String name = species.internalName;
        if (egg) {
            names.add("icon" + name + "egg_" + form);
            names.add("icon" + id + "egg_" + form);
            names.add("icon" + name + "egg");
            names.add("icon" + id + "egg");
            names.add("iconEgg");
            return names;
        }
        // factors in the plugin's push order; the last one is the species itself
        List<int[]> factors = new ArrayList<>();                 // {kind, actual}
        if (shadow) factors.add(new int[] {4, 1});
        if (female) factors.add(new int[] {1, 1});
        if (shiny) factors.add(new int[] {2, 1});
        if (superShiny) factors.add(new int[] {5, 1});
        if (form != 0) factors.add(new int[] {3, form});
        factors.add(new int[] {0, species.id});
        for (int i = 0; i < (1 << factors.size()); i++) {
            boolean useSpecies = true, useFemale = false, useShiny = false, useSuper = false, useShadow = false;
            int useForm = 0;
            for (int index = 0; index < factors.size(); index++) {
                int[] factor = factors.get(index);
                boolean actual = ((i >> index) & 1) == 0;
                switch (factor[0]) {
                    case 0: useSpecies = actual; break;
                    case 1: useFemale = actual; break;
                    case 2: useShiny = actual; break;
                    case 3: useForm = actual ? factor[1] : 0; break;
                    case 4: useShadow = actual; break;
                    case 5: useSuper = actual; break;
                    default: break;
                }
            }
            if (!useSpecies) {
                continue;                                        // species 0 would be icon000
            }
            String tail = (useFemale ? "f" : "") + (useShiny ? "s" : "") + (useSuper ? "s" : "")
                    + (useForm != 0 ? "_" + useForm : "") + (useShadow ? "_shadow" : "");
            names.add("icon" + name + tail);                     // :210 the internal name first, then the number
            names.add("icon" + id + tail);
        }
        return names;
    }
}
