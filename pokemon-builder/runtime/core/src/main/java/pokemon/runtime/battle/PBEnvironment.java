package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code module PBEnvironment} (PBEnvironment.rb:1-24),
 * transcribed one constant at a time.
 *
 * <p>The Ruby wraps the module in {@code begin/rescue Exception ... end}
 * (PBEnvironment.rb:1-30) so that a redefinition during compilation cannot take
 * the game down; a Java class has no equivalent and needs none. The ids are what
 * PBS/metadata.txt stores in its {@code Environment} field
 * (Misc_Data.rb:113 {@code "e", :PBEnvironment}) and what
 * {@code pbGetEnvironment} / {@code pbPrepareBattle} consume.</p>
 */
public final class PBEnvironment {

    private PBEnvironment() {
    }

    /** {@code PBEnvironment::None} (PBEnvironment.rb:3). */
    public static final int None = 0;
    /** {@code PBEnvironment::Grass} (PBEnvironment.rb:4). */
    public static final int Grass = 1;
    /** {@code PBEnvironment::TallGrass} (PBEnvironment.rb:5). */
    public static final int TallGrass = 2;
    /** {@code PBEnvironment::MovingWater} (PBEnvironment.rb:6). */
    public static final int MovingWater = 3;
    /** {@code PBEnvironment::StillWater} (PBEnvironment.rb:7). */
    public static final int StillWater = 4;
    /** {@code PBEnvironment::Puddle} (PBEnvironment.rb:8). */
    public static final int Puddle = 5;
    /** {@code PBEnvironment::Underwater} (PBEnvironment.rb:9). */
    public static final int Underwater = 6;
    /** {@code PBEnvironment::Cave} (PBEnvironment.rb:10). */
    public static final int Cave = 7;
    /** {@code PBEnvironment::Rock} (PBEnvironment.rb:11). */
    public static final int Rock = 8;
    /** {@code PBEnvironment::Sand} (PBEnvironment.rb:12). */
    public static final int Sand = 9;
    /** {@code PBEnvironment::Forest} (PBEnvironment.rb:13). */
    public static final int Forest = 10;
    /** {@code PBEnvironment::ForestGrass} (PBEnvironment.rb:14). */
    public static final int ForestGrass = 11;
    /** {@code PBEnvironment::Snow} (PBEnvironment.rb:15). */
    public static final int Snow = 12;
    /** {@code PBEnvironment::Ice} (PBEnvironment.rb:16). */
    public static final int Ice = 13;
    /** {@code PBEnvironment::Volcano} (PBEnvironment.rb:17). */
    public static final int Volcano = 14;
    /** {@code PBEnvironment::Graveyard} (PBEnvironment.rb:18). */
    public static final int Graveyard = 15;
    /** {@code PBEnvironment::Sky} (PBEnvironment.rb:19). */
    public static final int Sky = 16;
    /** {@code PBEnvironment::Space} (PBEnvironment.rb:20). */
    public static final int Space = 17;
    /** {@code PBEnvironment::UltraSpace} (PBEnvironment.rb:21). */
    public static final int UltraSpace = 18;

    /** {@code PBEnvironment.maxValue} (PBEnvironment.rb:23): 18, not a count. */
    public static int maxValue() {
        return 18;
    }
}
