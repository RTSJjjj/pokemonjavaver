# Stage 4 主线计划（收尾全部剩余工作）

> 目标：把「插件 → Java 运行时」的战斗系统**全部接线并验证完**，并使界面战线可独立推进。
> 铁律不变：一切以本工程插件原文为准 · 逐行转译 · 禁止自造/近似 · 无法转译立刻停下并登记。
> 每完成一项：`git` 显式路径提交（**禁用 `git add -A`**）· 联合编译 · 全量测试由 Lead 串行跑。
> 状态：`[ ]` `[~]` `[x]`

---

## 基线（2026-10-07 本轮末）

| 项 | 值 |
|---|---|
| `:core:compileJava` | BUILD SUCCESSFUL |
| `:core:test --rerun-tasks` | **675 tests / 0 failures**（B0/B1/B2/B3 已完成） |
| 特性+道具 handler 注册 | 575 条（13 个 L1' 文件） |
| 招式策略 | 526 个 function code 全注册；`MoveEffect` 钩子 105 个 |
| **trigger 接线覆盖** | **44 / 85**（B1/B2/B3 走的是成功检查阶段，不是 trigger） |
| `MoveEffect` 钩子被消费 | ~25 / 105 |
| `PendingApi` / `MoveFxPendingApi` | 18 / 28 个声明（其余为真无替代的 KEEP） |

**已经接通的**（运行时测试证明）：命中后触发的特性/道具（粗糙皮肤·静电·毒手·凸凸头盔）· 伤害计算的特性/道具乘区（大力士·毅力·适应力·命玉·专爱·属性道具·of Ruin）· 粉末免疫（草属性·防尘·防尘护目镜）· `damageState` 填充 · `Battler` 的 `ability`/`item` 初始化。

---

# A. 战斗流程接线（主线，按解锁量排序）

## B0　`[ ]` 修 3 条旧断言，恢复绿基线（**第一步，其他都排在它后面**）
`DamageCalc` 换成插件原体后，这 3 条单测仍用**无 `Battle` 的裸 `Battler`** 并断言旧简化公式的数值：
- `BattleTest: the damage formula matches floor((2*L/5+2)*P*A/D/50)+2 with STAB`（期望 `826..972`）
- `BattleTest: a type immunity deals no damage`
- `EndOfRoundTest: a critical hit ignores the stat stages`

失败统一为 NPE `user.battle is null`（忠实实现需要 `@battle`）。
**做法**：改成先构造 `Battle` 再取 battler，按新公式重新推导期望值（并保留「会心忽略能力等级」这条断言的本意）。
**验收**：674/0/0。

## B1　`[x]` 粉末免疫块
`Battler_UseMove_SuccessChecks:567-587` → `BattleSuccessChecks.java`；`Battle.execute` 插入调用。
运行时测试 3 条通过（草属性不麻痹 · 非草属性仍麻痹 · 防尘护目镜挡粉末）。

## B2　`[ ]` 能力/道具免疫块
**症状**：浮游 / 气球 / 电磁漂浮 / 意念移物 **不免疫地面招式**；蓄水 / 引火 / 干燥皮肤 / 避雷针 / 引水 / 防弹 / 隔音 等一整套免疫不生效。
**原文**：`Battler_UseMove_SuccessChecks:500-566`（`pbImmunityByAbility` 及逐条的文案与 `return false`）
**Java 缺口**：`MoveEffect.pbImmunityByAbility`（`MoveEffect.java:272`）已声明、`MoveEffectBase:579` 有实现，**全工程无调用点**
**成本**：约 70 行接线（正文在 `Battler_UseMove_SuccessChecks`）
**验收**：带电宝可梦被地面招式打中伤害为 0 且显示「用悬浮避开了攻击！」；气球持有者同理。

## B3　`[ ]` `pbFailsAgainstTarget?` 调用点
**原文**：`Move_Usage.rb:104`（`pbSuccessCheckAgainstTarget` 尾部）
**风险**：会启用数百个策略类的自定义失败条件 —— **单独做、单独验**，回归面最大的一条
**验收**：抽样 5 个有覆写的 code（含 `1BE` 那个 arity 冲突的）逐个断言。

## B4　`[ ]` 回合末（EOR）聚簇 —— **玩家可见度最高**
**症状**：剩饭 / 黑泥 / 大根茎 / 加速 / 雨盘 / 干燥皮肤 全不生效。
**原文**：`Battle_Phase_EndOfRound.txt`（**861 行**）剩余阶段 + `Battler#pbEndOfRoundPhase`
**Java 现状**：`Battle.endOfTurn` 只做「回合计数 + 中毒 + 烧伤 + flinch 复位」
**trigger**（6 个无调用点）：`EOREffectAbility` · `EOREffectItem` · `EORHealingAbility` · `EORHealingItem` · `EORGainItemAbility` · `EORWeatherAbility`（+ `WeatherExtenderItem` / `TerrainExtenderItem`）
**依赖**：B7（`pbInitEffects` 重开）里的 `CaptureNet` 初始化
**验收**：剩饭持有者回合末 `+maxHp/16`；黑泥对非毒属性 `-maxHp/8`。

## B5　`[ ]` 命中率聚簇
**原文**：`Move_Usage_Calculations:112-183`（`pbAccuracyCheck` + `pbCalcAccuracyModifiers`，**两个重活默认体仍未转译**，现为抛异常桩）
**trigger**（5）：`AccuracyCalcUserAbility` · `UserItem` · `UserAllyAbility` · `TargetAbility` · `TargetItem`
**症状**：沙隐 / 复眼 / 广角镜 / 光粉 等不生效
**注**：`Battle.hitChance` 现在是简化实现，做完本项后应改走 `pbAccuracyCheck`

## B6　`[ ]` 会心聚簇
**原文**：`Move_Usage_Calculations:196-229`（`pbIsCritical?` 重活默认体未转译）
**trigger**（4）：`CriticalCalcUserAbility` · `UserItem` · `TargetAbility` · `TargetItem`
**注**：① 里会心判定暂留在 `Battle.isCritical`（避免「动画算会心、伤害不算」不一致）；本项做完改为统一走策略 + trigger

## B7　`[ ]` 速度 / 优先度
**trigger**（6）：`SpeedCalcAbility` · `SpeedCalcItem` · `PriorityChangeAbility` · `PriorityBracketChangeAbility/Item` · `PriorityBracketUseAbility/Item`
**症状**：叶绿素 / 拨沙 / 轻装 / 先制之爪 等不生效

## B8　`[ ]` 换人 / 其它零散 trigger（约 20 个）
`ItemOnSwitchIn`（气球）· `MoveBlockingAbility`（如「广域防守」类）· `MoveImmunityTargetAbility` · `TrappingTargetAbility/Item` · `WeightCalcAbility/Item`（重量类招式）· `EVGainModifierItem` · `ExpGainModifierItem` · `RunFromBattleAbility/Item` · `CertainSwitchingUserAbility/Item` · `AbilityOnFlinch` · `AbilityOnInflictingStatus` · `AbilityOnOpposingStatGain` · `ItemOnOpposingStatGain` · `ItemOnStatLoss` · `TargetAbilityOnHitProp`

## B9　`[ ]` `Battle.execute` 改走策略（**526 个招式策略真正生效**）
**现状**：`Battle.execute` 仍读旧数据表 `MoveEffects.java`（119 行 / 75 个 code）；`Battle.java` 里仍有 15 处 `MoveEffects.`；526 个策略类里绝大多数（自定义 `pbEffectAgainstTarget` / `pbBaseDamage` 的招式）**不会被执行**
**做法（两选一，需先裁决）**：
- (a) 早退分流（**insert-only**，不碰既有行）：`if (BattleMoveWiring.isWired(move)) { BattleMoveWiring.execute(...); return; }` —— 新文件承载策略驱动流程
- (b) 直接改 `Battle.execute` 的既有行（与 `Battle.java` 的所有权约定冲突）
**同样要处理**：策略类覆写的 `pbCalcDamage` / `pbCalcDamageMultipliers`（① 把原体放在 `DamageCalc`，策略覆写目前不被消费）

## B10　`[ ]` `pbInitEffects` / `pbEndOfRoundPhase` 的重开合并
- `Arceus:36-50` + `场地:483-490` 重开 `PokeBattle_Battler#pbInitEffects`：batonPass 时按 `PowerShift` 交换攻防；初始化 `VictoryDance`/`PowerShift`/`StoneAxe`/`CaptureNet`/`CaptureNetUser`
- `场地:556+` 重开 `PokeBattle_Battle#pbEndOfRoundPhase`：CaptureNet 回合末降速
- 归口：`Battler.java` / `Battle.java`（insert-only）

---

# C. 数据与接口健全性

## C1　`[ ]` `PbsData.parse` 的加载顺序 bug（**真 bug，建议早修**）
`readSpecies`（`PbsData.java:684`）在 `readAbilities`（`:688`）**之前**运行 ⇒ species 的 `"abilities":[...]` 若写能力名会**静默解析失败**，整个 `pokemon.json` 变成空表（本轮实测：`species keys = []`，所有单测都得用 `"abilities":[]` 绕开）。
**做法**：调整 parse 顺序（或两遍解析），使 species 能解析能力名。
**验收**：新单测 —— species 写 `"abilities":["OVERGROW"]` 能被解析出 `species.abilities.get(0)=="OVERGROW"`。

## C2　`[ ]` `MoveEffect` 接口补 3 个钩子
`damageReducedByFrostbite?`（`Arceus:216`）· `undrowsesUser?`（`Arceus:218`）· `bugLureTerrainActiveFor?`（`场地:54`）
现在它们没有落点 ⇒ `Move_Usage_Calculations:500-503`（冰伤减半）等分支无法转译。
**做法**：加声明 + `MoveEffectBase` 默认体（后者按 `Arceus:218` 的 `[WILDCHARGE,SPARK,VOLTACKLE].include?(@id)` 实现）。
**注意**：接口改动会影响全部 526 个类与 `MoveEffectBase` 的 `@Override` 计数。

## C3　`[ ]` 等级上限
`Settings:42 MAXIMUM_LEVEL=210`，而运行时卡 100（`PBExperience:109-135` 未落地）。
**验收**：210 级宝可梦的经验/能力值计算与插件一致。

## C4　`[ ]` PP-Up 公式
`PBMove#totalpp`（`PokeBattle_Move.rb:66-70`）的 PP-Up 加成未落地 ⇒ 使用 PP-Up 无效。

## C5　`[ ]` 名称本地化不一致
`PBNatures.getName` 返回中文名，而 `natures.json`/`abilities.json`/`moves.json` 是英文名 —— **在宝可梦菜单/摘要页会直接可见**（与 D 战线联动）。

## C6　`[ ]` `Battle` 侧缺失成员（按需补）
`pbStartTerrain`（⇒ 11 条 SURGE 类特性不布场地）· `pbSideSize`（Dragon Darts 的 typeMod 重算）· `internalBattle`+`numBadges`（徽章乘区 `:413-428`）· `pbPursuit` · `pbActivateHealingWish` · `pbGetOwnerIndexFromBattlerIndex` · `pbSwapBattlers` · `pbReviveInParty`（定义在 `:1288-1309` 的 `class PokeBattle_Battle` 里）· `lastMoveUsed` · `battleBond` · `pbUseMoveSimple` · `decisions` 第 5 槽 + `pbCalculatePriority`（⇒ **DAZZLING 不生效**）· `battleRules`（⇒ SOULDEW）· `pbCheckGlobalAbility` 恒 null（⇒ AFTERMATH 的 Damp 守卫不触发）· `$game_switches[99]`（全局开关落地后必须回来接 `Move_Effects_180-1FF:1607`）

## C7　`[ ]` `Battler` 剩余低频道方法（~60 个）
`pbTransform` · `pbLowerAttackStatStageIntimidate` · `pbSleepSelf` · `num_fainted_allies` · `pbWeight` · `reborn?`/`setCanRebirth` · `eachMove`/`eachMoveWithIndex` · `isCommander?`/`isCommanderHost?` · `pbAddRageHit`/`num_times_hit` · `pbEffectsOnSwitchIn` · `pbRecoverHPFromDrain` · `pbAbleNonActiveCount` · `pbItemOpposingStatGainCheck` · `pbChangeTypes` · `setForm` · `isPledgeMove` · `pbCanFreeze`/`pbFreeze`/`pbSleep`/`pbFlinch`/`movedThisRound` ✅ 已补
（其中 `pbEffect*` 系列多为个别招式／特性依赖，按 B4–B8 的实际报错逐个补最快）

## C8　`[ ]` 未建模子系统（登记，按需排期）
Phone 子系统 · `pbClearData` · 动画缓存 · `PokeBattle_BattlePeer` · `PBattle_Safari` · `PokeBattle_BattlePalace`（AI 替身）

---

# D. 界面战线（可独立并行，交 Claude）

## D0　`[ ]` 抽取原文 + 三条校验（**先做这一步，否则后面全废**）
`PScreen_Party`（**1697 行**）不在任何 `.rb` 里，压缩在 `Data/Scripts.rxdata`（1,336,298 字节，RMXP Marshal + zlib）。
- 唯一正确读法：`pokemon-builder/tools/scanner/index.js` → `readScriptSources(root)`
- `pokemon-builder/**/scripts/sections.json`（384 段）**只有元数据、无 `source` 字段**
- 校验：行数 == 1697 · `definedSymbols` 齐全（`PokemonPartyBlankPanel`/`PokemonPartyCancelSprite`/`…Scene`/`…Screen`）· UTF-8 无 BOM
- **历史教训**：裸抽得到过 0 行空文件（`__r6-staging/l1-scripts/PScreen_PauseMenu.rb` 至今 0 行），`logs/build-menuref.mjs` 自身中文已乱码

## D1　`[ ]` 宝可梦菜单（暂停菜单 →「宝可梦」）
Java：`ui/menu/PartyView.java`(539) · `PartyModel.java`(83) · `pokemon/Party.java`(137) · 确认进 `SummaryView.java`(1172)
接线：`PauseMenuModel.java:61 Action.PARTY` → `PauseMenuOverlay.java:330` → `PartyView`；`BattleScreen.java:1478` 战斗内换人复用
插件：`PScreen_Party`(1697) · 摘要 `PScreen_Summary` / `BW PScreen_Summary`
流程：**先出对账表**（`docs/stage4-pscreen-party-gap.md`：逐 def 列「已转译/缺失/自创/近似」四类），Lead 审完才允许改代码

## D2　`[ ]` 同层其它界面（同一方法论）
寄存 `PScreen_PokemonStorage`/`PScreen_ItemStorage` → `StorageView`(787)（原文 `0220.rb` 1984 / `67932999.rb` 2348）
背包 `PScreen_Bag` → `BagView`(433)（`19305931.rb` 692 / `27522249.rb` 947）
暂停菜单本体：`Modular_Pause_Menu.rb`(402) + `PSystem_Utilities.txt`(1169)；**`PScreen_PauseMenu` 是空 section**
悬而未决：`BattleScreen.java`(3893) 是否也在重做范围（**0 处「登记」**，与插件 `PokeBattle_Scene` 等 3379 行/176 def 对不上）

## D3　`[ ]` 验收方式（全程）
像素统计优先（探针 `:lwjgl3:captureMenu "-PcaptureArgs=X:\generated|<out>|<模式>"`，模式含 wild/trainer/switch/trainer2/faint/fight-status/fight-0pp/damage/transition）· 文件 UTF-8 无 BOM（**禁用 pwsh 重定向写含中文的 Java**）· 全量测试由 Lead 串行跑

---

# E. 审计与交付

## E1　`[ ]` 插件缺陷清单（照抄不修，最终审计要能逐条指认）
已确认约 14 条，散在各文件 `// 登记:`：8 处 handler 自身坏（`pbRecoverHP?` 多 `?` · `dynamax?` 未定义 · `mirrorHerbUsed=` 无 accessor · `Battle::Scene::` 无此模块 · `target.pbWeather` · `item_to_use` 未定义 · `@ability_id` 未定义 · `Battler_Statuses:240` 未定义 `user`）· `DamageCalcTargetAbility` 3 条 copy 源缺失 · `CRAFTMIND` 形参错位 · `pbCalcAccuracyMultipliers` 死+坏 · `Tearalament` 重复声明 · `pbChangeTypes` 两版覆盖 · `USE_ABILITY_SPLASH=true` 致 30+ 死分支 · `SANDSPIT` 形参错位 · `SCREENCLEANER:2781` 查 ownSide 写 opposingSide · `TERAFORMZERO:3395` 拿 `None` 比 defaultWeather · `pbAdditionalEffectChance` arity 冲突 · `1AF:1948` 孤立表达式 · `Arceus:216/218` 钩子未进接口
**做法**：汇总成 `docs/plugin-defects.md`，每条附 `段:行号` + Java 处置（照抄/登记/抛异常助手）。

## E2　`[ ]` 跨段「重开覆盖」总表
本工程插件的系统性手法：后加载段重开并覆盖。已发现：
`PokeBattle_Move_*` 类（**15 处 / 11 code**）· `PBEffects` 常量（**6 个** ✅ 已修）· `PokeBattle_Move_*` 方法（`0B3`/`0A4`/`060`/`18A` 的 `alias` 链）· `PokeBattle_Battler#pbInitEffects`（2 处）· `PokeBattle_Battle#pbEndOfRoundPhase` · `pbEffectsAfterMove`（✅ 已做）· `PokeBattle_Move_1A4#pbFailsAgainstTarget?`（同文件两次）· `PokeBattle_Move_1C8`（两次声明）
**做法**：出全表（含加载顺序判定），逐条标注「已合并/待合并/不适用」。

## E3　`[ ]` 提交（分批，显式路径）
1. 构建器/数据（`pbs.js` `types.json` `PbsData`）2. M0 地基（`PBEffects`/`EffectMap`/`HandlerHash`/`BattleHandlers`/`BattleHandlerRegistry`）3. L1' 13 文件 4. L2 `movefx/` 5. §4 接线（`DamageCalc`/`BattlerHitEffects`/`BattleSuccessChecks`/`Battler`/`Battle`/`PendingApi`）6. docs
**禁用 `git add -A` / `git add .`**；`__r20-ref/` 与 `X:\runtime\lwjgl3\*` 不入库。

---

# 建议执行顺序（一次一批，每批后全量测试 + 提交）

| 批次 | 内容 | 理由 |
|---|---|---|
| ~~第 1 批~~ | ~~B0 修 3 条旧断言~~ + B1 粉末块 → **674/0/0 已达** | ✅ 完成 |
| **第 2 批** | B2 免疫块 + B3 `pbFailsAgainstTarget?` | 同属成功检查阶段，一次把这段做完；玩家可见（浮游/气球） |
| **第 3 批** | B4 EOR 阶段 + B10 的 `pbInitEffects`/`pbEndOfRoundPhase` 重开 | 解锁剩饭/黑泥/加速这一批「天天见到」的效果 |
| **第 4 批** | B5 命中率 + B6 会心（两个重活默认体 + 9 个 trigger） | 补齐计算链的另外两根支柱 |
| **第 5 批** | B9 `Battle.execute` 改走策略（早退分流，insert-only） | 让 526 个策略类真正生效 —— 单批改动最大、回归面最广 |
| **第 6 批** | B7 + B8 其余 trigger（速度/优先度/换人/零散） | 补齐 trigger 覆盖到 85/85 |
| **第 7 批** | C1 `PbsData` 顺序 bug + C2 接口补钩子 + C5 本地化 | 数据层健全性，为界面战线铺路 |
| **第 8 批** | C3/C4/C6/C7 零散缺口 | 按实际报错逐个补 |
| **并行线** | D0 → D1 → D2（交 Claude） | 与 A/C 无写冲突（`ui/**` vs `battle/**`），可同时推进 |
| **收尾** | E1/E2 审计文档 + E3 分批提交 | 交付前逐条指认 |
