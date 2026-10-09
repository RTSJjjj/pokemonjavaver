class ChainCatching
  
  attr_accessor :species        # 连锁的宝可梦
  attr_accessor :chain_times    # 连锁次数
  attr_accessor :shiny_retries  # 闪率加成
  attr_accessor :iv_guaranteed  # 个体保底
  
  def initialize
    @species = nil
    @chain_times = 0
    @shiny_retries = 0
    @iv_guaranteed = 0
  end
  
end

class PokeBattle_Trainer
  
  alias initialize_chainCatching initialize
  def initialize(name,trainertype)
    initialize_chainCatching(name,trainertype)
    # 初始化连锁
    @chainCatching = ChainCatching.new
  end
  
  def chainCatching
    if !defined?(@chainCatching) || !@chainCatching
      @chainCatching = ChainCatching.new
    end
    return @chainCatching
  end
  
  def debugChain(species,times)
    if !defined?(@chainCatching) || !@chainCatching
      @chainCatching = ChainCatching.new
    end
    @chainCatching.species = getConst(PBSpecies,species)
    @chainCatching.chain_times = times
    @chainCatching.shiny_retries = 64
    @chainCatching.iv_guaranteed = 5
  end
  
  def refreshChain(species)
    if !defined?(@chainCatching) || !@chainCatching
      @chainCatching = ChainCatching.new
    end
    # 如果捉的是正在连锁的宝可梦
    if @chainCatching.species == species
      @chainCatching.chain_times += 1     # 连锁次数+1
    # 否则重新连锁
    else
      @chainCatching.species = species
      @chainCatching.chain_times = 1     # 连锁次数重置为1
    end
    getChain(species)
  end
  
  def getChain(species)
    if !defined?(@chainCatching) || !@chainCatching
      @chainCatching = ChainCatching.new
    end
    # 如果遇到的是正在连锁的宝可梦
    if @chainCatching.species == species
      # 判断连锁次数，获取加成信息
      chain_times = @chainCatching.chain_times
      case chain_times / 10
      #连锁0次~9次
      when 0
        @chainCatching.shiny_retries = 0
        @chainCatching.iv_guaranteed = 0
      #连锁10次~19次
      when 1
        @chainCatching.shiny_retries = 16
        @chainCatching.iv_guaranteed = 1
      #连锁20次~29次
      when 2
        @chainCatching.shiny_retries = 32
        @chainCatching.iv_guaranteed = 2
      #连锁30次~39次
      when 3
        @chainCatching.shiny_retries = 48
        @chainCatching.iv_guaranteed = 3
      #连锁40次~49次
      when 4
        @chainCatching.shiny_retries = 64
        @chainCatching.iv_guaranteed = 4
      #连锁50次~59次
      when 5
        @chainCatching.shiny_retries = 96
        @chainCatching.iv_guaranteed = 5
      #连锁60次及以上
      else
        @chainCatching.shiny_retries = 128
        @chainCatching.iv_guaranteed = 5
      end
    # 否则闪率加成为0
    else
      @chainCatching.shiny_retries = 0
      @chainCatching.iv_guaranteed = 0
    end
  end
  
end

def pbRandomIV(num)
  return [31,31,31,31,31,31] if num>=6
  pkiv=[]
  for i in 0...6
    if i < num
      pkiv.push(31)
    else
      pkiv.push(rand(31))
    end
  end
  pkiv = pkiv.shuffle
  return pkiv
end

# 下面的东西只需要在原本的代码里加两行#---之间的就行了
#=begin
#==============================================================================
# 遭遇率加成
#==============================================================================
class PokemonEncounters
  def pbEncounteredPokemon(enctype,tries=1)
    if enctype<0 || enctype>EncounterTypes::EnctypeChances.length
      raise ArgumentError.new(_INTL("Encounter type out of range"))
    end
    # Get the encounter table
    encList = pbGetEncounterTable(enctype)
    return nil if encList==nil
    chances = EncounterTypes::EnctypeChances[enctype]
    # Static/Magnet Pull prefer wild encounters of certain types, if possible.
    # If they activate, they remove all Pokémon from the encounter table that do
    # not have the type they favor. If none have that type, nothing is changed.
    firstPkmn = $Trainer.firstPokemon
    if firstPkmn && rand(100)<50   # 50% chance of happening
      favoredType = -1
      if isConst?(firstPkmn.ability,PBAbilities,:STATIC) && hasConst?(PBTypes,:ELECTRIC)
        favoredType = getConst(PBTypes,:ELECTRIC)
      elsif isConst?(firstPkmn.ability,PBAbilities,:MAGNETPULL) && hasConst?(PBTypes,:STEEL)
        favoredType = getConst(PBTypes,:STEEL)
      elsif isConst?(firstPkmn.ability,PBAbilities,:FLASHFIRE) && hasConst?(PBTypes,:FIRE)
        favoredType = getConst(PBTypes,:FIRE)
      elsif isConst?(firstPkmn.ability,PBAbilities,:HARVEST) && hasConst?(PBTypes,:GRASS)
        favoredType = getConst(PBTypes,:GRASS)
      elsif isConst?(firstPkmn.ability,PBAbilities,:LIGHTNINGROD) && hasConst?(PBTypes,:ELECTRIC)
        favoredType = getConst(PBTypes,:ELECTRIC)
      elsif isConst?(firstPkmn.ability,PBAbilities,:STORMDRAIN) && hasConst?(PBTypes,:WATER)
        favoredType = getConst(PBTypes,:WATER)
      end
      if favoredType>=0
        newEncList = []
        newChances = []
        speciesData = pbLoadSpeciesData
        for i in 0...encList.length
          t1 = speciesData[encList[i][0]][SpeciesType1]
          t2 = speciesData[encList[i][0]][SpeciesType2]
          next if t1!=favoredType && (!t2 || t2!=favoredType)
          newEncList.push(encList[i])
          newChances.push(chances[i])
        end
        if newEncList.length>0
          encList = newEncList
          chances = newChances
        end
      end
    end
    #------------------------------------------------------------
    # 连锁同种族概率
    # 0~4次:   无增益
    # 5~9次:   增加5%概率
    # ......
    # 55~59次: 增加55%概率
    # 60次+:   增加60%概率
    chainRate = [60, $Trainer.chainCatching.chain_times].min
    if rand(100) < chainRate
      newEncList = []
      newChances = []
      speciesData = pbLoadSpeciesData
      for i in 0...encList.length
        species = encList[i][0]
        next if $Trainer.chainCatching.species != species
        newEncList.push(encList[i])
        newChances.push(chances[i])
      end
      if newEncList.length>0
        encList = newEncList
        chances = newChances
      end
    end
    #------------------------------------------------------------
    # Calculate the total probability value
    chanceTotal = 0
    chances.each { |a| chanceTotal += a }
    # Choose a random entry in the encounter table based on entry weights
    rnd = 0
    tries.times do
      r = rand(chanceTotal)
      rnd = r if rnd<r
    end
    chance = 0
    chosenPkmn = 0   # Index of the chosen entry
    for i in 0...chances.length
      chance += chances[i]
      if rnd<chance
        chosenPkmn = i
        break
      end
    end
    # Get the chosen species and level
    encounter = encList[chosenPkmn]
    return nil if !encounter
    level = encounter[1]+rand(1+encounter[2]-encounter[1])
    # Some abilities alter the level of the wild Pokémon
    if firstPkmn && rand(100)<50   # 50% chance of happening
      if isConst?(firstPkmn.ability,PBAbilities,:HUSTLE) ||
         isConst?(firstPkmn.ability,PBAbilities,:VITALSPIRIT) ||
         isConst?(firstPkmn.ability,PBAbilities,:PRESSURE)||
         isConst?(firstPkmn.ability,PBAbilities,:CALAMITYABYSSAL) ||
         isConst?(firstPkmn.ability,PBAbilities,:CALAMITYINFERNAL)
         level2 = encounter[1]+rand(1+encounter[2]-encounter[1])
        level = level2 if level2>level   # Higher level is more likely
      end
    end
    # Black Flute and White Flute alter the level of the wild Pokémon
    if NEWEST_BATTLE_MECHANICS
      if $PokemonMap.blackFluteUsed
        level = [level+1+rand(3),PBExperience.maxLevel].min
      elsif $PokemonMap.whiteFluteUsed
        level = [level-1-rand(3),1].max
      end
    end
    # Return [species, level]
    return [encounter[0],level]
  end
end
#==============================================================================
# 个体值保底和闪率加成
#==============================================================================
def pbGenerateWildPokemon(species,level,isRoamer=false)
  genwildpoke = pbNewPkmn(species,level)
  if $DEBUG && rand(100) < 25 || rand(4096) < 256
    genwildpoke.setAbility(2)
    # 狩猎地带不生成精英怪
    if !pbInSafari?
      genwildpoke.battleRank = 2
      genwildpoke.iv = pbRandomIV(1)
    end
  end
if $game_map.map_id == 508 || $game_map.map_id == 510
  genwildpoke.makeShiny
  genwildpoke.makeSuperShiny
end
  # Give the wild Pokémon a held item
  items = genwildpoke.wildHoldItems
  firstPkmn = $Trainer.firstPokemon
  chances = [50,5,1]
  chances = [60,20,5] if firstPkmn && isConst?(firstPkmn.ability,PBAbilities,:COMPOUNDEYES)
  chances = [50,50,50] if firstPkmn && isConst?(firstPkmn.ability,PBAbilities,:SUPERLUCK)
  itemrnd = rand(100)
  if (items[0]==items[1] && items[1]==items[2]) || itemrnd<chances[0]
    genwildpoke.setItem(items[0])
  elsif itemrnd<(chances[0]+chances[1])
    genwildpoke.setItem(items[1])
  elsif itemrnd<(chances[0]+chances[1]+chances[2])
    genwildpoke.setItem(items[2])
  end
  #---------------------------------------------------------------------------
  $Trainer.getChain(species)
  maxCount = 0
  genwildpoke.iv.each { |iv| maxCount += 1 if iv==31 }
  chain = $Trainer.chainCatching.iv_guaranteed
  genwildpoke.iv = pbRandomIV(chain) if chain > maxCount
  genwildpoke.calcStats
  shinyretries = $Trainer.chainCatching.shiny_retries
  if hasConst?(PBItems,:SHINYCHARM) && $PokemonBag.pbHasItem?(:SHINYCHARM)
    shinyretries += 8
  end
  shinyretries.times do
    genwildpoke.personalID = rand(65536)|(rand(65536)<<16)
    if genwildpoke.shiny?
      if !genwildpoke.superShiny? && rand(16)<1
        genwildpoke.makeSuperShiny
      end
      break
    end
  end
  #---------------------------------------------------------------------------
  # Give Pokérus
  if rand(65536)<POKERUS_CHANCE
    genwildpoke.givePokerus
  end
  # Change wild Pokémon's gender/nature depending on the lead party Pokémon's
  # ability
  if firstPkmn
    if isConst?(firstPkmn.ability,PBAbilities,:CUTECHARM) && !genwildpoke.singleGendered?
      if firstPkmn.male?
        (rand(3)<2) ? genwildpoke.makeFemale : genwildpoke.makeMale
      elsif firstPkmn.female?
        (rand(3)<2) ? genwildpoke.makeMale : genwildpoke.makeFemale
      end
    elsif isConst?(firstPkmn.ability,PBAbilities,:SYNCHRONIZE)
      genwildpoke.setNature(firstPkmn.nature) if !isRoamer
    end
  end
  # Trigger events that may alter the generated Pokémon further
  Events.onWildPokemonCreate.trigger(nil,genwildpoke)
  return genwildpoke
end
#==============================================================================
# 刷新连锁捕捉
#==============================================================================
module PokeBattle_BattleCommon
  def pbThrowPokeBall(idxBattler,ball,rareness=nil,showPlayer=false)
    # Determine which Pokémon you're throwing the Poké Ball at
    battler = nil
    if opposes?(idxBattler)
      battler = @battlers[idxBattler]
    else
      battler = @battlers[idxBattler].pbDirectOpposing(true)
    end
    if battler.fainted?
      battler.eachAlly do |b|
        battler = b
        break
      end
    end
    # Messages
    itemName = PBItems.getName(ball)
    if battler.fainted?
      if itemName.starts_with_vowel?
        pbDisplay(_INTL("{1}扔出了{2}！",pbPlayer.name,itemName))
      else
        pbDisplay(_INTL("{1}扔出了{2}！",pbPlayer.name,itemName))
      end
      pbDisplay(_INTL("但是没有目标……"))
      return
    end
    if itemName.starts_with_vowel?
      pbDisplayBrief(_INTL("{1}扔出了{2}！",pbPlayer.name,itemName))
    else
      pbDisplayBrief(_INTL("{1}扔出了{2}！",pbPlayer.name,itemName))
    end
    # Animation of opposing trainer blocking Poké Balls (unless it's a Snag Ball
    # at a Shadow Pokémon)
    if trainerBattle? && !(pbIsSnagBall?(ball) && battler.shadowPokemon?)
      @scene.pbThrowAndDeflect(ball,1)
      pbDisplay(_INTL("训练家打飞了球\n不要做小偷！"))
      return
    elsif $game_switches[60] # 不可捕捉的野外对战
       pbDisplay(_INTL("精灵球被破坏了！\n看来只能战胜它了！"))
       return
     end
    # Calculate the number of shakes (4=capture)
    pkmn = battler.pokemon
    @criticalCapture = false
    numShakes = pbCaptureCalc(pkmn,battler,rareness,ball)
    PBDebug.log("[Threw Poké Ball] #{itemName}, #{numShakes} shakes (4=capture)")
    # Animation of Ball throw, absorb, shake and capture/burst out
    @scene.pbThrow(ball,numShakes,@criticalCapture,battler.index,showPlayer)
    # Outcome message
    case numShakes
    when 0
      pbDisplay(_INTL("哦不！\n宝可梦逃出来了！"))
      BallHandlers.onFailCatch(ball,self,battler)
    when 1
      pbDisplay(_INTL("啊！\n还以为能抓住呢……"))
      BallHandlers.onFailCatch(ball,self,battler)
    when 2
      pbDisplay(_INTL("好可惜...\n差一点就能成功了"))
      BallHandlers.onFailCatch(ball,self,battler)
    when 3
      pbDisplay(_INTL("真可惜…\n明明只差一点点了。"))
      BallHandlers.onFailCatch(ball,self,battler)
    when 4
      pbDisplayBrief(_INTL("太好了！\n捉到了{1}！",pkmn.name))
      @scene.pbThrowSuccess   # Play capture success jingle
      pbRemoveFromParty(battler.index,battler.pokemonIndex)
      # Gain Exp
      if GAIN_EXP_FOR_CAPTURE
        battler.captured = true
        pbGainExp
        battler.captured = false
      end
      battler.pbReset
      if trainerBattle?
        @decision = 1 if pbAllFainted?(battler.index)
      else
        @decision = 4 if pbAllFainted?(battler.index)   # Battle ended by capture
      end
      # Modify the Pokémon's properties because of the capture
      if pbIsSnagBall?(ball)
        pkmn.ot        = pbPlayer.name
        pkmn.trainerID = pbPlayer.id
      end
      BallHandlers.onCatch(ball,self,pkmn)
      pkmn.ballused = pbGetBallType(ball)
      pkmn.makeUnmega if pkmn.mega?
      pkmn.makeUnprimal
      pkmn.pbUpdateShadowMoves if pkmn.shadowPokemon?
      pkmn.pbRecordFirstMoves
      # Reset form
      pkmn.forcedForm = nil if MultipleForms.hasFunction?(pkmn.species,"getForm")
      @peer.pbOnLeavingBattle(self,pkmn,true,true)
      # Make the Poké Ball and data box disappear
      @scene.pbHideCaptureBall(idxBattler)
      #-------------------------------------------------------
      # 刷新连锁捕捉
      $Trainer.refreshChain(pkmn.species)
      #-------------------------------------------------------
      pkmn.level=200 if pkmn.level>200
      pkmn.level=1 if $game_switches[35] #抓到的变为一级
      pkmn.battleRank = 1 if pkmn.battleRank > 1
      pkmn.calcStats
      pkmn.hp = 1 if pkmn.hp < 1
      # Save the Pokémon for storage at the end of battle
      @caughtPokemon.push(pkmn)
    end
  end
end
#=end