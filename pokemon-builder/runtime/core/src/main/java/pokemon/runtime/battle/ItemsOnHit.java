package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;

import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;

/**
 * Stage 4 / L1': the {@code BattleHandlers_Items.rb} registrations assigned to
 * this file by {@code stage4-l1-handler-roster.md} §1.2 - <b>21 entries</b>, no
 * {@code .copy} line lands here (§6: 21 registered symbols).
 *
 * <p>Groups (roster §1.3): {@code TargetItemOnHit} 13,
 * {@code TargetItemOnHitPositiveBerry} 3, {@code UserItemAfterMoveUse} 3,
 * {@code TargetItemAfterMoveUse} 2. Roster §7.1: 6 可转, 12 降级, 3 登记.</p>
 *
 * <h2>Translations</h2>
 * <ul>
 * <li>{@code isConst?(move.calcType,PBTypes,:X)} = {@code "X".equals(move.calcType())}:
 *     a type is identified by its internal name ({@link BattleHandlers}'
 *     javadoc). {@code move.calcType()} is {@code null} while the plugin's
 *     {@code @calcType} is still {@code -1}, and {@code equals} on the constant
 *     is false for {@code null} - the same answer {@code isConst?(-1,...)} gives.</li>
 * <li>Move predicates go through the movefx strategy layer
 *     ({@code MoveEffectRegistry.of(move.function())}), the L1' convention; see
 *     {@link #fx}.</li>
 * <li>{@code PBTypes.superEffective?(target.damageState.typeMod)} uses the
 *     one-argument {@code typeMod} form ({@link PBTypes#superEffective(int)}).</li>
 * <li>{@code battler.totalhp} is this runtime's {@link Battler#maxHp()} and
 *     {@code battler.item>0} is {@code item != null && !item.isEmpty()}
 *     ({@code Pokemon.item} is the internal-name String here; "no item" is
 *     {@code ""}), the conventions already used by
 *     {@code Battler.pbSymbiosis}/{@code pbRemoveItem}.</li>
 * <li>{@code user.takesIndirectDamage?} / {@code user.affectedByContactEffect?}
 *     are called with Ruby's default {@code showMsg=false}.</li>
 * <li>{@code PBDebug.log} lines are kept as comments at their line: a debug log
 *     with no gameplay effect and no runtime counterpart.</li>
 * <li>{@code _INTL} strings are concatenated with the plugin's wording verbatim.</li>
 * </ul>
 *
 * <h2>降级 (presentation)</h2>
 * <p>{@code battle.scene.pbDamageAnimation(user)} occurs four times
 * ({@code :1084}, {@code :1138}, {@code :1155}, {@code :1620}) and is the only
 * presentation call here with no engine-side counterpart: the runtime has no
 * {@code PokeBattle_Scene} and {@code Battle} exposes no "flash this battler"
 * hook, so each line is registered as a comment and nothing is substituted.
 * {@code battle.pbCommonAnimation(...)} is transcribed - it is the public
 * {@link Battle#commonAnimation(String, Battler)} that records the round event
 * the battle screen plays.</p>
 *
 * <h2>登记 (roster §7)</h2>
 * <ul>
 * <li>{@code TargetItemAfterMoveUse/EJECTBUTTON} ({@code :1282-1295}) and
 *     {@code REDCARD} ({@code :1297-1325}) depend on the switching subsystem
 *     (roster §7, 换人): the head of each handler is transcribed against
 *     {@link Battle}'s own 登记-annotated switching methods
 *     ({@code pbGetReplacementPokemonIndex}, {@code pbRecallAndReplace},
 *     {@code pbClearChoice}), and the parts that cannot be written as compiling
 *     Java are registered at their line.</li>
 * <li>{@code TargetItemOnHit/ROCKYHELMET} ({@code :1134-1146}) reads
 *     {@code user.pokemon.battleRank} (roster §7, BOSS); the field exists on
 *     {@link pokemon.runtime.pokemon.Pokemon}, so the branch is transcribed as
 *     the plugin writes it.</li>
 * <li><b>登记 (plugin defects, roster §4 #2/#4):</b> inside {@code REDCARD},
 *     {@code :1309 Battle::Scene::USE_ABILITY_SPLASH} (no {@code Battle::Scene}
 *     namespace exists anywhere in the project - guaranteed {@code NameError})
 *     and {@code :1317 user.dynamax?} (no such method, and no {@code defined?}
 *     guard here, unlike {@code BattleHandlers_Abilities:2973/2980}). Both are
 *     copied as comments at their lines; neither is "fixed", neither is given a
 *     default, and no substitute branch is invented. {@code :1320
 *     pbRecallAndReplace(user.index,newPkmn,true)} additionally has no 3-argument
 *     overload on this runtime's {@link Battle}, so it stays inside the same
 *     registered block.</li>
 * </ul>
 *
 * <p>The load order is fixed by {@link BattleHandlerRegistry} and must not be
 * changed here.</p>
 */
final class ItemsOnHit {

    private ItemsOnHit() {
    }

    /** {@code BattleHandlers_Items.rb}: 21 entries (roster §1.2, §2 main table). */
    static void register() {
        // ==================================================================
        // TargetItemOnHit (:1039-1219)
        // ==================================================================

        BattleHandlers.TargetItemOnHit.add("ABSORBBULB", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1039-1048
            if (!"WATER".equals(move.calcType())) {                          // :1041 isConst?(move.calcType,PBTypes,:WATER)
                return;
            }
            if (!target.pbCanRaiseStatStage(PBStats.SPATK, target)) {        // :1042
                return;
            }
            battle.commonAnimation("UseItem", target);                       // :1043
            target.pbRaiseStatStageByCause(PBStats.SPATK, 1, target, target.itemName()); // :1044
            target.pbHeldItemTriggered(item, 0, false);                      // :1045 (forcedItem=0,fling=false)
        });

        BattleHandlers.TargetItemOnHit.add("AIRBALLOON", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1049-1056
            battle.display(target.pbThis() + "的" + target.itemName()
                    + "爆炸了！");                                            // :1051
            target.pbConsumeItem(false, true, true);                         // :1052 (recoverable=false,symbiosis=true)
            target.pbSymbiosis();                                            // :1053
        });

        BattleHandlers.TargetItemOnHit.add("CELLBATTERY", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1057-1066
            if (!"ELECTRIC".equals(move.calcType())) {                       // :1059
                return;
            }
            if (!target.pbCanRaiseStatStage(PBStats.ATTACK, target)) {       // :1060
                return;
            }
            battle.commonAnimation("UseItem", target);                       // :1061
            target.pbRaiseStatStageByCause(PBStats.ATTACK, 1, target, target.itemName()); // :1062
            target.pbHeldItemTriggered(item, 0, false);                      // :1063
        });

        BattleHandlers.TargetItemOnHit.add("ENIGMABERRY", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1067-1076
            if (target.damageState.substitute || target.damageState.disguise     // :1069
                    || target.damageState.iceface || target.damageState.flameveil) {
                return;                                                      // :1069
            }
            if (!PBTypes.superEffective(target.damageState.typeMod)) {       // :1070
                return;
            }
            if (BattleHandlers.triggerTargetItemOnHitPositiveBerry(item, target, battle, false)) { // :1071
                target.pbHeldItemTriggered(item, 0, false);                  // :1072
            }
        });

        BattleHandlers.TargetItemOnHit.add("JABOCABERRY", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1077-1093 - 降级 (scene: pbDamageAnimation)
            if (battle.pbCheckOpposingAbility("UNNERVE", target.index, false) != null) { // :1079
                return;
            }
            if (battle.pbCheckOpposingAbility("CONFESSIONLIST", target.index, false) != null) { // :1080
                return;
            }
            if (!fx(move).physicalMove(move, null)) {                        // :1081 move.physicalMove?
                return;
            }
            if (!user.takesIndirectDamage(false)) {                          // :1082 (showMsg=false)
                return;
            }
            battle.commonAnimation("EatBerry", target);                      // :1083
            // :1084 battle.scene.pbDamageAnimation(user) —— 登记: Scene_Animations:224
            //       依赖 PokeBattle_Scene（未建模），无引擎侧入口，不造替代实现
            if (target.hasActiveAbility("RIPEN")) {                          // :1085
                user.pbReduceHP(user.maxHp() / 4, false, true, true);        // :1086 (anim=false)
            } else {
                user.pbReduceHP(user.maxHp() / 8, false, true, true);        // :1088
            }
            battle.display(target.pbThis() + "消耗" + target.itemName()
                    + "伤害了" + user.pbThis(true) + "！");                   // :1090-1091
            target.pbHeldItemTriggered(item, 0, false);                      // :1092
        });

        BattleHandlers.TargetItemOnHit.add("KEEBERRY", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1101-1109
            if (!fx(move).physicalMove(move, null)) {                        // :1103 move.physicalMove?
                return;
            }
            if (BattleHandlers.triggerTargetItemOnHitPositiveBerry(item, target, battle, false)) { // :1104
                target.pbHeldItemTriggered(item, 0, false);                  // :1105
            }
        });

        BattleHandlers.TargetItemOnHit.add("LUMINOUSMOSS", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1110-1118
            if (!"WATER".equals(move.calcType())) {                          // :1112
                return;
            }
            if (!target.pbCanRaiseStatStage(PBStats.SPDEF, target)) {        // :1113
                return;
            }
            battle.commonAnimation("UseItem", target);                       // :1114
            target.pbRaiseStatStageByCause(PBStats.SPDEF, 1, target, target.itemName()); // :1115
            target.pbHeldItemTriggered(item, 0, false);                      // :1116
        });

        BattleHandlers.TargetItemOnHit.add("MARANGABERRY", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1125-1132
            if (!fx(move).specialMove(move, null)) {                         // :1127 move.specialMove?
                return;
            }
            if (BattleHandlers.triggerTargetItemOnHitPositiveBerry(item, target, battle, false)) { // :1128
                target.pbHeldItemTriggered(item, 0, false);                  // :1129
            }
        });

        BattleHandlers.TargetItemOnHit.add("ROCKYHELMET", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1134-1146 - 登记: BOSS (battleRank)
            //                                  降级: scene pbDamageAnimation
            if (!fx(move).pbContactMove(move, user)                          // :1136 move.pbContactMove?(user)
                    || !user.affectedByContactEffect(false)) {               // :1136 (showMsg=false)
                return;
            }
            if (!user.takesIndirectDamage(false)) {                          // :1137
                return;
            }
            // :1138 battle.scene.pbDamageAnimation(user) —— 登记: Scene_Animations:224
            //       依赖 PokeBattle_Scene（未建模），无引擎侧入口，不造替代实现
            if (user.pokemon.battleRank > 2) {                               // :1139
                user.pbReduceHP(user.maxHp() / 30, false, true, true);       // :1140
            } else {
                user.pbReduceHP(user.maxHp() / 6, false, true, true);        // :1142
            }
            battle.display(user.pbThis() + "被" + target.itemName() + "伤害了！"); // :1144
        });

        BattleHandlers.TargetItemOnHit.add("ROWAPBERRY", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1148-1165 - 降级 (scene: pbDamageAnimation)
            if (battle.pbCheckOpposingAbility("UNNERVE", target.index, false) != null) { // :1150
                return;
            }
            if (battle.pbCheckOpposingAbility("CONFESSIONLIST", target.index, false) != null) { // :1151
                return;
            }
            if (!fx(move).specialMove(move, null)) {                         // :1152
                return;
            }
            if (!user.takesIndirectDamage(false)) {                          // :1153
                return;
            }
            battle.commonAnimation("EatBerry", target);                      // :1154
            // :1155 battle.scene.pbDamageAnimation(user) —— 登记: Scene_Animations:224
            //       依赖 PokeBattle_Scene（未建模），无引擎侧入口，不造替代实现
            if (target.hasActiveAbility("RIPEN")) {                          // :1156
                user.pbReduceHP(user.maxHp() / 4, false, true, true);        // :1157
            } else {
                user.pbReduceHP(user.maxHp() / 8, false, true, true);        // :1159
            }
            battle.display(target.pbThis() + "消耗" + target.itemName()
                    + "伤害了" + user.pbThis(true) + "！");                   // :1161-1162
            target.pbHeldItemTriggered(item, 0, false);                      // :1163
        });

        BattleHandlers.TargetItemOnHit.add("SNOWBALL", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1167-1175
            if (!"ICE".equals(move.calcType())) {                            // :1169
                return;
            }
            if (!target.pbCanRaiseStatStage(PBStats.ATTACK, target)) {       // :1170
                return;
            }
            battle.commonAnimation("UseItem", target);                       // :1171
            target.pbRaiseStatStageByCause(PBStats.ATTACK, 1, target, target.itemName()); // :1172
            target.pbHeldItemTriggered(item, 0, false);                      // :1173
        });

        BattleHandlers.TargetItemOnHit.add("STICKYBARB", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1177-1193
            if (!fx(move).pbContactMove(move, user)                          // :1179 move.pbContactMove?(user)
                    || !user.affectedByContactEffect(false)) {               // :1179 (showMsg=false)
                return;
            }
            if (user.fainted() || (user.item != null && !user.item.isEmpty())) { // :1180 user.item>0
                return;
            }
            user.item = target.item;                                         // :1181
            target.item = "";                                                // :1182 target.item = 0
            target.effects.set(PBEffects.Battler.Unburden, true);            // :1183
            if (battle.wildBattle() && !user.opposes(0)) {                   // :1184 (!user.opposes? = opposes?(0))
                if ((user.initialItem() == null || user.initialItem().isEmpty()) // :1185 user.initialItem==0
                        && target.initialItem() != null
                        && target.initialItem().equals(user.item)) {         // :1185 target.initialItem==user.item
                    user.setInitialItem(user.item);                          // :1186
                    target.setInitialItem("");                               // :1187 target.setInitialItem(0)
                }
            }
            battle.display(target.pbThis() + "的" + user.itemName()
                    + "转移到了" + user.pbThis(true) + "！");                 // :1190-1191
        });

        BattleHandlers.TargetItemOnHit.add("WEAKNESSPOLICY", (item, user, target, move, battle) -> {
            // BattleHandlers_Items.rb:1195-1212
            if (target.damageState.disguise || target.damageState.iceface
                    || target.damageState.flameveil) {                       // :1197
                return;
            }
            if (!PBTypes.superEffective(target.damageState.typeMod)) {       // :1198
                return;
            }
            if (!target.pbCanRaiseStatStage(PBStats.ATTACK, target)          // :1199
                    && !target.pbCanRaiseStatStage(PBStats.SPATK, target)) { // :1200
                return;
            }
            battle.commonAnimation("UseItem", target);                       // :1201
            boolean showAnim = true;                                         // :1202
            if (target.pbCanRaiseStatStage(PBStats.ATTACK, target)) {        // :1203
                target.pbRaiseStatStageByCause(PBStats.ATTACK, 2, target, target.itemName(), showAnim, false); // :1204
                showAnim = false;                                            // :1205
            }
            if (target.pbCanRaiseStatStage(PBStats.SPATK, target)) {         // :1207
                target.pbRaiseStatStageByCause(PBStats.SPATK, 2, target, target.itemName(), showAnim, false); // :1208
            }
            target.pbHeldItemTriggered(item, 0, false);                      // :1210
        });

        // ==================================================================
        // TargetItemOnHitPositiveBerry (:1220-1281)
        // ==================================================================

        BattleHandlers.TargetItemOnHitPositiveBerry.add("ENIGMABERRY", (item, battler, battle, forced) -> {
            // BattleHandlers_Items.rb:1220-1240
            if (!battler.canHeal()) {                                        // :1222
                return false;                                                // :1222
            }
            if (!forced && battler.isUnnerved()) {                           // :1223
                return false;                                                // :1223
            }
            String itemName = battle.pbs().item(item).name;              // :1224
            // :1225 PBDebug.log("[Item triggered] #{battler.pbThis}'s #{itemName}") if forced - debug log
            if (!forced) {                                                   // :1226
                battle.commonAnimation("EatBerry", battler);
            }
            if (battler.hasActiveAbility("RIPEN")) {                         // :1227
                battler.pbRecoverHP(battler.maxHp() / 2);                    // :1228
            } else {
                battler.pbRecoverHP(battler.maxHp() / 4);                    // :1230
            }
            if (forced) {                                                    // :1232
                battle.display(battler.pbThis() + "的HP回复了。");             // :1233
            } else {
                battle.display(battler.pbThis() + "使用" + itemName
                        + "回复了HP！");                                      // :1235-1236
            }
            return true;                                                     // :1238
        });

        BattleHandlers.TargetItemOnHitPositiveBerry.add("KEEBERRY", (item, battler, battle, forced) -> {
            // BattleHandlers_Items.rb:1242-1258
            int increment = 1;                                               // :1244
            if (!forced && battler.isUnnerved()) {                           // :1245
                return false;                                                // :1245
            }
            if (!battler.pbCanRaiseStatStage(PBStats.DEFENSE, battler)) {    // :1246
                return false;                                                // :1246
            }
            String itemName = battle.pbs().item(item).name;              // :1247
            if (battler.hasActiveAbility("RIPEN")) {                         // :1248
                increment *= 2;                                              // :1249
            }
            if (!forced) {                                                   // :1251
                battle.commonAnimation("EatBerry", battler);                 // :1252
                return battler.pbRaiseStatStageByCause(PBStats.DEFENSE, increment, battler, itemName); // :1253
            }
            // :1255 PBDebug.log("[Item triggered] #{battler.pbThis}'s #{itemName}") - debug log
            return battler.pbRaiseStatStage(PBStats.DEFENSE, increment, battler); // :1256
        });

        BattleHandlers.TargetItemOnHitPositiveBerry.add("MARANGABERRY", (item, battler, battle, forced) -> {
            // BattleHandlers_Items.rb:1260-1275
            int increment = 1;                                               // :1262
            if (!forced && battler.isUnnerved()) {                           // :1263
                return false;                                                // :1263
            }
            if (!battler.pbCanRaiseStatStage(PBStats.SPDEF, battler)) {      // :1264
                return false;                                                // :1264
            }
            String itemName = battle.pbs().item(item).name;              // :1265
            if (battler.hasActiveAbility("RIPEN")) {                         // :1266
                increment *= 2;                                              // :1267
            }
            if (!forced) {                                                   // :1269
                battle.commonAnimation("EatBerry", battler);                 // :1270
                return battler.pbRaiseStatStageByCause(PBStats.SPDEF, increment, battler, itemName); // :1271
            }
            // :1273 PBDebug.log("[Item triggered] #{battler.pbThis}'s #{itemName}") - debug log
            return battler.pbRaiseStatStage(PBStats.SPDEF, increment, battler); // :1274
        });

        // ==================================================================
        // TargetItemAfterMoveUse (:1282-1325) - 登记 (换人)
        // ==================================================================

        BattleHandlers.TargetItemAfterMoveUse.add("EJECTBUTTON", (item, battler, user, move, switched, battle) -> {
            // BattleHandlers_Items.rb:1282-1295
            // 登记: 换人子系统（roster §7）—— 下面每一步都走 Battle 上已 登记 的换人方法
            //       （pbGetReplacementPokemonIndex 的 random=false 分支要开队伍界面、
            //        pbRecallAndReplace 要 @scene.pbRecall/pbShowPartyLineup）。照抄调用形状，
            //        不补界面、不造替代实现。
            if (battle.pbAllFainted(battler.idxOpposingSide())) {            // :1284
                return;
            }
            if (!battle.pbCanChooseNonActive(battler.index)) {               // :1285
                return;
            }
            battle.commonAnimation("UseItem", battler);                      // :1286
            battle.display(battler.pbThis() + "使用" + battler.itemName()
                    + "逃脱了！");                                            // :1287
            battler.pbConsumeItem(true, false, true);                        // :1288 (recoverable=true,symbiosis=false)
            int newPkmn = battle.pbGetReplacementPokemonIndex(battler.index); // :1289 # Owner chooses
            if (newPkmn < 0) {                                               // :1290
                return;
            }
            battle.pbRecallAndReplace(battler.index, newPkmn);               // :1291
            battle.pbClearChoice(battler.index);                             // :1292 # Replacement does nothing this round
            switched.add(battler.index);                                     // :1293
        });

        BattleHandlers.TargetItemAfterMoveUse.add("REDCARD", (item, battler, user, move, switched, battle) -> {
            // BattleHandlers_Items.rb:1297-1325
            // 登记: 换人子系统（roster §7）+ 两处插件缺陷（花名册 §4 #2/#4）。
            if (user.fainted() || switched.contains(user.index, false)) {    // :1299 switched.include?(user.index)
                return;
            }
            int newPkmn = battle.pbGetReplacementPokemonIndex(user.index, true); // :1300 # Random
            if (newPkmn < 0) {                                               // :1301
                return;
            }
            battle.commonAnimation("UseItem", battler);                      // :1302
            battle.display(battler.pbThis() + "举起了它的" + battler.itemName()
                    + "\n来对抗" + user.pbThis(true) + "！");                 // :1303-1304
            battler.pbConsumeItem();                                         // :1305
            if (user.effects.truthy(PBEffects.Battler.Commander)) {          // :1306
                return;
            }
            // :1307-1324 登记: 整块不转译 ——
            //   :1307-1316 `if user.hasActiveAbility?([:SUCTIONCUPS,:FIGHTTODIE,:GUARDDOG]) && !battle.moldBreaker`
            //     分支里的 :1309 `Battle::Scene::USE_ABILITY_SPLASH` 在全工程不存在（只有
            //     PokeBattle_SceneConstants::USE_ABILITY_SPLASH，PokeBattle_SceneConstants:3），
            //     Ruby 走到该行必然 NameError（花名册 §4 #4）；:1308/:1310-1315 的
            //     splash 与文案照抄不出可编译形状，故连同分支整体登记，不按
            //     PokeBattle_SceneConstants 改读、不补默认值。
            //   :1317-1324 `if user.dynamax?` —— Battler 没有 dynamax?，且此处没有
            //     defined? 保护（花名册 §4 #2），照抄必编译不过；其 else 分支的
            //     :1320 `pbRecallAndReplace(user.index,newPkmn,true)` 第 3 实参
            //     （randomReplacement）在本运行时 Battle 上没有对应重载（只有 2 参版本），
            //     故 :1317-1324 一并登记，不近似、不拆分支。
        });

        // ==================================================================
        // UserItemAfterMoveUse (:1332-1370)
        // ==================================================================

        BattleHandlers.UserItemAfterMoveUse.add("LIFEORB", (item, user, targets, move, numHits, battle) -> {
            // BattleHandlers_Items.rb:1332-1347
            if (!user.takesIndirectDamage(false)) {                          // :1334 (showMsg=false)
                return;
            }
            if (!fx(move).pbDamagingMove(move) || numHits == 0) {            // :1335 move.pbDamagingMove?
                return;
            }
            boolean hitBattler = false;                                      // :1336
            for (Battler b : targets) {                                      // :1337
                if (!b.damageState.unaffected && !b.damageState.substitute) {
                    hitBattler = true;                                       // :1338
                }
                if (hitBattler) {
                    break;                                                   // :1339
                }
            }
            if (!hitBattler) {                                               // :1341
                return;
            }
            // :1342 PBDebug.log("[Item triggered] #{user.pbThis}'s #{user.itemName} (recoil)") - debug log
            user.pbReduceHP(user.maxHp() / 10);                              // :1343
            battle.display(user.pbThis() + "失去了HP！");                     // :1344
            user.pbItemHPHealCheck(0, false);                                // :1345 (forcedItem=0,fling=false)
            if (user.fainted()) {                                            // :1346
                user.pbFaint();
            }
        });

        BattleHandlers.UserItemAfterMoveUse.add("SHELLBELL", (item, user, targets, move, numHits, battle) -> {
            // BattleHandlers_Items.rb:1350-1359
            if (!user.canHeal()) {                                           // :1352
                return;
            }
            int totalDamage = 0;                                             // :1353
            for (Battler b : targets) {                                      // :1354
                totalDamage += b.damageState.totalHPLost;
            }
            if (totalDamage <= 0) {                                          // :1355
                return;
            }
            user.pbRecoverHP(totalDamage / 8);                               // :1356
            battle.display(user.pbThis() + "使用" + user.itemName()
                    + "回复了HP！");                                          // :1357-1358
        });

        BattleHandlers.UserItemAfterMoveUse.add("THROATSPRAY", (item, user, targets, move, numHits, battle) -> {
            // BattleHandlers_Items.rb:1362-1370
            if (!fx(move).soundMove(move) || numHits == 0) {                 // :1364 move.soundMove?
                return;
            }
            if (!user.pbCanRaiseStatStage(PBStats.SPATK, user)) {            // :1365
                return;
            }
            battle.commonAnimation("UseItem", user);                         // :1366
            boolean showAnim = true;                                         // :1367
            user.pbRaiseStatStageByCause(PBStats.SPATK, 1, user, user.itemName(), showAnim, false); // :1368
            user.pbConsumeItem();                                            // :1369
        });
    }

    /**
     * The move's effect strategy ({@code PokeBattle_Move.pbFromPBMove}); the L1'
     * convention is to ask the strategy for every move predicate, so an
     * un-registered function code falls back to
     * {@code PokeBattle_UnimplementedMove} and inherits the
     * {@link pokemon.runtime.battle.movefx.MoveEffectBase} bodies of
     * {@code PokeBattle_Move}.
     */
    private static MoveEffect fx(BattleMove move) {
        return MoveEffectRegistry.of(move.function());
    }
}
