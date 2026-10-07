module PokeBattle_BattleCommon
  #=============================================================================
  # Store caught Pokémon
  #=============================================================================
  def pbStorePokemon(pkmn)
    # Nickname the Pokémon (unless it's a Shadow Pokémon)
     #if !pkmn.shadowPokemon?
     #  if pbDisplayConfirm(_INTL("想为{1}取名字吗？",pkmn.name))
     #    nickname = @scene.pbNameEntry(_INTL("{1}的昵称是？",pkmn.speciesName),pkmn)
     #    pkmn.name = nickname if nickname!=""
    #   end
    # end
    # Store the Pokémon
    currentBox = @peer.pbCurrentBox
    storedBox  = @peer.pbStorePokemon(pbPlayer,pkmn)
    if storedBox<0
      pbDisplayPaused(_INTL("{1}加入了队伍。",pkmn.name))
      @initialItems[0][pbPlayer.party.length-1] = pkmn.item if @initialItems
      return
    end
    # Messages saying the Pokémon was stored in a PC box
    creator    = @peer.pbGetStorageCreatorName
    curBoxName = @peer.pbBoxName(currentBox)
    boxName    = @peer.pbBoxName(storedBox)
    if storedBox!=currentBox
      if creator
        pbDisplayPaused(_INTL("{2}电脑的盒子{1}已经满了。",curBoxName,creator))
      else
        pbDisplayPaused(_INTL("寄存系统的盒子{1}已经满了。",curBoxName))
      end
      pbDisplayPaused(_INTL("{1}被传送到盒子\"{2}了。\".",pkmn.name,boxName))
    else
      if creator
        pbDisplayPaused(_INTL("{1}被传送到{2}的电脑里。",pkmn.name,creator))
      else
        pbDisplayPaused(_INTL("{1}被传送到寄存系统里。",pkmn.name))
      end
      pbDisplayPaused(_INTL("存储到{1}了。",boxName))
    end
  end

  # Register all caught Pokémon in the Pokédex, and store them.
  def pbRecordAndStoreCaughtPokemon
    @caughtPokemon.each do |pkmn|
      pbSeenForm(pkmn)   # In case the form changed upon leaving battle
      # Record the Pokémon's species as owned in the Pokédex
      if !pbPlayer.hasOwned?(pkmn.species)
        pbPlayer.setOwned(pkmn.species)
        if $Trainer.pokedex
          pbDisplayPaused(_INTL("{1}的数据被记录在图鉴里了。",pkmn.name))
          @scene.pbShowPokedex(pkmn.species)
        end
      end
      # Record a Shadow Pokémon's species as having been caught
      if pkmn.shadowPokemon?
        pbPlayer.shadowcaught = [] if !pbPlayer.shadowcaught
        pbPlayer.shadowcaught[pkmn.species] = true
      end
      # Store caught Pokémon
      pbStorePokemon(pkmn)
    end
    @caughtPokemon.clear
  end

  #=============================================================================
  # Throw a Poké Ball
  #=============================================================================
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
      pkmn.level=1 if $game_switches[60]
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
      # Save the Pokémon for storage at the end of battle
      @caughtPokemon.push(pkmn)
    end
  end

  #=============================================================================
  # Calculate how many shakes a thrown Poké Ball will make (4 = capture)
  #=============================================================================
  def pbCaptureCalc(pkmn,battler,rareness,ball)
    return 4 if $DEBUG && Input.press?(Input::CTRL)
    # Get a rareness if one wasn't provided
    if !rareness
      rareness = pbGetSpeciesData(pkmn.species,pkmn.form,SpeciesRareness)
    end
    # Modify rareness depending on the Poké Ball's effect
    ultraBeast = (battler.isSpecies?(:NIHILEGO) ||
       battler.isSpecies?(:BUZZWOLE) ||
       battler.isSpecies?(:PHEROMOSA) ||
       battler.isSpecies?(:XURKITREE) ||
       battler.isSpecies?(:CELESTEELA) ||
       battler.isSpecies?(:KARTANA) ||
       battler.isSpecies?(:GUZZLORD) ||
       battler.isSpecies?(:POIPOLE) ||
       battler.isSpecies?(:NAGANADEL) ||
       battler.isSpecies?(:STAKATAKA) ||
       battler.isSpecies?(:BLACEPHALON))
    rareness = BallHandlers.modifyCatchRate(ball,rareness,self,battler,ultraBeast)
    # First half of the shakes calculation
    a = battler.totalhp
    b = battler.hp
    x = ((3*a-2*b)*rareness.to_f)/(3*a)
    # Calculation modifiers
    if battler.status==PBStatuses::SLEEP || battler.status==PBStatuses::FROZEN
      x *= 2.5
    elsif battler.status!=PBStatuses::NONE
      x *= 1.5
    end
    x = x.floor
    x = 1 if x<1
    # Definite capture, no need to perform randomness checks
    return 4 if x>=255 || BallHandlers.isUnconditional?(ball,self,battler)
    # Second half of the shakes calculation
    y = ( 65536 / ((255.0/x)**0.1875) ).floor
    # Critical capture check
    if ENABLE_CRITICAL_CAPTURES
      c = 0
      numOwned = $Trainer.pokedexOwned
      if numOwned>600;    c = x*5/12
      elsif numOwned>450; c = x*4/12
      elsif numOwned>300; c = x*3/12
      elsif numOwned>150; c = x*2/12
      elsif numOwned>30;  c = x/12
      end
   # Calculate the number of shakes
      rolls =($PokemonBag.pbHasItem?(:CATCHINGCHARM)) ? 4 : 1
      for i in 0...rolls
        if c>0 && pbRandom(256)<c
          @criticalCapture = true
          return 4 if pbRandom(65536)<y
          return 0
        end
      end
    end
    # Calculate the number of shakes
    numShakes = 0
    for i in 0...4
      break if numShakes<i
      numShakes += 1 if pbRandom(65536)<y
    end
    return numShakes
  end
end
