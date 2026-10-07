# Stage 5 战斗引擎重做 —— 步骤 1：必须保持不变的签名清单（待确认）

> 生成方式：脚本扫描 `Battler.java` / `Battle.java` 的顶层非 private 成员，再统计**其它文件**对 `.成员名` 的引用次数，分三类调用方：
> - **转译层** = battle 包内你已逐行转译的文件：`movefx/**`、`Abilities*`、`Items*`、`BattleHandlers*`、`HandlerArceusOverrides`、`BattleSuccessChecks`、`BattlerHitEffects`、`DamageCalc`、`BattleEndOfRound`、`BattleAftermath`、`PendingApi`、`MoveFxPendingApi` 等
> - **场景层** = `BattleScreen`、`*BattlePort`、`BattleSendOut`、`BattleEntry*`、`BattleAi`、`MapScreen`、battle 包外文件
> - **测试**
> - 按 `.名字` 文本匹配（不做类型解析），同名的 Battle/Battler/Move/内部类成员会互相蹭计数（例：`name`/`move`/`level`）。计数是**下限参考**，不是精确依赖图。
> - 数字列顺序：转译层 / 场景层 / 测试。

## Battler.java

### A. 转译层依赖 —— 冻结（170 个名字）

| 成员 | 签名（全部重载） | 转译/场景/测试 |
|---|---|---|
| `battle` | `Battle battle` | 1032/86/58 |
| `effects` | `EffectMap effects` | 885/0/70 |
| `pbThis` | `String pbThis()`<br>`String pbThis(boolean lowerCase)` | 723/0/5 |
| `Battler` | `Battler(Pokemon pokemon, boolean foe)` | 615/0/68 |
| `index` | `int index` | 236/129/54 |
| `damageState` | `DamageState damageState` | 198/0/4 |
| `item` | `String item` | 161/101/50 |
| `abilityName` | `String abilityName()` | 138/0/2 |
| `pbOwnSide` | `BattleSide pbOwnSide()` | 138/0/0 |
| `pbCanRaiseStatStage` | `boolean pbCanRaiseStatStage(int stat)`<br>`boolean pbCanRaiseStatStage(int stat, Battler user)`<br>`boolean pbCanRaiseStatStage(int stat, Battler user, BattleMove move, boolean showFailMsg)`<br>`boolean pbCanRaiseStatStage(int stat, Battler user, BattleMove move, boolean showFailMsg, boolean ignoreContrary)`<br>`boolean pbCanRaiseStatStage(int stat, Battler user, BattleMove move)` | 134/0/2 |
| `field` | `BattleField field` | 129/3/24 |
| `maxHp` | `int maxHp()` | 124/63/34 |
| `hasActiveAbility` | `boolean hasActiveAbility(String abil)`<br>`boolean hasActiveAbility(String abil, boolean ignoreFainted)`<br>`boolean hasActiveAbility(String[] abils)`<br>`boolean hasActiveAbility(String[] abils, boolean ignoreFainted)` | 109/0/8 |
| `fainted` | `boolean fainted()` | 108/18/1 |
| `hp` | `int hp` | 92/75/100 |
| `pokemon` | `Pokemon pokemon` | 89/182/94 |
| `pbs` | `pokemon.runtime.pokemon.PbsData pbs` | 83/16/4 |
| `ability` | `String ability` | 81/35/37 |
| `name` | `String name()` | 80/290/105 |
| `isSpecies` | `boolean isSpecies(String species)` | 80/0/3 |
| `pbRaiseStatStage` | `boolean pbRaiseStatStage(int stat, int increment, Battler user)`<br>`boolean pbRaiseStatStage(int stat, int increment, Battler user, boolean showAnim, boolean ignoreContrary)`<br>`boolean pbRaiseStatStage(int stat, int increment, Battler user, boolean showAnim)` | 70/0/0 |
| `stage` | `int stage(int pbStat)` | 64/5/20 |
| `status` | `String status` | 51/39/40 |
| `opposes` | `boolean opposes(int i)`<br>`boolean opposes(Battler other)` | 48/2/6 |
| `itemName` | `String itemName()` | 47/0/4 |
| `pbCanLowerStatStage` | `boolean pbCanLowerStatStage(int stat)`<br>`boolean pbCanLowerStatStage(int stat, Battler user)`<br>`boolean pbCanLowerStatStage(int stat, Battler user, BattleMove move, boolean showFailMsg, boolean ignoreContrary)`<br>`boolean pbCanLowerStatStage(int stat, Battler user, BattleMove move)`<br>`boolean pbCanLowerStatStage(int stat, Battler user, BattleMove move, boolean showFailMsg)` | 47/0/1 |
| `hasActiveItem` | `boolean hasActiveItem(String itm)`<br>`boolean hasActiveItem(String itm, boolean ignoreFainted)`<br>`boolean hasActiveItem(String[] items)`<br>`boolean hasActiveItem(String[] items, boolean ignoreFainted)` | 46/0/4 |
| `pbReduceHP` | `int pbReduceHP(int amount)`<br>`int pbReduceHP(int amount, boolean anim, boolean registerDamage, boolean anyAnim)` | 46/0/4 |
| `pbHasType` | `boolean pbHasType(String type)` | 45/0/3 |
| `pbOpposingSide` | `BattleSide pbOpposingSide()` | 44/0/0 |
| `pbRecoverHP` | `int pbRecoverHP(int amount)`<br>`int pbRecoverHP(int amount, boolean anim, boolean anyAnim)` | 39/0/1 |
| `pbCureStatus` | `void pbCureStatus()`<br>`void pbCureStatus(boolean showMessages)` | 37/0/0 |
| `pbItemHPHealCheck` | `void pbItemHPHealCheck(int forcedItem, boolean fling)`<br>`void pbItemHPHealCheck(String forcedItemName, boolean fling)` | 35/0/0 |
| `pbRaiseStatStageByAbility` | `boolean pbRaiseStatStageByAbility(int stat, int increment, Battler user, boolean splashAnim)` | 33/0/0 |
| `pbLowerStatStage` | `boolean pbLowerStatStage(int stat, int increment, Battler user)`<br>`boolean pbLowerStatStage(int stat, int increment, Battler user, boolean showAnim, boolean ignoreContrary, boolean ignoreMirrorArmor)`<br>`boolean pbLowerStatStage(int stat, int increment, Battler user, boolean showAnim)`<br>`boolean pbLowerStatStage(int stat, int increment, Battler user, boolean showAnim, boolean ignoreContrary)` | 33/0/0 |
| `form` | `int form()` | 32/58/8 |
| `lastRegularMoveUsed` | `String lastRegularMoveUsed` | 32/0/0 |
| `canHeal` | `boolean canHeal()` | 31/0/3 |
| `pbTeam` | `String pbTeam(boolean lowerCase)` | 29/0/2 |
| `hasUtilityUmbrella` | `boolean hasUtilityUmbrella()` | 26/0/2 |
| `hasStatus` | `boolean hasStatus(String id)` | 25/0/0 |
| `pbRaiseStatStageByCause` | `boolean pbRaiseStatStageByCause(int stat, int increment, Battler user, String cause)`<br>`boolean pbRaiseStatStageByCause(int stat, int increment, Battler user, String cause, boolean showAnim, boolean ignoreContrary)`<br>`boolean pbRaiseStatStageByCause(int stat, int increment, Battler user, String cause, boolean showAnim)` | 21/0/0 |
| `types` | `Array<String> types()` | 20/20/12 |
| `pbPoison` | `void pbPoison(Battler user, String msg, boolean toxic)` | 20/0/0 |
| `pbBurn` | `void pbBurn(Battler user, String msg)` | 20/0/0 |
| `initialItem` | `String initialItem()`<br>`String initialItem` | 18/0/0 |
| `affectedByContactEffect` | `boolean affectedByContactEffect(boolean showMsg)` | 18/0/0 |
| `pbCanPoison` | `boolean pbCanPoison(Battler user, boolean showMessages, BattleMove move)`<br>`boolean pbCanPoison(Battler user, boolean showMessages)` | 18/0/0 |
| `pbParalyze` | `void pbParalyze(Battler user, String msg)` | 18/0/0 |
| `setStage` | `void setStage(int pbStat, int value)` | 17/0/19 |
| `takesIndirectDamage` | `boolean takesIndirectDamage(boolean showMsg)` | 17/0/0 |
| `pbCanBurn` | `boolean pbCanBurn(Battler user, boolean showMessages, BattleMove move)`<br>`boolean pbCanBurn(Battler user, boolean showMessages)` | 17/0/0 |
| `level` | `int level()` | 16/67/24 |
| `pbOpposingTeam` | `String pbOpposingTeam(boolean lowerCase)` | 16/0/2 |
| `unlosableItem` | `boolean unlosableItem(String checkItem)` | 16/0/0 |
| `attack` | `int attack()`<br>`int attack(boolean critical)` | 15/2/6 |
| `allAllies` | `Array<Battler> allAllies()` | 15/0/0 |
| `abilityActive` | `boolean abilityActive()`<br>`boolean abilityActive(boolean ignoreFainted)` | 15/0/4 |
| `isUnnerved` | `boolean isUnnerved()` | 15/0/3 |
| `pbHeldItemTriggerCheck` | `void pbHeldItemTriggerCheck(int forcedItem, boolean fling)`<br>`void pbHeldItemTriggerCheck(String forcedItemName, boolean fling)` | 15/0/0 |
| `setInitialItem` | `void setInitialItem(String newItem)` | 14/0/0 |
| `pbCanParalyze` | `boolean pbCanParalyze(Battler user, boolean showMessages, BattleMove move)`<br>`boolean pbCanParalyze(Battler user, boolean showMessages)` | 14/0/0 |
| `airborne` | `boolean airborne()` | 14/0/7 |
| `pbConsumeItem` | `void pbConsumeItem()`<br>`void pbConsumeItem(boolean recoverable, boolean symbiosis, boolean belch)` | 13/0/0 |
| `pbChangeFormTransform` | `void pbChangeFormTransform(int newForm, String msg)` | 13/0/0 |
| `gender` | `int gender()` | 12/49/17 |
| `itemActive` | `boolean itemActive()`<br>`boolean itemActive(boolean ignoreFainted)` | 12/0/2 |
| `pbOnAbilityChanged` | `void pbOnAbilityChanged(String oldAbil)` | 12/0/0 |
| `pbCanConfuse` | `boolean pbCanConfuse(Battler user, boolean showMessages, BattleMove move, boolean selfInflicted)`<br>`boolean pbCanConfuse(Battler user)`<br>`boolean pbCanConfuse(Battler user, boolean showMessages)`<br>`boolean pbCanConfuse(Battler user, boolean showMessages, BattleMove move)`<br>`boolean pbCanConfuse()` | 12/0/0 |
| `movedThisRound` | `boolean movedThisRound()` | 12/0/0 |
| `pbHeldItemTriggered` | `void pbHeldItemTriggered(String thisItem, int forcedItem, boolean fling)` | 11/0/0 |
| `pbTypes` | `Array<String> pbTypes()`<br>`Array<String> pbTypes(boolean withType3)` | 11/0/6 |
| `defense` | `int defense()`<br>`int defense(boolean critical)` | 10/2/1 |
| `idxOpposingSide` | `int idxOpposingSide()` | 10/0/2 |
| `unstoppableAbility` | `boolean unstoppableAbility(String abil)` | 10/0/3 |
| `pbConfuse` | `void pbConfuse()`<br>`void pbConfuse(String msg)` | 10/0/0 |
| `spAtk` | `int spAtk()`<br>`int spAtk(boolean critical)` | 9/2/1 |
| `recycleItem` | `String recycleItem()`<br>`String recycleItem` | 9/0/0 |
| `pbFaint` | `void pbFaint()`<br>`void pbFaint(boolean showMessage)` | 9/0/0 |
| `pbHasAnyStatus` | `boolean pbHasAnyStatus()` | 9/0/2 |
| `asleep` | `boolean asleep()` | 9/0/1 |
| `turnCount` | `int turnCount` | 9/5/11 |
| `statsRaisedThisRound` | `boolean statsRaisedThisRound` | 9/0/2 |
| `pbOwnedByPlayer` | `boolean pbOwnedByPlayer()` | 8/0/2 |
| `pbRemoveItem` | `void pbRemoveItem(boolean permanent)` | 8/0/0 |
| `pbCanSleep` | `boolean pbCanSleep(Battler user, boolean showMessages, BattleMove move, boolean ignoreStatus)`<br>`boolean pbCanSleep(Battler user, boolean showMessages)`<br>`boolean pbCanSleep(Battler user, boolean showMessages, BattleMove move)` | 8/0/0 |
| `semiInvulnerable` | `boolean semiInvulnerable()` | 8/0/0 |
| `statsDropped` | `boolean statsDropped` | 8/0/2 |
| `statsLoweredThisRound` | `boolean statsLoweredThisRound` | 8/0/2 |
| `speed` | `int speed()` | 7/5/6 |
| `idxOwnSide` | `int idxOwnSide()` | 7/0/2 |
| `eachAlly` | `void eachAlly(java.util.function.Consumer<Battler> action)` | 7/0/0 |
| `pbItemStatusCureCheck` | `void pbItemStatusCureCheck(int forcedItem, boolean fling)`<br>`void pbItemStatusCureCheck(String forcedItemName, boolean fling)` | 7/0/0 |
| `poisoned` | `boolean poisoned()` | 7/0/2 |
| `pbHasOtherType` | `boolean pbHasOtherType(String type)` | 7/0/2 |
| `spDef` | `int spDef()`<br>`int spDef(boolean critical)` | 6/2/1 |
| `moveSlots` | `Array<BattleMove> moveSlots()` | 6/0/0 |
| `pbAbilitiesOnDamageTaken` | `boolean pbAbilitiesOnDamageTaken(int oldHP, int newHP)` | 6/0/0 |
| `burned` | `boolean burned()` | 6/0/1 |
| `pbSleep` | `void pbSleep()`<br>`void pbSleep(String msg)` | 6/0/0 |
| `pbFreeze` | `void pbFreeze()`<br>`void pbFreeze(String msg)` | 6/0/0 |
| `pbFlinch` | `void pbFlinch()`<br>`void pbFlinch(Battler user)` | 6/0/0 |
| `near` | `boolean near(int i)`<br>`boolean near(Battler other)` | 5/0/2 |
| `ungainableAbility` | `boolean ungainableAbility(String abil)` | 5/0/3 |
| `pbCanConsumeBerry` | `boolean pbCanConsumeBerry(String berryItem, boolean alwaysCheckGluttony)` | 5/0/5 |
| `pbLowerStatStageByAbility` | `boolean pbLowerStatStageByAbility(int stat, int increment, Battler user, boolean splashAnim, boolean checkContact)` | 5/0/0 |
| `hasAlteredStatStages` | `boolean hasAlteredStatStages()` | 5/0/1 |
| `affectedByTerrain` | `boolean affectedByTerrain()` | 5/0/0 |
| `effectiveWeather` | `int effectiveWeather()` | 5/0/0 |
| `pbCanFreeze` | `boolean pbCanFreeze(Battler user, boolean showMessages)`<br>`boolean pbCanFreeze(Battler user, boolean showMessages, BattleMove move)` | 5/0/0 |
| `lastHPLost` | `int lastHPLost` | 5/0/0 |
| `statused` | `boolean statused()` | 4/0/0 |
| `thisName` | `String thisName()` | 4/0/1 |
| `moveSlotPp` | `int moveSlotPp(int slot)` | 4/1/1 |
| `pbDirectOpposing` | `Battler pbDirectOpposing(boolean unfaintedOnly)` | 4/0/2 |
| `eachOpposing` | `void eachOpposing(java.util.function.Consumer<Battler> action)` | 4/0/0 |
| `setRecycleItem` | `void setRecycleItem(String newItem)` | 4/0/0 |
| `paralyzed` | `boolean paralyzed()` | 4/0/1 |
| `pbCureConfusion` | `void pbCureConfusion()` | 4/0/1 |
| `pbCanAttract` | `boolean pbCanAttract(Battler user, boolean showMessages)` | 4/0/0 |
| `pbAttract` | `void pbAttract(Battler user, String msg)` | 4/0/0 |
| `statStageAtMax` | `boolean statStageAtMax(int stat)` | 4/0/2 |
| `statStageAtMin` | `boolean statStageAtMin(int stat)` | 4/0/1 |
| `pbResetStatStages` | `void pbResetStatStages()` | 4/0/1 |
| `pokemonIndex` | `int pokemonIndex` | 4/0/1 |
| `initialHP` | `int initialHP` | 4/0/4 |
| `moveSlot` | `BattleMove moveSlot(int slot)` | 3/6/6 |
| `pbSetPP` | `boolean pbSetPP(String internalName, int pp)` | 3/0/0 |
| `MOVES_MAX` | `static int MOVES_MAX` | 3/4/5 |
| `statusCount` | `int statusCount` | 3/0/2 |
| `lastHPLostFromFoe` | `int lastHPLostFromFoe` | 3/0/0 |
| `tookPhysicalHit` | `boolean tookPhysicalHit` | 3/0/0 |
| `lastMoveUsedType` | `String lastMoveUsedType` | 3/0/0 |
| `lastRoundMoved` | `int lastRoundMoved` | 3/0/1 |
| `hasType` | `boolean hasType(String type)` | 2/0/0 |
| `moveSlotMaxPp` | `int moveSlotMaxPp(int slot)` | 2/1/1 |
| `uncopyableAbility` | `boolean uncopyableAbility(String abil)` | 2/0/2 |
| `pbItemStatRestoreCheck` | `void pbItemStatRestoreCheck(int forcedItem, boolean fling)` | 2/0/0 |
| `pbCanInflictStatus` | `boolean pbCanInflictStatus(int newStatus, Battler user, boolean showMessages, BattleMove move, boolean ignoreStatus)` | 2/0/7 |
| `pbCanPoisonSynchronize` | `boolean pbCanPoisonSynchronize(Battler target)` | 2/0/0 |
| `pbCanBurnSynchronize` | `boolean pbCanBurnSynchronize(Battler target)` | 2/0/0 |
| `pbCanParalyzeSynchronize` | `boolean pbCanParalyzeSynchronize(Battler target)` | 2/0/0 |
| `pbCureAttract` | `void pbCureAttract()` | 2/0/1 |
| `plainStats` | `int[] plainStats()` | 2/0/1 |
| `foe` | `boolean foe` | 2/11/54 |
| `mirrorHerbUsed` | `boolean mirrorHerbUsed` | 2/0/2 |
| `lastFoeAttacker` | `Array<Battler> lastFoeAttacker` | 2/0/0 |
| `cureStatus` | `void cureStatus()` | 1/0/0 |
| `pbSymbiosis` | `void pbSymbiosis()` | 1/0/0 |
| `pbItemEndOfMoveCheck` | `void pbItemEndOfMoveCheck(int forcedItem, boolean fling)`<br>`void pbItemEndOfMoveCheck(String forcedItemName, boolean fling)` | 1/0/0 |
| `pbItemTerrainStatBoostCheck` | `void pbItemTerrainStatBoostCheck()` | 1/0/0 |
| `pbItemOnIntimidatedCheck` | `void pbItemOnIntimidatedCheck()` | 1/0/0 |
| `pbAbilityOnTerrainChange` | `void pbAbilityOnTerrainChange(boolean abilityChanged)` | 1/0/0 |
| `pbAbilitiesOnSwitchOut` | `void pbAbilitiesOnSwitchOut()` | 1/0/0 |
| `affectedByPowder` | `boolean affectedByPowder(boolean showMsg)` | 1/0/0 |
| `pbInflictStatus` | `void pbInflictStatus(int newStatus, int newStatusCount, String msg, Battler user)` | 1/0/0 |
| `pbCheckFormOnStatusChange` | `void pbCheckFormOnStatusChange()` | 1/0/0 |
| `pbCheckFormOnWeatherChange` | `void pbCheckFormOnWeatherChange()` | 1/0/0 |
| `pbLowerStatStageByCause` | `boolean pbLowerStatStageByCause(int stat, int increment, Battler user, String cause)`<br>`boolean pbLowerStatStageByCause(int stat, int increment, Battler user, String cause, boolean showAnim, boolean ignoreContrary, boolean ignoreMirrorArmor)`<br>`boolean pbLowerStatStageByCause(int stat, int increment, Battler user, String cause, boolean showAnim)`<br>`boolean pbLowerStatStageByCause(int stat, int increment, Battler user, String cause, boolean showAnim, boolean ignoreContrary)` | 1/0/0 |
| `toxic` | `int toxic` | 1/1/5 |
| `confusion` | `int confusion` | 1/0/7 |
| `trainerBattle` | `boolean trainerBattle` | 1/11/16 |
| `droppedBelowHalfHP` | `boolean droppedBelowHalfHP` | 1/0/1 |
| `dummy` | `boolean dummy` | 1/0/1 |
| `lastAttacker` | `Array<Battler> lastAttacker` | 1/0/1 |
| `tookDamage` | `boolean tookDamage` | 1/0/2 |
| `lastMoveUsed` | `String lastMoveUsed` | 1/0/1 |
| `lastMoveFailed` | `boolean lastMoveFailed` | 1/0/1 |
| `lastRoundMoveFailed` | `boolean lastRoundMoveFailed` | 1/0/1 |
| `movesUsed` | `Array<String> movesUsed` | 1/0/1 |

### B. 仅场景层依赖 —— 事件流/UI 契约，冻结（4 个）

- `boolean hasUsableMove()` (2/1)
- `void syncHp()` (1/4)
- `boolean isMega()` (1/7)
- `boolean isPrimal()` (1/0)

### C. 仅测试依赖 —— 逐个判断改/删（16 个）

- `void setStatus(String id)` (测试 2)
- `BattleMove struggle(pokemon.runtime.pokemon.PbsData pbs)` (测试 4)
- `void initEffects(boolean batonPass)` (测试 7)
- `boolean activeAbilityShield()` (测试 3)
- `boolean pbHasStatus(int checkStatus)` (测试 2)
- `boolean frozen()` (测试 1)
- `void setStatusCount(int value)` (测试 2)
- `int pbRaiseStatStageBasic(int stat, int increment, boolean ignoreContrary)` (测试 4)
- `int pbLowerStatStageBasic(int stat, int increment, boolean ignoreContrary)` (测试 4)
- `int[] stages` (测试 13)
- `int[] hitStages` (测试 8)
- `int sleepTurns` (测试 3)
- `boolean flinched` (测试 1)
- `boolean focusEnergy` (测试 1)
- `boolean faintedFlag` (测试 1)
- `int lastRegularMoveTarget` (测试 1)

### D. 无外部引用（20 个，可自由改）

`canPoison`、`canBurn`、`canParalyze`、`canFreeze`、`canSleep`、`stageMultiplier`、`accuracyStage`、`evasionStage`、`hitStageMultiplier`、`resetForSwitchIn`、`canFight`、`hasMega`、`hasPrimal`、`pbAbilityStatusCureCheck`、`pbAbilitiesOnFainting`、`pbCanSynchronizeStatus`、`pbMirrorStatUpsOpposing`、`pbConfusionDuration`、`pbSleepDuration`、`yamaskhp`

## Battle.java

### A. 转译层依赖 —— 冻结（52 个名字）

| 成员 | 签名（全部重载） | 转译/场景/测试 |
|---|---|---|
| `display` | `void display(String msg)` | 949/1/2 |
| `showAbilitySplash` | `void showAbilitySplash(Battler battler)` | 192/0/1 |
| `hideAbilitySplash` | `void hideAbilitySplash(Battler battler)` | 180/0/1 |
| `field` | `BattleField field` | 129/3/24 |
| `pbs` | `PbsData pbs()` | 83/16/4 |
| `NEWEST_BATTLE_MECHANICS` | `static boolean NEWEST_BATTLE_MECHANICS` | 81/0/0 |
| `terrain` | `int terrain()` | 69/0/3 |
| `commonAnimation` | `void commonAnimation(String name, Battler user)`<br>`void commonAnimation(String name, Battler user, Array<Battler> targets)` | 65/0/5 |
| `pbRandom` | `int pbRandom(int x)` | 63/0/3 |
| `moldBreaker` | `boolean moldBreaker` | 51/0/1 |
| `pbWeather` | `int pbWeather()` | 48/0/3 |
| `wildBattle` | `boolean wildBattle()` | 21/5/5 |
| `choices` | `Object[] choices(int idxBattler)` | 20/4/8 |
| `eachBattler` | `Array<Battler> eachBattler()` | 18/0/1 |
| `weather` | `int weather()` | 17/0/14 |
| `battlerAt` | `Battler battlerAt(int idxBattler)` | 14/4/0 |
| `pbAllFainted` | `boolean pbAllFainted(int idxBattler)` | 13/0/4 |
| `replaceAbilitySplash` | `void replaceAbilitySplash(Battler battler)` | 12/0/1 |
| `pbClearChoice` | `void pbClearChoice(int idxBattler)` | 11/0/1 |
| `Battle` | `Battle(PbsData pbs, Random random, Controller playerController)` | 10/0/0 |
| `partyOf` | `Array<Battler> partyOf(int idxBattler)` | 10/2/0 |
| `pbCanChooseNonActive` | `boolean pbCanChooseNonActive(int idxBattler)` | 10/0/2 |
| `pbGetReplacementPokemonIndex` | `int pbGetReplacementPokemonIndex(int idxBattler)`<br>`int pbGetReplacementPokemonIndex(int idxBattler, boolean random)` | 10/0/3 |
| `turnCount` | `int turnCount()` | 9/5/11 |
| `eachOtherSideBattler` | `Array<Battler> eachOtherSideBattler(int idxBattler)` | 9/0/3 |
| `pbRecallAndReplace` | `void pbRecallAndReplace(int idxBattler, int idxParty)` | 9/0/1 |
| `allBattlers` | `Array<Battler> allBattlers()` | 7/0/9 |
| `pbCheckGlobalAbility` | `Battler pbCheckGlobalAbility(String abil)` | 7/0/1 |
| `futureSight` | `boolean futureSight` | 6/0/1 |
| `eachSameSideBattler` | `Array<Battler> eachSameSideBattler(int idxBattler)` | 5/0/4 |
| `pbGetOwnerName` | `String pbGetOwnerName(int idxBattler)` | 5/0/2 |
| `pbCheckOpposingAbility` | `Battler pbCheckOpposingAbility(String abil, int idxBattler, boolean nearOnly)` | 5/0/1 |
| `animation` | `void animation(BattleMove move, Battler user, Array<Battler> targets)`<br>`void animation(BattleMove move, Battler user, Array<Battler> targets, int hitNum)`<br>`void animation(int moveId, Battler user, Array<Battler> targets, int hitNum)` | 4/11/11 |
| `pbStartWeather` | `void pbStartWeather(Battler user, int newWeather)`<br>`void pbStartWeather(Battler user, int newWeather, boolean fixedDuration)`<br>`void pbStartWeather(Battler user, int newWeather, boolean fixedDuration, boolean showAnim)` | 4/0/4 |
| `allSameSideBattlers` | `Array<Battler> allSameSideBattlers(int idxBattler)` | 4/0/1 |
| `wiringFieldedBySpeed` | `Array<Battler> wiringFieldedBySpeed()` | 4/0/0 |
| `decision` | `int decision` | 4/15/1 |
| `environment` | `int environment()`<br>`int environment` | 3/5/2 |
| `displayBrief` | `void displayBrief(String msg)` | 3/0/1 |
| `foe` | `Battler foe()` | 2/11/54 |
| `displayPaused` | `void displayPaused(String msg)` | 2/0/1 |
| `pbSideBattlerCount` | `int pbSideBattlerCount(int idxBattler)` | 2/0/3 |
| `pbCanRun` | `boolean pbCanRun(int idxBattler)` | 2/0/6 |
| `choiceIsSwitch` | `boolean choiceIsSwitch(int idxBattler)` | 1/0/4 |
| `canSwitchLax` | `String canSwitchLax(int idxBattler, int idxParty)` | 1/1/6 |
| `canChooseMove` | `String canChooseMove(int idxBattler, int slot)` | 1/3/1 |
| `allOtherSideBattlers` | `Array<Battler> allOtherSideBattlers(int idxBattler)` | 1/0/1 |
| `pbCanSwitch` | `boolean pbCanSwitch(int idxBattler)`<br>`boolean pbCanSwitch(int idxBattler, int idxParty)` | 1/0/3 |
| `pbSetSeen` | `void pbSetSeen(Battler battler)` | 1/0/1 |
| `trainerBattle` | `boolean trainerBattle` | 1/11/16 |
| `endOfRound` | `boolean endOfRound` | 1/0/1 |
| `sideStatUps` | `java.util.List<int[]>[] sideStatUps` | 1/0/3 |

### B. 仅场景层依赖 —— 事件流/UI 契约，冻结（40 个）

- `BattleResult run(int maxTurns)` (76/10)
- `Battler player()` (30/81)
- `String playerName` (24/9)
- `BattleResult result()` (13/9)
- `boolean replace(int idxBattler, int idxParty)` (13/3)
- `int time() ; int time` (12/6)
- `java.util.Set<Integer> badges` (12/6)
- `BattleResult step()` (10/50)
- `Array<Battler> playerParty()` (9/5)
- `Array<ExpAward> lastExpAwards` (9/8)
- `Array<Battler> foeParty()` (6/3)
- `boolean switchStyle` (6/0)
- `boolean canRun()` (5/3)
- `int turns()` (5/7)
- `Battle setCanRun(boolean value)` (4/3)
- `boolean zaMode` (4/1)
- `BattleResult foeTurn()` (3/1)
- `Array<RoundEvent> roundEvents` (3/29)
- `int rawSpeed(Battler battler)` (2/3)
- `Battle addPlayer(Pokemon pokemon)` (2/61)
- `Battle addFoe(Pokemon pokemon)` (2/49)
- `String canSwitch(int idxBattler, int idxParty)` (2/0)
- `boolean registerSwitch(int idxBattler, int idxParty)` (2/5)
- `Battle setCryPlayer(CryPlayer player)` (2/0)
- `boolean levelLockOn` (2/1)
- `boolean leaguePass` (2/0)
- `int playerFieldIndex()` (1/4)
- `int foeFieldIndex()` (1/0)
- `void clearChoice(int idxBattler)` (1/1)
- `boolean canChooseNonActive(int idxBattler)` (1/0)
- `int getReplacementPokemonIndex(int idxBattler)` (1/2)
- `Array<Replacement> eorSwitchPlan(boolean favorDraws)` (1/6)
- `boolean canMegaEvolve(Battler battler)` (1/3)
- `int zaEnergy(int side)` (1/0)
- `boolean megaEvolve(Battler battler)` (1/3)
- `void awardCaptureExperience()` (1/0)
- `boolean registerMove(int idxBattler, int slot)` (1/1)
- `static int ZA_MAX_ENERGY` (1/0)
- `Object scene` (1/1)
- `boolean showAnims` (1/2)

### C. 仅测试依赖 —— 逐个判断改/删（12 个）

- `boolean allFainted(int side)` (测试 2)
- `int judge()` (测试 2)
- `int choiceSwitchParty(int idxBattler)` (测试 2)
- `int defaultChooseNewEnemy(int idxBattler)` (测试 1)
- `boolean isCritical(Battler attacker, Battler defender, BattleMove move, MoveEffects.Effect fx)` (测试 1)
- `double hitChance(Battler user, Battler target, BattleMove move)` (测试 4)
- `BattleMove chosenMove(int idxBattler)` (测试 1)
- `void pbEndPrimordialWeather()` (测试 4)
- `Object pbPlayer()` (测试 1)
- `static int OWNER_CHOOSES` (测试 2)
- `Array<String> roundMessages` (测试 33)
- `Array<String> endOfRoundMessages` (测试 9)

### D. 无外部引用（7 个，可自由改）

`refreshFieldIndices`、`partyBySide`、`cancelChoice`、`accuracyCheck`、`hasPrimal`、`chosenMoveSlot`、`lastExpGain`
