# Stage 3：野生宝可梦战斗 —— 逐条核对清单（工作单）

> 本轮任务：把 Java 运行时的**野生宝可梦战斗**按本工程插件（`Data/Scripts.rxdata`）逐条核对，
> 列出全部差异，再按「一次一个子系统」修复。
> 铁律不变：每条实现必须指到插件 `段名:行号`；文案照抄 `_INTL`；禁止自造效果/数值/文案。
> 基线：core 426 `@Test` 全绿、builder 295 全绿、脚本 `blocks=5441/translated=5202/coverage=96.4%`。

> ## ⚠️ 铁律加严（2026-10，用户明令，最高优先级）
>
> 1. **必须一行行对着插件 Ruby 转译**，不是"看着像"；一次转不完就**分批次**，时间充足，不许赶。
> 2. **禁止一切自己造、自己编的行为**：自创控制流 / 自创动画 / 自创数值 / 自创文案 /
>    自创"等价近似" / 自创"可接受的简化"，一律不许。所有修改必须指向原游戏插件的 Ruby 代码。
> 3. 每处改动都要在该行注释里写明对应 `段名:行号`；写不出行号就不许写。
> 4. **无法转译的（缺素材/缺数据/插件本身有问题/需先做别的子系统）→ 立刻停止该批次并向用户报告**，
>    由用户处理；不许绕过、不许造替代品、不许默默降级。
> 4b. **插件引用的资源（图/音/数据文件）必须先确认工程里真实存在**；找不到就**立刻停止并报告**，
>    **禁止自己造一个资源、禁止留空/注释掉当没看见**。（例：某段动画要 `Graphics/Animations/X`，
>    先确认 `X` 在工程里，再写代码。）
> 5. 交付按批次报告：本批 `段名:行号区间`、未转的行与原因、定点截图证据。
>
> 本文档前面几轮（R13–R18）的部分实现是"功能级近似"，**按新铁律需要重新逐行核对**，
> 已知需要重做的点见 §19。

---

## 0. 一句话结论

运行时现有战斗是**单人单挑的最小回合循环**（`Battle.java` 712 行 + `Battler.java` 164 行 +
`DamageCalc.java` 48 行 + `MoveEffects.java` 113 行），插件战斗引擎约 **35,000 行**。
缺的不只是招式，而是**整条野生战斗链路**：入口规则/返回值、野生个体生成、
回合阶段（指令→攻击→回合结束）、异常状态与能力等级、捕捉、AI、战斗后处理。

---

## 1. 野生战斗的完整链路（插件）

| 步骤 | 插件位置 | 运行时现状 |
|---|---|---|
| 事件调用 `pbWildBattle(species, level, outcomeVar, canRun, canLose)` | `PField_Battles:351-368` | `script-compiler.js:320` 只取 `species`/`level`，**丢掉 outcomeVar/canRun/canLose**；`EventInterpreter:1612` 直接开打 |
| 战斗规则 `setBattleRule` | `PField_Battles:60-77`、`:27-55` | `BattleRules.java` 有 `size/canLose/outcomeVar` 等，但 `prepareTrainerBattle()` 只消费 `canLose/outcomeVar`（`EventInterpreter:2008-2020`），`canRun/disablePokeBalls/expGain/moneyGain/switchStyle/battleAnims/terrain/weather/…` **未消费** |
| 无可用宝可梦 → 跳过战斗 | `PField_Battles:266-275` | **未实现** |
| 生成野生个体 `pbGenerateWildPokemon` | `PField_Encounters:417-463` | `HeadlessBattlePort.wildPokemon():119-131` 只有 IV+nature |
| 战斗准备 `pbPrepareBattle`（天气/场地/环境/背景/时间） | `PField_Battles:84-190` | **未实现**（无 weather/terrain/environment/backdrop/time） |
| 开场动画 + BGM | `PField_Battles:329`、`PField_Visuals:18-133` | ✅ 已做（上一轮） |
| 开场提示语（含 `battleRank` 变体） | `Battle_StartAndEnd:196-216` | 只做「哦！一只野生的{X}出现了！」（`InteractiveBattlePort:119`），`battleRank` 的「特殊的/强大的」「无法捕捉/不打倒不能捕捉」**未做** |
| 天气/场地宣告 + 入场特性 `pbOnActiveAll` | `Battle_StartAndEnd:327-354` | **未实现** |
| 主循环 `pbBattleLoop` | `Battle_StartAndEnd:362-389` | `Battle.step()` 一次一回合，无 `pbBossBuffPhase` |
| 指令阶段 `pbCommandPhase` | `Battle_Phase_Command`（265 行） | 简化：`BattleScreen` 的 战斗/背包/宝可梦/逃跑 |
| 攻击阶段 `pbAttackPhase` | `Battle_Phase_Attack`（196 行）+ `Battle_Action_AttacksPriority`（269 行） | 简化：priority→speed→随机 |
| 回合结束阶段 `pbEndOfRoundPhase` | `Battle_Phase_EndOfRound`（**860 行**） | `Battle.endOfTurn():359-386` 只有中毒/烧伤伤害 + 清除畏缩 |
| 胜负判定 `pbJudge` | `Battle_StartAndEnd:587` | `Battle.result():75-79` |
| 战斗结束 `pbEndOfBattle` | `Battle_StartAndEnd:444-545` | 部分（训练师胜利收尾已做） |
| 结局写入变量 `pbSet(outcomeVar, decision)` | `PField_Battles:343` | **野生战斗不写**（只有 TRAINER_BATTLE 写，`EventInterpreter:1654`） |
| 战斗后处理 `pbAfterBattle` | `PField_Battles:619-658` | **几乎全缺**（见 §4） |
| 战斗后事件 `Events.onEndBattle` | `PField_Battles:684-708` | **未实现**（见 §4） |

---

## 2. 入口流程差异（已逐行核对）

1. **【缺】野生战斗不写结局变量** —— `PField_Battles:343` `pbSet(outcomeVar,decision)`；
   运行时 `EventInterpreter:1612-1623` 的 `WILD_BATTLE` 分支没有 `pendingOutcomeVar`。
   后果：`battleLightDim`（`FreeWildBattle:30-39`）这类 `pbGet(outcome)` 的脚本全部失效。
2. **【缺】`pbWildBattle` 的返回值没接给脚本** —— `PField_Battles:367` `return (decision!=2 && decision!=5)`；
   运行时只有 `pbTrainerBattle` 有 `pendingBattleCondition`（`EventInterpreter:2001`）。
3. **【缺】`canRun` / `canLose` 参数被丢弃** —— `PField_Battles:360-361`；`script-compiler.js:320-326` 只保留 species/level。
4. **【缺】`pbFreeWildBattle(pkmn, …)` 语义错** —— `FreeWildBattle:2-8` 收的是**宝可梦对象**（`pbWildBattleCore(pkmn)`），
   运行时编译器把它当成 `WILD_BATTLE(species, level)`（`script-compiler.js:328-334`），`battleLightDim` 传的是局部变量名。
5. **【缺】`isRoamer` / 漫游宝可梦** —— `PField_RoamingPokemon`（248 行）整段未做。
6. **【缺】遭遇修正链** —— `PField_EncounterModifiers`（52 行）、`Events.onWildPokemonCreate`（`PField_Encounters:461`）。
7. **【缺】`$PokemonTemp.encounterType`** —— `PField_Encounters:468`；影响 `pbGetEnvironment`（`PField_Battles:197-203`）与钓鱼/冲浪判定。

---

## 3. 野生宝可梦生成差异（`PField_Encounters:417-463` + `PokeBattle_Pokemon:909-964`）

| 项 | 插件 | 运行时 |
|---|---|---|
| 性格值 `personalID` | `PokeBattle_Pokemon:919-922`（4×`rand(256)` 拼 32 位） | **从不赋值**（`Pokemon.java:48` 默认 0）→ 性别恒男、永不闪光 |
| 个体值 IV | `:929` `rand(IV_STAT_LIMIT+1)`，`IV_STAT_LIMIT=31`（`PokeBattle_Pokemon:58`） | `HeadlessBattlePort:123` `random.nextInt(32)` ✅ |
| 特性槽 | `:220/235` `@personalID&1` | **未实现**（固定第 1 特性，`Pokemon.java:85`） |
| 性格 | `:287` `@personalID%25` | 随机 ✅（但来源不同：插件由 personalID 派生） |
| 性别 | `:176` `(@personalID&0xFF) < genderByte(rate)` | 公式在（`Pokemon.java:217`），但 personalID=0 → 恒男 |
| 闪光 | `:314-321` `a=personalID^trainerID; d=(a&0xFFFF)^((a>>16)&0xFFFF); d<SHINY_POKEMON_CHANCE(32)`（`Settings:44`） | **未实现**（`shiny` 恒 false） |
| 梦特 | `PField_Encounters:419-421` `rand(4096)<256` → `setAbility(2)` | **未实现** |
| 持有物 | `:422-435` `wildHoldItems` + 几率 `[50,5,1]`／复合眼`[60,20,5]`／超幸运`[50,50,50]`，且三项相同必给 | **未实现** |
| 闪光护符 | `:436-442` 再多摇 2 次（3 倍） | **未实现** |
|  Pokérus | `:443-446` `rand(65536)<POKERUS_CHANCE(3)`（`Settings:45`） | **未实现**（字段在 `Pokemon.java:63`，从不设置） |
| 迷人身躯/同步 | `:448-459` Cute Charm 改性别、Synchronize 改性格（非漫游） | **未实现** |
| 形态/`battleRank`/`obtainMap`等 | `PokeBattle_Pokemon:951-957` | 多数未建模 |

---

## 4. 战斗后处理差异

`pbAfterBattle`（`PField_Battles:619-658`）：
1. **【缺】剧毒转普通** —— `:621` `pkmn.statusCount = 0 if status==POISON`
2. **【缺】全队解除 Mega/原始回归** —— `:622-623` `makeUnmega` / `makeUnprimal`
3. **【缺】ZACIAN/ZAMAZENTA 铁头 PP ×3 补正** —— `:624-630`
4. **【缺】战斗后进化检查**（`afterBattleCheck`）—— `:631-640`
5. **【缺】搭档队伍治疗** —— `:642-649`
6. **【缺】`canLose` 战败时全队治疗 + 等待** —— `:650-655`
7. **【缺】`$game_player.straighten`** —— `:657`

`Events.onEndBattle`（`:684-708`）：
8. **【缺】拾取 `pbPickup`** —— `:734-799`（10%、特性 PICKUP、按等级取池、权重 `[30,10,10,10,10,10,10,4,4,1,1]`）
9. **【缺】采蜜 `pbHoneyGather`** —— `:801-808`（`5+((level-1)/10)*5` %）
10. **【缺】战败白屏走 `pbStartOver` + `bgm_unpause`/`bgs_unpause`** —— `:701-706`；
    运行时由 `InteractiveBattlePort:66` 直接调 `whiteout`，且不区分 `canLose` 的时机

---

## 5. 回合引擎差异（`PokeBattle_Battle`／`Battle_Phase_*`／`Battle_Action_*`）

1. 【缺】濒死换人：没有「要更换宝可梦吗？」询问、不让玩家选人（直接派队首），无上场文案 —— `Battle_Action_Switching:209-231`（拒绝则 `pbRun(idxBattler,true)`）、`:284-305`；`Battle.java:742-749`、`InteractiveBattlePort:130-142`
2. 【缺】对手出招无播报，只显示玩家那行 —— `Battler_UseMove:290`；`InteractiveBattlePort:122-142`
3. 【错】烧伤回合伤害写死 1/8，本项目 NEWEST 应为 **1/16**（BOSS 为 /40）；冻伤/恶梦/诅咒/寄生种子/束缚/盐腌全缺 —— `Battle_Phase_EndOfRound:430`；`Battle.java:381`
4. 【错】回合结束伤害打到**后备队员**（插件只遍历场上），且按队伍序而非速度序 —— `Battle_Phase_EndOfRound:216-217/391`；`Battle.java:360-368`
5. 【缺】无天气系统（开场播报、沙暴/冰雹回合伤害、倒计时） —— `Battle_StartAndEnd:327-340`、`Battle_Phase_EndOfRound:35-110`
6. 【缺】无携带道具回合结算（剩饭/黑泥/火珠/毒珠/树果），而野生宝可梦本身会带道具 —— `Battle_Phase_EndOfRound:298-300`、`ChainCatching:255-260`
7. 【缺】特性完全不参与战斗 —— `PokeBattle_Move:296-541`、`Battle_Phase_Attack:69`
8. 【缺】招式效果只映射约 60 个 function code（自爆 0E0 不自爆、集气 023 成空操作） —— `Move_Effects_000-07F:676-689`、`Move_Effects_080-0FF:2862`；`MoveEffects.java:51-117`
9. 【错】道具结算位置：插件按速度序在攻击阶段结算，运行时先加血再让对手出手 —— `Battle_Phase_Attack:72-93`
10. 【缺】回合内判出胜负后插件直接跳出（不跑 EOR）；反作用力倒下时不应跳过对手行动 —— `PokeBattle_Battle:381-386`、`Battle_Phase_Attack:127-133`；`Battle.java:144-148`
11. 【错】麻痹减速 /4，NEWEST 应为 **/2**（直接影响出手顺序） —— `PokeBattle_Battler:269-271`；`Battler.java:139-141`
12. 【缺】变化招式不判命中，未命中也没有「没有命中」提示 —— `Battler_UseMove_SuccessChecks:604-660`
13. 【缺】没有平局（decision 5） —— `PokeBattle_Battle:587-594`
14. 【缺】战斗规则只记录不生效：`cannotRun`/`disablePokeballs`/`noexp`/`nomoney`/`setstyle`、`$game_switches[60]` —— `Battle_Action_Running:5-28`
15. 【错】逃跑判定：速度快时应 `rate=256` 必成功，且用**未修正**速度 —— `Battle_Action_Running:136-148`；`InteractiveBattlePort:214`
16. 【缺】场地系统（电气/青草/薄雾/超能）与青草场地回复 —— `Battle_Phase_EndOfRound:115-147`
17. 【缺】条款（Clauses）未接（野生默认 `@rules` 为空，影响面小） —— `PokeBattle_Clauses:11-48`
18. 【错】文案自造：`"使用了 招式！"`（插件 `Battler_UseMove:290` 为 `"{1}使用{2}！"`）、`"训练家对战中无法逃跑。"`（`Battle_Action_Running:58` 为 `"不行！\n绝不能临阵脱逃！"`）、`"逃跑失败！"`（`:155` 为 `"无法逃跑！"`）、`"成功逃跑了！"`（`:151` 为 `"安全地逃跑了！"`）、道具文案缺训练家名（`Battle_Action_UseItem:79`）
19. 【缺】`HeadlessBattlePort` 用 null controller（玩家侧也由 AI 出招），且不设 `trainerBattle/playerName`/等级锁 —— `Battle_Phase_Command:209-212`

## 6. 捕捉系统差异（`PokeBall_CatchEffects`／`PokeBattle_BattleCommon`）

1. 【缺·已修】26 种球只有 4 种能扔；球的专属倍率全无 —— `PokeBall_CatchEffects:1-30`、`PItem_Items:89-92`
2. 【缺·已修】判定是单次 `x/255` 而非 `x→y→4 次摇晃` —— `PokeBattle_BattleCommon:170-232`
3. 【缺·已修】捕捉率不吃徽章/图鉴加成（`numbadges*2 + pokedexOwned/32`） —— `PokeBall_CatchEffects:68-75`
4. 【缺·已修】睡眠/冰冻应为 **2.5 倍**（原为 2 倍）
5. 【缺·已修】不写 `pkmn.ballused` —— `PokeBattle_BattleCommon:152`
6. 【缺·已修】成功文案自造（"成功捕捉了 X！"/"宝可梦挣脱了！"），应为 4 档摇晃文案 + `"太好了！\n捉到了{1}！"` —— `:116-131`
7. 【缺·已修】没有 `OnCatch`（治愈球 `heal`、亲密球 `happiness=200`） —— `:251-257`
8. 【缺】"队伍和仓库已满。"/"不能捕捉训练家的宝可梦。"自造 → 已改为 `"电脑里已经没有空间了！"`/`"训练家打飞了球\n不要做小偷！"`（`PItem_BattleItemEffects:26-28`、`PokeBattle_BattleCommon:100-103`）
9. 【缺】捕捉不给经验（`GAIN_EXP_FOR_CAPTURE=true`） —— `PokeBattle_BattleCommon:134-138`；本轮已接 `awardCaptureExperience()`
10. 【缺】没有捕捉演出（`PokeballThrowCaptureAnimation:20-141`）
11. 【缺】不看 `$game_switches[60]`／`battle.disablePokeBalls`／`battleRank>1` —— `PokeBattle_BattleCommon:104-107`、`PItem_BattleItemEffects:30-33`
12. 【缺】捕捉后不记 `firstmoves`、不 refreshChain、无图鉴登记消息、`pbStorePokemon` 不先回血 —— `:156`、`ChainCatching:408-416`、`PokeBattle_BattlePeer:30`
13. 【缺】没有快速捕捉界面（A 键） —— `ES's Fast Catching:154-169`
14. 【非缺口】会心捕捉（`ENABLE_CRITICAL_CAPTURES=false`，Settings:164）、`OnFailCatch`（本工程无注册）

## 7. 战斗个体/数值差异

1. 【缺】战斗内完全没有特性系统（伤害/命中/免疫/换人触发） —— `Battler_AbilityAndItem:5-35`
2. 【缺】BOSS 分级未接：HP 倍率 `[1,1,2,5,10,15,15,20]`、伤害上限、rank>1 不倒下降服捕捉、rank>2 免异常 —— `PokeBattle_BOSS:3`、`Battler_Initialize:366-369`
3. 【缺】伤害缺天气/场地/墙/徽章/沙雪特防乘区 —— `Move_Usage_Calculations:392-460/508-531`
4. 【错】会心：应为 `[24,8,2,1]`（1/24，NEWEST）且会心时无视能力等级 —— `Move_Usage_Calculations:198/223-224/268-277`；`Battle.java:177`
5. 【错】麻痹减速 /4 → /2 —— `PokeBattle_Battler:268-271`
6. 【错】烧伤回合伤害 1/8 → 1/16 —— `Battle_Phase_EndOfRound:425-431`
7. 【缺】变化招式不判命中 —— `Battler_UseMove_SuccessChecks:604-643`
8. 【缺】异常状态附加缺大晴天/场地/吵闹/神秘守护/BOSS 免疫 —— `Battler_Statuses:57-84/181-198`
9. 【缺】没有 `participants`，经验只发给补刀者，无 Exp.Share/Exp.All —— `Battler_Initialize:97/400-404`、`Battle_ExpAndMoveLearning:15-64`
10. 【缺】胜利不涨努力值（`pbGainEVsOne`） —— `Battle_ExpAndMoveLearning:33-43/68`
11. 【缺】换人不重置能力等级与混乱 —— `Battle_Action_Switching:313`、`Battler_Initialize:66-70/127-156`
12. 【缺】能力等级 ±6 静默 clamp，无「不能再提高了」文案 —— `Battler_StatStages:9-26/126-170`
13. 【缺】战斗内道具（树果/剩饭/气势披带）不触发 —— `Battler_AbilityAndItem:235-288`
14. 【缺】变身/改变属性/回合末形态检查 —— `Battler_ChangeSelf:350/418-446`
15. 【缺】回合末只有毒/烧伤：缺天气伤害、寄生种子、Yawn、灭亡之歌 —— `Battle_Phase_EndOfRound:91-105/327-338/570-607`
16. 【缺】异常状态没有战斗文案（`pbContinueStatus`） —— `Battler_Statuses:443-466`
17. 【错】命中/闪避把两个阶段并成一个差值 —— `Move_Usage_Calculations:130-140`
18. 【缺】HP 公式缺脱壳忍者特例、等级上限硬编码 100（`MAXIMUM_LEVEL=210`，Settings:42）

## 8. 指令界面 / AI 差异

1. 【缺】数据盒不画状态图标（中毒/麻痹/睡眠看不出来） —— `PokeBattle_SceneElements:269-275`
2. 【错】招式列表按 PP>0 过滤后重建：0PP 招式整格消失、后续左移，且 PP 读数错位 —— `PokeBattle_SceneMenus:337-343/373-380`；`Battler.java:171-186`、`BattleScreen.java:202-204/1481-1486`
3. 【缺】A 键「快速捕捉」界面 —— `Scene_Commands:64-80`、`ES's Fast Catching:154-169`
4. 【缺】野生 BOSS 的 ×N 标记、闪光/超闪光图标、Mega/原始回归图标 —— `PokeBattle_SceneElements:230-264`
5. 【错】逃跑文案与判定（见 §5.15/§5.18）
6. 【缺】F5 战斗信息页／招式详情、光标记忆 `@lastCmd/@lastMove`、T 键代欧奇希斯形态 —— `Scene_Commands:60-63/94-96/149-172`
7. 【缺】PP 三档配色、已捕获 `icon_own`、等级>200 显示 `???`、长名字左移 —— `PokeBattle_SceneMenus:216-221/406-412`
8. 【错】野生 AI 应「可选中招式各 100 分后按分加权随机」；运行时是确定性取最大 `威力×相性×STAB`，且跳过变化招式、规避相性 0 —— `AI_Move:9/20-24/125-132/153-155`；`Battle.java:712-740`
9. 【缺】`battleRank>=2` 的野生 BOSS 应走 `pbRegisterMoveTrainer` 评分路径（skill=20×rank） —— `AI_Move:9/20-24`
10. 【缺】AI 评分不含命中率、多段招式、灼伤/墙/会心/天气/道具 —— `AI_Move:287-309`、`AI_Move_Utilities:215-224/266-575`
11. 【非缺口】`AI_Item`/`AI_Switch` 在野生战斗不触发（`AI_Item:41`、`AI_Switch:106-107`），与运行时一致

---

## 9. 本轮已修：捕捉子系统（R13）

| 项 | 实现 | 插件依据 |
|---|---|---|
| 按 `ITEM_TYPE` 识别精灵球 | `pbs.js` 导出 `type`（第 9 列）、`PbsData.Item.isPokeBall()` | `Compiler_PBS:551-573`、`PItem_Items:89-92` |
| `$BallTypes` 28 种球表 | `pokemon/runtime/pokemon/BallTypes.java` | `PokeBall_CatchEffects:1-50` |
| 捕捉判定 `x→y→4 次摇晃` | `battle/CaptureCalculator.java#shakes` | `PokeBattle_BattleCommon:170-232` |
| 徽章/图鉴加成、21 种球倍率 | `CaptureCalculator#modifyCatchRate` | `PokeBall_CatchEffects:68-247` |
| `OnCatch`（治愈球/亲密球） | `CaptureCalculator#onCatch` | `:251-257` |
| 全部文案照抄（出手/4 档摇晃/捉到了/打飞球/不可捕捉/仓库满） | `InteractiveBattlePort#ball` | `PokeBattle_BattleCommon:82-131`、`PItem_BattleItemEffects:24-53` |
| `ballused`、`makeUnmega`、`recordFirstMoves` | `InteractiveBattlePort#ball` | `PokeBattle_BattleCommon:152-158` |
| 捕捉给经验 | `Battle#awardCaptureExperience` | `:134-138`（`Settings:165`） |
| `battle.time`（洞窟/昼夜→黄昏球） | `setBattleEnvironment` + `DayNightTone` | `PField_Battles:181-188` |
| `battle.environment`（潜水球） | `metadata.environment` → PBEnvironment id | `PField_Battles:146-151/194-217` |

**未做**：捕捉演出（`PokeballThrowCaptureAnimation`）、快速捕捉界面、连锁（`ChainCatching`）、图鉴登记消息、`pbStorePokemon` 先回血、`pbIsSnagBall?` 的暗影宝可梦分支、Safari。

## 10. R14 已修：野生战斗外壳（§2 + §4 + 逃跑）

| 项 | 实现 | 插件依据 |
|---|---|---|
| `pbWildBattle` 的 3 个可选参数进 IR（`outcomeVar≠1`/`canRun=false`/`canLose=true` 才写规则） | `script-compiler.js#addWildBattleOptions` | `PField_Battles:359-361` |
| `pbFreeWildBattle(local, outcomeVar, …)` 同样保留参数 | `script-compiler.js` 正则分支 | `FreeWildBattle:2-8` |
| **野生战斗写结局变量**（默认 1；训练家战原有逻辑不变） | `EventInterpreter#prepareWildBattle` | `PField_Battles:343` |
| `pbWildBattle` 作为脚本条件分支（`decision!=2 && !=5`） | `EventInterpreter#startWildBattleCondition` | `PField_Battles:367` |
| `canRun`/`canLose`/`disablePokeBalls` 规则被消费 | `prepareWildBattle` + `BattlePort#setCanRun` | `PField_Battles:101/109` |
| 逃跑判定：速度更快 → `rate=256` 必逃；否则 `speed*128/foe+30*次数`；**用未修正速度** | `InteractiveBattlePort#escape` | `Battle_Action_Running:132-156` |
| 幽灵系必逃（`NEWEST_BATTLE_MECHANICS=true`） | 同上 | `:79-84` |
| `cannotRun` → `"逃跑失败了！"` 且不消耗回合 | 同上 | `:74-77` |
| 逃跑文案 `"安全地逃跑了！"` / `"无法逃跑！"`（原为自造） | 同上 | `:151` / `:155` |
| 训练家战拒绝逃跑 `"不行！\n绝不能临阵脱逃！"`（原为自造） | 同上 | `:58` |
| `pbAfterBattle`：解除 Mega/原始回归、ZACIAN 铁头 PP×3、`canLose` 全队治疗 | 新 `battle/BattleAftermath.java` | `PField_Battles:619-655` |
| `Events.onEndBattle`：胜利/捕捉后**拾取 `pbPickup`**（10%、18+11 池、等级段、权重 30/10×5/4/4/1/1）+ **采蜜 `pbHoneyGather`**（`5+((lv-1)/10)*5`%） | 同上 | `:684-708`、`:734-808` |
| 白屏时机改为 `onEndBattle`（`!canLose` 的战败），且在 `pbAfterBattle` 之后 | `InteractiveBattlePort#finish`、`HeadlessBattlePort#run` | `:656`、`:701-706` |
| 招式文案 `"{1}使用{2}！"`（原为 `"使用了 X！"`）、道具文案 `"{训练家}使用了{道具}。"`（原缺训练家名） | `InteractiveBattlePort` | `Battler_UseMove:290`、`Battle_Action_UseItem:77-80` |
| 战败文案 `"所有的宝可梦都倒下了……"`（原为自造） | `InteractiveBattlePort#endMessage` | `Battle_StartAndEnd:480` |

**验证**：core **451/451**（R14 新增 12 条）、builder **295/295**、`build-data` 基线不变
（`5441/5202/43/196`，96.4%）；81 帧截图回归只有 1 处像素变化，且集中在
`l1-battle-settle3` 的 x126..211 / y389..411 单行文字区（即招式文案修正），其余 80 帧含
`l1-battle.png`、`l1-map-after-load.png` 全等。

**未做（诚实清单）**：
- `pbAfterBattle:621` 剧毒转普通：运行时剧毒计数只在 `Battler.toxic` 上、随战斗丢弃，出战即普通，无需动作；
- `:631-640` 战后进化：本工程只有 `CriticalHits`/`DamageDone` 两种 `afterBattleCheck`（`Pokemon_Evolution:849-862`），而 PBS 无任何种族使用它们 → 该分支恒不触发（已核对 1216 个种族）；
- `:642-649` 搭档队伍治疗：多人对战未实现；
- `:657` `$game_player.straighten`（行走动画复位）未接；
- 对手出招不播报（`Battler_UseMove:290` 的对手侧），需要引擎回报双方行动（§5.2）；
- **待确认**：指令菜单第 4 项在插件里是 `_INTL("Run")`/`_INTL("Cancel")`（`Scene_Commands:13`，英文原文），
  运行时用 `"逃跑"`；`"逃跑"` 本身是工程里存在的字符串（`PokeBattle_SafariZone:259`），
  但 `Data/intl.dat` 的 ScriptTexts 表尚未解出 `"Run"` 的译文，故暂不改动、留待确认。
  → **R17 已解决**：`intl.dat` 里没有 `Run`/`Cancel` 键，插件显示英文；已独立成
  `BattleScreen.commandLabels(...)` 并采用 `Run`/`Cancel`/`呼唤`，详见 §16。

## 12. R15 已修：野生个体生成（§3）

| 项 | 实现 | 插件依据 |
|---|---|---|
| 性格值 `personalID` = 4×`rand(256)` 拼 32 位（原来恒为 0） | 新 `pokemon/WildGenerator.java`、`Pokemon.newPersonalID` | `PokeBattle_Pokemon:919-922` |
| 特性槽 = `personalID & 1`（含槽位空时回退另一槽） | `WildGenerator.naturalAbility` | `:219-245` |
| 性格 = `personalID % 25`（同步特性可覆盖） | `WildGenerator.natureOf` | `:286-288` |
| 性别 = `(personalID & 0xFF) < genderByte(rate)`，单一性别种族优先 | `Pokemon.effectiveGender()`（`displayGender` 改为委托） | `:166-177`、`PBGenderRates:11-23` |
| 梦特 `rand(4096) < 256` → `setAbility(2)` | `WildGenerator.generate` | `PField_Encounters:419-421` |
| 持有物：`wildHoldItems` + 50/5/1（复合眼 60/20/5、超幸运 50/50/50），三项相同必给 | `WildGenerator.giveHeldItem` | `:422-435` |
| 闪光 = `(pid^trainerID)` 两半异或 `< 32` | `Pokemon.isShiny` | `PokeBattle_Pokemon:314-321`、`Settings:44` |
| 闪光护符再多摇 2 次 | `WildGenerator.generate` | `:436-442` |
| Pokérus `rand(65536) < 3`（strain/time 编码） | `WildGenerator.givePokerus` | `:443-446`、`Settings:45` |
| 迷人身躯改性别（2/3）、同步改性格（非漫游） | `WildGenerator.generate` | `:448-459` |
| IV `rand(32)`、EV 全 0、`@item/@ballused` 归零、OT/obtain 字段 | `WildGenerator.generate` | `PokeBattle_Pokemon:925-955` |
| **训练家 32 位 id**（`$Trainer.id`，闪光公式需要） | `TrainerState.id` / `publicID()` / `newId()`，存档往返 | `PokeBattle_Trainer:259-266`、`:35-37` |
| 宝可梦 `trainerID`（完整 32 位）与派生 `publicID` | `Pokemon.trainerID` + `setTrainerID`，存档往返 | `PokeBattle_Pokemon:39/67-69` |
| 赠送/搭档队伍写入完整 trainerID | `EventInterpreter` | `PField_Field:1406-1410` |

**验证**：core **465/465**（R15 新增 14 条：性格值/性格/特性槽/梦特频率/性别/持有物三档概率/
三项相同/领队特性/迷人身躯 2/3/同步/闪光公式与频率/护符/Pokérus 编码/OT 与 obtain 字段/IV-EV 范围）；
builder 295/295；`build-data` 基线不变。截图回归：81 帧中战斗帧出现一处 **22×23 像素**的变化，
位置正是**对方数据盒的性别符号**（x449..470 / y15..37），即 `personalID` 开始真正参与派生的可见证据；
其余为标题动画噪声与 R14 已解释的招式文案行。

**未做（诚实清单）**：
- `Events.onWildPokemonCreate`（`:461`）钩子未接（本工程无注册者）；
- 漫游宝可梦的 `isRoamer` 分支（`PField_RoamingPokemon` 整段未做）；
- `superShiny`（超闪光）概率未接；
- `@obtainText` / 命运邂逅文案、`@timeReceived`、`@hatchedMap` 未建模（summary 已有显示位）。

## 14. R16 已修：回合结算 + NEWEST 数值（§5.3/5.4/5.6/5.11，§7.4-7.6）

> **为什么没做原定的「开场流程（天气/场地）」**：核对后确认它是**死代码**——
> 全工程只有 17 个脚本块调用 `$game_screen.weather(PBFieldWeather::X,…)`（分布在 5 张地图），
> 而这 17 块至今是 `unsupported`（`no handler for weather`），运行时的**地图天气系统根本不存在**
> （全工程 grep `weather` 只命中 `BattleRules` 的规则字段）。也就是说：
> `pbPrepareBattle` 读到的 `$game_screen.weather_type` 恒为 0，`setBattleRule("weather"/"terrain")`
> 全工程 0 处调用 ⇒ 现在做战场天气只会是死代码。要做得先做**地图天气子系统**
> （`Game_Screen#weather` + `PField_Weather` 281 行 + RMXP 天气指令 236/237 + `weather(...)` 脚本 API）。

| 项 | 实现 | 插件依据 |
|---|---|---|
| 回合结束**只结算场上宝可梦**（原来会打到后备） | `Battle.fieldedBySpeed()` + `endOfTurn` | `Battle_Phase_EndOfRound:391/424`、`Battle_Action_AttacksPriority:253-260`（`pbPriority(true)`） |
| 回合结束**按速度排序**（原来按队伍数组序） | 同上 | `:257-259` |
| 烧伤 `1/16`（NEWEST；原为 1/8） | `Battle.burnDamage` | `:430`、`Settings:160` |
| 烧伤/中毒 BOSS（`battleRank>2`）`/40`；剧毒 `*Toxic/160` | 同上 | `:412-413/427-428` |
| 中毒 `/8`、剧毒 `n/16`、计数先加后算、上限 15 | `Battle.poisonDamage` | `:394-396/414-415` |
| 状态伤害文案（`pbContinueStatus`） | `Battle.endOfRoundMessages` → 战斗消息队列 | `Battler_Statuses:443-464` |
| 麻痹减速 `÷2`（NEWEST；原为 ÷4） | `Battler.speed()` | `PokeBattle_Battler:268-271` |
| 会心表 `[24,8,2,1]`（1/24；原为固定 1/16）+ 高会心招式 `h` 标记 + 集气各 +1 档 | `Battle.isCritical` | `Move_Usage_Calculations:198/223-226/228` |
| 会心时无视能力等级（攻方降低/防方提升都归零） | `Battler.attack/defense/spAtk/spDef(critical)` | `:266-277` |
| **`pbThis`**：`野生的{X}` / `特殊的{X}` / `强大的{X}` / `对手的{X}`（战斗文案现在带前缀） | `Battler.thisName()` | `PokeBattle_Battler:214-231` |

**验证**：core **475/475**（R16 新增 10 条：只打场上/后备不受影响、烧伤 1/16、BOSS 1/40、
中毒 1/8 与剧毒 n/16 与上限、速度序、麻痹减半、会心无视能力等级、会心表三档频率、
濒死跳过、无异常时无文案）；builder 295/295；`build-data` 基线不变。
截图回归（81 帧，同一时刻钉死色调）：只有一处战斗帧变化 ——
`l1-battle-settle4` 的 **229×23 单行文字区**（y389..411），即战败文案加上 `野生的` 前缀；
其余为对方数据盒性别符号（R15 的随机性格值）与标题/时钟噪声。
**说明**：探针战斗一击结束且无异常状态，所以本轮的回合结算本身拍不到，由 10 条单元测试覆盖。

**未做（诚实清单，均属其它子系统）**：天气/场地的回合伤害与倒计时（依赖地图天气系统）、
寄生种子/束缚/盐腌/延烧等其它回合伤害（依赖 PBEffects 与招式效果）、
剩饭等携带道具回合结算、特性（`POISONHEAL`/`HEATPROOF`/`QUICKFEET`）、
`pbContinueStatus` 的动画、`pbPriority` 的 tie-breaker 与顺风/沼泽（需要 side effects）。

## 16. R17：指令菜单标签独立成函数 + 「逃跑不会逃不掉」的证明

**`Run`/`Cancel` 的结论（有证据，不再存疑）**：
`Scene_Commands:13` 用的是英文原文 `_INTL("Run")` / `_INTL("Cancel")`。
`_INTL` 走 `MessageTypes.getFromHash(MessageTypes::ScriptTexts=24, 原文)`（`Intl_Messages:563/613/720-731`），
而 `Data/intl.dat` 的第 24 张表（5677 个键，键就是英文原文，例如 `"Items"`/`"Medicine"`）
**没有 `"Run"`/`"Cancel"` 这两个键**（按 marshal 键的形式 `22 03 Run` / `22 06 Cancel` 逐字节检索为 0 次；
`intl.dat` 里 21 处裸 `Run` 全是别的长串的一部分）⇒ 插件菜单实际显示的就是英文 **`Run`**。
`"逃跑"` 在工程里只属于狩猎区菜单（`PokeBattle_SafariZone:259`）。

因此新增独立函数 `BattleScreen.commandLabels(...)`（`Scene_Commands:8-17` 的逐行转译）：

| 第 4 项 | 条件 | 插件 |
|---|---|---|
| `呼唤` | 暗影宝可梦 + 训练家战 | `:13`、`:16` |
| `Run` | 本回合第一个行动 | `:13` |
| `Cancel` | 本回合后续行动（`:17` 把 3 转成 -1） | `:13`、`:17` |

运行时单人战里玩家每回合只有一个行动，所以实际恒为 `Run`；暗影类型未建模故 `呼唤` 走不到
（函数保留该分支并写明条件）。**没有采用 `"逃跑"`**：那是狩猎区的串。

**「不会逃不掉」的证明**（用户提出）：`pbRun:143-149` 的 `rate = speed*128/foe + 30*@runCommand`，
`@runCommand` 每次失败 +1（`:135`），因此**连续 10 次失败后 rate ≥ 300 > 256**，
`rate>=256` 必成功（`:149`）—— 野戦只有 `setBattleRule("cannotRun")` 会拒绝（`:74-77`，
文案 `"逃跑失败了！"`）以及未实现的特性/效果（`pbCanRun?` 的 trapping 分支）。
新增测试 `escapeIsNeverADeadEnd` 直接跑循环把这个性质钉住（≤10 次必逃）。

## 18. 用户实测报出的野生战斗 bug（R18 工作单）

> 按用户原话整理并逐条核对；「状态」在修完后更新。用户说「肯定还有更多，只是我自己测不全」，
> 后续继续按同样方式核对。

| # | 现象 | 插件依据 | 核对结果 | 状态 |
|---|---|---|---|---|
| 1 | 「哦！一只野生的{X}出现了！」之后**没有「去吧！{X}！」** | `Battle_StartAndEnd:196-216`（野生开场，`pbDisplayPaused`）+ `:253-265`（我方 `_INTL("去吧！\n{1}！")`，`pbDisplayBrief`） | `InteractiveBattlePort.Session` 只给野生开场一句，`BattleScreen` 没补 → **确认缺** | 待修 |
| 2 | 这段期间**玩家角色立绘没出现** | `Scene_Animations:85 pbSendOutBattlers` + `Follower_Main:727`（我方弧线投球前玩家训练师立绘在场） | 野生战只画球与宝可梦 → **确认缺** | 待修 |
| 3 | 换人**没有「你已经尽力了，{X}！」/「加油啊，{X}！」** | `Battle_Action_Switching:284-305 pbMessagesOnReplace` | 换人只显示自造的「去吧，X！」 → **确认缺** | 待修 |
| 4 | **一回合可以无限换人** | `Battle_Phase_Command`：换人占用本回合指令 | 待核对 | 待核对 |
| 5 | 选「战斗」**每回合重放派出动画** | `Battle_StartAndEnd:229-274` 只在开场送出一轮 | **已定位**：`BattleScreen.closeMessage():391-393` 在任何消息结束且无结果时都跳回 `SEND_FOE/SEND_PLAYER` → 明确缺陷 | 待修 |
| 6 | 野生宝可梦**没有倒下动画** | `pbFaintBattler`（数据盒消失 + 精灵倒下 + SE） | 只改 HP + 一行文字 → **确认缺** | 待修 |
| 7 | 经验结算窗口**没有确认键**，显示完直接结束战斗 | `Battle_ExpAndMoveLearning:286-296` → `pbLevelUp` 窗口 + `pbDisplayPaused` | 运行时 `expWindowTimer = 1.8f` 定时自动前进 → **确认缺** | 待修 |
| 8 | 结束战斗**没有黑屏 + 淡入** | `PokeBattle_Scene:296-303 pbEndBattle` + `PField_Visuals:121-131` | 训练师收尾有 0.5s 淡出，野生战没有 → **确认缺** | 待修 |

### R18 进展（本轮）

| # | 状态 | 说明 |
|---|---|---|
| 5 | **已修** | `BattleScreen` 新增 `sendOutPending`：送出动画只在开场那句之后跑一次；其它消息（招式/道具/球失败）结束直接回指令菜单；送出阶段里 `closeMessage` 只清行、由阶段自身推进。 |
| 1 | **已修** | `sendOutFirstSide()` 与 `SEND_FOE→SEND_PLAYER` 分别播 `"{训练家}派出了\n{X}！"`（`:242`）和 `"去吧！\n{X}！"`（`:258`）。**关键**：`render()` 原本只在 `MESSAGE/BATTLE` 阶段画消息窗，`SEND_*` 期间不画 —— push 了也看不见，已补。 |
| 3 | **已修** | `Session.recallMessage()`（`Battle_Action_Switching:264-281` 五档）与 `replaceMessage()`（`:284-305` 四档）；为此给 `Battler` 加了 `turnCount`（`Battle_Phase_Attack:174`）。**原来自造的「去吧，X！」已删除。** |
| 2/4/6/7/8 | 待修 | 玩家立绘、换人回合限制、倒下动画、经验窗确认键、结束黑屏淡入 |

> **B1 更新（本轮）**：#1/#2/#3/#5 已全部改为**逐行转译**（不再是上面的"功能级"实现，
> `sendOutPending` 标志位也已删除，见「B1 已完成」）。仍待修：#4（换人回合限制，B2）、
> #6（倒下动画，B3）、#7（经验窗确认键，B4）、#8（结束黑屏淡入，B5）。

**验证缺口（重要）**：截图探针的战斗帧是**固定帧数**采样，实测基本停在开场那句附近，既到不了指令菜单、
也抓不到送出期间的消息窗；本轮 `l1-battle-fight` 变化 21.5% 只是时间线被推后、采样点落到别的阶段。
**这正是这些 bug 之前没被探针发现的原因** —— 下一步先给探针加定点采样（送出中 / 指令菜单 / 换人两句 /
经验窗 / 结束淡出），再继续修剩下 5 条。

## 19. 按新铁律需要重做/上报的既有实现（自查清单）

> R13–R18 期间我按"功能级"写过一些东西。逐条对照新铁律（必须逐行转译、不许近似）自查如下。
> 每条要么**重做成逐行转译**，要么**上报用户**。

### A. 我做过"近似/替代"的地方（必须重做）

| 位置 | 我写的近似 | 插件原文 | 处理 |
|---|---|---|---|
| `CaptureCalculator#moonStoneFamily` | 只走目标种族的**前向**进化链 | `pbCheckEvolutionFamilyForItemMethodItem`（整个进化家族，含前置形态） | 重做：先逐行转译该函数（含家族回溯） |
| `InteractiveBattlePort#captureContext` 的 `environment` | 只用 PBS/metadata 的 `Environment` | `pbGetEnvironment`（`PField_Battles:194-217`：地形标签覆盖 metadata） | 重做：先转译 `pbGetEnvironment` + `pbFacingTerrainTag` |
| 同上 `safari` | 恒 false | `pbInSafari?` | **上报**：Safari 系统未做，需先决定是否做 |
| 同上 `fishingRodEncounter` | 恒 false | `$PokemonTemp.encounterType`（`:468`，钓竿/头击） | 重做：先转译 `$PokemonTemp.encounterType` 的读写点 |
| `BattleAftermath.pbAfterBattle` 的 `:621` | 论证"运行时天然成立"、未写代码 | `pkmn.statusCount = 0 if status==POISON` | 重做：把 `statusCount` 建模进 `Pokemon` 再逐行转译 |
| `BattleAftermath`（`:642-649`、`:657`） | 未实现 | 搭档治疗、`$game_player.straighten` | **上报**：前者依赖多人对战、后者依赖行走动画复位 |
| `Battle.isCritical` | 缺特性/道具/`LuckyChant` 分支 | `Move_Usage_Calculations:196-213` | 重做（依赖特性/道具系统） |
| `Battler.attack()` 的烧伤减半 | 写在能力值 getter 里 | 插件写在伤害乘区（`Move_Usage_Calculations`） | 重做：挪进伤害乘区 |
| `BattleScreen.sendOutPending`（R18） | 我引入的标志位机制 | `pbStartBattleSendOut` 是**直线流程**（`Battle_StartAndEnd:194-274`） | **B1 已重做**：改成 `BattleSendOut.plan` 的直线步骤表，标志位删除 |
| `BattleScreen.commandLabels(false, …)` | `shadowTrainer` 硬编码 false | `Scene_Commands:7`（`PBTypes::SHADOW` + 训练家战） | **上报**：SHADOW 属性未建模 |
| `WildGenerator` 的 `Events.onWildPokemonCreate` | 未触发 | `PField_Encounters:461` | **上报**：本工程无注册者，是否保留空钩子待定 |

### B. 批次计划（每批一个 Ruby 函数集合，逐行转译 + 定点截图证据）

| 批次 | 转译对象（段:行号） | 对应 bug |
|---|---|---|
| ~~B1~~ | ~~`Battle_StartAndEnd:194-275 pbStartBattleSendOut` + `Scene_Animations:85-200 pbSendOutBattlers` + `PlayerFadeAnimation`/`PokeballPlayerSendOutAnimation`~~ **已完成，见下文「B1 已完成」** | #1 #2 #5 ✅ |
| B2 | `Battle_Action_Switching:209-313`（`pbRecallAndReplace`/`pbMessageOnRecall`/`pbMessagesOnReplace`/`pbEORSwitch`）+ `Battle_Phase_Command` 的每回合指令记账 | #3 #4 |
| B3 | `pbFaintBattler` + `BattlerFaintAnimation`/`DataBoxDisappearAnimation`（`Scene_Animations`）+ 对应 SE | #6 |
| B4 | `Battle_ExpAndMoveLearning:99-308` 的窗口与按键流程（`pbLevelUp`/`pbDisplayPaused`） | #7 |
| B5 | `PokeBattle_Scene:296-303 pbEndBattle` + `PField_Visuals:121-133`（黑幕 16 帧淡回地图） | #8 |

每批开工前先把该段 Ruby 完整导出读一遍；**中途遇到 A 表或新发现的"无法转译"就停下报告，不自行绕过**。

### B1 读书笔记（已完成的准备步骤）

**要转译的函数清单（精确行号）**：

| 函数 | 位置 |
|---|---|
| `pbStartBattleSendOut` | `Battle_StartAndEnd:194-274` |
| `pbSendOutBattlers` | `Scene_Animations:85-143`（含 `:134-142` 闪光/超闪光动画） |
| `PlayerFadeAnimation` | `PokeBattle_SceneAnimations:306-358` |
| `TrainerFadeAnimation` | `PokeBattle_SceneAnimations:359-403` |
| `PokeballPlayerSendOutAnimation` | 基类 `PokeBattle_SceneAnimations:404-485`，**本工程在 `Follower_Main:727` 覆盖**（载入顺序在后，生效的是 Follower_Main 版）→ 玩家投球要照 `Follower_Main:727` 转 |
| `PokeballTrainerSendOutAnimation` | `PokeBattle_SceneAnimations:486-557` |
| `DataBoxAppearAnimation` / `DataBoxDisappearAnimation` | `PokeBattle_SceneAnimations:189-209` / `:210-…` |
| 玩家立绘 sprite 创建 | `Scene_Initialize:235-251 pbCreateTrainerBackSprite`（+ `:150` 调用点） |

**资源核对（按新铁律 4b，先确认存在再写代码）**：

| 需要的资源 | 解析规则（插件） | 工程内实际 | 结论 |
|---|---|---|---|
| 玩家/对手训练师立绘 | `pbTrainerSpriteFile`（`PSystem_FileUtilities:359-367`）：`Graphics/Trainers/trainer<CONSTNAME>`，失败回退 `trainer%03d` | `Graphics/Trainers/trainer000.png` 等 **192 个** | ✅ 存在 |
| 注意 | `pbTrainerSpriteBackFile`（`trback…`，`:369-377`）**战斗场景不用**，别拿它去找文件 | 目录内无 `trback*` | 不适用 |
| 精灵球动画 | `PokeBattle_Animation:89/204/211` `Graphics/Battle animations/ball_%02d(_open)` | 该目录 **63 个文件**，`ball_00.png`/`ball_00_open.png`… | ✅ |
| 黑幕/指令条 | `PokeBattle_SceneAnimations:40/45` `black_screen`/`black_bar` | 同目录内 | ✅ 待逐名确认 |
| 战斗 HUD 图标 | `Graphics/Pictures/Battle/icon_ball*`（`:165-172`） | 该目录 **48 个文件** | ✅ |

**结论：B1 没有缺资源，不需要停下上报。** 下一步就是按上表逐行落代码。

### B1 已完成（本轮，逐行转译）

> 为了让「一行行对着 Ruby 写」真的成立，先把插件自己的脚本精灵引擎搬了过来：
> `PokeBattle_SceneAnimations` / `Follower_Main` 的每个动画类都是对 `PictureEx`
> 的一串 `set*/move*` 调用，没有这个引擎就只能"看着像"。

**新增文件（全部逐行转译，注释带 `段名:行号`）**

| 文件 | 转译的 Ruby |
|---|---|
| `battle/PictureEx.java` | `PictureEx:1-520`（`PictureOrigin`/`Processes`/`getCubicPoint2`/`PictureEx`/`setPictureSprite`）。`ensureDelayAndDuration` 把 **延时和时长都 ×40/20**，`totalDuration` 再 ×20/40 截断 —— 这就是插件注释里 `delay = ball.totalDuration # 0 or 7` 的由来 |
| `battle/BattleSprite.java` | 精灵表的一格：`PictureEx:77-93` 的字段 + `setPictureSprite:483-498` 的 ox/oy |
| `battle/BattleSprites.java` | `PokeBattle_Scene:5 @sprites`（按 z 升序绘制） |
| `battle/BattleSpriteShader.java` | RGSS `tone`/`color` 语义（`PictureEx:396-404`；同 `map/ToneShader` 的做法）：tone 是**通道位移**、color 是**向该色插值**（alpha=255 时是纯色剪影），两者都不是乘法 |
| `battle/PokeBattle_SceneConstants.java` | `PokeBattle_SceneConstants:1-67` |
| `battle/BattleAnimation.java` | `PokeBattle_Animation:1-59` + `PokeBattle_BallAnimationMixin:63-264` |
| `battle/BattleAnimations.java` | `BattleIntroAnimation:4-58`、`BattleIntroAnimation2:66-81`、`LineupAppearAnimation:88-182`、`DataBoxAppear/Disappear:189-223`、`PlayerFadeAnimation:306-351`、`TrainerFadeAnimation:359-396`、`PokeballTrainerSendOutAnimation:486-551`、**本工程覆盖版** `PokeballPlayerSendOutAnimation`（`Follower_Main:727-813`）、以及 `pbSendOutBattlers:85-143` 的更新循环 |
| `battle/BattleSendOut.java` | `pbStartBattleSendOut`（`Battle_StartAndEnd:194-275`，含野生 1/2/3、`battleRank>1` 的 特殊的/强大的、`$game_switches[196]`、训练家 1/2/3 的全部文案）+ `pbSendOut`（`Battle_Action_Switching:324-332`）→ 直线步骤表 |

**改写文件**

| 位置 | 内容 |
|---|---|
| `battle/BattleScreen.java` | 构造函数 = `Scene_Initialize:107-174 pbInitSprites` + `Scene_Animations:5 pbBattleIntroAnimation`；`Stage.INTRO/OPENING/BATTLE/EXP_GAIN/TRAINER_END` 取代旧的 `SEND_FOE/SEND_PLAYER`；战场改为精灵表按 z 绘制；`pbCreateTrainerBackSprite:235-251`（玩家立绘，`:150` 调用）；`pbChangePokemon:318-329`/`pbRefresh:63-68`；`changePokemon` 时重指数据盒（插件里 battler 对象不变、只换 `@pokemon`，本运行时是一怪一 Battler） |
| `battle/Battler.java` / `Battle.java` | `@index`（`PokeBattle_Battler`，`pbSetUpSides:122/180` 的 `2*slot+side`）+ `refreshFieldIndices()` |
| `audio/AudioManager.java` | `pbCryFile`（`PSystem_FileUtilities:509-539`，含形态） |
| `lwjgl3/MenuCapture.java` | 定点采样：`advanceUntilBattle(状态谓词)`；新增 `switch` 模式；`MapScreen.activeBattleScreen()` 探针钩子 |

**验证**：core **509/509**（B1 新增 31：`PictureExTest` 12、`BattleSendOutTest` 6、`BattleAnimationTest` 13）；
builder **295/295**。截图（`-Dpokemon.daynight.hour=19`）：
`b1-wild`（129 帧到开场句 → 关门后 2 帧进入送出 → 84 帧到指令菜单）、
`b1-trainer`（29 帧开场句 → 送出对手方 → 送出我方 → 102 帧到指令菜单）、
`b1-switch`（换人两句：18 帧「你已经尽力了，妙蛙种子！回来吧！」→ 27 帧「加油啊！小火龙」+ 投球）。
回归：`l1-battle-entry`、`l1-battle-intro`、`l1-battle-evil-vs0..3` 与上一轮验收截图**逐像素相同**；
玩家立绘（bug #2）、「去吧！」（#1）、换人两句（#3）、每回合重放送出（#5）均已修。

**B1 未转（停下上报，未做替代品）**

1. `Scene_Animations:41-52` 的闪光/超闪光 `pbCommonAnimation("SuperShiny"/"Shiny")`
   与 `Scene_Animations:134-142` 的同一调用：需要战斗公共动画播放器
   （`PokeBattle_AnimationPlayer`，878 行 + `Graphics/Animations/` 702 个文件），本运行时没有。
2. `Follower_Main:736-737/797-802` 的 `@followAnim` 分支：需要跟随宝可梦子系统
   （`$PokemonTemp.dependentEvents.refresh_sprite`，40 块 unsupported），走不到。
3. `PokemonBattlerShadowSprite`（`PokeBattle_SceneElements:638-696` + `pbLoadPokemonShadowBitmap`）
   的地面影子**只建表不绘制**：素材 `Graphics/Pictures/Battle/battler_shadow_1..3.PNG` 在，
   但 `pbLoadPokemonShadowBitmap` 是另一个函数，不属 B1 清单。
4. `Scene_Initialize:170` 的 `Tone.new(-80,-80,-80)` 之前是"乘法变暗"的近似，本轮换成
   `BattleSpriteShader` 的 RGSS 通道位移（这是纠正，不是新近似）。
5. `pbApplyBattlerMetricsToSprite:362` 的 `- altitude*2`：`BattlerAltitude` 在本工程
   `PBS/pokemon.txt` / `pokemonforms.txt` 里**一次都没出现**（全 0），故该行是空操作，未建模。




2. ~~野生战斗外壳~~（R14，§10）
3. ~~野生个体生成~~（R15，§12）
4. ~~回合结算 + NEWEST 数值~~（R16，§14）
5. **AI**（§8.8-8.10：野生对手应按分加权随机，现在是确定性取最大威力、且跳过变化招式）
   与**界面**（§8.1-8.2：数据盒状态图标、招式列表 PP 过滤错位；指令标签已见 §16）
6. **地图天气子系统 → 战场天气/场地**（§4，见 §14 开头的说明）
7. 特性系统（§7.1）与 BOSS 分级（§7.2）

---

# 20. B2 / B4 / B5 批次（本轮，逐行转译）

> 本轮由 Lead + 两名队友并行：Lead 负责 `BattleScreen`/`Battle`/`Battler`/`InteractiveBattlePort`/`MapScreen`/docs，
> `qwen38` 负责 B3-A（倒下动画 + 叫声时长，写权限见 task-1），`kimi27` 负责 B6（§8.1/§8.2，写权限见 task-2）。

## 20.1 B2：换人子系统（§18 用户 bug #3 / #4）

| 插件位置 | 运行时实现 |
|---|---|
| `Battle_Action_Switching:9-34` `pbCanSwitchLax?` | `Battle.canSwitchLax(idx, party)`（返回 null=可换；""=静默拒绝；否则=拒绝文案） |
| `:41-113` `pbCanSwitch?` | `Battle.canSwitch`（`:67` 幽灵必换已转；`:55-107` 的特性/道具/束缚分支需要特性+PBEffects，未转） |
| `:115-120` `pbCanChooseNonActive?` | `Battle.canChooseNonActive` |
| `:122-128` `pbRegisterSwitch` | `Battle.registerSwitch` + `InteractiveBattlePort.Session.registerSwitch` |
| `:136-151` `pbPartyScreen` | `BattleScreen.openPartyScreen/finishPartyScreen`（checkLaxOnly/canCancel/shouldRegister 三个参数照抄） |
| `:155-158` `pbSwitchInBetween`、`:241-253` `pbGetReplacementPokemonIndex` | `Battle.getReplacementPokemonIndex`（玩家侧返回 `OWNER_CHOOSES` 交界面） |
| `:165-239` `pbEORSwitch` | `Battle.eorSwitchPlan` + `BattleScreen.beginEorSwitch/stepEorSwitch/playEorReplacement` |
| `:185-202` Switch 风格询问 | `BattleScreen` 的 `CONFIRM_OPPONENT` + `pbShowCommands` 窗口（`switchStyle` 来自 `PField_Battles:111-112`） |
| `:256-262` `pbRecallAndReplace` | `BattleScreen.updateSwitch`（`SwitchStep` RECALL→LINEUP→MESSAGE→REPLACE→SEND） |
| `:264-281` `pbMessageOnRecall` | `Session.recallMessage`（含 `:277-280` 对手侧） |
| `:284-305` `pbMessagesOnReplace` | `Session.replaceMessage`（含 `:301-303` "派出了"） |
| `:309-320` `pbReplace` | `Battle.replace` + `Battler.resetForSwitchIn`（=`pbInitEffects(false)` 的子集，`Battler_Initialize:112-176`） |
| `:324-332` `pbSendOut` | 复用 B1 的 `SendOutSequence` |
| `Battle_Phase_Attack:50-71` `pbAttackPhaseSwitch` | `BattleScreen.queueRecallAndReplace(...)` + `afterPlayerSwitch()`（换人后对手照常行动） |
| `Battle_Phase_Command:5-23` 清/取消指令 | `Battle.clearChoice/cancelChoice` |
| `AI_Switch:179-186` + `:188-227` | `Battle.defaultChooseNewEnemy` |
| `Battle_StartAndEnd:587-594` `pbJudge` | `Battle.judge`（1/2/5），`BattleResult.Outcome.DRAW` 新增 |
| `PokeBattle_Battle:353-355` `pbAllFainted?` | `Battle.allFainted(side)`（蛋不算能战） |
| `PokeBattle_Scene:202-237` `pbShowCommands` | `BattleScreen.showChoice/updateChoice/finishChoice/drawChoiceWindow` |
| `PokeBattle_SceneAnimations:558-603` `BattlerRecallAnimation` | `BattleAnimations.BattlerRecallAnimation` |
| `PokeBattle_Animation:238-245` `battlerAbsorb` | `BattleAnimation.battlerAbsorb` |
| `Battle_Action_Running:36-157` `pbRun` | `Session.run(duringBattle)`（返回 -1/0/1，`:222` 的 `pbRun(idx,true)` 也用它） |

**田地槽位模型**（原来 `active()` 取"第一个没倒的"，会静默换人）：现在 `Battle.playerField/foeField` 是
`@party1order/@party2order`（`PokeBattle_Battle:315-321`）的对应物，`pbReplace` 移动槽位，`refreshFieldIndices`
写回 `@index = 2*slot+side`（`pbSetUpSides:122/180`）。

**证据**：core **522**（B2 新增 13 条 `BattleSwitchingTest`）；截图探针
- `b2-t2`（trainer2，对手 2 只）：`340`帧到指令菜单 → 一击打倒对手首发 → `eor=CONFIRM_OPPONENT@0 choice=是`（"…将要派出…要更换宝可梦吗？"）→ `PARTY` → 玩家换上第 2 只（`发送中 SEND:0`，槽位 0→1）→ 对手 `派出了` + 投球（`SEND:1`，槽位 0→1）→ 回指令菜单；
- `b2-faint`（野生 50 级把玩家首发打倒）：`eor=CONFIRM_OWN@0`（"要更换宝可梦吗？"）→ `PARTY` → 换上第 2 只 → 回指令菜单；
- `b2-wild` / `b2-switch2`：与 B1 逐帧一致（129/2/84、391/17/104/84）。

**B2 未转（停下上报，未做替代品）**：
1. `:55-107` `pbCanSwitch?` 的特性/道具/束缚类分支（需特性系统 + PBEffects）；
2. `:235-238` 的 `pbEffectsOnSwitchIn`（入场特性/入场钉）；
3. `AI_Switch:206-220` `pbChooseBestNewEnemy` 的后半段（`om.calcType = pbCalcType(b)` 需要特性 `-ate` 等，需特性系统）；
4. `:185-202` 的 `isConst?(enemyParty[idxPartyNew].ability,:ILLUSION)`（特性）。

## 20.2 B4：经验/升级结算窗口与确认键（§18 用户 bug #7）

| 插件位置 | 运行时实现 |
|---|---|
| `Battle_ExpAndMoveLearning:282-283` 升到 X 级（paused） | `BattleScreen` `ExpPhase.LEVEL_MESSAGE` + `playSe("Pkmn level up")` |
| `Scene_Animations:286-294` `pbLevelUp` 的两个窗口 | `ExpPhase.LEVEL_GAIN_WINDOW/LEVEL_TOTAL_WINDOW` + `drawLevelUpWindow` |
| `PItem_Items:547-562` `pbTopRightWindow` | 每个窗口 `pbPlayDecisionSE` 并**等一次确定键**（`:553/:559`） |
| `SpriteWindow_text:114-149/213-243`（`@lineHeight=32`、`height=dims[1]+borderY`） | 窗口 `198×224`，行距 32，文本原点 `(16,16)` |
| `SpriteWindow:445-472`（`borderX/Y=32`、`startX/Y=16`） | 同上 |
| `:286-296` 招式学习（1 个直接学 / 多个先问） | `ExpPhase.LEARN_QUESTION/LEARN_MOVE` |
| `:312-327` `pbLearnMove` 空槽学会 | `showExpBriefMessage` + `Pkmn move learnt` |

**同时纠正了三处"自造 SE"**（按铁律，插件点名什么就点什么，找不到就是静音）：
- `PokeBattle_SceneElements:358` `pbSEPlay("Exp full")`：本工程**没有** `Audio/SE/Exp full`（只有 `Pkmn exp full`）→
  `Game_System:236` 的 `FileTest.audio_exist?` 判定为不播 → 运行时现在请求 `Exp full`（日志会提示 manifest 里没有），不再挪用 `Pkmn exp full`；
- `Battle_ExpAndMoveLearning:283` `pbSEPlay("Pkmn level up")`：同样不存在 → 静音（原代码挪用了 `Pkmn exp full`）；
- `Battle_ExpAndMoveLearning:197` `pbSEPlay("Pkmn exp gain")`：存在 ✔。

**未转（停下上报）**：`:329-342` 四招满时的遗忘流程需要 `@scene.pbForgetMove`
（`Scene_Commands:482-490` → `PokemonSummaryScreen#pbStartForgetScreen`），本运行时没有概要画面的遗忘模式，
所以只转译到插件的 `:329` 那一行（"想要学会…可是它已经学会四个招式了。"）为止，后面的询问/替换/三条文案都没有实现。
**触发不到的那一步**：`pbTopRightWindow` 的窗口序列现在必须连按两次确定键（与插件一致）。

## 20.3 B5：战斗结束的黑幕与淡入（§18 用户 bug #8）

| 插件位置 | 运行时实现 |
|---|---|
| `PokeBattle_Scene:296-303` `pbEndBattle` | `BattleScreen.beginEndBattle/updateEndBattle` + `Stage.END_BATTLE` |
| `MessageConfig:602-619` `pbFadeOutAndHide` | `for j in 0..16` 把每个精灵的 `color` 设为 `(0,0,0,j*16)`，然后全部隐藏（`:612-617`） |
| `Audio_Play:71` `pbBGMFade(1.0)` → `Game_System:111-115` | `AudioManager.fadeBgm(1.0f)` |
| `PField_Visuals:121-131` 战后淡入 16 帧 | `MapScreen.renderBattleReturnFade`（`BATTLE_RETURN_FRAMES=40*4/10`、`alphaDiff=ceil(255/16)`） |

原先训练家战的 0.5s 自造淡出已删除（`END_FADE_SECONDS`/`endFade` 移除），改成同一套 `pbEndBattle`。
**证据**：`b4-wild` 探针 `l1-battle-settle8` 均值 **33.9**（战斗画面正在压黑），`settle9+` 回到地图（77.1）。

**B5 未转（停下上报）**：`Battle_StartAndEnd:465` `@scene.pbShowOpponent(i)` →
`Scene_Animations:71-77` `pbShowOpponent` + `TrainerAppearAnimation`（`PokeBattle_SceneAnimations:272-297`）
尚未转译，训练家战结束时的"对手滑回画面"仍是一个 8 帧占位计时器。

## 20.4 本轮发现的两个"插件自身问题"（按铁律 4 上报用户）

1. **`Audio_Utilities:1077` 读 OGG 采样率的偏移量少跳一个 channels 字节**（`qwen38` 用真实文件复现）：
   `file.pos=page[1]` → +0 packtype、+1..6 "vorbis"、+7..10 version，Ruby 在 **+11** 直接 `fgetdw`，
   而真实识别头是 **+11 channels(1B)、+12..15 sample_rate**。于是 rate 恒被放大 ~256 倍：
   `001Cry.ogg`（mono/44100，末页 granule 41397）真时长 0.9387s，Ruby 算法得 0.003667s →
   `pbCryFrameLength` = 5（正确应为 42）→ 倒下动画里"Pkmn faint"的 `delay = 5*20/40 = 2` 帧（正确应 21 帧）。
   本工程自己的 builder 是按正确偏移解析的（`tools/audio-compiler/index.js:315-316`），所以 manifest 里的
   sampleRate/duration 是对的。**当前按 A（照抄 Ruby，含 bug）实现**——因为"以插件为准"是最高铁律，
   而原版游戏里跑的就是这个行为（叫声刚起、倒下音效立刻响）。若用户要"作者本意"（B），改一行 + 换测试期望值即可。
2. **`PokeBattle_SceneElements:358` 请求的 `Audio/SE/Exp full`、`Battle_ExpAndMoveLearning:283` 请求的
   `Audio/SE/Pkmn level up` 在本工程都不存在**（`Audio/SE` 里只有 `Pkmn exp full`/`Pkmn exp gain`/
   `Pkmn faint`/`Pkmn move learnt`）。按 `Game_System:236` 的存在性判定，这两处**在原版就是静音**。
   运行时不再挪用别的文件（见 20.2）。

## 20.5 §8.2 的根因与引擎侧修改（本轮，Lead）

`Battler.moves()` 原来返回 **PP>0 过滤后**的数组，而插件的 `@moves` 是**固定 4 槽**
（`Battler_Initialize:98-101`，空槽 = `PBMove.new(0)` 即 `id==0`，`PokeBattle_Pokemon:498-521`），
战斗菜单的每个下标都是**槽位下标**（`Scene_Commands:136` `break if yield cw.index`；
`Battle_Phase_Command:84-85` `next false if !moves[cmd] || moves[cmd].id<=0`）→ 0PP 招式会整格消失并左移。

引擎侧已改（`Battler`/`Battle`/`InteractiveBattlePort`）：
- 删除 `Battler.moves()`，新增 `moveSlot(i)` / `moveSlots()`（恒 4 项，空槽 null）/ `moveSlotPp/moveSlotMaxPp` /
  `hasUsableMove()`（=`pbCanChooseAnyMove?`，`Battle_Action_AttacksPriority:20-32`）/ `struggle(pbs)`（=`@struggle`）；
- `Battle.canChooseMove(idx,slot)` = `pbCanChooseMove?`（`:5-18`，含 `:9-11` "技能已经没有PP了！"）、
  `Battle.registerMove(idx,slot)` = `pbRegisterMove`（`:70-79`）、`chosenMove/chosenMoveSlot`；
- `Session.chooseMove(slot)` 收**槽位**，被拒时把提示行放进 `session.message` 并返回 false（菜单不关）；
- `pickMove`/`defaultAi` 全部按槽位，空槽跳过，无可用招式时用 Struggle。

界面侧（`§8.1` 状态图标、`§8.2` 按钮/PP 正式转译）由 `kimi27` 在 task-2 里完成。

## 20.6 §5.2 对手出招不播报（本轮，Lead）

**现象**：对手（野生或训练家）出招时没有任何文字，玩家只看到自己 HP 掉。用户实测更容易把它当成"
N×PC 对战新 bug"。

**插件**：`Battler_UseMove:290` `pbDisplayBrief(_INTL("{1}使用{2}！",user.pbThis,choice[2].name))` ——
**对双方都执行**；倒下时 `Battler_ChangeSelf:71` `pbDisplayBrief(_INTL("{1}倒下了！",pbThis))`。

**实现**：
- `Battle.roundMessages`（新增，按执行顺序收集）：`Battle.execute` 在通过 `canAct` 之后追加
  `{pbThis}使用{招式}！`（`:290`），在目标倒下时追加 `{pbThis}倒下了！`（`Battler_ChangeSelf:71`）；
  每次行动开始时清空（`step()` / `foeTurn()`）；
- `InteractiveBattlePort.Session.log`（新增）：按 `pbDisplay` 的先后顺序拼装本轮的行 ——
  玩家自己的动作（道具/精灵球/逃跑失败）先入队，然后接引擎的 `roundMessages`，
  `endMessage()` 再用 `MESSAGE_BREAK` 连起来交给战斗画面逐条显示（画面侧**不需要**改动）；
- 顺带修好"玩家自己的宝可梦倒下时没有提示"（原来只在对手倒下时补了那一行）。

**证据**：`InteractiveBattlePortTest#bothSidesMovesAreAnnounced`（一轮里两行 `使用`，顺序=出手顺序）。

**未转**：招式效果文案（效果拔群/没有命中/击中要害等，`Battler_UseMove_TriggerEffects` 那一批）
依赖 `damageState`，属于后续批次。

## 20.7 B3：倒下演出（§18 用户 bug #6）

| 插件位置 | 运行时实现 |
|---|---|
| `Battler_ChangeSelf:61-99` `pbFaint`（行 + `:73` `@battle.scene.pbFaintBattler(self)`） | `Battle.execute` 记 `roundMessages` + `faintEvents`；`BattleScreen` 用 `Stage.FAINT` + `faintQueue` 逐个播 |
| `Battler_ChangeSelf:50-58` `pbTakeEffectDamage`（`:56 pbFaint if fainted?`） | 回合末中毒/烧伤打死时也进 `faintEvents`（`Battle.faintFromStatus`） |
| `Scene_Animations:299-312` `pbFaintBattler` | `BattleScreen.beginFaintAnimations/updateFaint`（两个动画**同时**跑，`:305-308`） |
| `PokeBattle_SceneAnimations:648-684` `BattlerFaintAnimation` | `BattleAnimations.BattlerFaintAnimation`（qwen38，task-1；含 `:662-667` 的 duration 整数除法、`:670-675` 叫声与 `pbCryFrameLength*20/40`、`:677-683` 下落/裁切/隐藏） |
| `PSystem_FileUtilities:448-469` `pbCryFrameLength` | `BattleScreen.cryFrameLength`（音高 **100**，`:450`）+ `AudioManager.sePlayTime`（`getPlayTime2`/`oggfiletime`，`SoundLength.java`） |

**顺序**：本轮消息 → 受击动画（B7，kimi27）→ 倒下动画 → 经验结算 → `pbEORSwitch`。
`faintEvents`/`roundMessages`/`hitEvents` 都在每个行动开始时清空。

**顺带修掉一个渲染层的隐藏 bug**：`BattleScreen.renderSprites` 里 `batch.setColor(1,1,1,精灵透明度)`
会**泄漏**给下一个用 `drawDataBox` 自绘的数据盒——倒下动画把对手精灵 `moveOpacity` 到 0 时，
玩家的数据盒会被一起淡成透明（截图里表现为"面板/血条/数字全没了，只剩名字"）。
现在数据盒分支显式 `batch.setColor(Color.WHITE)`（RMXP 每个 Sprite 的 tint 本来就是独立的）。
**证据**：`lead-fix` 探针 `settle2..5` 的面板像素恢复为 `(51,51,51)`/`(112,112,112)`；
`settle1` 的 `(22,22,22)` 是经验闪光的既有表现，在修复前的 `b4-wild`/`lead-wild` 里同样存在（非回归）。

## 20.8 本轮的团队分工与任务板（用户要求引入队友）

| 成员 | 角色 | 任务 | 状态 |
|---|---|---|---|
| Lead（我） | 引擎/场景接线 + 验收 + 文档 | B2 / B4 / B5 / §5.2 / B3 接线 / 渲染 bug | 已交付 |
| `qwen38` | 转译（B3-A）→ 优化 + 验证 | task-1（`BattlerFaintAnimation` + `SoundLength`/`pbCryFrameLength` + `fadeBgm`）；task-4（性能零行为变化 + 像素回归 + 探针计数器） | task-1 已验收；task-4 进行中 |
| `kimi27` | 转译（界面/演出） | task-2（§8.1 状态图标 + §8.2 固定 4 槽 + 升级窗口几何，已验收）；task-3（B7 受击动画 + 命中/无效播报 + 插件查漏清单） | task-2 已验收；task-3 进行中 |

> **模型说明**：`spawn_teammate` 没有 model 参数，本会话两名队友均由 harness 分配
> `deepseek-v4.1-flash`；用 `workflow` 的 `model:"qwen3.8"` 覆盖时子agent 直接失败（该模型未接入），
> 因此"qwen3.8/kimi2.7-code"只作为**角色名**使用。

**验收方式（Lead）**：不采信队友自述，独立复核主证据 ——
① 状态图标：把 `icon_statuses.png` 8 行分别做模板，对 `lead-verify/b6-status-battle.png` 在 (11,41) 邻域逐偏移比对，
只有第 2 行 256/256（其余 ≤125）；② 0PP：四张截图里右下格恒有 805 个名字像素，槽1 的 PP 读数是红 477/白 0；
③ 升级窗口：`settle6→settle7` 的差异只在右对齐数值列（`+N` → `N`），窗口高 224、6 行落在 `16+32i`；
④ 结束淡出：`settle8` 全屏均值 33.9（压黑）；⑤ 换人/倒下流程：`trainer2`/`faint` 探针的分帧日志（`eor`/`switch` 状态机 + 双方槽位）；
⑥ 受击闪烁：`b7-evidence/b7-damage-00..79.png` 对敌方精灵区逐帧统计，隐藏帧恰好 4 组（`0..5/12..17/24..29/36..41`），天空对照区全程 0 变化。

## 20.9 B7：受击演出 + 播报（kimi27，task-3，已验收）

| 插件位置 | 运行时实现 |
|---|---|
| `PokeBattle_SceneAnimations:610-641` `BattlerDamageAnimation` | `BattleAnimations.BattlerDamageAnimation`（`:624-629` 按 effectiveness 选 "Battle damage normal/weak/super"，`:630-636` 四次 4 帧闪烁，`:633-634` 的 `if batSprite.visible` 守卫照抄） |
| `Scene_Animations:239-268` `pbHitAndHPLossAnimation` | `BattleScreen.Stage.HIT` + `beginHitAndHpLossAnimation()`（`:244-247` 同时起全部目标 + `:246` 血条从 oldHp 重跑）+ `updateDamageFlashes()`（`:248-266` 动画与血条双条件） |
| `Scene_Animations:224-234` `pbDamageAnimation` | `BattleScreen.playDamageAnimation`（备好入口，供回合末/天气伤害批次调用：`Battle_Phase_EndOfRound:90/97/104`） |
| `Move_Usage:264-283` `pbAnimateHitAndHPLost` | 引擎 `Battle.HitEvent{idxBattler, oldHp, effectiveness}` + `hitEvents`（`effectiveness` 按 `:273-276`：resist→1、super→2、否则 0） |
| `Battler_UseMove_SuccessChecks:659` | `{pbThis}的攻击没有命中！` |
| `Battler_UseMove_SuccessChecks:528-533` | `这不能影响{pbThis}……`（伤害招式对目标属性无效时；不造成伤害、不进 `hitEvents`） |
| `Move_Usage:329-334` / `:297-320` / `Battler_UseMove:495-506` | `击中了要害！` / `这非常有效！`·`这不是很有效……` / `击中了{N}次！` |

**同批顺带修掉的真 bug**：`Battle.accuracyCheck` 原来是"把命中与闪避并成一个 stage"
（`(3+s)/3`），而插件是 **两个 stage 各自换算成百分比再相除**
（`Move_Usage_Calculations:130-138`，`stageMul/stageDiv` 分别为 `[3,3,3,3,3,3,3,4,5,6,7,8,9]`/`[9,8,7,6,5,4,3,3,3,3,3,3,3]`）。
两者**不等价**（命中+2/闪避+1：插件 167/133≈1.256，旧实现 4/3≈1.333）→ 已按插件改写 + `hitChance()` 可测 + 测试钉死。
状态检查也按 `Battler_UseMove_SuccessChecks:269-364` 重排（睡眠→冰冻→畏缩→混乱→麻痹）并补上全部文案：
`依旧在沉睡。`/`醒来了！`（`:447`/`:473`）、`被结实的冰冻着！`/`不再被冰冻了！`（`:456`/`:477`）、
`畏缩了，无法行动！`（`:323`）、`混乱了！`/`解除了混乱！`（`:338`/`:335`）、`麻痹了！\n无法行动！`（`:454`）；
`Battler_UseMove_SuccessChecks:318-319` 的 Boss 抗畏缩叫声走新增的 `Battle.CryPlayer` 钩子
（`RuntimeContext` 用 `pbCryFile`+`pbSEPlay(...,100,100)` 接线，无音频环境为 null）。

**证据**：core **553 tests / 0 failed**（新增 `BattleDamageAnimationTest` 4 条 + `BattleRoundMessagesTest` 5 条）；
探针 `damage` 模式的 4 组闪烁像素统计（Lead 独立复现，见 §20.8 ⑥）。

**已登记待做**：B8「回合事件有序化」——插件顺序是
`使用:290 → 扣血:716 → 受击闪烁:719 → 会心/效果播报:728 → 倒下了 BCS:71`，
运行时目前是三条并行列表（`roundMessages`/`hitEvents`/`faintEvents`）按"轮"聚合，
因此播报落在闪烁之前、且双方出手的闪烁被合并成一次。修法：合并成一条有序
`RoundEvent{kind: MESSAGE|HIT|FAINT}` 事件流，并把多段招式拆成每段一个 HIT（`oldHp` 用该段起始 HP）。

## 20.10 B8-B：kimi27 查漏清单里"可独立转译"的前两条（Lead 已落地）

按 §20.9 的 27 条清单，先做掉两条**纯引擎、收益大**的：

| 项 | 插件依据 | 处理 |
|---|---|---|
| 命中/闪避公式错 | `Move_Usage_Calculations:127-142` | **已重写**：两个 stage 各自换算成百分比（`stageMul`/`stageDiv`，索引 = stage+6）再相除，不再是"合并成一个 stage"。两者不等价（命中+2/闪避+1：插件 167/133≈1.256 vs 旧实现 4/3≈1.333）。`hitChance()` 单独可测 |
| 状态导致无法行动时**没有任何提示** | `Battler_UseMove_SuccessChecks:269-364` + `Battler_Statuses:443-478` | **已逐行转译**：顺序改为 睡眠→冰冻→畏缩→混乱→麻痹；补上 `依旧在沉睡。`/`醒来了！`、`被结实的冰冻着！`/`不再被冰冻了！`、`畏缩了，无法行动！`、`混乱了！`/`解除了混乱！`、`麻痹了！\n无法行动！`；`:318-319` 的 Boss 抗畏缩叫声走新增 `Battle.CryPlayer` 钩子（`RuntimeContext` 用 `pbCryFile`+`pbSEPlay(100,100)` 接线） |
| 能力升降**没有任何提示** | `Battler_StatStages:58-62`（升）/`:224-228`（降） | **已转译**：`{1}的{2}提升了！`/`大幅提升了！`/`巨幅提升了！`（`[increment-1,2].min`）与对应的"降低了"三档；名字照抄 `PBStats:18-30`（`血量/攻击/防御/速度/特攻/特防/命中/回避`）。`:55/:221` 的 `increment<=0` 静默返回也已照抄（"不能再提高了！"只属于 `showFailMsg` 调用方，本运行时无该调用方） |
| 出招前的连击/会心/效果播报 | `Move_Usage:329-345`、`Battler_UseMove:495-506` | **已转译**（见 §20.9） |

**性能（qwen38 task-4 的一条建议由 Lead 落地）**：`Battler.moveSlot(i)` 现在复用不可变的 `BattleMove` 包装
（按槽位缓存 + `entry.move` 身份比对失效），战斗菜单每帧的 4~6 次小对象分配消失。

**证据**：core **554 tests / 0 failed**（新增 `BattleRoundMessagesTest` 6 条：命中公式的三个 stage 组合、
类型免疫文案、未命中文案、睡眠/清醒文案、畏缩文案、能力升降文案与 +6 静默）。

## 20.11 B8-A：野生宝可梦 AI 出招（kimi27，task-5，已验收接线）

原来 `Battle.defaultAi` 是运行时自造的"确定性取最大 `威力×相性×STAB`、跳过变化招式"，
插件野生战斗走的是**按分加权随机**：

| 插件位置 | 运行时实现 |
|---|---|
| `PokeBattle_AI:83` `def pbAIRandom(x); return rand(x); end` | `BattleAi.aiRandom`（`0...x` **不含 x**；`x==0` 时 Ruby 返回 Float，Java 用 `nextDouble()` 而不是 `nextInt(0)`——后者会抛异常） |
| `AI_Move:8` `wildBattler = wildBattle? && opposes?` | `BattleAi.isWildBattler`（`Battle.wildBattle()` + `(index&1)!=0`） |
| `AI_Move:17-19/27` choices 构建与 `totalScore` | `BattleAi.chooseMove` 的循环（`Battle.canChooseMove` 拒绝的不入列） |
| `AI_Move:153-155` `pbRegisterMoveWild` | 每个可用招式 100 分 → 等价于"可用招式等概率" |
| `AI_Move:106-122` 无 choice 的重扫 / `pbAutoChooseMove` | 返回 `STRUGGLE`（`-1`），由 `Battle.pickMove` 换成 Struggle |
| `AI_Move:124-132` 加权随机 | 同结构（`randNum -= c[1]`，第一个 `<0`），返回槽位索引 |

**接线**：`Battle.pickMove` 在 `BattleAi.handles(...)` 为真时走它；**训练家战与 `battleRank>=2` 的野生 BOSS
仍走 `defaultAi` 兜底**（那两条路要 `pbRegisterMoveTrainer`，尚未转译）。

**未转（已上报）**：
1. `AI_Move:92-105`：唯一效果是写 `$nextTarget/$nextMove/$nextQue` 三个全局变量，而全库 12 处命中
   **全是写入/初始化、无任何读取点**（`PokeBattle_AI:46-48/139-141`、`AI_Move:58-60/100-102`）；
   字面转译需先转 `pbMoveBaseDamage`(`AI_Move_Utilities:171-264`)+`pbRoughDamage`(`:266-579`)+`pbRoughStat`(`:152-169`)，约 400 行只为写死变量 → 不转。
2. `AI_Move:133-145`：`PBDebug.log` + 代欧奇希斯形态；`PBEffects::DeoxysForm` 在插件里是活的
   （`Battler_UseMove:404-408` 读它改形态、`Scene_Commands:152-171` T 键写它），本运行时两者皆无 → **依赖未建模子系统**。

**训练家 AI 的工作量评估（kimi27 清单）**：完整转译 ≈ **5,065 行**，其中
`AI_Move_EffectScores:5-3866`（3,862 行）与 `AI_Move_Utilities:266-579`（314 行）**与特性/道具/PBEffects/天气
不可分割**（前者 `ability×132 / item×28 / PBEffects×179`）。因此**不建议先做训练家路径**：只做骨架会让
`pbGetMoveScore` 退化成"只有基础分"，属于明确降级。前置条件 = 特性子系统 + 战斗内道具触发 + PBEffects 表
（即查漏清单 #13/#14/#19/#20）。

**证据**：core **563 tests / 0 failed**（`BattleAiTest` 9 条：4 招 4000 轮全覆盖且各 1000±200、
0PP 永不被选、全 0PP 恒 Struggle、单候选恒选、`aiRandom` 的排他边界 20000 次、训练家/BOSS 返回
`NOT_HANDLED`、空槽跳过、以及 Lead 补的"接线有效性"——60 轮 × 3 回合里野生对手用到的招式 >1 种）。

**零行为回归（qwen38，接线前 `ai-base-a` 451 张 vs 接线后 `ai-after-a` 449 张，62 处差异）**：
- **非战斗帧零违规**：37 处非战斗差异 **100%** 落在接线前就存在的 4 类随机源
  （暂停菜单/存档的真实日期时间、标题屏 `System.currentTimeMillis()` 台词轮播、`BerryPlants.now()` 浆果闪光、
  敌方性别 RNG），每类都能在"同构建两次运行"里复现；
- 战斗 24 处全部在"对手出手之后"（招式不同 → 精灵/血条/文案不同），1 处在出手前但是已知的性别图标 RNG；
- 日志结构：`wild/trainer/switch/trainer2/fight-status/fight-0pp` **同形**，7/7 模式无卡死、无预算帧数 cap 命中、无崩溃；
- 性能：7 个模式的 `drawOrder` 调用数与接线前**逐一相同**，calls/frame 持平或下降；
- `:core:test --rerun-tasks` → **102 类 / 563 tests / 0 failed**。
- 附带结论：野生对手的招式选择**真的随机了** → `faint` 探针从"必定进入倒下分支"变成"约 2/3 概率进入"
  （Lead 实测 3 次：2 次进入 `CONFIRM_OWN@0`），已让 qwen38 把该探针的测试装置改成确定性（主角 HP=1 +
  对手只留伤害招式），并在注释里标明那是探针装置而非游戏逻辑。








## 20.12 B8：回合事件有序化（本轮，Lead，已写回工作树，待用户验收）

**做法**：`Battle.roundEvents` —— 一条有序 `RoundEvent{MESSAGE(brief)|HIT|HP_CHANGE|FAINT|BGM}`，替代
`hitEvents`/`faintEvents`（已删除）；`roundMessages`/`endOfRoundMessages` 保留为纯文本镜像（测试与文本调用方用）。
`InteractiveBattlePort.Session.takeEvents()` 交给画面：port 自己的行 → 引擎事件（招式 + 回合末）→ 收尾行；
`BattleScreen` 按序逐个播放。

| 插件位置 | 运行时 |
|---|---|
| `Battler_UseMove:305` → `Move_Usage:24` brief「使用」 | `Battle.pbDisplayBrief` |
| `Battler_UseMove:458-482` 每段一次 `pbProcessMoveHit` | `execute` 的 per-hit 循环（会心 `Move_Usage_Calculations:263` 改为每段掷一次） |
| `:716/:719` → `Move_Usage:264-292` 先己方后对方、`oldHP=hp+hpLost`、`:286-291` 低 HP BGM | `pbAnimateHitAndHPLost`（HIT 带 newHp）；BGM 事件 |
| `:728` → `Move_Usage:322-345/297-320` | `pbHitEffectivenessMessages` / `pbEffectivenessMessage`（连击招式只在 `:495-501` 报效果） |
| `:723/:753/:806-807/:541-542` 倒下检查 | `pbFaint`（`Battler_ChangeSelf:66-77`；`:70` `@fainted` = 新增 `Battler.faintedFlag`，由 `Battler_Initialize:157` 维护） |
| `:502-506` 击中 N 次（`realNumHits`） | 同 |
| `:539/:553` 反作用/吸取、经验 | 位置按插件挪到循环之后 |
| `Battler_UseMove:130-145` `pbConfusionDamage`：扣血→闪→文案→倒下 | `canAct` 混乱分支（旧实现文案在扣血前、且不检查倒下） |
| `Battle_Phase_EndOfRound:417-420/433-436` + `Battler_Statuses:462-464` + `Battler_ChangeSelf:14` | 中毒/灼伤：HP_CHANGE → 文案 → 倒下（旧实现无血条事件、倒下动画排在全部文案之后） |
| `PokeBattle_Scene:101-154` brief 留屏 / `pbWaitMessage` 40 帧；`Scene_Animations:212/240/300` 置 `@briefMessage=false` | `BattleScreen.briefMessage/messageReleased/waitMessageFrames` |
| `Scene_Animations:239-268/211-222/299-312` | HIT / HP_CHANGE / FAINT 各自阻塞，结束后 `resumeRound()` |
| `Game_System:65-72`（`pbBGMPlay` → `bgm_play_internal`：`:69` 文件存在才播，否则**当前 BGM 继续**） | `AudioManager.bgmExists` + `playBattleBgm`；`Battle low HP` 本工程不存在 → 战斗曲继续 |

**顺带修掉（同一流程内的旧偏差）**：血条在「使用」时就开始动（现按事件 `heldHp` 播放）；击倒那一击的数据盒提前消失
（现保留到 `DataBoxDisappearAnimation`）；回合末中毒倒下时「所有的宝可梦都倒下了……」排在中毒文案之前；
回合最后一行 engine 文案按 pbDisplay 40 帧自动关闭（之前被当成 paused 等按键；port 自己的收尾行仍 paused）；
HIT/FAINT 期间消息框条按 `PokeBattle_Scene:89` 保持显示。

**未转（已登记）**：`Battler_UseMove:688` 招式动画、`Battler_UseMove_SuccessChecks:337` / `Battler_Statuses:462`
公共动画（动画播放器）；`Battler_ChangeSelf:62-65`（Boss 捕获）、`:79-98`（亲密度/形态/LastRoundFainted/特性/原始天气/倒下计数）、
`:106-126`（Commander）；`Move_Usage:323-328/341-344`（画皮/冰脸/替身，需特性与 PBEffects）；反作用/吸取自己的文案与血条（招式效果批次）。

**仍存在的已知偏差（需用户决定是否单开一批）**：经验结算仍是回合后的独立阶段；插件 `pbGainExp` 在 `Battler_UseMove:553`（招式内，
早于回合末事件），训练家战里"击倒 + 我方中毒"时经验窗会排在中毒文案之后。

**证据**：core 571 tests（+8 `BattleRoundEventsTest`），云端 526 通过，余下 2 failed/43 aborted 与改动前基线完全相同
（均因云端无 `X:\generated`）；`BattleRoundEventsTest` 钉死：双方各自一次闪烁、闪烁→效果文案、KO 只 FAINT 一次、
连击每段一次闪烁且 HP 首尾相接、中毒 HP→文案→倒下、低 HP BGM 只对我方、混乱自伤顺序、port 事件流。
lwjgl3 探针已对新 core 编译通过（只读 `debugStage()`，"HIT" 阶段名保留）。**定点截图需在用户机器跑探针**（`damage`/`faint`/`fight-status` 模式）。
