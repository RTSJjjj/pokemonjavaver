package pokemon.runtime.battle;

import com.badlogic.gdx.utils.IntArray;

import pokemon.runtime.pokemon.Pokemon;

/**
 * Stage 4 / L1': the {@code BattleHandlers_Items.rb} registrations assigned to
 * this file by {@code stage4-l1-handler-roster.md} §1.2 - <b>38 entries</b>, span
 * subtotal 509: the 16 {@code HPHealItem} berries (:48-225), the 8
 * {@code StatusCureItem} berries (:226-382), {@code LEPPABERRY} /
 * {@code WHITEHERB} (:1377-1436), the experience and EV items (:1437-1492),
 * {@code ADRENALINEORB} (:1678-1690), {@code EJECTPACK} (:1702-1718),
 * {@code CLEARAMULET} (:1771-1779) and {@code MIRRORHERB} (:1780-1798). No
 * {@code copy} line belongs to these groups.
 *
 * <h2>Mapping notes</h2>
 * <ul>
 * <li>{@code PBItems.getName(item)} &rarr; {@code PendingApi.PBItems_getName}
 *     (items are internal-name Strings in this runtime; {@code HandlerHash}'s
 *     documented deviation).</li>
 * <li>{@code battler.totalhp} &rarr; {@link Battler#maxHp()} (the battle-time
 *     max HP the Pokemon mirrors).</li>
 * <li>{@code battler.status!=PBStatuses::X} &rarr; {@code !battler.hasStatus("X")}
 *     because this runtime stores the status as its internal name
 *     (PBStatuses.java's class comment); the integer constants are still used
 *     where the plugin's number is the value under test.</li>
 * <li>{@code battler.stages[s]} / {@code = 0} &rarr; {@link Battler#stage(int)} /
 *     {@link Battler#setStage(int,int)} - the accessors added for the
 *     {@code PBStats} numbering mismatch (task-12 §B).</li>
 * <li>{@code PBDebug.log(...)} is a presentation-only call (roster §1.1 rule 2
 *     "降级"): an empty body plus a {@code 登记} comment, never a fake log.</li>
 * <li>{@code battle.pbCommonAnimation} / {@code battle.pbDisplay} use the Battle
 *     methods added by task-13 ({@code commonAnimation} is itself a registered
 *     empty implementation: the animation player is not modelled).</li>
 * </ul>
 *
 * <h2>Registered here but NOT wired</h2>
 * <ul>
 * <li>{@code SITRUSBERRY} :186 - the plugin calls the misspelled
 *     {@code battler.pbRecoverHP?(...)} (roster §4); transcribed as a no-op as
 *     instructed, no method invented.</li>
 * <li>{@code LEPPABERRY} :1395 - {@code battler.moves[choice].pp = ...} has no
 *     target: this runtime keeps no separate battle-time PP mirror (see the
 *     comment there).</li>
 * <li>{@code EJECTPACK} - roster §7 classifies it 登记 (the switching
 *     subsystem); the body is transcribed against the Battle methods task-13
 *     landed and carries a {@code 登记} note.</li>
 * </ul>
 */
final class ItemsHealStatus {

    private ItemsHealStatus() {
    }

    /** {@code BattleHandlers_Items.rb}: 38 entries (roster §1.2, §2 main table). */
    static void register() {
        // ==============================================================
        // HPHealItem (BattleHandlers_Items.rb:48-225)
        // ==============================================================

        // :48-54 AGUAVBERRY
        BattleHandlers.HPHealItem.add("AGUAVBERRY", (item, battler, battle, forced) -> {
            // :50-52 next pbBattleConfusionBerry(battler,battle,item,forced,4,_INTL(...))
            return BattleHandlerHelpers.pbBattleConfusionBerry(battler, battle, item, forced, 4,
                    "对于" + battler.pbThis(true) + "来说，" + battle.pbs().item(item).name + "太苦了！");
        });

        // :55-60 APICOTBERRY
        BattleHandlers.HPHealItem.add("APICOTBERRY", (item, battler, battle, forced) -> {
            return BattleHandlerHelpers.pbBattleStatIncreasingBerry(battler, battle, item, forced, PBStats.SPDEF);   // :57
        });

        // :61-77 BERRYJUICE
        BattleHandlers.HPHealItem.add("BERRYJUICE", (item, battler, battle, forced) -> {
            if (!battler.canHeal()) {                                    // :63 next false if !battler.canHeal?
                return false;
            }
            if (!forced && battler.hp > battler.maxHp() / 2) {            // :64 next false if !forced && hp>totalhp/2
                return false;
            }
            String itemName = battle.pbs().item(item).name;          // :65
            if (forced) {                                                // :66 PBDebug.log(...) if forced - 登记: PBDebug（表现层）
            }
            if (!forced) {                                               // :67 battle.pbCommonAnimation("UseItem",battler) if !forced
                battle.commonAnimation("UseItem", battler);
            }
            battler.pbRecoverHP(20);                                     // :68
            if (forced) {                                                // :69
                battle.display(battler.pbThis() + "的HP回复了。");        // :70
            } else {
                battle.display(battler.pbThis() + "使用" + itemName + "回复了HP！");   // :72
            }
            return true;                                                 // :74 next true
        });

        // :78-84 FIGYBERRY
        BattleHandlers.HPHealItem.add("FIGYBERRY", (item, battler, battle, forced) -> {
            // :80-82 next pbBattleConfusionBerry(battler,battle,item,forced,0,_INTL(...))
            return BattleHandlerHelpers.pbBattleConfusionBerry(battler, battle, item, forced, 0,
                    "对于" + battler.pbThis(true) + "来说，" + battle.pbs().item(item).name + "太辣了！");
        });

        // :85-90 GANLONBERRY
        BattleHandlers.HPHealItem.add("GANLONBERRY", (item, battler, battle, forced) -> {
            return BattleHandlerHelpers.pbBattleStatIncreasingBerry(battler, battle, item, forced, PBStats.DEFENSE);   // :87
        });

        // :91-97 IAPAPABERRY
        BattleHandlers.HPHealItem.add("IAPAPABERRY", (item, battler, battle, forced) -> {
            // :93-95 next pbBattleConfusionBerry(battler,battle,item,forced,1,_INTL(...))
            return BattleHandlerHelpers.pbBattleConfusionBerry(battler, battle, item, forced, 1,
                    "对于" + battler.pbThis(true) + "来说，" + battle.pbs().item(item).name + "太酸了！");
        });

        // :98-113 LANSATBERRY
        BattleHandlers.HPHealItem.add("LANSATBERRY", (item, battler, battle, forced) -> {
            // :100 next false if !forced && !battler.pbCanConsumeBerry?(item)
            if (!forced && !battler.pbCanConsumeBerry(item, true)) {
                return false;
            }
            if (battler.effects.intVal(PBEffects.Battler.FocusEnergy) >= 2) {   // :101
                return false;
            }
            if (!forced) {                                               // :102
                battle.commonAnimation("EatBerry", battler);
            }
            battler.effects.set(PBEffects.Battler.FocusEnergy, 2);        // :103
            String itemName = battle.pbs().item(item).name;           // :104
            if (forced) {                                                 // :105
                battle.display(itemName + "使" + battler.pbThis() + "变得兴奋了！");   // :106
            } else {
                battle.display(battler.pbThis() + "因为使用" + itemName + "变得兴奋了！");   // :108
            }
            return true;                                                  // :110 next true
        });

        // :114-119 LIECHIBERRY
        BattleHandlers.HPHealItem.add("LIECHIBERRY", (item, battler, battle, forced) -> {
            return BattleHandlerHelpers.pbBattleStatIncreasingBerry(battler, battle, item, forced, PBStats.ATTACK);   // :116
        });

        // :120-126 MAGOBERRY
        BattleHandlers.HPHealItem.add("MAGOBERRY", (item, battler, battle, forced) -> {
            // :122-124 next pbBattleConfusionBerry(battler,battle,item,forced,2,_INTL(...))
            return BattleHandlerHelpers.pbBattleConfusionBerry(battler, battle, item, forced, 2,
                    "对于" + battler.pbThis(true) + "来说，" + battle.pbs().item(item).name + "太甜了！");
        });

        // :127-144 MICLEBERRY
        BattleHandlers.HPHealItem.add("MICLEBERRY", (item, battler, battle, forced) -> {
            // :129 next false if !forced && !battler.pbCanConsumeBerry?(item)
            if (!forced && !battler.pbCanConsumeBerry(item, true)) {
                return false;
            }
            if (!battler.effects.truthy(PBEffects.Battler.MicleBerry)) {   // :130 next false if !battler.effects[MicleBerry]
                return false;
            }
            if (!forced) {                                                // :131
                battle.commonAnimation("EatBerry", battler);
            }
            battler.effects.set(PBEffects.Battler.MicleBerry, true);      // :132
            String itemName = battle.pbs().item(item).name;           // :133
            if (forced) {                                                 // :134
                // :135 PBDebug.log("[Item triggered] Forced consuming of #{itemName}") - 登记: PBDebug（表现层）
                battle.display(battler.pbThis() + "提高了下个招式的命中率！");   // :136
            } else {
                battle.display(battler.pbThis() + "使用" + itemName + "提高了下一招的命中率！");   // :138-139
            }
            return true;                                                  // :141 next true
        });

        // :145-166 ORANBERRY
        BattleHandlers.HPHealItem.add("ORANBERRY", (item, battler, battle, forced) -> {
            if (!battler.canHeal()) {                                     // :147
                return false;
            }
            if (!forced && battler.isUnnerved()) {                        // :148
                return false;
            }
            if (!forced && battler.hp > battler.maxHp() / 2) {            // :149
                return false;
            }
            if (!forced) {                                                // :150
                battle.commonAnimation("EatBerry", battler);
            }
            if (battler.hasActiveAbility("RIPEN")) {                      // :151
                battler.pbRecoverHP(20);                                  // :152
            } else {
                battler.pbRecoverHP(10);                                  // :154
            }
            String itemName = battle.pbs().item(item).name;           // :156
            if (forced) {                                                 // :157
                // :158 PBDebug.log(...) - 登记: PBDebug（表现层）
                battle.display(battler.pbThis() + "的HP回复了。");         // :159
            } else {
                battle.display(battler.pbThis() + "使用" + itemName + "回复了HP！");   // :161
            }
            return true;                                                  // :163 next true
        });

        // :167-172 PETAYABERRY
        BattleHandlers.HPHealItem.add("PETAYABERRY", (item, battler, battle, forced) -> {
            return BattleHandlerHelpers.pbBattleStatIncreasingBerry(battler, battle, item, forced, PBStats.SPATK);   // :169
        });

        // :173-178 SALACBERRY
        BattleHandlers.HPHealItem.add("SALACBERRY", (item, battler, battle, forced) -> {
            return BattleHandlerHelpers.pbBattleStatIncreasingBerry(battler, battle, item, forced, PBStats.SPEED);   // :175
        });

        // :179-200 SITRUSBERRY
        BattleHandlers.HPHealItem.add("SITRUSBERRY", (item, battler, battle, forced) -> {
            if (!battler.canHeal()) {                                     // :181
                return false;
            }
            if (!forced && battler.isUnnerved()) {                        // :182
                return false;
            }
            if (!forced && battler.hp > battler.maxHp() / 2) {            // :183
                return false;
            }
            if (!forced) {                                                // :184
                battle.commonAnimation("EatBerry", battler);
            }
            if (battler.hasActiveAbility("RIPEN")) {                      // :185
                // 登记: :186 battler.pbRecoverHP?(battler.totalhp/2) - roster §4 lists
                //       this as a plugin defect: the method name is misspelled (no
                //       pbRecoverHP? exists anywhere), so the Ruby raises
                //       NoMethodError here. Transcribed as a no-op as instructed
                //       ("照抄调用形状，不要补方法"); no pbRecoverHP? was added.
            } else {
                battler.pbRecoverHP(battler.maxHp() / 4);                 // :188
            }
            String itemName = battle.pbs().item(item).name;           // :190
            if (forced) {                                                 // :191
                // :192 PBDebug.log(...) - 登记: PBDebug（表现层）
                battle.display(battler.pbThis() + "的HP回复了。");         // :193
            } else {
                battle.display(battler.pbThis() + "使用" + itemName + "回复了HP！");   // :195
            }
            return true;                                                  // :197 next true
        });

        // :201-214 STARFBERRY
        BattleHandlers.HPHealItem.add("STARFBERRY", (item, battler, battle, forced) -> {
            IntArray stats = new IntArray();                              // :203 stats = []
            for (int s : PBStats.EACH_MAIN_BATTLE_STAT) {                  // :204 PBStats.eachMainBattleStat
                if (battler.pbCanRaiseStatStage(s, battler)) {
                    stats.add(s);
                }
            }
            if (stats.size == 0) {                                         // :205 next false if stats.length==0
                return false;
            }
            int stat = stats.get(battle.pbRandom(stats.size));             // :206 stat = stats[battle.pbRandom(stats.length)]
            if (battler.hasActiveAbility("RIPEN")) {                       // :207
                return BattleHandlerHelpers.pbBattleStatIncreasingBerry(battler, battle, item, forced, stat, 4);   // :208
            }
            return BattleHandlerHelpers.pbBattleStatIncreasingBerry(battler, battle, item, forced, stat, 2);       // :210
        });

        // :215-225 WIKIBERRY
        BattleHandlers.HPHealItem.add("WIKIBERRY", (item, battler, battle, forced) -> {
            // :217-219 next pbBattleConfusionBerry(battler,battle,item,forced,3,_INTL(...))
            return BattleHandlerHelpers.pbBattleConfusionBerry(battler, battle, item, forced, 3,
                    "对于" + battler.pbThis(true) + "来说，" + battle.pbs().item(item).name + "太涩了！");
        });

        // ==============================================================
        // StatusCureItem (BattleHandlers_Items.rb:226-382)
        // ==============================================================

        // :226-238 ASPEARBERRY
        BattleHandlers.StatusCureItem.add("ASPEARBERRY", (item, battler, battle, forced) -> {
            if (!forced && battler.isUnnerved()) {                        // :228
                return false;
            }
            if (!battler.hasStatus("FROZEN")) {                           // :229 next false if battler.status!=PBStatuses::FROZEN
                return false;
            }
            String itemName = battle.pbs().item(item).name;           // :230
            if (forced) {                                                 // :231 PBDebug.log(...) if forced - 登记: PBDebug（表现层）
            }
            if (!forced) {                                                // :232
                battle.commonAnimation("EatBerry", battler);
            }
            battler.pbCureStatus(forced);                                 // :233
            if (!forced) {                                                // :234
                battle.display(itemName + "解冻了" + battler.pbThis() + "！");
            }
            return true;                                                  // :235 next true
        });

        // :239-251 CHERIBERRY
        BattleHandlers.StatusCureItem.add("CHERIBERRY", (item, battler, battle, forced) -> {
            if (!forced && battler.isUnnerved()) {                        // :241
                return false;
            }
            if (!battler.hasStatus("PARALYSIS")) {                        // :242
                return false;
            }
            String itemName = battle.pbs().item(item).name;           // :243
            if (forced) {                                                 // :244 PBDebug.log(...) - 登记
            }
            if (!forced) {                                                // :245
                battle.commonAnimation("EatBerry", battler);
            }
            battler.pbCureStatus(forced);                                 // :246
            if (!forced) {                                                // :247
                battle.display(battler.pbThis() + "的" + itemName + "治愈了麻痹！");
            }
            return true;                                                  // :248 next true
        });

        // :252-264 CHESTOBERRY
        BattleHandlers.StatusCureItem.add("CHESTOBERRY", (item, battler, battle, forced) -> {
            if (!forced && battler.isUnnerved()) {                        // :254
                return false;
            }
            if (!battler.hasStatus("SLEEP")) {                            // :255
                return false;
            }
            String itemName = battle.pbs().item(item).name;           // :256
            if (forced) {                                                 // :257 PBDebug.log(...) - 登记
            }
            if (!forced) {                                                // :258
                battle.commonAnimation("EatBerry", battler);
            }
            battler.pbCureStatus(forced);                                 // :259
            if (!forced) {                                                // :260
                battle.display(battler.pbThis() + "的" + itemName + "使它醒来了！");
            }
            return true;                                                  // :261 next true
        });

        // :265-299 LUMBERRY
        BattleHandlers.StatusCureItem.add("LUMBERRY", (item, battler, battle, forced) -> {
            if (!forced && battler.isUnnerved()) {                        // :267
                return false;
            }
            // :268-269 next false if battler.status==PBStatuses::NONE && battler.effects[Confusion]==0
            if (!battler.statused() && battler.effects.intVal(PBEffects.Battler.Confusion) == 0) {
                return false;
            }
            String itemName = battle.pbs().item(item).name;           // :270
            if (forced) {                                                 // :271 PBDebug.log(...) - 登记
            }
            if (!forced) {                                                // :272
                battle.commonAnimation("EatBerry", battler);
            }
            String oldStatus = battler.status;                            // :273 oldStatus = battler.status
            boolean oldConfusion = battler.effects.intVal(PBEffects.Battler.Confusion) > 0;   // :274
            battler.pbCureStatus(forced);                                 // :275
            battler.pbCureConfusion();                                    // :276
            if (forced) {                                                 // :277
                if (oldConfusion) {                                       // :278
                    battle.display(battler.pbThis() + "解除了混乱！");
                }
            } else {
                switch (PBStatuses.idOf(oldStatus)) {                     // :280 case oldStatus
                    case PBStatuses.SLEEP:                                // :281
                        battle.display(battler.pbThis() + "的" + itemName + "使它醒来了！");   // :282
                        break;
                    case PBStatuses.POISON:                               // :283
                        battle.display(battler.pbThis() + "的" + itemName + "消去了毒！");     // :284
                        break;
                    case PBStatuses.BURN:                                 // :285
                        battle.display(battler.pbThis() + "的" + itemName + "治愈了灼伤！");   // :286
                        break;
                    case PBStatuses.PARALYSIS:                            // :287
                        battle.display(battler.pbThis() + "的" + itemName + "治愈了麻痹！");   // :288
                        break;
                    case PBStatuses.FROZEN:                               // :289
                        battle.display(itemName + "解冻了" + battler.pbThis() + "！");          // :290
                        break;
                    default:                                              // :291 end
                        break;
                }
                if (oldConfusion) {                                       // :292
                    battle.display(battler.pbThis() + "的" + itemName + "解除了混乱！");       // :293
                }
            }
            return true;                                                  // :296 next true
        });

        // :300-334 MENTALHERB
        BattleHandlers.StatusCureItem.add("MENTALHERB", (item, battler, battle, forced) -> {
            // :302-307 next false if Attract==-1 && Taunt==0 && Encore==0 && !Torment
            //            && Disable==0 && HealBlock==0
            if (battler.effects.intVal(PBEffects.Battler.Attract) == -1
                    && battler.effects.intVal(PBEffects.Battler.Taunt) == 0
                    && battler.effects.intVal(PBEffects.Battler.Encore) == 0
                    && !battler.effects.truthy(PBEffects.Battler.Torment)
                    && battler.effects.intVal(PBEffects.Battler.Disable) == 0
                    && battler.effects.intVal(PBEffects.Battler.HealBlock) == 0) {
                return false;
            }
            String itemName = battle.pbs().item(item).name;            // :308
            // :309 PBDebug.log("[Item triggered] ...") - 登记: PBDebug（表现层）
            if (!forced) {                                                 // :310
                battle.commonAnimation("UseItem", battler);
            }
            if (battler.effects.intVal(PBEffects.Battler.Attract) >= 0) {   // :311
                if (forced) {                                              // :312
                    battle.display(battler.pbThis() + "不再迷恋对方了！");   // :313
                } else {
                    battle.display(battler.pbThis() + "使用了" + itemName + "解除了着迷！");   // :315-316
                }
                battler.pbCureAttract();                                   // :318
            }
            if (battler.effects.intVal(PBEffects.Battler.Taunt) > 0) {      // :320
                battle.display(battler.pbThis() + "的挑衅无效了！");
            }
            battler.effects.set(PBEffects.Battler.Taunt, 0);               // :321
            if (battler.effects.intVal(PBEffects.Battler.Encore) > 0) {     // :322
                battle.display(battler.pbThis() + "的再来一次状态解除了！");
            }
            battler.effects.set(PBEffects.Battler.Encore, 0);              // :323
            battler.effects.set(PBEffects.Battler.EncoreMove, 0);          // :324
            if (battler.effects.truthy(PBEffects.Battler.Torment)) {        // :325
                battle.display(battler.pbThis() + "不再受无理取闹的影响了！");
            }
            battler.effects.set(PBEffects.Battler.Torment, false);         // :326
            if (battler.effects.intVal(PBEffects.Battler.Disable) > 0) {    // :327
                battle.display(battler.pbThis() + "不再被封印了！");
            }
            battler.effects.set(PBEffects.Battler.Disable, 0);             // :328
            if (battler.effects.intVal(PBEffects.Battler.HealBlock) > 0) {  // :329
                battle.display(battler.pbThis() + "的回复封印解除了！");
            }
            battler.effects.set(PBEffects.Battler.HealBlock, 0);           // :330
            return true;                                                   // :331 next true
        });

        // :335-347 PECHABERRY
        BattleHandlers.StatusCureItem.add("PECHABERRY", (item, battler, battle, forced) -> {
            if (!forced && battler.isUnnerved()) {                         // :337
                return false;
            }
            if (!battler.hasStatus("POISON")) {                            // :338
                return false;
            }
            String itemName = battle.pbs().item(item).name;            // :339
            if (forced) {                                                  // :340 PBDebug.log(...) - 登记
            }
            if (!forced) {                                                 // :341
                battle.commonAnimation("EatBerry", battler);
            }
            battler.pbCureStatus(forced);                                  // :342
            if (!forced) {                                                 // :343
                battle.display(battler.pbThis() + "的" + itemName + "消去了毒！");
            }
            return true;                                                   // :344 next true
        });

        // :348-365 PERSIMBERRY
        BattleHandlers.StatusCureItem.add("PERSIMBERRY", (item, battler, battle, forced) -> {
            if (!forced && battler.isUnnerved()) {                         // :350
                return false;
            }
            if (battler.effects.intVal(PBEffects.Battler.Confusion) == 0) {   // :351
                return false;
            }
            String itemName = battle.pbs().item(item).name;            // :352
            if (forced) {                                                  // :353 PBDebug.log(...) - 登记
            }
            if (!forced) {                                                 // :354
                battle.commonAnimation("EatBerry", battler);
            }
            battler.pbCureConfusion();                                     // :355
            if (forced) {                                                  // :356
                battle.display(battler.pbThis() + "解除了混乱！");           // :357
            } else {
                battle.display(battler.pbThis() + "的" + itemName + "解除了混乱！");   // :359-360
            }
            return true;                                                   // :362 next true
        });

        // :366-377 RAWSTBERRY
        BattleHandlers.StatusCureItem.add("RAWSTBERRY", (item, battler, battle, forced) -> {
            if (!forced && battler.isUnnerved()) {                         // :368
                return false;
            }
            if (!battler.hasStatus("BURN")) {                              // :369
                return false;
            }
            String itemName = battle.pbs().item(item).name;            // :370
            if (forced) {                                                  // :371 PBDebug.log(...) - 登记
            }
            if (!forced) {                                                 // :372
                battle.commonAnimation("EatBerry", battler);
            }
            battler.pbCureStatus(forced);                                  // :373
            if (!forced) {                                                 // :374
                battle.display(battler.pbThis() + "的" + itemName + "治愈了灼伤！");
            }
            return true;                                                   // :375 next true
        });

        // ==============================================================
        // EndOfMoveItem / EndOfMoveStatRestoreItem (BattleHandlers_Items.rb:1377-1436)
        // ==============================================================

        // :1377-1404 LEPPABERRY
        BattleHandlers.EndOfMoveItem.add("LEPPABERRY", (item, battler, battle, forced) -> {
            if (!forced && battler.isUnnerved()) {                          // :1379
                return false;
            }
            IntArray found = new IntArray();                                // :1380 found = []
            for (int i = 0; i < battler.pokemon.moves.size; i++) {           // :1381 battler.pokemon.moves.each_with_index
                Pokemon.MoveSlot m = battler.pokemon.moves.get(i);
                if (m == null || m.move == null || m.move.id == 0) {          // :1382 next if !m || m.id==0
                    continue;
                }
                if (m.maxPp <= 0 || m.pp == m.maxPp) {                       // :1383 next if m.totalpp<=0 || m.pp==m.totalpp
                    continue;
                }
                if (!forced && m.pp > 0) {                                   // :1384 next if !forced && m.pp>0
                    continue;
                }
                found.add(i);                                                // :1385 found.push(i)
            }
            if (found.size == 0) {                                           // :1387 next false if found.length==0
                return false;
            }
            String itemName = battle.pbs().item(item).name;              // :1388
            if (forced) {                                                    // :1389 PBDebug.log(...) - 登记
            }
            if (!forced) {                                                   // :1390
                battle.commonAnimation("EatBerry", battler);
            }
            int choice = found.get(battle.pbRandom(found.size));              // :1391 choice = found[battle.pbRandom(found.length)]
            Pokemon.MoveSlot pkmnMove = battler.pokemon.moves.get(choice);    // :1392
            pkmnMove.pp += 10;                                               // :1393
            if (pkmnMove.pp > pkmnMove.maxPp) {                               // :1394
                pkmnMove.pp = pkmnMove.maxPp;
            }
            // :1395 battler.moves[choice].pp = pkmnMove.pp
            // 登记: Battler#moves (the battle-time PokeBattle_Move mirrors) does not
            //       exist in this runtime - Battler#moveSlotPp reads the Pokemon's
            //       MoveSlot directly, which is the same slot updated at :1393-1394,
            //       so the mirror write has no destination and none was invented.
            String moveName = pkmnMove.move.name;                            // :1396 PBMoves.getName(pkmnMove.id) = the move data's name
            if (forced) {                                                    // :1397
                battle.display(battler.pbThis() + "恢复了" + moveName + "的PP！");   // :1398
            } else {
                battle.display(battler.pbThis() + "的" + itemName + "回复了" + moveName + "的PP！");   // :1400
            }
            return true;                                                     // :1402 next true
        });

        // :1410-1431 WHITEHERB
        BattleHandlers.EndOfMoveStatRestoreItem.add("WHITEHERB", (item, battler, battle, forced) -> {
            boolean reducedStats = false;                                    // :1412
            for (int s : PBStats.EACH_BATTLE_STAT) {                          // :1413 PBStats.eachBattleStat do |s|
                if (battler.stage(s) >= 0) {                                  // :1414 next if battler.stages[s]>=0
                    continue;
                }
                battler.setStage(s, 0);                                       // :1415 battler.stages[s] = 0
                battler.statsRaisedThisRound = true;                          // :1416
                reducedStats = true;                                          // :1417
            }
            if (!reducedStats) {                                              // :1419 next false if !reducedStats
                return false;
            }
            String itemName = battle.pbs().item(item).name;               // :1420
            if (forced) {                                                     // :1421 PBDebug.log(...) - 登记
            }
            if (!forced) {                                                    // :1422
                battle.commonAnimation("UseItem", battler);
            }
            if (forced) {                                                     // :1423
                battle.display(battler.pbThis() + "的状态恢复正常了！");        // :1424
            } else {
                battle.display(battler.pbThis() + "使用" + itemName + "将状态恢复正常了！");   // :1426-1427
            }
            return true;                                                      // :1429 next true
        });

        // ==============================================================
        // ExpGainModifierItem / EVGainModifierItem (BattleHandlers_Items.rb:1437-1492)
        // ==============================================================

        // :1437-1441 LUCKYEGG
        BattleHandlers.ExpGainModifierItem.add("LUCKYEGG", (item, battler, exp) -> {
            return exp * 3 / 2;                                              // :1439 next exp*3/2
        });

        // :1447-1451 MACHOBRACE
        BattleHandlers.EVGainModifierItem.add("MACHOBRACE", (item, battler, evYield) -> {
            for (int i = 0; i < evYield.length; i++) {                        // :1449 evYield.collect! { |a| a*2 }
                evYield[i] *= 2;
            }
        });

        // :1453-1457 POWERANKLET
        BattleHandlers.EVGainModifierItem.add("POWERANKLET", (item, battler, evYield) -> {
            evYield[PBStats.SPEED] += 9;                                      // :1455
        });

        // :1459-1463 POWERBAND
        BattleHandlers.EVGainModifierItem.add("POWERBAND", (item, battler, evYield) -> {
            evYield[PBStats.SPDEF] += 9;                                      // :1461
        });

        // :1465-1469 POWERBELT
        BattleHandlers.EVGainModifierItem.add("POWERBELT", (item, battler, evYield) -> {
            evYield[PBStats.DEFENSE] += 9;                                    // :1467
        });

        // :1471-1475 POWERBRACER
        BattleHandlers.EVGainModifierItem.add("POWERBRACER", (item, battler, evYield) -> {
            evYield[PBStats.ATTACK] += 9;                                     // :1473
        });

        // :1477-1481 POWERLENS
        BattleHandlers.EVGainModifierItem.add("POWERLENS", (item, battler, evYield) -> {
            evYield[PBStats.SPATK] += 9;                                      // :1479
        });

        // :1483-1487 POWERWEIGHT
        BattleHandlers.EVGainModifierItem.add("POWERWEIGHT", (item, battler, evYield) -> {
            evYield[PBStats.HP] += 18;                                        // :1485
        });

        // ==============================================================
        // ItemOnIntimidated / ItemOnStatLoss / StatLossImmunityItem /
        // ItemOnOpposingStatGain (BattleHandlers_Items.rb:1678-1798)
        // ==============================================================

        // :1678-1690 ADRENALINEORB
        BattleHandlers.ItemOnIntimidated.add("ADRENALINEORB", (item, battler, battle) -> {
            if (!battler.pbCanRaiseStatStage(PBStats.SPEED, battler)) {        // :1680
                return false;
            }
            String itemName = battle.pbs().item(item).name;                // :1681
            battle.commonAnimation("UseItem", battler);                        // :1682
            return battler.pbRaiseStatStageByCause(PBStats.SPEED, 1, battler, itemName);   // :1683
        });

        // :1702-1718 EJECTPACK
        BattleHandlers.ItemOnStatLoss.add("EJECTPACK", (item, battler, user, move, switched, battle) -> {
            // 登记: roster §7 classifies this entry as 登记 (the switching subsystem).
            //       The body is transcribed against the Battle methods task-13 landed
            //       (pbCanChooseNonActive/pbGetReplacementPokemonIndex/pbRecallAndReplace/
            //       pbClearChoice); the switch itself therefore only runs once the
            //       wiring step accepts handler-driven switching.
            if (battle.pbAllFainted(battler.idxOpposingSide())) {              // :1704
                return;
            }
            if (!battle.pbCanChooseNonActive(battler.index)) {                 // :1705
                return;
            }
            if ("0EE".equals(move.function())) {                               // :1706 U-Turn, Volt-Switch, Flip Turn
                return;
            }
            if ("151".equals(move.function())) {                               // :1707 Parting Shot
                return;
            }
            battle.commonAnimation("UseItem", battler);                        // :1708
            battle.display(battler.pbThis() + "使用" + battler.itemName() + "逃脱了！");   // :1709
            battler.pbConsumeItem(true, false, false);                         // :1710 pbConsumeItem(true,false)
            int newPkmn = battle.pbGetReplacementPokemonIndex(battler.index);   // :1711 Owner chooses
            if (newPkmn < 0) {                                                 // :1712 next if newPkmn<0
                return;
            }
            battle.pbRecallAndReplace(battler.index, newPkmn);                 // :1713
            battle.pbClearChoice(battler.index);                               // :1714
            switched.add(battler.index);                                       // :1715 switched.push(battler.index)
        });

        // :1771-1779 CLEARAMULET
        BattleHandlers.StatLossImmunityItem.add("CLEARAMULET",
                (item, battler, stat, battle, showMessages) -> {
            if (showMessages) {                                                // :1773
                battle.display(battler.pbThis() + "的" + battler.itemName() + "防止了能力下降！");   // :1774
            }
            return true;                                                       // :1776 next true
        });

        // :1780-1798 MIRRORHERB
        BattleHandlers.ItemOnOpposingStatGain.add("MIRRORHERB",
                (item, battler, battle, statUps, forced) -> {
            boolean showAnim = true;                                           // :1782
            battler.mirrorHerbUsed = true;                                     // :1783
            for (int[] statUp : statUps) {                                     // :1784 statUps.each do |stat, increment|
                int stat = statUp[0];
                int increment = statUp[1];
                if (!battler.pbCanRaiseStatStage(stat, battler)) {              // :1785
                    continue;
                }
                // Ruby's 4th argument is showAnim; the 5th (ignoreContrary) keeps its
                // default false (Battler_StatStages:47).
                if (battler.pbRaiseStatStage(stat, increment, battler, showAnim, false)) {   // :1786
                    showAnim = false;                                           // :1787
                }
            }
            battler.mirrorHerbUsed = false;                                    // :1789
            if (showAnim) {                                                    // :1790 next false if showAnim
                return false;
            }
            if (!forced) {                                                     // :1791
                battle.commonAnimation("UseItem", battler);
            }
            // :1792 _INTL("{1}使用它的{2}\n复制了对手的能力变化！", battler.pbThis, PBItems.getName(item))
            battle.display(battler.pbThis() + "使用它的" + battle.pbs().item(item).name
                    + "\n复制了对手的能力变化！");
            return true;                                                       // :1794 next true
        });
    }
}
