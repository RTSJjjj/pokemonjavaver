#==============================================================================
# PScreen Bag Graphical Overhaul by LackDeJurane or CharizardThree3
# If used please give credits.
#=============================================================================
USEARROWS = false   # Whether to use BW Styled Arrows for pocket changing 
ANIMEBG   = true  # Whether to use an Animated Background for the bag
USETOUCHBAG = false# Whether to use mouse to click and change pockets

class Window_PokemonBag < Window_DrawableCommand
  attr_reader :pocket
  attr_accessor :sorting

  def initialize(bag,filterlist,pocket,x,y,width,height)
    @bag        = bag
    @filterlist = filterlist
    @pocket     = pocket
    @sorting = false
    @adapter = PokemonMartAdapter.new
    super(x,y,width,height)
    @selarrow  = AnimatedBitmap.new("Graphics/Pictures/Bag/cursor")
    @swaparrow = AnimatedBitmap.new("Graphics/Pictures/Bag/cursor_swap")
    self.windowskin = nil
  end

  def dispose
    @swaparrow.dispose
    super
  end
  
  def pocket=(value)
    @pocket = value
    @item_max = (@filterlist) ? @filterlist[@pocket].length+1 : @bag.pockets[@pocket].length+1
    self.index = @bag.getChoice(@pocket)
  end
  
  def page_row_max; return PokemonBag_Scene::ITEMSVISIBLE; end
  def page_item_max; return PokemonBag_Scene::ITEMSVISIBLE; end

  def item
    return 0 if @filterlist && !@filterlist[@pocket][self.index]
    thispocket = @bag.pockets[@pocket]
    item = (@filterlist) ? thispocket[@filterlist[@pocket][self.index]] : thispocket[self.index]
    return (item) ? item[0] : 0
  end

  def itemCount
    return (@filterlist) ? @filterlist[@pocket].length+1 : @bag.pockets[@pocket].length+1
  end

  def itemRect(item)
    if item<0 || item>=@item_max || item<self.top_item-1 ||
       item>self.top_item+self.page_item_max
      return Rect.new(0,0,0,0)
    else
      cursor_width = (self.width-self.borderX-(@column_max-1)*@column_spacing) / @column_max
      x = item % @column_max * (cursor_width + @column_spacing)
      y = item / @column_max * @row_height - @virtualOy
      return Rect.new(x, y, cursor_width, @row_height)
    end
  end

  def drawCursor(index,rect)
    if self.index==index
      bmp = (@sorting) ? @swaparrow.bitmap : @selarrow.bitmap
      pbCopyBitmap(self.contents,bmp,rect.x+12,rect.y+2)
    end
  end

  def drawItemIcon(x, y, item, contents)
    @itemicons = {} if !@itemicons
    new_rect = Rect.new(0, 0, 24, 24)
    if !@itemicons["#{item}"]
      itemicon = AnimatedBitmap.new(pbItemIconFile(item))
      @itemicons["#{item}"] = Bitmap.new(24, 24)
      @itemicons["#{item}"].stretch_blt(new_rect, itemicon.bitmap, Rect.new(0, 0, 48, 48))
    end
    contents.blt(x, y, @itemicons["#{item}"], new_rect)
  end
  
  def drawItem(index,count,rect)
    textpos = []
    rect = Rect.new(rect.x+16,rect.y+16,rect.width-16,rect.height)
    ypos = rect.y+4
    thispocket = @bag.pockets[@pocket]
    if index==self.itemCount-1
      textpos.push([_INTL("关闭背包"),rect.x+28,ypos,false,self.baseColor,self.shadowColor])
    else
      item = (@filterlist) ? thispocket[@filterlist[@pocket][index]][0] : thispocket[index][0]
      baseColor   = self.baseColor
      shadowColor = self.shadowColor
      if @sorting && index==self.index
        baseColor   = Color.new(224,0,0)
        shadowColor = Color.new(248,144,144)
      end

      drawItemIcon(rect.x, ypos+2, item, self.contents)
      textpos.push(
         [@adapter.getDisplayName(item),rect.x+28,ypos,false,baseColor,shadowColor]
      )
      if !pbIsImportantItem?(item)   # Not a Key item or HM (or infinite TM)
        qty = (@filterlist) ? thispocket[@filterlist[@pocket][index]][1] : thispocket[index][1]
        qtytext = _ISPRINTF("x{1: 3d}",qty)
        xQty    = rect.x+rect.width-self.contents.text_size(qtytext).width-16
        textpos.push([qtytext,xQty,ypos,false,baseColor,shadowColor])
      end
      if pbIsImportantItem?(item)
        if @bag.pbIsRegistered?(item)
          pbDrawImagePositions(self.contents,[
             ["Graphics/Pictures/Bag/icon_register",rect.x+rect.width-64,ypos+4,0,0,-1,24]
          ])
        elsif pbCanRegisterItem?(item)
          pbDrawImagePositions(self.contents,[
             ["Graphics/Pictures/Bag/icon_register",rect.x+rect.width-64,ypos+4,0,24,-1,24]
          ])
        end
      end
    end
    pbDrawTextPositions(self.contents,textpos)
  end
  

  def refresh
    @item_max = itemCount()
    self.update_cursor_rect
    dwidth  = self.width-self.borderX
    dheight = self.height-self.borderY
    self.contents = pbDoEnsureBitmap(self.contents,dwidth,dheight)
    self.contents.clear
    for i in 0...@item_max
      next if i<self.top_item-1 || i>self.top_item+self.page_item_max
      drawItem(i,@item_max,itemRect(i))
    end
    drawCursor(self.index,itemRect(self.index))
  end

  def update
    super
    @uparrow.visible   = false
    @downarrow.visible = false
  end
end
  

#===============================================================================
# Bag visuals
#===============================================================================
class PokemonBag_Scene
  ITEMLISTBASECOLOR     = Color.new(255,255,255)
  ITEMLISTSHADOWCOLOR   = Color.new(156,156,156)
  ITEMTEXTBASECOLOR     = Color.new(248,248,248)
  ITEMTEXTSHADOWCOLOR   = Color.new(90,90,90)
  POCKETNAMEBASECOLOR   = Color.new(248,248,248)
  POCKETNAMESHADOWCOLOR = Color.new(90,90,90)
  ITEMSVISIBLE          = 9

  def pbUpdate
    pbUpdateSpriteHash(@sprites)
  end

  def pbStartScene(bag,choosing=false,filterproc=nil,resetpocket=true)
    @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z = 99999
    @bag        = bag
    @choosing   = choosing
    @filterproc = filterproc
    pbRefreshFilter
    lastpocket = @bag.lastpocket
    numfilledpockets = @bag.pockets.length-1
    if @choosing
      numfilledpockets = 0
      if @filterlist!=nil
        for i in 1...@bag.pockets.length
          numfilledpockets += 1 if @filterlist[i].length>0
        end
      else
        for i in 1...@bag.pockets.length
          numfilledpockets += 1 if @bag.pockets[i].length>0
        end
      end
      lastpocket = (resetpocket) ? 1 : @bag.lastpocket
      if (@filterlist && @filterlist[lastpocket].length==0) ||
         (!@filterlist && @bag.pockets[lastpocket].length==0)
        for i in 1...@bag.pockets.length
          if @filterlist && @filterlist[i].length>0
            lastpocket = i; break
          elsif !@filterlist && @bag.pockets[i].length>0
            lastpocket = i; break
          end
        end
      end
    end
    @bag.lastpocket = lastpocket
    @sliderbitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/Bag/icon_slider"))
    @pocketbitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/Bag/icon_pocket"))
    @sprites = {}
    @arrowanim=0
    lastitem=@bag.getChoice(lastpocket) 
    @sprites["grid"]=AnimatedPlane.new(@viewport) 
  if $Trainer.isFemale?
    @sprites["grid"]=AnimatedPlane.new(@viewport)  
    @sprites["grid"].bitmap = Bitmap.new("Graphics/Pictures/Bag/bg_gridf")
  else
    @sprites["grid"]=AnimatedPlane.new(@viewport)
    @sprites["grid"].bitmap = Bitmap.new("Graphics/Pictures/Bag/bg_grid") 
  end  
    @sprites["bagsprite"] = IconSprite.new(-30,10,@viewport)
    @sprites["background"] = IconSprite.new(0,0,@viewport)
    @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
    pbSetSystemFont(@sprites["overlay"].bitmap)
    if !@choosing
      @sprites["overlay2"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
      pbSetSmallFont(@sprites["overlay2"].bitmap)
      pbDrawTextPositions(@sprites["overlay2"].bitmap,[
         ["[Z]:手动 [Shift]:自动",92+16,8,2,POCKETNAMEBASECOLOR,POCKETNAMESHADOWCOLOR]
      ])
    end
    @sprites["pocketicon"] = BitmapSprite.new(186,32,@viewport)
    @sprites["pocketicon"].x = 0
    @sprites["pocketicon"].y = -3
    @sprites["switchL"]=IconSprite.new(0,0,@viewport)
    @sprites["switchL"].setBitmap("Graphics/Pictures/Bag/switchL")
    @sprites["switchL"].x=2
    @sprites["switchL"].y=180
    @sprites["switchR"]=IconSprite.new(0,0,@viewport)
    @sprites["switchR"].setBitmap("Graphics/Pictures/Bag/switchR")
    @sprites["switchR"].x=162
    @sprites["switchR"].y=180
  if USEARROWS
    @sprites["switchR"].visible = true 
    @sprites["switchL"].visible = true
  else
    @sprites["switchR"].visible = false 
    @sprites["switchL"].visible = false
  end
    #@sprites["slider"]=IconSprite.new(Graphics.width-24,240,@viewport)
    #@sprites["slider"].setBitmap(sprintf("Graphics/Pictures/Bag/bagSlider"))
    @sprites["itemlist"] = Window_PokemonBag.new(@bag,@filterlist,lastpocket,218-32+64,-8,326+32,80+ITEMSVISIBLE*32)
    @sprites["itemlist"].viewport    = @viewport
    @sprites["itemlist"].pocket      = lastpocket
    @sprites["itemlist"].index       = @bag.getChoice(lastpocket)
    @sprites["itemlist"].baseColor   = ITEMLISTBASECOLOR
    @sprites["itemlist"].shadowColor = ITEMLISTSHADOWCOLOR
    @sprites["itemlist"].refresh
    @sprites["itemicon"] = ItemIconSprite.new(48,Graphics.height-48,-1,@viewport)
    @sprites["itemtext"] = Window_UnformattedTextPokemon.new("")
    @sprites["itemtext"].x           = 84
    @sprites["itemtext"].y           = 270+64
    @sprites["itemtext"].width       = Graphics.width-84-24
    @sprites["itemtext"].height      = 128
    @sprites["itemtext"].baseColor   = ITEMTEXTBASECOLOR
    @sprites["itemtext"].shadowColor = ITEMTEXTSHADOWCOLOR
    @sprites["itemtext"].visible     = true
    @sprites["itemtext"].viewport    = @viewport
    @sprites["itemtext"].windowskin  = nil
    @sprites["helpwindow"] = Window_UnformattedTextPokemon.new("")
    @sprites["helpwindow"].visible  = false
    @sprites["helpwindow"].viewport = @viewport
    @sprites["msgwindow"] = Window_AdvancedTextPokemon.new("")
    @sprites["msgwindow"].visible  = false
    @sprites["msgwindow"].viewport = @viewport
    pbBottomLeftLines(@sprites["helpwindow"],1)
    pbDeactivateWindows(@sprites)
    pbRefresh
    pbFadeInAndShow(@sprites)
  end

  def pbEndScene
    pbFadeOutAndHide(@sprites)
    pbDisposeSpriteHash(@sprites)
    @sliderbitmap.dispose
    @pocketbitmap.dispose
    @viewport.dispose
  end
  
 
def showPocketAnimation
  if $Trainer.isFemale?
    @sprites["bagsprite"].setBitmap("Graphics/Pictures/Bag/bag_#{@bag.lastpocket}_fm") 
    pbWait(3) 
    @sprites["bagsprite"].setBitmap("Graphics/Pictures/Bag/bag_#{@bag.lastpocket}_f")
  else
    @sprites["bagsprite"].setBitmap("Graphics/Pictures/Bag/bag_#{@bag.lastpocket}m")
    pbWait(3)
    @sprites["bagsprite"].setBitmap("Graphics/Pictures/Bag/bag_#{@bag.lastpocket}")
  end   
end


  def pbDisplay(msg,brief=false)
    UIHelper.pbDisplay(@sprites["msgwindow"],msg,brief) { pbUpdate }
  end

  def pbConfirm(msg)
    UIHelper.pbConfirm(@sprites["msgwindow"],msg) { pbUpdate }
  end

  def pbChooseNumber(helptext,maximum,initnum=1)
    return UIHelper.pbChooseNumber(@sprites["helpwindow"],helptext,maximum,initnum) { pbUpdate }
  end

  def pbShowCommands(helptext,commands,initcmd=0)
    return UIHelper.pbShowCommands(@sprites["helpwindow"],helptext,commands,initcmd) { pbUpdate }
  end
  
  def pbRefresh
    # Set the background image
    if $Trainer.isFemale?
      @sprites["background"].setBitmap(sprintf("Graphics/Pictures/Bag/bg_f"))
    else
      @sprites["background"].setBitmap(sprintf("Graphics/Pictures/Bag/bg"))
    end
    # Set the bag sprite
    fbagexists = pbResolveBitmap(sprintf("Graphics/Pictures/Bag/bag_#{@bag.lastpocket}_f"))
    if $Trainer.isFemale? && fbagexists
    @sprites["bagsprite"].setBitmap("Graphics/Pictures/Bag/bag_#{@bag.lastpocket}_f")
    else
    @sprites["bagsprite"].setBitmap("Graphics/Pictures/Bag/bag_#{@bag.lastpocket}")
  end
    # Draw the pocket icons
    @sprites["pocketicon"].bitmap.clear
    if @choosing && @filterlist
      for i in 1...@bag.pockets.length
        if @filterlist[i].length==0
          @sprites["pocketicon"].bitmap.blt(6+(i-1)*22,6,
             @pocketbitmap.bitmap,Rect.new((i-1)*20,30,20,20))
        end
      end
    end
    @sprites["pocketicon"].bitmap.blt(2+(@sprites["itemlist"].pocket-1)*0,2,
       @pocketbitmap.bitmap,Rect.new((@sprites["itemlist"].pocket-1)*28,0,28,28))
    # Refresh the item window
    @sprites["itemlist"].refresh
    # Refresh more things
    pbRefreshIndexChanged
  end

  def pbRefreshIndexChanged
    itemlist = @sprites["itemlist"]
    overlay = @sprites["overlay"].bitmap
    overlay.clear
    # Draw the pocket name
    pbDrawTextPositions(overlay,[
       [PokemonBag.pocketNames[@bag.lastpocket],94+16,186,2,POCKETNAMEBASECOLOR,POCKETNAMESHADOWCOLOR]
    ])
    
#=begin
    # Draw slider arrows
    showslider = false
    if itemlist.top_row>0
      overlay.blt(534+64,16,@sliderbitmap.bitmap,Rect.new(0,0,36,38))
      showslider = true
    end
    if itemlist.top_item+itemlist.page_item_max<itemlist.itemCount
      overlay.blt(534+64,228,@sliderbitmap.bitmap,Rect.new(0,38,36,38))
      showslider = true
    end
    if itemlist.top_item+itemlist.page_item_max<itemlist.itemCount
      overlay.blt(534+64,228,@sliderbitmap.bitmap,Rect.new(0,38,36,38))
      showslider = true
    end
#=end   
    # Draw slider box
    if showslider
      sliderheight = 174
      boxheight = (sliderheight*itemlist.page_row_max/itemlist.row_max).floor
      boxheight += [(sliderheight-boxheight)/2,sliderheight/6].min
      boxheight = [boxheight.floor,38].max
      y = 54
      y += ((sliderheight-boxheight)*itemlist.top_row/(itemlist.row_max-itemlist.page_row_max)).floor
      overlay.blt(534+64,y,@sliderbitmap.bitmap,Rect.new(36,0,36,4))
      i = 0; while i*16<boxheight-4-18
        height = [boxheight-4-18-i*16,16].min
        overlay.blt(534+64,y+4+i*16,@sliderbitmap.bitmap,Rect.new(36,4,36,height))
        i += 1
        showslider = false
      end
      overlay.blt(534+64,y+boxheight-18,@sliderbitmap.bitmap,Rect.new(36,20,36,18))
      showslider = false
    end

    # Set the selected item's icon
    @sprites["itemicon"].item = itemlist.item
    # Set the selected item's description
    description = pbGetMessage(MessageTypes::ItemDescriptions,itemlist.item)
    if itemlist.item == getConst(PBItems,:EXPPOT)
      $Trainer.exp_pot = 0 if !$Trainer.exp_pot
      description += _INTL("\n当前积累的经验值：{1}。",$Trainer.exp_pot)
    end
    @sprites["itemtext"].text = (itemlist.item==0) ? _INTL("关闭背包") : description
  end
  
  def pbRefreshFilter
    @filterlist = nil
    return if !@choosing
    if @filterproc!=nil
      @filterlist = []
      for i in 1...@bag.pockets.length
        @filterlist[i] = []
        for j in 0...@bag.pockets[i].length
          @filterlist[i].push(j) if @filterproc.call(@bag.pockets[i][j][0])
        end
      end
    else
    end
  end

  # Called when the item screen wants an item to be chosen from the screen
  def pbChooseItem
    @sprites["helpwindow"].visible = false
    itemwindow = @sprites["itemlist"]
    thispocket = @bag.pockets[itemwindow.pocket]
    swapinitialpos = 1
    pbActivateWindow(@sprites,"itemlist"){
      loop do
        @sprites["grid"].ox+=1 if ANIMEBG
        oldindex = itemwindow.index
        Graphics.update
        Input.update
        pbUpdate
        if itemwindow.sorting && itemwindow.index>=thispocket.length
          itemwindow.index = (oldindex==thispocket.length-1) ? 0 : thispocket.length-1
        end
        if itemwindow.index!=oldindex
          # Move the item being switched
          if itemwindow.sorting
            thispocket.insert(itemwindow.index,thispocket.delete_at(oldindex))
          end
          # Update selected item for current pocket
          @bag.setChoice(itemwindow.pocket,itemwindow.index)
          pbRefresh
        end
        if itemwindow.sorting
          if Input.trigger?(Input::A) ||
             Input.trigger?(Input::C)
            itemwindow.sorting = false
            pbPlayDecisionSE
            pbRefresh
          elsif Input.trigger?(Input::B)
            curindex = itemwindow.index
            thispocket.insert(swapinitialpos,thispocket.delete_at(itemwindow.index))
            itemwindow.index = swapinitialpos
            itemwindow.sorting = false
            pbPlayCancelSE
            pbRefresh
          end
        else       
          # Change pockets
          if Input.trigger?(Input::LEFT) #|| $mouse.leftClick?(@sprites["switchL"])
            newpocket = itemwindow.pocket
            loop do
              newpocket = (newpocket==1) ? PokemonBag.numPockets : newpocket-1
              break if !@choosing || newpocket==itemwindow.pocket
              if @filterlist; break if @filterlist[newpocket].length>0
              else; break if @bag.pockets[newpocket].length>0
              end
            end
            if itemwindow.pocket!=newpocket
              itemwindow.pocket = newpocket
              @bag.lastpocket   = itemwindow.pocket
              thispocket = @bag.pockets[itemwindow.pocket]
              showPocketAnimation
              @sprites["switchL"].setBitmap("Graphics/Pictures/Bag/switchL2") && USEARROWS
              pbWait(5)
              @sprites["switchL"].setBitmap("Graphics/Pictures/Bag/switchL") && USEARROWS
              pbSEPlay("BW2BagSound")
              pbRefresh
            end
            
          elsif Input.trigger?(Input::RIGHT) #|| $mouse.leftClick?(@sprites["switchR"])
            newpocket = itemwindow.pocket
            loop do             
              newpocket = (newpocket==PokemonBag.numPockets) ? 1 : newpocket+1
              break if !@choosing || newpocket==itemwindow.pocket
              if @filterlist; break if @filterlist[newpocket].length>0
              else; break if @bag.pockets[newpocket].length>0
              end
            end
            if itemwindow.pocket!=newpocket
              itemwindow.pocket = newpocket
              @bag.lastpocket   = itemwindow.pocket
              thispocket = @bag.pockets[itemwindow.pocket]
              showPocketAnimation
              @sprites["switchR"].setBitmap("Graphics/Pictures/Bag/switchR2") && USEARROWS
              pbWait(5)
              @sprites["switchR"].setBitmap("Graphics/Pictures/Bag/switchR") && USEARROWS
              pbSEPlay("BW2BagSound")
              pbRefresh
            end
            
      elsif defined?($mouse)     
        if $mouse.leftClick?(@sprites["switchL"]) && USEARROWS
            newpocket = itemwindow.pocket
            loop do
              newpocket = (newpocket==1) ? PokemonBag.numPockets : newpocket-1
              break if !@choosing || newpocket==itemwindow.pocket
              if @filterlist; break if @filterlist[newpocket].length>0
              else; break if @bag.pockets[newpocket].length>0
              end
            end
            if itemwindow.pocket!=newpocket
              itemwindow.pocket = newpocket
              @bag.lastpocket   = itemwindow.pocket
              thispocket = @bag.pockets[itemwindow.pocket]
              showPocketAnimation
              @sprites["switchL"].setBitmap("Graphics/Pictures/Bag/switchL2")
              pbWait(5)
              @sprites["switchL"].setBitmap("Graphics/Pictures/Bag/switchL") 
              pbSEPlay("BW2BagSound")
              pbRefresh
            end
            
          elsif $mouse.leftClick?(@sprites["switchR"]) && USEARROWS
            newpocket = itemwindow.pocket
            loop do             
              newpocket = (newpocket==PokemonBag.numPockets) ? 1 : newpocket+1
              break if !@choosing || newpocket==itemwindow.pocket
              if @filterlist; break if @filterlist[newpocket].length>0
              else; break if @bag.pockets[newpocket].length>0
              end
            end
            if itemwindow.pocket!=newpocket
              itemwindow.pocket = newpocket
              @bag.lastpocket   = itemwindow.pocket
              thispocket = @bag.pockets[itemwindow.pocket]
              showPocketAnimation
              @sprites["switchR"].setBitmap("Graphics/Pictures/Bag/switchR2")
              pbWait(5)
              @sprites["switchR"].setBitmap("Graphics/Pictures/Bag/switchR")
              pbSEPlay("BW2BagSound")
              pbRefresh
            end  
#Using Mouse for changing pocket by clicking on them
      elsif $mouse.areaClick?(0,55,86,118) && USETOUCHBAG
          if itemwindow.pocket!=3
              @bag.lastpocket = 3
              itemwindow.pocket = 3
              showPocketAnimation 
              pbSEPlay("BW2BagSound")
              pbRefresh
            end
        elsif $mouse.areaClick?(0,55,86,118,1) && USETOUCHBAG
          if itemwindow.pocket!=7
              @bag.lastpocket = 7
              itemwindow.pocket = 7
              showPocketAnimation 
              pbSEPlay("BW2BagSound")
              pbRefresh
            end               
        elsif $mouse.areaClick?(86,23,72,100) && USETOUCHBAG
          if itemwindow.pocket!=1
              @bag.lastpocket = 1
              itemwindow.pocket = 1
              showPocketAnimation 
              pbSEPlay("BW2BagSound")
              pbRefresh
            end    
        elsif $mouse.areaClick?(86,23,72,100,1) && USETOUCHBAG
          if itemwindow.pocket!=6
              @bag.lastpocket = 6
              itemwindow.pocket = 6
              showPocketAnimation 
              pbSEPlay("BW2BagSound")
              pbRefresh
            end  
        elsif $mouse.areaClick?(0,209,48,48) && USETOUCHBAG
          if itemwindow.pocket!=2
              @bag.lastpocket = 2
              itemwindow.pocket = 2
              showPocketAnimation 
              pbSEPlay("BW2BagSound")
              pbRefresh
            end 
        elsif $mouse.areaClick?(48,213,68,64) && USETOUCHBAG
          if itemwindow.pocket!=4
              @bag.lastpocket = 4
              itemwindow.pocket = 4
              showPocketAnimation 
              pbSEPlay("BW2BagSound")
              pbRefresh
            end  
        elsif $mouse.inAreaLeft?(120,211,65,42) && USETOUCHBAG 
          if itemwindow.pocket!=5
              @bag.lastpocket = 5
              itemwindow.pocket = 5
              showPocketAnimation 
              pbSEPlay("BW2BagSound")
              pbRefresh
            end  
           elsif $mouse.areaClick?(112,123,73,55) && USETOUCHBAG 
          if itemwindow.pocket!=8
              @bag.lastpocket = 8
              itemwindow.pocket = 8
              showPocketAnimation 
              pbSEPlay("BW2BagSound")
              pbRefresh
            end  
            elsif $mouse.inAreaLeftPress?(472,-120,40,384) && USETOUCHBAG 
             itemlist=@sprites["itemlist"]
             max=itemlist.itemCount-1
             itemlist.index=($mouse.y+5)*max/176
             itemlist.index=max if itemlist.index>max
             itemlist.index=0 if itemlist.index<0
             pbRefresh
           end
          end
          if Input.trigger?(Input::F5) # Register/unregister selected item
            if !@choosing && itemwindow.index<thispocket.length
              if @bag.pbIsRegistered?(itemwindow.item)
                @bag.pbUnregisterItem(itemwindow.item)
              elsif pbCanRegisterItem?(itemwindow.item)
                @bag.pbRegisterItem(itemwindow.item)
              end
              pbSEPlay("BW2MenuChoose")
              pbRefresh
            end
          elsif Input.trigger?(Input::SHIFT) # Auto sort items
            if !@choosing
              if thispocket.length>1 && itemwindow.index<thispocket.length
                thispocket = pbSortItemByCHS(thispocket)
                pbSEPlay("BW2MenuSelect")
                pbRefresh
              end
            end
          elsif Input.trigger?(Input::A) # Start switching the selected item
            if !@choosing
              if thispocket.length>1 && itemwindow.index<thispocket.length &&
                itemwindow.sorting = true
                swapinitialpos = itemwindow.index
                pbSEPlay("BW2MenuSelect")
                pbRefresh
              end
            end
          elsif Input.trigger?(Input::B) # Cancel the item screen
            pbSEPlay("BW2CloseMenu")
            return 0
          elsif Input.trigger?(Input::C) # Choose selected item
            pbPlayDecisionSE
            return itemwindow.item
          end
        end
      end
    }
  end
end
#===============================================================================
# Bag mechanics
#===============================================================================
class PokemonBagScreen
  def initialize(scene,bag)
    @bag   = bag
    @scene = scene
  end

  def pbStartScreen
    @scene.pbStartScene(@bag)
    item = 0
    loop do
      item = @scene.pbChooseItem
      break if item==0
      cmdRead     = -1
      cmdUse      = -1
      cmdRegister = -1
      cmdGive     = -1
      cmdToss     = -1
      cmdDebug    = -1
      commands = []
      # Generate command list
      commands[cmdRead = commands.length]        = _INTL("阅读") if pbIsMail?(item)
      if ItemHandlers.hasOutHandler(item) || (pbIsMachine?(item) && $Trainer.party.length>0)
        if ItemHandlers.hasUseText(item)
          commands[cmdUse = commands.length]     = ItemHandlers.getUseText(item)
        else
          commands[cmdUse = commands.length]     = _INTL("使用")
        end
      end
      commands[cmdGive = commands.length]        = _INTL("给予") if $Trainer.pokemonParty.length>0 && pbCanHoldItem?(item)
      commands[cmdToss = commands.length]        = _INTL("丢弃") if !pbIsImportantItem?(item) || $DEBUG
      if @bag.pbIsRegistered?(item)
        commands[cmdRegister = commands.length]  = _INTL("取消登录")
      elsif pbCanRegisterItem?(item)
        commands[cmdRegister = commands.length]  = _INTL("登录")
      end
      commands[cmdDebug = commands.length] = _INTL("调试") if $DEBUG
      commands[commands.length]                  = _INTL("取消")
      # Show commands generated above
      itemname = PBItems.getName(item) # Get item name
      command = @scene.pbShowCommands(_INTL("已选择{1}",itemname),commands)
      if cmdRead>=0 && command==cmdRead   # Read mail
        pbFadeOutIn(99999){
          pbDisplayMail(PokemonMail.new(item,"",""))
        }
      elsif cmdUse>=0 && command==cmdUse   # Use item
        ret = pbUseItem(@bag,item,@scene)
        # ret: 0=Item wasn't used; 1=Item used; 2=Close Bag to use in field
        break if ret==2 # End screen
        @scene.pbRefresh
        next
      elsif cmdGive>=0 && command==cmdGive   # Give item to Pok閙on
        if $Trainer.pokemonCount==0
          @scene.pbDisplay(_INTL("There is no Pok閙on."))
        elsif pbIsImportantItem?(item)
          @scene.pbDisplay(_INTL("宝可梦不能携带{1}。",itemname))
        else
          pbFadeOutIn(99999){
            sscene = PokemonParty_Scene.new
            sscreen = PokemonPartyScreen.new(sscene,$Trainer.party)
            sscreen.pbPokemonGiveScreen(item)
            @scene.pbRefresh
          }
        end
      elsif cmdToss>=0 && command==cmdToss   # Toss item
        qty = @bag.pbQuantity(item)
        if qty>1
          helptext = _INTL("要丢弃几个{1}？",PBItems.getNamePlural(item))
          qty = @scene.pbChooseNumber(helptext,qty)
        end
        if qty>0
          itemname = PBItems.getNamePlural(item) if qty>1
          if pbConfirm(_INTL("确定要丢弃{1}个{2}？",qty,itemname))
            pbDisplay(_INTL("丢弃了{1}个{2}。",qty,itemname))
            qty.times { @bag.pbDeleteItem(item) }
            @scene.pbRefresh
          end
        end   
      elsif cmdRegister>=0 && command==cmdRegister   # Register item
        if @bag.pbIsRegistered?(item)
          @bag.pbUnregisterItem(item)
        else
          @bag.pbRegisterItem(item)
        end
        @scene.pbRefresh
      elsif cmdDebug>=0 && command==cmdDebug   # Debug
        command = 0
        loop do
          command = @scene.pbShowCommands(_INTL("已选择{1}",itemname),[
            _INTL("改变数量"),
            _INTL("制作为神秘礼物"),
            _INTL("取消")
            ],command)
          case command
          ### Cancel ###
          when -1, 2
            break
          ### Change quantity ###
          when 0
            qty = @bag.pbQuantity(item)
            itemplural = PBItems.getNamePlural(item)
            params = ChooseNumberParams.new
            params.setRange(50,BAG_MAX_PER_SLOT)
            params.setDefaultValue(qty)
            newqty = pbMessageChooseNumber(
               _INTL("Choose new quantity of {1} (max. #{BAG_MAX_PER_SLOT}).",itemplural),params) { @scene.pbUpdate }
            if newqty>qty
              @bag.pbStoreItem(item,newqty-qty)
            elsif newqty<qty
              @bag.pbDeleteItem(item,qty-newqty)
            end
            @scene.pbRefresh
            break if newqty==0
          ### Make Mystery Gift ###
          when 1
            pbCreateMysteryGift(1,item)
          end
        end
      end
    end
    @scene.pbEndScene
    return item
  end

  def pbDisplay(text)
    @scene.pbDisplay(text)
  end

  def pbConfirm(text)
    return @scene.pbConfirm(text)
  end

  # UI logic for the item screen for choosing an item.
  def pbChooseItemScreen(proc=nil)
    oldlastpocket = @bag.lastpocket
    oldchoices = @bag.getAllChoices
    @scene.pbStartScene(@bag,true,proc)
    item = @scene.pbChooseItem
    @scene.pbEndScene
    @bag.lastpocket = oldlastpocket
    @bag.setAllChoices(oldchoices)
    return item
  end

  # UI logic for withdrawing an item in the item storage screen.
  def pbWithdrawItemScreen
    if !$PokemonGlobal.pcItemStorage
      $PokemonGlobal.pcItemStorage = PCItemStorage.new
    end
    storage = $PokemonGlobal.pcItemStorage
    @scene.pbStartScene(storage)
    loop do
      item = @scene.pbChooseItem
      break if item==0
      commands = [_INTL("取出"),_INTL("Give"),_INTL("Cancel")]
      itemname = PBItems.getName(item)
      command = @scene.pbShowCommands(_INTL("已选择{1}",itemname),commands)
      if command==0 # Withdraw
        qty = storage.pbQuantity(item)
        if qty>1 && !pbIsImportantItem?(item)
          qty = @scene.pbChooseNumber(_INTL("想要取回多少？"),qty)
        end
        if qty>0
          if !@bag.pbCanStore?(item,qty)
            pbDisplay(_INTL("已经没有位置存放了。"))
          else
            dispqty = (pbIsImportantItem?(item)) ? 1 : qty
            itemname = PBItems.getNamePlural(item) if dispqty>1
            pbDisplay(_INTL("取出了{1}个{2}。",dispqty,itemname))
            if !storage.pbDeleteItem(item,qty)
              raise "Can't delete items from storage"
            end
            if !@bag.pbStoreItem(item,qty)
              raise "Can't withdraw items from storage"
            end
          end
        end
      elsif command==1 # Give
        if $Trainer.pokemonCount==0
          @scene.pbDisplay(_INTL("There is no Pok閙on."))
          return 0
        elsif pbIsImportantItem?(item)
          @scene.pbDisplay(_INTL("宝可梦不能携带{1}。",itemname))
        else
          pbFadeOutIn(99999){
            sscene  = PokemonParty_Scene.new
            sscreen = PokemonPartyScreen.new(sscene,$Trainer.party)
            if sscreen.pbPokemonGiveScreen(item)
              # If the item was held, delete the item from storage
              if !storage.pbDeleteItem(item,1)
                raise "Can't delete item from storage"
              end
            end
            @scene.pbRefresh
          }
        end
      end
    end
    @scene.pbEndScene
  end

  # UI logic for depositing an item in the item storage screen.
  def pbDepositItemScreen
    @scene.pbStartScene(@bag)
    if !$PokemonGlobal.pcItemStorage
      $PokemonGlobal.pcItemStorage = PCItemStorage.new
    end
    storage = $PokemonGlobal.pcItemStorage
    item = 0
    loop do
      item = @scene.pbChooseItem
      break if item==0
      qty = @bag.pbQuantity(item)
      if qty>1 && !pbIsImportantItem?(item)
        qty = @scene.pbChooseNumber(_INTL("想要储存几个？"),qty)
      end
      if qty>0
        if !storage.pbCanStore?(item,qty)
          pbDisplay(_INTL("电脑里的储存盒已经满了……"))
        else
          dispqty  = (pbIsImportantItem?(item)) ? 1 : qty
          itemname = (dispqty>1) ? PBItems.getNamePlural(item) : PBItems.getName(item)
          pbDisplay(_INTL("储存了{1}个{2}。",dispqty,itemname))
          if !@bag.pbDeleteItem(item,qty)
            raise "Can't delete items from bag"
          end
          if !storage.pbStoreItem(item,qty)
            raise "Can't deposit items to storage"
          end
            @scene.pbRefresh
        end
      end
    end
    @scene.pbEndScene
  end

  # UI logic for tossing an item in the item storage screen.
  def pbTossItemScreen
    if !$PokemonGlobal.pcItemStorage
      $PokemonGlobal.pcItemStorage = PCItemStorage.new
    end
    storage = $PokemonGlobal.pcItemStorage
    @scene.pbStartScene(storage)
    loop do
      item = @scene.pbChooseItem
      break if item==0
      if pbIsImportantItem?(item)
        @scene.pbDisplay(_INTL("这不能丢弃！"))
        next
      end
      qty = storage.pbQuantity(item)
      itemname       = PBItems.getName(item)
      itemnameplural = PBItems.getNamePlural(item)
      if qty>1
        qty=@scene.pbChooseNumber(_INTL("要丢弃几个{1}？",itemnameplural),qty)
      end
      if qty>0
        itemname = itemnameplural if qty>1
        if pbConfirm(_INTL("确定要丢弃{1}个{2}？",qty,itemname))
          if !storage.pbDeleteItem(item,qty)
            raise "Can't delete items from storage"
          end
          pbDisplay(_INTL("丢弃了{1}个{2}。",qty,itemname))
        end
      end
    end
    @scene.pbEndScene
  end
end

# 道具排序
def pbSortItemByCHS(items)
  for i in 0...items.length - 1
    for j in 0...items.length - 1 - i
      item_pre  = items[j][0]
      pre_name  = PBItems.getName(item_pre)
      pre_index = get_chs_index(pre_name.split("")[0])
      next if pre_index[0] == -1
      
      item_after  = items[j + 1][0]
      after_name  = PBItems.getName(item_after)
      if after_name == nil || after_name == ""
        p "编号为" + item_after.to_s + "的道具名称为空，请检查PBS"
        return items
      end
      after_index = get_chs_index(after_name.split("")[0])
      next if after_index[0] == -1
      
      if pre_index[0] > after_index[0]  ||
         pre_index[0] == after_index[0] &&
         pre_index[1] > after_index[1]
        temp         = items[j + 1]
        items[j + 1] = items[j]
        items[j]     = temp
      end
    end
  end
  return items
end

