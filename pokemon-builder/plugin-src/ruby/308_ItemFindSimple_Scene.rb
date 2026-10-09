class ItemFindSimple_Scene
  def initialize(item, qty, berry_pick = false)
    disposeTempItem(1)
    @item = item
    @qty = qty
    @berry_pick = berry_pick
    @width = 184
    @height = 28
    @x = Graphics.width - @width
    @y = 192 - ($temp_item_found ? $temp_item_found.length : 0) * @height
    @sprites = {}
    
    # 创建背景
    @sprites["bg"] = BitmapSprite.new(@width, @height)
    @sprites["bg"].x = @x
    @sprites["bg"].y = @y
    @sprites["bg"].z = 99998
    
    # 如果是树果采摘，使用不同的背景颜色
    if @berry_pick
      @sprites["bg"].bitmap.fill_rect(0, 0, @width, @height, Color.new(50, 150, 50, 128))  # 绿色背景
    else
      @sprites["bg"].bitmap.fill_rect(0, 0, @width, @height, Color.new(0, 0, 0, 64))  # 默认黑色背景
    end
    
    # 添加物品图标
    @sprites["icon"] = ItemIconSprite.new(@x+14, @y + 14, @item)
    @sprites["icon"].zoom_x = 0.5
    @sprites["icon"].zoom_y = 0.5
    @sprites["icon"].z = 99998
    
    # 添加文字
    @sprites["text"] = BitmapSprite.new(@width, @height)
    @sprites["text"].x = @x
    @sprites["text"].y = @y
    @sprites["text"].z = 99998
    pbSetSmallFont(@sprites["text"].bitmap)
    
    color = Color.new(248, 248, 248)
    shadow = Color.new(64, 64, 64)
    
    # 根据不同情况显示不同的文字
    if @berry_pick
      textpos = [
        [_INTL("{1}", PBItems.getName(item)), 28, 5, 0, color, shadow],
        [_INTL("×{1}", qty), @width-2, 5, 1, color, shadow]
      ]
    else
      textpos = [
        [_INTL("{1}", PBItems.getName(item)), 28, 5, 0, color, shadow],
        [_INTL("×{1}", qty), @width-2, 5, 1, color, shadow]
      ]
    end
    
    pbDrawTextPositions(@sprites["text"].bitmap, textpos)
    
    # 播放音效
    if @berry_pick
      pbSEPlay("Berry pick") if pbResolveAudioSE("Berry pick")
    else
      pbSEPlay("Item get") if pbResolveAudioSE("Item get")
    end
  end
  
  def dispose
    pbDisposeSpriteHash(@sprites)
  end
end

class Sprite_Character
  alias temp_item_update update
  def update
    temp_item_update
    disposeTempItem(3)
  end
end

def disposeTempItem(sec=1)
  return unless $temp_item_found  # 如果变量未初始化，直接返回
  
  $temp_item_found.each_with_index do |temp, i|
    break if Time.now.to_i < temp[1] + sec
    $temp_item_found[i][0].dispose if $temp_item_found[i][0]
    $temp_item_found[i] = nil 
  end
  $temp_item_found.compact!
end

# 修改树果采摘函数
def pbPickBerry(berry, qty=1)
  if berry.is_a?(String) || berry.is_a?(Symbol)
    berry = getID(PBItems, berry)
  end
  
  itemname = (qty > 1) ? PBItems.getNamePlural(berry) : PBItems.getName(berry)
  
  if !$PokemonBag.pbCanStore?(berry, qty)
    Kernel.pbMessage(_INTL("太可惜了...\n背包已满。"))
    return
  end
  
  # 将物品添加到背包
  $PokemonBag.pbStoreItem(berry, qty)
  
  # 创建动画场景
  scene = ItemFindSimple_Scene.new(berry, qty, true)
  
  # 添加到全局数组以便后续处理
  $temp_item_found = [] unless $temp_item_found
  $temp_item_found.push([scene, Time.now.to_i])
  
  # 更新树果数据（根据您使用的树果系统）
  interp = pbMapInterpreter
  berryData = interp.getVariable
  
  # 兼容不同版本的树果系统
  if defined?(NEWBERRYPLANTS) && NEWBERRYPLANTS
    berryData = [0, 0, 0, 0, 0, 0, 0, 0]
  else
    berryData = [0, 0, false, 0, 0, 0]
  end
  
  interp.setVariable(berryData)
  
  # 设置事件开关（假设事件有开关A）
  thisEvent = interp.get_character(0)
  pbSetSelfSwitch(thisEvent.id, "A", true)
  
  return true
end

# 修改常规物品拾取函数以使用新系统
def pbItemBall(item, quantity=1)
  if item.is_a?(String) || item.is_a?(Symbol)
    item = getID(PBItems, item)
  end
  
  return false if !item || item <= 0 || quantity < 1
  
  itemname = (quantity > 1) ? PBItems.getNamePlural(item) : PBItems.getName(item)
  
  if $PokemonBag.pbStoreItem(item, quantity)  # 如果物品可以拾取
    # 创建动画场景
    scene = ItemFindSimple_Scene.new(item, quantity, false)
    
    # 添加到全局数组以便后续处理
    $temp_item_found = [] unless $temp_item_found
    $temp_item_found.push([scene, Time.now.to_i])
    
    return true
  else  # 无法添加物品
    if $ItemData[item][ITEMUSE] == 3 || $ItemData[item][ITEMUSE] == 4
      Kernel.pbMessage(_INTL("\\l[2]{1}找到了 \\c[1]{2}\\c[0]！\\wtnp[20]", $Trainer.name, itemname))
    elsif isConst?(item, PBItems, :LEFTOVERS)
      Kernel.pbMessage(_INTL("\\l[2]{1}找到了一些 \\c[1]{2}\\c[0]！\\wtnp[20]", $Trainer.name, itemname))
    elsif quantity > 1
      Kernel.pbMessage(_INTL("\\l[2]{1}找到了 {2} 个 \\c[1]{3}\\c[0]！\\wtnp[20]", $Trainer.name, quantity, itemname))
    else
      Kernel.pbMessage(_INTL("\\l[2]{1}找到了 \\c[1]{2}\\c[0]！\\wtnp[20]", $Trainer.name, itemname))
    end
    Kernel.pbMessage(_INTL("\\l[2]但是背包已满..."))
    return false
  end
end

# 兼容性包装
def Kernel.pbItemBall(item, quantity=1)
  pbItemBall(item, quantity)
end

# 兼容性包装（如果已经有旧的pbPickBerry函数）
begin
  alias old_pbPickBerry pbPickBerry
rescue
  # 如果没有旧函数，就不需要别名
end