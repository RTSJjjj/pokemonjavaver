# M0 接口冻结契约 —— Claude 收工后一次性要加进 Battler.java / Battle.java 的东西

> **状态**：待执行。用户裁决 = **方案 B：等 Claude 收工再改这两个文件**。
> 本文是「一次改完」的清单，**执行前必须重新 `read` 当前版本并 rebase**（Claude 还在改）。
> 详细逐方法依据见 `stage4-m0b-battler-api-notes.md`（battlerapi 交付，505 行，209 个签名 100% 带 `段:行号`）。

## 0. 为什么需要这份文档

能力/道具/招式效果的 handler 体（`BattleHandlers_Abilities` 4,585 行、`BattleHandlers_Items` 1,804 行、`PokeBall_CatchEffects` 258 行、`Move_Effects_*` 12,447 行）会调用：

| 接收者 | 不同方法数 | 最高频的前几个 |
|---|---|---|
| `Battler` | 147（去 4 个提取器伪影 → 143） | `pbThis` 308、`effects` 109、`abilityName` 86、`battle` 72、`index` 68、`totalhp` 54、`isSpecies?` 45、`pbCanRaiseStatStage?` 40、`damageState` 39 |
| `Battle` | 41 | `pbDisplay` 248、`pbShowAbilitySplash` 148、`pbHideAbilitySplash` 138、`pbCommonAnimation` 42、`field` 33、`pbRandom` 22 |
| `Move` | 25 | `specialMove?` 28、`physicalMove?` 26、`calcType` 23、`pbContactMove?` 13、`soundMove?` 11 |

这些方法在 Java 里**一个都不存在**（`effects`/`damageState` 连字段都没有）。不先冻结它们，1,000+ 条 handler 无法逐行转译。

`infra2` 正在把 `BattleHandlers` 转成新文件，它们对缺失方法的调用**统一走临时桩层 `PendingApi`**（抛 `UnsupportedOperationException`，不写假实现）。本文的任务就是：**把 `PendingApi` 的每个桩替换成真实实现**，并补上字段。

> **订正（infra2 脚本复核，以 Ruby 原文为准）**：`BattleHandlers.rb` 的注册表是 **86 条声明 / 85 个不同名字**，`def self.trigger*` 是 **85 条**（我先前手数的「84」是错的）。重复声明的是 `ItemOnOpposingStatGain`，位于 `:28` 与 `:92`（`:93` 是 `CertainSwitchingUserItem`）。
> 另订正两处：`PokeBattle_ActiveSide` 实际写 **24** 条初值（23 个 side 组名 + `:65` 的 battler 组 `DelusionSong`=172）；`PokeBattle_DamageState` 实际是 **22** 个 `attr_accessor`（`:2-9` + `:11-24`，`:10` 空行），嵌套 `SuccessState` 另 4 个。
> **接口类型冻结决定**（infra2 提出，已批准）：`mults`/`mods` 用 **`float[]`**（插件有 `*= 1.3/1.5` 等浮点运算，`int[]` 会静默截断）；`status` 参数用 **`int`**（照 Ruby 的 `PBStatuses` 数值 id，接线处用 `PBStatuses.idOf(battler.status)` 单点转换）；`type`/`immuneType` 参数用 **`String` 内部名**（本工程类型身份就是内部名字符串）。

---

## 1. `Battler.java` —— 要加的字段（Ruby 依据）

| Java 字段 | Ruby | 行号 | 说明 |
|---|---|---|---|
| `public EffectMap effects = new EffectMap();` | `@effects` | `Battler_Initialize:112-357`（`pbInitEffects`） | **最高频成员（109 次）**。初值逐行照 `pbInitEffects(false)` 的 `:127-357` 写 |
| `public DamageState damageState = new DamageState();` | `@damageState` | `PokeBattle_DamageState:1-56`，`pbInitialize:69 @damageState.reset` | **39 次** |
| `public Battle battle;` | `@battle` | `PokeBattle_Battler:216` 等 | 72 次。`Battle.addPlayer/addFoe` 里赋值 |
| `public String item;` | `@item` | `Battler_Initialize:84 @item = pkmn.item` | 40 次。**用内部名字符串**（本运行时惯例），`0`（Ruby 的"无道具"）↔ `""`/`null` |
| `public String ability;` | `@ability` | `Battler_Initialize:83 @ability = pkmn.ability` | 12 次。同上用内部名 |
| `public int statusCount;` | `@statusCount` | `:92` | `statusCount` 1 次 |
| `public String initialItem;` | `@initialItem` | `setInitialItem`/`initialItem` 9+8 次 | 见 `PokeBattle_Pokemon` 的对应实现 |
| `public String recycleItem;` | `@recycleItem` | `setRecycleItem`/`recycleItem` 2+6 次 | 同上 |
| `public boolean mirrorHerbUsed;` | `@mirrorHerbUsed` | `Battler_Initialize:356` | ⚠️ 插件**没有 `attr_accessor`**，但 `BattleHandlers_Items:1783/1790` 直接赋值 → 照抄：Java 里给 public 字段（并在 javadoc 登记该插件缺陷） |
| `public int pokemonIndex;` | `@pokemonIndex` | `:96` | 队伍索引，换人/经验要用 |
| `public boolean droppedBelowHalfHP;` | `@droppedBelowHalfHP` | `:163` | 1 次 |
| `public boolean statsDropped;` | `@statsDropped` | `pbEffectsOnSwitchIn:9` | 1 次 |
| `public boolean statsRaisedThisRound;` / `statsLoweredThisRound` | `:166-167` | | 2 次 |
| `public boolean dummy;` | `@dummy` | `Battler_Initialize:63` | 1 次 |
| `public boolean fainted;` | `@fainted` | `:157` | 与 `hp<=0` 并存（插件是显式字段） |
| `public int[] stages` 的编号 | `@stages` | `Battler_Initialize:127-133` | **见 §3，编号必须改** |

## 2. `Battler.java` —— 要加的方法（按用途分组）

完整签名与 Ruby 行号见 `stage4-m0b-battler-api-notes.md` §A。分组与数量（battlerapi 复核：可转 105 + 降级 27 + 登记 7 + 未定义 4）：

| 组 | 例子 | 可转 | 降级 | 登记 |
|---|---|---|---|---|
| 标识与查询 | `pbThis`/`pbThis(bool)`/`name`/`isSpecies?`/`form`/`species`/`gender`/`level`/`index`/`opposes?`/`idxOwnSide`/`idxOpposingSide`/`pbOwnSide`/`pbOpposingSide`/`pbTeam`/`pbOpposingTeam`/`pbOwnedByPlayer?`/`pbDirectOpposing`/`near?` | ✓ | | |
| 能力/道具 | `hasActiveAbility?`/`abilityActive?`/`unstoppableAbility?`/`ungainableAbility?`/`uncopyableAbility?`/`hasActiveItem?`/`itemActive?`/`unlosableItem?`/`abilityName`/`itemName`/`initialItem`/`setInitialItem`/`recycleItem`/`setRecycleItem`/`pbRemoveItem`/`pbConsumeItem`/`pbHeldItemTriggered`/`pbHeldItemTriggerCheck`/`pbItemHPHealCheck`/`pbItemStatusCureCheck`/`pbItemEndOfMoveCheck`/`pbItemStatRestoreCheck`/`pbItemTerrainStatBoostCheck`/`pbItemOnIntimidatedCheck`/`pbItemOnStatDropped`/`pbItemOpposingStatGainCheck`/`pbSymbiosis`/`hasUtilityUmbrella?` | ✓ | | |
| HP 与治疗 | `pbRecoverHP`/`pbRecoverHP?`/`pbReduceHP`/`canHeal?`/`takesIndirectDamage?`/`pbFaint`/`pbAbilitiesOnDamageTaken`/`pbAbilitiesOnSwitchOut`/`pbAbilitiesOnFainting` | ✓ | 部分（`scene.*` 登记） | |
| 状态异常 | `pbCanPoison?`/`pbPoison`/`pbCanBurn?`/`pbBurn`/`pbCanParalyze?`/`pbParalyze`/`pbCanSleep?`/`pbSleep`/`pbSleepSelf`/`pbCanFreeze?`/`pbCureStatus`/`pbCureConfusion`/`pbConfuse`/`pbCanConfuse?`/`pbCanConfuseSelf?`/`pbCureAttract`/`pbCanAttract?`/`pbAttract`/`pbHasAnyStatus?`/`burned?`/`poisoned?`/`asleep?`/`paralyzed?`/`frozen?`/`pbHasType?`/`pbHasOtherType?`/`pbChangeTypes` | ✓ | | |
| 能力等级 | `pbCanRaiseStatStage?`/`pbRaiseStatStage`/`pbRaiseStatStageByCause`/`pbRaiseStatStageByAbility`/`pbCanLowerStatStage?`/`pbLowerStatStage`/`pbLowerStatStageByCause`/`pbLowerStatStageByAbility`/`pbLowerAttackStatStageIntimidate`/`statStageAtMax?`/`statStageAtMin?`/`hasAlteredStatStages?`/`plainStats`/`pbResetStatStages` | ✓ | | |
| 特性触发 | `pbEffectsOnSwitchIn`/`pbContinualAbilityChecks`/`pbAbilityStatusCureCheck`/`pbOnAbilityChanged`/`pbAbilityOnTerrainChange`/`pbCheckFormOnWeatherChange`/`pbCheckFormOnStatusChange`/`pbChangeFormTransform`/`pbTransform`/`pbUpdate` | ✓ | 部分 | `pbChangeFormTransform` 依赖 scene |
| 场地/天气查询 | `effectiveWeather`/`pbWeather`(⚠️插件缺陷)/`airborne?`/`semiInvulnerable?`/`inTwoTurnAttack?`/`dynamax?`(⚠️插件缺陷)/`isCommander?`/`affectedByContactEffect?`/`affectedByPowder?`/`pbWeight`/`pbTypes` | ✓ | | |
| 迭代 | `eachAlly`/`allAllies`/`eachOpposing`/`eachMove`/`movedThisRound?`/`num_fainted_allies`/`reborn?`/`setReborn`/`isUnnerved?`/`pbCanConsumeBerry?`/`lastRegularMoveUsed`/`moves`/`attack`/`defense`/`spdef`/`speed`/`type1`/`type2`/`totalhp`/`hp`/`status`/`status!`/`gender!`/`index!`/`species!`/`pokemon`/`turnCount` | ✓ | | |

**登记（7 条，一律空实现 + 注释，不写假行为）**：`pbChangeFormTransform` 的 `scene.pbChangePokemonTransform`、`status=`/`statusCount=` 里的 `pbRefreshOne`、`pbFaint` 里的 `scene.pbFaintBattler` + `scene.sprites[...]`、`pbReduceHP`/`pbRecoverHP` 里的 `scene.pbHPChanged` 等。

**未定义（4 条，照抄插件缺陷）**：`uncopyableAbility?` 用了从未定义的 `@ability_id`（`PokeBattle_Battler:491`）→ 照抄为恒 `false` 并在注释登记；`dynamax?`、`pbRecoverHP?`、`pbWeather` 见 §5。

## 3. ⚠️ 必须一起改的：能力等级编号（`PBStats`）

**插件**（`PBStats.rb:9-16`）：`HP=0, ATTACK=1, DEFENSE=2, SPEED=3, SPATK=4, SPDEF=5, ACCURACY=6, EVASION=7`
**现有 Java**（`Battler.java:29-31`、`MoveEffects.java:15-21`）：`ATK=0, DEF=1, SPEED=2, SPATK=3, SPDEF=4, ACCURACY=5, EVASION=6`（整体 −1，且 `hitStages` 拆成另一个数组）

现状**自洽**（不是行为 bug），但 1,000+ 处逐行转译里每一处都要手工换算，这是最容易出错的地方。

**目标（推荐）**：改成插件原编号。
- `Battler.stages` → `int[8]`，索引直接用 `PBStats.ATTACK`…`PBStats.EVASION`；`hitStages` 删除，`accuracyStage()`/`evasionStage()` 改读 `stages[PBStats.ACCURACY]`/`stages[PBStats.EVASION]`。
- 调用点共 **main 45 处 + test 26 处**（`stages[`/`hitStages` 30 处 + `MoveEffects.ATK..EVASION` 15 处）。
- `MoveEffects.java` 的 7 个重复常量删除，改用 `PBStats.*`；`MoveEffects.of()` 里所有 `new int[]{ATK,1}` 等**逐条改成 `PBStats.*`**（这是真正的错误来源：`SPDEF` 现在是 4，插件是 5）。
- `Battle.addStage`/`applyStages` 的 `stat` 参数改按 `PBStats` 编号；`statName(int)` 改调 `PBStats.getName`。

**回退方案（若不愿动 Claude 的 `Battle.java` 等级代码）**：保留现有移位编号，改为提供 `Battler.stage(int pbStat)` / `setStage(int pbStat, int value)` 访问器做映射，handler 一律走访问器。缺点是 `Battle.addStage` 仍要按 `PBStats` 编号收参 → 仍要改 `Battle.java`，收益小。

**结论：按推荐方案一次改干净。**

## 4. `Battle.java` —— 要加的方法

| 组 | 方法 | 次数 | 备注 |
|---|---|---|---|
| 显示 | `pbDisplay`(248)、`pbDisplayPaused`(2)、`pbShowAbilitySplash`(148)、`pbHideAbilitySplash`(138)、`pbReplaceAbilitySplash`(6)、`pbCommonAnimation`(42)、`pbAnimation`(1) | | **必须接进 B8 的 `roundEvents` 事件流**（`roundMessages.add` + `roundEvents.add(RoundEvent.message(...))`），`pbCommonAnimation` 依赖未做的动画播放器 → 空实现 + 登记 |
| 场地 | `field`(33)、`pbWeather`(3)、`environment`(1)、`time`(1)、`pbStartTerrain`(12)、`pbStartWeather`、`pbEndPrimordialWeather`、`pbStartWeather` 的 `fixedDuration` | | `field` 用 `infra3` 新建的 `BattleField`；`@sides`/`@positions` 暂寄在 `BattleField` 上（见 task-9 javadoc） |
| 随机 | `pbRandom`(22) | | **必须用 `Battle` 现有的同一个 `Random`**（测试靠固定种子复现），不能另开一个 |
| 队伍/换人 | `allBattlers`(4)、`eachBattler`(1)、`eachSameSideBattler`(2)、`eachOtherSideBattler`(8)、`allOtherSideBattlers`(1)、`battlers`(1)、`pbSideBattlerCount`(1)、`pbAllFainted?`(10)、`pbCanSwitch?`(1)、`pbCanChooseNonActive?`(3)、`pbCanRun?`(1)、`pbGetReplacementPokemonIndex`(4)、`pbRecallAndReplace`(4)、`pbClearChoice`(5)、`choices`(3)、`pbGetOwnerName`(1)、`allSameSideBattlers`（插件在 `AI_Move_EffectScores:3866` 猴补） | | `infra2` 已在 `BattleHandlers.java` 内用 `partyBySide`/`partyOf` 做了私有等价助手；若 `Battle` 上加了真方法，可切回去 |
| 其它 | `moldBreaker`(5)、`pbCheckOpposingAbility`(4)、`pbCheckGlobalAbility`(1)、`pbSetSeen`(1)、`pbPlayer`(1)、`decision`(1)、`endOfRound`(1)、`turnCount`(2)、`futureSight`(1)、`sideStatUps`(1)、`scene`(21) | | `scene` → 登记（UI 不在本批）；`pbSetSeen` 依赖图鉴（`PSystem_PokemonUtilities:203`）→ 登记 |

## 5. 需要用户拍板的 8 处插件自身缺陷（照抄 + 注释登记，不自造）
| 位置 | 问题 | 处置 |
|---|---|---|
| `BattleHandlers_Items:186` | `pbRecoverHP?`（多一个 `?`，方法不存在） | 该分支照抄为「永不进入」并注释登记 |
| `BattleHandlers_Items:1317` | `user.dynamax?` 无 `defined?` 保护，全工程无 `dynamax?` | 登记（照抄调用形状） |
| `BattleHandlers_Items:1783/1790` | `battler.mirrorHerbUsed=` 没有 `attr_accessor` | Java 给 public 字段 + 注释登记 |
| `BattleHandlers_Items:1309` | `Battle::Scene::USE_ABILITY_SPLASH` 全工程无此模块 | 登记；用 `PokeBattle_SceneConstants.USE_ABILITY_SPLASH` 代替并注明 |
| `BattleHandlers_Abilities:4581` | `target.pbWeather`，Battler 上只有 `effectiveWeather` | 登记 |
| `Battler_AbilityAndItem:218` | `pbIsBerry?(item_to_use)`，`item_to_use` 从未定义 | 登记（该行在插件里必然抛错） |
| `PokeBattle_Battler:491` | `@ability_id` 从未定义 → `uncopyableAbility?` 恒 false | 照抄恒 false + 登记 |
| `Battler_Statuses:240` | 用了未定义的局部 `user` | 登记 |

**另有 3 处插件自身缺陷但不阻塞**：
- `PokeBattle_SceneConstants:2 USE_ABILITY_SPLASH = true` → 30+ 处 `if/else` 的 `else` 支是**死代码**；Java 侧要补这个常量（`PokeBattle_SceneConstants.java` 不在 Claude 改动集里，Lead 会补）。
- `PBEffects::Tearalament` 声明两次（166→171，后者胜）→ 已在 `PBEffects.java` 按 171 落地。
- `Battler#pbChangeTypes` 同段定义两次（`Battler_ChangeSelf:285-314` 覆盖 `:157-177`）→ 只转胜出版。

## 6. 执行顺序（Claude 收工后）

1. `read` 当前 `Battler.java` / `Battle.java` / `BattleScreen.java`，确认与 Claude 的最终版一致，rebase 本文清单。
2. 补 `PokeBattle_SceneConstants.USE_ABILITY_SPLASH`。
3. `Battler.java`：加 §1 字段 + `pbInitEffects` 逐行转译 + §2 的 143 个方法（登记项空实现）+ §3 的编号改造。
4. `Battle.java`：加 §4 的方法；`field` 接 `BattleField`；`pbDisplay*` 接进 `roundEvents`；`pbRandom` 复用同一个 `Random`。
5. `MoveEffects.java`：按 §3 换编号；`BattleMove`（现 70 行）扩到 `PokeBattle_Move` + `Move_Usage` 的 55 个钩子默认实现（L1 的 `movebase` 任务）。
6. 把 `PendingApi` 的每个桩替换成真实调用，然后**删除 `PendingApi`**（或留空类 + 注释）。
7. 验收：`:core:compileJava` BUILD SUCCESSFUL；`:core:test` **571/0/0** 不许掉；`lwjgl3:compileJava` 绿；builder 295/0 绿。

## 7. 与 Claude 的写权限边界（本会话已确认）

| 文件 | 归属 |
|---|---|
| `battle/Battle.java`、`battle/Battler.java`、`battle/BattleScreen.java`、`battle/InteractiveBattlePort.java`、`audio/AudioManager.java`、`test/battle/BattleRoundEventsTest.java`、`test/battle/BattleRoundMessagesTest.java`、`docs/stage3-b8-reading-notes.md`、`docs/stage3-wild-battle-audit.md` | **Claude**（用户指示：绝对不能碰） |
| `battle/MoveEffects.java`、`battle/DamageCalc.java`、`battle/BattleAi.java` | **Lead**（用户已授权） |
| `battle/PBEffects.java`、`EffectMap.java`、`HandlerHash.java`、`PBWeather.java`、`PBBattleTerrains.java`、`PBStats.java`、`PBStatuses.java`、`BattleHandlers.java`、`BattleHandlerHelpers.java`、`PendingApi.java`、`PBTypeEffectiveness.java`、`BattleField.java`、`BattleSide.java`、`BattlePosition.java`、`DamageState.java`、`BattleHandlerHelpers.java` | **Lead / infra2 / infra3**（全为新文件） |
| `pokemon-builder/builder/**`、`generated/pbs/types.json`、`pokemon/PbsData.java` | `buildertypes`（task-7） |
| `docs/stage4-*.md` | Lead / 各任务交付人 |

---

## 8. L1' 写权限切分（13 个文件）+ 注册顺序（battlerapi 花名册裁决后定稿）

依据：`stage4-l1-handler-roster.md`（559 条 = 特性 380 + 持有物 179，`copy` 展开后另有 95 个新增符号注册，覆盖 79 个组）。

**必须显式控制注册顺序**，因为 `HandlerHash#add` 是 `@hash[id]=handler` 直接覆盖（`Event_Handlers.rb:110-113`），而全工程 636 条注册里有 **16 条在 `Arceus` / `场地`** 两个段，这两段在 `--list` 里排在 `BattleHandlers_*` **之后** → 其中 **10 条覆盖主表**（`Arceus:775` INSOMNIA(StatusImmunity)、`:782` SWEETVEIL、`:790` MAGMAARMOR、`:798` INSOMNIA(StatusCure)、`:810` SYNCHRONIZE、`:871` HEALER、`:902` HYDRATION、`:934` SHEDSKIN、`:970` CHESTOBERRY、`:983` LUMBERRY），另 6 条新增（`场地:597` BUGLURESEED、`:614` COLDSEED、`:707` FROZENARMOR、`:720` TRAPTRICK、`:735` BUGLURESURGE、`:751` COLDSURGE）。

Java 静态初始化顺序不可控 → 用 `BattleHandlerRegistry.init()` 按 Ruby 段顺序显式调用 13 个 `register()`：`AbilitiesSwitchIn` → `AbilitiesOnHit` → `AbilitiesDamageUser` → `AbilitiesSpeedEor` → `AbilitiesStatus` → `AbilitiesAccuracyCritType` → `AbilitiesDamageTarget` → `ItemsDamageUser` → `ItemsHealStatus` → `ItemsDamageTarget` → `ItemsFieldSpeed` → `ItemsOnHit` → `HandlerArceusOverrides`。每个 `trigger*` 包装第一行调 `BattleHandlers.init()`（幂等 boolean）。

| 文件 | 条目数 | 行数 | 含 copy 的真实符号数 |
|---|---:|---:|---:|
| `AbilitiesSwitchIn` | 91 | 1174 | 108 |
| `AbilitiesOnHit` | 72 | 1190 | — |
| `AbilitiesDamageUser` | 72 | 590 | — |
| `AbilitiesSpeedEor` | 47 | 623 | — |
| `AbilitiesStatus` | 38 | 510 | 72 |
| `AbilitiesAccuracyCritType` | 32 | 249 | — |
| `AbilitiesDamageTarget` | 28 | 245 | — |
| `ItemsDamageUser` | 61 | 436 | 100 |
| `ItemsHealStatus` | 38 | 509 | — |
| `ItemsDamageTarget` | 31 | 234 | — |
| `ItemsFieldSpeed` | 28 | 283 | — |
| `ItemsOnHit` | 21 | 338 | — |
| `HandlerArceusOverrides` | 16 | — | 10 覆盖 + 6 新增 |
| **合计** | **559 + 16** | | **+95 copy 展开** |

**空组保留**（声明了但 0 注册的 6 个）：`StatGainImmunityAbility`、`AbilityOnStatGain`、`AbilityModifyTypeEffectiveness`、`CriticalCalcTargetItem`、`CertainSwitchingUserAbility`、`TrappingTargetItem` —— Ruby 里 handler 缺失时 `trigger` 返回 `nil`、`triggerXxx` 再还原默认值，空表不能省。

**登记 5 条**（BOSS 3 条因 `battleRank` 按 `int` 建模而降为可转；battlerapi 订正过我的分解）：
- 换人 4：`#1 AbilityOnHPDroppedBelowHalf/EMERGENCYEXIT`（@`Abilities:83-126`）、`#527 TargetItemAfterMoveUse/EJECTBUTTON`（@`Items:1282-1296`）、`#528 TargetItemAfterMoveUse/REDCARD`（@`Items:1297-1331`）、`#508 ItemOnStatLoss/EJECTPACK`（@`Items:1702-1718`）
- 图鉴 1：`#113 TargetAbilityOnHit/ILLUSION`（@`Abilities:1603-1613`，`battle.pbSetSeen(target)` @`:1610`）—— **唯一只靠图鉴/存档、不靠换人的条目，最容易漏**
- 其中 `#1 EMERGENCYEXIT` **另外**命中训练家（`battle.pbGetOwnerName` @`:107`）
