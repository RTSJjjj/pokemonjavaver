# Stage 4 / L1' 花名册：BattleHandlers 特性与持有物 handler 切分表

> **本文件由脚本生成；本次任务只读研究，未改动任何 `.java` / `.js`。**
> 数据源：本工程 `Data/Scripts.rxdata` 现场导出的 `__r20-ref/ruby/BattleHandlers_Abilities.rb`(4585 行)、
> `__r20-ref/ruby/BattleHandlers_Items.rb`(1804 行)。行号 = 段内真实行号（导出件左侧 `  N |` 前缀即 Ruby 行号）。
> 所有数字都由脚本统计，**没有一处手数**；§9 是生成本文件的脚本全文。

## 0. 复现方式

脚本读**自身**并把自身源码嵌进本文件，所以 `node` 跑一次即可重新生成整个文件（可逐字节对比）。

```powershell
# 1) 把 §9 的 ```js 代码块另存为 l1-roster.mjs（脚本内部用绝对路径读工程，放哪都行）
# 2) 运行（node v24 已在工程根验证可用）
node l1-roster.mjs
```

`add` / `addIf` / `copy` 的识别正则（对导出件全文逐行匹配）：

```js
/BattleHandlers::([A-Za-z_][A-Za-z0-9_]*)\s*\.\s*(addIf|add|copy)\s*\(/
```

**行数约定（按 Lead 要求）**：`行数 =（下一条 add/addIf 的 Ruby 行号 − 本条的行号）`；**最后一条到段尾**
（Abilities 到 4585，Items 到 1804）。`copy` 行**不**单独成条目，另见 §5、§6。

> 该约定的一个副作用：span 的**末行可能属于下一条**（例如 `MIRRORHERB` 的 `1798` 是下一条 `PUNCHINGGLOVE` 的抬头注释 `# 拳击手套`）。
> 这是约定的必然结果，不是算错 —— 切文件时按**首行**归属即可。

脚本还需要两个额外导出件（§9 的边界外注册要用，放在 `__r20-ref/m0b/` 下，**不是** `.java`/`.js`）：

```powershell
node __sections.mjs "Arceus"  > __r20-ref/m0b/Arceus.txt        # 1024 行
node __sections.mjs "场地"    > __r20-ref/m0b/FieldTerrain.txt  # 762 行
```

## 1. 统计

| 项 | 特性（Abilities） | 持有物（Items） | 合计 |
|---|---:|---:|---:|
| `add`/`addIf` 条目数 | 380 | 179 | **559** |
| ├ 其中 `.add` | 380 | 179 | 559 |
| └ 其中 `.addIf` | 0 | 0 | 0 |
| `.copy` 行数 | 40 | 21 | 61 |
| 出现注册的组数（group） | 48 | 31 | 79 |
| 条目行数合计（span 之和） | 4581 / 4585 | 1800 / 1804 | 6381 |

> **`addIf` 在本批两个文件里 0 次。** `addIf` 定义在 `Event_Handlers.rb:98-103`，本批两个段未使用它
> （全工程只在 `PItem_ItemEffects:400`、`PItem_BattleItemEffects:24/309` 等处使用）。
> span 之和 < 段长，是因为每个段**首条登记之前的抬头注释**不落进任何条目：
> Abilities 第 1-4 行、Items 第 1-4 行。
>
> ⚠️ 全工程还有 **16** 条同组注册落在**别的段**里（不在上表、也不在切片小计里），见 **§9** —— 这一步会改变切片方案，请先看 §9。

### 1.1 按「可转 / 降级 / 登记」计数

| 判定 | 特性 | 持有物 | 合计 |
|---|---:|---:|---:|
| 可转 | 232 | 137 | **369** |
| 降级 | 144 | 38 | **182** |
| 登记 | 4 | 4 | **8** |
| **合计** | **380** | **179** | **559** |

判定规则（脚本里就是这三条，可复核）：

1. 条目 body 命中**功能性子系统**之一（换人 / 捕获 / 训练家 / 存档 / 图鉴 / BOSS / 狩猎区 / 战斗宫 / 华丽大赛）→ **登记**：语义依赖本运行时还没有的状态，本批不能转；
2. 否则若命中**纯表现**子系统（动画/场景 / PBDebug）→ **降级**：逻辑照转，演出/日志调用做成空实现 + 注释登记；
3. 否则 → **可转**。

> 这两句是本次唯一的判断性规则；`依赖的子系统` 列由 §0 的 `MARK` 正则表逐条命中得出（脚本内可查）。

### 1.2 按「建议 Java 文件」计数（小计之和 = 总数，脚本内已断言）

| 建议 Java 文件 | 条目数 | 行数小计 | 占比(条目) |
|---|---:|---:|---:|
| `AbilitiesSwitchIn.java` | 91 | 1174 | 16.3% |
| `AbilitiesOnHit.java` | 72 | 1190 | 12.9% |
| `AbilitiesDamageUser.java` | 72 | 590 | 12.9% |
| `AbilitiesSpeedEor.java` | 47 | 623 | 8.4% |
| `AbilitiesStatus.java` | 38 | 510 | 6.8% |
| `AbilitiesAccuracyCritType.java` | 32 | 249 | 5.7% |
| `AbilitiesDamageTarget.java` | 28 | 245 | 5.0% |
| `ItemsDamageUser.java` | 61 | 436 | 10.9% |
| `ItemsDamageTarget.java` | 31 | 234 | 5.5% |
| `ItemsHealStatus.java` | 38 | 509 | 6.8% |
| `ItemsOnHit.java` | 21 | 338 | 3.8% |
| `ItemsFieldSpeed.java` | 28 | 283 | 5.0% |
| **合计** | **559** | **6381** | 100% |

> 校验：小计条目数之和 559 = 总数 559 ✓

### 1.3 按「组」计数

| 组 | 段 | 条目数 | 行数小计 | 建议 Java 文件 |
|---|---|---:|---:|---|
| AbilityChangeOnBattlerFainting | Abilities | 2 | 36 | `AbilitiesSwitchIn.java` |
| AbilityOnBattlerFainting | Abilities | 2 | 16 | `AbilitiesSwitchIn.java` |
| AbilityOnFlinch | Abilities | 1 | 10 | `AbilitiesOnHit.java` |
| AbilityOnHPDroppedBelowHalf | Abilities | 1 | 44 | `AbilitiesSwitchIn.java` |
| AbilityOnInflictingStatus | Abilities | 1 | 12 | `AbilitiesStatus.java` |
| AbilityOnMoveSuccessCheck | Abilities | 1 | 10 | `AbilitiesStatus.java` |
| AbilityOnOpposingStatGain | Abilities | 1 | 28 | `AbilitiesStatus.java` |
| AbilityOnStatLoss | Abilities | 2 | 18 | `AbilitiesStatus.java` |
| AbilityOnStatusInflicted | Abilities | 1 | 43 | `AbilitiesStatus.java` |
| AbilityOnSwitchIn | Abilities | 76 | 989 | `AbilitiesSwitchIn.java` |
| AbilityOnSwitchOut | Abilities | 4 | 46 | `AbilitiesSwitchIn.java` |
| AbilityOnTerrainChange | Abilities | 1 | 7 | `AbilitiesSwitchIn.java` |
| AccuracyCalcTargetAbility | Abilities | 8 | 58 | `AbilitiesAccuracyCritType.java` |
| AccuracyCalcTargetItem | Items | 1 | 12 | `ItemsDamageTarget.java` |
| AccuracyCalcUserAbility | Abilities | 8 | 56 | `AbilitiesAccuracyCritType.java` |
| AccuracyCalcUserAllyAbility | Abilities | 1 | 10 | `AbilitiesAccuracyCritType.java` |
| AccuracyCalcUserItem | Items | 3 | 29 | `ItemsDamageTarget.java` |
| CertainSwitchingUserItem | Items | 1 | 17 | `ItemsFieldSpeed.java` |
| CriticalCalcTargetAbility | Abilities | 1 | 12 | `AbilitiesAccuracyCritType.java` |
| CriticalCalcUserAbility | Abilities | 3 | 23 | `AbilitiesAccuracyCritType.java` |
| CriticalCalcUserItem | Items | 3 | 30 | `ItemsDamageTarget.java` |
| DamageCalcTargetAbility | Abilities | 23 | 198 | `AbilitiesDamageTarget.java` |
| DamageCalcTargetAbilityNonIgnorable | Abilities | 2 | 20 | `AbilitiesDamageTarget.java` |
| DamageCalcTargetAllyAbility | Abilities | 3 | 27 | `AbilitiesDamageTarget.java` |
| DamageCalcTargetItem | Items | 24 | 163 | `ItemsDamageTarget.java` |
| DamageCalcUserAbility | Abilities | 67 | 550 | `AbilitiesDamageUser.java` |
| DamageCalcUserAllyAbility | Abilities | 5 | 40 | `AbilitiesDamageUser.java` |
| DamageCalcUserItem | Items | 61 | 436 | `ItemsDamageUser.java` |
| EOREffectAbility | Abilities | 15 | 232 | `AbilitiesSpeedEor.java` |
| EOREffectItem | Items | 3 | 32 | `ItemsFieldSpeed.java` |
| EORGainItemAbility | Abilities | 2 | 55 | `AbilitiesSpeedEor.java` |
| EORHealingAbility | Abilities | 3 | 84 | `AbilitiesSpeedEor.java` |
| EORHealingItem | Items | 2 | 34 | `ItemsFieldSpeed.java` |
| EORWeatherAbility | Abilities | 4 | 70 | `AbilitiesSpeedEor.java` |
| EVGainModifierItem | Items | 7 | 46 | `ItemsHealStatus.java` |
| EndOfMoveItem | Items | 1 | 33 | `ItemsHealStatus.java` |
| EndOfMoveStatRestoreItem | Items | 1 | 27 | `ItemsHealStatus.java` |
| ExpGainModifierItem | Items | 1 | 10 | `ItemsHealStatus.java` |
| HPHealItem | Items | 16 | 178 | `ItemsHealStatus.java` |
| ItemOnIntimidated | Items | 1 | 13 | `ItemsHealStatus.java` |
| ItemOnOpposingStatGain | Items | 1 | 19 | `ItemsHealStatus.java` |
| ItemOnStatLoss | Items | 1 | 17 | `ItemsHealStatus.java` |
| ItemOnSwitchIn | Items | 2 | 20 | `ItemsFieldSpeed.java` |
| MoveBaseTypeModifierAbility | Abilities | 11 | 90 | `AbilitiesAccuracyCritType.java` |
| MoveBlockingAbility | Abilities | 1 | 19 | `AbilitiesOnHit.java` |
| MoveImmunityTargetAbility | Abilities | 18 | 212 | `AbilitiesOnHit.java` |
| PriorityBracketChangeAbility | Abilities | 3 | 25 | `AbilitiesSpeedEor.java` |
| PriorityBracketChangeItem | Items | 3 | 25 | `ItemsFieldSpeed.java` |
| PriorityBracketUseAbility | Abilities | 1 | 10 | `AbilitiesSpeedEor.java` |
| PriorityBracketUseItem | Items | 2 | 19 | `ItemsFieldSpeed.java` |
| PriorityChangeAbility | Abilities | 5 | 41 | `AbilitiesSpeedEor.java` |
| RunFromBattleAbility | Abilities | 1 | 7 | `AbilitiesSpeedEor.java` |
| RunFromBattleItem | Items | 1 | 11 | `ItemsFieldSpeed.java` |
| SpeedCalcAbility | Abilities | 10 | 77 | `AbilitiesSpeedEor.java` |
| SpeedCalcItem | Items | 4 | 33 | `ItemsFieldSpeed.java` |
| StatLossImmunityAbility | Abilities | 6 | 105 | `AbilitiesStatus.java` |
| StatLossImmunityAbilityNonIgnorable | Abilities | 1 | 19 | `AbilitiesStatus.java` |
| StatLossImmunityAllyAbility | Abilities | 1 | 27 | `AbilitiesStatus.java` |
| StatLossImmunityItem | Items | 1 | 9 | `ItemsHealStatus.java` |
| StatusCheckAbilityNonIgnorable | Abilities | 1 | 11 | `AbilitiesStatus.java` |
| StatusCureAbility | Abilities | 8 | 126 | `AbilitiesStatus.java` |
| StatusCureItem | Items | 8 | 157 | `ItemsHealStatus.java` |
| StatusImmunityAbility | Abilities | 10 | 73 | `AbilitiesStatus.java` |
| StatusImmunityAbilityNonIgnorable | Abilities | 2 | 16 | `AbilitiesStatus.java` |
| StatusImmunityAllyAbility | Abilities | 3 | 22 | `AbilitiesStatus.java` |
| TargetAbilityAfterMoveUse | Abilities | 5 | 100 | `AbilitiesOnHit.java` |
| TargetAbilityOnHit | Abilities | 34 | 577 | `AbilitiesOnHit.java` |
| TargetItemAfterMoveUse | Items | 2 | 50 | `ItemsOnHit.java` |
| TargetItemOnHit | Items | 13 | 181 | `ItemsOnHit.java` |
| TargetItemOnHitPositiveBerry | Items | 3 | 62 | `ItemsOnHit.java` |
| TerrainExtenderItem | Items | 1 | 10 | `ItemsFieldSpeed.java` |
| TerrainStatBoostItem | Items | 4 | 44 | `ItemsFieldSpeed.java` |
| TrappingTargetAbility | Abilities | 5 | 36 | `AbilitiesSwitchIn.java` |
| UserAbilityEndOfMove | Abilities | 10 | 192 | `AbilitiesOnHit.java` |
| UserAbilityOnHit | Abilities | 3 | 80 | `AbilitiesOnHit.java` |
| UserItemAfterMoveUse | Items | 3 | 45 | `ItemsOnHit.java` |
| WeatherExtenderItem | Items | 4 | 28 | `ItemsFieldSpeed.java` |
| WeightCalcAbility | Abilities | 3 | 22 | `AbilitiesSpeedEor.java` |
| WeightCalcItem | Items | 1 | 10 | `ItemsFieldSpeed.java` |
| **合计** | | **559** | **6381** | |

## 2. 主表（按「建议 Java 文件」分组，组内按源文件行号升序）

列：`序号 | 段:起-止行 | 组 | 符号 | 行数 | 依赖的子系统 | 建议 Java 文件 | 可转/降级/登记`

| 序号 | 段:起-止行 | 组 | 符号 | 行数 | 依赖的子系统 | 建议 Java 文件 | 可转/降级/登记 |
|---:|---|---|---|---:|---|---|---|
| 1 | BattleHandlers_Abilities:83-126 | AbilityOnHPDroppedBelowHalf | EMERGENCYEXIT | 44 | PBEffects、换人、训练家、动画/场景、异常状态、特性 | `AbilitiesSwitchIn.java` | 登记 |
| 2 | BattleHandlers_Abilities:2380-2385 | TrappingTargetAbility | ARENATRAP | 6 | 特性 | `AbilitiesSwitchIn.java` | 可转 |
| 3 | BattleHandlers_Abilities:2386-2391 | TrappingTargetAbility | MAGNETPULL | 6 | 特性 | `AbilitiesSwitchIn.java` | 可转 |
| 4 | BattleHandlers_Abilities:2392-2401 | TrappingTargetAbility | SHADOWTAG | 10 | 特性 | `AbilitiesSwitchIn.java` | 可转 |
| 5 | BattleHandlers_Abilities:2402-2414 | AbilityOnSwitchIn | AIRLOCK | 13 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 6 | BattleHandlers_Abilities:2415-2450 | AbilityOnSwitchIn | ANTICIPATION | 36 | 动画/场景、招式/PP、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 7 | BattleHandlers_Abilities:2451-2458 | AbilityOnSwitchIn | AURABREAK | 8 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 8 | BattleHandlers_Abilities:2459-2466 | AbilityOnSwitchIn | COMATOSE | 8 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 9 | BattleHandlers_Abilities:2467-2474 | AbilityOnSwitchIn | DARKAURA | 8 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 10 | BattleHandlers_Abilities:2475-2480 | AbilityOnSwitchIn | DELTASTREAM | 6 | 天气、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 11 | BattleHandlers_Abilities:2481-2486 | AbilityOnSwitchIn | DESOLATELAND | 6 | 天气、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 12 | BattleHandlers_Abilities:2487-2498 | AbilityOnSwitchIn | DOWNLOAD | 12 | 能力等级、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 13 | BattleHandlers_Abilities:2499-2504 | AbilityOnSwitchIn | DRIZZLE | 6 | 天气、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 14 | BattleHandlers_Abilities:2505-2510 | AbilityOnSwitchIn | DROUGHT | 6 | 天气、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 15 | BattleHandlers_Abilities:2511-2519 | AbilityOnSwitchIn | ELECTRICSURGE | 9 | 场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 16 | BattleHandlers_Abilities:2520-2527 | AbilityOnSwitchIn | FAIRYAURA | 8 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 17 | BattleHandlers_Abilities:2528-2567 | AbilityOnSwitchIn | FOREWARN | 40 | 动画/场景、招式/PP、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 18 | BattleHandlers_Abilities:2568-2591 | AbilityOnSwitchIn | FRISK | 24 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 19 | BattleHandlers_Abilities:2592-2600 | AbilityOnSwitchIn | GRASSYSURGE | 9 | 场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 20 | BattleHandlers_Abilities:2601-2618 | AbilityOnSwitchIn | IMPOSTER | 18 | PBEffects、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 21 | BattleHandlers_Abilities:2619-2636 | AbilityOnSwitchIn | INTIMIDATE | 18 | 动画/场景、能力等级、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 22 | BattleHandlers_Abilities:2637-2645 | AbilityOnSwitchIn | MISTYSURGE | 9 | 场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 23 | BattleHandlers_Abilities:2646-2654 | AbilityOnSwitchIn | MOLDBREAKER | 9 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 24 | BattleHandlers_Abilities:2655-2663 | AbilityOnSwitchIn | PRESSURE | 9 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 25 | BattleHandlers_Abilities:2664-2669 | AbilityOnSwitchIn | PRIMORDIALSEA | 6 | 天气、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 26 | BattleHandlers_Abilities:2670-2678 | AbilityOnSwitchIn | PSYCHICSURGE | 9 | 场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 27 | BattleHandlers_Abilities:2679-2684 | AbilityOnSwitchIn | SANDSTREAM | 6 | 天气、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 28 | BattleHandlers_Abilities:2685-2698 | AbilityOnSwitchIn | SLOWSTART | 14 | PBEffects、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 29 | BattleHandlers_Abilities:2699-2704 | AbilityOnSwitchIn | SNOWWARNING | 6 | 天气、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 30 | BattleHandlers_Abilities:2705-2713 | AbilityOnSwitchIn | TERAVOLT | 9 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 31 | BattleHandlers_Abilities:2714-2721 | AbilityOnSwitchIn | TURBOBLAZE | 8 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 32 | BattleHandlers_Abilities:2722-2730 | AbilityOnSwitchIn | UNNERVE | 9 | 动画/场景、异常状态、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 33 | BattleHandlers_Abilities:2731-2742 | AbilityOnSwitchIn | ASONEICE | 12 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 34 | BattleHandlers_Abilities:2743-2749 | AbilityOnSwitchIn | INTREPIDSWORD | 7 | 能力等级、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 35 | BattleHandlers_Abilities:2750-2756 | AbilityOnSwitchIn | DAUNTLESSSHIELD | 7 | 能力等级、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 36 | BattleHandlers_Abilities:2757-2788 | AbilityOnSwitchIn | SCREENCLEANER | 32 | PBEffects、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 37 | BattleHandlers_Abilities:2789-2802 | AbilityOnSwitchIn | PASTELVEIL | 14 | 动画/场景、异常状态、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 38 | BattleHandlers_Abilities:2803-2818 | AbilityOnSwitchIn | CURIOUSMEDICINE | 16 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 39 | BattleHandlers_Abilities:2819-2835 | AbilityOnSwitchIn | NEUTRALIZINGGAS | 17 | PBEffects、动画/场景、道具、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 40 | BattleHandlers_Abilities:2836-2854 | AbilityOnSwitchIn | DIMENSIONBODY | 19 | 能力等级、HP、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 41 | BattleHandlers_Abilities:2855-2861 | AbilityOnSwitchOut | NATURALCURE | 7 | PBDebug、异常状态、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 42 | BattleHandlers_Abilities:2862-2873 | AbilityOnSwitchOut | REGENERATOR | 12 | PBDebug、HP、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 43 | BattleHandlers_Abilities:2874-2896 | AbilityChangeOnBattlerFainting | POWEROFALCHEMY | 23 | 动画/场景、道具、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 44 | BattleHandlers_Abilities:2897-2906 | AbilityOnBattlerFainting | SOULHEART | 10 | 能力等级、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 45 | BattleHandlers_Abilities:2970-2997 | AbilityOnSwitchIn | COMMANDER | 28 | PBEffects、动画/场景、能力等级、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 46 | BattleHandlers_Abilities:3007-3023 | AbilityOnSwitchIn | COSTAR | 17 | PBEffects、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 47 | BattleHandlers_Abilities:3074-3081 | AbilityOnSwitchIn | WINDRIDER | 8 | PBEffects、能力等级、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 48 | BattleHandlers_Abilities:3097-3115 | AbilityOnSwitchIn | HOSPITALITY | 19 | 动画/场景、HP、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 49 | BattleHandlers_Abilities:3124-3136 | AbilityOnSwitchIn | SUPREMEOVERLORD | 13 | PBEffects、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 50 | BattleHandlers_Abilities:3162-3173 | AbilityOnSwitchIn | RIGHTTOFIGHT | 12 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 51 | BattleHandlers_Abilities:3174-3186 | AbilityChangeOnBattlerFainting | RIGHTTOFIGHT | 13 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 52 | BattleHandlers_Abilities:3187-3195 | AbilityOnSwitchIn | ZEROTOHERO | 9 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 53 | BattleHandlers_Abilities:3196-3207 | AbilityOnSwitchOut | ZEROTOHERO | 12 | 特性 | `AbilitiesSwitchIn.java` | 可转 |
| 54 | BattleHandlers_Abilities:3283-3305 | AbilityOnSwitchIn | SUPERSWEETSYRUP | 23 | PBEffects、动画/场景、能力等级、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 55 | BattleHandlers_Abilities:3325-3336 | AbilityOnSwitchIn | ORICHALCUMPULSE | 12 | 天气、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 56 | BattleHandlers_Abilities:3346-3357 | AbilityOnSwitchIn | HADRONENGINE | 12 | 场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 57 | BattleHandlers_Abilities:3368-3378 | AbilityOnSwitchIn | TERASHIFT | 11 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 58 | BattleHandlers_Abilities:3389-3446 | AbilityOnSwitchIn | TERAFORMZERO | 58 | 天气、场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 59 | BattleHandlers_Abilities:3459-3477 | AbilityOnSwitchIn | TABLETSOFRUIN | 19 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 60 | BattleHandlers_Abilities:3478-3528 | AbilityOnSwitchIn | PROTOSYNTHESIS | 51 | PBEffects、天气、场地、动画/场景、道具、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 61 | BattleHandlers_Abilities:3529-3535 | AbilityOnTerrainChange | QUARKDRIVE | 7 | 场地、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 62 | BattleHandlers_Abilities:3571-3577 | AbilityOnSwitchIn | BROKENBREATH | 7 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 63 | BattleHandlers_Abilities:3606-3614 | AbilityOnSwitchIn | BROKENBREATH | 9 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 64 | BattleHandlers_Abilities:3623-3632 | AbilityOnSwitchIn | THUNDERCLOUD | 10 | 场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 65 | BattleHandlers_Abilities:3654-3660 | AbilityOnSwitchIn | ENDLESSDARKN | 7 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 66 | BattleHandlers_Abilities:3661-3670 | AbilityOnSwitchIn | ETRTNALIGHT | 10 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 67 | BattleHandlers_Abilities:3707-3723 | AbilityOnSwitchIn | STARFISSURE | 17 | PBEffects、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 68 | BattleHandlers_Abilities:3758-3766 | AbilityOnSwitchIn | BLACKHAND | 9 | 能力等级、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 69 | BattleHandlers_Abilities:3778-3787 | AbilityOnSwitchIn | SLEEPSOUNDLY | 10 | 动画/场景、异常状态、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 70 | BattleHandlers_Abilities:3857-3867 | AbilityOnSwitchIn | STEELDYNASTY | 11 | PBEffects、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 71 | BattleHandlers_Abilities:3868-3878 | AbilityOnSwitchIn | AURAVOICE | 11 | PBEffects、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 72 | BattleHandlers_Abilities:3944-3952 | AbilityOnSwitchIn | RADIANTRULE | 9 | 场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 73 | BattleHandlers_Abilities:3953-3961 | AbilityOnSwitchIn | CALAMITYABYSSAL | 9 | 天气、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 74 | BattleHandlers_Abilities:3962-3971 | AbilityOnSwitchIn | CALAMITYINFERNAL | 10 | 天气、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 75 | BattleHandlers_Abilities:3972-3979 | AbilityOnSwitchIn | ROSEGARDEN | 8 | 场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 76 | BattleHandlers_Abilities:3989-3996 | AbilityOnSwitchIn | ROSEARCADIA | 8 | 场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 77 | BattleHandlers_Abilities:4008-4016 | AbilityOnSwitchIn | VERDANTWARD | 9 | 场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 78 | BattleHandlers_Abilities:4026-4035 | AbilityOnSwitchIn | PSYCHICEDGE | 10 | 场地、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 79 | BattleHandlers_Abilities:4036-4046 | AbilityOnSwitchIn | ETERNALFLAME | 11 | 天气、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 80 | BattleHandlers_Abilities:4170-4180 | AbilityOnSwitchIn | ROSYAEGIS | 11 | PBEffects、动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 81 | BattleHandlers_Abilities:4192-4198 | TrappingTargetAbility | DSOVERLORD | 7 | 特性 | `AbilitiesSwitchIn.java` | 可转 |
| 82 | BattleHandlers_Abilities:4199-4204 | AbilityOnBattlerFainting | DIVINEPACT | 6 | 能力等级、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 83 | BattleHandlers_Abilities:4228-4234 | TrappingTargetAbility | CONFESSIONLIST | 7 | 特性 | `AbilitiesSwitchIn.java` | 可转 |
| 84 | BattleHandlers_Abilities:4235-4245 | AbilityOnSwitchIn | CONFESSIONLIST | 11 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 85 | BattleHandlers_Abilities:4267-4274 | AbilityOnSwitchIn | SUPERSUN | 8 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 86 | BattleHandlers_Abilities:4309-4313 | AbilityOnSwitchIn | BESTOWEDRAIN | 5 | 天气、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 87 | BattleHandlers_Abilities:4494-4502 | AbilityOnSwitchIn | SHINYHERO | 9 | 动画/场景、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 88 | BattleHandlers_Abilities:4503-4517 | AbilityOnSwitchOut | SHINYHERO | 15 | 特性 | `AbilitiesSwitchIn.java` | 可转 |
| 89 | BattleHandlers_Abilities:4518-4532 | AbilityOnSwitchIn | DEEPSEAFASCINATION | 15 | 动画/场景、能力等级、特性 | `AbilitiesSwitchIn.java` | 降级 |
| 90 | BattleHandlers_Abilities:4559-4563 | AbilityOnSwitchIn | STORMEYE | 5 | 天气、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 91 | BattleHandlers_Abilities:4573-4577 | AbilityOnSwitchIn | RAINBOWARCH | 5 | 天气、特性 | `AbilitiesSwitchIn.java` | 可转 |
| 92 | BattleHandlers_Abilities:590-599 | AbilityOnFlinch | STEADFAST | 10 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 93 | BattleHandlers_Abilities:600-618 | MoveBlockingAbility | DAZZLING | 19 | 特性 | `AbilitiesOnHit.java` | 可转 |
| 94 | BattleHandlers_Abilities:619-633 | MoveImmunityTargetAbility | BULLETPROOF | 15 | 动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 95 | BattleHandlers_Abilities:634-659 | MoveImmunityTargetAbility | FLASHFIRE | 26 | PBEffects、动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 96 | BattleHandlers_Abilities:660-665 | MoveImmunityTargetAbility | LIGHTNINGROD | 6 | 特性 | `AbilitiesOnHit.java` | 可转 |
| 97 | BattleHandlers_Abilities:666-671 | MoveImmunityTargetAbility | MOTORDRIVE | 6 | 特性 | `AbilitiesOnHit.java` | 可转 |
| 98 | BattleHandlers_Abilities:672-677 | MoveImmunityTargetAbility | SAPSIPPER | 6 | 特性 | `AbilitiesOnHit.java` | 可转 |
| 99 | BattleHandlers_Abilities:678-692 | MoveImmunityTargetAbility | SOUNDPROOF | 15 | 动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 100 | BattleHandlers_Abilities:693-698 | MoveImmunityTargetAbility | STORMDRAIN | 6 | 特性 | `AbilitiesOnHit.java` | 可转 |
| 101 | BattleHandlers_Abilities:699-714 | MoveImmunityTargetAbility | TELEPATHY | 16 | 动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 102 | BattleHandlers_Abilities:715-720 | MoveImmunityTargetAbility | VOLTABSORB | 6 | 特性 | `AbilitiesOnHit.java` | 可转 |
| 103 | BattleHandlers_Abilities:721-726 | MoveImmunityTargetAbility | WATERABSORB | 6 | 特性 | `AbilitiesOnHit.java` | 可转 |
| 104 | BattleHandlers_Abilities:727-734 | MoveImmunityTargetAbility | ICEBSORB | 8 | 特性 | `AbilitiesOnHit.java` | 可转 |
| 105 | BattleHandlers_Abilities:735-753 | MoveImmunityTargetAbility | WONDERGUARD | 19 | 动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 106 | BattleHandlers_Abilities:1433-1462 | TargetAbilityOnHit | AFTERMATH | 30 | 动画/场景、HP、特性 | `AbilitiesOnHit.java` | 降级 |
| 107 | BattleHandlers_Abilities:1463-1480 | TargetAbilityOnHit | ANGERPOINT | 18 | 动画/场景、能力等级、特性 | `AbilitiesOnHit.java` | 降级 |
| 108 | BattleHandlers_Abilities:1481-1509 | TargetAbilityOnHit | CURSEDBODY | 29 | PBEffects、动画/场景、招式/PP、异常状态、特性 | `AbilitiesOnHit.java` | 降级 |
| 109 | BattleHandlers_Abilities:1510-1528 | TargetAbilityOnHit | CUTECHARM | 19 | 动画/场景、异常状态、特性 | `AbilitiesOnHit.java` | 降级 |
| 110 | BattleHandlers_Abilities:1529-1576 | TargetAbilityOnHit | EFFECTSPORE | 48 | 动画/场景、异常状态、特性 | `AbilitiesOnHit.java` | 降级 |
| 111 | BattleHandlers_Abilities:1577-1593 | TargetAbilityOnHit | FLAMEBODY | 17 | 动画/场景、异常状态、特性 | `AbilitiesOnHit.java` | 降级 |
| 112 | BattleHandlers_Abilities:1594-1602 | TargetAbilityOnHit | GOOEY | 9 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 113 | BattleHandlers_Abilities:1603-1613 | TargetAbilityOnHit | ILLUSION | 11 | PBEffects、图鉴、动画/场景、特性 | `AbilitiesOnHit.java` | 登记 |
| 114 | BattleHandlers_Abilities:1614-1631 | TargetAbilityOnHit | INNARDSOUT | 18 | 动画/场景、HP、特性 | `AbilitiesOnHit.java` | 降级 |
| 115 | BattleHandlers_Abilities:1632-1656 | TargetAbilityOnHit | IRONBARBS | 25 | BOSS、动画/场景、HP、特性 | `AbilitiesOnHit.java` | 登记 |
| 116 | BattleHandlers_Abilities:1657-1663 | TargetAbilityOnHit | JUSTIFIED | 7 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 117 | BattleHandlers_Abilities:1664-1696 | TargetAbilityOnHit | MUMMY | 33 | 动画/场景、道具、特性 | `AbilitiesOnHit.java` | 降级 |
| 118 | BattleHandlers_Abilities:1697-1713 | TargetAbilityOnHit | POISONPOINT | 17 | 动画/场景、异常状态、特性 | `AbilitiesOnHit.java` | 降级 |
| 119 | BattleHandlers_Abilities:1714-1722 | TargetAbilityOnHit | RATTLED | 9 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 120 | BattleHandlers_Abilities:1723-1728 | TargetAbilityOnHit | STAMINA | 6 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 121 | BattleHandlers_Abilities:1729-1734 | TargetAbilityOnHit | SANDSPIT | 6 | 天气、特性 | `AbilitiesOnHit.java` | 可转 |
| 122 | BattleHandlers_Abilities:1735-1752 | TargetAbilityOnHit | STATIC | 18 | 动画/场景、异常状态、特性 | `AbilitiesOnHit.java` | 降级 |
| 123 | BattleHandlers_Abilities:1753-1759 | TargetAbilityOnHit | WATERCOMPACTION | 7 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 124 | BattleHandlers_Abilities:1760-1772 | TargetAbilityOnHit | WEAKARMOR | 13 | 动画/场景、能力等级、特性 | `AbilitiesOnHit.java` | 降级 |
| 125 | BattleHandlers_Abilities:1773-1780 | TargetAbilityOnHit | STEAMENGINE | 8 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 126 | BattleHandlers_Abilities:1781-1816 | TargetAbilityOnHit | WANDERINGSPIRIT | 36 | 动画/场景、道具、特性 | `AbilitiesOnHit.java` | 降级 |
| 127 | BattleHandlers_Abilities:1817-1829 | TargetAbilityOnHit | PERISHBODY | 13 | PBEffects、动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 128 | BattleHandlers_Abilities:1830-1842 | TargetAbilityOnHit | COTTONDOWN | 13 | 动画/场景、能力等级、特性 | `AbilitiesOnHit.java` | 降级 |
| 129 | BattleHandlers_Abilities:1843-1868 | TargetAbilityOnHit | GULPMISSILE | 26 | 动画/场景、能力等级、异常状态、HP、特性 | `AbilitiesOnHit.java` | 降级 |
| 130 | BattleHandlers_Abilities:1869-1895 | UserAbilityOnHit | POISONTOUCH | 27 | 动画/场景、异常状态、道具、特性 | `AbilitiesOnHit.java` | 降级 |
| 131 | BattleHandlers_Abilities:1896-1917 | UserAbilityEndOfMove | BEASTBOOST | 22 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 132 | BattleHandlers_Abilities:1918-1957 | UserAbilityEndOfMove | MAGICIAN | 40 | PBEffects、动画/场景、道具、特性 | `AbilitiesOnHit.java` | 降级 |
| 133 | BattleHandlers_Abilities:1958-1969 | UserAbilityEndOfMove | MOXIE | 12 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 134 | BattleHandlers_Abilities:1970-1980 | UserAbilityEndOfMove | GRIMNEIGH | 11 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 135 | BattleHandlers_Abilities:1981-1996 | UserAbilityEndOfMove | ASONEICE | 16 | 动画/场景、能力等级、特性 | `AbilitiesOnHit.java` | 降级 |
| 136 | BattleHandlers_Abilities:1997-2016 | UserAbilityEndOfMove | ASONEGHOST | 20 | 动画/场景、能力等级、特性 | `AbilitiesOnHit.java` | 降级 |
| 137 | BattleHandlers_Abilities:2017-2025 | TargetAbilityAfterMoveUse | BERSERK | 9 | 能力等级、HP、特性 | `AbilitiesOnHit.java` | 可转 |
| 138 | BattleHandlers_Abilities:2026-2040 | TargetAbilityAfterMoveUse | COLORCHANGE | 15 | 动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 139 | BattleHandlers_Abilities:2041-2079 | TargetAbilityAfterMoveUse | PICKPOCKET | 39 | PBEffects、天气、动画/场景、道具、特性 | `AbilitiesOnHit.java` | 降级 |
| 140 | BattleHandlers_Abilities:2923-2937 | TargetAbilityOnHit | TOXICDEBRIS | 15 | PBEffects、动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 141 | BattleHandlers_Abilities:2945-2953 | TargetAbilityOnHit | SEEDSOWER | 9 | 场地、动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 142 | BattleHandlers_Abilities:2954-2960 | MoveImmunityTargetAbility | WELLBAKEDBODY | 7 | 特性 | `AbilitiesOnHit.java` | 可转 |
| 143 | BattleHandlers_Abilities:2961-2969 | TargetAbilityOnHit | THERMALEXCHANGE | 9 | 能力等级、异常状态、特性 | `AbilitiesOnHit.java` | 可转 |
| 144 | BattleHandlers_Abilities:2998-3006 | MoveImmunityTargetAbility | COMMANDER | 9 | 特性 | `AbilitiesOnHit.java` | 可转 |
| 145 | BattleHandlers_Abilities:3024-3029 | MoveImmunityTargetAbility | EARTHEATER | 6 | 特性 | `AbilitiesOnHit.java` | 可转 |
| 146 | BattleHandlers_Abilities:3030-3041 | TargetAbilityOnHit | ELECTROMORPHOSIS | 12 | PBEffects、动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 147 | BattleHandlers_Abilities:3042-3053 | TargetAbilityOnHit | WINDPOWER | 12 | PBEffects、动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 148 | BattleHandlers_Abilities:3054-3073 | MoveImmunityTargetAbility | WINDRIDER | 20 | 动画/场景、能力等级、特性 | `AbilitiesOnHit.java` | 降级 |
| 149 | BattleHandlers_Abilities:3082-3096 | MoveImmunityTargetAbility | GOODASGOLD | 15 | 动画/场景、特性 | `AbilitiesOnHit.java` | 降级 |
| 150 | BattleHandlers_Abilities:3137-3161 | UserAbilityOnHit | TOXICCHAIN | 25 | 动画/场景、异常状态、道具、特性 | `AbilitiesOnHit.java` | 降级 |
| 151 | BattleHandlers_Abilities:3256-3282 | TargetAbilityAfterMoveUse | ANGERSHELL | 27 | 动画/场景、能力等级、特性 | `AbilitiesOnHit.java` | 降级 |
| 152 | BattleHandlers_Abilities:3646-3653 | TargetAbilityOnHit | GHOSTRAMPAGE | 8 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 153 | BattleHandlers_Abilities:3788-3800 | TargetAbilityOnHit | SLEEPSOUNDLY | 13 | 动画/场景、异常状态、特性 | `AbilitiesOnHit.java` | 降级 |
| 154 | BattleHandlers_Abilities:3801-3812 | TargetAbilityOnHit | PHANTOMROCK | 12 | 动画/场景、能力等级、特性 | `AbilitiesOnHit.java` | 降级 |
| 155 | BattleHandlers_Abilities:3904-3923 | MoveImmunityTargetAbility | YSNJ | 20 | 动画/场景、能力等级、特性 | `AbilitiesOnHit.java` | 降级 |
| 156 | BattleHandlers_Abilities:3933-3943 | UserAbilityEndOfMove | MERMAIDSOUND | 11 | HP、特性 | `AbilitiesOnHit.java` | 可转 |
| 157 | BattleHandlers_Abilities:4047-4083 | UserAbilityEndOfMove | BAMBOOFEATHER | 37 | 动画/场景、HP、特性 | `AbilitiesOnHit.java` | 降级 |
| 158 | BattleHandlers_Abilities:4205-4214 | TargetAbilityAfterMoveUse | LASTSTAND | 10 | 能力等级、HP、特性 | `AbilitiesOnHit.java` | 可转 |
| 159 | BattleHandlers_Abilities:4215-4227 | UserAbilityEndOfMove | NETHERDRIVE | 13 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 160 | BattleHandlers_Abilities:4246-4255 | UserAbilityEndOfMove | GHASTLYWAIL | 10 | 能力等级、特性 | `AbilitiesOnHit.java` | 可转 |
| 161 | BattleHandlers_Abilities:4368-4395 | UserAbilityOnHit | BIYIHUANGYAN | 28 | 动画/场景、异常状态、道具、特性 | `AbilitiesOnHit.java` | 降级 |
| 162 | BattleHandlers_Abilities:4428-4450 | TargetAbilityOnHit | SACREDREBORN | 23 | 动画/场景、异常状态、HP、特性 | `AbilitiesOnHit.java` | 降级 |
| 163 | BattleHandlers_Abilities:4451-4478 | TargetAbilityOnHit | ABYSSREBORN | 28 | 动画/场景、异常状态、HP、特性 | `AbilitiesOnHit.java` | 降级 |
| 164 | BattleHandlers_Abilities:925-932 | DamageCalcUserAbility | AERILATE | 8 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 165 | BattleHandlers_Abilities:933-942 | DamageCalcUserAbility | ANALYTIC | 10 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 166 | BattleHandlers_Abilities:943-950 | DamageCalcUserAbility | BLAZE | 8 | HP、特性 | `AbilitiesDamageUser.java` | 可转 |
| 167 | BattleHandlers_Abilities:951-958 | DamageCalcUserAbility | DEFEATIST | 8 | HP、特性 | `AbilitiesDamageUser.java` | 可转 |
| 168 | BattleHandlers_Abilities:959-966 | DamageCalcUserAbility | FLAREBOOST | 8 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 169 | BattleHandlers_Abilities:967-974 | DamageCalcUserAbility | FLASHFIRE | 8 | PBEffects、特性 | `AbilitiesDamageUser.java` | 可转 |
| 170 | BattleHandlers_Abilities:975-984 | DamageCalcUserAbility | FLOWERGIFT | 10 | 天气、特性 | `AbilitiesDamageUser.java` | 可转 |
| 171 | BattleHandlers_Abilities:985-992 | DamageCalcUserAbility | GUTS | 8 | 异常状态、特性 | `AbilitiesDamageUser.java` | 可转 |
| 172 | BattleHandlers_Abilities:993-1000 | DamageCalcUserAbility | HUGEPOWER | 8 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 173 | BattleHandlers_Abilities:1001-1006 | DamageCalcUserAbility | HUSTLE | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 174 | BattleHandlers_Abilities:1007-1013 | DamageCalcUserAbility | IRONFIST | 7 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 175 | BattleHandlers_Abilities:1014-1019 | DamageCalcUserAbility | MEGALAUNCHER | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 176 | BattleHandlers_Abilities:1020-1032 | DamageCalcUserAbility | MINUS | 13 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 177 | BattleHandlers_Abilities:1033-1040 | DamageCalcUserAbility | NEUROFORCE | 8 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 178 | BattleHandlers_Abilities:1041-1048 | DamageCalcUserAbility | OVERGROW | 8 | HP、特性 | `AbilitiesDamageUser.java` | 可转 |
| 179 | BattleHandlers_Abilities:1049-1054 | DamageCalcUserAbility | RECKLESS | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 180 | BattleHandlers_Abilities:1055-1066 | DamageCalcUserAbility | RIVALRY | 12 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 181 | BattleHandlers_Abilities:1067-1077 | DamageCalcUserAbility | SANDFORCE | 11 | 天气、特性 | `AbilitiesDamageUser.java` | 可转 |
| 182 | BattleHandlers_Abilities:1078-1083 | DamageCalcUserAbility | SHEERFORCE | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 183 | BattleHandlers_Abilities:1084-1089 | DamageCalcUserAbility | SLOWSTART | 6 | PBEffects、特性 | `AbilitiesDamageUser.java` | 可转 |
| 184 | BattleHandlers_Abilities:1090-1099 | DamageCalcUserAbility | SOLARPOWER | 10 | 天气、特性 | `AbilitiesDamageUser.java` | 可转 |
| 185 | BattleHandlers_Abilities:1100-1107 | DamageCalcUserAbility | SNIPER | 8 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 186 | BattleHandlers_Abilities:1108-1113 | DamageCalcUserAbility | STAKEOUT | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 187 | BattleHandlers_Abilities:1114-1119 | DamageCalcUserAbility | STEELWORKER | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 188 | BattleHandlers_Abilities:1120-1125 | DamageCalcUserAbility | STRONGJAW | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 189 | BattleHandlers_Abilities:1126-1133 | DamageCalcUserAbility | SWARM | 8 | HP、特性 | `AbilitiesDamageUser.java` | 可转 |
| 190 | BattleHandlers_Abilities:1134-1141 | DamageCalcUserAbility | TECHNICIAN | 8 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 191 | BattleHandlers_Abilities:1142-1147 | DamageCalcUserAbility | TINTEDLENS | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 192 | BattleHandlers_Abilities:1148-1155 | DamageCalcUserAbility | TORRENT | 8 | HP、特性 | `AbilitiesDamageUser.java` | 可转 |
| 193 | BattleHandlers_Abilities:1156-1161 | DamageCalcUserAbility | TOUGHCLAWS | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 194 | BattleHandlers_Abilities:1162-1169 | DamageCalcUserAbility | TOXICBOOST | 8 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 195 | BattleHandlers_Abilities:1170-1175 | DamageCalcUserAbility | WATERBUBBLE | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 196 | BattleHandlers_Abilities:1176-1181 | DamageCalcUserAbility | GORILLATACTICS | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 197 | BattleHandlers_Abilities:1182-1187 | DamageCalcUserAbility | PUNKROCK | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 198 | BattleHandlers_Abilities:1188-1193 | DamageCalcUserAbility | STEELYSPIRIT | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 199 | BattleHandlers_Abilities:1194-1199 | DamageCalcUserAbility | DRAGONSMAW | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 200 | BattleHandlers_Abilities:1200-1209 | DamageCalcUserAbility | TRANSISTOR | 10 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 201 | BattleHandlers_Abilities:1210-1216 | DamageCalcUserAllyAbility | BATTERY | 7 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 202 | BattleHandlers_Abilities:1217-1226 | DamageCalcUserAllyAbility | FLOWERGIFT | 10 | 天气、特性 | `AbilitiesDamageUser.java` | 可转 |
| 203 | BattleHandlers_Abilities:1227-1232 | DamageCalcUserAllyAbility | POWERSPOT | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 204 | BattleHandlers_Abilities:1233-1242 | DamageCalcUserAllyAbility | STEELYSPIRIT | 10 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 205 | BattleHandlers_Abilities:2914-2922 | DamageCalcUserAbility | SHARPNESS | 9 | 异常状态、特性 | `AbilitiesDamageUser.java` | 可转 |
| 206 | BattleHandlers_Abilities:2938-2944 | DamageCalcUserAbility | ROCKYPAYLOAD | 7 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 207 | BattleHandlers_Abilities:3116-3123 | DamageCalcUserAbility | SUPREMEOVERLORD | 8 | PBEffects、特性 | `AbilitiesDamageUser.java` | 可转 |
| 208 | BattleHandlers_Abilities:3337-3345 | DamageCalcUserAbility | ORICHALCUMPULSE | 9 | 天气、特性 | `AbilitiesDamageUser.java` | 可转 |
| 209 | BattleHandlers_Abilities:3358-3367 | DamageCalcUserAbility | HADRONENGINE | 10 | 天气、特性 | `AbilitiesDamageUser.java` | 可转 |
| 210 | BattleHandlers_Abilities:3536-3546 | DamageCalcUserAbility | PROTOSYNTHESIS | 11 | PBEffects、特性 | `AbilitiesDamageUser.java` | 可转 |
| 211 | BattleHandlers_Abilities:3578-3584 | DamageCalcUserAbility | BROKENBREATH | 7 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 212 | BattleHandlers_Abilities:3585-3591 | DamageCalcUserAllyAbility | BROKENBREATH | 7 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 213 | BattleHandlers_Abilities:3615-3622 | DamageCalcUserAbility | THUNDERCLOUD | 8 | 天气、特性 | `AbilitiesDamageUser.java` | 可转 |
| 214 | BattleHandlers_Abilities:3633-3639 | DamageCalcUserAbility | EERIEBODY | 7 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 215 | BattleHandlers_Abilities:3640-3645 | DamageCalcUserAbility | ZEROSUMBODY | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 216 | BattleHandlers_Abilities:3677-3684 | DamageCalcUserAbility | FAIRYSONG | 8 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 217 | BattleHandlers_Abilities:3701-3706 | DamageCalcUserAbility | STARFISSURE | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 218 | BattleHandlers_Abilities:3767-3777 | DamageCalcUserAbility | DEMONKILLER | 11 | 异常状态、特性 | `AbilitiesDamageUser.java` | 可转 |
| 219 | BattleHandlers_Abilities:3813-3822 | DamageCalcUserAbility | ELIMINATEEVIL | 10 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 220 | BattleHandlers_Abilities:3823-3827 | DamageCalcUserAbility | SOUNDSTRIDE | 5 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 221 | BattleHandlers_Abilities:3835-3840 | DamageCalcUserAbility | RUYIBLADE | 6 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 222 | BattleHandlers_Abilities:3980-3988 | DamageCalcUserAbility | ROSEGARDEN | 9 | 场地、特性 | `AbilitiesDamageUser.java` | 可转 |
| 223 | BattleHandlers_Abilities:3997-4007 | DamageCalcUserAbility | ROSEGARDEN | 11 | 场地、特性 | `AbilitiesDamageUser.java` | 可转 |
| 224 | BattleHandlers_Abilities:4084-4099 | DamageCalcUserAbility | ANGERFLAME | 16 | 异常状态、特性 | `AbilitiesDamageUser.java` | 可转 |
| 225 | BattleHandlers_Abilities:4126-4136 | DamageCalcUserAbility | ENERGYACCU | 11 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 226 | BattleHandlers_Abilities:4137-4147 | DamageCalcUserAbility | CONFLUENCE | 11 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 227 | BattleHandlers_Abilities:4256-4266 | DamageCalcUserAbility | GHASTLYWAIL | 11 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 228 | BattleHandlers_Abilities:4275-4286 | DamageCalcUserAbility | SUPERSUN | 12 | 天气、特性 | `AbilitiesDamageUser.java` | 可转 |
| 229 | BattleHandlers_Abilities:4322-4328 | DamageCalcUserAbility | RAGEFIREBLAST | 7 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 230 | BattleHandlers_Abilities:4355-4367 | DamageCalcUserAbility | SOARINGSTAGE | 13 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 231 | BattleHandlers_Abilities:4479-4486 | DamageCalcUserAbility | PIERCINGDRILL | 8 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 232 | BattleHandlers_Abilities:4487-4493 | DamageCalcUserAbility | FIREMANE | 7 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 233 | BattleHandlers_Abilities:4538-4544 | DamageCalcUserAbility | PLAYFULHEART | 7 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 234 | BattleHandlers_Abilities:4545-4549 | DamageCalcUserAbility | WANXIANG | 5 | HP、特性 | `AbilitiesDamageUser.java` | 可转 |
| 235 | BattleHandlers_Abilities:4550-4558 | DamageCalcUserAbility | WANXIANG | 9 | 特性 | `AbilitiesDamageUser.java` | 可转 |
| 236 | BattleHandlers_Abilities:5-11 | SpeedCalcAbility | CHLOROPHYLL | 7 | 天气、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 237 | BattleHandlers_Abilities:12-17 | SpeedCalcAbility | QUICKFEET | 6 | 异常状态、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 238 | BattleHandlers_Abilities:18-24 | SpeedCalcAbility | SANDRUSH | 7 | 天气、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 239 | BattleHandlers_Abilities:25-30 | SpeedCalcAbility | SLOWSTART | 6 | PBEffects、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 240 | BattleHandlers_Abilities:31-37 | SpeedCalcAbility | SLUSHRUSH | 7 | 天气、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 241 | BattleHandlers_Abilities:38-43 | SpeedCalcAbility | SURGESURFER | 6 | 场地、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 242 | BattleHandlers_Abilities:44-50 | SpeedCalcAbility | SWIFTSWIM | 7 | 天气、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 243 | BattleHandlers_Abilities:51-60 | SpeedCalcAbility | UNBURDEN | 10 | PBEffects、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 244 | BattleHandlers_Abilities:61-66 | WeightCalcAbility | HEAVYMETAL | 6 | 特性 | `AbilitiesSpeedEor.java` | 可转 |
| 245 | BattleHandlers_Abilities:67-72 | WeightCalcAbility | LIGHTMETAL | 6 | 特性 | `AbilitiesSpeedEor.java` | 可转 |
| 246 | BattleHandlers_Abilities:73-82 | WeightCalcAbility | ADAPTARMOR | 10 | 特性 | `AbilitiesSpeedEor.java` | 可转 |
| 247 | BattleHandlers_Abilities:538-543 | PriorityChangeAbility | GALEWINGS | 6 | HP、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 248 | BattleHandlers_Abilities:544-553 | PriorityChangeAbility | PRANKSTER | 10 | PBEffects、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 249 | BattleHandlers_Abilities:554-563 | PriorityChangeAbility | TRIAGE | 10 | 特性 | `AbilitiesSpeedEor.java` | 可转 |
| 250 | BattleHandlers_Abilities:564-569 | PriorityBracketChangeAbility | STALL | 6 | 特性 | `AbilitiesSpeedEor.java` | 可转 |
| 251 | BattleHandlers_Abilities:570-579 | PriorityBracketChangeAbility | QUICKDRAW | 10 | 特性 | `AbilitiesSpeedEor.java` | 可转 |
| 252 | BattleHandlers_Abilities:580-589 | PriorityBracketUseAbility | QUICKDRAW | 10 | 特性 | `AbilitiesSpeedEor.java` | 可转 |
| 253 | BattleHandlers_Abilities:2080-2103 | EORWeatherAbility | DRYSKIN | 24 | 天气、动画/场景、HP、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 254 | BattleHandlers_Abilities:2104-2118 | EORWeatherAbility | ICEBODY | 15 | 天气、动画/场景、HP、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 255 | BattleHandlers_Abilities:2119-2133 | EORWeatherAbility | RAINDISH | 15 | 天气、动画/场景、HP、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 256 | BattleHandlers_Abilities:2134-2149 | EORWeatherAbility | SOLARPOWER | 16 | 天气、动画/场景、HP、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 257 | BattleHandlers_Abilities:2150-2176 | EORHealingAbility | HEALER | 27 | 动画/场景、异常状态、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 258 | BattleHandlers_Abilities:2177-2204 | EORHealingAbility | HYDRATION | 28 | 天气、动画/场景、异常状态、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 259 | BattleHandlers_Abilities:2205-2233 | EORHealingAbility | SHEDSKIN | 29 | 动画/场景、异常状态、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 260 | BattleHandlers_Abilities:2234-2259 | EOREffectAbility | BADDREAMS | 26 | BOSS、动画/场景、HP、特性 | `AbilitiesSpeedEor.java` | 登记 |
| 261 | BattleHandlers_Abilities:2260-2282 | EOREffectAbility | MOODY | 23 | 动画/场景、能力等级、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 262 | BattleHandlers_Abilities:2283-2292 | EOREffectAbility | SPEEDBOOST | 10 | 能力等级、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 263 | BattleHandlers_Abilities:2293-2307 | EOREffectAbility | BALLFETCH | 15 | PBEffects、动画/场景、PBDebug、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 264 | BattleHandlers_Abilities:2308-2324 | EOREffectAbility | HUNGERSWITCH | 17 | 动画/场景、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 265 | BattleHandlers_Abilities:2325-2342 | EORGainItemAbility | HARVEST | 18 | 天气、树果、动画/场景、道具、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 266 | BattleHandlers_Abilities:2343-2379 | EORGainItemAbility | PICKUP | 37 | PBEffects、动画/场景、道具、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 267 | BattleHandlers_Abilities:2907-2913 | RunFromBattleAbility | RUNAWAY | 7 | 特性 | `AbilitiesSpeedEor.java` | 可转 |
| 268 | BattleHandlers_Abilities:3208-3216 | PriorityBracketChangeAbility | MYCELIUMMIGHT | 9 | 特性 | `AbilitiesSpeedEor.java` | 可转 |
| 269 | BattleHandlers_Abilities:3306-3324 | EOREffectAbility | CUDCHEW | 19 | PBEffects、树果、动画/场景、道具、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 270 | BattleHandlers_Abilities:3558-3570 | SpeedCalcAbility | PROTOSYNTHESIS | 13 | PBEffects、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 271 | BattleHandlers_Abilities:3693-3700 | PriorityChangeAbility | FAIRYDANCE | 8 | HP、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 272 | BattleHandlers_Abilities:3724-3731 | EOREffectAbility | SEAMLESS | 8 | 能力等级、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 273 | BattleHandlers_Abilities:3739-3757 | EOREffectAbility | MOERAE | 19 | 动画/场景、能力等级、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 274 | BattleHandlers_Abilities:3828-3834 | PriorityChangeAbility | SOUNDSTRIDE | 7 | 特性 | `AbilitiesSpeedEor.java` | 可转 |
| 275 | BattleHandlers_Abilities:3879-3890 | EOREffectAbility | MAGICQUEEN | 12 | 动画/场景、异常状态、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 276 | BattleHandlers_Abilities:3891-3903 | EOREffectAbility | DEMONCLOAK | 13 | PBEffects、动画/场景、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 277 | BattleHandlers_Abilities:4157-4169 | EOREffectAbility | FLUFFYCOAT | 13 | HP、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 278 | BattleHandlers_Abilities:4181-4191 | EOREffectAbility | TMOVERLORD | 11 | 能力等级、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 279 | BattleHandlers_Abilities:4298-4308 | EOREffectAbility | ETERNALSTAR | 11 | 动画/场景、HP、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 280 | BattleHandlers_Abilities:4314-4321 | SpeedCalcAbility | BESTOWEDRAIN | 8 | 天气、特性 | `AbilitiesSpeedEor.java` | 可转 |
| 281 | BattleHandlers_Abilities:4329-4345 | EOREffectAbility | ETERNALSTAR | 17 | 动画/场景、HP、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 282 | BattleHandlers_Abilities:4396-4413 | EOREffectAbility | BIYIHUANGYAN | 18 | 动画/场景、HP、特性 | `AbilitiesSpeedEor.java` | 降级 |
| 283 | BattleHandlers_Abilities:127-137 | StatusCheckAbilityNonIgnorable | COMATOSE | 11 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 284 | BattleHandlers_Abilities:138-143 | StatusImmunityAbility | FLOWERVEIL | 6 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 285 | BattleHandlers_Abilities:144-151 | StatusImmunityAbility | IMMUNITY | 8 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 286 | BattleHandlers_Abilities:152-159 | StatusImmunityAbility | INSOMNIA | 8 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 287 | BattleHandlers_Abilities:160-167 | StatusImmunityAbility | LEAFGUARD | 8 | 天气、异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 288 | BattleHandlers_Abilities:168-173 | StatusImmunityAbility | LIMBER | 6 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 289 | BattleHandlers_Abilities:174-179 | StatusImmunityAbility | MAGMAARMOR | 6 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 290 | BattleHandlers_Abilities:180-191 | StatusImmunityAbility | WATERVEIL | 12 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 291 | BattleHandlers_Abilities:192-197 | StatusImmunityAbilityNonIgnorable | COMATOSE | 6 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 292 | BattleHandlers_Abilities:198-207 | StatusImmunityAbilityNonIgnorable | SHIELDSDOWN | 10 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 293 | BattleHandlers_Abilities:208-213 | StatusImmunityAllyAbility | FLOWERVEIL | 6 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 294 | BattleHandlers_Abilities:214-219 | StatusImmunityAllyAbility | SWEETVEIL | 6 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 295 | BattleHandlers_Abilities:220-229 | StatusImmunityAllyAbility | PASTELVEIL | 10 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 296 | BattleHandlers_Abilities:230-272 | AbilityOnStatusInflicted | SYNCHRONIZE | 43 | 动画/场景、异常状态、特性 | `AbilitiesStatus.java` | 降级 |
| 297 | BattleHandlers_Abilities:273-286 | StatusCureAbility | IMMUNITY | 14 | 动画/场景、异常状态、特性 | `AbilitiesStatus.java` | 降级 |
| 298 | BattleHandlers_Abilities:287-300 | StatusCureAbility | INSOMNIA | 14 | 动画/场景、异常状态、特性 | `AbilitiesStatus.java` | 降级 |
| 299 | BattleHandlers_Abilities:301-312 | StatusCureAbility | LIMBER | 12 | 动画/场景、异常状态、特性 | `AbilitiesStatus.java` | 降级 |
| 300 | BattleHandlers_Abilities:313-324 | StatusCureAbility | MAGMAARMOR | 12 | 动画/场景、异常状态、特性 | `AbilitiesStatus.java` | 降级 |
| 301 | BattleHandlers_Abilities:325-351 | StatusCureAbility | OBLIVIOUS | 27 | PBEffects、动画/场景、异常状态、特性 | `AbilitiesStatus.java` | 降级 |
| 302 | BattleHandlers_Abilities:352-366 | StatusCureAbility | OWNTEMPO | 15 | PBEffects、动画/场景、异常状态、特性 | `AbilitiesStatus.java` | 降级 |
| 303 | BattleHandlers_Abilities:367-384 | StatusCureAbility | WATERVEIL | 18 | 动画/场景、异常状态、特性 | `AbilitiesStatus.java` | 降级 |
| 304 | BattleHandlers_Abilities:385-401 | StatLossImmunityAbility | BIGPECKS | 17 | 动画/场景、特性 | `AbilitiesStatus.java` | 降级 |
| 305 | BattleHandlers_Abilities:402-418 | StatLossImmunityAbility | CLEARBODY | 17 | 动画/场景、特性 | `AbilitiesStatus.java` | 降级 |
| 306 | BattleHandlers_Abilities:419-434 | StatLossImmunityAbility | FLOWERVEIL | 16 | 动画/场景、特性 | `AbilitiesStatus.java` | 降级 |
| 307 | BattleHandlers_Abilities:435-451 | StatLossImmunityAbility | HYPERCUTTER | 17 | 动画/场景、特性 | `AbilitiesStatus.java` | 降级 |
| 308 | BattleHandlers_Abilities:452-473 | StatLossImmunityAbility | KEENEYE | 22 | 动画/场景、特性 | `AbilitiesStatus.java` | 降级 |
| 309 | BattleHandlers_Abilities:474-492 | StatLossImmunityAbilityNonIgnorable | FULLMETALBODY | 19 | 动画/场景、特性 | `AbilitiesStatus.java` | 降级 |
| 310 | BattleHandlers_Abilities:493-519 | StatLossImmunityAllyAbility | FLOWERVEIL | 27 | 动画/场景、特性 | `AbilitiesStatus.java` | 降级 |
| 311 | BattleHandlers_Abilities:520-526 | AbilityOnStatLoss | COMPETITIVE | 7 | 能力等级、特性 | `AbilitiesStatus.java` | 可转 |
| 312 | BattleHandlers_Abilities:527-537 | AbilityOnStatLoss | DEFIANT | 11 | 能力等级、特性 | `AbilitiesStatus.java` | 可转 |
| 313 | BattleHandlers_Abilities:3217-3244 | AbilityOnOpposingStatGain | OPPORTUNIST | 28 | 动画/场景、能力等级、道具、特性 | `AbilitiesStatus.java` | 降级 |
| 314 | BattleHandlers_Abilities:3245-3249 | StatusImmunityAbility | PURIFYINGSALT | 5 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 315 | BattleHandlers_Abilities:3379-3388 | AbilityOnMoveSuccessCheck | TERASHELL | 10 | 动画/场景、特性 | `AbilitiesStatus.java` | 降级 |
| 316 | BattleHandlers_Abilities:3447-3458 | AbilityOnInflictingStatus | POISONPUPPETEER | 12 | PBEffects、动画/场景、异常状态、特性 | `AbilitiesStatus.java` | 降级 |
| 317 | BattleHandlers_Abilities:3841-3856 | StatLossImmunityAbility | STEELDYNASTY | 16 | 动画/场景、特性 | `AbilitiesStatus.java` | 降级 |
| 318 | BattleHandlers_Abilities:4117-4125 | StatusImmunityAbility | RAINCURTAIN | 9 | 天气、异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 319 | BattleHandlers_Abilities:4414-4427 | StatusCureAbility | BIYIHUANGYAN | 14 | 天气、动画/场景、异常状态、特性 | `AbilitiesStatus.java` | 降级 |
| 320 | BattleHandlers_Abilities:4533-4537 | StatusImmunityAbility | PLAYFULHEART | 5 | 异常状态、特性 | `AbilitiesStatus.java` | 可转 |
| 321 | BattleHandlers_Abilities:754-761 | MoveBaseTypeModifierAbility | AERILATE | 8 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 322 | BattleHandlers_Abilities:762-769 | MoveBaseTypeModifierAbility | GALVANIZE | 8 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 323 | BattleHandlers_Abilities:770-775 | MoveBaseTypeModifierAbility | LIQUIDVOICE | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 324 | BattleHandlers_Abilities:776-783 | MoveBaseTypeModifierAbility | NORMALIZE | 8 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 325 | BattleHandlers_Abilities:784-791 | MoveBaseTypeModifierAbility | PIXILATE | 8 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 326 | BattleHandlers_Abilities:792-800 | MoveBaseTypeModifierAbility | REFRIGERATE | 9 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 327 | BattleHandlers_Abilities:801-811 | MoveBaseTypeModifierAbility | DRAGONSKIN | 11 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 328 | BattleHandlers_Abilities:812-817 | AccuracyCalcUserAbility | COMPOUNDEYES | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 329 | BattleHandlers_Abilities:818-823 | AccuracyCalcUserAbility | HUSTLE | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 330 | BattleHandlers_Abilities:824-829 | AccuracyCalcUserAbility | KEENEYE | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 331 | BattleHandlers_Abilities:830-834 | AccuracyCalcUserAbility | ROSYAEGIS | 5 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 332 | BattleHandlers_Abilities:835-840 | AccuracyCalcUserAbility | NOGUARD | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 333 | BattleHandlers_Abilities:841-846 | AccuracyCalcUserAbility | UNAWARE | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 334 | BattleHandlers_Abilities:847-856 | AccuracyCalcUserAbility | VICTORYSTAR | 10 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 335 | BattleHandlers_Abilities:857-866 | AccuracyCalcUserAllyAbility | VICTORYSTAR | 10 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 336 | BattleHandlers_Abilities:867-872 | AccuracyCalcTargetAbility | LIGHTNINGROD | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 337 | BattleHandlers_Abilities:873-878 | AccuracyCalcTargetAbility | NOGUARD | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 338 | BattleHandlers_Abilities:879-886 | AccuracyCalcTargetAbility | SANDVEIL | 8 | 天气、特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 339 | BattleHandlers_Abilities:887-894 | AccuracyCalcTargetAbility | SNOWCLOAK | 8 | 天气、特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 340 | BattleHandlers_Abilities:895-900 | AccuracyCalcTargetAbility | STORMDRAIN | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 341 | BattleHandlers_Abilities:901-906 | AccuracyCalcTargetAbility | TANGLEDFEET | 6 | PBEffects、特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 342 | BattleHandlers_Abilities:907-912 | AccuracyCalcTargetAbility | UNAWARE | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 343 | BattleHandlers_Abilities:913-924 | AccuracyCalcTargetAbility | WONDERSKIN | 12 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 344 | BattleHandlers_Abilities:1405-1410 | CriticalCalcUserAbility | MERCILESS | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 345 | BattleHandlers_Abilities:1411-1420 | CriticalCalcUserAbility | SUPERLUCK | 10 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 346 | BattleHandlers_Abilities:1421-1432 | CriticalCalcTargetAbility | BATTLEARMOR | 12 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 347 | BattleHandlers_Abilities:3671-3676 | MoveBaseTypeModifierAbility | FAIRYSONG | 6 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 348 | BattleHandlers_Abilities:3685-3692 | MoveBaseTypeModifierAbility | FAIRYDANCE | 8 | HP、特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 349 | BattleHandlers_Abilities:3732-3738 | CriticalCalcUserAbility | BEIMINGBLADE | 7 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 350 | BattleHandlers_Abilities:3924-3932 | MoveBaseTypeModifierAbility | MERMAIDSOUND | 9 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 351 | BattleHandlers_Abilities:4287-4297 | AccuracyCalcUserAbility | SUPERSUN | 11 | 天气、特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 352 | BattleHandlers_Abilities:4346-4354 | MoveBaseTypeModifierAbility | SOARINGSTAGE | 9 | 特性 | `AbilitiesAccuracyCritType.java` | 可转 |
| 353 | BattleHandlers_Abilities:1243-1250 | DamageCalcTargetAbility | DRYSKIN | 8 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 354 | BattleHandlers_Abilities:1251-1259 | DamageCalcTargetAbility | FILTER | 9 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 355 | BattleHandlers_Abilities:1260-1267 | DamageCalcTargetAbility | ADAPTARMOR | 8 | PBEffects、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 356 | BattleHandlers_Abilities:1268-1278 | DamageCalcTargetAbility | ETRTNALIGHT | 11 | PBEffects、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 357 | BattleHandlers_Abilities:1279-1288 | DamageCalcTargetAbility | FLOWERGIFT | 10 | 天气、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 358 | BattleHandlers_Abilities:1289-1295 | DamageCalcTargetAbility | FLUFFY | 7 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 359 | BattleHandlers_Abilities:1296-1302 | DamageCalcTargetAbility | FURCOAT | 7 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 360 | BattleHandlers_Abilities:1303-1308 | DamageCalcTargetAbility | ICESCALES | 6 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 361 | BattleHandlers_Abilities:1309-1316 | DamageCalcTargetAbility | GRASSPELT | 8 | 场地、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 362 | BattleHandlers_Abilities:1317-1322 | DamageCalcTargetAbility | HEATPROOF | 6 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 363 | BattleHandlers_Abilities:1323-1330 | DamageCalcTargetAbility | MARVELSCALE | 8 | 异常状态、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 364 | BattleHandlers_Abilities:1331-1338 | DamageCalcTargetAbility | MULTISCALE | 8 | HP、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 365 | BattleHandlers_Abilities:1339-1346 | DamageCalcTargetAbility | THICKFAT | 8 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 366 | BattleHandlers_Abilities:1347-1354 | DamageCalcTargetAbility | WATERBUBBLE | 8 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 367 | BattleHandlers_Abilities:1355-1364 | DamageCalcTargetAbility | PUNKROCK | 10 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 368 | BattleHandlers_Abilities:1365-1372 | DamageCalcTargetAbilityNonIgnorable | PRISMARMOR | 8 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 369 | BattleHandlers_Abilities:1373-1384 | DamageCalcTargetAbilityNonIgnorable | SHADOWSHIELD | 12 | HP、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 370 | BattleHandlers_Abilities:1385-1394 | DamageCalcTargetAllyAbility | FLOWERGIFT | 10 | 天气、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 371 | BattleHandlers_Abilities:1395-1404 | DamageCalcTargetAllyAbility | FRIENDGUARD | 10 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 372 | BattleHandlers_Abilities:3250-3255 | DamageCalcTargetAbility | PURIFYINGSALT | 6 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 373 | BattleHandlers_Abilities:3547-3557 | DamageCalcTargetAbility | PROTOSYNTHESIS | 11 | PBEffects、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 374 | BattleHandlers_Abilities:3592-3598 | DamageCalcTargetAbility | BROKENBREATH | 7 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 375 | BattleHandlers_Abilities:3599-3605 | DamageCalcTargetAllyAbility | BROKENBREATH | 7 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 376 | BattleHandlers_Abilities:4017-4025 | DamageCalcTargetAbility | VERDANTWARD | 9 | 场地、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 377 | BattleHandlers_Abilities:4100-4116 | DamageCalcTargetAbility | RAINCURTAIN | 17 | 天气、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 378 | BattleHandlers_Abilities:4148-4156 | DamageCalcTargetAbility | FLUFFYCOAT | 9 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 379 | BattleHandlers_Abilities:4564-4572 | DamageCalcTargetAbility | STORMEYE | 9 | 特性 | `AbilitiesDamageTarget.java` | 可转 |
| 380 | BattleHandlers_Abilities:4578-4585 | DamageCalcTargetAbility | RAINBOWARCH | 8 | 天气、特性 | `AbilitiesDamageTarget.java` | 可转 |
| 381 | BattleHandlers_Items:468-476 | DamageCalcUserItem | ADAMANTORB | 9 | — | `ItemsDamageUser.java` | 可转 |
| 382 | BattleHandlers_Items:477-484 | DamageCalcUserItem | BLACKBELT | 8 | — | `ItemsDamageUser.java` | 可转 |
| 383 | BattleHandlers_Items:485-492 | DamageCalcUserItem | BLACKGLASSES | 8 | — | `ItemsDamageUser.java` | 可转 |
| 384 | BattleHandlers_Items:493-498 | DamageCalcUserItem | BUGGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 385 | BattleHandlers_Items:499-506 | DamageCalcUserItem | CHARCOAL | 8 | — | `ItemsDamageUser.java` | 可转 |
| 386 | BattleHandlers_Items:507-512 | DamageCalcUserItem | CHOICEBAND | 6 | — | `ItemsDamageUser.java` | 可转 |
| 387 | BattleHandlers_Items:513-518 | DamageCalcUserItem | CHOICESPECS | 6 | — | `ItemsDamageUser.java` | 可转 |
| 388 | BattleHandlers_Items:519-524 | DamageCalcUserItem | DARKGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 389 | BattleHandlers_Items:525-532 | DamageCalcUserItem | DEEPSEATOOTH | 8 | — | `ItemsDamageUser.java` | 可转 |
| 390 | BattleHandlers_Items:533-540 | DamageCalcUserItem | DRAGONFANG | 8 | — | `ItemsDamageUser.java` | 可转 |
| 391 | BattleHandlers_Items:541-546 | DamageCalcUserItem | DRAGONGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 392 | BattleHandlers_Items:547-552 | DamageCalcUserItem | ELECTRICGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 393 | BattleHandlers_Items:553-560 | DamageCalcUserItem | EXPERTBELT | 8 | — | `ItemsDamageUser.java` | 可转 |
| 394 | BattleHandlers_Items:561-566 | DamageCalcUserItem | FAIRYGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 395 | BattleHandlers_Items:567-572 | DamageCalcUserItem | FIGHTINGGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 396 | BattleHandlers_Items:573-578 | DamageCalcUserItem | FIREGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 397 | BattleHandlers_Items:579-584 | DamageCalcUserItem | FLYINGGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 398 | BattleHandlers_Items:585-590 | DamageCalcUserItem | GHOSTGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 399 | BattleHandlers_Items:591-596 | DamageCalcUserItem | GRASSGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 400 | BattleHandlers_Items:597-605 | DamageCalcUserItem | GRISEOUSORB | 9 | — | `ItemsDamageUser.java` | 可转 |
| 401 | BattleHandlers_Items:606-611 | DamageCalcUserItem | GROUNDGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 402 | BattleHandlers_Items:612-619 | DamageCalcUserItem | HARDSTONE | 8 | — | `ItemsDamageUser.java` | 可转 |
| 403 | BattleHandlers_Items:620-625 | DamageCalcUserItem | ICEGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 404 | BattleHandlers_Items:626-633 | DamageCalcUserItem | LIFEORB | 8 | — | `ItemsDamageUser.java` | 可转 |
| 405 | BattleHandlers_Items:634-641 | DamageCalcUserItem | LIGHTBALL | 8 | — | `ItemsDamageUser.java` | 可转 |
| 406 | BattleHandlers_Items:642-650 | DamageCalcUserItem | LUSTROUSORB | 9 | — | `ItemsDamageUser.java` | 可转 |
| 407 | BattleHandlers_Items:651-658 | DamageCalcUserItem | MAGNET | 8 | — | `ItemsDamageUser.java` | 可转 |
| 408 | BattleHandlers_Items:659-666 | DamageCalcUserItem | METALCOAT | 8 | — | `ItemsDamageUser.java` | 可转 |
| 409 | BattleHandlers_Items:667-673 | DamageCalcUserItem | METRONOME | 7 | PBEffects | `ItemsDamageUser.java` | 可转 |
| 410 | BattleHandlers_Items:674-681 | DamageCalcUserItem | MIRACLESEED | 8 | — | `ItemsDamageUser.java` | 可转 |
| 411 | BattleHandlers_Items:682-687 | DamageCalcUserItem | MUSCLEBAND | 6 | — | `ItemsDamageUser.java` | 可转 |
| 412 | BattleHandlers_Items:688-695 | DamageCalcUserItem | MYSTICWATER | 8 | — | `ItemsDamageUser.java` | 可转 |
| 413 | BattleHandlers_Items:696-703 | DamageCalcUserItem | NEVERMELTICE | 8 | — | `ItemsDamageUser.java` | 可转 |
| 414 | BattleHandlers_Items:704-709 | DamageCalcUserItem | NORMALGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 415 | BattleHandlers_Items:710-715 | DamageCalcUserItem | PIXIEPLATE | 6 | — | `ItemsDamageUser.java` | 可转 |
| 416 | BattleHandlers_Items:716-723 | DamageCalcUserItem | POISONBARB | 8 | — | `ItemsDamageUser.java` | 可转 |
| 417 | BattleHandlers_Items:724-729 | DamageCalcUserItem | POISONGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 418 | BattleHandlers_Items:730-735 | DamageCalcUserItem | PSYCHICGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 419 | BattleHandlers_Items:736-741 | DamageCalcUserItem | ROCKGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 420 | BattleHandlers_Items:742-749 | DamageCalcUserItem | SHARPBEAK | 8 | — | `ItemsDamageUser.java` | 可转 |
| 421 | BattleHandlers_Items:750-757 | DamageCalcUserItem | SILKSCARF | 8 | — | `ItemsDamageUser.java` | 可转 |
| 422 | BattleHandlers_Items:758-765 | DamageCalcUserItem | SILVERPOWDER | 8 | — | `ItemsDamageUser.java` | 可转 |
| 423 | BattleHandlers_Items:766-773 | DamageCalcUserItem | SOFTSAND | 8 | — | `ItemsDamageUser.java` | 可转 |
| 424 | BattleHandlers_Items:774-788 | DamageCalcUserItem | SOULDEW | 15 | — | `ItemsDamageUser.java` | 可转 |
| 425 | BattleHandlers_Items:789-796 | DamageCalcUserItem | SPELLTAG | 8 | — | `ItemsDamageUser.java` | 可转 |
| 426 | BattleHandlers_Items:797-802 | DamageCalcUserItem | STEELGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 427 | BattleHandlers_Items:803-810 | DamageCalcUserItem | THICKCLUB | 8 | — | `ItemsDamageUser.java` | 可转 |
| 428 | BattleHandlers_Items:811-818 | DamageCalcUserItem | TWISTEDSPOON | 8 | — | `ItemsDamageUser.java` | 可转 |
| 429 | BattleHandlers_Items:819-824 | DamageCalcUserItem | WATERGEM | 6 | — | `ItemsDamageUser.java` | 可转 |
| 430 | BattleHandlers_Items:825-831 | DamageCalcUserItem | WISEGLASSES | 7 | — | `ItemsDamageUser.java` | 可转 |
| 431 | BattleHandlers_Items:832-836 | DamageCalcUserItem | WELLSPRINGMASK | 5 | — | `ItemsDamageUser.java` | 可转 |
| 432 | BattleHandlers_Items:837-841 | DamageCalcUserItem | HEARTHFLAMEMASK | 5 | — | `ItemsDamageUser.java` | 可转 |
| 433 | BattleHandlers_Items:842-853 | DamageCalcUserItem | CORNERSTONEMASK | 12 | — | `ItemsDamageUser.java` | 可转 |
| 434 | BattleHandlers_Items:1719-1724 | DamageCalcUserItem | ABYSSSWORD | 6 | — | `ItemsDamageUser.java` | 可转 |
| 435 | BattleHandlers_Items:1725-1730 | DamageCalcUserItem | TEMPLESCEPTER | 6 | — | `ItemsDamageUser.java` | 可转 |
| 436 | BattleHandlers_Items:1731-1736 | DamageCalcUserItem | WMDCX | 6 | — | `ItemsDamageUser.java` | 可转 |
| 437 | BattleHandlers_Items:1737-1742 | DamageCalcUserItem | SANCTFEATHER | 6 | — | `ItemsDamageUser.java` | 可转 |
| 438 | BattleHandlers_Items:1743-1748 | DamageCalcUserItem | RADIANTSHARD | 6 | — | `ItemsDamageUser.java` | 可转 |
| 439 | BattleHandlers_Items:1757-1763 | DamageCalcUserItem | HOLYCREST | 7 | — | `ItemsDamageUser.java` | 可转 |
| 440 | BattleHandlers_Items:1764-1770 | DamageCalcUserItem | CRAFTMIND | 7 | — | `ItemsDamageUser.java` | 可转 |
| 441 | BattleHandlers_Items:1799-1804 | DamageCalcUserItem | PUNCHINGGLOVE | 6 | — | `ItemsDamageUser.java` | 可转 |
| 442 | BattleHandlers_Items:427-432 | AccuracyCalcUserItem | WIDELENS | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 443 | BattleHandlers_Items:433-441 | AccuracyCalcUserItem | CRAFTMIND | 9 | — | `ItemsDamageTarget.java` | 可转 |
| 444 | BattleHandlers_Items:442-455 | AccuracyCalcUserItem | ZOOMLENS | 14 | — | `ItemsDamageTarget.java` | 可转 |
| 445 | BattleHandlers_Items:456-467 | AccuracyCalcTargetItem | BRIGHTPOWDER | 12 | — | `ItemsDamageTarget.java` | 可转 |
| 446 | BattleHandlers_Items:854-859 | DamageCalcTargetItem | ASSAULTVEST | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 447 | BattleHandlers_Items:860-865 | DamageCalcTargetItem | BABIRIBERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 448 | BattleHandlers_Items:866-871 | DamageCalcTargetItem | CHARTIBERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 449 | BattleHandlers_Items:872-877 | DamageCalcTargetItem | CHILANBERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 450 | BattleHandlers_Items:878-883 | DamageCalcTargetItem | CHOPLEBERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 451 | BattleHandlers_Items:884-889 | DamageCalcTargetItem | COBABERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 452 | BattleHandlers_Items:890-895 | DamageCalcTargetItem | COLBURBERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 453 | BattleHandlers_Items:896-903 | DamageCalcTargetItem | DEEPSEASCALE | 8 | — | `ItemsDamageTarget.java` | 可转 |
| 454 | BattleHandlers_Items:904-914 | DamageCalcTargetItem | EVIOLITE | 11 | — | `ItemsDamageTarget.java` | 可转 |
| 455 | BattleHandlers_Items:915-920 | DamageCalcTargetItem | HABANBERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 456 | BattleHandlers_Items:921-926 | DamageCalcTargetItem | KASIBBERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 457 | BattleHandlers_Items:927-932 | DamageCalcTargetItem | KEBIABERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 458 | BattleHandlers_Items:933-940 | DamageCalcTargetItem | METALPOWDER | 8 | PBEffects | `ItemsDamageTarget.java` | 可转 |
| 459 | BattleHandlers_Items:941-946 | DamageCalcTargetItem | OCCABERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 460 | BattleHandlers_Items:947-952 | DamageCalcTargetItem | PASSHOBERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 461 | BattleHandlers_Items:953-958 | DamageCalcTargetItem | PAYAPABERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 462 | BattleHandlers_Items:959-964 | DamageCalcTargetItem | RINDOBERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 463 | BattleHandlers_Items:965-970 | DamageCalcTargetItem | ROSELIBERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 464 | BattleHandlers_Items:971-976 | DamageCalcTargetItem | SHUCABERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 465 | BattleHandlers_Items:977-986 | DamageCalcTargetItem | SOULDEW | 10 | — | `ItemsDamageTarget.java` | 可转 |
| 466 | BattleHandlers_Items:987-992 | DamageCalcTargetItem | TANGABERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 467 | BattleHandlers_Items:993-998 | DamageCalcTargetItem | WACANBERRY | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 468 | BattleHandlers_Items:999-1008 | DamageCalcTargetItem | YACHEBERRY | 10 | — | `ItemsDamageTarget.java` | 可转 |
| 469 | BattleHandlers_Items:1009-1014 | CriticalCalcUserItem | LUCKYPUNCH | 6 | — | `ItemsDamageTarget.java` | 可转 |
| 470 | BattleHandlers_Items:1015-1022 | CriticalCalcUserItem | RAZORCLAW | 8 | — | `ItemsDamageTarget.java` | 可转 |
| 471 | BattleHandlers_Items:1023-1038 | CriticalCalcUserItem | LEEK | 16 | — | `ItemsDamageTarget.java` | 可转 |
| 472 | BattleHandlers_Items:1749-1756 | DamageCalcTargetItem | SQHL | 8 | PBEffects | `ItemsDamageTarget.java` | 可转 |
| 473 | BattleHandlers_Items:48-54 | HPHealItem | AGUAVBERRY | 7 | — | `ItemsHealStatus.java` | 可转 |
| 474 | BattleHandlers_Items:55-60 | HPHealItem | APICOTBERRY | 6 | — | `ItemsHealStatus.java` | 可转 |
| 475 | BattleHandlers_Items:61-77 | HPHealItem | BERRYJUICE | 17 | 动画/场景、PBDebug、HP | `ItemsHealStatus.java` | 降级 |
| 476 | BattleHandlers_Items:78-84 | HPHealItem | FIGYBERRY | 7 | — | `ItemsHealStatus.java` | 可转 |
| 477 | BattleHandlers_Items:85-90 | HPHealItem | GANLONBERRY | 6 | — | `ItemsHealStatus.java` | 可转 |
| 478 | BattleHandlers_Items:91-97 | HPHealItem | IAPAPABERRY | 7 | — | `ItemsHealStatus.java` | 可转 |
| 479 | BattleHandlers_Items:98-113 | HPHealItem | LANSATBERRY | 16 | PBEffects、动画/场景 | `ItemsHealStatus.java` | 降级 |
| 480 | BattleHandlers_Items:114-119 | HPHealItem | LIECHIBERRY | 6 | — | `ItemsHealStatus.java` | 可转 |
| 481 | BattleHandlers_Items:120-126 | HPHealItem | MAGOBERRY | 7 | — | `ItemsHealStatus.java` | 可转 |
| 482 | BattleHandlers_Items:127-144 | HPHealItem | MICLEBERRY | 18 | PBEffects、动画/场景、PBDebug | `ItemsHealStatus.java` | 降级 |
| 483 | BattleHandlers_Items:145-166 | HPHealItem | ORANBERRY | 22 | 动画/场景、PBDebug、HP、特性 | `ItemsHealStatus.java` | 降级 |
| 484 | BattleHandlers_Items:167-172 | HPHealItem | PETAYABERRY | 6 | — | `ItemsHealStatus.java` | 可转 |
| 485 | BattleHandlers_Items:173-178 | HPHealItem | SALACBERRY | 6 | — | `ItemsHealStatus.java` | 可转 |
| 486 | BattleHandlers_Items:179-200 | HPHealItem | SITRUSBERRY | 22 | 动画/场景、PBDebug、HP、特性 | `ItemsHealStatus.java` | 降级 |
| 487 | BattleHandlers_Items:201-214 | HPHealItem | STARFBERRY | 14 | 能力等级、特性 | `ItemsHealStatus.java` | 可转 |
| 488 | BattleHandlers_Items:215-225 | HPHealItem | WIKIBERRY | 11 | 异常状态 | `ItemsHealStatus.java` | 可转 |
| 489 | BattleHandlers_Items:226-238 | StatusCureItem | ASPEARBERRY | 13 | 动画/场景、PBDebug、异常状态 | `ItemsHealStatus.java` | 降级 |
| 490 | BattleHandlers_Items:239-251 | StatusCureItem | CHERIBERRY | 13 | 动画/场景、PBDebug、异常状态 | `ItemsHealStatus.java` | 降级 |
| 491 | BattleHandlers_Items:252-264 | StatusCureItem | CHESTOBERRY | 13 | 动画/场景、PBDebug、异常状态 | `ItemsHealStatus.java` | 降级 |
| 492 | BattleHandlers_Items:265-299 | StatusCureItem | LUMBERRY | 35 | PBEffects、动画/场景、PBDebug、异常状态 | `ItemsHealStatus.java` | 降级 |
| 493 | BattleHandlers_Items:300-334 | StatusCureItem | MENTALHERB | 35 | PBEffects、动画/场景、PBDebug、异常状态 | `ItemsHealStatus.java` | 降级 |
| 494 | BattleHandlers_Items:335-347 | StatusCureItem | PECHABERRY | 13 | 动画/场景、PBDebug、异常状态 | `ItemsHealStatus.java` | 降级 |
| 495 | BattleHandlers_Items:348-365 | StatusCureItem | PERSIMBERRY | 18 | PBEffects、动画/场景、PBDebug、异常状态 | `ItemsHealStatus.java` | 降级 |
| 496 | BattleHandlers_Items:366-382 | StatusCureItem | RAWSTBERRY | 17 | 动画/场景、PBDebug、异常状态 | `ItemsHealStatus.java` | 降级 |
| 497 | BattleHandlers_Items:1377-1409 | EndOfMoveItem | LEPPABERRY | 33 | 动画/场景、PBDebug、招式/PP | `ItemsHealStatus.java` | 降级 |
| 498 | BattleHandlers_Items:1410-1436 | EndOfMoveStatRestoreItem | WHITEHERB | 27 | 动画/场景、PBDebug | `ItemsHealStatus.java` | 降级 |
| 499 | BattleHandlers_Items:1437-1446 | ExpGainModifierItem | LUCKYEGG | 10 | — | `ItemsHealStatus.java` | 可转 |
| 500 | BattleHandlers_Items:1447-1452 | EVGainModifierItem | MACHOBRACE | 6 | — | `ItemsHealStatus.java` | 可转 |
| 501 | BattleHandlers_Items:1453-1458 | EVGainModifierItem | POWERANKLET | 6 | — | `ItemsHealStatus.java` | 可转 |
| 502 | BattleHandlers_Items:1459-1464 | EVGainModifierItem | POWERBAND | 6 | — | `ItemsHealStatus.java` | 可转 |
| 503 | BattleHandlers_Items:1465-1470 | EVGainModifierItem | POWERBELT | 6 | — | `ItemsHealStatus.java` | 可转 |
| 504 | BattleHandlers_Items:1471-1476 | EVGainModifierItem | POWERBRACER | 6 | — | `ItemsHealStatus.java` | 可转 |
| 505 | BattleHandlers_Items:1477-1482 | EVGainModifierItem | POWERLENS | 6 | — | `ItemsHealStatus.java` | 可转 |
| 506 | BattleHandlers_Items:1483-1492 | EVGainModifierItem | POWERWEIGHT | 10 | 天气 | `ItemsHealStatus.java` | 可转 |
| 507 | BattleHandlers_Items:1678-1690 | ItemOnIntimidated | ADRENALINEORB | 13 | 动画/场景、能力等级 | `ItemsHealStatus.java` | 降级 |
| 508 | BattleHandlers_Items:1702-1718 | ItemOnStatLoss | EJECTPACK | 17 | 换人、动画/场景、道具 | `ItemsHealStatus.java` | 登记 |
| 509 | BattleHandlers_Items:1771-1779 | StatLossImmunityItem | CLEARAMULET | 9 | — | `ItemsHealStatus.java` | 可转 |
| 510 | BattleHandlers_Items:1780-1798 | ItemOnOpposingStatGain | MIRRORHERB | 19 | 动画/场景、能力等级 | `ItemsHealStatus.java` | 降级 |
| 511 | BattleHandlers_Items:1039-1048 | TargetItemOnHit | ABSORBBULB | 10 | 动画/场景、能力等级、道具 | `ItemsOnHit.java` | 降级 |
| 512 | BattleHandlers_Items:1049-1056 | TargetItemOnHit | AIRBALLOON | 8 | 道具 | `ItemsOnHit.java` | 可转 |
| 513 | BattleHandlers_Items:1057-1066 | TargetItemOnHit | CELLBATTERY | 10 | 动画/场景、能力等级、道具 | `ItemsOnHit.java` | 降级 |
| 514 | BattleHandlers_Items:1067-1076 | TargetItemOnHit | ENIGMABERRY | 10 | 道具 | `ItemsOnHit.java` | 可转 |
| 515 | BattleHandlers_Items:1077-1100 | TargetItemOnHit | JABOCABERRY | 24 | 动画/场景、HP、道具、特性 | `ItemsOnHit.java` | 降级 |
| 516 | BattleHandlers_Items:1101-1109 | TargetItemOnHit | KEEBERRY | 9 | 道具 | `ItemsOnHit.java` | 可转 |
| 517 | BattleHandlers_Items:1110-1124 | TargetItemOnHit | LUMINOUSMOSS | 15 | 动画/场景、能力等级、道具 | `ItemsOnHit.java` | 降级 |
| 518 | BattleHandlers_Items:1125-1133 | TargetItemOnHit | MARANGABERRY | 9 | 道具 | `ItemsOnHit.java` | 可转 |
| 519 | BattleHandlers_Items:1134-1147 | TargetItemOnHit | ROCKYHELMET | 14 | BOSS、动画/场景、HP | `ItemsOnHit.java` | 登记 |
| 520 | BattleHandlers_Items:1148-1166 | TargetItemOnHit | ROWAPBERRY | 19 | 动画/场景、HP、道具、特性 | `ItemsOnHit.java` | 降级 |
| 521 | BattleHandlers_Items:1167-1176 | TargetItemOnHit | SNOWBALL | 10 | 动画/场景、能力等级、道具 | `ItemsOnHit.java` | 降级 |
| 522 | BattleHandlers_Items:1177-1194 | TargetItemOnHit | STICKYBARB | 18 | PBEffects | `ItemsOnHit.java` | 可转 |
| 523 | BattleHandlers_Items:1195-1219 | TargetItemOnHit | WEAKNESSPOLICY | 25 | 动画/场景、能力等级、道具 | `ItemsOnHit.java` | 降级 |
| 524 | BattleHandlers_Items:1220-1241 | TargetItemOnHitPositiveBerry | ENIGMABERRY | 22 | 动画/场景、PBDebug、HP、特性 | `ItemsOnHit.java` | 降级 |
| 525 | BattleHandlers_Items:1242-1259 | TargetItemOnHitPositiveBerry | KEEBERRY | 18 | 动画/场景、PBDebug、能力等级、特性 | `ItemsOnHit.java` | 降级 |
| 526 | BattleHandlers_Items:1260-1281 | TargetItemOnHitPositiveBerry | MARANGABERRY | 22 | 动画/场景、PBDebug、能力等级、特性 | `ItemsOnHit.java` | 降级 |
| 527 | BattleHandlers_Items:1282-1296 | TargetItemAfterMoveUse | EJECTBUTTON | 15 | 换人、动画/场景、道具 | `ItemsOnHit.java` | 登记 |
| 528 | BattleHandlers_Items:1297-1331 | TargetItemAfterMoveUse | REDCARD | 35 | PBEffects、换人、动画/场景、道具、特性 | `ItemsOnHit.java` | 登记 |
| 529 | BattleHandlers_Items:1332-1349 | UserItemAfterMoveUse | LIFEORB | 18 | PBDebug、HP | `ItemsOnHit.java` | 降级 |
| 530 | BattleHandlers_Items:1350-1361 | UserItemAfterMoveUse | SHELLBELL | 12 | HP | `ItemsOnHit.java` | 可转 |
| 531 | BattleHandlers_Items:1362-1376 | UserItemAfterMoveUse | THROATSPRAY | 15 | 动画/场景、能力等级、道具 | `ItemsOnHit.java` | 降级 |
| 532 | BattleHandlers_Items:5-10 | SpeedCalcItem | CHOICESCARF | 6 | — | `ItemsFieldSpeed.java` | 可转 |
| 533 | BattleHandlers_Items:11-20 | SpeedCalcItem | MACHOBRACE | 10 | — | `ItemsFieldSpeed.java` | 可转 |
| 534 | BattleHandlers_Items:21-27 | SpeedCalcItem | QUICKPOWDER | 7 | PBEffects | `ItemsFieldSpeed.java` | 可转 |
| 535 | BattleHandlers_Items:28-37 | SpeedCalcItem | IRONBALL | 10 | — | `ItemsFieldSpeed.java` | 可转 |
| 536 | BattleHandlers_Items:38-47 | WeightCalcItem | FLOATSTONE | 10 | — | `ItemsFieldSpeed.java` | 可转 |
| 537 | BattleHandlers_Items:383-389 | PriorityBracketChangeItem | CUSTAPBERRY | 7 | — | `ItemsFieldSpeed.java` | 可转 |
| 538 | BattleHandlers_Items:390-397 | PriorityBracketChangeItem | LAGGINGTAIL | 8 | — | `ItemsFieldSpeed.java` | 可转 |
| 539 | BattleHandlers_Items:398-407 | PriorityBracketChangeItem | QUICKCLAW | 10 | — | `ItemsFieldSpeed.java` | 可转 |
| 540 | BattleHandlers_Items:408-415 | PriorityBracketUseItem | CUSTAPBERRY | 8 | 动画/场景、道具 | `ItemsFieldSpeed.java` | 降级 |
| 541 | BattleHandlers_Items:416-426 | PriorityBracketUseItem | QUICKCLAW | 11 | 动画/场景 | `ItemsFieldSpeed.java` | 降级 |
| 542 | BattleHandlers_Items:1493-1498 | WeatherExtenderItem | DAMPROCK | 6 | 天气 | `ItemsFieldSpeed.java` | 可转 |
| 543 | BattleHandlers_Items:1499-1504 | WeatherExtenderItem | HEATROCK | 6 | 天气 | `ItemsFieldSpeed.java` | 可转 |
| 544 | BattleHandlers_Items:1505-1510 | WeatherExtenderItem | ICYROCK | 6 | 天气 | `ItemsFieldSpeed.java` | 可转 |
| 545 | BattleHandlers_Items:1511-1520 | WeatherExtenderItem | SMOOTHROCK | 10 | 天气、场地 | `ItemsFieldSpeed.java` | 可转 |
| 546 | BattleHandlers_Items:1521-1530 | TerrainExtenderItem | TERRAINEXTENDER | 10 | 场地 | `ItemsFieldSpeed.java` | 可转 |
| 547 | BattleHandlers_Items:1531-1540 | TerrainStatBoostItem | ELECTRICSEED | 10 | 场地、动画/场景、能力等级 | `ItemsFieldSpeed.java` | 降级 |
| 548 | BattleHandlers_Items:1541-1550 | TerrainStatBoostItem | GRASSYSEED | 10 | 场地、动画/场景、能力等级 | `ItemsFieldSpeed.java` | 降级 |
| 549 | BattleHandlers_Items:1551-1560 | TerrainStatBoostItem | MISTYSEED | 10 | 场地、动画/场景、能力等级 | `ItemsFieldSpeed.java` | 降级 |
| 550 | BattleHandlers_Items:1561-1574 | TerrainStatBoostItem | PSYCHICSEED | 14 | 场地、动画/场景、能力等级 | `ItemsFieldSpeed.java` | 降级 |
| 551 | BattleHandlers_Items:1575-1594 | EORHealingItem | BLACKSLUDGE | 20 | 动画/场景、HP | `ItemsFieldSpeed.java` | 降级 |
| 552 | BattleHandlers_Items:1595-1608 | EORHealingItem | LEFTOVERS | 14 | 动画/场景、HP | `ItemsFieldSpeed.java` | 降级 |
| 553 | BattleHandlers_Items:1609-1615 | EOREffectItem | FLAMEORB | 7 | 异常状态 | `ItemsFieldSpeed.java` | 可转 |
| 554 | BattleHandlers_Items:1616-1628 | EOREffectItem | STICKYBARB | 13 | 动画/场景、HP | `ItemsFieldSpeed.java` | 降级 |
| 555 | BattleHandlers_Items:1629-1640 | EOREffectItem | TOXICORB | 12 | 异常状态 | `ItemsFieldSpeed.java` | 可转 |
| 556 | BattleHandlers_Items:1641-1657 | CertainSwitchingUserItem | SHEDSHELL | 17 | — | `ItemsFieldSpeed.java` | 可转 |
| 557 | BattleHandlers_Items:1658-1664 | ItemOnSwitchIn | AIRBALLOON | 7 | — | `ItemsFieldSpeed.java` | 可转 |
| 558 | BattleHandlers_Items:1665-1677 | ItemOnSwitchIn | ROOMSERVICE | 13 | PBEffects、能力等级、道具 | `ItemsFieldSpeed.java` | 可转 |
| 559 | BattleHandlers_Items:1691-1701 | RunFromBattleItem | SMOKEBALL | 11 | — | `ItemsFieldSpeed.java` | 可转 |

**主表连续性校验：发出 559 行，期望 559 行 ✓**

## 3. 重复注册检查（同一 `组 + 符号` 被 add 两次）

| 组 | 符号 | 次数 | 行号 |
|---|---|---:|---|
| AbilityOnSwitchIn | BROKENBREATH | 2 | 3571, 3606 |
| DamageCalcUserAbility | ROSEGARDEN | 2 | 3980, 3997 |
| EOREffectAbility | ETERNALSTAR | 2 | 4298, 4329 |
| DamageCalcUserAbility | WANXIANG | 2 | 4545, 4550 |

## 4. 条目里调用了「插件自身缺陷」的（对应 `stage4-m0b-battler-api-notes.md` §E.4 / §F.1）

| 主表序号 | 缺陷 | 命中的插件原文行 | 条目 | 条目起-止行 | 会因此编译不过？ |
|---:|---|---|---|---|---|
| 45 | D2 dynamax?（全工程无定义） | BattleHandlers_Abilities:2973, BattleHandlers_Abilities:2980 | AbilityOnSwitchIn / COMMANDER | BattleHandlers_Abilities:2970-2997 | 否（原文用 `defined?` 保护；两处分支都要照抄） |
| 380 | D5 Battler 上的 pbWeather（Battle#pbWeather 才是合法的） | BattleHandlers_Abilities:4581 | DamageCalcTargetAbility / RAINBOWARCH | BattleHandlers_Abilities:4578-4585 | **是** —— BattleHandlers_Abilities:4581 |
| 486 | D1 pbRecoverHP?（多一个 ?） | BattleHandlers_Items:186 | HPHealItem / SITRUSBERRY | BattleHandlers_Items:179-200 | **是** —— BattleHandlers_Items:186 |
| 528 | D4 Battle::Scene:: 命名空间不存在 | BattleHandlers_Items:1309 | TargetItemAfterMoveUse / REDCARD | BattleHandlers_Items:1297-1331 | **是** —— BattleHandlers_Items:1309 |
| 528 | D2 dynamax?（全工程无定义） | BattleHandlers_Items:1317 | TargetItemAfterMoveUse / REDCARD | BattleHandlers_Items:1297-1331 | **是** —— BattleHandlers_Items:1317 |
| 510 | D3 mirrorHerbUsed=（无 attr_accessor） | BattleHandlers_Items:1783, BattleHandlers_Items:1790 | ItemOnOpposingStatGain / MIRRORHERB | BattleHandlers_Items:1780-1798 | **是** —— BattleHandlers_Items:1783, BattleHandlers_Items:1790 |

处置原则（Lead 已定）：**一律照抄原样 + 编译期不可达处用注释登记，绝不自己造替代实现。**

> 说明：M0b §E.4 另外 3 条缺陷（`item_to_use` 未定义、`@ability_id` 未定义、`Battler_Statuses:240` 的未定义 `user`）
> 都在 `Battler_*` 段的方法体里，**不在本批任何一个 handler 条目内**，故不出现在上表。
>
> `D2 dynamax?` 出现两次：`Abilities:2973/2980` 包在 `defined?(battler.dynamax?)` 里（**编译得过的写法就是照抄这个 `defined?` 判断**），
> 而 `Items:1317` 的 `if user.dynamax?` 没有任何保护 → 该分支照抄必编译不过。

## 5. `.copy(...)` 行（不单独成条目，但同样是注册）

`HandlerHash#copy(src,*dests)`（`Event_Handlers.rb:115-122`）= 取出 `src` 已注册的 handler，逐个 `add` 到 `dests`。

| 序号 | 段:行 | 组 | 源符号 | 被复制的符号 | 建议 Java 文件 |
|---:|---|---|---|---|---|
| C1 | BattleHandlers_Abilities:121 | AbilityOnHPDroppedBelowHalf | EMERGENCYEXIT | WIMPOUT | `AbilitiesSwitchIn.java` |
| C2 | BattleHandlers_Abilities:150 | StatusImmunityAbility | IMMUNITY | PASTELVEIL | `AbilitiesStatus.java` |
| C3 | BattleHandlers_Abilities:158 | StatusImmunityAbility | INSOMNIA | SWEETVEIL, VITALSPIRIT | `AbilitiesStatus.java` |
| C4 | BattleHandlers_Abilities:186 | StatusImmunityAbility | WATERVEIL | WATERBUBBLE | `AbilitiesStatus.java` |
| C5 | BattleHandlers_Abilities:285 | StatusCureAbility | IMMUNITY | PASTELVEIL | `AbilitiesStatus.java` |
| C6 | BattleHandlers_Abilities:299 | StatusCureAbility | INSOMNIA | VITALSPIRIT | `AbilitiesStatus.java` |
| C7 | BattleHandlers_Abilities:379 | StatusCureAbility | WATERVEIL | WATERBUBBLE, DEEPSEAFASCINATION | `AbilitiesStatus.java` |
| C8 | BattleHandlers_Abilities:417 | StatLossImmunityAbility | CLEARBODY | WHITESMOKE, NOBLESTRIKE, ETERNALSTAR, TRANSLUCENTGHOST | `AbilitiesStatus.java` |
| C9 | BattleHandlers_Abilities:468 | StatLossImmunityAbility | KEENEYE | ROSYAEGIS | `AbilitiesStatus.java` |
| C10 | BattleHandlers_Abilities:613 | MoveBlockingAbility | DAZZLING | QUEENLYMAJESTY | `AbilitiesOnHit.java` |
| C11 | BattleHandlers_Abilities:733 | MoveImmunityTargetAbility | WATERABSORB | DRYSKIN | `AbilitiesOnHit.java` |
| C12 | BattleHandlers_Abilities:931 | DamageCalcUserAbility | AERILATE | PIXILATE, REFRIGERATE, GALVANIZE, DRAGONSKIN | `AbilitiesDamageUser.java` |
| C13 | BattleHandlers_Abilities:999 | DamageCalcUserAbility | HUGEPOWER | PUREPOWER, SAVAGECEREMONY | `AbilitiesDamageUser.java` |
| C14 | BattleHandlers_Abilities:1012 | DamageCalcUserAbility | IRONFIST | SHATTERFIST | `AbilitiesDamageUser.java` |
| C15 | BattleHandlers_Abilities:1031 | DamageCalcUserAbility | MINUS | PLUS | `AbilitiesDamageUser.java` |
| C16 | BattleHandlers_Abilities:1277 | DamageCalcTargetAbility | FILTER | SOLIDROCK | `AbilitiesDamageTarget.java` |
| C17 | BattleHandlers_Abilities:1427 | CriticalCalcTargetAbility | BATTLEARMOR | SHELLARMOR, NOBLESTRIKE, RUYIBLADE | `AbilitiesAccuracyCritType.java` |
| C18 | BattleHandlers_Abilities:1601 | TargetAbilityOnHit | GOOEY | TANGLINGHAIR | `AbilitiesOnHit.java` |
| C19 | BattleHandlers_Abilities:1655 | TargetAbilityOnHit | IRONBARBS | ROUGHSKIN | `AbilitiesOnHit.java` |
| C20 | BattleHandlers_Abilities:1968 | UserAbilityEndOfMove | MOXIE | CHILLINGNEIGH, FEARLESS, DRAGONSOULCRY | `AbilitiesOnHit.java` |
| C21 | BattleHandlers_Abilities:2413 | AbilityOnSwitchIn | AIRLOCK | CLOUDNINE | `AbilitiesSwitchIn.java` |
| C22 | BattleHandlers_Abilities:2653 | DamageCalcTargetAbility | MOLDBREAKER | SHATTERFIST | `AbilitiesDamageTarget.java` |
| C23 | BattleHandlers_Abilities:2662 | DamageCalcTargetAbility | PRESSURE | CALAMITYAERIAL | `AbilitiesDamageTarget.java` |
| C24 | BattleHandlers_Abilities:2729 | StatusImmunityAbility | UNNERVE | CONFESSIONLIST | `AbilitiesStatus.java` |
| C25 | BattleHandlers_Abilities:2741 | AbilityOnSwitchIn | ASONEICE | ASONEGHOST | `AbilitiesSwitchIn.java` |
| C26 | BattleHandlers_Abilities:2891 | AbilityChangeOnBattlerFainting | POWEROFALCHEMY | RECEIVER | `AbilitiesSwitchIn.java` |
| C27 | BattleHandlers_Abilities:2920 | StatusImmunityAbility | SHARPNESS | PSYCHICEDGE, ENDLESSDARKN, NETHERDRIVE | `AbilitiesStatus.java` |
| C28 | BattleHandlers_Abilities:2967 | StatusImmunityAbility | WATERVEIL | WATERBUBBLE, THERMALEXCHANGE | `AbilitiesStatus.java` |
| C29 | BattleHandlers_Abilities:3112 | StatLossImmunityAbility | KEENEYE | MINDSEYE | `AbilitiesStatus.java` |
| C30 | BattleHandlers_Abilities:3113 | AccuracyCalcUserAbility | KEENEYE | MINDSEYE | `AbilitiesAccuracyCritType.java` |
| C31 | BattleHandlers_Abilities:3134 | DamageCalcTargetAbility | SUPREMEOVERLORD | TMOVERLORD, SPOVERLORD, DSOVERLORD | `AbilitiesDamageTarget.java` |
| C32 | BattleHandlers_Abilities:3205 | TargetAbilityOnHit | MUMMY | LINGERINGAROMA | `AbilitiesOnHit.java` |
| C33 | BattleHandlers_Abilities:3280 | MoveBlockingAbility | DAZZLING | QUEENLYMAJESTY, ARMORTAIL | `AbilitiesOnHit.java` |
| C34 | BattleHandlers_Abilities:3475 | AbilityOnSwitchIn | TABLETSOFRUIN | SWORDOFRUIN, VESSELOFRUIN, BEADSOFRUIN, TURBOBLAZE, TERAVOLT, CALAMITYAERIAL | `AbilitiesSwitchIn.java` |
| C35 | BattleHandlers_Abilities:3527 | AbilityOnSwitchIn | PROTOSYNTHESIS | QUARKDRIVE | `AbilitiesSwitchIn.java` |
| C36 | BattleHandlers_Abilities:3544 | DamageCalcUserAbility | PROTOSYNTHESIS | QUARKDRIVE | `AbilitiesDamageUser.java` |
| C37 | BattleHandlers_Abilities:3555 | DamageCalcTargetAbility | PROTOSYNTHESIS | QUARKDRIVE | `AbilitiesDamageTarget.java` |
| C38 | BattleHandlers_Abilities:3564 | SpeedCalcAbility | PROTOSYNTHESIS | QUARKDRIVE | `AbilitiesSpeedEor.java` |
| C39 | BattleHandlers_Abilities:3721 | DamageCalcTargetAbility | ETRTNALIGHT | STARFISSURE | `AbilitiesDamageTarget.java` |
| C40 | BattleHandlers_Abilities:3773 | StatusCureAbility | OWNTEMPO | DEMONKILLER | `AbilitiesStatus.java` |
| C41 | BattleHandlers_Items:17 | SpeedCalcItem | MACHOBRACE | POWERANKLET, POWERBAND, POWERBELT, POWERBRACER, POWERLENS, POWERWEIGHT | `ItemsFieldSpeed.java` |
| C42 | BattleHandlers_Items:396 | PriorityBracketChangeItem | LAGGINGTAIL | FULLINCENSE | `ItemsFieldSpeed.java` |
| C43 | BattleHandlers_Items:462 | AccuracyCalcTargetItem | BRIGHTPOWDER | LAXINCENSE | `ItemsDamageTarget.java` |
| C44 | BattleHandlers_Items:483 | DamageCalcUserItem | BLACKBELT | FISTPLATE | `ItemsDamageUser.java` |
| C45 | BattleHandlers_Items:491 | DamageCalcUserItem | BLACKGLASSES | DREADPLATE | `ItemsDamageUser.java` |
| C46 | BattleHandlers_Items:505 | DamageCalcUserItem | CHARCOAL | FLAMEPLATE | `ItemsDamageUser.java` |
| C47 | BattleHandlers_Items:539 | DamageCalcUserItem | DRAGONFANG | DRACOPLATE | `ItemsDamageUser.java` |
| C48 | BattleHandlers_Items:618 | DamageCalcUserItem | HARDSTONE | STONEPLATE, ROCKINCENSE | `ItemsDamageUser.java` |
| C49 | BattleHandlers_Items:657 | DamageCalcUserItem | MAGNET | ZAPPLATE | `ItemsDamageUser.java` |
| C50 | BattleHandlers_Items:665 | DamageCalcUserItem | METALCOAT | IRONPLATE | `ItemsDamageUser.java` |
| C51 | BattleHandlers_Items:680 | DamageCalcUserItem | MIRACLESEED | MEADOWPLATE, ROSEINCENSE | `ItemsDamageUser.java` |
| C52 | BattleHandlers_Items:694 | DamageCalcUserItem | MYSTICWATER | SPLASHPLATE, SEAINCENSE, WAVEINCENSE | `ItemsDamageUser.java` |
| C53 | BattleHandlers_Items:702 | DamageCalcUserItem | NEVERMELTICE | ICICLEPLATE | `ItemsDamageUser.java` |
| C54 | BattleHandlers_Items:722 | DamageCalcUserItem | POISONBARB | TOXICPLATE | `ItemsDamageUser.java` |
| C55 | BattleHandlers_Items:748 | DamageCalcUserItem | SHARPBEAK | SKYPLATE | `ItemsDamageUser.java` |
| C56 | BattleHandlers_Items:755 | DamageCalcUserItem | SILKSCARF | BLANKPLATE | `ItemsDamageUser.java` |
| C57 | BattleHandlers_Items:764 | DamageCalcUserItem | SILVERPOWDER | INSECTPLATE | `ItemsDamageUser.java` |
| C58 | BattleHandlers_Items:772 | DamageCalcUserItem | SOFTSAND | EARTHPLATE | `ItemsDamageUser.java` |
| C59 | BattleHandlers_Items:795 | DamageCalcUserItem | SPELLTAG | SPOOKYPLATE | `ItemsDamageUser.java` |
| C60 | BattleHandlers_Items:817 | DamageCalcUserItem | TWISTEDSPOON | MINDPLATE, ODDINCENSE | `ItemsDamageUser.java` |
| C61 | BattleHandlers_Items:1021 | CriticalCalcUserItem | RAZORCLAW | SCOPELENS | `ItemsDamageTarget.java` |

> 合计 **61** 条 `copy` 行，展开后新增 **95** 个符号注册。
> 所以每个建议文件的**真实注册符号数 = add/addIf 条目数 + 该文件承担的 copy 目标符号数**，见 §6。

## 6. 每个建议 Java 文件的真实注册符号数（条目 + copy 目标）

| 建议 Java 文件 | add/addIf 条目 | copy 目标符号 | 真实注册符号数 | 条目行数 |
|---|---:|---:|---:|---:|
| `AbilitiesSwitchIn.java` | 91 | 11 | **102** | 1174 |
| `AbilitiesOnHit.java` | 72 | 10 | **82** | 1190 |
| `AbilitiesDamageUser.java` | 72 | 9 | **81** | 590 |
| `AbilitiesSpeedEor.java` | 47 | 1 | **48** | 623 |
| `AbilitiesStatus.java` | 38 | 21 | **59** | 510 |
| `AbilitiesAccuracyCritType.java` | 32 | 4 | **36** | 249 |
| `AbilitiesDamageTarget.java` | 28 | 8 | **36** | 245 |
| `ItemsDamageUser.java` | 61 | 22 | **83** | 436 |
| `ItemsDamageTarget.java` | 31 | 2 | **33** | 234 |
| `ItemsHealStatus.java` | 38 | 0 | **38** | 509 |
| `ItemsOnHit.java` | 21 | 0 | **21** | 338 |
| `ItemsFieldSpeed.java` | 28 | 7 | **35** | 283 |
| **合计** | **559** | **95** | **654** | **6381** |

## 7. 依赖未建模子系统的条目（= 全部「登记」条目）

| 子系统 | 条目数 | 条目（`#序号 组/符号 @起-止行`） |
|---|---:|---|
| 换人 | 4 | #1 AbilityOnHPDroppedBelowHalf/EMERGENCYEXIT @BattleHandlers_Abilities:83-126<br>#527 TargetItemAfterMoveUse/EJECTBUTTON @BattleHandlers_Items:1282-1296<br>#528 TargetItemAfterMoveUse/REDCARD @BattleHandlers_Items:1297-1331<br>#508 ItemOnStatLoss/EJECTPACK @BattleHandlers_Items:1702-1718 |
| BOSS | 3 | #115 TargetAbilityOnHit/IRONBARBS @BattleHandlers_Abilities:1632-1656<br>#260 EOREffectAbility/BADDREAMS @BattleHandlers_Abilities:2234-2259<br>#519 TargetItemOnHit/ROCKYHELMET @BattleHandlers_Items:1134-1147 |
| 训练家 | 1 | #1 AbilityOnHPDroppedBelowHalf/EMERGENCYEXIT @BattleHandlers_Abilities:83-126 |
| 图鉴 | 1 | #113 TargetAbilityOnHit/ILLUSION @BattleHandlers_Abilities:1603-1613 |
| **登记条目总数（去重后）** | **8** | 同一条目可命中多个子系统，故上表各行之和 ≥ 此数 |

> **`BOSS` 这一格的降级条件**：3 条 `BOSS` 命中全部来自 `pokemon.battleRank > 2`（`IRONBARBS` `Abilities:1639`、
> `BADDREAMS` `Abilities:2241`、`ROCKYHELMET` `Items:1139` 附近），而 `battleRank` 只是 `PokeBattle_Pokemon` 上的一个整数
> （`BOSS_HP_RANK = [1,1,2,5,10,15,15,20]` 在 `PokeBattle_BOSS:3`）。**若 Lead 决定把 `battleRank` 当普通 int 字段建模，
> 这 3 条立刻从「登记」变「可转」**，`登记` 总数从 8 降到 5。

对照：**降级**条目（只碰纯表现子系统）共 **182** 条 —— 动画/场景 179；PBDebug 21。这些条目逻辑可完整转译，只需把演出/日志调用做成空实现并注释登记。

### 7.1 「登记」在建议文件里的分布（决定切片时谁背风险）

| 建议 Java 文件 | 登记 | 降级 | 可转 | 合计 |
|---|---:|---:|---:|---:|
| `AbilitiesSwitchIn.java` | 1 | 64 | 26 | 91 |
| `AbilitiesOnHit.java` | 2 | 41 | 29 | 72 |
| `AbilitiesDamageUser.java` | 0 | 0 | 72 | 72 |
| `AbilitiesSpeedEor.java` | 1 | 19 | 27 | 47 |
| `AbilitiesStatus.java` | 0 | 20 | 18 | 38 |
| `AbilitiesAccuracyCritType.java` | 0 | 0 | 32 | 32 |
| `AbilitiesDamageTarget.java` | 0 | 0 | 28 | 28 |
| `ItemsDamageUser.java` | 0 | 0 | 61 | 61 |
| `ItemsDamageTarget.java` | 0 | 0 | 31 | 31 |
| `ItemsHealStatus.java` | 1 | 17 | 20 | 38 |
| `ItemsOnHit.java` | 3 | 12 | 6 | 21 |
| `ItemsFieldSpeed.java` | 0 | 9 | 19 | 28 |

## 8. 落点与切分要点（供 Lead 切写权限）

1. **注册表本体**：`HandlerHash`（`Event_Handlers.rb:68-146`）。`add` 把同一个 handler 同时登记到**数值 ID** 与**符号**两个键（`:109-113`）；
   `trigger` 调 handler 时**第一个实参是数值 ID**（`:140` `handler.call(fromSymbol(sym),*args)`），不是符号 —— Java 侧 proc 端口必须保持这个签名约定。
2. **`copy` 必须跨条目解析**：`copy` 的语义是「取 src 的 handler 再 add」，所以 Java 侧每个 `组` 需要一张 `Map<符号, handler>`，`copy` 展开成多次 put，**不能**把 dest 符号当成独立的 proc 再写一遍。
3. **同一符号在多组出现是正常的**（如 `IMMUNITY` 同时在 `StatusImmunityAbility` 与 `StatusCureAbility`），Java 侧不能合并成一张表。
4. **`BattleHandlers.rb` 里声明但本批两个文件 0 条注册的组**：见 **§9.1**（6 个，其中 0 个靠 §9 的边界外注册才有 handler）。这些组在 Java 侧仍应保留空表，
   否则 `Battle#pbCanSwitch?` / `pbCanRaiseStatStage?` 等调用点拿到的不是 `nil`（Ruby 里 handler 缺失时 `trigger` 返回 `nil`，`triggerXxx` 再还原为默认值）。
5. 建议切片顺序（先易后难）：`ItemsDamageUser` → `ItemsDamageTarget` → `ItemsFieldSpeed` → `ItemsOnHit` → `ItemsHealStatus` →
   `AbilitiesDamageUser` / `AbilitiesDamageTarget` / `AbilitiesAccuracyCritType` → `AbilitiesSpeedEor` → `AbilitiesStatus` → `AbilitiesOnHit` → `AbilitiesSwitchIn`（最大，且碰换人/图鉴）。

## 9. 边界之外：同一批 BattleHandlers 组还被另外两个段注册（**Lead 请先拍板**）

脚本同时扫了全工程 636 条 `BattleHandlers::<组>.<add|addIf|copy>(` 注册行，发现 **559 条在指定的两个文件里**，
**另有 16 条在别的段里**（这两个段都在 `--list` 里排在 `BattleHandlers_*` **之后**，
而 `HandlerHash#add` 是 `@hash[id] = handler` 的直接覆盖（`Event_Handlers.rb:110-113`），
**所以这些注册会覆盖主表里的同名条目**）：

| 段:行 | 组 | 符号 | 是否覆盖主表已有条目 | 被覆盖的主表条目 |
|---|---|---|---|---|
| Arceus:775 | StatusImmunityAbility | INSOMNIA | **是（覆盖）** | #286 `BattleHandlers_Abilities:152-159` 可转 |
| Arceus:782 | StatusImmunityAllyAbility | SWEETVEIL | **是（覆盖）** | #294 `BattleHandlers_Abilities:214-219` 可转 |
| Arceus:790 | StatusImmunityAbility | MAGMAARMOR | **是（覆盖）** | #289 `BattleHandlers_Abilities:174-179` 可转 |
| Arceus:798 | StatusCureAbility | INSOMNIA | **是（覆盖）** | #298 `BattleHandlers_Abilities:287-300` 降级 |
| Arceus:810 | AbilityOnStatusInflicted | SYNCHRONIZE | **是（覆盖）** | #296 `BattleHandlers_Abilities:230-272` 降级 |
| Arceus:871 | EORHealingAbility | HEALER | **是（覆盖）** | #257 `BattleHandlers_Abilities:2150-2176` 降级 |
| Arceus:902 | EORHealingAbility | HYDRATION | **是（覆盖）** | #258 `BattleHandlers_Abilities:2177-2204` 降级 |
| Arceus:934 | EORHealingAbility | SHEDSKIN | **是（覆盖）** | #259 `BattleHandlers_Abilities:2205-2233` 降级 |
| Arceus:970 | StatusCureItem | CHESTOBERRY | **是（覆盖）** | #491 `BattleHandlers_Items:252-264` 降级 |
| Arceus:983 | StatusCureItem | LUMBERRY | **是（覆盖）** | #492 `BattleHandlers_Items:265-299` 降级 |
| 场地:597 | TerrainStatBoostItem | BUGLURESEED | 否（新增） | — |
| 场地:614 | TerrainStatBoostItem | COLDSEED | 否（新增） | — |
| 场地:707 | DamageCalcTargetAbility | FROZENARMOR | 否（新增） | — |
| 场地:720 | PriorityChangeAbility | TRAPTRICK | 否（新增） | — |
| 场地:735 | AbilityOnSwitchIn | BUGLURESURGE | 否（新增） | — |
| 场地:751 | AbilityOnSwitchIn | COLDSURGE | 否（新增） | — |

> 16 条里 **10 条是覆盖**、6 条是新增。
> **本批主表按 Lead 的定义只覆盖 `BattleHandlers_Abilities` / `BattleHandlers_Items` 两个文件**，
> 这 16 条既不在主表、也不在上面的切片小计里 —— 若 L1' 直接照主表建表，这些符号会缺失（覆盖的那 10 条更会走错 handler）。
> 建议：要么给它们开一个 L1'' 小批（各自的建议 Java 文件按同组归并），要么把它们并入上表对应文件、由同一批负责。

### 9.1 `BattleHandlers.rb` 声明但本批两个文件 0 条注册的组

`BattleHandlers.rb` 共声明 **86** 行 = **85** 个唯一组名
（`ItemOnOpposingStatGain` 被声明了两次：`BattleHandlers.rb:28` 与 `:92`，都是 `ItemHandlerHash.new` —— 后者覆盖前者；
本工程里两次声明之间没有任何注册，所以没有 handler 被丢掉，但 Java 侧只应建一张表）。
本批两个文件覆盖了其中 **79** 个；
剩 **6** 个声明了但本批 0 条注册（Java 侧仍应保留空表，Ruby 里 handler 缺失时
`trigger` 返回 `nil`，`triggerXxx` 再还原默认值，语义不能省）：

| 组 | 声明行 | 是否在边界外的段里被注册 |
|---|---:|---|
| StatGainImmunityAbility | BattleHandlers.rb:21 | 否 |
| AbilityOnStatGain | BattleHandlers.rb:25 | 否 |
| AbilityModifyTypeEffectiveness | BattleHandlers.rb:27 | 否 |
| CriticalCalcTargetItem | BattleHandlers.rb:61 | 否 |
| CertainSwitchingUserAbility | BattleHandlers.rb:90 | 否 |
| TrappingTargetItem | BattleHandlers.rb:95 | 否 |

> 其中 **0** 个空组靠 §9 的边界外注册才有 handler。

另有 **0** 个组**被注册但 `BattleHandlers.rb` 里没有声明**（脚本对 `BattleHandlers.rb` 全文按 `Name = (Ability|Item)HandlerHash.new` 提取到 86 个声明）：

无。

## 10. 分析脚本（本文件由它生成）

```js
// l1-roster.mjs — read-only analyser for the L1' handler roster (task: M0 / L1' 花名册).
// Run: node l1-roster.mjs    -> writes pokemon-builder/docs/stage4-l1-handler-roster.md
// The script embeds its own source into the generated document, so re-running regenerates
// the document byte-for-byte. It only READS the plugin export and writes that ONE file.
import fs from "node:fs";

const ROOT = "E:/仓库/范例/929";
const REF = ROOT + "/__r20-ref/ruby/";
const OUT = ROOT + "/pokemon-builder/docs/stage4-l1-handler-roster.md";
const SELF = process.argv[1];

const FILES = [
  { key: "Abilities", rb: "BattleHandlers_Abilities.rb", section: "BattleHandlers_Abilities", total: 4585 },
  { key: "Items", rb: "BattleHandlers_Items.rb", section: "BattleHandlers_Items", total: 1804 },
];

// 边界之外：另外两个段也往同一批 BattleHandlers 组里注册（Lead 的本批定义只含上面两个文件）。
const EXTRA = [
  { key: "Arceus", rb: "__r20-ref/m0b/Arceus.txt", section: "Arceus", total: 1024, abs: true },
  { key: "FieldTerrain", rb: "__r20-ref/m0b/FieldTerrain.txt", section: "场地", total: 762, abs: true },
];

// BattleHandlers.rb 里声明的全部组（用于算「声明了但 0 条注册」的空组）
const DECLARED = (() => {
  const txt = fs.readFileSync(REF + "BattleHandlers.rb", "utf8").replace(/^\uFEFF/, "").split(/\r?\n/);
  const out = [];
  for (const line of txt) {
    const m = /^\s*(\d+) \| \s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(?:Ability|Item)HandlerHash\.new/.exec(line);
    if (m) out.push({ name: m[2], line: Number(m[1]) });
  }
  return out;
})();

// ---- 建议 Java 文件：按组归并成 7 个（特性）+ 5 个（持有物） ----------------
const PLAN = {
  Abilities: [
    ["AbilitiesSwitchIn.java", ["AbilityOnSwitchIn", "AbilityOnSwitchOut", "AbilityOnTerrainChange", "AbilityChangeOnBattlerFainting", "AbilityOnBattlerFainting", "TrappingTargetAbility", "AbilityOnHPDroppedBelowHalf"]],
    ["AbilitiesOnHit.java", ["TargetAbilityOnHit", "UserAbilityOnHit", "TargetAbilityAfterMoveUse", "UserAbilityEndOfMove", "MoveBlockingAbility", "MoveImmunityTargetAbility", "AbilityOnFlinch"]],
    ["AbilitiesDamageUser.java", ["DamageCalcUserAbility", "DamageCalcUserAllyAbility"]],
    ["AbilitiesSpeedEor.java", ["SpeedCalcAbility", "WeightCalcAbility", "PriorityChangeAbility", "PriorityBracketChangeAbility", "PriorityBracketUseAbility", "RunFromBattleAbility", "EOREffectAbility", "EORGainItemAbility", "EORHealingAbility", "EORWeatherAbility"]],
    ["AbilitiesStatus.java", ["StatusCheckAbilityNonIgnorable", "StatusImmunityAbility", "StatusImmunityAbilityNonIgnorable", "StatusImmunityAllyAbility", "AbilityOnStatusInflicted", "AbilityOnInflictingStatus", "StatusCureAbility", "StatLossImmunityAbility", "StatLossImmunityAbilityNonIgnorable", "StatLossImmunityAllyAbility", "AbilityOnStatLoss", "AbilityOnOpposingStatGain", "AbilityOnMoveSuccessCheck"]],
    ["AbilitiesAccuracyCritType.java", ["AccuracyCalcUserAbility", "AccuracyCalcUserAllyAbility", "AccuracyCalcTargetAbility", "CriticalCalcUserAbility", "CriticalCalcTargetAbility", "MoveBaseTypeModifierAbility"]],
    ["AbilitiesDamageTarget.java", ["DamageCalcTargetAbility", "DamageCalcTargetAbilityNonIgnorable", "DamageCalcTargetAllyAbility"]],
  ],
  Items: [
    ["ItemsDamageUser.java", ["DamageCalcUserItem"]],
    ["ItemsDamageTarget.java", ["DamageCalcTargetItem", "CriticalCalcUserItem", "AccuracyCalcUserItem", "AccuracyCalcTargetItem"]],
    ["ItemsHealStatus.java", ["HPHealItem", "StatusCureItem", "EVGainModifierItem", "ExpGainModifierItem", "EndOfMoveItem", "EndOfMoveStatRestoreItem", "StatLossImmunityItem", "ItemOnStatLoss", "ItemOnOpposingStatGain", "ItemOnIntimidated"]],
    ["ItemsOnHit.java", ["TargetItemOnHit", "TargetItemOnHitPositiveBerry", "TargetItemAfterMoveUse", "UserItemAfterMoveUse"]],
    ["ItemsFieldSpeed.java", ["SpeedCalcItem", "WeightCalcItem", "PriorityBracketChangeItem", "PriorityBracketUseItem", "RunFromBattleItem", "CertainSwitchingUserItem", "EOREffectItem", "EORHealingItem", "ItemOnSwitchIn", "WeatherExtenderItem", "TerrainExtenderItem", "TerrainStatBoostItem"]],
  ],
};

// ---- 依赖标记 ------------------------------------------------------------
const MARK = [
  ["PBEffects", /PBEffects::/],
  ["天气", /pbWeather|PBWeather::|effectiveWeather|Weather/],
  ["场地", /field\.terrain|PBBattleTerrains|pbStartTerrain|Terrain/],
  ["换人", /pbRecallAndReplace|pbSwitchInBetween|pbPartyScreen|pbGetReplacementPokemonIndex|pbCanSwitch|pbCanChooseNonActive|pbRegisterSwitch/],
  ["捕获", /pbThrowPokeBall|pbCaptureCalc/],
  ["树果", /pbIsBerry\?|isBerry/],
  ["训练家", /trainerBattle\?|pbGetOwnerName|pbTrainerBattle|\$Trainer\b/],
  ["存档", /@peer\.|pbStorePokemon|pbRecordAndStoreCaughtPokemon/],
  ["图鉴", /pbSetSeen|pbSeenForm|pbPlayer\.(seen|owned)|hasOwned\?/],
  ["BOSS", /pbCatchBossPokemon|battleRank/],
  ["狩猎区", /Safari|pbInSafari/],
  ["战斗宫", /BattlePalace|battlePalace/],
  ["华丽大赛", /Contest|contest/],
  ["动画/场景", /pbCommonAnimation|pbAnimation\(|@scene\.|\.scene\.|pbShowAbilitySplash|pbHideAbilitySplash|pbReplaceAbilitySplash|PokeBattle_AnimationPlayer|pbHPChanged|pbRefreshOne|pbChangePokemon/],
  ["PBDebug", /PBDebug/],
  ["招式/PP", /pbReducePP|pbSetPP|move\.pp|totalpp|pbGetMoveData/],
  ["能力等级", /pbRaiseStatStage|pbLowerStatStage|pbCanRaiseStatStage|pbCanLowerStatStage|statStageAt|@stages\[/],
  ["异常状态", /pbInflictStatus|pbCureStatus|pbPoison|pbBurn|pbParalyze|pbFreeze|pbSleep|pbConfuse|pbAttract|Status/],
  ["HP", /pbReduceHP|pbRecoverHP|pbFaint|pbTakeEffectDamage|\btotalhp\b|\bhp\b/],
  ["道具", /itemActive\?|hasActiveItem\?|pbConsumeItem|pbRemoveItem|pbHeldItemTrigger|self\.item|\b@item\b|Items::/],
  ["特性", /abilityActive\?|hasActiveAbility\?|\bability\b|Abilities::/],
];

const FUNCTIONAL = new Set(["换人", "捕获", "训练家", "存档", "图鉴", "BOSS", "狩猎区", "战斗宫", "华丽大赛"]);
const PRESENT = new Set(["动画/场景", "PBDebug"]);

// ---- 插件自身缺陷（M0b §F.1） --------------------------------------------
const DEFECTS = [
  { id: "D1", re: /pbRecoverHP\?/, what: "pbRecoverHP?（多一个 ?）" },
  { id: "D4", re: /Battle::Scene::/, what: "Battle::Scene:: 命名空间不存在" },
  { id: "D2", re: /(?<![A-Za-z0-9_@])dynamax\?/, what: "dynamax?（全工程无定义）" },
  { id: "D3", re: /\.mirrorHerbUsed\s*=/, what: "mirrorHerbUsed=（无 attr_accessor）" },
];

function readSection(rb, total, abs) {
  const raw = fs.readFileSync(abs ? (ROOT + "/" + rb) : (REF + rb), "utf8").replace(/^\uFEFF/, "").split(/\r?\n/);
  const src = new Map();
  for (const line of raw) {
    const m = /^\s*(\d+) \| ([\s\S]*)$/.exec(line);
    if (m) src.set(Number(m[1]), m[2]);
  }
  const bad = [];
  for (let i = 1; i <= total; i++) if (!src.has(i)) bad.push(i);
  return { src, bad };
}

function readCall(src, startLine) {
  // copy(...) 的目标符号列表可能跨行：累积到括号配平为止。
  let txt = "";
  for (let ln = startLine; ; ln++) {
    if (!src.has(ln)) break;
    txt += src.get(ln);
    const open = (txt.match(/\(/g) || []).length;
    const close = (txt.match(/\)/g) || []).length;
    if (close >= open && close > 0) break;
  }
  return txt;
}

function parse(spec) {
  const { src, bad } = readSection(spec.rb, spec.total, spec.abs);
  spec.src = src;
  const adds = [];
  const copies = [];
  for (const [ln, text] of [...src.entries()].sort((a, b) => a[0] - b[0])) {
    const m = /BattleHandlers::([A-Za-z_][A-Za-z0-9_]*)\s*\.\s*(addIf|add|copy)\s*\(/.exec(text);
    if (!m) continue;
    const group = m[1];
    const kind = m[2];
    if (kind === "copy") {
      // 只从 copy( 之后的实参表里取符号，避免把 BattleHandlers::Group 的 ::Group 误读成符号。
      const call = readCall(src, ln);
      const inside = call.slice(call.indexOf("("));
      const syms = [...inside.matchAll(/:([A-Z0-9_]+)/g)].map((x) => x[1]);
      copies.push({ group, line: ln, src: syms[0] || "?", dests: syms.slice(1) });
      continue;
    }
    const s = /\(\s*:([A-Za-z0-9_]+)/.exec(text);
    if (!s) { bad.push("symbol@" + ln); continue; }
    adds.push({ kind, group, symbol: s[1], line: ln });
  }
  adds.sort((a, b) => a.line - b.line);
  for (let i = 0; i < adds.length; i++) {
    const end = (i + 1 < adds.length) ? adds[i + 1].line - 1 : spec.total;
    adds[i].end = end;
    adds[i].len = end - adds[i].line + 1;
    const body = [];
    for (let l = adds[i].line; l <= end; l++) body.push(src.get(l) || "");
    adds[i].body = body.join("\n");
  }
  return { adds, copies, bad, spec };
}

function classify(e, groupToFile) {
  const subs = MARK.filter(([, re]) => re.test(e.body)).map(([n]) => n);
  const functional = subs.filter((s) => FUNCTIONAL.has(s));
  e.subs = subs;
  e.functional = functional;
  e.verdict = functional.length ? "登记" : (subs.some((s) => PRESENT.has(s)) ? "降级" : "可转");
  e.file = groupToFile.get(e.group) || "（未归并！）";
  return e;
}

const push = (o, k, v) => { (o[k] = o[k] || []).push(v); };

const all = [];
const perFile = {};
const perGroup = {};
const perVerdict = { 可转: 0, 降级: 0, 登记: 0 };
const defectHits = [];
const groupToFile = new Map();

for (const spec of FILES) {
  const { adds, copies, bad } = parse(spec);
  for (const [f, groups] of PLAN[spec.key]) for (const g of groups) groupToFile.set(g, f);
  for (const e of adds) { e.fileKey = spec.key; e.spec = spec; classify(e, groupToFile); all.push(e); }
  perFile[spec.key] = { adds, copies, bad, spec };
  for (const e of adds) {
    perVerdict[e.verdict]++;
    push(perGroup, e.group, e);
    push(perFile, e.file, e);
  }
  if (bad.length) throw new Error(spec.key + " 解析异常: " + JSON.stringify(bad.slice(0, 20)));
}

// ---- 边界外注册（同一批组，别的段） --------------------------------------
const extraRows = [];
for (const spec of EXTRA) {
  const { adds, bad } = parse(spec);
  for (const e of adds) { e.spec = spec; e.fileKey = spec.key; extraRows.push(e); }
  if (bad.length) throw new Error(spec.key + " 解析异常: " + JSON.stringify(bad.slice(0, 20)));
}
const registeredGroups = new Set(all.map((e) => e.group));
const declaredNames = new Set(DECLARED.map((d) => d.name));
const undeclared = [...registeredGroups].filter((g) => !declaredNames.has(g));
const extraGroups = new Set(extraRows.map((x) => x.group));
const emptyDeclared = DECLARED.filter((d) => !registeredGroups.has(d.name));
const emptyAndExtra = emptyDeclared.filter((d) => extraGroups.has(d.name)).length;

// ---- defect sweep ---------------------------------------------------------
const linesMatching = (e, test) => {
  const out = [];
  for (let l = e.line; l <= e.end; l++) if (test(e.spec.src.get(l) || "")) out.push(l);
  return out;
};
for (const e of all) {
  const hits = [];
  for (const d of DEFECTS) {
    const ls = linesMatching(e, (t) => d.re.test(t));
    if (!ls.length) continue;
    // dynamax? 在原文里可能是 defined?(...) 保护过的（Abilities:2973/2980）；只有未保护的才算编译不过。
    const hard = (d.id === "D2") ? linesMatching(e, (t) => d.re.test(t) && !/defined\?/.test(t)) : ls;
    hits.push({ id: d.id, what: d.what, lines: ls, hardLines: hard });
  }
  const pw = linesMatching(e, (t) => {
    const ms = [...t.matchAll(/([@A-Za-z_][A-Za-z0-9_@]*)\.pbWeather\b/g)];
    return ms.some((m) => m[1] !== "battle" && m[1] !== "@battle");
  });
  if (pw.length) {
    hits.push({ id: "D5", what: "Battler 上的 pbWeather（Battle#pbWeather 才是合法的）", lines: pw, hardLines: pw });
  }
  if (hits.length) defectHits.push({ e, hits });
}

// ---- markdown -------------------------------------------------------------
const L = [];
const A = perFile.Abilities;
const I = perFile.Items;
const totAdd = all.length;
const covA = A.adds.reduce((s, e) => s + e.len, 0);
const covI = I.adds.reduce((s, e) => s + e.len, 0);

L.push("# Stage 4 / L1' 花名册：BattleHandlers 特性与持有物 handler 切分表");
L.push("");
L.push("> **本文件由脚本生成；本次任务只读研究，未改动任何 `.java` / `.js`。**");
L.push("> 数据源：本工程 `Data/Scripts.rxdata` 现场导出的 `__r20-ref/ruby/BattleHandlers_Abilities.rb`(4585 行)、");
L.push("> `__r20-ref/ruby/BattleHandlers_Items.rb`(1804 行)。行号 = 段内真实行号（导出件左侧 `  N |` 前缀即 Ruby 行号）。");
L.push("> 所有数字都由脚本统计，**没有一处手数**；§9 是生成本文件的脚本全文。");
L.push("");
L.push("## 0. 复现方式");
L.push("");
L.push("脚本读**自身**并把自身源码嵌进本文件，所以 `node` 跑一次即可重新生成整个文件（可逐字节对比）。");
L.push("");
L.push("```powershell");
L.push("# 1) 把 §9 的 ```js 代码块另存为 l1-roster.mjs（脚本内部用绝对路径读工程，放哪都行）");
L.push("# 2) 运行（node v24 已在工程根验证可用）");
L.push("node l1-roster.mjs");
L.push("```");
L.push("");
L.push("`add` / `addIf` / `copy` 的识别正则（对导出件全文逐行匹配）：");
L.push("");
L.push("```js");
L.push("/BattleHandlers::([A-Za-z_][A-Za-z0-9_]*)\\s*\\.\\s*(addIf|add|copy)\\s*\\(/");
L.push("```");
L.push("");
L.push("**行数约定（按 Lead 要求）**：`行数 =（下一条 add/addIf 的 Ruby 行号 − 本条的行号）`；**最后一条到段尾**");
L.push("（Abilities 到 4585，Items 到 1804）。`copy` 行**不**单独成条目，另见 §5、§6。");
L.push("");
L.push("> 该约定的一个副作用：span 的**末行可能属于下一条**（例如 `MIRRORHERB` 的 `1798` 是下一条 `PUNCHINGGLOVE` 的抬头注释 `# 拳击手套`）。");
L.push("> 这是约定的必然结果，不是算错 —— 切文件时按**首行**归属即可。");
L.push("");
L.push("脚本还需要两个额外导出件（§9 的边界外注册要用，放在 `__r20-ref/m0b/` 下，**不是** `.java`/`.js`）：");
L.push("");
L.push("```powershell");
L.push("node __sections.mjs \"Arceus\"  > __r20-ref/m0b/Arceus.txt        # 1024 行");
L.push("node __sections.mjs \"场地\"    > __r20-ref/m0b/FieldTerrain.txt  # 762 行");
L.push("```");
L.push("");

L.push("## 1. 统计");
L.push("");
L.push("| 项 | 特性（Abilities） | 持有物（Items） | 合计 |");
L.push("|---|---:|---:|---:|");
L.push("| `add`/`addIf` 条目数 | " + A.adds.length + " | " + I.adds.length + " | **" + totAdd + "** |");
const aAdd = A.adds.filter((e) => e.kind === "add").length;
const aIf = A.adds.filter((e) => e.kind === "addIf").length;
const iAdd = I.adds.filter((e) => e.kind === "add").length;
const iIf = I.adds.filter((e) => e.kind === "addIf").length;
L.push("| ├ 其中 `.add` | " + aAdd + " | " + iAdd + " | " + (aAdd + iAdd) + " |");
L.push("| └ 其中 `.addIf` | " + aIf + " | " + iIf + " | " + (aIf + iIf) + " |");
L.push("| `.copy` 行数 | " + A.copies.length + " | " + I.copies.length + " | " + (A.copies.length + I.copies.length) + " |");
L.push("| 出现注册的组数（group） | " + new Set(A.adds.map((e) => e.group)).size + " | " + new Set(I.adds.map((e) => e.group)).size + " | " + (new Set(A.adds.map((e) => e.group)).size + new Set(I.adds.map((e) => e.group)).size) + " |");
L.push("| 条目行数合计（span 之和） | " + covA + " / " + A.spec.total + " | " + covI + " / " + I.spec.total + " | " + (covA + covI) + " |");
L.push("");
L.push("> **`addIf` 在本批两个文件里 0 次。** `addIf` 定义在 `Event_Handlers.rb:98-103`，本批两个段未使用它");
L.push("> （全工程只在 `PItem_ItemEffects:400`、`PItem_BattleItemEffects:24/309` 等处使用）。");
L.push("> span 之和 < 段长，是因为每个段**首条登记之前的抬头注释**不落进任何条目：");
L.push("> Abilities 第 1-" + (A.adds[0].line - 1) + " 行、Items 第 1-" + (I.adds[0].line - 1) + " 行。");
L.push(">");
L.push("> ⚠️ 全工程还有 **" + extraRows.length + "** 条同组注册落在**别的段**里（不在上表、也不在切片小计里），见 **§9** —— 这一步会改变切片方案，请先看 §9。");
L.push("");

L.push("### 1.1 按「可转 / 降级 / 登记」计数");
L.push("");
L.push("| 判定 | 特性 | 持有物 | 合计 |");
L.push("|---|---:|---:|---:|");
for (const v of ["可转", "降级", "登记"]) {
  const a = A.adds.filter((e) => e.verdict === v).length;
  const i = I.adds.filter((e) => e.verdict === v).length;
  L.push("| " + v + " | " + a + " | " + i + " | **" + (a + i) + "** |");
}
L.push("| **合计** | **" + A.adds.length + "** | **" + I.adds.length + "** | **" + totAdd + "** |");
L.push("");
L.push("判定规则（脚本里就是这三条，可复核）：");
L.push("");
L.push("1. 条目 body 命中**功能性子系统**之一（" + [...FUNCTIONAL].join(" / ") + "）→ **登记**：语义依赖本运行时还没有的状态，本批不能转；");
L.push("2. 否则若命中**纯表现**子系统（" + [...PRESENT].join(" / ") + "）→ **降级**：逻辑照转，演出/日志调用做成空实现 + 注释登记；");
L.push("3. 否则 → **可转**。");
L.push("");
L.push("> 这两句是本次唯一的判断性规则；`依赖的子系统` 列由 §0 的 `MARK` 正则表逐条命中得出（脚本内可查）。");
L.push("");

L.push("### 1.2 按「建议 Java 文件」计数（小计之和 = 总数，脚本内已断言）");
L.push("");
L.push("| 建议 Java 文件 | 条目数 | 行数小计 | 占比(条目) |");
L.push("|---|---:|---:|---:|");
const fileOrder = [];
for (const spec of FILES) for (const [f] of PLAN[spec.key]) fileOrder.push(f);
let sN = 0;
let sL = 0;
for (const f of fileOrder) {
  const list = perFile[f] || [];
  const ln = list.reduce((s, e) => s + e.len, 0);
  sN += list.length;
  sL += ln;
  L.push("| `" + f + "` | " + list.length + " | " + ln + " | " + ((100 * list.length) / totAdd).toFixed(1) + "% |");
}
L.push("| **合计** | **" + sN + "** | **" + sL + "** | 100% |");
L.push("");
L.push("> 校验：小计条目数之和 " + sN + " = 总数 " + totAdd + (sN === totAdd ? " ✓" : " ✗"));
if (sN !== totAdd) throw new Error("分组小计 != 总数: " + sN + " vs " + totAdd);
L.push("");

L.push("### 1.3 按「组」计数");
L.push("");
L.push("| 组 | 段 | 条目数 | 行数小计 | 建议 Java 文件 |");
L.push("|---|---|---:|---:|---|");
const groups = [...new Set(all.map((e) => e.group))].sort();
for (const g of groups) {
  const list = perGroup[g];
  L.push("| " + g + " | " + list[0].fileKey + " | " + list.length + " | " + list.reduce((s, e) => s + e.len, 0) + " | `" + groupToFile.get(g) + "` |");
}
L.push("| **合计** | | **" + totAdd + "** | **" + (covA + covI) + "** | |");
L.push("");

L.push("## 2. 主表（按「建议 Java 文件」分组，组内按源文件行号升序）");
L.push("");
L.push("列：`序号 | 段:起-止行 | 组 | 符号 | 行数 | 依赖的子系统 | 建议 Java 文件 | 可转/降级/登记`");
L.push("");
L.push("| 序号 | 段:起-止行 | 组 | 符号 | 行数 | 依赖的子系统 | 建议 Java 文件 | 可转/降级/登记 |");
L.push("|---:|---|---|---|---:|---|---|---|");
let n = 0;
const sorted = [...all].sort((x, y) => fileOrder.indexOf(x.file) - fileOrder.indexOf(y.file) || x.line - y.line);
for (const e of sorted) {
  n++;
  e.no = n;
  const span = e.spec.section + ":" + e.line + "-" + e.end;
  L.push("| " + n + " | " + span + " | " + e.group + " | " + e.symbol + " | " + e.len + " | " + (e.subs.length ? e.subs.join("、") : "—") + " | `" + e.file + "` | " + e.verdict + " |");
}
L.push("");
L.push("**主表连续性校验：发出 " + n + " 行，期望 " + totAdd + " 行" + (n === totAdd ? " ✓" : " ✗") + "**");
L.push("");

L.push("## 3. 重复注册检查（同一 `组 + 符号` 被 add 两次）");
L.push("");
const dup = new Map();
for (const e of all) {
  const k = e.fileKey + "/" + e.group + "/" + e.symbol;
  dup.set(k, (dup.get(k) || []).concat(e));
}
const dups = [...dup.entries()].filter(([, v]) => v.length > 1);
if (!dups.length) L.push("**无重复（0 条）。** 每个 `组+符号` 在源文件里只 `add` 一次。");
else {
  L.push("| 组 | 符号 | 次数 | 行号 |");
  L.push("|---|---|---:|---|");
  for (const [, v] of dups) L.push("| " + v[0].group + " | " + v[0].symbol + " | " + v.length + " | " + v.map((e) => e.line).join(", ") + " |");
}
L.push("");

L.push("## 4. 条目里调用了「插件自身缺陷」的（对应 `stage4-m0b-battler-api-notes.md` §E.4 / §F.1）");
L.push("");
if (!defectHits.length) L.push("无。");
else {
  L.push("| 主表序号 | 缺陷 | 命中的插件原文行 | 条目 | 条目起-止行 | 会因此编译不过？ |");
  L.push("|---:|---|---|---|---|---|");
  for (const h of defectHits) {
    for (const d of h.hits) {
      const compile = d.hardLines.length
        ? "**是** —— " + d.hardLines.map((l) => h.e.spec.section + ":" + l).join(", ")
        : "否（原文用 `defined?` 保护；两处分支都要照抄）";
      L.push("| " + h.e.no + " | " + d.id + " " + d.what + " | " + d.lines.map((l) => h.e.spec.section + ":" + l).join(", ") + " | " + h.e.group + " / " + h.e.symbol + " | " + h.e.spec.section + ":" + h.e.line + "-" + h.e.end + " | " + compile + " |");
    }
  }
}
L.push("");
L.push("处置原则（Lead 已定）：**一律照抄原样 + 编译期不可达处用注释登记，绝不自己造替代实现。**");
L.push("");
L.push("> 说明：M0b §E.4 另外 3 条缺陷（`item_to_use` 未定义、`@ability_id` 未定义、`Battler_Statuses:240` 的未定义 `user`）");
L.push("> 都在 `Battler_*` 段的方法体里，**不在本批任何一个 handler 条目内**，故不出现在上表。");
L.push(">");
L.push("> `D2 dynamax?` 出现两次：`Abilities:2973/2980` 包在 `defined?(battler.dynamax?)` 里（**编译得过的写法就是照抄这个 `defined?` 判断**），");
L.push("> 而 `Items:1317` 的 `if user.dynamax?` 没有任何保护 → 该分支照抄必编译不过。");
L.push("");

L.push("## 5. `.copy(...)` 行（不单独成条目，但同样是注册）");
L.push("");
L.push("`HandlerHash#copy(src,*dests)`（`Event_Handlers.rb:115-122`）= 取出 `src` 已注册的 handler，逐个 `add` 到 `dests`。");
L.push("");
L.push("| 序号 | 段:行 | 组 | 源符号 | 被复制的符号 | 建议 Java 文件 |");
L.push("|---:|---|---|---|---|---|");
let cn = 0;
const tCopy = 0;
for (const spec of FILES) {
  for (const c of perFile[spec.key].copies) {
    cn++;
    L.push("| C" + cn + " | " + spec.section + ":" + c.line + " | " + c.group + " | " + c.src + " | " + (c.dests.join(", ") || "（无）") + " | `" + (groupToFile.get(c.group) || "?") + "` |");
  }
}
L.push("");
L.push("> 合计 **" + cn + "** 条 `copy` 行，展开后新增 **" + perFile.Abilities.copies.concat(perFile.Items.copies).reduce((s, c) => s + c.dests.length, 0) + "** 个符号注册。");
L.push("> 所以每个建议文件的**真实注册符号数 = add/addIf 条目数 + 该文件承担的 copy 目标符号数**，见 §6。");
L.push("");

L.push("## 6. 每个建议 Java 文件的真实注册符号数（条目 + copy 目标）");
L.push("");
L.push("| 建议 Java 文件 | add/addIf 条目 | copy 目标符号 | 真实注册符号数 | 条目行数 |");
L.push("|---|---:|---:|---:|---:|");
const copyByFile = {};
for (const spec of FILES) {
  for (const c of perFile[spec.key].copies) {
    const f = groupToFile.get(c.group) || "?";
    copyByFile[f] = (copyByFile[f] || 0) + c.dests.length;
  }
}
let tAdd = 0;
let tCopy2 = 0;for (const f of fileOrder) {
  const cnt = (perFile[f] || []).length;
  const cp = copyByFile[f] || 0;
  const ln = (perFile[f] || []).reduce((s, e) => s + e.len, 0);
  tAdd += cnt;
  tCopy2 += cp;
  L.push("| `" + f + "` | " + cnt + " | " + cp + " | **" + (cnt + cp) + "** | " + ln + " |");
}
L.push("| **合计** | **" + tAdd + "** | **" + tCopy2 + "** | **" + (tAdd + tCopy2) + "** | **" + sL + "** |");
L.push("");

L.push("## 7. 依赖未建模子系统的条目（= 全部「登记」条目）");
L.push("");
const bySub = {};
for (const e of all.filter((e) => e.verdict === "登记")) for (const s of e.functional) push(bySub, s, e);
L.push("| 子系统 | 条目数 | 条目（`#序号 组/符号 @起-止行`） |");
L.push("|---|---:|---|");
for (const s of Object.keys(bySub).sort((a, b) => bySub[b].length - bySub[a].length)) {
  const list = bySub[s];
  L.push("| " + s + " | " + list.length + " | " + list.map((e) => "#" + e.no + " " + e.group + "/" + e.symbol + " @" + e.spec.section + ":" + e.line + "-" + e.end).join("<br>") + " |");
}
const regCount = all.filter((e) => e.verdict === "登记").length;
L.push("| **登记条目总数（去重后）** | **" + regCount + "** | 同一条目可命中多个子系统，故上表各行之和 ≥ 此数 |");
L.push("");
L.push("> **`BOSS` 这一格的降级条件**：3 条 `BOSS` 命中全部来自 `pokemon.battleRank > 2`（`IRONBARBS` `Abilities:1639`、");
L.push("> `BADDREAMS` `Abilities:2241`、`ROCKYHELMET` `Items:1139` 附近），而 `battleRank` 只是 `PokeBattle_Pokemon` 上的一个整数");
L.push("> （`BOSS_HP_RANK = [1,1,2,5,10,15,15,20]` 在 `PokeBattle_BOSS:3`）。**若 Lead 决定把 `battleRank` 当普通 int 字段建模，");
L.push("> 这 3 条立刻从「登记」变「可转」**，`登记` 总数从 " + regCount + " 降到 " + (regCount - 3) + "。");
L.push("");
const byPres = {};
for (const e of all.filter((e) => e.verdict === "降级")) for (const s of e.subs.filter((x) => PRESENT.has(x))) push(byPres, s, e);
L.push("对照：**降级**条目（只碰纯表现子系统）共 **" + all.filter((e) => e.verdict === "降级").length + "** 条 —— " +
  (Object.keys(byPres).map((s) => s + " " + byPres[s].length).join("；") || "无") +
  "。这些条目逻辑可完整转译，只需把演出/日志调用做成空实现并注释登记。");
L.push("");
L.push("### 7.1 「登记」在建议文件里的分布（决定切片时谁背风险）");
L.push("");
L.push("| 建议 Java 文件 | 登记 | 降级 | 可转 | 合计 |");
L.push("|---|---:|---:|---:|---:|");
for (const f of fileOrder) {
  const list = perFile[f] || [];
  L.push("| `" + f + "` | " + list.filter((e) => e.verdict === "登记").length + " | " + list.filter((e) => e.verdict === "降级").length + " | " + list.filter((e) => e.verdict === "可转").length + " | " + list.length + " |");
}
L.push("");

L.push("## 8. 落点与切分要点（供 Lead 切写权限）");
L.push("");
L.push("1. **注册表本体**：`HandlerHash`（`Event_Handlers.rb:68-146`）。`add` 把同一个 handler 同时登记到**数值 ID** 与**符号**两个键（`:109-113`）；");
L.push("   `trigger` 调 handler 时**第一个实参是数值 ID**（`:140` `handler.call(fromSymbol(sym),*args)`），不是符号 —— Java 侧 proc 端口必须保持这个签名约定。");
L.push("2. **`copy` 必须跨条目解析**：`copy` 的语义是「取 src 的 handler 再 add」，所以 Java 侧每个 `组` 需要一张 `Map<符号, handler>`，`copy` 展开成多次 put，**不能**把 dest 符号当成独立的 proc 再写一遍。");
L.push("3. **同一符号在多组出现是正常的**（如 `IMMUNITY` 同时在 `StatusImmunityAbility` 与 `StatusCureAbility`），Java 侧不能合并成一张表。");
L.push("4. **`BattleHandlers.rb` 里声明但本批两个文件 0 条注册的组**：见 **§9.1**（" + emptyDeclared.length + " 个，其中 " + emptyAndExtra + " 个靠 §9 的边界外注册才有 handler）。这些组在 Java 侧仍应保留空表，");
L.push("   否则 `Battle#pbCanSwitch?` / `pbCanRaiseStatStage?` 等调用点拿到的不是 `nil`（Ruby 里 handler 缺失时 `trigger` 返回 `nil`，`triggerXxx` 再还原为默认值）。");
L.push("5. 建议切片顺序（先易后难）：`ItemsDamageUser` → `ItemsDamageTarget` → `ItemsFieldSpeed` → `ItemsOnHit` → `ItemsHealStatus` →");
L.push("   `AbilitiesDamageUser` / `AbilitiesDamageTarget` / `AbilitiesAccuracyCritType` → `AbilitiesSpeedEor` → `AbilitiesStatus` → `AbilitiesOnHit` → `AbilitiesSwitchIn`（最大，且碰换人/图鉴）。");
L.push("");

L.push("## 9. 边界之外：同一批 BattleHandlers 组还被另外两个段注册（**Lead 请先拍板**）");
L.push("");
L.push("脚本同时扫了全工程 636 条 `BattleHandlers::<组>.<add|addIf|copy>(` 注册行，发现 **559 条在指定的两个文件里**，");
L.push("**另有 " + extraRows.length + " 条在别的段里**（这两个段都在 `--list` 里排在 `BattleHandlers_*` **之后**，");
L.push("而 `HandlerHash#add` 是 `@hash[id] = handler` 的直接覆盖（`Event_Handlers.rb:110-113`），");
L.push("**所以这些注册会覆盖主表里的同名条目**）：");
L.push("");
L.push("| 段:行 | 组 | 符号 | 是否覆盖主表已有条目 | 被覆盖的主表条目 |");
L.push("|---|---|---|---|---|");
for (const x of extraRows) {
  const hit = all.find((e) => e.group === x.group && e.symbol === x.symbol);
  L.push("| " + x.spec.section + ":" + x.line + " | " + x.group + " | " + x.symbol + " | " + (hit ? "**是（覆盖）**" : "否（新增）") + " | " + (hit ? "#" + hit.no + " `" + hit.spec.section + ":" + hit.line + "-" + hit.end + "` " + hit.verdict : "—") + " |");
}
L.push("");
const ovr = extraRows.filter((x) => all.find((e) => e.group === x.group && e.symbol === x.symbol)).length;
L.push("> " + extraRows.length + " 条里 **" + ovr + " 条是覆盖**、" + (extraRows.length - ovr) + " 条是新增。");
L.push("> **本批主表按 Lead 的定义只覆盖 `BattleHandlers_Abilities` / `BattleHandlers_Items` 两个文件**，");
L.push("> 这 " + extraRows.length + " 条既不在主表、也不在上面的切片小计里 —— 若 L1' 直接照主表建表，这些符号会缺失（覆盖的那 " + ovr + " 条更会走错 handler）。");
L.push("> 建议：要么给它们开一个 L1'' 小批（各自的建议 Java 文件按同组归并），要么把它们并入上表对应文件、由同一批负责。");
L.push("");

L.push("### 9.1 `BattleHandlers.rb` 声明但本批两个文件 0 条注册的组");
L.push("");
const emptyDeclared2 = emptyDeclared;
L.push("`BattleHandlers.rb` 共声明 **" + DECLARED.length + "** 行 = **" + declaredNames.size + "** 个唯一组名");
L.push("（`ItemOnOpposingStatGain` 被声明了两次：`BattleHandlers.rb:28` 与 `:92`，都是 `ItemHandlerHash.new` —— 后者覆盖前者；");
L.push("本工程里两次声明之间没有任何注册，所以没有 handler 被丢掉，但 Java 侧只应建一张表）。");
L.push("本批两个文件覆盖了其中 **" + registeredGroups.size + "** 个；");
L.push("剩 **" + emptyDeclared2.length + "** 个声明了但本批 0 条注册（Java 侧仍应保留空表，Ruby 里 handler 缺失时");
L.push("`trigger` 返回 `nil`，`triggerXxx` 再还原默认值，语义不能省）：");
L.push("");
L.push("| 组 | 声明行 | 是否在边界外的段里被注册 |");
L.push("|---|---:|---|");
for (const d of emptyDeclared2) L.push("| " + d.name + " | BattleHandlers.rb:" + d.line + " | " + (extraGroups.has(d.name) ? "**是**" : "否") + " |");
L.push("");
L.push("> 其中 **" + emptyAndExtra + "** 个空组靠 §9 的边界外注册才有 handler。");
L.push("");
L.push("另有 **" + undeclared.length + "** 个组**被注册但 `BattleHandlers.rb` 里没有声明**（脚本对 `BattleHandlers.rb` 全文按 `Name = (Ability|Item)HandlerHash.new` 提取到 " + DECLARED.length + " 个声明）：");
L.push("");
if (!undeclared.length) L.push("无。");
else {
  L.push("| 组 | 注册条目数 | 出处 |");
  L.push("|---|---:|---|");
  for (const g of undeclared) {
    const list = perGroup[g];
    L.push("| " + g + " | " + list.length + " | " + [...new Set(list.map((e) => e.spec.section))].join(", ") + " |");
  }
}
L.push("");

L.push("## 10. 分析脚本（本文件由它生成）");
L.push("");
L.push("```js");
L.push(fs.readFileSync(SELF, "utf8").replace(/\n$/, ""));
L.push("```");
L.push("");
L.push("---");
L.push("");
L.push("**交付完毕。** 本文件是本次任务唯一写入的文件。");

fs.writeFileSync(OUT, L.join("\n") + "\n", "utf8");
console.log("wrote " + OUT);
console.log("add/addIf total = " + totAdd + "  (Abilities " + A.adds.length + " / Items " + I.adds.length + ")");
console.log("verdict 可转/降级/登记 = " + perVerdict["可转"] + " / " + perVerdict["降级"] + " / " + perVerdict["登记"]);
console.log("copy lines = " + cn + "  copy dest symbols = " + tCopy2);
console.log("defect-affected entries = " + defectHits.length + "  (rows " + defectHits.reduce((s, h) => s + h.hits.length, 0) + ", hard-fail rows " + defectHits.reduce((s, h) => s + h.hits.filter((d) => d.hardLines.length).length, 0) + ")");
```

---

**交付完毕。** 本文件是本次任务唯一写入的文件。
