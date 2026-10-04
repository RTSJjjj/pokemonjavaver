package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.files.FileHandle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * R6.16: the per-map {@code Outdoor} flag the Builder exports from
 * PBS/metadata.txt. The project's pbDayNightTint shades only outdoor maps, so
 * the runtime needs the flag to leave rooms unshaded.
 */
class MapDataOutdoorTest {

    @Test
    @DisplayName("outdoor / indoor / legacy data are told apart")
    void parsesOutdoorFlag() {
        JsonReader reader = new JsonReader();
        assertEquals(Boolean.TRUE, MapData.parse(reader.parse("{outdoor: true}")).outdoor);
        assertEquals(Boolean.FALSE, MapData.parse(reader.parse("{outdoor: false}")).outdoor);
        assertNull(MapData.parse(reader.parse("{}")).outdoor,
                "generated data from before R6.16 has no flag");
    }

    @Test
    @DisplayName("the real project marks towns outdoor and rooms indoor")
    void realProjectFlags() {
        File dataRoot = runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        File town = new File(dataRoot, "maps/map-002.json");
        File room = new File(dataRoot, "maps/map-353.json");
        assumeTrue(town.isFile() && room.isFile(), "map IR not available");

        JsonReader reader = new JsonReader();
        JsonValue townRoot = reader.parse(new FileHandle(town));
        assumeTrue(townRoot.get("outdoor") != null,
                "run builder build-data to export the R6.16 Outdoor flag");
        assertTrue(MapData.parse(townRoot).outdoor,
                "map 2 is an outdoor town (PBS/metadata.txt)");
        assertFalse(MapData.parse(reader.parse(new FileHandle(room))).outdoor,
                "map 353 is an indoor room");
    }

    /** generated/ sits next to the runtime checkout; also honour the property. */
    private static File runtimeDataRoot() {
        String configured = System.getProperty("pokemon.runtime.data");
        if (configured != null) {
            File file = new File(configured);
            if (file.isDirectory()) {
                return file;
            }
        }
        String[] candidates = { "../../generated", "../generated", "generated",
                "E:/仓库/范例/929/pokemon-builder/generated" };
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (new File(file, "project.json").isFile()) {
                return file;
            }
        }
        return null;
    }
}
