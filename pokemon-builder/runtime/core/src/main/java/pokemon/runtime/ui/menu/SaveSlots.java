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

    /** Fixed list of the save screen: MANUAL_SLOTS (SaveData::Game1..Game8). */
    public static final Array<String> SLOT_IDS =
            Array.with("1", "2", "3", "4", "5", "6", "7", "8");

    public static final class Slot {
        public final String id;
        public final String label;
        public final boolean exists;
        public final String playerName;
        public final int mapId;
        public final String mapName;
        public final long savedAtMillis;
        public final int badges;
        public final int seen;
        public final int playSeconds;
        /** Trainer gender (0 = male, 1 = female); -1 when unknown. */
        public final int gender;
        /** Trainer id for the start menu's walking charset (pbSetParty); -1 unknown. */
        public final int playerId;
        /** Party members, for the start menu's icon row. */
        public final Array<PartyEntry> party;

        /** One party member as the load screen's PokemonIconSprite needs it. */
        public static final class PartyEntry {
            public final String species;
            public final int gender;
            public final boolean shiny;
            public final boolean egg;

            PartyEntry(String species, int gender, boolean shiny, boolean egg) {
                this.species = species;
                this.gender = gender;
                this.shiny = shiny;
                this.egg = egg;
            }
        }

        Slot(String id, String label, boolean exists, String playerName, int mapId,
             String mapName, long savedAtMillis, int badges, int seen, int playSeconds, int gender,
             int playerId, Array<PartyEntry> party) {
            this.id = id;
            this.label = label;
            this.exists = exists;
            this.playerName = playerName;
            this.mapId = mapId;
            this.mapName = mapName;
            this.savedAtMillis = savedAtMillis;
            this.badges = badges;
            this.seen = seen;
            this.playSeconds = playSeconds;
            this.gender = gender;
            this.playerId = playerId;
            this.party = party == null ? new Array<PartyEntry>() : party;
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
        String label = "存档" + id;
        if (storage == null) {
            return new Slot(id, label, false, "", -1, "", 0L, 0, 0, 0, -1, -1, new Array<Slot.PartyEntry>());
        }
        File file = new File(storage.resolve("saves/" + id + ".json"));
        if (!file.isFile()) {
            return new Slot(id, label, false, "", -1, "", 0L, 0, 0, 0, -1, -1, new Array<Slot.PartyEntry>());
        }
        String playerName = "";
        int mapId = -1;
        int badges = 0;
        int seen = 0;
        int playSeconds = 0;
        int gender = -1;
        int playerId = -1;
        Array<Slot.PartyEntry> party = new Array<>();
        try {
            String json = new String(storage.readUtf8("saves/" + id + ".json"), StandardCharsets.UTF_8);
            JsonValue root = new JsonReader().parse(json);
            playerName = root.getString("playerName", "");
            playerId = root.getInt("playerId", -1);
            JsonValue map = root.get("map");
            if (map != null) {
                mapId = map.getInt("id", -1);
            }
            JsonValue trainer = root.get("trainer");
            if (trainer != null && trainer.isObject()) {
                JsonValue badgeList = trainer.get("badges");
                JsonValue seenList = trainer.get("seen");
                badges = badgeList != null && badgeList.isArray() ? badgeList.size : 0;
                seen = seenList != null && seenList.isArray() ? seenList.size : 0;
                // playSeconds is written as a number; accept either int or double.
                playSeconds = (int) trainer.getDouble("playSeconds", 0);
                if (trainer.has("gender")) {
                    gender = trainer.getInt("gender", -1);
                }
                JsonValue partyList = trainer.get("party");
                if (partyList != null && partyList.isArray()) {
                    for (JsonValue member : partyList) {
                        party.add(new Slot.PartyEntry(
                                member.getString("species", ""),
                                member.getInt("gender", -1),
                                member.getBoolean("shiny", false),
                                member.getBoolean("egg", false)));
                    }
                }
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
        return new Slot(id, label, true, playerName, mapId, mapName, file.lastModified(),
                badges, seen, playSeconds, gender, playerId, party);
    }
}
