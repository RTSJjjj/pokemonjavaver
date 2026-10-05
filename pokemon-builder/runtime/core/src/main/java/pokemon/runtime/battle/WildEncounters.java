package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.PbsData;

import java.util.Random;

/**
 * Stage 3 / P2: the wild encounter roll on the field, following the project's
 * {@code PokemonEncounters}: the map's Cave method wins on any tile, the Land
 * methods need a grass tile, and the chance per step is {@code density/180}
 * (the project's {@code rand(180*16) >= density*16}) with the first three steps
 * of a map safe. Triggered methods (RockSmash / fishing rods) skip the roll and
 * use {@link #pick}.
 */
public final class WildEncounters {

    private static final String[] LAND_METHODS = {"Land", "LandMorning", "LandDay", "LandNight"};
    /** PBTerrain: Grass / TallGrass (the tiles Land encounters happen on). */
    public static final int TERRAIN_GRASS = 2;
    public static final int TERRAIN_TALL_GRASS = 10;
    private static final int SAFE_STEPS = 3;

    private int stepCount;
    private int mapId = -1;

    /** One decided encounter. */
    public static final class WildEncounter {
        public final String species;
        public final int level;

        public WildEncounter(String species, int level) {
            this.species = species;
            this.level = level;
        }
    }

    /** Resets the safe-step counter when the player enters another map. */
    public void onMap(int newMapId) {
        if (newMapId != mapId) {
            mapId = newMapId;
            stepCount = 0;
        }
    }

    /** Called after an encounter so the next few steps are safe. */
    public void reset() {
        stepCount = 0;
    }

    public int stepCount() {
        return stepCount;
    }

    /**
     * Rolls a step encounter for the tile the player just stepped on.
     *
     * @return the encounter, or null when nothing happens here/now
     */
    public WildEncounter roll(PbsData pbs, int currentMapId, int terrainTag, Random random) {
        if (pbs == null) {
            return null;
        }
        PbsData.EncounterMap map = pbs.encounterMap(currentMapId);
        if (map == null) {
            return null;
        }
        String method = stepMethod(map, terrainTag);
        if (method == null) {
            return null;
        }
        stepCount++;
        if (stepCount <= SAFE_STEPS) {
            return null;
        }
        int density = map.density(method);
        if (density <= 0 || random.nextInt(180 * 16) >= density * 16) {
            return null;
        }
        return pick(map, method, random);
    }

    /** Cave wins anywhere; Land requires a grass tile. */
    private static String stepMethod(PbsData.EncounterMap map, int terrainTag) {
        if (hasTable(map, "Cave")) {
            return "Cave";
        }
        if ((terrainTag == TERRAIN_GRASS || terrainTag == TERRAIN_TALL_GRASS)) {
            for (String method : LAND_METHODS) {
                if (hasTable(map, method)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static boolean hasTable(PbsData.EncounterMap map, String method) {
        Array<PbsData.EncounterEntry> table = map.method(method);
        return table != null && table.size > 0 && map.density(method) > 0;
    }

    /** Picks a random row of a method (used by RockSmash and the rods). */
    public static WildEncounter pick(PbsData.EncounterMap map, String method, Random random) {
        Array<PbsData.EncounterEntry> table = map.method(method);
        if (table == null || table.size == 0) {
            return null;
        }
        PbsData.EncounterEntry entry = table.get(random.nextInt(table.size));
        return new WildEncounter(entry.species, entry.level(random));
    }
}
