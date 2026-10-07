class PokeBattle_Battle
  #=============================================================================
  # Decrement effect counters
  #=============================================================================
  def pbEORCountDownBattlerEffect(priority,effect)
    priority.each do |b|
      next if b.fainted? || b.effects[effect]==0
      b.effects[effect] -= 1
      yield b if block_given? && b.effects[effect]==0
    end
  end

  def pbEORCountDownSideEffect(side,effect,msg)
    if @sides[side].effects[effect]>0
      @sides[side].effects[effect] -= 1
      pbDisplay(msg) if @sides[side].effects[effect]==0
    end
  end

  def pbEORCountDownFieldEffect(effect,msg)
    if @field.effects[effect]>0
      @field.effects[effect] -= 1
      if @field.effects[effect]==0
        pbDisplay(msg)
        if effect==PBEffects::MagicRoom
          pbPriority(true).each { |b| b.pbItemTerrainStatBoostCheck }
        end
      end
    end
  end

  #=============================================================================
  # End Of Round weather
  #=============================================================================
  def pbEORWeather(priority)
    # NOTE: Primordial weather doesn't need to be checked here, because if it
    #       could wear off here, it will have worn off already.
    # Count down weather duration
    @field.weatherDuration -= 1 if @field.weatherDuration>0
    # Weather wears off
    if @field.weatherDuration==0
      case @field.weather
      when PBWeather::Sun
        pbDisplay(_INTL("阳光减弱了！"))
      when PBWeather::Rain
        pbDisplay(_INTL("雨停了！"))
      when PBWeather::Sandstorm
        pbDisplay(_INTL("沙尘暴平息了！"))
      when PBWeather::Hail
        pbDisplay(_INTL("冰雹停止了。"))
      when PBWeather::Snow
        pbDisplay(_INTL("不再下雪了。"))
      when PBWeather::ShadowSky
        pbDisplay(_INTL("暗影天空消失了。"))
      end
      @field.weather = PBWeather::None
      # Check for form changes caused by the weather changing
      eachBattler { |b| b.pbCheckFormOnWeatherChange }
      # Start up the default weather
      pbStartWeather(nil,@field.defaultWeather) if @field.defaultWeather!=PBWeather::None
      return if @field.weather==PBWeather::None
    end
    # Weather continues
    pbCommonAnimation(PBWeather.animationName(@field.weather))
    case @field.weather
#    when PBWeather::Sun;         pbDisplay(_INTL("阳光令人目眩！"))
#    when PBWeather::Rain;        pbDisplay(_INTL("雨仍在下！"))
    when PBWeather::Sandstorm;   pbDisplay(_INTL("沙暴正在肆虐！"))
    when PBWeather::Hail;        pbDisplay(_INTL("冰雹正在砸落！"))
#    when PBWeather::HarshSun;    pbDisplay(_INTL("阳光极为强烈！"))
#    when PBWeather::HeavyRain;   pbDisplay(_INTL("暴雨浩浩荡荡！"))
#    when PBWeather::StrongWinds; pbDisplay(_INTL("狂风呼啸不止！"))
    when PBWeather::ShadowSky;   pbDisplay(_INTL("天空依旧灰暗！"));
    end
    # Effects due to weather
    curWeather = pbWeather
    priority.each do |b|
      # Weather-related abilities
      if b.abilityActive?
        #next if !b.hasActiveAbility?(:ICEBODY)
        BattleHandlers.triggerEORWeatherAbility(b.ability,curWeather,b,self)
        b.pbFaint if b.fainted?
      end
      # Weather damage
      # NOTE:
      case curWeather
      when PBWeather::Sandstorm
        next if !b.takesSandstormDamage?
        pbDisplay(_INTL("{1}被沙暴伤害了！",b.pbThis))
        @scene.pbDamageAnimation(b)
        b.pbReduceHP(b.totalhp/16,false)
        b.pbItemHPHealCheck
        b.pbFaint if b.fainted?
      when PBWeather::Hail
        next if !b.takesHailDamage?
        pbDisplay(_INTL("{1}被冰雹伤害了！",b.pbThis))
        @scene.pbDamageAnimation(b)
        b.pbReduceHP(b.totalhp/16,false)
        b.pbItemHPHealCheck
        b.pbFaint if b.fainted?
      when PBWeather::ShadowSky
        next if !b.takesShadowSkyDamage?
        pbDisplay(_INTL("{1}被黑暗气象伤害了！",b.pbThis))
        @scene.pbDamageAnimation(b)
        b.pbReduceHP(b.totalhp/16,false)
        b.pbItemHPHealCheck
        b.pbFaint if b.fainted?
      end
    end
  end

  #=============================================================================
  # End Of Round terrain
  #=============================================================================
  def pbEORTerrain
    # Count down terrain duration
    @field.terrainDuration -= 1 if @field.terrainDuration>0
    # Terrain wears off
    if @field.terrain!=PBBattleTerrains::None && @field.terrainDuration==0
      case @field.terrain
      when PBBattleTerrains::Electric
        pbDisplay(_INTL("场上的电流消失了！"))
      when PBBattleTerrains::Grassy
        pbDisplay(_INTL("四周的青草枯萎了！"))
      when PBBattleTerrains::Misty
        pbDisplay(_INTL("四周的薄雾消散了！"))
      when PBBattleTerrains::Psychic
        pbDisplay(_INTL("场地恢复原样了！"))
      end
      @field.terrain = PBBattleTerrains::None
      eachBattler { |b| b.pbAbilityOnTerrainChange }
      # Start up the default terrain
      if @field.defaultTerrain!=PBBattleTerrains::None
        pbStartTerrain(nil,@field.defaultTerrain,false)
        eachBattler { |b| b.pbAbilityOnTerrainChange }
      end
      return if @field.terrain==PBBattleTerrains::None
    end
    # Terrain continues
    pbCommonAnimation(PBBattleTerrains.animationName(@field.terrain))
    case @field.terrain
    when PBBattleTerrains::Electric; pbDisplay(_INTL("电流在场上肆虐！"))
    when PBBattleTerrains::Grassy;   pbDisplay(_INTL("青草覆盖了四周！"))
    when PBBattleTerrains::Misty;    pbDisplay(_INTL("薄雾笼罩着四周！"))
    when PBBattleTerrains::Psychic;  pbDisplay(_INTL("周围变得极为瑰异！"))
    end
  end

  #=============================================================================
  # End Of Round shift distant battlers to middle positions
  #=============================================================================
  def pbEORShiftDistantBattlers
    # Move battlers around if none are near to each other
    # NOTE: This code assumes each side has a maximum of 3 battlers on it, and
    #       is not generalised to larger side sizes.
    if !singleBattle?
      swaps = []   # Each element is an array of two battler indices to swap
      for side in 0...2
        next if pbSideSize(side)==1   # Only battlers on sides of size 2+ need to move
        # Check if any battler on this side is near any battler on the other side
        anyNear = false
        eachSameSideBattler(side) do |b|
          eachOtherSideBattler(b) do |otherB|
            next if !nearBattlers?(otherB.index,b.index)
            anyNear = true
            break
          end
          break if anyNear
        end
        break if anyNear
        # No battlers on this side are near any battlers on the other side; try
        # to move them
        # NOTE: If we get to here (assuming both sides are of size 3 or less),
        #       there is definitely only 1 able battler on this side, so we
        #       don't need to worry about multiple battlers trying to move into
        #       the same position. If you add support for a side of size 4+,
        #       this code will need revising to account for that, as well as to
        #       add more complex code to ensure battlers will end up near each
        #       other.
        eachSameSideBattler(side) do |b|
          # Get the position to move to
          pos = -1
          case pbSideSize(side)
          when 2; pos = [2,3,0,1][b.index]   # The unoccupied position
          when 3; pos = (side==0) ? 2 : 3    # The centre position
          end
          next if pos<0
          # Can't move if the same trainer doesn't control both positions
          idxOwner = pbGetOwnerIndexFromBattlerIndex(b.index)
          next if pbGetOwnerIndexFromBattlerIndex(pos)!=idxOwner
          swaps.push([b.index,pos])
        end
      end
      # Move battlers around
      swaps.each do |pair|
        next if pbSideSize(pair[0])==2 && swaps.length>1
        next if !pbSwapBattlers(pair[0],pair[1])
        case pbSideSize(side)
        when 2
          pbDisplay(_INTL("{1}移动到了另一边！",@battlers[pair[1]].pbThis))
        when 3
          pbDisplay(_INTL("{1}移动到了中心！",@battlers[pair[1]].pbThis))
        end
      end
    end
  end

  #=============================================================================
  # End Of Round phase
  #=============================================================================
  def pbEndOfRoundPhase
    PBDebug.log("")
    PBDebug.log("[End of round]")
    @endOfRound = true
    @scene.pbBeginEndOfRoundPhase
    pbCalculatePriority           # recalculate speeds
    priority = pbPriority(true)   # in order of fastest -> slowest speeds only
    # Weather
    pbEORWeather(priority)
    # Future Sight/Doom Desire
    @positions.each_with_index do |pos,idxPos|
      next if !pos || pos.effects[PBEffects::FutureSightCounter]==0
      pos.effects[PBEffects::FutureSightCounter] -= 1
      next if pos.effects[PBEffects::FutureSightCounter]>0
      next if !@battlers[idxPos] || @battlers[idxPos].fainted?   # No target
      moveUser = nil
      eachBattler do |b|
        next if b.opposes?(pos.effects[PBEffects::FutureSightUserIndex])
        next if b.pokemonIndex!=pos.effects[PBEffects::FutureSightUserPartyIndex]
        moveUser = b
        break
      end
      next if moveUser && moveUser.index==idxPos   # Target is the user
      if !moveUser   # User isn't in battle, get it from the party
        party = pbParty(pos.effects[PBEffects::FutureSightUserIndex])
        pkmn = party[pos.effects[PBEffects::FutureSightUserPartyIndex]]
        if pkmn && pkmn.able?
          moveUser = PokeBattle_Battler.new(self,pos.effects[PBEffects::FutureSightUserIndex])
          moveUser.pbInitDummyPokemon(pkmn,pos.effects[PBEffects::FutureSightUserPartyIndex])
        end
      end
      next if !moveUser   # User is fainted
      move = pos.effects[PBEffects::FutureSightMove]
      pbDisplay(_INTL("{1}承受了{2}的攻击！",@battlers[idxPos].pbThis,PBMoves.getName(move)))
      # NOTE: Future Sight failing against the target here doesn't count towards
      #       Stomping Tantrum.
      userLastMoveFailed = moveUser.lastMoveFailed
      @futureSight = true
      moveUser.pbUseMoveSimple(move,idxPos)
      @futureSight = false
      moveUser.lastMoveFailed = userLastMoveFailed
      @battlers[idxPos].pbFaint if @battlers[idxPos].fainted?
      pos.effects[PBEffects::FutureSightCounter]        = 0
      pos.effects[PBEffects::FutureSightMove]           = 0
      pos.effects[PBEffects::FutureSightUserIndex]      = -1
      pos.effects[PBEffects::FutureSightUserPartyIndex] = -1
    end
    # Wish
    @positions.each_with_index do |pos,idxPos|
      next if !pos || pos.effects[PBEffects::Wish]==0
      pos.effects[PBEffects::Wish] -= 1
      next if pos.effects[PBEffects::Wish]>0
      next if !@battlers[idxPos] || !@battlers[idxPos].canHeal?
      wishMaker = pbThisEx(idxPos,pos.effects[PBEffects::WishMaker])
      @battlers[idxPos].pbRecoverHP(pos.effects[PBEffects::WishAmount])
      pbDisplay(_INTL("{1}的祈愿成真了！",wishMaker))
    end
    # Sea of Fire damage (Fire Pledge + Grass Pledge combination)
    curWeather = pbWeather
    for side in 0...2
      next if sides[side].effects[PBEffects::SeaOfFire]==0
      next if curWeather==PBWeather::Rain || curWeather==PBWeather::HeavyRain
      @battle.pbCommonAnimation("SeaOfFire") if side==0
      @battle.pbCommonAnimation("SeaOfFireOpp") if side==1
      priority.each do |b|
        next if b.opposes?(side)
        next if !b.takesIndirectDamage? || b.pbHasType?(:FIRE)
        oldHP = b.hp
        @scene.pbDamageAnimation(b)
        b.pbReduceHP(b.totalhp/8,false)
        pbDisplay(_INTL("{1}被火海伤害了!",b.pbThis))
        b.pbItemHPHealCheck
        b.pbAbilitiesOnDamageTaken(oldHP)
        b.pbFaint if b.fainted?
      end
    end
    # Status-curing effects/abilities and HP-healing items
    priority.each do |b|
      next if b.fainted?
      # Grassy Terrain (healing)
      if @field.terrain==PBBattleTerrains::Grassy && b.affectedByTerrain? && b.canHeal?
        PBDebug.log("[Lingering effect] Grassy Terrain heals #{b.pbThis(true)}")
        b.pbRecoverHP(b.totalhp/16)
        pbDisplay(_INTL("{1}的HP回复了。",b.pbThis))
      end
      # Healer, Hydration, Shed Skin
      BattleHandlers.triggerEORHealingAbility(b.ability,b,self) if b.abilityActive?
      # Black Sludge, Leftovers
      BattleHandlers.triggerEORHealingItem(b.item,b,self) if b.itemActive?
    end
    # 手术
    priority.each do |b|
      next if !b.effects[PBEffects::Operation]
      next if !b.canHeal?
      hpGain = b.totalhp/16
      b.pbRecoverHP(hpGain)
      pbDisplay(_INTL("手术恢复了{1}的HP！",b.pbThis(true)))
    end
    # Aqua Ring
    priority.each do |b|
      next if !b.effects[PBEffects::AquaRing]
      next if !b.canHeal?
      hpGain = b.totalhp/16
      hpGain = (hpGain*1.3).floor if b.hasActiveItem?(:BIGROOT)
      b.pbRecoverHP(hpGain)
      pbDisplay(_INTL("水流环恢复了{1}的HP！",b.pbThis(true)))
    end
    # Ingrain
    priority.each do |b|
      next if !b.effects[PBEffects::Ingrain]
      next if !b.canHeal?
      hpGain = b.totalhp/16
      hpGain = (hpGain*1.3).floor if b.hasActiveItem?(:BIGROOT)
      b.pbRecoverHP(hpGain)
      pbDisplay(_INTL("{1}通过根吸取了营养！",b.pbThis))
    end
    # Leech Seed
    priority.each do |b|
      next if b.effects[PBEffects::LeechSeed]<0
      next if !b.takesIndirectDamage?
      recipient = @battlers[b.effects[PBEffects::LeechSeed]]
      next if !recipient || recipient.fainted?
      oldHP = b.hp
      oldHPRecipient = recipient.hp
      pbCommonAnimation("LeechSeed",recipient,b)
      hpLoss = b.pbReduceHP(b.totalhp/8)
      recipient.pbRecoverHPFromDrain(hpLoss,b,
         _INTL("{1}被寄生种子吸取了营养！",b.pbThis))
      recipient.pbAbilitiesOnDamageTaken(oldHPRecipient) if recipient.hp<oldHPRecipient
      b.pbItemHPHealCheck
      b.pbAbilitiesOnDamageTaken(oldHP)
      b.pbFaint if b.fainted?
      recipient.pbFaint if recipient.fainted?
    end
    # Damage from Hyper Mode (Shadow Pokémon)
    priority.each do |b|
      next if !b.inHyperMode? || @choices[b.index][0]!=:UseMove
      hpLoss = (NEWEST_BATTLE_MECHANICS) ? b.totalhp/16 : b.totalhp/8
      @scene.pbDamageAnimation(b)
      b.pbReduceHP(hpLoss,false)
      pbDisplay(_INTL("因为处于暴走状态\n所以伤害了{1}！",b.pbThis(true)))
      b.pbFaint if b.fainted?
    end
    # Pokémon Legends: Arceus
    # Stone Axe Splinters
    priority.each do |b|
      next if b.effects[PBEffects::StoneAxe] < 0
      b.effects[PBEffects::StoneAxe] -= 1
      next if !b.takesIndirectDamage?
      pbCommonAnimation("StoneAxe",b)
      oldHP = b.hp
      if b.pokemon.battleRank > 2
        b.pbReduceHP(b.totalhp/40)
      else
        b.pbReduceHP(b.totalhp/8)
      end
      pbDisplay(_INTL("{1}因为锋利的碎片\n而损失了HP！",b.pbThis))
      b.pbItemHPHealCheck
      b.pbAbilitiesOnDamageTaken(oldHP)
      b.pbFaint if b.fainted?
    end
    # Ceaseless Edge Splinters
    priority.each do |b|
      next if b.fainted?
      next if b.effects[PBEffects::CeaselessEdge] < 0
      b.effects[PBEffects::CeaselessEdge] -= 1
      next if !b.takesIndirectDamage?
      pbCommonAnimation("CeaselessEdge",b)
      oldHP = b.hp
      if b.pokemon.battleRank > 2
        b.pbReduceHP(b.totalhp/40)
      else
        b.pbReduceHP(b.totalhp/8)
      end
      pbDisplay(_INTL("{1}因为锋利的碎片\n而损失了HP！",b.pbThis))
      b.pbItemHPHealCheck
      b.pbAbilitiesOnDamageTaken(oldHP)
      b.pbFaint if b.fainted?
    end
    # Damage from poisoning
    priority.each do |b|
      next if b.fainted?
      next if b.status!=PBStatuses::POISON
      if b.statusCount>0
        b.effects[PBEffects::Toxic] += 1
        b.effects[PBEffects::Toxic] = 15 if b.effects[PBEffects::Toxic]>15
      end
      if b.hasActiveAbility?(:POISONHEAL)
        if b.canHeal?
          pbCommonAnimation("Poison",b)
          pbShowAbilitySplash(b)
          b.pbRecoverHP(b.totalhp/8)
          if PokeBattle_SceneConstants::USE_ABILITY_SPLASH
            pbDisplay(_INTL("{1}的HP回复了。",b.pbThis))
          else
            pbDisplay(_INTL("{1}的{2}回复了HP。",b.pbThis,b.abilityName))
          end
          pbHideAbilitySplash(b)
        end
      elsif b.takesIndirectDamage?
        oldHP = b.hp
        if b.pokemon.battleRank > 2
          dmg = (b.statusCount==0) ? b.totalhp/40 : b.totalhp*b.effects[PBEffects::Toxic]/160
        else
          dmg = (b.statusCount==0) ? b.totalhp/8 : b.totalhp*b.effects[PBEffects::Toxic]/16
        end
        b.pbContinueStatus { b.pbReduceHP(dmg,false) }
        b.pbItemHPHealCheck
        b.pbAbilitiesOnDamageTaken(oldHP)
        b.pbFaint if b.fainted?
      end
    end
   # Damage from burn
   priority.each do |b|
      next if b.status!=PBStatuses::BURN || !b.takesIndirectDamage?
      oldHP = b.hp
      if b.pokemon.battleRank > 2
        dmg = b.totalhp/40
      else
        dmg = (NEWEST_BATTLE_MECHANICS) ? b.totalhp/16 : b.totalhp/8
      end
      dmg = (dmg/2.0).round if b.hasActiveAbility?(:HEATPROOF)
      b.pbContinueStatus { b.pbReduceHP(dmg,false) }
      b.pbItemHPHealCheck
      b.pbAbilitiesOnDamageTaken(oldHP)
      b.pbFaint if b.fainted?
    end
   # Damage from frostbite (Pokémon Legends: Arceus)
    priority.each do |b|
      next if b.status!=PBStatuses::FROSTBITE || !b.takesIndirectDamage?
      oldHP = b.hp
      if b.pokemon.battleRank > 2
        dmg = b.totalhp/40
      else
        dmg = (NEWEST_BATTLE_MECHANICS) ? b.totalhp/16 : b.totalhp/8
      end
      dmg = (dmg/2.0).round if b.hasActiveAbility?(:MAGMAARMOR)
      b.pbContinueStatus { b.pbReduceHP(dmg,false) }
      b.pbItemHPHealCheck
      b.pbAbilitiesOnDamageTaken(oldHP)
      b.pbFaint if b.fainted?
    end
    # Damage from sleep (Nightmare)
    priority.each do |b|
      b.effects[PBEffects::Nightmare] = false if !b.asleep?
      next if !b.effects[PBEffects::Nightmare] || !b.takesIndirectDamage?
      oldHP = b.hp
      if b.pokemon.battleRank > 2
        b.pbReduceHP(b.totalhp/40)
      else
        b.pbReduceHP(b.totalhp/4)
      end
      pbDisplay(_INTL("{1}被囚禁在恶梦之中！",b.pbThis))
      b.pbItemHPHealCheck
      b.pbAbilitiesOnDamageTaken(oldHP)
      b.pbFaint if b.fainted?
    end
    # Curse
    priority.each do |b|
      next if !b.effects[PBEffects::Curse] || !b.takesIndirectDamage?
      oldHP = b.hp
      if b.pokemon.battleRank > 2
        b.pbReduceHP(b.totalhp/40)
      else
        b.pbReduceHP(b.totalhp/4)
      end
      pbDisplay(_INTL("{1}遭受着诅咒的折磨！",b.pbThis))
      b.pbItemHPHealCheck
      b.pbAbilitiesOnDamageTaken(oldHP)
      b.pbFaint if b.fainted?
    end
    # Octolock
    priority.each do |b|
      next if b.fainted? || !b.effects[PBEffects::Octolock]
      octouser = @battlers[b.effects[PBEffects::OctolockUser]]
      if b.pbCanLowerStatStage?(PBStats::DEFENSE,octouser)
        b.pbLowerStatStage(PBStats::DEFENSE,1,octouser,true,false,true)
      end
      if b.pbCanLowerStatStage?(PBStats::SPDEF,octouser)
        b.pbLowerStatStage(PBStats::SPDEF,1,octouser,true,false,true)
      end
    end
    # Trapping attacks (Bind/Clamp/Fire Spin/Magma Storm/Sand Tomb/Whirlpool/Wrap)
    priority.each do |b|
      next if b.fainted? || b.effects[PBEffects::Trapping]==0
      b.effects[PBEffects::Trapping] -= 1
      moveName = PBMoves.getName(b.effects[PBEffects::TrappingMove])
      if b.effects[PBEffects::Trapping]==0
        pbDisplay(_INTL("{1}从{2}中脱身了！",b.pbThis,moveName))
      else
        trappingMove = b.effects[PBEffects::TrappingMove]
        if isConst?(trappingMove,PBMoves,:BIND);           pbCommonAnimation("Bind",b)
        elsif isConst?(trappingMove,PBMoves,:CLAMP);       pbCommonAnimation("Clamp",b)
        elsif isConst?(trappingMove,PBMoves,:FIRESPIN);    pbCommonAnimation("FireSpin",b)
        elsif isConst?(trappingMove,PBMoves,:MAGMASTORM);  pbCommonAnimation("MagmaStorm",b)
        elsif isConst?(trappingMove,PBMoves,:SANDTOMB);    pbCommonAnimation("SandTomb",b)
        elsif isConst?(trappingMove,PBMoves,:WRAP);        pbCommonAnimation("Wrap",b)
        elsif isConst?(trappingMove,PBMoves,:INFESTATION); pbCommonAnimation("Infestation",b)
        elsif isConst?(trappingMove,PBMoves,:SNAPTRAP); pbCommonAnimation("SnapTrap",b)
        elsif isConst?(trappingMove,PBMoves,:THUNDERCAGE); pbCommonAnimation("ThunderCage",b)
        else;                                              pbCommonAnimation("Wrap",b)
        end
        if b.takesIndirectDamage?
          hpLoss = (NEWEST_BATTLE_MECHANICS) ? b.totalhp/8 : b.totalhp/16
          if @battlers[b.effects[PBEffects::TrappingUser]].hasActiveItem?(:BINDINGBAND)
            hpLoss = (NEWEST_BATTLE_MECHANICS) ? b.totalhp/6 : b.totalhp/8
          end
          @scene.pbDamageAnimation(b)
          hpLoss /= 5 if b.pokemon.battleRank > 2
          b.pbReduceHP(hpLoss,false)
          pbDisplay(_INTL("{1}受到了来自{2}的伤害！",b.pbThis,moveName))
          b.pbItemHPHealCheck
          # NOTE: No need to call pbAbilitiesOnDamageTaken as b can't switch out.
          b.pbFaint if b.fainted?
        end
      end
    end
    # Taunt
    pbEORCountDownBattlerEffect(priority,PBEffects::Taunt) { |battler|
      pbDisplay(_INTL("{1}的挑衅无效了！",battler.pbThis))
    }
    # Encore
    priority.each do |b|
      next if b.fainted? || b.effects[PBEffects::Encore]==0
      idxEncoreMove = b.pbEncoredMoveIndex
      if idxEncoreMove>=0
        b.effects[PBEffects::Encore] -= 1
        if b.effects[PBEffects::Encore]==0 || b.moves[idxEncoreMove].pp==0
          b.effects[PBEffects::Encore] = 0
          pbDisplay(_INTL("{1}的再来一次状态解除了！",b.pbThis))
        end
      else
        PBDebug.log("[End of effect] #{b.pbThis}'s encore ended (encored move no longer known)")
        b.effects[PBEffects::Encore]     = 0
        b.effects[PBEffects::EncoreMove] = 0
      end
    end
    # Disable/Cursed Body
    pbEORCountDownBattlerEffect(priority,PBEffects::Disable) { |battler|
      battler.effects[PBEffects::DisableMove] = 0
      pbDisplay(_INTL("{1}不再被封印了！",battler.pbThis))
    }
    # Magnet Rise
    pbEORCountDownBattlerEffect(priority,PBEffects::MagnetRise) { |battler|
      pbDisplay(_INTL("{1}的电磁力消失了！",battler.pbThis))
    }
    # Telekinesis
    pbEORCountDownBattlerEffect(priority,PBEffects::Telekinesis) { |battler|
      pbDisplay(_INTL("{1}从念力中逃脱了！",battler.pbThis))
    }
    # Heal Block
    pbEORCountDownBattlerEffect(priority,PBEffects::HealBlock) { |battler|
      pbDisplay(_INTL("{1}的回复封印解除了！",battler.pbThis))
    }
    # Embargo
    pbEORCountDownBattlerEffect(priority,PBEffects::Embargo) { |battler|
      pbDisplay(_INTL("{1}可以使用道具了！",battler.pbThis))
      battler.pbItemTerrainStatBoostCheck
    }
    # Yawn
    pbEORCountDownBattlerEffect(priority,PBEffects::Yawn) { |battler|
      if battler.pbCanSleepYawn?
        PBDebug.log("[Lingering effect] #{battler.pbThis} fell asleep because of Yawn")
        battler.pbSleep
      end
    }
    # Perish Song
    perishSongUsers = []
    priority.each do |b|
      next if b.fainted? || b.effects[PBEffects::PerishSong]==0
      b.effects[PBEffects::PerishSong] -= 1
      pbDisplay(_INTL("距离{1}灭亡还剩下{2}回合！",b.pbThis,b.effects[PBEffects::PerishSong]))
      if b.effects[PBEffects::PerishSong]==0
        perishSongUsers.push(b.effects[PBEffects::PerishSongUser])
        b.pbReduceHP(b.hp)
      end
      b.pbItemHPHealCheck
      b.pbFaint if b.fainted?
    end
    if perishSongUsers.length>0
      # If all remaining Pokemon fainted by a Perish Song triggered by a single side
      if (perishSongUsers.find_all { |idxBattler| opposes?(idxBattler) }.length==perishSongUsers.length) ||
         (perishSongUsers.find_all { |idxBattler| !opposes?(idxBattler) }.length==perishSongUsers.length)
        pbJudgeCheckpoint(@battlers[perishSongUsers[0]])
      end
    end
      # 珠泪哀歌
    priority.each do |b|
      next if !b.takesIndirectDamage? || b.effects[PBEffects::Curse] ||
              !b.effects[PBEffects::Tearalament]
      oldHP = b.hp
      if b.pokemon.battleRank > 2
        b.pbReduceHP(b.totalhp/80)
      else
        b.pbReduceHP(b.totalhp/8)
      end
      pbDisplay(_INTL("{1}被恐惧与哀伤折磨！",b.pbThis))
      b.pbItemHPHealCheck
      b.pbAbilitiesOnDamageTaken(oldHP)
      b.pbFaint if b.fainted?
    end  
    # 盐腌
    priority.each do |battler|
      next if !battler.effects[PBEffects::SaltCure] || !battler.takesIndirectDamage?
      pbCommonAnimation("SaltCure", battler)
      fraction = (battler.pbHasType?(:STEEL) || battler.pbHasType?(:WATER)) ? 4 : 8
      fraction *= 5 if battler.pokemon.battleRank > 2
      battler.pbTakeEffectDamage(battler.totalhp / fraction) { |hp_lost|
        pbDisplay(_INTL("{1}被盐腌伤害了！", battler.pbThis))
      }
    end
    
    # 咒钉
    priority.each do |b|
      next if b.fainted? || b.effects[PBEffects::CurseNail]==0
      b.effects[PBEffects::CurseNail] -= 1
      pbDisplay(_INTL("距离{1}的咒钉消失\n还剩下{2}回合！",b.pbThis,b.effects[PBEffects::CurseNail]))
    end
    
    # 地魔之剑/海魔之雨回合
    priority.each do |b|
      next if b.fainted? || b.effects[PBEffects::PoisonVulnerability]==0
      b.effects[PBEffects::PoisonVulnerability] -= 1
    end
    priority.each do |b|
      next if b.fainted? || b.effects[PBEffects::IceVulnerability]==0
      b.effects[PBEffects::IceVulnerability] -= 1
    end
    # 复活
    priority.each do |b|
      next if b.fainted?
      b.setCanRebirth(false)
    end
    # Check for end of battle
    if @decision>0
      pbGainExp
      return
    end
    for side in 0...2
      # Reflect
      pbEORCountDownSideEffect(side,PBEffects::Reflect,
         _INTL("{1}的反射盾消失了！",@battlers[side].pbTeam))
      # Light Screen
      pbEORCountDownSideEffect(side,PBEffects::LightScreen,
         _INTL("{1}的光墙消失了！",@battlers[side].pbTeam))
      # Safeguard
      pbEORCountDownSideEffect(side,PBEffects::Safeguard,
         _INTL("{1}不再受神秘守护的保护了！",@battlers[side].pbTeam))
      # Mist
      pbEORCountDownSideEffect(side,PBEffects::Mist,
         _INTL("{1}不再受白雾的保护了！",@battlers[side].pbTeam))
      # Tailwind
      pbEORCountDownSideEffect(side,PBEffects::Tailwind,
         _INTL("{1}的顺风停止了！",@battlers[side].pbTeam))
      # Lucky Chant
      pbEORCountDownSideEffect(side,PBEffects::LuckyChant,
         _INTL("{1}的幸运咒语消失了！",@battlers[side].pbTeam))
      # Pledge Rainbow
      pbEORCountDownSideEffect(side,PBEffects::Rainbow,
         _INTL("{1}一方的彩虹消失了！",@battlers[side].pbTeam(true)))
      # Pledge Sea of Fire
      pbEORCountDownSideEffect(side,PBEffects::SeaOfFire,
         _INTL("{1}周围的火海消失了！",@battlers[side].pbTeam(true)))
      # Pledge Swamp
      pbEORCountDownSideEffect(side,PBEffects::Swamp,
         _INTL("{1}周围的沼泽消失了！",@battlers[side].pbTeam(true)))
      # Aurora Veil
      pbEORCountDownSideEffect(side,PBEffects::AuroraVeil,
         _INTL("{1}的极光幕消失了！",@battlers[side].pbTeam(true)))
           # 妄之歌
       pbEORCountDownSideEffect(side,PBEffects::DelusionSong,
     _INTL("妖精的歌声停止了！"))
    end
    # Trick Room
    pbEORCountDownFieldEffect(PBEffects::TrickRoom,
       _INTL("扭曲的时空恢复正常了！"))
    # Gravity
    pbEORCountDownFieldEffect(PBEffects::Gravity,
       _INTL("重力恢复正常了！"))
    # Water Sport
    pbEORCountDownFieldEffect(PBEffects::WaterSportField,
       _INTL("玩水的效果消失了。"))
    # Mud Sport
    pbEORCountDownFieldEffect(PBEffects::MudSportField,
       _INTL("玩泥巴的效果消失了。"))
    # Wonder Room
    pbEORCountDownFieldEffect(PBEffects::WonderRoom,
       _INTL("奇妙空间消失了！\n防御与特防恢复正常了！"))
    # Magic Room
    pbEORCountDownFieldEffect(PBEffects::MagicRoom,
       _INTL("魔法空间消失了！\n携带道具的效果恢复正常了！"))
    # End of terrains
    pbEORTerrain
    priority.each do |b|
      next if b.fainted?
      # Hyper Mode (Shadow Pokémon)
      if b.inHyperMode?
        if pbRandom(100)<10
          b.pokemon.hypermode = false
          b.pokemon.adjustHeart(-50)
          pbDisplay(_INTL("{1}清醒过来了！",b.pbThis))
        else
          pbDisplay(_INTL("{1}处于暴走状态！",b.pbThis))
        end
      end
      # Uproar
      if b.effects[PBEffects::Uproar]>0
        b.effects[PBEffects::Uproar] -= 1
        if b.effects[PBEffects::Uproar]==0
          pbDisplay(_INTL("{1}冷静下来了。",b.pbThis))
        else
          pbDisplay(_INTL("{1}正在制造噪音！",b.pbThis))
        end
      end
      # Slow Start's end message
      if b.effects[PBEffects::SlowStart]>0
        b.effects[PBEffects::SlowStart] -= 1
        if b.effects[PBEffects::SlowStart]==0
          pbDisplay(_INTL("{1}聚集了所有的力量！",b.pbThis))
        end
      end
      # Bad Dreams, Moody, Speed Boost
      BattleHandlers.triggerEOREffectAbility(b.ability,b,self) if b.abilityActive?
      # Flame Orb, Sticky Barb, Toxic Orb
      BattleHandlers.triggerEOREffectItem(b.item,b,self) if b.itemActive?
      # Harvest, Pickup
      BattleHandlers.triggerEORGainItemAbility(b.ability,b,self) if b.abilityActive?
    end
    pbGainExp
    return if @decision>0
    # Form checks
    priority.each { |b| b.pbCheckForm(true) }
    # Switch Pokémon in if possible
    pbEORSwitch
    return if @decision>0
    # In battles with at least one side of size 3+, move battlers around if none
    # are near to any foes
    pbEORShiftDistantBattlers
    # Try to make Trace work, check for end of primordial weather
    priority.each { |b| b.pbContinualAbilityChecks }
    # Reset/count down battler-specific effects (no messages)
    eachBattler do |b|
      b.effects[PBEffects::BanefulBunker]    = false
      b.effects[PBEffects::Charge]           -= 1 if b.effects[PBEffects::Charge]>0
      b.effects[PBEffects::Counter]          = -1
      b.effects[PBEffects::CounterTarget]    = -1
      b.effects[PBEffects::Electrify]        = false
      b.effects[PBEffects::Endure]           = false
      b.effects[PBEffects::FirstPledge]      = 0
      b.effects[PBEffects::Flinch]           = false
      b.effects[PBEffects::FocusPunch]       = false
      b.effects[PBEffects::FollowMe]         = 0
      b.effects[PBEffects::HelpingHand]      = false
      b.effects[PBEffects::HyperBeam]        -= 1 if b.effects[PBEffects::HyperBeam]>0
      b.effects[PBEffects::KingsShield]      = false
      b.effects[PBEffects::LaserFocus]       -= 1 if b.effects[PBEffects::LaserFocus]>0
      if b.effects[PBEffects::LockOn]>0   # Also Mind Reader
        b.effects[PBEffects::LockOn]         -= 1
        b.effects[PBEffects::LockOnPos]      = -1 if b.effects[PBEffects::LockOn]==0
      end
      b.effects[PBEffects::MagicBounce]      = false
      b.effects[PBEffects::MagicCoat]        = false
      b.effects[PBEffects::MirrorCoat]       = -1
      b.effects[PBEffects::MirrorCoatTarget] = -1
      b.effects[PBEffects::Powder]           = false
      b.effects[PBEffects::Prankster]        = false
      b.effects[PBEffects::PriorityAbility]  = false
      b.effects[PBEffects::PriorityItem]     = false
      b.effects[PBEffects::Protect]          = false
      b.effects[PBEffects::RagePowder]       = false
      b.effects[PBEffects::Roost]            = false
      b.effects[PBEffects::Snatch]           = 0
      b.effects[PBEffects::SpikyShield]      = false
      b.effects[PBEffects::Spotlight]        = 0
      b.effects[PBEffects::ThroatChop]       -= 1 if b.effects[PBEffects::ThroatChop]>0
      b.effects[PBEffects::BurningJealousy]          = false
      b.effects[PBEffects::LashOut]          = false
      b.effects[PBEffects::Obstruct]         = false
      b.lastHPLost                           = 0
      b.lastHPLostFromFoe                    = 0
      b.tookDamage                           = false
      b.tookPhysicalHit                      = false
      b.statsRaisedThisRound                 = false
      b.statsLoweredThisRound                = false
      b.lastRoundMoveFailed                  = b.lastMoveFailed
      b.lastAttacker.clear
      b.lastFoeAttacker.clear
    end
    # Reset/count down side-specific effects (no messages)
    for side in 0...2
      @sides[side].effects[PBEffects::CraftyShield]         = false
      if !@sides[side].effects[PBEffects::EchoedVoiceUsed]
        @sides[side].effects[PBEffects::EchoedVoiceCounter] = 0
      end
      @sides[side].effects[PBEffects::EchoedVoiceUsed]      = false
      @sides[side].effects[PBEffects::MatBlock]             = false
      @sides[side].effects[PBEffects::QuickGuard]           = false
      @sides[side].effects[PBEffects::Round]                = false
      @sides[side].effects[PBEffects::WideGuard]            = false
    end
    # Reset/count down field-specific effects (no messages)
    @field.effects[PBEffects::IonDeluge]   = false
    @field.effects[PBEffects::FairyLock]   -= 1 if @field.effects[PBEffects::FairyLock]>0
    @field.effects[PBEffects::FusionBolt]  = false
    @field.effects[PBEffects::FusionFlare] = false
    @field.effects[PBEffects::BurningBulwark] = false
    
	  allBattlers.each_with_index do |battler, i|
      battler.effects[PBEffects::BurningBulwark] = false
      battler.effects[PBEffects::Charge]   += 1 if battler.effects[PBEffects::Charge]     > 0
      battler.effects[PBEffects::GlaiveRush] -= 1 if battler.effects[PBEffects::GlaiveRush] > 0
    end
   # Neutralizing Gas
   pbCheckNeutralizingGas
  
    @endOfRound = false
  end
  
  
  def pbCheckNeutralizingGas(battler=nil)
    # Battler = the battler to switch out. 
  # Should be specified when called from pbAttackPhaseSwitch
  # Should be nil when called from pbEndOfRoundPhase
    return if !@field.effects[PBEffects::NeutralizingGas]
    return if battler &&
              (!isConst?(battler.ability,PBAbilities,:NEUTRALIZINGGAS) || 
              !isConst?(battler.ability,PBAbilities,:STEELDYNASTY) || 
              battler.effects[PBEffects::GastroAcid])
    hasabil=false
    eachBattler {|b|
      next if !b || b.fainted?
    next if battler && b.index == battler.index 
    # if specified, the battler will switch out, so don't consider it.
      # neutralizing gas can be blocked with gastro acid, ending the effect.
      if (isConst?(b.ability,PBAbilities,:NEUTRALIZINGGAS) ||
          isConst?(b.ability,PBAbilities,:STEELDYNASTY)) &&
         !b.effects[PBEffects::GastroAcid]
      hasabil=true; break
      end
    }
    if !hasabil
      @field.effects[PBEffects::NeutralizingGas] = false
      pbPriority(true).each { |b| 
      next if battler && b.index == battler.index
      b.pbEffectsOnSwitchIn
    }
    end
  end 
end
