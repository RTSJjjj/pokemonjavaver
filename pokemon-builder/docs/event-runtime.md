# 事件运行时（Event Runtime）

> 对应任务书 §11–§20、§26–§30。实现集中在 `runtime/core/.../event/` 与 `map/`；
> 本文件说明事件命令覆盖、触发/页面语义、移动路线、消息窗口与端口机制。

## 1. 命令覆盖（`EventInterpreter`）

| 码 | 命令 | 说明 |
|---|---|---|
| 0 / 104 / 108 / 401 / 404 / 412 | 空行 / 文本选项 / 注释 / 文本续行 / 分支收尾 | 消费/忽略 |
| 101 + 401 | 显示文本 | 2 行/页（R6.31）、颜色/换行/居中标签、名字框、`\w[skin]`；等待确认 |
| 102 + 402/403/404 | 显示选项 | `choice` 皮肤 + 光标；问题文本与选项同屏；只执行选中分支 |
| 106 | 等待 | 按帧计（40 fps） |
| 111 + 411/412 | 条件分支 | 开关/变量/自开关/角色方向/按钮/简单表达式（含 `$game_player.x/.y`，§10）；复杂 Ruby 走 IR `CONDITION` |
| 115 | 中止事件处理 | `program.finish()` |
| 116 | 公共事件 | 递归帧（公共事件源由 `GameDatabase.commonEvent(id)` 提供） |
| 121 / 122 / 123 | 开关 / 变量 / 自开关 | 122 参数为 `[start, end, operation, operandType, value]` |
| 201 | 场所移动 | `MapPort.transfer(...)`（含淡出淡入参数）；**不结束事件**，传送完成后从下一条命令继续（§9） |
| 202 | 设置事件位置 | 同步事件实体与 MapData |
| 203 / 204 / 206 | 滚动地图 / 更改地图设置 / 雾不透明度 | `MapEnvironment` + `CameraScroll`（R6.22） |
| 207 | 显示动画 | 角色动画（position 0/2 以头顶为锚点，R6.28）；不阻塞 |
| 208 | 更改透明状态 | 改**玩家**透明（到达门隐身依赖它） |
| 209 + 509 / 210 | 移动路线 / 等待移动结束 | `MoveRoutePlayer`；码表含 5–8 斜向、10/11 朝/离玩家、13 倒退、16–26 朝向类（R6.32） |
| 221 / 222 / 223 / 224 / 225 | 淡出 / 淡入 / 更改色调 / 闪烁 / 震动 | `ScreenEffects`（223 使用 Builder 导出的 Tone 数值与过渡帧数） |
| 231 / 232 / 235 | 显示 / 移动 / 消除图片 | `PictureService` |
| 241 / 242 / 245 / 249 / 250 | BGM / 停止 BGM / BGS / ME / SE | `AudioManager`；BGM 过场为 1 秒交叉淡出（R6.24） |
| 355 / 655 / 509 | 脚本 | Builder IR（`ScriptIr`）；355+655 合并为一块，只执行一次（R15） |
| 234 | 更改图片色调 | **未实现**：只告警一次，事件继续 |

## 2. 触发与页面

- 触发类型：`0` 确认、`1` 玩家接触、`2` 事件接触、`3` autorun、`4` 并行。
- **视线触发（L6b）**：名为 `Trainer(N)` / `Counter(N)` 的 **trigger 2** 事件在玩家
  走完一步时检查（原版 `Game_Player` 的 `pbCheckEventTriggerFromDistance([2])`）：
  `Trainer(N)` 要求玩家在事件**面朝轴**上 N 格内且中间每格可通行
  （`pbEventCanReachPlayer?`，用 `Collision.canStepFrom` 沿轴扫描）；
  `Counter(N)` 只要求玩家在同一轴的 N 格内（`pbEventFacesPlayer?`，无通行检查）。
  命中即按事件所在格启动其 trigger 2 页（每次一个，与共享解释器一致）。
  本工程 184 张图、326 个 Trainer + 118 个 Counter。
- `EventPages.resolve(state, mapId, event)` 按 开关1/开关2/变量/自开关 条件选页；
  换页后 `EventCharacters.refreshGraphics()` 重建立绘（空页/瓦片页不再复用旧图，R6.21）。
- **到达门**：门事件落地后由 `tsOff?("A")` 到达页接管（显示玩家、迈步、恢复透明）。
  R6.33 修复：多门地图上所有到达页同时为真时，autorun 驱动按事件顺序逐帧排队
  （`autorunCooldown = 1/40 s`），`DoorShowHold` 只有在**没有待执行 autorun 页**时
  才释放隐身。
- **并行事件**：各自持有独立解释器，互不抢占消息窗口（R6.2x 修复）。

## 3. 移动与碰撞

- `MapCharacter`：格子 → 像素插值、方向/图案、跳跃（14）、换行走图（41）、斜向
  双轴移动（5–8）、倒退保持朝向（13）、朝向类命令尊重 `directionFix`（16–26）。
- `Collision.canStep*`：地形通行（瓦片 passages 四向位）、事件阻挡（需活动页且有
  立绘的实体）、斜向两条 L 形路径（R6.32）。
- 自主移动：固定/随机/接近/自定义（页属性），移动时/停止时动画、速度/频度。
- 玩家越界行走：`MapLinks` 判断邻居可走并原地换图（无淡出；相机仅在
  `SnapEdges` 时夹取），保留玩家坐标的连续性（R6.23）。

## 4. 消息 / 选项 / 皮肤

- `MessageWindow`（R6.31）：Essentials `SpriteWindow` 九宫格（`speech bw 1`、
  `choice 1`、名字框）、`selarrow` 光标、暂停光标、22px 字体 + 2px 阴影；
  文字颜色 `\c[1..12]`、`<c3=...>`、`<c2=...>`、`\b`、`\r`；`\l[n]`、`\n`、
  `<ac>` 居中；54 半宽单位换行、每页 2 行。
- 选项：102 紧跟 101 时末页不等确认——问题与选项同屏；`402` 按索引执行分支。

## 5. 端口机制（防漏接）

解释器不直接触碰地图屏：所有屏上操作走 `MapPort`，由 `RuntimeContext` 转发到
`MapScreen`。新增端口必须同步：
`RuntimePortForwardingTest` 会在缺少转发时失败（R6.14 的教训）。当前端口涵盖
transfer/setEventLocation/eraseEvent/透明/色调/闪光/震动/滚动/雾/动画/气泡/
推石头/浮板/洞窟动画/移动路线/音频等。

## 6. 存档与状态

`GameState` 保存玩家位置与朝向、开关、变量、自开关、物品、任务、强度门控等；
`SaveManager` 写 JSON（`saveVersion=1`，`saves/` 经 `StoragePort`）。Desktop 入口
F6 快速存档 / F9 读取；地图/事件状态在加载后由 `MapScreen` 重建。

## 7. 无头可测性（§61）

事件解释器、`GameState`、移动/碰撞、IR 执行都不需要 OpenGL；R15 的
`FixtureGoldenTest` / `FixtureSmokeTest` 在无图形环境下跑完 fixture 的全部事件页，
`ScriptIrExecutionTest`、`WalkingRegressionTest` 等也全部无 GL。带图形的验证
（像素/截图/真窗口）位于 `__r6-staging` 探针与 R12/R14 的打包 exe 实测。

## 8. 桥（pbBridgeOn/Off）与浮板（toggle_liefeng_switches）— R6.34

### 8.1 桥

- 原文：`PField_Field:1363-1369`（`pbBridgeOn(height=2)` 写
  `$PokemonGlobal.bridge`，`pbBridgeOff` 写 0）；唯一读数是
  `Game_Map#playerPassable?`（`0025.rb:225-252`），并且 `Game_Map:162` 只把
  `$game_player` 交给它 —— 其他角色走的是它下面的分支，那个分支**没有任何桥判定**。
- 编译器早已把 `pbBridgeOn/Off` 编成 `SET_BRIDGE {on}`，但解释器一直没有这个
  case：383 处调用全部落空 → 20 张图的 1100+ 块桥砖（多数正压在完全不可通行的
  水/虚空上）永久是墙，这就是"走到桥边卡住"。
- 实现：`EventInterpreter` 新增 `SET_BRIDGE`（`on` → 高度 2，即 Ruby 默认值；
  全工程 161+222 处调用都是无参形态，见 `docs/api-usage.md`）；`GameState.bridge`
  进档（`SaveManager` 的 `bridge` 字段，旧档缺键即 0）；`TileMap.playerPassable`
  逐行转译 `playerPassable?`；`Collision.passage/canLand` 按 `character.isPlayer`
  分流；`terrain_tag` 增加带 bridge 的重载（`countBridge=false` 时才在
  `bridge==0` 跳过桥砖），遭遇查询改用该重载（`PField_Encounters:153` 就是不传
  `countBridge`）。
- 登记偏差（R6.37 已接线，见 §11）：桥砖的**贴图 z**（`Tilemap_XP:411` 的
  `spriteZ = 1`）与水面倒影高度（`0032.rb:61`）——R6.34 只做通行；图层规则已补，
  角色倒影精灵仍未做。
- 验证：`BridgeTest` 5 例（含真实 map 21：34 块桥砖，`pbBridgeOff` 时全部是墙、
  `pbBridgeOn` 时全部至少一个方向可走）。

### 8.2 浮板

- 机制（`33968587.rb:20-29` + 实盘数据）：5 张图（60/207/209/210/371）各有一个
  **并行页**（trigger 4，整页一行 `toggle_liefeng_switches`）每帧把"玩家是否站在
  这块板上"写进 102 块 `float_plate` 事件的自开关 A；板自身页 1（A 关）= 图形 520、
  决定键触发，页 2（A 开）= 图形 521、**玩家接触**触发，页内
  `变量26 = 随机(1..3)` 再三分支播 `SE GUI trainer card open`（音高 120/130/150）。
  「跳动」= 520→521 换图，「声音」= 这条 SE。
- 修复 1（音效）：`Interpreter#command_122` 的**随机操作数**（`operandType==2`，
  `0046.rb:839-840` 的 `@parameters[4] + rand(@parameters[5]-@parameters[4]+1)`）此前
  被报成 unsupported，变量 26 永远拿不到 1..3 → 三个分支全不成立、一条 SE 都不播。
  同时补齐 `applyOperation` 的源码守卫与 ±99999999 截断（`0046.rb:865-893`；其中
  `% 1` 在源码里是 `next`，即保持不变，不是 0）。
- 修复 2（时序）：RMXP 的 `@x/@y` 在**起步**就变成目标格，因此并行页在玩家还在走
  的时候就把开关翻开，落脚帧的 `check_event_trigger_here([1,2])`
  （`Game_Player:404`）必然看到页 2；本移植的格子坐标在**落脚**才变，于是
  (a) 插件读 `$game_player.x` 的语义用 `MapCharacter.logicalX()/logicalY()`
  （起步后 = 目标格）补齐；(b) `MapScreen.startOwnTileTouch()` 在玩家站立时对
  **脚下格**补检 Player Touch 页，按 (事件,页号) 闩锁，页不换就不重复启动
  （落脚帧先把闩锁随格子变化清空，所以页在落地后才翻也能补上）。
- 验证：`FieldInteractionsTest.floatPlatePageShape`（102 块板的页结构 + 随机形参
  1..3）、`ScriptIrExecutionTest.randomVariableOperand`（种子对照 `1+rand(3)`）、
  `toggleFloatPlatesUsesTheStepDestination`（起步即写开关）、`BridgeTest`
  的 `logicalX/logicalY` 断言。

## 9. 同图场所移动不再重开自动事件 — R6.35

- 症状（用户报告）：玩家被自动执行事件（trigger 3）用场所移动（201）挪到**同一张图**
  的另一格后，事件从第一页第一条命令重来，形成无限循环。实例：`Map006` 事件 29
  `Counter(6)` 页 2（自开关 A 开 → autorun）：`201 [0,6,7,20,6,0]`（map 6 = 当前图）
  之后还有 70 多条命令，直到最后才 `pbSetSelfSwitch(...,"B",true)`。
- 原文语义：`Interpreter#command_201`（`0046.rb:1001-1043`）只 `@index += 1` 再
  `return false`，**事件没有结束**，等待换图后继续；`Scene_Map#transfer_player`
  （`0047.rb:66-93`）只在 `$game_map.map_id != player_new_map_id` 时才
  `$MapFactory.setup`，**同图传送不重建地图**，`Game_Event` 与运行中的解释器都原样保留。
  首版把这理解成"传送结束事件"（`program.finish()`），换图时又总是新建 `MapScreen`
  并在构造里 `interpreter.stop()`，于是同图传送 = 丢掉正在跑的 autorun → 自开关 B
  永远等不到，页 2 条件恒真，循环。
- 实现：
  - `EventInterpreter.transferPlayer` 改为 `program.advance()` + `WAIT_TRANSFER`；
    新增 `resumeAfterTransfer()`（仅从 `WAIT_TRANSFER` 回到 `RUNNING`）。
  - `MapScreen.switchMap` 先判 `transferStaysInMap(currentMapId, targetMapId)`：
    同图走 `transferWithinMap` —— 就地 `player.teleport` + 朝向、写 `GameState`
    位置、**重置 `lastPlayerTile`**（RMXP 的 `moveto` 不算一步，否则落脚会误触发
    草丛/遭遇）、`latchOwnTouchTile()`（传送不是走路，落脚格不得触发玩家接触页：
    map35 事件 7/8 这类"传送门对"会互相弹射成死循环）、`cameraScroll.reset()`
    + `followPlayer()`、最后 `interpreter.resumeAfterTransfer()`；**不新建
    MapScreen、不 stop 解释器**，画面/图片/色调/临时开关全部保留（地图没有 setup）；
    带淡入淡出参数时按重建路径的语义 `fade(12, false)` 淡回。
  - 跨图传送维持原状（新建 MapScreen，旧事件结束）。
- 验证：`EventInterpreterTest.transferPlayer`（WAIT_TRANSFER → resume → 下一条命令执行）、
  `EventInterpreterTest.sameMapTransferContinuesTheEvent`（map6/event29 页 2 形状：
  同图 201 后自开关 B 必须被写、事件结束而不是从头再来）、
  `InMapTransferTest`（同图保留屏幕 / 跨图重建）、`RealSameMapCutsceneTest`
  （用真实 `generated/` 数据跑 map6 事件 29 页 2：同图传送被应用后事件跑到自开关 B，
  恰好一次传送、不循环）。
- 待真机验证：同图就地传送的**画面**行为（不重建 `MapScreen` 时图片层与相机）；
  无头测试覆盖到解释器与判定，渲染路径需要带图形的实测。

## 10. 玩家坐标条件 + 移动路线穿透/最前显示 — R6.36

- 症状（用户报告）：`Map036` 事件 6 `Counter(3)` 页 2 在 `201 [0,36,64,17,6,0]`
  之后用 `111 [12,"$game_player.y==16"]` / `"$game_player.y==18"` 三选一给事件 3
  发移动路线；结果"事件撞到玩家上卡住事件进行"。
- 三个根因：
  1. **脚本条件漏实现**：`scriptCondition` 只认 `$game_switches` / `$game_variables`
     / 自开关 / `onEvent?` / temp switch / 队伍人数。`$game_player.x/.y` 落到
     `unsupported()` 并**恒为 false**，所以两个分支永远不成立、总是走 else 路线。
     本工程 111[12] 里有 10 处这样的坐标判断（另有 `$PokemonGlobal.followerToggled`
     24 处等仍未实现，见 §11 待办）。
  2. **穿透 ON/OFF 对 NPC 无效**：`MapRouteContext.playerBlocks` 在 `Collision.canStep`
     之前判"玩家挡路"，却没看角色自己的 `through`。原文
     `Game_Character#passableEx?`（`0021:187`）在 `@through` 时**直接返回 true**，
     所以移动路线码 37（穿透 ON）正是让 NPC 从玩家身上走过的开关；漏掉后每步被挡，
     `blockedStep` 只能 3 秒超时跳一条，紧跟的 `210 等待移动结束` 就长时间卡住。
  3. **页面刷新清掉路线开关**：`MapScreen` 只要 `gameState.version()` 变化就调
     `EventCharacters.refreshGraphics()`，而它无条件用页属性覆盖
     `through/alwaysOnTop/directionFix/speed`。原文 `Game_Event#refresh` 在
     `new_page == @page` 时**直接 return**，路线临时开关不会被别人翻开关冲掉
     （本工程浮板地图的并行页每帧写开关，等于每帧清）。
- 实现：
  - `EventInterpreter` 新增 `SCRIPT_PLAYER_POSITION`
    （`^\$game_player\.(x|y)\s*(==|>=|<=|>|<|!=)\s*(-?\d+)$`），用 `GameState` 的
    玩家坐标判定（事件执行期间玩家不移动，坐标就是当前格）。
  - `MapRouteContext.playerBlocks` 先判 `character.through`：为真直接返回 false，
    与 `passableEx?` 同序（对 10/11、5–8、14 全部生效）。
  - `EventCharacters.refreshGraphics()` 在 `page == activePages[i]` 时 `continue`
    （对应 `Game_Event#refresh` 的提前返回），路线设置的
    through（37/38）、最前显示（39/40）、方向固定（35/36）、速度（29）得以保留。
- 验证：`EventInterpreterTest.playerPositionScriptConditions`（y==16 命中、y==18 不命中、
  x>=10 命中）、`MapRouteContextTest.playerBlocksNamedCharacters`（37 开后可以踏上玩家格、
  38 关掉恢复阻挡）、`EventCharactersTest.routeFlagsSurviveUnchangedPageRefresh`
  （翻开关后 through/alwaysOnTop 仍在）、`RealCutsceneBranchTest`
  （真实 map36 事件 6 页 2：y=16 走"先下"路线、y=17 走"横穿"路线；后者在 tile 层
  让 NPC 从 (66,17) 走到 (51,17) 且穿过玩家所在格，路线正常结束、38/40 关回）。
- 已知未覆盖：`$PokemonGlobal.followerToggled`（24 处）、`$Trainer.numbadges` 等
  条件仍是 unsupported→false，尚未接线；另外路线 37 只作用于**走路的那个角色**
  （`playerBlocks`），别的角色判它挡路时仍读页属性 `EventPages.through(page)`
  而不是路线临时开关（原文 `passableEx?:196` 读 `event.through`）——需要把
  `EventCharacters` 的活体角色接进 `Collision`，本轮未做。

## 11. 桥砖图层：在桥上/桥下两种状态 — R6.37

- 症状（用户报告 + 截图）：修好通行后玩家站在桥上被桥面盖住（只从板缝里露出一点），
  即"忘了调图层"。用户同时指出：**被桥盖住**正是"没触发上桥脚本、从桥下穿过"
  时该有的样子，所以不能简单地让桥永远画在角色下面。
- 原文规则（`Tilemap_XP:408-414` 的 `addTile`）逐块决定 z：
  ```ruby
  if PBTerrain.hasReflections?(terrain)          # StillWater / Puddle
    spriteZ = -100
  elsif $PokemonGlobal.bridge>0 && PBTerrain.isBridge?(terrain)
    spriteZ = 1                                  # 站在桥上：桥面沉到角色下面
  else
    spriteZ = (priority==0) ? 0 : ypos+priority*32+32   # 桥下：按优先级盖住角色
  end
  ```
  关键点是 `$PokemonGlobal.bridge`（`pbBridgeOn` 写入）**决定同一块桥砖的 z**，
  这就是两种状态的区别，而不是"桥砖固定在上/下"。
- 实现：
  - `MapRenderer.tileDepth(row, priority, terrain, bridgeHeight)`：StillWater(6)/
    Puddle(16) → `REFLECTION_DEPTH = -100`；bridge>0 且 terrain=15 →
    `BRIDGE_DEPTH = 1`；否则沿用原 `tileDepth(row, priority)`。
  - `MapRenderer.rebuildTileOrder()` 抽出静态层表的构建；`rebuildOrderIfNeeded()`
    发现 `state.bridge()` 与建表时不同就重建（`pbBridgeOn/Off` 翻转时立即换层），
    实体层不受影响。
  - `TileMap` 补 `TERRAIN_STILL_WATER = 6` / `TERRAIN_PUDDLE = 16` 常量
    （`PBTerrain.hasReflections?`，`0167.rb:80-83`）。
  - `MapScreen` 在换图构造与同图传送里补 `gameState.bridge(0)`
    （`Scene_Map#transfer_player:70` 的 `pbBridgeOff`）：传送后桥状态不跨图泄漏，
    否则新图的桥砖会一直停在"角色在上"的层。
- 验证：`MapRenderingContractTest.bridgeTilesLayerUnderTheHeroOnlyWhileTheBridgeIsUp`
  （桥关：z > 角色行；桥开：z=1 < 角色行；反射砖 -100；其它地形不变）、
  `BridgeTest.realBridgeMap` 追加真实 map 21 断言（31 块 priority 5 的桥砖在桥关时
  z 高于角色、桥开时一律 1）。
- 未做：`Sprite_Reflection`（`69168541-Follower_Main.rb:996`）的角色倒影精灵与
  `0032.rb:61` 的水面倒影高度；本轮只处理**瓦片自身的 z**。

