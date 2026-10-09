package pokemon.runtime.state;

/**
 * One entry of {@code $PokemonGlobal.dependentEvents} (182_PField_DependentEvents:483-492): an event that walks behind the
 * player. The array of the plugin is [original map id, event id, current map id, x, y, direction, character file, hue,
 * name, common event id]; the follower Pokemon of 297_Follower_Main is the entry named {@code "FollowerPkmn"}.
 */
public final class Dependent {
    public static final String FOLLOWER_NAME = "FollowerPkmn";

    /** [0] the map the event was taken from. */
    public int originalMap;
    /** [1] the event's id on that map. */
    public int eventId;
    /** [2] the map it stands on now. */
    public int currentMap;
    /** [3], [4], [5]. */
    public int x;
    public int y;
    public int direction;
    /** [6] the character file ("Following/001" once the follower took its Pokemon's graphic). */
    public String characterName;
    /** [8] */
    public String name;
    /** [9] the common event run when the player talks to it, or -1. */
    public int commonEvent = -1;

    public boolean isFollower() {
        return FOLLOWER_NAME.equals(name);
    }
}
