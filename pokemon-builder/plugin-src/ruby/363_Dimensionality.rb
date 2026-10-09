class DimensionalWarpScreen
  COLS = 3
  ROWS = 3
  PER_PAGE = COLS * ROWS  # 9
  
  # 基础传送数据（所有地点）
  BASE_WARP_DATA = [
    # === 原地区 ===
    ["茶月镇", 2, 36, 7],
    ["格诺镇", 7, 29, 11],
    ["茸舒镇", 16, 18, 36],
    ["苜蓿镇", 8, 14, 27],
    ["枫弦镇", 17, 38, 36],
    ["月央市", 12, 29, 43],
    ["隐龙市", 15, 50, 44],
    ["绯雷市", 9, 33, 34],
    ["时风镇", 13, 31, 27],
    ["曦寒镇", 11, 44, 22],
    ["柊埃联盟", 59, 26, 11],
    ["柊埃联盟（内部）", 158, 15, 10],
    ["铃兰市", 69, 32, 12],
    ["沃饶镇", 14, 19, 12],
    ["瑞乡镇", 57, 34, 11],
    ["培育屋", 38, 35, 14],
    ["绯隐牧场", 115, 30, 37],
    ["静隐路", 288, 24, 49],
    ["森楠岛", 25, 26, 18],
    ["莱法岛", 331, 23, 18],
    ["暮煦山", 431, 51, 15],
    # === 泷歧落地区 ===
    ["晴云镇", 67, 21, 16],
    ["无名小镇", 168, 21, 24],
    ["伊未镇", 126, 26, 17],
    ["雾绒镇", 189, 14, 17],
    ["雾绒码头", 191, 17, 32],
    ["影辞码头", 211, 21, 17],
    ["彼方镇", 214, 23, 19],
    ["影辞镇", 204, 25, 20],
    ["叹月镇", 282, 23, 18],
    ["清风镇", 301, 28, 13],
    ["风影镇", 305, 32, 11],
    ["夕墨镇", 303, 18, 32],
    ["缘眠镇", 317, 50, 21],
    ["古荷镇", 310, 25, 38],
    ["泷岐落花田", 313, 29, 31],
    # === 天空城 ===
    ["天空城祭坛", 201, 68, 19],
    ["幻谕岛", 142, 47, 45],
    ["灵诺森岛", 247, 24, 13],
    ["厄季斯岛", 242, 35, 14],
    ["绯焰岛", 243, 14, 13],
    ["茉克岛", 241, 26, 13],
    ["虹兰岛", 244, 41, 16],
    ["规盈岛", 269, 15, 12],
    ["落英岛", 264, 13, 44],
    ["狱怜岛", 250, 34, 32],
    ["翼霄岛", 411, 26, 28],
    ["磷火岛", 409, 33, 19],
    ["长夜屿", 408, 28, 31]
  ]
  
  # 特殊场景数据（需要条件开启）
  SPECIAL_WARP_DATA = [
    ["伊甸园", 347, 24, 61, "伊甸园", "SWITCH", 231],
    ["清澈湖", 349, 43, 56, "清澈湖", "SWITCH", 233],
    ["时空裂缝", 371, 15, 17, "裂界石窟", "SWITCH", 81],
    ["心魂岛", 233, 34, 6, "心魂之地", "SWITCH", 234],
    ["红枫雪域", 224, 59, 57, "红枫雪域", "SWITCH", 230],
    ["尘封山", 140, 18, 34, "尘封山", "SWITCH", 236],
    ["平衡之森", 376, 30, 27, "平衡之森", "SWITCH", 237],
    ["康特地区", 449, 40, 8, "初心镇", "SWITCH", 235],
    ["康特地区", 499, 11, 10, "康特联盟", "SWITCH", 241],
    ["白龙空间", 478, 25, 29, "白龙空间", "SWITCH", 238],
    ["零区研究所", 468, 15,20, "零区研究所", "SWITCH", 239],
    ["蛮荒之地", 487, 32, 28, "蛮荒之地", "SWITCH", 240]
  ]
  
  def initialize
    # 根据开关状态构建传送数据
    @warp_data = build_warp_data
    @total_pages = (@warp_data.length + PER_PAGE - 1) / PER_PAGE
    @page = 0
    @sprites = {}
    @viewport = Viewport.new(0, 0, Graphics.width, Graphics.height)
    @viewport.z = 99999
    
    # ... 其余初始化代码保持不变 ...
    initialize_ui
  end
  
  def build_warp_data
    data = BASE_WARP_DATA.dup
    
    SPECIAL_WARP_DATA.each do |special|
      name, map_id, x, y, display_name, condition_type, switch_id = special
      
      # 检查条件是否满足
      if condition_type == "SWITCH"
        if $game_switches && $game_switches[switch_id] == true
          # 添加特殊场景（使用显示名称）
          data.push([display_name, map_id, x, y])
        end
      end
    end
    
    data
  end
  
  def initialize_ui
    # 全屏半透明背景
    @sprites["bg"] = Sprite.new(@viewport)
    @sprites["bg"].bitmap = Bitmap.new(Graphics.width, Graphics.height)
    @sprites["bg"].bitmap.fill_rect(0, 0, Graphics.width, Graphics.height, Color.new(0, 0, 0, 200))
    @sprites["bg"].z = 0
    
    # 标题栏 - 高度增加到50以容纳左右提示
    @sprites["title"] = Sprite.new(@viewport)
    @sprites["title"].bitmap = Bitmap.new(Graphics.width, 50)
    @sprites["title"].bitmap.fill_rect(0, 0, Graphics.width, 50, Color.new(20, 20, 60))
    @sprites["title"].z = 1
    pbSetSystemFont(@sprites["title"].bitmap)
    
    # 中间标题
    pbDrawTextPositions(@sprites["title"].bitmap,
      [["次元传送装置", Graphics.width/2, 6, 2, Color.new(200, 220, 255), nil]])
    
    # 左侧提示
    pbDrawTextPositions(@sprites["title"].bitmap,
      [["← → 翻页", 10, 6, 0, Color.new(160, 200, 255), nil]])
    
    # 右侧提示
    pbDrawTextPositions(@sprites["title"].bitmap,
      [["C确认 Esc取消", Graphics.width - 85, 6, 2, Color.new(160, 200, 255), nil]])
    
    create_slots
    create_bottom_bar
    draw_page
  end
  
  def create_slots
    slot_w = Graphics.width / COLS
    slot_h = (Graphics.height - 96) / ROWS
    PER_PAGE.times do |i|
      col = i % COLS
      row = i / COLS
      x = col * slot_w + 8
      y = row * slot_h + 44
      w = slot_w - 16
      h = slot_h - 16
      
      bg = Sprite.new(@viewport)
      bg.bitmap = Bitmap.new(w, h)
      bg.x = x
      bg.y = y
      bg.z = 1
      @sprites["slot_bg_#{i}"] = bg
      
      txt = Sprite.new(@viewport)
      txt.bitmap = Bitmap.new(w - 16, h - 16)
      txt.x = x + 8
      txt.y = y + 8
      txt.z = 2
      @sprites["slot_txt_#{i}"] = txt
    end
  end
  
  def create_bottom_bar
    bar_y = Graphics.height - 48
    
    # 底栏
    @sprites["bar"] = Sprite.new(@viewport)
    @sprites["bar"].bitmap = Bitmap.new(Graphics.width, 48)
    @sprites["bar"].bitmap.fill_rect(0, 0, Graphics.width, 48, Color.new(20, 20, 60))
    @sprites["bar"].x = 0
    @sprites["bar"].y = bar_y
    @sprites["bar"].z = 1
    
    # 页码
    @sprites["page_text"] = Sprite.new(@viewport)
    @sprites["page_text"].bitmap = Bitmap.new(120, 32)
    @sprites["page_text"].x = Graphics.width / 2 - 60
    @sprites["page_text"].y = bar_y + 8
    @sprites["page_text"].z = 2
  end
  
  def draw_page
    start_idx = @page * PER_PAGE
    
    PER_PAGE.times do |i|
      idx = start_idx + i
      bg = @sprites["slot_bg_#{i}"]
      txt = @sprites["slot_txt_#{i}"]
      
      if idx < @warp_data.length
        name = @warp_data[idx][0]
        bg.bitmap.clear
        bg.bitmap.fill_rect(0, 0, bg.bitmap.width, bg.bitmap.height, Color.new(40, 40, 80))
        bg.bitmap.fill_rect(2, 2, bg.bitmap.width-4, bg.bitmap.height-4, Color.new(20, 20, 50))
        bg.visible = true
        
        txt.bitmap.clear
        pbSetSystemFont(txt.bitmap)
        pbDrawTextPositions(txt.bitmap,
          [[name, txt.bitmap.width/2, txt.bitmap.height/2 - 10, 2, Color.new(255, 255, 255), nil]])
        txt.visible = true
      elsif idx == @warp_data.length
        # 显示取消格子
        bg.bitmap.clear
        bg.bitmap.fill_rect(0, 0, bg.bitmap.width, bg.bitmap.height, Color.new(80, 30, 30))
        bg.bitmap.fill_rect(2, 2, bg.bitmap.width-4, bg.bitmap.height-4, Color.new(50, 15, 15))
        bg.visible = true
        
        txt.bitmap.clear
        pbSetSystemFont(txt.bitmap)
        pbDrawTextPositions(txt.bitmap,
          [["×取消", txt.bitmap.width/2, txt.bitmap.height/2 - 10, 2, Color.new(255, 180, 180), nil]])
        txt.visible = true
      else
        bg.visible = false
        txt.visible = false
      end
    end
    
    # 页码
    @sprites["page_text"].bitmap.clear
    pbSetSystemFont(@sprites["page_text"].bitmap)
    pbDrawTextPositions(@sprites["page_text"].bitmap,
      [["第 #{@page+1}/#{@total_pages} 页", 60, 6, 2, Color.new(200, 200, 255), nil]])
  end
  
  def update_slot_highlight(col, row, highlight)
    return if col < 0 || col >= COLS || row < 0 || row >= ROWS
    i = row * COLS + col
    idx = @page * PER_PAGE + i
    
    bg = @sprites["slot_bg_#{i}"]
    if !bg.visible
      return
    end
    
    if idx < @warp_data.length
      # 普通地点格子
      if highlight
        bg.bitmap.clear
        bg.bitmap.fill_rect(0, 0, bg.bitmap.width, bg.bitmap.height, Color.new(60, 140, 220))
        bg.bitmap.fill_rect(2, 2, bg.bitmap.width-4, bg.bitmap.height-4, Color.new(30, 70, 120))
      else
        bg.bitmap.clear
        bg.bitmap.fill_rect(0, 0, bg.bitmap.width, bg.bitmap.height, Color.new(40, 40, 80))
        bg.bitmap.fill_rect(2, 2, bg.bitmap.width-4, bg.bitmap.height-4, Color.new(20, 20, 50))
      end
    elsif idx == @warp_data.length
      # 取消格子
      if highlight
        bg.bitmap.clear
        bg.bitmap.fill_rect(0, 0, bg.bitmap.width, bg.bitmap.height, Color.new(220, 60, 60))
        bg.bitmap.fill_rect(2, 2, bg.bitmap.width-4, bg.bitmap.height-4, Color.new(140, 30, 30))
      else
        bg.bitmap.clear
        bg.bitmap.fill_rect(0, 0, bg.bitmap.width, bg.bitmap.height, Color.new(80, 30, 30))
        bg.bitmap.fill_rect(2, 2, bg.bitmap.width-4, bg.bitmap.height-4, Color.new(50, 15, 15))
      end
    end
  end
  
  def valid_slot?(col, row)
    i = row * COLS + col
    idx = @page * PER_PAGE + i
    @sprites["slot_bg_#{i}"].visible
  end
  
  def pbStartScreen
    @col = 0
    @row = 0
    update_slot_highlight(0, 0, true)
    
    loop do
      Graphics.update
      Input.update
      
      if Input.trigger?(Input::LEFT)
        pbPlayCursorSE
        update_slot_highlight(@col, @row, false)
        @col -= 1
        if @col < 0
          if @page > 0
            @page -= 1
            @col = COLS - 1
            @row = [@row, ROWS - 1].min
            (@col).downto(0) do |c|
              if valid_slot?(c, @row)
                @col = c
                break
              end
            end
            draw_page
          else
            @col = 0
          end
        end
        update_slot_highlight(@col, @row, true)
        
      elsif Input.trigger?(Input::RIGHT)
        pbPlayCursorSE
        update_slot_highlight(@col, @row, false)
        @col += 1
        if @col >= COLS || !valid_slot?(@col, @row)
          if @page < @total_pages - 1
            @page += 1
            @col = 0
            @row = [@row, ROWS - 1].min
            draw_page
          else
            @col -= 1
          end
        end
        update_slot_highlight(@col, @row, true)
        
      elsif Input.trigger?(Input::UP)
        pbPlayCursorSE
        update_slot_highlight(@col, @row, false)
        @row -= 1
        if @row < 0
          if @page > 0
            @page -= 1
            @row = ROWS - 1
            draw_page
          else
            @row = 0
          end
        end
        unless valid_slot?(@col, @row)
          @row += 1
        end
        update_slot_highlight(@col, @row, true)
        
      elsif Input.trigger?(Input::DOWN)
        pbPlayCursorSE
        update_slot_highlight(@col, @row, false)
        @row += 1
        if @row >= ROWS || !valid_slot?(@col, @row)
          if @page < @total_pages - 1
            @page += 1
            @row = 0
            draw_page
          else
            @row -= 1
          end
        end
        update_slot_highlight(@col, @row, true)
        
      elsif Input.trigger?(Input::C)
        pbPlayDecisionSE
        idx = @page * PER_PAGE + @row * COLS + @col
        if idx < @warp_data.length
          data = @warp_data[idx]
          pbWarpWithFade(data[1], data[2], data[3])
          break
        elsif idx == @warp_data.length
          break
        end
        
      elsif Input.trigger?(Input::B)
        pbPlayCancelSE
        break
      end
    end
    dispose
  end
  
  # 普通传送（带渐变）
  def pbWarpWithFade(map_id, x, y, direction = 2)
    # 创建黑色遮罩
    viewport = Viewport.new(0, 0, Graphics.width, Graphics.height)
    viewport.z = 999999
    
    sprite = Sprite.new(viewport)
    sprite.bitmap = Bitmap.new(Graphics.width, Graphics.height)
    sprite.bitmap.fill_rect(0, 0, Graphics.width, Graphics.height, Color.new(0, 0, 0))
    sprite.opacity = 0
    
    # 淡出
    fade_frames = 20
    fade_frames.times do
      sprite.opacity += 255 / fade_frames
      Graphics.update
    end
    sprite.opacity = 255
    Graphics.update
    
    # 隐藏并销毁界面
    @sprites.each_value do |s|
      s.visible = false if s && !s.disposed?
    end
    dispose
    
    # 传送
    $game_temp.player_new_map_id = map_id
    $game_temp.player_new_x = x
    $game_temp.player_new_y = y
    $game_temp.player_new_direction = direction
    $scene.transfer_player
    $scene.spriteset.update
    
    # 淡入
    fade_frames.times do
      sprite.opacity -= 255 / fade_frames
      Graphics.update
    end
    sprite.opacity = 0
    Graphics.update
    
    sprite.dispose
    viewport.dispose
  end
  
  def dispose
    @sprites.each_value { |s| s.dispose if s && !s.disposed? }
    @viewport.dispose
  end
end

def dimensionality_warp
  screen = DimensionalWarpScreen.new
  screen.pbStartScreen
end