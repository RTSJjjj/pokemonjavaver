package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
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
 * Stage 5 / 2b: the damage half of {@code PokeBattle_Move} (Move_Usage:166-436)
 * and {@code pbConfusionDamage} (Battler_UseMove:130-146). Each assertion names
 * the Ruby line it pins.
 */
class MoveUsageTest {

    private static void write(Path root, String name, String content) throws Exception {
        Path pbs = root.resolve("pbs");
        Files.createDirectories(pbs);
        Files.write(pbs.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    /** HERO knows STUNSPORE; FOE knows TACKLE. */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":2,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"STUNSPORE\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        write(root, "moves.json", "{\"total\":2,\"moves\":{"
                + "\"STUNSPORE\":{\"id\":1,\"internalName\":\"STUNSPORE\",\"name\":\"Stun Spore\","
                + "\"function\":\"007\",\"power\":0,\"type\":\"GRASS\",\"category\":\"Status\","
                + "\"accuracy\":100,\"pp\":30,\"flags\":\"bcel\",\"target\":\"NearOther\"},"
                + "\"TACKLE\":{\"id\":2,\"internalName\":\"TACKLE\",\"name\":\"Tackle\","
                + "\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"flags\":\"a\",\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(root, "types.json", "{\"total\":2,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"GRASS\":{\"id\":4,\"internalName\":\"GRASS\",\"name\":\"Grass\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }

    private static Battle battleOf(PbsData data) {
        return new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(new Pokemon(data.species("HERO"), 20, data))
                .addFoe(new Pokemon(data.species("FOE"), 20, data));
    }

    private static boolean said(Battle battle, String text) {
        for (String m : battle.roundMessages) {
            if (m.contains(text)) return true;
        }
        return false;
    }

    @Test
    @DisplayName("2b: a Substitute takes the hit and loses HP instead of the target (Move_Usage:168-171/199-204/254-255/341-344)")
    void substituteTakesTheDamage(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler user = battle.player();
        Battler target = battle.foe();
        BattleMove tackle = new BattleMove(data.move("TACKLE"));
        MoveEffect fx = MoveEffectRegistry.of(tackle.function());
        target.effects.set(PBEffects.Battler.Substitute, 30);
        target.damageState.calcDamage = 50;
        int hpBefore = target.hp;

        MoveUsage.pbCheckDamageAbsorption(fx, tackle, user, target);
        assertTrue(target.damageState.substitute, ":170");
        MoveUsage.pbReduceDamage(fx, tackle, user, target);
        assertEquals(30, target.damageState.hpLost, ":200 capped at the Substitute's HP");
        assertEquals(30, target.damageState.totalHPLost, ":202");
        MoveUsage.pbInflictHPDamage(target);
        assertEquals(0, target.effects.intVal(PBEffects.Battler.Substitute), ":255");
        assertEquals(hpBefore, target.hp, ":257 the target's own HP is untouched");

        MoveUsage.pbHitEffectivenessMessages(fx, tackle, user, target, 1);
        assertTrue(said(battle, "替身承受伤害保护了"), ":327");
        assertTrue(said(battle, "的替身消失了！"), ":343");
    }

    @Test
    @DisplayName("2b: a lethal hit is capped at the target's HP; Endure leaves 1 HP (Move_Usage:224-231/384-385)")
    void lethalHitAndEndure(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler user = battle.player();
        Battler target = battle.foe();
        BattleMove tackle = new BattleMove(data.move("TACKLE"));
        MoveEffect fx = MoveEffectRegistry.of(tackle.function());
        target.hp = 20;
        target.damageState.calcDamage = 9999;

        MoveUsage.pbReduceDamage(fx, tackle, user, target);
        assertEquals(20, target.damageState.hpLost, ":225 capped at hp");
        assertFalse(target.damageState.endured);

        target.damageState.reset();
        target.damageState.calcDamage = 9999;
        target.effects.set(PBEffects.Battler.Endure, true);
        MoveUsage.pbReduceDamage(fx, tackle, user, target);
        assertEquals(19, target.damageState.hpLost, ":231");
        assertTrue(target.damageState.endured, ":230");
        MoveUsage.pbEndureKOMessage(target);
        assertTrue(said(battle, "挺住了！"), ":385");
    }

    @Test
    @DisplayName("2b: Sturdy and Focus Sash survive a full-HP KO; Mold Breaker disables Sturdy (Move_Usage:232-238)")
    void sturdyAndFocusSash(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler user = battle.player();
        Battler target = battle.foe();
        BattleMove tackle = new BattleMove(data.move("TACKLE"));
        MoveEffect fx = MoveEffectRegistry.of(tackle.function());

        target.ability = "STURDY";
        target.hp = target.maxHp();
        target.damageState.calcDamage = 99999;
        MoveUsage.pbReduceDamage(fx, tackle, user, target);
        assertTrue(target.damageState.sturdy, ":234");
        assertEquals(target.maxHp() - 1, target.damageState.hpLost, ":235");

        target.damageState.reset();
        battle.moldBreaker = true;
        target.damageState.calcDamage = 99999;
        MoveUsage.pbReduceDamage(fx, tackle, user, target);
        assertFalse(target.damageState.sturdy, ":233 !moldBreaker");
        assertEquals(target.maxHp(), target.damageState.hpLost);
        battle.moldBreaker = false;

        target.ability = "";
        target.item = "FOCUSSASH";
        target.damageState.reset();
        target.damageState.calcDamage = 99999;
        MoveUsage.pbReduceDamage(fx, tackle, user, target);
        assertTrue(target.damageState.focusSash, ":237");
        assertEquals(target.maxHp() - 1, target.damageState.hpLost, ":238");
    }

    @Test
    @DisplayName("2b: pbRecordDamageLost fills the Counter / Revenge / Assurance bookkeeping (Move_Usage:408-436)")
    void recordDamageLost(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler user = battle.player();
        Battler target = battle.foe();
        BattleMove tackle = new BattleMove(data.move("TACKLE"));   // Physical
        MoveEffect fx = MoveEffectRegistry.of(tackle.function());
        target.damageState.hpLost = 12;

        MoveUsage.pbRecordDamageLost(fx, tackle, user, target);

        assertEquals(12, target.yamaskhp, ":410");
        assertEquals(12, target.effects.intVal(PBEffects.Battler.Counter), ":418 physical -> Counter");
        assertEquals(user.index, target.effects.intVal(PBEffects.Battler.CounterTarget), ":419");
        assertEquals(12, target.lastHPLost, ":429");
        assertTrue(target.tookDamage, ":430");
        assertTrue(target.lastAttacker.contains(user, true), ":431");
        assertEquals(12, target.lastHPLostFromFoe, ":433 target opposes user");
        assertTrue(target.lastFoeAttacker.contains(user, true), ":434");
    }

    @Test
    @DisplayName("2b: pbAnimateHitAndHPLost emits one HIT per side with the pre-hit HP and the effectiveness (Move_Usage:264-292)")
    void animateHitAndHpLost(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler user = battle.player();
        Battler target = battle.foe();
        target.hp = 100;
        target.damageState.hpLost = 40;
        target.damageState.typeMod = 16;                       // super effective (:275)
        battle.roundEvents.clear();
        Array<Battler> targets = new Array<>();
        targets.add(target);

        MoveUsage.pbAnimateHitAndHPLost(user, targets);

        assertEquals(1, battle.roundEvents.size);
        Battle.HitEvent hit = battle.roundEvents.get(0).hits[0];
        assertEquals(target.index, hit.idxBattler);
        assertEquals(140, hit.oldHp, ":271 oldHP = hp + hpLost");
        assertEquals(2, hit.effectiveness, ":275");
    }

    @Test
    @DisplayName("2b: pbConfusionDamage hurts the battler and prints the message (Battler_UseMove:130-146)")
    void confusionDamage(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler b = battle.player();
        int before = b.hp;

        b.pbConfusionDamage("混乱中伤害了自己！");

        assertTrue(b.hp < before, ":139 self.hp -= hpLost");
        assertEquals(before - b.hp, b.damageState.hpLost);
        assertEquals(before, b.damageState.initialHP, ":132");
        assertTrue(said(battle, "混乱中伤害了自己！"), ":141");
        assertEquals(b.hp, b.pokemon.hp, "hp= also writes the Pokemon (PokeBattle_Battler:94)");
    }
}
