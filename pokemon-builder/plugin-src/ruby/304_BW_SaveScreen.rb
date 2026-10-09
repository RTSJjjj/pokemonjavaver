#------------------------------------------------------------------------------
# BW Save Screen V18.1
#------------------------------------------------------------------------------
if defined?(PluginManager)
  PluginManager.register({
    :name => "BW Save Screen",
    :version => "1.1",
    :link    => "https://reliccastle.com/resources/460/",
    :credits => ["HDrawer (based on shiney570 script)"]
  })
end
#------------------------------------------------------------------------------
# Settings
#------------------------------------------------------------------------------

# Choose the background style
# 0 = BW
# 1 = BW2
BGSTYLE = 1

# Show or hide the clock on top of the screen
CLOCK = true

#------------------------------------------------------------------------------
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
  pbMessage(_INTL("The script is taking too long. The game will restart."))
  return if !$Trainer
  if safeExists?("Save/Game.rxdata")
    File.open("Save/Game.rxdata",  'rb') { |r|
      File.open("Save/Game.rxdata","wb") { |w|
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
    time=_ISPRINTF("{1:02d}:{2:02d}",hour,min)
    datenow=_ISPRINTF("{1:d}年{2:s}{3:d}日",
    $PokemonGlobal.pbGetTimeNow.year,
    pbGetAbbrevMonthName($PokemonGlobal.pbGetTimeNow.mon),
    $PokemonGlobal.pbGetTimeNow.day)
    @sprites["bg"]=IconSprite.new(0,0,@viewport)
    if BGSTYLE==0
      @sprites["bg"].setBitmap("Graphics/Pictures/Save/bw_background")
    elsif BGSTYLE==1
      @sprites["bg"].setBitmap("Graphics/Pictures/Save/bw2_background")
    end  
    # Creating Party Icons.
    if $Trainer
      if $Trainer.party.length>0
        for i in 0...$Trainer.party.length
          @sprites["pokemon#{i}"]=PokemonIconSprite.new($Trainer.party[i],@viewport)
          @sprites["pokemon#{i}"].x = 96+64+64*(i)
          @sprites["pokemon#{i}"].y = 122+32
        end
      end
    end
    @sprites["overlay"]=BitmapSprite.new(Graphics.width, Graphics.height, @viewport)
    overlay = @sprites["overlay"].bitmap
    overlay.clear
    bwbaseColor    = Color.new(80,80,88)
    bwshadowColor  = Color.new(160,160,168)
    bw2baseColor   = Color.new(231,231,231)
    bw2shadowColor = Color.new(140,140,140)
    pbSetSystemFont(@sprites["overlay"].bitmap)
    textos=[]
    if CLOCK==true
      if BGSTYLE==0
        textos.push([_ISPRINTF("{1:02d} : {2:02d}", Time.now.hour, Time.now.min),256+64,4,2,bw2baseColor,bw2shadowColor])
        textos.push([_INTL("徽章：{1}",$Trainer.numbadges),80+64,203+32,false,bwbaseColor,bwshadowColor])
        textos.push([_INTL("图鉴：{1}", $Trainer.pokedexSeen),288+64,203+32,false,bwbaseColor,bwshadowColor])
        textos.push([_INTL("{1}",$game_map.name),80+64,88+32,false,bwbaseColor,bwshadowColor])
        textos.push([_INTL("游戏时间：{1}", time),80+64,233+32,false,bwbaseColor,bwshadowColor])
        textos.push([_INTL("{1}", datenow),78+64,56+32,false,bwbaseColor,bwshadowColor])
        pbDrawTextPositions(overlay,textos)
      elsif BGSTYLE==1
        textos.push([_ISPRINTF("{1:02d} : {2:02d}", Time.now.hour, Time.now.min),288+64,4,2,bw2baseColor,bw2shadowColor])
        textos.push([_INTL("徽章：{1}",$Trainer.numbadges),80+64,203+32,false,bw2baseColor,bw2shadowColor])
        textos.push([_INTL("图鉴：{1}", $Trainer.pokedexSeen),288+64,203+32,false,bw2baseColor,bw2shadowColor])
        textos.push([_INTL("{1}",$game_map.name),80+64,88+32,false,bw2baseColor,bw2shadowColor])
        textos.push([_INTL("游戏时间：{1}", time),80+64,233+32,false,bw2baseColor,bw2shadowColor])
        textos.push([_INTL("{1}", datenow),78+64,56+32,false,bw2baseColor,bw2shadowColor])
        pbDrawTextPositions(overlay,textos)
      end
    elsif CLOCK==false
      if BGSTYLE==0
        textos.push([_INTL("徽章{1}",$Trainer.numbadges),80+64,203+32,false,bwbaseColor,bwshadowColor])
        textos.push([_INTL("图鉴{1}", $Trainer.pokedexSeen),288+64,203+32,false,bwbaseColor,bwshadowColor])
        textos.push([_INTL("{1}",$game_map.name),80+64,false,bwbaseColor,bwshadowColor])
        textos.push([_INTL("游戏时间：{1}", time),80+64,233+32,false,bwbaseColor,bwshadowColor])
        textos.push([_INTL("{1}", datenow),78+64,56+32,false,bwbaseColor,bwshadowColor])
        pbDrawTextPositions(overlay,textos)
      elsif BGSTYLE==1
        textos.push([_INTL("徽章{1}",$Trainer.numbadges),80+64,203+32,false,bw2baseColor,bw2shadowColor])
        textos.push([_INTL("图鉴{1}", $Trainer.pokedexSeen),288+64,203+32,false,bw2baseColor,bw2shadowColor])
        textos.push([_INTL("{1}",$game_map.name),80+64,88+32,false,bw2baseColor,bw2shadowColor])
        textos.push([_INTL("游戏时间：{1}", time),80+64,233+32,false,bw2baseColor,bw2shadowColor])
        textos.push([_INTL("{1}", datenow),78+64,56+32,false,bw2baseColor,bw2shadowColor])
        pbDrawTextPositions(overlay,textos)
      end
    end
  end

  def pbGetTimeNow
    return Time.now
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
    if pbConfirmMessage(_INTL("要保存直到目前为止的进度吗？"))
#    if pbConfirmMessageSystemModern(0,false,_INTL("Would you like to save the game?"))
      if safeExists?("Save/Game.rxdata")
        if $PokemonTemp.begunNewGame
          pbMessage(_INTL("注意！"))
          pbMessage(_INTL("已经在save文件夹有一个存档。"))
          pbMessage(_INTL("如果在此保存，\n另一个存档将丢失。"))
          if !pbConfirmMessageSerious(
             _INTL("确定要立即保存并覆盖其他\n进度文件吗?"))
            pbSEPlay("GUI save choice")
            @scene.pbEndScreen
            return false
          end
        end
      end
      $PokemonTemp.begunNewGame=false
      pbSEPlay("GUI save choice")
      if pbSave
        pbMessage(_INTL("\\se[]{1}保存了进度。\\me[GUI save game]\\wtnp[30]",$Trainer.name))
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