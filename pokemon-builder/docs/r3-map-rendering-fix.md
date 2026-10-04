# R3 地图渲染修复交接（2026-10-03）

> 后续更新：已进入 R4。本文中关于“仅静态事件、无玩家”“移动实体排序未接入”“TileMap.passable 反置”的历史状态，已由 [R4 行走修复](r4-walking-fix.md) 取代；新源码待用户本机验证。地图取图/分页规则继续沿用本次 R3 修复。

状态：基础地图渲染修复已由用户在本机完成多地图测试（2026-10-03）。用户原话：“测试了复数个地图，应该没有问题了。”这是用户运行结果反馈，不是 Agent 自动测试结果；本次修改的单元测试、agentCheck、captureMap 未确认执行通过。

验证经过：用户先明确反馈 Map002 正常；Map003 随后因 `Gen4 interior.png` 为 256×25000、末尾有 8 像素不足一行，被过严的尺寸检查拒绝。修正为读取完整的 781 行（24992 像素）、忽略尾部 8 像素后，收到上述多地图正常反馈。完整复测地图编号清单未提供，不额外推断覆盖范围。原始 RMXP 数据与素材均未修改。

任务勾选见 [task-flow-runtime.md 的 R3](../../project1/task-flow-runtime.md#r3--map-runtime--渲染)：已完成部分已拆分勾选，未实现的角色实体、动态事件和覆盖层保留待办，因此 R3 总项仍表示部分完成。

## 根因与修复

- RMXP 编号规则为 0..47 空白、48..383 七组自动元件、384 以上普通图块；普通图块图集索引须减 384。旧代码将两者反置。Map002 茶月镇地面 2,000 个格子全部是普通图块，因此旧分支会跳过绝大多数地面。
- 自动元件 48 种形状由四个 16×16 角块组成，不能用 shape/4 裁一个 32×32 格子。支持 96×128 帧横向排列及高度 32 的单格动画条，默认每帧 0.4 秒。
- Outside.png 为 256×24960，切成高度不超过 2048、与图块对齐的纹理页。TextureRepository 通过 AssetManager 管理纹理并统一 Nearest 过滤；GL 上下文丢失后可从源文件重建分页。
- MapRenderer 预加载所需资源，不再逐格逐帧查找文件或创建 Rectangle。三个编辑层按 tileset priority 与地图行深度排列，再与事件穿插。
- 事件角色按素材宽高的 1/4 裁切，水平居中、底部对齐，处理透明度。初始事件页按开关全 false、变量全 0 选择；有效空白页会隐藏旧图像。动态状态及事件解释器仍属于 R5/R6。
- MapData.TileData.tile 修正非正方形地图的行步长及坐标边界。
- 调整错误的几何测试，增加初始事件页、优先级遮挡与长方形地图回归测试；退出游戏释放当前 Screen 的纹理和 SpriteBatch。

格式交叉核对参考：[mkxp tilemap](https://github.com/Ancurio/mkxp/blob/master/src/tilemap.cpp)、[RMXP 自动元件四角布局](https://github.com/Ancurio/mkxp/blob/master/src/autotiles.cpp)。

## 修改文件索引

以下路径相对 `pokemon-builder/`；供下一位智能体定位实现，不需要重新改写已验收的渲染规则。

| 文件 | 本次变更 |
|---|---|
| `runtime/core/src/main/java/pokemon/runtime/map/TilesetGeometry.java` | 修正编号边界、普通图块偏移；48 形状四角表；两种自动元件帧宽与动画取帧。48 是形状数，不是动画帧数。 |
| `runtime/core/src/main/java/pokemon/runtime/map/TextureRepository.java` | AssetManager 管理普通 Texture 与自定义分页加载器；2048 高度页、Nearest、上下文恢复；忽略末尾不完整行。 |
| `runtime/core/src/main/java/pokemon/runtime/map/MapRenderer.java` | 预加载资源、primitive drawOrder、按 priority/行深度排序、可视裁剪、图块/角块绘制、静态初始事件页及实际角色帧尺寸。 |
| `runtime/core/src/main/java/pokemon/runtime/map/MapScreen.java` | 每帧调用 renderer.update(delta) 推进动画；删除旧 PNG 高度读取及冗余构造参数。 |
| `runtime/core/src/main/java/pokemon/runtime/data/MapData.java` | TileData.tile 使用 row * x + column，并分别检查两轴边界。 |
| `runtime/core/src/main/java/pokemon/runtime/app/PokemonGame.java` | dispose 时释放当前 Screen，进而释放纹理与 SpriteBatch。 |
| `runtime/core/src/test/java/pokemon/runtime/map/TileMapTest.java` | 移除错误的 384+ 自动元件预期，改为正确编号、角块、动画和普通图块边界断言。 |
| `runtime/core/src/test/java/pokemon/runtime/map/MapRenderingContractTest.java` | 新增矩形地图寻址、初始事件选页、priority 与角色遮挡深度测试。 |
| `runtime/lwjgl3/src/main/java/pokemon/runtime/lwjgl3/MapRenderCapture.java` | 新增隐藏窗口 OpenGL framebuffer 截图入口，共用生产 MapRenderer，支持全图/640×480 视窗。 |
| `runtime/lwjgl3/build.gradle` | 新增 captureMap JavaExec 任务，captureArgs 用竖线分隔参数。 |

未修改 `Graphics/`、`Data/`、`generated/` 地图数据，也未实现独立 `AutotileAnimator` 类；动画在 TilesetGeometry + MapRenderer.update 中。历史 R3 记录中关于该类及错误编号区间的描述不应继续作为实现依据。

## 下一位智能体的接续注意事项

- 保留当前编号、角块表和图集分页方案；TextureAtlas 是原需求推荐方案，当前实现使用受 AssetManager 管理的分页 Texture + SpriteBatch。
- R4 增加玩家/NPC 实体时，接入同一深度排序逻辑。目前 drawOrder 在构造时缓存，移动实体或运行时改图不能只改坐标而不更新排序。
- R5/R6 增加动态状态时替换 initialGraphic 的全 false/0 快照选页，并同步刷新事件图像和所需资源；当前缓存不会自动响应开关变化。
- R4 必须核对 TileMap.passable：现存逻辑把低四位 0 当作阻挡，与 RMXP 禁止通行位语义相反。该逻辑及原测试不在本次地图显示修复范围，不能因地图显示正常而认定碰撞正确。
- 当前视窗固定为 640×480，MapScreen 即使覆盖地图编号仍用 System 起始坐标居中；全图对照可用 captureMap，正式缩放/Viewport 属于 R11。
- 用户要求由其本机构建测试，本轮没有再次运行构建。后续是否执行新测试按用户新指令处理，不把历史旧版 agentCheck 日志用于证明这批修改通过。

## 用户本机测试（PowerShell）

本会话确认已有 `X:` 指向本项目的 `pokemon-builder`，用于避开原路径含中文时的 Gradle 工作进程类路径问题。

```powershell
$env:JAVA_HOME = 'C:\Users\Administrator\AppData\Roaming\.minecraft\runtime\java-runtime-delta'
Set-Location 'X:\runtime'
.\gradlew.bat :core:test :core:auditCoreDependencies :lwjgl3:run --args="X:/generated 2"
```

此命令启动茶月镇 Map002 的 640×480 视窗，只展示地图的一部分。全图对照可使用新增的 captureMap；它通过真实 libGDX/OpenGL 和生产 MapRenderer 导出整图，不是另写一套离线绘图逻辑：

```powershell
.\gradlew.bat :lwjgl3:captureMap '-PcaptureArgs=X:/generated|2|X:/logs/r3-map002-full.png|0'
.\gradlew.bat :lwjgl3:captureMap '-PcaptureArgs=X:/generated|2|X:/logs/r3-map002-viewport.png|0|viewport'
```

如 X: 不存在，在 PowerShell 中先执行 `subst X: 'E:\仓库\范例\929\pokemon-builder'`；如 X: 已用于其他目录，应换用空闲盘符并同步调整命令，不覆盖既有映射。

## 验收与边界

茶月镇应出现连续树林、灰色道路、红顶精灵中心、各色屋顶和花坛，与编辑器地图布局对应；不再大面积黑底或将树木画成花草。编辑器事件标记方框不属于游戏画面。

Map002 自身没有使用 48..383 的自动元件。用户多地图测试正常，但未提供水面动画、全部形状及遮挡的专项覆盖，因此这些专项验证仍未勾选。

不包含 R4 玩家实体绘制/移动、R5/R6 动态事件状态、事件色相/减色混合的完整还原、天气/雾/全景与 UI。基础地图显示修复已验收；原 R3 中超出本次修复的实体/覆盖层需求仍待完成。TileMap 原有通行位逻辑也需在 R4 核对；本次没有扩大到移动系统。

Agent 验证记录：普通 Gradle 调用被缓存锁权限阻止；授权调用随后在本机 loopback 建连失败，未到达编译任务（NOT TESTED IN AGENT ENVIRONMENT）。之后用户要求自行构建测试，Agent 未再启动构建或运行。用户随后反馈 Map002 正常以及最终多地图测试正常，作为本次地图显示验收依据。此前旧实现的成功日志不能用于证明本次自动测试通过。
