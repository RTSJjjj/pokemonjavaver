package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 4 / L1': the registrations that happen OUTSIDE the two
 * {@code BattleHandlers_*} files and therefore must run last (roster §9).
 *
 * <ul>
 * <li>{@code Arceus:775-983}: 10 entries that OVERWRITE main-table handlers -
 *     INSOMNIA ({@code :775} StatusImmunityAbility, {@code :798} StatusCureAbility),
 *     SWEETVEIL ({@code :782}), MAGMAARMOR ({@code :790}), SYNCHRONIZE
 *     ({@code :810}), HEALER ({@code :871}), HYDRATION ({@code :902}), SHEDSKIN
 *     ({@code :934}), CHESTOBERRY ({@code :970}), LUMBERRY ({@code :983}).</li>
 * <li>{@code 场地:597-751}: 6 new entries - BUGLURESEED ({@code :597}),
 *     COLDSEED ({@code :614}), FROZENARMOR ({@code :707}), TRAPTRICK
 *     ({@code :720}), BUGLURESURGE ({@code :735}), COLDSURGE ({@code :751}).</li>
 * </ul>
 *
 * <p>{@code HandlerHash#add} overwrites ({@code Event_Handlers.rb:110-113}), so
 * this must run after {@code AbilitiesSwitchIn..ItemsOnHit}; the order is fixed
 * by {@link BattleHandlerRegistry}. The two blocks are kept in that exact order
 * (Arceus first, 场地 second) because that is the plugin's load order.</p>
 *
 * <h2>Translations used in this file</h2>
 * <ul>
 * <li>Status comparisons: the runtime stores {@code battler.status} as the
 *     internal-name String (PBStatuses javadoc), so {@code battler.status==PBStatuses::X}
 *     is written {@code PBStatuses.idOf(battler.status)==PBStatuses.X} - the
 *     bridge PBStatuses already provides for exactly this.</li>
 * <li>{@code _INTL("{1}…{2}",a,b)}: concatenated at the call site, the
 *     convention {@code BattleHandlerHelpers.java:36/51} documents.</li>
 * <li>{@code battle.pbDisplay} / {@code pbShowAbilitySplash} /
 *     {@code pbHideAbilitySplash} / {@code pbCommonAnimation} use this runtime's
 *     landed methods ({@code Battle.display/showAbilitySplash/hideAbilitySplash/commonAnimation},
 *     task-13).</li>
 * <li>{@code PBItems.getName(item)} is {@link #itemName}: the item's name out of
 *     the same {@code PbsData.Items} table ({@code Compiler_PBS:446-448} generates
 *     {@code PBItems.getName} as a message-table lookup, which returns {@code ""}
 *     for a missing entry).</li>
 * <li>{@code PBDebug.log} is a debug-only log with no counterpart in this
 *     runtime; each call site is kept as a {@code 登记} comment
 *     (the convention {@code BattleHandlerHelpers} already uses).</li>
 * <li>{@code PokeBattle_SceneConstants::USE_ABILITY_SPLASH} is <b>true</b> in this
 *     project ({@code PokeBattle_SceneConstants.java}), so every
 *     {@code if !…USE_ABILITY_SPLASH} message branch below is dead code. It is
 *     transcribed line by line anyway.</li>
 * <li>{@code PBBattleTerrains::BugLure} ({@code 场地:6 BugLure = 5}) and
 *     {@code PBBattleTerrains::Cold} ({@code 场地:255 Cold = 6}) are declared by
 *     the {@code 场地} section re-opening that module; they live in
 *     {@link PBBattleTerrains} with those line references.</li>
 * <li>{@code battle.pbStartTerrain} ({@code PokeBattle_Battle:741}) is not
 *     modelled yet, so the two {@code …SURGE} entries call the
 *     {@code PendingApi.pbStartTerrain} stub.</li>
 * </ul>
 */
final class HandlerArceusOverrides {

    private HandlerArceusOverrides() {
    }

    /**
     * {@code Arceus:775-983} then {@code 场地:597-751} (roster §9: 10 overrides,
     * 6 additions).
     */
    static void register() {
        // ==================================================================
        // Arceus:775-983 - 10 handlers that overwrite the main table
        // ==================================================================

        BattleHandlers.StatusImmunityAbility.add("INSOMNIA", (ability, battler, status) -> {
            // Arceus:775-780
            if (status == PBStatuses.SLEEP) {                                // :777
                return true;
            }
            if (status == PBStatuses.DROWSY) {                               // :778
                return true;
            }
            return null;
        });

        BattleHandlers.StatusImmunityAllyAbility.add("SWEETVEIL", (ability, battler, status) -> {
            // Arceus:782-787
            if (status == PBStatuses.SLEEP) {                                // :784
                return true;
            }
            if (status == PBStatuses.DROWSY) {                               // :785
                return true;
            }
            return null;
        });

        BattleHandlers.StatusImmunityAbility.add("MAGMAARMOR", (ability, battler, status) -> {
            // Arceus:790-795
            if (status == PBStatuses.FROZEN) {                               // :792
                return true;
            }
            if (status == PBStatuses.FROSTBITE) {                            // :793
                return true;
            }
            return null;
        });

        BattleHandlers.StatusCureAbility.add("INSOMNIA", (ability, battler) -> {
            // Arceus:798-808
            int status = PBStatuses.idOf(battler.status);                    // :800
            if (status != PBStatuses.SLEEP && status != PBStatuses.DROWSY) {
                return null;
            }
            battler.battle.showAbilitySplash(battler);                       // :801
            battler.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :802
            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {             // :803
                battler.battle.display(battler.pbThis() + "的" + battler.abilityName()
                        + "使它醒来了！");                                    // :804
            }
            battler.battle.hideAbilitySplash(battler);                       // :806
            return null;
        });

        BattleHandlers.AbilityOnStatusInflicted.add("SYNCHRONIZE", (ability, battler, user, status) -> {
            // Arceus:810-868
            if (user == null || user.index == battler.index) {               // :812
                return;
            }
            switch (status) {                                                // :813
                case PBStatuses.POISON:                                      // :814
                    if (user.pbCanPoisonSynchronize(battler)) {              // :815
                        battler.battle.showAbilitySplash(battler);           // :816
                        String msg = null;                                   // :817
                        if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) { // :818
                            msg = battler.pbThis() + "的" + battler.abilityName()
                                    + "使" + user.pbThis(true) + "中毒了！";     // :819
                        }
                        user.pbPoison(null, msg, battler.statusCount > 0);   // :821
                        battler.battle.hideAbilitySplash(battler);           // :822
                    }
                    break;
                case PBStatuses.BURN:                                        // :824
                    if (user.pbCanBurnSynchronize(battler)) {                // :825
                        battler.battle.showAbilitySplash(battler);           // :826
                        String msg = null;                                   // :827
                        if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) { // :828
                            msg = battler.pbThis() + "的" + battler.abilityName()
                                    + "使" + user.pbThis(true) + "灼伤了！";     // :829
                        }
                        user.pbBurn(null, msg);                              // :831
                        battler.battle.hideAbilitySplash(battler);           // :832
                    }
                    break;
                case PBStatuses.PARALYSIS:                                   // :834
                    if (user.pbCanParalyzeSynchronize(battler)) {            // :835
                        battler.battle.showAbilitySplash(battler);           // :836
                        String msg = null;                                   // :837
                        if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) { // :838
                            msg = battler.pbThis() + "的" + battler.abilityName()
                                    + "使" + user.pbThis(true) + "麻痹了！\n"
                                    + user.pbThis(true) + "有可能无法行动！";      // :839-840
                        }
                        user.pbParalyze(null, msg);                          // :842
                        battler.battle.hideAbilitySplash(battler);           // :843
                    }
                    break;
                case PBStatuses.FROSTBITE:                                   // :845
                    if (PendingApi.pbCanFrostbiteSynchronize(user, battler)) {   // :846
                        battler.battle.showAbilitySplash(battler);           // :847
                        String msg = null;                                   // :848
                        if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) { // :849
                            msg = battler.pbThis() + "的" + battler.abilityName()
                                    + "使" + user.pbThis(true) + "陷入了冻伤！";  // :850
                        }
                        PendingApi.pbFrostbite(user, msg);                   // :852
                        battler.battle.hideAbilitySplash(battler);           // :853
                    }
                    break;
                case PBStatuses.DROWSY:                                      // :855
                    if (PendingApi.pbCanDrowseSynchronize(user, battler)) {   // :856
                        battler.battle.showAbilitySplash(battler);           // :857
                        String msg = null;                                   // :858
                        if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) { // :859
                            msg = battler.pbThis() + "'s " + battler.abilityName()
                                    + " made " + user.pbThis(true) + " drowsy!";   // :860-861
                        }
                        PendingApi.pbDrowse(user, msg);                      // :863
                        battler.battle.hideAbilitySplash(battler);           // :864
                    }
                    break;
                default:
                    break;
            }
        });

        BattleHandlers.EORHealingAbility.add("HEALER", (ability, battler, battle) -> {
            // Arceus:871-900
            if (!(battle.pbRandom(100) < 30)) {                              // :873 next unless …<30
                return;
            }
            battler.eachAlly(ally -> {                                      // :874
                if (PBStatuses.idOf(ally.status) == PBStatuses.NONE) {       // :875
                    return;
                }
                battle.showAbilitySplash(battler);                           // :876
                int oldStatus = PBStatuses.idOf(ally.status);                // :877
                ally.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :878
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :879
                    switch (oldStatus) {                                     // :880
                        case PBStatuses.SLEEP:                               // :881
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "把伙伴吵醒了！");                        // :882
                            break;
                        case PBStatuses.POISON:                              // :883
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了伙伴的毒！");                      // :884
                            break;
                        case PBStatuses.BURN:                                // :885
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了伙伴的灼伤！");                    // :886
                            break;
                        case PBStatuses.PARALYSIS:                           // :887
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了伙伴的麻痹！");                    // :888
                            break;
                        case PBStatuses.FROZEN:                              // :889
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "解冻了伙伴！");                          // :890
                            break;
                        case PBStatuses.FROSTBITE:                           // :891
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治好了伙伴的冻伤！");                    // :892
                            break;
                        case PBStatuses.DROWSY:                              // :893
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "唤醒了昏昏欲睡的伙伴！");                // :894
                            break;
                        default:
                            break;
                    }
                }
                battle.hideAbilitySplash(battler);                           // :897
            });
        });

        BattleHandlers.EORHealingAbility.add("HYDRATION", (ability, battler, battle) -> {
            // Arceus:902-932
            if (!battler.hasUtilityUmbrella()) {                             // :904
                if (PBStatuses.idOf(battler.status) == PBStatuses.NONE) {    // :905
                    return;
                }
                int curWeather = battle.pbWeather();                         // :906
                if (curWeather != PBWeather.Rain && curWeather != PBWeather.HeavyRain) {   // :907
                    return;
                }
                battle.showAbilitySplash(battler);                           // :908
                int oldStatus = PBStatuses.idOf(battler.status);             // :909
                battler.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :910
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :911
                    switch (oldStatus) {                                     // :912
                        case PBStatuses.SLEEP:                               // :913
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "使它醒来了！");                          // :914
                            break;
                        case PBStatuses.POISON:                              // :915
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了毒！");                            // :916
                            break;
                        case PBStatuses.BURN:                                // :917
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了灼伤！");                          // :918
                            break;
                        case PBStatuses.PARALYSIS:                           // :919
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了麻痹！");                          // :920
                            break;
                        case PBStatuses.FROZEN:                              // :921
                            battle.display(battler.abilityName() + "解冻了" + battler.pbThis()
                                    + "！");                                    // :922
                            break;
                        case PBStatuses.FROSTBITE:                           // :923
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治好了伙伴的冻伤！");                    // :924
                            break;
                        case PBStatuses.DROWSY:                              // :925
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "唤醒了昏昏欲睡的伙伴！");                // :926
                            break;
                        default:
                            break;
                    }
                }
                battle.hideAbilitySplash(battler);                           // :929
            }
        });

        BattleHandlers.EORHealingAbility.add("SHEDSKIN", (ability, battler, battle) -> {
            // Arceus:934-961
            if (PBStatuses.idOf(battler.status) == PBStatuses.NONE) {        // :936
                return;
            }
            if (!(battle.pbRandom(100) < 30)) {                              // :937 next unless …<30
                return;
            }
            battle.showAbilitySplash(battler);                               // :938
            int oldStatus = PBStatuses.idOf(battler.status);                 // :939
            battler.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :940
            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {             // :941
                switch (oldStatus) {                                         // :942
                    case PBStatuses.SLEEP:                                   // :943
                        battle.display(battler.pbThis() + "的" + battler.abilityName()
                                + "使它醒来了！");                              // :944
                        break;
                    case PBStatuses.POISON:                                  // :945
                        battle.display(battler.pbThis() + "的" + battler.abilityName()
                                + "治愈了毒！");                                // :946
                        break;
                    case PBStatuses.BURN:                                    // :947
                        battle.display(battler.pbThis() + "的" + battler.abilityName()
                                + "治愈了灼伤！");                              // :948
                        break;
                    case PBStatuses.PARALYSIS:                               // :949
                        battle.display(battler.pbThis() + "的" + battler.abilityName()
                                + "治愈了麻痹！");                              // :950
                        break;
                    case PBStatuses.FROZEN:                                  // :951
                        battle.display(battler.abilityName() + "解冻了" + battler.pbThis()
                                + "！");                                        // :952
                        break;
                    case PBStatuses.FROSTBITE:                               // :953
                        battle.display(battler.pbThis() + "的" + battler.abilityName()
                                + "治好了伙伴的冻伤！");                        // :954
                        break;
                    case PBStatuses.DROWSY:                                  // :955
                        battle.display(battler.pbThis() + "的" + battler.abilityName()
                                + "唤醒了昏昏欲睡的伙伴！");                    // :956
                        break;
                    default:
                        break;
                }
            }
            battle.hideAbilitySplash(battler);                               // :959
        });

        BattleHandlers.StatusCureItem.add("CHESTOBERRY", (item, battler, battle, forced) -> {
            // Arceus:970-981
            if (!forced && battler.isUnnerved()) {                           // :972
                return false;
            }
            int status = PBStatuses.idOf(battler.status);                    // :973
            if (status != PBStatuses.SLEEP && status != PBStatuses.DROWSY) {
                return false;
            }
            String itemName = itemName(battle, item);                        // :974
            // 登记: :975 PBDebug.log("[Item triggered] …") —— PBDebug 未建模
            if (!forced) {                                                   // :976
                battle.commonAnimation("EatBerry", battler);
            }
            battler.pbCureStatus(forced);                                    // :977
            if (!forced) {                                                   // :978
                battle.display(battler.pbThis() + "的" + itemName + "使它醒来了！");
            }
            return true;                                                     // :979
        });

        BattleHandlers.StatusCureItem.add("LUMBERRY", (item, battler, battle, forced) -> {
            // Arceus:983-1021
            if (!forced && battler.isUnnerved()) {                           // :985
                return false;
            }
            if (PBStatuses.idOf(battler.status) == PBStatuses.NONE            // :987-988
                    && battler.effects.intVal(PBEffects.Battler.Confusion) == 0) {
                return false;
            }
            String itemName = itemName(battle, item);                        // :989
            // 登记: :990 PBDebug.log("[Item triggered] …") —— PBDebug 未建模
            if (!forced) {                                                   // :991
                battle.commonAnimation("EatBerry", battler);
            }
            int oldStatus = PBStatuses.idOf(battler.status);                 // :992
            boolean oldConfusion =
                    battler.effects.intVal(PBEffects.Battler.Confusion) > 0;  // :993
            battler.pbCureStatus(forced);                                    // :994
            battler.pbCureConfusion();                                       // :995
            if (forced) {                                                    // :996
                if (oldConfusion) {                                          // :997
                    battle.display(battler.pbThis() + "解除了混乱！");
                }
            } else {                                                         // :998
                switch (oldStatus) {                                         // :999
                    case PBStatuses.SLEEP:                                   // :1000
                        battle.display(battler.pbThis() + "的" + itemName + "使它醒来了！");   // :1001
                        break;
                    case PBStatuses.POISON:                                  // :1002
                        battle.display(battler.pbThis() + "的" + itemName + "消去了毒！");     // :1003
                        break;
                    case PBStatuses.BURN:                                    // :1004
                        battle.display(battler.pbThis() + "的" + itemName + "治愈了灼伤！");   // :1005
                        break;
                    case PBStatuses.PARALYSIS:                               // :1006
                        battle.display(battler.pbThis() + "的" + itemName + "治愈了麻痹！");   // :1007
                        break;
                    case PBStatuses.FROZEN:                                  // :1008
                        battle.display(itemName + "解冻了" + battler.pbThis() + "！");         // :1009
                        break;
                    case PBStatuses.FROSTBITE:                               // :1010
                        battle.display(battler.pbThis() + "的" + itemName + "治好了伙伴的冻伤！");   // :1011
                        break;
                    case PBStatuses.DROWSY:                                  // :1012
                        battle.display(battler.pbThis() + "的" + itemName + "唤醒了昏昏欲睡的伙伴！");   // :1013
                        break;
                    default:
                        break;
                }
                if (oldConfusion) {                                          // :1015-1016
                    battle.display(battler.pbThis() + "的" + itemName + "解除了混乱！");
                }
            }
            return true;                                                     // :1019
        });

        // ==================================================================
        // 场地:597-751 - 6 brand new handlers
        // ==================================================================

        BattleHandlers.TerrainStatBoostItem.add("BUGLURESEED", (item, battler, battle) -> {
            // 场地:597-611 (虫蚀种子：虫惑场地中攻击提高1级)
            if (battle.field.terrain != PBBattleTerrains.BugLure) {          // :599 PBBattleTerrains::BugLure
                return false;
            }
            if (!battler.pbCanRaiseStatStage(PBStats.ATTACK, battler)) {     // :600-602
                return false;
            }
            String itemName = itemName(battle, item);                        // :604
            battle.commonAnimation("UseItem", battler);                      // :605
            return battler.pbRaiseStatStageByCause(                          // :607-609
                    PBStats.ATTACK, 1, battler, itemName);
        });

        BattleHandlers.TerrainStatBoostItem.add("COLDSEED", (item, battler, battle) -> {
            // 场地:614-628 (结冻种子：冰冷场地中防御提高1级)
            if (battle.field.terrain != PBBattleTerrains.Cold) {             // :616 PBBattleTerrains::Cold
                return false;
            }
            if (!battler.pbCanRaiseStatStage(PBStats.DEFENSE, battler)) {    // :617-619
                return false;
            }
            String itemName = itemName(battle, item);                        // :621
            battle.commonAnimation("UseItem", battler);                      // :622
            return battler.pbRaiseStatStageByCause(                          // :624-626
                    PBStats.DEFENSE, 1, battler, itemName);
        });

        BattleHandlers.DamageCalcTargetAbility.add("FROZENARMOR", (ability, user, target, move, mults, baseDmg, type) -> {
            // 场地:707-714 (结冻盔甲：冰冷场地中物理防御提高 50%)
            if (!move.physical()) {                                          // :709 move.physicalMove?
                return;
            }
            if (target.battle.field.terrain != PBBattleTerrains.Cold) {      // :710
                return;
            }
            mults[BattleHandlers.DEF_MULT] *= 1.5f;                          // :712
        });

        BattleHandlers.PriorityChangeAbility.add("TRAPTRICK", (ability, battler, move, pri) -> {
            // 场地:720-728 (陷阱诡计：虫惑场地中变化招式优先度 +1)
            if (!move.statusMove()) {                                        // :722
                return null;
            }
            if (battler.battle.field.terrain != PBBattleTerrains.BugLure) {  // :723
                return null;
            }
            if (!battler.affectedByTerrain()) {                              // :724
                return null;
            }
            return pri + 1;                                                  // :726
        });

        BattleHandlers.AbilityOnSwitchIn.add("BUGLURESURGE", (ability, battler, battle) -> {
            // 场地:735-745 (虫惑制造者：出场时生成虫惑场地)
            if (battle.field.terrain == PBBattleTerrains.BugLure) {          // :737
                return;
            }
            battle.showAbilitySplash(battler);                               // :739
            // :740-742 pbStartTerrain 会自动关闭特性提示框 (:743)
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.BugLure);
        });

        BattleHandlers.AbilityOnSwitchIn.add("COLDSURGE", (ability, battler, battle) -> {
            // 场地:751-759 (冰冷制造者：出场时生成冰冷场地)
            if (battle.field.terrain == PBBattleTerrains.Cold) {             // :753
                return;
            }
            battle.showAbilitySplash(battler);                               // :755
            PendingApi.pbStartTerrain(battle, battler, PBBattleTerrains.Cold);   // :756-758
        });
    }

    /**
     * {@code PBItems.getName(item)} (Compiler_PBS:446-448 generates it as a
     * message-table lookup): the item's display name. The runtime's item table is
     * {@code PbsData.Items}, and a missing entry reads {@code ""} - which is what
     * {@code pbGetMessage} returns for an unknown id ({@code Intl_Messages:555-561}).
     */
    private static String itemName(Battle battle, String item) {
        PbsData pbs = battle == null ? null : battle.pbs();
        PbsData.Item entry = pbs == null ? null : pbs.item(item);
        return entry == null || entry.name == null ? "" : entry.name;
    }
}
