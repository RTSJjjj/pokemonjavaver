package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * R6.30: the two project field interactions that need no Pokemon data - the
 * floating-plate puzzle switches ({@code toggle_liefeng_switches}) and the
 * Strength boulder push ({@code pbPushThisBoulder}). Both run against the real
 * generated project data, which is the only place with the plugin's event
 * names and the boulder layout.
 */
class FieldInteractionsTest {

    @Test
    @DisplayName("toggleFloatPlates marks the plate under the player (real map 60)")
    void toggleFloatPlates() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(60);
            List<MapData.EventData> plates = new ArrayList<>();
            for (MapData.EventData event : data.events) {
                if (event.name != null && event.name.contains("float_plate")) {
                    plates.add(event);
                }
            }
            assertTrue(plates.size() >= 2, "map 60 carries the plugin's floating plates");

            GameState state = new GameState();
            MapData.EventData on = plates.get(0);
            MapData.EventData off = plates.get(1);
            state.enterMap(60, on.x, on.y);

            FieldInteractions.toggleFloatPlates(state, data);

            assertTrue(state.selfSwitches().get(60, on.id, "A"),
                    "the plate under the player switches on");
            assertFalse(state.selfSwitches().get(60, off.id, "A"),
                    "every other plate switches off");

            state.setPlayerPosition(off.x, off.y);
            FieldInteractions.toggleFloatPlates(state, data);
            assertFalse(state.selfSwitches().get(60, on.id, "A"));
            assertTrue(state.selfSwitches().get(60, off.id, "A"));

            // Maps outside the plugin list never react to the function.
            MapData other = database.map(2);
            FieldInteractions.toggleFloatPlates(state, other);
            for (MapData.EventData event : other.events) {
                assertFalse(state.selfSwitches().get(2, event.id, "A"),
                        "map 2 must not be touched by the plate plugin");
            }
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("toggleFloatPlates reads the step's destination tile, as $game_player.x does")
    void toggleFloatPlatesUsesTheStepDestination() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(207);
            MapData.EventData on = null;
            MapData.EventData off = null;
            for (MapData.EventData event : data.events) {
                if (event.name != null && event.name.contains("float_plate")) {
                    if (on == null) {
                        on = event;
                    } else if (off == null) {
                        off = event;
                    }
                }
            }
            assertNotNull(on, "map 207 carries the plugin's floating plates");
            assertNotNull(off);

            GameState state = new GameState();
            // The player is still drawn on the old tile but has already
            // committed to the plate: RMXP's move_generic sets @x/@y when the
            // step starts, which is what $game_player.x reports here.
            MapCharacter player = new MapCharacter(on.x, on.y - 1, data.width, data.height,
                    "hero");
            player.isPlayer = true;
            assertTrue(player.startMove(on.x, on.y, 2), "the step onto the plate starts");
            assertEquals(on.x, player.logicalX(), "the logical tile is the destination");
            assertEquals(on.y, player.logicalY());
            assertEquals(on.y - 1, player.y(), "the drawn tile only moves when the step lands");

            state.enterMap(207, off.x, off.y);
            FieldInteractions.toggleFloatPlates(state, data, player.logicalX(), player.logicalY());

            assertTrue(state.selfSwitches().get(207, on.id, "A"),
                    "the plate the player is walking onto switches on during the step");
            assertFalse(state.selfSwitches().get(207, off.id, "A"),
                    "every other plate switches off");
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("every float_plate page pair is the RMXP shape the toggle needs (real data)")
    void floatPlatePageShape() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            int plates = 0;
            for (int mapId : new int[] { 60, 207, 209, 210, 371 }) {
                MapData data = database.map(mapId);
                for (MapData.EventData event : data.events) {
                    if (event.name == null || !event.name.contains("float_plate")) {
                        continue;
                    }
                    plates++;
                    assertEquals(2, event.pages.size,
                            "map " + mapId + " event " + event.id + " has the two plate pages");
                    MapData.EventPageData up = event.pages.get(0);
                    MapData.EventPageData down = event.pages.get(1);
                    assertEquals(0, up.trigger, "page 1 (self switch off) is an action page");
                    assertFalse(up.conditions.selfSwitchValid);
                    assertEquals(1, down.trigger,
                            "page 2 is Player Touch - the plate reacts to being stepped on");
                    assertTrue(down.conditions.selfSwitchValid);
                    assertEquals("A", down.conditions.selfSwitchCh);
                    assertTrue(down.commands.size > 0, "page 2 carries the plate's SE");

                    // Control Variable 26 = random(1..3), then three SE branches:
                    // the pitch the plate plays. operandType 2 is the random
                    // operand the interpreter used to reject.
                    boolean rolled = false;
                    for (int i = 0; i < down.commands.size; i++) {
                        pokemon.runtime.data.EventCommand command = down.commands.get(i);
                        if (command.code == 122 && command.parameters != null
                                && command.parameters.getInt(0) == 26
                                && command.parameters.getInt(3) == 2) {
                            assertEquals(1, command.parameters.getInt(4), "rand from 1");
                            assertEquals(3, command.parameters.getInt(5), "rand to 3");
                            rolled = true;
                        }
                    }
                    assertTrue(rolled, "map " + mapId + " event " + event.id
                            + " rolls the SE pitch before branching");
                }
            }
            assertTrue(plates >= 100, "the plugin's plate maps carry their plates: " + plates);
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("pushBoulder steps the boulder one tile in a free direction (real map 135)")
    void pushBoulder() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(135);
            TileMap map = new TileMap(data, database.tileset(data.tilesetId));
            MapData.EventData boulder = null;
            for (MapData.EventData event : data.events) {
                if (event.name != null && event.name.equalsIgnoreCase("Boulder")) {
                    boulder = event;
                    break;
                }
            }
            assertNotNull(boulder, "map 135 carries the boulder puzzle events");

            GameState state = new GameState();
            // The player may stand anywhere: the push only looks at the
            // boulder's own tile (passableStrict?) and the target tile.
            state.enterMap(135, boulder.x, boulder.y + 1);
            EventCharacters events = new EventCharacters(data, map, state);
            MapCharacter character = events.character(boulder.id);
            assertNotNull(character);

            int fromX = boulder.x;
            int fromY = boulder.y;
            int pushedDirection = 0;
            float seconds = 0f;
            for (int direction : new int[] { 6, 4, 8, 2 }) {
                float candidate = FieldInteractions.pushBoulder(state, map, data, events,
                        boulder.id, direction);
                if (candidate > 0f) {
                    pushedDirection = direction;
                    seconds = candidate;
                    break;
                }
            }
            assertTrue(pushedDirection != 0, "a real boulder has at least one free side");
            assertEquals(1f / character.speed(), seconds, 0.001f,
                    "the wait matches the character's step duration");
            assertEquals(0f, FieldInteractions.pushBoulder(state, map, data, events,
                            boulder.id, pushedDirection),
                    "a boulder cannot be pushed again mid-step");

            assertTrue(character.isMoving(), "the push starts the step animation");
            character.advance(1f);
            assertEquals(Collision.targetX(fromX, pushedDirection), character.x(),
                    "the boulder moved exactly one tile sideways");
            assertEquals(Collision.targetY(fromY, pushedDirection), character.y(),
                    "the boulder moved exactly one tile vertically");
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("strictlyPassable holds for the boulder's own tile")
    void strictlyPassableOnRealBoulder() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        GameDatabase database = GameDatabase.load(dataRoot.getAbsolutePath(), dataRoot);
        try {
            MapData data = database.map(135);
            TileMap map = new TileMap(data, database.tileset(data.tilesetId));
            MapData.EventData boulder = null;
            for (MapData.EventData event : data.events) {
                if (event.name != null && event.name.equalsIgnoreCase("Boulder")) {
                    boulder = event;
                    break;
                }
            }
            assertNotNull(boulder);
            GameState state = new GameState();
            state.enterMap(135, boulder.x, boulder.y + 1);
            assertTrue(FieldInteractions.strictlyPassable(state, map, data, boulder.id,
                    boulder.x, boulder.y), "the boulder stands on a fully passable tile");
            assertFalse(FieldInteractions.strictlyPassable(state, map, data, boulder.id,
                    -5, -5), "outside the map is never passable");
        } finally {
            database.dispose();
        }
    }
}
