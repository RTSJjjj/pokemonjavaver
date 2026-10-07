package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code module PBEggGroups} (PBEggGroups.rb:1-42), transcribed
 * one constant at a time.
 *
 * <p>The ids are what the compiler stores in a species' {@code Compatibility}
 * ({@code "eE", :PBEggGroups}, Misc_Data.rb:165). This runtime keeps the
 * internal names in
 * {@link pokemon.runtime.pokemon.PbsData.Species#compatibility} ("Water1",
 * "Humanlike", "Undiscovered", "Ditto"), so the plugin's
 * {@code isConst?(compat,PBEggGroups,:Ditto)} checks
 * (PField_DayCare:110-155, PSystem_PokemonUtilities:495-496) are name
 * comparisons here. The id order and the <em>display</em> strings ("Water 1",
 * "Human-like") are the gap this class fills.</p>
 */
public final class PBEggGroups {

    private PBEggGroups() {
    }

    /** {@code PBEggGroups::Undiscovered} (PBEggGroups.rb:2): NoEggs, None, NA. */
    public static final int Undiscovered = 0;
    /** {@code PBEggGroups::Monster} (PBEggGroups.rb:3). */
    public static final int Monster = 1;
    /** {@code PBEggGroups::Water1} (PBEggGroups.rb:4). */
    public static final int Water1 = 2;
    /** {@code PBEggGroups::Bug} (PBEggGroups.rb:5). */
    public static final int Bug = 3;
    /** {@code PBEggGroups::Flying} (PBEggGroups.rb:6). */
    public static final int Flying = 4;
    /** {@code PBEggGroups::Field} (PBEggGroups.rb:7): Ground. */
    public static final int Field = 5;
    /** {@code PBEggGroups::Fairy} (PBEggGroups.rb:8). */
    public static final int Fairy = 6;
    /** {@code PBEggGroups::Grass} (PBEggGroups.rb:9): Plant. */
    public static final int Grass = 7;
    /** {@code PBEggGroups::Humanlike} (PBEggGroups.rb:10): Humanoid, Humanshape, Human. */
    public static final int Humanlike = 8;
    /** {@code PBEggGroups::Water3} (PBEggGroups.rb:11). */
    public static final int Water3 = 9;
    /** {@code PBEggGroups::Mineral} (PBEggGroups.rb:12). */
    public static final int Mineral = 10;
    /** {@code PBEggGroups::Amorphous} (PBEggGroups.rb:13): Indeterminate. */
    public static final int Amorphous = 11;
    /** {@code PBEggGroups::Water2} (PBEggGroups.rb:14). */
    public static final int Water2 = 12;
    /** {@code PBEggGroups::Ditto} (PBEggGroups.rb:15). */
    public static final int Ditto = 13;
    /** {@code PBEggGroups::Dragon} (PBEggGroups.rb:16). */
    public static final int Dragon = 14;

    /** {@code PBEggGroups.maxValue} (PBEggGroups.rb:18): 14, not a count. */
    public static int maxValue() {
        return 14;
    }

    /** {@code PBEggGroups.getCount} (PBEggGroups.rb:19): 15. */
    public static int getCount() {
        return 15;
    }

    /**
     * {@code PBEggGroups.getName(id)} (PBEggGroups.rb:21-41). The plugin's
     * {@code id = getID(PBEggGroups,id)} normalisation is the identity here (the
     * constants equal the indices). An out-of-range id returns null, like
     * Ruby's {@code names[id]}.
     */
    public static String getName(int id) {
        String[] names = {
                "Undiscovered",   // :24
                "Monster",        // :25
                "Water 1",        // :26
                "Bug",            // :27
                "Flying",         // :28
                "Field",          // :29
                "Fairy",          // :30
                "Grass",          // :31
                "Human-like",     // :32
                "Water 3",        // :33
                "Mineral",        // :34
                "Amorphous",      // :35
                "Water 2",        // :36
                "Ditto",          // :37
                "Dragon"          // :38
        };
        return id >= 0 && id < names.length ? names[id] : null;   // :40
    }
}
