class Headtop_Name
  attr_reader :visible

  def initialize(sprite, event, viewport = nil)
    @headtopname   = $PokemonSystem.headtopname || 0
    @csprite       = sprite
    @event         = event
    @viewport      = viewport
    @base_color    = Color.new(248, 248, 248)
    @shadow_color  = Color.new(24, 24, 24)
    @male_color    = PREFIX_COLOR["#m"]
    @female_color  = PREFIX_COLOR["#f"]
    @disposed      = false
    @name          = nil
    @female        = $Trainer && $Trainer.female?
    @tsprite       = nil
    @name_changed  = false
    @last_event_name = nil
    @last_trainer_name = $Trainer ? $Trainer.name : nil
    @last_trainer_female = @female
    refreshName
    update
  end

  def refreshName
    return unless @event
    old_name = @name
    old_color = @shadow_color
    # 如果是主角
    if @event == $game_player
      return unless $Trainer
      @female = $Trainer.female?
      @shadow_color = @female ? @female_color : @male_color
      @name = $Trainer.name
    # 如果是NPC
    else
      return unless @event.name.include?('#')
      raw_name = @event.name[/\s*#(.+)/i]
      return unless raw_name
      color = nil
      PREFIX_COLOR.each do |key, c|
        if raw_name.include?(key)
          color = c
          raw_name = raw_name.gsub(key, '')
          break
        end
      end
      raw_name = raw_name.gsub('#', '')
      raw_name.gsub!(/\\pn/i, $Trainer.name) if $Trainer
      raw_name.gsub!(/\\rn/i, $Trainer.rivalName) if $Trainer
      @name = raw_name
      @shadow_color = color || SPECIAL_NAME_COLORS[@name] || Color.new(24, 24, 24)
    end
    @name_changed = true if @name != old_name || @shadow_color != old_color
  end

  # 新增方法：判断事件精灵是否在屏幕内
  def on_screen?
    return false if !@csprite || @csprite.disposed?
    # 精灵的 ox/oy 通常是图片中心，src_rect 为实际显示区域
    half_w = @csprite.ox || 0
    half_h = @csprite.oy || 0
    # 用矩形相交判断，避免主角刚好卡在边缘时名字闪烁
    return @csprite.x + half_w > 0 &&
           @csprite.x - half_w < Graphics.width &&
           @csprite.y + half_h > 0 &&
           @csprite.y - half_h < Graphics.height
  end

  def update
    return if disposed?
    # 如果是主角，检查名字是否改变
    if @event == $game_player
      if $Trainer && ($Trainer.name != @last_trainer_name || $Trainer.female? != @last_trainer_female)
        @last_trainer_name = $Trainer.name
        @last_trainer_female = $Trainer.female?
        refreshName
      end
    elsif @event.name != @last_event_name
      @last_event_name = @event.name
      refreshName
    end
    # 如果精灵不可见或透明度为0，则隐藏名字
    if !@csprite.visible || @csprite.opacity == 0
      if @tsprite
        @tsprite.visible = false
      end
      return
    end
    # 如果名字精灵不存在且名字不为空，则创建名字精灵
    if !@tsprite && @name
      @tsprite = BitmapSprite.new(256, 48)
      pbSetSmallFont(@tsprite.bitmap)
      @name_changed = true
    end
    # 如果名字已更改且精灵可见，则更新名字显示
    if @tsprite && @name_changed
      @tsprite.bitmap.clear
      tpos = [
        [_INTL("{1}", @name), 128, NAME_OFFSET_OY, 2, @base_color, @shadow_color, NAME_BORDER]
      ]
      pbDrawTextPositions(@tsprite.bitmap, tpos)
      @name_changed = false
    end
    # 如果名字精灵存在，则更新其位置和可见性
    if @tsprite
      @tsprite.x       = @csprite.x - 128
      @tsprite.y       = @csprite.y - @csprite.src_rect.height - NAME_OFFSET_Y
      @tsprite.z       = 9
      @tsprite.tone    = $game_screen.tone
      @tsprite.opacity = NAME_OPACITY
      @headtopname    = $PokemonSystem.headtopname || 0
      # 修改处：加入 on_screen? 判定，屏幕外的 NPC 不再显示名字
      @tsprite.visible = $game_map && $game_map.map_id != 1 &&
                         @event.character_name != '' &&
                         on_screen? &&
                         ((@headtopname == 1 && @event == $game_player) ||
                          (@headtopname == 2 && @event != $game_player) ||
                          (@headtopname == 3))
    end
  end

  def dispose
    return if @disposed
    @tsprite.dispose if @tsprite
    @tsprite  = nil
    @disposed = true
  end

  def disposed?
    @disposed
  end

  def visible=(value)
    @visible = value
    return unless @tsprite
    @tsprite.visible = false if !@tsprite.disposed?
  end
end