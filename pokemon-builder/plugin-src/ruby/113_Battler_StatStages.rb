class PokeBattle_Battler
  #=============================================================================
  # Increase stat stages
  #=============================================================================
  def statStageAtMax?(stat)
    return @stages[stat]>=6
  end
  
  def pbCanRaiseStatStage?(stat,user=nil,move=nil,showFailMsg=false,ignoreContrary=false)
    return false if fainted?
    # Contrary
    if hasActiveAbility?(:CONTRARY) && !ignoreContrary && !@battle.moldBreaker
      return pbCanLowerStatStage?(stat,user,move,showFailMsg,true)
    end
    if abilityActive?
      return false if BattleHandlers.triggerStatGainImmunityAbility(
          self.ability,self,stat,@battle,showFailMsg) if !@battle.moldBreaker
    end
    # Check the stat stage
    if statStageAtMax?(stat)
      @battle.pbDisplay(_INTL("{1}的{2}不能再提高了！",
         pbThis,PBStats.getName(stat))) if showFailMsg
      return false
    end
    return true
  end
  
  def pbRaiseStatStageBasic(stat,increment,ignoreContrary=false)
    if !@battle.moldBreaker
      # Contrary
      if hasActiveAbility?(:CONTRARY) && !ignoreContrary
        return pbLowerStatStageBasic(stat,increment,true)
      end
      # Simple
      increment *= 2 if hasActiveAbility?(:SIMPLE)
    end
    # Change the stat stage
    increment = [increment,6-@stages[stat]].min
    if increment>0
      s = PBStats.getName(stat); new = @stages[stat]+increment
      PBDebug.log("[Stat change] #{pbThis}'s #{s}: #{@stages[stat]} -> #{new} (+#{increment})")
      @stages[stat] += increment
    end
    return increment
  end

  def pbRaiseStatStage(stat,increment,user,showAnim=true,ignoreContrary=false)
    return false if !PBStats.validBattleStat?(stat)
    # Contrary
    if hasActiveAbility?(:CONTRARY) && !ignoreContrary && !@battle.moldBreaker
      return pbLowerStatStage(stat,increment,user,showAnim,true)
    end
    # Perform the stat stage change
    increment = pbRaiseStatStageBasic(stat,increment,ignoreContrary)
    return false if increment<=0
    # Stat up animation and message
    @battle.pbCommonAnimation("StatUp",self) if showAnim
    arrStatTexts = [
       _INTL("{1}的{2}提升了！",pbThis,PBStats.getName(stat)),
       _INTL("{1}的{2}大幅提升了！",pbThis,PBStats.getName(stat)),
       _INTL("{1}的{2}巨幅提升了！",pbThis,PBStats.getName(stat))]
    @battle.pbDisplay(arrStatTexts[[increment-1,2].min])
    # Trigger abilities upon stat gain
    if abilityActive?
      BattleHandlers.triggerAbilityOnStatGain(@ability,self,stat,user)
    end
    @effects[PBEffects::BurningJealousy] = true
    if !@mirrorHerbUsed && !(hasActiveAbility?(:CONTRARY) && !ignoreContrary && !@battle.moldBreaker)
      addSideStatUps(stat, increment)
    end
    return true
  end

  def pbRaiseStatStageByCause(stat,increment,user,cause,showAnim=true,ignoreContrary=false)
    return false if !PBStats.validBattleStat?(stat)
    # Contrary
    if hasActiveAbility?(:CONTRARY) && !ignoreContrary && !@battle.moldBreaker
      return pbLowerStatStageByCause(stat,increment,user,cause,showAnim,true)
    end
    # Perform the stat stage change
    increment = pbRaiseStatStageBasic(stat,increment,ignoreContrary)
    return false if increment<=0
    # Stat up animation and message
    @battle.pbCommonAnimation("StatUp",self) if showAnim
    if user.index==@index
      arrStatTexts = [
         _INTL("{1}的{2}提升了{3}！",pbThis,cause,PBStats.getName(stat)),
         _INTL("{1}的{2}大幅提升了{3}！",pbThis,cause,PBStats.getName(stat)),
         _INTL("{1}的{2}巨幅提升了{3}！",pbThis,cause,PBStats.getName(stat))]
    else
      arrStatTexts = [
         _INTL("{1}的{2}提升了{3}的{4}！",user.pbThis,cause,pbThis(true),PBStats.getName(stat)),
         _INTL("{1}的{2}大幅提升了{3}的{4}！",user.pbThis,cause,pbThis(true),PBStats.getName(stat)),
         _INTL("{1}的{2}巨幅提升了{3}的{4}！",user.pbThis,cause,pbThis(true),PBStats.getName(stat))]
    end
    @battle.pbDisplay(arrStatTexts[[increment-1,2].min])
    # Trigger abilities upon stat gain
    if abilityActive?
      BattleHandlers.triggerAbilityOnStatGain(@ability,self,stat,user)
    end
        @effects[PBEffects::BurningJealousy] = true
    if !@mirrorHerbUsed && !(hasActiveAbility?(:CONTRARY) && !ignoreContrary && !@battle.moldBreaker)
      addSideStatUps(stat, increment) 
    end
    return true
  end
  
  def pbRaiseStatStageByAbility(stat,increment,user,splashAnim=true)
    return false if fainted?
    ret = false
    @battle.pbShowAbilitySplash(user) if splashAnim
    if pbCanRaiseStatStage?(stat,user,nil,PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        ret = pbRaiseStatStage(stat,increment,user)
      else
        ret = pbRaiseStatStageByCause(stat,increment,user,user.abilityName)
      end
    end
    @battle.pbHideAbilitySplash(user) if splashAnim
    pbMirrorStatUpsOpposing
    return ret
  end
  #=============================================================================
  # Decrease stat stages
  #=============================================================================
  def statStageAtMin?(stat)
    return @stages[stat]<=-6
  end

  def pbCanLowerStatStage?(stat,user=nil,move=nil,showFailMsg=false,ignoreContrary=false)
    return false if fainted?
    # Contrary
    if hasActiveAbility?(:CONTRARY) && !ignoreContrary && !@battle.moldBreaker
      return pbCanRaiseStatStage?(stat,user,move,showFailMsg,true)
    end
    if !user || user.index!=@index   # Not self-inflicted
      if itemActive?
        return false if BattleHandlers.triggerStatLossImmunityItem(self.item, self, stat, @battle, showFailMsg)
      end
      if @effects[PBEffects::Substitute]>0 && !(move && move.ignoresSubstitute?(user))
        @battle.pbDisplay(_INTL("{1}被替身保护了！",pbThis)) if showFailMsg
        return false
      end
if pbOwnSide.effects[PBEffects::Mist] > 0 &&
   !(user && (user.hasActiveAbility?(:INFILTRATOR) ||
              user.hasActiveAbility?(:TRANSLUCENTGHOST)))
  @battle.pbDisplay(_INTL("{1} 被白雾保护了！", pbThis)) if showFailMsg
  return false
end
      if abilityActive?
        return false if BattleHandlers.triggerStatLossImmunityAbility(
           @ability,self,stat,@battle,showFailMsg) if !@battle.moldBreaker
        return false if BattleHandlers.triggerStatLossImmunityAbilityNonIgnorable(
           @ability,self,stat,@battle,showFailMsg)
      end
      if !@battle.moldBreaker
        eachAlly do |b|
          next if !b.abilityActive?
          return false if BattleHandlers.triggerStatLossImmunityAllyAbility(
             b.ability,b,self,stat,@battle,showFailMsg)
        end
      end
    end
    # Check the stat stage
    if statStageAtMin?(stat)
      @battle.pbDisplay(_INTL("{1}的{2}不能再降低了！", pbThis,PBStats.getName(stat))) if showFailMsg
      return false
    end
    return true
  end

  def pbLowerStatStageBasic(stat,increment,ignoreContrary=false)
    if !@battle.moldBreaker
      # Contrary
      if hasActiveAbility?(:CONTRARY) && !ignoreContrary
        return pbRaiseStatStageBasic(stat,increment,true)
      end
      # Simple
      increment *= 2 if hasActiveAbility?(:SIMPLE)
    end
    # Change the stat stage
    increment = [increment,6+@stages[stat]].min
    if increment>0
      s = PBStats.getName(stat); new = @stages[stat]-increment
      PBDebug.log("[Stat change] #{pbThis}'s #{s}: #{@stages[stat]} -> #{new} (-#{increment})")
      @stages[stat] -= increment
    end
    return increment
  end

  def pbLowerStatStage(stat,increment,user,showAnim=true,ignoreContrary=false, ignoreMirrorArmor=false)
    return false if !PBStats.validBattleStat?(stat)
    # Mirror Armor
    if !ignoreMirrorArmor && ( 
       hasActiveAbility?(:MIRRORARMOR) || 
       hasActiveAbility?(:SPOVERLORD) )&& (!user || user.index!=@index) && 
    !@battle.moldBreaker && pbCanLowerStatStage?(stat)
      battle.pbShowAbilitySplash(self)
      @battle.pbDisplay(_INTL("{1}的{2}发动了！",pbThis,abilityName))
      if !user
        battle.pbHideAbilitySplash(self)
        return false
      end
      if (!user.hasActiveAbility?(:MIRRORARMOR)|| 
          !user.hasActiveAbility?(:SPOVERLORD)) && user.pbCanLowerStatStage?(stat,nil,nil,true)
        user.pbLowerStatStageByAbility(stat,increment,user,splashAnim=false,checkContact=false)
    # Trigger user's abilities upon stat loss
    if user.abilityActive?
      BattleHandlers.triggerAbilityOnStatLoss(user.ability,user,stat,self)
    end
      end
      battle.pbHideAbilitySplash(self)
      return false
    end
    # Contrary
    if hasActiveAbility?(:CONTRARY) && !ignoreContrary && !@battle.moldBreaker
      return pbRaiseStatStage(stat,increment,user,showAnim,true)
    end
    # Perform the stat stage change
    increment = pbLowerStatStageBasic(stat,increment,ignoreContrary)
    return false if increment<=0
    # Stat down animation and message
    @battle.pbCommonAnimation("StatDown",self) if showAnim
    arrStatTexts = [
       _INTL("{1}的{2}降低了！",pbThis,PBStats.getName(stat)),
       _INTL("{1}的{2}大幅降低了！",pbThis,PBStats.getName(stat)),
       _INTL("{1}的{2}巨幅降低了！",pbThis,PBStats.getName(stat))]
    @battle.pbDisplay(arrStatTexts[[increment-1,2].min])
    # Trigger abilities upon stat loss
    if abilityActive?
      BattleHandlers.triggerAbilityOnStatLoss(@ability,self,stat,user)
    end
    @effects[PBEffects::LashOut] = true
    return true
  end

  def pbLowerStatStageByCause(stat,increment,user,cause,showAnim=true,ignoreContrary=false, ignoreMirrorArmor=false)
    return false if !PBStats.validBattleStat?(stat)
    # Mirror Armor
    if (!ignoreMirrorArmor && hasActiveAbility?(:MIRRORARMOR)|| 
      !ignoreMirrorArmor && hasActiveAbility?(:SPOVERLORD))&& (!user || user.index!=@index) && 
    !@battle.moldBreaker && pbCanLowerStatStage?(stat)
      battle.pbShowAbilitySplash(self)
      @battle.pbDisplay(_INTL("{1}的{2}发动了！",pbThis,abilityName))
      if !user
        battle.pbHideAbilitySplash(self)
        return false
      end
      if  (!user.hasActiveAbility?(:MIRRORARMOR) || 
           !user.hasActiveAbility?(:SPOVERLORD) )&& user.pbCanLowerStatStage?(stat,nil,nil,true)
        user.pbLowerStatStageByAbility(stat,increment,user,splashAnim=false,checkContact=false)
    # Trigger user's abilities upon stat loss
    if user.abilityActive?
      BattleHandlers.triggerAbilityOnStatLoss(user.ability,user,stat,self)
    end
      end
      battle.pbHideAbilitySplash(self)
      return false
    end
    # Contrary
    if hasActiveAbility?(:CONTRARY) && !ignoreContrary && !@battle.moldBreaker
      return pbRaiseStatStageByCause(stat,increment,user,cause,showAnim,true)
    end
    # Perform the stat stage change
    increment = pbLowerStatStageBasic(stat,increment,ignoreContrary)
    return false if increment<=0
    # Stat down animation and message
    @battle.pbCommonAnimation("StatDown",self) if showAnim
    if user.index==@index
      arrStatTexts = [
         _INTL("{1}的{2}降低了{3}！",pbThis,cause,PBStats.getName(stat)),
         _INTL("{1}的{2}大幅降低了{3}！",pbThis,cause,PBStats.getName(stat)),
         _INTL("{1}的{2}巨幅降低了{3}！",pbThis,cause,PBStats.getName(stat))]
    else
      arrStatTexts = [
         _INTL("{1}的{2}降低了{3}的{4}！",user.pbThis,cause,pbThis(true),PBStats.getName(stat)),
         _INTL("{1}的{2}大幅降低了{3}的{4}！",user.pbThis,cause,pbThis(true),PBStats.getName(stat)),
         _INTL("{1}的{2}巨幅降低了{3}的{4}！",user.pbThis,cause,pbThis(true),PBStats.getName(stat))]
    end
    @battle.pbDisplay(arrStatTexts[[increment-1,2].min])
    # Trigger abilities upon stat loss
    if abilityActive?
      BattleHandlers.triggerAbilityOnStatLoss(@ability,self,stat,user)
    end
    @effects[PBEffects::LashOut] = true
    return true
  end

  def pbLowerStatStageByAbility(stat,increment,user,splashAnim=true,checkContact=false)
    if hasActiveAbility?([:WATCHDOGEYE, :GUARDDOG]) &&
         isConst?(user.ability, PBAbilities, :INTIMIDATE)
      return pbRaiseStatStageByAbility(stat, increment, self, true)
    end
    ret = false
    @battle.pbShowAbilitySplash(user) if splashAnim
    if pbCanLowerStatStage?(stat,user,nil,PokeBattle_SceneConstants::USE_ABILITY_SPLASH) &&
       (!checkContact || affectedByContactEffect?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH))
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        ret = pbLowerStatStage(stat,increment,user)
      else
        ret = pbLowerStatStageByCause(stat,increment,user,user.abilityName)
      end
    end
    @battle.pbHideAbilitySplash(user) if splashAnim
    return ret
  end


  def pbLowerAttackStatStageIntimidate(user)
    return false if fainted?
    if !hasActiveAbility?(:CONTRARY) && @effects[PBEffects::Substitute] == 0
      if itemActive? && BattleHandlers.triggerStatLossImmunityItem(self.item, self, PBStats::ATTACK, @battle, true)
        return false
      end
    end
    # NOTE: Substitute intentially blocks Intimidate even if self has Contrary.
    if @effects[PBEffects::Substitute]>0
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        @battle.pbDisplay(_INTL("{1}受到了替身的保护！",pbThis))
      else
        @battle.pbDisplay(_INTL("{1}的替身挡下了{2}的{3}！",
           pbThis,user.pbThis(true),user.abilityName))
      end
      return false
    end
    # NOTE: These checks exist to ensure appropriate messages are shown if
    #       Intimidate is blocked somehow (i.e. the messages should mention the
    #       Intimidate ability by name).
    if !hasActiveAbility?(:CONTRARY)
      if pbOwnSide.effects[PBEffects::Mist]>0
        @battle.pbDisplay(_INTL("白雾保护{1}不受{2}的{3}影响！",
           pbThis,user.pbThis(true),user.abilityName))
        return false
      end
      if abilityActive?
        if BattleHandlers.triggerStatLossImmunityAbility(@ability,self,PBStats::ATTACK,@battle,false) ||
           BattleHandlers.triggerStatLossImmunityAbilityNonIgnorable(@ability,self,PBStats::ATTACK,@battle,false) ||
            hasActiveAbility?(:INNERFOCUS) || hasActiveAbility?(:OWNTEMPO) || hasActiveAbility?(:OBLIVIOUS) || hasActiveAbility?(:SCRAPPY) || hasActiveAbility?(:DEMONKILLER) || hasActiveAbility?(:FEARLESS)
          @battle.pbShowAbilitySplash(self) if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
          @battle.pbDisplay(_INTL("{1}的{2}阻止了\n{3}的{4}生效！",
             pbThis,abilityName,user.pbThis(true),user.abilityName))
          @battle.pbHideAbilitySplash(self) if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
          return false
        end
      end
      eachAlly do |b|
        next if !b.abilityActive?
        if BattleHandlers.triggerStatLossImmunityAllyAbility(b.ability,b,self,PBStats::ATTACK,@battle,false)
          @battle.pbShowAbilitySplash(b) if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
          @battle.pbDisplay(_INTL("{4}的{5}保护{1}免受{2}的{3}！",
             pbThis,user.pbThis(true),user.abilityName,b.pbThis(true),b.abilityName))
          @battle.pbHideAbilitySplash(b) if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
          return false
        end
      end
    end
    return false if !pbCanLowerStatStage?(PBStats::ATTACK,user)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      if pbLowerStatStageByAbility(PBStats::ATTACK,1,user,false)
        pbRaiseStatStageByAbility(PBStats::SPEED,1,self) if hasActiveAbility?(:RATTLED)
        return true
      else
        return false
      end
    else
      if pbLowerStatStageByCause(PBStats::ATTACK,1,user,user.abilityName)
        pbLowerStatStageByCause(PBStats::SPEED,1,self,self.abilityName) if hasActiveAbility?(:RATTLED)
        return true
      else
        return false
      end
    end
  end

  #=============================================================================
  # Reset stat stages
  #=============================================================================
  def hasAlteredStatStages?
    PBStats.eachBattleStat { |s| return true if @stages[s]!=0 }
    return false
  end

  def hasRaisedStatStages?
    PBStats.eachBattleStat { |s| return true if @stages[s]>0 }
    return false
  end

  def hasLoweredStatStages?
    PBStats.eachBattleStat { |s| return true if @stages[s]<0 }
    return false
  end

  def pbResetStatStages
    PBStats.eachBattleStat do |s|
      if @stages[s] > 0
        @statsLoweredThisRound = true
        @statsDropped = true
      elsif @stages[s] < 0
        @statsRaisedThisRound = true
      end
      @stages[s] = 0
    end
  end
end

