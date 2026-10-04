package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.TilesetData;
import pokemon.runtime.input.DefaultKeyBindings;
import pokemon.runtime.input.InputSampler;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.input.KeyStateSource;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless tests for the R4 movement system: passage bits, event blocking,
 * delta-time steps and the key sampler (project3 section 61).
 */
class MovementControllerTest {

    private static final int DOWN = 2;
    private static final int LEFT = 4;
    private static final int RIGHT = 6;
    private static final int UP = 8;

    /** R5: R4 collision checks now need live switches / variables. */
    private static final GameState STATE = new GameState();

    private static boolean canStep(TileMap map, MapData data, MapCharacter character, int direction) {
        return Collision.canStep(STATE, map, data, character, direction);
    }

    private static TilesetData tileset(int[] passages) {
        TilesetData data = new TilesetData();
        data.id = 1;
        data.name = "Fake";
        data.tilesetName = "Fake";
        data.autotileNames = new String[] { "a0", "a1", "a2", "a3", "a4", "a5", "a6" };
        data.passages = table(passages);
        data.priorities = table(new int[8]);
        data.terrainTags = table(null);
        return data;
    }

    private static TilesetData.TableData table(int[] values) {
        TilesetData.TableData table = new TilesetData.TableData();
        table.present = values != null;
        table.z = 1;
        table.x = 8;
        table.y = 1;
        table.total = values == null ? 0 : values.length;
        table.layers = values == null ? null : new int[][] { values };
        return table;
    }

    /** 5x3 map; ground layer filled with the given tile id. */
    private static MapData mapData(int fill) {
        MapData data = new MapData();
        data.mapId = 1;
        data.width = 5;
        data.height = 3;
        data.tilesetId = 1;
        MapData.TileData tileData = new MapData.TileData();
        tileData.present = true;
        tileData.z = 3;
        tileData.x = 5;
        tileData.y = 3;
        tileData.total = 5 * 3 * 3;
        tileData.layers = new int[3][5 * 3];
        for (int[] layer : tileData.layers) {
            java.util.Arrays.fill(layer, fill);
        }
        data.tileData = tileData;
        return data;
    }

    private static MapData.EventData event(int x, int y) {
        MapData.EventData event = new MapData.EventData();
        event.id = 1;
        event.name = "Sign";
        event.x = x;
        event.y = y;
        MapData.EventPageData page = new MapData.EventPageData();
        page.page = 1;
        page.trigger = 0;
        page.graphic = new MapData.EventGraphic();
        page.graphic.characterName = "boy_walk";
        event.pages.add(page);
        return event;
    }

    // ------------------------------------------------------------------

    @Test
    @DisplayName("a step interpolates with delta time and lands exactly on the tile")
    void stepInterpolatesWithDelta() {
        MapCharacter character = new MapCharacter(2, 1, 5, 3, "boy_walk");
        assertTrue(character.startMove(3, 1, RIGHT));

        character.update(0.125f);           // half a tile at 4 tiles/sec
        assertTrue(character.isMoving());
        assertEquals(2.5f * 32f, character.pixelX(), 0.01f);

        character.update(0.125f);           // step complete
        assertFalse(character.isMoving());
        assertEquals(3, character.x());
        assertEquals(1, character.y());
    }

    @Test
    @DisplayName("walk animation cycles while moving and rests at the page's column")
    void walkAnimation() {
        MapCharacter character = new MapCharacter(2, 1, 5, 3, "boy_walk");
        assertEquals(0, character.pattern());
        character.startMove(3, 1, RIGHT);
        character.updateAnimation(1f / 60f);
        assertEquals(1, character.pattern(), "started walking: first animation column");
        // Walking speed 4 = half a tile (5 frames at 40 fps) per animation column.
        character.updateAnimation(5f / 40f);
        assertEquals(2, character.pattern());
        character.advance(1f);
        character.updateAnimation(1f / 60f);
        assertEquals(0, character.pattern());   // standing: back to the page column
    }

    @Test
    @DisplayName("collision blocks a wall tile and lets open ground through")
    void collisionBits() {
        MapData data = mapData(1);          // every ground tile is tile id 1
        TilesetData blocked = tileset(new int[] { 0, 15, 15, 15, 15, 15, 15, 15 });
        TileMap map = new TileMap(data, blocked);
        MapCharacter character = new MapCharacter(2, 1, 5, 3, "boy_walk");
        assertFalse(canStep(map, data, character, RIGHT));   // tile 1 blocks
        assertFalse(canStep(map, data, character, LEFT));    // tile 1 blocks all directions

        TilesetData open = tileset(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });
        TileMap openMap = new TileMap(data, open);
        assertTrue(canStep(openMap, data, character, RIGHT));
    }

    @Test
    @DisplayName("map boundary and other characters block movement")
    void boundaryAndEvents() {
        MapData data = mapData(0);
        data.events.add(event(3, 1));       // a sign next to the player
        TilesetData open = tileset(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });
        TileMap map = new TileMap(data, open);
        MapCharacter character = new MapCharacter(2, 1, 5, 3, "boy_walk");

        assertFalse(canStep(map, data, character, RIGHT));   // the sign is there
        assertTrue(canStep(map, data, character, DOWN));      // blank tile, no event

        MapCharacter atLeft = new MapCharacter(0, 1, 5, 3, "boy_walk");
        assertFalse(canStep(map, data, atLeft, LEFT));       // map boundary
        MapCharacter atBottom = new MapCharacter(2, 2, 5, 3, "boy_walk");
        assertFalse(canStep(map, data, atBottom, DOWN));     // map boundary
        assertTrue(canStep(map, data, atBottom, RIGHT));
        character.through = true;
        assertTrue(canStep(map, data, character, RIGHT));     // through ignores all
    }

    @Test
    @DisplayName("holding a direction moves continuously and release completes the current step")
    void holdingRepeatsAndTurns() {
        MapData data = mapData(0);
        TilesetData open = tileset(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });
        TileMap map = new TileMap(data, open);
        MapCharacter character = new MapCharacter(2, 1, 5, 3, "boy_walk");

        InputManager input = new InputManager();
        StubKeys keys = new StubKeys();
        InputSampler sampler = new InputSampler(new DefaultKeyBindings());
        MovementController controller = new MovementController();

        // Press DOWN for one frame: one step, still moving.
        keys.down.add(com.badlogic.gdx.Input.Keys.DOWN);
        input.beginFrame();
        sampler.sample(keys, input);
        controller.update(input, 0.016f, character, mover(map, data, character));
        input.endFrame();
        assertEquals(1, character.y());
        assertEquals(DOWN, character.direction());

        // Keep DOWN held until the first step has finished.
        input.beginFrame();
        sampler.sample(keys, input);
        controller.update(input, 0.25f, character, mover(map, data, character));
        input.endFrame();
        assertEquals(2, character.y()); // Bottom boundary stops the next step.

        keys.down.clear();
        keys.down.add(com.badlogic.gdx.Input.Keys.RIGHT);
        input.beginFrame();
        sampler.sample(keys, input);
        controller.update(input, 0.125f, character, mover(map, data, character));
        input.endFrame();
        assertTrue(character.isMoving());
        keys.down.clear();
        input.beginFrame();
        sampler.sample(keys, input);
        controller.update(input, 0.5f, character, mover(map, data, character));
        input.endFrame();
        assertEquals(3, character.x());
        assertFalse(character.isMoving());
    }

    @Test
    @DisplayName("a blocked direction turns the character without moving")
    void blockedTurns() {
        MapData data = mapData(1);
        TilesetData blocked = tileset(new int[] { 0, 15, 15, 15, 15, 15, 15, 15 });
        TileMap map = new TileMap(data, blocked);
        MapCharacter character = new MapCharacter(2, 1, 5, 3, "boy_walk");

        InputManager input = new InputManager();
        StubKeys keys = new StubKeys();
        InputSampler sampler = new InputSampler(new DefaultKeyBindings());
        MovementController controller = new MovementController();

        keys.down.add(com.badlogic.gdx.Input.Keys.LEFT);
        input.beginFrame();
        sampler.sample(keys, input);
        controller.update(input, 0.016f, character, mover(map, data, character));
        input.endFrame();
        character.update(0.25f);

        assertEquals(LEFT, character.direction());
        assertEquals(2, character.x());
        assertEquals(1, character.y());
    }

    // ------------------------------------------------------------------

    private static MovementController.StepMover mover(TileMap map, MapData data, MapCharacter character) {
        return (who, direction) -> {
            if (canStep(map, data, character, direction)) {
                who.startMove(Collision.targetX(who.x(), direction),
                        Collision.targetY(who.y(), direction), direction);
                return true;
            }
            who.face(direction);
            return false;
        };
    }

    /** Test key state: a set of held libGDX key codes. */
    private static final class StubKeys implements KeyStateSource {
        private final java.util.Set<Integer> down = new java.util.HashSet<>();

        @Override
        public boolean isDown(int keyCode) {
            return down.contains(keyCode);
        }
    }
}
