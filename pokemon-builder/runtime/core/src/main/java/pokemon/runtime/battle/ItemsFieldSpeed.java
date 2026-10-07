package pokemon.runtime.battle;

/**
 * Stage 4 / L1': the {@code BattleHandlers_Items.rb} registrations assigned to
 * this file by {@code stage4-l1-handler-roster.md} §1.2 - <b>28 entries</b>,
 * plus the 7 {@code .copy} lines of roster §5 that land here (§6: 28 + 7 copy
 * targets = 35 registered symbols).
 *
 * <p>Groups (roster §1.3): {@code SpeedCalcItem} 4, {@code PriorityBracketChangeItem} 3,
 * {@code PriorityBracketUseItem} 2, {@code WeatherExtenderItem} 4,
 * {@code TerrainStatBoostItem} 4, {@code EOREffectItem} 3,
 * {@code EORHealingItem} 2, {@code ItemOnSwitchIn} 2, {@code WeightCalcItem} 1,
 * {@code TerrainExtenderItem} 1, {@code CertainSwitchingUserItem} 1,
 * {@code RunFromBattleItem} 1. Roster §7.1: 19 可转, 9 降级, 0 登记.</p>
 *
 * <h2>Translations</h2>
 * <ul>
 * <li>{@code mult} / {@code w} are {@code float}: the plugin writes
 *     {@code mult*1.5} ({@code :7}) and {@code [w/2,1].max} ({@code :40}), so
 *     {@link BattleHandlers.SpeedCalcItem} / {@link BattleHandlers.WeightCalcItem}
 *     return {@code Float} and {@code null} means "no handler / no override"
 *     (the trigger wrappers then keep the incoming value,
 *     {@code BattleHandlers.java:886-907}).</li>
 * <li>Type identity is the internal name and {@code weather} / {@code terrain}
 *     stay numeric ({@link PBWeather}, {@link PBBattleTerrains}) - see
 *     {@link BattleHandlers}' javadoc.</li>
 * <li>{@code battler.totalhp} is this runtime's {@link Battler#maxHp()} (see
 *     {@link BattleHandlerHelpers}' javadoc).</li>
 * <li>{@code battle.pbDisplay(...)} / {@code battle.pbCommonAnimation(...)} are
 *     the public mirrors on {@link Battle}; {@code commonAnimation} records the
 *     {@code ANIMATION} round event the battle screen plays
 *     ({@code Battle.java:2217-2238}), so the call sites are kept.</li>
 * <li>{@code _INTL("{1}...{2}...",a,b)} strings are concatenated at the call
 *     site with the plugin's wording copied verbatim.</li>
 * </ul>
 *
 * <h2>登记 (the 9 降级 entries: presentation only)</h2>
 * <ul>
 * <li>{@code battle.scene.pbDamageAnimation(battler)} ({@code :1620}, STICKYBARB)
 *     is the only presentation call with no engine-side counterpart: the runtime
 *     has no {@code PokeBattle_Scene} and {@code Battle} exposes no "play the
 *     damage animation on this battler" hook, so the line is registered as a
 *     comment and nothing is substituted for it.</li>
 * <li>Every other presentation call of those entries ({@code pbCommonAnimation})
 *     is transcribed; it is a no-op in a headless battle
 *     ({@code Battle#commonAnimation}).</li>
 * <li>{@code PBDebug.log} does not occur in this file.</li>
 * </ul>
 *
 * <p>The load order is fixed by {@link BattleHandlerRegistry} and must not be
 * changed here.</p>
 */
final class ItemsFieldSpeed {

    private ItemsFieldSpeed() {
    }

    /** {@code BattleHandlers_Items.rb}: 28 entries (roster §1.2, §2 main table). */
    static void register() {
        // ==================================================================
        // SpeedCalcItem (:5-37)
        // ==================================================================

        BattleHandlers.SpeedCalcItem.add("CHOICESCARF", (item, battler, mult) -> {
            // BattleHandlers_Items.rb:5-10
            return mult * 1.5f;                                              // :7
        });

        BattleHandlers.SpeedCalcItem.add("MACHOBRACE", (item, battler, mult) -> {
            // BattleHandlers_Items.rb:11-20
            return mult / 2f;                                                // :13
        });
        BattleHandlers.SpeedCalcItem.copy("MACHOBRACE", "POWERANKLET", "POWERBAND", "POWERBELT",
                "POWERBRACER", "POWERLENS", "POWERWEIGHT");                   // :17-19

        BattleHandlers.SpeedCalcItem.add("QUICKPOWDER", (item, battler, mult) -> {
            // BattleHandlers_Items.rb:21-27
            if (battler.isSpecies("DITTO")                                    // :23
                    && !battler.effects.truthy(PBEffects.Battler.Transform)) { // :24
                return mult * 2f;                                            // :23
            }
            return null;                                                     // :23 no `next` -> Ruby nil
        });

        BattleHandlers.SpeedCalcItem.add("IRONBALL", (item, battler, mult) -> {
            // BattleHandlers_Items.rb:28-37
            return mult / 2f;                                                // :30
        });

        // ==================================================================
        // WeightCalcItem (:38-47)
        // ==================================================================

        BattleHandlers.WeightCalcItem.add("FLOATSTONE", (item, battler, w) -> {
            // BattleHandlers_Items.rb:38-47
            return Math.max(w / 2f, 1f);                                     // :40 [w/2,1].max
        });

        // ==================================================================
        // PriorityBracketChangeItem (:383-407)
        // ==================================================================

        BattleHandlers.PriorityBracketChangeItem.add("CUSTAPBERRY", (item, battler, subPri, battle) -> {
            // BattleHandlers_Items.rb:383-389
            if (!battler.pbCanConsumeBerry(item, true)) {                    // :385 (alwaysCheckGluttony=true)
                return null;                                                 // :385
            }
            if (subPri < 1) {                                                // :386
                return 1;                                                    // :386
            }
            return null;                                                     // :386 no `next` -> Ruby nil
        });

        BattleHandlers.PriorityBracketChangeItem.add("LAGGINGTAIL", (item, battler, subPri, battle) -> {
            // BattleHandlers_Items.rb:390-397
            if (subPri == 0) {                                               // :392
                return -1;                                                   // :392
            }
            return null;                                                     // :392 no `next` -> Ruby nil
        });
        BattleHandlers.PriorityBracketChangeItem.copy("LAGGINGTAIL", "FULLINCENSE"); // :396

        BattleHandlers.PriorityBracketChangeItem.add("QUICKCLAW", (item, battler, subPri, battle) -> {
            // BattleHandlers_Items.rb:398-407
            if (subPri < 1 && battle.pbRandom(100) < 20) {                   // :400
                return 1;                                                    // :400
            }
            return null;                                                     // :400 no `next` -> Ruby nil
        });

        // ==================================================================
        // PriorityBracketUseItem (:408-426) - 降级 (scene: pbCommonAnimation)
        // ==================================================================

        BattleHandlers.PriorityBracketUseItem.add("CUSTAPBERRY", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:408-415
            battle.commonAnimation("EatBerry", battler);                     // :410
            battle.display(battler.itemName() + "使" + battler.pbThis()
                    + "可以先使用技能！");                                    // :411
            battler.pbConsumeItem();                                         // :412
        });

        BattleHandlers.PriorityBracketUseItem.add("QUICKCLAW", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:416-426
            battle.commonAnimation("UseItem", battler);                      // :418
            battle.display(battler.itemName() + "使" + battler.pbThis()
                    + "可以先使用技能！");                                    // :419
        });

        // ==================================================================
        // WeatherExtenderItem (:1493-1520)
        // ==================================================================

        BattleHandlers.WeatherExtenderItem.add("DAMPROCK", (item, weather, duration, battler, battle) -> {
            // BattleHandlers_Items.rb:1493-1498
            if (weather == PBWeather.Rain) {                                 // :1495
                return 8;                                                    // :1495
            }
            return null;                                                     // :1495 no `next` -> Ruby nil
        });

        BattleHandlers.WeatherExtenderItem.add("HEATROCK", (item, weather, duration, battler, battle) -> {
            // BattleHandlers_Items.rb:1499-1504
            if (weather == PBWeather.Sun) {                                  // :1501
                return 8;                                                    // :1501
            }
            return null;                                                     // :1501 no `next` -> Ruby nil
        });

        BattleHandlers.WeatherExtenderItem.add("ICYROCK", (item, weather, duration, battler, battle) -> {
            // BattleHandlers_Items.rb:1505-1510
            if (weather == PBWeather.Hail || weather == PBWeather.Snow) {    // :1507
                return 8;                                                    // :1507
            }
            return null;                                                     // :1507 no `next` -> Ruby nil
        });

        BattleHandlers.WeatherExtenderItem.add("SMOOTHROCK", (item, weather, duration, battler, battle) -> {
            // BattleHandlers_Items.rb:1511-1520
            if (weather == PBWeather.Sandstorm) {                            // :1513
                return 8;                                                    // :1513
            }
            return null;                                                     // :1513 no `next` -> Ruby nil
        });

        // ==================================================================
        // TerrainExtenderItem (:1521-1530)
        // ==================================================================

        BattleHandlers.TerrainExtenderItem.add("TERRAINEXTENDER", (item, terrain, duration, battler, battle) -> {
            // BattleHandlers_Items.rb:1521-1530
            return 8;                                                        // :1523
        });

        // ==================================================================
        // TerrainStatBoostItem (:1531-1574) - 降级 (scene: pbCommonAnimation)
        // ==================================================================

        BattleHandlers.TerrainStatBoostItem.add("ELECTRICSEED", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:1531-1540
            if (battle.field.terrain != PBBattleTerrains.Electric) {          // :1533
                return false;                                                // :1533
            }
            if (!battler.pbCanRaiseStatStage(PBStats.DEFENSE, battler)) {     // :1534
                return false;                                                // :1534
            }
            String itemName = battle.pbs().item(item).name;              // :1535
            battle.commonAnimation("UseItem", battler);                      // :1536
            return battler.pbRaiseStatStageByCause(PBStats.DEFENSE, 1, battler, itemName); // :1537
        });

        BattleHandlers.TerrainStatBoostItem.add("GRASSYSEED", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:1541-1550
            if (battle.field.terrain != PBBattleTerrains.Grassy) {            // :1543
                return false;                                                // :1543
            }
            if (!battler.pbCanRaiseStatStage(PBStats.DEFENSE, battler)) {     // :1544
                return false;                                                // :1544
            }
            String itemName = battle.pbs().item(item).name;              // :1545
            battle.commonAnimation("UseItem", battler);                      // :1546
            return battler.pbRaiseStatStageByCause(PBStats.DEFENSE, 1, battler, itemName); // :1547
        });

        BattleHandlers.TerrainStatBoostItem.add("MISTYSEED", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:1551-1560
            if (battle.field.terrain != PBBattleTerrains.Misty) {             // :1553
                return false;                                                // :1553
            }
            if (!battler.pbCanRaiseStatStage(PBStats.SPDEF, battler)) {       // :1554
                return false;                                                // :1554
            }
            String itemName = battle.pbs().item(item).name;              // :1555
            battle.commonAnimation("UseItem", battler);                      // :1556
            return battler.pbRaiseStatStageByCause(PBStats.SPDEF, 1, battler, itemName); // :1557
        });

        BattleHandlers.TerrainStatBoostItem.add("PSYCHICSEED", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:1561-1574
            if (battle.field.terrain != PBBattleTerrains.Psychic) {           // :1563
                return false;                                                // :1563
            }
            if (!battler.pbCanRaiseStatStage(PBStats.SPDEF, battler)) {       // :1564
                return false;                                                // :1564
            }
            String itemName = battle.pbs().item(item).name;              // :1565
            battle.commonAnimation("UseItem", battler);                      // :1566
            return battler.pbRaiseStatStageByCause(PBStats.SPDEF, 1, battler, itemName); // :1567
        });

        // ==================================================================
        // EORHealingItem (:1575-1608) - 降级 (scene: pbCommonAnimation)
        // ==================================================================

        BattleHandlers.EORHealingItem.add("BLACKSLUDGE", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:1575-1594
            if (battler.pbHasType("POISON")) {                               // :1577
                if (!battler.canHeal()) {                                    // :1578
                    return;
                }
                battle.commonAnimation("UseItem", battler);                  // :1579
                battler.pbRecoverHP(battler.maxHp() / 16);                   // :1580
                battle.display(battler.pbThis() + "使用" + battler.itemName()
                        + "回复了HP！");                                      // :1581-1582
            } else if (battler.takesIndirectDamage(false)) {                 // :1583 (showMsg=false)
                int oldHP = battler.hp;                                      // :1584
                battle.commonAnimation("UseItem", battler);                  // :1585
                battler.pbReduceHP(battler.maxHp() / 8);                     // :1586
                battle.display(battler.pbThis() + "被" + battler.itemName()
                        + "伤害了！");                                        // :1587
                battler.pbItemHPHealCheck(0, false);                         // :1588 (forcedItem=0,fling=false)
                battler.pbAbilitiesOnDamageTaken(oldHP, -1);                 // :1589 (newHP=-1)
                if (battler.fainted()) {                                     // :1590
                    battler.pbFaint();
                }
            }
        });

        BattleHandlers.EORHealingItem.add("LEFTOVERS", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:1595-1608
            if (!battler.canHeal()) {                                        // :1597
                return;
            }
            battle.commonAnimation("UseItem", battler);                      // :1598
            battler.pbRecoverHP(battler.maxHp() / 16);                       // :1599
            battle.display(battler.pbThis() + "使用" + battler.itemName()
                    + "回复了HP！");                                          // :1600-1601
        });

        // ==================================================================
        // EOREffectItem (:1609-1640)
        // ==================================================================

        BattleHandlers.EOREffectItem.add("FLAMEORB", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:1609-1615
            if (!battler.pbCanBurn(null, false, null)) {                     // :1611 (user=nil,showMessages=false)
                return;
            }
            battler.pbBurn(null, battler.itemName() + "使" + battler.pbThis()
                    + "被灼伤了！");                                          // :1612
        });

        BattleHandlers.EOREffectItem.add("STICKYBARB", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:1616-1628 - 降级 (scene: pbDamageAnimation)
            if (!battler.takesIndirectDamage(false)) {                       // :1618 (showMsg=false)
                return;
            }
            int oldHP = battler.hp;                                          // :1619
            // :1620 battle.scene.pbDamageAnimation(battler) —— 登记: Scene_Animations:224
            //       依赖 PokeBattle_Scene（未建模），无引擎侧入口，不造替代实现
            battler.pbReduceHP(battler.maxHp() / 8, false, true, true);      // :1621 (anim=false)
            battle.display(battler.pbThis() + "被" + battler.itemName()
                    + "伤害了！");                                            // :1622
            battler.pbItemHPHealCheck(0, false);                             // :1623 (forcedItem=0,fling=false)
            battler.pbAbilitiesOnDamageTaken(oldHP, -1);                     // :1624 (newHP=-1)
            if (battler.fainted()) {                                         // :1625
                battler.pbFaint();
            }
        });

        BattleHandlers.EOREffectItem.add("TOXICORB", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:1629-1640
            if (!battler.pbCanPoison(null, false, null)) {                   // :1631 (user=nil,showMessages=false)
                return;
            }
            battler.pbPoison(null, battler.itemName() + "使" + battler.pbThis()
                    + "中了剧毒！", true);                                     // :1632-1633 (toxic=true)
        });

        // ==================================================================
        // CertainSwitchingUserItem (:1641-1645)
        // ==================================================================

        BattleHandlers.CertainSwitchingUserItem.add("SHEDSHELL", (item, switcher, battle) -> {
            // BattleHandlers_Items.rb:1641-1645
            return true;                                                     // :1643
        });

        // ==================================================================
        // ItemOnSwitchIn (:1658-1677)
        // ==================================================================

        BattleHandlers.ItemOnSwitchIn.add("AIRBALLOON", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:1658-1664
            battle.display(battler.pbThis() + "靠着" + battler.itemName()
                    + "浮在了空中！");                                        // :1660-1661
        });

        BattleHandlers.ItemOnSwitchIn.add("ROOMSERVICE", (item, battler, battle) -> {
            // BattleHandlers_Items.rb:1665-1677
            if (battle.field.effects.intVal(PBEffects.Field.TrickRoom) == 0) { // :1667
                return;
            }
            if (!battler.pbCanLowerStatStage(PBStats.SPEED, battler)) {      // :1668
                return;
            }
            battler.pbLowerStatStageByCause(PBStats.SPEED, 1, battler, battler.itemName()); // :1669
            battler.pbConsumeItem();                                         // :1670
        });

        // ==================================================================
        // RunFromBattleItem (:1691-1695)
        // ==================================================================

        BattleHandlers.RunFromBattleItem.add("SMOKEBALL", (item, battler) -> {
            // BattleHandlers_Items.rb:1691-1695
            return true;                                                     // :1693
        });
    }
}
