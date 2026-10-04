package pokemon.runtime.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless data layer tests (project3 section 61): no OpenGL, no Android - the
 * Runtime Data API must be verifiable without a graphics context.
 */
class GameDatabaseTest {

    @TempDir
    Path dir;

    private void write(String relative, String text) throws IOException {
        Path target = dir.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.write(target, text.getBytes(StandardCharsets.UTF_8));
    }

    private void writeTinyRuntimeData() throws IOException {
        write("project.json", """
            {
              "format": "pokemon-builder/debug-ir/1",
              "kind": "project",
              "projectName": "FakeProject",
              "builderVersion": "0.1.0",
              "source": { "project": "FakeProject", "dataDir": "FakeProject/Data" },
              "counts": { "maps": 1, "tilesets": 1 },
              "outputs": { "maps": "maps/index.json", "tilesets": "tilesets.json", "system": "system.json" },
              "status": "ok"
            }
            """);
        write("maps/index.json", """
            {
              "format": "pokemon-builder/debug-ir/1",
              "kind": "maps",
              "total": 1,
              "maps": [
                { "mapId": 1, "name": "Starting Map", "file": "Map001.rxdata", "width": 2, "height": 2,
                  "tilesetId": 1, "events": 1, "pages": 1, "commands": 2, "scriptBlocks": 1,
                  "ir": "maps/map-001.json" }
              ]
            }
            """);
        write("maps/map-001.json", """
            {
              "format": "pokemon-builder/debug-ir/1",
              "kind": "map",
              "mapId": 1,
              "name": "Starting Map",
              "file": "Map001.rxdata",
              "width": 2,
              "height": 2,
              "tilesetId": 1,
              "encounterStep": 30,
              "bgm": { "name": "Town", "volume": 80, "pitch": 100 },
              "bgs": { "name": "", "volume": 80, "pitch": 100 },
              "autoplayBgm": false,
              "autoplayBgs": false,
              "encounters": [],
              "tileData": {
                "present": true, "z": 3, "x": 2, "y": 2, "total": 12,
                "layers": [[10, 11, 12, 13], [0, 0, 0, 0], [2048, 0, 0, 0]]
              },
              "events": [
                { "id": 1, "name": "Professor", "x": 1, "y": 1, "scriptBlockIds": ["map1/event1/page1/cmd0"],
                  "pages": [ { "page": 1, "trigger": 0,
                    "conditions": { "selfSwitchCh": "A", "selfSwitchValid": false, "switch1Valid": false,
                      "switch1Id": 1, "switch2Valid": false, "switch2Id": 2, "variableValid": false,
                      "variableId": 0, "variableValue": 0 },
                    "graphic": { "characterName": "prof", "characterHue": 0, "direction": 2, "pattern": 0,
                      "tileId": 0, "opacity": 255, "blendType": 0 },
                    "movement": null,
                    "commands": [
                      { "index": 0, "code": 101, "indent": 0, "parameters": ["Look at me!", 0], "scriptBlockId": null },
                      { "index": 1, "code": 355, "indent": 0, "parameters": ["pbSet(1,1)"], "scriptBlockId": "map1/event1/page1/cmd1" }
                    ] } ] }
              ],
              "scriptBlockIds": ["map1/event1/page1/cmd1"]
            }
            """);
        write("tilesets.json", """
            {
              "format": "pokemon-builder/debug-ir/1",
              "kind": "tilesets",
              "total": 1,
              "tilesets": [
                { "id": 1, "name": "Fake Tiles", "tilesetName": "Fake", "autotileNames": ["", "", "", "", "", "", ""],
                  "panoramaName": "", "panoramaHue": 0, "fogName": "", "fogHue": 0, "fogOpacity": 64,
                  "fogBlendType": 0, "fogZoom": 200, "fogSx": 0, "fogSy": 0, "battlebackName": "",
                  "passages": { "present": true, "z": 1, "x": 4, "y": 1, "total": 4, "layers": [[0, 15, 0, 15]] },
                  "priorities": { "present": true, "z": 1, "x": 4, "y": 1, "total": 4, "layers": [[0, 1, 0, 1]] },
                  "terrainTags": { "present": true, "z": 1, "x": 4, "y": 1, "total": 4, "layers": [[0, 0, 0, 0]] } }
              ]
            }
            """);
        write("system.json", """
            {
              "format": "pokemon-builder/debug-ir/1",
              "kind": "system",
              "magicNumber": 0,
              "startMapId": 1,
              "startX": 9,
              "startY": 7,
              "editMapId": 1,
              "windowskinName": "", "titleName": "", "gameoverName": "", "battlebackName": "",
              "elements": ["", "Normal"],
              "switches": ["", "SW1"],
              "variables": ["", "VAR1"],
              "words": [["hp", "HP"]]
            }
            """);
        write("common-events/index.json", """
            {
              "format": "pokemon-builder/debug-ir/1",
              "kind": "commonEvents",
              "total": 1,
              "commonEvents": [
                { "id": 1, "slot": 1, "name": "Intro Setup", "trigger": 0, "switchId": 0,
                  "commands": 1, "scriptBlocks": 0, "ir": "common-events/common-event-001.json" }
              ]
            }
            """);
        write("common-events/common-event-001.json", """
            {
              "format": "pokemon-builder/debug-ir/1",
              "kind": "commonEvent",
              "id": 1,
              "slot": 1,
              "name": "Intro Setup",
              "trigger": 0,
              "switchId": 0,
              "commands": [ { "index": 0, "code": 121, "indent": 0, "parameters": [1, 2], "scriptBlockId": null } ],
              "scriptBlockIds": []
            }
            """);
    }

    @Test
    @DisplayName("loads the project manifest, indexes and the System start position")
    void loadsManifestsAndIndexes() throws IOException {
        writeTinyRuntimeData();
        GameDatabase database = GameDatabase.load(dir.toString(), dir.toFile());

        assertEquals("FakeProject", database.projectName());
        assertEquals(1, database.mapCount());
        assertEquals(1, database.maps().size);
        assertEquals(1, database.tilesets().size);
        assertEquals(1, database.commonEvents().size);

        assertNotNull(database.system());
        assertEquals(1, database.startMapId());
        assertEquals(9, database.startX());
        assertEquals(7, database.startY());
        assertEquals("HP", database.system().words[0][1]);
    }

    @Test
    @DisplayName("parses the decoded tile tables and event commands")
    void parsesMapsAndEvents() throws IOException {
        writeTinyRuntimeData();
        GameDatabase database = GameDatabase.load(dir.toString(), dir.toFile());

        MapData map = database.map(1);
        assertEquals(2, map.width);
        assertEquals(2, map.height);
        assertEquals(1, map.tilesetId);
        assertEquals("Town", map.bgm.name);
        assertEquals(80, map.bgm.volume);

        assertNotNull(map.tileData.layers);
        assertEquals(3, map.tileData.layers.length);
        assertEquals(12, map.tileData.total);
        assertEquals(10, map.tileData.tile(0, 0, 0));
        assertEquals(11, map.tileData.tile(0, 1, 0));
        assertEquals(2048, map.tileData.tile(2, 0, 0));

        MapData.EventData event = map.events.get(0);
        assertEquals("Professor", event.name);
        MapData.EventPageData page = event.pages.get(0);
        assertEquals(2, page.commands.size);
        assertEquals(101, page.commands.get(0).code);
        assertEquals("Look at me!", page.commands.get(0).parameter(0).asString());
        // parameters[1] is the Show Text face position: 0 in the fixture.\n        assertEquals(0, page.commands.get(0).parameter(1).asInt());
        assertEquals("map1/event1/page1/cmd1", page.commands.get(1).scriptBlockId);

        TilesetData tileset = database.tileset(1);
        assertNotNull(tileset);
        assertEquals(15, tileset.passage(1));
        assertEquals(1, tileset.priority(1));
        assertEquals(0, tileset.priority(0));
    }

    @Test
    @DisplayName("parses common events and reports a missing audio manifest")
    void parsesCommonEventsAndMissingAudioManifest() throws IOException {
        writeTinyRuntimeData();
        GameDatabase database = GameDatabase.load(dir.toString(), dir.toFile());

        CommonEventData commonEvent = database.commonEvent(1);
        assertEquals("Intro Setup", commonEvent.name);
        assertEquals(1, commonEvent.commands.size);
        assertEquals(121, commonEvent.commands.get(0).code);
        assertEquals(1, commonEvent.commands.get(0).parameter(0).asInt());

        assertFalse(database.audioManifestLoaded());
        assertNull(database.audioEntry("Battle01"));
    }

    @Test
    @DisplayName("fails with a clear reason when the runtime data is missing")
    void failsWithoutRuntimeData() {
        Path empty = dir.resolve("empty");
        assertThrows(RuntimeDataException.class, () -> GameDatabase.load(empty.toString(), dir.toFile()));
    }

    @Test
    @DisplayName("rejects a map id that is not in the index")
    void rejectsUnknownMapId() throws IOException {
        writeTinyRuntimeData();
        GameDatabase database = GameDatabase.load(dir.toString(), dir.toFile());
        assertThrows(RuntimeDataException.class, () -> database.map(99));
    }
}