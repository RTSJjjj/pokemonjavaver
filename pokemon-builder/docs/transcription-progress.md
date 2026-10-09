# 转译补全进度（验收用）

对应 `transcription-roadmap.md`。每项都写了：改了什么、依据的插件位置、怎么验证。测试命令：

```text
cd /d X:\runtime && gradlew.bat --offline :core:test          (X: 是 subst 出来的 pokemon-builder)
node --test builder/tests/*.test.js                          (构建器，在 pokemon-builder 目录)
```

## 阶段 0 准备

| 项 | 状态 |
|---|---|
| 0.1 导出非战斗插件段到 `plugin-src/ruby/`（共 290 个段，索引见 `plugin-src/ruby/../INDEX.md`） | 完成 |

## 阶段 1 事件解释器

| 项 | 依据 | 实现与测试 |
|---|---|---|
| 指令 314 完全恢复（全队 + 全部电脑盒子，蛋不动） | `PField_Field:898-903`、`Pokemon_Storage:377-384`、`PokeBattle_Pokemon:769-774` | `EventInterpreter.recoverAll`、`Storage.eachPokemon`；`EventStage1CommandsTest` |
| 指令 118/119 标签与跳转（不扫描最后一条，找不到则继续） | `Interpreter:778-808` | `EventInterpreter.jumpToLabel`、`EventProgram.jumpTo`；含向前、向后、未知标签、末条标签 4 个测试 |
| 指令 125 更改金钱（钳在 0..999,999,999） | `PField_Field:876-880`、`PokeBattle_Trainer:73-75`、`Settings:96` | `changeGold`、`operateValue` |
| 指令 247/248 记忆/还原 BGM·BGS | `Interpreter:1361-1377`、`Game_System:122-129/220-226` | `AudioManager.memorizeBgmAndBgs/restoreBgmAndBgs` |
| 指令 246 BGS 渐出（登记：无音量渐变，直接停止） | `Messages:354-357`、`Game_System:210-214` | `AudioManager.fadeBgs` |
| 指令 234 更改图片色调（含逐帧渐变，40 帧/秒） | `Interpreter:1331-1339`、`Game_Picture:114-120/140-147` | `PictureService.tone`、`PictureLayer` 用 `ToneShader` 绘制 |
| 指令 236 设置天气（状态机；渲染留到阶段 10） | `Interpreter:1354-1359`、`Game_Screen:78-93/140-146`、`PBFieldWeather` | `state/ScreenWeather`、`GameState.weather()`、`MapScreen` 每帧更新 |
| 指令 354 回标题 | `Interpreter:1449-1454`、`Scene_Map:171-174` | `consumeTitleRequest`，`MapScreen` 检查 |
| 指令 103 输入数值（数字窗口：上下改位、左右移位、确认写回、取消保留原值） | `Messages:493-503/761-800`、`SpriteWindow_text:612-` | `MessageService` 数字模式、`updateNumberInput`、`MessageWindow.drawNumberInput` |
| **修复：变量条件分支忽略比较运算符** | `Interpreter:603-623` | 6 种运算符；游戏里 756 处「等于」曾被当成「大于等于」；新增金钱条件（34 处）与控制变量读 地图ID/金钱/游戏时间 |
| **修复：选项按 B 的结果** | `Messages:1370-1377` | 取消类型 N 时 B 选第 N-1 项（约 580 个是/否之类选项曾在按 B 后什么也不做）；类型 0 忽略 B |
| 脚本 `$game_screen.weather(...)`（17 处）、`get_character(n).setTempSwitchOn/Off`（5 处）、`pbToneChangeAll`（4 处） | `Game_Screen`、`Interpreter:400-412`、`PField_Visuals:691-696` | 构建器编译器 + 解释器 `SET_WEATHER / SET_TEMP_SWITCH(eventId) / TONE_CHANGE_ALL`；新增 `PBFieldWeather::X` 常量与 Ruby 浮点字面量解析 |

事件脚本覆盖率：5202 → 5228 块已翻译（96.4% → 96.9%）。

## 阶段 2

| 项 | 依据 | 实现与测试 |
|---|---|---|
| 2.1 柜台属性（passage 0x80）：面前是柜台就看再远一格的事件 | `Game_Map:294-301`、`Game_Player:279-312` | `TileMap.counter`、`MapScreen.startFacingEvent`；`CounterTest` |

## 阶段 6

| 项 | 依据 | 实现与测试 |
|---|---|---|
| 6.1 **精灵商店**（`OPEN_MART`，73 处）：问候与「购买/卖东西/退出」、购买列表（9 行可见、光标居中滚动、环绕）、数量窗口（左右 ±10、上下 ±1、环绕）、确认窗口、买够 10 个精灵球送纪念球、关键道具买后下架、卖东西（用背包场景挑选，卖价 = 价格/2×数量）、事件里的 `setPrice` / `setSellPrice`（改价；卖价 0 = 不收） | `PScreen_Mart:4-82/278-846/850-905` | `ui/menu/MartView`（界面状态机）、`MartRules`（价格/库存/金钱规则，无画面，`MartRulesTest` 9 条）、`state/MartPrices`、`MenuService.Kind.MART`、`PauseMenuOverlay`、`EventInterpreter` 的 `OPEN_MART` / `SET_MART_PRICE`；构建器 `pbPokemonMart(stock,speech,cantsell)`、`setPrice`、`setSellPrice` |
| 实机截图验证 | — | `scripts/jdk-capture.ps1 X:\generated X:\runtime\lwjgl3\mart-shots mart`：问候→购买→列表→数量→确认→谢谢惠顾，钱 12000→9900、超级伤药 2→5，与规则一致 |
| 登记 | — | 进出商店时地图滚动 `pbScrollMap`、列表上下箭头动画、卖东西时背包的黑幕渐变未建模；`$battle_item = [...]` 这类用脚本局部变量拼货架的写法（1 处）未翻译 |

## 阶段 8

| 项 | 依据 | 实现与测试 |
|---|---|---|
| 8.4 **选御三家**（`DiegoWTsStarterSelection.new(152,255,728)`）：淡入 25 刻、三球摇摆、手指上下浮动、选定后球座移动 + 精灵淡入 + 叫声 + 确定/取消框、取消后原路返回、确认后淡出，随后给宝可梦 | 段 333（565 行）全文 | `ui/menu/StarterSelectionView`（每个 40fps 刻度一个状态机步，各阶段刻数和每刻增减量照原文，包括整数除法）、`MenuService.Kind.STARTER`、解释器 `STARTER_SELECTION` 与 `starterChosen`：变量 7 仍为 0 时记下选择，`pbGenPkmn(dex,5)`、六项个体值 31、`setAbility(2)`、`calcStats`、`pbAddPokemon`；构建器 `DiegoWTsStarterSelection.new(...)` |
| 实机截图验证 | — | `scripts/jdk-capture.ps1 X:\generated X:\runtime\lwjgl3\starter-shots starter`：淡入→提示→选中中间球→左移→确认→移动→确认框→取消→返回→再确认→淡出，结果为所选球 |
| 登记 | — | 白色颜色混合 `Color.new(255,255,255,105)` 用「叠一层白」近似；HGSS 风格（`INSTYLE=1`）和大圆环（`STARTERCZ>=1`）原文默认不用，没做；`pbNicknameAndStore`（昵称询问）整个工程还没有，沿用现有 `GIVE_POKEMON` 的「得到了」提示 |

## 阶段 4

| 项 | 依据 | 实现与测试 |
|---|---|---|
| 4.1 **野外遇敌整体重写**（取代 112 行的简化版 `WildEncounters`）：`pbEncounterType`（冲浪=水面、洞穴、草地 + **早晨/白天/夜晚三套表**，用本项目「现实 1 小时 = 游戏 1 天」的时间）、`isEncounterPossibleHere?`（冰面不遇敌、草地要草地图块）、`pbGenerateEncounter`（前 3 步安全、密度×16 对 `rand(180*16)`、**骑车 ×0.8**、净化护符/纯净香 ×2/3、臭气/白烟/飞毛腿 ÷2、雪隐/沙隐按天气 ÷2、群聚 ×1.5、发光/沙穴/无防守 ×2、威吓/锐利目光/玫瑰庇护 50% 跳过弱怪，整数与浮点运算与 Ruby 一致）、`pbEncounteredPokemon`（**按权重**而非均匀抽取；静电/磁力/引火/收获/避雷针/引水 50% 偏向对应属性；活力/紧张/压迫等 50% 取更高等级；黑白笛 ±1..3 级）、`pbCanEncounter?`（驱虫喷雾拦截比首发等级低的野怪） | `175_PField_Encounters` 全文 | `field/PokemonEncounters`、`field/PBDayNight`、`state/FieldGlobals`（冲浪、潜水、骑车、驱虫、步数、笛子，进存档）、`PokemonEncountersTest` 11 条 |
| 4.2 **步数流水线** `pbOnStepTaken`：步数计数（`&= 0x7FFFFFFF`）、**走路亲密度**（每 128 步各只 50% 加）、**驱虫步数倒计时**（冰面不减，用完提示）、`pbBattleOnStepTaken`（无可战斗宝可梦不遇敌；高草 + 两只以上可战斗 30% 或有同伴 → **野外双打**）、**原地转向也会遇敌**（`Events.onChangeDirection`）；遇敌前的提示先播完再进战斗 | `PField_Field:262-269/356-384/488-516`、`PItem_ItemEffects:162-188` | `field/FieldSteps`、`Pokemon.changeHappiness`（整张表）、`MapScreen`（`onPlayerStep` / `onPlayerDirection`）、`BattlePort.doubleWildBattle`；`FieldStepsTest` 7 条 |
| 4.3 碎岩遇敌补上 **25% 概率**（原先每次都遇敌）并改用新类（同伴时双打） | `179_PField_FieldMoves:604-609` | 解释器 `ROCK_SMASH_ENCOUNTER` |
| 登记 | — | 毒伤害、托儿所/名人堂等其他步数处理、驱虫喷雾用完后的「再用一个吗」（要等阶段 5 的道具使用）、`EncounterModifier`（流浪宝可梦/宝可雷达）、虫子大赛表 |

## 阶段 5（A）：宝可梦身上的道具使用 —— 已完成

对照 `189_PItem_ItemEffects`（UseOnPokemon 全部处理器）、`188_PItem_Items`（pbHPItem / pbRestorePP / pbRaiseEffortValues / pbChangeLevel / pbLearnMove / pbUseItem / pbUseItemOnPokemon / pbGiveItemToPokemon）、`253_PSystem_Utilities`（pbMoveTutorChoose）。

- 新增 `field/ItemHandlers`（逐条转写，行号注释）、`ItemScene` 接口、`BlockingTask`/`ItemTask`/`TaskItemScene`（阻塞式 Ruby 处理器跑在独立线程，屏幕逐个应答）。
- `PartyView` 成为处理器的屏幕：pbDisplay/确认/选招式/选宝可梦/数量窗口（Window_InputNumberPokemon）/右上角能力窗口；背包「使用」「给予」都打开宝可梦页面（原先背包里自造的选择列表已去掉）；队伍页的「道具→使用」接上背包选择。
- 背包的指令列表按 `211_PScreen_Bag:467-483` 动态生成（使用/给予/丢弃/取消）。
- 顺带修了真实缺口：新增 `PBExperience`（087，等级上限 210，101 级以上用插件自己的公式），战斗经验/升级/摘要页不再卡在 100 级；`PBMove.totalpp`（PP Up 真正改变最大 PP）；`Pokemon.fused`、`calcStats` 的 HP 差值保留。
- 登记：①进化场景（阶段 8）只做物种替换；②招式遗忘用指令列表代替摘要页的遗忘模式（阶段 8.2）；③技能机器 TR（trmoves）、邮件、暗影宝可梦未建模；④`abilityIndex` 由特性名反推；⑤Fluctuating 在 100 级以上本身不单调（插件公式如此，照搬）；⑥战斗里的经验结算仍是简化版（单参与者、无经验分享），待补。
- 未做（阶段 5B/7）：UseFromBag/UseInField 的场景类道具（驱虫喷雾及喷雾失效后的“再用一个”、笛子、经验共享开关、硬币盒、城镇地图、神圣灰烬、逃脱绳、探宝器、自行车、钓竿、提灯、无限之笛……）。

## 阶段 5B（部分）与 6.6

| 项 | 依据 | 实现与测试 |
|---|---|---|
| 驱虫喷雾三种、无限喷雾、黑白笛、经验共享开关、硬币盒（新增 `FieldGlobals.coins`，进存档）、神圣灰烬（宝可梦页面） | `189_PItem_ItemEffects:21-392` | `ItemHandlers.useInField / sacredAsh`，背包「使用」经 `BagView.runFieldTask` 用背包消息窗应答；`ItemHandlersTest` 3 条 |
| **6.6 寄存系统队伍精灵一直画在盒子上** | `PokemonBoxPartySprite#x= / y=`（222:480-487）每次设位置都 `refresh` | 根因：`StorageView.sceneStartBox` 把队伍标签页移出屏幕后没有刷新图标，图标留在旧位置；补上 `boxparty.refresh()`。`MenuCapture storage` 截图确认盒子里只剩盒内精灵 |
| 名人堂缎带循环 `for i in $Trainer.pokemonParty / i.giveRibbon(:CHAMPION)` | `197_PokeBattle_Pokemon:571-576` | 构建器 `pbGiveRibbonToParty` + IR `GIVE_RIBBON_PARTY`；脚本覆盖 5232→5242；构建器测试 + `ScriptIrExecutionTest` |
| 仍未做 | — | 喷雾失效后的「再用一个吗」、城镇地图、逃脱绳/探宝器/自行车/钓竿/提灯/蜂蜜/无限之笛（与阶段 7 合并）；`pbUnlockDex`（依赖 3.3 图鉴多表）、`pbSlotMachine`（阶段 12）、`pbAddDependency2/pbRemoveDependencies`（阶段 9） |

## 阶段 6.3 电脑道具存取

| 项 | 依据 | 实现与测试 |
|---|---|---|
| 取出 / 储存 / 扔掉道具 | `224_PScreen_PC:4-54`、`223_PScreen_ItemStorage` 全文、`211_PScreen_Bag:591-690` | `state/PcItemStorage`（50 格 × 999，补格/拆格/删除与 `ItemStorageHelper` 一致，新档带一瓶伤药，进存档）、`ItemStorageView`（取出/扔掉，`pcItembg`，7 行列表、描述窗、「取消」行）、`NumberPrompt`（`UIHelper.pbChooseNumber`：x001 窗，上下 ±1 循环、左右 ±10）、`BagView.depositMode`（储存用背包本身）、`PokeCenterPcView` 接线；`PcItemStorageTest` 3 条；`MenuCapture pcitems` 截图 |
| 背包「丢弃」改为原文流程 | `211_PScreen_Bag:510-523` | 先问数量、再确认「确定要丢弃…」、再提示（原先直接丢 1 个） |
| 登记 | — | 背包容量（3.1）未建模，取出恒放得下；列表窗字体沿用背包字号；消息/是否窗用共享 `PbMessage` |

## 阶段 6.4 寄存系统的文字输入

| 项 | 依据 | 实现与测试 |
|---|---|---|
| 盒子命名、宝可梦昵称、搜索宝可梦（含范围选择与模糊匹配、命中后跳转到该盒） | `306_B2W2_PC:1275-1284`、`:1746-1765`、`:1800-1819`、`:2142-2184` | `StorageView.enterText`（复用 `TextEntryView`）、`searchPokemon`；昵称留空恢复物种名；原文 `pbRefreshSingle(pkmnid)` 引用未定义变量（插件缺陷）不照抄 |
| 登记 | — | 文字输入场景里没有宝可梦/盒子图标（原文 `pbEnterText` 的 subject）；盒子名上限沿用调用方参数 |

## 阶段 7.1 前置：事件里的 Ruby 条件（重大缺口）

**发现**：事件分支（指令 111 的「脚本」类型）共 1835 条，之前 Java 只认得 9 种固定写法，其余 727 条（约 40%）一律记警告并当作 false——包括 41 棵树的 `pbCut`、155 块岩石的 `pbRockSmash`、27 处徽章数判断、背包有无道具（`pbHasItem?`）、训练家 `pbPokerus?`、跟随者开关、`@ch_ret` 选项判断、昼夜判断、招式教学师 `pbMoveTutorChoose`（65 处）等，也就是树/岩石完全砍不了、凡是看徽章数的剧情门都进不去。

| 项 | 实现与测试 |
|---|---|
| 通用条件求值 | `event/ScriptCondition`（Ruby 表达式解析器：字面量/符号/字符串、`$全局`/`@实例变量`、`A::B` 常量、方法调用与下标、`! && || and or not`、比较、`+ - * / %`、`(expr rescue expr)`、数组字面量；对 nil 调方法抛错并可被 rescue）+ `event/ConditionEnv`（开关/变量/自身开关、`$Trainer.numbadges/badges/party/ablePokemonCount/pokemonCount`、`$PokemonBag.pbHasItem?/pbQuantity/pbCanStore?`、`$PokemonGlobal.followerToggled/coins/bicycle/surfing`、`pbGet`、`pbPokerus?`、`pbHasSpecies?`、`pbCheckAble`、`pbBoxesFull?`、`pbGetSelfSwitch`、`PBDayNight.*`、`isTempSwitchOn/Off?`、`MAX_COINS`）；不认识的原子仍记警告并为 false |
| 会和玩家说话的条件 | `EventInterpreter` 新增「脚本任务」：条件跑在独立线程（`BlockingTask`），每个 `pbMessage/pbConfirmMessage/选项/等待/音效` 由解释器用现成消息窗播放，答案回到脚本；`FieldScene`/`TaskFieldScene` |
| 居合斩/碎岩/怪力 | `field/FieldMoves.pbCut / pbRockSmash / pbStrength / pbCheckHiddenMoveBadge / pbCheckMove`（`179_PField_FieldMoves:194-208, 610-623, 652-672`，徽章要求按 `FIELD_MOVES_COUNT_BADGES`） |
| 招式教学师 | `pbMoveTutorChoose(PBMoves::X)` 作为菜单请求 `TUTOR`，由 `PauseMenuOverlay` 用宝可梦页面（`ItemHandlers.pbMoveTutorChoose`）应答，返回学会招式的宝可梦序号（0 在 Ruby 里为真） |
| 测试 | `ScriptConditionTest` 8 条（纯条件、运算符/rescue/常量/自身开关、未知原子、pbCut 无徽章/是/否、教学师有人学会/无人学会）；总计 1006 条全绿 |
| 仍未支持的条件原子 | `pbReceiveItem(...)`（6，条件里带消息）、`pbDoubleTrainerBattle`（6）、`pbDayCare*`/`pbEggGenerated?`（阶段 8.7）、`pbSafariState`、`transportPokemon`、`pbAddToParty`、`@ch_cmd` 的赋值（还需要脚本语句级解释，见阶段 13） |

## 缺陷修复批次（根据《错误·缺陷·bug汇报》）

- 获得道具：`pbItemBall` / `pbReceiveItem` 按 309_Item_Find 转写（`GIVE_ITEM` 新增 `mode: ball|receive`）；首次获得走 `\me[..]` + 文字 + “放进口袋”，再次拾取只放 ME 并在右侧弹 308 的 184×28 小框（`ItemFindToasts`，地图画面绘制）。消息新增 `\me[]` / `\wtnp[n]` 支持。`foundItems`（Game_Player.found_items）进存档。
- 贵重物品：`pbGetKeyItem` 按 302_BW_Get_Key_Item 逐帧转写（`KeyItemAnimation`，40fps，不透明度按整数截断），动画结束后才 `pbReceiveItem`。登记：`blackscreen` 图片缺失，用纯白铺满代替。
- 技能学习机图标：`pbItemIconFile`（258:273-302）完整链（itemXXX → item%03d → itemRecord/itemMachine + 招式属性 → item000），`ItemIcons` 统一各界面。
- 连锁捕捉 343_ChainCatching：`ChainCatching`（refreshChain/getChain/pbRandomIV）、遭遇表连锁加成、`pbGenerateWildPokemon` 的精英怪（battleRank 2 + 1 项满个体）、闪光重掷、508/510 图必闪；捕捉后 `level=200` 上限、开关 35 → 1 级（插件覆盖了 129 里的开关 60 写法）。
- 暂停菜单抬头：日期后加时段名（372 `pbGetDayNightName`）、292 随机标语、模式（难度）和连锁显示。
- 学新招式四招已满：摘要界面“遗忘”模式（303 `pbStartForgetScene` / `pbChooseMoveToForget`）、战斗经验结算的完整对话（Battle_ExpAndMoveLearning:312-343），道具学习（`ItemHandlers.pbLearnMove`）也改接真界面；顺带修了结算阶段不处理选择框的问题。
- 战败：`pbStartOver`（297_Follower_Main:815）转写：回精灵中心或家（Home 元数据），文字、开关 1、传送。登记：followers/依赖事件、逃脱点。
- 野生闪光登场动画：`pbBattleIntroAnimation:41-52`。登记：中途送出（Scene_Animations:137-141）未接。
- 未复现/待确认：选难度（Intro 事件在解释器里跑通，开关/变量正常）、NPC 锁定玩家后转向、VS 条不出现。
- 诊断工具：`RealEventSmokeTest`（环境变量 SMOKE=map,event,page,选项… 输出实际事件对话转录）。

## 阶段 7（部分）：自行车与冲浪（场景交互部分）

- 通行：`TileMap.playerPassable` 冲浪时水面可通行、自行车挡高草和冰面（027_Game_Map:225-252），`PBTerrain`。
- 载具状态与规则：`field/Vehicles`（025_Game_Player:448-475 `pbCancelVehicles/pbCanUseBike?/pbMountBike/pbDismountBike`，188_PItem_Items:724-747 `pbBikeCheck`，170:603-608 换图强制骑行/下车）；传送取消载具（Scene_Map#transfer_player）。
- 自行车：`BICYCLE/MACHBIKE/ACROBIKE` 的 UseInField（189:306-316），背包里按钮文字（UseText：骑行中“步行”，否则插件原文 “Use”），使用后关闭背包与暂停菜单；骑行速度 5。
- 冲浪：面对可冲浪水面按确认 → `pbSurf` 条件原子（179:702-720：徽章、确认问句、“使用了冲浪!”）→ `pbStartSurfing`（跳上水面，`surfJump` 期间水面底座留在水格）；水→陆 `pbEndSurf`（跳上岸，落地后下船、恢复地图 BGM）；冲浪速度 4；`base_surf` 底座与上下起伏（035_Sprite_SurfBase、026_Game_Player_Visuals:70-78）；按元数据 PlayerX 切换骑行/冲浪行走图（pbUpdateVehicle）；冲浪/骑行 BGM。
- 渲染缓存：玩家行走图名变化时重载贴图（之前永远用第一次的）。
- 修正：`pbStartOver` 取 Home 元数据时误用 `mapMetadata(0)`（恒为 null），改为 `globalMetadata()`——否则从未用过精灵中心时仍然不会回家。
- 登记：队伍菜单里的招式使用（HiddenMoveHandlers，含 Surf 的 `CanUseMove/UseMove`、隐藏招式动画）、Dive、`pbFacingEvent` 检查、followers、冰面速度。

## 地图名称牌、图片叠放、界面音效

- 地图名称牌：311_BW_SignPosts + 312 配置（`LocationSignpost`，`MapScreen.showSignpost`）：`ShowArea` 地图、与上一张图同名不显示（mapTrail）、按名字关键字取 Location 图（后面的列表覆盖前面，路线最后）、路线号用 `icon_numbers`（按插件原样“个位在前”绘制）、10 帧滑入/90–100 帧滑出、季节条。登记：小地图 `$miniMap`。同一 `Events.onMapChange` 里的“切图按元数据下天气”和 `visitedMaps` 还没做。
- 图片叠放：`PictureLayer` 按图片编号升序绘制（原先按哈希顺序，编号大的图可能被小编号的盖住）。
- 对话/选项音效（071_Messages、065_SpriteWindow_text、058_Audio_Play）：`UiSounds`（GUI sel cursor/decision/cancel，音量 80；`$data_system` 的音效全空）。消息开头播确认音（`\se[]` 开头静音、`\op` 不播），翻页确认音，选项移动光标音，数字输入光标/确认/取消音。
- 其余界面补齐：背包（光标、换口袋 BW2BagSound、确认、关闭 BW2CloseMenu）、道具仓库、图鉴列表与词条页（305/329/330）、训练家卡（BW2MenuChoose / BW2Cancel，原先误用蜂鸣）；数字输入框与选择框改用 pbPlayCursorSE/CancelSE 的音。
- 没接：交换界面、名字输入（键盘输入分支 pbEntry1 原文无音效）、图鉴搜索。

## 战败扣钱、眼前一黑、捉到后的图鉴与入库
- 131_Battle_StartAndEnd:479-495：战败后依次显示“所有的宝可梦都倒下了……”、“你输给了…！”（训练家战）、pbLoseMoney（等级×徽章倍率，开关 33 关闭扣钱）、“你眼前一黑！”（非 canLose）。`InteractiveBattlePort.Session.endMessage/loseMoney`。
- 129_PokeBattle_BattleCommon:43-63 pbRecordAndStoreCaughtPokemon + 166:25-40：新种类“{1}的数据被记录在图鉴里了。”并打开图鉴词条页，队伍未满“{1}加入了队伍。”，满则进盒子并显示盒子提示。`Session.storeCaught`，`BattleScreen.Stage.CAUGHT_STORE`。
- 捉到后起名：插件 129:6-12、252:10-12 均已被注释，原作没有此问询，故未做。
- pbStartOver 的传送改为经黑屏淡入（原 transfer_player 本身无淡入，是按用户要求加的）。
- 登记：词条页前后的 pbFadeOutIn、@initialItems、存储创建者名称。

## VS 动画条（玩家一侧）
- PField_Visuals:148 玩家的视口从 x=0 开始，之前 `BattleEntryAnimation.render` 把玩家的条画到了右半屏（被对手的条盖住），已改为 0。

## 战斗系统已知缺损（登记汇总）
- 战斗中途换上闪光宝可梦的闪光动画（Scene_Animations:137-141）。
- 战败/传送：跟随者与队友（pbRemoveDependenciesExceptFollower）、逃脱点清除（pbEraseEscapePoint）、虫赛分支。
- 捉到后：图鉴词条页前后的 pbFadeOutIn、@initialItems、存储创建者名称、暗影宝可梦记录；捕获无会心捕获、无 Snag Ball。
- 野外/训练家对战：Safari、虫赛、对战宫殿/竞技场等特殊规则未建模；blackscreen.png 缺失（用白色代替）。
- 训练家 AI：单打已接 CFRU 评分；换人与使用道具（阶段 2、3）、双打 AI（阶段 4）未做。
- 对战内文本：中途 \se[] 的时机；未接音效的画面：交换、图鉴搜索。
- 难度选择与 NPC 锁定后仍转向两项仍待复现。

## 跟随宝可梦与依附事件（296_Follower_Config / 297_Follower_Main / 182_PField_DependentEvents）
- 数据：`state/Dependent`（`$PokemonGlobal.dependentEvents` 一项）、`FieldGlobals.dependents/timeTaken/followerHoldItem`，存档写入。
- `field/FollowerRules`：FollowerRefresh 各处理器（自行车/冲浪/潜水/室内大个子/飞行·漂浮·常动画种类）、`change_sprite` 的图片名顺序（`Characters/Following/`，编号优先，形态/闪光/性别逐项回退，最后 000）。
- `field/FollowerTalk`：`Events.OnTalkToFollower` 全部处理器与 29 组台词（由插件原文脚本生成，未改字），含表情动画 95-100、等待帧、跟随者与玩家的原地动作路线、持有物（pbPokemonFound）。
- `map/FollowerController`：按插件的 `pbFollowEventAcrossMaps` 选后方/侧方可走格，随玩家每一步走一格（相距 2 格跳，更远直接出现），步行结束后转向玩家，速度同步，常动画（冰面除外），`come_back/remove_sprite/refresh_sprite/toggle`，累计 `timeTaken`（每 5000 帧好感 +1，超过 15000 帧可交出道具）。
- 接入：脚本 `pbPokemonFollow(n)`、`pbToggleFollowingPokemon`、`pbRemoveDependencies`、`pbRemoveDependenciesExceptFollower`、`pbAddDependency2`、`pbTalkToFollower`（builder 新增 IR：POKEMON_FOLLOW/REMOVE_DEPENDENCIES/ADD_DEPENDENCY/TALK_TO_FOLLOWER）；Ctrl 切换（`GameAction.TOGGLE_FOLLOWER`）；朝向跟随者按确认键执行其公共事件（跟随者为 5 号，伙伴 NPC 为各自编号）；自行车/冲浪/潜水变化、战斗结束、关闭暂停菜单后跟随者重新出现；战败 `pbStartOver` 只保留跟随者。
- 登记：与当前地图相连的邻图上的跟随者（这里始终站在玩家所在图）、跟随者的影子/倒影、阶梯格的特殊通行判定、两格以上追赶的第二步（改为跳）、`pbTalkToFollower` 里面朝水域时改走冲浪询问（297:70-75）、战斗开场的跟随者滑入（297:727-813）、HM 动画里的跟随者动作（297:689-722 在原文中被 =begin 注释掉）。

## 阶段 7.1 隐藏招式框架（179_PField_FieldMoves）
- `field/HiddenMoves`：`HiddenMoveHandlers` 的处理器表（CUT/DIG/DIVE/FLASH/FLY/HEADBUTT/ROCKSMASH/STRENGTH/SURF/SWEETSCENT/TELEPORT/WATERFALL/DEFOG + Chatter）、全部 `CanUseMove` 与 `ConfirmUseMove`（徽章检查、各句提示、`BAN_MAPS`、Teleport 禁用图表）。
- `event/HiddenMoveTask`：各招式的 `UseMove` 主体，在 `BlockingTask` 上运行（说话、横幅、等待、淡入淡出、战斗都由解释器代办），队伍菜单选中招式后菜单关闭、再由 `EventInterpreter.startHiddenMove` 执行（206_PScreen_PauseMenu:183）。
- 接入：队伍菜单的招式命令（`PartyView.hiddenMove`，210_PScreen_Party:1357-1412）、`MapPort` 新增的场景查询/动作、`map/HiddenMoveAnimation`（179:78-189 横幅与流光）、`map/FlyBirdAnimation`（340_Fly_Animation）、`map/DarknessOverlay`（171:364-404，3 张暗图 + Flash 扩圈）、飞行地图模式（`TownMapView.flyMode`，338/214 的 `pbMapScene(1)`，`visitedMaps` 存档）、水路按键（Dive/Surfacing/攀瀑，`pbDive/pbSurfacing/pbWaterfall` 条件原子）、`healingSpot`/`escapePoint`/`flashUsed`/`visitedMaps` 全局状态与存档。
- 登记：准备菜单（221_PScreen_ReadyMenu）里的招式快捷使用；`pbSetEscapePoint` 在工程脚本中没有调用，挖洞暂时恒不可用；Chatter 的 `UseMove` 在插件里调用了不存在的全局 `pbChatter`（会报错），这里按其本意播放叫声并等 40 帧；`pbFacingEvent` 对已擦除事件的细节；神奇糖果以外的 Teleport 动画。

## 短按转向、战败黑屏居中
- 025_Game_Player:350-375 `update_command_new`：新按下的方向只转向，同一方向按住超过 frame_rate/20（2 帧，即第 3 帧起）才走；刚走完一步接着按住则无需等待。`MovementController` 按此改写（短按可以只转身，面对跟随者再按确认键即可对话）。
- 171_PField_Visuals:18-132 + 174_PField_Battles:329-335/656：`pbAfterBattle`（含战败的 `pbStartOver`）在 `pbBattleAnimation` 的块内执行，战斗画面结束时是黑屏，战败提示和传送都在黑屏上进行，之后新地图从黑屏淡入。战败时 `RuntimeContext.whiteOut` 立即黑屏，`MapScreen` 不再提前淡出，信息窗口画在黑幕之上。
- 071_Messages:1198-1217 `\wm`/`\wu`/`\wd`：信息窗口居中/置顶/置底（`MessageText.Parsed.position` → `MessageService.position()` → `MessageWindow`），`\w[]` 无窗口皮肤的白字照旧。

## 缺陷修复批次 2（《错误·缺陷·bug汇报》的 BUG记录2）
- 恶性：地图右侧的拾物小框只在事件运行时计时，事件结束后永远不消失并每帧画文字（性能骤降）。改为每帧由地图屏幕计时（308_ItemFindSimple_Scene:78-88）。
- 恶性：战斗升级后直接进化——`Battle` 里一处临时的 `evolveOnCondition` 删除（插件 174_PField_Battles:687-694 战后进化检查被 =begin 注释，进化是队伍菜单的“进化”命令）。
- 恶性：烧伤自行消失——`pbInflictStatus`/`pbFaint` 写 `status` 时没同步 `pokemon.status`（PokeBattle_Battler:104 的 setter 两处都写），宝可梦换下再换上后状态丢失；补上并加测试。
- 异常状态/等级先于动画：每个回合事件带上创建时各战斗者的状态快照，数据框的状态图标在事件播放到那里才变（`BattleScreen.shownStatus`）；经验槽按层升级，升级时先放 “LevelUp” 通用动画，之后数据框的等级才 +1（132_Battle_ExpAndMoveLearning:249-265）。
- 得到事件给予的宝可梦：`pbAddPokemon`/`pbAddPokemonSilent`（252_PSystem_PokemonUtilities:4-101）全文：满箱提示、“{1}得到了{2}!”+Pkmn get 乐章、进箱提示、已见/已拥有记录。插件的 pbAddPokemon 本身不开图鉴页（只有战斗捕捉 129:50-51 会）。
- 战斗里的道具：背包里选中后先问“使用/取消”，再进真正的队伍界面“要对哪只宝可梦使用？”（156_Scene_Commands:235-346），队伍界面按 `pbPlayerDisplayParty` 把场上宝可梦排到最前（换人界面同样）。
- 背包选道具（捕捉后的球包、队伍菜单的“道具”）：按 305_BW_Bag:181-191/446-455 只显示本口袋里符合筛选的道具，没有符合项的口袋被跳过，不再能随意切换。
- 战斗指令/招式光标记忆：单打也记住上次的指令和招式（Scene_Commands:30/94/174）。
- 头顶名称：`Headtop_Name`（375/376）画在角色头上，选项“头顶名称”生效；选项“自动跟随”：走上草地时跟随者自动出现（170_PField_Field:361-366）。
- 招式学习机/技能机的名字后面带招式名（背包、道具存储，同 230_PScreen_Mart:21-28）；背包文字里的 `<icon=…>` 画成 Graphics/Icons 里的图（070_DrawText:604-610）。
- 跟随者在开桥脚本后与玩家重叠：补上 297_Follower_Main:1260-1261 的桥面判断。
- 战败扣钱按用户要求改为持有金钱的 5%（偏离插件 131:427-431）。
- 未处理/需要确认：战败回家的坐标——PBS 和 `Data/metadata.dat` 的 Home 都是 `3,17,14,8`，插件里没有别处会把玩家挪到 (17,12)，需要对照原工程确认；map036 的毒贝比不消失；闪退只有 Gradle 的退出码、没有堆栈；招式学习的“二次确认/没有学会”两句在这份插件的 132 里并不存在（插件改过原版）。

## 阶段 8.1/8.2 进化（201_Pokemon_Evolution、226_PScreen_Evolution）
- `pokemon/PBEvolution`：66 种进化方法的 `levelUpCheck/itemCheck/tradeCheck/afterBattleCheck/afterEvolution` 逐条抄录（含检查时改形态：Goomy/Petilil/Rufflet/Bergmite、鲤鱼王/双斧战龙/美纳斯/爪子巨锻匠特殊道具、糖饰随机形态），`pbGetEvolvedFormData/pbGetPreviousForm/pbGetBabySpecies/pbGetMinimumLevel/pbGetEvolutionFamilyData/pbCheckEvolutionFamilyFor…/pbCheckEvolutionEx/pbCheckEvolution`；世界状态经 `field/EvolutionWorld`（昼夜、天气、自行车/冲浪/潜水、暗地图、地区、队伍、背包）。
- `Pokemon.changeSpecies`（`species=`：保留昵称、形态号、特性栏位、HP 差，等级按新物种成长率）、`copy()`（pbDuplicatePokemon/脱壳忍者）、`beauty/criticalHits/yamaskhp` 字段。
- `ui/menu/EvolutionView`：`PokemonEvolutionScene` 全流程（淡黑 16 刻、精灵淡入 17 刻、“什么？X正在进化！”、50*40/20 刻等待、Evolution start/Evolution BGM、pbGenerateMetafiles 的两只白色剪影缩放动画、背景条收窄/展开、pbFlashInOut、B 取消、叫声长度等待、Evolution success、afterEvolution 消耗/复制、进化后学招式 pbLearnMove 全文含遗忘界面）。队伍菜单“进化”命令与进化石/卷轴（`ItemScene.pbEvolution`）都经它；队伍格上的“可进化”标记按 pbCheckEvolution 刷新时计算。
- 登记：暂停箭头未画；宝可梦与背景平面的动画是静态图；`getMoveList` 用物种本身的招式表；香（incense）婴儿种变体未做；Pokemon 侧 `beauty` 无来源；交换进化（TradeModel）、战后进化（afterBattleCheck 的 CriticalHits/DamageDone，数据里没有物种用）尚未改走新场景。
- 同批修复：`BattleAttackPhase` 选了道具/换人时 `choice[1]` 不是整数导致的 ClassCastException（崩溃日志 00:41 与 08:31 两次）。

## 特性提示条（130_PokeBattle_Battle:801-818、157_Scene_Animations:171-203、150_PokeBattle_SceneAnimations:230-267、152_PokeBattle_SceneElements:405-497）
- 引擎：`Battle.showAbilitySplash/hideAbilitySplash/replaceAbilitySplash` 不再是空函数，产生回合事件 `ABILITY_SPLASH_SHOW/HIDE`（显示时记下宝可梦名与特性名；`delay=true` 的调用点——`AbilitiesSwitchIn` 的 9 处与两个招式效果——带 1 秒停顿）。
- 画面：`abilityBar_0/1`（`ability_bar.png` 上下半张分别给我方/对方），`AbilitySplashAppear/DisappearAnimation`（8 刻滑入/滑出），已显示时先滑出再滑入，文字“{名}发动了特性”+特性名（我方左对齐 x=10，对方右对齐 width-24）。
- 登记：`Battler`/其它文件里原文是 `(…,true)` 的 `pbShowAbilitySplash` 调用，没有逐一核对到 `delay`，只改了确认的位置；数据框/消息框与提示条的层叠按 z=120 的顺序画。
- 另：`pokemon-runtime-battle.log` 每回合记录引擎事件与双方能力等级（诊断用）。
- 进化丢失项复查（同批）：①旧 `PokemonGrowth.evolve` 把特性重置为新物种第一特性 → 改走 `Pokemon.changeSpecies`（按栏位保留，隐藏特性随新物种的隐藏特性）；②`Pokemon.setForm` 每次都把特性重置为第一个（原文 198_Pokemon_Forms:17-24 的 setForm 不碰特性，Goomy/Bergmite/Rufflet 每次刷新队伍的进化检查都会触发）→ 改为按栏位跟随；③进化前设置的形态号（鲤鱼王 2、双斧战龙 1、美纳斯 2、爪子巨锻匠 1、古鲁蛋/泥巴龟…、Kubfu→Urshifu）在数据里没有对应词条而丢失 → 新增 `Pokemon.looseForm`，进化时由新物种取词条。测试：`PBEvolutionTest`。仍简化：交换进化（`TradeModel`）不走进化画面，不学进化招式、无进化乐曲。

## 交换（227_PScreen_Trading、252_PSystem_PokemonUtilities:277-296、210_PScreen_Party:1270-1296）
- 选宝可梦：`pbChoosePokemonForTrade` → `pbChooseTradablePokemon`，用真正的队伍界面（可用/无效 标注、“这个宝可梦不能参加。”），不再是自造的“宝可梦交换”面板。
- 交换场景：`TradeSceneView` + `battle/TradeAnimation`（pbScene1/pbScene2 的 PictureEx 时间线逐行：召回变色、球飞出屏幕、球落地四次弹跳、开球、缩放出场、叫声），消息全文（“{名}\nID: …   OT: …”、“{3}选择{4}\n与{1}的{2}交换。”、“{1}向{2}告别了。”、“请照顾好{1}！”）、外层淡出入与音乐、`tradebg`，结束后 `pbTradeCheckEvolution` 走进化场景（TradeItem/TradeSpecies/Trade… 与消耗道具）。
- `pbStartTrade` 前半（`TradeModel.prepare`）：外来训练家 ID/OT、昵称、obtainMode=2、resetMoves（给物种名时）、pbRecordFirstMoves、图鉴见过/拥有。
- 新增 `ui/menu/SceneMessage`（自带屏幕的场景的消息窗：逐字、\se/\wt/\wtnp/\1），`EvolutionView` 已改用它。
- 登记：前沿精灵的 metrics 偏移（pbApplyBattlerMetricsToSprite）与暂停箭头未画。

## 蛋招式传授师（362_changeShiny:38-85 `teachEggMoves`，map-100）
- 编译器：`teachEggMoves` → IR `TEACH_EGG_MOVES`；运行时 `event/EggMoveTutor` 逐行转译（5000 金钱、确认、选宝可梦、`pbGetBabySpecies` 的蛋招式、去掉已会的、选招式、`pbLearnMove(.., false, true)`（含遗忘界面）、扣钱与“你支付了5000金钱。”、金钱不足/没有蛋招式/取消的各句）。
- 新增通用件：消息的 `\g` 金钱窗（`pbDisplayGoldWindow`，“零花钱：”+金额，goldskin，消息在顶部时放底部）；`MenuService.Kind.CHOOSE_NON_EGG`（`pbChooseNonEggPokemon` 的队伍界面，“可以授予/不能被授予”）与 `FORGET_MOVE`（摘要界面的遗忘模式）供场景脚本使用。
- 登记：`pbGetSpeciesEggMoves(baby, form)` 只取物种的蛋招式，形态专属蛋招式未建模。
- 同文件的 `pbChangeShinyByNPC`（map-093）尚未转译。
- 招式回忆已在下一节做完。

## 招式传授师（条件分歧里的 `pbMoveTutorChoose`，253_PSystem_Utilities:955-1018）复核
- 早已转译（65 处调用，map-043 HARUKIKAGE 等）；复核时发现形态不通：原文用 `fSpecies`（`JIGGLYPUFF_1`）查 tm.txt，我们只查基础物种。`ItemHandlers.compatibleWithMove` 现按形态键查；测试 `MoveTutorFormTest`。登记：黑暗宝可梦的拒绝句未做（工程里没有）。

## 招式回忆（228_PScreen_MoveRelearner、210_PScreen_Party:1462-1490、map-105 Move Relearner）
- 数据：`pokemon/MoveRelearner`（`pbGetRelearnableMoves/pbHasRelearnableMove?`：初始招式 + 招式记录 + 升级招式，去重，选择已会的不列出，Kyurem 非 0 形态去掉 GLACIATE）；`Pokemon.trMoves`（`trmoves`）、`Pokemon.getMoveList`（按形态词条）；`firstMoves/trMoves` 现在写进存档（之前 `firstMoves` 没存档）。
- 招式记录（TR）：原文 188:859-863 / 948-953 用完即消耗并记入 `trmoves`；之前 TR 不消耗也不记录，已补（`ItemHandlers.pbUseMachine`、`pbUseItemOnPokemon`）。测试 `TechnicalRecordTest`、`MoveRelearnerTest`。
- 画面：`ui/menu/RelearnerView`（reminderbg、reminderSel 两个框、类型图标、PP、分类/威力/命中、描述、按键提示；“教什么招式？”“放弃{1}的新招式？”“教学{1}？”；学习走 `ItemHandlers.pbLearnMove`，遗忘界面是摘要的 forget 模式），队伍菜单“招式→回忆”现在可用（“{1}没有可以回忆的招式。”）。
- 事件：编译器 `pbChoosePokemon(1,3,proc{|p| pbHasRelearnableMove?(p)},true)` → IR `CHOOSE_POKEMON`（`MenuService.Kind.CHOOSE_ABLE`，allowIneligible）；条件原子 `pbHasRelearnableMove?`、`pbRelearnMoveScreen`（`MenuService.Kind.RELEARN`）。
- 登记：`UIHelper.pbConfirm` 的“是/否”用共用的 `PbMessage` 命令窗；宝可梦图标不动；黑暗宝可梦未建模；形态没有自己招式表时读物种的。
- 未验证（需实机）：回忆画面的位置/配色、事件整条流程。

## 改异色 NPC（362_changeShiny:1-36 `pbChangeShinyByNPC`，map-093）
- 编译器：`pbChangeShinyByNPC` → IR `CHANGE_SHINY`；`EggMoveTutor.changeShinyByNPC` 逐行转译（250000 金钱、选宝可梦、已是异色可免费变回、不足/取消/成功各句、`pbMEPlay("Pkmn get")`）。新增 `FieldScene.pbMEPlay`。测试 `EggMoveTutorTest.changesShinyForTheFee`。
- 登记：`makeShiny/makeNotShiny` 只改 `shiny` 标志（与原文一致，不改 superShiny）。未实机验证。

## 孵蛋（225_PScreen_EggHatching、361_myAddEgg、210_PScreen_Party:1438-1448）
- 步数：`Party.stepEggs` 按原文 218-233：每步 -1，队伍里有火焰之躯/熔岩铠甲的（非蛋）再 -1，到 0 即孵化。`EggHatching.pbHatch`（192-205）：名字=物种名、OT=玩家、ID、亲密度 120、孵化时间/地图（新增 `Pokemon.hatchedMap/timeEggHatched`，已存档）、obtainMode=1、图鉴见过/拥有、`recordFirstMoves`。
- 画面：`ui/menu/HatchSceneView`（Huh?、淡出入与音乐、hatchbg、蛋图与 eggCracks 五帧裂纹、五次摇晃 swingEgg 的逐刻位置、白闪、换成宝可梦、叫声、BGM Evolution/ME Evolution success、“{1}从蛋中孵化出来了！”、起名字确认与昵称输入）。由 `MenuService.Kind.HATCH` 在 `MapScreen` 排队触发（多个蛋依次）。
- 队伍菜单“昵称”（210:1438-1448，原来是空实现）现在可用。
- `myAddEgg`（361）补上了原文的消息：“{1}加入了队伍。”、盒子满/传送到电脑的各句、obtainText。
- 登记：`pbApplyBattlerMetricsToSprite` 与昵称界面的宝可梦图标未画；“Huh?”消息画在宿主画面之上，实际宿主是否可见取决于暂停菜单的底图；`pbGetStorageCreator` 未建模；`PartyView` 摘要的“蛋孵化日期/地点”备注仍用简化版（212:564-575 未接）。未实机验证。
