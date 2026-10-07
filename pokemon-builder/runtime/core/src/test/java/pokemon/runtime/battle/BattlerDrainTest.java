package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code pbRecoverHPFromDrain} (Battler_ChangeSelf:33-48): the drain-move heal
 * that Absorb/Giga Drain (0DD) and Draining Kiss reach; it used to be a throwing stub.
 */
class BattlerDrainTest {

    private static void write(Path root, String name, String content) throws Exception {
        Path pbs = root.resolve("pbs");
        Files.createDirectories(pbs);
        Files.write(pbs.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    /** HERO knows STUNSPORE (30 PP); FOE knows TACKLE. */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":2,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"STUNSPORE\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        write(root, "moves.json", "{\"total\":2,\"moves\":{"
                + "\"STUNSPORE\":{\"id\":1,\"internalName\":\"STUNSPORE\",\"name\":\"Stun Spore\","
                + "\"function\":\"007\",\"power\":0,\"type\":\"GRASS\",\"category\":\"Status\","
                + "\"accuracy\":100,\"pp\":30,\"flags\":\"bcel\",\"target\":\"NearOther\"},"
                + "\"TACKLE\":{\"id\":2,\"internalName\":\"TACKLE\",\"name\":\"Tackle\","
                + "\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"flags\":\"a\",\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(root, "types.json", "{\"total\":2,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"GRASS\":{\"id\":4,\"internalName\":\"GRASS\",\"name\":\"Grass\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }

    private static Battler hero(PbsData data) {
        Battle battle = new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(new Pokemon(data.species("HERO"), 20, data))
                .addFoe(new Pokemon(data.species("FOE"), 20, data));
        return battle.player();
    }

    @Test
    @DisplayName("drain: the user recovers the amount and says so (Battler_ChangeSelf:41-45)")
    void drainHeals(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(new Pokemon(data.species("HERO"), 20, data))
                .addFoe(new Pokemon(data.species("FOE"), 20, data));
        Battler user = battle.player();
        Battler target = battle.foe();
        user.pbReduceHP(30);
        int before = user.hp;
        user.pbRecoverHPFromDrain(10, target);
        assertEquals(before + 10, user.hp);
    }

    @Test
    @DisplayName("drain: Liquid Ooze hurts the drainer instead (Battler_ChangeSelf:34-39)")
    void liquidOozeHurts(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(new Pokemon(data.species("HERO"), 20, data))
                .addFoe(new Pokemon(data.species("FOE"), 20, data));
        Battler user = battle.player();
        Battler target = battle.foe();
        target.ability = "LIQUIDOOZE";
        int before = user.hp;
        user.pbRecoverHPFromDrain(10, target);
        assertEquals(before - 10, user.hp);
    }
}
