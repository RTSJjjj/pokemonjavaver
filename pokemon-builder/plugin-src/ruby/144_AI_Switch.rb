# Switching pkmn
class PokeBattle_AI
	#=============================================================================
	# Decide whether the opponent should switch Pokémon
	#=============================================================================
	def pbEnemyShouldWithdraw?(idxBattler)
		return pbEnemyShouldWithdrawEx?(idxBattler,false)
	end
	
	def shouldSwitchHandler(idxBattler,battler,opps)
    battler = @battle.battlers[idxBattler]
		skill = getSkill(idxBattler,@battle)
    #@battle.pbGetOwnerFromBattlerIndex(idxBattler).skill rescue 0
		moves = battler.moves
		hp = battler.hp
		thp = battler.totalhp
		opps = battler.eachOpposing if !opps || opps.empty?
		move_pri = false
		move_super = false
		faster = false
		opp_move_pri = false
    higherhp = false
    hyper = false
    all_bad_move = true
		moves.each do |m|
      next if !m || m.id==0
      m.calcType = m.pbCalcType(battler)
			if m.priority>0
				move_pri = true
			end
			opps.each do |o|
        if !PBTypes.resistant?(m.calcType,o.type1,o.type2)
          all_bad_move = false
          break
        end
				if pbRoughStat(battler,PBStats::SPEED,skill)>pbRoughStat(o,PBStats::SPEED,skill) && battler.status != PBStatuses::PARALYSIS
					faster = true
				end
				oppmoves = o.moves
				oppmoves.each do |om|
          next if !om || om.id==0
					if om.priority>0
						opp_move_pri = true
					end
				end
				if hp > o.hp
					higherhp = true
				else
					higherhp = false
				end
				if @battle.pbSideSize(battler.index+1)==1 && !(battler.pbDirectOpposing.fainted?) && skill>=PBTrainerAI.highSkill
					opp = battler.pbDirectOpposing
					if opp.effects[PBEffects::HyperBeam]>0 ||
						(opp.hasActiveAbility?(:TRUANT) && opp.effects[PBEffects::Truant])
						hyper = true
					end
				end
			end
		end
    if all_bad_move && pbAIRandom(100)<70
      return true
    end
        if move_pri && !opp_move_pri
            return false
        end
    perishSong = battler.effects[PBEffects::PerishSong]
    if perishSong>0 && pbAIRandom(100)<(80.0/perishSong)
      return true
    end
    if battler.effects[PBEffects::Curse] || battler.effects[PBEffects::Tearalament]
      if battler.hp < battler.totalhp / 8
        return true if pbAIRandom(100) < 80
      elsif battler.hp < battler.totalhp / 4
        return true if pbAIRandom(100) < 60
      elsif battler.hp < battler.totalhp / 2
        return true if pbAIRandom(100) < 40
      end
    end
    if hyper
      return false
    end
    if battler.effects[PBEffects::Encore]>0
      idxEncoredMove = battler.pbEncoredMoveIndex
      if idxEncoredMove>=0
        scoreSum   = 0
        scoreCount = 0
        battler.eachOpposing do |b|
          scoreSum += pbGetMoveScore(battler.moves[idxEncoredMove],battler,b,skill)
          scoreCount += 1
        end
        if scoreCount>0 && scoreSum/scoreCount<=20
          return false
        end
      end
    end
    if battler.status==PBStatuses::POISON && battler.statusCount>0
      toxicHP = battler.totalhp/16
      nextToxicHP = toxicHP*(battler.effects[PBEffects::Toxic]+1)
      if battler.hp<=nextToxicHP && battler.hp>toxicHP*2
        return true
      end
		end
		return false
	end
	
	def pbEnemyShouldWithdrawEx?(idxBattler,forceSwitch)
		return false if @battle.wildBattle?
    return false if @battle.pbSideSize(idxBattler) == 1 && pbAIRandom(100)<20
		shouldSwitch = forceSwitch if forceSwitch
		batonPass = -1
		moveType = -1
		skill = getSkill(idxBattler,@battle)
    #@battle.pbGetOwnerFromBattlerIndex(idxBattler).skill || 0
		battler = @battle.battlers[idxBattler]
    opps = []
    battler.eachOpposing do |o|
      next if !o || o.fainted?
      opps.push(o)
    end

		#I removed all this bc it's handled in the shouldSwitchHandler def
		shouldSwitch = shouldSwitchHandler(idxBattler,battler,opps) if !forceSwitch
		if shouldSwitch
			list = []
			@battle.pbParty(idxBattler).each_with_index do |pkmn,i|
				next if !@battle.pbCanSwitch?(idxBattler,i)
				# If perish count is 1, it may be worth it to switch
				# even with Spikes, since Perish Song's effect will end
				if battler.effects[PBEffects::PerishSong]!=1
					# Will contain effects that recommend against switching
					spikes = battler.pbOwnSide.effects[PBEffects::Spikes]
					# Don't switch to this if too little HP
					if spikes>0
						spikesDmg = [8,6,4][spikes-1]
						if pkmn.hp<=pkmn.totalhp/spikesDmg
							next if !pkmn.hasType?(:FLYING) && !pkmn.hasActiveAbility?(:LEVITATE)
						end
					end
				end
				# moveType is the type of the target's last used move
				if moveType>=0 && PBTypes.ineffective?(pbCalcTypeMod(moveType,battler,battler))
					weight = 60
					typeMod = pbCalcTypeModPokemon(pkmn,battler.pbDirectOpposing(true))
					if PBTypes.superEffective?(typeMod.to_f/PBTypeEffectivenesss::NORMAL_EFFECTIVE)
						# Greater weight if new Pokemon's type is effective against target
						weight = 90
					end
					list.unshift(i) if pbAIRandom(100)<weight   # Put this Pokemon first
				elsif moveType>=0 && PBTypes.resistant?(pbCalcTypeMod(moveType,battler,battler))
					weight = 40
					typeMod = pbCalcTypeModPokemon(pkmn,battler.pbDirectOpposing(true))
					if PBTypes.superEffective?(typeMod.to_f/PBTypeEffectivenesss::NORMAL_EFFECTIVE)
						# Greater weight if new Pokemon's type is effective against target
						weight = 60
					end
					list.unshift(i) if pbAIRandom(100)<weight   # Put this Pokemon first
				else
					list.push(i)   # put this Pokemon last
				end
			end
			if list.length>0
				if batonPass>=0 && @battle.pbRegisterMove(idxBattler,batonPass,false)
					PBDebug.log("[AI] #{battler.pbThis} (#{idxBattler}) will use Baton Pass to avoid Perish Song")
					return true
				end
				if @battle.pbRegisterSwitch(idxBattler,list[0])
					PBDebug.log("[AI] #{battler.pbThis} (#{idxBattler}) will switch with " +
						"#{@battle.pbParty(idxBattler)[list[0]].name}")
					return true
				end
			end
		end
		return false
	end
	
	#=============================================================================
	# Choose a replacement Pokémon
	#=============================================================================
	def pbDefaultChooseNewEnemy(idxBattler,party)
		enemies = []
		party.each_with_index do |p,i|
			enemies.push(i) if @battle.pbCanSwitchLax?(idxBattler,i)
		end
		return -1 if enemies.length==0
		return pbChooseBestNewEnemy(idxBattler,party,enemies)
	end
	
	def pbChooseBestNewEnemy(idxBattler,party,enemies)
		return -1 if !enemies || enemies.length==0
		best    = -1
		bestSum = 0
		movesData = pbLoadMovesData
		enemies.each do |i|
			pkmn = party[i]
			sum  = 0
			pkmn.moves.each do |m|
				next if m.id==0
				moveData = movesData[m.id]
				next if moveData[MOVE_BASE_DAMAGE]==0
				@battle.battlers[idxBattler].eachOpposing do |b|
					bTypes = b.pbTypes(true)
					sum += PBTypes.getCombinedEffectiveness(moveData[MOVE_TYPE],
						bTypes[0],bTypes[1],bTypes[2])
				end
			end
      #================
            types = [pkmn.type1, pkmn.type2]
            @battle.battlers[idxBattler].eachOpposing do |b|
                b.moves.each do |om|
                    next if !om || om.id==0
                    om.calcType = om.pbCalcType(b)
                    typeMod = PBTypes.getCombinedEffectiveness(om.calcType,types[0],types[1])
                    if PBTypes.superEffective?(typeMod.to_f/PBTypeEffectiveness::NORMAL_EFFECTIVE)
                        sum -= 30
                    elsif PBTypes.resistant?(om.calcType,types[0],types[1])
                        sum += 10
                    end
                end
            end
      #================
			if best==-1 || sum>bestSum
				best = i
				bestSum = sum
			end
		end
		return best
	end
end
