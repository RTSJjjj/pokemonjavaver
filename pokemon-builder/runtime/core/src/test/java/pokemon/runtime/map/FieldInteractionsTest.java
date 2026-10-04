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
