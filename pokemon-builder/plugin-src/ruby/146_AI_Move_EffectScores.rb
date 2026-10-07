class PokeBattle_AI
  #=============================================================================
  # Get a score for the given move based on its effect
  #=============================================================================
  def pbGetMoveScoreFunctionCode(score,move,user,target,skill=100)
    if move.priority > 0
      if move.physicalMove?(move.type)
        score += user.stages[PBStats::ATTACK]*10
      elsif move.specialMove?(move.type)
        score += user.stages[PBStats::SPATK]*10
      end
    end
    case move.function
    #---------------------------------------------------------------------------
    when "000"   # No extra effect 
      if target.hp<target.totalhp/10 && move.priority >= 1
        score += 200
    end
    #---------------------------------------------------------------------------
    when "001"
      score -= 95
      score = 0 if skill>=PBTrainerAI.highSkill
    #---------------------------------------------------------------------------
    when "002"   # Struggle
    #---------------------------------------------------------------------------
    when "003"
      if target.pbCanSleep?(user,false)
        score += 100
        if skill>=PBTrainerAI.mediumSkill
          score -= 30 if target.effects[PBEffects::Yawn]>0
        end
        if skill>=PBTrainerAI.highSkill
          score -= 30 if target.hasActiveAbility?(:MARVELSCALE)
        end
        if skill>=PBTrainerAI.bestSkill
          if target.pbHasMoveFunction?("011","0B4")   # Snore, Sleep Talk
            score -= 50
          end
        end
      else
        if skill>=PBTrainerAI.mediumSkill
          score -= 90 if move.statusMove?
        end
      end
    #---------------------------------------------------------------------------
    when "004"
      if target.effects[PBEffects::Yawn]>0 || !target.pbCanSleep?(user,false)
        score -= 90 if skill>=PBTrainerAI.mediumSkill
      else
        score += 30
        if skill>=PBTrainerAI.highSkill
          score -= 30 if target.hasActiveAbility?(:MARVELSCALE)
        end
        if skill>=PBTrainerAI.bestSkill
          if target.pbHasMoveFunction?("011","0B4")   # Snore, Sleep Talk
            score -= 50
          end
        end
      end
    #---------------------------------------------------------------------------
    when "005", "006", "0BE"
      if target.pbCanPoison?(user,false)
        score += 30
        if skill>=PBTrainerAI.mediumSkill
          score += 30 if target.hp<=target.totalhp/4
          score += 50 if target.hp<=target.totalhp/8
          score -= 40 if target.effects[PBEffects::Yawn]>0
        end
        if skill>=PBTrainerAI.highSkill
          score += 10 if pbRoughStat(target,PBStats::DEFENSE,skill)>100
          score += 10 if pbRoughStat(target,PBStats::SPDEF,skill)>100
          score -= 40 if target.hasActiveAbility?([:GUTS,:MARVELSCALE,:TOXICBOOST])
        end
      else
        score -= 90 if move.statusMove?
      end
    #---------------------------------------------------------------------------
    when "007", "008", "009", "0C5"
      if target.pbCanParalyze?(user,false) &&
         !(skill>=PBTrainerAI.mediumSkill &&
         isConst?(move.id,PBMoves,:THUNDERWAVE) &&
         PBTypes.ineffective?(pbCalcTypeMod(move.type,user,target)))
        score += 30
        if skill>=PBTrainerAI.mediumSkill
           aspeed = pbRoughStat(user,PBStats::SPEED,skill)
           ospeed = pbRoughStat(target,PBStats::SPEED,skill)
          if aspeed<ospeed
            score += 30
          elsif aspeed>ospeed
            score -= 40
          end
        end
        if skill>=PBTrainerAI.highSkill
          score -= 40 if target.hasActiveAbility?([:GUTS,:MARVELSCALE,:QUICKFEET])
        end
      else
        if skill>=PBTrainerAI.mediumSkill
          score -= 90 if move.statusMove?
        end
      end
    #---------------------------------------------------------------------------
    when "00A", "00B", "0C6"
      if target.pbCanBurn?(user,false)
        score += 30
        if skill>=PBTrainerAI.highSkill
          score -= 40 if target.hasActiveAbility?([:GUTS,:MARVELSCALE,:QUICKFEET,:FLAREBOOST])
        end
      else
        if skill>=PBTrainerAI.mediumSkill
          score -= 90 if move.statusMove?
        end
      end
    #---------------------------------------------------------------------------
    when "00C", "00D", "00E"
      if target.pbCanFreeze?(user,false)
        score += 30
        if skill>=PBTrainerAI.highSkill
          score -= 20 if target.hasActiveAbility?(:MARVELSCALE)
        end
      else
        if skill>=PBTrainerAI.mediumSkill
          score -= 90 if move.statusMove?
        end
      end
    #---------------------------------------------------------------------------
    when "00F"
      score += 30
      if skill>=PBTrainerAI.highSkill
        score += 30 if !target.hasActiveAbility?(:INNERFOCUS) &&
                       target.effects[PBEffects::Substitute]==0
      end
    #---------------------------------------------------------------------------
    when "010"
      if skill>=PBTrainerAI.highSkill
        score += 30 if !target.hasActiveAbility?(:INNERFOCUS) &&
                       target.effects[PBEffects::Substitute]==0
      end
      score += 30 if target.effects[PBEffects::Minimize]
    #---------------------------------------------------------------------------
    when "011"
      if user.asleep?
        score += 100   # Because it can only be used while asleep
        if skill>=PBTrainerAI.highSkill
          score += 30 if !target.hasActiveAbility?(:INNERFOCUS) &&
                         target.effects[PBEffects::Substitute]==0
        end
      else
        score -= 90   # Because it will fail here
        score = 0 if skill>=PBTrainerAI.bestSkill
      end
    #---------------------------------------------------------------------------
    when "012"
      if user.turnCount==0
        if skill>=PBTrainerAI.highSkill
          score += 130 if !target.hasActiveAbility?(:INNERFOCUS) &&
                         target.effects[PBEffects::Substitute]==0
        end
      else
        score -= 200   # Because it will fail here
        score = 0 if skill>=PBTrainerAI.bestSkill
      end
    #---------------------------------------------------------------------------
    when "013", "014", "015"
      if target.pbCanConfuse?(user,false)
        score += 30
      else
        if skill>=PBTrainerAI.mediumSkill
          score -= 90 if move.statusMove?
        end
      end
    #---------------------------------------------------------------------------
    when "016"
      canattract = true
      agender = user.gender
      ogender = target.gender
      if agender==2 || ogender==2 || agender==ogender
        score -= 90; canattract = false
      elsif target.effects[PBEffects::Attract]>=0
        score -= 80; canattract = false
      elsif skill>=PBTrainerAI.bestSkill && target.hasActiveAbility?(:OBLIVIOUS)
        score -= 80; canattract = false
      end
      if skill>=PBTrainerAI.highSkill
        if canattract && target.hasActiveItem?(:DESTINYKNOT) &&
           user.pbCanAttract?(target,false)
          score -= 30
        end
      end
    #---------------------------------------------------------------------------
    when "017"
      score += 30 if target.status==PBStatuses::NONE
    #---------------------------------------------------------------------------
    when "018"
      case user.status
      when PBStatuses::POISON
        score += 40
        if skill>=PBTrainerAI.mediumSkill
          if user.hp<user.totalhp/8
            score += 60
          elsif skill>=PBTrainerAI.highSkill &&
             user.hp<(user.effects[PBEffects::Toxic]+1)*user.totalhp/16
            score += 60
          end
        end
      when PBStatuses::BURN, PBStatuses::PARALYSIS
        score += 40
      else
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "019"
      statuses = 0
      @battle.pbParty(user.index).each do |pkmn|
        statuses += 1 if pkmn && pkmn.status!=PBStatuses::NONE
      end
      if statuses==0
        score -= 80
      else
        score += 20*statuses
      end
    #---------------------------------------------------------------------------
    when "01A"
      if user.pbOwnSide.effects[PBEffects::Safeguard]>0
        score -= 80
      elsif user.status!=0
        score -= 40
      else
        score += 30
      end
    #---------------------------------------------------------------------------
    when "01B"
      if user.status==PBStatuses::NONE
        score -= 90
      else
        score += 40
      end
    #---------------------------------------------------------------------------
    when "01C"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::ATTACK)
          score -= 90
        else
          score -= user.stages[PBStats::ATTACK]*20
          if skill>=PBTrainerAI.mediumSkill
            hasPhysicalAttack = false
            user.eachMove do |m|
              next if !m.physicalMove?(m.type)
              hasPhysicalAttack = true
              break
            end
            if hasPhysicalAttack
              score += 20
            elsif skill>=PBTrainerAI.highSkill
              score -= 90
            end
          end
        end
      else
        score += 20 if user.stages[PBStats::ATTACK]<0
        if skill>=PBTrainerAI.mediumSkill
          hasPhysicalAttack = false
          user.eachMove do |m|
            next if !m.physicalMove?(m.type)
            hasPhysicalAttack = true
            break
          end
          score += 20 if hasPhysicalAttack
        end
      end
    #---------------------------------------------------------------------------
    when "01D", "01E", "0C8"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::DEFENSE)
          score -= 90
        else
          score -= user.stages[PBStats::DEFENSE]*20
        end
      else
        score += 20 if user.stages[PBStats::DEFENSE]<0
      end
    #---------------------------------------------------------------------------
    when "01F"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::SPEED)
          score -= 90
        else
          score -= user.stages[PBStats::SPEED]*10
          if skill>=PBTrainerAI.highSkill
            aspeed = pbRoughStat(user,PBStats::SPEED,skill)
            ospeed = pbRoughStat(target,PBStats::SPEED,skill)
            score += 30 if aspeed<ospeed && aspeed*2>ospeed
          end
        end
      else
        score += 20 if user.stages[PBStats::SPEED]<0
      end
    #---------------------------------------------------------------------------
    when "020"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::SPATK)
          score -= 90
        else
          score -= user.stages[PBStats::SPATK]*20
          if skill>=PBTrainerAI.mediumSkill
            hasSpecicalAttack = false
            user.eachMove do |m|
              next if !m.specialMove?(m.type)
              hasSpecicalAttack = true
              break
            end
            if hasSpecicalAttack
              score += 20
            elsif skill>=PBTrainerAI.highSkill
              score -= 90
            end
          end
        end
      else
        score += 20 if user.stages[PBStats::SPATK]<0
        if skill>=PBTrainerAI.mediumSkill
          hasSpecicalAttack = false
          user.eachMove do |m|
            next if !m.specialMove?(m.type)
            hasSpecicalAttack = true
            break
          end
          score += 20 if hasSpecicalAttack
        end
      end
    #---------------------------------------------------------------------------
    when "021"
      foundMove = false
      user.eachMove do |m|
        next if !isConst?(m.type,PBTypes,:ELECTRIC) || !m.damagingMove?
        foundMove = true
        break
      end
      score += 20 if foundMove
      if move.statusMove?
        if user.statStageAtMax?(PBStats::SPDEF)
          score -= 90
        else
          score -= user.stages[PBStats::SPDEF]*20
        end
      else
        score += 20 if user.stages[PBStats::SPDEF]<0
      end
    #---------------------------------------------------------------------------
    when "022"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::EVASION)
          score -= 90
        else
          score -= user.stages[PBStats::EVASION]*10
        end
      else
        score += 20 if user.stages[PBStats::EVASION]<0
      end
    #---------------------------------------------------------------------------
    when "023"
      if move.statusMove?
        if user.effects[PBEffects::FocusEnergy]>=2
          score -= 80
        else
          score += 30
        end
      else
        score += 30 if user.effects[PBEffects::FocusEnergy]<2
      end
    #---------------------------------------------------------------------------
    when "024"
      if user.statStageAtMax?(PBStats::ATTACK) &&
         user.statStageAtMax?(PBStats::DEFENSE)
        score -= 90
      else
        score -= user.stages[PBStats::ATTACK]*10
        score -= user.stages[PBStats::DEFENSE]*10
        if skill>=PBTrainerAI.mediumSkill
          hasPhysicalAttack = false
          user.eachMove do |m|
            next if !m.physicalMove?(m.type)
            hasPhysicalAttack = true
            break
          end
          if hasPhysicalAttack
            score += 20
          elsif skill>=PBTrainerAI.highSkill
            score -= 90
          end
        end
      end
    #---------------------------------------------------------------------------
    when "025"
      if user.statStageAtMax?(PBStats::ATTACK) &&
         user.statStageAtMax?(PBStats::DEFENSE) &&
         user.statStageAtMax?(PBStats::ACCURACY)
        score -= 90
      else
        score -= user.stages[PBStats::ATTACK]*10
        score -= user.stages[PBStats::DEFENSE]*10
        score -= user.stages[PBStats::ACCURACY]*10
        if skill>=PBTrainerAI.mediumSkill
          hasPhysicalAttack = false
          user.eachMove do |m|
            next if !m.physicalMove?(m.type)
            hasPhysicalAttack = true
            break
          end
          if hasPhysicalAttack
            score += 20
          elsif skill>=PBTrainerAI.highSkill
            score -= 90
          end
        end
      end
    #---------------------------------------------------------------------------
    when "026"
      score += 40 if user.turnCount==0   # Dragon Dance tends to be popular
      if user.statStageAtMax?(PBStats::ATTACK) &&
         user.statStageAtMax?(PBStats::SPEED)
        score -= 90
      else
        score -= user.stages[PBStats::ATTACK]*10
        score -= user.stages[PBStats::SPEED]*10
        if skill>=PBTrainerAI.mediumSkill
          hasPhysicalAttack = false
          user.eachMove do |m|
            next if !m.physicalMove?(m.type)
            hasPhysicalAttack = true
            break
          end
          if hasPhysicalAttack
            score += 30
          elsif skill>=PBTrainerAI.highSkill
            score -= 90
          end
        end
        if skill>=PBTrainerAI.highSkill
          aspeed = pbRoughStat(user,PBStats::SPEED,skill)
          ospeed = pbRoughStat(target,PBStats::SPEED,skill)
          score += 30 if aspeed<ospeed && aspeed*2>ospeed
        end
      end
    #---------------------------------------------------------------------------
    when "027", "028"
      if user.statStageAtMax?(PBStats::ATTACK) &&
         user.statStageAtMax?(PBStats::SPATK)
        score -= 90
      else
        score -= user.stages[PBStats::ATTACK]*10
        score -= user.stages[PBStats::SPATK]*10
        if skill>=PBTrainerAI.mediumSkill
          hasDamagingAttack = false
          user.eachMove do |m|
            next if !m.damagingMove?
            hasDamagingAttack = true
            break
          end
          if hasDamagingAttack
            score += 20
          elsif skill>=PBTrainerAI.highSkill
            score -= 90
          end
        end
        if move.function=="028"   # Growth
          score += 20 if @battle.pbWeather==PBWeather::Sun ||
                         @battle.pbWeather==PBWeather::HarshSun
        end
      end
    #---------------------------------------------------------------------------
    when "029"
      if user.statStageAtMax?(PBStats::ATTACK) &&
         user.statStageAtMax?(PBStats::ACCURACY)
        score -= 90
      else
        score -= user.stages[PBStats::ATTACK]*10
        score -= user.stages[PBStats::ACCURACY]*10
        if skill>=PBTrainerAI.mediumSkill
          hasPhysicalAttack = false
          user.eachMove do |m|
            next if !m.physicalMove?(m.type)
            hasPhysicalAttack = true
            break
          end
          if hasPhysicalAttack
            score += 30
          elsif skill>=PBTrainerAI.highSkill
            score -= 90
          end
        end
      end
    #---------------------------------------------------------------------------
    when "02A"
      if user.statStageAtMax?(PBStats::DEFENSE) &&
         user.statStageAtMax?(PBStats::SPDEF)
        score -= 90
      else
        score -= user.stages[PBStats::DEFENSE]*10
        score -= user.stages[PBStats::SPDEF]*10
      end
    #---------------------------------------------------------------------------
    when "02B"
      if user.statStageAtMax?(PBStats::SPEED) &&
         user.statStageAtMax?(PBStats::SPATK) &&
         user.statStageAtMax?(PBStats::SPDEF)
        score -= 90
      else
        score -= user.stages[PBStats::SPATK]*10
        score -= user.stages[PBStats::SPDEF]*10
        score -= user.stages[PBStats::SPEED]*10
        if skill>=PBTrainerAI.mediumSkill
          hasSpecicalAttack = false
          user.eachMove do |m|
            next if !m.specialMove?(m.type)
            hasSpecicalAttack = true
            break
          end
          if hasSpecicalAttack
            score += 30
          elsif skill>=PBTrainerAI.highSkill
            score -= 90
          end
        end
        if skill>=PBTrainerAI.highSkill
          aspeed = pbRoughStat(user,PBStats::SPEED,skill)
          ospeed = pbRoughStat(target,PBStats::SPEED,skill)
          if aspeed<ospeed && aspeed*2>ospeed
            score += 30
          end
        end
      end
    #---------------------------------------------------------------------------
    when "02C"
      if user.statStageAtMax?(PBStats::SPATK) &&
         user.statStageAtMax?(PBStats::SPDEF)
        score -= 90
      else
        score += 50 if user.turnCount==0   # Calm Mind tends to be popular
        score -= user.stages[PBStats::SPATK]*10
        score -= user.stages[PBStats::SPDEF]*10
        if skill>=PBTrainerAI.mediumSkill
          hasSpecicalAttack = false
          user.eachMove do |m|
            next if !m.specialMove?(m.type)
            hasSpecicalAttack = true
            break
          end
          if hasSpecicalAttack
            score += 30
          elsif skill>=PBTrainerAI.highSkill
            score -= 90
          end
        end
      end
    #---------------------------------------------------------------------------
    when "02D"
      PBStats.eachMainBattleStat { |s| score += 10 if user.stages[s]<0 }
      if skill>=PBTrainerAI.mediumSkill
        hasDamagingAttack = false
        user.eachMove do |m|
          next if !m.damagingMove?
          hasDamagingAttack = true
          break
        end
        score += 20 if hasDamagingAttack
      end
    #---------------------------------------------------------------------------
    when "02E"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::ATTACK)
          score -= 90
        else
          score += 30 if user.turnCount==0
          score -= user.stages[PBStats::ATTACK]*10
          if skill>=PBTrainerAI.mediumSkill
            hasPhysicalAttack = false
            user.eachMove do |m|
              next if !m.physicalMove?(m.type)
              hasPhysicalAttack = true
              break
            end
            if hasPhysicalAttack
              score += 30
            elsif skill>=PBTrainerAI.highSkill
              score -= 90
            end
          end
        end
      else
        score += 30 if user.turnCount==0
        score += 30 if user.stages[PBStats::ATTACK]<0
        if skill>=PBTrainerAI.mediumSkill
          hasPhysicalAttack = false
          user.eachMove do |m|
            next if !m.physicalMove?(m.type)
            hasPhysicalAttack = true
            break
          end
          score += 20 if hasPhysicalAttack
        end
      end
    #---------------------------------------------------------------------------
    when "02F"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::DEFENSE)
          score -= 90
        else
          score += 30 if user.turnCount==0
          score -= user.stages[PBStats::DEFENSE]*10
        end
      else
        score += 30 if user.turnCount==0
        score += 30 if user.stages[PBStats::DEFENSE]<0
      end
    #---------------------------------------------------------------------------
    when "030", "031"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::SPEED)
          score -= 90
        else
          score += 30 if user.turnCount==0
          score -= user.stages[PBStats::SPEED]*10
          if skill>=PBTrainerAI.highSkill
            aspeed = pbRoughStat(user,PBStats::SPEED,skill)
            ospeed = pbRoughStat(target,PBStats::SPEED,skill)
            score += 40 if aspeed<ospeed && aspeed*2>ospeed
          end
        end
      else
        score += 10 if user.turnCount==0
        score += 20 if user.stages[PBStats::SPEED]<0
      end
    #---------------------------------------------------------------------------
    when "032"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::SPATK)
          score -= 90
        else
          score += 60 if user.turnCount==0
          score -= user.stages[PBStats::SPATK]*20
          if skill>=PBTrainerAI.mediumSkill
            hasSpecicalAttack = false
            user.eachMove do |m|
              next if !m.specialMove?(m.type)
              hasSpecicalAttack = true
              break
            end
            if hasSpecicalAttack
              score += 30
            elsif skill>=PBTrainerAI.highSkill
              score -= 90
            end
          end
        end
      else
        score += 30 if user.turnCount==0
        score += 30 if user.stages[PBStats::SPATK]<0
        if skill>=PBTrainerAI.mediumSkill
          hasSpecicalAttack = false
          user.eachMove do |m|
            next if !m.specialMove?(m.type)
            hasSpecicalAttack = true
            break
          end
          score += 30 if hasSpecicalAttack
        end
      end
    #---------------------------------------------------------------------------
    when "033"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::SPDEF)
          score -= 90
        else
          score += 60 if user.turnCount==0
          score -= user.stages[PBStats::SPDEF]*20
        end
      else
        score += 30 if user.turnCount==0
        score += 30 if user.stages[PBStats::SPDEF]<0
      end
    #---------------------------------------------------------------------------
    when "034"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::EVASION)
          score -= 90
        else
          score += 40 if user.turnCount==0
          score -= user.stages[PBStats::EVASION]*10
        end
      else
        score += 10 if user.turnCount==0
        score += 20 if user.stages[PBStats::EVASION]<0
      end
    #---------------------------------------------------------------------------
    when "035"
      score -= user.stages[PBStats::ATTACK]*20
      score -= user.stages[PBStats::SPEED]*20
      score -= user.stages[PBStats::SPATK]*20
      score += user.stages[PBStats::DEFENSE]*10
      score += user.stages[PBStats::SPDEF]*10
      if skill>=PBTrainerAI.mediumSkill
        hasDamagingAttack = false
        user.eachMove do |m|
          next if !m.damagingMove?
          hasDamagingAttack = true
          break
        end
        if hasDamagingAttack
          score += 30 
          score += 30 if user.turnCount==0
        end
      end
    #---------------------------------------------------------------------------
    when "036"
      if user.statStageAtMax?(PBStats::ATTACK) &&
         user.statStageAtMax?(PBStats::SPEED)
        score -= 90
      else
        score -= user.stages[PBStats::ATTACK]*10
        score -= user.stages[PBStats::SPEED]*10
        if skill>=PBTrainerAI.mediumSkill
          hasPhysicalAttack = false
          user.eachMove do |m|
            next if !m.physicalMove?(m.type)
            hasPhysicalAttack = true
            break
          end
          if hasPhysicalAttack
            score += 20
          elsif skill>=PBTrainerAI.highSkill
            score -= 90
          end
        end
        if skill>=PBTrainerAI.highSkill
          aspeed = pbRoughStat(user,PBStats::SPEED,skill)
          ospeed = pbRoughStat(target,PBStats::SPEED,skill)
          score += 30 if aspeed<ospeed && aspeed*2>ospeed
        end
      end
    #---------------------------------------------------------------------------
    when "037"
      avgStat = 0; canChangeStat = false
      PBStats.eachBattleStat do |s|
        next if target.statStageAtMax?(s)
        avgStat -= target.stages[s]
        canChangeStat = true
      end
      if canChangeStat
        avgStat = avgStat/2 if avgStat<0   # More chance of getting even better
        score += avgStat*10
      else
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "038"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::DEFENSE)
          score -= 90
        else
          score += 40 if user.turnCount==0
          score -= user.stages[PBStats::DEFENSE]*30
        end
      else
        score += 10 if user.turnCount==0
        score += 30 if user.stages[PBStats::DEFENSE]<0
      end
    #---------------------------------------------------------------------------
    when "039"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::SPATK)
          score -= 90
        else
          score += 40 if user.turnCount==0
          score -= user.stages[PBStats::SPATK]*30
          if skill>=PBTrainerAI.mediumSkill
            hasSpecicalAttack = false
            user.eachMove do |m|
              next if !m.specialMove?(m.type)
              hasSpecicalAttack = true
              break
            end
            if hasSpecicalAttack
              score += 20
            elsif skill>=PBTrainerAI.highSkill
              score -= 90
            end
          end
        end
      else
        score += 10 if user.turnCount==0
        score += 30 if user.stages[PBStats::SPATK]<0
        if skill>=PBTrainerAI.mediumSkill
          hasSpecicalAttack = false
          user.eachMove do |m|
            next if !m.specialMove?(m.type)
            hasSpecicalAttack = true
            break
          end
          score += 30 if hasSpecicalAttack
        end
      end
    #---------------------------------------------------------------------------
    when "03A"
      if user.statStageAtMax?(PBStats::ATTACK) ||
         user.hp<=user.totalhp/2
        score -= 100
      else
        score += (6-user.stages[PBStats::ATTACK])*10
        if skill>=PBTrainerAI.mediumSkill
          hasPhysicalAttack = false
          user.eachMove do |m|
            next if !m.physicalMove?(m.type)
            hasPhysicalAttack = true
            break
          end
          if hasPhysicalAttack
            score += 60
          elsif skill>=PBTrainerAI.highSkill
            score -= 90
          end
        end
      end
    #---------------------------------------------------------------------------
    when "03B"
      avg =  user.stages[PBStats::ATTACK]*10
      avg += user.stages[PBStats::DEFENSE]*10
      score += avg/2
    #---------------------------------------------------------------------------
    when "03C"
      avg =  user.stages[PBStats::DEFENSE]*10
      avg += user.stages[PBStats::SPDEF]*10
      score += avg/2
    #---------------------------------------------------------------------------
    when "03D"
      avg =  user.stages[PBStats::DEFENSE]*10
      avg += user.stages[PBStats::SPEED]*10
      avg += user.stages[PBStats::SPDEF]*10
      score += (avg/3).floor
    #---------------------------------------------------------------------------
    when "03E"
      if @battle.field.effects[PBEffects::TrickRoom] > 0
        score += 60
      else
        score += user.stages[PBStats::SPEED]*10
      end
    #---------------------------------------------------------------------------
    when "03F"
      score += user.stages[PBStats::SPATK]*10
    #---------------------------------------------------------------------------
    when "040"
      if target.hasActiveAbility?(:CONTRARY)
        score += 30
        score += target.stages[PBStats::SPATK]*10
      else
        if !target.pbCanConfuse?(user,false)
          score -= 90
        else
          score += 30 if target.stages[PBStats::SPATK]<0
        end
      end
    #---------------------------------------------------------------------------
    when "041"
      if target.hasActiveAbility?(:CONTRARY)
        score += 30
        score += target.stages[PBStats::ATTACK]*10
      else
        if !target.pbCanConfuse?(user,false)
          score -= 90
        else
          score += 30 if target.stages[PBStats::ATTACK]<0
        end
      end
    #---------------------------------------------------------------------------
    when "042"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::ATTACK,user)
          score -= 60
        else
          score += target.stages[PBStats::ATTACK]*10
          if skill>=PBTrainerAI.mediumSkill
            hasPhysicalAttack = false
            target.eachMove do |m|
              next if !m.physicalMove?(m.type)
              hasPhysicalAttack = true
              break
            end
            if hasPhysicalAttack
              score += 20
            elsif skill>=PBTrainerAI.highSkill
              score -= 90
            end
          end
        end
      else
        score += 20 if target.stages[PBStats::ATTACK]>0
        if skill>=PBTrainerAI.mediumSkill
          hasPhysicalAttack = false
          target.eachMove do |m|
            next if !m.physicalMove?(m.type)
            hasPhysicalAttack = true
            break
          end
          score += 20 if hasPhysicalAttack
        end
      end
    #---------------------------------------------------------------------------
    when "043"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::DEFENSE,user)
          score -= 60
        else
          score += target.stages[PBStats::DEFENSE]*10
        end
      else
        score += 20 if target.stages[PBStats::DEFENSE]>0
      end
    #---------------------------------------------------------------------------
    when "044"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::SPEED,user)
          score -= 60
        else
          score += target.stages[PBStats::SPEED]*10
          if skill>=PBTrainerAI.highSkill
            aspeed = pbRoughStat(user,PBStats::SPEED,skill)
            ospeed = pbRoughStat(target,PBStats::SPEED,skill)
            score += 30 if aspeed<ospeed && aspeed*2>ospeed
          end
        end
      else
        score += 20 if user.stages[PBStats::SPEED]>0
      end
    #---------------------------------------------------------------------------
    when "045"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::SPATK,user)
          score -= 90
        else
          score += user.stages[PBStats::SPATK]*20
          if skill>=PBTrainerAI.mediumSkill
            hasSpecicalAttack = false
            target.eachMove do |m|
              next if !m.specialMove?(m.type)
              hasSpecicalAttack = true
              break
            end
            if hasSpecicalAttack
              score += 20
            elsif skill>=PBTrainerAI.highSkill
              score -= 90
            end
          end
        end
      else
        score += 20 if user.stages[PBStats::SPATK]>0
        if skill>=PBTrainerAI.mediumSkill
          hasSpecicalAttack = false
          target.eachMove do |m|
            next if !m.specialMove?(m.type)
            hasSpecicalAttack = true
            break
          end
          score += 20 if hasSpecicalAttack
        end
      end
    #---------------------------------------------------------------------------
    when "046"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::SPDEF,user)
          score -= 90
        else
          score += target.stages[PBStats::SPDEF]*20
        end
      else
        score += 20 if target.stages[PBStats::SPDEF]>0
      end
    #---------------------------------------------------------------------------
    when "047"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::ACCURACY,user)
          score -= 90
        else
          score += target.stages[PBStats::ACCURACY]*10
        end
      else
        score += 20 if target.stages[PBStats::ACCURACY]>0
      end
    #---------------------------------------------------------------------------
    when "048"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::EVASION,user)
          score -= 90
        else
          score += target.stages[PBStats::EVASION]*10
        end
      else
        score += 20 if target.stages[PBStats::EVASION]>0
      end
    #---------------------------------------------------------------------------
    when "049"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::EVASION,user)
          score -= 90
        else
          score += target.stages[PBStats::EVASION]*10
        end
      else
        score += 20 if target.stages[PBStats::EVASION]>0
      end
      score += 30 if target.pbOwnSide.effects[PBEffects::AuroraVeil]>0 ||
                     target.pbOwnSide.effects[PBEffects::Reflect]>0 ||
                     target.pbOwnSide.effects[PBEffects::LightScreen]>0 ||
                     target.pbOwnSide.effects[PBEffects::Mist]>0 ||
                     target.pbOwnSide.effects[PBEffects::Safeguard]>0
      score -= 30 if target.pbOwnSide.effects[PBEffects::Spikes]>0 ||
                     target.pbOwnSide.effects[PBEffects::ToxicSpikes]>0 ||
                     target.pbOwnSide.effects[PBEffects::StealthRock]
    #---------------------------------------------------------------------------
    when "04A"
      avg =  target.stages[PBStats::ATTACK]*10
      avg += target.stages[PBStats::DEFENSE]*10
      score += avg/2
    #---------------------------------------------------------------------------
    when "04B"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::ATTACK,user)
          score -= 90
        else
          score += 40 if user.turnCount==0
          score += target.stages[PBStats::ATTACK]*20
          if skill>=PBTrainerAI.mediumSkill
            hasPhysicalAttack = false
            target.eachMove do |m|
              next if !m.physicalMove?(m.type)
              hasPhysicalAttack = true
              break
            end
            if hasPhysicalAttack
              score += 20
            elsif skill>=PBTrainerAI.highSkill
              score -= 90
            end
          end
        end
      else
        score += 10 if user.turnCount==0
        score += 20 if target.stages[PBStats::ATTACK]>0
        if skill>=PBTrainerAI.mediumSkill
          hasPhysicalAttack = false
          target.eachMove do |m|
            next if !m.physicalMove?(m.type)
            hasPhysicalAttack = true
            break
          end
          score += 20 if hasPhysicalAttack
        end
      end
    #---------------------------------------------------------------------------
    when "04C"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::DEFENSE,user)
          score -= 90
        else
          score += 40 if user.turnCount==0
          score += target.stages[PBStats::DEFENSE]*20
        end
      else
        score += 10 if user.turnCount==0
        score += 20 if target.stages[PBStats::DEFENSE]>0
      end
    #---------------------------------------------------------------------------
    when "04D"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::SPEED,user)
          score -= 90
        else
          score += 20 if user.turnCount==0
          score += target.stages[PBStats::SPEED]*20
          if skill>=PBTrainerAI.highSkill
            aspeed = pbRoughStat(user,PBStats::SPEED,skill)
            ospeed = pbRoughStat(target,PBStats::SPEED,skill)
            score += 30 if aspeed<ospeed && aspeed*2>ospeed
          end
        end
      else
        score += 10 if user.turnCount==0
        score += 30 if target.stages[PBStats::SPEED]>0
      end
    #---------------------------------------------------------------------------
    when "04E"
      if user.gender==2 || target.gender==2 || user.gender==target.gender ||
         target.hasActiveAbility?(:OBLIVIOUS)
        score -= 90
      elsif move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::SPATK,user)
          score -= 90
        else
          score += 40 if user.turnCount==0
          score += target.stages[PBStats::SPATK]*20
          if skill>=PBTrainerAI.mediumSkill
            hasSpecicalAttack = false
            target.eachMove do |m|
              next if !m.specialMove?(m.type)
              hasSpecicalAttack = true
              break
            end
            if hasSpecicalAttack
              score += 20
            elsif skill>=PBTrainerAI.highSkill
              score -= 90
            end
          end
        end
      else
        score += 10 if user.turnCount==0
        score += 20 if target.stages[PBStats::SPATK]>0
        if skill>=PBTrainerAI.mediumSkill
          hasSpecicalAttack = false
          target.eachMove do |m|
            next if !m.specialMove?(m.type)
            hasSpecicalAttack = true
            break
          end
          score += 30 if hasSpecicalAttack
        end
      end
    #---------------------------------------------------------------------------
    when "04F"
      if move.statusMove?
        if !target.pbCanLowerStatStage?(PBStats::SPDEF,user)
          score -= 90
        else
          score += 40 if user.turnCount==0
          score += target.stages[PBStats::SPDEF]*20
        end
      else
        score += 10 if user.turnCount==0
        score += 20 if target.stages[PBStats::SPDEF]>0
      end
    #---------------------------------------------------------------------------
    when "050"
      if target.effects[PBEffects::Substitute]>0
        score -= 90
      else
        avg = 0; anyChange = false
        PBStats.eachBattleStat do |s|
          next if target.stages[s]==0
          avg += target.stages[s]
          anyChange = true
        end
        if anyChange
          score += avg*10
        else
          score -= 90
        end
      end
    #---------------------------------------------------------------------------
    when "051"
      if skill>=PBTrainerAI.mediumSkill
        stages = 0
        @battle.eachBattler do |b|
          totalStages = 0
          PBStats.eachBattleStat { |s| totalStages += b.stages[s] }
          if b.opposes?(user)
            stages += totalStages
          else
            stages -= totalStages
          end
        end
        score += stages*10
      end
    #---------------------------------------------------------------------------
    when "052"
      if skill>=PBTrainerAI.mediumSkill
        aatk = user.stages[PBStats::ATTACK]
        aspa = user.stages[PBStats::SPATK]
        oatk = target.stages[PBStats::ATTACK]
        ospa = target.stages[PBStats::SPATK]
        if aatk>=oatk && aspa>=ospa
          score -= 80
        else
          score += (oatk-aatk)*10
          score += (ospa-aspa)*10
        end
      else
        score -= 50
      end
    #---------------------------------------------------------------------------
    when "053"
      if skill>=PBTrainerAI.mediumSkill
        adef = user.stages[PBStats::DEFENSE]
        aspd = user.stages[PBStats::SPDEF]
        odef = target.stages[PBStats::DEFENSE]
        ospd = target.stages[PBStats::SPDEF]
        if adef>=odef && aspd>=ospd
          score -= 80
        else
          score += (odef-adef)*10
          score += (ospd-aspd)*10
        end
      else
        score -= 50
      end
    #---------------------------------------------------------------------------
    when "054"
      if skill>=PBTrainerAI.mediumSkill
        userStages = 0; targetStages = 0
        PBStats.eachBattleStat do |s|
          userStages   += user.stages[s]
          targetStages += target.stages[s]
        end
        score += (targetStages-userStages)*10
      else
        score -= 50
      end
    #---------------------------------------------------------------------------
    when "055"
      if skill>=PBTrainerAI.mediumSkill
        equal = true
        PBStats.eachBattleStat do |s|
          stagediff = target.stages[s]-user.stages[s]
          score += stagediff*10
          equal = false if stagediff!=0
        end
        score -= 80 if equal
      else
        score -= 50
      end
    #---------------------------------------------------------------------------
    when "056"
      score -= 80 if user.pbOwnSide.effects[PBEffects::Mist]>0
    #---------------------------------------------------------------------------
    when "057"
      if skill>=PBTrainerAI.mediumSkill
        aatk = pbRoughStat(user,PBStats::ATTACK,skill)
        adef = pbRoughStat(user,PBStats::DEFENSE,skill)
        if aatk==adef ||
           user.effects[PBEffects::PowerTrick]   # No flip-flopping
          score -= 90
        elsif adef>aatk   # Prefer a higher Attack
          score += 30
        else
          score -= 30
        end
      else
        score -= 30
      end
    #---------------------------------------------------------------------------
    when "058"
      if skill>=PBTrainerAI.mediumSkill
        aatk   = pbRoughStat(user,PBStats::ATTACK,skill)
        aspatk = pbRoughStat(user,PBStats::SPATK,skill)
        oatk   = pbRoughStat(target,PBStats::ATTACK,skill)
        ospatk = pbRoughStat(target,PBStats::SPATK,skill)
        if aatk<oatk && aspatk<ospatk
          score += 50
        elsif aatk+aspatk<oatk+ospatk
          score += 30
        else
          score -= 50
        end
      else
        score -= 30
      end
    #---------------------------------------------------------------------------
    when "059"
      if skill>=PBTrainerAI.mediumSkill
        adef   = pbRoughStat(user,PBStats::DEFENSE,skill)
        aspdef = pbRoughStat(user,PBStats::SPDEF,skill)
        odef   = pbRoughStat(target,PBStats::DEFENSE,skill)
        ospdef = pbRoughStat(target,PBStats::SPDEF,skill)
        if adef<odef && aspdef<ospdef
          score += 50
        elsif adef+aspdef<odef+ospdef
          score += 30
        else
          score -= 50
        end
      else
        score -= 30
      end
    #---------------------------------------------------------------------------
    when "05A"
      if target.effects[PBEffects::Substitute]>0
        score -= 90
      elsif user.hp>=(user.hp+target.hp)/2
        score -= 90
      elsif user.hp<=target.hp/3
        score += 60
      else
        score += 30
      end
    #---------------------------------------------------------------------------
    when "05B"
      if user.pbOwnSide.effects[PBEffects::Tailwind]>0
        score -= 100
      else
        score += 30 * (5 - user.turnCount)
        score += 50 if user.hp < user.totalhp / 4
      end
    #---------------------------------------------------------------------------
    when "05C"
      moveBlacklist = [
         "002",   # Struggle
         "014",   # Chatter
         "05C",   # Mimic
         "05D",   # Sketch
         "0B6"    # Metronome
      ]
      lastMoveData = pbGetMoveData(target.lastRegularMoveUsed)
      if user.effects[PBEffects::Transform] ||
         target.lastRegularMoveUsed<=0 ||
         moveBlacklist.include?(lastMoveData[MOVE_FUNCTION_CODE]) ||
         isConst?(lastMoveData[MOVE_TYPE],PBTypes,:SHADOW)
        score -= 90
      end
      user.eachMove do |m|
        next if m.id!=target.lastRegularMoveUsed
        score -= 90
        break
      end
    #---------------------------------------------------------------------------
    when "05D"
      moveBlacklist = [
         "002",   # Struggle
         "014",   # Chatter
         "05D"    # Sketch
      ]
      lastMoveData = pbGetMoveData(target.lastRegularMoveUsed)
      if user.effects[PBEffects::Transform] ||
         target.lastRegularMoveUsed<=0 ||
         moveBlacklist.include?(lastMoveData[MOVE_FUNCTION_CODE]) ||
         isConst?(lastMoveData[MOVE_TYPE],PBTypes,:SHADOW)
        score -= 90
      end
      user.eachMove do |m|
        next if m.id!=target.lastRegularMoveUsed
        score -= 90   # User already knows the move that will be Sketched
        break
      end
    #---------------------------------------------------------------------------
    when "05E"
      if isConst?(user.ability,PBAbilities,:MULTITYPE) ||
         isConst?(user.ability,PBAbilities,:RKSSYSTEM)
        score -= 90
      else
        types = []
        user.eachMove do |m|
          next if m.id==@id
          next if PBTypes.isPseudoType?(m.type)
          next if user.pbHasType?(m.type)
          types.push(m.type) if !types.include?(m.type)
        end
        score -= 90 if types.length==0
      end
    #---------------------------------------------------------------------------
    when "05F"
      if isConst?(user.ability,PBAbilities,:MULTITYPE) ||
         isConst?(user.ability,PBAbilities,:RKSSYSTEM)
        score -= 90
      elsif target.lastMoveUsed<=0 ||
         PBTypes.isPseudoType?(pbGetMoveData(target.lastMoveUsed,MOVE_TYPE))
        score -= 90
      else
        aType = -1
        target.eachMove do |m|
          next if m.id!=target.lastMoveUsed
          aType = m.pbCalcType(user)
          break
        end
        if aType<0
          score -= 90
        else
          types = []
          for i in 0..PBTypes.maxValue
            next if user.pbHasType?(i)
            types.push(i) if PBTypes.resistant?(aType,i)
          end
          score -= 90 if types.length==0
        end
      end
    #---------------------------------------------------------------------------
    when "060"
      if isConst?(user.ability,PBAbilities,:MULTITYPE) ||
         isConst?(user.ability,PBAbilities,:RKSSYSTEM)
        score -= 90
      elsif skill>=PBTrainerAI.mediumSkill
        envtypes = [
           :NORMAL, # None
           :GRASS,  # Grass
           :GRASS,  # Tall grass
           :WATER,  # Moving water
           :WATER,  # Still water
           :WATER,  # Underwater
           :ROCK,   # Rock
           :ROCK,   # Cave
           :GROUND  # Sand
        ]
        type = envtypes[@battle.environment]
        score -= 90 if user.pbHasType?(type)
      end
    #---------------------------------------------------------------------------
    when "061"
      if target.effects[PBEffects::Substitute]>0 ||
         isConst?(target.ability,PBAbilities,:MULTITYPE) ||
         isConst?(target.ability,PBAbilities,:RKSSYSTEM)
        score -= 90
      elsif target.pbHasType?(:WATER)
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "062"
      if isConst?(user.ability,PBAbilities,:MULTITYPE) ||
         isConst?(user.ability,PBAbilities,:RKSSYSTEM)
        score -= 90
      elsif user.pbHasType?(target.type1) &&
         user.pbHasType?(target.type2) &&
         target.pbHasType?(user.type1) &&
         target.pbHasType?(user.type2)
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "063"
      if target.effects[PBEffects::Substitute]>0
        score -= 90
      elsif skill>=PBTrainerAI.mediumSkill
        if isConst?(target.ability,PBAbilities,:MULTITYPE) ||
           isConst?(target.ability,PBAbilities,:RKSSYSTEM) ||
           isConst?(target.ability,PBAbilities,:SIMPLE) ||
           isConst?(target.ability,PBAbilities,:TRUANT)
          score -= 90
        end
      end
    #---------------------------------------------------------------------------
    when "064"
      if target.effects[PBEffects::Substitute]>0
        score -= 90
      elsif skill>=PBTrainerAI.mediumSkill
        if isConst?(target.ability,PBAbilities,:INSOMNIA) ||
           isConst?(target.ability,PBAbilities,:MULTITYPE) ||
           isConst?(target.ability,PBAbilities,:RKSSYSTEM) ||
           isConst?(target.ability,PBAbilities,:TRUANT)
          score -= 90
        end
      end
    #---------------------------------------------------------------------------
    when "065"
      score -= 40   # don't prefer this move
      if skill>=PBTrainerAI.mediumSkill
        if target.ability==0 || user.ability==target.ability ||
           isConst?(user.ability,PBAbilities,:MULTITYPE) ||
           isConst?(user.ability,PBAbilities,:RKSSYSTEM) ||
           isConst?(target.ability,PBAbilities,:FLOWERGIFT) ||
           isConst?(target.ability,PBAbilities,:FORECAST) ||
           isConst?(target.ability,PBAbilities,:ILLUSION) ||
           isConst?(target.ability,PBAbilities,:IMPOSTER) ||
           isConst?(target.ability,PBAbilities,:MULTITYPE) ||
           isConst?(target.ability,PBAbilities,:RKSSYSTEM) ||
           isConst?(target.ability,PBAbilities,:TRACE) ||
           isConst?(target.ability,PBAbilities,:WONDERGUARD) ||
           isConst?(target.ability,PBAbilities,:ZENMODE) ||
           isConst?(target.ability,PBAbilities,:ICEFACE) ||
           isConst?(target.ability,PBAbilities,:FLAMEVEIL) ||
           isConst?(target.ability,PBAbilities,:GULPMISSILE) ||
           isConst?(target.ability,PBAbilities,:NEUTRALIZINGGAS)
          score -= 90
        end
      end
      if skill>=PBTrainerAI.highSkill
        if isConst?(target.ability,PBAbilities,:TRUANT) &&
           user.opposes?(target)
          score -= 90
        elsif isConst?(target.ability,PBAbilities,:SLOWSTART) &&
           user.opposes?(target)
          score -= 90
        end
      end
    #---------------------------------------------------------------------------
    when "066"
      score -= 40   # don't prefer this move
      if target.effects[PBEffects::Substitute]>0
        score -= 90
      elsif skill>=PBTrainerAI.mediumSkill
        if user.ability==0 || user.ability==target.ability ||
           isConst?(target.ability,PBAbilities,:MULTITYPE) ||
           isConst?(target.ability,PBAbilities,:RKSSYSTEM) ||
           isConst?(target.ability,PBAbilities,:TRUANT) ||
           isConst?(user.ability,PBAbilities,:FLOWERGIFT) ||
           isConst?(user.ability,PBAbilities,:FORECAST) ||
           isConst?(user.ability,PBAbilities,:ILLUSION) ||
           isConst?(user.ability,PBAbilities,:IMPOSTER) ||
           isConst?(user.ability,PBAbilities,:MULTITYPE) ||
           isConst?(user.ability,PBAbilities,:RKSSYSTEM) ||
           isConst?(user.ability,PBAbilities,:TRACE) ||
           isConst?(user.ability,PBAbilities,:ZENMODE) ||
           isConst?(user.ability,PBAbilities,:ICEFACE) ||
           isConst?(user.ability,PBAbilities,:GULPMISSILE) ||
           isConst?(user.ability,PBAbilities,:NEUTRALIZINGGAS)
          score -= 90
        end
        if skill>=PBTrainerAI.highSkill
          if isConst?(user.ability,PBAbilities,:TRUANT) &&
             user.opposes?(target)
            score += 90
          elsif isConst?(user.ability,PBAbilities,:SLOWSTART) &&
             user.opposes?(target)
            score += 90
          end
        end
      end
    #---------------------------------------------------------------------------
    when "067"
      score -= 40   # don't prefer this move
      if skill>=PBTrainerAI.mediumSkill
        if (user.ability==0 && target.ability==0) ||
           user.ability==target.ability ||
           isConst?(user.ability,PBAbilities,:ILLUSION) ||
           isConst?(user.ability,PBAbilities,:MULTITYPE) ||
           isConst?(user.ability,PBAbilities,:RKSSYSTEM) ||
           isConst?(user.ability,PBAbilities,:WONDERGUARD) ||
           isConst?(target.ability,PBAbilities,:ILLUSION) ||
           isConst?(target.ability,PBAbilities,:MULTITYPE) ||
           isConst?(target.ability,PBAbilities,:RKSSYSTEM) ||
           isConst?(target.ability,PBAbilities,:WONDERGUARD) ||
           isConst?(target.ability,PBAbilities,:ICEFACE) ||
           isConst?(target.ability,PBAbilities,:GULPMISSILE) ||
           isConst?(target.ability,PBAbilities,:NEUTRALIZINGGAS)
          score -= 90
        end
      end
      if skill>=PBTrainerAI.highSkill
        if isConst?(target.ability,PBAbilities,:TRUANT) &&
           user.opposes?(target)
          score -= 90
        elsif isConst?(target.ability,PBAbilities,:SLOWSTART) &&
          user.opposes?(target)
          score -= 90
        end
      end
    #---------------------------------------------------------------------------
    when "068"
      if target.effects[PBEffects::Substitute]>0 ||
         target.effects[PBEffects::GastroAcid]
        score -= 90
      elsif skill>=PBTrainerAI.highSkill
        score -= 90 if isConst?(target.ability,PBAbilities,:MULTITYPE)
        score -= 90 if isConst?(target.ability,PBAbilities,:RKSSYSTEM)
        score -= 90 if isConst?(target.ability,PBAbilities,:SLOWSTART)
        score -= 90 if isConst?(target.ability,PBAbilities,:TRUANT)
      end
    #---------------------------------------------------------------------------
    when "069"
      score -= 70
    #---------------------------------------------------------------------------
    when "06A"
      if target.hp<=20
        score += 80
      elsif target.level>=25
        score -= 60   # Not useful against high-level Pokemon
      end
    #---------------------------------------------------------------------------
    when "06B"
      score += 80 if target.hp<=40
    #---------------------------------------------------------------------------
    when "06C"
      score -= 150
      score += target.hp*100/target.totalhp
    #---------------------------------------------------------------------------
    when "06D"
      score += 80 if target.hp<=user.level
    #---------------------------------------------------------------------------
    when "06E"
      if user.hp>=target.hp
        score -= 90
      elsif user.hp<target.hp/2
        score += 100
      elsif target.pbHasType?(:GHOST)
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "06F"
      score += 30 if target.hp<=user.level
    #---------------------------------------------------------------------------
    when "070"
      score -= 90 if target.hasActiveAbility?(:STURDY)
      score -= 90 if target.level>user.level
    #---------------------------------------------------------------------------
    when "071"
      if target.effects[PBEffects::HyperBeam]>0
        score -= 90
      else
        attack = pbRoughStat(user,PBStats::ATTACK,skill)
        spatk  = pbRoughStat(user,PBStats::SPATK,skill)
        if attack*1.5<spatk
          score -= 60
        elsif skill>=PBTrainerAI.mediumSkill && target.lastMoveUsed>0
          moveData = pbGetMoveData(target.lastMoveUsed)
          if moveData[MOVE_BASE_DAMAGE]>0 &&
             (MOVE_CATEGORY_PER_MOVE && moveData[MOVE_CATEGORY]==0) ||
             (!MOVE_CATEGORY_PER_MOVE && PBTypes.isPhysicalType?(moveData[MOVE_TYPE]))
            score += 60
          else
            score -= 60
          end
          score += 30 if moveData[MOVE_BASE_DAMAGE]>= target.hp / 2
          score += 30 if moveData[MOVE_BASE_DAMAGE]>= target.hp
        end
      end
    #---------------------------------------------------------------------------
    when "072"
      if target.effects[PBEffects::HyperBeam]>0
        score -= 90
      else
        attack = pbRoughStat(user,PBStats::ATTACK,skill)
        spatk  = pbRoughStat(user,PBStats::SPATK,skill)
        if attack>spatk*1.5
          score -= 60
        elsif skill>=PBTrainerAI.mediumSkill && target.lastMoveUsed>0
          moveData = pbGetMoveData(target.lastMoveUsed)
          if moveData[MOVE_BASE_DAMAGE]>0 &&
             (MOVE_CATEGORY_PER_MOVE && moveData[MOVE_CATEGORY]==1) ||
             (!MOVE_CATEGORY_PER_MOVE && !PBTypes.isSpecialType?(moveData[MOVE_TYPE]))
            score += 60
          else
            score -= 60
          end
          score += 30 if moveData[MOVE_BASE_DAMAGE]>= target.hp / 2
          score += 30 if moveData[MOVE_BASE_DAMAGE]>= target.hp
        end
      end
    #---------------------------------------------------------------------------
    when "073"
      score -= 90 if target.effects[PBEffects::HyperBeam]>0
    #---------------------------------------------------------------------------
    when "074"
      target.eachAlly do |b|
        next if !b.near?(target)
        score += 10
      end
    #---------------------------------------------------------------------------
    when "075"
    #---------------------------------------------------------------------------
    when "076"
    #---------------------------------------------------------------------------
    when "077"
    #---------------------------------------------------------------------------
    when "078"
      if skill>=PBTrainerAI.highSkill
        score += 30 if !target.hasActiveAbility?(:INNERFOCUS) &&
                       target.effects[PBEffects::Substitute]==0
      end
    #---------------------------------------------------------------------------
    when "079"
    #---------------------------------------------------------------------------
    when "07A"
    #---------------------------------------------------------------------------
    when "07B"
    #---------------------------------------------------------------------------
    when "07C"
      score -= 20 if target.status==PBStatuses::PARALYSIS   # Will cure status
    #---------------------------------------------------------------------------
    when "07D"
      score -= 20 if target.status==PBStatuses::SLEEP &&   # Will cure status
                     target.statusCount>1
    #---------------------------------------------------------------------------
    when "07E"
    #---------------------------------------------------------------------------
    when "07F"
    #---------------------------------------------------------------------------
    when "080"
    #---------------------------------------------------------------------------
    when "081"
      attspeed = pbRoughStat(user,PBStats::SPEED,skill)
      oppspeed = pbRoughStat(target,PBStats::SPEED,skill)
      score += 30 if oppspeed>attspeed
    #---------------------------------------------------------------------------
    when "082"
      score += 20 if @battle.pbOpposingBattlerCount(user)>1
    #---------------------------------------------------------------------------
    when "083"
      if skill>=PBTrainerAI.mediumSkill
        user.eachAlly do |b|
          next if !b.pbHasMove?(move.id)
          score += 20
        end
      end
    #---------------------------------------------------------------------------
    when "084"
      attspeed = pbRoughStat(user,PBStats::SPEED,skill)
      oppspeed = pbRoughStat(target,PBStats::SPEED,skill)
      score += 30 if oppspeed>attspeed
    #---------------------------------------------------------------------------
    when "085"
    #---------------------------------------------------------------------------
    when "086"
    #---------------------------------------------------------------------------
    when "087"
    #---------------------------------------------------------------------------
    when "088"
    #---------------------------------------------------------------------------
    when "089"
    #---------------------------------------------------------------------------
    when "08A"
    #---------------------------------------------------------------------------
    when "08B"
    #---------------------------------------------------------------------------
    when "08C"
    #---------------------------------------------------------------------------
    when "08D"
    #---------------------------------------------------------------------------
    when "08E"
    #---------------------------------------------------------------------------
   when "08F"
      PBStats.eachBattleStat do |s|
        next if target.stages[s] <= 0
        score += target.stages[s] * 10
      end
    #---------------------------------------------------------------------------
    when "090"
    #---------------------------------------------------------------------------
    when "091"
    #---------------------------------------------------------------------------
    when "092"
    #---------------------------------------------------------------------------
    when "093"
      score += 25 if user.effects[PBEffects::Rage]
    #---------------------------------------------------------------------------
    when "094"
    #---------------------------------------------------------------------------
    when "095"
    #---------------------------------------------------------------------------
    when "096"
      score -= 90 if !pbIsBerry?(user.item) || !user.itemActive?
    #---------------------------------------------------------------------------
    when "097"
    #---------------------------------------------------------------------------
    when "098"
    #---------------------------------------------------------------------------
    when "099"
    #---------------------------------------------------------------------------
    when "09A"
    #---------------------------------------------------------------------------
    when "09B"
    #---------------------------------------------------------------------------
    when "09C"
      hasAlly = false
      user.eachAlly do |b|
        hasAlly = true
        score += 30
        break
      end
      score -= 90 if !hasAlly
    #---------------------------------------------------------------------------
    when "09D"
      score -= 90 if user.effects[PBEffects::MudSport]
    #---------------------------------------------------------------------------
    when "09E"
      score -= 90 if user.effects[PBEffects::WaterSport]
    #---------------------------------------------------------------------------
    when "09F"
      if user.isSpecies?(:ARCEUS)
        score += 40
      end
    #---------------------------------------------------------------------------
    when "0A0"
    #---------------------------------------------------------------------------
    when "0A1"
      score -= 90 if user.pbOwnSide.effects[PBEffects::LuckyChant]>0
    #---------------------------------------------------------------------------
    when "0A2"
      score -= 90 if user.pbOwnSide.effects[PBEffects::Reflect]>0
    #---------------------------------------------------------------------------
    when "0A3"
      score -= 90 if user.pbOwnSide.effects[PBEffects::LightScreen]>0
    #---------------------------------------------------------------------------
    when "0A4"
    #---------------------------------------------------------------------------
    when "0A5"
    #---------------------------------------------------------------------------
    when "0A6"
      score -= 90 if target.effects[PBEffects::Substitute]>0
      score -= 90 if user.effects[PBEffects::LockOn]>0
    #---------------------------------------------------------------------------
    when "0A7"
      if target.effects[PBEffects::Foresight]
        score -= 90
      elsif target.pbHasType?(:GHOST)
        score += 70
      elsif target.stages[PBStats::EVASION]<=0
        score -= 60
      end
    #---------------------------------------------------------------------------
    when "0A8"
      if target.effects[PBEffects::MiracleEye]
        score -= 90
      elsif target.pbHasType?(:DARK)
        score += 70
      elsif target.stages[PBStats::EVASION]<=0
        score -= 60
      end
    #---------------------------------------------------------------------------
    when "0A9"
    #---------------------------------------------------------------------------
    when "0AA"
      if user.effects[PBEffects::ProtectRate]>1 ||
         target.effects[PBEffects::HyperBeam]>0
        score -= 90
      else
        if skill>=PBTrainerAI.mediumSkill
          score -= user.effects[PBEffects::ProtectRate]*40
        end
        score += 50 if user.turnCount==0
        score += 30 if target.effects[PBEffects::TwoTurnAttack]>0
      end
    #---------------------------------------------------------------------------
    when "0AB"
    #---------------------------------------------------------------------------
    when "0AC"
    #---------------------------------------------------------------------------
    when "0AD"
    #---------------------------------------------------------------------------
    when "0AE"
      score -= 40
      if skill>=PBTrainerAI.highSkill
        score -= 100 if target.lastRegularMoveUsed<=0 ||
           !pbGetMoveData(target.lastRegularMoveUsed,MOVE_FLAGS)[/e/]   # Not copyable by Mirror Move
      end
    #---------------------------------------------------------------------------
    when "0AF"
    #---------------------------------------------------------------------------
    when "0B0"
    #---------------------------------------------------------------------------
    when "0B1"
    #---------------------------------------------------------------------------
    when "0B2"
    #---------------------------------------------------------------------------
    when "0B3"
    #---------------------------------------------------------------------------
    when "0B4"
      if user.asleep?
        score += 100   # Because it can only be used while asleep
      else
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "0B5"
    #---------------------------------------------------------------------------
    when "0B6"
    #---------------------------------------------------------------------------
    when "0B7"
      score -= 90 if target.effects[PBEffects::Torment]
    #---------------------------------------------------------------------------
    when "0B8"
      score -= 90 if user.effects[PBEffects::Imprison]
    #---------------------------------------------------------------------------
    when "0B9"
      score -= 90 if target.effects[PBEffects::Disable]>0
    #---------------------------------------------------------------------------
    when "0BA"
      score -= 90 if target.effects[PBEffects::Taunt]>0
    #---------------------------------------------------------------------------
    when "0BB"
      score -= 90 if target.effects[PBEffects::HealBlock]>0
    #---------------------------------------------------------------------------
    when "0BC"
      aspeed = pbRoughStat(user,PBStats::SPEED,skill)
      ospeed = pbRoughStat(target,PBStats::SPEED,skill)
      if target.effects[PBEffects::Encore]>0
        score -= 90
      elsif aspeed>ospeed
        if target.lastMoveUsed<=0
          score -= 90
        else
          moveData = pbGetMoveData(target.lastRegularMoveUsed)
          if moveData[MOVE_CATEGORY]==2 &&   # Status move
             (moveData[MOVE_TARGET]==PBTargets::User ||
             moveData[MOVE_TARGET]==PBTargets::BothSides)
            score += 60
          elsif moveData[MOVE_CATEGORY]!=2 &&   # Damaging move
             moveData[MOVE_TARGET]==PBTargets::NearOther &&
             PBTypes.ineffective?(pbCalcTypeMod(moveData[MOVE_TYPE],target,user))
            score += 60
          end
        end
      end
    #---------------------------------------------------------------------------
    when "0BD"
    #---------------------------------------------------------------------------
    when "0BF"
    #---------------------------------------------------------------------------
    when "0C0"
    #---------------------------------------------------------------------------
    when "0C1"
    #---------------------------------------------------------------------------
    when "0C2"
    #---------------------------------------------------------------------------
    when "0C3"
    #---------------------------------------------------------------------------
    when "0C4"
    #---------------------------------------------------------------------------
    when "0C7"
      score += 20 if user.effects[PBEffects::FocusEnergy]>0
      if skill>=PBTrainerAI.highSkill
        score += 20 if !target.hasActiveAbility?(:INNERFOCUS) &&
                       target.effects[PBEffects::Substitute]==0
      end
    #---------------------------------------------------------------------------
    when "0C9"
    #---------------------------------------------------------------------------
    when "0CA"
    #---------------------------------------------------------------------------
    when "0CB"
    #---------------------------------------------------------------------------
    when "0CC"
    #---------------------------------------------------------------------------
    when "0CD"
    #---------------------------------------------------------------------------
    when "0CE"
    #---------------------------------------------------------------------------
    when "0CF"
      score += 40 if target.effects[PBEffects::Trapping]==0
    #---------------------------------------------------------------------------
    when "0D0"
      score += 40 if target.effects[PBEffects::Trapping]==0
    #---------------------------------------------------------------------------
    when "0D1"
    #---------------------------------------------------------------------------
    when "0D2"
    #---------------------------------------------------------------------------
    when "0D3"
    #---------------------------------------------------------------------------
    when "0D4"
      if user.hp<=user.totalhp/4
        score -= 90
      elsif user.hp<=user.totalhp/2
        score -= 50
      end
    #---------------------------------------------------------------------------
    when "0D5", "0D6"
      if user.hp==user.totalhp || (skill>=PBTrainerAI.mediumSkill && !user.canHeal?)
        score -= 90
      else
        score += 50
        score -= user.hp*100/user.totalhp
      end
    #---------------------------------------------------------------------------
    when "0D7"
      score -= 90 if @battle.positions[user.index].effects[PBEffects::Wish]>0
    #---------------------------------------------------------------------------
    when "0D8"
      if user.hp==user.totalhp || (skill>=PBTrainerAI.mediumSkill && !user.canHeal?)
        score -= 90
      else
        case @battle.pbWeather
        when PBWeather::Sun, PBWeather::HarshSun
          score += 30
        when PBWeather::None
        else
          score -= 30
        end
        score += 50
        score -= user.hp*100/user.totalhp
      end
    #---------------------------------------------------------------------------
    when "0D9"
      if user.hp==user.totalhp || !user.pbCanSleep?(user,false,nil,true)
        score -= 90
      else
        score += 70
        score -= user.hp*140/user.totalhp
        score += 30 if user.status!=0
      end
    #---------------------------------------------------------------------------
    when "0DA"
      score -= 90 if user.effects[PBEffects::AquaRing]
    #---------------------------------------------------------------------------
    when "0DB"
      score -= 90 if user.effects[PBEffects::Ingrain]
    #---------------------------------------------------------------------------
    when "0DC"
      if target.effects[PBEffects::LeechSeed]>=0
        score -= 90
      elsif skill>=PBTrainerAI.mediumSkill && target.pbHasType?(:GRASS)
        score -= 90
      else
        score += 60 if user.turnCount==0
      end
    #---------------------------------------------------------------------------
    when "0DD"
      if skill>=PBTrainerAI.mediumSkill && target.hasActiveAbility?(:LIQUIDOOZE)
        score -= 90
      else
        score += 20 if user.hp<=user.totalhp/2
      end
    #---------------------------------------------------------------------------
    when "0DE"
      if !target.asleep?
        score -= 100
      elsif skill>=PBTrainerAI.highSkill && target.hasActiveAbility?(:LIQUIDOOZE)
        score -= 70
      else
        score += 20 if user.hp<=user.totalhp/2
      end
    #---------------------------------------------------------------------------
    when "0DF"
      if user.opposes?(target)
        score -= 100
      else
        score += 20 if target.hp<target.totalhp/2 &&
                       target.effects[PBEffects::Substitute]==0
      end
    #---------------------------------------------------------------------------
    when "0E0"
      reserves = @battle.pbAbleNonActiveCount(user.idxOwnSide)
      foes     = @battle.pbAbleNonActiveCount(user.idxOpposingSide)
      if @battle.pbCheckGlobalAbility(:DAMP)
        score -= 100
      elsif skill>=PBTrainerAI.mediumSkill && reserves==0 && foes>0
        score -= 100   # don't want to lose
      elsif skill>=PBTrainerAI.highSkill && reserves==0 && foes==0
        score += 80   # want to draw
      else
        score -= user.hp*100/user.totalhp
      end
    #---------------------------------------------------------------------------
    when "0E1"
    #---------------------------------------------------------------------------
    when "0E2"
      if !user.isSpecies?(:CHANDELURE)
        if !target.pbCanLowerStatStage?(PBStats::ATTACK,user) &&
           !target.pbCanLowerStatStage?(PBStats::SPATK,user)
          score -= 100
        elsif @battle.pbAbleNonActiveCount(user.idxOwnSide)==0
          score -= 100
        else
          score += target.stages[PBStats::ATTACK]*10
          score += target.stages[PBStats::SPATK]*10
          score -= user.hp*80/user.totalhp
        end
      end
    #---------------------------------------------------------------------------
    when "0E3", "0E4"
      score -= 70
    #---------------------------------------------------------------------------
    when "0E5"
      if @battle.pbAbleNonActiveCount(user.idxOwnSide)==0
        score -= 90
      else
        score -= 90 if target.effects[PBEffects::PerishSong]>0
      end
    #---------------------------------------------------------------------------
    when "0E6"
      score += 50
      score -= user.hp*100/user.totalhp
      score += 30 if user.hp<=user.totalhp/10
    #---------------------------------------------------------------------------
    when "0E7"
      score += 50
      score -= user.hp*100/user.totalhp
      score += 30 if user.hp<=user.totalhp/10
    #---------------------------------------------------------------------------
    when "0E8"
      score -= 25 if user.hp>user.totalhp/2
      if skill>=PBTrainerAI.mediumSkill
        score -= 90 if user.effects[PBEffects::ProtectRate]>1
        score -= 90 if target.effects[PBEffects::HyperBeam]>0
      else
        score -= user.effects[PBEffects::ProtectRate]*40
      end
    #---------------------------------------------------------------------------
    when "0E9"
      if target.hp==1
        score -= 90
      elsif target.hp<=target.totalhp/8
        score -= 60
      elsif target.hp<=target.totalhp/4
        score -= 30
      end
    #---------------------------------------------------------------------------
    when "0EA"
      score -= 100# if @battle.trainerBattle?
    #---------------------------------------------------------------------------
    when "0EB"
      if target.effects[PBEffects::Ingrain] ||
         (skill>=PBTrainerAI.highSkill && target.hasActiveAbility?(:SUCTIONCUPS))
        score -= 90
      else
        ch = 0
        @battle.pbParty(target.index).each_with_index do |pkmn,i|
          ch += 1 if @battle.pbCanSwitchLax?(target.index,i)
        end
        score -= 90 if ch==0
      end
      if score>20
        score += 50 if target.pbOwnSide.effects[PBEffects::Spikes]>0
        score += 50 if target.pbOwnSide.effects[PBEffects::ToxicSpikes]>0
        score += 50 if target.pbOwnSide.effects[PBEffects::StealthRock]
      end
    #---------------------------------------------------------------------------
    when "0EC"
      if !target.effects[PBEffects::Ingrain] &&
         !(skill>=PBTrainerAI.highSkill && target.hasActiveAbility?(:SUCTIONCUPS))
        score += 40 if target.pbOwnSide.effects[PBEffects::Spikes]>0
        score += 40 if target.pbOwnSide.effects[PBEffects::ToxicSpikes]>0
        score += 40 if target.pbOwnSide.effects[PBEffects::StealthRock]
      end
    #---------------------------------------------------------------------------
    when "0ED"
      if !@battle.pbCanChooseNonActive?(user.index)
        score -= 80
      else
        score -= 40 if user.effects[PBEffects::Confusion]>0
        total = 0
        PBStats.eachBattleStat { |s| total += user.stages[s] }
        if total<=0 || user.turnCount==0
          score -= 90
        else
          score += total*10
          score += user.stages[PBStats::SPEED]*20
          score += 60 if user.hp <= user.totalhp/2
          # special case: user has no damaging moves
          hasDamagingMove = false
          user.eachMove do |m|
            next if !m.damagingMove?
            hasDamagingMove = true
            break
          end
          score += 75 if !hasDamagingMove
        end
      end
    #---------------------------------------------------------------------------
    when "0EE"
      if skill >= PBTrainerAI.mediumSkill
        if !@battle.pbCanChooseNonActive?(user.index) ||
            @battle.pbTeamAbleNonActiveCount(user.index) > 1
          score -= 100
        end
        user.eachOpposing do |o|
          o.moves.each do |m|
            next if m.type == nil
            if PBTypes.superEffective?(m.type,user.type1) &&
                user.type1 != user.type2 && PBTypes.superEffective?(m.type,user.type2)
              score += 90
              break
            elsif PBTypes.superEffective?(m.type,user.type1) ||
                user.type1 != user.type2 && PBTypes.superEffective?(m.type,user.type2)
              score += 60
              break
            end
          end
        end
      end
    #---------------------------------------------------------------------------
    when "0EF"
      score -= 90 if target.effects[PBEffects::MeanLook]>=0
    #---------------------------------------------------------------------------
    when "0F0"
      if skill>=PBTrainerAI.highSkill
        score += 20 if target.item!=0
      end
    #---------------------------------------------------------------------------
    when "0F1"
      if skill>=PBTrainerAI.highSkill
        if user.item==0 && target.item!=0
          score += 40
        else
          score -= 90
        end
      else
        score -= 80
      end
    #---------------------------------------------------------------------------
    when "0F2"
      if user.item==0 && target.item==0
        score -= 90
      elsif skill>=PBTrainerAI.highSkill && target.hasActiveAbility?(:STICKYHOLD)
        score -= 90
      elsif user.hasActiveItem?([:FLAMEORB,:TOXICORB,:STICKYBARB,:IRONBALL,
                                 :CHOICEBAND,:CHOICESCARF,:CHOICESPECS])
        score += 50
      elsif user.item==0 && target.item!=0
        score -= 30 if pbGetMoveData(user.lastMoveUsed,MOVE_FUNCTION_CODE)=="0F2"   # Trick/Switcheroo
      end
    #---------------------------------------------------------------------------
    when "0F3"
      if user.item==0 || target.item!=0
        score -= 90
      else
        if user.hasActiveItem?([:FLAMEORB,:TOXICORB,:STICKYBARB,:IRONBALL,
                                :CHOICEBAND,:CHOICESCARF,:CHOICESPECS])
          score += 50
        else
          score -= 80
        end
      end
    #---------------------------------------------------------------------------
    when "0F4", "0F5"
      if target.effects[PBEffects::Substitute]==0
        if skill>=PBTrainerAI.highSkill && pbIsBerry?(target.item)
          score += 30
        end
      end
    #---------------------------------------------------------------------------
    when "0F6"
      if user.recycleItem==0 || user.item!=0
        score -= 80
      elsif user.recycleItem!=0
        score += 30
      end
    #---------------------------------------------------------------------------
    when "0F7"
      if user.item==0 || !user.itemActive? ||
         user.unlosableItem?(user.item) || pbIsPokeBall?(user.item)
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "0F8"
      score -= 90 if target.effects[PBEffects::Embargo]>0
    #---------------------------------------------------------------------------
    when "0F9"
      if @battle.field.effects[PBEffects::MagicRoom]>0
        score -= 90
      else
        score += 30 if user.item==0 && target.item!=0
      end
    #---------------------------------------------------------------------------
    when "0FA"
      score -= 25
    #---------------------------------------------------------------------------
    when "0FB"
      score -= 30
    #---------------------------------------------------------------------------
    when "0FC"
      score -= 40
    #---------------------------------------------------------------------------
    when "0FD"
      score -= 30
      if target.pbCanParalyze?(user,false)
        score += 30
        if skill>=PBTrainerAI.mediumSkill
           aspeed = pbRoughStat(user,PBStats::SPEED,skill)
           ospeed = pbRoughStat(target,PBStats::SPEED,skill)
          if aspeed<ospeed
            score += 30
          elsif aspeed>ospeed
            score -= 40
          end
        end
        if skill>=PBTrainerAI.highSkill
          score -= 40 if target.hasActiveAbility?([:GUTS,:MARVELSCALE,:QUICKFEET])
        end
      end
    #---------------------------------------------------------------------------
    when "0FE"
      score -= 30
      if target.pbCanBurn?(user,false)
        score += 30
        if skill>=PBTrainerAI.highSkill
          score -= 40 if target.hasActiveAbility?([:GUTS,:MARVELSCALE,:QUICKFEET,:FLAREBOOST])
        end
      end
    #---------------------------------------------------------------------------
    when "0FF"
      if @battle.pbCheckGlobalAbility(:AIRLOCK) ||
         @battle.pbCheckGlobalAbility(:CLOUDNINE)
        score -= 90
      elsif @battle.pbWeather==PBWeather::Sun
        score -= 90
      elsif @battle.pbWeather==PBWeather::Rain ||
          @battle.pbWeather==PBWeather::Sandstorm ||
          @battle.pbWeather==PBWeather::Hail ||
          @battle.pbWeather==PBWeather::Snow
        score += 30
      else
        score += 60 if user.turnCount==0
        user.eachMove do |m|
          next if !m.damagingMove? || !isConst?(m.type,PBTypes,:FIRE)
          score += 30
        end
      end
    #---------------------------------------------------------------------------
    when "100"
      if @battle.pbCheckGlobalAbility(:AIRLOCK) ||
         @battle.pbCheckGlobalAbility(:CLOUDNINE)
        score -= 90
      elsif @battle.pbWeather==PBWeather::Rain
        score -= 90
      elsif @battle.pbWeather==PBWeather::Sun ||
          @battle.pbWeather==PBWeather::Sandstorm ||
          @battle.pbWeather==PBWeather::Hail ||
          @battle.pbWeather==PBWeather::Snow
        score += 30
      else
        score += 60 if user.turnCount==0
        user.eachMove do |m|
          next if !m.damagingMove? || !isConst?(m.type,PBTypes,:WATER)
          score += 30
        end
      end
    #---------------------------------------------------------------------------
    when "101"
      if @battle.pbCheckGlobalAbility(:AIRLOCK) ||
         @battle.pbCheckGlobalAbility(:CLOUDNINE)
        score -= 90
      elsif @battle.pbWeather==PBWeather::Sandstorm
        score -= 90
      elsif @battle.pbWeather==PBWeather::Rain ||
          @battle.pbWeather==PBWeather::Sun ||
          @battle.pbWeather==PBWeather::Hail ||
          @battle.pbWeather==PBWeather::Snow
        score += 30
      end
      if skill>=PBTrainerAI.mediumSkill
        user.eachOpposing do |b|
          score -= 40 if b.hasActiveAbility?(:ICEFACE) && b.form==0
        end
        user.eachAlly do |b|
          score += 40 if b.hasActiveAbility?(:ICEFACE) && b.form==0
        end
      end
    #---------------------------------------------------------------------------
    when "102"
      if @battle.pbCheckGlobalAbility(:AIRLOCK) ||
         @battle.pbCheckGlobalAbility(:CLOUDNINE)
        score -= 90
      elsif @battle.pbWeather==PBWeather::Hail
        score -= 90
      elsif @battle.pbWeather==PBWeather::Rain ||
          @battle.pbWeather==PBWeather::Sun ||
          @battle.pbWeather==PBWeather::Sandstorm
        score += 30
      end
    #---------------------------------------------------------------------------
    when "103"
      canChoose = false
      user.eachOpposing do |b|
        next if !@battle.pbCanChooseNonActive?(b.index)
        canChoose = true
        break
      end
      if !canChoose
        # Opponent can't switch in any Pokemon
        score = 0
      else
        score += 10*@battle.pbAbleNonActiveCount(user.idxOpposingSide)
        score -= user.pbOpposingSide.effects[PBEffects::Spikes]*30
      end
    #---------------------------------------------------------------------------
    when "104"
      if user.pbOpposingSide.effects[PBEffects::ToxicSpikes]>=2
        score -= 90
      else
        canChoose = false
        user.eachOpposing do |b|
          next if !@battle.pbCanChooseNonActive?(b.index)
          canChoose = true
          break
        end
        if !canChoose
          # Opponent can't switch in any Pokemon
          score -= 90
        else
          score += 8*@battle.pbAbleNonActiveCount(user.idxOpposingSide)
          score += [26,13][user.pbOpposingSide.effects[PBEffects::ToxicSpikes]]
        end
      end
    #---------------------------------------------------------------------------
    when "105"
      if user.pbOpposingSide.effects[PBEffects::StealthRock]
        score -= 90
      else
        canChoose = false
        user.eachOpposing do |b|
          next if !@battle.pbCanChooseNonActive?(b.index)
          canChoose = true
          break
        end
        if !canChoose
          # Opponent can't switch in any Pokemon
          score -= 90
        else
          score += 10 if user.turnCount==0
          score += 10*@battle.pbAbleNonActiveCount(user.idxOpposingSide)
        end
      end
    #---------------------------------------------------------------------------
    when "106"
    #---------------------------------------------------------------------------
    when "107"
    #---------------------------------------------------------------------------
    when "108"
    #---------------------------------------------------------------------------
    when "109"
    #---------------------------------------------------------------------------
    when "10A"
      score += 20 if user.pbOpposingSide.effects[PBEffects::AuroraVeil]>0
      score += 20 if user.pbOpposingSide.effects[PBEffects::Reflect]>0
      score += 20 if user.pbOpposingSide.effects[PBEffects::LightScreen]>0
    #---------------------------------------------------------------------------
    when "10B"
      score += 10*(user.stages[PBStats::ACCURACY]-target.stages[PBStats::EVASION])
    #---------------------------------------------------------------------------
    when "10C"
      if user.effects[PBEffects::Substitute]>0
        score -= 90
      elsif user.hp<=user.totalhp/4
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "10D"
      if user.pbHasType?(:GHOST)
        if target.effects[PBEffects::Curse]
          score -= 90
        elsif user.hp<=user.totalhp/2
          if @battle.pbAbleNonActiveCount(user.idxOwnSide)==0
            score -= 90
          else
            score -= 50
            score -= 30 if @battle.switchStyle
          end
        end
      else
        avg  = user.stages[PBStats::SPEED]*10
        avg -= user.stages[PBStats::ATTACK]*10
        avg -= user.stages[PBStats::DEFENSE]*10
        score += avg/3
      end
    #---------------------------------------------------------------------------
    when "10E"
      score -= 40
    #---------------------------------------------------------------------------
    when "10F"
      if target.effects[PBEffects::Nightmare] ||
         target.effects[PBEffects::Substitute]>0
        score -= 90
      elsif !target.asleep?
        score -= 90
      else
        score -= 90 if target.statusCount<=1
        score += 50 if target.statusCount>3
      end
    #---------------------------------------------------------------------------
    when "110"
      score += 30 if user.effects[PBEffects::Trapping]>0
      score += 30 if user.effects[PBEffects::LeechSeed]>=0
      if @battle.pbAbleNonActiveCount(user.idxOwnSide)>0
        score += 80 if user.pbOwnSide.effects[PBEffects::Spikes]>0
        score += 80 if user.pbOwnSide.effects[PBEffects::ToxicSpikes]>0
        score += 80 if user.pbOwnSide.effects[PBEffects::StealthRock]
      end
    #---------------------------------------------------------------------------
    when "111"
      if @battle.positions[target.index].effects[PBEffects::FutureSightCounter]>0
        score -= 100
      elsif @battle.pbAbleNonActiveCount(user.idxOwnSide)==0
        # Future Sight tends to be wasteful if down to last Pokemon
        score -= 70
      end
    #---------------------------------------------------------------------------
    when "112"
      avg = 0
      avg -= user.stages[PBStats::DEFENSE]*10
      avg -= user.stages[PBStats::SPDEF]*10
      score += avg/2
      if user.effects[PBEffects::Stockpile]>=3
        score -= 80
      else
        # More preferable if user also has Spit Up/Swallow
        score += 20 if user.pbHasMoveFunction?("113","114")   # Spit Up, Swallow
      end
    #---------------------------------------------------------------------------
    when "113"
      score -= 100 if user.effects[PBEffects::Stockpile]==0
    #---------------------------------------------------------------------------
    when "114"
      if user.effects[PBEffects::Stockpile]==0
        score -= 90
      elsif user.hp==user.totalhp
        score -= 90
      else
        mult = [0,25,50,100][user.effects[PBEffects::Stockpile]]
        score += mult
        score -= user.hp*mult*2/user.totalhp
      end
    #---------------------------------------------------------------------------
    when "115"
      score += 50 if target.effects[PBEffects::HyperBeam]>0
      score -= 35 if target.hp<=target.totalhp/2   # If target is weak, no
      score -= 70 if target.hp<=target.totalhp/4   # need to risk this move
    #---------------------------------------------------------------------------
    when "116"
    #---------------------------------------------------------------------------
    when "117"
      hasAlly = false
      user.eachAlly do |b|
        hasAlly = true
        break
      end
      score -= 90 if !hasAlly
    #---------------------------------------------------------------------------
    when "118"
      if @battle.field.effects[PBEffects::Gravity]>0
        score -= 90
      elsif skill>=PBTrainerAI.mediumSkill
        score -= 30
        score -= 20 if user.effects[PBEffects::SkyDrop]>=0
        score -= 20 if user.effects[PBEffects::MagnetRise]>0
        score -= 20 if user.effects[PBEffects::Telekinesis]>0
        score -= 20 if user.pbHasType?(:FLYING)
        score -= 20 if user.hasActiveAbility?(:LEVITATE)
        score -= 20 if user.hasActiveItem?(:AIRBALLOON)
        score += 20 if target.effects[PBEffects::SkyDrop]>=0
        score += 20 if target.effects[PBEffects::MagnetRise]>0
        score += 20 if target.effects[PBEffects::Telekinesis]>0
        score += 20 if target.inTwoTurnAttack?("0C9","0CC","0CE")   # Fly, Bounce, Sky Drop
        score += 20 if target.pbHasType?(:FLYING)
        score += 20 if target.hasActiveAbility?(:LEVITATE)
        score += 20 if target.hasActiveItem?(:AIRBALLOON)
      end
    #---------------------------------------------------------------------------
    when "119"
      if user.effects[PBEffects::MagnetRise]>0 ||
         user.effects[PBEffects::Ingrain] ||
         user.effects[PBEffects::SmackDown]
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "11A"
      if target.effects[PBEffects::Telekinesis]>0 ||
         target.effects[PBEffects::Ingrain] ||
         target.effects[PBEffects::SmackDown]
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "11B"
    #---------------------------------------------------------------------------
    when "11C"
      if skill>=PBTrainerAI.mediumSkill
        score += 20 if target.effects[PBEffects::MagnetRise]>0
        score += 20 if target.effects[PBEffects::Telekinesis]>0
        score += 20 if target.inTwoTurnAttack?("0C9","0CC")   # Fly, Bounce
        score += 20 if target.pbHasType?(:FLYING)
        score += 20 if target.hasActiveAbility?(:LEVITATE)
        score += 20 if target.hasActiveItem?(:AIRBALLOON)
      end
    #---------------------------------------------------------------------------
    when "11D"
    #---------------------------------------------------------------------------
    when "11E"
    #---------------------------------------------------------------------------
    when "11F"
      aspeed = pbRoughStat(user,PBStats::SPEED,skill)
      ospeed = pbRoughStat(target,PBStats::SPEED,skill)
      tricked = @battle.field.effects[PBEffects::TrickRoom]>0
      num = user.turnCount == 1 ? 90 : 50
      if aspeed<ospeed
        score += tricked ? -90 : num
      elsif aspeed>ospeed
        score += tricked ? num : -90
      end
    #---------------------------------------------------------------------------
    when "120"
    #---------------------------------------------------------------------------
    when "121"
    #---------------------------------------------------------------------------
    when "122"
    #---------------------------------------------------------------------------
    when "123"
      if !target.pbHasType?(user.type1) &&
         !target.pbHasType?(user.type2)
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "124"
    #---------------------------------------------------------------------------
    when "125"
      score -= 90 if user.turnCount==0
    #---------------------------------------------------------------------------
    when "126"
      score += 20   # Shadow moves are more preferable
    #---------------------------------------------------------------------------
    when "127"
      score += 20   # Shadow moves are more preferable
      if target.pbCanParalyze?(user,false)
        score += 30
        if skill>=PBTrainerAI.mediumSkill
           aspeed = pbRoughStat(user,PBStats::SPEED,skill)
           ospeed = pbRoughStat(target,PBStats::SPEED,skill)
          if aspeed<ospeed
            score += 30
          elsif aspeed>ospeed
            score -= 40
          end
        end
        if skill>=PBTrainerAI.highSkill
          score -= 40 if target.hasActiveAbility?([:GUTS,:MARVELSCALE,:QUICKFEET])
        end
      end
    #---------------------------------------------------------------------------
    when "128"
      score += 20   # Shadow moves are more preferable
      if target.pbCanBurn?(user,false)
        score += 30
        if skill>=PBTrainerAI.highSkill
          score -= 40 if target.hasActiveAbility?([:GUTS,:MARVELSCALE,:QUICKFEET,:FLAREBOOST])
        end
      end
    #---------------------------------------------------------------------------
    when "129"
      score += 20   # Shadow moves are more preferable
      if target.pbCanFreeze?(user,false)
        score += 30
        if skill>=PBTrainerAI.highSkill
          score -= 20 if target.hasActiveAbility?(:MARVELSCALE)
        end
      end
    #---------------------------------------------------------------------------
    when "12A"
      score += 20   # Shadow moves are more preferable
      if target.pbCanConfuse?(user,false)
        score += 30
      else
        if skill>=PBTrainerAI.mediumSkill
          score -= 90
        end
      end
    #---------------------------------------------------------------------------
    when "12B"
      score += 20   # Shadow moves are more preferable
      if !target.pbCanLowerStatStage?(PBStats::DEFENSE,user)
        score -= 90
      else
        score += 40 if user.turnCount==0
        score += target.stages[PBStats::DEFENSE]*20
      end
    #---------------------------------------------------------------------------
    when "12C"
      score += 20   # Shadow moves are more preferable
      if !target.pbCanLowerStatStage?(PBStats::EVASION,user)
        score -= 90
      else
        score += target.stages[PBStats::EVASION]*15
      end
    #---------------------------------------------------------------------------
    when "12D"
      score += 20   # Shadow moves are more preferable
    #---------------------------------------------------------------------------
    when "12E"
      score += 20   # Shadow moves are more preferable
      score += 20 if target.hp>=target.totalhp/2
      score -= 20 if user.hp<user.hp/2
    #---------------------------------------------------------------------------
    when "12F"
      score += 20   # Shadow moves are more preferable
      score -= 110 if target.effects[PBEffects::MeanLook]>=0
    #---------------------------------------------------------------------------
    when "130"
      score += 20   # Shadow moves are more preferable
      score -= 40
    #---------------------------------------------------------------------------
    when "131"
      score += 20   # Shadow moves are more preferable
      if @battle.pbCheckGlobalAbility(:AIRLOCK) ||
         @battle.pbCheckGlobalAbility(:CLOUDNINE)
        score -= 90
      elsif @battle.pbWeather==PBWeather::ShadowSky
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "132"
      score += 20   # Shadow moves are more preferable
      if target.pbOwnSide.effects[PBEffects::AuroraVeil]>0 ||
         target.pbOwnSide.effects[PBEffects::Reflect]>0 ||
         target.pbOwnSide.effects[PBEffects::LightScreen]>0 ||
         target.pbOwnSide.effects[PBEffects::Safeguard]>0
        score += 30
        score -= 90 if user.pbOwnSide.effects[PBEffects::AuroraVeil]>0 ||
                       user.pbOwnSide.effects[PBEffects::Reflect]>0 ||
                       user.pbOwnSide.effects[PBEffects::LightScreen]>0 ||
                       user.pbOwnSide.effects[PBEffects::Safeguard]>0
      else
        score -= 110
      end
    #---------------------------------------------------------------------------
    when "133", "134"
      score -= 95
      score = 0 if skill>=PBTrainerAI.highSkill
    #---------------------------------------------------------------------------
    when "135"
      if target.pbCanFreeze?(user,false)
        score += 30
        if skill>=PBTrainerAI.highSkill
          score -= 20 if target.hasActiveAbility?(:MARVELSCALE)
        end
      end
    #---------------------------------------------------------------------------
    when "136"
      score += 20 if user.stages[PBStats::DEFENSE]<0
    #---------------------------------------------------------------------------
    when "137"
      hasEffect = user.statStageAtMax?(PBStats::DEFENSE) &&
                  user.statStageAtMax?(PBStats::SPDEF)
      user.eachAlly do |b|
        next if b.statStageAtMax?(PBStats::DEFENSE) && b.statStageAtMax?(PBStats::SPDEF)
        hasEffect = true
        score -= b.stages[PBStats::DEFENSE]*10
        score -= b.stages[PBStats::SPDEF]*10
      end
      if hasEffect
        score -= user.stages[PBStats::DEFENSE]*10
        score -= user.stages[PBStats::SPDEF]*10
      else
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "138"
      if target.statStageAtMax?(PBStats::SPDEF)
        score -= 90
      else
        score -= target.stages[PBStats::SPDEF]*10
      end
    #---------------------------------------------------------------------------
    when "139"
      if !target.pbCanLowerStatStage?(PBStats::ATTACK,user)
        score -= 90
      else
        score += target.stages[PBStats::ATTACK]*20
        if skill>=PBTrainerAI.mediumSkill
          hasPhysicalAttack = false
          target.eachMove do |m|
            next if !m.physicalMove?(m.type)
            hasPhysicalAttack = true
            break
          end
          if hasPhysicalAttack
            score += 20
          elsif skill>=PBTrainerAI.highSkill
            score -= 90
          end
        end
      end
    #---------------------------------------------------------------------------
    when "13A"
      avg  = target.stages[PBStats::ATTACK]*10
      avg += target.stages[PBStats::SPATK]*10
      score += avg/2
    #---------------------------------------------------------------------------
    when "13B"
      if !user.isSpecies?(:HOOPA) || user.form!=1
        score -= 100
      else
        score += 20 if target.stages[PBStats::DEFENSE]>0
      end
    #---------------------------------------------------------------------------
    when "13C"
      score += 20 if target.stages[PBStats::SPATK]>0
    #---------------------------------------------------------------------------
    when "13D"
      if !target.pbCanLowerStatStage?(PBStats::SPATK,user)
        score -= 90
      else
        score += 40 if user.turnCount==0
        score += target.stages[PBStats::SPATK]*20
      end
    #---------------------------------------------------------------------------
    when "13E"
      count = 0
      @battle.eachBattler do |b|
        if b.pbHasType?(:GRASS) && !b.airborne? &&
           (!b.statStageAtMax?(PBStats::ATTACK) || !b.statStageAtMax?(PBStats::SPATK))
          count += 1
          if user.opposes?(b)
            score -= 20
          else
            score -= user.stages[PBStats::ATTACK]*10
            score -= user.stages[PBStats::SPATK]*10
          end
        end
      end
      score -= 95 if count==0
    #---------------------------------------------------------------------------
    when "13F"
      count = 0
      @battle.eachBattler do |b|
        if b.pbHasType?(:GRASS) && !b.statStageAtMax?(PBStats::DEFENSE)
          count += 1
          if user.opposes?(b)
            score -= 20
          else
            score -= user.stages[PBStats::DEFENSE]*10
          end
        end
      end
      score -= 95 if count==0
    #---------------------------------------------------------------------------
    when "140"
      count=0
      @battle.eachBattler do |b|
        if b.poisoned? &&
           (!b.statStageAtMin?(PBStats::ATTACK) ||
           !b.statStageAtMin?(PBStats::SPATK) ||
           !b.statStageAtMin?(PBStats::SPEED))
          count += 1
          if user.opposes?(b)
            score += user.stages[PBStats::ATTACK]*10
            score += user.stages[PBStats::SPATK]*10
            score += user.stages[PBStats::SPEED]*10
          else
            score -= 20
          end
        end
      end
      score -= 95 if count==0
    #---------------------------------------------------------------------------
    when "141"
      if target.effects[PBEffects::Substitute]>0
        score -= 90
      else
        numpos = 0; numneg = 0
        PBStats.eachBattleStat do |s|
          numpos += target.stages[s] if target.stages[s]>0
          numneg += target.stages[s] if target.stages[s]<0
        end
        if numpos!=0 || numneg!=0
          score += (numpos-numneg)*10
        else
          score -= 95
        end
      end
    #---------------------------------------------------------------------------
    when "142"
      score -= 90 if target.pbHasType?(:GHOST)
    #---------------------------------------------------------------------------
    when "143"
      score -= 90 if target.pbHasType?(:GRASS)
    #---------------------------------------------------------------------------
    when "144"
    #---------------------------------------------------------------------------
    when "145"
      aspeed = pbRoughStat(user,PBStats::SPEED,skill)
      ospeed = pbRoughStat(target,PBStats::SPEED,skill)
      score -= 90 if aspeed>ospeed
    #---------------------------------------------------------------------------
    when "146"
    #---------------------------------------------------------------------------
    when "147"
    #---------------------------------------------------------------------------
    when "148"
      aspeed = pbRoughStat(user,PBStats::SPEED,skill)
      ospeed = pbRoughStat(target,PBStats::SPEED,skill)
      if aspeed>ospeed
        score -= 90
      else
        score += 30 if target.pbHasMoveType?(:FIRE)
      end
    #---------------------------------------------------------------------------
    when "149"
      if user.turnCount==0
        score += 30
      else
        score -= 90   # Because it will fail here
        score = 0 if skill>=PBTrainerAI.bestSkill
      end
    #---------------------------------------------------------------------------
    when "14A"
    #---------------------------------------------------------------------------
    when "14B", "14C"
      if user.effects[PBEffects::ProtectRate]>1 ||
         target.effects[PBEffects::HyperBeam]>0
        score -= 90
      else
        if skill>=PBTrainerAI.mediumSkill
          score -= user.effects[PBEffects::ProtectRate]*40
        end
        score += 150 if user.turnCount==0
        score += 130 if target.effects[PBEffects::TwoTurnAttack]>0
      end
    #---------------------------------------------------------------------------
    when "14D"
    #---------------------------------------------------------------------------
    when "14E"
      if user.statStageAtMax?(PBStats::SPATK) &&
         user.statStageAtMax?(PBStats::SPDEF) &&
         user.statStageAtMax?(PBStats::SPEED)
        score -= 90
      else
        score -= user.stages[PBStats::SPATK]*10   # Only *10 isntead of *20
        score -= user.stages[PBStats::SPDEF]*10   # because two-turn attack
        score -= user.stages[PBStats::SPEED]*10
        if skill>=PBTrainerAI.mediumSkill
          hasSpecialAttack = false
          user.eachMove do |m|
            next if !m.specialMove?(m.type)
            hasSpecialAttack = true
            break
          end
          if hasSpecialAttack
            score += 20
          elsif skill>=PBTrainerAI.highSkill
            score -= 90
          end
        end
        if skill>=PBTrainerAI.highSkill
          aspeed = pbRoughStat(user,PBStats::SPEED,skill)
          ospeed = pbRoughStat(target,PBStats::SPEED,skill)
          score += 30 if aspeed<ospeed && aspeed*2>ospeed
        end
      end
    #---------------------------------------------------------------------------
    when "14F"
      if skill>=PBTrainerAI.highSkill && target.hasActiveAbility?(:LIQUIDOOZE)
        score -= 80
      else
        score += 40 if user.hp<=user.totalhp/2
      end
    #---------------------------------------------------------------------------
    when "150"
      score += 20 if !user.statStageAtMax?(PBStats::ATTACK) && target.hp<=target.totalhp/4
    #---------------------------------------------------------------------------
    when "151"
      avg  = target.stages[PBStats::ATTACK] * 10
      avg += target.stages[PBStats::SPATK] * 10
      score += avg/2
      score -= 60 if !@battle.pbCanChooseNonActive?(user.index)
      if skill >= PBTrainerAI.mediumSkill
        user.eachOpposing do |o|
          o.moves.each do |m|
            next if m.type == nil
            if PBTypes.superEffective?(m.type,user.type1) &&
                user.type1 != user.type2 && PBTypes.superEffective?(m.type,user.type2)
              score += 100
              break
            elsif PBTypes.superEffective?(m.type,user.type1) ||
                user.type1 != user.type2 && PBTypes.superEffective?(m.type,user.type2)
              score += 60
              break
            end
          end
        end
      end
    #---------------------------------------------------------------------------
    when "152"
    #---------------------------------------------------------------------------
    when "153"
      score -= 95 if user.pbOpposingSide.effects[PBEffects::StickyWeb]
    #---------------------------------------------------------------------------
    when "154"
    #---------------------------------------------------------------------------
    when "155"
    #---------------------------------------------------------------------------
    when "156"
    #---------------------------------------------------------------------------
    when "157"
      score -= 90
    #---------------------------------------------------------------------------
    when "158"
      score -= 90 if !user.belched?
    #---------------------------------------------------------------------------
    when "159"
      if !target.pbCanPoison?(user,false) && !target.pbCanLowerStatStage?(PBStats::SPEED,user)
        score -= 90
      else
        if target.pbCanPoison?(user,false)
          score += 30
          if skill>=PBTrainerAI.mediumSkill
            score += 30 if target.hp<=target.totalhp/4
            score += 50 if target.hp<=target.totalhp/8
            score -= 40 if target.effects[PBEffects::Yawn]>0
          end
          if skill>=PBTrainerAI.highSkill
            score += 10 if pbRoughStat(target,PBStats::DEFENSE,skill)>100
            score += 10 if pbRoughStat(target,PBStats::SPDEF,skill)>100
            score -= 40 if target.hasActiveAbility?([:GUTS,:MARVELSCALE,:TOXICBOOST])
          end
        end
        if target.pbCanLowerStatStage?(PBStats::SPEED,user)
          score += target.stages[PBStats::SPEED]*10
          if skill>=PBTrainerAI.highSkill
            aspeed = pbRoughStat(user,PBStats::SPEED,skill)
            ospeed = pbRoughStat(target,PBStats::SPEED,skill)
            score += 30 if aspeed<ospeed && aspeed*2>ospeed
          end
        end
      end
    #---------------------------------------------------------------------------
    when "15A"
      if target.opposes?(user)
        score -= 40 if target.status==PBStatuses::BURN
      else
        score += 40 if target.status==PBStatuses::BURN
      end
    #---------------------------------------------------------------------------
    when "15B"
      if target.status==PBStatuses::NONE
        score -= 90
      elsif user.hp==user.totalhp && target.opposes?(user)
        score -= 90
      else
        score += (user.totalhp-user.hp)*50/user.totalhp
        score -= 30 if target.opposes?(user)
      end
    #---------------------------------------------------------------------------
    when "15C"
      hasEffect = user.statStageAtMax?(PBStats::ATTACK) &&
                  user.statStageAtMax?(PBStats::SPATK)
      user.eachAlly do |b|
        next if b.statStageAtMax?(PBStats::ATTACK) && b.statStageAtMax?(PBStats::SPATK)
        hasEffect = true
        score -= b.stages[PBStats::ATTACK]*10
        score -= b.stages[PBStats::SPATK]*10
      end
      if hasEffect
        score -= user.stages[PBStats::ATTACK]*10
        score -= user.stages[PBStats::SPATK]*10
      else
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "15D"
      numStages = 0
      PBStats.eachBattleStat do |s|
        next if target.stages[s]<=0
        numStages += target.stages[s]
      end
      score += numStages*20
    #---------------------------------------------------------------------------
    when "15E"
      if user.effects[PBEffects::LaserFocus]>0
        score -= 90
      else
        score += 40
      end
    #---------------------------------------------------------------------------
    when "15F"
      score += user.stages[PBStats::DEFENSE]*10
    #---------------------------------------------------------------------------
    when "160"
      if target.statStageAtMin?(PBStats::ATTACK)
        score -= 90
      else
        if target.pbCanLowerStatStage?(PBStats::ATTACK,user)
          score += target.stages[PBStats::ATTACK]*20
          if skill>=PBTrainerAI.mediumSkill
            hasPhysicalAttack = false
            target.eachMove do |m|
              next if !m.physicalMove?(m.type)
              hasPhysicalAttack = true
              break
            end
            if hasPhysicalAttack
              score += 20
            elsif skill>=PBTrainerAI.highSkill
              score -= 90
            end
          end
        end
        score += (user.totalhp-user.hp)*50/user.totalhp
      end
    #---------------------------------------------------------------------------
    when "161"
      if skill>=PBTrainerAI.mediumSkill
        if user.speed>target.speed
          score += 50
        else
          score -= 70
        end
      end
    #---------------------------------------------------------------------------
    when "162"
      score -= 90 if !user.pbHasType?(:FIRE)
    #---------------------------------------------------------------------------
    when "163"
    #---------------------------------------------------------------------------
    when "164"
    #---------------------------------------------------------------------------
    when "165"
      if skill>=PBTrainerAI.mediumSkill
         userSpeed   = pbRoughStat(user,PBStats::SPEED,skill)
         targetSpeed = pbRoughStat(target,PBStats::SPEED,skill)
        if userSpeed<targetSpeed
          score += 30
        end
      else
        score += 30
      end
    #---------------------------------------------------------------------------
    when "166"
    #---------------------------------------------------------------------------
    when "167"
      if user.pbOwnSide.effects[PBEffects::AuroraVeil]>0 ||
          @battle.pbWeather!=PBWeather::Hail && @battle.pbWeather!=PBWeather::Snow
        score -= 90
      else
        score += 40
      end
    #---------------------------------------------------------------------------
    when "168"
      if user.effects[PBEffects::ProtectRate]>1 ||
         target.effects[PBEffects::HyperBeam]>0
        score -= 90
      else
        if skill>=PBTrainerAI.mediumSkill
          score -= user.effects[PBEffects::ProtectRate]*40
        end
        score += 50 if user.turnCount==0
        score += 30 if target.effects[PBEffects::TwoTurnAttack]>0
        score += 20   # Because of possible poisoning
      end
    #---------------------------------------------------------------------------
    when "169"
    #---------------------------------------------------------------------------
    when "16A"
      hasAlly = false
      target.eachAlly do |b|
        hasAlly = true
        break
      end
      score -= 90 if !hasAlly
    #---------------------------------------------------------------------------
    when "16B"
      if skill>=PBTrainerAI.mediumSkill
        if target.lastRegularMoveUsed<0 ||
           !target.pbHasMove?(target.lastRegularMoveUsed) ||
           target.usingMultiTurnAttack?
          score -= 90
        else
          # Without lots of code here to determine good/bad moves and relative
          # speeds, using this move is likely to just be a waste of a turn
          score -= 50
        end
      end
    #---------------------------------------------------------------------------
    when "16C"
      if target.effects[PBEffects::ThroatChop]==0 && skill>=PBTrainerAI.highSkill
        hasSoundMove = false
        user.eachMove do |m|
          next if !m.soundMove?
          hasSoundMove = true
          break
        end
        score += 40 if hasSoundMove
      end
    #---------------------------------------------------------------------------
    when "16D"
      if user.hp==user.totalhp || (skill>=PBTrainerAI.mediumSkill && !user.canHeal?)
        score -= 90
      else
        score += 50
        score -= user.hp*100/user.totalhp
        score += 30 if @battle.pbWeather==PBWeather::Sandstorm
      end
    #---------------------------------------------------------------------------
    when "16E"
      if user.hp==user.totalhp || (skill>=PBTrainerAI.mediumSkill && !user.canHeal?)
        score -= 90
      else
        score += 50
        score -= user.hp*100/user.totalhp
        if skill>=PBTrainerAI.mediumSkill
          score += 30 if @battle.field.terrain==PBBattleTerrains::Grassy
        end
      end
    #---------------------------------------------------------------------------
    when "16F"
      if !target.opposes?(user)
        if target.hp==target.totalhp || (skill>=PBTrainerAI.mediumSkill && !target.canHeal?)
          score -= 90
        else
          score += 50
          score -= target.hp*100/target.totalhp
        end
      end
    #---------------------------------------------------------------------------
    when "170"
      reserves = @battle.pbAbleNonActiveCount(user.idxOwnSide)
      foes     = @battle.pbAbleNonActiveCount(user.idxOpposingSide)
      if @battle.pbCheckGlobalAbility(:DAMP)
        score -= 100
      elsif skill>=PBTrainerAI.mediumSkill && reserves==0 && foes>0
        score -= 100   # don't want to lose
      elsif skill>=PBTrainerAI.highSkill && reserves==0 && foes==0
        score += 80   # want to draw
      else
        score -= (user.total.hp-user.hp)*75/user.totalhp
      end
    #---------------------------------------------------------------------------
    when "171"
      if skill>=PBTrainerAI.mediumSkill
        hasPhysicalAttack = false
        target.eachMove do |m|
          next if !m.physicalMove?(m.type)
          hasPhysicalAttack = true
          break
        end
        score -= 80 if !hasPhysicalAttack
      end
    #---------------------------------------------------------------------------
    when "172"
      score += 20   # Because of possible burning
    #---------------------------------------------------------------------------
    when "173"
    #---------------------------------------------------------------------------
    when "174"
      score -= 90 if user.turnCount>0 || user.lastRoundMoved>=0
    #---------------------------------------------------------------------------
    when "175"
      score += 30 if target.effects[PBEffects::Minimize]
    #--------------------------------------------------------------------------
    when "177"
      if skill>=PBTrainerAI.mediumSkill
        score += user.stages[PBStats::DEFENSE]*10
      end
    #---------------------------------------------------------------------------
    when "178"
      if skill>=PBTrainerAI.mediumSkill
        userSpeed   = pbRoughStat(user,PBStats::SPEED,skill)
        targetSpeed = pbRoughStat(target,PBStats::SPEED,skill)
        if userSpeed > targetSpeed
          score += 30 + skill
        end
      end
    #---------------------------------------------------------------------------
    when "179"
      if skill>=PBTrainerAI.mediumSkill
        score += 60 if user.turnCount==0
        score -= 30 if user.hp <= user.totalhp / 2
      end
    #---------------------------------------------------------------------------
    when "17B"
      if skill>=PBTrainerAI.mediumSkill
        if target.hasActivity?(:CONTRARY)
          score += 60
        else
          score -= 60
        end
      end
    #---------------------------------------------------------------------------
    when "17E"
      statuses = 0
      @battle.pbParty(user.index).each do |pkmn|
        if pkmn && 
          (pkmn.hp<=pkmn.totalhp/2 || pkmn.status!=PBStatuses::NONE)
          statuses += 1
        end
      end
      if statuses==0
        score -= 80
      else
        score += 20*statuses
      end
    #---------------------------------------------------------------------------
    when "17F"
      pkmnCount = 0
      @battle.pbParty(user.index).each do |pkmn|
        if !pkmn.fainted?
          pkmnCount += 1
        end
      end
      score += 30 if pkmnCount<=1
    #---------------------------------------------------------------------------
    when "181"
      if skill>=PBTrainerAI.mediumSkill
        score += 30
      end
    #---------------------------------------------------------------------------
    when "183"
      if pbIsBerry?(user.item)
        score += 30
        score += 30 if user.hp <= user.totalhp / 2
      end
    #---------------------------------------------------------------------------
    when "184"
      @battle.eachBattler do |b|
        if pbIsBerry?(b.item)
          score += 10
        end
      end
    #---------------------------------------------------------------------------
    when "186"
      if skill>=PBTrainerAI.mediumSkill
        userSpeed   = pbRoughStat(user,PBStats::SPEED,skill)
        targetSpeed = pbRoughStat(target,PBStats::SPEED,skill)
        if userSpeed < targetSpeed
          score += 30
        end
      end
    #---------------------------------------------------------------------------
    when "189"
      statuses = 0
      @battle.pbParty(user.index).each do |pkmn|
        if pkmn && 
          (pkmn.hp<=pkmn.totalhp/2 || pkmn.status!=PBStatuses::NONE)
          statuses += 1
        end
      end
      if statuses==0
        score -= 80
      else
        score += 20*statuses
      end
    #---------------------------------------------------------------------------
    when "18B"
      if skill>=PBTrainerAI.mediumSkill
        if target.pbCanBurn?(user,false) && target.effects[PBEffects::BurningJealousy]
          score += 60
        end
      end
    #---------------------------------------------------------------------------
    when "18C"
      if skill>=PBTrainerAI.mediumSkill
        if @battle.field.terrain==PBBattleTerrains::Grassy
          score += 60
        end
      end
    #---------------------------------------------------------------------------
    when "18D"
      if skill>=PBTrainerAI.mediumSkill
        if @battle.field.terrain==PBBattleTerrains::Electric
          score += 60
        end
      end
    #---------------------------------------------------------------------------
    when "18E"
      allyCount = 0
      @battle.pbParty(user.index).each do |pkmn|
        if pkmn
          allyCount += 1
        end
      end
      score += 20*allyCount
    #---------------------------------------------------------------------------
    when "190"
      if skill>=PBTrainerAI.mediumSkill
        if @battle.field.terrain==PBBattleTerrains::Electric
          score += 40
        end
        targetCount = 0
        @battle.pbParty(target.index).each do |pkmn|
          if pkmn
            targetCount += 1
          end
        end
        score += 10*targetCount
      end
    #---------------------------------------------------------------------------
    when "191"
      if move.statusMove?
        if user.statStageAtMax?(PBStats::SPATK)
          score -= 90
        else
          score -= user.stages[PBStats::SPATK]*20
        end
      else
        score += 20 if user.stages[PBStats::SPATK]<0
      end
    #---------------------------------------------------------------------------
    when "192"
      if target.item==0
        score -= 90
      elsif target.hasActiveItem?([:FLAMEORB,:TOXICORB,:POISONBARB,
                                   :LIGHTBALL,:KINGSROCK,:RAZORFANG])
        score += 50
      end
    #---------------------------------------------------------------------------
    when "193"
      if !user.statStageAtMin?(PBStats::DEFENSE) &&
         !user.statStageAtMax?(PBStats::SPEED)
        score += user.stages[PBStats::DEFENSE]*10
        score -= user.stages[PBStats::SPEED]*10
      else
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "195"
      if @battle.field.terrain==PBBattleTerrains::None
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "196"
      score += 20 if @battle.field.terrain==PBBattleTerrains::Misty
      if user.hp==user.totalhp
        score -= 60
      elsif user.hp<=user.totalhp/3
        score += 40
      end
    #---------------------------------------------------------------------------
    # 下面是自己添加的
    #---------------------------------------------------------------------------
     when "19A"
      if user.pbOwnSide.effects[PBEffects::Reflect] == 0
        score += 20
      end
      if user.pbOwnSide.effects[PBEffects::LightScreen] == 0
        score += 20
      end
    #---------------------------------------------------------------------------
    when "19B"
      if user.stages[PBStats::ATTACK]>0
        score += user.stages[PBStats::ATTACK] * 10
      end
  #---------------------------------------------------------------------------
     when "19C"
      cursed = target.effects[PBEffects::Curse] || target.effects[PBEffects::Tearalament]
      if !cursed    # 未被诅咒或珠泪哀歌
        score += 30
      end
    #---------------------------------------------------------------------------
    when "19D"
      score += 30 if @battle.pbWeather==PBWeather::Sun || @battle.pbWeather==PBWeather::HarshSun
    #---------------------------------------------------------------------------
    when "19E"
      score += 30 if @battle.field.terrain==PBBattleTerrains::Electric
      score += 30 if @battle.pbWeather==PBWeather::Rain || @battle.pbWeather==PBWeather::HeavyRain
    #---------------------------------------------------------------------------
    when "19F"
      if user.hp==user.totalhp
        score += 60
      elsif user.hp>=user.totalhp*3/4
        score += 30
      elsif user.hp>user.totalhp/2
        score += 10
      else
        score -= 60
      end
    #---------------------------------------------------------------------------
    when "1A0"
      score += 30 if user.stages[PBStats::ATTACK]<=0
      score += 30 if user.stages[PBStats::SPEED]<=0
    #---------------------------------------------------------------------------
3    when "1A2"
      score += 60 if user.effects[PBEffects::UsedRapidSpin]
    #---------------------------------------------------------------------------
    when "1A3"
      score += 60 if user.effects[PBEffects::UsedCoil] || user.hasActiveAbility?(:SKILLLINK)
    #---------------------------------------------------------------------------
    when "1A4"#蛇咬
      if target.status==PBStatuses::NONE
        score -= 90
      else
        if user.hp==user.totalhp
          score -= 30
        elsif user.hp>=user.totalhp*3/4
          score += 10
        elsif user.hp>user.totalhp/2
          score += 30
        else
          score += 60
        end
      end
    #---------------------------------------------------------------------------
    when "1A5" #回响之音
      ghostType = getConst(PBTypes,:GHOST)
      isGhost = target.pbHasType?(:GHOST)
      cursed = target.effects[PBEffects::Curse]
      if !isGhost && !cursed  # 没有幽灵属性¸未被诅咒
        score += 60
      elsif !isGhost	# 没有幽灵属性      score += 30
      elsif !cursed	# 未被诅咒
        score += 30
      end  
   #---------------------------------------------------------------------------
    when "1A6"#悠然龙吟
      if user.pbOwnSide.effects[PBEffects::Reflect] > 0
        score += 20
      end
      if user.pbOwnSide.effects[PBEffects::LightScreen] > 0
        score += 20
      end
    #---------------------------------------------------------------------------
    when "1A7"#千里击涛
    if move.statusMove?
    if !target.pbCanLowerStatStage?(PBStats::SPDEF, user) &&
       !target.pbCanLowerStatStage?(PBStats::DEFENSE, user)
      score -= 90
    else
      score += target.stages[PBStats::SPDEF] * 20
      score += target.stages[PBStats::DEFENSE] * 10
    end
  else
    score += 20 if target.stages[PBStats::SPDEF] > 0
    score += 20 if target.stages[PBStats::DEFENSE] > 0
  end
    #---------------------------------------------------------------------------
    when "1A8"#百花绽放
      if user.hp==user.totalhp
        score += 60
      elsif user.hp>=user.totalhp*3/4
        score += 30
      elsif user.hp>=user.totalhp/2
        score += 10
      else
        score -= 60
      end
    #---------------------------------------------------------------------------
    when "1A9"#精神利刃
      if skill>=PBTrainerAI.mediumSkill
        if @battle.field.terrain==PBBattleTerrains::Electric
          score += 60
        end
      end
    #---------------------------------------------------------------------------
    when "1AA"  #神威日冕
    #---------------------------------------------------------------------------
    when "1AB"  #永夜灵霄
    #---------------------------------------------------------------------------
    when "1AC"  #
    #---------------------------------------------------------------------------
    when "1AD"  #
    #---------------------------------------------------------------------------
   when "1C1"  # 归无之光 
    score += 40  # 基础加分  # 1. 可以击中妖精属性（龙系技能对妖精无效时这是巨大优势）
     if skill >= PBTrainerAI.mediumSkill && target.pbHasType?(:FAIRY)
       score += 60
      end
     if skill >= PBTrainerAI.mediumSkill
     if target.stages[PBStats::DEFENSE] > 0 || target.stages[PBStats::SPDEF] > 0
      score += (target.stages[PBStats::DEFENSE] + target.stages[PBStats::SPDEF]) * 10
    end
    if skill >= PBTrainerAI.highSkill
      if target.pbOwnSide.effects[PBEffects::AuroraVeil] > 0 ||
         target.pbOwnSide.effects[PBEffects::LightScreen] > 0
        score += 30
      end
    end
  end
  if skill >= PBTrainerAI.highSkill
    user.eachOpposing do |b|
      if b.pbHasType?(:FAIRY) && !b.fainted?
        score += 30
        break
      end
    end
  end
    #---------------------------------------------------------------------------
    when "1AD"  # 晶光转转
      score += 20 if user.effects[PBEffects::Trapping]>0
      score += 20 if user.effects[PBEffects::LeechSeed]>=0
      score += 20 if user.pbOwnSide.effects[PBEffects::StealthRock]
      score += 20 if user.pbOwnSide.effects[PBEffects::Spikes]>0
      score += 20 if user.pbOwnSide.effects[PBEffects::ToxicSpikes]>0
      score += 20 if user.pbOwnSide.effects[PBEffects::StickyWeb]
      score += 20 if target.pbCanPoison?(user,false)
    #---------------------------------------------------------------------------
    when "1EA"  # 愤怒之拳
      score += user.num_times_hit * 10
    #---------------------------------------------------------------------------
    when "1C0"  # 雪景
      if @battle.pbCheckGlobalAbility(:AIRLOCK) ||
         @battle.pbCheckGlobalAbility(:CLOUDNINE)
        score -= 90
      elsif @battle.pbWeather==PBWeather::Snow
        score -= 90
      elsif @battle.pbWeather==PBWeather::Rain ||
          @battle.pbWeather==PBWeather::Sun ||
          @battle.pbWeather==PBWeather::Sandstorm
        score += 30
      end
    #---------------------------------------------------------------------------
    when "1EE"  # 全开猛撞,闪电猛冲
      score += 20
    #---------------------------------------------------------------------------
    when "19D"  # 水蒸气
      if @battle.pbWeather==PBWeather::Sun || @battle.pbWeather==PBWeather::HarshSun
        score += 30
      end
    #---------------------------------------------------------------------------
    when "1A9"  # 精神剑
      if @battle.terrain==PBBattleTerrains::Electric
        score += 30
      end
    #---------------------------------------------------------------------------
    when "1C3"  # 糖浆炸弹
      aspeed = pbRoughStat(user,PBStats::SPEED,skill)
      ospeed = pbRoughStat(target,PBStats::SPEED,skill)
      if aspeed<ospeed
        score += 30
      elsif aspeed>ospeed
        score -= 20
      end
    #---------------------------------------------------------------------------
    when "1CA"  # 电光束
      if @battle.pbWeather==PBWeather::Rain
        score += 30
      end
    #---------------------------------------------------------------------------
    when "1CE"  # 线阱
      score += target.stages[PBStats::SPEED] * 10 if user.stages[PBStats::SPEED] > 0
    #---------------------------------------------------------------------------
    when "1D0"  # 鼠数儿
      score += 30 if user.hasActiveItem?(:LOADEDDICE) || user.hasActiveAbility?(:SKILLLINK)
    #---------------------------------------------------------------------------
    when "1D1"  # 大扫除
      failed = true
      2.times do |i|
        side = (i == 0) ? user.pbOwnSide : user.pbOpposingSide
        next unless side.effects[PBEffects::Spikes] > 0 ||
                    side.effects[PBEffects::ToxicSpikes] > 0 ||
                    side.effects[PBEffects::StealthRock] ||
                    side.effects[PBEffects::StickyWeb] ||
                    defined?(PBEffects::Steelsurge) && side.effects[PBEffects::Steelsurge]
        score += 30
        failed = false
        break
      end
      @battle.allBattlers.each do |b|
        next if b.effects[PBEffects::Substitute] == 0
          failed = false
        break
      end
      failed2 = true
      statUp = [PBStats::ATTACK, 1, PBStats::SPEED, 1]
      (statUp.length / 2).times do |i|
        next if !user.pbCanRaiseStatStage?(statUp[i * 2], user, self)
        score += 10
        failed2 = false
        break
      end
      if failed && failed2
        score -= 90
      end
    #---------------------------------------------------------------------------
    when "1D2"  # 盐腌
      if target.pbHasType?(:STEEL) || target.pbHasType?(:WATER)
        score += 30
      end
    #---------------------------------------------------------------------------
    when "1D6"  # 扫墓
      score += user.num_fainted_allies * 10
    #---------------------------------------------------------------------------
    when "1D7"  # 冰旋
      if @battle.field.terrain != PBBattleTerrains::None
        score += 20
      end
    #---------------------------------------------------------------------------
    when "1D8"  # 甩肉
      if user.statStageAtMax?(PBStats::ATTACK) && user.statStageAtMax?(PBStats::SPATK) && user.statStageAtMax?(PBStats::SPEED) ||
         user.hp<=user.totalhp/2
        score -= 100
      else
        score += (6-user.stages[PBStats::ATTACK])*2
        score += (6-user.stages[PBStats::SPATK])*2
        score += (6-user.stages[PBStats::SPEED])*3
      end
    #---------------------------------------------------------------------------
    when "1D9"  # 上菜
      if user.isCommanderHost?
        commandar_form = user.effects[PBEffects::Commander][1]
        commandar_stat = [PBStats::ATTACK, PBStats::DEFENSE, PBStats::SPEED][commandar_form]
        if user.pbCanRaiseStatStage?(commandar_stat, user, self)
          score += 30
        end
      end
    #---------------------------------------------------------------------------
    when "1DA"  # 淘金潮
      score += user.stages[PBStats::SPATK]*10
    #---------------------------------------------------------------------------
    when "1DC"  # 星晶爆发
      score += 10
      score += 30 if user.hasActiveAbility?(:CONTRARY)
    #---------------------------------------------------------------------------
    when "1EE"  # 全开猛撞,闪电猛冲
      score += 20
    #---------------------------------------------------------------------------
    when "1DE"  # 刷刷茶炮
      if target.pbCanBurn?(user,false)
        score += 30
        if skill>=PBTrainerAI.highSkill
          score -= 40 if target.hasActiveAbility?([:GUTS,:MARVELSCALE,:QUICKFEET,:FLAREBOOST])
        end
      end
    #---------------------------------------------------------------------------
    when "1DF"  # 硬压
      if target.hp==target.totalhp
        score += 60
      elsif target.hp>=target.totalhp*3/4
        score += 30
      elsif target.hp>=target.totalhp/2
        score += 10
      else
        score -= 60
      end
    #---------------------------------------------------------------------------
    when "1E0"  # 晶光星群
      if user.isSpecies?(:TERAPAGOS)
        score += 50
        score += 50 if user.hp < user.totalhp / 2
        score += (1+user.turnCount) * 50 if user.form == 1
      end
    #---------------------------------------------------------------------------
    when "200" # Dire Claw
      score += 30 if target.status == PBStatuses::NONE
    #---------------------------------------------------------------------------
    when "201" # Power Shift
      if skill >= PBTrainerAI.mediumSkill
        aatk = pbRoughStat(user, PBStats::ATTACK, skill)
        adef = pbRoughStat(user, PBStats::DEFENSE, skill)
        asatk = pbRoughStat(user, PBStats::SPATK, skill)
        asdef = pbRoughStat(user, PBStats::SPDEF, skill)
        if (aatk == adef && asatk == asdef) ||
          user.effects[PBEffects::PowerShift] # No flip-flopping
          score -= 90
        elsif adef > aatk || asdef > asatk # Prefer a higher Attack
          score += 30
        else
          score -= 30
        end
      else
        score -= 30
      end
    #---------------------------------------------------------------------------
    when "202" # Stone Axe
      score += 60 if target.effects[PBEffects::StoneAxe] < 0
      #---------------------------------------------------------------------------
    when "203" # Ceaseless Edge
      score += 60 if target.effects[PBEffects::CeaselessEdge] < 0
      #---------------------------------------------------------------------------
    when "204" # Springtide Storm
      if move.statusMove?
        if user.statStageAtMax?(PBStats::DEFENSE)
          score -= 90
        else
          score += 40 if user.turnCount == 0
          score -= user.stages[PBStats::DEFENSE] * 20
        end
      else
        score += 10 if user.turnCount == 0
        score += 20 if user.stages[PBStats::DEFENSE] < 0
      end
      #---------------------------------------------------------------------------
    when "205" # Mystical Power
      if skill >= PBTrainerAI.mediumSkill
        aatk = pbRoughStat(user, PBStats::ATTACK, skill)
        adef = pbRoughStat(user, PBStats::DEFENSE, skill)
        asatk = pbRoughStat(user, PBStats::SPATK, skill)
        asdef = pbRoughStat(user, PBStats::SPDEF, skill)
        if aatk + asatk >= adef + asdef
          if user.statStageAtMax?(PBStats::ATTACK) && user.statStageAtMax?(PBStats::SPATK)
            score -= 90
          else
            score += 40 if user.turnCount == 0
            score -= user.stages[PBStats::ATTACK] * 20
            score -= user.stages[PBStats::SPATK] * 20
          end
        else
          if user.statStageAtMax?(PBStats::DEFENSE) && user.statStageAtMax?(PBStats::SPDEF)
            score -= 90
          else
            score += 40 if user.turnCount == 0
            score -= user.stages[PBStats::DEFENSE] * 20
            score -= user.stages[PBStats::SPDEF] * 20
          end
        end
      else
        score -= 30
      end
      #---------------------------------------------------------------------------
    when "206" # Wave Crash
      score -= 20
      #---------------------------------------------------------------------------
    when "207" # Chloroblast
      score -= 20
      #---------------------------------------------------------------------------
    when "208" # Victory Dance
      if skill >= PBTrainerAI.mediumSkill
        if user.statStageAtMax?(PBStats::DEFENSE) && user.statStageAtMax?(PBStats::SPDEF) &&
          user.statStageAtMax?(PBStats::ATTACK) && user.statStageAtMax?(PBStats::SPATK)
          score -= 90
        else
          score += 40 if user.turnCount == 0 || !user.effects[PBEffects::VictoryDance]
          score -= user.stages[PBStats::DEFENSE] * 10
          score -= user.stages[PBStats::SPDEF] * 10
          score -= user.stages[PBStats::ATTACK] * 10
          score -= user.stages[PBStats::SPATK] * 10
        end
      else
        score -= 30
      end
      #---------------------------------------------------------------------------
    when "209" # Barb Barrage
      score += 30 if target.status != PBStatuses::NONE
      #---------------------------------------------------------------------------
    when "210" # Bitter Malice
      score += 30 if target.status != PBStatuses::NONE
      #---------------------------------------------------------------------------
    when "211" # Infernal Parade
      score += 30 if target.status != PBStatuses::NONE
      #---------------------------------------------------------------------------
    when "212" # Triple Arrows
      score += 30 if user.effects[PBEffects::FocusEnergy] < 2
      score += user.stages[PBStats::DEFENSE] * 10
      #---------------------------------------------------------------------------
    when "213" # Bleakwind Storm
      if target.pbCanFrostbite?(user, false)
        score += 30
        if skill >= PBTrainerAI.highSkill
          score -= 40 if target.hasActiveAbility?([:GUTS, :MARVELSCALE, :QUICKFEET])
        end
      else
        if skill >= PBTrainerAI.mediumSkill
          score -= 90 if move.statusMove?
        end
      end
      #---------------------------------------------------------------------------
    when "214" # Lunar Blessing
      if user.hp == user.totalhp || (skill >= PBTrainerAI.mediumSkill && !user.canHeal?)
        score -= 90
      else
        score += 50
        score -= user.hp * 100 / user.totalhp
      end
      case user.status
      when PBStatuses::POISON
        score += 40
        if skill >= PBTrainerAI.mediumSkill
          if user.hp < user.totalhp / 8
            score += 60
          elsif skill >= PBTrainerAI.highSkill &&
            user.hp < (user.effects[PBEffects::Toxic] + 1) * user.totalhp / 16
            score += 60
          end
        end
      when PBStatuses::BURN, PBStatuses::PARALYSIS, PBStatuses::FROSTBITE, PBStatuses::DROWSY
        score += 40
      else
        score -= 90
      end
      #---------------------------------------------------------------------------
    when "215" # Take Heart
      if skill >= PBTrainerAI.mediumSkill
        if user.statStageAtMax?(PBStats::DEFENSE) && user.statStageAtMax?(PBStats::SPDEF) &&
          user.statStageAtMax?(PBStats::ATTACK) && user.statStageAtMax?(PBStats::SPATK)
          score -= 90
        else
          score += 40 if user.turnCount == 0
          score -= user.stages[PBStats::DEFENSE] * 10
          score -= user.stages[PBStats::SPDEF] * 10
          score -= user.stages[PBStats::ATTACK] * 10
          score -= user.stages[PBStats::SPATK] * 10
        end
      else
        score -= 30
      end
      case user.status
      when PBStatuses::POISON
        score += 40
        if skill >= PBTrainerAI.mediumSkill
          if user.hp < user.totalhp / 8
            score += 60
          elsif skill >= PBTrainerAI.highSkill &&
            user.hp < (user.effects[PBEffects::Toxic] + 1) * user.totalhp / 16
            score += 60
          end
        end
      when PBStatuses::BURN, PBStatuses::PARALYSIS, PBStatuses::FROSTBITE, PBStatuses::DROWSY
        score += 40
      else
        score -= 90
      end
    end
    return score
  end
end


# 新增的
class PokeBattle_Battle
  def allSameSideBattlers(idxBattler = 0)
    idxBattler = idxBattler.index if idxBattler.respond_to?("index")
    return @battlers.select { |b| b && !b.fainted? && !b.opposes?(idxBattler) }
  end
  
  def pbTeamAbleNonActiveCount(idxBattler = 0)
    inBattleIndices = allSameSideBattlers(idxBattler).map { |b| b.pokemonIndex }
    count = 0
    eachInTeamFromBattlerIndex(idxBattler) do |pkmn, i|
      next if !pkmn || !pkmn.able?
      next if inBattleIndices.include?(i)
      count += 1
    end
    return count
  end
  
end