package pokemon.runtime.map;

import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * R6.23: Essentials map connections, generated from {@code PBS/connections.txt}
 * by the Builder (see the project's own {@code PokemonMapFactory} /
 * {@code MapFactoryHelper}).
 *
 * <p>Every link stores the two border points in the maps' own tile
 * coordinates, exactly like {@code MapFactoryHelper.getMapConnections}
 * normalizes them: for {@code 195,West,0,189,East,0} the points are
 * {@code (0,0)} on map 195 and {@code (width,0)} on map 189. The runtime
 * derives both the seamless crossing (the player may step off the edge and
 * lands on the neighbour) and the drawing offset of the neighbouring map
 * (its tiles stay visible beyond the border) from those points.</p>
 */
public final class MapLinks {

    private static final int TILE = TilesetGeometry.TILE_SIZE;

    /** One normalized connection ({@code a} and {@code b} are both map ids). */
    public static final class Link {
        public final int a;
        public final int ax;
        public final int ay;
        public final int b;
        public final int bx;
        public final int by;

        Link(int a, int ax, int ay, int b, int bx, int by) {
            this.a = a;
            this.ax = ax;
            this.ay = ay;
            this.b = b;
            this.bx = bx;
            this.by = by;
        }

        public boolean touches(int mapId) {
            return a == mapId || b == mapId;
        }
    }

    /** Where a step off the current map's edge lands. */
    public static final class Crossing {
        public final int mapId;
        public final int x;
        public final int y;

        Crossing(int mapId, int x, int y) {
            this.mapId = mapId;
            this.x = x;
            this.y = y;
        }
    }

    /** A neighbour map and its offset in the current map's y-up pixel space. */
    public static final class Neighbour {
        public final int mapId;
        public final int offsetX;
        public final int offsetY;

        Neighbour(int mapId, int offsetX, int offsetY) {
            this.mapId = mapId;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }

    private final List<Link> links = new ArrayList<>();
    private final Map<Integer, List<Link>> byMap = new HashMap<>();
    private final Map<Integer, int[]> dims = new HashMap<>();

    private MapLinks() {
    }

    /** Parses {@code generated/connections.json}; never returns {@code null}. */
    public static MapLinks parse(JsonValue root) {
        MapLinks result = new MapLinks();
        if (root == null) {
            return result;
        }
        JsonValue dims = root.get("dims");
        if (dims != null) {
            for (JsonValue entry = dims.child; entry != null; entry = entry.next) {
                // In an object, each iteration node is the value: entry itself
                // is the [width, height] array and entry.name is the map id.
                if (!entry.isArray()) {
                    continue;
                }
                JsonValue width = entry.child;
                JsonValue height = width == null ? null : width.next;
                if (width == null || height == null) {
                    continue;
                }
                try {
                    result.dims.put(Integer.parseInt(entry.name),
                            new int[] {width.asInt(), height.asInt()});
                } catch (NumberFormatException ignored) {
                    // a non-numeric key cannot be a map id
                }
            }
        }
        JsonValue entries = root.get("connections");
        if (entries != null && entries.isArray()) {
            for (JsonValue entry = entries.child; entry != null; entry = entry.next) {
                Link link = new Link(entry.getInt("a", -1), entry.getInt("ax", 0),
                        entry.getInt("ay", 0), entry.getInt("b", -1),
                        entry.getInt("bx", 0), entry.getInt("by", 0));
                // Without both map sizes the link cannot be resolved; the
                // Builder already reports unknown maps, so skip it here.
                if (!result.dims.containsKey(link.a) || !result.dims.containsKey(link.b)) {
                    continue;
                }
                result.links.add(link);
                result.byMap.computeIfAbsent(link.a, key -> new ArrayList<>()).add(link);
                result.byMap.computeIfAbsent(link.b, key -> new ArrayList<>()).add(link);
            }
        }
        return result;
    }

    public boolean isEmpty() {
        return links.isEmpty();
    }

    public int size() {
        return links.size();
    }

    public List<Link> all() {
        return links;
    }

    /** True when {@code mapId} takes part in at least one connection. */
    public boolean has(int mapId) {
        return byMap.containsKey(mapId);
    }

    /** The links touching one map; empty when it has no connections. */
    public List<Link> linksOf(int mapId) {
        List<Link> list = byMap.get(mapId);
        return list == null ? java.util.Collections.<Link>emptyList() : list;
    }

    public int[] dims(int mapId) {
        return dims.get(mapId);
    }

    /**
     * {@code PokemonMapFactory#getNewMap}: a tile that is outside the current
     * map is translated into the connected neighbour
     * ({@code newx = x + bx - ax}, {@code newy = y + by - ay}) and only counts
     * when it lands inside that map.
     */
    public Crossing crossing(int fromMap, int x, int y) {
        for (Link link : linksOf(fromMap)) {
            int mapId;
            int targetX;
            int targetY;
            if (link.a == fromMap) {
                mapId = link.b;
                targetX = x + link.bx - link.ax;
                targetY = y + link.by - link.ay;
            } else {
                mapId = link.a;
                targetX = x + link.ax - link.bx;
                targetY = y + link.ay - link.by;
            }
            int[] size = dims(mapId);
            if (size != null && targetX >= 0 && targetY >= 0
                    && targetX < size[0] && targetY < size[1]) {
                return new Crossing(mapId, targetX, targetY);
            }
        }
        return null;
    }

    /**
     * Where a link's other map has to be drawn in the current map's y-up pixel
     * space. Anchored to the border points, so the neighbour's tiles and the
     * crossing stay exactly aligned: for a border tile,
     * {@code local * TILE + offset} is the same screen position on both maps.
     */
    public Neighbour neighbour(Link link, int currentMapId) {
        int[] a = dims(link.a);
        int[] b = dims(link.b);
        if (link.a == currentMapId) {
            return new Neighbour(link.b,
                    (link.ax - link.bx) * TILE,
                    (a[1] - b[1] + link.by - link.ay) * TILE);
        }
        return new Neighbour(link.a,
                (link.bx - link.ax) * TILE,
                (b[1] - a[1] + link.ay - link.by) * TILE);
    }
}
