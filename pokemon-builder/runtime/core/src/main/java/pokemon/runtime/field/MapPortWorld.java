package pokemon.runtime.field;

import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.MapInfo;
import pokemon.runtime.event.MapPort;
import pokemon.runtime.state.GameState;

/** {@link HiddenMoves.World} answered by the map screen's {@link MapPort} and the database. */
public final class MapPortWorld implements HiddenMoves.World {
    private final MapPort port;
    private final GameState state;
    private final GameDatabase database;

    public MapPortWorld(MapPort port, GameState state, GameDatabase database) {
        this.port = port;
        this.state = state;
        this.database = database;
    }

    @Override
    public String facingEventName() {
        return port == null ? null : port.facingEventName();
    }

    @Override
    public int facingTerrainTag() {
        return port == null ? 0 : port.facingTerrainTag();
    }

    @Override
    public int playerTerrainTag() {
        return port == null ? 0 : port.playerTerrainTag();
    }

    @Override
    public boolean facingPassable() {
        return port != null && port.facingPassable();
    }

    @Override
    public boolean hasDependentEvents() {
        return port != null && port.hasDependentEvents();
    }

    @Override
    public int mapId() {
        return state.currentMapId();
    }

    @Override
    public boolean outdoor() {
        MapData map = database == null ? null : database.map(state.currentMapId());
        return map != null && Boolean.TRUE.equals(map.outdoor);
    }

    @Override
    public int terrainTagOn(int map) {
        return port == null ? 0 : port.terrainTagOnMap(map);
    }

    @Override
    public String mapName(int id) {
        if (database != null) {
            for (MapInfo info : database.maps()) {
                if (info.mapId == id) {
                    return info.name == null ? "" : info.name;
                }
            }
        }
        return "";
    }
}
