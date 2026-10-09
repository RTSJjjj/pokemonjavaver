class PokeBattle_Pokemon
  attr_accessor :formTime     # Time when Furfrou's/Hoopa's form was set
  attr_accessor :forcedForm

  def form
    return @forcedForm if @forcedForm!=nil
    return (@form || 0) if $game_temp.in_battle
    v = MultipleForms.call("getForm",self)
    self.form = v if v!=nil && (!@form || v!=@form)
    return @form || 0
  end

  def form=(value)
    setForm(value)
  end

  def setForm(value)
    oldForm = @form
    @form = value
    yield if block_given?
    MultipleForms.call("onSetForm",self,value,oldForm)
    self.calcStats
    pbSeenForm(self)
  end

  def formSimple
    return @forcedForm if @forcedForm!=nil
    return @form || 0
  end

  def formSimple=(value)
    @form = value
    self.calcStats
  end

  def fSpecies
    return pbGetFSpeciesFromForm(@species,formSimple)
  end

  alias __mf_initialize initialize
  def initialize(*args)
    @form = (pbGetSpeciesFromFSpecies(args[0])[1] rescue 0)
    __mf_initialize(*args)
    if @form==0
      f = MultipleForms.call("getFormOnCreation",self)
      if f
        self.form = f
        self.resetMoves
      end
    end
  end
end



class PokeBattle_RealBattlePeer
  def pbOnEnteringBattle(_battle,pkmn,wild=false)
    f = MultipleForms.call("getFormOnEnteringBattle",pkmn,wild)
    pkmn.form = f if f
  end

  # For switching out, including due to fainting, and for the end of battle
  def pbOnLeavingBattle(battle,pkmn,usedInBattle,endBattle=false)
    return if !pkmn
    f = MultipleForms.call("getFormOnLeavingBattle",pkmn,battle,usedInBattle,endBattle)
    pkmn.form = f if f && pkmn.form!=f
    pkmn.hp = pkmn.totalhp if pkmn.hp>pkmn.totalhp
  end
end



module MultipleForms
  @@formSpecies = SpeciesHandlerHash.new

  def self.copy(sym,*syms)
    @@formSpecies.copy(sym,*syms)
  end

  def self.register(sym,hash)
    @@formSpecies.add(sym,hash)
  end

  def self.registerIf(cond,hash)
    @@formSpecies.addIf(cond,hash)
  end

  def self.hasFunction?(pkmn,func)
    spec = (pkmn.is_a?(Numeric)) ? pkmn : pkmn.species
    sp = @@formSpecies[spec]
    return sp && sp[func]
  end

  def self.getFunction(pkmn,func)
    spec = (pkmn.is_a?(Numeric)) ? pkmn : pkmn.species
    sp = @@formSpecies[spec]
    return (sp && sp[func]) ? sp[func] : nil
  end

  def self.call(func,pkmn,*args)
    sp = @@formSpecies[pkmn.species]
    return nil if !sp || !sp[func]
    return sp[func].call(pkmn,*args)
  end
end



def drawSpot(bitmap,spotpattern,x,y,red,green,blue)
  height = spotpattern.length
  width  = spotpattern[0].length
  for yy in 0...height
    spot = spotpattern[yy]
    for xx in 0...width
      if spot[xx]==1
        xOrg = (x+xx)<<1
        yOrg = (y+yy)<<1
        color = bitmap.get_pixel(xOrg,yOrg)
        r = color.red+red
        g = color.green+green
        b = color.blue+blue
        color.red   = [[r,0].max,255].min
        color.green = [[g,0].max,255].min
        color.blue  = [[b,0].max,255].min
        bitmap.set_pixel(xOrg,yOrg,color)
        bitmap.set_pixel(xOrg+1,yOrg,color)
        bitmap.set_pixel(xOrg,yOrg+1,color)
        bitmap.set_pixel(xOrg+1,yOrg+1,color)
      end
    end
  end
end

def pbSpindaSpots(pkmn,bitmap)
  spot1 = [
     [0,0,1,1,1,1,0,0],
     [0,1,1,1,1,1,1,0],
     [1,1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1,1],
     [0,1,1,1,1,1,1,0],
     [0,0,1,1,1,1,0,0]
  ]
  spot2 = [
     [0,0,1,1,1,0,0],
     [0,1,1,1,1,1,0],
     [1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1],
     [0,1,1,1,1,1,0],
     [0,0,1,1,1,0,0]
  ]
  spot3 = [
     [0,0,0,0,0,1,1,1,1,0,0,0,0],
     [0,0,0,1,1,1,1,1,1,1,0,0,0],
     [0,0,1,1,1,1,1,1,1,1,1,0,0],
     [0,1,1,1,1,1,1,1,1,1,1,1,0],
     [0,1,1,1,1,1,1,1,1,1,1,1,0],
     [1,1,1,1,1,1,1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1,1,1,1,1,1,1],
     [0,1,1,1,1,1,1,1,1,1,1,1,0],
     [0,1,1,1,1,1,1,1,1,1,1,1,0],
     [0,0,1,1,1,1,1,1,1,1,1,0,0],
     [0,0,0,1,1,1,1,1,1,1,0,0,0],
     [0,0,0,0,0,1,1,1,0,0,0,0,0]
  ]
  spot4 = [
     [0,0,0,0,1,1,1,0,0,0,0,0],
     [0,0,1,1,1,1,1,1,1,0,0,0],
     [0,1,1,1,1,1,1,1,1,1,0,0],
     [0,1,1,1,1,1,1,1,1,1,1,0],
     [1,1,1,1,1,1,1,1,1,1,1,0],
     [1,1,1,1,1,1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1,1,1,1,1,1],
     [1,1,1,1,1,1,1,1,1,1,1,0],
     [0,1,1,1,1,1,1,1,1,1,1,0],
     [0,0,1,1,1,1,1,1,1,1,0,0],
     [0,0,0,0,1,1,1,1,1,0,0,0]
  ]
  id = pkmn.personalID
  h = (id>>28)&15
  g = (id>>24)&15
  f = (id>>20)&15
  e = (id>>16)&15
  d = (id>>12)&15
  c = (id>>8)&15
  b = (id>>4)&15
  a = (id)&15
  if pkmn.shiny?
    drawSpot(bitmap,spot1,b+33,a+25,-75,-10,-150)
    drawSpot(bitmap,spot2,d+21,c+24,-75,-10,-150)
    drawSpot(bitmap,spot3,f+39,e+7,-75,-10,-150)
    drawSpot(bitmap,spot4,h+15,g+6,-75,-10,-150)
  else
    drawSpot(bitmap,spot1,b+33,a+25,0,-115,-75)
    drawSpot(bitmap,spot2,d+21,c+24,0,-115,-75)
    drawSpot(bitmap,spot3,f+39,e+7,0,-115,-75)
    drawSpot(bitmap,spot4,h+15,g+6,0,-115,-75)
  end
end

#===============================================================================
# Regular form differences
#===============================================================================

MultipleForms.register(:PIKACHU,{
  "onSetForm" => proc { |pkmn,form,oldForm|
    pkmn.makeFemale if [3..8].include?(form)
    formMoves = [
       :ICICLECRASH,      # Belle Pikachu
       :FLYINGPRESS,      # Libre Pikachu
       :ELECTRICTERRAIN,  # PhD Pikachu
       :DRAININGKISS,     # Pop Star Pikachu
       :METEORMASH        # Rockstar Pikachu
    ]
    idxMoveToReplace = -1
    pkmn.moves.each_with_index do |move,i|
      next if !move
      formMoves.each do |newMove|
        next if !isConst?(move.id,PBMoves,newMove)
        idxMoveToReplace = i
        break
      end
      break if idxMoveToReplace>=0
    end
    if !([3..8].include?(form))
      if idxMoveToReplace>=0
        moveName = PBMoves.getName(pkmn.moves[idxMoveToReplace].id)
        pkmn.pbDeleteMoveAtIndex(idxMoveToReplace)
        pbMessage(_INTL("{1} forgot {2}...",pkmn.name,moveName))
        pkmn.pbLearnMove(:THUNDERSHOCK) if pkmn.numMoves==0
      end
    else
      newMove = getConst(PBMoves,formMoves[form-4])
      if idxMoveToReplace>=0
        oldMoveName = PBMoves.getName(pkmn.moves[idxMoveToReplace].id)
        if newMove && newMove>0
          newMoveName = PBMoves.getName(newMove)
          pkmn.moves[idxMoveToReplace].id = newMove
          pbMessage(_INTL("1,\\wt[16] 2, and\\wt[16]...\\wt[16] ...\\wt[16] ... Ta-da!\\se[Battle ball drop]\1"))
          pbMessage(_INTL("{1} forgot how to use {2}.\\nAnd...\1",pkmn.name,oldMoveName))
          pbMessage(_INTL("\\se[]{1} learned {2}!\\se[Pkmn move learnt]",pkmn.name,newMoveName))
        else
          pkmn.pbDeleteMoveAtIndex(idxMoveToReplace)
          pbMessage(_INTL("{1} forgot {2}...",pkmn.name,oldMoveName))
          pkmn.pbLearnMove(:THUNDERWAVE) if pkmn.numMoves==0
        end
      elsif newMove && newMove>0
        pbLearnMove(pkmn,newMove,true)
      end
    end
},
"getForm" => proc { |pkmn|
  next if pkmn.formSimple>=2
  mapPos = pbGetMetadata($game_map.map_id,MetadataMapPosition)
  next 1 if mapPos && mapPos[0]==1   # Tiall region
  next 0
}})


MultipleForms.register(:UNOWN,{
  "getFormOnCreation" => proc { |pkmn|
    next rand(28)
  }
})

MultipleForms.register(:SPINDA,{
  "alterBitmap" => proc { |pkmn,bitmap|
    pbSpindaSpots(pkmn,bitmap)
  }
})

MultipleForms.register(:CASTFORM,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0
  }
})

MultipleForms.register(:BURMY,{
  "getFormOnCreation" => proc { |pkmn|
    case pbGetEnvironment
    when PBEnvironment::Rock, PBEnvironment::Sand, PBEnvironment::Cave
      next 1   # Sandy Cloak
    when PBEnvironment::None
      next 2   # Trash Cloak
    else
      next 0   # Plant Cloak
    end
  },
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next if !endBattle || !usedInBattle
    case battle.environment
    when PBEnvironment::Rock, PBEnvironment::Sand, PBEnvironment::Cave
      next 1   # Sandy Cloak
    when PBEnvironment::None
      next 2   # Trash Cloak
    else
      next 0   # Plant Cloak
    end
  }
})

MultipleForms.register(:WORMADAM,{
  "getFormOnCreation" => proc { |pkmn|
    case pbGetEnvironment
    when PBEnvironment::Rock, PBEnvironment::Sand, PBEnvironment::Cave
      next 1   # Sandy Cloak
    when PBEnvironment::None
      next 2   # Trash Cloak
    else
      next 0   # Plant Cloak
    end
  }
})

MultipleForms.register(:CHERRIM,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0
  }
})

MultipleForms.register(:ROTOM,{
  "onSetForm" => proc { |pkmn,form,oldForm|
    formMoves = [
       :OVERHEAT,    # Heat, Microwave
       :HYDROPUMP,   # Wash, Washing Machine
       :BLIZZARD,    # Frost, Refrigerator
       :AIRSLASH,    # Fan
       :LEAFSTORM    # Mow, Lawnmower
    ]
    idxMoveToReplace = -1
    pkmn.moves.each_with_index do |move,i|
      next if !move
      formMoves.each do |newMove|
        next if !isConst?(move.id,PBMoves,newMove)
        idxMoveToReplace = i
        break
      end
      break if idxMoveToReplace>=0
    end
    if form==0
      if idxMoveToReplace>=0
        moveName = PBMoves.getName(pkmn.moves[idxMoveToReplace].id)
        pkmn.pbDeleteMoveAtIndex(idxMoveToReplace)
        pbMessage(_INTL("{1} forgot {2}...",pkmn.name,moveName))
        pkmn.pbLearnMove(:THUNDERSHOCK) if pkmn.numMoves==0
      end
    else
      newMove = getConst(PBMoves,formMoves[form-1])
      if idxMoveToReplace>=0
        oldMoveName = PBMoves.getName(pkmn.moves[idxMoveToReplace].id)
        if newMove && newMove>0
          newMoveName = PBMoves.getName(newMove)
          pkmn.moves[idxMoveToReplace].id = newMove
          pbMessage(_INTL("1,\\wt[16] 2, and\\wt[16]...\\wt[16] ...\\wt[16] ... Ta-da!\\se[Battle ball drop]\1"))
          pbMessage(_INTL("{1} forgot how to use {2}.\\nAnd...\1",pkmn.name,oldMoveName))
          pbMessage(_INTL("\\se[]{1} learned {2}!\\se[Pkmn move learnt]",pkmn.name,newMoveName))
        else
          pkmn.pbDeleteMoveAtIndex(idxMoveToReplace)
          pbMessage(_INTL("{1} forgot {2}...",pkmn.name,oldMoveName))
          pkmn.pbLearnMove(:THUNDERSHOCK) if pkmn.numMoves==0
        end
      elsif newMove && newMove>0
        pbLearnMove(pkmn,newMove,true)
      end
    end
  }
})

MultipleForms.register(:GIRATINA,{
  "getForm" => proc { |pkmn|
    maps = [157,165]   # Map IDs for Origin Forme
    if pkmn.hasItem?(:GRISEOUSORB) || maps.include?($game_map.map_id)
      next 1
    end
    next 0
  }
})

MultipleForms.register(:SHAYMIN,{
  "getForm" => proc { |pkmn|
    next 0 if pkmn.fainted? || pkmn.status==PBStatuses::FROZEN ||
              PBDayNight.isNight?
  }
})

MultipleForms.register(:ARCEUS,{
  "getForm" => proc { |pkmn|
    next nil if !isConst?(pkmn.ability,PBAbilities,:MULTITYPE)
    typeArray = {
       1  => [:FISTPLATE],
       2  => [:SKYPLATE],
       3  => [:TOXICPLATE],
       4  => [:EARTHPLATE],
       5  => [:STONEPLATE],
       6  => [:INSECTPLATE],
       7  => [:SPOOKYPLATE],
       8  => [:IRONPLATE],
       10 => [:FLAMEPLATE],
       11 => [:SPLASHPLATE],
       12 => [:MEADOWPLATE],
       13 => [:ZAPPLATE],
       14 => [:MINDPLATE],
       15 => [:ICICLEPLATE],
       16 => [:DRACOPLATE],
       17 => [:DREADPLATE],
       18 => [:PIXIEPLATE],
       19 => [:LIGHTPLATE],
       20 => [:SHADOWPLATE],
       21 => [:LEGENDPLATE]
    }
    ret = 0
    typeArray.each do |f, items|
      for item in items
        next if !pkmn.hasItem?(item)
        ret = f
        break
      end
      break if ret>0
    end
    next ret
  },
  "getFormOnLeavingBattle" => proc { |pkmn, battle, usedInBattle, endBattle|
    next 0 if pkmn.hasItem?(:LEGENDPLATE)
  }
})

MultipleForms.register(:BASCULIN,{
  "getFormOnCreation" => proc { |pkmn|
    next rand(2)
  }
})

MultipleForms.register(:DARMANITAN,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0 if pkmn.form==2
    next 1 if pkmn.form==3
  }
})

MultipleForms.register(:DEERLING,{
  "getForm" => proc { |pkmn|
    next pbGetSeason
  }
})

MultipleForms.copy(:DEERLING,:SAWSBUCK)

MultipleForms.register(:KYUREM,{
  "getFormOnEnteringBattle" => proc { |pkmn,wild|
    next pkmn.form+2 if pkmn.form==1 || pkmn.form==2
  },
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next pkmn.form-2 if pkmn.form>=3   # Fused forms stop glowing
  },
  "onSetForm" => proc { |pkmn,form,oldForm|
    case form
    when 0   # Normal
      pkmn.moves.each do |move|
        next if !move
        #冰封世界
        if (isConst?(move.id,PBMoves,:ICEBURN) ||
            isConst?(move.id,PBMoves,:FREEZESHOCK)) && 
            hasConst?(PBMoves,:GLACIATE)
          move.id = getConst(PBMoves,:GLACIATE)
        end
        #鬼面
        if (isConst?(move.id,PBMoves,:FUSIONFLARE) ||
            isConst?(move.id,PBMoves,:FUSIONBOLT))&&
            hasConst?(PBMoves,:SCARYFACE)
          move.id = getConst(PBMoves,:SCARYFACE)
        end
      end
    when 1   # White
      pkmn.moves.each do |move|
        next if !move
        if isConst?(move.id,PBMoves,:GLACIATE) && hasConst?(PBMoves,:ICEBURN)
          move.id = getConst(PBMoves,:ICEBURN)
        end
        if isConst?(move.id,PBMoves,:SCARYFACE) && hasConst?(PBMoves,:FUSIONFLARE)
          move.id = getConst(PBMoves,:FUSIONFLARE)
        end
      end
    when 2   # Black
      pkmn.moves.each do |move|
        next if !move
        if isConst?(move.id,PBMoves,:GLACIATE) && hasConst?(PBMoves,:FREEZESHOCK)
          move.id = getConst(PBMoves,:FREEZESHOCK)
        end
        if isConst?(move.id,PBMoves,:SCARYFACE) && hasConst?(PBMoves,:FUSIONBOLT)
          move.id = getConst(PBMoves,:FUSIONBOLT)
        end
      end
    end
  }
})

MultipleForms.register(:KELDEO,{
  "getForm" => proc { |pkmn|
    next 1 if pkmn.hasMove?(:SECRETSWORD) # Resolute Form
    next 0                                # Ordinary Form
  }
})

MultipleForms.register(:MELOETTA,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0
  }
})

MultipleForms.register(:GENESECT,{
  "getForm" => proc { |pkmn|
    next 1 if pkmn.hasItem?(:SHOCKDRIVE)
    next 2 if pkmn.hasItem?(:BURNDRIVE)
    next 3 if pkmn.hasItem?(:CHILLDRIVE)
    next 4 if pkmn.hasItem?(:DOUSEDRIVE)
    next 0
  }
})

MultipleForms.register(:GRENINJA,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 1 if pkmn.form == 2 && (pkmn.fainted? || endBattle)
  }
})

MultipleForms.register(:SCATTERBUG,{
  "getFormOnCreation" => proc { |pkmn|
    next $Trainer.secretID%18
  }
})

MultipleForms.copy(:SCATTERBUG,:SPEWPA,:VIVILLON)

MultipleForms.register(:FLABEBE,{
  "getFormOnCreation" => proc { |pkmn|
    next rand(5)
  }
})

MultipleForms.copy(:FLABEBE,:FLOETTE,:FLORGES)


MultipleForms.register(:ESPURR,{
  "getFormOnCreation" => proc { |pkmn|
    next pkmn.gender
  },
  "getForm" => proc { |pkmn|
    next pkmn.gender
  }
})

MultipleForms.copy(:ESPURR,:MEOWSTIC,:INDEEDEE)

MultipleForms.register(:AEGISLASH,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0
  }
})



MultipleForms.register(:PUMPKABOO,{
  "getFormOnCreation" => proc { |pkmn|
    r = rand(100)
    if r<5;     next 3   # Super Size (5%)
    elsif r<20; next 2   # Large (15%)
    elsif r<65; next 1   # Average (45%)
    end
    next 0               # Small (35%)
  }
})

MultipleForms.copy(:PUMPKABOO,:GOURGEIST)

MultipleForms.register(:XERNEAS,{
  "getFormOnEnteringBattle" => proc { |pkmn,wild|
    next 1
  },
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0
  }
})

MultipleForms.register(:ZYGARDE,{
  # 禁止捕捉时自动变形
  "getFormOnCapture" => proc { |pkmn|
    next pkmn.form  # 保持原形态不变
  },
  
  # 如果存在其他转换逻辑，也一并覆盖
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    # 只对形态0-5生效，6/7/8不变化
    if pkmn.form <= 5
      next pkmn.form - 2 if pkmn.form >= 4 && (pkmn.fainted? || endBattle)
    end
    next pkmn.form
  },
  
  "onSetForm" => proc { |pkmn,form,oldForm|
    # 只对形态0-5生效
    if oldForm <= 5 && form <= 5
      if oldForm == 5 && form != 5
        pkmn.moves.each do |move|
          next if !move
          if isConst?(move.id,PBMoves,:NIHILLIGHT)
            move.id = getConst(PBMoves,:COREENFORCER)
          end
        end
      end
    end
  }
})

MultipleForms.register(:HOOPA,{
  "getForm" => proc { |pkmn|
    if !pkmn.formTime || pbGetTimeNow.to_i>pkmn.formTime.to_i+60*60*24*3   # 3 days
      next 0
    end
  },
  "onSetForm" => proc { |pkmn,form,oldForm|
    pkmn.formTime = (form>0) ? pbGetTimeNow.to_i : nil
  }
})

MultipleForms.register(:ORICORIO,{
  "getFormOnCreation" => proc { |pkmn|
    next rand(4)   # 0=red, 1=yellow, 2=pink, 3=purple
  },
})

MultipleForms.register(:ROCKRUFF,{
  "getForm" => proc { |pkmn|
    next if pkmn.formSimple>=2   # Own Tempo Rockruff cannot become another form
    next 1 if PBDayNight.isNight?
    next 0
  }
})

MultipleForms.register(:LYCANROC,{
  "getFormOnCreation" => proc { |pkmn|
    next 2 if PBDayNight.isEvening?   # Dusk
    next 1 if PBDayNight.isNight?     # Midnight
    next 0                            # Midday
  },
})

MultipleForms.register(:WISHIWASHI,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0
  }
})

MultipleForms.register(:DIALGA,{
  "getForm" => proc { |pkmn|
    next 1 if isConst?(pkmn.item,PBItems,:ADAMANTORB)
    next 0
  }
})

MultipleForms.register(:PALKIA,{
  "getForm" => proc { |pkmn|
    next 1 if isConst?(pkmn.item,PBItems,:LUSTROUSORB)
    next 0
  }
})

MultipleForms.register(:REGIGIGAS,{
  "getForm" => proc { |pkmn|
    next 1 if isConst?(pkmn.item,PBItems,:REGISPELL)
    next 0
  }
})

MultipleForms.register(:WISHIWASHI,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0
  }
})

MultipleForms.register(:SILVALLY,{
  "getForm" => proc { |pkmn|
    next nil if !isConst?(pkmn.ability,PBAbilities,:RKSSYSTEM)
    typeArray = {
       1  => [:FIGHTINGMEMORY],
       2  => [:FLYINGMEMORY],
       3  => [:POISONMEMORY],
       4  => [:GROUNDMEMORY],
       5  => [:ROCKMEMORY],
       6  => [:BUGMEMORY],
       7  => [:GHOSTMEMORY],
       8  => [:STEELMEMORY],
       10 => [:FIREMEMORY],
       11 => [:WATERMEMORY],
       12 => [:GRASSMEMORY],
       13 => [:ELECTRICMEMORY],
       14 => [:PSYCHICMEMORY],
       15 => [:ICEMEMORY],
       16 => [:DRAGONMEMORY],
       17 => [:DARKMEMORY],
       18 => [:FAIRYMEMORY]
    }
    ret = 0
    typeArray.each do |f, items|
      for item in items
        next if !pkmn.hasItem?(item)
        ret = f
        break
      end
      break if ret>0
    end
    next ret
  }
})

MultipleForms.register(:MINIOR,{
  "getFormOnCreation" => proc { |pkmn|
    next 7+rand(7)   # Meteor forms are 0-6, Core forms are 7-13
  },
  "getFormOnEnteringBattle" => proc { |pkmn,wild|
    next pkmn.form-7 if pkmn.form>=7 && wild   # Wild Minior always appear in Meteor form
  },
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next pkmn.form+7 if pkmn.form<7
  }
})

MultipleForms.register(:MIMIKYU,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0 if pkmn.fainted? || endBattle
  }
})

MultipleForms.register(:NECROZMA,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    # Fused forms are 1 and 2, Ultra form is 3 or 4 depending on which fusion
    next pkmn.form-2 if pkmn.form>=3 && (pkmn.fainted? || endBattle)
  },
  "onSetForm" => proc { |pkmn,form,oldForm|
    next if form>2 || oldForm>2   # Ultra form changes don't affect moveset
    formMoves = [
       :SUNSTEELSTRIKE,   # Dusk Mane (with Solgaleo) (form 1)
       :MOONGEISTBEAM     # Dawn Wings (with Lunala) (form 2)
    ]
    if form==0
      idxMoveToReplace = -1
      pkmn.moves.each_with_index do |move,i|
        next if !move
        formMoves.each do |newMove|
          next if !isConst?(move.id,PBMoves,newMove)
          idxMoveToReplace = i
          break
        end
        break if idxMoveToReplace>=0
      end
      if idxMoveToReplace>=0
        moveName = PBMoves.getName(pkmn.moves[idxMoveToReplace].id)
        pkmn.pbDeleteMoveAtIndex(idxMoveToReplace)
        pbMessage(_INTL("{1} forgot {2}...",pkmn.name,moveName))
        pkmn.pbLearnMove(:CONFUSION) if pkmn.numMoves==0
      end
    else
      newMove = getConst(PBMoves,formMoves[form-1])
      if newMove && newMove>0
        pbLearnMove(pkmn,newMove,true)
      end
    end
  }
})

MultipleForms.register(:TOXEL,{
  "getFormOnCreation" => proc { |pkmn|
   natures=[1,5,7,10,12,15,16,17,18,20,21,23]
   lowkey=false
   nature=pkmn.nature
   lowkey = true if natures.include?(nature)
   next 1 if lowkey
   next 0
  },
})

MultipleForms.copy(:TOXEL,:TOXTRICITY)

MultipleForms.register(:EISCUE,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0 if pkmn.fainted? || endBattle
  }
})

MultipleForms.register(:INDEEDEE,{
  "getForm" => proc { |pkmn|
    next pkmn.gender
  }
})

MultipleForms.register(:ZAMAZENTA,{
  "getForm" => proc { |pkmn|
    next 1 if isConst?(pkmn.item,PBItems,:RUSTEDSHIELD)
    next 0
  }
})

MultipleForms.register(:ZACIAN,{
  "getForm" => proc { |pkmn|
    next 1 if isConst?(pkmn.item,PBItems,:RUSTEDSWORD)
    next 0
  }
})


MultipleForms.register(:CHALLEN,{
  "getForm" => proc { |pkmn|
    current_form = pkmn.instance_variable_get(:@form)
    if current_form == 3
      next 3
    end
    if pkmn.hasItem?(:CHARLENEITE)
      next 1
    elsif pkmn.hasItem?(:ZEROSUMHEART)
      next 2
    else
      next 0
    end
  }
})
  
MultipleForms.register(:MORPEKO,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0 if pkmn.fainted? || endBattle
  }
})

MultipleForms.register(:CRAMORANT,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0
  }
})


MultipleForms.register(:VALKYRIE,{
  "getForm" => proc { |pkmn|
    next 1 if isConst?(pkmn.item,PBItems,:BRIGHTSTONE)
    next 0
  }
})

MultipleForms.register(:BLACKKNIGHT,{
  "getForm" => proc { |pkmn|
    next 1 if isConst?(pkmn.item,PBItems,:DAWNISTONE)
    next 0
  }
})

MultipleForms.register(:SOLGALEO,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0
  }
})

MultipleForms.register(:ETERNATUS,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0
  }
})


MultipleForms.register(:MEWTWO,{
  "getForm" => proc { |pkmn|
    if pkmn.hasItem?(:BINDARMOR)
      next 1
    elsif pkmn.hasItem?(:RARMOR)
      next 2
    end
    next 0
  },
  "onSetForm" => proc { |pkmn,form,oldForm|
    case form
    # 普通形态
    when 0
      pkmn.moves.each do |move|
        next if !move
        if isConst?(move.id,PBMoves,:ARMORRELEASE) || 
           isConst?(move.id,PBMoves,:PSYCHOBLAST)
          move.id = getConst(PBMoves,:PSYSTRIKE)
        end
      end
    # 拘束解除形态 (拘束装甲)
    when 1
      pkmn.moves.each do |move|
        next if !move
        if isConst?(move.id,PBMoves,:PSYSTRIKE) || 
           isConst?(move.id,PBMoves,:PSYCHOBLAST)
          move.id = getConst(PBMoves,:ARMORRELEASE)
        end
      end
    # 心灵破坏炮形态 (R装甲)
    when 2
      pkmn.moves.each do |move|
        next if !move
        if isConst?(move.id,PBMoves,:PSYSTRIKE) || 
           isConst?(move.id,PBMoves,:ARMORRELEASE)
          move.id = getConst(PBMoves,:PSYCHOBLAST)
        end
      end
    end
    
    fSpecies = pbGetFSpeciesFromForm(pkmn.species,form)
    formname = pbGetMessage(MessageTypes::FormNames,fSpecies)
    if form > 0 && formname
      pkmn.name = formname
    else
      pkmn.name = pkmn.speciesName
    end
  }
})

MultipleForms.register(:ROSEDRAGON,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0 if pkmn.fainted? || endBattle
  }
})


MultipleForms.register(:SUGARDEVOIR, {
  "getForm" => proc { |pkmn|
    current_form = pkmn.instance_variable_get(:@form)
    
    if current_form == 6
      next 6
    elsif pkmn.hasItem?(:RADIANTSHARD)
      next 5
    elsif pkmn.hasItem?(:SANCTFEATHER)
      next 4
    elsif pkmn.hasItem?(:SQHL)
      next 3
    elsif pkmn.hasItem?(:WMDCX)
      next 2
    elsif pkmn.hasItem?(:TEMPLESCEPTER)
      next 1
    else
      next 0
    end
  },
  "onSetForm" => proc { |pkmn, form, oldForm|
    if form != oldForm
      case form
      when 0
        pkmn.moves.each do |move|
          next if !move
          if isConst?(move.id, PBMoves, :TEMPLESHINE) || 
             isConst?(move.id, PBMoves, :EVOBEAM) ||
             isConst?(move.id, PBMoves, :TIDEARIA) ||
             isConst?(move.id, PBMoves, :RADIANTVERDICT) ||
             isConst?(move.id, PBMoves, :TRUTHCRASH)
            move.id = getConst(PBMoves, :SUTACKLE)
          end
        end
        pkmn.name = "素纱奈"
      when 1
        pkmn.moves.each do |move|
          next if !move
          if isConst?(move.id, PBMoves, :SUTACKLE) || 
             isConst?(move.id, PBMoves, :EVOBEAM) ||
             isConst?(move.id, PBMoves, :TIDEARIA) ||
             isConst?(move.id, PBMoves, :RADIANTVERDICT) ||
             isConst?(move.id, PBMoves, :TRUTHCRASH)
            move.id = getConst(PBMoves, :TEMPLESHINE)
          end
        end
        pkmn.name = "圣殿女王"
      when 2
        pkmn.moves.each do |move|
          next if !move
          if isConst?(move.id, PBMoves, :SUTACKLE) || 
             isConst?(move.id, PBMoves, :TEMPLESHINE) ||
             isConst?(move.id, PBMoves, :TIDEARIA) ||
             isConst?(move.id, PBMoves, :RADIANTVERDICT) ||
             isConst?(move.id, PBMoves, :TRUTHCRASH)
            move.id = getConst(PBMoves, :EVOBEAM)
          end
        end
        pkmn.name = "文明续约"
      when 3
        pkmn.moves.each do |move|
          next if !move
          if isConst?(move.id, PBMoves, :SUTACKLE) || 
             isConst?(move.id, PBMoves, :TEMPLESHINE) ||
             isConst?(move.id, PBMoves, :EVOBEAM) ||
             isConst?(move.id, PBMoves, :RADIANTVERDICT) ||
             isConst?(move.id, PBMoves, :TRUTHCRASH)
            move.id = getConst(PBMoves, :TIDEARIA)
          end
        end
        pkmn.name = "潮心公主"
      when 4
        pkmn.moves.each do |move|
          next if !move
          if isConst?(move.id, PBMoves, :SUTACKLE) || 
             isConst?(move.id, PBMoves, :TEMPLESHINE) ||
             isConst?(move.id, PBMoves, :EVOBEAM) ||
             isConst?(move.id, PBMoves, :TIDEARIA) ||
             isConst?(move.id, PBMoves, :TRUTHCRASH)
            move.id = getConst(PBMoves, :RADIANTVERDICT)
          end
        end
        pkmn.name = "圣洁之翼"
      when 5
        pkmn.moves.each do |move|
          next if !move
          if isConst?(move.id, PBMoves, :SUTACKLE) || 
             isConst?(move.id, PBMoves, :TEMPLESHINE) ||
             isConst?(move.id, PBMoves, :EVOBEAM) ||
             isConst?(move.id, PBMoves, :TIDEARIA) ||
             isConst?(move.id, PBMoves, :RADIANTVERDICT)
            move.id = getConst(PBMoves, :TRUTHCRASH)
          end
        end
        pkmn.name = "真理之核"
      when 6
        pkmn.name = "厉鬼之凄厉"
      end
    end
  }
})

MultipleForms.register(:SUJINRAKU, {
  "getForm" => proc { |pkmn|
    form = pkmn.instance_variable_get(:@form)
    if form == 2
      next 2
    elsif pkmn.hasItem?(:ABYSSSWORD)
      next 1
    else
      next 0
    end
  },
  "onSetForm" => proc { |pkmn, form, oldForm|
    # 只在形态真正改变时设置名称
    if form != oldForm
      case form
      when 1
        pkmn.name = "暗渊守卫"
      when 2
        pkmn.name = "冥焰之战骑"
      else
        pkmn.name = "素艾路"
      end
    end
  }
})

MultipleForms.register(:SIRFETCHD,{
  "getForm" => proc { |pkmn|
    next 2 if isConst?(pkmn.item,PBItems,:HOLYCREST)
    next 0
  }
})

MultipleForms.register(:CALYREX,{
  "onSetForm" => proc { |pkmn,form,oldForm|
    case form
    when 0   # Normal
      exclusiveMoves = [:TACKLE, :TAILWHIP, :DOUBLEKICK, :AVALANCHE, :HEX, :STOMP, :TORMENT, :CONFUSERAY,
         :MIST, :HAZE, :ICICLECRASH, :SHADOWBALL, :TAKEDOWN, :IRONDEFENSE, :AGILITY, :THRASH, :TAUNT, :DISABLE,
          :DOUBLEEDGE, :SWORDSDANCE, :NASTYPLOT, :GLACIALLANCE, :ASTRALBARRAGE].map! { |name| getID(PBMoves, name) }
      pkmn.moves.each_with_index do |move,i|
        next if !move || move.id==0
        if exclusiveMoves.include?(move.id)
          pbMessage(_INTL("{1} forgot {2}...",pkmn.name,PBMoves.getName(move.id)))
          pkmn.pbDeleteMoveAtIndex(i)
        end
      end
      pkmn.pbLearnMove(:CONFUSION) if pkmn.numMoves==0
    when 1   # Ice Rider
      pbLearnMove(pkmn,getID(PBMoves,:GLACIALLANCE),true) if hasConst?(PBMoves,:GLACIALLANCE)
    when 2   # Black
      pbLearnMove(pkmn,getID(PBMoves,:ASTRALBARRAGE),true) if hasConst?(PBMoves,:ASTRALBARRAGE)
    end
  }
})

#===============================================================================
# Alolan forms
#===============================================================================

# These species don't have visually different Alolan forms, but they need to
# evolve into different forms depending on the location where they evolved.
MultipleForms.register(:EXEGGCUTE,{
  "getForm" => proc { |pkmn|
    next if pkmn.formSimple>=2
    mapPos = pbGetMetadata($game_map.map_id,MetadataMapPosition)
    next 1 if mapPos && mapPos[0]==1   # Tiall region
    next 0
  }
})

MultipleForms.copy(:EXEGGCUTE,:CUBONE)

#===============================================================================
# Galarian forms
#===============================================================================

# These species don't have visually different Galarian forms, but they need to
# evolve into different forms depending on the location where they evolved.
MultipleForms.register(:KOFFING,{
  "getForm" => proc { |pkmn|
    next if pkmn.formSimple>=2
    mapPos = pbGetMetadata($game_map.map_id,MetadataMapPosition)
    next 1 if mapPos && mapPos[0]==1   # Tiall region
    next 0
  }
})

MultipleForms.copy(:KOFFING,:MIMEJR)



MultipleForms.register(:RAPIDASH,{
  "onSetForm" => proc { |pkmn,form,oldForm|
    next if form != 0
    pkmn.moves.each do |move|
      next if !move
      if (isConst?(move.id,PBMoves,:FLAMEEXPLOSION))
        move.id = getConst(PBMoves,:FLAREBLITZ)
      end
    end
  }
})


MultipleForms.register(:KABLIT,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0 if pkmn.fainted? || endBattle
  }
})
MultipleForms.register(:FLAMBLOOM,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0 if pkmn.fainted? || endBattle
  }
})
MultipleForms.register(:BLAZEPHEX,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0 if pkmn.fainted? || endBattle
  }
})
MultipleForms.register(:SOLGALEO,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0 if pkmn.fainted? || endBattle
  }
})
MultipleForms.register(:LUNALA,{
  "getFormOnLeavingBattle" => proc { |pkmn,battle,usedInBattle,endBattle|
    next 0 if pkmn.fainted? || endBattle
  }
})

#===============================================================================
# 帕底亚地区 形态
#===============================================================================
MultipleForms.register(:TAUROS,{
  "getFormOnCreation" => proc { |pkmn|
    ret = 0 if pbGetCurrentRegion > 0
    ret = rand(4)
    next ret
  }
})

MultipleForms.register(:WOOPER,{
  "getFormOnCreation" => proc { |pkmn|
    ret = 0 if pbGetCurrentRegion > 0
    ret = rand(2)
    next ret
  }
})

MultipleForms.register(:LECHONK, {
  "getForm" => proc { |pkmn|
    next pkmn.gender
  },
  "getFormOnCreation" => proc { |pkmn|
    next pkmn.gender
  }
})

MultipleForms.copy(:LECHONK, :OINKOLOGNE)

MultipleForms.register(:TANDEMAUS, {
  "getFormOnCreation" => proc { |pkmn|
    next (pkmn.personalID % 100 == 0) ? 1 : 0
  }
})
MultipleForms.copy(:TANDEMAUS, :MAUSHOLD)

MultipleForms.register(:SQUAWKABILLY, {
  "getFormOnCreation" => proc { |pkmn|
    next rand(4)
  }
})

MultipleForms.register(:PALAFIN, {
  "getFormOnLeavingBattle" => proc { |pkmn, battle, usedInBattle, endBattle|
    next 0 if endBattle
  }
})

MultipleForms.register(:TATSUGIRI, {
  "getFormOnCreation" => proc { |pkmn|
    next rand(3)
  }
})

MultipleForms.register(:DUNSPARCE, {
  "getFormOnCreation" => proc { |pkmn|
    next (pkmn.personalID % 100 == 0) ? 1 : 0
  }
})
MultipleForms.copy(:DUNSPARCE, :DUDUNSPARCE)

MultipleForms.register(:GIMMIGHOUL, {
  "getFormOnCreation" => proc { |pkmn|
    next (pkmn.personalID % 1000 == 0) ? 0 : 1
  }
})

MultipleForms.register(:POLTCHAGEIST, {
  "getFormOnCreation" => proc { |pkmn|
    next 1 if rand(100) < 10   # Antique
    next 0                     # Phony
  }
})

MultipleForms.copy(:POLTCHAGEIST, :SINISTCHA)

MultipleForms.register(:TERAPAGOS, {
  "getFormOnLeavingBattle" => proc { |pkmn, battle, usedInBattle, endBattle|
    next 0 if pkmn.form > 0 && endBattle
  }
})

 
#厄诡椪
MultipleForms.register(:OGERPON,{
  "getForm" => proc { |pkmn|
    if pkmn.hasItem?(:WELLSPRINGMASK)
      next 1
    elsif pkmn.hasItem?(:HEARTHFLAMEMASK)
      next 2
    elsif pkmn.hasItem?(:CORNERSTONEMASK)
      next 3
    end
    next 0
  },
  "getFormOnEnteringBattle" => proc { |pkmn,wild|
    if pkmn.form < 4
      next pkmn.form + 4
    end
    next pkmn.form
  },
  "getFormOnLeavingBattle" => proc { |pkmn, battle, usedInBattle, endBattle|
    if pkmn.form >= 4
      next pkmn.form - 4
    end
    next pkmn.form
  }
})


MultipleForms.register(:SHINYGARCHOMP, {
  "getFormOnLeavingBattle" => proc { |pkmn, battle, usedInBattle, endBattle|
    next 0 if endBattle
  }
})