# 阶段 3 · 战斗界面 + 入场过渡 交接文档

> 面向接手**战斗界面/战斗入场动画**的工程师/模型。
> 先读总交接 `docs/stage3-berry-port-handoff.md`（铁律/环境/全量工作单）与实施记录 `docs/battle-ui-fixes-plan.md`。
> 本轮工作区**未提交**，全部改动都在 `E:\仓库\范例\929` 工作树里。
>
> ⚠️ **铁律第 2 条（2026-10 用户加严，最高优先级）**：战斗界面/演出必须**逐行转译插件 Ruby**，
> 禁止自创控制流、自创动画、"看着像"的实现与"等价近似"；每行改动注明 `段名:行号`；
> **无法转译就立刻停止并报告用户**。此前战斗界面正是"看着像"写出来的，导致派出动画每回合重放、
> "去吧！"消息缺失等一批 bug（见 `docs/stage3-wild-battle-audit.md` §18）。

---

## 0. 一句话现状

- 战斗界面（`BattleScreen`）与战斗入场动画已照本工程插件重做并逐条核对；**本轮重点是纠正入场过渡**：上一版把默认入场实现成"白闪 + 冻结画面"，是错的。
- 测试基线：**core 412 个 `@Test` 全绿**、**builder `node --test` 293/293 全绿**。
- 脚本覆盖：`blocks=5441 / translated=5202 / coveragePercent=96.4`；`problems.unsupported=196`、`problems.javaHandlerRequired=43`。

---

## 1. 本轮做了什么

### 1.1 战斗入场过渡 = 本工程的 KGC Special Transition（纠错）

- **不是"没有素材"**。`Graphics/Transitions/*` 属于脚本段 **`Transitions`（KGC Special Transition，1718 行）**：它 alias 了 `Graphics.transition`，并用 `judge_special_transition`（`Transitions:48-95`）把命名过渡变成一组类。
- **名字怎么选**：`PField_Visuals:44-76` 按 昼/夜（`PBDayNight.isDay?`）× 战斗类型（0 野生 / 1 训练师 / 3 双打）× 地形（0 外面 / 1 室内 / 2 洞窟 / 3 水上）选一个名字；`PField_Visuals:98 Graphics.transition(40*1.25 = 50, "Graphics/Transitions/<anim>")`。名字是**类选择器**，不是图片文件。
- **已逐行转译**（新文件 `runtime/core/src/main/java/pokemon/runtime/battle/BattleEntryTransition.java`）：

  | 名字 | 行号 | 用到的素材 |
  |---|---|---|
  | `SnakeSquares` | `Transitions:640` | `black_square` |
  | `DiagonalBubble`（origin 0/1/2/3） | `:722` | `black_square` |
  | `RisingSplash` | `:805` | `water_1`/`water_2`/`black_half` + 冻结画面 |
  | `TwoBallPass` | `:911` | `black_half`/`ball_small` + 冻结画面 |
  | `SpinBallSplit` | `:1011` | `black_half`/`ball_large` + 冻结画面 |
  | `ThreeBallDown` | `:1121` | `black_square`/`ball_small` + 冻结画面 |
  | `BallDown` | `:1224` | `black_half`/`black_curve`/`ball_small` + 冻结画面 |
  | `WavyThreeBallUp` | `:1322` | `black_half`/`ball_small` + 冻结画面 |
  | `WavySpinBall` | `:1426` | `black_half`/`ball_large` + 冻结画面 |
  | `FourBallBurst` | `:1527` | `black_wedge_1..4`/`ball_small` |

- **`Graphics.snap_to_bitmap` 的等价物**：`MapScreen.captureEntrySnapshot()` 在本帧世界画完后从 framebuffer 抓屏（`Pixmap.createFromFrameBuffer(sx,sy,sw,sh)`），手工逐行翻转（**本工程 libgdx 没有带 `flipY` 的 `drawPixmap` 重载**），再 BiLinear 缩放到逻辑屏，交 `BattleEntryAnimation.setSnapshot()`；动画播完 `battleEntry.dispose()` 释放贴图。
- **入口流程**（`BattleEntryAnimation`，DEFAULT 模式）：白/黑闪 32 帧（`location==2 || isNight?` → 黑，否则白）→ 该命名过渡 50 帧 → 黑幕 4 帧。`rocket:6-101` 邪恶组织 VS、`PField_Visuals:135-291` 内建 VS 两条路径**未改**。

### 1.2 渲染约定（照 RMXP，别改错）

- z 序：`update()` 用**创建顺序**的下标，`render()` 用按 z **稳定排序**后的 `drawOrder`。曾把两者混用，导致球/黑幕不出现。
- 坐标：RMXP 的 `(x,y)` 是"位图原点落点"，换算到 libgdx 左下角要翻 y：`dy = h - (y - oy*zoomY) - h*zoomY`。
- 旋转：RMXP 角度是顺时针，libgdx 是逆时针，取 `-angle`；旋转原点 `(ox*zoomX, dh - oy*zoomY)`。

### 1.3 测试钩子

- `-Dpokemon.daynight.hour=10` 钉死昼/夜（`DayNightTone.currentHour()`），便于截图/复现。
- 截图探针新增两种分支：`-PcaptureArgs=X:\generated|<out>|transition`（野生 → `SnakeSquares`）与 `...|transition-trainer`（挑没有 `vsTrainer` 图的训练师 → `TwoBallPass`）。

---

## 2. 怎么验证

```bat
REM 盘符视图（丢了就重建）
subst X: "E:\仓库\范例\929\pokemon-builder"
set JAVA_HOME=C:\Users\Administrator\AppData\Roaming\.minecraft\runtime\java-runtime-delta

REM 编译 + core 测试
cmd /c 'X: >nul && cd X:\runtime && .\gradlew.bat :core:test :lwjgl3:compileJava --console=plain'

REM 截图（<out> 在 X:\runtime\lwjgl3\<out>）
cmd /c 'X: >nul && cd X:\runtime && .\gradlew.bat :lwjgl3:captureMenu "-PcaptureArgs=X:\generated|l1x|transition" --console=plain'
cmd /c 'X: >nul && cd X:\runtime && .\gradlew.bat :lwjgl3:captureMenu "-PcaptureArgs=X:\generated|l1x|transition-trainer" --console=plain'

REM builder 测试（在 pokemon-builder 下跑）
node --test --test-reporter=tap builder/tests/*.test.js
```

**验证要用像素统计，不要读图**（用户明确要求）：本轮的证据
- `l1-battle-flash`（白天=白闪）、`l1-transition0..4`：`SnakeSquares` 逐行覆盖→全黑，相邻帧差异 16%→100%。
- `l1-trainer-flash`/`l1-trainer-transition0..3`：`TwoBallPass`，球从两侧飞过、黑幕合拢→全黑。
- 回归：不带第三段的完整 capture（`l1x`）野生战全流程 + `l1-battle-evil-vs0..3` + `l1-battle-entry-vs` 照常 OK。

---

## 3. 上一轮（战斗界面 7 条 + 结算）的关键结论（别重做/别踩坑）

- 开场/送球照**本工程实际场景**（`Scene_Initialize:15` / `Scene_Animations:5,85` / `Follower_Main:727` 的我方弧线投球），不是 `PokeBattle_SceneAnimations` 默认。
- 我方底座锚点：`Scene_Initialize:221-229`，`oy = foe ? height/2 : 0`。
- 7 条差异：①性别符号上色（`PokeBattle_SceneElements:16-21/223-229`）②对方性别按 `personalID+genderRate` 派生 ③我方 HP 显示 `x/y`（`icon_numbers`）④Mega 槽居中（用户指定，插件是 210）⑤玩家回合数据盒/宝可梦 ±2 反向跃动 ⑥训练师开场提示语合成一个两行窗口（`MESSAGE_BREAK`）⑦结算 + exp 槽动画/音效。
- 结算照 `Battle_ExpAndMoveLearning:99-308 pbGainExpOne` 全量公式；exp 段时长按插件成正比但加 `MIN_EXP_SEGMENT_SECONDS = 0.5s` 下限（插件无最小值，用户要求）；SE `Pkmn exp gain` / `Pkmn exp full`（本工程 `Audio/SE` 只有这两个）。
- 训练师胜利收尾 `Stage.TRAINER_END`（`Battle_StartAndEnd:444-473`）：你打败了 → `TrainerAppearAnimation` → LoseText → 奖金 → 0.5s 淡出。
- 经验句与进度条**同拍**（brief 消息，`PokeBattle_Scene:130-135`）。

---

## 4. 下一步（二选一，先跟用户确认）

**(A) 战斗侧继续（推荐先做）**：战斗 BGM / 胜利 ME
1. 先让生成器导出 `PBS/metadata.txt` 的 `[000]` 全局段（`WildBattleBGM`/`TrainerBattleBGM`/`WildVictoryME`/`TrainerVictoryME`），落进 `generated/metadata/`。
2. 按 `pbBattleAnimation` 里 `$game_system.bgm_pause` + `pbBGMPlay` 接线；`pbWildBattleSuccess`/`pbTrainerBattleSuccess` 的胜利 ME 接到 `Stage.EXP_GAIN`/`TRAINER_END` 结束处。

**(B) 回脚本块（43 块）**，按投入产出排序：
1. `pbCrystalWarp` 12（传送菜单 + `pbShadowWarp` 16 帧黑幕）—— 一个系统吃掉 12 块。
2. `pbMessage_ex` 4 + `pbGet/pbSet/pbMessage` 楼层选择 4 —— 需给 IR 加"带选项消息的返回值"。
3. `isConst?` Deoxys 形态循环 4。
4. 单块插件 API：`pbMrHyper`/`changeBalls`/`pbChangeShinyByNPC`/`teachEggMoves`/`resurrection2`/`battleOverlordflos`/`battleIronJugulis`/`pbGiveAllMemories`/`pbTalkToFollower`/`pbChoosePokemon`/`pbPokemonMart($battle_item)`。

战斗侧其它挂账：叫声 `pbPlayCry`、闪光/超闪光、换人收回 `BattlerRecallAnimation`、多人对战（引擎仍是 singles）、`vsBar<Type>_<outfit>` 的 outfit 变体、名字缺字 `???`。

---

## 5. 已知差异 / 未做（诚实清单）

- 生成数据只带 metadata `Outdoor`，**没有"洞窟/水上"标记**，所以入场 `location` 只能取 0/1（洞窟=2、水上=3 判不出）。
- `Transitions` 里战斗入场命不到的其他类（`BreakingGlass`/`Splash`/`Mosaic`/`Scroll*`/`ZoomIn`/`RandomStripe`/`ShrinkingPieces` 等）未转译。
- 战斗 BGM/胜利 ME 未做（生成器未导 metadata `[000]`）。
- 多人对战仍只是记录 `setBattleRule("double")`；跟随宝可梦、`pbCommonEvent` API、天气/场地/招式效果未做。
