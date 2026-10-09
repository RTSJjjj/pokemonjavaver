######Egg Hatcher By: KYU  ##################################################
#There are two ways of using this script:
#   -Making an object:
#     First, add the following line to items PBS 
#     (change xxx to the id of the item):
#        XXX,EGGHATCHER,Egg Hatcher,Egg Hatchers,8,0,"An Egg Hatcher in which to keep up to 6 eggs until they hatch.",2,0,6
#     Then, add itemXXX.png to the Graphics/Icons folder.
#     
#     The functionality code is already implemented within this script. 
#     That includes item usage and egg storage after receiving them.
#
#   -External script call:
#     Whether calling it from the menu or a random npc, just call the following 
#     method:
#         openHatcher
#     In case of using this method to use the hatcher, you can use a 
#     global switch to make it available to the player. 
#     Just change openHatcher_SWITCH to the id of the switch you wanna use.
# 
#CREDITS MUST BE GIVEN TO EVERYONE LISTED ON THE POST
################################################################################
# CONSTANTS
################################################################################
EGGHATCHER_SWITCH = 50 # Global switch that indicates if the hatcher is available
################################################################################

if defined?(PluginManager)
  PluginManager.register({
  :name => "Egg Hatcher",
  :version => "1.0",
  :credits => ["Kyu","Clara","Turner"]
  })
end

class PokemonGlobalMetadata
  attr_accessor :eggs
  alias old_initialize initialize
  def initialize
    old_initialize
    @eggs ||= [nil,nil,nil,nil,nil,nil]
  end
end

class EggSprite < SpriteWrapper
  def initialize(viewport,selected,pokemon, x, y)
    super(viewport)
    @sprites = {}
    @animframe = 0
    @curFrame = 0
    @pokemon = pokemon
    @selected = selected
    self.bitmap = Bitmap.new(68, 100)
    self.x = x
    self.y = y
    refresh
  end
  
  def refresh
    if @pokemon != nil
      @frameskip = 20
      @frameskip = 15 if @pokemon.eggsteps<10200
      @frameskip = 10 if @pokemon.eggsteps<2550
      @frameskip = 5 if @pokemon.eggsteps<1275
      @sprites["egg"] = AnimatedSprite.create(pbPokemonIconFile(@pokemon),2,@frameskip,self.viewport)
      @sprites["egg"].x = self.x + 2
      @sprites["egg"].y = self.y - 5
      @sprites["egg"].play
      base = Color.new(6,35,52)
      shadow= Color.new(169,179,184)
      pbSetSystemFont(self.bitmap)
      pbDrawTextPositions(self.bitmap,[[@pokemon.eggsteps.to_s,35,65,2,base,shadow]])
    end

    if @selected
      self.bitmap.blt(0,0,Bitmap.new("Graphics/Pictures/Egg Hatcher/selection"),Rect.new(0,0,68,100))
    end
  end

  def dispose
    super
    pbDisposeSpriteHash(@sprites)
  end
  
  def update
    pbUpdateSpriteHash(@sprites)
  end
end

class Hatcher
  def initialize
    if !$PokemonGlobal.eggs
      $PokemonGlobal.eggs ||= [nil,nil,nil,nil,nil,nil]
    end
    @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z = 99999
    @sprites = {}
    @eggs = {}
    @index = 0
    
    @sprites["bg"] = Sprite.new(@viewport)
    @sprites["bg"].bitmap = Bitmap.new("Graphics/Pictures/Egg Hatcher/hatcherbg")

    @sprites["text"] = Sprite.new(@viewport)
    @sprites["text"].bitmap = Bitmap.new(183,183)
    @sprites["text"].x = 356 
    @sprites["text"].y = 84
    pbSetSystemFont(@sprites["text"].bitmap)
    refresh
  end
  
  def refresh
    disposeEggs
    @sprites["text"].bitmap.clear
    eggs = $PokemonGlobal.eggs
    for index in 0..5
      if index < 3
        x = 46 + 80*index
        y = 46
      else
        x = 46 + 80*(index - 3)
        y = 158
      end
      selected = (index == @index)? true : false
      @eggs["#{index}"] = EggSprite.new(@viewport,selected,eggs[index], x, y)
    end
    
    if eggs[@index] != nil
      pokemon = eggs[@index]
      eggstate=_INTL("看来这个蛋需\n要很长时间才\n能孵化。")
      eggstate=_INTL("会孵化出什么\n宝可梦呢？似乎\n还需要一些时间。") if pokemon.eggsteps<10200
      eggstate=_INTL("蛋偶尔会摇动，\n应该是快\n要孵化了。") if pokemon.eggsteps<2550
      eggstate=_INTL("可以听到从里\n面传出的声音！\n似乎要孵化了！") if pokemon.eggsteps<1275
      drawFormattedTextEx(@sprites["text"].bitmap,0,0,183,eggstate)
    end
  end
  
  def disposeEggs
    @eggs.each_value{|egg|
      egg.dispose
    }
  end
  
  def dispose
    disposeEggs
    pbDisposeSpriteHash(@sprites)
    @viewport.dispose
  end
  
  def update
    loop do
      @eggs.each_value{|egg|
      egg.update
      }
      
      if Input.trigger?(Input::RIGHT) && (@index+1)%3 != 0
        @index += 1
        pbSEPlay("Choose")
        refresh
      end
      
      if Input.trigger?(Input::LEFT) && (@index != 0 && @index != 3)
        @index -= 1
        pbSEPlay("Choose")
        refresh
      end
      
      if Input.trigger?(Input::UP) && @index >= 3
        @index -= 3
        pbSEPlay("Choose")
        refresh
      end
      
      if Input.trigger?(Input::DOWN) && @index <= 2 
        @index += 3
        pbSEPlay("Choose")
        refresh
      end
      
      if Input.trigger?(Input::C)
        if $PokemonGlobal.eggs[@index] == nil
    ret = pbMessage("孵化器是空的，\n要从哪里选择蛋并放入？",["队伍","盒子","取消"],-1)
          case ret
          when 0
            chosen=-1
            pbFadeOutIn(99999){
              scene=PokemonParty_Scene.new
              screen=PokemonPartyScreen.new(scene,$Trainer.party)
              screen.pbStartScene(_INTL("请选择一颗蛋。"),false)
              chosen=screen.pbChoosePokemon
              screen.pbEndScene
            }
            if chosen>=0 && chosen<=5
              if !$Trainer.party[chosen].egg?
                Kernel.pbMessage("选择的宝可梦不是一颗蛋。")
              else
                $PokemonGlobal.eggs[@index] = $Trainer.party[chosen]
                $Trainer.party.delete_at(chosen)
              end
            end
          when 1
            chosen=nil
            pbFadeOutIn(99999){
              scene=PokemonStorageScene.new
              screen=PokemonStorageScreen.new(scene,$PokemonStorage)
              chosen=screen.pbChooseEggToHatch
            }
            if chosen
              $PokemonGlobal.eggs[@index] = $PokemonStorage[chosen[0],chosen[1]]
              $PokemonStorage.pbDelete(chosen[0],chosen[1])
            end
          end
        end
        refresh
      end
            
      if Input.trigger?(Input::B)
        dispose
        Input.update
        break
      end
    
      Graphics.update
      Input.update
    end
  end
end

def takeEgg(egg,index)
  sel = Kernel.pbConfirmMessage(_INTL("要将新出生的宝可梦放进队伍吗？"))
  if sel==true
    Kernel.pbMessage(_INTL("您的队伍满了！\1")) if $Trainer.party.length == 6
    pbStorePokemon(egg)
  else
    if pbBoxesFull?
      Kernel.pbMessage(_INTL("队伍没有更多的空间了！\1"))
      Kernel.pbMessage(_INTL("宝可梦盒子已满，不能再接受了！"))
      return
    end
    oldcurbox=$PokemonStorage.currentBox
    storedbox=$PokemonStorage.pbStoreCaught(egg)
    curboxname=$PokemonStorage[oldcurbox].name
    boxname=$PokemonStorage[storedbox].name
    creator=nil
    creator=Kernel.pbGetStorageCreator if $PokemonGlobal.seenStorageCreator
    if storedbox!=oldcurbox
      if creator
     Kernel.pbMessage(_INTL("盒子 \"{1}\" 已满。\1", curboxname, creator))
    else
     Kernel.pbMessage(_INTL("盒子 \"{1}\" 已满。\1", curboxname))
   end
     Kernel.pbMessage(_INTL("{1} 被转移到了盒子 \"{2}\"。", egg.name, boxname))
   else
   Kernel.pbMessage(_INTL("{1} 被转移到了盒子 \"{2}\"。", egg.name, boxname))
    end
  end
  $PokemonGlobal.eggs[index] = nil
end

def pbGenerateEgg(pokemon,text="")
  return false if !pokemon || !$Trainer
  if pokemon.is_a?(String) || pokemon.is_a?(Symbol)
    pokemon=getID(PBSpecies,pokemon)
  end
  begin
    if pokemon.is_a?(Integer)
    pokemon=PokeBattle_Pokemon.new(pokemon,EGGINITIALLEVEL,$Trainer)
    end
    # Get egg steps
    dexdata=pbOpenDexData
    pbDexDataOffset(dexdata,pokemon.species,21)
    eggsteps=dexdata.fgetw
    dexdata.close
    # Set egg's details
    pokemon.name=_INTL("Egg")
    pokemon.eggsteps=eggsteps
    pokemon.obtainText=text
    pokemon.calcStats
  rescue
    pokemon = getID(PBSpecies,pokemon)
    if pokemon.is_a?(Integer)
      pokemon = pbNewPkmn(pokemon,EGG_LEVEL)
    end
    # Get egg steps
    eggSteps = pbGetSpeciesData(pokemon.species,pokemon.form,SpeciesStepsToHatch)
    # Set egg's details
    pokemon.name       = _INTL("Egg")
    pokemon.eggsteps   = eggSteps
    pokemon.obtainText = text
    pokemon.calcStats
  end
  # Add egg to party
  if (getID(PBItems,:EGGHATCHER)==-1 || $PokemonBag.pbHasItem?(:EGGHATCHER)) || $game_switches[EGGHATCHER_SWITCH]
    ret = Kernel.pbConfirmMessage("您想将蛋添加到孵化器中吗？")
    if ret == true
      ret = addEgg(pokemon)
      if ret == true
        return true
      end
    end
  end
  pbStorePokemon(pokemon)
  return true
end

def addEgg(egg)
  if !$PokemonGlobal.eggs
    $PokemonGlobal.eggs ||= [nil,nil,nil,nil,nil,nil]
  end
  $PokemonGlobal.eggs.each_index{|index|
    if $PokemonGlobal.eggs[index] == nil
      $PokemonGlobal.eggs[index] = egg
      return true
    end
  }
  Kernel.pbMessage("孵化器已满。")
  return false
end

Events.onStepTaken+=proc {|sender,e|
   next if !$Trainer || !$PokemonGlobal.eggs
   for i in 0...$PokemonGlobal.eggs.length
     egg = $PokemonGlobal.eggs[i]
     next if egg == nil
     if egg.eggsteps>0
       egg.eggsteps-=1
       for x in $Trainer.pokemonParty
         if isConst?(x.ability,PBAbilities,:FLAMEBODY) ||
            isConst?(x.ability,PBAbilities,:MAGMAARMOR)
           egg.eggsteps-=1
           break
         end
       end
       if egg.eggsteps<=0
        egg.eggsteps=0
        pbHatch(egg)
        takeEgg(egg,i)
       end
     end
   end
}

ItemHandlers::UseFromBag.add(:EGGHATCHER,proc{|item|
  pbFadeOutIn(99999){
    openHatcher
  }
  next 1
})

ItemHandlers::UseInField.add(:EGGHATCHER,proc{|item|
  pbFadeOutIn(99999){
    openHatcher
  }
  next 1
})

def openHatcher
  scene = Hatcher.new
  scene.update
end

