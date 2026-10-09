# api-usage

> 由 `builder.bat audit` 自动生成(阶段 6),请勿手工编辑。

- 工程路径: `E:\仓库\范例\929`
- 生成时间: 2026-10-09 05:49:02 UTC
- Builder 版本: 0.1.0


## 总览


| 指标 | 数值 |
| --- | --- |
| Essentials API(被事件使用) | 120 |
| 调用次数(occurrences) | 7219 |
| 参数形态总数(unique patterns) | 128 |
| 单一形态的 API | 91 |
| 多形态的 API(需重点翻译) | 29 |
| Unresolved 标识符 | 0 |


## 全部 Essentials API


| API | Count | Blocks | Maps | Unique patterns | 参数形态 | 样例位置 |
| --- | --- | --- | --- | --- | --- | --- |
| `pbSetSelfSwitch` | 722 | 444 | 441 | 1 | pbSetSelfSwitch(num,none,const) x722 | Map002.rxdata event 8 page 2 |
| `setTempSwitchOn` | 680 | 680 | 680 | 1 | setTempSwitchOn(none) x680 | Map002.rxdata event 1 page 2 |
| `pbItemBall` | 642 | 637 | 637 | 2 | pbItemBall(sym,num) x631<br>pbItemBall(sym) x11 | Map002.rxdata event 22 page 1 |
| `pbTrainerIntro` | 467 | 467 | 467 | 1 | pbTrainerIntro(sym) x467 | Map006.rxdata event 29 page 1 |
| `pbTrainerEnd` | 423 | 423 | 423 | 1 | pbTrainerEnd(<no-parens>) x423 | Map006.rxdata event 29 page 1 |
| `pbReceiveItem` | 387 | 332 | 332 | 3 | pbReceiveItem(sym,num) x384<br>pbReceiveItem(expr) x2<br>pbReceiveItem(sym) x1 | Map003.rxdata event 4 page 2 |
| `get_character` | 335 | 335 | 335 | 1 | get_character(num) x335 | Map010.rxdata event 1 page 1 |
| `pbNoticePlayer` | 329 | 329 | 329 | 1 | pbNoticePlayer(expr) x329 | Map010.rxdata event 1 page 1 |
| `pbGenPkmn` | 232 | 210 | 210 | 1 | pbGenPkmn(sym,num) x232 | Map003.rxdata event 9 page 3 |
| `pbBridgeOff` | 222 | 222 | 222 | 1 | pbBridgeOff(<no-parens>) x222 | Map021.rxdata event 12 page 1 |
| `pbSmashThisEvent` | 196 | 196 | 196 | 1 | pbSmashThisEvent(<no-parens>) x196 | Map042.rxdata event 7 page 1 |
| `pbBridgeOn` | 161 | 161 | 161 | 1 | pbBridgeOn(<no-parens>) x161 | Map021.rxdata event 20 page 1 |
| `pbRockSmashRandomEncounter` | 155 | 155 | 155 | 1 | pbRockSmashRandomEncounter(<no-parens>) x155 | Map048.rxdata event 29 page 1 |
| `pbSEPlay` | 132 | 130 | 130 | 2 | pbSEPlay(none) x81<br>pbSEPlay(name) x51 | Map005.rxdata event 1 page 1 |
| `pbBerryPlant` | 124 | 124 | 124 | 1 | pbBerryPlant(<no-parens>) x124 | Map010.rxdata event 10 page 2 |
| `makeShiny` | 118 | 118 | 118 | 1 | makeShiny(<no-parens>) x118 | Map006.rxdata event 31 page 1 |
| `pbAddPokemon` | 118 | 96 | 96 | 3 | pbAddPokemon(name,num) x110<br>pbAddPokemon(sym,num) x6<br>pbAddPokemon(name) x2 | Map003.rxdata event 9 page 3 |
| `pbPickBerry` | 117 | 117 | 117 | 1 | pbPickBerry(sym,num) x117 | Map010.rxdata event 10 page 1 |
| `pbSet` | 109 | 96 | 95 | 2 | pbSet(num,name) x89<br>pbSet(num,expr) x20 | Map005.rxdata event 1 page 1 |
| `_I` | 97 | 42 | 42 | 1 | _I(none) x97 | Map009.rxdata event 65 page 1 |
| `pbWait` | 81 | 81 | 81 | 1 | pbWait(num) x81 | Map005.rxdata event 1 page 1 |
| `pokemonCount` | 81 | 81 | 81 | 1 | pokemonCount(<no-parens>) x81 | Map005.rxdata event 1 page 1 |
| `pbPokemonMart` | 77 | 77 | 77 | 2 | pbPokemonMart(expr) x76<br>pbPokemonMart(global) x1 | Map005.rxdata event 9 page 1 |
| `pbCaveEntrance` | 68 | 68 | 68 | 1 | pbCaveEntrance(<no-parens>) x68 | Map008.rxdata event 3 page 1 |
| `pbGet` | 65 | 54 | 54 | 1 | pbGet(num) x65 | Map009.rxdata event 65 page 1 |
| `setBattleRule` | 65 | 63 | 63 | 1 | setBattleRule(none) x65 | Map010.rxdata event 15 page 2 |
| `pbShowMap` | 61 | 61 | 60 | 1 | pbShowMap(<no-parens>) x61 | Map077.rxdata event 6 page 1 |
| `pbCaveExit` | 59 | 59 | 59 | 1 | pbCaveExit(<no-parens>) x59 | Map020.rxdata event 1 page 1 |
| `pbPokeCenterPC` | 59 | 59 | 59 | 1 | pbPokeCenterPC(<no-parens>) x59 | Map005.rxdata event 3 page 1 |
| `setAbility` | 57 | 57 | 57 | 1 | setAbility(num) x57 | Map003.rxdata event 9 page 3 |
| `pbSetPokemonCenter` | 56 | 56 | 56 | 1 | pbSetPokemonCenter(<no-parens>) x56 | Map005.rxdata event 1 page 1 |
| `pbCryFile` | 51 | 49 | 49 | 2 | pbCryFile(num) x50<br>pbCryFile(sym) x1 | Map038.rxdata event 50 page 1 |
| `pbStoreItem` | 48 | 48 | 48 | 1 | pbStoreItem(sym) x48 | Map044.rxdata event 19 page 1 |
| `calcStats` | 47 | 47 | 47 | 1 | calcStats(<no-parens>) x47 | Map009.rxdata event 65 page 1 |
| `pbPushThisBoulder` | 45 | 45 | 45 | 1 | pbPushThisBoulder(<no-parens>) x45 | Map135.rxdata event 5 page 1 |
| `pbChoosePokemonForTrade` | 39 | 39 | 39 | 1 | pbChoosePokemonForTrade(num,num,sym) x39 | Map009.rxdata event 65 page 1 |
| `pbStartTrade` | 37 | 37 | 37 | 2 | pbStartTrade(expr,name,expr,expr,num) x23<br>pbStartTrade(expr,name,expr,expr,num,num) x14 | Map009.rxdata event 65 page 1 |
| `count` | 35 | 35 | 35 | 1 | count(<no-parens>) x35 | Map005.rxdata event 1 page 1 |
| `pbDeleteItem` | 35 | 34 | 34 | 2 | pbDeleteItem(sym) x34<br>pbDeleteItem(expr) x1 | Map022.rxdata event 12 page 1 |
| `pbRemoveDependencies` | 28 | 28 | 28 | 1 | pbRemoveDependencies(none) x28 | Map012.rxdata event 69 page 1 |
| `pbRegisterPartner` | 27 | 27 | 27 | 2 | pbRegisterPartner(sym,none,num) x14<br>pbRegisterPartner(sym,none) x13 | Map012.rxdata event 69 page 1 |
| `pbSlotMachine` | 24 | 24 | 24 | 1 | pbSlotMachine(<no-parens>) x24 | Map326.rxdata event 1 page 1 |
| `setPrice` | 22 | 3 | 3 | 1 | setPrice(sym,num,num) x22 | Map069.rxdata event 78 page 1 |
| `setItem` | 20 | 18 | 18 | 1 | setItem(sym) x20 | Map004.rxdata event 1 page 4 |
| `pbDeregisterPartner` | 19 | 19 | 18 | 1 | pbDeregisterPartner(<no-parens>) x19 | Map132.rxdata event 14 page 1 |
| `new` | 17 | 17 | 17 | 5 | new(sym,num,global) x6<br>new(<no-parens>) x5<br>new(num,num,num,num) x4<br>new(num,num,num) x1<br>new(name) x1 | Map001.rxdata event 1 page 1 |
| `weather` | 17 | 17 | 17 | 1 | weather(name,num,num) x17 | Map304.rxdata event 41 page 1 |
| `pbAddDependency2` | 16 | 16 | 16 | 2 | pbAddDependency2(num,none,num) x8<br>pbAddDependency2(ivar,none,num) x8 | Map012.rxdata event 69 page 1 |
| `pbLearnMove` | 16 | 5 | 5 | 1 | pbLearnMove(sym) x16 | Map145.rxdata event 6 page 3 |
| `pbSave` | 16 | 16 | 16 | 1 | pbSave(<no-parens>) x16 | Map111.rxdata event 10 page 1 |
| `makeFemale` | 15 | 15 | 15 | 1 | makeFemale(<no-parens>) x15 | Map038.rxdata event 48 page 1 |
| `pokemonParty` | 14 | 14 | 14 | 1 | pokemonParty(<no-parens>) x14 | Map119.rxdata event 1 page 1 |
| `giveRibbon` | 11 | 11 | 11 | 1 | giveRibbon(sym) x11 | Map025.rxdata event 8 page 1 |
| `makeMale` | 9 | 9 | 9 | 1 | makeMale(<no-parens>) x9 | Map024.rxdata event 10 page 3 |
| `makeSuperShiny` | 6 | 6 | 6 | 1 | makeSuperShiny(<no-parens>) x6 | Map081.rxdata event 18 page 1 |
| `pbMessage_ex` | 6 | 6 | 6 | 2 | pbMessage_ex(name,ivar) x5<br>pbMessage_ex(name,ivar,num) x1 | Map019.rxdata event 4 page 1 |
| `pbSetEventTime` | 6 | 6 | 6 | 1 | pbSetEventTime(<no-parens>) x6 | Map326.rxdata event 17 page 1 |
| `getName` | 5 | 5 | 5 | 2 | getName(name) x3<br>getName(expr) x2 | Map128.rxdata event 1 page 1 |
| `name` | 5 | 5 | 5 | 1 | name(<no-parens>) x5 | Map025.rxdata event 8 page 1 |
| `pbHallOfFameEntry` | 5 | 5 | 5 | 1 | pbHallOfFameEntry(<no-parens>) x5 | Map119.rxdata event 1 page 1 |
| `setVariable` | 5 | 5 | 5 | 1 | setVariable(sym) x5 | Map326.rxdata event 17 page 1 |
| `isConst?` | 4 | 4 | 4 | 1 | isConst?(expr,name,sym) x4 | Map152.rxdata event 8 page 1 |
| `pbDayCareGetDeposited` | 4 | 3 | 3 | 2 | pbDayCareGetDeposited(num,num,num) x3<br>pbDayCareGetDeposited(expr,num,num) x1 | Map038.rxdata event 1 page 1 |
| `pbMessage` | 4 | 4 | 4 | 2 | pbMessage(expr,expr,expr,const,name) x2<br>pbMessage(expr,array,expr,const,name) x2 | Map102.rxdata event 2 page 1 |
| `pbToneChangeAll` | 4 | 4 | 4 | 1 | pbToneChangeAll(expr,num) x4 | Map001.rxdata event 1 page 1 |
| `pbUnlockDex` | 4 | 3 | 3 | 1 | pbUnlockDex(num) x4 | Map004.rxdata event 10 page 2 |
| `setNature` | 4 | 4 | 4 | 1 | setNature(sym) x4 | Map055.rxdata event 21 page 1 |
| `getID` | 3 | 3 | 3 | 1 | getID(name,name) x3 | Map326.rxdata event 28 page 1 |
| `item` | 3 | 3 | 3 | 1 | item(<no-parens>) x3 | Map326.rxdata event 28 page 1 |
| `price` | 3 | 3 | 3 | 1 | price(<no-parens>) x3 | Map326.rxdata event 28 page 1 |
| `firstPokemon` | 2 | 2 | 2 | 1 | firstPokemon(<no-parens>) x2 | Map025.rxdata event 8 page 1 |
| `pbChangePlayer` | 2 | 2 | 2 | 1 | pbChangePlayer(num) x2 | Map001.rxdata event 2 page 1 |
| `pbChooseNonEggPokemon` | 2 | 2 | 2 | 1 | pbChooseNonEggPokemon(num,num) x2 | Map100.rxdata event 1 page 1 |
| `pbDayCareDeposit` | 2 | 2 | 2 | 1 | pbDayCareDeposit(expr) x2 | Map100.rxdata event 1 page 1 |
| `pbEnd` | 2 | 2 | 2 | 1 | pbEnd(<no-parens>) x2 | Map028.rxdata event 2 page 2 |
| `pbGetSelfSwitch` | 2 | 1 | 1 | 1 | pbGetSelfSwitch(num,none) x2 | Map093.rxdata event 9 page 1 |
| `pbTrainerName` | 2 | 2 | 2 | 1 | pbTrainerName(none) x2 | Map001.rxdata event 2 page 1 |
| `pbWildBattle` | 2 | 2 | 2 | 1 | pbWildBattle(sym,num) x2 | Map225.rxdata event 4 page 1 |
| `hasRibbon?` | 1 | 1 | 1 | 1 | hasRibbon?(sym) x1 | Map025.rxdata event 8 page 1 |
| `id` | 1 | 1 | 1 | 1 | id(<no-parens>) x1 | Map453.rxdata event 4 page 2 |
| `insert` | 1 | 1 | 1 | 1 | insert(num,none) x1 | Map319.rxdata event 1 page 1 |
| `party` | 1 | 1 | 1 | 1 | party(<no-parens>) x1 | Map205.rxdata event 2 page 5 |
| `pbBuyTriads` | 1 | 1 | 1 | 1 | pbBuyTriads(<no-parens>) x1 | Map326.rxdata event 23 page 1 |
| `pbChooseFossil` | 1 | 1 | 1 | 1 | pbChooseFossil(num) x1 | Map128.rxdata event 1 page 1 |
| `pbChoosePokemon` | 1 | 1 | 1 | 1 | pbChoosePokemon(num,num,expr,const) x1 | Map105.rxdata event 10 page 1 |
| `pbConvertItemToPokemon` | 1 | 1 | 1 | 1 | pbConvertItemToPokemon(num,name) x1 | Map128.rxdata event 1 page 1 |
| `pbDayCareChoose` | 1 | 1 | 1 | 1 | pbDayCareChoose(expr,num) x1 | Map100.rxdata event 1 page 1 |
| `pbDayCareGenerateEgg` | 1 | 1 | 1 | 1 | pbDayCareGenerateEgg(<no-parens>) x1 | Map038.rxdata event 1 page 1 |
| `pbDayCareGetCompatibility` | 1 | 1 | 1 | 1 | pbDayCareGetCompatibility(num) x1 | Map038.rxdata event 1 page 1 |
| `pbDayCareWithdraw` | 1 | 1 | 1 | 1 | pbDayCareWithdraw(expr) x1 | Map100.rxdata event 1 page 1 |
| `pbDoubleTrainerBattle` | 1 | 1 | 1 | 1 | pbDoubleTrainerBattle(sym,none,num,const,sym,none,num,const,const) x1 | Map218.rxdata event 16 page 2 |
| `pbExclaim` | 1 | 1 | 1 | 1 | pbExclaim(expr) x1 | Map194.rxdata event 3 page 1 |
| `pbFadeOutIn` | 1 | 1 | 1 | 1 | pbFadeOutIn(num) x1 | Map012.rxdata event 69 page 1 |
| `pbGenerateEgg` | 1 | 1 | 1 | 1 | pbGenerateEgg(sym) x1 | Map058.rxdata event 12 page 2 |
| `pbHasRelearnableMove?` | 1 | 1 | 1 | 1 | pbHasRelearnableMove?(name) x1 | Map105.rxdata event 10 page 1 |
| `pbLottery` | 1 | 1 | 1 | 1 | pbLottery(expr,num,num,num) x1 | Map326.rxdata event 17 page 1 |
| `pbMiningGame` | 1 | 1 | 1 | 1 | pbMiningGame(<no-parens>) x1 | Map022.rxdata event 21 page 1 |
| `pbNextMysteryGiftID` | 1 | 1 | 1 | 1 | pbNextMysteryGiftID(<no-parens>) x1 | Map453.rxdata event 4 page 2 |
| `pbReceiveMysteryGift` | 1 | 1 | 1 | 1 | pbReceiveMysteryGift(name) x1 | Map453.rxdata event 4 page 2 |
| `pbRemoveDependency2` | 1 | 1 | 0 | 1 | pbRemoveDependency2(none) x1 | CommonEvents.rxdata event 2 page 1 |
| `pbSellTriads` | 1 | 1 | 1 | 1 | pbSellTriads(<no-parens>) x1 | Map326.rxdata event 23 page 1 |
| `pbSetLotteryNumber` | 1 | 1 | 1 | 1 | pbSetLotteryNumber(num) x1 | Map326.rxdata event 17 page 1 |
| `pbStart` | 1 | 1 | 1 | 1 | pbStart(num) x1 | Map028.rxdata event 2 page 1 |
| `pbTrainerPC` | 1 | 1 | 1 | 1 | pbTrainerPC(<no-parens>) x1 | Map267.rxdata event 2 page 1 |
| `pbVoltorbFlip` | 1 | 1 | 1 | 1 | pbVoltorbFlip(<no-parens>) x1 | Map326.rxdata event 16 page 1 |
| `pokedexOwned` | 1 | 1 | 0 | 1 | pokedexOwned(<no-parens>) x1 | CommonEvents.rxdata event 1 page 1 |
| `pokedexSeen` | 1 | 1 | 0 | 1 | pokedexSeen(<no-parens>) x1 | CommonEvents.rxdata event 1 page 1 |
| `$game_screen` | 0 | 17 | 17 | 0 |  |  |
| `$PokemonBag` | 0 | 84 | 84 | 0 |  |  |
| `$PokemonGlobal` | 0 | 8 | 8 | 0 |  |  |
| `$scene` | 0 | 5 | 5 | 0 |  |  |
| `$Trainer` | 0 | 125 | 124 | 0 |  |  |
| `PBFieldWeather` | 0 | 17 | 17 | 0 |  |  |
| `PBItems` | 0 | 13 | 13 | 0 |  |  |
| `pbSafariState` | 0 | 3 | 3 | 0 |  |  |
| `PBSpecies` | 0 | 6 | 6 | 0 |  |  |
| `PokeBattle_Pokemon` | 0 | 6 | 6 | 0 |  |  |
| `PokemonTrainerCard_Scene` | 0 | 1 | 1 | 0 |  |  |
| `PokemonTrainerCardScreen` | 0 | 1 | 1 | 0 |  |  |
| `Scene_Credits` | 0 | 5 | 5 | 0 |  |  |


## 需重点翻译的 API(多种参数形态)


| API | Unique patterns | 形态明细 |
| --- | --- | --- |
| `new` | 5 | 6x new(sym,num,global)<br>5x new(<no-parens>)<br>4x new(num,num,num,num)<br>1x new(num,num,num)<br>1x new(name) |
| `pbReceiveItem` | 3 | 384x pbReceiveItem(sym,num)<br>2x pbReceiveItem(expr)<br>1x pbReceiveItem(sym) |
| `pbAddPokemon` | 3 | 110x pbAddPokemon(name,num)<br>6x pbAddPokemon(sym,num)<br>2x pbAddPokemon(name) |
| `pbItemBall` | 2 | 631x pbItemBall(sym,num)<br>11x pbItemBall(sym) |
| `pbSEPlay` | 2 | 81x pbSEPlay(none)<br>51x pbSEPlay(name) |
| `pbSet` | 2 | 89x pbSet(num,name)<br>20x pbSet(num,expr) |
| `pbPokemonMart` | 2 | 76x pbPokemonMart(expr)<br>1x pbPokemonMart(global) |
| `pbCryFile` | 2 | 50x pbCryFile(num)<br>1x pbCryFile(sym) |
| `pbStartTrade` | 2 | 23x pbStartTrade(expr,name,expr,expr,num)<br>14x pbStartTrade(expr,name,expr,expr,num,num) |
| `pbDeleteItem` | 2 | 34x pbDeleteItem(sym)<br>1x pbDeleteItem(expr) |
| `pbRegisterPartner` | 2 | 14x pbRegisterPartner(sym,none,num)<br>13x pbRegisterPartner(sym,none) |
| `pbAddDependency2` | 2 | 8x pbAddDependency2(num,none,num)<br>8x pbAddDependency2(ivar,none,num) |
| `pbMessage_ex` | 2 | 5x pbMessage_ex(name,ivar)<br>1x pbMessage_ex(name,ivar,num) |
| `getName` | 2 | 3x getName(name)<br>2x getName(expr) |
| `pbDayCareGetDeposited` | 2 | 3x pbDayCareGetDeposited(num,num,num)<br>1x pbDayCareGetDeposited(expr,num,num) |
| `pbMessage` | 2 | 2x pbMessage(expr,expr,expr,const,name)<br>2x pbMessage(expr,array,expr,const,name) |


## RPG Maker / Ruby 运行时引用


| 引用 | 分类 | Count | Blocks |
| --- | --- | --- | --- |
| `push` | RUBY | 29 | 27 |
| `p` | RUBY | 2 | 2 |

