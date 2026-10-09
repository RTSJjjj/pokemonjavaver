#===============================================================================#===============================================================================
#  Party Screen BW Style
#  for Pokémon Essentials version 18.x
#  
#===============================================================================
#
# Instructions: Put this script above Main. Download the file BW Party.rar and
# extract the files in your project main folder.
#
#===============================================================================
#
#  Modified by DeepBlue PacificWaves
#
#   Special thanks to Shashu-Greninja, that help with the implementation of the 
#   ZUD Compatibility Patch
#
#  Graphics Ripped by DeepBlue PacificWaves
#
#
#  If used, please give credits. For more information on how to credit, look 
#  for the original post on Relic Castle or PokéCommunity.
#   
#===============================================================================

PluginManager.register({
  :name => "影辞风格队伍界面",
  :version => "2.0",
  :credits => ["咪啪","ES泽洛"]
})

#===============================================================================
# Pokémon party buttons and menu
#===============================================================================
class PokemonPartyConfirmCancelSprite < SpriteWrapper
  attr_reader :selected
  def initialize(text,x,y,narrowbox=false,viewport=nil)
    super(viewport)
    @refreshBitmap = true
    @bgsprite = ChangelingSprite.new(0,0,viewport)
    if narrowbox
      @bgsprite.addBitmap("desel","Graphics/Pictures/Party/icon_cancel_narrow")
      @bgsprite.addBitmap("sel","Graphics/Pictures/Party/icon_cancel_narrow_sel")
    else
      @bgsprite.addBitmap("desel","Graphics/Pictures/Party/icon_cancel")
      @bgsprite.addBitmap("sel","Graphics/Pictures/Party/icon_cancel_sel")
    end
    @bgsprite.changeBitmap("desel")
    @overlaysprite = BitmapSprite.new(@bgsprite.bitmap.width,@bgsprite.bitmap.height,viewport)
    @overlaysprite.z = self.z+1
    pbSetSystemFont(@overlaysprite.bitmap)
    @yoffset = 8
# Changed color of text
    textpos = [[text,55,(narrowbox) ? 4 : 8,2,Color.new(255,255,255),Color.new(132,132,132)]]
    pbDrawTextPositions(@overlaysprite.bitmap,textpos)
    self.x = x
    self.y = y
  end
  def dispose
    @bgsprite.dispose
    @overlaysprite.bitmap.dispose
    @overlaysprite.dispose
    super
  end
  def viewport=(value)
    super
    refresh
  end
  def x=(value)
    super
    refresh
  end
  def y=(value)
    super
    refresh
  end
  def color=(value)
    super
    refresh
  end
  def selected=(value)
    if @selected!=value
      @selected = value
      refresh
    end
  end
  def refresh
    if @bgsprite && !@bgsprite.disposed?
      @bgsprite.changeBitmap((@selected) ? "sel" : "desel")
      @bgsprite.x     = self.x
      @bgsprite.y     = self.y
      @bgsprite.color = self.color
    end
    if @overlaysprite && !@overlaysprite.disposed?
      @overlaysprite.x     = self.x
      @overlaysprite.y     = self.y
      @overlaysprite.color = self.color
    end
  end
end
class PokemonPartyCancelSprite < PokemonPartyConfirmCancelSprite
  def initialize(viewport=nil)
# Changed Cancel button position
    super(_INTL("取消"),378+64+64,330+64,false,viewport)
  end
end
class PokemonPartyConfirmSprite < PokemonPartyConfirmCancelSprite
  def initialize(viewport=nil)
    super(_INTL("确认"),378+64+64,310+64,true,viewport)
  end
end
class PokemonPartyCancelSprite2 < PokemonPartyConfirmCancelSprite
  def initialize(viewport=nil)
    super(_INTL("取消"),378+64+64,348+64,true,viewport)
  end
end
class Window_CommandPokemonColor < Window_CommandPokemon
  def initialize(commands,width=nil)
    @colorKey = []
    for i in 0...commands.length
      if commands[i].is_a?(Array)
        @colorKey[i] = commands[i][1]
        commands[i] = commands[i][0]
      end
    end
    super(commands,width)
  end
  def drawItem(index,_count,rect)
    pbSetSystemFont(self.contents) if @starting
    rect = drawCursor(index,rect)
    base   = self.baseColor
    shadow = self.shadowColor
    if @colorKey[index] && @colorKey[index]==1
      base   = Color.new(0,80,160)
      shadow = Color.new(128,192,240)
    end
    pbDrawShadowText(self.contents,rect.x,rect.y,rect.width,rect.height,@commands[index],base,shadow)
  end
end
#===============================================================================
# Pokémon  party selection Background Panel
#===============================================================================
class PokemonPartySelectionBackgroundPanel < SpriteWrapper
  def initialize(viewport=nil)
    super(viewport)
    self.x = (Graphics.width - 430) / 2
    self.y = 228+64
    @panelSelectionBgSprite = AnimatedBitmap.new("Graphics/Pictures/Party/panel_pok_base_bg")
    self.bitmap = @panelSelectionBgSprite.bitmap
  end
  def dispose
    @panelSelectionBgSprite.dispose
    super
  end
  def selected; return false; end
  def selected=(value); end
  def preselected; return false; end
  def preselected=(value); end
  def switching; return false; end
  def switching=(value); end
  def refresh; end
end
class PokemonPartySelectionBlankPanel < SpriteWrapper
  attr_accessor :text
  def initialize(index,viewport=nil)
    super(viewport) 
    # 同样使用72像素的间距
    slot_width = 72
    start_x = (Graphics.width - 430) / 2
    self.x = start_x + slot_width * index
    self.y = 240
    @text = nil
  end
  
  def dispose
    super
  end
  def selected; return false; end
  def selected=(value); end
  def preselected; return false; end
  def preselected=(value); end
  def switching; return false; end
  def switching=(value); end
  def refresh; end
end
# =============================================================================
# Pokemon party selection panel (details)
# =============================================================================
class PokemonPartyDetailsPanel < SpriteWrapper
  def initialize(viewport=nil, pokemon=nil)
    super(viewport)
    @pokemon = pokemon
    @viewport = viewport

    # in the middle of the screen
    self.x = Graphics.width/2
    self.y = Graphics.height/2 - 128
    @pkmnsprite =  PokemonSprite.new(@viewport)
    @pkmnsprite.setOffset(PictureOrigin::Center)
    @pkmnsprite.x = self.x
    @pkmnsprite.y = self.y + 30
    @pkmnsprite.z = self.z
    # overlay for hp, name, level, etc. 
    @overlaysprite = BitmapSprite.new(Graphics.width,300,@viewport)
    @overlaysprite.z = @viewport.z + 2
    pbSetSystemFont(@overlaysprite.bitmap)
    # overlay for nature, ability, mvoes, types, etc. 
    @overlaysprite2 = BitmapSprite.new(Graphics.width,300,@viewport)
    @overlaysprite2.z = @viewport.z + 2
    pbSetSmallFont(@overlaysprite2.bitmap)
    @text = ''
    @pkmndata = {
      :item => @pokemon.item,
      :species => @pokemon.species,
      :form => @pokemon.form,
      :name => @pokemon.name,
      :hp => @pokemon.hp,
      :level => @pokemon.level,
      :nature => @pokemon.nature,
      :ability => @pokemon.ability,
      :moves => @pokemon.moves
    }
    @expBar = PokemonPartyExpBar.new(@viewport, @pokemon)
    @expBar.x = 235+48  # 血条旁边
    @expBar.y = 220+56  # 血条下方
  end
  def text=(value)
    if @text!=value && value.is_a?(String)
      @text = value
      refreshOverlay
    end
  end
  def color=(value)
    super
    refresh
  end
  def pokemon
    return @pokemon
  end
  def pokemon=(value)
    return if @refreshing
    @pokemon = value
    # if not initialized, refresh
    @refreshing = true
    if !@pkmnspriteItem
      @pkmnspriteItem = ItemIconSprite.new(194,194+64,@pokemon.item,@viewport)
      @pkmnspriteItem.z = @viewport.z + 1
      refreshOverlay
    end
    # if pkmndata changed, refresh
    should_refresh = false
    for key in @pkmndata.keys
      if @pkmndata[key] != @pokemon.send(key)
        @pkmndata[key] = @pokemon.send(key)
        should_refresh = true
      end
    end
    refreshOverlay if should_refresh
    @refreshing = false
    refresh
    @expBar.pokemon = value if @expBar && !@expBar.disposed?
   end

   def refreshOverlay
    @pkmnsprite.setPokemonBitmap(@pokemon)
    offset = Graphics.width/2 - 96
    if @overlaysprite && !@overlaysprite.disposed? 
      @overlaysprite.bitmap.clear
      # Draw HP bar
      hp_w = @pokemon.hp*96*1.0/@pokemon.totalhp
      hp_w = 1 if hp_w<1
      hp_w = ((hp_w/2).round)*2
      hpzone = 0
      hpzone = 1 if @pokemon.hp<=(@pokemon.totalhp/2).floor
      hpzone = 2 if @pokemon.hp<=(@pokemon.totalhp/4).floor
      pbDrawImagePositions(@overlaysprite.bitmap,[
        ["Graphics/Pictures/Party/overlay_hp",91+offset,194+56,0,hpzone*6,hp_w,6]
      ])
      # Draw Text
      line = Color.new(255,255,255)
      outline = Color.new(132,132,132)
      textpos = [
        [@pokemon.name.to_s,-90+offset,15,1, line,outline],
        [@pokemon.level.to_s,262+offset+32,18,3,Color.new(90,82,82),Color.new(165,165,173)],
        [@text,Graphics.width-16,200+32,1, line,outline],
      ]
      textpos2 = [
        [sprintf("%3d/%3d",@pokemon.hp,@pokemon.totalhp),130+offset,200+56,2, line,outline]
      ]
      if !@pokemon.egg?
      # Draw gender symbol as image
      if @pokemon.male?
       pbDrawImagePositions(@overlaysprite.bitmap,[[
         "Graphics/Pictures/Party/icon_male",152,18,0,0,20,20]])
       elsif @pokemon.female?
        pbDrawImagePositions(@overlaysprite.bitmap,[[
         "Graphics/Pictures/Party/icon_famale",152,18,0,0,20,20]])
       end
      end
      pbDrawTextPositions(@overlaysprite.bitmap,textpos)
      # Draw shiny icon
      if @pokemon.shiny?
        if @pokemon.superShiny?
          pbDrawImagePositions(@overlaysprite.bitmap,[[
             "Graphics/Pictures/superShiny",358+offset,18,0,0,20,20]])
        else
          pbDrawImagePositions(@overlaysprite.bitmap,[[
             "Graphics/Pictures/shiny",358+offset,18,0,0,20,20]])
        end
     end
      if !@pokemon.egg?
        # Draw ball icon
        ball = pbItemIconFile(pbBallTypeToItem(@pokemon.ballused))
        pbDrawImagePositions(@overlaysprite.bitmap,[[
             ball,Graphics.width-52,3,0,0,48,48]])
      end
    end
    if @overlaysprite2 && !@overlaysprite2.disposed? 
      @overlaysprite2.bitmap.clear
    # Draw nature, ability, moves, types
      if !@pokemon.egg?
        # Get form info
        fSpecies = pbGetFSpeciesFromForm(@pokemon.species,@pokemon.form)
        formName = pbGetMessage(MessageTypes::FormNames,fSpecies)
        formName = "默认形态" if formName == ""
        formName += "※" if @pokemon.forcedForm
        # Draw form name
        textpos2.push([_INTL("{1}",formName),4,108+8,0,Color.new(90,82,82),Color.new(165,165,173)])
        # Draw nature
        nature = PBNatures.getName(@pkmndata[:nature])
        textpos2.push([_INTL("性格:{1}",nature),4,134+8,0,Color.new(90,82,82),Color.new(165,165,173)])
        # Draw ability
        ability = PBAbilities.getName(@pkmndata[:ability])
        textpos2.push([_INTL("特性:{1}",ability),4,160+8,0,Color.new(90,82,82),Color.new(165,165,173)])
        # Draw IVs
        textpos2.push([_INTL("个体:"),4,186+8,0,Color.new(90,82,82),Color.new(165,165,173)])
        # Draw moves
        textpos2.push([_INTL("招式:"),232+offset+42,54+32,0,line,outline])
        i = 0
        @pkmndata[:moves].each do |m|
          next if m.id == 0
          move = PBMoves.getName(m.id)
          textpos2.push([_INTL("{1}",move),248+offset+24,78+32+24*i,0,Color.new(90,82,82),Color.new(165,165,173)])
          i += 1
        end
        # Draw types icon
        textpos2.push([_INTL("属性:"),4,56,0,line,outline])
        pbDrawTextPositions(@overlaysprite2.bitmap,textpos2)
        pbDrawImagePositions(@overlaysprite2.bitmap,[[
             "Graphics/Pictures/types",4,80,0,@pokemon.type1*28,64,28]])
        if @pokemon.type1 != @pokemon.type2
          pbDrawImagePositions(@overlaysprite2.bitmap,[[
               "Graphics/Pictures/types",68,80,0,@pokemon.type2*28,64,28]])
        end
        # Draw IVs icon
        # HP Atk Def SpAtk SpDef Spd
        xs = [56, 71, 86, 132, 101, 116]
        @pokemon.iv.each_with_index do |iv, i|
          if iv > 30
             rating = sprintf("Graphics/Pictures/Summary/RatingS")
          elsif iv > 22
             rating = sprintf("Graphics/Pictures/Summary/RatingA")
          elsif iv > 15
             rating = sprintf("Graphics/Pictures/Summary/RatingB")
          elsif iv > 7
             rating = sprintf("Graphics/Pictures/Summary/RatingC")
          elsif iv > 0
             rating = sprintf("Graphics/Pictures/Summary/RatingD")
          else
             rating = sprintf("Graphics/Pictures/Summary/RatingF")
           end
          pbDrawImagePositions(@overlaysprite2.bitmap,
                               [[rating,xs[i],188+8,0,0,16,20]])
        end
      end
    end
  end
  
  
  def dispose
    super
    @shadowsprite.dispose if @shadowsprite && !@shadowsprite.disposed?
    @pkmnsprite.dispose if @pkmnsprite && !@pkmnsprite.disposed?
    @overlaysprite.dispose if @overlaysprite && !@overlaysprite.disposed?
    @overlaysprite2.dispose if @overlaysprite2 && !@overlaysprite2.disposed?
    @pkmnspriteItem.dispose if @pkmnspriteItem && !@pkmnspriteItem.disposed?
    @expBar.dispose if @expBar && !@expBar.disposed?
  end
  def refresh
    return if disposed?
    return if @refreshing
    @refreshing = true
    if @pkmnsprite && !@pkmnsprite.disposed?
      @pkmnsprite.color = self.color
    end
    if @pkmnspriteItem && !@pkmnspriteItem.disposed? 
      @pkmnspriteItem.color = self.color
      @pkmnspriteItem.item = @pokemon.item
      if @pokemon.item>0 
        @pkmnspriteItem.visible = true  
      else 
        @pkmnspriteItem.visible = false  
      end
    end
    if @overlaysprite && !@overlaysprite.disposed? 
      @overlaysprite.color = self.color
    end
    if @overlaysprite2 && !@overlaysprite2.disposed? 
      @overlaysprite2.color = self.color
    end
    @expBar.color = self.color if @expBar && !@expBar.disposed?
    @refreshing = false
  end
end

# =============================================================================
# Pokemon party selection panel (below)
# =============================================================================
class PokemonPartySelectionPanel < SpriteWrapper
  attr_reader :pokemon
  attr_reader :active
  attr_reader :selected
  attr_reader :preselected
  attr_reader :switching
  attr_reader :text
  def initialize(pokemon,index,viewport=nil,details=nil)
    super(viewport)  # 必须先调用 super
    
    # 每个槽位约72像素宽，6个槽位总宽约430
    slot_width = 72
    start_x = (Graphics.width - 430) / 2  # = 121
    self.x = start_x + slot_width * index
    self.y = 226+64
    @pokemon = pokemon
    @active = (index==0) 
    @details = details
    
    # Shadow sprite (added below the Pokemon icon)
    @shadowsprite = IconSprite.new(0,0,viewport)
    @shadowsprite.setBitmap("Graphics/Pictures/Party/shadow")
    @shadowsprite.x = self.x + slot_width/2  # 槽位中心
    @shadowsprite.y = self.y + 64
    @shadowsprite.z = 1
    
    # PokemonIconSprite
    @pkmnsprite = PokemonIconSprite.new(pokemon,viewport)
    @pkmnsprite.setOffset(PictureOrigin::Center)
    @pkmnsprite.x      = self.x + slot_width/2  # 槽位中心
    @pkmnsprite.y      = self.y + 52
    @pkmnsprite.active = @active
    @pkmnsprite.z = self.z
    
    # HeldItemIconSprite
    @helditem   = HeldItemIconSprite.new(0, 0, @pokemon, viewport)
    @helditem.x = self.x + 40
    @helditem.y = self.y + 56
    @helditem.z = self.z + 1
    
    @overlaysprite   = BitmapSprite.new(96,160,viewport)
    @overlaysprite.x = self.x + 12
    @overlaysprite.y = self.y
    @statuses        = AnimatedBitmap.new(_INTL("Graphics/Pictures/statuses"))
    @evolution       = AnimatedBitmap.new(_INTL("Graphics/Pictures/Party/icon_evo"))
    @selected      = false
    @preselected   = false
    @switching     = false
    @text          = nil
    @refreshBitmap = true
    @refreshing    = false
    @arrow_normal    = AnimatedBitmap.new(_INTL("Graphics/Pictures/Party/arrow_normal"))
    @arrow_preselect = AnimatedBitmap.new(_INTL("Graphics/Pictures/Party/arrow_preselect"))
  end
  
  def dispose
    super
    @arrow_normal.dispose if @arrow_normal && !@arrow_normal.disposed?
    @arrow_preselect.dispose if @arrow_preselect && !@arrow_preselect.disposed?
    @statuses.dispose if @statuses && !@statuses.disposed?
    @evolution.dispose if @evolution && !@evolution.disposed?
    @helditem.dispose if @helditem && !@helditem.disposed?
    @pkmnsprite.dispose if @pkmnsprite && !@pkmnsprite.disposed?
    @shadowsprite.dispose if @shadowsprite && !@shadowsprite.disposed?
    @overlaysprite.dispose if @overlaysprite && !@overlaysprite.disposed?
    @details.dispose if @details && !@details.disposed?
  end
  def x=(value)
    super
    refresh
  end
  def y=(value)
    super
    refresh
  end
  def color=(value)
    super
    refresh
  end
  def text=(value)
    if @text!=value
      @text = value
      @refreshBitmap = true
      refresh
    end
  end
  def pokemon=(value)
    @pokemon = value
    @pkmnsprite.pokemon = value if @pkmnsprite && !@pkmnsprite.disposed?
    @helditem.pokemon = value if @helditem && !@helditem.disposed?
    @refreshBitmap = true
    refresh
  end
  def selected=(value)
    if @selected!=value
      @selected = value
      refresh
    end
  end
  def preselected=(value)
    if @preselected!=value
      @preselected = value
      refresh
    end
  end
  def switching=(value)
    if @switching!=value
      @switching = value
      refresh
    end
  end
  def update
    super
    @arrow_normal.update if @arrow_normal && !@arrow_normal.disposed?
    @arrow_preselect.update if @arrow_preselect && !@arrow_preselect.disposed?
    @statuses.update if @statuses && !@statuses.disposed?
    @evolution.update if @evolution && !@evolution.disposed?
    @pkmnsprite.update if @pkmnsprite && !@pkmnsprite.disposed?
    @helditem.update if @helditem && !@helditem.disposed?
    @overlaysprite.update if @overlaysprite && !@overlaysprite.disposed?
  end
  
def refresh
    return if disposed?
    return if @refreshing
    @refreshing = true
    
    # 更新阴影位置和z轴（始终保持在最底层）
    if @shadowsprite && !@shadowsprite.disposed?
      @shadowsprite.x = self.x + 24  # slot_width/2
      @shadowsprite.y = self.y + 64
      @shadowsprite.color = self.color
      @shadowsprite.z = self.z  # 强制保持阴影在底层
    end
    
    # 更新宝可梦图标位置和z轴
    if @pkmnsprite && !@pkmnsprite.disposed?
      @pkmnsprite.color    = self.color
      @pkmnsprite.selected = self.selected
      @pkmnsprite.x = self.x + 36  # slot_width/2
      @pkmnsprite.y = self.y + 52
      @pkmnsprite.z = self.z  # 图标在阴影上方
    end
    
    # 更新道具图标位置和z轴
    if @helditem && !@helditem.disposed?
      if @helditem.visible
        @helditem.color = self.color
        @helditem.x = self.x + 40
        @helditem.y = self.y + 56
        @helditem.z = self.z + 1  # 道具在图标上方
      end
    end
    
    # 更新覆盖层位置和z轴
    if @overlaysprite && !@overlaysprite.disposed?
      @overlaysprite.color = self.color
      @overlaysprite.x = self.x + 12
      @overlaysprite.y = self.y
      @overlaysprite.z = self.z + 2  # 覆盖层在最上层
      @overlaysprite.bitmap.clear
    end
    
    if @pokemon
      # Draw status
      status = -1
      status = 8 if @pokemon.pokerusStage==1
      status = @pokemon.status-1 if @pokemon.status>0
      status = 7 if @pokemon.hp<=0
      if status>=0
        statusrect = Rect.new(0,16*status,44,16)
        @overlaysprite.bitmap.blt(0,12,@statuses.bitmap,statusrect)
      end
      # Draw evolution
      ret = pbCheckEvolutionEx(@pokemon){ |pkmn, method, parameter, new_species|
        success = PBEvolution.call("levelUpCheck", method, pkmn, parameter)
        next (success) ? new_species : -1
      }
      if ret>0
        @overlaysprite.bitmap.blt(0,64,@evolution.bitmap,Rect.new(0,0,34,13))
      end
      if @selected
        # Draw arrow normal
        @overlaysprite.bitmap.blt(12,0,@arrow_normal.bitmap,Rect.new(0,0,20,12))
        @details.color = self.color 
        @details.text =  @text
        @details.pokemon = @pokemon
      end
      if @preselected
        # Draw arrow preselect
        @overlaysprite.bitmap.blt(12,0,@arrow_preselect.bitmap,Rect.new(0,0,20,12))
      end
    end
    @refreshing = false
  end
  
end

#===============================================================================
# Pokémon party panels
#===============================================================================
class PokemonPartyBlankPanel < SpriteWrapper
  attr_accessor :text
  def initialize(_pokemon,index,viewport=nil)
    super(viewport)
    self.x = [0, Graphics.width/2][index%2]
    self.y = [0, 16, 96, 112, 192, 208][index]
    @panelbgsprite = AnimatedBitmap.new("Graphics/Pictures/Party/panel_blank")
    self.bitmap = @panelbgsprite.bitmap
    @text = nil
  end
  def dispose
    @panelbgsprite.dispose
    super
  end
  def selected; return false; end
  def selected=(value); end
  def preselected; return false; end
  def preselected=(value); end
  def switching; return false; end
  def switching=(value); end
  def refresh; end
end
#===============================================================================
# Pokémon party visuals
#===============================================================================
class PokemonParty_Scene
  def pbStartScene(party,starthelptext,annotations=nil,multiselect=false,can_access_storage=false)
    @sprites = {}
    @party = party
    @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z = 99999
    @multiselect = multiselect
    @can_access_storage = can_access_storage
    addBackgroundPlane(@sprites,"partybg","Party/bg",@viewport)
    addBackgroundPlane(@sprites,"partybg2","Party/bg2",@viewport)
    @sprites["partybg2"].z = @viewport.z+1
    @sprites["messagebox"] = Window_AdvancedTextPokemon.new("")
    @sprites["messagebox"].viewport       = @viewport
    @sprites["messagebox"].visible        = false
    @sprites["messagebox"].setSkin("Graphics/Windowskins/bw choice")
    @sprites["messagebox"].letterbyletter = true    
    pbBottomLeftLines(@sprites["messagebox"],2)
    # 寄存系统
    @sprites["storagetext"] = Window_UnformattedTextPokemon.new(
      @can_access_storage ? _INTL("[F]:寄存系统") : "")
    offset_x = @sprites["storagetext"].contents.text_size("[F]: 寄存系统").width
    @sprites["storagetext"].x           = Graphics.width - offset_x - 16
    @sprites["storagetext"].y           = 182+32
    @sprites["storagetext"].z           = 10
    @sprites["storagetext"].viewport    = @viewport
    @sprites["storagetext"].baseColor   = Color.new(248, 248, 248)
    @sprites["storagetext"].shadowColor = Color.new(132,132,132)
    @sprites["storagetext"].windowskin  = nil
    
    @sprites["helpwindow"] = Window_UnformattedTextPokemon.new(starthelptext)
    @sprites["helpwindow"].viewport = @viewport
    @sprites["helpwindow"].visible  = true
    @sprites["helpwindow"].setSkin("Graphics/Windowskins/bw choice")
    # add Pokémon party selection group panel
    @sprites["selectionpanel"] = PokemonPartySelectionBackgroundPanel.new(@viewport)
    @sprites["detailspanel"] = PokemonPartyDetailsPanel.new(@viewport, @party[0])
    pbBottomLeftLines(@sprites["helpwindow"],1)
    pbSetHelpText(starthelptext)
    # Add party Pokémon sprites
    for i in 0...6
      if @party[i]
        @sprites["pokemon#{i}"] = PokemonPartySelectionPanel.new(@party[i],i,@viewport,@sprites["detailspanel"])
      else
        @sprites["pokemon#{i}"] = PokemonPartySelectionBlankPanel.new(i,@viewport)
      end
      @sprites["pokemon#{i}"].text = annotations[i] if annotations
    end
    if @multiselect
      @sprites["pokemon6"] = PokemonPartyConfirmSprite.new(@viewport)
      @sprites["pokemon7"] = PokemonPartyCancelSprite2.new(@viewport)
    else
      @sprites["pokemon6"] = PokemonPartyCancelSprite.new(@viewport)
    end
    # Select first Pokémon
    @activecmd = 0
    @sprites["pokemon0"].selected = true
    pbFadeInAndShow(@sprites) { update }
  end
  def pbEndScene
    pbFadeOutAndHide(@sprites) { update }
    pbDisposeSpriteHash(@sprites)
    @viewport.dispose
  end
  def pbDisplay(text)
    @sprites["messagebox"].text    = text
    @sprites["messagebox"].visible = true
    @sprites["helpwindow"].visible = false
    pbPlayDecisionSE
    loop do
      Graphics.update
      Input.update
      self.update
      if @sprites["messagebox"].busy?
        if Input.trigger?(Input::C)
          pbPlayDecisionSE if @sprites["messagebox"].pausing?
          @sprites["messagebox"].resume
        end
      else
        if Input.trigger?(Input::B) || Input.trigger?(Input::C)
          break
        end
      end
    end
    @sprites["messagebox"].visible = false
    @sprites["helpwindow"].visible = true
  end
  def pbDisplayConfirm(text)
    ret = -1
    @sprites["messagebox"].text    = text
    @sprites["messagebox"].visible = true
    @sprites["helpwindow"].visible = false
    using(cmdwindow = Window_CommandPokemon.new([_INTL("是"),_INTL("否")])) {
      cmdwindow.visible = false
      pbBottomRight(cmdwindow)
      cmdwindow.y -= @sprites["messagebox"].height
      cmdwindow.z = @viewport.z+1
      loop do
        Graphics.update
        Input.update
        cmdwindow.visible = true if !@sprites["messagebox"].busy?
        cmdwindow.update
        self.update
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
    @sprites["helpwindow"].visible = true
    return ret
  end
  def pbShowCommands(helptext,commands,index=0)
    ret = -1
    helpwindow = @sprites["helpwindow"]
    helpwindow.visible = true
    using(cmdwindow = Window_CommandPokemonColor.new(commands)) {
      cmdwindow.z     = @viewport.z+1
      cmdwindow.index = index
      pbBottomRight(cmdwindow)
      helpwindow.resizeHeightToFit(helptext,Graphics.width-cmdwindow.width)
      helpwindow.text = helptext
      pbBottomLeft(helpwindow)
      loop do
        Graphics.update
        Input.update
        cmdwindow.update
        self.update
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
  def pbSetHelpText(helptext)
    helpwindow = @sprites["helpwindow"]
    pbBottomLeftLines(helpwindow,1)
    helpwindow.text = helptext
    # Changed help window width
    helpwindow.width = 378
    helpwindow.visible = true
  end
  def pbHasAnnotations?
    return @sprites["pokemon0"].text!=nil
  end
  def pbAnnotate(annot)
    for i in 0...6
      @sprites["pokemon#{i}"].text = (annot) ? annot[i] : nil
    end
  end
  def pbSelect(item)
    @activecmd = item
    numsprites = (@multiselect) ? 8 : 7
    for i in 0...numsprites
      @sprites["pokemon#{i}"].selected = (i==@activecmd)
    end
  end
  def pbPreSelect(item)
    @activecmd = item
  end
  def pbSwitchBegin(oldid,newid)
    pbSEPlay("GUI party switch")
    oldsprite = @sprites["pokemon#{oldid}"]
    newsprite = @sprites["pokemon#{newid}"]
    pbWait(16)
  end
  def pbSwitchEnd(oldid,newid)
    pbSEPlay("GUI party switch")
    oldsprite = @sprites["pokemon#{oldid}"]
    newsprite = @sprites["pokemon#{newid}"]
    oldsprite.pokemon = @party[oldid]
    newsprite.pokemon = @party[newid]
    pbWait(16)
    pbClearSwitching
    pbRefresh
  end
  def pbClearSwitching
    for i in 0...6
      @sprites["pokemon#{i}"].preselected = false
      @sprites["pokemon#{i}"].switching   = false
    end
  end
  def pbSummary(pkmnid,inbattle=false)
    oldsprites = pbFadeOutAndHide(@sprites)
    scene = PokemonSummary_Scene.new
    screen = PokemonSummaryScreen.new(scene,inbattle)
    screen.pbStartScreen(@party,pkmnid)
    yield if block_given?
    pbFadeInAndShow(@sprites,oldsprites)
    pbHardRefresh
  end
  def pbChooseItem(bag)
    ret = 0
    pbFadeOutIn {
      scene = PokemonBag_Scene.new
      screen = PokemonBagScreen.new(scene,bag)
      ret = screen.pbChooseItemScreen(Proc.new { |item| pbCanHoldItem?(item) })
      yield if block_given?
    }
    return ret
  end
  def pbUseItem(bag,pokemon)
    ret = 0
    pbFadeOutIn {
      scene = PokemonBag_Scene.new
      screen = PokemonBagScreen.new(scene,bag)
      ret = screen.pbChooseItemScreen(Proc.new { |item|
        next false if !pbCanUseOnPokemon?(item)
        if pbIsMachine?(item)
          move = pbGetMachine(item)
          next false if pokemon.hasMove?(move) || !pokemon.compatibleWithMove?(move)
        end
        next true
      })
      yield if block_given?
    }
    return ret
  end
  def pbChoosePokemon(switching=false,initialsel=-1,canswitch=0)
    for i in 0...6
      @sprites["pokemon#{i}"].preselected = (switching && i==@activecmd)
      @sprites["pokemon#{i}"].switching   = switching
    end
    @activecmd = initialsel if initialsel>=0
    pbRefresh
    loop do
      Graphics.update
      Input.update
      self.update
      oldsel = @activecmd
      key = -1
      key = Input::DOWN if Input.repeat?(Input::DOWN)
      key = Input::RIGHT if Input.repeat?(Input::RIGHT)
      key = Input::LEFT if Input.repeat?(Input::LEFT)
      key = Input::UP if Input.repeat?(Input::UP)
      if key>=0
        @activecmd = pbChangeSelection(key,@activecmd)
      end
      if @activecmd!=oldsel   # Changing selection
        pbPlayCursorSE
        numsprites = (@multiselect) ? 8 : 7
        for i in 0...numsprites
          @sprites["pokemon#{i}"].selected = (i==@activecmd)
        end
      end
      cancelsprite = (@multiselect) ? 7 : 6
      if Input.trigger?(Input::A) && canswitch==1 && @activecmd!=cancelsprite
        pbPlayDecisionSE
        return [1,@activecmd]
      elsif Input.trigger?(Input::A) && canswitch==2
        return -1
      elsif Input.trigger?(Input::B)
        pbPlayCloseMenuSE if !switching
        return -1
      elsif Input.trigger?(Input::C)
        if @activecmd==cancelsprite
          (switching) ? pbPlayDecisionSE : pbPlayCloseMenuSE
          return -1
        else
          pbPlayDecisionSE
          return @activecmd
        end
      elsif Input.trigger?(Input::F5) && $Trainer.pokepc &&
          @can_access_storage && canswitch != 2
         maps = [95,96,97,98,117,118,119,153,299,357,416,465,467]
        if maps.include?($game_map.map_id)
          pbMessage(_INTL("暂时无法使用。"))
          next
        end
        pbFadeOutIn {
          scene = PokemonStorageScene.new
          screen = PokemonStorageScreen.new(scene,$PokemonStorage)
          screen.pbStartScreen(0)
          pbHardRefresh
        }
        if $PokemonGlobal.followerToggled
          $PokemonTemp.dependentEvents.come_back(false)
        end
      end
    end
  end
  def pbChangeSelection(key,currentsel)
    numsprites = (@multiselect) ? 8 : 7
    case key
    when Input::LEFT
      begin
        currentsel -= 1
      end while currentsel>0 && currentsel<@party.length && !@party[currentsel]
      if currentsel>=@party.length && currentsel<6
        currentsel = @party.length-1
      end
      currentsel = numsprites-1 if currentsel<0
    when Input::RIGHT
      begin
        currentsel += 1
      end while currentsel<@party.length && !@party[currentsel]
      if currentsel==@party.length
        currentsel = 6
      elsif currentsel==numsprites
        currentsel = 0
      end
    when Input::UP
      if currentsel==0
        currentsel = 6
      else
        begin
          currentsel = 0
        end while currentsel>0 && !@party[currentsel]
      end
      if currentsel>=@party.length && currentsel<6
        currentsel = @party.length-1
      end
      currentsel = numsprites-1 if currentsel<0
    when Input::DOWN
      if currentsel == 6
        currentsel = 0
      else
        currentsel = 6
      end
      if currentsel>=@party.length && currentsel<6
        currentsel = 6
      elsif currentsel>=numsprites
        currentsel = 0
      end
    end
    return currentsel
  end
  def pbHardRefresh
    oldtext = []
    lastselected = -1
    for i in 0...6
      oldtext.push(@sprites["pokemon#{i}"].text)
      lastselected = i if @sprites["pokemon#{i}"].selected
      @sprites["pokemon#{i}"].dispose
    end
    @sprites["selectionpanel"].dispose
    lastselected = @party.length-1 if lastselected>=@party.length
    lastselected = 0 if lastselected<0
    @sprites["detailspanel"] = PokemonPartyDetailsPanel.new(@viewport, @party[0])
    @sprites["selectionpanel"] = PokemonPartySelectionBackgroundPanel.new(@viewport)
    for i in 0...6
      if @party[i]
        @sprites["pokemon#{i}"] = PokemonPartySelectionPanel.new(@party[i],i,@viewport,@sprites["detailspanel"])
      else
        @sprites["pokemon#{i}"] = PokemonPartySelectionBlankPanel.new(i,@viewport)
      end
      @sprites["pokemon#{i}"].text = oldtext[i]
    end
    pbSelect(lastselected)
  end
  def pbRefresh
    for i in 0...6
      sprite = @sprites["pokemon#{i}"]
      if sprite
        if sprite.is_a?(PokemonPartySelectionPanel)
          sprite.pokemon = sprite.pokemon
        else
          sprite.refresh
        end
      end
    end
  end
  def pbRefreshSingle(i)
    sprite = @sprites["pokemon#{i}"]
    if sprite
      if sprite.is_a?(PokemonPartySelectionPanel)
        sprite.pokemon = sprite.pokemon
      else
        sprite.refresh
      end
    end
  end
  def update
    pbUpdateSpriteHash(@sprites)
  end
end
#===============================================================================
# Pokémon party mechanics
#===============================================================================
class PokemonPartyScreen
  attr_reader :scene
  attr_reader :party
  def initialize(scene,party)
    @scene = scene
    @party = party
  end
  def pbStartScene(helptext,_numBattlersOut,annotations=nil)
    @scene.pbStartScene(@party,helptext,annotations)
  end
  def pbChoosePokemon(helptext=nil)
    @scene.pbSetHelpText(helptext) if helptext
    return @scene.pbChoosePokemon
  end
  def pbPokemonGiveScreen(item)
    @scene.pbStartScene(@party,_INTL("给哪个宝可梦？"))
    pkmnid = @scene.pbChoosePokemon
    ret = false
    if pkmnid>=0
      ret = pbGiveItemToPokemon(item,@party[pkmnid],self,pkmnid)
    end
    pbRefreshSingle(pkmnid)
    @scene.pbEndScene
    return ret
  end
  def pbPokemonGiveMailScreen(mailIndex)
    @scene.pbStartScene(@party,_INTL("给哪个宝可梦？"))
    pkmnid = @scene.pbChoosePokemon
    if pkmnid>=0
      pkmn = @party[pkmnid]
      if pkmn.hasItem? || pkmn.mail
        pbDisplay(_INTL("宝可梦已经携带道具了。\n不能携带邮件。"))
      elsif pkmn.egg?
        pbDisplay(_INTL("宝可梦蛋不能携带邮件。"))
      else
        pbDisplay(_INTL("邮件从邮箱传过来了。"))
        pkmn.mail = $PokemonGlobal.mailbox[mailIndex]
        pkmn.setItem(pkmn.mail.item)
        $PokemonGlobal.mailbox.delete_at(mailIndex)
        pbRefreshSingle(pkmnid)
      end
    end
    @scene.pbEndScene
  end
  def pbEndScene
    @scene.pbEndScene
  end
  def pbUpdate
    @scene.update
  end
  def pbHardRefresh
    @scene.pbHardRefresh
  end
  def pbRefresh
    @scene.pbRefresh
  end
  def pbRefreshSingle(i)
    @scene.pbRefreshSingle(i)
  end
  def pbDisplay(text)
    @scene.pbDisplay(text)
  end
  def pbConfirm(text)
    return @scene.pbDisplayConfirm(text)
  end
  def pbShowCommands(helptext,commands,index=0)
    return @scene.pbShowCommands(helptext,commands,index)
  end
  # Checks for identical species
  def pbCheckSpecies(array)   # Unused
    for i in 0...array.length
      for j in i+1...array.length
        return false if array[i].species==array[j].species
      end
    end
    return true
  end
  # Checks for identical held items
  def pbCheckItems(array)   # Unused
    for i in 0...array.length
      next if !array[i].hasItem?
      for j in i+1...array.length
        return false if array[i].item==array[j].item
      end
    end
    return true
  end
  def pbSwitch(oldid,newid)
    if oldid!=newid
      @scene.pbSwitchBegin(oldid,newid)
      tmp = @party[oldid]
      @party[oldid] = @party[newid]
      @party[newid] = tmp
      @scene.pbSwitchEnd(oldid,newid)
    end
  end
  def pbChooseMove(pokemon,helptext,index=0)
    movenames = []
    for i in pokemon.moves
      break if i.id==0
      if i.totalpp<=0
        movenames.push(_INTL("{1} (PP：---)",PBMoves.getName(i.id)))
      else
        movenames.push(_INTL("{1} (PP：{2}/{3})",PBMoves.getName(i.id),i.pp,i.totalpp))
      end
    end
    return @scene.pbShowCommands(helptext,movenames,index)
  end
  def pbRefreshAnnotations(ableProc)   # For after using an evolution stone
    return if !@scene.pbHasAnnotations?
    annot = []
    for pkmn in @party
      elig = ableProc.call(pkmn)
      annot.push((elig) ? _INTL("可以使用") : _INTL("无效"))
    end
    @scene.pbAnnotate(annot)
  end
  def pbClearAnnotations
    @scene.pbAnnotate(nil)
  end
  def pbPokemonMultipleEntryScreenEx(ruleset)
    annot = []
    statuses = []
    ordinals = [
       _INTL("不符合"),
       _INTL("未进入"),
       _INTL("禁止"),
       _INTL("第一"),
       _INTL("第二"),
       _INTL("第三"),
       _INTL("第四"),
       _INTL("第五"),
       _INTL("第六")
    ]
    return nil if !ruleset.hasValidTeam?(@party)
    ret = nil
    addedEntry = false
    for i in 0...@party.length
      statuses[i] = (ruleset.isPokemonValid?(@party[i])) ? 1 : 2
    end
    for i in 0...@party.length
      annot[i] = ordinals[statuses[i]]
    end
    @scene.pbStartScene(@party,_INTL("选择宝可梦或取消。"),annot,true)
    loop do
      realorder = []
      for i in 0...@party.length
        for j in 0...@party.length
          if statuses[j]==i+3
            realorder.push(j)
            break
          end
        end
      end
      for i in 0...realorder.length
        statuses[realorder[i]] = i+3
      end
      for i in 0...@party.length
        annot[i] = ordinals[statuses[i]]
      end
      @scene.pbAnnotate(annot)
      if realorder.length==ruleset.number && addedEntry
        @scene.pbSelect(6)
      end
      @scene.pbSetHelpText(_INTL("选择宝可梦或取消。"))
      pkmnid = @scene.pbChoosePokemon
      addedEntry = false
      if pkmnid==6   # Confirm was chosen
        ret = []
        for i in realorder; ret.push(@party[i]); end
        error = []
        break if ruleset.isValid?(ret,error)
        pbDisplay(error[0])
        ret = nil
      end
      break if pkmnid<0   # Cancelled
      cmdEntry   = -1
      cmdNoEntry = -1
      cmdSummary = -1
      commands = []
      if (statuses[pkmnid] || 0) == 1
        commands[cmdEntry = commands.length]   = _INTL("参加")
      elsif (statuses[pkmnid] || 0) > 2
        commands[cmdNoEntry = commands.length] = _INTL("退出")
      end
      pkmn = @party[pkmnid]
      commands[cmdSummary = commands.length]   = _INTL("查看能力")
      commands[commands.length]                = _INTL("退出")
      command = @scene.pbShowCommands(_INTL("要对{1}做什么？",pkmn.name),commands) if pkmn
      if cmdEntry>=0 && command==cmdEntry
        if realorder.length>=ruleset.number && ruleset.number>0
          pbDisplay(_INTL("最多只能让{1}只宝可梦参加。",ruleset.number))
        else
          statuses[pkmnid] = realorder.length+3
          addedEntry = true
          pbRefreshSingle(pkmnid)
        end
      elsif cmdNoEntry>=0 && command==cmdNoEntry
        statuses[pkmnid] = 1
        pbRefreshSingle(pkmnid)
      elsif cmdSummary>=0 && command==cmdSummary
        @scene.pbSummary(pkmnid) {
          @scene.pbSetHelpText((@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"))
        }
      end
    end
    @scene.pbEndScene
    return ret
  end
  def pbChooseAblePokemon(ableProc,allowIneligible=false)
    annot = []
    eligibility = []
    for pkmn in @party
      elig = ableProc.call(pkmn)
      eligibility.push(elig)
      annot.push((elig) ? _INTL("可以授予") : _INTL("不能被授予"))
    end
    ret = -1
    @scene.pbStartScene(@party,
       (@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"),annot)
    loop do
      @scene.pbSetHelpText(
         (@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"))
      pkmnid = @scene.pbChoosePokemon
      break if pkmnid<0
      if !eligibility[pkmnid] && !allowIneligible
        pbDisplay(_INTL("这个宝可梦不能参加。"))
      else
        ret = pkmnid
        break
      end
    end
    @scene.pbEndScene
    return ret
  end
  def pbChooseTradablePokemon(ableProc,allowIneligible=false)
    annot = []
    eligibility = []
    for pkmn in @party
      elig = ableProc.call(pkmn)
      elig = false if pkmn.egg? || pkmn.shadowPokemon?
      eligibility.push(elig)
      annot.push((elig) ? _INTL("可以使用") : _INTL("无效"))
    end
    ret = -1
    @scene.pbStartScene(@party,
       (@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"),annot)
    loop do
      @scene.pbSetHelpText(
         (@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"))
      pkmnid = @scene.pbChoosePokemon
      break if pkmnid<0
      if !eligibility[pkmnid] && !allowIneligible
        pbDisplay(_INTL("这个宝可梦不能参加。"))
      else
        ret = pkmnid
        break
      end
    end
    @scene.pbEndScene
    return ret
  end
  def pbPokemonScreen
    @scene.pbStartScene(@party,
                       (@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"),
                        nil,false,$Trainer.pokepc)
    loop do
      @scene.pbSetHelpText((@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"))
      pkmnid = @scene.pbChoosePokemon(false,-1,1)
      break if (pkmnid.is_a?(Numeric) && pkmnid<0) || (pkmnid.is_a?(Array) && pkmnid[1]<0)
      if pkmnid.is_a?(Array) && pkmnid[0]==1   # Switch
        @scene.pbSetHelpText(_INTL("移动到哪里？"))
        oldpkmnid = pkmnid[1]
        pkmnid = @scene.pbChoosePokemon(true,-1,2)
        if pkmnid>=0 && pkmnid!=oldpkmnid
          pbSwitch(oldpkmnid,pkmnid)
        end
        next
      end
      pkmn = @party[pkmnid]
      ret = pbCheckEvolutionEx(pkmn) { |pkmn, method, parameter, new_species|
        success = PBEvolution.call("levelUpCheck", method, pkmn, parameter)
        next (success) ? new_species : -1
      }
      commands   = []
      cmdSummary = -1
      cmdPokedex = -1
      cmdNickname= -1
      cmdEvolution = -1
      cmdRelearn = -1
      cmdDebug   = -1
      cmdMoves   = [-1,-1,-1,-1]
      cmdSwitch  = -1
      cmdMail    = -1
      cmdItem    = -1
      # Build the commands 
      commands[cmdSummary = commands.length]      = _INTL("查看能力")
      commands[cmdPokedex = commands.length]       = _INTL("查看图鉴") if $Trainer.pokedex && !pkmn.egg?
      commands[cmdSwitch = commands.length]       = _INTL("移动位置") if @party.length>1
      commands[cmdNickname = commands.length]     = _INTL("昵称") if !pkmn.egg? && $Trainer.id==pkmn.trainerID
      commands[cmdEvolution = commands.length]     = _INTL("进化") if ret>0 #&& pkmn.species!=@party[0].species # Only if it's an egg
      commands[cmdRelearn = commands.length]      = _INTL("招式") if !pkmn.egg?
      for i in 0...pkmn.moves.length
        move = pkmn.moves[i]
        # Check for hidden moves and add any that were found
        if !pkmn.egg? && (isConst?(move.id,PBMoves,:MILKDRINK) ||
                          isConst?(move.id,PBMoves,:SOFTBOILED) ||
                          HiddenMoveHandlers.hasHandler(move.id))
          commands[cmdMoves[i] = commands.length] = [PBMoves.getName(move.id),1]
        end
      end
      if !pkmn.egg?
        if pkmn.mail
          commands[cmdMail = commands.length]     = _INTL("邮件")
        else
          commands[cmdItem = commands.length]     = _INTL("道具")
        end
      end
      commands[cmdDebug = commands.length]        = _INTL("调试") if $DEBUG
      commands[commands.length]                   = _INTL("退出")
      command = @scene.pbShowCommands(_INTL("要对{1}做什么？",pkmn.name),commands)
      havecommand = false
      for i in 0...4
        if cmdMoves[i]>=0 && command==cmdMoves[i]
          havecommand = true
          if isConst?(pkmn.moves[i].id,PBMoves,:SOFTBOILED) ||
             isConst?(pkmn.moves[i].id,PBMoves,:MILKDRINK)
            amt = [(pkmn.totalhp/5).floor,1].max
            if pkmn.hp<=amt
              pbDisplay(_INTL("没有足够的HP..."))
              break
            end
            @scene.pbSetHelpText(_INTL("要对哪只宝可梦使用？"))
            oldpkmnid = pkmnid
            loop do
              @scene.pbPreSelect(oldpkmnid)
              pkmnid = @scene.pbChoosePokemon(true,pkmnid)
              break if pkmnid<0
              newpkmn = @party[pkmnid]
              movename = PBMoves.getName(pkmn.moves[i].id)
              if pkmnid==oldpkmnid
                pbDisplay(_INTL("{1}不能对自己使用{2}！",pkmn.name,movename))
              elsif newpkmn.egg?
                pbDisplay(_INTL("{1}不能对宝可梦蛋使用！",movename))
              elsif newpkmn.hp==0 || newpkmn.hp==newpkmn.totalhp
                pbDisplay(_INTL("{1}不能对这只宝可梦使用！",movename))
              else
                pkmn.hp -= amt
                hpgain = pbItemRestoreHP(newpkmn,amt)
                @scene.pbDisplay(_INTL("{1}的HP恢复了{2}点。",newpkmn.name,hpgain))
                pbRefresh
              end
              break if pkmn.hp<=amt
            end
            @scene.pbSelect(oldpkmnid)
            pbRefresh
            break
          elsif pbCanUseHiddenMove?(pkmn,pkmn.moves[i].id)
            if pbConfirmUseHiddenMove(pkmn,pkmn.moves[i].id)
              @scene.pbEndScene
              if isConst?(pkmn.moves[i].id,PBMoves,:FLY)
                scene = PokemonRegionMap_Scene.new(-1,false)
                screen = PokemonRegionMapScreen.new(scene)
                ret = screen.pbStartFlyScreen
                if ret
                  $PokemonTemp.flydata=ret
                  return [pkmn,pkmn.moves[i].id]
                end
                @scene.pbStartScene(@party,
                   (@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"))
                break
              end
              return [pkmn,pkmn.moves[i].id]
            end
          else
            break
          end
        end
      end
      next if havecommand
      if cmdSummary>=0 && command==cmdSummary
        @scene.pbSummary(pkmnid) {
          @scene.pbSetHelpText((@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"))
        }
#查看图鉴
      elsif cmdPokedex>=0 && command==cmdPokedex
        pbUpdateLastSeenForm(pkmn)
        pbFadeOutIn {
          scene = PokemonPokedexInfo_Scene.new
          screen = PokemonPokedexInfoScreen.new(scene)
          screen.pbStartSceneSingle(pkmn.species)
        }
        dorefresh = true
      elsif cmdDebug>=0 && command==cmdDebug
        pbPokemonDebug(pkmn,pkmnid)
      elsif cmdSwitch>=0 && command==cmdSwitch
        @scene.pbSetHelpText(_INTL("移动到哪里？"))
        oldpkmnid = pkmnid
        pkmnid = @scene.pbChoosePokemon(true)
        if pkmnid>=0 && pkmnid!=oldpkmnid
          pbSwitch(oldpkmnid,pkmnid)
        end
        elsif cmdNickname>=0 && command==cmdNickname
        speciesname = PBSpecies.getName(pkmn.species)
        oldname = (pkmn.name && pkmn.name!=speciesname) ? pkmn.name : ""
        newname = pbEnterPokemonName(_INTL("{1}的昵称是？",speciesname),
            0,PokeBattle_Pokemon::MAX_POKEMON_NAME_SIZE,oldname,pkmn)
        if newname && newname!=""
          pkmn.name = newname
          pbRefreshSingle(pkmnid)
        elsif newname && newname==""
          pkmn.name = speciesname
          pbRefreshSingle(pkmnid)
        end
      elsif cmdEvolution>=0 && command==cmdEvolution
          if $game_system.playing_bgm
          bgm = $game_system.playing_bgm.name
          volume = $PokemonSystem.bgmvolume
        end
        evo = PokemonEvolutionScene.new
        evo.pbStartScreen(pkmn,ret)
        evo.pbEvolution(true)
        evo.pbEndScreen
        if bgm
          pbBGMPlay(bgm, volume)
        end
        elsif cmdRelearn>=0 && command==cmdRelearn
        command = 0
        loop do
          command = @scene.pbShowCommands(_INTL("要对{1}的招式做什么？", pkmn.name),
             [_INTL("回忆"), _INTL("忘记"), _INTL("取消")], command)
          case command
          when 0
            if pbHasRelearnableMove?(pkmn)
              pbRelearnMoveScreen(pkmn)
              pbHardRefresh
              pbRefreshSingle(pkmnid)
            else
              pbMessage(_INTL("{1}没有可以回忆的招式。",pkmn.name))
            end
          when 1
            if pkmn.moves[1].id > 0
              moveindex = pbChooseMove(pkmn,_INTL("选择需要忘记的招式。"))
              if moveindex >= 0
                movename = PBMoves.getName(pkmn.moves[moveindex].id)
                pkmn.pbDeleteMoveAtIndex(moveindex)
                pbDisplay(_INTL("{1}忘记了{2}。", pkmn.name, movename))
                pbHardRefresh
              end
            else
              pbDisplay(_INTL("{1}不想再忘记招式了！",pkmn.name))
            end
          else
            break
          end
        end
      elsif cmdMail>=0 && command==cmdMail
        command = @scene.pbShowCommands(_INTL("要对邮件做什么？"),
           [_INTL("阅读"),_INTL("Take"),_INTL("Cancel")])
        case command
        when 0   # Read
          pbFadeOutIn {
            pbDisplayMail(pkmn.mail,pkmn)
            @scene.pbSetHelpText((@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"))
          }
        when 1   # Take
          if pbTakeItemFromPokemon(pkmn,self)
            pbRefreshSingle(pkmnid)
          end
        end
      elsif cmdItem>=0 && command==cmdItem
        itemcommands = []
        cmdUseItem   = -1
        cmdGiveItem  = -1
        cmdTakeItem  = -1
        cmdMoveItem  = -1
        # Build the commands
        itemcommands[cmdUseItem=itemcommands.length]  = _INTL("使用")
        itemcommands[cmdGiveItem=itemcommands.length] = _INTL("携带")
        itemcommands[cmdTakeItem=itemcommands.length] = _INTL("收回") if pkmn.hasItem?
        itemcommands[cmdMoveItem=itemcommands.length] = _INTL("移动") if pkmn.hasItem? && !pbIsMail?(pkmn.item)
        itemcommands[itemcommands.length]             = _INTL("退出")
        command = @scene.pbShowCommands(_INTL("用道具做什么？"),itemcommands)
        if cmdUseItem>=0 && command==cmdUseItem   # Use
          item = @scene.pbUseItem($PokemonBag,pkmn) {
            @scene.pbSetHelpText((@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"))
          }
          if item>0
            pbUseItemOnPokemon(item,pkmn,self)
            pbRefreshSingle(pkmnid)
          end
        elsif cmdGiveItem>=0 && command==cmdGiveItem   # Give
          item = @scene.pbChooseItem($PokemonBag) {
            @scene.pbSetHelpText((@party.length>1) ? _INTL("请选择宝可梦。") : _INTL("选择宝可梦或取消。"))
          }
          if item>0
            if pbGiveItemToPokemon(item,pkmn,self,pkmnid)
              pbRefreshSingle(pkmnid)
            end
          end
        elsif cmdTakeItem>=0 && command==cmdTakeItem   # Take
          if pbTakeItemFromPokemon(pkmn,self)
            pbRefreshSingle(pkmnid)
          end
        elsif cmdMoveItem>=0 && command==cmdMoveItem   # Move
          item = pkmn.item
          itemname = PBItems.getName(item)
          @scene.pbSetHelpText(_INTL("要将{1}移动到哪里？",itemname))
          oldpkmnid = pkmnid
          loop do
            @scene.pbPreSelect(oldpkmnid)
            pkmnid = @scene.pbChoosePokemon(true,pkmnid)
            break if pkmnid<0
            newpkmn = @party[pkmnid]
            if pkmnid==oldpkmnid
              break
            elsif newpkmn.egg?
              pbDisplay(_INTL("宝可梦蛋不能携带物品。"))
            elsif !newpkmn.hasItem?
              newpkmn.setItem(item)
              pkmn.setItem(0)
              @scene.pbClearSwitching
              pbRefresh
              pbDisplay(_INTL("{1}已交给{2}携带。",newpkmn.name,itemname))
              break
            elsif pbIsMail?(newpkmn.item)
              pbDisplay(_INTL("必须先拿回{1}身上的邮件，\n然后才能给予道具。",newpkmn.name))
            else
              newitem = newpkmn.item
              newitemname = PBItems.getName(newitem)
              if isConst?(newitem,PBItems,:LEFTOVERS)
                pbDisplay(_INTL("{1} is already holding some {2}.\1",newpkmn.name,newitemname))
              elsif newitemname.starts_with_vowel?
                pbDisplay(_INTL("{1} is already holding an {2}.\1",newpkmn.name,newitemname))
              else
                pbDisplay(_INTL("{1} is already holding a {2}.\1",newpkmn.name,newitemname))
              end
              if pbConfirm(_INTL("想要交换这两个道具吗？"))
                newpkmn.setItem(item)
                pkmn.setItem(newitem)
                @scene.pbClearSwitching
                pbRefresh
                pbDisplay(_INTL("{1}已交给{2}携带。",newpkmn.name,itemname))
                pbDisplay(_INTL("{1}已交给{2}携带。",pkmn.name,newitemname))
                break
              end
            end
          end
        end
      end
    end
    @scene.pbEndScene
    return nil
  end
end
def pbPokemonScreen
  pbFadeOutIn {
    sscene = PokemonParty_Scene.new
    sscreen = PokemonPartyScreen.new(sscene,$Trainer.party)
    sscreen.pbPokemonScreen
  }
end

class PokemonPartyExpBar < SpriteWrapper
  attr_reader :pokemon
  attr_reader :animating

  EXP_BAR_FILL_TIME = 1.75 

  def initialize(viewport=nil, pokemon=nil)
    super(viewport)
    @pokemon = pokemon
    @animating = false
    @currentExp = 0
    @endExp = 0
    @rangeExp = 0
    @expBarBitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/Party/overlay_exp"))
    @expBar = SpriteWrapper.new(viewport)
    @expBar.bitmap = @expBarBitmap.bitmap
    @expBar.src_rect.height = @expBarBitmap.height
    @expBar.x = 95      # 横坐标
    @expBar.y = 215     # 纵坐标 
    
    self.z = viewport.z + 1
    
    refresh
  end

  def dispose
    @expBar.dispose if @expBar && !@expBar.disposed?
    @expBarBitmap.dispose if @expBarBitmap && !@expBarBitmap.disposed?
    super
  end

  def x=(value)
    super
    @expBar.x = value
  end

  def y=(value)
    super
    @expBar.y = value
  end

  def z=(value)
    super
    @expBar.z = value
  end

  def visible=(value)
    super
    @expBar.visible = value
  end

  def color=(value)
    super
    @expBar.color = value
  end

  def pokemon=(value)
    @pokemon = value
    refresh
  end

  def expFraction
    return 0 if !@pokemon || @pokemon.egg?
    return (@animating) ? @currentExp.to_f / @rangeExp : @pokemon.expFraction
  end

  def animateExp(oldExp, newExp, rangeExp)
    @currentExp = oldExp
    @endExp = newExp
    @rangeExp = rangeExp
    @expIncPerFrame = rangeExp / (EXP_BAR_FILL_TIME * 40)
    @animating = true
  end

  def refresh
    return if !@pokemon || @pokemon.egg?
    
    w = self.expFraction * @expBarBitmap.width
    return if w.nan?
    
    w = ((w / 2.0).round) * 2
    @expBar.src_rect.width = w
  end

  def update
    super
    if @animating
      if @currentExp < @endExp
        @currentExp += @expIncPerFrame
        @currentExp = @endExp if @currentExp >= @endExp
      elsif @currentExp > @endExp
        @currentExp -= @expIncPerFrame
        @currentExp = @endExp if @currentExp <= @endExp
      end
      
      refresh
      @animating = false if @currentExp == @endExp
    end
  end
end
