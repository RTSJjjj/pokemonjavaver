package pokemon.runtime.map;

import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;
import static org.junit.jupiter.api.Assertions.*;

class MapRenderingContractTest {
    @Test void rectangularTableUsesWidthStrideAndChecksBothAxes() {
        MapData.TileData data = new MapData.TileData();
        data.x = 3; data.y = 2;
        data.layers = new int[][] { {384, 385, 386, 387, 388, 389} };
        assertEquals(387, data.tile(0, 0, 1));
        assertEquals(389, data.tile(0, 2, 1));
        assertEquals(0, data.tile(0, 3, 0));
        assertEquals(0, data.tile(0, -1, 1));
        assertEquals(0, data.tile(0, 0, 2));
    }
    @Test void freshGameDoesNotShowConditionalLaterPages() {
        MapData.EventData event = new MapData.EventData();
        MapData.EventPageData first = new MapData.EventPageData();
        first.graphic = new MapData.EventGraphic();
        event.pages.add(first);
        MapData.EventPageData later = new MapData.EventPageData();
        later.conditions = new MapData.EventConditions();
        later.conditions.switch1Valid = true;
        later.conditions.switch1Id = 1;
        later.graphic = new MapData.EventGraphic();
        event.pages.add(later);
        GameState state = new GameState();
        assertSame(first, EventPages.resolve(state, 1, event));
        state.switches().set(1, true);
        assertSame(later, EventPages.resolve(state, 1, event));
    }
    @Test void priorityRatherThanEditorLayerControlsOcclusion() {
        assertEquals(0, MapRenderer.tileDepth(20, 0));
        assertTrue(MapRenderer.tileDepth(4, 2) > MapRenderer.eventDepth(5));
        assertTrue(MapRenderer.tileDepth(4, 2) < MapRenderer.eventDepth(6));
    }
}
