package pokemon.runtime.ui.menu;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import pokemon.runtime.app.StoragePort;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * L1: the save/load screens' slot list. Slots live where SaveManager writes
 * them ({@code <user>/.pokemon-runtime/saves/<id>.json}); the list shows the
 * trainer name, the map name and the file time, so an empty slot is obvious.
 */
public final class SaveSlots {

    /** Fixed list of the save screen; "quick" is the F5/F9 slot. */
    public static final Array<String> SLOT_IDS = Array.with("1", "2", "3", "quick");

    public static final class Slot {
        public final String id;
        public final String label;
        public final boolean exists;
        public final String playerName;
        public final int mapId;
        public final String mapName;
        public final long savedAtMillis;

        Slot(String id, String label, boolean exists, String playerName, int mapId,
             String mapName, long savedAtMillis) {
            this.id = id;
            this.label = label;
            this.exists = exists;
            this.playerName = playerName;
            this.mapId = mapId;
            this.mapName = mapName;
            this.savedAtMillis = savedAtMillis;
        }

        /** One line for the slot row ("空档" when nothing was saved yet). */
        public String describe() {
            if (!exists) {
                return "空档";
            }
            String name = playerName == null || playerName.isEmpty() ? "训练家" : playerName;
            String map = mapName == null || mapName.isEmpty() ? "地图 " + mapId : mapName;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return name + " · " + map + " · " + format.format(new Date(savedAtMillis));
        }
    }

    private SaveSlots() {
    }

    public static Array<Slot> list(StoragePort storage, GameDatabase database) {
        Array<Slot> slots = new Array<>();
        for (String id : SLOT_IDS) {
            slots.add(read(storage, database, id));
        }
        return slots;
    }

    public static Slot read(StoragePort storage, GameDatabase database, String id) {
        String label = "quick".equals(id) ? "快速存档" : "存档 " + id;
        if (storage == null) {
            return new Slot(id, label, false, "", -1, "", 0L);
        }
        File file = new File(storage.resolve("saves/" + id + ".json"));
        if (!file.isFile()) {
            return new Slot(id, label, false, "", -1, "", 0L);
        }
        String playerName = "";
        int mapId = -1;
        try {
            String json = new String(storage.readUtf8("saves/" + id + ".json"), StandardCharsets.UTF_8);
            JsonValue root = new JsonReader().parse(json);
            playerName = root.getString("playerName", "");
            JsonValue map = root.get("map");
            if (map != null) {
                mapId = map.getInt("id", -1);
            }
        } catch (RuntimeException error) {
            // A broken slot still exists; the load action will report it.
        }
        String mapName = "";
        if (database != null && mapId >= 0) {
            try {
                MapData data = database.map(mapId);
                if (data != null && data.name != null) {
                    mapName = data.name;
                }
            } catch (RuntimeException error) {
                mapName = "";
            }
        }
        return new Slot(id, label, true, playerName, mapId, mapName, file.lastModified());
    }
}
