class FastCatch_Scene
  
  def initialize(battle, idxBattler, balls)
    @viewport   = Viewport.new(0, 48, Graphics.width, Graphics.height-192)
    @viewport.z = 100000
    @battle     = battle
    @battler    = @battle.battlers[idxBattler]
    @balls      = balls
    @index      = [@battle.esfc_ball_index, @balls.length - 1].min
    @sprites    = {}
    @sprites["bg"]        = Sprite.new(@viewport)
    if $Trainer.female?
      @sprites["bg"].bitmap = Bitmap.new("Graphics/Pictures/Battle/bg_moveinfo_f")
    else
      @sprites["bg"].bitmap = Bitmap.new("Graphics/Pictures/Battle/bg_moveinfo_m")
    end
    @sprites["bg"].x      = 0
    @sprites["bg"].y      = 0
    @sprites["bg"].z      = @viewport.z
    
    @half_width  = Graphics.width / 2
    @half_height = Graphics.height / 2
    @arrow    = AnimatedBitmap.new("Graphics/Pictures/battle/Arrow")
    @sprites["arrow"]    = BitmapSprite.new(Graphics.width, Graphics.height)
    @sprites["arrow"].bitmap.blt(0, 0, @arrow.bitmap ,Rect.new(0, 0, 20, 12))
    @sprites["arrow"].x = @half_width - 10
    @sprites["arrow"].y = @half_height / 2
    @sprites["arrow"].z  = @viewport.z + 1
    
    @sprites["overlay"]   = BitmapSprite.new(Graphics.width, @half_height)
    @sprites["overlay"].x = 0
    @sprites["overlay"].y = 48
    @sprites["overlay"].z = @viewport.z + 1
    pbSetSmallFont(@sprites["overlay"].bitmap)
    
    @oys = [0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 3, 3, 3, 2, 2, 2, 1,1, 1]
    @oy  = 0
    
    @base           = Color.new(248, 248, 248)
    @shadow         = Color.new( 64,  64,  64)
  end
  
  def pbStartScreen
    drawBallInfo
    loop do
      Graphics.update
      Input.update
      @sprites["bg"].ox += 1
      @sprites["bg"].ox = 0 if @sprites["bg"].ox > 32
      @sprites["arrow"].oy = @oys[@oy]
      @oy += 1
      @oy = 0 if @oy >= @oys.length
      if Input.repeat?(Input::LEFT)
        next if @balls.length == 1
        @index -= 1
        @index = @balls.length - 1 if @index < 0
      elsif Input.repeat?(Input::RIGHT)
        next if @balls.length == 1
        @index += 1
        @index = 0 if @index >= @balls.length
      elsif Input.trigger?(Input::L)
        next if @balls.length < 5
        @index -= 5
        @index = 0 if @index < 0
      elsif Input.trigger?(Input::R)
        next if @balls.length < 5
        @index += 5
        @index = @balls.length - 1 if @index >= @balls.length
      elsif Input.trigger?(Input::C)
        if isConst?(@balls[@index], PBItems, :MASTERBALL)
          next if !pbConfirmMessageSerious(_INTL("确定要扔出大师球吗？"))
          #next if !@battle.pbDisplayConfirmSerious(_INTL("确定要扔出大师球吗？"))
        end
        self.pbEndScreen
        @battle.pbThrowPokeBall(@battler.index, @balls[@index])
        $PokemonBag.pbDeleteItem(@balls[@index])
        @battle.end_command = true
        @battle.decision = 4 if !@battle.caughtPokemon.empty?
        break
      elsif Input.trigger?(Input::A) || Input.trigger?(Input::B)
        break
      end
      if @battle.esfc_ball_index != @index
        pbSEPlay("GUI naming tab swap start", 80)
        @battle.esfc_ball_index = @index
        drawBallInfo
      end
    end
  end
  
  def drawBallInfo
    @sprites["overlay"].bitmap.clear
    y = @half_height / 2
    textpos = [
      [_INTL("要扔出哪一个精灵球？[{1}/{2}]", @index + 1, @balls.length),
       @half_width, y - 86, 2, @base, @shadow]
    ]
    @balls.each_with_index do |ball, i|
      @sprites["ball_#{i}"].dispose if @sprites["ball_#{i}"]
      x = @half_width + 100 * (i - @index)
      @sprites["ball_#{i}"]   = ItemIconSprite.new(x, y - 12, ball, @viewport)
      @sprites["ball_#{i}"].z = @viewport.z + 1
      textpos.push(
        [_INTL("{1}×{2}", PBItems.getName(ball), $PokemonBag.pbQuantity(ball)),
         x, y + 12, 2, @base, @shadow]
      )
    end
    pbDrawTextPositions(@sprites["overlay"].bitmap, textpos)
    # 道具描述
    drawDescription
  end
  
  def drawDescription
    overlay = @sprites["overlay"].bitmap
    if @balls[@index]
      description = pbGetMessage(MessageTypes::ItemDescriptions, @balls[@index])
      desc = description.gsub(' ', '')
      length = desc.length
      for ch_i in 0...desc.length
        ch_width = overlay.text_size("字").width
        if ch_width * ch_i >= Graphics.width - 136
          length = ch_i
          break
        end
      end
      desc_arr = desc.scan(/.{1,#{length}}/)
      desc = ""
      desc_arr.each do |r|
        desc += r + "\n"
      end
    else
      desc = "道具描述"
    end
    drawTextEx(overlay, 62, @half_height / 2 + 36, Graphics.width - 124, 2, desc, @base, @shadow, 24)
  end

  def pbEndScreen
    pbDisposeSpriteHash(@sprites) if @sprites
    @viewport.dispose if @viewport
  end
end

class FastCatchScreen
  def initialize(scene)
    @scene = scene
  end

  def pbShowScreen
    @scene.pbStartScreen
    @scene.pbEndScreen
  end
end

def fastCatching(battle, idxBattler)
  battle.end_command = false
  balls = []
  for i in 0...$BallTypes.size
    ball = pbBallTypeToItem(i)
    next if !$PokemonBag.pbHasItem?(ball)
    balls.push(ball)
  end
  if balls.empty?
    battle.pbDisplay(_INTL("没有可供捕捉的精灵球。"))
    return false
  end
  scene = FastCatch_Scene.new(battle, idxBattler, balls)
  screen = FastCatchScreen.new(scene)
  screen.pbShowScreen
end