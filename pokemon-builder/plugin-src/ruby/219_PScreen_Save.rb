def pbSave(safesave=false)
  $Trainer.metaID=$PokemonGlobal.playerID
  begin
    File.open("Save/Game.rxdata","wb") { |f|
       Marshal.dump($Trainer,f)
       Marshal.dump(Graphics.frame_count,f)
       if $data_system.respond_to?("magic_number")
         $game_system.magic_number = $data_system.magic_number
       else
         $game_system.magic_number = $data_system.version_id
       end
       $game_system.save_count+=1
       Marshal.dump($game_system,f)
       Marshal.dump($PokemonSystem,f)
       Marshal.dump($game_map.map_id,f)
       Marshal.dump($game_switches,f)
       Marshal.dump($game_variables,f)
       Marshal.dump($game_self_switches,f)
       Marshal.dump($game_screen,f)
       Marshal.dump($MapFactory,f)
       Marshal.dump($game_player,f)
       $PokemonGlobal.safesave=safesave
       Marshal.dump($PokemonGlobal,f)
       Marshal.dump($PokemonMap,f)
       Marshal.dump($PokemonBag,f)
       Marshal.dump($PokemonStorage,f)
       Marshal.dump(ESSENTIALS_VERSION,f)
    }
    Graphics.frame_reset
  rescue
    return false
  end
  return true
end

def pbEmergencySave
  oldscene=$scene
  $scene=nil
  pbMessage(_INTL("脚本花的时间太长了。\n游戏将重新开始。"))
  return if !$Trainer
  if safeExists?("Save/Game.rxdata")
    File.open("Save/Game.rxdata",  'rb') { |r|
      File.open("Save/Game.rxdata", 'wb') { |w|
        while s = r.read(4096)
          w.write s
        end
      }
    }
  end
  if pbSave
    pbMessage(_INTL("\\se[]The game was saved.\\me[GUI save game] The previous save file has been backed up.\\wtnp[30]"))
  else
    pbMessage(_INTL("\\se[]Save failed.\\wtnp[30]"))
  end
  $scene=oldscene
end



class PokemonSave_Scene
  def pbStartScreen
    @viewport=Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z=99999
    @sprites={}
    totalsec = Graphics.frame_count / 40
    hour = totalsec / 60 / 60
    min = totalsec / 60 % 60
    mapname=$game_map.name
    textColor = ["0070F8,78B8E8","E82010,F8A8B8","0070F8,78B8E8"][$Trainer.gender]
    locationColor = "209808,90F090"   # green
    loctext=_INTL("<ac><c3={1}>{2}</c3></ac>",locationColor,mapname)
    loctext+=_INTL("玩家<r><c3={1}>{2}</c3><br>",textColor,$Trainer.name)
    if hour>0
      loctext+=_INTL("时间<r><c3={1}>{2}h {3}m</c3><br>",textColor,hour,min)
    else
      loctext+=_INTL("时间<r><c3={1}>{2}m</c3><br>",textColor,min)
    end
    loctext+=_INTL("徽章<r><c3={1}>{2}</c3><br>",textColor,$Trainer.numbadges)
    if $Trainer.pokedex
      loctext+=_INTL("图鉴<r><c3={1}>{2}/{3}</c3>",textColor,$Trainer.pokedexOwned,$Trainer.pokedexSeen)
    end
    @sprites["locwindow"]=Window_AdvancedTextPokemon.new(loctext)
    @sprites["locwindow"].viewport=@viewport
    @sprites["locwindow"].x=0
    @sprites["locwindow"].y=0
    @sprites["locwindow"].width=228 if @sprites["locwindow"].width<228
    @sprites["locwindow"].visible=true
  end

  def pbEndScreen
    pbDisposeSpriteHash(@sprites)
    @viewport.dispose
  end
end



class PokemonSaveScreen
  def initialize(scene)
    @scene=scene
  end

  def pbDisplay(text,brief=false)
    @scene.pbDisplay(text,brief)
  end

  def pbDisplayPaused(text)
    @scene.pbDisplayPaused(text)
  end

  def pbConfirm(text)
    return @scene.pbConfirm(text)
  end

  def pbSaveScreen
    ret=false
    @scene.pbStartScreen
    if pbConfirmMessage(_INTL("您是否要保存自己的进度？"))
      if safeExists?("Save/Game.rxdata")
        if $PokemonTemp.begunNewGame
          pbMessage(_INTL("警告！"))
          pbMessage(_INTL("已经有一个其他时空的进度文件。"))
          pbMessage(_INTL("如果您在此进度保存，另一个\n进度将丢失。"))
          if !pbConfirmMessageSerious(
             _INTL("您确定要立即保存并覆盖其他\n进度文件吗?"))
            pbSEPlay("GUI save choice")
            @scene.pbEndScreen
            return false
          end
        end
      end
      $PokemonTemp.begunNewGame=false
      pbSEPlay("GUI save choice")
      if pbSave
        pbMessage(_INTL("\\se[]{1}正在保存目前的进度。\\me[GUI save game]\\wtnp[30]",$Trainer.name))
        ret=true
      else
        pbMessage(_INTL("\\se[]Save failed.\\wtnp[30]"))
        ret=false
      end
    else
      pbSEPlay("GUI save choice")
    end
    @scene.pbEndScreen
    return ret
  end
end



def pbSaveScreen
  scene = PokemonSave_Scene.new
  screen = PokemonSaveScreen.new(scene)
  ret = screen.pbSaveScreen
  return ret
end
