# Architecture

Pokemon Builder 架构说明(第一阶段 - 第十一阶段)。

## 总体数据流

```text
RMXP / Pokémon Essentials 工程(只读输入)
   │  Data/*.rxdata, PBS/, Graphics/, Scripts.rxdata
   ▼
Scanner(阶段 3)
   │  Ruby Marshal 解析(.rxdata)+ zlib 解压(Scripts.rxdata)
   ▼
Event Analyzer(阶段 4)
   │  MapXXX.rxdata / CommonEvents.rxdata,合并 355/655 为完整 Ruby block
   ▼
Script Analyzer(阶段 5)
   │  分类:ESSENTIALS_API / PLUGIN_API / RMXP_GLOBAL /
   │        SIMPLE_EXPRESSION / COMPLEX_RUBY / UNKNOWN
   ▼
Reports(阶段 6)
   ├─ docs/*.md             人类可读报告
   └─ build/reports/*.json  机器可读(审计产物)
   ▼
Debug IR Converter(阶段 8)
   │  复用阶段 3/4/5 的分析结果;地图数据再解析一次写 IR
   ▼
generated/(JSON Debug IR)
   ├─ project.json                    清单 + 计数
   ├─ maps/map-NNN.json               每地图一个文件(事件/页面/命令全量)
   ├─ events/map-NNN.json             合并后的 Ruby script blocks
   ├─ common-events/common-event-NNN.json
   ├─ scripts/{sections,blocks,apis}.json
   └─ metadata/{build,sources,problems}.json
   ▼
(远期)Java / libGDX Runtime → dist/(PC 包 / Android APK)
```

## 模块划分

- `builder/src/cli.js` — 进程入口。定位 builder 根目录,调用 `runCli`,以退出码结束。
- `builder/src/commands.js` — 命令分发与各命令处理器。
  - 退出码约定:`0` 成功;`1` 错误(参数 / 校验 / IO);`2` 阶段未实现。
  - 未实现的阶段必须明确打印 `NOT IMPLEMENTED (Phase N)` 并返回 2,严禁假装成功。
  - `build-pc` / `build-android` / `build-android-legacy` 先执行已实现的前置步骤
    (工程校验),再输出 `Runtime is not implemented yet.` 与 `Skipping packaging.`。
  - `clean` 只允许删除 builder 根目录内的 `build/`、`generated/`、
    `logs/build-temp/`;`--all` 追加 `dist/` 与 `logs/*.log`(保留当前运行的日志);
    `isInside()` 守卫保证绝不触碰原始工程、`docs/`、`scripts/`;
    删后重建目录与 `.gitkeep`,下一步构建立即可用。
- `builder/src/config.js` — 读取 `builder-config.json`,合并命令行覆盖
  (如位置参数工程路径),解析输出目录;配置缺失 / 非法 JSON 抛 `ConfigError`。
- `builder/src/logger.js` — 控制台 + `logs/<时间戳>.log` + `logs/latest.log`
  三写一;记录命令、参数、工程路径、Builder 版本、步骤、错误、耗时。
- `builder/src/runtime.js` — 阶段 9 Java Runtime 探测(只读):
  `detectJavaRuntime(builderRoot, { run, env })` 依次检查
  ① 随工具分发的 `<builder 根>/runtime/java`、② `JAVA_HOME`(存在 `bin/java.exe`
  才计入,指向缺失 JDK 时记入 `skipped` 而不谎报)、③ `PATH`,
  对每个候选执行 `java -version` 取版本号;`probeJavaExecutable()` 的 `run`
  可注入,因此测试不依赖机器是否装了 JDK;`formatRuntimeStatus()` 输出单行摘要。
  探测只读、不启动长驻进程、找不到运行时不单独失败——由调用方决定动作
  (`build-pc` 跳过打包并明说)。阶段 10 的 Android 构建复用同一探测。
- `builder/src/project.js` — 只读工程校验:存在性、`Data/`、`Game.rxproj`、
  `builder/src/runtime.js` 另有 `ANDROID_TARGETS`(阶段 10):Modern(API 21+、4 个 ABI、
  Gradle 模块 `:android`、产物 `PokemonGame-Android.apk`)与 Legacy(API 14+、仅 ARMv7、
  Gradle 模块 `:android-legacy`、产物 `PokemonGame-Android-Legacy.apk`)两份**独立**目标描述;
  二者只共用 `generated/` 游戏数据,`androidTarget(legacy)` 取其一,
  `formatTargetStatus()` 打印目标、API 级别、ABI、Gradle 模块与产物名。
  `detectGradle()` 依次探测 builder 根 `gradlew(.bat)` 与 PATH 上 `gradle --version`,
  探测与 Java 一样可注入、只读、失败不单独中断构建(由调用方报告并跳过打包)。
  `PBS/`(Essentials 数据)、`Graphics/`;缺失必须项记 error,缺 PBS / Graphics 记 warning。
- `builder/src/marshal.js` — Ruby 1.8 / RGSS Marshal 流读取器,仅支持 RGSS
  产生的节点类型,其他类型抛出 `MarshalError`(带偏移量),绝不静默跳过。
  阶段 3 验证真实工程时修复了 4 个缺陷:
  1. `long()` 短负数:字节 128..251 是「负 Fixnum」(值 = 字节 - 251),
     旧代码误判为多字节长度,导致 `Map001.rxdata` 起全部文件解析脱同步;
  2. 类名与实例变量名是完整符号节点(`:` 定义 / `;` 引用),旧代码按裸索引读;
  3. 链接表:字符串 / 浮点 / Bignum 也占用链接槽(旧代码漏登记);
  4. Float 节点为 `f` + 长度 + ASCII(NUL 结尾),`I`(IVAR)与
     `C`(UCLASS,Essentials 的 `PBAnimations`/`PBAnimation`)旧代码不支持。
  符号统一解码为名字字符串;字符串按 latin1 保字节,文本由上层按 UTF-8 解释。
- Hash 辅助:`hashEntries()` 把 Marshal Hash 还原为 `[key, value]` 对
  (RMXP 的 `RPG::Map#events`、`MapInfos.rxdata` 都带非字符串键)。
- `builder/src/util.js` — UTF-8 / latin1 回退解码、zlib inflate、sha1、行数统计。
- `tools/scanner/index.js` — 阶段 3 扫描器(`scanDataFiles` /
  `readScriptsSection` / `scanProject` / `formatScanSummary`):
  枚举 `Data/*.rxdata`,逐文件 Marshal 解析并按文件名归类
  (map / mapInfos / commonEvents / system / tilesets / database);
  `Scripts.rxdata` 逐段 zlib 解压,无法恢复的段记入 `problems[]`
  (段 index + 名称 + 原因),不静默忽略;无法读取的数据 → `ok=false`。
- `tools/scanner/scan.js` — 扫描器 CLI:`node tools/scanner/scan.js <项目> [--json 文件]`,
  退出码 0/1,绝不写入被扫描工程。
- `tools/event-analyzer/index.js` — 阶段 4 事件分析器
  (`analyzeProjectEvents` / `analyzeMapData` / `analyzeCommonEventsData` /
  `extractScriptBlocks` / `formatEventSummary`):遍历 `MapXXX.rxdata` 与
  `CommonEvents.rxdata`,把 Event Command `355`(Script)连同后续连续 `655`(续行)
  合并成完整 Ruby block,每条记录含 Map ID / Map Name / Event ID / Event Name /
  Page / PageIndex / Trigger / Command Index / continuations / lines / characters /
  `rubySource`;CommonEvent 没有 page 概念,统一记 page=1 并用 `switchId` 记触发开关;
  事件按数值 id 排序输出;合并规则取自工程解释器 `command_355`(每 chunk 补 `\\n`,
  后续 `655`/`355` 均算续行);孤立 `655` 与 `parameters[0]` 非字符串的 `355` 记入
  `problems[]`(带 `file` + location)并令 `ok=false`,绝不静默跳过。
- `tools/event-analyzer/analyze.js` — 分析器 CLI:
  `node tools/event-analyzer/analyze.js <项目> [--json 文件] [--list N] [--map ids] [--quiet]`,
  退出码 0/1;`--map` 只筛地图(调试用),此时跳过 CommonEvents 并给 warning。
- `tools/script-analyzer/index.js` — 阶段 5 脚本分类与统计:
  以 `Scripts.rxdata` 符号表(`def` / `class` / `module` / 全局赋值,由 `readScriptSources`
  提供源码)为词表,把每个事件脚本块分类为 `ESSENTIALS_API`(`pb*` / `PB*` / `PokeBattle_*` …,
  或由核心区段定义)/ `PLUGIN_API`(仅插件 / 注入区段定义,含插件自带的 `pbCrystalWarp`
  这类 pb* 方法)/ `RMXP_GLOBAL`(`$game_*`、`Sprite`、`Tone` …)/ `SIMPLE_EXPRESSION` /
  `COMPLEX_RUBY` / `UNKNOWN`。
  核心/插件边界取第一个带插件标记的段(版本号 / `插件` / CJK 分隔线),边界与核心段索引
  都写进结果,便于人工核对;局部变量、`@ivar`、`A::B` 限定常量、`:SYMBOL` 不参与分类;
  无法识别的标识符进 `unresolvedIdentifiers`(带位置),绝不静默忽略。
  统计口径:`occurrences`(每次调用)/ `blocks` / `maps` / `uniquePatterns`
  (去重后的参数形态,迁移工作量按它计算)。
- `tools/script-analyzer/analyze.js` — 分析 CLI:
  `node tools/script-analyzer/analyze.js <项目> [--json 文件] [--top N] [--api 名字] [--strict]`,
  退出码 0/1;`--api` 打印单个 API 的全部参数形态与样例位置,`--strict` 把未解析标识符
  升级为失败。
- `tools/report-writer/index.js` — 阶段 6 报告生成器:
  `writeAuditReports()` 一次性写 `docs/source-audit.md` / `script-event-audit.md` /
  `api-usage.md` / `plugin-usage.md` / `unsupported-scripts.md` 与
  `build/reports/script-audit.json`;`isComplexBlock()`(控制流 / ≥3 行 / ≥2 次续行)
  决定哪些 block 需要人工移植。JSON 是转译器的程序输入,
  每条记录带 map / mapName / event / eventName / page / commandIndex / type /
  method / argumentsShape / rubySource;Markdown 只是人类报告。
  **只写这 5 个报告文件,绝不覆盖手写的 `architecture.md`**(测试已锁住)。
- `audit` 命令流程:`[1/5]` 校验 → `[2/5]` 扫描 → `[3/5]` 事件 → `[4/5]` 脚本分类 →
  `[5/5]` 报告;任一步失败即 `BUILD FAILED` 并非 0 退出,全部成功输出
  `AUDIT SUCCESS` + `BUILD SUCCESS`。
- `scripts/` — 阶段 7-11 的 6 个一键 `.bat`(`audit-project` / `build-data` / `build-pc` / `build-android` / `build-android-legacy` / `clean-build`;第 7 个 `test-builder.bat` 属阶段 12);`builder.bat`(根目录)定位 CLI、自动创建
  输出目录、`%~1` 安全传参、透传 Node 退出码。
  - `scripts/audit-project.bat`(阶段 7):一键审计入口。只做环境检查(Node /
    `builder.bat` / CLI / `builder-config.json` 存在)、`chcp 65001`,然后
    `call "%BAT%" audit %*` 透传参数(工程路径可含空格 / 中文 / 日文,也可省略而用
    `source.rmxpProject`,或写 `--project <path>`),以 `exit /b %errorlevel%` 原样返回
    退出码。封装层不写文件、不删文件、不解析工程数据——审计逻辑只在 CLI 一处实现,
    避免出现第二套解析口径;任一步失败由 CLI 负责 `BUILD FAILED` 并非 0 退出。
  - `scripts/build-data.bat`(阶段 8):一键数据构建入口,写法与
    `audit-project.bat` 完全一致,只是 `call "%BAT%" build-data %*`。
    产物写入 `generated/`,Markdown 报告仍由 `audit-project.bat` 生成。
  - `scripts/build-pc.bat`(阶段 9):一键 PC 构建入口,封装层同样只做环境检查
    (Node / `builder.bat` / CLI / `builder-config.json`)、`chcp 65001` 与
    `call "%BAT%" build-pc %*` 参数透传,以 `exit /b %errorlevel%` 原样返回
    退出码(0 / 1 / 2);不写文件、不删文件、不解析工程数据。
  - `tools/data-converter/index.js` — 阶段 8 Debug IR 转换器
    (`convertProject` / `buildMapIr` / `buildCommonEventIr` / `irValue` /
    `ownBlocksForCommonEvent` / `formatConvertSummary`):
    校验 → 复用 `scanProject` / `analyzeProjectEvents` / `readScriptSources` /
    `analyzeScriptUsage` → 逐地图解析并写 IR。每地图一个文件(Map 粒度,
    阶段 13 增量构建的单位);事件命令全量保留,355/655 留在原位置并带
    `scriptBlockId` 指向合并后的 Ruby block(续行也算同一 block);
    Ruby 字符串按 UTF-8 解码(latin1 回退),Marshal 对象转成 `{class, ...ivars}`;
    `metadata/sources.json` 记录 `.rxdata` / PBS 的 sha1 与字节数(增量缓存种子),
    `metadata/problems.json` 汇总所有不可读 / 不可解释 / 未解析内容(带位置)。
    写盘前只清空 `generated/` 下自有的 5 个子目录(不删用户文件),并拒绝把 IR
    写进原始工程目录(除非位于 builder 根内,即 `pokemon-builder/generated/`)。
  - `tools/data-converter/convert.js` — 转换器 CLI:
    `node tools/data-converter/convert.js <项目> [--out 目录] [--quiet]`,退出码 0/1。
  - `build-data` 命令流程:`[1/3]` 校验 → `[2/3]` 扫描 → `[3/3]` 转换;
    成功输出 `BUILD DATA SUCCESS` + `BUILD SUCCESS`,失败输出 `CONVERT FAILED` +
    `BUILD FAILED` 并非 0 退出。
  - `build-pc` 命令流程(阶段 9):`[1/4]` 校验 → `[2/4]` 审计 → `[3/4]` 转换 →
    `[4/4]` 运行时检查。四个步骤全部复用既有实现:`runAuditPipeline()`
    (扫描 / 事件 / 脚本分类 / 报告,与 `audit` 同一个函数)、`runConvert()`
    (与 `build-data` 同一个函数,转换器内部步骤标签可经 `stepLabels` 覆盖,
    于是 `[2/3]`/`[3/3]` 在 build-pc 里显示为 `[3/4]`)、`detectJavaRuntime()`。
    任一步失败立即 `BUILD FAILED`、退出码 1,不产出半成品 IR,也不会走到打包。
    PC Runtime(Java / libGDX)尚未实现:数据三步成功后输出 `DATA BUILD SUCCESS`,
    紧接着 `PC Runtime is not implemented yet.` + `Skipping packaging.`,
    退出码 2(`EXIT_NOT_IMPLEMENTED`),`dist/` 不写任何文件,
    绝不假装产出 PC 游戏。
  - `build-android` / `build-android-legacy`(阶段 10):5 步流水线
    `[1/5]` 校验 → `[2/5]` 审计 → `[3/5]` 编译游戏数据 → `[4/5]` 构建 Runtime →
    `[5/5]` 打包 APK。前三步与 `audit` / `build-data` 共用同一实现;步骤 4 报告
    `detectJavaRuntime()` 与 `detectGradle()`,并打印 `formatTargetStatus()`;
    成功后按目标输出 `Android Runtime is not implemented yet.` /
    `Legacy Android Runtime is not implemented yet.` + `Skipping packaging.`,
    退出码 2,`dist/` 不写任何文件。
  - 两个 Android 目标不共用构建配置(测试已锁住):Modern = API 21+、ABI
    `armeabi-v7a` / `arm64-v8a` / `x86` / `x86_64`、Gradle `:android:assembleRelease`、
    产物 `dist\PokemonGame-Android.apk`;Legacy = API 14+、仅 `armeabi-v7a`、
    Gradle `:android-legacy:assembleRelease`、产物 `dist\PokemonGame-Android-Legacy.apk`。
    二者只共用 `generated/`(同一次 `build-data` 产物);Runtime 描述、API 级别、ABI、
    Gradle 模块、产物名全部不同。
  - `scripts/build-android.bat` / `scripts/build-android-legacy.bat`(阶段 10):
    两个独立一键入口,封装层写法与 `audit-project.bat` 完全一致,只做环境检查、
    `chcp 65001`、`call "%BAT%" build-android %*`(或 `build-android-legacy %*`)
    与 `exit /b %errorlevel%`;不写文件、不删文件、不解析工程数据。
  - `scripts/clean-build.bat`(阶段 11):一键清理入口,同样只做环境检查与
    `call "%BAT%" clean %*` 透传(`--all` 一并透传),`exit /b %errorlevel%` 原样返回;
    封装层自身不删任何文件——删除逻辑只在 CLI `cmdClean` / `removeAndReset()` 一处,
    每个目录先过 `isInside()` 守卫(不在 builder 根内直接抛错停止),
    因此原始 RMXP 工程、`docs/`、`dist/`、`scripts/`、源码都不会被误删。

## 关键设计

- **路径安全**:`.bat` 用 `%~1` 并全程引号;`%~dp0` 定位脚本自身;
  `chcp 65001` 保证中文 / 日文路径可见;Node 侧统一 `path` 模块。
- **错误处理**:无法读取 RMXP Data、无法解析 Event、发现不支持的 Script、
  缺少必须资源、输出目录不可写、转换器异常 → 一律失败并写日志;
  禁止 catch 后忽略并继续。
- **日志**:`logs/latest.log`(当前运行)+ `logs/<YYYY-MM-DD_HHmmss>.log`(归档)。
- **增量构建预留**:`build/cache.json` 保存 hash / mtime / dependency;
  架构按 Map 粒度组织,未修改的 Map 不重编译(阶段 13 细化)。
- **机器可读优先**:下游转译器只消费 JSON(`build/reports/*.json`、
  `generated/**`),Markdown 仅作人类报告;`generated/` 的 IR 带 `format` 版本标记,
  后续二进制格式可在不改流水线的前提下替换这些文件。

## build-tool 整合结论(阶段 1)

原 `build-tool/src/{marshal.js,util.js}` 已**迁移**至 `pokemon-builder/builder/src/`,
原 `build-tool/` 目录已删除。理由:需求规定实现目录为 `pokemon-builder/`,
保留两处副本会造成双源不一致;迁移后导入冒烟测试通过。

## 阶段路线图

| 阶段 | 内容 | 状态 |
|---|---|---|
| 1 | 基础目录与配置 | 已完成(2026-10-02) |
| 2 | Builder CLI 与 builder.bat | 已完成(2026-10-02) |
| 3 | 项目扫描器(.rxdata 解析) | 已完成(2026-10-02):541/541 文件解析,384 段脚本,44 项测试 |
| 4 | RMXP Event 审计器(355/655 合并) | 已完成(2026-10-02):5,440 blocks / 8,783 chunks,62 项测试 |
| 5 | Script 分类与 Unique Pattern 统计 | 已完成(2026-10-02):5,440 blocks,135 API / 143 pattern,76 项测试 |
| 6 | 审计报告输出(Markdown + JSON) | 已完成(2026-10-02):5 个 md + script-audit.json,80 项测试 |
| 7 | audit-project.bat | 已完成(2026-10-02):`scripts/audit-project.bat` 复用 5 步流水线,真实工程 0.6 s 跑通,失败路径 exit 1 |
| 8 | build-data.bat 与中间数据 | 已完成(2026-10-02):`tools/data-converter/` + `scripts/build-data.bat`,522 maps / 130,608 commands → 1,221 个 JSON(68 MB),105 项测试 |
| 9 | build-pc.bat | 已完成(2026-10-02):`builder/src/runtime.js` + `scripts/build-pc.bat`,CLI `build-pc` 4 步流水线(校验/审计/转换/运行时检查)复用既有实现,数据成功后 `DATA BUILD SUCCESS` + `PC Runtime is not implemented yet.` + `Skipping packaging.`(退出码 2),122 项测试 |
| 10 | build-android.bat / build-android-legacy.bat | 已完成(2026-10-03):两个独立入口 + CLI 5 步流水线(前三步复用阶段 2-8);Modern(API 21+/4 ABI/`:android`)与 Legacy(API 14+/ARMv7/`:android-legacy`)只共用 `generated/` 数据,配置互不相同;数据成功后按目标 `Android Runtime is not implemented yet.` + `Skipping packaging.`(退出码 2),138 项测试 |
| 11 | clean-build.bat | 已完成(2026-10-03):scripts/clean-build.bat 透传 uilder.bat clean;默认清理 uild/、generated/、logs/build-temp/(重建目录),--all 追加 dist/ 与归档日志;isInside() 守卫 + 任一步失败退出码 1,142 项测试 |
| 12 | test-builder.bat 与自动测试 | 待开始 |
| 13 | 日志 / 增量构建预留 / 错误处理 | 待开始 |
| 14 | 完成标准验证与最终报告 | 待开始 |