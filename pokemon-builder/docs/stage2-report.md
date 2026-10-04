# Stage 2 Report（阶段 2 报告）

> 结构按任务书 §83。状态时间：2026-10-04（R1–R17 完成；DoD 29 项全部通过）。
> 证据索引：`project1/task-flow-runtime.md` 变更记录、`project1/worklog-codex-2026-10-04.md`、
> `pokemon-builder/logs/backup-*`、`__r6-staging/`（探针与日志）。

## IMPLEMENTED

- **Builder（构建阶段）**
  - 扫描台：`tools/scanner`（Data/*.rxdata、Scripts.rxdata、PBS）、`tools/event-analyzer`
    （355/655 合并、类别/API 统计）、`tools/script-analyzer`、`tools/report-writer`（审计报告）。
  - 转换器：`tools/data-converter` → `generated/`（project/system/tilesets/animations/
    connections/maps/events/common-events/scripts/metadata），含 8 项增量缓存与全量重建。
  - 音频：`tools/audio-compiler`（内容判型、ffmpeg 转码、`audio-manifest.json` 2403 条）。
  - **脚本编译器**：`builder/src/script-compiler.js`（事件 Ruby → JSON IR；§58/§59 策略）。
  - 真正流水线：`build-pc` / `build-android` / `build-android-legacy` = 8 步
    （§63），任一步失败退出码 1；`generated/build-report.json`（§55 全字段）。
  - CLI/包装：`builder.bat` + `scripts/*.bat`（audit/build-data/build-pc/build-android*/
    audio/clean/test-builder/ci-build）。
- **Runtime（Java / libGDX）**
  - 模块：`core`（跨平台逻辑）、`lwjgl3`（Desktop）、`android`（API 21+）、
    `android-legacy`（占位）。
  - 地图：渲染（tileset 分页/自动元件/优先级/事件深度）、碰撞、相机、地图连接
    （无缝越界）、203/204/206 全景与雾、色调/闪光/震动、洞窟动画、格子动画（草丛等）、
    推石头/浮板、到达门队列。
  - 事件：101/102/106/111/115/116/121/122/123/201–210/221–225/231/232/235/241/242/
    245/249/250/355/655/509 全量；消息窗口（Essentials 皮肤、颜色、名字框、选项、
    光标）、移动路线全码表（含 5–8/10/11/13/16–26/14/41）、自开关/临时开关。
  - 状态：`GameState`（玩家/开关/变量/自开关/物品/任务/强度门控）+ JSON 存档
    （`SaveManager`，F5/F9）。
  - 菜单/标题（L1，2026-10-04）：标题屏（splash 序列 →「按下空格开始游戏」→
    新游戏/继续/退出，`generated/title.json` 数据驱动）、暂停菜单（训练家/保存/
    读档/设置/回到标题/退出游戏，MPM 背景+滚动 panorama+图标+工程 windowskin 与
    GUI 音效）、存读档槽位（1/2/3/快速，显示玩家名/地图/时间）、设置
    （BGM/SE/BGS 音量 + 全屏，`settings.json` 持久化）。
  - 音频：BGM/BGS/ME/SE、cry 解析、BGM 过场 1 秒交叉淡出。
- **桌面交付**：Runnable JAR（16.5 MB）+ jpackage app-image
  （`dist/Windows/`：exe + app/ + runtime/ + runtime-data/，无需安装 JDK）。
- **Android 交付（L3/L4）**：fat **debug** APK 482.9 MB 与 **签名 release** APK
  481.9 MB（预览 keystore；数据+素材随包，首启解包）、瘦 debug APK（`--no-data`，
  4.4 MB，配 `dist/PokemonGame-Android-data.zip` 外部投递）、`android-legacy`
  debug 占位 APK（Technical Preview，四 ABI，含 gdx + freetype 原生库）。
- **测试/CI**：Small Test Project fixture + Golden + Headless Smoke；Builder 252 /
  Core 280；`gradlew agentCheck`（不依赖 Android SDK）；`scripts/ci-build.bat` +
  `.github/workflows/stage2-ci.yml`（Builder → Compiler → Core → Desktop → Android）。

## TESTED

- **自动测试**：Builder **252/252**；Core **280/280**（含 fixture 8 项）；
  `gradlew agentCheck`（X: 盘）BUILD SUCCESSFUL；`scripts/ci-build.bat` 五阶段链
  （沙箱实测）。
- **构建级 E2E（真实工程、中文路径）**：`build-pc`（522 地图 / 8835 事件 /
  IR 3712 转译 / 音频复用 2411 / agentCheck + dist → `PC BUILD SUCCESS`）；
  `build-android`/`build-android-legacy`（APK 5.3 MB，`ANDROID BUILD SUCCESS`）。
- **真窗口/像素**：R6.x 批次的大量 OpenGL 探针（对话框皮肤、草丛、动画、
  地图连接、门流程、移动路线）；打包 exe 12–15 秒稳定运行（IR 3712、672×448）。
- **L1 菜单/标题证据**（2026-10-04）：隐藏窗口 FBO 截图 12 张
  （`__r6-staging/l1-captures/`：标题 4 + 独立菜单 5 + 真 `MapScreen` 集成 3
  ——开菜单/读档页/读档后换屏）+ 真实可见窗口截屏（`run-l1-live.ps1`）；
  标题 splash/提示/命令、暂停菜单 6 条目、存/读档、设置、训练家各页已核对。
  期间抓到并修复三个真 bug：`TitleScreen` 缺 `resize()`（真实窗口全黑）、
  暂停菜单误用世界相机（游戏内全黑）、两处换屏生命周期隐患
  （overlay 内换屏后的绘制检查、标题屏帧末换屏）。
- **用户本机验收**：R3/R4/R5 与 R6.29（草丛）已确认；R6.30–R6.33 已确认
  （"没问题了"）；R12–R16 已复核（2026-10-04，A–F 全过；见 NOT TESTED 的
  Android 运行画面与 GH Actions 两项）。

## PARTIALLY IMPLEMENTED

- **邻居地图**：连接处只静态绘制邻居的瓦片与事件（不推进其自主移动/并行页）——
  R6.23 起已知边界，用户确认不做动画。
- **图形素材（L4 已完成）**：运行时优先读数据根下的随包副本（`Graphics/`+`Fonts/`）；
  `build-pc` 默认把两者复制进 `dist/Windows/runtime-data/`（增量），发行包不再依赖
  源工程（`--no-assets` 可跳过）。
- **音频（L8 + L8.1 已部署，试听待验收）**：24 首 BGM 带 `LOOPSTART/LOOPEND` 区间，构建
  切分为 **loop 段 + intro 段**（`looped=24 intros=24 / failed=0`；`file`=循环段、
  `introFile`=前奏段；ffprobe 校验两段时长与区间最大偏差 0.010s；详见
  `docs/audio-loop-regions.md`）。运行时**先播前奏一次、再整段无缝循环 loop 段**
  （不再裁掉前奏，也不做会静音 0.3–0.6s 的 seek 回环）。剩余：145 首 Ogg
  待定不猜标签、3 首接缝排除、4 首 MP3 未处理、30 个 MIDI 需外部合成
  （由用户转码后导入）。
- **存档**：F5/F9 快速槽与菜单多槽位（1/2/3/快速）均已可用；槽位列表显示
  玩家名/地图/时间；载入会按存档的地图与坐标换屏。
- **Android 数据/素材（L3/L4 已完成，待真机复核）**：`build-android` 默认把
  `generated/`+`Graphics/`+`Fonts/` 打成 `assets/runtime-data.zip`（APK 内不重复压缩），
  启动器首启解包到外置 files 目录（版本标记，内容哈希变化才重解）；另有
  `dist/PokemonGame-Android-data.zip` 外部投递与 `--no-data` 瘦 APK（adb push）
  两种方式；release 变体用预览 keystore 签名（`--release`，正式分发前需替换）。
  真机安装与运行画面待用户复核。
- **sign `\op/\cl`**：牌子皮肤有静态行数，滑动动画未做（全工程 0 处使用）。
- **菜单皮肤**：消息/选项/名字框（R6.31）与 L1 的菜单态（暂停菜单 MPM 素材 +
  工程 windowskin）已完成；其余菜单域（图鉴/宝可梦/背包等）随阶段 3。

## NOT IMPLEMENTED

- **宝可梦域（阶段 3）**：战斗系统、招式/特性、背包/存储、训练家、野外遭遇、捕获、
  经验、进化、队伍；`$Trainer`/`$PokemonBag` 等 891 个块登记为
  `java-handler-required`。
- **触摸 UI（§51）**：Virtual DPad / A-B / 多点触控属独立任务；当前只有抽象与
  键盘适配。
- **Legacy API 14 实机后端**：`android-legacy` 是占位（受当前 libGDX 后端 minSdk 21
  限制），需要旧后端另立批次。
- **通用 Ruby→Java 编译器**：按 §84 **明确不做**。

## NOT TESTED

- **Android 运行画面（等用户真机复核）**：L3/L4 数据包与素材已随 APK 分发，
  首启自动解包；沙箱无设备，安装后的实际画面（标题屏/地图/菜单）需用户在新
  APK 上复核。旧版黑屏原因（未打包数据/素材）已由 L3/L4 解决。
- **GitHub Actions 的真实仓库执行**：workflow 已提交，需真实仓库运行一次
  （本地 `scripts/ci-build.bat` 已通过）。
- **L1 菜单/标题真机验收（待用户）**：沙箱截图与两套测试已全绿；请在桌面版
  按 X/Esc 开暂停菜单、走一遍保存/读档/设置/回标题/退出，并确认标题屏
  splash 序列与 BGM（`title_hgss_0`@20）。

## KNOWN ISSUES

1. **中文路径 + Gradle 测试 worker**：`@argfile` 编码不一致导致 `ClassNotFoundException`；
   解决：ASCII 盘符（build-pc 自动 `subst`）或 `-Dfile.encoding=GBK`（详见
   `desktop-build.md`）。
2. **`dist/` 体积**：`runtime-data/` 是 `generated/` 的完整副本（约数百 MB）。
3. **R6.25 边界**：建图首帧 49–72 ms 由传送淡出掩盖；邻居预载 10–39 ms。
4. **Android debug 签名**：Technical Preview 用 debug keystore；发布签名未配置。
5. **`stripDebugDebugSymbols` 提示**：无 NDK 时原生库原样打包（调试无影响）。
6. **234（更改图片色调）** 只告警并继续；工程内 0 处使用。

## UNSUPPORTED SCRIPT PATTERNS

- 统计（真实工程，L6 消化后 2026-10-04）：脚本块 5441；**translated 4052 /
  javaHandlerRequired 891 / unsupported 498**（有效覆盖 90.8 %；L6 前为
  3712 / 891 / 838，84.6 %）。
- **UNSUPPORTED（498）**：无法静态解释、且不属于已识别域名的模式；L6 逐类消化后
  剩余部分集中在**阶段 3 域**（战斗 `setBattleRule`/`pbRockSmashRandomEncounter`、
  背包 `pbDeleteItem`、队伍/缎带、伙伴/跟随、小游戏、场景构造）与插件私有机制
  （扩展 `pbMessage` 选择 + `$PokemonGlobal.eventvars` 事件变量、`@ch_cmd`
  解释器数组）；另有天气（17）与 `pbToneChangeAll`（4）属可做的独立特性批次。
  发布构建必须失败并给出行级位置
  （`docs/unsupported-scripts.md`、`build/reports/script-coverage.json`）。
- **JAVA_HANDLER_REQUIRED（891）**：宝可梦域/战斗域 API，例如 `pbSet`/`pbGet`
  （`$Trainer` 循环）、`pbStoreItem`/`$PokemonBag`、`pbShowMap`、`pbPokeCenterPC`、
  `pbSetPokemonCenter`、`pbRegisterPartner`、`pbGenPkmn` 与全部战斗类
  （`build/reports/java-handler-required.json`）——阶段 3 的输入清单。
- 运行时**不运行 Ruby、不依赖 RGSS**（§84）；Ruby 只存在于源工程与构建阶段。

## NEXT STAGE

1. **阶段 3 · Pokémon Domain**：战斗/招式/特性、背包与存储、训练家、野外遭遇、
   捕获、经验、进化、队伍；把 `java-handler-required` 的 891 块逐批落地。
2. **触摸系统**：Virtual DPad / A-B / 多点触控（基于 `GameAction`/`InputManager`）。
3. **菜单 UI**：暂停菜单、存/读档界面、菜单皮肤。
4. **打包完善**：图形/音频数据随包分发（无源工程可玩）、发布签名、
   `jlink` 完整路径回归。
5. **PBS 数据完整化**：encounters/trainers/items 等（随宝可梦域需求）。
