# 阶段 3 规划：宝可梦域（L7）与收尾

> 2026-10-05 制定。依据：`generated/scripts/*` 的真实分类（L12 之后的最新数据），
> 分类脚本 `logs/stage3-recon.mjs`，结果 `logs/stage3-recon.json`。

## 0. 目标

把阶段 2 的"地图/事件/脚本运行时"扩展成**可玩的宝可梦游戏**：宝可梦数据模型 → 队伍/背包
→ 遇敌与战斗 → 培育（升级/进化/生蛋）→ PC/图鉴/菜单 → 插件特性 → 触摸与暂停菜单视觉，
并让脚本覆盖率从 90.8% 收敛到接近 100%。

阶段 2 已具备（复用，不重做）：rxdata/PBS 转换、地图/事件/IR 运行时、解释器（全部基础
事件命令 + 90.8% 脚本块）、存档、音频（含 loop 区间）、菜单/标题/存读档、素材随包、
桌面/Android 打包、**GitHub Actions 全链路 CI（已绿）**。

## 1. 现状数据（2026-10-05，5441 个脚本块）

| 状态 | 块数 | 说明 |
|---|---|---|
| 已转译 TRANSLATED | 4052 | 事件可执行 |
| 需 Java 处理器 JAVA_HANDLER_REQUIRED | **891** | 已知 Essentials API，等运行时实现（837 为"宝可梦域"、54 为项目私有插件） |
| 未支持 UNSUPPORTED | **498** | 无处理器/复杂表达式/插件私有 |

按 API 计（一个块可能命中多个 API，用于排优先级）：

```
JAVA_HANDLER_REQUIRED  top: pbGenPkmn 206 · pbBerryPlant 124 · pbPickBerry 117 · makeShiny 113
  pbFreeWildBattle 89 · pbAddPokemon 86 · pbSEPlay 85 · pbSet 83 · $Trainer 82 · pokemonCount 81
  pbShowMap 61 · pbPokeCenterPC 59 · pbSetPokemonCenter 56 · setAbility 50 · pbStoreItem 48
  $PokemonBag 48 · calcStats 40 · pbChoosePokemonForTrade 39 · pbGet 38 · pbStartTrade 37 …（共 40 项）

UNSUPPORTED top: pbSmashThisEvent 155 · pbRockSmashRandomEncounter 155 · setBattleRule 62
  pbTrainerIntro 56 · $Trainer 43 · $PokemonBag 36 · pbDeleteItem 34 · pbGetKeyItem 28
  pbRemoveDependencies 28 · pbSlotMachine 24 · weather/PBFieldWeather 17 · new 17
  pbRegisterPartner 16 · giveRibbon 11 · pbPokemonFollow 6 · advanceQuestToStage 6
  pbHallOfFameEntry 5 · Scene_Credits 5 · 复杂表达式（`<none>`/complex）26 …
```

结论：**近 6 成待办集中在"宝可梦数据模型 + 队伍/背包 + 战斗"三件事**，其余是设施/菜单、
培育、插件小特性。

## 2. 阶段划分

### P0 宝可梦数据模型 + PBS 生成（地基，先行）
- Builder：解析 `PBS/*.txt`（pokemon/moves/items/abilities/natures/types/trainers/metadata 等）
  → `generated/pbs/*.json`（BaseStats、Abilities、Moves、Compat、Evolution、Form、TM 等），
  接入现有 `data-converter` 缓存与 golden 测试。
- Runtime `pokemon` 包：`Pokemon`（等级/经验/HP/攻防速特/性格/特性/招式/IV/EV/蛋/闪光/性别/
  昵称/道具/缎带）、`SpeciesData`/`FormData`/`MoveData`/`ItemData`/`AbilityData`/`NatureData`。
- 端口：`$Trainer`（姓名/性别/金钱/徽章/队伍/背包/PC）、`$PokemonBag`、`$PokemonGlobal`、
  `$PokemonMap`、`$PokemonSystem`；`pbSet`/`pbGet`。
- 消化目标：`$Trainer/$PokemonBag/$PokemonGlobal`、`pbSet/pbGet`、`pokemonCount`、
  `setAbility/calcStats/setItem/setNature`、`makeShiny/makeSuperShiny/makeMale/makeFemale`、
  `pbReceiveItem` 等（≈300 块）。
- 验收：HP/六维/经验曲线/性格修正单测；PBS 解析 golden；`build-data` pending 数下降。

### P1 队伍 / 背包 / PC
- `Party`（6 只、加删换位、治疗、蛋、首位、跟随位）、`Bag`（分栏/数量/关键物品）、
  `Storage`（PC 盒子）。
- 处理器：`pbAddPokemon`、`pbStoreItem`、`pbDeleteItem`、`pbGetKeyItem`、`pbLearnMove`、
  `myAddEgg`、`pbCrystalWarp`、`pbSetPokemonCenter`、`pbPokeCenterPC`、`pbShowMap`、
  `pbChoosePokemonForTrade`、`pbStartTrade`、`pbRegisterPartner`/`pbDeregisterPartner` 等（≈350 块）。
- 验收：队伍/背包/PC 单测；存读档包含队伍与背包（扩展 `SaveManager`）；CI 绿。

### P2 遇敌与战斗（最大批次）
- 数据：野生遭遇表（地图 metadata）、trainers.txt、`setBattleRule`、`pbTrainerIntro`。
- 触发器：草/洞步数遇敌、**碎岩**（`pbSmashThisEvent`+`pbRockSmashRandomEncounter` 310 块）、
  训练家视线（阶段 2 已做）与战前对话。
- 战斗引擎子集：回合、属性相克、能力/状态异常、道具/换人/逃跑、伤害与命中公式、
  经验/升级/学招、捕捉、胜负结算、`pbFreeWildBattle`/`pbWildBattle`；双打先留接口。
- UI：战斗界面（Battlebacks/UI 素材）、血条/招式菜单/提示。
- 验收：公式单测（伤害/命中/经验）、固定种子回放、探针（遇敌→胜→回地图 / 败→白屏回中心）。

### P3 培育：升级 / 进化 / 生蛋 / 缎带
- 升级学招、进化（等级/道具/条件/时间）、生蛋（`myAddEgg`）、缎带（`giveRibbon`）、
  闪光/性别/性格工具（`makeShiny` 等）。
- 验收：进化条件矩阵单测、蛋孵化流程探针。

### P4 菜单域（含 L15 暂停菜单视觉 + Essentials 菜单脚本）
- 宝可梦菜单（详情/招式/道具/交换）、背包、图鉴（seen/owned）、训练家卡、PC 存储、
  交易界面（`pbStartTrade`/`pbChoosePokemonForTrade`）。
- 架构：Essentials 的 `PScreen_*` 由事件脚本调用——逐屏映射为运行时 UI；**L15**（MPM 横幅/
  训练家立绘/模糊地图快照/滚动条/顶部信息）在这一批里做。
- 验收：截图探针（每屏一张）+ 模型单测；菜单脚本 pending 数下降。

### P5 插件与可选小特性（可穿插、按需排序）
| 特性 | 数据规模 | 说明 |
|---|---|---|
| 跟随宝可梦 | `pbPokemonFollow` 6 · `pbAddDependency2` 15 · `pbRemoveDependencies` 28 | 队伍首位跟随/多跟随 |
| 任务系统 | `advanceQuestToStage` 6 | `QuestLog` 阶段 2 已有一半 |
| 天气 | `weather`/`PBFieldWeather` 17 | 全屏天气图+音效，独立小批次 |
| 老虎机 | `pbSlotMachine` 24 | 小游戏 |
| Boss 奖励 | `boss_reward` 20 等 | 项目私有插件（54 块） |
| 时间/杂项 | `pbSetEventTime`、`pbCrystalWarp`、`giveRibbon` | 零散 API |
| 其它已列 | `pbToneChangeAll` 4、事件侧视线钩子（NPC 转身发现玩家） | L6 遗留 |

### P6 触摸（L2）+ 真机复核
- 虚拟 D-pad / A-B / 菜单键：**按键集合在 P4 菜单定型后再加**（用户原话），
  适配 `InputManager` 的新输入源，Android 真机复核 H1/H2 + 音频 I3。

### P7 收敛与发布
- 未支持脚本再分类：扩展脚本翻译器处理复杂表达式（26+）、`new` 17、`_I` 37 等；
  目标 `translated + handler ≥ 99%`（剩余为可解释的插件私有）。
- 发布：Git ReLeases 挂 `PokemonGame-Android*.apk` / 桌面便携包；版本 tag 与 CI 产物对齐。

## 3. 跨阶段基建（每批都要做）

- **数据**：`build-data` 全量重建 + pending 计数快照（`build/reports/*`）作为回归指标。
- **测试**：Java 单测（模型/公式）+ Builder golden（PBS/IR）+ 探针（真实 `MapScreen` 无头驱动）。
- **CI**：GitHub Actions 每次 push 跑 Builder/Compiler/Core/桌面/Android（已绿），PR/分支同样触发。
- **文档**：每批更新 `docs/`（本文件 + `stage2-report.md` 的后续篇章）与 `project1/` 记录。

## 4. 风险与开放问题

1. **战斗保真**：Essentials 的伤害/状态公式分支极多 —— 先用"可玩子集 + 单测锚点"，再逐步补齐。
2. **菜单架构**：`PScreen_*` 屏幕脚本与运行时 UI 的对应关系需要一次专门的架构设计（P4 开工前）。
3. **素材覆盖**：战斗 UI/Battlebacks/图鉴图标是否齐全要清点（缺失则降级或占位）。
4. **性能**：Android 上战斗/菜单的渲染预算；沿用现有 FBO 截图探针做基线。
5. **范围**：891+498 块体量最大，严格按 P0→P1→P2 的顺序才能让"多数事件立刻可玩"。
6. **存档兼容**：队伍/背包/PC 引入后存档结构变化，需要版本字段与迁移策略。

## 5. 里程碑验收

- M1（P0+P1）：能拿到宝可梦、队伍/背包/PC 可用，`pbAddPokemon` 等脚本生效；
  存读档含队伍。
- M2（P2）：草丛遇敌、训练家对战、胜负结算、碎岩遇敌可用。
- M3（P3+P4）：可升级/进化/生蛋；菜单/图鉴/背包/训练家卡/PC 界面完成（含 L15 视觉）。
- M4（P5+P6）：插件特性按需交付；触摸与 Android 真机复核通过。
- M5（P7）：覆盖率 ≥99%，Releases 可发布。

## 6. 与遗留清单的对应

| 遗留项 | 归属 |
|---|---|
| L7（891 JAVA_HANDLER_REQUIRED） | P0–P5 |
| L2 触摸 | P6（依赖 P4 的按键集合） |
| L15 暂停菜单视觉 | P4 |
| 天气 17 / pbToneChangeAll 4 / 事件侧视线钩子 | P5 |
| Android 真机 H1/H2/I3 | P6 |
| 未支持 498 的复杂表达式 | P7 |
| 阶段 2 待用户验收 G/H/I/L14 | 随时（与阶段 3 并行） |
