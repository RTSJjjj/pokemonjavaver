class PokemonLoad_Scene

  def pbStartScene(commands,showContinue,trainer,framecount,mapid)
    @commands = commands
    @sprites = {}
    @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z = 99998
    addBackgroundOrColoredPlane(@sprites,"background","loadbg",Color.new(248,248,248),@viewport)

    @sprites["tips"] = BitmapSprite.new(Graphics.width, 20, @viewport)
    pbSetSmallFont(@sprites["tips"].bitmap)
    font_color = Color.new(248,248,248, 128)
    shadow_color = Color.new(88,88,88, 128)
    textpos = [
      [_INTL("[←]/[→]:切换存档插槽"), 0, 0, 0, font_color, shadow_color]
    ]
    pbDrawTextPositions(@sprites["tips"].bitmap, textpos)
    @sprites["tips"].visible = false

    y = 16*2
    for i in 0...commands.length
      @sprites["panel#{i}"] = PokemonLoadPanel.new(i,commands[i],
         (showContinue) ? (i==0) : false,trainer,framecount,mapid,@viewport)
      @sprites["panel#{i}"].x = 24*2 + 32 + 64
      @sprites["panel#{i}"].y = y
      @sprites["panel#{i}"].pbRefresh
      y += (showContinue && i==0) ? 112*2 : 24*2
    end
    @sprites["cmdwindow"] = Window_CommandPokemon.new([])
    @sprites["cmdwindow"].viewport = @viewport
    @sprites["cmdwindow"].visible  = false
  end

  def pbSetParty(trainer)
    return if !trainer || !trainer.party
    meta = pbGetMetadata(0,MetadataPlayerA+trainer.metaID)
    if meta
      filename = pbGetPlayerCharset(meta,1,trainer,true)
      @sprites["player"] = TrainerWalkingCharSprite.new(filename,@viewport)
      charwidth  = @sprites["player"].bitmap.width
      charheight = @sprites["player"].bitmap.height
      @sprites["player"].x        = 56*2-charwidth/8 + 32 + 64
      @sprites["player"].y        = 32*2-charheight/8
      @sprites["player"].src_rect = Rect.new(0,0,charwidth/4,charheight/4)
    end
    for i in 0...trainer.party.length
      @sprites["party#{i}"] = PokemonIconSprite.new(trainer.party[i],@viewport)
      @sprites["party#{i}"].setOffset(PictureOrigin::Center)
      @sprites["party#{i}"].x = (46+32*i)*2 + 32 + 64
      @sprites["party#{i}"].y = 110*2
      @sprites["party#{i}"].z = 99998
    end
  end
  
  def pbChoose(commands, continue_idx)
    @sprites["cmdwindow"].commands = commands
    loop do
      Graphics.update
      Input.update
      @sprites["tips"].visible = (@sprites["cmdwindow"].index == continue_idx)
      pbUpdate
      if Input.trigger?(Input::C)
        return @sprites["cmdwindow"].index
      elsif @sprites["cmdwindow"].index == continue_idx
        if Input.trigger?(Input::LEFT)
          return -3
        elsif Input.trigger?(Input::RIGHT)
          return -2
        end
      end
    end
  end
  
end

class PokemonLoadScreen
  def initialize(scene)
    @scene = scene
    @selected_file = SaveData.get_newest_slot
    $miniMap.dispose if $miniMap
    $miniMap = nil
  end

  def pbStartLoadScreen
    $PokemonTemp   = PokemonTemp.new
    $game_temp     = Game_Temp.new
    $game_system   = Game_System.new
    $PokemonSystem = PokemonSystem.new if !$PokemonSystem
    data_system = pbLoadRxData("Data/System")
    mapfile = ($RPGVX) ? sprintf("Data/Map%03d.rvdata",data_system.start_map_id) :
                         sprintf("Data/Map%03d.rxdata",data_system.start_map_id)
    if data_system.start_map_id==0 || !pbRgssExists?(mapfile)
      pbMessage(_INTL("No starting position was set in the map editor.\1"))
      pbMessage(_INTL("The game cannot continue."))
      @scene.pbEndScene
      $scene = nil
      return
    end
    pbLoadBattleAnimations
    save_file_list = SaveData::AUTO_SLOTS + SaveData::MANUAL_SLOTS
    first_time = true
    loop do # Outer loop is used for switching save files
      if File.exists?(SaveData.get_full_path(@selected_file))
        savefile = SaveData.get_full_path(@selected_file)
      else
        savefile = "Save/Game.rxdata"
      end
      commands = []
      cmdContinue    = -1
      cmdNewGame     = -1
      cmdSaveFold    = -1
      cmdOption      = -1
      cmdDebug       = -1
      cmdQuit        = -1
      if safeExists?(savefile)
        trainer      = nil
        framecount   = 0
        mapid        = 0
        haveBackup   = false
        showContinue = false
        begin
          trainer, framecount, $game_system, $PokemonSystem, mapid = pbTryLoadFile(savefile)
          showContinue = true
        rescue
          if safeExists?(savefile+".bak")
            begin
              trainer, framecount, $game_system, $PokemonSystem, mapid = pbTryLoadFile(savefile+".bak")
              haveBackup   = true
              showContinue = true
            rescue
            end
          end
          if haveBackup
            pbMessage(_INTL("存档文件已损坏。将会读取之前备份的存档。"))
          else
            pbMessage(_INTL("存档文件已损坏, 或者不兼容当前游戏版本。"))
            if !pbConfirmMessageSerious(_INTL("你想要将它删除并重新开始吗？"))
              $scene = nil
              return
            end
            begin; File.delete(savefile); rescue; end
            begin; File.delete(savefile+".bak"); rescue; end
            $game_system   = Game_System.new
            $PokemonSystem = PokemonSystem.new if !$PokemonSystem
            pbMessage(_INTL("存档文件已删除。"))
          end
        end
        if showContinue
          if !haveBackup
            begin; File.delete(savefile+".bak"); rescue; end
          end
        end
        if showContinue
          if @selected_file
            if @selected_file == "Game"
              selected_title = "←自动保存→"
            else
              selected_title = "←" + @selected_file.gsub("Game","存档") + "→"
            end
          else
            selected_title = "继续游戏"
          end
          commands[cmdContinue = commands.length]    = _INTL("#{selected_title}")
        end
        commands[cmdNewGame = commands.length]     = _INTL("新的冒险")
      else
        commands[cmdNewGame = commands.length]     = _INTL("新的冒险")
      end
      commands[cmdSaveFold = commands.length]      = _INTL("打开存档文件夹") if os_family=="windows"
      commands[cmdOption = commands.length]        = _INTL("选项")
      commands[cmdDebug = commands.length]         = _INTL("调试") if $DEBUG
      commands[cmdQuit = commands.length]          = _INTL("退出游戏")
      cmdLeft = -3
      cmdRight = -2
      @scene.pbStartScene(commands,showContinue,trainer,framecount,mapid)
      @scene.pbSetParty(trainer) if showContinue
      if first_time
        @scene.pbStartScene2
        first_time = false
      else
        @scene.pbUpdate
      end
      loop do
        command = @scene.pbChoose(commands, cmdContinue)
        pbPlayDecisionSE if command != cmdQuit
        case command
        when cmdContinue
          unless safeExists?(savefile)
            pbPlayBuzzerSE
            next
          end
          @scene.pbEndScene
          metadata = nil
          File.open(savefile) { |f|
            $Trainer             = Marshal.load(f)
            #Marshal.load(f)   # Trainer already loaded
            #$Trainer             = trainer
            Graphics.frame_count = Marshal.load(f)
            $game_system         = Marshal.load(f)
            Marshal.load(f)   # PokemonSystem already loaded
            Marshal.load(f)   # Current map id no longer needed
            $game_switches       = Marshal.load(f)
            $game_variables      = Marshal.load(f)
            $game_self_switches  = Marshal.load(f)
            $game_screen         = Marshal.load(f)
            $MapFactory          = Marshal.load(f)
            $game_map            = $MapFactory.map
            $game_player         = Marshal.load(f)
            $PokemonGlobal       = Marshal.load(f)
            metadata             = Marshal.load(f)
            $PokemonBag          = Marshal.load(f)
            $PokemonStorage      = Marshal.load(f)
            $PokemonStorage.expand
            $SaveVersion         = Marshal.load(f) unless f.eof?
            if !$DEBUG
              if !$PokemonGlobal.currentVersion || $PokemonGlobal.currentVersion < MIN_VERSION
                pbMessage(_INTL("存档版本低于当前游戏支持的最低版本，\n不支持串档升级。({1})",
                                $PokemonGlobal.currentVersion))
                pbStartLoadScreen
                return
              elsif $PokemonGlobal.currentVersion > CURRENT_VERSION
                pbMessage(_INTL("存档版本高于当前游戏版本，\n不支持串档降级。({1})",
                                $PokemonGlobal.currentVersion))
                pbStartLoadScreen
                return
              end
            end
            $PokemonGlobal.currentVersion = CURRENT_VERSION
            pbRefreshResizeFactor   # To fix Game_Screen pictures
            magicNumberMatches = false
            if $data_system.respond_to?("magic_number")
              magicNumberMatches = ($game_system.magic_number==$data_system.magic_number)
            else
              magicNumberMatches = ($game_system.magic_number==$data_system.version_id)
            end
            if !magicNumberMatches || $PokemonGlobal.safesave
              if pbMapInterpreterRunning?
                pbMapInterpreter.setup(nil,0)
              end
              begin
                $MapFactory.setup($game_map.map_id)   # calls setMapChanged
              rescue Errno::ENOENT
                if $DEBUG
                  pbMessage(_INTL("未找到地图{1}。",$game_map.map_id))
                  map = pbWarpToMap
                  if map
                    $MapFactory.setup(map[0])
                    $game_player.moveto(map[1],map[2])
                  else
                    $game_map = nil
                    $scene = nil
                    return
                  end
                else
                  $game_map = nil
                  $scene = nil
                  pbMessage(_INTL("未找到地图，游戏无法继续。"))
                end
              end
              $game_player.center($game_player.x, $game_player.y)
            else
              $MapFactory.setMapChanged($game_map.map_id)
            end
          }
          if !$game_map.events   # Map wasn't set up
            $game_map = nil
            $scene = nil
            pbMessage(_INTL("地图损坏了，游戏无法继续。"))
            return
          end
          $PokemonMap = metadata
          $PokemonEncounters = PokemonEncounters.new
          $PokemonEncounters.setup($game_map.map_id)
          pbAutoplayOnSave
          $game_map.update
          $PokemonMap.updateMap
          $scene = Scene_Map.new
          $miniMap = ESMiniMap.new if !$miniMap
          #继续游戏，更新数据
          #$game_variables[234]+=1  if $PokemonBag.pbHasItem?(:STRANGEBADGE1)
          if $game_variables[234]>=1
            exit if !passCheck("10692778","",0,8,false)
          end
          return
        when cmdNewGame
          pbPlayDecisionSE
          @scene.pbEndScene
          fuckyou if !$game_screen
          if $game_map && $game_map.events
            for event in $game_map.events.values
              event.clear_starting
            end
          end
          $game_temp.common_event_id = 0 if $game_temp
          $scene               = Scene_Map.new
          Graphics.frame_count = 0
          $game_system         = Game_System.new
          $game_switches       = Game_Switches.new
          $game_variables      = Game_Variables.new
          $game_self_switches  = Game_SelfSwitches.new
          $game_screen         = Game_Screen.new
          $game_player         = Game_Player.new
          $PokemonMap          = PokemonMapMetadata.new
          $PokemonGlobal       = PokemonGlobalMetadata.new
          $PokemonStorage      = PokemonStorage.new
          $PokemonEncounters   = PokemonEncounters.new
          $PokemonTemp.begunNewGame = true
          pbRefreshResizeFactor   # To fix Game_Screen pictures
          $data_system         = pbLoadRxData("Data/System")
          $MapFactory          = PokemonMapFactory.new($data_system.start_map_id)   # calls setMapChanged
          $game_player.moveto($data_system.start_x, $data_system.start_y)
          $game_player.refresh
          $game_map.autoplay
          $game_map.update
          $PokemonGlobal.currentVersion = CURRENT_VERSION
          return
        when cmdSaveFold
          if !File.exist?("Save")
            Dir.mkdir("Save") rescue return nil
          end
          system('explorer save')
        when cmdOption
          pbFadeOutIn {
            scene = PokemonOption_Scene.new
            screen = PokemonOptionScreen.new(scene)
            screen.pbStartScreen(true)
          }
        when cmdDebug
          pbFadeOutIn { pbDebugMenu(false) }
        when cmdQuit
          pbPlayCloseMenuSE
          @scene.pbEndScene
          $scene = nil
          return
        when cmdLeft
          @scene.pbCloseScene
          @selected_file = SaveData.get_prev_slot(save_file_list, @selected_file)
          break # to outer loop
        when cmdRight
          @scene.pbCloseScene
          @selected_file = SaveData.get_next_slot(save_file_list, @selected_file)
          break # to outer loop
        else
          pbPlayBuzzerSE
        end
      end
    end
  end
end

class PokemonLoadPanel < SpriteWrapper
  
  SECONDS_PER_MINUTE = 60
  SECONDS_PER_HOUR   = SECONDS_PER_MINUTE * 60
  SECONDS_PER_DAY    = SECONDS_PER_HOUR * 24
  SECONDS_PER_MONTH  = SECONDS_PER_DAY * 30
  SECONDS_PER_YEAR   = SECONDS_PER_DAY * 365
  
  def refresh
    return if @refreshing
    return if disposed?
    @refreshing = true
    if !self.bitmap || self.bitmap.disposed?
      self.bitmap = BitmapWrapper.new(@bgbitmap.width,111*2)
      pbSetSystemFont(self.bitmap)
    end
    if @refreshBitmap
      @refreshBitmap = false
      self.bitmap.clear if self.bitmap
      if @isContinue
        self.bitmap.blt(0,0,@bgbitmap.bitmap,Rect.new(0,(@selected) ? 111*2 : 0,@bgbitmap.width,111*2))
      else
        self.bitmap.blt(0,0,@bgbitmap.bitmap,Rect.new(0,111*2*2+((@selected) ? 23*2 : 0),@bgbitmap.width,23*2))
      end
      textpos = []
      line_y=[5,32,48,64]
      if @isContinue
        textpos.push([@title,14*2,line_y[0]*2,0,TEXTCOLOR,TEXTSHADOWCOLOR])
        textpos.push([_INTL("徽章："),134*2,line_y[1]*2,0,TEXTCOLOR,TEXTSHADOWCOLOR])
        textpos.push([@trainer.numbadges.to_s,194*2,line_y[1]*2,1,TEXTCOLOR,TEXTSHADOWCOLOR])
        textpos.push([_INTL("图鉴："),134*2,line_y[2]*2,0,TEXTCOLOR,TEXTSHADOWCOLOR])
        textpos.push([@trainer.pokedexSeen.to_s,194*2,line_y[2]*2,1,TEXTCOLOR,TEXTSHADOWCOLOR])
        textpos.push([_INTL("时长："),14*2,line_y[2]*2,0,TEXTCOLOR,TEXTSHADOWCOLOR])
        hour = @totalsec / 60 / 60
        min  = @totalsec / 60 % 60
        if hour>0
          textpos.push([_INTL("{1}小时{2}分钟",hour,min),124*2,line_y[2]*2,1,TEXTCOLOR,TEXTSHADOWCOLOR])
        else
          textpos.push([_INTL("{1}分钟",min),124*2,line_y[2]*2,1,TEXTCOLOR,TEXTSHADOWCOLOR])
        end
        #======================================================================
        textpos.push([_INTL("保存时间："),14*2,line_y[3]*2,0,TEXTCOLOR,TEXTSHADOWCOLOR])
        @trainer.last_saved = 0 if !@trainer.last_saved
        save_time = Time.now.to_i - @trainer.last_saved
        if save_time < SECONDS_PER_MINUTE
          last_saved = "刚刚"
        elsif save_time < SECONDS_PER_HOUR
          last_saved = (save_time / SECONDS_PER_MINUTE).to_s + "分钟前"
        elsif save_time < SECONDS_PER_DAY
          last_saved = (save_time / SECONDS_PER_HOUR).to_s + "小时前"
        elsif save_time < SECONDS_PER_MONTH
          last_saved = (save_time / SECONDS_PER_DAY).to_s + "天前"
        elsif save_time < SECONDS_PER_YEAR
          last_saved = (save_time / SECONDS_PER_MONTH).to_s + "个月前"
        elsif save_time < SECONDS_PER_YEAR * 2
          last_saved = (save_time / SECONDS_PER_YEAR).to_s + "年前"
        else
          last_saved = "很久之前"
        end
        textpos.push([last_saved,124*2,line_y[3]*2,1,TEXTCOLOR,TEXTSHADOWCOLOR])
        #======================================================================
        if @trainer.male?
          textpos.push([@trainer.name,56*2,28*2,0,MALETEXTCOLOR,MALETEXTSHADOWCOLOR])
        elsif @trainer.female?
          textpos.push([@trainer.name,56*2,28*2,0,FEMALETEXTCOLOR,FEMALETEXTSHADOWCOLOR])
        else
          textpos.push([@trainer.name,56*2,28*2,0,TEXTCOLOR,TEXTSHADOWCOLOR])
        end
        mapname = pbGetMapNameFromId(@mapid)
        mapname.gsub!(/\\PN/,@trainer.name)
        textpos.push([mapname,194*2,line_y[0]*2,1,TEXTCOLOR,TEXTSHADOWCOLOR])
        
      else
        textpos.push([@title,16*2,line_y[0]*2,0,TEXTCOLOR,TEXTSHADOWCOLOR])
      end
      pbDrawTextPositions(self.bitmap,textpos)
    end
    @refreshing = false
  end
  
end