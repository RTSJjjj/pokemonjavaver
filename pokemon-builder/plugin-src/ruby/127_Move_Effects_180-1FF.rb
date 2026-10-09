#===============================================================================
# User is protected against damaging moves this round. Decreases the Defense of
# the user of a stopped contact move by 2 stages. (Obstruct)
#===============================================================================
class PokeBattle_Move_180 < PokeBattle_ProtectMove
  def initialize(battle,move)
    super
    @effect = PBEffects::Obstruct
  end
end



#===============================================================================
# Lowers target's Defense and Special Defense by 1 stage at the end of each
# turn. Prevents target from retreating. (Octolock)
#===============================================================================
class PokeBattle_Move_181 < PokeBattle_Move
  def pbFailsAgainstTarget?(user,target)
    if target.effects[PBEffects::OctolockUser]>=0 || (target.damageState.substitute && !ignoresSubstitute?(user))
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    if NEWEST_BATTLE_MECHANICS && target.pbHasType?(:GHOST)
      @battle.pbDisplay(_INTL("这不能影响{1}……",target.pbThis(true)))
      return true
    end
    return false
  end

  def pbEffectAgainstTarget(user,target)
    target.effects[PBEffects::OctolockUser] = user.index
    target.effects[PBEffects::Octolock] = true
    @battle.pbDisplay(_INTL("{1}不能逃脱！",target.pbThis))
  end
end



#===============================================================================
# Ignores move redirection from abilities and moves. (Snipe Shot)
#===============================================================================
class PokeBattle_Move_182 < PokeBattle_Move
end



#===============================================================================
# Consumes berry and raises the user's Defense by 2 stages. (Stuff Cheeks)
#===============================================================================
class PokeBattle_Move_183 < PokeBattle_Move
  def pbEffectGeneral(user)
    if !user.item || user.item==0 || !pbIsBerry?(user.item)
      @battle.pbDisplay("But it failed!")
      return -1
    end
    if user.pbCanRaiseStatStage?(PBStats::DEFENSE,user,self)
      user.pbRaiseStatStage(PBStats::DEFENSE,2,user)
    end
    user.pbHeldItemTriggerCheck(user.item,false)
    user.pbConsumeItem(true,true,false) if user.item>0
  end
end



#===============================================================================
# Forces all active Pokémon to consume their held berries. This move bypasses
# Substitutes. (Tea Time)
#===============================================================================
class PokeBattle_Move_184 < PokeBattle_Move
  def ignoresSubstitute?(user); return true; end

  def pbMoveFailed?(user,targets)
    @validTargets = []
    @battle.eachBattler do |b|
      next if !b.item == 0 || !pbIsBerry?(b.item)
      @validTargets.push(b.index)
    end
    if @validTargets.length==0
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    @battle.pbDisplay(_INTL("It's tea time! Everyone dug in to their Berries!"))
    return false
  end

  def pbFailsAgainstTarget?(user,target)
    return false if @validTargets.include?(target.index)
    return true if target.semiInvulnerable?
  end

  def pbEffectAgainstTarget(user,target)
    target.pbHeldItemTriggerCheck(target.item,false)
    target.pbConsumeItem(true,true,false) if pbIsBerry?(target.item)
  end
end



#===============================================================================
# Decreases Opponent's Defense by 1 stage. Does Double Damage under gravity
# (Grav Apple)
#===============================================================================
class PokeBattle_Move_185 < PokeBattle_TargetStatDownMove
  def initialize(battle,move)
    super
    @statDown = [PBStats::DEFENSE,1]
  end

  def pbBaseDamage(baseDmg,user,target)
    baseDmg=baseDmg*1.5 if @battle.field.effects[PBEffects::Gravity]>0
    return baseDmg
  end
end



#===============================================================================
# Decrease 1 stage of speed and weakens target to fire moves. (Tar Shot)
#===============================================================================
class PokeBattle_Move_186 < PokeBattle_Move
  def pbEffectAgainstTarget(user,target)
    if !target.pbCanLowerStatStage?(PBStats::SPEED,target,self) && !target.effects[PBEffects::TarShot]
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    if target.pbCanLowerStatStage?(PBStats::SPEED,target,self)
      target.pbLowerStatStage(PBStats::SPEED,1,target)
    end
    if target.effects[PBEffects::TarShot]==false
      target.effects[PBEffects::TarShot]=true
      @battle.pbDisplay(_INTL("{1} became weaker to fire!",target.pbThis))
    end
  end
end



#===============================================================================
# Changes Category based on Opponent's Def and SpDef. Has 20% Chance to Poison
# (Shell Side Arm)
#===============================================================================
class PokeBattle_Move_187 < PokeBattle_Move_005
  def initialize(battle,move)
    super
    @calcCategory = 1
  end

  def pbEffectAgainstTarget(user,target)
    if rand(5)<1 && target.pbCanPoison?(user,true,self)
      target.pbPoison(user)
    end
  end

  def physicalMove?(thisType=nil); return (@calcCategory==0); end
  def specialMove?(thisType=nil);  return (@calcCategory==1); end

  def pbOnStartUse(user,targets)
    stageMul = [2,2,2,2,2,2, 2, 3,4,5,6,7,8]
    stageDiv = [8,7,6,5,4,3, 2, 2,2,2,2,2,2]
    defense      = targets[0].defense
    defenseStage = targets[0].stages[PBStats::DEFENSE]+6
    realDefense  = (defense.to_f*stageMul[defenseStage]/stageDiv[defenseStage]).floor
    spdef        = targets[0].spdef
    spdefStage   = targets[0].stages[PBStats::SPDEF]+6
    realSpdef    = (spdef.to_f*stageMul[spdefStage]/stageDiv[spdefStage]).floor
    # Determine move's category
    return @calcCategory = 0 if realDefense<realSpdef
    return @calcCategory = 1 if realDefense>=realSpdef
    if isConst?(@id,PBMoves,:WONDERROOM); end
  end
end



#===============================================================================
# Hits 3 times and always critical. (Surging Strikes)
#===============================================================================
class PokeBattle_Move_188 < PokeBattle_Move_0A0
  def multiHitMove?;           return true; end
  def pbNumHits(user,targets); return 3;    end
end

#===============================================================================
# Restore HP and heals any status conditions of itself and its allies
# (Jungle Healing)
#===============================================================================
class PokeBattle_Move_189 < PokeBattle_Move
  def healingMove?; return true; end

  def pbMoveFailed?(user,targets)
    jglheal = 0
    for i in 0...targets.length
      jglheal += 1 if (targets[i].hp == targets[i].totalhp || !targets[i].canHeal?) && targets[i].status ==PBStatuses::NONE
    end
    if jglheal == targets.length
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbEffectAgainstTarget(user,target)
      target.pbCureStatus
    if target.hp != target.totalhp && target.canHeal?
      hpGain = (target.totalhp/4.0).round
      target.pbRecoverHP(hpGain)
      @battle.pbDisplay(_INTL("{1}'s health was restored.",target.pbThis))
    end
    super
  end
end



#===============================================================================
# Changes type and base power based on Battle Terrain (Terrain Pulse)
#===============================================================================
class PokeBattle_Move_18A < PokeBattle_Move
  def pbBaseDamage(baseDmg,user,target)
    baseDmg *= 2 if @battle.field.terrain != PBBattleTerrains::None && !user.airborne?
    return baseDmg
  end

  def pbBaseType(user)
    ret = getID(PBTypes,:NORMAL)
    if !user.airborne?
      case @battle.field.terrain
      when PBBattleTerrains::Electric
        ret = getConst(PBTypes,:ELECTRIC) || ret
      when PBBattleTerrains::Grassy
        ret = getConst(PBTypes,:GRASS) || ret
      when PBBattleTerrains::Misty
        ret = getConst(PBTypes,:FAIRY) || ret
      when PBBattleTerrains::Psychic
        ret = getConst(PBTypes,:PSYCHIC) || ret
      end
    end
    return ret
  end

  def pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)
    t = pbBaseType(user)
    hitNum = 1 if isConst?(t,PBTypes,:ELECTRIC)
    hitNum = 2 if isConst?(t,PBTypes,:GRASS)
    hitNum = 3 if isConst?(t,PBTypes,:FAIRY)
    hitNum = 4 if isConst?(t,PBTypes,:PSYCHIC)
    super
  end
end



#===============================================================================
# Burns opposing Pokemon that have increased their stats in that turn before the
# execution of this move (Burning Jealousy)
#===============================================================================
class PokeBattle_Move_18B < PokeBattle_Move
  def pbAdditionalEffect(user,target)
    return if target.damageState.substitute
    if target.pbCanBurn?(user,false,self) &&
       target.effects[PBEffects::BurningJealousy]
      target.pbBurn(user)
    end
  end
end



#===============================================================================
# Move has increased Priority in Grassy Terrain (Grassy Glide)
#===============================================================================
class PokeBattle_Move_18C < PokeBattle_Move
end



#===============================================================================
# Power Doubles onn Electric Terrain (Rising Voltage)
#===============================================================================
class PokeBattle_Move_18D < PokeBattle_Move
  def pbBaseDamage(baseDmg,user,target)
    baseDmg *= 2 if @battle.field.terrain==PBBattleTerrains::Electric &&
                    !target.airborne?
    return baseDmg
  end
end



#===============================================================================
# Boosts Targets' Attack and Defense (Coaching)
#===============================================================================
class PokeBattle_Move_18E < PokeBattle_TargetMultiStatUpMove
  def initialize(battle,move)
    super
    @statUp = [PBStats::ATTACK,1,PBStats::DEFENSE,1]
  end
end



#===============================================================================
# Renders item unusable (Corrosive Gas)
#===============================================================================
class PokeBattle_Move_18F < PokeBattle_Move
  def pbEffectAgainstTarget(user,target)
    return if @battle.wildBattle? && user.opposes?   # Wild Pokémon can't knock off
    return if user.fainted?
    return if target.damageState.substitute
    return if target.item==0 || target.unlosableItem?(target.item)
    return if target.hasActiveAbility?(:STICKYHOLD) && !@battle.moldBreaker
    itemName = target.itemName
    target.pbRemoveItem(false)
    @battle.pbDisplay(_INTL("{1}的{2}被拍落了！",target.pbThis,itemName))
  end
end



#===============================================================================
# Power is boosted on Psychic Terrain (Expanding Force)
#===============================================================================
class PokeBattle_Move_190 < PokeBattle_Move

  def pbTarget(user)
    if @battle.field.terrain == PBBattleTerrains::Psychic
      return PBTargets::AllNearFoes
    end
    return super
  end

  def pbBaseDamage(baseDmg,user,target)
    baseDmg *= 1.5 if @battle.field.terrain==PBBattleTerrains::Psychic
    return baseDmg
  end
end



#===============================================================================
# Boosts Sp Atk on 1st Turn and Attacks on 2nd (Meteor Beam)
#===============================================================================
class PokeBattle_Move_191 < PokeBattle_TwoTurnMove
  def pbChargingTurnMessage(user,targets)
    @battle.pbDisplay(_INTL("{1} is overflowing with space power!",user.pbThis))
  end

  def pbChargingTurnEffect(user,target)
    if user.pbCanRaiseStatStage?(PBStats::SPATK,user,self)
      user.pbRaiseStatStage(PBStats::SPATK,1,user)
    end
  end
end



#===============================================================================
# Fails if the Target has no Item (Poltergeist)
#===============================================================================
class PokeBattle_Move_192 < PokeBattle_Move
  def pbFailsAgainstTarget?(user,target)
    if target.item!=0
      @battle.pbDisplay(_INTL("{1} is about to be attacked by its {2}!",target.pbThis,target.itemName))
      return false
    end
    @battle.pbDisplay(_INTL("但是失败了！"))
    return true
  end
end



#===============================================================================
# Reduces Defense and Raises Speed after all hits (Scale Shot)
#===============================================================================
class PokeBattle_Move_193 < PokeBattle_Move_0C0
  def pbEffectAfterAllHits(user,target)
    if user.pbCanRaiseStatStage?(PBStats::SPEED,user,self)
      user.pbRaiseStatStage(PBStats::SPEED,1,user)
    end
    if user.pbCanLowerStatStage?(PBStats::DEFENSE,target)
      user.pbLowerStatStage(PBStats::DEFENSE,1,user)
    end
  end
end



#===============================================================================
# Double damage if stats were lowered that turn. (Lash Out)
#===============================================================================
class PokeBattle_Move_194 < PokeBattle_Move
  def pbBaseDamage(baseDmg,user,target)
    baseDmg *= 2 if user.effects[PBEffects::LashOut]
    return baseDmg
  end
end



#===============================================================================
# Removes all Terrain. Fails if there is no Terrain (Steel Roller)
#===============================================================================
class PokeBattle_Move_195 < PokeBattle_Move
  def pbMoveFailed?(user,targets)
    if @battle.field.terrain == PBBattleTerrains::None
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbEffectGeneral(user)
    case @battle.field.terrain
      when PBBattleTerrains::Electric
        @battle.pbDisplay(_INTL("场上的电流消失了！"))
      when PBBattleTerrains::Grassy
        @battle.pbDisplay(_INTL("四周的青草枯萎了！"))
      when PBBattleTerrains::Misty
        @battle.pbDisplay(_INTL("四周的薄雾消散了！"))
      when PBBattleTerrains::Psychic
        @battle.pbDisplay(_INTL("场地恢复原样了！"))
    end
    @battle.pbStartTerrain(user,PBBattleTerrains::None,true)
  end
end



#===============================================================================
# Self KO. Boosted Damage when on Misty Terrain (Misty Explosion)
#===============================================================================
class PokeBattle_Move_196 < PokeBattle_Move_0E0
  def pbBaseDamage(baseDmg,user,target)
    if @battle.field.terrain==PBBattleTerrains::Misty && !user.airborne?
      baseDmg = (baseDmg*1.5).round
    end
    return baseDmg
  end
end



#===============================================================================
# Target becomes Psychic type. (Magic Powder)
#===============================================================================
class PokeBattle_Move_197 < PokeBattle_Move
  def pbFailsAgainstTarget?(user,target)
    if !target.canChangeType? ||
       !target.pbHasOtherType?(getConst(PBTypes,:PSYCHIC))
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbEffectAgainstTarget(user,target)
    newType = getConst(PBTypes,:PSYCHIC)
    target.pbChangeTypes(newType)
    typeName = PBTypes.getName(newType)
    @battle.pbDisplay(_INTL("{1}变为了{2}属性！",target.pbThis,typeName))
  end
end

#===============================================================================
# Target's last move used loses 3 PP. (Eerie Spell - Galarian Slowking)
#===============================================================================
class PokeBattle_Move_198 < PokeBattle_Move
  def pbFailsAgainstTarget?(user,target)
    failed = true
    target.eachMove do |m|
      next if m.id!=target.lastRegularMoveUsed || m.pp==0 || m.totalpp<=0
      failed = false; break
    end
    if failed
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbEffectAgainstTarget(user,target)
    target.eachMove do |m|
      next if m.id!=target.lastRegularMoveUsed
      reduction = [3,m.pp].min
      target.pbSetPP(m,m.pp-reduction)
      @battle.pbDisplay(_INTL("{1}的{2}减少了{3}点PP！",
         target.pbThis(true),m.name,reduction))
      break
    end
  end
end

#蔷薇开华
# 如果场上没有反射壁和光墙：两个之间开随机一个
# 如果只有反射壁：开5回合光墙
# 如果只有光墙：开5回合反射壁
# 如果两个都有：没有附加效果
class PokeBattle_Move_19A < PokeBattle_Move
  
  def pbAdditionalEffect(user,target)
    effects = []
    if user.pbOwnSide.effects[PBEffects::Reflect] == 0
      effects.push([PBEffects::Reflect, "反射壁", "防御"])
    end
    if user.pbOwnSide.effects[PBEffects::LightScreen] == 0
      effects.push([PBEffects::LightScreen, "光墙", "特防"])
    end
    return if effects.empty?
    effect = effects.sample
    turn = user.hasActiveItem?(:LIGHTCLAY) ? 8 : 5
    user.pbOwnSide.effects[effect[0]] = turn
    @battle.pbDisplay(_INTL("{1}提高了{2}的{3}！",effect[1],user.pbTeam(true),effect[2]))
  end
end


#嗟怨震天
class PokeBattle_Move_19B < PokeBattle_Move
  #基础威力80，物攻每提高1级增加20威力，物攻等级小于0时不会降低威力
  def pbBaseDamage(baseDmg,user,target)
    mult = 0
    mult += user.stages[PBStats::ATTACK] if user.stages[PBStats::ATTACK]>0
    return 80 + 20*mult
  end
end

#珠泪哀歌
class PokeBattle_Move_19C < PokeBattle_Move
  
  #无视替身
  def ignoresSubstitute?(user); return true; end

  def pbEffectAgainstTarget(user,target)
    return if target.fainted? #如果已击杀对方
    cursed = target.effects[PBEffects::Curse] || target.effects[PBEffects::Tearalament]
    if !cursed    # 未被诅咒或珠泪哀歌
      target.effects[PBEffects::Tearalament] = true
      @battle.pbDisplay(_INTL("{1}陷入了恐惧与哀伤！",target.pbThis))
    end
  end
end


#水蒸气
class PokeBattle_Move_19D < PokeBattle_Move
  def pbBaseDamage(baseDmg, user, target)
    baseDmg *= 3 if @battle.pbWeather==PBWeather::Sun || @battle.pbWeather==PBWeather::HarshSun
    return baseDmg
  end
end
# 火焰守护
class PokeBattle_Move_1CC < PokeBattle_ProtectMove
  def initialize(battle, move)
    super
    @effect = PBEffects::BurningBulwark
  end
end
#===============================================================================
# Deals double damage to Dynamax POkémons. Dynamax is not implemented though.
# (Behemoth Blade, Behemoth Bash, Dynamax Cannon)
#===============================================================================
class PokeBattle_Move_199 < PokeBattle_Move
  # DYNAMAX IS NOT IMPLEMENTED.
end

# 古龙雷枪
class PokeBattle_Move_19E < PokeBattle_Move

  #如果自身处于电气场地时则威力翻倍
  def pbBaseDamage(baseDmg,user,target)
    baseDmg *= 2 if @battle.field.terrain==PBBattleTerrains::Electric &&
                    !user.airborne?
    return baseDmg
  end
  
  
end

# 咒钉
class PokeBattle_Move_19F < PokeBattle_Move
  
  # 处于咒钉状态
  # 有幽灵属性
  # 除了幽灵没有其他属性
  # 则失败
  def pbFailsAgainstTarget?(user, target)
    if target.effects[PBEffects::CurseNail] > 0 ||
       target.pbHasType?(:GHOST) ||
       !target.pbHasOtherType?(getConst(PBTypes, :GHOST))
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbEffectAgainstTarget(user, target)
    return if target.fainted?       # 如果对方已倒下直接结束
    # 修改对方为咒钉状态
    target.effects[PBEffects::CurseNail] = 3
    @battle.pbDisplay(_INTL("{1}被钉下了3回合的咒钉！",target.pbThis))
    if !target.effects[PBEffects::Curse]
      target.effects[PBEffects::Curse] = true
      @battle.pbDisplay(_INTL("{1}被诅咒了！",target.pbThis))
    end
  end
end


# 强袭炸裂
class PokeBattle_Move_1A0 < PokeBattle_Move
  def pbEffectAfterAllHits(user,target)
    return if !target.damageState.fainted
    if user.pbCanRaiseStatStage?(PBStats::ATTACK,user,self)
      user.pbRaiseStatStage(PBStats::ATTACK,1,user)
    end
    if user.pbCanRaiseStatStage?(PBStats::SPEED,user,self)
      user.pbRaiseStatStage(PBStats::SPEED,1,user)
    end
  end
end

class PokeBattle_Move_1A1 < PokeBattle_Move
  def pbCritialOverride(user,target); return 1; end
  def nonLethal?(user,target); return true; end
end
# NOTE: If you're inventing new move effects, use function code 199 and onwards.
#       Actually, you might as well use high numbers like 500+ (up to FFFF),
#       just to make sure later additions to Essentials don't clash with your
#       new effects.

# 大剑旋钻
class PokeBattle_Move_1A2 < PokeBattle_Move
  def pbBaseDamageMultiplier(damageMult,user,target)
    damageMult *= 2 if user.effects[PBEffects::UsedRapidSpin]
    return damageMult
  end
end

# 蛇咬
class PokeBattle_Move_1A3 < PokeBattle_Move
  def multiHitMove?; return true; end

  def pbNumHits(user,targets)
    return 5 if user.effects[PBEffects::UsedCoil]
    hitChances = [2,2,3,3,4,5]
    r = @battle.pbRandom(hitChances.length)
    r = hitChances.length-1 if user.hasActiveAbility?(:SKILLLINK)
    return hitChances[r]
  end
  
  def pbAdditionalEffect(user,target)
    return if target.damageState.substitute
    case @battle.pbRandom(2)
    when 0; target.pbConfuse if target.pbCanConfuse?(user,false,self)
    when 1; target.pbParalyze(user) if target.pbCanParalyze?(user,false,self)
    end
  end
end

# 手术
class PokeBattle_Move_1A4 < PokeBattle_HealingMove
  
  # 满血也能用
  def pbMoveFailed?(user,targets)
    return false
  end
  def pbFailsAgainstTarget?(user,target)
    if target.status==PBStatuses::NONE ||
       target.effects[PBEffects::Operation] ||
       target.pokemon.battleRank > 2
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbFailsAgainstTarget?(user,target)
    if target.status==PBStatuses::NONE
      @battle.pbDisplay(_INTL("但是失败了！")) || target.effects[PBEffects::Operation]
      return true
    end
    return false
  end

  def pbHealAmount(user)
    return (user.totalhp/4.0).round
  end

  def pbEffectGeneral(user);  end
  def pbEffectAgainstTarget(user,target)
    target.effects[PBEffects::Operation] = true
    target.pbCureStatus
    target.pbReduceHP(target.hp-1)
    amt = pbHealAmount(user)
    # 满血不治疗自己
    return if user.hp==user.totalhp
    user.pbRecoverHP(amt)
    @battle.pbDisplay(_INTL("{1}的HP回复了。",user.pbThis))
  end
end
#圣殿光辉
class PokeBattle_Move_1B1 < PokeBattle_TargetStatDownMove
  
  def initialize(battle,move)
    super
    @statDown = [PBStats::ATTACK,1]
  end
  
  def pbMoveFailed?(user,targets)
if !(user.isSpecies?(:SUGARDEVOIR) && user.form == 1)
      @battle.pbDisplay(_INTL("但是{1}无法使用这个招式。",user.pbThis(true)))
      return true
    end
    return false
  end
  
end

#深渊暗影
class PokeBattle_Move_1B2 < PokeBattle_TargetStatDownMove
  
  def initialize(battle,move)
    super
    @statDown = [PBStats::SPATK,1]
  end
  
  def pbMoveFailed?(user,targets)
    if !user.isSpecies?(:SUJINRAKU)
      @battle.pbDisplay(_INTL("但是{1}无法使用这个招式。",user.pbThis(true)))
      return true
    end
    return false
  end
  
end

# 断刃鏖杀
class PokeBattle_Move_1B3 < PokeBattle_Move

  def pbEffectAgainstTarget(user,target)
    return if target.fainted? || target.damageState.substitute
    # 无法替换
    if target.effects[PBEffects::FierceKilling] == -1
      target.effects[PBEffects::FierceKilling] = user.index
      @battle.pbDisplay(_INTL("{1}无法逃脱了！",target.pbThis))
    end
    # 无法使用招式和道具
    return if @battle.pbRandom(100) >= 15
    return if target.effects[PBEffects::FierceKilling2]
    target.effects[PBEffects::FierceKilling2] = true
    @battle.pbDisplay(_INTL("{1}无法行动了！",target.pbThis))
  end
end
# 高出力吐息
class PokeBattle_Move_1B4 < PokeBattle_Move
  def pbAdditionalEffect(user,target)
    return if target.damageState.substitute
    case @battle.pbRandom(3)
    when 0; target.pbPoison(user) if target.pbCanPoison?(user,false,self)
    when 1; target.pbParalyze(user) if target.pbCanParalyze?(user,false,self)
    when 2; target.pbBurn(user) if target.pbCanBurn?(user,false,self)
    end
  end
end

#回响之音
class PokeBattle_Move_1A5 < PokeBattle_Move
  
  #无视替身
  def ignoresSubstitute?(user); return true; end

  def pbEffectAgainstTarget(user,target)
    return if target.fainted? #如果已击杀对方
    ghostType = getConst(PBTypes,:GHOST)
    isGhost = target.pbHasType?(:GHOST)
    cursed = target.effects[PBEffects::Curse]
    if !isGhost && !cursed  # 没有幽灵属性且未被诅咒
      #追加对方幽灵属性
      target.effects[PBEffects::Type3] = ghostType
      typeName = PBTypes.getName(ghostType)
      #修改对方为诅咒状态
      target.effects[PBEffects::Curse] = true
      @battle.pbDisplay(_INTL("{1}被诅咒了，\n并且被追加了{2}属性！",target.pbThis,typeName))
    elsif !isGhost	# 没有幽灵属性
      target.effects[PBEffects::Type3] = ghostType
      typeName = PBTypes.getName(ghostType)
      @battle.pbDisplay(_INTL("{1}被追加了{2}属性！",target.pbThis,typeName))
    elsif !cursed	# 未被诅咒
      target.effects[PBEffects::Curse] = true
      @battle.pbDisplay(_INTL("{1}被诅咒了！",target.pbThis))
    end
  end
end

#悠然龙吟
# 如果场上没有反射壁和光墙：没有附加效果
# 如果只有反射壁：固定延长反射壁的回合数
# 如果只有光墙：固定延长光墙的回合数
# 如果两个都有：两个之间随机一个延长回合数
class PokeBattle_Move_1A6 < PokeBattle_Move
  
  def pbEffectAgainstTarget(user,target)
    effects = []
    if user.pbOwnSide.effects[PBEffects::Reflect] > 0
      effects.push([PBEffects::Reflect, "反射壁"])
    end
    if user.pbOwnSide.effects[PBEffects::LightScreen] > 0
      effects.push([PBEffects::LightScreen, "光墙"])
    end
    return if effects.empty?
    effect = effects.sample
    user.pbOwnSide.effects[effect[0]] += 1
    @battle.pbDisplay(_INTL("{1}延长了{2}\n{3}的回合数！",
                            @name, user.pbTeam(true), effect[1]))
  end
end

#千里击涛
class PokeBattle_Move_1A7 < PokeBattle_Move
  #额外克制龙属性
  def pbCalcTypeModSingle(moveType,defType,user,target)
    ret = super
    if isConst?(defType,PBTypes,:DRAGON)
      ret = PBTypeEffectiveness::SUPER_EFFECTIVE_ONE
    end
    return ret
  end
end

#百花绽放
class PokeBattle_Move_1A8 < PokeBattle_Move
  
  def pbCalcTypeModSingle(moveType,defType,user,target)
    ret = super
#影辞效果:同时带有草属性和龙属性
#本地效果:额外克制龙属性
    if isConst?(defType,PBTypes,:DRAGON)
      ret = PBTypeEffectiveness::SUPER_EFFECTIVE_ONE
    end
    return ret
  end
  
  def pbBaseDamage(baseDmg,user,target)
    return [150*user.hp/user.totalhp,1].max
  end
end

#进化光线
class PokeBattle_Move_1CD < PokeBattle_Move
  
  def pbBaseDamage(baseDmg, user, target)
    baseDmg *= 2 if pbGetEvolvedFormData(target.species).length <= 0
    return baseDmg
  end
end

#神威日冕
class PokeBattle_Move_1AA < PokeBattle_Move
  def pbOnStartUse(user, targets)
    return if !user.isSpecies?(:SOLGALEO)
    return if user.form == 1
    user.pbChangeFormTransform(1, _INTL("{1}在太阳的光芒中解放了真正的力量！",user.pbThis))
  end

  def pbCalcTypeModSingle(moveType,defType,user,target)
    ret = super
    if isConst?(defType,PBTypes,:DARK)  ||
       isConst?(defType,PBTypes,:GHOST) ||
       isConst?(defType,PBTypes,:DRAGON)
      ret = PBTypeEffectiveness::SUPER_EFFECTIVE_ONE
    end
    return ret
  end
  def pbEffectGeneral(user)
    user.effects[PBEffects::HyperBeam] = 2
    user.currentMove = @id
  end
end

#永夜灵霄
class PokeBattle_Move_1AB < PokeBattle_Move
  def pbOnStartUse(user, targets)
    return if !user.isSpecies?(:LUNALA)
    return if user.form == 1
    user.pbChangeFormTransform(1, _INTL("{1}在月亮的光芒中解放了真正的力量！",user.pbThis))
  end

  def pbCalcTypeModSingle(moveType,defType,user,target)
    ret = super
    if isConst?(defType,PBTypes,:PSYCHIC) ||
       isConst?(defType,PBTypes,:FIGHTING) ||
       isConst?(defType,PBTypes,:FAIRY)
      ret = PBTypeEffectiveness::SUPER_EFFECTIVE_ONE
    end
    return ret
  end
    def pbEffectGeneral(user)
    user.effects[PBEffects::HyperBeam] = 2
    user.currentMove = @id
  end
end

class PokeBattle_Move_1AC < PokeBattle_Move
  def pbEffectAgainstTarget(user,target)
    user.eachOpposing do |b|
      b.eachMoveWithIndex do |m,i|
        next if m.id!=b.lastRegularMoveUsed || m.pp==0 || m.totalpp<=0
        reduction = [2,m.pp].min
        b.pbSetPP(m,m.pp-reduction)
        @battle.pbDisplay(_INTL("{1}的PP降低了!",b.pbThis))
        break
      end
    end
  end
end
#===============================================================================
class PokeBattle_Move_1A9 < PokeBattle_Move
  def pbBaseDamage(baseDmg,user,target)
    baseDmg *= 1.5 if @battle.field.terrain==PBBattleTerrains::Electric &&
                    !user.airborne?
    return baseDmg
  end
end

# 晶光转转
class PokeBattle_Move_1AD < PokeBattle_Move
  
  def initialize(battle,move)
    super
    @toxic = false
  end

  def pbAdditionalEffect(user,target)
    return if target.damageState.substitute
    target.pbPoison(user,nil,@toxic) if target.pbCanPoison?(user,false,self)
  end
  
  def pbEffectAfterAllHits(user,target)
    return if user.fainted? || target.damageState.unaffected
    if user.effects[PBEffects::Trapping]>0
      trapMove = PBMoves.getName(user.effects[PBEffects::TrappingMove])
      trapUser = @battle.battlers[user.effects[PBEffects::TrappingUser]]
      @battle.pbDisplay(_INTL("{1}摆脱了{2}的{3}！",user.pbThis,trapUser.pbThis(true),trapMove))
      user.effects[PBEffects::Trapping]     = 0
      user.effects[PBEffects::TrappingMove] = 0
      user.effects[PBEffects::TrappingUser] = -1
    end
    if user.effects[PBEffects::LeechSeed]>=0
      user.effects[PBEffects::LeechSeed] = -1
      @battle.pbDisplay(_INTL("{1}清除了寄生种子！",user.pbThis))
    end
    if user.pbOwnSide.effects[PBEffects::StealthRock]
      user.pbOwnSide.effects[PBEffects::StealthRock] = false
      @battle.pbDisplay(_INTL("{1}吹散了隐形岩！",user.pbThis))
    end
    if user.pbOwnSide.effects[PBEffects::Spikes]>0
      user.pbOwnSide.effects[PBEffects::Spikes] = 0
      @battle.pbDisplay(_INTL("{1}吹走了铁菱！",user.pbThis))
    end
    if user.pbOwnSide.effects[PBEffects::ToxicSpikes]>0
      user.pbOwnSide.effects[PBEffects::ToxicSpikes] = 0
      @battle.pbDisplay(_INTL("{1}吹走了毒菱！",user.pbThis))
    end
    if user.pbOwnSide.effects[PBEffects::StickyWeb]
      user.pbOwnSide.effects[PBEffects::StickyWeb] = false
      user.pbOwnSide.effects[PBEffects::StickyWebUser] = -1
      @battle.pbDisplay(_INTL("{1}吹走了粘网！",user.pbThis))
    end
  end
end

# 雪景
class PokeBattle_Move_1C0 < PokeBattle_WeatherMove
  def initialize(battle,move)
    super
    @weatherType = PBWeather::Snow
  end
end

# 巨剑突击
class PokeBattle_Move_1C2 < PokeBattle_Move
  def pbEffectWhenDealingDamage(user, target)
    user.effects[PBEffects::GlaiveRush] = 2
  end
end

# 糖浆炸弹
class PokeBattle_Move_1C3 < PokeBattle_Move
  def pbEffectAgainstTarget(user, target)
    return if target.fainted? || target.damageState.substitute
    return if target.effects[PBEffects::Syrupy] > 0
    target.effects[PBEffects::Syrupy] = 3
    target.effects[PBEffects::SyrupyUser] = user.index
    @battle.pbDisplay(_INTL("{1}被粘稠的糖浆覆盖了！", target.pbThis))
  end

  def pbShowAnimation(id, user, targets, hitNum = 0, showAnimation = true)
    hitNum = (user.shiny?) ? 1 : 0
    super
  end
end
# 电光束
class PokeBattle_Move_1CA < PokeBattle_TwoTurnMove
  attr_reader :statUp

  def initialize(battle, move)
    super
    @statUp = [PBStats::SPATK, 1]
  end

  def pbIsChargingTurn?(user)
    ret = super
    if user.effects[PBEffects::TwoTurnAttack]==0
      w = @battle.pbWeather
      if (w==PBWeather::Rain || w==PBWeather::HeavyRain) && !user.hasUtilityUmbrella?
        @powerHerb = false
        @chargingTurn = true
        @damagingTurn = true
        return false
      end
    end
    return ret
  end

  def pbChargingTurnMessage(user, targets)
    @battle.pbDisplay(_INTL("{1}吸收了电力！", user.pbThis))
  end

  def pbChargingTurnEffect(user, target)
    if user.pbCanRaiseStatStage?(@statUp[0], user, self)
      user.pbRaiseStatStage(@statUp[0], @statUp[1], user)
    end
  end
end

# 随机光
class PokeBattle_Move_1CB < PokeBattle_Move
  def pbOnStartUse(user, targets)
    @allOutAttack = (@battle.pbRandom(100) < 30)
    if @allOutAttack
      @battle.pbDisplay(_INTL("{1}正全力以赴发动这次攻击！", user.pbThis))
    end
  end

  def pbBaseDamage(baseDmg, user, target)
    return (@allOutAttack) ? baseDmg * 2 : baseDmg
  end
  
  def pbShowAnimation(id, user, targets, hitNum = 0, showAnimation = true)
    hitNum = 1 if @allOutAttack
    super
  end
end

#御剑连斩
class PokeBattle_Move_1F1 < PokeBattle_Move
  def multiHitMove?; return true; end

  def pbNumHits(user,targets)
    return 4 + @battle.pbRandom(2) if user.hasActiveItem?(:LOADEDDICE)
    hitChances = [2,2,3,3,4,5]
    r = @battle.pbRandom(hitChances.length)
    r = hitChances.length-1 if user.hasActiveAbility?(:SKILLLINK)
    return hitChances[r]
  end
  
  def pbOnStartUse(user, targets)
    @forceEnd = false
    @accCheckPerHit = !user.hasActiveAbility?(:SKILLLINK) && !user.hasActiveItem?(:LOADEDDICE)
  end
  
  def pbEffectWhenDealingDamage(user, target)
    super
    @forceEnd = target.fainted?
  end
  
  def pbAdditionalEffect(user,target)
    if user.effects[PBEffects::FocusEnergy]<=2
      user.effects[PBEffects::FocusEnergy]+=1
      @battle.pbDisplay(_INTL("{1}的要害命中率提高！",user.pbThis))
    end
  end
end

#势如破竹
class PokeBattle_Move_1F2 < PokeBattle_Move
  
  def pbBaseDamage(baseDmg,user,target)
    mults = [1.0, 1.5, 2.0, 2.5, 3.0]
    index = [user.effects[PBEffects::BambooSword], mults.length - 1].min
    baseDmg *= mults[index]
    return baseDmg
  end
  
  def pbEffectWhenDealingDamage(user, target)
    user.effects[PBEffects::BambooSword] += 1
  end
  
  def pbMoveFailed?(user,targets)
    if !user.isSpecies?(:BAMSTRANE)
      @battle.pbDisplay(_INTL("但是{1}无法使用这个招式。",user.pbThis(true)))
      return true
    end
    return false
  end
  
  def pbEffectAfterAllHits(user,target)
    if !user.effects[PBEffects::LoseGrassType]
      user.effects[PBEffects::LoseGrassType] = true
      @battle.pbDisplay(_INTL("{1}失去了草属性！",user.pbThis))
    end
  end
end
#爆焰突进
class PokeBattle_Move_1F3 < PokeBattle_Move
  def pbMoveFailed?(user,targets)
    if !user.isSpecies?(:BLAZEPANDA)
      @battle.pbDisplay(_INTL("但是{1}无法使用这个招式。",user.pbThis(true)))
      return true
    end
    return false
  end
  
  def pbEffectAfterAllHits(user,target)
    if !user.effects[PBEffects::LoseFireType]
      user.effects[PBEffects::LoseFireType] = true
      @battle.pbDisplay(_INTL("{1}失去了火属性！",user.pbThis))
    end
    if @battle.pbWeather==PBWeather::Sun || @battle.pbWeather==PBWeather::HarshSun
      if user.hasActiveAbility?(:CONTRARY) &&
         user.pbCanLowerStatStage?(PBStats::DEFENSE,user,self)
        user.stages[PBStats::DEFENSE] = -6
        @battle.pbCommonAnimation("StatDown",user)
        @battle.pbDisplay(_INTL("{1}的防御下降到最低！",user.pbThis))
      elsif user.pbCanRaiseStatStage?(PBStats::DEFENSE,user,self)
        user.stages[PBStats::DEFENSE] = 6
        @battle.pbCommonAnimation("StatUp",user)
        @battle.pbDisplay(_INTL("{1}的防御提高到最大！",user.pbThis))
      end
    else
      if user.hasActiveAbility?(:CONTRARY) &&
         user.pbCanLowerStatStage?(PBStats::DEFENSE,user,self)
        user.pbLowerStatStage(PBStats::DEFENSE,2,user)
      elsif user.pbCanRaiseStatStage?(PBStats::DEFENSE,user,self)
        user.pbRaiseStatStage(PBStats::DEFENSE,2,user)
      end
    end
  end
end
#追本溯源
class PokeBattle_Move_1F4 < PokeBattle_Move
  
  def healingMove?; return NEWEST_BATTLE_MECHANICS; end

  def pbEffectAgainstTarget(user,target)
    ret = 1
    @battle.pbParty(user).each do |pkmn|
      next if !pkmn || pkmn.fainted?
      next if !pkmn.hasType?(:WATER) && !pkmn.hasType?(:POISON)
      ret += 1
    end
    ret = [ret, 5].min
    hpHeal = (user.totalhp*ret/8.0).round
    user.pbRecoverHP(hpHeal)
  end
  
  def pbMoveFailed?(user,targets)
    if !user.isSpecies?(:TOXICMANDER)
      @battle.pbDisplay(_INTL("但是{1}无法使用这个招式。",user.pbThis(true)))
      return true
    end
    return false
  end
  
  def pbEffectAfterAllHits(user,target)
    if !user.effects[PBEffects::LoseWaterType]
      user.effects[PBEffects::LoseWaterType] = true
      @battle.pbDisplay(_INTL("{1}失去了水属性！",user.pbThis))
    end
  end
end

# 烟雨针
class PokeBattle_Move_1F5 < PokeBattle_Move
  
  def pbAdditionalEffect(user,target)
    arr = []
    if !target.damageState.substitute && target.pbCanPoison?(user,false,self)
      arr.push(0)
    end
    if @battle.field.weather != PBWeather::Rain &&
       @battle.field.weather != PBWeather::HeavyRain
      arr.push(1)
    end
    ret = arr.shuffle.first
    case ret
    when 0
      target.pbPoison(user)
    when 1
      @battle.pbStartWeather(user,PBWeather::Rain,true,false)
    end
  end
end

# 电光双击
class PokeBattle_Move_1AE < PokeBattle_Move
  def pbMoveFailed?(user,targets)
    if !user.pbHasType?(:ELECTRIC)
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbEffectAfterAllHits(user,target)
    if !user.effects[PBEffects::DoubleShock]
      user.effects[PBEffects::DoubleShock] = true
      @battle.pbDisplay(_INTL("{1}失去了电属性！",user.pbThis))
    end
  end
end

# 辣椒精华
class PokeBattle_Move_1BE < PokeBattle_Move
  attr_reader :statUp, :statDown
  def canMagicCoat?; return true; end

  def initialize(battle, move)
    super
    @statUp   = [PBStats::ATTACK, 2]
    @statDown = [PBStats::DEFENSE, 2]
  end

  def pbFailsAgainstTarget?(user, target, show_message)
    return false if damagingMove?
    failed = !target.pbCanRaiseStatStage?(@statUp[0], user, self) && 
             !target.pbCanLowerStatStage?(@statDown[0], user, self)
    if failed
      @battle.pbDisplay(_INTL("{1}的能力等级不能再变化了！", target.pbThis)) if show_message
      return true
    end
    return false
  end

  def pbEffectAgainstTarget(user, target)
    return if damagingMove?
    if target.pbCanRaiseStatStage?(@statUp[0], user, self)
      target.pbRaiseStatStage(@statUp[0], @statUp[1], user)
    end
    if target.pbCanLowerStatStage?(@statDown[0], user, self)
      target.pbLowerStatStage(@statDown[0], @statDown[1], user)
    end
  end
end

# 愤怒之拳
class PokeBattle_Move_1EA < PokeBattle_Move
  def pbBaseDamage(baseDmg, user, target)
    bonus = 50 * user.num_times_hit
    return [baseDmg + bonus, 350].min
  end
end


# 复生祈祷
class PokeBattle_Move_1DD < PokeBattle_Move
  def healingMove?; return true; end
  
  def pbMoveFailed?(user, targets)
    @numFainted = 0
    user.battle.pbParty(user.idxOwnSide).each { |b| @numFainted += 1 if b.fainted? }
    if @numFainted == 0
      @battle.pbDisplay(_INTL("但是它失败了！"))
      return true
    end
    return false
  end
  
  def pbEndOfMoveUsageEffect(user, targets, numHits, switchedBattlers)
    return if user.fainted? || @numFainted == 0
    @battle.pbReviveInParty(user.index)
  end
end

class PokeBattle_Battle
  def pbReviveInParty(idxBattler, canCancel = false)
    party_index = -1
    if pbOwnedByPlayer?(idxBattler)
      @scene.pbPartyScreen(idxBattler, canCancel, 2) { |idxParty, partyScene|
        party_index = idxParty
        next true
      }
    else
      # AI使用的代码懒得移植了
      pbDisplay(_INTL("但是它失败了！"))
      return true
    end
    return if party_index < 0
    party = pbParty(idxBattler)
    pkmn = party[party_index]
    pkmn.hp = [1, (pkmn.totalhp / 2).floor].max
    pkmn.healStatus
    displayname = (pbOwnedByPlayer?(idxBattler)) ? pkmn.name : _INTL("对手的{1}", pkmn.name)
    pbDisplay(_INTL("{1}复活了并准备再次战斗！", displayname))
  end
end

#归无之光
class PokeBattle_Move_1C1 < PokeBattle_Move
  def pbCalcTypeModSingle(moveType,defType,user,target)
    ret = super
    if isConst?(defType,PBTypes,:FAIRY)
      ret = PBTypeEffectiveness::SUPER_EFFECTIVE_ONE
    end
    return ret
  end
  
    def pbGetDefenseStats(user,target)
    ret1, _ret2 = super
    return ret1, 6   # Def/SpDef stat stage
  end
end

# 全开猛撞/闪电猛冲
class PokeBattle_Move_1EE < PokeBattle_Move
  def pbMoveFailed?(user,targets)
    if !user.isSpecies?(:KORAIDON) && !user.isSpecies?(:MIRAIDON)
      @battle.pbDisplay(_INTL("但是{1}无法使用这个招式。",user.pbThis(true)))
      return true
    end
    return false
  end
  def pbBaseDamage(baseDmg, user, target)
    baseDmg *= 4 / 3.0 if PBTypes.superEffective?(target.damageState.typeMod)
    baseDmg *= 2 if target.pokemon.battleRank > 2
    return baseDmg
  end
end
# 线阱
class PokeBattle_Move_1CE < PokeBattle_ProtectMove
  def initialize(battle,move)
    super
    @effect = PBEffects::SilkTrap
  end
end

# 下压踢
class PokeBattle_Move_1CF < PokeBattle_ConfuseMove
  def recoilMove?;        return true; end

  def pbCrashDamage(user)
    return if !user.takesIndirectDamage?
    @battle.pbDisplay(_INTL("{1}继续前进并坠毁！",user.pbThis))
    @battle.scene.pbDamageAnimation(user)
    user.pbReduceHP(user.totalhp/2,false)
    user.pbItemHPHealCheck
    user.pbFaint if user.fainted?
  end
end

# 鼠数儿
class PokeBattle_Move_1D0 < PokeBattle_Move
  def multiHitMove?; return true; end

  def pbNumHits(user, targets)
    return 4 + rand(7) if user.hasActiveItem?(:LOADEDDICE)
    return 10
  end

  def successCheckPerHit?
    return @accCheckPerHit
  end

  def pbOnStartUse(user, targets)
    @accCheckPerHit = !user.hasActiveAbility?(:SKILLLINK) && !user.hasActiveItem?(:LOADEDDICE)
  end
end

# 大扫除
class PokeBattle_Move_1D1 < PokeBattle_MultiStatUpMove
  def initialize(battle, move)
    super
    @statUp = [PBStats::ATTACK, 1, PBStats::SPEED, 1]
  end
  
  def pbMoveFailed?(user, targets)
    failed = true
    2.times do |i|
      side = (i == 0) ? user.pbOwnSide : user.pbOpposingSide
      next unless side.effects[PBEffects::Spikes] > 0 ||
                  side.effects[PBEffects::ToxicSpikes] > 0 ||
                  side.effects[PBEffects::StealthRock] ||
                  side.effects[PBEffects::StickyWeb] ||
                  defined?(PBEffects::Steelsurge) && side.effects[PBEffects::Steelsurge]
      failed = false
      break
    end
    @battle.allBattlers.each do |b|
      next if b.effects[PBEffects::Substitute] == 0
        failed = false
      break
    end
    failed2 = true
    (@statUp.length / 2).times do |i|
      next if !user.pbCanRaiseStatStage?(@statUp[i * 2], user, self)
      failed2 = false
      break
    end
    if failed && failed2
      @battle.pbDisplay(_INTL("但是它失败了！", user.pbThis))
      return true
    end
    return false
  end

  def pbEffectGeneral(user)
    showMsg = false
    2.times do |i|
      side = (i == 0) ? user.pbOwnSide : user.pbOpposingSide
      team = (i == 0) ? user.pbTeam(true) : user.pbOpposingTeam(true)
      if side.effects[PBEffects::StealthRock]
        side.effects[PBEffects::StealthRock] = false
        @battle.pbDisplay(_INTL("{1}场上的隐形岩消失了！", team))
        showMsg = true
      end
      if defined?(PBEffects::Steelsurge) && side.effects[PBEffects::Steelsurge]
        side.effects[PBEffects::Steelsurge] = false
        @battle.pbDisplay(_INTL("{1}场上的钢钉消失了！", team))
        showMsg = true
      end
      if side.effects[PBEffects::Spikes] > 0
        side.effects[PBEffects::Spikes] = 0
        @battle.pbDisplay(_INTL("{1}场上的铁菱消失了！", team))
        showMsg = true
      end
      if side.effects[PBEffects::ToxicSpikes] > 0
        side.effects[PBEffects::ToxicSpikes] = 0
        @battle.pbDisplay(_INTL("{1}场上的毒菱消失了！", team))
        showMsg = true
      end
      if side.effects[PBEffects::StickyWeb]
        side.effects[PBEffects::StickyWeb] = false
        @battle.pbDisplay(_INTL("{1}场上的粘网消失了！", team))
        showMsg = true
      end

    end
    @battle.allBattlers.each do |b|
      next if b.effects[PBEffects::Substitute] == 0
      b.effects[PBEffects::Substitute] = 0
      showMsg = true
    end
    @battle.pbDisplay(_INTL("大扫除完成！")) if showMsg
    super
  end
end

# 盐腌
class PokeBattle_Move_1D2 < PokeBattle_Move
  def canMagicCoat?; return true; end

  def pbFailsAgainstTarget?(user, target)
    return false if damagingMove?
    if target.effects[PBEffects::SaltCure]
      @battle.pbDisplay(_INTL("但是它失败了！"))
      return true
    end
    return false
  end

  def pbEffectAgainstTarget(user, target)
    return if target.fainted?
    return if damagingMove?
    target.effects[PBEffects::SaltCure] = true
    @battle.pbDisplay(_INTL("{1}正在被盐腌！", target.pbThis))
  end

  def pbAdditionalEffect(user, target)
    return if target.fainted?
	  return if target.damageState.substitute
	  target.effects[PBEffects::SaltCure] = true
    @battle.pbDisplay(_INTL("{1}正在被盐腌！", target.pbThis))
  end
end

# 描绘
class PokeBattle_Move_1D3 < PokeBattle_Move
  def ignoresSubstitute?(user); return true; end
  
  def pbMoveFailed?(user, targets)
    @battle.allSameSideBattlers(user.index).each do |b|
      next if !b.unstoppableAbility?
      @battle.pbDisplay(_INTL("但是它失败了！"))
      return true
    end
    if user.hasActiveItem?(:ABILITYSHIELD)
      @battle.pbDisplay(_INTL("{1}的特性\n被特性护具的效果保护了！",user.pbThis))
      return true
    end
    return false
  end

  def pbFailsAgainstTarget?(user, target)
    if !target.ability || user.ability == target.ability
      @battle.pbDisplay(_INTL("但是它失败了！"))
      return true
    end
    if target.uncopyableAbility?
      @battle.pbDisplay(_INTL("但是它失败了！"))
      return true
    end
    return false
  end
  
  def pbEffectAgainstTarget(user, target)
    @battle.allSameSideBattlers(user).each do |b|
	  next if b.ability == target.ability
      if b.hasActiveItem?(:ABILITYSHIELD)
        @battle.pbDisplay(_INTL("{1}的特性\n被特性护具的效果保护了！", b.pbThis))
      else
        @battle.pbShowAbilitySplash(b, true, false)
        oldAbil = b.ability
        b.ability = target.ability
        @battle.pbReplaceAbilitySplash(b)
        @battle.pbDisplay(_INTL("{1}复制了{2}的{3}！",
                            user.pbThis, target.pbThis(true), target.abilityName))
        @battle.pbHideAbilitySplash(b)
        b.pbOnAbilityChanged(oldAbil)
      end
    end
  end
end

# 疾速转轮
class PokeBattle_Move_1D4 < PokeBattle_StatDownMove
  def initialize(battle,move)
    super
    @statDown = [PBStats::SPEED,2]
  end
end

# 断尾
class PokeBattle_Move_1D5 < PokeBattle_Move
  def pbMoveFailed?(user, targets)
    if user.effects[PBEffects::Substitute] > 0
      @battle.pbDisplay(_INTL("{1}已经有替身了！", user.pbThis))
      return true
    end
    @lifeCost = [(user.totalhp / 2).ceil, 1].max
    @subLife = [(@lifeCost / 4).ceil, 1].max
    if user.hp <= @lifeCost
      @battle.pbDisplay(_INTL("但是它没有足够的HP去制造分身！"))
      return true
    end
    return false
  end
  
  def pbOnStartUse(user, targets)
    user.pbReduceHP(@lifeCost, false, false)
    user.pbItemHPHealCheck
  end

  def pbEffectGeneral(user)
    user.effects[PBEffects::Trapping]     = 0
    user.effects[PBEffects::TrappingMove] = nil
    user.effects[PBEffects::Substitute]   = @subLife
    @battle.pbDisplay(_INTL("{1}甩掉了尾巴来制造诱饵！", user.pbThis))
  end

  def pbEndOfMoveUsageEffect(user, targets, numHits, switchedBattlers)
    return if user.fainted? || numHits == 0
    # 监视之眼
    targets.each do |t|
      if t.hasActiveAbility?(:WATCHDOGEYE)
        @battle.pbShowAbilitySplash(t)
        @battle.pbDisplay(_INTL("{1}的{2}阻止了\n{3}替换！",t.pbThis,
                          t.abilityName,user.pbThis))
        @battle.pbHideAbilitySplash(t)
        return
      end
    end
    return if !@battle.pbCanChooseNonActive?(user.index)
    @battle.pbDisplay(_INTL("{1}回到了{2}身边！", user.pbThis, @battle.pbGetOwnerName(user.index)))
    @battle.pbPursuit(user.index)
    oldSub = user.effects[PBEffects::Substitute]
    return if user.fainted?
    newPkmn = @battle.pbGetReplacementPokemonIndex(user.index)
    return if newPkmn < 0
    @battle.pbRecallAndReplace(user.index, newPkmn)
    @battle.pbClearChoice(user.index)
    @battle.moldBreaker = false
    switchedBattlers.push(user.index)
    user.pbEffectsOnSwitchIn(true)
    user.effects[PBEffects::Substitute] = oldSub
  end
end

# 扫墓
class PokeBattle_Move_1D6 < PokeBattle_Move
  def pbBaseDamage(baseDmg, user, target)
    numFainted = user.num_fainted_allies
    return baseDmg if numFainted <= 0
    baseDmg += 50 * numFainted
    max = $game_switches[99] ? 150 : 5050
    return [baseDmg, max].min
  end
end

# 冰旋
class PokeBattle_Move_1D7 < PokeBattle_Move
  def pbEffectGeneral(user)
    return if user.fainted?
    return if @battle.field.terrain == :None
    case @battle.field.terrain
    when PBBattleTerrains::Electric
      @battle.pbDisplay(_INTL("电流从场地上消失了！"))
    when PBBattleTerrains::Grassy
      @battle.pbDisplay(_INTL("草地从场地上消失了！"))
    when PBBattleTerrains::Misty
      @battle.pbDisplay(_INTL("迷雾从场地上消失了！"))
    when PBBattleTerrains::Psychic
      @battle.pbDisplay(_INTL("诡异的气息从场地上消失了！"))
    end
    @battle.pbStartTerrain(user,PBBattleTerrains::None,true)
  end
end

# 甩肉
class PokeBattle_Move_1D8 < PokeBattle_Move
  def initialize(battle,move)
    super
    @statUp = [PBStats::ATTACK,2,PBStats::SPATK,2,PBStats::SPEED,2]
  end
  
  def pbMoveFailed?(user,targets)
    hpLoss = [user.totalhp/2,1].max
    if user.hp<=hpLoss
      @battle.pbDisplay(_INTL("但是它失败了！"))
      return true
    end
    failed = true
    for i in 0...@statUp.length/2
      next if !user.pbCanRaiseStatStage?(@statUp[i*2],user,self)
      failed = false
      break
    end
    if failed
      @battle.pbDisplay(_INTL("{1}的能力不能再提高了！",user.pbThis))
      return true
    end
    return false
  end

  def pbEffectGeneral(user)
    showAnim = true
    hpLoss = [user.totalhp/2,1].max
    user.pbReduceHP(hpLoss,false)
    for i in 0...@statUp.length/2
      if user.hasActiveAbility?(:CONTRARY)
        next if !user.pbCanLowerStatStage?(@statUp[i*2],user,self)
        if user.pbLowerStatStage(@statUp[i*2],@statUp[i*2+1],user,showAnim)
          showAnim = false
        end
      else
        next if !user.pbCanRaiseStatStage?(@statUp[i*2],user,self)
        if user.pbRaiseStatStage(@statUp[i*2],@statUp[i*2+1],user,showAnim)
          showAnim = false
        end
      end
    end
    user.pbItemHPHealCheck
  end
end

# 上菜
class PokeBattle_Move_1D9 < PokeBattle_Move
  def pbEffectGeneral(user)
    if user.isCommanderHost?
      form = user.effects[PBEffects::Commander][1]
      stat = [PBStats::ATTACK, PBStats::DEFENSE, PBStats::SPEED][form]
      if user.pbCanRaiseStatStage?(stat, user, self)
        user.pbRaiseStatStage(stat, 1, user, true)
      end
    end
  end
  
  def pbShowAnimation(id, user, targets, hitNum = 0, showAnimation = true)
    hitNum = user.effects[PBEffects::Commander][1] + 1 if user.isCommanderHost? # Different animation based on Tatsugiri's form
    super
  end
end

# 淘金潮
class PokeBattle_Move_1DA < PokeBattle_Move
  attr_reader :statDown
  def initialize(battle, move)
    super
    @statDown = [PBStats::SPATK, 2]
  end
  
  def pbEndOfMoveUsageEffect(user, targets, numHits, switchedBattlers)
    #return if @battle.pbAllFainted?(user.idxOpposingSide)
    hit_target = false
    targets.each do |b|
      next if b.damageState.missed
      next if b.damageState.protected
      next if b.damageState.unaffected
      hit_target = true
      # Money modifier
      next if !user.pbOwnedByPlayer?
      @battle.field.effects[PBEffects::PayDay] += 5 * user.level
    end
    @battle.pbDisplay(_INTL("钱币散落地到处都是！")) if hit_target
    # Stats modifier
    if user.pbCanLowerStatStage?(@statDown[0], user, self) && hit_target
      user.pbLowerStatStage(@statDown[0], @statDown[1], user)
    end
  end
end

# 棘藤棒
class PokeBattle_Move_1B9 < PokeBattle_Move
  def initialize(battle,move)
    super
    if isConst?(@id,PBMoves,:IVYCUDGEL)
      @itemTypes = {
         :WELLSPRINGMASK   => :WATER,
         :HEARTHFLAMEMASK    => :FIRE,
         :CORNERSTONEMASK  => :ROCK
      }
    end
  end

  def pbBaseType(user)
    ret = getID(PBTypes,:GRASS)
    if user.itemActive?
      @itemTypes.each do |item, itemType|
        next if !isConst?(user.item, PBItems, item)
        t = getConst(PBTypes, itemType)
        ret = t || ret
        break
      end
    end
    return ret
  end
end

# 刷刷茶炮 
class PokeBattle_Move_1DE < PokeBattle_BurnMove
  def healingMove?; return true; end

  def pbEffectAgainstTarget(user, target)
    return if target.damageState.hpLost <= 0
    hpGain = (target.damageState.hpLost / 2.0).round
    user.pbRecoverHPFromDrain(hpGain, target)
    super
  end
end

# 硬压 
class PokeBattle_Move_1DF < PokeBattle_Move
  def pbBaseDamage(baseDmg, user, target)
    return [100 * target.hp / target.totalhp, 1].max
  end
end

# 巨兽斩/巨兽弹/极巨炮
class PokeBattle_Move_1DB < PokeBattle_Move
  def pbModifyDamage(damageMult,user,target)
    damageMult *= 2 if target.pokemon.battleRank > 2
    return damageMult
  end
end

# 晶光星群 
class PokeBattle_Move_1E0 < PokeBattle_Move
  def initialize(battle,move)
    super
    @calcCategory = 1
  end

  def pbMoveFailed?(user,targets)
    if !user.isSpecies?(:TERAPAGOS)
      @battle.pbDisplay(_INTL("但是{1}无法使用这个招式。",user.pbThis(true)))
      return true
    end
    return false
  end

  def physicalMove?(thisType=nil); return (@calcCategory==0); end
  def specialMove?(thisType=nil);  return (@calcCategory==1); end
  
  def pbBaseType(user)
    return @type if !(user.isSpecies?(:TERAPAGOS) && user.form == 2)
    return getConst(PBTypes,:STELLAR)
  end

  def pbTarget(user)
    if user.isSpecies?(:TERAPAGOS) && user.form == 2
      return PBTargets::AllFoes
    end
    return super
  end

  def pbOnStartUse(user, targets)
    # Calculate user's effective attacking value
    stageMul = [2,2,2,2,2,2, 2, 3,4,5,6,7,8]
    stageDiv = [8,7,6,5,4,3, 2, 2,2,2,2,2,2]
    atk        = user.attack
    atkStage   = user.stages[PBStats::ATTACK]+6
    realAtk    = (atk.to_f*stageMul[atkStage]/stageDiv[atkStage]).floor
    spAtk      = user.spatk
    spAtkStage = user.stages[PBStats::SPATK]+6
    realSpAtk  = (spAtk.to_f*stageMul[spAtkStage]/stageDiv[spAtkStage]).floor
    # Determine move's category
    @calcCategory = (realAtk>realSpAtk) ? 0 : 1
    return if !(user.isSpecies?(:TERAPAGOS) && user.form == 1)
    user.pbChangeFormTransform(2, _INTL("{1}变成了星晶形态！",user.pbThis))
  end
end

# 星晶爆发
class PokeBattle_Move_1DC < PokeBattle_StatDownMove
  def initialize(battle,move)
    super
    @calcCategory = 1
    @statDown = [PBStats::ATTACK,1,PBStats::SPATK,1]
  end

  def physicalMove?(thisType=nil); return (@calcCategory==0); end
  def specialMove?(thisType=nil);  return (@calcCategory==1); end
  
  def pbOnStartUse(user, targets)
    # Calculate user's effective attacking value
    stageMul = [2,2,2,2,2,2, 2, 3,4,5,6,7,8]
    stageDiv = [8,7,6,5,4,3, 2, 2,2,2,2,2,2]
    atk        = user.attack
    atkStage   = user.stages[PBStats::ATTACK]+6
    realAtk    = (atk.to_f*stageMul[atkStage]/stageDiv[atkStage]).floor
    spAtk      = user.spatk
    spAtkStage = user.stages[PBStats::SPATK]+6
    realSpAtk  = (spAtk.to_f*stageMul[spAtkStage]/stageDiv[spAtkStage]).floor
    # Determine move's category
    @calcCategory = (realAtk>realSpAtk) ? 0 : 1
  end
  
end


# 龙声鼓舞 
class PokeBattle_Move_1E1 < PokeBattle_Move
  def ignoresSubstitute?(user); return true; end
  def canSnatch?; return true; end

  def pbMoveFailed?(user, targets)
    @validTargets = []
    @battle.allSameSideBattlers(user).each do |b|
      next if b.index == user.index
      next if b.effects[PBEffects::FocusEnergy] > 0
      @validTargets.push(b)
    end
    if @validTargets.length == 0
      @battle.pbDisplay(_INTL("但是它失败了！"))
      return true
    end
    return false
  end

  def pbFailsAgainstTarget?(user, target)
    return false if @validTargets.any? { |b| b.index == target.index }
    @battle.pbDisplay(_INTL("{1}已经被鼓舞了！", target.pbThis))
    return true
  end

  def pbEffectAgainstTarget(user, target)
    boost = (target.pbHasType?(:DRAGON)) ? 2 : 1
    target.effects[PBEffects::FocusEnergy] = boost
    @battle.pbCommonAnimation("StatUp", target)
    @battle.pbDisplay(_INTL("{1}正在振奋起来！", target.pbThis))
  end
end

# 怒牛
class PokeBattle_Move_1E2 < PokeBattle_Move
  def pbBaseType(user)
    return @type if !user.isSpecies?(:TAUROS)
    userTypes = user.pbTypes
    return userTypes[1] || userTypes[0] || @type
  end
  
  def pbShowAnimation(id, user, targets, hitNum = 0, showAnimation = true)
    case pbBaseType(user)
    when :FIGHTING then hitNum = 1
    when :FIRE     then hitNum = 2
    when :WATER    then hitNum = 3
    else                hitNum = 0
    end
    super
  end
end

# 精神噪音
class PokeBattle_Move_1E3 < PokeBattle_Move
  def pbAdditionalEffect(user, target)
    return if target.effects[PBEffects::HealBlock] > 0
    return if pbMoveFailedAromaVeil?(user, target, false)
    target.effects[PBEffects::HealBlock] = 2
    @battle.pbDisplay(_INTL("{1}被阻止了治疗！", target.pbThis))
    target.pbItemStatusCureCheck
  end
end

# 魅诱之声 
class PokeBattle_Move_1E4 < PokeBattle_ConfuseMove
  def pbAdditionalEffect(user, target)
    super if target.statsRaisedThisRound
  end
end

# 无极光束
class PokeBattle_Move_1FD < PokeBattle_Move
  def pbOnStartUse(user, targets)
    return if !user.isSpecies?(:ETERNATUS)
    return if user.form == 1
    user.pbChangeFormTransform(1, _INTL("{1}变回了原来的样子！",user.pbThis))
  end
  def pbEffectGeneral(user)
    user.effects[PBEffects::HyperBeam] = 2
    user.currentMove = @id
  end
  def pbModifyDamage(damageMult,user,target)
    damageMult *= 2 if target.pokemon.battleRank > 2
    return damageMult
  end
end

# 星星光轮
class PokeBattle_Move_1AF < PokeBattle_FlinchMove
  def multiHitMove?;              return true; end
  def pbNumHits(user,targets);    return 2;    end
  def tramplesMinimize?(param=1); return true; end
    
  def pbCalcTypeModSingle(moveType,defType,user,target)
    ret = super
    if isConst?(defType,PBTypes,:DARK)
       isConst?(defType,PBTypes,:GHOST)
      ret = PBTypeEffectiveness::SUPER_EFFECTIVE_ONE
    end
    return ret
  end
  def pbModifyDamage(damageMult,user,target)
    damageMult *= 2 if target.pokemon.battleRank > 2
    return damageMult
  end
end


#===============================================================================
#Discus
#===============================================================================
class PokeBattle_Move_0BP < PokeBattle_Move
  def multiHitMove?;           return true; end
  def pbNumHits(user,targets); return 2;    end
  def pbCalcAccuracyMultipliers(user,target,multipliers)
    super
    modifiers[EVA_STAGE] = 0   # Accuracy stat stage
  end

  def pbGetDefenseStats(user,target)
    ret1, _ret2 = super
    return ret1, 6   # Def/SpDef stat stage
  end
  end
#===============================================================================
#GRANDEUR
#===============================================================================  
#===============================================================================
# losedamage if user was hit by a damaging move this round. 
#===============================================================================
class PokeBattle_Move_0BQ < PokeBattle_Move
  def pbDisplayChargeMessage(user)
    user.effects[PBEffects::FocusPunch] = true
    @battle.pbCommonAnimation("FocusPunch",user)
    @battle.pbDisplay(_INTL("{1}正在聚集阳光！",user.pbThis))
  end

  def pbDisplayUseMessage(user)
    super if !user.effects[PBEffects::FocusPunch] || user.lastHPLost==0
  end
  def pbBaseDamage(baseDmg,user,target)
    if user.effects[PBEffects::FocusPunch] && user.lastHPLost>0
     @battle.pbDisplay(_INTL("{1}没能聚集足够阳光！",user.pbThis))
     baseDmg *= 0.3 
    end
    return baseDmg
  end
end


#火之神神乐
class PokeBattle_Move_1B0 < PokeBattle_Move
  def pbCalcTypeModSingle(moveType,defType,user,target)
    ret = super
    if isConst?(defType,PBTypes,:DIM) ||
       isConst?(defType,PBTypes,:GHOST) ||
       isConst?(defType,PBTypes,:DARK)
      ret = PBTypeEffectiveness::SUPER_EFFECTIVE_ONE
    end
    return ret
  end
end
#群星闪耀
class PokeBattle_Move_1BF < PokeBattle_Move
  def pbEffectAgainstTarget(user,target)
    if target.damageState.calcDamage>0 && !target.damageState.substitute &&
       target.hasAlteredStatStages?
      target.pbResetStatStages
      @battle.pbDisplay(_INTL("{1}的能力变化被重置了！",target.pbThis))
    end
  end

  def pbModifyDamage(damageMult,user,target)
    damageMult *= 2 if target.pokemon.battleRank > 2
    return damageMult
  end
end

#群星陨落
class PokeBattle_Move_1EB < PokeBattle_Move
  def pbCalcAccuracyMultipliers(user,target,multipliers)
    super
    modifiers[EVA_STAGE] = 0   # Accuracy stat stage
  end

  def pbGetDefenseStats(user,target)
    ret1, _ret2 = super
    return ret1, 6   # Def/SpDef stat stage
  end

  def pbModifyDamage(damageMult,user,target)
    damageMult *= 2 if target.pokemon.battleRank > 2
    return damageMult
  end
end


# 光子喷涌
class PokeBattle_Move_1EC < PokeBattle_Move_163
  def initialize(battle, move)
    super
    @calcCategory = 1
  end

  def physicalMove?(thisType = nil)
    return (@calcCategory == 0)
  end

  def specialMove?(thisType = nil)
    return (@calcCategory == 1)
  end

    def pbOnStartUse(user, targets)
    if user.isSpecies?(:NECROZMA)
      if user.form == 1 # 黄昏之鬃 -> 形态3
        user.pbChangeFormTransform(3, _INTL("{1}在太阳的光芒中解放了真正的力量！", user.pbThis))
      elsif user.form == 2 # 拂晓之翼 -> 形态4
        user.pbChangeFormTransform(4, _INTL("{1}在月亮的光芒中解放了真正的力量！", user.pbThis))
      end
    end
    
    stageMul = [2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8]
    stageDiv = [8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2]
    atk = user.attack
    atkStage = user.stages[PBStats::ATTACK] + 6
    realAtk = (atk.to_f * stageMul[atkStage] / stageDiv[atkStage]).floor
    spAtk = user.spatk
    spAtkStage = user.stages[PBStats::SPATK] + 6
    realSpAtk = (spAtk.to_f * stageMul[spAtkStage] / stageDiv[spAtkStage]).floor
    @calcCategory = (realAtk > realSpAtk) ? 0 : 1
    if @battle.moldBreaker && targets && targets[0] && targets[0].hasActiveItem?(:ABILITYSHIELD)
      @battle.moldBreaker = false
    end
  end
end

# 断龙裁决剑
class PokeBattle_Move_1ED < PokeBattle_Move
  def pbBaseDamage(baseDmg,user,target)
    if target.hp < target.totalhp
      baseDmg *= 2
    end
    return baseDmg
  end
end
#===============================================================================
# Hits all near foes. Raises the user's Attack by 1 stage. (Starlight Galaxy - 星河流羽)
#===============================================================================
class PokeBattle_Move_1F0 < PokeBattle_Move
  def pbAdditionalEffect(user, target)
    return if target.damageState.substitute
    if user.pbCanRaiseStatStage?(PBStats::ATTACK, user, self)
      user.pbRaiseStatStage(PBStats::ATTACK, 1, user)
    end
  end
end
#===============================================================================
# 君王凌驾 
# 攻击敌方全体。如果有目标被本次攻击打倒，使用者的防御和特防会提高1级。
#===============================================================================
class PokeBattle_Move_1EF < PokeBattle_Move
  def pbTarget(user)
    return PBTargets::AllFoes
  end

  def pbEffectAfterAllHits(user, target)
    # 检查是否有目标被本次攻击打倒
    return if !target.damageState.fainted
    return if @battle.pbAllFainted?(target.idxOwnSide) && target.damageState.fainted
    showAnim = true
    if user.pbCanRaiseStatStage?(PBStats::DEFENSE, user, self)
      if user.pbRaiseStatStage(PBStats::DEFENSE, 1, user, showAnim)
        showAnim = false
      end
    end
    if user.pbCanRaiseStatStage?(PBStats::SPDEF, user, self)
      user.pbRaiseStatStage(PBStats::SPDEF, 1, user, showAnim)
    end
  end
end

#===============================================================================
# Extreme Evoboost
#九彩升华
#===============================================================================
# Raises all stats by 2 stages.
#-------------------------------------------------------------------------------
class PokeBattle_Move_1B5 < PokeBattle_Move
  def initialize(battle, move)
    super(battle, move)
  end

  def pbEffectGeneral(user)
    # 提升所有能力 1 级
    stats = [PBStats::ATTACK, PBStats::DEFENSE, 
             PBStats::SPATK, PBStats::SPDEF]
    showAnim = true
    stats.each do |stat|
      if user.pbCanRaiseStatStage?(stat, user, self)
        user.pbRaiseStatStage(stat, 1, user, showAnim)
        showAnim = false
      end
    end
    @battle.pbDisplay(_INTL("{1}借助伙伴们的力量，\n所有能力都大幅提升了！", user.pbThis))
    return 0
  end
end

# 黑暗重拳
class PokeBattle_Move_1B6 < PokeBattle_FlinchMove
  def pbModifyDamage(damageMult,user,target)
    damageMult *= 2 if target.pokemon.battleRank > 2
    return damageMult
  end
end



#===============================================================================
# 地魔之剑 (EARTHDEMONSWORD)
# 用凝聚地魔之力的大剑斩击全体对手。有时会让对手陷入中毒状态，
# 且使其在2回合内受到的毒属性伤害增加。
#===============================================================================
class PokeBattle_Move_1F9 < PokeBattle_Move
  
  #30%概率
  def pbAdditionalEffect(user, target)
    return if target.fainted? || target.damageState.substitute
    # 使对手中毒
    return if !target.pbCanPoison?(user, false, self)
    target.pbPoison(user)
    # 设置毒系增伤标记，持续2回合
    if !target.effects[PBEffects::PoisonVulnerability]
      target.effects[PBEffects::PoisonVulnerability] = 2
      @battle.pbDisplay(_INTL("{1}被地魔之力侵蚀了！", target.pbThis))
    end
  end
end

#===============================================================================
# 海魔之雨 (SEADEMONRAIN)
# 用凝聚海魔之力的暴雨攻击全体对手。有时会让对手陷入冻伤状态，
# 且使其在2回合内受到的冰属性伤害增加。
#===============================================================================
class PokeBattle_Move_1FA < PokeBattle_Move
  
  #30%概率
  def pbAdditionalEffect(user, target)
    return if target.fainted? || target.damageState.substitute
    # 使对手冻伤
    return if !target.pbCanFreeze?(user, false, self)
    target.pbFreeze
    # 设置冰系增伤标记，持续2回合
    if !target.effects[PBEffects::IceVulnerability]
      target.effects[PBEffects::IceVulnerability] = 2
      @battle.pbDisplay(_INTL("{1}被海魔之力侵蚀了！", target.pbThis))
    end
  end
end


#===============================================================================
# Genesis Supernova
#===============================================================================
# Sets Psychic Terrain.
#-------------------------------------------------------------------------------
class PokeBattle_Move_1B7 < PokeBattle_Move
  def pbAdditionalEffect(user,target)
    @battle.pbStartTerrain(user,PBBattleTerrains::Psychic)
  end
end 
#===============================================================================
# Guardian of Alola
#===============================================================================
# Inflicts 75% of the target's current HP.
#-------------------------------------------------------------------------------
class PokeBattle_Move_1B8 < PokeBattle_Move
  def pbFixedDamage(user,target)
    # 如果目标战斗等级大于2，伤害减为原来的1/5
    if target.pokemon.battleRank > 2
      return (target.hp * 0.75 / 5.0).round
    else
      return (target.hp * 0.75).round
    end
  end
  
  def pbCalcDamage(user,target,numTargets=1)
    target.damageState.critical   = false
    target.damageState.calcDamage = pbFixedDamage(user,target)
    target.damageState.calcDamage = 1 if target.damageState.calcDamage<1
  end
end
#===============================================================================
# Menacing Moonraze Maelstrom, Searing Sunraze Smash
#===============================================================================
# Ignores ability.
#-------------------------------------------------------------------------------
class PokeBattle_Move_1BA < PokeBattle_Move
  def pbChangeUsageCounters(user,specialUsage)
    super
    @battle.moldBreaker = true if !specialUsage
  end
end 
#===============================================================================
# Splintered Stormshards
#===============================================================================
# Removes terrains.
#-------------------------------------------------------------------------------
class PokeBattle_Move_1BB < PokeBattle_Move
  def pbAdditionalEffect(user,target)
    case @battle.field.terrain
    when PBBattleTerrains::Electric
      @battle.pbDisplay(_INTL("电流从战场上消失了!"))
    when PBBattleTerrains::Grassy
      @battle.pbDisplay(_INTL("草原从战场上消失了!"))
    when PBBattleTerrains::Misty
      @battle.pbDisplay(_INTL("大雾从战场上消失了!"))
    when PBBattleTerrains::Psychic
      @battle.pbDisplay(_INTL("奇怪的气场从战场上消失了!"))
    end
    @battle.pbStartTerrain(user,PBBattleTerrains::None,true)
  end
end 
#等离子闪电拳
class PokeBattle_Move_1BC < PokeBattle_Move

  def flinchingMove?
    return isConst?(@id, PBMoves, :PLASMAFISTS)
  end

  def pbMoveFailed?(user,targets)
    return false if damagingMove?
    if @battle.field.effects[PBEffects::IonDeluge]
      @battle.pbDisplay(_INTL("但是它失败了！"))
      return true
    end
    return true if pbMoveFailedLastInRound?(user)
    return false
  end

  def pbEffectGeneral(user)
    return if @battle.field.effects[PBEffects::IonDeluge]
    @battle.field.effects[PBEffects::IonDeluge] = true
    @battle.pbDisplay(_INTL("洪流般的等离子充满着场地！"))
  end

  def pbAdditionalEffect(user,target)
    return if !isConst?(@id, PBMoves, :PLASMAFISTS)
    return if target.damageState.substitute
    target.pbFlinch(user)
  end
  
end

#===============================================================================
# 根源波动 (Origin Pulse)
# 雨天时必定命中
#===============================================================================
class PokeBattle_Move_1BD < PokeBattle_Move
  def pbCalcAccuracyMultipliers(user,target,multipliers)
    super
    # 如果天气是雨天或大雨天，命中率变为必中
    if @battle.pbWeather==PBWeather::Rain ||
       @battle.pbWeather==PBWeather::HeavyRain
       modifiers[EVA_STAGE]  = 0  # 0表示必中
    end
  end
    def pbModifyDamage(damageMult,user,target)
    damageMult *= 2 if target.pokemon.battleRank > 2
    return damageMult
  end
end

#===============================================================================
# 断崖之剑 (Precipice Blades)
# 晴天时必定命中
#===============================================================================
class PokeBattle_Move_1C4 < PokeBattle_Move
  def pbAccuracyCheck(user,target)
    # 如果天气是晴天或大晴天，必定命中
    if @battle.pbWeather==PBWeather::Sun ||
       @battle.pbWeather==PBWeather::HarshSun
      return true
    end
    return super
  end
    def pbModifyDamage(damageMult,user,target)
    damageMult *= 2 if target.pokemon.battleRank > 2
    return damageMult
  end
end

#===============================================================================
# 晶光雨 (GLITTERRAIN)
# 冰系特殊招式，攻击所有敌人。雨天时威力变为1.5倍。
#===============================================================================
class PokeBattle_Move_1C5 < PokeBattle_Move
  def pbBaseDamage(baseDmg, user, target)
    w = @battle.pbWeather
    if (w == PBWeather::Rain || w == PBWeather::HeavyRain) && !user.hasUtilityUmbrella?
      baseDmg = (baseDmg * 1.5).round
    end
    return baseDmg
  end
end

#===============================================================================
# 光烨羽舞 (FEATHERDANCE)
# 光系特殊招式，攻击所有敌人。使用后自身特防提升1级。
#===============================================================================
class PokeBattle_Move_1C6 < PokeBattle_Move
  def pbEffectGeneral(user)
    # 攻击后提升自身特防1级
    if user.pbCanRaiseStatStage?(PBStats::SPDEF, user, self)
      user.pbRaiseStatStage(PBStats::SPDEF, 1, user)
    end
  end
end

#===============================================================================
# 妄之歌 (SONGOFDELUSION)
# 在对方场上降下妖精的歌声，持续5回合。
# 使对方场上和替换出场的宝可梦陷入混乱状态。
#===============================================================================
class PokeBattle_Move_1C8 < PokeBattle_Move
  def pbMoveFailed?(user, targets)
    if user.pbOpposingSide.effects[PBEffects::DelusionSong] > 0
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    return false
  end

  def pbEffectGeneral(user)
    user.pbOpposingSide.effects[PBEffects::DelusionSong] = 5
    @battle.pbDisplay(_INTL("妖精的歌声在{1}场上回荡！", user.pbOpposingTeam(true)))
    # 立即让对方场上所有宝可梦混乱
    @battle.eachOtherSideBattler(user) do |b|
      next if b.fainted?
      next if b.effects[PBEffects::Substitute] > 0
      if b.pbCanConfuse?(user, false, self)
        b.pbConfuse
      end
    end
  end
end


#===============================================================================
# 哀悼悲叹 (MOURNFULLAMENT)
# 幽灵系特殊招式。队伍中存在濒死状态的队友时，威力变为2倍。
#===============================================================================
class PokeBattle_Move_1C9 < PokeBattle_Move
  def pbBaseDamage(baseDmg, user, target)
    # 检查队伍中是否有濒死的队友
    @battle.pbParty(user.index).each_with_index do |pkmn, i|
      next if !pkmn || i == user.pokemonIndex
      next if !pkmn.fainted?
      baseDmg *= 2
      break
    end
    return baseDmg
  end
end

#===============================================================================
# 幻海妖歌 (SIRENSONG)
# 水系特殊招式。40%概率使目标陷入魅惑（着迷）状态。
# 若目标已处于魅惑状态，则无视其特防提升。
#===============================================================================
class PokeBattle_Move_1C7 < PokeBattle_Move
  def pbAdditionalEffect(user, target)
    return if target.fainted? || target.damageState.substitute
    return if @battle.pbRandom(100) >= 40

    # 40%概率使目标着迷（target 对着 user 着迷）
    if target.effects[PBEffects::Attract] < 0 &&
       target.pbCanAttract?(user, false)
      target.pbAttract(user)
    end
  end

  # 若目标已处于魅惑状态，无视其特防提升
  def pbGetDefenseStats(user, target)
    if target.effects[PBEffects::Attract] >= 0
      return target.spdef, 6   # 特防等级视为 0
    end
    return super
  end
end

#巨力锤
class PokeBattle_Move_1C8 < PokeBattle_Move
  def pbEffectWhenDealingDamage(user, target)
    user.effects[PBEffects::SuccessiveMove] = @id
  end
end


# 浴火重生
class PokeBattle_Move_1E5 < PokeBattle_Move

  def pbEffectAgainstTarget(user,target)
    if user.reborn?
      @battle.pbDisplay(_INTL("但是{1}无法再次复活了",user.pbThis(true)))
      return
    end
    user.setCanRebirth
  end
end



# 热带海流
class PokeBattle_Move_1E6 < PokeBattle_Move

  def pbEffectGeneral(user)
    case @battle.field.weather
    when PBWeather::Rain,
         PBWeather::HeavyRain,
         PBWeather::HarshSun,
         PBWeather::StrongWinds
    else
      @battle.pbStartWeather(user,PBWeather::Rain,true,false)
    end
    if user.pbOwnSide.effects[PBEffects::Tailwind] < 1
      user.pbOwnSide.effects[PBEffects::Tailwind] = 4
      @battle.pbDisplay(_INTL("{1}刮起了顺风！",user.pbTeam(true)))
    end
    if user.effects[PBEffects::LeechSeed]>=0
      user.effects[PBEffects::LeechSeed] = -1
      @battle.pbDisplay(_INTL("{1}清除了寄生种子！",user.pbThis))
    end
    if user.pbOwnSide.effects[PBEffects::StealthRock]
      user.pbOwnSide.effects[PBEffects::StealthRock] = false
      @battle.pbDisplay(_INTL("{1}吹散了隐形岩！",user.pbThis))
    end
    if user.pbOwnSide.effects[PBEffects::Spikes]>0
      user.pbOwnSide.effects[PBEffects::Spikes] = 0
      @battle.pbDisplay(_INTL("{1}吹走了铁菱！",user.pbThis))
    end
    if user.pbOwnSide.effects[PBEffects::ToxicSpikes]>0
      user.pbOwnSide.effects[PBEffects::ToxicSpikes] = 0
      @battle.pbDisplay(_INTL("{1}吹走了毒菱！",user.pbThis))
    end
    if user.pbOwnSide.effects[PBEffects::StickyWeb]
      user.pbOwnSide.effects[PBEffects::StickyWeb] = false
      user.pbOwnSide.effects[PBEffects::StickyWebUser] = -1
      @battle.pbDisplay(_INTL("{1}吹走了粘网！",user.pbThis))
    end
    case @battle.field.terrain
    when PBBattleTerrains::Electric
      @battle.pbDisplay(_INTL("电流从场上消失了!"))
    when PBBattleTerrains::Grassy
      @battle.pbDisplay(_INTL("青草从场上消失了！"))
    when PBBattleTerrains::Misty
      @battle.pbDisplay(_INTL("迷雾从场上消失了！"))
    when PBBattleTerrains::Psychic
      @battle.pbDisplay(_INTL("诡异的气场从场上消失了!"))
    when PBBattleTerrains::BugLure
      @battle.pbDisplay(_INTL("虫网从场上消失了！"))
    when PBBattleTerrains::Cold
      @battle.pbDisplay(_INTL("冰霜从场上消失了！"))
    end
    @battle.pbStartTerrain(user,PBBattleTerrains::None,true)
  end
  
  def pbEffectAfterAllHits(user,target)
    return if user.fainted? || target.damageState.unaffected
    if target.pbOwnSide.effects[PBEffects::AuroraVeil]>0
      target.pbOwnSide.effects[PBEffects::AuroraVeil] = 0
      @battle.pbDisplay(_INTL("{1}的极光幕消失了！",target.pbTeam))
    end
    if target.pbOwnSide.effects[PBEffects::LightScreen]>0
      target.pbOwnSide.effects[PBEffects::LightScreen] = 0
      @battle.pbDisplay(_INTL("{1}的光墙消失了！",target.pbTeam))
    end
    if target.pbOwnSide.effects[PBEffects::Reflect]>0
      target.pbOwnSide.effects[PBEffects::Reflect] = 0
      @battle.pbDisplay(_INTL("{1}的反射壁消失了！",target.pbTeam))
    end
    if target.pbOwnSide.effects[PBEffects::Mist]>0
      target.pbOwnSide.effects[PBEffects::Mist] = 0
      @battle.pbDisplay(_INTL("{1}的迷雾消失了！",target.pbTeam))
    end
    if target.pbOwnSide.effects[PBEffects::Safeguard]>0
      target.pbOwnSide.effects[PBEffects::Safeguard] = 0
      @battle.pbDisplay(_INTL("{1}不再被神秘守护保护了！",target.pbTeam))
    end
  end

  def pbAdditionalEffect(user,target)
    return if target.damageState.substitute
    target.pbBurn(user) if target.pbCanBurn?(user,false,self)
  end
end



# 极光虹雨
# 用天光与海气交织成的虹色光雨攻击对手。晴天或雨天时威力上升，凤王与洛奇亚同时在场则进一步提升。
class PokeBattle_Move_1E7 < PokeBattle_Move

  def pbMoveFailed?(user,targets)
    if !user.isSpecies?(:HOOH) && !user.isSpecies?(:LUGIA)
      @battle.pbDisplay(_INTL("但是{1}无法使用这个招式！",user.pbThis))
      return true
    end
    return false
  end
  
  def pbBaseDamage(baseDmg,user,target)
    mult = 1.0
    if [PBWeather::Sun, PBWeather::HarshSun, PBWeather::Rain, PBWeather::HeavyRain].include?(@battle.pbWeather)
      mult += 0.2
    end
    user.eachAlly do | ally |
      next if ally.fainted?
      if (ally.isSpecies?(:HOOH) && user.isSpecies?(:LUGIA)) ||
         (ally.isSpecies?(:LUGIA) && user.isSpecies?(:HOOH))
        mult += 0.3
        break
      end
    end
    return (baseDmg * mult).round
  end
end


#===============================================================================
# 生命屏障 (Life Barrier)
# 妖精属性，变化招式，必中。
# 使用者损失最大HP的1/2（向下取整），制造极光幕，在5回合内减弱物理和特殊的伤害。
#===============================================================================
class PokeBattle_Move_1E8 < PokeBattle_Move
  def pbMoveFailed?(user, targets)
    # 己方已有极光幕时失败
    if user.pbOwnSide.effects[PBEffects::AuroraVeil] > 0
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    # 检查HP是否足够支付一半最大HP
    hpLoss = [user.totalhp / 2, 1].max
    if user.hp <= hpLoss
      @battle.pbDisplay(_INTL("但是没有足够的HP制造屏障！"))
      return true
    end
    return false
  end

  def pbOnStartUse(user, targets)
    # 损失最大HP的1/2（向下取整）
    hpLoss = [user.totalhp / 2, 1].max
    user.pbReduceHP(hpLoss, false, false)
    user.pbItemHPHealCheck
  end

  def pbEffectGeneral(user)
    # 制造极光幕，持续5回合；携带光之黏土则延长至8回合
    turns = user.hasActiveItem?(:LIGHTCLAY) ? 8 : 5
    user.pbOwnSide.effects[PBEffects::AuroraVeil] = turns
    @battle.pbDisplay(_INTL("{1}牺牲了生命，制造了生命屏障！", user.pbThis))
    end

  # 变化招式必中
  def pbAccuracyCheck(user, target)
    return true
  end
end

#===============================================================================
# 空魔之雷 (Void Demon Thunder)
# 电属性，特殊，威力90，必中。
# 招式效果：自身进入充电状态。
# 额外效果：自身特防提升1级。
# 两个效果相互独立，互不影响。
#===============================================================================
class PokeBattle_Move_1F6 < PokeBattle_Move
  # 必中
  def pbAccuracyCheck(user, target)
    return true
  end

  def pbEffectAfterAllHits(user, target)
    return if user.fainted?
    return if @effectApplied
    @effectApplied = true

    # 独立效果1：进入充电状态
    user.effects[PBEffects::Charge] = 2
    @battle.pbDisplay(_INTL("{1}溢出的电能使其进入了充电状态！", user.pbThis))
  end
end

#===============================================================================
# 捕鼠笼 (Mousetrap)
# 地面属性，物理，威力120，命中100，PP5(8)，先制+2，非接触。
# 蓄力时双防+1并将对手的攻击吸引到自己身上；受到攻击时进行攻击。
#===============================================================================
class PokeBattle_Move_1F7 < PokeBattle_Move
  # 蓄力：双防+1，吸引攻击
  def pbDisplayChargeMessage(user)
    user.effects[PBEffects::MouseTrap] = true
    @battle.pbCommonAnimation("ShellTrap", user)
    @battle.pbDisplay(_INTL("{1}张开捕鼠笼，等待对手自投罗网！", user.pbThis))

    # 双防各 +1
    showAnim = true
    [PBStats::DEFENSE, PBStats::SPDEF].each do |stat|
      next if !user.pbCanRaiseStatStage?(stat, user, self)
      showAnim = false if user.pbRaiseStatStage(stat, 1, user, showAnim)
    end

    # 吸引对手单体攻击指向自己
    user.effects[PBEffects::FollowMe] = 1
    user.eachAlly do |b|
      next if b.effects[PBEffects::FollowMe] < user.effects[PBEffects::FollowMe]
      user.effects[PBEffects::FollowMe] = b.effects[PBEffects::FollowMe] + 1
    end
  end

  # 只有本回合确实受到伤害时才显示出招信息
  def pbDisplayUseMessage(user)
    super if user.lastHPLost > 0
  end

  def pbMoveFailed?(user, targets)
    if !user.effects[PBEffects::MouseTrap]
      @battle.pbDisplay(_INTL("但是失败了！"))
      return true
    end
    if user.lastHPLost == 0
      @battle.pbDisplay(_INTL("{1}的捕鼠笼没有捕获到猎物……", user.pbThis))
      user.effects[PBEffects::MouseTrap] = false   # 失败时也清除标记
      return true
    end
    return false
  end

  # 成功结算后清除标记
  def pbEffectAfterAllHits(user, target)
    user.effects[PBEffects::MouseTrap] = false
  end
end