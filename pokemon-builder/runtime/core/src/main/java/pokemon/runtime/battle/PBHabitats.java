package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code module PBHabitats} (PBHabitats.rb:1-32), transcribed one
 * constant at a time.
 *
 * <p>The ids are what the compiler stores for a species' {@code Habitat} field
 * ({@code "e", :PBHabitats}, Misc_Data.rb:190). This runtime keeps the internal
 * name in {@link pokemon.runtime.pokemon.PbsData.Species#habitat} ("Forest"),
 * with {@code null} where the PBS omits the field - which is the plugin's id 0
 * ({@code None}); the id order and the display strings ("Water's Edge", "Rough
 * Terrain") are the gap this class fills.</p>
 */
public final class PBHabitats {

    private PBHabitats() {
    }

    /** {@code PBHabitats::None} (PBHabitats.rb:3). */
    public static final int None = 0;
    /** {@code PBHabitats::Grassland} (PBHabitats.rb:4). */
    public static final int Grassland = 1;
    /** {@code PBHabitats::Forest} (PBHabitats.rb:5). */
    public static final int Forest = 2;
    /** {@code PBHabitats::WatersEdge} (PBHabitats.rb:6). */
    public static final int WatersEdge = 3;
    /** {@code PBHabitats::Sea} (PBHabitats.rb:7). */
    public static final int Sea = 4;
    /** {@code PBHabitats::Cave} (PBHabitats.rb:8). */
    public static final int Cave = 5;
    /** {@code PBHabitats::Mountain} (PBHabitats.rb:9). */
    public static final int Mountain = 6;
    /** {@code PBHabitats::RoughTerrain} (PBHabitats.rb:10). */
    public static final int RoughTerrain = 7;
    /** {@code PBHabitats::Urban} (PBHabitats.rb:11). */
    public static final int Urban = 8;
    /** {@code PBHabitats::Rare} (PBHabitats.rb:12). */
    public static final int Rare = 9;

    /** {@code PBHabitats.maxValue} (PBHabitats.rb:13): 9, not a count. */
    public static int maxValue() {
        return 9;
    }

    /** {@code PBHabitats.getCount} (PBHabitats.rb:14): 10. */
    public static int getCount() {
        return 10;
    }

    /**
     * {@code PBHabitats.getName(id)} (PBHabitats.rb:16-31). The plugin's
     * {@code id = getID(PBHabitats,id)} normalisation is the identity here (the
     * constants equal the indices). An out-of-range id returns null, like
     * Ruby's {@code names[id]}.
     */
    public static String getName(int id) {
        String[] names = {
                "None",           // :19
                "Grassland",      // :20
                "Forest",         // :21
                "Water's Edge",   // :22
                "Sea",            // :23
                "Cave",           // :24
                "Mountain",       // :25
                "Rough Terrain",  // :26
                "Urban",          // :27
                "Rare"            // :28
        };
        return id >= 0 && id < names.length ? names[id] : null;   // :30
    }
}
