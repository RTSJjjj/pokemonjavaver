# 战斗界面 7 条差异修正（② 已实施；下面是逐条出处与方案）

对照：图1 = 当前 Java 工程，图2 = 原工程（`Graphics`/插件）。
铁律：一切以本工程插件为准；④ 是用户指定的 Java 版修正（原工程本身没做好）。

## 实施记录（已全部完成）

### 追加：战斗入场动画（`PField_Visuals` + `rocket`）

- **`PField_Visuals:18-133 pbBattleAnimation`**：战斗前先在**地图上**放过渡动画（viewport 在地图之上），结束于黑幕，再由战斗场景自己的开场把黑幕淡出。三种模式：
  1. **`rocket:6-101`（邪恶组织 VS，优先）**：`EVIL_TEAMS = {211→001_Rocket, 210→000_Shadow, 212→002_Magma, 213→003_Aqua, 214→005_Plasma, 215→006_Hyacinth, 216→007_Galactic}`，任一开关开 → `Graphics/Transitions/<图>` 居中，110 帧：白闪（`vsFlash`，26/帧淡出）→ 抖动 `j=i%4`（1..2 帧 x+2,y-2 否则 x-2,y+2）→ 50 帧起黑底 tone → 100 帧起 0→255 黑幕 → `viewport.color=黑`；SE `Vs sword`。
  2. **`PField_Visuals:135-291`（内建 VS）**：单训练师战且有 `vsBar<type>`/`vsTrainer<type>` 时才走。10 帧条滑入 → 48 帧（白闪淡出、条滚动 `ox∓16/帧`、24..34 帧训练师滑入）→ VS logo + 双方名字 + 失败方解除黑 tone → 110 帧（抖动 70 帧、放大 0.2/帧、100 帧起黑幕）。SE `Vs flash`/`Vs sword`。
  3. **`PField_Visuals:77-106`（默认）**：白（洞窟/夜晚为黑）闪 2×16 帧 → `Graphics.freeze` + `Graphics.transition(50, "Graphics/Transitions/<anim>")` → 黑幕 + 4 帧停顿。`<anim>` 由 `PField_Visuals:44-76` 按 **时刻（昼/夜）×战斗类型（野生/训练师/双打）×地形（0 外面/1 室内/2 洞窟/3 水上）** 选定。
- **过渡动画不是"没有素材"**：本工程自带 **KGC Special Transition**（脚本段 `Transitions`，1718 行），它把 `Graphics.transition` 接管成一组类（`Transitions:48-95 judge_special_transition`），素材就在 `Graphics/Transitions/`（`black_square`/`black_half`/`ball_small`/`ball_large`/`black_curve`/`black_wedge_1..4`/`water_1`/`water_2`）。战斗入场能命中的已逐行转译进 `BattleEntryTransition`：`SnakeSquares`(`:640`)、`DiagonalBubble`(`:722`，4 个 origin)、`RisingSplash`(`:805`)、`TwoBallPass`(`:911`)、`SpinBallSplit`(`:1011`)、`ThreeBallDown`(`:1121`)、`BallDown`(`:1224`)、`WavyThreeBallUp`(`:1322`)、`WavySpinBall`(`:1426`)、`FourBallBurst`(`:1527`)。会"扭曲冻结画面"的那几个用 `Graphics.snap_to_bitmap` 的等价物：`MapScreen.captureEntrySnapshot()` 在本帧世界绘制后从 framebuffer 抓屏（`Pixmap.createFromFrameBuffer` + 手工垂直翻转，因为本工程 libgdx 没有 `drawPixmap(...flipY)` 重载），缩放到逻辑屏 672×488 后交回动画。z 序按 RMXP 升序（`drawOrder`）；坐标把 RMXP 的"原点落在 (x,y)"换算到 libgdx 左下角并翻转 y 轴，旋转取 `-angle`（RMXP 顺时针）。
- 运行时：新增 `BattleEntryAnimation`（`pokemon.runtime.battle`），由 `MapScreen` 在战斗 pending 时先播（地图照常绘制、动画以屏幕空间叠在上面；播放期间不处理移动输入），播完才创建 `BattleScreen`；战斗结束后 `battleEntryPlayed` 复位。夜晚判定 `DayNightTone.test("isNight?")`；地形只能取 0/1（本工程生成数据只有 metadata `Outdoor`，没有洞窟/水上）；测试钩子 `-Dpokemon.daynight.hour=10` 可钉死时刻。
- 截图（像素统计验证）：`l1-battle-flash`（白天=白闪）、`l1-transition0..4`（`SnakeSquares` 逐行覆盖→全黑，帧间差异 16%→100%）、`l1-trainer-flash`/`l1-trainer-transition0..3`（`TwoBallPass`：球从两侧飞过、黑幕合拢→全黑），以及原有 `l1-battle-evil-vs0..3`（火箭队 R 抖动→放大→黑幕）、`l1-battle-entry-vs`（内建 VS：双方立绘+VS 标+名字）。
- 未做：战斗 BGM/胜利 ME（`pbBattleMusic`/`pbGetTrainerVictoryME`，生成器还没导出 metadata `[000]`）；`vsBar<Type>_<outfit>` 的 outfit 变体（`$Trainer.outfit` 未建模，走数字回退）；对战双方名字里字体缺字的仍显示 `???`。

---

### 追加：经验条/音效与消息同拍 + 训练师胜利收尾

- **经验条与消息同拍**：`PokeBattle_Scene:115-135 pbDisplayMessage(brief=true)` 的语义是"A brief message lingers on-screen while other things happen"（`@briefMessage`）。经验那句改成 brief（`showExpBriefMessage` → `showBattleMessage(text,false)`），`stepExpGain` 紧接着就启动进度条与 SE（不再等确定）；brief 消息只在确定或下一条消息替换时消失（`tickBriefMessage`，不阻塞阶段机）。升级/学招式那几句仍是 `pbDisplayPaused`（要确定）。
- **训练师胜利收尾**（`Battle_StartAndEnd:444-473`）：WIN 且是训练师战 → 新 `Stage.TRAINER_END`：①「你打败了\n{fullname}！」（`:456`）→ ② `pbShowOpponent`（`TrainerAppearAnimation`，`Scene_Animations:272-297`：从 `x+64+W/4` 8 帧滑到 `x+64`）→ ③ 失败台词 `trainers.txt` 的 `LoseText`（空则 `"..."`，`:466-467`，`\PN` 用 `MessageText.clean` 替换）→ ④ 奖金「你赢得了${N}！」（`:407-409`，`N = 对方队伍最高等级 × moneyEarned`，`moneyEarned`=`trainerTypes.baseMoney` 缺省 30，`PokeBattle_Trainer:77-81`）→ ⑤ `pbEndBattle` 淡出（`:296-303`）。
- 顺带：`pushMessages` 现在对每条消息跑 `MessageText.clean`（`\PN`/`\se[]`/颜色码按事件消息同样处理）；`Session.trainerFullname/endSpeech/prizeMoney` 新增并在获胜时结算奖金。
- 截图：`l1-battle-trainer-end4`（你打败了…）、`end6`（对手已滑回 + 你赢得了$400！）、`end8`（已淡出回地图）。

---

- 文字阴影：`MenuFont.shadowEnabled` 默认恢复 **true**（`-Dpokemon.menu.shadow=false` 可关）。
- ① 性别符号色：`BattleScreen` 加 `MALE_BASE(48,96,216)` / `FEMALE_BASE(248,88,40)` / `NAME_SHADOW(112,112,112)`，符号与名字都用插件色。
- ② 对方性别：`Pokemon.displayGender()`（`PokeBattle_Pokemon:166-177`）+ `PokemonStats.genderByte`（`PBGenderRates:11-23`）；数据盒两边都画。
- ③ HP 数值：`BattleScreen.drawBattleNumber`（`icon_numbers` 132x16＝11 帧×12px），只我方；HP 右端 `baseX+138`、`/` 在 138、总数从 `baseX+150`，`y=topY+46`；等级数字也改用该位图（`:239`）。数值取动画中的 `hpShown[0]`。
- ④ Mega 槽：`(w-151)/2 = 260.5`（右移 50px，有意偏离插件 210）。
- ⑤ 跃动：`BattleScreen.frameCounter`（40fps）+ BATTLE 阶段 `page 0/1` 时数据盒 ±2（相位 1→-2、3→+2）、宝可梦 ±2（反向）；周期 24 帧（`QUARTER_ANIM_PERIOD=6`）。截图回归 `l1-battle-bob0..3` 实测两者都按 4px 摆幅反向摆动。
- ⑥ 开场提示语：`InteractiveBattlePort.MESSAGE_BREAK="\f"` 区分「多条消息」，`"\n"` 保留为同一窗口内换行；`BattleScreen.pushMessages` 按 `\f` 切、`drawMessageWindow` 按 `\n` 分行画（96px 内最多两行，块居中）。
- ⑦ 结算：`Battle.ExpAward` + `awardExperience` 照 `pbGainExpOne` 全量公式（SCALED=真、SPLIT=假、训练师×1.5、外来×1.5、幸福≥180×1.2、**超闪光×1.2** `PokeBattle_Pokemon:335`）；`Stage.EXP_GAIN` 逐条播「获得了N点经验值！」→ 逐级 exp 槽（1.75s/满槽）→「升到了N级！」+ 右上能力窗（`drawLevelUpWindow`）→ 学招式 → 经验储罐行。
  - **等级上限**（`:167-186` 与逐级 `:221-257`）：`Battle.levelLockOn/leaguePass/badges` + `MAX_LEVEL`（`Settings:29`）；被锁住的那份经验转进经验储罐（`potGain`），无 exp 消息/进度条。开关来自 `$game_switches[199]/[12]`，`EventInterpreter.applyBattleSwitches()` 在每场战斗前写入。
  - **学招式**（`pbLearnMove:312-343`）：升级时把该等级要学的招式记进 `ExpAward.movesToLearn`，结算里播「{X}学会了{Y}！」+ SE `Pkmn move learnt`；**四招已满时的遗忘流程未做**（`pbForgetMove` 的选招 UI 战斗场景还没有），只播「想要学会…可是已经学会四个招式了。」
  - **音效**：`Pkmn exp gain`（每段起，`PokeBattle_SceneElements:197`）；**填充时长按经验量成正比**（`@expIncPerFrame = rangeExp/(1.75*40)`），填完立刻 `pbSEStop`（`:368`）；满槽时 `pbSEStop` + 数据盒 8 帧闪蓝（`:354-362`）+ `Pkmn exp full`。`AudioManager.stopSe()` 照 `pbSEStop`（Audio_Play:223）实现。
    - ⚠️ **有意加的下限**：`Pkmn exp gain.ogg` 实测 2.68s（给 1.75s 满槽用）。插件按比例算时长，6 点经验只够 0.4 帧 → 第一帧就 `pbSEStop`，只剩一声"咔"（插件本身**没有**最小值处理）。按用户要求加 `MIN_EXP_SEGMENT_SECONDS = 0.5s`（每段至少 0.5s），既不是整段 2.68s、也不是 1 帧。
    - 用户确认本工程只有 `Pkmn exp gain`/`Pkmn exp full` 两个文件，故不再有 `Pkmn level up`。
  - 截图为证：`l1-battle-settle2`（exp 消息）、`settle3`（升级语+能力窗，小量经验几乎瞬填）、`settle11`（「妙蛙草升到了51级！」+ 右上能力窗）。
  - 仍未做：遗忘招式选招 UI；`$game_switches[199]` 之外的等级上限细节（`goldFinger`/`PField_DayCare`/道具的同类判断）。
- 顺带：`GameState.playerName(...)` 同步 `trainer.name`（Essentials 只有一个 `$Trainer.name`），外来判定与赠礼文案才一致。
- 测试：core 全绿（新增 `PokemonStatsTest.genderBytes`、`PokemonGrowthTest.derivedGender`、`BattleTest.expAwardFollowsThePluginFormula`）。

---


## ① 性别符号没有颜色

- **插件**：`PokeBattle_SceneElements:16-21`
  `NAME_BASE_COLOR=(255,255,255)`、`NAME_SHADOW_COLOR=(112,112,112)`、
  `MALE_BASE_COLOR=(48,96,216)`、`FEMALE_BASE_COLOR=(248,88,40)`（两个性别都用 `NAME_SHADOW_COLOR`）；
  `:223-229` 画符号：`textPos.push([♂/♀, @spriteBaseX+1, 12, false, 性别色, NAME_SHADOW_COLOR])`。
- **现状**：`BattleScreen.drawDataBox`（`font.draw(batch, "♂/♀", baseX+1, h-(topY+12))`）用的是默认字体色（白+菜单灰），所以图1里符号是白的。
- **修法**：符号改用显式主色/阴影色 `font.draw(batch, symbol, x, y, main, shadow)`：
  男 `(48,96,216)` / 女 `(248,88,40)`，阴影 `(112,112,112)`。位置不变（`baseX+1, topY+12` 已对齐插件）。

## ② 对方宝可梦没有性别符号

- **插件**：性别是**算出来的**，不是存的：`PokeBattle_Pokemon:166-177`
  ```
  AlwaysMale→0 / AlwaysFemale→1 / Genderless→2
  否则 @genderflag（显式设置过就用它）或 ((@personalID & 0xFF) < PBGenderRates.genderByte(rate)) ? 1 : 0
  ```
  `PBGenderRates:11-23`：FemaleOneEighth=32、25%=64、50%=128、75%=192、7/8=224、AlwaysFemale=254。
  数据盒画符号的代码（`:224`）**没有分敌我**，两边都该有。
- **现状**：`Pokemon.gender` 默认 `GENDERLESS`，只有 trainers.txt 写了性别时才赋值
  （`HeadlessBattlePort:156-157`）。野生/玩家宝可梦从来没算过 → 对方符号根本不出现
  （图1 左侧我方有符号是因为 capture 测试种子里手动设了 `gender=0`）。
- **修法**：给 `Pokemon` 加**派生 getter** `gender()`：优先 `genderflag`（现字段改名/保留为显式覆盖），
  否则按 `personalID & 0xFF` 与 `species.genderRate` 的 `genderByte` 比较；
  AlwaysMale/Female/Genderless 直接返回 0/1/2。数据盒与 `displayGender` 都改用它。
  顺带 `PbsData.Species.genderRate` 已是字符串（如 `Female50Percent`），映射表照 `PBGenderRates` 写。

## ③ 我方 HP 数值 x/y 没画

- **插件**：`PokeBattle_SceneElements:50-51` 只有**我方**（sideSize==1）`@showHP=true`；
  `:84-86` 建 `140x18` 的 `hpNumbers` 位图（`pbSetSmallFont`）；`:123/:130` 位置
  `x = dataBox.x + 8 + 84`、`y = dataBox.y + 44`；`:281-289` 画法：
  ```
  pbDrawNumber(self.hp, 54, 2, align=1)   # 右对齐，右端在 54
  pbDrawNumber(-1(=“/”), 54, 2)
  pbDrawNumber(totalhp, 66, 2)
  ```
  数字贴图 `Graphics/Pictures/Battle/icon_numbers`（**132x16 = 11 字 × 12px**，第 11 个是“/”）。
  `self.hp` 是**动画中的值**（`@currentHP`），所以数值跟血条一起滚。
- **现状**：Java 完全没有画数值。
- **修法**：`BattleScreen` 加 `drawBattleNumber(batch, value, x, y, align)`（用 `icon_numbers` 条带，
  `-1` 画“/”），在 `drawDataBox` 里**只对我方**画：
  HP 右端 `baseX+84+54`、`/` 在 `baseX+84+54`、总数从 `baseX+84+66` 起，`y = topY+44+2`；
  取值用已有的 `hpShown[0]`（四舍五入）与 `maxHp()`。

## ④ Mega 按钮居中（用户指定：Java 版修正，原工程 210 没居中）

- **插件**：`PokeBattle_SceneMenus:275-281`：`@megaButton.x = self.x+210`（self.x=0）、
  `y = (Graphics.height-96) - height/2`、帧高 `height/2`；`cursor_mega.png` 实测 **151x150**（帧 151x75）。
- **现状**：`BattleScreen.drawFightMenu` 照抄 210 → 图1 偏左。
- **修法**：`x = Graphics.width/2 - 151/2 = 260.5 → 260`（整数，右移 50px）；y 不动
  （`352-75=277` 顶边，现 Java 的 `h-(BAR_Y-half)-half` 已经等价）。文档里标注这是**有意偏离插件**。

## ⑤ 玩家回合的上下跃动（宝可梦 + 数据盒）

- **插件**：`PokeBattle_SceneElements:374-386`（数据盒）与 `:602-618`（宝可梦精灵）：
  `QUARTER_ANIM_PERIOD = 40*3/20 = 6 帧`；`phase = (frameCounter/6).floor % 4`；
  `@selected==1`（正在为这只选指令）时：phase1 → y+2（下），phase3 → y-2（上），其余 0；
  **数据盒与精灵同相位同方向**。`@selected` 由 `pbSelectBattler(idx,1)` 设置
  （`Scene_Commands:31 pbCommandMenuEx`、`:105 pbFightMenu`），攻击阶段开始清 0
  （`PokeBattle_Scene:288-291 pbBeginAttackPhase`）。
- **现状**：Java 无此动画。
- **修法**：`BattleScreen` 加 40fps 帧计数（`frameCounter += delta*40`），在 BATTLE 阶段
  `page==0||page==1`（指令/战斗菜单打开）时，把我方数据盒与我方宝可梦精灵的绘制 y 加 ±2
  （phase 1 → +2，phase 3 → -2）。注意数据盒的 y 偏移只影响数据盒自身（HP/EXP 条跟随）。

## ⑥ 训练师开场提示语被拆成两条

- **插件**：`Battle_StartAndEnd:220` `pbDisplayPaused("{1}\n向你发起挑战！")` —— **一条消息两行**。
- **现状**：`BattleScreen.pushMessages` 按 `\n` 拆成多条 Message → 两个窗口（图1 现象）；
  且 `drawMessageWindow` 只画一行（`message.text` 单行居中）。
- **修法**：
  1. `pushMessages` 不再按 `\n` 拆（整段作为一条消息）；
  2. `drawMessageWindow` 按 `\n` 分行画（战斗消息窗口 96px 高，最多 2 行；插件窗口
     `Scene_Initialize:116-121`：`(16, 448-96+2, 640, 96)`，`baseColor=(248,248,248)`、
     `shadowColor=(104,104,104)`、逐字显示），逐字进度覆盖整段（含换行）。
  3. 其它多行消息（如“{X}想要学会{Y}。\n可是它已经学会四个招式了。”）同样受益。

## ⑦ 战斗结算 + EXP 槽动画（完全没做）

- **插件**：
  - `Battle_ExpAndMoveLearning:5-66 pbGainExp`：对每只倒下的敌人 → 参战/带学习装置/经验全开者
    逐个 `pbGainExpOne`；结束再算经验储罐（:58-61）。
  - `:99-308 pbGainExpOne`：`Settings:162 SCALED_EXP_FORMULA=true`、`:163 SPLIT_EXP_BETWEEN_GAINERS=false`；
    `a = 敌Lv * 敌baseExp` → 参战者 `a/(numPartic)`（非分割）→ 训练师战 ×1.5 → `/5` 后
    `levelAdjust=((2L+10)/(L+L'+10))^5` 开方 → 参战 +1 → 外来 ×1.5/1.7 → 道具/幸福度≥180×1.2/
    超闪光×1.2/EXPCHARM×1.5 → 等级上限（`Settings:29 MAX_LEVEL` + `$game_switches[199]` + 徽章）
    或 Lv≥200 时 `exp_pot += max(1, exp/8)`、exp=0；
    **消息** `:196 "{X}获得了{N}点经验值！"`（pbDisplayPaused）；
    **逐级动画** `:258-263`：`levelMinExp/levelMaxExp` → `@scene.pbEXPBar(battler, levelMinExp, levelMaxExp, tempExp1, tempExp2)`；
    **升级** `:283 "{X}升到了{N}级！"` + `:284 pbLevelUp`（右上角六项能力窗）+ `:286-296` 学招式
    （`pbDisplayConfirm("要{X}立即学习招式吗？")` → `pbLearnMove`）；结尾 `:298-307` 经验储罐。
  - `Scene_Animations:273-281 pbEXPBar` → `dataBox.animateExp(startExpLevel,endExpLevel,expRange)`
    并等 `animatingExp` 结束；`PokeBattle_SceneElements:11-12` `EXP_BAR_FILL_TIME=1.75s`（满槽时间）、
    `:336-371 updateExpAnimation`（满槽闪蓝光 `Exp full` SE）。
  - `Scene_Animations:286-294 pbLevelUp`：右上窗口先画“最大HP +n / 攻击 +n / …”再画总值。
- **现状**：`Battle.awardExperience` 用简化公式 `floor(baseExp*level/7)` 平均分给全队、**静默**
  （`lastExpGain` 只进日志）；`BattleScreen` 虽有 `expShown/expFrom/expTo/expT` +
  `EXP_BAR_FILL_TIME=1.75`，但没人驱动；打完直接 `finished → port.finish()`。
- **修法**（建议分两步）：
  1. **数据侧**：`Battle` 按 `pbGainExpOne` 全量改写：参战者记录（singles 即当前出战者）、
     完整公式、每级的分段经验（`levelMinExp/levelMaxExp/tempExp1/tempExp2`）、升级标记、
     学会招式列表、经验储罐；产出结构化结果（如 `Battle.expResults`）供界面播放。
  2. **界面侧**：`BattleScreen` 新增结算阶段（如 `Stage.EXP_GAIN`，在 BATTLE 之后、finish 之前）：
     逐条播消息 → 每级 `expFrom/expTo` 段动画（1.75s/满槽）→ 升级消息 + 右上能力窗 →
     学招式确认（复用现有 CHOICE/消息机制）→ 再结束战斗。
     右上能力窗（`pbTopRightWindow`）Java 侧还没有，需要新画一个（或先只播消息，窗口留后续）。

---

### 影响文件速查

| 项 | 文件 | 方法 |
|---|---|---|
| ①③ | `BattleScreen.java` | `drawDataBox`（性别色、HP 数值），新增 `drawBattleNumber` |
| ② | `Pokemon.java`（派生 `gender()`）+ `BattleScreen`/`Battler` 取值处 | 照 `PokeBattle_Pokemon:166-177`、`PBGenderRates:11-23` |
| ④ | `BattleScreen.java` | `drawFightMenu` 的 mega x → 260 |
| ⑤ | `BattleScreen.java` | 新增 frameCounter + 绘制偏移（BATTLE 阶段 page 0/1） |
| ⑥ | `BattleScreen.java` | `pushMessages`、`drawMessageWindow` |
| ⑦ | `Battle.java`（公式/结果）+ `BattleScreen.java`（EXP_GAIN 阶段、能力窗）+ `PokemonGrowth`（学招式） | 照 `Battle_ExpAndMoveLearning` / `Scene_Animations:273-294` |
