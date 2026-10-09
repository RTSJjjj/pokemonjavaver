# script-event-audit

> 由 `builder.bat audit` 自动生成(阶段 6),请勿手工编辑。

- 工程路径: `E:\仓库\范例\929`
- 生成时间: 2026-10-09 11:42:56 UTC
- Builder 版本: 0.1.0


## 总量


| 指标 | 数量 |
| --- | --- |
| 地图文件 | 522 |
| 已分析地图 | 522 |
| 含事件脚本的地图 | 489 |
| 事件 | 8835 |
| 事件页(page) | 13280 |
| 事件命令 | 130618 |
| CommonEvent | 200 |
| Script block(355/655 合并后) | 5441 |
| 其中来自 CommonEvent | 11 |
| 合并的续行命令 | 3343 |
| Ruby 行数 | 8784 |


## 脚本分类


| 分类 | block 数 | 占比 |
| --- | --- | --- |
| ESSENTIALS_API | 4950 | 91.0% |
| PLUGIN_API | 465 | 8.5% |
| SIMPLE_EXPRESSION | 26 | 0.5% |


## Essentials API 使用表


| API | Count | Blocks | Maps | Unique patterns | 参数形态 |
| --- | --- | --- | --- | --- | --- |
| `pbSetSelfSwitch` | 722 | 444 | 441 | 1 | pbSetSelfSwitch(num,none,const) x722 |
| `setTempSwitchOn` | 680 | 680 | 680 | 1 | setTempSwitchOn(none) x680 |
| `pbItemBall` | 642 | 637 | 637 | 2 | pbItemBall(sym,num) x631<br>pbItemBall(sym) x11 |
| `pbTrainerIntro` | 467 | 467 | 467 | 1 | pbTrainerIntro(sym) x467 |
| `pbTrainerEnd` | 423 | 423 | 423 | 1 | pbTrainerEnd(<no-parens>) x423 |
| `pbReceiveItem` | 387 | 332 | 332 | 3 | pbReceiveItem(sym,num) x384<br>pbReceiveItem(expr) x2<br>pbReceiveItem(sym) x1 |
| `get_character` | 335 | 335 | 335 | 1 | get_character(num) x335 |
| `pbNoticePlayer` | 329 | 329 | 329 | 1 | pbNoticePlayer(expr) x329 |
| `pbGenPkmn` | 232 | 210 | 210 | 1 | pbGenPkmn(sym,num) x232 |
| `pbBridgeOff` | 222 | 222 | 222 | 1 | pbBridgeOff(<no-parens>) x222 |
| `pbSmashThisEvent` | 196 | 196 | 196 | 1 | pbSmashThisEvent(<no-parens>) x196 |
| `pbBridgeOn` | 161 | 161 | 161 | 1 | pbBridgeOn(<no-parens>) x161 |
| `pbRockSmashRandomEncounter` | 155 | 155 | 155 | 1 | pbRockSmashRandomEncounter(<no-parens>) x155 |
| `pbSEPlay` | 132 | 130 | 130 | 2 | pbSEPlay(none) x81<br>pbSEPlay(name) x51 |
| `pbBerryPlant` | 124 | 124 | 124 | 1 | pbBerryPlant(<no-parens>) x124 |
| `makeShiny` | 118 | 118 | 118 | 1 | makeShiny(<no-parens>) x118 |
| `pbAddPokemon` | 118 | 96 | 96 | 3 | pbAddPokemon(name,num) x110<br>pbAddPokemon(sym,num) x6<br>pbAddPokemon(name) x2 |
| `pbPickBerry` | 117 | 117 | 117 | 1 | pbPickBerry(sym,num) x117 |
| `pbSet` | 109 | 96 | 95 | 2 | pbSet(num,name) x89<br>pbSet(num,expr) x20 |
| `_I` | 97 | 42 | 42 | 1 | _I(none) x97 |
| `pbWait` | 81 | 81 | 81 | 1 | pbWait(num) x81 |
| `pokemonCount` | 81 | 81 | 81 | 1 | pokemonCount(<no-parens>) x81 |
| `pbPokemonMart` | 77 | 77 | 77 | 2 | pbPokemonMart(expr) x76<br>pbPokemonMart(global) x1 |
| `pbCaveEntrance` | 68 | 68 | 68 | 1 | pbCaveEntrance(<no-parens>) x68 |
| `pbGet` | 65 | 54 | 54 | 1 | pbGet(num) x65 |
| `setBattleRule` | 65 | 63 | 63 | 1 | setBattleRule(none) x65 |
| `pbShowMap` | 61 | 61 | 60 | 1 | pbShowMap(<no-parens>) x61 |
| `pbCaveExit` | 59 | 59 | 59 | 1 | pbCaveExit(<no-parens>) x59 |
| `pbPokeCenterPC` | 59 | 59 | 59 | 1 | pbPokeCenterPC(<no-parens>) x59 |
| `setAbility` | 57 | 57 | 57 | 1 | setAbility(num) x57 |
| `pbSetPokemonCenter` | 56 | 56 | 56 | 1 | pbSetPokemonCenter(<no-parens>) x56 |
| `pbCryFile` | 51 | 49 | 49 | 2 | pbCryFile(num) x50<br>pbCryFile(sym) x1 |
| `pbStoreItem` | 48 | 48 | 48 | 1 | pbStoreItem(sym) x48 |
| `calcStats` | 47 | 47 | 47 | 1 | calcStats(<no-parens>) x47 |
| `pbPushThisBoulder` | 45 | 45 | 45 | 1 | pbPushThisBoulder(<no-parens>) x45 |
| `pbChoosePokemonForTrade` | 39 | 39 | 39 | 1 | pbChoosePokemonForTrade(num,num,sym) x39 |
| `pbStartTrade` | 37 | 37 | 37 | 2 | pbStartTrade(expr,name,expr,expr,num) x23<br>pbStartTrade(expr,name,expr,expr,num,num) x14 |
| `count` | 35 | 35 | 35 | 1 | count(<no-parens>) x35 |
| `pbDeleteItem` | 35 | 34 | 34 | 2 | pbDeleteItem(sym) x34<br>pbDeleteItem(expr) x1 |
| `pbRemoveDependencies` | 28 | 28 | 28 | 1 | pbRemoveDependencies(none) x28 |
| `pbRegisterPartner` | 27 | 27 | 27 | 2 | pbRegisterPartner(sym,none,num) x14<br>pbRegisterPartner(sym,none) x13 |
| `pbSlotMachine` | 24 | 24 | 24 | 1 | pbSlotMachine(<no-parens>) x24 |
| `setPrice` | 22 | 3 | 3 | 1 | setPrice(sym,num,num) x22 |
| `setItem` | 20 | 18 | 18 | 1 | setItem(sym) x20 |
| `pbDeregisterPartner` | 19 | 19 | 18 | 1 | pbDeregisterPartner(<no-parens>) x19 |
| `new` | 17 | 17 | 17 | 5 | new(sym,num,global) x6<br>new(<no-parens>) x5<br>new(num,num,num,num) x4<br>new(num,num,num) x1<br>new(name) x1 |
| `weather` | 17 | 17 | 17 | 1 | weather(name,num,num) x17 |
| `pbAddDependency2` | 16 | 16 | 16 | 2 | pbAddDependency2(num,none,num) x8<br>pbAddDependency2(ivar,none,num) x8 |
| `pbLearnMove` | 16 | 5 | 5 | 1 | pbLearnMove(sym) x16 |
| `pbSave` | 16 | 16 | 16 | 1 | pbSave(<no-parens>) x16 |
| `makeFemale` | 15 | 15 | 15 | 1 | makeFemale(<no-parens>) x15 |
| `pokemonParty` | 14 | 14 | 14 | 1 | pokemonParty(<no-parens>) x14 |
| `giveRibbon` | 11 | 11 | 11 | 1 | giveRibbon(sym) x11 |
| `makeMale` | 9 | 9 | 9 | 1 | makeMale(<no-parens>) x9 |
| `makeSuperShiny` | 6 | 6 | 6 | 1 | makeSuperShiny(<no-parens>) x6 |
| `pbMessage_ex` | 6 | 6 | 6 | 2 | pbMessage_ex(name,ivar) x5<br>pbMessage_ex(name,ivar,num) x1 |
| `pbSetEventTime` | 6 | 6 | 6 | 1 | pbSetEventTime(<no-parens>) x6 |
| `getName` | 5 | 5 | 5 | 2 | getName(name) x3<br>getName(expr) x2 |
| `name` | 5 | 5 | 5 | 1 | name(<no-parens>) x5 |
| `pbHallOfFameEntry` | 5 | 5 | 5 | 1 | pbHallOfFameEntry(<no-parens>) x5 |
| `setVariable` | 5 | 5 | 5 | 1 | setVariable(sym) x5 |
| `isConst?` | 4 | 4 | 4 | 1 | isConst?(expr,name,sym) x4 |
| `pbDayCareGetDeposited` | 4 | 3 | 3 | 2 | pbDayCareGetDeposited(num,num,num) x3<br>pbDayCareGetDeposited(expr,num,num) x1 |
| `pbMessage` | 4 | 4 | 4 | 2 | pbMessage(expr,expr,expr,const,name) x2<br>pbMessage(expr,array,expr,const,name) x2 |
| `pbToneChangeAll` | 4 | 4 | 4 | 1 | pbToneChangeAll(expr,num) x4 |
| `pbUnlockDex` | 4 | 3 | 3 | 1 | pbUnlockDex(num) x4 |
| `setNature` | 4 | 4 | 4 | 1 | setNature(sym) x4 |
| `getID` | 3 | 3 | 3 | 1 | getID(name,name) x3 |
| `item` | 3 | 3 | 3 | 1 | item(<no-parens>) x3 |
| `price` | 3 | 3 | 3 | 1 | price(<no-parens>) x3 |
| `firstPokemon` | 2 | 2 | 2 | 1 | firstPokemon(<no-parens>) x2 |
| `pbChangePlayer` | 2 | 2 | 2 | 1 | pbChangePlayer(num) x2 |
| `pbChooseNonEggPokemon` | 2 | 2 | 2 | 1 | pbChooseNonEggPokemon(num,num) x2 |
| `pbDayCareDeposit` | 2 | 2 | 2 | 1 | pbDayCareDeposit(expr) x2 |
| `pbEnd` | 2 | 2 | 2 | 1 | pbEnd(<no-parens>) x2 |
| `pbGetSelfSwitch` | 2 | 1 | 1 | 1 | pbGetSelfSwitch(num,none) x2 |
| `pbTrainerName` | 2 | 2 | 2 | 1 | pbTrainerName(none) x2 |
| `pbWildBattle` | 2 | 2 | 2 | 1 | pbWildBattle(sym,num) x2 |
| `hasRibbon?` | 1 | 1 | 1 | 1 | hasRibbon?(sym) x1 |
| `id` | 1 | 1 | 1 | 1 | id(<no-parens>) x1 |
| `insert` | 1 | 1 | 1 | 1 | insert(num,none) x1 |
| `party` | 1 | 1 | 1 | 1 | party(<no-parens>) x1 |
| `pbBuyTriads` | 1 | 1 | 1 | 1 | pbBuyTriads(<no-parens>) x1 |
| `pbChooseFossil` | 1 | 1 | 1 | 1 | pbChooseFossil(num) x1 |
| `pbChoosePokemon` | 1 | 1 | 1 | 1 | pbChoosePokemon(num,num,expr,const) x1 |
| `pbConvertItemToPokemon` | 1 | 1 | 1 | 1 | pbConvertItemToPokemon(num,name) x1 |
| `pbDayCareChoose` | 1 | 1 | 1 | 1 | pbDayCareChoose(expr,num) x1 |
| `pbDayCareGenerateEgg` | 1 | 1 | 1 | 1 | pbDayCareGenerateEgg(<no-parens>) x1 |
| `pbDayCareGetCompatibility` | 1 | 1 | 1 | 1 | pbDayCareGetCompatibility(num) x1 |
| `pbDayCareWithdraw` | 1 | 1 | 1 | 1 | pbDayCareWithdraw(expr) x1 |
| `pbDoubleTrainerBattle` | 1 | 1 | 1 | 1 | pbDoubleTrainerBattle(sym,none,num,const,sym,none,num,const,const) x1 |
| `pbExclaim` | 1 | 1 | 1 | 1 | pbExclaim(expr) x1 |
| `pbFadeOutIn` | 1 | 1 | 1 | 1 | pbFadeOutIn(num) x1 |
| `pbGenerateEgg` | 1 | 1 | 1 | 1 | pbGenerateEgg(sym) x1 |
| `pbHasRelearnableMove?` | 1 | 1 | 1 | 1 | pbHasRelearnableMove?(name) x1 |
| `pbLottery` | 1 | 1 | 1 | 1 | pbLottery(expr,num,num,num) x1 |
| `pbMiningGame` | 1 | 1 | 1 | 1 | pbMiningGame(<no-parens>) x1 |
| `pbNextMysteryGiftID` | 1 | 1 | 1 | 1 | pbNextMysteryGiftID(<no-parens>) x1 |
| `pbReceiveMysteryGift` | 1 | 1 | 1 | 1 | pbReceiveMysteryGift(name) x1 |
| `pbRemoveDependency2` | 1 | 1 | 0 | 1 | pbRemoveDependency2(none) x1 |
| `pbSellTriads` | 1 | 1 | 1 | 1 | pbSellTriads(<no-parens>) x1 |
| `pbSetLotteryNumber` | 1 | 1 | 1 | 1 | pbSetLotteryNumber(num) x1 |
| `pbStart` | 1 | 1 | 1 | 1 | pbStart(num) x1 |
| `pbTrainerPC` | 1 | 1 | 1 | 1 | pbTrainerPC(<no-parens>) x1 |
| `pbVoltorbFlip` | 1 | 1 | 1 | 1 | pbVoltorbFlip(<no-parens>) x1 |
| `pokedexOwned` | 1 | 1 | 0 | 1 | pokedexOwned(<no-parens>) x1 |
| `pokedexSeen` | 1 | 1 | 0 | 1 | pokedexSeen(<no-parens>) x1 |
| `$game_screen` | 0 | 17 | 17 | 0 |  |
| `$PokemonBag` | 0 | 84 | 84 | 0 |  |
| `$PokemonGlobal` | 0 | 8 | 8 | 0 |  |
| `$scene` | 0 | 5 | 5 | 0 |  |
| `$Trainer` | 0 | 125 | 124 | 0 |  |
| `PBFieldWeather` | 0 | 17 | 17 | 0 |  |
| `PBItems` | 0 | 13 | 13 | 0 |  |
| `pbSafariState` | 0 | 3 | 3 | 0 |  |
| `PBSpecies` | 0 | 6 | 6 | 0 |  |
| `PokeBattle_Pokemon` | 0 | 6 | 6 | 0 |  |
| `PokemonTrainerCard_Scene` | 0 | 1 | 1 | 0 |  |
| `PokemonTrainerCardScreen` | 0 | 1 | 1 | 0 |  |
| `Scene_Credits` | 0 | 5 | 5 | 0 |  |

共 **120** 个 Essentials API、**7219** 次调用、**128** 种参数形态。迁移工作量按 unique pattern 计,不按出现次数计。

## Complex Ruby 明细(需人工移植)


判定标准:含 if / unless / while / until / for / case / begin / do、≥3 行或 ≥2 次续行合并。

共 **604** 个 block。


### Map003.rxdata . \PN的家1f

- **Map 3 / Event 8(Counter(2)) / Page 1 / Cmd 93** - 3 行 / 81 字符
```ruby
activateQuest(:Quest1)
pbSetSelfSwitch(5,"A",true) 
pbSetSelfSwitch(9,"A",true)
```

- **Map 3 / Event 9(#f妈妈) / Page 3 / Cmd 56** - 4 行 / 84 字符
```ruby
p=pbGenPkmn(:PANSAGE,15)
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```

- **Map 3 / Event 9(#f妈妈) / Page 3 / Cmd 82** - 4 行 / 84 字符
```ruby
p=pbGenPkmn(:PANSEAR,15)
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```

- **Map 3 / Event 9(#f妈妈) / Page 3 / Cmd 108** - 4 行 / 84 字符
```ruby
p=pbGenPkmn(:PANPOUR,15)
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```

- **Map 3 / Event 9(#f妈妈) / Page 3 / Cmd 133** - 4 行 / 85 字符
```ruby
p=pbGenPkmn(:PANSHOCK,15)
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```


### Map004.rxdata . 茶月研究所

- **Map 4 / Event 1(#f伊娟博士) / Page 4 / Cmd 76** - 6 行 / 121 字符
```ruby
p=pbGenPkmn(:MAGEARNA,100)
p.setItem(:MAGEARNAITE)
p.form = 1
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```

- **Map 4 / Event 8(Controlling event) / Page 5 / Cmd 30** - 4 行 / 79 字符
```ruby
p=pbGenPkmn(:PIKACHU,5)
p.iv=[31,31,31,31,31,31]
p.form = 17
pbAddPokemon(p,1)
```


### Map005.rxdata . 茶月中心

- **Map 5 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 5 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 5 / Event 9(Mart) / Page 1 / Cmd 1** - 7 行 / 126 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:POTION,:SUPERPOTION,
:ANTIDOTE,:PARALYZEHEAL,
:AWAKENING,:BURNHEAL,
:ICEHEAL,:REPEL
])
```

- **Map 5 / Event 9(Mart) / Page 1 / Cmd 11** - 4 行 / 38 字符
```ruby
pbPokemonMart([
:POKEBALL,
:POTION
])
```


### Map006.rxdata . 荼蘼镇

- **Map 6 / Event 31(EV031) / Page 1 / Cmd 4** - 5 行 / 86 字符
```ruby
p=pbGenPkmn(:GOOMY,5)
p.makeShiny
p.iv=[31,31,31,0,31,31]
p.ot="阿辽"
pbAddPokemon(p,1)
```


### Map007.rxdata . 格诺镇

- **Map 7 / Event 22(Trainer(3)) / Page 1 / Cmd 149** - 4 行 / 106 字符
```ruby
completeQuest(:Quest6)
activateQuest(:Quest7)
pbSetSelfSwitch(21,"A",true) 
pbSetSelfSwitch(25,"A",true)
```


### Map009.rxdata . 绯雷市

- **Map 9 / Event 22(大叔) / Page 1 / Cmd 7** - 4 行 / 80 字符
```ruby
p=pbGenPkmn(:MUNCHLAX,5)
p.iv=[31,31,31,31,31,31]
p.makeShiny
pbAddPokemon(p,1)
```

- **Map 9 / Event 65(EV065) / Page 1 / Cmd 19** - 7 行 / 147 字符
```ruby
p=pbGenPkmn(:PAWNIARD,38)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("Keito"), _I("敬人"),0,906)
```


### Map010.rxdata . 1号道路

- **Map 10 / Event 19(闪光) / Page 1 / Cmd 0** - 5 行 / 112 字符
```ruby
pkmn = pbGenPkmn(:RALTS,10)
pkmn.makeShiny
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map012.rxdata . 月央市

- **Map 12 / Event 69(Counter(4)) / Page 1 / Cmd 9** - 5 行 / 133 字符
```ruby
scene = PokemonTrainerCard_Scene.new
screen = PokemonTrainerCardScreen.new(scene)
pbFadeOutIn(99999) { 
 screen.pbStartBadgeScreen
}
```

- **Map 12 / Event 69(Counter(4)) / Page 1 / Cmd 145** - 5 行 / 122 字符
```ruby
pbRemoveDependencies()
pbAddDependency2(70,"索伽",30)
pbRegisterPartner(
  :SUOJIA, "索伽",4)
advanceQuestToStage(:Quest14,2)
```

- **Map 12 / Event 74(EV074) / Page 2 / Cmd 8** - 3 行 / 89 字符
```ruby
pbSetSelfSwitch(56,"A",true)
pbSetSelfSwitch(72,"A",true) 
pbSetSelfSwitch(73,"A",true)
```


### Map015.rxdata . 隐龙市

- **Map 15 / Event 41(EV041) / Page 1 / Cmd 4** - 3 行 / 52 字符
```ruby
p=pbGenPkmn(:BAGON,5)
p.makeShiny
pbAddPokemon(p,1)
```

- **Map 15 / Event 77(Counter(5)) / Page 1 / Cmd 19** - 3 行 / 78 字符
```ruby
completeQuest(:Quest17)
activateQuest(:Quest18)
pbSetSelfSwitch(76,"A",true)
```


### Map017.rxdata . 枫弦镇

- **Map 17 / Event 33(Counter(4)) / Page 1 / Cmd 139** - 4 行 / 108 字符
```ruby
pbSetSelfSwitch(31,"A",true) 
pbSetSelfSwitch(32,"A",true) 
completeQuest(:Quest12)
activateQuest(:Quest13)
```


### Map018.rxdata . 2号道路

- **Map 18 / Event 1(短裤小子) / Page 2 / Cmd 16** - 7 行 / 132 字符
```ruby
p=PokeBattle_Pokemon.new(:PHANTUMP,15,
$Trainer)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
p.calcStats
pbAddPokemon(p,1)
```

- **Map 18 / Event 9(Counter(5)) / Page 1 / Cmd 64** - 4 行 / 106 字符
```ruby
pbSetSelfSwitch(5,"A",true) 
pbToggleFollowingPokemon("on")
completeQuest(:Quest8)
activateQuest(:Quest9)
```

- **Map 18 / Event 20(闪光) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:FLORAGATO,20)
pkmn.form = 1
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map019.rxdata . 铃兰码头

- **Map 19 / Event 4(EV004) / Page 1 / Cmd 20** - 3 行 / 66 字符
```ruby
@ch_cmd.push("取消")
str="要前往哪里？"
@ch_ret=pbMessage_ex(str,@ch_cmd)
```


### Map022.rxdata . 沃饶遗迹

- **Map 22 / Event 9(EV009) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:SOLROCK,30)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 22 / Event 10(EV010) / Page 1 / Cmd 0** - 5 行 / 117 字符
```ruby
pkmn = pbGenPkmn(:LUNATONE,30)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map024.rxdata . 瑞乡民居

- **Map 24 / Event 6(EV006) / Page 1 / Cmd 2** - 3 行 / 48 字符
```ruby
pbPokemonMart([
:TOXICORB,:FLAMEORB,:LIFEORB
])
```

- **Map 24 / Event 10(大木) / Page 3 / Cmd 7** - 40 行 / 727 字符
```ruby
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 9
p.makeMale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 10
p.makeMale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 11
p.makeMale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 12
p.makeMale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 13
p.makeMale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 14
p.makeMale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 15
p.makeMale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 16
p.makeMale
pbAddPokemon(p,1)
```


### Map025.rxdata . 森楠自然公园

- **Map 25 / Event 6(EV006) / Page 1 / Cmd 0** - 5 行 / 84 字符
```ruby
pbPokemonMart([
:XATTACK,:XSPATK,
:XSPEED,:XACCURACY,
:DIREHIT,:XSPDEF,:XDEFENSE
])
```

- **Map 25 / Event 8(Effort ribbon giver) / Page 1 / Cmd 2** - 9 行 / 150 字符
```ruby
p=$Trainer.firstPokemon
ev=0
for i in 0...6
  ev+=p.ev[i]
end
maxed=(ev>=255) ? 1 : 0
maxed=2 if p.hasRibbon?(:EFFORT)
pbSet(1,p.name)
pbSet(2,maxed)
```

- **Map 25 / Event 9(EV009) / Page 1 / Cmd 2** - 5 行 / 65 字符
```ruby
pbPokemonMart([
:HPUP,:PROTEIN,
:IRON,:CALCIUM,
:ZINC,:CARBOS
])
```


### Map028.rxdata . 原野区入口

- **Map 28 / Event 7(EV007) / Page 1 / Cmd 3** - 4 行 / 78 字符
```ruby
p=pbGenPkmn(:DITTO,20)
p.iv=[31,31,31,31,31,31]
p.makeShiny
pbAddPokemon(p,1)
```


### Map029.rxdata . 原野区·西1区

- **Map 29 / Event 2(EV002) / Page 1 / Cmd 0** - 5 行 / 119 字符
```ruby
pkmn = pbGenPkmn(:HELIOPTILE,25)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map031.rxdata . 原野区·北1区

- **Map 31 / Event 4(EV004) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:UMBREON,25)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map034.rxdata . 隐龙民居

- **Map 34 / Event 12(EV012) / Page 1 / Cmd 4** - 8 行 / 147 字符
```ruby
p=pbGenPkmn(:QUAQUAVAL,40)
p.iv=[31,31,31,31,31,31]
p.setItem(:PRETTYFEATHER)
p.form = 1
p.name= "地才舞鸭"
p.ot= "科雷瑟利亚"
p.makeMale
pbAddPokemon(p,1)
```


### Map035.rxdata . 月央民居

- **Map 35 / Event 11(EV011) / Page 1 / Cmd 17** - 7 行 / 139 字符
```ruby
p=pbGenPkmn(:PONYTA,42)
p.iv=[31,0,31,0,31,31]
p.form = 1
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("塞拉斯蒂亚"), _I("露娜"), 1)
```


### Map036.rxdata . 3号道路

- **Map 36 / Event 6(Counter(3)) / Page 2 / Cmd 134** - 4 行 / 106 字符
```ruby
completeQuest(:Quest2)
activateQuest(:Quest3)
pbToggleFollowingPokemon("on")
pbSetSelfSwitch(3,"B",true)
```

- **Map 36 / Event 11(EV011) / Page 1 / Cmd 0** - 5 行 / 114 字符
```ruby
pkmn = pbGenPkmn(:SQUIRTLE,8)
pkmn.battleRank=2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 36 / Event 41(EV041) / Page 1 / Cmd 4** - 5 行 / 95 字符
```ruby
p=pbGenPkmn(:PHANTOMITE,15)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.ot="明月老师"
pbAddPokemon(p,1)
```


### Map037.rxdata . 4号道路

- **Map 37 / Event 30(Trainer(3)) / Page 2 / Cmd 31** - 4 行 / 76 字符
```ruby
p=pbGenPkmn(:PONYTA,3)
p.iv=[31,31,31,31,31,31]
p.ot="无名"
pbAddPokemon(p,1)
```

- **Map 37 / Event 37(EV037) / Page 1 / Cmd 103** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(36,"A",true) 
pbSetSelfSwitch(50,"A",true) 
pbSetSelfSwitch(51,"A",true)
```

- **Map 37 / Event 40(Counter(3)) / Page 1 / Cmd 32** - 4 行 / 108 字符
```ruby
completeQuest(:Quest10)
activateQuest(:Quest11)
pbSetSelfSwitch(39,"A",true) 
pbSetSelfSwitch(38,"A",true)
```

- **Map 37 / Event 50(EV050) / Page 1 / Cmd 101** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(36,"A",true) 
pbSetSelfSwitch(37,"A",true) 
pbSetSelfSwitch(51,"A",true)
```

- **Map 37 / Event 51(EV051) / Page 1 / Cmd 101** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(36,"A",true) 
pbSetSelfSwitch(50,"A",true) 
pbSetSelfSwitch(37,"A",true)
```

- **Map 37 / Event 52(EV052) / Page 1 / Cmd 3** - 6 行 / 116 字符
```ruby
p=pbGenPkmn(:PIKACHU,22)
p.iv=[31,31,31,31,31,31]
p.setItem(:LIGHTBALL)
p.name= "比卡超"
p.ot= "米洛克"
pbAddPokemon(p,1)
```


### Map038.rxdata . 5号道路

- **Map 38 / Event 1(培育大叔) / Page 1 / Cmd 19** - 3 行 / 82 字符
```ruby
pbDayCareGenerateEgg
$PokemonGlobal.daycareEgg=0
$PokemonGlobal.daycareEggSteps=0
```

- **Map 38 / Event 1(培育大叔) / Page 1 / Cmd 57** - 3 行 / 89 字符
```ruby
pbDayCareGetDeposited(0,3,-1)
pbDayCareGetDeposited(1,4,-1)
pbDayCareGetCompatibility(5)
```

- **Map 38 / Event 12(Trainer(5)) / Page 1 / Cmd 15** - 4 行 / 121 字符
```ruby
pbReceiveItem(:MEGARING,1)
pbReceiveItem(:MEGANIUMITER,1)
pbReceiveItem(:BLAZIKENITER,1)
pbReceiveItem(:PRIMARINAITER,1)
```

- **Map 38 / Event 32(EV032) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:GROWLITHE,30)
pkmn.battleRank=2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 38 / Event 48(EV048) / Page 1 / Cmd 7** - 3 行 / 45 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :PHANTOMITE
)
```

- **Map 38 / Event 48(EV048) / Page 1 / Cmd 15** - 8 行 / 155 字符
```ruby
p=pbGenPkmn(:MURKROW,25)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.makeFemale
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("黑暗鸦"), _I("天之下"), 1)
```

- **Map 38 / Event 50(EV050) / Page 1 / Cmd 2** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1014)
pbSEPlay(cry) if cry
```

- **Map 38 / Event 50(EV050) / Page 1 / Cmd 25** - 5 行 / 96 字符
```ruby
p=pbGenPkmn(:OGERPON,50)
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
p.calcStats
pbAddPokemon(p,1)
```


### Map039.rxdata . 曦寒商店

- **Map 39 / Event 4(Mart) / Page 1 / Cmd 0** - 8 行 / 170 字符
```ruby
pbPokemonMart([
:ULTRABALL,:POTION,
:SUPERPOTION,:HYPERPOTION,
:MAXPOTION,:FULLRESTORE,
:MAXREPEL,:FULLHEAL,:REVIVE,
:SUNSTONE,:DUSKSTONE,:DAWNSTONE,
:DRAGONSOULARMOR
])
```


### Map040.rxdata . 曦寒图书馆

- **Map 40 / Event 9(EV009) / Page 2 / Cmd 5** - 5 行 / 94 字符
```ruby
pbPokemonMart([
:ELECTRICSEED,:PSYCHICSEED,
:MISTYSEED,:GRASSYSEED,
:BUGLURESEED,:COLDSEED
])
```

- **Map 40 / Event 16(EV016) / Page 1 / Cmd 190** - 3 行 / 73 字符
```ruby
completeQuest(:Quest29)
activateQuest(:Quest30)
activateQuest(:Quest212)
```

- **Map 40 / Event 16(EV016) / Page 1 / Cmd 197** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(15,"A",true) 
pbSetSelfSwitch(17,"A",true) 
pbSetSelfSwitch(18,"A",true)
```

- **Map 40 / Event 37(EV037) / Page 1 / Cmd 5** - 3 行 / 45 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :INCINEROAR
)
```

- **Map 40 / Event 37(EV037) / Page 1 / Cmd 13** - 8 行 / 152 字符
```ruby
p=pbGenPkmn(:GENGAR,50)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.makeFemale
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("高桥"), _I("诡域"), 1)
```


### Map041.rxdata . 银月桥

- **Map 41 / Event 12(桥) / Page 2 / Cmd 70** - 6 行 / 180 字符
```ruby
pbSetSelfSwitch(13,"A",true) 
pbSetSelfSwitch(15,"A",true) 
pbSetSelfSwitch(18,"A",true) 
pbSetSelfSwitch(19,"A",true) 
pbSetSelfSwitch(20,"A",true) 
pbSetSelfSwitch(21,"A",true)
```

- **Map 41 / Event 12(桥) / Page 2 / Cmd 121** - 3 行 / 85 字符
```ruby
pbSetSelfSwitch(11,"A",true) 
completeQuest(:Quest16)
pbToggleFollowingPokemon("on")
```


### Map043.rxdata . 桃苑森林

- **Map 43 / Event 15(EV015) / Page 2 / Cmd 51** - 4 行 / 119 字符
```ruby
pbSetSelfSwitch(16,"A",true) 
pbSetSelfSwitch(17,"A",true) 
pbSetSelfSwitch(18,"A",true) 
pbSetSelfSwitch(8,"A",true)
```

- **Map 43 / Event 39(EV039) / Page 1 / Cmd 38** - 4 行 / 109 字符
```ruby
pbAddDependency2(38,"阿青",33)
pbRegisterPartner(:CYAN,"阿青",0)
completeQuest(:Quest69)
activateQuest(:Quest70)
```


### Map044.rxdata . 樱祭公园

- **Map 44 / Event 25(EV025) / Page 1 / Cmd 6** - 3 行 / 45 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :BUTTERFREE
)
```

- **Map 44 / Event 25(EV025) / Page 1 / Cmd 16** - 6 行 / 130 字符
```ruby
p=pbGenPkmn(:SCYTHER,10)
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("飞螳螂"), _I("虫一郎"), 1)
```

- **Map 44 / Event 27(EV027) / Page 1 / Cmd 2** - 5 行 / 90 字符
```ruby
p=pbGenPkmn(:QUICROWN,5)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.ot="毛毛"
pbAddPokemon(p,1)
```

- **Map 44 / Event 30(EV030) / Page 1 / Cmd 2** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1015)
pbSEPlay(cry) if cry
```

- **Map 44 / Event 30(EV030) / Page 1 / Cmd 23** - 7 行 / 151 字符
```ruby
pbRemoveDependenciesExceptFollower
pbPokemonFollow(28)
p=pbGenPkmn(:OGERPON,50)
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
p.calcStats
pbAddPokemon(p,1)
```


### Map046.rxdata . 7号道路

- **Map 46 / Event 27(EV027) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:MIENSHAO,45)
pkmn.makeShiny
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 46 / Event 37(EV037) / Page 1 / Cmd 0** - 4 行 / 99 字符
```ruby
pkmn = pbGenPkmn(:CHERUBI,30)
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map048.rxdata . 9号道路

- **Map 48 / Event 8(EV008) / Page 1 / Cmd 3** - 5 行 / 114 字符
```ruby
pkmn = pbGenPkmn(:ZORUA,30)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 48 / Event 25(EV025) / Page 1 / Cmd 0** - 5 行 / 118 字符
```ruby
pkmn = pbGenPkmn(:CARNIVINE,40)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map049.rxdata . 10号道路

- **Map 49 / Event 2(Trainer(2)) / Page 1 / Cmd 0** - 3 行 / 94 字符
```ruby
pbToggleFollowingPokemon("off")
pbTrainerIntro(:SCIENTIST_M)
pbNoticePlayer(get_character(0))
```

- **Map 49 / Event 21(EV021) / Page 1 / Cmd 0** - 5 行 / 119 字符
```ruby
pkmn = pbGenPkmn(:CHESNAUGHT,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map051.rxdata . 12号道路

- **Map 51 / Event 3(船票剧情) / Page 2 / Cmd 250** - 3 行 / 88 字符
```ruby
pbSetSelfSwitch(1,"A",true) 
pbSetSelfSwitch(2,"A",true) 
pbSetSelfSwitch(14,"A",true)
```


### Map052.rxdata . 13号道路

- **Map 52 / Event 6(闪光) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:ROSELIA,15)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 52 / Event 8(EV008) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:LOMBRE,45)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map053.rxdata . 海底遗迹

- **Map 53 / Event 21(EV021) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:SLOWBRO,50)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 53 / Event 26(EV026) / Page 2 / Cmd 8** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map054.rxdata . 14号道路

- **Map 54 / Event 7(EV007) / Page 1 / Cmd 0** - 5 行 / 117 字符
```ruby
pkmn = pbGenPkmn(:VAPOREON,55)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map055.rxdata . 骇浪岛

- **Map 55 / Event 1(EV001) / Page 1 / Cmd 0** - 5 行 / 117 字符
```ruby
pkmn = pbGenPkmn(:GRENINJA,40)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 55 / Event 21(EV021) / Page 1 / Cmd 14** - 8 行 / 164 字符
```ruby
p=pbGenPkmn(:SHARPEDO,55)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
p.setNature(:ADAMANT)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("巨牙鲨"), _I("暗呜"), 1)
```

- **Map 55 / Event 23(EV023) / Page 1 / Cmd 2** - 36 行 / 649 字符
```ruby
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 3
p.name= "换装皮卡丘"
p.makeFemale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 4
p.name= "贵妇皮卡丘"
p.makeFemale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 5
p.name= "蒙面皮卡丘"
p.makeFemale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 6
p.name= "博士皮卡丘"
p.makeFemale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 7
p.name= "偶像皮卡丘"
p.makeFemale
pbAddPokemon(p,1)
p=pbGenPkmn(:PIKACHU,25)
p.iv=[31,31,31,31,31,31]
p.form = 8
p.name= "硬摇滚皮卡丘"
p.makeFemale
pbAddPokemon(p,1)
```


### Map056.rxdata . 15号道路

- **Map 56 / Event 5(草果) / Page 2 / Cmd 7** - 4 行 / 100 字符
```ruby
pkmn = pbGenPkmn(:BLUNIB,40)
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 56 / Event 8(EV008) / Page 1 / Cmd 0** - 5 行 / 117 字符
```ruby
pkmn = pbGenPkmn(:POLITOED,55)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map058.rxdata . 月央民居

- **Map 58 / Event 4(EV004) / Page 1 / Cmd 8** - 7 行 / 126 字符
```ruby
p=pbGenPkmn(:DRIFBLIM,25)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.trainerID=225
p.name= "Chisato"
p.ot="千砂都"
pbAddPokemon(p,1)
```

- **Map 58 / Event 6(EV006) / Page 1 / Cmd 8** - 7 行 / 122 字符
```ruby
p=pbGenPkmn(:PANCHAM,25)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.trainerID=717
p.name= "KEKE"
p.ot="唐可可"
pbAddPokemon(p,1)
```

- **Map 58 / Event 7(EV007) / Page 1 / Cmd 9** - 7 行 / 123 字符
```ruby
p=pbGenPkmn(:VIKAVOLT,25)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.trainerID=501
p.name= "Kanon"
p.ot="香音"
pbAddPokemon(p,1)
```

- **Map 58 / Event 9(EV009) / Page 1 / Cmd 14** - 7 行 / 146 字符
```ruby
p=pbGenPkmn(:STARMIE,32)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("Ren"), _I("叶月恋"),0,1124)
```

- **Map 58 / Event 10(堇) / Page 1 / Cmd 11** - 7 行 / 122 字符
```ruby
p=pbGenPkmn(:WIMPOD,15)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.trainerID=928
p.name= "Sumire"
p.ot="名堇"
pbAddPokemon(p,1)
```


### Map059.rxdata . 椿叶市

- **Map 59 / Event 15(前往裂缝剧情) / Page 2 / Cmd 59** - 5 行 / 153 字符
```ruby
pbSetSelfSwitch(10,"A",true) 
pbSetSelfSwitch(11,"A",true) 
pbSetSelfSwitch(18,"A",true) 
pbSetSelfSwitch(21,"A",true) 
advanceQuestToStage(:Quest33,2)
```

- **Map 59 / Event 29(EV029) / Page 1 / Cmd 30** - 7 行 / 143 字符
```ruby
p=PokeBattle_Pokemon.new(:ROTOM,5,
$Trainer)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.calcStats
pbAddPokemon(p,1)
pbReceiveItem(:ROTOMCATALOG,1)
```

- **Map 59 / Event 29(EV029) / Page 1 / Cmd 111** - 3 行 / 85 字符
```ruby
pbSetSelfSwitch(28,"A",true) 
pbSetSelfSwitch(30,"A",true) 
completeQuest(:Quest231)
```


### Map060.rxdata . 裂界边廊

- **Map 60 / Event 8(裂界清勉) / Page 1 / Cmd 92** - 4 行 / 80 字符
```ruby
pbAddDependency2(
   @event_id,"清勉",25)
pbRegisterPartner(
  :CHAMPION, "清勉",2)
```

- **Map 60 / Event 8(裂界清勉) / Page 1 / Cmd 108** - 4 行 / 80 字符
```ruby
pbAddDependency2(
   @event_id,"清勉",25)
pbRegisterPartner(
  :CHAMPION, "清勉",2)
```


### Map061.rxdata . 16号道路

- **Map 61 / Event 24(EV024) / Page 1 / Cmd 0** - 5 行 / 117 字符
```ruby
pkmn = pbGenPkmn(:ACCELGOR,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map064.rxdata . 森楠民居

- **Map 64 / Event 5(EV005) / Page 1 / Cmd 1** - 6 行 / 162 字符
```ruby
pbPokemonMart([
:BERRYJUICE,:FRESHWATER,:SODAPOP,
:LAVACOOKIE,:OLDGATEAU,:CASTELIACONE,
:RAGECANDYBAR,:SHALOURSABLE,:BIGMALASADA,
:LUMIOSEGALETTE,:SWEETHEART,
])
```

- **Map 64 / Event 12(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 64 / Event 12(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map066.rxdata . 绯隐牧场

- **Map 66 / Event 12(EV012) / Page 1 / Cmd 1** - 6 行 / 123 字符
```ruby
pbPokemonMart([
:GROWTHMULCH,:DAMPMULCH,
:STABLEMULCH,:GOOEYMULCH,
:REDNECTAR,:YELLOWNECTAR,
:PINKNECTAR,:PURPLENECTAR,
])
```


### Map069.rxdata . 铃兰市

- **Map 69 / Event 18(#f熏香商人) / Page 1 / Cmd 2** - 6 行 / 136 字符
```ruby
pbPokemonMart([
:ODDINCENSE,
:SEAINCENSE,:WAVEINCENSE,
:ROSEINCENSE,:ROCKINCENSE,:LAXINCENSE,
:FULLINCENSE,:PUREINCENSE,:LUCKINCENSE
])
```

- **Map 69 / Event 19(#f树果商人) / Page 1 / Cmd 2** - 9 行 / 290 字符
```ruby
pbPokemonMart([
:CORNNBERRY,:MAGOSTBERRY,:RABUTABERRY,
:NOMELBERRY,:SPELONBERRY,:PAMTREBERRY,
:WATMELBERRY,:DURINBERRY,:BELUEBERRY,
:LIECHIBERRY,:GANLONBERRY,:SALACBERRY,
:PETAYABERRY,:APICOTBERRY,:LANSATBERRY,
:STARFBERRY,:ENIGMABERRY,:MICLEBERRY,
:CUSTAPBERRY,:JABOCABERRY,:ROWAPBERRY
])
```

- **Map 69 / Event 26(#f道具商人) / Page 1 / Cmd 2** - 11 行 / 304 字符
```ruby
pbPokemonMart([
:CHOICEBAND,:CHOICESPECS,
:CHOICESCARF,:LIGHTCLAY,:SACHET,
:BINDINGBAND,:BIGROOT,:SHELLBELL,
:MENTALHERB,:WHITEHERB,:POWERHERB,
:EVERSTONE,:ASSAULTVEST,:SAFETYGOGGLES,
:PROTECTIVEPADS,:TERRAINEXTENDER,
:LUMINOUSMOSS,:WEAKNESSPOLICY,
:PROTECTOR,:STICKYBARB,:DESTINYKNOT,
:BOOSTERENERGY
])
```

- **Map 69 / Event 41(#f薄荷商人) / Page 1 / Cmd 2** - 12 行 / 267 字符
```ruby
pbPokemonMart([
:LONELYMINT,:ADAMANTMINT,
:NAUGHTYMINT,:BRAVEMINT,
:BOLDMINT,:IMPISHMINT,
:LAXMINT,:RELAXEDMINT,
:MODESTMINT,:MILDMINT,
:RASHMINT,:QUIETMINT,
:CALMMINT,:GENTLEMINT,
:CAREFULMINT,:SASSYMINT,
:TIMIDMINT,:HASTYMINT,
:JOLLYMINT,:NAIVEMINT,:SERIOUSMINT
])
```

- **Map 69 / Event 58(#m进化道具商人) / Page 1 / Cmd 2** - 12 行 / 357 字符
```ruby
pbPokemonMart([
:PRETTYFEATHER,:SILKSCARF, :CRACKEDPOT,
:CHIPPEDPOT, :UNREMARKABLETEACUP,
:MASTERPIECETEACUP,:DRAGONSCALE,
:DEEPSEATOOTH,:DEEPSEASCALE,:METALCOAT, 
:PROTECTOR,:ELECTIRIZER,:MAGMARIZER,
:REAPERCLOTH,:GALARICACUFF,:GALARICAWREATH,
:METALALLOY,:AUSPICIOUSARMOR,
:MALICIOUSARMOR,
:KINGSROCK,:RAZORCLAW,
:RAZORFANG,:BLACKAUGURITE,:PEATBLOCK,
])
```

- **Map 69 / Event 61(鲤鱼王大叔) / Page 1 / Cmd 7** - 4 行 / 78 字符
```ruby
p=pbGenPkmn(:FEEBAS,5)
p.iv=[31,31,31,31,31,31]
p.makeShiny
pbAddPokemon(p,1)
```

- **Map 69 / Event 66(#f树果商人) / Page 1 / Cmd 1** - 12 行 / 381 字符
```ruby
pbPokemonMart([
:CHERIBERRY,:CHESTOBERRY,:PECHABERRY,
:RAWSTBERRY,:ASPEARBERRY,:LEPPABERRY,
:ORANBERRY,:PERSIMBERRY,:LUMBERRY,
:SITRUSBERRY,:FIGYBERRY,:WIKIBERRY,
:MAGOBERRY,:AGUAVBERRY,:IAPAPABERRY,
:RAZZBERRY,:BLUKBERRY,:NANABBERRY,
:WEPEARBERRY,:PINAPBERRY,:SALACBERRY,
:PETAYABERRY,:APICOTBERRY,
:POMEGBERRY,:KELPSYBERRY,:QUALOTBERRY,
:HONDEWBERRY,:GREPABERRY,:TAMATOBERRY,
])
```

- **Map 69 / Event 78(#b变隐龙商人) / Page 1 / Cmd 1** - 23 行 / 693 字符
```ruby
setPrice(:HELIXFOSSIL,20000,0)
setPrice(:DOMEFOSSIL,20000,0)
setPrice(:OLDAMBER,20000,0)
setPrice(:ROOTFOSSIL,20000,0)
setPrice(:CLAWFOSSIL,20000,0)
setPrice(:SKULLFOSSIL,20000,0)
setPrice(:ARMORFOSSIL,20000,0)
setPrice(:COVERFOSSIL,20000,0)
setPrice(:PLUMEFOSSIL,20000,0)
setPrice(:JAWFOSSIL,20000,0)
setPrice(:SAILFOSSIL,20000,0)
setPrice(:FOSSILIZEDBIRD,20000,0)
setPrice(:FOSSILIZEDFISH,20000,0)
setPrice(:FOSSILIZEDDRAKE,20000,0)
setPrice(:FOSSILIZEDDINO,20000,0)
pbPokemonMart([
:HELIXFOSSIL,:DOMEFOSSIL,:OLDAMBER,
:ROOTFOSSIL,:CLAWFOSSIL,:SKULLFOSSIL,
:ARMORFOSSIL,:COVERFOSSIL,:PLUMEFOSSIL,
:JAWFOSSIL,:SAILFOSSIL,:FOSSILIZEDBIRD,
:FOSSILIZEDFISH,:FOSSILIZEDDRAKE,
:FOSSILIZEDDINO,
])
```


### Map071.rxdata . 绯雷民居

- **Map 71 / Event 7(#m黎明梦魇) / Page 1 / Cmd 7** - 3 行 / 44 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :ZEBSTRIKA
)
```

- **Map 71 / Event 7(#m黎明梦魇) / Page 1 / Cmd 15** - 9 行 / 178 字符
```ruby
p=pbGenPkmn(:CERULEDGE,60)
p.iv=[31,31,31,31,31,31]
p.setItem(:DAWNSTONE)
p.form = 1
p.makeMale
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("圣谕邪魇"), _I("黎明梦魇"), 1)
```


### Map073.rxdata . 格诺中心

- **Map 73 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 73 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 73 / Event 9(Mart) / Page 1 / Cmd 0** - 6 行 / 94 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:POTION,:SUPERPOTION,
:ANTIDOTE,:FULLHEAL,
:MAXREPEL
])
```


### Map075.rxdata . 格诺民居

- **Map 75 / Event 6(#f艾克莉西娅) / Page 1 / Cmd 10** - 3 行 / 40 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :RALTS
)
```

- **Map 75 / Event 6(#f艾克莉西娅) / Page 1 / Cmd 19** - 6 行 / 130 字符
```ruby
p=pbGenPkmn(:GOTHITA,10)
p.iv=[31,0,31,0,31,31]
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("阿不思"), _I("艾克莉西娅"), 1)
```


### Map076.rxdata . 曦寒山脚

- **Map 76 / Event 5(闪光) / Page 1 / Cmd 0** - 5 行 / 119 字符
```ruby
pkmn = pbGenPkmn(:ESCAVALIER,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map077.rxdata . 苜蓿中心

- **Map 77 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 77 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 77 / Event 9(Mart) / Page 1 / Cmd 0** - 6 行 / 118 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:SUPERPOTION,:HYPERPOTION,
:ANTIDOTE,:FULLHEAL,
:MAXREPEL,:LIFEORB,:EVIOLITE
])
```


### Map078.rxdata . 苜蓿民居

- **Map 78 / Event 20(#m睿睿) / Page 1 / Cmd 5** - 3 行 / 41 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :CROBAT
)
```

- **Map 78 / Event 20(#m睿睿) / Page 1 / Cmd 14** - 8 行 / 162 字符
```ruby
p=pbGenPkmn(:TAILLOW,15)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setItem(:FLAMEORB)
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("斯摩夫"), _I("睿睿"), 1)
```


### Map079.rxdata . 荼蘼中心

- **Map 79 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 79 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 79 / Event 9(Mart) / Page 1 / Cmd 0** - 7 行 / 131 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:POTION,:SUPERPOTION,
:ANTIDOTE,:PARALYZEHEAL,
:AWAKENING,:BURNHEAL,
:ICEHEAL,:SUPERREPEL
])
```


### Map080.rxdata . 荼蘼民居

- **Map 80 / Event 12(#m怪蜀黍) / Page 1 / Cmd 4** - 3 行 / 43 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :GRAVELER
)
```

- **Map 80 / Event 12(#m怪蜀黍) / Page 1 / Cmd 12** - 11 行 / 206 字符
```ruby
p=pbGenPkmn(:MACHAMP,32)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setItem(:BIGROOT)
p.form = 2
p.makeFemale
p.setNature(:LONELY)
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("辣伊辣"), _I("擅于"), 1)
```


### Map081.rxdata . 荼蘼俱乐部

- **Map 81 / Event 3(EV003) / Page 2 / Cmd 14** - 5 行 / 99 字符
```ruby
pbReceiveItem(:TM69,1)
p=pbGenPkmn(:EEVEE,5)
p.iv=[31,31,31,31,31,31]
p.form = 1
pbAddPokemon(p,1)
```

- **Map 81 / Event 18(EV018) / Page 1 / Cmd 1** - 5 行 / 83 字符
```ruby
p=pbGenPkmn(:CATERPIE,5)
p.makeShiny
p.makeSuperShiny
p.ot="383"
pbAddPokemon(p,1)
```


### Map082.rxdata . 茸舒中心

- **Map 82 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 82 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 82 / Event 9(Mart) / Page 1 / Cmd 0** - 5 行 / 90 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:POTION,:SUPERPOTION,
:ANTIDOTE,:FULLHEAL,:REPEL
])
```

- **Map 82 / Event 41(#m红郎) / Page 1 / Cmd 16** - 6 行 / 133 字符
```ruby
p=pbGenPkmn(:KINGLER,32)
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("Kuro"), _I("红郎"),0,126)
```


### Map083.rxdata . 茸舒民居

- **Map 83 / Event 23(EV023) / Page 1 / Cmd 1** - 4 行 / 88 字符
```ruby
pbPokemonMart([
:HEALPOWDER,:XIANGSHAWLPILL,:ENERGYPOWDER,
:ENERGYROOT,:REVIVALHERB,
])
```


### Map085.rxdata . 枫弦中心

- **Map 85 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 85 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 85 / Event 9(Mart) / Page 1 / Cmd 0** - 7 行 / 126 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:POTION,:SUPERPOTION,
:ANTIDOTE,:PARALYZEHEAL,
:AWAKENING,:BURNHEAL,
:ICEHEAL,:REPEL
])
```

- **Map 85 / Event 10(EV010) / Page 1 / Cmd 1** - 4 行 / 86 字符
```ruby
p=pbGenPkmn(:CRYMANDER,15)
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```

- **Map 85 / Event 42(EV042) / Page 1 / Cmd 18** - 7 行 / 148 字符
```ruby
p=pbGenPkmn(:HONEDGE,33)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("Kanzaki"), _I("飒马"),0,420)
```


### Map087.rxdata . 月央中心

- **Map 87 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 87 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 87 / Event 9(Mart) / Page 1 / Cmd 0** - 7 行 / 126 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:POTION,:SUPERPOTION,
:ANTIDOTE,:PARALYZEHEAL,
:AWAKENING,:BURNHEAL,
:ICEHEAL,:REPEL
])
```


### Map090.rxdata . 101号道路

- **Map 90 / Event 15(闪光) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:FURRET,30)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map091.rxdata . 隐龙中心

- **Map 91 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 91 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 91 / Event 9(Mart) / Page 1 / Cmd 0** - 8 行 / 189 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,:ULTRABALL,
:REPEATBALL,:TIMERBALL,
:LUXURYBALL,:DUSKBALL,
:AWAKENING,:SUPERPOTION,
:HYPERPOTION,:REVIVE,:MAXREPEL,
:RAZORFANG,:FOCUSBAND,:FOCUSSASH
])
```

- **Map 91 / Event 43(EV043) / Page 1 / Cmd 15** - 8 行 / 174 字符
```ruby
p=pbGenPkmn(:SWAMPERT,42)
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.setItem(:MYSTICWATER)
p.setItem(:BIGROOT)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("古巨沼"), _I("希元"), 1)
```


### Map092.rxdata . 枫弦民居

- **Map 92 / Event 8(EV008) / Page 1 / Cmd 4** - 5 行 / 76 字符
```ruby
p=pbGenPkmn(:ROWLET,5)
p.form = 2
p.name= "南小鸟"
p.ot="缪斯"
pbAddPokemon(p,1)
```


### Map093.rxdata . 联盟中心

- **Map 93 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 93 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 93 / Event 8(Mart) / Page 1 / Cmd 0** - 11 行 / 248 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:ULTRABALL,:POTION,
:SUPERPOTION,:HYPERPOTION,
:MAXPOTION,:FULLRESTORE,
:REVIVE,:ANTIDOTE,
:PARALYZEHEAL,:AWAKENING,
:BURNHEAL,:ICEHEAL,
:FULLHEAL,:REPEL,:SUPERREPEL,
:MAXREPEL,:ABILITYCAPSULE,:ABILITYPATCH
])
```

- **Map 93 / Event 9(#m精灵转移) / Page 1 / Cmd 9** - 11 行 / 224 字符
```ruby
@ch_cmd=[]
if !pbGetSelfSwitch(9,"A")
 @ch_cmd.push("转移旧版存档")
end
if !pbGetSelfSwitch(9,"B")
 @ch_cmd.push("转移零维存档")
end
@ch_cmd.push("取消")
s="现在可以将其他存档中的部分宝可梦转移到"+
"\n当前存档中，但有几点限制，需要转移吗？"
@ch_ret=pbMessage_ex(s,@ch_cmd,-1)
```


### Map094.rxdata . 花影祭坛

- **Map 94 / Event 3(Counter(3)) / Page 3 / Cmd 419** - 3 行 / 89 字符
```ruby
pbSetSelfSwitch(1,"A",false) 
pbSetSelfSwitch(7,"A",false) 
pbSetSelfSwitch(2,"A",true)
```

- **Map 94 / Event 13(Counter(3)) / Page 2 / Cmd 134** - 3 行 / 68 字符
```ruby
p=pbGenPkmn(:RESKING,50)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 94 / Event 20(Counter(3)) / Page 2 / Cmd 117** - 5 行 / 150 字符
```ruby
pbSetSelfSwitch(15,"A",true) 
pbSetSelfSwitch(16,"A",true) 
pbSetSelfSwitch(17,"A",true) 
pbSetSelfSwitch(18,"A",true) 
pbSetSelfSwitch(19,"A",true)
```


### Map099.rxdata . 绯雷中心

- **Map 99 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 99 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 99 / Event 9(Mart) / Page 1 / Cmd 0** - 9 行 / 182 字符
```ruby
pbPokemonMart([
:GREATBALL,:ULTRABALL,
:REPEATBALL,:TIMERBALL,
:HEALBALL,:POTION,
:SUPERPOTION,:HYPERPOTION,
:REVIVE,:FULLHEAL,
:REPEL,:SUPERREPEL,
:MAXREPEL,:LIGHTCLAY,:GRIPCLAW
])
```


### Map102.rxdata . 百货中心.电梯

- **Map 102 / Event 2(Controls) / Page 1 / Cmd 0** - 9 行 / 208 字符
```ruby
numfloors=6
cur=numfloors-pbGet(10)
pbSet(11,pbMessage(
   _I("要前往哪一层？"),
   [_I("6F"),_I("5F"),_I("4F"),_I("3F"),
   _I("2F"),_I("1F"),_I("Exit")],
   numfloors+1,nil,cur))
t=pbGet(11)
pbSet(11,numfloors-t)
```


### Map103.rxdata . 百货中心.2F

- **Map 103 / Event 5(Cashier top) / Page 1 / Cmd 0** - 8 行 / 154 字符
```ruby
pbPokemonMart([
:POTION,:SUPERPOTION,
:HYPERPOTION,:MAXPOTION,
:FULLRESTORE,:REVIVE,
:ANTIDOTE,:PARALYZEHEAL,
:BURNHEAL,:ICEHEAL,
:AWAKENING,:FULLHEAL
])
```

- **Map 103 / Event 6(Cashier bottom) / Page 1 / Cmd 0** - 7 行 / 129 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:ULTRABALL,:REPEL,:SUPERREPEL,
:MAXREPEL,:GRASSMAIL,
:FLAMEMAIL,:BUBBLEMAIL,
:SPACEMAIL
])
```


### Map104.rxdata . 百货中心.3F

- **Map 104 / Event 5(Cashier) / Page 1 / Cmd 0** - 10 行 / 363 字符
```ruby
pbPokemonMart([
:TR00,:TR01,:TR02,:TR03,:TR04,:TR05,:TR06,
:TR07,:TR08,:TR09,:TR10,:TR11,:TR12,:TR13,
:TR14,:TR15,:TR16,:TR17,:TR18,:TR19,:TR20,
:TR21,:TR22,:TR23,:TR24,:TR25,:TR26,:TR27,
:TR28,:TR29,:TR30,:TR31,:TR32,:TR33,:TR34,
:TR35,:TR36,:TR37,:TR38,:TR39,:TR40,:TR41,
:TR42,:TR43,:TR44,:TR45,:TR46,:TR47,:TR48,
:TR49,:TR50,:TR51,:TR52,:TR53,:TR54,:TR55,
])
```

- **Map 104 / Event 6(Cashier bottom) / Page 1 / Cmd 0** - 7 行 / 192 字符
```ruby
pbPokemonMart([
:HEALBALL,:FASTBALL,:LEVELBALL,
:LUREBALL,:HEAVYBALL,:LOVEBALL,
:FRIENDBALL,:MOONBALL,:NETBALL,
:DIVEBALL,:NESTBALL,:REPEATBALL,
:TIMERBALL,:LUXURYBALL,:DUSKBALL,:QUICKBALL
])
```

- **Map 104 / Event 21(EV021) / Page 1 / Cmd 0** - 12 行 / 397 字符
```ruby
pbPokemonMart([
:TR56,:TR57,:TR58,:TR59,:TR60,:TR61,
:TR62,:TR63,:TR64,:TR65,:TR66,:TR67,
:TR68,:TR69,:TR70,:TR71,:TR72,:TR73,
:TR74,:TR75,:TR76,:TR77,:TR78,:TR79,
:TR80,:TR81,:TR82,:TR83,:TR84,:TR85,
:TR86,:TR87,:TR88,:TR89,:TR90,:TR91,
:TR92,:TR93,:TR94,:TR95,:TR96,:TR97,
:TR98,:TR99,:TR100,:TR101,:TR102,:TR103,
:TR104,:TR105,:TR106,:TR107,:TR108,:TR109,
:TR110,:TR111,:TR112,:TR113,:TR114
])
```


### Map105.rxdata . 绯雷民居

- **Map 105 / Event 10(Move Relearner) / Page 1 / Cmd 15** - 3 行 / 63 字符
```ruby
pbChoosePokemon(1,3,proc{|p|
 pbHasRelearnableMove?(p)
},true)
```


### Map106.rxdata . 时风民居

- **Map 106 / Event 6(EV006) / Page 2 / Cmd 60** - 3 行 / 70 字符
```ruby
p=pbGenPkmn(:STONECALF,15)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```


### Map107.rxdata . 102号道路

- **Map 107 / Event 3(闪光) / Page 1 / Cmd 0** - 5 行 / 117 字符
```ruby
pkmn = pbGenPkmn(:CINCCINO,55)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map110.rxdata . 百货中心.4F

- **Map 110 / Event 5(Cashier) / Page 1 / Cmd 0** - 8 行 / 191 字符
```ruby
pbPokemonMart([
:POKEDOLL,:AIRMAIL,
:TUNNELMAIL,:BLOOMMAIL,:FIRESTONE,
:THUNDERSTONE,:MOONSTONE,:SUNSTONE,
:WATERSTONE,:DUSKSTONE,:DAWNSTONE,
:SHINYSTONE,:LEAFSTONE,:ICESTONE,
:HISUISTONE
])
```

- **Map 110 / Event 6(Cashier) / Page 1 / Cmd 0** - 6 行 / 58 字符
```ruby
pbPokemonMart([
:ETHER,
:MAXETHER,
:ELIXIR,
:MAXELIXIR
])
```


### Map112.rxdata . 瑞乡中心

- **Map 112 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 112 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 112 / Event 13(EV013) / Page 1 / Cmd 15** - 9 行 / 167 字符
```ruby
p=pbGenPkmn(:FURFROU,31)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.form = 2
p.makeFemale
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("水君"), _I("共荣"),0,676)
```


### Map115.rxdata . 绯隐牧场

- **Map 115 / Event 11(Trainer(4)) / Page 1 / Cmd 73** - 3 行 / 88 字符
```ruby
pbSetSelfSwitch(8,"A",true) 
pbSetSelfSwitch(9,"A",true) 
pbSetSelfSwitch(10,"A",true)
```

- **Map 115 / Event 19(EV019) / Page 1 / Cmd 0** - 5 行 / 118 字符
```ruby
pkmn = pbGenPkmn(:SERPERIOR,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 115 / Event 28(EV028) / Page 1 / Cmd 2** - 6 行 / 104 字符
```ruby
p=pbGenPkmn(:SANDSHREW,30)
p.iv=[31,31,31,31,31,31]
p.makeShiny
p.ot= "墨酋"
p.makeMale
pbAddPokemon(p,1)
```


### Map116.rxdata . 百货中心.5F

- **Map 116 / Event 5(Cashier top) / Page 1 / Cmd 0** - 6 行 / 79 字符
```ruby
pbPokemonMart([
:HPUP,:PROTEIN,
:IRON,:CALCIUM,
:ZINC,:CARBOS,
:PPUP,:PPMAX
])
```

- **Map 116 / Event 6(Cashier bottom) / Page 1 / Cmd 0** - 10 行 / 293 字符
```ruby
pbPokemonMart([
:METRONOME,:PUNCHINGGLOVE,
:WISEGLASSES,:SCOPELENS,
:RINGTARGET,:WIDELENS,:ZOOMLENS,
:STICKYBARB,:BLUNDERPOLICY,:EJECTPACK,
:HEAVYDUTYBOOTS,:ROOMSERVICE,:THROATSPRAY,
:UTILITYUMBRELLA,:UPGRADE,:DUBIOUSDISC,
:COVERTCLOAK,:LOADEDDICE,:CLEARAMULET,
:ABILITYSHIELD,:MIRRORHERB,
])
```


### Map118.rxdata . 柊埃联盟

- **Map 118 / Event 7(EV007) / Page 2 / Cmd 212** - 3 行 / 87 字符
```ruby
pbSetSelfSwitch(4,"A",true) 
pbSetSelfSwitch(5,"A",true) 
pbSetSelfSwitch(6,"A",true)
```


### Map119.rxdata . 冠军殿堂

- **Map 119 / Event 1(Hall of Fame autorun) / Page 1 / Cmd 80** - 3 行 / 61 字符
```ruby
for i in $Trainer.pokemonParty
  i.giveRibbon(:CHAMPION)
end
```

- **Map 119 / Event 1(Hall of Fame autorun) / Page 1 / Cmd 94** - 3 行 / 61 字符
```ruby
for i in $Trainer.pokemonParty
  i.giveRibbon(:CHAMPION)
end
```

- **Map 119 / Event 4(EV004) / Page 1 / Cmd 52** - 3 行 / 61 字符
```ruby
for i in $Trainer.pokemonParty
  i.giveRibbon(:CHAMPION)
end
```

- **Map 119 / Event 4(EV004) / Page 1 / Cmd 63** - 3 行 / 61 字符
```ruby
for i in $Trainer.pokemonParty
  i.giveRibbon(:CHAMPION)
end
```


### Map123.rxdata . 沃饶中心

- **Map 123 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 123 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 123 / Event 16(EV016) / Page 2 / Cmd 1** - 9 行 / 248 字符
```ruby
pbPokemonMart([
  :FIRESTONE,:THUNDERSTONE,:WATERSTONE,
  :LEAFSTONE,:MOONSTONE,:SUNSTONE,
  :DUSKSTONE,:DAWNSTONE,:SHINYSTONE,
  :ICESTONE,:OVALSTONE,:HEATROCK,
  :DAMPROCK,:SMOOTHROCK,:ICYROCK,
  :FLOATSTONE,:HARDSTONE,
  :EVERSTONE,:EVIOLITE
])
```


### Map124.rxdata . 铃兰中心

- **Map 124 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 124 / Event 1(Nurse) / Page 2 / Cmd 8** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 124 / Event 9(Mart) / Page 1 / Cmd 0** - 6 行 / 136 字符
```ruby
pbPokemonMart([
:GREATBALL,:ULTRABALL,:TIMERBALL,
:REPEATBALL,:NESTBALL,:DIVEBALL,
:EJECTBUTTON,:ROCKYHELMET,
:SMOKEBALL,:SHEDSHELL,
])
```


### Map128.rxdata . 时风研究所

- **Map 128 / Event 1(#f奥菲利娅) / Page 1 / Cmd 38** - 13 行 / 443 字符
```ruby
arr = [:HELIXFOSSIL,:OMANYTE,
:DOMEFOSSIL,:KABUTO,:OLDAMBER,:AERODACTYL,
:ROOTFOSSIL,:LILEEP,:CLAWFOSSIL,:ANORITH,
:SKULLFOSSIL,:CRANIDOS,
:ARMORFOSSIL,:SHIELDON,
:COVERFOSSIL,:TIRTOUGA,:PLUMEFOSSIL,:ARCHEN,
:JAWFOSSIL,:TYRUNT,:SAILFOSSIL,:AMAURA,
:FOSSILIZEDBIRD,:SHANLEINIAO,
:FOSSILIZEDFISH,:JUELSAIYU,
:FOSSILIZEDDRAKE,:JIANCILONG,
:FOSSILIZEDDINO,:PANGPANGHAISHOU,
:HAMMEROSSIL,:STONAIL,:SCREWOSSIL,:SCRSON]
pbConvertItemToPokemon(9,arr)
```

- **Map 128 / Event 5(苍泽) / Page 5 / Cmd 381** - 9 行 / 214 字符
```ruby
p=pbGenPkmn(:COPPERNAKE,15)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
p=pbGenPkmn(:STRANGEBEAST,15)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
p=pbGenPkmn(:FERROHEAD,15)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 128 / Event 6(EV006) / Page 1 / Cmd 5** - 3 行 / 68 字符
```ruby
p=pbGenPkmn(:TORCHIC,15)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```


### Map132.rxdata . 桃苑森林

- **Map 132 / Event 11(米迦勒) / Page 2 / Cmd 19** - 2 行 / 42 字符
```ruby
cry = pbCryFile(251)
pbSEPlay(cry) if cry
```

- **Map 132 / Event 26(Trainer(3)) / Page 1 / Cmd 43** - 2 行 / 42 字符
```ruby
cry = pbCryFile(251)
pbSEPlay(cry) if cry
```


### Map133.rxdata . 曦寒道馆

- **Map 133 / Event 1(墨云) / Page 1 / Cmd 38** - 3 行 / 75 字符
```ruby
pbReceiveItem(:EXPCANDYXL,5)
pbReceiveItem(:TM04,1)
pbReceiveItem(:TM96,1)
```


### Map134.rxdata . 曦寒山

- **Map 134 / Event 3(EV003) / Page 1 / Cmd 0** - 5 行 / 114 字符
```ruby
pkmn = pbGenPkmn(:GOOMY,20)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map135.rxdata . 海眼洞窟

- **Map 135 / Event 11(阿青) / Page 1 / Cmd 89** - 3 行 / 88 字符
```ruby
pbSetSelfSwitch(10,"A",true)
pbSetSelfSwitch(13,"A",true)
pbSetSelfSwitch(14,"A",true)
```

- **Map 135 / Event 11(阿青) / Page 1 / Cmd 186** - 3 行 / 79 字符
```ruby
pbSetSelfSwitch(1,"A",true) 
pbSetSelfSwitch(12,"A",true) 
pbPokemonFollow(15)
```


### Map136.rxdata . 自行车店

- **Map 136 / Event 7(EV007) / Page 1 / Cmd 14** - 8 行 / 156 字符
```ruby
p=pbGenPkmn(:WOOPER,32)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.form = 1
p.setAbility(2)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("乌波"), _I("早起不能的上班族"), 1)
```


### Map138.rxdata . 清风中心

- **Map 138 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 138 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 138 / Event 9(Mart) / Page 1 / Cmd 0** - 9 行 / 222 字符
```ruby
pbPokemonMart([
  :GREATBALL,:ULTRABALL,    
  :REPEATBALL,:TIMERBALL,    
  :HEALBALL,:FULLRESTORE,   
  :MAXPOTION,:REVIVE,       
  :MAXREVIVE,:FULLHEAL,     
  :MAXREPEL,:LIGHTCLAY,    
  :GRIPCLAW,:ABILITYCAPSULE 
])
```


### Map139.rxdata . 沃饶大厅

- **Map 139 / Event 21(EV021) / Page 1 / Cmd 4** - 8 行 / 145 字符
```ruby
p=pbGenPkmn(:DUCKLETT,30)
p.iv=[31,31,31,31,31,31]
p.setItem(:PRETTYFEATHER)
p.form = 1
p.name= "地才鸭"
p.ot= "jolin"
p.makeMale
pbAddPokemon(p,1)
```


### Map140.rxdata . 尘封山

- **Map 140 / Event 5(蛇蛇) / Page 1 / Cmd 49** - 4 行 / 79 字符
```ruby
pbAddDependency2(
   @event_id,"阿特拉",31)
pbRegisterPartner(
  :ALTESKY, "阿特拉")
```

- **Map 140 / Event 5(蛇蛇) / Page 2 / Cmd 10** - 4 行 / 79 字符
```ruby
pbAddDependency2(
   @event_id,"阿特拉",31)
pbRegisterPartner(
  :ALTESKY, "阿特拉")
```

- **Map 140 / Event 8(EV008) / Page 2 / Cmd 2** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map141.rxdata . 月央警视厅

- **Map 141 / Event 11(蒂纹缇) / Page 1 / Cmd 11** - 4 行 / 118 字符
```ruby
pbReceiveItem(:ELECTRICSEED,1)
pbReceiveItem(:PSYCHICSEED,1)
pbReceiveItem(:MISTYSEED,1)
pbReceiveItem(:GRASSYSEED,1)
```


### Map142.rxdata . 幻谕岛

- **Map 142 / Event 43(Counter(3)) / Page 3 / Cmd 39** - 4 行 / 120 字符
```ruby
pbSetSelfSwitch(39,"A",true) 
pbSetSelfSwitch(40,"A",true)
pbSetSelfSwitch(41,"A",true) 
pbSetSelfSwitch(42,"A",true)
```


### Map145.rxdata . 弥留之塔3f

- **Map 145 / Event 6(剧情) / Page 2 / Cmd 40** - 5 行 / 117 字符
```ruby
pkmn = pbGenPkmn(:SHENYUNQUAN,15)
pkmn.form = 1
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 145 / Event 6(剧情) / Page 3 / Cmd 59** - 9 行 / 198 字符
```ruby
p=pbGenPkmn(:ALTARIA,35)
p.makeShiny
p.setItem(:ALTARIANITE)
p.pbLearnMove(:LOVEPOLLENPUFF)
p.pbLearnMove(:FLY)
p.pbLearnMove(:SAINTMELODYSTORM)
p.iv=[31,31,31,31,31,31]
p.ot="清勉"
pbAddPokemon(p,1)
```


### Map146.rxdata . 弥留之塔4f

- **Map 146 / Event 4(EV004) / Page 1 / Cmd 0** - 5 行 / 113 字符
```ruby
pkmn = pbGenPkmn(:GASTLY,30)
pkmn.makeShiny
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map148.rxdata . 月央餐馆

- **Map 148 / Event 16(npc) / Page 1 / Cmd 3** - 6 行 / 142 字符
```ruby
pbPokemonMart([
:LAVACOOKIE,:OLDGATEAU,:CASTELIACONE,
:RAGECANDYBAR,:SHALOURSABLE,:BIGMALASADA,
:LUMIOSEGALETTE,:SWEETHEART,
:WHIPPEDDREAM
])
```


### Map149.rxdata . 幻谕旅馆

- **Map 149 / Event 16(剑盾支线2) / Page 3 / Cmd 29** - 4 行 / 120 字符
```ruby
pbSetSelfSwitch(13,"A",true) 
pbSetSelfSwitch(14,"A",true)
pbSetSelfSwitch(12,"A",true) 
pbSetSelfSwitch(15,"A",true)
```


### Map150.rxdata . 幻谕塔

- **Map 150 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 150 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 150 / Event 4(Mart) / Page 1 / Cmd 0** - 11 行 / 244 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:ULTRABALL,:POTION,
:SUPERPOTION,:HYPERPOTION,
:MAXPOTION,:FULLRESTORE,
:REVIVE,:ANTIDOTE,
:PARALYZEHEAL,:AWAKENING,
:BURNHEAL,:ICEHEAL,
:FULLHEAL,:ESCAPEROPE,
:REPEL,:SUPERREPEL,:MAXREPEL,:ABILITYPATCH
])
```

- **Map 150 / Event 5(服务员（红）) / Page 2 / Cmd 2** - 4 行 / 122 字符
```ruby
pbReceiveItem(:EXPCANDYXL,8)
pbReceiveItem(:ABILITYPATCH, 2)
pbReceiveItem(:BOTTLECAP,2)
pbReceiveItem(:GOLDBOTTLECAP,1)
```

- **Map 150 / Event 5(服务员（红）) / Page 3 / Cmd 2** - 5 行 / 150 字符
```ruby
pbReceiveItem(:EXPCANDYXL,15)
pbReceiveItem(:ABILITYPATCH,5)
pbReceiveItem(:GOLDBOTTLECAP,8)
pbReceiveItem(:BOTTLECAP,6)
pbReceiveItem(:MASTERBALL,6)
```

- **Map 150 / Event 5(服务员（红）) / Page 4 / Cmd 3** - 5 行 / 152 字符
```ruby
pbReceiveItem(:EXPCANDYXL,20)
pbReceiveItem(:ABILITYPATCH,5)
pbReceiveItem(:GOLDBOTTLECAP,10)
pbReceiveItem(:BOTTLECAP,10)
pbReceiveItem(:MASTERBALL,8)
```

- **Map 150 / Event 5(服务员（红）) / Page 4 / Cmd 21** - 5 行 / 151 字符
```ruby
pbReceiveItem(:EXPCANDYXL,20)
pbReceiveItem(:ABILITYPATCH,5)
pbReceiveItem(:GOLDBOTTLECAP,10)
pbReceiveItem(:BOTTLECAP,8)
pbReceiveItem(:MASTERBALL,8)
```

- **Map 150 / Event 6(服务员（紫）) / Page 2 / Cmd 2** - 4 行 / 122 字符
```ruby
pbReceiveItem(:EXPCANDYXL,10)
pbReceiveItem(:ABILITYPATCH,3)
pbReceiveItem(:MASTERBALL,5)
pbReceiveItem(:GOLDBOTTLECAP,7)
```


### Map152.rxdata . 苜蓿博物馆

- **Map 152 / Event 8(EV008) / Page 1 / Cmd 10** - 8 行 / 154 字符
```ruby
for pkmn in $Trainer.pokemonParty
  if isConst?(pkmn.species,PBSpecies,
              :DEOXYS)
    pkmn.form=0
    pbSet(1,pkmn.name)
    break
  end
end
```

- **Map 152 / Event 8(EV008) / Page 1 / Cmd 22** - 8 行 / 154 字符
```ruby
for pkmn in $Trainer.pokemonParty
  if isConst?(pkmn.species,PBSpecies,
              :DEOXYS)
    pkmn.form=1
    pbSet(1,pkmn.name)
    break
  end
end
```

- **Map 152 / Event 8(EV008) / Page 1 / Cmd 34** - 8 行 / 154 字符
```ruby
for pkmn in $Trainer.pokemonParty
  if isConst?(pkmn.species,PBSpecies,
              :DEOXYS)
    pkmn.form=3
    pbSet(1,pkmn.name)
    break
  end
end
```

- **Map 152 / Event 8(EV008) / Page 1 / Cmd 46** - 8 行 / 154 字符
```ruby
for pkmn in $Trainer.pokemonParty
  if isConst?(pkmn.species,PBSpecies,
              :DEOXYS)
    pkmn.form=2
    pbSet(1,pkmn.name)
    break
  end
end
```


### Map156.rxdata . 曦寒山腰

- **Map 156 / Event 8(EV008) / Page 1 / Cmd 7** - 5 行 / 113 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:POTION,:SUPERPOTION,:HYPERPOTION,
:REVIVE,:FULLHEAL,:REPEL,:SUPERREPEL
])
```

- **Map 156 / Event 14(无限之笛剧情) / Page 1 / Cmd 197** - 4 行 / 115 字符
```ruby
pbSetSelfSwitch(13,"A",true) 
pbSetSelfSwitch(15,"A",true) 
pbSetSelfSwitch(16,"A",true) 
completeQuest(:Quest202)
```

- **Map 156 / Event 19(...) / Page 1 / Cmd 0** - 6 行 / 138 字符
```ruby
pkmn = pbGenPkmn(:HYDREIGON,54)
pkmn.makeShiny
pkmn.makeSuperShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map160.rxdata . 隐林

- **Map 160 / Event 5(EV005) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:VENUSAUR,40)
pkmn.makeShiny
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map161.rxdata . 曦寒山

- **Map 161 / Event 5(EV005) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:EMBOAR,40)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map162.rxdata . 曦寒山

- **Map 162 / Event 6(...) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:WEAVILE,60)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map165.rxdata . 灵诺森民居

- **Map 165 / Event 1(EV001) / Page 1 / Cmd 2** - 4 行 / 77 字符
```ruby
p=pbGenPkmn(:RAICHU,50)
p.iv=[31,31,31,31,31,31]
p.form= 1
pbAddPokemon(p,1)
```

- **Map 165 / Event 8(#f环彩羽) / Page 1 / Cmd 26** - 5 行 / 76 字符
```ruby
p=pbGenPkmn(:EEVEE,25)
p.form = 2
p.name= "丘比"
p.ot="环彩羽"
pbAddPokemon(p,1)
```

- **Map 165 / Event 11(EV011) / Page 1 / Cmd 16** - 7 行 / 158 字符
```ruby
p=pbGenPkmn(:METAGROSS,80)
p.iv=[31,31,31,31,31,31]
p.pbLearnMove(:SHIFTGEAR)
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("铁臂膀"), _I("星今C"), 1)
```


### Map166.rxdata . 海之家

- **Map 166 / Event 11(EV011) / Page 1 / Cmd 3** - 3 行 / 30 字符
```ruby
pbPokemonMart([
:EVEBURGER
])
```

- **Map 166 / Event 16(#f卡琳) / Page 1 / Cmd 17** - 9 行 / 169 字符
```ruby
p=pbGenPkmn(:FURFROU,33)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.form = 6
p.makeFemale
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("基格尔德"), _I("卡琳"),0,676)
```

- **Map 166 / Event 18(#m莫) / Page 1 / Cmd 10** - 3 行 / 42 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :AMBIPOM
)
```

- **Map 166 / Event 18(#m莫) / Page 1 / Cmd 18** - 8 行 / 153 字符
```ruby
p=pbGenPkmn(:HATTERENE,60)
p.iv=[31,31,31,31,31,31]
p.form = 1
p.makeMale
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("莫伊莱"), _I("小莫"), 1)
```


### Map169.rxdata . 103号道路

- **Map 169 / Event 3(Trainer(5)) / Page 1 / Cmd 0** - 3 行 / 85 字符
```ruby
pbTrainerIntro(:SWIMMER2_M)
setBattleRule("double")
pbNoticePlayer(get_character(0))
```


### Map170.rxdata . 伊未中心

- **Map 170 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 170 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 170 / Event 9(EV009) / Page 1 / Cmd 7** - 3 行 / 44 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :CELESTORM
)
```

- **Map 170 / Event 9(EV009) / Page 1 / Cmd 16** - 6 行 / 127 字符
```ruby
p=pbGenPkmn(:FERROSEED,30)
p.iv=[31,0,0,0,0,31]
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("铁荆棘"), _I("荆棘"), 1)
```


### Map171.rxdata . 伊末民居

- **Map 171 / Event 9(EV009) / Page 1 / Cmd 14** - 8 行 / 150 字符
```ruby
p=pbGenPkmn(:POPPLIO,15)
p.iv=[31,0,31,0,31,31]
p.form = 2
p.makeFemale
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("芝士"), _I("伊芙"), 1)
```


### Map172.rxdata . 无名镇民居

- **Map 172 / Event 6(EV006) / Page 1 / Cmd 12** - 7 行 / 139 字符
```ruby
p=pbGenPkmn(:VOLTORB,30)
p.iv=[31,0,31,0,31,31]
p.form = 1
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("哔哩哔哩"), _I("立冬"), 1)
```


### Map173.rxdata . 无名海岛

- **Map 173 / Event 44(EV044) / Page 1 / Cmd 0** - 6 行 / 131 字符
```ruby
pkmn = pbGenPkmn(:GYARADOS,50)
pkmn.battleRank = 2
pkmn.form = 0
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 173 / Event 45(EV045) / Page 1 / Cmd 0** - 5 行 / 119 字符
```ruby
pkmn = pbGenPkmn(:TYPHLOSION,40)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map174.rxdata . 西风海岛

- **Map 174 / Event 1(EV001) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:SCEPTILE,40)
pkmn.makeShiny
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 174 / Event 11(测没对战) / Page 1 / Cmd 0** - 3 行 / 84 字符
```ruby
pbTrainerIntro(:HEXMANIAC)
setBattleRule("double")
pbNoticePlayer(get_character(0))
```

- **Map 174 / Event 11(测没对战) / Page 2 / Cmd 5** - 3 行 / 84 字符
```ruby
pbTrainerIntro(:HEXMANIAC)
setBattleRule("double")
pbNoticePlayer(get_character(0))
```


### Map176.rxdata . 晴云民居

- **Map 176 / Event 6(#m艾尔) / Page 1 / Cmd 15** - 9 行 / 167 字符
```ruby
p=pbGenPkmn(:FURFROU,31)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.form = 1
p.makeFemale
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("梦幻"), _I("艾尔"),0,676)
```


### Map177.rxdata . 无名镇码头

- **Map 177 / Event 1(EV001) / Page 1 / Cmd 12** - 3 行 / 66 字符
```ruby
@ch_cmd.push("取消")
str="要前往哪里？"
@ch_ret=pbMessage_ex(str,@ch_cmd)
```


### Map178.rxdata . 曦寒民居

- **Map 178 / Event 4(#m旭少) / Page 1 / Cmd 7** - 3 行 / 46 字符
```ruby
pbChoosePokemonForTrade(1,2,
 :VOLCANICWORM
)
```

- **Map 178 / Event 4(#m旭少) / Page 1 / Cmd 15** - 9 行 / 173 字符
```ruby
p=pbGenPkmn(:SAMUROTT,55)
p.iv=[31,31,31,31,31,31]
p.form = 2
p.setItem(:SACREDASH)
p.makeMale
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("霍光"), _I("旭少"), 1)
```


### Map179.rxdata . 紫罗洞穴

- **Map 179 / Event 14(EV014) / Page 1 / Cmd 0** - 4 行 / 100 字符
```ruby
pkmn = pbGenPkmn(:GRAVELER,25)
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map180.rxdata . 紫罗洞穴

- **Map 180 / Event 10(EV010) / Page 1 / Cmd 0** - 5 行 / 112 字符
```ruby
pkmn = pbGenPkmn(:GIBLE,25)
pkmn.makeShiny
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map182.rxdata . 月落集团3f

- **Map 182 / Event 4(EV004) / Page 1 / Cmd 14** - 3 行 / 68 字符
```ruby
p=pbGenPkmn(:PORYGON,10)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```


### Map184.rxdata . 月落集团.电梯

- **Map 184 / Event 2(Controls) / Page 1 / Cmd 0** - 9 行 / 188 字符
```ruby
numfloors=4
cur=numfloors-pbGet(10)
pbSet(11,pbMessage(
   _I("要前往哪一层？"),
   [_I("4F"),_I("3F"),
   _I("2F"),_I("1F"),_I("取消")],
   numfloors+1,nil,cur))
t=pbGet(11)
pbSet(11,numfloors-t)
```


### Map185.rxdata . 董事长办公室

- **Map 185 / Event 5(EV005) / Page 1 / Cmd 228** - 3 行 / 61 字符
```ruby
for i in $Trainer.pokemonParty
  i.giveRibbon(:CHAMPION)
end
```

- **Map 185 / Event 5(EV005) / Page 1 / Cmd 240** - 3 行 / 61 字符
```ruby
for i in $Trainer.pokemonParty
  i.giveRibbon(:CHAMPION)
end
```


### Map186.rxdata . 曦寒山

- **Map 186 / Event 20(EV020) / Page 1 / Cmd 6** - 3 行 / 85 字符
```ruby
pbReceiveItem(:EXPCANDYXL,5)
pbReceiveItem(:RARECANDY,2)
pbReceiveItem(:BOTTLECAP,2)
```


### Map188.rxdata . 曦寒中心

- **Map 188 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 188 / Event 1(Nurse) / Page 2 / Cmd 8** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map190.rxdata . 104号道路

- **Map 190 / Event 7(EV007) / Page 1 / Cmd 3** - 6 行 / 108 字符
```ruby
p=pbGenPkmn(:MUNCHLAX,15)
p.form = 1
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.ot="闪雪人小卡比兽刷"
pbAddPokemon(p,1)
```


### Map193.rxdata . 雾绒中心

- **Map 193 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 193 / Event 1(Nurse) / Page 2 / Cmd 8** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map194.rxdata . 雾绒商店

- **Map 194 / Event 2(Mart) / Page 1 / Cmd 1** - 11 行 / 230 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:ULTRABALL,:POTION,
:SUPERPOTION,:HYPERPOTION,
:MAXPOTION,:FULLRESTORE,
:REVIVE,:ANTIDOTE,
:PARALYZEHEAL,:AWAKENING,
:BURNHEAL,:ICEHEAL,
:FULLHEAL,:ESCAPEROPE,
:REPEL,:SUPERREPEL,:MAXREPEL
])
```

- **Map 194 / Event 2(Mart) / Page 1 / Cmd 16** - 11 行 / 217 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:ULTRABALL,:POTION,
:SUPERPOTION,:HYPERPOTION,
:MAXPOTION,:REVIVE,
:ANTIDOTE,:PARALYZEHEAL,
:AWAKENING,:BURNHEAL,
:ICEHEAL,:FULLHEAL,
:ESCAPEROPE,:REPEL,
:SUPERREPEL,:MAXREPEL
])
```

- **Map 194 / Event 2(Mart) / Page 1 / Cmd 31** - 11 行 / 206 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:ULTRABALL,:POTION,
:SUPERPOTION,:HYPERPOTION,
:REVIVE,:ANTIDOTE,
:PARALYZEHEAL,:AWAKENING,
:BURNHEAL,:ICEHEAL,
:FULLHEAL,:ESCAPEROPE,
:REPEL,:SUPERREPEL,
:MAXREPEL
])
```

- **Map 194 / Event 2(Mart) / Page 1 / Cmd 46** - 9 行 / 173 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:POTION,:SUPERPOTION,
:HYPERPOTION,:REVIVE,
:ANTIDOTE,:PARALYZEHEAL,
:AWAKENING,:BURNHEAL,
:ICEHEAL,:ESCAPEROPE,
:REPEL,:SUPERREPEL
])
```

- **Map 194 / Event 2(Mart) / Page 1 / Cmd 59** - 8 行 / 139 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:POTION,:SUPERPOTION,
:ANTIDOTE,:PARALYZEHEAL,
:AWAKENING,:BURNHEAL,
:ICEHEAL,:ESCAPEROPE,
:REPEL
])
```

- **Map 194 / Event 2(Mart) / Page 1 / Cmd 70** - 4 行 / 38 字符
```ruby
pbPokemonMart([
:POKEBALL,
:POTION
])
```


### Map195.rxdata . 106号道路

- **Map 195 / Event 9(EV009) / Page 1 / Cmd 0** - 6 行 / 132 字符
```ruby
pkmn = pbGenPkmn(:NINETALES,50)
pkmn.makeShiny
pkmn.form = 1
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map198.rxdata . 冰绒森林

- **Map 198 / Event 3(EV003) / Page 1 / Cmd 0** - 5 行 / 118 字符
```ruby
pkmn = pbGenPkmn(:INFERNAPE,65)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map199.rxdata . 传召室

- **Map 199 / Event 15(Counter(3)) / Page 2 / Cmd 30** - 3 行 / 69 字符
```ruby
advanceQuestToStage(:Quest33,2) 
pbSetSelfSwitch(3,"A",true) 
pbSave
```

- **Map 199 / Event 18(EV018) / Page 2 / Cmd 104** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(20,"A",true) 
pbSetSelfSwitch(17,"A",true) 
pbSetSelfSwitch(19,"A",true)
```

- **Map 199 / Event 22(EV022) / Page 1 / Cmd 49** - 3 行 / 66 字符
```ruby
@ch_cmd.push("取消")
str="要前往哪里？"
@ch_ret=pbMessage_ex(str,@ch_cmd)
```


### Map201.rxdata . 天空城

- **Map 201 / Event 5(Counter(3)) / Page 1 / Cmd 99** - 5 行 / 127 字符
```ruby
pbSetSelfSwitch(16,"A",true) 
pbSetSelfSwitch(17,"B",true) 
pbSetSelfSwitch(18,"A",true) 
pbSetSelfSwitch(19,"A",true) 
pbSave
```


### Map204.rxdata . 影辞镇

- **Map 204 / Event 7(EV007) / Page 1 / Cmd 2** - 4 行 / 87 字符
```ruby
p=pbGenPkmn(:SPRIGATITO,15)
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```


### Map205.rxdata . 影辞民居

- **Map 205 / Event 1(EV001) / Page 1 / Cmd 3** - 5 行 / 105 字符
```ruby
p=pbGenPkmn(:SUGARDEVOIR,80)
p.iv=[31,31,31,31,31,31]
p.ot="星晶"
pbAddPokemon(p,1)
pbReceiveItem(:SQHL,1)
```

- **Map 205 / Event 2(es) / Page 5 / Cmd 5** - 6 行 / 172 字符
```ruby
$Trainer.party.each_index do |i|
 next if $Trainer.party[i].ot != "ES泽洛"
 $Trainer.party[i].otgender = 0
 $Trainer.party[i].makeShiny
 $Trainer.party[i].makeSuperShiny
end
```

- **Map 205 / Event 3(EV003) / Page 2 / Cmd 3** - 5 行 / 108 字符
```ruby
p=pbGenPkmn(:SUGARDEVOIR,50)
p.iv=[31,31,31,31,31,31]
p.ot="幼芙利特"
pbAddPokemon(p,1)
pbReceiveItem(:WMDCX,1)
```

- **Map 205 / Event 4(EV004) / Page 1 / Cmd 9** - 5 行 / 107 字符
```ruby
p=pbGenPkmn(:SUGARDEVOIR,10)
p.iv=[31,31,31,31,31,31]
p.setItem(:RADIANTSHARD)
p.ot="无限"
pbAddPokemon(p,1)
```

- **Map 205 / Event 6(EV006) / Page 1 / Cmd 4** - 5 行 / 107 字符
```ruby
p=pbGenPkmn(:SUGARDEVOIR,70)
p.iv=[31,31,31,31,31,31]
p.setItem(:SANCTFEATHER)
p.ot="源泉"
pbAddPokemon(p,1)
```


### Map206.rxdata . 影辞公园

- **Map 206 / Event 5(EV005) / Page 2 / Cmd 5** - 3 行 / 39 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :XBPS
)
```

- **Map 206 / Event 5(EV005) / Page 2 / Cmd 18** - 11 行 / 237 字符
```ruby
p=pbGenPkmn(:RAICHU,40)
p.makeShiny
p.pbLearnMove(:SURF)
p.pbLearnMove(:FLY)
p.pbLearnMove(:EXTREMESPEED)
p.pbLearnMove(:VOLTTACKLE)
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("戈罗丘"), _I("百合"), 1)
```

- **Map 206 / Event 8(EV008) / Page 1 / Cmd 1** - 4 行 / 68 字符
```ruby
setPrice(:GOLDBOTTLECAP,80000,0)
pbPokemonMart([
:GOLDBOTTLECAP,
])
```

- **Map 206 / Event 15(EV015) / Page 1 / Cmd 4** - 7 行 / 128 字符
```ruby
p=pbGenPkmn(:EEVEE,25)
p.name= "布布"
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setItem(:SILKSCARF)
p.ot="靖宇rainy"
pbAddPokemon(p,1)
```


### Map207.rxdata . 时隐灵根

- **Map 207 / Event 5(EV005) / Page 1 / Cmd 0** - 5 行 / 119 字符
```ruby
pkmn = pbGenPkmn(:SCREAMTAIL,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map208.rxdata . 110道路

- **Map 208 / Event 5(闪光) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:PETILIL,30)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 208 / Event 9(Trainer(4)) / Page 1 / Cmd 0** - 3 行 / 90 字符
```ruby
pbTrainerIntro(:POKEMONRANGER_M)
setBattleRule("double")
pbNoticePlayer(get_character(0))
```

- **Map 208 / Event 10(永恒之花) / Page 1 / Cmd 0** - 5 行 / 102 字符
```ruby
pkmn = pbGenPkmn(:FLOETTE,30)
pkmn.makeShiny
pkmn.form = 2
pkmn.battleRank = 2
pbFreeWildBattle(pkmn)
```


### Map209.rxdata . 破界外环

- **Map 209 / Event 6(EV006) / Page 1 / Cmd 3** - 4 行 / 80 字符
```ruby
pbAddDependency2(
   @event_id,"清勉",25)
pbRegisterPartner(
  :CHAMPION, "清勉",2)
```

- **Map 209 / Event 11 / Page 1 / Cmd 78** - 3 行 / 88 字符
```ruby
pbSetSelfSwitch(4,"A",true) 
pbSetSelfSwitch(12,"A",true) 
pbSetSelfSwitch(6,"A",true)
```


### Map210.rxdata . 破时幽廊

- **Map 210 / Event 2(奉) / Page 1 / Cmd 1** - 3 行 / 84 字符
```ruby
pbToggleFollowingPokemon("off")
pbTrainerIntro(:SHADOWBOSS)
setBattleRule("double")
```


### Map212.rxdata . 影辞码头

- **Map 212 / Event 1(EV001) / Page 1 / Cmd 16** - 3 行 / 66 字符
```ruby
@ch_cmd.push("取消")
str="要前往哪里？"
@ch_ret=pbMessage_ex(str,@ch_cmd)
```


### Map217.rxdata . 黎明洞穴

- **Map 217 / Event 14(EV014) / Page 1 / Cmd 0** - 5 行 / 118 字符
```ruby
pkmn = pbGenPkmn(:AEGISLASH,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 217 / Event 15(EV015) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:NOIVERN,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 217 / Event 16(EV016) / Page 1 / Cmd 0** - 5 行 / 112 字符
```ruby
pkmn = pbGenPkmn(:MUK,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 217 / Event 17(EV017) / Page 1 / Cmd 12** - 3 行 / 44 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :GOURGEIST
)
```

- **Map 217 / Event 17(EV017) / Page 1 / Cmd 21** - 7 行 / 136 字符
```ruby
p=pbGenPkmn(:MUK,50)
p.iv=[31,31,31,31,31,31]
p.form = 1
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("啊罗嘿"), _I("郁实"), 1)
```


### Map218.rxdata . 黎明洞穴

- **Map 218 / Event 5(EV005) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:DELPHOX,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 218 / Event 16(EV016) / Page 2 / Cmd 265** - 3 行 / 67 字符
```ruby
pbDoubleTrainerBattle(
:JESSIE,"武藏",0,nil,
:JAMES,"小次郎",0,nil,nil)
```

- **Map 218 / Event 16(EV016) / Page 2 / Cmd 325** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(18,"A",true)
pbSetSelfSwitch(19,"A",true)  
pbSetSelfSwitch(20,"A",true)
```


### Map221.rxdata . 花影祭坛

- **Map 221 / Event 5(EV005) / Page 1 / Cmd 191** - 3 行 / 87 字符
```ruby
pbSetSelfSwitch(1,"A",true) 
pbSetSelfSwitch(2,"A",true) 
pbSetSelfSwitch(3,"A",true)
```

- **Map 221 / Event 5(EV005) / Page 1 / Cmd 221** - 3 行 / 74 字符
```ruby
completeQuest(:Quest34)
activateQuest(:Quest35)
activateQuest(:Quest35_1)
```


### Map222.rxdata . 彼方民居

- **Map 222 / Event 1(EV001) / Page 1 / Cmd 2** - 6 行 / 106 字符
```ruby
pbPokemonMart([
:TM15,:TM27,:TM87,:TM94,
:TM63,:TM50,:TM12,:TM41,
:TM20,:TM28,:TM76,:TM64,
:TM72,:TM79
])
```

- **Map 222 / Event 7(EV007) / Page 3 / Cmd 7** - 3 行 / 41 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :HAPPUN
)
```

- **Map 222 / Event 7(EV007) / Page 3 / Cmd 19** - 6 行 / 131 字符
```ruby
p=pbGenPkmn(:ROSERADE,60)
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("Rose"), _I("拾月"), 1)
```


### Map224.rxdata . 红枫雪域

- **Map 224 / Event 3(镜子) / Page 2 / Cmd 3** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 224 / Event 9(EV009) / Page 1 / Cmd 0** - 5 行 / 118 字符
```ruby
pkmn = pbGenPkmn(:CHARIZARD,50)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 224 / Event 11(EV011) / Page 1 / Cmd 5** - 5 行 / 91 字符
```ruby
p=pbGenPkmn(:SNIVY,5)
p.form = 1
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
boss_reward(7)
```


### Map225.rxdata . 心鸣湖泊

- **Map 225 / Event 1(EV001) / Page 1 / Cmd 0** - 4 行 / 96 字符
```ruby
pkmn = pbGenPkmn(:UXIE,50)
pkmn.iv=[31,31,31,31,31,31]
pkmn.battleRank=3
pbFreeWildBattle(pkmn)
```

- **Map 225 / Event 2(EV002) / Page 1 / Cmd 0** - 4 行 / 99 字符
```ruby
pkmn = pbGenPkmn(:MESPRIT,50)
pkmn.iv=[31,31,31,31,31,31]
pkmn.battleRank=3
pbFreeWildBattle(pkmn)
```

- **Map 225 / Event 3(EV003) / Page 1 / Cmd 0** - 4 行 / 97 字符
```ruby
pkmn = pbGenPkmn(:AZELF,50)
pkmn.iv=[31,31,31,31,31,31]
pkmn.battleRank=3
pbFreeWildBattle(pkmn)
```

- **Map 225 / Event 8(竹兰) / Page 1 / Cmd 95** - 4 行 / 79 字符
```ruby
p=pbGenPkmn(:GARCHOMP,80)
p.ot="竹兰"
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```


### Map226.rxdata . 祥云之地

- **Map 226 / Event 12(EV012) / Page 1 / Cmd 0** - 6 行 / 136 字符
```ruby
pkmn = pbGenPkmn(:MILOTIC,50)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.makeSuperShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map227.rxdata . 和荀小径

- **Map 227 / Event 5(EV005) / Page 1 / Cmd 0** - 6 行 / 140 字符
```ruby
pkmn = pbGenPkmn(:SLITHERWING,50)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.makeSuperShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map230.rxdata . 和荀平原

- **Map 230 / Event 22(EV022) / Page 1 / Cmd 156** - 14 行 / 261 字符
```ruby
p=pbGenPkmn(:SUGARDEVOIR,50)
p.ot="ES泽洛"
p.otgender=0
p.iv=[31,31,31,31,31,31]
p.setItem(:TEMPLESCEPTER)
p.calcStats
pbAddPokemon(p)
p=pbGenPkmn(:SUJINRAKU,50)
p.ot="ES泽洛"
p.otgender=0
p.iv=[31,31,31,31,31,31]
p.setItem(:ABYSSSWORD)
p.calcStats
pbAddPokemon(p)
```

- **Map 230 / Event 25(EV025) / Page 1 / Cmd 7** - 5 行 / 90 字符
```ruby
p=pbGenPkmn(:HAXORUS,50)
p.iv=[31,31,31,31,31,31]
p.form = 2
p.makeMale
pbAddPokemon(p,1)
```


### Map233.rxdata . 心魂之地

- **Map 233 / Event 9(EV009) / Page 3 / Cmd 59** - 4 行 / 123 字符
```ruby
pbSetSelfSwitch(11,"A",true)
pbSetSelfSwitch(12,"A",true)  
pbSetSelfSwitch(13,"A",true)
advanceQuestToStage(:Quest221,2)
```


### Map236.rxdata . 神秘洞穴

- **Map 236 / Event 4(铁武者) / Page 2 / Cmd 53** - 4 行 / 81 字符
```ruby
p=pbGenPkmn(:DARKMEWTWO,70)
p.iv=[31,31,31,31,31,31]
p.ot="未悠"
pbAddPokemon(p,1)
```


### Map238.rxdata . 雾绒民居

- **Map 238 / Event 5(EV005) / Page 3 / Cmd 5** - 3 行 / 44 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :PHANTRESS
)
```

- **Map 238 / Event 5(EV005) / Page 3 / Cmd 17** - 8 行 / 161 字符
```ruby
p=pbGenPkmn(:GLACEON,55)
p.iv=[31,31,31,31,31,31]
p.setItem(:LOVEBALL)
p.makeMale
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("阿冰"), _I("洛梦缘"), 1)
```


### Map239.rxdata . 骇浪中心

- **Map 239 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 239 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map240.rxdata . 骇浪民居

- **Map 240 / Event 6(EV006) / Page 1 / Cmd 4** - 10 行 / 167 字符
```ruby
p=pbGenPkmn(:WIGGLYTUFF,55)
p.form = 1
p.name= "索艾尔"
p.ot="四月一日君寻"
pbAddPokemon(p,1)
p=pbGenPkmn(:WIGGLYTUFF,55)
p.form = 2
p.name= "拉古"
p.ot="壹元侑子"
pbAddPokemon(p,1)
```

- **Map 240 / Event 17(EV017) / Page 1 / Cmd 8** - 3 行 / 42 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :SYLVEON
)
```

- **Map 240 / Event 17(EV017) / Page 1 / Cmd 17** - 10 行 / 195 字符
```ruby
p=pbGenPkmn(:FURRET,40)
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.setItem(:SHELLBELL)
p.form = 1
p.makeFemale
p.setNature(:LONELY)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("冰尾"), _I("九天后"), 1)
```

- **Map 240 / Event 20(EV020) / Page 1 / Cmd 4** - 3 行 / 53 字符
```ruby
p=pbGenPkmn(:LITLEO,25)
p.form = 2
pbAddPokemon(p,1)
```


### Map241.rxdata . 茉克岛

- **Map 241 / Event 28(EV028) / Page 1 / Cmd 174** - 3 行 / 93 字符
```ruby
pbSetSelfSwitch(24,"A",true) 
pbSetSelfSwitch(27,"A",true) 
advanceQuestToStage(:Quest39,2)
```


### Map242.rxdata . 厄季斯岛

- **Map 242 / Event 46(风信剧情开始) / Page 2 / Cmd 83** - 3 行 / 70 字符
```ruby
p=pbGenPkmn(:MISMAGIUS,50)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 242 / Event 46(风信剧情开始) / Page 2 / Cmd 157** - 3 行 / 93 字符
```ruby
pbSetSelfSwitch(47,"A",true) 
pbSetSelfSwitch(48,"A",true) 
advanceQuestToStage(:Quest41,2)
```

- **Map 242 / Event 52(朗日) / Page 2 / Cmd 177** - 3 行 / 89 字符
```ruby
pbSetSelfSwitch(49,"A",true) 
pbSetSelfSwitch(52,"B",true)
pbSetSelfSwitch(51,"C",true)
```


### Map243.rxdata . 绯焰岛

- **Map 243 / Event 17(EV017) / Page 1 / Cmd 6** - 4 行 / 79 字符
```ruby
p=pbGenPkmn(:BLAZIKEN,50)
p.iv=[31,31,31,31,31,31]
p.form= 2
pbAddPokemon(p,1)
```

- **Map 243 / Event 28(Counter(4)) / Page 3 / Cmd 72** - 4 行 / 120 字符
```ruby
pbSetSelfSwitch(24,"A",true) 
pbSetSelfSwitch(25,"A",true) 
pbSetSelfSwitch(26,"A",true) 
pbSetSelfSwitch(27,"A",true)
```

- **Map 243 / Event 28(Counter(4)) / Page 3 / Cmd 123** - 3 行 / 94 字符
```ruby
pbSetSelfSwitch(29,"B",true) 
advanceQuestToStage(:Quest42,2) 
pbToggleFollowingPokemon("on")
```


### Map245.rxdata . 茉克道馆

- **Map 245 / Event 7(Trainer(5)) / Page 1 / Cmd 0** - 3 行 / 82 字符
```ruby
pbTrainerIntro(:CUEBALL)
setBattleRule("double")
pbNoticePlayer(get_character(0))
```


### Map246.rxdata . 茉克中心

- **Map 246 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 246 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 246 / Event 9(Mart) / Page 1 / Cmd 0** - 7 行 / 192 字符
```ruby
pbPokemonMart([ 
:HEALBALL,:ULTRABALL,:DREAMBALL,
:LUXURYBALL,:QUICKBALL,
:MAXPOTION,:FULLRESTORE,:REVIVE,
:FULLHEAL,:GOLDSPRAY,:FAIRYGEM,:MISTYSEED,
:MENTALHERB,:WHITEHERB,:ABILITYCAPSULE
])
```

- **Map 246 / Event 26(#f祖丽) / Page 1 / Cmd 15** - 9 行 / 169 字符
```ruby
p=pbGenPkmn(:FURFROU,33)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.form = 4
p.makeFemale
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("阿尔宙斯"), _I("祖丽"),0,676)
```


### Map249.rxdata . 绯雷星泉

- **Map 249 / Event 5(星泉剧情一周目) / Page 1 / Cmd 202** - 5 行 / 115 字符
```ruby
pbSetSelfSwitch(2,"A",true) 
completeQuest(:Quest24)
activateQuest(:Quest25)
pbToggleFollowingPokemon("on")
pbSave
```

- **Map 249 / Event 11(EV011) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:LUXRAY,40)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map251.rxdata . 孤风酒馆

- **Map 251 / Event 1(Nurse) / Page 2 / Cmd 8** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 251 / Event 10(EV010) / Page 4 / Cmd 20** - 3 行 / 65 字符
```ruby
p=pbGenPkmn(:EEVEE,5)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 251 / Event 22(EV022) / Page 1 / Cmd 14** - 9 行 / 169 字符
```ruby
p=pbGenPkmn(:FURFROU,33)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.form = 7
p.makeFemale
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("银伴战兽"), _I("凛霄"),0,676)
```


### Map253.rxdata . 厄季斯民居

- **Map 253 / Event 1(阿尔特) / Page 1 / Cmd 35** - 4 行 / 97 字符
```ruby
pkmn = pbGenPkmn(:GHOST,80)
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 253 / Event 1(阿尔特) / Page 2 / Cmd 12** - 3 行 / 69 字符
```ruby
p=pbGenPkmn(:NOROWAWA,50)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 253 / Event 1(阿尔特) / Page 3 / Cmd 18** - 7 行 / 133 字符
```ruby
p=PokeBattle_Pokemon.new(:SPIRITOMB,25,
$Trainer)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
p.calcStats
pbAddPokemon(p,1)
```


### Map254.rxdata . 厄季斯中心

- **Map 254 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 254 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 254 / Event 9(Mart) / Page 1 / Cmd 0** - 7 行 / 202 字符
```ruby
pbPokemonMart([
:ULTRABALL,:DUSKBALL,:MOONBALL,:QUICKBALL,
:FULLRESTORE,:MAXPOTION,:FULLHEAL,
:GOLDSPRAY,:TWISTEDSPOON,:PSYCHICGEM,
:LIGHTCLAY,:WISEGLASSES,:SMOKEBALL,
:ADRENALINEORB,:ABILITYCAPSULE
])
```

- **Map 254 / Event 25(EV025) / Page 1 / Cmd 14** - 11 行 / 241 字符
```ruby
p=pbGenPkmn(:SIRFETCHD,50)
p.makeShiny
p.pbLearnMove(:SURF)
p.pbLearnMove(:FLY)
p.pbLearnMove(:EXTREMESPEED)
p.pbLearnMove(:VOLTTACKLE)
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("北京烤鸭"), _I("玖肆"), 1)
```

- **Map 254 / Event 26(EV026) / Page 1 / Cmd 14** - 9 行 / 169 字符
```ruby
p=pbGenPkmn(:FURFROU,33)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.form = 3
p.makeFemale
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("土地云"), _I("宁提渊"),0,676)
```


### Map255.rxdata . 绯焰中心

- **Map 255 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 255 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 255 / Event 9(Mart) / Page 1 / Cmd 0** - 7 行 / 187 字符
```ruby
pbPokemonMart([
:ULTRABALL,:QUICKBALL,:HYPERPOTION,
:MAXPOTION,:MAXETHER,:MAXELIXIR,
:MAXREVIVE,:FULLHEAL,:GOLDSPRAY,
:FIRESTONE,:HEATROCK,:FIREGEM,
:FLAMEORB,:LIFEORB,:ABILITYCAPSULE
])
```


### Map259.rxdata . 灵诺森中心

- **Map 259 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 259 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 259 / Event 9(Mart) / Page 1 / Cmd 0** - 6 行 / 133 字符
```ruby
pbPokemonMart([
:FASTBALL,:ULTRABALL,:HYPERPOTION,
:MAXPOTION,:MAXREVIVE,:FULLHEAL,
:MAXREPEL,:DAWNSTONE,:SHINYSTONE,
:PSYCHICGEM
])
```

- **Map 259 / Event 26(EV026) / Page 1 / Cmd 16** - 9 行 / 168 字符
```ruby
p=pbGenPkmn(:FURFROU,33)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.form = 5
p.makeFemale
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("眷恋云"), _I("伽隆"),0,676)
```


### Map264.rxdata . 落英岛

- **Map 264 / Event 30(Counter(4)) / Page 1 / Cmd 181** - 3 行 / 70 字符
```ruby
advanceQuestToStage(:Quest47,2) 
pbSetSelfSwitch(31,"A",true) 
pbSave
```


### Map265.rxdata . 永恒花田

- **Map 265 / Event 2(永恒之花) / Page 2 / Cmd 21** - 4 行 / 80 字符
```ruby
p=pbGenPkmn(:FLOETTE,150)
p.form = 5
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 265 / Event 4(EV004) / Page 1 / Cmd 0** - 4 行 / 89 字符
```ruby
pkmn = pbGenPkmn(:CHIKORITA,18)
pkmn.form = 2
pkmn.battleRank = 2
pbFreeWildBattle(pkmn)
```

- **Map 265 / Event 26(阿青) / Page 1 / Cmd 122** - 4 行 / 116 字符
```ruby
pbRemoveDependencies()
pbAddDependency2(26,"阿青",33)
pbRegisterPartner(:CYAN,"阿青",0)
advanceQuestToStage(:Quest71,3)
```


### Map268.rxdata . 虹兰中心

- **Map 268 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 268 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 268 / Event 9(Mart) / Page 1 / Cmd 0** - 7 行 / 165 字符
```ruby
pbPokemonMart([
:ULTRABALL,:FRIENDBALL,:REPEATBALL,
:QUICKBALL,:HYPERPOTION,
:FULLRESTORE,:MAXREVIVE,:FULLHEAL,
:GOLDSPRAY,:SILKSCARF,:SHELLBELL,
:ABILITYCAPSULE
])
```

- **Map 268 / Event 26(#f晓夜) / Page 1 / Cmd 14** - 9 行 / 167 字符
```ruby
p=pbGenPkmn(:FURFROU,33)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.form = 9
p.makeFemale
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("苍响"), _I("晓夜"),0,676)
```


### Map269.rxdata . 规盈岛

- **Map 269 / Event 42(Counter(5)) / Page 1 / Cmd 140** - 4 行 / 108 字符
```ruby
pbSetSelfSwitch(40,"A",true) 
pbSetSelfSwitch(41,"A",true) 
completeQuest(:Quest48)
activateQuest(:Quest49)
```

- **Map 269 / Event 48(裂缝回来) / Page 2 / Cmd 34** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(51,"A",true) 
pbSetSelfSwitch(52,"A",true) 
pbSetSelfSwitch(45,"B",true)
```

- **Map 269 / Event 48(裂缝回来) / Page 2 / Cmd 193** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(50,"A",true) 
pbSetSelfSwitch(47,"A",true) 
pbSetSelfSwitch(53,"A",true)
```


### Map270.rxdata . 绚丽花店

- **Map 270 / Event 2(EV002) / Page 1 / Cmd 1** - 6 行 / 123 字符
```ruby
pbPokemonMart([
:GROWTHMULCH,:DAMPMULCH,
:STABLEMULCH,:GOOEYMULCH,
:REDNECTAR,:YELLOWNECTAR,
:PINKNECTAR,:PURPLENECTAR,
])
```


### Map271.rxdata . 落英中心

- **Map 271 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 271 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 271 / Event 9(Mart) / Page 1 / Cmd 0** - 8 行 / 144 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:POTION,:SUPERPOTION,
:ANTIDOTE,:PARALYZEHEAL,
:AWAKENING,:BURNHEAL,
:ICEHEAL,:ESCAPEROPE,
:SUPERREPEL
])
```


### Map272.rxdata . 狱怜中心

- **Map 272 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 272 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 272 / Event 9(Mart) / Page 1 / Cmd 0** - 11 行 / 229 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:ULTRABALL,:POTION,
:SUPERPOTION,:HYPERPOTION,
:MAXPOTION,:FULLRESTORE,
:REVIVE,:ANTIDOTE,
:PARALYZEHEAL,:AWAKENING,
:BURNHEAL,:ICEHEAL,
:FULLHEAL,:MAXREVIVE,
:REPEL,:SUPERREPEL,:MAXREPEL
])
```


### Map273.rxdata . 规盈中心

- **Map 273 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 273 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 273 / Event 9(Mart) / Page 1 / Cmd 0** - 11 行 / 229 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:ULTRABALL,:POTION,
:SUPERPOTION,:HYPERPOTION,
:MAXPOTION,:FULLRESTORE,
:REVIVE,:ANTIDOTE,
:PARALYZEHEAL,:AWAKENING,
:BURNHEAL,:ICEHEAL,
:FULLHEAL,:MAXREVIVE,
:REPEL,:SUPERREPEL,:MAXREPEL
])
```


### Map275.rxdata . 绯焰民居

- **Map 275 / Event 9(#m之雨) / Page 1 / Cmd 14** - 9 行 / 169 字符
```ruby
p=pbGenPkmn(:FURFROU,33)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.form = 8
p.makeFemale
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("藏玛然特"), _I("之雨"),0,676)
```


### Map278.rxdata . 规盈俱乐部

- **Map 278 / Event 13(EV013) / Page 1 / Cmd 0** - 8 行 / 247 字符
```ruby
pbPokemonMart([
:DIVEBALL,:LUREBALL,:NETBALL,:WATERSTONE,
:DEEPSEATOOTH,:DEEPSEASCALE,:MYSTICWATER,
:SEAINCENSE,:WAVEINCENSE,:SUPERREPEL,
:MAXREPEL,:HYPERPOTION,:MAXPOTION,
:FULLRESTORE,:FULLHEAL,:REVIVE,
:MAXREVIVE,:HEARTSCALE,:PRETTYFEATHER,
])
```


### Map280.rxdata . 狱怜民居

- **Map 280 / Event 14(EV014) / Page 1 / Cmd 2** - 9 行 / 259 字符
```ruby
pbPokemonMart([
:TR61,:TR62,:TR63,:TR64,:TR65,
:TR66,:TR67,:TR68,:TR69,:TR70,:TR71,
:TR72,:TR73,:TR74,:TR75,:TR76,
:TR77,:TR78,:TR79,:TR80,:TR81,:TR82,
:TR83,:TR84,:TR85,:TR86,:TR87,:TR88,
:TR89,:TR90,:TR91,:TR92,:TR93,:TR94,
:TR95,:TR96,:TR97,:TR98,:TR99
])
```


### Map283.rxdata . 113号道路

- **Map 283 / Event 21(EV021) / Page 1 / Cmd 12** - 3 行 / 44 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :SIRFETCHD
)
```

- **Map 283 / Event 36(EV036) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:SWAMPERT,50)
pkmn.battleRank=2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map284.rxdata . 幻夜之林

- **Map 284 / Event 7(Counter(4)) / Page 1 / Cmd 88** - 4 行 / 82 字符
```ruby
p=pbGenPkmn(:VOLTCAT,15)
p.iv=[31,31,31,31,31,31]
p.ballused=26
pbAddPokemon(p,1)
```

- **Map 284 / Event 13(EV013) / Page 1 / Cmd 2** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1016)
pbSEPlay(cry) if cry
```

- **Map 284 / Event 13(EV013) / Page 1 / Cmd 25** - 5 行 / 96 字符
```ruby
p=pbGenPkmn(:OGERPON,50)
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
p.calcStats
pbAddPokemon(p,1)
```


### Map286.rxdata . 试炼之丘

- **Map 286 / Event 7(EV007) / Page 1 / Cmd 0** - 5 行 / 118 字符
```ruby
pkmn = pbGenPkmn(:DECIDUEYE,40)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map288.rxdata . 静隐路

- **Map 288 / Event 12(Counter(4)) / Page 1 / Cmd 96** - 4 行 / 118 字符
```ruby
pbSetSelfSwitch(7,"A",true) 
pbSetSelfSwitch(6,"A",true) 
pbSetSelfSwitch(10,"A",true) 
pbSetSelfSwitch(11,"A",true)
```

- **Map 288 / Event 12(Counter(4)) / Page 1 / Cmd 103** - 3 行 / 79 字符
```ruby
completeQuest(:Quest21)
activateQuest(:Quest22)
pbToggleFollowingPokemon("on")
```

- **Map 288 / Event 22(草果) / Page 2 / Cmd 7** - 4 行 / 104 字符
```ruby
pkmn = pbGenPkmn(:GRASSFRUIT,40)
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map289.rxdata . 17号道路

- **Map 289 / Event 7(EV007) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:CORSOLA,45)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map291.rxdata . 联盟哨卡

- **Map 291 / Event 7(EV007) / Page 2 / Cmd 3** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map297.rxdata . 深流洞穴

- **Map 297 / Event 5(EV005) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:TOXICROAK,80)
pkmn.battleRank=2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 297 / Event 13(EV013) / Page 1 / Cmd 5** - 4 行 / 123 字符
```ruby
pbRemoveDependencies()
pbAddDependency2(12,"墨云",34)
pbRegisterPartner(:LEADER_Ice,"墨云",2)
advanceQuestToStage(:Quest72, 2)
```


### Map300.rxdata . 114号道路

- **Map 300 / Event 2(EV002) / Page 1 / Cmd 0** - 5 行 / 119 字符
```ruby
pkmn = pbGenPkmn(:FERALIGATR,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map303.rxdata . 夕墨镇

- **Map 303 / Event 37(Counter(3)) / Page 1 / Cmd 154** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(39,"A",true) 
pbSetSelfSwitch(38,"A",true) 
pbSetSelfSwitch(40,"A",true)
```


### Map304.rxdata . 115号道路

- **Map 304 / Event 33(杂鱼b) / Page 2 / Cmd 0** - 5 行 / 119 字符
```ruby
pbRemoveDependencies()
pbRegisterPartner(
  :NNANXIAO, "南晓",3)
pbToggleFollowingPokemon("off")
setBattleRule("double")
```

- **Map 304 / Event 33(杂鱼b) / Page 2 / Cmd 47** - 5 行 / 127 字符
```ruby
pbAddDependency2(
   35,"南晓",26)
advanceQuestToStage(:Quest237,3) 
pbSetSelfSwitch(34,"A",true) 
pbSetSelfSwitch(35,"A",true)
```

- **Map 304 / Event 40(EV040) / Page 1 / Cmd 0** - 5 行 / 119 字符
```ruby
pkmn = pbGenPkmn(:INCINEROAR,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 304 / Event 41(EV041) / Page 1 / Cmd 0** - 3 行 / 52 字符
```ruby
$game_screen.weather(
   
PBFieldWeather::Sun,3,20)
```


### Map306.rxdata . 歌舞森林

- **Map 306 / Event 10(EV010) / Page 1 / Cmd 0** - 5 行 / 118 字符
```ruby
pkmn = pbGenPkmn(:RILLABOOM,50)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 306 / Event 11(EV011) / Page 1 / Cmd 0** - 5 行 / 120 字符
```ruby
pkmn = pbGenPkmn(:MEOWSCARADA,50)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map308.rxdata . 舞墨湖畔

- **Map 308 / Event 6(EV006) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:GALLADE,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map309.rxdata . 117号道路

- **Map 309 / Event 1(EV001) / Page 1 / Cmd 10** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:SNORLAX,75)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map311.rxdata . 莲心湖

- **Map 311 / Event 12(EV012) / Page 1 / Cmd 0** - 5 行 / 117 字符
```ruby
pkmn = pbGenPkmn(:LUDICOLO,60)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map312.rxdata . 118号道路

- **Map 312 / Event 13(EV013) / Page 1 / Cmd 0** - 5 行 / 117 字符
```ruby
pkmn = pbGenPkmn(:LURANTIS,50)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map314.rxdata . 119号道路

- **Map 314 / Event 3(EV003) / Page 1 / Cmd 0** - 5 行 / 119 字符
```ruby
pkmn = pbGenPkmn(:VICTREEBEL,60)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map316.rxdata . 绊风之森

- **Map 316 / Event 13(EV013) / Page 1 / Cmd 0** - 5 行 / 118 字符
```ruby
pkmn = pbGenPkmn(:EXEGGUTOR,60)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map317.rxdata . 缘眠镇

- **Map 317 / Event 25(EV025) / Page 1 / Cmd 5** - 4 行 / 78 字符
```ruby
p=pbGenPkmn(:MAGIKARP,20)
p.iv=[31,0,31,0,31,0]
p.makeShiny
pbAddPokemon(p,1)
```

- **Map 317 / Event 26(EV026) / Page 2 / Cmd 25** - 2 行 / 42 字符
```ruby
cry = pbCryFile(493)
pbSEPlay(cry) if cry
```


### Map321.rxdata . 时空裂缝

- **Map 321 / Event 5(EV005) / Page 2 / Cmd 68** - 3 行 / 87 字符
```ruby
pbSetSelfSwitch(4,"A",true) 
pbSetSelfSwitch(3,"A",true) 
pbSetSelfSwitch(2,"A",true)
```


### Map322.rxdata . 星夜长河

- **Map 322 / Event 15(奈克洛兹玛) / Page 3 / Cmd 1** - 2 行 / 42 字符
```ruby
cry = pbCryFile(800)
pbSEPlay(cry) if cry
```

- **Map 322 / Event 15(奈克洛兹玛) / Page 3 / Cmd 145** - 2 行 / 42 字符
```ruby
cry = pbCryFile(800)
pbSEPlay(cry) if cry
```

- **Map 322 / Event 15(奈克洛兹玛) / Page 4 / Cmd 10** - 3 行 / 69 字符
```ruby
p=pbGenPkmn(:NECROZMA,70)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 322 / Event 15(奈克洛兹玛) / Page 4 / Cmd 45** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(17,"B",true) 
pbSetSelfSwitch(18,"B",true) 
pbSetSelfSwitch(19,"B",true)
```

- **Map 322 / Event 15(奈克洛兹玛) / Page 4 / Cmd 56** - 3 行 / 67 字符
```ruby
p=pbGenPkmn(:COSMOG,35)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 322 / Event 15(奈克洛兹玛) / Page 4 / Cmd 72** - 4 行 / 120 字符
```ruby
pbSetSelfSwitch(22,"B",true) 
pbSetSelfSwitch(23,"B",true) 
pbSetSelfSwitch(18,"C",true) 
pbSetSelfSwitch(19,"C",true)
```

- **Map 322 / Event 16(EV016) / Page 2 / Cmd 122** - 2 行 / 42 字符
```ruby
cry = pbCryFile(800)
pbSEPlay(cry) if cry
```

- **Map 322 / Event 20(EV020) / Page 4 / Cmd 29** - 2 行 / 42 字符
```ruby
cry = pbCryFile(800)
pbSEPlay(cry) if cry
```


### Map326.rxdata . 绯雷游戏城

- **Map 326 / Event 28(Prize vendor left) / Page 1 / Cmd 5** - 11 行 / 254 字符
```ruby
item=[:SMOKEBALL,:MIRACLESEED,
      :CHARCOAL,:MYSTICWATER,
      :YELLOWFLUTE,0][pbGet(1)]
price=[800,1000,1000,1000,1600,0
      ][pbGet(1)]
if item && item!=0
  item=getID(PBItems,item)
end
pbSet(1,item)
pbSet(2,price)
pbSet(3,PBItems.getName(item))
```

- **Map 326 / Event 29(Prize vendor middle) / Page 1 / Cmd 5** - 12 行 / 291 字符
```ruby
item=[:SMEARGLE,:CLEFAIRY,
      :DRATINI,:MARACTUS,
      :VOLCARONA,0][pbGet(1)]
price=[180,500,2800,5500,6666,
       0][pbGet(1)]
lv=[9,8,18,25,26,0][pbGet(1)]
if item && item!=0
  item=getID(PBSpecies,item)
end
pbSet(1,item); pbSet(2,price)
pbSet(3,PBSpecies.getName(item))
pbSet(4,lv)
```

- **Map 326 / Event 30(Prize vendor right) / Page 1 / Cmd 5** - 10 行 / 219 字符
```ruby
item=[:TM54,:TM56,:TM75,
      :TM83,:TM82,0][pbGet(1)]
price=[4000,3500,4000,4500,4000,
       0][pbGet(1)]
if item && item!=0
  item=getID(PBItems,item)
end
pbSet(1,item)
pbSet(2,price)
pbSet(3,PBItems.getName(item))
```


### Map327.rxdata . 原野区·北2区

- **Map 327 / Event 1(EV001) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:GRUMPIG,40)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map328.rxdata . 叹月研究所

- **Map 328 / Event 2(EV002) / Page 1 / Cmd 26** - 5 行 / 97 字符
```ruby
p=pbGenPkmn(:CYNDAQUIL,15)
p.form = 1
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```

- **Map 328 / Event 2(EV002) / Page 1 / Cmd 56** - 5 行 / 96 字符
```ruby
p=pbGenPkmn(:OSHAWOTT,15)
p.form = 1
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```

- **Map 328 / Event 2(EV002) / Page 1 / Cmd 86** - 5 行 / 94 字符
```ruby
p=pbGenPkmn(:ROWLET,15)
p.form = 1
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```


### Map329.rxdata . 夕墨中心

- **Map 329 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 329 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 329 / Event 9(Mart) / Page 1 / Cmd 0** - 9 行 / 209 字符
```ruby
pbPokemonMart([
  :ULTRABALL, :DUSKBALL,
  :QUICKBALL,:TIMERBALL,     
  :HEALBALL,:FULLRESTORE,    
  :MAXPOTION,:MAXREVIVE,   
  :FULLHEAL,:MAXREPEL,     
  :LIGHTCLAY,:WEAKNESSPOLICY, 
  :ROOMSERVICE   
])
```


### Map332.rxdata . 和平希望中心

- **Map 332 / Event 15(EV015) / Page 2 / Cmd 8** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map341.rxdata . 第一治疗室

- **Map 341 / Event 9(EV009) / Page 2 / Cmd 8** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map342.rxdata . 风影中心

- **Map 342 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 342 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 342 / Event 9(Mart) / Page 1 / Cmd 0** - 9 行 / 182 字符
```ruby
pbPokemonMart([
:GREATBALL,:ULTRABALL,
:REPEATBALL,:TIMERBALL,
:HEALBALL,:POTION,
:SUPERPOTION,:HYPERPOTION,
:REVIVE,:FULLHEAL,
:REPEL,:SUPERREPEL,
:MAXREPEL,:LIGHTCLAY,:GRIPCLAW
])
```


### Map343.rxdata . 茶月保护区

- **Map 343 / Event 4(EV004) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:MUDKIP,12)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map344.rxdata . 努力店铺

- **Map 344 / Event 1(EV001) / Page 1 / Cmd 2** - 10 行 / 322 字符
```ruby
pbPokemonMart([
:POMEGBERRY,:KELPSYBERRY,:QUALOTBERRY,
:HONDEWBERRY,:GREPABERRY,:TAMATOBERRY,
:OCCABERRY,:PASSHOBERRY,:WACANBERRY,
:RINDOBERRY,:YACHEBERRY,:CHOPLEBERRY,
:KEBIABERRY,:SHUCABERRY,:COBABERRY,
:PAYAPABERRY,:TANGABERRY,:CHARTIBERRY,
:KASIBBERRY,:HABANBERRY,:COLBURBERRY,
:BABIRIBERRY,:CHILANBERRY,:HOPOBERRY
])
```

- **Map 344 / Event 2(EV002) / Page 1 / Cmd 2** - 5 行 / 105 字符
```ruby
pbPokemonMart([
:MACHOBRACE,:POWERWEIGHT,
:POWERBRACER,:POWERBELT,
:POWERLENS,:POWERBAND,:POWERANKLET
])
```


### Map345.rxdata . 黄沙之地

- **Map 345 / Event 1(EV001) / Page 1 / Cmd 0** - 5 行 / 113 字符
```ruby
pkmn = pbGenPkmn(:FLYGON,80)
pkmn.battleRank=2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 345 / Event 2(EV002) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:MANDIBUZZ,70)
pkmn.battleRank=2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 345 / Event 17(EV017) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:MARACTUS,70)
pkmn.battleRank=2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map346.rxdata . 静隐林

- **Map 346 / Event 15(小剧情) / Page 1 / Cmd 119** - 4 行 / 120 字符
```ruby
pbSetSelfSwitch(11,"B",true) 
pbSetSelfSwitch(12,"B",true) 
pbSetSelfSwitch(16,"B",true) 
pbSetSelfSwitch(14,"A",true)
```


### Map347.rxdata . 伊甸园

- **Map 347 / Event 4(暗夜) / Page 1 / Cmd 17** - 8 行 / 160 字符
```ruby
p=pbGenPkmn(:BRILLIANT,80)
p.ot="暗夜"
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
p=pbGenPkmn(:DARKNIGHT,80)
p.ot="暗夜"
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```


### Map352.rxdata . 作者之家

- **Map 352 / Event 1(EV001) / Page 2 / Cmd 3** - 10 行 / 242 字符
```ruby
p=pbGenPkmn(:CALYREX,100)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
p=pbGenPkmn(:SPECTRIER,100)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
p=pbGenPkmn(:GLASTRIER,100)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
pbReceiveItem(:REINSOFUNITY,1)
```

- **Map 352 / Event 4(EV004) / Page 1 / Cmd 11** - 7 行 / 124 字符
```ruby
p=pbGenPkmn(:PIPLUP,15)
p.name= "小波加"
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setItem(:WATERGEM)
p.ot="夏影"
pbAddPokemon(p,1)
```

- **Map 352 / Event 7(EV007) / Page 1 / Cmd 6** - 9 行 / 216 字符
```ruby
p=pbGenPkmn(:IRONSLASHER,100)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
p=pbGenPkmn(:HAILONGSHA,100)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
p=pbGenPkmn(:TERAPAGOS,100)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 352 / Event 22(EV022) / Page 1 / Cmd 5** - 10 行 / 194 字符
```ruby
p=pbGenPkmn(:VESPIQUEN,45)
p.iv=[31,31,31,31,31,31]
p.makeShiny
p.ot="book boy"
pbAddPokemon(p,1)
p=pbGenPkmn(:SWIRLIX,50)
p.iv=[31,31,31,31,31,31]
p.makeShiny
p.ot="book boy"
pbAddPokemon(p,1)
```

- **Map 352 / Event 23(EV023) / Page 1 / Cmd 6** - 3 行 / 72 字符
```ruby
p=pbGenPkmn(:SKYMONSTER,100)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 352 / Event 24(EV024) / Page 1 / Cmd 1** - 5 行 / 106 字符
```ruby
p=pbGenPkmn(:CHARIZARD,40)
p.iv=[31,31,31,31,31,31]
p.setItem(:CHARIZARDITEG)
p.ot="银羽"
pbAddPokemon(p,1)
```


### Map356.rxdata . 纹渊的家

- **Map 356 / Event 9(EV009) / Page 1 / Cmd 248** - 4 行 / 120 字符
```ruby
pbSetSelfSwitch(10,"B",true) 
pbSetSelfSwitch(11,"B",true) 
pbSetSelfSwitch(13,"B",true) 
pbSetSelfSwitch(14,"B",true)
```

- **Map 356 / Event 9(EV009) / Page 1 / Cmd 403** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(16,"A",true) 
pbSetSelfSwitch(17,"A",true) 
pbSetSelfSwitch(18,"A",true)
```

- **Map 356 / Event 9(EV009) / Page 1 / Cmd 576** - 6 行 / 178 字符
```ruby
pbSetSelfSwitch(7,"A",true) 
pbSetSelfSwitch(8,"A",true) 
pbSetSelfSwitch(10,"B",true) 
pbSetSelfSwitch(16,"B",true) 
pbSetSelfSwitch(17,"B",true) 
pbSetSelfSwitch(18,"B",true)
```


### Map358.rxdata . 古荷中心

- **Map 358 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 358 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 358 / Event 9(Mart) / Page 1 / Cmd 0** - 10 行 / 200 字符
```ruby
pbPokemonMart([
  :ULTRABALL,:DUSKBALL,
  :TIMERBALL,:QUICKBALL,
  :MAXPOTION,:FULLRESTORE,
  :REVIVE,:MAXREVIVE,
  :FULLHEAL,:MAXREPEL,
  :PPUP,:PPMAX,
  :HYPERTRAINING,
  :FOCUSBAND,:WISEGLASSES
])
```


### Map361.rxdata . 索伽家

- **Map 361 / Event 11(EV011) / Page 2 / Cmd 266** - 6 行 / 108 字符
```ruby
p=pbGenPkmn(:DRAGONCLOUD,25)
p.name= "李云龙"
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.ot="索伽"
pbAddPokemon(p,1)
```


### Map362.rxdata . 缘眠中心

- **Map 362 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 362 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map368.rxdata . 彩虹山脉

- **Map 368 / Event 2(EV002) / Page 2 / Cmd 282** - 4 行 / 116 字符
```ruby
pbSetSelfSwitch(3,"A",true) 
pbSetSelfSwitch(6,"A",true) 
pbSetSelfSwitch(7,"A",true) 
pbSetSelfSwitch(9,"A",true)
```


### Map369.rxdata . 暴风之巅

- **Map 369 / Event 1(银色羽毛) / Page 1 / Cmd 3** - 3 行 / 89 字符
```ruby
pbSetSelfSwitch(11,"A",true) 
pbSetSelfSwitch(10,"A",true) 
pbSetSelfSwitch(9,"A",true)
```


### Map371.rxdata . 裂界石窟

- **Map 371 / Event 4(EV004) / Page 2 / Cmd 2** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map372.rxdata . 千夜岛

- **Map 372 / Event 4(Trainer(2)) / Page 1 / Cmd 6** - 4 行 / 78 字符
```ruby
pbAddDependency2(
   @event_id,"索伽",27)
pbRegisterPartner(
  :SUOJIA, "索伽",1)
```

- **Map 372 / Event 4(Trainer(2)) / Page 1 / Cmd 81** - 6 行 / 135 字符
```ruby
pbRemoveDependencies()
pbAddDependency2(
   @event_id,"索伽",27)
pbRegisterPartner(
  :SUOJIA, "索伽",1)
advanceQuestToStage(:Quest238,3)
```

- **Map 372 / Event 11(EV011) / Page 1 / Cmd 0** - 5 行 / 117 字符
```ruby
pkmn = pbGenPkmn(:HARIYAMA,50)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map376.rxdata . 平衡之森

- **Map 376 / Event 7(剧情1) / Page 1 / Cmd 62** - 3 行 / 63 字符
```ruby
activateQuest(:Quest240)
pbToggleFollowingPokemon("on")
pbSave
```

- **Map 376 / Event 12(伊裴尔塔尔结束) / Page 4 / Cmd 204** - 3 行 / 89 字符
```ruby
pbSetSelfSwitch(5,"A",true) 
pbSetSelfSwitch(13,"A",true) 
pbSetSelfSwitch(16,"A",true)
```


### Map381.rxdata . 界隙之核

- **Map 381 / Event 7(EV007) / Page 1 / Cmd 168** - 3 行 / 88 字符
```ruby
pbSetSelfSwitch(8,"A",true) 
pbSetSelfSwitch(9,"A",true) 
pbSetSelfSwitch(10,"A",true)
```

- **Map 381 / Event 7(EV007) / Page 1 / Cmd 415** - 3 行 / 68 字符
```ruby
p=pbGenPkmn(:ZYGARDE,70)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 381 / Event 7(EV007) / Page 3 / Cmd 36** - 3 行 / 94 字符
```ruby
pbSetSelfSwitch(1,"C",true) 
pbSetSelfSwitch(18,"A",true) 
advanceQuestToStage(:Quest226, 3)
```


### Map383.rxdata . 千夜洞穴

- **Map 383 / Event 24(EV024) / Page 1 / Cmd 0** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:AGGRON,50)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map384.rxdata . 微冰洞穴

- **Map 384 / Event 9(EV009) / Page 1 / Cmd 0** - 5 行 / 118 字符
```ruby
pkmn = pbGenPkmn(:ABOMASNOW,60)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map385.rxdata . 千夜隐湖

- **Map 385 / Event 3(EV003) / Page 1 / Cmd 193** - 3 行 / 87 字符
```ruby
pbSetSelfSwitch(4,"A",true)
pbSetSelfSwitch(5,"A",true) 
pbSetSelfSwitch(6,"A",true)
```


### Map386.rxdata . 微冰小经

- **Map 386 / Event 4(EV004) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:WALREIN,60)
pkmn.makeShiny
pkmn.battleRank = 2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map387.rxdata . 格诺小径

- **Map 387 / Event 22(EV022) / Page 1 / Cmd 3** - 4 行 / 78 字符
```ruby
p=pbGenPkmn(:WOOPER,10)
p.form = 0
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```


### Map388.rxdata . 枪之柱

- **Map 388 / Event 5(赤日`望罗) / Page 5 / Cmd 31** - 2 行 / 42 字符
```ruby
cry = pbCryFile(487)
pbSEPlay(cry) if cry
```

- **Map 388 / Event 5(赤日`望罗) / Page 5 / Cmd 49** - 2 行 / 42 字符
```ruby
cry = pbCryFile(487)
pbSEPlay(cry) if cry
```

- **Map 388 / Event 5(赤日`望罗) / Page 5 / Cmd 333** - 4 行 / 122 字符
```ruby
pbSetSelfSwitch(16,"A",false) 
pbSetSelfSwitch(17,"A",false) 
pbSetSelfSwitch(19,"A",true) 
pbSetSelfSwitch(20,"B",true)
```

- **Map 388 / Event 6(EV006) / Page 2 / Cmd 48** - 4 行 / 118 字符
```ruby
pbRemoveDependencies()
pbAddDependency2(4,"望罗",32)
pbRegisterPartner(:VOLO,"望罗",0)
advanceQuestToStage(:Quest230, 3)
```

- **Map 388 / Event 10(EV010) / Page 2 / Cmd 27** - 3 行 / 71 字符
```ruby
cry = pbCryFile(483)
pbSEPlay(cry) if cry
pbSetSelfSwitch(2,"A",true)
```

- **Map 388 / Event 10(EV010) / Page 2 / Cmd 39** - 3 行 / 71 字符
```ruby
cry = pbCryFile(484)
pbSEPlay(cry) if cry
pbSetSelfSwitch(1,"A",true)
```

- **Map 388 / Event 10(EV010) / Page 2 / Cmd 133** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(13,"A",true) 
pbSetSelfSwitch(14,"A",true) 
pbSetSelfSwitch(15,"A",true)
```

- **Map 388 / Event 10(EV010) / Page 2 / Cmd 152** - 4 行 / 84 字符
```ruby
cry = pbCryFile(483)
pbSEPlay(cry) if cry
cry = pbCryFile(484)
pbSEPlay(cry) if cry
```

- **Map 388 / Event 10(EV010) / Page 2 / Cmd 217** - 3 行 / 72 字符
```ruby
cry = pbCryFile(487)
pbSEPlay(cry) if cry
pbSetSelfSwitch(16,"A",true)
```

- **Map 388 / Event 10(EV010) / Page 2 / Cmd 228** - 3 行 / 72 字符
```ruby
cry = pbCryFile(487)
pbSEPlay(cry) if cry
pbSetSelfSwitch(17,"A",true)
```

- **Map 388 / Event 10(EV010) / Page 2 / Cmd 257** - 4 行 / 102 字符
```ruby
setBattleRule("double")
pbRegisterPartner(:VOLO, "望罗")
pbTrainerIntro(:CYRUS)
setBattleRule("double")
```

- **Map 388 / Event 10(EV010) / Page 3 / Cmd 35** - 2 行 / 42 字符
```ruby
cry = pbCryFile(487)
pbSEPlay(cry) if cry
```

- **Map 388 / Event 10(EV010) / Page 3 / Cmd 40** - 4 行 / 116 字符
```ruby
pbSetSelfSwitch(5,"A",true) 
pbSetSelfSwitch(7,"B",true) 
pbSetSelfSwitch(8,"B",true) 
pbSetSelfSwitch(9,"B",true)
```

- **Map 388 / Event 10(EV010) / Page 3 / Cmd 58** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(13,"B",true) 
pbSetSelfSwitch(14,"B",true) 
pbSetSelfSwitch(15,"B",true)
```

- **Map 388 / Event 10(EV010) / Page 3 / Cmd 70** - 4 行 / 84 字符
```ruby
cry = pbCryFile(483)
pbSEPlay(cry) if cry
cry = pbCryFile(487)
pbSEPlay(cry) if cry
```

- **Map 388 / Event 10(EV010) / Page 3 / Cmd 82** - 7 行 / 202 字符
```ruby
pbSetSelfSwitch(25,"A",true) 
pbSetSelfSwitch(4,"A",true) 
pbSetSelfSwitch(5,"B",true) 
pbSetSelfSwitch(11,"A",true) 
pbSetSelfSwitch(12,"A",true) 
pbSetSelfSwitch(17,"A",false) 
pbRemoveDependencies()
```

- **Map 388 / Event 23(EV023) / Page 2 / Cmd 2** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map389.rxdata . 天空祭坛

- **Map 389 / Event 2(希斯娜) / Page 1 / Cmd 21** - 4 行 / 126 字符
```ruby
pbReceiveItem(:VENUSAURITE,1)
pbReceiveItem(:CHARIZARDITEX,1)
pbReceiveItem(:CHARIZARDITEY,1)
pbReceiveItem(:BLASTOISINITE,1)
```

- **Map 389 / Event 5(EV005) / Page 2 / Cmd 354** - 11 行 / 315 字符
```ruby
pbSetSelfSwitch(6,"A",true)
pbSetSelfSwitch(7,"A",true)
pbSetSelfSwitch(8,"A",true)
pbSetSelfSwitch(9,"A",true)
pbSetSelfSwitch(10,"A",true)
pbSetSelfSwitch(11,"A",true)
pbSetSelfSwitch(12,"A",true)
pbSetSelfSwitch(13,"A",true)
pbSetSelfSwitch(14,"A",true)
pbSetSelfSwitch(15,"A",true)
pbSetSelfSwitch(17,"A",true)
```


### Map390.rxdata . 幽寂遗迹

- **Map 390 / Event 11(EV011) / Page 2 / Cmd 29** - 4 行 / 116 字符
```ruby
pbSetSelfSwitch(7,"A",true) 
pbSetSelfSwitch(8,"A",true) 
pbSetSelfSwitch(9,"A",true)
pbSetSelfSwitch(12,"A",true)
```

- **Map 390 / Event 11(EV011) / Page 2 / Cmd 92** - 5 行 / 103 字符
```ruby
pbRegisterPartner(
  :LEADER_Dragon, "未明",2)
pbAddDependency2(
   13,"未明",28)
activateQuest(:Quest217)
```


### Map398.rxdata . 隐龙塔3F

- **Map 398 / Event 9(EV009) / Page 2 / Cmd 45** - 3 行 / 87 字符
```ruby
pbSetSelfSwitch(6,"A",true) 
pbSetSelfSwitch(7,"A",true) 
pbSetSelfSwitch(8,"A",true)
```

- **Map 398 / Event 9(EV009) / Page 3 / Cmd 30** - 3 行 / 87 字符
```ruby
pbSetSelfSwitch(6,"B",true) 
pbSetSelfSwitch(7,"B",true) 
pbSetSelfSwitch(8,"B",true)
```

- **Map 398 / Event 9(EV009) / Page 4 / Cmd 110** - 4 行 / 100 字符
```ruby
setBattleRule("double")
pbRegisterPartner(:N, "N")
pbTrainerIntro(:GHETSIS)
setBattleRule("double")
```

- **Map 398 / Event 9(EV009) / Page 5 / Cmd 63** - 3 行 / 88 字符
```ruby
pbSetSelfSwitch(11,"B",true) 
pbSetSelfSwitch(3,"A",true) 
pbSetSelfSwitch(5,"A",true)
```

- **Map 398 / Event 9(EV009) / Page 5 / Cmd 102** - 5 行 / 131 字符
```ruby
pbSetSelfSwitch(1,"C",true) 
pbSetSelfSwitch(2,"C",true) 
pbSetSelfSwitch(13,"A",true) 
pbRemoveDependencies()
pbPokemonFollow(14)
```


### Map400.rxdata . 陨石

- **Map 400 / Event 1(EV001) / Page 1 / Cmd 5** - 5 行 / 128 字符
```ruby
pkmn = pbGenPkmn(:DEOXYS,100)
pkmn.battleRank=4
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
pbSetSelfSwitch(4,"B",true)
```


### Map403.rxdata . 龙之栖巢

- **Map 403 / Event 1(EV001) / Page 1 / Cmd 38** - 6 行 / 120 字符
```ruby
p=PokeBattle_Pokemon.new(:RAYQUAZA,50,
$Trainer)
p.iv=[31,31,31,31,31,31]
p.setAbility(0)
p.calcStats
pbAddPokemon(p,1)
```

- **Map 403 / Event 1(EV001) / Page 1 / Cmd 147** - 6 行 / 120 字符
```ruby
p=PokeBattle_Pokemon.new(:RAYQUAZA,50,
$Trainer)
p.iv=[31,31,31,31,31,31]
p.setAbility(0)
p.calcStats
pbAddPokemon(p,1)
```


### Map406.rxdata . 坠落遗迹

- **Map 406 / Event 9(EV009) / Page 1 / Cmd 145** - 6 行 / 180 字符
```ruby
pbSetSelfSwitch(13,"A",false) 
pbSetSelfSwitch(12,"A",false) 
pbSetSelfSwitch(4,"C",true) 
pbSetSelfSwitch(5,"C",true) 
pbSetSelfSwitch(11,"A",false) 
pbSetSelfSwitch(8,"A",true)
```


### Map410.rxdata . 甜品屋

- **Map 410 / Event 4(EV004) / Page 1 / Cmd 4** - 10 行 / 274 字符
```ruby
setPrice(:EXPCANDYXS, 500, 0)
setPrice(:EXPCANDYS, 1100, 0)  
setPrice(:EXPCANDYM, 3300, 0)
setPrice(:EXPCANDYL, 5500, 0) 
setPrice(:EXPCANDYXL, 11000, 0)
setPrice(:RARECANDY, 16500, 0)
pbPokemonMart([
:EXPCANDYXS,:EXPCANDYS,:EXPCANDYM,
:EXPCANDYL,:EXPCANDYXL,:RARECANDY
])
```

- **Map 410 / Event 5(EV005) / Page 1 / Cmd 1** - 6 行 / 162 字符
```ruby
pbPokemonMart([
:BERRYJUICE,:FRESHWATER,:SODAPOP,
:LAVACOOKIE,:OLDGATEAU,:CASTELIACONE,
:RAGECANDYBAR,:SHALOURSABLE,:BIGMALASADA,
:LUMIOSEGALETTE,:SWEETHEART,
])
```

- **Map 410 / Event 15(EV015) / Page 1 / Cmd 5** - 12 行 / 154 字符
```ruby
pbPokemonMart([
:SWEETAPPLE,
:TARTAPPLE,
:SYRUPYAPPLE,
:STRAWBERRYSWEET,
:LOVESWEET,
:BERRYSWEET,
:CLOVERSWEET,
:FLOWERSWEET,
:STARSWEET,
:RIBBONSWEET
])
```

- **Map 410 / Event 18(EV018) / Page 2 / Cmd 196** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(16,"A",true) 
pbSetSelfSwitch(17,"A",true) 
pbSetSelfSwitch(19,"A",true)
```


### Map411.rxdata . 翼霄岛

- **Map 411 / Event 12(EV012) / Page 1 / Cmd 83** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(13,"A",true) 
pbSetSelfSwitch(14,"A",true) 
pbSetSelfSwitch(15,"A",true)
```

- **Map 411 / Event 19(EV019) / Page 1 / Cmd 350** - 5 行 / 150 字符
```ruby
pbSetSelfSwitch(20,"A",true) 
pbSetSelfSwitch(21,"A",true) 
pbSetSelfSwitch(22,"A",true) 
pbSetSelfSwitch(23,"A",true) 
pbSetSelfSwitch(24,"B",true)
```

- **Map 411 / Event 19(EV019) / Page 1 / Cmd 379** - 4 行 / 97 字符
```ruby
p=pbGenPkmn(:COSMOG,35)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
pbSetSelfSwitch(25,"A",true)
```


### Map412.rxdata . 春之岛

- **Map 412 / Event 13(Counter(2)) / Page 1 / Cmd 22** - 2 行 / 42 字符
```ruby
cry = pbCryFile(785)
pbSEPlay(cry) if cry
```

- **Map 412 / Event 13(Counter(2)) / Page 1 / Cmd 43** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1001)
pbSEPlay(cry) if cry
```

- **Map 412 / Event 13(Counter(2)) / Page 1 / Cmd 75** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1001)
pbSEPlay(cry) if cry
```

- **Map 412 / Event 13(Counter(2)) / Page 1 / Cmd 98** - 3 行 / 81 字符
```ruby
cry = pbCryFile(1001)
pbSEPlay(cry) if cry
pbRegisterPartner(:TAPUKOKO, "卡璞·鸣鸣")
```


### Map413.rxdata . 夏之岛

- **Map 413 / Event 12(美月) / Page 1 / Cmd 14** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1004)
pbSEPlay(cry) if cry
```

- **Map 413 / Event 12(美月) / Page 1 / Cmd 46** - 2 行 / 42 字符
```ruby
cry = pbCryFile(786)
pbSEPlay(cry) if cry
```

- **Map 413 / Event 13(EV013) / Page 1 / Cmd 33** - 2 行 / 42 字符
```ruby
cry = pbCryFile(786)
pbSEPlay(cry) if cry
```


### Map414.rxdata . 秋之岛

- **Map 414 / Event 12(Counter(4)) / Page 1 / Cmd 26** - 3 行 / 73 字符
```ruby
pbSetSelfSwitch(11,"A",true) 
cry = pbCryFile(1003)
pbSEPlay(cry) if cry
```

- **Map 414 / Event 13(朗日) / Page 1 / Cmd 15** - 3 行 / 74 字符
```ruby
cry = pbCryFile(1003)
pbSEPlay(cry) if cry
pbRegisterPartner(:ELIO, "朗日")
```


### Map415.rxdata . 冬之岛

- **Map 415 / Event 10(库库伊) / Page 1 / Cmd 16** - 3 行 / 76 字符
```ruby
cry = pbCryFile(1002)
pbSEPlay(cry) if cry
pbRegisterPartner(:KUKUI, "库库伊")
```

- **Map 415 / Event 10(库库伊) / Page 1 / Cmd 20** - 3 行 / 63 字符
```ruby
pbDeregisterPartner
cry = pbCryFile(1002)
pbSEPlay(cry) if cry
```

- **Map 415 / Event 11(EV011) / Page 1 / Cmd 77** - 3 行 / 72 字符
```ruby
pbSetSelfSwitch(9,"A",true) 
cry = pbCryFile(1002)
pbSEPlay(cry) if cry
```


### Map419.rxdata . 实验室走廊

- **Map 419 / Event 35(EV035) / Page 1 / Cmd 1** - 4 行 / 120 字符
```ruby
pbSetSelfSwitch(38,"A",true) 
pbSetSelfSwitch(39,"A",true) 
pbSetSelfSwitch(40,"A",true) 
pbSetSelfSwitch(41,"A",true)
```


### Map420.rxdata . 1号升降机

- **Map 420 / Event 2(Controls) / Page 2 / Cmd 0** - 8 行 / 167 字符
```ruby
numfloors=2
cur=numfloors-pbGet(10)
pbSet(11,pbMessage(
   _I("要前往哪一层？"),
   [_I("-1F"),_I("1F"),_I("离开")],
   numfloors+1,nil,cur))
t=pbGet(11)
pbSet(11,numfloors-t)
```


### Map423.rxdata . 2号升降机

- **Map 423 / Event 2(Controls) / Page 1 / Cmd 1** - 8 行 / 168 字符
```ruby
numfloors=2
cur=numfloors-pbGet(10)
pbSet(11,pbMessage(
   _I("要前往哪一层？"),
   [_I("-2F"),_I("-1F"),_I("离开")],
   numfloors+1,nil,cur))
t=pbGet(11)
pbSet(11,numfloors-t)
```


### Map426.rxdata . bgmTEST

- **Map 426 / Event 17(EV017) / Page 1 / Cmd 0** - 3 行 / 79 字符
```ruby
activateQuest(:Quest7)
advanceQuestToStage(:Quest7, 2) 
completeQuest(:Quest6)
```


### Map429.rxdata . 18号道路

- **Map 429 / Event 8(EV008) / Page 1 / Cmd 1** - 5 行 / 115 字符
```ruby
pkmn = pbGenPkmn(:QUAXLY,10)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 429 / Event 12(#f腌咸菜) / Page 1 / Cmd 9** - 7 行 / 147 字符
```ruby
p=pbGenPkmn(:MEWTWO,33)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(1)
p.calcStats
pbStartTrade(pbGet(1), 
p, _I("关注叶昕苍喵"), _I("腌咸菜"),0,209)
```


### Map430.rxdata . 19号道路

- **Map 430 / Event 1(EV001) / Page 1 / Cmd 33** - 5 行 / 110 字符
```ruby
p=pbGenPkmn(:SEEKMANBO,35)
p.name= "乐乐曼波"
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
completeQuest(:Quest236)
```


### Map433.rxdata . 暮煦山

- **Map 433 / Event 30(桑德) / Page 1 / Cmd 40** - 3 行 / 88 字符
```ruby
pbSetSelfSwitch(10,"A",true)
pbSetSelfSwitch(13,"A",true)
pbSetSelfSwitch(14,"A",true)
```

- **Map 433 / Event 34(纹渊) / Page 1 / Cmd 4** - 4 行 / 99 字符
```ruby
pbRemoveDependencies()
pbAddDependency2(34,"纹渊",35)
pbRegisterPartner(
   :ISLANDSGUARDIAN,"纹渊",0)
```


### Map440.rxdata . 尘封工厂

- **Map 440 / Event 7(EV007) / Page 1 / Cmd 23** - 3 行 / 88 字符
```ruby
pbSetSelfSwitch(8,"A",true) 
pbSetSelfSwitch(9,"A",true) 
pbSetSelfSwitch(10,"A",true)
```

- **Map 440 / Event 7(EV007) / Page 1 / Cmd 53** - 10 行 / 291 字符
```ruby
pbSetSelfSwitch(1,"B",true) 
pbSetSelfSwitch(2,"B",true) 
pbSetSelfSwitch(3,"B",true) 
pbSetSelfSwitch(4,"B",true) 
pbSetSelfSwitch(5,"B",true) 
pbSetSelfSwitch(6,"B",true) 
pbSetSelfSwitch(7,"B",true) 
pbSetSelfSwitch(8,"B",true) 
pbSetSelfSwitch(9,"B",true) 
pbSetSelfSwitch(10,"B",true)
```


### Map444.rxdata . 尘封坟

- **Map 444 / Event 5(EV005) / Page 1 / Cmd 11** - 3 行 / 42 字符
```ruby
pbChoosePokemonForTrade(1, 2,
 :CHALLEN
)
```


### Map449.rxdata . 初心镇

- **Map 449 / Event 21(EV021) / Page 1 / Cmd 0** - 2 行 / 47 字符
```ruby
cry = pbCryFile(:SNORLAX)
pbSEPlay(cry) if cry
```


### Map452.rxdata . 初心镇精灵中心

- **Map 452 / Event 3(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 452 / Event 3(Nurse) / Page 2 / Cmd 8** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 452 / Event 7(EV007) / Page 1 / Cmd 0** - 2 行 / 42 字符
```ruby
cry = pbCryFile(113)
pbSEPlay(cry) if cry
```

- **Map 452 / Event 7(EV007) / Page 2 / Cmd 0** - 2 行 / 42 字符
```ruby
cry = pbCryFile(242)
pbSEPlay(cry) if cry
```

- **Map 452 / Event 11(EV011) / Page 1 / Cmd 3** - 4 行 / 86 字符
```ruby
p=pbGenPkmn(:FIREPANDA,15)
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```


### Map453.rxdata . 初心镇友好商店

- **Map 453 / Event 3(店员) / Page 1 / Cmd 1** - 10 行 / 185 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:ULTRABALL,:PREMIERBALL,
:SUPERPOTION,:HYPERPOTION,
:MAXPOTION,:FULLRESTORE,
:REVIVE,
:FULLHEAL,
:TERRAINEXTENDER,
:REPEL,:SUPERREPEL,:MAXREPEL
])
```

- **Map 453 / Event 3(店员) / Page 1 / Cmd 15** - 9 行 / 171 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:ULTRABALL,:PREMIERBALL,
:SUPERPOTION,:HYPERPOTION,
:MAXPOTION,:REVIVE,
:FULLHEAL,
:TERRAINEXTENDER,
:REPEL,:SUPERREPEL,:MAXREPEL
])
```

- **Map 453 / Event 3(店员) / Page 1 / Cmd 28** - 9 行 / 160 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,
:ULTRABALL,:PREMIERBALL,
:SUPERPOTION,:HYPERPOTION,
:REVIVE,:FULLHEAL,
:TERRAINEXTENDER,
:REPEL,:SUPERREPEL,
:MAXREPEL
])
```

- **Map 453 / Event 3(店员) / Page 1 / Cmd 41** - 11 行 / 185 字符
```ruby
pbPokemonMart([
:POKEBALL,
:GREATBALL,
:PREMIERBALL,
:SUPERPOTION,
:HYPERPOTION,:REVIVE,
:ANTIDOTE,:PARALYZEHEAL,
:AWAKENING,:BURNHEAL,
:ICEHEAL,:TERRAINEXTENDER,
:REPEL,:SUPERREPEL
])
```

- **Map 453 / Event 3(店员) / Page 1 / Cmd 55** - 7 行 / 139 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,:PREMIERBALL,
:POTION,:SUPERPOTION,
:ANTIDOTE,:PARALYZEHEAL,
:AWAKENING,:BURNHEAL,
:ICEHEAL,:REPEL
])
```


### Map455.rxdata . 反转世界

- **Map 455 / Event 6(EV006) / Page 2 / Cmd 2** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map457.rxdata . 信心路

- **Map 457 / Event 21(Counter(2)) / Page 1 / Cmd 12** - 4 行 / 100 字符
```ruby
pkmn = pbGenPkmn(:SHOGRANE,15)
pkmn.battleRank=2
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 457 / Event 21(Counter(2)) / Page 1 / Cmd 71** - 5 行 / 115 字符
```ruby
pbSetSelfSwitch(20,"A",true) 
p=pbGenPkmn(:SHOGRANE,15)
p.iv=[31,31,31,31,31,31]
p.setAbility(2)
pbAddPokemon(p,1)
```


### Map458.rxdata . 反转世界

- **Map 458 / Event 5(EV005) / Page 1 / Cmd 0** - 5 行 / 117 字符
```ruby
pkmn = pbGenPkmn(:ARCANINE,100)
pkmn.form = 1
pkmn.battleRank = 3
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map459.rxdata . 反转世界

- **Map 459 / Event 5(EV005) / Page 1 / Cmd 0** - 5 行 / 118 字符
```ruby
pkmn = pbGenPkmn(:ELECTRODE,100)
pkmn.form = 1
pkmn.battleRank = 3
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 459 / Event 6(EV006) / Page 1 / Cmd 0** - 5 行 / 118 字符
```ruby
pkmn = pbGenPkmn(:LILLIGANT,100)
pkmn.form = 1
pkmn.battleRank = 3
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map460.rxdata . 反转世界

- **Map 460 / Event 5(EV005) / Page 1 / Cmd 0** - 5 行 / 116 字符
```ruby
pkmn = pbGenPkmn(:AVALUGG,100)
pkmn.form = 1
pkmn.battleRank = 3
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 460 / Event 6(EV006) / Page 1 / Cmd 0** - 2 行 / 42 字符
```ruby
cry = pbCryFile(487)
pbSEPlay(cry) if cry
```


### Map462.rxdata . 创世之巅

- **Map 462 / Event 1(EV001) / Page 2 / Cmd 4** - 4 行 / 98 字符
```ruby
p=pbGenPkmn(:ARCEUS,100)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
pbReceiveItem(:LEGENDPLATE,1)
```


### Map463.rxdata . 静隐林

- **Map 463 / Event 14(Counter(4)) / Page 1 / Cmd 2** - 2 行 / 42 字符
```ruby
cry = pbCryFile(494)
pbSEPlay(cry) if cry
```


### Map464.rxdata . 被锁的实验室

- **Map 464 / Event 2(EV002) / Page 2 / Cmd 0** - 2 行 / 42 字符
```ruby
cry = pbCryFile(649)
pbSEPlay(cry) if cry
```

- **Map 464 / Event 2(EV002) / Page 2 / Cmd 4** - 12 行 / 271 字符
```ruby
p=PokeBattle_Pokemon.new(:GENESECT,50,
$Trainer)
p.makeShiny
p.iv=[31,31,31,31,31,31]
p.setAbility(0)
p.pbLearnMove(:IRONHEAD)  
p.pbLearnMove(:SHIFTGEAR)     
p.pbLearnMove(:TECHNOSWORD)  
p.pbLearnMove(:STRUGGLEBUG)   
p.setNature(:JOLLY)
p.calcStats
pbAddPokemon(p,1)
```

- **Map 464 / Event 3(EV003) / Page 1 / Cmd 12** - 4 行 / 103 字符
```ruby
pbItemBall(:DOUSEDRIVE,1)
pbItemBall(:SHOCKDRIVE,1)
pbItemBall(:BURNDRIVE,1)
pbItemBall(:CHILLDRIVE,1)
```

- **Map 464 / Event 3(EV003) / Page 1 / Cmd 19** - 3 行 / 71 字符
```ruby
cry = pbCryFile(649)
pbSEPlay(cry) if cry
pbSetSelfSwitch(2,"A",true)
```


### Map468.rxdata . 零区研究所-中厅

- **Map 468 / Event 23(铁斑叶) / Page 2 / Cmd 70** - 4 行 / 118 字符
```ruby
pbSetSelfSwitch(8,"A",true) 
pbSetSelfSwitch(9,"A",true) 
pbSetSelfSwitch(10,"A",true)
pbSetSelfSwitch(11,"A",true)
```


### Map469.rxdata . 零区研究所-前厅

- **Map 469 / Event 11(EV011) / Page 1 / Cmd 83** - 3 行 / 90 字符
```ruby
pbSetSelfSwitch(14,"A",true) 
pbSetSelfSwitch(15,"A",true) 
pbSetSelfSwitch(16,"A",true)
```

- **Map 469 / Event 11(EV011) / Page 2 / Cmd 4** - 7 行 / 217 字符
```ruby
pbSetSelfSwitch(12,"A",false) 
pbSetSelfSwitch(13,"A",false) 
pbSetSelfSwitch(14,"A",false) 
pbSetSelfSwitch(15,"A",false) 
pbSetSelfSwitch(16,"A",false) 
pbSetSelfSwitch(19,"A",false) 
pbSetSelfSwitch(18,"A",false)
```

- **Map 469 / Event 11(EV011) / Page 2 / Cmd 32** - 3 行 / 87 字符
```ruby
pbSetSelfSwitch(4,"A",true) 
pbSetSelfSwitch(5,"A",true) 
pbSetSelfSwitch(6,"A",true)
```


### Map474.rxdata . 和荀小径

- **Map 474 / Event 8(EV008) / Page 1 / Cmd 0** - 6 行 / 139 字符
```ruby
pkmn = pbGenPkmn(:AERODACTYL,50)
pkmn.battleRank = 2
pkmn.makeShiny
pkmn.makeSuperShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```


### Map476.rxdata . 叹月中心

- **Map 476 / Event 1(Nurse) / Page 1 / Cmd 13** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 476 / Event 1(Nurse) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 476 / Event 9(Mart) / Page 1 / Cmd 0** - 9 行 / 222 字符
```ruby
pbPokemonMart([
  :GREATBALL,:ULTRABALL,    
  :REPEATBALL,:TIMERBALL,    
  :HEALBALL,:FULLRESTORE,   
  :MAXPOTION,:REVIVE,       
  :MAXREVIVE,:FULLHEAL,     
  :MAXREPEL,:LIGHTCLAY,    
  :GRIPCLAW,:ABILITYCAPSULE 
])
```


### Map477.rxdata . 暮煦山

- **Map 477 / Event 22(EV022) / Page 1 / Cmd 14** - 2 行 / 42 字符
```ruby
cry = pbCryFile(721)
pbSEPlay(cry) if cry
```

- **Map 477 / Event 22(EV022) / Page 1 / Cmd 24** - 2 行 / 42 字符
```ruby
cry = pbCryFile(721)
pbSEPlay(cry) if cry
```


### Map480.rxdata . 零区研究所-东区

- **Map 480 / Event 7(安希斯) / Page 1 / Cmd 65** - 4 行 / 102 字符
```ruby
pbSetSelfSwitch(9,"B",true) 
p=pbGenPkmn(:IRONBOULDER,150)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 480 / Event 8(EV008) / Page 1 / Cmd 172** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1022)
pbSEPlay(cry) if cry
```

- **Map 480 / Event 9(铁磐岩) / Page 1 / Cmd 0** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1022)
pbSEPlay(cry) if cry
```


### Map481.rxdata . 零区研究所-后室

- **Map 481 / Event 6(安希斯) / Page 1 / Cmd 91** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1008)
pbSEPlay(cry) if cry
```

- **Map 481 / Event 9(密勒顿) / Page 2 / Cmd 112** - 3 行 / 73 字符
```ruby
p=pbGenPkmn(:NEBULACRANE,150)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```


### Map483.rxdata . 迷迭之路

- **Map 483 / Event 80(EV080) / Page 3 / Cmd 1** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1025)
pbSEPlay(cry) if cry
```

- **Map 483 / Event 93(EV093) / Page 1 / Cmd 0** - 5 行 / 114 字符
```ruby
pkmn = pbGenPkmn(:BUNEARY,15)
pkmn.battleRank=2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 483 / Event 94(EV094) / Page 1 / Cmd 0** - 5 行 / 114 字符
```ruby
pkmn = pbGenPkmn(:SUNKERN,15)
pkmn.battleRank=2
pkmn.makeShiny
pkmn.iv=[31,31,31,31,31,31]
pbFreeWildBattle(pkmn)
```

- **Map 483 / Event 109(EV109) / Page 1 / Cmd 81** - 4 行 / 126 字符
```ruby
pbSetSelfSwitch(110, "A", true)
pbSetSelfSwitch(99, "A", true)
pbSetSelfSwitch(80, "C", true)
pbSetSelfSwitch(100, "D", true)
```


### Map484.rxdata . 格诺森泉

- **Map 484 / Event 8(徘徊者A) / Page 2 / Cmd 5** - 3 行 / 87 字符
```ruby
pbSetSelfSwitch(3,"A",true) 
pbSetSelfSwitch(4,"A",true) 
pbSetSelfSwitch(5,"A",true)
```

- **Map 484 / Event 8(徘徊者A) / Page 2 / Cmd 57** - 3 行 / 67 字符
```ruby
p=pbGenPkmn(:KUBFU,150)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```


### Map487.rxdata . 晦木古林

- **Map 487 / Event 11(EV011) / Page 2 / Cmd 23** - 3 行 / 93 字符
```ruby
pbSetSelfSwitch(12,"A",true) 
pbSetSelfSwitch(13,"A",true) 
advanceQuestToStage(:Quest75, 2)
```


### Map489.rxdata . 烬骨火山

- **Map 489 / Event 2(EV002) / Page 1 / Cmd 8** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1020)
pbSEPlay(cry) if cry
```


### Map490.rxdata . 断脊岩台

- **Map 490 / Event 1(EV001) / Page 1 / Cmd 8** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1021)
pbSEPlay(cry) if cry
```


### Map491.rxdata . 烬启之墟

- **Map 491 / Event 1(EV001) / Page 1 / Cmd 10** - 2 行 / 43 字符
```ruby
cry = pbCryFile(1007)
pbSEPlay(cry) if cry
```

- **Map 491 / Event 7(EV007) / Page 1 / Cmd 71** - 3 行 / 73 字符
```ruby
p=pbGenPkmn(:FIREPHOENIX,150)
p.iv=[31,31,31,31,31,31]
pbAddPokemon(p,1)
```

- **Map 491 / Event 7(EV007) / Page 1 / Cmd 227** - 3 行 / 91 字符
```ruby
pbSetSelfSwitch(1,"B",true) 
pbSetSelfSwitch(6,"B",true) 
advanceQuestToStage(:Quest75, 4)
```


### Map497.rxdata . 第二治疗室

- **Map 497 / Event 9(EV009) / Page 2 / Cmd 8** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```


### Map500.rxdata . 康特联盟大厅

- **Map 500 / Event 3(#f护士姐姐) / Page 1 / Cmd 82** - 6 行 / 108 字符
```ruby
count=$Trainer.pokemonCount
for i in 1..count
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 500 / Event 3(#f护士姐姐) / Page 2 / Cmd 7** - 5 行 / 96 字符
```ruby
for i in 1..$Trainer.pokemonCount
  pbSet(6,i)
  pbSEPlay("Battle ball shake")
  pbWait(16)
end
```

- **Map 500 / Event 4(#m常规) / Page 1 / Cmd 0** - 5 行 / 160 字符
```ruby
pbPokemonMart([
:POKEBALL,:GREATBALL,:ULTRABALL,:MAXPOTION,
:FULLRESTORE,:FULLHEAL,:REVIVE,:MAXREVIVE,
:REPEL,:SUPERREPEL,
:MAXREPEL,:DIAMONDREPEL,:EPICREPEL])
```

- **Map 500 / Event 7(#m对战) / Page 1 / Cmd 0** - 14 行 / 545 字符
```ruby
$battle_item = [:FLAMEORB,:TOXICORB,
:CHOICEBAND,:CHOICESPECS,:CHOICESCARF,
:MUSCLEBAND,:WISEGLASSES,:EXPERTBELT,
:LIFEORB,:METRONOME,:SHELLBELL,:ASSAULTVEST,
:MENTALHERB,:WHITEHERB,:POWERHERB,
:MIRRORHERB,:SCOPELENS,:WIDELENS,:ZOOMLENS,
:KINGSROCK,:FOCUSBAND,:FOCUSSASH,
:AIRBALLOON,:ROCKYHELMET,:WEAKNESSPOLICY,
:BLUNDERPOLICY,:EJECTBUTTON,:REDCARD,
:ABILITYSHIELD,:PUNCHINGGLOVE,:CLEARAMULET,
:SAFETYGOGGLES,:PROTECTIVEPADS,:COVERTCLOAK,
:LOADEDDICE,:TERRAINEXTENDER,:BOOSTERENERGY]
$battle_item.push(:MAGICCLOAK)
pbPokemonMart($battle_item)
```

- **Map 500 / Event 8(EV008) / Page 1 / Cmd 0** - 2 行 / 42 字符
```ruby
cry = pbCryFile(113)
pbSEPlay(cry) if cry
```

- **Map 500 / Event 8(EV008) / Page 2 / Cmd 0** - 2 行 / 42 字符
```ruby
cry = pbCryFile(242)
pbSEPlay(cry) if cry
```


### Map506.rxdata . 荣耀殿堂

- **Map 506 / Event 1(Hall of Fame autorun) / Page 1 / Cmd 48** - 3 行 / 61 字符
```ruby
for i in $Trainer.pokemonParty
  i.giveRibbon(:CHAMPION)
end
```

- **Map 506 / Event 1(Hall of Fame autorun) / Page 1 / Cmd 60** - 3 行 / 61 字符
```ruby
for i in $Trainer.pokemonParty
  i.giveRibbon(:CHAMPION)
end
```

- **Map 506 / Event 1(Hall of Fame autorun) / Page 2 / Cmd 43** - 3 行 / 61 字符
```ruby
for i in $Trainer.pokemonParty
  i.giveRibbon(:CHAMPION)
end
```

- **Map 506 / Event 1(Hall of Fame autorun) / Page 2 / Cmd 55** - 3 行 / 61 字符
```ruby
for i in $Trainer.pokemonParty
  i.giveRibbon(:CHAMPION)
end
```


### CommonEvents.rxdata

- **CommonEvent 42 / Event 42(对话（诊断) / Page 1 / Cmd 143** - 3 行 / 94 字符
```ruby
pbSetSelfSwitch(21,"A",true) 
pbSetSelfSwitch(22,"A",true) 
advanceQuestToStage(:Quest235, 6)
```

- **CommonEvent 45 / Event 45(对话（后日谈) / Page 1 / Cmd 38** - 3 行 / 92 字符
```ruby
pbSetSelfSwitch(4, "A", true)
pbSetSelfSwitch(19, "A", true)
pbSetSelfSwitch(20, "A", true)
```

