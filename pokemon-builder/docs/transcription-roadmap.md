# 转译补全路线图（2026-10-08）

依据：`docs/untranslated-audit.md`（缺口与近似实现的核对结果）。战斗、AI、Boss、双打/三打已完成，不在本表。

## 工作规则（沿用工程铁律）

1. 一切以插件为准；逐行转译 Ruby 原文，注释里写 `段名:行号`；不自创控制流、数值、文案。
2. 无法转译（缺素材/缺数据/依赖未做的子系统）→ 立刻停止该批次并报告，不绕过、不降级；确实留下的缺口用 `登记` 标记。
3. 小批次提交；每批带测试；不开 PR 除非你说。
4. 玩法优先看"游戏实际用到多少次"：用量为 0 的不做（阶段 11 已删除）。
5. 测试：`cd runtime && gradlew.bat --offline :core:test -q`。中文路径下 Gradle 会找不到类，先 `subst X: "…\pokemon-builder"` 再从 `X:\runtime` 跑。

## 阶段 0：准备（半天）

| 项 | 内容 |
|---|---|
| 0.1 | 把非战斗的插件段导出到 `plugin-src/ruby/`（现在只有战斗相关 56+3 个段）：`PField_*`、`Game_*`、`Interpreter`、`PItem_*`、`PScreen_*`、`Pokemon_*`、`PMinigame_*`、`Follower_*`、`002_Quest_*`、`DiegoWT…`、`Fly Animation`、`Egg Hatcher`、`PBDayNight` 等，云会话才能对照转译 |
| 0.2 | 状态存档升级机制：新增大量 `$PokemonGlobal/$PokemonTemp` 字段前，先确认 `StateVersion`/`SaveManager` 的迁移写法，保证旧存档能读 |
| 0.3 | 建一个 `GlobalState`（`$PokemonGlobal`/`$PokemonMap`/`$PokemonTemp`/`$PokemonSystem` 的 Java 对应）作为后续阶段共用的字段容器，字段随各阶段逐个加，不一次性造空壳 |

## 阶段 1：事件解释器缺的指令与脚本（最优先，改动小、影响大）

| 项 | 用量 | 插件/出处 | Java |
|---|---|---|---|
| 1.1 指令 314 完全恢复（精灵中心护士） | 196 | RMXP 314 | `EventInterpreter`，回复全队 HP/PP/状态 |
| 1.2 指令 118/119 标签与跳转 | 39 / 92 | RMXP | 解释器指令指针按标签重定位 |
| 1.3 指令 125 更改金钱 | 47 | RMXP | `TrainerState.money` |
| 1.4 指令 247/248 记忆/还原 BGM·BGS | 20 / 21 | RMXP | `AudioManager` |
| 1.5 指令 236 设置天气（先登记，等阶段 10 做渲染） | 7 | RMXP | 先落状态 |
| 1.6 指令 354 / 246 / 103 | 4 / 1 / 1 | RMXP | 回标题、BGS 渐出、输入数值 |
| 1.7 指令 234 更改图片色调（现在只告警） | 99 | RMXP | `PictureService` |
| 1.8 脚本：`setVariable(:ITEM)`、`get_character(n).setTempSwitchOn`、`pbGet`、`pbSetEventTime`、`pbUnlockDex`、`pbChangePlayer`、`pbChooseNonEggPokemon`、`pbChoosePokemon`、`pbChooseFossil`、`pbConvertItemToPokemon`、`pbTrainerPC`、`pbMessage_ex`、`insert`、`party`、`pbEnd`/`pbStart` | 各 1～10 | `Interpreter` 等 | `generate`/`ScriptIr` 增加命令 |
| 1.9 自定义插件脚本：`pbCrystalWarp`、`pbMrHyper`、`changeBalls`、`pbChangeShinyByNPC`、`resurrection2`、`teachEggMoves`、`pbGiveAllMemories`、`pbTalkToFollower`、`battleOverlordflos`、`battleIronJugulis`、`hasRibbon?`、`isConst?` | 各 1～12 | 段 360、359、365、362、351、… | `ScriptIr` |

## 阶段 2：地图与玩家基础

| 项 | 内容 |
|---|---|
| 2.1 **柜台属性** | `Game_Map:294 counter?`（passage 0x80）+ `Game_Player:160/282` 隔柜台开事件；`MapScreen.startFacingEvent` 与 `SightTriggers` 一起改 |
| 2.2 `$PokemonGlobal` 移动态 | `surfing / diving / bicycle`、`surfJump`、`bridge` 联动；`Game_Player` 的上下水、碰撞切换、移动速度 |
| 2.3 角色可视 | `Game_Player_Visuals`（冲浪/潜水/骑车 charset）、`Sprite_SurfBase`、`Sprite_WaterReflection`（水面倒影） |
| 2.4 地形标签补全 | `PBTerrain` 14 个判断（Ice 滑行、Waterfall、Sand、WaterfallCrest 等）逐个核对 `Collision`/`TileMap` |
| 2.5 自动存档 | `001_MSF_AutoSave`（`autosave_steps`） |

## 阶段 3：训练家/背包/图鉴状态

| 项 | 内容 |
|---|---|
| 3.1 背包 | `PItem_Bag` 容量 `pbCanStore?`/`pbStoreItem`（现在恒可存放）、口袋名与排序、`pbQuantity` 一致 |
| 3.2 硬币盒与硬币 | `$PokemonGlobal.coins`、`Messages` 金钱/硬币窗口 |
| 3.3 图鉴 | `pokedexDex/Viable/Unlocked/Mode/Index`、`formseen/formlastseen`、多地区图鉴、字首拼音排序（段 331） |
| 3.4 训练家 | `startTime`（训练家卡）、`cardlevel`、`mysterygift`、`rivalName` |
| 3.5 邮件 | `PItem_Mail`、`$PokemonGlobal.mailbox`、队伍与电脑里的邮件选项 |

## 阶段 4：野外遇敌重写（对照 `PField_Encounters` 全文）

4.1 `pbEncounterType`（水面/洞穴/草地/早晨白天夜晚/虫赛）；4.2 `pbCanEncounter?`（驱虫喷雾 + 等级过滤、`encounter_disabled`）；4.3 `pbEncounteredPokemon`（静电/磁力等偏好、黑白笛修正）；4.4 `pbGenerateEncounter`/`pbGenerateWildPokemon`（已有，复核）；4.5 `PField_EncounterModifiers`；4.6 `PBDayNight.isDay/isNight/isMorning` 供遇敌用；4.7 野外双打（`isDoubleWildBattle?`）；4.8 `$PokemonTemp.encounterType/forceSingleBattle/encounterTriggered`；4.9 `Rock Smash`/头锤触发遇敌复核。**前置：阶段 2.2（水面）。**

## 阶段 5：道具野外使用（对照 `PItem_ItemEffects` 全文，83 个对宝可梦、22 个野外、12 个背包直用）

| 子项 | 内容 |
|---|---|
| 5.1 恢复类（已有）复核 | HP/状态/复活；`pbHPItem`、`pbBattleHPItem` 已在 `188_PItem_Items`，核对提示文字 |
| 5.2 PP 类 | Ether/Max Ether/Elixir/Max Elixir/PP Up/PP Max |
| 5.3 能力提升 | HP Up/Protein/Iron/Calcium/Zinc/Carbos、各种羽毛、树果降 EV、神奇糖果、经验糖 |
| 5.4 形态/特性/其他 | Ability Capsule、Gracidea、Reveal Glass、DNA 融合器、日月光、Rotom 目录、Lonely Mint 系列、Prison Bottle |
| 5.5 野外使用 | 驱虫喷雾/超级/极致/无限、黑白笛、圣灰、蜂蜜、逃脱绳/无限绳（带确认）、探宝器、提灯、镇图地图、硬币盒、学习装置(ExpAll) |
| 5.6 背包直用 | 自行车(含 `UseText`)、鱼竿、Eon 笛、以太星等伪道具 |
| 5.7 TM/HM 使用 | `pbLearnMove` 通用版 + 招式遗忘界面 |
| 5.8 背包"对宝可梦使用"流程 | `PartyView.pbUseItem` 登记项 |

## 阶段 6：商店、精灵中心与电脑

| 项 | 内容 |
|---|---|
| 6.1 **精灵商店** `PScreen_Mart`（73 处 `pbPokemonMart`） | `OPEN_MART` + 商店界面（买/卖、`setPrice`、`getMoneyString`） |
| 6.2 精灵中心 | `pbSetPokemonCenter`（已有）+ 护士事件整套（依赖 1.1） |
| 6.3 电脑道具存取 | `PScreen_ItemStorage`（Withdraw/Deposit/Toss） |
| 6.4 电脑起名与搜索 | 把已有的 `TextEntryView` 接入 `StorageView`；`seenStorageCreator` |
| 6.5 `Pokemon_Storage` 复核 | `pbStoreCaught`、`formTime` |
| **6.6 寄存系统全面核对（已知缺陷，高优先级）** | **症状**：电脑盒子界面里，玩家携带的宝可梦会一直画在盒子区域上（2026-10-08 截图：盒子 1 的草地背景上散落着队伍里的宝可梦图标，位置与格子不对齐）。**要查**：① 这些图标是不是队伍精灵被误画到盒子层（对照 `PokemonStorageScene#pbRefresh`、`PokemonBoxSprite`、`PokemonBoxPartySprite` 的显示/隐藏时机：队伍面板只在「队伍」模式或拿起宝可梦时出现）；② 盒子格子的坐标与图标锚点（`pbSetArrow`、`PokemonBoxSprite#refresh` 的格子偏移）；③ 切换盒子、箭头、选择模式、搜索、`pbHeldPokemon` 的渲染顺序与释放。**范围**：`StorageView`（对照 `PScreen_PokemonStorage` 1983 行 + `B2W2 PC` 2347 行）逐函数对照，连同 6.3/6.4 一起收口，并补渲染层的测试（至少断言「默认状态下队伍精灵不在盒子层」） |

## 阶段 7：秘传招式与移动方式（`PField_FieldMoves`，33 个函数）

| 子项 | 内容 | 前置 |
|---|---|---|
| 7.1 `HiddenMoveHandlers` 框架 | `addCanUseMove/addConfirmUseMove/addUseMove`、`pbCanUseHiddenMove?`、徽章检查、`pbHiddenMoveAnimation`；接入队伍菜单 | 无 |
| 7.2 居合斩 Cut、碎岩 Rock Smash、头锤 Headbutt、怪力 Strength（使用） | 7.1 | 7.1 |
| 7.3 闪光 Flash + 洞穴黑暗 | `darknessSprite`、`flashUsed` | 7.1 |
| 7.4 冲浪 Surf、潜水 Dive、攀瀑 Waterfall | `pbSurf/pbStartSurfing/pbEndSurf`、`pbDive/pbSurfacing/pbTransferUnderwater`、`pbAscend/DescendWaterfall` | 2.2、2.3、7.1 |
| 7.5 瞬间移动/挖洞/甜甜香气/除雾 | `escapePoint`、`pbSweetScent`、`pbDefog` | 7.1、阶段 4 |
| 7.6 **飞空术** | `Fly Animation`（段 340）、`PScreen_RegionMap` 飞行界面（`pbStartFlyScreen`）、`flydata` | 7.1 |
| 7.7 **自行车** | 上下车、速度、禁用区、`PField_Encounters` 的骑车判断 | 2.2、5.6 |
| 7.8 **钓鱼** | 三种鱼竿、等待与咬钩、`pbFishing`、触发遇敌 | 阶段 4 |

## 阶段 8：成长与进化

| 项 | 内容 |
|---|---|
| 8.1 **进化全套** | 重写 `PokemonGrowth` 对照 `Pokemon_Evolution`（909 行）：`pbCheckEvolutionEx` 全方法、昼夜进化、道具/交换/地点/亲密度/性别/招式进化、`pbGetBabySpecies`/`pbGetPreviousForm`、进化家族 |
| 8.2 进化界面 | `PScreen_Evolution`（30/42 函数）对照复核 |
| 8.3 战后进化 | `$PokemonTemp.evolutionLevels` + `PField_Battles` 战后检查 |
| 8.4 **选御三家** | `DiegoWT's Starter Selection`（段 333，565 行）的场景与 `pbStartChoosing` 流程 |
| 8.5 **招式回忆师/教学师** | `PScreen_MoveRelearner`、`pbRelearnMoveScreen`、`teachEggMoves`；队伍菜单里的"招式"回忆复核 |
| 8.6 孵蛋 | `PScreen_EggHatching`、`Egg Hatcher`（段 323）、`myAddEgg` |
| 8.7 **托儿所** | `PField_DayCare`（13 个函数）：存放/取回/产蛋/步数/等级成长/`pbEggGenerated`；事件 `pbDayCare*` |
| 8.8 缎带 | `PBRibbons`、`giveRibbon/hasRibbon?/upgradeRibbon`、摘要页缎带 |
| 8.9 走路效果 | 亲密度步数 `happinessSteps`、中毒走路扣血与晕厥（`PField_Field`）、`pbPokerus` 传播 |
| 8.10 名人堂 | `PScreen_HallOfFame` + `pbHallOfFameEntry` + `hallOfFame` 记录 |

## 阶段 9：同伴与跟随事件

`PField_DependentEvents`（32 个函数）、`Follower_Main`（58）、`Follower_Config`：`pbAddDependency2/pbRemoveDependencies/pbRemoveDependency2/pbTalkToFollower`、跟随者出入队、`followerToggled`、`$PokemonTemp.dependentEvents`、`come_back`、地图切换时跟随者位置。**前置：阶段 2。**

## 阶段 10：时间、天气与环境视觉

| 项 | 内容 |
|---|---|
| 10.1 `PField_Weather` | `$game_screen.weather` 全部（雨/雪/沙暴/阳光/大雨…）、`pbWeather` 渲染（`ParticleEngine`、`Game_Screen`） |
| 10.2 `PField_Time` | `pbSetEventTime`、日夜判断、`isRainbow/moonphase/zodiac`；与遇敌共用 `PBDayNight` |
| 10.3 环境视觉 | `Sprite_DynamicShadows`、`Tilemap_Perspective`、`PField_Visuals` 剩余项 |

## 阶段 11：已删除

低用量系统（流浪宝可梦、宝可雷达、手机、野生区、虫赛、随机迷宫、对战工厂等）经核对游戏数据没有引用，不做。

## 阶段 12：小游戏与可选界面

老虎机（24 处调用，最优先）、Voltorb Flip、Triple Triad（买卖卡牌）、彩票(`pbLottery/pbSetLotteryNumber`)、挖矿(`pbMiningGame`)、拼图、决斗；任务面板 `003_Quest_UI` + `004_Quest_Data`（1684 行）；训练家卡徽章页 `PokemonTrainerCardScreen`、`Scene_Credits`、`PScreen_Jukebox`、`PScreen_ReadyMenu`；神秘礼物。

## 阶段 13：近似实现收口（对照原文复核）

`WildEncounters` 被阶段 4 替换；`PokemonGrowth` 被阶段 8 替换；`ItemUse` 被阶段 5 替换；`QuestLog` 补任务定义；`Pokemon.getMoveList` 形态招式表；`TrainerView.startTime`；`DexEntryView` Area 页（地区地图数据）；`MoveRoutePlayer` 逐码复核；`PartyView/StorageView/PokeCenterPcView` 的 64 处 `登记` 逐项清零；淡入淡出 `pbFadeInAndShow/OutAndHide/OutIn`；`Messages`/`TextEntry`/`MessageConfig` 缺的函数；`Audio_Utilities`。

## 阶段 14：回归与总核对

- 重跑 `untranslated-audit` 的三个统计：事件脚本覆盖率（现 96.4%）、事件指令码未处理清单（现 11 个）、全局变量缺口（现 ~147 个）。
- 用真实存档 + 真实地图走一遍：新游戏→御三家→精灵中心→商店→捕捉→进化→冲浪/飞空/自行车/钓鱼→托儿所→名人堂。
- 更新 `docs/*-handoff.md`。

## 与 CFRU AI 的关系

CFRU 训练家 AI 已完成，交接文档里的小项（Role Play 评分、MURKYMIST/ABYSSSHADOW 保持原样、CFRU 独有项未转写）不在本路线图内。

## 建议的起手顺序

1 → 2.1 → 6.1 → 8.4 → 4（含 2.2）→ 5 → 7（框架→切/碎/怪力→冲浪→飞空→自行车→钓鱼）→ 8 余项 → 9 → 10 → 12 → 13 → 14。
理由：1、2.1、6.1、8.4 是剧情流程里会直接卡住玩家的；4、5、7 互相依赖；9 依赖 2。
