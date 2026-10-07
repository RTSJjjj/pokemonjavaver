package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code PBStatuses} (PBStatuses.rb:1-27).
 *
 * <h2>Documented deviation: the runtime stores status as its internal name</h2>
 * The plugin stores {@code @status} as one of these integers
 * ({@code @status = PBStatuses::POISON}) and its {@code statusCount} beside it.
 * This runtime has stored the status as the internal-name String
 * ({@code "POISON"}) since stage 3, and that String is what {@code Pokemon}
 * persists in the save file - switching to integers would change the save
 * format for no behavioural gain.
 *
 * <p>So the integer constants below are the plugin's real values and are used
 * wherever the plugin's number is what matters ({@code PBStatuses.getName},
 * ordering, bit tests), while the battle state carries the matching name:</p>
 *
 * <table border="1">
 * <caption>Integer &lt;-&gt; name</caption>
 * <tr><th>Constant</th><th>Value</th><th>{@link #NAME_OF} entry / {@code Pokemon.status}</th></tr>
 * <tr><td>{@link #NONE}</td><td>0</td><td>{@code ""} (empty string)</td></tr>
 * <tr><td>{@link #SLEEP}</td><td>1</td><td>{@code "SLEEP"}</td></tr>
 * <tr><td>{@link #POISON}</td><td>2</td><td>{@code "POISON"}</td></tr>
 * <tr><td>{@link #BURN}</td><td>3</td><td>{@code "BURN"}</td></tr>
 * <tr><td>{@link #PARALYSIS}</td><td>4</td><td>{@code "PARALYSIS"}</td></tr>
 * <tr><td>{@link #FROZEN}</td><td>5</td><td>{@code "FROZEN"}</td></tr>
 * <tr><td>{@link #FROSTBITE}</td><td>6</td><td>{@code "FROSTBITE"}</td></tr>
 * <tr><td>{@link #DROWSY}</td><td>7</td><td>{@code "DROWSY"}</td></tr>
 * </table>
 *
 * <p>Every plugin site of the form {@code @status==PBStatuses::X} therefore
 * becomes {@code battler.hasStatus("X")} (or the matching predicate such as
 * {@code poisoned?}/{@code burned?}/{@code asleep?}); {@code @status =
 * PBStatuses::NONE} becomes {@code battler.cureStatus()}.</p>
 */
public final class PBStatuses {

    private PBStatuses() {
    }

    public static final int NONE = 0;
    public static final int SLEEP = 1;
    public static final int POISON = 2;
    public static final int BURN = 3;
    public static final int PARALYSIS = 4;
    public static final int FROZEN = 5;
    /** Pokémon Legends: Arceus (PBStatuses.rb:10). */
    public static final int FROSTBITE = 6;
    /** Pokémon Legends: Arceus (PBStatuses.rb:11). */
    public static final int DROWSY = 7;

    /** The internal name of each status, indexed by the constants above; {@code ""} for {@code NONE}. */
    public static final String[] NAME_OF = {
        "", "SLEEP", "POISON", "BURN", "PARALYSIS", "FROZEN", "FROSTBITE", "DROWSY"
    };

    /** {@code PBStatuses.getName} (PBStatuses.rb:13-26), the English message-table wording. */
    public static String getName(int id) {
        String[] names = {"healthy", "asleep", "poisoned", "burned", "paralyzed",
                          "frozen", "frostbitten", "drowsy"};
        return id >= 0 && id < names.length ? names[id] : null;
    }

    /** The {@link #NAME_OF} entry for a status integer ({@code ""} when out of range). */
    public static String nameOf(int id) {
        return id >= 0 && id < NAME_OF.length ? NAME_OF[id] : "";
    }

    /** The status integer for an internal name, or {@link #NONE} for {@code null}/{@code ""}/unknown. */
    public static int idOf(String name) {
        if (name == null || name.isEmpty()) {
            return NONE;
        }
        for (int i = 1; i < NAME_OF.length; i++) {
            if (NAME_OF[i].equals(name)) {
                return i;
            }
        }
        return NONE;
    }
}
