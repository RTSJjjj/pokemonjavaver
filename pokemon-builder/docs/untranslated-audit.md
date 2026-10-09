# 未转译内容核对（2026-10-08）

方法：① `build/reports/script-coverage.json`（事件脚本块的翻译结果）；② 统计所有 `Map*.rxdata` / `CommonEvents.rxdata` 实际用到的 RMXP 事件指令码，对照 `event/` 包里有没有对应的 `case`；③ 对插件 384 个脚本段逐个 `def` 检查 Java 里有没有对应实现，再对可疑项用 `grep` 在 `runtime/**/src/main` 里人工复核。
"未实现"指在 Java 里找不到任何对应代码（含别名），不是"实现了但有问题"。战斗、AI、Boss、双打/三打不在本表，它们另有交接文档。

## 一、地图上会直接撞到的（优先）

| 项目 | 游戏里的用量 | Java 现状 | 插件出处 |
|---|---|---|---|
| **事件指令 314「完全恢复」** | **196 次**（精灵中心护士等） | 解释器没有 `case 314`：回复动作不会发生 | RMXP 事件指令 |
| 事件指令 119「跳转标签」/118「标签」 | 92 / 39 次 | 没有 `case`，分支跳转不生效 | RMXP 事件指令 |
| 事件指令 125「更改金钱」 | 47 次 | 没有 `case` | RMXP 事件指令 |
| 事件指令 247/248「记忆/还原 BGM」 | 20 / 21 次 | 没有 `case` | RMXP 事件指令 |
| 事件指令 236「设置天气」 | 7 次 | 没有 `case` | RMXP 事件指令 |
| 事件指令 354 / 246 / 103 / 408 | 4 / 1 / 1 / 12 次 | 没有 `case`（408 是注释续行，可忽略） | RMXP 事件指令 |
| **柜台属性（地图图块 passage 0x80）** | 精灵中心、商店等柜台后的 NPC | `startFacingEvent`（`MapScreen.java:1032`）只看面前一格；没有 `counter?` 判断，隔着柜台说不了话 | `Game_Map:294`、`Game_Player:160/282` |
| **精灵商店 `pbPokemonMart`** | 73 次 | 脚本能翻成 IR `OPEN_MART`，但 `EventInterpreter` 里没有这个 `case`，也没有商店界面 | `PScreen_Mart`（935 行） |
| **精灵中心回复** | 56 个 `pbSetPokemonCenter` | 复活点（`SET_POKEMON_CENTER`）已有；回复动作依赖上面的指令 314；护士动画/对话在事件里，随 314 一起补 | `PField_Field`、事件指令 |
| **选御三家** `DiegoWTsStarterSelection.new(...)` | 1 | 脚本无法翻译（`new` 没有处理器），Java 里没有该场景 | 段 333（565 行） |
| 训练家卡徽章页 `PokemonTrainerCardScreen` / `Scene_Credits` | 各 1 | 脚本无法翻译 | 段 300、80 |
| 天气 `$game_screen.weather(...)` | 17 | 无处理器（`PField_Weather` 14 个函数里 7 个有对应） | `PField_Weather`、`Game_Screen` |
| 同伴/跟随事件 `pbRemoveDependencies`、`pbAddDependency2/pbRemoveDependency2` | 25 / 9 / 1 | 无处理器（`PField_DependentEvents` 32 个函数只对上 6 个） | `PField_DependentEvents` |
| 老虎机 `pbSlotMachine` | 24 | 无处理器 | `PMinigame_SlotMachine` |
| `pbCrystalWarp` | 12 | 需要 Java 处理器，还没写 | 段 360 |
| 奖励缎带 `giveRibbon` / `hasRibbon?` | 11 / 1 | 无处理器 | `PBRibbons`、`PokeBattle_Pokemon` |
| `pbSetEventTime` | 6 | 无处理器 | `PField_Time` |
| 名人堂 `pbHallOfFameEntry` | 5 | 无处理器（`PScreen_HallOfFame` 33 个函数只对上 10 个） | `PScreen_HallOfFame` |
| `setVariable(:ITEM)` | 5 | 无处理器 | 项目插件 |
| `get_character(n).setTempSwitchOn` | 5 | 无处理器 | `Interpreter` |
| 托儿所 `pbDayCare*`（存放/取回/产蛋/选择） | 约 8 块 | 无处理器，整个 `PField_DayCare`（13 个函数）一个都没有 | `PField_DayCare`（580 行） |
| `pbUnlockDex`、`pbChangePlayer`、`pbChooseNonEggPokemon`、`pbChooseFossil`、`pbConvertItemToPokemon`、`pbTrainerPC`、`pbMiningGame`、`pbVoltorbFlip`、`pbLottery`、`pbSetLotteryNumber`、`pbBuyTriads`/`pbSellTriads`、`pbReceiveMysteryGift` | 各 1～3 | 无处理器 | 各自的 `PMinigame_*`/`PScreen_*` |
| `pbMrHyper`、`changeBalls`、`pbChangeShinyByNPC`、`resurrection2`、`teachEggMoves`、`pbGiveAllMemories`、`pbTalkToFollower` | 各 1 | 标为"需要 Java 处理器"，未写 | 项目插件 |

事件脚本整体：5441 块里 5202 块已翻译（96.4%），196 块不支持，43 块待 Java 处理器。

## 二、玩家主动操作的系统（Java 里整套不存在）

| 项目 | 现状 | 插件出处 |
|---|---|---|
| **秘传招式**（居合斩 Cut、冲浪 Surf、怪力 Strength 的使用、飞空术 Fly、碎岩 Rock Smash、头锤 Headbutt、攀瀑 Waterfall、潜水 Dive、闪光 Flash、瞬间移动 Teleport、甜甜香气 Sweet Scent、除雾 Defog） | `HiddenMoveHandlers` 没建模（`PartyView.java:635` 自己登记过，`hasHiddenMoveHandler` 恒为 false）。只有两处沾边：`pbPushThisBoulder` 靠 `strengthUsed` 状态推石头（`EventInterpreter.java:1485`）、岩石破碎遇敌（`ROCK_SMASH_ENCOUNTER`） | `PField_FieldMoves`（33 个函数只对上 7 个，且多是名字巧合） |
| **冲浪状态** | 没有 `surfing` 状态、上下水、水面碰撞切换 | `PField_FieldMoves:702-754`、`PBTerrain` |
| **自行车** | 只有 `bikeCharset` 配置字段和骑行速度的注释，没有上下车、速度、禁用区 | `PItem_ItemEffects`（BICYCLE） |
| **钓鱼** | 没有鱼竿使用和钓鱼遇敌 | `PItem_ItemEffects`（SUPERROD 等）、`PField_Encounters` |
| **道具野外使用** | `ItemUse.java` 只有药品/复活类；`PItem_ItemEffects` 里 83 个对宝可梦的用法缺 47 个：PP 类（Ether/Elixir/PP Up/PP Max）、能力提升（HP Up/Protein/Iron/Calcium/Zinc/Carbos、各种羽毛、树果降 EV）、神奇糖果 RARECANDY、经验糖、特性胶囊、形态道具等。`UseFromBag`/`UseInField` 22 项全缺：驱虫喷雾、逃脱绳、探宝器、提灯、镇图地图、硬币盒、学习装置、黑白笛、圣灰、蜂蜜 | `PItem_ItemEffects`（1820 行） |
| **中毒走路扣血、驱虫喷雾步数、遇敌率修正** | 没有 | `PField_Field`、`PField_Encounters` |
| **招式学习/回忆** | 队伍菜单"招式"里有回忆（`PartyView.java:612`），但没有独立的招式回忆师场景，事件里也没有调用；招式教学师（Move Tutor）整套没有 | `PScreen_MoveRelearner`、`PField_Field` |
| 学会招式 `pbLearnMove` | Boss 奖励里有，通用版本有 | — |
| 流浪宝可梦（Roaming） | 只有零星痕迹 | `PField_RoamingPokemon`（8 个函数没有） |
| 手机、宝可雷达、邮件 | 没有 | `PItem_Phone`、`PItem_PokeRadar`、`PItem_Mail` |
| 随机迷宫 | 没有 | `PField_RandomDungeons` |
| 野外时间/昼夜 | `DayNightTone` 有色调；`PField_Time` 29 个函数只对上 10 个 | `PField_Time`、`PBDayNight` |
| 精灵球/图鉴等小件 | 图鉴（`PScreen_PokedexMain`）、地区地图、背包、存档等已有 | — |

## 三、小游戏与可选系统

老虎机（24 处调用，最多）、Voltorb Flip、Triple Triad（买卖卡牌）、彩票、挖矿（Mining）、拼图（TilePuzzles）、决斗（Duel）、神秘礼物、Pokémon Transporter、Boonzeet 栖息地列表、Chain Catching、BW 风格图鉴。这些在 Java 里都是零星或没有。

## 四、不需要转译的段

Editor_*、Compiler*、Debug_*、RMXP Event Exporter、FastMapExporter、Sockets、Win32API、Plugin_Manager 等是开发/编辑器工具，运行时不用。

## 五、建议的顺序

1. 解释器补指令：314 完全恢复、119/118 标签跳转、125 金钱、247/248 BGM 记忆/还原、236 天气（改动小，影响面最大）。
2. 柜台属性（`startFacingEvent` 里补 `counter?`）。
3. 精灵商店 `OPEN_MART` + `PScreen_Mart`。
4. 选御三家场景。
5. 同伴/跟随事件、天气 `game_screen.weather`、托儿所。
6. 秘传招式整套（先建 `HiddenMoveHandlers` 框架，再逐个招式，冲浪/自行车/钓鱼各自带状态）。
7. 道具野外使用（PP、能力提升、神奇糖果、驱虫喷雾、逃脱绳……）。
8. 老虎机等小游戏、名人堂、缎带。

## 复核方法

```text
事件脚本：   pokemon-builder/build/reports/script-coverage.json
指令码统计： 用 builder/src/marshal.js 解析 Data/Map*.rxdata + CommonEvents.rxdata，统计 @code
插件源码：   pokemon-builder/plugin-src/ruby/（只含战斗相关段）；其余段用 tools/scanner 的 readScriptsSection 现场导出
```

---

# 附：做了"差不多"的部分与缺失的状态变量（2026-10-08 追加）

判断依据：① 源码里自己写了"未建模/近似/占位/only"的位置；② 同一功能的 Java 行数与插件原文行数差距大、且没有对应的插件行号引用；③ 插件里读写的 `$PokemonGlobal / $PokemonMap / $PokemonTemp / $Trainer / $PokemonSystem` 字段，在 Java 状态里搜不到对应字段（先按名字搜，再用宽松关键词复核）。

## 一、早期版本留下的近似实现（没有逐行对原文）

| 功能 | Java | 插件原文 | 缺什么 |
|---|---|---|---|
| **野外遇敌** | `battle/WildEncounters.java`（112 行，自述 "Stage 3 / P2 … following the project's PokemonEncounters"） | `PField_Encounters`（482 行） | 只有 草地(Land)/洞穴(Cave)、步数密度、前 3 步安全、岩石破碎。**没有**：冲浪水面遇敌(`$PokemonGlobal.surfing`→Water)、Land 的早晨/白天/夜晚三种表(`LandMorning/Day/Night`)、虫子大赛表、驱虫喷雾与等级过滤(`pbCanEncounter?` 的 `repel` 判断)、黑白笛修正、特性偏好属性(静电/磁力/引火/收获/避雷针/引水 50% 倾向)、`pbPokeRadarOnShakingGrass`、双打野外(`isDoubleWildBattle?`)、`$PokemonTemp.encounterType/forceSingleBattle` |
| **升级后进化** | `pokemon/PokemonGrowth.java`（127 行，注释自述 LevelDay/LevelNight 当作 Level） | `Pokemon_Evolution`（909 行）+ `PScreen_Evolution` | 只处理 等级/亲密度。无白天/夜晚进化、无进化家族查询（`pbGetPreviousForm/pbGetBabySpecies/pbGetMinimumLevel`）、`pbCheckEvolutionEx` 的全部方法分支只做了一小部分；`$PokemonTemp.evolutionLevels`（战斗后统一进化）整个没有 |
| **野外道具使用** | `pokemon/ItemUse.java`（214 行） | `PItem_ItemEffects`（1820 行） | 只有药品/状态药/复活；PP 类、能力提升类、糖果、羽毛、树果降 EV、驱虫、逃脱绳、自行车、鱼竿、探宝器、提灯等见上文 |
| **任务系统** | `state/QuestLog.java`（自述 "tracking the state here is enough to make 331 script calls work"） | `002_Quest_Main`（20 个函数）、`003_Quest_UI`、`004_Quest_Data`（1684 行任务数据） | 只记状态，没有任务面板界面，没有任务定义文本/阶段文字 |
| **队伍学习招式** | `pokemon/Pokemon.java:112` 自述 "getMoveList is species.moves; a form's own list is not modelled" | `PokeBattle_Pokemon#getMoveList` | 形态各自的招式表不生效（`pokemonforms.json` 里有） |
| **训练家卡** | `ui/menu/TrainerView.java:179` | `B2W2 Trainer Card` | `$PokemonGlobal.startTime` 未建模，开始时间栏显示当前日期 |
| **图鉴 Area 页** | `ui/menu/DexEntryView.java:413` | `PokemonPokedexInfo_Scene` | 地区地图数据未建模，栖息地/出现地点页不可用 |
| **事件指令 234** | `EventInterpreter.java:489` | RMXP | 更改图片色调：只告警跳过（99 次使用） |

## 二、界面里自己"登记"为未建模的功能（`登记` 标记）

`PartyView`（39 处）、`StorageView`（19 处）、`PokeCenterPcView`（6 处）共通的缺口：

- 淡入淡出过渡 `pbFadeInAndShow/pbFadeOutAndHide/pbFadeOutIn`：全部省略。
- 队伍菜单：背包"对宝可梦使用"流程 `pbUseItem`、`HiddenMoveHandlers`、飞行界面 `pbStartFlyScreen`、招式回忆 `pbRelearnMoveScreen`、跟随者 `dependentEvents.come_back`、邮件、`$DEBUG` 命令。
- 电脑/寄存系统：给宝可梦/盒子起名（文字输入，`TextEntryView` 是新加的，尚未接入这里）、搜索、道具存取 `PCItemStorage/WithdrawItemScene/DepositItemScene/TossItemScene`、邮箱、`seenStorageCreator`、`formTime`（沙奈朵等形态时间重置）、背包容量（`pbStoreItem` 恒成功）。

## 三、插件里有、Java 状态里完全没有的全局变量

| 变量 | 插件用量 | 影响 |
|---|---|---|
| `$PokemonGlobal.surfing / diving / bicycle` 与 `$PokemonTemp.surfJump` | 38 / 24 / 24 / 8 | 冲浪、潜水、自行车整套状态 |
| `$PokemonGlobal.repel / infRepel`、`$PokemonMap.blackFluteUsed / whiteFluteUsed`、`$PokemonGlobal.happinessSteps` | 9 / 5 / 9 / 5 | 驱虫步数、笛子、走路加亲密度 |
| `$PokemonGlobal.escapePoint`、`$PokemonTemp.flydata`、`$PokemonGlobal.mapTrail`、`visitedMaps` | 15 / 17 / 17 / 5 | 逃脱绳、飞行、地图轨迹、去过的地图 |
| `$PokemonGlobal.flashUsed`、`$PokemonTemp.darknessSprite` | 6 / 11 | 闪光、洞穴黑暗 |
| `$PokemonGlobal.daycare / daycareEgg / daycareEggSteps` | 22 / 5 / 6 | 托儿所 |
| `$PokemonGlobal.roamPokemon / roamPosition`、`$PokemonTemp.roamerIndex` | 12 / 10 / 5 | 流浪宝可梦 |
| `$PokemonGlobal.phoneNumbers / phoneTime`、`$PokemonTemp.pokeradar`、`pokeradarBattery` | 22 / 7 / 26 / 7 | 手机、宝可雷达 |
| `$PokemonGlobal.coins`、`triads`、`mailbox` | 14 / 41 / 14 | 硬币盒/老虎机、卡牌游戏、邮件 |
| `$PokemonGlobal.hallOfFame` | 9 | 名人堂记录 |
| `$PokemonGlobal.pokedexDex / pokedexViable / pokedexUnlocked / pokedexMode / pokedexIndex`、`$Trainer.formseen / formlastseen` | 16 / 16 / 47 / 14 / 14 / 7 / 31 | 多地区图鉴、图鉴排序模式、形态已见记录 |
| `$Trainer.habitatData / habitatPokeIndexes / habitatMapIndexes`、`$PokemonTemp.habitatEncounters` | 23 / 10 / 5 / 7 | 栖息地列表 |
| `$Trainer.mysterygift`、`chainCatching`、`cardlevel`、`autosave_steps` | 11 / 7 / 5 / 5 | 神秘礼物、连锁捕捉、训练家卡等级、自动存档 |
| `$PokemonGlobal.trainerRecording / safesave / currentVersion`、`$PokemonTemp.begunNewGame` | 5 / 5 / 11 / 9 | 存档/读档辅助 |
| `$PokemonSystem.show_miniMap / miniMap_size / miniMap_zoom / miniMap_position / miniMap_opacity` | 12 / 10 / 10 / 8 / 5 | 小地图设置（ESMM） |
| `$PokemonTemp.heartgauges`、`$Trainer.exp_pot`（已有）、`evolutionLevels`、`nextBattleBack`、`menuLastChoice` | 5 / 5 / 6 | 暗影宝可梦心之槽、战后进化、战斗背景覆盖 |

已有、可以放心的：金钱、名字、性别、ID、徽章、图鉴开关、治疗点(`healMapId/X/Y`)、桥(`bridge`)、跟随开关(`followerToggled`)、同伴(`partner`)、`nextBattleBGM/ME`、`battleRules`、`waitingTrainer`、`pokemonMapStrengthUsed`。
