package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import com.badlogic.gdx.utils.Array;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Battle_Action_AttacksPriority:136-267 pbCalculatePriority / pbPriority and
 * Battle_Phase_Attack:24-48/105-194.
 */
class BattleAttackPhaseTest {

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
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,200,60,60],"
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


    private static Battle battle(PbsData data) {
        return new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(new Pokemon(data.species("HERO"), 20, data))
                .addFoe(new Pokemon(data.species("FOE"), 20, data));
    }

    @Test
    @DisplayName("pbCalculatePriority: the faster battler goes first and the move's priority is saved in choices[4] (:151-174, :235)")
    void speedOrderAndSavedPriority(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.registerMove(0, 0);
        b.registerMove(1, 0);
        b.pbCalculatePriority(true, null);
        Array<Battler> order = b.pbPriority(false);
        assertEquals(1, order.get(0).index, "FOE has the higher Speed");
        assertEquals(0, ((Integer) b.choices(0)[4]).intValue());
        assertEquals(1, b.pbPriority(true).get(0).index);
    }

    @Test
    @DisplayName("pbCalculatePriority: Trick Room reverses the speed order (:230-232)")
    void trickRoom(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.registerMove(0, 0);
        b.registerMove(1, 0);
        b.field.effects.set(PBEffects.Field.TrickRoom, 5);
        b.pbCalculatePriority(true, null);
        assertEquals(0, b.pbPriority(false).get(0).index);
    }

    @Test
    @DisplayName("pbAttackPhase: both battlers use their move (:105-164)")
    void attackPhaseRunsBothMoves(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.registerMove(0, 0);
        b.registerMove(1, 0);
        BattleAttackPhase.pbAttackPhase(b);
        assertTrue(b.player().movedThisRound());
        assertTrue(b.foe().movedThisRound());
    }

    @Test
    @DisplayName("pbAbleNonActiveCount counts the able party members that are not on the field (PokeBattle_Battle:340-352)")
    void ableNonActiveCount(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data).addPlayer(new Pokemon(data.species("HERO"), 20, data));
        assertEquals(1, b.pbAbleNonActiveCount(0));
        assertEquals(0, b.pbAbleNonActiveCount(1));
    }
}
