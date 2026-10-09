package pokemon.runtime.battle;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.*;
import pokemon.runtime.state.FieldGlobals;
import pokemon.runtime.state.Inventory;

import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 160_PokeBattle_SafariZone / 242_PBattle_Safari: the Safari Zone's battle and visit state. */
class SafariBattleTest {
    @TempDir Path temp;
    private PbsData data;
    private TrainerState trainer;
    private InteractiveBattlePort port;
    private int balls = 30;

    @BeforeEach void setup() {
        data = PbsData.parse(temp.toFile());
        trainer = new TrainerState();
        trainer.name = "小测";
        PbsData.Species easy = new PbsData.Species(); easy.internalName = "A"; easy.name = "好抓";
        easy.baseStats = new int[] {50, 50, 50, 50, 50, 50}; easy.rareness = 255; data.species.put("A", easy);
        PbsData.Species hard = new PbsData.Species(); hard.internalName = "B"; hard.name = "难抓";
        hard.baseStats = new int[] {50, 50, 50, 50, 50, 50}; hard.rareness = 3; data.species.put("B", hard);
        PbsData.Move m = new PbsData.Move(); m.id = 1; m.internalName = "HIT"; m.name = "撞击"; m.type = "NORMAL";
        m.power = 50; m.pp = 20; m.category = "Physical"; m.function = "000"; m.target = "NearOther"; data.moves.put("HIT", m);
        PbsData.Item ball = new PbsData.Item();
        ball.id = 849; ball.internalName = "WILDERNESSBALL"; ball.name = "狩猎球"; ball.pocket = 3; ball.type = 3;
        data.items.put("WILDERNESSBALL", ball);
        trainer.party.add(pokemon("A", 20));
        port = new InteractiveBattlePort(trainer, new Inventory(), () -> data, () -> { }, new Random(1));
        port.setSafariSource(() -> balls);
    }

    private Pokemon pokemon(String species, int level) {
        Pokemon p = new Pokemon(data.species(species), level, data);
        p.moves.add(new Pokemon.MoveSlot(data.move("HIT")));
        return p;
    }

    private boolean has(com.badlogic.gdx.utils.Array<Battle.RoundEvent> events, Battle.RoundEvent.Kind kind) {
        for (Battle.RoundEvent event : events) if (event.kind == kind) return true;
        return false;
    }

    @Test @DisplayName("a wild battle inside the Zone is a Safari battle with the player's balls")
    void safariSessionCarriesTheBalls() {
        port.freeWildBattle(pokemon("A", 10));
        assertTrue(port.session().safari);
        assertEquals(30, port.session().ballCount);
        assertEquals("野生的好抓出现了！", port.session().safariAppearMessage());
    }

    @Test @DisplayName("a Safari battle starts without any able Pokemon (the override runs first)")
    void needsNoAblePokemon() {
        trainer.party.first().hp = 0;
        port.freeWildBattle(pokemon("A", 10));
        assertTrue(port.pending());
        port.session().safariCommand(0);
        assertNotNull(port.session().result);
    }

    @Test @DisplayName("without the Zone the battle is an ordinary one")
    void ordinaryBattleOutsideTheZone() {
        port.setSafariSource(() -> -1);
        port.freeWildBattle(pokemon("A", 10));
        assertFalse(port.session().safari);
    }

    @Test @DisplayName("Run ends the battle with decision 3 and plays the flee sound")
    void run() {
        port.freeWildBattle(pokemon("A", 10));
        port.session().safariCommand(3);
        assertEquals(BattleResult.Outcome.ESCAPE, port.session().result.outcome);
        com.badlogic.gdx.utils.Array<Battle.RoundEvent> events = port.session().takeEvents();
        assertTrue(has(events, Battle.RoundEvent.Kind.SE));
        assertEquals("安全地逃跑了！", events.get(1).text);
        port.session().storeCaught();
        port.finish();
        assertEquals(30, port.lastSafariBalls(), "a flight spends no ball");
    }

    @Test @DisplayName("Bait and Rock play their animations and the round goes on")
    void baitAndRock() {
        port.freeWildBattle(pokemon("A", 10));
        port.session().safariCommand(1);
        com.badlogic.gdx.utils.Array<Battle.RoundEvent> bait = port.session().takeEvents();
        assertEquals("小测向好抓丢了一些诱饵！", bait.first().text);
        assertTrue(has(bait, Battle.RoundEvent.Kind.SAFARI_BAIT));
        if (port.session().result == null) {
            port.session().safariCommand(2);
            assertTrue(has(port.session().takeEvents(), Battle.RoundEvent.Kind.SAFARI_ROCK));
        }
    }

    @Test @DisplayName("a Ball spends one, and a sure catch ends the battle with the Pokemon")
    void ballCatchesAnEasyPokemon() {
        Pokemon wild = pokemon("A", 10);
        port.freeWildBattle(wild);
        port.session().safariCommand(0);
        assertEquals(29, port.session().ballCount);
        assertEquals(BattleResult.Outcome.CAUGHT, port.session().result.outcome);
        assertSame(wild, port.session().result.caught);
        com.badlogic.gdx.utils.Array<Battle.RoundEvent> events = port.session().takeEvents();
        Battle.RoundEvent thrown = null;
        for (Battle.RoundEvent event : events) if (event.kind == Battle.RoundEvent.Kind.BALL_THROW) thrown = event;
        assertNotNull(thrown);
        assertTrue(thrown.ball.showTrainer, "the Safari throw shows the player");
        assertTrue(has(events, Battle.RoundEvent.Kind.BALL_SUCCESS));
        port.session().storeCaught();
        assertTrue(trainer.party.contains(wild));
        port.finish();
        assertEquals(29, port.lastSafariBalls());
    }

    @Test @DisplayName("the last ball missing ends the visit with decision 2")
    void lastBallMissed() {
        balls = 1;
        port.freeWildBattle(pokemon("B", 10));
        port.session().safariCommand(0);
        assertEquals(0, port.session().ballCount);
        BattleResult result = port.session().result;
        assertNotNull(result);
        if (result.outcome == BattleResult.Outcome.ESCAPE) assertEquals(2, result.decision);
    }

    @Test @DisplayName("SafariState: steps, reception map and the Zone's maps")
    void safariState() {
        FieldGlobals globals = new FieldGlobals();
        globals.safariMapOf = id -> id == 28 || id == 30;
        assertFalse(globals.inSafari(28), "no visit in progress");
        globals.safari.begin(27, 5, 6, 8, 30);
        assertEquals(1800, globals.safari.steps);
        assertTrue(globals.inSafari(27), "the reception map");
        assertTrue(globals.inSafari(30), "a Safari map");
        assertFalse(globals.inSafari(12));
        globals.safari.end();
        assertFalse(globals.inSafari(27));
        assertEquals(0, globals.safari.ballcount);
    }
}
