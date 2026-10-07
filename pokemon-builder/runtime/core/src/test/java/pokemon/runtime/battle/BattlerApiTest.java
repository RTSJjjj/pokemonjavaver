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
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 4 / M0: the pure-append Battler surface landed from
 * {@code stage4-m0b-battler-api-notes.md} (143 methods) and
 * {@code stage4-m0-interface-freeze.md} §1-§3.
 *
 * <p>These tests only touch what is runnable today: the new fields, the
 * {@code stage}/{@code setStage} mapping, {@code pbInitEffects} (逐行
 * {@code Battler_Initialize:112-357}) and the predicates whose dependencies exist.
 * Methods that reach {@code PendingApi} stubs (any {@code battle.pbDisplay} /
 * {@code pbCommonAnimation} path) are deliberately NOT called here - they throw
 * until {@code Battle} lands those methods, which is the M0 design.</p>
 */
class BattlerApiTest {

    // ------------------------------------------------------------------ fields

    @Test
    @DisplayName("M0: the new Battler fields exist with the plugin's initial values")
    void fieldsAreInitialized(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 20, data);
        Battler battler = new Battler(hero, false);
        assertNotNull(battler.effects, "PokeBattle_Battler:23 @effects");
        assertNotNull(battler.damageState, "PokeBattle_Battler:42 @damageState");
        assertNotNull(battler.field, "PokeBattle_ActiveField 的桥接引用");
        // §4 wiring: the constructor now copies @item/@ability from the Pokemon
        // (Battler_Initialize:83-84 `@ability = pkmn.ability` / `@item = pkmn.item`).
        // Before that pass both stayed "" - so every BattleHandlers lookup keyed on
        // them (triggerAbilityOnSwitchIn(user.ability, ...)) found no handler - and
        // these two assertions pinned that placeholder. They now pin the transcribed
        // behaviour instead.
        assertEquals(hero.item == null ? "" : hero.item, battler.item, "PokeBattle_Battler:69");
        assertEquals(hero.ability == null ? "" : hero.ability, battler.ability, "PokeBattle_Battler:11");
        assertEquals(-1, battler.pokemonIndex, "Battler_Initialize:96");
        assertFalse(battler.mirrorHerbUsed, "Battler_Initialize:356");
        assertFalse(battler.droppedBelowHalfHP, "Battler_Initialize:163");
        assertFalse(battler.statsDropped);
        assertFalse(battler.statsRaisedThisRound);
        assertFalse(battler.statsLoweredThisRound);
        assertFalse(battler.dummy);
        assertEquals(0, battler.initialHP);
        assertTrue(battler.lastAttacker.isEmpty(), "Battler_Initialize:159");
        assertTrue(battler.movesUsed.isEmpty(), "Battler_Initialize:175");
        assertNull(battler.lastMoveUsed, "Battler_Initialize:168 的 -1 哨兵");
    }

    // ------------------------------------------------------- stage accessors

    @Test
    @DisplayName("M0: stage()/setStage() map PBStats' plugin numbering onto the 5-element arrays")
    void stageAccessorMapsPluginNumbering(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);

        battler.setStage(PBStats.ATTACK, 3);
        battler.setStage(PBStats.DEFENSE, 2);
        battler.setStage(PBStats.SPEED, 1);
        battler.setStage(PBStats.SPATK, -1);
        battler.setStage(PBStats.SPDEF, -2);
        battler.setStage(PBStats.ACCURACY, 4);
        battler.setStage(PBStats.EVASION, -4);

        // PBStats.ATTACK(1) -> stages[0] ... SPDEF(5) -> stages[4]
        assertEquals(3, battler.stages[0]);
        assertEquals(2, battler.stages[1]);
        assertEquals(1, battler.stages[2]);
        assertEquals(-1, battler.stages[3]);
        assertEquals(-2, battler.stages[4]);
        // ACCURACY(6) -> hitStages[0], EVASION(7) -> hitStages[1]
        assertEquals(4, battler.hitStages[0]);
        assertEquals(-4, battler.hitStages[1]);
        // and the accessors read the same slots back
        assertEquals(3, battler.stage(PBStats.ATTACK));
        assertEquals(-2, battler.stage(PBStats.SPDEF));
        assertEquals(4, battler.stage(PBStats.ACCURACY));
        assertEquals(-4, battler.stage(PBStats.EVASION));
    }

    @Test
    @DisplayName("M0: stage(HP) and out-of-range stats read 0 and are never written")
    void stageAccessorIgnoresHpAndOutOfRange(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertEquals(0, battler.stage(PBStats.HP), "HP(0) 是基础值槽，插件不用它做能力等级");
        assertEquals(0, battler.stage(99));
        battler.setStage(PBStats.HP, 6);
        battler.setStage(99, 6);
        for (int i = 0; i < battler.stages.length; i++) {
            assertEquals(0, battler.stages[i], "HP/越界都不许写 stages");
        }
        assertEquals(0, battler.hitStages[0]);
        assertEquals(0, battler.hitStages[1]);
    }

    // ------------------------------------------------------------ pbInitEffects

    @Test
    @DisplayName("M0: initEffects(false) zeroes every stage (Battler_Initialize:127-133)")
    void initEffectsResetsStages(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.setStage(PBStats.ATTACK, 5);
        battler.setStage(PBStats.EVASION, -3);
        battler.initEffects(false);
        for (int s : PBStats.EACH_BATTLE_STAT) {
            assertEquals(0, battler.stage(s), "PBStats " + s);
        }
    }

    @Test
    @DisplayName("M0: initEffects(false) writes the plugin's sentinel values verbatim")
    void initEffectsSetsSentinelValues(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.initEffects(false);

        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.Attract), ":177");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.LeechSeed), ":144");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.LockOnPos), ":146");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.PerishSongUser), ":149");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.BideTarget), ":185");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.ChoiceBand), ":188");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.MeanLook), ":246");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.SkyDrop), ":282");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.Type3), ":310");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.GorillaTactics), ":316");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.SwitchedAlly), ":323");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.SuccessiveMove), ":346");
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.SyrupyUser), ":344");
        assertEquals(1, battler.effects.intVal(PBEffects.Battler.ProtectRate), ":274");
        assertEquals(-1, battler.lastRegularMoveTarget, ":171");
        assertEquals(-1, battler.lastRoundMoved, ":172");
        assertFalse(battler.mirrorHerbUsed, ":356");
        assertEquals(0, battler.turnCount, ":176");
        assertFalse(battler.lastMoveFailed, ":173");
        assertFalse(battler.lastRoundMoveFailed, ":174");
        assertEquals(0, battler.initialHP, ":158");
    }

    @Test
    @DisplayName("M0: initEffects writes 0 for the boolean-ish effects too, and 0 stays truthy")
    void initEffectsZeroIsTruthy(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.initEffects(false);
        // :135 Confusion = 0 - Ruby's `if effects[Confusion]` is TRUE for 0.
        assertEquals(0, battler.effects.intVal(PBEffects.Battler.Confusion));
        assertTrue(battler.effects.truthy(PBEffects.Battler.Confusion),
                "Ruby 里 0 是真 —— truthy() 不能简化成 intVal!=0");
        // :151 Substitute = 0 as well.
        assertEquals(0, battler.effects.intVal(PBEffects.Battler.Substitute));
        assertTrue(battler.effects.truthy(PBEffects.Battler.Substitute));
        // :134 AquaRing = false is FALSY.
        assertFalse(battler.effects.truthy(PBEffects.Battler.AquaRing));
    }

    @Test
    @DisplayName("M0: initEffects sets Illusion to nil and the null keys are absent (Battler_Initialize:213)")
    void initEffectsIllusionIsNil(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.effects.set(PBEffects.Battler.Illusion, "something");
        battler.effects.set(PBEffects.Battler.Commander, "something");
        battler.initEffects(false);
        assertFalse(battler.effects.has(PBEffects.Battler.Illusion), ":213 = nil");
        assertFalse(battler.effects.has(PBEffects.Battler.Commander), ":337 = nil");
        assertNull(battler.effects.raw(PBEffects.Battler.Illusion));
    }

    @Test
    @DisplayName("M0: initEffects(true) keeps the stages and doubles the baton-pass timers (:116-117)")
    void initEffectsBatonPass(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.setStage(PBStats.ATTACK, 2);
        battler.effects.set(PBEffects.Battler.LaserFocus, 1);
        battler.effects.set(PBEffects.Battler.LockOn, 3);
        battler.initEffects(true);
        assertEquals(2, battler.stage(PBStats.ATTACK), ":113-124 分支不清 stages");
        assertEquals(2, battler.effects.intVal(PBEffects.Battler.LaserFocus), ":116 >0 -> 2");
        assertEquals(2, battler.effects.intVal(PBEffects.Battler.LockOn), ":117 >0 -> 2");
    }

    @Test
    @DisplayName("M0: initEffects(true) swaps attack/defense stages under Power Trick (:118-120)")
    void initEffectsBatonPassPowerTrick(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.setStage(PBStats.ATTACK, 3);
        battler.setStage(PBStats.DEFENSE, -2);
        battler.effects.set(PBEffects.Battler.PowerTrick, true);
        battler.initEffects(true);
        assertEquals(-2, battler.stage(PBStats.ATTACK), ":119 @attack,@defense = @defense,@attack");
        assertEquals(3, battler.stage(PBStats.DEFENSE));
    }

    @Test
    @DisplayName("M0: initEffects clears other battlers' Attract/LockOn/Trapping pointing at self")
    void initEffectsClearsCrossBattlerEffects(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 20, data);
        Pokemon foe = new Pokemon(data.species("FOE"), 20, data);
        Battle battle = new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(hero).addFoe(foe);
        Battler player = battle.player();
        Battler enemy = battle.foe();
        player.battle = battle;
        enemy.battle = battle;

        // The foe is attracted to / locking on to / trapped by the player (index 0).
        enemy.effects.set(PBEffects.Battler.Attract, player.index);
        enemy.effects.set(PBEffects.Battler.LockOn, 2);
        enemy.effects.set(PBEffects.Battler.LockOnPos, player.index);
        enemy.effects.set(PBEffects.Battler.TrappingUser, player.index);
        enemy.effects.set(PBEffects.Battler.Trapping, 3);
        enemy.effects.set(PBEffects.Battler.Octolock, true);
        enemy.effects.set(PBEffects.Battler.OctolockUser, player.index);
        enemy.effects.set(PBEffects.Battler.JawLock, true);
        enemy.effects.set(PBEffects.Battler.JawLockUser, player.index);
        enemy.effects.set(PBEffects.Battler.SkyDrop, player.index);
        enemy.effects.set(PBEffects.Battler.SyrupyUser, player.index);
        enemy.effects.set(PBEffects.Battler.Syrupy, 5);

        player.initEffects(false);

        assertEquals(-1, enemy.effects.intVal(PBEffects.Battler.Attract), ":178-180");
        assertEquals(0, enemy.effects.intVal(PBEffects.Battler.LockOn), ":224-229");
        assertEquals(-1, enemy.effects.intVal(PBEffects.Battler.LockOnPos), ":224-229");
        assertEquals(-1, enemy.effects.intVal(PBEffects.Battler.TrappingUser), ":303-307");
        assertEquals(0, enemy.effects.intVal(PBEffects.Battler.Trapping), ":303-307");
        assertFalse(enemy.effects.truthy(PBEffects.Battler.Octolock), ":232-237");
        assertEquals(-1, enemy.effects.intVal(PBEffects.Battler.OctolockUser));
        assertFalse(enemy.effects.truthy(PBEffects.Battler.JawLock), ":238-243");
        assertEquals(-1, enemy.effects.intVal(PBEffects.Battler.SkyDrop), ":283-285");
        assertEquals(0, enemy.effects.intVal(PBEffects.Battler.Syrupy), ":351-355");
        assertEquals(-1, enemy.effects.intVal(PBEffects.Battler.SyrupyUser), ":351-355");
    }

    // ------------------------------------------------------------- identity

    @Test
    @DisplayName("M0: opposes?/idxOwnSide/idxOpposingSide follow the index parity")
    void opposesAndSideIndices(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler player = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        Battler enemy = new Battler(new Pokemon(data.species("FOE"), 20, data), true);
        player.index = 0;
        enemy.index = 1;
        assertFalse(player.opposes(0), "PokeBattle_Battler:786");
        assertTrue(player.opposes(1));
        assertTrue(enemy.opposes(0));
        assertFalse(enemy.opposes(1));
        assertTrue(player.opposes(enemy));
        assertFalse(player.opposes(player));
        assertEquals(0, player.idxOwnSide());
        assertEquals(1, player.idxOpposingSide());
        assertEquals(1, enemy.idxOwnSide());
        assertEquals(0, enemy.idxOpposingSide());
        assertTrue(player.near(enemy), "1v1 下 nearBattlers? 只排除同一位置");
        assertFalse(player.near(player));
    }

    @Test
    @DisplayName("M0: pbThis covers the player, wild, trainer and Boss-rank wordings (:214-231)")
    void pbThisWording(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler player = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        player.index = 0;
        assertEquals("Hero", player.pbThis(), "玩家自己的宝可梦只用名字");

        Battler wild = new Battler(new Pokemon(data.species("FOE"), 20, data), true);
        wild.index = 1;
        assertEquals("野生的Foe", wild.pbThis(), ":225");

        Battler trainer = new Battler(new Pokemon(data.species("FOE"), 20, data), true);
        trainer.index = 1;
        trainer.trainerBattle = true;
        assertEquals("对手的Foe", trainer.pbThis(), ":217");

        Battler boss = new Battler(new Pokemon(data.species("FOE"), 20, data), true);
        boss.index = 1;
        boss.pokemon.battleRank = 2;
        assertEquals("特殊的Foe", boss.pbThis(), ":222");
        boss.pokemon.battleRank = 3;
        assertEquals("强大的Foe", boss.pbThis(), ":220");
    }

    @Test
    @DisplayName("M0: pbTeam/pbOpposingTeam are deliberately inverted (PokeBattle_Battler:233-245)")
    void teamNames(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler player = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        player.index = 0;
        Battler enemy = new Battler(new Pokemon(data.species("FOE"), 20, data), true);
        enemy.index = 1;
        assertEquals("我方队伍", player.pbTeam(false));
        assertEquals("对方队伍", player.pbOpposingTeam(false));
        assertEquals("对方队伍", enemy.pbTeam(false), ":235");
        assertEquals("我方队伍", enemy.pbOpposingTeam(false), ":242");
    }

    @Test
    @DisplayName("M0: pbOwnedByPlayer? is the player side (Battle:287-290)")
    void pbOwnedByPlayer(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler player = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        player.index = 0;
        Battler enemy = new Battler(new Pokemon(data.species("FOE"), 20, data), true);
        enemy.index = 1;
        assertTrue(player.pbOwnedByPlayer());
        assertFalse(enemy.pbOwnedByPlayer());
    }

    @Test
    @DisplayName("M0: pbDirectOpposing returns the other field slot (1v1)")
    void pbDirectOpposing(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 20, data);
        Pokemon foe = new Pokemon(data.species("FOE"), 20, data);
        Battle battle = new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(hero).addFoe(foe);
        Battler player = battle.player();
        Battler enemy = battle.foe();
        player.battle = battle;
        enemy.battle = battle;
        assertSame(enemy, player.pbDirectOpposing(false));
        assertSame(player, enemy.pbDirectOpposing(false));
    }

    // ---------------------------------------------------------- ability/item

    @Test
    @DisplayName("M0: abilityActive?/hasActiveAbility? follow PokeBattle_Battler:379-399")
    void abilityChecks(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.ability = "OVERGROW";
        assertTrue(battler.abilityActive());
        assertTrue(battler.hasActiveAbility("OVERGROW"));
        assertFalse(battler.hasActiveAbility("BLAZE"));
        assertFalse(battler.hasActiveAbility(""), "空名字不算匹配");
        assertTrue(battler.hasActiveAbility(new String[] {"BLAZE", "OVERGROW"}));
        assertFalse(battler.hasActiveAbility(new String[] {"BLAZE", "TORRENT"}));
        // Gastro Acid negates it (:383)
        battler.effects.set(PBEffects.Battler.GastroAcid, true);
        assertFalse(battler.abilityActive());
        assertFalse(battler.hasActiveAbility("OVERGROW"));
    }

    @Test
    @DisplayName("M0: a fainted battler has no active ability unless ignoreFainted (:380)")
    void abilityActiveIgnoresFainted(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.ability = "OVERGROW";
        battler.hp = 0;
        assertFalse(battler.abilityActive());
        assertTrue(battler.abilityActive(true));
        assertFalse(battler.hasActiveAbility("OVERGROW"));
        assertTrue(battler.hasActiveAbility("OVERGROW", true));
    }

    @Test
    @DisplayName("M0: the three ability blacklists match PokeBattle_Battler:403-503")
    void abilityBlacklists(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertTrue(battler.unstoppableAbility("MULTITYPE"), ":405-437");
        assertTrue(battler.unstoppableAbility("TERASHELL"));
        assertFalse(battler.unstoppableAbility("FLOWERGIFT"), "插件把它注释掉了");
        assertTrue(battler.ungainableAbility("ILLUSION"), ":447-483");
        assertTrue(battler.ungainableAbility("FLOWERGIFT"), "这里没被注释");
        assertFalse(battler.ungainableAbility("OVERGROW"));
        assertTrue(battler.uncopyableAbility("TRACE"), ":494-498");
        // :491 `abil = @ability_id if !abil` —— @ability_id 从未定义 → 无参调用恒 false
        assertFalse(battler.uncopyableAbility(null), "插件缺陷照抄：恒 false");
    }

    @Test
    @DisplayName("M0: activeAbilityShield? needs the Ability Shield item (PokeBattle_Battler:370-377)")
    void activeAbilityShield(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertFalse(battler.activeAbilityShield(), ":372 没有道具");
        battler.item = "ABILITYSHIELD";
        assertTrue(battler.activeAbilityShield());
        battler.ability = "KLUTZ";
        assertFalse(battler.activeAbilityShield(), ":375");
    }

    @Test
    @DisplayName("M0: itemActive?/hasActiveItem?/hasUtilityUmbrella? (PokeBattle_Battler:506-530, :610)")
    void itemChecks(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertTrue(battler.itemActive(), "空手也算 active（只受 Embargo/MagicRoom/Klutz 影响）");
        assertFalse(battler.hasActiveItem("AIRBALLOON"));
        battler.item = "AIRBALLOON";
        assertTrue(battler.hasActiveItem("AIRBALLOON"));
        assertTrue(battler.hasActiveItem(new String[] {"IRONBALL", "AIRBALLOON"}));
        battler.effects.set(PBEffects.Battler.Embargo, 1);
        assertFalse(battler.itemActive(), ":508");
        assertFalse(battler.hasActiveItem("AIRBALLOON"));
        assertFalse(battler.hasUtilityUmbrella());
        battler.effects.set(PBEffects.Battler.Embargo, 0);
        battler.item = "UTILITYUMBRELLA";
        assertTrue(battler.hasUtilityUmbrella(), ":611");
    }

    @Test
    @DisplayName("M0: isUnnerved? looks for the four abilities on the opposing side (:577-583)")
    void isUnnerved(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 20, data);
        Pokemon foe = new Pokemon(data.species("FOE"), 20, data);
        Battle battle = new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(hero).addFoe(foe);
        Battler player = battle.player();
        Battler enemy = battle.foe();
        player.battle = battle;
        enemy.battle = battle;
        player.ability = "OVERGROW";
        enemy.ability = "OVERGROW";
        assertFalse(player.isUnnerved());
        enemy.ability = "UNNERVE";
        assertTrue(player.isUnnerved(), ":578");
        assertFalse(enemy.isUnnerved(), "自己方不算");
    }

    @Test
    @DisplayName("M0: pbCanConsumeBerry? uses the 1/4 and Gluttony 1/2 thresholds (:152-159)")
    void pbCanConsumeBerry(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.hp = battler.maxHp();
        assertFalse(battler.pbCanConsumeBerry("SITRUSBERRY", true));
        battler.hp = battler.maxHp() / 4;
        assertTrue(battler.pbCanConsumeBerry("SITRUSBERRY", true), ":154");
        battler.hp = battler.maxHp() / 2;
        assertFalse(battler.pbCanConsumeBerry("SITRUSBERRY", true), "没有贪吃鬼");
        battler.ability = "GLUTTONY";
        assertTrue(battler.pbCanConsumeBerry("SITRUSBERRY", true), ":156");
        // :155 `alwaysCheckGluttony || NEWEST_BATTLE_MECHANICS` —— 本工程
        // NEWEST_BATTLE_MECHANICS = true（Settings:160），所以传 false 也照样查贪吃鬼。
        assertTrue(battler.pbCanConsumeBerry("SITRUSBERRY", false),
                "NEWEST_BATTLE_MECHANICS=true 时 alwaysCheckGluttony 不改变结果");
    }

    @Test
    @DisplayName("M0: abilityName()/itemName() resolve the display name from PbsData (:211-212)")
    void displayNames(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.pbs = data;
        battler.ability = "OVERGROW";
        battler.item = "AIRBALLOON";
        assertEquals("茂盛", battler.abilityName());
        assertEquals("气球", battler.itemName());
        // without the PbsData bridge it degrades to the internal name (documented)
        battler.pbs = null;
        assertEquals("OVERGROW", battler.abilityName());
        assertEquals("AIRBALLOON", battler.itemName());
    }

    // ------------------------------------------------------------------ HP

    @Test
    @DisplayName("M0: canHeal? is false at full HP or under Heal Block (:684-688)")
    void canHeal(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertFalse(battler.canHeal(), "满血不能回复");
        battler.hp = battler.maxHp() / 2;
        assertTrue(battler.canHeal());
        battler.effects.set(PBEffects.Battler.HealBlock, 1);
        assertFalse(battler.canHeal(), ":686");
    }

    @Test
    @DisplayName("M0: pbReduceHP floors at 1 while alive and pbRecoverHP caps at max HP (:5-31)")
    void reduceAndRecoverHp(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.hp = battler.maxHp();
        assertEquals(1, battler.pbReduceHP(0), ":8 amt = 1 if amt<1 && !fainted?");
        assertEquals(battler.maxHp() - 1, battler.hp);
        battler.hp = 5;
        assertEquals(5, battler.pbReduceHP(999), ":7 amt = @hp if amt>@hp");
        assertEquals(0, battler.hp);
        assertTrue(battler.fainted());
        // Recovering cannot exceed max HP (:21)
        battler.hp = battler.maxHp() - 1;
        assertEquals(1, battler.pbRecoverHP(999));
        assertEquals(battler.maxHp(), battler.hp);
    }

    @Test
    @DisplayName("M0: pbReduceHP marks tookDamage only when registerDamage (:15)")
    void reduceHpRegisterDamage(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.hp = battler.maxHp();
        battler.pbReduceHP(3, false, false, false);
        assertFalse(battler.tookDamage);
        battler.pbReduceHP(3, false, true, false);
        assertTrue(battler.tookDamage);
    }

    // --------------------------------------------------------------- status

    @Test
    @DisplayName("M0: the status predicates read the runtime's status string")
    void statusPredicates(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertFalse(battler.poisoned());
        assertFalse(battler.burned());
        assertFalse(battler.asleep());
        assertFalse(battler.paralyzed());
        assertFalse(battler.frozen());
        assertFalse(battler.pbHasAnyStatus(), ":22");
        battler.status = "POISON";
        assertTrue(battler.poisoned(), ":372");
        assertTrue(battler.pbHasStatus(PBStatuses.POISON));
        assertFalse(battler.pbHasStatus(PBStatuses.BURN));
        assertTrue(battler.pbHasAnyStatus());
    }

    @Test
    @DisplayName("M0: pbCanInflictStatus? blocks type immunities and an existing status (:25-200)")
    void pbCanInflictStatus(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler normal = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertTrue(normal.pbCanInflictStatus(PBStatuses.POISON, null, false, null, false),
                "普通属性可以被中毒");
        Battler steel = new Battler(new Pokemon(data.species("STEELMON"), 20, data), false);
        assertFalse(steel.pbCanInflictStatus(PBStatuses.POISON, null, false, null, false), ":92-93");
        Battler fire = new Battler(new Pokemon(data.species("FIREMON"), 20, data), false);
        assertFalse(fire.pbCanInflictStatus(PBStatuses.BURN, null, false, null, false), ":96");
        Battler ice = new Battler(new Pokemon(data.species("ICEMON"), 20, data), false);
        assertFalse(ice.pbCanInflictStatus(PBStatuses.FROSTBITE, null, false, null, false), ":101-103");
        // FROZEN additionally reads @battle.pbWeather (:57-58). §4 wiring turned
        // PendingApi.fieldWeather into a delegation (`battle.field.weather`), and this
        // battler was built without a Battle, so the read now fails on the null battle
        // exactly as Ruby's `nil.pbWeather` raises NoMethodError. The point of the
        // assertion is unchanged: the weather read must not silently pretend the
        // weather is clear.
        assertThrows(NullPointerException.class,
                () -> ice.pbCanInflictStatus(PBStatuses.FROZEN, null, false, null, false),
                "§4: @battle.pbWeather needs a battle (Ruby: nil.pbWeather -> NoMethodError)");
        // An already-statused battler cannot be given another one (:46-48)
        normal.status = "POISON";
        assertFalse(normal.pbCanInflictStatus(PBStatuses.BURN, null, false, null, false));
        // :29 `if self.status==newStatus && !ignoreStatus` —— ignoreStatus=true 时连
        // 「已经是这个状态」这一关都跳过，方法返回 true（Ruby 如此）。
        assertTrue(normal.pbCanInflictStatus(PBStatuses.POISON, null, false, null, true),
                "ignoreStatus=true 跳过 :29 的重复状态检查");
    }

    @Test
    @DisplayName("M0: setStatusCount keeps @statusCount and the legacy sleepTurns/toxic in step")
    void statusCountSync(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.status = "SLEEP";
        battler.setStatusCount(3);
        assertEquals(3, battler.statusCount);
        assertEquals(3, battler.sleepTurns, "SLEEP 时 @statusCount 就是剩余回合");
        battler.status = "POISON";
        battler.setStatusCount(1);
        assertEquals(1, battler.statusCount);
        assertEquals(1, battler.toxic, "POISON 时 @statusCount 就是剧毒计数");
    }

    @Test
    @DisplayName("M0: pbCureConfusion/pbCureAttract clear the effect keys (:547-549, :612-614)")
    void cureConfusionAndAttract(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.effects.set(PBEffects.Battler.Confusion, 3);
        battler.effects.set(PBEffects.Battler.Attract, 1);
        battler.pbCureConfusion();
        assertEquals(0, battler.effects.intVal(PBEffects.Battler.Confusion));
        battler.pbCureAttract();
        assertEquals(-1, battler.effects.intVal(PBEffects.Battler.Attract));
    }

    // ----------------------------------------------------------- stat stages

    @Test
    @DisplayName("M0: statStageAtMax/Min and pbCanRaiseStatStage honour the ±6 cap (:5-26)")
    void statStageLimits(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertFalse(battler.statStageAtMax(PBStats.ATTACK));
        assertTrue(battler.pbCanRaiseStatStage(PBStats.ATTACK));
        battler.setStage(PBStats.ATTACK, 6);
        assertTrue(battler.statStageAtMax(PBStats.ATTACK));
        assertFalse(battler.pbCanRaiseStatStage(PBStats.ATTACK), ":20-23");
        battler.setStage(PBStats.SPEED, -6);
        assertTrue(battler.statStageAtMin(PBStats.SPEED));
        assertFalse(battler.pbCanLowerStatStage(PBStats.SPEED), ":165-167");
    }

    @Test
    @DisplayName("M0: pbRaiseStatStageBasic clamps at +6 and returns the real increment (:28-45)")
    void raiseStatStageBasic(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertEquals(2, battler.pbRaiseStatStageBasic(PBStats.ATTACK, 2, false));
        assertEquals(2, battler.stage(PBStats.ATTACK));
        assertEquals(4, battler.pbRaiseStatStageBasic(PBStats.ATTACK, 4, false), ":38 min(4, 6-2)");
        assertEquals(6, battler.stage(PBStats.ATTACK));
        assertEquals(0, battler.pbRaiseStatStageBasic(PBStats.ATTACK, 1, false), "已到顶");
    }

    @Test
    @DisplayName("M0: pbLowerStatStageBasic clamps at -6 (:172-189)")
    void lowerStatStageBasic(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertEquals(3, battler.pbLowerStatStageBasic(PBStats.DEFENSE, 3, false));
        assertEquals(-3, battler.stage(PBStats.DEFENSE));
        assertEquals(3, battler.pbLowerStatStageBasic(PBStats.DEFENSE, 5, false), ":182 min(5, 6-3)");
        assertEquals(-6, battler.stage(PBStats.DEFENSE));
        assertEquals(0, battler.pbLowerStatStageBasic(PBStats.DEFENSE, 1, false));
    }

    @Test
    @DisplayName("M0: Contrary inverts the increment inside the *Basic helpers (:31-32, :175-176)")
    void contraryInvertsBasic(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.ability = "CONTRARY";
        assertEquals(2, battler.pbRaiseStatStageBasic(PBStats.ATTACK, 2, false), ":32 -> lower");
        assertEquals(-2, battler.stage(PBStats.ATTACK));
        battler.setStage(PBStats.ATTACK, 0);
        assertEquals(2, battler.pbLowerStatStageBasic(PBStats.ATTACK, 2, false), ":176 -> raise");
        assertEquals(2, battler.stage(PBStats.ATTACK));
    }

    @Test
    @DisplayName("M0: pbResetStatStages zeroes everything and flips the round flags (:393-403)")
    void resetStatStages(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        battler.setStage(PBStats.ATTACK, 2);
        battler.setStage(PBStats.SPEED, -2);
        battler.pbResetStatStages();
        for (int s : PBStats.EACH_BATTLE_STAT) {
            assertEquals(0, battler.stage(s));
        }
        assertTrue(battler.statsDropped, ":397 升过的被清掉");
        assertTrue(battler.statsLoweredThisRound, ":396");
        assertTrue(battler.statsRaisedThisRound, ":399 降过的被清掉");
        assertFalse(battler.hasAlteredStatStages());
    }

    @Test
    @DisplayName("M0: plainStats returns the eight-slot PBStats array (PokeBattle_Battler:297-305)")
    void plainStats(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        int[] stats = battler.plainStats();
        assertEquals(8, stats.length);
        assertEquals(battler.attack(), stats[PBStats.ATTACK]);
        assertEquals(battler.defense(), stats[PBStats.DEFENSE]);
        assertEquals(battler.spAtk(), stats[PBStats.SPATK]);
        assertEquals(battler.spDef(), stats[PBStats.SPDEF]);
        assertEquals(battler.speed(), stats[PBStats.SPEED]);
    }

    // ---------------------------------------------------------------- types

    @Test
    @DisplayName("M0: pbHasType?/pbHasOtherType? read pbTypes(true) (:350-363)")
    void hasType(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertTrue(battler.pbHasType("NORMAL"));
        assertFalse(battler.pbHasType("FIRE"));
        assertFalse(battler.pbHasType(null));
        assertTrue(battler.pbHasOtherType("FIRE"));
        assertFalse(battler.pbHasOtherType("NORMAL"), "去掉唯一的属性后没有别的了");
    }

    @Test
    @DisplayName("M0: pbTypes applies BurnUp/LoseFireType and Roost's Normal fallback (:314-348)")
    void pbTypesEffects(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler fire = new Battler(new Pokemon(data.species("FIREMON"), 20, data), false);
        assertTrue(fire.pbTypes().contains("FIRE", false));
        fire.effects.set(PBEffects.Battler.BurnUp, true);
        assertFalse(fire.pbTypes().contains("FIRE", false), ":334-336");
        fire.effects.set(PBEffects.Battler.BurnUp, false);
        fire.effects.set(PBEffects.Battler.LoseFireType, true);
        assertFalse(fire.pbTypes().contains("FIRE", false), ":322-324");

        Battler flyer = new Battler(new Pokemon(data.species("FLYER"), 20, data), false);
        flyer.effects.set(PBEffects.Battler.Roost, true);
        assertFalse(flyer.pbTypes().contains("FLYING", false), ":339-340");
        assertEquals(1, flyer.pbTypes().size);
        assertEquals("NORMAL", flyer.pbTypes().get(0), ":341 没有属性了就补普通");
    }

    @Test
    @DisplayName("M0: airborne? follows PokeBattle_Battler:591-602")
    void airborne(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler flyer = new Battler(new Pokemon(data.species("FLYER"), 20, data), false);
        assertTrue(flyer.airborne(), ":596 飞行属性");
        flyer.item = "IRONBALL";
        assertFalse(flyer.airborne(), ":592");
        flyer.item = "";
        flyer.effects.set(PBEffects.Battler.Ingrain, true);
        assertFalse(flyer.airborne(), ":593");
        flyer.effects.set(PBEffects.Battler.Ingrain, false);
        flyer.effects.set(PBEffects.Battler.SmackDown, true);
        assertFalse(flyer.airborne(), ":594");
        flyer.effects.set(PBEffects.Battler.SmackDown, false);
        flyer.ability = "LEVITATE";
        flyer.ability = "LEVITATE";
        assertTrue(flyer.airborne());
        Battler ground = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertFalse(ground.airborne());
        ground.ability = "LEVITATE";
        assertTrue(ground.airborne(), ":597");
    }

    @Test
    @DisplayName("M0: gender() reads the Pokemon's gender (PokeBattle_Battler:13)")
    void gender(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon pkmn = new Pokemon(data.species("HERO"), 20, data);
        pkmn.gender = 1;
        Battler battler = new Battler(pkmn, false);
        assertEquals(1, battler.gender());
    }

    @Test
    @DisplayName("M0: isSpecies?/form() read the Pokemon (PokeBattle_Battler:307-309, :62)")
    void speciesAndForm(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battler battler = new Battler(new Pokemon(data.species("HERO"), 20, data), false);
        assertTrue(battler.isSpecies("HERO"));
        assertFalse(battler.isSpecies("FOE"));
        assertFalse(battler.isSpecies(null));
        assertEquals(0, battler.form());
    }

    // ------------------------------------------------------------- helpers

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    /** The species/types/items/abilities the M0 Battler tests need. */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":7,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[100,120,80,120,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[\"OVERGROW\"],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[40,40,40,40,40,40],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"FIREMON\":{\"id\":3,\"internalName\":\"FIREMON\",\"name\":\"Firemon\","
                + "\"types\":[\"FIRE\"],\"baseStats\":[60,60,60,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],\"moves\":[]},"
                + "\"STEELMON\":{\"id\":4,\"internalName\":\"STEELMON\",\"name\":\"Steelmon\","
                + "\"types\":[\"STEEL\"],\"baseStats\":[60,60,60,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],\"moves\":[]},"
                + "\"ICEMON\":{\"id\":5,\"internalName\":\"ICEMON\",\"name\":\"Icemon\","
                + "\"types\":[\"ICE\"],\"baseStats\":[60,60,60,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],\"moves\":[]},"
                + "\"FLYER\":{\"id\":6,\"internalName\":\"FLYER\",\"name\":\"Flyer\","
                + "\"types\":[\"FLYING\"],\"baseStats\":[60,60,60,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],\"moves\":[]},"
                + "\"NORMALMON\":{\"id\":7,\"internalName\":\"NORMALMON\",\"name\":\"Normalmon\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[60,60,60,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],\"moves\":[]}}}");
        write(root, "moves.json", "{\"total\":1,\"moves\":{"
                + "\"SLASH\":{\"id\":1,\"internalName\":\"SLASH\",\"name\":\"Slash\","
                + "\"function\":\"000\",\"power\":70,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":20,\"flags\":\"a\"}}}");
        write(root, "abilities.json", "{\"total\":1,\"abilities\":{"
                + "\"OVERGROW\":{\"id\":1,\"internalName\":\"OVERGROW\",\"name\":\"茂盛\","
                + "\"description\":\"\"}}}");
        write(root, "types.json", "{\"total\":8,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"FIRE\":{\"id\":1,\"internalName\":\"FIRE\",\"name\":\"Fire\"},"
                + "\"GRASS\":{\"id\":2,\"internalName\":\"GRASS\",\"name\":\"Grass\"},"
                + "\"STEEL\":{\"id\":3,\"internalName\":\"STEEL\",\"name\":\"Steel\"},"
                + "\"ICE\":{\"id\":4,\"internalName\":\"ICE\",\"name\":\"Ice\"},"
                + "\"FLYING\":{\"id\":5,\"internalName\":\"FLYING\",\"name\":\"Flying\"},"
                + "\"POISON\":{\"id\":6,\"internalName\":\"POISON\",\"name\":\"Poison\"},"
                + "\"ELECTRIC\":{\"id\":7,\"internalName\":\"ELECTRIC\",\"name\":\"Electric\"}}}");
        write(root, "items.json", "{\"total\":1,\"items\":{"
                + "\"AIRBALLOON\":{\"id\":1,\"internalName\":\"AIRBALLOON\",\"name\":\"气球\","
                + "\"namePlural\":\"气球\",\"pocket\":1,\"price\":0,\"description\":\"\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }
}
