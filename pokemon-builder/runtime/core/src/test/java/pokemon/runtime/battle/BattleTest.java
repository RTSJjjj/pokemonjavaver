package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.TrainerState;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 3 / P2: the headless battle core (damage formula, turn order, faint,
 * experience). Fixed seeds keep the outcomes reproducible.
 */
class BattleTest {

    @Test
    @DisplayName("P2: the damage formula matches floor((2*L/5+2)*P*A/D/50)+2 with STAB")
    void damageFormula(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler hero = new Battler(new Pokemon(data.species("HERO"), 50, data), false);
        Battler foe = new Battler(new Pokemon(data.species("FOE"), 2, data), true);
        BattleMove slash = new BattleMove(data.move("SLASH"));

        // 648 base * 1.5 STAB * [0.85,1.0] random => 826..972.
        for (int seed = 0; seed < 20; seed++) {
            int damage = DamageCalc.compute(hero, foe, slash, data, new Random(seed));
            assertTrue(damage >= 826 && damage <= 972, "damage " + damage);
        }
    }

    @Test
    @DisplayName("P2: a type immunity deals no damage")
    void immunity(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        // FOE is NORMAL; a GHOST move is neutral here, but NORMAL vs a GHOST
        // defender is immune per the synthetic type chart.
        PbsData.Species ghostSpecies = data.species("FOE");
        Battler ghost = new Battler(new Pokemon(ghostSpecies, 5, data), true);
        // Give the defender the GHOST type via a form-less trick: use a species
        // with GHOST; the synthetic chart has SPOOK for that.
        Battler spook = new Battler(new Pokemon(data.species("SPOOK"), 5, data), true);
        Battler hero = new Battler(new Pokemon(data.species("HERO"), 50, data), false);
        BattleMove slash = new BattleMove(data.move("SLASH"));
        assertEquals(0, DamageCalc.compute(hero, spook, slash, data, new Random(1)));
        assertTrue(DamageCalc.compute(hero, ghost, slash, data, new Random(1)) > 0);
    }

    @Test
    @DisplayName("P2: the stronger side wins and gains experience")
    void battleWinAndExperience(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 50, data);
        Pokemon foe = new Pokemon(data.species("FOE"), 2, data);
        int expBefore = hero.exp;

        Battle battle = new Battle(data, new Random(7), null)
                .addPlayer(hero)
                .addFoe(foe);
        BattleResult result = battle.run(100);

        assertEquals(BattleResult.Outcome.WIN, result.outcome);
        assertTrue(result.turns >= 1);
        assertEquals(0, foe.hp, "the foe fainted and its HP persisted");
        assertTrue(hero.hp > 0);
        assertTrue(hero.exp > expBefore, "the winner gained experience");
    }

    @Test
    @DisplayName("P2: a high-experience foe levels the winner up")
    void levelUp(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 5, data);
        Pokemon foe = new Pokemon(data.species("BIGEXP"), 2, data);
        int levelBefore = hero.level;

        BattleResult result = new Battle(data, new Random(3), null)
                .addPlayer(hero).addFoe(foe).run(100);

        assertEquals(BattleResult.Outcome.WIN, result.outcome);
        assertTrue(hero.level > levelBefore, "level " + levelBefore + " -> " + hero.level);
        assertEquals(hero.maxHp(), hero.hp, "a level-up refills HP");
    }

    @Test
    @DisplayName("P2: losing is reported and every battler's HP is written back")
    void battleLoss(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon weak = new Pokemon(data.species("FOE"), 2, data);
        Pokemon strong = new Pokemon(data.species("HERO"), 50, data);

        BattleResult result = new Battle(data, new Random(11), null)
                .addPlayer(weak).addFoe(strong).run(100);

        assertEquals(BattleResult.Outcome.LOSS, result.outcome);
        assertEquals(0, weak.hp);
        assertTrue(strong.hp > 0);
    }

    @Test
    @DisplayName("P2: experience thresholds keep the level in sync")
    void experienceThresholds(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 5, data);
        assertEquals(5, hero.level);
        int toNext = hero.experienceToNextLevel();
        assertTrue(toNext > 0);
        assertFalse(hero.gainExperience(toNext - 1), "not enough yet");
        assertEquals(5, hero.level);
        assertTrue(hero.gainExperience(toNext), "the threshold levels up");
        assertEquals(6, hero.level);
        assertTrue(hero.exp >= PokemonStats.experienceForLevel("Medium", 6));
        assertEquals(PokemonStats.levelForExperience("Medium", hero.exp), hero.level);
    }

    @Test
    @DisplayName("P2: the headless port runs a wild battle and white-outs on a loss")
    void headlessPort(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));

        TrainerState winner = new TrainerState();
        winner.party.add(new Pokemon(data.species("HERO"), 50, data));
        java.util.concurrent.atomic.AtomicBoolean whited =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        HeadlessBattlePort port = new HeadlessBattlePort(winner, () -> data,
                () -> whited.set(true), new Random(5));
        BattleResult result = port.wildBattle("FOE", 2);
        assertEquals(BattleResult.Outcome.WIN, result.outcome);
        assertFalse(whited.get(), "no white-out after a win");

        TrainerState loser = new TrainerState();
        loser.party.add(new Pokemon(data.species("FOE"), 2, data));
        HeadlessBattlePort losing = new HeadlessBattlePort(loser, () -> data,
                () -> whited.set(true), new Random(5));
        BattleResult loss = losing.wildBattle("HERO", 50);
        assertEquals(BattleResult.Outcome.LOSS, loss.outcome);
        assertTrue(whited.get(), "a loss triggers the white-out callback");

        assertNull(port.wildBattle("MISSINGNO", 5), "an unknown species starts no battle");
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    /** HERO (strong), FOE (weak), BIGEXP (huge baseExp), SPOOK (GHOST). */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":4,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[100,100,100,100,100,100],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[\"OVERGROW\"],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[1,1,1,1,1,1],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"BIGEXP\":{\"id\":3,\"internalName\":\"BIGEXP\",\"name\":\"Bigexp\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[1,1,1,1,1,1],"
                + "\"growthRate\":\"Medium\",\"baseExp\":100000,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"SPOOK\":{\"id\":4,\"internalName\":\"SPOOK\",\"name\":\"Spook\","
                + "\"types\":[\"GHOST\"],\"baseStats\":[50,50,50,50,50,50],"
                + "\"growthRate\":\"Medium\",\"baseExp\":50,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]}}}");
        write(root, "moves.json", "{\"total\":2,\"moves\":{"
                + "\"SLASH\":{\"id\":1,\"internalName\":\"SLASH\",\"name\":\"Slash\","
                + "\"power\":70,\"type\":\"NORMAL\",\"category\":\"Physical\",\"accuracy\":100,\"pp\":20},"
                + "\"SHADOW\":{\"id\":2,\"internalName\":\"SHADOW\",\"name\":\"Shadow\","
                + "\"power\":70,\"type\":\"GHOST\",\"category\":\"Physical\",\"accuracy\":100,\"pp\":15}}}");
        write(root, "abilities.json", "{\"total\":1,\"abilities\":{"
                + "\"OVERGROW\":{\"id\":65,\"internalName\":\"OVERGROW\",\"name\":\"Overgrow\"}}}");
        write(root, "types.json", "{\"total\":2,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"GHOST\":{\"id\":7,\"internalName\":\"GHOST\",\"name\":\"Ghost\",\"immunities\":[\"NORMAL\"]}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }
}
