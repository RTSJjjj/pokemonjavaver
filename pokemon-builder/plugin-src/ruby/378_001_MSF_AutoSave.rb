# 每200步自动存档
Events.onStepTaken += proc {
  $Trainer.autosave_steps = 0 if !$Trainer.autosave_steps
  next if $PokemonGlobal.sliding
  # 禁用自动存档的地图id
  ban_maps = [1, 119, 426]
  next if ban_maps.include?($game_map.map_id)
  $Trainer.autosave_steps += 1
  if $Trainer.autosave_steps >= 200
    $Trainer.autosave_steps = 0
    pbSave(nil,true)
  end
}

class PokeBattle_Trainer
  attr_accessor :save_slot
  attr_accessor :autosave_steps
  attr_accessor :last_saved
end