package pokemon.runtime.data;

import java.io.File;

/**
 * Locates the Builder runtime data root (project3 section 9).
 *
 * <p>Resolution order: explicit argument, the POKEMON_RUNTIME_DATA environment
 * variable, the pokemon.runtime.data system property, then
 * {@code <base>/generated} (the Builder default output) and finally
 * {@code <base>/runtime-data}. The runtime never writes here: the data is a
 * build artifact.</p>
 */
public final class RuntimeDataLocator {

    private RuntimeDataLocator() {
    }

    /**
     * A root that holds no {@code project.json} itself but has {@code runtime-data/} or {@code generated/} below it is that
     * folder: the Android launcher hands over the app's files directory, where the data pack unpacks the Builder output as
     * {@code generated/} (and {@code Graphics/}, {@code Fonts/} next to it).
     */
    static File withLayout(File root) {
        if (new File(root, "project.json").isFile()) {
            return root;
        }
        for (String child : new String[] {"runtime-data", "generated"}) {
            File candidate = new File(root, child);
            if (new File(candidate, "project.json").isFile()) {
                return candidate;
            }
        }
        return root;
    }

    public static File resolve(String explicit, File baseDir) {
        if (explicit != null && !explicit.isEmpty()) {
            return withLayout(new File(explicit));
        }
        String fromEnv = System.getenv("POKEMON_RUNTIME_DATA");
        if (fromEnv != null && !fromEnv.isEmpty()) {
            return withLayout(new File(fromEnv));
        }
        String fromProperty = System.getProperty("pokemon.runtime.data");
        if (fromProperty != null && !fromProperty.isEmpty()) {
            return withLayout(new File(fromProperty));
        }
        File generated = new File(baseDir, "generated");
        if (generated.isDirectory()) {
            return generated;
        }
        return new File(baseDir, "runtime-data");
    }
}