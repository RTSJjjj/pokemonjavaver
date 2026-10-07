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
 * Stage 5 / 2c: {@code Battler_UseMove_SuccessChecks:1-247, 604-661}
 * ({@code pbCanChooseMove?}, {@code pbObedienceCheck?}, {@code pbDisobey},
 * {@code pbTryUseMove}, {@code pbSuccessCheckPerHit}, {@code pbMissMessage}).
 * Each assertion names the Ruby line it pins.
 */
class BattlerUseMoveChecksTest {

    private static void write(Path root, String name, String content) throws Exception {
        Path pbs = root.resolve("pbs");
        Files.createDirectories(pbs);
        Files.write(pbs.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    /** HERO knows STUNSPORE (status) and TACKLE; FOE knows TACKLE. DIG has function 0CA. */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":2,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"STUNSPORE\"},{\"level\":1,\"move\":\"TACKLE\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        write(root, "moves.json", "{\"total\":3,\"moves\":{"
                + "\"STUNSPORE\":{\"id\":1,\"internalName\":\"STUNSPORE\",\"name\":\"Stun Spore\","
                + "\"function\":\"007\",\"power\":0,\"type\":\"GRASS\",\"category\":\"Status\","
                + "\"accuracy\":100,\"pp\":30,\"flags\":\"bcel\",\"target\":\"NearOther\"},"
                + "\"TACKLE\":{\"id\":2,\"internalName\":\"TACKLE\",\"name\":\"Tackle\","
                + "\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"flags\":\"a\",\"target\":\"NearOther\"},"
                + "\"DIG\":{\"id\":3,\"internalName\":\"DIG\",\"name\":\"Dig\","
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
        // pbInitialize -> pbInitEffects(false) (Battler_Initialize:74); the live engine does not run it yet (stage 5 / 2d).
        battle.player().initEffects(false);
        battle.foe().initEffects(false);
        return battle;
    }

    private static boolean said(Battle battle, String text) {
        for (String m : battle.roundMessages) {
            if (m.contains(text)) return true;
        }
        return false;
    }

    // ----------------------------------------------------------- pbCanChooseMove?

    @Test
    @DisplayName("2c: Disable blocks the move unless it is a special usage (:12-18)")
    void disable(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        BattleMove spore = hero.moveSlot(0);
        hero.effects.set(PBEffects.Battler.DisableMove, spore.id());

        assertFalse(hero.pbCanChooseMove(spore, false, true, false), ":17");
        assertTrue(said(battle, "Stun Spore被定住了！"), ":14");
        assertTrue(hero.pbCanChooseMove(spore, false, true, true), ":12 !specialUsage");
        assertTrue(hero.pbCanChooseMove(hero.moveSlot(1), false, true, false), "another move is fine");
    }

    @Test
    @DisplayName("2c: Taunt refuses status moves only (:82-88)")
    void taunt(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        hero.effects.set(PBEffects.Battler.Taunt, 3);

        assertFalse(hero.pbCanChooseMove(hero.moveSlot(0), false, true, false), ":87 status move");
        assertTrue(said(battle, "所以无法使用Stun Spore！"), ":84");
        assertTrue(hero.pbCanChooseMove(hero.moveSlot(1), false, true, false), "a damaging move is fine");
    }

    @Test
    @DisplayName("2c: Torment refuses the move used last; Instructed lifts it (:90-97)")
    void torment(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        hero.effects.set(PBEffects.Battler.Torment, true);
        hero.lastMoveUsed = "TACKLE";

        assertFalse(hero.pbCanChooseMove(hero.moveSlot(1), false, true, false), ":96");
        assertTrue(said(battle, "因无理取闹而不能使用相同的招式！"), ":93");
        assertTrue(hero.pbCanChooseMove(hero.moveSlot(0), false, true, false), "a different move");
        hero.effects.set(PBEffects.Battler.Instructed, true);
        assertTrue(hero.pbCanChooseMove(hero.moveSlot(1), false, true, false), ":90 !Instructed");
    }

    @Test
    @DisplayName("2c: an Imprisoning foe that knows the move blocks it (:108-115)")
    void imprison(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        battle.foe().effects.set(PBEffects.Battler.Imprison, true);

        assertFalse(hero.pbCanChooseMove(hero.moveSlot(1), false, true, false), "foe knows TACKLE");
        assertTrue(said(battle, "不能使用被封印的Tackle！"), ":111");
        assertTrue(hero.pbCanChooseMove(hero.moveSlot(0), false, true, false), "foe does not know STUNSPORE");
    }

    @Test
    @DisplayName("2c: Choice Band locks the held move; without the item the lock resets (:52-66)")
    void choiceBand(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        hero.item = "CHOICEBAND";
        hero.effects.set(PBEffects.Battler.ChoiceBand, hero.moveSlot(0).id());

        assertFalse(hero.pbCanChooseMove(hero.moveSlot(1), true, true, false), ":61");
        assertTrue(hero.pbCanChooseMove(hero.moveSlot(0), true, true, false), ":55 same move");

        hero.item = "";
        assertTrue(hero.pbCanChooseMove(hero.moveSlot(1), true, true, false), ":64 lock lifted");
        assertEquals(-1, hero.effects.intVal(PBEffects.Battler.ChoiceBand), ":64");
    }

    @Test
    @DisplayName("2c: commandPhase uses pbDisplayPaused, else pbDisplay; showMessages=false is silent (:14-15)")
    void silentWhenNotShowing(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        hero.effects.set(PBEffects.Battler.Taunt, 3);
        battle.roundEvents.clear();

        assertFalse(hero.pbCanChooseMove(hero.moveSlot(0), true, false, false));
        assertEquals(0, battle.roundEvents.size, "showMessages=false shows nothing");
    }

    @Test
    @DisplayName("2c: Battle#pbCanChooseMove? refuses 0 PP and the non-encored slot (Battle_Action_AttacksPriority:5-18)")
    void battleCanChooseMove(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();

        assertTrue(battle.pbCanChooseMove(hero.index, 0, false, false));
        hero.pbSetPP("STUNSPORE", 0);
        assertFalse(battle.pbCanChooseMove(hero.index, 0, false, false), ":9");
        assertTrue(battle.pbCanChooseMove(hero.index, 0, false, true), ":9 sleepTalk ignores PP");
        assertTrue(battle.pbCanShowFightMenu(hero.index), "Battle_Phase_Command:49-52 slot 1 is usable");

        hero.effects.set(PBEffects.Battler.Encore, 3);
        hero.effects.set(PBEffects.Battler.EncoreMove, hero.moveSlot(1).id());
        assertFalse(battle.pbCanChooseMove(hero.index, 0, false, true), ":15");
        assertTrue(battle.pbCanChooseMove(hero.index, 1, false, false));
        assertFalse(battle.pbCanShowFightMenu(hero.index), "Battle_Phase_Command:46 Encore");
        assertTrue(battle.pbCanChooseAnyMove(hero.index, false), "Battle_Action_AttacksPriority:20-32");
    }

    // ------------------------------------------------------------- pbTryUseMove

    @Test
    @DisplayName("2c: pbTryUseMove stops on HyperBeam recharge, and skipAccuracyCheck skips the status block (:259-268)")
    void tryUseMoveGuards(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        BattleMove tackle = hero.moveSlot(1);
        Object[] choice = {":UseMove", 1, tackle, -1};

        hero.effects.set(PBEffects.Battler.HyperBeam, 1);
        assertFalse(hero.pbTryUseMove(choice, tackle, false, false), ":261");
        assertTrue(said(battle, "必须休息一下！"), ":260");
        hero.effects.set(PBEffects.Battler.HyperBeam, 0);

        hero.effects.set(PBEffects.Battler.Flinch, true);
        assertTrue(hero.pbTryUseMove(choice, tackle, true, true), ":268 skipAccuracyCheck returns true before the flinch");
        assertFalse(hero.pbTryUseMove(choice, tackle, false, false), ":328 flinch");
        assertTrue(said(battle, "畏缩了，无法行动！"), ":323");
        assertTrue(hero.lastMoveFailed, ":327");
    }

    @Test
    @DisplayName("2c: pbTryUseMove - sleep counts down, wakes at 0, otherwise blocks a normal move (:271-281)")
    void tryUseMoveSleep(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        BattleMove tackle = hero.moveSlot(1);
        Object[] choice = {":UseMove", 1, tackle, -1};

        hero.status = "SLEEP";
        hero.statusCount = 3;
        assertFalse(hero.pbTryUseMove(choice, tackle, false, false), ":279");
        assertEquals(2, hero.statusCount, ":272");
        assertTrue(said(battle, "依旧在沉睡。"), ":447 via pbContinueStatus");

        hero.statusCount = 1;
        assertTrue(hero.pbTryUseMove(choice, tackle, false, false), ":274 woke up, may act");
        assertTrue(hero.status == null || hero.status.isEmpty(), ":274 pbCureStatus");
        assertTrue(said(battle, "醒来了！"));
    }

    @Test
    @DisplayName("2c: pbTryUseMove - confusion counts down and is cured at 0 (:331-336)")
    void tryUseMoveConfusion(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        BattleMove tackle = hero.moveSlot(1);
        Object[] choice = {":UseMove", 1, tackle, -1};
        hero.effects.set(PBEffects.Battler.Confusion, 1);

        assertTrue(hero.pbTryUseMove(choice, tackle, false, false), ":376");
        assertEquals(0, hero.effects.intVal(PBEffects.Battler.Confusion), ":332-334");
        assertTrue(said(battle, "解除了混乱！"), ":335");
    }

    @Test
    @DisplayName("2c: pbTryUseMove - infatuation names the other battler (:366-369)")
    void tryUseMoveAttract(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        BattleMove tackle = hero.moveSlot(1);
        Object[] choice = {":UseMove", 1, tackle, -1};
        hero.effects.set(PBEffects.Battler.Attract, battle.foe().index);

        hero.pbTryUseMove(choice, tackle, false, false);
        assertTrue(said(battle, "爱上了"), ":368");
    }

    // ----------------------------------------------------------- pbObedienceCheck?

    @Test
    @DisplayName("2c: obedience - foes, non-moves, multi-turn moves and non-internal battles always obey (:137-140)")
    void obedienceShortCircuits(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        BattleMove tackle = hero.moveSlot(1);
        Object[] move = {":UseMove", 1, tackle, -1};

        assertTrue(battle.foe().pbObedienceCheck(new Object[] {":UseMove", 0, battle.foe().moveSlot(0), -1}), ":140");
        assertTrue(hero.pbObedienceCheck(new Object[] {":SwitchOut", 1, null, -1}), ":138");
        battle.internalBattle = false;
        assertTrue(hero.pbObedienceCheck(move), ":139");
        battle.internalBattle = true;
        hero.effects.set(PBEffects.Battler.Rollout, 1);
        assertTrue(hero.pbObedienceCheck(move), ":137");
        hero.effects.set(PBEffects.Battler.Rollout, 0);
        assertTrue(hero.pbObedienceCheck(move), ":186 own Pokemon under the badge level obeys");
    }

    @Test
    @DisplayName("2c: pbDisobey clears the Rage effect before anything random happens (:195)")
    void disobeyRefusals(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        BattleMove tackle = hero.moveSlot(1);
        hero.effects.set(PBEffects.Battler.Rage, true);
        // The outcome after :195 is random (pbRandom); only the first line is deterministic.
        hero.pbDisobey(new Object[] {":UseMove", 1, tackle, -1}, 1000);
        assertFalse(hero.effects.truthy(PBEffects.Battler.Rage), ":195");
    }

    // --------------------------------------------------------- pbSuccessCheckPerHit

    @Test
    @DisplayName("2c: per-hit check - a charging turn, Lock-On and skipAccuracyCheck all pass (:606-609/640)")
    void perHitEarlyOuts(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        Battler foe = battle.foe();
        BattleMove tackle = hero.moveSlot(1);

        assertTrue(hero.pbSuccessCheckPerHit(tackle, hero, foe, true), ":640");
        hero.effects.set(PBEffects.Battler.LockOn, 2);
        hero.effects.set(PBEffects.Battler.LockOnPos, foe.index);
        assertTrue(hero.pbSuccessCheckPerHit(tackle, hero, foe, false), ":608-609");
        hero.effects.set(PBEffects.Battler.LockOn, 0);
        hero.effects.set(PBEffects.Battler.TwoTurnAttack, data.move("DIG").id);
        assertTrue(hero.pbSuccessCheckPerHit(tackle, hero, foe, false), ":606");
    }

    @Test
    @DisplayName("2c: per-hit check - a Digging target is missed by an ordinary move even when accuracy is skipped (:625-626)")
    void perHitDigging(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        Battler foe = battle.foe();
        BattleMove tackle = hero.moveSlot(1);
        foe.effects.set(PBEffects.Battler.TwoTurnAttack, data.move("DIG").id);

        assertTrue(foe.inTwoTurnAttack("0CA"), "PokeBattle_Battler:718-723");
        assertTrue(foe.semiInvulnerable(), "PokeBattle_Battler:725-727");
        assertFalse(hero.pbSuccessCheckPerHit(tackle, hero, foe, true), ":626 miss");
        assertFalse(foe.inTwoTurnAttack("0C9"), "other function codes do not match");
    }

    @Test
    @DisplayName("2c: pbMissMessage - a charging target 'avoids the attack', otherwise the user's attack missed (:652-661)")
    void missMessage(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();
        Battler foe = battle.foe();
        BattleMove tackle = hero.moveSlot(1);

        hero.pbMissMessage(tackle, hero, foe);
        assertTrue(said(battle, "的攻击没有命中！"), ":659");

        foe.effects.set(PBEffects.Battler.TwoTurnAttack, data.move("DIG").id);
        hero.pbMissMessage(tackle, hero, foe);
        assertTrue(said(battle, "避开了攻击!"), ":657");
    }

    // ------------------------------------------------------------ small helpers

    @Test
    @DisplayName("2c: pbContinueStatus shows the status line; pbSleepSelf puts the user to sleep (Battler_Statuses:358/443-466)")
    void continueStatusAndSleepSelf(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battleOf(data);
        Battler hero = battle.player();

        hero.status = "PARALYSIS";
        hero.pbContinueStatus();
        assertTrue(said(battle, "麻痹了！\n无法行动！"), ":454");

        hero.status = "";
        hero.pbSleepSelf("Hero开始睡觉了……");
        assertEquals("SLEEP", hero.status, ":359");
        assertTrue(hero.statusCount > 0, "pbSleepDuration rolled 2..4");
        assertTrue(said(battle, "开始睡觉了……"));
    }
}
