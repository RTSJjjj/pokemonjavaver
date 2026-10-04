# pokemon-runtime

阶段 2 Runtime：libGDX 多模块工程（project3 §6）。

## 模块

```text
runtime/
├─ core/            逻辑与数据：GameDatabase / InputManager / AudioManager /
│                   SaveManager / ScreenManager / StoragePort（禁止依赖任何平台 API）
├─ lwjgl3/          Desktop 后端：Lwjgl3Launcher（窗口 / 全屏 / 分辨率 / VSync）
├─ android/         Modern Android 后端：AndroidLauncher（API 21+）
└─ android-legacy/  Legacy 占位模块：LegacyRuntime 复用契约 + AndroidLauncherLegacy
                    （真实 API 14 / ARMv7 后端属后续阶段，见模块 README）
```

版本集中管理：`gradle.properties` 的 `gdxVersion`（libGDX）与根 `build.gradle` 里的 AGP 版本。

## 首次构建（用户本机）

Gradle Wrapper 的二进制 `gradle/wrapper/gradle-wrapper.jar` 无法在此仓库中提交
（需由 Gradle 工具生成一次）。若 `gradlew` 报
`Could not find or load main class org.gradle.wrapper.GradleWrapperMain`，
在装有 Gradle 的机器上执行一次：

```bat
cd pokemon-builder\runtime
gradle wrapper --gradle-version 8.11.1
```

（或用 Android Studio 打开 `runtime/` 让其自动生成；之后 `gradlew` 即可正常使用。）

## 常用命令

```bat
REM 不依赖 Android SDK 的检查（project3 §44）：Builder 测试 + core 测试 + 依赖审计
gradlew agentCheck

REM Desktop：运行
gradlew lwjgl3:run

REM Desktop 打包（R12 §45–47）：Runnable JAR + dist\Windows\ 自包含应用（用户无需另装 JDK）
gradlew lwjgl3:dist

REM Android（需要 Android SDK 与 ANDROID_HOME）
gradlew android:assembleDebug
gradlew android-legacy:assembleDebug
```

`lwjgl3:dist` 产出两部分：`pokemon-builder/dist/PokemonGame.jar`（`java -jar`
即玩）与 `pokemon-builder/dist/Windows/`（`PokemonGame.exe` + `app/` +
`runtime/` + `runtime-data/`＝`generated/` 的副本，exe 旁布局由启动器自动识别）。
打包用 JDK 自带的 jpackage（app-image）：`jmods/` 缺失的裁剪 JDK 用
`-PruntimeImage=<运行时目录>`，`-PnoConsole` 可去掉调试控制台；首次打包前先
`builder.bat build-data` 生成 `generated/`。

## Windows 中文路径 / Gradle 测试（重要）

工程位于 `E:\仓库\范例\929\...`（路径含中文）时，Gradle 的测试 worker 会因
`@argfile` 编码不一致（守护进程按 `file.encoding` 写 UTF-8、JVM 启动器按系统
本地编码 GBK 读）而**全部报 `ClassNotFoundException`**。两种可用方式：

```bat
REM 方式 1（推荐，与验收流程一致）：从 ASCII 盘符运行
subst X: "E:\仓库\范例\929\pokemon-builder"
cd /d X:\runtime
gradlew.bat agentCheck

REM 方式 2：让守护进程编码与系统一致
cd /d E:\仓库\范例\929\pokemon-builder\runtime
gradlew.bat "-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=GBK" agentCheck
```

编译与打包（`lwjgl3:dist` 等）不受影响，两种路径都可以；只有测试 worker 受影响。
`settings.gradle` 在中文路径下会打出同样的提示。

## Android 模块的条件加载

未检测到 Android SDK（`ANDROID_HOME` / `ANDROID_SDK_ROOT` / `local.properties`）时，
`android` 与 `android-legacy` 两个模块不参与构建，`gradlew agentCheck` 等命令因此
不依赖 Android SDK（project3 §44）。安装 SDK 或用 Android Studio 打开本目录（会自动
生成 `local.properties`）后，两个模块自动回归构建。

R13 构建要求（均按实际构建证据配置）：AGP 8.7.3（根 `build.gradle` 固定）、
`compileSdk 35`（libGDX 1.13.5 后端依赖的 `androidx.core` 要求）、`minSdk 21`
（同一后端硬性要求；API 14/ARMv7 属后续阶段）、`android.useAndroidX=true`
（`gradle.properties`）。`gradlew android:assembleDebug` / `android-legacy:assembleDebug`
产出 debug APK；真机安装与运行仍需设备复核（见 `pokemon-builder/logs/backup-r13-*`）。

core 依赖审计（`auditCoreDependencies`）会拒绝 `com.badlogic.gdx.backends.android.*`、
`java.awt.*`、`javax.swing.*`、`javafx.*`、`org.jruby.*`、`org.rubygems.*` 等导入，
保证 core 可跨平台复用（project3 §7）。