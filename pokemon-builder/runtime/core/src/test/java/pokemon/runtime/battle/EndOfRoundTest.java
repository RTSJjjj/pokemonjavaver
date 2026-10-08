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
 * R16: {@code pbEndOfRoundPhase}'s status damage (Battle_Phase_EndOfRound:390-436)
 * and the {@code NEWEST_BATTLE_MECHANICS} numbers that go with it
 * (Settings:160): the burn fraction, the paralysis Speed penalty and the
 * critical hit table.
 */
class EndOfRoundTest {

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

    private Battle battle(Pokemon player, Pokemon foe) {
        Battle battle = new Battle(pbs, new Random(11), (user, target, moves) -> 0);
        battle.addPlayer(player);
        battle.addFoe(foe);
        return battle;
    }

    @Test
    @DisplayName("end of round damage only reaches the Pokemon on the field (:391-436)")
    void onlyFieldedBattlersTakeStatusDamage() {
        Pokemon active = pokemon("TANK", 50);
        Pokemon benched = pokemon("TANK", 50);
        benched.hp = benched.maxHp() / 2;
        Battle battle = battle(active, pokemon("TANK", 50));
        // The bench shares the party but never the field.
        Battler benchedBattler = new Battler(benched, false);
        battle.playerParty().add(benchedBattler);
        // Statuses live on the battler while the battle runs (pbEffect sets them
        // there), so the fixture sets them there too.
        battle.player().status = "POISON";
        benchedBattler.status = "POISON";
        int benchHp = benched.hp;

        battle.step();

        assertTrue(active.hp < active.maxHp(), "the fielded Pokemon takes the damage");
        assertEquals(benchHp, benched.hp, "the bench keeps its HP");
        assertEquals(1, battle.endOfRoundMessages.size);
        assertTrue(battle.endOfRoundMessages.first().contains("因为中毒受到了伤害！"),
                battle.endOfRoundMessages.toString());
    }

    @Test
    @DisplayName("burn takes 1/16 with NEWEST_BATTLE_MECHANICS (:430)")
    void burnTakesOneSixteenth() {
        Pokemon player = pokemon("TANK", 50);
        Battle battle = battle(player, pokemon("TANK", 50));
        battle.player().status = "BURN";
        int maxHp = player.maxHp();

        battle.step();

        assertEquals(maxHp - Math.max(1, maxHp / 16), player.hp, "1/16, not 1/8");
        assertTrue(battle.endOfRoundMessages.first().contains("因为灼伤受到了伤害！"));
    }

    @Test
    @DisplayName("a Boss (battleRank > 2) takes 1/40 instead (:427-428)")
    void bossBurnTakesOneFortieth() {
        Pokemon player = pokemon("TANK", 50);
        player.battleRank = 3;
        Battle battle = battle(player, pokemon("TANK", 50));
        battle.player().status = "BURN";
        // Battler_Initialize:44-45/78-79: a rank-3 Boss fights with BOSS_HP_RANK[3] = 5 times its HP
        int maxHp = player.maxHp() * 5;
        assertEquals(maxHp, battle.player().maxHp());

        battle.step();

        assertEquals(maxHp - Math.max(1, maxHp / 40), player.hp);
    }

    @Test
    @DisplayName("poison is 1/8, badly poisoned n/16 with the counter capped at 15 (:394-415)")
    void poisonDamage() {
        Pokemon player = pokemon("TANK", 50);
        Battle battle = battle(player, pokemon("TANK", 50));
        Battler fielded = battle.player();
        fielded.status = "POISON";
        int maxHp = player.maxHp();

        battle.step();
        assertEquals(maxHp - Math.max(1, maxHp / 8), player.hp, "regular poison");

        // The engine works on the battler's HP, which syncs back to the Pokemon.
        fielded.hp = maxHp;
        fielded.setStatusCount(1);             // badly poisoned: statusCount>0 (:394), the counter is PBEffects::Toxic
        fielded.effects.set(PBEffects.Battler.Toxic, 1);
        battle.step();
        assertEquals(2, fielded.effects.intVal(PBEffects.Battler.Toxic), "the counter grows before the damage (:395)");
        assertEquals(maxHp - Math.max(1, maxHp * 2 / 16), player.hp, "n/16 with n = 2");

        fielded.effects.set(PBEffects.Battler.Toxic, 15);
        fielded.hp = maxHp;
        battle.step();
        assertEquals(15, fielded.effects.intVal(PBEffects.Battler.Toxic), "the counter is capped at 15 (:396)");
        assertEquals(maxHp - Math.max(1, maxHp * 15 / 16), player.hp);
    }

    @Test
    @DisplayName("the end of round sweeps run fastest first (pbPriority(true), :253-260)")
    void endOfRoundRunsFastestFirst() {
        Pokemon slow = pokemon("TANK", 50);
        Pokemon quick = pokemon("FAST", 50);
        Battle battle = battle(slow, quick);
        battle.player().status = "POISON";
        battle.foe().status = "POISON";

        battle.step();

        assertEquals(2, battle.endOfRoundMessages.size);
        assertTrue(battle.endOfRoundMessages.first().startsWith("野生的" + quick.name),
                "the faster Pokemon's line comes first: " + battle.endOfRoundMessages);
    }

    @Test
    @DisplayName("paralysis halves Speed with NEWEST_BATTLE_MECHANICS (PokeBattle_Battler:268-271)")
    void paralysisHalvesSpeed() {
        Pokemon player = pokemon("TANK", 50);
        Battler battler = new Battler(player, false);
        int healthy = battler.speed();
        battler.status = "PARALYSIS";
        assertEquals(Math.max(1, healthy / 2), battler.speed());
        assertNotEquals(healthy / 4, battler.speed(), "not the pre-Newest quarter");
    }

    @Test
    @DisplayName("a critical hit ignores the stat stages (Move_Usage_Calculations:266-277)")
    void criticalIgnoresStatStages() {
        Pokemon attacker = pokemon("FAST", 50);
        Pokemon defender = pokemon("TANK", 50);
        Battler user = new Battler(attacker, false);
        Battler target = new Battler(defender, true);

        // §4: DamageCalc now runs the plugin's pbCalcDamage / pbCalcDamageMultipliers,
        // which read @battle for weather, abilities, items and side effects. These
        // tests exercise the formula in isolation, so they attach a battle (a real
        // battle is what the plugin always has at this point).
        Battle readBattle = battle(attacker, defender);   // the file's own helper (:68)
        user.battle = readBattle;
        target.battle = readBattle;
        BattleMove move = new BattleMove(pbs.move("SLASH"));

        int plain = DamageCalc.compute(user, target, move, pbs, new Random(5), false);
        user.stages[0] = -2;                   // the attacker's Attack is lowered
        target.stages[1] = 2;                  // the defender's Defense is raised
        int lowered = DamageCalc.compute(user, target, move, pbs, new Random(5), false);
        int critical = DamageCalc.compute(user, target, move, pbs, new Random(5), true);
        assertTrue(lowered < plain, "the stages do matter without a crit");
        assertEquals(Math.round(plain * 1.5f), critical,
                "a crit uses neutral stages, so it is the plain damage times 1.5");
    }

    @Test
    @DisplayName("the critical table is [24,8,2,1] with the high-crit flag and Focus Energy (:198-228)")
    void criticalTable() {
        Battle battle = battle(pokemon("TANK", 50), pokemon("TANK", 50));
        Battler user = battle.player();
        Battler target = battle.foe();
        BattleMove plain = new BattleMove(pbs.move("GROWL"));
        BattleMove highCrit = new BattleMove(pbs.move("SLASH"));

        assertEquals(1.0 / 24, criticalRate(battle, user, target, plain), 0.004,
                "the base rate is 1/24, not the pre-Newest 1/16");
        assertEquals(1.0 / 8, criticalRate(battle, user, target, highCrit), 0.006,
                "the moves.txt \"h\" flag adds a step");
        user.effects.set(PBEffects.Battler.FocusEnergy, 1);       // Focus Energy (Move_Usage_Calculations:224 c += FocusEnergy)
        assertEquals(1.0 / 8, criticalRate(battle, user, target, plain), 0.006,
                "Focus Energy adds a step too");
        assertEquals(0.5, criticalRate(battle, user, target, highCrit), 0.02,
                "flag + Focus Energy reaches the 1/2 step");
    }

    /** The observed critical rate over many rolls. */
    private double criticalRate(Battle battle, Battler user, Battler target, BattleMove move) {
        pokemon.runtime.battle.movefx.MoveEffect fx = pokemon.runtime.battle.movefx.MoveEffectRegistry.of(move.function());
        int hits = 0;
        int rolls = 60000;
        for (int i = 0; i < rolls; i++) {
            if (fx.pbIsCritical(move, user, target)) {
                hits++;
            }
        }
        return (double) hits / rolls;
    }

    @Test
    @DisplayName("a fainted Pokemon is skipped by both sweeps (:392, :425)")
    void faintedBattlersAreSkipped() {
        Pokemon player = pokemon("TANK", 50);
        Battle battle = battle(player, pokemon("TANK", 50));
        battle.player().status = "POISON";
        battle.player().hp = 0;
        battle.step();
        assertEquals(0, player.hp, "no damage line for a fainted battler");
        assertEquals(0, battle.endOfRoundMessages.size);
    }

    @Test
    @DisplayName("the round leaves no end-of-round lines when nothing is wrong")
    void quietRound() {
        Pokemon player = pokemon("TANK", 50);
        Battle battle = battle(player, pokemon("TANK", 50));
        Array<String> lines = battle.endOfRoundMessages;
        int hp = player.hp;
        battle.step();
        assertEquals(0, lines.size);
        assertEquals(hp, player.hp);
    }
}
