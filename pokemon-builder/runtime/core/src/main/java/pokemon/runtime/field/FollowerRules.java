package pokemon.runtime.field;

import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 296_Follower_Config:29-130 (the {@code Events.FollowerRefresh} handlers and the animated-species list) and
 * 297_Follower_Main:196-252 {@code change_sprite} (the graphic file the following Pokemon takes).
 */
public final class FollowerRules {
    /** 296_Follower_Config:29. */
    public static final int FOLLOWER_COMMON_EVENT = 5;
    /** 296_Follower_Config:33-40. */
    public static final int ANIMATION_COME_OUT = 93;
    public static final int ANIMATION_COME_IN = 94;
    public static final int EMO_HAPPY = 95;
    public static final int EMO_NORMAL = 96;
    public static final int EMO_HATE = 97;
    public static final int EMO_POISON = 98;
    public static final int EMO_SING = 99;
    public static final int EMO_LOVE = 100;
    /** 296_Follower_Config:55 {@code ALWAYS_ANIMATE}. */
    public static final boolean ALWAYS_ANIMATE = true;

    /** 296_Follower_Config:58-83 {@code ALWAYS_ANIMATED_FOLLOWERS}. */
    private static final Set<Integer> ALWAYS_ANIMATED = new HashSet<>();

    static {
        int[] species = {
            12, 15, 17, 18, 22, 41, 42, 49, 63, 74, 81, 92, 93, 109, 110, 120, 121, 137, 142, 144, 145,
            146, 149, 150, 151,
            164, 165, 166, 169, 176, 187, 188, 189, 193, 200, 201, 207, 226, 227, 233,
            249, 250, 251,
            267, 269, 277, 278, 279, 284, 291, 292, 307, 313, 314, 330, 333, 334,
            337, 338, 343, 344, 351, 353, 355, 358, 362, 374, 375,
            380, 381, 384, 385,
            397, 398, 414, 415, 416, 425, 426, 429, 433, 436, 437, 442, 455, 458,
            462, 468, 469, 472, 474, 476, 477, 478, 479, 480, 481, 482, 487, 488, 489, 490, 491,
            517, 518, 520, 521, 527, 528, 561, 562, 563, 567, 577, 578,
            579, 581, 582, 583, 584, 592, 593, 605, 606, 608, 609,
            615, 628, 630, 635, 637, 641, 642, 643, 644,
            662, 663, 666, 682, 691, 703, 707, 708, 714, 715, 717, 719, 720,
            738, 742, 743, 764, 774, 781, 785, 786, 787, 788, 789, 790, 792,
            793, 797, 798, 800, 801, 803, 804,
            822, 823, 826, 841, 845, 854, 855, 873, 885, 886, 887, 890, 894, 895, 898,
        };
        for (int id : species) {
            ALWAYS_ANIMATED.add(id);
        }
    }

    private FollowerRules() {
    }

    /** What the {@code FollowerRefresh} handlers read. */
    public static final class Context {
        public boolean bicycle;
        public boolean surfing;
        public boolean diving;
        /** {@code pbGetMetadata(map, MetadataOutdoor) == true}. */
        public boolean outdoor;
        /** {@code $PokemonEncounters.isEncounterPossibleHere?}. */
        public boolean encounterPossible;
        public String mapName = "";
    }

    public static boolean hasType(Pokemon pkmn, String type) {
        for (String own : pkmn.types()) {
            if (type.equals(own)) {
                return true;
            }
        }
        return false;
    }

    public static boolean alwaysAnimated(Pokemon pkmn) {
        return pkmn.species != null && ALWAYS_ANIMATED.contains(pkmn.species.id);
    }

    /**
     * {@code Events.FollowerRefresh.trigger(pkmn)} (FollowerEvent#trigger: the first handler that answers true or false
     * decides): true = the follower shows, false = it hides, null = no handler answered (Ruby's -1, which still shows).
     */
    public static Boolean refresh(Pokemon pkmn, Context context) {
        if (context.bicycle) {
            return Boolean.FALSE;                                                   // :91
        }
        if ("Cedolan Gym".equals(context.mapName)) {
            return Boolean.FALSE;                                                   // :98
        }
        if (context.surfing) {                                                      // :102-107
            if (hasType(pkmn, "WATER")) return Boolean.FALSE;
            if (hasType(pkmn, "FLYING") || "LEVITATE".equals(pkmn.ability)) return Boolean.FALSE;
            return alwaysAnimated(pkmn) ? Boolean.TRUE : Boolean.FALSE;
        }
        if (context.diving) {                                                       // :111-114
            return hasType(pkmn, "WATER") ? Boolean.TRUE : Boolean.FALSE;
        }
        if (!context.outdoor) {                                                     // :118-122
            float height = pkmn.form != null && pkmn.form.height != null ? pkmn.form.height
                    : pkmn.species == null ? 0f : pkmn.species.height;
            if (height > 2.5f && !context.encounterPossible) {
                return Boolean.FALSE;
            }
        }
        if (hasType(pkmn, "FLYING") || "LEVITATE".equals(pkmn.ability) || alwaysAnimated(pkmn)) {
            return Boolean.TRUE;                                                    // :127-129
        }
        return null;
    }

    /**
     * {@code change_sprite} (297_Follower_Main:196-252): the files tried, best first. The plugin builds the name from
     * the species (internal name, then the number), "f" for a female, "s" for a shiny, "_form", "_shadow"; and drops
     * the factors one by one when the file does not exist. Each entry is a path below Graphics/Characters.
     */
    public static List<String> spriteCandidates(Pokemon pkmn) {
        List<Object[]> factors = new ArrayList<>();                                 // [kind, value, fallback]
        int form = pkmn.formIndex();
        boolean female = pkmn.gender == PokemonStats.FEMALE;
        if (female) factors.add(new Object[] {1});                                  // :199 gender
        if (pkmn.shiny) factors.add(new Object[] {2});                              // :200 shiny
        if (form != 0) factors.add(new Object[] {3});                               // :201 form
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < (1 << factors.size()); i++) {                           // :208 (the species factor falls back to 0 last)
            boolean tryFemale = female, tryShiny = pkmn.shiny;
            int tryForm = form;
            for (int index = 0; index < factors.size(); index++) {
                boolean useFallback = ((i / (1 << index)) % 2) != 0;
                int kind = (Integer) factors.get(index)[0];
                if (useFallback) {
                    if (kind == 1) tryFemale = false;
                    if (kind == 2) tryShiny = false;
                    if (kind == 3) tryForm = 0;
                }
            }
            String suffix = (tryFemale ? "f" : "") + (tryShiny ? "s" : "") + (tryForm != 0 ? "_" + tryForm : "");
            if (pkmn.species != null) {
                String number = String.format(java.util.Locale.ROOT, "%03d", pkmn.species.id) + suffix;   // :222 j == 1 wins over j == 0
                paths.add("Following/" + number);                                   // :230 wins over :229
                paths.add(number);
                paths.add("Following/" + pkmn.species.internalName + suffix);
                paths.add(pkmn.species.internalName + suffix);
            }
        }
        paths.add("Following/000");                                                 // species fallback 0 ("000" is the placeholder)
        paths.add("000");
        return paths;
    }
}
