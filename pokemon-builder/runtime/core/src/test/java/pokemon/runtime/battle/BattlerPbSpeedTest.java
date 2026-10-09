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
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 109_PokeBattle_Battler:250-279 {@code pbSpeed}: the speed the battle info screen shows. */
class BattlerPbSpeedTest {

    @TempDir
    Path tempDir;
    private Battle battle;
    private Battler player;

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(tempDir, "pokemon.json", "{\"total\":1,\"byId\":{},\"species\":{"
                + "\"BULBASAUR\":{\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"妙蛙种子\","
                + "\"types\":[\"GRASS\"],\"baseStats\":[45,49,49,45,65,65],\"rareness\":45,"
                + "\"weight\":6.9,\"genderRate\":\"Female50Percent\",\"evolutions\":[]}}}");
        write(tempDir, "moves.json", "{\"total\":1,\"moves\":{\"TACKLE\":{\"id\":33,\"internalName\":\"TACKLE\","
                + "\"name\":\"撞击\",\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"effectChance\":0,\"target\":\"NearOther\",\"priority\":0,\"flags\":\"\"}}}");
        PbsData pbs = PbsData.parse(tempDir.toFile());
        battle = new Battle(pbs, new Random(7), (user, foe, moves) -> 0);
        battle.addPlayer(new Pokemon(pbs.species("BULBASAUR"), 50, pbs));
        battle.addFoe(new Pokemon(pbs.species("BULBASAUR"), 50, pbs));
        player = battle.battlerAt(0);
    }

    @Test
    @DisplayName("without modifiers the speed is the stat; a stage and paralysis scale it; Tailwind doubles it")
    void stagesParalysisAndTailwind() {
        int raw = player.baseSpeed();
        assertEquals(raw, player.pbSpeed(0));
        player.setStage(PBStats.SPEED, 2);
        assertEquals(Math.max(1, raw * 4 / 2), player.pbSpeed(0));          // +2 = 4/2
        player.setStage(PBStats.SPEED, 0);
        player.setStatus("PARALYSIS");
        assertEquals(Math.max(1, Math.round(raw * 0.5f)), player.pbSpeed(0));
        player.setStatus("");
        player.pbOwnSide().effects.set(PBEffects.Side.Tailwind, 3);
        assertEquals(raw * 2, player.pbSpeed(0));
    }

    @Test
    @DisplayName("the badge boost needs the player's own Pokemon in an internal battle and 3 badges")
    void badgeBoost() {
        int raw = player.baseSpeed();
        assertEquals(raw, player.pbSpeed(2));
        assertEquals(Math.round(raw * 1.1f), player.pbSpeed(3));
        Battler foe = battle.battlerAt(1);
        assertEquals(foe.baseSpeed(), foe.pbSpeed(8));                            // a foe never gets the boost
    }

    @Test
    @DisplayName("badges raise the damage the player's Pokemon deal and lower what it takes (122_Move_Usage_Calculations:413-428)")
    void badgeBoostInDamage() {
        Battler foe = battle.battlerAt(1);
        BattleMove tackle = new BattleMove(battle.pbs().move("TACKLE"));
        battle.numBadges = 0;
        int plain = DamageCalc.compute(player, foe, tackle, battle.pbs(), new Random(5), false, 1);
        int taken = DamageCalc.compute(foe, player, tackle, battle.pbs(), new Random(5), false, 1);
        battle.numBadges = 8;
        int boosted = DamageCalc.compute(player, foe, tackle, battle.pbs(), new Random(5), false, 1);
        int takenWithBadges = DamageCalc.compute(foe, player, tackle, battle.pbs(), new Random(5), false, 1);
        assertTrue(boosted > plain, boosted + " vs " + plain);
        assertTrue(takenWithBadges < taken, takenWithBadges + " vs " + taken);
        battle.internalBattle = false;
        assertEquals(plain, DamageCalc.compute(player, foe, tackle, battle.pbs(), new Random(5), false, 1));
    }

    @Test
    @DisplayName("a fainted battler's speed is 1")
    void faintedIsOne() {
        player.hp = 0;
        assertEquals(1, player.pbSpeed(8));
    }
}
