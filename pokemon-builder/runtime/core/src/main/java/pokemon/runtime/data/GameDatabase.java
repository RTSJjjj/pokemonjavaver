package pokemon.runtime.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import pokemon.runtime.map.MapLinks;
import pokemon.runtime.pokemon.PbsData;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * The one place that holds the data the Builder compiled: the project
 * manifest, the map index, tilesets, the System start position and the audio
 * manifest. Maps and common events are loaded lazily from their JSON files and
 * kept in a small bounded cache: data is read from disk once per map, never
 * per frame (project3 section 81).
 */
public final class GameDatabase {

    private static final int MAP_CACHE_LIMIT = 8;

    private final File dataRoot;
    private final Json json;
    private final JsonReader jsonReader;
    private final Array<String> warnings = new Array<>();

    private final ProjectInfo project;
    private final SystemData system;
    private final TilesetData[] tilesetsById;
    private final Array<TilesetData> tilesets;
    private final Array<MapInfo> maps;
    private final Array<CommonEventInfo> commonEvents;
    private final AudioManifestData audioManifest;
    /** R6.23: PBS/connections.txt map links (empty when the data predates them). */
    private final MapLinks links;
    /** R6.27: Data/Animations.rxdata for Show Animation (207). */
    private final AnimationData animations;
    /** L1: title screen configuration (splash slides / title BGM). */
    private final TitleData title;

    /** Stage 3 / P0: PBS tables (species, moves, items, ...), loaded lazily. */
    private PbsData pbs;
    private boolean pbsLoaded;
    private PinyinTable pinyin;
    private boolean pinyinLoaded;

    private final ObjectMap<Integer, MapData> mapCache = new ObjectMap<>();
    private final ObjectMap<Integer, CommonEventData> commonEventCache = new ObjectMap<>();

    private GameDatabase(File dataRoot, ProjectInfo project, SystemData system,
            Array<TilesetData> tilesets, Array<MapInfo> maps, Array<CommonEventInfo> commonEvents,
            AudioManifestData audioManifest, MapLinks links, AnimationData animations,
            TitleData title) {
        this.dataRoot = dataRoot;
        this.project = project;
        this.system = system;
        this.tilesets = tilesets;
        this.maps = maps;
        this.commonEvents = commonEvents;
        this.audioManifest = audioManifest;
        this.links = links;
        this.animations = animations;
        this.title = title;
        this.json = new Json();
        this.json.setIgnoreUnknownFields(true);
        this.jsonReader = new JsonReader();

        int maxTilesetId = 0;
        for (TilesetData tileset : tilesets) {
            if (tileset.id > maxTilesetId) {
                maxTilesetId = tileset.id;
            }
        }
        this.tilesetsById = new TilesetData[maxTilesetId + 1];
        for (TilesetData tileset : tilesets) {
            if (tileset.id >= 0 && tileset.id <= maxTilesetId) {
                tilesetsById[tileset.id] = tileset;
            }
        }
    }

    /**
     * Loads the manifests and indexes that exist in the runtime data root.
     * Maps and common events stay lazy: only the index is read up front.
     */
    public static GameDatabase load(String explicitDataRoot, File baseDir) {
        File root = RuntimeDataLocator.resolve(explicitDataRoot, baseDir);
        if (!new File(root, "project.json").isFile()) {
            throw new RuntimeDataException(
                    "runtime data not found: expected project.json in " + root
                            + " (run `builder build-data` first)");
        }
        Json json = new Json();
        json.setIgnoreUnknownFields(true);

        ProjectInfo project;
        try {
            project = json.fromJson(ProjectInfo.class,
                    new String(Files.readAllBytes(new File(root, "project.json").toPath()), StandardCharsets.UTF_8));
        } catch (Exception error) {
            throw new RuntimeDataException("cannot read project.json: " + error.getMessage(), error);
        }

        JsonValue tilesetsRoot = readJson(root, "tilesets.json");
        Array<TilesetData> tilesets = new Array<>();
        if (tilesetsRoot != null) {
            JsonValue tilesetEntries = tilesetsRoot.get("tilesets");
            if (tilesetEntries != null && tilesetEntries.isArray()) {
                for (JsonValue entry = tilesetEntries.child; entry != null; entry = entry.next) {
                    TilesetData tileset = TilesetData.parse(entry);
                    if (tileset.id >= 0) {
                        tilesets.add(tileset);
                    }
                }
            }
        }

        JsonValue systemRoot = readJson(root, "system.json");
        SystemData system = systemRoot == null ? null : SystemData.parse(systemRoot);

        MapsIndex mapsIndex = read(json, root, "maps/index.json", MapsIndex.class);
        Array<MapInfo> maps = new Array<>();
        if (mapsIndex != null && mapsIndex.maps != null) {
            maps.addAll(mapsIndex.maps);
        }

        CommonEventsIndex commonEventsIndex = read(json, root, "common-events/index.json", CommonEventsIndex.class);
        Array<CommonEventInfo> commonEvents = new Array<>();
        if (commonEventsIndex != null && commonEventsIndex.commonEvents != null) {
            commonEvents.addAll(commonEventsIndex.commonEvents);
        }

        File audioManifestFile = new File(root, "audio/audio-manifest.json");
        AudioManifestData audioManifest;
        if (audioManifestFile.isFile()) {
            try {
                audioManifest = AudioManifestData.parse(
                        new JsonReader().parse(new String(Files.readAllBytes(audioManifestFile.toPath()), StandardCharsets.UTF_8)));
            } catch (Exception error) {
                throw new RuntimeDataException("cannot read audio-manifest.json: " + error.getMessage(), error);
            }
        } else {
            audioManifest = AudioManifestData.notLoaded();
        }

        MapLinks links = MapLinks.parse(readJson(root, "connections.json"));
        AnimationData animations = AnimationData.parse(readJson(root, "animations.json"));
        TitleData title = TitleData.parse(readJson(root, "title.json"));
        GameDatabase database = new GameDatabase(root, project, system, tilesets, maps, commonEvents, audioManifest,
                links, animations, title);
        database.quests = QuestTable.parse(readJson(root, "quests.json"));   // 284_004_Quest_Data
        return database;
    }

    private QuestTable quests = QuestTable.empty();

    /** The Quest plugin's quest definitions (never null; empty without quests.json). */
    public QuestTable quests() {
        return quests;
    }

    /** The RMXP map name of {@code mapId} ({@code $game_map.name}), or "" for an unknown map. */
    public String mapName(int mapId) {
        for (MapInfo info : maps) {
            if (info.mapId == mapId) {
                return info.name == null ? "" : info.name;
            }
        }
        return "";
    }

    private static JsonValue readJson(File root, String relative) {
        File file = new File(root, relative);
        if (!file.isFile()) {
            return null;
        }
        try {
            return new JsonReader().parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (Exception error) {
            throw new RuntimeDataException("cannot read " + relative + ": " + error.getMessage(), error);
        }
    }

    private static <T> T read(Json json, File root, String relative, Class<T> type) {
        File file = new File(root, relative);
        if (!file.isFile()) {
            return null;
        }
        try {
            return json.fromJson(type, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (Exception error) {
            throw new RuntimeDataException("cannot read " + relative + ": " + error.getMessage(), error);
        }
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public File dataRoot() {
        return dataRoot;
    }

    public ProjectInfo project() {
        return project;
    }

    public String projectName() {
        return project.projectName;
    }

    public String builderVersion() {
        return project.builderVersion;
    }

    public Array<String> warnings() {
        return warnings;
    }

    public Array<MapInfo> maps() {
        return maps;
    }

    /**
     * Stage 3 / P0: the Essentials PBS tables ({@code generated/pbs/*.json}).
     * Loaded on first use so the title screen and menus do not pay for the
     * ~6 MB of JSON up front.
     */
    public PbsData pbs() {
        if (!pbsLoaded) {
            pbs = PbsData.parse(dataRoot);
            pbsLoaded = true;
        }
        return pbs;
    }

    /**
     * P4: the in-game pinyin name table ({@code generated/text/pinyin.json}).
     * Loaded on first use like {@link #pbs()}.
     */
    public PinyinTable pinyin() {
        if (!pinyinLoaded) {
            pinyin = PinyinTable.load(dataRoot);
            pinyinLoaded = true;
        }
        return pinyin;
    }

    public int mapCount() {
        return maps.size;
    }

    public Array<CommonEventInfo> commonEvents() {
        return commonEvents;
    }

    public Array<TilesetData> tilesets() {
        return tilesets;
    }

    public TilesetData tileset(int tilesetId) {
        if (tilesetId < 0 || tilesetId >= tilesetsById.length) {
            return null;
        }
        return tilesetsById[tilesetId];
    }

    public SystemData system() {
        return system;
    }

    /** L1: title screen configuration (never null; empty without title.json). */
    public TitleData title() {
        return title;
    }

    /** The map the game starts on, from System.rxdata. */
    public int startMapId() {
        return system == null ? 1 : system.startMapId;
    }

    public int startX() {
        return system == null ? 0 : system.startX;
    }

    public int startY() {
        return system == null ? 0 : system.startY;
    }

    public boolean audioManifestLoaded() {
        return audioManifest.loaded;
    }

    public AudioManifestData audio() {
        return audioManifest;
    }

    /** R6.23: the Essentials map connections (never null, possibly empty). */
    public MapLinks links() {
        return links;
    }

    /** R6.27: Show Animation (207) data (never null, possibly empty). */
    public AnimationData animations() {
        return animations;
    }

    public AudioManifestData.Entry audioEntry(String logicalId) {
        return audioManifest.find(logicalId);
    }

    /** Lazily loads one map document; cached with a small bounded map cache. */
    public MapData map(int mapId) {
        MapData cached = mapCache.get(mapId);
        if (cached != null) {
            return cached;
        }
        MapInfo info = null;
        for (MapInfo candidate : maps) {
            if (candidate.mapId == mapId) {
                info = candidate;
                break;
            }
        }
        if (info == null || info.ir == null) {
            throw new RuntimeDataException("map " + mapId + " is not part of the runtime data index");
        }
        MapData data = parse(new File(dataRoot, info.ir), MapData::parse);
        if (mapCache.size >= MAP_CACHE_LIMIT) {
            // R6.24: evict one map instead of dropping the whole cache - walking
            // a map connection would otherwise re-parse the neighbours we just
            // walked through.
            Integer oldest = null;
            for (ObjectMap.Entry<Integer, MapData> entry : mapCache.entries()) {
                oldest = entry.key;
                break;
            }
            if (oldest != null) {
                mapCache.remove(oldest);
            }
        }
        mapCache.put(mapId, data);
        return data;
    }

    /** Lazily loads one common event document. */
    public CommonEventData commonEvent(int commonEventId) {
        CommonEventData cached = commonEventCache.get(commonEventId);
        if (cached != null) {
            return cached;
        }
        CommonEventInfo info = null;
        for (CommonEventInfo candidate : commonEvents) {
            if (candidate.id == commonEventId) {
                info = candidate;
                break;
            }
        }
        if (info == null || info.ir == null) {
            throw new RuntimeDataException("common event " + commonEventId + " is not part of the runtime data index");
        }
        CommonEventData data = parse(new File(dataRoot, info.ir), CommonEventData::parse);
        if (commonEventCache.size >= MAP_CACHE_LIMIT) {
            commonEventCache.clear();
        }
        commonEventCache.put(commonEventId, data);
        return data;
    }

    private <T> T parse(File file, java.util.function.Function<JsonValue, T> parser) {
        if (!file.isFile()) {
            throw new RuntimeDataException("runtime data file missing: " + file.getAbsolutePath());
        }
        try {
            JsonValue root = jsonReader.parse(
                    new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            return parser.apply(root);
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeDataException("cannot parse " + file.getName() + ": " + error.getMessage(), error);
        }
    }

    // ------------------------------------------------------------------
    // JSON documents bound by name (unknown fields are ignored)
    // ------------------------------------------------------------------

    private static class MapsIndex {
        public Array<MapInfo> maps;
    }

    private static class CommonEventsIndex {
        public Array<CommonEventInfo> commonEvents;
    }

    /** Releases the lazily loaded map and common event documents. */
    public void dispose() {
        mapCache.clear();
        commonEventCache.clear();
        pbs = null;
        pbsLoaded = false;
    }
}
