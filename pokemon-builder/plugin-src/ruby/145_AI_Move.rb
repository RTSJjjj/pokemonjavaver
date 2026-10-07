# * Attacks
class PokeBattle_AI
	#=============================================================================
	# Main move-choosing method
	#=============================================================================
	def pbChooseMoves(idxBattler)
		user        = @battle.battlers[idxBattler]
		wildBattler = (@battle.wildBattle? && @battle.opposes?(idxBattler))
		skill       = wildBattler ? 20 * user.pokemon.battleRank : getSkill(user.index,@battle)

		opposing = []
		user.eachOpposing do |o|
			next if !o || o.fainted?
			opposing.push(o)
		end

		choices = []
		user.eachMoveWithIndex do |m,i|
			next if !@battle.pbCanChooseMove?(idxBattler,i,false)
			if wildBattler && user.pokemon.battleRank < 2
				pbRegisterMoveWild(user,i,choices)
			else
				pbRegisterMoveTrainer(user,i,choices,skill)
			end
		end

		totalScore = choices.inject(0) { |sum, c| sum + c[1] }
    maxScore   = choices.inject(0) { |max, c| c[1] > max ? c[1] : max }
		
		if !wildBattler && skill>=PBTrainerAI.mediumSkill && maxScore>150
			stDev = pbStdDev(choices)
			if stDev>=40 && pbAIRandom(100) < 90
				preferredMoves = []
				choices.each do |c|
					move = user.moves[c[0]]
					next if c[1] < 200 && c[1] < maxScore*0.8

					move.calcType = move.pbCalcType(user)
					isPreferred = false
					isPreferred ||= move.priority > 0 && c[1] >= maxScore*0.8
					opposing.each do |o|
						if PBTypes.superEffective?(move.calcType,o.type1) ||
							PBTypes.superEffective?(move.calcType,o.type2)
							isPreferred = true
							break
						end
					end
					isPreferred ||= c[1] == maxScore
					preferredMoves.push(c) if isPreferred
				end
				preferredMoves = choices.dup if preferredMoves.empty?

				if preferredMoves.length > 0
					m = preferredMoves[pbAIRandom(preferredMoves.length)]
					PBDebug.log("[AI] #{user.pbThis} (#{user.index}) prefers #{user.moves[m[0]].name}")
					@battle.pbRegisterMove(idxBattler,m[0],false)
					@battle.pbRegisterTarget(idxBattler,m[2]) if m[2]>=0
          $nextTarget = m[2]
					$nextMove = m[0]
					$nextQue = 1
          return
				end
			end
		end
		# Decide whether all choices are bad, and if so, try switching instead
		if !wildBattler
			badMoves = false
			if ((maxScore <= 20 && user.turnCount > 2) ||
				 (maxScore <= 40 && user.turnCount > 5)) && pbAIRandom(100)<80
				badMoves = true
			end
			if !badMoves && totalScore < 100 && user.turnCount > 1
				badMoves = true
				choices.each do |c|
					next if !user.moves[c[0]].damagingMove?
					badMoves = false
					break
				end
				badMoves = false if badMoves && pbAIRandom(100) < 10
			end
			if badMoves && pbEnemyShouldWithdrawEx?(idxBattler, true)
				if $INTERNAL
					PBDebug.log("[AI] #{user.pbThis} (#{user.index}) will switch due to terrible moves lol. you should have better moves tbh")
				end
				idxParty = @battle.pbGetReplacementPokemonIndex(idxBattler)
				if idxParty >= 0
					@battle.pbRegisterSwitch(idxBattler, idxParty)
					return
				end
			end
		end
		moves = user.moves
		skill = getSkill(idxBattler,@battle)
		#@battle.pbGetOwnerFromBattlerIndex(idxBattler) rescue 0
		moves.each_with_index do |move,i|
			next if !move || move.id==0
			opposing.each do |o|
				baseDmg = pbMoveBaseDamage(move,user,o,skill)
				if pbRoughDamage(move,user,o,skill,baseDmg) >= o.hp
					$nextTarget = o.index
					$nextMove = i
					$nextQue = 1 
				end
			end
		end
		# If there are no calculated choices, pick one at random
		if choices.length==0
			if pbEnemyShouldWithdrawEx?(idxBattler, true)
				idxParty = @battle.pbGetReplacementPokemonIndex(idxBattler)
				if idxParty >= 0
					@battle.pbRegisterSwitch(idxBattler, idxParty)
					return
				end
			end
			PBDebug.log("[AI] #{user.pbThis} (#{user.index}) doesn't want to use any moves; picking one at random")
			user.eachMoveWithIndex do |_m, i|
				next if !@battle.pbCanChooseMove?(idxBattler, i, false)
				choices.push([i, 100, -1])   # Move index, score, target
			end
			if choices.length == 0   # No moves are physically possible to use; use Struggle
				@battle.pbAutoChooseMove(user.index)
			end
		end
		# Randomly choose a move from the choices and register it
		randNum = pbAIRandom(totalScore)
		choices.each do |c|
			randNum -= c[1]
			next if randNum>=0
			@battle.pbRegisterMove(idxBattler,c[0],false)
			@battle.pbRegisterTarget(idxBattler,c[2]) if c[2]>=0
			break
		end
		# Log the result
		if @battle.choices[idxBattler][2]
			if user.isSpecies?(:DEOXYS)
				m = @battle.choices[idxBattler][2]
				if m.physicalMove? || m.specialMove?
					form = rand(2) * 2  + 1
				else
					form = rand(2) + 2
				end
				user.effects[PBEffects::DeoxysForm] = form
			end
			PBDebug.log("[AI] #{user.pbThis} (#{user.index}) will use #{@battle.choices[user.index][2].name} boom boom you die")
		end
  end
	
	#=============================================================================
	# Get scores for the given move against each possible target
	#=============================================================================
	# Wild Pokémon choose their moves randomly.
	# If you dont want this and want wild battles to be hard tell me		
	def pbRegisterMoveWild(user,idxMove,choices)
		choices.push([idxMove,100,-1])   # Move index, score, target
	end
	
	# Trainer Pokémon calculate how much they want to use each of their moves.
	def pbRegisterMoveTrainer(user,idxMove,choices,skill)
		move = user.moves[idxMove]
		targetType = move.pbTarget(user)
		if PBTargets.multipleTargets?(targetType)
			# If move affects multiple battlers and you don't choose a particular one
			totalScore = 0
			@battle.eachBattler do |b|
				next if !@battle.pbMoveCanTarget?(user.index,b.index,targetType)
				score = pbGetMoveScore(move,user,b,skill)
				totalScore += ((user.opposes?(b)) ? score : -score)
			end
			choices.push([idxMove,totalScore,-1]) if totalScore>0
		elsif PBTargets.noTargets?(targetType)
			# If move has no targets, affects the user, a side or the whole field
			score = pbGetMoveScore(move,user,user,skill)
			choices.push([idxMove,score,-1]) if score>0
		else
			# If move affects one battler and you have to choose which one
			scoresAndTargets = []
			@battle.eachBattler do |b|
				next if !@battle.pbMoveCanTarget?(user.index,b.index,targetType)
				next if PBTargets.canChooseFoeTarget?(targetType) && !user.opposes?(b)
				next if b.isCommander?
				score = pbGetMoveScore(move,user,b,skill)
				scoresAndTargets.push([score,b.index]) if score>0
			end
			if scoresAndTargets.length>0
				# Get the one best target for the move
				scoresAndTargets.sort! { |a,b| b[0]<=>a[0] }
				choices.push([idxMove,scoresAndTargets[0][0],scoresAndTargets[0][1]])
			end
		end
	end
	
	#=============================================================================
	# Get a score for the given move being used against the given target
	#=============================================================================
	def pbGetMoveScore(move,user,target,skill=100)
		skill = PBTrainerAI.minimumSkill if skill<PBTrainerAI.minimumSkill
		score = 100
		score = pbGetMoveScoreFunctionCode(score,move,user,target,skill)
		if move.priority >= 1 && move.damagingMove?
			if target.hp<=target.totalhp/10 || target.hp<=10
				score += skill
			end
		end
		# A score of 0 here means it absolutely should not be used
		return 0 if score<=0
		if skill>=PBTrainerAI.mediumSkill
			# Prefer damaging moves if AI has no more Pokémon or AI is less clever
			if @battle.pbAbleNonActiveCount(user.idxOwnSide)==0
				if !(skill>=PBTrainerAI.highSkill && @battle.pbAbleNonActiveCount(target.idxOwnSide)>0)
					if move.statusMove?
						score /= 1.5
					elsif target.hp<=target.totalhp/2
						score *= 1.5
					end
				end
			end
			# Don't prefer attacking the target if they'd be semi-invulnerable
			if skill>=PBTrainerAI.highSkill && move.accuracy>0 &&
				(target.semiInvulnerable? || target.effects[PBEffects::SkyDrop]>=0)
				miss = true
				miss = false if user.hasActiveAbility?(:NOGUARD) ||
                        target.hasActiveAbility?(:NOGUARD) ||
                        user.hasActiveAbility?(:LASERSIGHTS) ||
                        user.hasActiveAbility?(:ASONEOMEGA)
				if miss && pbRoughStat(user,PBStats::SPEED,skill)>pbRoughStat(target,PBStats::SPEED,skill)
					# Knows what can get past semi-invulnerability
					if target.effects[PBEffects::SkyDrop]>=0
						miss = false if move.hitsFlyingTargets?
					else
						if target.inTwoTurnAttack?("0C9","0CC","0CE")   # Fly, Bounce, Sky Drop
							miss = false if move.hitsFlyingTargets?
						elsif target.inTwoTurnAttack?("0CA")          # Dig
							miss = false if move.hitsDiggingTargets?
						elsif target.inTwoTurnAttack?("0CB")          # Dive
							miss = false if move.hitsDivingTargets?
						end
					end
				end
				score -= 80 if miss
			end
			# Pick a good move for the Choice items
			if user.hasActiveItem?([:CHOICEBAND,:CHOICESPECS,:CHOICESCARF]) ||
         			user.hasActiveAbility?(:GORILLATACTICS)
				if move.baseDamage>=100;    score += 100
				elsif move.baseDamage>=60;  score += 60
				elsif move.damagingMove?;   score += 30
				elsif move.function=="0F2"; score += 70   # Trick
				else;                       score -= 60
				end
			end
			# If user is asleep, prefer moves that are usable while asleep
			if user.status==PBStatuses::SLEEP && !move.usableWhenAsleep?
				hasSleepMove = false
				user.eachMove do |m|
					next unless m.usableWhenAsleep?
					score -= 60
					break
				end
			end
			# If user is frozen, prefer a move that can thaw the user
			if user.status==PBStatuses::FROZEN
				if move.thawsUser?
					score += 60
				else
					user.eachMove do |m|
						next unless m.thawsUser?
						score -= 60
						break
					end
				end
			end
			# If target is frozen, don't prefer moves that could thaw them
			if target.status==PBStatuses::FROZEN
				user.eachMove do |m|
					next if m.thawsUser?
					score -= 60
					break
				end
			end
		end
		# Adjust score based on how much damage it can deal
		if move.damagingMove?
			score = pbGetMoveScoreDamage(score,move,user,target,skill)
		else   # Status moves
			# Don't prefer attacks which don't deal damage
			score -= 10
			# Account for accuracy of move
			accuracy = pbRoughAccuracy(move,user,target,skill)
			score *= accuracy/100.0
			score = 0 if score<=10 && skill>=PBTrainerAI.highSkill
		end
		score = score.to_i
		score = 0 if score<0
		return score
	end
	
	#=============================================================================
	# Add to a move's score based on how much damage it will deal (as a percentage
	# of the target's current HP)
	#=============================================================================
	def pbGetMoveScoreDamage(score,move,user,target,skill)
		# Don't prefer moves that are ineffective because of abilities or effects
		return 0 if score<=0 || pbCheckMoveImmunity(score,move,user,target,skill)
		# Calculate how much damage the move will do (roughly)
		baseDmg = pbMoveBaseDamage(move,user,target,skill)
		realDamage = pbRoughDamage(move,user,target,skill,baseDmg)
		# Account for accuracy of move
		accuracy = pbRoughAccuracy(move,user,target,skill)
		realDamage *= accuracy/100.0
		# Two-turn attacks waste 2 turns to deal one lot of damage
		if move.chargingTurnMove? || move.function=="0C2"   # Hyper Beam
			realDamage *= 2/3   # Not halved because semi-invulnerable during use or hits first turn
		end
		# Prefer flinching external effects (note that move effects which cause
		# flinching are dealt with in the function code part of score calculation)
		if skill>=PBTrainerAI.mediumSkill
			if !target.hasActiveAbility?(:INNERFOCUS) &&
				!target.hasActiveAbility?(:SHIELDDUST) &&
				target.effects[PBEffects::Substitute]==0
				canFlinch = false
				if move.canKingsRock? && user.hasActiveItem?([:KINGSROCK,:RAZORFANG])
					canFlinch = true
				end
				if user.hasActiveAbility?(:STENCH) && !move.flinchingMove?
					canFlinch = true
				end
				realDamage *= 1.3 if canFlinch
			end
		end
		# Convert damage to percentage of target's remaining HP
		damagePercentage = realDamage*100.0/target.hp
		# Don't prefer weak attacks
		#    damagePercentage /= 2 if damagePercentage<20
		# Prefer damaging attack if level difference is significantly high
		damagePercentage *= 1.2 if user.level-10>target.level
		# Adjust score
		damagePercentage = 130 if damagePercentage>130   # Treat all lethal moves the same
		damagePercentage += 50 if damagePercentage>90   # Prefer moves likely to be lethal
		score += damagePercentage.to_i
		return score
	end
end

#------------------------------------------------------------------------------#
PluginManager.register({                                                 
		:name    => "Better AI",                             
		:version => "2.0",                                   
		:link    => "https://www.pokecommunity.com/showthread.php?t=442787",             
		:credits => ["#Not Important"]
})