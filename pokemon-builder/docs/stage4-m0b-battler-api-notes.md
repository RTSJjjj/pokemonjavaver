# Stage 4 / M0b 读书笔记：Battler / Battle / Move 助手面的 Ruby 依据

> **本文件是唯一交付物。** 本次任务为只读研究，未改动任何 `.java` / `.js` 文件。
>
> **一切依据均为本工程 `Data/Scripts.rxdata` 的现场导出**（`node __sections.mjs "<段名>"`，行号 = 段内行号）。
> 未参考任何官方 Pokémon Essentials 记忆；与官方 v21 的差异若被发现，均记在 §F。
>
> 导出件落在 `E:\仓库\范例\929\__r20-ref\m0b\`（新目录，未改动 `__r20-ref\ruby\` 下 Lead 的既有文件）。
> 方法清单来源：Lead 的 `__r20-ref\api-surface.mjs`，我对同三个文件重跑过一遍，结果与其给出的
> 147 / 41 / 25 完全一致（`__r20-ref\m0b\api-surface.txt`）。

---

## 0. 统计

| 项 | 数量 | 说明 |
|---|---:|---|
| 入口总数 | **213** | Battler 147 + Battle 41 + Move 25 |
| 去掉提取器伪影 | −4 | `status!` / `gender!` / `index!` / `species!` 实为 `status!=` / `gender!=` / `index!=` / `species!=` 比较（见 §0.1） |
| **实际需要冻结签名的方法** | **209** | 143 + 41 + 25 |
| **可转（纯逻辑，零未建模依赖）** | **159** | 见各行 `可转`（§A 105 + §B 30 + §C 24） |
| **可转(降级)（主体可转，函数体内有演出/日志调用需按 §E 处理）** | **27** | 见各行 `可转(降级)`，全部在 §A |
| **登记（主体行为依赖未建模子系统，本批不能转）** | **18** | 见各行 `登记`（§A 7 + §B 11），汇总于 §E |
| **未在插件中定义** | **4** | `dynamax?` / `mirrorHerbUsed` / `pbWeather`(Battler 上) / `pbRecoverHP?`（§F.1） |
| Ruby 内置 | 1 | `is_a?`（`Object#is_a?`） |
| 提取器伪影 | 4 | 同一符号的 `!=` 形式，已在 `可转` 中按原方法计一次 |

> A / B / C 的具体数字在 §A–§C 三张表末尾各自给出，§0.2 汇总。为避免口头合计出错，
> 每张表最后一行都写明了该表的分类计数。

### 0.1 四个「伪影」的认定依据（不是猜的）

`api-surface.mjs:13` 的正则是 `[a-z_][a-zA-Z0-9_]*[?!]?`，对 `battler.status!=PBStatuses::FROZEN`
会贪婪地吃掉 `status!`，把 `!=` 留在外面。我把四个名字逐一定位到真实行：

| 伪影名 | 真实写法 | 出处（行号） |
|---|---|---|
| `status!`（10 次） | `battler.status!=PBStatuses::X` | BattleHandlers_Items:229/242/255/338/369；BattleHandlers_Abilities:275/289/303/315/369 |
| `gender!`（2 次） | `user.gender!=2 && target.gender!=2` | BattleHandlers_Abilities:1057（一行两处） |
| `index!`（1 次） | `user.index!=target.index` | BattleHandlers_Abilities:1136 |
| `species!`（1 次） | `b.species!=battler.species` | PokeBall_CatchEffects:203 |

因此这四个**不是方法**，真实签名已在 `status` / `gender` / `index` / `species` 四行覆盖。

### 0.2 三表分类计数

| 表 | 可转 | 可转(降级) | 登记 | 未定义 | Ruby 内置 | 合计 |
|---|---:|---:|---:|---:|---:|---:|
| §A Battler | 105 | 27 | 7 | 4 | 0 | 143(实)+4(伪影) |
| §B Battle | 30 | 0 | 11 | 0 | 0 | 41 |
| §C Move | 24 | 0 | 0 | 0 | 1 | 25 |
| **合计** | **159** | **27** | **18** | **4** | **1** | **209** |

---

## 1. Java 落点约定（本笔记统一代号）

| 代号 | 实际文件 | 备注 |
|---|---|---|
| `Battler` | `pokemon-builder/runtime/core/src/main/java/pokemon/runtime/battle/Battler.java` | 现 389 行（10:20 时 377 行，队友并发改动中），`hp/stages/status/index/turnCount` 等字段已在 |
| `Battle` | `.../battle/Battle.java` | 现 1884 行（10:20 时 1663 行，队友并发改动中） |
| `BattleMove` | `.../battle/BattleMove.java` | 现 70 行，只有 `internalName/name/type/power/accuracy/priority/additionalChance/function/flags/statusMove/physical` |
| `BattleHandlers`(新) | `.../battle/BattleHandlers.java` | 需新建；`BattleHandlers.rb`(675 行) 的端口 |
| `PBEffects`(**已存在**) | `.../battle/PBEffects.java` | **M0 已落地**（2026-10-07 10:31 生成）：`PBEffects.Battler` / `.Position` / `.Side` / `.Field` 四组常量，逐条对 `PBEffects.rb:6-232`；已按原样保留 `Tearalament` 双定义(取 171) 与 `Side.DelusionSong=172` 两个坑 |
| `EffectMap`(**已存在**) | `.../battle/EffectMap.java` | **M0 已落地**（10:32 生成）：`@effects` 的存储，`truthy(int)` / `intVal(int)` 两个读法刻意区分 Ruby 的「真值」与「非零值」（Ruby 里 `0` 是 truthy） |
| `ActiveField`(新) | `.../battle/ActiveField.java` | `PokeBattle_ActiveField.rb:3-36` 的端口：`effects/defaultWeather/weather/weatherDuration/defaultTerrain/terrain/terrainDuration`（Java 仍无） |
| `ActiveSide`(新) | `.../battle/ActiveSide.java` | `PokeBattle_ActiveField.rb:38-73` 的 `PokeBattle_ActiveSide` / `PokeBattle_ActivePosition` 端口（Java 仍无） |
| `DamageState`(新) | `.../battle/DamageState.java` | `PokeBattle_DamageState.rb`(91 行) 的端口（Java 仍无；`Battle.java` 只在注释里提过它） |
| `PokeBattle_SceneConstants`(**已存在，但缺常量**) | `.../battle/PokeBattle_SceneConstants.java` | 已有 `NUM_BALLS` / 四个坐标 / `battlerPosition` / `trainerPosition`（对 `PokeBattle_SceneConstants.rb:8-59`）；**但没有 `USE_ABILITY_SPLASH`**，见 §F.3 |
| `Scene` | `.../battle/BattleScreen.java`（现 160 KB）| 演出侧，由 kimi27/Lead 管 |

**Java 侧现状核对（2026-10-07 10:35 现场 `Get-ChildItem` + grep，非记忆）：**
- **已有**：`PBEffects.java`、`EffectMap.java`、`PBWeather.java`、`PBBattleTerrains.java`、`PokeBattle_SceneConstants.java`、`BattleScreen.java`、`BattleAnimations.java`（其余见 `battle/` 目录）。
- **仍缺**：`ActiveField` / `ActiveSide` / `ActivePosition` / `DamageState` / `BattleHandlers`；grep `class (DamageState|ActiveField|ActiveSide)` 在 `core/src/main/java` 下 **0 命中**。
- **仍缺挂载点**：`Battler.java` / `Battle.java` 里 **没有 `effects`（EffectMap）字段、没有 `damageState` 字段、没有 weather/terrain 字段**（grep 只在 `Battle.java:705/742/837` 的注释里出现 `damageState` 字样）。
- `Battler.damageState`(39 次) 与 `Battler.effects`(109 次) 是使用频率最高的两个 Battler 成员，**M0 必须先把这两个挂上去**（类已就绪，缺的是 `Battler`/`Battle` 上的字段与访问器）。

---

## §A. Battler 上的方法（147 个入口 / 143 个真实名字，字母序）

列说明：`Java 落点建议 | Ruby 段:行号 | Ruby 原文（关键行） | 语义 | 依赖 | 可转/登记`

| Java 落点建议 | Ruby 段:行号 | Ruby 原文（截断） | 语义 | 依赖 | 可转/登记 |
|---|---|---|---|---|---|
| `Battler.ability` 字段 | PokeBattle_Battler:11 | `attr_accessor :ability` | 当前特性 ID | 被 `hasActiveAbility?`… 全量读取 | 可转 |
| `Battler.abilityName()` | PokeBattle_Battler:211 | `def abilityName; return PBAbilities.getName(@ability); end` | 特性显示名 | `PBAbilities` 名称表 | 可转 |
| `Battler.affectedByContactEffect?()` | PokeBattle_Battler:695-702 | `return false if fainted?` / `if hasActiveItem?(:PROTECTIVEPADS)` | 接触类效果是否生效（防尘护目镜） | `hasActiveItem?`、`itemName`、`pbDisplay` | 可转 |
| `Battler.affectedByPowder?()` | PokeBattle_Battler:656-682 | `return true if !NEWEST_BATTLE_MECHANICS` / `if pbHasType?(:GRASS)` / `hasActiveAbility?([:OVERCOAT,:DIVINEPACT])` | 粉末类招式是否生效 | `pbHasType?`、`hasActiveItem?`、`pbShowAbilitySplash`、`abilityName`、`itemName` | 可转(降级) |
| `Battler.airborne?()` | PokeBattle_Battler:591-602 | `return false if hasActiveItem?(:IRONBALL)` … `return true if pbHasType?(:FLYING)` … `hasActiveAbility?(:LEVITATE) && !@battle.moldBreaker` | 是否浮空 | `PBEffects::Ingrain/SmackDown/MagnetRise/Telekinesis`、`@battle.field.effects[Gravity]` | 可转（需 `ActiveField`） |
| `Battler.allAllies()` | PokeBattle_Battler:829-831 | `return @battle.allSameSideBattlers(@index).reject { \|b\| b.index == @index }` | 未倒下的同伴数组 | **`Battle#allSameSideBattlers` 不在 PokeBattle_Battle 里，而是插件在 AI_Move_EffectScores:3866-3870 猴补的**（见 §F.2） | 可转 |
| `Battler.asleep?()` | Battler_Statuses:314-316 | `return pbHasStatus?(PBStatuses::SLEEP)` | 是否睡眠（含 Comatose） | `pbHasStatus?`、`BattleHandlers.triggerStatusCheckAbilityNonIgnorable` | 可转 |
| `Battler.attack` 字段 | PokeBattle_Battler:15 | `attr_accessor :attack` | 当前攻击值（已含能力/道具修正，`pbUpdate` 时算好） | `Battler.pbUpdate` | 可转 |
| `Battler.battle` 字段 | PokeBattle_Battler:3 | `attr_reader :battle` | 所属 Battle | — | 可转（Java 用 `Battle` 反向引用/不持有） |
| `Battler.burned?()` | Battler_Statuses:390-392 | `return pbHasStatus?(PBStatuses::BURN)` | 是否灼伤 | `pbHasStatus?` | 可转 |
| `Battler.canHeal?()` | PokeBattle_Battler:684-688 | `return false if fainted? \|\| @hp>=@totalhp` / `return false if @effects[PBEffects::HealBlock]>0` | 能否被回复 HP | `PBEffects::HealBlock` | 可转 |
| `Battler.damageState` 字段 | PokeBattle_Battler:42 | `attr_accessor :damageState` | 本次伤害的结算状态对象（39 次读取） | `EffectMap`(已有 10:32) + 需新建 `DamageState`（`PokeBattle_DamageState.rb:3-24` 24 个字段） | 可转（需新建类） |
| `Battler.defense()` | PokeBattle_Battler:76-79 | `return @spdef if @battle.field.effects[PBEffects::WonderRoom]>0` / `return @defense` | 防御（奇迹空间下与特防互换） | `ActiveField.effects[WonderRoom]` | 可转 |
| `Battler.droppedBelowHalfHP` 字段 | PokeBattle_Battler:44 | `attr_accessor :droppedBelowHalfHP   # Used for Emergency Exit/Wimp Out` | 本回合是否跌破半血 | `pbAbilitiesOnDamageTaken` 写入 | 可转 |
| `Battler.dummy` 字段 | PokeBattle_Battler:22 | `attr_reader :dummy` | 是否占位假人（`pbInitDummyPokemon` 设置） | `Battler_Initialize:38-65` | 可转 |
| `Battler.eachAlly { }` | PokeBattle_Battler:823-827 | `yield b if b && !b.fainted? && !b.opposes?(@index) && b.index!=@index` | 遍历未倒下同伴（**排除自己**） | `@battle.battlers`、`opposes?` | 可转 |
| `Battler.eachMove { }` | PokeBattle_Battler:543-545 | `@moves.each { \|m\| yield m if m && m.id != 0 }` | 遍历非空招式槽（**保留槽位顺序**） | `@moves`（固定 4 格） | 可转 |
| `Battler.eachOpposing { }` | PokeBattle_Battler:833-835 | `yield b if b && !b.fainted? && b.opposes?(@index)` | 遍历未倒下对手 | `@battle.battlers`、`opposes?` | 可转 |
| `Battler.effectiveWeather()` | PokeBattle_Battler:856-861 | `ret = @battle.pbWeather` / `ret = PBWeather::None if weathers.include?(ret) && hasActiveItem?(:UTILITYUMBRELLA)` | 对自身有效的天气（万能伞屏蔽晴/雨） | `Battle.pbWeather`、`PBWeather` 常量 | 可转 |
| `Battler.effects` 字段 | PokeBattle_Battler:23 | `attr_accessor :effects` | 每个 battler 一份的 int 效果表（**109 次读取，最高频成员之一**） | **`PBEffects.Battler` + `EffectMap` 已就绪（2026-10-07 10:31/10:32），缺 `Battler` 上的字段** | 可转（需新建类） |
| `Battler.fainted?()` | PokeBattle_Battler:97-98 | `def fainted?; return @hp<=0; end` / `alias isFainted? fainted?` | HP<=0 | `@hp` | 可转（Java 已有 `fainted()`） |
| `Battler.form` 字段 | PokeBattle_Battler:62-67 | `attr_reader :form` / `def form=(value); @form=value; @pokemon.form=value if @pokemon; end` | 形态，**写时同步到 Pokemon** | `Pokemon_Forms:5-24` | 可转 |
| `Battler.gender` 字段 | PokeBattle_Battler:13 | `attr_accessor :gender` | 0=雄 1=雌 2=无性别 | — | 可转 |
| `Battler.hasActiveAbility?(a,ignoreFainted=false)` | PokeBattle_Battler:387-399 | `return false if !abilityActive?(ignoreFainted)` / `ability = getID(PBAbilities,ability)` / `return ability!=0 && ability==@ability` | 特性是否生效且匹配（**支持数组**） | `abilityActive?`、`getID`、`PBAbilities` | 可转 |
| `Battler.hasActiveItem?(item,ignoreFainted=false)` | PokeBattle_Battler:519-530 | `return false if !itemActive?(ignoreFainted)` / `item = getID(PBItems,item)` / `return item!=0 && item==@item` | 道具是否生效且匹配（**支持数组**） | `itemActive?`、`getID`、`PBItems` | 可转 |
| `Battler.hasAlteredStatStages?()` | Battler_StatStages:378-381 | `PBStats.eachBattleStat { \|s\| return true if @stages[s]!=0 }` | 是否有任何能力等级变动 | `PBStats.eachBattleStat` | 可转 |
| `Battler.hasUtilityUmbrella?()` | PokeBattle_Battler:610-613 | `return true if hasActiveItem?(:UTILITYUMBRELLA)` | 是否持有效果绝佳伞 | `hasActiveItem?` | 可转 |
| `Battler.hp` 字段 | PokeBattle_Battler:90-95 | `attr_reader :hp` / `def hp=(value); @hp=value.to_i; @pokemon.hp=value.to_i if @pokemon; end` | 当前 HP，**写时同步 Pokemon** | `Pokemon.hp=` | 可转 |
| `Battler.idxOpposingSide()` | PokeBattle_Battler:808-810 | `return (@index&1)^1` | 对手方 side 下标 | `@index` | 可转 |
| `Battler.idxOwnSide()` | PokeBattle_Battler:802-804 | `return @index&1` | 自己方 side 下标 | `@index` | 可转 |
| `Battler.inTwoTurnAttack?(*fn)` | PokeBattle_Battler:718-723 | `return false if @effects[PBEffects::TwoTurnAttack]==0` / `ttaFunction = pbGetMoveData(...,MOVE_FUNCTION_CODE)` / `arg.each { \|a\| return true if a==ttaFunction }` | 是否处于两回合招式的蓄力/攻击回合（按 function 码匹配） | `PBEffects::TwoTurnAttack`、`pbGetMoveData` | 可转 |
| `Battler.index` 字段 | PokeBattle_Battler:4 | `attr_accessor :index` | 场上位置 0..5（**奇偶=阵营**） | — | 可转（Java 已有 `index`） |
| `Battler.initialItem()` | PokeBattle_Battler:740-742 | `return @battle.initialItems[@index&1][@pokemonIndex]` | 进场时携带的道具 | `Battle.initialItems`、`pokemonIndex` | 可转 |
| `Battler.isCommander?()` | PokeBattle_Battler:866-869 | `commander = @effects[PBEffects::Commander]` / `return commander && commander.length == 1` | 是否是「指挥」的从者 | `PBEffects::Commander` | 可转 |
| `Battler.isSpecies?(s)` | PokeBattle_Battler:307-309 | `return @pokemon && @pokemon.isSpecies?(species)` | 种族判定 | `Pokemon#isSpecies?`（PokeBattle_Pokemon:668） | 可转 |
| `Battler.isUnnerved?()` | PokeBattle_Battler:577-583 | `return true if @battle.pbCheckOpposingAbility(:UNNERVE,@index)` … `:ASONEICE` / `:CONFESSIONLIST` / `:ASONEGHOST` | 是否被对手威慑（不能吃树果） | `Battle.pbCheckOpposingAbility` | 可转 |
| `Battler.item` 字段 | PokeBattle_Battler:69-74 | `attr_reader :item` / `def item=(value); @item=value; @pokemon.setItem(value) if @pokemon; end` | 携带道具，**写时同步 Pokemon** | `Pokemon#setItem` | 可转 |
| `Battler.itemActive?()` | PokeBattle_Battler:506-512 | `return false if fainted? && !ignoreFainted` / `return false if @effects[PBEffects::Embargo]>0` / `…MagicRoom` / `hasActiveAbility?(:KLUTZ,…)` | 道具是否生效 | `ActiveField.effects[MagicRoom]` | 可转 |
| `Battler.itemName()` | PokeBattle_Battler:212 | `def itemName; return PBItems.getName(@item); end` | 道具显示名 | `PBItems` 名称表 | 可转 |
| `Battler.lastRegularMoveUsed` 字段 | PokeBattle_Battler:33 | `attr_accessor :lastRegularMoveUsed` | 上一次用的常规招式 ID（再来一次/模仿等） | — | 可转 |
| `Battler.level` 字段 | PokeBattle_Battler:55-60 | `attr_reader :level` / `def level=(value); @level=value; @pokemon.level=value if @pokemon; end` | 等级，**写时同步 Pokemon** | `Pokemon.level=` | 可转 |
| `Battler.movedThisRound?()` | PokeBattle_Battler:704-706 | `return @lastRoundMoved && @lastRoundMoved==@battle.turnCount` | 本回合是否已行动 | `Battle.turnCount`（Java 是 `turns()`） | 可转 |
| `Battler.moves` 字段 | PokeBattle_Battler:12 | `attr_accessor :moves` | 4 个 `PokeBattle_Move`（不是 PBMove） | `PokeBattle_Move.pbFromPBMove` | 可转 |
| `Battler.near?(i)` | PokeBattle_Battler:790-793 | `i = i.index if i.respond_to?("index")` / `return @battle.nearBattlers?(@index,i)` | 三方对战中的「相邻」判定 | `Battle.nearBattlers?`（PokeBattle_Battle:544-568） | 可转 |
| `Battler.num_fainted_allies()` | PokeBattle_Battler:514-516 | `return @battle.pbFaintedAllyCount(self)` | 本方已倒下数 | `Battle.pbFaintedAllyCount`（PokeBattle_Battle:833） | 可转 |
| `Battler.opposes?(i=0)` | PokeBattle_Battler:784-787 | `i = i.index if i.respond_to?("index")` / `return (@index&1)!=(i&1)` | 是否敌对（**可传 Battler 或 int**） | `@index` | 可转 |
| `Battler.pbAbilitiesOnDamageTaken(oldHP,newHP=-1)` | Battler_AbilityAndItem:67-73 | `return false if !abilityActive?` / `return false if oldHP<@totalhp/2 \|\| newHP>=@totalhp/2` / `BattleHandlers.triggerAbilityOnHPDroppedBelowHalf(...)` | 跌破半血触发（ Emergency Exit / Wimp Out），返回是否换下 | `BattleHandlers` 端口 | 可转 |
| `Battler.pbAbilitiesOnSwitchOut()` | Battler_AbilityAndItem:41-52 | `BattleHandlers.triggerAbilityOnSwitchOut(...)` / `@battle.peer.pbOnLeavingBattle(...)` / `@hp=0; @fainted=true` / `@battle.pbEndPrimordialWeather` | 换下时的特性收尾 | **`@battle.peer.pbOnLeavingBattle`（BattlePeer/存档子系统，`PokeBattle_BattlePeer.rb:7`）** | 登记 |
| `Battler.pbAbilityOnTerrainChange(ability_changed=false)` | Battler_AbilityAndItem:75-78 | `return if !abilityActive?` / `BattleHandlers.triggerAbilityOnTerrainChange(...)` | 场地变化时触发特性 | `BattleHandlers` 端口 | 可转 |
| `Battler.pbAttract(user,msg=nil)` | Battler_Statuses:598-610 | `@effects[PBEffects::Attract] = user.index` / `@battle.pbCommonAnimation("Attract",self)` / `if hasActiveItem?(:DESTINYKNOT) && user.pbCanAttract?(self,false)` | 陷入着迷（含红线连锁） | `pbCommonAnimation`（登记）、`pbItemStatusCureCheck`、`pbAbilityStatusCureCheck` | 可转(降级) |
| `Battler.pbBurn(user=nil,msg=nil)` | Battler_Statuses:402-404 | `pbInflictStatus(PBStatuses::BURN,0,msg,user)` | 灼伤 | `pbInflictStatus`（:255-309，内部 `pbCommonAnimation("Burn")`） | 可转(降级) |
| `Battler.pbCanAttract?(user,showMessages=true)` | Battler_Statuses:554-596 | `return false if !user \|\| user.fainted?` / `agender=user.gender; ogender=gender` / `if agender==2 \|\| ogender==2 \|\| agender==ogender` / `hasActiveAbility?([:AROMAVEIL,:OBLIVIOUS])` | 能否着迷（性别/特性判定） | `pbShowAbilitySplash`/`pbHideAbilitySplash`（登记） | 可转(降级) |
| `Battler.pbCanBurn?(user,showMessages,move=nil)` | Battler_Statuses:394-396 | `return pbCanInflictStatus?(PBStatuses::BURN,user,showMessages,move)` | 能否灼伤 | `pbCanInflictStatus?` | 可转 |
| `Battler.pbCanBurnSynchronize?(target)` | Battler_Statuses:398-400 | `return pbCanSynchronizeStatus?(PBStatuses::BURN,target)` | 同步特性扩散灼伤判定 | `pbCanSynchronizeStatus?` | 可转 |
| `Battler.pbCanConfuse?(user=nil,showMessages=true,move=nil,selfInflicted=false)` | Battler_Statuses:488-525 | `return false if fainted?` / `if @effects[PBEffects::Confusion]>0` / `hasActiveAbility?([:OWNTEMPO,:DEMONKILLER])` | 能否混乱 | `pbShowAbilitySplash`、`abilityName` | 可转(降级) |
| `Battler.pbCanConsumeBerry?(_item,alwaysCheckGluttony=true)` | Battler_AbilityAndItem:152-159 | `return false if isUnnerved?` / `return true if @hp<=@totalhp/4` / `return true if @hp<=@totalhp/2 && hasActiveAbility?(:GLUTTONY)` | 能否吃树果 | `isUnnerved?` | 可转 |
| `Battler.pbCanLowerStatStage?(stat,user=nil,move=nil,showFailMsg=false,ignoreContrary=false)` | Battler_StatStages:130-170 | `return false if fainted?` / `if hasActiveAbility?(:CONTRARY)` / `if !user \|\| user.index!=@index` / Mist / `BattleHandlers.triggerStatLossImmunity*` | 能否降能力 | `BattleHandlers`、`ActiveSide.effects[Mist]`、`Substitute` | 可转 |
| `Battler.pbCanParalyze?(user,showMessages,move=nil)` | Battler_Statuses:413-415 | `return pbCanInflictStatus?(PBStatuses::PARALYSIS,user,showMessages,move)` | 能否麻痹 | `pbCanInflictStatus?` | 可转 |
| `Battler.pbCanParalyzeSynchronize?(target)` | Battler_Statuses:417-419 | `return pbCanSynchronizeStatus?(PBStatuses::PARALYSIS,target)` | 同步扩散麻痹判定 | `pbCanSynchronizeStatus?` | 可转 |
| `Battler.pbCanPoison?(user,showMessages,move=nil)` | Battler_Statuses:375-377 | `return pbCanInflictStatus?(PBStatuses::POISON,user,showMessages,move)` | 能否中毒 | `pbCanInflictStatus?` | 可转 |
| `Battler.pbCanPoisonSynchronize?(target)` | Battler_Statuses:379-381 | `return pbCanSynchronizeStatus?(PBStatuses::POISON,target)` | 同步扩散中毒判定 | `pbCanSynchronizeStatus?` | 可转 |
| `Battler.pbCanRaiseStatStage?(stat,user=nil,move=nil,showFailMsg=false,ignoreContrary=false)` | Battler_StatStages:9-26 | `if hasActiveAbility?(:CONTRARY) && !ignoreContrary && !@battle.moldBreaker` / `BattleHandlers.triggerStatGainImmunityAbility(...)` / `if statStageAtMax?(stat)` | 能否升能力（**40 次调用**） | `BattleHandlers`、`pbThis`、`PBStats.getName` | 可转 |
| `Battler.pbCanSleep?(user,showMessages,move=nil,ignoreStatus=false)` | Battler_Statuses:318-320 | `return pbCanInflictStatus?(PBStatuses::SLEEP,user,showMessages,move,ignoreStatus)` | 能否睡眠 | `pbCanInflictStatus?` | 可转 |
| `Battler.pbChangeFormTransform(newForm,msg)` | Battler_ChangeSelf:182-195 | `oldDmg = @totalhp-@hp` / `self.form = newForm` / `pbUpdate(true)` / `@hp = @totalhp-oldDmg` / `@battle.scene.pbChangePokemonTransform(self,@pokemon)` / `@battle.scene.pbRefreshOne(@index)` / `@battle.pbDisplay(msg)` / `@battle.pbSetSeen(self)` | 变身（**保持 HP 绝对差**） | **`@battle.scene.pbChangePokemonTransform` 是 `Transform Mosaic` 插件对 `PokeBattle_Scene` 的猴补（Transform Mosaic:66-67）**、`scene.pbRefreshOne`、`pbSetSeen`(图鉴存档) | 登记 |
| `Battler.pbChangeTypes(newType)` | Battler_ChangeSelf:285-314（**第二定义覆盖** :157-177 的第一定义） | `if newType.is_a?(PokeBattle_Battler)` / `elsif newType.is_a?(Array)` / `@effects[PBEffects::Type3] = newType3` | 改属性。**同段内被定义两次，Ruby 后者胜出** | `PBEffects::Type3/BurnUp/Roost` | 可转 |
| `Battler.pbCheckFormOnStatusChange()` | Battler_ChangeSelf:197-215 | `if isSpecies?(:SHAYMIN) && frozen?` / `elsif isSpecies?(:ROSEDRAGON) && hasActiveAbility?(:SLEEPSOUNDLY)` | 状态变化时的形态检查 | `pbChangeFormTransform`（登记） | 可转(降级) |
| `Battler.pbCheckFormOnWeatherChange()` | Battler_ChangeSelf:227-280 | `return if hasUtilityUmbrella?` / `isSpecies?(:CASTFORM)` / `isSpecies?(:CHERRIM)` / `:EISCUE` + `:ICEFACE` / `hasActiveAbility?(:PROTOSYNTHESIS)` | 天气形态检查（飘浮泡泡/樱花儿/冰砌鹅） | `Battle.pbWeather`、`pbShowAbilitySplash(self,true)` | 可转(降级) |
| `Battler.pbConfuse(msg=nil)` | Battler_Statuses:531-540 | `@effects[PBEffects::Confusion] = pbConfusionDuration` / `@battle.pbCommonAnimation("Confusion",self)` / `pbItemStatusCureCheck` / `pbAbilityStatusCureCheck` | 陷入混乱 | `pbCommonAnimation`（登记）、`pbConfusionDuration` | 可转(降级) |
| `Battler.pbConsumeItem(recoverable=true,symbiosis=true,belch=true)` | Battler_AbilityAndItem:170-180 | `setRecycleItem(@item)` / `@effects[PBEffects::PickupItem]=@item` / `setBelched if belch && pbIsBerry?(@item)` / `pbRemoveItem` / `pbSymbiosis if symbiosis` | 消耗道具（可回收→回收道具表） | `PBDebug.log`、`Battle.nextPickupUse`、`pbIsBerry?` | 可转(降级) |
| `Battler.pbCureAttract()` | Battler_Statuses:612-614 | `@effects[PBEffects::Attract] = -1` | 解除着迷 | `PBEffects::Attract` | 可转 |
| `Battler.pbCureConfusion()` | Battler_Statuses:547-549 | `@effects[PBEffects::Confusion] = 0` | 解除混乱 | `PBEffects::Confusion` | 可转 |
| `Battler.pbCureStatus(showMessages=true)` | Battler_Statuses:468-483 | `oldStatus = status` / `self.status = PBStatuses::NONE` / `case oldStatus … @battle.pbDisplay(_INTL("{1}醒来了！",pbThis))` | 治愈异常状态（按旧状态给文案） | `status=`（内部 `@battle.scene.pbRefreshOne`）、`PBDebug.log` | 可转(降级) |
| `Battler.pbDirectOpposing(unfaintedOnly=false)` | PokeBattle_Battler:842-854 | `@battle.pbGetOpposingIndicesInOrder(@index).each …` / `return @battle.battlers[(@index^1)]` | 最直接的对面目标 | `Battle.pbGetOpposingIndicesInOrder` | 可转 |
| `Battler.pbFaint(showMessage=true)` | Battler_ChangeSelf:61-99 与 :105-127（**第二定义 alias `paldea_pbFaint` 包住第一定义**） | `@battle.scene.pbFaintBattler(self)` / `pbCatchBossPokemon(self) if @battle.decision==0` / `@battle.peer.pbOnLeavingBattle(...)` / `batSprite = @battle.scene.sprites["pokemon_#{…}"]` | 倒下：清效果/清状态/掉亲密度/重置形态/清选择/触发他人特性/检查原始天气/计倒下数 | **`@battle.scene.pbFaintBattler`（Scene_Animations:299）**、**`pbCatchBossPokemon`（PokeBattle_BOSS:157，BOSS 子系统）**、**`@battle.peer.pbOnLeavingBattle`（存档）**、`@battle.scene.sprites[...]` | 登记 |
| `Battler.pbHasAnyStatus?()` | Battler_Statuses:18-23 | `if BattleHandlers.triggerStatusCheckAbilityNonIgnorable(@ability,self,nil)` / `return @status!=PBStatuses::NONE` | 是否有任意异常状态 | `BattleHandlers` | 可转 |
| `Battler.pbHasOtherType?(type)` | PokeBattle_Battler:357-363 | `activeTypes = pbTypes(true)` / `activeTypes.reject! { \|t\| t==type }` / `return activeTypes.length>0` | 除某属性外是否还有别的属性 | `pbTypes`、`getConst` | 可转 |
| `Battler.pbHasType?(type)` | PokeBattle_Battler:350-355 | `type = getConst(PBTypes,type) if type.is_a?(Symbol) \|\| type.is_a?(String)` / `return activeTypes.include?(type)` | 是否有某属性（**接受 Symbol/String/int**） | `pbTypes(true)` | 可转 |
| `Battler.pbHeldItemTriggerCheck(forcedItem=0,fling=false)` | Battler_AbilityAndItem:235-249 | `return if fainted?` / `return if forcedItem==0 && !itemActive?` / `pbItemHPHealCheck` / `pbItemStatusCureCheck` / `pbItemEndOfMoveCheck` / `BattleHandlers.triggerTargetItemOnHitPositiveBerry` | 道具触发总闸 | `BattleHandlers` 端口 | 可转 |
| `Battler.pbHeldItemTriggered(thisItem,forcedItem=0,fling=false)` | Battler_AbilityAndItem:207-225 | `if hasActiveAbility?(:CHEEKPOUCH) && pbIsBerry?(thisItem) && canHeal?` / `if hasActiveAbility?(:CUDCHEW) && pbIsBerry?(item_to_use) && fling` / `pbConsumeItem if forcedItem<=0` | 道具被吃/被消耗后的连锁（颊袋/反刍） | **`item_to_use` 在该方法内从未定义（参数名是 `thisItem`）→ 拥有反刍特性的宝可梦会 `NoMethodError`（§F.1）** | 登记 |
| `Battler.pbItemHPHealCheck(forcedItem=0,fling=false)` | Battler_AbilityAndItem:253-261 | `thisItem = (forcedItem>0) ? forcedItem : @item` / `if BattleHandlers.triggerHPHealItem(...)` / `elsif forcedItem==0` / `pbItemTerrainStatBoostCheck` | 回复类道具检查 | `BattleHandlers` 端口 | 可转 |
| `Battler.pbItemOnIntimidatedCheck()` | Battler_AbilityAndItem:318-323 | `return if !itemActive?` / `if BattleHandlers.triggerItemOnIntimidated(@item,self,@battle)` | 威吓触发道具（胆量之珠） | `BattleHandlers` 端口 | 可转 |
| `Battler.pbItemOpposingStatGainCheck(statUps,item_to_use=0)` | Battler_AbilityAndItem:333-340 | `itm = (item_to_use != 0) ? item_to_use : self.item` / `BattleHandlers.triggerItemOnOpposingStatGain(...)` | 对手升能力时触发（模仿香草） | `BattleHandlers` 端口 | 可转 |
| `Battler.pbItemStatRestoreCheck(forcedItem=0,fling=false)` | Battler_AbilityAndItem:295-302 | `if BattleHandlers.triggerEndOfMoveStatRestoreItem(...)` / `pbHeldItemTriggered(...)` | 能力恢复道具（白色香草） | `BattleHandlers` 端口 | 可转 |
| `Battler.pbItemStatusCureCheck(forcedItem=0,fling=false)` | Battler_AbilityAndItem:267-274 | `if BattleHandlers.triggerStatusCureItem(...)` / `pbHeldItemTriggered(...)` | 状态治愈道具（万能药类树果/精神香草） | `BattleHandlers` 端口 | 可转 |
| `Battler.pbItemTerrainStatBoostCheck()` | Battler_AbilityAndItem:307-312 | `if BattleHandlers.triggerTerrainStatBoostItem(@item,self,@battle)` / `pbHeldItemTriggered(@item)` | 场地种子道具 | `BattleHandlers` 端口 | 可转 |
| `Battler.pbLowerAttackStatStageIntimidate(user)` | Battler_StatStages:309-373 | `return false if fainted?` / `if @effects[PBEffects::Substitute]>0 …` / `hasActiveAbility?(:INNERFOCUS) \|\| :OWNTEMPO \|\| :OBLIVIOUS \|\| :SCRAPPY \|\| :DEMONKILLER \|\| :FEARLESS` / `pbRaiseStatStageByAbility(PBStats::SPEED,1,self) if hasActiveAbility?(:RATTLED)` | 威吓专用降攻（含专门文案/白雾/替身/同伴特性保护） | `BattleHandlers`、`pbShowAbilitySplash`、`USE_ABILITY_SPLASH` 分支 | 可转(降级) |
| `Battler.pbLowerStatStage(stat,increment,user,showAnim=true,ignoreContrary=false,ignoreMirrorArmor=false)` | Battler_StatStages:191-235 | Mirror Armor 段（:194-214）/ Contrary 段 / `@battle.pbCommonAnimation("StatDown",self)` / `BattleHandlers.triggerAbilityOnStatLoss` / `@effects[PBEffects::LashOut]=true` | 降能力 | `BattleHandlers`、`pbShowAbilitySplash`、`pbCommonAnimation` | 可转(降级) |
| `Battler.pbLowerStatStageByAbility(stat,increment,user,splashAnim=true,checkContact=false)` | Battler_StatStages:289-306 | `if hasActiveAbility?([:WATCHDOGEYE,:GUARDDOG]) && isConst?(user.ability,PBAbilities,:INTIMIDATE)` / `@battle.pbShowAbilitySplash(user) if splashAnim` / `ret = pbLowerStatStage(...)` | 由特性触发的降能力（含看门狗反向升） | `pbShowAbilitySplash`/`pbHideAbilitySplash`（登记） | 可转(降级) |
| `Battler.pbLowerStatStageByCause(stat,increment,user,cause,showAnim=true,ignoreContrary=false,ignoreMirrorArmor=false)` | Battler_StatStages:237-287 | 同 `pbLowerStatStage` 但文案带 `cause` | 带来源名文案的降能力 | 同左 | 可转(降级) |
| `Battler.pbOnAbilityChanged(oldAbil)` | Battler_AbilityAndItem:132-147 | `if @effects[PBEffects::Illusion] && isConst?(oldAbil,PBAbilities,:ILLUSION)` / `@battle.scene.pbChangePokemon(self,@pokemon)` / `pbCheckFormOnWeatherChange` / `@battle.pbEndPrimordialWeather` | 特性变化后的收尾（幻觉解除/形态回退） | **`@battle.scene.pbChangePokemon`（Graphics 精灵重绘）** | 登记 |
| `Battler.pbOpposingSide()` | PokeBattle_Battler:818-820 | `return @battle.sides[idxOpposingSide]` | 对手方 side 数据 | `Battle.sides`、**需新建 `ActiveSide`** | 可转 |
| `Battler.pbOpposingTeam(lowerCase=false)` | PokeBattle_Battler:240-245 | `if opposes? … _INTL("我方队伍") …` | 「对方队伍」/「我方队伍」文案（**注意语义取反**） | `opposes?` | 可转 |
| `Battler.pbOwnSide()` | PokeBattle_Battler:813-815 | `return @battle.sides[idxOwnSide]` | 自己方 side 数据 | `Battle.sides`、**需新建 `ActiveSide`** | 可转 |
| `Battler.pbOwnedByPlayer?()` | PokeBattle_Battler:796-798 | `return @battle.pbOwnedByPlayer?(@index)` | 是否玩家自己的宝可梦 | `Battle.pbOwnedByPlayer?`（:287） | 可转 |
| `Battler.pbParalyze(user=nil,msg=nil)` | Battler_Statuses:421-423 | `pbInflictStatus(PBStatuses::PARALYSIS,0,msg,user)` | 麻痹 | `pbInflictStatus`（内部 `pbCommonAnimation("Paralysis")`） | 可转(降级) |
| `Battler.pbPoison(user=nil,msg=nil,toxic=false)` | Battler_Statuses:383-385 | `pbInflictStatus(PBStatuses::POISON,(toxic) ? 1 : 0,msg,user)` | 中毒/剧毒 | `pbInflictStatus`（内部 `pbCommonAnimation("Poison"/"Toxic")`） | 可转(降级) |
| `Battler.pbRaiseStatStage(stat,increment,user,showAnim=true,ignoreContrary=false)` | Battler_StatStages:47-72 | `return false if !PBStats.validBattleStat?(stat)` / `increment = pbRaiseStatStageBasic(...)` / `@battle.pbCommonAnimation("StatUp",self) if showAnim` / `arrStatTexts[[increment-1,2].min]` / `if !@mirrorHerbUsed && !(hasActiveAbility?(:CONTRARY)…)` / `addSideStatUps(stat,increment)` | 升能力（含 3 档文案：提升/大幅/巨幅） | `BattleHandlers.triggerAbilityOnStatGain`、`pbCommonAnimation`、`@mirrorHerbUsed`（§F.1） | 可转(降级) |
| `Battler.pbRaiseStatStageByAbility(stat,increment,user,splashAnim=true)` | Battler_StatStages:108-122 | `@battle.pbShowAbilitySplash(user) if splashAnim` / `if pbCanRaiseStatStage?(stat,user,nil,USE_ABILITY_SPLASH)` / `ret = pbRaiseStatStage(...)` / `@battle.pbHideAbilitySplash(user) if splashAnim` / `pbMirrorStatUpsOpposing` | 特性触发的升能力（**32 次调用，第 12 高频**） | `pbShowAbilitySplash`/`pbHideAbilitySplash`（登记）、`pbMirrorStatUpsOpposing` | 可转(降级) |
| `Battler.pbRaiseStatStageByCause(stat,increment,user,cause,showAnim=true,ignoreContrary=false)` | Battler_StatStages:74-106 | 同 `pbRaiseStatStage`，文案分流 `if user.index==@index`（带 cause） | 带来源名文案的升能力 | 同左 | 可转(降级) |
| `Battler.pbRecoverHP(amt,anim=true,anyAnim=true)` | Battler_ChangeSelf:19-31 | `amt = @totalhp-@hp if amt>@totalhp-@hp` / `amt = 1 if amt<1 && @hp<@totalhp` / `self.hp += amt` / `@battle.scene.pbHPChanged(self,oldHP,anim) if anyAnim && amt>0` / `self.yamaskhp = 0` | 回血（**至少回 1**） | `scene.pbHPChanged`（Scene_Animations:211）、`PBDebug.log`、`yamaskhp` | 可转(降级) |
| `Battler.pbReduceHP(amt,anim=true,registerDamage=true,anyAnim=true)` | Battler_ChangeSelf:5-17 | `amt = amt.round` / `amt = @hp if amt>@hp` / `amt = 1 if amt<1 && !fainted?` / `raise _INTL("HP小于0") if @hp<0` / `@battle.scene.pbHPChanged(self,oldHP,anim)` / `@tookDamage = true if amt>0 && registerDamage` | 扣血（**至少扣 1，并 raise 断言**） | `scene.pbHPChanged`、`PBDebug.log` | 可转(降级) |
| `Battler.pbResetStatStages()` | Battler_StatStages:393-403 | `PBStats.eachBattleStat do \|s\|` / `if @stages[s] > 0 → @statsLoweredThisRound=true; @statsDropped=true` / `elsif <0 → @statsRaisedThisRound=true` / `@stages[s]=0` | 清空能力等级（**同时翻转本回合标记**） | `PBStats.eachBattleStat` | 可转 |
| `Battler.pbSleep(msg=nil)` | Battler_Statuses:354-356 | `pbInflictStatus(PBStatuses::SLEEP,pbSleepDuration,msg)` | 睡眠（随机 2+0..2 回合） | `pbSleepDuration`、`pbInflictStatus` | 可转(降级) |
| `Battler.pbSleepSelf(msg=nil,duration=-1)` | Battler_Statuses:358-360 | `pbInflictStatus(PBStatuses::SLEEP,pbSleepDuration(duration),msg)` | 使自己睡眠（固定回合可选） | 同上 | 可转(降级) |
| `Battler.pbSymbiosis()` | Battler_AbilityAndItem:182-205 | `return if fainted?` / `return if @item!=0` / `next if !b.hasActiveAbility?(:SYMBIOSIS)` / `next if b.item==0 \|\| b.unlosableItem?(b.item)` / `self.item = b.item; b.item = 0` | 共生（同伴把道具塞给自己） | `Battle.pbPriority`、`pbShowAbilitySplash`、`unlosableItem?` | 可转(降级) |
| `Battler.pbTeam(lowerCase=false)` | PokeBattle_Battler:233-238 | `if opposes? … _INTL("对方队伍") …` | 「对方队伍」/「我方队伍」文案 | `opposes?` | 可转 |
| `Battler.pbThis(lowerCase=false)` | PokeBattle_Battler:214-231 | `if opposes?` / `if @battle.trainerBattle? → _INTL("对手的{1}",name)` / `elsif @pokemon && @pokemon.battleRank > 1 → "强大的{1}" / "特殊的{1}"` / `else → _INTL("野生的{1}",name)` / `elsif !pbOwnedByPlayer? → _INTL("队友的{1}",name)` / `return name` | 说话时对宝可梦的称呼前缀（**308 次，最高频**）。`lowerCase` 参数在本插件里三分支返回同一字符串（作者留的坑，照抄即可） | `opposes?`、`@battle.trainerBattle?`、`@pokemon.battleRank`（BOSS 等级）、`pbOwnedByPlayer?`、`name`（含幻觉） | 可转 |
| `Battler.pbTransform(target)` | Battler_ChangeSelf:418-446 | `@effects[PBEffects::Transform]=true` / `@effects[PBEffects::TransformSpecies]=target.species` / `pbChangeTypes(target)` / 复制六围/能力等级/招式（**PP 与最大 PP 都设为 5**） / `@battle.scene.pbRefreshOne(@index)` / `pbOnAbilityChanged(oldAbil)` | 变身 | **`@battle.scene.pbRefreshOne`**、`PokeBattle_Move.pbFromPBMove`（招式子类工厂，需 function 码→类名映射） | 登记 |
| `Battler.pbTypes(withType3=false)` | PokeBattle_Battler:314-348 | `ret=[@type1]; ret.push(@type2) if @type2!=@type1` / `LoseGrassType` / `LoseFireType` / `LoseWaterType` / `DoubleShock` / `BurnUp` / `Roost`（掉飞行后无属性则补普通）/ `Type3` | 当前有效属性数组（**本插件新增 5 个去属性效果**） | `PBEffects::LoseGrassType/LoseFireType/LoseWaterType/DoubleShock/BurnUp/Roost/Type3` | 可转 |
| `Battler.pbUpdate(fullChange=false)` | Battler_Initialize:362-383 | `@pokemon.calcStats` / `rank = BOSS_HP_RANK[@pokemon.battleRank]` / `@totalhp=@pokemon.totalhp*rank` / `@hp=@pokemon.hp*rank` / 复制六围 / `fullChange` 时复制属性与特性 | 重算战斗内数值 | `BOSS_HP_RANK`（`PokeBattle_BOSS.rb:3`，**BOSS 子系统的常量，但只是 8 项数组**） | 可转 |
| `Battler.pbWeather` | **未在插件中定义** | 调用点 BattleHandlers_Abilities:4581 `target.pbWeather` | 插件本意应是 `target.battle.pbWeather` 或 `target.effectiveWeather` | — | 未定义（§F.1） |
| `Battler.pbWeight()` | PokeBattle_Battler:281-292 | `ret = (@pokemon) ? @pokemon.weight : 500` / `ret += @effects[PBEffects::WeightChange]` / `BattleHandlers.triggerWeightCalcAbility` / `triggerWeightCalcItem` | 体重 | `BattleHandlers` 端口、`Pokemon.weight` | 可转 |
| `Battler.plainStats()` | PokeBattle_Battler:297-305 | `ret[PBStats::ATTACK]=self.attack` … `ret[PBStats::SPEED]=self.speed` | 攻/防/特攻/特防/速 的原始数组 | `PBStats` 索引 | 可转 |
| `Battler.pokemon` 字段 | PokeBattle_Battler:6 | `attr_reader :pokemon` | 底层 `PokeBattle_Pokemon`（可为 nil＝假人） | — | 可转（Java 用 `Pokemon`，`Battler.pokemon` 是 final 字段） |
| `Battler.poisoned?()` | Battler_Statuses:371-373 | `return pbHasStatus?(PBStatuses::POISON)` | 是否中毒 | `pbHasStatus?` | 可转 |
| `Battler.paralyzed?()` | Battler_Statuses:409-411 | `return pbHasStatus?(PBStatuses::PARALYSIS)` | 是否麻痹 | `pbHasStatus?` | 可转 |
| `Battler.recycleItem()` | PokeBattle_Battler:748-750 | `return @battle.recycleItems[@index&1][@pokemonIndex]` | 可回收道具 | `Battle.recycleItems` | 可转 |
| `Battler.reborn?()` | PokeBattle_Battler:773-775 | `return @battle.rebirth[@index & 1][@pokemonIndex]` | 复活标记（**本插件新增**） | `Battle.rebirth` | 可转 |
| `Battler.semiInvulnerable?()` | PokeBattle_Battler:725-727 | `return inTwoTurnAttack?("0C9","0CA","0CB","0CC","0CD","0CE","14D")` | 是否半无敌（挖洞/潜水/飞空等） | `inTwoTurnAttack?`、function 码表 | 可转 |
| `Battler.setInitialItem(newItem)` | PokeBattle_Battler:744-746 | `@battle.initialItems[@index&1][@pokemonIndex] = newItem` | 写进场道具记录 | `Battle.initialItems` | 可转 |
| `Battler.setReborn()` | PokeBattle_Battler:777-779 | `@battle.rebirth[@index & 1][@pokemonIndex] = true` | 打复活标记 | `Battle.rebirth` | 可转 |
| `Battler.setRecycleItem(newItem)` | PokeBattle_Battler:752-754 | `@battle.recycleItems[@index&1][@pokemonIndex] = newItem` | 写回收道具 | `Battle.recycleItems` | 可转 |
| `Battler.species` 字段 | PokeBattle_Battler:8 | `attr_accessor :species` | 种族（**变身时由 Pokemon 同步，不是 `pokemon.species`**） | — | 可转 |
| `Battler.speed` 字段 | PokeBattle_Battler:17 | `attr_accessor :speed` | 速度（已含修正） | — | 可转 |
| `Battler.spdef()` | PokeBattle_Battler:83-86 | `return @defense if @battle.field.effects[PBEffects::WonderRoom]>0` / `return @spdef` | 特防（奇迹空间互换） | `ActiveField.effects[WonderRoom]` | 可转 |
| `Battler.stages` 字段 | PokeBattle_Battler:18 | `attr_accessor :stages` | 能力等级数组（索引 `PBStats::*`） | `PBStats` | 可转（Java `stages[5]`） |
| `Battler.statStageAtMax?(stat)` | Battler_StatStages:5-7 | `return @stages[stat]>=6` | 是否已达 +6 | `@stages` | 可转 |
| `Battler.statStageAtMin?(stat)` | Battler_StatStages:126-128 | `return @stages[stat]<=-6` | 是否已达 −6 | `@stages` | 可转 |
| `Battler.status` 字段 | PokeBattle_Battler:100-109 | `def status=(value)` / `@effects[PBEffects::Truant]=false if @status==SLEEP && value!=SLEEP` / `@effects[PBEffects::Toxic]=0 if value!=POISON` / `@battle.scene.pbRefreshOne(@index)` | 异常状态，**写时联动 Truant/Toxic 并刷数据盒** | `scene.pbRefreshOne` | 可转(降级) |
| `Battler.statusCount` 字段 | PokeBattle_Battler:111-117 | `attr_reader :statusCount` / `def statusCount=(value)` / `@battle.scene.pbRefreshOne(@index)` | 睡眠回合 / 剧毒计数 | `scene.pbRefreshOne` | 可转(降级) |
| `Battler.statsRaisedThisRound` 字段 | PokeBattle_Battler:48 | `attr_accessor :statsRaisedThisRound` | 本回合是否升过能力 | `pbResetStatStages` 写入 | 可转 |
| `Battler.takesIndirectDamage?()` | PokeBattle_Battler:615-630 | `return false if fainted?` / `if hasActiveAbility?(:MAGICGUARD)` / `@battle.pbShowAbilitySplash(self)` / `@battle.pbDisplay(_INTL("{1}没有受到影响！",pbThis))` | 是否吃间接伤害（魔法守护） | `pbShowAbilitySplash`/`pbHideAbilitySplash`、`USE_ABILITY_SPLASH` | 可转(降级) |
| `Battler.totalhp` 字段 | PokeBattle_Battler:19 | `attr_reader :totalhp` | 最大 HP（**已乘 BOSS 倍率**） | `pbUpdate`、`BOSS_HP_RANK` | 可转 |
| `Battler.turnCount` 字段 | PokeBattle_Battler:25 | `attr_accessor :turnCount` | 该宝可梦在场的回合数 | — | 可转（Java 已有 `turnCount`） |
| `Battler.type1` 字段 | PokeBattle_Battler:9 | `attr_accessor :type1` | 属性 1 | — | 可转 |
| `Battler.type2` 字段 | PokeBattle_Battler:10 | `attr_accessor :type2` | 属性 2 | — | 可转 |
| `Battler.ungainableAbility?(abil=nil)` | PokeBattle_Battler:445-488 | `abil = @ability if !abil` / `abilityBlacklist = [:BATTLEBOND,:DISGUISE,:FLAMEVEIL,:FLOWERGIFT,:FORECAST,:MULTITYPE,…]` | 能否被赋予该特性 | `isConst?` | 可转 |
| `Battler.unlosableItem?(check_item)` | PokeBattle_Battler:534-541 | `return false if check_item<=0` / `return true if pbIsMail?(check_item)` / `return true if @pokemon && @pokemon.getMegaForm(true) > 0` / `return pbIsUnlosableItem?(...)` | 道具是否不可被夺 | `pbIsMail?`、`getMegaForm`、`pbIsUnlosableItem?` | 可转 |
| `Battler.uncopyableAbility?(abil=nil)` | PokeBattle_Battler:490-503 | `abil = @ability_id if !abil` ← **`@ability_id` 从未定义**（§F.1） | 能否被复制（无参时永远返回 false） | `ungainableAbility?` | 登记 |
| `Battler.unstoppableAbility?(abil=nil)` | PokeBattle_Battler:403-442 | `abilityBlacklist = [:BATTLEBOND,:DISGUISE,:FLAMEVEIL,:MULTITYPE,:POWERCONSTRUCT,:SCHOOLING,…]` | 特性是否不可被压制/替换 | `isConst?` | 可转 |
| `Battler.status!`（伪影） | — | 实为 `status!=` 比较 | 见 §0.1 | — | 伪影 |
| `Battler.gender!`（伪影） | — | 实为 `gender!=` 比较 | 见 §0.1 | — | 伪影 |
| `Battler.index!`（伪影） | — | 实为 `index!=` 比较 | 见 §0.1 | — | 伪影 |
| `Battler.species!`（伪影） | — | 实为 `species!=` 比较 | 见 §0.1 | — | 伪影 |
| `Battler.dynamax?` | **未在插件中定义** | 调用点 BattleHandlers_Items:1317（**无 `defined?` 保护**）；BattleHandlers_Abilities:2973/2980 用 `defined?(b.dynamax?)` 保护 | 极巨化（本工程未实现） | — | 未定义（§F.1） |
| `Battler.mirrorHerbUsed` | **未在插件中定义** | 初始化 Battler_Initialize:356 `@mirrorHerbUsed = false`；读取 Battler_StatStages:68/102；写入 BattleHandlers_Items:1783/1790 `battler.mirrorHerbUsed = true` | 模仿香草是否已用 | **没有 `attr_accessor :mirrorHerbUsed`，写入即 `NoMethodError`**（§F.1） | 未定义（§F.1） |
| `Battler.pbRecoverHP?` | **未在插件中定义** | 调用点 BattleHandlers_Items:186 `battler.pbRecoverHP?(battler.totalhp/2)`（里普利特性，应为 `pbRecoverHP`） | — | — | 未定义（§F.1） |

**§A 分类计数：可转 105 ｜ 可转(降级) 27 ｜ 登记 7 ｜ 未定义 4 ｜ 伪影 4（合计 147 入口 = 143 真实 + 4 伪影）。**

---

## §B. Battle 上的方法（41 个，字母序）

| Java 落点建议 | Ruby 段:行号 | Ruby 原文（截断） | 语义 | 依赖 | 可转/登记 |
|---|---|---|---|---|---|
| `Battle.allBattlers()` | PokeBattle_Battle:440-442 | `def allBattlers; return @battlers.select { \|b\| b && !b.fainted? }; end` | **未倒下**的 battler 数组 | `@battlers` | 可转 |
| `Battle.allOtherSideBattlers(idxBattler=0)` | PokeBattle_Battle:454-457 | `idxBattler = idxBattler.index if idxBattler.respond_to?("index")` / `select { \|b\| b && !b.fainted? && b.opposes?(idxBattler) }` | 对面未倒下的数组 | `opposes?` | 可转 |
| `Battle.battlers` 字段 | PokeBattle_Battle:46 | `attr_reader :battlers` | 6 槽位数组（含 nil） | — | 可转 |
| `Battle.choices` 字段 | PokeBattle_Battle:72 | `attr_accessor :choices` | 每回合每人的选择 `[action,arg1,arg2,arg3]` | — | 可转（Java 已有 `clearChoice`/`chosenMoveSlot`） |
| `Battle.decision` 字段 | PokeBattle_Battle:53 | `attr_accessor :decision  # 0=undecided; 1=win; 2=loss; 3=escaped; 4=caught` | 战斗结果码。**`pbFaint` 读它来判断 BOSS 是否可捕获** | `Battle.battleRank`、BOSS 子系统 | 可转 |
| `Battle.eachBattler { }` | PokeBattle_Battle:437-439 | `@battlers.each { \|b\| yield b if b && !b.fainted? }` | 遍历未倒下 | — | 可转 |
| `Battle.eachOtherSideBattler(idxBattler=0) { }` | PokeBattle_Battle:449-452 | `yield b if b && !b.fainted? && b.opposes?(idxBattler)` | 遍历对面未倒下 | `opposes?` | 可转 |
| `Battle.eachSameSideBattler(idxBattler=0) { }` | PokeBattle_Battle:444-447 | `yield b if b && !b.fainted? && !b.opposes?(idxBattler)` | 遍历同方未倒下 | `opposes?` | 可转 |
| `Battle.endOfRound` 字段 | PokeBattle_Battle:84 | `attr_reader :endOfRound  # True during the end of round` | 是否处于回合结束阶段 | `Battle_Phase_EndOfRound:211-` | 可转 |
| `Battle.environment` 字段 | PokeBattle_Battle:51 | `attr_accessor :environment` | 战斗地形环境（`PBEnvironment`） | `PBEnvironment.rb:1-31` | 可转 |
| `Battle.field` 字段 | PokeBattle_Battle:43 | `attr_reader :field  # Effects common to the whole of a battle` | 全场效果（天气/场地/重力/房间） | **需新建 `ActiveField`**（`PokeBattle_ActiveField.rb:3-36`，字段 `effects/defaultWeather/weather/weatherDuration/defaultTerrain/terrain/terrainDuration`） | 可转（需新建类） |
| `Battle.futureSight` 字段 | PokeBattle_Battle:83 | `attr_reader :futureSight  # True if Future Sight is hitting` | 预知未来是否正在结算 | `Move_Effects_*` | 可转 |
| `Battle.moldBreaker` 字段 | PokeBattle_Battle:85 | `attr_accessor :moldBreaker  # True if Mold Breaker applies` | 破格是否生效（**被 40+ 处读取**） | — | 可转 |
| `Battle.pbAllFainted?(idxBattler=0)` | PokeBattle_Battle:353-355 | `return pbAbleCount(idxBattler)==0` | 某方是否全灭 | `pbAbleCount`(PokeBattle_Battle:333-338) | 可转 |
| `Battle.pbAnimation(move,user,targets,hitNum=0)` | PokeBattle_Battle:793-795 | `@scene.pbAnimation(move,user,targets,hitNum) if @showAnims` | 招式动画播放 | **`@scene.pbAnimation` → `Scene_Animations:510` → `PokeBattle_AnimationPlayer`(878 行) + `Graphics/Animations/*`** | 登记 |
| `Battle.pbCanChooseNonActive?(idxBattler)` | Battle_Action_Switching:115-120 | `pbParty(idxBattler).each_with_index { \|_pkmn,i\| return true if pbCanSwitchLax?(idxBattler,i) }` | 是否有可换上的宝可梦 | `pbCanSwitchLax?`、`pbParty` | 可转（Java 已有 `canChooseNonActive`） |
| `Battle.pbCanRun?(idxBattler)` | Battle_Action_Running:5-28 | `return false if trainerBattle?` / `return true if battler.pbHasType?(:GHOST) && NEWEST_BATTLE_MECHANICS` / `BattleHandlers.triggerRunFromBattleAbility/Item` / 各种束缚效果 | 能否逃跑 | `BattleHandlers` 端口、`@canRun` | 可转 |
| `Battle.pbCanSwitch?(idxBattler,idxParty=-1,partyScene=nil)` | Battle_Action_Switching:41-113 | `return false if !pbCanSwitchLax?(...)` / `eachSameSideBattler` 查重 / `BattleHandlers.triggerCertainSwitchingUserAbility/Item` / `triggerTrappingTargetAbility/Item` | 能否换上 | `BattleHandlers` 端口、`partyScene`（界面，可为 nil） | 可转 |
| `Battle.pbCheckGlobalAbility(abil)` | PokeBattle_Battle:478-481 | `eachBattler { \|b\| return b if b.hasActiveAbility?(abil) }; return nil` | 全场找某特性，**返回 Battler 或 nil** | `hasActiveAbility?` | 可转 |
| `Battle.pbCheckOpposingAbility(abil,idxBattler=0,nearOnly=false)` | PokeBattle_Battle:483-489 | `eachOtherSideBattler(idxBattler) do \|b\| next if nearOnly && !b.near?(idxBattler); return b if b.hasActiveAbility?(abil); end` | 对面找某特性 | `near?` | 可转 |
| `Battle.pbClearChoice(idxBattler)` | Battle_Phase_Command:5-11 | `@choices[idxBattler][0] = :None` / `[1]=0` / `[2]=nil` / `[3]=-1` | 清空某人的选择（**4 槽全部重置**） | `@choices` | 可转（Java 已有 `clearChoice`） |
| `Battle.pbCommonAnimation(name,user=nil,targets=nil)` | PokeBattle_Battle:797-799 | `@scene.pbCommonAnimation(name,user,targets) if @showAnims` | 通用演出（"StatUp"/"StatDown"/"Poison"/"Burn"/…，共 42 次调用） | **`@scene.pbCommonAnimation` → `Scene_Animations:527` → `PokeBattle_AnimationPlayer` + `Graphics/Animations/*`** | 登记 |
| `Battle.pbDisplay(msg,&block)` | PokeBattle_Battle:773-775 | `@scene.pbDisplayMessage(msg,&block)` | 显示消息（**248 次，最高频**） | `@scene.pbDisplayMessage`（PokeBattle_Scene:115）；Java 已有 `RoundEvent.message` + BattleScreen | 可转（已建模） |
| `Battle.pbDisplayPaused(msg,&block)` | PokeBattle_Battle:781-783 | `@scene.pbDisplayPausedMessage(msg,&block)` | 需要按键继续的消息 | 同上 | 可转（已建模） |
| `Battle.pbGetOwnerName(idxBattler)` | PokeBattle_Battle:266-271 | `return @opponent[idxTrainer].fullname if opposes?(idxBattler)` / `return @player[idxTrainer].fullname if idxTrainer>0` / `return @player[idxTrainer].name` | 训练家名 | **`PokeBattle_Trainer`（`PokeBattle_Trainer.rb:285`）→ 训练家/存档子系统** | 登记 |
| `Battle.pbGetReplacementPokemonIndex(idxBattler,random=false)` | Battle_Action_Switching:241-253 | `return choices[pbRandom(choices.length)] if random` / `return pbSwitchInBetween(idxBattler,true)` | 选替补（`random=false` 时打开队伍界面） | **`pbSwitchInBetween` → `pbPartyScreen` → `@scene.pbPartyScreen`（界面）** | 登记（random=true 分支可转，false 分支需界面） |
| `Battle.pbHideAbilitySplash(battler)` | PokeBattle_Battle:810-813 | `return if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH` / `@scene.pbHideAbilitySplash(battler)` | 收起特性水花（**138 次**） | **`@scene.pbHideAbilitySplash`（Scene_Animations:186）**；`USE_ABILITY_SPLASH=true` ⇒ 恒走该分支 | 登记 |
| `Battle.pbPlayer()` | PokeBattle_Battle:226 | `def pbPlayer; return @player[0]; end` | 玩家训练家对象 | `PokeBattle_Trainer` | 可转（Java `player()` 返回 Battler，签名要改） |
| `Battle.pbRandom(x)` | PokeBattle_Battle:98 | `def pbRandom(x); return rand(x); end` | 0…x−1 均匀随机（**22 次调用**） | Ruby `rand`；Java 侧 `Battle` 已持 `Random`(构造参数) | 可转 |
| `Battle.pbRecallAndReplace(idxBattler,idxParty,randomReplacement=false,batonPass=false)` | Battle_Action_Switching:256-262 | `@scene.pbRecall(idxBattler) if !@battlers[idxBattler].fainted?` / `@scene.pbShowPartyLineup(idxBattler&1) if pbSideSize(idxBattler)==1` / `pbMessagesOnReplace` / `pbReplace` | 收回并换人 | **`@scene.pbRecall`（Scene_Animations:148）、`@scene.pbShowPartyLineup`（:58）** | 登记 |
| `Battle.pbReplaceAbilitySplash(battler)` | PokeBattle_Battle:815-818 | `return if !USE_ABILITY_SPLASH` / `@scene.pbReplaceAbilitySplash(battler)` | 替换特性水花（6 次） | 同 `pbShowAbilitySplash` | 登记 |
| `Battle.pbSetSeen(battler)` | PokeBattle_Battle:652-656 | `return if !battler \|\| !@internalBattle` / `pbPlayer.seen[battler.displaySpecies] = true` / `pbSeenForm(...)` | 图鉴「已看见」登记 | **`pbPlayer.seen`（存档）、`pbSeenForm`（`PSystem_PokemonUtilities:203`）** | 登记 |
| `Battle.pbShowAbilitySplash(battler,delay=false,logTrigger=true,ability=nil)` | PokeBattle_Battle:801-808 | `PBDebug.log("[Ability triggered] …") if logTrigger` / `return if !USE_ABILITY_SPLASH` / `@scene.pbShowAbilitySplash(battler,ability)` / `if delay; 40.times { @scene.pbUpdate }; end` | 显示特性水花（**148 次**）；`delay` 会空转 40 帧 | **`@scene.pbShowAbilitySplash`（Scene_Animations:171）**、`PBDebug` | 登记 |
| `Battle.pbSideBattlerCount(idxBattler=0)` | PokeBattle_Battle:459-463 | `eachSameSideBattler(idxBattler) { \|_b\| ret += 1 }` | 同方未倒下数 | `eachSameSideBattler` | 可转 |
| `Battle.pbStartTerrain(user,newTerrain,fixedDuration=true)` | PokeBattle_Battle:741-769 | `duration = (fixedDuration) ? 5 : -1` / `BattleHandlers.triggerTerrainExtenderItem` / `pbCommonAnimation(PBBattleTerrains.animationName(@field.terrain))` / `eachBattler { b.pbAbilityOnTerrainChange; b.pbCheckFormOnTerrainChange; b.pbItemTerrainStatBoostCheck }` | 开启场地（**12 次**） | `ActiveField.terrain`、`pbCommonAnimation`、`PBBattleTerrains`、`pbCalculatePriority` | 登记（演出 + 场地子系统未建模） |
| `Battle.pbWeather()` | PokeBattle_Battle:673-676 | `eachBattler { \|b\| return PBWeather::None if b.hasActiveAbility?([:CLOUDNINE,:AIRLOCK]) }` / `return @field.weather` | 有效天气（云九/气闸屏蔽） | `ActiveField.weather`、`PBWeather` | 可转（需新建 `ActiveField`） |
| `Battle.scene` 字段 | PokeBattle_Battle:41 | `attr_reader :scene  # Scene object for this battle` | 场景对象（21 次） | **场景/Graphics 子系统** | 登记 |
| `Battle.sideStatUps` 字段 | PokeBattle_Battle:94 | `attr_accessor :sideStatUps  # 用于统计与跟风/模仿香草配合的能力强化次数` | 每方能力强化计数（**本插件为跟风/模仿香草新增**） | `Battler.addSideStatUps`、`pbMirrorStatUpsOpposing` | 可转 |
| `Battle.time` 字段 | PokeBattle_Battle:50 | `attr_accessor :time  # Time of day (0=day, 1=eve, 2=night)` | 昼夜时段 | `PBDayNight` 插件 | 可转 |
| `Battle.turnCount` 字段 | PokeBattle_Battle:52 | `attr_reader :turnCount` | 回合计数（2 次读取） | **Java 是 `turns()`**；注意 `Battler.turnCount` 是另一个字段 | 可转 |
| `Battle.wildBattle?()` | PokeBattle_Battle:190 | `def wildBattle?; return @opponent.nil?; end` | 是否野生战（7 次） | `@opponent` | 可转（Java 已有 `wildBattle()`） |

**§B 分类计数：可转 30 ｜ 登记 11 ｜ 未定义 0（合计 41）。**

---

## §C. Move 上的方法（25 个，字母序）

> Move 侧的 25 个方法**全部来自两个段**：`PokeBattle_Move`(144 行，基类) 与 `Move_Usage`(438 行，用法/判定)。
> `Move_Effects_Generic:42-44` 展示了子类如何覆写 `physicalMove?`/`specialMove?`（`return true` / `return false`）。

| Java 落点建议 | Ruby 段:行号 | Ruby 原文（截断） | 语义 | 依赖 | 可转/登记 |
|---|---|---|---|---|---|
| `BattleMove.addlEffect()` | PokeBattle_Move:13 | `attr_reader :addlEffect` | 附加效果发动率（PBS EffectChance，初始化自 `moveData[MOVE_EFFECT_CHANCE]`，:39） | — | 可转（Java 已有 `additionalChance()`） |
| `BattleMove.bitingMove?()` | PokeBattle_Move:121 | `def bitingMove?; return @flags[/i/]; end` | 咬合招式（标志 `i`） | `@flags` 字符串 | 可转 |
| `BattleMove.bombMove?()` | PokeBattle_Move:126 | `def bombMove?; return @flags[/n/]; end` | 炸弹招式（标志 `n`） | `@flags` | 可转 |
| `BattleMove.calcType()` | PokeBattle_Move:17 | `attr_accessor :calcType` | 计算后属性（初始化 `-1`，:43；由 `Move_Usage_Calculations:14 pbCalcType` 写入） | `pbCalcType(user)` 是**另一个方法**，不在本清单里 | 可转（Java 需加字段） |
| `BattleMove.contactMove?()` | PokeBattle_Move:113 | `def contactMove?; return @flags[/a/]; end` | 接触招式（标志 `a`） | `@flags` | 可转 |
| `BattleMove.damagingMove?()` | PokeBattle_Move:90 | `def damagingMove?; return @category!=2; end` | 是否伤害招式（**分类码 ≠2**，不是 `power>0`！） | `@category` | 可转（**注意 Java `statusMove()` 现在用 `power<=0`，与插件不等价**） |
| `BattleMove.function()` | PokeBattle_Move:6 | `attr_reader :function` | 招式 function 码（3 位十六进制字符串） | `pbGetMoveData` | 可转（Java 已有 `function()`） |
| `BattleMove.healingMove?()` | PokeBattle_Move:95 | `def healingMove?; return false; end` | 回复招式（基类 false，子类覆写） | 子类覆写（`Move_Effects_Generic:569`） | 可转 |
| `BattleMove.id()` | PokeBattle_Move:4 | `attr_accessor :id` | 招式 ID | — | 可转 |
| `BattleMove.is_a?(klass)` | — | `!move.is_a?(PokeBattle_Confusion)`（BattleHandlers_Items:628） | Ruby 内置 `Object#is_a?`；此处用来判断「不是混乱招式」 | 需要 Java 里招式**子类层次**（`PokeBattle_Move_XXX` 工厂） | Ruby 内置（**Java 侧需等价类型判定**） |
| `BattleMove.name()` | PokeBattle_Move:5 | `attr_reader :name` | 招式名（初始化自 `PBMoves.getName(@id)`，:30） | `PBMoves` 名称表 | 可转 |
| `BattleMove.pbContactMove?(user)` | Move_Usage:40-46 | `return false if user.hasActiveAbility?(:LONGREACH)` / `:NOBLESTRIKE` / `:RUYIBLADE` / `return true if physicalMove? && @function=="196"` / `return contactMove?` | 实际是否算接触（远隔/钢之意志/如意剑） | `hasActiveAbility?`、`physicalMove?`、`@function` | 可转 |
| `BattleMove.pbDamagingMove?()` | Move_Usage:38 | `def pbDamagingMove?; return damagingMove?; end` | 可覆写的「是否伤害」 | `damagingMove?` | 可转 |
| `BattleMove.pbMoveFailedAromaVeil?(user,target,showMessage=true)` | Move_Usage:131-146+ | `return false if @battle.moldBreaker` / `if target.hasActiveAbility?(:AROMAVEIL)` / `@battle.pbShowAbilitySplash(target)` | 芳香幕阻挡 | `pbShowAbilitySplash`（登记） | 可转（内部 splash 登记） |
| `BattleMove.physicalMove?(thisType=nil)` | PokeBattle_Move:74-79 | `return (@category==0) if MOVE_CATEGORY_PER_MOVE` / `thisType \|\|= @calcType if @calcType>=0` / `return !PBTypes.isSpecialType?(thisType)` | 是否物理。**当 `MOVE_CATEGORY_PER_MOVE==false` 时按类型名判定（物特分家前的行为）** | `MOVE_CATEGORY_PER_MOVE` / `PBTypes.isSpecialType?` | 可转（**必须查 `MOVE_CATEGORY_PER_MOVE` 的真值**，见 §F.4） |
| `BattleMove.powerBoost()` | PokeBattle_Move:18 | `attr_accessor :powerBoost` | 属性皮肤/威力提升标记（初始化 `false`，:44） | `pbCalcType` 写入 | 可转 |
| `BattleMove.pulseMove?()` | PokeBattle_Move:125 | `def pulseMove?; return @flags[/m/]; end` | 波动招式（标志 `m`） | `@flags` | 可转 |
| `BattleMove.punchingMove?()` | PokeBattle_Move:122 | `def punchingMove?; return @flags[/j/]; end` | 拳类招式（标志 `j`） | `@flags` | 可转 |
| `BattleMove.recoilMove?()` | PokeBattle_Move:96 | `def recoilMove?; return false; end` | 反伤招式（基类 false） | 子类覆写（`Move_Effects_Generic:593`） | 可转 |
| `BattleMove.slicingMove?()` | PokeBattle_Move:128 | `def slicingMove?; return @flags[/p/]; end` | 切割招式（标志 `p`） | `@flags` | 可转 |
| `BattleMove.soundMove?()` | PokeBattle_Move:123 | `def soundMove?; return @flags[/k/]; end` | 声音招式（标志 `k`） | `@flags` | 可转 |
| `BattleMove.specialMove?(thisType=nil)` | PokeBattle_Move:83-88 | `return (@category==1) if MOVE_CATEGORY_PER_MOVE` / `return PBTypes.isSpecialType?(thisType)` | 是否特殊 | 同 `physicalMove?` | 可转（同上） |
| `BattleMove.statusMove?()` | PokeBattle_Move:91 | `def statusMove?; return @category==2; end` | 是否变化招式 | `@category` | 可转 |
| `BattleMove.type()` | PokeBattle_Move:8 | `attr_reader :type` | 招式属性（初始化自 `moveData[MOVE_TYPE]`，:35） | `pbGetMoveData` | 可转 |
| `BattleMove.windMove?()` | PokeBattle_Move:129 | `def windMove?; return @flags[/r/]; end` | 风类招式（标志 `r`） | `@flags` | 可转 |

**§C 分类计数：可转 24 ｜ 登记 0 ｜ Ruby 内置 1（合计 25）。**

### §C.1 `@flags` 标志位总表（M0 必须一次冻结）

`PokeBattle_Move:113-129` 共 17 个标志位，Java `BattleMove` 现在只有 `flags()` 原始字符串：

| 字母 | 方法 | 字母 | 方法 |
|---|---|---|---|
| `a` | `contactMove?` | `j` | `punchingMove?` |
| `b` | `canProtectAgainst?` | `k` | `soundMove?` |
| `c` | `canMagicCoat?` | `l` | `powderMove?` |
| `d` | `canSnatch?` | `m` | `pulseMove?` |
| `e` | `canMirrorMove?` | `n` | `bombMove?` |
| `f` | `canKingsRock?` | `o` | `danceMove?` |
| `g` | `thawsUser?` | `p` | `slicingMove?` |
| `h` | `highCriticalRate?` | `r` | `windMove?` |
| `i` | `bitingMove?` | | （**无 `q`**，`r` 后无 `s`） |

---

## §D. 必须覆盖的 12 个 Ruby 段：核对结果

| 段名 | 声称行数 | 实际导出行数 | 与 209 方法的关系 |
|---|---:|---:|---|
| `PokeBattle_Battler` | 900 | 900 ✓ | 定义 **66** 个被调方法（属性 30 + 方法 36） |
| `Battler_Initialize` | 407 | 407 ✓ | 定义 `pbUpdate`；`@mirrorHerbUsed=false`(:356)；`@dummy`(:38-65) |
| `Battler_ChangeSelf` | 449 | 449 ✓ | 定义 `pbReduceHP` :5 / `pbRecoverHP` :19 / `pbFaint` :61+:105 / `pbChangeTypes` :157+:285 / `pbChangeFormTransform` :182 / `pbCheckFormOnStatusChange` :197 / `pbCheckFormOnWeatherChange` :227 / `pbTransform` :418 |
| `Battler_Statuses` | 624 | 624 ✓ | 定义 **27** 个被调方法（`pbHasAnyStatus?`/`asleep?`/`poisoned?`/`burned?`/`paralyzed?`/`pbPoison`/`pbBurn`/`pbParalyze`/`pbSleep`/`pbSleepSelf`/`pbConfuse`/`pbCanConfuse?`/`pbCureConfusion`/`pbAttract`/`pbCanAttract?`/`pbCureAttract`/`pbCureStatus`/`pbCanPoison?`/`pbCanBurn?`/`pbCanParalyze?`/`pbCanSleep?`/`pbCan*Synchronize?` …）|
| `Battler_StatStages` | 406 | 406 ✓ | 定义 **14** 个被调方法（`statStageAtMax?`/`statStageAtMin?`/`pbCan{Raise,Lower}StatStage?`/`pbRaiseStatStage{,ByCause,ByAbility}`/`pbLowerStatStage{,ByCause,ByAbility}`/`pbLowerAttackStatStageIntimidate`/`hasAlteredStatStages?`/`pbResetStatStages`） |
| `Battler_AbilityAndItem` | 359 | 359 ✓ | 定义 **15** 个被调方法（`pbAbilitiesOnSwitchOut`/`pbAbilitiesOnDamageTaken`/`pbAbilityOnTerrainChange`/`pbOnAbilityChanged`/`pbCanConsumeBerry?`/`pbConsumeItem`/`pbSymbiosis`/`pbHeldItemTriggered`/`pbHeldItemTriggerCheck`/`pbItemHPHealCheck`/`pbItemStatusCureCheck`/`pbItemStatRestoreCheck`/`pbItemTerrainStatBoostCheck`/`pbItemOnIntimidatedCheck`/`pbItemOpposingStatGainCheck`） |
| `Battler_UseMove` | 811 | 811 ✓ | 定义 `pbProcessTurn` :5 / `pbBeginTurn` :71 / `pbCancelMoves` :92 / `pbEndTurn` :111 / `pbConfusionDamage` :130 / `pbUseMoveSimple` :152 / `pbUseMove` :170 / `pbProcessMoveHit` :627。**这 8 个都不在 209 清单里**（handler 体不直接调它们）→ 本批不涉及，但 M0 冻结签名时应保留接口位 |
| `PokeBattle_BattleCommon` | 234 | 234 ✓ | `pbStorePokemon` :5 / `pbRecordAndStoreCaughtPokemon` :43 / `pbThrowPokeBall` :68 / `pbCaptureCalc` :170。**都不在 41 清单里**，但 `PokeBall_CatchEffects`(258 行) 的 handler 会经 `pbThrowPokeBall` 进来 |
| `PokeBattle_Battle` | 838 | 838 ✓ | 定义 **29** 个被调方法/属性 |
| `PokeBattle_Move` | 144 | 144 ✓ | 定义 **18** 个被调方法/属性 |
| `Move_Usage` | 438 | 438 ✓ | 定义 **6** 个被调方法（`pbContactMove?` :40 / `pbDamagingMove?` :38 / `pbMoveFailedAromaVeil?` :131 / `pbMissMessage` :27 / `pbShowAnimation` :67 / `pbNumHits` :50） |
| `PokeBattle_SceneConstants` | 68 | 68 ✓ | **`USE_ABILITY_SPLASH = true`(:3)** —— 决定性事实：所有 `if USE_ABILITY_SPLASH … else …` 的 `else` 分支在本工程里是**死代码**（因此 handler 里 86/148/138 次的 `abilityName` 参数其实走了 splash 分支） |

另需一并记录（虽不在 12 段内，却定义了被调方法）：

| 段名:行号 | 定义 | 为什么重要 |
|---|---|---|
| Battle_Action_Running:5-28 | `pbCanRun?` | §B 里唯一不在 12 段内的 Battle 方法 |
| Battle_Action_Switching:41/:115/:241/:256 | `pbCanSwitch?` / `pbCanChooseNonActive?` / `pbGetReplacementPokemonIndex` / `pbRecallAndReplace` | §B 的 4 个 |
| Battle_Phase_Command:5-11 | `pbClearChoice` | §B 的 1 个 |
| AI_Move_EffectScores:3866-3870 | `PokeBattle_Battle#allSameSideBattlers`（插件新增） | `Battler.allAllies` 依赖它（§F.2） |
| Transform Mosaic:66-67 | `PokeBattle_Scene#pbChangePokemonTransform`（插件新增） | `Battler.pbChangeFormTransform` 依赖它 |
| PokeBattle_BOSS:3 | `BOSS_HP_RANK = [1,1,2,5,10,15,15,20]` | `Battler.pbUpdate` 依赖 |
| PBMove:32 | `pbGetMoveData`（顶层 def） | `inTwoTurnAttack?` / `semiInvulnerable?` / `PokeBattle_Move#initialize` 依赖 |
| PSystem_PokemonUtilities:203 | `pbSeenForm` | `Battle.pbSetSeen` 依赖 |

---

## §E. 登记表（不可转译的行 + 为什么 + 建议处理）

### E.1 类别一：`@battle.scene.*` / Graphics 精灵（本运行时无场景层，且 Java 侧 `BattleScreen` 由别的任务独占）

| 段名:行号 | 被谁调用 | 为什么不能转 | 建议处理 |
|---|---|---|---|
| Battler_ChangeSelf:14 | `Battler#pbReduceHP` | `@battle.scene.pbHPChanged(self,oldHP,anim)`（`Scene_Animations:211-222`，要读精灵/播音效） | **请求资源/接口**：由 Lead 决定是否在 `BattleScreen` 暴露 `onHpChanged(idx,oldHp,anim)`；在签名里先保留布尔 `anim` 参数，**空实现 + 注释登记** |
| Battler_ChangeSelf:28 | `Battler#pbRecoverHP` | 同上 | 同上 |
| Battler_ChangeSelf:73 | `Battler#pbFaint`（第一定义） | `@battle.scene.pbFaintBattler(self)`（`Scene_Animations:299`，`BattlerFaintAnimation` 已在 task-1 转译） | **接口已存在**（task-1 的 `BattlerFaintAnimation`）；接线由 `BattleScreen` 负责，本批只登记 |
| Battler_ChangeSelf:117 | `Battler#pbFaint`（第二定义） | `@battle.scene.sprites["pokemon_#{pairedBattler.index}"].visible = true`（指挥官配对） | **静音 + 注释登记**（只影响指挥特性的出场表现） |
| Battler_ChangeSelf:190-191 | `Battler#pbChangeFormTransform` | `@battle.scene.pbChangePokemonTransform(self,@pokemon)`（来自 `Transform Mosaic:66`，做位图马赛克）+ `pbRefreshOne` | **空实现 + 注释登记**；`pkmn` 数据层（属性/HP 差值）照转 |
| Battler_ChangeSelf:443 | `Battler#pbTransform` | `@battle.scene.pbRefreshOne(@index)` | 空实现 + 注释登记 |
| Battler_AbilityAndItem:136 | `Battler#pbOnAbilityChanged` | `@battle.scene.pbChangePokemon(self,@pokemon)`（幻觉解除要换图） | 空实现 + 注释登记 |
| PokeBattle_Battler:108 | `Battler#status=` | `@battle.scene.pbRefreshOne(@index)` | 空实现 + 注释登记 |
| PokeBattle_Battler:116 | `Battler#statusCount=` | 同上 | 空实现 + 注释登记 |
| PokeBattle_Battle:804-806 | `Battle#pbShowAbilitySplash` | `@scene.pbShowAbilitySplash` + `40.times { @scene.pbUpdate }`（`Scene_Animations:171-185`） | **点名请求资源**：`Graphics/Pictures/Battle/` 下的能力水花图 + 精灵 `abilitySplash`；本批登记 |
| PokeBattle_Battle:812 | `Battle#pbHideAbilitySplash` | `@scene.pbHideAbilitySplash`（`Scene_Animations:186-199`） | 同上 |
| PokeBattle_Battle:817 | `Battle#pbReplaceAbilitySplash` | `@scene.pbReplaceAbilitySplash`（`Scene_Animations:200-209`） | 同上 |
| PokeBattle_Battle:798 | `Battle#pbCommonAnimation` | `@scene.pbCommonAnimation` → `PokeBattle_AnimationPlayer`(878 行) + `Graphics/Animations/*` | **点名请求资源 + 建议先空实现**；但 **不能** 把 `pbCommonAnimation` 的调用点删掉，必须留接口位并注释 `PokeBattle_Battle:798` |
| Battler_StatStages:57 / :84 / :223 / :268 | `pbRaiseStatStage` / `pbRaiseStatStageByCause` / `pbLowerStatStage` / `pbLowerStatStageByCause` | `@battle.pbCommonAnimation("StatUp"/"StatDown",self)` | 空实现 + 注释登记（数值/文案照转） |
| Battler_Statuses:263/267/270/274/277/280/283/286 | `pbInflictStatus`（被 `pbPoison`/`pbBurn`/`pbParalyze`/`pbSleep` 调用） | `@battle.pbCommonAnimation("Sleep"/"Toxic"/"Poison"/"Burn"/"Paralysis"/"Frozen"/"Frostbitten"/"Drowsy",self)` | 空实现 + 注释登记 |
| Battler_Statuses:462 | `pbContinueStatus` | `@battle.pbCommonAnimation(anim,self)` | 空实现 + 注释登记 |
| Battler_Statuses:533 | `pbConfuse` | `@battle.pbCommonAnimation("Confusion",self)` | 空实现 + 注释登记 |
| Battler_Statuses:600 | `pbAttract` | `@battle.pbCommonAnimation("Attract",self)` | 空实现 + 注释登记 |
| Battle_Action_Switching:257-259 | `Battle#pbRecallAndReplace` | `@scene.pbRecall` / `@scene.pbShowPartyLineup` | **接口请求**；本批登记 |
| PokeBattle_Battle:795 | `Battle#pbAnimation` | `@scene.pbAnimation` → `PokeBattle_AnimationPlayer` | 空实现 + 注释登记 |
| PokeBattle_Battle:41 | `Battle#scene` | 场景对象本身 | 登记；Java 侧不要暴露 `scene`，改为把需要的东西做成 `Battle` 的回调接口 |

### E.2 类别二：PBDebug（纯日志）

| 段名:行号 | 被谁调用 | 为什么不能转 | 建议处理 |
|---|---|---|---|
| Battler_ChangeSelf:11 / :25 | `pbReduceHP` / `pbRecoverHP` | `PBDebug.log("[HP change] …")` | **静音（空实现）+ 注释登记** |
| Battler_ChangeSelf:67 / :72 | `pbFaint` | `PBDebug.log("!!!***Can't faint…")` / `"[Pokémon fainted] …"` | 静音 |
| Battler_ChangeSelf:193 | `pbChangeFormTransform` | `PBDebug.log("[Form changed] …")` | 静音 |
| Battler_StatStages:41 / :185 | `pbRaiseStatStageBasic` / `pbLowerStatStageBasic` | `PBDebug.log("[Stat change] …")` | 静音 |
| Battler_Statuses:291 / :465 / :482 / :536 | `pbInflictStatus` / `pbContinueStatus` / `pbCureStatus` / `pbConfuse` | `PBDebug.log("[Status change] …")` 等 | 静音 |
| Battler_AbilityAndItem:171 | `pbConsumeItem` | `PBDebug.log("[Item consumed] …")` | 静音 |
| PokeBattle_Battle:802 | `pbShowAbilitySplash` | `PBDebug.log("[Ability triggered] …")` | 静音（**但 `logTrigger` 参数必须保留在签名里**） |
| 全工程 | `PBDebug` 段共 41 行 | 输出到 `PBDebug.log` 文件 | 建议 Java 侧统一加一个 `BattleLog.noop(...)`，全部调用点保留 + 注释标 `PBDebug:行号` |

> `PBDebug.rb` 段确认存在（41 行）。

### E.3 类别三：未建模的功能子系统

| 子系统 | 段名:行号 | 谁依赖它 | 为什么不能转 | 建议处理 |
|---|---|---|---|---|
| BOSS 战 | `PokeBattle_BOSS:157 pbCatchBossPokemon`；`PokeBattle_BOSS:3 BOSS_HP_RANK` | `Battler#pbFaint`(:63)、`Battler#pbUpdate`(:366) | `pbCatchBossPokemon` 是 `Boss_Battles`(4368 行) 的入口；`BOSS_HP_RANK` 只是数组可直接内联 | `BOSS_HP_RANK` **可转**（内联 8 项数组）；`pbCatchBossPokemon` **登记**，`pbFaint` 里那一行 `return` 语义必须保留（`if @pokemon.battleRank > 1 … return`），否则 BOSS 战会走错分支 |
| 存档 / BattlePeer | `PokeBattle_BattlePeer:7 pbOnLeavingBattle` | `Battler#pbFaint`(:87)、`Battler#pbAbilitiesOnSwitchOut`(:46) | `@battle.peer` 是 `PokeBattle_BattlePeer.create`（:110），PC/队伍持久化 | **登记**；建议 java 侧先做 `BattlePeer.noop()` + 注释 |
| 图鉴 | `PSystem_PokemonUtilities:203 pbSeenForm`；`Battle#pbSetSeen`(:652-656) | `pbChangeFormTransform`(:194)、`pbOnAbilityChanged`(:138)、`pbSetSeen` | 依赖 `$Trainer`/存档的 `seen/owned/form` 表 | **登记**；建议 Lead 拍板是否在 M0 就建 `PokedexState` 接口 |
| 训练家对象 | `PokeBattle_Trainer:285`(285 行) | `Battle#pbGetOwnerName`(:266-271)、`Battle#pbPlayer`(:226) | Java 侧 `Battle` 只有 `player()`/`foe()` 返回 `Battler`，没有 Trainer 对象 | **登记**；`pbGetOwnerName` 是 41 清单里唯一必须靠 Trainer 的 |
| 队伍界面 | `Battle_Action_Switching:156 pbSwitchInBetween` / `:136 pbPartyScreen` | `Battle#pbGetReplacementPokemonIndex`(random=false 分支) | 要开界面 + 回调 | **登记**；签名保留 `random` 参数，Java 侧建议只实现 `random=true` 语义并显式抛「未实现」而不是自造界面 |
| 场地（terrain） | `PokeBattle_Battle:741-769` + `PBBattleTerrains`(26 行) | `Battle#pbStartTerrain` | `PBBattleTerrains.animationName` 要 Graphics；`pbCalculatePriority` 属 AI 子系统 | `PBBattleTerrains` 常量表 **可转**；`pbStartTerrain` 的动画行 **登记**，其余可转 |
| 华丽大赛 / 战斗宫 / 狩猎区 | `PBattle_BugContest`(404) / `PokeBattle_BattlePalace`(276) / `PokeBattle_SafariZone`(506) / `PBattle_Safari`(139) | 本批 209 方法**没有一条**直接依赖它们 | — | **本批无需登记**；但注意 `PokeBattle_BattlePalace:152` 覆写了 `pbAutoFightMenu`、`:137` 覆写 `pbRegisterMove`，将来冻结 `Battle` 接口时要留 `@` 钩子 |

### E.4 类别四：**插件自身缺陷（本工程里就是坏的）—— 不许自造替代**

| # | 段名:行号 | 问题 | 后果 | 建议处理（**等 Lead 拍板，我不提替代实现**） |
|---|---|---|---|---|
| 1 | BattleHandlers_Items:186 | `battler.pbRecoverHP?(battler.totalhp/2)` —— 方法名多了一个 `?` | `Battler` 无 `pbRecoverHP?`；持有**里普利(RIPEN)**特性吃文柚果时 `NoMethodError` | 登记；请 Lead 决定「原样保留错误语义」还是「按 `pbRecoverHP` 处理」—— 后者属修正插件，需授权 |
| 2 | BattleHandlers_Items:1317 | `if user.dynamax?` —— 无 `defined?` 保护 | `Battler` 无 `dynamax?`；该 handler（拖入战斗类）走到即崩 | 登记；同上的授权问题。注意 Abilities:2973/2980 用了 `defined?(battler.dynamax?)` 保护，说明作者知道它可能不存在 |
| 3 | BattleHandlers_Items:1783,1790 | `battler.mirrorHerbUsed = true/false` | `Battler` **没有** `attr_accessor :mirrorHerbUsed`（只有 `@mirrorHerbUsed` 在 Battler_Initialize:356 初始化，被 Battler_StatStages:68/102 读取） → `NoMethodError` | 登记；Java 侧建议**保留该字段**（`Battler.mirrorHerbUsed` boolean），因为 Battler_StatStages:68/102 是真读它的 |
| 4 | BattleHandlers_Items:1309 | `if Battle::Scene::USE_ABILITY_SPLASH` | 全工程**没有** `module Battle`/`class Battle`/`Battle::Scene`（`__find.mjs` 已确认）；只有 `PokeBattle_SceneConstants::USE_ABILITY_SPLASH`(PokeBattle_SceneConstants:3) → `NameError` | 登记；请 Lead 明确这批是「按 `PokeBattle_SceneConstants` 读」还是「原样抛错」 |
| 5 | BattleHandlers_Abilities:4581 | `target.pbWeather` | `Battler` 无 `pbWeather`（只有 `effectiveWeather`，PokeBattle_Battler:856）；`Battle` 才有 `pbWeather`(PokeBattle_Battle:673) | 登记；同授权问题 |
| 6 | Battler_AbilityAndItem:218 | `if hasActiveAbility?(:CUDCHEW) && pbIsBerry?(item_to_use) && fling` —— `item_to_use` 在本方法内**从未定义**（参数名是 `thisItem`） | 拥有**反刍(CUDCHEW)**特性的宝可梦道具被消耗时 `NoMethodError`（短路求值使其只在 CUDCHEW 生效时爆） | 登记；请 Lead 拍板 |
| 7 | PokeBattle_Battler:491 | `abil = @ability_id if !abil` —— `@ability_id` 从未定义（应为 `@ability`） | `uncopyableAbility?` 无参调用时恒为 `nil` → 恒返回 `false` | 登记。**注意 Java 侧若照抄，会让「特性不可复制」判定永远为假**，与 Ruby 实际行为一致，但结果可疑 |
| 8 | PokeBattle_Battler:808-810 / Battler_Statuses:240-242 | `pbOpposingTeam` / `pbCanSynchronizeStatus?` 里 `pbCanSynchronizeStatus?` 第 240 行用了 **未定义的局部变量 `user`**（方法签名只有 `newStatus,target`） | `pbCanSynchronizeStatus?` 走到 Safeguard 检查时才爆 | 登记；同授权问题 |

---

## §F. 需 Lead 先拍板的疑点

### F.1 四个「未定义」与八个「插件缺陷」
即 §E.4 全部 8 条 + §A 表里 4 个 `未定义` 行。**核心问题只有一个：本工程有若干 handler 引用了不存在的方法/常量。
按铁律我不造替代实现，但 M0 冻结签名时必须决定这 12 处怎么办**，否则 L1'/L2 写 handler 体时会在同一处卡住。

### F.2 `allSameSideBattlers` 的归属
`Battler#allAllies`（PokeBattle_Battler:829-831）依赖 `Battle#allSameSideBattlers`，
而该方法**不在 `PokeBattle_Battle`(838 行) 里**，是插件在 `AI_Move_EffectScores:3866-3870`
（注释写着「新增的」）猴补到 `PokeBattle_Battle` 上的。请 Lead 决定它在 Java 侧落 `Battle` 还是 `BattleAi`。
同类情况还有 `Transform Mosaic:66` 猴补的 `PokeBattle_Scene#pbChangePokemonTransform`。

### F.3 `USE_ABILITY_SPLASH == true` 造成的大量死代码
`PokeBattle_SceneConstants:3` 为 `true`。这意味着 `pbRaiseStatStageByAbility` / `pbLowerStatStageByAbility` /
`pbCanInflictStatus?` / `pbLowerAttackStatStageIntimidate` / `affectedByPowder?` / `takesIndirectDamage?` 里
**所有 `else` 分支（用 `abilityName` 拼文案的那些）在本工程里永远不会执行**。
请 Lead 确认 M0 的 Java 签名是否要照抄两分支（我建议**照抄**，因为常量可能被改，且 handler 的 86 次 `abilityName` 读法由此决定）。

### F.4 `MOVE_CATEGORY_PER_MOVE` 的真值
`PokeBattle_Move:75 / :84` 的 `physicalMove?` / `specialMove?` 分两条路：
真→看 `@category`（0/1），假→看 `PBTypes.isSpecialType?(@type)`。
这两个方法被 handler 调用 26+28 次，**真值决定 Java 里 `physical()/specialMove()` 该怎么实现**。
我这一批没有导出 `Settings` 段去查它。**请 Lead 或后续任务确认 `MOVE_CATEGORY_PER_MOVE` 与 `NEWEST_BATTLE_MECHANICS` 的值**——
后者在 Battler_Statuses:98、PokeBattle_Battler:658/270、PokeBattle_Move:136 等处决定分支。

### F.5 `Battler#pbChangeTypes` 有**两个定义**（后者胜出）
`Battler_ChangeSelf:157-177`（只处理 Battler / 标量）被 `Battler_ChangeSelf:285-314`（多一个 `Array` 分支，
且**不再复位 `LoseGrassType/LoseFireType/LoseWaterType`**）覆盖。
Java 只能有一份实现，**建议按 :285-314（胜出版）**，但这意味着 `:174-176` 那三行复位逻辑丢失 —— 与 Ruby 实际行为一致。请确认。

### F.6 `Battler#pbFaint` 与 `Battle#pbFaint` 的命名冲突
`Battler_ChangeSelf:61` 定义 `pbFaint`，`:105` 又用 `alias paldea_pbFaint pbFaint` + 重定义包一层。
Java 里需要 `pbFaintInternal` + `pbFaint` 两个方法名，或用一个 boolean 参数。请 Lead 定签名。

### F.7 `pbDisplay` 的签名
`Battle#pbDisplay(msg,&block)` 带 block（handler 里 248 次调用几乎都不传 block，但 `pbContinueStatus` 会 `yield` 后再 `pbDisplay`）。
Java 侧建议 `pbDisplay(String)` + 可选 `Runnable`。请确认与现有 `Battle.RoundEvent.message` 的关系。

### F.8 `Battler.itemName` / `abilityName` 的返回
`PBAbilities.getName(@ability)` / `PBItems.getName(@item)` —— Java 侧 `PbsData` 里是否有等价的
「按 internalName 拿显示名」入口？我这一批没查（不在 209 清单里）。若没有，需要 Lead 决定
是走 `PbsData.Abilities/Items` 查表还是退回 internalName。

---

## §G. 方法与段名交叉索引（便于 L1'/L2 定位）

| 段名 | 段内定义、且出现在 209 清单里的方法（行号） |
|---|---|
| PokeBattle_Battler | 3,4,6,8,9,10,11,12,13,15,17,18,19,22,23,25,33,42,44,48,55,57,62,64,69,71,90,92,97,100,102,111,113,211,212,214,233,240,281,297,307,314,350,357,387,403,445,490,506,514,519,534,543,577,591,610,615,656,684,695,704,718,725,740,744,748,752,773,777,784,790,796,802,808,813,818,823,829,833,842,856,866 |
| Battler_Initialize | 362（另 356 为 `@mirrorHerbUsed` 初始化） |
| Battler_ChangeSelf | 5,19,61,105,157,182,197,227,285,418 |
| Battler_Statuses | 11,18,25,202,255,314,318,354,358,371,375,379,383,390,394,398,402,409,413,417,421,428,432,436,468,488,531,547,554,598,612 |
| Battler_StatStages | 5,9,28,47,74,108,126,130,172,191,237,289,309,378,393 |
| Battler_AbilityAndItem | 41,67,75,123,132,152,163,170,182,207,235,253,267,279,295,307,318,333 |
| PokeBattle_Battle | 41,43,46,50,52,53,72,83,84,85,94,98,190,226,266,353,437,440,444,449,454,459,478,483,652,673,741,773,781,793,797,801,810,815 |
| Move_Usage | 38,40,131 |
| PokeBattle_Move | 4,5,6,8,13,17,18,74,83,90,91,95,96,113,121,122,123,125,126,128,129 |

> 行号已逐条对照本笔记 §A/§B/§C 的表格；如需追溯原文，用
> `node __sections.mjs "<段名>"` 现场导出，或读 `__r20-ref\m0b\<段名>.txt`（我导出时的快照，逐行带原始行号）。

**—— 交付完毕。本文件是本次任务唯一写入的文件；未改动 `__r20-ref\ruby\` 下 Lead 的既有导出，未新增/修改任何 `.java` / `.js`。**
