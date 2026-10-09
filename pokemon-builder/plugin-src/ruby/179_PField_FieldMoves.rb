#===============================================================================
# Hidden move handlers
#===============================================================================
class MoveHandlerHash < HandlerHash
  def initialize
    super(:PBMoves)
  end
end



module HiddenMoveHandlers
  CanUseMove     = MoveHandlerHash.new
  ConfirmUseMove = MoveHandlerHash.new
  UseMove        = MoveHandlerHash.new

  def self.addCanUseMove(item,proc);     CanUseMove.add(item,proc);     end
  def self.addConfirmUseMove(item,proc); ConfirmUseMove.add(item,proc); end
  def self.addUseMove(item,proc);        UseMove.add(item,proc);        end

  def self.hasHandler(item)
    return CanUseMove[item]!=nil && UseMove[item]!=nil
  end

  # Returns whether move can be used
  def self.triggerCanUseMove(item,pokemon,showmsg)
    return false if !CanUseMove[item]
    return CanUseMove.trigger(item,pokemon,showmsg)
  end

  # Returns whether the player confirmed that they want to use the move
  def self.triggerConfirmUseMove(item,pokemon)
    return true if !ConfirmUseMove[item]
    return ConfirmUseMove.trigger(item,pokemon)
  end

  # Returns whether move was used
  def self.triggerUseMove(item,pokemon)
    return false if !UseMove[item]
    return UseMove.trigger(item,pokemon)
  end
end



def pbCanUseHiddenMove?(pkmn,move,showmsg=true)
  return HiddenMoveHandlers.triggerCanUseMove(move,pkmn,showmsg)
end

def pbConfirmUseHiddenMove(pokemon,move)
  return HiddenMoveHandlers.triggerConfirmUseMove(move,pokemon)
end

def pbUseHiddenMove(pokemon,move)
  return HiddenMoveHandlers.triggerUseMove(move,pokemon)
end

# Unused
def pbHiddenMoveEvent
  Events.onAction.trigger(nil)
end

def pbCheckHiddenMoveBadge(badge=-1,showmsg=true)
  return true if badge<0   # No badge requirement
  return true if $DEBUG
  if (FIELD_MOVES_COUNT_BADGES) ? $Trainer.numbadges>=badge : $Trainer.badges[badge]
    return true
  end
  pbMessage(_INTL("对不起，\n这需要拥有对应的徽章。")) if showmsg
  return false
end



#===============================================================================
# Hidden move animation
#===============================================================================
def pbHiddenMoveAnimation(pokemon)
  return false if !pokemon
  viewport=Viewport.new(0,0,0,0)
  viewport.z=99999
  bg=Sprite.new(viewport)
  bg.bitmap=BitmapCache.load_bitmap("Graphics/Pictures/hiddenMovebg")
  sprite=PokemonSprite.new(viewport)
  sprite.setOffset(PictureOrigin::Center)
  sprite.setPokemonBitmap(pokemon)
  sprite.z=1
  sprite.visible=false
  strobebitmap=AnimatedBitmap.new("Graphics/Pictures/hiddenMoveStrobes")
  strobes=[]
  15.times do |i|
    strobe=BitmapSprite.new(26*2,8*2,viewport)
    strobe.bitmap.blt(0,0,strobebitmap.bitmap,Rect.new(0,(i%2)*8*2,26*2,8*2))
    strobe.z=((i%2)==0 ? 2 : 0)
    strobe.visible=false
    strobes.push(strobe)
  end
  strobebitmap.dispose
  interp=RectInterpolator.new(
     Rect.new(0,Graphics.height/2,Graphics.width,0),
     Rect.new(0,(Graphics.height-bg.bitmap.height)/2,Graphics.width,bg.bitmap.height),
     40/4)
  ptinterp=nil
  phase=1
  frames=0
  strobeSpeed = 64*20/40
  loop do
    Graphics.update
    Input.update
    sprite.update
    case phase
    when 1   # Expand viewport height from zero to full
      interp.update
      interp.set(viewport.rect)
      bg.oy=(bg.bitmap.height-viewport.rect.height)/2
      if interp.done?
        phase=2
        ptinterp=PointInterpolator.new(
           Graphics.width+(sprite.bitmap.width/2),bg.bitmap.height/2,
           Graphics.width/2,bg.bitmap.height/2,
           40*4/10)
      end
    when 2   # Slide Pokémon sprite in from right to centre
      ptinterp.update
      sprite.x=ptinterp.x
      sprite.y=ptinterp.y
      sprite.visible=true
      if ptinterp.done?
        phase=3
        pbPlayCry(pokemon)
        frames=0
      end
    when 3   # Wait
      frames+=1
      if frames>40*3/4
        phase=4
        ptinterp=PointInterpolator.new(
           Graphics.width/2,bg.bitmap.height/2,
           -(sprite.bitmap.width/2),bg.bitmap.height/2,
           40*4/10)
        frames=0
      end
    when 4   # Slide Pokémon sprite off from centre to left
      ptinterp.update
      sprite.x=ptinterp.x
      sprite.y=ptinterp.y
      if ptinterp.done?
        phase=5
        sprite.visible=false
        interp=RectInterpolator.new(
           Rect.new(0,(Graphics.height-bg.bitmap.height)/2,Graphics.width,bg.bitmap.height),
           Rect.new(0,Graphics.height/2,Graphics.width,0),
           40/4)
      end
    when 5   # Shrink viewport height from full to zero
      interp.update
      interp.set(viewport.rect)
      bg.oy=(bg.bitmap.height-viewport.rect.height)/2
      phase=6 if interp.done?
    end
    # Constantly stream the strobes across the screen
    for strobe in strobes
      strobe.ox=strobe.viewport.rect.x
      strobe.oy=strobe.viewport.rect.y
      if !strobe.visible   # Initial placement of strobes
        randomY = 16*(1+rand(bg.bitmap.height/16-2))
        strobe.y = randomY+(Graphics.height-bg.bitmap.height)/2
        strobe.x = rand(Graphics.width)
        strobe.visible = true
      elsif strobe.x<Graphics.width   # Move strobe right
        strobe.x += strobeSpeed
      else   # Strobe is off the screen, reposition it to the left of the screen
        randomY = 16*(1+rand(bg.bitmap.height/16-2))
        strobe.y = randomY+(Graphics.height-bg.bitmap.height)/2
        strobe.x = -strobe.bitmap.width-rand(Graphics.width/4)
      end
    end
    pbUpdateSceneMap
    break if phase==6
  end
  sprite.dispose
  for strobe in strobes
    strobe.dispose
  end
  strobes.clear
  bg.dispose
  viewport.dispose
  return true
end

#===============================================================================
# Cut
#===============================================================================
def pbCut
  move = getID(PBMoves,:CUT)
  movefinder = pbCheckMove(move) || $PokemonBag.pbHasItem?(:HM01)
  if !pbCheckHiddenMoveBadge(BADGE_FOR_CUT,false) || (!$DEBUG && !movefinder)
    pbMessage(_INTL("这棵树看起来似乎可以被砍倒。"))
    return false
  end
  pbMessage(_INTL("这棵树看起来似乎可以被砍倒！"))
  if pbConfirmMessage(_INTL("想要砍倒它吗？"))
    speciesname = (movefinder.is_a?(PokeBattle_Pokemon)) ? movefinder.name : $Trainer.name
    pbMessage(_INTL("{1}使用了{2}！",speciesname,PBMoves.getName(move)))
    return true
  end
  return false
end

HiddenMoveHandlers::CanUseMove.add(:CUT,proc { |move,pkmn,showmsg|
  next false if !pbCheckHiddenMoveBadge(BADGE_FOR_CUT,showmsg)
  facingEvent = $game_player.pbFacingEvent
  if !facingEvent || facingEvent.name.downcase!="tree"
    pbMessage(_INTL("不能在这里使用。")) if showmsg
    next false
  end
  next true
})

HiddenMoveHandlers::UseMove.add(:CUT,proc { |move,pokemon|
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
  end
  facingEvent = $game_player.pbFacingEvent
  if facingEvent
    pbSmashEvent(facingEvent)
  end
  next true
})

def pbSmashEvent(event)
  return if !event
  if event.name.downcase=="tree";    pbSEPlay("Cut",80)
  elsif event.name.downcase=="rock"; pbSEPlay("Rock Smash",80)
  end
  pbMoveRoute(event,[
     PBMoveRoute::Wait,2,
     PBMoveRoute::TurnLeft,
     PBMoveRoute::Wait,2,
     PBMoveRoute::TurnRight,
     PBMoveRoute::Wait,2,
     PBMoveRoute::TurnUp,
     PBMoveRoute::Wait,2
  ])
  pbWait(40*4/10)
  event.erase
  $PokemonMap.addErasedEvent(event.id) if $PokemonMap
end



#===============================================================================
# Dig
#===============================================================================
HiddenMoveHandlers::CanUseMove.add(:DIG,proc { |move,pkmn,showmsg|
  escape = ($PokemonGlobal.escapePoint rescue nil)
  if !escape || escape==[]
    pbMessage(_INTL("不能在这里使用。")) if showmsg
    next false
  end
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。")) if showmsg
    next false
  end
  next true
})

HiddenMoveHandlers::ConfirmUseMove.add(:DIG,proc { |move,pkmn|
  escape = ($PokemonGlobal.escapePoint rescue nil)
  next false if !escape || escape==[]
  mapname = pbGetMapNameFromId(escape[0])
  next pbConfirmMessage(_INTL("想从这里出去回到{1}吗？",mapname))
})

HiddenMoveHandlers::UseMove.add(:DIG,proc { |move,pokemon|
  escape = ($PokemonGlobal.escapePoint rescue nil)
  if escape
    if !pbHiddenMoveAnimation(pokemon)
      pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
    end
    pbFadeOutIn {
      $game_temp.player_new_map_id    = escape[0]
      $game_temp.player_new_x         = escape[1]
      $game_temp.player_new_y         = escape[2]
      $game_temp.player_new_direction = escape[3]
      $scene.transfer_player
      $game_map.autoplay
      $game_map.refresh
    }
    pbEraseEscapePoint
    next true
  end
  next false
})


#===============================================================================
# Dive
#===============================================================================
def pbDive
  divemap = pbGetMetadata($game_map.map_id,MetadataDiveMap)
  return false if !divemap
  move = getID(PBMoves,:DIVE)
  movefinder = $PokemonBag.pbHasItem?(:HM06)   # 修改：只需要背包里有HM06
  if !pbCheckHiddenMoveBadge(BADGE_FOR_DIVE,false) || (!$DEBUG && !movefinder)
    pbMessage(_INTL("这里的水很深，\n宝可梦也许可以潜入水下。"))
    return false
  end
  if pbConfirmMessage(_INTL("这里的水很深，要使用潜水吗？"))
    speciesname = $Trainer.name   # 修改：统一显示训练师名字
    pbMessage(_INTL("{1}使用{2}！",speciesname,PBMoves.getName(move)))
    pbHiddenMoveAnimation(movefinder)
    pbFadeOutIn {
       $game_temp.player_new_map_id    = divemap
       $game_temp.player_new_x         = $game_player.x
       $game_temp.player_new_y         = $game_player.y
       $game_temp.player_new_direction = $game_player.direction
       $PokemonGlobal.surfing = false
       $PokemonGlobal.diving  = true
       pbUpdateVehicle
       $scene.transfer_player(false)
       $game_map.autoplay
       $game_map.refresh
    }
    return true
  end
  return false
end

def pbSurfacing
  return if !$PokemonGlobal.diving
  divemap = nil
  meta = pbLoadMetadata
  for i in 0...meta.length
    if meta[i] && meta[i][MetadataDiveMap] && meta[i][MetadataDiveMap]==$game_map.map_id
      divemap = i; break
    end
  end
  return if !divemap
  move = getID(PBMoves,:DIVE)
  movefinder = $PokemonBag.pbHasItem?(:HM06)   # 修改：只需要背包里有HM06
  if !pbCheckHiddenMoveBadge(BADGE_FOR_DIVE,false) || (!$DEBUG && !movefinder)
    pbMessage(_INTL("光从上方洒下，\n宝可梦也许可以浮出水面。"))
    return false
  end
  if pbConfirmMessage(_INTL("光从上方洒下，要使用潜水吗？"))
    speciesname = $Trainer.name   # 修改：统一显示训练师名字
    pbMessage(_INTL("{1}使用{2}！",speciesname,PBMoves.getName(move)))
    pbHiddenMoveAnimation(movefinder)
    pbFadeOutIn {
       $game_temp.player_new_map_id    = divemap
       $game_temp.player_new_x         = $game_player.x
       $game_temp.player_new_y         = $game_player.y
       $game_temp.player_new_direction = $game_player.direction
       $PokemonGlobal.surfing = true
       $PokemonGlobal.diving  = false
       pbUpdateVehicle
       $scene.transfer_player(false)
       surfbgm = pbGetMetadata(0,MetadataSurfBGM)
       (surfbgm) ?  pbBGMPlay(surfbgm) : $game_map.autoplayAsCue
       $game_map.refresh
    }
    return true
  end
  return false
end

def pbTransferUnderwater(mapid,x,y,direction=$game_player.direction)
  pbFadeOutIn {
    $game_temp.player_new_map_id    = mapid
    $game_temp.player_new_x         = x
    $game_temp.player_new_y         = y
    $game_temp.player_new_direction = direction
    $scene.transfer_player(false)
    $game_map.autoplay
    $game_map.refresh
  }
end

Events.onAction += proc { |_sender,_e|
  if $PokemonGlobal.diving
    if DIVING_SURFACE_ANYWHERE
      pbSurfacing
    else
      divemap = nil
      meta = pbLoadMetadata
      for i in 0...meta.length
        if meta[i] && meta[i][MetadataDiveMap] && meta[i][MetadataDiveMap]==$game_map.map_id
          divemap = i; break
        end
      end
      if divemap && PBTerrain.isDeepWater?($MapFactory.getTerrainTag(divemap,$game_player.x,$game_player.y))
        pbSurfacing
      end
    end
  else
    pbDive if PBTerrain.isDeepWater?($game_player.terrain_tag)
  end
}

HiddenMoveHandlers::CanUseMove.add(:DIVE,proc { |move,pkmn,showmsg|
  # 修改：检查背包中是否有HM06
  next false if !pbCheckHiddenMoveBadge(BADGE_FOR_DIVE,showmsg)
  next false if !$PokemonBag.pbHasItem?(:HM06)
  if $PokemonGlobal.diving
    next true if DIVING_SURFACE_ANYWHERE
    divemap = nil
    meta = pbLoadMetadata
    for i in 0...meta.length
      if meta[i] && meta[i][MetadataDiveMap] && meta[i][MetadataDiveMap]==$game_map.map_id
        divemap = i; break
      end
    end
    if !PBTerrain.isDeepWater?($MapFactory.getTerrainTag(divemap,$game_player.x,$game_player.y))
      pbMessage(_INTL("不能在这里使用。")) if showmsg
      next false
    end
  else
    if !pbGetMetadata($game_map.map_id,MetadataDiveMap)
      pbMessage(_INTL("不能在这里使用。")) if showmsg
      next false
    end
    if !PBTerrain.isDeepWater?($game_player.terrain_tag)
      pbMessage(_INTL("不能在这里使用。")) if showmsg
      next false
    end
  end
  next true
})

HiddenMoveHandlers::UseMove.add(:DIVE,proc { |move,pokemon|
  wasdiving = $PokemonGlobal.diving
  if $PokemonGlobal.diving
    divemap = nil
    meta = pbLoadMetadata
    for i in 0...meta.length
      if meta[i] && meta[i][MetadataDiveMap] && meta[i][MetadataDiveMap]==$game_map.map_id
        divemap = i; break
      end
    end
  else
    divemap = pbGetMetadata($game_map.map_id,MetadataDiveMap)
  end
  next false if !divemap
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
  end
  pbFadeOutIn {
    $game_temp.player_new_map_id    = divemap
    $game_temp.player_new_x         = $game_player.x
    $game_temp.player_new_y         = $game_player.y
    $game_temp.player_new_direction = $game_player.direction
    $PokemonGlobal.surfing = wasdiving
    $PokemonGlobal.diving  = !wasdiving
    pbUpdateVehicle
    $scene.transfer_player(false)
    $game_map.autoplay
    $game_map.refresh
  }
  next true
})

#===============================================================================
# Flash
#===============================================================================
HiddenMoveHandlers::CanUseMove.add(:FLASH,proc { |move,pkmn,showmsg|
  next false if !pbCheckHiddenMoveBadge(BADGE_FOR_FLASH,showmsg)
  if !pbGetMetadata($game_map.map_id,MetadataDarkMap)
    pbMessage(_INTL("不能在这里使用。")) if showmsg
    next false
  end
  if $PokemonGlobal.flashUsed
    pbMessage(_INTL("已经使用闪光了。")) if showmsg
    next false
  end
  next true
})

HiddenMoveHandlers::UseMove.add(:FLASH,proc { |move,pokemon|
  darkness = $PokemonTemp.darknessSprite
  next false if !darkness || darkness.disposed?
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
  end
  $PokemonGlobal.flashUsed = true
  radiusDiff = 8*Graphics.frame_rate/40
  while darkness.radius<darkness.radiusMax
    Graphics.update
    Input.update
    pbUpdateSceneMap
    darkness.radius += radiusDiff
    darkness.radius = darkness.radiusMax if darkness.radius>darkness.radiusMax
  end
  next true
})



#===============================================================================
# Fly
#===============================================================================
HiddenMoveHandlers::CanUseMove.add(:FLY,proc { |move,pkmn,showmsg|
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。")) if showmsg
    next false
  end
  if !pbGetMetadata($game_map.map_id,MetadataOutdoor)||
  ESMM_Config::BAN_MAPS.include?($game_map.map_id) 
    pbMessage(_INTL("不能在这里使用。")) if showmsg
    next false
  end
  next true
})

HiddenMoveHandlers::UseMove.add(:FLY,proc { |move,pokemon|
  if !$PokemonTemp.flydata||
  ESMM_Config::BAN_MAPS.include?($game_map.map_id) 
    pbMessage(_INTL("不能在这里使用。"))if showmsg
    next false
  end
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
  end
  pbFlyAnimation
  pbFadeOutIn {
    $game_temp.player_new_map_id    = $PokemonTemp.flydata[0]
    $game_temp.player_new_x         = $PokemonTemp.flydata[1]
    $game_temp.player_new_y         = $PokemonTemp.flydata[2]
    $game_temp.player_new_direction = 2
    $PokemonTemp.flydata = nil
    $scene.transfer_player
    $game_map.autoplay
    $game_map.refresh
  }
  pbFlyAnimation(false)
  pbEraseEscapePoint
  next true
})



#===============================================================================
# Headbutt
#===============================================================================
def pbHeadbuttEffect(event=nil)
  event = $game_player.pbFacingEvent(true) if !event
  a = (event.x+(event.x/24).floor+1)*(event.y+(event.y/24).floor+1)
  a = (a*2/5)%10   # Even 2x as likely as odd, 0 is 1.5x as likely as odd
  b = ($Trainer.publicID)%10   # Practically equal odds of each value
  chance = 1                             # ~50%
  if a==b;                  chance = 8   # 10%
  elsif a>b && (a-b).abs<5; chance = 5   # ~30.3%
  elsif a<b && (a-b).abs>5; chance = 5   # ~9.7%
  end
  if rand(10)>=chance
    pbMessage(_INTL("不。\n什么都没有……"))
  else
    enctype = (chance==1) ? EncounterTypes::HeadbuttLow : EncounterTypes::HeadbuttHigh
    if !pbEncounter(enctype)
      pbMessage(_INTL("不。\n什么都没有……"))
    end
  end
end

def pbHeadbutt(event=nil)
  move = getID(PBMoves,:HEADBUTT)
  movefinder = pbCheckMove(move)
  if !$DEBUG && !movefinder
    pbMessage(_INTL("或许有宝可梦在这棵树上。\n宝可梦也许可以晃动它。"))
    return false
  end
  if pbConfirmMessage(_INTL("或许有宝可梦在这棵树上。\n你想用头锤吗？"))
    speciesname = (movefinder) ? movefinder.name : $Trainer.name
    pbMessage(_INTL("{1}使用{2}！",speciesname,PBMoves.getName(move)))
    pbHiddenMoveAnimation(movefinder)
    pbHeadbuttEffect(event)
    return true
  end
  return false
end

HiddenMoveHandlers::CanUseMove.add(:HEADBUTT,proc { |move,pkmn,showmsg|
  facingEvent = $game_player.pbFacingEvent
  if !facingEvent || facingEvent.name.downcase!="headbutttree"
    pbMessage(_INTL("不能在这里使用。")) if showmsg
    next false
  end
  next true
})

HiddenMoveHandlers::UseMove.add(:HEADBUTT,proc { |move,pokemon|
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
  end
  facingEvent = $game_player.pbFacingEvent
  pbHeadbuttEffect(facingEvent)
})



#===============================================================================
# Rock Smash
#===============================================================================
def pbRockSmashRandomEncounter
  if rand(100)<25
    pbEncounter(EncounterTypes::RockSmash)
  end
end

def pbRockSmash
  move = getID(PBMoves,:ROCKSMASH)
  movefinder = pbCheckMove(move) || $PokemonBag.pbHasItem?(:HM07)
  if !pbCheckHiddenMoveBadge(BADGE_FOR_ROCKSMASH,false) || (!$DEBUG && !movefinder)
    pbMessage(_INTL("这是一块岩石，\n也许宝可梦能破坏它。"))
    return false
  end
  if pbConfirmMessage(_INTL("这是一块岩石，要使用岩石粉碎吗？"))
    speciesname = (movefinder.is_a?(PokeBattle_Pokemon)) ? movefinder.name : $Trainer.name
    pbMessage(_INTL("{1}使用了{2}！",speciesname,PBMoves.getName(move)))
    return true
  end
  return false
end

HiddenMoveHandlers::CanUseMove.add(:ROCKSMASH,proc { |move,pkmn,showmsg|
  next false if !pbCheckHiddenMoveBadge(BADGE_FOR_ROCKSMASH,showmsg)
  facingEvent = $game_player.pbFacingEvent
  if !facingEvent || facingEvent.name.downcase!="rock"
    pbMessage(_INTL("不能在这里使用。")) if showmsg
    next false
  end
  next true
})

HiddenMoveHandlers::UseMove.add(:ROCKSMASH,proc { |move,pokemon|
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
  end
  facingEvent = $game_player.pbFacingEvent
  if facingEvent
    pbSmashEvent(facingEvent)
    pbRockSmashRandomEncounter
  end
  next true
})



#===============================================================================
# Strength
#===============================================================================
def pbStrength
  if $PokemonMap.strengthUsed
    pbMessage(_INTL("现在可以移动石头了。"))
    return false
  end
  move = getID(PBMoves,:STRENGTH)
  movefinder = $PokemonBag.pbHasItem?(:HM04)
  if !pbCheckHiddenMoveBadge(BADGE_FOR_STRENGTH,false)
    pbMessage(_INTL("这是一块石头，\n但也许宝可梦能移动它。"))
    return false
  end
  pbMessage(_INTL("这是一块石头，\n但也许宝可梦能移动它。\1"))
  if pbConfirmMessage(_INTL("要使用怪力吗？"))
    speciesname = $Trainer.name
    pbMessage(_INTL("{1}使用{2}！",speciesname,PBMoves.getName(move)))
    pbMessage(_INTL("{1}的怪力\n现在可以移动石头了。",speciesname))
    $PokemonMap.strengthUsed = true
    return true
  end
  return false
end

Events.onAction += proc { |_sender,_e|
  facingEvent = $game_player.pbFacingEvent
  pbStrength if facingEvent && facingEvent.name.downcase=="boulder"
}

HiddenMoveHandlers::CanUseMove.add(:STRENGTH,proc { |move,pkmn,showmsg|
  next false if !pbCheckHiddenMoveBadge(BADGE_FOR_STRENGTH,showmsg)
  if $PokemonMap.strengthUsed
    pbMessage(_INTL("已经使用怪力了。")) if showmsg
    next false
  end
  next true
})

HiddenMoveHandlers::UseMove.add(:STRENGTH,proc { |move,pokemon|
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！\1",pokemon.name,PBMoves.getName(move)))
  end
  pbMessage(_INTL("{1}的怪力\n现在可以移动石头了。",pokemon.name))
  $PokemonMap.strengthUsed = true
  next true
})



#===============================================================================
# Surf
#===============================================================================
def pbSurf
  return false if $game_player.pbFacingEvent
  return false if $game_player.pbHasDependentEvents?
  move = getID(PBMoves,:SURF)
  movefinder = $PokemonBag.pbHasItem?(:HM03)
  if !pbCheckHiddenMoveBadge(BADGE_FOR_SURF,false)
    return false
  end
  if pbConfirmMessage(_INTL("这里的水蔚蓝蔚蓝的，\n你想要在这里冲浪吗？"))
    speciesname = $Trainer.name
    pbMessage(_INTL("{1}使用了{2}!",speciesname,PBMoves.getName(move)))
    pbCancelVehicles
    surfbgm = pbGetMetadata(0,MetadataSurfBGM)
    pbCueBGM(surfbgm,0.5) if surfbgm
    pbStartSurfing
    return true
  end
  return false
end


def pbStartSurfing
  pbCancelVehicles
  $PokemonEncounters.clearStepCount
  $PokemonGlobal.surfing = true
  pbUpdateVehicle
  $PokemonTemp.surfJump = $MapFactory.getFacingCoords($game_player.x,$game_player.y,$game_player.direction)
  pbJumpToward
  $PokemonTemp.surfJump = nil
  $game_player.check_event_trigger_here([1,2])
end

def pbEndSurf(_xOffset,_yOffset)
  return false if !$PokemonGlobal.surfing
  x = $game_player.x
  y = $game_player.y
  currentTag = $game_map.terrain_tag(x,y)
  facingTag = pbFacingTerrainTag
  if PBTerrain.isSurfable?(currentTag) && !PBTerrain.isSurfable?(facingTag)
    $PokemonTemp.surfJump = [x,y]
    if pbJumpToward(1,false,true)
      $game_map.autoplayAsCue
      $game_player.increase_steps
      result = $game_player.check_event_trigger_here([1,2])
      pbOnStepTaken(result)
    end
    $PokemonTemp.surfJump = nil
    return true
  end
  return false
end

def pbTransferSurfing(mapid,xcoord,ycoord,direction=$game_player.direction)
  pbFadeOutIn {
    $game_temp.player_new_map_id    = mapid
    $game_temp.player_new_x         = xcoord
    $game_temp.player_new_y         = ycoord
    $game_temp.player_new_direction = direction
    $scene.transfer_player(false)
    $game_map.autoplay
    $game_map.refresh
  }
end

Events.onAction += proc { |_sender,_e|
  next if $PokemonGlobal.surfing
  next if pbGetMetadata($game_map.map_id,MetadataBicycleAlways)
  next if !PBTerrain.isSurfable?(pbFacingTerrainTag)
  next if !$game_map.passable?($game_player.x,$game_player.y,$game_player.direction,$game_player)
  pbSurf
}

HiddenMoveHandlers::CanUseMove.add(:SURF,proc { |move,pkmn,showmsg|
  next false if !pbCheckHiddenMoveBadge(BADGE_FOR_SURF,showmsg)
  if $PokemonGlobal.surfing
    pbMessage(_INTL("已经使用冲浪了。")) if showmsg
    next false
  end
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。")) if showmsg
    next false
  end
  if pbGetMetadata($game_map.map_id,MetadataBicycleAlways)
    pbMessage(_INTL("享受骑车的乐趣吧！")) if showmsg
    next false
  end
  if !PBTerrain.isSurfable?(pbFacingTerrainTag) ||
     !$game_map.passable?($game_player.x,$game_player.y,$game_player.direction,$game_player)
    pbMessage(_INTL("这里不能冲浪！")) if showmsg
    next false
  end
  next true
})

HiddenMoveHandlers::UseMove.add(:SURF,proc { |move,pokemon|
  $game_temp.in_menu = false
  pbCancelVehicles
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
  end
  surfbgm = pbGetMetadata(0,MetadataSurfBGM)
  pbCueBGM(surfbgm,0.5) if surfbgm
  pbStartSurfing
  next true
})



#===============================================================================
# Sweet Scent
#===============================================================================
def pbSweetScent
  if $game_screen.weather_type!=PBFieldWeather::None
    pbMessage(_INTL("因为某些原因，香气散去了……"))
    return
  end
  viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
  viewport.z = 99999
  count = 0
  viewport.color.red   = 255
  viewport.color.green = 0
  viewport.color.blue  = 0
  viewport.color.alpha -= 10
  alphaDiff = 12 * Graphics.frame_rate / 40
  loop do
    if count==0 && viewport.color.alpha<128
      viewport.color.alpha += alphaDiff
    elsif count>40/4
      viewport.color.alpha -= alphaDiff
    else
      count += 1
    end
    Graphics.update
    Input.update
    pbUpdateSceneMap
    break if viewport.color.alpha<=0
  end
  viewport.dispose
  enctype = $PokemonEncounters.pbEncounterType
  if enctype<0 || !$PokemonEncounters.isEncounterPossibleHere? ||
     !pbEncounter(enctype)
    pbMessage(_INTL("这里好像什么都没有……"))
  end
end

HiddenMoveHandlers::CanUseMove.add(:SWEETSCENT,proc { |move,pkmn,showmsg|
  next true
})

HiddenMoveHandlers::UseMove.add(:SWEETSCENT,proc { |move,pokemon|
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
  end
  pbSweetScent
  next true
})



#===============================================================================
# Teleport
#===============================================================================
HiddenMoveHandlers::CanUseMove.add(:TELEPORT,proc { |move,pkmn,showmsg|
  if !pbGetMetadata($game_map.map_id,MetadataOutdoor)
    pbMessage(_INTL("不能在这里使用。")) if showmsg
    next false
  end
  healing = $PokemonGlobal.healingSpot
  healing = pbGetMetadata(0,MetadataHome) if !healing   # Home
  if !healing
    pbMessage(_INTL("不能在这里使用。")) if showmsg
    next false
  end
  if [60, 226, 207, 321, 322, 209, 210].include?($game_map.map_id)
    pbMessage(_INTL("这里不能使用。"))
    next false
  end
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。")) if showmsg
    next false
  end
  next true
})

HiddenMoveHandlers::ConfirmUseMove.add(:TELEPORT,proc { |move,pkmn|
  healing = $PokemonGlobal.healingSpot
  healing = pbGetMetadata(0,MetadataHome) if !healing   # Home
  next false if !healing
  mapname = pbGetMapNameFromId(healing[0])
  next pbConfirmMessage(_INTL("想要回到{1}吗？",mapname))
})

HiddenMoveHandlers::UseMove.add(:TELEPORT,proc { |move,pokemon|
  healing = $PokemonGlobal.healingSpot
  healing = pbGetMetadata(0,MetadataHome) if !healing   # Home
  next false if !healing
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
  end
  pbFadeOutIn {
    $game_temp.player_new_map_id    = healing[0]
    $game_temp.player_new_x         = healing[1]
    $game_temp.player_new_y         = healing[2]
    $game_temp.player_new_direction = 2
    $scene.transfer_player
    $game_map.autoplay
    $game_map.refresh
  }
  pbEraseEscapePoint
  next true
})



#===============================================================================
# Waterfall
#===============================================================================
def pbAscendWaterfall(event=nil)
  event = $game_player if !event
  return if !event
  return if event.direction!=8   # can't ascend if not facing up
  oldthrough   = event.through
  oldmovespeed = event.move_speed
  terrain = pbFacingTerrainTag
  return if !PBTerrain.isWaterfall?(terrain)
  event.through = true
  event.move_speed = 2
  loop do
    event.move_up
    terrain = pbGetTerrainTag(event)
    break if !PBTerrain.isWaterfall?(terrain)
  end
  event.through    = oldthrough
  event.move_speed = oldmovespeed
end

def pbDescendWaterfall(event=nil)
  event = $game_player if !event
  return if !event
  return if event.direction!=2   # Can't descend if not facing down
  oldthrough   = event.through
  oldmovespeed = event.move_speed
  terrain = pbFacingTerrainTag
  return if !PBTerrain.isWaterfall?(terrain)
  event.through = true
  event.move_speed = 2
  loop do
    event.move_down
    terrain = pbGetTerrainTag(event)
    break if !PBTerrain.isWaterfall?(terrain)
  end
  event.through    = oldthrough
  event.move_speed = oldmovespeed
end

def pbWaterfall
  move = getID(PBMoves,:WATERFALL)
  movefinder = pbCheckMove(move) || $PokemonBag.pbHasItem?(:HM05)
  if !pbCheckHiddenMoveBadge(BADGE_FOR_WATERFALL,false) || (!$DEBUG && !movefinder)
    pbMessage(_INTL("一道瀑布气势非凡地横挂在眼前。"))
    return false
  end
  if pbConfirmMessage(_INTL("这是一道巨大的瀑布，要使用攀瀑吗？"))
    speciesname = $Trainer.name
    pbMessage(_INTL("{1}使用了{2}!",speciesname,PBMoves.getName(move)))
    pbAscendWaterfall
    return true
  end
  return false
end


Events.onAction += proc { |_sender,_e|
  terrain = pbFacingTerrainTag
  if terrain==PBTerrain::Waterfall
    pbWaterfall
  elsif terrain==PBTerrain::WaterfallCrest
    pbMessage(_INTL("伴随着震耳欲聋的轰鸣声，\n庞大的瀑布倾泻而下。"))
  end
}

HiddenMoveHandlers::CanUseMove.add(:WATERFALL,proc { |move,pkmn,showmsg|
  next false if !pbCheckHiddenMoveBadge(BADGE_FOR_WATERFALL,showmsg)
  if pbFacingTerrainTag!=PBTerrain::Waterfall
    pbMessage(_INTL("不能在这里使用。")) if showmsg
    next false
  end
  next true
})

HiddenMoveHandlers::UseMove.add(:WATERFALL,proc { |move,pokemon|
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
  end
  pbAscendWaterfall
  next true
})

#===============================================================================
# Defog
#===============================================================================
def pbDefog
  $game_screen.weather(0,0,0)
end

HiddenMoveHandlers::CanUseMove.add(:DEFOG,proc { |move,pkmn,showmsg|
  next false if $game_screen.weather_type!=PBFieldWeather::Fog
  next true
})

HiddenMoveHandlers::UseMove.add(:DEFOG,proc { |move,pokemon|
  if !pbHiddenMoveAnimation(pokemon)
    pbMessage(_INTL("{1}使用{2}！",pokemon.name,PBMoves.getName(move)))
  end
  pbDefog
  next true
})

