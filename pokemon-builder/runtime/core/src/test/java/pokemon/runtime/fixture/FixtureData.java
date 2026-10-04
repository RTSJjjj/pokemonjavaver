package pokemon.runtime.fixture;

import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;

import java.io.File;

/**
 * R15: locates the Small Test Project fixture (project3 section 56) - the
 * committed two map RMXP project under {@code tests/fixture-project}. The
 * Java tests read its converted {@code generated/}; the Node side converts the
 * {@code .rxdata} sources, so the runtime never needs a Ruby marshal reader.
 */
public final class FixtureData {

    private FixtureData() {
    }

    /** The converted fixture data root, or null when it is not available. */
    public static File root() {
        String configured = System.getProperty("pokemon.fixture.data");
        if (configured != null) {
            File file = new File(configured);
            if (new File(file, "project.json").isFile()) {
                return file;
            }
        }
        String[] candidates = {
                "../../tests/fixture-project/generated",
                "../tests/fixture-project/generated",
                "tests/fixture-project/generated",
                "E:/仓库/范例/929/pokemon-builder/tests/fixture-project/generated",
        };
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (new File(file, "project.json").isFile()) {
                return file;
            }
        }
        return null;
    }

    /** Loads the fixture database, or null when the fixture is not available. */
    public static GameDatabase load() {
        File root = root();
        if (root == null) {
            return null;
        }
        return GameDatabase.load(root.getAbsolutePath(), root);
    }

    /** Event by editor name on a map, or null. */
    public static MapData.EventData eventByName(MapData data, String name) {
        for (MapData.EventData event : data.events) {
            if (name.equals(event.name)) {
                return event;
            }
        }
        return null;
    }
}
