def goldFinger
  cheatList=[
    "治疗全队", "刷新导航", "添加道具", "全开图鉴", "修改精灵", "金钱最大"
  ]
  cheatList.push("经验储罐")
  if $DEBUG || $game_switches[42]  || $game_switches[197] 
    select = pbMessage(sprintf("请选择需要进行的操作。"),cheatList,-1)
  else
    select = 1
  end
  case select
  when -1
    return
  when 0
    pbHealAll
    pbMessage(sprintf("已治疗全队精灵。"))
  when 1
    if pbConfirmMessage("确定要刷新野生宝可梦分布的导航数据吗？")
      Habitats.setup
      pbMessage(_INTL("已刷新分布导航，可能需要重新进入地图。"))
    end
    return
  when 2
    gfAddItem
  when 3
    if pbConfirmMessage("确定要获得全部图鉴并填充信息吗？\n(这不会获得宝可梦)")
      pbLockDex
      d = pbDexNames
      dexLength = $DEBUG ? d.length : d.length - 1
       for i in 0...dexLength
        $PokemonGlobal.pokedexUnlocked[i] = true
      end
      for sp in 1...PBSpecies.maxValue + 1
        $Trainer.setSeen(sp)
        $Trainer.setOwned(sp)
        Habitats.updateHabitatsForSpecies(sp)
      end
      pbMessage(_INTL("获得全部图鉴并填充信息成功。"))
    end
  when 4
    alterPokemon
  when 5
    $Trainer.money = MAX_MONEY
    pbMessage(_INTL("金钱已设为最大。"))
  when 6
    $Trainer.exp_pot = 0 if !$Trainer.exp_pot
    params = ChooseNumberParams.new
    params.setRange(0,EXP_POT_MAX)
    params.setInitialValue($Trainer.exp_pot)
    params.setCancelValue(0)
    $Trainer.exp_pot = pbMessageChooseNumber(_INTL("设置经验储罐剩余量。"),params)
  end
   goldFinger
end

def add_exp_pot(exp_added = 1, show_msg = false)
  show_msg = show_msg && $PokemonBag.pbHasItem?(:EXPPOT)
  exp_max = EXP_POT_MAX
  $Trainer.exp_pot = 0 if !$Trainer.exp_pot
  if $Trainer.exp_pot + exp_added > exp_max
    exp_added = exp_max - $Trainer.exp_pot
  end
  $Trainer.exp_pot += exp_added
  pbMessage(_INTL("经验储罐累积的经验值增加了{1}点。", exp_added)) if show_msg
end

def gfAddItem
  nameList = [
    "大师球",   "神奇糖果", "全复药", "活力块", "PP极限提升剂",
    "进化道具", "无限喷雾"
  ]
  i = pbMessage(_INTL("请选择需要添加的道具。"), nameList, -1)
  items = [
    :MASTERBALL, :RARECANDY, :FULLRESTORE, :MAXREVIVE, :PPMAX,
     nil,        :INFINITEREPEL
  ]
  if i > -1
    item = items[i]
    if item != nil
      itemName = PBItems.getName(item)
      params = ChooseNumberParams.new
      params.setRange(1, 99)
      params.setDefaultValue(1)
      params.setCancelValue(0)
      qty = 1
      qty = pbMessageChooseNumber(_INTL("请选择要添加的{1}的\n数量(1~99)",
                                  itemName), params) if i <= 4
      return if qty == 0
      if $PokemonBag.pbCanStore?(item, qty)
        $PokemonBag.pbStoreItem(item, qty)
        pbMessage(_INTL("已向背包添加{1}个{2}。", qty, itemName))
      else
        pbMessage(_INTL("背包已经满了。"))
      end
    else
      evoStone = [
        :FIRESTONE, :THUNDERSTONE, :WATERSTONE, :LEAFSTONE,
        :MOONSTONE, :SUNSTONE, :DUSKSTONE, :DAWNSTONE,
        :SHINYSTONE, :ICESTONE, :PRISMSCALE, :OVALSTONE,
        :BLACKAUGURITE, :PEATBLOCK, :HISUISTONE
      ]
      evoStone.each do |stone|
        if $PokemonBag.pbCanStore?(stone, 6)
          $PokemonBag.pbStoreItem(stone, 6)
        else
          stoneName = PBItems.getName(stone)
          pbMessage(_INTL("背包已经满了，无法继续添加{1}。",stoneName))
          break
        end
      end
      pbMessage("已向背包添加进化道具各6个。")
    end
  end
end

def alterPokemon
  if $Trainer.party.length==0
    pbMessage("请领取御三家后使用。")
    return
  end
  pbChoosePokemon(1,3)
  pkmnid=pbGet(1)
  if pkmnid>=0&&pkmnid<=5
    pkmn = $Trainer.party[pkmnid]
    if pkmn.egg?
      alterPokemonEgg(pkmn,pkmnid)
    else
      alterPokemonOption(pkmn,pkmnid)
    end
  end
end

def alterPokemonEgg(pkmn,pkmnid)
  if pbConfirmMessage(_INTL("\\l[1]要快速孵化这只精灵蛋吗？"))
    pkmn.name       = PBSpecies.getName(pkmn.species)
    pkmn.eggsteps   = 0
    pkmn.hatchedMap = 0
    pkmn.obtainMode = 0
    pbMessage(_INTL("\\l[1]{1}从蛋中孵化了出来。",pkmn.name))
  end
end

def alterPokemonOption(pkmn,pkmnid)
  optionList=[
    "治愈精灵", "修改等级", "修改性格", "修改特性", "修改异色",
    "教学招式", "修改亲密", "修改个体", "修改努力"
  ]
  option = pbMessage(_INTL("\\l[1]请选择要进行的修改。"),optionList,-1)
  case option
  when -1
    return
  when 0
    pkmn.heal
    pbMessage(_INTL("\\l[1]{1}恢复了健康。",pkmn.name))
  when 1
    maxlLevels = [16, 25, 32, 36, 42, 48, 58, 63, 85]
    i = [$Trainer.numbadges, 8].min
    max = $game_switches[12] ? 100 : maxlLevels[i]
    params = ChooseNumberParams.new
    params.setRange(1, max)
    params.setDefaultValue(pkmn.level)
    level = pbMessageChooseNumber(_INTL("\\l[1]修改精灵等级(最大{1})。",max),params)
    if level != pkmn.level
      pkmn.level = level
      pkmn.calcStats
      pbMessage(_INTL("\\l[1]{1}已被修改为{2}级。",pkmn.name,pkmn.level))
    end
  when 2
    natureList = []
    (PBNatures.getCount).times do |i|
      statUp   = PBNatures.getStatRaised(i)
      statDown = PBNatures.getStatLowered(i)
      if statUp!=statDown
        text = _INTL("{1} (+{2}, -{3})",PBNatures.getName(i),
           PBStats.getNameBrief(statUp),PBStats.getNameBrief(statDown))
      else
        text = _INTL("{1} (---)",PBNatures.getName(i))
      end
      natureList.push(text)
    end
    oldnature = PBNatures.getName(pkmn.nature)
    newNature = pbMessage(sprintf("\\l[1]请选择要修改的性格。"),natureList,-1)
    if newNature>=0 && newNature<PBNatures.getCount
      pkmn.setNature(newNature)
      pkmn.calcStats
      pbMessage(_INTL("\\l[1]{1}的性格已被修改为{2}。",pkmn.name,PBNatures.getName(pkmn.nature)))
    end
  when 3
    abils = pkmn.getAbilityList
    commands = []
    for i in abils
      commands.push(((i[1]<2) ? "" : "(隐藏)")+PBAbilities.getName(i[0]))
    end
    oldabil = PBAbilities.getName(pkmn.ability)
    cmd = pbMessage(sprintf("\\l[1]请选择要修改的特性。"),commands,-1)
    if cmd>=0 && cmd<abils.length
      pkmn.setAbility(abils[cmd][1])
      pbMessage(_INTL("\\l[1]{1}的特性已被修改为{2}。",pkmn.name,PBAbilities.getName(pkmn.ability)))
    end
  when 4
    shinyChoice = ["普通","异色","超闪"]
    shiny = pbMessage(_INTL("\\l[1]修改{1}是否异色。",pkmn.name),shinyChoice,-1)
    return if shiny == -1
    case shiny
    when 0
      pkmn.makeNotShiny
      pkmn.makeNotSuperShiny
    when 1
      pkmn.makeShiny
      pkmn.makeNotSuperShiny
    when 2
      pkmn.makeShiny
      pkmn.makeSuperShiny
    end
    pbMessage(_INTL("\\l[1]{1}已被修改为{2}。",pkmn.name,shinyChoice[shiny]))
  when 5
     fSpecies = pbGetFSpeciesFromForm(pkmn.species, pkmn.form)
    move = pbChooseValidMoveListForSpecies(fSpecies)
    if move != 0
      movename = PBMoves.getName(move)
      pbLearnMove(pkmn,move)
    end
  when 6
    params = ChooseNumberParams.new
    params.setRange(0,255)
    params.setDefaultValue(pkmn.happiness)
    happiness = pbMessageChooseNumber(_INTL("\\l[1]修改精灵亲密度(0~255)。"),params)
    if happiness != pkmn.happiness
      pkmn.happiness = happiness
      pbMessage(_INTL("\\l[1]{1}的亲密度已被修改为{2}。",pkmn.name,pkmn.happiness))
    end
  when 7
    alterPokemonIV(pkmn)
  when 8
    alterPokemonEV(pkmn)
  end
  alterPokemonOption(pkmn,pkmnid)
end

def alterPokemonIV(pkmn)
  numstats = 6
  ersid = sprintf("0x%08X",pkmn.personalID)
  totaliv = 0
  ivcommands = []
  for i in 0...numstats
    ivcommands.push(PBStats.getName(i)+" (#{pkmn.iv[i]})")
    totaliv += pkmn.iv[i]
  end
  msg = _INTL("修改哪一项个体值？\n总计：{1}/{2} ({3}%)",
      totaliv,numstats*31,100*totaliv/(numstats*31))
  cmd = pbMessage(msg,ivcommands,-1)
  if cmd>=0 && cmd<ivcommands.length
    params = ChooseNumberParams.new
    params.setRange(0,31)
    params.setDefaultValue(pkmn.iv[cmd])
    params.setCancelValue(pkmn.iv[cmd])
    f = pbMessageChooseNumber(_INTL("\\l[1]修改{1}个体值(最大31)。",
        PBStats.getName(cmd)),params)
    if f != pkmn.iv[cmd]
      pkmn.iv[cmd] = f
      pkmn.calcStats
      pbMessage(_INTL("\\l[1]已修改{1}的{2}个体值为{3}。",pkmn.name,PBStats.getName(cmd),f))
      alterPokemonIV(pkmn)
    end
  end
end

def alterPokemonEV(pkmn)
  numstats = 6
  totalev = 0
  evcommands = []
  for i in 0...numstats
    evcommands.push(PBStats.getName(i)+" (#{pkmn.ev[i]})")
    totalev += pkmn.ev[i]
  end
  sum_max = PokeBattle_Pokemon::EV_LIMIT
  one_max = PokeBattle_Pokemon::EV_STAT_LIMIT
  cmd = pbMessage(_INTL("修改哪一项努力值？\n总计：{1}/{2} ({3}%)",
              totalev,sum_max, 100*totalev/sum_max),evcommands,-1)
  if cmd>=0 && cmd<numstats
    params = ChooseNumberParams.new
    upperLimit = 0
    for i in 0...numstats
      upperLimit += pkmn.ev[i] if i!=cmd
    end
    upperLimit = sum_max-upperLimit
    upperLimit = [upperLimit,one_max].min
    thisValue = [pkmn.ev[cmd],upperLimit].min
    params.setRange(0,upperLimit)
    params.setDefaultValue(thisValue)
    params.setCancelValue(thisValue)
    f = pbMessageChooseNumber(_INTL("\\l[1]修改{1}努力值(最大{2})。",
        PBStats.getName(cmd),upperLimit),params)
    if f!=pkmn.ev[cmd]
      pkmn.ev[cmd] = f
      pkmn.calcStats
      pbMessage(_INTL("\\l[1]已修改{1}的{2}努力值为{3}。",pkmn.name,PBStats.getName(cmd),f))
      alterPokemonEV(pkmn)
    end
  end
end

def pbChooseValidMoveListForSpecies(species,defaultMoveID=0)
  cmdwin = pbListWindow([],200)
  commands = []
  moveDefault = 0
  legalMoves = pbGetLegalMoves(species)
  for move in legalMoves
    commands.push([move,PBMoves.getName(move)])
  end
  commands.sort! { |a,b| a[0]<=>b[0] }
  if defaultMoveID>0
    commands.each_with_index do |_item,i|
      moveDefault = i if moveDefault==0 && i[0]==defaultMoveID
    end
  end
  realcommands = []
  for command in commands
    realcommands.push("#{command[1]}")
  end
  ret = pbCommands2(cmdwin,realcommands,-1,moveDefault,true)
  cmdwin.dispose
  return (ret>=0) ? commands[ret][0] : 0
end

