# Stage 3 战斗侧交接（B1–B8-A 之后）

> **这份文档是给"下一个智能体"的**：读完它 + `docs/stage3-wild-battle-audit.md`（工作单，含逐条清单）
> 就能接手，不需要重新摸索。
> 生成时间：本轮收尾。工程根 `E:\仓库\范例\929`，**所有改动都在工作树里、未提交**。

---

## 0. 30 秒结论

- 用户实测的 **8 个战斗 bug 全部修完**（§3 对照表）；两名队友又查漏补了 6 类（§4）。
- 测试基线：**core 563 tests / 0 failed / 0 errors**；builder 295 全绿（未动）。
- 探针 7 个模式（`wild/trainer/switch/trainer2/faint/fight-status/fight-0pp`）全部 `MENU CAPTURE OK`，
  且经"接线前 vs 接线后"受控像素回归：**非战斗帧零违规**。
- **6 件事需要用户拍板**（§6），下一批建议做 **B8 回合事件有序化**（§7）。

---

## 1. 铁律（用户反复强调，违反会被骂；原文照抄）

1. **一切以本工程插件为准**。源码在 `Data/Scripts.rxdata`，用 `readScriptsSection(projectPath,{withSources:true})`
   现场导出；段名精确匹配（**含尾空格**）。每条实现都要能指到插件行号；文案照抄 `_INTL`。
2. **【最高优先级】必须逐行转译 Ruby 原文**：
   - 一行行对着插件 Ruby 写，不是"看着像"；一次转不完就分批次，时间充足，不许赶。
   - 禁止一切自己造/自己编：自创控制流、自创动画、自创数值、自创文案、自创"等价近似"、自创"可接受简化"。
   - 每处改动都要在该行注释里写明对应 `段名:行号`；**写不出行号就不许写**。
   - 无法转译（缺素材/缺数据/**插件本身有问题**/需先做别的子系统）→ **立刻停止该批次并报告用户**，
     不许绕过、不许造替代品、不许默默降级。
   - 插件引用的资源（图/音/数据）必须先确认工程里真实存在再写代码；找不到就立刻停止报告，
     禁止自己造一个资源，禁止留空/注释掉当没看见。
   - 交付按批次报告：本批 `段名:行号区间` + 未转的行与原因 + 定点截图证据。
3. **一次只做一个系统（一批）**，用户验收后再下一个。
4. 老虎机（`pbSlotMachine`）、挖矿（`pbMiningGame`）已决定跳过。
5. 验证优先用**像素统计/测试**，不要一次读很多图。

---

## 2. 环境、命令与工具（照抄即可）

```powershell
# 盘符映射（丢了重建）
subst X: "E:\仓库\范例\929\pokemon-builder"

# 编译 + 全量测试（**验收以 --rerun-tasks 为准**，队友可能正在改文件造成瞬时红）
cmd /c 'X: >nul && cd X:\runtime && .\gradlew.bat :core:test :lwjgl3:compileJava --console=plain'
cmd /c 'X: >nul && cd X:\runtime && .\gradlew.bat :core:test --rerun-tasks --console=plain'

# 截图探针（7 个模式；输出目录会写 PNG，已 gitignore）
#   模式：省略(野生) | trainer | switch | trainer2 | faint | fight-status[|STATUS] | fight-0pp | damage | transition | transition-trainer
cmd /c 'X: >nul && cd X:\runtime && .\gradlew.bat :lwjgl3:captureMenu "-PcaptureArgs=X:\generated|X:\runtime\lwjgl3\<out>|<模式>" --console=plain'
# 昼夜钉死（只影响 isDay?/isNight? 与色调）
$env:JAVA_TOOL_OPTIONS='-Dpokemon.daynight.hour=19'
# 探针精灵表 dump（在每个 shot 旁边打印 @sprites 全表）
$env:JAVA_TOOL_OPTIONS='-Dpokemon.capture.sprites=1'
# 探针性能计数器（drawOrder 调用/重建、worldMs；每 60 帧一条 + 结束 TOTAL）
$env:POKEMON_CAPTURE_PERF='1'      # 注意：-Dpokemon.capture.perf=1 **传不到** JavaExec，见 §8
# 像素统计/对比（qwen38 新增，正式工具；递归、退出码 0/1/2）
python pokemon-builder/runtime/lwjgl3/pixel-diff.py stats "X:\runtime\lwjgl3\<dir>" "l1-*.png"
python pokemon-builder/runtime/lwjgl3/pixel-diff.py diff  "X:\runtime\lwjgl3\<a>" "X:\runtime\lwjgl3\<b>"

# 插件 Ruby 分段导出（临时脚本，用完删；**本轮已删，需要时重建**）
#   工程根 __sections.mjs：node __sections.mjs "段名"        （每行左侧数字 = Ruby 行号）
#   工程根 __find.mjs   ：node __find.mjs "正则"            （全段搜索，输出 段名:行号: 内容）
#   两个脚本本轮保留在工程根，未提交。

# 其他
node pokemon-builder\builder\src\cli.js build-data "E:\仓库\范例\929"       # 重建数据（本轮未动）
cd pokemon-builder; node --test --test-reporter=tap builder/tests/*.test.js  # builder 测试
```

**Python 解释器**（Pillow 12.3.0 可用）：
`C:\Users\Administrator\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe`

---

## 3. 用户实测的 8 个 bug：全部修完（附插件依据）

| # | 现象 | 插件依据 | 落地位置 |
|---|---|---|---|
| 1 | 没有「去吧！」 | `Battle_StartAndEnd:194-275` | `BattleSendOut.plan` + `BattleScreen.stepOpening` |
| 2 | 玩家立绘不显示 | `Scene_Initialize:235-251`（调用点 `:150`） | `BattleScreen.pbCreateTrainerBackSprite` |
| 3 | 换人只有一句 | `Battle_Action_Switching:264-305` | `Session.recallMessage` / `replaceMessage` |
| 4 | **一回合能无限换人** | `Battle_Phase_Command:122-128/207` | `Battle.registerSwitch` + `BattleScreen.finishPartyScreen`（换人登记为本回合行动） |
| 5 | 每回合重放派出动画 | `:194-275` 直线流程 | `BattleSendOut.plan` 步骤表（替换旧的 `sendOutPending` 布尔机制） |
| 6 | 倒下没有动画 | `Scene_Animations:299-312` + `PokeBattle_SceneAnimations:648-684` | `BattleScreen.Stage.FAINT` + `BattleAnimations.BattlerFaintAnimation` |
| 7 | 经验窗没有确认键 | `PItem_Items:547-562` + `Scene_Animations:286-294` | `ExpPhase.LEVEL_GAIN_WINDOW/LEVEL_TOTAL_WINDOW` + `drawLevelUpWindow`（198×224、行距 32） |
| 8 | 结束没有黑屏+淡入 | `PokeBattle_Scene:296-303` + `MessageConfig:602-619` + `PField_Visuals:121-131` | `Stage.END_BATTLE` + `MapScreen.renderBattleReturnFade` |

---

## 4. 本轮新增/重写的实现（按批次，行号即插件依据）

### B1 战斗入场与派出（上一轮 + 本轮收尾）
- `PictureEx`（`PictureEx:1-520`，含 `setPictureSprite:454-519`）、`BattleAnimation`（`PokeBattle_Animation:1-59` +
  `PokeBattle_BallAnimationMixin:63-264`）、`BattleAnimations`（`BattleIntroAnimation:4-58`、`LineupAppearAnimation:88-182`、
  `DataBoxAppear/Disappear:189-223`、`PlayerFade/TrainerFade:306-403`、`PokeballTrainerSendOut:486-551`、
  本工程覆盖版 `PokeballPlayerSendOut`=`Follower_Main:727-813`、`BattlerRecallAnimation:558-603`、
  `BattlerDamageAnimation:610-641`、`BattlerFaintAnimation:648-684`）
- 关键引擎事实（**新 agent 必读**）：`ensureDelayAndDuration` 把**延时和时长都 ×40/20（=×2）**，
  `totalDuration` 再 ×20/40 截断；`PictureEx.origin` 字段**要到第一次 `update()` 才写入**。

### B2 换人子系统（§20.1）
`pbCanSwitchLax?:9-34`、`pbCanSwitch?:41-113`、`pbCanChooseNonActive?:115-120`、`pbRegisterSwitch:122-128`、
`pbPartyScreen:136-151`、`pbSwitchInBetween:155-158`、`pbGetReplacementPokemonIndex:241-253`、
`pbEORSwitch:165-239`、`pbRecallAndReplace:256-262`、`pbMessageOnRecall:264-281`、`pbMessagesOnReplace:284-305`、
`pbReplace:309-320`、`pbSendOut:324-332`、`pbAttackPhaseSwitch:50-71`、`AI_Switch:179-227`（对手选谁上场）、
`pbJudge:587-594`、`PokeBattle_Battle:353-355` `pbAllFainted?`、`PokeBattle_Scene:202-237` `pbShowCommands`、
`Battle_Action_Running:36-157` `pbRun`。
田地槽位改成 `Battle.playerField/foeField`（= `@party1order/@party2order`），`Battler.resetForSwitchIn` = `pbInitEffects(false)` 子集。

### B3 倒下演出（§20.7）+ B4 升级窗口（§20.2）+ B5 结束黑屏（§20.3）
见工作单同名小节；同时修掉一个**渲染层隐藏 bug**：`BattleScreen.renderSprites` 里
`batch.setColor(1,1,1,精灵透明度)` 会泄漏给下一个自绘的数据盒 → 数据盒显式 `setColor(WHITE)`。

### B6 战斗界面（kimi27，§20.8/§20.9）
`PokeBattle_SceneElements:15/218-275`（状态图标 `icon_statuses` 第 `(s-1)` 行、名字 >116px 左移、
`×BOSS_HP_RANK`、等级 >200 的 `???`、shiny/mega/primal/`icon_own`）、
`PokeBattle_SceneMenus:213-427`（固定 4 槽、`refreshButtonNames/refreshSelection/refreshMoveData`、
`PP_COLORS`、`ppFraction=min(ceil(4*pp/maxpp),3)`）、`Scene_Commands:119-131` 槽位导航、`Battle_Phase_Command:84-86`。

### B7 受击演出 + 播报（kimi27 + Lead，§20.9）
`Scene_Animations:211-268`（`pbHPChanged`/`pbDamageAnimation`/`pbHitAndHPLossAnimation`）、
`Move_Usage:264-345`（`pbAnimateHitAndHPLost`、`pbHitEffectivenessMessages`、`pbEffectivenessMessage`）、
`Battler_UseMove:290/495-506`、`Battler_ChangeSelf:71`。

### B8-A 野生 AI（kimi27，§20.11）
`AI_Move:6-155` 野生路径（`pbAIRandom` = `PokeBattle_AI:83` `rand(x)`，**0...x 不含 x**）、
接线点 `Battle.pickMove`；**训练家战与 `battleRank>=2` 的野生 BOSS 仍走旧的 `defaultAi` 兜底**。

### Lead 在引擎里补的（§20.10）
命中/闪避公式（`Move_Usage_Calculations:127-142`）、状态无法行动的文案（`Battler_UseMove_SuccessChecks:269-364` +
`Battler_Statuses:443-478`）、能力升降文案（`Battler_StatStages:58-62/224-228` + `PBStats:18-30`）、
`Battler.moveSlot` 复用（性能）。

---

## 5. 文件所有权地图（下一个会话若再开队友，按这个切）

| 区域 | 文件 |
|---|---|
| 引擎/端口（Lead 区） | `battle/Battle.java`(1561) `Battler.java`(330) `InteractiveBattlePort.java`(759) `BattleResult.java` `BattlePort.java` `MapScreen.java`(battle 分支) `app/RuntimeContext.java`（战斗音频钩子） |
| 场景/界面 | `battle/BattleScreen.java`(3042) `BattleAnimations.java`(894) `BattleAnimation.java`(452) `BattleSprite/Sprites/Shader` `PictureEx.java` |
| 新增文件（本轮） | `battle/BattleAi.java`(233)、`battle/BattleSendOut.java`(229)、`battle/PokeBattle_SceneConstants.java`、`audio/SoundLength.java`、`lwjgl3/pixel-diff.py` |
| 测试（18 个战斗测试类） | `core/src/test/.../battle/`：新增 `BattleSwitchingTest`(13) `BattleSendOutTest`(6) `BattleAnimationTest`(13) `PictureExTest`(12) `BattleFaintAnimationTest`(5) `SoundLengthTest`(6) `BattleDamageAnimationTest`(4) `BattleRoundMessagesTest`(6) `BattleAiTest`(9) `BattleSpritesDrawOrderTest`(6) `AudioManagerFadeBgmTest`(4) |
| 文档 | `docs/stage3-wild-battle-audit.md`（**工作单，最全**）、`docs/next-session-intro.md`（开场白）、本文件 |

---

## 6. 需要用户拍板 / 知晓的事项（按铁律上报，均未自行绕过）

1. **插件自身 bug（等裁决）**：`Audio_Utilities:1075-1077` 读 OGG 采样率少跳一个 channels 字节 →
   rate 被放大 ~256 倍。`001Cry.ogg`（mono/44100，末页 granule 41397）真时长 0.9387s 被算成 0.003667s →
   倒下动画里 "Pkmn faint" 的 `delay` 从 **21 帧**变成 **2 帧**（叫声刚起、倒下音效立刻响）。
   当前实现取 **A：照抄 Ruby（含 bug）**（理由：铁律 1"以插件为准"，且原版游戏跑的就是这个行为）。
   **切到 B（读 +12 的正确采样率）只需改 1 行 + 换测试期望值。**
2. **两个 SE 本工程不存在**：插件点名的 `Audio/SE/Exp full`（`PokeBattle_SceneElements:358`）与
   `Audio/SE/Pkmn level up`（`Battle_ExpAndMoveLearning:283`）；本工程只有 `Pkmn exp full`。
   按 `Game_System:236` 的存在性判定，**原版这两处本来就是静音** → 运行时照插件点名请求（日志提示 manifest 无此项），
   **不再挪用别的文件**（这正是之前的一处自造，已纠正）。
3. `AI_Move:92-105` **未转**：唯一效果是写 `$nextMove/$nextTarget/$nextQue`，全库 12 处命中**全是写入、无读取**
   （`PokeBattle_AI:46-48/139-141`、`AI_Move:58-60/100-102`）；字面转它要先转约 400 行的
   `pbMoveBaseDamage:171-264`+`pbRoughDamage:266-579`。用户若要"字面 100%"，需要显式下令。
4. **代欧奇希斯形态**（`AI_Move:133-145`）未转：依赖 `PBEffects::DeoxysForm`（`Battler_UseMove:404-408` 会读它改形态）
   + `Scene_Commands:149-172` 的 T 键 UI，两者都没有。
5. **四招满时的遗忘流程**未转：需要概要画面的遗忘模式（`Scene_Commands:482-490` →
   `PokemonSummaryScreen#pbStartForgetScreen`），现在只转译到插件 `Battle_ExpAndMoveLearning:329` 那一行。
6. **训练家 AI ≈5,065 行，其中 4,176 行与特性/道具/PBEffects/天气不可分割**
   （`AI_Move_EffectScores:5-3866` 一段就 3,862 行，`ability×132/item×28/PBEffects×179`）。
   **建议不要先做**：只做骨架会让 `pbGetMoveScore` 退化成"只有基础分"，属明确降级。
   前置 = 特性子系统 + 战斗内道具 + PBEffects 表。
7. Safari（狩猎区）系统未做 → `pbInSafari?` 分支无法转译；SHADOW 属性未建模 → `Scene_Commands:7` 的「呼唤」落不了地；
   搭档队伍治疗 + `$game_player.straighten`（`PField_Battles:642-649/657`）依赖多人对战与行走动画复位。

---

## 7. 已知偏差与下一批建议

### B8「回合事件有序化」（建议下一批，Lead 做）
插件同一次命中的顺序：`使用:290` → 招式动画(`Move_Usage_Calculations:688`) → **扣血 `:716`** →
**受击闪烁+血条 `pbAnimateHitAndHPLost:719`** → **会心/效果/连击播报 `:728`、`Battler_UseMove:495-506`** →
`倒下了！`(`Battler_ChangeSelf:71`)。
运行时现状：`roundMessages` / `hitEvents` / `faintEvents` 三张**并行列表按"轮"聚合**，屏幕流水线是
「本轮全部消息 → 受击动画 → 倒下动画 → 经验/回合尾」→ 因此①播报落在闪烁**之前**、②双方出手的闪烁被**合并成一次**。
**修法**：合并成一条有序 `RoundEvent{kind: MESSAGE|HIT|FAINT, ...}` 事件流，屏幕按序播；
并把多段招式拆成每段一个 HIT（`oldHp` 用该段起始 HP）。
涉及 `Battle.java` + `BattleScreen.java` + `InteractiveBattlePort.java` 三处（**必须串行做，别和别的批次并行**）。

### 其他已记录偏差
- 训练家结束的"对手滑回画面"仍是 8 帧占位：`Scene_Animations:71-77 pbShowOpponent` +
  `PokeBattle_SceneAnimations:272-297 TrainerAppearAnimation` 未转。
- `pbHPChanged:213-217` 的 `pbCommonAnimation("HealthUp/Down")` 依赖公共动画子系统，未转（那一整套
  `PokeBattle_AnimationPlayer`(878 行) + `Graphics/Animations`(702 文件) 都还没做）。
- `updateBars` 是线性 1.0s 补间，插件的 `animateHP`(`PokeBattle_SceneElements:181-185`) 还有 `minInc` 下限。
- `PokeBattle_Scene:296-303 pbEndBattle` 已做，但 `pbEndOfBattle` 里的 `pbShowOpponent`/`pbGainMoney` 细节见工作单。

### 工作单里的 27 条"还没啃"清单
全部在 `docs/stage3-wild-battle-audit.md` 的 §20.8/§20.9（含行号、现象、依赖）。
优先级建议：**B8 有序化 → 天气/场地子系统（`Battle_StartAndEnd:327-352`、`Battle_Phase_EndOfRound:35-147`）→
PBEffects 表 → 异常状态扩展（`Battler_Statuses`）→ 特性子系统（最大一块）**。

---

## 8. 踩过的坑（新 agent 看过能省几小时）

1. **`ensureDelayAndDuration` 双倍**：`PictureEx:99-105` 里 `set*/move*` 的**延时和时长都 ×40/20**。
   `setXY(3,…)` 是 6 帧后生效；`setVisible(7,true).totalDuration()==7`。B1 之前所有"固定帧数"硬编码都错在这。
2. **`PictureEx.origin` 的写入时机**：`forSprite` 只排工序（`PictureEx:832`），字段在第一次 `update()` 才写
   （`PictureEx:655-657`）。测试里"构造后立刻断言 origin"必然拿到 `TOP_LEFT=0`。
3. **batch 颜色会泄漏**：`renderSprites` 里每个精灵 `batch.setColor(1,1,1,opacity/255)`，
   下一个"自绘"的精灵（数据盒）会继承它 → 必须显式 `setColor(WHITE)`。这一条害我查了很久
   （症状：某几帧数据盒只剩名字、面板/血条/数字全没了）。
4. **`__r19/px.py` 与 Pillow 的 `getbbox()` 陷阱**：`ImageChops.difference` 的结果若**带 alpha 通道**，
   `getbbox()` 默认只看 alpha（`alpha_only=True`）→ RGB-only 差异会被误判成"相同"。
   正确写法：`delta.convert("RGB").getbbox()`。本轮的 `pixel-diff.py` 已按此写。
5. **`-D` 传不到 JavaExec**：`gradlew ... "-Dpokemon.capture.perf=1"` 不会传给探针子进程；
   用**环境变量**（`POKEMON_CAPTURE_PERF=1`）。`JAVA_TOOL_OPTIONS=-D...` 也可以（会传给所有 JVM）。
6. **Gradle 并发**：两个会话/队友同时跑 gradle 会抢锁，表现为
   `Unable to delete directory ... test-results/test/binary` 或瞬时编译失败（`RuntimePortForwardingTest` 找不到类）。
   **验收一律用 `--rerun-tasks` 复跑一次**；瞬时红不要当成真红。
7. **`pbThis(true)` 的 `lowerCase` 是死参数**（`PokeBattle_Battler:214-231`：`:216-226` 忽略它、`:228` 两分支同串、
   `:230` 直接 `return name`）→ `pbThis(true) ≡ thisName()`，不需要新 API。
8. **`pbAIRandom(x) = rand(x)`**（`PokeBattle_AI:83`）= **`0...x` 不含 x**；且 Ruby 的 `rand(0)` 返回 Float，
   而 Java 的 `nextInt(0)` 会抛异常 → 空 choices 必须提前返回（`BattleAi` 里已双保险）。
9. **`totalScore` 在二次扫描后不重算**（`AI_Move:116-119` 之后仍用 `:27` 的旧值）——野生路径不可达，
   **训练家批次会踩到**（那时会走 `pbAIRandom(0)` ⇒ 取第一个 choice），别"顺手修正"。
10. **探针的定点采样而不是固定帧数**：`advanceUntilBattle(predicate, maxFrames)` 才是可靠做法
    （战斗动画 40fps、渲染 60fps，固定帧数会飘）。新增探针模式时**只加分支，别改现有模式**（别人在用它做回归）。
11. **探针的非确定性是可接受的**：野生 AI 随机化后 `faint` 模式从"必进倒下分支"变成"约 2/3 概率"，
    已用"探针装置"（主角 HP=1 + 对手只留伤害招式）钉成确定性，并在注释里标明**那是探针装置、不是游戏逻辑**。
12. **测试计数基线**：本轮结束时 core **563 tests / 0 failed**；builder 295。任何批次交付必须全绿并把数字报出来。

---

## 9. 本轮团队分工（用户要求引入队友；**名字只是角色名**）

| 成员 | 角色 | 任务 | 结果 |
|---|---|---|---|
| Lead | 引擎/接线/验收/文档 | B2、B4、B5、§5.2、B3 接线、命中公式、状态/能力文案、渲染 bug | 全部落地并自验 |
| `qwen38` | 转译 → 优化 + 验证 | task-1（`BattlerFaintAnimation`+`SoundLength`/`pbCryFrameLength`+`fadeBgm`）、task-4（`drawOrder` 缓存 + 像素回归工具链 + 计数器）、AI 接线的零行为回归、`faint` 探针确定性修复 | 已验收 |
| `kimi27` | 转译（界面/演出/AI） | task-2（§8.1/§8.2）、task-3（B7 受击演出 + 播报 + 27 条查漏清单）、task-5（野生 AI + 训练家 AI 工作量清单） | 已验收 |

⚠️ **`spawn_teammate` 没有 model 参数**，队友由 harness 统一分配 `deepseek-v4.1-flash`；
用 `workflow` 的 `model:"qwen3.8"` 覆盖时子 agent 直接失败（模型未接入）。**"qwen3.8 / kimi2.7-code"只是角色名。**

**协作纪律（本轮验证有效，建议沿用）**：
① 开工先做**读书笔记**（导出 Ruby、列精确行号、核对资源、列出待改 Java 位置），发给 Lead 等 GO；
② 写权限按文件切分，禁改文件只能发"接口请求"；
③ 队友自述**不作为验收依据**——Lead 用独立方法复核主证据（模板比对/像素统计/分帧状态机日志/自己重跑测试）；
④ 每批必须报：转了哪些 `段名:行号` / 未转的行与原因 / 证据 / 不确定项。

---

## 10. 证据位置（都在 `X:\runtime\lwjgl3\` 下，可再生）

| 目录 | 内容 |
|---|---|
| `b1-wild` `b1-trainer` `b1-switch` | B1 三场景定点截图 |
| `b2-t2` `b2-faint2` `b2-switch2` | B2 换人/对手换人/玩家倒下换人 |
| `b4-wild` `lead-wild` `lead-fix` | B4 升级窗口 + B5 结束淡出 + 渲染 bug 修复前后 |
| `lead-verify` `lead-0pp` | §8.1 状态图标 / §8.2 0PP（Lead 独立复核用） |
| `repo-b6-evidence*` `repo-b7-evidence` | kimi27 的 §8.1/§8.2 与受击闪烁证据（原在仓库里，已移出） |
| `ai-base-a/b/c` `ai-after-a/b` `ai-faint-probe1..3` | AI 接线的受控像素回归 + 探针修复 |
| `perf-base` `perf-after` `perf-verify1..4` | 优化的像素回归 |
| `accept-*` | Lead 最终验收 7 模式 |

⚠️ `runtime/lwjgl3/*/` 已整体 gitignore（源码除外），仓库里不会因为截图变脏。
