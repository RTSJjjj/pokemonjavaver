def resurrection2
  fossils = [
      [:FOSSILIZEDBIRD, :FOSSILIZEDDRAKE, :DRACOZOLT],   # 雷鸟龙
      [:FOSSILIZEDBIRD, :FOSSILIZEDDINO,  :ARCTOZOLT],   # 雷鸟海兽
      [:FOSSILIZEDFISH, :FOSSILIZEDDRAKE, :DRACOVISH],   # 鳃鱼龙
      [:FOSSILIZEDFISH, :FOSSILIZEDDINO,  :ARCTOVISH]    # 鳃鱼海兽
  ]
  items = []
  species = []
  choices = []
  fossils.each do |fossil|
    if $PokemonBag.pbHasItem?(fossil[0]) && $PokemonBag.pbHasItem?(fossil[1])
      items.push([fossil[0], fossil[1]])
      species.push(fossil[2])
      choices.push(PBSpecies.getName(fossil[2]))
    end
  end
  if choices.length == 0
    pbMessage("你还没有任何可以拼接复活的化石啊，\n如果有的话，我可以帮你拼接并复活\n其中的宝可梦。")
    return
  end
  pbMessage("你有可以拼接复活的化石啊。")
  i = pbMessage(_INTL("需要我帮你拼接复活哪个化石中的\n宝可梦呢？"), choices, -1)
  if i == -1
    pbMessage("如果你想要拼接复活化石中的\n宝可梦，随时都可以来找我。")
    return
  end
  pbMessage("好的，请稍等。\\wtnp[20]")
  pbMessage("机器正在运行中\\wtnp[10]... \\wtnp[10]... \\wtnp[10]...\n\\wtnp[10]... \\wtnp[10]... \\wtnp[10]... \\wtnp[10]...")
  pbMessage("化石复活完成！")
  $PokemonBag.pbDeleteItem(items[i][0])
  $PokemonBag.pbDeleteItem(items[i][1])
  pkmn = pbGenPkmn(species[i], 1)
  if $game_variables[25] == 0
    pkmn.iv=[31, 31, 31, 31, 31, 31]
  elsif $game_variables[25] == 3 && $game_switches[99] == true
    pkmn.iv=[0, 0, 0, 0, 0, 0]
  end
  pkmn.calcStats
  pbAddPokemon(pkmn)
end