package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonValue;

/** One tileset of generated/tilesets.json, with the decoded RGSS Tables. */
public final class TilesetData {

    public int id;
    public String name;
    public String tilesetName;
    public String[] autotileNames;
    public String panoramaName;
    public int panoramaHue;
    public String fogName;
    public int fogHue;
    public int fogOpacity;
    public int fogBlendType;
    public int fogZoom;
    public int fogSx;
    public int fogSy;
    public String battlebackName;
    public TableData passages;
    public TableData priorities;
    public TableData terrainTags;

    public static TilesetData parse(JsonValue root) {
        TilesetData tileset = new TilesetData();
        tileset.id = root.getInt("id", -1);
        tileset.name = root.getString("name", "");
        tileset.tilesetName = root.getString("tilesetName", "");
        tileset.autotileNames = stringArray(root.get("autotileNames"));
        tileset.panoramaName = root.getString("panoramaName", "");
        tileset.panoramaHue = root.getInt("panoramaHue", 0);
        tileset.fogName = root.getString("fogName", "");
        tileset.fogHue = root.getInt("fogHue", 0);
        tileset.fogOpacity = root.getInt("fogOpacity", 0);
        tileset.fogBlendType = root.getInt("fogBlendType", 0);
        tileset.fogZoom = root.getInt("fogZoom", 0);
        tileset.fogSx = root.getInt("fogSx", 0);
        tileset.fogSy = root.getInt("fogSy", 0);
        tileset.battlebackName = root.getString("battlebackName", "");
        tileset.passages = TableData.parse(root.get("passages"));
        tileset.priorities = TableData.parse(root.get("priorities"));
        tileset.terrainTags = TableData.parse(root.get("terrainTags"));
        return tileset;
    }

    private static String[] stringArray(JsonValue node) {
        if (node == null || !node.isArray()) {
            return new String[0];
        }
        String[] values = new String[node.size];
        int i = 0;
        for (JsonValue entry = node.child; entry != null; entry = entry.next, i++) {
            values[i] = entry.asString();
        }
        return values;
    }

    public int passage(int tileId) {
        return passages == null ? 0 : passages.value(tileId);
    }

    public int priority(int tileId) {
        return priorities == null ? 0 : priorities.value(tileId);
    }

    public int terrainTag(int tileId) {
        return terrainTags == null ? 0 : terrainTags.value(tileId);
    }

    /**
     * One RGSS Table as emitted by the Builder. Tileset tables are single
     * layer, so layers holds one array with every element.
     */
    public static final class TableData {
        public boolean present;
        public String error;
        public int z;
        public int x;
        public int y;
        public int total;
        public int[][] layers;

        public int value(int position) {
            if (layers == null || layers.length == 0) {
                return 0;
            }
            if (position < 0 || position >= layers[0].length) {
                return 0;
            }
            return layers[0][position];
        }

        public static TableData parse(JsonValue node) {
            TableData table = new TableData();
            if (node == null || !node.getBoolean("present", false)) {
                return table;
            }
            table.present = true;
            String error = node.getString("error", null);
            if (error != null) {
                table.error = error;
                return table;
            }
            table.z = node.getInt("z", 0);
            table.x = node.getInt("x", 0);
            table.y = node.getInt("y", 0);
            table.total = node.getInt("total", 0);
            JsonValue layers = node.get("layers");
            if (layers == null || !layers.isArray()) {
                table.error = "table has no layers array";
                return table;
            }
            int layerCount = layers.size;
            table.layers = new int[layerCount][];
            int layer = 0;
            for (JsonValue values = layers.child; values != null; values = values.next, layer++) {
                int[] row = new int[values.isArray() ? values.size : 0];
                if (values.isArray()) {
                    int i = 0;
                    for (JsonValue value = values.child; value != null; value = value.next, i++) {
                        row[i] = value.asInt();
                    }
                }
                table.layers[layer] = row;
            }
            return table;
        }
    }
}