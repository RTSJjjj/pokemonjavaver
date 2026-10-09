def dumpDexData(region = 0, regionName = "地区图鉴")
  dexlist = []
  speciesData = pbLoadSpeciesData
  regionalSpecies = pbAllRegionalSpecies(region)
  for i in 1...regionalSpecies.length
    nationalSpecies = regionalSpecies[i]
    dexlist.push([nationalSpecies,PBSpecies.getName(nationalSpecies)])
  end
  dexData = ""
  dexlist.each_with_index do |dex, index|
    i = (index + 1).to_s
    dexData += i + " "*(12-i.length) + dex[0].to_s + " "*(12-dex[0].to_s.length) + dex[1] + "\n"
  end
  dexFile = File.new(regionName + ".txt", "w+")
  dexFile.syswrite(dexData)
  dexFile.close
end

def toggle_liefeng_switches
 return if  ![60, 207, 371,209,210].include?($game_map.map_id)
  x = $game_player.x
  y = $game_player.y
  $game_map.events.each_value do |event|
    next if !event.name.include?("float_plate")
    ret = (event.x==x && event.y==y) ? true : false
    pbSetSelfSwitch(event.id, "A", ret)
  end
end