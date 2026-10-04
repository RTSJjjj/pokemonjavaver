# Android 构建

> 对应任务书 §48–§51、§54。实现：`runtime/android/`、`runtime/android-legacy/`、
> `runtime/gradle/android-natives.gradle`、`scripts/build-android*.bat`。

## 1. 模块

| 模块 | 类型 | 说明 |
|---|---|---|
| `runtime/android` | AGP 应用（Modern） | `AndroidLauncher` + `AndroidKeyStateSource`；API 21+；四 ABI |
| `runtime/android-legacy` | AGP 应用（Legacy 占位，§50） | 原脚手架 `AndroidLauncherLegacy`（外置数据目录 + `LegacyKeyStateSource`）+ `LegacyRuntime` 复用证明（`GameDatabase.load` / `InputManager` 编译在 `:core` 上） |

两模块只共享 `generated/` 游戏数据，**不共享构建配置**。Modern 模块默认打包
**debug APK**（Technical Preview），L3 起支持 **release 变体**（预览 keystore 签名）
与**数据包**（`assets/runtime-data.zip`，首启解包，见 §5）；legacy 仍为 debug 占位。

## 2. 构建配置（按实际构建证据）

- AGP **8.7.3**（根 `build.gradle` 固定），Java 17 源码/目标。
- `namespace` / `applicationId`：`pokemon.runtime.android`、`pokemon.runtime.android.legacy`。
- `compileSdk 35`（libGDX 1.13.5 后端依赖的 `androidx.core` 要求）；
  `minSdk 21`（同一后端硬性要求）；`targetSdk 34`。
- `android.useAndroidX=true`（`runtime/gradle.properties`；gdx 后端依赖 androidx.core）。
- 清单：横屏、`exported` 启动 Activity、`@string/app_name`。

## 3. 原生库

`gradle/android-natives.gradle`（两个模块共用）：

- 声明 `gdx-platform` 与 `gdx-freetype-platform` 的
  `arm64-v8a / armeabi-v7a / x86 / x86_64` 原生包；
- `copyAndroidNatives` 解包 `.so` 到 `<module>/build/gdx-natives/jniLibs`，
  挂到 `merge*JniLibFolders` 与 `preBuild`——`assembleDebug` 不需要额外手工步骤；
- APK 内含 `lib/<abi>/libgdx.so` 与 `libgdx-freetype.so`（消息窗口字体）。

## 4. 游戏数据与输入

- 数据目录：应用**外置 files 目录**（`Android/data/pokemon.runtime.android/files`）。
  三种投递方式都落在同一目录：
  1. **L3 数据包（默认）**：`build-android` 把 `generated/ + Graphics/ + Fonts/`
     打成 `assets/runtime-data.zip`（zip 在 APK 内不重复压缩，`noCompress 'zip'`）；
     `AndroidLauncher` 首次启动用 core `DataPackUnpacker` 解包（版本标记
     `.data-pack-version`，包内容哈希变化才重解）。
  2. **外部数据包**：把 `dist/PokemonGame-Android-data.zip` 拷到上述目录，启动时自动解包。
  3. **adb push**：`adb push generated <dir>/runtime-data`（R13 流程，仍然可用）。
- 素材（L4）：解包后 `runtime-data/Graphics` 与 `runtime-data/Fonts` 就在数据根下，
  `GraphicsLocator` 优先读取随包副本（源工程只在开发时兜底）。
- 输入：与桌面同形的键盘适配器（`Gdx.input.isKeyPressed` → `InputManager`）；
  触摸/虚拟 DPad/A-B 是独立任务（§51），阶段 2 只保留 `GameAction`/`InputManager`
  抽象（`InputManagerTest` 锁定语义）。

## 5. 数据包（L3）

- 生成：`builder/src/data-pack.js`（同步、零依赖的 ZIP 写入；媒体类已压缩、
  JSON 用 deflate 1 级；条目按路径排序，内容哈希作为解包版本，约 26k 文件）。
- 解包：core `data/DataPackUnpacker`（纯 Java、zip-slip 防护、断点可重跑；
  Android 端由 `AndroidLauncher` 在 `initialize` 前同步执行 + Toast 提示）。
- 排除项：`--no-data` 跳过数据包（瘦 APK，配合 adb push / 外部数据包）。
- 预览签名：`runtime/android/keystore/preview-release.jks`（别名/口令均为
  `pokemon-preview`，`build-android --release`）；**这是开发用密钥，正式分发前必须替换**。

## 6. 命令与产物

```bat
REM 需要 Android SDK（ANDROID_HOME / ANDROID_SDK_ROOT / local.properties）
cd pokemon-builder\runtime
gradlew.bat android:assembleDebug            REM → android/build/outputs/apk/debug/android-debug.apk
gradlew.bat android:assembleRelease          REM L3：预览签名 release（keystore 存在时）
gradlew.bat android-legacy:assembleDebug     REM → dist 同名 legacy APK

REM 全流水线（R14，八步：审计→数据→脚本→音频→校验→数据包→Gradle→打包）
cd pokemon-builder
scripts\build-android.bat "D:\Game\PokemonProject"              REM 默认带 L3 数据包（fat debug APK）
scripts\build-android.bat "D:\Game\PokemonProject" --no-data    REM 瘦 APK（adb push / 外部数据包）
scripts\build-android.bat "D:\Game\PokemonProject" --release    REM 签名 release APK
scripts\build-android-legacy.bat "D:\Game\PokemonProject"
```

产物：`dist/PokemonGame-Android.apk`（fat debug，含 `assets/runtime-data.zip`）、
`dist/PokemonGame-Android-release.apk`（`--release`）、
`dist/PokemonGame-Android-data.zip`（同一数据包，供外部投递）、
`dist/PokemonGame-Android-Legacy.apk`。构建报告 `generated/build-report.json`
记录 `buildTarget`/`technicalPreview`/产物路径（含数据包）。

## 7. SDK 依赖与门控

`settings.gradle` 只有在检测到 Android SDK 时才包含 `android`/`android-legacy`，
因此 `agentCheck` 与 CI 的测试阶段**不依赖 Android SDK**（§44）；没有 SDK 时
`ci-build.bat` 的第 5 阶段会明确 SKIPPED（不是假装成功）。

所需 SDK 组件（实测）：`cmdline-tools` + `platforms;android-35`（compileSdk 35 必需；
`android-34` 视 AGP 默认 build-tools） + `build-tools;34.0.0`；用 Android Studio 打开
`runtime/` 会自动写 `local.properties`。

## 8. 验证证据（沙箱实测）

- `gradlew android:assembleDebug android-legacy:assembleDebug` → BUILD SUCCESSFUL
  （34 s，72 任务），两 APK 各 **5.3 MB**。
- `aapt2 dump badging`：包名、启动 Activity、`sdkVersion:'21'`、`targetSdkVersion:'34'`、
  `compileSdkVersion='35'`、`native-code: arm64-v8a armeabi-v7a x86 x86_64`。
- APK 内容：`classes.dex`、四 ABI 的 `libgdx.so` + `libgdx-freetype.so`。
- 打包流水线 E2E：`build-android` → `ANDROID BUILD SUCCESS`；
  `build-android-legacy` → `LEGACY ANDROID BUILD SUCCESS`。
- **NOT TESTED（agent 环境无设备）**：APK 的真机/模拟器安装与运行；
  `stripDebugDebugSymbols` 因无 NDK 只提示 "packaging as they are"（调试包无影响）。
