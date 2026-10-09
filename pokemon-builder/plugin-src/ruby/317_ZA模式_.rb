#===============================================================================
# ZA模式 - 超级能量系统（每侧独立，开场满）
# 带存档兼容兜底 + 防重复加载
#===============================================================================
class PokeBattle_Battle
  ZA_MAX_ENERGY = 3

  def za_mode?
    return $PokemonSystem.battle_rule == 1
  end

  # ★ 兜底初始化：兼容旧存档加载的战斗对象
  def za_ensure_init
    @za_energy        ||= [ZA_MAX_ENERGY, ZA_MAX_ENERGY]
    @za_mega_active   ||= [false, false]
    @za_mega_pkmn     ||= [nil, nil]
    @za_mega_requests ||= [[], []]
    # 如果因为存档只初始化了部分，兜底保证数组长度对
    @za_energy        = [ZA_MAX_ENERGY, ZA_MAX_ENERGY] unless @za_energy.is_a?(Array)
    @za_mega_active   = [false, false]                 unless @za_mega_active.is_a?(Array)
    @za_mega_pkmn     = [nil, nil]                     unless @za_mega_pkmn.is_a?(Array)
    @za_mega_requests = [[], []]                       unless @za_mega_requests.is_a?(Array)
  end

  unless method_defined?(:__za_initialize)
    alias __za_initialize initialize
    def initialize(*args)
      __za_initialize(*args)
      za_ensure_init
    end
  end

  unless method_defined?(:__za_pbCanMegaEvolve?)
    alias __za_pbCanMegaEvolve? pbCanMegaEvolve?
    def pbCanMegaEvolve?(idxBattler)
      return __za_pbCanMegaEvolve?(idxBattler) if !za_mode?
      za_ensure_init
      return false if $game_switches[NO_MEGA_EVOLUTION]
      battler = @battlers[idxBattler]
      return false if !battler
      return false if !battler.hasMega?
      return false if battler.mega?

      side = battler.idxOwnSide
      return false if @za_mega_active[side]
      return false if @za_energy[side] < ZA_MAX_ENERGY

      if @za_mega_requests[side] && !@za_mega_requests[side].empty?
        @za_mega_requests[side].each do |reqIdx|
          next if reqIdx == idxBattler
          return false
        end
      end

      rank = battler.pokemon.respond_to?(:battleRank) ? battler.pokemon.battleRank : 0
      return rank > 2 if wildBattle? && opposes?(idxBattler)
      return false if wildBattle? && opposes?(idxBattler)
      return true if $DEBUG && Input.press?(Input::CTRL)
      return false if battler.effects[PBEffects::SkyDrop] >= 0
      return false if !pbHasMegaRing?(idxBattler)
      return true
    end
  end

  unless method_defined?(:__za_pbRegisterMegaEvolution)
    alias __za_pbRegisterMegaEvolution pbRegisterMegaEvolution
    def pbRegisterMegaEvolution(idxBattler)
      return __za_pbRegisterMegaEvolution(idxBattler) if !za_mode?
      za_ensure_init
      return false if !pbCanMegaEvolve?(idxBattler)
      battler = @battlers[idxBattler]
      return false if !battler
      side  = battler.idxOwnSide
      owner = pbGetOwnerIndexFromBattlerIndex(idxBattler)
      @megaEvolution[side][owner] = idxBattler
      @za_mega_requests[side] << idxBattler if !@za_mega_requests[side].include?(idxBattler)
      return true
    end
  end

  unless method_defined?(:__za_pbUnregisterMegaEvolution)
    alias __za_pbUnregisterMegaEvolution pbUnregisterMegaEvolution
    def pbUnregisterMegaEvolution(idxBattler)
      return __za_pbUnregisterMegaEvolution(idxBattler) if !za_mode?
      za_ensure_init
      battler = @battlers[idxBattler]
      if battler
        side  = battler.idxOwnSide
        owner = pbGetOwnerIndexFromBattlerIndex(idxBattler)
        @megaEvolution[side][owner] = -1 if @megaEvolution[side][owner] == idxBattler
        @za_mega_requests[side].delete(idxBattler)
      end
    end
  end

  unless method_defined?(:__za_pbToggleRegisteredMegaEvolution)
    alias __za_pbToggleRegisteredMegaEvolution pbToggleRegisteredMegaEvolution
    def pbToggleRegisteredMegaEvolution(idxBattler)
      return __za_pbToggleRegisteredMegaEvolution(idxBattler) if !za_mode?
      za_ensure_init
      battler = @battlers[idxBattler]
      return if !battler
      side = battler.idxOwnSide
      if @za_mega_requests[side].include?(idxBattler)
        pbUnregisterMegaEvolution(idxBattler)
      else
        pbRegisterMegaEvolution(idxBattler)
      end
    end
  end

  unless method_defined?(:__za_pbRegisteredMegaEvolution?)
    alias __za_pbRegisteredMegaEvolution? pbRegisteredMegaEvolution?
    def pbRegisteredMegaEvolution?(idxBattler)
      return __za_pbRegisteredMegaEvolution?(idxBattler) if !za_mode?
      za_ensure_init
      battler = @battlers[idxBattler]
      return false if !battler
      return @za_mega_requests[battler.idxOwnSide].include?(idxBattler)
    end
  end

  unless method_defined?(:__za_pbMegaEvolve)
    alias __za_pbMegaEvolve pbMegaEvolve
    def pbMegaEvolve(idxBattler)
      battler = @battlers[idxBattler]
      return if !battler
      side = battler.idxOwnSide
      za_ensure_init if za_mode?

      if za_mode? && @za_mega_active[side]
        return
      end

      __za_pbMegaEvolve(idxBattler)

      return if !za_mode?
      return if !battler.pokemon

      @za_mega_requests[side].delete(idxBattler)
      @za_mega_active[side] = true
      @za_mega_pkmn[side]   = battler.pokemon
      pbDisplay(_INTL("{1}的超级进化开始了！", battler.pbThis))
    end
  end

  unless method_defined?(:zaForceExitMega)
    def zaForceExitMega(side)
      za_ensure_init
      pkmn = @za_mega_pkmn[side]
      if pkmn && pkmn.mega?
        pkmn.makeUnmega
      end
      @za_energy[side]      = 0
      @za_mega_active[side] = false
      @za_mega_pkmn[side]   = nil
    end
  end

  unless method_defined?(:pbZARevertMega)
    def pbZARevertMega(idxBattler)
      battler = @battlers[idxBattler]
      return if !battler
      pbZARevertMegaByPkmn(battler.pokemon)
    end
  end

  unless method_defined?(:pbZARevertMegaByPkmn)
    def pbZARevertMegaByPkmn(pkmn)
      return if !pkmn
      za_ensure_init

      side = -1
      @za_mega_pkmn.each_with_index do |p, i|
        if p == pkmn
          side = i
          break
        end
      end

      if side >= 0
        @za_mega_pkmn[side]   = nil
        @za_mega_active[side] = false
      end

      return if !pkmn.mega?
      pkmn.makeUnmega

      idxOnField = -1
      @battlers.each_with_index do |b, i|
        next if !b
        if b.pokemon && b.pokemon == pkmn
          idxOnField = i
          break
        end
      end

      if idxOnField >= 0
        battler = @battlers[idxOnField]
        newForm = pkmn.form
        battler.pbChangeFormTransform(newForm,
          _INTL("{1}的超级进化结束了，变回了原来的样子！", battler.pbThis))
      end
    end
  end

  unless method_defined?(:__za_pbEndOfBattle)
    alias __za_pbEndOfBattle pbEndOfBattle
    def pbEndOfBattle(*args)
      za_ensure_init
      if za_mode?
        [0, 1].each do |side|
          if @za_mega_pkmn[side]
            pbZARevertMegaByPkmn(@za_mega_pkmn[side])
          end
        end
      end
      @za_energy        = [0, 0]
      @za_mega_active   = [false, false]
      @za_mega_pkmn     = [nil, nil]
      __za_pbEndOfBattle(*args)
    end
  end

  unless method_defined?(:__za_pbCommandPhase)
    alias __za_pbCommandPhase pbCommandPhase
    def pbCommandPhase(*args)
      za_ensure_init
      @za_mega_requests = [[], []] if za_mode?
      __za_pbCommandPhase(*args)
    end
  end

  unless method_defined?(:__za_pbEndOfRoundPhase)
    alias __za_pbEndOfRoundPhase pbEndOfRoundPhase
    def pbEndOfRoundPhase(*args)
      __za_pbEndOfRoundPhase(*args)
      return if !za_mode?
      za_ensure_init

      [0, 1].each do |side|
        if @za_mega_active[side]
          pkmn = @za_mega_pkmn[side]
          next if !pkmn

          on_field = @battlers.any? do |b|
            b && !b.fainted? && b.pokemon == pkmn
          end

          if !on_field
            zaForceExitMega(side)
            pbDisplay(_INTL("超级能量归零！")) if side == 0
            next
          end

          @za_energy[side] -= 1
          if @za_energy[side] <= 0
            @za_energy[side] = 0
            pbZARevertMegaByPkmn(pkmn)
          else
            pbDisplay(_INTL("超级能量：{1}/{2}", @za_energy[side], ZA_MAX_ENERGY)) if side == 0
          end
        else
          if @za_energy[side] < ZA_MAX_ENERGY
            @za_energy[side] += 1
            pbDisplay(_INTL("超级能量：{1}/{2}", @za_energy[side], ZA_MAX_ENERGY)) if side == 0
          end
        end
      end
    end
  end
end

class PokeBattle_Scene
  unless method_defined?(:__za_pbFightMenu)
    alias __za_pbFightMenu pbFightMenu
    def pbFightMenu(battler, canMegaEvolve, *args, &block)
      if $PokemonSystem && $PokemonSystem.battle_rule == 1 && @battle
        canMegaEvolve = @battle.pbCanMegaEvolve?(battler)
      end
      __za_pbFightMenu(battler, canMegaEvolve, *args, &block)
    end
  end
end