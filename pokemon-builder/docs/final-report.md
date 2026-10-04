# 最终报告 · 阶段 14 完成标准验证

> 由阶段 14 产出。验证对象：`pokemon-builder`（本目录）对真实工程 `E:\仓库\范例\929` 的完整表现。
> 验证环境：Windows + PowerShell，Node.js v24.16.0；本机未安装 Java Runtime 与 Gradle（见 NOT TESTED）。

## IMPLEMENTED

- 根入口 `builder.bat`：自动定位 CLI / 检查 Node / 自动创建 `logs build generated dist` / `%~1` 安全传参 / 子命令失败返回非 0 退出码。
- `scripts/` 一键批处理 7 件套（均 `chcp 65001` + 存在性检查 + `call` + `exit /b %errorlevel%` 透传退出码）：
  `audit-project.bat` / `build-data.bat` / `build-pc.bat` / `build-android.bat` / `build-android-legacy.bat` / `clean-build.bat` / `test-builder.bat`。
- Builder 源码：`builder/src/`（cli / commands / config / logger / marshal / project / runtime）+ `tools/`（scanner / event-analyzer / script-analyzer / report-writer / data-converter 含 cache）。
- 引擎产物：`docs/` 5 个 Markdown 报告、`build/reports/` 3 个 JSON 报告、`generated/` JSON Debug IR（522 个地图文件等 1,221 个）、`logs/` 日志与 `build/cache.json` Map 粒度增量缓存。
- 命令行子命令全部就位：`audit` / `convert` / `validate` / `build-data` / `build-pc` / `build-android` / `build-android-legacy` / `clean`。

## TESTED

- 自动测试：`scripts\test-builder.bat` 实跑 **175 项全部通过（fail 0），约 9.7 s，`TEST SUCCESS`，退出码 0**；覆盖单元 / 解析 / 事件扫描 / 脚本扫描 / 路径与编码 / 封装层批处理（全部经 `cmd.exe` 真实执行）。
- `clean-build.bat`：删除并重建 `build/`、`generated/`、`logs/build-temp/`，保留 `docs/`、`dist/` 与原始工程，退出码 0（`CLEAN SUCCESS`）。
- `audit-project.bat "E:\仓库\范例\929"`：`AUDIT SUCCESS` + `BUILD SUCCESS`，退出码 0。541/541 `.rxdata` 解析（map 522 / database 13 / scripts 1 / other 5）；`Scripts.rxdata` 384/384 段、180,629 行；522 地图 / 8,834 事件 / 13,279 page / 130,608 命令；合并 355/655 得 5,440 个 script block；分类 ESSENTIALS_API 4,949 / PLUGIN_API 465 / SIMPLE_EXPRESSION 26；151 个 API、157 个 unique pattern、**未解析标识符 0**。
- `build-data.bat "E:\仓库\范例\929"`：`BUILD DATA SUCCESS` + `BUILD SUCCESS`，退出码 0；一次写出 **1,221 个文件、68,265,059 字节**（`project.json` + `maps/` + `events/` + `common-events/` + `scripts/` + `metadata/`）；`build-pc` 二跑实测增量：`524 units reused, 0 rebuilt`。
- `build-pc.bat`：校验 → 审计 → 转换（`DATA BUILD SUCCESS`）→ 运行时探测后输出 `PC Runtime is not implemented yet.` + `Skipping packaging.`，**退出码 2**，`dist/` 无产物，不假装成功。
- `build-android.bat` / `build-android-legacy.bat`：数据三步入 `generated/` 后分别按 Modern（API 21+、4 ABI、`:android:assembleRelease`、`PokemonGame-Android.apk`）与 Legacy（API 14+、仅 ARMv7、`:android-legacy:assembleRelease`、`PokemonGame-Android-Legacy.apk`）配置停在打包前，各输出对应 `... Runtime is not implemented yet.` + `Skipping packaging.`，**退出码 2**；两目标配置经测试锁定互不共用。
- `builder.bat` 根入口：仓库根目录 `.\pokemon-builder\builder.bat validate` 退出码 0；从 `C:\` 等其他工作目录调用正常（`%~dp0` 定位）。
- 中文 / 日文 / 空格路径：除测试套件 9 项专项外，另行把真实工程 `Data` + `PBS` 复制到 `my 项目 プロジェクト v2` 目录实跑 `audit-project.bat` 与 `build-data.bat`，均 `BUILD SUCCESS`、退出码 0。
- 原始工程只读：逐轮 `.bat` 前后对原始工程（排除 `pokemon-builder/`、`project1/`）**25,162 个文件做 SHA1 快照对比，完全一致**。
- 失败路径（测试中真实执行）：工程不存在 / 缺 `Data/` / 损坏 `.rxdata` / 缺 `Scripts.rxdata` / 输出目录不可写 / PBS 不可读 → 立即 `BUILD FAILED` 或明确报错，退出码 1，不产出半成品。

## NOT TESTED

- **PC / Android 打包**：本机未安装 Java Runtime 与 Gradle，`build-pc` / `build-android` / `build-android-legacy` 的打包步骤（libGDX Core、`gradle assembleRelease`、产出 `.apk` / PC 包）按设计诚实跳过（退出码 2 + 明确提示），需在装有 JDK + Gradle 的用户本机由下一阶段实现后验证。
- 完整 Pokémon Essentials 迁移到 Java / 任意 Ruby → Java 编译：超出第一阶段范围，未实现也未测。
- 报告内容随工程变化的快照对比（本次以真实工程一次性生成为准）。

## KNOWN ISSUES

- 无阻塞性缺陷。以下为已知设计取舍：
  - 切换扫描的工程路径后，增量缓存因工程指纹不匹配会回退全量重建（预期行为，缓存只加速不破坏正确性）。
  - `generated/` IR 为 Debug JSON 格式，体量约 68 MB；最终 `.bin` 二进制格式留待后续阶段。
  - 构建产物（报告 / `generated/` / `logs/`）位于构建器目录内，反复构建会覆盖上一轮输出（`latest.log` 与时间戳日志除外，日志按时间戳归档）。

## NEXT PHASE

1. 基于 `build/reports/script-audit.json` + `generated/` IR 实现 Ruby → Java 转译器（从 157 个 unique pattern / 151 个 API 起步，事件脚本 5,440 块）。
2. 实现 libGDX Runtime 核心（精灵 / 地图 / 事件解释器），先打通 PC 目标，再接两个 Android 目标（Modern / Legacy 配置已就绪）。
3. 接入 Gradle：`build-pc` 产出桌面包，`build-android(-legacy)` 产出 `dist\PokemonGame-Android(-Legacy).apk`。
4. 将 Debug IR 升级为最终二进制格式，并利用 Map 粒度增量缓存加速迭代。
