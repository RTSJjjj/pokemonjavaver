def pbCrystalWarp
  return if !$DEBUG && !$PokemonBag.pbHasItem?(:HEAVENCRYSEAL)
  maps = [
    [201, 68, 19, 6],   #天空城祭坛
    [241, 26, 13, 2],   #茉克岛
    [242, 35, 14, 2],   #厄季斯岛
    [247, 24, 13, 2],   #灵诺森岛
    [243, 14, 13, 2],   #绯焰岛
    [244, 41, 16, 2],   #虹兰岛
    [264, 13, 44, 2],   #落英岛
    [269, 15, 12, 2],   #规盈岛
    [250, 34, 32, 2],   #狱怜岛
    [142, 47, 45, 2],   #幻谕岛
    [411, 26, 28, 2],   #翼霄岛
    [408, 28, 31, 2],   #影宿岛
    [409, 21, 16, 2]    #磷火岛
  ]
  choices = []
  for i in 0..$game_variables[62]
    break if !maps[i]
    name = pbGetMessage(MessageTypes::MapNames, maps[i][0])
    choices.push(name)
  end
  pbMessage(_INTL("好的，那么你要前往何地？"))
  loop do
    c = pbMessage(_INTL("请选择你要前往的岛"), choices, -1)
    if c == -1
      pbMessage("好的，要前往居民区随时来找我。")
      return
    end
    map_id = maps[c][0]
    x      = maps[c][1]
    y      = maps[c][2]
    dir    = maps[c][3]
    if map_id != $game_map.map_id
      pbShadowWarp(map_id, x, y, dir)
      return
    end
    pbMessage(_INTL("已经在这里了。"))
  end
end

def pbShadowWarp(map_id,x,y,dir)
  pbEraseEscapePoint
  viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  viewport.z = 99999
  viewport.color = Color.new(0,0,0,0)
  16.times do
    viewport.color.alpha += 16
    Graphics.update
  end
  $game_temp.player_new_map_id = map_id
  $game_temp.player_new_x = x
  $game_temp.player_new_y = y
  $game_temp.player_new_direction = dir
  $scene.transfer_player
  16.times do
    viewport.color.alpha -= 16
    Graphics.update
  end
  $game_map.refresh
end
