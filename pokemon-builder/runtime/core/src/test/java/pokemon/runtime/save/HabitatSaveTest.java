package pokemon.runtime.save;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.HabitatLog;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.GameState;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 294 Habitat List: the habitat data follows seen / owned, survives a save, and old saves get it built. */
class HabitatSaveTest {

    private static final int MAP = 10;       // 一号道路: grass, surf, fish

    @Test
    @DisplayName("seeing then catching the only grass species marks the kind seen then owned, and the log round-trips")
    void followsSeenAndOwned(@TempDir Path dir) throws Exception {
        PbsData pbs = PbsData.parse(pbs(dir));
        SaveManager saves = new SaveManager();
        saves.attachPbs(pbs);
        GameState state = new GameState();
        state.enterMap(3, 2, 2);
        state.trainer().habitats.attachPbs(pbs);
        state.trainer().habitats.setup(pbs);
        int index = state.trainer().habitats.indexByMapId(MAP);
        assertTrue(index >= 0);
        HabitatLog.Kind grass = state.trainer().habitats.data.get(index).encounters.get("grass");
        assertFalse(grass.seen);
        assertFalse(grass.owned);
        assertEquals(java.util.List.of("BULBASAUR"), grass.list);

        state.trainer().setSeen("BULBASAUR");
        assertTrue(grass.seen);
        assertFalse(grass.owned);
        state.trainer().setOwned("BULBASAUR");
        assertTrue(grass.owned);

        GameState restored = new GameState();
        assertTrue(saves.fromJson(saves.toJson(state), restored));
        HabitatLog.Kind again = restored.trainer().habitats.data.get(index).encounters.get("grass");
        assertTrue(again.owned);
        assertEquals(java.util.List.of("BULBASAUR"), again.list);
        assertEquals(index, restored.trainer().habitats.indexByMapId(MAP));
        assertTrue(restored.trainer().habitats.habitatList(Set.of(99)).isEmpty());
        assertEquals(1, restored.trainer().habitats.habitatList(Set.of(MAP)).size());
    }

    @Test
    @DisplayName("a save without the habitat key gets the list built from what was already seen")
    void oldSaveIsMigrated(@TempDir Path dir) throws Exception {
        PbsData pbs = PbsData.parse(pbs(dir));
        SaveManager saves = new SaveManager();
        saves.attachPbs(pbs);
        GameState state = new GameState();
        state.enterMap(3, 2, 2);
        state.trainer().seen.add("BULBASAUR");
        state.trainer().owned.add("BULBASAUR");
        String json = saves.toJson(state).replace("\"habitats\"", "\"habitatsGone\"");
        GameState restored = new GameState();
        assertTrue(saves.fromJson(json, restored));
        int index = restored.trainer().habitats.indexByMapId(MAP);
        assertTrue(index >= 0);
        assertTrue(restored.trainer().habitats.data.get(index).encounters.get("grass").owned);
    }

    private static java.io.File pbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":1,\"species\":{\"BULBASAUR\":{"
                + "\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"Bulbasaur\","
                + "\"types\":[\"GRASS\"],\"baseStats\":[45,49,49,45,65,65],\"growthRate\":\"Medium\","
                + "\"abilities\":[\"OVERGROW\"],\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        write(root, "moves.json", "{\"total\":1,\"moves\":{"
                + "\"TACKLE\":{\"id\":33,\"internalName\":\"TACKLE\",\"name\":\"Tackle\",\"pp\":35,\"target\":\"NearOther\"}}}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "abilities.json", "{\"total\":1,\"abilities\":{"
                + "\"OVERGROW\":{\"id\":65,\"internalName\":\"OVERGROW\",\"name\":\"Overgrow\"}}}");
        write(root, "types.json", "{\"total\":1,\"types\":{\"GRASS\":{\"id\":1,\"internalName\":\"GRASS\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":[{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        write(root, "encounters.json", "{\"byMap\":{\"10\":{\"id\":10,\"name\":\"R1\",\"methods\":{"
                + "\"Land\":[{\"species\":\"BULBASAUR\",\"min\":2,\"max\":4}]}}}}");
        return root.toFile();
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }
}
