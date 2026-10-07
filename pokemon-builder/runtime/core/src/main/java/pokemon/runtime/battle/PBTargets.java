package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code module PBTargets} (PBTargets.rb:1-70), transcribed one
 * constant at a time.
 *
 * <p><b>The numbers are deliberately out of order</b> - the plugin's own
 * comment (PBTargets.rb:2-3) says they must be left like this for backwards
 * compatibility, so they are copied verbatim rather than renumbered. A move's
 * target comes from the PBS {@code Target} column (PbsData.Move.target) and is
 * resolved through the type constants of {@code PBMoves} (Compiler_PBS), so
 * these values are the ids the plugin exchanges.</p>
 */
public final class PBTargets {

    private PBTargets() {
    }

    /** {@code PBTargets::None} (PBTargets.rb:4): Bide, Counter, Metal Burst, Mirror Coat. */
    public static final int None = 1;
    /** {@code PBTargets::User} (PBTargets.rb:5). */
    public static final int User = 10;
    /** {@code PBTargets::NearAlly} (PBTargets.rb:6): Aromatic Mist, Helping Hand, Hold Hands. */
    public static final int NearAlly = 100;
    /** {@code PBTargets::UserOrNearAlly} (PBTargets.rb:7): Acupressure. */
    public static final int UserOrNearAlly = 200;
    /** {@code PBTargets::UserAndAllies} (PBTargets.rb:8): Aromatherapy, Gear Up, Heal Bell, ... */
    public static final int UserAndAllies = 5;
    /** {@code PBTargets::NearFoe} (PBTargets.rb:9): Me First. */
    public static final int NearFoe = 400;
    /** {@code PBTargets::RandomNearFoe} (PBTargets.rb:10): Petal Dance, Outrage, Struggle, ... */
    public static final int RandomNearFoe = 2;
    /** {@code PBTargets::AllNearFoes} (PBTargets.rb:11). */
    public static final int AllNearFoes = 4;
    /** {@code PBTargets::Foe} (PBTargets.rb:12): for throwing a Poké Ball. */
    public static final int Foe = 9;
    /** {@code PBTargets::AllFoes} (PBTargets.rb:13): unused, kept for completeness. */
    public static final int AllFoes = 6;
    /** {@code PBTargets::NearOther} (PBTargets.rb:14). */
    public static final int NearOther = 0;
    /** {@code PBTargets::AllNearOthers} (PBTargets.rb:15). */
    public static final int AllNearOthers = 8;
    /** {@code PBTargets::Other} (PBTargets.rb:16): most Flying-type and pulse moves. */
    public static final int Other = 3;
    /** {@code PBTargets::AllBattlers} (PBTargets.rb:17): Flower Shield, Perish Song, ... */
    public static final int AllBattlers = 7;
    /** {@code PBTargets::UserSide} (PBTargets.rb:18). */
    public static final int UserSide = 40;
    /** {@code PBTargets::FoeSide} (PBTargets.rb:19): entry hazards. */
    public static final int FoeSide = 80;
    /** {@code PBTargets::BothSides} (PBTargets.rb:20). */
    public static final int BothSides = 20;

    /** {@code PBTargets.noTargets?(target)} (PBTargets.rb:22-28). */
    public static boolean noTargets(int target) {
        return target == None ||                              // :23
                target == User ||                             // :24
                target == UserSide ||                         // :25
                target == FoeSide ||                          // :26
                target == BothSides;                          // :27
    }

    /**
     * {@code PBTargets.oneTarget?(target)} (PBTargets.rb:32-34): "can a target
     * be chosen for this move", i.e. neither a no-target nor a multi-target
     * move.
     */
    public static boolean oneTarget(int target) {
        return !noTargets(target) &&                          // :32
                !multipleTargets(target);                     // :33
    }

    /** {@code PBTargets.multipleTargets?(target)} (PBTargets.rb:36-42). */
    public static boolean multipleTargets(int target) {
        return target == AllNearFoes ||                       // :37
                target == AllNearOthers ||                    // :38
                target == UserAndAllies ||                    // :39
                target == AllFoes ||                          // :40
                target == AllBattlers;                        // :41
    }

    /**
     * {@code PBTargets.targetsFoeSide?(target)} (PBTargets.rb:46-48): moves that
     * do not target a specific Pokémon but are still affected by Pressure.
     */
    public static boolean targetsFoeSide(int target) {
        return target == FoeSide ||                           // :46
                target == BothSides;                          // :47
    }

    /** {@code PBTargets.canChooseDistantTarget?(target)} (PBTargets.rb:50-52). */
    public static boolean canChooseDistantTarget(int target) {
        return target == Other;                               // :51
    }

    /** {@code PBTargets.canChooseOneFoeTarget?(target)} (PBTargets.rb:55-59). */
    public static boolean canChooseOneFoeTarget(int target) {
        return target == NearFoe ||                           // :56
                target == NearOther ||                        // :57
                target == Other ||                            // :58
                target == RandomNearFoe;                      // :59
    }

    /**
     * {@code PBTargets.canChooseFoeTarget?(target)} (PBTargets.rb:64-68): used
     * by the AI to avoid targeting an ally when an opponent can be chosen
     * instead. The plugin's body is character-for-character the same as
     * {@link #canChooseOneFoeTarget(int)} (PBTargets.rb:55-59); both are kept
     * because both exist and can diverge.
     */
    public static boolean canChooseFoeTarget(int target) {
        return target == NearFoe ||                           // :65
                target == NearOther ||                        // :66
                target == Other ||                            // :67
                target == RandomNearFoe;                      // :68
    }

    // ------------------------------------------------------------------
    // PBS column name <-> constant
    //
    // The compiler reads the moves.txt Target column (field 10) through this
    // module: {@code Compiler_PBS:610-613} declares the schema
    // {@code [0,"vnssueeuuuyiss", ...,PBTargets, ...]}, the {@code y} slot
    // compiles with {@code csvEnumFieldOrInt!} (Compiler:404-408, called from
    // Compiler:588-590) and resolves the name with
    // {@code enumer.const_defined?(ret)} (Compiler:410-419), i.e. the PBS
    // spelling is the Ruby constant name. This runtime keeps the PBS spelling as
    // a String ({@code PbsData.Move.target}) and needs both directions; the
    // mapping is therefore the constant name itself.
    //
    // PBS/moves.txt uses 16 of the 17 constants: every value in the file maps
    // 1:1, and only PBTargets::Foe (PBTargets.rb:12, "for throwing a Poké Ball")
    // never appears there because the ball's move is created at runtime.
    // ------------------------------------------------------------------

    /** The PBS Target column spellings, in PBTargets.rb:4-20 declaration order. */
    private static final String[] NAMES = {
            "None",             // :4
            "User",             // :5
            "NearAlly",         // :6
            "UserOrNearAlly",   // :7
            "UserAndAllies",    // :8
            "NearFoe",          // :9
            "RandomNearFoe",    // :10
            "AllNearFoes",      // :11
            "Foe",              // :12
            "AllFoes",          // :13
            "NearOther",        // :14
            "AllNearOthers",    // :15
            "Other",            // :16
            "AllBattlers",      // :17
            "UserSide",         // :18
            "FoeSide",          // :19
            "BothSides"         // :20
    };

    /** The constants above, in the same order. */
    private static final int[] VALUES = {
            None, User, NearAlly, UserOrNearAlly, UserAndAllies, NearFoe, RandomNearFoe,
            AllNearFoes, Foe, AllFoes, NearOther, AllNearOthers, Other, AllBattlers,
            UserSide, FoeSide, BothSides
    };

    /**
     * The {@code PBTargets} constant of a PBS {@code Target} column value
     * (Compiler_PBS:610-613, Compiler:583-590).
     *
     * <p>Matching is case-insensitive on the Lead's instruction, which is
     * deliberately looser than the compiler: {@code checkEnumField}
     * (Compiler:410-419) requires an exact {@code const_defined?(name)} match, so
     * the plugin itself is case-sensitive. The Builder already writes the exact
     * spelling, so this only tolerates hand-edited JSON.</p>
     *
     * <p>A {@code null} or unknown name throws instead of returning a default
     * target: PBTargets.rb has no default (the compiler raises "Undefined value"
     * too, Compiler:413-415), and silently picking one would invent
     * behaviour.</p>
     *
     * @throws IllegalArgumentException when the name is not a PBTargets constant
     */
    public static int fromName(String pbsTargetName) {
        if (pbsTargetName != null) {
            for (int i = 0; i < NAMES.length; i++) {
                if (NAMES[i].equalsIgnoreCase(pbsTargetName)) {
                    return VALUES[i];
                }
            }
        }
        throw new IllegalArgumentException("not a PBS Target value: " + pbsTargetName);
    }

    /**
     * The PBS {@code Target} spelling of a {@code PBTargets} constant (the
     * inverse of {@link #fromName(String)}; the plugin does this only in the
     * editor, {@code Editor_SaveData:102 getConstantName(PBTargets,...)}).
     *
     * @return the name, or {@code null} when the value is not a PBTargets
     *         constant
     */
    public static String nameOf(int target) {
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i] == target) {
                return NAMES[i];
            }
        }
        return null;
    }
}
