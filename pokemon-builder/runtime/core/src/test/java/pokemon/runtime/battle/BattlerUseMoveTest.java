package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 5 / 2d: {@code pbUseMove} / {@code pbProcessMoveHit} / targeting driven
 * end to end on the singles field (Battler_UseMove:152-809,
 * Battler_UseMove_Targeting). Each assertion names the Ruby line it pins.
 */
class BattlerUseMoveTest {

    private static void write(Path root, String name, String content) throws Exception {
        Path pbs = root.resolve("pbs");
        Files.createDirectories(pbs);
        Files.write(pbs.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    /** HERO knows TACKLE, STUNSPORE, DOUBLEKICK; FOE knows TACKLE. */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":2,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,100,100,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"},{\"level\":1,\"move\":\"STUNSPORE\"},{\"level\":1,\"move\":\"DOUBLEKICK\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        write(root, "moves.json", "{\"total\":4,\"moves\":{"
                + "\"TACKLE\":{\"id\":1,\"internalName\":\"TACKLE\",\"name\":\"Tackle\","
                + "\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"flags\":\"a\",\"target\":\"NearOther\"},"
                + "\"STUNSPORE\":{\"id\":2,\"internalName\":\"STUNSPORE\",\"name\":\"Stun Spore\","
                + "\"function\":\"007\",\"power\":0,\"type\":\"GRASS\",\"category\":\"Status\","
                + "\"accuracy\":100,\"pp\":30,\"flags\":\"bcel\",\"target\":\"NearOther\"},"
                + "\"DOUBLEKICK\":{\"id\":3,\"internalName\":\"DOUBLEKICK\",\"name\":\"Double Kick\","
                + "\"function\":\"0BD\",\"power\":30,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":30,\"flags\":\"a\",\"target\":\"NearOther\"},"
                + "\"DIG\":{\"id\":4,\"internalName\":\"DIG\",\"name\":\"Dig\","
                + "\"function\":\"0CA\",\"power\":80,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":10,\"flags\":\"a\",\"target\":\"NearOther\"}}}");
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
        Battle battle = new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(new Pokemon(data.species("HERO"), 20, data))
                .addFoe(new Pokemon(data.species("FOE"), 20, data));
        battle.badges = new HashSet<>();
        battle.player().initEffects(false);                      // Battler_Initialize:74 (live wiring: next step)
        battle.foe().initEffects(false);
        return battle;
    }

    private static Object[] choice(Battler user, int slot) {
        return new Object[] {":UseMove", slot, user.moveSlot(slot), -1};
    }

    private static int indexOfMessage(Battle battle, String text) {
        for (int i = 0; i < battle.roundMessages.size; i++) {
            if (battle.roundMessages.get(i).contains(text)) return i;
        }
        return -1;
    }

    @Test
    @DisplayName("2d: a damaging move shows 'used X!', animates, hurts the target and records the move (:305/688/716/719/256-264)")
    void damagingMove(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        Battler foe = battle.foe();
        int foeHp = foe.hp;
        int pp = hero.moveSlotPp(0);

        hero.pbUseMove(choice(hero, 0), false);

        assertTrue(foe.hp < foeHp, ":716 the target lost HP");
        assertEquals(pp - 1, hero.moveSlotPp(0), ":212 PP spent");
        assertTrue(indexOfMessage(battle, "Hero使用Tackle！") >= 0, ":305");
        assertEquals("TACKLE", hero.lastMoveUsed, ":256");
        assertEquals("TACKLE", hero.lastRegularMoveUsed, ":259");
        assertEquals(data.move("TACKLE").id, battle.lastMoveUsed, ":263");
        assertEquals(hero.index, battle.lastMoveUser, ":264");
        assertEquals(hero.battle.turnCount(), hero.lastRoundMoved, ":112 pbEndTurn");
        boolean sawHit = false;
        for (Battle.RoundEvent e : battle.roundEvents) if (e.kind == Battle.RoundEvent.Kind.HIT) sawHit = true;
        assertTrue(sawHit, ":719 the hit animation event");
        assertEquals(foeHp - foe.hp, foe.damageState.hpLost);
    }

    @Test
    @DisplayName("2d: a status move runs pbEffectAgainstTarget (Stun Spore paralyses) (:759)")
    void statusMove(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();

        hero.pbUseMove(choice(hero, 1), false);

        assertEquals("PARALYSIS", battle.foe().status, ":759");
        assertEquals(29, hero.moveSlotPp(1));
    }

    @Test
    @DisplayName("2d: a two-hit move hits twice and reports it (:455-473/495-506)")
    void multiHitMove(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();

        hero.pbUseMove(choice(hero, 2), false);

        assertTrue(indexOfMessage(battle, "击中了2次！") >= 0, ":505");
        int hits = 0;
        for (Battle.RoundEvent e : battle.roundEvents) if (e.kind == Battle.RoundEvent.Kind.HIT) hits++;
        assertEquals(2, hits, ":719 one hit animation per hit");
    }

    @Test
    @DisplayName("2d: a move against a Digging target misses with the 'avoided' message and deals nothing (:622-628/654)")
    void missesDiggingTarget(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        Battler foe = battle.foe();
        foe.effects.set(PBEffects.Battler.TwoTurnAttack, data.move("DIG").id);
        int foeHp = foe.hp;

        hero.pbUseMove(choice(hero, 0), false);

        assertEquals(foeHp, foe.hp, ":667 nothing happens");
        assertTrue(indexOfMessage(battle, "避开了攻击!") >= 0, ":657");
        assertTrue(hero.lastMoveFailed, ":469");
    }

    @Test
    @DisplayName("2d: a flinched user does not move (pbTryUseMove:315-328) and the next use works again")
    void flinchedUser(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        Battler foe = battle.foe();
        int foeHp = foe.hp;
        hero.effects.set(PBEffects.Battler.Flinch, true);

        hero.pbUseMove(choice(hero, 0), false);
        assertEquals(foeHp, foe.hp);
        assertTrue(indexOfMessage(battle, "畏缩了，无法行动！") >= 0);
        assertNull(hero.lastMoveUsed, ":197");

        hero.effects.set(PBEffects.Battler.Flinch, false);
        hero.pbUseMove(choice(hero, 0), false);
        assertTrue(foe.hp < foeHp);
    }

    @Test
    @DisplayName("2d: knocking out the foe awards Exp once (pbGainExp :553)")
    void knockOutAwardsExp(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        Battler foe = battle.foe();
        foe.hp = 1;

        hero.pbUseMove(choice(hero, 0), false);

        assertTrue(foe.fainted(), ":807 target fainted");
        assertTrue(foe.expAwarded);
        assertEquals(1, battle.lastExpAwards.size, "awardExperience ran once");
        hero.pbUseMove(choice(hero, 0), false);
        assertEquals(1, battle.lastExpAwards.size, "no second award");
    }

    @Test
    @DisplayName("2d: a move with no PP left prints the refusal and stops (:211-222)")
    void noPp(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        hero.pbSetPP("TACKLE", 0);
        int foeHp = battle.foe().hp;

        hero.pbUseMove(choice(hero, 0), false);

        assertEquals(foeHp, battle.foe().hp);
        assertTrue(indexOfMessage(battle, "但招式已经没有PP了！") >= 0, ":214");
        assertTrue(hero.lastMoveFailed, ":219");
    }

    @Test
    @DisplayName("2d: pbFindTargets on the singles field picks the opposing battler (:56-64)")
    void findTargets(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        Object[] c = choice(hero, 0);

        com.badlogic.gdx.utils.Array<Battler> targets = hero.pbFindTargets(c, (BattleMove) c[2], hero);

        assertEquals(1, targets.size);
        assertSame(battle.foe(), targets.get(0));
    }
}
