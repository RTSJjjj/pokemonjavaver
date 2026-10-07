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
 * Stage 5 / 2a: {@code pbBeginTurn}/{@code pbCancelMoves}/{@code pbEndTurn}
 * (Battler_UseMove:71-128), {@code pbReducePP}/{@code pbReducePPOther}
 * (Battler_ChangeSelf:141-152) and the small Battler queries they need. Each
 * assertion names the Ruby line it pins.
 */
class BattlerTurnBookkeepingTest {

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
                + "\"accuracy\":100,\"pp\":30,\"flags\":\"bcel\"},"
                + "\"TACKLE\":{\"id\":2,\"internalName\":\"TACKLE\",\"name\":\"Tackle\","
                + "\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"flags\":\"a\"}}}");
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
    @DisplayName("2a: pbBeginTurn clears the lingering effects and remembers DestinyBond (Battler_UseMove:73-79)")
    void beginTurnResetsLingeringEffects(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler b = hero(data);
        b.effects.set(PBEffects.Battler.BeakBlast, true);
        b.effects.set(PBEffects.Battler.DestinyBond, true);
        b.effects.set(PBEffects.Battler.Grudge, true);
        b.effects.set(PBEffects.Battler.MoveNext, true);
        b.effects.set(PBEffects.Battler.Quash, 3);
        b.effects.set(PBEffects.Battler.ShellTrap, true);

        b.pbBeginTurn(new Object[0]);

        assertFalse(b.effects.truthy(PBEffects.Battler.BeakBlast), ":73");
        assertTrue(b.effects.truthy(PBEffects.Battler.DestinyBondPrevious), ":74 previous takes the old value");
        assertFalse(b.effects.truthy(PBEffects.Battler.DestinyBond), ":75");
        assertFalse(b.effects.truthy(PBEffects.Battler.Grudge), ":76");
        assertFalse(b.effects.truthy(PBEffects.Battler.MoveNext), ":77");
        assertEquals(0, b.effects.intVal(PBEffects.Battler.Quash), ":78");
        assertFalse(b.effects.truthy(PBEffects.Battler.ShellTrap), ":79");
    }

    @Test
    @DisplayName("2a: pbBeginTurn ends Encore when the encored move is gone, keeps it when known (Battler_UseMove:81-84)")
    void beginTurnEncoreEndsWhenMoveUnavailable(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler b = hero(data);
        int stunSpore = data.move("STUNSPORE").id;
        int tackle = data.move("TACKLE").id;

        b.effects.set(PBEffects.Battler.Encore, 3);
        b.effects.set(PBEffects.Battler.EncoreMove, stunSpore);
        assertEquals(0, b.pbEncoredMoveIndex(), "PokeBattle_Battler:733-734 slot index of the encored move");
        b.pbBeginTurn(new Object[0]);
        assertEquals(3, b.effects.intVal(PBEffects.Battler.Encore), ":81 still available -> kept");

        b.effects.set(PBEffects.Battler.EncoreMove, tackle);
        assertEquals(-1, b.pbEncoredMoveIndex(), "PokeBattle_Battler:731/737");
        b.pbBeginTurn(new Object[0]);
        assertEquals(0, b.effects.intVal(PBEffects.Battler.Encore), ":82");
        assertEquals(0, b.effects.intVal(PBEffects.Battler.EncoreMove), ":83");
    }

    @Test
    @DisplayName("2a: pbCancelMoves drops the multi-turn state and the usage counters (Battler_UseMove:99-108)")
    void cancelMovesResetsMultiTurnState(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler b = hero(data);
        b.effects.set(PBEffects.Battler.TwoTurnAttack, 5);
        b.effects.set(PBEffects.Battler.Rollout, 2);
        b.effects.set(PBEffects.Battler.Uproar, 2);
        b.effects.set(PBEffects.Battler.Bide, 2);
        b.effects.set(PBEffects.Battler.HyperBeam, 2);
        b.effects.set(PBEffects.Battler.FuryCutter, 2);
        b.effects.set(PBEffects.Battler.BambooSword, 2);
        b.currentMove = 5;
        assertTrue(b.usingMultiTurnAttack(), "PokeBattle_Battler:708-716");

        b.pbCancelMoves();

        assertEquals(0, b.effects.intVal(PBEffects.Battler.TwoTurnAttack), ":99");
        assertEquals(0, b.effects.intVal(PBEffects.Battler.Rollout), ":100");
        assertEquals(0, b.effects.intVal(PBEffects.Battler.Uproar), ":102");
        assertEquals(0, b.effects.intVal(PBEffects.Battler.Bide), ":103");
        assertEquals(0, b.currentMove, ":104");
        assertEquals(0, b.effects.intVal(PBEffects.Battler.FuryCutter), ":106");
        assertEquals(0, b.effects.intVal(PBEffects.Battler.BambooSword), ":107");
        assertEquals(-1, b.effects.intVal(PBEffects.Battler.SuccessiveMove), ":108");
        assertEquals(2, b.effects.intVal(PBEffects.Battler.HyperBeam), ":90 Hyper Beam is NOT cancelled");
    }

    @Test
    @DisplayName("2a: pbCancelMoves confuses an Outrager on its final turn (Battler_UseMove:95-97)")
    void cancelMovesConfusesFinalOutrageTurn(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler b = hero(data);
        b.effects.set(PBEffects.Battler.Outrage, 1);

        b.pbCancelMoves();

        assertTrue(b.effects.intVal(PBEffects.Battler.Confusion) > 0, ":96 pbConfuse");
        assertEquals(0, b.effects.intVal(PBEffects.Battler.Outrage), ":101");
    }

    @Test
    @DisplayName("2a: pbEndTurn stamps lastRoundMoved and clears Charge==1 / GemConsumed (Battler_UseMove:112-126)")
    void endTurnBookkeeping(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler b = hero(data);
        b.effects.set(PBEffects.Battler.Charge, 1);
        b.effects.set(PBEffects.Battler.GemConsumed, 7);

        b.pbEndTurn(new Object[0]);

        assertEquals(b.battle.turnCount(), b.lastRoundMoved, ":112");
        assertEquals(0, b.effects.intVal(PBEffects.Battler.Charge), ":125");
        assertEquals(0, b.effects.intVal(PBEffects.Battler.GemConsumed), ":126");

        b.effects.set(PBEffects.Battler.Charge, 2);
        b.pbEndTurn(new Object[0]);
        assertEquals(2, b.effects.intVal(PBEffects.Battler.Charge), ":125 only the value 1 is cleared");
    }

    @Test
    @DisplayName("2a: pbReducePP spends 1 PP, refuses at 0, and spends nothing mid multi-turn / for foreign moves (Battler_ChangeSelf:141-148)")
    void reducePp(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler b = hero(data);
        BattleMove spore = b.moveSlot(0);
        assertEquals(30, b.moveSlotPp(0));

        assertTrue(b.pbReducePP(spore), ":147");
        assertEquals(29, b.moveSlotPp(0), ":146");

        b.effects.set(PBEffects.Battler.Rollout, 1);
        assertTrue(b.pbReducePP(spore), ":142");
        assertEquals(29, b.moveSlotPp(0), ":142 no PP spent while usingMultiTurnAttack?");
        b.effects.set(PBEffects.Battler.Rollout, 0);

        assertTrue(b.pbReducePP(new BattleMove(data.move("TACKLE"))), ":143 not in the moveset = pp<0 object");
        assertEquals(29, b.moveSlotPp(0));

        b.pbSetPP("STUNSPORE", 0);
        assertFalse(b.pbReducePP(spore), ":145 ran out of PP");
        assertEquals(0, b.moveSlotPp(0));
    }

    @Test
    @DisplayName("2a: pbReducePPOther spends 1 PP when above 0 (Battler_ChangeSelf:150-152)")
    void reducePpOther(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler b = hero(data);
        BattleMove spore = b.moveSlot(0);
        b.pbReducePPOther(spore);
        assertEquals(29, b.moveSlotPp(0), ":151");
        b.pbSetPP("STUNSPORE", 0);
        b.pbReducePPOther(spore);
        assertEquals(0, b.moveSlotPp(0), ":151 never below 0");
    }

    @Test
    @DisplayName("2a: pbHasMove? looks the PBS id up in the moveset (PokeBattle_Battler:551-556)")
    void hasMove(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler b = hero(data);
        assertTrue(b.pbHasMove(data.move("STUNSPORE").id));
        assertFalse(b.pbHasMove(data.move("TACKLE").id));
        assertFalse(b.pbHasMove(0), ":553");
        assertFalse(b.pbHasMove(-1), ":553");
    }
}
