# B8 回合事件有序化 —— 读书笔记（Ruby 侧，未开工写 Java）

> 状态：**已实施（见 stage3-wild-battle-audit.md §20.12），待用户验收。**
> 本会话无法读取（见 §0），因此"待改 Java 位置"一栏只能按总交接 §5/§7 的描述列出，开工前必须重新核对。
> Ruby 均用 `readScriptsSection(...,{withSources:true})` 现场导出，行号 = 段内行号。

## 0. 本会话的环境阻塞（已上报用户）

1. 本会话**没有用户电脑上的 shell**（只有文件桥），无法跑 `gradlew` / 探针 / `pixel-diff.py`。
2. 文件桥只能访问"连接文件夹下 ≤7 层"的文件；`runtime\core\src\main\java\pokemon\runtime\battle\*.java` 在第 9 层，
   **读不到也写不回**。
3. 云端工作区访问不到 Maven Central，无法在云端编译/跑测试。

## 1. 一次命中的插件顺序（`Battler_UseMove:627-809` `pbProcessMoveHit`）

| 顺序 | 插件行 | 内容 | 备注 |
|---|---|---|---|
| 0 | `Battler_UseMove:305` → `Move_Usage:23-25` | `pbDisplayBrief("{1}使用{2}！")` | **brief**：文字显示完即返回、留在屏幕上（`PokeBattle_Scene:130-134`） |
| 1 | `Battler_UseMove:640-669` | 命中判定；全部未命中 → `:654` `pbMissMessage` 后 `return false` | |
| 2 | `Battler_UseMove:673-686` | 算伤害（`pbCalcDamage`/`pbReduceDamage`） | |
| 3 | `Battler_UseMove:688` | `move.pbShowAnimation` 招式动画 | **无法转译**：依赖 `PokeBattle_AnimationPlayer`（878 行）+ `Graphics/Animations`，未做（总交接 §7 已登记） |
| 4 | `Battler_UseMove:698-709` | 多目标时的部分未命中文案 | |
| 5 | `Battler_UseMove:714-717` → `Move_Usage:253-259` | 扣 HP（只改数值） | |
| 6 | `Battler_UseMove:719` → `Move_Usage:264-283` | `pbAnimateHitAndHPLost`：**先己方后对方**（`:267` `for side in 0...2`），每侧一次 `pbHitAndHPLossAnimation`，同侧多个目标**同时**闪 | `oldHP = b.hp + hpLost`（`:271`）；`effectiveness` 按 `:273-276` |
| 6a | `Scene_Animations:239-268` | `@briefMessage=false`（`:240`）→「使用」那行**在闪烁期间留在屏幕上**，下一条消息直接替换它，不再额外停 1 秒 | |
| 6b | `Move_Usage:284-291` | 我方被打到 ≤1/4 HP 时 `pbBGMPlay("Battle low HP")` | **资源缺失**：`Audio/BGM/` 下没有 `Battle low HP.*`（已逐个核对目录） |
| 7 | `Battler_UseMove:722-723` | 自爆类 `pbSelfKO`、使用者倒下 | |
| 8 | `Battler_UseMove:724-731` → `Move_Usage:322-345` | `pbHitEffectivenessMessages`：替身承受 → 会心 → 效果（仅非连击招式，`:338`）→ 替身消失 | |
| 9 | `Battler_UseMove:734-750` | 伤害后效果/特性道具/挺住文案/树果回复 | 依赖特性/道具子系统，不在本批 |
| 10 | `Battler_UseMove:753` | `pbPriority(true)` 顺序对所有倒下者 `pbFaint` | |
| 10a | `Battler_ChangeSelf:61-99`（+ `:104-127` paldea alias） | `:71` brief「{1}倒下了！」→ `:73` `pbFaintBattler`（`Scene_Animations:299-312`，`@briefMessage=false`） | |
| 11 | `Battler_UseMove:757-807` | 主效果、追加效果、畏缩、减伤树果；`:762/:806-807` 再次倒下检查 | |

## 2. 多段招式（`Battler_UseMove:455-506`）

- `:458-482` 每一段**单独**调用 `pbProcessMoveHit` → 每段各自一次步骤 6 的闪烁+血条（`oldHP` 用该段起始 HP）。
- `:474-481` 中断条件：使用者倒下 / 睡眠或冰冻 / 目标全倒（`17C` 龙箭例外）。
- `:495-501` 循环结束后才对每个目标播效果文案（`Move_Usage:297-320`）；`:502-506`「击中了1次！」/「击中了{1}次！」。

## 3. 消息的两种时序（必须在事件流里区分）

| 种类 | 插件 | 行为 |
|---|---|---|
| 普通 `pbDisplay` | `PokeBattle_Scene:115-154` | 显示完后停 `MESSAGE_PAUSE_TIME=40` 帧（`:13`，1 秒）自动关闭，或按 B/C 立即关闭 |
| 简短 `pbDisplayBrief` | 同上 `brief=true`，`:130-134` | 显示完立即返回，文字留屏；下一条消息前 `pbWaitMessage`（`:101-111`）再停 40 帧，**除非**中间的动画把 `@briefMessage` 置 false |
| 分页 `pbDisplayPaused` | `:161-196` | 3 秒或按键（本批不涉及） |

→ 事件流至少需要：`MESSAGE{text, brief}`、`HIT{side 批次, [idx, oldHp, effectiveness]...}`、`FAINT{idx}`。

## 4. 待改 Java 位置（按总交接描述，开工前需重新读源码核对）

- `Battle.java`：`roundMessages` / `hitEvents` / `faintEvents` → 一条有序 `RoundEvent` 列表；`execute` 里按 §1 顺序追加；
  多段招式每段一个 HIT。
- `InteractiveBattlePort.java`：`Session.log` 现在把消息用 `MESSAGE_BREAK` 拼成一串交给画面 → 改成转交事件流。
- `BattleScreen.java`：现在的流水线「全部消息 → `Stage.HIT` → `Stage.FAINT` → 经验」→ 改成按事件逐个播；
  brief 消息在 HIT/FAINT 期间留屏。

## 5. 需要用户裁决

1. **`Battle low HP` BGM 不存在**（`Move_Usage:288`）：照以往 SE 的做法"照插件点名请求、manifest 无此项即静音"，还是本批不接这 3 行？
2. **`Battler_UseMove:688` 招式动画**无法转译（动画播放器子系统未做）：本批是否允许按"该行未转、已登记"继续？
3. **环境**（§0）：需要能读写 9 层深的 Java 源码 + 能跑 gradle。
