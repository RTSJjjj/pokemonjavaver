#--------------------------------------------
# 对战动画覆盖 - 邪恶组织特别版
#--------------------------------------------

# 定义组织对应的开关ID和图片名称
EVIL_TEAMS = {
  211 => "001_Rocket",    # 火箭队
  210 => "000_Shadow",    # 影宿团
  212 => "002_Magma",     # 熔岩队
  213 => "003_Aqua",      # 海洋队
  214 => "005_Plasma",    # 等离子队
  215 => "006_Hyacinth",   # 风信队
  216 => "007_Galactic"   # 银河队
}

# 保存原始方法
alias __original_pbBattleAnimationOverride pbBattleAnimationOverride

def pbBattleAnimationOverride(viewport, battletype=0, foe=nil)
  # 检查是否有激活的邪恶组织开关
  EVIL_TEAMS.each do |switch_id, image_name|
    if $game_switches[switch_id]
      return play_evil_team_vs_animation(viewport, image_name)
    end
  end
  
  # 如果没有激活的邪恶组织开关，调用原始方法
  return __original_pbBattleAnimationOverride(viewport, battletype, foe)
end

def play_evil_team_vs_animation(viewport, image_name)
  # 设置动画参数
  anim_time = (40 * 2.75).floor
  shudder_time = (40 * 1.2).floor
  zoom_time = (40 * 2.5).floor
  shudder_delta = 2
  
  # 创建视口和精灵
  viewvs = Viewport.new(0, 0, Graphics.width, Graphics.height)
  viewvs.z = viewport.z
  
  # VS 标志
  vs = Sprite.new(viewvs)
  vs.bitmap = BitmapCache.load_bitmap("Graphics/Transitions/#{image_name}")
  vs.ox = vs.bitmap.width / 2
  vs.oy = vs.bitmap.height / 2
  vs.x = Graphics.width / 2
  vs.y = Graphics.height / 2
  vs.visible = false
  
  # 闪光效果
  flash = Sprite.new(viewvs)
  flash.bitmap = BitmapCache.load_bitmap("Graphics/Transitions/vsFlash")
  flash.opacity = 0
  
  # 播放音效并显示初始闪光
  pbSEPlay("Vs sword")
  flash.opacity = 255
  vs.visible = true
  
  # 动画循环
  anim_time.times do |i|
    # 闪光效果处理
    if i < shudder_time
      # 淡出白色闪光
      flash.opacity -= (52 * 20 / 40) if flash.opacity > 0
    elsif i == shudder_time
      # 将闪光变为黑色
      flash.tone = Tone.new(-255, -255, -255)
    elsif i >= zoom_time
      # 淡入黑色
      flash.opacity += (52 * 20 / 40) if flash.opacity < 255
    end
    
    # VS标志动画
    if i < shudder_time
      # 震动效果
      j = i % (2 * 40 / 20)
      if j >= 0.5 * 40 / 20 && j < 1.5 * 40 / 20
        vs.x += shudder_delta
        vs.y -= shudder_delta
      else
        vs.x -= shudder_delta
        vs.y += shudder_delta
      end
    elsif i < zoom_time
      # 放大效果
      vs.zoom_x += 0.4 * 20 / 40
      vs.zoom_y += 0.4 * 20 / 40
    end
    
    pbWait(1)
  end
  
  # 清理资源
  flash.dispose
  vs.dispose
  viewport.color = Color.new(0, 0, 0, 255)
  
  return true
end