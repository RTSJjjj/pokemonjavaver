#===============================================================================
#
#===============================================================================
# 切换地图时刷新小地图
Events.onMapChange += proc { |_sender,*args|
  $miniMap = ESMiniMap.new if !$miniMap
  $miniMap.update
}

# 商店购物时临时隐藏
alias esmm_pbPokemonMart pbPokemonMart
def pbPokemonMart(stock,speech=nil,cantsell=false)
  $miniMap.dispose if $miniMap
  $miniMap = nil
  esmm_pbPokemonMart(stock,speech,cantsell)
  if $PokemonSystem.show_miniMap == 0
    $miniMap = ESMiniMap.new if !$miniMap
  end
end
# 若小地图显示在下方，则对话时临时隐藏
alias esmm_pbMessageDisplay pbMessageDisplay
def pbMessageDisplay(msgwindow,message,letterbyletter=true,commandProc=nil)
  if $PokemonSystem.miniMap_position[1] != 0
    $miniMap.dispose if $miniMap
    $miniMap = nil
  end
  ret = esmm_pbMessageDisplay(msgwindow,message,letterbyletter,commandProc)
  if $PokemonSystem.miniMap_position[1] != 0 && $PokemonSystem.show_miniMap == 0
    $miniMap = ESMiniMap.new if !$miniMap
  end
  return ret
end
# 昼夜切换时刷新小地图颜色
alias esmm_pbDayNightTint pbDayNightTint
def pbDayNightTint(object)
  return if !$scene.is_a?(Scene_Map)
  esmm_pbDayNightTint(object)
  if $miniMap && $miniMap.tone
    if $miniMap.tone.red != object.tone.red ||
       $miniMap.tone.green != object.tone.green ||
       $miniMap.tone.blue != object.tone.blue
      $miniMap.update
    end
  end
end

#===============================================================================
# 添加从区域地图界面进入小地图设置的入口
#===============================================================================
class PokemonRegionMap_Scene

  def pbMapScene(mode=0)
    if !@wallmap
      if !@sprites["esmm"]
        @sprites["esmm"] = BitmapSprite.new(Graphics.width, 32, @viewport)
        @sprites["esmm"].y = Graphics.height-28
        pbSetSystemFont(@sprites["esmm"].bitmap)
      end
      @sprites["esmm"].bitmap.clear
      pbDrawTextPositions(
        @sprites["esmm"].bitmap,
        [["[Z]:小地图设置",Graphics.width/2,0,2,Color.new(248,248,248),Color.new(0,0,0)]]
      )
    end
    xOffset = 0
    yOffset = 0
    newX = 0
    newY = 0
    @sprites["cursor"].x = -SQUAREWIDTH/2+(@mapX*SQUAREWIDTH)+(Graphics.width-@sprites["map"].bitmap.width)/2
    @sprites["cursor"].y = -SQUAREHEIGHT/2+(@mapY*SQUAREHEIGHT)+(Graphics.height-@sprites["map"].bitmap.height)/2
    loop do
      Graphics.update
      Input.update
      pbUpdate
      if xOffset!=0 || yOffset!=0
        distancePerFrame = 8*20/40
        xOffset += (xOffset>0) ? -distancePerFrame : (xOffset<0) ? distancePerFrame : 0
        yOffset += (yOffset>0) ? -distancePerFrame : (yOffset<0) ? distancePerFrame : 0
        @sprites["cursor"].x = newX-xOffset
        @sprites["cursor"].y = newY-yOffset
        next
      end
      @sprites["mapbottom"].maplocation = pbGetMapLocation(@mapX,@mapY)
      @sprites["mapbottom"].mapdetails  = pbGetMapDetails(@mapX,@mapY)
      ox = 0
      oy = 0
      case Input.dir8
      when 1   # lower left
        oy = 1 if @mapY<BOTTOM
        ox = -1 if @mapX>LEFT
      when 2   # down
        oy = 1 if @mapY<BOTTOM
      when 3   # lower right
        oy = 1 if @mapY<BOTTOM
        ox = 1 if @mapX<RIGHT
      when 4   # left
        ox = -1 if @mapX>LEFT
      when 6   # right
        ox = 1 if @mapX<RIGHT
      when 7   # upper left
        oy = -1 if @mapY>TOP
        ox = -1 if @mapX>LEFT
      when 8   # up
        oy = -1 if @mapY>TOP
      when 9   # upper right
        oy = -1 if @mapY>TOP
        ox = 1 if @mapX<RIGHT
      end
      if ox!=0 || oy!=0
        @mapX += ox
        @mapY += oy
        xOffset = ox*SQUAREWIDTH
        yOffset = oy*SQUAREHEIGHT
        newX = @sprites["cursor"].x+xOffset
        newY = @sprites["cursor"].y+yOffset
      end
      max_length = $game_switches[81] ? @mapdata.length : @mapdata.length - 1
      if Input.trigger?(Input::B)
        if @editor && @changed
          if pbConfirmMessage(_INTL("Save changes?")) { pbUpdate }
            pbSaveMapData
          end
          if pbConfirmMessage(_INTL("Exit from the map?")) { pbUpdate }
            break
          end
        else
          break
        end
      elsif Input.trigger?(Input::L)
        next if pbGetCurrentRegion == 3
        if @region < 0
          playerpos = (!$game_map) ? nil : pbGetMetadata($game_map.map_id,MetadataMapPosition)
          @region = playerpos[0]
        end
        @region -= 1
        @region = max_length - 1 if @region < 0
        pbSEPlay("GUI naming tab swap start")
        pbEndScene
        pbStartScene(@editor,mode)
        return pbMapScene(mode)
      elsif Input.trigger?(Input::R)
        next if pbGetCurrentRegion == 3
        if @region < 0
          playerpos = (!$game_map) ? nil : pbGetMetadata($game_map.map_id,MetadataMapPosition)
          @region = playerpos[0]
        end
        @region += 1
        @region = 0 if @region > max_length - 1
        pbSEPlay("GUI naming tab swap start")
        pbEndScene
        pbStartScene(@editor,mode)
        return pbMapScene(mode)
      elsif Input.trigger?(Input::C) && mode==1   # Choosing an area to fly to
        healspot = pbGetHealingSpot(@mapX,@mapY)
        if healspot
          if $PokemonGlobal.visitedMaps[healspot[0]] || ($DEBUG && Input.press?(Input::CTRL))
            return healspot
          end
        end
      elsif Input.trigger?(Input::C) && @editor   # Intentionally after other C input check
        pbChangeMapLocation(@mapX,@mapY)
        elsif Input.trigger?(Input::F5) && $Trainer.habitatData && $Trainer.pokedex
        playerpos = (!$game_map) ? nil : pbGetMetadata($game_map.map_id,MetadataMapPosition)
        if !playerpos
          mapindex = 0
        elsif @region>=0 && @region!=playerpos[0] && @mapdata[@region]
          mapindex = @region
        else
          mapindex = playerpos[0]
        end
        if $DEBUG && Input.trigger?(Input::CTRL)
          Kernel.pbMessage("Location: [#{mapindex}, #{@mapX}, #{@mapY}]")
        else
          find = HabitatConfig::RegionOverride.find { |key, value| key[0] == mapindex && key[1] == @mapX && key[2] == @mapY}
          if find.nil?
            hab = Habitats.getIndexByRegionCoords(@mapX,@mapY,mapindex,@mapdata)
          else
            find_ex = find[1].find { |mapid| $game_map.map_id == mapid }
            hab = Habitats.getIndexByMapID(find_ex)
          end
          if hab > -1
            pbPlayDecisionSE
            oldsprites = pbFadeOutAndHide(@sprites)
            ret = -1
            scene = HabitatDetailScene.new
            screen = HabitatDetailScreen.new(scene)
            ret = screen.pbStartScreenSingle(hab)
            pbFadeInAndShow(@sprites, oldsprites)
          else
            pbPlayBuzzerSE
          end
        end
      elsif Input.trigger?(Input::A)
        if $PokemonBag.pbHasItem?(:TOWNMAP) && !@wallmap
          pbFadeOutIn {
            esmm_scene = MiniMapOption_Scene.new
            esmm_screen = MiniMapOptionScreen.new(esmm_scene)
            esmm_screen.pbStartScreen
          }
        end
      end
    end
    pbPlayCloseMenuSE
    return nil
  end
end

#===============================================================================
# 添加快捷键
#===============================================================================
module Input
  MAP     = 97
  FANGDA  = 98
  SUOXIAO = 99
  
  class << Input
    alias esmm_buttonToKey buttonToKey
  end

  def self.buttonToKey(button)
    ret = esmm_buttonToKey(button)
    if ret.empty?
      case button
      when Input::MAP;       return [0x4D]                # M
      when Input::FANGDA;    return [0xBB]                # =+
      when Input::SUOXIAO;   return [0xBD]                # -_
      end
    end
    return ret
  end
  
end

class Scene_Map

  alias esmm_update update
  def update
    esmm_update
    if !pbMapInterpreterRunning? && $PokemonBag.pbHasItem?(:TOWNMAP)
      # 未按住 Ctrl 键时按下 M 键，打开全屏地图
      if !Input.press?(Input::CTRL) && Input.trigger?(Input::MAP)
        pbShowMap(-1, false)
      # 按住 Ctrl 键时按下 M 键，显示/隐藏小地图
      elsif Input.press?(Input::CTRL) && Input.trigger?(Input::MAP)
        $PokemonSystem.show_miniMap = 1 - $PokemonSystem.show_miniMap
        $miniMap = ESMiniMap.new if !$miniMap
        $miniMap.update_visible
      # 未按住 Ctrl 键时按下 =+ / -_键，调整小地图尺寸
      elsif !Input.press?(Input::CTRL) && Input.trigger?(Input::FANGDA)
        $PokemonSystem.miniMap_size += 1
        $PokemonSystem.miniMap_size = 0 if $PokemonSystem.miniMap_size > ESMM_Config::MAP_SIZE.length - 1
        $miniMap.update if $miniMap
      elsif !Input.press?(Input::CTRL) && Input.trigger?(Input::SUOXIAO)
        $PokemonSystem.miniMap_size -= 1
        $PokemonSystem.miniMap_size = ESMM_Config::MAP_SIZE.length - 1 if $PokemonSystem.miniMap_size < 0
        $miniMap.update if $miniMap
      # 按住 Ctrl 键时按下 =+ / -_ 键，调整小地图zoom
      elsif Input.press?(Input::CTRL) && Input.trigger?(Input::FANGDA)
        $PokemonSystem.miniMap_zoom += 1
        $PokemonSystem.miniMap_zoom = 0 if $PokemonSystem.miniMap_zoom > ESMM_Config::MAP_ZOOM.length - 1
        $miniMap.update if $miniMap
      elsif Input.press?(Input::CTRL) && Input.trigger?(Input::SUOXIAO)
        $PokemonSystem.miniMap_zoom -= 1
        $PokemonSystem.miniMap_zoom = ESMM_Config::MAP_ZOOM.length - 1 if $PokemonSystem.miniMap_zoom < 0
        $miniMap.update if $miniMap
      end
    end
  end

end