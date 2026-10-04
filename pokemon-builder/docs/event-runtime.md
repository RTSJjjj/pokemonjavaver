# 事件运行时（Event Runtime）

> 对应任务书 §11–§20、§26–§30。实现集中在 `runtime/core/.../event/` 与 `map/`；
> 本文件说明事件命令覆盖、触发/页面语义、移动路线、消息窗口与端口机制。

## 1. 命令覆盖（`EventInterpreter`）

| 码 | 命令 | 说明 |
|---|---|---|
| 0 / 104 / 108 / 401 / 404 / 412 | 空行 / 文本选项 / 注释 / 文本续行 / 分支收尾 | 消费/忽略 |
| 101 + 401 | 显示文本 | 2 行/页（R6.31）、颜色/换行/居中标签、名字框、`\w[skin]`；等待确认 |
| 102 + 402/403/404 | 显示选项 | `choice` 皮肤 + 光标；问题文本与选项同屏；只执行选中分支 |
| 106 | 等待 | 按帧计（40 fps） |
| 111 + 411/412 | 条件分支 | 开关/变量/自开关/角色方向/按钮/简单表达式；复杂 Ruby 走 IR `CONDITION` |
| 115 | 中止事件处理 | `program.finish()` |
| 116 | 公共事件 | 递归帧（公共事件源由 `GameDatabase.commonEvent(id)` 提供） |
| 121 / 122 / 123 | 开关 / 变量 / 自开关 | 122 参数为 `[start, end, operation, operandType, value]` |
| 201 | 场所移动 | `MapPort.transfer(...)`（含淡出淡入参数） |
| 202 | 设置事件位置 | 同步事件实体与 MapData |
| 203 / 204 / 206 | 滚动地图 / 更改地图设置 / 雾不透明度 | `MapEnvironment` + `CameraScroll`（R6.22） |
| 207 | 显示动画 | 角色动画（position 0/2 以头顶为锚点，R6.28）；不阻塞 |
| 208 | 更改透明状态 | 改**玩家**透明（到达门隐身依赖它） |
| 209 + 509 / 210 | 移动路线 / 等待移动结束 | `MoveRoutePlayer`；码表含 5–8 斜向、10/11 朝/离玩家、13 倒退、16–26 朝向类（R6.32） |
| 221 / 222 / 223 / 224 / 225 | 淡出 / 淡入 / 更改色调 / 闪烁 / 震动 | `ScreenEffects`（223 使用 Builder 导出的 Tone 数值与过渡帧数） |
| 231 / 232 / 235 | 显示 / 移动 / 消除图片 | `PictureService` |
| 241 / 242 / 245 / 249 / 250 | BGM / 停止 BGM / BGS / ME / SE | `AudioManager`；BGM 过场为 1 秒交叉淡出（R6.24） |
| 355 / 655 / 509 | 脚本 | Builder IR（`ScriptIr`）；355+655 合并为一块，只执行一次（R15） |
| 234 | 更改图片色调 | **未实现**：只告警一次，事件继续 |

## 2. 触发与页面

- 触发类型：`0` 确认、`1` 玩家接触、`2` 事件接触、`3` autorun、`4` 并行。
- **视线触发（L6b）**：名为 `Trainer(N)` / `Counter(N)` 的 **trigger 2** 事件在玩家
  走完一步时检查（原版 `Game_Player` 的 `pbCheckEventTriggerFromDistance([2])`）：
  `Trainer(N)` 要求玩家在事件**面朝轴**上 N 格内且中间每格可通行
  （`pbEventCanReachPlayer?`，用 `Collision.canStepFrom` 沿轴扫描）；
  `Counter(N)` 只要求玩家在同一轴的 N 格内（`pbEventFacesPlayer?`，无通行检查）。
  命中即按事件所在格启动其 trigger 2 页（每次一个，与共享解释器一致）。
  本工程 184 张图、326 个 Trainer + 118 个 Counter。
- `EventPages.resolve(state, mapId, event)` 按 开关1/开关2/变量/自开关 条件选页；
  换页后 `EventCharacters.refreshGraphics()` 重建立绘（空页/瓦片页不再复用旧图，R6.21）。
- **到达门**：门事件落地后由 `tsOff?("A")` 到达页接管（显示玩家、迈步、恢复透明）。
  R6.33 修复：多门地图上所有到达页同时为真时，autorun 驱动按事件顺序逐帧排队
  （`autorunCooldown = 1/40 s`），`DoorShowHold` 只有在**没有待执行 autorun 页**时
  才释放隐身。
- **并行事件**：各自持有独立解释器，互不抢占消息窗口（R6.2x 修复）。

## 3. 移动与碰撞

- `MapCharacter`：格子 → 像素插值、方向/图案、跳跃（14）、换行走图（41）、斜向
  双轴移动（5–8）、倒退保持朝向（13）、朝向类命令尊重 `directionFix`（16–26）。
- `Collision.canStep*`：地形通行（瓦片 passages 四向位）、事件阻挡（需活动页且有
  立绘的实体）、斜向两条 L 形路径（R6.32）。
- 自主移动：固定/随机/接近/自定义（页属性），移动时/停止时动画、速度/频度。
- 玩家越界行走：`MapLinks` 判断邻居可走并原地换图（无淡出；相机仅在
  `SnapEdges` 时夹取），保留玩家坐标的连续性（R6.23）。

## 4. 消息 / 选项 / 皮肤

- `MessageWindow`（R6.31）：Essentials `SpriteWindow` 九宫格（`speech bw 1`、
  `choice 1`、名字框）、`selarrow` 光标、暂停光标、22px 字体 + 2px 阴影；
  文字颜色 `\c[1..12]`、`<c3=...>`、`<c2=...>`、`\b`、`\r`；`\l[n]`、`\n`、
  `<ac>` 居中；54 半宽单位换行、每页 2 行。
- 选项：102 紧跟 101 时末页不等确认——问题与选项同屏；`402` 按索引执行分支。

## 5. 端口机制（防漏接）

解释器不直接触碰地图屏：所有屏上操作走 `MapPort`，由 `RuntimeContext` 转发到
`MapScreen`。新增端口必须同步：
`RuntimePortForwardingTest` 会在缺少转发时失败（R6.14 的教训）。当前端口涵盖
transfer/setEventLocation/eraseEvent/透明/色调/闪光/震动/滚动/雾/动画/气泡/
推石头/浮板/洞窟动画/移动路线/音频等。

## 6. 存档与状态

`GameState` 保存玩家位置与朝向、开关、变量、自开关、物品、任务、强度门控等；
`SaveManager` 写 JSON（`saveVersion=1`，`saves/` 经 `StoragePort`）。Desktop 入口
F5 快速存档 / F9 读取；地图/事件状态在加载后由 `MapScreen` 重建。

## 7. 无头可测性（§61）

事件解释器、`GameState`、移动/碰撞、IR 执行都不需要 OpenGL；R15 的
`FixtureGoldenTest` / `FixtureSmokeTest` 在无图形环境下跑完 fixture 的全部事件页，
`ScriptIrExecutionTest`、`WalkingRegressionTest` 等也全部无 GL。带图形的验证
（像素/截图/真窗口）位于 `__r6-staging` 探针与 R12/R14 的打包 exe 实测。
