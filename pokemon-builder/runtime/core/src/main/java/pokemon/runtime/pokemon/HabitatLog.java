package pokemon.runtime.pokemon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 294_Boonzeet_s_Habitat_List {@code Habitats} and the {@code $Trainer.habitatData / habitatPokeIndexes / habitatMapIndexes} it
 * keeps: for every habitat of {@link HabitatConfig} and every kind of encounter it has (grass, cave, surf, fish) the Pokemon that
 * can be met there, whether all of them were seen / caught, and whether the player has been told (the "alert" that plays the stamp
 * animation when the habitat is looked at).
 *
 * <p>登记: the plugin compares the raw encounter entry with {@code $Trainer.owned} when it updates a kind
 * ({@code updateHabitatTypeStatus}, :482-505), so a form entry ({@code RATICATE_1}) is never "owned" there and its habitat could
 * never be completed again after the first update; the base species is compared here, which is what the list clearly means.
 * {@code MetadataMapSize} (areas of several tiles) is not in this project's PBS (no map has it), so a map is one tile.</p>
 */
public final class HabitatLog {

    /** {@code habitatData[i][:encounters][kind]}. */
    public static final class Kind {
        public boolean alert = true;
        public boolean seen = true;
        public boolean owned = true;
        /** The encounter entries as the encounter tables name them (a form is {@code SPECIES_N}). */
        public final List<String> list = new ArrayList<>();
    }

    /** {@code habitatData[i]}. */
    public static final class Habitat {
        public int[] mapIds = new int[0];
        public boolean completed;
        /** In the plugin's order (the config's kinds). */
        public final Map<String, Kind> encounters = new LinkedHashMap<>();
    }

    /** One row of {@code Habitats.getHabitatList}: {@code [index, first map id, encounters, completed]}. */
    public static final class Entry {
        public final int index;
        public final int mapId;
        public final Habitat habitat;

        Entry(int index, int mapId, Habitat habitat) {
            this.index = index;
            this.mapId = mapId;
            this.habitat = habitat;
        }
    }

    private final TrainerState trainer;
    public final List<Habitat> data = new ArrayList<>();
    /** {@code habitatPokeIndexes}: base species -> the habitats it appears in. */
    private final Map<String, List<Integer>> pokeIndexes = new HashMap<>();
    /** {@code habitatMapIndexes}: map id -> habitat index. */
    private final Map<Integer, Integer> mapIndexes = new HashMap<>();

    public HabitatLog(TrainerState trainer) {
        this.trainer = trainer;
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    /** {@code Habitats.clear} (:451-456): wipes everything. */
    public void clear() {
        data.clear();
        pokeIndexes.clear();
        mapIndexes.clear();
    }

    // ---------------------------------------------------------------- setup / update

    /** {@code Habitats.setup} (:458-466): the first-run setup from the config (and the way to add habitats to an existing save). */
    public void setup(PbsData pbs) {
        clear();
        for (int i = 0; i < HabitatConfig.HABITAT_MAPS.length; i++) {
            data.add(addHabitat(pbs, HabitatConfig.HABITAT_MAPS[i], HabitatConfig.HABITAT_KINDS[i], i));
        }
    }

    /** {@code Habitats.update} (:468-479): re-reads the encounter tables without clearing progress; new config entries are appended. */
    public void update(PbsData pbs) {
        for (int i = 0; i < HabitatConfig.HABITAT_MAPS.length; i++) {
            if (i < data.size()) {
                updateHabitat(pbs, HabitatConfig.HABITAT_MAPS[i], HabitatConfig.HABITAT_KINDS[i], i);
            } else {
                data.add(addHabitat(pbs, HabitatConfig.HABITAT_MAPS[i], HabitatConfig.HABITAT_KINDS[i], i));
            }
        }
    }

    /** {@code Habitats.addHabitat} (:388-416). */
    private Habitat addHabitat(PbsData pbs, int[] mapIds, String[] kinds, int index) {
        Habitat habitat = new Habitat();
        habitat.mapIds = mapIds.clone();                                        // :mapIDs
        for (int map : mapIds) {
            mapIndexes.put(map, index);
        }
        for (String kind : kinds) {
            habitat.encounters.put(kind, new Kind());
        }
        for (int map : mapIds) {
            for (String kind : kinds) {
                loadEncountersForType(pbs, habitat.encounters.get(kind), map, kind, index);
            }
        }
        return habitat;
    }

    /** {@code Habitats.updateHabitat} (:418-449). */
    private void updateHabitat(PbsData pbs, int[] mapIds, String[] kinds, int index) {
        Habitat existing = data.get(index);
        for (int map : mapIds) {
            mapIndexes.put(map, index);
        }
        for (String kind : kinds) {
            existing.encounters.computeIfAbsent(kind, k -> new Kind());
        }
        for (int map : mapIds) {
            for (String kind : kinds) {
                Kind type = existing.encounters.get(kind);
                loadEncountersForType(pbs, type, map, kind, index);
                if (!type.owned) {
                    existing.completed = false;
                }
            }
        }
    }

    /** {@code Habitats.loadEncountersForType} (:367-386): the Pokemon of one map for one kind, added to the kind's list. */
    private void loadEncountersForType(PbsData pbs, Kind kind, int mapId, String kindName, int index) {
        List<String> pokes = new ArrayList<>();
        PbsData.EncounterMap table = pbs == null ? null : pbs.encounterMap(mapId);
        if (table != null) {
            for (String type : HabitatConfig.encounterTypes(kindName)) {
                com.badlogic.gdx.utils.Array<PbsData.EncounterEntry> entries = table.method(type);   // hasEncounter?
                if (entries == null) continue;
                for (int i = 0; i < entries.size; i++) {
                    String species = entries.get(i).species;
                    if (species != null && !pokes.contains(species)) pokes.add(species);              // pokes.uniq!
                }
            }
        }
        for (String name : pokes) {
            processEncounterPokemon(pbs, name, kind, index);
        }
        for (String name : pokes) {
            if (!kind.list.contains(name)) kind.list.add(name);                 // habitatEncounter[:list].push(*pokes).uniq!
        }
    }

    /** {@code Habitats.processEncounterPokemon} (:348-365). */
    private void processEncounterPokemon(PbsData pbs, String name, Kind kind, int index) {
        String base = baseSpecies(pbs, name);
        List<Integer> indexes = pokeIndexes.computeIfAbsent(base, k -> new ArrayList<>());
        if (!indexes.contains(index)) indexes.add(index);                       // (duplicates add nothing)
        if (!trainer.owned.contains(base)) {
            kind.owned = false;
            if (!trainer.seen.contains(base)) {
                kind.seen = false;
                kind.alert = false;
            }
        }
    }

    /** {@code pbGetSpeciesFromFSpecies(n)[0]}: the species a form entry belongs to. */
    public static String baseSpecies(PbsData pbs, String name) {
        PbsData.Species species = pbs == null ? null : pbs.species(name);
        return species == null ? name : species.internalName;
    }

    /** The form number of an encounter entry ({@code SPECIES_N} -> N, else 0). */
    public static int formOf(String name) {
        int cut = name.lastIndexOf('_');
        if (cut > 0) {
            String suffix = name.substring(cut + 1);
            if (!suffix.isEmpty() && suffix.chars().allMatch(Character::isDigit)) {
                return Integer.parseInt(suffix);
            }
        }
        return 0;
    }

    // ---------------------------------------------------------------- status

    /** {@code Habitats.updateHabitatTypeStatus} (:482-505): seen / owned of a kind, and the alert when something changed. */
    private void updateHabitatTypeStatus(PbsData pbs, Kind kind) {
        boolean seen = true;
        boolean owned = true;
        for (String name : kind.list) {
            String base = baseSpecies(pbs, name);
            if (!trainer.owned.contains(base)) {
                owned = false;
                if (!trainer.seen.contains(base)) {
                    seen = false;
                    break;
                }
            }
        }
        if (seen != kind.seen || owned != kind.owned) {
            kind.alert = true;                                                  // anything changed, completed included
        }
        if (owned && !kind.owned) {
            kind.alert = true;
        }
        kind.seen = seen;
        kind.owned = owned;
    }

    /** {@code Habitats.updateHabitatStatus(index)} (:512-520). */
    public void updateHabitatStatus(PbsData pbs, int index) {
        if (index > -1 && index < data.size()) {
            for (Kind kind : data.get(index).encounters.values()) {
                updateHabitatTypeStatus(pbs, kind);
            }
        }
    }

    private PbsData pbsForUpdates;

    /** The PBS the species updates compare names with (set once the game's data is loaded). */
    public void attachPbs(PbsData pbs) {
        this.pbsForUpdates = pbs;
    }

    /** {@code Habitats.updateHabitatsForSpecies(species)} (:522-533): called when a species is seen or caught. */
    public void updateForSpecies(String base) {
        List<Integer> ids = pokeIndexes.get(base);
        if (ids == null) return;
        for (int id : ids) {
            updateHabitatStatus(pbsForUpdates, id);
        }
    }

    // ---------------------------------------------------------------- lookups

    /** {@code Habitats.getIndexByMapID} (:507-510). */
    public int indexByMapId(int mapId) {
        Integer index = mapIndexes.get(mapId);
        return index == null ? -1 : index;
    }

    /** {@code Habitats.visited?(index)} (:535-550): the player has been on one of the habitat's maps. */
    public boolean visited(int index, Set<Integer> visitedMaps) {
        if (index > data.size() || index < 0 || index >= data.size()) {
            return false;
        }
        for (int map : data.get(index).mapIds) {
            if (visitedMaps.contains(map)) return true;
        }
        return false;
    }

    /**
     * {@code Habitats.checkMapHabitat(mapID, type)} (:552-567): 2 when every Pokemon of that kind was caught, 1 when every one was
     * seen, 0 when not; -1 (the plugin shows "地图#N中的分布未设置。" and returns nil) when the map has no habitat or no such kind.
     */
    public int checkMapHabitat(int mapId, String kindName) {
        int index = indexByMapId(mapId);
        if (index == -1) return -1;
        Kind kind = data.get(index).encounters.get(kindName);
        if (kind == null) return -1;
        return kind.owned ? 2 : kind.seen ? 1 : 0;
    }

    /** {@code Habitats.getHabitatList} (:569-582): the habitats the player has been to, in the config's order. */
    public List<Entry> habitatList(Set<Integer> visitedMaps) {
        List<Entry> list = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            if (visited(i, visitedMaps)) {
                Habitat habitat = data.get(i);
                list.add(new Entry(i, habitat.mapIds.length == 0 ? 0 : habitat.mapIds[0], habitat));
            }
        }
        return list;
    }

    /**
     * {@code Habitats.getIndexByRegionCoords(x, y, region, mapdata)} (:584-635): the habitat of the map that sits on that tile of the
     * region map (the first map by id), when the player has been there; -1 otherwise. 登记: the plugin's second search (a map
     * spread over several tiles) needs {@code MetadataMapSize}, which this project's PBS never sets.
     */
    public int indexByRegionCoords(PbsData pbs, int x, int y, int region, Set<Integer> visitedMaps) {
        Set<Integer> ids = new TreeSet<>(pbs.metadataMapIds());
        for (int mapId : ids) {
            PbsData.Metadata meta = pbs.mapMetadata(mapId);
            if (meta == null || meta.mapPosition == null || meta.mapPosition.length < 3) continue;
            int[] position = meta.mapPosition;
            if (position[0] != region) continue;
            if (position[1] == x && position[2] == y) {
                int index = indexByMapId(mapId);
                return visited(index, visitedMaps) ? index : -1;
            }
        }
        return -1;
    }

    /** The first match of {@code HabitatConfig::RegionOverride} for a region map tile, or null. */
    public static int[] regionOverride(int region, int x, int y) {
        for (int i = 0; i < HabitatConfig.REGION_OVERRIDE_KEYS.length; i++) {
            int[] key = HabitatConfig.REGION_OVERRIDE_KEYS[i];
            if (key[0] == region && key[1] == x && key[2] == y) {
                return HabitatConfig.REGION_OVERRIDE_MAPS[i];
            }
        }
        return null;
    }

    /** Rebuilds the two indexes from {@link #data} (they are not saved): the maps of each habitat and the species of each list. */
    public void rebuildIndexes(PbsData pbs) {
        pokeIndexes.clear();
        mapIndexes.clear();
        for (int i = 0; i < data.size(); i++) {
            Habitat habitat = data.get(i);
            for (int map : habitat.mapIds) mapIndexes.put(map, i);
            for (Kind kind : habitat.encounters.values()) {
                for (String name : kind.list) {
                    List<Integer> indexes = pokeIndexes.computeIfAbsent(baseSpecies(pbs, name), k -> new ArrayList<>());
                    if (!indexes.contains(i)) indexes.add(i);
                }
            }
        }
    }
}
