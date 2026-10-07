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
    private PbsData.TrainerData trainerOf(String name) {
        PbsData.TrainerData opponent = new PbsData.TrainerData();
        opponent.name = name;
        PbsData.TrainerPokemon member = new PbsData.TrainerPokemon(); member.species = "A"; member.level = 10; opponent.party.add(member);
        return opponent;
    }

    @Test void doubleWildBattleFieldsTwoOfEachSide() {
        trainer.party.add(pokemon(20));
        port.setBattleSize("double");
        port.freeWildBattle(java.util.Arrays.asList(pokemon(20), pokemon(20)));
        Battle battle = port.session().battle;
        assertEquals(2, battle.pbSideSize(0));
        assertEquals(2, battle.pbSideSize(1));
        assertEquals(4, battle.eachBattler().size);
        assertTrue(battle.battlerAt(2).pbOwnedByPlayer());
    }

    @Test void aDoubleWithOnePlayerPokemonShrinksTheSideLikePbEnsureParticipants() {
        // Battle_StartAndEnd:44-99: the player side cannot be filled, so it loses a position; the wild side keeps its size.
        port.setBattleSize("double");
        port.freeWildBattle(java.util.Arrays.asList(pokemon(20), pokemon(20)));
        Battle battle = port.session().battle;
        assertEquals(1, battle.pbSideSize(0));
        assertEquals(2, battle.pbSideSize(1));
        assertEquals(3, battle.eachBattler().size);
    }

    @Test void twoOpposingTrainersAndAPartnerMakeTwoVsTwo() {
        // pbTrainerBattleCore:428-487: the partner's party follows the player's, the second trainer's follows the first's.
        port.setPartner("", "Friend", java.util.Arrays.asList(pokemon(20)));
        port.setBattleSize("double");
        port.trainerBattle(java.util.Arrays.asList(trainerOf("One"), trainerOf("Two")));
        InteractiveBattlePort.Session session = port.session();
        Battle battle = session.battle;
        assertEquals(2, battle.pbSideSize(0));
        assertEquals(2, battle.pbSideSize(1));
        assertFalse(battle.battlerAt(2).pbOwnedByPlayer(), "the partner's Pokemon stands at position 2");
        assertEquals(1, battle.battlerAt(3).ownerIndex, "the second opponent stands at position 3");
        assertEquals("One", session.trainerFullname);
        assertEquals("Two", session.trainerFullname2);
        assertEquals("Friend", session.partnerFullname);
    }

    @Test void noPartnerRuleKeepsThePartnerOut() {
        port.setPartner("", "Friend", java.util.Arrays.asList(pokemon(20)));
        port.setNoPartner(true);
        port.freeWildBattle(java.util.Arrays.asList(pokemon(20), pokemon(20)));
        assertNull(port.session().partnerFullname);
        assertEquals(1, port.session().battle.pbSideSize(0));
    }
    @Test void aDoubleBattleTakesACommandPerPlayerBattlerAndRunsOneRound() {
        // Battle_Phase_Command:197-263: each of the player's battlers chooses a move (and a target, :87-88), then the round runs.
        trainer.party.add(pokemon(20));
        port.setBattleSize("double");
        Pokemon foeA = pokemon(15);
        Pokemon foeB = pokemon(15);
        port.freeWildBattle(java.util.Arrays.asList(foeA, foeB));
        InteractiveBattlePort.Session session = port.session();
        Battle battle = session.battle;

        assertArrayEquals(new int[] {0, 2}, session.commandBattlers());
        assertTrue(session.needsTargetChoice());
        assertNull(session.registerMove(0, 0));
        int type = session.targetType(0, 0);
        assertEquals(PBTargets.NearOther, type);
        String[] texts = session.targetTexts(0, type);
        assertNull(texts[0], "a battler is not near itself");
        assertNotNull(texts[1]);
        assertNotNull(texts[2], "NearOther includes the ally (Scene_Commands:385)");
        assertNotNull(texts[3]);
        assertEquals(3, session.firstTarget(0, type), "the most opposite foe comes first (:408)");
        session.registerTarget(0, 3);
        assertNull(session.registerMove(2, 0));
        session.registerTarget(2, 1);
        assertEquals(java.util.Arrays.asList(), java.util.Arrays.asList(), "no command is left");
        int hp1 = battle.battlerAt(1).hp, hp3 = battle.battlerAt(3).hp;

        session.foeTurn();

        assertTrue(battle.battlerAt(1).hp < hp1, "battler 2 hit position 1");
        assertTrue(battle.battlerAt(3).hp < hp3, "battler 0 hit position 3");
        assertEquals(1, battle.turns());
    }

    @Test void startRoundRunsADoubleRoundOnceEveryBattlerHasItsCommand() {
        trainer.party.add(pokemon(20));
        port.setBattleSize("double");
        port.freeWildBattle(java.util.Arrays.asList(pokemon(15), pokemon(15)));
        InteractiveBattlePort.Session session = port.session();
        Battle battle = session.battle;
        assertNull(session.registerMove(0, 0));
        session.registerTarget(0, 1);
        assertNull(session.registerMove(2, 0));
        session.registerTarget(2, 3);
        int hp1 = battle.battlerAt(1).hp, hp3 = battle.battlerAt(3).hp;

        assertTrue(session.startRound());

        assertTrue(battle.battlerAt(1).hp < hp1);
        assertTrue(battle.battlerAt(3).hp < hp3);
        assertEquals(1, battle.turns());
        assertTrue(session.takeEvents().size > 0, "the round's lines are handed to the screen");
    }

    @Test void twoSwitchersOfOneRoundShareTheTurnAndComeInPriorityOrder() {
        // Battle_Phase_Attack:50-71 pbAttackPhaseSwitch walks pbPriority; the round's turn count and pbChooseAll run once.
        trainer.party.add(pokemon(20));
        trainer.party.add(pokemon(20));
        trainer.party.add(pokemon(20));
        port.setBattleSize("double");
        port.freeWildBattle(java.util.Arrays.asList(pokemon(15), pokemon(15)));
        InteractiveBattlePort.Session session = port.session();
        Battle battle = session.battle;
        assertNull(session.registerSwitch(0, 2));
        assertNull(session.registerSwitch(2, 3));
        assertTrue(session.switching(0) && session.switching(2));

        session.beginSwitchPhase();
        int[] order = session.switchOrder();
        assertEquals(2, order.length);
        assertEquals(2, session.switchParty(0));
        assertEquals(3, session.switchParty(2));
        session.pursuitOnSwitch(order[0]);
        session.pursuitOnSwitch(order[1]);
        assertEquals(1, battle.turns(), "the turn is counted once however many battlers switch");
        for (int idx : order) {
            assertTrue(battle.replace(idx, session.switchParty(idx)));
        }
        session.foeTurn();
        assertEquals(1, battle.turns());
    }

    @Test void anItemUsedInADoubleSpendsOnlyThatBattlersActionAndKeepsTheRoundOpen() {
        PbsData.Item potion = new PbsData.Item();
        potion.id = 5; potion.internalName = "SUPERPOTION"; potion.name = "好伤药"; potion.pocket = 2; potion.battleUse = 1;
        data.items.put("SUPERPOTION", potion);
        bag.add("SUPERPOTION", 1);
        trainer.party.add(pokemon(20));
        port.setBattleSize("double");
        port.freeWildBattle(java.util.Arrays.asList(pokemon(15), pokemon(15)));
        InteractiveBattlePort.Session session = port.session();
        Battle battle = session.battle;
        battle.battlerAt(0).hp = 5;
        battle.battlerAt(0).syncHp();

        assertTrue(session.commandItem("SUPERPOTION", 0, 0, -1));

        assertEquals(":UseItem", battle.choices(0)[0]);
        assertEquals(0, battle.turns(), "the round has not run");
        assertEquals(0, bag.count("SUPERPOTION"));
        assertArrayEquals(new int[] {2}, session.commandBattlers(), "the other battler still chooses");
    }

    @Test void everyPositionOfADoubleHasItsOwnActivePosition() {
        // Battle_StartAndEnd:110 @positions[idxBattler]: a hurt battler at position 2 enters (pbActivateHealingWish reads it).
        trainer.party.add(pokemon(20));
        trainer.party.get(1).hp = 1;
        port.setBattleSize("double");
        port.freeWildBattle(java.util.Arrays.asList(pokemon(15), pokemon(15)));
        InteractiveBattlePort.Session session = port.session();
        assertEquals(4, session.battle.field.positions.length);
        session.onActiveAll();
        assertNull(session.result);
    }

    @Test void aFaintedPartnerIsReplacedByTheEndOfRoundSwitchAndTheLinesNameItsOwner() {
        // Battle_Action_Switching:178-204: the partner (not owned by the player) refills its position itself.
        port.setPartner("", "Friend", java.util.Arrays.asList(pokemon(20), pokemon(20)));
        port.setBattleSize("double");
        port.trainerBattle(java.util.Arrays.asList(trainerOf("One"), trainerOf("Two")));
        InteractiveBattlePort.Session session = port.session();
        Battle battle = session.battle;
        Battler partner = battle.battlerAt(2);
        assertFalse(partner.pbOwnedByPlayer());
        partner.hp = 0;
        partner.syncHp();
        battle.pbEORSwitch(false);
        assertNotSame(partner, battle.battlerAt(2), "the partner's second Pokemon took the position");
        assertFalse(battle.battlerAt(2).fainted());
        assertTrue(session.recallMessage(2).contains("Friend"), "pbMessageOnRecall names the partner as the owner");
        assertTrue(session.replaceMessage(2, 1).startsWith("Friend派出了"), "pbMessagesOnReplace's trainer branch");
    }

    @Test void partyStartsMarkWhereEachTrainersTeamBegins() {
        port.setPartner("", "Friend", java.util.Arrays.asList(pokemon(20)));
        port.setBattleSize("double");
        port.trainerBattle(java.util.Arrays.asList(trainerOf("One"), trainerOf("Two")));
        Battle battle = port.session().battle;
        assertArrayEquals(new int[] {0, 1}, battle.partyStarts(0));
        assertArrayEquals(new int[] {0, 1}, battle.partyStarts(1));
    }

    @Test void goingBackClearsThePreviousBattlersChoice() {
        trainer.party.add(pokemon(20));
        port.setBattleSize("double");
        port.freeWildBattle(java.util.Arrays.asList(pokemon(20), pokemon(20)));
        InteractiveBattlePort.Session session = port.session();
        assertNull(session.registerMove(0, 0));
        assertEquals(":UseMove", session.battle.choices(0)[0]);
        session.cancelChoice(0);                                   // Battle_Phase_Command:252
        assertEquals(":None", session.battle.choices(0)[0]);
        assertArrayEquals(new int[] {0, 2}, session.commandBattlers());
    }
}
