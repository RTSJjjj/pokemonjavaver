class PokeBattle_Battle
  class BattleAbortedException < Exception; end

  def pbAbort
    raise BattleAbortedException.new("Battle aborted")
  end

  #=============================================================================
  # Makes sure all Pokémon exist that need to. Alter the type of battle if
  # necessary. Will never try to create battler positions, only delete them
  # (except for wild Pokémon whose number of positions are fixed). Reduces the
  # size of each side by 1 and tries again. If the side sizes are uneven, only
  # the larger side's size will be reduced by 1 each time, until both sides are
  # an equal size (then both sides will be reduced equally).
  #=============================================================================
  def pbEnsureParticipants
    # Prevent battles larger than 2v2 if both sides have multiple trainers
    # NOTE: This is necessary to ensure that battlers can never become unable to
    #       hit each other due to being too far away. In such situations,
    #       battlers will move to the centre position at the end of a round, but
    #       because they cannot move into a position owned by a different
    #       trainer, it's possible that battlers will be unable to move close
    #       enough to hit each other if there are multiple trainers on each
    #       side.
    if trainerBattle? && (@sideSizes[0]>2 || @sideSizes[1]>2) &&
       @player.length>1 && @opponent.length>1
      raise _INTL("不能在双方都有多名训练家的情况\n下进行大于2v2的战斗！")
    end
    # Find out how many Pokémon each trainer has
    side1counts = pbAbleTeamCounts(0)
    side2counts = pbAbleTeamCounts(1)
    # Change the size of the battle depending on how many wild Pokémon there are
    if wildBattle? && side2counts[0]!=@sideSizes[1]
      if @sideSizes[0]==@sideSizes[1]
        # Even number of battlers per side, change both equally
        @sideSizes = [side2counts[0],side2counts[0]]
      else
        # Uneven number of battlers per side, just change wild side's size
        @sideSizes[1] = side2counts[0]
      end
    end
    # Check if battle is possible, including changing the number of battlers per
    # side if necessary
    loop do
      needsChanging = false
      for side in 0...2   # Each side in turn
        next if side==1 && wildBattle?   # Wild side's size already checked above
        sideCounts = (side==0) ? side1counts : side2counts
        requireds = []
        # Find out how many Pokémon each trainer on side needs to have
        for i in 0...@sideSizes[side]
          idxTrainer = pbGetOwnerIndexFromBattlerIndex(i*2+side)
          requireds[idxTrainer] = 0 if requireds[idxTrainer].nil?
          requireds[idxTrainer] += 1
        end
        # Compare the have values with the need values
        if requireds.length>sideCounts.length
          raise _INTL("错误：\ndef pbGetOwnerIndexFromBattlerIndex\n为战斗类型{2}v{3}，训练家{4}v{5}\n提供了无效的所有者索引！\n{1}",
             requireds.length-1,@sideSizes[0],@sideSizes[1],side1counts.length,side2counts.length)
        end
        sideCounts.each_with_index do |_count,i|
          if !requireds[i] || requireds[i]==0
            raise _INTL("玩家一方{1}没有战斗位置\n宝可梦无法去尝试{2}V{3}战斗！",
               i+1,@sideSizes[0],@sideSizes[1]) if side==0
            raise _INTL("对手{1}没有战斗位置宝可\n梦无法去尝试{2}V{3}战斗！",
               i+1,@sideSizes[0],@sideSizes[1]) if side==1
          end
          next if requireds[i]<=sideCounts[i]   # Trainer has enough Pokémon to fill their positions
          if requireds[i]==1
            raise _INTL("玩家一方{1}没有可用的宝可梦！",i+1) if side==0
            raise _INTL("对方{1}没有可用的宝可梦！",i+1) if side==1
          end
          # Not enough Pokémon, try lowering the number of battler positions
          needsChanging = true
          break
        end
        break if needsChanging
      end
      break if !needsChanging
      # Reduce one or both side's sizes by 1 and try again
      if wildBattle?
        PBDebug.log("#{@sideSizes[0]}v#{@sideSizes[1]} battle isn't possible " +
                    "(#{side1counts} player-side teams versus #{side2counts[0]} wild Pokémon)")
        newSize = @sideSizes[0]-1
      else
        PBDebug.log("#{@sideSizes[0]}v#{@sideSizes[1]} battle isn't possible " +
                    "(#{side1counts} player-side teams versus #{side2counts} opposing teams)")
        newSize = @sideSizes.max-1
      end
      if newSize==0
        raise _INTL("不要再降低任何一方的规模\n不然战斗是无法发生的！")
      end
      for side in 0...2
        next if side==1 && wildBattle?   # Wild Pokémon's side size is fixed
        next if @sideSizes[side]==1 || newSize>@sideSizes[side]
        @sideSizes[side] = newSize
      end
      PBDebug.log("Trying #{@sideSizes[0]}v#{@sideSizes[1]} battle instead")
    end
  end

  #=============================================================================
  # Set up all battlers
  #=============================================================================
  def pbCreateBattler(idxBattler,pkmn,idxParty)
    if !@battlers[idxBattler].nil?
      raise _INTL("战斗索引{1}已经存在了！",idxBattler)
    end
    @battlers[idxBattler] = PokeBattle_Battler.new(self,idxBattler)
    @positions[idxBattler] = PokeBattle_ActivePosition.new
    pbClearChoice(idxBattler)
    @successStates[idxBattler] = PokeBattle_SuccessState.new
    @battlers[idxBattler].pbInitialize(pkmn,idxParty)
  end

  def pbSetUpSides
    ret = [[],[]]
    for side in 0...2
      # Set up wild Pokémon
      if side==1 && wildBattle?
        pbParty(1).each_with_index do |pkmn,idxPkmn|
          pbCreateBattler(2*idxPkmn+side,pkmn,idxPkmn)
          # Changes the Pokémon's form upon entering battle (if it should)
          @peer.pbOnEnteringBattle(self,pkmn,true)
          pbSetSeen(@battlers[2*idxPkmn+side])
          @usedInBattle[side][idxPkmn] = true
        end
        next
      end
      # Set up player's Pokémon and trainers' Pokémon
      trainer = (side==0) ? @player : @opponent
      requireds = []
      # Find out how many Pokémon each trainer on side needs to have
      for i in 0...@sideSizes[side]
        idxTrainer = pbGetOwnerIndexFromBattlerIndex(i*2+side)
        requireds[idxTrainer] = 0 if requireds[idxTrainer].nil?
        requireds[idxTrainer] += 1
      end
      #训练家战斗难度加强
      if side==1 && !wildBattle?
        #动态等级
        if $game_variables[100] > 0
          level = 1
          case $game_variables[100]
          when 2    #平均等级
            #累加自己队伍等级
            level_sum = 0
            pbParty(0).each { |pkmn| level_sum += pkmn.level }
            #计算平均等级
            level = [1, level_sum / pbParty(0).size].max
          when 1    #最高等级
            #获取自己队伍最高等级
            pbParty(0).each { |pkmn| level = pkmn.level if level < pkmn.level}
          end
          #修改对面等级
          pbParty(1).each do |pkmn|
            next if pkmn.level==1
            pkmn.level = level if pkmn.level < level
            pkmn.calcStats
          end
        end
        #打完一道馆后自动设置努力值
      if $Trainer.badges[0]
        pbParty(1).each do |pkmn|
          lev = (pkmn.level * 1.5).round
          if pkmn.ev == [0,0,0,0,0,0] || pkmn.ev == [85,85,85,85,85,85] ||
             pkmn.ev == [lev,lev,lev,lev,lev,lev]
            pkmn = changeEVandNature(pkmn, false)
          end
        end
      end
    end
      # For each trainer in turn, find the needed number of Pokémon for them to
      # send out, and initialize them
      battlerNumber = 0
      trainer.each_with_index do |_t,idxTrainer|
        ret[side][idxTrainer] = []
        eachInTeam(side,idxTrainer) do |pkmn,idxPkmn|
          next if !pkmn.able?
          idxBattler = 2*battlerNumber+side
          pbCreateBattler(idxBattler,pkmn,idxPkmn)
          ret[side][idxTrainer].push(idxBattler)
          battlerNumber += 1
          break if ret[side][idxTrainer].length>=requireds[idxTrainer]
        end
      end
    end
    return ret
  end

  #=============================================================================
  # Send out all battlers at the start of battle
  #=============================================================================
  def pbStartBattleSendOut(sendOuts)
    # "Want to battle" messages
    if wildBattle?
      foeParty = pbParty(1)
      case foeParty.length
      when 1
        if foeParty[0].battleRank > 1
        prefix = foeParty[0].battleRank > 2 ? "强大的" : "特殊的"
        if $game_switches[196]
            pbDisplayPaused(_INTL("哦！{1}{2}出现了！\n无论如何都是无法捕捉的！",prefix,foeParty[0].name))
          else
            pbDisplayPaused(_INTL("哦！{1}{2}出现了！\n看来不打倒它是无法捕捉的！",prefix,foeParty[0].name))
          end
        else
          pbDisplayPaused(_INTL("哦！一只野生的{1}出现了！",foeParty[0].name))
        end
      when 2
        pbDisplayPaused(_INTL("野生的{1}和{2}\n出现了！",foeParty[0].name,
           foeParty[1].name))
      when 3
        pbDisplayPaused(_INTL("野生的{1}、{2}、{3}\n出现了！",foeParty[0].name,
           foeParty[1].name,foeParty[2].name))
      end
    else   # Trainer battle
      case @opponent.length
      when 1
        pbDisplayPaused(_INTL("{1}\n向你发起挑战！",@opponent[0].fullname))
      when 2
        pbDisplayPaused(_INTL("{1}、{2}\n向你发起挑战！",@opponent[0].fullname,
           @opponent[1].fullname))
      when 3
        pbDisplayPaused(_INTL("{1}、{2}、{3}\n向你发起挑战!",
           @opponent[0].fullname,@opponent[1].fullname,@opponent[2].fullname))
      end
    end
    # Send out Pokémon (opposing trainers first)
    for side in [1,0]
      next if side==1 && wildBattle?
      msg = ""
      toSendOut = []
      trainers = (side==0) ? @player : @opponent
      # Opposing trainers and partner trainers's messages about sending out Pokémon
      trainers.each_with_index do |t,i|
        next if side==0 && i==0   # The player's message is shown last
        msg += "\r\n" if msg.length>0
        sent = sendOuts[side][i]
        case sent.length
        when 1
          msg += _INTL("{1}派出了\n{2}！",t.fullname,@battlers[sent[0]].name)
        when 2
          msg += _INTL("{1}派出了\n{2}和{3}！",t.fullname,
             @battlers[sent[0]].name,@battlers[sent[1]].name)
        when 3
          msg += _INTL("{1}派出了\n{2}、 {3}和{4}！",t.fullname,
             @battlers[sent[0]].name,@battlers[sent[1]].name,@battlers[sent[2]].name)
        end
        toSendOut.concat(sent)
      end
      # The player's message about sending out Pokémon
      if side==0
        msg += "\r\n" if msg.length>0
        sent = sendOuts[side][0]
        case sent.length
        when 1
          msg += _INTL("去吧！\n{1}！",@battlers[sent[0]].name)
        when 2
          msg += _INTL("去吧！\n{1}和{2}！",@battlers[sent[0]].name,@battlers[sent[1]].name)
        when 3
          msg += _INTL("去吧！\n{1}、{2}和{3}！",@battlers[sent[0]].name,
             @battlers[sent[1]].name,@battlers[sent[2]].name)
        end
        toSendOut.concat(sent)
      end
      pbDisplayBrief(msg) if msg.length>0
      # The actual sending out of Pokémon
      animSendOuts = []
      toSendOut.each do |idxBattler|
        animSendOuts.push([idxBattler,@battlers[idxBattler].pokemon])
      end
      pbSendOut(animSendOuts,true)
    end
  end
  #=============================================================================
  # Start a battle
  #=============================================================================
  def pbStartBattle
    # Set spinning to false if a battle is started. Prevents evolution by illegal means
    $PokemonTemp.clockwiseSpin = false
    $PokemonTemp.antiClockwiseSpin = false
    PBDebug.log("")
    PBDebug.log("******************************************")
    logMsg = "[Started battle] "
    if @sideSizes[0]==1 && @sideSizes[1]==1
      logMsg += "Single "
    elsif @sideSizes[0]==2 && @sideSizes[1]==2
      logMsg += "Double "
    elsif @sideSizes[0]==3 && @sideSizes[1]==3
      logMsg += "Triple "
    else
      logMsg += "#{@sideSizes[0]}v#{@sideSizes[1]} "
    end
    logMsg += "wild " if wildBattle?
    logMsg += "trainer " if trainerBattle?
    logMsg += "battle (#{@player.length} trainer(s) vs. "
    logMsg += "#{pbParty(1).length} wild Pokémon)" if wildBattle?
    logMsg += "#{@opponent.length} trainer(s))" if trainerBattle?
    PBDebug.log(logMsg)
    pbEnsureParticipants
    #检测队伍
    pbParty(0).each { |pokemon| 
      next if !pokemon
      exit if pokemon.level>200
    }
    #敌方队伍
    pbParty(1).each { |pokemon| 
      next if !pokemon
    }
    begin
      pbStartBattleCore
    rescue BattleAbortedException
      @decision = 0
      @scene.pbEndBattle(@decision)
    end
    return @decision
  end

  def pbStartBattleCore
    # Set up the battlers on each side
    sendOuts = pbSetUpSides
    # Create all the sprites and play the battle intro animation
    @scene.pbStartBattle(self)
    # Show trainers on both sides sending out Pokémon
    pbStartBattleSendOut(sendOuts)
    # Weather announcement
    pbCommonAnimation(PBWeather.animationName(@field.weather))
    case @field.weather
    when PBWeather::Sun;         pbDisplay(_INTL("阳光很强烈。"))
    when PBWeather::Rain;        pbDisplay(_INTL("正在下雨。"))
    when PBWeather::Sandstorm;   pbDisplay(_INTL("沙暴正在肆虐。"))
    when PBWeather::Hail;        pbDisplay(_INTL("冰雹正在下坠。"))
    when PBWeather::Snow;        pbDisplay(_INTL("雪花正在飘落。"))
    when PBWeather::HarshSun;    pbDisplay(_INTL("阳光极其强烈。"))
    when PBWeather::HeavyRain;   pbDisplay(_INTL("大雨倾盆而下。"))
    when PBWeather::StrongWinds; pbDisplay(_INTL("狂风呼啸。"))
    when PBWeather::ShadowSky;   pbDisplay(_INTL("天空布满阴影。"))
    when PBWeather::Fog;         pbDisplay(_INTL("场上云雾缭绕..."))
    end
    # Terrain announcement
    pbCommonAnimation(PBBattleTerrains.animationName(@field.terrain))
    case @field.terrain
    when PBBattleTerrains::Electric
      pbDisplay(_INTL("电流在场上肆虐！"))
    when PBBattleTerrains::Grassy
      pbDisplay(_INTL("青草覆盖着四周！"))
    when PBBattleTerrains::Misty
      pbDisplay(_INTL("薄雾在场上盘旋！"))
    when PBBattleTerrains::Psychic
      pbDisplay(_INTL("周围极为瑰异！"))
    end
    # Abilities upon entering battle
    pbOnActiveAll
    # Main battle loop
    pbBattleLoop
  end

  #=============================================================================
  # Main battle loop
  #=============================================================================
  def pbBattleLoop
    @turnCount = 0
    loop do   # Now begin the battle loop
      PBDebug.log("")
      PBDebug.log("***Round #{@turnCount+1}***")
      if @debug && @turnCount>=100
        @decision = pbDecisionOnTime
        PBDebug.log("")
        PBDebug.log("***Undecided after 100 rounds, aborting***")
        pbAbort
        break
      end
      PBDebug.log("")
      # BOSS恢复异常状态和清强化
      PBDebug.logonerr { pbBossBuffPhase }
      # Command phase
      PBDebug.logonerr { pbCommandPhase }
      break if @decision>0
      # Attack phase
      PBDebug.logonerr { pbAttackPhase }
      break if @decision>0
      # End of round phase
      PBDebug.logonerr { pbEndOfRoundPhase }
      break if @decision>0
      @turnCount += 1
    end
    pbEndOfBattle
  end

  #=============================================================================
  # End of battle
  #=============================================================================
  def pbGainMoney
    return if !@internalBattle || !@moneyGain
    # Money rewarded from opposing trainers
    if trainerBattle?
      tMoney = 0
       @opponent.each_with_index do |t,i|
        tMoney += pbMaxLevelInTeam(1,i)*t.moneyEarned
      end
      tMoney *= 2 if @field.effects[PBEffects::AmuletCoin]
      tMoney *= 2 if @field.effects[PBEffects::HappyHour]
      oldMoney = pbPlayer.money
      pbPlayer.money += tMoney
      moneyGained = pbPlayer.money-oldMoney
      if moneyGained>0
        pbDisplayPaused(_INTL("你赢得了${1}！",moneyGained.to_s_formatted))
      end
    end
    # Pick up money scattered by Pay Day
    if @field.effects[PBEffects::PayDay]>0
      @field.effects[PBEffects::PayDay] *= 2 if @field.effects[PBEffects::AmuletCoin]
      @field.effects[PBEffects::PayDay] *= 2 if @field.effects[PBEffects::HappyHour]
      oldMoney = pbPlayer.money
      pbPlayer.money += @field.effects[PBEffects::PayDay]
      moneyGained = pbPlayer.money-oldMoney
      if moneyGained>0
        pbDisplayPaused(_INTL("你捡到了${1}！",moneyGained.to_s_formatted))
      end
    end
  end

  def pbLoseMoney
    return if !@internalBattle || !@moneyGain
    return if $game_switches[NO_MONEY_LOSS]
    maxLevel = pbMaxLevelInTeam(0,0)   # Player's Pokémon only, not partner's
    multiplier = [8,16,24,36,48,64,80,100,120]
    idxMultiplier = [pbPlayer.numbadges,multiplier.length-1].min
    tMoney = maxLevel*multiplier[idxMultiplier]
    tMoney = pbPlayer.money if tMoney>pbPlayer.money
    oldMoney = pbPlayer.money
    pbPlayer.money -= tMoney
    moneyLost = oldMoney-pbPlayer.money
    if moneyLost>0
      if trainerBattle?
        pbDisplayPaused(_INTL("你给了获胜者${1}……",moneyLost.to_s_formatted))
      else
        pbDisplayPaused(_INTL("你不小心掉了${1}……",moneyLost.to_s_formatted))
      end
    end
  end

  def pbEndOfBattle
    oldDecision = @decision
    @decision = 4 if @decision==1 && wildBattle? && @caughtPokemon.length>0
    case oldDecision
    ##### WIN #####
    when 1
      PBDebug.log("")
      PBDebug.log("***Player won***")
      if trainerBattle?
        @scene.pbTrainerBattleSuccess
        case @opponent.length
        when 1
          pbDisplayPaused(_INTL("你打败了\n{1}！",@opponent[0].fullname))
        when 2
          pbDisplayPaused(_INTL("你打败了\n{1}和{2}！",@opponent[0].fullname,
             @opponent[1].fullname))
        when 3
          pbDisplayPaused(_INTL("你打败了\n{1}、{2}和{3}！",@opponent[0].fullname,
             @opponent[1].fullname,@opponent[2].fullname))
        end
        @opponent.each_with_index do |_t,i|
          @scene.pbShowOpponent(i)
          msg = (@endSpeeches[i] && @endSpeeches[i]!="") ? @endSpeeches[i] : "..."
          pbDisplayPaused(msg.gsub(/\\[Pp][Nn]/,pbPlayer.name))
        end
      end
      # Gain money from winning a trainer battle, and from Pay Day
      pbGainMoney if @decision!=4
      # Hide remaining trainer
      @scene.pbShowOpponent(@opponent.length) if trainerBattle? && @caughtPokemon.length>0
    ##### LOSE, DRAW #####
    when 2, 5
      PBDebug.log("")
      PBDebug.log("***Player lost***") if @decision==2
      PBDebug.log("***Player drew with opponent***") if @decision==5
      if @internalBattle
        pbDisplayPaused(_INTL("所有的宝可梦都倒下了……"))
        if trainerBattle?
          case @opponent.length
          when 1
            pbDisplayPaused(_INTL("你输给了\n{1}！",@opponent[0].fullname))
          when 2
            pbDisplayPaused(_INTL("你输给了\n{1}和{2}！",
               @opponent[0].fullname,@opponent[1].fullname))
          when 3
            pbDisplayPaused(_INTL("你输给了\n{1}、{2}和{3}！",
               @opponent[0].fullname,@opponent[1].fullname,@opponent[2].fullname))
          end
        end
        # Lose money from losing a battle
        pbLoseMoney
        pbDisplayPaused(_INTL("你眼前一黑！")) if !@canLose
      elsif @decision==2
        if @opponent
          @opponent.each_with_index do |_t,i|
            @scene.pbShowOpponent(i)
            msg = (@endSpeechesWin[i] && @endSpeechesWin[i]!="") ? @endSpeechesWin[i] : "..."
            pbDisplayPaused(msg.gsub(/\\[Pp][Nn]/,pbPlayer.name))
          end
        end
      end
    ##### CAUGHT WILD POKÉMON #####
    when 4
      @scene.pbWildBattleSuccess if !GAIN_EXP_FOR_CAPTURE
    end
    # Register captured Pokémon in the Pokédex, and store them
    pbRecordAndStoreCaughtPokemon
    # Collect Pay Day money in a wild battle that ended in a capture
    pbGainMoney if @decision==4
    # Pass on Pokérus within the party
    if @internalBattle
      infected = []
      $Trainer.party.each_with_index do |pkmn,i|
        infected.push(i) if pkmn.pokerusStage==1
      end
      infected.each do |idxParty|
        strain = $Trainer.party[idxParty].pokerusStrain
        if idxParty>0 && $Trainer.party[idxParty-1].pokerusStage==0
          $Trainer.party[idxParty-1].givePokerus(strain) if rand(3)==0   # 33%
        end
        if idxParty<$Trainer.party.length-1 && $Trainer.party[idxParty+1].pokerusStage==0
          $Trainer.party[idxParty+1].givePokerus(strain) if rand(3)==0   # 33%
        end
      end
    end
    # Clean up battle stuff
    @scene.pbEndBattle(@decision)
    @battlers.each do |b|
      next if !b
      pbCancelChoice(b.index)   # Restore unused items to Bag
      BattleHandlers.triggerAbilityOnSwitchOut(b.ability,b,true) if b.abilityActive?
    end
    pbParty(0).each_with_index do |pkmn,i|
      next if !pkmn
      @peer.pbOnLeavingBattle(self,pkmn,@usedInBattle[0][i],true)   # Reset form
      pkmn.setItem(@initialItems[0][i] || 0)
    end
    return @decision
  end

  #=============================================================================
  # Judging
  #=============================================================================
  def pbJudgeCheckpoint(user,move=nil); end

  def pbDecisionOnTime
    counts   = [0,0]
    hpTotals = [0,0]
    for side in 0...2
      pbParty(side).each do |pkmn|
        next if !pkmn || !pkmn.able?
        counts[side]   += 1
        hpTotals[side] += pkmn.hp
      end
    end
    return 1 if counts[0]>counts[1]       # Win (player has more able Pokémon)
    return 2 if counts[0]<counts[1]       # Loss (foe has more able Pokémon)
    return 1 if hpTotals[0]>hpTotals[1]   # Win (player has more HP in total)
    return 2 if hpTotals[0]<hpTotals[1]   # Loss (foe has more HP in total)
    return 5                              # Draw
  end

  # Unused
  def pbDecisionOnTime2
    counts   = [0,0]
    hpTotals = [0,0]
    for side in 0...2
      pbParty(side).each do |pkmn|
        next if !pkmn || !pkmn.able?
        counts[side]   += 1
        hpTotals[side] += 100*pkmn.hp/pkmn.totalhp
      end
      hpTotals[side] /= counts[side] if counts[side]>1
    end
    return 1 if counts[0]>counts[1]       # Win (player has more able Pokémon)
    return 2 if counts[0]<counts[1]       # Loss (foe has more able Pokémon)
    return 1 if hpTotals[0]>hpTotals[1]   # Win (player has a bigger average HP %)
    return 2 if hpTotals[0]<hpTotals[1]   # Loss (foe has a bigger average HP %)
    return 5                              # Draw
  end

  def pbDecisionOnDraw; return 5; end     # Draw

  def pbJudge
    fainted1 = pbAllFainted?(0)
    fainted2 = pbAllFainted?(1)
    if fainted1 && fainted2; @decision = pbDecisionOnDraw   # Draw
    elsif fainted1;          @decision = 2                  # Loss
    elsif fainted2;          @decision = 1                  # Win
    end
  end
end
