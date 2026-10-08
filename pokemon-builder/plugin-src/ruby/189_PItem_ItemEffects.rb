#===============================================================================
# UseText handlers
#===============================================================================
ItemHandlers::UseText.add(:BICYCLE,proc { |item|
  next ($PokemonGlobal.bicycle) ? _INTL("步行") : _INTL("Use")
})

ItemHandlers::UseText.copy(:BICYCLE,:MACHBIKE,:ACROBIKE)

#===============================================================================
# UseFromBag handlers
# Return values: 0 = not used
#                1 = used, item not consumed
#                2 = close the Bag to use, item not consumed
#                3 = used, item consumed
#                4 = close the Bag to use, item consumed
# If there is no UseFromBag handler for an item being used from the Bag (not on
# a Pokémon and not a TM/HM), calls the UseInField handler for it instead.
#===============================================================================

ItemHandlers::UseFromBag.add(:HONEY,proc { |item|
  next 4
})

ItemHandlers::UseFromBag.add(:ESCAPEROPE,proc { |item|
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next 0
  end
  if ($PokemonGlobal.escapePoint rescue false) && $PokemonGlobal.escapePoint.length>0
    next 4   # End screen and consume item
  end
  pbMessage(_INTL("这里不能使用。"))
  next 0
})

ItemHandlers::UseFromBag.add(:INFINITEROPE,proc { |item|
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next 0
  end
  if ($PokemonGlobal.escapePoint rescue false) && $PokemonGlobal.escapePoint.length>0
    next 2   # End screen and consume item
  end
  pbMessage(_INTL("这里不能使用。"))
  next 0
})

ItemHandlers::UseFromBag.add(:BICYCLE,proc { |item|
  next (pbBikeCheck) ? 2 : 0
})

ItemHandlers::UseFromBag.copy(:BICYCLE,:MACHBIKE,:ACROBIKE)

ItemHandlers::UseFromBag.add(:SUPERROD,proc { |item|
  terrain = pbFacingTerrainTag
  notCliff = $game_map.passable?($game_player.x,$game_player.y,$game_player.direction,$game_player)
  if (PBTerrain.isWater?(terrain) && !$PokemonGlobal.surfing && notCliff) ||
     (PBTerrain.isWater?(terrain) && $PokemonGlobal.surfing)
    next 2
  end
  pbMessage(_INTL("这里不能使用。"))
  next 0
})
ItemHandlers::UseFromBag.add(:ITEMFINDER,proc { |item|
  next 2
})

ItemHandlers::UseFromBag.copy(:ITEMFINDER,:DOWSINGMCHN,:DOWSINGMACHINE)

#===============================================================================
# ConfirmUseInField handlers
# Return values: true/false
# Called when an item is used from the Ready Menu.
# If an item does not have this handler, it is treated as returning true.
#===============================================================================

ItemHandlers::ConfirmUseInField.add(:ESCAPEROPE,proc { |item|
  escape = ($PokemonGlobal.escapePoint rescue nil)
  if !escape || escape==[]
    pbMessage(_INTL("这里不能使用。"))
    next false
  end
  if [60, 226, 207, 321, 322, 209, 210].include?($game_map.map_id)
    pbMessage(_INTL("这里不能使用。"))
    next false
  end
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next false
  end
  mapname = pbGetMapNameFromId(escape[0])
  next pbConfirmMessage(_INTL("想从这里出去回到{1}吗？",mapname))
})

ItemHandlers::ConfirmUseInField.add(:INFINITEROPE,proc { |item|
  escape = ($PokemonGlobal.escapePoint rescue nil)
  if !escape || escape==[]
    pbMessage(_INTL("这里不能使用。"))
    next false
  end
    if [60, 226, 207, 321, 322, 209, 210].include?($game_map.map_id)
    pbMessage(_INTL("这里不能使用。"))
    next false
  end
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next false
  end
  mapname = pbGetMapNameFromId(escape[0])
  next pbConfirmMessage(_INTL("想从这里出去回到{1}吗？",mapname))
})

#===============================================================================
# UseInField handlers
# Return values: 0 = not used
#                1 = used, item not consumed
#                3 = used, item consumed
# Called if an item is used from the Bag (not on a Pokémon and not a TM/HM) and
# there is no UseFromBag handler above.
# If an item has this handler, it can be registered to the Ready Menu.
#===============================================================================

def pbRepel(item,steps)
  if $PokemonGlobal.infRepel
    pbMessage(_INTL("无限喷雾的效果仍然存在。"))
    return 0
  end
  if $PokemonGlobal.repel>0
    pbMessage(_INTL("但喷雾剂仍然有效。"))
    return 0
  end
  pbUseItemMessage(item)
  $PokemonGlobal.repel = steps
  return 3
end

ItemHandlers::UseInField.add(:INFINITEREPEL,proc { |item|
  $PokemonGlobal.repel = 0
  infinite = $PokemonGlobal.infRepel
  $PokemonGlobal.infRepel = !infinite
  if infinite
    pbMessage(_INTL("已关闭无限喷雾。"))
  else
    pbMessage(_INTL("已开启无限喷雾。"))
  end
  next 1
})

ItemHandlers::UseInField.add(:REPEL,proc { |item|
  next pbRepel(item,100)
})

ItemHandlers::UseInField.add(:SUPERREPEL,proc { |item|
  next pbRepel(item,200)
})

ItemHandlers::UseInField.add(:MAXREPEL,proc { |item|
  next pbRepel(item,250)
})

Events.onStepTaken += proc {
  if $PokemonGlobal.repel>0
    if !PBTerrain.isIce?($game_player.terrain_tag)   # Shouldn't count down if on ice
      $PokemonGlobal.repel -= 1
      if $PokemonGlobal.repel<=0
        if $PokemonBag.pbHasItem?(:REPEL) ||
           $PokemonBag.pbHasItem?(:SUPERREPEL) ||
           $PokemonBag.pbHasItem?(:MAXREPEL)
          if pbConfirmMessage(_INTL("使用的喷雾剂失去效果了！\n想再用一个吗？"))
            ret = 0
            pbFadeOutIn {
              scene = PokemonBag_Scene.new
              screen = PokemonBagScreen.new(scene,$PokemonBag)
              ret = screen.pbChooseItemScreen(Proc.new { |item|
                isConst?(item,PBItems,:REPEL) ||
                isConst?(item,PBItems,:SUPERREPEL) ||
                isConst?(item,PBItems,:MAXREPEL)
              })
            }
            pbUseItem($PokemonBag,ret) if ret>0
          end
        else
          pbMessage(_INTL("使用的喷雾剂失去效果了！"))
        end
      end
    end
  end
}

ItemHandlers::UseInField.add(:BLACKFLUTE,proc { |item|
  pbUseItemMessage(item)
  pbMessage(_INTL("野生的宝可梦将被驱散。"))
  $PokemonMap.blackFluteUsed = true
  $PokemonMap.whiteFluteUsed = false
  next 1
})

ItemHandlers::UseInField.add(:WHITEFLUTE,proc { |item|
  pbUseItemMessage(item)
  pbMessage(_INTL("野生的宝可梦将被吸引。"))
  $PokemonMap.blackFluteUsed = false
  $PokemonMap.whiteFluteUsed = true
  next 1
})

ItemHandlers::UseInField.add(:HONEY,proc { |item|
  pbUseItemMessage(item)
  pbSweetScent
  next 3
})

ItemHandlers::UseInField.add(:ESCAPEROPE,proc { |item|
  escape = ($PokemonGlobal.escapePoint rescue nil)
  if !escape || escape==[]
    pbMessage(_INTL("这里不能使用。"))
    next 0
  end
  if [60, 226, 207, 321, 322, 209, 210].include?($game_map.map_id)
    pbMessage(_INTL("这里不能使用。"))
    next 0
  end
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next 0
  end
  pbUseItemMessage(item)
  pbFadeOutIn {
    $game_temp.player_new_map_id    = escape[0]
    $game_temp.player_new_x         = escape[1]
    $game_temp.player_new_y         = escape[2]
    $game_temp.player_new_direction = escape[3]
    pbCancelVehicles
    $scene.transfer_player
    $game_map.autoplay
    $game_map.refresh
  }
  pbEraseEscapePoint
  next 3
})

ItemHandlers::UseInField.add(:INFINITEROPE,proc { |item|
  escape = ($PokemonGlobal.escapePoint rescue nil)
  if !escape || escape==[]
    pbMessage(_INTL("这里不能使用。"))
    next 0
  end
  if [60, 226, 207, 321, 322, 209, 210].include?($game_map.map_id)
    pbMessage(_INTL("这里不能使用。"))
    next 0
  end
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next 0
  end
  pbUseItemMessage(item)
  pbFadeOutIn {
    $game_temp.player_new_map_id    = escape[0]
    $game_temp.player_new_x         = escape[1]
    $game_temp.player_new_y         = escape[2]
    $game_temp.player_new_direction = escape[3]
    pbCancelVehicles
    $scene.transfer_player
    $game_map.autoplay
    $game_map.refresh
  }
  pbEraseEscapePoint
  next 1
})

ItemHandlers::UseInField.add(:SACREDASH,proc { |item|
  if $Trainer.pokemonCount==0
    pbMessage(_INTL("没有宝可梦。"))
    next 0
  end
  canrevive = false
  for i in $Trainer.pokemonParty
    next if !i.fainted?
    canrevive = true; break
  end
  if !canrevive
    pbMessage(_INTL("这没有任何效果…"))
    next 0
  end
  revived = 0
  pbFadeOutIn {
    scene = PokemonParty_Scene.new
    screen = PokemonPartyScreen.new(scene,$Trainer.party)
    screen.pbStartScene(_INTL("使用道具……"),false)
    for i in 0...$Trainer.party.length
      if $Trainer.party[i].fainted?
        revived += 1
        $Trainer.party[i].heal
        screen.pbRefreshSingle(i)
        screen.pbDisplay(_INTL("{1}的HP回复了。",$Trainer.party[i].name))
      end
    end
    if revived==0
      screen.pbDisplay(_INTL("这没有任何效果…"))
    end
    screen.pbEndScene
  }
  next (revived==0) ? 0 : 3
})

ItemHandlers::UseInField.add(:BICYCLE,proc { |item|
  if pbBikeCheck
    if $PokemonGlobal.bicycle
      pbDismountBike
    else
      pbMountBike
    end
    next 1
  end
  next 0
})

ItemHandlers::UseInField.add(:SUPERROD, proc { |item|
  terrain = pbFacingTerrainTag
  notCliff = $game_map.passable?($game_player.x, $game_player.y, $game_player.direction, $game_player)
  if !PBTerrain.isWater?(terrain) || (!notCliff && !$PokemonGlobal.surfing)
    pbMessage(_INTL("这里不能使用。"))
    next 0
  end
  # 从三种钓竿分布中随机选一个
  rodTypes = [EncounterTypes::OldRod, EncounterTypes::GoodRod, EncounterTypes::SuperRod]
  chosenRod = rodTypes.sample
  encounter = $PokemonEncounters.hasEncounter?(chosenRod)
  if pbFishing(encounter, 3)
    pbEncounter(chosenRod)
  end
  next 1
})

ItemHandlers::UseInField.add(:ITEMFINDER,proc { |item|
  event = pbClosestHiddenItem
  if !event
    pbMessage(_INTL("... \\wt[10]... \\wt[10]... \\wt[10]...\\wt[10]Nope! There's no response."))
  else
    offsetX = event.x-$game_player.x
    offsetY = event.y-$game_player.y
    if offsetX==0 && offsetY==0   # Standing on the item, spin around
      4.times do
        pbWait(Graphics.frame_rate*2/10)
        $game_player.turn_right_90
      end
      pbWait(Graphics.frame_rate*3/10)
      pbMessage(_INTL("{1}指向的是……\n脚下！",PBItems.getName(item)))
    else   # Item is nearby, face towards it
      direction = $game_player.direction
      if offsetX.abs>offsetY.abs
        direction = (offsetX<0) ? 4 : 6
      else
        direction = (offsetY<0) ? 8 : 2
      end
      case direction
      when 2; $game_player.turn_down
      when 4; $game_player.turn_left
      when 6; $game_player.turn_right
      when 8; $game_player.turn_up
      end
      pbWait(Graphics.frame_rate*3/10)
      pbMessage(_INTL("Huh? The {1}'s responding!\1",PBItems.getName(item)))
      pbMessage(_INTL("有东西埋在附近！"))
    end
  end
  next 1
})

ItemHandlers::UseInField.copy(:ITEMFINDER,:DOWSINGMCHN,:DOWSINGMACHINE)

ItemHandlers::UseInField.add(:TOWNMAP,proc { |item|
  pbShowMap(-1,false)
  next 1
})

ItemHandlers::UseInField.add(:COINCASE,proc { |item|
  pbMessage(_INTL("代币:{1}",$PokemonGlobal.coins.to_s_formatted))
  next 1
})

ItemHandlers::UseInField.add(:EXPALL,proc { |item|
  $PokemonBag.pbChangeItem(:EXPALL,:EXPALLOFF)
  pbMessage(_INTL("经验共享已关闭。"))
  next 1
})

ItemHandlers::UseInField.add(:EXPALLOFF,proc { |item|
  $PokemonBag.pbChangeItem(:EXPALLOFF,:EXPALL)
  pbMessage(_INTL("经验共享已打开。"))
  next 1
})

#===============================================================================
# UseOnPokemon handlers
#===============================================================================

# Applies to all items defined as an evolution stone.
# No need to add more code for new ones.
ItemHandlers::UseOnPokemon.addIf(proc { |item| pbIsEvolutionStone?(item)},
  proc { |item,pkmn,scene|
    if pkmn.shadowPokemon?
      scene.pbDisplay(_INTL("这没有任何效果…"))
      next false
    end
    newspecies = pbCheckEvolution(pkmn,item)
    if newspecies<=0
      scene.pbDisplay(_INTL("这没有任何效果…"))
      next false
    else
      pbFadeOutInWithMusic {
        evo = PokemonEvolutionScene.new
        evo.pbStartScreen(pkmn,newspecies)
        evo.pbEvolution(false)
        evo.pbEndScreen
        if scene.is_a?(PokemonPartyScreen)
          scene.pbRefreshAnnotations(proc { |p| pbCheckEvolution(p,item)>0 })
          scene.pbRefresh
        end
      }
      next true
    end
  }
)

ItemHandlers::UseOnPokemon.add(:POTION,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,20,scene)
})

ItemHandlers::UseOnPokemon.copy(:POTION,:BERRYJUICE)
ItemHandlers::UseOnPokemon.copy(:POTION,:RAGECANDYBAR) if !NEWEST_BATTLE_MECHANICS

ItemHandlers::UseOnPokemon.add(:SWEETHEART,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,80,scene)
})
ItemHandlers::UseOnPokemon.add(:SUPERPOTION,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,60,scene)
})

ItemHandlers::UseOnPokemon.add(:HYPERPOTION,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,120,scene)
})

ItemHandlers::UseOnPokemon.add(:MAXPOTION,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,pkmn.totalhp-pkmn.hp,scene)
})

ItemHandlers::UseOnPokemon.add(:FRESHWATER,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,50,scene)
})

ItemHandlers::UseOnPokemon.add(:SODAPOP,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,60,scene)
})

ItemHandlers::UseOnPokemon.add(:LEMONADE,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,80,scene)
})

ItemHandlers::UseOnPokemon.add(:MOOMOOMILK,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,100,scene)
})

ItemHandlers::UseOnPokemon.add(:ORANBERRY,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,10,scene)
})

ItemHandlers::UseOnPokemon.add(:SITRUSBERRY,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,pkmn.totalhp/4,scene)
})

ItemHandlers::UseOnPokemon.add(:CIDER,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,50,scene)
})

ItemHandlers::UseOnPokemon.add(:MIXEDBEVERAGES,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,100,scene)
})

ItemHandlers::UseOnPokemon.add(:WWINE,proc { |item,pkmn,scene|
  next pbHPItem(pkmn,150,scene)
})

ItemHandlers::UseOnPokemon.add(:AWAKENING,proc { |item,pkmn,scene|
  if pkmn.fainted? || pkmn.status!=PBStatuses::SLEEP
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.healStatus
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}醒来了！",pkmn.name))
  next true
})

ItemHandlers::UseOnPokemon.copy(:AWAKENING,:CHESTOBERRY,:BLUEFLUTE,:POKEFLUTE)

ItemHandlers::UseOnPokemon.add(:ANTIDOTE,proc { |item,pkmn,scene|
  if pkmn.fainted? || pkmn.status!=PBStatuses::POISON
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.healStatus
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}的毒被消去了！",pkmn.name))
  next true
})

ItemHandlers::UseOnPokemon.copy(:ANTIDOTE,:PECHABERRY)

ItemHandlers::UseOnPokemon.add(:BURNHEAL,proc { |item,pkmn,scene|
  if pkmn.fainted? || pkmn.status!=PBStatuses::BURN
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.healStatus
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}的灼伤被治愈了！",pkmn.name))
  next true
})

ItemHandlers::UseOnPokemon.copy(:BURNHEAL,:RAWSTBERRY)

ItemHandlers::UseOnPokemon.add(:PARLYZHEAL,proc { |item,pkmn,scene|
  if pkmn.fainted? || pkmn.status!=PBStatuses::PARALYSIS
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.healStatus
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}的麻痹被解除了！",pkmn.name))
  next true
})

ItemHandlers::UseOnPokemon.copy(:PARLYZHEAL,:PARALYZEHEAL,:CHERIBERRY)

ItemHandlers::UseOnPokemon.add(:ICEHEAL,proc { |item,pkmn,scene|
  if pkmn.fainted? || pkmn.status!=PBStatuses::FROZEN
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.healStatus
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}不再被冰冻了",pkmn.name))
  next true
})

ItemHandlers::UseOnPokemon.copy(:ICEHEAL,:ASPEARBERRY)

ItemHandlers::UseOnPokemon.add(:FULLHEAL,proc { |item,pkmn,scene|
  if pkmn.fainted? || pkmn.status==PBStatuses::NONE
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.healStatus
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}恢复健康了。",pkmn.name))
  next true
})

ItemHandlers::UseOnPokemon.add(:XIANGSHAWLPILL,proc { |item,pkmn,scene|
  if pkmn.fainted? || pkmn.status==PBStatuses::NONE
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.healStatus
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}恢复健康了。",pkmn.name))
  next true
})


ItemHandlers::UseOnPokemon.copy(:FULLHEAL,
   :LAVACOOKIE,:OLDGATEAU,:CASTELIACONE,:LUMIOSEGALETTE,:SHALOURSABLE,
   :BIGMALASADA,:LUMBERRY,:XIANGSHAWLPILL)
ItemHandlers::UseOnPokemon.copy(:FULLHEAL,:RAGECANDYBARY,:XIANGSHAWLPILL) if NEWEST_BATTLE_MECHANICS

ItemHandlers::UseOnPokemon.add(:FULLRESTORE,proc { |item,pkmn,scene|
  if pkmn.fainted? || (pkmn.hp==pkmn.totalhp && pkmn.status==PBStatuses::NONE)
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  hpgain = pbItemRestoreHP(pkmn,pkmn.totalhp-pkmn.hp)
  pkmn.healStatus
  scene.pbRefresh
  if hpgain>0
    scene.pbDisplay(_INTL("{1}的HP恢复了{2}点。",pkmn.name,hpgain))
  else
    scene.pbDisplay(_INTL("{1}恢复健康了。",pkmn.name))
  end
  next true
})

ItemHandlers::UseOnPokemon.add(:REVIVE,proc { |item,pkmn,scene|
  if !pkmn.fainted?
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.hp = (pkmn.totalhp/2).floor
  pkmn.hp = 1 if pkmn.hp<=0
  pkmn.healStatus
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}的HP回复了。",pkmn.name))
  next true
})

ItemHandlers::UseOnPokemon.add(:MAXREVIVE,proc { |item,pkmn,scene|
  if !pkmn.fainted?
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.healHP
  pkmn.healStatus
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}的HP回复了。",pkmn.name))
  next true
})

ItemHandlers::UseOnPokemon.add(:EVEBURGER,proc { |item,pkmn,scene|
  if !pkmn.fainted?
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.healHP
  pkmn.healStatus
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}的HP回复了。",pkmn.name))
  next true
})

ItemHandlers::UseOnPokemon.add(:ENERGYPOWDER,proc { |item,pkmn,scene|
  if pbHPItem(pkmn,60,scene)
    pkmn.changeHappiness("powder")
    next true
  end
  next false
})

ItemHandlers::UseOnPokemon.add(:ENERGYROOT,proc { |item,pkmn,scene|
  if pbHPItem(pkmn,200,scene)
    pkmn.changeHappiness("energyroot")
    next true
  end
  next false
})

ItemHandlers::UseOnPokemon.add(:HEALPOWDER,proc { |item,pkmn,scene|
  if pkmn.fainted? || pkmn.status==PBStatuses::NONE
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.healStatus
  pkmn.changeHappiness("powder")
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}恢复健康了。",pkmn.name))
  next true
})

ItemHandlers::UseOnPokemon.add(:REVIVALHERB,proc { |item,pkmn,scene|
  if !pkmn.fainted?
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  pkmn.healHP
  pkmn.healStatus
  pkmn.changeHappiness("revivalherb")
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}的HP回复了。",pkmn.name))
  next true
})

ItemHandlers::UseOnPokemon.add(:ETHER,proc { |item,pkmn,scene|
  move = scene.pbChooseMove(pkmn,_INTL("要回复哪个招式？"))
  next false if move<0
  if pbRestorePP(pkmn,move,10)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("PP恢复了。"))
  next true
})

ItemHandlers::UseOnPokemon.copy(:ETHER,:LEPPABERRY)

ItemHandlers::UseOnPokemon.add(:MAXETHER,proc { |item,pkmn,scene|
  move = scene.pbChooseMove(pkmn,_INTL("要回复哪个招式？"))
  next false if move<0
  if pbRestorePP(pkmn,move,pkmn.moves[move].totalpp-pkmn.moves[move].pp)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("PP恢复了。"))
  next true
})

ItemHandlers::UseOnPokemon.add(:ELIXIR,proc { |item,pkmn,scene|
  pprestored = 0
  for i in 0...pkmn.moves.length
    pprestored += pbRestorePP(pkmn,i,10)
  end
  if pprestored==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("PP恢复了。"))
  next true
})

ItemHandlers::UseOnPokemon.add(:MAXELIXIR,proc { |item,pkmn,scene|
  pprestored = 0
  for i in 0...pkmn.moves.length
    pprestored += pbRestorePP(pkmn,i,pkmn.moves[i].totalpp-pkmn.moves[i].pp)
  end
  if pprestored==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("PP恢复了。"))
  next true
})

ItemHandlers::UseOnPokemon.add(:PPUP,proc { |item,pkmn,scene|
  move = scene.pbChooseMove(pkmn,_INTL("增加哪招的PP？"))
  if move>=0
    if pkmn.moves[move].totalpp<=1 || pkmn.moves[move].ppup>=3
      scene.pbDisplay(_INTL("这没有任何效果…"))
      next false
    end
    pkmn.moves[move].ppup += 1
    movename = PBMoves.getName(pkmn.moves[move].id)
    scene.pbDisplay(_INTL("{1}的PP增加了。",movename))
    next true
  end
  next false
})

ItemHandlers::UseOnPokemon.add(:PPMAX,proc { |item,pkmn,scene|
  move = scene.pbChooseMove(pkmn,_INTL("增加哪招的PP？"))
  if move>=0
    if pkmn.moves[move].totalpp<=1 || pkmn.moves[move].ppup>=3
      scene.pbDisplay(_INTL("这没有任何效果…"))
      next false
    end
    pkmn.moves[move].ppup = 3
    movename = PBMoves.getName(pkmn.moves[move].id)
    scene.pbDisplay(_INTL("{1}的PP增加了。",movename))
    next true
  end
  next false
})

ItemHandlers::UseOnPokemon.add(:HPUP,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::HP)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}的HP增加了。",pkmn.name))
  pkmn.changeHappiness("vitamin")
  next true
})

ItemHandlers::UseOnPokemon.add(:PROTEIN,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::ATTACK)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("{1}的攻击增加了。",pkmn.name))
  pkmn.changeHappiness("vitamin")
  next true
})

ItemHandlers::UseOnPokemon.add(:IRON,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::DEFENSE)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("{1}的防御增加了。",pkmn.name))
  pkmn.changeHappiness("vitamin")
  next true
})

ItemHandlers::UseOnPokemon.add(:CALCIUM,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::SPATK)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("{1}的特攻增加了。",pkmn.name))
  pkmn.changeHappiness("vitamin")
  next true
})

ItemHandlers::UseOnPokemon.add(:ZINC,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::SPDEF)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("{1}的特防增加了。",pkmn.name))
  pkmn.changeHappiness("vitamin")
  next true
})

ItemHandlers::UseOnPokemon.add(:CARBOS,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::SPEED)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("{1}的速度增加了。",pkmn.name))
  pkmn.changeHappiness("vitamin")
  next true
})

ItemHandlers::UseOnPokemon.add(:HEALTHWING,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::HP,1,false)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}的HP增加了。",pkmn.name))
  pkmn.changeHappiness("wing")
  next true
})

ItemHandlers::UseOnPokemon.add(:MUSCLEWING,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::ATTACK,1,false)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("{1}的攻击增加了。",pkmn.name))
  pkmn.changeHappiness("wing")
  next true
})

ItemHandlers::UseOnPokemon.add(:RESISTWING,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::DEFENSE,1,false)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("{1}的防御增加了。",pkmn.name))
  pkmn.changeHappiness("wing")
  next true
})

ItemHandlers::UseOnPokemon.add(:GENIUSWING,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::SPATK,1,false)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("{1}的特攻增加了。",pkmn.name))
  pkmn.changeHappiness("wing")
  next true
})

ItemHandlers::UseOnPokemon.add(:CLEVERWING,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::SPDEF,1,false)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("{1}的特防增加了。",pkmn.name))
  pkmn.changeHappiness("wing")
  next true
})

ItemHandlers::UseOnPokemon.add(:SWIFTWING,proc { |item,pkmn,scene|
  if pbRaiseEffortValues(pkmn,PBStats::SPEED,1,false)==0
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  scene.pbDisplay(_INTL("{1}的速度增加了。",pkmn.name))
  pkmn.changeHappiness("wing")
  next true
})

# 神奇糖果
ItemHandlers::UseOnPokemon.add(:RARECANDY,proc { |item,pkmn,scene|
  maxLevels = MAX_LEVEL
  lv_lock_on = $game_switches[199]
  legue_pass = $game_switches[12]
  index = -1
  # 如果超过200级则无法使用
  if pkmn.level>=maxLevels[index]
    scene.pbDisplay(_INTL("没有任何效果。"))
    next false
  end
  # 如果开启了等级限制
  if lv_lock_on
    if legue_pass
      for i in 8..15
        if !$Trainer.badges[i]
          index = i+1
          break
        end
      end
    else
      for i in 0..7
        if !$Trainer.badges[i]
          index = i
          break
        end
      end
    end
    # 如果当前等级超过上限
    if pkmn.level>=maxLevels[index]
      if index == -1
        scene.pbDisplay(_INTL("没有任何效果。"))
      else
        scene.pbDisplay(_INTL("直到解锁{1}级限制前无法使用。",maxLevels[index+1]))
      end
      next false
    end
  end
  qty = 1
  quantity = [maxLevels[-1]-pkmn.level, $PokemonBag.pbQuantity(item)].min
  if quantity > 1
    params = ChooseNumberParams.new
    params.setRange(1,quantity)
    params.setDefaultValue(1)
    params.setCancelValue(0)
    qty = pbMessageChooseNumber(_INTL("需要使用多少个{1}？\n(当前最大只能使用{2}个)",PBItems.getName(item),quantity),params)
  end
  next false if qty == 0
  pbChangeLevel(pkmn,pkmn.level+qty,scene)
  $PokemonBag.pbDeleteItem(item, qty-1)
  scene.pbHardRefresh
  next true
})

ItemHandlers::UseOnPokemon.add(:HOPOBERRY,proc { |item,pkmn,scene|
  next pbEmptyAllEV(pkmn, scene, _INTL("{1}的所有努力值都清空了！",pkmn.name))
})

ItemHandlers::UseOnPokemon.add(:POMEGBERRY,proc { |item,pkmn,scene|
  next pbRaiseHappinessAndLowerEV(pkmn,scene,PBStats::HP,[
     _INTL("{1}十分喜欢你！\n基础HP降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n基础HP不能再降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n但是基础HP降低了。",pkmn.name)
  ])
})

ItemHandlers::UseOnPokemon.add(:KELPSYBERRY,proc { |item,pkmn,scene|
  next pbRaiseHappinessAndLowerEV(pkmn,scene,PBStats::ATTACK,[
     _INTL("{1}十分喜欢你！\n攻击降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n攻击不能再降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n但是攻击降低了。",pkmn.name)
  ])
})

ItemHandlers::UseOnPokemon.add(:QUALOTBERRY,proc { |item,pkmn,scene|
  next pbRaiseHappinessAndLowerEV(pkmn,scene,PBStats::DEFENSE,[
     _INTL("{1}十分喜欢你！\n防御降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n防御不能再降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n但是防御降低了。",pkmn.name)
  ])
})

ItemHandlers::UseOnPokemon.add(:HONDEWBERRY,proc { |item,pkmn,scene|
  next pbRaiseHappinessAndLowerEV(pkmn,scene,PBStats::SPATK,[
     _INTL("{1}十分喜欢你！\n特攻降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n特攻不能再降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n但是特攻降低了。",pkmn.name)
  ])
})

ItemHandlers::UseOnPokemon.add(:GREPABERRY,proc { |item,pkmn,scene|
  next pbRaiseHappinessAndLowerEV(pkmn,scene,PBStats::SPDEF,[
     _INTL("{1}十分喜欢你！\n特防降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n特防不能再降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n但是特防降低了。",pkmn.name)
  ])
})

ItemHandlers::UseOnPokemon.add(:TAMATOBERRY,proc { |item,pkmn,scene|
  next pbRaiseHappinessAndLowerEV(pkmn,scene,PBStats::SPEED,[
     _INTL("{1}十分喜欢你！\n速度降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n速度不能再降低了。",pkmn.name),
     _INTL("{1}更加喜欢你了。\n但是速度降低了。",pkmn.name)
  ])
})

ItemHandlers::UseOnPokemon.add(:GRACIDEA,proc { |item,pkmn,scene|
  if !pkmn.isSpecies?(:SHAYMIN) || pkmn.form!=0 ||
     pkmn.status==PBStatuses::FROZEN || PBDayNight.isNight?
    scene.pbDisplay(_INTL("无法生效。"))
    next false
  end
  if pkmn.fainted?
    scene.pbDisplay(_INTL("不能用在晕倒的宝可梦上。"))
    next false
  end
  pkmn.setForm(1) {
    scene.pbRefresh
    scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
  }
  next true
})


ItemHandlers::UseOnPokemon.add(:REVEALGLASS,proc { |item,pkmn,scene|
  if !pkmn.isSpecies?(:TORNADUS) &&
     !pkmn.isSpecies?(:THUNDURUS) &&
     !pkmn.isSpecies?(:LANDORUS) &&
     !pkmn.isSpecies?(:ENAMORUS)
    scene.pbDisplay(_INTL("无法生效。"))
    next false
  end
  if pkmn.fainted?
    scene.pbDisplay(_INTL("不能用在晕倒的宝可梦上。"))
    next false
  end
  newForm = (pkmn.form==0) ? 1 : 0
  pkmn.setForm(newForm) {
    scene.pbRefresh
    scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
  }
  next true
})

ItemHandlers::UseOnPokemon.add(:PRISONBOTTLE,proc { |item,pkmn,scene|
  if !pkmn.isSpecies?(:HOOPA)
    scene.pbDisplay(_INTL("无法生效。"))
    next false
  end
  if pkmn.fainted?
    scene.pbDisplay(_INTL("不能用在晕倒的宝可梦上。"))
  end
  newForm = (pkmn.form==0) ? 1 : 0
  pkmn.setForm(newForm) {
    scene.pbRefresh
    scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
  }
  next true
})

ItemHandlers::UseOnPokemon.add(:DNASPLICERS,proc { |item,pkmn,scene|
  if !pkmn.isSpecies?(:KYUREM)
    scene.pbDisplay(_INTL("无法生效。"))
    next false
  end
  if pkmn.fainted?
    scene.pbDisplay(_INTL("不能用在晕倒的宝可梦上。"))
    next false
  end
  # Fusing
  if pkmn.fused==nil
    chosen = scene.pbChoosePokemon(_INTL("与哪个宝可梦融合？"))
    next false if chosen<0
    poke2 = $Trainer.party[chosen]
    if pkmn==poke2
      scene.pbDisplay(_INTL("不能与自身融合。"))
      next false
    elsif poke2.egg?
      scene.pbDisplay(_INTL("不能与蛋融合。"))
      next false
    elsif poke2.fainted?
      scene.pbDisplay(_INTL("不能与晕倒的宝可梦融合。"))
      next false
    elsif !poke2.isSpecies?(:RESHIRAM) &&
          !poke2.isSpecies?(:ZEKROM)
      scene.pbDisplay(_INTL("不能与宝可梦融合。"))
      next false
    end
    newForm = 0
    newForm = 1 if poke2.isSpecies?(:RESHIRAM)
    newForm = 2 if poke2.isSpecies?(:ZEKROM)
    pkmn.setForm(newForm) {
      pkmn.fused = poke2
      pbRemovePokemonAt(chosen)
      scene.pbHardRefresh
      scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
    }
    next true
  end
  # Unfusing
  if $Trainer.party.length>=6
    scene.pbDisplay(_INTL("没有空间可供分离。"))
    next false
  end
  pkmn.setForm(0) {
    $Trainer.party[$Trainer.party.length] = pkmn.fused
    pkmn.fused = nil
    scene.pbHardRefresh
    scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
  }
  next true
})

ItemHandlers::UseOnPokemon.add(:NSOLARIZER,proc { |item,pkmn,scene|
  if !pkmn.isSpecies?(:NECROZMA) || pkmn.form == 2
    scene.pbDisplay(_INTL("无法生效。"))
    next false
  end
  if pkmn.fainted?
    scene.pbDisplay(_INTL("不能用在晕倒的宝可梦上。"))
    next false
  end
  # Fusing
  if pkmn.fused==nil
    chosen = scene.pbChoosePokemon(_INTL("与哪个宝可梦融合？"))
    next false if chosen<0
    poke2 = $Trainer.party[chosen]
    if pkmn==poke2
      scene.pbDisplay(_INTL("不能与自身融合。"))
      next false
    elsif poke2.egg?
      scene.pbDisplay(_INTL("不能与蛋融合。"))
      next false
    elsif poke2.fainted?
      scene.pbDisplay(_INTL("不能与晕倒的宝可梦融合。"))
      next false
    elsif !poke2.isSpecies?(:SOLGALEO)
      scene.pbDisplay(_INTL("不能与宝可梦融合。"))
      next false
    end
    pkmn.setForm(1) {
      pkmn.fused = poke2
      pbRemovePokemonAt(chosen)
      scene.pbHardRefresh
      scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
    }
    next true
  end
  # Unfusing
  if $Trainer.party.length>=6
    scene.pbDisplay(_INTL("没有空间可供分离。"))
    next false
  end
  pkmn.setForm(0) {
    $Trainer.party[$Trainer.party.length] = pkmn.fused
    pkmn.fused = nil
    scene.pbHardRefresh
    scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
  }
  next true
})

ItemHandlers::UseOnPokemon.add(:NLUNARIZER,proc { |item,pkmn,scene|
  if !pkmn.isSpecies?(:NECROZMA) || pkmn.form == 1
    scene.pbDisplay(_INTL("无法生效。"))
    next false
  end
  if pkmn.fainted?
    scene.pbDisplay(_INTL("不能用在晕倒的宝可梦上。"))
    next false
  end
  # Fusing
  if pkmn.fused==nil
    chosen = scene.pbChoosePokemon(_INTL("与哪个宝可梦融合？"))
    next false if chosen<0
    poke2 = $Trainer.party[chosen]
    if pkmn==poke2
      scene.pbDisplay(_INTL("不能与自身融合。"))
      next false
    elsif poke2.egg?
      scene.pbDisplay(_INTL("不能与蛋融合。"))
      next false
    elsif poke2.fainted?
      scene.pbDisplay(_INTL("不能与晕倒的宝可梦融合。"))
      next false
    elsif !poke2.isSpecies?(:LUNALA)
      scene.pbDisplay(_INTL("不能与宝可梦融合。"))
      next false
    end
    pkmn.setForm(2) {
      pkmn.fused = poke2
      pbRemovePokemonAt(chosen)
      scene.pbHardRefresh
      scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
    }
    next true
  end
  # Unfusing
  if $Trainer.party.length>=6
    scene.pbDisplay(_INTL("没有空间可供分离。"))
    next false
  end
  pkmn.setForm(0) {
    $Trainer.party[$Trainer.party.length] = pkmn.fused
    pkmn.fused = nil
    scene.pbHardRefresh
    scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
  }
  next true
})

ItemHandlers::UseOnPokemon.add(:ABILITYCAPSULE,proc { |item,pkmn,scene|
  abils = pkmn.getAbilityList
  abil1 = 0; abil2 = 0
  for i in abils
    abil1 = i[0] if i[1]==0
    abil2 = i[0] if i[1]==1
  end
  if abil1<=0 || abil2<=0 || pkmn.hasHiddenAbility? || pkmn.isSpecies?(:ZYGARDE)
    scene.pbDisplay(_INTL("这没有任何效果…"))
    next false
  end
  newabil = (pkmn.abilityIndex+1)%2
  newabilname = PBAbilities.getName((newabil==0) ? abil1 : abil2)
  if scene.pbConfirm(_INTL("是否要将{1}的特性更改为{2}？",
     pkmn.name,newabilname))
    pkmn.setAbility(newabil)
    scene.pbRefresh
    scene.pbDisplay(_INTL("{1}的特性更改为{2}！",pkmn.name,
       PBAbilities.getName(pkmn.ability)))
    next true
  end
  next false
})


ItemHandlers::UseOnPokemon.add(:EXPCANDYXS,proc { |item,pkmn,scene|
  maxLevels =  MAX_LEVEL
  lv_lock_on = $game_switches[199]
  legue_pass = $game_switches[12]
  index = -1
  # 如果超过200级则无法使用
  if    pkmn.level>=maxLevels[index]
    scene.pbDisplay(_INTL("没有任何效果。"))
    next false
  end
  experience=100   if isConst?(item,PBItems,:EXPCANDYXS)
  experience=800   if isConst?(item,PBItems,:EXPCANDYS)
  experience=3000  if isConst?(item,PBItems,:EXPCANDYM)
  experience=10000 if isConst?(item,PBItems,:EXPCANDYL)
  experience=30000 if isConst?(item,PBItems,:EXPCANDYXL)
  experience_gain = PBExperience.pbGetStartExperience(maxLevels[index],pkmn.growthrate)-pkmn.exp
  # 如果开启了等级限制
  if lv_lock_on
    if legue_pass
      for i in 8..15
        if !$Trainer.badges[i]
          index = i+1
          break
        end
      end
    else
      for i in 0..7
        if !$Trainer.badges[i]
          index = i
          break
        end
      end
    end
    # 如果当前等级超过上限
    if pkmn.level>=maxLevels[index]
      ret = pbCheckEvolution(pkmn,0)
      if ret<=0
        if index == -1
          scene.pbDisplay(_INTL("没有任何效果。"))
        else
          scene.pbDisplay(_INTL("直到解锁{1}级限制前无法使用。",maxLevels[index+1]))
        end
        next false
      end
    end
    experience_gain = PBExperience.pbGetStartExperience(maxLevels[index],pkmn.growthrate)-pkmn.exp
  end
  qty = 1
  quantity = [(experience_gain*1.0/experience).ceil, $PokemonBag.pbQuantity(item)].min
  if quantity > 1
    params = ChooseNumberParams.new
    params.setRange(1,quantity)
    params.setDefaultValue(1)
    params.setCancelValue(0)
    qty = pbMessageChooseNumber(_INTL("需要使用多少个{1}？\n(当前最大只能使用{2}个)",PBItems.getName(item),quantity),params)
    next false if qty == 0
  end
  experience_gain = qty * experience if experience_gain >= qty * experience
  newexp=PBExperience.pbAddExperience(pkmn.exp,experience_gain,pkmn.growthrate)
  newlevel=PBExperience.pbGetLevelFromExperience(newexp,pkmn.growthrate)
  curlevel=pkmn.level
  leveldif = newlevel - curlevel
  scene.pbDisplay(_INTL("你的宝可梦获得了{1}点经验值！",experience_gain))
  if newlevel==curlevel
    pkmn.exp=newexp
    pkmn.calcStats
    scene.pbRefresh
  else
    pbSEPlay("Pkmn level up")
    old_level = pkmn.level
    pbChangeLevel(pkmn, pkmn.level + leveldif, scene, false)
    moveList = pkmn.getMoveList
    moveList.each do |m|
      next if m[0] <= old_level || m[0] > pkmn.level
      pbLearnMove(pkmn, m[1], true) { scene.pbUpdate }
    end
    scene.pbHardRefresh
  end
  $PokemonBag.pbDeleteItem(item, qty-1)
  next true
})


ItemHandlers::UseOnPokemon.add(:ROTOMCATALOG,proc{|item,pkmn,scene|
  if (isConst?(pkmn.species,PBSpecies,:ROTOM))
    if pkmn.hp>0
      scene.pbDisplay(_INTL("目录中包含了 {1} 可以拥有的家电清单！",pkmn.name))
      cmd=0
      msg = _INTL("您想订购哪种家电？")
      cmd = scene.pbShowCommands(msg,[
  _INTL("灯泡"),
  _INTL("微波炉"),
  _INTL("洗衣机"),
  _INTL("冰箱"),
  _INTL("电风扇"),
  _INTL("割草机"),
        _INTL("取消")],cmd)
      if cmd>=0 && cmd<6
        scene.pbDisplay(_INTL("{1}变身了！",pkmn.name))
        scene.pbRefresh
        pkmn.form = cmd
        scene.pbRefresh
      else
        scene.pbDisplay(_INTL("没有订购任何家电"))
      end
      scene.pbRefresh
      next true
    else
      scene.pbDisplay(_INTL("不能用在晕倒的宝可梦上。"))
    end
  else
    scene.pbDisplay(_INTL("无法生效。"))
    next false
  end
})

ItemHandlers::UseOnPokemon.copy(:EXPCANDYXS,:EXPCANDYS,:EXPCANDYM,:EXPCANDYL,:EXPCANDYXL)

ItemHandlers::UseOnPokemon.add(:LONELYMINT,proc { |item,pkmn,scene|
   a = PBNatures::LONELY     if isConst?(item,PBItems,:LONELYMINT)
   a = PBNatures::ADAMANT    if isConst?(item,PBItems,:ADAMANTMINT)
   a = PBNatures::NAUGHTY    if isConst?(item,PBItems,:NAUGHTYMINT)
   a = PBNatures::BRAVE      if isConst?(item,PBItems,:BRAVEMINT)
   a = PBNatures::BOLD       if isConst?(item,PBItems,:BOLDMINT)
   a = PBNatures::IMPISH     if isConst?(item,PBItems,:IMPISHMINT)
   a = PBNatures::LAX        if isConst?(item,PBItems,:LAXMINT)
   a = PBNatures::RELAXED    if isConst?(item,PBItems,:RELAXEDMINT)
   a = PBNatures::MODEST     if isConst?(item,PBItems,:MODESTMINT)
   a = PBNatures::MILD       if isConst?(item,PBItems,:MILDMINT)
   a = PBNatures::RASH       if isConst?(item,PBItems,:RASHMINT)
   a = PBNatures::QUIET      if isConst?(item,PBItems,:QUIETMINT)
   a = PBNatures::CALM       if isConst?(item,PBItems,:CALMMINT)
   a = PBNatures::GENTLE     if isConst?(item,PBItems,:GENTLEMINT)
   a = PBNatures::CAREFUL    if isConst?(item,PBItems,:CAREFULMINT)
   a = PBNatures::SASSY      if isConst?(item,PBItems,:SASSYMINT)
   a = PBNatures::TIMID      if isConst?(item,PBItems,:TIMIDMINT)
   a = PBNatures::HASTY      if isConst?(item,PBItems,:HASTYMINT)
   a = PBNatures::JOLLY      if isConst?(item,PBItems,:JOLLYMINT)
   a = PBNatures::NAIVE      if isConst?(item,PBItems,:NAIVEMINT)
   a = PBNatures::SERIOUS    if isConst?(item,PBItems,:SERIOUSMINT)
 b = pkmn.nature
 b = 12 if pkmn.nature == 0 || pkmn.nature == 6 || pkmn.nature == 18 || pkmn.nature == 24
 if pkmn.natureOverride == a || b == a
   scene.pbDisplay(_INTL("这没有任何效果…"))
   next false
 else
   if scene.pbConfirm(_INTL("这可能会影响{1}性格，\n确定要使用它吗？",pkmn.name))
     scene.pbDisplay(_INTL("{1}的性格由于{2}而改变!",pkmn.name,PBItems.getName(item)))
     #pkmn.natureOverride = a
     pkmn.setNature(a)
     pkmn.calcStats
     next true
   end
 end
 next false
})

ItemHandlers::UseOnPokemon.copy(:LONELYMINT,:ADAMANTMINT,:NAUGHTYMINT,:BRAVEMINT,:BOLDMINT,:IMPISHMINT,:LAXMINT,:RELAXEDMINT,:MODESTMINT,:MILDMINT,:RASHMINT,:QUIETMINT,:CALMMINT,:GENTLEMINT,:CAREFULMINT,:SASSYMINT,:TIMIDMINT,:HASTYMINT,:JOLLYMINT,:NAIVEMINT,:SERIOUSMINT)


ItemHandlers::UseOnPokemon.add(:ABILITYPATCH, proc { |item, pkmn, scene|
  abils = pkmn.getAbilityList #此精灵的所有特性
  hiddenArr =[]
  if abils.length <= 1 || pkmn.isSpecies?(:ZYGARDE)
    scene.pbDisplay(_INTL("没有任何效果。"))
    next false
  end
  #a[1]表示精灵特性下标，a[0]是特性对象
  if pkmn.abilityIndex < 2  #当前为普特
    for a in abils
      if a[1] > 1
        newabil = [a[1], a[0]]
        newabilname = PBAbilities.getName(a[0])
        break
      end
    end
    if newabil == nil
      scene.pbDisplay(_INTL("没有任何效果。"))
      next false
    end
  else    #d当前为梦特
    abilArr = []  #存放普特对象的数组
    commands = []  #存放普特名称的数组
    for a in abils
      if pkmn.abilityIndex != a[1]  #如果不为当前特性
        abilArr.push([a[1], a[0]])  #追加到普特对象的数组中
        commands.push(PBAbilities.getName(a[0]))  #追加到普特名称的数组中
      end
    end
    if abilArr.length > 1 #普特对象数组长度大于1，让玩家选择
      cmd = pbMessage(sprintf("请选择要修改的特性。"), commands, -1)
      next false if cmd == -1
    else  #否则就一个普特
      cmd = 0
    end
    newabil = abilArr[cmd]
    newabilname = commands[cmd]
  end
  if scene.pbConfirm(_INTL("你想要将{1}的特性改变为\n{2}吗？", pkmn.name, newabilname))
    pkmn.setAbility(newabil[0])
    scene.pbRefresh
    scene.pbDisplay(_INTL("{1}的特性变为{2}了！", pkmn.name, newabilname))
    next true
  end
  next false
})

ItemHandlers::UseOnPokemon.add(:REINSOFUNITY,proc { |item,pkmn,scene|
  if !pkmn.isSpecies?(:CALYREX)
    scene.pbDisplay(_INTL("无法生效。"))
    next false
  end
  if pkmn.fainted?
    scene.pbDisplay(_INTL("不能用在晕倒的宝可梦上。"))
    next false
  end
  # Fusing
  if pkmn.fused==nil
    chosen = scene.pbChoosePokemon(_INTL("与哪个宝可梦融合？"))
    next false if chosen<0
    poke2 = $Trainer.party[chosen]
    if pkmn==poke2
      scene.pbDisplay(_INTL("不能与自身融合。"))
      next false
    elsif poke2.egg?
      scene.pbDisplay(_INTL("不能与蛋融合。"))
      next false
    elsif poke2.fainted?
      scene.pbDisplay(_INTL("不能与晕倒的宝可梦融合。"))
      next false
    elsif !poke2.isSpecies?(:GLASTRIER) &&
          !poke2.isSpecies?(:SPECTRIER)
      scene.pbDisplay(_INTL("不能与宝可梦融合。"))
      next false
    end
    newForm = 0
    newForm = 1 if poke2.isSpecies?(:GLASTRIER)
    newForm = 2 if poke2.isSpecies?(:SPECTRIER)
    pkmn.setForm(newForm) {
      pkmn.fused = poke2
      pbRemovePokemonAt(chosen)
      scene.pbHardRefresh
      scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
    }
    next true
  end
  # Unfusing
  if $Trainer.party.length>=6
    scene.pbDisplay(_INTL("没有空间可供分离。"))
    next false
  end
  pkmn.setForm(0) {
    $Trainer.party[$Trainer.party.length] = pkmn.fused
    pkmn.fused = nil
    scene.pbHardRefresh
    scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
  }
  next true
})

ItemHandlers::UseOnPokemon.add(:ZYGARDECUBE,proc { |item,pkmn,scene|
  if !pkmn.isSpecies?(:ZYGARDE)
    scene.pbDisplay(_INTL("没有任何效果。"))
    next false
  end
  if pkmn.fainted?
    scene.pbDisplay(_INTL("无法对濒死的宝可梦使用。"))
    next false
  end
  
  # 检查是否为不可用的形态（核心形态和细胞形态）
  invalidForms = [4, 5, 6, 7, 8]  # 红核心、蓝核心、细胞
  if invalidForms.include?(pkmn.form)
    scene.pbDisplay(_INTL("这个形态无法使用基格尔德多面体。"))
    next false
  end

  cmd = pbMessage(_INTL("你想要做什么？"), ["教学招式", "改变形态","取消"],-1)
  case cmd
  when -1
    next false
  when 0
    choices = [:EXTREMESPEED,:THOUSANDARROWS,:DRAGONDANCE,:THOUSANDWAVES,:COREENFORCER]
    displayChoices = []
    choices.each{|ch| displayChoices.push(ch)};
    displayChoices.map!{ |name| PBMoves.getName(getID(PBMoves,name))}
    displayChoices.push("取消")
    if displayChoices.length>1
      cmd2 = pbMessage(_INTL("想要让基格尔德学习哪一个招式？"),displayChoices)
      if cmd2 < (displayChoices.length - 1)
        pbLearnMove(pkmn,getConst(PBMoves,choices[cmd2]))
        next true
      end
    else
      scene.pbDisplay(_INTL("未发现任何核心"))
      next false
    end
  when 1
    oldForm = pkmn.form
    # 只显示可用的形态选项（排除核心和细胞形态）
    forms = ["50%气场破坏","10%气场破坏","50%群聚变形","10%群聚变形","取消"]
    cmd2 = pbMessage(_INTL("想要变成哪一个形态？"),forms,-1)
    next false if cmd2 == -1 || cmd2 == 4
    pkmn.form = cmd2
    if pkmn.form != oldForm
      scene.pbDisplay(_INTL("{1}改变了形态！",pkmn.name))
      next true
    else
      scene.pbDisplay(_INTL("它已经是{1}形态了！",forms[cmd2]))
      next false
    end
  end
})

ItemHandlers::UseFromBag.add(:LANTERN,proc{|item|
darkness = $PokemonTemp.darknessSprite
next false if !darkness || darkness.disposed?
   next 2
})
ItemHandlers::UseInField.add(:LANTERN,proc{|item|
next false if !pbCheckHiddenMoveBadge(BADGE_FOR_FLASH,true)
  if !pbGetMetadata($game_map.map_id,MetadataDarkMap)
    pbMessage(_INTL("不能在这里使用。"))
    next false
  end
  if $PokemonGlobal.flashUsed
    pbMessage(_INTL("这里已经被照亮了。"))
    next false
  end
  darkness = $PokemonTemp.darknessSprite
  next false if !darkness || darkness.disposed?
  pbMessage(_INTL("拿出了提灯！"))
  $PokemonGlobal.flashUsed = true
  radiusDiff = 8*Graphics.frame_rate/20
  while darkness.radius<darkness.radiusMax
    Graphics.update
    Input.update
    pbUpdateSceneMap
    darkness.radius += radiusDiff
    darkness.radius = darkness.radiusMax if darkness.radius>darkness.radiusMax
  end
  next true
})

# 无限之笛 - 在背包中使用
ItemHandlers::UseFromBag.add(:EONFLUTE,proc{|item|
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next false
  end
  pbMessage(_INTL("\\me[无限之笛]{1}吹响了无限之笛。\\wtnp[10]",$Trainer.name))
  if !pbGetMetadata($game_map.map_id,MetadataOutdoor)||
    ESMM_Config::BAN_MAPS.include?($game_map.map_id) 
    pbMessage(_INTL("似乎没有宝可梦听到笛声。"))
    next 0
  end
  pbMessage(_INTL("宝可梦听到笛声了！"))
  scene = PokemonRegionMap_Scene.new(-1,false)
  screen = PokemonRegionMapScreen.new(scene)
  ret = screen.pbStartFlyScreen
  next 0 if !ret
  $PokemonTemp.flydata=ret
  # 10%固拉多，45%拉帝欧斯，45%拉帝亚斯
  r = rand(100)
  if r < 10
    pkmn = pbGenPkmn(:GROUDON, 50)
    pkmn.form = 2  # 飞翔固拉多
  elsif r < 55
    pkmn = pbGenPkmn(:LATIOS, 50)
  else
    pkmn = pbGenPkmn(:LATIAS, 50)
  end
  flybird_species = pkmn.species
  if !pbHiddenMoveAnimation(pkmn)
    pbMessage(_INTL("{1}召唤的宝可梦使用了飞翔！",$Trainer.name))
  end
  pbFlyAnimation(true, nil, :EONFLUTE, flybird_species)
  pbFadeOutIn {
    $game_temp.player_new_map_id    = $PokemonTemp.flydata[0]
    $game_temp.player_new_x         = $PokemonTemp.flydata[1]
    $game_temp.player_new_y         = $PokemonTemp.flydata[2]
    $game_temp.player_new_direction = 2
    $PokemonTemp.flydata = nil
    $scene.transfer_player
    $game_map.autoplay
    $game_map.refresh
  }
  pbFlyAnimation(false, nil, :EONFLUTE, flybird_species)
  pbEraseEscapePoint
  next 0
})

# 无限之笛 - 在野外通过快捷键使用
ItemHandlers::UseInField.add(:EONFLUTE,proc{|item|
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next false
  end
  pbMessage(_INTL("\\me[无限之笛]{1}吹响了无限之笛。\\wtnp[10]",$Trainer.name))
  if !pbGetMetadata($game_map.map_id,MetadataOutdoor)||
    ESMM_Config::BAN_MAPS.include?($game_map.map_id) 
    pbMessage(_INTL("似乎没有宝可梦听到笛声。"))
    next false
  end
  pbMessage(_INTL("宝可梦听到笛声了！"))
  scene = PokemonRegionMap_Scene.new(-1,false)
  screen = PokemonRegionMapScreen.new(scene)
  ret = screen.pbStartFlyScreen
  next false if !ret
  $PokemonTemp.flydata=ret
  # 10%固拉多，45%拉帝欧斯，45%拉帝亚斯
  r = rand(100)
  if r < 10
    pkmn = pbGenPkmn(:GROUDON, 50)
    pkmn.form = 2  # 飞翔固拉多
  elsif r < 55
    pkmn = pbGenPkmn(:LATIOS, 50)
  else
    pkmn = pbGenPkmn(:LATIAS, 50)
  end
  flybird_species = pkmn.species
  if !pbHiddenMoveAnimation(pkmn)
    pbMessage(_INTL("{1}召唤的宝可梦使用了飞翔！",$Trainer.name))
  end
  pbFlyAnimation(true, nil, :EONFLUTE, flybird_species)
  pbFadeOutIn {
    $game_temp.player_new_map_id    = $PokemonTemp.flydata[0]
    $game_temp.player_new_x         = $PokemonTemp.flydata[1]
    $game_temp.player_new_y         = $PokemonTemp.flydata[2]
    $game_temp.player_new_direction = 2
    $PokemonTemp.flydata = nil
    $scene.transfer_player
    $game_map.autoplay
    $game_map.refresh
  }
  pbFlyAnimation(false, nil, :EONFLUTE, flybird_species)
  pbEraseEscapePoint
  next true
})

ItemHandlers::UseFromBag.add(:ETHEREALNEXUS,proc { |item|
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next false
  end
  #if !pbGetMetadata($game_map.map_id,MetadataOutdoor)||
  if ESMM_Config::BAN_MAPS.include?($game_map.map_id) 
    pbMessage(_INTL("无法在这里使用"))
    next 0
  end
  next 2
  dimensionality_warp
})
ItemHandlers::UseInField.add(:ETHEREALNEXUS,proc { |item|
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next false
  end
  #if !pbGetMetadata($game_map.map_id,MetadataOutdoor)||
  if ESMM_Config::BAN_MAPS.include?($game_map.map_id) 
    pbMessage(_INTL("无法在这里使用"))
    next 0
  end
  dimensionality_warp
  next 1
})


ItemHandlers::UseFromBag.add(:HEAVENCRYSEAL,proc { |item|
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next false
  end
  if !pbGetMetadata($game_map.map_id,MetadataOutdoor)||
    ESMM_Config::BAN_MAPS.include?($game_map.map_id) 
    pbMessage(_INTL("无法在这里使用"))
    next 0
  end
  next 2
  pbCrystalWarp
})
ItemHandlers::UseInField.add(:HEAVENCRYSEAL,proc { |item|
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    next false
  end
  if !pbGetMetadata($game_map.map_id,MetadataOutdoor)||
    ESMM_Config::BAN_MAPS.include?($game_map.map_id) 
    pbMessage(_INTL("无法在这里使用"))
    next 0
  end
  pbCrystalWarp
  next 1
})



ItemHandlers::UseOnPokemon.add(:SCROLLOFWATERS,proc { |item,pkmn,scene|
  if !pkmn.isSpecies?(:KUBFU)
    scene.pbDisplay("没有任何效果。")
    next false
  end
  newspecies = getID(PBSpecies,:URSHIFU)
  pkmn.setForm(1)
  pbFadeOutInWithMusic {
    evo = PokemonEvolutionScene.new
    evo.pbStartScreen(pkmn,newspecies)
    evo.pbEvolution(false)
    evo.pbEndScreen
    if scene.is_a?(PokemonPartyScreen)
      scene.pbRefresh
    end
  }
  next true
})

ItemHandlers::UseOnPokemon.add(:SCROLLOFDARKNESS,proc { |item,pkmn,scene|
  if !pkmn.isSpecies?(:KUBFU)
    scene.pbDisplay("没有任何效果。")
    next false
  end
  newspecies = getID(PBSpecies,:URSHIFU)
  pkmn.setForm(0)
  pbFadeOutInWithMusic {
    evo = PokemonEvolutionScene.new
    evo.pbStartScreen(pkmn,newspecies)
    evo.pbEvolution(false)
    evo.pbEndScreen
    if scene.is_a?(PokemonPartyScreen)
      scene.pbRefresh
    end
  }
  next true
})

ItemHandlers::UseOnPokemon.add(:EXPPOT,proc { |item,pkmn,scene|
  $Trainer.exp_pot = 0 if !$Trainer.exp_pot
  if $Trainer.exp_pot == 0
    scene.pbDisplay(_INTL("可供灌注的经验值不足。"))
    next false
  end
  maxLevels = MAX_LEVEL
  lv_lock_on = $game_switches[199]
  legue_pass = $game_switches[12]
  index = -1
  # 如果超过200级则无法使用
  if	pkmn.level>=maxLevels[index]
    scene.pbDisplay(_INTL("没有任何效果。"))
    next false
  end
  exp_max = PBExperience.pbGetStartExperience(maxLevels[index],pkmn.growthrate)-pkmn.exp
  # 如果开启了等级限制
  if lv_lock_on
    if legue_pass
      for i in 8..15
        if !$Trainer.badges[i]
          index = i+1
          break
        end
      end
    else
      for i in 0..7
        if !$Trainer.badges[i]
          index = i
          break
        end
      end
    end
    # 如果当前等级超过上限
    if pkmn.level>=maxLevels[index]
      ret = pbCheckEvolution(pkmn,0)
      if ret<=0
        if index == -1
          scene.pbDisplay(_INTL("没有任何效果。"))
        else
          scene.pbDisplay(_INTL("直到解锁{1}级限制前无法使用。",maxLevels[index+1]))
        end
        next false
      end
    end
    exp_max = PBExperience.pbGetStartExperience(maxLevels[index],pkmn.growthrate)-pkmn.exp
  end
  exp_max = [$Trainer.exp_pot, exp_max].min
  params = ChooseNumberParams.new
  params.setRange(1,exp_max)
  params.setDefaultValue(1)
  params.setCancelValue(0)
  exp_gain = pbMessageChooseNumber(_INTL("需要灌注多少经验值？\n(当前最大只能灌注{1})",exp_max),params)
  next false if exp_gain == 0
  newexp=PBExperience.pbAddExperience(pkmn.exp,exp_gain,pkmn.growthrate)
  newlevel=PBExperience.pbGetLevelFromExperience(newexp,pkmn.growthrate)
  curlevel=pkmn.level
  leveldif = newlevel - curlevel
  scene.pbDisplay(_INTL("你的宝可梦获得了{1}点经验值！",exp_gain))
  if newlevel==curlevel
    pkmn.exp=newexp
    pkmn.calcStats
    scene.pbRefresh
  else
    pbSEPlay("Pkmn level up")
    old_level = pkmn.level
    pbChangeLevel(pkmn, pkmn.level + leveldif, scene)
    moveList = pkmn.getMoveList
    moveList.each do |m|
      next if m[0] <= old_level || m[0] > pkmn.level
      pbLearnMove(pkmn, m[1], true) { scene.pbUpdate }
    end
    scene.pbHardRefresh
  end
  $Trainer.exp_pot -= exp_gain
  next true
})