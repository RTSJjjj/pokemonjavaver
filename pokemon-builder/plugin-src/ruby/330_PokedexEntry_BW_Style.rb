#===============================================================================
#  PokedexEntry BW Style
#  for Pokémon Essentials version 18.x
#
#===============================================================================
#
# Instructions: Put this script below BW_PokedexMain. Download the file BW Pokédex.rar and
# extract the files in your project main folder.
#   
#    Requires Scripting Utilities by Luka S.J. for the script to work correctly.
#
#===============================================================================
#
#  Modified by DeepBlue PacificWaves
#  Special thanks to NettoHikari, that helped with the Entry Scene/Entry Page
#  code.
#
#  Graphics Ripped by Xtreme1992
#
#  If used, please give credits. For more information on how to credit, look 
#  for the original post on Relic Castle or PokéCommunity.
#
#===============================================================================

PluginManager.register({
  :name => "PokedexEntry BW Style",
  :version => "1.3",
  :credits => ["DeepBlue PacificWaves", "NettoHikari", "Xtreme1992"],
  :dependencies => [
    ["Luka's Scripting Utilities"],
  ],
})

#===============================================================================

def pbFindEncounter(encounter,species)
  return false if !encounter
  for i in 0...encounter.length
    next if !encounter[i]
    for j in 0...encounter[i].length
      return true if encounter[i][j][0]==species
    end
  end
  return false
end



class PokemonPokedexInfo_Scene
  def pbStartScene(dexlist,index,region)
    @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z = 99999
    @dexlist = dexlist
    @index   = index
    @species = @dexlist[@index][0]
    @region  = region
    @page = 1
    @show_shiny = 0
    @switch_msg = ["超闪","异色"]
    @data_show_type = 0
    @data_msg = ["先天招式","升级招式","机器招式","蛋招式"]
    @move_page = 0
    @typebitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/Pokedex/icon_types"))
    @sprites = {}
# Defines the Scrolling Background, as well as the overlay on top of it    
    @sprites["background"] = IconSprite.new(0,0,@viewport)
    @sprites["background"] = ScrollingSprite.new(@viewport)
    @sprites["background"].speed = 1
    @sprites["infoverlay"] = IconSprite.new(0,0,@viewport)
    @sprites["infosprite"] = PokemonSprite.new(@viewport)
    @sprites["infosprite"].setOffset(PictureOrigin::Center)
# Changes the postion of the Pokémon in the Entry Page    
    @sprites["infosprite"].x = 98+64
    @sprites["infosprite"].y = 112
    @sprites["infosprite"].z += 1
    @mapdata = pbLoadTownMapData
    mappos = ($game_map) ? pbGetMetadata($game_map.map_id,MetadataMapPosition) : nil
    if @region<0 || @region >= 1 || !@mapdata[@region]                                  # Use player's current region
      @region = (mappos) ? mappos[0] : 0                      # Region 0 default
    end
    @sprites["areamap"] = IconSprite.new(0,0,@viewport)
    @sprites["areamap"].setBitmap("Graphics/Pictures/#{@mapdata[@region][1]}")
    @sprites["areamap"].x += (Graphics.width-@sprites["areamap"].bitmap.width)/2
    @sprites["areamap"].y += (Graphics.height-@sprites["areamap"].bitmap.height)/2
    for hidden in REGION_MAP_EXTRAS
      if hidden[0]==@region && hidden[1]>0 && $game_switches[hidden[1]]
        pbDrawImagePositions(@sprites["areamap"].bitmap,[
           ["Graphics/Pictures/#{hidden[4]}",
              hidden[2]*PokemonRegionMap_Scene::SQUAREWIDTH,
              hidden[3]*PokemonRegionMap_Scene::SQUAREHEIGHT]
        ])
      end
    end
    @sprites["areahighlight"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
    @sprites["areaoverlay"] = IconSprite.new(0,0,@viewport)
    @sprites["areaoverlay"].setBitmap("Graphics/Pictures/Pokedex/overlay_area")
# Changes the X and Y position of the front sprite of the Pokémon in the 
# Forms Page    
    @sprites["form"] = PokemonSprite.new(@viewport)
    @sprites["form"].setOffset(PictureOrigin::Center)
    @sprites["form"].x = 158
    @sprites["form"].y = 240+32
    @sprites["form"].z += 1
# Changes the X position of the shiny sprite of the Pokémon in the Forms Page 
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
# Changes the X and Y position of the icon sprite of the Pokémon in the 
# Forms Page  
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
# Changes the X and Y position of the Up Arrow sprite of the Pokémon in the 
# Forms Page  
    @sprites["uparrow"] = AnimatedSprite.new("Graphics/Pictures/uparrow",8,28,40,2,@viewport)
    @sprites["uparrow"].x = 242+80
    @sprites["uparrow"].y = 40
    @sprites["uparrow"].z += 1
    @sprites["uparrow"].play
    @sprites["uparrow"].visible = false
# Changes the X and Y position of the Down Arrow sprite of the Pokémon in the 
# Forms Page  
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
    pbUpdateDummyPokemon
    @available = pbGetAvailableForms
    drawPage(@page)
    pbFadeInAndShow(@sprites) { pbUpdate }
  end

  def pbStartSceneBrief(species)  # For standalone access, shows first page only
    @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z = 99999
    @show_shiny = 0
    @switch_msg = ["超闪","异色"]
    @data_show_type = 0
    @data_msg = ["先天招式","升级招式","机器招式","蛋招式"]
    @move_page = 0
#    @region = 0
    dexnum = species
    dexnumshift = false
    if $PokemonGlobal.pokedexUnlocked[$PokemonGlobal.pokedexUnlocked.length-1]
      dexnumshift = true if DEXES_WITH_OFFSETS.include?(-1)
    else
      dexnum = 0
      for i in 0...$PokemonGlobal.pokedexUnlocked.length-1
        next if !$PokemonGlobal.pokedexUnlocked[i]
        num = pbGetRegionalNumber(i,species)
        next if num<=0
        dexnum = num
        dexnumshift = true if DEXES_WITH_OFFSETS.include?(i)
#        @region = pbDexNames[i][1] if pbDexNames[i].is_a?(Array)
        break
      end
    end
    @dexlist = [[species,"",0,0,dexnum,dexnumshift]]
    @index   = 0
    @page = 1
    @brief = true
    @typebitmap = AnimatedBitmap.new(_INTL("Graphics/Pictures/Pokedex/icon_types"))
    @sprites = {}
# Defines the Scrolling Background of the Entry Scene when capturing a Wild 
# Pokémon, as well as the overlay on top of it
    @sprites["background"] = IconSprite.new(0,0,@viewport)
    @sprites["background"] = ScrollingSprite.new(@viewport)
    @sprites["background"].speed = 1
    @sprites["infoverlay"] = IconSprite.new(0,0,@viewport)
    @sprites["capturebar"] = IconSprite.new(0,0,@viewport)
    @sprites["infosprite"] = PokemonSprite.new(@viewport)
    @sprites["infosprite"].setOffset(PictureOrigin::Center)
# Changes the X and Y position of the front sprite of the Pokémon in the  Entry 
# Scene when capturing a Wild Pokémon
    @sprites["infosprite"].x = 98+64
    @sprites["infosprite"].y = 136
    @sprites["overlay"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
    pbSetSystemFont(@sprites["overlay"].bitmap)
    @sprites["overlay_shiny"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
    pbSetSystemFont(@sprites["overlay_shiny"].bitmap)
    @sprites["overlay_data"] = BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
    pbSetSmallFont(@sprites["overlay_data"].bitmap)
    pbUpdateDummyPokemon
    drawPage(@page)
    pbFadeInAndShow(@sprites) { pbUpdate }
  end

  def pbEndScene
    pbFadeOutAndHide(@sprites) { pbUpdate }
    pbDisposeSpriteHash(@sprites)
    @typebitmap.dispose
    @viewport.dispose
  end

  def pbUpdate
    if @page==5
      intensity = (Graphics.frame_count%40)*12
      intensity = 480-intensity if intensity>240
      @sprites["areahighlight"].opacity = intensity if @sprites["areahighlight"]
    end
    pbUpdateSpriteHash(@sprites)
  end

  def pbUpdateDummyPokemon
    overlay_shiny = @sprites["overlay_shiny"].bitmap
    overlay_shiny.clear
    @species = @dexlist[@index][0]
    text_shiny = [
      [_INTL("[C]:切换形态 [Z]:切换{1}",@switch_msg[@show_shiny]),Graphics.width/1.6,4,2,Color.new(255,255,255),Color.new(115,115,115)]
    ]
    pbDrawTextPositions(overlay_shiny,text_shiny)
    @gender  = ($Trainer.formlastseen[@species][0] || 0)
    @form    = ($Trainer.formlastseen[@species][1] || 0)
    # 检查 formlastseen 是否存在，如果不存在则创建
    if !$Trainer.formlastseen[@species]
      $Trainer.formlastseen[@species] = [0,0]
    end
    @sprites["infosprite"].setSpeciesBitmap(@species,(@gender==1),@form)
    # ========普色========
    if @sprites["form"]
      @sprites["form"].setSpeciesBitmap(@species,(@gender==1),@form)
      @sprites["form"].visible = true
    end
    if @sprites["formicon"]
      @sprites["formicon"].pbSetParams(@species,@gender,@form)
      @sprites["formicon"].visible = true
    end
    # ========异色========
    if @sprites["forms"]
      @sprites["forms"].setSpeciesBitmap(@species,(@gender==1),@form,true)
      @sprites["forms"].visible = (@show_shiny==0)
    end
    if @sprites["formicons"]
      @sprites["formicons"].pbSetParams(@species,@gender,@form,true)
      @sprites["formicons"].visible = (@show_shiny==0)
    end
    # ========超闪========
    if @sprites["formss"]
      @sprites["formss"].setSpeciesBitmap(@species,(@gender==1),@form,true,false,false,false,true)
      @sprites["formss"].visible = (@show_shiny==1)
    end
    if @sprites["formiconss"]
      @sprites["formiconss"].pbSetParams(@species,@gender,@form,true,true)
      @sprites["formiconss"].visible = (@show_shiny==1)
    end
  end

  def pbGetAvailableForms
    available = []   # [name, gender, form]
    formdata = pbLoadFormToSpecies
    possibleforms = []
    multiforms = false
    if formdata[@species]
      for i in 0...formdata[@species].length
        fSpecies = pbGetFSpeciesFromForm(@species,i)
        formname = pbGetMessage(MessageTypes::FormNames,fSpecies)
        genderRate = pbGetSpeciesData(@species,i,SpeciesGenderRate)
        if i==0 || (formname && formname!="")
          multiforms = true if i>0
          case genderRate
          when PBGenderRates::AlwaysMale,
               PBGenderRates::AlwaysFemale,
               PBGenderRates::Genderless
            gendertopush = (genderRate==PBGenderRates::AlwaysFemale) ? 1 : 0
            gendertopush = 2 if genderRate==PBGenderRates::Genderless
            possibleforms.push([i,gendertopush,formname])
          else   # Both male and female
            for g in 0...2
              possibleforms.push([i,g,formname])
              break if (formname && formname!="")
            end
          end
        end
      end
    end
    for thisform in possibleforms
      if thisform[2] && thisform[2]!=""   # Has a form name
        thisformname = thisform[2]
      else   # Necessarily applies only to form 0
        case thisform[1]
        when 0; thisformname = _INTL("雄性")
        when 1; thisformname = _INTL("雌性")
        else
          thisformname = (multiforms) ? _INTL("默认形态") : _INTL("无性别")
        end
      end
      # Push to available array
      gendertopush = (thisform[1]==2) ? 0 : thisform[1]
      available.push([thisformname,gendertopush,thisform[0]])
    end
    return available
  end

  def drawPage(page)
    overlay = @sprites["overlay"].bitmap
    overlay.clear
    overlay_shiny = @sprites["overlay_shiny"].bitmap
    overlay_shiny.clear
    overlay_data = @sprites["overlay_data"].bitmap
    overlay_data.clear
    # Make certain sprites visible
    @sprites["infosprite"].visible    = (@page==1)
    @sprites["areamap"].visible       = (@page==5) if @sprites["areamap"]
    @sprites["areahighlight"].visible = (@page==5) if @sprites["areahighlight"]
    @sprites["areaoverlay"].visible   = (@page==5) if @sprites["areaoverlay"]
    
    @sprites["form"].visible     = (@page==4) if @sprites["form"]
    @sprites["formicon"].visible = (@page==4) if @sprites["formicon"]
    
    @sprites["forms"].visible     = (@page==4 && @show_shiny==0) if @sprites["forms"]
    @sprites["formicons"].visible = (@page==4 && @show_shiny==0) if @sprites["formicons"]
    
    @sprites["formss"].visible     = (@page==4 && @show_shiny==1) if @sprites["formss"]
    @sprites["formiconss"].visible = (@page==4 && @show_shiny==1) if @sprites["formiconss"]
    # Draw page-specific information
    case page
    when 1; drawPageInfo
    when 2; drawPageData
    when 3; drawPageEvolution
    when 4; drawPageForms
    when 5; drawPageArea
    end
  end


  def drawPageInfo
# Sets the Scrolling Background of the Entry Page, as well as the overlay on top 
# of it
    @sprites["background"].setBitmap(_INTL("Graphics/Pictures/Pokedex/bg_info"))
    @sprites["infoverlay"].setBitmap(_INTL("Graphics/Pictures/Pokedex/info_overlay"))
    overlay = @sprites["overlay"].bitmap
    base   = Color.new(82,82,90)
    shadow = Color.new(165,165,173)
    imagepos = []
    if @brief
# Sets the Scrolling Background of the Entry Scena when capturing a wild Pokémon,
# as well as the overlay on top of it
      #imagepos.push([_INTL("Graphics/Pictures/Pokedex/overlay_info"),0,0])
      @sprites["background"].setBitmap(_INTL("Graphics/Pictures/Pokedex/bg_capture"))
      @sprites["infoverlay"].setBitmap(_INTL("Graphics/Pictures/Pokedex/capture_overlay"))
      @sprites["capturebar"].setBitmap(_INTL("Graphics/Pictures/Pokedex/overlay_info"))
    end
    # Write various bits of text
    indexText = "???"
    if @dexlist[@index][4]>0
      indexNumber = @dexlist[@index][4]
      indexNumber -= 1 if @dexlist[@index][5]
      indexText = sprintf("%04d",indexNumber)
    end

# This bit of the code woudn't have been possible without the help of NettoHikari.
# He helped me to set the Sprites and Texts differently, depending on if the 
# Pokédex Entry Scene is playing, when the payer is capturing a Wild Pokémon, 
# or if the player is seeing the "normal" Dex Entry page on the Pokédex.
#
# Basically, this next lines changes the position of various text 
# (height, weight, the Name's Species, etc), depending on if the 
# Pokédex Entry Scene is playing, when the payer is capturing a Wild Pokémon, 
# or if the player is seeing the "normal" Dex Entry page on the Pokédex.
    
    if @brief
      textpos = [
       [_INTL("宝可梦的资料记录在图鉴了！"),114,4,0,Color.new(255,255,255),Color.new(165,165,173)],
       [_INTL("{1} {2}",indexText,PBSpecies.getName(@species)),
          328+64,61,0,Color.new(82,82,90),Color.new(165,165,173)],
       [_INTL("身高"),334+64,176,0,base,shadow],
       [_INTL("体重"),334+64,206,0,base,shadow]        
    ]
    else
    textpos = [
       [_INTL("{1} {2}",indexText,PBSpecies.getName(@species)),
          328+64,23,0,Color.new(82,82,90),Color.new(165,165,173)],
       [_INTL("身高"),334+64,138,0,base,shadow],
       [_INTL("体重"),334+64,168,0,base,shadow]
      # Pokémon Registration Complete
    ]
    end   
    
    if $Trainer.owned[@species]
      speciesData = pbGetSpeciesData(@species,@form)
      fSpecies = pbGetFSpeciesFromForm(@species,@form)
      # Write the kind
      kind = pbGetMessage(MessageTypes::Kinds,fSpecies)
      kind = pbGetMessage(MessageTypes::Kinds,@species) if !kind || kind==""
      if @brief
        textpos.push([_INTL("{1}宝可梦",kind),420+64,96,2,base,shadow])
      else
        textpos.push([_INTL("{1}宝可梦",kind),420+64,58,2,base,shadow])
      end    
      # Write the height and weight
      height = speciesData[SpeciesHeight] || 1
      weight = speciesData[SpeciesWeight] || 1
      if @brief
        textpos.push([_ISPRINTF("{1:.1f} m",height/10.0),510+64,176,1,base,shadow])
      else
        textpos.push([_ISPRINTF("{1:.1f} m",height/10.0),510+64,138,1,base,shadow])
      end
      if @brief
        textpos.push([_ISPRINTF("{1:.1f} kg",weight/10.0),510+64,206,1,base,shadow])
      else
        textpos.push([_ISPRINTF("{1:.1f} kg",weight/10.0),510+64,168,1,base,shadow])
      end
      # Draw the Pokédex entry text. Changed
      base   = Color.new(255,255,255)
      shadow = Color.new(165,165,173)
      entry = pbGetMessage(MessageTypes::Entries,fSpecies)
      entry = pbGetMessage(MessageTypes::Entries,@species) if !entry || entry==""
      if @brief
        drawTextEx(overlay,39,254+64,Graphics.width-(40*2),4,entry,base,shadow)
      else
        drawTextEx(overlay,39,216+64,Graphics.width-(40*2),4,entry,base,shadow)
      end
      # Draw the footprint. Changed
      #footprintfile = pbPokemonFootprintFile(@species,@form)
      #if footprintfile
      #  footprint = BitmapCache.load_bitmap(footprintfile)
      #  if @brief
      #    overlay.blt(278,150,footprint,footprint.rect)
      #  else
      #    overlay.blt(278,112,footprint,footprint.rect)
      #  end
      #  footprint.dispose
      #end
      # Draw the shape. Changed
      shapebitmap = AnimatedBitmap.new("Graphics/Pictures/Pokedex/icon_shapes")
      shape = speciesData[SpeciesShape] || 1
      shapes = []
      shaperect = Rect.new(0,(shape-1)*60,60,60)
      if @brief
        overlay.blt(260+64,138,shapebitmap.bitmap,shaperect)
      else
        overlay.blt(260+64,100,shapebitmap.bitmap,shaperect)
      end
      # Show the owned icon. Changed
      if @brief
        imagepos.push(["Graphics/Pictures/Pokedex/icon_own",261+64,57])
      else
        imagepos.push(["Graphics/Pictures/Pokedex/icon_own",261+64,19])
      end
      # Draw the type icon(s). Changed
      type1 = speciesData[SpeciesType1] || 0
      type2 = speciesData[SpeciesType2] || type1
      type1rect = Rect.new(0,type1*32,96,32)
      type2rect = Rect.new(0,type2*32,96,32)
      if @brief
        overlay.blt(330+64,133,@typebitmap.bitmap,type1rect)
      else
        overlay.blt(330+64,97,@typebitmap.bitmap,type1rect)
      end
      if @brief
        overlay.blt(397+64,133,@typebitmap.bitmap,type2rect) if type1!=type2
      else
        overlay.blt(397+64,97,@typebitmap.bitmap,type2rect) if type1!=type2
      end
    else
  # This bit of the code below is simply the Entry Page when you have seen the 
  # Pokémon, but did'nt capture it yet.    
      # Write the kind. Changed
      textpos.push([_INTL("？？？宝可梦"),348+64,58,0,base,shadow])
      # Write the height and weight. Changed
      textpos.push([_INTL("???.? m"),489+32+64,138,1,base,shadow])
      textpos.push([_INTL("???.? kg"),489+32+64,168,1,base,shadow])
    end
    # Draw all text
    pbDrawTextPositions(@sprites["overlay"].bitmap,textpos)
    # Draw all images
    pbDrawImagePositions(overlay,imagepos)
  end

  def drawPageArea
# Sets the Scrolling Background of the Area Page, as well as the overlay on top of it
    @sprites["background"].setBitmap(_INTL("Graphics/Pictures/Pokedex/bg_area"))
    @sprites["infoverlay"].setBitmap(_INTL("Graphics/Pictures/Pokedex/map_overlay"))
    @sprites["infoverlay"].z = @sprites["background"].z + 1
    overlay = @sprites["overlay"].bitmap
    base   = Color.new(88,88,80)
    shadow = Color.new(168,184,184)
    @sprites["areahighlight"].bitmap.clear
    # Fill the array "points" with all squares of the region map in which the
    # species can be found. Unchanged.
    points = []
    mapwidth = 1+PokemonRegionMap_Scene::RIGHT-PokemonRegionMap_Scene::LEFT
    encdata = pbLoadEncountersData
    for enc in encdata.keys
      enctypes = encdata[enc][1]
      if pbFindEncounter(enctypes,@species)
        mappos = pbGetMetadata(enc,MetadataMapPosition)
        if mappos && mappos[0]==@region
          showpoint = true
          for loc in @mapdata[@region][2]
            showpoint = false if loc[0]==mappos[1] && loc[1]==mappos[2] &&
                                 loc[7] && !$game_switches[loc[7]]
          end
          if showpoint
            mapsize = pbGetMetadata(enc,MetadataMapSize)
            if mapsize && mapsize[0] && mapsize[0]>0
              sqwidth  = mapsize[0]
              sqheight = (mapsize[1].length*1.0/mapsize[0]).ceil
              for i in 0...sqwidth
                for j in 0...sqheight
                  if mapsize[1][i+j*sqwidth,1].to_i>0
                    points[mappos[1]+i+(mappos[2]+j)*mapwidth] = true
                  end
                end
              end
            else
              points[mappos[1]+mappos[2]*mapwidth] = true
            end
          end
        end
      end
    end
    # Draw coloured squares on each square of the region map with a nest
    pointcolor   = Color.new(0,248,248)
    pointcolorhl = Color.new(192,248,248)
    sqwidth = PokemonRegionMap_Scene::SQUAREWIDTH
    sqheight = PokemonRegionMap_Scene::SQUAREHEIGHT
    for j in 0...points.length
      if points[j]
        x = (j%mapwidth)*sqwidth
        x += (Graphics.width-@sprites["areamap"].bitmap.width)/2
        y = (j/mapwidth)*sqheight
        y += (Graphics.height-@sprites["areamap"].bitmap.height)/2
        @sprites["areahighlight"].bitmap.fill_rect(x,y,sqwidth,sqheight,pointcolor)
        if j-mapwidth<0 || !points[j-mapwidth]
          @sprites["areahighlight"].bitmap.fill_rect(x,y-2,sqwidth,2,pointcolorhl)
        end
        if j+mapwidth>=points.length || !points[j+mapwidth]
          @sprites["areahighlight"].bitmap.fill_rect(x,y+sqheight,sqwidth,2,pointcolorhl)
        end
        if j%mapwidth==0 || !points[j-1]
          @sprites["areahighlight"].bitmap.fill_rect(x-2,y,2,sqheight,pointcolorhl)
        end
        if (j+1)%mapwidth==0 || !points[j+1]
          @sprites["areahighlight"].bitmap.fill_rect(x+sqwidth,y,2,sqheight,pointcolorhl)
        end
      end
    end
    # Set the text
# Changes the color of the text, to the one used in BW
    base   = Color.new(255,255,255)
    shadow = Color.new(165,165,173)
    textpos = []
    if points.length==0
      pbDrawImagePositions(overlay,[
         [sprintf("Graphics/Pictures/Pokedex/overlay_areanone"),108+32,148]
      ])
# Changes the postion of the Area unknown text, as well as the color of it, 
# to the one used in BW
      textpos.push([_INTL("栖息地不明"),Graphics.width/2,152,2,base,shadow])
    end
# Minor changes to the color of the text, to mimic the one used in BW
    textpos.push([pbGetMessage(MessageTypes::RegionNames,@region),88,4,2,Color.new(255,255,255),Color.new(115,115,115)])
    pkmnname = $Trainer.seen[@species] ? PBSpecies.getName(@species) : "??????"
    textpos.push([_INTL("{1}的分布",pkmnname),
       Graphics.width/1.4+32,4,2,Color.new(255,255,255),Color.new(115,115,115)])
    pbDrawTextPositions(overlay,textpos)
  end

  def drawPageForms
# Sets the Scrolling Background of the Forms Page, as well as the overlay on top of it
    @sprites["background"].setBitmap(_INTL("Graphics/Pictures/Pokedex/bg_forms"))
    @sprites["infoverlay"].setBitmap(_INTL("Graphics/Pictures/Pokedex/forms_overlay"))
    overlay = @sprites["overlay"].bitmap
    overlay_shiny = @sprites["overlay_shiny"].bitmap
# Changes the color of the text, to the one used in BW
    base   = Color.new(255,255,255)
    shadow = Color.new(165,165,173)
    # Write species and form name
    formname = ""
    for i in @available
      if i[1]==@gender && i[2]==@form
        formname = i[0]; break
      end
    end
    textpos = [
       [_INTL("形象"),88,4,2,Color.new(255,255,255),Color.new(115,115,115)],
       [PBSpecies.getName(@species), 384-32, 72, 2, base, shadow],
       [formname, 384-32, 72+32, 2, base, shadow],
    ]
    # Draw all text
    pbDrawTextPositions(overlay,textpos)
    if $Trainer.seen[@species]
      text_switch = [
         [_INTL("[C]:切换形态 [Z]:切换{1}",@switch_msg[@show_shiny]),Graphics.width/1.6,4,2,Color.new(255,255,255),Color.new(115,115,115)]
      ]
      pbDrawTextPositions(overlay_shiny,text_switch)
    end
  end

  def drawPageData
# Sets the Scrolling Background of the Forms Page, as well as the overlay on top of it
    @sprites["background"].setBitmap(_INTL("Graphics/Pictures/Pokedex/bg_data"))
    @sprites["infoverlay"].setBitmap(_INTL("Graphics/Pictures/Pokedex/data_overlay"))
    overlay = @sprites["overlay"].bitmap
    overlay_data = @sprites["overlay_data"].bitmap
# Changes the color of the text, to the one used in BW
    base   = Color.new(255,255,255)
    shadow = Color.new(165,165,173)
    # Write data
    pkmnname = $Trainer.seen[@species] ? PBSpecies.getName(@species) : "??????"
    form_name = pbGetMessage(MessageTypes::FormNames,pbGetFSpeciesFromForm(@species,@form))
    form_name = "默认形态"  if form_name == ""
    textpos = [
       [_INTL("数据"),88,4,2,Color.new(255,255,255),Color.new(115,115,115)],
       [pkmnname+"-"+form_name,Graphics.width-66,4,1,Color.new(255,255,255),Color.new(115,115,115)]
    ]
    pbDrawTextPositions(overlay,textpos)
    pbUpdatePokemonData
  end

  def pbUpdatePokemonData
    overlay_data = @sprites["overlay_data"].bitmap
    overlay_data.clear
    base   = Color.new(255,255,255)
    shadow = Color.new(165,165,173)
    highlight = Color.new(255,255,192)
    highshadow = Color.new(160,160,115)
    datapos = [
      [_INTL("特性"),100,40,2,highlight,highshadow]
    ]
    # 特性
    abis = []
    abilities = pbGetSpeciesData(@species,@form,SpeciesAbilities)
    if abilities.is_a?(Array)
      abilities.each_with_index { |a,i| abis.push(a) if a && a>0 }
    else
      abis.push(abilities) if abilities>0
    end
    ys = [60, 80, 100]
    abis.each_index do |i|
      datapos.push(
        [_INTL("特性{1}:{2}",(i+1),PBAbilities.getName(abis[i])),
               24,ys[i],0,base,shadow]
      )
    end
    # 隐藏特性
    h_abis = []
    hiddenAbil = pbGetSpeciesData(@species,@form,SpeciesHiddenAbility)
    if hiddenAbil.is_a?(Array)
      hiddenAbil.each_with_index { |a,i| h_abis.push(a) if a && a>0 }
    else
      h_abis.push(hiddenAbil) if hiddenAbil>0
    end
    if h_abis[0]
      datapos.push(
        [_INTL("隐藏特性:{1}",PBAbilities.getName(h_abis[0])),
               24,ys[2],0,base,shadow]
      )
    end
    # 种族值
    bvs   = pbGetSpeciesData(@species,@form,SpeciesBaseStats)
    total = 0
    bvs.each { |bv| total += bv }
    datapos.push(
      [_INTL("种族值总和:{1}",total),266+32+64,40,2,highlight,highshadow]
    )
    hp      = bvs[PBStats::HP]
    attack  = bvs[PBStats::ATTACK]
    defense = bvs[PBStats::DEFENSE]
    speed   = bvs[PBStats::SPEED]
    spAtk   = bvs[PBStats::SPATK]
    spDef   = bvs[PBStats::SPDEF]
    datapos.push(
      [_INTL("HP:  {1}",hp),        184+32+64,ys[0],0,base,shadow],
      [_INTL("攻击:{1}",attack),    184+32+64,ys[1],0,base,shadow],
      [_INTL("防御:{1}",defense),   184+32+64,ys[2],0,base,shadow],
      [_INTL("速度:{1}",speed),     266+32+64,ys[0],0,base,shadow],
      [_INTL("特攻:{1}",spAtk),     266+32+64,ys[1],0,base,shadow],
      [_INTL("特防:{1}",spDef),     266+32+64,ys[2],0,base,shadow]
    )
    # 野生持有物
    datapos.push(
      [_INTL("野生持有物"),450+32+64,40,2,highlight,highshadow]
    )
    commonItem   = pbGetSpeciesData(@species,@form,SpeciesWildItemCommon)   || 0
    unCommonItem = pbGetSpeciesData(@species,@form,SpeciesWildItemUncommon) || 0
    rareItem     = pbGetSpeciesData(@species,@form,SpeciesWildItemRare)     || 0
    if commonItem == unCommonItem && unCommonItem == rareItem
      if commonItem>0
        datapos.push([_INTL("100%:{1}",PBItems.getName(commonItem)),380+32+64,60,0,base,shadow])
      else
        datapos.push([_INTL("100%:----"),380+32+64,60,0,base,shadow])
      end
    else
      commonName   = (commonItem>0)   ? PBItems.getName(commonItem)   : "----"
      unCommonName = (unCommonItem>0) ? PBItems.getName(unCommonItem) : "----"
      rareName     = (rareItem>0)     ? PBItems.getName(rareItem)     : "----"
      datapos.push(
        [_INTL("50%: {1}",commonName),   380+32+64,60,0,base,shadow],
        [_INTL("5%:  {1}",unCommonName), 380+32+64,80,0,base,shadow],
        [_INTL("1%:  {1}",rareName),     380+32+64,100,0,base,shadow]
      )
    end
    # 招式
if $Trainer.seen[@species] || (defined?($DEBUG) && $DEBUG) || $game_variables[25] == 0
  last_page = @data_show_type > 0 ? @data_show_type-1 : @data_msg.length-1
      next_page = @data_show_type < @data_msg.length-1 ? @data_show_type + 1 : 0
      datapos.push(
        [_INTL("◀[A]:{1}",@data_msg[last_page]), Graphics.width/2 - 80 - 32, 122, 1, base, shadow],
        [_INTL("【{1}】",@data_msg[@data_show_type]), Graphics.width/2, 122, 2, highlight, highshadow],
        [_INTL("[S]:{1}▶",@data_msg[next_page]), Graphics.width/2 + 80 + 32, 122, 0, base, shadow]
      )
    end
    if $Trainer.seen[@species]
      xs = [24, 24+Graphics.width/3, 24+Graphics.width/3*2]
      ys = [144, 166, 188, 210, 232, 254, 276, 298, 320, 340]
      case @data_show_type
      when 0    # 先天招式
        moveset = pbGetSpeciesMoveset(@species,@form)
        moveList = []
        moveset.each do |m|
          next if m[0] > 1
          moveList.push(m)
        end
        if moveList.length > 0
          i = 0
          moveList.each do |m|
            case m[0]
            when 0
              datapos.push(
                [_INTL("进化:{1}", PBMoves.getName(m[1])), xs[i%3],ys[i/3],0,base,shadow]
              )
              i += 1
            when 1
              datapos.push(
                [_INTL("{1}级:{2}", m[0], PBMoves.getName(m[1])), xs[i%3],ys[i/3],0,base,shadow]
              )
              i += 1
            end
          end
        else
          datapos.push(
              [_INTL("没有先天招式"), Graphics.width / 2, ys[0], 2, base, shadow]
            )
        end
      when 1    # 升级招式
        moveset = pbGetSpeciesMoveset(@species,@form)
        moveList = []
        moveset.each do |m|
          next if m[0] <= 1
          moveList.push(m)
        end
        if moveList.length > 0
          i = 0
          moveList.each do |m|
            datapos.push(
              [_INTL("{1}级:{2}", m[0], PBMoves.getName(m[1])), xs[i%3],ys[i/3],0,base,shadow]
            )
            i += 1
          end
        else
          datapos.push(
              [_INTL("没有升级招式"), Graphics.width / 2, ys[0], 2, base, shadow]
            )
        end
      when 2    # 机器招式
        fSpecies = pbGetFSpeciesFromForm(@species,@form)
        moveList = [[],[],[],[],[],[],[],[],[],[],[],[],[],[],[]]
        count = 0
        items = [[383,384,385,386,387,388,801],288..382,621..625,692..791,1200..1299]
        items.each do |range|
          range.each do |it|
            next if !pbIsMachine?(it)
            item = PBItems.getName(it)
            m = pbGetMachine(it)
            move = PBMoves.getName(m)
            next if !pbSpeciesCompatible?(fSpecies, m)
            moveList[count/24].push([item,move])
            count += 1
          end
        end
        moveList.each_index do |i|
          moveList[i] = nil if moveList[i].length == 0
        end
        moveList.compact!
        @move_page = 0 if @move_page >= moveList.length
        if moveList.length > 0 && moveList[@move_page].length > 0
          i = 0
          moveList[@move_page].each do |m|
            datapos.push(
              [_INTL("{1}:{2}", m[0],m[1]), xs[i%3], ys[i/3], 0, base, shadow]
            )
            i += 1
          end
          if moveList.length > 1
            datapos.push(
              [_INTL("{1}/{2}页 [Z]:显示下一页", @move_page + 1, moveList.length),
               Graphics.width / 2, ys[8], 2, base, shadow]
            )
          end
        else
          datapos.push(
              [_INTL("没有可学习的学习器招式"), Graphics.width / 2, ys[0], 2, base, shadow]
            )
        end
      when 3    # 蛋招式
        fSpecies = pbGetFSpeciesFromForm(@species,@form)
        item = getConst(PBItems,:EVERSTONE)
        babyspecies = pbGetBabySpecies(fSpecies,item)
        moveList = pbGetSpeciesEggMoves(babyspecies,@form)
        if moveList.length > 0
          i = 0
          moveList.each do |m|
            datapos.push(
              [_INTL("{1}", PBMoves.getName(m)), xs[i%3],ys[i/3],0,base,shadow]
            )
            i += 1
          end
        else
          datapos.push(
              [_INTL("没有蛋招式"), Graphics.width / 2, ys[0], 2, base, shadow]
            )
        end
      end
    else
      datapos.push(
        [_INTL("尚未记录招式数据，请发现宝可梦后查看"), Graphics.width / 2,Graphics.height / 2, 2, base, shadow]
      )
    end
    pbDrawTextPositions(overlay_data,datapos)
  end
  
  def drawPageEvolution
    overlay_data = @sprites["overlay_data"].bitmap
    overlay_data.clear
# Sets the Scrolling Background of the Forms Page, as well as the overlay on top of it
    @sprites["background"].setBitmap(_INTL("Graphics/Pictures/Pokedex/bg_data"))
    @sprites["infoverlay"].setBitmap(_INTL("Graphics/Pictures/Pokedex/evo_overlay"))
    overlay = @sprites["overlay"].bitmap
    overlay_data = @sprites["overlay_data"].bitmap
# Changes the color of the text, to the one used in BW
    base   = Color.new(255,255,255)
    shadow = Color.new(165,165,173)
    # Write data
    pkmnname = $Trainer.seen[@species] ? PBSpecies.getName(@species) : "??????"
    form_name = pbGetMessage(MessageTypes::FormNames,pbGetFSpeciesFromForm(@species,@form))
    form_name = "默认形态"  if form_name == ""
    textpos = [
      [_INTL("进化"),88,4,2,Color.new(255,255,255),Color.new(115,115,115)],
      [pkmnname+"-"+form_name,Graphics.width-66,4,1,Color.new(255,255,255),Color.new(115,115,115)],
    ]
    datapos = []
    xs = [16, Graphics.width / 2 + 16]
    ys = [96, 118, 140, 162, 184, 206, 228, 250, 272, 294, 316]
    fSpecies = pbGetFSpeciesFromForm(@species,@form)
    textpos.push([_INTL("进化成谁"),Graphics.width/2,54,2,base,shadow])
    pbDrawTextPositions(overlay,textpos)
    evos = pbGetEvolvedFormData(fSpecies,true)
    if evos != nil && evos.length > 0
      i = 0
      evos.each do |evo|  # [Method, parameter, species]
        method      = evo[0]
        parameter   = evo[1]
        new_species = evo[2]
        next if new_species==0
        cnew_species = getConstantName(PBSpecies,new_species) rescue pbGetSpeciesConst(new_species)
        evo_type = getConstantName(PBEvolution,method) rescue pbGetEvolutionConst(method)
        evoname = PBEvolution.getName(evo_type)
        next if !cnew_species || cnew_species==""
        param_type = PBEvolution.getFunction(method, "parameterType")
        has_param = !PBEvolution.hasFunction?(method, "parameterType") || param_type != nil
        if has_param
          if param_type
            parameter = (getConstantName(param_type, parameter) rescue parameter)
          end
          if evo_type == "Location"
            mapinfos = ($RPGVX) ? load_data("Data/MapInfos.rvdata") : load_data("Data/MapInfos.rxdata")
            parameter = mapinfos[parameter].name
          elsif evo_type == "Region"
            parameter = ["柊埃地区", "群岛地区", "天空城"][parameter]
          elsif evo_type.include?("Item") || evo_type == "LastEvolution"
            parameter = PBItems.getName(parameter)
          elsif evo_type.include?("MoveType")
            parameter = PBTypes.getName(parameter) + "属性"
          elsif evo_type.include?("Move")
            parameter = PBMoves.getName(parameter)
          elsif evo_type == "HasInParty" || evo_type == "TradeSpecies"
            parameter = PBSpecies.getName(parameter)
          end
          datapos.push(
            [_INTL("{1}{2}→{3}", evoname, parameter, 
             PBSpecies.getName(cnew_species)),xs[i/11], ys[i%11], 0, base, shadow]
          )
        else
          datapos.push(
            [_INTL("{1}→{2}", evoname, PBSpecies.getName(cnew_species)),xs[i/11], ys[i%11], 0, base, shadow]
          )
        end
        i += 1
      end
    else
      datapos.push([_INTL("无法继续进化"), Graphics.width / 2, ys[0], 2, base, shadow])
    end
    pbDrawTextPositions(overlay_data,datapos)
  end

  def pbGoToPrevious
    newindex = @index
    while newindex>0
      newindex -= 1
      if $Trainer.seen[@dexlist[newindex][0]]
        @index = newindex
        break
      end
    end
  end

  def pbGoToNext
    newindex = @index
    while newindex<@dexlist.length-1
      newindex += 1
      if $Trainer.seen[@dexlist[newindex][0]]
        @index = newindex
        break
      end
    end
  end
  
  def pbChooseForm
    index = 0
    for i in 0...@available.length
      if @available[i][1]==@gender && @available[i][2]==@form
        index = i
        break
      end
    end
    oldindex = -1
    loop do
      if oldindex!=index
        $Trainer.formlastseen[@species][0] = @available[index][1]
        $Trainer.formlastseen[@species][1] = @available[index][2]
        pbUpdateDummyPokemon
        drawPage(@page)
        @sprites["uparrow"].visible   = (index>0)
        @sprites["downarrow"].visible = (index<@available.length-1)
        oldindex = index
      end
      Graphics.update
      Input.update
      pbUpdate
      if Input.trigger?(Input::UP)
        pbPlayCursorSE
        index = (index+@available.length-1) % @available.length
      elsif Input.trigger?(Input::DOWN)
        pbPlayCursorSE
        index = (index+1) % @available.length
      elsif Input.trigger?(Input::B)
        pbPlayCancelSE
        break
      elsif Input.trigger?(Input::C)
        pbPlayDecisionSE
        break
      end
    end
    @sprites["uparrow"].visible   = false
    @sprites["downarrow"].visible = false
  end

  def pbScene
    pbPlayCrySpecies(@species,@form) if $Trainer.seen[@species]
    loop do
      Graphics.update
      Input.update
      pbUpdate
      dorefresh = false
      if Input.trigger?(Input::A)
        pbSEStop
        case @page
        when 1
          pbPlayCrySpecies(@species,@form) if $Trainer.seen[@species]
        when 2
          if @data_show_type == 2 && $Trainer.seen[@species]
            @move_page += 1
            pbPlayCursorSE
            pbUpdatePokemonData
          end
        when 4
          if $Trainer.seen[@species]
            pbPlayCursorSE
            @show_shiny = 1 - @show_shiny
            pbUpdateDummyPokemon
          end
        end
      elsif Input.trigger?(Input::B)
        pbPlayCloseMenuSE
        break
      elsif Input.trigger?(Input::C)
        if @page==4   # Forms（现在是第4页）
          if @available.length>1
            pbPlayDecisionSE
            pbChooseForm
            dorefresh = true
          end
        end
      elsif Input.trigger?(Input::UP)
        oldindex = @index
        pbGoToPrevious
        if @index!=oldindex
          pbUpdateDummyPokemon
          @available = pbGetAvailableForms
          pbSEStop
          (@page==1 && $Trainer.seen[@species]) ? pbPlayCrySpecies(@species,@form) : pbPlayCursorSE
          dorefresh = true
        end
      elsif Input.trigger?(Input::DOWN)
        oldindex = @index
        pbGoToNext
        if @index!=oldindex
          pbUpdateDummyPokemon
          @available = pbGetAvailableForms
          pbSEStop
          (@page==1 && $Trainer.seen[@species]) ? pbPlayCrySpecies(@species,@form) : pbPlayCursorSE
          dorefresh = true
        end
      elsif Input.trigger?(Input::LEFT)
        oldpage = @page
        @page -= 1
        @page = 5 if @page<1
        @page = 1 if @page>5
        if @page!=oldpage
          pbSEPlay("GUI naming tab swap start")
          dorefresh = true
        end
      elsif Input.trigger?(Input::RIGHT)
        oldpage = @page
        @page += 1
        @page = 5 if @page<1
        @page = 1 if @page>5
        if @page!=oldpage
          pbSEPlay("GUI naming tab swap start")
          dorefresh = true
        end
      elsif Input.trigger?(Input::L)
        if @page==2 && $Trainer.seen[@species]
          @data_show_type -= 1
          @data_show_type = @data_msg.length - 1 if @data_show_type < 0
          pbSEPlay("GUI naming tab swap start")
          pbUpdatePokemonData
        end
      elsif Input.trigger?(Input::R)
        if @page==2 && $Trainer.seen[@species]
          @data_show_type += 1
          @data_show_type = 0 if @data_show_type >= @data_msg.length
          pbSEPlay("GUI naming tab swap start")
          pbUpdatePokemonData
        end
      end
      if dorefresh
        drawPage(@page)
      end
    end
    return @index
  end

  def pbSceneBrief
    pbPlayCrySpecies(@species,@form) if $Trainer.seen[@species]
    loop do
      Graphics.update
      Input.update
      pbUpdate
      if Input.trigger?(Input::A)
        pbSEStop
        pbPlayCrySpecies(@species,@form) if $Trainer.seen[@species]
      elsif Input.trigger?(Input::B)
        pbPlayCloseMenuSE
        break
      elsif Input.trigger?(Input::C)
        pbPlayDecisionSE
        break
      end
    end
  end
end


class PokemonPokedexInfoScreen
  def initialize(scene)
    @scene = scene
  end

  def pbStartScreen(dexlist,index,region)
    @scene.pbStartScene(dexlist,index,region)
    ret = @scene.pbScene
    @scene.pbEndScene
    return ret   # Index of last species viewed in dexlist
  end

  def pbStartSceneSingle(species)   # For use from a Pokémon's summary screen
    region = -1
    if USE_CURRENT_REGION_DEX
      region = pbGetCurrentRegion
      region = -1 if region>=$PokemonGlobal.pokedexUnlocked.length-1
    else
      region = $PokemonGlobal.pokedexDex # National Dex -1, regional dexes 0 etc.
    end
    dexnum = pbGetRegionalNumber(region,species)
    dexnumshift = DEXES_WITH_OFFSETS.include?(region)
    dexlist = [[species,PBSpecies.getName(species),0,0,dexnum,dexnumshift]]
    @scene.pbStartScene(dexlist,0,region)
    @scene.pbScene
    @scene.pbEndScene
  end

  def pbDexEntry(species)   # For use when capturing a new species
    @scene.pbStartSceneBrief(species)
    @scene.pbSceneBrief
    @scene.pbEndScene
  end
end