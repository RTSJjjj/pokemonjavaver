class PokeBattle_Battle
  #=============================================================================
  # Gaining Experience
  #=============================================================================
  def pbGainExp
    # Play wild victory music if it's the end of the battle (has to be here)
    @scene.pbWildBattleSuccess if wildBattle? && pbAllFainted?(1) && !pbAllFainted?(0)
    return if !@internalBattle || !@expGain
    # Go through each battler in turn to find the Pokémon that participated in
    # battle against it, and award those Pokémon Exp/EVs
    expAll = (hasConst?(PBItems,:EXPALL) && $PokemonBag.pbHasItem?(:EXPALL))
    p1 = pbParty(0)
    @battlers.each do |b|
      next unless b && b.opposes?   # Can only gain Exp from fainted foes
      next if b.participants.length==0
      next unless b.fainted? || b.captured
      # Count the number of participants
      numPartic = 0
      b.participants.each do |partic|
        next unless p1[partic] && p1[partic].able? && pbIsOwner?(0,partic)
        numPartic += 1
      end
      # Find which Pokémon have an Exp Share
      expShare = []
      if !expAll
        eachInTeam(0,0) do |pkmn,i|
          next if !pkmn.able?
          next if !pkmn.hasItem?(:EXPSHARE) &&
                  !isConst?(@initialItems[0][i],PBItems,:EXPSHARE)
          expShare.push(i)
        end
      end
      # Calculate EV and Exp gains for the participants
      if numPartic>0 || expShare.length>0 || expAll
        # 经验储罐
        $Trainer.exp_pot = 0 if !$Trainer.exp_pot
        old_exp_pot = $Trainer.exp_pot
        # Gain EVs and Exp for participants
        eachInTeam(0,0) do |pkmn,i|
          next if !pkmn.able?
          next unless b.participants.include?(i) || expShare.include?(i)
          pbGainEVsOne(i,b)
          pbGainExpOne(i,b,numPartic,expShare,expAll)
        end
        # Gain EVs and Exp for all other Pokémon because of Exp All
        if expAll
          showMessage = true
          eachInTeam(0,0) do |pkmn,i|
            next if !pkmn.able?
            next if b.participants.include?(i) || expShare.include?(i)
            pbDisplayPaused(_INTL("其他宝可梦也获得了经验。")) if showMessage
            showMessage = false
            pbGainEVsOne(i,b)
            pbGainExpOne(i,b,numPartic,expShare,expAll,false)
          end
        end
        # 经验储罐
        if $Trainer.exp_pot > old_exp_pot && $PokemonBag.pbHasItem?(:EXPPOT)
          exp_addded = $Trainer.exp_pot - old_exp_pot
          pbDisplayPaused(_INTL("经验储罐累积的经验值增加了{1}点。",exp_addded))
        end
      end
      # Clear the participants array
      b.participants = []
    end
  end

  def pbGainEVsOne(idxParty,defeatedBattler)
    pkmn = pbParty(0)[idxParty]   # The Pokémon gaining EVs from defeatedBattler
    evYield = defeatedBattler.pokemon.evYield
    # Num of effort points pkmn already has
    evTotal = 0
    PBStats.eachStat { |s| evTotal += pkmn.ev[s] }
    # Modify EV yield based on pkmn's held item
    if !BattleHandlers.triggerEVGainModifierItem(pkmn.item,pkmn,evYield)
      BattleHandlers.triggerEVGainModifierItem(@initialItems[0][idxParty],pkmn,evYield)
    end
    # Double EV gain because of Pokérus
    if pkmn.pokerusStage>=1   # Infected or cured
      evYield.collect! { |a| a*2 }
    end
    # Gain EVs for each stat in turn
    PBStats.eachStat do |s|
      evGain = evYield[s]
      # Can't exceed overall limit
      if evTotal+evGain>PokeBattle_Pokemon::EV_LIMIT
        evGain = PokeBattle_Pokemon::EV_LIMIT-evTotal
      end
      # Can't exceed individual stat limit
      if pkmn.ev[s]+evGain>PokeBattle_Pokemon::EV_STAT_LIMIT
        evGain = PokeBattle_Pokemon::EV_STAT_LIMIT-pkmn.ev[s]
      end
      # Add EV gain
      pkmn.ev[s] += evGain
      evTotal += evGain
    end
  end

  def pbGainExpOne(idxParty,defeatedBattler,numPartic,expShare,expAll,showMessages=true)
    $Trainer.exp_pot = 0 if !$Trainer.exp_pot
    pkmn = pbParty(0)[idxParty]   # The Pokémon gaining EVs from defeatedBattler
    growthRate = pkmn.growthrate
    # Don't bother calculating if gainer is already at max Exp
    if pkmn.exp>=PBExperience.pbGetMaxExperience(growthRate)
      pkmn.calcStats   # To ensure new EVs still have an effect
      return
    end
    isPartic    = defeatedBattler.participants.include?(idxParty)
    hasExpShare = expShare.include?(idxParty)
    level = defeatedBattler.level
    # Main Exp calculation
    exp = 0
    a = level*defeatedBattler.pokemon.baseExp
    if expShare.length>0 && (isPartic || hasExpShare)
      if numPartic==0   # No participants, all Exp goes to Exp Share holders
        exp = a/(SPLIT_EXP_BETWEEN_GAINERS ? expShare.length : 1)
      elsif SPLIT_EXP_BETWEEN_GAINERS   # Gain from participating and/or Exp Share
        exp = a/(2*numPartic) if isPartic
        exp += a/(2*expShare.length) if hasExpShare
      else   # Gain from participating and/or Exp Share (Exp not split)
        exp = (isPartic) ? a : a/2
      end
    elsif isPartic   # Participated in battle, no Exp Shares held by anyone
      exp = a/(SPLIT_EXP_BETWEEN_GAINERS ? numPartic : 1)
    elsif expAll   # Didn't participate in battle, gaining Exp due to Exp All
      # NOTE: Exp All works like the Exp Share from Gen 6+, not like the Exp All
      #       from Gen 1, i.e. Exp isn't split between all Pokémon gaining it.
      exp = a/2
    end
    return if exp<=0
    # Pokémon gain more Exp from trainer battles
    exp = (exp*1.5).floor if trainerBattle?
    # Scale the gained Exp based on the gainer's level (or not)
    if SCALED_EXP_FORMULA
      exp /= 5
      levelAdjust = (2*level+10.0)/(pkmn.level+level+10.0)
      levelAdjust = levelAdjust**5
      levelAdjust = Math.sqrt(levelAdjust)
      exp *= levelAdjust
      exp = exp.floor
      exp += 1 if isPartic || hasExpShare
    else
      exp /= 7
    end
    # Foreign Pokémon gain more Exp
    isOutsider = (pkmn.trainerID!=pbPlayer.id ||
                 (pkmn.language!=0 && pkmn.language!=pbPlayer.language))
    if isOutsider
      if pkmn.language!=0 && pkmn.language!=pbPlayer.language
        exp = (exp*1.7).floor
      else
        exp = (exp*1.5).floor
      end
    end
    # Modify Exp gain based on pkmn's held item
    i = BattleHandlers.triggerExpGainModifierItem(pkmn.item,pkmn,exp)
    if i<0
      i = BattleHandlers.triggerExpGainModifierItem(@initialItems[0][idxParty],pkmn,exp)
    end
    exp = (exp*1.2).floor if pkmn.happiness>=180
    exp = (exp*1.2).floor if pkmn.superShiny?
    exp = (exp*1.5).floor if hasConst?(PBItems,:EXPCHARM) && $PokemonBag.pbHasItem?(:EXPCHARM) # EXP Charm Code
    exp = i if i>=0
    if pkmn.level>=200
      $Trainer.exp_pot += [1, exp / 8].max
      exp = 0
    else
      # 如果开启了等级限制且达到上限则经验值为0
      if $game_switches[199]
        maxLevel = MAX_LEVEL
        # 二周目
        if $game_switches[12]
          if (8..15).any? { |i| pkmn.level >= maxLevel[i+1] && !$Trainer.badges[i] }
            $Trainer.exp_pot += [1, exp / 8].max
            exp = 0
          end
        # 一周目
        else
          if (0..7).any? { |i| pkmn.level >= maxLevel[i] && !$Trainer.badges[i] } ||
             pkmn.level >= maxLevel[8]
            $Trainer.exp_pot += [1, exp / 8].max
            exp = 0
          end
        end
      end
    end
    # Make sure Exp doesn't exceed the maximum
    expFinal = PBExperience.pbAddExperience(pkmn.exp,exp,growthRate)
    expGained = expFinal-pkmn.exp
    return if expGained<=0
    # "Exp gained" message
    if showMessages
      if isOutsider
        pbDisplayPaused(_INTL("{1}获得了得到增幅的{2}点经验值！",pkmn.name,expGained))
      else
        pbDisplayPaused(_INTL("{1}获得了{2}点经验值！",pkmn.name,expGained))
      end
    end
    oldLevel = pkmn.level
    oldTotalHP = pkmn.totalhp
    oldAttack  = pkmn.attack
    oldDefense = pkmn.defense
    oldSpAtk   = pkmn.spatk
    oldSpDef   = pkmn.spdef
    oldSpeed   = pkmn.speed
    curLevel = pkmn.level
    newLevel = PBExperience.pbGetLevelFromExperience(expFinal,growthRate)
    if newLevel<curLevel
      debugInfo = "等级: #{curLevel}->#{newLevel} | 经验: #{pkmn.exp}->#{expFinal} | 获得: #{expGained}经验"
      p _INTL("{1}的新等级低于当前等级，这不应该发生，请将此弹窗截图并联系制作组，否则将可能永远无法修复。\r\n[{2}]",
          pkmn.name, debugInfo)
      newLevel = curLevel
    end
    # Give Exp
    if pkmn.shadowPokemon?
      pkmn.exp += expGained
      return
    end
    tempExp1 = pkmn.exp
    battler = pbFindBattler(idxParty)
    loop do   # For each level gained in turn...
      break if curLevel >= 200
      if $game_switches[199]
        if curLevel>=16 && !$Trainer.badges[1]
          break
        elsif curLevel>=25 && !$Trainer.badges[0]
          break
        elsif	curLevel>=32 && !$Trainer.badges[2]
          break
        elsif	curLevel>=36 && !$Trainer.badges[3]
          break
        elsif	curLevel>=42 && !$Trainer.badges[4]
          break
        elsif	curLevel>=48 && !$Trainer.badges[5]
          break
        elsif	curLevel>=58 && !$Trainer.badges[6]
          break
        elsif	curLevel>=63 && !$Trainer.badges[7]
          break
        elsif curLevel>=85 && !$game_switches[12]
          break
        elsif	curLevel>=113 && !$Trainer.badges[9]
          break
        elsif	curLevel>=125 && !$Trainer.badges[10]
          break
        elsif	curLevel>=138 && !$Trainer.badges[11]
          break
        elsif	curLevel>=150 && !$Trainer.badges[12]
          break
        elsif	curLevel>=163 && !$Trainer.badges[13]
          break
        elsif	curLevel>=175 && !$Trainer.badges[14]
          break
        elsif	curLevel>=188 && !$Trainer.badges[15]
          break
        end
      end
      # EXP Bar animation
      levelMinExp = PBExperience.pbGetStartExperience(curLevel,growthRate)
      levelMaxExp = PBExperience.pbGetStartExperience(curLevel+1,growthRate)
      tempExp2 = (levelMaxExp<expFinal) ? levelMaxExp : expFinal
      pkmn.exp = tempExp2
      @scene.pbEXPBar(battler,levelMinExp,levelMaxExp,tempExp1,tempExp2)
      tempExp1 = tempExp2
      curLevel += 1
      if curLevel>newLevel
        # Gained all the Exp now, end the animation
        pkmn.calcStats
        battler.pbUpdate(false) if battler
        @scene.pbRefreshOne(battler.index) if battler
        break
      end
      # Levelled up
      pbCommonAnimation("LevelUp",battler) if battler
      if battler && battler.pokemon
        battler.pokemon.changeHappiness("levelup")
      end
      pkmn.calcStats
      battler.pbUpdate(false) if battler
      @scene.pbRefreshOne(battler.index) if battler
    end
    if curLevel-1 > oldLevel
      pbDisplayPaused(_INTL("{1}升到了{2}级！",pkmn.name,curLevel-1)) { pbSEPlay("Pkmn level up") }
      @scene.pbLevelUp(pkmn,battler,oldTotalHP,oldAttack,oldDefense,
                                    oldSpAtk,oldSpDef,oldSpeed)
      # Learn all moves learned at this level
      realList = []
      moveList = pkmn.getMoveList
      moveList.each do |m|
        next if m[0] <= oldLevel || m[0] > curLevel-1
        realList.push(m)
      end
      if realList.length == 1 || realList.length > 1 &&
         pbDisplayConfirm(_INTL("要{1}立即学习招式吗？",pkmn.name))
        realList.each { |m| pbLearnMove(idxParty, m[1]) }
      end
    end
    # 经验储罐
    if $PokemonBag.pbHasItem?(:EXPPOT)
      exp_max = EXP_POT_MAX
      $Trainer.exp_pot = 0 if !$Trainer.exp_pot
      exp_addded = [1, expGained / 8].max
      if $Trainer.exp_pot + exp_addded > exp_max
        exp_addded = exp_max - $Trainer.exp_pot
      end
      $Trainer.exp_pot += exp_addded
    end
  end
  #=============================================================================
  # Learning a move
  #=============================================================================
  def pbLearnMove(idxParty,newMove)
    pkmn = pbParty(0)[idxParty]
    return if !pkmn
    pkmnName = pkmn.name
    battler = pbFindBattler(idxParty)
    moveName = PBMoves.getName(newMove)
    # Find a space for the new move in pkmn's moveset and learn it
    pkmn.moves.each_with_index do |m,i|
      return if m.id==newMove   # Already knows the new move
      next if m.id!=0           # Not a blank move slot
      pkmn.moves[i] = PBMove.new(newMove)
      battler.moves[i] = PokeBattle_Move.pbFromPBMove(self,pkmn.moves[i]) if battler
      pbDisplay(_INTL("{1}学会了{2}！",pkmnName,moveName)) { pbSEPlay("Pkmn move learnt") }
      battler.pbCheckFormOnMovesetChange if battler
      return
    end
    # pkmn already knows four moves, need to forget one to learn newMove
      pbDisplayPaused(_INTL("{1}想要学会{2}。\n可是它已经学会四个招式了。",pkmnName,moveName))
      if pbDisplayConfirm(_INTL("遗忘一个招式来学习{1}吗？",moveName))
        pbDisplayPaused(_INTL("要忘记哪一个招式？"))
        forgetMove = @scene.pbForgetMove(pkmn,newMove)
        if forgetMove>=0
          oldMoveName = PBMoves.getName(pkmn.moves[forgetMove].id)
          pkmn.moves[forgetMove] = PBMove.new(newMove)   # Replaces current/total PP
          battler.moves[forgetMove] = PokeBattle_Move.pbFromPBMove(self,pkmn.moves[forgetMove]) if battler
          pbDisplayPaused(_INTL("一，二…………当当!"))
          pbDisplayPaused(_INTL("{1}遗忘了{2}。\n以及……",pkmnName,oldMoveName))
          pbDisplay(_INTL("{1}学会了{2}！",pkmnName,moveName)) { pbSEPlay("Pkmn move learnt") }
          battler.pbCheckFormOnMovesetChange if battler
      end
    end
  end
end
