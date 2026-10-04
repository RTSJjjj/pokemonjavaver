package pokemon.runtime.state;

/**
 * Runtime game state (project3 section 30): current map, player position,
 * switches, variables, self switches and temporary event state. Stage 3
 * Pokemon data and the save system (R10) hang off this object instead of
 * global variables, and the event interpreter (R6) reads and writes it.
 *
 * <p>{@link #version()} counts every switch / variable / self switch change;
 * rendering refreshes event pages when it moves. Player movement does not
 * change the version because page conditions never depend on it.</p>
 */
public final class GameState {

    /** RMXP direction codes: 2 down, 4 left, 6 right, 8 up. */
    public static final int DEFAULT_DIRECTION = 2;

    private final StateVersion version = new StateVersion();
    private final GameSwitches switches = new GameSwitches(version);
    private final GameVariables variables = new GameVariables(version);
    private final GameSelfSwitches selfSwitches = new GameSelfSwitches(version);
    private final GameTempSwitches tempSwitches = new GameTempSwitches(version);
    private final TemporaryEventState temporary = new TemporaryEventState();
    private final Inventory inventory = new Inventory();
    /** Quest plugin state (plugin batch 1). */
    private final QuestLog quests = new QuestLog(version);
    /** Essentials {@code $PokemonGlobal.followerToggled} (the follower sprite needs a party). */
    private boolean followerToggled;
    /**
     * Essentials {@code $PokemonMap.strengthUsed} (R6.30): set when the
     * Strength field move is used. It gates boulder pushing; the field move
     * itself needs the party / badges and arrives with stage 3.
     */
    private boolean pokemonMapStrengthUsed;

    private int mapId = -1;
    private int playerX;
    private int playerY;
    private int playerDirection = DEFAULT_DIRECTION;
    /**
     * Trainer name used by {@code \PN} in messages. The new-game / intro flow
     * does not exist yet, so R6 keeps a placeholder that a save or an intro
     * screen (R10/R14) overwrites.
     */
    private String playerName = "训练家";

    public long version() {
        return version.value();
    }

    public GameSwitches switches() {
        return switches;
    }

    public GameVariables variables() {
        return variables;
    }

    public GameSelfSwitches selfSwitches() {
        return selfSwitches;
    }

    /**
     * Essentials temp switches (R6.11): transient per (map, event, channel)
     * flags read by the named switches {@code s:tsOn?("A")} / {@code s:tsOff?("A")}.
     */
    public GameTempSwitches tempSwitches() {
        return tempSwitches;
    }

    /**
     * RMXP switch names (from System.rxdata). A name starting with {@code s:}
     * is a script expression evaluated per event instead of a stored boolean -
     * see {@code EventPages.switchOn}.
     */
    public void switchNames(String[] names) {
        this.switchNames = names == null ? new String[0] : names.clone();
    }

    public String switchName(int id) {
        return id >= 0 && id < switchNames.length ? switchNames[id] : null;
    }

    /**
     * Sink for "this switch expression is not supported yet" reports; the
     * runtime wires it to the deduplicating game log.
     */
    public void switchWarnings(java.util.function.Consumer<String> sink) {
        this.switchWarnings = sink;
    }

    /** Named-switch evaluation lives in the map package; it reports through here. */
    public void reportSwitchWarning(String message) {
        if (switchWarnings != null) {
            switchWarnings.accept(message);
        }
    }

    private String[] switchNames = new String[0];
    private java.util.function.Consumer<String> switchWarnings;

    public TemporaryEventState temporary() {
        return temporary;
    }

    /** Item counts; part of the saved state (R10) and grown in stage 3. */
    public Inventory inventory() {
        return inventory;
    }

    /** Quests activated / advanced / completed by event scripts (plugin batch 1). */
    public QuestLog quests() {
        return quests;
    }

    /**
     * Essentials {@code $PokemonGlobal.followerToggled}: whether the player
     * asked for a following Pokemon. The visual follower arrives with the party
     * (stage 3); the flag is what the events toggle.
     */
    public boolean followerToggled() {
        return followerToggled;
    }

    public void followerToggled(boolean value) {
        this.followerToggled = value;
    }

    /**
     * R6.30: {@code $PokemonMap.strengthUsed} - boulders only move while the
     * Strength field move is active. The flag is part of the save so a loaded
     * game keeps its pushable boulders until the map is left.
     */
    public boolean pokemonMapStrengthUsed() {
        return pokemonMapStrengthUsed;
    }

    public void pokemonMapStrengthUsed(boolean value) {
        this.pokemonMapStrengthUsed = value;
    }

    public int currentMapId() {
        return mapId;
    }

    /**
     * Moves the player to a map / tile (new game, transfer or debug override).
     * The temporary state of the target map is dropped, matching RMXP's
     * {@code Game_Map#setup} creating fresh Game_Event objects.
     */
    public void enterMap(int mapId, int playerX, int playerY) {
        if (mapId < 0) {
            throw new IllegalArgumentException("map id must be >= 0 but was " + mapId);
        }
        this.mapId = mapId;
        this.playerX = playerX;
        this.playerY = playerY;
        temporary.clearMap(mapId);
        tempSwitches.clear(); // RMXP rebuilds every event on map setup
    }

    /**
     * R6.23: a map-connection walk into the neighbouring map. Unlike
     * {@link #enterMap} the neighbour is already loaded (RMXP's MapFactory
     * switches {@code $game_map} without a {@code Game_Map#setup}), so the
     * temporary state of that map survives.
     */
    public void walkToMap(int mapId, int playerX, int playerY) {
        if (mapId < 0) {
            throw new IllegalArgumentException("map id must be >= 0 but was " + mapId);
        }
        this.mapId = mapId;
        this.playerX = playerX;
        this.playerY = playerY;
    }

    public int playerX() {
        return playerX;
    }

    public int playerY() {
        return playerY;
    }

    public int playerDirection() {
        return playerDirection;
    }

    public String playerName() {
        return playerName;
    }

    public void playerName(String name) {
        this.playerName = name;
    }

    /**
     * L1: new game. Clears every progress value (switches, variables, self /
     * temp switches, temporary event state, items, quests, field flags) and
     * puts the player back to the placeholder name; the title screen then
     * enters the System start map.
     */
    public void reset() {
        switches.clear();
        variables.clear();
        selfSwitches.clear();
        tempSwitches.clear();
        temporary.clear();
        inventory.clear();
        quests.clear();
        followerToggled = false;
        pokemonMapStrengthUsed = false;
        mapId = -1;
        playerX = 0;
        playerY = 0;
        playerDirection = DEFAULT_DIRECTION;
        playerName = "训练家";
    }

    public void setPlayerPosition(int x, int y) {
        this.playerX = x;
        this.playerY = y;
    }

    public void setPlayerPosition(int x, int y, int direction) {
        this.playerX = x;
        this.playerY = y;
        this.playerDirection = direction;
    }
}
