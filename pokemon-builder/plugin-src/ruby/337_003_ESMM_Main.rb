class ESMiniMap
  attr_accessor :show_miniMap

  def initialize
    @square = ESMM_Config::SQUARE
    init_global_var
    
    @sprites = {}
    @sprites["border"] = BitmapSprite.new(@map_size * @square + 4, @map_size * @square + 4)
    @sprites["border"].z = ESMM_Config::MAP_Z - 1

    @sprites["miniMap"] = BitmapSprite.new(@map_size * @square, @map_size * @square)
    @sprites["miniMap"].z = ESMM_Config::MAP_Z

    @sprites["text"] = BitmapSprite.new(@map_size * @square + 4, @map_size * @square + 4)
    @sprites["text"].z = ESMM_Config::MAP_Z + 1

    @sprites["player"] = IconSprite.new(0, 0)
    @sprites["player"].z = ESMM_Config::MAP_Z + 1

    visible = @show_miniMap
    
    @font_color   = Color.new(248, 248, 248)
    @shadow_color = Color.new(64, 64, 64)
    @text = []
    # 初始化时调用一次刷新
    update
  end

  def init_global_var
    @show_miniMap = ($PokemonSystem.show_miniMap == 0)
    @map_position = ESMM_Config::MAP_POSITION[$PokemonSystem.miniMap_position]
    @map_zoom     = ESMM_Config::MAP_ZOOM[$PokemonSystem.miniMap_zoom]
    @map_size     = ESMM_Config::MAP_SIZE[$PokemonSystem.miniMap_size]
    @blt_map_size  = @map_size / @map_zoom
    @border_color = ESMM_Config::BORDER_COLOR[$PokemonSystem.miniMap_border]
    @opacity      = $PokemonSystem.miniMap_opacity * 255 / 100
  end

  def update
    init_map_data
    init_global_var
    
    @sprites["border"].dispose if @sprites["border"]
    @sprites["border"] = nil
    @sprites["border"] = BitmapSprite.new(@map_size * @square + 4, @map_size * @square + 4)
    @sprites["border"].x = @map_position[0] == 0 ? 0 : @map_position[0] - @map_size * @square - 4
    @sprites["border"].y = @map_position[1] == 0 ? 0 : @map_position[1] - @map_size * @square - 4
    @sprites["border"].z = ESMM_Config::MAP_Z - 1
    @sprites["border"].bitmap.fill_rect(0, 0, @map_size * @square + 4, @map_size * @square + 4, @border_color)

    # 计算起始格子位置
    start_x = @mapX - (@blt_map_size - 1) / 2
    start_y = @mapY - (@blt_map_size - 1) / 2
    # 边界处理
    start_tile_x = [[start_x, 0].max, [30 - @blt_map_size, 0].max].min
    start_tile_y = [[start_y, 0].max, [20 - @blt_map_size, 0].max].min
    # 转换为像素
    offset_x = start_tile_x * @square
    offset_y = start_tile_y * @square

    @sprites["miniMap"].dispose if @sprites["miniMap"]
    @sprites["miniMap"] = nil
    @sprites["miniMap"] = BitmapSprite.new(@blt_map_size * @square, @blt_map_size * @square)
    @sprites["miniMap"].x = @sprites["border"].x + 2
    @sprites["miniMap"].y = @sprites["border"].y + 2
    @sprites["miniMap"].z = ESMM_Config::MAP_Z
    @sprites["miniMap"].zoom_x = @map_zoom
    @sprites["miniMap"].zoom_y = @map_zoom
    outdoor = pbGetMetadata($game_map.map_id,MetadataOutdoor) && ESMM_Config::USE_DAYNIGHT_TONE
    @sprites["miniMap"].tone = outdoor ? PBDayNight.getTone : Tone.new(0, 0, 0)
    regionMap = AnimatedBitmap.new("Graphics/Pictures/#{@map[1]}")
    @sprites["miniMap"].bitmap.blt(0, 0, regionMap.bitmap, Rect.new(offset_x, offset_y, @blt_map_size * @square, @blt_map_size * @square))

    @text = []
    @sprites["text"].dispose if @sprites["text"]
    @sprites["text"] = nil
    @sprites["text"] = BitmapSprite.new(@map_size * @square, @map_size * @square)
    pbSetTinyFont(@sprites["text"].bitmap)
    @sprites["text"].x = (@map_position[0] == 0) ? 2 : @map_position[0] - @sprites["border"].bitmap.width + 2
    @sprites["text"].y = (@map_position[1] == 0) ? 2 : @map_position[1] - @sprites["border"].bitmap.height + 2
    @sprites["text"].z = ESMM_Config::MAP_Z + 1
    text_width = @sprites["text"].bitmap.width
    font_size = @sprites["text"].bitmap.font.size
    if ESMM_Config::SHOW_CURRENT_MAP_NAME
      map_name = ($game_map.name rescue nil) || "???"
    else
      map_name = pbGetMapLocation(@mapX, @mapY)
    end
    map_name = split_str(map_name, text_width - 4)
    map_name.each_with_index do |line, i|
      @text.push([_INTL("{1}", line), text_width / 2, font_size * i, 2, @font_color, @shadow_color])
    end
    if @map_size > ESMM_Config::MAP_SIZE[0]
      @text.push([_INTL("[M]键"), text_width, text_width - font_size, 1, @font_color, @shadow_color])
    end
    player_x = [$game_player.x, 0].max
    player_y = [$game_player.y, 0].max
    @text.push([_INTL("{1},{2}", player_x, player_y), 2, text_width - font_size, 0, @font_color, @shadow_color])
    pbDrawTextPositions(@sprites["text"].bitmap, @text)
    
    trainertype = $Trainer && $Trainer.trainertype ? $Trainer.trainertype : 0
    @sprites["player"].setBitmap(pbPlayerHeadFile(trainertype))
    @sprites["player"].x = @sprites["miniMap"].x + @blt_map_size * @map_zoom / 2 * @square + (start_x - start_tile_x) * @square * @map_zoom - @sprites["player"].bitmap.width / 2 * @map_zoom
    @sprites["player"].y = @sprites["miniMap"].y + @blt_map_size * @map_zoom / 2 * @square + (start_y - start_tile_y) * @square * @map_zoom - @sprites["player"].bitmap.height / 2 * @map_zoom
    @sprites["player"].zoom_x = @map_zoom
    @sprites["player"].zoom_y = @map_zoom
    @sprites["player"].tone = outdoor ? PBDayNight.getTone : Tone.new(0, 0, 0)
    update_visible
    update_opacity
    update_text
  end

  def tone
    return @sprites["miniMap"].tone
  end

  def visible?
    # 位于禁用的地图内时不显示
    return false if ESMM_Config::BAN_MAPS.include?($game_map.map_id)
    # 没有地图道具时不显示
    return false if !$PokemonBag.pbHasItem?(:TOWNMAP)
    # 开关控制是否显示
    return @show_miniMap
  end

  def update_visible
    @show_miniMap = ($PokemonSystem.show_miniMap == 0)
    @sprites["border"].visible  = visible?
    @sprites["miniMap"].visible = visible?
    @sprites["text"].visible = visible? && @map_size >= 5
    @sprites["player"].visible = visible?
  end

  def update_text
    return if !@sprites["text"] || !@sprites["text"].bitmap
    @sprites["text"].bitmap.clear
    player_x = [$game_player.x, 0].max
    player_y = [$game_player.y, 0].max
    @text[-1][0] = _INTL("{1},{2}", player_x, player_y)
    pbDrawTextPositions(@sprites["text"].bitmap, @text)
  end

  def update_opacity
    @opacity = ($PokemonSystem.miniMap_opacity * 2.55).to_i
    @sprites["border"].opacity = @opacity / 2
    @sprites["miniMap"].opacity = @opacity
    @sprites["text"].opacity = @opacity
    @sprites["player"].opacity = @opacity
  end

  def dispose
    pbDisposeSpriteHash(@sprites)
  end

  def init_map_data
    @mapdata = pbLoadTownMapData
    @region = pbGetCurrentRegion
    playerpos = (!$game_map) ? nil : pbGetMetadata($game_map.map_id,MetadataMapPosition)
    if !playerpos
      @map     = @mapdata[0]
      @mapX    = 0
      @mapY    = 0
    elsif @region>=0 && @region!=playerpos[0] && @mapdata[@region]
      @map     = @mapdata[@region]
      @mapX    = 0
      @mapY    = 0
    else
      @map     = @mapdata[playerpos[0]]
      @mapX    = playerpos[1]
      @mapY    = playerpos[2]
      mapsize = (!$game_map) ? nil : pbGetMetadata($game_map.map_id,MetadataMapSize)
      if mapsize && mapsize[0] && mapsize[0]>0
        sqwidth  = mapsize[0]
        sqheight = (mapsize[1].length*1.0/mapsize[0]).ceil
        if sqwidth>1
          @mapX += ($game_player.x*sqwidth/$game_map.width).floor
        end
        if sqheight>1
          @mapY += ($game_player.y*sqheight/$game_map.height).floor
        end
      end
    end
    if !@map
      pbMessage(_INTL("找不到地图数据"))
      return false
    end
  end

  def pbGetMapLocation(x,y)
    return "" if !@map[2]
    for loc in @map[2]
      if loc[0]==x && loc[1]==y
        if !loc[7] || (!@wallmap && $game_switches[loc[7]])
          maploc = pbGetMessageFromHash(MessageTypes::PlaceNames,loc[2])
          return @editor ? loc[2] : maploc
        else
          return ""
        end
      end
    end
    return ""
  end

  def split_str(str, width)
    length = str.length
    ch_width = @sprites["text"].bitmap.font.size
    for ch_i in 0...str.length
      if ch_width * ch_i >= width - 4
        length = ch_i
        break
      end
    end
   str_arr = length > 0 ? str.scan(/.{1,#{length}}/) : str
    return str_arr
  end

end

# 移动时刷新坐标
Events.onStepTaken += proc {
  $miniMap = ESMiniMap.new if !$miniMap
  $miniMap.update_text
}