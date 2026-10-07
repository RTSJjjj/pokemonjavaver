# Bug 报告：对手换人后双方数据盒消失（可直接粘贴给 Claude）

## 1. 症状
训练家对战里**打倒对手一只宝可梦、对手换上新宝可梦之后，双方的数据盒都不见了**（宝可梦立绘、背景、指令菜单、消息窗口都正常）。

## 2. 复现（确定性，不需要手动打）
```powershell
cmd /c "X: >nul && cd X:\runtime && .\gradlew.bat :lwjgl3:captureMenu ""-PcaptureArgs=X:\generated|X:\runtime\lwjgl3\m0-box|trainer2"" --console=plain"
```
`trainer2` 模式就是「训练家对战 + 打倒一只 → 对手换人」。探针自带的 `[battle-state]` 诊断（`BattleScreen.java:2732` + `boxSignature`）会逐帧打印数据盒状态。

## 3. 关键日志（原样摘录，已删重复行）
```
stage=BATTLE page=3 window=1 msg=shown queue=0 choice=false switch=SEND   sendOut=running  box0=v255@-609  box1=h255@776F
stage=BATTLE page=3 window=1 msg=shown queue=0 choice=false switch=SEND   sendOut=running  box0=v255@-588  box1=h255@776F
   … 每帧 +21 …（box0.x: -609 → -588 → … → -336，共 14 帧，然后停住）
stage=BATTLE page=3 window=1 msg=shown queue=0 choice=false switch=SEND   sendOut=running  box0=v255@-336  box1=h255@776F
stage=BATTLE page=3 window=1 msg=-     queue=0 choice=false switch=RECALL  sendOut=-        box0=v255@-336  box1=h255@776F
stage=BATTLE page=3 window=1 msg=-     queue=0 choice=false switch=LINEUP  sendOut=-        box0=v255@-336  box1=h255@776F
stage=BATTLE page=3 window=1 msg=-     queue=0 choice=false switch=MESSAGE sendOut=-        box0=v255@-336  box1=h255@776F
stage=BATTLE page=3 window=1 msg=shown queue=0 choice=false switch=REPLACE sendOut=-        box0=v255@-336  box1=h255@776F
stage=BATTLE page=3 window=1 msg=shown queue=0 choice=false switch=SEND   sendOut=running  box0=v255@-336  box1=v255@1112
   … 每帧 −21 …（box1.x: 1112 → 1091 → … → 776，共 16 帧，停住）
stage=BATTLE page=0 window=2 msg=-     queue=0 choice=false switch=NONE   sendOut=-        box0=v255@-336  box1=v255@776
```

## 4. 诊断字段的含义
`boxSignature(BattleSprite box)`（`BattleScreen.java:2735` 附近）打印
`(visible?"v":"h") + Math.round(box.opacity) + "@" + Math.round(box.x) + (fainted?"F":"")`
→ 即 **可见性 / opacity / 数据盒的 x 坐标 / 该侧是否已倒下**。

## 5. 分析（已确立的事实）
1. **`-336` 正好等于 `-PictureEx.Graphics.WIDTH / 2`（672/2）**，也就是 `DataBoxAppearAnimation` 的**起始位移**：
   ```java
   // BattleAnimations.DataBoxAppearAnimation（PokeBattle_SceneAnimations:195-202）
   PictureEx box = addSprite(sprite);                            // :197
   box.setVisible(0, true);                                      // :198
   float dir = ((idxBox % 2) == 0) ? -1f : 1f;                   // :199
   box.setDelta(0, dir * PictureEx.Graphics.WIDTH / 2f, 0);      // :200  → box0: -336
   box.moveDelta(0, 8, -dir * PictureEx.Graphics.WIDTH / 2f, 0); // :201  → 8 帧内 delta 回到 0
   ```
   **box0 的 `moveDelta`（把 delta 拉回 0 的那道工序）没有生效/没有跑完** —— 它的 delta 停在 `-336`，盒子被画在屏幕外 336 px 处。
2. **box1 的同一道工序是跑完的**：它的 x 从 `1112` 一路走到 `776` 后停住，`1112 = 776 + 336`，即 delta 已归 0、停在基准位置。**只有 box0（玩家侧）没归位。**
3. **不是「动画队列被清空」**：全文件 grep `animations.clear/removeAll/removeIndex/removeValue` **0 命中**。
4. **步长是 21/帧，而不是 42/帧**：`moveDelta(0, 8, 336, 0)` 名义上 8 帧、42/帧；实测 **21/帧**，即**实际跑了 16 帧** —— 与本工程 `PictureEx` 已知的「延时与时长都 ×2」行为一致（`ensureDelayAndDuration` 的 ×40/20）。所以工序本身是被正确排队的，问题是 box0 这一道**中途丢失/被覆盖**。
5. **时序**：box0 的异常发生在第一次 `switch=SEND sendOut=running` 期间；随后 `RECALL → LINEUP → MESSAGE → REPLACE` 一路带着 `box0@-336` 没恢复；`REPLACE` 时 `refreshDataBoxes()` 重建数据盒（`BattleScreen.java:1294`）也没把它拉回来。

## 6. 建议排查点（`BattleScreen.java`）
- `case REPLACE`（**:1291-1301**）：`session.battle.replace(...)` → **`refreshDataBoxes()`** → `switchStep = SEND` → `new BattleAnimations.SendOutSequence(...)`。
- `refreshDataBoxes()` 里创建/重建数据盒精灵并加 `DataBoxAppearAnimation` 的地方（**`:898`**：`animations.add(new BattleAnimations.DataBoxAppearAnimation(this, idxBattler));`）。
- `case SEND`（**:1302-1325**）等 `sendOut.done()` 的分支。
- 怀疑点：`refreshDataBoxes()` 在**旧的 appear 工序还挂在旧精灵上**时重建了数据盒（或对同一个 `dataBox_0` 精灵重新 `setDelta`），导致 box0 的 `moveDelta` 被丢弃；也可能 box0 的 appear 动画被加了两次而第二次的 `setDelta` 覆盖了第一次正在进行的 `moveDelta`。

## 7. 修好的判据
同样命令跑 `trainer2`，等换人流程走到 `switch=NONE` 之后，诊断应为
```
box0=v255@<基准>   box1=v255@776
```
即 **box0 的 x 不再是 -336**（delta 已归 0）。建议同时在 `:core:test` 加一条针对「对手换人后数据盒 delta 归位」的断言。

## 8. 与 M0（本轮 BattleHandlers/招式效果地基）无关的证据
| 检查 | 结果 |
|---|---|
| `BattleScreen.java`（数据盒绘制、`DataBoxAppearAnimation` 调用点、`boxSignature`、`refreshDataBoxes`） | 相对本轮接线前快照 **逐字节相同**（`git diff --no-index` 无输出） |
| `Battler.java` 前 388 行 | 与快照**逐字节相同**（唯一差异是收尾 `}` 移到了文件末尾，纯追加的必然结果） |
| `Battle.java` 前 1898 行 | 只有 6 行**插入**（`refreshFieldIndices()` 里写 `battler.battle/field/pbs` 三个新字段），无任何已有行被改 |
| `BattleAnimations.java` | 本轮未改 |
