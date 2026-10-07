#===============================================================================
# SpeedCalcAbility handlers
#===============================================================================

BattleHandlers::SpeedCalcAbility.add(:CHLOROPHYLL,
  proc { |ability,battler,mult|
    w = battler.battle.pbWeather
    next mult*2 if w==PBWeather::Sun || w==PBWeather::HarshSun if !battler.hasUtilityUmbrella?
  }
)

BattleHandlers::SpeedCalcAbility.add(:QUICKFEET,
  proc { |ability,battler,mult|
    next mult*1.5 if battler.pbHasAnyStatus?
  }
)

BattleHandlers::SpeedCalcAbility.add(:SANDRUSH,
  proc { |ability,battler,mult|
    w = battler.battle.pbWeather
    next mult*2 if w==PBWeather::Sandstorm
  }
)

BattleHandlers::SpeedCalcAbility.add(:SLOWSTART,
  proc { |ability,battler,mult|
    next mult/2 if battler.effects[PBEffects::SlowStart]>0
  }
)

BattleHandlers::SpeedCalcAbility.add(:SLUSHRUSH,
  proc { |ability,battler,mult|
    w = battler.battle.pbWeather
    next mult*2 if w==PBWeather::Hail || w==PBWeather::Snow
  }
)

BattleHandlers::SpeedCalcAbility.add(:SURGESURFER,
  proc { |ability,battler,mult|
    next mult*2 if battler.battle.field.terrain==PBBattleTerrains::Electric
  }
)

BattleHandlers::SpeedCalcAbility.add(:SWIFTSWIM,
  proc { |ability,battler,mult|
    w = battler.battle.pbWeather
    next mult*2 if w==PBWeather::Rain || w==PBWeather::HeavyRain if !battler.hasUtilityUmbrella?
  }
)

BattleHandlers::SpeedCalcAbility.add(:UNBURDEN,
  proc { |ability,battler,mult|
    next mult*2 if battler.effects[PBEffects::Unburden] && battler.item==0
  }
)

#===============================================================================
# WeightCalcAbility handlers
#===============================================================================

BattleHandlers::WeightCalcAbility.add(:HEAVYMETAL,
  proc { |ability,battler,w|
    next w*2
  }
)

BattleHandlers::WeightCalcAbility.add(:LIGHTMETAL,
  proc { |ability,battler,w|
    next [w/2,1].max
  }
)

BattleHandlers::WeightCalcAbility.add(:ADAPTARMOR,
  proc { |ability,battler,w|
    next w*2
  }
)

#===============================================================================
# AbilityOnHPDroppedBelowHalf handlers
#===============================================================================

BattleHandlers::AbilityOnHPDroppedBelowHalf.add(:EMERGENCYEXIT,
  proc { |ability,battler,battle|
    next false if battler.effects[PBEffects::SkyDrop]>=0 || battler.inTwoTurnAttack?("0CE")   # Sky Drop
    # In wild battles
    if battle.wildBattle?
      next false if battler.opposes? && battle.pbSideBattlerCount(battler.index)>1
      next false if !battle.pbCanRun?(battler.index)
      battle.pbShowAbilitySplash(battler,true)
      battle.pbHideAbilitySplash(battler)
      pbSEPlay("Battle flee")
      battle.pbDisplay(_INTL("{1}逃离战斗了！",battler.pbThis))
      battle.decision = 3   # Escaped
      next true
    end
    # In trainer battles
    next false if battle.pbAllFainted?(battler.idxOpposingSide)
    next false if !battle.pbCanSwitch?(battler.index)   # Battler can't switch out
    next false if !battle.pbCanChooseNonActive?(battler.index)   # No Pokémon can switch in
    battle.pbShowAbilitySplash(battler,true)
    battle.pbHideAbilitySplash(battler)
    if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("{1}的{2}被触发了！",battler.pbThis,battler.abilityName))
    end
    battle.pbDisplay(_INTL("{1}回到了{2}身边！",
       battler.pbThis,battle.pbGetOwnerName(battler.index)))
    if battle.endOfRound   # Just switch out
      battle.scene.pbRecall(battler.index) if !battler.fainted?
      battler.pbAbilitiesOnSwitchOut   # Inc. primordial weather check
      next true
    end
    newPkmn = battle.pbGetReplacementPokemonIndex(battler.index)   # Owner chooses
    next false if newPkmn<0   # Shouldn't ever do this
    battle.pbRecallAndReplace(battler.index,newPkmn)
    battle.pbClearChoice(battler.index)   # Replacement Pokémon does nothing this round
    next true
  }
)

BattleHandlers::AbilityOnHPDroppedBelowHalf.copy(:EMERGENCYEXIT,:WIMPOUT)

#===============================================================================
# StatusCheckAbilityNonIgnorable handlers
#===============================================================================

BattleHandlers::StatusCheckAbilityNonIgnorable.add(:COMATOSE,
  proc { |ability,battler,status|
    next false if !battler.isSpecies?(:KOMALA)
    next true if status.nil? || status==PBStatuses::SLEEP
  }
)

#===============================================================================
# StatusImmunityAbility handlers
#===============================================================================

BattleHandlers::StatusImmunityAbility.add(:FLOWERVEIL,
  proc { |ability,battler,status|
    next true if battler.pbHasType?(:GRASS)
  }
)

BattleHandlers::StatusImmunityAbility.add(:IMMUNITY,
  proc { |ability,battler,status|
    next true if status==PBStatuses::POISON
  }
)

BattleHandlers::StatusImmunityAbility.copy(:IMMUNITY,:PASTELVEIL)

BattleHandlers::StatusImmunityAbility.add(:INSOMNIA,
  proc { |ability,battler,status|
    next true if status==PBStatuses::SLEEP
  }
)

BattleHandlers::StatusImmunityAbility.copy(:INSOMNIA,:SWEETVEIL,:VITALSPIRIT)

BattleHandlers::StatusImmunityAbility.add(:LEAFGUARD,
  proc { |ability,battler,status|
    w = battler.battle.pbWeather
    next true if (w==PBWeather::Sun || w==PBWeather::HarshSun) &&
    !battler.hasUtilityUmbrella?
  }
)

BattleHandlers::StatusImmunityAbility.add(:LIMBER,
  proc { |ability,battler,status|
    next true if status==PBStatuses::PARALYSIS
  }
)

BattleHandlers::StatusImmunityAbility.add(:MAGMAARMOR,
  proc { |ability,battler,status|
    next true if status==PBStatuses::FROZEN
  }
)

BattleHandlers::StatusImmunityAbility.add(:WATERVEIL,
  proc { |ability,battler,status|
    next true if status==PBStatuses::BURN
  }
)

BattleHandlers::StatusImmunityAbility.copy(:WATERVEIL,:WATERBUBBLE)

#===============================================================================
# StatusImmunityAbilityNonIgnorable handlers
#===============================================================================

BattleHandlers::StatusImmunityAbilityNonIgnorable.add(:COMATOSE,
  proc { |ability,battler,status|
    next true if battler.isSpecies?(:KOMALA)
  }
)

BattleHandlers::StatusImmunityAbilityNonIgnorable.add(:SHIELDSDOWN,
  proc { |ability,battler,status|
    next true if battler.isSpecies?(:MINIOR) && battler.form<7
  }
)

#===============================================================================
# StatusImmunityAllyAbility handlers
#===============================================================================

BattleHandlers::StatusImmunityAllyAbility.add(:FLOWERVEIL,
  proc { |ability,battler,status|
    next true if battler.pbHasType?(:GRASS)
  }
)

BattleHandlers::StatusImmunityAllyAbility.add(:SWEETVEIL,
  proc { |ability,battler,status|
    next true if status==PBStatuses::SLEEP
  }
)

BattleHandlers::StatusImmunityAllyAbility.add(:PASTELVEIL,
  proc { |ability,battler,status|
    next true if status==PBStatuses::POISON
  }
)

#===============================================================================
# AbilityOnStatusInflicted handlers
#===============================================================================

BattleHandlers::AbilityOnStatusInflicted.add(:SYNCHRONIZE,
  proc { |ability,battler,user,status|
    next if !user || user.index==battler.index
    case status
    when PBStatuses::POISON
      if user.pbCanPoisonSynchronize?(battler)
        battler.battle.pbShowAbilitySplash(battler)
        msg = nil
        if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
          msg = _INTL("{1}的{2}使{3}中毒了！",battler.pbThis,battler.abilityName,user.pbThis(true))
        end
        user.pbPoison(nil,msg,(battler.statusCount>0))
        battler.battle.pbHideAbilitySplash(battler)
      end
    when PBStatuses::BURN
      if user.pbCanBurnSynchronize?(battler)
        battler.battle.pbShowAbilitySplash(battler)
        msg = nil
        if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
          msg = _INTL("{1}的{2}使{3}灼伤了！",battler.pbThis,battler.abilityName,user.pbThis(true))
        end
        user.pbBurn(nil,msg)
        battler.battle.pbHideAbilitySplash(battler)
      end
    when PBStatuses::PARALYSIS
      if user.pbCanParalyzeSynchronize?(battler)
        battler.battle.pbShowAbilitySplash(battler)
        msg = nil
        if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
          msg = _INTL("{1}的{2}使{3}麻痹了！\n{3}有可能无法行动！",
             battler.pbThis,battler.abilityName,user.pbThis(true))
        end
        user.pbParalyze(nil,msg)
        battler.battle.pbHideAbilitySplash(battler)
      end
    end
  }
)

#===============================================================================
# StatusCureAbility handlers
#===============================================================================

BattleHandlers::StatusCureAbility.add(:IMMUNITY,
  proc { |ability,battler|
    next if battler.status!=PBStatuses::POISON
    battler.battle.pbShowAbilitySplash(battler)
    battler.pbCureStatus(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
    if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battler.battle.pbDisplay(_INTL("{1}的{2}消去了毒！",battler.pbThis,battler.abilityName))
    end
    battler.battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::StatusCureAbility.copy(:IMMUNITY,:PASTELVEIL)

BattleHandlers::StatusCureAbility.add(:INSOMNIA,
  proc { |ability,battler|
    next if battler.status!=PBStatuses::SLEEP
    battler.battle.pbShowAbilitySplash(battler)
    battler.pbCureStatus(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
    if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battler.battle.pbDisplay(_INTL("{1}的{2}使它醒来了！",battler.pbThis,battler.abilityName))
    end
    battler.battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::StatusCureAbility.copy(:INSOMNIA,:VITALSPIRIT)

BattleHandlers::StatusCureAbility.add(:LIMBER,
  proc { |ability,battler|
    next if battler.status!=PBStatuses::PARALYSIS
    battler.battle.pbShowAbilitySplash(battler)
    battler.pbCureStatus(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
    if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battler.battle.pbDisplay(_INTL("{1}的{2}治愈了麻痹！",battler.pbThis,battler.abilityName))
    end
    battler.battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::StatusCureAbility.add(:MAGMAARMOR,
  proc { |ability,battler|
    next if battler.status!=PBStatuses::FROZEN
    battler.battle.pbShowAbilitySplash(battler)
    battler.pbCureStatus(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
    if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battler.battle.pbDisplay(_INTL("{2}解冻了{1}！",battler.pbThis,battler.abilityName))
    end
    battler.battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::StatusCureAbility.add(:OBLIVIOUS,
  proc { |ability,battler|
    next if battler.effects[PBEffects::Attract]<0 &&
            (battler.effects[PBEffects::Taunt]==0 || !NEWEST_BATTLE_MECHANICS)
    battler.battle.pbShowAbilitySplash(battler)
    if battler.effects[PBEffects::Attract]>=0
      battler.pbCureAttract
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battler.battle.pbDisplay(_INTL("{1}不再迷恋对方了！",battler.pbThis))
      else
        battler.battle.pbDisplay(_INTL("{1}的{2}解除了着迷状态！",
           battler.pbThis,battler.abilityName))
      end
    end
    if battler.effects[PBEffects::Taunt]>0 && NEWEST_BATTLE_MECHANICS
      battler.effects[PBEffects::Taunt] = 0
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battler.battle.pbDisplay(_INTL("{1}的挑衅无效了！",battler.pbThis))
      else
        battler.battle.pbDisplay(_INTL("{1}的{2}使挑衅无效了！",
           battler.pbThis,battler.abilityName))
      end
    end
    battler.battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::StatusCureAbility.add(:OWNTEMPO,
  proc { |ability,battler|
    next if battler.effects[PBEffects::Confusion]==0
    battler.battle.pbShowAbilitySplash(battler)
    battler.pbCureConfusion
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battler.battle.pbDisplay(_INTL("{1}解除了混乱！",battler.pbThis))
    else
      battler.battle.pbDisplay(_INTL("{1}的{2}解除了混乱！",
         battler.pbThis,battler.abilityName))
    end
    battler.battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::StatusCureAbility.add(:WATERVEIL,
  proc { |ability,battler|
    next if battler.status!=PBStatuses::BURN
    battler.battle.pbShowAbilitySplash(battler)
    battler.pbCureStatus(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
    if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battler.battle.pbDisplay(_INTL("{1}的{2}治愈了灼伤！",battler.pbThis,battler.abilityName))
    end
    battler.battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::StatusCureAbility.copy(:WATERVEIL,:WATERBUBBLE,:DEEPSEAFASCINATION)

#===============================================================================
# StatLossImmunityAbility handlers
#===============================================================================

BattleHandlers::StatLossImmunityAbility.add(:BIGPECKS,
  proc { |ability,battler,stat,battle,showMessages|
    next false if stat!=PBStats::DEFENSE
    if showMessages
      battle.pbShowAbilitySplash(battler)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的{2}不能被降低了！",battler.pbThis,PBStats.getName(stat)))
      else
        battle.pbDisplay(_INTL("{1}的{2}防止了{3}遗失！",battler.pbThis,
           battler.abilityName,PBStats.getName(stat)))
      end
      battle.pbHideAbilitySplash(battler)
    end
    next true
  }
)

BattleHandlers::StatLossImmunityAbility.add(:CLEARBODY,
  proc { |ability,battler,stat,battle,showMessages|
    if showMessages
      battle.pbShowAbilitySplash(battler)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的能力值不能被降低了！",battler.pbThis))
      else
        battle.pbDisplay(_INTL("{1}的{2}防止了能力值降低！",battler.pbThis,battler.abilityName))
      end
      battle.pbHideAbilitySplash(battler)
    end
    next true
  }
)

BattleHandlers::StatLossImmunityAbility.copy(:CLEARBODY,:WHITESMOKE,:NOBLESTRIKE,:ETERNALSTAR,:TRANSLUCENTGHOST)

BattleHandlers::StatLossImmunityAbility.add(:FLOWERVEIL,
  proc { |ability,battler,stat,battle,showMessages|
    next false if !battler.pbHasType?(:GRASS)
    if showMessages
      battle.pbShowAbilitySplash(battler)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的能力值不能被降低了！",battler.pbThis))
      else
        battle.pbDisplay(_INTL("{1}的{2}防止了能力值降低！",battler.pbThis,battler.abilityName))
      end
      battle.pbHideAbilitySplash(battler)
    end
    next true
  }
)

BattleHandlers::StatLossImmunityAbility.add(:HYPERCUTTER,
  proc { |ability,battler,stat,battle,showMessages|
    next false if stat!=PBStats::ATTACK
    if showMessages
      battle.pbShowAbilitySplash(battler)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的{2}不能被降低了！",battler.pbThis,PBStats.getName(stat)))
      else
        battle.pbDisplay(_INTL("{1}的{2}防止了{3}遗失！",battler.pbThis,
           battler.abilityName,PBStats.getName(stat)))
      end
      battle.pbHideAbilitySplash(battler)
    end
    next true
  }
)

BattleHandlers::StatLossImmunityAbility.add(:KEENEYE,
  proc { |ability,battler,stat,battle,showMessages|
    next false if stat!=PBStats::ACCURACY
    if showMessages
      battle.pbShowAbilitySplash(battler)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的{2}不能被降低了！",battler.pbThis,PBStats.getName(stat)))
      else
        battle.pbDisplay(_INTL("{1}的{2}防止了{3}遗失！",battler.pbThis,
           battler.abilityName,PBStats.getName(stat)))
      end
      battle.pbHideAbilitySplash(battler)
    end
    next true
  }
)
BattleHandlers::StatLossImmunityAbility.copy(:KEENEYE,:ROSYAEGIS)

#===============================================================================
# StatLossImmunityAbilityNonIgnorable handlers
#===============================================================================

BattleHandlers::StatLossImmunityAbilityNonIgnorable.add(:FULLMETALBODY,
  proc { |ability,battler,stat,battle,showMessages|
    if showMessages
      battle.pbShowAbilitySplash(battler)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的能力值不能被降低了！",battler.pbThis))
      else
        battle.pbDisplay(_INTL("{1}的{2}防止了能力值降低！",battler.pbThis,battler.abilityName))
      end
      battle.pbHideAbilitySplash(battler)
    end
    next true
  }
)

#===============================================================================
# StatLossImmunityAllyAbility handlers
#===============================================================================

BattleHandlers::StatLossImmunityAllyAbility.add(:FLOWERVEIL,
  proc { |ability,bearer,battler,stat,battle,showMessages|
    next false if !battler.pbHasType?(:GRASS)
    if showMessages
      battle.pbShowAbilitySplash(bearer)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的能力值不能被降低了！",battler.pbThis))
      else
        battle.pbDisplay(_INTL("{1}的{2}防止了{3}的能力值降低！",
           bearer.pbThis,bearer.abilityName,battler.pbThis(true)))
      end
      battle.pbHideAbilitySplash(bearer)
    end
    next true
  }
)

#===============================================================================
# AbilityOnStatGain handlers
#===============================================================================

# There aren't any!

#===============================================================================
# AbilityOnStatLoss handlers
#===============================================================================

BattleHandlers::AbilityOnStatLoss.add(:COMPETITIVE,
  proc { |ability,battler,stat,user|
    next if user && !user.opposes?(battler)
    battler.pbRaiseStatStageByAbility(PBStats::SPATK,2,battler)
  }
)

BattleHandlers::AbilityOnStatLoss.add(:DEFIANT,
  proc { |ability,battler,stat,user|
    next if user && !user.opposes?(battler)
    battler.pbRaiseStatStageByAbility(PBStats::ATTACK,2,battler)
  }
)

#===============================================================================
# PriorityChangeAbility handlers
#===============================================================================

BattleHandlers::PriorityChangeAbility.add(:GALEWINGS,
  proc { |ability,battler,move,pri|
    next pri+1 if battler.hp==battler.totalhp && isConst?(move.type,PBTypes,:FLYING)
  }
)

BattleHandlers::PriorityChangeAbility.add(:PRANKSTER,
  proc { |ability,battler,move,pri|
    if move.statusMove?
      battler.effects[PBEffects::Prankster] = true
      next pri+1
    end
  }
)


BattleHandlers::PriorityChangeAbility.add(:TRIAGE,
  proc { |ability,battler,move,pri|
    next pri+3 if move.healingMove?
  }
)

#===============================================================================
# PriorityBracketChangeAbility handlers
#===============================================================================

BattleHandlers::PriorityBracketChangeAbility.add(:STALL,
  proc { |ability,battler,subPri,battle|
    next -1 if subPri==0
  }
)

BattleHandlers::PriorityBracketChangeAbility.add(:QUICKDRAW,
  proc { |ability,battler,subPri,battle|
    next 1 if subPri<1 && battle.pbRandom(10)<3
  }
)

#===============================================================================
# PriorityBracketUseAbility handlers
#===============================================================================

BattleHandlers::PriorityBracketUseAbility.add(:QUICKDRAW,
  proc { |ability,battler,battle|
    battle.pbDisplay(_INTL("{2}使{1}可以先使用技能！",battler.pbThis,battler.abilityName))
  }
)

#===============================================================================
# AbilityOnFlinch handlers
#===============================================================================

BattleHandlers::AbilityOnFlinch.add(:STEADFAST,
  proc { |ability,battler,battle|
    battler.pbRaiseStatStageByAbility(PBStats::SPEED,1,battler)
  }
)

#===============================================================================
# MoveBlockingAbility handlers
#===============================================================================

BattleHandlers::MoveBlockingAbility.add(:DAZZLING,
  proc { |ability,bearer,user,targets,move,battle|
    next false if battle.choices[user.index][4]<=0
    next false if !bearer.opposes?(user)
    ret = false
    targets.each do |b|
      next if !b.opposes?(user)
      ret = true
    end
    next ret
  }
)

BattleHandlers::MoveBlockingAbility.copy(:DAZZLING,:QUEENLYMAJESTY)

#===============================================================================
# MoveImmunityTargetAbility handlers
#===============================================================================

BattleHandlers::MoveImmunityTargetAbility.add(:BULLETPROOF,
  proc { |ability,user,target,move,type,battle|
    next false if !move.bombMove?
    battle.pbShowAbilitySplash(target)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("这不能影响{1}……",target.pbThis(true)))
    else
      battle.pbDisplay(_INTL("{1}的{2}使{3}无效了！",
         target.pbThis,target.abilityName,move.name))
    end
    battle.pbHideAbilitySplash(target)
    next true
  }
)

BattleHandlers::MoveImmunityTargetAbility.add(:FLASHFIRE,
  proc { |ability,user,target,move,type,battle|
    next false if user.index==target.index
    next false if !isConst?(type,PBTypes,:FIRE)
    battle.pbShowAbilitySplash(target)
    if !target.effects[PBEffects::FlashFire]
      target.effects[PBEffects::FlashFire] = true
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的火属性招式威力上升！",target.pbThis(true)))
      else
        battle.pbDisplay(_INTL("{2}使得{1}\n火属性招式的威力上升了！",
           target.pbThis(true),target.abilityName))
      end
    else
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("这不能影响{1}……",target.pbThis(true)))
      else
        battle.pbDisplay(_INTL("{1}的{2}使{3}无效了！",
           target.pbThis,target.abilityName,move.name))
      end
    end
    battle.pbHideAbilitySplash(target)
    next true
  }
)

BattleHandlers::MoveImmunityTargetAbility.add(:LIGHTNINGROD,
  proc { |ability,user,target,move,type,battle|
    next pbBattleMoveImmunityStatAbility(user,target,move,type,:ELECTRIC,PBStats::SPATK,1,battle)
  }
)

BattleHandlers::MoveImmunityTargetAbility.add(:MOTORDRIVE,
  proc { |ability,user,target,move,type,battle|
    next pbBattleMoveImmunityStatAbility(user,target,move,type,:ELECTRIC,PBStats::SPEED,1,battle)
  }
)

BattleHandlers::MoveImmunityTargetAbility.add(:SAPSIPPER,
  proc { |ability,user,target,move,type,battle|
    next pbBattleMoveImmunityStatAbility(user,target,move,type,:GRASS,PBStats::ATTACK,1,battle)
  }
)

BattleHandlers::MoveImmunityTargetAbility.add(:SOUNDPROOF,
  proc { |ability,user,target,move,type,battle|
    next false if !move.soundMove?
    battle.pbShowAbilitySplash(target)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("这不能影响{1}……",target.pbThis(true)))
    else
      battle.pbDisplay(_INTL("{1}的{2}阻止了{3}！",target.pbThis,target.abilityName,move.name))
    end
    battle.pbHideAbilitySplash(target)
    next true

  }
)

BattleHandlers::MoveImmunityTargetAbility.add(:STORMDRAIN,
  proc { |ability,user,target,move,type,battle|
    next pbBattleMoveImmunityStatAbility(user,target,move,type,:WATER,PBStats::SPATK,1,battle)
  }
)

BattleHandlers::MoveImmunityTargetAbility.add(:TELEPATHY,
  proc { |ability,user,target,move,type,battle|
    next false if move.statusMove?
    next false if user.index==target.index || target.opposes?(user)
    battle.pbShowAbilitySplash(target)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("{1}避免了友方的攻击！",target.pbThis(true)))
    else
      battle.pbDisplay(_INTL("{1}因为{2}避免了友方的攻击！",
         target.pbThis,target.abilityName))
    end
    battle.pbHideAbilitySplash(target)
    next true
  }
)

BattleHandlers::MoveImmunityTargetAbility.add(:VOLTABSORB,
  proc { |ability,user,target,move,type,battle|
    next pbBattleMoveImmunityHealAbility(user,target,move,type,:ELECTRIC,battle)
  }
)

BattleHandlers::MoveImmunityTargetAbility.add(:WATERABSORB,
  proc { |ability,user,target,move,type,battle|
    next pbBattleMoveImmunityHealAbility(user,target,move,type,:WATER,battle)
  }
)
#霜食
BattleHandlers::MoveImmunityTargetAbility.add(:ICEBSORB,
  proc { |ability,user,target,move,type,battle|
    next pbBattleMoveImmunityHealAbility(user,target,move,type,:ICE,battle)
  }
)

BattleHandlers::MoveImmunityTargetAbility.copy(:WATERABSORB,:DRYSKIN)

BattleHandlers::MoveImmunityTargetAbility.add(:WONDERGUARD,
  proc { |ability,user,target,move,type,battle|
    next false if move.statusMove?
    next false if type<0 || PBTypes.superEffective?(target.damageState.typeMod)
    battle.pbShowAbilitySplash(target)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("这不能影响{1}……",target.pbThis(true)))
    else
      battle.pbDisplay(_INTL("{1}因为{2}避免了伤害！",target.pbThis,target.abilityName))
    end
    battle.pbHideAbilitySplash(target)
    next true
  }
)

#===============================================================================
# MoveBaseTypeModifierAbility handlers
#===============================================================================

BattleHandlers::MoveBaseTypeModifierAbility.add(:AERILATE,
  proc { |ability,user,move,type|
    next if !isConst?(type,PBTypes,:NORMAL) || !hasConst?(PBTypes,:FLYING)
    move.powerBoost = true
    next getConst(PBTypes,:FLYING)
  }
)

BattleHandlers::MoveBaseTypeModifierAbility.add(:GALVANIZE,
  proc { |ability,user,move,type|
    next if !isConst?(type,PBTypes,:NORMAL) || !hasConst?(PBTypes,:ELECTRIC)
    move.powerBoost = true
    next getConst(PBTypes,:ELECTRIC)
  }
)

BattleHandlers::MoveBaseTypeModifierAbility.add(:LIQUIDVOICE,
  proc { |ability,user,move,type|
    next getConst(PBTypes,:WATER) if hasConst?(PBTypes,:WATER) && move.soundMove?
  }
)

BattleHandlers::MoveBaseTypeModifierAbility.add(:NORMALIZE,
  proc { |ability,user,move,type|
    next if !hasConst?(PBTypes,:NORMAL)
    move.powerBoost = true if NEWEST_BATTLE_MECHANICS
    next getConst(PBTypes,:NORMAL)
  }
)

BattleHandlers::MoveBaseTypeModifierAbility.add(:PIXILATE,
  proc { |ability,user,move,type|
    next if !isConst?(type,PBTypes,:NORMAL) || !hasConst?(PBTypes,:FAIRY)
    move.powerBoost = true
    next getConst(PBTypes,:FAIRY)
  }
)

BattleHandlers::MoveBaseTypeModifierAbility.add(:REFRIGERATE,
  proc { |ability,user,move,type|
    next if !isConst?(type,PBTypes,:NORMAL) || !hasConst?(PBTypes,:ICE)
    move.powerBoost = true
    next getConst(PBTypes,:ICE)
  }
)


BattleHandlers::MoveBaseTypeModifierAbility.add(:DRAGONSKIN,
  proc { |ability,user,move,type|
    next if !isConst?(type,PBTypes,:NORMAL) || !hasConst?(PBTypes,:DRAGON)
    move.powerBoost = true
    next getConst(PBTypes,:DRAGON)
  }
)
#===============================================================================
# AccuracyCalcUserAbility handlers
#===============================================================================

BattleHandlers::AccuracyCalcUserAbility.add(:COMPOUNDEYES,
  proc { |ability,mods,user,target,move,type|
    mods[ACC_MULT] *= 1.3
  }
)

BattleHandlers::AccuracyCalcUserAbility.add(:HUSTLE,
  proc { |ability,mods,user,target,move,type|
    mods[ACC_MULT] *= 0.8 if move.physicalMove?
  }
)

BattleHandlers::AccuracyCalcUserAbility.add(:KEENEYE,
  proc { |ability,mods,user,target,move,type|
    mods[EVA_STAGE] = 0 if mods[EVA_STAGE]>0 && NEWEST_BATTLE_MECHANICS
  }
)
#蔷薇庇护
BattleHandlers::AccuracyCalcUserAbility.add(:ROSYAEGIS,
  proc { |ability,mods,user,target,move,type|
    mods[EVA_STAGE] = 0 if mods[EVA_STAGE]>0 && NEWEST_BATTLE_MECHANICS
  }
)
BattleHandlers::AccuracyCalcUserAbility.add(:NOGUARD,
  proc { |ability,mods,user,target,move,type|
    mods[BASE_ACC] = 0
  }
)

BattleHandlers::AccuracyCalcUserAbility.add(:UNAWARE,
  proc { |ability,mods,user,target,move,type|
    mods[EVA_STAGE] = 0 if move.damagingMove?
  }
)

BattleHandlers::AccuracyCalcUserAbility.add(:VICTORYSTAR,
  proc { |ability,mods,user,target,move,type|
    mods[ACC_MULT] *= 1.1
  }
)

#===============================================================================
# AccuracyCalcUserAllyAbility handlers
#===============================================================================

BattleHandlers::AccuracyCalcUserAllyAbility.add(:VICTORYSTAR,
  proc { |ability,mods,user,target,move,type|
    mods[ACC_MULT] *= 1.1
  }
)

#===============================================================================
# AccuracyCalcTargetAbility handlers
#===============================================================================

BattleHandlers::AccuracyCalcTargetAbility.add(:LIGHTNINGROD,
  proc { |ability,mods,user,target,move,type|
    mods[BASE_ACC] = 0 if isConst?(type,PBTypes,:ELECTRIC)
  }
)

BattleHandlers::AccuracyCalcTargetAbility.add(:NOGUARD,
  proc { |ability,mods,user,target,move,type|
    mods[BASE_ACC] = 0
  }
)

BattleHandlers::AccuracyCalcTargetAbility.add(:SANDVEIL,
  proc { |ability,mods,user,target,move,type|
    if target.battle.pbWeather==PBWeather::Sandstorm
      mods[EVA_MULT] *= 1.25
    end
  }
)

BattleHandlers::AccuracyCalcTargetAbility.add(:SNOWCLOAK,
  proc { |ability,mods,user,target,move,type|
    if target.battle.pbWeather==PBWeather::Hail || target.battle.pbWeather==PBWeather::Snow
      mods[EVA_MULT] *= 1.2
    end
  }
)

BattleHandlers::AccuracyCalcTargetAbility.add(:STORMDRAIN,
  proc { |ability,mods,user,target,move,type|
    mods[BASE_ACC] = 0 if isConst?(type,PBTypes,:WATER)
  }
)

BattleHandlers::AccuracyCalcTargetAbility.add(:TANGLEDFEET,
  proc { |ability,mods,user,target,move,type|
    mods[ACC_MULT] /= 2 if target.effects[PBEffects::Confusion]>0
  }
)

BattleHandlers::AccuracyCalcTargetAbility.add(:UNAWARE,
  proc { |ability,mods,user,target,move,type|
    mods[ACC_STAGE] = 0 if move.damagingMove?
  }
)

BattleHandlers::AccuracyCalcTargetAbility.add(:WONDERSKIN,
  proc { |ability,mods,user,target,move,type|
    if move.statusMove? && user.opposes?(target)
      mods[BASE_ACC] = 0 if mods[BASE_ACC]>50
    end
  }
)

#===============================================================================
# DamageCalcUserAbility handlers
#===============================================================================

BattleHandlers::DamageCalcUserAbility.add(:AERILATE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 1.2 if move.powerBoost
  }
)

BattleHandlers::DamageCalcUserAbility.copy(:AERILATE,:PIXILATE,:REFRIGERATE,:GALVANIZE,:DRAGONSKIN)

BattleHandlers::DamageCalcUserAbility.add(:ANALYTIC,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if (target.battle.choices[target.index][0]!=:UseMove &&
       target.battle.choices[target.index][0]!=:Shift) ||
       target.movedThisRound?
      mults[BASE_DMG_MULT] *= 1.3
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:BLAZE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.hp<=user.totalhp/3 && isConst?(type,PBTypes,:FIRE)
      mults[ATK_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:DEFEATIST,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] /= 2 if user.hp<=user.totalhp/2
  }
)



BattleHandlers::DamageCalcUserAbility.add(:FLAREBOOST,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.burned? && move.specialMove?
      mults[BASE_DMG_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:FLASHFIRE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.effects[PBEffects::FlashFire] && isConst?(type,PBTypes,:FIRE)
      mults[ATK_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:FLOWERGIFT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    w = user.battle.pbWeather
    if move.physicalMove? && (w==PBWeather::Sun || w==PBWeather::HarshSun) &&
    !target.hasUtilityUmbrella?
      mults[ATK_MULT] = (mults[ATK_MULT]*1.5).round
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:GUTS,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.pbHasAnyStatus? && move.physicalMove?
      mults[ATK_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:HUGEPOWER,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] *= 2 if move.physicalMove?
  }
)

BattleHandlers::DamageCalcUserAbility.copy(:HUGEPOWER,:PUREPOWER,:SAVAGECEREMONY)

BattleHandlers::DamageCalcUserAbility.add(:HUSTLE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] *= 1.5 if move.physicalMove?
  }
)

BattleHandlers::DamageCalcUserAbility.add(:IRONFIST,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 1.2 if move.punchingMove?
  }
)
BattleHandlers::DamageCalcUserAbility.copy(:IRONFIST,:SHATTERFIST)

BattleHandlers::DamageCalcUserAbility.add(:MEGALAUNCHER,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 1.5 if move.pulseMove?
  }
)

BattleHandlers::DamageCalcUserAbility.add(:MINUS,
  proc { |ability,user,target,move,mults,baseDmg,type|
    next if !move.specialMove?
    user.eachAlly do |b|
      next if !b.hasActiveAbility?([:MINUS,:PLUS])
      mults[ATK_MULT] *= 1.5
      break
    end
  }
)

BattleHandlers::DamageCalcUserAbility.copy(:MINUS,:PLUS)

BattleHandlers::DamageCalcUserAbility.add(:NEUROFORCE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if PBTypes.superEffective?(target.damageState.typeMod)
      mults[FINAL_DMG_MULT] *= 1.25
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:OVERGROW,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.hp<=user.totalhp/3 && isConst?(type,PBTypes,:GRASS)
      mults[ATK_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:RECKLESS,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 1.2 if move.recoilMove?
  }
)

BattleHandlers::DamageCalcUserAbility.add(:RIVALRY,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.gender!=2 && target.gender!=2
      if user.gender==target.gender
        mults[BASE_DMG_MULT] *= 1.25
      else
        mults[BASE_DMG_MULT] *= 0.75
      end
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:SANDFORCE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.battle.pbWeather==PBWeather::Sandstorm &&
       (isConst?(type,PBTypes,:ROCK) ||
       isConst?(type,PBTypes,:GROUND) ||
       isConst?(type,PBTypes,:STEEL))
      mults[BASE_DMG_MULT] *= 1.3
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:SHEERFORCE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 1.3 if move.addlEffect>0
  }
)

BattleHandlers::DamageCalcUserAbility.add(:SLOWSTART,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] /= 2 if user.effects[PBEffects::SlowStart]>0 && move.physicalMove?
  }
)

BattleHandlers::DamageCalcUserAbility.add(:SOLARPOWER,
  proc { |ability,user,target,move,mults,baseDmg,type|
    w = user.battle.pbWeather
    if move.specialMove? && (w==PBWeather::Sun || w==PBWeather::HarshSun) &&
      !target.hasUtilityUmbrella?
      mults[ATK_MULT] = (mults[ATK_MULT]*1.5).round
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:SNIPER,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if target.damageState.critical
      mults[FINAL_DMG_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:STAKEOUT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] *= 2 if target.battle.choices[target.index][0]==:SwitchOut
  }
)

BattleHandlers::DamageCalcUserAbility.add(:STEELWORKER,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] *= 1.5 if isConst?(type,PBTypes,:STEEL)
  }
)

BattleHandlers::DamageCalcUserAbility.add(:STRONGJAW,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 1.5 if move.bitingMove?
  }
)

BattleHandlers::DamageCalcUserAbility.add(:SWARM,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.hp<=user.totalhp/3 && isConst?(type,PBTypes,:BUG)
      mults[ATK_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:TECHNICIAN,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.index!=target.index && move.id>0 && baseDmg*mults[BASE_DMG_MULT]<=60
      mults[BASE_DMG_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:TINTEDLENS,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[FINAL_DMG_MULT] *= 2 if PBTypes.resistant?(target.damageState.typeMod)
  }
)

BattleHandlers::DamageCalcUserAbility.add(:TORRENT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.hp<=user.totalhp/3 && isConst?(type,PBTypes,:WATER)
      mults[ATK_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:TOUGHCLAWS,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 4/3.0 if move.contactMove?
  }
)

BattleHandlers::DamageCalcUserAbility.add(:TOXICBOOST,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.poisoned? && move.physicalMove?
      mults[BASE_DMG_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcUserAbility.add(:WATERBUBBLE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] *= 2 if isConst?(type,PBTypes,:WATER)
  }
)

BattleHandlers::DamageCalcUserAbility.add(:GORILLATACTICS,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] = (mults[ATK_MULT]*1.5).round if move.physicalMove?
  }
)

BattleHandlers::DamageCalcUserAbility.add(:PUNKROCK,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] = (mults[BASE_DMG_MULT]*1.3).round if move.soundMove?
  }
)

BattleHandlers::DamageCalcUserAbility.add(:STEELYSPIRIT,
  proc { |ability,user,target,move,mults,baseDmg,type|
      mults[BASE_DMG_MULT] = (mults[BASE_DMG_MULT]*1.5).round if isConst?(type,PBTypes,:STEEL)
  }
)

BattleHandlers::DamageCalcUserAbility.add(:DRAGONSMAW,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] = (mults[ATK_MULT]*1.5) if isConst?(type,PBTypes,:DRAGON)
  }
)

BattleHandlers::DamageCalcUserAbility.add(:TRANSISTOR,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] = (mults[ATK_MULT]*1.5) if isConst?(type,PBTypes,:ELECTRIC)
  }
)

#===============================================================================
# DamageCalcUserAllyAbility handlers
#===============================================================================

BattleHandlers::DamageCalcUserAllyAbility.add(:BATTERY,
  proc { |ability,user,target,move,mults,baseDmg,type|
    next if !move.specialMove?
    mults[FINAL_DMG_MULT] *= 1.3
  }
)

BattleHandlers::DamageCalcUserAllyAbility.add(:FLOWERGIFT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    w = user.battle.pbWeather
    if move.physicalMove? && (w==PBWeather::Sun || w==PBWeather::HarshSun) &&
      !target.hasUtilityUmbrella?
      mults[ATK_MULT] = (mults[ATK_MULT]*1.5).round
    end
  }
)

BattleHandlers::DamageCalcUserAllyAbility.add(:POWERSPOT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[FINAL_DMG_MULT] = (mults[FINAL_DMG_MULT]*1.3).round
  }
)

BattleHandlers::DamageCalcUserAllyAbility.add(:STEELYSPIRIT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] = (mults[BASE_DMG_MULT]*1.5).round if isConst?(type,PBTypes,:STEEL)
  }
)

#===============================================================================
# DamageCalcTargetAbility handlers
#===============================================================================

BattleHandlers::DamageCalcTargetAbility.add(:DRYSKIN,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if isConst?(type,PBTypes,:FIRE)
      mults[BASE_DMG_MULT] *= 1.25
    end
  }
)

BattleHandlers::DamageCalcTargetAbility.add(:FILTER,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if PBTypes.superEffective?(target.damageState.typeMod)
      mults[FINAL_DMG_MULT] *= 0.75
    end
  }
)

#======================适应装甲======================
BattleHandlers::DamageCalcTargetAbility.add(:ADAPTARMOR,
  proc { |ability,user,target,move,mults,baseDmg,type|
    type3 = target.effects[PBEffects::Type3]<0 ? nil : target.effects[PBEffects::Type3]
    mod = PBTypes.getCombinedEffectiveness(type,target.type1,target.type2,type3) / 8.0
    mults[FINAL_DMG_MULT] /= mod if mod>1.0
  }
)
#======================无限之光======================
BattleHandlers::DamageCalcTargetAbility.add(:ETRTNALIGHT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    type3 = target.effects[PBEffects::Type3]<0 ? nil : target.effects[PBEffects::Type3]
    mod = PBTypes.getCombinedEffectiveness(type,target.type1,target.type2,type3) / 8.0
    mults[FINAL_DMG_MULT] /= mod if mod>1.0
  }
)


BattleHandlers::DamageCalcTargetAbility.copy(:FILTER,:SOLIDROCK)

BattleHandlers::DamageCalcTargetAbility.add(:FLOWERGIFT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    w = user.battle.pbWeather
    if move.specialMove? && (w==PBWeather::Sun || w==PBWeather::HarshSun) &&
      !target.hasUtilityUmbrella?
      mults[DEF_MULT] = (mults[DEF_MULT]*1.5).round
    end
  }
)

BattleHandlers::DamageCalcTargetAbility.add(:FLUFFY,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[FINAL_DMG_MULT] *= 2 if isConst?(move.calcType,PBTypes,:FIRE)
    mults[FINAL_DMG_MULT] /= 2 if move.contactMove?
  }
)

BattleHandlers::DamageCalcTargetAbility.add(:FURCOAT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[DEF_MULT] *= 2 if move.physicalMove? || move.function=="122"   # Psyshock
  }
)


  BattleHandlers::DamageCalcTargetAbility.add(:ICESCALES,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[FINAL_DMG_MULT] /= 2 if move.specialMove?
  }
)

BattleHandlers::DamageCalcTargetAbility.add(:GRASSPELT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.battle.field.terrain==PBBattleTerrains::Grassy
      mults[DEF_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcTargetAbility.add(:HEATPROOF,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] /= 2 if isConst?(type,PBTypes,:FIRE)
  }
)

BattleHandlers::DamageCalcTargetAbility.add(:MARVELSCALE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if target.pbHasAnyStatus? && move.physicalMove?
      mults[DEF_MULT] *= 1.5
    end
  }
)

BattleHandlers::DamageCalcTargetAbility.add(:MULTISCALE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if target.hp==target.totalhp
      mults[FINAL_DMG_MULT] /= 2
    end
  }
)

BattleHandlers::DamageCalcTargetAbility.add(:THICKFAT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if isConst?(type,PBTypes,:FIRE) || isConst?(type,PBTypes,:ICE)
      mults[BASE_DMG_MULT] /= 2
    end
  }
)

BattleHandlers::DamageCalcTargetAbility.add(:WATERBUBBLE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if isConst?(type,PBTypes,:FIRE)
      mults[FINAL_DMG_MULT] /= 2
    end
  }
)

BattleHandlers::DamageCalcTargetAbility.add(:PUNKROCK,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[FINAL_DMG_MULT] /= 2 if move.soundMove?
  }
)

#===============================================================================
# DamageCalcTargetAbilityNonIgnorable handlers
#===============================================================================

BattleHandlers::DamageCalcTargetAbilityNonIgnorable.add(:PRISMARMOR,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if PBTypes.superEffective?(target.damageState.typeMod)
      mults[FINAL_DMG_MULT] *= 0.75
    end
  }
)

BattleHandlers::DamageCalcTargetAbilityNonIgnorable.add(:SHADOWSHIELD,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if target.hp==target.totalhp
      mults[FINAL_DMG_MULT] /= 2
    end
  }
)

#===============================================================================
# DamageCalcTargetAllyAbility handlers
#===============================================================================

BattleHandlers::DamageCalcTargetAllyAbility.add(:FLOWERGIFT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    w = user.battle.pbWeather
    if move.specialMove? && (w==PBWeather::Sun || w==PBWeather::HarshSun) &&
      !target.hasUtilityUmbrella?
      mults[DEF_MULT] = (mults[DEF_MULT]*1.5).round
    end
  }
)

BattleHandlers::DamageCalcTargetAllyAbility.add(:FRIENDGUARD,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[FINAL_DMG_MULT] *= 0.75
  }
)

#===============================================================================
# CriticalCalcUserAbility handlers
#===============================================================================

BattleHandlers::CriticalCalcUserAbility.add(:MERCILESS,
  proc { |ability,user,target,c|
    next 99 if target.poisoned?
  }
)

BattleHandlers::CriticalCalcUserAbility.add(:SUPERLUCK,
  proc { |ability,user,target,c|
    next c+1
  }
)

#===============================================================================
# CriticalCalcTargetAbility handlers
#===============================================================================

BattleHandlers::CriticalCalcTargetAbility.add(:BATTLEARMOR,
  proc { |ability,user,target,c|
    next -1
  }
)

BattleHandlers::CriticalCalcTargetAbility.copy(:BATTLEARMOR,:SHELLARMOR,:NOBLESTRIKE,:RUYIBLADE)

#===============================================================================
# TargetAbilityOnHit handlers
#===============================================================================

BattleHandlers::TargetAbilityOnHit.add(:AFTERMATH,
  proc { |ability,user,target,move,battle|
    next if !target.fainted?
    next if !move.pbContactMove?(user)
    battle.pbShowAbilitySplash(target)
    if !battle.moldBreaker
      dampBattler = battle.pbCheckGlobalAbility(:DAMP)
      if dampBattler
        battle.pbShowAbilitySplash(dampBattler)
        if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
          battle.pbDisplay(_INTL("{1}不能使用{2}了！",target.pbThis,target.abilityName))
        else
          battle.pbDisplay(_INTL("{3}的{4}使得{1}无法使用{2}了！",
             target.pbThis,target.abilityName,dampBattler.pbThis(true),dampBattler.abilityName))
        end
        battle.pbHideAbilitySplash(dampBattler)
        battle.pbHideAbilitySplash(target)
        next
      end
    end
    if user.takesIndirectDamage?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH) &&
       user.affectedByContactEffect?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      battle.scene.pbDamageAnimation(user)
      user.pbReduceHP(user.totalhp/4,false)
      battle.pbDisplay(_INTL("{1}陷入了爆炸中！",user.pbThis))
    end
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:ANGERPOINT,
  proc { |ability,user,target,move,battle|
    next if !target.damageState.critical
    next if !target.pbCanRaiseStatStage?(PBStats::ATTACK,target)
    battle.pbShowAbilitySplash(target)
    target.stages[PBStats::ATTACK] = 6
    target.statsRaisedThisRound = true
    battle.pbCommonAnimation("StatUp",target)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("{1}最大化了{2}！",target.pbThis,PBStats.getName(PBStats::ATTACK)))
    else
      battle.pbDisplay(_INTL("{1}的{2}最大化了{3}！",
         target.pbThis,target.abilityName,PBStats.getName(PBStats::ATTACK)))
    end
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:CURSEDBODY,
  proc { |ability,user,target,move,battle|
    next if user.fainted?
    next if user.effects[PBEffects::Disable]>0
    regularMove = nil
    user.eachMove do |m|
      next if m.id!=user.lastRegularMoveUsed
      regularMove = m
      break
    end
    next if !regularMove || (regularMove.pp==0 && regularMove.totalpp>0)
    next if battle.pbRandom(100)>=30
    battle.pbShowAbilitySplash(target)
    if !move.pbMoveFailedAromaVeil?(target,user,PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      user.effects[PBEffects::Disable]     = 3
      user.effects[PBEffects::DisableMove] = regularMove.id
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的{2}被禁用了！",user.pbThis,regularMove.name))
      else
        battle.pbDisplay(_INTL("{1}的{2}被{3}的{4}禁用了！",
           user.pbThis,regularMove.name,target.pbThis(true),target.abilityName))
      end
      battle.pbHideAbilitySplash(target)
      user.pbItemStatusCureCheck
    end
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:CUTECHARM,
  proc { |ability,user,target,move,battle|
    next if target.fainted?
    next if !move.pbContactMove?(user)
    next if battle.pbRandom(100)>=30
    battle.pbShowAbilitySplash(target)
    if user.pbCanAttract?(target,PokeBattle_SceneConstants::USE_ABILITY_SPLASH) &&
       user.affectedByContactEffect?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      msg = nil
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        msg = _INTL("{1}的{2}让{3}坠入爱河了！",target.pbThis,
           target.abilityName,user.pbThis(true))
      end
      user.pbAttract(target,msg)
    end
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:EFFECTSPORE,
  proc { |ability,user,target,move,battle|
    # NOTE: This ability has a 30% chance of triggering, not a 30% chance of
    #       inflicting a status condition. It can try (and fail) to inflict a
    #       status condition that the user is immune to.
    next if !move.pbContactMove?(user)
    next if battle.pbRandom(100)>=30
    r = battle.pbRandom(3)
    next if r==0 && user.asleep?
    next if r==1 && user.poisoned?
    next if r==2 && user.paralyzed?
    battle.pbShowAbilitySplash(target)
    if user.affectedByPowder?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH) &&
       user.affectedByContactEffect?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      case r
      when 0
        if user.pbCanSleep?(target,PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
          msg = nil
          if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
            msg = _INTL("{1}的{2}让{3}睡着了！",target.pbThis,
               target.abilityName,user.pbThis(true))
          end
          user.pbSleep(msg)
        end
      when 1
        if user.pbCanPoison?(target,PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
          msg = nil
          if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
            msg = _INTL("{1}的{2}使{3}中毒了！",target.pbThis,
               target.abilityName,user.pbThis(true))
          end
          user.pbPoison(target,msg)
        end
      when 2
        if user.pbCanParalyze?(target,PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
          msg = nil
          if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
            msg = _INTL("{1}的{2}使{3}麻痹了！\n{3}有可能无法行动！",
               target.pbThis,target.abilityName,user.pbThis(true))
          end
          user.pbParalyze(target,msg)
        end
      end
    end
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:FLAMEBODY,
  proc { |ability,user,target,move,battle|
    next if !move.pbContactMove?(user)
    next if user.burned? || battle.pbRandom(100)>=30
    battle.pbShowAbilitySplash(target)
    if user.pbCanBurn?(target,PokeBattle_SceneConstants::USE_ABILITY_SPLASH) &&
       user.affectedByContactEffect?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      msg = nil
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        msg = _INTL("{1}的{2}使{3}灼伤了！",target.pbThis,target.abilityName,user.pbThis(true))
      end
      user.pbBurn(target,msg)
    end
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:GOOEY,
  proc { |ability,user,target,move,battle|
    next if !move.pbContactMove?(user)
    user.pbLowerStatStageByAbility(PBStats::SPEED,1,target,true,true)
  }
)

BattleHandlers::TargetAbilityOnHit.copy(:GOOEY,:TANGLINGHAIR)

BattleHandlers::TargetAbilityOnHit.add(:ILLUSION,
  proc { |ability,user,target,move,battle|
    # NOTE: This intentionally doesn't show the ability splash.
    next if !target.effects[PBEffects::Illusion]
    target.effects[PBEffects::Illusion] = nil
    battle.scene.pbChangePokemon(target,target.pokemon)
    battle.pbDisplay(_INTL("{1}的幻象被识破了！",target.pbThis))
    battle.pbSetSeen(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:INNARDSOUT,
  proc { |ability,user,target,move,battle|
    next if !target.fainted? || user.dummy
    battle.pbShowAbilitySplash(target)
    if user.takesIndirectDamage?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      battle.scene.pbDamageAnimation(user)
      user.pbReduceHP(target.damageState.hpLost,false)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}受到了伤害！",user.pbThis))
      else
        battle.pbDisplay(_INTL("{1}被{2}的{3}伤害了！",user.pbThis,
           target.pbThis(true),target.abilityName))
      end
    end
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:IRONBARBS,
  proc { |ability,user,target,move,battle|
    next if !move.pbContactMove?(user)
    battle.pbShowAbilitySplash(target)
    if user.takesIndirectDamage?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH) &&
       user.affectedByContactEffect?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      battle.scene.pbDamageAnimation(user)
      if user.pokemon.battleRank > 2
        user.pbReduceHP(user.totalhp/40,false)
      else
        user.pbReduceHP(user.totalhp/8,false)
      end
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}受到了伤害！",user.pbThis))
      else
        battle.pbDisplay(_INTL("{1}被{2}的{3}伤害了！",user.pbThis,
           target.pbThis(true),target.abilityName))
      end
    end
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.copy(:IRONBARBS,:ROUGHSKIN)

BattleHandlers::TargetAbilityOnHit.add(:JUSTIFIED,
  proc { |ability,user,target,move,battle|
    next if !isConst?(move.calcType,PBTypes,:DARK)
    target.pbRaiseStatStageByAbility(PBStats::ATTACK,1,target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:MUMMY,
  proc { |ability,user,target,move,battle|
    next if !move.pbContactMove?(user)
    next if user.fainted?
    next if user.unstoppableAbility?
    abilities = [getConst(PBAbilities, :MUMMY), getConst(PBAbilities, :LINGERINGAROMA)]
    next if abilities.include?(user.ability)
    next if user.hasActiveItem?(:ABILITYSHIELD)
    oldAbil = -1
    battle.pbShowAbilitySplash(target) if user.opposes?(target)
    if user.affectedByContactEffect?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      oldAbil = user.ability
      battle.pbShowAbilitySplash(user,true,false) if user.opposes?(target)
      user.ability = ability
      battle.pbReplaceAbilitySplash(user) if user.opposes?(target)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        case ability
        when getConst(PBAbilities, :MUMMY)
          msg = _INTL("{1}的特性变为{2}！", user.pbThis, user.abilityName)
        when getConst(PBAbilities, :LINGERINGAROMA)
          msg = _INTL("一股甩不掉的气味笼罩着{1}！", user.pbThis(true))
        end
        battle.pbDisplay(msg)
      else
        battle.pbDisplay(_INTL("{1}的特性因为{3}\n而变为了{2}！",
           user.pbThis, target.pbThis(true), user.abilityName))
      end
      battle.pbHideAbilitySplash(user) if user.opposes?(target)
    end
    battle.pbHideAbilitySplash(target) if user.opposes?(target)
    user.pbOnAbilityChanged(oldAbil) if oldAbil>=0
  }
)
BattleHandlers::TargetAbilityOnHit.add(:POISONPOINT,
  proc { |ability,user,target,move,battle|
    next if !move.pbContactMove?(user)
    next if user.poisoned? || battle.pbRandom(100)>=30
    battle.pbShowAbilitySplash(target)
    if user.pbCanPoison?(target,PokeBattle_SceneConstants::USE_ABILITY_SPLASH) &&
       user.affectedByContactEffect?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      msg = nil
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        msg = _INTL("{1}的{2}使{3}中毒了！",target.pbThis,target.abilityName,user.pbThis(true))
      end
      user.pbPoison(target,msg)
    end
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:RATTLED,
  proc { |ability,user,target,move,battle|
    next if !isConst?(move.calcType,PBTypes,:BUG) &&
            !isConst?(move.calcType,PBTypes,:DARK) &&
            !isConst?(move.calcType,PBTypes,:GHOST)
    target.pbRaiseStatStageByAbility(PBStats::SPEED,1,target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:STAMINA,
  proc { |ability,user,target,move,battle|
    target.pbRaiseStatStageByAbility(PBStats::DEFENSE,1,target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:SANDSPIT,
  proc { |ability,target,battler,move,battle|
    pbBattleWeatherAbility(PBWeather::Sandstorm,battler,battle)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:STATIC,
  proc { |ability,user,target,move,battle|
    next if !move.pbContactMove?(user)
    next if user.paralyzed? || battle.pbRandom(100)>=30
    battle.pbShowAbilitySplash(target)
    if user.pbCanParalyze?(target,PokeBattle_SceneConstants::USE_ABILITY_SPLASH) &&
       user.affectedByContactEffect?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      msg = nil
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        msg = _INTL("{1}的{2}使{3}麻痹了！\n{3}有可能无法行动！",
           target.pbThis,target.abilityName,user.pbThis(true))
      end
      user.pbParalyze(target,msg)
    end
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:WATERCOMPACTION,
  proc { |ability,user,target,move,battle|
    next if !isConst?(move.calcType,PBTypes,:WATER)
    target.pbRaiseStatStageByAbility(PBStats::DEFENSE,2,target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:WEAKARMOR,
  proc { |ability,user,target,move,battle|
    next if !move.physicalMove?
    next if !target.pbCanLowerStatStage?(PBStats::DEFENSE,target) &&
            !target.pbCanRaiseStatStage?(PBStats::SPEED,target)
    battle.pbShowAbilitySplash(target)
    target.pbLowerStatStageByAbility(PBStats::DEFENSE,1,target,false)
    target.pbRaiseStatStageByAbility(PBStats::SPEED,
       (NEWEST_BATTLE_MECHANICS) ? 2 : 1,target,false)
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:STEAMENGINE,
  proc { |ability,user,target,move,battle|
    next if !isConst?(move.calcType,PBTypes,:FIRE) &&
      !isConst?(move.calcType,PBTypes,:WATER)
    target.pbRaiseStatStageByAbility(PBStats::SPEED,6,target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:WANDERINGSPIRIT,
  proc { |ability,user,target,move,battle|
    next if !move.pbContactMove?(user)
    next if user.fainted?
    next if user.uncopyableAbility?
    next if user.hasActiveItem?(:ABILITYSHIELD) || target.hasActiveItem?(:ABILITYSHIELD)
    oldAbil = -1
    battle.pbShowAbilitySplash(target) if user.opposes?(target)
    if user.affectedByContactEffect?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      oldAbil = user.ability
      battle.pbShowAbilitySplash(user,true,false) if user.opposes?(target)
      user.ability = getConst(PBAbilities,:WANDERINGSPIRIT)
      target.ability = oldAbil
      if user.opposes?(target)
        battle.pbReplaceAbilitySplash(user)
        battle.pbReplaceAbilitySplash(target)
      end
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的特性变为{2}！",user.pbThis,user.abilityName))
      else
        battle.pbDisplay(_INTL("{1}的特性变为{2}，\n因为{3}！",
           user.pbThis,user.abilityName,target.pbThis(true)))
      end

      battle.pbHideAbilitySplash(user)
    end
    battle.pbHideAbilitySplash(target) if user.opposes?(target)
    if oldAbil>=0
      user.pbOnAbilityChanged(oldAbil)
      target.pbOnAbilityChanged(getConst(PBAbilities,:WANDERINGSPIRIT))
    end

  }
)


BattleHandlers::TargetAbilityOnHit.add(:PERISHBODY,
  proc { |ability,user,target,move,battle|
    next if !move.pbContactMove?(user)
    next if !user.affectedByContactEffect?
    next if user.effects[PBEffects::PerishSong]>0
    battle.pbShowAbilitySplash(target)
    battle.pbDisplay(_INTL("Both Pokémon will faint in three turns!"))
    user.effects[PBEffects::PerishSong] = 3
    target.effects[PBEffects::PerishSong] = 3 if target.effects[PBEffects::PerishSong] == 0
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:COTTONDOWN,
  proc { |ability,user,target,move,battle|
    battle.pbShowAbilitySplash(target)
    target.eachOpposing{|b|
      b.pbLowerStatStage(PBStats::SPEED,1,target)
    }
    target.eachAlly{|b|
      b.pbLowerStatStage(PBStats::SPEED,1,target)
    }
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityOnHit.add(:GULPMISSILE,
  proc { |ability,user,target,move,battle|
    next if target.form==0
    if isConst?(target.species,PBSpecies,:CRAMORANT)
      battle.pbShowAbilitySplash(target)
      gulpform=target.form
      target.form = 0
      battle.scene.pbChangePokemon(target,target.pokemon)
      battle.scene.pbDamageAnimation(user)
      if user.takesIndirectDamage?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
        user.pbReduceHP(user.totalhp/4,false)
      end
      if gulpform==1
        user.pbLowerStatStageByAbility(PBStats::DEFENSE,1,target,false)
      elsif gulpform==2
        msg = nil
        user.pbParalyze(target,msg)
      end
      battle.pbHideAbilitySplash(target)
    end
  }
)
#===============================================================================
# UserAbilityOnHit handlers
#===============================================================================

BattleHandlers::UserAbilityOnHit.add(:POISONTOUCH,
  proc { |ability,user,target,move,battle|
    next if !move.contactMove?
    next if battle.pbRandom(100)>=30
    next if target.hasActiveItem?(:COVERTCLOAK)
    battle.pbShowAbilitySplash(user)
    if target.hasActiveAbility?(:SHIELDDUST) && !battle.moldBreaker
      battle.pbShowAbilitySplash(target)
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}没有受到影响！",target.pbThis))
      end
      battle.pbHideAbilitySplash(target)
    elsif target.pbCanPoison?(user,PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      msg = nil
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        msg = _INTL("{1}的{2}使{3}中毒了！",user.pbThis,user.abilityName,target.pbThis(true))
      end
      target.pbPoison(user,msg)
    end
    battle.pbHideAbilitySplash(user)
  }
)

#===============================================================================
# UserAbilityEndOfMove handlers
#===============================================================================

BattleHandlers::UserAbilityEndOfMove.add(:BEASTBOOST,
  proc { |ability,user,targets,move,battle|
    next if battle.pbAllFainted?(user.idxOpposingSide)
    numFainted = 0
    targets.each { |b| numFainted += 1 if b.damageState.fainted }
    next if numFainted==0
    userStats = user.plainStats
    highestStatValue = 0
    userStats.each { |value|
      next if !value
      highestStatValue = value if highestStatValue < value
    }
    PBStats.eachMainBattleStat do |s|
      next if userStats[s]<highestStatValue
      if user.pbCanRaiseStatStage?(s,user)
        user.pbRaiseStatStageByAbility(s,numFainted,user)
      end
      break
    end
  }
)

BattleHandlers::UserAbilityEndOfMove.add(:MAGICIAN,
  proc { |ability,user,targets,move,battle|
    next if !battle.futureSight
    next if !move.pbDamagingMove?
    next if user.item>0
    next if battle.wildBattle? && user.opposes?
    targets.each do |b|
      next if b.damageState.unaffected || b.damageState.substitute
      next if b.item==0
      next if b.unlosableItem?(b.item) || user.unlosableItem?(b.item)
      battle.pbShowAbilitySplash(user)
      if b.hasActiveAbility?(:STICKYHOLD)
        battle.pbShowAbilitySplash(b) if user.opposes?(b)
        if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
          battle.pbDisplay(_INTL("{1}的道具不能偷窃！",b.pbThis))
        end
        battle.pbHideAbilitySplash(b) if user.opposes?(b)
        next
      end
      user.item = b.item
      b.item = 0
      b.effects[PBEffects::Unburden] = true
      if battle.wildBattle? && user.initialItem==0 && b.initialItem==user.item
        user.setInitialItem(user.item)
        b.setInitialItem(0)
      end
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}偷窃了{2}的{3}！",user.pbThis,
           b.pbThis(true),user.itemName))
      else
        battle.pbDisplay(_INTL("{1}用{4}偷窃了{2}的{3}！",user.pbThis,
           b.pbThis(true),user.itemName,user.abilityName))
      end
      battle.pbHideAbilitySplash(user)
      user.pbHeldItemTriggerCheck
      break
    end
  }
)

BattleHandlers::UserAbilityEndOfMove.add(:MOXIE,
  proc { |ability,user,targets,move,battle|
    next if battle.pbAllFainted?(user.idxOpposingSide)
    numFainted = 0
    targets.each { |b| numFainted += 1 if b.damageState.fainted }
    next if numFainted==0 || !user.pbCanRaiseStatStage?(PBStats::ATTACK,user)
    user.pbRaiseStatStageByAbility(PBStats::ATTACK,numFainted,user)
  }
)

BattleHandlers::UserAbilityEndOfMove.copy(:MOXIE,:CHILLINGNEIGH,:FEARLESS,:DRAGONSOULCRY)

BattleHandlers::UserAbilityEndOfMove.add(:GRIMNEIGH,
  proc { |ability,user,targets,move,battle|
    next if battle.pbAllFainted?(user.idxOpposingSide)
    numFainted = 0
    targets.each { |b| numFainted += 1 if b.damageState.fainted }
    next if numFainted==0 || !user.pbCanRaiseStatStage?(PBStats::SPATK,user)
    user.pbRaiseStatStageByAbility(PBStats::SPATK,numFainted,user)
  }
)


BattleHandlers::UserAbilityEndOfMove.add(:ASONEICE,
  proc { |ability,user,targets,move,battle|
    next if battle.pbAllFainted?(user.idxOpposingSide)
    numFainted = 0
    targets.each { |b| numFainted += 1 if b.damageState.fainted }
    next if numFainted==0 || !user.pbCanRaiseStatStage?(PBStats::ATTACK,user) || user.fainted?
    battle.pbShowAbilitySplash(user,false,true,PBAbilities.getName(getID(PBAbilities,:CHILLINGNEIGH)))
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      user.pbRaiseStatStage(PBStats::ATTACK,numFainted,user)
    else
      user.pbRaiseStatStageByCause(PBStats::ATTACK,numFainted,user,PBAbilities.getName(getID(PBAbilities,:CHILLINGNEIGH)))
    end
    battle.pbHideAbilitySplash(user)
  }
)

BattleHandlers::UserAbilityEndOfMove.add(:ASONEGHOST,
  proc { |ability,user,targets,move,battle|
    next if battle.pbAllFainted?(user.idxOpposingSide)
    numFainted = 0
    targets.each { |b| numFainted += 1 if b.damageState.fainted }
    next if numFainted==0 || !user.pbCanRaiseStatStage?(PBStats::ATTACK,user) || user.fainted?
    battle.pbShowAbilitySplash(user,false,true,PBAbilities.getName(getID(PBAbilities,:GRIMNEIGH)))
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      user.pbRaiseStatStage(PBStats::SPATK,numFainted,user)
    else
      user.pbRaiseStatStageByCause(PBStats::SPATK,numFainted,user,PBAbilities.getName(getID(PBAbilities,:GRIMNEIGH)))
    end
    battle.pbHideAbilitySplash(user)
  }
)

#===============================================================================
# TargetAbilityAfterMoveUse handlers
#===============================================================================

BattleHandlers::TargetAbilityAfterMoveUse.add(:BERSERK,
  proc { |ability,target,user,move,switched,battle|
    next if !move.damagingMove?
    next if target.damageState.initialHP<target.totalhp/2 || target.hp>=target.totalhp/2
    next if !target.pbCanRaiseStatStage?(PBStats::SPATK,target)
    target.pbRaiseStatStageByAbility(PBStats::SPATK,1,target)
  }
)

BattleHandlers::TargetAbilityAfterMoveUse.add(:COLORCHANGE,
  proc { |ability,target,user,move,switched,battle|
    next if target.damageState.calcDamage==0 || target.damageState.substitute
    next if move.calcType<0 || PBTypes.isPseudoType?(move.calcType)
    next if isConst?(move.calcType,PBTypes,:STELLAR)
    next if target.pbHasType?(move.calcType) && !target.pbHasOtherType?(move.calcType)
    typeName = PBTypes.getName(move.calcType)
    battle.pbShowAbilitySplash(target)
    target.pbChangeTypes(move.calcType)
    battle.pbDisplay(_INTL("{1}的{2}使它\n变成了{3}属性！",target.pbThis,
       target.abilityName,typeName))
    battle.pbHideAbilitySplash(target)
  }
)

BattleHandlers::TargetAbilityAfterMoveUse.add(:PICKPOCKET,
  proc { |ability,target,user,move,switched,battle|
    # NOTE: According to Bulbapedia, this can still trigger to steal the user's
    #       item even if it was switched out by a Red Card. This doesn't make
    #       sense, so this code doesn't do it.
    next if battle.wildBattle? && target.opposes?
    next if !move.contactMove?
    next if switched.include?(user.index)
    next if user.effects[PBEffects::Substitute]>0 || target.damageState.substitute
    next if target.item>0 || user.item==0
    next if user.unlosableItem?(user.item) || target.unlosableItem?(user.item)
    battle.pbShowAbilitySplash(target)
    if user.hasActiveAbility?(:STICKYHOLD)
      battle.pbShowAbilitySplash(user) if target.opposes?(user)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的道具不能偷窃！",user.pbThis))
      end
      battle.pbHideAbilitySplash(user) if target.opposes?(user)
      battle.pbHideAbilitySplash(target)
      next
    end
    target.item = user.item
    user.item = 0
    user.effects[PBEffects::Unburden] = true
    if battle.wildBattle? && target.initialItem==0 && user.initialItem==target.item
      target.setInitialItem(target.item)
      user.setInitialItem(0)
    end
    battle.pbDisplay(_INTL("{1}顺手偷走了{2}的{3}！",target.pbThis,
       user.pbThis(true),target.itemName))
    battle.pbHideAbilitySplash(target)
    target.pbHeldItemTriggerCheck
  }
)

#===============================================================================
# EORWeatherAbility handlers
#===============================================================================

BattleHandlers::EORWeatherAbility.add(:DRYSKIN,
  proc { |ability,weather,battler,battle|
  case weather
  when PBWeather::Sun, PBWeather::HarshSun
    battle.pbShowAbilitySplash(battler)
    battle.scene.pbDamageAnimation(battler)
    battler.pbReduceHP(battler.totalhp/8,false)
    battle.pbDisplay(_INTL("{1}被强烈的阳光晒伤了！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
    battler.pbItemHPHealCheck
  when PBWeather::Rain, PBWeather::HeavyRain
    next if !battler.canHeal?
    battle.pbShowAbilitySplash(battler)
    battler.pbRecoverHP(battler.totalhp/8)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("{1}的HP回复了。",battler.pbThis))
    else
      battle.pbDisplay(_INTL("{1}的{2}回复了HP。",battler.pbThis,battler.abilityName))
    end
    battle.pbHideAbilitySplash(battler)
  end
  }
)

BattleHandlers::EORWeatherAbility.add(:ICEBODY,
  proc { |ability,weather,battler,battle|
    next unless weather==PBWeather::Hail || weather==PBWeather::Snow
    next if !battler.canHeal?
    battle.pbShowAbilitySplash(battler)
    battler.pbRecoverHP(battler.totalhp/16)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("{1}的HP回复了。",battler.pbThis))
    else
      battle.pbDisplay(_INTL("{1}的{2}回复了它的HP。",battler.pbThis,battler.abilityName))
    end
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::EORWeatherAbility.add(:RAINDISH,
  proc { |ability,weather,battler,battle|
  next unless weather==PBWeather::Rain || weather==PBWeather::HeavyRain
  next if !battler.canHeal?
  battle.pbShowAbilitySplash(battler)
  battler.pbRecoverHP(battler.totalhp/16)
  if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
    battle.pbDisplay(_INTL("{1}的HP回复了。",battler.pbThis))
  else
    battle.pbDisplay(_INTL("{1}的{2}回复了HP。",battler.pbThis,battler.abilityName))
  end
  battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::EORWeatherAbility.add(:SOLARPOWER,
  proc { |ability,weather,battler,battle|
  next unless weather==PBWeather::Sun || weather==PBWeather::HarshSun
  battle.pbShowAbilitySplash(battler)
  battle.scene.pbDamageAnimation(battler)
  battler.pbReduceHP(battler.totalhp/8,false)
  battle.pbDisplay(_INTL("{1}被强烈的阳光晒伤了！",battler.pbThis))
  battle.pbHideAbilitySplash(battler)
  battler.pbItemHPHealCheck
  }
)

#===============================================================================
# EORHealingAbility handlers
#===============================================================================

BattleHandlers::EORHealingAbility.add(:HEALER,
  proc { |ability,battler,battle|
    next unless battle.pbRandom(100)<30
    battler.eachAlly do |b|
      next if b.status==PBStatuses::NONE
      battle.pbShowAbilitySplash(battler)
      oldStatus = b.status
      b.pbCureStatus(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        case oldStatus
        when PBStatuses::SLEEP
          battle.pbDisplay(_INTL("{1}的{2}把伙伴吵醒了！",battler.pbThis,battler.abilityName))
        when PBStatuses::POISON
          battle.pbDisplay(_INTL("{1}的{2}治愈了伙伴的毒！",battler.pbThis,battler.abilityName))
        when PBStatuses::BURN
          battle.pbDisplay(_INTL("{1}的{2}治愈了伙伴的灼伤！",battler.pbThis,battler.abilityName))
        when PBStatuses::PARALYSIS
          battle.pbDisplay(_INTL("{1}的{2}治愈了伙伴的麻痹！",battler.pbThis,battler.abilityName))
        when PBStatuses::FROZEN
          battle.pbDisplay(_INTL("{1}的{2}解冻了伙伴！",battler.pbThis,battler.abilityName))
        end
      end
      battle.pbHideAbilitySplash(battler)
    end
  }
)

BattleHandlers::EORHealingAbility.add(:HYDRATION,
  proc { |ability,battler,battle|
  if !battler.hasUtilityUmbrella?
    next if battler.status==PBStatuses::NONE
    curWeather = battle.pbWeather
    next if curWeather!=PBWeather::Rain && curWeather!=PBWeather::HeavyRain
    battle.pbShowAbilitySplash(battler)
    oldStatus = battler.status
    battler.pbCureStatus(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
    if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      case oldStatus
      when PBStatuses::SLEEP
        battle.pbDisplay(_INTL("{1}的{2}使它醒来了！",battler.pbThis,battler.abilityName))
      when PBStatuses::POISON
        battle.pbDisplay(_INTL("{1}的{2}治愈了毒！",battler.pbThis,battler.abilityName))
      when PBStatuses::BURN
        battle.pbDisplay(_INTL("{1}的{2}治愈了灼伤！",battler.pbThis,battler.abilityName))
      when PBStatuses::PARALYSIS
        battle.pbDisplay(_INTL("{1}的{2}治愈了麻痹！",battler.pbThis,battler.abilityName))
      when PBStatuses::FROZEN
        battle.pbDisplay(_INTL("{2}解冻了{1}！",battler.pbThis,battler.abilityName))
      end
    end
    battle.pbHideAbilitySplash(battler)
  end
  }
)

BattleHandlers::EORHealingAbility.add(:SHEDSKIN,
  proc { |ability,battler,battle|
    next if battler.status==PBStatuses::NONE
    next unless battle.pbRandom(100)<30
    battle.pbShowAbilitySplash(battler)
    oldStatus = battler.status
    battler.pbCureStatus(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
    if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      case oldStatus
      when PBStatuses::SLEEP
        battle.pbDisplay(_INTL("{1}的{2}使它醒来了！",battler.pbThis,battler.abilityName))
      when PBStatuses::POISON
        battle.pbDisplay(_INTL("{1}的{2}治愈了毒！",battler.pbThis,battler.abilityName))
      when PBStatuses::BURN
        battle.pbDisplay(_INTL("{1}的{2}治愈了灼伤！",battler.pbThis,battler.abilityName))
      when PBStatuses::PARALYSIS
        battle.pbDisplay(_INTL("{1}的{2}治愈了麻痹！",battler.pbThis,battler.abilityName))
      when PBStatuses::FROZEN
        battle.pbDisplay(_INTL("{2}解冻了{1}！",battler.pbThis,battler.abilityName))
      end
    end
    battle.pbHideAbilitySplash(battler)
  }
)

#===============================================================================
# EOREffectAbility handlers
#===============================================================================

BattleHandlers::EOREffectAbility.add(:BADDREAMS,
  proc { |ability,battler,battle|
    battle.eachOtherSideBattler(battler.index) do |b|
      next if !b.near?(battler) || !b.asleep?
      battle.pbShowAbilitySplash(battler)
      next if !b.takesIndirectDamage?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      oldHP = b.hp
      if b.pokemon.battleRank > 2
        b.pbReduceHP(b.totalhp/40)
      else
        b.pbReduceHP(b.totalhp/8)
      end
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}备受折磨！",b.pbThis))
      else
        battle.pbDisplay(_INTL("{1}被{2}的{3}折磨着！",b.pbThis,
           battler.pbThis(true),battler.abilityName))
      end
      battle.pbHideAbilitySplash(battler)
      b.pbItemHPHealCheck
      b.pbAbilitiesOnDamageTaken(oldHP)
      b.pbFaint if b.fainted?
    end
  }
)

BattleHandlers::EOREffectAbility.add(:MOODY,
  proc { |ability,battler,battle|
    randomUp = []; randomDown = []
    PBStats.eachMainBattleStat do |s|
      randomUp.push(s) if battler.pbCanRaiseStatStage?(s,battler)
      randomDown.push(s) if battler.pbCanLowerStatStage?(s,battler)
    end
    next if randomUp.length==0 && randomDown.length==0
    battle.pbShowAbilitySplash(battler)
    if randomUp.length>0
      r = battle.pbRandom(randomUp.length)
      battler.pbRaiseStatStageByAbility(randomUp[r],2,battler,false)
      randomDown.delete(randomUp[r])
    end
    if randomDown.length>0
      r = battle.pbRandom(randomDown.length)
      battler.pbLowerStatStageByAbility(randomDown[r],1,battler,false)
    end
    battle.pbHideAbilitySplash(battler)
    battler.pbItemStatRestoreCheck if randomDown.length>0
  }
)

BattleHandlers::EOREffectAbility.add(:SPEEDBOOST,
  proc { |ability,battler,battle|
    # A Pokémon's turnCount is 0 if it became active after the beginning of a
    # round
    if battler.turnCount>0 && battler.pbCanRaiseStatStage?(PBStats::SPEED,battler)
      battler.pbRaiseStatStageByAbility(PBStats::SPEED,1,battler)
    end
  }
)

BattleHandlers::EOREffectAbility.add(:BALLFETCH,
  proc { |ability,battler,battle|
    if battler.effects[PBEffects::BallFetch]!=0 && battler.item<=0
      ball=battler.effects[PBEffects::BallFetch]
      battler.item=ball
      battler.setInitialItem(battler.item)
      PBDebug.log("[Ability triggered] #{battler.pbThis}'s Ball Fetch found #{PBItems.getName(ball)}")
      battle.pbShowAbilitySplash(battler) if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("{1}回收了{2}！",battler.pbThis,PBItems.getName(ball)))
      battler.effects[PBEffects::BallFetch]=0
      battle.pbHideAbilitySplash(battler) if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
    end
  }
)

BattleHandlers::EOREffectAbility.add(:HUNGERSWITCH,
  proc { |ability,battler,battle|
    if isConst?(battler.species,PBSpecies,:MORPEKO)
      battle.pbShowAbilitySplash(battler)
      battler.form=(battler.form==0) ? 1 : 0
      battler.pbUpdate(true)
      battle.scene.pbChangePokemon(battler,battler.pokemon)
      battle.pbDisplay(_INTL("{1}变身了！",battler.pbThis))
      battle.pbHideAbilitySplash(battler)
    end
  }
)

#===============================================================================
# EORGainItemAbility handlers
#===============================================================================

BattleHandlers::EORGainItemAbility.add(:HARVEST,
  proc { |ability,battler,battle|
    next if battler.item>0
    next if battler.recycleItem<=0 || !pbIsBerry?(battler.recycleItem) && !battler.hasUtilityUmbrella?
    curWeather = battle.pbWeather
    if curWeather!=PBWeather::Sun && curWeather!=PBWeather::HarshSun
      next unless battle.pbRandom(100)<50
    end
    battle.pbShowAbilitySplash(battler)
    battler.item = battler.recycleItem
    battler.setRecycleItem(0)
    battler.setInitialItem(battler.item) if battler.initialItem==0
    battle.pbDisplay(_INTL("{1}收获了{2}！",battler.pbThis,battler.itemName))
    battle.pbHideAbilitySplash(battler)
    battler.pbHeldItemTriggerCheck
  }
)

BattleHandlers::EORGainItemAbility.add(:PICKUP,
  proc { |ability,battler,battle|
    next if battler.item>0
    foundItem = 0; fromBattler = nil; use = 0
    battle.eachBattler do |b|
      next if b.index==battler.index
      next if b.effects[PBEffects::PickupUse]<=use
      foundItem   = b.effects[PBEffects::PickupItem]
      fromBattler = b
      use         = b.effects[PBEffects::PickupUse]
    end
    next if foundItem<=0
    battle.pbShowAbilitySplash(battler)
    battler.item = foundItem
    fromBattler.effects[PBEffects::PickupItem] = 0
    fromBattler.effects[PBEffects::PickupUse]  = 0
    fromBattler.setRecycleItem(0) if fromBattler.recycleItem==foundItem
    if battle.wildBattle? && battler.initialItem==0 && fromBattler.initialItem==foundItem
      battler.setInitialItem(foundItem)
      fromBattler.setInitialItem(0)
    end
    battle.pbDisplay(_INTL("{1}回收了{2}！",battler.pbThis,battler.itemName))
    battle.pbHideAbilitySplash(battler)
    battler.pbHeldItemTriggerCheck
  }
)

#===============================================================================
# CertainSwitchingUserAbility handlers
#===============================================================================

# There aren't any!

#===============================================================================
# TrappingTargetAbility handlers
#===============================================================================

BattleHandlers::TrappingTargetAbility.add(:ARENATRAP,
  proc { |ability,switcher,bearer,battle|
    next true if !switcher.airborne?
  }
)

BattleHandlers::TrappingTargetAbility.add(:MAGNETPULL,
  proc { |ability,switcher,bearer,battle|
    next true if switcher.pbHasType?(:STEEL)
  }
)

BattleHandlers::TrappingTargetAbility.add(:SHADOWTAG,
  proc { |ability,switcher,bearer,battle|
    next true if !switcher.hasActiveAbility?(:SHADOWTAG)
  }
)

#===============================================================================
# AbilityOnSwitchIn handlers
#===============================================================================

BattleHandlers::AbilityOnSwitchIn.add(:AIRLOCK,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("{1}已经拥有了{2}!",battler.pbThis,battler.abilityName))
    end
    battle.pbDisplay(_INTL("天气的影响消失了。"))
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.copy(:AIRLOCK,:CLOUDNINE)

BattleHandlers::AbilityOnSwitchIn.add(:ANTICIPATION,
  proc { |ability,battler,battle|
    next if !battler.pbOwnedByPlayer?
    battlerTypes = battler.pbTypes(true)
    type1 = (battlerTypes.length>0) ? battlerTypes[0] : nil
    type2 = (battlerTypes.length>1) ? battlerTypes[1] : type1
    type3 = (battlerTypes.length>2) ? battlerTypes[2] : type2
    found = false
    battle.eachOtherSideBattler(battler.index) do |b|
      b.eachMove do |m|
        next if m.statusMove?
        moveData = pbGetMoveData(m.id)
        if type1
          moveType = moveData[MOVE_TYPE]
          if NEWEST_BATTLE_MECHANICS && isConst?(m.id,PBMoves,:HIDDENPOWER)
            moveType = pbHiddenPower(b.pokemon)[0]
          end
          eff = PBTypes.getCombinedEffectiveness(moveType,type1,type2,type3)
          next if PBTypes.ineffective?(eff)
          next if !PBTypes.superEffective?(eff) && moveData[MOVE_FUNCTION_CODE]!="070"   # OHKO
        else
          next if moveData[MOVE_FUNCTION_CODE]!="070"   # OHKO
        end
        found = true
        break
      end
      break if found
    end
    if found
      battle.pbShowAbilitySplash(battler)
      battle.pbDisplay(_INTL("{1}因为预知到了危险而发抖！",battler.pbThis))
      battle.pbHideAbilitySplash(battler)
    end
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:AURABREAK,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}逆转了其它宝可梦的气场!",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:COMATOSE,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}正在打瞌睡！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:DARKAURA,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}正在释放暗黑气场！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:DELTASTREAM,
  proc { |ability,battler,battle|
    pbBattleWeatherAbility(PBWeather::StrongWinds,battler,battle,true)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:DESOLATELAND,
  proc { |ability,battler,battle|
    pbBattleWeatherAbility(PBWeather::HarshSun,battler,battle,true)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:DOWNLOAD,
  proc { |ability,battler,battle|
    oDef = oSpDef = 0
    battle.eachOtherSideBattler(battler.index) do |b|
      oDef   += b.defense
      oSpDef += b.spdef
    end
    stat = (oDef<oSpDef) ? PBStats::ATTACK : PBStats::SPATK
    battler.pbRaiseStatStageByAbility(stat,1,battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:DRIZZLE,
  proc { |ability,battler,battle|
    pbBattleWeatherAbility(PBWeather::Rain,battler,battle)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:DROUGHT,
  proc { |ability,battler,battle|
    pbBattleWeatherAbility(PBWeather::Sun,battler,battle)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:ELECTRICSURGE,
  proc { |ability,battler,battle|
    next if battle.field.terrain==PBBattleTerrains::Electric
    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(battler,PBBattleTerrains::Electric)
    # NOTE: The ability splash is hidden again in def pbStartTerrain.
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:FAIRYAURA,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}正在释放妖精气场！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:FOREWARN,
  proc { |ability,battler,battle|
    next if !battler.pbOwnedByPlayer?
    highestPower = 0
    forewarnMoves = []
    battle.eachOtherSideBattler(battler.index) do |b|
      b.eachMove do |m|
        moveData = pbGetMoveData(m.id)
        power = moveData[MOVE_BASE_DAMAGE]
        power = 160 if ["070"].include?(moveData[MOVE_FUNCTION_CODE])    # OHKO
        power = 150 if ["08B"].include?(moveData[MOVE_FUNCTION_CODE])    # Eruption
        # Counter, Mirror Coat, Metal Burst
        power = 120 if ["071","072","073"].include?(moveData[MOVE_FUNCTION_CODE])
        # Sonic Boom, Dragon Rage, Night Shade, Endeavor, Psywave,
        # Return, Frustration, Crush Grip, Gyro Ball, Hidden Power,
        # Natural Gift, Trump Card, Flail, Grass Knot
        power = 80 if ["06A","06B","06D","06E","06F",
                       "089","08A","08C","08D","090",
                       "096","097","098","09A"].include?(moveData[MOVE_FUNCTION_CODE])
        next if power<highestPower
        forewarnMoves = [] if power>highestPower
        forewarnMoves.push(m.id)
        highestPower = power
      end
    end
    if forewarnMoves.length>0
      battle.pbShowAbilitySplash(battler)
      forewarnMoveID = forewarnMoves[battle.pbRandom(forewarnMoves.length)]
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}预知到了{2}！",
          battler.pbThis,PBMoves.getName(forewarnMoveID)))
      else
        battle.pbDisplay(_INTL("{1}的预知梦预知到了{2}！",
          battler.pbThis,PBMoves.getName(forewarnMoveID)))
      end
      battle.pbHideAbilitySplash(battler)
    end
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:FRISK,
  proc { |ability,battler,battle|
    next if !battler.pbOwnedByPlayer?
    foes = []
    battle.eachOtherSideBattler(battler.index) do |b|
      foes.push(b) if b.item>0
    end
    if foes.length>0
      battle.pbShowAbilitySplash(battler)
      if NEWEST_BATTLE_MECHANICS
        foes.each do |b|
          battle.pbDisplay(_INTL("{1}察觉到了{2}的{3}！",
             battler.pbThis,b.pbThis(true),PBItems.getName(b.item)))
        end
      else
        foe = foes[battle.pbRandom(foes.length)]
        battle.pbDisplay(_INTL("{1}察觉到了敌人的{2}！",
           battler.pbThis,PBItems.getName(foe.item)))
      end
      battle.pbHideAbilitySplash(battler)
    end
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:GRASSYSURGE,
  proc { |ability,battler,battle|
    next if battle.field.terrain==PBBattleTerrains::Grassy
    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(battler,PBBattleTerrains::Grassy)
    # NOTE: The ability splash is hidden again in def pbStartTerrain.
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:IMPOSTER,
  proc { |ability,battler,battle|
    next if battler.effects[PBEffects::Transform]
    choice = battler.pbDirectOpposing
    next if choice.fainted?
    next if choice.effects[PBEffects::Transform] ||
            choice.effects[PBEffects::Illusion] ||
            choice.effects[PBEffects::Substitute]>0 ||
            choice.effects[PBEffects::SkyDrop]>=0 ||
            choice.semiInvulnerable?
    battle.pbShowAbilitySplash(battler,true)
    battle.pbHideAbilitySplash(battler)
    #马赛克动画
    battle.scene.pbChangePokemonTransform(battler,choice.pokemon)
    battler.pbTransform(choice)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:INTIMIDATE,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.eachOtherSideBattler(battler.index) do |b|
      next if !b.near?(battler)
      check_item = true
      if b.hasActiveAbility?([:CONTRARY, :GUARDDOG])
        check_item = false if b.statStageAtMax?(PBStats::ATTACK)
      elsif b.statStageAtMin?(PBStats::ATTACK)
        check_item = false
      end
      b.pbLowerAttackStatStageIntimidate(battler)
      b.pbItemOnIntimidatedCheck if check_item
    end
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:MISTYSURGE,
  proc { |ability,battler,battle|
    next if battle.field.terrain==PBBattleTerrains::Misty
    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(battler,PBBattleTerrains::Misty)
    # NOTE: The ability splash is hidden again in def pbStartTerrain.
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:MOLDBREAKER,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}打破了常规！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)
BattleHandlers::DamageCalcTargetAbility.copy(:MOLDBREAKER,:SHATTERFIST)

BattleHandlers::AbilityOnSwitchIn.add(:PRESSURE,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}正在施加压力！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)
BattleHandlers::DamageCalcTargetAbility.copy(:PRESSURE,:CALAMITYAERIAL)

BattleHandlers::AbilityOnSwitchIn.add(:PRIMORDIALSEA,
  proc { |ability,battler,battle|
    pbBattleWeatherAbility(PBWeather::HeavyRain,battler,battle,true)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:PSYCHICSURGE,
  proc { |ability,battler,battle|
    next if battle.field.terrain==PBBattleTerrains::Psychic
    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(battler,PBBattleTerrains::Psychic)
    # NOTE: The ability splash is hidden again in def pbStartTerrain.
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:SANDSTREAM,
  proc { |ability,battler,battle|
    pbBattleWeatherAbility(PBWeather::Sandstorm,battler,battle)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:SLOWSTART,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battler.effects[PBEffects::SlowStart] = 5
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("{1}无法顺利行动！",battler.pbThis))
    else
      battle.pbDisplay(_INTL("{2}使得{1}无法顺利行动！",
         battler.pbThis,battler.abilityName))
    end
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:SNOWWARNING,
  proc { |ability,battler,battle|
    pbBattleWeatherAbility(PBWeather::Snow,battler,battle)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:TERAVOLT,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}正在释放溅射气场！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)


BattleHandlers::AbilityOnSwitchIn.add(:TURBOBLAZE,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}正在释放炽热气场！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:UNNERVE,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}太紧张导致无法吃下树果！",battler.pbOpposingTeam))
    battle.pbHideAbilitySplash(battler)
  }
)
BattleHandlers::StatusImmunityAbility.copy(:UNNERVE,:CONFESSIONLIST)

BattleHandlers::AbilityOnSwitchIn.add(:ASONEICE,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}拥有两种特性！",battler.pbThis))
    battle.pbShowAbilitySplash(battler,false,true,PBAbilities.getName(getID(PBAbilities,:UNNERVE)))
    battle.pbDisplay(_INTL("{1}正在施加紧张感！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.copy(:ASONEICE,:ASONEGHOST)

BattleHandlers::AbilityOnSwitchIn.add(:INTREPIDSWORD,
  proc { |ability,battler,battle|
    stat = PBStats::ATTACK
    battler.pbRaiseStatStageByAbility(stat,1,battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:DAUNTLESSSHIELD,
  proc { |ability,battler,battle|
    stat = PBStats::DEFENSE
    battler.pbRaiseStatStageByAbility(stat,1,battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:SCREENCLEANER,
  proc { |ability,battler,battle|
    target=battler
    battle.pbShowAbilitySplash(battler)
    if target.pbOwnSide.effects[PBEffects::AuroraVeil]>0
      target.pbOwnSide.effects[PBEffects::AuroraVeil] = 0
      battle.pbDisplay(_INTL("{1}的极光幕消失了！",target.pbTeam))
    end
    if target.pbOwnSide.effects[PBEffects::LightScreen]>0
      target.pbOwnSide.effects[PBEffects::LightScreen] = 0
      battle.pbDisplay(_INTL("{1}的光墙消失了！",target.pbTeam))
    end
    if target.pbOwnSide.effects[PBEffects::Reflect]>0
      target.pbOwnSide.effects[PBEffects::Reflect] = 0
      battle.pbDisplay(_INTL("{1}的反射盾消失了！",target.pbTeam))
    end
    if target.pbOpposingSide.effects[PBEffects::AuroraVeil]>0
      target.pbOpposingSide.effects[PBEffects::AuroraVeil] = 0
      battle.pbDisplay(_INTL("{1}的极光幕消失了！",target.pbOpposingTeam))
    end
    if target.pbOpposingSide.effects[PBEffects::LightScreen]>0
      target.pbOpposingSide.effects[PBEffects::LightScreen] = 0
      battle.pbDisplay(_INTL("{1}的光墙消失了！",target.pbOpposingTeam))
    end
    if target.pbOwnSide.effects[PBEffects::Reflect]>0
      target.pbOpposingSide.effects[PBEffects::Reflect] = 0
      battle.pbDisplay(_INTL("{1}的反射盾消失了！",target.pbOpposingTeam))
    end
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:PASTELVEIL,
  proc { |ability,battler,battle|
    battler.eachAlly do |b|
      next if b.status != PBStatuses::POISON
      battle.pbShowAbilitySplash(battler)
      b.pbCureStatus(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的{2}治愈了{3}的中毒状态！",battler.pbThis,battler.abilityName,b.pbThis(true)))
      end
      battle.pbHideAbilitySplash(battler)
    end
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:CURIOUSMEDICINE,
  proc { |ability,battler,battle|
    done= false
    battler.eachAlly do |b|
      next if !b.hasAlteredStatStages?
      b.pbResetStatStages
      done = true
    end
    if done
      battle.pbShowAbilitySplash(battler)
      battle.pbDisplay(_INTL("所有队友的能力变化都被消除了！"))
      battle.pbHideAbilitySplash(battler)
    end
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:NEUTRALIZINGGAS,
  proc { |ability,battler,battle|
    next if battle.field.effects[PBEffects::NeutralizingGas]
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}的气体无效化了所有特性！",battler.pbThis))
    battle.field.effects[PBEffects::NeutralizingGas] = true
    battle.pbHideAbilitySplash(battler)
    battle.allBattlers.each do |b|
      if b.hasActiveItem?(:ABILITYSHIELD)
        itemname = PBItems.getName(b.item)
        battle.pbDisplay(_INTL("{1}的特性\n被{2}的效果保护了！",b.pbThis,itemname))
        next
      end
    end
  }
)
# 次元之躯
BattleHandlers::AbilityOnSwitchIn.add(:DIMENSIONBODY,
  proc { |ability,battler,battle|
    # 遍历对手
    battle.eachOtherSideBattler(battler.index) do |b|
      if b.hp != battler.hp       # 双方HP不相等
        if b.hp < battler.hp      # 对手HP少于自己
          battler.pbRaiseStatStageByAbility(PBStats::SPATK,2,battler)
        elsif b.hp > battler.hp   # 对手HP大于自己
          battler.pbRaiseStatStageByAbility(PBStats::EVASION,2,battler)
        end
      end
    end
  }
)

#===============================================================================
# AbilityOnSwitchOut handlers
#===============================================================================

BattleHandlers::AbilityOnSwitchOut.add(:NATURALCURE,
  proc { |ability,battler,endOfBattle|
    PBDebug.log("[Ability triggered] #{battler.pbThis}'s #{battler.abilityName}")
    battler.status = PBStatuses::NONE
  }
)

BattleHandlers::AbilityOnSwitchOut.add(:REGENERATOR,
  proc { |ability,battler,endOfBattle|
    next if endOfBattle
    PBDebug.log("[Ability triggered] #{battler.pbThis}'s #{battler.abilityName}")
    battler.pbRecoverHP(battler.totalhp/3,false,false)
  }
)

#===============================================================================
# AbilityChangeOnBattlerFainting handlers
#===============================================================================

BattleHandlers::AbilityChangeOnBattlerFainting.add(:POWEROFALCHEMY,
  proc { |ability,battler,fainted,battle|
    next if battler.opposes?(fainted)
    next if battler.hasActiveItem?(:ABILITYSHIELD)
    next if fainted.ungainableAbility? ||
       isConst?(fainted.ability, PBAbilities, :POWEROFALCHEMY) ||
       isConst?(fainted.ability, PBAbilities, :RECEIVER) ||
       isConst?(fainted.ability, PBAbilities, :TRACE) ||
       isConst?(fainted.ability, PBAbilities, :WONDERGUARD)
    battle.pbShowAbilitySplash(battler,true)
    battler.ability = fainted.ability
    battle.pbReplaceAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}的{2}被继承了！",fainted.pbThis,fainted.abilityName))
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::AbilityChangeOnBattlerFainting.copy(:POWEROFALCHEMY,:RECEIVER)

#===============================================================================
# AbilityOnBattlerFainting handlers
#===============================================================================

BattleHandlers::AbilityOnBattlerFainting.add(:SOULHEART,
  proc { |ability,battler,fainted,battle|
    battler.pbRaiseStatStageByAbility(PBStats::SPATK,1,battler)
  }
)

#===============================================================================
# RunFromBattleAbility handlers
#===============================================================================

BattleHandlers::RunFromBattleAbility.add(:RUNAWAY,
  proc { |ability,battler|
    next true
  }
)

#锋锐
BattleHandlers::DamageCalcUserAbility.add(:SHARPNESS,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 1.5 if move.slicingMove?
  }
)

BattleHandlers::StatusImmunityAbility.copy(:SHARPNESS,:PSYCHICEDGE,:ENDLESSDARKN,:NETHERDRIVE)

#毒满地
BattleHandlers::TargetAbilityOnHit.add(:TOXICDEBRIS,
  proc { |ability,user,target,move,battle|
    next if !move.physicalMove?
    next if target.damageState.substitute
    next if target.pbOpposingSide.effects[PBEffects::ToxicSpikes] >= 2
    battle.pbShowAbilitySplash(target)
    target.pbOpposingSide.effects[PBEffects::ToxicSpikes] += 1
    battle.pbAnimation(getID(PBMoves,:TOXICSPIKES), target, target.pbDirectOpposing)
    battle.pbDisplay(_INTL("{1}脚下散落着毒菱！", target.pbOpposingTeam(true)))
    battle.pbHideAbilitySplash(target)
  }
)


# 搬岩
BattleHandlers::DamageCalcUserAbility.add(:ROCKYPAYLOAD,
  proc { |ability, user, target, move, mults, baseDmg, type|
    mults[ATK_MULT] *= 1.5 if isConst?(type, PBTypes, :ROCK)
  }
)

# 掉出种子
BattleHandlers::TargetAbilityOnHit.add(:SEEDSOWER,
  proc { |ability, user, target, move, battle|
    next if !move.damagingMove?
    next if battle.field.terrain == PBBattleTerrains::Grassy
    battle.pbShowAbilitySplash(target)
    battle.pbStartTerrain(target, PBBattleTerrains::Grassy)
  }
)
# 焦香之躯
BattleHandlers::MoveImmunityTargetAbility.add(:WELLBAKEDBODY,
  proc { |ability, user, target, move, type, battle|
    next pbBattleMoveImmunityStatAbility(user, target, move, type, :FIRE, PBStats::DEFENSE, 2, battle)
  }
)

# 热交换
BattleHandlers::TargetAbilityOnHit.add(:THERMALEXCHANGE,
  proc { |ability,user,target,move,battle|
    next if !isConst?(move.calcType,PBTypes,:FIRE)
    target.pbRaiseStatStageByAbility(PBStats::ATTACK,1,target)
  }
)
BattleHandlers::StatusImmunityAbility.copy(:WATERVEIL,:WATERBUBBLE,:THERMALEXCHANGE)

# 发号施令
BattleHandlers::AbilityOnSwitchIn.add(:COMMANDER,
  proc { |ability, battler, battle|
    next if battler.effects[PBEffects::Commander]
    next if defined?(battler.dynamax?) && battler.dynamax?
    showAnim = true
    battler.allAllies.each{|b|
      next if !b || !b.near?(battler) || b.fainted?
      next if battle.choices[b.index][0] == :SwitchOut
      next if !b.isSpecies?(:DONDOZO)
      next if b.effects[PBEffects::Commander]
      next if defined?(b.dynamax?) && b.dynamax?
      battle.pbShowAbilitySplash(battler)
      battle.pbClearChoice(battler.index)
      battle.pbDisplay(_INTL("{1}进入了{2}的嘴里！", battler.pbThis, b.pbThis(true)))
      battle.scene.sprites["pokemon_#{battler.index}"].visible = false
      b.effects[PBEffects::Commander] = [battler.index, battler.form]
      battler.effects[PBEffects::Commander] = [b.index]
      [PBStats::ATTACK, PBStats::DEFENSE, PBStats::SPATK, PBStats::SPDEF, PBStats::SPEED].each do |stat|
        next if !b.pbCanRaiseStatStage?(stat, b)
        if b.pbRaiseStatStage(stat, 2, b, showAnim)
          showAnim = false
        end
      end
      battle.pbHideAbilitySplash(battler)
      break
    }
  }
)
BattleHandlers::MoveImmunityTargetAbility.add(:COMMANDER,
  proc { |ability,user,target,move,type,battle|
    next false if !target.isCommander?
    battle.pbDisplay(_INTL("{1}避免了攻击！", target.pbThis))
    next true
  }
)

# 同台共演
BattleHandlers::AbilityOnSwitchIn.add(:COSTAR,
  proc { |ability, battler, battle|
    battler.allAllies.each do |b|
      next if b.index == battler.index
      next if !b.hasAlteredStatStages? && b.effects[PBEffects::FocusEnergy] == 0
      battle.pbShowAbilitySplash(battler)
      battler.effects[PBEffects::FocusEnergy] = b.effects[PBEffects::FocusEnergy]
      PBStats.eachMainBattleStat { |stat| battler.stages[stat] = b.stages[stat] }
      battle.pbDisplay(_INTL("{1}复制了\n{2}的能力变化！", battler.pbThis, b.pbThis(true)))
      battle.pbHideAbilitySplash(battler)
      break
    end
  }
)


# 食土
BattleHandlers::MoveImmunityTargetAbility.add(:EARTHEATER,
  proc { |ability, user, target, move, type, battle|
    next pbBattleMoveImmunityHealAbility(user, target, move, type, :GROUND, battle)
  }
)
# 电力转换
BattleHandlers::TargetAbilityOnHit.add(:ELECTROMORPHOSIS,
  proc { |ability, user, target, move, battle|
    next if target.fainted?
    next if target.effects[PBEffects::Charge] > 0
    battle.pbShowAbilitySplash(target)
    target.effects[PBEffects::Charge] = 2
    battle.pbDisplay(_INTL("由于受到{1}攻击，\n{2}充满了力量！", move.name, target.pbThis(true)))
    battle.pbHideAbilitySplash(target)
  }
)

# 风力发电
BattleHandlers::TargetAbilityOnHit.add(:WINDPOWER,
  proc { |ability, user, target, move, battle|
    next if !move.windMove?
    next if target.effects[PBEffects::Charge] > 0
    battle.pbShowAbilitySplash(target)
    target.effects[PBEffects::Charge] = 2
    battle.pbDisplay(_INTL("由于受到{1}攻击，\n{2}充满了力量！", move.name, target.pbThis(true)))
    battle.pbHideAbilitySplash(target)
  }
)

# 乘风
BattleHandlers::MoveImmunityTargetAbility.add(:WINDRIDER,
  proc { |ability, user, target, move, type, battle|
    next false if !move.windMove?
    next false if user.index == target.index
    battle.pbShowAbilitySplash(target)
    if target.pbCanRaiseStatStage?(PBStats::ATTACK, user, move)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        target.pbRaiseStatStage(PBStats::ATTACK, 1, user)
      else
        target.pbRaiseStatStageByCause(PBStats::ATTACK, 1, user, target.abilityName)
      end
    elsif PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("这不能影响到{1}...", target.pbThis(true)))
    else
      battle.pbDisplay(_INTL("{1}的{2}使{3}无效了！", target.pbThis, target.abilityName, move.name))
    end
    battle.pbHideAbilitySplash(target)
    next true
  }
)
BattleHandlers::AbilityOnSwitchIn.add(:WINDRIDER,
  proc { |ability, battler, battle|
    next if battler.pbOwnSide.effects[PBEffects::Tailwind] <= 0
    next if !battler.pbCanRaiseStatStage?(PBStats::ATTACK, battler)
    battler.pbRaiseStatStageByAbility(PBStats::ATTACK, 1, battler)
  }
)
# 黄金之躯
BattleHandlers::MoveImmunityTargetAbility.add(:GOODASGOLD,
  proc { |ability, user, target, move, type, battle|
    next false if !move.statusMove?
    next false if user.index == target.index
    battle.pbShowAbilitySplash(target)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("这不能影响到{1}...", target.pbThis(true)))
    else
      battle.pbDisplay(_INTL("{1}的{2}使{3}无效了！", target.pbThis, target.abilityName, move.name))
    end
    battle.pbHideAbilitySplash(target)
    next true
  }
)
# 款待
BattleHandlers::AbilityOnSwitchIn.add(:HOSPITALITY,
  proc { |ability, battler, battle|
    next if battler.allAllies.empty? { |b| b.canHeal? }
    battle.pbShowAbilitySplash(battler)
    battler.allAllies.each do |b|
      next if !b.canHeal?
      amt = (b.totalhp / 4).floor
      b.pbRecoverHP(amt)
      battle.pbDisplay(_INTL("{1}喝光了{2}做的抹茶！", b.pbThis, battler.pbThis(true)))
    end
    battle.pbHideAbilitySplash(battler)
  }
)

# 心眼
BattleHandlers::StatLossImmunityAbility.copy(:KEENEYE, :MINDSEYE)
BattleHandlers::AccuracyCalcUserAbility.copy(:KEENEYE, :MINDSEYE)

# 大将
BattleHandlers::DamageCalcUserAbility.add(:SUPREMEOVERLORD,
  proc { |ability,user,target,move,mults,baseDmg,type|
    bonus = user.effects[PBEffects::SupremeOverlord]
    next if bonus <= 0
    mults[BASE_DMG_MULT] *= (1 + (0.1 * bonus))
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:SUPREMEOVERLORD,
  proc { |ability,battler,battle|
    numFainted = [5, battler.num_fainted_allies].min
    next if numFainted <= 0
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}从\n被打倒的同伴身上得到力量了！", battler.pbThis))
    battler.effects[PBEffects::SupremeOverlord] = numFainted
    battle.pbHideAbilitySplash(battler)
  }
)
BattleHandlers::DamageCalcTargetAbility.copy(:SUPREMEOVERLORD,:TMOVERLORD,:SPOVERLORD,:DSOVERLORD)

# 毒锁链
BattleHandlers::UserAbilityOnHit.add(:TOXICCHAIN,
  proc { |ability, user, target, move, battle|
    next if target.fainted?
    next if battle.pbRandom(100) >= 30
    next if target.hasActiveItem?(:COVERTCLOAK)
    battle.pbShowAbilitySplash(user)
    if target.hasActiveAbility?(:SHIELDDUST) && !battle.moldBreaker
      battle.pbShowAbilitySplash(target)
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}不受影响！", target.pbThis))
      end
      battle.pbHideAbilitySplash(target)
    elsif target.pbCanPoison?(user, PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      msg = nil
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        msg = _INTL("{1}中剧毒了！", target.pbThis)
      end
      target.pbPoison(user, msg, true)
    end
    battle.pbHideAbilitySplash(user)
  }
)


# 正义变身
BattleHandlers::AbilityOnSwitchIn.add(:RIGHTTOFIGHT,
  proc { |ability,battler,battle|
    numFainted = [5, battler.num_fainted_allies].min
    next if numFainted <= 0
    next if !battler.isSpecies?(:KRICKETUNE) || battler.form == 1
    battle.pbShowAbilitySplash(battler, true)
    battler.pbChangeFormTransform(1, nil)
    battle.pbReplaceAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}为了\n倒下的队友而正义变身了！", battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)
BattleHandlers::AbilityChangeOnBattlerFainting.add(:RIGHTTOFIGHT,
  proc { |ability, battler, fainted, battle|
    next if battler.opposes?(fainted)
    next if !battler.isSpecies?(:KRICKETUNE) || battler.form == 1
    battle.pbShowAbilitySplash(battler, true)
    battler.pbChangeFormTransform(1, nil)
    battle.pbReplaceAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}为了倒下的{2}\n而正义变身了！", battler.pbThis, fainted.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)

# 全能变身
BattleHandlers::AbilityOnSwitchIn.add(:ZEROTOHERO,
  proc { |ability, battler, battle|
    next if !battler.isSpecies?(:PALAFIN)
    next if battler.form == 0
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}变身后归来了！", battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)
BattleHandlers::AbilityOnSwitchOut.add(:ZEROTOHERO,
  proc { |ability, battler, endOfBattle|
    next if !battler.isSpecies?(:PALAFIN)
    next if battler.form == 1 || endOfBattle
    battler.pbChangeFormTransform(1, "")
  }
)

# 甩不掉的气味
BattleHandlers::TargetAbilityOnHit.copy(:MUMMY, :LINGERINGAROMA)

# 菌丝之力
BattleHandlers::PriorityBracketChangeAbility.add(:MYCELIUMMIGHT,
  proc { |ability,battler,subPri,battle|
    choices = battle.choices[battler.index]
    if choices[0] == :UseMove
      next -1 if choices[2].statusMove?
    end
  }
)
# 跟风
BattleHandlers::AbilityOnOpposingStatGain.add(:OPPORTUNIST,
  proc { |ability, battler, battle, statUps|
    showAnim = true
    battle.pbShowAbilitySplash(battler)
    statUps.each do |stat, increment|
	    next if !battler.pbCanRaiseStatStage?(stat, battler)
      if battler.pbRaiseStatStage(stat, increment, battler, showAnim)
        showAnim = false
      end
    end
    battle.pbDisplay(_INTL("{1}的能力不能再提高了！", battler.pbThis)) if showAnim
    battle.pbHideAbilitySplash(battler)
    battler.pbItemOpposingStatGainCheck(statUps)
    # Mirror Herb can trigger off this ability.
    if !showAnim 
      opposingStatUps = battle.sideStatUps[battler.idxOwnSide]
      battle.allOtherSideBattlers(battler.index).each do |b|
        next if !b || b.fainted?
        if b.itemActive?
          b.pbItemOpposingStatGainCheck(opposingStatUps)
        end
      end
      opposingStatUps.clear
    end
  }
)

# 洁净之盐
BattleHandlers::StatusImmunityAbility.add(:PURIFYINGSALT,
  proc { |ability, battler, status|
    next true
  }
)
BattleHandlers::DamageCalcTargetAbility.add(:PURIFYINGSALT,
  proc { |ability, user, target, move, mults, baseDmg, type|
    mults[ATK_MULT] /= 2 if isConst?(type, PBTypes, :GHOST)
  }
)
# 愤怒甲壳
BattleHandlers::TargetAbilityAfterMoveUse.add(:ANGERSHELL,
  proc { |ability, target, user, move, switched, battle|
    next if !move.damagingMove?
    next if !target.droppedBelowHalfHP
    showAnim = true
    battle.pbShowAbilitySplash(target)
    [PBStats::ATTACK, PBStats::SPATK, PBStats::SPEED].each do |stat|
      next if !target.pbCanRaiseStatStage?(stat, user, nil, true)
      if target.pbRaiseStatStage(stat, 1, user, showAnim)
        showAnim = false
      end
    end
    showAnim = true
    [PBStats::DEFENSE, PBStats::SPDEF].each do |stat|
      next if !target.pbCanLowerStatStage?(stat, user, nil, true)
      if target.pbLowerStatStage(stat, 1, user, showAnim)
        showAnim = false
      end
    end
    battle.pbHideAbilitySplash(target)
  }
)

# 尾甲
BattleHandlers::MoveBlockingAbility.copy(:DAZZLING, :QUEENLYMAJESTY, :ARMORTAIL)

# 甘露之蜜
BattleHandlers::AbilityOnSwitchIn.add(:SUPERSWEETSYRUP,
  proc { |ability, battler, battle|
    next if (battler.effects[PBEffects::SupersweetSyrup] || 0) > 0
    battler.effects[PBEffects::SupersweetSyrup] = 1
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("一股超甜的香气正从\n覆盖着{1}的糖浆中飘散出来！", battler.pbThis(true)))
    battle.battlers.each do |b|
      next if !b || b.fainted? 
      next if !b.opposes?(battler)  
            if b.hasActiveAbility?(:CLEARBODY) || 
              b.hasActiveAbility?(:WHITESMOKE) || 
              b.hasActiveAbility?(:TRANSLUCENTGHOST)
        if b.effects[PBEffects::Substitute] == 0  
          battle.pbDisplay(_INTL("{1}的{2}防止了能力值降低！", b.pbThis, b.abilityName))
        end
        next
      end
      b.pbLowerStatStageByAbility(PBStats::EVASION, 1, battler, false) 
    end
    battle.pbHideAbilitySplash(battler)
  }
)
# 反刍
BattleHandlers::EOREffectAbility.add(:CUDCHEW,
  proc { |ability, battler, battle|
    next if battler.item > 0
    next if !battler.recycleItem || !pbIsBerry?(battler.recycleItem)
    case battler.effects[PBEffects::CudChew]
    when 0 # End round after eat berry
      battler.effects[PBEffects::CudChew] += 1
    else # next turn after eat berry
      battler.effects[PBEffects::CudChew] = 0
      battle.pbShowAbilitySplash(battler, true)
      battle.pbHideAbilitySplash(battler)
      battler.pbHeldItemTriggerCheck(battler.recycleItem, true)
      battler.setRecycleItem(nil)
    end
  }
)


# 绯红脉动
BattleHandlers::AbilityOnSwitchIn.add(:ORICHALCUMPULSE,
  proc { |ability, battler, battle|
    if [PBWeather::Sun, PBWeather::HarshSun].include?(battler.effectiveWeather)
      battle.pbShowAbilitySplash(battler, true)
      battle.pbDisplay(_INTL("{1}沐浴在阳光下，\n激起了古代的脉动！！", battler.pbThis))
      battle.pbHideAbilitySplash(battler)
    else
      battle.pbDisplay(_INTL("{1}令日照变强，\n激起了古代的脉动！！", battler.pbThis))
      pbBattleWeatherAbility(PBWeather::Sun, battler, battle)
    end
  }
)
BattleHandlers::DamageCalcUserAbility.add(:ORICHALCUMPULSE,
  proc { |ability, user, target, move, mults, baseDmg, type|
    if move.physicalMove? && [PBWeather::Sun, PBWeather::HarshSun].include?(user.effectiveWeather)
      mults[ATK_MULT] *= 4 / 3.0
    end
  }
)

# 强子引擎
BattleHandlers::AbilityOnSwitchIn.add(:HADRONENGINE,
  proc { |ability, battler, battle|
    battle.pbShowAbilitySplash(battler, true)
    if battle.field.terrain == PBBattleTerrains::Electric
      battle.pbDisplay(_INTL("{1}利用电气场地\n使未来的机关悦动起来！！", battler.pbThis))
      battle.pbHideAbilitySplash(battler)
    else
      battle.pbDisplay(_INTL("{1}布下电气场地\n使未来的机关悦动起来！！", battler.pbThis))
      battle.pbStartTerrain(battler, PBBattleTerrains::Electric)
    end
  }
)
BattleHandlers::DamageCalcUserAbility.add(:HADRONENGINE,
  proc { |ability, user, target, move, mults, baseDmg, type|
    if move.specialMove? && [PBWeather::Sun, PBWeather::HarshSun].include?(user.effectiveWeather)
      mults[ATK_MULT] *= 4 / 3.0
    end
  }
)


# 太晶变形
BattleHandlers::AbilityOnSwitchIn.add(:TERASHIFT,
  proc { |ability, battler, battle|
    return if !battler.isSpecies?(:TERAPAGOS)
    return if battler.form > 0
    battle.pbShowAbilitySplash(battler, true)
    battle.pbHideAbilitySplash(battler)
    battler.pbChangeFormTransform(1, _INTL("{1}的样子发生了变化！", battler.pbThis))
  }
)

# 太晶甲壳
BattleHandlers::AbilityOnMoveSuccessCheck.add(:TERASHELL,
  proc { |ability, user, target, move, battle|
    next if !target.damageState.terashell
    battle.pbShowAbilitySplash(target)
    battle.pbDisplay(_INTL("{1}的{2}扭曲了\n属性相性！", target.pbThis, target.abilityName))
    battle.pbHideAbilitySplash(target)
  }
)

# 归零化境
BattleHandlers::AbilityOnSwitchIn.add(:TERAFORMZERO,
  proc { |ability, battler, battle|
    weather = battle.field.weather
    terrain = battle.field.terrain
    next if weather == PBWeather::None && terrain == PBBattleTerrains::None
    showSplash = false
    if weather != PBWeather::None && battle.field.defaultWeather == PBBattleTerrains::None
	    showSplash = true
      battle.pbShowAbilitySplash(battler)
      battle.field.weather = PBWeather::None
      battle.field.weatherDuration = 0
      case weather
      when PBWeather::Sun
        battle.pbDisplay(_INTL("阳光暗淡了。"))
      when PBWeather::Rain
        battle.pbDisplay(_INTL("大雨停止了。"))
      when PBWeather::Sandstorm 
        battle.pbDisplay(_INTL("沙暴平息了。"))
      when PBWeather::Hail
        battle.pbDisplay(_INTL("冰雹停止了。"))
      when PBWeather::Snow
        battle.pbDisplay(_INTL("不再下雪了。"))
      when PBWeather::HarshSun
        battle.pbDisplay(_INTL("刺眼的阳光暗淡了！"))
      when PBWeather::HeavyRain 
        battle.pbDisplay(_INTL("倾盆大雨停止了！"))
      when PBWeather::StrongWinds
        battle.pbDisplay(_INTL("神秘的气流消散了！"))
      else
        battle.pbDisplay(_INTL("天气回归了正常。"))
      end
    end
    if terrain != PBBattleTerrains::None && battle.field.defaultTerrain == PBBattleTerrains::None
      battle.pbShowAbilitySplash(battler) if !showSplash
      battle.field.terrain = PBBattleTerrains::None
      battle.field.terrainDuration = 0
      case terrain
      when PBBattleTerrains::Electric
        battle.pbDisplay(_INTL("电光从场上消失了！"))
      when PBBattleTerrains::Grassy
        battle.pbDisplay(_INTL("草从场上消失了！"))
      when PBBattleTerrains::Psychic
        battle.pbDisplay(_INTL("迷雾从场上消失了！"))
      when PBBattleTerrains::Misty
        battle.pbDisplay(_INTL("诡异从场上消失了！"))
      else
        battle.pbDisplay(_INTL("场地回归了正常。"))
      end
    end
    next if !showSplash
    battle.pbHideAbilitySplash(battler)
    battle.allBattlers.each { |b| b.pbCheckFormOnWeatherChange }
    battle.allBattlers.each { |b| b.pbAbilityOnTerrainChange }
    battle.allBattlers.each { |b| b.pbItemTerrainStatBoostCheck }
  }
)

# 毒傀儡
BattleHandlers::AbilityOnInflictingStatus.add(:POISONPUPPETEER,
  proc { |ability, user, battler, status|
    next if !user || user.index == battler.index
    next if status != PBStatuses::POISON
    next if battler.effects[PBEffects::Confusion] > 0
    user.battle.pbShowAbilitySplash(user)
    battler.pbConfuse if battler.pbCanConfuse?(user, false, nil)
    user.battle.pbHideAbilitySplash(user)
  }
)

# 灾祸之X
BattleHandlers::AbilityOnSwitchIn.add(:TABLETSOFRUIN,
  proc { |ability, battler, battle|
    case ability
    when PBAbilities::TABLETSOFRUIN then stat_name = PBStats.getName(PBStats::ATTACK)
    when PBAbilities::SWORDOFRUIN   then stat_name = PBStats.getName(PBStats::DEFENSE)
    when PBAbilities::VESSELOFRUIN  then stat_name = PBStats.getName(PBStats::SPATK)
    when PBAbilities::BEADSOFRUIN   then stat_name = PBStats.getName(PBStats::SPDEF)
    when PBAbilities::TURBOBLAZE    then stat_name = PBStats.getName(PBStats::SPDEF)
    when PBAbilities::TERAVOLT      then stat_name = PBStats.getName(PBStats::DEFENSE)
    when PBAbilities::CALAMITYAERIAL      then stat_name = PBStats.getName(PBStats::SPEED)
    end
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}的{2}\n使周围宝可梦的{3}降低了!", battler.pbThis, battler.abilityName, stat_name))
    battle.pbHideAbilitySplash(battler)
  }
)
BattleHandlers::AbilityOnSwitchIn.copy(:TABLETSOFRUIN, :SWORDOFRUIN, :VESSELOFRUIN, :BEADSOFRUIN, :TURBOBLAZE, :TERAVOLT, :CALAMITYAERIAL)

# 古代活性 / 夸克充能
BattleHandlers::AbilityOnSwitchIn.add(:PROTOSYNTHESIS,
  proc { |ability, battler, battle|
    next if battler.effects[PBEffects::Transform]
    case ability
    when getConst(PBAbilities,:PROTOSYNTHESIS)
      field_check = [PBWeather::Sun, PBWeather::HarshSun].include?(battle.pbWeather)
    when getConst(PBAbilities,:QUARKDRIVE)
      field_check = battle.field.terrain == PBBattleTerrains::Electric
    end
    if !field_check && !battler.effects[PBEffects::BoosterEnergy] && battler.effects[PBEffects::ParadoxStat]
      battle.pbDisplay(_INTL("{1}的{2}\n效果消失了！", battler.pbThis(true), battler.abilityName))
      battler.effects[PBEffects::ParadoxStat] = nil
    end
    next if battler.effects[PBEffects::ParadoxStat]
    next if !field_check && !isConst?(battler.item, PBItems, :BOOSTERENERGY)
    highestStat = nil
    highestStatVal = 0
    stageMul = [2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8]
    stageDiv = [8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2]
    battler.plainStats.each_with_index do |val, stat|
      next if !val
      stage = battler.stages[stat] + 6
      realStat = (val.to_f * stageMul[stage] / stageDiv[stage]).floor
      if realStat > highestStatVal
        highestStatVal = realStat 
        highestStat = stat
      end
    end
    if highestStat
      battle.pbShowAbilitySplash(battler)
      if field_check
        case ability
        when getConst(PBAbilities,:PROTOSYNTHESIS)
          cause = "强烈的阳光"
        when getConst(PBAbilities,:QUARKDRIVE)
          cause = "电气场地"
        end
        battle.pbDisplay(_INTL("#{cause}激活了\n{1}的{2}！", battler.pbThis(true), battler.abilityName))
      elsif isConst?(battler.item, PBItems, :BOOSTERENERGY)
        battler.effects[PBEffects::BoosterEnergy] = true
        battle.pbDisplay(_INTL("{1}使用它的{2}\n激活了{3}！", battler.pbThis, battler.itemName, battler.abilityName))
        battler.pbHeldItemTriggered(battler.item)
      end
      battler.effects[PBEffects::ParadoxStat] = highestStat
      battle.pbDisplay(_INTL("{1}的{2}提高了！", battler.pbThis, PBStats.getName(highestStat)))
      battle.pbHideAbilitySplash(battler)
    end
  }
)
BattleHandlers::AbilityOnSwitchIn.copy(:PROTOSYNTHESIS, :QUARKDRIVE)

BattleHandlers::AbilityOnTerrainChange.add(:QUARKDRIVE,
  proc { |ability, battler, battle, switch_in|
    BattleHandlers::AbilityOnSwitchIn.trigger(ability, battler, battle)
  }
)

# Damage calcs (User).
BattleHandlers::DamageCalcUserAbility.add(:PROTOSYNTHESIS,
  proc { |ability, user, target, move, mults, baseDmg, type|
    next if user.effects[PBEffects::Transform]
    stat = user.effects[PBEffects::ParadoxStat]
    mults[ATK_MULT] *= 1.3 if move.physicalMove? && stat == PBStats::ATTACK
    mults[ATK_MULT] *= 1.3 if move.specialMove?  && stat == PBStats::SPATK
  }
)
BattleHandlers::DamageCalcUserAbility.copy(:PROTOSYNTHESIS, :QUARKDRIVE)

# Damage calcs (Target).
BattleHandlers::DamageCalcTargetAbility.add(:PROTOSYNTHESIS,
  proc { |ability, user, target, move, mults, baseDmg, type|
    next if target.effects[PBEffects::Transform]
    stat = target.effects[PBEffects::ParadoxStat]
    mults[DEF_MULT] *= 1.3 if move.physicalMove? && stat == PBStats::DEFENSE
    mults[DEF_MULT] *= 1.3 if move.specialMove?  && stat == PBStats::SPDEF
  }
)
BattleHandlers::DamageCalcTargetAbility.copy(:PROTOSYNTHESIS, :QUARKDRIVE)

# Speed calcs.
BattleHandlers::SpeedCalcAbility.add(:PROTOSYNTHESIS,
  proc { |ability, battler, mult|
    next mult if battler.effects[PBEffects::Transform]
    next mult * 1.5 if battler.effects[PBEffects::ParadoxStat] == PBStats::SPEED
  }
)
BattleHandlers::SpeedCalcAbility.copy(:PROTOSYNTHESIS, :QUARKDRIVE)


#===============================================================================
# 破灭之息会使除自身外所有精灵的特防减半
#===============================================================================

BattleHandlers::AbilityOnSwitchIn.add(:BROKENBREATH,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}的破灭之息降低了\n其他宝可梦的特防！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)
BattleHandlers::DamageCalcUserAbility.add(:BROKENBREATH,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if move.specialMove?
      mults[ATK_MULT] *= 2
    end
  }
)
BattleHandlers::DamageCalcUserAllyAbility.add(:BROKENBREATH,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if move.specialMove?
      mults[ATK_MULT] *= 2
    end
  }
)
BattleHandlers::DamageCalcTargetAbility.add(:BROKENBREATH,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if move.specialMove?
      mults[DEF_MULT] = (mults[DEF_MULT]*0.5).round
    end
  }
)
BattleHandlers::DamageCalcTargetAllyAbility.add(:BROKENBREATH,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if move.specialMove?
      mults[DEF_MULT] = (mults[DEF_MULT]*0.5).round
    end
  }
)
BattleHandlers::AbilityOnSwitchIn.add(:BROKENBREATH,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}的破灭之息降低了\n在场所有宝可梦的特防！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)

#雷云
BattleHandlers::DamageCalcUserAbility.add(:THUNDERCLOUD,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.battle.pbWeather==PBWeather::Rain &&
       (isConst?(type,PBTypes,:ELECTRIC))
      mults[BASE_DMG_MULT] *= 1.5
    end
  }
)
BattleHandlers::AbilityOnSwitchIn.add(:THUNDERCLOUD,
  proc { |ability,battler,battle|
    next if battle.field.terrain == PBBattleTerrains::Electric
    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(battler,PBBattleTerrains::Electric)
    # NOTE: The ability splash is hidden again in def pbStartTerrain.
  }
)

#灵异躯体
BattleHandlers::DamageCalcUserAbility.add(:EERIEBODY,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] = (mults[ATK_MULT]*1.3) if isConst?(type,PBTypes,:DARK) || isConst?(type,PBTypes,:PSYCHIC)
  }
)

#零和躯体
BattleHandlers::DamageCalcUserAbility.add(:ZEROSUMBODY,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] = (mults[ATK_MULT]*1.3) if isConst?(type,PBTypes,:LIGHT) || isConst?(type,PBTypes,:PSYCHIC)
  }
)
# 幽灵暴走
BattleHandlers::TargetAbilityOnHit.add(:GHOSTRAMPAGE,
  proc { |ability,user,target,move,battle|
    next if !isConst?(move.calcType,PBTypes,:DARK)
    target.pbRaiseStatStageByAbility(PBStats::ATTACK,2,target)
  }
)

#无限光/暗
BattleHandlers::AbilityOnSwitchIn.add(:ENDLESSDARKN,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}降下了黑暗！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)
BattleHandlers::AbilityOnSwitchIn.add(:ETRTNALIGHT,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}降下了圣光！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)

# 妖醒之歌
#变属性
BattleHandlers::MoveBaseTypeModifierAbility.add(:FAIRYSONG,
  proc { |ability,user,move,type|
    next getConst(PBTypes,:FAIRY) if hasConst?(PBTypes,:FAIRY) && move.soundMove?
  }
)
#加威力
BattleHandlers::DamageCalcUserAbility.add(:FAIRYSONG,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 1.2 if move.soundMove?
  }
)

# 妖醒之舞
#变属性
BattleHandlers::MoveBaseTypeModifierAbility.add(:FAIRYDANCE,
  proc { |ability,user,move,type|
    next getConst(PBTypes,:FAIRY) if user.hp<=user.totalhp/2 &&
                                     hasConst?(PBTypes,:FAIRY) &&
                                     move.contactMove?
  }
)
#加先制
BattleHandlers::PriorityChangeAbility.add(:FAIRYDANCE,
  proc { |ability,battler,move,pri|
    next pri+1 if battler.hp<=battler.totalhp/2 && move.contactMove?
  }
)

# 星界裂隙
#加威力
BattleHandlers::DamageCalcUserAbility.add(:STARFISSURE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] = (mults[ATK_MULT]*1.3) if isConst?(type,PBTypes,:VOID)
  }
)
#开空间
BattleHandlers::AbilityOnSwitchIn.add(:STARFISSURE,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}布下了星界丝缕！",battler.pbThis))
    if battle.field.effects[PBEffects::TrickRoom]>0
      battle.field.effects[PBEffects::TrickRoom] = 0
      battle.pbDisplay(_INTL("{1}使空间恢复正常了！",battler.pbThis))
    else
      battle.field.effects[PBEffects::TrickRoom] = 8
      battle.pbDisplay(_INTL("{1}扭曲了时空！",battler.pbThis))
    end
    battle.pbHideAbilitySplash(battler)
  }
)
BattleHandlers::DamageCalcTargetAbility.copy(:ETRTNALIGHT,:STARFISSURE)

#此身无间加速
BattleHandlers::EOREffectAbility.add(:SEAMLESS,
  proc { |ability,battler,battle|
    if battler.turnCount>0 && battler.pbCanRaiseStatStage?(PBStats::SPEED,battler)
      battler.pbRaiseStatStageByAbility(PBStats::SPEED,1,battler)
    end
  }
)
#北冥之刃
BattleHandlers::CriticalCalcUserAbility.add(:BEIMINGBLADE,
  proc { |ability,user,target,c|
    next c+1
  }
)

#命运之轮
BattleHandlers::EOREffectAbility.add(:MOERAE,
  proc { |ability,battler,battle|
    randomUp = []; randomDown = []
    PBStats.eachMainBattleStat do |s|
      randomUp.push(s) if battler.pbCanRaiseStatStage?(s,battler)
      randomDown.push(s) if battler.pbCanLowerStatStage?(s,battler)
    end
    next if randomUp.length==0 && randomDown.length==0
    battle.pbShowAbilitySplash(battler)
    if randomUp.length>0
      r = battle.pbRandom(randomUp.length)
      battler.pbRaiseStatStageByAbility(randomUp[r],1,battler,false)
      randomDown.delete(randomUp[r])
    end
    battle.pbHideAbilitySplash(battler)
    battler.pbItemStatRestoreCheck if randomDown.length>0
  }
)
#黑手
BattleHandlers::AbilityOnSwitchIn.add(:BLACKHAND,
  proc { |ability,battler,battle|
    stat = PBStats::ACCURACY
    battler.pbRaiseStatStageByAbility(stat,1,battler)
  }
)

# 皆杀恶魔
#接触类招式威力翻倍（基础威力）
BattleHandlers::DamageCalcUserAbility.add(:DEMONKILLER,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 2 if move.contactMove?
  }
)
#不会混乱
BattleHandlers::StatusCureAbility.copy(:OWNTEMPO,:DEMONKILLER)


# 酣眠
#出场时睡觉
BattleHandlers::AbilityOnSwitchIn.add(:SLEEPSOUNDLY,
  proc { |ability, battler, battle|
    return if !battler.isSpecies?(:ROSEDRAGON)
    return if !battler.pbCanSleep?(battler,false)
    battle.pbShowAbilitySplash(battler)
    battler.pbSleepSelf(_INTL("{1}陷入了酣睡！",battler.pbThis),2)
    battle.pbHideAbilitySplash(battler)
  }
)
#受到攻击时醒来
BattleHandlers::TargetAbilityOnHit.add(:SLEEPSOUNDLY,
  proc { |ability,user,target,move,battle|
    return if !target.isSpecies?(:ROSEDRAGON)
    return if !target.asleep?
    battle.pbShowAbilitySplash(target)
    target.pbCureStatus(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
    target.pbCheckFormOnStatusChange
    battle.pbHideAbilitySplash(target)
  }
)

# 幽灵岩壳
#受到物理招式伤害时，速度会提高到最大。
BattleHandlers::TargetAbilityOnHit.add(:PHANTOMROCK,
  proc { |ability,user,target,move,battle|
    next if !move.physicalMove?
    next if !target.pbCanRaiseStatStage?(PBStats::SPEED,target)
    battle.pbShowAbilitySplash(target)
    target.stages[PBStats::SPEED] = 6
    battle.pbCommonAnimation("StatUp",target)
    battle.pbHideAbilitySplash(target)
  }
)
# 除恶
#对幽灵或恶属性目标造成效果绝佳伤害时伤害翻倍。
BattleHandlers::DamageCalcUserAbility.add(:ELIMINATEEVIL,
  proc { |ability,user,target,move,mults,baseDmg,type|
    next unless target.pbHasType?(:GHOST) || target.pbHasType?(:DARK)
    next if move.statusMove?
    next if type<0 || !PBTypes.superEffective?(target.damageState.typeMod)
    mults[FINAL_DMG_MULT] *= 2
  }
)

# 音跃节拍
BattleHandlers::DamageCalcUserAbility.add(:SOUNDSTRIDE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] = (mults[ATK_MULT]*1.3) if isConst?(type,PBTypes,:BUG)
  }
) 
BattleHandlers::PriorityChangeAbility.add(:SOUNDSTRIDE,
	proc { |ability,battler,move,pri|
    next pri+1 if isConst?(move.type,PBTypes,:BUG)
  }
)

# 如意神兵
BattleHandlers::DamageCalcUserAbility.add(:RUYIBLADE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[FINAL_DMG_MULT] *= 2 if PBTypes.resistant?(target.damageState.typeMod)
  }
)
# 钢铁始皇
BattleHandlers::StatLossImmunityAbility.add(:STEELDYNASTY,
  proc { |ability,battler,stat,battle,showMessages|
    # 无视 NeutralizingGas，直接防止能力下降
    if showMessages
      battle.pbShowAbilitySplash(battler)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}的能力值不能被降低了！",battler.pbThis))
      else
        battle.pbDisplay(_INTL("{1}的{2}防止了能力值降低！",battler.pbThis,battler.abilityName))
      end
      battle.pbHideAbilitySplash(battler)
    end
    next true
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:STEELDYNASTY,
  proc { |ability,battler,battle|
    next if battle.field.effects[PBEffects::NeutralizingGas]
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}的钢铁始皇使所有特性失效！",battler.pbThis))
    battle.field.effects[PBEffects::NeutralizingGas] = true
    battle.pbHideAbilitySplash(battler)
  }
)

# 欧若拉之声
BattleHandlers::AbilityOnSwitchIn.add(:AURAVOICE,
  proc { |ability, battler, battle|
    next if battler.pbOwnSide.effects[PBEffects::AuroraVeil] >  0
    battle.pbShowAbilitySplash(battler)
    battler.pbOwnSide.effects[PBEffects::AuroraVeil] = 5
    battle.pbDisplay(_INTL("{1}受到的物理和特殊伤害减弱了！", battler.pbTeam(true)))
    battle.pbHideAbilitySplash(battler)
  }
)

# 女王之心
BattleHandlers::EOREffectAbility.add(:MAGICQUEEN,
  proc { |ability,battler,battle|
    next if battler.turnCount==0
    battle.pbShowAbilitySplash(battler)
    battler.eachOpposing{|b|
      next if !b.pbCanConfuse?(battler,true,self)
      b.pbConfuse
    }
    battle.pbHideAbilitySplash(battler)
  }
)
# 恶魔大衣
BattleHandlers::EOREffectAbility.add(:DEMONCLOAK,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battler.eachOpposing{|b|
      next if b.effects[PBEffects::Yawn]>0
      next if !b.pbCanSleep?(battler,true,self)
      b.effects[PBEffects::Yawn] = 2
      battle.pbDisplay(_INTL("{1}让{2}昏昏欲睡！",battler.pbThis,b.pbThis(true)))
    }
    battle.pbHideAbilitySplash(battler)
  }
)
# 源石内境
BattleHandlers::MoveImmunityTargetAbility.add(:YSNJ,
  proc { |ability,user,target,move,type,battle|
    next false if move.statusMove?
    #概率30%
    next false if battle.pbRandom(100) >= 30
    battle.pbShowAbilitySplash(target)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("{1}免疫了{2}使出的招式！", target.pbThis, user.pbThis))
    else
      battle.pbDisplay(_INTL("{1}的{2}，使{3}无效化了！",target.pbThis,target.abilityName, move.name))
    end
    target.pbRaiseStatStage(PBStats::SPATK, 1, target)
    battle.pbHideAbilitySplash(target)
    next true
  }
)


# 人鱼之声
# 自身声音招式变为水属性，
BattleHandlers::MoveBaseTypeModifierAbility.add(:MERMAIDSOUND,
  proc { |ability,user,move,type|
    next if !move.soundMove?
    next if !move.pbDamagingMove?   # 排除叫声
    next if !hasConst?(PBTypes,:WATER)
    next getConst(PBTypes,:WATER)
  }
)
# 并回复已损失HP的1/4。
BattleHandlers::UserAbilityEndOfMove.add(:MERMAIDSOUND,
  proc { |ability,user,targets,move,battle|
    next if !move.soundMove?
    next if !move.pbDamagingMove?   # 排除叫声
    next if !user.canHeal?
    heal_hp = [1,((user.totalhp-user.hp)/4.0).round].max
    user.pbRecoverHP(heal_hp)
  }
)

# 辉耀领域
BattleHandlers::AbilityOnSwitchIn.add(:RADIANTRULE,
  proc { |ability,battler,battle|
    next if battle.field.terrain==PBBattleTerrains::Psychic
    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(battler,PBBattleTerrains::Psychic)
  }
)

# 灾厄·海孽·焱祟
BattleHandlers::AbilityOnSwitchIn.add(:CALAMITYABYSSAL,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}带来了灾厄般的威压……",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
    pbBattleWeatherAbility(PBWeather::Rain,battler,battle)
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:CALAMITYINFERNAL,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}带来了灾厄般的威压……",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
    pbBattleWeatherAbility(PBWeather::Sun,battler,battle)
  }
)

# 蔷薇花园
BattleHandlers::AbilityOnSwitchIn.add(:ROSEGARDEN,
  proc { |ability,battler,battle|
    next if battle.field.terrain==PBBattleTerrains::Grassy
    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(battler,PBBattleTerrains::Grassy)
    # NOTE: The ability splash is hidden again in def pbStartTerrain.
  }
)
BattleHandlers::DamageCalcUserAbility.add(:ROSEGARDEN,
  proc { |ability,user,target,move,mults,baseDmg,type|
   if user.battle.field.terrain==PBBattleTerrains::Grassy &&
       (isConst?(type,PBTypes,:FIRE))
      mults[BASE_DMG_MULT] *= 1.5
    end
  }
)
# 蔷薇箱庭
BattleHandlers::AbilityOnSwitchIn.add(:ROSEARCADIA,
  proc { |ability,battler,battle|
    next if battle.field.terrain==PBBattleTerrains::Grassy
    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(battler,PBBattleTerrains::Grassy)
    # NOTE: The ability splash is hidden again in def pbStartTerrain.
  }
)
BattleHandlers::DamageCalcUserAbility.add(:ROSEGARDEN,
  proc { |ability,user,target,move,mults,baseDmg,type|
   if user.battle.field.terrain==PBBattleTerrains::Grassy &&
       (isConst?(type,PBTypes,:ICE))
      mults[BASE_DMG_MULT] *= 1.5
    end
  }
)


# 翠野护场
BattleHandlers::AbilityOnSwitchIn.add(:VERDANTWARD,
  proc { |ability,battler,battle|
    next if battle.field.terrain==PBBattleTerrains::Grassy
    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(battler,PBBattleTerrains::Grassy)
    # NOTE: The ability splash is hidden again in def pbStartTerrain.
  }
)

BattleHandlers::DamageCalcTargetAbility.add(:VERDANTWARD,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if user.battle.field.terrain==PBBattleTerrains::Grassy
      mults[DEF_MULT] *= 1.5
    end
  }
)


BattleHandlers::AbilityOnSwitchIn.add(:PSYCHICEDGE,
  proc { |ability,battler,battle|
    next if battle.field.terrain==PBBattleTerrains::Psychic
    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(battler,PBBattleTerrains::Psychic)
    # NOTE: The ability splash is hidden again in def pbStartTerrain.
  }
)

#永恒之焱
BattleHandlers::AbilityOnSwitchIn.add(:ETERNALFLAME,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}降下了圣光",battler.pbThis))
    pbBattleWeatherAbility(PBWeather::Sun,battler,battle)
    battle.pbHideAbilitySplash(battler)
  }
)

 
# 竹翎
BattleHandlers::UserAbilityEndOfMove.add(:BAMBOOFEATHER,
  proc { |ability,user,targets,move,battle|
    next unless move.damagingMove?
    stage_atk = user.stages[PBStats::ATTACK]
    stage_speed  = user.stages[PBStats::SPEED]
    targets.each do |t|
      next if t.fainted?
      next if !t.opposes?(user)
      next if t.lastHPLostFromFoe == 0
      next if !t.takesIndirectDamage?(PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      battle.pbShowAbilitySplash(user)
      
      n  = 1
      n += 1 if t.totalhp > user.totalhp
      n += 1 if t.attack > user.attack
      n += 1 if t.stages[PBStats::ATTACK] > stage_atk
      n += 1 if t.spatk > user.attack
      n += 1 if t.stages[PBStats::SPATK] > stage_atk
      n += 1 if t.speed > user.speed
      n += 1 if t.stages[PBStats::SPEED] > stage_speed
      
      battle.scene.pbDamageAnimation(t)
      extra_damage = (t.lastHPLostFromFoe * n / 8.0).floor
      t.pbReduceHP(extra_damage, true)
      if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}受伤了！", t.pbThis))
      else
        battle.pbDisplay(_INTL("{1}受到{2}的{3}伤害了！", t.pbThis, user.pbThis, user.abilityName))
      end
      t.pbFaint if t.fainted?
      battle.pbHideAbilitySplash(user)
    end
  }
)

# 怒炎
#陷入异常状态时或有降低能力时，攻击会提高。
BattleHandlers::DamageCalcUserAbility.add(:ANGERFLAME,
  proc { |ability,user,target,move,mults,baseDmg,type|
    next if !move.physicalMove?
    lossStages = false
    PBStats.eachMainBattleStat { |s|
      if user.stages[s] < 0
        lossStages = true
        break
      end
    }
    mults[ATK_MULT] *= 1.5 if user.pbHasAnyStatus? || lossStages
  }
)

# 雨幕
#雨天时双防提高30%~60%，
BattleHandlers::DamageCalcTargetAbility.add(:RAINCURTAIN,
  proc { |ability,user,target,move,mults,baseDmg,type|
    w = target.battle.pbWeather
    if (w==PBWeather::Rain || w==PBWeather::HeavyRain) && !target.hasUtilityUmbrella?
      ret = 0
      user.battle.pbParty(user.index).each do |pkmn|
        next if !pkmn || pkmn.fainted?
        next if !pkmn.hasType?(:WATER) && !pkmn.hasType?(:POISON)
        ret += 1
      end
      ret = [ret, 6].min
      mult = 1.24 + ret * 0.06
      mults[DEF_MULT] = (mults[DEF_MULT] * mult).round
    end
  }
)
#不会陷入异常状态。
BattleHandlers::StatusImmunityAbility.add(:RAINCURTAIN,
  proc { |ability,battler,status|
    w = battler.battle.pbWeather
    next true if (w==PBWeather::Rain || w==PBWeather::HeavyRain) &&
    !battler.hasUtilityUmbrella?
  }
)

#聚能
BattleHandlers::DamageCalcUserAbility.add(:ENERGYACCU,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if isConst?(type,PBTypes,:STEEL) ||
       isConst?(type,PBTypes,:GRASS) ||
       isConst?(type,PBTypes,:ROCK)
      mults[BASE_DMG_MULT] *= 1.3
    end
  }
)

#汇流
BattleHandlers::DamageCalcUserAbility.add(:CONFLUENCE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if isConst?(type,PBTypes,:WATER) ||
       isConst?(type,PBTypes,:FIRE) ||
       isConst?(type,PBTypes,:ELECTRIC)
      mults[BASE_DMG_MULT] *= 1.3
    end
  }
)

# 毛棉棉 - 受到招式伤害减半，每回合损失1/8HP
BattleHandlers::DamageCalcTargetAbility.add(:FLUFFYCOAT,
  proc { |ability,user,target,move,mults,baseDmg,type|
    if move.damagingMove?
      mults[FINAL_DMG_MULT] /= 2
    end
  }
)

# 回合结束损失HP
BattleHandlers::EOREffectAbility.add(:FLUFFYCOAT,
  proc { |ability,battler,battle|
    if battler.turnCount > 0 && battler.hp > 0
      # 使用 pbReduceHP 代替 pbRecoilDamage
      battler.pbReduceHP(battler.totalhp / 8, false)
      battle.pbDisplay(_INTL("{1}因毛棉棉而受到了伤害！", battler.pbThis))
      battler.pbItemHPHealCheck  # 检查是否有道具因HP变化而触发
      battler.pbFaint if battler.fainted?  # 检查是否濒死
    end
  }
)

#蔷薇庇护
BattleHandlers::AbilityOnSwitchIn.add(:ROSYAEGIS,
  proc { |ability, battler, battle|
    next if battler.pbOwnSide.effects[PBEffects::AuroraVeil] >  0
    battle.pbShowAbilitySplash(battler)
    battler.pbOwnSide.effects[PBEffects::AuroraVeil] = 5
    battle.pbDisplay(_INTL("{1}召唤了光之花幕！\n受到的物理和特殊伤害减弱了！", battler.pbTeam(true)))
    battle.pbHideAbilitySplash(battler)
  }
)

#时间领主
BattleHandlers::EOREffectAbility.add(:TMOVERLORD,
  proc { |ability,battler,battle|
    # A Pokémon's turnCount is 0 if it became active after the beginning of a
    # round
    if battler.turnCount>0 && battler.pbCanRaiseStatStage?(PBStats::SPEED,battler)
      battler.pbRaiseStatStageByAbility(PBStats::SPEED,1,battler)
    end
  }
)

#毁坏领主
BattleHandlers::TrappingTargetAbility.add(:DSOVERLORD,
  proc { |ability,switcher,bearer,battle|
    next true if !switcher.hasActiveAbility?(:DSOVERLORD)
  }
)

#圣约之链
BattleHandlers::AbilityOnBattlerFainting.add(:DIVINEPACT,
  proc { |ability,battler,fainted,battle|
    battler.pbRaiseStatStageByAbility(PBStats::SPATK,1,battler)
  }
)
#困兽之斗
BattleHandlers::TargetAbilityAfterMoveUse.add(:LASTSTAND,
  proc { |ability,target,user,move,switched,battle|
    next if !move.damagingMove?
    next if target.damageState.initialHP<target.totalhp/2 || target.hp>=target.totalhp/2
    next if !target.pbCanRaiseStatStage?(PBStats::ATTACK,target)
    target.pbRaiseStatStageByAbility(PBStats::ATTACK,1,target)
  }
)

#冥焰战意
BattleHandlers::UserAbilityEndOfMove.add(:NETHERDRIVE,
  proc { |ability,user,targets,move,battle|
    next if battle.pbAllFainted?(user.idxOpposingSide)
    numFainted = 0
    targets.each { |b| numFainted += 1 if b.damageState.fainted }
    next if numFainted==0 || !user.pbCanRaiseStatStage?(PBStats::SPATK,user)
    user.pbRaiseStatStageByAbility(PBStats::SPATK,numFainted,user)
  }
)

#===============================================================================
# 忏悔表
#===============================================================================
BattleHandlers::TrappingTargetAbility.add(:CONFESSIONLIST,
  proc { |ability, battler, trappingBattler, battle|
    next false if battler.pbHasType?(:FLYING)
    next true
  }
)

BattleHandlers::AbilityOnSwitchIn.add(:CONFESSIONLIST,
  proc { |ability, battler,battle|
    battle.pbShowAbilitySplash(battler)
        battle.eachOtherSideBattler(battler.index) do |b|
      battle.pbDisplay(_INTL("{1}被{2}的忏悔表束缚了！", 
        b.pbThis, battler.pbThis(true)))
    end
    battle.pbHideAbilitySplash(battler)
  }
)
# 厉鬼悲鸣
BattleHandlers::UserAbilityEndOfMove.add(:GHASTLYWAIL,
  proc { |ability,user,targets,move,battle|
    next if battle.pbAllFainted?(user.idxOpposingSide)
    numFainted = 0
    targets.each { |b| numFainted += 1 if b.damageState.fainted }
    next if numFainted==0 || !user.pbCanRaiseStatStage?(PBStats::SPATK,user)
    user.pbRaiseStatStageByAbility(PBStats::SPATK,numFainted,user)
  }
)

BattleHandlers::DamageCalcUserAbility.add(:GHASTLYWAIL,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] = (mults[BASE_DMG_MULT]*1.5).round if move.soundMove?
  }
)


#===============================================================================
# 超级太阳 (SUPERSUN)
#===============================================================================

BattleHandlers::AbilityOnSwitchIn.add(:SUPERSUN,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}散发出如烈日般的光芒！", battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)

BattleHandlers::DamageCalcUserAbility.add(:SUPERSUN,
  proc { |ability,user,target,move,mults,baseDmg,type|
    # 如果环境已经是晴天，不重复计算
    next if user.battle.pbWeather == PBWeather::Sun || user.battle.pbWeather == PBWeather::HarshSun
    if isConst?(type, PBTypes, :FIRE)
      mults[ATK_MULT] *= 1.5
    elsif isConst?(type, PBTypes, :WATER)
      mults[ATK_MULT] /= 2
    end
  }
)

BattleHandlers::AccuracyCalcUserAbility.add(:SUPERSUN,
  proc { |ability,mods,user,target,move,type|
    next if user.battle.pbWeather == PBWeather::Sun || user.battle.pbWeather == PBWeather::HarshSun
    # 晴天下打雷和暴风命中降低
    if isConst?(move.id, PBMoves, :THUNDER) || isConst?(move.id, PBMoves, :HURRICANE)
      mods[BASE_ACC] = 50
    end
  }
)

#星火长明
BattleHandlers::EOREffectAbility.add(:ETERNALSTAR,
  proc { |ability,battler,battle|
    next if !battler.canHeal?
    battle.pbShowAbilitySplash(battler)
    battler.pbRecoverHP(battler.totalhp/8)
    battle.pbDisplay(_INTL("{1}的伤口通过星火愈合了！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)

#悲雨落叹
BattleHandlers::AbilityOnSwitchIn.add(:BESTOWEDRAIN,
  proc { |ability,battler,battle|
    pbBattleWeatherAbility(PBWeather::Rain,battler,battle)
  }
)
BattleHandlers::SpeedCalcAbility.add(:BESTOWEDRAIN,
  proc { |ability,battler,mult|
    w = battler.battle.pbWeather
    next mult*2 if w==PBWeather::Rain || w==PBWeather::HeavyRain if !battler.hasUtilityUmbrella?
  }
)

#怒炎焚击
BattleHandlers::DamageCalcUserAbility.add(:RAGEFIREBLAST,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 4/3.0 if move.contactMove?
  }
)

#清澈之心
BattleHandlers::EOREffectAbility.add(:ETERNALSTAR,
  proc { |ability,battler,battle|
    next if !battler.canHeal?
    battle.pbShowAbilitySplash(battler)
    battler.pbRecoverHP(battler.totalhp/8)
    battle.pbDisplay(_INTL("{1}的伤口愈合了！",battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)


#===============================================================================
# 高歌舞台 (SOARINGSTAGE)
# 声音类招式变为飞行系，且在场时我方招式威力提升30%
#===============================================================================

# 1. 声音类招式变为飞行系
BattleHandlers::MoveBaseTypeModifierAbility.add(:SOARINGSTAGE,
  proc { |ability, user, move, type|
    next if !move.soundMove?   # 使用 k 标志判断声音类
    next if !hasConst?(PBTypes, :FLYING)
    next getConst(PBTypes, :FLYING)
  }
)

# 2. 在场时自身招式威力提升30%
BattleHandlers::DamageCalcUserAbility.add(:SOARINGSTAGE,
  proc { |ability, user, target, move, mults, baseDmg, type|
    mults[FINAL_DMG_MULT] = (mults[FINAL_DMG_MULT] * 1.3).round
  }
)


#===============================================================================
# 比翼煌炎 (BIYIHUANGYAN)
# 火与光属性招式必定造成灼伤。灼伤伤害转化为自身HP回复。晴天下灼伤无法治愈。
#===============================================================================

# 1. 火与光属性招式必定造成灼伤（100%触发）
BattleHandlers::UserAbilityOnHit.add(:BIYIHUANGYAN,
  proc { |ability, user, target, move, battle|
    # 只对火属性和光属性招式生效
    next if !isConst?(move.calcType, PBTypes, :FIRE) && !isConst?(move.calcType, PBTypes, :LIGHT)
    next if !move.damagingMove?   # 只对攻击招式生效
    next if target.fainted?
    next if target.burned?        # 已经灼伤则不重复触发
    next if target.hasActiveItem?(:COVERTCLOAK)
    
    battle.pbShowAbilitySplash(user)
    if target.hasActiveAbility?(:SHIELDDUST) && !battle.moldBreaker
      battle.pbShowAbilitySplash(target)
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        battle.pbDisplay(_INTL("{1}没有受到影响！", target.pbThis))
      end
      battle.pbHideAbilitySplash(target)
    elsif target.pbCanBurn?(user, PokeBattle_SceneConstants::USE_ABILITY_SPLASH)
      msg = nil
      if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH
        msg = _INTL("{1}的{2}使{3}灼伤了！", user.pbThis, user.abilityName, target.pbThis(true))
      end
      target.pbBurn(user, msg)
    end
    battle.pbHideAbilitySplash(user)
  }
)

# 2. 灼伤伤害转化为自身HP回复（回合结束时触发）
BattleHandlers::EOREffectAbility.add(:BIYIHUANGYAN,
  proc { |ability, battler, battle|
    next if !battler.burned?
    # 回复灼伤伤害量（最大HP的1/16）
    next if !battler.canHeal?
    battle.pbShowAbilitySplash(battler)
    heal_amt = battler.totalhp / 16
    battler.pbRecoverHP(heal_amt)
    if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
      battle.pbDisplay(_INTL("{1}将灼伤的痛楚转化为了力量，HP回复了！", battler.pbThis))
    else
      battle.pbDisplay(_INTL("{1}的{2}将灼伤转化为了HP！", battler.pbThis, battler.abilityName))
    end
    battle.pbHideAbilitySplash(battler)
  }
)

# 3. 晴天下灼伤无法被治愈（阻止治愈效果）
BattleHandlers::StatusCureAbility.add(:BIYIHUANGYAN,
  proc { |ability, battler|
    w = battler.battle.pbWeather
    # 晴天或大晴天时，灼伤无法被治愈
    if (w == PBWeather::Sun || w == PBWeather::HarshSun) && battler.burned?
      battler.battle.pbShowAbilitySplash(battler)
      battler.battle.pbDisplay(_INTL("在晴天的照耀下，{1}的灼伤无法被治愈！", battler.pbThis))
      battler.battle.pbHideAbilitySplash(battler)
      next true   # 阻止治愈
    end
  }
)

# 圣光涅槃
BattleHandlers::TargetAbilityOnHit.add(:SACREDREBORN,
  proc { |ability,user,target,move,battle|
    next if !target.isSpecies?(:SUGARDEVOIR)
    if target.fainted? && !target.reborn?
      target.setReborn
      battle.scene.pbReborn1Battler(target)
      battle.pbDisplayPaused(_INTL("{1}倒下了...？",target.pbThis))
      pbWait(20)
      
      battle.pbShowAbilitySplash(target)
      battle.scene.pbReborn2Battler(target)
      
      target.pbRecoverHP(target.totalhp / 2,false)
      target.pbCureStatus(false)
      target.pbCureConfusion
      
      battle.pbDisplay(_INTL("<c3=FFCCCC,FF0000>{1}从濒死中复活了！</c3>",target.pbThis))
      
      battle.pbHideAbilitySplash(target)
    end
  }
)
# 暗渊返魄
BattleHandlers::TargetAbilityOnHit.add(:ABYSSREBORN,
  proc { |ability,user,target,move,battle|
    next if !target.isSpecies?(:SUJINRAKU)
    if target.fainted? && !target.reborn?
      target.setReborn
      battle.scene.pbReborn1Battler(target)
      battle.pbDisplayPaused(_INTL("{1}倒下了...？",target.pbThis))
      pbWait(20)
      
      battle.pbShowAbilitySplash(target)
      battle.scene.pbReborn2Battler(target)
      
      target.pbRecoverHP(target.totalhp / 2,false)
      target.pbCureStatus(false)
      target.pbCureConfusion
      
      battle.pbDisplay(_INTL("<c3=FFCCCC,FF0000>{1}从濒死中复活了！</c3>",target.pbThis))
      
      battle.pbHideAbilitySplash(target)
    end
  }
)
#===============================================================================
# 贯穿钻 (PIERCINGDRILL)
# 使用接触类招式时无视防守效果，但伤害降低至原本的1/4。
#===============================================================================

# --- 伤害计算：接触类招式伤害变为1/4 ---
BattleHandlers::DamageCalcUserAbility.add(:PIERCINGDRILL,
  proc { |ability, user, target, move, mults, baseDmg, type|
    next if !move.contactMove?
    mults[FINAL_DMG_MULT] /= 4.0
  }
)

# 火焰鬃毛
BattleHandlers::DamageCalcUserAbility.add(:FIREMANE,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] *= 1.5 if isConst?(type,PBTypes,:FIRE)
  }
)

# 闪耀变身
BattleHandlers::AbilityOnSwitchIn.add(:SHINYHERO,
  proc { |ability, battler, battle|
    next if !battler.isSpecies?(:SHINYGARCHOMP)
    next if battler.form == 0
    battle.pbShowAbilitySplash(battler)
    battle.pbDisplay(_INTL("{1}变身后归来了！", battler.pbThis))
    battle.pbHideAbilitySplash(battler)
  }
)
BattleHandlers::AbilityOnSwitchOut.add(:SHINYHERO,
  proc { |ability, battler, endOfBattle|
    next if !battler.isSpecies?(:SHINYGARCHOMP)
    next if battler.form == 1 || endOfBattle
    battler.pbChangeFormTransform(1, "")
  }
)

#===============================================================================
# 深海幻惑 (DEEPSEAFASCINATION)
# 出场时，降低直接对位的对手特攻1级。
# 对陷入魅惑的目标，自身招式伤害提升15%。
#===============================================================================

# --- 出场效果：只降低直接对位对手的特攻1级 ---
BattleHandlers::AbilityOnSwitchIn.add(:DEEPSEAFASCINATION,
  proc { |ability,battler,battle|
    battle.pbShowAbilitySplash(battler)
    target = battler.pbDirectOpposing
    if target && !target.fainted? &&
       target.pbCanLowerStatStage?(PBStats::SPATK, battler)
      target.pbLowerStatStage(PBStats::SPATK, 1, battler)
    end
    battle.pbHideAbilitySplash(battler)
  }
)



# 玩乐之心
BattleHandlers::StatusImmunityAbility.add(:PLAYFULHEART,
  proc { |ability, battler, status|
    next true
  }
)
BattleHandlers::DamageCalcUserAbility.add(:PLAYFULHEART,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[BASE_DMG_MULT] *= 4/3.0 if move.contactMove?
  }
)

# 万相乖离
BattleHandlers::DamageCalcUserAbility.add(:WANXIANG,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] /= 2 if user.hp<=user.totalhp/2
  }
)
BattleHandlers::DamageCalcUserAbility.add(:WANXIANG,
  proc { |ability,user,target,move,mults,baseDmg,type|
    mults[ATK_MULT] *= 2 if move.physicalMove?
  }
)



# 风暴之眼
BattleHandlers::AbilityOnSwitchIn.add(:STORMEYE,
  proc { |ability,battler,battle|
    pbBattleWeatherAbility(PBWeather::Rain,battler,battle)
  }
)
BattleHandlers::DamageCalcTargetAbility.add(:STORMEYE,
  proc { |ability,user,target,move,mults,baseDmg,type|
      if user.isSpecies?(:LUGIA) && isConst?(type,PBTypes,:ELECTRIC)
        mults[BASE_DMG_MULT] *= 0.25
      end
  }
)

# 虹霓之穹
BattleHandlers::AbilityOnSwitchIn.add(:RAINBOWARCH,
  proc { |ability,battler,battle|
    pbBattleWeatherAbility(PBWeather::Sun,battler,battle)
  }
)
BattleHandlers::DamageCalcTargetAbility.add(:RAINBOWARCH,
  proc { |ability,user,target,move,mults,baseDmg,type|
      if user.isSpecies?(:HOOH) && isConst?(type, PBTypes, :WATER)
        mult = [PBWeather::Sun, PBWeather::HarshSun].include?(target.pbWeather) ? 0.5 : 0.25
        mults[BASE_DMG_MULT] *= mult
      end
  }
)