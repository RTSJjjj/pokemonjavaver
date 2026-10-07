# B4 回合末（EOR）阶段清单 —— `pbEndOfRoundPhase` 逐阶段转译的核对表

> 原文：`__r20-ref/m0b/Battle_Phase_EndOfRound.txt`（861 行）
> - `pbEORCountDownBattlerEffect` `:5` · `pbEORCountDownSideEffect` `:13` · `pbEORCountDownFieldEffect` `:20`
> - `pbEORWeather(priority)` `:35-111`（内含 `triggerEORWeatherAbility` `:81`）
> - `pbEORTerrain` `:115-148`
> - `pbEORShiftDistantBattlers` `:152-207`
> - **`pbEndOfRoundPhase` `:211-829`（619 行）** ← 本表主体
> - `pbCheckNeutralizingGas` `:830-`
>
> 现状：`Battle.endOfTurn` 只有 20 行（回合计数 + 中毒 + 烧伤 + flinch 复位 + `endOfRoundZa`）。
> `:218` 起共 **60+ 个阶段**，下面逐条标覆盖。`[x]` 已有 · `[ ]` 缺口 · `[~]` 部分

## 阶段表（按插件执行顺序）

| 原文 | 阶段 | 覆盖 | 备注 |
|---|---|---|---|
| `:214` | `@endOfRound = true` | `[ ]` | `Battle.endOfRound` 字段存在 |
| `:215` | `@scene.pbBeginEndOfRoundPhase` | `[ ]` | 场景 |
| `:216` | `pbCalculatePriority`（重算速度） | `[ ]` | 未建模（C6） |
| `:217` | `priority = pbPriority(true)` | `[ ]` | 运行时用 `fieldedBySpeed()` |
| `:218` | **Weather**（`pbEORWeather`） | `[ ]` | 含 `triggerEORWeatherAbility` `:81` · 天气倒数 · 形态变化 · primordial 守卫 |
| `:220` | Future Sight / Doom Desire | `[ ]` | 含 `pbUseMoveSimple` · `FutureSight*` 位置效果 |
| `:258` | Wish | `[ ]` | `Wish`/`WishAmount`/`WishMaker` |
| `:268` | Sea of Fire（火+草誓约） | `[ ]` | |
| `:287` | 状态治疗特性/道具 + HP 回复道具 | `[ ]` | **`:297 triggerEORHealingAbility`** · **`:298 triggerEORHealingItem`** ⇒ 剩饭/黑泥/治愈/湿润之躯/蜕皮 |
| `:290` | Grassy Terrain 回复 | `[ ]` | |
| `:301` | 手术（`Operation`） | `[ ]` | |
| `:309` | Aqua Ring | `[ ]` | 含 `BIGROOT` ×1.3 |
| `:318` | Ingrain | `[ ]` | 同上 |
| `:327` | Leech Seed | `[ ]` | |
| `:345` | Hyper Mode 伤害（暗影宝可梦） | `[ ]` | Shadow 子系统 |
| `:354` | Stone Axe Splinters | `[ ]` | Arceus 段 |
| `:372` | Ceaseless Edge Splinters | `[ ]` | Arceus 段 |
| `:390` | **中毒伤害** | `[x]` | `Battle.poisonDamage`（含 BOSS rank / toxic 计数） |
| `:423` | **烧伤伤害** | `[x]` | `Battle.burnDamage` |
| `:438` | 冻伤伤害（LA） | `[ ]` | 需 `pbFrostbite` |
| `:453` | 睡眠（Nightmare） | `[ ]` | |
| `:468` | Curse | `[ ]` | |
| `:482` | Octolock | `[ ]` | |
| `:493` | 束缚类（Bind/Clamp/Fire Spin/Magma Storm/Sand Tomb/Whirlpool/Wrap） | `[ ]` | |
| `:528` | Taunt | `[ ]` | |
| `:532` | Encore | `[ ]` | |
| `:548` | Disable / Cursed Body | `[ ]` | |
| `:553` | Magnet Rise | `[ ]` | |
| `:557` | Telekinesis | `[ ]` | |
| `:561` | Heal Block | `[ ]` | |
| `:565` | Embargo | `[ ]` | |
| `:570` | Yawn | `[ ]` | |
| `:577` | Perish Song | `[ ]` | |
| `:591` | 灭亡之歌同侧全灭判定 | `[ ]` | |
| `:597` | 珠泪哀歌 | `[~]` | `endOfRoundZa()` |
| `:612` | 盐腌 | `[ ]` | |
| `:623` | 咒钉 | `[ ]` | |
| `:630` | 地魔之剑 / 海魔之雨回合 | `[ ]` | 与 `PoisonVulnerability`/`IceVulnerability` 相关 |
| `:639` | 复活 | `[ ]` | |
| `:644` | 检查战斗结束 | `[ ]` | |
| `:650-701` | **侧/场地效果倒数**：Reflect `:650` · Light Screen `:653` · Safeguard `:656` · Mist `:659` · Tailwind `:662` · Lucky Chant `:665` · Pledge Rainbow `:668` · Pledge Sea of Fire `:671` · Pledge Swamp `:674` · Aurora Veil `:677` · 妄之歌 `:680` · Trick Room `:684` · Gravity `:687` · Water Sport `:690` · Mud Sport `:693` · Wonder Room `:696` · Magic Room `:699` | `[ ]` | 用 `pbEORCountDownSideEffect`/`pbEORCountDownFieldEffect` |
| `:702` | 场地结束（`pbEORTerrain`） | `[ ]` | |
| `:706` | Hyper Mode（暗影） | `[ ]` | |
| `:716` | Uproar | `[ ]` | |
| `:725` | Slow Start 结束提示 | `[ ]` | |
| `:732` | **Bad Dreams / Moody / Speed Boost** | `[ ]` | **`:733 triggerEOREffectAbility`** ⇒ 加速/梦魇/情绪不定 |
| `:734` | **Flame Orb / Sticky Barb / Toxic Orb** | `[ ]` | **`:735 triggerEOREffectItem`** |
| `:736` | **Harvest / Pickup** | `[ ]` | **`:737 triggerEORGainItemAbility`** ⇒ 收获/捡拾 |
| `:741` | 形态检查 | `[ ]` | |
| `:743` | 可换人则换人 | `[ ]` | |
| `:746` | 3v3 以上且互相不邻近时移动站位 | `[ ]` | `pbEORShiftDistantBattlers` |
| `:749` | Trace 生效 + 始源天气结束检查 | `[ ]` | |
| `:751-798` | **battler 侧效果倒数**（无提示）：`pbEORCountDownBattlerEffect` | `[ ]` | 含 flinch `[~]`（`endOfTurn` 里置 false） |
| `:799-810` | **side 侧效果倒数** | `[ ]` | |
| `:811-822` | **field 侧效果倒数** | `[ ]` | |
| `:823` | Neutralizing Gas | `[ ]` | `pbCheckNeutralizingGas` |

## 6 个 EOR trigger（本批的接线目标）

| 原文 | trigger | 现状 |
|---|---|---|
| `:81` | `triggerEORWeatherAbility` | 无调用点 |
| `:297` | `triggerEORHealingAbility` | 无调用点 |
| `:298` | `triggerEORHealingItem` | 无调用点 |
| `:733` | `triggerEOREffectAbility` | 无调用点 |
| `:735` | `triggerEOREffectItem` | 无调用点 |
| `:737` | `triggerEORGainItemAbility` | 无调用点 |

另有两个延长类（在别处调用）：`triggerWeatherExtenderItem` · `triggerTerrainExtenderItem`。

## B10 同期要做的重开合并

| 原文 | 内容 |
|---|---|
| `Arceus:36-50` | 重开 `PokeBattle_Battler#pbInitEffects`：batonPass 时按 `PowerShift` 交换攻防；非 batonPass 时初始化 `VictoryDance`/`PowerShift`/`StoneAxe` |
| `场地:483-490` | 同上重开：初始化 `CaptureNet`/`CaptureNetUser` |
| `场地:556+` | 重开 `PokeBattle_Battle#pbEndOfRoundPhase`：CaptureNet 回合末降速 |
| ✅ 已完成 | `Arceus:181-210` 重开 `pbEffectsAfterMove`（`BattlerHitEffects` 里已含） |

## 实施建议（分批，每批一次编译 + 全量测试）

1. **EOR-1 倒计时助手**：`pbEORCountDownBattlerEffect` / `SideEffect` / `FieldEffect`（`:5-31`，约 30 行）—— 后面 `:650-822` 全靠它们
2. **EOR-2 天气/场地**：`pbEORWeather`(77 行) + `pbEORTerrain`(34 行) + 接 `triggerEORWeatherAbility`
3. **EOR-3 回复类**：`:287-344`（治疗特性/道具 + Grassy + 手术 + Aqua Ring + Ingrain + Leech Seed）⇒ **接 `triggerEORHealing*`**，解锁剩饭/黑泥
4. **EOR-4 伤害类**：`:345-527`（冻伤/梦魇/诅咒/束缚…）—— 中毒与烧伤已有
5. **EOR-5 倒数类**：`:528-702`（Taunt/Encore/Perish/侧场地倒数）
6. **EOR-6 效果类 + 收尾**：`:706-829`（**接 `triggerEOREffect*` / `triggerEORGainItemAbility`** + 三组倒数 + Neutralizing Gas）
7. **B10**：两个 `pbInitEffects` 重开合并 + `场地` 的 `pbEndOfRoundPhase` 重开（CaptureNet）

**依赖**：EOR-6 的 Harvest/Pickup 需要 `pbItemEndOfMoveCheck`/道具写入路径；EOR-2 的形态变化依赖 `pbChangeFormTransform`（已有）；`场地` 重开依赖 EOR-1。

---

## 本轮更新（覆盖标记以此为准，上表未同步）

**已完成并验证**（:core:test 682/0/0）：

| 原文 | 阶段 | 实现 |
|---|---|---|
| :5-11 | pbEORCountDownBattlerEffect | BattleEndOfRound（助手，待 EOR-5 使用） |
| :20-30 | pbEORCountDownFieldEffect | BattleEndOfRound + 已用于 :685-701 |
| :287-300 | 治疗阶段（Grassy / **Healer·Hydration·Shed Skin** / **Black Sludge·Leftovers**） | pbEORHealing · **剩饭回血运行时测试 2 条通过** |
| :301-308 | 手术 | pbEORHealing |
| :309-317 | Aqua Ring（含大根茎 ×1.3） | pbEORHealing |
| :318-326 | Ingrain（含大根茎 ×1.3） | pbEORHealing |
| :685-701 | 场地倒数：Trick Room · Gravity · Water Sport · Mud Sport · Wonder Room · Magic Room | pbEORFieldCountdowns |
| :725-731 | Slow Start 结束提示 | pbEOREffect |
| :732-733 | Bad Dreams / Moody / Speed Boost | pbEOREffect · **triggerEOREffectAbility 已接线** |
| :734-735 | Flame Orb / Sticky Barb / Toxic Orb | pbEOREffect · **triggerEOREffectItem 已接线** |
| :736-737 | Harvest / Pickup | pbEOREffect · **triggerEORGainItemAbility 已接线** |

**接线状态**：6 个 EOR trigger 中已接 5 个；	riggerEORWeatherAbility(:81) 待 EOR-2。
trigger 总覆盖 **46 / 85**。

**下一步**：EOR-2（pbEORWeather :35-111 + pbEORTerrain :115-148 + 	riggerEORWeatherAbility + 两个 Extender 道具）→ EOR-4（伤害 :345-527）→ EOR-5（倒数 :528-702，需要 @sides[side] 访问器）→ EOR-6 余（:751-829 + Neutralizing Gas）→ B10（两个 pbInitEffects 重开 + 场地 的 pbEndOfRoundPhase）。
