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

    public static File resolve(String explicit, File baseDir) {
        if (explicit != null && !explicit.isEmpty()) {
            return new File(explicit);
        }
        String fromEnv = System.getenv("POKEMON_RUNTIME_DATA");
        if (fromEnv != null && !fromEnv.isEmpty()) {
            return new File(fromEnv);
        }
        String fromProperty = System.getProperty("pokemon.runtime.data");
        if (fromProperty != null && !fromProperty.isEmpty()) {
            return new File(fromProperty);
        }
        File generated = new File(baseDir, "generated");
        if (generated.isDirectory()) {
            return generated;
        }
        return new File(baseDir, "runtime-data");
    }
}