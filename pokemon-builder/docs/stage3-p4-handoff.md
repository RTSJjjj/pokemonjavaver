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
  - **铁律：禁止自造**——界面、面板、提示文字、坐标、控件一律照原工程脚本库插件（含脚本传入的 `starthelptext`、窗口皮肤等）；插件没有的先问，不自作主张。自造过的已删除（`TrainerSetupView`、背包/宝可梦菜单的自制帮助窗与操作提示等）。
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
  - 口袋图标：`icon_pocket`（28×28，`blt(2,2,src((pocket-1)*28,0,28,28))` 在 `(0,-3)` 的 186×32 sprite 上；`icon_pocket` 只有 8 列，pocket 9 越界不画）；口袋名 `PokemonBag.pocketNames[pocket]` 画在 `(110,186)`；
  - **口袋表对齐插件**：`BagModel.POCKETS[0]=""`、只循环 **1..9**（`pbPocketNames` 第 0 项为空、`numPockets=9`）；运行时原来的 `"其它"` 口袋是自造的，已删（pocket 0 的道具不再出现在任何栏）；
  - 物品窗 `Window_PokemonBag` `x=218-32+64=250, y=-8, w=326+32=358, h=80+9*32=368`，`ITEMSVISIBLE=9`、行高 32；**`windowskin=nil` ⇒ SpriteWindow 边框 32、`startX/Y=16`，内容原点 `(266,8)`**，`drawItem` 再 `+16/+16`，故：光标 `Bag/cursor` 在 `(266+12, row+1)`、图标 `Icons/item%03d` 48×48 缩 24×24 在 `(266+16, row+22)`、名字 `(266+44, row+20)`、数量 `x%3d` 右缘 `266+326-16=576`、`icon_register` 在 `266+326-64`；重要道具（`pocket==8` 或 `fieldUse==4`）不显示数量、显示 `icon_register`；末行 `关闭背包`；
  - `Bag/cursor.png` 是 **314×68**、圆角框在 `y=16..53`（上下各 16 透明）；`drawCursor` 是 `pbCopyBitmap(contents, cursor, rect.x+12, rect.y+2)` **整图拷贝**，透明边把框居中在行上（只取上半会削一半、只画 `src(0,16,..)` 会偏高）；按原工程 3× 截图核算后：光标整图放在 `rowTop-1`，行文字再 `+1px`（名字/数量 `rowTop+21`、图标 `rowTop+23`）正好居中且不压到下一行；
  - 右侧滚动条 `icon_slider`（`534+64=598`，上/下箭头 + 滑块，多于 9 行时）；
  - 选中道具 48×48 图标 `(48,height-48)` Center 原点、说明文字框 `(84,334)`⇒内容 `(100,350)` `564×128`（`ITEMTEXTBASECOLOR 248,248,248`/阴影 `90,90,90`）；左上 `[Z]:手动 [Shift]:自动` 用**小字**（`PauseMenuOverlay` 新增 size 14 的 `smallFont`）右对齐 `(108,8)`。
- **选项窗体照插件**：`BW Bag` 的 `pbShowCommands` 用项目 windowskin 的窗口；`BagView.drawAction` 改画右侧命令窗（`使用/给予/丢弃/取消`，`▶` 光标）+ 底部消息窗 `已选择<道具名>`，不再是自绘面板。
- `BagModel`：`cursor` 可见 9 行、多一行 `关闭背包`（`selected()` 在末行返回 null → 关闭背包）。
- 机制：`使用 / 给予 / 丢弃(remove 1) / 取消`（`ItemUse`/`PartyModel.giveItem`），未移植登录/整理（插件 `A/SHIFT` 排序）——需要再说。
- `PauseMenuOverlay` 加 `openAt(action)` 便于截图/测试直接进入某子界面；`MenuCapture` 增加 `l1-bag*` 四张（seed 了道具/队伍）。

### 9.8 宝可梦菜单对齐 `PScreen_Party`（进行中）
- `PartyView` 重写为 `PScreen_Party` 的 `PokemonParty_Scene#pbStartScene` 版式：
  - 背景 `Pictures/Party/bg` + `bg2`（各 672×448，bg2 z+1）；
  - 选择底板 `PokemonPartySelectionBackgroundPanel`：`Pictures/Party/panel_pok_base_bg`（430×88）在 `((width-430)/2, 228+64)=(121,292)`；
  - 6 个槽 `PokemonPartySelectionPanel`：`slot_width=72`、`start_x=(width-430)/2=121`、`y=226+64=290`；每槽：阴影 `Party/shadow`(32×14) 中心 `(x+24, y+64)`、宝可梦图标 `Icons/icon%03d[+s]`（`PictureOrigin::Center`，`oy=h*5/8`）在 `(x+36, y+52)`、携带道具 `Icons/item%03d` 在 `(x+40, y+56)`、选中箭头 `Party/arrow_normal`(20×12) 在 `(x+24, y)`、状态 `Pictures/statuses`(44×16, `src(0,16*status,44,16)`) 在 `(x+12, y+12)`；
  - 详情 `PokemonPartyDetailsPanel`：`self.y=height/2-128=96`；对战图（`Battlers/%03d[+s]`）中心 `(width/2, 126)`；HP 条 `Party/overlay_hp`(96×6, `src(0,hpzone*6,hpw,6)`) 在 `(91+offset, 250)`，`offset=width/2-96=240`；名字居中 `(-90+offset,15)`、等级右对齐 `262+offset+32`、HP 文本右对齐 `130+offset`、性别图标 `(152+offset,18)`；
  - 底部帮助窗（`bw choice` skin）。
- 交互保留：`←→/↑↓` 移动光标、`Z` 打开 `查看详情/交换位置/取下道具/返回`、`X` 返回；交换/取下道具沿用 `PartyModel`。
- 详情分栏（`pbSetSmallFont` overlay2）：`属性:`(4,56) + 属性图标 `Pictures/types`(64×28, `src(0,type*28,..)`) 在 `(4,80)/(68,80)`、形态 `(4,116)`、`性格:`(4,142)、`特性:`(4,168)、`个体:`(4,194) + 6 个 `Summary/RatingS..F`(16×20) 于 `xs=[56,71,86,132,101,116],y196`、`招式:`(232+offset+42=514,86) + 招式(512,110+24i)；底部**白底帮助窗**显示脚本传入的 `starthelptext`（`选择宝可梦或取消`；交换时 `移动到哪里？`），**右下角 `取消` 按钮** = `PokemonPartyCancelSprite`：`Pictures/Party/icon_cancel`(134×42) 在 `(378+64+64, 330+64)=(506,394)`，`取消` 文字在按钮相对 `(55,8)` 右对齐（白/阴影 `132,132,132`）——不要自造的操作提示。`pbDrawTextPositions` 的 align 实测映射是 **1=右、2=中、3=左**：名字**右对齐** `(-90+offset=150,15)`、**性别图标** `(152,18)`（x **不加** offset，20×20）、等级**左对齐** `(262+offset+32=534,18)`、HP **居中** `(130+offset=370,256)`。
- **仍待补**：`[F]:寄存系统`、消息窗(`bw choice` 皮肤，`Graphics/Windowskins` 里没有该文件，待确认用哪个)。`MenuCapture` `l1-party*` 两张。

### 9.9 宝可梦详情页对齐 `BW PScreen_Summary`
- 新增 `SummaryView`（`PartyView` 的"查看详情"进入），照插件 `PokemonSummary_Scene`：
  - 背景：滚动 `Summary/background`（512×512 平铺，`ox=6`→`+=1`/帧、`oy=-36`→`+=1`/帧、各 mod 512）；页底板 `Summary/bg_<1..5>`（蛋 `bg_egg`）672×448；
  - 立绘 `Battlers/%03d` Center 原点 `(460+64,208+32)=(524,240)`；携带道具 `Icons/item%03d` 48×48 **Center** 原点 `(552+96,360+64)=(648,424)`；
  - 共用头：精灵球 `Summary/icon_ball_00`(32×32) `(486,44)`、状态 `Pictures/statuses`(44×16, `src(0,16*status,44,16)`) `(540,88)`、闪光 `Pictures/shiny`(20×20) `(514,364)`、页名 `(26,8)` 白/`132,132,132`、名字 `(520,46)`、等级 `(506,84)`、`携带道具` `(462,389)`、道具名 `(462,418)`、性别 `♂/♀` 文本 `(646,46)`（蓝 `0,0,214`/`15,148,255`、红 `198,0,0`/`255,155,155`）、标记 `Summary/markings`(16×16×6) `(576,370)`；
  - 页1：`图鉴ID(34,68)/种族名称(34,102)/形态(34,134)/属性(34,166)/训练家(34,198)/ID No.(34,230)`，值 `x=164`（形态 `166`）；`属性` 图标 `Pictures/types`(64×28) `(164,164)/(232,164)`；`亲密度(34,262)`；`经验值(34,292)` 值居中 `(215,324)`、`下个等级所需要的经验(34,356)` 值居中 `(177,388)`；经验条 `Summary/overlay_exp`(128×6, 宽=小数×128 取偶) `(140,424)`；
  - 页2：`drawFormattedTextEx((22,64),宽300)` 备忘录（性格行 蓝 `0000D6/7394FF`、其余灰 `404040/B0B0B0`）；
  - 页3：6 行 `HP/攻击/防御/特攻/特防/速度` 行 y `100/130/162/194/226/258`，列值**居中** 能力值 `268+88`、努力 `194+84`、个体 `152+58`、种族 `152+10`；个体评级 `Summary/RatingS..F`(14×20) `x=110+128`；`总和(16,290)`、`特性(12,326)` 名居中 `240`、描述 `drawTextEx((12,352),282,3)`；HP 条 `Summary/overlay_hp`(96×24, 区 `*8`) `(32,80)`；性格影响行阴影色 升 `206,148,156`/降 `148,148,214`；
  - 页4：招式行 y 从 `82` 步进 `64`：属性图标 `(32,y+2)`、名字 `(100,y)`、`PP(126,y+32)`、PP 值**右对齐** `(244,y+32)`；空招 `-(100,y)`/`--(226,y+32)`；
  - 页5：`缎带数量(38,303)`、数量**右对齐** `(157,334)`、缎带图 `Pictures/ribbons`(64×64, `src(64*(id%8),64*(id/8))`) `(2+68*(c%4),74+68*(c/4))`；
  - 蛋页 `drawPageOneEgg`：球 `(518,44)`、`面板(26,8)`、名字 `(424,46)`、`道具(366,322)`、道具名 `(290,350)`/`无(360,350)`、备忘 `(10,82)`。
- 操作：`←→` 翻页(1..5)、`↑↓` 换队伍成员、`X/B` 返回（蛋不翻页）。
- **各页功能（照插件）**：
  - 页1-3 `Z` → `pbOptions` 命令窗（`pbShowCommands`，`pbBottomRight` 贴右下角）：`携带道具`/`拿回道具`(有道具时)/`标记`/`取消`。`携带道具` → 背包"为某只宝可梦选道具"模式（`BagView.chooseHold`，插件的 `pbChooseItemScreen`，经 `PauseMenuOverlay.openBagForHold` 回跳）；`拿回道具` → 放回背包；`标记` → `pbMarking`（`overlay_marking` 210×268 @`(260,88)`、6 标记 `(300+58*(i%3),154+50*(i/3))`、`cursor_marking` @`(284+58*(i%3),144+50*(i/3))`、OK `(284,244)`/取消 `(284,294)`、文字右对齐 `x=366`）。
  - 页4 `Z` → `pbMoveSelection`：`bg_movedetail`、招式列表 `yPos=98` 步进 `64`（属性图 `(310,y+2)`、名 `(380,y)`、`PP` `(380,y+32)`、值右缘 `492`、分类图 `(310,y+32)`）、`招式/威力/命中/标签` `(26,8)/(20,122)/(20,154)/(20,186)`、威力/命中居中 `(216,122)/(216,155)`、标签图 `(120,188)` 步进 26（`b/e` 是**没有才显示**）、描述 `((4,220),宽230,5行)`、`cursor_move` `(286,91+64*i)`、`pokeicon` `(46,92)`；`↑↓` 换招式、`Z` 选/换位、`X` 返回。
  - 页5 `Z` → `pbRibbonSelection`：`overlay_ribbon` `(0,280)`、名 `(30,286)`、描述 `((30,318),480)`、`cursor_ribbon` `(0+68*(i%4),72+68*(i/4))`、上下箭头 `(260,56)/(260,260)`；`↑↓←→` 移、`Z` 换位、页滚动 `ribbonOffset`。名字/描述来自新移植的 `pokemon/Ribbons`（`PBRibbons` 全表）。
- **已补 P0 字段**（`Pokemon` + 存档往返）：`personalID`/`publicID`/`otGender`/`ballused`/`markings`/`obtainMap`/`obtainLevel`/`obtainMode`/`obtainText`/`pokerus`。页1 的训练家名/ID/颜色、球图、标记、Pokerus、页2 获取信息、特性（`personalID%6`）都按插件。
- **缎带 id 口径**：插件是 int（`PBRibbons` 常量，1-based），本工程事件/测试存的是字符串 → 页面只解析**数字字符串**为 id（其余跳过）。若要完全一致需把 `Pokemon.ribbons` 改 `IntArray` 并改事件/存档/测试。
- 顺带修复：队伍屏状态图路径 `Pictures/Party/statuses` → 插件全局 `Pictures/statuses`，且画在**详情面板** `(self.x+12, self.y+12)=(348,108)`（不是每个槽）；行索引改成插件的 `status-1`（0 睡/1 毒/2 烧/3 麻/4 冰、7 濒死）。
- **待补**：页1-3 选项里的 `查看图鉴`（工程未建模 `$Trainer.pokedex`）。`MenuCapture` 增 `l1-summary-1..5`、`l1-summary-{ribbon,move,move2,options,hold,marking}`。

### 9.10 图鉴主列表对齐 `PokedexMain BW Style`（进行中）
- `PokedexView` 重写为 `PokemonPokedex_Scene` 的主列表：
  - 背景滚动 `Pokedex/bg_list`（ScrollingSprite，1px/帧）+ `Pokedex/list_overlay`（整屏，自带 9 行行框/黄色球槽/滑轨/"Enter: INFO" 提示）；
  - 列表窗 `Window_Pokedex.new(184*2=368, 94, 276, 344)`（`windowskin=nil` ⇒ 边框 32、内容原点 `(384,110)`、行高 32、可见 9 行）：光标 `Pokedex/cursor_list`(244×44) 整图放 `(384, 110+row*32)`；拥有 → `Icons/icon%03d` 缩 32×32 于内容 `x=7`（绝对 391）、已见未拥有 → `Pokedex/icon_seen`(28×28) 于内容 `x=11`（绝对 395）；文字 `%04d name`（未见 `%04d ----------`）在内容 `x=52`（绝对 436）`y=row+6`，色 `222,222,222`/`132,132,132`；
  - 选中立绘 `Battlers/%03d` Center 原点 `(110+64,196+32)=(174,228)`；未拥有→灰(`0.72` 近似 `Tone` 去色)、未发现→全黑剪影；
  - 顶部信息：`宝可梦图鉴(18+64,8)`、物种名居中 `(114+64, 332+64)`、`发现的：(30+32,57)` 右缘 `220+32`、`拥有的：(280+32,57)` 右缘 `470+32`；滑轨 `Pokedex/icon_slider`(40 宽) `x=468+32+128`、上/下 `(628,118)/(628,307)`、滑块 `(40,0/8/24)` 按 `top/(total-9)`；
  - `↑↓` 移光标（整行滚动、对齐 baked 行框）、`Z` 进条目、`X` 退出。
- 口径：插件 `pbGetDexList` 用 `$Trainer.formlastseen` **过滤未发现**，工程无该表 → 这里列出**全部**物种（未见显示 `----------`，与 `drawItem` 的 else 分支一致）。
- **条目页（`PokedexEntry BW Style`）已做**：`DexEntryView`，5 页 Info/Data/Evolution/Forms/Area，`←→` 翻页、`↑↓` 换物种、`C` 形态页换形态、`X` 返回：
  - 背景滚动 `Pokedex/bg_<info|data|forms|area>` + `Pokedex/<info|data|evo|forms|map>_overlay`；立绘 `Battlers/%03d` Center `(98+64,112)`；
  - 页1：`{0001} {名字}(392,23)`、`{种类}宝可梦` 居中 `(484,58)`、`身高(398,138)/体重(398,168)` 值右缘 `574`、条目文字 `((39,280),592,4行)`、体形 `icon_shapes`(60×60, `src(0,(shape-1)*60)`) `(324,100)`、已拥有 `icon_own`(28×28) `(325,19)`、属性 `icon_types`(72×32) `(394,97)/(461,97)`；未见显示 `？？？宝可梦`/`???.?`；
  - 页2：`数据(88,4)`、`{名}-{形态}` 右缘 `w-66`；小字(20) `特性` 居中 `(100,40)`、`特性n:..(24,60/80/100)`、`隐藏特性:`、`种族值总和` 居中 `(362,40)`、`HP/攻击/防御(280,60/80/100)`、`速度/特攻/特防(362,60/80/100)`、`野生持有物` 居中 `(546,40)`、`100%/50%/5%/1%` `(476,60/80/100)`；招式选择器 `◀[A]:..(w/2-112,122)`/`【当前】`居中/`[S]:..▶(w/2+112,122)`、列表 `xs=[24, 24+w/3, 24+2w/3]`、`ys=[144,166,188,210,232,254,276,298,320,340]`（先天/升级/机器/蛋；工程无 TM/机器表 → 机器招式显示"没有可学习的学习器招式"）；
  - 页3：`进化成谁` 居中 `(w/2,54)` + `{方法}{参数}→{目标}` `xs=[16,w/2+16]`、`ys=[96,118,...,316]`；
  - 页4：`形象(88,4)`、`{名} 居中(352,72)`、`{形态名}(352,104)`、`[C]:切换形态 [Z]:切换{超闪/异色}` `(w/1.6,4)`、普通/异色立绘 `(158,272)/(510,272)`；
  - 页5 分布：工程未建模地区地图/分布图 → 显示插件兜底 `overlay_areanone` `(140,148)` + `栖息地不明` 居中 `(336,152)` + `地区`/`{名}的分布`。
  - 口径：`height/weight` 直接用工程 PBS 值（PBS 已是米/kg，如 `0.7`/`6.9`），不用插件的 `/10`（插件的 PBS 是分米/百克）。
- **仍待补**：图鉴搜索模式（`Input::A` → `pbDexSearch`）；分布页地区地图；`PokedexMenu BW Style`（多地区选择）。`MenuCapture` 增 `l1-dex*` / `l1-dex-entry1..5`。
