class PokeBattle_Trainer
  attr_accessor :old_save_pid
  attr_accessor :zde_save_pid

  alias old_save_initialize initialize
  def initialize(name,trainertype)
    old_save_initialize(name,trainertype)
    @old_save_pid = nil
    @zde_save_pid = nil
  end
end

class PokemonStorage
  def expand(maxPokemon=30)
    return if maxBoxes == NUM_STORAGE_BOXES
    for i in maxBoxes...NUM_STORAGE_BOXES
      @boxes[i] = PokemonBox.new(_INTL("盒子{1}",i+1),maxPokemon)
      @boxes[i].background = i%BASICWALLPAPERQTY
    end
  end
end

def transportPokemon(version="shadowvow")
  case version
  when "shadowvow"
    transportOldPokemon
  when "zde","0vej"
    transportZDEPokemon
  else
    pbMessage("不支持的转移版本。")
    return false
  end
end

#==============================================================================
# 影辞本家旧版本存档
def transportOldPokemon
  if $Trainer.old_save_pid
    pbMessage("当前存档已从旧版存档转入宝可梦，无法再次转入。")
    return false
  end
  save_file = "save/GameOld.rxdata"
  if !File.exist?(save_file)
    pbMessage("未发现旧版本存档，请确保旧版存档位于\nsave文件夹内，且文件名为GameOld.rxdata")
    return false
  end
  begin
    File.open(save_file) { |f|
      old_trainer        = Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      old_pokemon_global = Marshal.load(f)
      old_save_version   = old_pokemon_global.currentVersion
      Marshal.load(f)
      Marshal.load(f)
      old_storage        = Marshal.load(f)
      old_storage.expand
      Marshal.load(f) unless f.eof?
      allow_version_list = [4000]
      if !allow_version_list.include?(old_save_version)
        pbMessage(_INTL("不支持{1}版本的存档，\n请确保是3.x版本的存档。", old_save_version))
        return false
      end
      
      # 检查盒子100和盒子200
      box_to_transfer = nil
      box_number = nil
      
      if old_storage && old_storage[199] && !old_storage[199].empty?
        box_to_transfer = old_storage[199]
        box_number = 200
      elsif old_storage && old_storage[99] && !old_storage[99].empty?
        box_to_transfer = old_storage[99]
        box_number = 100
      else
        pbMessage("未在盒子100或盒子200中发现宝可梦，转移已取消。\n请在旧版游戏中确认待转移的宝可梦。")
        return false
      end
      
      if box_to_transfer && validate_old_storage(box_to_transfer, box_number)
        move_old_storage(box_to_transfer)
        $Trainer.old_save_pid = old_trainer.id
        pbMessage(_INTL("已成功将此旧存档中的{1}号盒子内的宝可梦\n转移到新版本存档中。", box_number))
        scene = PokemonSave_Scene.new
        screen = PokemonSaveScreen.new(scene)
        if screen.pbSaveScreen
          File.delete(save_file) if File.exists?(save_file)
        end
        return true
      else
        pbMessage("读取旧版本存档失败。")
        return false
      end
    }
  rescue  => e
    p "读取旧版本存档失败：#{e.message}"
    return false
  end
end

def validate_old_storage(box, box_number=200)
  if box.empty?
    pbMessage(_INTL("盒子{1}是空的，转移已取消。\n请在旧版游戏中确认待转移的宝可梦。", box_number))
    return false
  end
  for i in 0...box.length
    pkmn = box[i]
    next if !pkmn
    pkmn.species = update_old_species(pkmn.species)
    if [483, 484, 487, 493, 383, 382, 384, 250, 249,
      644, 643, 646, 716, 717, 718, 791, 792, 800, 
      888, 889, 898, 1007, 1008, 1024, 1072, 1073, 
      1074, 1087, 1088, 1049, 1078, 1069, 1067, 1068, 1138, 1140, 1141].include?(pkmn.species) ||
      pkmn.species >= 1087 && pkmn.species <= 1200
      if pbConfirmMessage(_INTL("无法转移{1}，\n是否跳过并继续转移其他宝可梦？", pkmn.speciesName))
        box[i] = nil
      else
        pbMessage(_INTL("转移已取消。"))
        return false
      end
    elsif pkmn.hasItem?
      if pbConfirmMessage(_INTL("无法转移{1}的道具{2}，\n是否移除道具并继续转移宝可梦？", pkmn.speciesName, PBItems.getName(pkmn.item)))
        pkmn.setItem(0)
      else
        pbMessage(_INTL("转移已取消。"))
        return false
      end
    end
  end
  return true
end

def move_old_storage(box)
  box.each do |pkmn|
    next if !pkmn
    if pbBoxesFull?
      pbMessage(_INTL("没有空间存放精灵了！\1"))
      pbMessage(_INTL("宝可梦盒已经满了，并且无法接受更多。"))
      return
    end
    pkmn.markings = 0
    pkmn.obtainText="穿越时间与空间"
    pkmn.level = 100 if pkmn.level > 100
    pkmn.resetMoves
    pkmn.calcStats
    $PokemonStorage.pbStoreCaught(pkmn)
    Habitats.updateHabitatsForSpecies(pkmn.species)
  end
end

def update_old_species(species)
  species_hash = {
  
  }
  return species if !species_hash.keys.include?(species)
  return species_hash[species]
end


#===========================================================================================
# 兼容零维E界
#===========================================================================================
def transportZDEPokemon
  if $Trainer.zde_save_pid
    pbMessage("当前存档已从零维E界存档转入宝可梦，\n无法再次转入。")
    return false
  end
  save_file = "save/GameZDE.rxdata"
  if !File.exist?(save_file)
    pbMessage("未发现零维E界存档，请确保存档位于\nSave文件夹内，且文件名为GameZDE.rxdata")
    return false
  end
  begin
    File.open(save_file) { |f|
      zde_trainer        = Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      Marshal.load(f)
      zde_pokemon_global = Marshal.load(f)
      zde_save_version   = zde_pokemon_global.currentVersion
      Marshal.load(f)
      Marshal.load(f)
      zde_storage        = Marshal.load(f)
      zde_storage.expand
      Marshal.load(f) unless f.eof?
      allow_version = 260200260605
      if allow_version > zde_save_version
        pbMessage(_INTL("不支持{1}版本的存档，\n请确保是2.2以上版本的存档。", zde_save_version))
        return false
      end
      if zde_storage && validate_zde_storage(zde_storage[47])
        move_zde_storage(zde_storage[47])
        $Trainer.zde_save_pid = zde_trainer.id
        pbMessage(_INTL("已成功将零维E界存档中的48号盒子内的\n宝可梦转移到当前存档中。"))
        scene = PokemonSave_Scene.new
        screen = PokemonSaveScreen.new(scene)
        if screen.pbSaveScreen
          File.delete(save_file) if File.exists?(save_file)
        end
        return true
      else
        pbMessage("转移已取消。")
        return false
      end
    }
  rescue  => e
    p "读取零维E界存档失败：#{e.message}"
    return false
  end
end

def validate_zde_storage(box)
  if box.empty?
    pbMessage(_INTL("盒子48是空的，转移已取消。\n请在零维E界中确认待转移的宝可梦。"))
    return false
  end
  for i in 0...box.length
    pkmn = box[i]
    next if !pkmn
    update_zde_species(pkmn)
    update_zde_form(pkmn)
    ban_species = [483, 484, 487, 493]
    allow_species = [1129, 1130, 1131, 1132, 1133, 1134, 1135, 1136, 1137, 1044, 1045]
    pkmnname = pkmn.species > 1025 && !allow_species.include?(pkmn.species) ? "未知宝可梦" : pkmn.speciesName
    if ban_species.include?(pkmn.species) || pkmn.species >= 1025 && !allow_species.include?(pkmn.species)
      if pbConfirmMessage(_INTL("无法转移{1}，\n是否跳过并继续转移其他宝可梦？", pkmnname))
        box[i] = nil
      else
        pbMessage(_INTL("转移已取消。"))
        return false
      end
    elsif pkmn.hasItem?
      if pbConfirmMessage(_INTL("无法转移{1}的道具，\n是否移除道具并继续转移宝可梦？", pkmnname))
        pkmn.setItem(0)
      else
        pbMessage(_INTL("转移已取消。"))
        return false
      end
    end
  end
  return true
end

def update_zde_species(pkmn)
  species_hash = {
    1026 => 1129,
    1027 => 1130,
    1028 => 1131,
    1029 => 1132,
    1030 => 1133,
    1031 => 1134,
    1032 => 1135,
    1033 => 1136,
    1034 => 1137,
    1035 => 1044,
    1036 => 1045
  }
  if species_hash.keys.include?(pkmn.species)
    pkmn.species = species_hash[pkmn.species]
  end
end

def update_zde_form(pkmn)
  case pkmn.species
  when 152, 153, 154, 255, 256, 257, 728, 729, 730
    if pkmn.form == 0
      pkmn.setForm(2)
    else
      pkmn.setForm(0)
    end
  when 282, 392, 475, 1044, 1045
    pkmn.setForm(0)
  end
end

def move_zde_storage(box)
  box.each do |pkmn|
    next if !pkmn
    if pbBoxesFull?
      pbMessage(_INTL("没有空间存放精灵了！\1"))
      pbMessage(_INTL("宝可梦盒已经满了，并且无法接受更多。"))
      return
    end
    pkmn.ballused = 0 if pkmn.ballused > 25
    pkmn.markings = 0
    pkmn.obtainText="来自零维E界"
    pkmn.level = 100 if pkmn.level > 100
    pkmn.ev = [0,0,0,0,0,0]
    pkmn.resetMoves
    pkmn.calcStats
    $PokemonStorage.pbStoreCaught(pkmn)
    Habitats.updateHabitatsForSpecies(pkmn.species)
  end
end