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
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B1: {@code pbStartBattleSendOut} (Battle_StartAndEnd:194-275) transcribed into
 * a straight-line step list by {@link BattleSendOut}.
 */
class BattleSendOutTest {

    @TempDir
    Path tempDir;

    private PbsData pbs;

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(tempDir, "pokemon.json", "{\"total\":3,\"byId\":{},\"species\":{"
                + "\"BULBASAUR\":{\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"妙蛙种子\","
                + "\"types\":[\"GRASS\"],\"baseStats\":[45,49,49,45,65,65],\"rareness\":45,"
                + "\"weight\":6.9,\"genderRate\":\"Female50Percent\",\"evolutions\":[]},"
                + "\"IVYSAUR\":{\"id\":2,\"internalName\":\"IVYSAUR\",\"name\":\"妙蛙草\","
                + "\"types\":[\"GRASS\"],\"baseStats\":[60,62,63,60,80,80],\"rareness\":45,"
                + "\"weight\":13.0,\"genderRate\":\"Female50Percent\",\"evolutions\":[]},"
                + "\"VENUSAUR\":{\"id\":3,\"internalName\":\"VENUSAUR\",\"name\":\"妙蛙花\","
                + "\"types\":[\"GRASS\"],\"baseStats\":[80,82,83,80,100,100],\"rareness\":45,"
                + "\"weight\":100.0,\"genderRate\":\"Female50Percent\",\"evolutions\":[]}}}");
        write(tempDir, "moves.json", "{\"total\":1,\"moves\":{"
                + "\"TACKLE\":{\"id\":33,\"internalName\":\"TACKLE\",\"name\":\"撞击\","
                + "\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\",\"pp\":35,\"target\":\"NearOther\"}}}");
        pbs = PbsData.parse(tempDir.toFile());
    }

    private Pokemon pokemon(String species) {
        Pokemon p = new Pokemon(pbs.species(species), 20, pbs);
        p.moves.clear();
        p.moves.add(new Pokemon.MoveSlot(pbs.move("TACKLE")));
        return p;
    }

    private Battle battle(Pokemon... foes) {
        Battle battle = new Battle(pbs, new Random(1), (user, foe, moves) -> 0);
        battle.addPlayer(pokemon("BULBASAUR"));
        for (Pokemon foe : foes) {
            battle.addFoe(foe);
        }
        return battle;
    }

    private static java.util.function.IntPredicate noSwitches() {
        return id -> false;
    }

    @Test
    @DisplayName("a wild battle opens with the wild line, then 去吧！and one send-out (:196-208, :253-273)")
    void wildSingle() {
        Battle battle = battle(pokemon("IVYSAUR"));
        List<BattleSendOut.Step> plan = BattleSendOut.plan(battle, false, "", 0, noSwitches());

        assertEquals(3, plan.size());
        assertEquals(BattleSendOut.Step.Kind.DISPLAY_PAUSED, plan.get(0).kind);
        assertEquals("哦！一只野生的妙蛙草出现了！", plan.get(0).message);          // :208
        assertEquals(BattleSendOut.Step.Kind.DISPLAY_BRIEF, plan.get(1).kind);
        assertEquals("去吧！\n妙蛙种子！", plan.get(1).message);                     // :258 (@battlers[0])
        assertEquals(BattleSendOut.Step.Kind.SEND_OUT, plan.get(2).kind);
        assertArrayEquals(new int[] { 0 }, plan.get(2).battlers, "the player's slot only");
    }

    @Test
    @DisplayName("a Boss wild battle prefixes 特殊的/强大的 and its capture line (:200-206)")
    void wildBoss() {
        Pokemon boss = pokemon("IVYSAUR");
        boss.battleRank = 2;
        List<BattleSendOut.Step> plan = BattleSendOut.plan(battle(boss), false, "", 0, noSwitches());
        assertEquals("哦！特殊的妙蛙草出现了！\n看来不打倒它是无法捕捉的！", plan.get(0).message);   // :205

        boss.battleRank = 3;
        plan = BattleSendOut.plan(battle(boss), false, "", 0, noSwitches());
        assertEquals("哦！强大的妙蛙草出现了！\n看来不打倒它是无法捕捉的！", plan.get(0).message);

        // $game_switches[196] switches the second line (:202-203).
        plan = BattleSendOut.plan(battle(boss), false, "", 0, id -> id == 196);
        assertEquals("哦！强大的妙蛙草出现了！\n无论如何都是无法捕捉的！", plan.get(0).message);
    }

    @Test
    @DisplayName("two and three wild Pokemon use the 和 / 、 wording (:210-215)")
    void wildMultiple() {
        List<BattleSendOut.Step> plan = BattleSendOut.plan(
                battle(pokemon("BULBASAUR"), pokemon("IVYSAUR")), false, "", 0, noSwitches());
        assertEquals("野生的妙蛙种子和妙蛙草\n出现了！", plan.get(0).message);        // :211

        plan = BattleSendOut.plan(battle(pokemon("BULBASAUR"), pokemon("IVYSAUR"),
                pokemon("VENUSAUR")), false, "", 0, noSwitches());
        assertEquals("野生的妙蛙种子、妙蛙草、妙蛙花\n出现了！", plan.get(0).message);   // :214
    }

    @Test
    @DisplayName("a trainer battle sends the opponent's side out first (:217-227, :230-273)")
    void trainerBattle() {
        Battle battle = battle(pokemon("IVYSAUR"));
        List<BattleSendOut.Step> plan = BattleSendOut.plan(battle, true, "南晓", 1, noSwitches());

        assertEquals(5, plan.size());
        assertEquals("南晓\n向你发起挑战！", plan.get(0).message);                    // :220
        assertEquals(BattleSendOut.Step.Kind.DISPLAY_BRIEF, plan.get(1).kind);
        assertEquals("南晓派出了\n妙蛙草！", plan.get(1).message);                    // :242
        assertArrayEquals(new int[] { 1 }, plan.get(2).battlers, "the foe's slot first");
        assertEquals("去吧！\n妙蛙种子！", plan.get(3).message);                      // :258
        assertArrayEquals(new int[] { 0 }, plan.get(4).battlers, "then the player's");
    }
    @Test
    @DisplayName("two opposing trainers and a partner: one line and one send-out each, the partner's before the player's (:222-223, :236-266)")
    void doubleTrainerBattle() {
        Battle battle = new Battle(pbs, new Random(1), (user, foe, moves) -> 0);
        battle.addPlayer(pokemon("BULBASAUR"));
        battle.addPartner(pokemon("VENUSAUR"));
        battle.addFoe(pokemon("IVYSAUR"));
        battle.addFoeSecondTrainer(pokemon("VENUSAUR"));
        battle.setBattleMode("double");
        List<BattleSendOut.Step> plan = BattleSendOut.plan(battle, true, new String[] { "南晓", "北辰" },
                new String[] { null, "小伙伴" }, noSwitches());

        assertEquals("南晓、北辰\n向你发起挑战！", plan.get(0).message);                      // :222-223
        assertEquals("南晓派出了\n妙蛙草！\r\n北辰派出了\n妙蛙花！", plan.get(1).message);        // :242 per trainer, :238 joined
        assertArrayEquals(new int[] { 1, 3 }, plan.get(2).battlers, "the opposing side first");
        assertEquals("小伙伴派出了\n妙蛙花！\r\n去吧！\n妙蛙种子！", plan.get(3).message);       // :237 the player's line is last
        assertArrayEquals(new int[] { 2, 0 }, plan.get(4).battlers, "the partner's Pokemon before the player's");
    }

    @Test
    @DisplayName("a wild battle never fills the opposing side (Battle_StartAndEnd:120-129/231)")
    void wildSkipsSideOne() {
        List<BattleSendOut.Step> plan = BattleSendOut.plan(battle(pokemon("IVYSAUR")), false, "", 0,
                noSwitches());
        long sendOuts = plan.stream().filter(s -> s.kind == BattleSendOut.Step.Kind.SEND_OUT).count();
        assertEquals(1, sendOuts, "only the player's send-out runs");
    }

    @Test
    @DisplayName("the battler slots follow pbSetUpSides: player 0, foe 1 (Battle_StartAndEnd:122/180)")
    void fieldSlots() {
        Battle battle = battle(pokemon("IVYSAUR"));
        assertEquals(0, battle.player().index);
        assertEquals(1, battle.foe().index);
        assertSame(battle.player(), BattleSendOut.battler(battle, 0));
        assertSame(battle.foe(), BattleSendOut.battler(battle, 1));
        assertNull(BattleSendOut.battler(battle, 3), "no third field slot in singles");
    }
}
