# R5 GameState + 开关变量 交接（2026-10-03）

状态：源码、测试与文档已补齐；**agent 沙箱内用 JDK 21 + 缓存依赖直跑 JUnit 38 项全过（7 个测试类），Gradle 侧待用户本机复核**。本沙箱无法启动 Gradle（写权限 + loopback 被拦），因此本轮没有 Gradle 构建记录；不要把它当作 `agentCheck` 已通过。

## 本次新增

| 文件（相对 `runtime/core/src/main/java/pokemon/runtime/`） | 行为与原因 |
|---|---|
| `state/GameState.java`（新增） | §30 要求的运行期状态：Current Map、Player Position（含朝向）、Switches、Variables、Self Switches、Temporary Event State。`version()` 统计开关/变量/自开关改动次数，渲染据此判断是否需要重解析事件页；玩家移动不改变 version，因为页面条件不依赖坐标。 |
| `state/GameSwitches.java`（新增） | §28 `$game_switches`：id→boolean，默认 off，只存“开”的 id；`onIds()` 给存档/调试用。id<1 直接抛错（数据错误不静默）。 |
| `state/GameVariables.java`（新增） | §28 `$game_variables`：id→int，默认 0，只存非 0；`ids()` 给存档用。 |
| `state/GameSelfSwitches.java`（新增） | §28 self switch：按 `(mapId, eventId, channel A–D)` 存 boolean，channel 大小写不敏感；键编码为 `mapId<<32 | eventId<<2 | channel`。自开关属于地图，离开再进入不会重置（与原引擎一致）。 |
| `state/TemporaryEventState.java`（新增） | §30 Temporary Event State：按 `(mapId, eventId)` 保存每个事件的运行期记录。R5 记录“上次解析出的页号（-1 = 无有效页）”；R6 的解释器字段（RUNNING/WAIT_*、move route 等）加在 `EventState` 上。`clearMap` 对应 RMXP `Game_Map#setup` 重建 Game_Event。 |
| `state/StateVersion.java`（新增） | 三个容器共享的单调计数器，避免渲染每帧 diff 整张开关表。 |

## 本次修改

| 文件（相对 `runtime/core/src/main/java/pokemon/runtime/`） | 行为与原因 |
|---|---|
| `map/EventPages.java` | 删除“初始状态选页”`initial()`，改为 `resolve(state, mapId, event)`：按 RMXP `Game_Event#conditions_met?` 从末页往前找，switch1/switch2 必须为真、variable 用 `>= 条件值`、self switch 按 map+event+channel；解析结果写入 `TemporaryEventState`。渲染、碰撞、后续解释器共用这一个答案。 |
| `map/Collision.java` | `canStep` / `debugSpawn` 增加 `GameState` 参数，事件页的 through、tile 通行位与人物阻挡都改用活状态解析（开关翻页后阻挡随之改变）。 |
| `map/MapRenderer.java` | 构造时接收 `GameState` 与地图 id；事件图不再按“初始页”一次性定型：`refreshEventPages()` 在 `state.version()` 变化时重解析所有事件页并加载新页所需的人物图/自动元件，事件绘制槽位改为常驻（原本“初始无图”的事件永远不画）。 |
| `map/MapScreen.java` | 从 `context.gameState()` 取状态，`enterMap(mapId, x, y)` 记录当前地图与出生点；每帧把玩家坐标/朝向写回 GameState；移动与渲染都带状态。 |
| `map/TileMap.java` | 新增 `mapId()`（自开关与页缓存按地图 id 归属）。 |
| `app/RuntimeContext.java` | 持有并暴露 `gameState()`，供 Screen / 解释器 / 存档共用。 |
| `lwjgl3/.../MapRenderCapture.java` | 截图工具同步传 `GameState`（R5 给 `MapRenderer` 构造器加了状态参数）。**这是 R5 首轮遗漏的调用点**：核心测试全绿，但 `gradlew lwjgl3:run` 因此编译失败（Gradle problems report 指向该文件第 47 行）。 |

测试源码（相对 `runtime/core/src/test/java/pokemon/runtime/`）：

- `state/GameStateTest.java`（新增 4 项）：开关默认 off 且只有真实变化才 bump 版本、变量默认 0 且只存非 0、self switch 按 map/event/channel 隔离且大小写不敏感、`enterMap` 记录玩家并只清理该地图的临时事件状态、非法 id/频道抛错。
- `map/EventPageStateTest.java`（新增 5 项）：开关页替换首页并改变碰撞、变量页 `>=` 语义（含阈值 0 立即成立）、self switch 页按 map/event/channel 隔离、through 页从活状态开启通行、解析结果写入临时事件状态。
- `map/MapRenderingContractTest.java` / `map/MovementControllerTest.java` / `map/WalkingRegressionTest.java`：改传 `GameState`；其中“渲染与碰撞共用同一有效页”用例改为用开关驱动换页（原先直接改条件标志）。

## 验证（agent 环境实测）

```text
javac (JDK 21.0.7) main 40 个源文件  -> exit 0
javac (JDK 21.0.7) test  7 个源文件  -> exit 0
     javac (JDK 21.0.7) lwjgl3 3 个源文件 -> exit 0（首轮遗漏后已修复）
JUnit Platform 直跑 7 个测试类       -> started=38 succeeded=38 failed=0
  GameDatabaseTest 5 / TileMapTest 6 / MapRenderingContractTest 3 /
  MovementControllerTest 6 / WalkingRegressionTest 9 / GameStateTest 4 /
  EventPageStateTest 5
MapRenderCapture（隐藏窗口 + 真实 OpenGL + 生产 MapRenderer）：
  Map001 / Map002 / Map003 各输出 640x480 PNG，CAPTURE OK，exit 0
  （证据：__r5-staging/capture-map001.png、capture-map002.png、capture-map003.png）
```

构建类路径取自 Gradle 上次生成的 `C:\Users\Administrator\.gradle\.tmp\gradle-worker-classpath*.txt`，未运行 Gradle。`agentCheck` 里的 `auditCoreDependencies` 需要 Gradle，未执行；新增代码只 import libGDX Core 与 JDK 基础类，没有禁用前缀。

教训（写给下一阶段）：**改 `core` 的公开签名时必须同时编译 `lwjgl3`（还有 android 模块）**。本轮只跑了核心测试，漏掉 `lwjgl3` 里的 `MapRenderCapture`，是用户跑 `lwjgl3:run` 时才暴露的。

## 依据与边界

- 条件语义按源工程 `Data/Scripts.rxdata` 的 `Game_Event#conditions_met?`：条件为 switch1/switch2 为真、`$game_variables[id] >= value`、self switch 为真；页从最后一个往前找，命中即用。
- 生成数据抽查：523 张地图、1905 个开关条件页、2990 个 self switch 页、487 个变量条件页，**0 个非法条件**（id<1 或频道非 A–D）；因此“非法条件直接抛错”没有在本工程数据上触发。
- 本轮**没有**实现：Control Switches/Variables/Self Switch 命令、Conditional Branch、Message、Common Event、NPC 自主移动与移动路线（都属 R6）；事件解释器跨帧状态机（§16/§17）尚未存在，`GameState.version()` 目前只由代码/测试推动。
- 存档（R10）尚未序列化 `GameState`；`onIds()` / `ids()` 是为它准备的枚举入口。
- 事件页数据在“地图载入后不变”的前提下处理（Builder 输出是只读的）；如果将来支持运行时改页条件，需要显式 bump 版本。
- 走路 demo 的可见行为与 R4 相同：demo 地图没有任何开关被打开，所以事件选页结果和“全 false/0”的初始状态一致；R5 的验收靠 `agentCheck` 的测试，而不是画面变化。

## 用户本机验证（PowerShell）

```powershell
Set-Location X:\runtime
.\gradlew.bat :core:test :core:auditCoreDependencies
```

期望：`GameStateTest`（4）与 `EventPageStateTest`（5）在内的 7 个测试类全部通过，`core dependency audit: OK`。

游戏内冒烟（可选，确认没有回归）：

```powershell
.\gradlew.bat :lwjgl3:run --args="X:/generated 2"
```

R6 将在 `EventState` 上扩展解释器状态，并实现 Control Switches / Control Variables / Control Self Switch / Conditional Branch / Message / Common Event；届时开关造成的换页会直接在游戏里可见。
