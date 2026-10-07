class PokeBattle_Battler
  #=============================================================================
  # Change HP
  #=============================================================================
  def pbReduceHP(amt,anim=true,registerDamage=true,anyAnim=true)
    amt = amt.round
    amt = @hp if amt>@hp
    amt = 1 if amt<1 && !fainted?
    oldHP = @hp
    self.hp -= amt
    PBDebug.log("[HP change] #{pbThis} lost #{amt} HP (#{oldHP}=>#{@hp})") if amt>0
    raise _INTL("HP小于0") if @hp<0
    raise _INTL("HP大于最大HP") if @hp>@totalhp
    @battle.scene.pbHPChanged(self,oldHP,anim) if anyAnim && amt>0
    @tookDamage = true if amt>0 && registerDamage
    return amt
  end

  def pbRecoverHP(amt,anim=true,anyAnim=true)
    amt = amt.round
    amt = @totalhp-@hp if amt>@totalhp-@hp
    amt = 1 if amt<1 && @hp<@totalhp
    oldHP = @hp
    self.hp += amt
    PBDebug.log("[HP change] #{pbThis} gained #{amt} HP (#{oldHP}=>#{@hp})") if amt>0
    raise _INTL("HP小于0") if @hp<0
    raise _INTL("HP大于最大HP") if @hp>@totalhp
    @battle.scene.pbHPChanged(self,oldHP,anim) if anyAnim && amt>0
    self.yamaskhp = 0
    return amt
  end

  def pbRecoverHPFromDrain(amt,target,msg=nil)
    if target.hasActiveAbility?(:LIQUIDOOZE)
      @battle.pbShowAbilitySplash(target)
      pbReduceHP(amt)
      @battle.pbDisplay(_INTL("{1}吸到了污泥浆！",pbThis))
      @battle.pbHideAbilitySplash(target)
      pbItemHPHealCheck
    else
      msg = _INTL("{1}回复了HP！",pbThis) if !msg || msg==""
      @battle.pbDisplay(msg)
      if canHeal?
        amt = (amt*1.3).floor if hasActiveItem?(:BIGROOT)
        pbRecoverHP(amt)
      end
    end
  end
  
  def pbTakeEffectDamage(amt, show_anim = true)
    @droppedBelowHalfHP = false
    hp_lost = pbReduceHP(amt, show_anim)
    yield hp_lost if block_given?   # Show message
    pbItemHPHealCheck
    pbAbilitiesOnDamageTaken(@hp)
    pbFaint if fainted?
    @droppedBelowHalfHP = false
  end

  
  def pbFaint(showMessage=true)
    if @pokemon && @pokemon.battleRank > 1
      pbCatchBossPokemon(self) if @battle.decision == 0
      return
    end
    if !fainted?
      PBDebug.log("!!!***Can't faint with HP greater than 0")
      return
    end
    return if @fainted   # Has already fainted properly
    @battle.pbDisplayBrief(_INTL("{1}倒下了！",pbThis)) if showMessage
    PBDebug.log("[Pokémon fainted] #{pbThis} (#{@index})") if !showMessage
    @battle.scene.pbFaintBattler(self)
    pbInitEffects(false)
    # Reset status
    self.status      = PBStatuses::NONE
    self.statusCount = 0
    # Lose happiness
    if @pokemon && @battle.internalBattle
      badLoss = false
      @battle.eachOtherSideBattler(@index) do |b|
        badLoss = true if b.level>=self.level+30
      end
      @pokemon.changeHappiness((badLoss) ? "faintbad" : "faint")
    end
    # Reset form
    @battle.peer.pbOnLeavingBattle(@battle,@pokemon,@battle.usedInBattle[idxOwnSide][@index/2])
    @pokemon.makeUnmega if mega?
    @pokemon.makeUnprimal if primal?
    @pokemon.yamaskhp = 0 # Yamask
    # Do other things
    @battle.pbClearChoice(@index)   # Reset choice
    pbOwnSide.effects[PBEffects::LastRoundFainted] = @battle.turnCount
    # Check other battlers' abilities that trigger upon a battler fainting
    pbAbilitiesOnFainting
    # Check for end of primordial weather
    @battle.pbEndPrimordialWeather
    @battle.pbAddFaintedAlly(self)
  end
  #-----------------------------------------------------------------------------
  # -Aliased to end the effects of Commander when one of the pair faints
  # -Adds to the number of fainted party members this battle.
  #-----------------------------------------------------------------------------
  alias paldea_pbFaint pbFaint
  def pbFaint(showMessage = true)
    commanderMsg = nil
    if @effects[PBEffects::Commander]
      pairedBattler = @battle.battlers[@effects[PBEffects::Commander][0]]
      if pairedBattler && pairedBattler.effects[PBEffects::Commander]
        if isCommander?
          order = [pbThis, pairedBattler.pbThis(true)]
        else
          order = [pairedBattler.pbThis, pbThis(true)]
          pairedBattler.effects[PBEffects::Commander] = nil
        end
        commanderMsg = _INTL("{1}从{2}的嘴里出来了！", *order)
        batSprite = @battle.scene.sprites["pokemon_#{pairedBattler.index}"]
      end
    end
    isFainted = @fainted
    paldea_pbFaint(showMessage)
    @battle.pbAddFaintedAlly(self) if !isFainted && @fainted
    if commanderMsg
      @battle.pbDisplay(commanderMsg)
      batSprite.visible = true
    end
  end

  #=============================================================================
  # Move PP
  #=============================================================================
  def pbSetPP(move,pp)
    move.pp = pp
    # No need to care about @effects[PBEffects::Mimic], since Mimic can't copy
    # Mimic
    if move.realMove && move.id==move.realMove.id && !@effects[PBEffects::Transform]
      move.realMove.pp = pp
    end
  end

  def pbReducePP(move)
    return true if usingMultiTurnAttack?
    return true if move.pp<0         # Don't reduce PP for special calls of moves
    return true if move.totalpp<=0   # Infinite PP, can always be used
    return false if move.pp==0       # Ran out of PP, couldn't reduce
    pbSetPP(move,move.pp-1) if move.pp>0
    return true
  end

  def pbReducePPOther(move)
    pbSetPP(move,move.pp-1) if move.pp>0
  end

  #=============================================================================
  # Change type
  #=============================================================================
  def pbChangeTypes(newType)
    if newType.is_a?(PokeBattle_Battler)
      newTypes = newType.pbTypes
      newTypes.push(getConst(PBTypes,:NORMAL) || 0) if newTypes.length==0
      newType3 = newType.effects[PBEffects::Type3]
      newType3 = -1 if newTypes.include?(newType3)
      @type1 = newTypes[0]
      @type2 = (newTypes.length==1) ? newTypes[0] : newTypes[1]
      @effects[PBEffects::Type3] = newType3
    else
      newType = getConst(PBTypes,newType) if newType.is_a?(Symbol) || newType.is_a?(String)
      @type1 = newType
      @type2 = newType
      @effects[PBEffects::Type3] = -1
    end
    @effects[PBEffects::BurnUp] = false
    @effects[PBEffects::Roost]  = false
    @effects[PBEffects::LoseGrassType] = false
    @effects[PBEffects::LoseFireType] = false
    @effects[PBEffects::LoseWaterType] = false
  end

  #=============================================================================
  # Forms
  #=============================================================================
  def pbChangeFormTransform(newForm,msg)
    return if fainted? || @effects[PBEffects::Transform] || @form==newForm
    oldForm = @form
    oldDmg = @totalhp-@hp
    self.form = newForm
    pbUpdate(true)
    @hp = @totalhp-oldDmg
    @effects[PBEffects::WeightChange] = 0 if NEWEST_BATTLE_MECHANICS
    @battle.scene.pbChangePokemonTransform(self,@pokemon)
    @battle.scene.pbRefreshOne(@index)
    @battle.pbDisplay(msg) if msg && msg!=""
    PBDebug.log("[Form changed] #{pbThis} changed from form #{oldForm} to form #{newForm}")
    @battle.pbSetSeen(self)
  end

  def pbCheckFormOnStatusChange
    return if fainted? || @effects[PBEffects::Transform]
    # Shaymin - reverts if frozen
    if isSpecies?(:SHAYMIN) && frozen?
      pbChangeFormTransform(0,_INTL("{1}变身了！",pbThis))
    elsif isSpecies?(:ROSEDRAGON) && hasActiveAbility?(:SLEEPSOUNDLY)
      if asleep?
        pbChangeFormTransform(1,_INTL("{1}的身体变得坚硬！",pbThis))
      else
        pbChangeFormTransform(0,_INTL("{1}变得具有攻击性！",pbThis))
  if pbCanRaiseStatStage?(PBStats::ATTACK)
          pbRaiseStatStageByAbility(PBStats::ATTACK,1,self,false)
        end
        if pbCanRaiseStatStage?(PBStats::SPEED)
          pbRaiseStatStageByAbility(PBStats::SPEED,1,self,false)
        end
      end
    end
  end

  def pbCheckFormOnMovesetChange
    return if fainted? || @effects[PBEffects::Transform]
    # Keldeo - knowing Secret Sword
    if isSpecies?(:KELDEO)
      newForm = 0
      newForm = 1 if pbHasMove?(:SECRETSWORD)
      pbChangeFormTransform(newForm,_INTL("{1}变身了！",pbThis))
    end
  end

  def pbCheckFormOnWeatherChange
    return if fainted? || @effects[PBEffects::Transform]
    return if hasUtilityUmbrella?
    # Castform - Forecast
    if isSpecies?(:CASTFORM)
      if hasActiveAbility?(:FORECAST)
        newForm = 0
        case @battle.pbWeather
        when PBWeather::Sun, PBWeather::HarshSun
          newForm = 1
        when PBWeather::Rain, PBWeather::HeavyRain
          newForm = 2
        when PBWeather::Hail
          newForm = 3
        end
        if @form!=newForm
          @battle.pbShowAbilitySplash(self,true)
          @battle.pbHideAbilitySplash(self)
          pbChangeFormTransform(newForm,_INTL("{1}变身了！",pbThis))
        end
      else
        pbChangeFormTransform(0,_INTL("{1}变身了！",pbThis))
      end
    end
    # Cherrim - Flower Gift
    if isSpecies?(:CHERRIM)
      if hasActiveAbility?(:FLOWERGIFT)
        newForm = 0
        case @battle.pbWeather
        when PBWeather::Sun, PBWeather::HarshSun; newForm = 1
        end
        if @form!=newForm
          @battle.pbShowAbilitySplash(self,true)
          @battle.pbHideAbilitySplash(self)
          pbChangeFormTransform(newForm,_INTL("{1}变身了！",pbThis))
        end
      else
        pbChangeFormTransform(0,_INTL("{1}变身了！",pbThis))
      end
    end
    # Eiscue - Ice Face
    if isConst?(@species,PBSpecies,:EISCUE) && hasActiveAbility?(:ICEFACE) &&
        (@battle.pbWeather == PBWeather::Hail || @battle.pbWeather == PBWeather::Snow)
      if @form==1
        @battle.pbShowAbilitySplash(self,true)
        @battle.pbHideAbilitySplash(self)
        pbChangeFormTransform(0,_INTL("{1}改变了形态！",pbThis))
      end
    end
    # 古代活性
    if hasActiveAbility?(:PROTOSYNTHESIS)
      BattleHandlers::AbilityOnSwitchIn.trigger(self.ability, self, @battle)
    end
  end

  #=============================================================================
  # Change type of Galarian Stunfisk - Mimicry
  #=============================================================================
  def pbChangeTypes(newType)
    if newType.is_a?(PokeBattle_Battler)
      newTypes = newType.pbTypes
      newTypes.push(getConst(PBTypes,:NORMAL) || 0) if newTypes.length==0
      newType3 = newType.effects[PBEffects::Type3]
      newType3 = -1 if newTypes.include?(newType3)
      @type1 = newTypes[0]
      @type2 = (newTypes.length==1) ? newTypes[0] : newTypes[1]
      @effects[PBEffects::Type3] = newType3
    elsif newType.is_a?(Array)
      newType = newType.map {|t|
        if t.is_a?(Symbol) || t.is_a?(String)
          getConst(PBTypes,t)
        else
          t
        end
      }
      newType3 = newType[2] || -1
      @type1 = newType[0]
      @type2 = newType[1] || newType[0]
      @effects[PBEffects::Type3] = newType3
    else
      newType = getConst(PBTypes,newType) if newType.is_a?(Symbol) || newType.is_a?(String)
      @type1 = newType
      @type2 = newType
      @effects[PBEffects::Type3] = -1
    end
    @effects[PBEffects::BurnUp] = false
    @effects[PBEffects::Roost]  = false
  end

   def pbCheckFormOnTerrainChange
    return if fainted? #|| @effects[PBEffects::Transform] Ditto reverts back to Normal.
    if hasActiveAbility?(:MIMICRY)
      newTypes = self.pbTypes
      originalTypes=[@pokemon.type1,@pokemon.type2] | []
      case @battle.field.terrain
      when PBBattleTerrains::Electric;   newTypes = [getID(PBTypes,:ELECTRIC)]
      when PBBattleTerrains::Grassy;     newTypes = [getID(PBTypes,:GRASS)]
      when PBBattleTerrains::Misty;      newTypes = [getID(PBTypes,:FAIRY)]
      when PBBattleTerrains::Psychic;    newTypes = [getID(PBTypes,:PSYCHIC)]
      else;                              newTypes = originalTypes.dup
      end
      if self.pbTypes!=newTypes
        pbChangeTypes(newTypes)
        @battle.pbShowAbilitySplash(self,true)
        @battle.pbHideAbilitySplash(self)
        if newTypes!=originalTypes
          if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
            @battle.pbDisplay(_INTL("{1}'s type changed to {3}!",pbThis,
             self.abilityName,PBTypes.getName(newTypes[0])))
          else
            @battle.pbDisplay(_INTL("{2}使{1}变成了{3}属性！",pbThis,
             self.abilityName,PBTypes.getName(newTypes[0])))
          end
        else
          @battle.pbDisplay(_INTL("{1} returned back to normal!",pbThis))
        end
      end
    end
  end

  # Checks the Pokémon's form and updates it if necessary. Used for when a
  # Pokémon enters battle (endOfRound=false) and at the end of each round
  # (endOfRound=true).
  def pbCheckForm(endOfRound=false)
    return if fainted? || @effects[PBEffects::Transform]
    # Form changes upon entering battle and when the weather changes
    pbCheckFormOnWeatherChange if !endOfRound
  pbCheckFormOnTerrainChange if !endOfRound
    # Darmanitan - Zen Mode
    if isConst?(@species,PBSpecies,:DARMANITAN) && isConst?(@ability,PBAbilities,:ZENMODE)
      if @hp<=@totalhp/2
        if @form!=2 && @form!=3
          @battle.pbShowAbilitySplash(self,true)
          @battle.pbHideAbilitySplash(self)
          pbChangeFormTransform(2,_INTL("{1}启动了！",abilityName)) if @form == 0
          pbChangeFormTransform(3,_INTL("{1}启动了！",abilityName)) if @form == 1
        end
      elsif @form!=0 && @form != 1
        @battle.pbShowAbilitySplash(self,true)
        @battle.pbHideAbilitySplash(self)
        pbChangeFormTransform(0,_INTL("{1}启动了！",abilityName)) if @form == 2
        pbChangeFormTransform(1,_INTL("{1}启动了！",abilityName)) if @form == 3
      end
    end
    # Minior - Shields Down
    if isSpecies?(:MINIOR) && isConst?(@ability,PBAbilities,:SHIELDSDOWN)
      if @hp>@totalhp/2   # Turn into Meteor form
        newForm = (@form>=7) ? @form-7 : @form
        if @form!=newForm
          @battle.pbShowAbilitySplash(self,true)
          @battle.pbHideAbilitySplash(self)
          pbChangeFormTransform(newForm,_INTL("{1}被打破了！",abilityName))
        elsif !endOfRound
          @battle.pbDisplay(_INTL("{1}被打破了！",abilityName))
        end
      elsif @form<7   # Turn into Core form
        @battle.pbShowAbilitySplash(self,true)
        @battle.pbHideAbilitySplash(self)
        pbChangeFormTransform(@form+7,_INTL("{1}启动了！",abilityName))
      end
    end
    # Wishiwashi - Schooling
    if isSpecies?(:WISHIWASHI) && isConst?(@ability,PBAbilities,:SCHOOLING)
      if @level>=20 && @hp>@totalhp/4
        if @form!=1
          @battle.pbShowAbilitySplash(self,true)
          @battle.pbHideAbilitySplash(self)
          pbChangeFormTransform(1,_INTL("{1}的伙伴们聚集起来了！",pbThis))
        end
      elsif @form!=0
        @battle.pbShowAbilitySplash(self,true)
        @battle.pbHideAbilitySplash(self)
        pbChangeFormTransform(0,_INTL("{1}的伙伴们离去了！",pbThis))
      end
    end
     # Zygarde - Power Construct
     # 在角色出场时的检查
   if isSpecies?(:ZYGARDE) && isConst?(@ability, PBAbilities, :POWERCONSTRUCT)
  # 检查是否为可以变身的形态（0-3）
  if @form >= 0 && @form <= 3
    # 记录原始形态用于后续恢复
    @originalZygardeForm ||= @form
    
    newForm = 4  # 完全体形态
    @battle.pbDisplay(_INTL("{1}感觉到许多人的存在！", pbThis))
    @battle.pbShowAbilitySplash(self, true)
    @battle.pbHideAbilitySplash(self)
    pbChangeFormTransform(newForm, _INTL("变成了完全体形态！", pbThis))
  end
end
    end
  def pbTransform(target)
    oldAbil = @ability
    @effects[PBEffects::Transform]        = true
    @effects[PBEffects::TransformSpecies] = target.species
    pbChangeTypes(target)
    @ability = target.ability
    @attack  = target.attack
    @defense = target.defense
    @spatk   = target.spatk
    @spdef   = target.spdef
    @speed   = target.speed
    PBStats.eachBattleStat { |s| @stages[s] = target.stages[s] }
    if NEWEST_BATTLE_MECHANICS
      @effects[PBEffects::FocusEnergy] = target.effects[PBEffects::FocusEnergy]
      @effects[PBEffects::LaserFocus]  = target.effects[PBEffects::LaserFocus]
    end
    @moves.clear
    target.moves.each_with_index do |m,i|
      @moves[i] = PokeBattle_Move.pbFromPBMove(@battle,PBMove.new(m.id))
      @moves[i].pp      = 5
      @moves[i].totalpp = 5
    end
    @effects[PBEffects::Disable]      = 0
    @effects[PBEffects::DisableMove]  = 0
    @effects[PBEffects::WeightChange] = target.effects[PBEffects::WeightChange]
    @battle.scene.pbRefreshOne(@index)
    @battle.pbDisplay(_INTL("{1}变成了{2}！",pbThis,target.pbThis(true)))
    pbOnAbilityChanged(oldAbil)
  end
  def pbHyperMode; end
end
