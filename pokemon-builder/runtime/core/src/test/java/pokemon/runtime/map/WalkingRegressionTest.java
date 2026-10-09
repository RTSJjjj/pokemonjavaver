package pokemon.runtime.map;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.TilesetData;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.state.GameState;
import static org.junit.jupiter.api.Assertions.*;

/** User-run regressions for the integrated R4 fixes; no GL context required. */
class WalkingRegressionTest {
    private static final GameState STATE = new GameState();

    private static boolean canStep(TileMap map, MapData data, MapCharacter character, int direction) {
        return Collision.canStep(STATE, map, data, character, direction);
    }

    private static boolean canStep(GameState state, TileMap map, MapData data,
                                   MapCharacter character, int direction) {
        return Collision.canStep(state, map, data, character, direction);
    }

    private MapData data() {
        MapData d = new MapData(); d.width = 20; d.height = 4;
        d.tileData = new MapData.TileData();
        d.tileData.layers = new int[3][80];
        java.util.Arrays.fill(d.tileData.layers[0], 384);
        return d;
    }
    private TilesetData tiles() {
        TilesetData t = new TilesetData();
        t.passages = table(); t.priorities = table(); t.terrainTags = table();
        return t;
    }
    private TilesetData.TableData table() {
        TilesetData.TableData t = new TilesetData.TableData(); t.layers = new int[][] {new int[400]}; return t;
    }
    private MapCharacter actor() { return new MapCharacter(2, 1, 20, 4, "trchar000"); }
    private MovementController.StepMover mover(TileMap map, MapData data) {
        return (who, direction) -> canStep(map, data, who, direction)
                && who.startMove(Collision.targetX(who.x(), direction), Collision.targetY(who.y(), direction), direction);
    }
    private MapData.EventData event(int x, int y) {
        MapData.EventData e = new MapData.EventData(); e.x = x; e.y = y;
        MapData.EventPageData p = new MapData.EventPageData();
        p.graphic = new MapData.EventGraphic(); p.graphic.characterName = "NPC";
        e.pages.add(p); return e;
    }
    @Test void sourceExitAndDestinationEntryUseOppositeBits() {
        MapData d = data(); TilesetData t = tiles(); TileMap map = new TileMap(d, t);
        d.tileData.layers[0][22] = 385; d.tileData.layers[0][23] = 386;
        MapCharacter c = actor();
        t.passages.layers[0][385] = 4; // Source blocks right.
        assertFalse(canStep(map, d, c, 6));
        t.passages.layers[0][385] = 0;
        t.passages.layers[0][386] = 2; // Destination blocks left-side entry.
        assertFalse(canStep(map, d, c, 6));
        t.passages.layers[0][386] = 1; // Destination blocks down only.
        assertTrue(canStep(map, d, c, 6));
        assertFalse(canStep(map, d, c, 0));
    }
    @Test void upperGroundOverridesLowerWallButPriorityTileDoesNot() {
        MapData d = data(); TilesetData t = tiles(); TileMap map = new TileMap(d, t);
        d.tileData.layers[0][23] = 385; t.passages.layers[0][385] = 15;
        d.tileData.layers[2][23] = 386;
        assertTrue(canStep(map, d, actor(), 6));
        t.priorities.layers[0][386] = 1;
        assertFalse(canStep(map, d, actor(), 6));
        t.priorities.layers[0][386] = 0;
        t.terrainTags.layers[0][386] = 13; // Neutral art cannot override the wall.
        assertFalse(canStep(map, d, actor(), 6));
    }
    @Test void rendererAndCollisionUseTheSameActivePageAndThroughFlag() {
        MapData d = data(); TileMap map = new TileMap(d, tiles());
        MapData.EventData e = event(3, 1); d.events.add(e);
        MapData.EventPageData later = new MapData.EventPageData();
        later.conditions = new MapData.EventConditions();
        later.conditions.switch1Valid = true; later.conditions.switch1Id = 7;
        e.pages.add(later);
        GameState state = new GameState();
        assertSame(e.pages.first(), EventPages.resolve(state, d.mapId, e));
        assertFalse(canStep(state, map, d, actor(), 6));
        e.pages.first().movement.through = true;
        assertTrue(canStep(state, map, d, actor(), 6));
        e.pages.first().movement.through = false;
        state.switches().set(7, true); // The later blank page hides AND unblocks.
        assertSame(later, EventPages.resolve(state, d.mapId, e));
        assertNull(later.graphic);
        assertTrue(canStep(state, map, d, actor(), 6));
    }
    @Test void tileEventUsesDirectionalPassageRatherThanUnconditionalBlocking() {
        MapData d = data(); TilesetData t = tiles(); TileMap map = new TileMap(d, t);
        MapData.EventData e = event(3, 1); d.events.add(e);
        e.pages.first().graphic.characterName = null; e.pages.first().graphic.tileId = 385;
        t.passages.layers[0][385] = 1;
        assertTrue(canStep(map, d, actor(), 6));
        t.passages.layers[0][385] = 2;
        assertFalse(canStep(map, d, actor(), 6));
    }
    @Test void heldMovementHasNoPerTileDelayAndRunningDoublesDistance() {
        MapData d = data(); TileMap map = new TileMap(d, tiles());
        MapCharacter walk = actor(), run = actor();
        InputManager input = new InputManager(); input.press(GameAction.RIGHT);
        MovementController walker = new MovementController();
        walker.update(input, 0.001f, walk, mover(map, d));          // the frame the key goes down only turns (025_Game_Player:363)
        walker.update(input, 1f, walk, mover(map, d));
        input.press(GameAction.RUN);
        MovementController runner = new MovementController();
        runner.update(input, 0.001f, run, mover(map, d));
        runner.update(input, 1f, run, mover(map, d));
        assertEquals(6, walk.x()); assertEquals(10, run.x());
    }
    @Test void framePartitionsGiveTheSameDistanceAndReleaseFinishesOnlyCurrentStep() {
        MapData d = data(); TileMap map = new TileMap(d, tiles());
        MapCharacter c = actor(); MovementController controller = new MovementController();
        InputManager input = new InputManager(); input.press(GameAction.RIGHT);
        controller.update(input, 0.001f, c, mover(map, d));          // the turn frame
        for (int i = 0; i < 8; i++) controller.update(input, 0.125f, c, mover(map, d));
        assertEquals(6, c.x());
        controller.update(input, 0.125f, c, mover(map, d));
        input.release(GameAction.RIGHT);
        controller.update(input, 1f, c, mover(map, d));
        assertEquals(7, c.x()); assertFalse(c.isMoving());
    }
    /** 025_Game_Player:350-375: a short tap only turns; the step comes once the key has been held a few frames. */
    @Test void aTapTurnsAndOnlyAHoldWalks() {
        MapData d = data(); TileMap map = new TileMap(d, tiles());
        MapCharacter c = actor(); c.face(2);
        MovementController controller = new MovementController();
        InputManager input = new InputManager(); input.press(GameAction.RIGHT);
        controller.update(input, 1f / 40f, c, mover(map, d));
        assertEquals(6, c.direction(), "the first frame turns");
        assertEquals(2, c.x());
        controller.update(input, 1f / 40f, c, mover(map, d));
        input.release(GameAction.RIGHT);
        controller.update(input, 1f / 40f, c, mover(map, d));
        assertEquals(2, c.x(), "a tap of two frames never walks");
        assertFalse(c.isMoving());
        input.press(GameAction.RIGHT);
        for (int i = 0; i < 5; i++) controller.update(input, 1f / 40f, c, mover(map, d));
        assertTrue(c.isMoving() || c.x() > 2, "held long enough: it walks");
    }
    @Test void blockedDirectionTurnsWithoutMovingAndInFlightStepCannotBeOverwritten() {
        MapData d = data(); TilesetData t = tiles(); TileMap map = new TileMap(d, t);
        t.passages.layers[0][384] = 15;
        MapCharacter c = actor(); InputManager input = new InputManager(); input.press(GameAction.LEFT);
        new MovementController().update(input, 0.1f, c, mover(map, d));
        assertEquals(4, c.direction()); assertEquals(2, c.x());
        assertTrue(c.startMove(3, 1, 6));
        assertFalse(c.startMove(2, 2, 2));
        c.update(0.25f); assertEquals(3, c.x()); assertEquals(1, c.y());
    }
    @Test void debugSpawnClampsAndAvoidsBlockedOrEmptyCells() {
        MapData d = data(); TilesetData t = tiles();
        java.util.Arrays.fill(d.tileData.layers[0], 385); t.passages.layers[0][385] = 15;
        d.tileData.layers[0][22] = 384; d.tileData.layers[0][23] = 384;
        int[] spawn = Collision.debugSpawn(STATE, new TileMap(d, t), d, 99, -2);
        assertArrayEquals(new int[] {3, 1}, spawn);
    }
    @Test void cameraFollowsHalfStepsAndRunGraphicStopsWithMovement() {
        MapCharacter c = actor(); c.runningCharacterName = "boy_run"; c.running(true);
        c.startMove(3, 1, 6); c.update(0.0625f);
        MapCamera camera = new MapCamera(20, 4, 64, 64);
        camera.centerOnPixels(c.pixelX(), c.pixelY());
        assertEquals(64, camera.originX()); assertEquals("boy_run", c.graphicName());
        c.update(0.0625f); assertEquals("trchar000", c.graphicName());
    }
}
