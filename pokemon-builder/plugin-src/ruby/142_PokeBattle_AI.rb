#===============================================================================
# ** Better AI
# ** By #Not Important
#===============================================================================
=begin
Changes:
  - There is now an AI class for over 200 skill, beast mode.
	- Mega Evolution will only be used if:
    ~ One of the AI's moves is super effective
    ~ The opponent is on low HP (1/3)
  - The switching out for AI is now *much* more sophisticated, here are a few
    things I did:
    ~ If the user has a priority move, stay in
    ~ If the user is faster than the opponent and has a super effective move,
      stay in
		~ If the opponent is in the middle of a two-turn move, and cannot attack,
      stay in.
    ~ If the user is in the last turn of perish song, switch
    ~ I did more stuff but cannot be bothered to document it all here
  - Moves are NOT chosen as a possibility if they are not:
		~ Priority
		~ Super effective
    ~ Powerful
  - If no moves fit the above conditions, choose a random one
=end
MEGAEVOMETHOD = 2 #if its 1, it will start as false and run checks to make sure it needs to, if 2, the opposite
SPIRIT_POWERS = false
#-------------------------------------------------------------------------------
# AI skill levels:
#     0:     Wild Pokémon
#     1-31:  Basic trainer (young/inexperienced)
#     32-47: Some skill
#     48-99: High skill
#     100+:  Best trainers (Gym Leaders, Elite Four, Champion)
# NOTE: A trainer's skill value can range from 0-255, but by default only four
#       distinct skill levels exist. The skill value is typically the same as
#       the trainer's base money value.
module PBTrainerAI
    # Minimum skill level to be in each AI category.
    def self.minimumSkill; return 1;   end
    def self.mediumSkill;  return 16;  end
    def self.highSkill;    return 32;  end
    def self.bestSkill;    return 48;  end
    def self.beastMode;    return 100; end
end
$nextMove   = nil
$nextTarget = nil
$nextQue    = 0
def superEffective?(battler1,battler2)
	types = [battler2.type1,battler2.type2]
	mod = 0
	battler1.moves do |m|
    next if !m || m.id == 0
		types do |t|
			if PBTypes.superEffective?(m.calcType,t)
				mod += 1
			else
				mod -= 1
			end
		end
	end
	if mod >= 1
		return true
	end
	return false
end
def getSkill(battler,battle)
  begin
    s = battle.pbGetOwnerFromBattlerIndex(battler).skill
    if !s
      s = 32
    end
    return s
  rescue
    return 32
  end
end
class PokeBattle_AI
	def initialize(battle)
		@battle = battle
	end
	
	def pbAIRandom(x); return rand(x); end
	
	def pbStdDev(choices)
		sum = 0
		n   = 0
		choices.each do |c|
			sum += c[1]
			n   += 1
		end
		return 0 if n<2
		mean = sum.to_f/n.to_f
		varianceTimesN = 0
		choices.each do |c|
			next if c[1]<=0
			deviation = c[1].to_f-mean
			varianceTimesN += deviation*deviation
		end
		# Using population standard deviation 
		# [(n-1) makes it a sample std dev, would be 0 with only 1 sample]
		return Math.sqrt(varianceTimesN/n)
	end
	
	#=============================================================================
	# Decide whether the opponent should Mega Evolve their Pokémon
	#=============================================================================
	def pbEnemyShouldMegaEvolve?(idxBattler)
		battler = @battle.battlers[idxBattler]
		opposing = []
    for i in @battle.battlers
      if i != battler
        if i
          if not(i.fainted?)
            if i.opposes?
              opposing.push(i)
            end
          end
        end
      end
    end
		moves = battler.moves
		if MEGAEVOMETHOD==1
      should = false
    else
      should = true
    end
		move   = false
    skill  = getSkill(idxBattler,@battle)
    #if @battle.pbGetOwnerFromBattlerIndex(idxBattler).skill
    #  skill = @battle.pbGetOwnerFromBattlerIndex(idxBattler).skill
    #end
		battler.moves.each do |m|
      next if !m || m.id == 0
			opposing.each do |o|
				baseDmg = pbMoveBaseDamage(m,battler,o,skill)
				if pbRoughDamage(m,battler,o,skill,baseDmg) >= o.hp
					move = false
					$nextTarget = o
					$nextMove = m
					$nextQue = 1 
				end
			end
		end
		for o in opposing
			if superEffective?(battler,o)
				move = true
			end 
		end
		for o in opposing
			if o.hp <= (o.totalhp/3).floor
				should = true
			end
		end
		if move
			should = true
		end
		if should && @battle.pbCanMegaEvolve?(idxBattler)
			PBDebug.log("[AI] #{battler.pbThis} (#{idxBattler}) will Mega Evolve")
			return true
		end
		return false
	end
	
	#=============================================================================
	# Choose an action
	#=============================================================================
	def pbDefaultChooseEnemyCommand(idxBattler)
		return if pbEnemyShouldWithdraw?(idxBattler)
		return if pbEnemyShouldUseItem?(idxBattler)
		return if @battle.pbAutoFightMenu(idxBattler)
		@battle.pbRegisterMegaEvolution(idxBattler) if pbEnemyShouldMegaEvolve?(idxBattler)
		if SPIRIT_POWERS
			@battle.pbRegisterSpiritPower(idxBattler) if pbEnemyShouldUseSpiritPower?(idxBattler)
		end
    pbChooseMoves(idxBattler)
	end
end