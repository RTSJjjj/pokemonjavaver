# R4 行走系统接手修复（2026-10-03）

状态：基础源码已补齐，**待用户本机构建及运行验收**。本轮未执行 Gradle、javac、JUnit 或游戏；静态检查与素材核对不能代替实际结果。R3 先前的多地图显示通过，不等于 R4 移动系统通过。

## 本次修改

| 文件（相对 runtime/core/src/main/java/pokemon/runtime/） | 行为与原因 |
|---|---|
| map/MapScreen.java | 移除不存在的 boy_walk。依据 PBS/metadata.txt 的 PlayerA，使用已核对存在的 trchar000.png、boy_run.png（均 512×512）；目前是本工程默认玩家常量，尚非通用 PBS/性别配置加载器。缓存 StepMover，控制器负责全部移动更新，避免一帧更新两次。 |
| map/MapCharacter.java | 步行 4 格/秒、Shift 跑步 8 格/秒；步行/跑步图选择；完成一步时返回剩余时间；禁止覆盖进行中的一步，检查邻接方向及边界。 |
| map/MovementController.java | 去掉每走一格后的按键重复等待，持续按键连续走；保留剩余帧时间，松开只完成已经开始的一步；新按下方向优先，阻挡时原地转向。旧带 holdDelay/repeatInterval 参数的构造器已删除。 |
| map/Collision.java | 检查出发格方向与目标格反方向，而非把目标任意通行位当作四向阻挡；tile 事件参与通行判断，人物事件按有效页和 through 阻挡；调试地图提供最近可走出生点查找。 |
| map/TileMap.java | 从第三层向地面检查；阻挡位立即拒绝，通行的 priority=0 层立即通过，不再无条件累计所有下层阻挡。普通步行忽略本工程 Neutral=13、Bridge=15（初始桥状态为 0）图层。 |
| map/EventPages.java（新增） | 渲染、碰撞共用同一初始事件页选择逻辑；开关/self switch=false、变量=0；有效空白页遮蔽旧页；读取 movement.through。R5/R6 应在这里扩展真实状态。 |
| map/MapCamera.java | centerOnPixels 跟随移动插值，保留边界夹取和小地图居中。 |
| map/MapRenderer.java | 预加载步行/跑步两套角色图，按插值像素深度更新遮挡；复用实体和输出数组，与固定图块/事件排序合并，不再每换行分配整个地图排序数组。 |
| input/DefaultKeyBindings.java | bind 改为公开，可在代码中修改逻辑键映射；输入数组复制以避免调用者改写原数组。 |

测试源码：`runtime/core/src/test/java/pokemon/runtime/map/WalkingRegressionTest.java` 新增 9 项，覆盖方向出入口、上层地面优先级、有效页/through、tile 事件、持续行走/跑步距离、帧时间分片/松键、阻挡转向/步进不可覆盖、调试出生点、像素相机/跑步图；`MovementControllerTest.java` 更新控制器接口及持续按键用例。这些测试本轮未执行。

## 依据与边界

读取原工程 `Data/Scripts.rxdata` 中 Game_Map.passable?/playerPassable?、Game_Character.passableEx? 和 PBTerrain 的静态源代码核对规则；仅在构建工具的现有 Marshal 解码器中查看，没有执行 Ruby 或给 Runtime 引入 Ruby 依赖。

- 事件碰撞不以 opacity=0 为穿透条件；是否 through 来自有效页。没有图像的事件不按人物阻挡，tile 图像事件使用其 passages/priority。
- NPC 仍使用静态事件页图像；未加入自主行走、事件路线、交谈、门传送、存档或开关刷新。
- 尚未移植冲浪、骑车、跳台阶、冰面、动态桥状态和地图无缝连接。当前范围为基础步行。
- 默认玩家取 PlayerA 的两张已知图；后续 R5/角色选择需要将其换成运行时配置。运动中更换角色图名仍需重新注册/预加载资源。
- 调试启动不同地图时选最近且至少能走出一侧的非空白格；没有此类位置则退回地图边界内坐标，以便查看纯剧情/空白地图。原工程地图/起始位置不被写回。
- 旧 `r3-map-rendering-fix.md` 中关于“没有玩家”“动态实体排序未接入”“passable 反置”的记录已由本次 R4 修复取代；其他 R3 限制仍有效。

## 用户本机验证（PowerShell）

沿用现有 X: 映射，位于 `X:\runtime`：

```powershell
.\gradlew.bat :core:test :core:auditCoreDependencies :lwjgl3:run --args="X:/generated 2"
```

Map003：关闭上一个游戏后执行：

```powershell
.\gradlew.bat :lwjgl3:run --args="X:/generated 3"
```

方向键/WASD 行走，按住 Shift 跑步。检查连续走路不逐格停顿；松键完成当前格；墙和 NPC 阻挡且能原地转向；镜头不每格跳动；人物经过树木/屋檐时遮挡正确。地图不显示白色事件编辑框，门和剧情互动仍待 R6。

用户确认后再勾选 R4 总项；不要把旧 R3 的 14 项测试记录当作本次 R4 测试通过证据。
