package pokemon.runtime.battle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B8-A: {@code BattleAi}, the wild Pokemon's move choice transcribed from
 * {@code PokeBattle_AI#pbChooseMoves} ({@code AI_Move:6-146}) and
 * {@code pbRegisterMoveWild} ({@code :153-155}).
 *
 * <p>The plugin gives every usable wild move the same 100 ({@code :154}), so the
 * weighted pick of {@code :124-132} has to end up covering all of them - the
 * first test measures that over many rounds with a fixed seed rather than
 * asserting one exact pick.</p>
 */
class BattleAiTest {

    @TempDir
    Path tempDir;

    private PbsData pbs;
    private Battle battle;
    private Pokemon foe;

    /** The four moves every test's wild Pokemon knows, all with PP. */
    private static final String[] MOVES = { "TACKLE", "GROWL", "VINEWHIP", "EMBER" };

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static String move(String id, String name, int pp) {
        return "\"" + id + "\":{\"id\":" + (id.hashCode() & 0x7fff) + ",\"internalName\":\"" + id
                + "\",\"name\":\"" + name + "\",\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\","
                + "\"category\":\"Physical\",\"accuracy\":100,\"pp\":" + pp
                + ",\"effectChance\":0,\"target\":\"NearOther\",\"priority\":0,\"flags\":\"\"}";
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(tempDir, "pokemon.json", "{\"total\":1,\"byId\":{},\"species\":{"
                + "\"BULBASAUR\":{\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"妙蛙种子\","
                + "\"types\":[\"GRASS\"],\"baseStats\":[45,49,49,45,65,65],\"rareness\":45,"
                + "\"weight\":6.9,\"genderRate\":\"Female50Percent\",\"evolutions\":[]}}}");
        write(tempDir, "moves.json", "{\"total\":4,\"moves\":{"
                + move("TACKLE", "撞击", 35) + "," + move("GROWL", "叫声", 40) + ","
                + move("VINEWHIP", "藤鞭", 25) + "," + move("EMBER", "火花", 25) + "}}");
        pbs = PbsData.parse(tempDir.toFile());

        battle = new Battle(pbs, new Random(7), (user, foe, moves) -> 0);
        battle.addPlayer(new Pokemon(pbs.species("BULBASAUR"), 5, pbs));
        foe = new Pokemon(pbs.species("BULBASAUR"), 5, pbs);
        // Replace the learnset with the four fixture moves so every slot is known.
        foe.moves.clear();
        for (String id : MOVES) {
            PbsData.Move data = pbs.move(id);
            assertNotNull(data, id + " must exist in the fixture");
            foe.moves.add(new Pokemon.MoveSlot(data));
        }
        battle.addFoe(foe);
    }

    private Battler foeBattler() {
        return battle.foe();
    }

    @Test
    @DisplayName("wild battler with four usable moves uses all four over many rounds (:124-132)")
    void allFourMovesAreReachable() {
        Battler user = foeBattler();
        assertTrue(BattleAi.handles(battle, user), "a wild foe is the AI's own case (:8/:20)");

        Random random = new Random(20261007);
        int[] picked = new int[Battler.MOVES_MAX];
        int rounds = 4000;
        for (int i = 0; i < rounds; i++) {
            int slot = BattleAi.chooseMove(battle, user, random);
            assertTrue(slot >= 0 && slot < Battler.MOVES_MAX, "slot out of range: " + slot);
            picked[slot]++;
        }
        for (int slot = 0; slot < Battler.MOVES_MAX; slot++) {
            assertTrue(picked[slot] > 0, "slot " + slot + " was never chosen: "
                    + java.util.Arrays.toString(picked));
        }
        // Every choice scores 100 (:154), so the pick has to be uniform: allow a
        // generous 20% band around rounds/4 rather than an exact count.
        int expected = rounds / Battler.MOVES_MAX;
        for (int slot = 0; slot < Battler.MOVES_MAX; slot++) {
            assertTrue(Math.abs(picked[slot] - expected) < expected / 5,
                    "slot " + slot + " is not uniform: " + java.util.Arrays.toString(picked));
        }
    }

    @Test
    @DisplayName("a 0 PP slot is refused by pbCanChooseMove? and never chosen (:19)")
    void zeroPpSlotIsNeverChosen() {
        Battler user = foeBattler();
        foe.moves.get(2).pp = 0;                               // slot 2 is out of PP
        assertNotNull(battle.canChooseMove(user.index, 2), "the engine refuses a 0 PP slot");

        Random random = new Random(20261007);
        for (int i = 0; i < 2000; i++) {
            int slot = BattleAi.chooseMove(battle, user, random);
            assertNotEquals(2, slot, "a 0 PP move must never be picked");
        }
    }

    @Test
    @DisplayName("with nothing usable the plugin runs pbAutoChooseMove, i.e. Struggle (:120-122)")
    void noUsableMoveFallsBackToStruggle() {
        Battler user = foeBattler();
        for (Pokemon.MoveSlot slot : foe.moves) {
            slot.pp = 0;                                       // every slot out of PP
        }
        assertFalse(user.hasUsableMove(), "the runtime's pbCanChooseAnyMove? agrees");

        Random random = new Random(20261007);
        for (int i = 0; i < 50; i++) {
            assertEquals(BattleAi.STRUGGLE, BattleAi.chooseMove(battle, user, random));
        }
    }

    @Test
    @DisplayName("one usable slot is always the answer (:27/:125-131)")
    void singleCandidateIsAlwaysChosen() {
        Battler user = foeBattler();
        foe.moves.get(0).pp = 35;                              // slot 0 keeps its PP
        foe.moves.get(1).pp = 0;
        foe.moves.get(2).pp = 0;
        foe.moves.get(3).pp = 0;

        Random random = new Random(20261007);
        for (int i = 0; i < 200; i++) {
            assertEquals(0, BattleAi.chooseMove(battle, user, random));
        }
    }

    @Test
    @DisplayName("pbAIRandom is rand(x): 0...x, never x (PokeBattle_AI:83)")
    void aiRandomIsExclusive() {
        Random random = new Random(20261007);
        boolean sawZero = false;
        int highest = -1;
        for (int i = 0; i < 20000; i++) {
            double value = BattleAi.aiRandom(4, random);
            assertTrue(value >= 0 && value < 4, "out of 0...4: " + value);
            assertEquals(Math.rint(value), value, "a positive bound returns an Integer in Ruby");
            if (value == 0) {
                sawZero = true;
            }
            highest = Math.max(highest, (int) value);
        }
        assertTrue(sawZero, "0 is inclusive");
        assertEquals(3, highest, "3 is the largest value rand(4) can return (4 is exclusive)");

        // rand(0) returns a Float in [0,1) rather than an Integer, and must not
        // become Random.nextInt(0)'s IllegalArgumentException.
        for (int i = 0; i < 100; i++) {
            double value = BattleAi.aiRandom(0, random);
            assertTrue(value >= 0.0 && value < 1.0, "rand(0) must stay in [0,1): " + value);
        }
    }

    @Test
    @DisplayName("a trainer battle is not the wild path (:8/:20 -> pbRegisterMoveTrainer)")
    void trainerBattleIsNotHandled() {
        Pokemon player = new Pokemon(pbs.species("BULBASAUR"), 5, pbs);
        Battle trainer = new Battle(pbs, new Random(1), (user, enemy, moves) -> 0);
        trainer.trainerBattle = true;
        trainer.addPlayer(player);
        trainer.addFoe(new Pokemon(pbs.species("BULBASAUR"), 5, pbs));

        assertFalse(trainer.wildBattle());
        assertFalse(BattleAi.handles(trainer, trainer.foe()));
        assertEquals(BattleAi.NOT_HANDLED,
                BattleAi.chooseMove(trainer, trainer.foe(), new Random(3)));
    }

    @Test
    @DisplayName("a wild boss (battleRank >= 2) takes the trainer branch and stays unhandled (:20)")
    void wildBossIsNotHandled() {
        Battler user = foeBattler();
        foe.battleRank = 2;
        assertFalse(BattleAi.handles(battle, user));
        assertEquals(BattleAi.NOT_HANDLED, BattleAi.chooseMove(battle, user, new Random(3)));
    }

    @Test
    @DisplayName("blank slots are skipped, not scored (:18 eachMoveWithIndex)")
    void blankSlotsAreSkipped() {
        Battler user = foeBattler();
        foe.moves.removeRange(1, 3);                           // only slot 0 is left
        assertEquals(1, foe.moves.size);

        Random random = new Random(20261007);
        for (int i = 0; i < 100; i++) {
            assertEquals(0, BattleAi.chooseMove(battle, user, random));
        }
    }

    @Test
    @DisplayName("Battle.pickMove actually uses it for a wild opponent")
    void battleUsesTheWildAi() {
        // Battle.pickMove wires BattleAi.handles() before the simple AI, so a
        // wild opponent's choice must vary instead of always taking the biggest
        // hit. The player's controller always uses slot 0, so the foe is the only
        // side randomising here.
        java.util.Set<String> used = new java.util.HashSet<>();
        Random random = new Random(20261007);
        for (int round = 0; round < 60; round++) {
            Battle fresh = new Battle(pbs, random, (user, foe, moves) -> 0);
            fresh.addPlayer(new Pokemon(pbs.species("BULBASAUR"), 20, pbs));
            Pokemon wild = new Pokemon(pbs.species("BULBASAUR"), 20, pbs);
            wild.moves.clear();
            for (String id : MOVES) {
                wild.moves.add(new Pokemon.MoveSlot(pbs.move(id)));
            }
            fresh.addFoe(wild);
            for (int turn = 0; turn < 3; turn++) {
                fresh.step();
                for (int slot = 0; slot < 4; slot++) {
                    BattleMove move = fresh.foe().moveSlot(slot);
                    if (move != null && fresh.foe().moveSlotPp(slot)
                            < fresh.foe().moveSlotMaxPp(slot)) {
                        used.add(move.name());
                    }
                }
            }
        }
        assertTrue(used.size() > 1, "the wild AI must not always pick the same move: " + used);
    }
}
