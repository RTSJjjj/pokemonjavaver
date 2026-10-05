# 原工程菜单布局对照表（供 P4 对齐用）

> 自动从工程 `Scripts.rxdata` 抽取（`pbStartScene/pbStartScreen` 与自定义面板类的坐标/素材）。
> 基准：`Graphics.width = SCREEN_WIDTH = 672`，`Graphics.height = SCREEN_HEIGHT = 448`。
> 坐标是 **左上原点、y 向下** 的 RMXP 屏幕像素；运行时是 libGDX **左下原点**，移植时 `y_gdx = 448 - y_rmxp - height`。
> 只列布局关键行，非完整代码。素材名即 `Graphics/...` 路径（如 `setSkin("Graphics/Windowskins/bw choice")`）。

## 主菜单（暂停菜单）（`PScreen_PauseMenu`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @sprites["cmdwindow"] = Window_CommandPokemon.new([])
  @sprites["cmdwindow"].visible = false
  @sprites["cmdwindow"].viewport = @viewport
  @sprites["infowindow"] = Window_UnformattedTextPokemon.newWithSize("",0,0,32,32,@viewport)
  @sprites["infowindow"].visible = false
  @sprites["helpwindow"] = Window_UnformattedTextPokemon.newWithSize("",0,0,32,32,@viewport)
  @sprites["helpwindow"].visible = false
```

## 宝可梦菜单（`PScreen_Party`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @sprites["partybg2"].z = @viewport.z+1
  @sprites["messagebox"] = Window_AdvancedTextPokemon.new("")
  @sprites["messagebox"].viewport       = @viewport
  @sprites["messagebox"].visible        = false
  @sprites["messagebox"].setSkin("Graphics/Windowskins/bw choice")
  @sprites["messagebox"].letterbyletter = true
  pbBottomLeftLines(@sprites["messagebox"],2)
  @sprites["storagetext"] = Window_UnformattedTextPokemon.new(
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
  @sprites["selectionpanel"] = PokemonPartySelectionBackgroundPanel.new(@viewport)
  @sprites["detailspanel"] = PokemonPartyDetailsPanel.new(@viewport, @party[0])
  pbBottomLeftLines(@sprites["helpwindow"],1)
  @sprites["pokemon#{i}"] = PokemonPartySelectionPanel.new(@party[i],i,@viewport,@sprites["detailspanel"])
  @sprites["pokemon#{i}"] = PokemonPartySelectionBlankPanel.new(i,@viewport)
  @sprites["pokemon#{i}"].text = annotations[i] if annotations
  @sprites["pokemon6"] = PokemonPartyConfirmSprite.new(@viewport)
  @sprites["pokemon7"] = PokemonPartyCancelSprite2.new(@viewport)
  @sprites["pokemon6"] = PokemonPartyCancelSprite.new(@viewport)
  @sprites["pokemon0"].selected = true
```

## 宝可梦摘要（`PScreen_Summary`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @typebitmap    = AnimatedBitmap.new(_INTL("Graphics/Pictures/types"))
  @markingbitmap = AnimatedBitmap.new("Graphics/Pictures/Summary/markings")
  @sprites["background"] = IconSprite.new(0,0,@viewport)
  @sprites["pokemon"] = PokemonSprite.new(@viewport)
  @sprites["pokemon"].setOffset(PictureOrigin::Center)
  @sprites["pokemon"].x = 104
  @sprites["pokemon"].y = 206
  @sprites["pokemon"].setPokemonBitmap(@pokemon)
  @sprites["pokeicon"] = PokemonIconSprite.new(@pokemon,@viewport)
  @sprites["pokeicon"].setOffset(PictureOrigin::Center)
  @sprites["pokeicon"].x       = 46
  @sprites["pokeicon"].y       = 92
  @sprites["pokeicon"].visible = false
  @sprites["itemicon"] = ItemIconSprite.new(30,320,@pokemon.item,@viewport)
  @sprites["itemicon"].blankzero = true
  @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  pbSetSystemFont(@sprites["overlay"].bitmap)
  @sprites["movepresel"] = MoveSelectionSprite.new(@viewport)
  @sprites["movepresel"].visible     = false
  @sprites["movepresel"].preselected = true
  @sprites["movesel"] = MoveSelectionSprite.new(@viewport)
  @sprites["movesel"].visible = false
  @sprites["ribbonpresel"] = RibbonSelectionSprite.new(@viewport)
  @sprites["ribbonpresel"].visible     = false
  @sprites["ribbonpresel"].preselected = true
  @sprites["ribbonsel"] = RibbonSelectionSprite.new(@viewport)
  @sprites["ribbonsel"].visible = false
  @sprites["uparrow"] = AnimatedSprite.new("Graphics/Pictures/uparrow",8,28,40,2,@viewport)
  @sprites["uparrow"].x = 350
  @sprites["uparrow"].y = 56
  @sprites["uparrow"].play
  @sprites["uparrow"].visible = false
  @sprites["downarrow"] = AnimatedSprite.new("Graphics/Pictures/downarrow",8,28,40,2,@viewport)
  @sprites["downarrow"].x = 350
  @sprites["downarrow"].y = 260
  @sprites["downarrow"].play
  @sprites["downarrow"].visible = false
  @sprites["markingbg"] = IconSprite.new(260,88,@viewport)
  @sprites["markingbg"].setBitmap("Graphics/Pictures/Summary/overlay_marking")
  @sprites["markingbg"].visible = false
  @sprites["markingoverlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  @sprites["markingoverlay"].visible = false
  pbSetSystemFont(@sprites["markingoverlay"].bitmap)
  @sprites["markingsel"] = IconSprite.new(0,0,@viewport)
  @sprites["markingsel"].setBitmap("Graphics/Pictures/Summary/cursor_marking")
  @sprites["markingsel"].src_rect.height = @sprites["markingsel"].bitmap.height/2
  @sprites["markingsel"].visible = false
  @sprites["messagebox"] = Window_AdvancedTextPokemon.new("")
  @sprites["messagebox"].viewport       = @viewport
  @sprites["messagebox"].visible        = false
  @sprites["messagebox"].letterbyletter = true
  pbBottomLeftLines(@sprites["messagebox"],2)
```

## 宝可梦摘要（BW 风格）（`BW PScreen_Summary`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @typebitmap    = AnimatedBitmap.new(_INTL("Graphics/Pictures/types"))
  @markingbitmap = AnimatedBitmap.new("Graphics/Pictures/Summary/markings")
  @sprites["bg_overlay"] = IconSprite.new(0,0,@viewport)
  @sprites["background"].ox+=6
  @sprites["background"].oy-=36
  @sprites["menuoverlay"] = IconSprite.new(0,0,@viewport)
  @sprites["pokemon"] = PokemonSprite.new(@viewport)
  @sprites["pokemon"].setOffset(PictureOrigin::Center)
  @sprites["pokemon"].x = 460+64
  @sprites["pokemon"].y = 208+32
  @sprites["pokemon"].setPokemonBitmap(@pokemon)
  @sprites["pokeicon"] = PokemonIconSprite.new(@pokemon,@viewport)
  @sprites["pokeicon"].setOffset(PictureOrigin::Center)
  @sprites["pokeicon"].x       = 46
  @sprites["pokeicon"].y       = 92
  @sprites["pokeicon"].visible = false
  @sprites["itemicon"] = ItemIconSprite.new(552+96,360+64,@pokemon.item,@viewport)
  @sprites["itemicon"].blankzero = true
  @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  pbSetSystemFont(@sprites["overlay"].bitmap)
  @sprites["movepresel"] = MoveSelectionSprite.new(@viewport)
  @sprites["movepresel"].visible     = false
  @sprites["movepresel"].preselected = true
  @sprites["movesel"] = MoveSelectionSprite.new(@viewport)
  @sprites["movesel"].visible = false
  @sprites["ribbonpresel"] = RibbonSelectionSprite.new(@viewport)
  @sprites["ribbonpresel"].visible     = false
  @sprites["ribbonpresel"].preselected = true
  @sprites["ribbonsel"] = RibbonSelectionSprite.new(@viewport)
  @sprites["ribbonsel"].visible = false
  @sprites["uparrow"] = AnimatedSprite.new("Graphics/Pictures/uparrow",8,28,40,2,@viewport)
  @sprites["uparrow"].x = 260
  @sprites["uparrow"].y = 56
  @sprites["uparrow"].play
  @sprites["uparrow"].visible = false
  @sprites["downarrow"] = AnimatedSprite.new("Graphics/Pictures/downarrow",8,28,40,2,@viewport)
  @sprites["downarrow"].x = 260
  @sprites["downarrow"].y = 260
  @sprites["downarrow"].play
  @sprites["downarrow"].visible = false
  @sprites["markingbg"] = IconSprite.new(260,88,@viewport)
  @sprites["markingbg"].setBitmap("Graphics/Pictures/Summary/overlay_marking")
  @sprites["markingbg"].visible = false
  @sprites["markingoverlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  @sprites["markingoverlay"].visible = false
  pbSetSystemFont(@sprites["markingoverlay"].bitmap)
  @sprites["markingsel"] = IconSprite.new(0,0,@viewport)
  @sprites["markingsel"].setBitmap("Graphics/Pictures/Summary/cursor_marking")
  @sprites["markingsel"].src_rect.height = @sprites["markingsel"].bitmap.height/2
  @sprites["markingsel"].visible = false
  @sprites["messagebox"] = Window_AdvancedTextPokemon.new("")
  @sprites["messagebox"].viewport       = @viewport
  @sprites["messagebox"].visible        = false
  @sprites["messagebox"].letterbyletter = true
  pbBottomLeftLines(@sprites["messagebox"],2)
```

## 背包（`PScreen_Bag`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @sliderbitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/Bag/icon_slider"))
  @pocketbitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/Bag/icon_pocket"))
  @sprites["background"] = IconSprite.new(0,0,@viewport)
  @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  pbSetSystemFont(@sprites["overlay"].bitmap)
  @sprites["bagsprite"] = IconSprite.new(30,20,@viewport)
  @sprites["pocketicon"] = BitmapSprite.new(186,32,@viewport)
  @sprites["pocketicon"].x = 0
  @sprites["pocketicon"].y = 224
  @sprites["leftarrow"] = AnimatedSprite.new("Graphics/Pictures/leftarrow",8,40,28,2,@viewport)
  @sprites["leftarrow"].x       = -4
  @sprites["leftarrow"].y       = 76
  @sprites["leftarrow"].visible = (!@choosing || numfilledpockets>1)
  @sprites["leftarrow"].play
  @sprites["rightarrow"] = AnimatedSprite.new("Graphics/Pictures/rightarrow",8,40,28,2,@viewport)
  @sprites["rightarrow"].x       = 150
  @sprites["rightarrow"].y       = 76
  @sprites["rightarrow"].visible = (!@choosing || numfilledpockets>1)
  @sprites["rightarrow"].play
  @sprites["itemlist"] = Window_PokemonBag.new(@bag,@filterlist,lastpocket,168,-8,314,40+32+ITEMSVISIBLE*32)
  @sprites["itemlist"].viewport    = @viewport
  @sprites["itemlist"].pocket      = lastpocket
  @sprites["itemlist"].index       = @bag.getChoice(lastpocket)
  @sprites["itemlist"].baseColor   = ITEMLISTBASECOLOR
  @sprites["itemlist"].shadowColor = ITEMLISTSHADOWCOLOR
  @sprites["itemicon"] = ItemIconSprite.new(48,Graphics.height-48,-1,@viewport)
  @sprites["itemtext"] = Window_UnformattedTextPokemon.new("")
  @sprites["itemtext"].x           = 72
  @sprites["itemtext"].y           = 270
  @sprites["itemtext"].width       = Graphics.width-72-24
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
```

## 图鉴菜单入口（`PScreen_PokedexMenu`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @sprites["background"] = IconSprite.new(0,0,@viewport)
  @sprites["background"].setBitmap(_INTL("Graphics/Pictures/Pokedex/bg_menu"))
  @sprites["headings"]=Window_AdvancedTextPokemon.newWithSize(
  @sprites["headings"].windowskin  = nil
  @sprites["commands"] = Window_DexesList.new(commands,commands2,Graphics.width-84)
  @sprites["commands"].x      = 40
  @sprites["commands"].y      = 192
  @sprites["commands"].height = 192
  @sprites["commands"].viewport = @viewport
```

## 图鉴列表（`PScreen_PokedexMain`）

```ruby
  @sliderbitmap       = AnimatedBitmap.new("Graphics/Pictures/Pokedex/icon_slider")
  @typebitmap         = AnimatedBitmap.new(_INTL("Graphics/Pictures/Pokedex/icon_types"))
  @shapebitmap        = AnimatedBitmap.new("Graphics/Pictures/Pokedex/icon_shapes")
  @hwbitmap           = AnimatedBitmap.new("Graphics/Pictures/Pokedex/icon_hw")
  @selbitmap          = AnimatedBitmap.new("Graphics/Pictures/Pokedex/icon_searchsel")
  @searchsliderbitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/Pokedex/icon_searchslider"))
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @sprites["searchbg"].visible = false
  @sprites["pokedex"] = Window_Pokedex.new(206,30,276,364,@viewport)
  @sprites["icon"] = PokemonSprite.new(@viewport)
  @sprites["icon"].setOffset(PictureOrigin::Center)
  @sprites["icon"].x = 112
  @sprites["icon"].y = 196
  @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  pbSetSystemFont(@sprites["overlay"].bitmap)
  @sprites["searchcursor"] = PokedexSearchSelectionSprite.new(@viewport)
  @sprites["searchcursor"].visible = false
```

## 图鉴列表（BW 风格）（`PokedexMain BW Style`）

```ruby
  @sliderbitmap       = AnimatedBitmap.new("Graphics/Pictures/Pokedex/icon_slider")
  @typebitmap         = AnimatedBitmap.new(_INTL("Graphics/Pictures/Pokedex/icon_types"))
  @shapebitmap        = AnimatedBitmap.new("Graphics/Pictures/Pokedex/icon_shapes")
  @hwbitmap           = AnimatedBitmap.new("Graphics/Pictures/Pokedex/icon_hw")
  @selbitmap          = AnimatedBitmap.new("Graphics/Pictures/Pokedex/icon_searchsel")
  @searchsliderbitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/Pokedex/icon_searchslider"))
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @sprites["background"] = ScrollingSprite.new(@viewport)
  @sprites["background"].speed = 1
  @sprites["infoverlay"] = IconSprite.new(0,0,@viewport)
  @sprites["background"] = ScrollingSprite.new(@viewport)
  @sprites["background"].speed = 1
  @sprites["infoverlay"] = IconSprite.new(0,0,@viewport)
  @sprites["background"] = ScrollingSprite.new(@viewport)
  @sprites["background"].speed = 1
  @sprites["infoverlay"] = IconSprite.new(0,0,@viewport)
  @sprites["searchbg"].visible = false
  @sprites["pokedex"] = Window_Pokedex.new(184*2, 94, 276, 344, @viewport)
  @sprites["icon"] = PokemonSprite.new(@viewport)
  @sprites["icon"].setOffset(PictureOrigin::Center)
  @sprites["icon"].x = 110+64
  @sprites["icon"].y = 196+32
  @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  pbSetSystemFont(@sprites["overlay"].bitmap)
  @sprites["searchcursor"] = PokedexSearchSelectionSprite.new(@viewport)
  @sprites["searchcursor"].visible = false
```

## 图鉴条目（`PScreen_PokedexEntry`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @typebitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/Pokedex/icon_types"))
  @sprites["background"] = IconSprite.new(0,0,@viewport)
  @sprites["infosprite"] = PokemonSprite.new(@viewport)
  @sprites["infosprite"].setOffset(PictureOrigin::Center)
  @sprites["infosprite"].x = 104
  @sprites["infosprite"].y = 136
  @sprites["areamap"] = IconSprite.new(0,0,@viewport)
  @sprites["areamap"].setBitmap("Graphics/Pictures/#{@mapdata[@region][1]}")
  @sprites["areamap"].x += (Graphics.width-@sprites["areamap"].bitmap.width)/2
  @sprites["areamap"].y += (Graphics.height+32-@sprites["areamap"].bitmap.height)/2
  pbDrawImagePositions(@sprites["areamap"].bitmap,[
  @sprites["areahighlight"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  @sprites["areaoverlay"] = IconSprite.new(0,0,@viewport)
  @sprites["areaoverlay"].setBitmap("Graphics/Pictures/Pokedex/overlay_area")
  @sprites["formfront"] = PokemonSprite.new(@viewport)
  @sprites["formfront"].setOffset(PictureOrigin::Center)
  @sprites["formfront"].x = 130
  @sprites["formfront"].y = 158
  @sprites["formback"] = PokemonSprite.new(@viewport)
  @sprites["formback"].setOffset(PictureOrigin::Bottom)
  @sprites["formback"].x = 382   # y is set below as it depends on metrics
  @sprites["formicon"] = PokemonSpeciesIconSprite.new(0,@viewport)
  @sprites["formicon"].setOffset(PictureOrigin::Center)
  @sprites["formicon"].x = 82
  @sprites["formicon"].y = 328
  @sprites["uparrow"] = AnimatedSprite.new("Graphics/Pictures/uparrow",8,28,40,2,@viewport)
  @sprites["uparrow"].x = 242
  @sprites["uparrow"].y = 268
  @sprites["uparrow"].play
  @sprites["uparrow"].visible = false
  @sprites["downarrow"] = AnimatedSprite.new("Graphics/Pictures/downarrow",8,28,40,2,@viewport)
  @sprites["downarrow"].x = 242
  @sprites["downarrow"].y = 348
  @sprites["downarrow"].play
  @sprites["downarrow"].visible = false
  @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  pbSetSystemFont(@sprites["overlay"].bitmap)
```

## 图鉴条目（BW 风格）（`PokedexEntry BW Style`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @typebitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/Pokedex/icon_types"))
  @sprites["background"] = IconSprite.new(0,0,@viewport)
  @sprites["background"] = ScrollingSprite.new(@viewport)
  @sprites["background"].speed = 1
  @sprites["infoverlay"] = IconSprite.new(0,0,@viewport)
  @sprites["infosprite"] = PokemonSprite.new(@viewport)
  @sprites["infosprite"].setOffset(PictureOrigin::Center)
  @sprites["infosprite"].x = 98+64
  @sprites["infosprite"].y = 112
  @sprites["infosprite"].z += 1
  @sprites["areamap"] = IconSprite.new(0,0,@viewport)
  @sprites["areamap"].setBitmap("Graphics/Pictures/#{@mapdata[@region][1]}")
  @sprites["areamap"].x += (Graphics.width-@sprites["areamap"].bitmap.width)/2
  @sprites["areamap"].y += (Graphics.height-@sprites["areamap"].bitmap.height)/2
  pbDrawImagePositions(@sprites["areamap"].bitmap,[
  @sprites["areahighlight"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  @sprites["areaoverlay"] = IconSprite.new(0,0,@viewport)
  @sprites["areaoverlay"].setBitmap("Graphics/Pictures/Pokedex/overlay_area")
  @sprites["form"] = PokemonSprite.new(@viewport)
  @sprites["form"].setOffset(PictureOrigin::Center)
  @sprites["form"].x = 158
  @sprites["form"].y = 240+32
  @sprites["form"].z += 1
  @sprites["forms"] = PokemonSprite.new(@viewport)
  @sprites["forms"].setOffset(PictureOrigin::Center)
  @sprites["forms"].x = 414+64+32
  @sprites["forms"].y = 240+32
  @sprites["forms"].z += 1
  @sprites["formss"] = PokemonSprite.new(@viewport)
  @sprites["formss"].setOffset(PictureOrigin::Center)
  @sprites["formss"].x = 414+64+32
  @sprites["formss"].y = 240+32
  @sprites["formss"].z += 1
  @sprites["formicon"] = PokemonSpeciesIconSprite.new(0,@viewport)
  @sprites["formicon"].setOffset(PictureOrigin::Center)
  @sprites["formicon"].x = 96+32
  @sprites["formicon"].y = 112
  @sprites["formicon"].z += 1
  @sprites["formicons"] = PokemonSpeciesIconSprite.new(0,@viewport)
  @sprites["formicons"].setOffset(PictureOrigin::Center)
  @sprites["formicons"].x = Graphics.width-96-16
  @sprites["formicons"].y = 112
  @sprites["formicons"].z += 1
  @sprites["formiconss"] = PokemonSpeciesIconSprite.new(0,@viewport)
  @sprites["formiconss"].setOffset(PictureOrigin::Center)
  @sprites["formiconss"].x = Graphics.width-96-16
  @sprites["formiconss"].y = 112
  @sprites["formiconss"].z += 1
  @sprites["uparrow"] = AnimatedSprite.new("Graphics/Pictures/uparrow",8,28,40,2,@viewport)
  @sprites["uparrow"].x = 242+80
  @sprites["uparrow"].y = 40
  @sprites["uparrow"].z += 1
  @sprites["uparrow"].play
  @sprites["uparrow"].visible = false
  @sprites["downarrow"] = AnimatedSprite.new("Graphics/Pictures/downarrow",8,28,40,2,@viewport)
  @sprites["downarrow"].x = 242+80
  @sprites["downarrow"].y = 128
  @sprites["downarrow"].z += 1
  @sprites["downarrow"].play
  @sprites["downarrow"].visible = false
  @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  @sprites["overlay"].z += 1
  pbSetSystemFont(@sprites["overlay"].bitmap)
  @sprites["overlay_shiny"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  @sprites["overlay_shiny"].z += 1
  pbSetSystemFont(@sprites["overlay_shiny"].bitmap)
  @sprites["overlay_data"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  @sprites["overlay_data"].z += 1
  pbSetSmallFont(@sprites["overlay_data"].bitmap)
```

## 训练家卡（`PScreen_TrainerCard`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @sprites["card"] = IconSprite.new(0,0,@viewport)
  @sprites["card"].setBitmap("Graphics/Pictures/Trainer Card/card_f")
  @sprites["card"].setBitmap("Graphics/Pictures/Trainer Card/card")
  @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
  pbSetSystemFont(@sprites["overlay"].bitmap)
  @sprites["trainer"] = IconSprite.new(336,130,@viewport)
  @sprites["trainer"].setBitmap(pbPlayerSpriteFile($Trainer.trainertype))
  @sprites["trainer"].x -= (@sprites["trainer"].bitmap.width-128)/2
  @sprites["trainer"].y -= (@sprites["trainer"].bitmap.height-128)
  @sprites["trainer"].z = 2
```

## PC 存储（`PScreen_PokemonStorage`）

```ruby

```

## PC 道具存储（`PScreen_ItemStorage`）

```ruby
  @viewport   = Viewport.new(0,0,Graphics.width,Graphics.height)
  @sprites["background"] = IconSprite.new(0,0,@viewport)
  @sprites["background"].setBitmap("Graphics/Pictures/pcItembg")
  @sprites["icon"] = ItemIconSprite.new(83,334,-1,@viewport)
  @sprites["itemwindow"] = Window_PokemonItemStorage.new(@bag,130,14,334,32+ITEMSVISIBLE*32)
  @sprites["itemwindow"].viewport    = @viewport
  @sprites["itemwindow"].index       = 0
  @sprites["itemwindow"].baseColor   = ITEMLISTBASECOLOR
  @sprites["itemwindow"].shadowColor = ITEMLISTSHADOWCOLOR
  @sprites["itemwindow"].refresh
  @sprites["pocketwindow"] = BitmapSprite.new(120,64,@viewport)
  @sprites["pocketwindow"].x = 56
  @sprites["pocketwindow"].y = 16
  pbSetNarrowFont(@sprites["pocketwindow"].bitmap)
  @sprites["itemtextwindow"] = Window_UnformattedTextPokemon.newWithSize("",116,270,Graphics.width-84,128,@viewport)
  @sprites["itemtextwindow"].baseColor   = ITEMTEXTBASECOLOR
  @sprites["itemtextwindow"].shadowColor = ITEMTEXTSHADOWCOLOR
  @sprites["itemtextwindow"].windowskin  = nil
  @sprites["helpwindow"] = Window_UnformattedTextPokemon.new("")
  @sprites["helpwindow"].visible  = false
  @sprites["helpwindow"].viewport = @viewport
  @sprites["msgwindow"] = Window_AdvancedTextPokemon.new("")
  @sprites["msgwindow"].visible  = false
  @sprites["msgwindow"].viewport = @viewport
  pbBottomLeftLines(@sprites["helpwindow"],1)
```

## PC 服务菜单（`PScreen_PC`）

```ruby

```

## 交易（`PScreen_Trading`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @sprites["rsprite1"] = PokemonSprite.new(@viewport)
  @sprites["rsprite1"].setPokemonBitmap(@pokemon,false)
  @sprites["rsprite1"].setOffset(PictureOrigin::Bottom)
  @sprites["rsprite1"].x = Graphics.width/2
  @sprites["rsprite1"].y = 264
  @sprites["rsprite1"].z = 10
  pbApplyBattlerMetricsToSprite(@sprites["rsprite1"],1,@pokemon.fSpecies)
  @sprites["rsprite2"] = PokemonSprite.new(@viewport)
  @sprites["rsprite2"].setPokemonBitmap(@pokemon2,false)
  @sprites["rsprite2"].setOffset(PictureOrigin::Bottom)
  @sprites["rsprite2"].x = Graphics.width/2
  @sprites["rsprite2"].y = 264
  @sprites["rsprite2"].z = 10
  pbApplyBattlerMetricsToSprite(@sprites["rsprite2"],1,@pokemon2.fSpecies)
  @sprites["rsprite2"].visible = false
  @sprites["msgwindow"] = pbCreateMessageWindow(@viewport)
```

## 商店（`PScreen_Mart`）

```ruby

```

## 登记菜单（`PScreen_ReadyMenu`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  @sprites["cmdwindow"] = Window_CommandPokemon.new((@index[2]==0) ? @movecommands : @itemcommands)
  @sprites["cmdwindow"].height = 6*32
  @sprites["cmdwindow"].visible = false
  @sprites["cmdwindow"].viewport = @viewport
  @sprites["movebutton#{i}"] = ReadyMenuButton.new(i,@commands[0][i],@index[0],@index[2],@viewport)
  @sprites["itembutton#{i}"] = ReadyMenuButton.new(i,@commands[1][i],@index[1],@index[2],@viewport)
```

## 选项（`PScreen_Options`）

```ruby
  @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
@sprites["title"] = Window_UnformattedTextPokemon.newWithSize(
  _INTL("设置"),0,0,Graphics.width,64,@viewport)
  @sprites["textbox"] = pbCreateMessageWindow
  @sprites["textbox"].text           = _INTL("对话框样式 {1}.",1+$PokemonSystem.textskin)
  @sprites["textbox"].letterbyletter = false
  pbSetSystemFont(@sprites["textbox"].contents)
  @sprites["option"] = Window_PokemonOption.new(@PokemonOptions,0,
  @sprites["title"].height,Graphics.width,
  Graphics.height-@sprites["title"].height-@sprites["textbox"].height)
  @sprites["option"].viewport = @viewport
  @sprites["option"].visible  = true
  @sprites["option"].setValueNoRefresh(i,(@PokemonOptions[i].get || 0))
  @sprites["option"].refresh
```

# 自定义面板 / 精灵类的坐标

> 上面各屏用到的 `Xxx.new` 自定义类，这里给出它们 `initialize` 里的布局行。

## `AnimatedSprite`（`SpriteWrapper`）

```ruby
  @animbitmap=AnimatedBitmap.new(animname).deanimate
  @animbitmap=Bitmap.new(framewidth,frameheight)
  self.src_rect.width=@framewidth
  self.src_rect.height=@frameheight
```

## `ItemIconSprite`（`PItem_Sprites`）

```ruby
  self.x = x
  self.y = y
```

## `MoveSelectionSprite`（`PScreen_Summary`）

```ruby
  @movesel = AnimatedBitmap.new("Graphics/Pictures/Summary/cursor_move")
```

## `PokedexSearchSelectionSprite`（`PScreen_PokedexMain`）

```ruby
  @selbitmap = AnimatedBitmap.new("Graphics/Pictures/Pokedex/cursor_search")
```

## `PokemonPartyDetailsPanel`（`PScreen_Party`）

```ruby
  self.x = Graphics.width/2
  self.y = Graphics.height/2 - 128
  @pkmnsprite =  PokemonSprite.new(@viewport)
  @pkmnsprite.x = self.x
  @pkmnsprite.y = self.y + 30
  @overlaysprite = BitmapSprite.new(Graphics.width,300,@viewport)
  @overlaysprite2 = BitmapSprite.new(Graphics.width,300,@viewport)
  @expBar.x = 235+48  # 血条旁边
  @expBar.y = 220+56  # 血条下方
```

## `PokemonPartySelectionBackgroundPanel`（`PScreen_Party`）

```ruby
  self.x = (Graphics.width - 430) / 2
  self.y = 228+64
  @panelSelectionBgSprite = AnimatedBitmap.new("Graphics/Pictures/Party/panel_pok_base_bg")
  self.bitmap = @panelSelectionBgSprite.bitmap
```

## `PokemonPartySelectionBlankPanel`（`PScreen_Party`）

```ruby
  start_x = (Graphics.width - 430) / 2
  self.x = start_x + slot_width * index
  self.y = 240
```

## `PokemonPartySelectionPanel`（`PScreen_Party`）

```ruby
  start_x = (Graphics.width - 430) / 2  # = 121
  self.x = start_x + slot_width * index
  self.y = 226+64
  @shadowsprite = IconSprite.new(0,0,viewport)
  @shadowsprite.x = self.x + slot_width/2  # 槽位中心
  @shadowsprite.y = self.y + 64
  @pkmnsprite = PokemonIconSprite.new(pokemon,viewport)
  @pkmnsprite.x      = self.x + slot_width/2  # 槽位中心
  @pkmnsprite.y      = self.y + 52
  @helditem   = HeldItemIconSprite.new(0, 0, @pokemon, viewport)
  @helditem.x = self.x + 40
  @helditem.y = self.y + 56
  @overlaysprite   = BitmapSprite.new(96,160,viewport)
  @overlaysprite.x = self.x + 12
  @overlaysprite.y = self.y
  @statuses        = AnimatedBitmap.new(_INTL("Graphics/Pictures/statuses"))
  @evolution       = AnimatedBitmap.new(_INTL("Graphics/Pictures/Party/icon_evo"))
  @arrow_normal    = AnimatedBitmap.new(_INTL("Graphics/Pictures/Party/arrow_normal"))
  @arrow_preselect = AnimatedBitmap.new(_INTL("Graphics/Pictures/Party/arrow_preselect"))
```

## `ReadyMenuButton`（`PScreen_ReadyMenu`）

```ruby
  @button = AnimatedBitmap.new("Graphics/Pictures/Ready Menu/icon_movebutton")
  @button = AnimatedBitmap.new("Graphics/Pictures/Ready Menu/icon_itembutton")
  @icon = PokemonIconSprite.new($Trainer.party[@command[3]],viewport)
  @icon = ItemIconSprite.new(0,0,@command[0],viewport)
```

## `RibbonSelectionSprite`（`PScreen_Summary`）

```ruby
  @movesel = AnimatedBitmap.new("Graphics/Pictures/Summary/cursor_ribbon")
```

