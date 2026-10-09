#===============================================================================
#
#===============================================================================
module SaveData
  # You can rename these slots or change the amount of them
  # They change the actual save file names though, so it would take some extra work to use the translation system on them.
  AUTO_SLOTS = [
    'Game'
  ]
  MANUAL_SLOTS = [
    'Game1',
    'Game2',
    'Game3',
    'Game4',
    'Game5',
    'Game6',
    'Game7',
    'Game8'
  ]
  
  SAVE_DIR = "./Save"

  def self.each_slot
    (AUTO_SLOTS + MANUAL_SLOTS).each { |f| yield f }
  end

  def self.get_full_path(file)
    return "#{SAVE_DIR}/#{file}.rxdata"
  end

  # Given a list of save file names and a file name in it, return the next file after it which exists
  # If no other file exists, will just return the same file again
  def self.get_next_slot(file_list, file)
    old_index = file_list.index(file)
    ordered_list = file_list.rotate(old_index + 1)
    ordered_list.each do |f|
      return f if File.file?(self.get_full_path(f))
    end
    # should never reach here since the original file should always exist
    return file
  end
  
  def self.get_prev_slot(file_list, file)
    return self.get_next_slot(file_list.reverse, file)
  end

  def self.get_newest_slot
    newest_time = 1
    newest_slot = "Game"
    self.each_slot do |file_slot|
      full_path = self.get_full_path(file_slot)
      next if !File.file?(full_path)
      trainer = self.read_trainer_from_file(full_path)
      save_time = trainer.last_saved || 1
      if save_time > newest_time
        newest_time = save_time
        newest_slot = file_slot
      end
    end
    return newest_slot
  end
  
  def self.read_trainer_from_file(savefile)
    trainer       = nil
    File.open(savefile) { |f|
      trainer       = Marshal.load(f)
    }
    raise "Corrupted file" if !trainer.is_a?(PokeBattle_Trainer)
    return trainer
  end
  
end