package pokemon.runtime.battle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.TrainerState;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R14: the wild battle shell - {@code PField_Battles:619-658 pbAfterBattle} and
 * {@code PField_Battles:684-708 Events.onEndBattle}.
 */
class BattleAftermathTest {

    @TempDir
    Path tempDir;

    private PbsData pbs;
    private TrainerState trainer;
    private Random random;

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(tempDir, "pokemon.json", "{\"total\":2,\"byId\":{},\"species\":{"
                + "\"CATERPIE\":{\"id\":10,\"internalName\":\"CATERPIE\",\"name\":\"绿毛虫\","
                + "\"types\":[\"BUG\"],\"baseStats\":[45,30,35,45,20,20],\"rareness\":255,"
                + "\"weight\":2.9,\"genderRate\":\"Female50Percent\",\"evolutions\":[]},"
                + "\"ZACIAN\":{\"id\":888,\"internalName\":\"ZACIAN\",\"name\":\"苍响\","
                + "\"types\":[\"FAIRY\"],\"baseStats\":[92,130,115,138,80,115],\"rareness\":10,"
                + "\"weight\":110.0,\"genderRate\":\"Genderless\",\"evolutions\":[]}}}");
        write(tempDir, "moves.json", "{\"total\":3,\"moves\":{"
                + "\"IRONHEAD\":{\"id\":442,\"internalName\":\"IRONHEAD\",\"name\":\"铁头\","
                + "\"power\":80,\"type\":\"STEEL\",\"category\":\"Physical\",\"pp\":15},"
                + "\"TACKLE\":{\"id\":33,\"internalName\":\"TACKLE\",\"name\":\"撞击\","
                + "\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\",\"pp\":35},"
                + "\"GROWL\":{\"id\":45,\"internalName\":\"GROWL\",\"name\":\"叫声\","
                + "\"power\":0,\"type\":\"NORMAL\",\"category\":\"Status\",\"pp\":40}}}");
        // Pickup's pools must be complete (18 + 11 entries, :773-774), so the
        // fixture carries exactly the plugin's lists.
        StringBuilder items = new StringBuilder();
        for (String name : new String[] {
                "POTION", "ANTIDOTE", "SUPERPOTION", "GREATBALL", "REPEL", "ESCAPEROPE",
                "FULLHEAL", "HYPERPOTION", "ULTRABALL", "REVIVE", "RARECANDY", "SUNSTONE",
                "MOONSTONE", "HEARTSCALE", "FULLRESTORE", "MAXREVIVE", "PPUP", "MAXELIXIR",
                "NUGGET", "KINGSROCK", "ETHER", "IRONBALL", "DESTINYKNOT", "ELIXIR",
                "LEFTOVERS", "HONEY", "POKEBALL"}) {
            if (items.length() > 0) items.append(",");
            items.append("\"").append(name).append("\":{\"id\":1,\"internalName\":\"")
                 .append(name).append("\",\"name\":\"").append(name).append("\",\"pocket\":1}");
        }
        write(tempDir, "items.json", "{\"total\":27,\"items\":{" + items + "}}");
        pbs = PbsData.parse(tempDir.toFile());
        trainer = new TrainerState();
        random = new Random(7);
    }

    private Pokemon pokemon(String species) {
        Pokemon p = new Pokemon(pbs.species(species), 20, pbs);
        p.moves.clear();
        p.moves.add(new Pokemon.MoveSlot(pbs.move("TACKLE")));
        return p;
    }

    @Test
    @DisplayName("pbAfterBattle undoes Mega/Primal and heals the party when canLose")
    void pbAfterBattle() {
        Pokemon hurt = pokemon("CATERPIE");
        hurt.hp = 1;
        trainer.party.add(hurt);

        BattleAftermath.pbAfterBattle(trainer, pbs, win(), false);
        assertEquals(1, hurt.hp, "a loss without canLose does not heal");

        BattleAftermath.pbAfterBattle(trainer, pbs, loss(), true);
        assertEquals(hurt.maxHp(), hurt.hp, "canLose heals the party (:650-655)");
    }

    @Test
    @DisplayName("Zacian's Iron Head PP is topped up after a battle (:624-630)")
    void zacianIronHead() {
        Pokemon zacian = pokemon("ZACIAN");
        zacian.moves.clear();
        Pokemon.MoveSlot ironHead = new Pokemon.MoveSlot(pbs.move("IRONHEAD"));
        ironHead.pp = 1;
        zacian.moves.add(ironHead);
        Pokemon other = pokemon("CATERPIE");
        other.moves.clear();
        Pokemon.MoveSlot tackle = new Pokemon.MoveSlot(pbs.move("TACKLE"));
        tackle.pp = 1;
        other.moves.add(tackle);
        trainer.party.add(zacian);
        trainer.party.add(other);

        BattleAftermath.pbAfterBattle(trainer, pbs, win(), false);
        assertEquals(3, ironHead.pp, "pp < 5 triples (:626-628)");
        assertEquals(1, tackle.pp, "other Pokemon are untouched");

        ironHead.pp = 5;
        BattleAftermath.pbAfterBattle(trainer, pbs, win(), false);
        assertEquals(5, ironHead.pp, "pp >= 5 is left alone");
    }

    @Test
    @DisplayName("onEndBattle: a win or a capture runs Pickup and Honey Gather (:696-700)")
    void pickupAndHoneyGather() {
        Pokemon picker = pokemon("CATERPIE");
        picker.ability = "PICKUP";
        Pokemon gatherer = pokemon("CATERPIE");
        gatherer.ability = "HONEYGATHER";
        gatherer.level = 51;                       // chance 5 + 5*5 = 30 %
        trainer.party.add(picker);
        trainer.party.add(gatherer);

        // A fixed seed that triggers both rolls (the plugin's 10 % / 30 %).
        boolean picked = false;
        boolean gathered = false;
        for (int seed = 0; seed < 200 && !(picked && gathered); seed++) {
            picker.item = null;
            gatherer.item = null;
            Random random = new Random(seed);
            BattleAftermath.onEndBattle(trainer, pbs, random, win(), false);
            picked |= picker.item != null;
            gathered |= "HONEY".equals(gatherer.item);
        }
        assertTrue(picked, "Pickup must fire for some seed");
        assertTrue(gathered, "Honey Gather must fire for some seed");
    }

    @Test
    @DisplayName("Pickup respects the ability, the held item and the egg rule (:736-738)")
    void pickupGuards() {
        Pokemon wrongAbility = pokemon("CATERPIE");
        wrongAbility.ability = "SHIELDDUST";
        assertFalse(BattleAftermath.pickup(pbs, wrongAbility, new Random(1)));

        Pokemon holds = pokemon("CATERPIE");
        holds.ability = "PICKUP";
        holds.item = "POTION";
        assertFalse(BattleAftermath.pickup(pbs, holds, new Random(1)), "already holding something");

        Pokemon egg = pokemon("CATERPIE");
        egg.ability = "PICKUP";
        egg.egg = true;
        assertFalse(BattleAftermath.pickup(pbs, egg, new Random(1)));
    }

    @Test
    @DisplayName("the Pickup pool follows the level band (:776-785)")
    void pickupPoolBand() {
        Pokemon picker = pokemon("CATERPIE");
        picker.ability = "PICKUP";
        // Level 20 -> itemStartIndex 1, so the pool is common[1..9] + rare[1..2]
        // and POTION (index 0) can never come out.
        picker.level = 20;
        for (int seed = 0; seed < 3000; seed++) {
            picker.item = null;
            BattleAftermath.pickup(pbs, picker, new Random(seed));
            assertNotEquals("POTION", picker.item, "POTION left the pool at level 20");
        }
    }

    @Test
    @DisplayName("onEndBattle reports the white-out for a loss without canLose (:701-706)")
    void whiteOutDecision() {
        assertTrue(BattleAftermath.onEndBattle(trainer, pbs, random, loss(), false));
        assertFalse(BattleAftermath.onEndBattle(trainer, pbs, random, loss(), true),
                "canLose suppresses the white-out (:98-99)");
        assertFalse(BattleAftermath.onEndBattle(trainer, pbs, random, win(), false));
        assertFalse(BattleAftermath.onEndBattle(trainer, pbs, random, null, false));
    }

    @Test
    @DisplayName("the escape chance uses unmodified Speed (Battle_Action_Running:134-148)")
    void escapeUsesRawSpeed() {
        Battle battle = new Battle(pbs, new Random(1), null);
        Pokemon slow = pokemon("CATERPIE");
        Pokemon fast = pokemon("ZACIAN");
        battle.addPlayer(slow);
        battle.addFoe(fast);
        Battler player = battle.player();
        Battler foe = battle.foe();

        assertEquals(PokemonStats.stat(fast.baseStat(PokemonStats.SPEED), fast.ivs[PokemonStats.SPEED],
                        fast.evs[PokemonStats.SPEED], fast.level, 1f),
                battle.rawSpeed(foe), "the raw stat, before battle modifiers");
        assertTrue(battle.rawSpeed(foe) > battle.rawSpeed(player));
        assertTrue(battle.canRun(), "a wild battle lets the player run by default");
        assertFalse(battle.setCanRun(false).canRun(), "setBattleRule(\"cannotRun\")");
    }

    @Test
    @DisplayName("pbRun refuses a cannotRun battle with the plugin's line (:74-77)")
    void escapeRefusedWhenCannotRun() {
        InteractiveBattlePort port = new InteractiveBattlePort(trainer,
                new pokemon.runtime.state.Inventory(), () -> pbs, null, new Random(3));
        trainer.party.add(pokemon("CATERPIE"));
        // The rule is consumed when the battle starts (PField_Battles:497-498),
        // so it is set before wildBattle, like prepareWildBattle does.
        port.setCanRun(false);
        port.wildBattle("CATERPIE", 20);
        InteractiveBattlePort.Session session = port.session();
        session.escape();
        assertEquals("逃跑失败了！", session.message, "Battle_Action_Running:75");
        assertNull(session.result, "...and the turn is not spent (:76 return 0)");
        assertEquals(0, session.battle.turns());
    }

    @Test
    @DisplayName("a Ghost type always gets away (Battle_Action_Running:79-84)")
    void ghostAlwaysEscapes() throws Exception {
        write(tempDir, "pokemon.json", "{\"total\":1,\"byId\":{},\"species\":{"
                + "\"GASTLY\":{\"id\":92,\"internalName\":\"GASTLY\",\"name\":\"鬼斯\","
                + "\"types\":[\"GHOST\",\"POISON\"],\"baseStats\":[30,35,30,80,100,35],"
                + "\"rareness\":190,\"weight\":0.1,\"genderRate\":\"Female50Percent\","
                + "\"evolutions\":[]}}}");
        PbsData ghostData = PbsData.parse(tempDir.toFile());
        TrainerState ghostTrainer = new TrainerState();
        Pokemon gastly = new Pokemon(ghostData.species("GASTLY"), 5, ghostData);
        Pokemon slow = new Pokemon(ghostData.species("GASTLY"), 100, ghostData);
        ghostTrainer.party.add(gastly);
        InteractiveBattlePort port = new InteractiveBattlePort(ghostTrainer,
                new pokemon.runtime.state.Inventory(), () -> ghostData, null, new Random(5));
        port.freeWildBattle(slow);
        InteractiveBattlePort.Session session = port.session();
        session.escape();
        assertEquals("安全地逃跑了！", session.message);
        assertEquals(BattleResult.Outcome.ESCAPE, session.result.outcome,
                "the slow Ghost still escapes on the first try");
    }

    @Test
    @DisplayName("a slower runner is never stuck: the +30 bonus guarantees an escape (:135-149)")
    void escapeIsNeverADeadEnd() {
        InteractiveBattlePort port = new InteractiveBattlePort(trainer,
                new pokemon.runtime.state.Inventory(), () -> pbs, null, new Random(9));
        Pokemon slow = pokemon("CATERPIE");
        trainer.party.add(slow);
        // A far faster foe whose only move is harmless (叫声), so the loop is
        // about the escape roll and not about surviving hits.
        Pokemon fast = pokemon("ZACIAN");
        fast.moves.clear();
        fast.moves.add(new Pokemon.MoveSlot(pbs.move("GROWL")));
        port.freeWildBattle(fast);
        InteractiveBattlePort.Session session = port.session();

        int attempts = 0;
        while (session.result == null && attempts < 20) {
            slow.hp = slow.maxHp();            // keep the loop on the escape roll
            session.escape();
            attempts++;
        }
        assertEquals(BattleResult.Outcome.ESCAPE, session.result.outcome,
                "the wild battle must always let the player out");
        // rate = speed*128/foe + 30*attempts, so 10 failures already exceed 256.
        assertTrue(attempts <= 10, "escaped after " + attempts + " attempts");
    }

    private static BattleResult win() {
        return new BattleResult(BattleResult.Outcome.WIN, 3, null);
    }

    private static BattleResult loss() {
        return new BattleResult(BattleResult.Outcome.LOSS, 3, null);
    }
}
