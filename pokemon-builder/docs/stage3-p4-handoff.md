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
- **玩家角色链**（已做）：`GameState.playerId`（-1=空行走图），`pbGenderSelector→GENDER_SELECTOR`（当前兜底默认男，**完整选择画面 + 名字输入 + `WRITETRAINERNAME` 归 P4**），`pbChangePlayer(id)→CHANGE_PLAYER` 用 `project.json runtime.players[PlayerA..H]`。
- `subst X:` 会被清理；丢了重建。
- 改 `builder/tests/fixture-project` 影响的金牌输出：`node tests/fixture-project/generate.mjs`（**从 `pokemon-builder/` 根跑**）后提交。
- `build-pc`/`ci-build` 会重写 `docs/*-audit.md` 的时间戳行（可一并提交）。
- 仓库工作流：`E:\仓库\范例\929` 直接是 main 工作副本；GitHub Desktop 或 `git` 均可。

---

## 6. 测试与验收规范
- Builder：`builder/` 下 `node --test`（278 用例）。
- Java：`X:\runtime` 下 `gradlew :core:test`（331 用例）；无头优先，所有界面逻辑尽量抽成可单测的 model（参考 `PauseMenuModel`）。
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
| 计划/文档 | `docs/stage3-plan.md`, `docs/architecture.md`, `docs/event-runtime.md`, `docs/script-compiler.md` |

---

## 8. 交接检查表（开工前）
- [ ] 能 `scripts\ci-build.bat` 全绿（本地）。
- [ ] 能 `gradlew :lwjgl3:run` 看到开场（选角前空行走图，跑完变 `trchar000`）。
- [ ] 读通 `PauseMenuOverlay` 的 open/update/render/Host 模式。
- [ ] 明确 P4 子块顺序（建议 P4a 基座 → P4b/c/e 菜单 → P4f PC → P4d/g → P4h L15；P2d 可先或并行）。
