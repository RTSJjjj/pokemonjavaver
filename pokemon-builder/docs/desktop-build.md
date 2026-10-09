# Desktop 构建与打包

> 对应任务书 §38–§47、§53、§63。实现：`runtime/lwjgl3/build.gradle`、
> `builder/src/gradle.js`、`scripts/build-pc.bat`、`scripts/ci-build.bat`。

## 1. 命令

```bat
REM 运行
cd pokemon-builder\runtime
gradlew.bat lwjgl3:run                     REM 开发运行（参数见下）

REM 打包（R12）
gradlew.bat lwjgl3:dist                    REM Runnable JAR + dist\Windows 自包含应用（无控制台窗口）
gradlew.bat lwjgl3:dist -Pconsole          REM 开发者：保留调试控制台（看 stdout 日志）
gradlew.bat lwjgl3:dist -PruntimeImage="C:\jre"  REM JDK 无 jmods/ 时直接捆绑该运行时

REM 全流水线（R14，含数据/音频/测试）
scripts\build-pc.bat "D:\Game\PokemonProject"
scripts\build-pc.bat --runtime-image "C:\jre"
```

## 2. Gradle 任务

| 任务 | 产物 | 说明 |
|---|---|---|
| `runnableJar` | `runtime/lwjgl3/build/dist/PokemonGame.jar` | `Main-Class` + 全部运行时依赖与原生库（约 16.5 MB），`java -jar` 即玩 |
| `jpackageImage` | `runtime/lwjgl3/build/jpackage/PokemonGame/` | JDK 自带 jpackage，`--type app-image`（exe + `app/` + 捆绑运行时）；默认 jlink 裁 `java.base,java.desktop,java.logging,jdk.unsupported`；`-PruntimeImage=<目录>` 跳过 jlink 直接捆绑现成运行时；R17 起**默认不带 `--win-console`**（双击 exe 只出游戏窗口），`-Pconsole` 保留调试控制台 |
| `dist` | `pokemon-builder/dist/` | `PokemonGame.jar` + `dist/Windows/`（exe + `app/` + `runtime/` + `runtime-data/`＝`generated/` 副本） |
| `captureMap` | 截图 | 开发用：离屏渲染一张地图（探针） |

`dist/Windows/` 的布局即任务书 §47：用户**无需安装 Java**。

## 3. 启动器与数据定位

`Lwjgl3Launcher`：

```text
PokemonGame [runtime-data-root] [mapId] [--fullscreen|--size WxH|--logical WxH|--no-vsync|--resizable]
```

- 逻辑分辨率默认取工程 `project.json.runtime.screenWidth/Height`（本工程 672×448），
  窗口按屏幕取最大整数倍（F11 全屏、F5 快速存档、F9 读取）。
- **启动流程（L1）**：不给 `mapId` 时先进入标题屏（splash → 提示 → 新游戏/继续/退出）；
  给 `mapId` 时直进该地图（探针/测试口径）；地图内 X/Esc 打开暂停菜单，菜单里含
  保存/读档/设置（音量、全屏，写 `settings.json`）/回到标题/退出游戏。
- **打包形态**：双击 exe 时工作目录不一定是 exe 目录；启动器经 `jpackage.app-path`
  在 exe 旁找 `runtime-data/`（或 `generated/`），命中后作为显式数据根；显式参数优先。
- **无控制台启动**：R17 起 `dist/Windows/PokemonGame.exe` 默认是 GUI 子系统的
  window-only 启动器（`jpackage` 不再带 `--win-console`）。旧包或想绕过 exe 时用
  捆绑运行时直接跑 fat JAR，同样没有终端窗口：

  ```bat
  cd pokemon-builder\dist\Windows
  runtime\bin\javaw.exe -jar app\PokemonGame.jar runtime-data
  ```

  `javaw` 丢弃 stdout：未捕获异常仍写 `%USERPROFILE%\pokemon-runtime-crash.log`；
  要看运行期日志就重新打包时加 `-Pconsole`。
- `RuntimeDataLocator` 解析顺序：显式参数 → `POKEMON_RUNTIME_DATA` →
  `-Dpokemon.runtime.data` → CWD/`generated` → CWD/`runtime-data`。
- **图形素材（L4）**：运行时优先读数据根下的随包副本（`runtime-data/Graphics` +
  `runtime-data/Fonts`），没有时才回退源工程 `Graphics/`（`project.json.source.project`）。
  `build-pc` 的打包步骤默认把 `Graphics/`+`Fonts/` 复制进 `dist/Windows/runtime-data/`
  （增量：大小+时间未变则跳过；`--no-assets` 可跳过以加快本地迭代），
  因此发行包不依赖源工程。

## 4. build-pc 流水线（§63 八步）

```text
[1/8] Audit              校验 + 扫描数据/事件/脚本 + 报告
[2/8] Convert Maps       RMXP 数据 → 地图 IR
[3/8] Compile Events     事件 IR
[4/8] Translate Scripts  scripts/ir.json（translated/unsupported 计数）
[5/8] Optimize Audio     generated/audio + manifest（增量复用）
[6/8] Validate Runtime Data  project.json / ir.json / audio-manifest 校验
[7/8] Gradle Build       agentCheck（运行时测试）+ lwjgl3:dist
[8/8] Package            校验 dist 产物 + 写 generated/build-report.json（§55 全字段）
```

任何一步失败立即退出码 1，不输出 BUILD SUCCESS。`--runtime-image` /
`POKEMON_RUNTIME_IMAGE` 会透传 `-PruntimeImage`。

## 5. Windows 细节

- **中文路径**：Gradle 测试 worker 的 `@argfile` 由守护进程按 `file.encoding`
  （UTF-8）写、JVM 启动器按系统本地编码（GBK）读 → 中文路径下所有测试类
  `ClassNotFoundException`。两种可用方式：
  1. 从 ASCII 盘符运行：`subst X: "...\pokemon-builder"` 后 `cd /d X:\runtime`
     （`builder/src/gradle.js` 在 build-pc/build-android 里会自动 subst 并在结束后卸载）；
  2. `gradlew "-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=GBK" ...`。
  `runtime/settings.gradle` 在非 ASCII 根路径下会打印同样的提示。
- **`.bat` 只能经 `cmd.exe` 调用**（Node 直接 spawn `.bat` 会失败）——CI 脚本与
  `gradle.js` 均已处理。

## 6. 验证证据（沙箱实测）

- `gradlew lwjgl3:dist -PruntimeImage=...` → BUILD SUCCESSFUL（27 s）；产物
  `dist/PokemonGame.jar` 16.5 MB + `dist/Windows/` 完整；打包 exe 从 `%TEMP%`
  零参数启动 15 秒稳定：`logical 672x448`、`script IR loaded: 3712`。
- `scripts\build-pc.bat` 真实工程 E2E：数据 522 地图 / 8835 事件、`agentCheck`
  （Builder 252 + core 280）→ `lwjgl3:dist` → `PC BUILD SUCCESS`；
  `generated/build-report.json` 字段完整。
- **jpackage/JDK 选择（L5，2026-10-04 修）**：`build-pc`/`gradlew lwjgl3:dist`
  在检测到的 JDK **没有 `jmods/`**（jlink 运行时，如某些启动器自带的 Java）时，
  自动把该运行时当作 `--runtime-image` 捆绑（无需再手传 `--runtime-image`）；
  有 `jmods/` 的完整 JDK 仍走 jlink 默认裁剪路径。显式参数/环境变量优先。
  另：Gradle 失败时构建日志会打印输出尾部（此前失败原因不可见）；
  `lwjgl3:dist` 在 jpackage 产物为空时拒绝 Sync（避免清空既有 `dist/Windows`）。
