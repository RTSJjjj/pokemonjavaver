package pokemon.runtime.battle;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.*;
import pokemon.runtime.state.Inventory;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/** Triple battles (3v3): positions, near?, opposing order, shifting and a full headless fight. */
class BattleTriplesTest {
    @TempDir Path temp;
    private PbsData data;
    private TrainerState trainer;
    private InteractiveBattlePort port;

    @BeforeEach void setup() {
        data = PbsData.parse(temp.toFile());
        trainer = new TrainerState();
        PbsData.Species s = new PbsData.Species(); s.internalName = "A"; s.name = "测试";
        s.baseStats = new int[] {50, 50, 50, 50, 50, 50}; s.rareness = 255; data.species.put("A", s);
        PbsData.Move m = new PbsData.Move(); m.id = 1; m.internalName = "HIT"; m.name = "撞击"; m.type = "NORMAL";
        m.power = 50; m.pp = 20; m.category = "Physical"; m.function = "000"; m.target = "NearOther"; data.moves.put("HIT", m);
        for (int i = 0; i < 3; i++) trainer.party.add(pokemon(50));
        port = new InteractiveBattlePort(trainer, new Inventory(), () -> data, () -> { }, new Random(1));
    }

    private Pokemon pokemon(int level) {
        Pokemon p = new Pokemon(data.species("A"), level, data);
        p.moves.add(new Pokemon.MoveSlot(data.move("HIT")));
        return p;
    }

    private Battle tripleWild() {
        port.setBattleSize("triple");
        port.freeWildBattle(Arrays.asList(pokemon(5), pokemon(5), pokemon(5)));
        return port.session().battle;
    }

    @Test void aTripleFieldsSixPositions() {
        Battle battle = tripleWild();
        assertEquals(3, battle.pbSideSize(0));
        assertEquals(3, battle.pbSideSize(1));
        assertEquals(6, battle.eachBattler().size);
        assertEquals(5, battle.maxBattlerIndex());
        assertEquals(6, battle.field.positions.length);
    }

    @Test void nearAndOpposingOrderFollowThePluginTables() {
        Battle battle = tripleWild();
        assertFalse(battle.nearBattlers(0, 1), "[0,1] are too far apart in a triple (:553)");
        assertFalse(battle.nearBattlers(4, 5), "[4,5] too (:554)");
        assertTrue(battle.nearBattlers(0, 3));
        assertTrue(battle.nearBattlers(2, 1));
        assertFalse(battle.nearBattlers(2, 2));
        assertArrayEquals(new int[] {5, 3, 1}, battle.pbGetOpposingIndicesInOrder(0));
        assertArrayEquals(new int[] {3, 5, 1}, battle.pbGetOpposingIndicesInOrder(2));
        assertArrayEquals(new int[] {0, 2, 4}, battle.pbGetOpposingIndicesInOrder(5));
    }

    @Test void theMiddleBattlerCannotShiftAndSidesOfThreeCan() {
        Battle battle = tripleWild();
        assertTrue(battle.pbCanShift(0));
        assertFalse(battle.pbCanShift(2), "already in the middle (:14)");
        assertTrue(battle.pbCanShift(4));
    }

    @Test void aLoneBattlerFarFromEveryFoeMovesToTheMiddleAtTheEndOfTheRound() {
        Battle battle = tripleWild();
        for (int idx : new int[] {2, 4}) battle.battlerAt(idx).hp = 0;
        for (int idx : new int[] {3, 5}) battle.battlerAt(idx).hp = 0;
        Battler left = battle.battlerAt(0);
        Battler foe = battle.battlerAt(1);
        battle.pbEORShiftDistantBattlers();                       // Battle_Phase_EndOfRound:152-206
        assertSame(left, battle.battlerAt(2), "the player's battler took the centre position");
        assertSame(foe, battle.battlerAt(3), "so did the foe");
        boolean moved = false;
        for (Battle.RoundEvent e : battle.roundEvents) {
            if (e.kind == Battle.RoundEvent.Kind.MESSAGE && e.text.contains("移动到了中心")) moved = true;
        }
        assertTrue(moved, battle.roundEvents.toString());
    }

    @Test void aTripleWildBattleRunsToAResultHeadless() {
        Battle battle = tripleWild();
        InteractiveBattlePort.Session session = port.session();
        for (int round = 0; round < 60 && session.result == null; round++) {
            for (int idx : session.commandBattlers()) {
                assertNull(session.registerMove(idx, 0));
                int type = session.targetType(idx, 0);
                session.registerTarget(idx, session.firstTarget(idx, type));
            }
            session.startRound();
            session.takeEvents();
            if (session.suspended()) session.answer(null);
        }
        assertNotNull(session.result, "the fight ended");
        assertEquals(BattleResult.Outcome.WIN, session.result.outcome);
    }

    @Test void aBossFightIsThreeAgainstOne() {
        // The project's only triple: setBattleRule("3v1") + a wild boss.
        port.setBattleSize("3v1");
        Pokemon boss = pokemon(30);
        boss.battleRank = 3;
        port.freeWildBattle(boss);
        InteractiveBattlePort.Session session = port.session();
        Battle battle = session.battle;
        assertEquals(3, battle.pbSideSize(0));
        assertEquals(1, battle.pbSideSize(1));
        assertArrayEquals(new int[] {0, 2, 4}, session.commandBattlers());
        assertArrayEquals(new int[] {1}, battle.pbGetOpposingIndicesInOrder(0));
        assertArrayEquals(new int[] {2, 0, 4}, battle.pbGetOpposingIndicesInOrder(1));
        assertTrue(battle.nearBattlers(1, 2));
        assertFalse(battle.nearBattlers(0, 4), "[0,4] (:548)");
        for (int idx : session.commandBattlers()) {
            assertNull(session.registerMove(idx, 0));
            session.registerTarget(idx, 1);
        }
        assertTrue(session.startRound());
        assertEquals(1, battle.turns());
    }

    @Test void aRankThreeBossScoresItsMovesAgainstTheThreeChallengers() {
        // CFRU scoring now drives wild Bosses (rank >= 2) in a 3v1 fight: the boss picks a living challenger, never itself
        port.setBattleSize("3v1");
        Pokemon boss = pokemon(30);
        boss.battleRank = 3;
        port.freeWildBattle(boss);
        Battle battle = port.session().battle;
        Battler foe = battle.battlerAt(1);
        assertTrue(foe.foe && foe.pokemon.battleRank >= 2);
        for (int seed = 0; seed < 20; seed++) {
            AiDoubles.Choice choice = AiDoubles.choose(battle, foe, new Random(seed));
            assertNotNull(choice);
            assertEquals(0, choice.slot);
            assertTrue(choice.target == 0 || choice.target == 2 || choice.target == 4, "target " + choice.target);
        }
    }

    @Test void theTrainerCapForTwoTrainersOnBothSidesStaysAtDouble() {
        // Battle_StartAndEnd:25-28
        Pokemon partner = pokemon(5);
        port.setPartner("", "Friend", Arrays.asList(partner));
        port.setBattleSize("triple");
        PbsData.TrainerData one = new PbsData.TrainerData();
        one.name = "One";
        PbsData.TrainerPokemon member = new PbsData.TrainerPokemon(); member.species = "A"; member.level = 5; one.party.add(member);
        PbsData.TrainerData two = new PbsData.TrainerData();
        two.name = "Two";
        two.party.add(member);
        assertThrows(IllegalStateException.class, () -> port.trainerBattle(Arrays.asList(one, two)));
    }

    @Test void threeOpposingTrainersMakeATripleTrainerBattle() {
        port.setBattleSize("triple");
        java.util.List<PbsData.TrainerData> three = new java.util.ArrayList<>();
        for (String name : new String[] {"One", "Two", "Three"}) {
            PbsData.TrainerData t = new PbsData.TrainerData();
            t.name = name;
            PbsData.TrainerPokemon member = new PbsData.TrainerPokemon(); member.species = "A"; member.level = 5; t.party.add(member);
            three.add(t);
        }
        port.trainerBattle(three);
        InteractiveBattlePort.Session session = port.session();
        assertEquals("One", session.trainerFullname(0));
        assertEquals("Two", session.trainerFullname(1));
        assertEquals("Three", session.trainerFullname(2));
        Battle battle = session.battle;
        assertEquals(3, battle.pbSideSize(1));
        assertEquals(0, battle.battlerAt(1).ownerIndex);
        assertEquals(1, battle.battlerAt(3).ownerIndex);
        assertEquals(2, battle.battlerAt(5).ownerIndex);
        assertEquals("Three", battle.pbGetOwnerName(5));
    }
}
