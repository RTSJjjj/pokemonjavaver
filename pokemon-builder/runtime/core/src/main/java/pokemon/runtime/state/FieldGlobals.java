package pokemon.runtime.state;

/**
 * The parts of {@code $PokemonGlobal} / {@code $PokemonMap} the field rules read and write
 * (170_PField_Field, 175_PField_Encounters, 189_PItem_ItemEffects). {@code surfing}, {@code bicycle}, the repel
 * counters and the step counters belong to the save; the two flutes are {@code $PokemonMap}, which is cleared on
 * every map change ({@code Events.onMapChange}, PField_Field:537 {@code $PokemonMap.clear}).
 */
public final class FieldGlobals {
    /** {@code $PokemonGlobal.surfing}. */
    public boolean surfing;
    /** {@code $PokemonGlobal.diving}. */
    public boolean diving;
    /** {@code $PokemonGlobal.bicycle}. */
    public boolean bicycle;
    /** {@code $PokemonGlobal.repel}: steps of repel left. */
    public int repel;
    /** {@code $PokemonGlobal.infRepel}: the Infinite Repel is on. */
    public boolean infRepel;
    /** {@code $PokemonGlobal.stepcount} (&amp;= 0x7FFFFFFF each step, PField_Field:366). */
    public int stepcount;
    /** {@code $PokemonGlobal.happinessSteps}: steps toward the next walking happiness roll. */
    public int happinessSteps;
    /** {@code $PokemonGlobal.runningShoes} (173_PField_Metadata:12, :74): the player may run once an event gave the shoes. */
    public boolean runningShoes;
    /** {@code $PokemonGlobal.creditsPlayed} (173_PField_Metadata:27, :89): the credits can be skipped once they were seen. */
    public boolean creditsPlayed;
    /** {@code $PokemonGlobal.safariState} (242_PBattle_Safari:68-71). */
    public final SafariState safari = new SafariState();
    /** {@code pbGetMetadata(mapid, MetadataSafariMap)}; set by the map screen. */
    public transient java.util.function.IntPredicate safariMapOf;

    /** {@code pbInSafari?} (242_PBattle_Safari:57-66): the reception map and the maps marked as Safari maps. */
    public boolean inSafari(int mapId) {
        if (!safari.inProgress) return false;
        if (mapId == safari.receptionMap()) return true;
        return safariMapOf != null && safariMapOf.test(mapId);
    }

    /** {@code $PokemonGlobal.startTime} (epoch seconds; 0 = not set yet), shown on the trainer card. */
    public long startTime;
    /** {@code $PokemonGlobal.pokedexUnlocked} (173:92-99): which Dex lists are unlocked; the first one is unlocked at the start. */
    public final java.util.List<Boolean> pokedexUnlocked = new java.util.ArrayList<>();
    /** {@code $PokemonGlobal.pokerusTime} as an epoch day ({@code Long.MIN_VALUE} = nil): the day Pokerus last counted down. */
    public long pokerusDay = Long.MIN_VALUE;
    /** {@code $PokemonGlobal.coins}: the Game Corner coins. */
    public int coins;
    /** {@code $PokemonGlobal.triads} (235_PMinigame_TripleTriad:991-999): the Triple Triad cards; null until first used. */
    public TriadStorage triads;
    /** {@code $PokemonGlobal.pcItemStorage}: null until the PC's item storage is first opened (:19-21). */
    public PcItemStorage pcItemStorage;
    /** {@code $game_player.found_items} (309_Item_Find:107-125): the items picked up before, by internal name. */
    public final java.util.Set<String> foundItems = new java.util.LinkedHashSet<>();
    /** {@code pbGetTerrainTag($game_player)} of the map the player is on (written by the map screen, not saved). */
    public transient int playerTerrainTag;
    /** {@code pbFacingTerrainTag} (written by the map screen, not saved). */
    public transient int facingTerrainTag;
    /** The map's {@code Outdoor} flag by map id ({@code pbGetMetadata(mapid, MetadataOutdoor)}); set by the map screen. */
    public transient java.util.function.IntFunction<Boolean> outdoorOf;
    /** {@code $PokemonGlobal.mapTrail} (170_PField_Field:559-565): the last four maps, the current one first; 0 = none. */
    public final int[] mapTrail = new int[4];
    /** {@code $PokemonGlobal.dependentEvents} (182_PField_DependentEvents:40-47). */
    public final java.util.List<Dependent> dependents = new java.util.ArrayList<>();
    /** {@code $PokemonGlobal.timeTaken} (297_Follower_Main:1444): frames the follower has been out. */
    public int timeTaken;
    /** {@code $PokemonGlobal.followerHoldItem} (297_Follower_Main:1449): the follower can hand over an item. */
    public boolean followerHoldItem;
    /** {@code $PokemonGlobal.flashUsed} (170_PField_Field:570-580): Flash lit the dark map. */
    public boolean flashUsed;
    /** {@code $PokemonGlobal.escapePoint} (170_PField_Field:1370-1385): map, x, y, direction; empty = none. */
    public int[] escapePoint = new int[0];
    /** {@code $PokemonGlobal.healingSpot} (170_PField_Field:535-537): map, x, y of the last map that had one; null = none. */
    public int[] healingSpot;
    /** {@code $PokemonGlobal.visitedMaps} (170_PField_Field:540): the maps the player has been on. */
    public final java.util.Set<Integer> visitedMaps = new java.util.TreeSet<>();
    /** {@code $PokemonMap.blackFluteUsed}. */
    public boolean blackFluteUsed;
    /** {@code $PokemonMap.whiteFluteUsed}. */
    public boolean whiteFluteUsed;

    /** {@code $PokemonMap.clear}: what a map change forgets. */
    public void clearMap() {
        blackFluteUsed = false;
        whiteFluteUsed = false;
    }

    /** A new game. */
    public void reset() {
        surfing = false;
        diving = false;
        bicycle = false;
        repel = 0;
        infRepel = false;
        stepcount = 0;
        happinessSteps = 0;
        runningShoes = false;
        creditsPlayed = false;
        pokedexUnlocked.clear();
        pokerusDay = Long.MIN_VALUE;
        coins = 0;
        pcItemStorage = null;
        foundItems.clear();
        dependents.clear();
        visitedMaps.clear();
        flashUsed = false;
        escapePoint = new int[0];
        healingSpot = null;
        timeTaken = 0;
        followerHoldItem = false;
        clearMap();
    }
}
