class PokemonBox
  attr_reader   :pokemon
  attr_accessor :name
  attr_accessor :background

  def initialize(name,maxPokemon=30)
    @pokemon = []
    @name = name
    @background = 0
    for i in 0...maxPokemon
      @pokemon[i] = nil
    end
  end

  def length
    return @pokemon.length
  end

  def nitems
    return @pokemon.nitems
  end

  def full?
    return (@pokemon.nitems==self.length)
  end

  def empty?
    return (@pokemon.nitems==0)
  end

  def [](i)
    return @pokemon[i]
  end

  def []=(i,value)
    @pokemon[i] = value
  end

  def each
    @pokemon.each { |item| yield item }
  end

  def clear
    @pokemon.clear
  end
end



class PokemonStorage
  attr_reader   :boxes
  attr_accessor :currentBox
  attr_writer   :unlockedWallpapers
  BASICWALLPAPERQTY = 42

  def initialize(maxBoxes=NUM_STORAGE_BOXES,maxPokemon=30)
    @boxes = []
    for i in 0...maxBoxes
      @boxes[i] = PokemonBox.new(_INTL("盒子 {1}",i+1),maxPokemon)
      @boxes[i].background = i%BASICWALLPAPERQTY
    end
    @currentBox = 0
    @boxmode = -1
    @unlockedWallpapers = []
    for i in 0...allWallpapers.length
      @unlockedWallpapers[i] = false
    end
  end

  def allWallpapers
    return [
       _INTL("森林"),_INTL("城市"),_INTL("沙漠"),_INTL("草原"),
       _INTL("峭壁"),_INTL("火山"),_INTL("雪地"),_INTL("洞穴"),
       _INTL("海滩"),_INTL("海底"),_INTL("河流"),_INTL("天空"),
       _INTL("精灵中心"),_INTL("机器"),_INTL("方格"),_INTL("简约"),
       _INTL("莱希拉姆1"),_INTL("捷克罗姆1"),_INTL("黑白"),_INTL("银河队1"),
       _INTL("樱花"),_INTL("索罗亚克"),_INTL("合众地图"),_INTL("大赛"),
       _INTL("方块"),_INTL("银河队2"),_INTL("宝莱坞"),_INTL("冠军"),
       _INTL("暗黑酋雷姆"),_INTL("焰白酋雷姆"),_INTL("莱希拉姆2"),_INTL("捷克罗姆2"),
       _INTL("心灵之金"),_INTL("灵魂之银"),_INTL("型男"),_INTL("全能竞技赛"),
       _INTL("城都初始"),_INTL("刺耳皮丘"),_INTL("和服少女"),_INTL("火箭队"),
       _INTL("合众小可爱"),_INTL("黑白酋雷姆")
     ]
  end

  def unlockedWallpapers
    @unlockedWallpapers = [] if !@unlockedWallpapers
    return @unlockedWallpapers
  end

  def isAvailableWallpaper?(i)
    @unlockedWallpapers = [] if !@unlockedWallpapers
    return true if i<BASICWALLPAPERQTY
    return true if @unlockedWallpapers[i]
    return false
  end

  def availableWallpapers
    ret = [[],[]]   # Names, IDs
    papers = allWallpapers
    @unlockedWallpapers = [] if !@unlockedWallpapers
    for i in 0...papers.length
      next if !isAvailableWallpaper?(i)
      ret[0].push(papers[i]); ret[1].push(i)
    end
    return ret
  end

  def party
    $Trainer.party
  end

  def party=(_value)
    raise ArgumentError.new("Not supported")
  end

  def maxBoxes
    return @boxes.length
  end

  def maxPokemon(box)
    return 0 if box>=self.maxBoxes
    return (box<0) ? 6 : self[box].length
  end

  def full?
    for i in 0...self.maxBoxes
      return false if !@boxes[i].full?
    end
    return true
  end

  def pbFirstFreePos(box)
    if box==-1
      ret = self.party.nitems
      return (ret==6) ? -1 : ret
    else
      for i in 0...maxPokemon(box)
        return i if !self[box,i]
      end
      return -1
    end
  end

  def [](x,y=nil)
    if y==nil
      return (x==-1) ? self.party : @boxes[x]
    else
      for i in @boxes
        raise "Box is a Pokémon, not a box" if i.is_a?(PokeBattle_Pokemon)
      end
      return (x==-1) ? self.party[y] : @boxes[x][y]
    end
  end

  def []=(x,y,value)
    if x==-1
      self.party[y] = value
    else
      @boxes[x][y] = value
    end
  end

  def pbCopy(boxDst,indexDst,boxSrc,indexSrc)
    if indexDst<0 && boxDst<self.maxBoxes
      found = false
      for i in 0...maxPokemon(boxDst)
        next if self[boxDst,i]
        found = true
        indexDst = i
        break
      end
      return false if !found
    end
    if boxDst==-1   # Copying into party
      return false if self.party.nitems>=6
      self.party[self.party.length] = self[boxSrc,indexSrc]
      self.party.compact!
    else   # Copying into box
      pkmn = self[boxSrc,indexSrc]
      raise "Trying to copy nil to storage" if !pkmn
      pkmn.formTime = nil if pkmn.respond_to?("formTime")
      pkmn.form     = 0 if pkmn.isSpecies?(:SHAYMIN)
      pkmn.heal
      self[boxDst,indexDst] = pkmn
    end
    return true
  end

  def pbMove(boxDst,indexDst,boxSrc,indexSrc)
    return false if !pbCopy(boxDst,indexDst,boxSrc,indexSrc)
    pbDelete(boxSrc,indexSrc)
    return true
  end

  def pbMoveCaughtToParty(pkmn)
    return false if self.party.nitems>=6
    self.party[self.party.length] = pkmn
  end

  def pbMoveCaughtToBox(pkmn,box)
    for i in 0...maxPokemon(box)
      if self[box,i]==nil
        if box>=0
          pkmn.formTime = nil if pkmn.respond_to?("formTime") && pkmn.formTime
          pkmn.form     = 0 if pkmn.isSpecies?(:SHAYMIN)
          pkmn.heal
        end
        self[box,i] = pkmn
        return true
      end
    end
    return false
  end

  def pbStoreCaught(pkmn)
    if @currentBox>=0
      pkmn.formTime = nil if pkmn.respond_to?("formTime")
      pkmn.form     = 0 if pkmn.isSpecies?(:SHAYMIN)
      pkmn.heal
    end
    for i in 0...maxPokemon(@currentBox)
      if self[@currentBox,i]==nil
        self[@currentBox,i] = pkmn
        return @currentBox
      end
    end
    for j in 0...self.maxBoxes
      for i in 0...maxPokemon(j)
        if self[j,i]==nil
          self[j,i] = pkmn
          @currentBox = j
          return @currentBox
        end
      end
    end
    return -1
  end

  def pbDelete(box,index)
    if self[box,index]
      self[box,index] = nil
      self.party.compact! if box==-1
    end
  end

  def clear
    for i in 0...self.maxBoxes
      @boxes[i].clear
    end
  end
end



#===============================================================================
# Regional Storage scripts
#===============================================================================
class RegionalStorage
  def initialize
    @storages = []
    @lastmap = -1
    @rgnmap = -1
  end

  def getCurrentStorage
    if !$game_map
      raise _INTL("The player is not on a map, so the region could not be determined.")
    end
    if @lastmap!=$game_map.map_id
      @rgnmap = pbGetCurrentRegion   # may access file IO, so caching result
      @lastmap = $game_map.map_id
    end
    if @rgnmap<0
      raise _INTL("The current map has no region set. Please set the MapPosition metadata setting for this map.")
    end
    if !@storages[@rgnmap]
      @storages[@rgnmap] = PokemonStorage.new
    end
    return @storages[@rgnmap]
  end

  def allWallpapers
    return getCurrentStorage.allWallpapers
  end

  def availableWallpapers
    return getCurrentStorage.availableWallpapers
  end

  def unlockWallpaper(index)
    getCurrentStorage.unlockWallpaper(index)
  end

  def boxes
    return getCurrentStorage.boxes
  end

  def party
    return getCurrentStorage.party
  end

  def maxBoxes
    return getCurrentStorage.maxBoxes
  end

  def maxPokemon(box)
    return getCurrentStorage.maxPokemon(box)
  end

  def full?
    getCurrentStorage.full?
  end

  def currentBox
    return getCurrentStorage.currentBox
  end

  def currentBox=(value)
    getCurrentStorage.currentBox = value
  end

  def [](x,y=nil)
    getCurrentStorage[x,y]
  end

  def []=(x,y,value)
    getCurrentStorage[x,y] = value
  end

  def pbFirstFreePos(box)
    getCurrentStorage.pbFirstFreePos(box)
  end

  def pbCopy(boxDst,indexDst,boxSrc,indexSrc)
    getCurrentStorage.pbCopy(boxDst,indexDst,boxSrc,indexSrc)
  end

  def pbMove(boxDst,indexDst,boxSrc,indexSrc)
    getCurrentStorage.pbCopy(boxDst,indexDst,boxSrc,indexSrc)
  end

  def pbMoveCaughtToParty(pkmn)
    getCurrentStorage.pbMoveCaughtToParty(pkmn)
  end

  def pbMoveCaughtToBox(pkmn,box)
    getCurrentStorage.pbMoveCaughtToBox(pkmn,box)
  end

  def pbStoreCaught(pkmn)
    getCurrentStorage.pbStoreCaught(pkmn)
  end

  def pbDelete(box,index)
    getCurrentStorage.pbDelete(pkmn)
  end
end



#===============================================================================
#
#===============================================================================
def pbUnlockWallpaper(index)
  $PokemonStorage.unlockedWallpapers[index] = true
end

def pbLockWallpaper(index)   # Don't know why you'd want to do this
  $PokemonStorage.unlockedWallpapers[index] = false
end

#===============================================================================
# Look through Pokémon in storage
#===============================================================================
# Yields every Pokémon/egg in storage in turn.
def pbEachPokemon
  for i in -1...$PokemonStorage.maxBoxes
    for j in 0...$PokemonStorage.maxPokemon(i)
      pkmn = $PokemonStorage[i][j]
      yield(pkmn,i) if pkmn
    end
  end
end

# Yields every Pokémon in storage in turn.
def pbEachNonEggPokemon
  pbEachPokemon { |pkmn,box| yield(pkmn,box) if !pkmn.egg? }
end
