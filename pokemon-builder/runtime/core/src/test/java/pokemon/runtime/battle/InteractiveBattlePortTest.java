package pokemon.runtime.battle;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.*;
import pokemon.runtime.state.Inventory;
import java.nio.file.Path;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class InteractiveBattlePortTest {
    @TempDir Path temp;
    private PbsData data;
    private TrainerState trainer;
    private Inventory bag;
    private InteractiveBattlePort port;
    private int whiteouts;
    @BeforeEach void setup() {
        data = PbsData.parse(temp.toFile()); trainer = new TrainerState(); bag = new Inventory();
        PbsData.Species s = new PbsData.Species(); s.internalName = "A"; s.name = "测试";
        s.baseStats = new int[] {50, 50, 50, 50, 50, 50}; s.rareness = 255; data.species.put("A", s);
        PbsData.Move m = new PbsData.Move(); m.id = 1; m.internalName = "HIT"; m.name = "撞击"; m.type = "NORMAL";
        m.power = 50; m.pp = 20; m.category = "Physical"; m.function = "000"; m.target = "NearOther"; data.moves.put("HIT", m);
        // R13: a Poké Ball is recognised by its ITEM_TYPE (PItem_Items:89-92),
        // so the fixture needs the item itself.
        PbsData.Item ball = new PbsData.Item();
        ball.id = 4; ball.internalName = "MASTERBALL"; ball.name = "大师球";
        ball.pocket = 3; ball.type = 3;
        data.items.put("MASTERBALL", ball);
        trainer.party.add(pokemon(20));
        port = new InteractiveBattlePort(trainer, bag, () -> data, () -> whiteouts++, new Random(1));
    }
    private Pokemon pokemon(int level) {
        Pokemon p = new Pokemon(data.species("A"), level, data); p.moves.add(new Pokemon.MoveSlot(data.move("HIT"))); return p;
    }
    @Test void startsPendingWithoutAutoBattlingAndConsumesOneTurnAtATime() {
        Pokemon foe = pokemon(20); int hp = foe.hp; port.freeWildBattle(foe);
        assertTrue(port.pending()); assertEquals(hp, foe.hp); assertTrue(trainer.seen.contains("A"));
        assertTrue(port.session().chooseMove(0)); assertEquals(1, port.session().battle.turns());
        assertTrue(foe.hp < hp); assertEquals(19, trainer.first().moves.first().pp);
        assertTrue(port.pending());
    }
    @Test void captureAddsOwnedAndWaitsForAcknowledgement() {
        bag.add("MASTERBALL", 1); Pokemon foe = pokemon(10); port.freeWildBattle(foe);
        assertTrue(port.session().item("MASTERBALL", 0));
        assertEquals(BattleResult.Outcome.CAUGHT, port.session().result.outcome);
        assertTrue(trainer.party.contains(foe)); assertTrue(trainer.owned.contains("A")); assertEquals(0, bag.count("MASTERBALL"));
        assertTrue(port.pending()); port.finish(); assertFalse(port.pending());
    }
    @Test void switchingUsesATurnAndProtectsFaintedOrActiveTargets() {
        trainer.party.add(pokemon(20)); port.freeWildBattle(pokemon(20));
        // pbCanSwitchLax (Battle_Action_Switching:29-33) refuses the battler that
        // is already on the field.
        assertNotNull(port.session().registerSwitch(0));
        assertNull(port.session().registerSwitch(1));
        // :124-125 the choice is registered, so the round is spent on the switch.
        assertTrue(port.session().battle.choiceIsSwitch(0));
        assertEquals(1, port.session().battle.choiceSwitchParty(0));
        // :313 pbReplace moves the field slot, then the round's foe turn runs.
        assertTrue(port.session().battle.replace(0, 1));
        assertSame(trainer.party.get(1), port.session().battle.player().pokemon);
        port.session().foeTurn();
        assertEquals(1, port.session().battle.turns());
    }
    @Test void bothSidesMovesAreAnnounced() {
        // Battler_UseMove:290 pbDisplayBrief("{1}使用{2}！") runs for whichever
        // side acts, so a round between two able Pokemon reports both lines, in
        // the order they moved.
        port.freeWildBattle(pokemon(20));
        assertTrue(port.session().chooseMove(0));
        String message = port.session().message;
        int uses = message.split("使用", -1).length - 1;
        assertEquals(2, uses, message);
        assertTrue(message.startsWith("\u6d4b\u8bd5\u4f7f\u7528") || message.contains("野生的"),
                "the lines name the battlers with pbThis: " + message);
    }
    @Test void lossWhiteoutRunsOnlyOnAcknowledgement() {
        trainer.first().hp = 1; port.freeWildBattle(pokemon(100));
        port.session().chooseMove(0); assertEquals(BattleResult.Outcome.LOSS, port.session().result.outcome);
        assertEquals(0, whiteouts); port.finish(); assertEquals(1, whiteouts); port.finish(); assertEquals(1, whiteouts);
    }
    @Test void trainerBattleRejectsCaptureWithoutConsumingBall() {
        PbsData.TrainerData opponent = new PbsData.TrainerData();
        PbsData.TrainerPokemon member = new PbsData.TrainerPokemon(); member.species = "A"; member.level = 10; opponent.party.add(member);
        bag.add("MASTERBALL", 1); port.trainerBattle(opponent);
        // PokeBattle_BattleCommon:100-103: the trainer bats the ball away, so the
        // throw is reported but the ball is not spent.
        assertTrue(port.session().item("MASTERBALL", 0));
        assertEquals(1, bag.count("MASTERBALL"));
        assertTrue(port.session().message.contains("训练家打飞了球"));
        assertNull(port.session().result);
        port.session().escape(); assertNull(port.session().result); assertEquals(0, port.session().battle.turns());
    }
    @Test void canLoseSuppressesWhiteoutAndResetsForTheNextBattle() {
        trainer.first().hp = 1;
        port.setCanLose(true);
        port.freeWildBattle(pokemon(100));
        port.session().chooseMove(0);
        port.finish();
        assertEquals(0, whiteouts, "canLose skips the white-out");
        assertEquals(BattleResult.Outcome.LOSS, port.lastResult().outcome);

        trainer.first().hp = 1;
        port.freeWildBattle(pokemon(100));
        assertNull(port.lastResult(), "the next battle resets the remembered result");
        port.session().chooseMove(0);
        port.finish();
        assertEquals(1, whiteouts, "the rule does not leak into the next battle");
    }
    @Test void faintedLeadStopsTheEngineOnEachSceneCallOfPbEORSwitch() {
        // Battle_Action_Switching:165-239 + :256-262: the engine asks the screen, one call at a time, and carries on
        // from the same place with the answer.
        trainer.party.add(pokemon(20));
        trainer.first().hp = 1;
        port.freeWildBattle(pokemon(100));
        InteractiveBattlePort.Session session = port.session();

        assertTrue(session.chooseMove(0));
        assertTrue(session.suspended(), "the lead fainted: pbEORSwitch asks whether to switch (:221)");
        assertNull(session.result);
        assertEquals(Battle.SceneCall.Kind.CONFIRM, session.request().kind);
        assertTrue(session.takeEvents().size > 0, "the round's lines are there to be played first");

        session.answer(true);                                       // :221 yes
        assertEquals(Battle.SceneCall.Kind.PARTY_SCREEN, session.request().kind);   // :228 -> :251 pbSwitchInBetween
        assertNull(session.request().validator.apply(1), "the block accepts party slot 1 (:140-148)");
        session.answer(null);                                       // the party screen accepted party slot 1

        assertEquals(Battle.SceneCall.Kind.SHOW_PARTY_LINEUP, session.request().kind);   // :259 (a fainted one is not recalled, :257)
        session.answer(null);
        assertEquals(Battle.SceneCall.Kind.SEND_OUT, session.request().kind);             // :326
        assertEquals(1, session.battle.playerFieldIndex(), "pbReplace (:313) ran before the send-out");
        session.answer(null);

        assertFalse(session.suspended());
        assertNull(session.result);
        assertSame(trainer.party.get(1), session.battle.player().pokemon);
    }
    @Test void uTurnInTheMiddleOfAMoveAsksTheScreenAndTheRoundGoesOn() {
        // Move_Effects_080-0FF:3284-3309 pbEndOfMoveUsageEffect -> pbGetReplacementPokemonIndex -> pbRecallAndReplace:
        // the move's effect is stopped on the party screen and carries on with the answer.
        PbsData.Move uturn = new PbsData.Move(); uturn.id = 2; uturn.internalName = "UTURN"; uturn.name = "急速折返";
        uturn.type = "BUG"; uturn.power = 70; uturn.pp = 20; uturn.category = "Physical"; uturn.function = "0EE";
        uturn.target = "NearOther"; data.moves.put("UTURN", uturn);
        trainer.first().moves.clear();
        trainer.first().moves.add(new Pokemon.MoveSlot(uturn));
        trainer.party.add(pokemon(20));
        port.freeWildBattle(pokemon(20));
        InteractiveBattlePort.Session session = port.session();
        Battler lead = session.battle.player();

        assertTrue(session.chooseMove(0));
        assertTrue(session.suspended(), "U-turn asks which Pokemon comes in (:3302)");
        assertEquals(Battle.SceneCall.Kind.PARTY_SCREEN, session.request().kind);
        assertNull(session.request().validator.apply(1));
        session.answer(null);
        while (session.suspended()) {                                 // recall (:257), lineup (:259), send-out (:326)
            session.answer(null);
        }

        assertEquals(1, session.battle.playerFieldIndex());
        assertNotSame(lead, session.battle.player());
        assertEquals(1, session.battle.turns(), "the round went on after the switch");
    }
}
