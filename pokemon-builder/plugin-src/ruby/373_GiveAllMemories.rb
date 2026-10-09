def pbGiveAllMemories
  memories = [
    :FIREMEMORY, :WATERMEMORY, :ELECTRICMEMORY, :GRASSMEMORY,
    :ICEMEMORY, :FIGHTINGMEMORY, :POISONMEMORY, :GROUNDMEMORY,
    :FLYINGMEMORY, :PSYCHICMEMORY, :BUGMEMORY, :ROCKMEMORY,
    :GHOSTMEMORY, :DRAGONMEMORY, :DARKMEMORY, :STEELMEMORY,
    :FAIRYMEMORY
  ]
  
  memories.each { |item| $PokemonBag.pbStoreItem(item, 1) }
  
  pbMessage("\\me[Item get]获得了存储碟套装！")
  pbMessage("把存储碟放在了背包里面！。")
end
