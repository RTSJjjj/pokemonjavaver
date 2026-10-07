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
 * Battle_Phase_Command (forced choices, auto-choosing a move), Battle_Action_AttacksPriority:36-68 and
 * PokeBattle_BOSS:37-153.
 */
class BattleCommandPhaseTest {

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
        Battle b = new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(new Pokemon(data.species("HERO"), 20, data))
                .addFoe(new Pokemon(data.species("FOE"), 20, data));
        b.pbCalculatePriority(true, null);
        return b;
    }

    @Test
    @DisplayName("pbAutoChooseMove: a battler with no usable move is registered to use Struggle (:60-67)")
    void struggle(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.player().pokemon.moves.get(0).pp = 0;
        assertFalse(b.pbCanShowFightMenu(0));
        assertTrue(b.pbAutoChooseMove(0, true));
        Object[] c = b.choices(0);
        assertEquals(":UseMove", c[0]);
        assertEquals(-1, c[1]);
        assertEquals("STRUGGLE", ((BattleMove) c[2]).internalName());
        boolean said = false;
        for (Battle.RoundEvent e : b.roundEvents) if (e.toString().contains("没有招式可使用了")) said = true;
        assertTrue(said);
    }

    @Test
    @DisplayName("a battler in the middle of a multi-turn attack keeps its choice into the next round (:183)")
    void forcedChoiceStays(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.registerMove(0, 0);
        b.player().effects.set(PBEffects.Battler.HyperBeam, 2);
        b.player().currentMove = b.player().moveSlot(0).id();      // pbUseMove:178 uses @currentMove
        assertFalse(b.pbCanShowCommands(0));
        b.step();
        assertEquals(":UseMove", b.choices(0)[0]);
        assertEquals(":None", b.choices(1)[0]);
    }

    @Test
    @DisplayName("a Mega Evolution that was registered but not performed is dropped when the next command phase begins (:186-190)")
    void unusedMegaRegistrationDropped(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.megaEvolution[0][0] = 0;
        b.registerMove(0, 0);
        b.step();
        assertEquals(-1, b.megaEvolution[0][0]);
    }

    @Test
    @DisplayName("hasAnyNegativeEffects / removeAllNegativeEffects (PokeBattle_BOSS:95-153)")
    void negativeEffects(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        Battler p = b.player();
        assertFalse(p.hasAnyNegativeEffects());
        p.effects.set(PBEffects.Battler.Taunt, 3);
        p.setStage(PBStats.ATTACK, -2);
        assertTrue(p.hasAnyNegativeEffects());
        p.removeAllNegativeEffects(true);
        assertFalse(p.hasAnyNegativeEffects());
        assertEquals(0, p.stage(PBStats.ATTACK));
    }

    @Test
    @DisplayName("pbBossBuffPhase: on round 0 a rank>2 foe raises one main stat (:44-57)")
    void bossFirstRound(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.foe().pokemon.battleRank = 3;
        b.pbBossBuffPhase();
        int raised = 0;
        for (int s : PBStats.EACH_MAIN_BATTLE_STAT) raised += b.foe().stage(s);
        assertEquals(1, raised);
        boolean said = false;
        for (String m : b.roundMessages) if (m.contains("凭借它的力量")) said = true;
        assertTrue(said);
    }

    @Test
    @DisplayName("pbBossBuffPhase: an ordinary foe is left alone")
    void ordinaryFoe(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.pbBossBuffPhase();
        assertTrue(b.roundMessages.isEmpty());
    }
}
