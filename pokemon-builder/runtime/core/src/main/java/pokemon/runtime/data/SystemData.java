package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonValue;

/**
 * generated/system.json: the start position plus the display names referenced
 * by the game data (elements, switches, variables, words). Parsed manually
 * because the words table is an array of [key, text] pairs.
 */
public final class SystemData {

    public String kind;
    public int magicNumber;
    public int startMapId;
    public int startX;
    public int startY;
    public int editMapId;
    public String windowskinName;
    public String titleName;
    public String gameoverName;
    public String battlebackName;
    public String[] elements;
    public String[] switches;
    public String[] variables;
    public String[][] words;

    public static SystemData parse(JsonValue root) {
        SystemData data = new SystemData();
        data.kind = root.getString("kind", null);
        data.magicNumber = root.getInt("magicNumber", 0);
        data.startMapId = root.getInt("startMapId", 1);
        data.startX = root.getInt("startX", 0);
        data.startY = root.getInt("startY", 0);
        data.editMapId = root.getInt("editMapId", 0);
        data.windowskinName = root.getString("windowskinName", "");
        data.titleName = root.getString("titleName", "");
        data.gameoverName = root.getString("gameoverName", "");
        data.battlebackName = root.getString("battlebackName", "");
        data.elements = strings(root.get("elements"));
        data.switches = strings(root.get("switches"));
        data.variables = strings(root.get("variables"));
        data.words = wordPairs(root.get("words"));
        return data;
    }

    private static String[] strings(JsonValue node) {
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

    private static String[][] wordPairs(JsonValue node) {
        if (node == null || !node.isArray()) {
            return new String[0][];
        }
        String[][] pairs = new String[node.size][];
        int i = 0;
        for (JsonValue entry = node.child; entry != null; entry = entry.next, i++) {
            if (entry.isArray() && entry.size >= 2) {
                pairs[i] = new String[] { entry.child.asString(), entry.child.next.asString() };
            } else {
                pairs[i] = new String[] { entry.asString(), "" };
            }
        }
        return pairs;
    }
}