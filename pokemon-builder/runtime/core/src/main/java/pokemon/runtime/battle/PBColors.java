package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code module PBColors} (PBColors.rb:1-33), transcribed one
 * constant at a time.
 *
 * <p>"Colors must begin at 0 and have no missing numbers" (PBColors.rb:1), so
 * the constant equals the array index and the plugin's {@code getID} round trip
 * is the identity. The ids are what the compiler stores for a species (its
 * {@code Color} field is {@code "e", :PBColors}, Misc_Data.rb:169): this
 * runtime keeps the <em>internal name</em> in
 * {@link pokemon.runtime.pokemon.PbsData.Species#color} ("Green"), while the
 * dex's colour filter needs the id order and the display string
 * (PScreen_PokedexMain:741-742 {@code for i in 0..PBColors.maxValue;
 * j = PBColors.getName(i)}), which is the gap this class fills.</p>
 */
public final class PBColors {

    private PBColors() {
    }

    /** {@code PBColors::Red} (PBColors.rb:3). */
    public static final int Red = 0;
    /** {@code PBColors::Blue} (PBColors.rb:4). */
    public static final int Blue = 1;
    /** {@code PBColors::Yellow} (PBColors.rb:5). */
    public static final int Yellow = 2;
    /** {@code PBColors::Green} (PBColors.rb:6). */
    public static final int Green = 3;
    /** {@code PBColors::Black} (PBColors.rb:7). */
    public static final int Black = 4;
    /** {@code PBColors::Brown} (PBColors.rb:8). */
    public static final int Brown = 5;
    /** {@code PBColors::Purple} (PBColors.rb:9). */
    public static final int Purple = 6;
    /** {@code PBColors::Gray} (PBColors.rb:10). */
    public static final int Gray = 7;
    /** {@code PBColors::White} (PBColors.rb:11). */
    public static final int White = 8;
    /** {@code PBColors::Pink} (PBColors.rb:12). */
    public static final int Pink = 9;

    /** {@code PBColors.maxValue} (PBColors.rb:14): 9, not a count. */
    public static int maxValue() {
        return 9;
    }

    /** {@code PBColors.getCount} (PBColors.rb:15): 10. */
    public static int getCount() {
        return 10;
    }

    /**
     * {@code PBColors.getName(id)} (PBColors.rb:17-32). The plugin's
     * {@code id = getID(PBColors,id)} normalisation is the identity here (the
     * constants equal the indices, :1); an out-of-range id returns null, like
     * Ruby's {@code names[id]}.
     */
    public static String getName(int id) {
        String[] names = {
                "Red",      // :20
                "Blue",     // :21
                "Yellow",   // :22
                "Green",    // :23
                "Black",    // :24
                "Brown",    // :25
                "Purple",   // :26
                "Gray",     // :27
                "White",    // :28
                "Pink"      // :29
        };
        return id >= 0 && id < names.length ? names[id] : null;   // :31
    }
}
