# android-legacy（占位模块）

project3 §50：阶段 2 只建立模块与复用契约，**不实现** API 14 / ARMv7 后端，
也不承诺老设备实机兼容；真实 legacy 后端属后续阶段。

- `LegacyRuntime` 直接编译在 `:core` 上，用两个方法把契约钉死：
  - `loadData(...)` → `GameDatabase.load(...)`：同一个 Builder 产物（Runtime
    Data）可以被 legacy 后端读取；
  - `newInput()` → `InputManager`：逻辑输入（GameAction）是纯 Java 的，未来
    触点/键盘层只需要 `press/release/set`。
  一旦 core 引入平台专属依赖，本类会编译失败，契约被破坏会立刻暴露。
- `AndroidLauncherLegacy` 是可构建的占位启动器（minSdk 21）。当前 libGDX
  1.13.5 的 Android 后端自身就要求 minSdk 21（构建报错原文：`minSdkVersion 19
  cannot be smaller than version 21 declared in library
  [gdx-backend-android]`），因此占位不能低于 21；真正的 API 14 设备工作必须搭配
  旧后端，单独立项。
- 本模块与 `android` 一样受 Android SDK 门控（见 `settings.gradle`）：没有
  SDK 时两个模块都不参与配置，`agentCheck` 不受影响。

```bat
gradlew android-legacy:assembleDebug
```
