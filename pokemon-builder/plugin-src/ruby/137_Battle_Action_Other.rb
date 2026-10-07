class PokeBattle_Battle
  #=============================================================================
  # Shifting a battler to another position in a battle larger than double
  #=============================================================================
  def pbCanShift?(idxBattler)
    return false if pbSideSize(0)<=2 && pbSideSize(1)<=2   # Double battle or smaller
    idxOther = -1
    case pbSideSize(idxBattler)
    when 1
      return false   # Only one battler on that side
    when 2
      idxOther = (idxBattler+2)%4
    when 3
      return false if idxBattler==2 || idxBattler==3   # In middle spot already
      idxOther = ((idxBattler%2)==0) ? 2 : 3
    end
    return false if pbGetOwnerIndexFromBattlerIndex(idxBattler)!=pbGetOwnerIndexFromBattlerIndex(idxOther)
    return false if @battlers[idxBattler].effects[PBEffects::Commander] || 
                    @battlers[idxOther].effects[PBEffects::Commander]
    return true
  end

  def pbRegisterShift(idxBattler)
    @choices[idxBattler][0] = :Shift
    @choices[idxBattler][1] = 0
    @choices[idxBattler][2] = nil
    return true
  end

  #=============================================================================
  # Calling at a battler
  #=============================================================================
  def pbRegisterCall(idxBattler)
    @choices[idxBattler][0] = :Call
    @choices[idxBattler][1] = 0
    @choices[idxBattler][2] = nil
    return true
  end

  def pbCall(idxBattler)
    battler = @battlers[idxBattler]
    trainerName = pbGetOwnerName(idxBattler)
    pbDisplay(_INTL("{1}呼唤了{2}！",trainerName,battler.pbThis(true)))
    pbDisplay(_INTL("{1}！",battler.name))
    if battler.shadowPokemon?
      if battler.inHyperMode?
        battler.pokemon.hypermode = false
        battler.pokemon.adjustHeart(-300)
        pbDisplay(_INTL("{1}在训练家的呼唤下苏醒过来了！",battler.pbThis))
      else
        pbDisplay(_INTL("但什么也没有发生！"))
      end
    elsif battler.status==PBStatuses::SLEEP
      battler.pbCureStatus
    elsif battler.pbCanRaiseStatStage?(PBStats::ACCURACY,battler)
      battler.pbRaiseStatStage(PBStats::ACCURACY,1,battler)
    else
      pbDisplay(_INTL("但什么也没有发生！"))
    end
  end

  #=============================================================================
  # Choosing to Mega Evolve a battler
  #=============================================================================
  def pbHasMegaRing?(idxBattler)
    return true if !pbOwnedByPlayer?(idxBattler)   # Assume AI trainer have a ring
    MEGA_RINGS.each do |item|
      return true if hasConst?(PBItems,item) && $PokemonBag.pbHasItem?(item)
    end
    return false
  end

  def pbGetMegaRingName(idxBattler)
    if pbOwnedByPlayer?(idxBattler)
      MEGA_RINGS.each do |i|
        next if !hasConst?(PBItems,i)
        return PBItems.getName(getConst(PBItems,i)) if $PokemonBag.pbHasItem?(i)
      end
    end
    # NOTE: Add your own Mega objects for particular NPC trainers here.
#    if isConst?(pbGetOwnerFromBattlerIndex(idxBattler).trainertype,PBTrainers,:BUGCATCHER)
#      return _INTL("超级网")
#    end
    return _INTL("超级环")
  end

  def pbCanMegaEvolve?(idxBattler)
    return false if $game_switches[NO_MEGA_EVOLUTION]
    return false if !@battlers[idxBattler].hasMega?
    is_boss = @battlers[idxBattler].pokemon.battleRank > 2
    return is_boss if wildBattle? && opposes?(idxBattler)
    return false if wildBattle? && opposes?(idxBattler)
    return true if $DEBUG && Input.press?(Input::CTRL)
    return false if @battlers[idxBattler].effects[PBEffects::SkyDrop]>=0
    return false if !pbHasMegaRing?(idxBattler)
    side  = @battlers[idxBattler].idxOwnSide
    owner = pbGetOwnerIndexFromBattlerIndex(idxBattler)
    return @megaEvolution[side][owner]==-1
  end

  def pbRegisterMegaEvolution(idxBattler)
    side  = @battlers[idxBattler].idxOwnSide
    owner = pbGetOwnerIndexFromBattlerIndex(idxBattler)
    @megaEvolution[side][owner] = idxBattler
  end

  def pbUnregisterMegaEvolution(idxBattler)
    side  = @battlers[idxBattler].idxOwnSide
    owner = pbGetOwnerIndexFromBattlerIndex(idxBattler)
    @megaEvolution[side][owner] = -1 if @megaEvolution[side][owner]==idxBattler
  end

  def pbToggleRegisteredMegaEvolution(idxBattler)
    side  = @battlers[idxBattler].idxOwnSide
    owner = pbGetOwnerIndexFromBattlerIndex(idxBattler)
    if @megaEvolution[side][owner]==idxBattler
      @megaEvolution[side][owner] = -1
    else
      @megaEvolution[side][owner] = idxBattler
    end
  end

  def pbRegisteredMegaEvolution?(idxBattler)
    side  = @battlers[idxBattler].idxOwnSide
    owner = pbGetOwnerIndexFromBattlerIndex(idxBattler)
    return @megaEvolution[side][owner]==idxBattler
  end

  #=============================================================================
  # Mega Evolving a battler
  #=============================================================================
  def pbMegaEvolve(idxBattler)
    battler = @battlers[idxBattler]
    return if !battler || !battler.pokemon
    return if !battler.hasMega? || battler.mega?
    trainerName = pbGetOwnerName(idxBattler)
    # Break Illusion
    if battler.hasActiveAbility?(:ILLUSION)
      BattleHandlers.triggerTargetAbilityOnHit(battler.ability,nil,battler,nil,self)
    end
    # Mega Evolve
    case battler.pokemon.megaMessage
    when 1   # Rayquaza
      pbDisplay(_INTL("{1}衷心的祈愿传达给{2}了！",trainerName,battler.pbThis))
    else
      pbDisplay(_INTL("{1}的{2}与{3}的{4}\n发生了反应！",
         battler.pbThis,battler.itemName,trainerName,pbGetMegaRingName(idxBattler)))
    end
    pbCommonAnimation("MegaEvolution",battler)
    battler.pokemon.makeMega
    battler.form = battler.pokemon.form
    battler.pbUpdate(true)
    @scene.pbChangePokemon(battler,battler.pokemon)
    @scene.pbRefreshOne(idxBattler)
    pbCommonAnimation("MegaEvolution2",battler)
    megaName = battler.pokemon.megaName
    if !megaName || megaName==""
      megaName = _INTL("超级{1}",PBSpecies.getName(battler.pokemon.species))
    end
    pbDisplay(_INTL("{1}超级进化为\n{2}！",battler.pbThis,megaName))
    side  = battler.idxOwnSide
    owner = pbGetOwnerIndexFromBattlerIndex(idxBattler)
    @megaEvolution[side][owner] = -2
    if battler.isSpecies?(:GENGAR) && battler.mega?
      battler.effects[PBEffects::Telekinesis] = 0
    end
    pbCalculatePriority(false,[idxBattler]) if NEWEST_BATTLE_MECHANICS
    # Trigger ability
    battler.pbEffectsOnSwitchIn
  end

  #=============================================================================
  # Primal Reverting a battler
  #=============================================================================
  def pbPrimalReversion(idxBattler)
    battler = @battlers[idxBattler]
    return if !battler || !battler.pokemon
    return if !battler.hasPrimal? ||battler.primal?
    if battler.isSpecies?(:KYOGRE)
        pbCommonAnimation("PrimalKyogre",battler)
      megascene = ScenePrimalblue.new
      megascene.start(time,backdrop,battler.pokemon)
      battler.pokemon.makePrimal
      battler.form = battler.pokemon.form
      battler.pbUpdate(true)
      @scene.pbChangePokemon(battler,battler.pokemon)
      @scene.pbRefreshOne(idxBattler)
      megascene.endScene
      pbCommonAnimation("PrimalKyogre2",battler)
    elsif battler.isSpecies?(:GROUDON)
        pbCommonAnimation("PrimalGroudon",battler)
      megascene = ScenePrimalred.new
      megascene.start(time,backdrop,battler.pokemon)
      battler.pokemon.makePrimal
      battler.form = battler.pokemon.form
      battler.pbUpdate(true)
      @scene.pbChangePokemon(battler,battler.pokemon)
      @scene.pbRefreshOne(idxBattler)
      megascene.endScene
      pbCommonAnimation("PrimalGroudon2",battler)
    end
    pbDisplay(_INTL("{1}原始回归了！\n{1}回到了原始形态！",battler.pbThis))
  end
end
