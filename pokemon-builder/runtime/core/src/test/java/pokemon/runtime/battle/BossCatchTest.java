package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
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
 * PokeBattle_BOSS:157-187 {@code pbCatchBossPokemon}: a Boss with no HP left is not defeated; the player may
 * throw a Poke Ball at it (PokeBattle_BattleCommon:68-164 {@code pbThrowPokeBall}), otherwise it runs away.
 */
class BossCatchTest {

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
        write(tempDir, "pokemon.json", "{\"total\":2,\"byId\":{},\"species\":{"
                + "\"TANK\":{\"id\":1,\"internalName\":\"TANK\",\"name\":\"坦克\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[200,50,50,50,50,50],\"rareness\":45,"
                + "\"weight\":10.0,\"genderRate\":\"Female50Percent\","
                + "\"moves\":[{\"level\":1,\"move\":\"GROWL\"}],\"evolutions\":[]},"
                + "\"FAST\":{\"id\":2,\"internalName\":\"FAST\",\"name\":\"快\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[200,50,50,200,50,50],\"rareness\":45,"
                + "\"weight\":10.0,\"genderRate\":\"Female50Percent\","
                + "\"moves\":[{\"level\":1,\"move\":\"GROWL\"}],\"evolutions\":[]}}}");
        // A status move: it deals no damage, so only the end of round moves HP.
        write(tempDir, "moves.json", "{\"total\":2,\"moves\":{"
                + "\"GROWL\":{\"id\":45,\"internalName\":\"GROWL\",\"name\":\"叫声\","
                + "\"function\":\"001\",\"power\":0,\"type\":\"NORMAL\",\"category\":\"Status\",\"pp\":40,\"target\":\"NearOther\"},"
                + "\"SLASH\":{\"id\":163,\"internalName\":\"SLASH\",\"name\":\"劈开\","
                + "\"function\":\"000\",\"power\":70,\"type\":\"NORMAL\",\"category\":\"Physical\",\"pp\":20,"
                + "\"flags\":\"h\"}}}");
        write(tempDir, "items.json", "{\"total\":0,\"items\":{}}");
        write(tempDir, "natures.json", "{\"total\":2,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"},"
                + "{\"id\":1,\"internalName\":\"LONELY\",\"name\":\"Lonely\","
                + "\"statUp\":\"ATTACK\",\"statDown\":\"DEFENSE\"}]}");
        pbs = PbsData.parse(tempDir.toFile());
    }

    private Pokemon pokemon(String species, int level) {
        return new Pokemon(pbs.species(species), level, pbs);
    }

    /** What the player answers to the Boss's questions: whether to throw, and which ball. */
    private static final class Answers implements Battle.Scene {
        boolean confirm;
        String ball;
        final java.util.List<String> asked = new java.util.ArrayList<>();

        @Override public void pbPartyScreen(int idxBattler, boolean canCancel, java.util.function.IntFunction<String> block) { }
        @Override public boolean pbDisplayConfirmMessage(String msg) { asked.add(msg); return confirm; }
        @Override public void pbRecall(int idxBattler) { }
        @Override public void pbShowPartyLineup(int side) { }
        @Override public void pbSendOutBattlers(int[] idxBattlers, boolean startBattle) { }
        @Override public String pbChooseBallFromBag() { return ball; }
    }

    private static final class Hooks implements Battle.CaptureHooks {
        int shakes = 4;
        int rarenessSeen = -2;
        final java.util.List<String> deleted = new java.util.ArrayList<>();
        @Override public String playerName() { return "玩家"; }
        @Override public String itemName(String ball) { return "精灵球"; }
        @Override public int ballType(String ball) { return 0; }
        @Override public void deleteItem(String ball) { deleted.add(ball); }
        @Override public int pbCaptureCalc(Battler target, String ball, int rareness) { rarenessSeen = rareness; return shakes; }
        @Override public void onCatch(String ball, Pokemon pkmn) { }
    }

    private Battle bossBattle(int rank, Answers answers, Hooks hooks) {
        Pokemon boss = pokemon("TANK", 50);
        boss.battleRank = rank;
        Battle battle = new Battle(pbs, new Random(11), (user, target, moves) -> 0);
        battle.addPlayer(pokemon("TANK", 50));
        battle.addFoe(boss);
        battle.scene = answers;
        battle.captureHooks = hooks;
        return battle;
    }

    @Test
    @DisplayName("BOSS_HP_RANK[battleRank] scales the battler's total HP and HP (Battler_Initialize:44-45/78-79)")
    void hpIsMultiplied() {
        Battle battle = bossBattle(4, new Answers(), new Hooks());
        Pokemon boss = battle.foe().pokemon;
        assertEquals(boss.maxHp() * 10, battle.foe().maxHp());
        assertEquals(boss.hp * 10, battle.foe().hp);
    }

    @Test
    @DisplayName("a Boss at 0 HP asks, takes the chosen ball, goes to 1 HP and is thrown at with rareness 255 (:166-176)")
    void captureFlow() {
        Answers answers = new Answers();
        answers.confirm = true;
        answers.ball = "POKEBALL";
        Hooks hooks = new Hooks();
        Battle battle = bossBattle(3, answers, hooks);
        Battler boss = battle.foe();
        boss.setHp(0);

        boss.pbFaint();

        assertEquals(1, answers.asked.size());
        assertTrue(answers.asked.get(0).endsWith("现在很虚弱！\n要扔出球捕捉吗？"));
        assertEquals(java.util.List.of("POKEBALL"), hooks.deleted);          // :173
        assertEquals(255, hooks.rarenessSeen);                                // :176
        assertEquals(1, boss.pokemon.hp);                                     // :175
        assertEquals(4, battle.decision);                                     // PokeBattle_BattleCommon:143
        assertEquals(1, battle.caughtPokemon.size());
        assertSame(boss.pokemon, battle.caughtPokemon.get(0));
        BattleResult result = battle.result();
        assertEquals(BattleResult.Outcome.CAUGHT, result.outcome);
        assertSame(boss.pokemon, result.caught);
        assertEquals(1, boss.pokemon.hp);                                     // the write-back leaves the caught Pokemon alone
    }

    @Test
    @DisplayName("declining the question (or backing out of the Bag) lets the Boss run away: decision 1 (:178-185)")
    void declined() {
        Answers answers = new Answers();
        answers.confirm = false;
        Battle battle = bossBattle(3, answers, new Hooks());
        battle.foe().setHp(0);
        battle.foe().pbFaint();
        assertEquals(1, battle.decision);
        assertTrue(battle.caughtPokemon.isEmpty());

        answers = new Answers();
        answers.confirm = true;
        answers.ball = null;
        battle = bossBattle(3, answers, new Hooks());
        battle.foe().setHp(0);
        battle.foe().pbFaint();
        assertEquals(1, battle.decision);
        assertTrue(battle.caughtPokemon.isEmpty());
    }

    @Test
    @DisplayName("with $game_switches[196] the Boss cannot be caught and runs away without a question (:158-162)")
    void uncatchable() {
        Answers answers = new Answers();
        answers.confirm = true;
        Battle battle = bossBattle(3, answers, new Hooks());
        battle.gameSwitches = id -> id == 196;
        battle.foe().setHp(0);
        battle.foe().pbFaint();
        assertTrue(answers.asked.isEmpty());
        assertEquals(1, battle.decision);
    }

    @Test
    @DisplayName("a ball that shakes out leaves the Boss at 1 HP and the battle undecided (:116-128)")
    void ballFailed() {
        Answers answers = new Answers();
        answers.confirm = true;
        answers.ball = "POKEBALL";
        Hooks hooks = new Hooks();
        hooks.shakes = 2;
        Battle battle = bossBattle(3, answers, hooks);
        battle.foe().setHp(0);
        battle.foe().pbFaint();
        assertEquals(0, battle.decision);
        assertEquals(1, battle.foe().hp);
        assertTrue(battle.roundMessages.contains("好可惜...\n差一点就能成功了", false));
    }
}
