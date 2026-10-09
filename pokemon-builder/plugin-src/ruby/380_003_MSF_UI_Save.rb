def pbSave(slot=nil,auto=true,safesave=false)
  $Trainer.metaID=$PokemonGlobal.playerID
  begin
    if !File.exist?("save")
      Dir.mkdir("save") rescue nil
    end
    slot = $Trainer.save_slot if !slot
    if slot && !auto
      $Trainer.last_saved = Time.now.to_i
      $Trainer.save_slot = slot
      savefile = SaveData.get_full_path(slot)
    else
      $Trainer.last_saved -= 1
      savefile = SaveData.get_full_path(SaveData::AUTO_SLOTS[0])
    end
    File.open(savefile,"wb") { |f|
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

class PokemonSaveScreen
  
  def pbSaveScreen
    if !File.exist?("Save")
      Dir.mkdir("Save") rescue return nil
    end
    ret=false
    @scene.pbStartScreen
    slots = SaveData::MANUAL_SLOTS
    slots_name = []
    slots.each { |s| 
      name = s.gsub("Game","存档")
      name += File.exist?("Save/"+s+".rxdata") ? "(■)" : "(□)"
      slots_name.push(name)
   }
    pre_select = 0
    pre_select = slots.index($Trainer.save_slot) if $Trainer.save_slot
    choice = pbMessage(_INTL("要将目前为止的游戏进度保存到哪里？"),slots_name,-1,nil,pre_select)
    if choice > -1
      slot = slots[choice]
      savefile = SaveData.get_full_path(slot)
      if safeExists?(savefile)
        if $PokemonTemp.begunNewGame
          pbMessage(_INTL("警告！"))
          pbMessage(_INTL("已经存在一个不同的游戏存档了！"))
          if !pbConfirmMessageSerious(
             _INTL("是否确定要备份其他保存文件并立即保存？"))
            pbSEPlay("GUI save choice")
            @scene.pbEndScreen
            return false
          end
          File.open(savefile,  'rb') { |r|
            File.open(savefile+".bak",'wb'){|w|
              while s = r.read(4096)
                w.write s
              end
            }
          }
          pbMessage(_INTL("已将旧存档备份为"+savefile+".bak。\n"+
                          "如需回到之前的存档，可手动删除save\n"+
                          "文件夹内的"+savefile+"，并重命名\n"+
                          savefile+".bak，删除结尾的.bak。"))
        end
      end
      $PokemonTemp.begunNewGame=false
      pbSEPlay("GUI save choice")
      if pbSave(slot,false)
        pbMessage(_INTL("\\se[]{1}将游戏进度保存到了{2}。\\me[GUI save game]\\wtnp[30]",
                        $Trainer.name, slots_name[choice]))
        ret=true
      else
        pbMessage(_INTL("\\se[]保存失败，请检查是否拥有文件夹权限。\\wtnp[30]"))
        ret=false
      end
    else
      pbSEPlay("GUI save choice")
    end
    @scene.pbEndScreen
    return ret
  end
end
  