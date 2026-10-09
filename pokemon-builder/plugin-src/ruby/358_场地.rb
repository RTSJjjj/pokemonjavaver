#===============================================================================
# 虫惑场地
#===============================================================================

module PBBattleTerrains
  BugLure = 5
end

module BugLureTerrainSettings
  # 不会被虫惑场地追加攻击的特殊招式
  EXCLUDED_MOVES = [
    :STRUGGLE,
    :BIDE,
    :FUTURESIGHT,
    :DOOMDESIRE
  ]
end

#===============================================================================
# 场地开始、持续与结束
#===============================================================================
class PokeBattle_Battle
  alias bug_lure_pbStartTerrain pbStartTerrain
  def pbStartTerrain(user,newTerrain,fixedDuration=true)
    oldTerrain = @field.terrain
    bug_lure_pbStartTerrain(user,newTerrain,fixedDuration)
    if oldTerrain!=newTerrain &&
       @field.terrain==PBBattleTerrains::BugLure
      pbDisplay(_INTL("无数虫群笼罩了场地！"))
    end
  end

  alias bug_lure_pbEORTerrain pbEORTerrain
  def pbEORTerrain
    wasBugLure = (@field.terrain==PBBattleTerrains::BugLure)
    ending = wasBugLure && @field.terrainDuration==1

    pbDisplay(_INTL("笼罩场地的虫群消散了！")) if ending
    bug_lure_pbEORTerrain

    if wasBugLure && !ending &&
       @field.terrain==PBBattleTerrains::BugLure
      pbDisplay(_INTL("虫群仍在场上飞舞！"))
    end
  end
end

#===============================================================================
# 虫属性招式威力提高30%
# 地面上的宝可梦使用虫属性攻击招式时攻击两次
# 第二次伤害为第一次的1/4
#===============================================================================
class PokeBattle_Move
  def bugLureTerrainActiveFor?(user)
    return false if !user
    return false if user.airborne?
    return false if @battle.field.terrain!=PBBattleTerrains::BugLure
    return true
  end

  def bugLureMoveExcluded?
    return true if chargingTurnMove?
    return true if callsAnotherMove?

    BugLureTerrainSettings::EXCLUDED_MOVES.each do |moveSymbol|
      moveID = getConst(PBMoves,moveSymbol)
      return true if moveID && @id==moveID
    end
    return false
  end

  alias bug_lure_pbNumHits pbNumHits
  def pbNumHits(user,targets)
    @bugLureDoubleHit = false
    normalHits = bug_lure_pbNumHits(user,targets)

    return normalHits if normalHits!=1
    return normalHits if !pbDamagingMove?
    return normalHits if targets.length==0
    return normalHits if !bugLureTerrainActiveFor?(user)
    return normalHits if bugLureMoveExcluded?

    moveType = @calcType
    moveType = pbBaseType(user) if moveType.nil?
    return normalHits if !isConst?(moveType,PBTypes,:BUG)

    # 借用亲子爱计数器，让引擎自动将第二击伤害变为1/4。
    user.effects[PBEffects::ParentalBond] = 3
    @bugLureDoubleHit = true
    return 2
  end

  # 第二击继续播放原招式动画，不播放亲子爱动画。
  alias bug_lure_pbShowAnimation pbShowAnimation
  def pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)
    if @bugLureDoubleHit &&
       user.effects[PBEffects::ParentalBond]==1
      @battle.pbAnimation(id,user,targets,hitNum) if showAnimation
      return
    end
    bug_lure_pbShowAnimation(id,user,targets,hitNum,showAnimation)
  end

  # 在最终伤害上增加30%，确保自定义伤害类同样能够生效。
  alias bug_lure_pbCalcDamage pbCalcDamage
  def pbCalcDamage(user,target,numTargets=1)
    bug_lure_pbCalcDamage(user,target,numTargets)
    return if statusMove?
    return if !bugLureTerrainActiveFor?(user)

    moveType = @calcType
    moveType = pbBaseType(user) if moveType.nil?
    return if !isConst?(moveType,PBTypes,:BUG)
    return if target.damageState.calcDamage<=0

    target.damageState.calcDamage =
      (target.damageState.calcDamage*1.3).round
  end
end

#===============================================================================
# 使用虫惑场地
#===============================================================================
class PokeBattle_Move_216 < PokeBattle_Move
  def pbMoveFailed?(user,targets)
    if @battle.field.terrain==PBBattleTerrains::BugLure
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbEffectGeneral(user)
    @battle.pbStartTerrain(user,PBBattleTerrains::BugLure)
  end
end

#===============================================================================
# 自然之力变为虫鸣
#===============================================================================
class PokeBattle_Move_0B3
  alias bug_lure_pbOnStartUse pbOnStartUse
  def pbOnStartUse(user,targets)
    bug_lure_pbOnStartUse(user,targets)
    if @battle.field.terrain==PBBattleTerrains::BugLure
      @npMove = getConst(PBMoves,:BUGBUZZ) || @npMove
    end
  end
end

#===============================================================================
# 秘密之力：50%几率降低目标攻击1级
#===============================================================================
class PokeBattle_Move_0A4
  alias bug_lure_secret_pbOnStartUse pbOnStartUse
  def pbOnStartUse(user,targets)
    bug_lure_secret_pbOnStartUse(user,targets)
    @bugLureSecretPower = false

    if @battle.field.terrain==PBBattleTerrains::BugLure
      # 使用原版“降低攻击”效果编号
      @secretPower = 5
      @bugLureSecretPower = true
    end
  end

  alias bug_lure_pbAdditionalEffectChance pbAdditionalEffectChance
  def pbAdditionalEffectChance(user,target)
    return 50 if @bugLureSecretPower
    return bug_lure_pbAdditionalEffectChance(user,target)
  end

  alias bug_lure_secret_pbShowAnimation pbShowAnimation
  def pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)
    if @bugLureSecretPower
      anim = getConst(PBMoves,:BUGBUZZ) || id
      @battle.pbAnimation(anim,user,targets,hitNum) if showAnimation
      return
    end
    bug_lure_secret_pbShowAnimation(
      id,user,targets,hitNum,showAnimation
    )
  end
end

#===============================================================================
# 保护色变为虫属性
#===============================================================================
class PokeBattle_Move_060
  alias bug_lure_camouflage_pbMoveFailed pbMoveFailed?
  def pbMoveFailed?(user,targets)
    if @battle.field.terrain==PBBattleTerrains::BugLure
      if !user.canChangeType?
        @battle.pbDisplay(_INTL("但是失败了！"))
        return true
      end

      @newType = getID(PBTypes,:BUG)
      if !user.pbHasOtherType?(@newType)
        @battle.pbDisplay(_INTL("但是失败了！"))
        return true
      end
      return false
    end

    return bug_lure_camouflage_pbMoveFailed(user,targets)
  end
end

#===============================================================================
# 拟态变为虫属性
#===============================================================================
class PokeBattle_Battler
  alias bug_lure_pbCheckFormOnTerrainChange pbCheckFormOnTerrainChange
  def pbCheckFormOnTerrainChange
    if @battle.field.terrain==PBBattleTerrains::BugLure &&
       hasActiveAbility?(:MIMICRY)
      return if fainted?

      newTypes = [getID(PBTypes,:BUG)]
      if pbTypes!=newTypes
        pbChangeTypes(newTypes)
        @battle.pbShowAbilitySplash(self,true)
        @battle.pbHideAbilitySplash(self)
        @battle.pbDisplay(_INTL("{1}变成了虫属性！",pbThis))
      end
      return
    end

    bug_lure_pbCheckFormOnTerrainChange
  end
end

#===============================================================================
# 大地波动变为虫属性
# 原技能类已经会在任意场地中将威力提高100%
#===============================================================================
class PokeBattle_Move_18A
  alias bug_lure_terrain_pulse_pbBaseType pbBaseType
  def pbBaseType(user)
    if @battle.field.terrain==PBBattleTerrains::BugLure &&
       !user.airborne?
      return getID(PBTypes,:BUG)
    end
    return bug_lure_terrain_pulse_pbBaseType(user)
  end
end


#===============================================================================
# 冰冷场地
#===============================================================================

module PBBattleTerrains
  Cold = 6
end

#===============================================================================
# 场地开始、回合伤害、持续与结束
#===============================================================================
class PokeBattle_Battle
  alias cold_terrain_pbStartTerrain pbStartTerrain
  def pbStartTerrain(user,newTerrain,fixedDuration=true)
    oldTerrain = @field.terrain
    cold_terrain_pbStartTerrain(user,newTerrain,fixedDuration)

    if oldTerrain!=newTerrain &&
       @field.terrain==PBBattleTerrains::Cold
      pbDisplay(_INTL("刺骨的寒气笼罩了场地！"))
    end
  end

  alias cold_terrain_pbEORTerrain pbEORTerrain
  def pbEORTerrain
    wasCold = (@field.terrain==PBBattleTerrains::Cold)

    # 冰属性以外、位于地面上的宝可梦受到伤害
    if wasCold
      eachBattler do |b|
        next if b.fainted?
        next if b.airborne?
        next if b.pbHasType?(:ICE)
        next if !b.takesIndirectDamage?

        pbDisplay(_INTL("{1}受到了刺骨寒气的伤害！",b.pbThis))
        @scene.pbDamageAnimation(b)

        damage = b.totalhp/16
        damage = 1 if damage<1
        b.pbReduceHP(damage,false)
        b.pbItemHPHealCheck
        b.pbFaint if b.fainted?
      end
    end

    ending = wasCold && @field.terrainDuration==1
    pbDisplay(_INTL("场上的刺骨寒气消散了！")) if ending

    cold_terrain_pbEORTerrain

    if wasCold && !ending &&
       @field.terrain==PBBattleTerrains::Cold
      pbDisplay(_INTL("刺骨的寒气仍笼罩着场地！"))
    end
  end
end

#===============================================================================
# 冰属性招式威力提高30%
# 格斗属性招式威力减半
#===============================================================================
class PokeBattle_Move
  alias cold_terrain_pbCalcDamage pbCalcDamage
  def pbCalcDamage(user,target,numTargets=1)
    cold_terrain_pbCalcDamage(user,target,numTargets)
    return if statusMove?
    return if @battle.field.terrain!=PBBattleTerrains::Cold
    return if target.damageState.calcDamage<=0

    moveType = @calcType
    moveType = pbBaseType(user) if moveType.nil?

    # 只有站在地面上的使用者获得冰属性增幅
    if !user.airborne? && isConst?(moveType,PBTypes,:ICE)
      target.damageState.calcDamage =
        (target.damageState.calcDamage*1.3).round
    end

    # 格斗属性招式不论使用者是否浮空，威力都会减半
    if isConst?(moveType,PBTypes,:FIGHTING)
      target.damageState.calcDamage =
        (target.damageState.calcDamage*0.5).round
      target.damageState.calcDamage = 1 if
        target.damageState.calcDamage<1
    end
  end
end

#===============================================================================
# 冰冷场地技能效果
#===============================================================================
class PokeBattle_Move_217 < PokeBattle_Move
  def pbMoveFailed?(user,targets)
    if @battle.field.terrain==PBBattleTerrains::Cold
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbEffectGeneral(user)
    @battle.pbStartTerrain(user,PBBattleTerrains::Cold)
  end
end

#===============================================================================
# 自然之力变为冰冻光束
#===============================================================================
class PokeBattle_Move_0B3
  alias cold_terrain_nature_power_pbOnStartUse pbOnStartUse
  def pbOnStartUse(user,targets)
    cold_terrain_nature_power_pbOnStartUse(user,targets)

    if @battle.field.terrain==PBBattleTerrains::Cold
      @npMove = getConst(PBMoves,:ICEBEAM) || @npMove
    end
  end
end

#===============================================================================
# 秘密之力：15%几率使目标冰冻
#===============================================================================
class PokeBattle_Move_0A4
  alias cold_terrain_secret_power_pbOnStartUse pbOnStartUse
  def pbOnStartUse(user,targets)
    cold_terrain_secret_power_pbOnStartUse(user,targets)
    @coldTerrainSecretPower = false

    if @battle.field.terrain==PBBattleTerrains::Cold
      # 原版秘密之力中的冰冻效果编号
      @secretPower = 9
      @coldTerrainSecretPower = true
    end
  end

  alias cold_terrain_secret_chance pbAdditionalEffectChance
  def pbAdditionalEffectChance(user,target)
    return 15 if @coldTerrainSecretPower
    return cold_terrain_secret_chance(user,target)
  end

  alias cold_terrain_secret_animation pbShowAnimation
  def pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)
    if @coldTerrainSecretPower
      anim = getConst(PBMoves,:ICEBEAM) || id
      @battle.pbAnimation(anim,user,targets,hitNum) if showAnimation
      return
    end

    cold_terrain_secret_animation(
      id,user,targets,hitNum,showAnimation
    )
  end
end

#===============================================================================
# 保护色变为冰属性
#===============================================================================
class PokeBattle_Move_060
  alias cold_terrain_camouflage_pbMoveFailed pbMoveFailed?
  def pbMoveFailed?(user,targets)
    if @battle.field.terrain==PBBattleTerrains::Cold
      if !user.canChangeType?
        @battle.pbDisplay(_INTL("但是失败了！"))
        return true
      end

      @newType = getID(PBTypes,:ICE)
      if !user.pbHasOtherType?(@newType)
        @battle.pbDisplay(_INTL("但是失败了！"))
        return true
      end
      return false
    end

    return cold_terrain_camouflage_pbMoveFailed(user,targets)
  end
end

#===============================================================================
# 拟态变为冰属性
#===============================================================================
class PokeBattle_Battler
  alias cold_terrain_pbCheckFormOnTerrainChange pbCheckFormOnTerrainChange
  def pbCheckFormOnTerrainChange
    if @battle.field.terrain==PBBattleTerrains::Cold &&
       hasActiveAbility?(:MIMICRY)
      return if fainted?

      newTypes = [getID(PBTypes,:ICE)]
      if pbTypes!=newTypes
        pbChangeTypes(newTypes)
        @battle.pbShowAbilitySplash(self,true)
        @battle.pbHideAbilitySplash(self)
        @battle.pbDisplay(_INTL("{1}变成了冰属性！",pbThis))
      end
      return
    end

    cold_terrain_pbCheckFormOnTerrainChange
  end
end

#===============================================================================
# 大地波动变为冰属性
# 原18A效果会在场地中自动令威力提高100%
#===============================================================================
class PokeBattle_Move_18A
  alias cold_terrain_pulse_pbBaseType pbBaseType
  def pbBaseType(user)
    if @battle.field.terrain==PBBattleTerrains::Cold &&
       !user.airborne?
      return getID(PBTypes,:ICE)
    end

    return cold_terrain_pulse_pbBaseType(user)
  end
end


#===============================================================================
# 结冰武器、捕获网
#===============================================================================

module PBEffects
  CaptureNet     = 304
  CaptureNetUser = 305
end

#===============================================================================
# 初始化捕获网状态
#===============================================================================
class PokeBattle_Battler
  alias capture_net_pbInitEffects pbInitEffects
  def pbInitEffects(batonPass)
    capture_net_pbInitEffects(batonPass)
    @effects[PBEffects::CaptureNet]     = false
    @effects[PBEffects::CaptureNetUser] = -1
  end
end

#===============================================================================
# 结冰武器
# 攻击提高1级；冰冷场地中提高2级
#===============================================================================
class PokeBattle_Move_218 < PokeBattle_StatUpMove
  def initialize(battle,move)
    super
    @statUp = [PBStats::ATTACK,1]
  end

  def pbOnStartUse(user,targets)
    increment = 1
    if @battle.field.terrain==PBBattleTerrains::Cold
      increment = 2
    end
    @statUp[1] = increment
  end
end

#===============================================================================
# 捕获网
# 令目标无法交换或逃走
# 如果命中时目标正受到虫惑场地影响，则每回合降低速度
#===============================================================================
class PokeBattle_Move_219 < PokeBattle_Move
  def pbFailsAgainstTarget?(user,target)
    if target.effects[PBEffects::MeanLook]>=0
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end

    if NEWEST_BATTLE_MECHANICS && target.pbHasType?(:GHOST)
      @battle.pbDisplay(
        _INTL("这不能影响{1}……",target.pbThis(true))
      )
      return true
    end

    return false
  end

  def pbEffectAgainstTarget(user,target)
    # 使用黑色目光的原版效果阻止目标交换和逃走
    target.effects[PBEffects::MeanLook] = user.index

    # 只有命中时位于地面且虫惑场地生效，才附加持续降速
    if @battle.field.terrain==PBBattleTerrains::BugLure &&
       target.affectedByTerrain?
      target.effects[PBEffects::CaptureNet] = true
      target.effects[PBEffects::CaptureNetUser] = user.index
      @battle.pbDisplay(
        _INTL("{1}被虫惑黏网牢牢缠住了！",target.pbThis)
      )
    else
      target.effects[PBEffects::CaptureNet] = false
      target.effects[PBEffects::CaptureNetUser] = -1
      @battle.pbDisplay(_INTL("{1}不能逃脱！",target.pbThis))
    end
  end
end

#===============================================================================
# 捕获网的回合结束降速
#===============================================================================
class PokeBattle_Battle
  alias capture_net_pbEndOfRoundPhase pbEndOfRoundPhase
  def pbEndOfRoundPhase
    capture_net_pbEndOfRoundPhase
    return if @decision!=0

    pbPriority(true).each do |b|
      next if !b || b.fainted?
      next if !b.effects[PBEffects::CaptureNet]
      next if b.effects[PBEffects::MeanLook]<0

      userIndex = b.effects[PBEffects::CaptureNetUser]
      netUser = @battlers[userIndex]

      # 捕获网的使用者离场后，束缚及降速效果结束
      if !netUser || netUser.fainted? ||
         b.effects[PBEffects::MeanLook]!=userIndex
        b.effects[PBEffects::CaptureNet] = false
        b.effects[PBEffects::CaptureNetUser] = -1
        next
      end

      if b.pbCanLowerStatStage?(PBStats::SPEED,netUser)
        b.pbLowerStatStage(
          PBStats::SPEED,1,netUser,true,false,true
        )
      end
    end
  end
end


#===============================================================================
# 虫蚀种子、结冻种子
#===============================================================================

#===============================================================================
# 道具效果
#===============================================================================

# 虫蚀种子：虫惑场地中攻击提高1级
BattleHandlers::TerrainStatBoostItem.add(:BUGLURESEED,
  proc { |item,battler,battle|
    next false if battle.field.terrain!=PBBattleTerrains::BugLure
    next false if !battler.pbCanRaiseStatStage?(
      PBStats::ATTACK,battler
    )

    itemName = PBItems.getName(item)
    battle.pbCommonAnimation("UseItem",battler)

    next battler.pbRaiseStatStageByCause(
      PBStats::ATTACK,1,battler,itemName
    )
  }
)

# 结冻种子：冰冷场地中防御提高1级
BattleHandlers::TerrainStatBoostItem.add(:COLDSEED,
  proc { |item,battler,battle|
    next false if battle.field.terrain!=PBBattleTerrains::Cold
    next false if !battler.pbCanRaiseStatStage?(
      PBStats::DEFENSE,battler
    )

    itemName = PBItems.getName(item)
    battle.pbCommonAnimation("UseItem",battler)

    next battler.pbRaiseStatStageByCause(
      PBStats::DEFENSE,1,battler,itemName
    )
  }
)

#===============================================================================
# 场地生成时，令所有种子先发动，再处理共生
#===============================================================================
class PokeBattle_Battle
  def customTerrainSeedBatch?
    return @customTerrainSeedBatch==true
  end

  def pbQueueCustomTerrainSeedSymbiosis(battler)
    @customTerrainSeedSymbiosisQueue ||= []
    if !@customTerrainSeedSymbiosisQueue.include?(battler)
      @customTerrainSeedSymbiosisQueue.push(battler)
    end
  end

  alias custom_seed_order_pbStartTerrain pbStartTerrain
  def pbStartTerrain(user,newTerrain,fixedDuration=true)
    batchSeeds = (
      @field.terrain!=newTerrain &&
      [PBBattleTerrains::BugLure,
       PBBattleTerrains::Cold].include?(newTerrain)
    )

    if !batchSeeds
      return custom_seed_order_pbStartTerrain(
        user,newTerrain,fixedDuration
      )
    end

    @customTerrainSeedBatch = true
    @customTerrainSeedSymbiosisQueue = []

    result = custom_seed_order_pbStartTerrain(
      user,newTerrain,fixedDuration
    )

    @customTerrainSeedBatch = false
    queue = @customTerrainSeedSymbiosisQueue
    @customTerrainSeedSymbiosisQueue = []

    # 所有场地种子检查完成后，再依次触发共生
    queue.each do |battler|
      next if !battler || battler.fainted?
      battler.pbSymbiosis
    end

    return result
  end
end

class PokeBattle_Battler
  alias custom_seed_order_pbConsumeItem pbConsumeItem
  def pbConsumeItem(recoverable=true,symbiosis=true,belch=true)
    customSeed = (
      isConst?(@item,PBItems,:BUGLURESEED) ||
      isConst?(@item,PBItems,:COLDSEED)
    )

    if customSeed && symbiosis &&
       @battle.customTerrainSeedBatch?
      @battle.pbQueueCustomTerrainSeedSymbiosis(self)

      # 暂时不触发共生，等其他种子全部发动
      return custom_seed_order_pbConsumeItem(
        recoverable,false,belch
      )
    end

    return custom_seed_order_pbConsumeItem(
      recoverable,symbiosis,belch
    )
  end
end
#===============================================================================
# 结冻盔甲
# 冰冷场地中，物理防御提高50%
#===============================================================================
BattleHandlers::DamageCalcTargetAbility.add(:FROZENARMOR,
  proc { |ability,user,target,move,mults,baseDmg,type|
    next if !move.physicalMove?
    next if target.battle.field.terrain!=PBBattleTerrains::Cold

    mults[DEF_MULT] *= 1.5
  }
)

#===============================================================================
# 陷阱诡计
# 受到虫惑场地影响时，变化招式优先度+1
#===============================================================================
BattleHandlers::PriorityChangeAbility.add(:TRAPTRICK,
  proc { |ability,battler,move,priority|
    next if !move.statusMove?
    next if battler.battle.field.terrain!=PBBattleTerrains::BugLure
    next if !battler.affectedByTerrain?

    next priority+1
  }
)


#===============================================================================
# 虫惑制造者
# 出场时生成虫惑场地
#===============================================================================
BattleHandlers::AbilityOnSwitchIn.add(:BUGLURESURGE,
  proc { |ability,battler,battle|
    next if battle.field.terrain==PBBattleTerrains::BugLure

    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(
      battler,PBBattleTerrains::BugLure
    )
    # pbStartTerrain会自动关闭特性提示框
  }
)

#===============================================================================
# 冰冷制造者
# 出场时生成冰冷场地
#===============================================================================
BattleHandlers::AbilityOnSwitchIn.add(:COLDSURGE,
  proc { |ability,battler,battle|
    next if battle.field.terrain==PBBattleTerrains::Cold

    battle.pbShowAbilitySplash(battler)
    battle.pbStartTerrain(
      battler,PBBattleTerrains::Cold
    )
    # pbStartTerrain会自动关闭特性提示框
  }
)
