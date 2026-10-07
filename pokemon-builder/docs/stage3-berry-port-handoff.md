# Stage 3 交接：树果移植 + 后续工作单

> 本文是跨会话交接。新会话请先读本文件，再动手。
> 战斗界面/入场过渡的最近一轮见 `docs/stage3-battle-entry-handoff.md`；新会话开场白见 `docs/next-session-intro.md`。
> 上一轮工作区**未提交**（全部改动都在 `E:\仓库\范例\929` 工作树里）。

---

## 0. 铁律（用户反复强调，违反会被骂）

1. **一切以原工程脚本（插件）为准**。禁止自造效果/数值/文案。
   - 插件源码在 `E:\仓库\范例\929\Data\Scripts.rxdata`，用下面的方法现场导出。
   - 每条实现都要能指到插件行号；文案（`_INTL(...)`）也要照抄。
2. **【2026-10 用户加严，最高优先级】必须逐行转译 Ruby 原文**：
   - 所有实现都要**一行行对着插件 Ruby 写**，不是"看着像"；一次转不完就**分批次**，时间充足，不要赶进度。
   - **禁止一切自己造/自己编的行为**：不准自创控制流、自创动画、自创数值、自创文案、自创"等价近似"、自创"可接受的简化"。
   - 每处改动都必须在该行注释里指出对应的插件 `段名:行号`；指不出来就不许写。
   - **遇到无法转译的情况（缺素材 / 缺数据 / 插件本身有问题 / 需要先做别的子系统）：立刻停止该批次并向用户报告**，由用户处理；不许自行绕过、不许造替代品、不许默默降级。
   - **插件引用的资源（图/音/数据文件）必须在工程里真实存在才能用**：先确认存在再写代码；
     **工程里找不到的，立刻停止并报告用户**；**禁止自己画/自己造一个资源补上，也禁止留空/注释掉当没看见**。
   - 交付时按批次报告：本批转译了哪些 Ruby 函数（`段:行号区间`）、哪些行没转及原因、探针定点截图证据。
3. **一次只做一屏/一个系统**，用户验收后再下一屏。
4. 不写交接文档——但跨会话例外（就是本文）。
5. 老虎机（`pbSlotMachine` 24 块）和挖矿（`pbMiningGame` 1 块）**已决定跳过**，不用做。

## 1. 环境与命令

```powershell
# 插件源码现场导出（写临时 mjs，用完删）
#   readScriptsSection(projectPath, { withSources: true }) 逐段返回 {name, source}
#   段名要精确匹配（例：'ZA模式 ' 带尾空格）
node __dump.mjs

# 构建运行时数据（改过 tools/ 或 PBS 后必须跑）
node builder/src/cli.js build-data "E:\仓库\范例\929"

# 编译 + 测试
$env:JAVA_HOME='C:\Users\Administrator\AppData\Roaming\.minecraft\runtime\java-runtime-delta'
cmd /c 'X: >nul && cd X:\runtime && .\gradlew.bat :core:test :lwjgl3:compileJava --console=plain'
# X: = subst X: "E:\仓库\范例\929\pokemon-builder"（丢了就重建）

# 截图探针（l1-battle / l1-party 等）
cmd /c 'X: >nul && cd X:\runtime && .\gradlew.bat :lwjgl3:captureMenu "-PcaptureArgs=X:\generated|C:\Users\Administrator\AppData\Local\Temp\opencode\menucap" --console=plain'

# Builder 测试（改过生成的 JSON 结构时必跑）
node --test --test-reporter=tap builder/tests/*.test.js

# 用户的存档目录：C:\Users\Administrator\.pokemon-runtime\saves\
```

## 2. 关键常量（已确认）

| 常量 | 值 | 影响 |
|---|---|---|
| `NEW_BERRY_PLANTS` | **true**（Settings:69） | 树果走 **Gen 4** 机制（8 元素数组），Gen 3 分支不用实现 |
| `NEWEST_BATTLE_MECHANICS` | true | — |

原版按键映射（工程自己重写了 `Input.buttonToKey`，见 `PSystem_Controls`）：

| 动作 | 物理键 |
|---|---|
| 移动 | 仅方向键（**无 WASD**：W/Y/Z 是 `Input::A`，A/S 是 `Input::L/R`） |
| `Input::C` 确认 | C / Enter / Space |
| `Input::B` 取消+开菜单 | X / Esc |
| `Input::A` 跑步 + 战斗 Mega 切换 | Z / W / Y / Shift |
| `Input::F5` 登记道具 | F / F5 / Tab |

## 3. 本轮已完成（都在工作树里，未提交）

### 3.1 战斗侧
- **按键映射**照 `Input.buttonToKey` 重写（`DefaultKeyBindings`），新增 `GameAction.SPECIAL` = `Input::A`。
- **战斗界面按插件重写**（`BattleScreen`）：窗口模型 `BLANK/MESSAGE_BOX/COMMAND_BOX/FIGHT_BOX`（`pbShowWindow`，同一时刻只显示一个）；消息逐条显示并自动/按键隐藏（`pbDisplayMessage` 40 帧、`pbDisplayPausedMessage` 带 ▼ 等输入）；`CommandMenuDisplay`/`FightMenuDisplay` 几何照抄；Mega 按钮 `(210, 352-h/2)`，按 `Input::A` **只登记**、选招式才应用。
- **开场 + 送球动画按本工程实际场景重做**（`BattleScreen` 的 `Stage` 状态机，40fps 帧数照插件）：
  - 开场 `Scene_Initialize:15 pbStartBattle` → `pbBattleIntroAnimation`（`PokeBattle_SceneAnimations:5`）：背景按 `battle_bg`+镜像 `battle_bg2` 从 ±W/2 滑入、双方 base 从 ±W 滑入（**底座锚点照 `Scene_Initialize:221-229` 修正**：玩家 `oy=height` 底边落在 `PLAYER_BASE_Y+60`、敌方 `oy=height/2` 居中；此前玩家底座按底边又减了一整个高度，整块沉到指令栏后面看不见）、`black_screen` 8 帧淡出、`black_bar`（**底部 96px 指令栏**）15→20 帧淡出；训练家战再 `pbShowPartyLineup(0/1,true)`（`LineupAppearAnimation:88`：条 8 帧、球每 2 帧错开、8 帧滑入，训练师 `trainerNNN.png` 从右侧滑入）；野生战 `DataBoxAppearAnimation:189`（8 帧滑入）+ `BattleIntroAnimation2:66`（`Tone(-80,-80,-80)` 4 帧复原）。
  - 提示语之后 `Battle_StartAndEnd:194 pbStartBattleSendOut` → `Scene_Animations:85 pbSendOutBattlers`：先 `TrainerFade/PlayerFadeAnimation:306/359`（条/球 3-15 帧淡出、滑走，训练师 16 帧滑出后隐藏），再 `PokeballTrainerSendOutAnimation:486`（敌方球原地出现→挤压 120/80→开球，`ballOpenUp` 帧 14-20，球 18-20 帧淡出，宝可梦 16-21 帧 zoom 0→100%、21-31 帧球色 tint 褪去）+ **`Follower_Main:727` 覆盖的我方弧线投球**（12 帧抛物线 `(-6,202)→(0,battlerY-144)→(battlerX,battlerY-18)`、8 帧翻滚+1080° 旋转、开球、现身）+ `DataBoxAppearAnimation`。**球会飞、会开、会消失**；队伍条只在对战开始时出现、送球时收起，战斗中不再常驻（换人时按 `Battle_Action_Switching:256-262` 重新出现再收起）。
  - 未做：叫声、闪光/超闪光、`[F]/[Z]` 提示（`pbShowExtraInfo`）、换人时的收回动画（`BattlerRecallAnimation`）、「{训练家}派出了/去吧」brief 消息（提示语已改为插件原文 `{fullname}向你发起挑战！`，野生为「哦！一只野生的{X}出现了！」）。
  - 截图回归：`:lwjgl3:captureMenu -PcaptureArgs=X:\generated|<out>|trainer` 逐帧产出 `l1-battle-trainer-intro/lineup/message/foe/foe-out/player/reveal/battle`；不带第三段则跑野生战 `l1-battle-intro/wild/ball/battle/fight`（另有 `l1-battle-bob0..3` 跃动、`l1-battle-settle0..15` 结算）。
- **战斗界面细节照插件补齐**（详见 `docs/battle-ui-fixes-plan.md`）：**战斗入场动画**（`PField_Visuals:18-133 pbBattleAnimation` 三种模式：`rocket:6-101` 邪恶组织 VS、`PField_Visuals:135-291` 内建 VS、`:77-106` 默认＝白/黑闪 2×16 帧 + 本工程 `Transitions` 段（KGC Special Transition，1718 行）的命名过渡——`BattleEntryTransition` 已逐行转译 `SnakeSquares`/`DiagonalBubble`/`RisingSplash`/`TwoBallPass`/`SpinBallSplit`/`ThreeBallDown`/`BallDown`/`WavyThreeBallUp`/`WavySpinBall`/`FourBallBurst`，并用 `MapScreen.captureEntrySnapshot()` 抓 `Graphics.snap_to_bitmap` 等价物；运行时 `BattleEntryAnimation` + `MapScreen` 挂载）、性别符号上色（`PokeBattle_SceneElements:16-21/223-229`）、对方性别按 `personalID+genderRate` 派生（`PokeBattle_Pokemon:166-177`、`PBGenderRates:11-23`）、我方 HP 数值 `icon_numbers`（`:281-289`）、Mega 槽居中（用户指定，原插件 210）、玩家回合数据盒/宝可梦 ±2 跃动（`:374-386/602-618`）、开场提示语合并为一个两行窗口（`Battle_StartAndEnd:220` + `MESSAGE_BREAK`）、战斗结算与 exp 槽动画/音效（`Battle_ExpAndMoveLearning:99-308` + `Scene_Animations:273-294`，含等级上限/超闪光/学招式/经验储罐/SE 收尾与最小值）、**经验句与进度条同拍**（brief 消息）、**训练师胜利收尾**（`Battle_StartAndEnd:444-473`：你打败了→对手滑回登场→LoseText→奖金→淡出）。文字阴影 `MenuFont.shadowEnabled` 恢复默认开。
- **胜利结算**去掉了自造的「战斗胜利！」，改为 `{我方}使用了{招式}！` → `{对方}倒下了！` → `{我方}获得了{N}点经验值！`（`Battle.lastExpGain`）。
- **战斗内背包/队伍 = 真整屏界面**：`PartyView.battleChoose()`（`pbPartyScreen`）、`BagModel.battleOnly()`+`BagView.battleMode()`（`PokemonBag_Scene` battle=true + `ITEM_BATTLE_USE` 过滤）。
- **战斗结束多走一步已修**（正确修法）：踩步检测保持「tile 变化即触发」（一步一次），战斗开始时把正在滑的那一步**落位** —— `player.teleport(player.x(), player.y())`（`MapCharacter.teleport` 会把 from/to 归位并清零进度，`isMoving()` 随即为假）。
  - ⚠️ **踩过的坑**：曾试图用 `!player.isMoving()` 当门槛来推迟判定，结果草叶只剩玩家脚下、草里也不再遇敌。原因：`MovementController.update()` 是 `while` 循环，**按住方向键时一步走完立刻起下一步**，帧边界上角色永远是「移动中」，该门槛几乎永不成立。**不要再用 `isMoving()` 做踩步门槛。**

### 3.2 道具效果（第 1 批：`UseOnPokemon`）
`ItemUse.java` 已照 `PItem_ItemEffects` 抄入：HP 类（POTION 20 / SUPERPOTION 60 / HYPERPOTION 120 / MAXPOTION 全满 / FRESHWATER 50 / SODAPOP 60 / LEMONADE 80 / MOOMOOMILK 100 / ORANBERRY 10 / SITRUSBERRY 1/4 / CIDER 50 / MIXEDBEVERAGES 100 / WWINE 150 / SWEETHEART 80 / ENERGYPOWDER 60 / ENERGYROOT 200 / FULLRESTORE）、异常类、复活类；**无效不消耗**且文案照 `pbHPItem`/`pbStatusItem` 原文（`ItemUse.lastMessage`）。
- **还没做**：PP 类（ETHER/MAXETHER/ELIXIR/MAXELIXIR/PPUP/PPMAX，需 `pbChooseMove` 选招式步）、EV 类（HPUP/PROTEIN/IRON/CALCIUM/ZINC/CARBOS/六翅膀/HOPOBERRY/六降 EV 树果，需先读 `pbRaiseEffortValues`）、形态类（GRACIDEA/REVEALGLASS/PRISONBOTTLE/DNASPLICERS/NSOLARIZER/NLUNARIZER/REINSOFUNITY/ZYGARDECUBE/SCROLLOF* /ABILITYCAPSULE/ABILITYPATCH/薄荷/ROTOMCATALOG/RARECANDY）、幸福度变化。
- **写 TM/进化成功的文案还没做**（不填自造文案）。

### 3.3 树果基建（全部已绿）
1. `state/GameEventVars.java` — `$PokemonGlobal.eventvars`：`(mapId,eventId) → int[]`，`GameState.eventVars()`，**不在 `enterMap` 清**。
2. `SaveManager` — 新增 `eventVars: [{map,event,values[]}]` 往返。
3. `MapPort.getEventVariable/setEventVariable/turnEvent` + `MapScreen` 实现。
4. `field/BerryPlants.java` — Gen 4 生长数学（照 `updatePlantDetails` 147-217）：复种循环/阶段推进/每小时干燥+产量惩罚/四种肥料倍率；`yield()`；`now()`。
   - 注意语义：`update(...)` 可能返回**新数组**（复种超限 → 全 0），调用方必须赋值回去（插件就是 `berryData = updatePlantDetails(berryData)`）。
5. `BerryPlantsTest` 4 例。
6. **`Data/berry_plants.dat` 已导出**：`pbs.js#buildBerryPlants` → `generated/pbs/berryplants.json`（64 项，按道具 id）→ `PbsData.berryPlants` + `berryPlantData(itemId)`（缺省回落 `[3,15,2,5]`）。
7. `MenuService.Kind.CHOOSE_ITEM` + `Request.filter` → `BagModel.chooseFilter` → `BagView.chooseItem(filter)`/`pickedItem()` → `PauseMenuOverlay` 回填 `PbsData.Item.id`（对应 `pbChooseItemScreen(proc)`）。

## 3.4 R8 脚本块转译（javaHandlerRequired 140 → 43，97 块转正）

规则：每个 API 都指到插件行；编译器 `builder/src/script-compiler.js` 负责把 Ruby 翻成 IR，运行时 `EventInterpreter` 只认 IR。已做：

1. **关键道具/徽章/图鉴（29 块）**：`PBItems::X`/`PBSpecies::X` 等常量解析成内部名（`parseValue`）；`pbGetKeyItem(PBItems::X,n)` → `GIVE_KEY_ITEM`（**假道具**：不在 PBS Items 里的 `"itemXxxKey"` 只记日志、不进背包，照 `BW Get Key Item:174`）；`$Trainer.badges[i] = true` → `SET_BADGE`；`$Trainer.pokedex/pokepc = true` → `SET_TRAINER_FLAG`（`PokeBattle_Trainer:15/23`）；`pbSet(n,$Trainer.pokedexSeen/Owned)` → `{trainerStat:...}`。运行时新增 `TrainerState.pokedex/pokepc`（存档 + `Modular Menu:67/82` 门禁：图鉴/寄存系统入口，测试已同步）。
2. **Boss 奖励（32 块）**：`boss_reward(n)` → `BOSS_REWARD`（6 档 × 5 组随机奖励，`Boss_reward:6-64`）、`BossRewards.pokemon_reward` → `BOSS_POKEMON_REWARD`（40/30/20/10% 四池 + 1-3 只 + 封印球 ballused=26 + 中文名表）、`BossRewards.blissey` → `BOSS_BLISSEY`（经验储罐 `add_exp_pot`，`goldFinger:56-65` + `Settings:28 EXP_POT_MAX`）、`BossRewards.gholdengo_money` → `BOSS_GHOLDENGO_MONEY`。数据由临时脚本从 `Boss_reward` 段直接生成 `BossRewardsData.java`（120 条奖励 / 95 池条目 / 95 中文名，行号写在文件头）；消息按插件原文（`\se[ItemGet]` 走 `AudioManager.playSe`），多行消息用解释器的待播队列逐条 `pbMessage`。`TrainerState.expPot` 已存档。
3. **搭档（30 块）**：`pbRegisterPartner(:TYPE,"名字"[,partyId])` → `REGISTER_PARTNER`（`PField_Field:1399-1412`：载入 trainers.txt 队伍、`setForeignID` 随机 id、`trainerID/ot/calcStats` 盖到每只、存 `$PokemonGlobal.partner`）、`pbDeregisterPartner` → `DEREGISTER_PARTNER`。运行时 `GameState.Partner`（type/name/id/party）；**多人对战（double）仍未实现**，搭档目前只是状态。
4. **Pokemon 赠送（12 块）**：`p.trainerID/otgender/ballused` 三个属性（`POKEMON_SET` → `publicID/otGender/ballused`）；单调用形式 `pbAddPokemon(:SPECIES,lv)` → `GIVE_POKEMON`（建怪 + 进队/进盒 + 「{玩家}得到了{X}！」+ 盒子满文案，`PSystem_PokemonUtilities:67-83`）。

仍剩 **43 块**：`pbCrystalWarp` 12（传送菜单 + 16 帧黑幕）、`pbMessage_ex` 4 + `pbGet/pbSet/pbMessage` 楼层选择 4（需给 IR 加“带选项的消息返回值”）、`isConst?` Deoxys 形态循环 4、单块插件 API（`pbMrHyper`/`changeBalls`/`pbChangeShinyByNPC`/`teachEggMoves`/`resurrection2`/`battleOverlordflos`/`battleIronJugulis`/`pbGiveAllMemories`/`pbTalkToFollower`/`pbChoosePokemon`/`pbPokemonMart($battle_item)` 等）。

## 3.5 战斗入场过渡（KGC Special Transition）——最近一轮修正

> 本轮把上一版"默认白闪 + 冻结画面"的错误实现改成了本工程真正的过渡动画。详见 `docs/battle-ui-fixes-plan.md` 的"战斗入场动画"段。

- **纠错**：那批 `Graphics/Transitions/*` 素材不是废图，也不缺。本工程有脚本段 **`Transitions`（KGC Special Transition，1718 行）**，它 alias 了 `Graphics.transition` 并用 `judge_special_transition`（`Transitions:48-95`）把命名过渡变成一组类。
- **命名选择**：`PField_Visuals:44-76` 按 昼/夜（`PBDayNight.isDay?`）× 类型（0 野生/1 训练师/3 双打）× 地形（0 外面/1 室内/2 洞窟/3 水上）选名；`:98 Graphics.transition(40*1.25=50, "Graphics/Transitions/<anim>")`。名字是**类选择器**，不是图片文件。
- **逐行转译**（新文件 `runtime/core/.../battle/BattleEntryTransition.java`）：`SnakeSquares:640`、`DiagonalBubble:722`(origin 0/1/2/3)、`RisingSplash:805`、`TwoBallPass:911`、`SpinBallSplit:1011`、`ThreeBallDown:1121`、`BallDown:1224`、`WavyThreeBallUp:1322`、`WavySpinBall:1426`、`FourBallBurst:1527`。z 序按 RMXP 升序（`drawOrder`）；坐标把 RMXP "原点落在 (x,y)" 换算到 libgdx 左下角并翻 y；旋转取 `-angle`（RMXP 顺时针）。
- **`Graphics.snap_to_bitmap` 等价物**：`MapScreen.captureEntrySnapshot()` 在本帧世界画完后 `Pixmap.createFromFrameBuffer(sx,sy,sw,sh)` 抓屏（**本工程 libgdx 没有带 `flipY` 的 `drawPixmap` 重载**，所以手工逐行翻转），BiLinear 缩放到逻辑屏，交 `BattleEntryAnimation.setSnapshot`；动画播完 `battleEntry.dispose()` 释放贴图。
- **流程**：DEFAULT = 白/黑闪 32 帧（`location==2 || isNight?` → 黑）→ 过渡 50 帧 → 黑幕 4 帧；`rocket` 邪恶组织 VS 与内建 VS 两条路径不变。
- **测试钩子**：`-Dpokemon.daynight.hour=10` 钉死昼/夜（`DayNightTone.currentHour()`）。
- **验证（像素统计）**：`l1-battle-flash`/`l1-transition0..4`（`SnakeSquares` 逐行覆盖→全黑，相邻帧差异 16%→100%）、`l1-trainer-flash`/`l1-trainer-transition0..3`（挑无 `vsTrainer` 图的训练师 type 158 → `TwoBallPass`，球从两侧飞过、黑幕合拢→全黑）。
- **未做/差异**：生成数据只有 metadata `Outdoor`，`location` 只能取 0/1（洞窟/水上判不出）；`Transitions` 里战斗入场命不到的其他类（BreakingGlass/Splash/Mosaic/Scroll…）未转译。
- **易踩的坑**：`BattleEntryTransition.update()` 用的下标必须与创建顺序一致，`render()` 才用 z 排序后的 `drawOrder`（曾把两者混在一起导致球/黑幕不出现）。

## 4. 树果交互本体（已做完，见 3.3；下面是当时的清单，留作参考）

目标文件：新建 `runtime/core/src/main/java/pokemon/runtime/event/BerryInteraction.java`（步进机），照 `PField_BerryPlants:313-590`：

1. **入口**：`InterpreterState.WAIT_BERRY` + `EventInterpreter` 的 `BERRY_PLANT` / `BERRY_PICK` case（照 `SHOW_TEXT` 的范式：`1016-1031`；等待态照 `advanceWait` 439-463；消息/选项泵照 `updateMessage` 520-550）。
2. **`begin`**：读事件变量（空则 `[0,0,0,0,0,0,0,0]`）→ `BerryPlants.update(...)` 结算生长 → 写回 → `turnEvent`（阶段 1/2 下、3 左、4 右、5 上）→ 按阶段分派。
3. **阶段 0**：`pbMessage("泥土看起来相当的松软。", [施肥, 种植, 返回], -1)`；
   - 施肥 → `CHOOSE_ITEM(pbIsMulch?)` → `berryData[7]=id` → 提示 → `pbConfirmMessage("想要种植树果吗？")` → 选树果 → 种下；
   - 种植 → `CHOOSE_ITEM(pbIsBerry?)` → 种下；
   - 种下 = `[1,berryId,0,now,100,0,0,mulch]` + 背包 -1 + `"{X}种在了土里。"`。
4. **阶段 1/2/3/4**：一句提示（「这里种着{X}。」/「{X}已经发芽了。」/「{X}长得很大了。」/「{X}开花了。」），然后浇水循环：按 `SPRAYDUCK`→`SQUIRTBOTTLE`→`WAILMERPAIL`→`SPRINKLOTAD` 顺序，**有哪个就问**「想用{1}浇水吗？」→ `berryData[4]=100` + 「{X}浇了水。」+「它看起来很开心！」。
5. **阶段 5**：`berrycount = max(maxYield - berryData[6], minYield)`；`pbConfirmMessage("这里有{N}个{X}！\n要摘下吗？")` → `pbCanStore?` → 进包 → 「摘下了…」→「{X}将…放进了…袋」→「土壤又变得松软了。」→ `berryData=[0,0,0,0,0,0,0,0]`。
6. **`pbPickBerry(berry,qty)`**：预置树果事件，确认+进包+重置（并在结束时 `pbSetSelfSwitch(thisEvent.id,"A",true)`）。
7. **编译器**：`builder/src/script-compiler.js` 的 `HANDLERS` 加
   `pbBerryPlant()` → `{command:"BERRY_PLANT"}`、`pbPickBerry(args)` → `{command:"BERRY_PICK", berry:args[0], qty:args[1]??1}`，
   然后 `build-data` 校验 `javaHandlerRequired` 从 **582 → 341**（241 块转正）。
8. **显示**（可后做）：事件图 `berrytree<道具名>`（缺则 `sprintf("berrytree%03d",id)`），阶段靠朝向；湿润度 `berrytreeDry/Damp/Wet`。

## 5. 更大的工作单（780 块，不是 18 万行）

构建器每次 `build-data` 都会写：
- `pokemon-builder/build/reports/java-handler-required.json`（**当前 43 块**，带 `apis`/`rubySource`/`reason`）
- `pokemon-builder/build/reports/script-coverage.json`（`blocks=5441 / translated=5202 / coveragePercent=96.4`，`problems.unsupported=196` 块 + `problems.javaHandlerRequired=43`）

测试现状（改完必跑）：`core` **412** 个 `@Test` 全绿；`builder` `node --test --test-reporter=tap builder/tests/*.test.js` **293** 全绿。

43 块按 API：`pbCrystalWarp=12`、`pbGet=7`、`pbMessage_ex=4`、`isConst?=4`，其余单块 `hasRibbon?`/`pbGenPkmn`/`pbMrHyper`/`changeBalls`/`pbChangeShinyByNPC`/`battleOverlordflos`/`teachEggMoves`/`pbChoosePokemon`/`pbDeleteItem`/`resurrection2`/`pbRegisterPartner`/`battleIronJugulis`/`pbGiveAllMemories`/`pbPokemonMart`/`pbTalkToFollower` 各 1。

140 → 43 的转正轨迹：R8-1 关键道具/徽章/图鉴 29 块 → 111；R8-2 Boss 奖励 + Pokemon 属性 32 块 → 79；R8-3 搭档 30 块 → 49；R8-4 `pbAddPokemon` 单调用 6 块 → 43。

当前 43 块按系统分布：
1. `pbCrystalWarp` 12（传送菜单 + `pbShadowWarp` 黑幕）
2. `pbMessage_ex` 4 + `pbGet/pbSet/pbMessage` 楼层选择 4（需 IR 支持“带选项消息的返回值”）
3. `isConst?` Deoxys 形态循环 4
4. 单块插件 API：`pbMrHyper`/`changeBalls`/`pbChangeShinyByNPC`/`teachEggMoves`/`resurrection2`/`battleOverlordflos`/`battleIronJugulis`/`pbGiveAllMemories`/`pbTalkToFollower`/`pbChoosePokemon`/`pbPokemonMart($battle_item)`/`hasRibbon?`/`pbPokemonFollow` 等

196 块 unsupported 的构成：复杂表达式 26（需脚本翻译器 R7）／跟随宝可梦 40／老虎机 24（**跳过**）／天气 17／缎带 11／事件时序 19／名人堂 5／寄放屋 7／挖矿 1（**跳过**）／其余零散。

后续大方向（用户指定顺序）：**道具 → 招式 → 场地 → 天气**效果。

## 6. 已知未做（战斗侧挂账）

- 战斗 BGM / 胜利 ME：数值在 `PBS/metadata.txt` 的 `[000]`（`WildBattleBGM`/`TrainerBattleBGM`/`WildVictoryME`/`TrainerVictoryME`），**生成器还没导出这段全局元数据**；播放点 `pbBattleMusic`/`pbWildBattleSuccess`/`pbTrainerBattleSuccess`（`pbBattleAnimation` 里 `$game_system.bgm_pause` + `pbBGMPlay`）。
- 进战斗过渡**已做**（`BattleEntryTransition` + `MapScreen.captureEntrySnapshot`，见 §3.5）；内建 VS / `rocket` 邪恶组织 VS 也已做（见 §3.1）。仍缺：叫声 `pbPlayCry`、闪光/超闪光 `pbCommonAnimation("Shiny"/"SuperShiny")`、换人收回 `BattlerRecallAnimation`、多人对战（`sideSizes>1` 的 `pbBattlerPosition` 偏移）、`vsBar<Type>_<outfit>` 的 outfit 变体（`$Trainer.outfit` 未建模，走数字回退）、对战双方名字字体缺字仍显示 `???`。
- 生成数据只带 metadata `Outdoor`，没有"洞窟/水上"标记，所以入场 `location` 只能 0/1。
- `[F]:战斗信息` / `[Z]:快速捕捉` 提示（ES 信息面板 + 快速捕捉两个插件）。
- 招式/场地/天气效果（未开始）。

## 7. 工程模型缺口（曾用占位的地方）

- 详情页/训练家卡：无 `publicID`/`startTime`/`pokedex` 标志。
- `Storage` 是紧凑列表（无固定 30 格 / 盒名 / 背景）。
- `ribbons` 存的是名字字符串（插件是 int id）。
- `PBS/natures.txt` 不存在（性格名英文）。
- `ItemUse.evolution` 里的 `SpecialItem/PhantomItem/MiloticmItem/TinkatonItem` 是按工程特例硬写的，未经逐条核对。
