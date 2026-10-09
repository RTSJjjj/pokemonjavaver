#===============================================================================
#  Modular Pause Menu
#    by Luka S.J.
# ----------------
#  Provides only features present in the default version of the Pokedex in
#  Essentials. Mean as a new cosmetic overhaul, adhering to the UI design
#  language of the Elite Battle System: The Next Generation
#
#  Enjoy the script, and make sure to give credit!
#  (DO NOT ALTER THE NAMES OF THE INDIVIDUAL SCRIPT SECTIONS OR YOU WILL BREAK
#   YOUR SYSTEM!)
#-------------------------------------------------------------------------------
#  Main module for handling each menu item/entry
#===============================================================================
module MenuHandlers
  # hash used to store the elements inside of the menu
  @@menuEntry = {}
  # hash used to store whether or not an element is unlocked
  @@available = {}
  # hash used to store the index of each element; for sorting
  @@indexes = {}
  @@index = 0
  # function to add a new element/entry to the menu.
  def self.addEntry(ref,name,icon,proc,conditional)
    @@menuEntry[ref] = [name,icon,proc]
    @@available[ref] = conditional
    @@indexes[ref] = @@index
    @@index += 1
  end
  # function to get the name of an element/entry
  def self.getName(ref)
    return @@menuEntry[ref][0]
  end
  # function to get the icon of an element/entry
  def self.getIcon(ref)
    icon = @@menuEntry[ref][1]
    icon = "menuBag_f" if $Trainer.female? && icon == "menuBag"
    return "Graphics/Pictures/MPM/#{icon}"
  end
  # function to get all the possible keys from the main hash
  def self.getKeys
    entries = Array.new(@@menuEntry.keys.length)
    for key in @@menuEntry.keys
      entries[@@indexes[key]] = key
    end
    return entries
  end
  # function used to invoke the stored code for each element/entry
  def self.runAction(ref,scene)
    @@menuEntry[ref][2].call(scene)
  end
  # function to check if the player has access to an element/entry
  def self.available?(ref)
    return @@available[ref].call
  end
  # function that lists all accessible menu elements/entries
  def self.elements?
    ent = self.getKeys
    items = 0
    for val in ent
      items += 1 if self.available?(val)
    end
    return items
  end
end
#-------------------------------------------------------------------------------
#  Main class used to handle the visuals
#-------------------------------------------------------------------------------
class PokemonPauseMenu_Scene
  attr_accessor :index
  attr_accessor :entries
  attr_accessor :endscene
  attr_accessor :close
  attr_accessor :hidden

  def initialize
    @baseColor = Color.new(248,248,248)
    @shadowColor = Color.new(88,88,88)
  end
  
  # retained for compatibility
  def pbShowInfo(text)
    @sprites["helpwindow"].resizeToFit(text,Graphics.height)
    @sprites["helpwindow"].text = text
    @sprites["helpwindow"].visible = true
    @helpstate = true
    pbBottomLeft(@sprites["helpwindow"])
  end
  # retained for compatibility
  def pbShowHelp(text)
    @sprites["helpwindow"].resizeToFit(text,Graphics.height)
    @sprites["helpwindow"].text = text
    @sprites["helpwindow"].visible = true
    @helpstate = true
    pbBottomLeft(@sprites["helpwindow"])
  end
  # main scene generation
  def pbStartScene
    pbSetViableDexes
    # sets the default index
    @index = $PokemonTemp.menuLastChoice.nil? ? 0 : $PokemonTemp.menuLastChoice
    @index = 0 if @index >= MenuHandlers.elements?
    @oldindex = 0
    @endscene = true
    @close = false
    @hidden = false
    # loads the visual parts of the
    @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z = 99999
    @sprites = {}
    # initializes the background graphic
    @bitmap = Graphics.snap_to_bitmap if !@bitmap
    @sprites["background"] = Sprite.new(@viewport)
    @sprites["background"].bitmap = @bitmap
    @sprites["background"].blur_sprite(3)
    @sprites["background"].bitmap.blt(0,0,pbBitmap("Graphics/Pictures/MPM/bg"),Rect.new(0,0,Graphics.width,Graphics.height))
    bmp = pbBitmap("Graphics/Pictures/MPM/scrollbar_bg")
    @sprites["background"].bitmap.blt(Graphics.width - 26,(Graphics.height - bmp.height)/2,bmp,Rect.new(0,0,bmp.width,bmp.height))
    # initializes the scrolling panorama
    @sprites["panorama"] = ScrollingSprite.new(@viewport)
    @sprites["panorama"].setBitmap("Graphics/Pictures/MPM/panorama")
    @sprites["panorama"].speed = 1
    # retained for compatibility
    @sprites["infowindow"] = Window_UnformattedTextPokemon.newWithSize("",0,0,32,32,@viewport)
    @sprites["infowindow"].visible = false
    @sprites["helpwindow"] = Window_UnformattedTextPokemon.newWithSize("",0,0,32,32,@viewport)
    @sprites["helpwindow"].visible = false
    # draw the contest crap
    @sprites["textOverlay"] = Sprite.new(@viewport)
    @sprites["textOverlay"].bitmap = Bitmap.new(@viewport.rect.width,@viewport.rect.height)
    @sprites["textOverlay"].end_x = 0
    @sprites["textOverlay"].x = -@viewport.rect.width
    @sprites["textOverlay"].z = @viewport.z + 1
    pbSetSystemFont(@sprites["textOverlay"].bitmap)
    bmp = pbBitmap("Graphics/Pictures/MPM/partyBar")
    content = []
    text = []
    if pbInSafari?
      content.push(_INTL("剩余步数: {1}/{2}",pbSafariState.steps, SAFARI_STEPS)) if SAFARI_STEPS > 0
      content.push(_INTL("狩猎球剩余:{1}",pbSafariState.ballcount))
    elsif pbInBugContest?
      if pbBugContestState.lastPokemon
        content.push(_INTL("已捕获: {1}",PBSpecies.getName(pbBugContestState.lastPokemon.species)))
        content.push(_INTL("等级: {1}",pbBugContestState.lastPokemon.level))
        content.push(_INTL("精灵球剩余: {1}",pbBugContestState.ballcount))
      else
        content.push("已捕获: 无")
      end
      content.push(_INTL("精灵球剩余: {1}",pbBugContestState.ballcount))
    end
    for i in 0...content.length
      text.push([content[i],16, 60 + i*50, 0, Color.new(255,255,255),Color.new(0,0,0,65)])
      @sprites["textOverlay"].bitmap.blt(-2,92 + i*50,bmp,Rect.new(0,0,bmp.width,bmp.height))
    end
    #--------------------------------------------------------
    # 日期时间
    now = Time.now
    text.push([
      _INTL("{1}年{2}月{3}日", now.year, now.mon, now.day) + " " +
      _ISPRINTF("{1:02d}:{2:02d}",now.hour, now.min) + " " +
      _INTL("{1}", PBDayNight.pbGetDayNightName),
      16, 2, 0, Color.new(255,255,255), Color.new(0, 0, 0, 65)
    ])
    # 在日期时间部分后添加
    splash_message = SPLASH_MESSAGE.sample
    text.push([
    _INTL("{1}", splash_message),
    286, 2,  0, Color.new(255, 216, 0), Color.new(216, 128, 0)
    ])
    # 难度
    if $game_switches[197]
      difficulty = "懒狗模式"
    else
      case $game_variables[100]
      when 2
        difficulty = "平均等级"
      when 1
        difficulty = "最高等级"
      else
        difficulty = "简单难度"
      end
    end
    text.push(
      [_INTL("模式：{1}", difficulty), 16, 28, 0,
        Color.new(255,255,255), Color.new(0, 0, 0, 65)]
    )
    #-----------------------------------------------------------------
    # 连锁显示（添加到难度显示下方）
    #-----------------------------------------------------------------
    if $Trainer && $Trainer.chainCatching
      chain_species = $Trainer.chainCatching.species
      chain_times = $Trainer.chainCatching.chain_times
      if chain_times > 1
        chain_text = _INTL("连锁：{1} {2}次", PBSpecies.getName(chain_species), chain_times)
        text.push(   
        [chain_text, 16+160, 28, 0,
        Color.new(255, 255, 100), Color.new(0, 0, 0, 65)]  # 使用金色文字突出显示
        )
        end 
         end
   #-----------------------------------------------------------------
    text.push(
      [_INTL("版本号：{1}", CURRENT_NAME), 16, Graphics.height-24, 0,
        Color.new(255, 255, 100), Color.new(0, 0, 0, 65)]
    )
    #--------------------------------------------------------
    pbDrawTextPositions(@sprites["textOverlay"].bitmap,text)
    #--------------------------------------------------------
    # 主角立绘
    @sprites["trainer"] = IconSprite.new(0,0,@viewport)
    @sprites["trainer"].zoom_x = 1.0
    @sprites["trainer"].zoom_y = 1.0
    @sprites["trainer"].x = 0
    @sprites["trainer"].y = Graphics.height - 340
    if $Trainer.female?
      @sprites["trainer"].setBitmap("Graphics/Pictures/MPM/intro_Girl1")
    else
      @sprites["trainer"].setBitmap("Graphics/Pictures/MPM/intro_Boy1")
    end
    #--------------------------------------------------------
    # initializes the scroll bar
    @sprites["scroll"] = Sprite.new(@viewport)
    # rendering elements on screen
    self.refresh
    self.update
    # memorizes the target opacities and sets them to 0
    @opacities = {}
    for key in @sprites.keys
      @opacities[key] = @sprites[key].opacity
      @sprites[key].opacity = 0
    end
  end

  def pbHideMenu
    # animations for closing the menu
    @sprites["textOverlay"].end_x = -@viewport.rect.width
    8.times do
      for key in @sprites.keys
        next if !@sprites[key] || @sprites[key].disposed?
        @sprites[key].opacity -= 32
      end
      @sprites["textOverlay"].x += (@sprites["textOverlay"].end_x - @sprites["textOverlay"].x)*0.2
      Graphics.update
    end
  end

  def pbShowMenu
    # animations for opening the menu
    @sprites["textOverlay"].end_x = 0
    8.times do
      for key in @sprites.keys
        next if !@sprites[key] || @sprites[key].disposed?
        @sprites[key].opacity += 32 if @sprites[key].opacity < @opacities[key]
      end
      @sprites["textOverlay"].x += (@sprites["textOverlay"].end_x - @sprites["textOverlay"].x)*0.4
      Graphics.update
    end
  end

  def refresh
    # index safety
    @index = MenuHandlers.elements? - 1 if @index >= MenuHandlers.elements?
    @oldindex = @index
    # disposes old items in the menu
    if @entries
      for i in 0...@entries.length
        @sprites["#{i}"].dispose if @sprites["#{i}"]
      end
    end
    # creates a new list of available items
    ent = MenuHandlers.getKeys
    @entries = []
    for val in ent
      @entries.push(val) if MenuHandlers.available?(val)
    end
    # draws individual item entries
    bmp = pbBitmap("Graphics/Pictures/MPM/sel")
    for i in 0...@entries.length
      key = @entries[i]
      @sprites["#{i}"] = Sprite.new(@viewport)
      @sprites["#{i}"].bitmap = Bitmap.new(bmp.width,bmp.height)
      pbSetSystemFont(@sprites["#{i}"].bitmap)
      @sprites["#{i}"].src_rect.set(0,0,bmp.width/2,bmp.height)
      @sprites["#{i}"].bitmap.blt(0,0,bmp,Rect.new(0,0,bmp.width,bmp.height))
      for j in 0...2
        opac = j == 0 ? 155 : 255
        icon = pbBitmap(MenuHandlers.getIcon(key))
        text = MenuHandlers.getName(key).clone
        text.gsub!("\\pn"){"#{$Trainer.name}"}
        text.gsub!("\\contest"){pbInSafari? ? "Quit" : "Quit Contest"}
        @sprites["#{i}"].bitmap.blt(8 + j*bmp.width/2,0,icon,Rect.new(0,0,64,72),opac)
        pbDrawOutlineText(@sprites["#{i}"].bitmap,66 + j*bmp.width/2,16,136,48,text,@baseColor,@shadowColor,1)
      end
      @sprites["#{i}"].x = Graphics.width - bmp.width/2 - 40
      @sprites["#{i}"].y = 40 + (bmp.height + 12)*i
      @sprites["#{i}"].opacity = 128
    end
    # configures the scroll bar
    n = (@entries.length < 4 ? 1 : @entries.length - 3)
    height = 204/n
    height += 204 - (height*n)
    height += 16
    @sprites["scroll"].bitmap = Bitmap.new(16,height)
    bmp = pbBitmap("Graphics/Pictures/MPM/scrollbar_kn")
    @sprites["scroll"].bitmap.blt(0,0,bmp,Rect.new(0,0,16,6))
    @sprites["scroll"].bitmap.stretch_blt(Rect.new(0,6,16,height-14),bmp,Rect.new(0,6,16,1))
    @sprites["scroll"].bitmap.blt(0,height-8,bmp,Rect.new(0,8,16,8))
    @sprites["scroll"].x = Graphics.width - 28
    @sprites["scroll"].y = (Graphics.height - 204)/2
    @sprites["scroll"].end_y = (Graphics.height - 204)/2
  end

  def update
    # scrolling background image
    @sprites["panorama"].update
    # calculations for updating the scrollbar position
    k = (@entries.length < 4 ? 0 : @index - 3)
    k = 0 if k < 0
    n = (@entries.length < 4 ? 1 : @entries.length - 3)
    height = 204/n
    @sprites["scroll"].end_y = (Graphics.height-204)/2 + height*k
    @sprites["scroll"].y += (@sprites["scroll"].end_y - @sprites["scroll"].y)*0.2
    # updates for each element/entry in the menu
    for i in 0...@entries.length
      j = @entries.length < 4 ? 0 : (@index - 3)
      j = 0 if j < 0
      y = (-j)*(@sprites["#{i}"].src_rect.height + 12) + 49 + i*(@sprites["#{i}"].src_rect.height + 12)
      @sprites["#{i}"].y -= (@sprites["#{i}"].y - y)*0.1
      @sprites["#{i}"].src_rect.x = @sprites["#{i}"].src_rect.width*(@index == i ? 1 : 0)
      @sprites["#{i}"].x += 2 if @sprites["#{i}"].x < Graphics.width - @sprites["#{i}"].src_rect.width - 40
      if i.between?(j,j+3)
        @sprites["#{i}"].opacity += 15 if @sprites["#{i}"].opacity < 255
      else
        @sprites["#{i}"].opacity -= 15 if @sprites["#{i}"].opacity > 128
      end
      if @index == i
        @sprites["#{i}"].tone.gray -= 51 if @sprites["#{i}"].tone.gray > 0
      else
        @sprites["#{i}"].tone.gray += 51 if @sprites["#{i}"].tone.gray < 255
      end
    end
    # sets the index
    if @oldindex != @index
      @sprites["#{@index}"].x -= 6
      @oldindex = @index
    end
  end

  def pbEndScene
    # disposes the sprite hash
    pbSEPlay("GUI menu close")
    pbHideMenu
    pbDisposeSpriteHash(@sprites)
    @viewport.dispose
  end

  def pbRefresh
  end
end
#-------------------------------------------------------------------------------
#  Main class used to handle the logic of the pause menu
#-------------------------------------------------------------------------------
class PokemonPauseMenu
  def initialize(scene)
    @scene = scene
  end

  def pbShowMenu
    #@scene.pbRefresh
    @scene.pbShowMenu
  end

  def pbStartPokemonMenu
    # loads up the scene
    pbSEPlay("GUI menu open")
    @scene.pbStartScene
    @scene.pbShowMenu
    loop do
      # main loop
      Graphics.update
      Input.update
      @scene.update
      if Input.repeat?(Input::DOWN)
        @scene.index += 1
        @scene.index = 0 if @scene.index > @scene.entries.length - 1
        $PokemonTemp.menuLastChoice = @scene.index
        pbSEPlay("SE_Select1", 75)
      elsif Input.repeat?(Input::UP)
        @scene.index -= 1
        @scene.index = @scene.entries.length - 1 if @scene.index < 0
        $PokemonTemp.menuLastChoice = @scene.index
        pbSEPlay("SE_Select1", 75)
      elsif Input.trigger?(Input::C)
        pbPlayDecisionSE()
        MenuHandlers.runAction(@scene.entries[@scene.index],@scene)
      end
      break if @scene.close || Input.trigger?(Input::B)
    end
    # used to dispose of the scene
    @scene.pbEndScene if @scene.endscene
  end
end
