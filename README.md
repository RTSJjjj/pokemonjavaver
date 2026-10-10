# pokemonjavaver
宝可影辞的JAVA版构建器（有可能给0vej也能用。）
1. 数据编译(通用部分)
Builder 用 Node 写成,读取工程的 Data/*.rxdata 和 Scripts.rxdata,先解析 Ruby Marshal 格式。
它把地图、事件、公共事件、PBS 数据、音频、动画全部转成 JSON,放进 generated/。
这部分只依赖 RMXP 和 Essentials 的数据格式,所以对不同工程基本通用。
还带增量缓存,用 sha1 判断哪些单元需要重建。
2. 事件脚本编译(模式匹配,不是真转译)
RMXP 事件里的脚本框(命令 355/655)是 Ruby。
Builder 把这些脚本框合并成块,共 5441 个,然后按顶层语句拆开。
每条语句用 script-compiler.js 里的 HANDLERS 表做模式匹配。例如 pbItemBall(:POTION,3) 变成 {GIVE_ITEM, POTION, 3}。
不认识的 Ruby 不会被翻译,而是登记为 unsupported,并带上地图、事件、页、命令的位置。
发布构建遇到 unsupported 会失败。只有加 --allow-unsupported 的开发构建才放行。
3. Java 运行时
约 300 个 Java 文件,基于 libGDX。战斗、招式、特性、AI、进化、背包、电脑、宝可梦培育屋等都在这一层。
这部分逻辑不是自动生成的,是对照 plugin-src/ruby/ 里导出的插件源码逐行手工转写的。
运行时不带 Ruby,只执行 IR 命令和 JSON 数据。
正因如此，本工程只适用于宝可梦影辞9.27未发布版本及所有之前旧版本。

### 目录速查

| 目录 / 文件 | 是什么 | 备注 |
|---|---|---|
| `X:\你的\某个文件夹下的\原工程文件夹`(上一级) | 你的 RMXP 工程本体 | 在 RMXP 里改 |
| `pokemon-builder\scripts\*.bat` | 一键脚本 | **运行它们** |
| `pokemon-builder\builder-config.json` | 配置(工程路径等) | 偶尔改 |
| `pokemon-builder\generated\` | 中间数据 | 不用动,自动生成 |
| `pokemon-builder\dist\` | **最终成品** | windows版打个压缩包即可发布 |
| `pokemon-builder\logs\latest.log` | 最近一次运行的日志 | 出错时看 |
| `pokemon-builder\build\reports\` | 审计和脚本覆盖报告 | 检查质量时看（有插件更新导致build停止时也来看） |
| `pokemon-builder\docs\` | 说明文档 | 全是铸币克劳德写的人类无法参透，等我未来换成人话。 |
| `pokemon-builder\runtime\` | 游戏引擎(Java) | 这个一般不用管。 |




使用方法
环境准备。 
| **Node.js 18 或更新** | 运行 Builder | 命令行输入 `node -v` 能看到版本号 |
| **JDK**(建议 17 或更新) | 编译、测试、打包游戏 | 输入 `java -version` |
| **Android SDK**(可选) | 只在要出 Android 安装包时需要,并设置 `ANDROID_HOME` | 不出 APK 就不用装 |
| **FFmpeg**(放进tools下) | 音频步骤把不能直接用的音频转成游戏可用格式 | 确认 `tools\ffmpeg\bin\ffmpeg.exe` 存在 |（本质格式工厂）

缺 Node.js 时,所有脚本会直接报 Node.js was not found in PATH 并停止。
项目路径里有中文也可以。构建时工具会自动把路径临时映射成一个盘符,避开 Gradle 的编码问题。
如果运行 gradlew 时提示 Could not find or load main class org.gradle.wrapper.GradleWrapperMain, 说明缺 Gradle Wrapper 的 jar,见 runtime\README.md「首次构建」一节。
在builder-config.json中确认rmxp的ESSENTIAL1.18版本宝可梦改版工程本体（带有pbs数据及不为空audio文件夹）的地址。
###关于ffmpeg
- 构建只认这个固定位置:`pokemon-builder\tools\ffmpeg\bin\ffmpeg.exe`。**不会**去系统 PATH 里找。
- 这个目录被 `.gitignore` 排除了(`/pokemon-builder/tools/ffmpeg/`),所以**新克隆的仓库里没有它**,需要自己放:
  1. 下载 Windows 版 FFmpeg(完整版或 essentials 版均可,本机现在用的是 9.0.2 full build)。
  2. 解压后把 `bin`、`presets` 等内容放进 `pokemon-builder\tools\ffmpeg\`,使 `tools\ffmpeg\bin\ffmpeg.exe` 存在。
- 验证:在命令行运行 `tools\ffmpeg\bin\ffmpeg.exe -version`,能打印版本号即可。
- 只跑体检 `audio-audit` 不需要 FFmpeg(它只做分析)。
- 能直接使用的音频文件只是复制;只有需要转码或切分循环的文件才会调用 FFmpeg。缺 FFmpeg 时,这些文件会在音频步骤里被登记为失败,构建随之失败。
- 发布前的检查清单里写了要确认 `tools\ffmpeg` 被一并带上(`docs\release-1-plan.md`)。
打开命令行(Windows 终端或 cmd),进入 `pokemon-builder` 目录:

```bat
cd /d E:\仓库\范例\929\pokemon-builder
```

### 第 1 步:体检(约几秒)

```bat
scripts\audit-project.bat "E:\仓库\范例\929"
```

看到 `AUDIT SUCCESS` 就说明工程能被处理。它会在 `docs\` 和 `build\reports\` 生成报告。

### 第 2 步:生成数据(约几秒到一分钟)

```bat
scripts\build-data.bat "E:\仓库\范例\929"
```

看到 `BUILD SUCCESS` 即可。结果在 `generated\`。

> 这一步同时会处理 Boss 战:见第 6 节。

### 第 3 步:出 Windows 版(最常用)

```bat
scripts\build-pc.bat "E:\仓库\范例\929"
```

这是完整流水线,共 8 步,每一步都会打印进度:

```text
[1/8] Audit              体检
[2/8] Convert Maps       转换地图
[3/8] Compile Events     编译事件
[4/8] Translate Scripts  编译脚本(并生成 Boss 数据)
[5/8] Optimize Audio     音频处理
[6/8] Validate Runtime   校验运行时数据
[7/8] Gradle Build       运行测试 + 打包
[8/8] Package            检查产物 + 写 build-report.json
```

最后出现 `… BUILD SUCCESS` 就是成功。**任何一步失败都会立刻停下并给出非 0 的退出码,不会假装成功。**

成品位置:

```text
pokemon-builder\dist\Windows\PokemonGame.exe     ← 双击就能玩
pokemon-builder\dist\PokemonGame.jar             ← 有 Java 的机器上 java -jar 也能玩
```

把整个 `dist\Windows\` 文件夹发给别人即可,对方**不用安装 Java**。

### 第 4 步(可选):出 Android 包

```bat
scripts\build-android.bat "E:\仓库\范例\929"
```

需要 Android SDK。成品是 `dist\PokemonGame-Android.apk`。

- 想要瘦包(不含游戏数据,自己用 adb 推数据):加 `--no-data`
- 想要签名的 release 包:加 `--release`

---

## 4. 日常工作流:改了游戏之后怎么办

最常见的循环:

```text
在 RMXP 里改工程 → 保存 → 运行 build-pc → 试玩 dist\Windows\PokemonGame.exe
```

| 你改了什么 | 要做什么 |
|---|---|
| 地图、事件、对话、传送、开关/变量 | 直接 `build-pc`。只会重建改动过的地图(增量缓存),很快 |
| PBS 数据(训练家、野外遇敌、道具、宝可梦条目) | 直接 `build-pc`。但见下方"需要 Java 配合"提醒 |
| 图片、音频素材 | 直接 `build-pc` |
| 在 `Boss_Battles` 里新增一个 `def battleXxx` | 直接 `build-pc`,会自动生成,见第 6 节 |

### ⚠ 需要 Java 配合的情况(这些不是点个按钮就行)

下列改动,工具**不能自动处理**,需要有人改 Java 代码:

1. **新招式用了全新的"功能码"**,或新特性、新道具效果:数据能读进来,但行为要在 Java 里实现。
2. **事件脚本里写了工具没见过的新调用**,或者参数是变量/表达式(例如 `pbItemBall($game_variables[5])`)。构建会报 unsupported 并告诉你位置。
3. **在插件脚本里改了战斗、菜单、进化等逻辑**:运行时是对照插件原文手工移植的,不会自动跟着变。
4. **Boss 的写法和现有的不一样**:生成器很严格,不认识的语句会报错。

遇到这些:**不要硬绕过**。把报错信息(带地图 / 事件 / 页 / 命令位置)记下来交给开发者。

---

 5. 构建结果怎么看、出错怎么办

### 成功了怎么确认

- 控制台最后一行是 `BUILD SUCCESS`(PC)或对应的成功提示。
- `generated\build-report.json` 记录了这次构建的统计。

### 失败了怎么办

1. **先看控制台最后几行**,错误信息里一般带文件名或位置。
2. 想看完整过程:打开 `pokemon-builder\logs\latest.log`。
3. 常见情况:

| 现象 | 原因与处理 |
|---|---|
| `Node.js was not found in PATH` | 没装 Node.js,或没加入 PATH。安装 Node 18+ |
| `Cannot read ... Data\...rxdata` / 解析失败 | 工程里某个 `.rxdata` 损坏。用 RMXP 重新保存,或从备份恢复 |
| `unsupported script block ... map=… event=… page=… cmd=…` | 事件脚本里有工具不认识的写法。记下位置,交给开发者处理 |
| `Boss_Battles … unknown statement …` | 新增的 Boss `def` 里有不认识的语句,带行号 |
| `events call boss function(s) with no def` | 事件里调用了 `battleXxx`,但 `Boss_Battles` 里没写它 |
| 音频步骤报 ffmpeg 找不到、无法执行 | `tools\ffmpeg\bin\ffmpeg.exe` 不存在,按第 2 节放好 |
| Gradle 相关错误(找不到 JDK、找不到类) | 确认 `java -version` 正常;路径含中文时让工具自动映射盘符,不要手动移动目录 |
| 构建很慢 | 首次构建较慢,第二次起会复用缓存 |
| 怀疑缓存有问题 | 加 `--no-cache` 全量重建,或运行 `scripts\clean-build.bat` 清空后重来 |

### 检查"脚本覆盖率"

运行 `build-pc` 或 `build-data` 后查看:

- `build\reports\script-coverage.json`:有多少脚本块被转换、多少未支持
- `build\reports\java-handler-required.json`:需要 Java 处理的块清单

目标是 **unsupported 为 0**。发布版构建(不加开发开关)遇到 unsupported 会失败。

---

##  添加新 Boss(完整步骤)

Boss 登记在游戏脚本里的 **`Boss_Battles`** 段(RMXP 脚本编辑器里的一个脚本页)。

1. 在 RMXP 脚本编辑器打开 `Boss_Battles` 段。
2. 复制一个现有的 `def battleXxx ... end` 整段,改名字、种类、等级、属性。**保持同样的结构**:

   ```ruby
   def battleNewBoss
     $game_switches[196] = true
     count = $Trainer.ablePokemonCount
     size = (count > 2) ? 3 : (count > 1) ? 2 : 1
     setBattleRule(sprintf("%dv1",size))
     setBattleRule("canlose")
     setBattleRule("noexp")
     pkmn = pbGenPkmn(:SPECIES, 80)
     pkmn.battleRank = 3
     decision = pbWildBattleCore(pkmn)
     $game_switches[196] = false
     return decision==1
   end
   ```

   (以上只是示意,请以工程里已有的 `def` 为准照抄。)
3. 在地图事件里用条件分支调用它(脚本条件 `battleNewBoss`)。
4. 保存工程,运行 `scripts\build-pc.bat "E:\仓库\范例\929"`。

构建的第 4 步会自动:导出该段 → 生成 `BossBattleData.java` → 校验。你会在控制台看到类似:

```text
[4/8] Boss_Battles: exporting the plugin section and generating BossBattleData.java...
      defs=178 entries=177 updated
```

`defs` 比 `entries` 多 1 是正常的,多出来的是 `battleBoss`(它没有被转写,事件里也没用到)。

---

## 所有命令速查

在 `pokemon-builder` 目录下运行:

| 命令 | 作用 |
|---|---|
| `scripts\audit-project.bat "<工程路径>"` | 体检,只出报告 |
| `scripts\build-data.bat "<工程路径>"` | 只生成数据(`generated\`) |
| `scripts\build-pc.bat "<工程路径>"` | **完整出 Windows 版** |
| `scripts\build-android.bat "<工程路径>"` | 出 Android 版(需要 Android SDK) |
| `scripts\build-android-legacy.bat "<工程路径>"` | Android 旧版占位包 |
| `scripts\clean-build.bat` | 清理中间产物(不会动你的工程) |
| `scripts\clean-build.bat --all` | 连 `dist\` 和旧日志一起清 |
| `scripts\test-builder.bat` | 运行 Builder 自带的自动测试 |

常用附加选项(写在命令末尾):

| 选项 | 作用 |
|---|---|
| `--no-cache` | 忽略缓存,全量重建 |
| `--no-assets` | (`build-pc`)不复制图形素材进 dist,加快本地反复构建 |
| `--runtime-image "C:\jre"` | (`build-pc`)JDK 没有 `jmods` 时指定运行时 |
| `--no-data` / `--release` | (`build-android`)瘦包 / 签名 release 包 |
| `--project "<路径>"` | 用命令行指定工程路径 |

退出码:`0` 成功,`1` 出错。写批处理串联命令时可据此判断。

---

##  玩游戏时的按键

| 按键 | 作用 |
|---|---|
| X / Esc | 打开暂停菜单(存档、读档、设置、回标题、退出) |
| F11 | 全屏 |
| F6 | 快速存档 |
| F9 | 读取快速存档 |

---

## 绝对不要做的事

- **不要手动编辑 `generated\`、`dist\` 里的文件**。下次构建会覆盖,也容易造成数据不一致。
- **不要手动编辑 `BossBattleData.java`**。它是自动生成的,文件头写着 "do not edit by hand"。
- **不要用 `--allow-unsupported` 出正式发布版**。这个开关只给开发调试用,会放行没转换的脚本,玩家遇到会出问题。
- **不要把工程放进被同步盘实时同步的目录**(网盘、云盘)构建,容易因为文件被占用而失败。(经验建议,仓库文档没有明确写这一条。)

---

## 想看更详细的资料（全是克劳德写的鬼话，能看懂你也是智能体了）

| 想了解 | 看这里 |
|---|---|
| 系统整体结构 | `docs\architecture.md` |
| 脚本是怎么编译的、Boss 流水线 | `docs\script-compiler.md` |
| PC 打包细节、启动参数 | `docs\desktop-build.md` |
| Android 打包细节 | `docs\android-build.md` |
| 事件解释器支持哪些指令 | `docs\event-runtime.md` |
| 还没转译完的内容和路线 | `docs\transcription-roadmap.md`、`docs\untranslated-audit.md` |
| 分支与提交约定(开发者) | `docs\git-workflow.md` |
