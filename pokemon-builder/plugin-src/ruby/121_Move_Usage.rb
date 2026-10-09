class PokeBattle_Move
  #=============================================================================
  # Effect methods per move usage
  #=============================================================================
  def pbCanChooseMove?(user,commandPhase,showMessages); return true; end   # For Belch
  def pbDisplayChargeMessage(user); end   # For Focus Punch/shell Trap/Beak Blast
  def pbOnStartUse(user,targets); end
  def pbAddTarget(targets,user); end      # For Counter, etc. and Bide

  # Reset move usage counters (child classes can increment them).
  def pbChangeUsageCounters(user,specialUsage)
    user.effects[PBEffects::GlaiveRush] = 0
    user.effects[PBEffects::FuryCutter]   = 0
    user.effects[PBEffects::ParentalBond] = 0
    user.effects[PBEffects::ProtectRate]  = 1
    @battle.field.effects[PBEffects::FusionBolt]  = false
    @battle.field.effects[PBEffects::FusionFlare] = false
    user.effects[PBEffects::SuccessiveMove] = -1 if user.effects[PBEffects::SuccessiveMove] != @id
    user.effects[PBEffects::BambooSword]  = 0 if getConst(PBMoves, :BAMBOOSWORD) != @id
  end


  def pbDisplayUseMessage(user)
    @battle.pbDisplayBrief(_INTL("{1}使用{2}！",user.pbThis,@name))
  end

  def pbMissMessage(user,target); return false; end

  #=============================================================================
  #
  #=============================================================================
  # Whether the move is currently in the "charging" turn of a two turn attack.
  # Is false if Power Herb or another effect lets a two turn move charge and
  # attack in the same turn.
  # user.effects[PBEffects::TwoTurnAttack] is set to the move's ID during the
  # charging turn, and is 0 during the attack turn.
  def pbIsChargingTurn?(user); return false; end
  def pbDamagingMove?; return damagingMove?; end

  def pbContactMove?(user)
    return false if user.hasActiveAbility?(:LONGREACH)
    return false if user.hasActiveAbility?(:NOBLESTRIKE)
    return false if user.hasActiveAbility?(:RUYIBLADE)
    return true if physicalMove? && @function=="196" # Shell Side Arm
    return contactMove?
  end

  # The maximum number of hits in a round this move will actually perform. This
  # can be 1 for Beat Up, and can be 2 for any moves affected by Parental Bond.
  def pbNumHits(user,targets)
    if user.hasActiveAbility?(:PARENTALBOND) && pbDamagingMove? &&
       !chargingTurnMove? && targets.length==1
      # Record that Parental Bond applies, to weaken the second attack
      user.effects[PBEffects::ParentalBond] = 3
      return 2
    end
    return 1
  end

  #=============================================================================
  # Effect methods per hit
  #=============================================================================
  def pbOverrideSuccessCheckPerHit(user,target); return false; end
  def pbCrashDamage(user); end
  def pbInitialEffect(user,targets,hitNum); end

  def pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)
    return if !showAnimation
    if user.effects[PBEffects::ParentalBond]==1
      @battle.pbCommonAnimation("ParentalBond",user,targets)
    else
      @battle.pbAnimation(id,user,targets,hitNum)
    end
  end

  def pbSelfKO(user); end
  def pbEffectWhenDealingDamage(user,target); end
  def pbEffectAgainstTarget(user,target); end
  def pbEffectGeneral(user); end
  def pbAdditionalEffect(user,target); end
  def pbEffectAfterAllHits(user,target); end   # Move effects that occur after all hits
  def pbSwitchOutTargetsEffect(user,targets,numHits,switchedBattlers); end
  def pbEndOfMoveUsageEffect(user,targets,numHits,switchedBattlers); end

  #=============================================================================
  # Check if target is immune to the move because of its ability
  #=============================================================================
  def pbImmunityByAbility(user,target)
    return false if @battle.moldBreaker
    ret = false
    if target.abilityActive?
      ret = BattleHandlers.triggerMoveImmunityTargetAbility(target.ability,
         user,target,self,@calcType,@battle)
    end
    return ret
  end

  #=============================================================================
  # Move failure checks
  #=============================================================================
  # Check whether the move fails completely due to move-specific requirements.
  def pbMoveFailed?(user,targets); return false; end
  # Checks whether the move will be ineffective against the target.
  def pbFailsAgainstTarget?(user,target); return false; end

  def pbMoveFailedLastInRound?(user)
    unmoved = false
    @battle.eachBattler do |b|
      next if b.index==user.index
      next if @battle.choices[b.index][0]!=:UseMove && @battle.choices[b.index][0]!=:Shift
      next if b.movedThisRound?
      unmoved = true
      break
    end
    if !unmoved
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbMoveFailedTargetAlreadyMoved?(target)
    if (@battle.choices[target.index][0]!=:UseMove &&
       @battle.choices[target.index][0]!=:Shift) || target.movedThisRound?
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbMoveFailedAromaVeil?(user,target,showMessage=true)
    return false if @battle.moldBreaker
    if target.hasActiveAbility?(:AROMAVEIL)
      if showMessage
        @battle.pbShowAbilitySplash(target)
        if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
          @battle.pbDisplay(_INTL("{1}没有受到影响！",target.pbThis))
        else
          @battle.pbDisplay(_INTL("{1}因{2}而不受影响！",
            target.pbThis,target.abilityName))
        end
        @battle.pbHideAbilitySplash(target)
      end
      return true
    end
    target.eachAlly do |b|
      next if !b.hasActiveAbility?(:AROMAVEIL)
      if showMessage
        @battle.pbShowAbilitySplash(target)
        if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
          @battle.pbDisplay(_INTL("{1}没有受到影响！",target.pbThis))
        else
          @battle.pbDisplay(_INTL("由于{2}的{3}\n{1}不受影响！",
            target.pbThis,b.pbThis(true),b.abilityName))
        end
        @battle.pbHideAbilitySplash(target)
      end
      return true
    end
    return false
  end

  #=============================================================================
  # Weaken the damage dealt (doesn't actually change a battler's HP)
  #=============================================================================
  def pbCheckDamageAbsorption(user,target)
    # Substitute will take the damage
    if target.effects[PBEffects::Substitute]>0 && !ignoresSubstitute?(user) &&
       (!user || user.index!=target.index)
      target.damageState.substitute = true
      return
    end
    # Disguise will take the damage
    if !@battle.moldBreaker && target.isSpecies?(:MIMIKYU) &&
       target.form==0 && isConst?(target.ability,PBAbilities,:DISGUISE)
      target.damageState.disguise = true
      return
    end

    # Ice Face will take the damage
    if !@battle.moldBreaker && isConst?(target.species,PBSpecies,:EISCUE) &&
       target.form==0 && isConst?(target.ability,PBAbilities,:ICEFACE) && physicalMove?
      target.damageState.iceface = true
      return
    end
  if !@battle.moldBreaker && (
   target.isSpecies?(:KABLIT)  || 
   target.isSpecies?(:FLAMBLOOM)|| 
   target.isSpecies?(:BLAZEPHEX)) &&
   target.form==0 && isConst?(target.ability,PBAbilities,:FLAMEVEIL)
   target.damageState.flameveil = true
  return
 end
end
 
  def pbReduceDamage(user,target)
    damage = target.damageState.calcDamage
    # Substitute takes the damage
    if target.damageState.substitute
      damage = target.effects[PBEffects::Substitute] if damage>target.effects[PBEffects::Substitute]
      target.damageState.hpLost       = damage
      target.damageState.totalHPLost += damage
      return
    end
    # Disguise takes the damage
    return if target.damageState.disguise
    # Ice Face takes the damage
    return if target.damageState.iceface
    # 火焰外壳
    return if target.damageState.flameveil
    # Target takes the damage
    if target.damageState.bossHP == 0
      rank = target.pokemon.battleRank
      if rank > 1 && target.hp > 0
        part_hp = target.totalhp / 2
        overflow = target.hp % part_hp
        overflow = part_hp if overflow == 0
        if damage >= overflow
          damage = overflow
          target.damageState.bossHP += 1
        end
      end
    end
    if damage>=target.hp
      damage = target.hp
      # Survive a lethal hit with 1 HP effects
      if nonLethal?(user,target)
        damage -= 1
      elsif target.effects[PBEffects::Endure]
        target.damageState.endured = true
        damage -= 1
      elsif damage==target.totalhp
        if target.hasActiveAbility?(:STURDY) && !@battle.moldBreaker
          target.damageState.sturdy = true
          damage -= 1
        elsif target.hasActiveItem?(:FOCUSSASH) && target.hp==target.totalhp
          target.damageState.focusSash = true
          damage -= 1
        elsif target.hasActiveItem?(:FOCUSBAND) && @battle.pbRandom(100)<10
          target.damageState.focusBand = true
          damage -= 1
        end
      end
    end
    damage = 0 if damage<0
    target.damageState.hpLost       = damage
    target.damageState.totalHPLost += damage
  end

  #=============================================================================
  # Change the target's HP by the amount calculated above
  #=============================================================================
  def pbInflictHPDamage(target)
    if target.damageState.substitute
      target.effects[PBEffects::Substitute] -= target.damageState.hpLost
    else
      target.hp -= target.damageState.hpLost
    end
  end
  #=============================================================================
  # Animate the damage dealt, including lowering the HP
  #=============================================================================
  # Animate being damaged and losing HP (by a move)
  def pbAnimateHitAndHPLost(user,targets)
    # Animate allies first, then foes
    animArray = []
    for side in 0...2   # side here means "allies first, then foes"
      targets.each do |b|
        next if b.damageState.unaffected || b.damageState.hpLost==0
        next if (side==0 && b.opposes?(user)) || (side==1 && !b.opposes?(user))
        oldHP = b.hp+b.damageState.hpLost
        PBDebug.log("[Move damage] #{b.pbThis} lost #{b.damageState.hpLost} HP (#{oldHP}=>#{b.hp})")
        effectiveness = 0
        if PBTypes.resistant?(b.damageState.typeMod);         effectiveness = 1
        elsif PBTypes.superEffective?(b.damageState.typeMod); effectiveness = 2
        end
        animArray.push([b,oldHP,effectiveness])
      end
      if animArray.length>0
        @battle.scene.pbHitAndHPLossAnimation(animArray)
        animArray.clear
      end
    end
    return if user.pbOwnedByPlayer? && user.allAllies.length > 1
    return if !user.pbOwnedByPlayer? && user.allOpposing.length > 1
    targets.each do |t|
      if t.pbOwnedByPlayer? && !t.fainted? && t.hp <= t.totalhp / 4
        pbBGMPlay("Battle low HP")
        break
      end
    end
  end

  #=============================================================================
  # Messages upon being hit
  #=============================================================================
  def pbEffectivenessMessage(user,target,numTargets=1)
    return if target.damageState.disguise
    return if target.damageState.iceface
    return if target.damageState.flameveil
    if PBTypes.superEffective?(target.damageState.typeMod)
      if isConst?(target.ability,PBAbilities,:ADAPTARMOR)  ||#目标特性是适应装甲
         isConst?(target.ability,PBAbilities,:ETRTNALIGHT) ||#目标特性是无限之光
         isConst?(target.ability,PBAbilities,:STARFISSURE) #目标特性是星界裂隙
        @battle.pbDisplay(_INTL("{1}因为{2}而受到常规伤害！",target.name,PBAbilities.getName(target.ability)))
         else
        if numTargets>1
          @battle.pbDisplay(_INTL("这对{1}非常有效！",target.pbThis(true)))
        else
          @battle.pbDisplay(_INTL("这非常有效！"))
        end
      end
    elsif PBTypes.notVeryEffective?(target.damageState.typeMod)
      if numTargets>1
        @battle.pbDisplay(_INTL("这对{1}不是很有效……",target.pbThis(true)))
      else
        @battle.pbDisplay(_INTL("这不是很有效……"))
      end
    end
  end

  def pbHitEffectivenessMessages(user,target,numTargets=1)
    return if target.damageState.disguise
    return if target.damageState.iceface
    return if target.damageState.flameveil
    if target.damageState.substitute
      @battle.pbDisplay(_INTL("替身承受伤害保护了{1}！",target.pbThis(true)))
    end
    if target.damageState.critical
      if numTargets>1
        @battle.pbDisplay(_INTL("击中了{1}的要害！",target.pbThis(true)))
      else
        @battle.pbDisplay(_INTL("击中了要害！"))
      end
     user.criticalHits+=1
    end
    # Effectiveness message, for moves with 1 hit
    if !multiHitMove? && user.effects[PBEffects::ParentalBond]==0
      pbEffectivenessMessage(user,target,numTargets)
    end
    if target.damageState.substitute && target.effects[PBEffects::Substitute]==0
      target.effects[PBEffects::Substitute] = 0
      @battle.pbDisplay(_INTL("{1}的替身消失了！",target.pbThis))
    end
  end

  def pbEndureKOMessage(target)
    if target.damageState.disguise
      @battle.pbShowAbilitySplash(target)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        @battle.pbDisplay(_INTL("画皮挡住了攻击！"))
      else
        @battle.pbDisplay(_INTL("{1}的画皮挡住了攻击！",target.pbThis))
      end
      @battle.pbHideAbilitySplash(target)
      target.pbChangeFormTransform(1,_INTL("{1}的画皮被破坏了！",target.pbThis))
      target.pbReduceHP(target.totalhp/8)
      
    elsif target.damageState.iceface
      @battle.pbShowAbilitySplash(target)
      target.pbChangeFormTransform(1,_INTL("{1}变身了！",target.pbThis))
      @battle.pbHideAbilitySplash(target)
  
     elsif target.damageState.flameveil  
    @battle.pbShowAbilitySplash(target)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      @battle.pbDisplay(_INTL("火焰外壳挡住了攻击！"))
    else
      @battle.pbDisplay(_INTL("{1}的火焰外壳挡住了攻击！",target.pbThis))
    end
    @battle.pbHideAbilitySplash(target)
    target.pbChangeFormTransform(1,_INTL("{1}的火焰外壳被破坏了！",target.pbThis))
    # 焚火面纱破坏后的效果
    @battle.pbShowAbilitySplash(target)
    if target.pbCanRaiseStatStage?(PBStats::SPEED,target)
      target.pbRaiseStatStage(PBStats::SPEED,2,target)
      end
    if target.pbCanRaiseStatStage?(PBStats::ATTACK,target)
      target.pbRaiseStatStage(PBStats::ATTACK,1,target)
    end
    @battle.pbHideAbilitySplash(target)


    elsif target.damageState.endured
      @battle.pbDisplay(_INTL("{1}挺住了！",target.pbThis))
    elsif target.damageState.sturdy
      @battle.pbShowAbilitySplash(target)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        @battle.pbDisplay(_INTL("{1}挺住了！",target.pbThis))
      else
        @battle.pbDisplay(_INTL("{1}靠着结实挺住了！",target.pbThis))
      end
      @battle.pbHideAbilitySplash(target)
    elsif target.damageState.focusSash
      @battle.pbCommonAnimation("UseItem",target)
      @battle.pbDisplay(_INTL("{1}靠着气势披带撑住了！",target.pbThis))
      target.pbConsumeItem
    elsif target.damageState.focusBand
      @battle.pbCommonAnimation("UseItem",target)
      @battle.pbDisplay(_INTL("{1}靠着气势头带撑住了！",target.pbThis))
    elsif target.damageState.bossHP == 1
      @battle.pbDisplay(_INTL("<c3=FFEE88,FF6600>{1}减免了溢出的伤害！</c3>",target.pbThis))
      target.damageState.bossHP += 1
    end
  end

  # Used by Counter/Mirror Coat/Metal Burst/Revenge/Focus Punch/Bide/Assurance.
  def pbRecordDamageLost(user,target)
    damage = target.damageState.hpLost
    target.yamaskhp+= damage
    # NOTE: In Gen 3 where a move's category depends on its type, Hidden Power
    #       is for some reason countered by Counter rather than Mirror Coat,
    #       regardless of its calculated type. Hence the following two lines of
    #       code.
    moveType = nil
    moveType = getID(PBTypes,:NORMAL) if @function=="090"   # Hidden Power
    if physicalMove?(moveType)
      target.effects[PBEffects::Counter]       = damage
      target.effects[PBEffects::CounterTarget] = user.index
    elsif specialMove?(moveType)
      target.effects[PBEffects::MirrorCoat]       = damage
      target.effects[PBEffects::MirrorCoatTarget] = user.index
    end
    if target.effects[PBEffects::Bide]>0
      target.effects[PBEffects::BideDamage] += damage
      target.effects[PBEffects::BideTarget] = user.index
    end
    target.damageState.fainted = true if target.fainted?
    target.lastHPLost = damage             # For Focus Punch
    target.tookDamage = true if damage>0   # For Assurance
    target.lastAttacker.push(user.index)   # For Revenge
    if target.opposes?(user)
      target.lastHPLostFromFoe = damage              # For Metal Burst
      target.lastFoeAttacker.push(user.index)        # For Metal Burst
    end
  end
end
