package pokemon.runtime.state;

import pokemon.runtime.pokemon.TrainerState;

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
    /** Essentials {@code $PokemonGlobal.eventvars} (R7: berry plants). */
    private final GameEventVars eventVars = new GameEventVars(version);
    private final TemporaryEventState temporary = new TemporaryEventState();
    private final ScreenWeather weather = new ScreenWeather();
    private final MartPrices martPrices = new MartPrices();
    private final FieldGlobals fieldGlobals = new FieldGlobals();
    private final Inventory inventory = new Inventory();
    /** Quest plugin state (plugin batch 1). */
    private final QuestLog quests = new QuestLog(version);
    /**
     * P0c: the player trainer's Pokemon state ({@code $Trainer}: name, party,
     * money). The Pokemon construction scripts fill it; save serialisation
     * arrives with P1.
     */
    private final TrainerState trainer = new TrainerState();
    /** Essentials {@code $PokemonGlobal.followerToggled} (the follower sprite needs a party). */
    private boolean followerToggled;
    /**
     * Essentials {@code $PokemonTemp.battleRules} (PField_Battles): the rules
     * setBattleRule records for the next battle. Temp state, never saved.
     */
    private final BattleRules battleRules = new BattleRules();
    /**
     * Essentials {@code $PokemonMap.strengthUsed} (R6.30): set when the
     * Strength field move is used. It gates boulder pushing; the field move
     * itself needs the party / badges and arrives with stage 3.
     */
    private boolean pokemonMapStrengthUsed;
    /**
     * Essentials {@code $PokemonGlobal.bridge} (PField_Field:1363-1369): the
     * height a bridge is drawn at, 0 = not on a bridge. {@code pbBridgeOn} sets
     * it to its {@code height} argument (2 by default) and {@code pbBridgeOff}
     * back to 0; {@code Game_Map#playerPassable?} makes the bridge tiles under
     * the player passable only while it is above 0. Part of the save, like
     * {@code PokemonGlobal} itself.
     */
    private int bridge;
    /**
     * PField_Field:1399-1416: {@code $PokemonGlobal.partner} =
     * [trainerType, name, id, party]; null when no partner is registered.
     */
    public static final class Partner {
        public String trainerType;
        public String name;
        public int id;
        public final com.badlogic.gdx.utils.Array<pokemon.runtime.pokemon.Pokemon> party =
                new com.badlogic.gdx.utils.Array<>();
    }

    private Partner partner;

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
    /**
     * P-select: the player graphic id chosen by {@code pbChangePlayer} (0..7).
     * -1 until the intro's gender selector picks one, so a new game starts with
     * no walking sprite (the project redefines it in the selection script).
     */
    private int playerId = -1;

    public long version() {
        return version.value();
    }

    /**
     * Marks every page cache stale (the renderer's event graphics and the map
     * screen's entity refresh). Erase Event (RMXP 116) changes an event's pages
     * without touching a switch, so it has to announce that explicitly.
     */
    public void touch() {
        version.bump();
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

    /** {@code $PokemonTemp.battleRules} (PField_Battles:19). */
    public BattleRules battleRules() {
        return battleRules;
    }

    /**
     * Essentials temp switches (R6.11): transient per (map, event, channel)
     * flags read by the named switches {@code s:tsOn?("A")} / {@code s:tsOff?("A")}.
     */
    public GameTempSwitches tempSwitches() {
        return tempSwitches;
    }

    /**
     * Essentials {@code $PokemonGlobal.eventvars} (Game_Event:80-87): the map
     * interpreter's {@code getVariable}/{@code setVariable} table. Unlike the
     * temp switches it is NOT rebuilt on map setup - the berry plants keep
     * their growth state here for as long as the save lives.
     */
    public GameEventVars eventVars() {
        return eventVars;
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
     * P0c: the player trainer's Pokemon state ({@code $Trainer}). Event scripts
     * fill the party through the Pokemon construction IR commands.
     */
    public TrainerState trainer() {
        return trainer;
    }

    /**
     * P-select: the player graphic chosen by {@code pbChangePlayer}, or -1
     * before the gender selector has run.
     */
    public int playerId() {
        return playerId;
    }

    public void playerId(int id) {
        this.playerId = id;
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

    /** {@code $PokemonTemp.flydata} (214_PScreen_RegionMap fly mode): [map, x, y] of the chosen town; null = none. Never saved. */
    private int[] flyData;

    public int[] flyData() {
        return flyData;
    }

    public void flyData(int[] value) {
        this.flyData = value;
    }

    /** The next map screen plays the arrival half of the fly animation ({@code pbFlyAnimation(false)}, 179:535). Never saved. */
    private boolean flyArrival;

    public boolean takeFlyArrival() {
        boolean value = flyArrival;
        flyArrival = false;
        return value;
    }

    public void flyArrival(boolean value) {
        this.flyArrival = value;
    }

    /** PField_Field:1411/1435: the registered partner trainer, or null. */
    public Partner partner() {
        return partner;
    }

    public void partner(Partner value) {
        this.partner = value;
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

    /** Essentials {@code $PokemonGlobal.bridge}: 0 = off, {@code pbBridgeOn}'s height otherwise. */
    public int bridge() {
        return bridge;
    }

    public void bridge(int value) {
        this.bridge = Math.max(0, value);
    }

    /**
     * Essentials {@code $PokemonGlobal.nextBattleBGM} / {@code nextBattleME} /
     * {@code nextBattleCaptureME} (PField_Battles:5-7). The event commands
     * "Change Battle BGM" (132) and "Change Battle ME" (133) write them
     * (PField_Field:882-890), and {@code pbGetWildBattleBGM} /
     * {@code pbGetTrainerBattleBGM} / {@code pbGetWildVictoryME} give them
     * priority over every metadata source (PSystem_FileUtilities:547/566/619).
     * Temp state: {@code pbBattleAnimation} clears all three once the battle is
     * over (PField_Visuals:116-118).
     */
    private String nextBattleBGM;
    private String nextBattleME;
    private String nextBattleCaptureME;

    public String nextBattleBGM() {
        return nextBattleBGM;
    }

    public void nextBattleBGM(String value) {
        this.nextBattleBGM = value;
    }

    public String nextBattleME() {
        return nextBattleME;
    }

    public void nextBattleME(String value) {
        this.nextBattleME = value;
    }

    public String nextBattleCaptureME() {
        return nextBattleCaptureME;
    }

    public void nextBattleCaptureME(String value) {
        this.nextBattleCaptureME = value;
    }

    /** PField_Visuals:116-118: every pending battle audio is dropped. */
    public void clearNextBattleAudio() {
        nextBattleBGM = null;
        nextBattleME = null;
        nextBattleCaptureME = null;
    }

    /** {@code $PokemonGlobal} / {@code $PokemonMap} field state (surfing, bicycle, repel, step counters, flutes). */
    public FieldGlobals fieldGlobals() {
        return fieldGlobals;
    }

    /** {@code $game_temp.mart_prices} (230_PScreen_Mart:850-860): price overrides of the shop in progress. */
    public MartPrices martPrices() {
        return martPrices;
    }

    /** The weather half of {@code $game_screen} (Set Weather Effects, 019_Game_Screen). */
    public ScreenWeather weather() {
        return weather;
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
        fieldGlobals.clearMap(); // Events.onMapChange: $PokemonMap.clear (PField_Field:537)
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
        // Essentials has one player name ($Trainer.name); keep the trainer in
        // step so the gift messages and the battle's outsider check agree.
        if (name != null && !name.isEmpty()) {
            trainer.name = name;
        }
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
        eventVars.clear();
        temporary.clear();
        weather.reset();
        fieldGlobals.reset();
        inventory.clear();
        trainer.reset();
        playerId = -1;
        quests.clear();
        followerToggled = false;
        pokemonMapStrengthUsed = false;
        bridge = 0;
        partner = null;
        clearNextBattleAudio();
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
