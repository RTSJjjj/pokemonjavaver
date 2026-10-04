package pokemon.runtime.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/**
 * One generated/maps/map-NNN.json document. Parsed manually from the JSON tree
 * because the event command parameters are heterogeneous; everything else
 * (tile tables, audio, encounters) has a fixed shape.
 */
public final class MapData {

    public String name;
    public int mapId;
    public int width;
    public int height;
    public int tilesetId;
    public int encounterStep;
    public boolean autoplayBgm;
    public boolean autoplayBgs;
    /**
     * R6.16: PBS/metadata.txt "Outdoor" flag. {@code null} = the generated data
     * predates the field (the runtime keeps its old behaviour and asks for a
     * data rebuild); {@code false} = indoor (no day/night tint, matching the
     * project's pbDayNightTint which only shades MetadataOutdoor maps).
     */
    public Boolean outdoor;
    /**
     * R6.23: PBS/metadata.txt "SnapEdges" flag. {@code null} = the generated
     * data predates the field (the runtime keeps clamping the camera like
     * before); {@code false} = the camera follows the player past the map edge,
     * which is what the project's {@code Game_Map#display_x=} does and what
     * makes the connected maps scroll seamlessly.
     */
    public Boolean snapEdges;
    public AudioRef bgm;
    public AudioRef bgs;
    public Array<Encounter> encounters = new Array<>();
    public TileData tileData;
    public Array<EventData> events = new Array<>();
    public Array<String> scriptBlockIds = new Array<>();

    public static MapData parse(JsonValue root) {
        MapData data = new MapData();
        data.mapId = root.getInt("mapId", -1);
        data.name = root.getString("name", null);
        data.width = root.getInt("width", 0);
        data.height = root.getInt("height", 0);
        data.tilesetId = root.getInt("tilesetId", -1);
        data.encounterStep = root.getInt("encounterStep", 0);
        data.autoplayBgm = root.getBoolean("autoplayBgm", false);
        data.autoplayBgs = root.getBoolean("autoplayBgs", false);
        JsonValue outdoor = root.get("outdoor");
        data.outdoor = outdoor == null ? null : outdoor.asBoolean();
        JsonValue snapEdges = root.get("snapEdges");
        data.snapEdges = snapEdges == null ? null : snapEdges.asBoolean();
        data.bgm = AudioRef.parse(root.get("bgm"));
        data.bgs = AudioRef.parse(root.get("bgs"));

        JsonValue encounters = root.get("encounters");
        if (encounters != null && encounters.isArray()) {
            for (JsonValue entry = encounters.child; entry != null; entry = entry.next) {
                data.encounters.add(Encounter.parse(entry));
            }
        }
        data.tileData = TileData.parse(root.get("tileData"));

        JsonValue events = root.get("events");
        if (events != null && events.isArray()) {
            for (JsonValue entry = events.child; entry != null; entry = entry.next) {
                data.events.add(EventData.parse(entry));
            }
        }
        JsonValue scriptBlockIds = root.get("scriptBlockIds");
        if (scriptBlockIds != null && scriptBlockIds.isArray()) {
            for (JsonValue id = scriptBlockIds.child; id != null; id = id.next) {
                data.scriptBlockIds.add(id.asString());
            }
        }
        return data;
    }

    // ------------------------------------------------------------------
    // Nested value types
    // ------------------------------------------------------------------

    /** RPG::AudioFile as emitted by the converter. */
    public static final class AudioRef {
        public String name;
        public int volume;
        public int pitch;

        public boolean isEmpty() {
            return name == null || name.isEmpty();
        }

        public static AudioRef parse(JsonValue node) {
            if (node == null || !node.isObject()) {
                return null;
            }
            AudioRef ref = new AudioRef();
            ref.name = node.getString("name", "");
            ref.volume = node.getInt("volume", 100);
            ref.pitch = node.getInt("pitch", 100);
            return ref;
        }
    }

    /** One encounter step row of a map. */
    public static final class Encounter {
        public int troopId;
        public int weight;
        public Array<Integer> regions = new Array<>();

        public static Encounter parse(JsonValue node) {
            Encounter encounter = new Encounter();
            encounter.troopId = node.getInt("troopId", 0);
            encounter.weight = node.getInt("weight", 0);
            JsonValue regions = node.get("regions");
            if (regions != null && regions.isArray()) {
                for (JsonValue region = regions.child; region != null; region = region.next) {
                    encounter.regions.add(region.asInt());
                }
            }
            return encounter;
        }
    }

    /**
     * The RGSS map tile table: dim0 / dim1 / dim2 int headers plus one flat
     * array per layer (layer 0 is the ground layer). A corrupt or absent table
     * keeps present=false so the renderer can say so instead of guessing.
     */
    public static final class TileData {
        public boolean present;
        public String error;
        public int z;
        public int x;
        public int y;
        public int total;
        public int[][] layers;

        public int tile(int layer, int column, int row) {
            if (layers == null || layer < 0 || layer >= layers.length) {
                return 0;
            }
            int[] values = layers[layer];
            if (column < 0 || row < 0 || column >= x || row >= y) return 0;
            int index = row * x + column;
            if (index < 0 || index >= values.length) {
                return 0;
            }
            return values[index];
        }

        public static TileData parse(JsonValue node) {
            TileData tileData = new TileData();
            if (node == null || !node.getBoolean("present", false)) {
                return tileData;
            }
            tileData.present = true;
            String error = node.getString("error", null);
            if (error != null) {
                tileData.error = error;
                return tileData;
            }
            tileData.z = node.getInt("z", 0);
            tileData.x = node.getInt("x", 0);
            tileData.y = node.getInt("y", 0);
            tileData.total = node.getInt("total", 0);
            JsonValue layers = node.get("layers");
            if (layers == null || !layers.isArray()) {
                tileData.error = "tile data has no layers array";
                return tileData;
            }
            tileData.layers = new int[layers.size][];
            int layer = 0;
            for (JsonValue values = layers.child; values != null; values = values.next, layer++) {
                int[] row = new int[values.isArray() ? values.size : 0];
                if (values.isArray()) {
                    int i = 0;
                    for (JsonValue value = values.child; value != null; value = value.next, i++) {
                        row[i] = value.asInt();
                    }
                }
                tileData.layers[layer] = row;
            }
            return tileData;
        }
    }

    /** One event of a map, with its pages and command lists. */
    public static final class EventData {
        public int id;
        public String name;
        public int x;
        public int y;
        public Array<EventPageData> pages = new Array<>();
        public Array<String> scriptBlockIds = new Array<>();

        public static EventData parse(JsonValue node) {
            EventData event = new EventData();
            event.id = node.getInt("id", -1);
            event.name = node.getString("name", "");
            event.x = node.getInt("x", 0);
            event.y = node.getInt("y", 0);
            JsonValue pages = node.get("pages");
            if (pages != null && pages.isArray()) {
                for (JsonValue page = pages.child; page != null; page = page.next) {
                    event.pages.add(EventPageData.parse(page));
                }
            }
            JsonValue scriptBlockIds = node.get("scriptBlockIds");
            if (scriptBlockIds != null && scriptBlockIds.isArray()) {
                for (JsonValue id = scriptBlockIds.child; id != null; id = id.next) {
                    event.scriptBlockIds.add(id.asString());
                }
            }
            return event;
        }
    }

    /** One event page: activation conditions, graphic and command list. */
    public static final class EventPageData {
        public int page;
        public int trigger;
        public EventConditions conditions;
        public EventGraphic graphic;
        public EventMovement movement = new EventMovement();
        public Array<EventCommand> commands = new Array<>();

        public static EventPageData parse(JsonValue node) {
            EventPageData pageData = new EventPageData();
            pageData.page = node.getInt("page", -1);
            pageData.trigger = node.getInt("trigger", 0);
            pageData.conditions = EventConditions.parse(node.get("conditions"));
            pageData.graphic = EventGraphic.parse(node.get("graphic"));
            pageData.movement = EventMovement.parse(node.get("movement"));
            JsonValue commands = node.get("commands");
            if (commands != null && commands.isArray()) {
                for (JsonValue command = commands.child; command != null; command = command.next) {
                    pageData.commands.add(EventCommand.parse(command));
                }
            }
            return pageData;
        }
    }

    /**
     * The movement half of an event page (RGSS {@code RPG::Event::Page}): the
     * autonomous move type with its speed / frequency / custom route plus the
     * five option checkboxes the editor shows (walk anime, step anime, direction
     * fix, through, always on top). Builder map IR carries them under
     * {@code movement}; the runtime used to keep them as raw JSON and ignored
     * them, so every event stood still facing one direction.
     */
    public static final class EventMovement {
        /** 0 fixed, 1 random, 2 toward the player, 3 custom route. */
        public int moveType;
        /** RGSS move speed 1..6 (3 = walking, 4 = running). */
        public int moveSpeed = 3;
        /** RGSS move frequency 1..6; the editor labels them Slowest..Fastest. */
        public int moveFrequency = 3;
        /** Only used when {@link #moveType} is 3. */
        public MoveRoute moveRoute = new MoveRoute();
        public boolean through;
        public boolean alwaysOnTop;
        public boolean walkAnime = true;
        public boolean stepAnime;
        public boolean directionFix;

        public static EventMovement parse(JsonValue node) {
            EventMovement movement = new EventMovement();
            if (node == null || !node.isObject()) {
                return movement;
            }
            movement.moveType = node.getInt("moveType", 0);
            movement.moveSpeed = node.getInt("moveSpeed", 3);
            movement.moveFrequency = node.getInt("moveFrequency", 3);
            movement.moveRoute = MoveRoute.parse(node.get("moveRoute"));
            movement.through = node.getBoolean("through", false);
            movement.alwaysOnTop = node.getBoolean("alwaysOnTop", false);
            movement.walkAnime = node.getBoolean("walkAnime", true);
            movement.stepAnime = node.getBoolean("stepAnime", false);
            movement.directionFix = node.getBoolean("directionFix", false);
            return movement;
        }
    }

    /** Page activation conditions (RMXP event page conditions). */
    public static final class EventConditions {
        public String selfSwitchCh;
        public boolean selfSwitchValid;
        public boolean switch1Valid;
        public int switch1Id;
        public boolean switch2Valid;
        public int switch2Id;
        public boolean variableValid;
        public int variableId;
        public int variableValue;

        public static EventConditions parse(JsonValue node) {
            EventConditions conditions = new EventConditions();
            if (node == null || !node.isObject()) {
                return conditions;
            }
            conditions.selfSwitchCh = node.getString("selfSwitchCh", "A");
            conditions.selfSwitchValid = node.getBoolean("selfSwitchValid", false);
            conditions.switch1Valid = node.getBoolean("switch1Valid", false);
            conditions.switch1Id = node.getInt("switch1Id", 0);
            conditions.switch2Valid = node.getBoolean("switch2Valid", false);
            conditions.switch2Id = node.getInt("switch2Id", 0);
            conditions.variableValid = node.getBoolean("variableValid", false);
            conditions.variableId = node.getInt("variableId", 0);
            conditions.variableValue = node.getInt("variableValue", 0);
            return conditions;
        }
    }

    /** The character graphic of a page (sprite sheet cell, direction, ...). */
    public static final class EventGraphic {
        public String characterName;
        public int characterHue;
        public int direction;
        public int pattern;
        public int tileId;
        public int opacity;
        public int blendType;

        public static EventGraphic parse(JsonValue node) {
            EventGraphic graphic = new EventGraphic();
            if (node == null || !node.isObject()) {
                return graphic;
            }
            graphic.characterName = node.getString("characterName", "");
            graphic.characterHue = node.getInt("characterHue", 0);
            graphic.direction = node.getInt("direction", 2);
            graphic.pattern = node.getInt("pattern", 0);
            graphic.tileId = node.getInt("tileId", 0);
            graphic.opacity = node.getInt("opacity", 255);
            graphic.blendType = node.getInt("blendType", 0);
            return graphic;
        }
    }
}
