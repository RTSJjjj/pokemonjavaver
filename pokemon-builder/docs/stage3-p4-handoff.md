# 阶段 3 · P4（含 P2d 战斗界面）交接文档

> 面向接手 **P4 菜单域 + 图形界面** 的工程师/模型。
> 数据、事件、战斗核心（P0–P3）已完成并有测试；本阶段主要是**画面与交互**。
> 先读 `docs/stage3-plan.md`（总计划）和 `docs/architecture.md`、`docs/runtime-architecture.md`、`docs/event-runtime.md`、`docs/script-compiler.md`。

---

## 0. 一句话现状

- 仓库：`https://github.com/RTSJjjj/pokemonjavaver`（public，main=HEAD）。
- 已完成：地图/事件运行时（阶段 2）、**P0 宝可梦数据模型**、**P1 队伍/背包(扁平)/PC/存档**、**P2a 遇敌&训练家数据**、**P2b 步数遇敌+碎岩**、**P2c 无头战斗核心**、**P3 升级学招/进化/孵蛋/缎带**。
- 未做（本交接范围）：**P2d 战斗界面**、**P4 全部菜单/界面**、背包分栏、道具使用与道具进化、图鉴 seen/owned、PC 盒子 UI + 多地区分仓、交易界面、**L15 暂停菜单视觉**。
- 测试基线：**Builder `node --test` 278/278**、**Java `:core:test` 331/331**、CI（`.github/workflows/stage2-ci.yml`）绿。
- 覆盖：脚本 IR `translated 4526 / javaHandlerRequired 717 / unsupported 198 / 96.4%`。

---

## 1. 环境与命令（Windows）

```bat
REM 中文路径下 Gradle 需要 ASCII 盘符视图；subst 有时会被清理，丢了就重建
subst X: "E:\仓库\范例\929\pokemon-builder"
set JAVA_HOME=C:\Users\Administrator\AppData\Roaming\.minecraft\runtime\java-runtime-delta

REM 构建数据（PBS/地图/事件/脚本 → generated/）：改任何 converter/编译器后必须跑
E:\仓库\范例\929\pokemon-builder\scripts\build-data.bat "E:\仓库\范例\929"

REM 测试
cd E:\仓库\范例\929\pokemon-builder\builder && node --test        REM Builder 测试（别在仓库根跑，会扫到 logs/ 备份）
cd X:\runtime && gradlew.bat :core:test --console=plain           REM Java 测试

REM 从源码跑游戏（现在会自动指向 generated/；见 build.gradle 的 run 任务）
cd X:\runtime && gradlew.bat :lwjgl3:run --args="X:\generated 1"   REM 可直接带地图 id 跳过标题

REM 出包
scripts\build-pc.bat "E:\仓库\范例\929"          REM dist/Windows（含 runtime-data/）
scripts\build-android.bat [--release]            REM APK

REM 本地整链（= CI 第 5 步）：Builder→编译器→Core→桌面→Android
scripts\ci-build.bat
```

- 崩溃排查：启动器已装全局异常处理，堆栈写入 **`~/pokemon-runtime-crash.log`**（`Lwjgl3Launcher.installCrashLog`）。
- CI 偶发红一般是 **Maven Central / plugins.gradle.org 返回 403**（依赖解析瞬时抽风，代码无关）；重推空提交即可。
- Git 凭据已存（GCM），push 直接可用。
- **仓库只跟踪 `.github` + `pokemon-builder`**；`generated/` 与 `dist/` 均被 gitignore，不要提交产物（fixture golden 例外，见 §6）。

---

## 2. 代码地图（`pokemon-builder/`）

### 2.1 Builder / 编译器（Node）
- `tools/data-converter/`：`index.js`（扫描+转换+IR 落盘+project.json）、`pbs.js`（PBS 文本→JSON）、`convert.js`、`cache.js`。
- `tools/scanner/`、`tools/script-analyzer/`、`tools/report-writer/`。
- `builder/src/script-compiler.js`：**事件脚本 → IR**。核心两张表：
  - `HANDLERS`：简单 API 调用 → IR；`POKEMON_METHODS`/`POKEMON_PROPERTIES`/`pokemonStatement`：局部变量+宝可梦对象方言；`DOMAIN_APIS`：阶段 3 域 API 兜底分类。
- `generated/` 产物：`project.json`、`maps/`、`events/`、`common-events/`、`scripts/{blocks,ir}.json`、`pbs/*.json`、`metadata/`。

### 2.2 运行时（Java，`runtime/`）
- 入口：`runtime/core/.../app/PokemonGame.java`（`Game.setScreen` 切屏）、`RuntimeContext.java`（共享上下文：`gameState`、`database`、`messageService`、`inputManager`、`audioManager`、`mapPort`、`battlePort`、`saveManager`、`screenEffects`、`pictureService`；`newEventInterpreter()` 给并行事件）。
- 数据：`data/GameDatabase.java`、`MapData`、`TilesetData`、`ProjectInfo`（含 `runtime.players`/`playerCharset`/`messageFont`/screen size）、`data/pokemon/PbsData.java`（所有 PBS 表 + 遇敌/训练家）、`Battle`/`Battler`/`BattleMove`/`DamageCalc`/`BattlePort`/`HeadlessBattlePort`/`WildEncounters`。
- 状态：`state/GameState.java`（switches/variables/self/temp、`inventory()`、`quests()`、`trainer()`、`playerName`、`playerId`）、`pokemon/TrainerState.java`（name/gender/money/`Party`/`Storage`/heal point）、`pokemon/Party.java`、`pokemon/Storage.java`（**200 箱×30**）、`pokemon/Pokemon.java`、`PokemonStats`、`PokemonGrowth`。
- 地图/事件：`map/MapScreen.java`（渲染+输入+走步钩子：草地沙沙/孵蛋/遇敌）、`map/EventCharacters`、`Collision`、`FieldInteractions`、`TileMap`、`MapRenderer`、`CaveTransition`；`event/EventInterpreter.java`（IR 逐条执行，所有命令在这里 `switch`）、`MessageService`、`MapPort`、`PictureService`、`ScreenEffects`、`ScriptIr`。
- 存档：`save/SaveManager.java`（`saveVersion 2`；队伍/PC/缎带/经验/`playerId` 等）。
- **UI 框架（P4 主要在此扩展）**：
  - `ui/WindowSkin.java`（窗口皮）、`ui/MessageWindow.java`（文字窗，分页/选项）、`ui/MessagePalette.java`、`ui/PictureLayer.java`、`ui/ScreenManager.java`（目前是占位屏）。
  - `ui/menu/`：`MenuAssets.java`（加载 Graphics/Pictures、windowskin）、`MenuFont.java`（字体）、`MenuSe.java`（音效）、`PauseMenuModel.java`、**`PauseMenuOverlay.java`（现有菜单范例：`open()/close()/isOpen()/update()/render()` + `Host` 回调，由 `MapScreen` 持有并叠在地图上）**、`TrainerView.java`、`SaveLoadView.java`、`SaveSlots.java`、`OptionsView.java`、`GameSettings.java`、`TitleScreen.java`、`SplashSequence.java`、`TitleAnimations.java`。
  - **做 P4 菜单最省事的路线：照 `PauseMenuOverlay` 的 overlay + Host 模式，新开一个 `ui/menu/PartyView`/`BagView`/…，由 `MapScreen` 或暂停菜单拉起。**
- 截图/无头探针（P4 验收用）：`runtime/lwjgl3/.../MenuCapture.java`、`MapRenderCapture.java`、`MessageCapture.java`（可用 `gradlew :lwjgl3:captureMap -PcaptureArgs=...` 等产出 PNG 自检）。

### 2.3 关键约定
- **新增事件 IR 命令**要三处同步：① `script-compiler.js` 的 `HANDLERS`；② `EventInterpreter.executeIrStep` 的 `case`；③ 两侧测试。改完跑 `build-data`。
- `pub` 端口模式：地图相关走 `event/MapPort.java`，战斗走 `battle/BattlePort.java`，由 `RuntimeContext` 绑定实现。
- IR 版本 `IR_FORMAT = pokemon-builder/debug-ir/4`；全量重建由 `build-data` 负责。
- 存档字段尽量**可选**（旧档可读）；`fromJson` 版本接受 `1..SAVE_VERSION`。

---

## 3. P4 任务清单（建议顺序）

> 每块都要：单测（headless）+ 截图探针（如适用）+ `build-data` 若改编译器 + 更新 fixture golden（如改动影响 fixture 工程输出）。

### P4a 菜单窗口/字体/配色基座
- 复用 `MenuAssets`/`MenuFont`/`WindowSkin`；抽一个通用「列表/详情窗」控件（左右双窗、光标、滚动、返回）。
- 参考 `PauseMenuOverlay` 的输入处理（`InputManager.wasPressed(GameAction.*)`）与 `MessageWindow` 的分页/选项。
- 验收：一个空菜单可开/关/上下移动/返回，截图一张。

### P4b 宝可梦菜单（详情 / 招式 / 道具 / 交换换位）
- 数据来源：`GameState.trainer().party`（`Party`）与 `Storage`；`Pokemon` 的六维/IV/EV/性格/特性/招式/HP/状态/缎带。
- 需要：`Pokemon` 的 `nature`/`ability` 显示名解析（`PbsData`），招式 PP/威力/命中。
- 「交换换位」用 `Party.swap`。

### P4c 背包（分栏 + 关键物品 + 使用道具 + 道具进化）
- 现状：`state/Inventory.java` 是**扁平计数**（`add/remove/count/counts`），存档只存 `item→count`。
- 需要：按 `PbsData.Item.pocket` 分栏（口袋名/顺序见工程 `PItem_Bag`/`Pockets`），关键物品单列，**数量 0 不显示**；使用道具流程（回复/进化石/学习机）。
- **道具进化**：`PokemonGrowth.evolve(pokemon, species)` 已就绪；用 `pbs.species(...).evolutions` 里 `method=="Item"`（及 `ItemMale/ItemFemale/SpecialItem/...`，`parameter` 是道具名）匹配。
- 事件侧：`pbStoreItem`/`pbDeleteItem`/`pbGetKeyItem` 已能加到期（`GIVE_ITEM`/`REMOVE_ITEM`/`GIVE_KEY_ITEM`），分栏只影响 UI。

### P4d 图鉴 seen / owned
- 现状：`TrainerState` 没有 seen/owned。`pbAddPokemon`（P1）应标记 owned（当前未标）。加 `seen`/`owned` 集合到 `TrainerState` 并纳入存档；`PbsData.speciesById` 可做 dex 顺序。
- 事件脚本：`pbShowMap`(61 块) 等地图界面可一起。

### P4e 训练家卡
- 参考 `ui/menu/TrainerView.java`（已有骨架，`state.inventory().size()` 等）；显示姓名/金钱/徽章/时间/图鉴数。

### P4f PC 存储（含多地区分仓）+ 盒子 UI
- 现状：`Storage`（30×200，单仓）。原工程 `$PokemonStorage` 是**按地区多仓**（`@storages[@rgnmap]`, `getCurrentStorage`）——之前约定**在 P4 做 PC 界面时一并处理**。
- 需要：多仓结构（region→Storage）、移动/取出/放出、盒子切换、背景图、`pbPokeCenterPC`(59)/`pbStoreItem` 相关。

### P4g 交易界面 + 事件接线
- 事件脚本 `pbStartTrade`(37)/`pbChoosePokemonForTrade`(39) 目前 `JAVA_HANDLER_REQUIRED`（pending）。做交易 UI（我方选一只 / 对方 `TrainerData` / 昵称）后把它们转成 IR 并在解释器执行。
- 参考原工程 `PScreen_Trading`、`PSystem_PokemonUtilities#pbChoosePokemonForTrade`。

### P4h L15 暂停菜单视觉
- 现有 `PauseMenuOverlay` + `SaveLoadView`/`OptionsView`/`TrainerView` 逻辑在，视觉待补：MPM 横幅、训练家立绘、模糊地图快照、滚动条、顶部信息（见 `PauseMenuModel` 与工程 `ModularPauseMenu`/`MPM` 素材）。

### P4i（若先做）P2d 战斗界面
- 现状：`BattlePort` + `HeadlessBattlePort`（同步跑完），`Battle` 是无头引擎（回合/伤害/命中/濒死/经验，无状态/道具/换人/捕捉）。
- 目标：`BattleScreen` 按帧驱动 `Battle`；把 `HeadlessBattlePort` 换成交互式 `BattlePort`（玩家选招式/道具/换人/逃跑，AI 出招）。素材：`Battlebacks/`、`Graphics/UI`。
- 建议与 P4 菜单共用同一套窗口/字体管线，先搭 `BattleScreen` 帧循环再补细节。

---

## 4. P4 之后（背景信息）
- **P5 插件特性**：特/状态/道具战斗效果、`Move_Effects_*`、跟随宝可梦、`boss_reward` 等；`DOMAIN_APIS` 里 `setBattleRule` 等逐步实现。
- **P6 触摸 + 真机**：L2 触摸额外按键、Android 验收。
- **P7 收敛发布**：unsupported 清零、release 签名、最终报告。

---

## 5. 已知坑 / 注意事项
- **图形工作注意**：本仓库是可运行的真实 libGDX 项目，UI 全部用现有 `SpriteBatch`/`Texture`/`BitmapFont` 资产；不要引入新依赖。所有素材来自工程 `Graphics/`（已随包到 `runtime-data/`）。
- **stdout 缓冲**：从 gradle `run` 看不到游戏内日志（进程退出才 flush）；排查用 `~/pokemon-runtime-crash.log`。
- **空参数容错**：`EventInterpreter.intParam/floatParam` 已容错空串（曾导致开场闪退），新命令读参数也请用它们。
- **玩家角色链**（已做）：`GameState.playerId`（-1=空行走图），`pbGenderSelector→GENDER_SELECTOR`（选性别 → **游戏内「字库」拼音输名字**，见 §9.2），`pbChangePlayer(id)→CHANGE_PLAYER` 用 `project.json runtime.players[PlayerA..H]`。
- `subst X:` 会被清理；丢了重建。
- 改 `builder/tests/fixture-project` 影响的金牌输出：`node tests/fixture-project/generate.mjs`（**从 `pokemon-builder/` 根跑**）后提交。
- `build-pc`/`ci-build` 会重写 `docs/*-audit.md` 的时间戳行（可一并提交）。
- 仓库工作流：`E:\仓库\范例\929` 直接是 main 工作副本；GitHub Desktop 或 `git` 均可。

---

## 6. 测试与验收规范
- Builder：`builder/` 下 `node --test`（278 用例）。
- Java：`X:\runtime` 下 `gradlew :core:test`（351 用例）；无头优先，所有界面逻辑尽量抽成可单测的 model（参考 `PauseMenuModel`、`NameEntryModel`）。
- 图形探针：`MenuCapture`/`MapRenderCapture`/`MessageCapture` 可无交互产出 PNG，适合 P4 每屏一张截图回归。
- 事件/IR 改动的三处同步 + `build-data`；分类保持诚实（域 API→`JAVA_HANDLER_REQUIRED`，非域未知→`UNSUPPORTED`）。
- CI 必须绿（`.github/workflows/stage2-ci.yml` → `scripts/ci-build.bat`）。

---

## 7. 关键文件速查
| 目标 | 文件 |
|---|---|
| 事件 IR 编译 | `builder/src/script-compiler.js` |
| 转换/落盘 | `tools/data-converter/index.js`, `pbs.js` |
| 解释器命令 | `runtime/core/.../event/EventInterpreter.java` |
| 共享上下文/接线 | `runtime/core/.../app/RuntimeContext.java` |
| 状态 | `state/GameState.java`, `pokemon/TrainerState.java`, `pokemon/Party.java`, `pokemon/Storage.java` |
| 宝可梦 | `pokemon/Pokemon.java`, `PokemonStats.java`, `PokemonGrowth.java`, `pokemon/PbsData.java` |
| 战斗 | `battle/Battle.java`, `Battler.java`, `DamageCalc.java`, `BattlePort.java`, `HeadlessBattlePort.java`, `WildEncounters.java` |
| 存档 | `save/SaveManager.java` |
| UI 基座/范例 | `ui/WindowSkin.java`, `ui/MessageWindow.java`, `ui/menu/PauseMenuOverlay.java`, `MenuAssets.java`, `MenuFont.java`, `TrainerView.java` |
| 地图/走步钩子 | `map/MapScreen.java`（`updateGrassRustle`/`checkStepEncounter`/`stepEggs`/`applyPlayerCharset`） |
| 截图探针 | `runtime/lwjgl3/.../MenuCapture.java`, `MapRenderCapture.java`, `MessageCapture.java` |
| 字库拼音输入（P4） | `tools/data-converter/index.js#parsePinyinTable`, `data/PinyinTable.java`, `ui/menu/NameEntryModel.java`, `ui/menu/TrainerSetupView.java` |
| 原工程菜单布局对照 | `docs/stage3-menu-layout-reference.md` |
| 计划/文档 | `docs/stage3-plan.md`, `docs/architecture.md`, `docs/event-runtime.md`, `docs/script-compiler.md` |

---

## 8. 交接检查表（开工前）
- [ ] 能 `scripts\ci-build.bat` 全绿（本地）。
- [ ] 能 `gradlew :lwjgl3:run` 看到开场（选角前空行走图，跑完变 `trchar000`）。
- [ ] 读通 `PauseMenuOverlay` 的 open/update/render/Host 模式。
- [ ] 明确 P4 子块顺序（建议 P4a 基座 → P4b/c/e 菜单 → P4f PC → P4d/g → P4h L15；P2d 可先或并行）。

---

## 9. 最新补丁（DeepSeek，工作区未提交；与 P4 一起提交）

> gpt6 的 P4/P2d 仍在工作区，下面是本轮补的**修复**与**「字库」名字输入**实现。
> 涉及文件：`event/EventInterpreter.java`、`event/EventInterpreterTest.java`、`ui/menu/GenderSelectorView.java`(新，取代自造的 `TrainerSetupView`)、
> `ui/menu/NameEntryModel.java`(新)、`ui/menu/NameEntryModelTest.java`(新)、`ui/menu/PauseMenuOverlay.java`、`ui/menu/TitleScreen.java`、
> `data/PinyinTable.java`(新)、`data/GameDatabase.java`、`tools/data-converter/index.js`、`ui/menu/P4MenuTest.java`。

### 9.1 修复：Show / Move Picture 坐标错位（开场图片全偏）
- 工程 `Interpreter#command_231/232` 参数顺序：`[number, name, origin, appointmentMode, x, y, zoomX, zoomY, opacity, blend]`。
- 运行时原读 `x=p[3], y=p[4]`（把"坐标来源模式"当成了 x）→ 开场 `mapRegion0` 本应居中 `(336,224)`，实际画到 `(0,336)`。
- 现读 `x=p[4], y=p[5]`；`p[3]==1` 时按变量 id（`$game_variables[p[4]/p[5]]`）。方法：`showPicture`/`movePicture`/`pictureCoordinate`。
- 测试：`EventInterpreterTest.showPictureCoordinates`（并更新了两个用旧 9 参数格式的旧测试）。

### 9.2 修复：选角色后事件卡死 + 接入「字库」名字输入
- **根因**：libGDX 1.13.5 桌面 `DefaultLwjgl3Input.getTextInput` 是 no-op（源码注释 `// FIXME getTextInput does nothing` + `listener.canceled()`），所以 gpt6 的 `Gdx.input.getTextInput` 名字框在桌面**永不返回** → `MenuService.Kind.GENDER` 请求永不 `done` → 事件卡在原地。
- **正解（已实现）**：移植工程 `字库` 插件（`class PBZ_IM_quanpin` 全拼字库 + `class PokemonEntryScene2` 选字 GUI）为**游戏内输入**：
  - 数据：`tools/data-converter/index.js#parsePinyinTable` → `generated/text/pinyin.json`（**424 音节→汉字**，`yu`→50 字等），已注册 expectedOutputs。
  - 运行时：`data/PinyinTable`（读 `text/pinyin.json`）+ `GameDatabase.pinyin()`（惰性）。
  - 模型（无头，可测）：`ui/menu/NameEntryModel` —— 字母→拼音缓冲→该音节候选字（分页，每行 11 字 = `PokemonEntryScene2::MaxCharsPerLine`）→选字组合；退格先删拼音再删名字；长度上限。
  - 视图（**照原工程 `BWGenderSelector` 移植**，不再自造）：`ui/menu/GenderSelectorView` —— 用 `Graphics/Pictures/{introbg,genderselect,introBoy,introGirl}.png`，流程与脚本一致：
    `introbg` 背景 + `genderselect` 男/女条 + `introBoy`/`introGirl` 立绘 → 淡入 → `你是男生还是女生？` → ←→选角 → 确认 `是男生吗？/是女生吗？` → `pbChangePlayer(0/1)` + `$game_variables[52]=id` → `还不知道你的名字呢…` → **字库拼音输名字** → `你的名字是{1}？` → 完成。
    坐标用脚本里的 `boy.x=100`、`girl.x=Graphics.width-130`、`y=Graphics.height/2`、`genderselect` 上/下半条与 `ox=barwidth/2∓barwidth/6`（RMXP 左上原点 → libGDX `y_gdx = 448 - y`）。
- **视觉待补**：脚本里的逐帧动画（`selectBoy/selectGirl` 6 帧、`selection` 22 帧的位移/zoom/tone）现为插值近似；`genderselect` 条与立绘的精确动效可按原脚本细调。

### 9.3 其它
- `P4MenuTest.optionalSaveFieldsRoundTripAndLegacyBackfillsDex` 修复：测试 `setup()` 少了 `state.enterMap(...)`，且旧档 JSON 没带 `map`（`fromJson` 契约要求地图；与已提交的 `SaveManagerTest.rejectsBadDocuments` 一致）。
- 玩家角色链（P-select，已提交 `8926116`）：`GameState.playerId`（-1=空行走图）/ `GENDER_SELECTOR` / `CHANGE_PLAYER` / `project.json runtime.players[PlayerA..H]` / 存档 `playerId`。
- 参考文档：`docs/stage3-menu-layout-reference.md`（原工程各屏与自定义面板的坐标/素材对照表）。
- 当前测试：Java **351/0**（含 `NameEntryModelTest` 3 项）；`build-data` 成功（translated **4661** / 96.4%）。

### 9.4 标题错位修复 + **GUI 插件权威来源**（照脚本库来，别自造）
- **标题错位**：原插件 `MTS_Element_Logo` 里 `logo1` 与 `logo2` 都各自 `ox = bitmap.width/2`（各自按自身宽度居中于 `@x`）。运行时原来让 `logo2` 沿用 `logo1` 的左边（logo1 386×148、logo2 200×113 → logo2 左移约 93px）。已改为**各自居中**（`TitleScreen.renderScene`：`logo1Left`/`logo2Left`），并核对了其余项均与插件一致：
  - `background:bw` → `Backgrounds/bw`（672×448 全屏）；
  - `overlay2` → `MTS_Element_OL2` 默认 `Overlays/scrolling002`（672×448）；
  - `effect6_y312` → `Particles/shine002`（中心 `(width/2, y=312)`）；
  - `misc4_s2_x284_y339` → `MTS_Element_MX4` 在 `ModularTitle::SPECIES` 有值时画宝可梦，本工程 `SPECIES=nil` → **正确地不画**；
  - `start`（闪烁"按任意键"）→ `start.y = height*0.85` 居中（`Scene_Intro` 的 `@pic2`）。
- **GUI 一律对照脚本库里的插件**（名字带 `bw`/`b2w2` 或 `MSF`/`Modular`）：
  | 界面 | 插件分节 |
  |---|---|
  | 标题 / 载入图 | `Modular Title Screen`（配置 `ModularTitle`）+ `MTS_Script`（渲染器 `ModularTS`）；"Press Enter" = `Scene_Intro` |
  | 开始/读档菜单（新的冒险/继续/选项/退出 + 存档面板）| `004_MSF_UI_Load`（`PokemonLoad_Scene`/`PokemonLoadScreen`；面板 `x=24*2+32+64`、`y=32` 步进 `48`；玩家行走图 `x=56*2-cw/8+32+64, y=32*2-ch/8`；队伍图标 `x=(46+32*i)*2+32+64, y=110*2`）；相邻 `001_MSF_AutoSave`/`002_MSF_SaveData`/`003_MSF_UI_Save` |
  | 选角色 + 名字 | `BWGenderSelector`（+ `BWGenderSettings`）+ `字库`(`PBZ_IM_quanpin`) |
  | 宝可梦菜单 | `BW PScreen_Summary`（+ `PScreen_Party`） |
  | 背包 | `BW Bag`（+ `PScreen_Bag`） |
  | 图鉴 | `PokedexMenu/Main/Entry BW Style`（+ `PScreen_Pokedex*`） |
  | 训练家卡 | `B2W2 Trainer Card` |
  | PC 存储 | `B2W2 PC`（+ `PScreen_PokemonStorage`/`PScreen_ItemStorage`） |
  | 存/读档界面 | `BW_SaveScreen`、`003_MSF_UI_Save`、`004_MSF_UI_Load` |
  | 路牌 | `BW_SignPosts`（+ `BW_SignPosts_Config`） |
  | 暂停菜单 | `Modular Pause Menu`/`Modular Menu` |
  更细的窗口坐标/素材见 `docs/stage3-menu-layout-reference.md`。

### 9.5 训练家/暂停菜单对齐 `Modular Menu` + `Modular Pause Menu`
- **入口**（`PauseMenuModel`，照 `MenuHandlers.addEntry` 顺序/标签/图标）：`图鉴 / 寄存系统 / 宝可梦 / 背包 / 训练家(\pn=玩家名) / 保存 / 读档 / 退出 / 设置 / 退出游戏`；未实现的 `分布图鉴/宝可装置/任务/调试` 不显示；`宝可梦` 按插件条件 `party.length>0` 隐藏。（**更正**：`训练家` 条目本就在插件里，标签 `\pn`，不是自造。）
- **版式**（`PauseMenuOverlay.renderMain`，照 `ModularPauseMenu#refresh/pbStartScene`）：
  - **立绘在左**：`(0, 340-立绘高)`（插件把左上角放 top-origin `y=height-340`；libGDX 从左下角画，故 `y=340-H`，之前写成 `height-340` 显示的是下半身）；`MPM/intro_Girl1`/`Boy1`（按性别）；
  - **窗口不再留黑边**：`DisplaySettings.windowUnit*` 改用**逻辑分辨率**(672×448)，窗口是其整数倍，标题/暂停菜单底部不再被黑幕(旧的 672×488 窗口单位造成的 letterbox)挡住版本号；`DisplaySettingsTest` 已同步；
  - 入口是**右侧面板**：`MPM/sel.png` 420×72，左半=未选、右半=选中；图标 64×72 画在面板 `+8`，文字 `+66`；面板 `x = width-210-40`、`y = 40+84*i`（选中项左移 6、未选变暗）；可见窗 4 项，`j=max(0,index-3)` 滚动；
  - 最右滚动条 `MPM/scrollbar_bg`(8×204) + 拉伸的 `scrollbar_kn`(16×16)，`x=width-28, y=(height-204)/2`；
  - **细节修正**（照 `PokemonLoad_Scene`/`PokemonLoadPanel#refresh` 的 dumps 逐条核对）：版本号/页脚改用 `MenuFont.lineHeight()` 定位（`y=lineHeight()+4`），不再被窗口下边缘裁掉（之前 `y=6/8`，去掉 letterbox 后文字下半截被切）；
  - 背景：地图快照(模糊) + `MPM/bg` + 滚动 `MPM/panorama`；左上日期时间、左下 `版本号：CURRENT_NAME`；去掉了自造的顶栏(名字/金钱/图鉴)与底栏(Z 确认/X 返回)。
- 旧的 `PauseMenuModelTest` 已按新顺序更新，并加了"有队伍时宝可梦条目出现在插件位置"的用例。

### 9.6 开始/读档菜单对齐 `004_MSF_UI_Load`
- `TitleScreen.renderCommands` 去掉自造的 windowskin 窗口，改为照 `PokemonLoad_Scene#pbStartScene`：
  - 背景 `Graphics/Pictures/loadbg`（672×448）；
  - 命令面板 `Graphics/Pictures/loadPanels`（408×536：继续未选 0、继续选中 222、普通未选 444、普通选中 490），面板 `x=144`、`y` 从 32 起步进 **224（继续槽 408×222）/ 48（普通 408×46）**；
  - "继续"面板（`PokemonLoadPanel#refresh`，文本画在面板自身 bitmap 上、坐标面板相对）：标题 = `←存档N→`/`←自动保存→`（`SaveData.get_*_slot`），名字在 `(112,56)` 且按性别用 `MALETEXTCOLOR`/`FEMALETEXTCOLOR`，地图名右对齐 `388/10`；`line_y=[10,64,96,128]`（top-origin）：`徽章：`/`图鉴：` 在 `268/64`、`268/96`，值右对齐 `388`；`时长：`/`保存时间：` 在 `28/96`、`28/128`，值右对齐 `248`；普通行标签在 `(32,10)`（`TEXTCOLOR=232,232,232`、阴影 `136,136,136`）；
  - 顶部提示 `[←]/[→]:切换存档插槽`（`004` 的 20px tips bitmap，`y=0` 左上、半透明 `248/88,128`），**仅当"继续"行被选中时显示**；`←/→` 在继续行上循环四个存档槽（`SaveSlots.list`，默认选 `get_newest_slot` 的最新档），确认直接读取该槽（`SaveSlots.Slot` 另读取 `gender`）；
  - `SaveSlots.Slot` 现已读取徽章数/图鉴数/时长/性别；`playSeconds` 按 double 读。
- `MenuCapture` 现在有 Gradle 任务 `:lwjgl3:captureMenu`（`-PcaptureArgs=dataRoot|outputDir`）逐屏产出 PNG，便于 P4 每屏回归（已用 `l14-title-commands.png` 核对本屏；capture 里已 seed 玩家 `playerId=0` 与 BULBASAUR/CHARMANDER/SQUIRTLE 队伍）。
- **已补 `pbSetParty`**：`SaveSlots.Slot` 另读 `playerId` 与 `trainer.party[].{species,gender,shiny,egg}`；`TitleScreen.drawPartySprites` 画训练家行走图（`Characters/<runtime.players[id].charset>`，取 `src_rect` 首帧 `w/4×h/4`，`x=208-frameW/2, y=height-64-frameH/2`）与队伍图标（`Icons/icon%03d[+f][+s]`，原点 Center=脚下 `(188+64i, 220)`、`oy=h*5/8`）；`MenuAssets` 加 `character()`/`icon()` 载入器。

### 9.7 背包对齐 `BW Bag`（PScreen Bag Graphical Overhaul）
- `BagView` 重写为 `BW Bag` 的 `PokemonBag_Scene#pbStartScene/pbRefresh*` 版式（不再用自造 `MenuPanel`）：
  - 背景：滚动 `AnimatedPlane` `Pictures/Bag/bg_grid`（女 `bg_gridf`，576×336 平铺，`ox+=1`/帧）→ 背包图 `bag_<pocket>`（女且存在则 `bag_<pocket>_f`，`(-30,10)`）→ 外框 `bg`/`bg_f`（672×448）；
  - 口袋图标：`icon_pocket`（28×28，`blt(2,2,src((pocket-1)*28,0,28,28))` 在 `(0,-3)` 的 186×32 sprite 上）；口袋名 `PokemonBag.pocketNames[pocket]` 画在 `(110,186)`；
  - 物品窗 `Window_PokemonBag` `x=218-32+64=250, y=-8, w=326+32=358, h=80+9*32=368`，`ITEMSVISIBLE=9`、行高 32；**`windowskin=nil` ⇒ SpriteWindow 边框 32、`startX/Y=16`，内容原点 `(266,8)`**，`drawItem` 再 `+16/+16`，故：光标 `Bag/cursor` 在 `(266+12, row+1)`、图标 `Icons/item%03d` 48×48 缩 24×24 在 `(266+16, row+22)`、名字 `(266+44, row+20)`、数量 `x%3d` 右缘 `266+326-16=576`、`icon_register` 在 `266+326-64`；重要道具（`pocket==8` 或 `fieldUse==4`）不显示数量、显示 `icon_register`；末行 `关闭背包`；
  - `Bag/cursor.png` 是 **314×68**、圆角框在 `y=16..53`（38 高），要取 `src(0,16,314,38)`（之前只取上半 → 高亮框被削一半）；
  - 右侧滚动条 `icon_slider`（`534+64=598`，上/下箭头 + 滑块，多于 9 行时）；
  - 选中道具 48×48 图标 `(48,height-48)` Center 原点、说明文字框 `(84,334)`⇒内容 `(100,350)` `564×128`（`ITEMTEXTBASECOLOR 248,248,248`/阴影 `90,90,90`）；左上 `[Z]:手动 [Shift]:自动` 用**小字**（`PauseMenuOverlay` 新增 size 14 的 `smallFont`）右对齐 `(108,8)`。
- **选项窗体照插件**：`BW Bag` 的 `pbShowCommands` 用项目 windowskin 的窗口；`BagView.drawAction` 改画右侧命令窗（`使用/给予/丢弃/取消`，`▶` 光标）+ 底部消息窗 `已选择<道具名>`，不再是自绘面板。
- `BagModel`：`cursor` 可见 9 行、多一行 `关闭背包`（`selected()` 在末行返回 null → 关闭背包）。
- 机制：`使用 / 给予 / 丢弃(remove 1) / 取消`（`ItemUse`/`PartyModel.giveItem`），未移植登录/整理（插件 `A/SHIFT` 排序）——需要再说。
- `PauseMenuOverlay` 加 `openAt(action)` 便于截图/测试直接进入某子界面；`MenuCapture` 增加 `l1-bag*` 四张（seed 了道具/队伍）。
