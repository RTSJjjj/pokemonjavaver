package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A small PBS (species and moves) written to a temporary folder and parsed, for battle tests that need a few Pokemon and
 * moves and not the whole game's data.
 *
 * <pre>{@code
 * PbsData pbs = new BattleFixture(tempDir)
 *         .species("BULBASAUR", 1, "妙蛙种子", "GRASS", new int[] {45, 49, 49, 45, 65, 65}, "OVERGROW")
 *         .move("TACKLE", 33, "NORMAL", 35)
 *         .build();
 * }</pre>
 */
final class BattleFixture {

    private final Path root;
    private final List<String> species = new ArrayList<>();
    private final List<String> moves = new ArrayList<>();

    BattleFixture(Path root) {
        this.root = root;
    }

    /** A species with one type and its natural abilities (a "Female50Percent" gender rate). */
    BattleFixture species(String internalName, int id, String name, String type, int[] baseStats, String... abilities) {
        StringBuilder stats = new StringBuilder();
        for (int i = 0; i < baseStats.length; i++) stats.append(i == 0 ? "" : ",").append(baseStats[i]);
        StringBuilder abilityList = new StringBuilder();
        for (int i = 0; i < abilities.length; i++) abilityList.append(i == 0 ? "" : ",").append('"').append(abilities[i]).append('"');
        species.add("\"" + internalName + "\":{\"id\":" + id + ",\"internalName\":\"" + internalName + "\",\"name\":\"" + name
                + "\",\"types\":[\"" + type + "\"],\"baseStats\":[" + stats + "],\"rareness\":45,\"genderRate\":\"Female50Percent\","
                + "\"abilities\":[" + abilityList + "],\"evolutions\":[]}");
        return this;
    }

    /** A 40-power physical move with function code 000. */
    BattleFixture move(String internalName, int id, String type, int pp) {
        return move(internalName, id, type, pp, "000", 40, "Physical");
    }

    BattleFixture move(String internalName, int id, String type, int pp, String function, int power, String category) {
        moves.add("\"" + internalName + "\":{\"id\":" + id + ",\"internalName\":\"" + internalName + "\",\"name\":\"" + internalName
                + "\",\"function\":\"" + function + "\",\"power\":" + power + ",\"type\":\"" + type + "\",\"category\":\"" + category
                + "\",\"accuracy\":100,\"pp\":" + pp + ",\"effectChance\":0,\"target\":\"NearOther\",\"priority\":0,\"flags\":\"\"}");
        return this;
    }

    PbsData build() {
        try {
            write("index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
            write("pokemon.json", "{\"total\":" + species.size() + ",\"byId\":{},\"species\":{" + String.join(",", species) + "}}");
            write("moves.json", "{\"total\":" + moves.size() + ",\"moves\":{" + String.join(",", moves) + "}}");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return PbsData.parse(root.toFile());
    }

    private void write(String name, String json) throws IOException {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }
}
