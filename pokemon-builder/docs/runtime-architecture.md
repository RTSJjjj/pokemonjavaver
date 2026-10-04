# Runtime 架构（阶段 2）

> 对应任务书 §6–§9、§38–§41、§48–§51、§61。本文描述 `pokemon-builder/runtime/`
> 的模块边界、数据流与关键子系统；构建/打包细节见 `desktop-build.md` 与
> `android-build.md`。

## 1. 模块

```text
runtime/
├─ core/            纯 Java + libGDX Core API：逻辑、数据、输入、音频、存档、UI
├─ lwjgl3/          Desktop 后端：Lwjgl3Launcher / Lwjgl3KeyStateSource / 打包任务
├─ android/         Modern Android 后端：AndroidLauncher（API 21+）
└─ android-legacy/  Legacy 占位模块：复用契约（真实 API 14 后端属后续阶段）
```

`core` **禁止**依赖 Android 后端、AWT/Swing/JavaFX、JRuby/RubyGems（`auditCoreDependencies`
在每次 `agentCheck` 中强制）。所有平台差异通过后端注入的端口（`KeyStateSource`、
`StoragePort`）进入核心。

## 2. 数据流

```text
源 RMXP 工程 (Data/*.rxdata, PBS/, Audio/, Graphics/)
        │  Builder: scanner / event-analyzer / script-analyzer
        │           data-converter / script-compiler / audio-compiler
        ▼
pokemon-builder/generated/                 （运行时只读的构建产物）
  project.json system.json tilesets.json animations.json connections.json title.json
  maps/*.json events/*.json common-events/*.json
  scripts/{sections,blocks,apis,ir}.json
  audio/audio-manifest.json (2406 ogg)
  metadata/{build,sources,problems}.json build-report.json
        │  RuntimeDataLocator（显式参数 → POKEMON_RUNTIME_DATA → -Dpokemon.runtime.data → CWD/generated → CWD/runtime-data）
        ▼
GameDatabase.load(...)  →  RuntimeContext  →  TitleScreen → MapScreen / ScreenManager
```

- **图形（L4）**：`GraphicsLocator`（大小写不敏感）优先读数据根下的随包副本
  `Graphics/`（`build-pc`/数据包把 `Graphics/`+`Fonts/` 放到那里），
  没有时才回退源工程 `project.json.source.project`；桌面发行包因此不依赖源工程。
- **音频**：`generated/audio/`（ogg）由 `AudioManager` 播放；BGM 过场按原工程
  `pbCueBGM` 语义 1 秒交叉淡出（R6.24）。
- **存档**：`SaveManager`（JSON、`saveVersion=1`）经 `StoragePort` 写到 `saves/`；
  Desktop 入口 F5 快速存档、F9 读取、F11 全屏；L1 起可通过标题屏/暂停菜单用
  菜单槽位（1/2/3/快速）存取。设置写 `settings.json`（音量/全屏）。
- **启动流程（L1）**：无地图参数 → 标题屏（splash → 提示 → 新游戏/继续/退出）；
  显式地图参数（探针/测试/调试）仍直进 `MapScreen`；地图内 X/Esc 开暂停菜单。

## 3. core 主要子系统

| 包 | 代表类 | 说明 |
|---|---|---|
| `app` | `PokemonGame` / `RuntimeContext` / `ScreenManager` / `DisplaySettings` / `ScreenMetrics` | 生命周期、数据装载、端口转发、窗口模式与逻辑分辨率（672×448 工程默认，整数倍窗口） |
| `data` | `GameDatabase` / `MapData` / `SystemData` / `TilesetData` / `AnimationData` / `AudioManifestData` / `DataPackUnpacker` | 读取 Builder JSON；`RuntimeDataLocator` 解析数据根；`GameDatabase.load` 之后由各子系统取用；`DataPackUnpacker` 解包 Android 数据包（L3） |
| `map` | `MapScreen` / `MapRenderer` / `TileMap` / `MapCharacter` / `Collision` / `EventTriggers` / `EventPages` / `EventCharacters` / `MapLinks` / `MapAnimations` / `MapEnvironment` / `CameraScroll` / `CaveTransition` / `FieldInteractions` / `MapRouteContext` / `DoorShowHold` / `GraphicsLocator` | 地图渲染、碰撞、事件页状态、移动路线、地图连接（无缝越界）、格子/角色动画、203/204/206、洞窟动画、草丛/推石头、到达门队列；`GraphicsLocator` 优先随包素材（L4） |
| `event` | `EventInterpreter` / `EventProgram` / `MoveRoutePlayer` / `MessageService` / `MessageText` / `PictureService` / `ScreenEffects` / `ScriptIr` / `MapPort` | RMXP 事件命令与 IR 执行；`MapPort` 是解释器向地图屏发命令的唯一出口（`RuntimeContext` 转发，`RuntimePortForwardingTest` 防漏接） |
| `input` | `InputManager` / `GameAction` / `DefaultKeyBindings` | 逻辑动作（UP/DOWN/LEFT/RIGHT/CONFIRM/CANCEL/MENU/RUN）；平台只提供原始按键状态 |
| `audio` | `AudioManager` | BGM/BGS/ME/SE、cry 解析、BGM cue 交叉过渡 |
| `state` | `GameState` / `GameSwitches` / `GameVariables` / `Inventory` / `QuestLog` | 全量可存档状态（开关/变量/自开关/物品/任务/地图/玩家） |
| `save` | `SaveManager` / `StoragePort` | JSON 存档与跨平台存储端口 |
| `ui` | `MessageWindow` / `WindowSkin` / `MessagePalette` | Essentials `SpriteWindow` 九宫格皮肤、文字颜色、选项/名字框 |
| `ui.menu` | `TitleScreen` / `PauseMenuOverlay` / `SaveLoadView` / `OptionsView` / `TrainerView` / `MenuAssets` / `MenuFont` / `MenuSe` / `PauseMenuModel` / `SaveSlots` / `GameSettings` / `SplashSequence` | L1 菜单/标题层：splash 序列、暂停菜单（MPM 素材+工程 windowskin+GUI 音效）、存读档槽位、设置（`settings.json` 持久化）；标题数据来自 `generated/title.json` |

## 4. 事件与地图运行时（要点）

- **触发与页面**：0 确认 / 1 玩家接触 / 2 事件接触 / 3 autorun / 4 并行；页面按
  开关/变量/自开关条件解析（`EventPages`）。门由"到达页 + `tsOff?("A")`"驱动，
  多门地图的 autorun 排队见 `event-runtime.md`（R6.33 修复）。
- **脚本**：事件 355/655 由 Builder 编译为 `generated/scripts/ir.json`；运行时
  `ScriptIr` 按块 id 取命令执行，**355 与 655 续行合并为一段**（R15 修复双执行）。
- **地图连接**：`MapLinks` 把连通邻居画在接缝外，越界行走后原地换图（无淡出）；
  相机仅在 `SnapEdges` 时夹取（R6.23）。
- **性能**：邻居视图跨图复用、tileset 分页只解码一次、立绘按需加载、慢操作日志
  （R6.25；切图帧 67–95 ms → <8 ms）。

## 5. 测试与验证

| 层 | 位置 | 运行 |
|---|---|---|
| Builder 单元（252） | `builder/tests/*.test.js` | `scripts\test-builder.bat` / `node --test` |
| Small Test Project + Golden + Headless Smoke | `tests/fixture-project/` + `runtime/core/src/test/java/pokemon/runtime/fixture` | fixture 测试（Node 4 项 + Java 8 项） |
| Core 单元（280） | `runtime/core/src/test/java` | `gradlew :core:test`（无 OpenGL；真实 `generated/` 存在时数据用例实际执行） |
| 构建级 E2E | `logs/backup-*`、`__r6-staging/run-*.ps1` | `build-pc` / `build-android`（见 R14/R15 记录） |
| 真机/真窗口 | 用户本机 | `gradlew lwjgl3:run`、`dist\Windows\PokemonGame.exe` |

`gradlew agentCheck` = Builder 全套 + `:core:test` + `:core:auditCoreDependencies`，
不依赖 Android SDK（§44）。

## 6. 明确不做（阶段 2）

- 不实现通用 Ruby→Java 编译器：Ruby 只存在于源工程与 Builder 构建阶段（§84）；
  运行时不携带 Ruby、不运行 RGSS。
- 战斗/宝可梦域（`pbSet`/`pbGet`/背包/伙伴/战斗等）属阶段 3；相关脚本块在
  `build/reports/java-handler-required.json` 中如实登记（当前 891 条）。
