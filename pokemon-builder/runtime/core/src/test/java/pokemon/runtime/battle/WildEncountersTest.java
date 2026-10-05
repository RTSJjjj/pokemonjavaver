package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 3 / P2: the step-encounter roll (Cave anywhere, Land on grass, the
 * density/180 chance and the three safe steps).
 */
class WildEncountersTest {

    /** nextInt always returns 0: the roll always succeeds. */
    private static final Random ALWAYS = new Random() {
        @Override
        public int nextInt(int bound) {
            return 0;
        }
    };
    /** nextInt always returns bound-1: the roll always fails. */
    private static final Random NEVER = new Random() {
        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }
    };

    @Test
    @DisplayName("P2: Land needs a grass tile and three safe steps precede a roll")
    void landEncounters(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        WildEncounters encounters = new WildEncounters();
        encounters.onMap(10);

        for (int i = 0; i < 3; i++) {
            assertNull(encounters.roll(data, 10, WildEncounters.TERRAIN_GRASS, ALWAYS),
                    "step " + (i + 1) + " is safe");
        }
        WildEncounters.WildEncounter hit =
                encounters.roll(data, 10, WildEncounters.TERRAIN_GRASS, ALWAYS);
        assertNotNull(hit);
        assertEquals("FOE", hit.species);
        assertTrue(hit.level >= 5 && hit.level <= 8, "level " + hit.level);

        // A neutral tile has no Land encounters.
        encounters.reset();
        for (int i = 0; i < 3; i++) {
            encounters.roll(data, 10, 13, ALWAYS);
        }
        assertNull(encounters.roll(data, 10, 13, ALWAYS), "neutral tile stays quiet");
    }

    @Test
    @DisplayName("P2: the density decides the roll, and Cave works on any tile")
    void caveAndDensity(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        WildEncounters encounters = new WildEncounters();

        // Land density 15: a failing roll never triggers.
        encounters.onMap(10);
        for (int i = 0; i < 3; i++) {
            encounters.roll(data, 10, WildEncounters.TERRAIN_GRASS, NEVER);
        }
        assertNull(encounters.roll(data, 10, WildEncounters.TERRAIN_GRASS, NEVER));

        // Map 11 has Cave encounters; any tile works.
        encounters.onMap(11);
        for (int i = 0; i < 3; i++) {
            encounters.roll(data, 11, 13, ALWAYS);
        }
        WildEncounters.WildEncounter cave = encounters.roll(data, 11, 13, ALWAYS);
        assertNotNull(cave);
        assertEquals("CAVEFOE", cave.species);

        // pick() skips the probability roll (RockSmash).
        WildEncounters.WildEncounter smashed =
                WildEncounters.pick(data.encounterMap(10), "Land", new Random(1));
        assertNotNull(smashed);
        assertNull(WildEncounters.pick(data.encounterMap(10), "Cave", new Random(1)),
                "map 10 has no Cave table");
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "encounters.json", "{\"total\":2,\"byMap\":{"
                + "\"10\":{\"id\":10,\"name\":\"Route 1\","
                + "\"densities\":{\"Land\":15,\"Cave\":0,\"Water\":0,\"LandDay\":15,\"LandNight\":15},"
                + "\"methods\":{\"Land\":[{\"species\":\"FOE\",\"min\":5,\"max\":8}]}},"
                + "\"11\":{\"id\":11,\"name\":\"Cave\","
                + "\"densities\":{\"Land\":0,\"Cave\":10},"
                + "\"methods\":{\"Cave\":[{\"species\":\"CAVEFOE\",\"min\":9,\"max\":9}]}}}}");
        return root.toFile();
    }
}
