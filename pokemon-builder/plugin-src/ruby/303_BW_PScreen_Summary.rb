#===============================================================================
#  Summary Screen BW Style - ZUD Patch
#  for Pokémon Essentials version 18.x
#
#===============================================================================
#
# Instructions: Put this script above Main. Download the file BW Summary.rar and
# extract the files in your project main folder.
#
#===============================================================================
#
#  Modified by DeepBlue PacificWaves
# 
#   Scrolling Background inspired by Mr. Gela's HGSS Trainer Card Scene
#
#  IV Ratings on Summary Screen
#    Adapted from Lucidious89's IV star script by Tommaniacal 
#
#  Stat Screen Upgrade (EVs and IVs in Summary) 
#    By Weibrot, Kobi2604 and dirkriptide
#      Converted to BW Summary Pack by DeepBlue PacificWaves
#
#   Special thanks to Shashu-Greninja, that help with the implementation of the 
#   ZUD Compatibility Patch
#
#
#  Graphics Ripped by DeepBlue PacificWaves 
#    Font Ripped by Ploaj
#
#
#  If used, please give credits. For more information on how to credit, look 
#  for the original post on Relic Castle or PokéCommunity.
#  
#===============================================================================

PluginManager.register({
  :name => "Summary BW Style",
  :version => "1.3",
  :credits => ["DeepBlue PacificWaves", "Tommaniacal", "Lucidious89", "Weibrot", "Kobi2604", "Dirkriptide", "Ploaj", "Mr. Gela","Shashu-Greninja"],
})

#===============================================================================
class MoveSelectionSprite < SpriteWrapper
  attr_reader :preselected
  attr_reader :index
  def initialize(viewport=nil,fifthmove=false)
    super(viewport)
    # Sets the Move Selection Cursor
    @movesel = AnimatedBitmap.new("Graphics/Pictures/Summary/cursor_move")
    @frame = 0
    @index = 0
    @fifthmove = fifthmove
    @preselected = false
    @updating = false
    refresh
  end
  def dispose
    @movesel.dispose
    super
  end
  def index=(value)
    @index = value
    refresh
  end
  def preselected=(value)
    @preselected = value
    refresh
  end
  def refresh
    w = @movesel.width
    h = @movesel.height/2
    self.x = 286
    # Changed the position of the Move Select cursor
    self.y = 91+(self.index*64)
    self.y -= 76 if @fifthmove
    self.y += 20 if @fifthmove && self.index==4
    self.bitmap = @movesel.bitmap
    if self.preselected
      self.src_rect.set(0,h,w,h)
    else
      self.src_rect.set(0,0,w,h)
    end
  end
  def update
    @updating = true
    super
    @movesel.update
    @updating = false
    refresh
  end
end
class RibbonSelectionSprite < MoveSelectionSprite
  def initialize(viewport=nil)
    super(viewport)
    # Sets the Ribbon Selection Cursor
    @movesel = AnimatedBitmap.new("Graphics/Pictures/Summary/cursor_ribbon")
    @frame = 0
    @index = 0
    @preselected = false
    @updating = false
    @spriteVisible = true
    refresh
  end
  def visible=(value)
    super
    @spriteVisible = value if !@updating
  end
  def refresh
    w = @movesel.width
    h = @movesel.height/2
 # Changed the position of the Ribbon Select cursor
    self.x = 0+(self.index%4)*68
    self.y = 72+((self.index/4).floor*68)
    self.bitmap = @movesel.bitmap
    if self.preselected
      self.src_rect.set(0,h,w,h)
    else
      self.src_rect.set(0,0,w,h)
    end
  end
  def update
    @updating = true
    super
    self.visible = @spriteVisible && @index>=0 && @index<12
    @movesel.update
    @updating = false
    refresh
  end
end
class PokemonSummary_Scene
        
    def wait(frames)  
    frames.times do  
    Graphics.update  
    end  
  end  
  
  def pbUpdate
    pbUpdateSpriteHash(@sprites)
    # Sets the Moving Background
        if @sprites["background"]
      @sprites["background"].ox-= -1
      @sprites["background"].oy-= -1
    end
  end
  def pbStartScene(party,partyindex,inbattle=false)
    @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z = 99999
    @party      = party
    @partyindex = partyindex
    @pokemon    = @party[@partyindex]
    @inbattle   = inbattle
    @page = 1
    @typebitmap    = AnimatedBitmap.new(_INTL("Graphics/Pictures/types"))
    @markingbitmap = AnimatedBitmap.new("Graphics/Pictures/Summary/markings")
    @sprites = {}
    # Sets the Summary Background  
    # Background glitch fixed by Shashu-Greninja  
    @sprites["bg_overlay"] = IconSprite.new(0,0,@viewport)  
  addBackgroundPlane(@sprites,"background","Summary/background",@viewport)  
    # Sets the Moving Background Loop  
    @sprites["background"].ox+=6  
    @sprites["background"].oy-=36
    # Sets the Summary Overlays
    @sprites["menuoverlay"] = IconSprite.new(0,0,@viewport) 
    @sprites["pokemon"] = PokemonSprite.new(@viewport)
    @sprites["pokemon"].setOffset(PictureOrigin::Center)
    # Changed the position of Pokémon Battler
    @sprites["pokemon"].x = 460+64
    @sprites["pokemon"].y = 208+32
    @sprites["pokemon"].setPokemonBitmap(@pokemon)
    @sprites["pokeicon"] = PokemonIconSprite.new(@pokemon,@viewport)
    @sprites["pokeicon"].setOffset(PictureOrigin::Center)
    # Changed the position of Pokémon Icon
    @sprites["pokeicon"].x       = 46
    @sprites["pokeicon"].y       = 92
    @sprites["pokeicon"].visible = false
    # Changed the position of the held Item icon
    @sprites["itemicon"] = ItemIconSprite.new(552+96,360+64,@pokemon.item,@viewport)
    @sprites["itemicon"].blankzero = true
    @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
    pbSetSystemFont(@sprites["overlay"].bitmap)
    @sprites["movepresel"] = MoveSelectionSprite.new(@viewport)
    @sprites["movepresel"].visible     = false
    @sprites["movepresel"].preselected = true
    @sprites["movesel"] = MoveSelectionSprite.new(@viewport)
    @sprites["movesel"].visible = false
    # Draws the Ribbon Selection Cursor
    @sprites["ribbonpresel"] = RibbonSelectionSprite.new(@viewport)
    @sprites["ribbonpresel"].visible     = false
    @sprites["ribbonpresel"].preselected = true
    @sprites["ribbonsel"] = RibbonSelectionSprite.new(@viewport)
    @sprites["ribbonsel"].visible = false
    # Sets the Up Arrow in Ribbons Page 
    @sprites["uparrow"] = AnimatedSprite.new("Graphics/Pictures/uparrow",8,28,40,2,@viewport)
    # Draws the Up Arrow in Ribbons Page 
    @sprites["uparrow"].x = 260
    @sprites["uparrow"].y = 56
    @sprites["uparrow"].play
    @sprites["uparrow"].visible = false
    # Sets the Down Arrow in Ribbons Page
    @sprites["downarrow"] = AnimatedSprite.new("Graphics/Pictures/downarrow",8,28,40,2,@viewport)
    # Draws the Up Arrow in Ribbons Page 
    @sprites["downarrow"].x = 260
    @sprites["downarrow"].y = 260
    @sprites["downarrow"].play
    @sprites["downarrow"].visible = false
    # Sets the Marking Overlay
    @sprites["markingbg"] = IconSprite.new(260,88,@viewport)
    @sprites["markingbg"].setBitmap("Graphics/Pictures/Summary/overlay_marking")
    @sprites["markingbg"].visible = false
    @sprites["markingoverlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
    @sprites["markingoverlay"].visible = false
    pbSetSystemFont(@sprites["markingoverlay"].bitmap)
    # Sets the Marking Selector
    @sprites["markingsel"] = IconSprite.new(0,0,@viewport)
    @sprites["markingsel"].setBitmap("Graphics/Pictures/Summary/cursor_marking")
    @sprites["markingsel"].src_rect.height = @sprites["markingsel"].bitmap.height/2
    @sprites["markingsel"].visible = false
    @sprites["messagebox"] = Window_AdvancedTextPokemon.new("")
    @sprites["messagebox"].viewport       = @viewport
    @sprites["messagebox"].visible        = false
    @sprites["messagebox"].letterbyletter = true
    pbBottomLeftLines(@sprites["messagebox"],2)
    drawPage(@page)
    pbFadeInAndShow(@sprites) { pbUpdate }
  end
      # Sets the Forget Move Screen
  def pbStartForgetScene(party,partyindex,moveToLearn)
    @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z = 99999
    @party      = party
    @partyindex = partyindex
    @pokemon    = @party[@partyindex]
    @page = 4
    @typebitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/types"))
    @sprites = {}
    # Sets the Summary Background  
    # Background glitch fixed by Shashu-Greninja  
    @sprites["bg_overlay"] = IconSprite.new(0,0,@viewport)  
  addBackgroundPlane(@sprites,"background","Summary/background",@viewport)  
    # Sets the Moving Background Loop  
    @sprites["background"].ox+=6  
    @sprites["background"].oy-=36
    # Sets the Summary Overlays
    @sprites["menuoverlay"] = IconSprite.new(0,0,@viewport) 
    @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
    pbSetSystemFont(@sprites["overlay"].bitmap)
    @sprites["pokeicon"] = PokemonIconSprite.new(@pokemon,@viewport)
    @sprites["pokeicon"].setOffset(PictureOrigin::Center)
    # Sets the Pokémon Icon on the scene
    @sprites["pokeicon"].x       = 46
    @sprites["pokeicon"].y       = 92
    @sprites["movesel"] = MoveSelectionSprite.new(@viewport,moveToLearn>0)
    @sprites["movesel"].visible = false
    @sprites["movesel"].visible = true
    @sprites["movesel"].index   = 0
    drawSelectedMove(moveToLearn,@pokemon.moves[0].id)
    pbFadeInAndShow(@sprites)
  end
  def pbEndScene
    pbFadeOutAndHide(@sprites) { pbUpdate }
    pbDisposeSpriteHash(@sprites)
    @typebitmap.dispose
    @markingbitmap.dispose if @markingbitmap
    @viewport.dispose
  end
  def pbDisplay(text)
    @sprites["messagebox"].text = text
    @sprites["messagebox"].visible = true
    pbPlayDecisionSE()
    loop do
      Graphics.update
      Input.update
      pbUpdate
      if @sprites["messagebox"].busy?
        if Input.trigger?(Input::C)
          pbPlayDecisionSE() if @sprites["messagebox"].pausing?
          @sprites["messagebox"].resume
        end
      elsif Input.trigger?(Input::C) || Input.trigger?(Input::B)
        break
      end
    end
    @sprites["messagebox"].visible = false
  end
  def pbConfirm(text)
    ret = -1
    @sprites["messagebox"].text    = text
    @sprites["messagebox"].visible = true
    using(cmdwindow = Window_CommandPokemon.new([_INTL("是"),_INTL("否")])) {
      cmdwindow.z       = @viewport.z+1
      cmdwindow.visible = false
      pbBottomRight(cmdwindow)
      cmdwindow.y -= @sprites["messagebox"].height
      loop do
        Graphics.update
        Input.update
        cmdwindow.visible = true if !@sprites["messagebox"].busy?
        cmdwindow.update
        pbUpdate
        if !@sprites["messagebox"].busy?
          if Input.trigger?(Input::B)
            ret = false
            break
          elsif Input.trigger?(Input::C) && @sprites["messagebox"].resume
            ret = (cmdwindow.index==0)
            break
          end
        end
      end
    }
    @sprites["messagebox"].visible = false
    return ret
  end
  def pbShowCommands(commands,index=0)
    ret = -1
    using(cmdwindow = Window_CommandPokemon.new(commands)) {
       cmdwindow.z = @viewport.z+1
       cmdwindow.index = index
       pbBottomRight(cmdwindow)
       loop do
         Graphics.update
         Input.update
         cmdwindow.update
         pbUpdate
         if Input.trigger?(Input::B)
           pbPlayCancelSE
           ret = -1
           break
         elsif Input.trigger?(Input::C)
           pbPlayDecisionSE
           ret = cmdwindow.index
           break
         end
       end
    }
    return ret
  end
  def drawMarkings(bitmap,x,y)
    markings = @pokemon.markings
    markrect = Rect.new(0,0,16,16)
    for i in 0...6
      markrect.x = i*16
      markrect.y = (markings&(1<<i)!=0) ? 16 : 0
      bitmap.blt(x+i*16,y,@markingbitmap.bitmap,markrect)
    end
  end
  
  
#=============================================================================
# IV Ratings - Shows IV ratings on Page 3 (Stats)
#   Adaptaded from Lucidious89's IV star script by Tommaniacal
# 
# Converted to BW Summary Pack by DeepBlue PacificWaves
#=============================================================================
  
  def pbDisplayIVRating
    ratingf=sprintf("Graphics/Pictures/Summary/RatingF")
    ratingd=sprintf("Graphics/Pictures/Summary/RatingD")
    ratingc=sprintf("Graphics/Pictures/Summary/RatingC")
    ratingb=sprintf("Graphics/Pictures/Summary/RatingB")
    ratinga=sprintf("Graphics/Pictures/Summary/RatingA")
    ratings=sprintf("Graphics/Pictures/Summary/RatingS")
    overlay = @sprites["overlay"].bitmap
    imagepos=[]
    #HP
    if @pokemon.iv[0]>30
      imagepos.push([ratings,110+128,100,0,0,-1,-1])
    elsif @pokemon.iv[0]>22 && @pokemon.iv[0]<31
      imagepos.push([ratinga,110+128,100,0,0,-1,-1])
    elsif @pokemon.iv[0]>15 && @pokemon.iv[0]<23
      imagepos.push([ratingb,110+128,100,0,0,-1,-1])
    elsif @pokemon.iv[0]>7 && @pokemon.iv[0]<16
      imagepos.push([ratingc,110+128,100,0,0,-1,-1])
    elsif @pokemon.iv[0]>0 && @pokemon.iv[0]<8
      imagepos.push([ratingd,110+128,100,0,0,-1,-1])
    else
      imagepos.push([ratingf,110+128,100,0,0,-1,-1])
    end
    #Atk
    if @pokemon.iv[1]>30
      imagepos.push([ratings,110+128,132,0,0,-1,-1])
    elsif @pokemon.iv[1]>22 && @pokemon.iv[1]<31
      imagepos.push([ratinga,110+128,132,0,0,-1,-1])
    elsif @pokemon.iv[1]>15 && @pokemon.iv[1]<23
      imagepos.push([ratingb,110+128,132,0,0,-1,-1])
    elsif @pokemon.iv[1]>7 && @pokemon.iv[1]<16
      imagepos.push([ratingc,110+128,132,0,0,-1,-1])
    elsif @pokemon.iv[1]>0 && @pokemon.iv[1]<8
      imagepos.push([ratingd,110+128,132,0,0,-1,-1])
    else
      imagepos.push([ratingf,110+128,132,0,0,-1,-1])
    end
    #Def
    if @pokemon.iv[2]>30
      imagepos.push([ratings,110+128,164,0,0,-1,-1])
    elsif @pokemon.iv[2]>22 && @pokemon.iv[2]<31
      imagepos.push([ratinga,110+128,164,0,0,-1,-1])
    elsif @pokemon.iv[2]>15 && @pokemon.iv[2]<23
      imagepos.push([ratingb,110+128,164,0,0,-1,-1])
    elsif @pokemon.iv[2]>7 && @pokemon.iv[2]<16
      imagepos.push([ratingc,110+128,164,0,0,-1,-1])
    elsif @pokemon.iv[2]>0 && @pokemon.iv[2]<8
      imagepos.push([ratingd,110+128,164,0,0,-1,-1])
    else
      imagepos.push([ratingf,110+128,164,0,0,-1,-1])
    end
    #SpAtk
    if @pokemon.iv[4]>30
      imagepos.push([ratings,110+128,196,0,0,-1,-1])
    elsif @pokemon.iv[4]>22 && @pokemon.iv[4]<31
      imagepos.push([ratinga,110+128,196,0,0,-1,-1])
    elsif @pokemon.iv[4]>15 && @pokemon.iv[4]<23
      imagepos.push([ratingb,110+128,196,0,0,-1,-1])
    elsif @pokemon.iv[4]>7 && @pokemon.iv[4]<16
      imagepos.push([ratingc,110+128,196,0,0,-1,-1])
    elsif @pokemon.iv[4]>0 && @pokemon.iv[4]<8
      imagepos.push([ratingd,110+128,196,0,0,-1,-1])
    else
      imagepos.push([ratingf,110+128,196,0,0,-1,-1])
    end
    #SpDef
    if @pokemon.iv[5]>30
      imagepos.push([ratings,110+128,228,0,0,-1,-1])
    elsif @pokemon.iv[5]>22 && @pokemon.iv[5]<31
      imagepos.push([ratinga,110+128,228,0,0,-1,-1])
    elsif @pokemon.iv[5]>15 && @pokemon.iv[5]<23
      imagepos.push([ratingb,110+128,228,0,0,-1,-1])
    elsif @pokemon.iv[5]>7 && @pokemon.iv[5]<16
      imagepos.push([ratingc,110+128,228,0,0,-1,-1])
    elsif @pokemon.iv[5]>0 && @pokemon.iv[5]<8
      imagepos.push([ratingd,110+128,228,0,0,-1,-1])
    else
      imagepos.push([ratingf,110+128,228,0,0,-1,-1])
    end
    #Speed
    if @pokemon.iv[3]>30
      imagepos.push([ratings,110+128,260,0,0,-1,-1])
    elsif @pokemon.iv[3]>22 && @pokemon.iv[3]<31
      imagepos.push([ratinga,110+128,260,0,0,-1,-1])
    elsif @pokemon.iv[3]>15 && @pokemon.iv[3]<23
      imagepos.push([ratingb,110+128,260,0,0,-1,-1])
    elsif @pokemon.iv[3]>7 && @pokemon.iv[3]<16
      imagepos.push([ratingc,110+128,260,0,0,-1,-1])
    elsif @pokemon.iv[3]>0 && @pokemon.iv[3]<8
      imagepos.push([ratingd,110+128,260,0,0,-1,-1])
    else
      imagepos.push([ratingf,110+128,260,0,0,-1,-1])
    end
    pbDrawImagePositions(overlay,imagepos)
  end 
#=============================================================================
  def drawPage(page)
    if @pokemon.egg?
      drawPageOneEgg; return
    end
    @sprites["itemicon"].item = @pokemon.item
    overlay = @sprites["overlay"].bitmap
    overlay.clear
    # Changes the color of the text, to the one used in BW
    base   = Color.new(90,82,82)
    shadow = Color.new(165,165,173)
    # Set background image
    @sprites["bg_overlay"].setBitmap("Graphics/Pictures/Summary/background")           
    @sprites["menuoverlay"].setBitmap("Graphics/Pictures/Summary/bg_#{page}")
    imagepos=[]
    # Show the Poké Ball containing the Pokémon
    ballimage = sprintf("Graphics/Pictures/Summary/icon_ball_%02d",@pokemon.ballused)
    imagepos.push([ballimage,390+64+32,44])
    # Show status/fainted/Pokérus infected icon
    status = -1
    status = 8 if @pokemon.pokerusStage==1
    status = @pokemon.status-1 if @pokemon.status>0
    status = 7 if @pokemon.hp==0
    if status>=0
      imagepos.push(["Graphics/Pictures/statuses",476+64,88,0,16*status,44,16])
    end
    # Show Pokérus cured icon
    if @pokemon.pokerusStage==2
      imagepos.push([sprintf("Graphics/Pictures/Summary/icon_pokerus"),376,305])
    end
    # Show shininess star
    if @pokemon.shiny?
      if @pokemon.superShiny?
        imagepos.push([sprintf("Graphics/Pictures/superShiny"),450+64,300+64])
      else
        imagepos.push([sprintf("Graphics/Pictures/shiny"),450+64,300+64])
      end
    end
    # Draw all images
    pbDrawImagePositions(overlay,imagepos)
    # Write various bits of text
    pagename = [_INTL("宝可梦信息"),
                _INTL("详细信息"),
                _INTL("能力"),
                _INTL("招式"),
                _INTL("缎带")][page-1]
    # Changed various positions of the text
    textpos = [
       [pagename,26,8,0,Color.new(255,255,255),Color.new(132,132,132)],
       [@pokemon.name,424+64+32,46,0,Color.new(90,82,82),Color.new(165,165,173)],
       [@pokemon.level.to_s,410+64+32,84,0,Color.new(90,82,82),Color.new(165,165,173)],
       [_INTL("携带道具"),366+64+32,325+64,0,base,shadow]
       
    ]
    # Write the held item's name
    if @pokemon.hasItem?
      textpos.push([PBItems.getName(@pokemon.item),366+64+32,354+64,0,base,shadow])
    else
      textpos.push([_INTL("无"),366+64+32,354+64,0,base,shadow])
    end
    # Write the gender symbol
    if @pokemon.male?
      textpos.push([_INTL("♂"),550+96,46,0,Color.new(0,0,214),Color.new(15,148,255)])
    elsif @pokemon.female?
      textpos.push([_INTL("♀"),550+96,46,0,Color.new(198,0,0),Color.new(255,155,155)])
    end
    # Draw all text
    pbDrawTextPositions(overlay,textpos)
    # Draw the Pokémon's markings
    drawMarkings(overlay,480+64+32,306+64)
    # Draw page-specific information
    case page
    when 1; drawPageOne
    when 2; drawPageTwo
    when 3; drawPageThree
    when 4; drawPageFour
    when 5; drawPageFive
    end
  end
  def drawPageOne
    overlay = @sprites["overlay"].bitmap
    # Changes the color of the text, to the one used in BW
    base   = Color.new(255,255,255)
    shadow = Color.new(165,165,173)
    dexNumBase   = (@pokemon.shiny?) ? Color.new(198,0,0) : Color.new(90,82,82)
    dexNumShadow = (@pokemon.shiny?) ? Color.new(255,155,155) : Color.new(165,165,173)
    # If a Shadow Pokémon, draw the heart gauge area and bar
    if @pokemon.shadowPokemon?
      shadowfract = @pokemon.heartgauge*1.0/PokeBattle_Pokemon::HEARTGAUGESIZE
      imagepos = [
         ["Graphics/Pictures/Summary/overlay_shadow",0,228],
         ["Graphics/Pictures/Summary/overlay_shadowbar",90,268,0,0,(shadowfract*248).floor,-1]
      ]
      pbDrawImagePositions(overlay,imagepos)
    end
    # Write various bits of text. Changed various positions of the text
    fSpecies = pbGetFSpeciesFromForm(@pokemon.species,@pokemon.form)
    formName = pbGetMessage(MessageTypes::FormNames,fSpecies)
    formName = "默认形态" if formName == ""
    formName += "※" if @pokemon.forcedForm
    textpos = [
       [_INTL("图鉴ID"),34,68,0,base,shadow],
       [_INTL("种族名称"),34,102,0,base,shadow],
       [PBSpecies.getName(@pokemon.species),164,102,0,Color.new(90,82,82),Color.new(165,165,173)],
       [formName,134+32,134,0,Color.new(90,82,82),Color.new(165,165,173)],
       [_INTL("形态"),34,134,0,base,shadow],
       [_INTL("属性"),34,166,0,base,shadow],
       [_INTL("训练家"),34,198,0,base,shadow],
       [_INTL("ID No."),34,230,0,base,shadow],
    ]
    # Write the Regional/National Dex number
    dexnum = @pokemon.species
    dexnumshift = false
    if $PokemonGlobal.pokedexUnlocked[$PokemonGlobal.pokedexUnlocked.length-1]
      dexnumshift = true if DEXES_WITH_OFFSETS.include?(-1)
    else
      dexnum = 0
      for i in 0...$PokemonGlobal.pokedexUnlocked.length-1
        next if !$PokemonGlobal.pokedexUnlocked[i]
        num = pbGetRegionalNumber(i,@pokemon.species)
        next if num<=0
        dexnum = num
        dexnumshift = true if DEXES_WITH_OFFSETS.include?(i)
        break
      end
    end
    if dexnum<=0
      # Write ??? if Pokémon is not founf in the Dex
      textpos.push(["???",164,68,0,dexNumBase,dexNumShadow])
    else
      dexnum -= 1 if dexnumshift
      # Write the Dex Number
      textpos.push([sprintf("%03d",dexnum),164,68,0,dexNumBase,dexNumShadow])
    end
    # Write Original Trainer's name and ID number
    if @pokemon.ot==""
      textpos.push([_INTL("租借"),164,164,0,Color.new(90,82,82),Color.new(165,165,173)])
      textpos.push(["?????",164,194,0,Color.new(90,82,82),Color.new(165,165,173)])
    else
      # Changes the color of the text, to the one used in BW
      ownerbase   = Color.new(90,82,82)
      ownershadow = Color.new(165,165,173)
      case @pokemon.otgender
      when 0; ownerbase = Color.new(0,0,214); ownershadow = Color.new(15,148,255)
      when 1; ownerbase = Color.new(198,0,0);  ownershadow = Color.new(255,155,155)
      end
      # Write Trainer's name 
      textpos.push([@pokemon.ot,164,198,0,ownerbase,ownershadow])
      # Write Pokémon's ID 
      textpos.push([sprintf("%05d",@pokemon.publicID),164,230,0,Color.new(90,82,82),Color.new(165,165,173)])
    end
    # ========== 亲密度 ==========
    @pokemon.happiness = 255 if @pokemon.happiness > 255
    textpos.push([_INTL("亲密度"),34,262,0,base,shadow])
    textpos.push([sprintf("%3d/%3d",@pokemon.happiness,255),164,262,0,Color.new(90,82,82),Color.new(165,165,173)])
    # Write Exp text OR heart gauge message (if a Shadow Pokémon)
    if @pokemon.shadowPokemon?
      textpos.push([_INTL("净化计"),33,228,0,base,shadow])
      heartmessage = [_INTL("心门打开了！\n解开最后的束缚吧！"),
                      _INTL("心门几乎打开了。"),
                      _INTL("心门快要打开了。"),
                      _INTL("心门越开越大。"),
                      _INTL("心门正在打开。"),
                      _INTL("心门紧闭着。")][@pokemon.heartStage]
       # Changed the text color, to the one used in BW
       memo = sprintf("<c3=404040,B0B0B0>%s\n",heartmessage)
      drawFormattedTextEx(overlay,60,292,264,memo)
    else
      endexp = PBExperience.pbGetStartExperience(@pokemon.level+1,@pokemon.growthrate)
      textpos.push([_INTL("经验值"),34,228+64,0,base,shadow])
      # Changed the Positon of No. of Exp 
      textpos.push([@pokemon.exp.to_s_formatted,215,260+64,2,Color.new(90,82,82),Color.new(165,165,173)])
      textpos.push([_INTL("下个等级所需要的经验"),34,292+64,0,base,shadow])
            # Changed the Positon of No. of Exp to Next Level
      textpos.push([(endexp-@pokemon.exp).to_s_formatted,177,324+64,2,Color.new(90,82,82),Color.new(165,165,173)]) 
    end
    # Draw all text
    pbDrawTextPositions(overlay,textpos)
    # Draw Pokémon type(s)
    type1rect = Rect.new(0,@pokemon.type1*28,64,28)
    type2rect = Rect.new(0,@pokemon.type2*28,64,28)
    if @pokemon.type1==@pokemon.type2
      overlay.blt(164,164,@typebitmap.bitmap,type1rect)
    else
      overlay.blt(164,164,@typebitmap.bitmap,type1rect)
      overlay.blt(232,164,@typebitmap.bitmap,type2rect)
    end
    # Draw Exp bar
    if @pokemon.level<PBExperience.maxLevel
      w = @pokemon.expFraction*128
      w = ((w/2).round)*2
      pbDrawImagePositions(overlay,[
         ["Graphics/Pictures/Summary/overlay_exp",140,360+64,0,0,w,6]
      ])
    end
  end
  def drawPageOneEgg
    @sprites["itemicon"].item = @pokemon.item
    overlay = @sprites["overlay"].bitmap
    overlay.clear
    # Changes the color of the text, to the one used in BW
    base   = Color.new(90,82,82)
    shadow = Color.new(165,165,173)
    # Set background image
    @sprites["menuoverlay"].setBitmap("Graphics/Pictures/Summary/bg_egg")
    imagepos = []
    # Show the Poké Ball containing the Pokémon
    ballimage = sprintf("Graphics/Pictures/Summary/icon_ball_%02d",@pokemon.ballused)
    imagepos.push([ballimage,390+128,44])
    # Draw all images
    pbDrawImagePositions(overlay,imagepos)
    # Write various bits of text
    textpos = [
       [_INTL("面板"),26,8,0,Color.new(255,255,255),Color.new(132,132,132)],
       [@pokemon.name,424,46,0,base,shadow],
       [_INTL("道具"),366,322,0,base,shadow]
    ]
    # Write the held item's name
    if @pokemon.hasItem?
      textpos.push([PBItems.getName(@pokemon.item),290,350,0,base,shadow])
    else
      textpos.push([_INTL("无"),360,350,0,base,shadow])
    end
    # Draw all text
    pbDrawTextPositions(overlay,textpos)
    memo = ""
    # Write date received
    if @pokemon.timeReceived
      date  = @pokemon.timeReceived.day
      month = pbGetMonthName(@pokemon.timeReceived.mon)
      year  = @pokemon.timeReceived.year
      # Changed the color of the text, to the one used in BW
      memo += _INTL("<c3=404040,B0B0B0>{1}年{2}{3}日。\n",year,month,date)
    end
    # Write map name egg was received on
    mapname = pbGetMapNameFromId(@pokemon.obtainMap)
    if (@pokemon.obtainText rescue false) && @pokemon.obtainText!=""
      mapname = @pokemon.obtainText
    end
    if mapname && mapname!=""
      mapname.gsub!("\\RN",$Trainer.rivalName) if mapname=="\\RN"
      # Changed the color of the text, to the one used in BW
      memo += _INTL("<c3=404040,B0B0B0>在{1}<c3=0000D6,7394FF>\n<c3=404040,B0B0B0>获得了一个神秘的宝可梦的蛋。",mapname)
    else
      # Changed the color of the text, to the one used in BW
      memo += _INTL("<c3=404040,B0B0B0>\n一个神秘的宝可梦蛋",mapname)
    end
    memo += "\n" # Empty line
    # Write Egg Watch blurb
    memo += _INTL("<c3=404040,B0B0B0>\"状态\"\n")
    eggstate = _INTL("看起来，这个蛋需要很长时间才能孵化。")
    eggstate = _INTL("会孵化出什么宝可梦呢？\n似乎还需要一些时间。") if @pokemon.eggsteps<10200
    eggstate = _INTL("偶尔会摇动，应该是快要孵化了。") if @pokemon.eggsteps<2550
    eggstate = _INTL("可以听到从里面传出的声音！似乎要孵化了！") if @pokemon.eggsteps<1275
    memo += sprintf("<c3=404040,B0B0B0>%s\n",eggstate)
    # Draw all text
    drawFormattedTextEx(overlay,10,82,268,memo)
    # Draw the Pokémon's markings
    drawMarkings(overlay,480+64+32,306+64)
    end
  def drawPageTwo
    overlay = @sprites["overlay"].bitmap
    memo = ""
    # Write nature
    showNature = !@pokemon.shadowPokemon? || @pokemon.heartStage>3
    if showNature
      natureName = PBNatures.getName(@pokemon.nature)
      # Changed the color of the text, to the one used in BW
      memo += _INTL("<c3=0000d6,7394ff>{1}<c3=404040,B0B0B0>性格。\n",natureName)
    end
    # Write date received
    if @pokemon.timeReceived
      date  = @pokemon.timeReceived.day
      month = pbGetMonthName(@pokemon.timeReceived.mon)
      year  = @pokemon.timeReceived.year
      # Changed the color of the text, to the one used in BW
      memo += _INTL("<c3=404040,B0B0B0>{3}年{2}{1}日\n在",date,month,year)
    end
    # Write map name Pokémon was received on
    mapname = pbGetMapNameFromId(@pokemon.obtainMap)
    if (@pokemon.obtainText rescue false) && @pokemon.obtainText!=""
      mapname = @pokemon.obtainText
    end
    mapname.gsub!("\\RN",$Trainer.rivalName) if mapname=="\\RN"
    mapname = _INTL("遥远的地方") if !mapname || mapname==""
    # Changed the color of the text, to the one used in BW
    memo += sprintf("<c3=0000d6,7394ff>%s\n",mapname)
    # Write how Pokémon was obtained
    mettext = [_INTL("等级{1}时遇见",@pokemon.obtainLevel),
               _INTL("获得的宝可梦的蛋。"),
               _INTL("等级{1}时交换获得。",@pokemon.obtainLevel),
               "",
               _INTL("等级{1}时有命运般的相遇。",@pokemon.obtainLevel)
              ][@pokemon.obtainMode]
              # Changed the color of the text, to the one used in BW
    memo += sprintf("<c3=404040,B0B0B0>%s\n",mettext) if mettext && mettext!=""
    # If Pokémon was hatched, write when and where it hatched
    if @pokemon.obtainMode==1
      if @pokemon.timeEggHatched
        date  = @pokemon.timeEggHatched.day
        month = pbGetMonthName(@pokemon.timeEggHatched.mon)
        year  = @pokemon.timeEggHatched.year
        # Changed the color of the text, to the one used in BW
        memo += _INTL("<c3=404040,B0B0B0>{3}年{2},{1}日 在",date,month,year)
      end
      mapname = pbGetMapNameFromId(@pokemon.hatchedMap)
      mapname = _INTL("遥远的地方") if !mapname || mapname==""
        # Changed the colors of the text, to the one used in BW
      memo += sprintf("\n<c3=C60000,FF7373>%s",mapname)
      memo += _INTL("<c3=404040,B0B0B0>从蛋孵化的。\n")
    else
      memo += "\n"   # Empty line
    end
    # Write characteristic
    if showNature
      bestiv     = 0
      tiebreaker = @pokemon.personalID%6
      for i in 0...6
        if @pokemon.iv[i]==@pokemon.iv[bestiv]
          bestiv = i if i>=tiebreaker && bestiv<tiebreaker
        elsif @pokemon.iv[i]>@pokemon.iv[bestiv]
          bestiv = i
        end
      end
      characteristic = [_INTL("非常喜欢吃东西。"),
                        _INTL("经常睡午觉。"),
                        _INTL("常常打睡意。"),
                        _INTL("经常乱扔东西。"),
                        _INTL("喜欢悠然自在。"),
                        _INTL("以力气大为傲。"),
                        _INTL("喜欢胡闹。"),
                        _INTL("有点容易生气。"),
                        _INTL("喜欢打架。"),
                        _INTL("血气方刚。"),
                        _INTL("身体强壮。"),
                        _INTL("抗打能力强。"),
                        _INTL("顽强不屈。"),
                        _INTL("能吃苦耐劳。"),
                        _INTL("善于忍耐。"),
                        _INTL("喜欢比谁跑得快。"),
                        _INTL("对声音敏感。"),
                        _INTL("冒冒失失。"),
                        _INTL("有点容易得意忘形。"),
                        _INTL("逃得快。"),
                        _INTL("好奇心强。"),
                        _INTL("喜欢恶作剧。"),
                        _INTL("做事万无一失。"),
                        _INTL("经常思考。"),
                        _INTL("一丝不苟。"),
                        _INTL("性格强势。"),
                        _INTL("有一点点爱慕虚荣。"),
                        _INTL("争强好胜。"),
                        _INTL("不服输。"),
                        _INTL("有一点点固执。")
                       ][bestiv*5+@pokemon.iv[bestiv]%5]
      # Changed the color of the text, to the one used in BW
      memo += sprintf("<c3=404040,B0B0B0>%s\n",characteristic)
    end
    # Write all text
    drawFormattedTextEx(overlay,22,64,300,memo)
  end
  def drawPageThree
    overlay = @sprites["overlay"].bitmap
    # Changes the color of the text, to the one used in BW
    base   = Color.new(90,82,82)
    shadow = Color.new(165,165,173)
    # Determine which stats are boosted and lowered by the Pokémon's nature
    pbDisplayIVRating
    # Show IV Letters Grades 
    statshadows = []
    PBStats.eachStat { |s| statshadows[s] = shadow }
    if !@pokemon.shadowPokemon? || @pokemon.heartStage>3
      natup = PBNatures.getStatRaised(@pokemon.calcNature)
      natdn = PBNatures.getStatLowered(@pokemon.calcNature)
      statshadows[natup] = Color.new(206,148,156) if natup!=natdn
      statshadows[natdn] = Color.new(148,148,214) if natup!=natdn
    end
    #===============================================================================
    # Stat Screen Upgrade (EVs and IVs in Summary) 
    #   By Weibrot, Kobi2604 and dirkriptide
    #
    #     Converted to BW Summary Pack by DeepBlue PacificWaves
    #===============================================================================
    # 获取种族值
    baseStats = pbGetSpeciesData(@pokemon.species,@pokemon.form,SpeciesBaseStats)
    # Write various bits of text
    bv_sum = 0
    @pokemon.baseStats.each { |bv| bv_sum += bv }
    textpos = [
       [_INTL("HP"),16,100,0,Color.new(255,255,255),statshadows[PBStats::HP]],
       [sprintf("%d/%d",@pokemon.hp,@pokemon.totalhp),268+88,100,2,base,shadow],
       [sprintf("%d",@pokemon.ev[0]),194+84,100,2,base,shadow],
       [sprintf("%d",@pokemon.iv[0]),152+58,100,2,base,shadow],
       [sprintf("%d",baseStats[0]),152+10,100,2,base,shadow],  # HP种族值
       
       [_INTL("攻击"),16,130,0,Color.new(255,255,255),statshadows[PBStats::ATTACK]],
       [sprintf("%d",@pokemon.attack),268+88,130,2,base,shadow],
       [sprintf("%d",@pokemon.ev[1]),194+84,130,2,base,shadow],
       [sprintf("%d",@pokemon.iv[1]),152+58,130,2,base,shadow],
       [sprintf("%d",baseStats[1]),152+10,130,2,base,shadow],  # 攻击种族值
       
       [_INTL("防御"),16,162,0,Color.new(255,255,255),statshadows[PBStats::DEFENSE]],
       [sprintf("%d",@pokemon.defense),268+88,162,2,base,shadow],
       [sprintf("%d",@pokemon.ev[2]),194+84,162,2,base,shadow],
       [sprintf("%d",@pokemon.iv[2]),152+58,162,2,base,shadow],
       [sprintf("%d",baseStats[2]),152+10,162,2,base,shadow],  # 防御种族值
       
       [_INTL("特攻"),16,194,0,Color.new(255,255,255),statshadows[PBStats::SPATK]],
       [sprintf("%d",@pokemon.spatk),268+88,194,2,base,shadow],
       [sprintf("%d",@pokemon.ev[4]),194+84,194,2,base,shadow],
       [sprintf("%d",@pokemon.iv[4]),152+58,194,2,base,shadow],
       [sprintf("%d",baseStats[4]),152+10,194,2,base,shadow],  # 特攻种族值
       
       [_INTL("特防"),16,226,0,Color.new(255,255,255),statshadows[PBStats::SPDEF]],
       [sprintf("%d",@pokemon.spdef),268+88,226,2,base,shadow],
       [sprintf("%d",@pokemon.ev[5]),194+84,226,2,base,shadow],
       [sprintf("%d",@pokemon.iv[5]),152+58,226,2,base,shadow],
       [sprintf("%d",baseStats[5]),152+10,226,2,base,shadow],  # 特防种族值
       
       [_INTL("速度"),16,258,0,Color.new(255,255,255),statshadows[PBStats::SPEED]],
       [sprintf("%d",@pokemon.speed),268+88,258,2,base,shadow],              
       [sprintf("%d",@pokemon.ev[3]),194+84,258,2,base,shadow],              
       [sprintf("%d",@pokemon.iv[3]),152+58,258,2,base,shadow],
       [sprintf("%d",baseStats[3]),152+10,258,2,base,shadow],  # 速度种族值
       
       [_INTL("总和"),16,290,0,Color.new(255,255,255),shadow],
       [sprintf("%d",bv_sum),152+10,290,2,Color.new(255,255,255),shadow],  # 种族值总和
       
       [_INTL("特性"),12,294+32,0,Color.new(255,255,255),Color.new(165,165,173)],
       [PBAbilities.getName(@pokemon.ability),240,294+32,2,Color.new(90,82,82),Color.new(165,165,173)],
    ]
    # Draw all text
    pbDrawTextPositions(overlay,textpos)
    # Draw ability description
    abilitydesc = pbGetMessage(MessageTypes::AbilityDescs,@pokemon.ability)
    drawTextEx(overlay,12,320+32,282,3,abilitydesc,base,shadow)
    # Draw HP bar
    if @pokemon.hp>0
      w = @pokemon.hp*96*1.0/@pokemon.totalhp
      w = 1 if w<1
      w = ((w/2).round)*2
      hpzone = 0
      hpzone = 1 if @pokemon.hp<=(@pokemon.totalhp/2).floor
      hpzone = 2 if @pokemon.hp<=(@pokemon.totalhp/4).floor
      imagepos = [
         ["Graphics/Pictures/Summary/overlay_hp",32,80,0,hpzone*8,w,6]
      ]
      pbDrawImagePositions(overlay,imagepos)
    end
  end
  def drawPageFour
    overlay = @sprites["overlay"].bitmap
    # Changes the color of the text, to the one used in BW
    moveBase   = Color.new(255,255,255)
    moveShadow = Color.new(123,123,123)
    ppBase   = [moveBase,                # More than 1/2 of total PP
                Color.new(255,214,0),    # 1/2 of total PP or less
                Color.new(255,115,0),   # 1/4 of total PP or less
                Color.new(255,8,72)]    # Zero PP
    ppShadow = [moveShadow,             # More than 1/2 of total PP
                Color.new(123,99,0),   # 1/2 of total PP or less
                Color.new(115,57,0),   # 1/4 of total PP or less
                Color.new(123,8,49)]   # Zero PP
    @sprites["pokemon"].visible  = true
    @sprites["pokeicon"].visible = false
    @sprites["itemicon"].visible = true
    textpos  = []
    imagepos = []
    # Write move names, types and PP amounts for each known move
    yPos = 82
    for i in 0...@pokemon.moves.length
      move=@pokemon.moves[i]
      if move.id>0
        imagepos.push(["Graphics/Pictures/types",32,yPos+2,0,move.type*28,64,28])
        textpos.push([PBMoves.getName(move.id),100,yPos,0,moveBase,moveShadow])
        if move.totalpp>0
          textpos.push([_INTL("PP"),126,yPos+32,0,moveBase,moveShadow])
          ppfraction = 0
          if move.pp==0;                 ppfraction = 3
          elsif move.pp*4<=move.totalpp; ppfraction = 2
          elsif move.pp*2<=move.totalpp; ppfraction = 1
          end
          textpos.push([sprintf("%d/%d",move.pp,move.totalpp),244,yPos+32,1,ppBase[ppfraction],ppShadow[ppfraction]])
        end
      else
        textpos.push(["-",100,yPos,0,moveBase,moveShadow])
        textpos.push(["--",226,yPos+32,1,moveBase,moveShadow])
      end
      yPos += 64
    end
    # Draw all text and images
    pbDrawTextPositions(overlay,textpos)
    pbDrawImagePositions(overlay,imagepos)
  end
  def drawSelectedMove(moveToLearn,moveid)
    # Draw all of page four when selected move's details
    drawMoveSelection(moveToLearn)
    # Set various values
    overlay = @sprites["overlay"].bitmap
    # Changes the color of the text, to the one used in BW
    base   = Color.new(90,82,82)
    shadow = Color.new(165,165,173)
    @sprites["pokemon"].visible = false if @sprites["pokemon"]
    @sprites["pokeicon"].pokemon  = @pokemon
    @sprites["pokeicon"].visible  = true
    @sprites["itemicon"].visible  = false if @sprites["itemicon"]
    # Get data for selected move
    moveData = pbGetMoveData(moveid)
    basedamage = moveData[MOVE_BASE_DAMAGE]
    flags      = moveData[MOVE_FLAGS]
    accuracy   = moveData[MOVE_ACCURACY]
    textpos = []
    # Write power and accuracy values for selected move
    if basedamage==0   # Status move
      textpos.push(["---",216,122,1,base,shadow])
    elsif basedamage==1   # Variable power move
      textpos.push(["???",216,122,1,base,shadow])
    else
      textpos.push([sprintf("%d",basedamage),216,122,1,base,shadow])
    end
    if accuracy==0
      textpos.push(["---",216,155,1,base,shadow])
    else
    # Write move's accurancy
      textpos.push([sprintf("%d%",accuracy),216+overlay.text_size("%").width,155,1,base,shadow])
    end
    # Draw all text
    pbDrawTextPositions(overlay,textpos)
    # Draw selected move's flag icon
    imagepos = []
    flags_arr = ["a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r"]
    flags_x = 120
    flags_arr.each do|flag|
      # 其他flags是有就显示
      no_skip = flags.include?(flag)
      # 守住、鹦鹉学舌是没有才显示
      no_skip = !no_skip if ["b","e"].include?(flag)
      next unless no_skip
      imagepos.push(["Graphics/Pictures/Move Flags/"+flag,flags_x,188,0,0,26,28])
      flags_x += 26
    end
    pbDrawImagePositions(overlay,imagepos)
    # Draw selected move's description
    drawTextEx(overlay,4,220,230,5,
       pbGetMessage(MessageTypes::MoveDescriptions,moveid),base,shadow)
  end
  def drawMoveSelection(moveToLearn)
    # Learn a New Move Scene 
    overlay = @sprites["overlay"].bitmap
    overlay.clear
    # Changes the color of the text, to the one used in BW
    base   = Color.new(255,255,255)
    shadow = Color.new(123,123,123)
    moveBase   = Color.new(255,255,255)
    moveShadow = Color.new(123,123,123)
    ppBase   = [moveBase,                # More than 1/2 of total PP
                Color.new(255,214,0),    # 1/2 of total PP or less
                Color.new(255,115,0),   # 1/4 of total PP or less
                Color.new(255,8,74)]    # Zero PP
    ppShadow = [moveShadow,             # More than 1/2 of total PP
                Color.new(123,99,0),   # 1/2 of total PP or less
                Color.new(115,57,0),   # 1/4 of total PP or less
                Color.new(123,8,49)]   # Zero PP
    # Set background image
    if moveToLearn!=0
      @sprites["menuoverlay"].setBitmap("Graphics/Pictures/Summary/bg_learnmove")
    else
      @sprites["menuoverlay"].setBitmap("Graphics/Pictures/Summary/bg_movedetail")
    end
    # Write various bits of text
    textpos = [
       [_INTL("招式"),26,8,0,base,shadow],
       [_INTL("威力"),20,122,0,base,shadow],
       [_INTL("命中"),20,154,0,base,shadow],
       [_INTL("标签"),20,186,0,base,shadow]
    ]
    imagepos = []
    # Write move names, types and PP amounts for each known move
    yPos = 98
    yPos -= 76 if moveToLearn!=0
    for i in 0...5
      move = @pokemon.moves[i]
      if i==4
        move = PBMove.new(moveToLearn) if moveToLearn!=0
        yPos += 20
      end
      if move && move.id>0
        imagepos.push(["Graphics/Pictures/types",310,yPos+2,0,move.type*28,64,28])
        textpos.push([PBMoves.getName(move.id),380,yPos,0,moveBase,moveShadow])
        if move.totalpp>0
          textpos.push([_INTL("PP"),380,yPos+32,0,moveBase,moveShadow])
          ppfraction = 0
          if move.pp==0;                 ppfraction = 3
          elsif move.pp*4<=move.totalpp; ppfraction = 2
          elsif move.pp*2<=move.totalpp; ppfraction = 1
          end
          textpos.push([sprintf("%d/%d",move.pp,move.totalpp),492,yPos+32,1,ppBase[ppfraction],ppShadow[ppfraction]])
        end
        # Draw selected move's damage category icon
        moveData = pbGetMoveData(move.id)
        category = moveData[MOVE_CATEGORY]
        imagepos.push(["Graphics/Pictures/category",310,yPos+32,0,category*28,64,28])
      elsif i < 4
        textpos.push(["-",328,yPos,0,moveBase,moveShadow])
        textpos.push(["--",454,yPos+32,1,moveBase,moveShadow])
      end
      yPos += 64
    end
    # Draw all text and images
    pbDrawTextPositions(overlay,textpos)
    pbDrawImagePositions(overlay,imagepos)
    # Draw Pokémon's type icon(s)
    type1rect = Rect.new(0,@pokemon.type1*28,64,28)
    type2rect = Rect.new(0,@pokemon.type2*28,64,28)
    if @pokemon.type1==@pokemon.type2
      overlay.blt(130,78,@typebitmap.bitmap,type1rect)
    else
      overlay.blt(96,78,@typebitmap.bitmap,type1rect)
      overlay.blt(166,78,@typebitmap.bitmap,type2rect)
    end
  end
  def drawPageFive
    overlay = @sprites["overlay"].bitmap
    @sprites["uparrow"].visible   = false
    @sprites["downarrow"].visible = false
    # Write various bits of text
    textpos = [
       [_INTL("缎带数量"),38,303,0,Color.new(255,255,255),Color.new(165,165,173)],
       [@pokemon.ribbonCount.to_s,157,334,1,Color.new(90,82,82),Color.new(165,165,173)],
    ]
    # Draw all text
    pbDrawTextPositions(overlay,textpos)
    # Show all ribbons
    imagepos = []
    coord = 0
    if @pokemon.ribbons
      for i in @ribbonOffset*4...@ribbonOffset*4+12
        break if !@pokemon.ribbons[i]
        ribn = @pokemon.ribbons[i]-1
    # Sets the X an Y position of the Ribbons in Ribbons Page (the first value)
        imagepos.push(["Graphics/Pictures/ribbons",2+68*(coord%4),74+68*(coord/4).floor,
                                                   64*(ribn%8),64*(ribn/8).floor,64,64])
        coord += 1
        break if coord>=12
      end
    end
    # Draw all images
    pbDrawImagePositions(overlay,imagepos)
  end
  def drawSelectedRibbon(ribbonid)
    # Draw all of page five
    drawPage(5)
    # Set various values
    overlay = @sprites["overlay"].bitmap
    # Changes the color of the text, to the one used in BW
    base   = Color.new(90,82,82)
    shadow = Color.new(165,165,173)
    nameBase   = Color.new(248,248,248)
    nameShadow = Color.new(104,104,104)
    # Get data for selected ribbon
    name = ribbonid ? PBRibbons.getName(ribbonid) : ""
    desc = ribbonid ? PBRibbons.getDescription(ribbonid) : ""
    # Draw the description box
    imagepos = [
       ["Graphics/Pictures/Summary/overlay_ribbon",0,280]
    ]
    pbDrawImagePositions(overlay,imagepos)
    # Draw name of selected ribbon
    textpos = [
       [name,30,286,0,nameBase,nameShadow]
    ]
    pbDrawTextPositions(overlay,textpos)
    # Draw selected ribbon's description
    drawTextEx(overlay,30,318,480,0,desc,base,shadow)
  end
  def pbGoToPrevious
    newindex = @partyindex
    while newindex>0
      newindex -= 1
      if @party[newindex] && (@page==1 || !@party[newindex].egg?)
        @partyindex = newindex
        break
      end
    end
  end
  def pbGoToNext
    newindex = @partyindex
    while newindex<@party.length-1
      newindex += 1
      if @party[newindex] && (@page==1 || !@party[newindex].egg?)
        @partyindex = newindex
        break
      end
    end
  end
  def pbChangePokemon
    @pokemon = @party[@partyindex]
    @sprites["pokemon"].setPokemonBitmap(@pokemon)
    @sprites["itemicon"].item = @pokemon.item
    pbSEStop
    pbPlayCry(@pokemon)
  end
  def pbMoveSelection
    @sprites["movesel"].visible = true
    @sprites["movesel"].index   = 0
    selmove    = 0
    oldselmove = 0
    switching = false
    drawSelectedMove(0,@pokemon.moves[selmove].id)
    loop do
      Graphics.update
      Input.update
      pbUpdate
      if @sprites["movepresel"].index==@sprites["movesel"].index
        @sprites["movepresel"].z = @sprites["movesel"].z+1
      else
        @sprites["movepresel"].z = @sprites["movesel"].z
      end
      if Input.trigger?(Input::B)
        (switching) ? pbPlayCancelSE : pbPlayCloseMenuSE
        break if !switching
        @sprites["movepresel"].visible = false
        switching = false
      elsif Input.trigger?(Input::C)
        pbPlayDecisionSE
        if selmove==4
          break if !switching
          @sprites["movepresel"].visible = false
          switching = false
        else
          if !@pokemon.shadowPokemon?
            if !switching
              @sprites["movepresel"].index   = selmove
              @sprites["movepresel"].visible = true
              oldselmove = selmove
              switching = true
            else
              tmpmove                    = @pokemon.moves[oldselmove]
              @pokemon.moves[oldselmove] = @pokemon.moves[selmove]
              @pokemon.moves[selmove]    = tmpmove
              @sprites["movepresel"].visible = false
              switching = false
              drawSelectedMove(0,@pokemon.moves[selmove].id)
            end
          end
        end
      elsif Input.trigger?(Input::UP)
        selmove -= 1
        if selmove<4 && selmove>=@pokemon.numMoves
          selmove = @pokemon.numMoves-1
        end
        selmove = 0 if selmove>=4
        selmove = @pokemon.numMoves-1 if selmove<0
        @sprites["movesel"].index = selmove
        newmove = @pokemon.moves[selmove].id
        pbPlayCursorSE
        drawSelectedMove(0,newmove)
      elsif Input.trigger?(Input::DOWN)
        selmove += 1
        selmove = 0 if selmove<4 && selmove>=@pokemon.numMoves
        selmove = 0 if selmove>=4
        selmove = 4 if selmove<0
        @sprites["movesel"].index = selmove
        newmove = @pokemon.moves[selmove].id
        pbPlayCursorSE
        drawSelectedMove(0,newmove)
      end
    end
    @sprites["movesel"].visible=false
  end
  def pbRibbonSelection
    @sprites["ribbonsel"].visible = true
    @sprites["ribbonsel"].index   = 0
    selribbon    = @ribbonOffset*4
    oldselribbon = selribbon
    switching = false
    numRibbons = @pokemon.ribbons.length
    numRows    = [((numRibbons+3)/4).floor,3].max
    drawSelectedRibbon(@pokemon.ribbons[selribbon])
    loop do
      @sprites["uparrow"].visible   = (@ribbonOffset>0)
      @sprites["downarrow"].visible = (@ribbonOffset<numRows-3)
      Graphics.update
      Input.update
      pbUpdate
      if @sprites["ribbonpresel"].index==@sprites["ribbonsel"].index
        @sprites["ribbonpresel"].z = @sprites["ribbonsel"].z+1
      else
        @sprites["ribbonpresel"].z = @sprites["ribbonsel"].z
      end
      hasMovedCursor = false
      if Input.trigger?(Input::B)
        (switching) ? pbPlayCancelSE : pbPlayCloseMenuSE
        break if !switching
        @sprites["ribbonpresel"].visible = false
        switching = false
      elsif Input.trigger?(Input::C)
        if !switching
          if @pokemon.ribbons[selribbon]
            pbPlayDecisionSE
            @sprites["ribbonpresel"].index = selribbon-@ribbonOffset*4
            oldselribbon = selribbon
            @sprites["ribbonpresel"].visible = true
            switching = true
          end
        else
          pbPlayDecisionSE
          tmpribbon                      = @pokemon.ribbons[oldselribbon]
          @pokemon.ribbons[oldselribbon] = @pokemon.ribbons[selribbon]
          @pokemon.ribbons[selribbon]    = tmpribbon
          if @pokemon.ribbons[oldselribbon] || @pokemon.ribbons[selribbon]
            @pokemon.ribbons.compact!
            if selribbon>=numRibbons
              selribbon = numRibbons-1
              hasMovedCursor = true
            end
          end
          @sprites["ribbonpresel"].visible = false
          switching = false
          drawSelectedRibbon(@pokemon.ribbons[selribbon])
        end
      elsif Input.trigger?(Input::UP)
        selribbon -= 4
        selribbon += numRows*4 if selribbon<0
        hasMovedCursor = true
        pbPlayCursorSE
      elsif Input.trigger?(Input::DOWN)
        selribbon += 4
        selribbon -= numRows*4 if selribbon>=numRows*4
        hasMovedCursor = true
        pbPlayCursorSE
      elsif Input.trigger?(Input::LEFT)
        selribbon -= 1
        selribbon += 4 if selribbon%4==3
        hasMovedCursor = true
        pbPlayCursorSE
      elsif Input.trigger?(Input::RIGHT)
        selribbon += 1
        selribbon -= 4 if selribbon%4==0
        hasMovedCursor = true
        pbPlayCursorSE
      end
      if hasMovedCursor
        @ribbonOffset = (selribbon/4).floor if selribbon<@ribbonOffset*4
        @ribbonOffset = (selribbon/4).floor-2 if selribbon>=(@ribbonOffset+3)*4
        @ribbonOffset = 0 if @ribbonOffset<0
        @ribbonOffset = numRows-3 if @ribbonOffset>numRows-3
        @sprites["ribbonsel"].index    = selribbon-@ribbonOffset*4
        @sprites["ribbonpresel"].index = oldselribbon-@ribbonOffset*4
        drawSelectedRibbon(@pokemon.ribbons[selribbon])
      end
    end
    @sprites["ribbonsel"].visible = false
  end
  def pbMarking(pokemon)
    @sprites["markingbg"].visible      = true
    @sprites["markingoverlay"].visible = true
    @sprites["markingsel"].visible     = true
    # Changed the color of the text, to the one used in BW
    base   = Color.new(248,248,248)
    shadow = Color.new(104,104,104)
    ret = pokemon.markings
    markings = pokemon.markings
    index = 0
    redraw = true
    markrect = Rect.new(0,0,16,16)
    loop do
      # Redraw the markings and text
      if redraw
        @sprites["markingoverlay"].bitmap.clear
        for i in 0...6
          markrect.x = i*16
          markrect.y = (markings&(1<<i)!=0) ? 16 : 0
          @sprites["markingoverlay"].bitmap.blt(300+58*(i%3),154+50*(i/3),@markingbitmap.bitmap,markrect)
        end
        textpos = [
           [_INTL("标记{1}",pokemon.name),366,96,2,base,shadow],
           [_INTL("OK"),366,248,2,base,shadow],
           [_INTL("取消"),366,298,2,base,shadow]
        ]
        pbDrawTextPositions(@sprites["markingoverlay"].bitmap,textpos)
        redraw = false
      end
      # Reposition marking the cursor
      @sprites["markingsel"].x = 284+58*(index%3)
      @sprites["markingsel"].y = 144+50*(index/3)
      if index==6   # OK
        @sprites["markingsel"].x = 284
        @sprites["markingsel"].y = 244
        @sprites["markingsel"].src_rect.y = @sprites["markingsel"].bitmap.height/2
      elsif index==7   # Cancel
        @sprites["markingsel"].x = 284
        @sprites["markingsel"].y = 294
        @sprites["markingsel"].src_rect.y = @sprites["markingsel"].bitmap.height/2
      else
        @sprites["markingsel"].src_rect.y = 0
      end
      Graphics.update
      Input.update
      pbUpdate
      if Input.trigger?(Input::B)
        pbPlayCloseMenuSE
        break
      elsif Input.trigger?(Input::C)
        pbPlayDecisionSE
        if index==6   # OK
          ret = markings
          break
        elsif index==7   # Cancel
          break
        else
          mask = (1<<index)
          if (markings&mask)==0
            markings |= mask
          else
            markings &= ~mask
          end
          redraw = true
        end
      elsif Input.trigger?(Input::UP)
        if index==7;    index = 6
        elsif index==6; index = 4
        elsif index<3;  index = 7
        else;           index -= 3
        end
        pbPlayCursorSE
      elsif Input.trigger?(Input::DOWN)
        if index==7;    index = 1
        elsif index==6; index = 7
        elsif index>=3; index = 6
        else;           index += 3
        end
        pbPlayCursorSE
      elsif Input.trigger?(Input::LEFT)
        if index<6
          index -= 1
          index += 3 if index%3==2
          pbPlayCursorSE
        end
      elsif Input.trigger?(Input::RIGHT)
        if index<6
          index += 1
          index -= 3 if index%3==0
          pbPlayCursorSE
        end
      end
    end
    @sprites["markingbg"].visible      = false
    @sprites["markingoverlay"].visible = false
    @sprites["markingsel"].visible     = false
    if pokemon.markings!=ret
      pokemon.markings = ret
      return true
    end
    return false
  end
  def pbOptions
    dorefresh = false
    commands   = []
    cmdGiveItem = -1
    cmdTakeItem = -1
    cmdPokedex  = -1
    cmdMark     = -1
    if !@pokemon.egg?
      commands[cmdGiveItem = commands.length] = _INTL("携带道具")
      commands[cmdTakeItem = commands.length] = _INTL("拿回道具") if @pokemon.hasItem?
      commands[cmdPokedex = commands.length]  = _INTL("查看图鉴") if $Trainer.pokedex
    end
    commands[cmdMark = commands.length]       = _INTL("标记")
    commands[commands.length]                 = _INTL("取消")
    command = pbShowCommands(commands)
    if cmdGiveItem>=0 && command==cmdGiveItem
      item = 0
      pbFadeOutIn {
        scene = PokemonBag_Scene.new
        screen = PokemonBagScreen.new(scene,$PokemonBag)
        item = screen.pbChooseItemScreen(Proc.new { |itm| pbCanHoldItem?(itm) })
      }
      if item>0
        dorefresh = pbGiveItemToPokemon(item,@pokemon,self,@partyindex)
      end
    elsif cmdTakeItem>=0 && command==cmdTakeItem
      dorefresh = pbTakeItemFromPokemon(@pokemon,self)
    elsif cmdPokedex>=0 && command==cmdPokedex
      pbUpdateLastSeenForm(@pokemon)
      pbFadeOutIn {
        scene = PokemonPokedexInfo_Scene.new
        screen = PokemonPokedexInfoScreen.new(scene)
        screen.pbStartSceneSingle(@pokemon.species)
      }
      dorefresh = true
    elsif cmdMark>=0 && command==cmdMark
      dorefresh = pbMarking(@pokemon)
    end
    return dorefresh
  end
  def pbChooseMoveToForget(moveToLearn)
    selmove = 0
    maxmove = (moveToLearn>0) ? 4 : 3
    loop do
      Graphics.update
      Input.update
      pbUpdate
      if Input.trigger?(Input::B)
        selmove = 4
        pbPlayCloseMenuSE if moveToLearn>0
        break
      elsif Input.trigger?(Input::C)
        pbPlayDecisionSE
        break
      elsif Input.trigger?(Input::UP)
        selmove -= 1
        selmove = maxmove if selmove<0
        if selmove<4 && selmove>=@pokemon.numMoves
          selmove = @pokemon.numMoves-1
        end
        @sprites["movesel"].index = selmove
        newmove = (selmove==4) ? moveToLearn : @pokemon.moves[selmove].id
        drawSelectedMove(moveToLearn,newmove)
      elsif Input.trigger?(Input::DOWN)
        selmove += 1
        selmove = 0 if selmove>maxmove
        if selmove<4 && selmove>=@pokemon.numMoves
          selmove = (moveToLearn>0) ? maxmove : 0
        end
        @sprites["movesel"].index = selmove
        newmove = (selmove==4) ? moveToLearn : @pokemon.moves[selmove].id
        drawSelectedMove(moveToLearn,newmove)
      end
    end
    return (selmove==4) ? -1 : selmove
  end
  def pbScene
    pbPlayCry(@pokemon)
    loop do
      Graphics.update
      Input.update
      pbUpdate
      dorefresh = false
      if Input.trigger?(Input::A)
        pbSEStop
        pbPlayCry(@pokemon)
      elsif Input.trigger?(Input::B)
        pbPlayCloseMenuSE
        break
      elsif Input.trigger?(Input::C)
        if @page==4
          pbPlayDecisionSE
          pbMoveSelection
          dorefresh = true
        elsif @page==5
          pbPlayDecisionSE
          pbRibbonSelection
          dorefresh = true
        elsif !@inbattle
          pbPlayDecisionSE
          dorefresh = pbOptions
        end
      elsif Input.trigger?(Input::UP) && @partyindex>0
        oldindex = @partyindex
        pbGoToPrevious
        if @partyindex!=oldindex
          pbChangePokemon
          @ribbonOffset = 0
          dorefresh = true
        end
      elsif Input.trigger?(Input::DOWN) && @partyindex<@party.length-1
        oldindex = @partyindex
        pbGoToNext
        if @partyindex!=oldindex
          pbChangePokemon
          @ribbonOffset = 0
          dorefresh = true
        end
      elsif Input.trigger?(Input::LEFT) && !@pokemon.egg?
        oldpage = @page
        @page -= 1
        @page = 1 if @page<1
        @page = 5 if @page>5
        if @page!=oldpage   # Move to next page
          pbSEPlay("GUI summary change page")
          @ribbonOffset = 0
          dorefresh = true
        end
      elsif Input.trigger?(Input::RIGHT) && !@pokemon.egg?
        oldpage = @page
        @page += 1
        @page = 1 if @page<1
        @page = 5 if @page>5
        if @page!=oldpage   # Move to next page
          pbSEPlay("GUI summary change page")
          @ribbonOffset = 0
          dorefresh = true
        end
      end
      if dorefresh
        drawPage(@page)
      end
    end
    return @partyindex
  end
end
class PokemonSummaryScreen
  def initialize(scene,inbattle=false)
    @scene = scene
    @inbattle = inbattle
  end
  def pbStartScreen(party,partyindex)
    @scene.pbStartScene(party,partyindex,@inbattle)
    ret = @scene.pbScene
    @scene.pbEndScene
    return ret
  end
  def pbStartForgetScreen(party,partyindex,moveToLearn)
    ret = -1
    @scene.pbStartForgetScene(party,partyindex,moveToLearn)
    loop do
      ret = @scene.pbChooseMoveToForget(moveToLearn)
  #    if ret>=0 && moveToLearn!=0 && pbIsHiddenMove?(party[partyindex].moves[ret].id) && !$DEBUG
  #     pbMessage(_INTL("现在还不能忘记秘传招式。")) { @scene.pbUpdate }
  #    else
  #      break
  #     end
      break
    end
    @scene.pbEndScene
    return ret
  end
  def pbStartChooseMoveScreen(party,partyindex,message)
    ret = -1
    @scene.pbStartForgetScene(party,partyindex,0)
    pbMessage(message) { @scene.pbUpdate }
    loop do
      ret = @scene.pbChooseMoveToForget(0)
      if ret<0
        pbMessage(_INTL("必须选择一个招式！")) { @scene.pbUpdate }
      else
        break
      end
    end
    @scene.pbEndScene
    return ret
  end
end
