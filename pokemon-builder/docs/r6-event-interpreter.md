# R6 事件解释器交接（2026-10-03，R6.1）

状态：R6.1（解释器核心 + Message 服务 + 控制/条件/公共事件命令）已通过用户本机 `agentCheck`；R6.2（MessageWindow + 地图交互）源码与测试已补齐，**agent 环境 JUnit 53 项全过**，并已用隐藏窗口真实渲染出对话/说话人/选择项三张截图，待用户本机 `lwjgl3:run` 复核。R6.3–R6.4 见计划书。

## 本次新增（相对 `runtime/core/src/main/java/pokemon/runtime/`）

| 文件 | 行为与原因 |
|---|---|
| `event/InterpreterState.java` | §17 要求的状态枚举。WAIT_INPUT / WAIT_MOVEMENT / WAIT_TRANSFER 已就位，分别留给按钮输入命令、移动路线（R6.3）与地图传送（R6.3）。 |
| `event/EventProgram.java` | 一条命令列表的游标：`skipBlock()` 对应 RMXP `command_skip`（跳过 indent 更深的块），`branchResult()` 保存条件分支结果，`finish()` 对应 115。 |
| `event/MessageService.java` | §19/§20 的消息状态：4 行分页、等待确认、选择项 + 光标 + 取消策略。**不含任何 libGDX UI 类型**，窗口只读它，解释器因此可 headless 测试。 |
| `event/MessageText.java` | 文本管线：`\n` 拆行、`\v[n]` 取变量、`\xn[名]` 取说话人、`\PN` 取玩家名，其余控制码/富文本标签（`\c[]`、`\l[]`、`\w[skin]`、`\G`、`<ac>`、`<c3=...>`）暂时剥离；末行 `\^` 表示不等确认。 |
| `event/EventInterpreter.java` | 跨帧解释器：命令分派、101/401 文本收集、102 选择、111 条件、121/122/123 控制、116 公共事件（嵌套 + 递归拒绝 + 深度上限）、106 Wait、115 退出、音频命令转发 AudioManager；未实现命令记录日志并跳过。 |
| `state/GameState.java`（改） | 新增 `playerName`（`\PN` 用），默认 `训练家`——尚无新游戏/取名流程（R10/R14），先占位。 |

测试：`event/EventInterpreterTest.java`（13 项，headless）。

## 与源工程数据的对应（523 张地图 + 200 公共事件实测统计）

- 条件分支类型分布：script 12（1835）、variable 1（790）、character 6（579）、switch 0（100）、self switch 2（82）、gold 7（34）、button 11（3）。
  R6.1 实现 0/1/2/6/11/12（简单形态）；7（金钱）与 12 的复杂脚本留待 R7（脚本转译）/阶段 3（队伍数据）。
- 文本标记出现次数（消息相关命令内）：`\xn[...]` 说话人约 4000+、`\PN` 4000+、`\n` 993、`\c[]` 89、`\l[]` 98、`\w[...]` 274、`<ac>`/`<c3=...>` 约 430。
  R6.1 全部解析/剥离；**只有配色与皮肤被丢弃**（"行为正确优先"，§20），R6.2 起可选择把 `<c3=...>` 映射成 BitmapFont 颜色。
- 本工程 112/113/413（Loop）为 0 次，故 R6.1 未实现循环；一旦出现会在日志里点名。

## 设计要点（后续阶段别踩坑）

1. **命令面分批**：R6.1 未实现的命令（201/202/203/204/207/208/209/210/223/231/232/235/355/509/655 …）统一"记录 + 跳过"，日志格式 `event command <code> (index N) is not implemented yet; skipped`。R6.2 起逐批把它们从 skip 变成实现，并同步更新本文件与计划书。
2. **脚本命令（355/509/655）**：本工程出现 20400 + 6267 + 2516 次，是最大的未实现面，属 R7 脚本转译；R6 阶段它们只会记录日志。
3. **复杂 Ruby 条件**：`pbCut`、`pbRockSmash`、`$PokemonBag.pbCanStore?` 等一律判 false 并记录（含原文），**不是静默通过**；R7 转译后替换。
4. **消息分页在解释器里**（RMXP 同构）：`MessageService` 只持有"当前这一页"，翻页/关闭由解释器驱动，窗口只渲染。
5. **计时语义**：`Wait` 的第一帧只登记计时器、不消耗 delta（与 RMXP 一致）；测试里要注意这点。
6. **输入测试**：`InputManager.press()` 只在按键由 up→down 时置位 `wasPressed`；模拟"点一下"必须 release，否则第二次确认收不到（测试助手已按此实现）。

## 验证（agent 环境实测）

```text
javac (JDK 21.0.7) main 45 个源文件 -> exit 0
javac (JDK 21.0.7) test  8 个源文件 -> exit 0
JUnit Platform 直跑 8 个测试类      -> started=51 succeeded=51 failed=0
  GameDatabaseTest 5 / TileMapTest 6 / MapRenderingContractTest 3 /
  MovementControllerTest 6 / WalkingRegressionTest 9 / EventPageStateTest 5 /
  GameStateTest 4 / EventInterpreterTest 13
```

（`lwjgl3` 未改动，未重新编译；下一批改了 core 公开 API 时会一并编译。）

## 用户本机验证

```powershell
Set-Location X:\runtime
.\gradlew.bat :core:test :core:auditCoreDependencies
```

R6.1 还没有可见的游戏内变化（消息窗口与事件触发在 R6.2），所以这一批的验收就是"测试全绿 + 依赖审计 OK"。

## 下一步（R6.2）

（已完成，见下一节。）

---

# R6.2 消息窗口与地图交互（2026-10-03）

## 本次修改

| 文件（相对 `runtime/`） | 行为与原因 |
|---|---|
| `core/.../ui/MessageWindow.java`（新增） | 只负责画：底框、说话人名条、4 行文本、选择项窗口（含光标高亮）。字体走 gdx-freetype 加载项目自带 CJK 像素字体 `Fonts/FusionPixelMonoPatched.ttf`，`incremental = true` 按需生成汉字字形。选择模式只画选择窗口（与 RMXP 一致，文本页此时已关闭）。 |
| `core/.../map/MapScreen.java` | 事件运行中冻结玩家（RMXP 语义）；否则正常移动，并在确认键按下时触发**面朝方向**事件页（trigger 0）。消息窗口在地图之上绘制；`dispose` 释放字体/纹理。 |
| `core/.../app/RuntimeContext.java` | 持有 `MessageService` 与 `EventInterpreter`；`CommonEventSource` 接 `GameDatabase.commonEvent(id)`，解释器日志接 `PokemonGame.log`。 |
| `core/.../map/GraphicsLocator.java` | 新增 `font(name)`：在源工程 `Fonts/` 下做大小写不敏感查找（与 Graphics 查找同一套实现）。 |
| `core/.../event/MessageText.java` | 新增按字符数自动换行（默认 21 列，RMXP 按窗口宽度折行）；`EventInterpreter.messageColumns(n)` 可调。 |
| `core/.../event/EventInterpreter.java` | 消息解析带换行宽度；消息文本 `\v[n]` 等行为不变。 |
| `core/build.gradle` | 新增 `implementation com.badlogicgames.gdx:gdx-freetype:${gdxVersion}`。 |
| `lwjgl3/build.gradle` | 新增 `runtimeOnly ...gdx-freetype-platform:${gdxVersion}:natives-desktop`。 |
| `lwjgl3/.../MessageCapture.java`（新增） | 隐藏窗口里跑"真实地图 + 生产 MapRenderer + 真实事件 + MessageWindow"并输出 PNG：`dataRoot mapId eventId out.png [confirms]`，`confirms` 用来翻到后续页（例如选择项）。 |

测试：`EventInterpreterTest` 新增 2 项（超长行折行到 21/4 字、`\v[n]` 取变量），共 15 项。

## 依赖与坑（务必记住）

1. **新依赖需要联网一次**：`gdx-freetype` 与 `gdx-freetype-platform:natives-desktop` 不在本机 Gradle 缓存里，用户本机第一次 `gradlew` 会下载；之后可离线。
2. **incremental 字体不能提前 dispose generator**：libGDX 的增量字形是"边用边生成"，`FreeTypeFontGenerator` 必须活到字体销毁之后。首轮实测直接把 generator 在 `generateFont` 后 dispose，结果 `gdx-freetype64.dll` 触发 `EXCEPTION_ACCESS_VIOLATION`（JVM 崩溃）——已改为持有 generator，`dispose()` 时先 font 后 generator。
3. 字体加载失败不阻塞游戏：`MessageWindow.ready() == false` 时窗口不绘制，日志给出 "message font not found"。

## 验证（agent 环境实测）

```text
javac core main 46 文件 / test 8 文件 exit 0；javac lwjgl3 4 文件 exit 0
JUnit 8 个测试类 -> started=53 succeeded=53 failed=0
MessageCapture（真实 OpenGL，隐藏窗口）：
  map002 ev11             -> 文本页      capture-message-text.png
  map002 ev10（\xn[南晓]） -> 说话人名条  capture-message-speaker.png
  map002 ev11 +1 次确认    -> 选择项 是/否 capture-message-choices.png
```

三张截图都是"地图 + 生产渲染器 + 真实事件命令"跑出来的，文字为项目自带像素字体的中文。

## 边界（R6.3 起继续）

- 只接**动作触发（trigger 0）**：玩家接触(1)/事件接触(2)/自动执行(3)/并行(4) 未实现。
- 101 的立绘（face）参数被忽略；`\c[]`/`<c3=...>` 配色仍被剥离（只有字体颜色统一白色），说话人名为黄色。
- 未实现的命令仍是"记录 + 跳过"（201/202/209/210/223/231/232/235/355/509/655 …）。
- 玩家在事件中冻结，但没有做 RMXP 的"事件朝向玩家/转向玩家"。

## 用户本机验证

```powershell
Set-Location X:\runtime
.\gradlew.bat :core:test :core:auditCoreDependencies
.\gradlew.bat :lwjgl3:run --args="X:/generated 2"
```

游戏内：走到 NPC 旁按确认键（Z/Enter），应出现底部对话框；带说话人的事件会在框上方显示名字条；有选择项的事件会弹出 是/否 列表（上下选择 + 确认）。日志里会打印尚未实现的命令编号。

---

# R6.3 地图类命令（第一批）：Transfer Player + 玩家接触触发 + 672×488（2026-10-03）

## 本次新增/修改

| 文件（相对 `runtime/`） | 行为与原因 |
|---|---|
| `core/.../event/MapPort.java`（新增） | 解释器访问地图的接口（当前只有 `transfer`），解释器因此保持 headless 可测：游戏侧接 RuntimeContext，测试侧记录调用。 |
| `core/.../event/EventInterpreter.java` | 201 Transfer Player：`program.finish()`（RMXP 在传送时结束事件）→ 调 port → 进入 `WAIT_TRANSFER`；没有 port 时记录日志并跳过。 |
| `core/.../app/RuntimeContext.java` | 持有待处理的 `Transfer` 请求与 `takePendingTransfer()`；MapPort 只登记请求，真正的切屏在帧末执行（RMXP 语义）。 |
| `core/.../map/MapScreen.java` | ① 新增"显式出生点 + 朝向"构造器（传送目标）；② 帧末取 pending transfer → 建新 MapScreen → `game.setScreen` → **释放旧 screen**（`Game.setScreen` 不负责 dispose）；③ 新 screen 构造时 `interpreter.stop()`；④ 玩家接触触发（见下）。 |
| `core/.../map/EventTriggers.java`（新增） | RMXP 触发器选择：0 = 动作键、1 = 玩家接触；`pageAt`/`eventIdAt` 判断某格是否有对应触发器且有效页有命令，headless 可测。 |
| `core/.../app/ScreenMetrics.java`（新增） | 逻辑分辨率 **672×488**（用户指定，等同源工程实际分辨率）；MapScreen、相机、桌面窗口、两个截图工具统一引用。 |
| `lwjgl3/.../Lwjgl3Launcher.java` | 窗口 640×480 → **672×488**（R11 再加 FitViewport 缩放）。 |
| `lwjgl3/.../MapRenderCapture.java` / `MessageCapture.java` | 截图尺寸改为 672×488。 |

**玩家接触触发（door 的关键）**：本工程的门（`Next door`/`中心`）都是 `trigger=1` + `doorsNN` 图像。实现为两条路径：

1. 玩家**踩到新格**（一步走完、x/y 变化）→ 触发该格 `trigger=1` 事件；
2. 玩家**朝门走但被挡住**（该方向键仍按着、位置没变）→ 触发前方格子的 `trigger=1` 事件。

动作键（`trigger=0`）仍走"面朝方向"逻辑，两条互不干扰。

## 关于事件里的图片

本项目的事件用"显示图片"（231 Show Picture / 232 Move Picture / 235 Erase Picture）来放立绘等画面元素；R6.4 按普通事件命令实现这三条即可（数据来源就是事件命令本身），不单独做立绘系统。

## 验证（agent 环境实测）

```text
javac core main 49 文件 / test 9 文件 exit 0；javac lwjgl3 4 文件 exit 0
JUnit 9 个测试类 -> started=57 succeeded=57 failed=0
  （新增 EventTriggersTest 2 项：门/NPC 触发器分离、开关隐藏页后门失效；
    EventInterpreterTest 新增 2 项：201 调用 port 并结束事件、direction=0 保持朝向）
MessageCapture 672x488 -> MESSAGE OK（实测 PNG 尺寸 = 672x488）
```

## 边界（写入 R6.3b / R6.4 待办）

- 202 Set Event Location / 203 Scroll Map / 204 Map Settings / 208 Change Transparent Flag / **209 Set Move Route / 210 Wait for Move's Completion** 仍是"记录 + 跳过"（需要 NPC 运行时实体），下一批 R6.3b。
- 传送暂无 RMXP 的淡出淡入（221/222 属 R6.4 画面类）。
- 事件接触(2)/自动执行(3)/并行(4) 触发未实现。

## 修复记录（2026-10-03，用户报"接触事件一接触就闪退"）

**根因**：Transfer Player(201) 的参数读错位。RMXP 的真实布局是

```text
201: [type(0=直接/1=用变量), mapId, x, y, direction, fade]
     例：[0,3,14,15,8,1]  -> 直接传送到地图 3 (14,15)，朝上
```

首版把 `params[0]`（type，值为 0）当成地图 id，于是请求打开"地图 0"，`database.map(0)` 抛异常 → 闪退。

**修复**：按真实布局读取（mapId 索引 1、x 索引 2、y 索引 3、direction 索引 4）；type=1 时 mapId/x/y 改从变量读取；map id < 1 或没有 MapPort 时记录日志并继续执行后续命令（不再中止事件）。另外 `MapScreen.switchMap` 加了兜底：目标地图构造失败时保留当前地图、停掉等待中的事件并记录原因，不再让整个游戏崩掉。

**回归证据**：`RealDoorTransferTest` 用真实 `generated/` 数据跑 Map002 事件 1（'Next door'，玩家接触，页面含 209 开门动画 → 106 Wait 8 帧 → 201），断言"恰好一次传送，目标地图存在且坐标在图内"。没有运行时数据时该测试跳过。

## 修复记录 2（2026-10-03，用户报切换房间时 `Used autotile slot has no graphic: 0`）

**根因**：目标地图（如 354，tileset 41 `g4 Indoors`）的瓦片数据里有 22 个格子落在自动元件区间（48..95），但该 tileset 在源工程里**七个自动元件槽全是空的**（`tilesetNames = ["","","","","","",""]`）。全工程 50 个 tileset 中有 22 个是这种情况（`Poké Center`、`g4 Indoors` 等室内 tileset）。RMXP 对空槽的格子就是"什么都不画"，而首版渲染器把它当数据错误抛异常，导致 `MapScreen` 构造失败（因为已经有兜底，所以只记录 "transfer to map 354 failed" 而不是闪退）。

**修复**：`MapRenderer.loadAutotile` 遇到空槽时标记该槽为空并跳过（`drawTile` 本来就对 null 纹理不绘制），只在日志里提示一次；其余校验（图集尺寸、普通图块越界）保持不变。这样传送进这些房间能正常打开地图，格子按 RMXP 语义留空。

---

# R6.3b-1 移动路线核心与 209/210 接线（2026-10-03）

## 本次新增/修改

| 文件（相对 `runtime/`） | 行为与原因 |
|---|---|
| `core/.../data/MoveRoute.java`（新增） | 209 的路线模型：解析 `{list, repeat, skippable}`，每个移动指令保留 `code` 与原始参数；`audioName()` 从 Builder 写出的 Ruby inspect 字符串里提取 SE 名（形如 `@{class=RPG::AudioFile; ...; name=Door enter; pitch=100}`）。类注释里记录了 RPG::MoveCommand 码表（0 结束 / 1-4 四向移动 / 12-13 前进后退 / 15 等待 / 16-19 转向 / 20-22 相对转向 / 27-28 开关 / 29-30 速度频率 / 35-38 方向固定与穿透 / 44 播放 SE / 45 脚本）。 |
| `core/.../event/MapPort.java` | 新增 `setMoveRoute(eventId, route)` 与 `isMoving(eventId)`（默认实现：不播放 / 不在移动），地图侧在 R6.3b-2 接上真实 NPC 实体。 |
| `core/.../event/EventInterpreter.java` | 209 解析路线并交给 MapPort（空路线只记录日志）；210 进入 `WAIT_MOVEMENT`，每帧向 MapPort 询问，完成即继续。这样 `WAIT_MOVEMENT` 状态第一次被真正使用。 |

测试：`MoveRouteTest`（2 项，用 Map002 事件 1 的**真实**开门路线：44 播放 SE → 15/17/15/18/15/19/15 → 0，断言指令数/码值/SE 名与空路线容错）；`EventInterpreterTest` 新增 2 项（209 把路线交给 port、210 阻塞到 port 报告完成）。总计 **69 项全过**。

## 现状与 b-2 待办

门事件现在的执行顺序已经是：`209`（交给 MapPort）→ `210`（不再立刻通过，而是询问 MapPort）→ … → `201` 传送。因为 MapPort 的默认实现"不播放、不在移动"，所以门目前仍是**瞬间传送**；R6.3b-2 要做的是：

1. NPC 运行时实体（事件位置/朝向/步进动画/透明标志独立于地图数据），渲染器与碰撞改读实体；
2. `MoveRoutePlayer` 按码表播放路线（含等待、转向、播放 SE、开关、速度），接进 `setMoveRoute`/`isMoving`；
3. 顺带实现 202 Set Event Location / 208 Transparent Flag / 203 Scroll / 204 Map Settings。

---

# R6.3b-2 运行时事件实体 + 移动路线播放器（2026-10-03）

## 本次新增/修改

| 文件（相对 `runtime/`） | 行为与原因 |
|---|---|
| `core/.../event/MoveRoutePlayer.java`（新增） | 按 RPG::MoveCommand 码表播放一条路线：0 结束（repeat 则重头）/ 1-4 四向步进 / 9 随机 / 10-11 朝玩家或远离 / 12-13 前进后退 / 15 等待（1/20 秒）/ 16-19 转向 / 20-22 相对转向 / 23-24 随机转向 / 27-28 开关 / 29 速度 / 37-38 穿透 / 44 播放 SE；未实现码记录+跳过。步进走 `Context.step`（碰撞仍在地图侧），起步后等这一步落地再继续；阻挡时按 `skippable` 跳过或稍后重试。 |
| `core/.../map/EventCharacters.java`（新增） | 事件的运行时实体表：只给"人物图"事件建 `MapCharacter`（瓦片图/空白页仍走静态渲染），提供 `setLocation`(202)、`setTransparent`(208)、`setMoveRoute`/`isMoving`(209/210) 与 `update`。位置/朝向/动画因此不再改地图数据。 |
| `core/.../map/MapCharacter.java` | 新增 `teleport(x, y)`：202 与传送的瞬移（不播走步动画）。 |

测试：`MoveRoutePlayerTest`（6 项：转向、等待、步进落地、阻挡跳过、SE/开关副作用、朝玩家）与 `EventCharactersTest`（2 项：人物事件建实体而瓦片事件不建；setLocation/透明/路线播放生效）。总计 **77 项全过**。

## 边界（b-3 待办）

实体与播放器目前是"模型层"：`MapScreen` 还没把它们接进渲染与碰撞，也没有把 209 的路线交给 `MoveRoutePlayer`（MapPort 仍是默认实现）。b-3 要做的就三件事：

1. `MapScreen` 用 `EventCharacters` 建实体并交给 `MapRenderer.setEntities`（人物图事件从静态路径改为实体路径，深度排序复用现有逻辑）；
2. `Collision` 读实体的运行时位置/透明标志（202/208 立刻生效）；
3. 实现 `MapPort`（`setMoveRoute`/`isMoving` → `EventCharacters`），`update` 每帧推进路线 —— 那时门会先播开门动作与音效再传送。

---

# R6.3b-3 接线完成（2026-10-03）

1. `RuntimeContext` 新增 `bindScreenPort`：传送仍由 Context 自己处理，`setMoveRoute`/`isMoving` 转发给当前可见的 `MapScreen`。
2. `MapScreen` 构造时建 `EventCharacters`、绑定 screen port，并准备 `MoveRoutePlayer.Context`（步进走 `Collision.canStep`，SE 走 `AudioManager`，开关写 `GameState`，告警进游戏日志）；每帧调用 `eventCharacters.update(delta, routeContext)`。
3. `EventCharacters` 把运行时角色**同步回 `MapData`**（`event.x/y`、当前页 `graphic.direction`，202/208 另用页面 opacity/through 表示）。`MapData` 是每块屏幕的运行时对象（绝不写源工程），所以渲染器与碰撞不需要任何改动就能看到移动/转向/透明，避免了两套位置真源。

**已知限制（b-4 若需要再做）**：事件移动是"逐格跳跃"而不是像素插值（渲染器实体路径尚未接管人物图事件）；202/208 通过改写页面 opacity/through 表达，后续若要做 `MoveRoute` 的换图/换速等更复杂效果，建议改为真正的实体渲染路径。

验证：javac main 54 文件 / test 15 文件 exit 0；JUnit **77 项全过**（暂存区跑一次，安装后再跑一次）。

**修复（同日，用户报"门的动画还是播不了"）**：RMXP 的 202/209/210 用**角色寻址**——`0 = 本事件`、`-1 = 玩家`、其他值为事件 id。首版把 209 的 `params[0]` 直接当事件 id，门路线目标是 0（本事件）于是查不到事件、路线被丢弃；210 又无参数，被当成了玩家。现在 `setMoveRoute`/`waitForMovement` 都经 `resolveCharacter()`（0 → 正在运行的事件 id），因此门的 209 → 210 → 201 链路完整：先播 `Door enter` + 转向/等待动画，再传送。新增 2 项测试（209 目标 0 = 本事件、指定 id 仍指向该事件），总计 **78 项全过**。

---

# R6.4b 画面类命令（2026-10-03）

| 文件（相对 `runtime/`） | 行为与原因 |
|---|---|
| `core/.../event/ScreenEffects.java`（新增） | 无 UI 的画面效果状态：Fade（221/222，0=透明 255=全黑，按帧插值）、Flash（224，颜色+强度+时长，随时间衰减）、Shake（225，正弦位移+衰减）；时长一律按 RMXP 帧（1/20 秒）。 |
| `core/.../event/EventInterpreter.java` | 221/222 设淡入淡出（`WAIT` 标志 → `WAIT_TIME`）；224 闪光（颜色缺省白色，带 wait）；225 震动（带 wait）；223 Tint 与 234 图片色调**明确记录"未实现"并跳过**（见下）。 |
| `core/.../app/RuntimeContext.java` | 持有 `ScreenEffects` 并 `attachScreenEffects` 给解释器（可选服务，避免再改构造签名）。 |
| `core/.../map/MapScreen.java` | 每帧 `screenEffects.update(delta)`；`followPlayer` 把 `shakeX/shakeY` 叠加到相机位置；渲染顺序末尾画全屏覆盖层（Flash 颜色 → Fade 黑幕），覆盖图片与消息窗口，符合 RMXP 语义。 |

测试：`ScreenEffectsTest` 4 项（淡出到全黑/淡入回透明、0 帧立即生效、闪光衰减并保留颜色、震动归零 + clear）与解释器 2 项（221/222 驱动状态、224/225 启动效果并可按 wait 阻塞）。总计 **84 项全过**（暂存区 + 安装后各一次）。

## 边界

- **223 Tint Screen 无法实现**：Builder 导出的参数里 Tone 是空对象（`[{"class":"Tone"},6]`，没有 red/green/blue/gray 字段），所以只能记录日志跳过。要真正支持，需要先扩 Builder 的 Tone 解码（R2.1 式的小扩展），再做画面染色。
- **234 Change Picture Color Tone** 同理未实现（需要图片色调 + 混合）。
- 淡入淡出/闪光目前是"整屏覆盖层"，没有 RMXP 的过渡（Transition）命令（211/212）与地图切换配合；传送仍是瞬切。

## 用户本机验证

```powershell
Set-Location X:\runtime
.\gradlew.bat :core:test :core:auditCoreDependencies
.\gradlew.bat :lwjgl3:run --args="X:/generated 2"
```

窗口应为 672×488。走到地图边缘/建筑门口（例如 Map002 (36,6) 的 `Next door`）应直接切到目标地图；对话框、说话人、选择项行为同 R6.2。

---

# R6.4a 图片命令：Show / Move / Erase Picture（2026-10-03）

## 本次新增/修改

| 文件（相对 `runtime/`） | 行为与原因 |
|---|---|
| `core/.../event/PictureService.java`（新增） | 231/232/235 的无 UI 状态：编号→图片（名称、原点、x/y、zoomX/Y、不透明度、混合），232 用帧数（1/20 秒）做线性插值并支持"等待完成"。`update(delta)` 由帧循环驱动（MapScreen），保持解释器 headless 可测。 |
| `core/.../ui/PictureLayer.java`（新增） | 把 `PictureService` 画出来：屏幕坐标（RMXP 的 y 向下）、原点 0=左上 / 1=居中、混合 1=加算；缺图只报一次日志。MapScreen 与截图工具共用。 |
| `core/.../event/EventInterpreter.java` | 231/232/235 命令实现；232 的 wait 标志 → `WAIT_TIME`。 |
| `core/.../app/RuntimeContext.java` | 持有 `PictureService` 并注入解释器。 |
| `core/.../map/MapScreen.java` | 每帧 `pictureService.update(delta)`；进入地图时 `clear()`（RMXP 换图清空图片）；绘制顺序：地图 → 图片 → 消息窗口。 |
| `lwjgl3/.../MessageCapture.java` | 截图时也画图片层，用于验证立绘。 |

## 关键发现：本工程图片命令的参数布局

本工程的 231/232 参数比标准 RMXP 多一个字段（位于索引 5），例如：

```text
231: [1,"【立绘】南晓",0,0,0,0,100,100,255,0]      ← 多出的 0 在索引 5
232: [3,15,0,0,-120,0,100,100,255,0]               ← 多出的 0 在索引 5
```

按标准布局读会得到 zoomX=0（宽 0，图片不可见）、opacity=100、blend=255；跳过索引 5 后 zoom=100/100、opacity=255、blend=0，语义与画面都正确。因此 `showPicture/movePicture` 读取索引 6/7/8/9，232 的 wait 标志读索引 10。**如果以后遇到别的工程/版本，先按这份实证核对参数表。**

## 验证（agent 环境实测）

```text
javac core main 51 文件 / test 10 文件 exit 0；javac lwjgl3 4 文件 exit 0
JUnit 10 个测试类 -> started=62 succeeded=62 failed=0
  （新增 PictureServiceTest 3 项：show 覆盖、move 插值、0 帧 move + erase；
    EventInterpreterTest 新增 2 项：231+235、232 wait）
MessageCapture（672x488，Map002 事件 10）：
  [PictureLayer] 立绘 672x448 -> 截图 capture-portrait.png：立绘 + 名字条 + 对话框
```

![立绘与对话框](E:/仓库/范例/929/__r6-staging/capture-portrait.png)

## 边界

- 232 的"等待完成"用解释器计时近似（图片动画本身由帧循环推进），测试里需手动 `pictures.update(delta)`。
- 234 Change Picture Color Tone 仍未实现（记录+跳过）。
- 221/222 Fade、223 Tint、224 Flash、225 Shake 仍待实现（R6.4b）。

## 后续归属确认（用户 2026-10-03 提问）

| 未做的东西 | 归属 | 说明 |
|---|---|---|
| 开门动画（209 Set Move Route / 210 Wait for Move's Completion / 509 路线指令 / 208 透明标志） | **R6.3b** | 需要 NPC 运行时实体（事件位置/朝向/动画独立于地图数据），做完后门会先播开门动作与音效再传送。 |
| 淡出/淡入(221/222)、色调(223)、闪烁(224)、震动(225)、图片色调(234) | **R6.4b** | 需要可叠加的屏幕效果状态（目前只有消息窗口与图片层）。 |
| **Essentials 地图拼合（Map Connections / `$map_factory`，玩家跨地图无缝行走）** | **R7 及之后的 Essentials 脚本转译阶段**，不是 R6 | 依赖 `PBS/connections.txt` 与 Essentials 的 `Game_MapFactory`：玩家在地图边缘不切屏，而是把相邻地图一起渲染并做坐标偏移。当前 Runtime 是"一张地图一个 Screen"，必须先有脚本转译与地图工厂 Handler 才能接；阶段 2 内跨图只提供 201 传送（门/台阶）。 |
| **对话框/UI 皮肤（windowskin 九宫格、`\c[n]` 文字颜色）** | **R6.5（可后置）** | 用户 2026-10-03 截图确认：当前 R6.2 的样式"行为正确"但没用工程 `Graphics/Windowskins` 材质。§20 明确"行为正确优先，不要求完全还原 Essentials 皮肤"，所以皮肤放到 UI 阶段（R11 前后）统一做。 |
