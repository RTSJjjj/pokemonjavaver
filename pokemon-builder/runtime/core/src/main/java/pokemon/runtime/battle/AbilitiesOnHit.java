package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.pokemon.PbsData;

/**
 * L1' (task-14's sibling batch): the {@code AbilitiesOnHit} share of
 * {@code BattleHandlers_Abilities.rb} - <b>72 registrations</b> in 7 groups, as
 * assigned by {@code stage4-l1-handler-roster.md} §1.2/§2, plus the 7
 * {@code copy} lines of §5 that belong here.
 *
 * <table border="1">
 * <caption>Groups</caption>
 * <tr><th>Group</th><th>Entries</th></tr>
 * <tr><td>{@code AbilityOnFlinch}</td><td>1</td></tr>
 * <tr><td>{@code MoveBlockingAbility}</td><td>1</td></tr>
 * <tr><td>{@code MoveImmunityTargetAbility}</td><td>18</td></tr>
 * <tr><td>{@code TargetAbilityOnHit}</td><td>34</td></tr>
 * <tr><td>{@code UserAbilityOnHit}</td><td>3</td></tr>
 * <tr><td>{@code UserAbilityEndOfMove}</td><td>10</td></tr>
 * <tr><td>{@code TargetAbilityAfterMoveUse}</td><td>5</td></tr>
 * </table>
 *
 * <h2>Conventions used below</h2>
 * <ul>
 * <li>Every handler body starts with {@code // BattleHandlers_Abilities.rb:起-止行},
 *     and each transcribed line carries its own {@code :行号}.</li>
 * <li>{@code isConst?(x,PBTypes,:FOO)} is {@link PendingApi#isConst(String, String)}
 *     (types are internal-name Strings in this runtime), {@code battle.pbDisplay}
 *     is {@link Battle#display(String)}, {@code battle.pbShowAbilitySplash} is
 *     {@link Battle#showAbilitySplash(Battler)} etc. - the task-13 names.</li>
 * <li>Move-flag/effect questions go through the L2 strategy
 *     ({@link #fx(BattleMove)} = {@code MoveEffectRegistry.of(move.function())}):
 *     {@code MoveEffectBase} holds the plugin's base-class implementations of
 *     {@code PokeBattle_Move.rb:113/123/126/129/74-79/90} and
 *     {@code Move_Usage.rb:38/40/131}, and unregistered function codes answer
 *     with {@code PokeBattle_UnimplementedMove}, so the base semantics are exact.</li>
 * <li>{@code battler.totalhp} is {@link Battler#maxHp()}; {@code battler.item}/{@code ability}
 *     are internal-name Strings ("" = none), so Ruby's {@code item>0}/{@code item==0}
 *     are {@link #hasItem(Battler)}/{@code !hasItem(...)}.</li>
 * <li>{@code PokeBattle_SceneConstants::USE_ABILITY_SPLASH} is true, so the
 *     {@code else} branch of each of those {@code if}s is dead in this project -
 *     it is transcribed anyway (roster §F.3: do not delete plugin code).</li>
 * </ul>
 *
 * <h2>登记 (not transcribed, copied call shape + comment)</h2>
 * <ul>
 * <li>{@code battle.scene.pbDamageAnimation(user)} - :1455, :1619, :1638, :1851, :4068 (scene).</li>
 * <li>{@code battle.scene.pbChangePokemon(target,target.pokemon)} - :1608, :1850 (scene).</li>
 * <li>{@code target.form = 0} - :1849: {@code Battler.form()} is read-only here, so
 *     the assignment goes through {@link PendingApi#setForm(Battler, int)}
 *     (already present for HUNGERSWITCH, BattleHandlers_Abilities:2312).</li>
 * <li>{@code battle.choices[user.index][4]} - :602 (DAZZLING): the priority {@link BattleAttackPhase} saved.</li>
 * <li>{@code battle.scene.pbReborn1Battler/pbReborn2Battler} - :4433, :4438, :4456, :4461: the revive animation only;
 *     the revive itself ({@code reborn(..)} below) is implemented.</li>
 * <li>{@code pbWait(20)} - :1436, :1459 (the scene's frame wait).</li>
 * </ul>
 *
 * <h2>Runtime gaps already registered elsewhere (the handler is transcribed, the
 * callee is not)</h2>
 * <ul>
 * <li>{@link Battle#pbCheckGlobalAbility(String)} (:478-481) currently returns
 *     {@code null}, so AFTERMATH's Damp guard (:1440) never fires.</li>
 * <li>{@link Battle#showAbilitySplash(Battler)} etc. are scene 登记 stubs; the
 *     Ruby calls that pass {@code delay}/{@code logTrigger}/{@code ability}
 *     (:1677, :1792, :1987, :2004) lose those arguments.</li>
 * </ul>
 */
final class AbilitiesOnHit {

    private AbilitiesOnHit() {
    }

    /** The L2 strategy of a move; its base class holds the plugin's flag predicates. */
    private static MoveEffect fx(BattleMove move) {
        return MoveEffectRegistry.of(move.function());
    }

    /** Ruby's {@code battler.item>0} / {@code item==0}: the runtime's held item is its internal name ("" = none). */
    private static boolean hasItem(String item) {
        return item != null && !item.isEmpty();
    }

    private static boolean hasItem(Battler battler) {
        return hasItem(battler.item);
    }

    /**
     * {@code PBAbilities.getName(getID(PBAbilities,:X))}: the ability's display
     * name from the PBS table, or {@code null} when the project has no such
     * ability (Ruby's {@code getID} is then {@code nil} and the generated
     * {@code getName} returns {@code nil} too).
     */
    private static String abilityDisplayName(Battle battle, String internalName) {
        PbsData.Ability data = battle.pbs() == null ? null : battle.pbs().ability(internalName);
        return data == null ? null : data.name;
    }

    /** {@code BattleHandlers_Abilities.rb}: 72 entries + 7 {@code copy} lines. */
    static void register() {
        registerAbilityOnFlinch();
        registerMoveBlockingAbility();
        registerMoveImmunityTargetAbility();
        registerTargetAbilityOnHit();
        registerUserAbilityOnHit();
        registerUserAbilityEndOfMove();
        registerTargetAbilityAfterMoveUse();
    }

    // ==================================================================
    // AbilityOnFlinch (1 entry)
    // ==================================================================

    private static void registerAbilityOnFlinch() {
        BattleHandlers.AbilityOnFlinch.add("STEADFAST", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:590-594
            battler.pbRaiseStatStageByAbility(PBStats.SPEED, 1, battler, true);   // :592 splashAnim 默认 true
        });
    }

    // ==================================================================
    // MoveBlockingAbility (1 entry + 2 copy lines)
    // ==================================================================

    private static void registerMoveBlockingAbility() {
        BattleHandlers.MoveBlockingAbility.add("DAZZLING", (ability, bearer, user, targets, move, battle) -> {
            // BattleHandlers_Abilities.rb:600-618
            if ((Integer) battle.choices(user.index)[4] <= 0) {                // :602
                return false;
            }
            if (!bearer.opposes(user)) {                                       // :603
                return false;
            }
            boolean ret = false;                                               // :604
            for (Battler b : targets) {                                        // :605-608
                if (!b.opposes(user)) {
                    continue;
                }
                ret = true;
            }
            return ret;                                                        // :609
        });

        BattleHandlers.MoveBlockingAbility.copy("DAZZLING", "QUEENLYMAJESTY");   // :613
        BattleHandlers.MoveBlockingAbility.copy("DAZZLING", "QUEENLYMAJESTY", "ARMORTAIL");   // :3280
    }

    // ==================================================================
    // MoveImmunityTargetAbility (18 entries + 1 copy line)
    // ==================================================================

    private static void registerMoveImmunityTargetAbility() {
        BattleHandlers.MoveImmunityTargetAbility.add("BULLETPROOF",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:619-633
            if (!fx(move).bombMove(move)) {                                    // :621
                return false;
            }
            battle.showAbilitySplash(target);                                  // :622
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                // :623
                battle.display("这不能影响" + target.pbThis(true) + "……");      // :624
            } else {
                battle.display(target.pbThis() + "的" + target.abilityName()
                        + "使" + move.name() + "无效了！");                     // :626-627
            }
            battle.hideAbilitySplash(target);                                  // :629
            return true;                                                       // :630
        });

        BattleHandlers.MoveImmunityTargetAbility.add("FLASHFIRE",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:634-659
            if (user.index == target.index) {                                  // :636
                return false;
            }
            if (!"FIRE".equals(type)) {                           // :637
                return false;
            }
            battle.showAbilitySplash(target);                                  // :638
            if (!target.effects.truthy(PBEffects.Battler.FlashFire)) {         // :639
                target.effects.set(PBEffects.Battler.FlashFire, true);         // :640
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {            // :641
                    battle.display(target.pbThis(true) + "的火属性招式威力上升！");   // :642
                } else {
                    battle.display(target.abilityName() + "使得" + target.pbThis(true)
                            + "\n火属性招式的威力上升了！");                    // :644-645
                }
            } else {
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {            // :648
                    battle.display("这不能影响" + target.pbThis(true) + "……");  // :649
                } else {
                    battle.display(target.pbThis() + "的" + target.abilityName()
                            + "使" + move.name() + "无效了！");                 // :651-652
                }
            }
            battle.hideAbilitySplash(target);                                  // :655
            return true;                                                       // :656
        });

        BattleHandlers.MoveImmunityTargetAbility.add("LIGHTNINGROD",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:660-665
            return BattleHandlerHelpers.pbBattleMoveImmunityStatAbility(
                    user, target, move, type, "ELECTRIC", PBStats.SPATK, 1, battle);   // :662
        });

        BattleHandlers.MoveImmunityTargetAbility.add("MOTORDRIVE",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:666-671
            return BattleHandlerHelpers.pbBattleMoveImmunityStatAbility(
                    user, target, move, type, "ELECTRIC", PBStats.SPEED, 1, battle);   // :668
        });

        BattleHandlers.MoveImmunityTargetAbility.add("SAPSIPPER",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:672-677
            return BattleHandlerHelpers.pbBattleMoveImmunityStatAbility(
                    user, target, move, type, "GRASS", PBStats.ATTACK, 1, battle);    // :674
        });

        // 狂暴身躯 (ANGRYBODY): project addition, pbs-only ability with no plugin script - immune to Fairy moves, Attack +1 when hit by one
        // (the Sap Sipper pattern).
        BattleHandlers.MoveImmunityTargetAbility.add("ANGRYBODY",
                (ability, user, target, move, type, battle) ->
                        BattleHandlerHelpers.pbBattleMoveImmunityStatAbility(
                                user, target, move, type, "FAIRY", PBStats.ATTACK, 1, battle));

        BattleHandlers.MoveImmunityTargetAbility.add("SOUNDPROOF",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:678-692
            if (!fx(move).soundMove(move)) {                                   // :680
                return false;
            }
            battle.showAbilitySplash(target);                                  // :681
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                // :682
                battle.display("这不能影响" + target.pbThis(true) + "……");      // :683
            } else {
                battle.display(target.pbThis() + "的" + target.abilityName()
                        + "阻止了" + move.name() + "！");                       // :685
            }
            battle.hideAbilitySplash(target);                                  // :687
            return true;                                                       // :688
        });

        BattleHandlers.MoveImmunityTargetAbility.add("STORMDRAIN",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:693-698
            return BattleHandlerHelpers.pbBattleMoveImmunityStatAbility(
                    user, target, move, type, "WATER", PBStats.SPATK, 1, battle);     // :695
        });

        BattleHandlers.MoveImmunityTargetAbility.add("TELEPATHY",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:699-714
            if (move.statusMove()) {                                           // :701
                return false;
            }
            if (user.index == target.index || target.opposes(user)) {          // :702
                return false;
            }
            battle.showAbilitySplash(target);                                  // :703
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                // :704
                battle.display(target.pbThis(true) + "避免了友方的攻击！");      // :705
            } else {
                battle.display(target.pbThis() + "因为" + target.abilityName()
                        + "避免了友方的攻击！");                               // :707-708
            }
            battle.hideAbilitySplash(target);                                  // :710
            return true;                                                       // :711
        });

        BattleHandlers.MoveImmunityTargetAbility.add("VOLTABSORB",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:715-720
            return BattleHandlerHelpers.pbBattleMoveImmunityHealAbility(
                    user, target, move, type, "ELECTRIC", battle);             // :717
        });

        BattleHandlers.MoveImmunityTargetAbility.add("WATERABSORB",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:721-726
            return BattleHandlerHelpers.pbBattleMoveImmunityHealAbility(
                    user, target, move, type, "WATER", battle);                // :723
        });

        BattleHandlers.MoveImmunityTargetAbility.add("ICEBSORB",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:727-734
            return BattleHandlerHelpers.pbBattleMoveImmunityHealAbility(
                    user, target, move, type, "ICE", battle);                  // :729
        });

        BattleHandlers.MoveImmunityTargetAbility.copy("WATERABSORB", "DRYSKIN");   // :733

        BattleHandlers.MoveImmunityTargetAbility.add("WONDERGUARD",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:735-753
            if (move.statusMove()) {                                           // :737
                return false;
            }
            // :738 type<0 - Ruby's type is the numeric calc type, -1 when unset; here it is
            // the internal-name String, null when unset.
            if (type == null || PBTypes.superEffective(target.damageState.typeMod)) {
                return false;
            }
            battle.showAbilitySplash(target);                                  // :739
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                // :740
                battle.display("这不能影响" + target.pbThis(true) + "……");      // :741
            } else {
                battle.display(target.pbThis() + "因为" + target.abilityName()
                        + "避免了伤害！");                                     // :743
            }
            battle.hideAbilitySplash(target);                                  // :745
            return true;                                                       // :746
        });

        BattleHandlers.MoveImmunityTargetAbility.add("WELLBAKEDBODY",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:2954-2960
            return BattleHandlerHelpers.pbBattleMoveImmunityStatAbility(
                    user, target, move, type, "FIRE", PBStats.DEFENSE, 2, battle);    // :2956
        });

        BattleHandlers.MoveImmunityTargetAbility.add("COMMANDER",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:2998-3006
            if (!PendingApi.isCommander(target)) {                             // :3000
                return false;
            }
            battle.display(target.pbThis() + "避免了攻击！");                   // :3001
            return true;                                                       // :3002
        });

        BattleHandlers.MoveImmunityTargetAbility.add("EARTHEATER",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:3024-3029
            return BattleHandlerHelpers.pbBattleMoveImmunityHealAbility(
                    user, target, move, type, "GROUND", battle);               // :3026
        });

        BattleHandlers.MoveImmunityTargetAbility.add("WINDRIDER",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:3054-3073
            if (!fx(move).windMove(move)) {                                    // :3056
                return false;
            }
            if (user.index == target.index) {                                  // :3057
                return false;
            }
            battle.showAbilitySplash(target);                                  // :3058
            if (target.pbCanRaiseStatStage(PBStats.ATTACK, user, move, false)) {   // :3059 (showFailMsg=false)
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {            // :3060
                    target.pbRaiseStatStage(PBStats.ATTACK, 1, user);          // :3061
                } else {
                    target.pbRaiseStatStageByCause(PBStats.ATTACK, 1, user,
                            target.abilityName());                             // :3063
                }
            } else if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :3065
                battle.display("这不能影响到" + target.pbThis(true) + "...");    // :3066
            } else {
                battle.display(target.pbThis() + "的" + target.abilityName()
                        + "使" + move.name() + "无效了！");                     // :3068
            }
            battle.hideAbilitySplash(target);                                  // :3070
            return true;                                                       // :3071
        });

        BattleHandlers.MoveImmunityTargetAbility.add("GOODASGOLD",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:3082-3096
            if (!move.statusMove()) {                                          // :3084
                return false;
            }
            if (user.index == target.index) {                                  // :3085
                return false;
            }
            battle.showAbilitySplash(target);                                  // :3086
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                // :3087
                battle.display("这不能影响到" + target.pbThis(true) + "...");    // :3088
            } else {
                battle.display(target.pbThis() + "的" + target.abilityName()
                        + "使" + move.name() + "无效了！");                     // :3090
            }
            battle.hideAbilitySplash(target);                                  // :3092
            return true;                                                       // :3093
        });

        BattleHandlers.MoveImmunityTargetAbility.add("YSNJ",
                (ability, user, target, move, type, battle) -> {
            // BattleHandlers_Abilities.rb:3904-3919
            if (move.statusMove()) {                                           // :3906
                return false;
            }
            if (battle.pbRandom(100) >= 30) {                                  // :3908 概率30%
                return false;
            }
            battle.showAbilitySplash(target);                                  // :3909
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                // :3910
                battle.display(target.pbThis() + "免疫了" + user.pbThis() + "使出的招式！");   // :3911
            } else {
                battle.display(target.pbThis() + "的" + target.abilityName()
                        + "，使" + move.name() + "无效化了！");                 // :3913
            }
            target.pbRaiseStatStage(PBStats.SPATK, 1, target);                 // :3915
            battle.hideAbilitySplash(target);                                  // :3916
            return true;                                                       // :3917
        });
    }

    // ==================================================================
    // TargetAbilityOnHit (34 entries + 2 copy lines)
    // ==================================================================

    private static void registerTargetAbilityOnHit() {
        BattleHandlers.TargetAbilityOnHit.add("AFTERMATH", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1433-1462
            if (!target.fainted()) {                                           // :1435
                return;
            }
            if (!fx(move).pbContactMove(move, user)) {                         // :1436
                return;
            }
            battle.showAbilitySplash(target);                                  // :1437
            if (!battle.moldBreaker) {                                         // :1438
                Battler dampBattler = battle.pbCheckGlobalAbility("DAMP");     // :1439
                if (dampBattler != null) {                                     // :1440
                    battle.showAbilitySplash(dampBattler);                     // :1441
                    if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {        // :1442
                        battle.display(target.pbThis() + "不能使用" + target.abilityName() + "了！");   // :1443
                    } else {
                        battle.display(dampBattler.pbThis(true) + "的" + dampBattler.abilityName()
                                + "使得" + target.pbThis() + "无法使用" + target.abilityName()
                                + "了！");                                     // :1445-1446
                    }
                    battle.hideAbilitySplash(dampBattler);                     // :1448
                    battle.hideAbilitySplash(target);                          // :1449
                    return;                                                    // :1450
                }
            }
            if (user.takesIndirectDamage(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)   // :1453
                    && user.affectedByContactEffect(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :1454
                // 登记: BattleHandlers_Abilities:1455 battle.scene.pbDamageAnimation(user)（场景未建模）
                user.pbReduceHP(user.maxHp() / 4, false, true, true);          // :1456 (anim=false)
                battle.display(user.pbThis() + "陷入了爆炸中！");                // :1457
            }
            battle.hideAbilitySplash(target);                                  // :1459
        });

        BattleHandlers.TargetAbilityOnHit.add("ANGERPOINT", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1463-1480
            if (!target.damageState.critical) {                                // :1465
                return;
            }
            if (!target.pbCanRaiseStatStage(PBStats.ATTACK, target)) {         // :1466
                return;
            }
            battle.showAbilitySplash(target);                                  // :1467
            target.setStage(PBStats.ATTACK, 6);                                // :1468 stages[PBStats::ATTACK] = 6
            target.statsRaisedThisRound = true;                                // :1469
            battle.commonAnimation("StatUp", target);                          // :1470
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                // :1471
                battle.display(target.pbThis() + "最大化了" + PBStats.getName(PBStats.ATTACK) + "！");   // :1472
            } else {
                battle.display(target.pbThis() + "的" + target.abilityName() + "最大化了"
                        + PBStats.getName(PBStats.ATTACK) + "！");             // :1474-1475
            }
            battle.hideAbilitySplash(target);                                  // :1477
        });

        BattleHandlers.TargetAbilityOnHit.add("CURSEDBODY", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1481-1509
            if (user.fainted()) {                                              // :1483
                return;
            }
            if (user.effects.intVal(PBEffects.Battler.Disable) > 0) {           // :1484
                return;
            }
            // :1486-1490 user.eachMove { |m| next if m.id!=user.lastRegularMoveUsed; ... }
            // eachMove is @moves.each (PokeBattle_Battler:543); a blank slot (id==0) can never
            // equal lastRegularMoveUsed, so skipping the runtime's null slots is exact.
            BattleMove regularMove = null;
            for (BattleMove m : user.moveSlots()) {
                if (m == null) {
                    continue;
                }
                if (user.lastRegularMoveUsed == null
                        || !user.lastRegularMoveUsed.equals(m.internalName())) {
                    // :1487 m.id!=user.lastRegularMoveUsed - the runtime stores the internal name
                    continue;
                }
                regularMove = m;
                break;
            }
            if (regularMove == null
                    || (user.moveSlotPp(user.moveSlotIndex(regularMove)) == 0 && user.moveTotalPp(regularMove) > 0)) {   // :1491
                return;
            }
            if (battle.pbRandom(100) >= 30) {                                  // :1492
                return;
            }
            battle.showAbilitySplash(target);                                  // :1493
            if (!fx(move).pbMoveFailedAromaVeil(move, user, target,
                    PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {           // :1494
                user.effects.set(PBEffects.Battler.Disable, 3);                // :1495
                user.effects.set(PBEffects.Battler.DisableMove, regularMove.id());   // :1496
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {            // :1497
                    battle.display(user.pbThis() + "的" + regularMove.name() + "被禁用了！");   // :1498
                } else {
                    battle.display(user.pbThis() + "的" + regularMove.name() + "被"
                            + target.pbThis(true) + "的" + target.abilityName()
                            + "禁用了！");                                     // :1500-1501
                }
                battle.hideAbilitySplash(target);                              // :1503
                user.pbItemStatusCureCheck(0, false);                      // :1504 (Ruby 无参 = forcedItem 0, fling false)
            }
            battle.hideAbilitySplash(target);                                  // :1506
        });

        BattleHandlers.TargetAbilityOnHit.add("CUTECHARM", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1510-1528
            if (target.fainted()) {                                            // :1512
                return;
            }
            if (!fx(move).pbContactMove(move, user)) {                         // :1513
                return;
            }
            if (battle.pbRandom(100) >= 30) {                                  // :1514
                return;
            }
            battle.showAbilitySplash(target);                                  // :1515
            if (user.pbCanAttract(target, PokeBattle_SceneConstants.USE_ABILITY_SPLASH)   // :1516
                    && user.affectedByContactEffect(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :1517
                String msg = null;                                             // :1518
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :1519
                    msg = target.pbThis() + "的" + target.abilityName() + "让"
                            + user.pbThis(true) + "坠入爱河了！";               // :1521-1522
                }
                user.pbAttract(target, msg);                                   // :1523
            }
            battle.hideAbilitySplash(target);                                  // :1525
        });

        BattleHandlers.TargetAbilityOnHit.add("EFFECTSPORE", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1529-1576
            // NOTE (plugin comment :1531-1533): the 30% is the chance of TRIGGERING,
            // not of inflicting - it can try and fail against an immune user.
            if (!fx(move).pbContactMove(move, user)) {                         // :1534
                return;
            }
            if (battle.pbRandom(100) >= 30) {                                  // :1535
                return;
            }
            int r = battle.pbRandom(3);                                        // :1536
            if (r == 0 && user.asleep()) {                                     // :1537
                return;
            }
            if (r == 1 && user.poisoned()) {                                   // :1538
                return;
            }
            if (r == 2 && user.paralyzed()) {                                  // :1539
                return;
            }
            battle.showAbilitySplash(target);                                  // :1540
            if (user.affectedByPowder(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)   // :1541
                    && user.affectedByContactEffect(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :1542
                switch (r) {                                                   // :1543
                    case 0:                                                    // :1544
                        if (user.pbCanSleep(target, PokeBattle_SceneConstants.USE_ABILITY_SPLASH, null, false)) {   // :1545
                            String msg = null;                                 // :1546
                            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {   // :1547
                                msg = target.pbThis() + "的" + target.abilityName() + "让"
                                        + user.pbThis(true) + "睡着了！";       // :1549-1550
                            }
                            user.pbSleep(msg);                     // :1551
                        }
                        break;
                    case 1:                                                    // :1553
                        if (user.pbCanPoison(target, PokeBattle_SceneConstants.USE_ABILITY_SPLASH, null)) {   // :1554
                            String msg = null;                                 // :1555
                            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {   // :1556
                                msg = target.pbThis() + "的" + target.abilityName() + "使"
                                        + user.pbThis(true) + "中毒了！";       // :1558-1559
                            }
                            user.pbPoison(target, msg, false);                 // :1560
                        }
                        break;
                    case 2:                                                    // :1562
                        if (user.pbCanParalyze(target, PokeBattle_SceneConstants.USE_ABILITY_SPLASH, null)) {   // :1563
                            String msg = null;                                 // :1564
                            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {   // :1565
                                msg = target.pbThis() + "的" + target.abilityName() + "使"
                                        + user.pbThis(true) + "麻痹了！\n"
                                        + user.pbThis(true) + "有可能无法行动！";   // :1567-1568
                            }
                            user.pbParalyze(target, msg);                      // :1569
                        }
                        break;
                    default:
                        break;
                }
            }
            battle.hideAbilitySplash(target);                                  // :1573
        });

        BattleHandlers.TargetAbilityOnHit.add("FLAMEBODY", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1577-1593
            if (!fx(move).pbContactMove(move, user)) {                         // :1579
                return;
            }
            if (user.burned() || battle.pbRandom(100) >= 30) {                 // :1580
                return;
            }
            battle.showAbilitySplash(target);                                  // :1581
            if (user.pbCanBurn(target, PokeBattle_SceneConstants.USE_ABILITY_SPLASH, null)   // :1582
                    && user.affectedByContactEffect(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :1583
                String msg = null;                                             // :1584
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :1585
                    msg = target.pbThis() + "的" + target.abilityName() + "使"
                            + user.pbThis(true) + "灼伤了！";                    // :1587
                }
                user.pbBurn(target, msg);                                      // :1588
            }
            battle.hideAbilitySplash(target);                                  // :1590
        });

        BattleHandlers.TargetAbilityOnHit.add("GOOEY", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1594-1602
            if (!fx(move).pbContactMove(move, user)) {                         // :1596
                return;
            }
            user.pbLowerStatStageByAbility(PBStats.SPEED, 1, target, true, true);   // :1597
        });

        BattleHandlers.TargetAbilityOnHit.copy("GOOEY", "TANGLINGHAIR");        // :1601

        BattleHandlers.TargetAbilityOnHit.add("ILLUSION", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1603-1613
            // NOTE (:1605): this intentionally doesn't show the ability splash.
            if (!target.effects.truthy(PBEffects.Battler.Illusion)) {          // :1606
                return;
            }
            pokemon.runtime.pokemon.Pokemon shownBefore = target.visiblePokemon();
            target.effects.set(PBEffects.Battler.Illusion, null);              // :1607 = nil
            target.queueLookChange(shownBefore);                               // :1608 battle.scene.pbChangePokemon(target,target.pokemon)
            battle.display(target.pbThis() + "的幻象被识破了！");                // :1609
            battle.pbSetSeen(target);                                          // :1610
        });

        BattleHandlers.TargetAbilityOnHit.add("INNARDSOUT", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1614-1631
            if (!target.fainted() || user.dummy) {                             // :1616
                return;
            }
            battle.showAbilitySplash(target);                                  // :1617
            if (user.takesIndirectDamage(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :1618
                // 登记: BattleHandlers_Abilities:1619 battle.scene.pbDamageAnimation(user)（场景未建模）
                user.pbReduceHP(target.damageState.hpLost, false, true, true);  // :1620 (anim=false)
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {            // :1621
                    battle.display(user.pbThis() + "受到了伤害！");              // :1622
                } else {
                    battle.display(user.pbThis() + "被" + target.pbThis(true) + "的"
                            + target.abilityName() + "伤害了！");               // :1624-1625
                }
            }
            battle.hideAbilitySplash(target);                                  // :1628
        });

        BattleHandlers.TargetAbilityOnHit.add("IRONBARBS", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1632-1656
            if (!fx(move).pbContactMove(move, user)) {                         // :1634
                return;
            }
            battle.showAbilitySplash(target);                                  // :1635
            if (user.takesIndirectDamage(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)   // :1636
                    && user.affectedByContactEffect(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :1637
                // 登记: BattleHandlers_Abilities:1638 battle.scene.pbDamageAnimation(user)（场景未建模）
                if (user.pokemon.battleRank > 2) {                             // :1639
                    user.pbReduceHP(user.maxHp() / 40, false, true, true);     // :1640
                } else {
                    user.pbReduceHP(user.maxHp() / 8, false, true, true);      // :1642
                }
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {            // :1644
                    battle.display(user.pbThis() + "受到了伤害！");              // :1645
                } else {
                    battle.display(user.pbThis() + "被" + target.pbThis(true) + "的"
                            + target.abilityName() + "伤害了！");               // :1647-1648
                }
            }
            battle.hideAbilitySplash(target);                                  // :1651
        });

        BattleHandlers.TargetAbilityOnHit.copy("IRONBARBS", "ROUGHSKIN");       // :1655

        BattleHandlers.TargetAbilityOnHit.add("JUSTIFIED", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1657-1663
            if (!"DARK".equals(move.calcType())) {                 // :1659
                return;
            }
            target.pbRaiseStatStageByAbility(PBStats.ATTACK, 1, target, true);  // :1660
        });

        BattleHandlers.TargetAbilityOnHit.add("MUMMY", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1664-1696
            if (!fx(move).pbContactMove(move, user)) {                         // :1666
                return;
            }
            if (user.fainted()) {                                              // :1667
                return;
            }
            if (user.unstoppableAbility(null)) {                                // :1668 (Ruby 无参 = abil nil)
                return;
            }
            // :1669 getConst(PBAbilities,:MUMMY)/:LINGERINGAROMA - internal names in this runtime.
            String[] abilities = { "MUMMY", "LINGERINGAROMA" };
            if ("MUMMY".equals(user.ability) || "LINGERINGAROMA".equals(user.ability)) {   // :1670 include?
                return;
            }
            if (user.hasActiveItem("ABILITYSHIELD")) {                          // :1671
                return;
            }
            String oldAbil = null;                                              // :1672 oldAbil = -1
            if (user.opposes(target)) {                                         // :1673
                battle.showAbilitySplash(target);
            }
            if (user.affectedByContactEffect(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :1674
                oldAbil = user.ability;                                         // :1675
                if (user.opposes(target)) {                                     // :1676
                    battle.showAbilitySplash(user);                             // Ruby: (user,true,false)
                }
                user.ability = ability;                                         // :1677
                if (user.opposes(target)) {                                     // :1678
                    battle.replaceAbilitySplash(user);
                }
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {             // :1679
                    String msg = null;                                          // :1680-1686
                    if ("MUMMY".equals(ability)) {                              // :1681 getConst(PBAbilities,:MUMMY)
                        msg = user.pbThis() + "的特性变为" + user.abilityName() + "！";   // :1682
                    } else if ("LINGERINGAROMA".equals(ability)) {              // :1683
                        msg = "一股甩不掉的气味笼罩着" + user.pbThis(true) + "！";   // :1684
                    }
                    battle.display(msg);                                        // :1686
                } else {
                    battle.display(user.pbThis() + "的特性因为" + user.abilityName()
                            + "\n而变为了" + target.pbThis(true) + "！");        // :1688-1689
                }
                if (user.opposes(target)) {                                     // :1691
                    battle.hideAbilitySplash(user);
                }
            }
            if (user.opposes(target)) {                                         // :1693
                battle.hideAbilitySplash(target);
            }
            if (oldAbil != null) {                                              // :1694 oldAbil>=0
                user.pbOnAbilityChanged(oldAbil);
            }
        });

        BattleHandlers.TargetAbilityOnHit.copy("MUMMY", "LINGERINGAROMA");      // :3205

        BattleHandlers.TargetAbilityOnHit.add("POISONPOINT", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1697-1713
            if (!fx(move).pbContactMove(move, user)) {                         // :1699
                return;
            }
            if (user.poisoned() || battle.pbRandom(100) >= 30) {               // :1700
                return;
            }
            battle.showAbilitySplash(target);                                  // :1701
            if (user.pbCanPoison(target, PokeBattle_SceneConstants.USE_ABILITY_SPLASH, null)   // :1702
                    && user.affectedByContactEffect(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :1703
                String msg = null;                                             // :1704
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :1705
                    msg = target.pbThis() + "的" + target.abilityName() + "使"
                            + user.pbThis(true) + "中毒了！";                    // :1707
                }
                user.pbPoison(target, msg, false);                             // :1708
            }
            battle.hideAbilitySplash(target);                                  // :1710
        });

        BattleHandlers.TargetAbilityOnHit.add("RATTLED", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1714-1722
            if (!"BUG".equals(move.calcType())                     // :1716
                    && !"DARK".equals(move.calcType())             // :1717
                    && !"GHOST".equals(move.calcType())) {         // :1718
                return;
            }
            target.pbRaiseStatStageByAbility(PBStats.SPEED, 1, target, true);   // :1719
        });

        BattleHandlers.TargetAbilityOnHit.add("STAMINA", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1723-1728
            target.pbRaiseStatStageByAbility(PBStats.DEFENSE, 1, target, true);   // :1725
        });

        // NOTE: the plugin's proc names these parameters |ability,target,battler,move,battle|,
        // i.e. positionally they are the interface's (user, target) - SANDSPIT starts the
        // weather from the move's TARGET, which is the ability's holder (:1730-1731).
        BattleHandlers.TargetAbilityOnHit.add("SANDSPIT", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1729-1734
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Sandstorm, target, battle);   // :1731
        });

        BattleHandlers.TargetAbilityOnHit.add("STATIC", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1735-1752
            if (!fx(move).pbContactMove(move, user)) {                         // :1737
                return;
            }
            if (user.paralyzed() || battle.pbRandom(100) >= 30) {              // :1738
                return;
            }
            battle.showAbilitySplash(target);                                  // :1739
            if (user.pbCanParalyze(target, PokeBattle_SceneConstants.USE_ABILITY_SPLASH, null)   // :1740
                    && user.affectedByContactEffect(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :1741
                String msg = null;                                             // :1742
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :1743
                    msg = target.pbThis() + "的" + target.abilityName() + "使"
                            + user.pbThis(true) + "麻痹了！\n"
                            + user.pbThis(true) + "有可能无法行动！";           // :1745-1746
                }
                user.pbParalyze(target, msg);                                  // :1747
            }
            battle.hideAbilitySplash(target);                                  // :1749
        });

        BattleHandlers.TargetAbilityOnHit.add("WATERCOMPACTION", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1753-1759
            if (!"WATER".equals(move.calcType())) {                // :1755
                return;
            }
            target.pbRaiseStatStageByAbility(PBStats.DEFENSE, 2, target, true);   // :1756
        });

        BattleHandlers.TargetAbilityOnHit.add("WEAKARMOR", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1760-1772
            if (!fx(move).physicalMove(move, null)) {                          // :1762
                return;
            }
            if (!target.pbCanLowerStatStage(PBStats.DEFENSE, target)            // :1763
                    && !target.pbCanRaiseStatStage(PBStats.SPEED, target)) {    // :1764
                return;
            }
            battle.showAbilitySplash(target);                                  // :1765
            target.pbLowerStatStageByAbility(PBStats.DEFENSE, 1, target, false, true);   // :1766
            target.pbRaiseStatStageByAbility(PBStats.SPEED,                     // :1767
                    Battle.NEWEST_BATTLE_MECHANICS ? 2 : 1, target, false);     // :1768
            battle.hideAbilitySplash(target);                                  // :1769
        });

        BattleHandlers.TargetAbilityOnHit.add("STEAMENGINE", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1773-1780
            if (!"FIRE".equals(move.calcType())                    // :1775
                    && !"WATER".equals(move.calcType())) {         // :1776
                return;
            }
            target.pbRaiseStatStageByAbility(PBStats.SPEED, 6, target, true);    // :1777
        });

        BattleHandlers.TargetAbilityOnHit.add("WANDERINGSPIRIT", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1781-1816
            if (!fx(move).pbContactMove(move, user)) {                         // :1783
                return;
            }
            if (user.fainted()) {                                              // :1784
                return;
            }
            if (user.uncopyableAbility(null)) {                                 // :1785 (Ruby 无参)
                return;
            }
            if (user.hasActiveItem("ABILITYSHIELD") || target.hasActiveItem("ABILITYSHIELD")) {   // :1786
                return;
            }
            String oldAbil = null;                                              // :1787 oldAbil = -1
            if (user.opposes(target)) {                                         // :1788
                battle.showAbilitySplash(target);
            }
            if (user.affectedByContactEffect(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :1789
                oldAbil = user.ability;                                         // :1790
                if (user.opposes(target)) {                                     // :1791
                    battle.showAbilitySplash(user);                             // Ruby: (user,true,false)
                }
                user.ability = "WANDERINGSPIRIT";                               // :1792 getConst(PBAbilities,:WANDERINGSPIRIT)
                target.ability = oldAbil;                                       // :1793
                if (user.opposes(target)) {                                     // :1794
                    battle.replaceAbilitySplash(user);                          // :1795
                    battle.replaceAbilitySplash(target);                        // :1796
                }
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {             // :1798
                    battle.display(user.pbThis() + "的特性变为" + user.abilityName() + "！");   // :1799
                } else {
                    battle.display(user.pbThis() + "的特性变为" + user.abilityName()
                            + "，\n因为" + target.pbThis(true) + "！");           // :1801-1802
                }
                battle.hideAbilitySplash(user);                                 // :1805
            }
            if (user.opposes(target)) {                                         // :1807
                battle.hideAbilitySplash(target);
            }
            if (oldAbil != null) {                                              // :1808 oldAbil>=0
                user.pbOnAbilityChanged(oldAbil);                               // :1809
                target.pbOnAbilityChanged("WANDERINGSPIRIT");                   // :1810 getConst(PBAbilities,:WANDERINGSPIRIT)
            }
        });

        BattleHandlers.TargetAbilityOnHit.add("PERISHBODY", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1817-1829
            if (!fx(move).pbContactMove(move, user)) {                         // :1819
                return;
            }
            if (!user.affectedByContactEffect(true)) {                          // :1820 (Ruby 无参 = showMsg true)
                return;
            }
            if (user.effects.intVal(PBEffects.Battler.PerishSong) > 0) {        // :1821
                return;
            }
            battle.showAbilitySplash(target);                                  // :1822
            battle.display("Both Pokémon will faint in three turns!");         // :1823 _INTL 原文
            user.effects.set(PBEffects.Battler.PerishSong, 3);                 // :1824
            if (target.effects.intVal(PBEffects.Battler.PerishSong) == 0) {     // :1825
                target.effects.set(PBEffects.Battler.PerishSong, 3);
            }
            battle.hideAbilitySplash(target);                                  // :1826
        });

        BattleHandlers.TargetAbilityOnHit.add("COTTONDOWN", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1830-1842
            battle.showAbilitySplash(target);                                  // :1832
            target.eachOpposing(b -> b.pbLowerStatStage(PBStats.SPEED, 1, target));   // :1833-1835
            target.eachAlly(b -> b.pbLowerStatStage(PBStats.SPEED, 1, target));       // :1836-1838
            battle.hideAbilitySplash(target);                                  // :1839
        });

        BattleHandlers.TargetAbilityOnHit.add("GULPMISSILE", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1843-1868
            if (target.form() == 0) {                                          // :1845
                return;
            }
            if (target.isSpecies("CRAMORANT")) {                               // :1846 isConst?(target.species,PBSpecies,:CRAMORANT)
                battle.showAbilitySplash(target);                              // :1847
                int gulpform = target.form();                                  // :1848
                PendingApi.setForm(target, 0);                                 // :1849 target.form = 0
                // 登记: BattleHandlers_Abilities:1850 battle.scene.pbChangePokemon(target,target.pokemon)
                // 登记: BattleHandlers_Abilities:1851 battle.scene.pbDamageAnimation(user)
                if (user.takesIndirectDamage(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :1852
                    user.pbReduceHP(user.maxHp() / 4, false, true, true);      // :1853 (anim=false)
                }
                if (gulpform == 1) {                                           // :1855
                    user.pbLowerStatStageByAbility(PBStats.DEFENSE, 1, target, false, true);   // :1856
                } else if (gulpform == 2) {                                    // :1857
                    String msg = null;                                         // :1858
                    user.pbParalyze(target, msg);                              // :1859
                }
                battle.hideAbilitySplash(target);                              // :1861
            }
        });

        BattleHandlers.TargetAbilityOnHit.add("TOXICDEBRIS", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:2923-2937
            if (!fx(move).physicalMove(move, null)) {                          // :2925
                return;
            }
            if (target.damageState.substitute) {                               // :2926
                return;
            }
            if (target.pbOpposingSide().effects.intVal(PBEffects.Side.ToxicSpikes) >= 2) {   // :2927
                return;
            }
            battle.showAbilitySplash(target);                                  // :2928
            target.pbOpposingSide().effects.add(PBEffects.Side.ToxicSpikes, 1);   // :2929 += 1
            // :2930 battle.pbAnimation(getID(PBMoves,:TOXICSPIKES), target, target.pbDirectOpposing)
            PbsData.Move toxicSpikes = battle.pbs() == null ? null : battle.pbs().move("TOXICSPIKES");
            Array<Battler> animTargets = new Array<>();
            animTargets.add(target.pbDirectOpposing(false));                    // Ruby 无参 = unfaintedOnly false
            battle.animation(toxicSpikes == null ? 0 : toxicSpikes.id, target, animTargets, 0);   // :793 hitNum=0
            battle.display(target.pbOpposingTeam(true) + "脚下散落着毒菱！");     // :2931
            battle.hideAbilitySplash(target);                                  // :2932
        });

        BattleHandlers.TargetAbilityOnHit.add("SEEDSOWER", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:2945-2953
            if (!fx(move).damagingMove(move)) {                                // :2947
                return;
            }
            if (battle.field.terrain == PBBattleTerrains.Grassy) {              // :2948
                return;
            }
            battle.showAbilitySplash(target);                                  // :2949
            // 登记: BattleHandlers_Abilities:2950 battle.pbStartTerrain(target,PBBattleTerrains::Grassy)
            //   （Battle 没有 pbStartTerrain，§B 已定为登记）
        });

        BattleHandlers.TargetAbilityOnHit.add("THERMALEXCHANGE", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:2961-2969
            if (!"FIRE".equals(move.calcType())) {                 // :2963
                return;
            }
            target.pbRaiseStatStageByAbility(PBStats.ATTACK, 1, target, true);   // :2964
        });

        BattleHandlers.TargetAbilityOnHit.add("ELECTROMORPHOSIS", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:3030-3041
            if (target.fainted()) {                                            // :3032
                return;
            }
            if (target.effects.intVal(PBEffects.Battler.Charge) > 0) {          // :3033
                return;
            }
            battle.showAbilitySplash(target);                                  // :3034
            target.effects.set(PBEffects.Battler.Charge, 2);                   // :3035
            battle.display("由于受到" + move.name() + "攻击，\n"
                    + target.pbThis(true) + "充满了力量！");                     // :3036
            battle.hideAbilitySplash(target);                                  // :3037
        });

        BattleHandlers.TargetAbilityOnHit.add("WINDPOWER", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:3042-3053
            if (!fx(move).windMove(move)) {                                    // :3044
                return;
            }
            if (target.effects.intVal(PBEffects.Battler.Charge) > 0) {          // :3045
                return;
            }
            battle.showAbilitySplash(target);                                  // :3046
            target.effects.set(PBEffects.Battler.Charge, 2);                   // :3047
            battle.display("由于受到" + move.name() + "攻击，\n"
                    + target.pbThis(true) + "充满了力量！");                     // :3048
            battle.hideAbilitySplash(target);                                  // :3049
        });

        BattleHandlers.TargetAbilityOnHit.add("GHOSTRAMPAGE", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:3646-3653
            if (!"DARK".equals(move.calcType())) {                 // :3648
                return;
            }
            target.pbRaiseStatStageByAbility(PBStats.ATTACK, 2, target, true);   // :3649
        });

        BattleHandlers.TargetAbilityOnHit.add("SLEEPSOUNDLY", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:3788-3800
            if (!target.isSpecies("ROSEDRAGON")) {                             // :3790
                return;
            }
            if (!target.asleep()) {                                            // :3791
                return;
            }
            battle.showAbilitySplash(target);                                  // :3792
            target.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :3793
            target.pbCheckFormOnStatusChange();                                // :3794
            battle.hideAbilitySplash(target);                                  // :3795
        });

        BattleHandlers.TargetAbilityOnHit.add("PHANTOMROCK", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:3801-3812
            if (!fx(move).physicalMove(move, null)) {                          // :3803
                return;
            }
            if (!target.pbCanRaiseStatStage(PBStats.SPEED, target)) {           // :3804
                return;
            }
            battle.showAbilitySplash(target);                                  // :3805
            target.setStage(PBStats.SPEED, 6);                                 // :3806 stages[PBStats::SPEED] = 6
            battle.commonAnimation("StatUp", target);                          // :3807
            battle.hideAbilitySplash(target);                                  // :3808
        });

        BattleHandlers.TargetAbilityOnHit.add("SACREDREBORN", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:4428-4449
            if (!target.isSpecies("SUGARDEVOIR")) {                              // :4430
                return;
            }
            reborn(target, battle);
        });

        BattleHandlers.TargetAbilityOnHit.add("ABYSSREBORN", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:4451-4472
            if (!target.isSpecies("SUJINRAKU")) {                                // :4453
                return;
            }
            reborn(target, battle);
        });
    }

    /**
     * The shared body of SACREDREBORN / ABYSSREBORN (BattleHandlers_Abilities.rb:4431-4446 / :4454-4469): a Pokemon that has just
     * fainted comes back once per battle with half its HP, status and confusion cured.
     * 登记: the scene calls (:4433 pbReborn1Battler, :4438 pbReborn2Battler) and pbWait(20) - animation only.
     */
    private static void reborn(Battler target, Battle battle) {
        if (!target.fainted() || target.reborn()) {                              // :4431
            return;
        }
        target.setReborn();                                                      // :4432
        battle.display(target.pbThis() + "倒下了...？");                          // :4434
        battle.showAbilitySplash(target);                                        // :4437
        target.pbRecoverHP(target.maxHp() / 2, false, false);                  // :4440
        target.pbCureStatus(false);                                              // :4441
        target.pbCureConfusion();                                                // :4442
        battle.display(target.pbThis() + "从濒死中复活了！");                     // :4444
        battle.hideAbilitySplash(target);                                        // :4446
    }

    // ==================================================================
    // UserAbilityOnHit (3 entries)
    // ==================================================================

    private static void registerUserAbilityOnHit() {
        BattleHandlers.UserAbilityOnHit.add("POISONTOUCH", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:1869-1895
            if (!fx(move).contactMove(move)) {                                 // :1871
                return;
            }
            if (battle.pbRandom(100) >= 30) {                                  // :1872
                return;
            }
            if (target.hasActiveItem("COVERTCLOAK")) {                         // :1873
                return;
            }
            battle.showAbilitySplash(user);                                    // :1874
            if (target.hasActiveAbility("SHIELDDUST") && !battle.moldBreaker) {   // :1875
                battle.showAbilitySplash(target);                              // :1876
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :1877
                    battle.display(target.pbThis() + "没有受到影响！");          // :1878
                }
                battle.hideAbilitySplash(target);                              // :1880
            } else if (target.pbCanPoison(user, PokeBattle_SceneConstants.USE_ABILITY_SPLASH, null)) {   // :1881
                String msg = null;                                             // :1882
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :1883
                    msg = user.pbThis() + "的" + user.abilityName() + "使"
                            + target.pbThis(true) + "中毒了！";                  // :1884
                }
                target.pbPoison(user, msg, false);                             // :1886
            }
            battle.hideAbilitySplash(user);                                    // :1888
        });

        BattleHandlers.UserAbilityOnHit.add("TOXICCHAIN", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:3137-3158
            if (target.fainted()) {                                            // :3139
                return;
            }
            if (battle.pbRandom(100) >= 30) {                                  // :3140
                return;
            }
            if (target.hasActiveItem("COVERTCLOAK")) {                         // :3141
                return;
            }
            battle.showAbilitySplash(user);                                    // :3142
            if (target.hasActiveAbility("SHIELDDUST") && !battle.moldBreaker) {   // :3143
                battle.showAbilitySplash(target);                              // :3144
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :3145
                    battle.display(target.pbThis() + "不受影响！");              // :3146
                }
                battle.hideAbilitySplash(target);                              // :3148
            } else if (target.pbCanPoison(user, PokeBattle_SceneConstants.USE_ABILITY_SPLASH, null)) {   // :3149
                String msg = null;                                             // :3150
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :3151
                    msg = target.pbThis() + "中剧毒了！";                        // :3152
                }
                target.pbPoison(user, msg, true);                              // :3154
            }
            battle.hideAbilitySplash(user);                                    // :3156
        });

        BattleHandlers.UserAbilityOnHit.add("BIYIHUANGYAN", (ability, user, target, move, battle) -> {
            // BattleHandlers_Abilities.rb:4368-4395
            // :4367 1. 火与光属性招式必定造成灼伤（100%触发）
            if (!"FIRE".equals(move.calcType())                   // :4371
                    && !"LIGHT".equals(move.calcType())) {         // :4371
                return;
            }
            if (!fx(move).damagingMove(move)) {                                // :4372 只对攻击招式生效
                return;
            }
            if (target.fainted()) {                                            // :4373
                return;
            }
            if (target.burned()) {                                             // :4374 已经灼伤则不重复触发
                return;
            }
            if (target.hasActiveItem("COVERTCLOAK")) {                         // :4375
                return;
            }
            battle.showAbilitySplash(user);                                    // :4377
            if (target.hasActiveAbility("SHIELDDUST") && !battle.moldBreaker) {   // :4378
                battle.showAbilitySplash(target);                              // :4379
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :4380
                    battle.display(target.pbThis() + "没有受到影响！");          // :4381
                }
                battle.hideAbilitySplash(target);                              // :4383
            } else if (target.pbCanBurn(user, PokeBattle_SceneConstants.USE_ABILITY_SPLASH, null)) {   // :4384
                String msg = null;                                             // :4385
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {           // :4386
                    msg = user.pbThis() + "的" + user.abilityName() + "使"
                            + target.pbThis(true) + "灼伤了！";                  // :4387
                }
                target.pbBurn(user, msg);                                      // :4389
            }
            battle.hideAbilitySplash(user);                                    // :4391
        });
    }

    // ==================================================================
    // UserAbilityEndOfMove (10 entries + 1 copy line)
    // ==================================================================

    private static void registerUserAbilityEndOfMove() {
        BattleHandlers.UserAbilityEndOfMove.add("BEASTBOOST", (ability, user, targets, move, battle) -> {
            // BattleHandlers_Abilities.rb:1896-1917
            if (battle.pbAllFainted(user.idxOpposingSide())) {                  // :1898
                return;
            }
            int numFainted = 0;                                                // :1899
            for (Battler b : targets) {                                        // :1900
                if (b.damageState.fainted) {
                    numFainted++;
                }
            }
            if (numFainted == 0) {                                             // :1901
                return;
            }
            int[] userStats = user.plainStats();                               // :1902
            int highestStatValue = 0;                                          // :1903
            for (int value : userStats) {                                      // :1904-1907
                // :1905 next if !value - Ruby's plainStats leaves the HP slot nil; this
                // runtime leaves it 0 and a real battle stat is never 0, so 0 is that nil.
                if (value == 0) {
                    continue;
                }
                if (highestStatValue < value) {
                    highestStatValue = value;
                }
            }
            for (int s : PBStats.EACH_MAIN_BATTLE_STAT) {                      // :1908 eachMainBattleStat
                if (userStats[s] < highestStatValue) {                         // :1909
                    continue;
                }
                if (user.pbCanRaiseStatStage(s, user)) {                       // :1910
                    user.pbRaiseStatStageByAbility(s, numFainted, user, true);   // :1911
                }
                break;                                                         // :1913
            }
        });

        BattleHandlers.UserAbilityEndOfMove.add("MAGICIAN", (ability, user, targets, move, battle) -> {
            // BattleHandlers_Abilities.rb:1918-1957
            if (!battle.futureSight) {                                         // :1920
                return;
            }
            if (!fx(move).pbDamagingMove(move)) {                              // :1921
                return;
            }
            if (hasItem(user)) {                                               // :1922 user.item>0
                return;
            }
            if (battle.wildBattle() && user.foe) {                             // :1923 user.opposes?
                return;
            }
            for (Battler b : targets) {                                        // :1924
                if (b.damageState.unaffected || b.damageState.substitute) {     // :1925
                    continue;
                }
                if (!hasItem(b)) {                                             // :1926 b.item==0
                    continue;
                }
                if (b.unlosableItem(b.item) || user.unlosableItem(b.item)) {    // :1927
                    continue;
                }
                battle.showAbilitySplash(user);                                // :1928
                if (b.hasActiveAbility("STICKYHOLD")) {                        // :1929
                    if (user.opposes(b)) {                                     // :1930
                        battle.showAbilitySplash(b);
                    }
                    if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {        // :1931
                        battle.display(b.pbThis() + "的道具不能偷窃！");          // :1932
                    }
                    if (user.opposes(b)) {                                     // :1934
                        battle.hideAbilitySplash(b);
                    }
                    continue;                                                  // :1935
                }
                user.item = b.item;                                            // :1937
                b.item = "";                                                   // :1938 b.item = 0
                b.effects.set(PBEffects.Battler.Unburden, true);               // :1939
                if (battle.wildBattle() && !hasItem(user.initialItem)           // :1940 user.initialItem==0
                        && b.initialItem.equals(user.item)) {                   // :1940 b.initialItem==user.item
                    user.setInitialItem(user.item);                            // :1941
                    b.setInitialItem("");                                      // :1942 b.setInitialItem(0)
                }
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {            // :1944
                    battle.display(user.pbThis() + "偷窃了" + b.pbThis(true) + "的"
                            + user.itemName() + "！");                          // :1945-1946
                } else {
                    battle.display(user.pbThis() + "用" + user.abilityName() + "偷窃了"
                            + b.pbThis(true) + "的" + user.itemName() + "！");   // :1948-1949
                }
                battle.hideAbilitySplash(user);                                // :1951
                user.pbHeldItemTriggerCheck(0, false);                         // :1952 (Ruby 无参 = forcedItem 0, fling false)
                break;                                                         // :1953
            }
        });

        BattleHandlers.UserAbilityEndOfMove.add("MOXIE", (ability, user, targets, move, battle) -> {
            // BattleHandlers_Abilities.rb:1958-1969
            if (battle.pbAllFainted(user.idxOpposingSide())) {                  // :1960
                return;
            }
            int numFainted = 0;                                                // :1961
            for (Battler b : targets) {                                        // :1962
                if (b.damageState.fainted) {
                    numFainted++;
                }
            }
            if (numFainted == 0 || !user.pbCanRaiseStatStage(PBStats.ATTACK, user)) {   // :1963
                return;
            }
            user.pbRaiseStatStageByAbility(PBStats.ATTACK, numFainted, user, true);   // :1964
        });

        BattleHandlers.UserAbilityEndOfMove.copy("MOXIE", "CHILLINGNEIGH", "FEARLESS", "DRAGONSOULCRY");   // :1968

        BattleHandlers.UserAbilityEndOfMove.add("GRIMNEIGH", (ability, user, targets, move, battle) -> {
            // BattleHandlers_Abilities.rb:1970-1980
            if (battle.pbAllFainted(user.idxOpposingSide())) {                  // :1972
                return;
            }
            int numFainted = 0;                                                // :1973
            for (Battler b : targets) {                                        // :1974
                if (b.damageState.fainted) {
                    numFainted++;
                }
            }
            if (numFainted == 0 || !user.pbCanRaiseStatStage(PBStats.SPATK, user)) {   // :1975
                return;
            }
            user.pbRaiseStatStageByAbility(PBStats.SPATK, numFainted, user, true);   // :1976
        });

        BattleHandlers.UserAbilityEndOfMove.add("ASONEICE", (ability, user, targets, move, battle) -> {
            // BattleHandlers_Abilities.rb:1981-1996
            if (battle.pbAllFainted(user.idxOpposingSide())) {                  // :1983
                return;
            }
            int numFainted = 0;                                                // :1984
            for (Battler b : targets) {                                        // :1985
                if (b.damageState.fainted) {
                    numFainted++;
                }
            }
            if (numFainted == 0 || !user.pbCanRaiseStatStage(PBStats.ATTACK, user)
                    || user.fainted()) {                                       // :1986
                return;
            }
            String chillingNeigh = abilityDisplayName(battle, "CHILLINGNEIGH");   // :1987 getID+getName
            battle.showAbilitySplash(user);                                    // :1987 Ruby: (user,false,true,name)
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                // :1988
                user.pbRaiseStatStage(PBStats.ATTACK, numFainted, user);       // :1989
            } else {
                user.pbRaiseStatStageByCause(PBStats.ATTACK, numFainted, user, chillingNeigh);   // :1991
            }
            battle.hideAbilitySplash(user);                                    // :1993
        });

        BattleHandlers.UserAbilityEndOfMove.add("ASONEGHOST", (ability, user, targets, move, battle) -> {
            // BattleHandlers_Abilities.rb:1997-2016
            if (battle.pbAllFainted(user.idxOpposingSide())) {                  // :1999
                return;
            }
            int numFainted = 0;                                                // :2000
            for (Battler b : targets) {                                        // :2001
                if (b.damageState.fainted) {
                    numFainted++;
                }
            }
            if (numFainted == 0 || !user.pbCanRaiseStatStage(PBStats.ATTACK, user)
                    || user.fainted()) {                                       // :2002
                return;
            }
            String grimNeigh = abilityDisplayName(battle, "GRIMNEIGH");        // :2003 getID+getName
            battle.showAbilitySplash(user);                                    // :2003 Ruby: (user,false,true,name)
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                // :2004
                user.pbRaiseStatStage(PBStats.SPATK, numFainted, user);        // :2005
            } else {
                user.pbRaiseStatStageByCause(PBStats.SPATK, numFainted, user, grimNeigh);   // :2007
            }
            battle.hideAbilitySplash(user);                                    // :2009
        });

        BattleHandlers.UserAbilityEndOfMove.add("MERMAIDSOUND", (ability, user, targets, move, battle) -> {
            // BattleHandlers_Abilities.rb:3933-3943
            if (!fx(move).soundMove(move)) {                                   // :3935
                return;
            }
            if (!fx(move).pbDamagingMove(move)) {                              // :3936 排除叫声
                return;
            }
            if (!user.canHeal()) {                                             // :3937
                return;
            }
            int healHp = Math.max(1, Math.round((user.maxHp() - user.hp) / 4.0f));   // :3938 (totalhp-hp)/4.0 round
            user.pbRecoverHP(healHp);                                          // :3939
        });

        BattleHandlers.UserAbilityEndOfMove.add("BAMBOOFEATHER", (ability, user, targets, move, battle) -> {
            // BattleHandlers_Abilities.rb:4047-4080
            if (!fx(move).damagingMove(move)) {                                // :4049
                return;
            }
            int stageAtk = user.stage(PBStats.ATTACK);                          // :4050 stages[PBStats::ATTACK]
            int stageSpeed = user.stage(PBStats.SPEED);                         // :4051
            for (Battler t : targets) {                                        // :4052
                if (t.fainted()) {                                             // :4053
                    continue;
                }
                if (!t.opposes(user)) {                                        // :4054
                    continue;
                }
                if (t.lastHPLostFromFoe == 0) {                                // :4055
                    continue;
                }
                if (!t.takesIndirectDamage(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :4056
                    continue;
                }
                battle.showAbilitySplash(user);                                // :4057
                int n = 1;                                                     // :4059
                if (t.maxHp() > user.maxHp()) {                                // :4060 totalhp
                    n += 1;
                }
                if (t.attack() > user.attack()) {                              // :4061
                    n += 1;
                }
                if (t.stage(PBStats.ATTACK) > stageAtk) {                       // :4062 stages[PBStats::ATTACK]
                    n += 1;
                }
                if (t.spAtk() > user.attack()) {                               // :4063 spatk > user.attack
                    n += 1;
                }
                if (t.stage(PBStats.SPATK) > stageAtk) {                        // :4064 stages[PBStats::SPATK]
                    n += 1;
                }
                if (t.speed() > user.speed()) {                                // :4065
                    n += 1;
                }
                if (t.stage(PBStats.SPEED) > stageSpeed) {                      // :4066
                    n += 1;
                }
                // 登记: BattleHandlers_Abilities:4068 battle.scene.pbDamageAnimation(t)（场景未建模）
                int extraDamage = (int) Math.floor(t.lastHPLostFromFoe * n / 8.0);   // :4069
                t.pbReduceHP(extraDamage, true, true, true);                    // :4070 (anim=true)
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {             // :4071
                    battle.display(t.pbThis() + "受伤了！");                     // :4072
                } else {
                    battle.display(t.pbThis() + "受到" + user.pbThis() + "的"
                            + user.abilityName() + "伤害了！");                  // :4074
                }
                if (t.fainted()) {                                              // :4076
                    t.pbFaint();
                }
                battle.hideAbilitySplash(user);                                 // :4077
            }
        });

        BattleHandlers.UserAbilityEndOfMove.add("NETHERDRIVE", (ability, user, targets, move, battle) -> {
            // BattleHandlers_Abilities.rb:4215-4227
            if (battle.pbAllFainted(user.idxOpposingSide())) {                  // :4217
                return;
            }
            int numFainted = 0;                                                // :4218
            for (Battler b : targets) {                                        // :4219
                if (b.damageState.fainted) {
                    numFainted++;
                }
            }
            if (numFainted == 0 || !user.pbCanRaiseStatStage(PBStats.SPATK, user)) {   // :4220
                return;
            }
            user.pbRaiseStatStageByAbility(PBStats.SPATK, numFainted, user, true);   // :4221
        });

        BattleHandlers.UserAbilityEndOfMove.add("GHASTLYWAIL", (ability, user, targets, move, battle) -> {
            // BattleHandlers_Abilities.rb:4246-4253
            if (battle.pbAllFainted(user.idxOpposingSide())) {                  // :4248
                return;
            }
            int numFainted = 0;                                                // :4249
            for (Battler b : targets) {                                        // :4250
                if (b.damageState.fainted) {
                    numFainted++;
                }
            }
            if (numFainted == 0 || !user.pbCanRaiseStatStage(PBStats.SPATK, user)) {   // :4251
                return;
            }
            user.pbRaiseStatStageByAbility(PBStats.SPATK, numFainted, user, true);   // :4252
        });
    }

    // ==================================================================
    // TargetAbilityAfterMoveUse (5 entries)
    // ==================================================================

    private static void registerTargetAbilityAfterMoveUse() {
        BattleHandlers.TargetAbilityAfterMoveUse.add("BERSERK",
                (ability, target, user, move, switched, battle) -> {
            // BattleHandlers_Abilities.rb:2017-2025
            if (!fx(move).damagingMove(move)) {                                // :2019
                return;
            }
            if (target.damageState.initialHP < target.maxHp() / 2             // :2020 totalhp
                    || target.hp >= target.maxHp() / 2) {
                return;
            }
            if (!target.pbCanRaiseStatStage(PBStats.SPATK, target)) {           // :2021
                return;
            }
            target.pbRaiseStatStageByAbility(PBStats.SPATK, 1, target, true);    // :2022
        });

        BattleHandlers.TargetAbilityAfterMoveUse.add("COLORCHANGE",
                (ability, target, user, move, switched, battle) -> {
            // BattleHandlers_Abilities.rb:2026-2040
            if (target.damageState.calcDamage == 0 || target.damageState.substitute) {   // :2028
                return;
            }
            // :2029 move.calcType<0 - Ruby's numeric calc type, -1 when unset; null here.
            if (move.calcType() == null || PBTypes.isPseudoType(battle.pbs(), move.calcType())) {   // :2029
                return;
            }
            if ("STELLAR".equals(move.calcType())) {                // :2030
                return;
            }
            if (target.pbHasType(move.calcType()) && !target.pbHasOtherType(move.calcType())) {   // :2031
                return;
            }
            String typeName = PBTypes.getName(battle.pbs(), move.calcType());    // :2032
            battle.showAbilitySplash(target);                                   // :2033
            PendingApi.pbChangeTypes(target, move.calcType());                  // :2034
            battle.display(target.pbThis() + "的" + target.abilityName()        // :2035-2036
                    + "使它\n变成了" + typeName + "属性！");
            battle.hideAbilitySplash(target);                                   // :2037
        });

        BattleHandlers.TargetAbilityAfterMoveUse.add("PICKPOCKET",
                (ability, target, user, move, switched, battle) -> {
            // BattleHandlers_Abilities.rb:2041-2079
            // NOTE (:2043-2045): the plugin deliberately does not steal when the user
            // was switched out by a Red Card.
            if (battle.wildBattle() && target.foe) {                            // :2046 target.opposes?
                return;
            }
            if (!fx(move).contactMove(move)) {                                  // :2047
                return;
            }
            if (switched.contains(user.index, false)) {                         // :2048 switched.include?(user.index)
                return;
            }
            if (user.effects.intVal(PBEffects.Battler.Substitute) > 0
                    || target.damageState.substitute) {                         // :2049
                return;
            }
            if (hasItem(target) || !hasItem(user)) {                            // :2050 target.item>0 || user.item==0
                return;
            }
            if (user.unlosableItem(user.item) || target.unlosableItem(user.item)) {   // :2051
                return;
            }
            battle.showAbilitySplash(target);                                   // :2052
            if (user.hasActiveAbility("STICKYHOLD")) {                          // :2053
                if (target.opposes(user)) {                                     // :2054
                    battle.showAbilitySplash(user);
                }
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {             // :2055
                    battle.display(user.pbThis() + "的道具不能偷窃！");           // :2056
                }
                if (target.opposes(user)) {                                     // :2058
                    battle.hideAbilitySplash(user);
                }
                battle.hideAbilitySplash(target);                               // :2059
                return;                                                         // :2060
            }
            target.item = user.item;                                            // :2062
            user.item = "";                                                     // :2063 user.item = 0
            user.effects.set(PBEffects.Battler.Unburden, true);                 // :2064
            if (battle.wildBattle() && !hasItem(target.initialItem)             // :2065 target.initialItem==0
                    && user.initialItem.equals(target.item)) {                  // :2065 user.initialItem==target.item
                target.setInitialItem(target.item);                             // :2066
                user.setInitialItem("");                                        // :2067 user.setInitialItem(0)
            }
            battle.display(target.pbThis() + "顺手偷走了" + user.pbThis(true)    // :2069-2070
                    + "的" + target.itemName() + "！");
            battle.hideAbilitySplash(target);                                   // :2071
            target.pbHeldItemTriggerCheck(0, false);                            // :2072 (Ruby 无参 = forcedItem 0, fling false)
        });

        BattleHandlers.TargetAbilityAfterMoveUse.add("ANGERSHELL",
                (ability, target, user, move, switched, battle) -> {
            // BattleHandlers_Abilities.rb:3256-3282
            if (!fx(move).damagingMove(move)) {                                 // :3258
                return;
            }
            if (!target.droppedBelowHalfHP) {                                   // :3259
                return;
            }
            boolean showAnim = true;                                            // :3260
            battle.showAbilitySplash(target);                                   // :3261
            int[] upStats = { PBStats.ATTACK, PBStats.SPATK, PBStats.SPEED };   // :3262
            for (int stat : upStats) {
                if (!target.pbCanRaiseStatStage(stat, user, null, true)) {      // :3263 showFailMsg=true
                    continue;
                }
                if (target.pbRaiseStatStage(stat, 1, user, showAnim, false)) {  // :3264 ignoreContrary=false
                    showAnim = false;                                           // :3265
                }
            }
            showAnim = true;                                                    // :3268
            int[] downStats = { PBStats.DEFENSE, PBStats.SPDEF };               // :3269
            for (int stat : downStats) {
                if (!target.pbCanLowerStatStage(stat, user, null, true, false)) {   // :3270 showFailMsg=true
                    continue;
                }
                if (target.pbLowerStatStage(stat, 1, user, showAnim, false, false)) {   // :3271
                    showAnim = false;                                           // :3272
                }
            }
            battle.hideAbilitySplash(target);                                   // :3275
        });

        BattleHandlers.TargetAbilityAfterMoveUse.add("LASTSTAND",
                (ability, target, user, move, switched, battle) -> {
            // BattleHandlers_Abilities.rb:4205-4214
            if (!fx(move).damagingMove(move)) {                                 // :4207
                return;
            }
            if (target.damageState.initialHP < target.maxHp() / 2             // :4208 totalhp
                    || target.hp >= target.maxHp() / 2) {
                return;
            }
            if (!target.pbCanRaiseStatStage(PBStats.ATTACK, target)) {           // :4209
                return;
            }
            target.pbRaiseStatStageByAbility(PBStats.ATTACK, 1, target, true);   // :4210
        });
    }
}
