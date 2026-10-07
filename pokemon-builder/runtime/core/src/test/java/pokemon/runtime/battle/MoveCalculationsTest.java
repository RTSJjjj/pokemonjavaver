package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 5 / 2c2: {@code pbAccuracyCheck} / {@code pbCalcAccuracyModifiers}
 * (Move_Usage_Calculations:112-183) and {@code pbIsCritical?} (:195-229).
 */
class MoveCalculationsTest {

    private static void write(Path root, String name, String content) throws Exception {
        Path pbs = root.resolve("pbs");
        Files.createDirectories(pbs);
        Files.write(pbs.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    /** TACKLE (acc 100), HONE (status, acc 0 = never misses), HIGHCRIT (flag h). */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":2,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        write(root, "moves.json", "{\"total\":3,\"moves\":{"
                + "\"TACKLE\":{\"id\":1,\"internalName\":\"TACKLE\",\"name\":\"Tackle\","
                + "\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"flags\":\"a\",\"target\":\"NearOther\"},"
                + "\"SWIFT\":{\"id\":2,\"internalName\":\"SWIFT\",\"name\":\"Swift\","
                + "\"function\":\"000\",\"power\":60,\"type\":\"NORMAL\",\"category\":\"Special\","
                + "\"accuracy\":0,\"pp\":20,\"flags\":\"\",\"target\":\"NearOther\"},"
                + "\"SLASH\":{\"id\":3,\"internalName\":\"SLASH\",\"name\":\"Slash\","
                + "\"function\":\"000\",\"power\":70,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":20,\"flags\":\"ah\",\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(root, "types.json", "{\"total\":1,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }

    private static Battle battleOf(PbsData data, long seed) {
        Battle battle = new Battle(data, new Random(seed), (user, target, moves) -> 0)
                .addPlayer(new Pokemon(data.species("HERO"), 20, data))
                .addFoe(new Pokemon(data.species("FOE"), 20, data));
        battle.player().initEffects(false);
        battle.foe().initEffects(false);
        return battle;
    }

    private static double hitRate(Battle battle, BattleMove move, int trials) {
        MoveEffect fx = MoveEffectRegistry.of(move.function());
        int hits = 0;
        for (int i = 0; i < trials; i++) {
            if (fx.pbAccuracyCheck(move, battle.player(), battle.foe())) hits++;
        }
        return hits / (double) trials;
    }

    @Test
    @DisplayName("2c2: 100% accuracy at neutral stages always hits; accuracy 0 (Swift) never misses (:117-118/128/140)")
    void accuracyBasics(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data, 1);
        BattleMove tackle = new BattleMove(data.move("TACKLE"));
        BattleMove swift = new BattleMove(data.move("SWIFT"));

        assertEquals(1.0, hitRate(battle, tackle, 500), ":140 100*100/100 = 100");
        battle.player().setStage(PBStats.ACCURACY, -6);
        assertEquals(1.0, hitRate(battle, swift, 500), ":118 baseAcc==0 always hits");
    }

    @Test
    @DisplayName("2c2: accuracy -6 stages hits ~33% (100*3/9), Gravity lifts it to ~55%, evasion +6 lowers it (:130-140/170-171)")
    void accuracyStages(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data, 7);
        BattleMove tackle = new BattleMove(data.move("TACKLE"));
        battle.player().setStage(PBStats.ACCURACY, -6);

        double base = hitRate(battle, tackle, 4000);
        assertEquals(0.33, base, 0.04, ":134 33% (accuracy 33 / evasion 100)");
        assertTrue(battle.player().effects.truthy(PBEffects.Battler.BlunderPolicy), ":141 a miss arms Blunder Policy");

        battle.field.effects.set(PBEffects.Field.Gravity, 5);
        double gravity = hitRate(battle, tackle, 4000);
        assertEquals(0.55, gravity, 0.04, ":171 x5/3 -> accuracy 55");

        battle.field.effects.set(PBEffects.Field.Gravity, 0);
        battle.player().setStage(PBStats.ACCURACY, 0);
        battle.foe().setStage(PBStats.EVASION, 6);
        assertEquals(0.33, hitRate(battle, tackle, 4000), 0.04, ":135 evasion 300 -> 100*100/300");
    }

    @Test
    @DisplayName("2c2: Telekinesis always hits; Foresight/Miracle Eye cancel a raised evasion; Micle Berry is spent (:115/173-178)")
    void accuracySpecialCases(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data, 3);
        BattleMove tackle = new BattleMove(data.move("TACKLE"));
        battle.foe().setStage(PBStats.EVASION, 6);

        battle.foe().effects.set(PBEffects.Battler.Telekinesis, 3);
        assertEquals(1.0, hitRate(battle, tackle, 300), ":115");
        battle.foe().effects.set(PBEffects.Battler.Telekinesis, 0);

        battle.foe().effects.set(PBEffects.Battler.Foresight, true);
        assertEquals(1.0, hitRate(battle, tackle, 300), ":177 evasion stage reset to 0");
        battle.foe().effects.set(PBEffects.Battler.Foresight, false);

        battle.foe().setStage(PBStats.EVASION, 0);
        battle.player().effects.set(PBEffects.Battler.MicleBerry, true);
        float[] mods = {100, 0, 0, 1f, 1f};
        MoveEffectRegistry.of("000").pbCalcAccuracyModifiers(tackle, battle.player(), battle.foe(), mods);
        assertEquals(1.2f, mods[BattleHandlers.ACC_MULT], 1e-5, ":175");
        assertFalse(battle.player().effects.truthy(PBEffects.Battler.MicleBerry), ":174");
    }

    @Test
    @DisplayName("2c2: Fog rounds the accuracy multiplier x3/5 to a whole number (:179-181)")
    void accuracyFog(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data, 3);
        BattleMove tackle = new BattleMove(data.move("TACKLE"));
        battle.field.weather = PBWeather.Fog;
        float[] mods = {100, 0, 0, 1f, 1f};
        MoveEffectRegistry.of("000").pbCalcAccuracyModifiers(tackle, battle.player(), battle.foe(), mods);
        assertEquals(1f, mods[BattleHandlers.ACC_MULT], ":181 (1.0*3/5).round = 1");
    }

    @Test
    @DisplayName("2c2: critical hits - Lucky Chant, Laser Focus, and the ratio table (:196-228)")
    void critical(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data, 11);
        Battler hero = battle.player();
        Battler foe = battle.foe();
        BattleMove tackle = new BattleMove(data.move("TACKLE"));
        BattleMove slash = new BattleMove(data.move("SLASH"));   // flag h
        MoveEffect fx = MoveEffectRegistry.of("000");

        int crits = 0;
        for (int i = 0; i < 4800; i++) if (fx.pbIsCritical(tackle, hero, foe)) crits++;
        assertEquals(200, crits, 60, ":198/:228 one in 24");

        foe.pbOwnSide().effects.set(PBEffects.Side.LuckyChant, 5);
        assertFalse(fx.pbIsCritical(tackle, hero, foe), ":196");
        foe.pbOwnSide().effects.set(PBEffects.Side.LuckyChant, 0);

        hero.effects.set(PBEffects.Battler.LaserFocus, 2);
        assertTrue(fx.pbIsCritical(tackle, hero, foe), ":222");
        hero.effects.set(PBEffects.Battler.LaserFocus, 0);

        hero.effects.set(PBEffects.Battler.FocusEnergy, 3);       // c = 3 -> ratio 1
        assertTrue(fx.pbIsCritical(tackle, hero, foe), ":224/:226/:228 ratios[3] == 1 always");
        hero.effects.set(PBEffects.Battler.FocusEnergy, 0);

        crits = 0;
        for (int i = 0; i < 4800; i++) if (fx.pbIsCritical(slash, hero, foe)) crits++;
        assertEquals(600, crits, 120, ":223 high critical rate -> one in 8");
    }

    @Test
    @DisplayName("2c2: confusion damage can never be a critical hit (pbCritialOverride -1, :216-219)")
    void confusionNeverCrits(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data, 5);
        battle.player().effects.set(PBEffects.Battler.FocusEnergy, 3);
        MoveEffect confusion = MoveEffectRegistry.confusion();
        BattleMove move = MoveEffectRegistry.confusionMove();
        for (int i = 0; i < 200; i++) {
            assertFalse(confusion.pbIsCritical(move, battle.player(), battle.player()));
        }
    }
}
