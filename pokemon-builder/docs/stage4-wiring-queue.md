# Stage 4 §4 接线队列（按「解锁量 / 成本」排序）

> 生成于本轮：① 伤害公式转译完成之后。
> 全部条目都标了**插件原文位置**，实施时以原文为准；本文只是索引，不含转译内容。
> 状态：`[ ]` 未做 · `[~]` 进行中 · `[x]` 完成

---

## 0. 当前基线（动任何东西之前先确认）

| 项 | 值 |
|---|---|
| `:core:compileJava` | BUILD SUCCESSFUL |
| `:core:test --rerun-tasks` | **671 tests / 3 failures**（见 Q9） |
| handler 注册 | 575 条 `add/addIf`（13 个 L1' 文件） |
| 招式策略 | 526 个 function code 已注册进 `MoveEffectRegistry` |
| trigger 接线覆盖 | **41 / 85** |
| `MoveEffect` 钩子被消费 | 约 23 / 105 |

---

## Q1 粉末免疫 + 能力/道具免疫块（成功检查阶段）　`[ ]`

**症状（玩家可见）**
- 麻痹粉 / 催眠粉 / 毒粉 / 蘑菇孢子 **能影响草属性**；防尘、防尘护目镜无效
- **浮游 / 气球 / 电磁漂浮 / 意念移物**不免疫地面招式

**插件原文**：`Battler_UseMove_SuccessChecks:500-587`（`pbSuccessCheckAgainstTarget` 内）
- `:500-566` 能力/道具免疫块（`pbImmunityByAbility` 及 Levitate / Air Balloon / Magnet Rise / Telekinesis 的文案与 `return false`）
- `:567-587` 粉末块：`NEWEST_BATTLE_MECHANICS && move.powderMove?` 下的 `:GRASS`（:569-573）· `OVERCOAT`(:574-583) · `SAFETYGOGGLES`(:584-587)

**Java 现状**
- `Battler.affectedByPowder(boolean)`（`Battler.java:1874`）**已转译齐全**（`:1881` GRASS、`:1887` OVERCOAT/DIVINEPACT、`:1899` SAFETYGOGGLES）—— **但全工程无人调用**
- `MoveEffect.pbImmunityByAbility` 已声明（`MoveEffect.java:272`），`MoveEffectBase.java:579` 有实现 —— **同样无调用点**
- `Battle.execute` 只做了类型免疫，没有这一段

**修法**：新增成功检查助手（建议 `BattleSuccessChecks.java`，我的新文件）转译 `:500-587`，在 `Battle.execute` 的类型免疫检查之后**插入**一处调用（insert-only，不改既有行）。

**验收**：新单测 —— 草属性被麻痹粉打中后 `status` 仍为空；带 `SAFETYGOGGLES` 的宝可梦同样；浮游特性对地面招式伤害为 0。全量测试回绿。

---

## Q2 `pbFailsAgainstTarget?` 调用点　`[ ]`

**插件原文**：`Move_Usage.rb:104` 的调用位置（`pbSuccessCheckAgainstTarget` 内，紧接粉末块之后）
**现状**：钩子已声明且 526 个策略类里有覆写，**但没有任何调用点**
**注意**：这会启用大量策略类的自定义失败条件，**单独做、单独验**（回归面最大的一条）
**依赖**：Q1 同一段代码，建议 Q1 之后紧接着做

---

## Q3 ② 回合末（EOR）聚簇　`[ ]`

**症状**：剩饭 / 黑泥 / 大根茎 / 加速 / 雨盘 / 干燥皮肤 这一批**全不生效**
**插件原文**：`Battle_Phase_EndOfRound.txt`（861 行）剩余阶段 + `Battler#pbEndOfRoundPhase`
**Java 现状**：`Battle.endOfTurn` 只做「回合计数 + 中毒 + 烧伤 + flinch 复位」；6 个 EOR trigger 无调用点：
`triggerEOREffectAbility` · `triggerEOREffectItem` · `triggerEORHealingAbility` · `triggerEORHealingItem` · `triggerEORGainItemAbility` · `triggerEORWeatherAbility`
（另有 `triggerWeatherExtenderItem` / `triggerTerrainExtenderItem`）
**验收**：剩饭每次回合末回复 `maxHp/16`（`battle.step()` 两次断言 HP 上升）

---

## Q4 命中率聚簇　`[ ]`

**症状**：沙隐 / 复眼 / 广角镜 / 光粉 等命中修正不生效
**插件原文**：`Move_Usage_Calculations:112-183`（`pbAccuracyCheck` + `pbCalcAccuracyModifiers`，两个**重活默认体**仍未转译）
**trigger**（5 个无调用点）：`triggerAccuracyCalcUserAbility` · `UserItem` · `UserAllyAbility` · `TargetAbility` · `TargetItem`
**Java 现状**：`Battle.hitChance` 是简化实现；`MoveEffectBase.pbAccuracyCheck` / `pbCalcAccuracyModifiers` 仍是抛异常桩

---

## Q5 会心聚簇　`[ ]`

**插件原文**：`Move_Usage_Calculations:196-229`（`pbIsCritical?` 重活默认体未转译）
**trigger**（4 个）：`triggerCriticalCalcUserAbility` · `UserItem` · `TargetAbility` · `TargetItem`
**注**：① 里我把会心判定保持在 `Battle.execute` 旧路径（避免「动画算会心、伤害不算」不一致）；做完本项后应改为统一走策略/trigger。

---

## Q6 速度 / 优先度　`[ ]`

**trigger**（4 个）：`triggerSpeedCalcAbility` · `triggerSpeedCalcItem` · `triggerPriorityChangeAbility` · `triggerPriorityBracket*`（另有 `triggerPriorityBracketUseAbility/Item`）
**症状**：叶绿素 / 拨沙 / 轻装 / 先制之爪 等不生效

---

## Q7 换人道具　`[ ]`

**trigger**：`triggerItemOnSwitchIn`（1 个无调用点）
**症状**：气球类换人时道具效果不触发

---

## Q8 `Battle.execute` 改走策略（526 个招式策略真正生效）　`[ ]`

**现状**：`Battle.execute` 仍读旧数据表 `MoveEffects.java`（119 行 / 75 个 code 条目），`Battle.java` 里 15 处 `MoveEffects.` 引用；526 个策略类里绝大多数（自定义 `pbEffectAgainstTarget` / `pbBaseDamage` 的招式）**不会被执行**
**已接的策略钩子**：约 23 / 105（含 `BattlerHitEffects` 那 8 个 trigger 用到的 `pbContactMove`/`damagingMove`/`addlEffect`/`pbSwitchOutTargetsEffect`/`pbEndOfMoveUsageEffect`，以及 `DamageCalc` 用到的 `pbCalcType`/`pbBaseDamage*`/`pbModifyDamage`/`pbGetAttackStats`/`pbGetDefenseStats`/`pbCalcTypeMod`）
**注意**：这是**修改既有行**的操作，与 `Battle.java` 的所有权约定冲突，需要先裁决（shadow wiring 的早退分流是 insert-only 的替代方案）
**同样未消费**：策略类覆写的 `pbCalcDamage` / `pbCalcDamageMultipliers`（① 把原体放在 `DamageCalc`）

---

## Q9 ① 遗留：3 条单测需更新　`[ ]`

`DamageCalc` 换成插件原体后，这 3 条单测仍按旧简化公式构造（**无 `Battle`** 的裸 `Battler`）并断言旧数值：
- `BattleTest: the damage formula matches floor((2*L/5+2)*P*A/D/50)+2 with STAB`（期望 `826..972`）
- `BattleTest: a type immunity deals no damage`
- `EndOfRoundTest: a critical hit ignores the stat stages`

失败原因统一为 NPE `user.battle is null`（忠实实现需要 `@battle`）。
**推荐修法**：改成先构造 `Battle` 再取 battler，并按新公式重新推导期望值。
**备选修法**：在 `DamageCalc` 里加「无 battle 时跳过战斗相关阶段」的守卫（`pbCalcType` 的 `@battle.field` 已加过一次同样的守卫并写了 `登记:`）。

---

## Q10 零散登记（可穿插做）　`[ ]`

| 项 | 原文 | 现状 |
|---|---|---|
| 徽章乘区 | `Move_Usage_Calculations:413-428` | `@battle.internalBattle` / `numBadges` / `NUM_BADGES_BOOST_*` 未建模，整段跳过 |
| `damageReducedByFrostbite?` | `Arceus:216` 定义 | **从未加进 `MoveEffect` 接口** ⇒ `:500-503` 的冰伤减半无钩子可调 |
| Dragon Darts 的 typeMod 重算 | `Move_Usage_Calculations:486-491` | 需要 `@battle.pbSideSize`，运行时没有 |
| 种族值总和 > 830 守卫 | `Move_Usage_Calculations:286-291` | 需要 `pokemon.baseStats` 汇总 |
| `pbStartTerrain` | `PokeBattle_Battle:741-769` | 未落地 ⇒ 11 条 SURGE 类特性不布场地 |
| `battleRules`（SOULDEW 等） | `PokeBattle_Battle` | 桩 |
| 全局开关 `$game_switches[99]` | `Move_Effects_180-1FF:1607` | 按非开关分支取 `5050`；**开关落地后必须回来接** |
| `pbIsCritical?` | `Move_Usage_Calculations:196-229` | 仍走 `Battle.isCritical` 旧路径（见 Q5） |
| `$game_switches` / Phone 子系统 / `pbClearData` / 动画缓存 | — | 未建模 |

---

## 已完成的接线（供对照）

| 项 | 内容 |
|---|---|
| `[x]` ① 伤害公式 | `pbCalcDamage`(:252-295) + `pbCalcDamageMultipliers`(:296-542) 转译进 `DamageCalc.java`（55 → 390 行）；`pbCalcType`(:14-29) / `pbCalcTypeModSingle`(:34-71) / `pbCalcTypeMod`(:73-103) 转译进 `MoveEffectBase`；**7 个 `triggerDamageCalc*` 接上**（trigger 34 → 41 / 85） |
| `[x]` 命中时聚簇 | `BattlerHitEffects.java`：`pbEffectsOnMakingHit` + `pbEffectsAfterMove`(+`2`) 逐行转译，含 `Arceus:181-210` 重开层；**8 个 trigger 接上** |
| `[x]` 根因修复 | `Battler` 构造器补 `@ability`/`@item`（`Battler_Initialize:83-84`）；`damageState` 填充（`calcDamage`/`hpLost`/`initialHP`/`totalHPLost`） |
| `[x]` 桩层退役 | `PendingApi` 17 个方法改委托；`movefx/` 的 `PendingApi.*` 迁 694 处；L1' 迁 74 处；movefx 第四轮 30 处 |
