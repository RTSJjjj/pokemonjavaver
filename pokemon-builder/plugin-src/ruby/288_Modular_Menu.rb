#===============================================================================
#  Modular Pause Menu
#    by Luka S.J.
# ---------------- 
#  Provides only features present in the default version of the Pokedex in
#  Essentials. Mean as a new cosmetic overhaul, adhering to the UI design
#  language of the Elite Battle System: The Next Generation
#
#  Enjoy the script, and make sure to give credit!
#-------------------------------------------------------------------------------
#  load script
#===============================================================================
# set up plugin metadata
if defined?(PluginManager)
  PluginManager.register({
    :name => "Modular Menu",
    :version => "1.3",
    :link => "https://luka-sj.com/res/modmn",
    :dependencies => [
      ["Luka's Scripting Utilities", "3.0"]
    ],
    :credits => ["Luka S.J."],
    :incompatibilities => "DP Pause Menu"
  })
else
  raise "This script is only compatible with Essentials v18.x!"
end
#-------------------------------------------------------------------------------
#  Your own entries for the pause menu
#
#  How to use
#
#  MenuHandlers.addEntry(:name,"button text","icon name",proc{|menu|
#    # code you want to run
#    # when the entry in the menu is selected
#  },proc{ # code to check if menu entry is available })
#-------------------------------------------------------------------------------
# PokeDex
MenuHandlers.addEntry(:POKEDEX,_INTL("图鉴"),"menuPokedex",proc{|menu|
 reset_regional_dex
if USE_CURRENT_REGION_DEX
    pbFadeOutIn(99999){
      scene = PokemonPokedex_Scene.new
      screen = PokemonPokedexScreen.new(scene)
      screen.pbStartScreen
      menu.refresh
    }
  else
    if $PokemonGlobal.pokedexViable.length==1
      $PokemonGlobal.pokedexDex = $PokemonGlobal.pokedexViable[0]
      $PokemonGlobal.pokedexDex = -1 if $PokemonGlobal.pokedexDex==$PokemonGlobal.pokedexUnlocked.length-1
      pbFadeOutIn(99999){
        scene = PokemonPokedex_Scene.new
        screen = PokemonPokedexScreen.new(scene)
        screen.pbStartScreen
        menu.refresh
      }
    else
      pbFadeOutIn(99999){
        scene = PokemonPokedexMenu_Scene.new
        screen = PokemonPokedexMenuScreen.new(scene)
        screen.pbStartScreen
        menu.refresh
      }
    end
  end
},proc{ return $Trainer.pokedex && $PokemonGlobal.pokedexViable.length > 0 })

#change_盒子#
MenuHandlers.addEntry(:PC,_INTL("寄存系统"),"menuPC",proc{|menu|
  pbSEPlay("BW2MenuChoose")
  maps = [95,96,97,98,117,118,119,153,299,357,416,465,467]
  if maps.include?($game_map.map_id)
    pbMessage(_INTL("暂时无法使用。"))
  else
    pbFadeOutIn {
      scene = PokemonStorageScene.new
      screen = PokemonStorageScreen.new(scene,$PokemonStorage)
      screen.pbStartScreen(0)
    }
  end
},proc{ return $Trainer.pokepc })

# Party Screen
MenuHandlers.addEntry(:POKEMON,_INTL("宝可梦"),"menuPokemon",proc{|menu|
  sscene = PokemonParty_Scene.new
  sscreen = PokemonPartyScreen.new(sscene,$Trainer.party)
  hiddenmove = nil
  pbFadeOutIn(99999) { 
    hiddenmove = sscreen.pbPokemonScreen
    if hiddenmove
      menu.pbEndScene
      menu.endscene = false
    end
  }
  if hiddenmove
    Kernel.pbUseHiddenMove(hiddenmove[0],hiddenmove[1])
    menu.close = true
  end
},proc{ return $Trainer.party.length > 0 })
# Bag Screen
MenuHandlers.addEntry(:BAG,_INTL("背包"),"menuBag",proc{|menu|
  item = 0
  scene = PokemonBag_Scene.new
  screen = PokemonBagScreen.new(scene,$PokemonBag)
  pbFadeOutIn(99999) { 
  item = screen.pbStartScreen 
  if item > 0
    menu.pbEndScene
    menu.endscene = false
  end
  }
  if item > 0
    Kernel.pbUseKeyItemInField(item)
    menu.close = true
  end
},proc{ return true })

# Habitat List (分布列表)
MenuHandlers.addEntry(:HABITAT, _INTL("分布图鉴"), "menuQuests", proc{|menu|
  pbFadeOutIn(99999) {
    pbLoadHabitatList
    menu.refresh
  }
}, proc{ 
  return $Trainer && $Trainer.habitatData && $Trainer.habitatData.length > 0 && 
         Habitats.getHabitatList.length > 0
})

# PokeGear
MenuHandlers.addEntry(:POKEGEAR,_INTL("宝可装置"),"menuPokegear",proc{|menu|
  scene = PokemonPokegear_Scene.new
  screen = PokemonPokegearScreen.new(scene)
  pbFadeOutIn(99999) { 
    screen.pbStartScreen
  }
},proc{ return $Trainer.pokegear })
#-------------------------------------------------------------------------------
#  MQS
#-------------------------------------------------------------------------------
MenuHandlers.addEntry(:MQS,_INTL("任务"), "menuQuests",proc{|menu|
    scene = QuestList_Scene.new
    screen = QuestList_Screen.new(scene)
      pbFadeOutIn(99999) {
    screen.pbStartScreen
      }
},proc{ return true })
 # }
#end
# condition to satisfy
#MenuHandlers.add_condition(:MQS) { next hasAnyQuests? }


# Trainer Card
MenuHandlers.addEntry(:TRAINER,_INTL("\\pn"),"menuTrainer",proc{|menu|
  scene = PokemonTrainerCard_Scene.new
  screen = PokemonTrainerCardScreen.new(scene)
  pbFadeOutIn(99999) { 
    screen.pbStartScreen
  }
},proc{ return true })

# Save Screen
MenuHandlers.addEntry(:SAVE,_INTL("保存"),"menuSave",proc{|menu|
  scene = PokemonSave_Scene.new
  screen = PokemonSaveScreen.new(scene)
  menu.pbEndScene
  menu.endscene = false
  if screen.pbSaveScreen
    menu.close = true
  else
    menu.pbStartScene
    menu.pbShowMenu
    menu.close = false
  end
},proc{ return !$game_system || !$game_system.save_disabled && !(pbInSafari? || pbInBugContest?)})

# Load Screen
MenuHandlers.addEntry(:LOAD,_INTL("读档"),"menuLoad",proc{|menu|
  scene = PokemonLoad_Scene.new
  screen = PokemonLoadScreen.new(scene)
  menu.pbEndScene
  menu.endscene = false
  if pbConfirmMessage(_INTL("确定要返回到加载界面吗？"))
    pbBGMStop
    screen.pbStartLoadScreen
    menu.close = true
  else
    menu.pbStartScene
    menu.pbShowMenu
    menu.close = false
  end
},proc{ return true })


# Quit Safari-Zone
MenuHandlers.addEntry(:QUIT,_INTL("退出"),"menuQuit",proc{|menu|
  if pbInSafari?
    if Kernel.pbConfirmMessage(_INTL("你想退出狩猎吗？"))
      menu.pbEndScene
      menu.endscene = false
      menu.close = true
      pbSafariState.decision=1
      pbSafariState.pbGoToStart
    end
  else
    if Kernel.pbConfirmMessage(_INTL("你想结束比赛吗？"))
      menu.pbEndScene
      menu.endscene = false
      menu.close = true
      pbBugContestState.pbStartJudging
      return
    end
  end
},proc{ return pbInSafari? || pbInBugContest? })
# Options Screen
MenuHandlers.addEntry(:OPTIONS,_INTL("设置"),"menuOptions",proc{|menu|
  scene = PokemonOption_Scene.new
  screen = PokemonOptionScreen.new(scene)
  pbFadeOutIn(99999) {
    screen.pbStartScreen
    pbUpdateSceneMap
  }
},proc{ return true })

MenuHandlers.addEntry(:EXIT,_INTL("退出游戏"),"menuExit",proc{|menu|
  menu.pbEndScene
  menu.endscene = false
  choice = pbMessage(_INTL("在退出游戏前你要保存进度吗？"),["存档退出","直接退出","取消"],-1)
  case choice
  when 0
    if pbSave
      pbMessage(_INTL("\\me[GUI save game]记录保存成功！\\wtnp[80]"))
      should_exit = true
    else
      pbMessage(_INTL("\\se[GUI sel buzzer]记录保存失败！\\wtnp[30]"))
    end
  when 1
    should_exit = true
  else
    should_exit = false
  end
  menu.pbStartScene
  menu.pbShowMenu
  menu.close = should_exit
  exit if should_exit
},proc{ return true })
# Debug Menu
MenuHandlers.addEntry(:DEBUG,_INTL("调试"),"menuDebug",proc{|menu|
  pbFadeOutIn(99999) { 
    pbDebugMenu
    menu.refresh
  }
},proc{ return $DEBUG })
