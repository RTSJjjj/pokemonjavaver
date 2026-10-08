package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 4 / L1': the {@code BattleHandlers_Abilities.rb} registrations assigned to
 * this file by {@code stage4-l1-handler-roster.md} §1.2 — <b>91 entries</b>
 * (7 groups, span subtotal 1174) plus the 6 {@code copy} lines whose group lives
 * here (11 destination symbols, so 102 registered symbols in total).
 *
 * <h2>Order</h2>
 * Every {@code add}/{@code copy} below is emitted in <b>Ruby source order</b>
 * (ascending line number), because {@code HandlerHash#add} is a plain
 * {@code @hash[id] = handler} overwrite ({@code Event_Handlers.rb:110-113}) and
 * {@code copy} reads the source handler that must already be registered. Two
 * places depend on this:
 * <ul>
 * <li>{@code BROKENBREATH} is registered <b>twice</b> (:3571 and :3606) — the
 *     second wins, exactly as in Ruby.</li>
 * <li>every {@code copy} sits after its source ({@code :121}, {@code :2413},
 *     {@code :2741}, {@code :2891}, {@code :3475}, {@code :3527}).</li>
 * </ul>
 *
 * <h2>Deviations, all documented at the call site</h2>
 * <ul>
 * <li>{@code battle.pbDisplay} → {@link Battle#display} (the M0 rename).</li>
 * <li>{@code battle.pbShowAbilitySplash(battler,delay)} → the 1-argument
 *     {@link Battle#showAbilitySplash}: the plugin's {@code delay=true} branch
 *     only idles 40 frames ({@code PokeBattle_Battle:806}) and has no parameter
 *     here — registered at each site.</li>
 * <li>{@code PBItems.getName(b.item)} → {@code b.itemName()} — literally the same
 *     expression ({@code PokeBattle_Battler:212}).</li>
 * <li>{@code PBMoves.getName(id)} / {@code pbGetMoveData(id)} → a lookup by the
 *     move's <b>internal name</b> through {@code battle.pbs()}, because this
 *     runtime keys moves by name, not by number.</li>
 * <li>{@code battler.eachMove} → {@code battler.moveSlots()} filtered by
 *     {@code m != null && m.id() != 0} — the same predicate
 *     ({@code PokeBattle_Battler:543-545}).</li>
 * <li>Anything with no counterpart yet is a no-op carrying
 *     {@code // 登记: <段:行号> ...} — never an invented behaviour.</li>
 * </ul>
 */
final class AbilitiesSwitchIn {

    private AbilitiesSwitchIn() {
    }

    /**
     * {@code battler.inTwoTurnAttack?(fn)} (PokeBattle_Battler:718-723).
     *
     * <p>登记: the real body needs
     * {@code pbGetMoveData(@effects[TwoTurnAttack],MOVE_FUNCTION_CODE)}
     * ({@code PBMove:32}), which has no Java port yet. Nothing in this runtime
     * ever writes {@code @effects[TwoTurnAttack]} — {@code initEffects} sets it to
     * {@code 0} ({@code Battler_Initialize:309}) — so the answer is provably
     * {@code false} here, matching the plugin when no two-turn move is charging.
     * Not an invented value.</p>
     */
    private static boolean inTwoTurnAttack(Battler battler, String function) {
        return false;
    }


    /**
     * {@code pbGetMoveData(move.id)} (PBMove:32): the move's row from the compiled
     * move table. This runtime identifies moves by internal name, so the lookup
     * goes through {@code battle.pbs().move(internalName)}.
     */
    private static PbsData.Move moveData(Battle battle, BattleMove move) {
        if (battle == null || battle.pbs() == null || move == null) {
            return null;
        }
        return battle.pbs().move(move.internalName());
    }

    /** {@code PBMoves.getName(id)} — the display name of a move held by internal name. */
    private static String moveName(Battle battle, String internalName) {
        PbsData.Move data = battle == null || battle.pbs() == null ? null
                : battle.pbs().move(internalName);
        return data == null || data.name == null ? internalName : data.name;
    }

    /** {@code PBDebug.log} — 登记: PBDebug is not modelled; no-op keeps the call site visible. */
    private static void pbDebugLog(String message) {
        // 登记: PBDebug.log（PBDebug.rb:1-41，本批未建模）→ 空实现
    }

    /** {@code PBAbilities.getName(id)} — 登记: no ability-name table is reachable here yet. */
    private static String abilityDisplayName(String internalName) {
        // 登记: PBAbilities.getName / getID(PBAbilities,...)（Compiler_PBS:446 生成）本批未建模
        return internalName;
    }

    /** {@code BattleHandlers_Abilities.rb}: 91 entries (roster §1.2, §2 main table). */
    static void register() {

        // =====================================================================
        // AbilityOnHPDroppedBelowHalf — BattleHandlers_Abilities.rb:83-126
        // =====================================================================
        BattleHandlers.AbilityOnHPDroppedBelowHalf.add("EMERGENCYEXIT", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:83-118
            if (battler.effects.intVal(PBEffects.Battler.SkyDrop) >= 0
                    || inTwoTurnAttack(battler, "0CE")) {                    // :85 Sky Drop
                return false;
            }
            if (battle.wildBattle()) {                                       // :87
                if (battler.opposes(0) && battle.pbSideBattlerCount(battler.index) > 1) { // :88
                    return false;
                }
                if (!battle.pbCanRun(battler.index)) {                       // :89
                    return false;
                }
                battle.showAbilitySplash(battler);                           // :90 (delay=true，见类 javadoc)
                battle.hideAbilitySplash(battler);                           // :91
                // 登记: BattleHandlers_Abilities.rb:92 pbSEPlay("Battle flee") 依赖音频子系统
                battle.display(battler.pbThis() + "逃离战斗了！");            // :93
                battle.decision = 3;                                        // :94 Escaped
                return true;                                                 // :95
            }
            if (battle.pbAllFainted(battler.idxOpposingSide())) {            // :98
                return false;
            }
            if (!battle.pbCanSwitch(battler.index)) {                        // :99
                return false;
            }
            if (!battle.pbCanChooseNonActive(battler.index)) {               // :100
                return false;
            }
            battle.showAbilitySplash(battler);                               // :101 (delay=true)
            battle.hideAbilitySplash(battler);                               // :102
            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {             // :103 死代码（常量恒 true）
                battle.display(battler.pbThis() + "的" + battler.abilityName() + "被触发了！"); // :104
            }
            battle.display(battler.pbThis() + "回到了"
                    + battle.pbGetOwnerName(battler.index) + "身边！");       // :106-107
            if (battle.endOfRound) {                                            // :108
                // 登记: :109 battle.scene.pbRecall(battler.index) 依赖 PokeBattle_Scene
                battler.pbAbilitiesOnSwitchOut();                            // :110
                return true;                                                 // :111
            }
            int newPkmn = battle.pbGetReplacementPokemonIndex(battler.index); // :113 Owner chooses
            if (newPkmn < 0) {                                               // :114
                return false;
            }
            battle.pbRecallAndReplace(battler.index, newPkmn);                // :115
            battle.pbClearChoice(battler.index);                             // :116
            return true;                                                     // :117
        });
        BattleHandlers.AbilityOnHPDroppedBelowHalf.copy("EMERGENCYEXIT", "WIMPOUT"); // :121

        // =====================================================================
        // TrappingTargetAbility — BattleHandlers_Abilities.rb:2380-2401, :4192, :4228
        // =====================================================================
        BattleHandlers.TrappingTargetAbility.add("ARENATRAP", (ability, switcher, bearer, battle) -> {
            // BattleHandlers_Abilities.rb:2380-2383
            return !switcher.airborne();                                     // :2382
        });
        BattleHandlers.TrappingTargetAbility.add("MAGNETPULL", (ability, switcher, bearer, battle) -> {
            // BattleHandlers_Abilities.rb:2386-2389
            return switcher.pbHasType("STEEL");                              // :2388
        });
        BattleHandlers.TrappingTargetAbility.add("SHADOWTAG", (ability, switcher, bearer, battle) -> {
            // BattleHandlers_Abilities.rb:2392-2395
            return !switcher.hasActiveAbility("SHADOWTAG");                  // :2394
        });
        BattleHandlers.TrappingTargetAbility.add("DSOVERLORD", (ability, switcher, bearer, battle) -> {
            // BattleHandlers_Abilities.rb:4192-4195 毁坏领主
            return !switcher.hasActiveAbility("DSOVERLORD");                 // :4194
        });
        BattleHandlers.TrappingTargetAbility.add("CONFESSIONLIST", (ability, battler, trappingBattler, battle) -> {
            // BattleHandlers_Abilities.rb:4228-4232 忏悔表
            if (battler.pbHasType("FLYING")) {                               // :4230
                return false;
            }
            return true;                                                     // :4231
        });

        // =====================================================================
        // AbilityOnSwitchIn — BattleHandlers_Abilities.rb:2402-... (76 entries)
        // =====================================================================
        BattleHandlers.AbilityOnSwitchIn.add("AIRLOCK", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2402-2409
            battle.showAbilitySplash(battler);                               // :2404
            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {             // :2405 死代码
                battle.display(battler.pbThis() + "已经拥有了" + battler.abilityName() + "!"); // :2406
            }
            battle.display("天气的影响消失了。");                             // :2408
            battle.hideAbilitySplash(battler);                               // :2409
        });
        BattleHandlers.AbilityOnSwitchIn.copy("AIRLOCK", "CLOUDNINE");        // :2413

        BattleHandlers.AbilityOnSwitchIn.add("ANTICIPATION", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2415-2448
            if (!battler.pbOwnedByPlayer()) {                                // :2417
                return;
            }
            Array<String> battlerTypes = battler.pbTypes(true);              // :2418
            String type1 = battlerTypes.size > 0 ? battlerTypes.get(0) : null;  // :2419
            String type2 = battlerTypes.size > 1 ? battlerTypes.get(1) : type1; // :2420
            String type3 = battlerTypes.size > 2 ? battlerTypes.get(2) : type2; // :2421
            boolean found = false;                                           // :2422
            for (Battler b : battle.eachOtherSideBattler(battler.index)) {   // :2423
                for (BattleMove m : b.moveSlots()) {                         // :2424 eachMove (:543-545)
                    if (m == null || m.id() == 0) {
                        continue;
                    }
                    if (m.statusMove()) {                                    // :2425
                        continue;
                    }
                    PbsData.Move moveData = moveData(battle, m);             // :2426
                    if (moveData == null) {
                        continue;
                    }
                    if (type1 != null) {                                     // :2427
                        String moveType = moveData.type;                     // :2428
                        if (Battle.NEWEST_BATTLE_MECHANICS && "HIDDENPOWER".equals(m.internalName())) { // :2429
                            // 登记: BattleHandlers_Abilities.rb:2430 pbHiddenPower(b.pokemon)[0]
                            //       依赖觉醒力量的个体值类型表（本批未建模）→ 保留 moveData.type
                        }
                        int eff = PBTypes.getCombinedEffectiveness(battle.pbs(), moveType,
                                type1, type2, type3);                        // :2432
                        if (PBTypes.ineffective(eff)) {                      // :2433
                            continue;
                        }
                        if (!PBTypes.superEffective(eff)
                                && !"070".equals(moveData.function)) {       // :2434 OHKO
                            continue;
                        }
                    } else if (!"070".equals(moveData.function)) {           // :2436 OHKO
                        continue;
                    }
                    found = true;                                            // :2438
                    break;                                                   // :2439
                }
                if (found) {                                                 // :2441
                    break;
                }
            }
            if (found) {                                                     // :2443
                battle.showAbilitySplash(battler);                           // :2444
                battle.display(battler.pbThis() + "因为预知到了危险而发抖！");  // :2445
                battle.hideAbilitySplash(battler);                           // :2446
            }
        });

        BattleHandlers.AbilityOnSwitchIn.add("AURABREAK", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2451-2456
            battle.showAbilitySplash(battler);                               // :2453
            battle.display(battler.pbThis() + "逆转了其它宝可梦的气场!");      // :2454
            battle.hideAbilitySplash(battler);                               // :2455
        });

        BattleHandlers.AbilityOnSwitchIn.add("COMATOSE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2459-2464
            battle.showAbilitySplash(battler);                               // :2461
            battle.display(battler.pbThis() + "正在打瞌睡！");                // :2462
            battle.hideAbilitySplash(battler);                               // :2463
        });

        BattleHandlers.AbilityOnSwitchIn.add("DARKAURA", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2467-2472
            battle.showAbilitySplash(battler);                               // :2469
            battle.display(battler.pbThis() + "正在释放暗黑气场！");           // :2470
            battle.hideAbilitySplash(battler);                               // :2471
        });

        BattleHandlers.AbilityOnSwitchIn.add("DELTASTREAM", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2475-2478
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.StrongWinds, battler, battle, true); // :2477
        });

        BattleHandlers.AbilityOnSwitchIn.add("DESOLATELAND", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2481-2484
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.HarshSun, battler, battle, true); // :2483
        });

        BattleHandlers.AbilityOnSwitchIn.add("DOWNLOAD", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2487-2496
            int oDef = 0;                                                    // :2489
            int oSpDef = 0;
            for (Battler b : battle.eachOtherSideBattler(battler.index)) {   // :2490
                oDef += b.defense();                                         // :2491
                oSpDef += b.spDef();                                         // :2492
            }
            int stat = oDef < oSpDef ? PBStats.ATTACK : PBStats.SPATK;       // :2494
            battler.pbRaiseStatStageByAbility(stat, 1, battler, true);       // :2495
        });

        BattleHandlers.AbilityOnSwitchIn.add("DRIZZLE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2499-2502
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Rain, battler, battle); // :2501
        });

        BattleHandlers.AbilityOnSwitchIn.add("DROUGHT", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2505-2508
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Sun, battler, battle); // :2507
        });

        BattleHandlers.AbilityOnSwitchIn.add("ELECTRICSURGE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2511-2517
            if (battle.field.terrain == PBBattleTerrains.Electric) {         // :2513
                return;
            }
            battle.showAbilitySplash(battler);                               // :2514
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Electric); // :2515
        });

        BattleHandlers.AbilityOnSwitchIn.add("FAIRYAURA", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2520-2525
            battle.showAbilitySplash(battler);                               // :2522
            battle.display(battler.pbThis() + "正在释放妖精气场！");           // :2523
            battle.hideAbilitySplash(battler);                               // :2524
        });

        BattleHandlers.AbilityOnSwitchIn.add("FOREWARN", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2528-2565
            if (!battler.pbOwnedByPlayer()) {                                // :2530
                return;
            }
            int highestPower = 0;                                            // :2531
            Array<String> forewarnMoves = new Array<>();                     // :2532 存内部名（本运行时身份）
            for (Battler b : battle.eachOtherSideBattler(battler.index)) {   // :2533
                for (BattleMove m : b.moveSlots()) {                         // :2534 eachMove
                    if (m == null || m.id() == 0) {
                        continue;
                    }
                    PbsData.Move moveData = moveData(battle, m);             // :2535
                    if (moveData == null) {
                        continue;
                    }
                    int power = moveData.power;                              // :2536 MOVE_BASE_DAMAGE
                    String fn = moveData.function;                           // MOVE_FUNCTION_CODE
                    if ("070".equals(fn)) {                                  // :2537 OHKO
                        power = 160;
                    }
                    if ("08B".equals(fn)) {                                  // :2538 Eruption
                        power = 150;
                    }
                    // :2540 Counter, Mirror Coat, Metal Burst
                    if ("071".equals(fn) || "072".equals(fn) || "073".equals(fn)) {
                        power = 120;
                    }
                    // :2542-2546 Sonic Boom … Grass Knot
                    if ("06A".equals(fn) || "06B".equals(fn) || "06D".equals(fn) || "06E".equals(fn)
                            || "06F".equals(fn) || "089".equals(fn) || "08A".equals(fn)
                            || "08C".equals(fn) || "08D".equals(fn) || "090".equals(fn)
                            || "096".equals(fn) || "097".equals(fn) || "098".equals(fn)
                            || "09A".equals(fn)) {
                        power = 80;
                    }
                    if (power < highestPower) {                              // :2547
                        continue;
                    }
                    if (power > highestPower) {                              // :2548
                        forewarnMoves.clear();
                    }
                    forewarnMoves.add(m.internalName());                     // :2549 push(m.id)
                    highestPower = power;                                    // :2550
                }
            }
            if (forewarnMoves.size > 0) {                                    // :2553
                battle.showAbilitySplash(battler);                           // :2554
                String forewarnMoveID = forewarnMoves.get(battle.pbRandom(forewarnMoves.size)); // :2555
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {          // :2556
                    battle.display(battler.pbThis() + "预知到了"
                            + moveName(battle, forewarnMoveID) + "！");      // :2557-2558 PBMoves.getName
                } else {
                    battle.display(battler.pbThis() + "的预知梦预知到了"
                            + moveName(battle, forewarnMoveID) + "！");      // :2560-2561
                }
                battle.hideAbilitySplash(battler);                           // :2563
            }
        });

        BattleHandlers.AbilityOnSwitchIn.add("FRISK", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2568-2588
            if (!battler.pbOwnedByPlayer()) {                                // :2570
                return;
            }
            Array<Battler> foes = new Array<>();                             // :2571
            for (Battler b : battle.eachOtherSideBattler(battler.index)) {   // :2572
                if (b.item != null && !b.item.isEmpty()) {                   // :2573 b.item>0
                    foes.add(b);
                }
            }
            if (foes.size > 0) {                                             // :2575
                battle.showAbilitySplash(battler);                           // :2576
                if (Battle.NEWEST_BATTLE_MECHANICS) {                        // :2577
                    for (Battler b : foes) {                                 // :2578
                        battle.display(battler.pbThis() + "察觉到了" + b.pbThis(true) + "的"
                                + b.itemName() + "！");                      // :2579-2580 PBItems.getName(b.item)
                    }
                } else {
                    Battler foe = foes.get(battle.pbRandom(foes.size));      // :2583
                    battle.display(battler.pbThis() + "察觉到了敌人的"
                            + foe.itemName() + "！");                        // :2584-2585
                }
                battle.hideAbilitySplash(battler);                           // :2587
            }
        });

        BattleHandlers.AbilityOnSwitchIn.add("GRASSYSURGE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2592-2597
            if (battle.field.terrain == PBBattleTerrains.Grassy) {           // :2594
                return;
            }
            battle.showAbilitySplash(battler);                               // :2595
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Grassy); // :2596
        });

        BattleHandlers.AbilityOnSwitchIn.add("IMPOSTER", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2601-2616
            if (battler.effects.truthy(PBEffects.Battler.Transform)) {       // :2603
                return;
            }
            Battler choice = battler.pbDirectOpposing(false);                // :2604
            if (choice == null || choice.fainted()) {                        // :2605
                return;
            }
            if (choice.effects.truthy(PBEffects.Battler.Transform)           // :2606-2610
                    || choice.effects.truthy(PBEffects.Battler.Illusion)
                    || choice.effects.intVal(PBEffects.Battler.Substitute) > 0
                    || choice.effects.intVal(PBEffects.Battler.SkyDrop) >= 0
                    || choice.semiInvulnerable()) {
                return;
            }
            battle.showAbilitySplash(battler);                               // :2611 (delay=true)
            battle.hideAbilitySplash(battler);                               // :2612
            // 登记: :2614 battle.scene.pbChangePokemonTransform(battler,choice.pokemon) 依赖 PokeBattle_Scene
            // 登记: :2615 battler.pbTransform(choice) —— Battler 尚无 pbTransform（PokeBattle_Battler:418-446）
        });

        BattleHandlers.AbilityOnSwitchIn.add("INTIMIDATE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2619-2634
            battle.showAbilitySplash(battler);                               // :2621
            for (Battler b : battle.eachOtherSideBattler(battler.index)) {   // :2622
                if (!b.near(battler)) {                                      // :2623
                    continue;
                }
                boolean checkItem = true;                                    // :2624
                if (b.hasActiveAbility(new String[] {"CONTRARY", "GUARDDOG", "FEARLESS"})) { // :2625
                    if (b.statStageAtMax(PBStats.ATTACK)) {                  // :2626
                        checkItem = false;
                    }
                } else if (b.statStageAtMin(PBStats.ATTACK)) {               // :2627
                    checkItem = false;
                }
                // 登记: :2630 b.pbLowerAttackStatStageIntimidate(battler)
                //       —— Battler 尚无该方法（Battler_StatStages:309-373）
                if (checkItem) {                                             // :2631
                    b.pbItemOnIntimidatedCheck();
                }
            }
            battle.hideAbilitySplash(battler);                               // :2633
        });

        BattleHandlers.AbilityOnSwitchIn.add("MISTYSURGE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2637-2642
            if (battle.field.terrain == PBBattleTerrains.Misty) {            // :2639
                return;
            }
            battle.showAbilitySplash(battler);                               // :2640
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Misty); // :2641
        });

        BattleHandlers.AbilityOnSwitchIn.add("MOLDBREAKER", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2646-2651
            battle.showAbilitySplash(battler);                               // :2648
            battle.display(battler.pbThis() + "打破了常规！");                // :2649
            battle.hideAbilitySplash(battler);                               // :2650
        });

        BattleHandlers.AbilityOnSwitchIn.add("PRESSURE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2655-2660
            battle.showAbilitySplash(battler);                               // :2657
            battle.display(battler.pbThis() + "正在施加压力！");              // :2658
            battle.hideAbilitySplash(battler);                               // :2659
        });

        BattleHandlers.AbilityOnSwitchIn.add("PRIMORDIALSEA", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2664-2667
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.HeavyRain, battler, battle, true); // :2666
        });

        BattleHandlers.AbilityOnSwitchIn.add("PSYCHICSURGE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2670-2675
            if (battle.field.terrain == PBBattleTerrains.Psychic) {          // :2672
                return;
            }
            battle.showAbilitySplash(battler);                               // :2673
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Psychic); // :2674
        });

        BattleHandlers.AbilityOnSwitchIn.add("SANDSTREAM", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2679-2682
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Sandstorm, battler, battle); // :2681
        });

        BattleHandlers.AbilityOnSwitchIn.add("SLOWSTART", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2685-2696
            battle.showAbilitySplash(battler);                               // :2687
            battler.effects.set(PBEffects.Battler.SlowStart, 5);             // :2688
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {              // :2689
                battle.display(battler.pbThis() + "无法顺利行动！");          // :2690
            } else {
                battle.display(battler.abilityName() + "使得" + battler.pbThis()
                        + "无法顺利行动！");                                  // :2692-2693
            }
            battle.hideAbilitySplash(battler);                               // :2695
        });

        BattleHandlers.AbilityOnSwitchIn.add("SNOWWARNING", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2699-2702
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Snow, battler, battle); // :2701
        });

        BattleHandlers.AbilityOnSwitchIn.add("TERAVOLT", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2705-2710
            battle.showAbilitySplash(battler);                               // :2707
            battle.display(battler.pbThis() + "正在释放溅射气场！");           // :2708
            battle.hideAbilitySplash(battler);                               // :2709
        });

        BattleHandlers.AbilityOnSwitchIn.add("TURBOBLAZE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2714-2719
            battle.showAbilitySplash(battler);                               // :2716
            battle.display(battler.pbThis() + "正在释放炽热气场！");           // :2717
            battle.hideAbilitySplash(battler);                               // :2718
        });

        BattleHandlers.AbilityOnSwitchIn.add("UNNERVE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2722-2727
            battle.showAbilitySplash(battler);                               // :2724
            battle.display(battler.pbOpposingTeam(false) + "太紧张导致无法吃下树果！"); // :2725
            battle.hideAbilitySplash(battler);                               // :2726
        });

        BattleHandlers.AbilityOnSwitchIn.add("ASONEICE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2731-2738
            battle.showAbilitySplash(battler);                               // :2733
            battle.display(battler.pbThis() + "拥有两种特性！");              // :2734
            // :2735 pbShowAbilitySplash(battler,false,true,PBAbilities.getName(getID(PBAbilities,:UNNERVE)))
            // 登记: 该 4 参重载（logTrigger/ability 参数）不存在；能力显示名表也未建模
            //       （abilityDisplayName 为登记用的占位），故这里只保留调用形状
            abilityDisplayName("UNNERVE");
            battle.display(battler.pbThis() + "正在施加紧张感！");            // :2736
            battle.hideAbilitySplash(battler);                               // :2737
        });
        BattleHandlers.AbilityOnSwitchIn.copy("ASONEICE", "ASONEGHOST");     // :2741

        BattleHandlers.AbilityOnSwitchIn.add("INTREPIDSWORD", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2743-2747
            int stat = PBStats.ATTACK;                                       // :2745
            battler.pbRaiseStatStageByAbility(stat, 1, battler, true);       // :2746
        });

        BattleHandlers.AbilityOnSwitchIn.add("DAUNTLESSSHIELD", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2750-2754
            int stat = PBStats.DEFENSE;                                      // :2752
            battler.pbRaiseStatStageByAbility(stat, 1, battler, true);       // :2753
        });

        BattleHandlers.AbilityOnSwitchIn.add("SCREENCLEANER", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2757-2786
            Battler target = battler;                                        // :2759
            battle.showAbilitySplash(battler);                               // :2760
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0) { // :2761
                target.pbOwnSide().effects.set(PBEffects.Side.AuroraVeil, 0); // :2762
                battle.display(target.pbTeam(false) + "的极光幕消失了！");     // :2763
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.LightScreen) > 0) { // :2765
                target.pbOwnSide().effects.set(PBEffects.Side.LightScreen, 0); // :2766
                battle.display(target.pbTeam(false) + "的光墙消失了！");       // :2767
            }
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.Reflect) > 0) { // :2769
                target.pbOwnSide().effects.set(PBEffects.Side.Reflect, 0);    // :2770
                battle.display(target.pbTeam(false) + "的反射盾消失了！");     // :2771
            }
            if (target.pbOpposingSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0) { // :2773
                target.pbOpposingSide().effects.set(PBEffects.Side.AuroraVeil, 0); // :2774
                battle.display(target.pbOpposingTeam(false) + "的极光幕消失了！"); // :2775
            }
            if (target.pbOpposingSide().effects.intVal(PBEffects.Side.LightScreen) > 0) { // :2777
                target.pbOpposingSide().effects.set(PBEffects.Side.LightScreen, 0); // :2778
                battle.display(target.pbOpposingTeam(false) + "的光墙消失了！"); // :2779
            }
            // :2781 插件此处条件查的是 pbOwnSide 却写 pbOpposingSide（原样照抄）
            if (target.pbOwnSide().effects.intVal(PBEffects.Side.Reflect) > 0) {
                target.pbOpposingSide().effects.set(PBEffects.Side.Reflect, 0); // :2782
                battle.display(target.pbOpposingTeam(false) + "的反射盾消失了！"); // :2783
            }
            battle.hideAbilitySplash(battler);                               // :2785
        });

        BattleHandlers.AbilityOnSwitchIn.add("PASTELVEIL", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2789-2800
            battler.eachAlly(b -> {                                          // :2791
                if (!"POISON".equals(b.status)) {                            // :2792
                    return;
                }
                battle.showAbilitySplash(battler);                           // :2793
                b.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH); // :2794
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :2795 死代码
                    battle.display(battler.pbThis() + "的" + battler.abilityName()
                            + "治愈了" + b.pbThis(true) + "的中毒状态！");     // :2796
                }
                battle.hideAbilitySplash(battler);                           // :2798
            });
        });

        BattleHandlers.AbilityOnSwitchIn.add("CURIOUSMEDICINE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2803-2815
            boolean[] done = {false};                                        // :2805
            battler.eachAlly(b -> {                                          // :2806
                if (!b.hasAlteredStatStages()) {                             // :2807
                    return;
                }
                b.pbResetStatStages();                                       // :2808
                done[0] = true;                                              // :2809
            });
            if (done[0]) {                                                   // :2811
                battle.showAbilitySplash(battler);                           // :2812
                battle.display("所有队友的能力变化都被消除了！");              // :2813
                battle.hideAbilitySplash(battler);                           // :2814
            }
        });

        BattleHandlers.AbilityOnSwitchIn.add("NEUTRALIZINGGAS", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2819-2833
            if (battle.field.effects.truthy(PBEffects.Field.NeutralizingGas)) { // :2821
                return;
            }
            battle.showAbilitySplash(battler);                               // :2822
            battle.display(battler.pbThis() + "的气体无效化了所有特性！");     // :2823
            battle.field.effects.set(PBEffects.Field.NeutralizingGas, true);  // :2824
            battle.hideAbilitySplash(battler);                               // :2825
            for (Battler b : battle.allBattlers()) {                         // :2826
                if (b.hasActiveItem("ABILITYSHIELD")) {                      // :2827
                    String itemname = b.itemName();                          // :2828 PBItems.getName(b.item)
                    battle.display(b.pbThis() + "的特性\n被" + itemname + "的效果保护了！"); // :2829
                    continue;                                                // :2830
                }
            }
        });

        BattleHandlers.AbilityOnSwitchIn.add("DIMENSIONBODY", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2836-2848 次元之躯
            for (Battler b : battle.eachOtherSideBattler(battler.index)) {   // :2839
                if (b.hp != battler.hp) {                                    // :2840 双方 HP 不相等
                    if (b.hp < battler.hp) {                                 // :2841 对手 HP 少于自己
                        battler.pbRaiseStatStageByAbility(PBStats.SPATK, 2, battler, true); // :2842
                    } else if (b.hp > battler.hp) {                          // :2843 对手 HP 大于自己
                        battler.pbRaiseStatStageByAbility(PBStats.EVASION, 2, battler, true); // :2844
                    }
                }
            }
        });

        // =====================================================================
        // AbilityOnSwitchOut — BattleHandlers_Abilities.rb:2855-2873, :3196, :4503
        // =====================================================================
        BattleHandlers.AbilityOnSwitchOut.add("NATURALCURE", (ability, battler, endOfBattle) -> {
            // BattleHandlers_Abilities.rb:2855-2859
            pbDebugLog("[Ability triggered] " + battler.pbThis() + "'s " + battler.abilityName()); // :2857
            battler.cureStatus();                                            // :2858 self.status = NONE
        });

        BattleHandlers.AbilityOnSwitchOut.add("REGENERATOR", (ability, battler, endOfBattle) -> {
            // BattleHandlers_Abilities.rb:2862-2867
            if (endOfBattle) {                                               // :2864
                return;
            }
            pbDebugLog("[Ability triggered] " + battler.pbThis() + "'s " + battler.abilityName()); // :2865
            battler.pbRecoverHP(battler.maxHp() / 3, false, false);          // :2866 battler.totalhp/3
        });

        // =====================================================================
        // AbilityChangeOnBattlerFainting — BattleHandlers_Abilities.rb:2874-2896, :3174
        // =====================================================================
        BattleHandlers.AbilityChangeOnBattlerFainting.add("POWEROFALCHEMY", (ability, battler, fainted, battle) -> {
            // BattleHandlers_Abilities.rb:2874-2888
            if (battler.opposes(fainted)) {                                  // :2876
                return;
            }
            if (battler.hasActiveItem("ABILITYSHIELD")) {                    // :2877
                return;
            }
            if (fainted.ungainableAbility(null)                              // :2878
                    || "POWEROFALCHEMY".equals(fainted.ability)              // :2879
                    || "RECEIVER".equals(fainted.ability)                    // :2880
                    || "TRACE".equals(fainted.ability)                       // :2881
                    || "WONDERGUARD".equals(fainted.ability)) {              // :2882
                return;
            }
            battle.showAbilitySplash(battler);                               // :2883 (delay=true)
            battler.ability = fainted.ability;                               // :2884
            battle.replaceAbilitySplash(battler);                            // :2885
            battle.display(fainted.pbThis() + "的" + fainted.abilityName() + "被继承了！"); // :2886
            battle.hideAbilitySplash(battler);                               // :2887
        });
        BattleHandlers.AbilityChangeOnBattlerFainting.copy("POWEROFALCHEMY", "RECEIVER"); // :2891

        BattleHandlers.AbilityChangeOnBattlerFainting.add("RIGHTTOFIGHT", (ability, battler, fainted, battle) -> {
            // BattleHandlers_Abilities.rb:3174-3183 正义变身
            if (battler.opposes(fainted)) {                                  // :3176
                return;
            }
            if (!battler.isSpecies("KRICKETUNE") || battler.form() == 1) {   // :3177
                return;
            }
            battle.showAbilitySplash(battler);                               // :3178 (delay=true)
            battler.pbChangeFormTransform(1, null);                          // :3179
            battle.replaceAbilitySplash(battler);                            // :3180
            battle.display(battler.pbThis() + "为了倒下的" + fainted.pbThis()
                    + "\n而正义变身了！");                                    // :3181
            battle.hideAbilitySplash(battler);                               // :3182
        });

        // =====================================================================
        // AbilityOnBattlerFainting — BattleHandlers_Abilities.rb:2897-2901, :4199
        // =====================================================================
        BattleHandlers.AbilityOnBattlerFainting.add("SOULHEART", (ability, battler, fainted, battle) -> {
            // BattleHandlers_Abilities.rb:2897-2900
            battler.pbRaiseStatStageByAbility(PBStats.SPATK, 1, battler, true); // :2899
        });

        BattleHandlers.AbilityOnBattlerFainting.add("DIVINEPACT", (ability, battler, fainted, battle) -> {
            // BattleHandlers_Abilities.rb:4199-4202 圣约之链
            battler.pbRaiseStatStageByAbility(PBStats.SPATK, 1, battler, true); // :4201
        });

        // =====================================================================
        // AbilityOnSwitchIn (continued) — :2970-...
        // =====================================================================
        BattleHandlers.AbilityOnSwitchIn.add("COMMANDER", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2970-2996
            if (battler.effects.truthy(PBEffects.Battler.Commander)) {       // :2972
                return;
            }
            // :2973 `next if defined?(battler.dynamax?) && battler.dynamax?`
            // 登记: dynamax? 在插件里未定义（花名册 §4 缺陷 #2）；原文用 defined? 保护，
            //       所以 Ruby 里这段恒不成立 → Java 侧同样不进入（不新增 dynamax? 方法）
            boolean showAnim = true;                                         // :2974
            for (Battler b : battler.allAllies()) {                          // :2975
                if (b == null || !b.near(battler) || b.fainted()) {          // :2976
                    continue;
                }
                if (battle.choiceIsSwitch(b.index)) {                        // :2977 choices[b.index][0]==:SwitchOut
                    continue;
                }
                if (!b.isSpecies("DONDOZO")) {                               // :2978
                    continue;
                }
                if (b.effects.truthy(PBEffects.Battler.Commander)) {         // :2979
                    continue;
                }
                // :2980 `next if defined?(b.dynamax?) && b.dynamax?` —— 同 :2973 的登记
                battle.showAbilitySplash(battler);                           // :2981
                battle.pbClearChoice(battler.index);                         // :2982
                battle.display(battler.pbThis() + "进入了" + b.pbThis(true) + "的嘴里！"); // :2983
                // 登记: :2984 battle.scene.sprites["pokemon_#{battler.index}"].visible = false
                //       依赖 PokeBattle_Scene / Graphics（本批未建模）
                // :2985 b.effects[Commander] = [battler.index, battler.form]
                b.effects.set(PBEffects.Battler.Commander,
                        new int[] {battler.index, battler.form()});
                // :2986 battler.effects[Commander] = [b.index]
                battler.effects.set(PBEffects.Battler.Commander, new int[] {b.index});
                for (int stat : new int[] {PBStats.ATTACK, PBStats.DEFENSE, PBStats.SPATK,
                                           PBStats.SPDEF, PBStats.SPEED}) {   // :2987
                    if (!b.pbCanRaiseStatStage(stat, b)) {                   // :2988
                        continue;
                    }
                    if (b.pbRaiseStatStage(stat, 2, b, showAnim, false)) {   // :2989（ignoreContrary 默认 false）
                        showAnim = false;                                    // :2990
                    }
                }
                battle.hideAbilitySplash(battler);                           // :2993
                break;                                                       // :2994
            }
        });

        BattleHandlers.AbilityOnSwitchIn.add("COSTAR", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3007-3019 同台共演
            for (Battler b : battler.allAllies()) {                          // :3009
                if (b.index == battler.index) {                              // :3010
                    continue;
                }
                if (!b.hasAlteredStatStages()
                        && b.effects.intVal(PBEffects.Battler.FocusEnergy) == 0) { // :3011
                    continue;
                }
                battle.showAbilitySplash(battler);                           // :3012
                battler.effects.set(PBEffects.Battler.FocusEnergy,
                        b.effects.intVal(PBEffects.Battler.FocusEnergy));     // :3013
                for (int stat : PBStats.EACH_MAIN_BATTLE_STAT) {              // :3014 eachMainBattleStat
                    battler.setStage(stat, b.stage(stat));                     // :3014 走 stage/setStage 映射（stages 是 5 元错位）
                }
                battle.display(battler.pbThis() + "复制了\n" + b.pbThis(true) + "的能力变化！"); // :3015
                battle.hideAbilitySplash(battler);                           // :3016
                break;                                                       // :3017
            }
        });

        BattleHandlers.AbilityOnSwitchIn.add("WINDRIDER", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3074-3080 乘风
            if (battler.pbOwnSide().effects.intVal(PBEffects.Side.Tailwind) <= 0) { // :3076
                return;
            }
            if (!battler.pbCanRaiseStatStage(PBStats.ATTACK, battler)) {     // :3077
                return;
            }
            battler.pbRaiseStatStageByAbility(PBStats.ATTACK, 1, battler, true); // :3078
        });

        BattleHandlers.AbilityOnSwitchIn.add("HOSPITALITY", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3097-3108 款待
            boolean anyHealable = false;                                     // :3099 allAllies.empty? { |b| b.canHeal? }
            for (Battler b : battler.allAllies()) {
                if (b.canHeal()) {
                    anyHealable = true;
                    break;
                }
            }
            if (!anyHealable) {
                return;
            }
            battle.showAbilitySplash(battler);                               // :3100
            for (Battler b : battler.allAllies()) {                          // :3101
                if (!b.canHeal()) {                                          // :3102
                    continue;
                }
                int amt = b.maxHp() / 4;                                     // :3103 (b.totalhp/4).floor
                b.pbRecoverHP(amt);                                          // :3104
                battle.display(b.pbThis() + "喝光了" + battler.pbThis(true) + "做的抹茶！"); // :3105
            }
            battle.hideAbilitySplash(battler);                               // :3107
        });

        BattleHandlers.AbilityOnSwitchIn.add("SUPREMEOVERLORD", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3124-3133 大将
            // 登记: :3126 battler.num_fainted_allies 依赖 Battle#pbFaintedAllyCount
            //       （PokeBattle_Battle:833-836，Battle 未暴露）→ 无法取数，本 handler 空实现
            int numFainted = 0;
            if (numFainted <= 0) {                                           // :3127
                return;
            }
            battle.showAbilitySplash(battler);                               // :3128
            battle.display(battler.pbThis() + "从\n被打倒的同伴身上得到力量了！"); // :3129
            battler.effects.set(PBEffects.Battler.SupremeOverlord, numFainted); // :3130
            battle.hideAbilitySplash(battler);                               // :3131
        });

        BattleHandlers.AbilityOnSwitchIn.add("RIGHTTOFIGHT", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3162-3172 正义变身
            // 登记: :3164 battler.num_fainted_allies（同 SUPREMEOVERLORD 的登记）
            int numFainted = 0;
            if (numFainted <= 0) {                                           // :3165
                return;
            }
            if (!battler.isSpecies("KRICKETUNE") || battler.form() == 1) {   // :3166
                return;
            }
            battle.showAbilitySplash(battler);                               // :3167 (delay=true)
            battler.pbChangeFormTransform(1, null);                          // :3168
            battle.replaceAbilitySplash(battler);                            // :3169
            battle.display(battler.pbThis() + "为了\n倒下的队友而正义变身了！"); // :3170
            battle.hideAbilitySplash(battler);                               // :3171
        });

        BattleHandlers.AbilityOnSwitchIn.add("ZEROTOHERO", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3187-3194 全能变身
            if (!battler.isSpecies("PALAFIN")) {                             // :3189
                return;
            }
            if (battler.form() == 0) {                                       // :3190
                return;
            }
            battle.showAbilitySplash(battler);                               // :3191
            battle.display(battler.pbThis() + "变身后归来了！");              // :3192
            battle.hideAbilitySplash(battler);                               // :3193
        });

        BattleHandlers.AbilityOnSwitchOut.add("ZEROTOHERO", (ability, battler, endOfBattle) -> {
            // BattleHandlers_Abilities.rb:3196-3201
            if (!battler.isSpecies("PALAFIN")) {                             // :3198
                return;
            }
            if (battler.form() == 1 || endOfBattle) {                        // :3199
                return;
            }
            battler.pbChangeFormTransform(1, "");                            // :3200
        });

        BattleHandlers.AbilityOnSwitchIn.add("SUPERSWEETSYRUP", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3283-3303
            if (battler.effects.intVal(PBEffects.Battler.SupersweetSyrup) > 0) { // :3285 (x || 0) > 0
                return;
            }
            battler.effects.set(PBEffects.Battler.SupersweetSyrup, 1);       // :3286
            battle.showAbilitySplash(battler);                               // :3287
            battle.display("一股超甜的香气正从\n覆盖着" + battler.pbThis(true) + "的糖浆中飘散出来！"); // :3288
            for (Battler b : battle.allBattlers()) {                         // :3289 battle.battlers
                if (b == null || b.fainted()) {                              // :3290
                    continue;
                }
                if (!b.opposes(battler)) {                                   // :3291
                    continue;
                }
                if (b.hasActiveAbility("CLEARBODY")                          // :3292-3294
                        || b.hasActiveAbility("WHITESMOKE")
                        || b.hasActiveAbility("TRANSLUCENTGHOST")) {
                    if (b.effects.intVal(PBEffects.Battler.Substitute) == 0) { // :3295
                        battle.display(b.pbThis() + "的" + b.abilityName()
                                + "防止了能力值降低！");                     // :3296
                    }
                    continue;                                                // :3298
                }
                b.pbLowerStatStageByAbility(PBStats.EVASION, 1, battler, false, false); // :3300
            }
            battle.hideAbilitySplash(battler);                               // :3302
        });

        BattleHandlers.AbilityOnSwitchIn.add("ORICHALCUMPULSE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3325-3335 绯红脉动
            int w = battler.effectiveWeather();                              // :3327
            if (w == PBWeather.Sun || w == PBWeather.HarshSun) {
                battle.showAbilitySplash(battler);                           // :3328 (delay=true)
                battle.display(battler.pbThis() + "沐浴在阳光下，\n激起了古代的脉动！！"); // :3329
                battle.hideAbilitySplash(battler);                           // :3330
            } else {
                battle.display(battler.pbThis() + "令日照变强，\n激起了古代的脉动！！"); // :3332
                BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Sun, battler, battle); // :3333
            }
        });

        BattleHandlers.AbilityOnSwitchIn.add("HADRONENGINE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3346-3356 强子引擎
            battle.showAbilitySplash(battler);                               // :3348 (delay=true)
            if (battle.field.terrain == PBBattleTerrains.Electric) {         // :3349
                battle.display(battler.pbThis() + "利用电气场地\n使未来的机关悦动起来！！"); // :3350
                battle.hideAbilitySplash(battler);                           // :3351
            } else {
                battle.display(battler.pbThis() + "布下电气场地\n使未来的机关悦动起来！！"); // :3353
                PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Electric); // :3354
            }
        });

        BattleHandlers.AbilityOnSwitchIn.add("TERASHIFT", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3368-3375 太晶变形
            if (!battler.isSpecies("TERAPAGOS")) {                           // :3370
                return;
            }
            if (battler.form() > 0) {                                        // :3371
                return;
            }
            battle.showAbilitySplash(battler);                               // :3372 (delay=true)
            battle.hideAbilitySplash(battler);                               // :3373
            battler.pbChangeFormTransform(1, battler.pbThis() + "的样子发生了变化！"); // :3374
        });

        BattleHandlers.AbilityOnSwitchIn.add("TERAFORMZERO", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3389-3443 归零化境
            int weather = battle.field.weather;                              // :3391
            int terrain = battle.field.terrain;                              // :3392
            if (weather == PBWeather.None && terrain == PBBattleTerrains.None) { // :3393
                return;
            }
            boolean showSplash = false;                                      // :3394
            // :3395 插件此处用 PBBattleTerrains::None 比较 defaultWeather（两者都是 0，行为不变）
            if (weather != PBWeather.None && battle.field.defaultWeather == PBBattleTerrains.None) {
                showSplash = true;                                           // :3396
                battle.showAbilitySplash(battler);                           // :3397
                battle.field.weather = PBWeather.None;                       // :3398
                battle.field.weatherDuration = 0;                            // :3399
                switch (weather) {                                           // :3401-3419
                    case PBWeather.Sun:         battle.display("阳光暗淡了。"); break;
                    case PBWeather.Rain:        battle.display("大雨停止了。"); break;
                    case PBWeather.Sandstorm:   battle.display("沙暴平息了。"); break;
                    case PBWeather.Hail:        battle.display("冰雹停止了。"); break;
                    case PBWeather.Snow:        battle.display("不再下雪了。"); break;
                    case PBWeather.HarshSun:    battle.display("刺眼的阳光暗淡了！"); break;
                    case PBWeather.HeavyRain:   battle.display("倾盆大雨停止了！"); break;
                    case PBWeather.StrongWinds: battle.display("神秘的气流消散了！"); break;
                    default:                    battle.display("天气回归了正常。"); break;
                }
            }
            if (terrain != PBBattleTerrains.None
                    && battle.field.defaultTerrain == PBBattleTerrains.None) { // :3421
                if (!showSplash) {                                           // :3422
                    battle.showAbilitySplash(battler);
                }
                battle.field.terrain = PBBattleTerrains.None;                // :3423
                battle.field.terrainDuration = 0;                            // :3424
                switch (terrain) {                                           // :3426-3436
                    case PBBattleTerrains.Electric: battle.display("电光从场上消失了！"); break;
                    case PBBattleTerrains.Grassy:   battle.display("草从场上消失了！"); break;
                    case PBBattleTerrains.Psychic:  battle.display("迷雾从场上消失了！"); break;
                    case PBBattleTerrains.Misty:    battle.display("诡异从场上消失了！"); break;
                    default:                        battle.display("场地回归了正常。"); break;
                }
            }
            if (!showSplash) {                                               // :3438
                return;
            }
            battle.hideAbilitySplash(battler);                               // :3439
            for (Battler b : battle.allBattlers()) {                         // :3440
                b.pbCheckFormOnWeatherChange();
            }
            for (Battler b : battle.allBattlers()) {                         // :3441
                b.pbAbilityOnTerrainChange(false);
            }
            for (Battler b : battle.allBattlers()) {                         // :3442
                b.pbItemTerrainStatBoostCheck();
            }
        });

        BattleHandlers.AbilityOnSwitchIn.add("TABLETSOFRUIN", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3459-3473 灾祸之X
            String statName = null;                                          // :3461-3469
            if ("TABLETSOFRUIN".equals(ability)) {
                statName = PBStats.getName(PBStats.ATTACK);
            } else if ("SWORDOFRUIN".equals(ability)) {
                statName = PBStats.getName(PBStats.DEFENSE);
            } else if ("VESSELOFRUIN".equals(ability)) {
                statName = PBStats.getName(PBStats.SPATK);
            } else if ("BEADSOFRUIN".equals(ability)) {
                statName = PBStats.getName(PBStats.SPDEF);
            } else if ("TURBOBLAZE".equals(ability)) {
                statName = PBStats.getName(PBStats.SPDEF);
            } else if ("TERAVOLT".equals(ability)) {
                statName = PBStats.getName(PBStats.DEFENSE);
            } else if ("CALAMITYAERIAL".equals(ability)) {
                statName = PBStats.getName(PBStats.SPEED);
            }
            battle.showAbilitySplash(battler);                               // :3470
            battle.display(battler.pbThis() + "的" + battler.abilityName()
                    + "\n使周围宝可梦的" + statName + "降低了!");              // :3471
            battle.hideAbilitySplash(battler);                               // :3472
        });
        BattleHandlers.AbilityOnSwitchIn.copy("TABLETSOFRUIN", "SWORDOFRUIN", "VESSELOFRUIN",
                "BEADSOFRUIN", "TURBOBLAZE", "TERAVOLT", "CALAMITYAERIAL");   // :3475

        BattleHandlers.AbilityOnSwitchIn.add("PROTOSYNTHESIS", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3478-3525 古代活性 / 夸克充能
            if (battler.effects.truthy(PBEffects.Battler.Transform)) {       // :3480
                return;
            }
            boolean fieldCheck;                                              // :3481-3486
            if ("PROTOSYNTHESIS".equals(ability)) {
                int w = battle.weather();                                    // :3483 battle.pbWeather
                fieldCheck = w == PBWeather.Sun || w == PBWeather.HarshSun;
            } else if ("QUARKDRIVE".equals(ability)) {
                fieldCheck = battle.field.terrain == PBBattleTerrains.Electric; // :3485
            } else {
                fieldCheck = false;
            }
            if (!fieldCheck && !battler.effects.truthy(PBEffects.Battler.BoosterEnergy)
                    && battler.effects.truthy(PBEffects.Battler.ParadoxStat)) { // :3487
                battle.display(battler.pbThis(true) + "的" + battler.abilityName()
                        + "\n效果消失了！");                                  // :3488
                battler.effects.set(PBEffects.Battler.ParadoxStat, (Object) null); // :3489 = nil
            }
            if (battler.effects.truthy(PBEffects.Battler.ParadoxStat)) {     // :3491
                return;
            }
            if (!fieldCheck && !"BOOSTERENERGY".equals(battler.item)) {      // :3492 isConst?(item,PBItems,:BOOSTERENERGY)
                return;
            }
            int highestStat = -1;                                            // :3493
            int highestStatVal = 0;                                          // :3494
            int[] stageMul = {2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8};        // :3495
            int[] stageDiv = {8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2};        // :3496
            int[] plain = battler.plainStats();                              // :3497
            for (int stat = 0; stat < plain.length; stat++) {                // each_with_index
                int val = plain[stat];
                if (val == 0) {                                              // :3498 next if !val
                    continue;
                }
                int stage = battler.stage(stat) + 6;                          // :3499 battler.stages[stat]
                int realStat = (int) Math.floor(val * stageMul[stage] / stageDiv[stage]); // :3500
                if (realStat > highestStatVal) {                             // :3501
                    highestStatVal = realStat;
                    highestStat = stat;                                      // :3503
                }
            }
            if (highestStat >= 0) {                                          // :3506
                battle.showAbilitySplash(battler);                           // :3507
                if (fieldCheck) {                                            // :3508
                    String cause = null;                                     // :3509-3514
                    if ("PROTOSYNTHESIS".equals(ability)) {
                        cause = "强烈的阳光";
                    } else if ("QUARKDRIVE".equals(ability)) {
                        cause = "电气场地";
                    }
                    battle.display(cause + "激活了\n" + battler.pbThis(true)
                            + "的" + battler.abilityName() + "！");           // :3515
                } else if ("BOOSTERENERGY".equals(battler.item)) {           // :3516
                    battler.effects.set(PBEffects.Battler.BoosterEnergy, true); // :3517
                    battle.display(battler.pbThis() + "使用它的" + battler.itemName()
                            + "\n激活了" + battler.abilityName() + "！");      // :3518
                    battler.pbHeldItemTriggered(battler.item, 0, false);      // :3519
                }
                battler.effects.set(PBEffects.Battler.ParadoxStat, highestStat); // :3521
                battle.display(battler.pbThis() + "的" + PBStats.getName(highestStat)
                        + "提高了！");                                       // :3522
                battle.hideAbilitySplash(battler);                           // :3523
            }
        });
        BattleHandlers.AbilityOnSwitchIn.copy("PROTOSYNTHESIS", "QUARKDRIVE"); // :3527

        // =====================================================================
        // AbilityOnTerrainChange — BattleHandlers_Abilities.rb:3529-3532
        // =====================================================================
        BattleHandlers.AbilityOnTerrainChange.add("QUARKDRIVE", (ability, battler, battle, abilityChanged) -> {
            // BattleHandlers_Abilities.rb:3529-3532
            // :3531 BattleHandlers::AbilityOnSwitchIn.trigger(ability,battler,battle)
            BattleHandlers.triggerAbilityOnSwitchIn(ability, battler, battle);
        });

        // =====================================================================
        // AbilityOnSwitchIn (continued) — :3571-...
        // =====================================================================
        BattleHandlers.AbilityOnSwitchIn.add("BROKENBREATH", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3571-3576 —— 第一次注册（:3606 的第二次会覆盖它）
            battle.showAbilitySplash(battler);                               // :3573
            battle.display(battler.pbThis() + "的破灭之息降低了\n其他宝可梦的特防！"); // :3574
            battle.hideAbilitySplash(battler);                               // :3575
        });

        BattleHandlers.AbilityOnSwitchIn.add("BROKENBREATH", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3606-3611 —— 第二次注册，按 HandlerHash#add 覆盖上面那条
            battle.showAbilitySplash(battler);                               // :3608
            battle.display(battler.pbThis() + "的破灭之息降低了\n在场所有宝可梦的特防！"); // :3609
            battle.hideAbilitySplash(battler);                               // :3610
        });

        BattleHandlers.AbilityOnSwitchIn.add("THUNDERCLOUD", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3623-3629 雷云
            if (battle.field.terrain == PBBattleTerrains.Electric) {         // :3625
                return;
            }
            battle.showAbilitySplash(battler);                               // :3626
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Electric); // :3627
        });

        BattleHandlers.AbilityOnSwitchIn.add("ENDLESSDARKN", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3654-3658 无限暗
            battle.showAbilitySplash(battler);                               // :3656
            battle.display(battler.pbThis() + "降下了黑暗！");                // :3657
            battle.hideAbilitySplash(battler);                               // :3658
        });

        BattleHandlers.AbilityOnSwitchIn.add("ETRTNALIGHT", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3661-3665 无限光
            battle.showAbilitySplash(battler);                               // :3663
            battle.display(battler.pbThis() + "降下了圣光！");                // :3664
            battle.hideAbilitySplash(battler);                               // :3665
        });

        BattleHandlers.AbilityOnSwitchIn.add("STARFISSURE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3707-3719 星界裂隙
            battle.showAbilitySplash(battler);                               // :3709
            battle.display(battler.pbThis() + "布下了星界丝缕！");            // :3710
            if (battle.field.effects.intVal(PBEffects.Field.TrickRoom) > 0) { // :3711
                battle.field.effects.set(PBEffects.Field.TrickRoom, 0);      // :3712
                battle.display(battler.pbThis() + "使空间恢复正常了！");      // :3713
            } else {
                battle.field.effects.set(PBEffects.Field.TrickRoom, 8);      // :3715
                battle.display(battler.pbThis() + "扭曲了时空！");            // :3716
            }
            battle.hideAbilitySplash(battler);                               // :3718
        });

        BattleHandlers.AbilityOnSwitchIn.add("BLACKHAND", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3758-3762
            int stat = PBStats.ACCURACY;                                     // :3760
            battler.pbRaiseStatStageByAbility(stat, 1, battler, true);       // :3761
        });

        BattleHandlers.AbilityOnSwitchIn.add("SLEEPSOUNDLY", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3778-3785 酣眠
            if (!battler.isSpecies("ROSEDRAGON")) {                          // :3780
                return;
            }
            if (!battler.pbCanSleep(battler, false, null, false)) {          // :3781
                return;
            }
            battle.showAbilitySplash(battler);                               // :3782
            // 登记: :3783 battler.pbSleepSelf(msg,2) —— Battler 尚无 pbSleepSelf
            //       （Battler_Statuses:358-360，需要 pbSleepDuration）
            battle.hideAbilitySplash(battler);                               // :3784
        });

        BattleHandlers.AbilityOnSwitchIn.add("STEELDYNASTY", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3857-3864 钢铁始皇
            if (battle.field.effects.truthy(PBEffects.Field.NeutralizingGas)) { // :3859
                return;
            }
            battle.showAbilitySplash(battler);                               // :3860
            battle.display(battler.pbThis() + "的钢铁始皇使所有特性失效！");   // :3861
            battle.field.effects.set(PBEffects.Field.NeutralizingGas, true);  // :3862
            battle.hideAbilitySplash(battler);                               // :3863
        });

        BattleHandlers.AbilityOnSwitchIn.add("AURAVOICE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3868-3875 欧若拉之声
            if (battler.pbOwnSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0) { // :3870
                return;
            }
            battle.showAbilitySplash(battler);                               // :3871
            battler.pbOwnSide().effects.set(PBEffects.Side.AuroraVeil, 5);    // :3872
            battle.display(battler.pbTeam(true) + "受到的物理和特殊伤害减弱了！"); // :3873
            battle.hideAbilitySplash(battler);                               // :3874
        });

        BattleHandlers.AbilityOnSwitchIn.add("RADIANTRULE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3944-3949
            if (battle.field.terrain == PBBattleTerrains.Psychic) {          // :3946
                return;
            }
            battle.showAbilitySplash(battler);                               // :3947
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Psychic); // :3948
        });

        BattleHandlers.AbilityOnSwitchIn.add("CALAMITYABYSSAL", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3953-3959 灾厄·海孽
            battle.showAbilitySplash(battler);                               // :3955
            battle.display(battler.pbThis() + "带来了灾厄般的威压……");       // :3956
            battle.hideAbilitySplash(battler);                               // :3957
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Rain, battler, battle); // :3958
        });

        BattleHandlers.AbilityOnSwitchIn.add("CALAMITYINFERNAL", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3962-3968 灾厄·焱祟
            battle.showAbilitySplash(battler);                               // :3964
            battle.display(battler.pbThis() + "带来了灾厄般的威压……");       // :3965
            battle.hideAbilitySplash(battler);                               // :3966
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Sun, battler, battle); // :3967
        });

        BattleHandlers.AbilityOnSwitchIn.add("ROSEGARDEN", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3972-3978 蔷薇花园
            if (battle.field.terrain == PBBattleTerrains.Grassy) {           // :3974
                return;
            }
            battle.showAbilitySplash(battler);                               // :3975
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Grassy); // :3976
        });

        BattleHandlers.AbilityOnSwitchIn.add("ROSEARCADIA", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3989-3995 蔷薇箱庭
            if (battle.field.terrain == PBBattleTerrains.Grassy) {           // :3991
                return;
            }
            battle.showAbilitySplash(battler);                               // :3992
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Grassy); // :3993
        });

        BattleHandlers.AbilityOnSwitchIn.add("VERDANTWARD", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4008-4014 翠野护场
            if (battle.field.terrain == PBBattleTerrains.Grassy) {           // :4010
                return;
            }
            battle.showAbilitySplash(battler);                               // :4011
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Grassy); // :4012
        });

        BattleHandlers.AbilityOnSwitchIn.add("PSYCHICEDGE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4026-4032
            if (battle.field.terrain == PBBattleTerrains.Psychic) {          // :4028
                return;
            }
            battle.showAbilitySplash(battler);                               // :4029
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Psychic); // :4030
        });

        BattleHandlers.AbilityOnSwitchIn.add("ETERNALFLAME", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4036-4042 永恒之焱
            battle.showAbilitySplash(battler);                               // :4038
            battle.display(battler.pbThis() + "降下了圣光");                  // :4039
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Sun, battler, battle); // :4040
            battle.hideAbilitySplash(battler);                               // :4041
        });

        BattleHandlers.AbilityOnSwitchIn.add("ROSYAEGIS", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4170-4177
            if (battler.pbOwnSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0) { // :4172
                return;
            }
            battle.showAbilitySplash(battler);                               // :4173
            battler.pbOwnSide().effects.set(PBEffects.Side.AuroraVeil, 5);    // :4174
            battle.display(battler.pbTeam(true) + "召唤了光之花幕！\n受到的物理和特殊伤害减弱了！"); // :4175
            battle.hideAbilitySplash(battler);                               // :4176
        });

        BattleHandlers.AbilityOnSwitchIn.add("CONFESSIONLIST", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4235-4243 忏悔表
            battle.showAbilitySplash(battler);                               // :4237
            for (Battler b : battle.eachOtherSideBattler(battler.index)) {   // :4238
                battle.display(b.pbThis() + "被" + battler.pbThis(true) + "的忏悔表束缚了！"); // :4239-4240
            }
            battle.hideAbilitySplash(battler);                               // :4242
        });

        BattleHandlers.AbilityOnSwitchIn.add("SUPERSUN", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4267-4272 超级太阳
            battle.showAbilitySplash(battler);                               // :4269
            battle.display(battler.pbThis() + "散发出如烈日般的光芒！");      // :4270
            battle.hideAbilitySplash(battler);                               // :4271
        });

        BattleHandlers.AbilityOnSwitchIn.add("BESTOWEDRAIN", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4309-4312 悲雨落叹
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Rain, battler, battle); // :4311
        });

        BattleHandlers.AbilityOnSwitchIn.add("SHINYHERO", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4494-4501
            if (!battler.isSpecies("SHINYGARCHOMP")) {                       // :4496
                return;
            }
            if (battler.form() == 0) {                                       // :4497
                return;
            }
            battle.showAbilitySplash(battler);                               // :4498
            battle.display(battler.pbThis() + "变身后归来了！");              // :4499
            battle.hideAbilitySplash(battler);                               // :4500
        });

        BattleHandlers.AbilityOnSwitchOut.add("SHINYHERO", (ability, battler, endOfBattle) -> {
            // BattleHandlers_Abilities.rb:4503-4508
            if (!battler.isSpecies("SHINYGARCHOMP")) {                       // :4505
                return;
            }
            if (battler.form() == 1 || endOfBattle) {                        // :4506
                return;
            }
            battler.pbChangeFormTransform(1, "");                            // :4507
        });

        BattleHandlers.AbilityOnSwitchIn.add("DEEPSEAFASCINATION", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4518-4527 深海幻惑
            battle.showAbilitySplash(battler);                               // :4520
            Battler target = battler.pbDirectOpposing(false);                // :4521
            if (target != null && !target.fainted()                        // :4522
                    && target.pbCanLowerStatStage(PBStats.SPATK, battler)) { // :4523
                target.pbLowerStatStage(PBStats.SPATK, 1, battler);          // :4524
            }
            battle.hideAbilitySplash(battler);                               // :4526
        });

        BattleHandlers.AbilityOnSwitchIn.add("STORMEYE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4559-4562 风暴之眼
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Rain, battler, battle); // :4561
        });

        BattleHandlers.AbilityOnSwitchIn.add("RAINBOWARCH", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4573-4576 虹霓之穹
            BattleHandlerHelpers.pbBattleWeatherAbility(PBWeather.Sun, battler, battle); // :4575
        });
    }
}
