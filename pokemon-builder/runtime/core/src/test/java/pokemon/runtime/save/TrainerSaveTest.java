package pokemon.runtime.save;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 3 / P1: the save document carries the trainer, the party and the PC
 * storage, and a pre-P1 (saveVersion 1) document still loads with an empty
 * party.
 */
class TrainerSaveTest {

    @Test
    @DisplayName("P1: party, PC storage, money and the heal point survive a round trip")
    void roundTrip(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        SaveManager saves = new SaveManager();
        saves.attachPbs(data);

        GameState original = new GameState();
        original.enterMap(3, 2, 2);
        original.trainer().name = "小明";
        original.trainer().money = 4321;
        original.trainer().setPokemonCenter(5, 10, 12, 2);

        Pokemon lead = new Pokemon(data.species("BULBASAUR"), 12, data);
        lead.ivs = new int[] {31, 30, 29, 28, 27, 26};
        lead.evs = new int[] {4, 0, 0, 252, 0, 252};
        lead.nature = data.nature("LONELY");
        lead.ability = "CHLOROPHYLL";
        lead.item = "LEFTOVERS";
        lead.shiny = true;
        lead.originalTrainer = "阿辽";
        lead.battleRank = 7;
        lead.ribbons.add("EFFORT");
        lead.hp = 1;
        lead.status = "POISON";
        original.trainer().party.add(lead);

        Pokemon boxed = new Pokemon(data.species("BULBASAUR"), 5, data);
        boxed.egg = true;
        original.trainer().storage.store(boxed);

        String json = saves.toJson(original);
        GameState restored = new GameState();
        assertTrue(saves.fromJson(json, restored), () -> "rejected: " + json);

        assertEquals("小明", restored.trainer().name);
        assertEquals(4321, restored.trainer().money);
        assertTrue(restored.trainer().hasPokemonCenter());
        assertEquals(5, restored.trainer().healMapId);
        assertEquals(10, restored.trainer().healX);
        assertEquals(12, restored.trainer().healY);
        assertEquals(2, restored.trainer().healDirection);

        assertEquals(1, restored.trainer().partyCount());
        Pokemon reloaded = restored.trainer().first();
        assertNotNull(reloaded);
        assertEquals("BULBASAUR", reloaded.species.internalName);
        assertEquals(12, reloaded.level);
        assertArrayEquals(new int[] {31, 30, 29, 28, 27, 26}, reloaded.ivs);
        assertArrayEquals(new int[] {4, 0, 0, 252, 0, 252}, reloaded.evs);
        assertEquals("Lonely", reloaded.nature.name);
        assertEquals("CHLOROPHYLL", reloaded.ability);
        assertEquals("LEFTOVERS", reloaded.item);
        assertTrue(reloaded.shiny);
        assertEquals("阿辽", reloaded.originalTrainer);
        assertEquals(7, reloaded.battleRank);
        assertTrue(reloaded.ribbons.contains("EFFORT", false), "ribbons survive the save");
        assertEquals(1, reloaded.hp);
        assertEquals("POISON", reloaded.status);

        assertEquals(1, restored.trainer().storage.count());
        assertTrue(restored.trainer().storage.get(0, 0).egg);
    }

    @Test
    @DisplayName("P1: a saveVersion 1 document loads with an empty party (migration)")
    void versionOneMigration() {
        String legacy = "{\"saveVersion\":1,\"playerName\":\"\",\"map\":{\"id\":2,\"x\":1,\"y\":1,\"direction\":2},"
                + "\"switches\":[],\"variables\":{},\"selfSwitches\":[],\"items\":{}}";
        GameState state = new GameState();
        SaveManager saves = new SaveManager();
        assertTrue(saves.fromJson(legacy, state));
        assertEquals(0, state.trainer().partyCount());
        assertEquals(0, state.trainer().storage.count());
        assertFalse(state.trainer().hasPokemonCenter());
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":1,\"species\":{\"BULBASAUR\":{"
                + "\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"Bulbasaur\","
                + "\"types\":[\"GRASS\"],\"baseStats\":[45,49,49,45,65,65],\"growthRate\":\"Medium\","
                + "\"abilities\":[\"OVERGROW\"],\"hiddenAbility\":\"CHLOROPHYLL\","
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        write(root, "moves.json", "{\"total\":1,\"moves\":{"
                + "\"TACKLE\":{\"id\":33,\"internalName\":\"TACKLE\",\"name\":\"Tackle\",\"pp\":35}}}");
        write(root, "items.json", "{\"total\":1,\"items\":{"
                + "\"LEFTOVERS\":{\"id\":211,\"internalName\":\"LEFTOVERS\",\"name\":\"Leftovers\"}}}");
        write(root, "abilities.json", "{\"total\":2,\"abilities\":{"
                + "\"OVERGROW\":{\"id\":65,\"internalName\":\"OVERGROW\",\"name\":\"Overgrow\"},"
                + "\"CHLOROPHYLL\":{\"id\":34,\"internalName\":\"CHLOROPHYLL\",\"name\":\"Chlorophyll\"}}}");
        write(root, "types.json", "{\"total\":1,\"types\":{\"GRASS\":{\"id\":1,\"internalName\":\"GRASS\"}}}");
        write(root, "natures.json", "{\"total\":2,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"},"
                + "{\"id\":1,\"internalName\":\"LONELY\",\"name\":\"Lonely\","
                + "\"statUp\":\"ATTACK\",\"statDown\":\"DEFENSE\"}]}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }
}
