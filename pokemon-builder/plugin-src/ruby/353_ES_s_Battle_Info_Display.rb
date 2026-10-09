class Battle_Info_Display
  
  def initialize(battle)
    @viewport   = Viewport.new(0, 0, Graphics.width, Graphics.height)
    @viewport.z = 99999
    @battle     = battle
    @sprites    = {}
    @sprites["bg"]        = Sprite.new(@viewport)
    if $Trainer.female?
      @sprites["bg"].bitmap = Bitmap.new("Graphics/Pictures/Battle/bg_esbid_f")
    else
      @sprites["bg"].bitmap = Bitmap.new("Graphics/Pictures/Battle/bg_esbid_m")
    end
    @sprites["bg"].x      = 0
    @sprites["bg"].y      = 0
    @sprites["bg"].z      = @viewport.z
    
    @sprites["names"]    = BitmapSprite.new(Graphics.width, Graphics.height)
    @sprites["names"].x  = 0
    @sprites["names"].y  = 0
    @sprites["names"].z  = @viewport.z
    pbSetSmallFont(@sprites["names"].bitmap)
    
    @sprites["stats"]    = BitmapSprite.new(Graphics.width, Graphics.height)
    @sprites["stats"].x  = 0
    @sprites["stats"].y  = 0
    @sprites["stats"].z  = @viewport.z
    pbSetSmallFont(@sprites["stats"].bitmap)
    
    @sprites["field"]    = BitmapSprite.new(Graphics.width, Graphics.height)
    @sprites["field"].x  = 0
    @sprites["field"].y  = 0
    @sprites["field"].z  = @viewport.z
    pbSetSmallFont(@sprites["field"].bitmap)
    
    @sprites["overlay_data"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
    @sprites["field"].x  = 0
    @sprites["field"].y  = 0
    @sprites["overlay_data"].z  = @viewport.z
    
    @arrow    = AnimatedBitmap.new("Graphics/Pictures/battle/Arrow")
    @sprites["arrow"]    = BitmapSprite.new(Graphics.width, Graphics.height)
    @sprites["arrow"].bitmap.blt(0, 0, @arrow.bitmap ,Rect.new(0, 0, 20, 12))
    @sprites["arrow"].z  = @viewport.z
    
    @oys = [0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 3, 3, 3, 2, 2, 2, 1, 1, 1]
    @oy  = 0
  end
  
  def pbStartScreen
    fontColor           = Color.new(248, 248, 248)
    shadowColor         = Color.new( 64,  64,  64)
    shadowAlly          = Color.new(  0, 128, 248)
    shadowFoe           = Color.new(248, 128,  96)
    shadowRaise         = Color.new(  0, 216,  24)
    shadowLoss          = Color.new(248,  48,  48)
    namepos = []
    allies = 0
    foes   = 0
    @battle.battlers.each_with_index do |battler, i|
      next if !battler
      if i % 2 == 0
        allies += 1
      else
        foes += 1
      end
    end
    arrow_x = [
        Graphics.width / (allies * 2), Graphics.width / (foes * 2),
        Graphics.width / (allies * 2) * 3, Graphics.width / (foes * 2) * 3,
        Graphics.width / (allies * 2) * 5, Graphics.width / (foes * 2) * 5
    ]
    arrow_y = [
        Graphics.height - 164, 40, Graphics.height - 164, 40,
        Graphics.height - 164, 40
    ]
    @sprites["arrow"].x = arrow_x[0] - 10
    @sprites["arrow"].y = arrow_y[0] - 16
    @battle.battlers.each_with_index do |battler, i|
      next if !battler
      if i % 2 == 0
        shadow = shadowAlly
        x = Graphics.width / (allies * 2) * (i + 1)
        y = Graphics.height - 164
      else
        shadow = shadowFoe
        x = Graphics.width / (foes * 2) * i
        y = 40
      end
      shadow = shadowColor if battler.fainted?
      namepos.push([
        _INTL("{1}", battler.name), x, y, 2, fontColor, shadow
      ])
      # 显示宝可梦图标
      pkmn = battler.pokemon.clone
      if battler.effects[PBEffects::Illusion] 
        pkmn.species = battler.effects[PBEffects::Illusion].species
      elsif battler.effects[PBEffects::Transform]
        pkmn.species = battler.effects[PBEffects::TransformSpecies]
      end
      @sprites["pokeicon#{i}"]   = PokemonIconSprite.new(pkmn)
      @sprites["pokeicon#{i}"].x = x - 16
      @sprites["pokeicon#{i}"].y = y + 20
      @sprites["pokeicon#{i}"].z = @viewport.z
      @sprites["pokeicon#{i}"].zoom_x = 0.5
      @sprites["pokeicon#{i}"].zoom_y = 0.5
      tone = battler.fainted? ? Tone.new(0, 0, 0, 255) : Tone.new(0, 0, 0, 0)
      @sprites["pokeicon#{i}"].tone   = tone
      # 显示属性图标
      if $DEBUG || $game_switches[197] || battler.ownedEx?
        offset_size = @sprites["names"].bitmap.font.size * 0.55
        namepos[-1][1] -= offset_size
        offset_x = (battler.name.length / 3.0 - 1) * offset_size
        types = battler.pbTypes
        if !battler.is_a?(PokeBattle_FakeBattler) && battler.effects[PBEffects::Illusion]
          ils = battler.effects[PBEffects::Illusion]
          types = [ils.types[0]]
          types.push(ils.types[1]) if ils.types[1] && ils.types[0] != ils.types[1]
        end
        @sprites["type#{i}1"] = IconSprite.new(x + offset_x, y + 3)
        @sprites["type#{i}1"].setBitmap("Graphics/Pictures/Battle/icon_type/type#{types[0]}")
        @sprites["type#{i}1"].z = @viewport.z
        if types[1]
          @sprites["type#{i}2"] = IconSprite.new(x + offset_x + 16, y + 3)
          @sprites["type#{i}2"].setBitmap("Graphics/Pictures/Battle/icon_type/type#{types[1]}")
          @sprites["type#{i}2"].z = @viewport.z
        end
      end
			# 显示携带道具
      if $DEBUG || $game_switches[197] || (battler.ownedEx? && @battle.wildBattle?)
        @sprites["itemicon#{i}"]    = HeldItemIconSprite.new(0,0,pkmn)
        @sprites["itemicon#{i}"].x  = x + 8
        @sprites["itemicon#{i}"].y  = y + 28
        @sprites["itemicon#{i}"].z  = @viewport.z
        @sprites["pokeicon#{i}"].x -= 8 if @sprites["itemicon#{i}"].bitmap
      end
      if $DEBUG || $game_switches[197] || battler.pbOwnedByPlayer?
        # 显示特性名称
        namepos.push([
          _INTL("{1}", PBAbilities.getName(battler.ability)), x-2, y+50, 1, fontColor, shadow
        ])
        # 显示HP数字
        if battler.fainted?
          namepos.push(["濒死", x+2, y+50, 0, fontColor, shadowColor])
        else
          namepos.push([
            _INTL("{1}/{2}", battler.hp, battler.totalhp), x+2, y+50, 0, fontColor, shadow
          ])
        end
      end
    end
    pbDrawTextPositions(@sprites["names"].bitmap, namepos)
    
    statpos = []
    xs = [0, 32, 32, Graphics.width / 2 + 16, 32, 32, Graphics.width / 2 + 16,
          Graphics.width / 2 + 16, Graphics.width / 2 + 16]
    ys = [0, 120, 142, 120, 164, 186, 140, 164, 186]
    for i in 1...@battle.battlers[0].stages.length do
      stat_num = ""
      if $DEBUG || $game_switches[197] || @battle.battlers[0].pbOwnedByPlayer?
        stat_num = pbRoughStat(@battle.battlers[0], i)
      end
      stat = @battle.battlers[0].stages[i]
      if stat > 0
        statpos.push([
          _INTL("{1}：{2}", PBStats.getName(i), stat_num), xs[i], ys[i], 0, fontColor, shadowRaise
        ])
        statpos.push([
          _INTL("{1}", "▲" * stat), xs[i] + 104, ys[i], 0, fontColor, shadowRaise
        ])
      elsif stat < 0
        statpos.push([
          _INTL("{1}：{2}", PBStats.getName(i), stat_num), xs[i], ys[i], 0, fontColor, shadowLoss
        ])
        statpos.push([
          _INTL("{1}", "▼" * (0-stat)), xs[i] + 104, ys[i], 0, fontColor, shadowLoss
        ])
      else
        statpos.push([
          _INTL("{1}：{2}", PBStats.getName(i), stat_num), xs[i], ys[i], 0, fontColor, shadowColor
        ])
      end
    end
    if @battle.battlers[0].effects[PBEffects::FocusEnergy] > 0
      statpos.push([
        _INTL("会心："),  xs[8], ys[8], 0, fontColor, shadowRaise
      ])
      statpos.push([
        _INTL("{1}", "▲" * @battle.battlers[0].effects[PBEffects::FocusEnergy]),
            xs[8] + 96, ys[8], 0, fontColor, shadowRaise
      ])
    else
      statpos.push([
        _INTL("会心："),  xs[8], ys[8], 0, fontColor, shadowColor
      ])
    end
    statpos.push([
      _INTL("精灵回合数：{1}", @battle.battlers[0].turnCount+1), Graphics.width / 2 + 16, Graphics.height - 80, 0, 
            fontColor, shadowColor
    ])
    pbDrawTextPositions(@sprites["stats"].bitmap, statpos)
    # 显示属性克制
    show_type_calc(@battle.battlers[0])
    
    fieldpos = []
    fieldpos.push([
      _INTL("战斗回合数：{1}", @battle.turnCount+1), 32, Graphics.height - 80, 0, 
            fontColor, shadowColor
    ])
    weather = @battle.pbWeather
    weather = 0 if weather == nil
    weatherEffect = weather==0 ? "" : (@battle.pbWeather==weather ? "(有效)" : "(无效)")
    weatherName = [
      "无", "大晴天", "下雨", "沙暴", "冰雹", "大日照",
      "大雨", "乱流", "暗影", "起雾", "下雪"
    ]
    fieldpos.push([
      _INTL("天气：{1}{2}", weatherName[weather], weatherEffect), 32, Graphics.height - 56, 0, 
            fontColor, shadowColor
    ])
    if weather != PBWeather::None
      if @battle.field.weatherDuration > 0
        fieldpos.push([
          _INTL("{1}回合", @battle.field.weatherDuration),
            Graphics.width / 2 - 16, Graphics.height - 56, 1, fontColor, shadowColor
        ])
      else
        fieldpos.push([
          _INTL("持续存在"), 
            Graphics.width / 2 - 16, Graphics.height - 56, 1, fontColor, shadowColor
        ])
      end
    end
    terrain = @battle.field.terrain
    terrain = 0 if terrain == nil
    terrainName = ["无", "电气场地", "青草场地", "薄雾场地", "精神场地", "虫惑场地", "冰冷场地"]
    fieldpos.push([
      _INTL("场地：{1}", terrainName[terrain]), 
        Graphics.width / 2 + 16, Graphics.height - 56, 0, fontColor, shadowColor
    ])
    if terrain != PBBattleTerrains::None
      if @battle.field.terrainDuration > 0
        fieldpos.push([
          _INTL("{1}回合", @battle.field.terrainDuration),
            Graphics.width - 32, Graphics.height - 56, 1, fontColor, shadowColor
        ])
      else
        fieldpos.push([
          _INTL("持续存在"),
            Graphics.width - 32, Graphics.height - 56, 1, fontColor, shadowColor
        ])
      end
    end
    fieldpos.push([
      _INTL("戏法空间："), 32, Graphics.height - 32, 0, fontColor, shadowColor
    ])
    if @battle.field.effects[PBEffects::TrickRoom]>0
      fieldpos.push([
        _INTL("{1}回合", @battle.field.effects[PBEffects::TrickRoom]),
					Graphics.width / 2 - 16, Graphics.height - 32, 1, fontColor, shadowColor
      ])
    else
      fieldpos.push([
        _INTL("不存在"), Graphics.width / 2 - 16, Graphics.height - 32, 1, fontColor, shadowColor
      ])
    end
    fieldpos.push([
      _INTL("奇妙空间："), Graphics.width / 2 + 16, Graphics.height - 32, 0, fontColor, shadowColor
    ])
    if @battle.field.effects[PBEffects::WonderRoom]>0
      fieldpos.push([
        _INTL("{1}回合", @battle.field.effects[PBEffects::WonderRoom]),
					Graphics.width - 32, Graphics.height - 32, 1, fontColor, shadowColor
      ])
    else
      fieldpos.push([
        _INTL("不存在"), Graphics.width - 32, Graphics.height - 32, 1, fontColor, shadowColor
      ])
    end
    pbDrawTextPositions(@sprites["field"].bitmap, fieldpos)
    
    index = 0
    loop do
      Graphics.update
      Input.update
      break if Input.trigger?(Input::F5) || Input.trigger?(Input::B)
      @sprites["bg"].ox += 1
      @sprites["bg"].ox = 0 if @sprites["bg"].ox > 32
      if @battle.battlers.length > 1
        @sprites["arrow"].oy = @oys[@oy]
        @oy += 1
        @oy = 0 if @oy >= @oys.length
        old_index = index
        if Input.trigger?(Input::RIGHT)
          index += 2 if index < @battle.battlers.length - 2
        elsif Input.trigger?(Input::LEFT)
          index -= 2 if index > 1
        elsif Input.trigger?(Input::UP)
          index += 1 if index < @battle.battlers.length - 1 && index % 2 == 0
          index = 1 if foes == 1
        elsif Input.trigger?(Input::DOWN)
          index -= 1 if index % 2 != 0
        end
        index = old_index if !@battle.battlers[index]
        next if old_index == index
        pbSEPlay("GUI sel decision")
        @sprites["arrow"].x = arrow_x[index] - 10
        @sprites["arrow"].y = arrow_y[index] - 12
        
        @sprites["stats"].bitmap.clear
        next if @battle.battlers[index].fainted?
        statpos = []
        for i in 1...@battle.battlers[index].stages.length do
          stat_num = ""
          if $DEBUG || $game_switches[197] || @battle.battlers[index].pbOwnedByPlayer?
            stat_num = pbRoughStat(@battle.battlers[index], i)
          end
          stat = @battle.battlers[index].stages[i]
          if stat > 0
            statpos.push([
              _INTL("{1}：{2}", PBStats.getName(i), stat_num), xs[i], ys[i], 0, fontColor, shadowRaise
            ])
            statpos.push([
              _INTL("{1}", "▲" * stat), xs[i] + 104, ys[i], 0, fontColor, shadowRaise
            ])
          elsif stat < 0
            statpos.push([
              _INTL("{1}：{2}", PBStats.getName(i), stat_num), xs[i], ys[i], 0, fontColor, shadowLoss
            ])
            statpos.push([
              _INTL("{1}", "▼" * (0-stat)), xs[i] + 104, ys[i], 0, fontColor, shadowLoss
            ])
          else
            statpos.push([
              _INTL("{1}：{2}", PBStats.getName(i), stat_num), xs[i], ys[i], 0, fontColor, shadowColor
            ])
          end
        end
        if @battle.battlers[index].effects[PBEffects::FocusEnergy] > 0
          statpos.push([
            _INTL("会心："), xs[8], ys[8], 0, fontColor, shadowRaise
          ])
          statpos.push([
            _INTL("{1}", "▲" * @battle.battlers[index].effects[PBEffects::FocusEnergy]),
                xs[8] + 96, ys[8], 0, fontColor, shadowRaise
          ])
        else
          statpos.push([
            _INTL("会心："), xs[8], ys[8], 0, fontColor, shadowColor
          ])
        end
        statpos.push([
          _INTL("精灵回合数：{1}", @battle.battlers[index].turnCount+1), Graphics.width / 2 + 16, Graphics.height - 80, 0, 
                fontColor, shadowColor
        ])
        pbDrawTextPositions(@sprites["stats"].bitmap, statpos)
        # 显示属性克制
        show_type_calc(@battle.battlers[index])
      end
    end
  end
  
  def show_type_calc(battler)
    overlay_data = @sprites["overlay_data"].bitmap
    overlay_data.clear
    imgpos = []
    effective_8   = "Graphics/Pictures/Battle/effective_8"
    effective_4   = "Graphics/Pictures/Battle/effective_4"
    effective_2   = "Graphics/Pictures/Battle/effective_2"
    effective_1   = "Graphics/Pictures/Battle/effective_1"
    effective_1_2 = "Graphics/Pictures/Battle/effective_1_2"
    effective_1_4 = "Graphics/Pictures/Battle/effective_1_4"
    effective_1_8 = "Graphics/Pictures/Battle/effective_1_8"
    effective_0   = "Graphics/Pictures/Battle/effective_0"
    x_start = Graphics.width / 2 - 16 * (PBTypes.maxValue - 1) / 2
    y_start = Graphics.height - 234
    types = battler.pbTypes(true)
    type1 = types[0] || 0
    type2 = types[1] || type1
    type3 = types[2]
    if type3
      if type1 == type2
        imgpos.push(["Graphics/Pictures/Battle/icon_type/type#{type1}", x_start - 16 * 2, y_start + 8])
        imgpos.push(["Graphics/Pictures/Battle/icon_type/type#{type3}", x_start - 16, y_start + 8])
      else
        imgpos.push(["Graphics/Pictures/Battle/icon_type/type#{type1}", x_start - 16 * 3, y_start + 8])
        imgpos.push(["Graphics/Pictures/Battle/icon_type/type#{type2}", x_start - 16 * 2, y_start + 8])
        imgpos.push(["Graphics/Pictures/Battle/icon_type/type#{type3}", x_start - 16, y_start + 8])
      end
    else
      if type1 == type2
        imgpos.push(["Graphics/Pictures/Battle/icon_type/type#{type1}", x_start - 16, y_start + 8])
      else
        imgpos.push(["Graphics/Pictures/Battle/icon_type/type#{type1}", x_start - 16 * 2, y_start + 8])
        imgpos.push(["Graphics/Pictures/Battle/icon_type/type#{type2}", x_start - 16, y_start + 8])
      end
    end
    PBTypes.maxValue.times do |atkType|
      next if atkType==getConst(PBTypes,:STELLAR)
      x_pos = atkType
      x_pos -= 1 if atkType > getConst(PBTypes,:STELLAR)
      icon_type = "Graphics/Pictures/Battle/icon_type/type#{atkType}"
      imgpos.push([icon_type, x_start + (x_pos + 1) * 16, y_start])
      mult = PBTypes.getCombinedEffectiveness(atkType, type1, type2, type3)
      case mult
      when 8 * 8;       icon_mult = effective_8
      when 8 * 4;       icon_mult = effective_4
      when 8 * 2;       icon_mult = effective_2
      when 8 / 2;       icon_mult = effective_1_2
      when 8 / 4;       icon_mult = effective_1_4
      when 8 / 8;       icon_mult = effective_1_8
      when 0;           icon_mult = effective_0
      else;             icon_mult = effective_1
      end
      imgpos.push([icon_mult, x_start + (x_pos + 1) * 16, y_start + 16])
    end
    pbDrawImagePositions(overlay_data, imgpos)
  end
  
  def pbEndScreen
    @arrow.dispose
    pbDisposeSpriteHash(@sprites)
    @viewport.dispose
  end
  
  def pbRoughStat(battler,stat)
    return battler.pbSpeed if stat==PBStats::SPEED
    stageMul = [2,2,2,2,2,2, 2, 3,4,5,6,7,8]
    stageDiv = [8,7,6,5,4,3, 2, 2,2,2,2,2,2]
    stage = battler.stages[stat] + 6
    playerowned = battler.battle.internalBattle && battler.pbOwnedByPlayer?
    numbadges = battler.battle.pbPlayer.numbadges
    case stat
    when PBStats::ATTACK
      value = battler.attack
      # 携带道具修正
      if battler.hasActiveItem?(:CHOICEBAND)
        value *= 1.5
      elsif battler.hasActiveItem?(:LIGHTBALL) && battler.isSpecies?(:PIKACHU)
        value *= 2
      elsif battler.hasActiveItem?(:THICKCLUB) && (battler.isSpecies?(:CUBONE) || battler.isSpecies?(:MAROWAK))
        value *= 2
      elsif battler.hasActiveItem?(:ABYSSSWORD) && battler.isSpecies?(:SUJINRAKU)
        value *= 1.2
      end
      # 特性修正
      if battler.hasActiveAbility?([:HUGEPOWER, :PUREPOWER])
        value *= 2
      elsif battler.hasActiveAbility?(:FLOWERGIFT) && battler.isSpecies?(:CHERRIM)
        value *= 1.5
      elsif battler.hasActiveAbility?(:GUTS) && battler.pbHasAnyStatus?
        value *= 1.5
      elsif battler.hasActiveAbility?(:TOXICBOOST) && battler.poisoned?
        value *= 1.5
      elsif battler.hasActiveAbility?([:HUSTLE, :GORILLATACTICS])
        value *= 1.5
      elsif battler.hasActiveAbility?(:ORICHALCUMPULSE) && [PBWeather::Sun, PBWeather::HarshSun].include?(battler.effectiveWeather)
        value *= 4 / 3.0
      elsif battler.hasActiveAbility?(:ANGERFLAME)
        lossStages = false
        PBStats.eachMainBattleStat { |s|
          if battler.stages[s] < 0
            lossStages = true
            break
          end
        }
        value *= 1.5 if lossStages || battler.pbHasAnyStatus?
      elsif battler.hasActiveAbility?(:DEFEATIST) && user.hp <= user.totalhp / 2
        value *= 0.5
      elsif battler.hasActiveAbility?(:WANXIANG) && user.hp <= user.totalhp / 2
        value *= 0.5
      elsif battler.hasActiveAbility?(:SLOWSTART) && user.effects[PBEffects::SlowStart] > 0
        value *= 0.5
      elsif battler.burned? && !battler.hasActiveAbility?([:GUTS, :ANGERFLAME])
        value *= 0.5
      end
      # 徽章修正
      if playerowned && numbadges >= NUM_BADGES_BOOST_ATTACK
        value *= 1.1
      end
    when PBStats::DEFENSE
      value = battler.defense
      # 携带道具修正
      if battler.hasActiveItem?(:METALPOWDER) && battler.isSpecies?(:DITTO) && !battler.effects[PBEffects::Transform]
        value *= 1.5
      elsif battler.hasActiveItem?(:EVIOLITE)
        evos = pbGetEvolvedFormData(battler.pokemon.fSpecies, true)
        value *= 1.5 if evos && evos.length > 0
      end
      # 特性修正
      if battler.hasActiveAbility?(:MARVELSCALE) && battler.pbHasAnyStatus?
        value *= 1.5
      elsif battler.hasActiveAbility?(:GRASSPELT) && battler.battle.field.terrain == PBBattleTerrains::Grassy
        value *= 1.5
      end
      # 天气修正
      if battler.pbHasType?(:ICE) && battler.effectiveWeather == PBWeather::Snow
        value *= 1.5
      end
      # 徽章修正
      if playerowned && numbadges >= NUM_BADGES_BOOST_DEFENSE
        value *= 1.1
      end
    when PBStats::SPATK
      value = battler.spatk
      # 携带道具修正
      if battler.hasActiveItem?(:CHOICESPECS)
        value *= 1.5
      elsif battler.hasActiveItem?(:LIGHTBALL) && battler.isSpecies?(:PIKACHU)
        value *= 2
      elsif battler.hasActiveItem?(:DEEPSEATOOTH) && battler.isSpecies?(:CLAMPERL)
        value *= 2
      elsif battler.hasActiveItem?(:TEMPLESCEPTER) && battler.isSpecies?(:SUGARDEVOIR)
        value *= 1.2
      end
      # 特性修正
      if battler.hasActiveAbility?(:SOLARPOWER)
        value *= 1.5
      elsif battler.hasActiveAbility?(:FLAREBOOST) && battler.burned?
        value *= 1.5
      elsif battler.hasActiveAbility?(:HADRONENGINE) && battler.battle.field.terrain == PBBattleTerrains::Electric
        value *= 4 / 3.0
      elsif battler.hasActiveAbility?(:DEFEATIST) && user.hp <= user.totalhp / 2
        value *= 0.5
      elsif battler.hasActiveAbility?(:WANXIANG) && user.hp <= user.totalhp / 2
        value *= 0.5      
      end
      # 徽章修正
      if playerowned && numbadges >= NUM_BADGES_BOOST_SPATK
        value *= 1.1
      end
    when PBStats::SPDEF
      value = battler.spdef
      # 携带道具修正
      if battler.hasActiveItem?(:ASSAULTVEST)
        value *= 1.5
      elsif battler.hasActiveItem?(:EVIOLITE)
        evos = pbGetEvolvedFormData(battler.pokemon.fSpecies, true)
        value *= 1.5 if evos && evos.length > 0
      elsif battler.hasActiveItem?(:DEEPSEASCALE) && battler.isSpecies?(:CLAMPERL)
        value *= 2
      end
      # 特性修正
      if battler.hasActiveAbility?(:FLOWERGIFT) && battler.isSpecies?(:CHERRIM)
        value *= 1.5
      end
      # 天气修正
      if battler.pbHasType?(:ROCK) && battler.effectiveWeather == PBWeather::Sandstorm
        value *= 1.5
      end
      # 徽章修正
      if playerowned && numbadges >= NUM_BADGES_BOOST_SPDEF
        value *= 1.1
      end
    else
      return ""
    end
    # 古代活性/夸克充能
    if battler.effects[PBEffects::ParadoxStat] == stat
      value *= stat==PBStats::ATTACK ? 1.5 : 1.3
    end
    return (value.to_f*stageMul[stage]/stageDiv[stage]).floor
  end
  
end

class BattlerInfoScreen
  def initialize(scene)
    @scene = scene
  end

  def pbShowScreen
    @scene.pbStartScreen
    @scene.pbEndScreen
  end
end
def pbBattleInfo(battle)
  return if pbInSafari?
  scene = Battle_Info_Display.new(battle)
  screen = BattlerInfoScreen.new(scene)
  screen.pbShowScreen
end
#==============================================================================
# 重写携带道具图标
class HeldItemIconSprite < SpriteWrapper
  def initialize(x, y, pokemon, viewport = nil)
    super(viewport)
    self.x = x
    self.y = y
    self.zoom_x = 0.5
    self.zoom_y = 0.5
    @pokemon = pokemon
    @item = 0
    self.item = @pokemon.item
  end
end
def pbHeldItemIconFile(item)   # Used in the party screen
  return nil if !item || item==0
  bitmapFileName = sprintf("Graphics/Icons/item%03d",getConstantName(PBItems,item)) rescue nil
  if !pbResolveBitmap(bitmapFileName)
    
    if pbIsTechnicalRecord?(item)
      move = pbGetMachine(item)
      type = pbGetMoveData(move,MOVE_TYPE)
      bitmapFileName = sprintf("Graphics/Icons/itemRecord%s",getConstantName(PBTypes,type)) rescue nil
      if !pbResolveBitmap(bitmapFileName)
        bitmapFileName = sprintf("Graphics/Icons/itemRecord%03d",type)
      end
    else
      bitmapFileName = sprintf("Graphics/Icons/item%03d",item)
      if !pbResolveBitmap(bitmapFileName)
        bitmapFileName = sprintf("Graphics/Party/icon_item")
      end
    end
  end
  return bitmapFileName
end
#==============================================================================
# 添加一个用于判断是否已获得的方法,
# 和原版方法相比忽略训练家
class PokeBattle_Battler
  def ownedEx?
    return $Trainer.owned[displaySpecies]
  end
  alias ownedEx ownedEx?
end

PluginManager.register({
  :name    => "ESBID-影辞定制版",
  :version => "3.0",
  :link    => "https://0vej.esplus.club",
  :credits => ["ES泽洛"]
})