# pokemon-builder

RPG Maker XP / Pokemon Essentials 工程自动构建工具。

- 输入:RMXP / Pokemon Essentials 工程(只读,绝不修改)。
- 输出:审计报告(Markdown + JSON)、中间数据(Debug IR)、构建日志。
- 远期:Java / libGDX Runtime,产出 Windows PC 包与 Android APK。

## 当前进度

按 `project1/task-flow.md` 的阶段推进,当前进度见该文件的「阶段总览」。
第一阶段(2026-10-02):目录树、`builder-config.json`、README / architecture 文档,
`build-tool/` 旧代码迁移至 `builder/src/`。
第二阶段(2026-10-02):Builder CLI 与 `builder.bat` 入口,子命令全部就位,
尚未实现的流水线阶段明确报 `NOT IMPLEMENTED`(退出码 2),不假装成功。
第三阶段(2026-10-02):项目扫描器。`tools/scanner/` 读取 `Data/*.rxdata`
(Ruby Marshal 4.8)与 `Scripts.rxdata`(zlib 解压),修复了阶段 1 迁移的
`marshal.js` 的 4 个解析缺陷(短负数 long 编码、类名/ivar 名按符号节点读取、
Float/Bignum/字符串参与链接表、`I`/`C` 节点);
`builder.bat audit` 的 `[2/4]` 步骤已接入真实扫描并输出 `build/reports/scan.json`;
真实参考工程 `E:\仓库\范例\929` 541 个 `.rxdata`、384 段脚本(18 万行)全部解析成功。
44 项自动测试通过。
第七阶段(2026-10-02):一键审计入口 `scripts/audit-project.bat`。封装层负责环境检查
(Node / `builder.bat` / CLI / 配置)、`chcp 65001`、参数透传与退出码原样返回,
审计逻辑复用阶段 2-6 的 5 步流水线,不引入第二套解析。真实工程实测:541 个 `.rxdata`、
384 段脚本(18 万行)、5,440 个 script block 一次跑通(约 0.6 s),5 个 Markdown + 
`build/reports/script-audit.json` 全部生成;缺工程、缺 `Data/`、损坏 `.rxdata` 均立即 
`BUILD FAILED`(退出码 1)。85 项自动测试通过。
第八阶段(2026-10-02):`tools/data-converter/` + `scripts/build-data.bat`。CLI `build-data` 变为
3 步真实实现(校验 → 扫描 → 转换),把工程转成 JSON Debug IR 写入 `generated/`:
`project.json`、`maps/`(每地图一个文件,事件/页面/命令全量,355/655 带 `scriptBlockId`)、
`events/`、`common-events/`、`scripts/`(段清单 / 脚本块 / API 用量)、
`metadata/`(build / sources(sha1) / problems)。真实工程一次输出 1,221 个文件(约 68 MB),
失败即 `BUILD FAILED` 且不写半成品。105 项自动测试通过。

第九阶段(2026-10-02):`scripts/build-pc.bat` + `builder/src/runtime.js`。CLI `build-pc` 变为 4 步真实流水线:
`[1/4]` 校验 → `[2/4]` 审计 → `[3/4]` 转换(写 `generated/` JSON Debug IR)→ `[4/4]` 检查 Java Runtime。
审计与转换复用阶段 2-8 的同一实现,任一步失败立即 `BUILD FAILED`、退出码 1。
PC Runtime(Java / libGDX)尚未实现,因此数据三步成功后输出 `DATA BUILD SUCCESS` +
`PC Runtime is not implemented yet.` + `Skipping packaging.`,退出码 2,`dist/` 不写任何文件——
不假装产出 PC 游戏。真实工程实测(541 `.rxdata`、384 段脚本、5,440 block → 1,221 个 JSON 约 68 MB,
本机无 Java Runtime),约 5.6 s,退出码 2。122 项自动测试通过。
第十阶段(2026-10-03):`scripts/build-android.bat` + `scripts/build-android-legacy.bat`。CLI 两个命令变为
5 步真实流水线:`[1/5]` 校验 → `[2/5]` 审计 → `[3/5]` 编译游戏数据(`generated/` Debug IR)
→ `[4/5]` 构建 Android Runtime → `[5/5]` 打包 APK;前三步复用阶段 2-8 的同一实现。
两个目标**不共用构建配置**:Modern(API 21+、`armeabi-v7a`/`arm64-v8a`/`x86`/`x86_64`、
Gradle `:android:assembleRelease`、`dist\PokemonGame-Android.apk`)与
Legacy(API 14+、仅 `armeabi-v7a`、Gradle `:android-legacy:assembleRelease`、
`dist\PokemonGame-Android-Legacy.apk`)只共用 `generated/` 游戏数据。
Android Runtime 未实现:数据成功后按目标输出 `Android Runtime is not implemented yet.` /
`Legacy Android Runtime is not implemented yet.` + `Skipping packaging.`,退出码 2,`dist/` 不写文件;
步骤 4 同时报告 Java Runtime 与 Gradle 探测结果。138 项自动测试通过。
第十一阶段(2026-10-03):`scripts/clean-build.bat`(7 个一键 `.bat` 全部就位)。封装层只做环境检查与
`call "%BAT%" clean %*` 参数透传,删除逻辑仍只在 CLI 一处:默认清空 `build/`、`generated/`、
`logs/build-temp/`(删后重建目录与 `.gitkeep`),绝不删原始 RMXP 工程、`docs/`、`dist/`;
`--all` 追加 `dist/` 与归档日志(保留 `latest.log`)。每个目录都过 `isInside()` 守卫,
任一步失败即 `BUILD FAILED`、退出码 1。142 项自动测试通过。

第十二阶段(2026-10-03):`scripts/test-builder.bat` 一键测试入口(7 个一键 `.bat` 全部就位)。
封装层只做环境检查(Node / `builder.bat` / CLI / 配置 / `builder\tests`)与 `cd /d` 到 builder 根,
然后运行 `node --test "builder/tests/*.test.js"`;额外参数原样透传给 `node --test`
(如 `--test-name-pattern=<正则>` 或单个测试文件),node 的退出码经 `exit /b %STATUS%` 原样返回——
任何一项测试失败都会得到 `TEST FAILED` 与非 0 退出码,不存在「测试失败但构建成功」。
新增 `builder/tests/path-handling.test.js`(9 项,全部经 `cmd.exe` 实跑 `.bat`):
空格 / 中文 / 日文 / 空格+中文+日文工程路径跑通 audit 与 build-data,不存在路径与损坏 `Data/` 文件
立即 `BUILD FAILED`(退出码 1)并带文件名,无 PBS 目录只告警不失败,无第三方插件时
`plugin sections: 0` 且全部分类为核心 API;另新增 `builder/tests/test-builder.test.js`(6 项)
覆盖 test-builder.bat 的成功 / 失败 / 安装不完整 / 参数透传 / 原始工程只读路径。157 项自动测试通过。
第十三阶段(2026-10-03):日志补全 + 增量构建缓存 + 错误处理收口。
日志(`builder/src/logger.js`)新增 `output()`:每个输出文件(审计报告、扫描 / 事件 JSON、`generated/` 下 IR、
`build/cache.json`)都以 `[OUTPUT]` 逐条写入 `logs/latest.log` 与时间戳日志(只进日志,不刷控制台);
原有「命令 / 参数 / 项目 / 版本 / 步骤 / 警告 / 错误 / 退出码 / 耗时」保持不变。
增量构建(新模块 `tools/data-converter/cache.js`):转换成功后写 `build/cache.json`,记录全部输入的指纹
(sha1 + bytes + mtime)、每个单元的依赖与产物;再次构建逐单元比对,输入不变且产物仍在则复用
(连文件都不重写),只改一个地图就只重建该地图,其余单元原样保留。缓存格式 / 版本 / 工程路径不匹配、
JSON 损坏或 `--no-cache` → 全量重建(缓存只加速,永不损害正确性);已不属于工程的 IR 文件按文件清理,
`generated/` 内用户文件保留。错误处理:全仓 `catch` 审计,PBS 目录不可读与输出目录不可写
(如 `generated/` / `docs/` / `build/cache.json` 位置是文件)全部判失败并写日志,禁止「失败但构建成功」。
真实工程实测:首跑 2.5 s,二跑 524 个单元全部复用、只重写 7 个聚合文件、0.9 s;`clean-build.bat`
清掉 `build/`(含缓存)后下一跑自动回到全量。175 项自动测试通过。

第十四阶段 / R14(2026-10-04):真实构建流水线(新模块 `builder/src/gradle.js` / `runtime-data.js` /
`build-report.js` + `commands.js` 重写)。`build-pc` 与 `build-android*` 都变成 8 步真实流水线
(project3 §63):`[1/8]` 审计 → `[2/8]` 地图转换 → `[3/8]` 事件编译 → `[4/8]` 脚本转译(IR)
→ `[5/8]` 音频编译 → `[6/8]` 运行时数据校验 → `[7/8]` Gradle 构建
(PC:`agentCheck` 运行时测试 + `lwjgl3:dist`;Android:`<module>:assembleDebug`)
→ `[8/8]` 打包(校验产物、写 `generated/build-report.json`,§55 全字段)并输出 `… BUILD SUCCESS`;
任何一步失败立即 `… FAILED` + 退出码 1,绝不假装成功(「not implemented / Skipping packaging」路径已删除)。
Windows 细节:`.bat` 包装经 `cmd.exe` 调用;工程路径含中文时自动 `subst` 到空闲盘符(ASCII)后再跑 Gradle
(规避 Gradle 测试 worker `@argfile` 的路径编码问题);`JAVA_HOME` 取自 Java Runtime 探测结果;
`build-pc --runtime-image <dir>`(或 `POKEMON_RUNTIME_IMAGE`)让 jpackage 捆绑该运行时
(供没有 `jmods/` 的 JDK)。`build-android` 打包 debug APK(Technical Preview,`:android:assembleDebug`),
`build-android-legacy` 用 `:android-legacy:assembleDebug`;两者只共享 `generated/` 游戏数据。
真实工程实测:build-pc 数据侧 522 地图 / 8835 事件 / 5441 脚本块(3712 转译)、音频复用 2411、
`agentCheck` + `lwjgl3:dist` 全绿,`dist/` 产物与 `build-report.json` 落地。234 项自动测试通过。

第十五阶段 / R15(2026-10-04):自动测试体系(`tests/fixture-project/` + `builder/tests/fixture-project.test.js`
+ runtime 的 `pokemon.runtime.fixture` 无头测试)。Small Test Project 是两张地图的合成 RMXP 工程
(NPC 对话+开关/变量、门传送、二选一、公共事件、`pbItemBall(:POTION,3)`、未支持脚本、BGM/SE),
`generate.mjs` 确定性重建(含提交的 `generated/`;Node 测试做 golden 漂移检查)。Node 4 项:
fixture 形状 / §58 IR / §59 未支持块带位置且发布失败 / golden 漂移。Java 8 项:Golden
(起点/门目标/NPC/选项/公共事件/物品/碰撞)与 Headless Smoke(全部事件页跑完)。
顺带修复解释器对 355+655 脚本块的双执行 bug。238 项自动测试通过。

第十六阶段 / R16(2026-10-04):agentCheck + CI + 6 份文档。
`gradlew agentCheck`(= Builder 全套 + `:core:test` + `core:auditCoreDependencies`,不依赖 Android SDK)
与 `scripts/ci-build.bat`(五阶段链:Builder Tests → Compiler Tests → Core Tests → Desktop Build
→ Android Debug Build;无 SDK 时第 5 阶段明确 SKIPPED)已就位,`.github/workflows/stage2-ci.yml`
提供 GitHub Actions 版本。文档:`docs/runtime-architecture.md` / `script-compiler.md` /
`event-runtime.md` / `desktop-build.md` / `android-build.md` / `stage2-report.md`(§83 结构)。
第十七阶段 / L1(2026-10-04,R17 后批次):菜单/标题/存读档界面 + 菜单皮肤。
`data-converter` 从脚本段「Modular Title Screen」提取 `generated/title.json`(splash 序列/时长/
标题 BGM);运行时新增 `ui/menu`(标题屏 → splash→提示→新游戏/继续/退出;暂停菜单 MPM 素材+
工程 windowskin+GUI 音效;存读档槽位 1/2/3/快速;设置音量/全屏写 `settings.json`)。
Builder **240** / Java **266**;顺带修 `TitleScreen` 缺 `resize()` 导致真实窗口全黑的 bug。

第十八阶段 / L3+L4(2026-10-04):Android 数据包/素材随包 + release 签名。
`builder/src/data-pack.js`(零依赖 ZIP 写入,26k 文件/54 秒,内容哈希版本)把
`generated/`+`Graphics/`+`Fonts/` 打成 `assets/runtime-data.zip`;core 新增
`DataPackUnpacker`(zip-slip 防护、版本标记)由 Android 启动器首启解包;
`GraphicsLocator` 优先读随包 `Graphics/Fonts`,桌面 `build-pc` 默认复制素材进
`dist/Windows/runtime-data/`(L4);`build-android --release` 用预览 keystore 签名,
`--no-data` 保持瘦 APK。Builder **246** / Java **272**。

第二十阶段 / L6(2026-10-04):未支持脚本消化(阶段 2 可做部分)。
`pbNoticePlayer`(324 条)→ `NOTICE_PLAYER`(面向检查+感叹号+玩家转身+事件走近);
`pbSave`(16 条)→ `SAVE_GAME`(静默写快速槽)。translated **3712→4052**、
unsupported **838→498**、有效覆盖 **84.6%→90.8%**;其余分类留阶段 3(战斗/背包/伙伴/
小游戏)与插件机制(事件变量、扩展选择);天气/`pbToneChangeAll` 属独立特性批次。
Builder **252** / Java **280**。

## 目录结构

```text
pokemon-builder/
├─ builder/

│  ├─ src/        # cli.js / commands.js / config.js / logger.js / project.js
│  │              # runtime.js(Java Runtime / Gradle / Android 目标探测)
│  │              # gradle.js / runtime-data.js / build-report.js(R14 构建流水线)
│  │              # marshal.js / util.js(阶段 1 迁移)
│  ├─ tests/      # 自动测试(node:test):单元 / 解析器 / 事件 / 脚本 / 路径 / 封装层
│  └─ config/     # 内部配置
├─ tools/
│  ├─ scanner/          # 工程扫描器(阶段 3):index.js + scan.js CLI
│  ├─ event-analyzer/   # Event 审计器(355/655 合并,阶段 4)
│  └─ script-analyzer/  # Script 分类与统计(阶段 5)
├─ scripts/       # 一键 .bat(audit-project / build-data / build-pc / build-android /
│                #   build-android-legacy / clean-build / test-builder / ci-build)
├─ tests/         # R15 Small Test Project fixture(project/ + generated/ + generate.mjs)
├─ generated/     # 中间数据 / Debug IR(阶段 8):project.json + maps/ events/ common-events/ scripts/ metadata/
├─ build/         # 构建缓存(reports/、cache.json 增量缓存,阶段 13)
├─ dist/          # 最终产物(PC 包 / APK)
├─ logs/          # latest.log + 时间戳日志 + ci-build.log
├─ docs/          # 审计报告 + runtime-architecture / script-compiler / event-runtime /
│                #   desktop-build / android-build / stage2-report
├─ builder.bat    # 统一入口
└─ builder-config.json
```

## 使用方法

```bat
scripts\audit-project.bat "D:\Game\PokemonProject"
scripts\build-data.bat "D:\Game\PokemonProject"
scripts\build-pc.bat "D:\Game\PokemonProject"
scripts\build-android.bat "D:\Game\PokemonProject"
scripts\build-android-legacy.bat "D:\Game\PokemonProject"
scripts\clean-build.bat
scripts\test-builder.bat
scripts\build-data.bat "D:\Game\PokemonProject" --no-cache
builder.bat audit "D:\Game\PokemonProject"
builder.bat build-data "D:\Game\PokemonProject"
builder.bat build-pc "D:\Game\PokemonProject"
builder.bat build-android "D:\Game\PokemonProject"
builder.bat build-android-legacy "D:\Game\PokemonProject"
builder.bat clean [--all]
```

命令(项目路径也可来自 `builder-config.json` 的 `source.rmxpProject`,命令行参数优先):

| 命令 | 说明 |
|---|---|
| `audit <project>` | 审计工程(扫描数据 / 事件 / 脚本 / API) |
| `convert <project>` | 转换为中间数据(Debug IR) |
| `validate <project>` | 校验工程是否可处理 |
| `build-data <project>` | 校验 + 扫描 + 转换为 `generated/` JSON Debug IR |
| `build-pc <project>` | PC 构建(R14:8 步真实流水线;`agentCheck` + `lwjgl3:dist` → `dist\PokemonGame.jar` 与 `dist\Windows\` 自包含应用 + `generated\build-report.json`) |
| `build-android <project>` | Android 构建(R14:8 步;Modern,API 21+ / 4 ABI / `:android:assembleDebug` / `dist\PokemonGame-Android.apk`,Technical Preview) |
| `build-android-legacy <project>` | Android 构建(R14:8 步;Legacy 占位,API 21+ / 4 ABI / `:android-legacy:assembleDebug` / `dist\PokemonGame-Android-Legacy.apk`,Technical Preview) |
| `clean [--all]` | 清理构建产物(绝不触碰原始工程;`--all` 追加 `dist/` 与归档日志) |
| `help` | 显示帮助 |

选项:`--project <path>` 覆盖配置中的工程路径;`--all` 让 `clean` 同时清理
`dist/` 与 `logs/*.log`;`--no-cache` 让 `build-data` / `build-pc` / `build-android*` 忽略
`build/cache.json` 全量重建(仍是正确构建,只是不跳过未变化单元);
`--runtime-image <path>`(仅 `build-pc`)让 jpackage 捆绑该 JDK/JRE 作为游戏运行时,
用于没有 `jmods/` 的 JDK;不传则用当前 JDK 的 jlink 裁剪。

也可以直接调用 Node:`node builder\src\cli.js audit "D:\Game\PokemonProject"`。

扫描器还可以单独运行(不经过 builder.bat):

```bat
node tools\scanner\scan.js "D:\Game\PokemonProject" --json build\reports\scan.json
```

`--json` 写出机器可读扫描结果;`--quiet` 只输出问题与最终状态行。
退出码:`0` 扫描成功,`1` 存在无法读取的数据或无法解压的 Script 段(不会静默跳过)。

## 退出码

- `0` 成功。
- `1` 错误(参数错误、校验失败、IO 失败,或构建流水线的任一步失败——失败的那一步会明确打印)。

## 运行测试

一键运行整个套件(任何一项失败即 `TEST FAILED` 与非 0 退出码):

```bat
scripts\test-builder.bat
```

也可以直接调用 Node:

```bat
node --test "builder/tests/*.test.js"
```

test-builder.bat 会把额外参数原样透传,便于只跑子集:

```bat
scripts\test-builder.bat --test-name-pattern=spaces
scripts\test-builder.bat builder\tests\cli.test.js
```

当前共 238 项(R14 新增:gradle.js 9、runtime-data 5、build-report 3;R15 新增 fixture 4;
CLI 与封装层测试改写为真实流水线行为):CLI 行为(33,含增量缓存复用 / `--no-cache` / 输出目录不可写硬失败)、
Ruby Marshal 读取器(16)、项目扫描器(9)、事件分析器(18)、脚本分析器(14)、报告生成器(5)、
Debug IR 转换器(20,含 8 项增量缓存)、Runtime 探测(17)、Logger(3:latest.log 内容 / 耗时退出码 / 多运行不串档)、
路径与编码(9,全部经 `cmd.exe` 实跑:空格 / 中文 / 日文 / 混合路径,不存在路径、损坏 `Data/`、
无 PBS 目录、无第三方插件)、test-builder.bat 封装层(6)、
批处理封装层(25,真实调用 `cmd.exe` 执行 `audit-project.bat` / `build-data.bat` / `build-pc.bat` /
`build-android.bat` / `build-android-legacy.bat` / `clean-build.bat`,含二跑缓存复用)。

## 硬性原则

1. 不修改原始 RMXP 工程;原始项目只作为输入。
2. 所有生成文件输出到独立目录。
3. 不允许静默忽略无法识别的 Ruby Script,必须明确报告位置。
4. 所有工具支持命令行调用,Windows 用户通过 `.bat` 一键运行。
5. 路径包含空格 / 中文 / 日文时必须正常;`.bat` 必须检查错误码。
6. 任一步骤失败必须停止后续构建,错误全部写入日志。

详见 `docs/architecture.md`。