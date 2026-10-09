#===============================================================================
# ■ Fly Animation by KleinStudio
# http://pokemonfangames.com
#===============================================================================
# A.I.R (Update for v21.1)
#===============================================================================
BIRD_ANIMATION_TIME = 0.15

SHOW_GEN_4_BIRD = true

#===============================================================================

class Game_Character
  def setOpacity(value)
    @opacity = value
  end
end

#-------------------
# Animation
#-------------------

def pbFlyAnimation(landing = true, pokemon = nil, item = nil, flybird_species = nil)
  if landing
    $game_player.turn_left
    pbSEPlay("flybird")
  end
  width = Graphics.width
  height = Graphics.height
  @flybird = Sprite.new
  
  # 根据预设的宝可梦来决定显示什么
  @flybird.bitmap = if flybird_species
                      # 使用预设的宝可梦（保证前后一致）
                      if isConst?(flybird_species, PBSpecies, :LATIOS)
                        pbBitmap("Graphics/Pictures/flybird_Latios")
                      elsif isConst?(flybird_species, PBSpecies, :LATIAS)
                        pbBitmap("Graphics/Pictures/flybird_Latias")
                      elsif isConst?(flybird_species, PBSpecies, :GROUDON)
                        pbBitmap("Graphics/Pictures/flybird_Groudon")
                      else
                        if SHOW_GEN_4_BIRD == false
                          pbBitmap("Graphics/Pictures/flybird")
                        else
                          pbBitmap("Graphics/Pictures/flybird_gen4")
                        end
                      end
                    else
                      # 普通飞翔：10%固拉多，否则普通鸟
                      if rand(100) < 10
                        pbBitmap("Graphics/Pictures/flybird_Groudon")
                      elsif SHOW_GEN_4_BIRD == false
                        pbBitmap("Graphics/Pictures/flybird")
                      else
                        pbBitmap("Graphics/Pictures/flybird_gen4")
                      end
                    end
                    
  @flybird.ox = @flybird.bitmap.width / 2
  @flybird.oy = @flybird.bitmap.height / 2
  
  # 起始位置（屏幕右侧外）
  start_x = width + @flybird.bitmap.width
  start_y = height / 4
  @flybird.x = start_x
  @flybird.y = start_y
  
  # 中心位置
  center_x = width / 2 + 10
  center_y = height / 2
  
  # 结束位置（屏幕左侧外）
  exit_x = -@flybird.bitmap.width
  
  # 计算总帧数
  total_frames = (BIRD_ANIMATION_TIME * Graphics.frame_rate).to_i
  total_frames = 1 if total_frames < 1  # 确保至少1帧
  
  # 飞入阶段：从右侧飞到中心
  if landing
    $game_player.setOpacity(0)  # 玩家在动画开始时消失
  end
  
  # 飞入动画
  frames = 0
  loop do
    Graphics.update
    pbUpdateSceneMap
    frames += 1
    progress = frames.to_f / total_frames
    
    if progress <= 1.0
      # 线性插值：从起始位置到中心
      @flybird.x = start_x + (center_x - start_x) * progress
      @flybird.y = start_y + (center_y - start_y) * progress
    else
      break
    end
    
    break if frames >= total_frames
  end
  
  # 如果不是降落（即起飞），在中心位置让玩家出现
  if !landing
    $game_player.setOpacity(255)
  end
  
  # 飞离动画
  frames = 0
  loop do
    Graphics.update
    pbUpdateSceneMap
    frames += 1
    progress = frames.to_f / total_frames
    
    if progress <= 1.0
      # 线性插值：从中心到左侧外
      @flybird.x = center_x + (exit_x - center_x) * progress
      @flybird.y = center_y + (start_y - center_y) * progress  # 飞回上方
    else
      break
    end
    
    break if frames >= total_frames
  end
  
  # 如果是降落，动画结束时让玩家出现
  if landing
    $game_player.setOpacity(255)
  end
  
  @flybird.dispose
  @flybird = nil
end