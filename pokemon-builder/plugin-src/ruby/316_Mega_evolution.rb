# Mega evolution (refactored)
# Credit: bo4p5687

PluginManager.register({
  :name => "Mega evolution (refactored)",
  :credits => "bo4p5687"
})

class BaseMegaEvolutionScene
  def initialize
    @sprites = {}
    @viewport = Viewport.new(0, 0, Graphics.width, Graphics.height)
    @viewport.z = 99999
    @red = {}; @green = {}; @blue = {}
  end

  # 子类可覆盖的配置
  def icon_filename; "icon_mega"; end
  def circle2_filename; "Circle 2"; end
  def bubble_filename; "Buble Pink"; end
  def bgm_name; "Mega evolution"; end

  def start(time, backdrop, pkmn)
    @previousBGM = $game_system.getPlayingBGM
    pbBGMPlay(bgm_name)
    @pkmn = pkmn

    # 时间处理
    case time
    when 1; time = "eve"
    when 2; time = "night"
    end

    backdropFilename = backdrop
    if time
      trialName = sprintf("%s_%s", backdropFilename, time)
      backdropFilename = trialName if pbResolveBitmap(sprintf("Graphics/Battlebacks/"+trialName+"_bg"))
    end

    @middlex = SCREEN_WIDTH / 2
    @middley = SCREEN_HEIGHT / 2

    # 背景
    create_sprite("bg", "#{backdropFilename}_bg", @viewport, "Battlebacks")
    set_zoom("bg", 1, 1.35)

    # 基础图层
    create_sprite("base", "Base", @viewport)
    @sprites["base"].opacity = 0

    # 保存背景颜色
    red = @sprites["bg"].color.red
    green = @sprites["bg"].color.green
    blue = @sprites["bg"].color.blue
    saveColor("bg", red, green, blue)

    # 白色屏幕
    @sprites["bg"].color.red = 255
    @sprites["bg"].color.green = 255
    @sprites["bg"].color.blue = 255
    17.times { @sprites["bg"].color.alpha += 15; pbWait(1) }
    pbWait(5)

    # 恢复背景颜色
    restoreColor("bg", red, green, blue)
    17.times { @sprites["bg"].color.alpha -= 15; pbWait(1) }
    pbWait(2)

    # 宝可梦
    @sprites["pkmn"] = Sprite.new(@viewport)
    @sprites["pkmn"].bitmap = bitmapPkmn.bitmap
    ox = @sprites["pkmn"].bitmap.width / 2
    oy = @sprites["pkmn"].bitmap.height / 2
    set_oxoy_sprite("pkmn", ox, oy)
    set_xy_sprite("pkmn", @middlex, @middley)
    pbPlayCry(@pkmn)
    pbWait(30)

    # 气泡效果
    setPositionEffectBuble

    # 红色屏幕
    @sprites["bg"].color.red = 235
    @sprites["bg"].color.green = 0
    @sprites["bg"].color.blue = 0
    17.times do
      @sprites["bg"].color.alpha += 12
      @sprites["base"].opacity += 15
      @sprites["pkmn"].color.alpha += 15
      pbWait(1)
    end
    pbWait(2)

    # 圆环
    create_sprite("circle", "Circle", @viewport)
    ox = @sprites["circle"].bitmap.width / 2
    oy = @sprites["circle"].bitmap.height / 2
    set_oxoy_sprite("circle", ox, oy)
    set_xy_sprite("circle", @middlex, @middley)
    set_zoom("circle", 0, 0)

    # 气泡移动
    setEffectBuble(0, 0); setEffectBuble(4, 1); setEffectBuble(8, 2); setEffectBuble(12, 3)
    setEffectBuble(1, 0); setEffectBuble(5, 1); setEffectBuble(9, 2); setEffectBuble(13, 3)
    setEffectBuble(2, 0); setEffectBuble(6, 1); setEffectBuble(10, 2); setEffectBuble(14, 3)
    setEffectBuble(3, 0); setEffectBuble(7, 1); setEffectBuble(11, 2); setEffectBuble(15, 3)

    # 圆环缩放动画
    zoom = 0
    15.times { zoom += 0.2; set_zoom("circle", zoom, zoom); pbWait(2) }

    # 保存宝可梦颜色
    redpkmn = @sprites["pkmn"].color.red
    greenpkmn = @sprites["pkmn"].color.green
    bluepkmn = @sprites["pkmn"].color.blue
    saveColor("pkmn", redpkmn, greenpkmn, bluepkmn)

    # 设置宝可梦白色
    @sprites["pkmn"].color.red = 255
    @sprites["pkmn"].color.green = 255
    @sprites["pkmn"].color.blue = 255
    @sprites["pkmn"].color.alpha = 255
    set_visible_sprite("circle")
    pbWait(1)

    # 光线
    create_sprite("light", "Light", @viewport)
    srcw = @sprites["light"].bitmap.width / 3
    srch = @sprites["light"].bitmap.height
    set_src_wh_sprite("light", srcw, srch)
    set_src_xy_sprite("light", 0, 0)
    set_visible_sprite("light")

    # 第二圆环
    create_sprite(circle2_filename, circle2_filename, @viewport)
    srcw = @sprites[circle2_filename].bitmap.width / 3
    srch = @sprites[circle2_filename].bitmap.height
    set_src_wh_sprite(circle2_filename, srcw, srch)
    set_src_xy_sprite(circle2_filename, 0, 0)
    set_oxoy_sprite(circle2_filename, srcw / 2, srch / 2)
    set_xy_sprite(circle2_filename, @middlex, @middley)

    # 保存圆环颜色
    redc = @sprites[circle2_filename].color.red
    greenc = @sprites[circle2_filename].color.green
    bluec = @sprites[circle2_filename].color.blue
    saveColor(circle2_filename, redc, greenc, bluec)

    # 设置圆环白色
    @sprites[circle2_filename].color.red = 255
    @sprites[circle2_filename].color.green = 255
    @sprites[circle2_filename].color.blue = 255
    @sprites[circle2_filename].color.alpha = 255
    pbWait(1)

    # 第二圆环动画
    zoom = 1
    mul = 0
    inc = 0
    9.times do
      mul += 1
      zoom += 0.1
      set_zoom(circle2_filename, zoom, zoom)
      if mul >= 7
        restoreColor(circle2_filename, redc, greenc, bluec)
        @sprites[circle2_filename].color.alpha = 0
        srcx = @sprites[circle2_filename].bitmap.width / 3 * inc
        set_src_xy_sprite(circle2_filename, srcx, 0)
        set_visible_sprite("light", true)
        srcx = @sprites["light"].bitmap.width / 3 * inc
        set_src_xy_sprite("light", srcx, 0)
        inc += 1
      end
      pbWait(5)
    end

    # 新场景
    create_sprite("scene", "#{backdropFilename}_bg", @viewport, "Battlebacks")
    set_zoom("scene", 1, 1.35)
    @sprites["scene"].color.red = 255
    @sprites["scene"].color.green = 255
    @sprites["scene"].color.blue = 255
    @sprites["scene"].color.alpha = 255

    # 恢复宝可梦颜色
    restoreColor("pkmn", redpkmn, greenpkmn, bluepkmn)
    @sprites["pkmn"].color.alpha = 0
    set_zoom("pkmn", 2, 2)

    # 恢复背景颜色
    restoreColor("bg", red, green, blue)
    @sprites["bg"].color.alpha = 0

    pbWait(10)
    set_visible_sprite(circle2_filename)
    set_visible_sprite("light")
    set_visible_sprite("base")
  end

  def megaEvolve
    # 重置形态
    @sprites["pkmn"].bitmap = bitmapPkmn.bitmap
    17.times { @sprites["scene"].opacity -= 15; pbWait(2) }
    zoom = 2
    10.times { zoom -= 0.1; set_zoom("pkmn", zoom, zoom); pbWait(2) }

    create_sprite("light 2", "Light 2", @viewport)
    ox = @sprites["light 2"].bitmap.width / 2
    oy = @sprites["light 2"].bitmap.height / 2
    set_oxoy_sprite("light 2", ox, oy)
    set_xy_sprite("light 2", @middlex, @middley - 130)

    create_sprite("icon", icon_filename, @viewport, "Pictures/Battle")
    ox = @sprites["icon"].bitmap.width / 2
    oy = @sprites["icon"].bitmap.height / 2
    set_oxoy_sprite("icon", ox, oy)
    set_xy_sprite("icon", @middlex, @middley - 130)
    set_zoom("icon", 1.5, 1.5)

    o = 0
    10.times do
      @sprites["light 2"].angle += 36
      pbPlayCry(@pkmn) if o == 5
      o += 1
      pbWait(5)
    end

    pbBGMPlay(@previousBGM)
  end

  def setXYB(xname, yname, xcoor, ycoor)
    @xyb = {} if !@xyb
    @xyb[xname] = xcoor
    @xyb[yname] = ycoor
  end

  def setPositionEffectBuble
    (0...16).each do |i|
      create_sprite("buble #{i}", bubble_filename, @viewport) if !@sprites["buble #{i}"]
      case i
      when 0..3
        xbb = @middlex / 2 - rand(10)
        ybb = @middley / 2 - rand(10)
      when 4..7
        xbb = @middlex / 2 - rand(10)
        ybb = @middley * 1.5 + rand(10)
      when 8..11
        xbb = @middlex * 1.5 + rand(10)
        ybb = @middley / 2 - rand(10)
      else
        xbb = @middlex * 1.5 + rand(10)
        ybb = @middley * 1.5 + rand(10)
      end
      setXYB("x #{i}", "y #{i}", xbb, ybb)
      set_xy_sprite("buble #{i}", xbb, ybb)
      set_visible_sprite("buble #{i}")
    end
  end

  def setEffectBuble(number, calc)
    set_visible_sprite("buble #{number}", true)
    5.times do
      xch = @xyb["x #{number}"]
      ych = @xyb["y #{number}"]
      xxx = ((@middlex - xch) / 5).floor
      yyy = ((@middley - ych) / 5).floor
      xxx2 = ((xch - @middlex) / 5).floor
      yyy2 = ((ych - @middley) / 5).floor
      case calc
      when 0
        @sprites["buble #{number}"].x += xxx
        @sprites["buble #{number}"].y += yyy
      when 1
        @sprites["buble #{number}"].x += xxx
        @sprites["buble #{number}"].y -= yyy2
      when 2
        @sprites["buble #{number}"].x -= xxx2
        @sprites["buble #{number}"].y += yyy
      else
        @sprites["buble #{number}"].x -= xxx2
        @sprites["buble #{number}"].y -= yyy2
      end
      pbWait(2)
    end
    set_visible_sprite("buble #{number}")
    pbWait(1)
  end

  def bitmapPkmn
    pbLoadPokemonBitmapSpecies(@pkmn, @pkmn.species)
  end

  def saveColor(sprite, red, green, blue)
    @red[sprite] = red
    @green[sprite] = green
    @blue[sprite] = blue
  end

  def restoreColor(sprite, red, green, blue)
    red = @red[sprite]
    green = @green[sprite]
    blue = @blue[sprite]
  end

  def create_sprite(spritename, filename, vp, dir = "Pictures/Mega Evolution")
    @sprites[spritename.to_s] = Sprite.new(vp)
    @sprites[spritename.to_s].bitmap = Bitmap.new("Graphics/#{dir}/#{filename}")
  end

  def set_oxoy_sprite(spritename, ox, oy)
    @sprites[spritename.to_s].ox = ox
    @sprites[spritename.to_s].oy = oy
  end

  def set_xy_sprite(spritename, x, y)
    @sprites[spritename.to_s].x = x
    @sprites[spritename.to_s].y = y
  end

  def set_zoom(spritename, zoom_x, zoom_y)
    @sprites[spritename.to_s].zoom_x = zoom_x
    @sprites[spritename.to_s].zoom_y = zoom_y
  end

  def set_visible_sprite(spritename, vsb = false)
    @sprites[spritename.to_s].visible = vsb
  end

  def set_src_wh_sprite(spritename, w, h)
    @sprites[spritename.to_s].src_rect.width = w
    @sprites[spritename.to_s].src_rect.height = h
  end

  def set_src_xy_sprite(spritename, x, y)
    @sprites[spritename.to_s].src_rect.x = x
    @sprites[spritename.to_s].src_rect.y = y
  end

  def endScene
    megaEvolve
    pbDisposeSpriteHash(@sprites)
  end
end

# Mega Evolution 场景
class SceneMegaEvolution < BaseMegaEvolutionScene
  def icon_filename; "icon_mega"; end
  def circle2_filename; "Circle 2"; end
end

# 原始固拉多场景
class ScenePrimalred < BaseMegaEvolutionScene
  def icon_filename; "icon_primal_Groudon"; end
  def circle2_filename; "Circle 2_Red"; end
end

# 原始盖欧卡场景
class ScenePrimalblue < BaseMegaEvolutionScene
  def icon_filename; "icon_primal_Kyogre"; end
  def circle2_filename; "Circle 2_Blue"; end
end

# 战斗中的 Mega 进化逻辑
class PokeBattle_Battle
  # 是否播放完整 Mega 动画
  def za_full_mega_animation?
    return true if !defined?($PokemonSystem) || $PokemonSystem.nil?
    return ($PokemonSystem.mega_animation || 0) == 0
  end

  def pbMegaEvolve(idxBattler)
    battler = @battlers[idxBattler]
    return if !battler || !battler.pokemon
    return if !battler.hasMega? || battler.mega?

    trainerName = nil
    if !wildBattle? || !battler.opposes?
      trainerName = pbGetOwnerName(idxBattler)
    end

    # 打破幻影
    if battler.hasActiveAbility?(:ILLUSION)
      BattleHandlers.triggerTargetAbilityOnHit(battler.ability, nil, battler, nil, self)
    end

    # Mega 进化信息
    if trainerName
      case battler.pokemon.megaMessage
      when 1
        pbDisplay(_INTL("{1}衷心的祈愿传达给{2}了！", trainerName, battler.pbThis))
      else
        pbDisplay(_INTL("{1}的{2}与\n{3}的{4}发生了反应！",
          battler.pbThis, battler.itemName, trainerName, pbGetMegaRingName(idxBattler)))
      end
    else
      pbDisplay(_INTL("{1}的力量暴走了！", battler.pbThis))
    end

    # ───────── 根据设置选择完整 / 简洁动画 ─────────
    if za_full_mega_animation?
      # ── 完整版：使用 megascene ──
      megascene = if battler.isSpecies?(:KYOGRE)
        ScenePrimalblue.new
      elsif battler.isSpecies?(:GROUDON)
        ScenePrimalred.new
      else
        SceneMegaEvolution.new
      end

      megascene.start(get_time_value, backdrop, battler.pokemon)

      battler.pokemon.makeMega
      battler.form = battler.pokemon.form
      battler.pbUpdate(true)
      @scene.pbChangePokemon(battler, battler.pokemon)
      @scene.pbRefreshOne(idxBattler)
      megascene.endScene
    else
      # ── 简洁版：压缩版动画，约 3-4 秒 ──
      pbCommonAnimation("MegaEvolution", battler)
      pbWait(30)                      # 等待闪光结束

      battler.pokemon.makeMega
      battler.form = battler.pokemon.form
      battler.pbUpdate(true)
      @scene.pbChangePokemon(battler, battler.pokemon)
      @scene.pbRefreshOne(idxBattler)
      pbWait(20)                      # 等待变身完成

      pbCommonAnimation("MegaEvolution2", battler)
      pbWait(30)                      # 等待第二次闪光
    end
    # ───────── ZA动画设置结束 ─────────

    megaName = battler.pokemon.megaName
    if !megaName || megaName == ""
      megaName = _INTL("超级{1}", PBSpecies.getName(battler.pokemon.species))
    end
    pbDisplay(_INTL("{1}超级进化为\n{2}！", battler.pbThis, megaName))

    side = battler.idxOwnSide
    owner = pbGetOwnerIndexFromBattlerIndex(idxBattler)
    @megaEvolution[side][owner] = -2

    # 特殊效果
    if battler.isSpecies?(:GENGAR) && battler.mega?
      battler.effects[PBEffects::Telekinesis] = 0
    end

    if battler.isSpecies?(:RAPIDASH) && battler.mega?
      replace_move(battler, :FLAREBLITZ, :FLAMEEXPLOSION, "爆焰角袭")
    end

    if battler.isSpecies?(:ZYGARDE) && battler.mega?
      replace_move(battler, :COREENFORCER, :NIHILLIGHT, "归无之光")
    end

    pbCalculatePriority(false, [idxBattler]) if defined?(NEWEST_BATTLE_MECHANICS) && NEWEST_BATTLE_MECHANICS
    battler.pbEffectsOnSwitchIn
  end

  # 原始回归（也支持简洁动画）
  def pbPrimalReversion(idxBattler)
    battler = @battlers[idxBattler]
    return if !battler || !battler.pokemon
    return if !battler.hasPrimal? || battler.primal?

    full_anim = za_full_mega_animation?

    if battler.isSpecies?(:KYOGRE)
      pbCommonAnimation("PrimalKyogre", battler)
      if full_anim
        megascene = ScenePrimalblue.new
        megascene.start(time, backdrop, battler.pokemon)
      end
      battler.pokemon.makePrimal
      battler.form = battler.pokemon.form
      battler.pbUpdate(true)
      @scene.pbChangePokemon(battler, battler.pokemon)
      @scene.pbRefreshOne(idxBattler)
      if full_anim
        megascene.endScene
      end
      pbCommonAnimation("PrimalKyogre2", battler)
    elsif battler.isSpecies?(:GROUDON)
      pbCommonAnimation("PrimalGroudon", battler)
      if full_anim
        megascene = ScenePrimalred.new
        megascene.start(time, backdrop, battler.pokemon)
      end
      battler.pokemon.makePrimal
      battler.form = battler.pokemon.form
      battler.pbUpdate(true)
      @scene.pbChangePokemon(battler, battler.pokemon)
      @scene.pbRefreshOne(idxBattler)
      if full_anim
        megascene.endScene
      end
      pbCommonAnimation("PrimalGroudon2", battler)
    end
    pbDisplay(_INTL("{1}原始回归了！\n{1}回到了原始形态！", battler.pbThis))
  end

  private

  def get_time_value
    return 0
  end

  def replace_move(battler, old_move, new_move, display_name)
    battler.moves.each_index do |i|
      next if !battler.moves[i]
      if isConst?(battler.moves[i].id, PBMoves, old_move)
        battler.pokemon.moves[i].id = getConst(PBMoves, new_move)
        battler.pokemon.moves[i].pp -= 1 if battler.pokemon.moves[i].pp > 0
        battler.moves[i] = PokeBattle_Move.pbFromPBMove(self, battler.pokemon.moves[i])
        pbDisplayPaused(_INTL("{1}的{2}变成了{3}！", battler.pbThis,
          PBMoves.getName(old_move), display_name))
      end
    end
  end
end