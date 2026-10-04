package pokemon.runtime.map;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.23: Essentials map connections. The tests mirror
 * {@code PokemonMapFactory#getNewMap} and the project's
 * {@code MapFactoryHelper#getMapConnections} border points.
 */
class MapLinksTest {

    private static final int TILE = TilesetGeometry.TILE_SIZE;

    /** 195 is 20x30, 189 is 25x30, connected west edge to east edge. */
    private static MapLinks westLink() {
        return MapLinks.parse(new JsonReader().parse(
                "{\"dims\":{\"195\":[20,30],\"189\":[25,30]},"
                        + "\"connections\":[{\"a\":195,\"ax\":0,\"ay\":0,\"b\":189,\"bx\":25,\"by\":0}]}"));
    }

    @Test
    @DisplayName("a step off the west edge lands on the neighbour's east column")
    void crossingWest() {
        MapLinks links = westLink();
        assertTrue(links.has(195));
        MapLinks.Crossing crossing = links.crossing(195, -1, 5);
        assertNotNull(crossing);
        assertEquals(189, crossing.mapId);
        assertEquals(24, crossing.x);
        assertEquals(5, crossing.y);
        // Off the edge but outside every border point: no crossing.
        assertNull(links.crossing(195, -1, -5));
        // The same link walked the other way: off 189's east column lands on
        // 195's first column.
        MapLinks.Crossing back = links.crossing(189, 25, 5);
        assertNotNull(back);
        assertEquals(195, back.mapId);
        assertEquals(0, back.x);
        assertEquals(5, back.y);
    }

    @Test
    @DisplayName("the neighbour offset keeps the border tiles pixel-aligned")
    void neighbourOffsetIsContinuous() {
        MapLinks links = westLink();
        MapLinks.Link link = links.linksOf(195).get(0);
        MapLinks.Crossing crossing = links.crossing(195, -1, 5);
        MapLinks.Neighbour neighbour = links.neighbour(link, 195);
        assertEquals(189, neighbour.mapId);
        assertEquals(-25 * TILE, neighbour.offsetX);
        assertEquals(0, neighbour.offsetY);
        // -1 tile on 195 and 24 tiles on 189 must be the same screen pixel.
        assertEquals(-TILE, crossing.x * TILE + neighbour.offsetX);

        // The same link from the other side draws 195 on the east.
        MapLinks.Neighbour back = links.neighbour(link, 189);
        assertEquals(195, back.mapId);
        assertEquals(25 * TILE, back.offsetX);
    }

    @Test
    @DisplayName("a north/south link offsets the neighbour by a map height")
    void crossingSouth() {
        MapLinks links = MapLinks.parse(new JsonReader().parse(
                "{\"dims\":{\"191\":[30,40],\"190\":[30,20]},"
                        + "\"connections\":[{\"a\":191,\"ax\":0,\"ay\":0,\"b\":190,\"bx\":20,\"by\":20}]}"));
        MapLinks.Crossing crossing = links.crossing(191, 4, -1);
        assertNotNull(crossing);
        assertEquals(190, crossing.mapId);
        assertEquals(24, crossing.x);
        assertEquals(19, crossing.y);
        MapLinks.Neighbour neighbour = links.neighbour(links.linksOf(191).get(0), 191);
        // B is north of A: its rows are one A-height further up.
        assertEquals(40 * TILE, neighbour.offsetY);

        // Continuity on the row: A row -1 and B row 19 share the pixel row.
        int aUp = (40 - (-1) - 1) * TILE;
        int bUp = (20 - crossing.y - 1) * TILE;
        assertEquals(aUp, bUp + neighbour.offsetY);
    }

    @Test
    @DisplayName("only maps that take part in a connection are linked")
    void unrelatedMaps() {
        MapLinks links = westLink();
        assertFalse(links.has(77));
        assertNull(links.crossing(77, -1, 0));
        assertEquals(1, links.size());
        assertEquals(2, links.dims(195).length);
    }
}
