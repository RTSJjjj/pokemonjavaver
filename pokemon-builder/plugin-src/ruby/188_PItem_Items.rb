#===============================================================================
# Item data
#===============================================================================
ITEM_ID          = 0
ITEM_NAME        = 1
ITEM_PLURAL      = 2
ITEM_POCKET      = 3
ITEM_PRICE       = 4
ITEM_DESCRIPTION = 5
ITEM_FIELD_USE   = 6
ITEM_BATTLE_USE  = 7
ITEM_TYPE        = 8
ITEM_MACHINE     = 9



class PokemonTemp
  attr_accessor :itemsData
end



def pbLoadItemsData
  $PokemonTemp = PokemonTemp.new if !$PokemonTemp
  if !$PokemonTemp.itemsData
    $PokemonTemp.itemsData = load_data("Data/items.dat") || []
  end
  return $PokemonTemp.itemsData
end

def pbGetItemData(item,itemDataType)
  item = getID(PBItems,item)
  itemsData = pbLoadItemsData
  return itemsData[item][itemDataType] if itemsData[item]
  return nil
end

alias __itemsData__pbClearData pbClearData
def pbClearData
  $PokemonTemp.itemsData = nil if $PokemonTemp
  __itemsData__pbClearData
end

def pbGetPocket(item)
  ret = pbGetItemData(item,ITEM_POCKET)
  return ret || 0
end

def pbGetPrice(item)
  ret = pbGetItemData(item,ITEM_PRICE)
  return ret || 0
end

def pbGetMachine(item)
  ret = pbGetItemData(item,ITEM_MACHINE)
  return ret || 0
end

def pbIsTechnicalMachine?(item)
  ret = pbGetItemData(item,ITEM_FIELD_USE)
  return ret && ret==3
end

def pbIsHiddenMachine?(item)
  ret = pbGetItemData(item,ITEM_FIELD_USE)
  return ret && ret==4
end

def pbIsMachine?(item)
  ret = pbGetItemData(item,ITEM_FIELD_USE)
  return ret && (ret==3 || ret==4 || ret==6)
end

def pbIsTechnicalRecord?(item)
  ret = pbGetItemData(item,ITEM_FIELD_USE)
  return ret && ret==6
end

def pbIsMail?(item)
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && (ret==1 || ret==2)
end

def pbIsMailWithPokemonIcons?(item)
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && ret==2
end

def pbIsPokeBall?(item)
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && (ret==3 || ret==4)
end

def pbIsSnagBall?(item)
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && (ret==3 || (ret==4 && $PokemonGlobal.snagMachine))
end

def pbIsBerry?(item)
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && ret==5
end

def pbIsKeyItem?(item)
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && ret==6
end

def pbIsEvolutionStone?(item)
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && ret==7
end

def pbIsFossil?(item)
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && ret==8
end

def pbIsApricorn?(item)
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && ret==9
end

def pbIsGem?(item)
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && ret==10
end

def pbIsMulch?(item)
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && ret==11
end

def pbIsMegaStone?(item)   # Does NOT include Red Orb/Blue Orb
  ret = pbGetItemData(item,ITEM_TYPE)
  return ret && ret==12
end

# Important items can't be sold, given to hold, or tossed.
def pbIsImportantItem?(item)
  itemData = pbLoadItemsData[getID(PBItems,item)]
  return false if !itemData
  return true if itemData[ITEM_TYPE] && itemData[ITEM_TYPE]==6   # Key item
  return true if itemData[ITEM_FIELD_USE] && itemData[ITEM_FIELD_USE]==4   # HM
  return true if itemData[ITEM_FIELD_USE] && itemData[ITEM_FIELD_USE]==3 && INFINITE_TMS   # TM
  return false
end

def pbCanHoldItem?(item)
  return !pbIsImportantItem?(item)
end

def pbCanRegisterItem?(item)
  return ItemHandlers.hasUseInFieldHandler(item)
end

def pbCanUseOnPokemon?(item)
  return ItemHandlers.hasUseOnPokemon(item) || pbIsMachine?(item)
end

def pbIsHiddenMove?(move)
  itemsData = pbLoadItemsData
  return false if !itemsData
  for i in 0...itemsData.length
    next if !pbIsHiddenMachine?(i)
    atk = pbGetMachine(i)
    return true if move==atk
  end
  return false
end
#拍落
def pbIsUnlosableItem?(item,species,ability)
  return false if isConst?(species,PBSpecies,:ARCEUS) &&
                  !isConst?(ability,PBAbilities,:MULTITYPE)
  return false if isConst?(species,PBSpecies,:SILVALLY) &&
                  !isConst?(ability,PBAbilities,:RKSSYSTEM)
  combos = {
    #mega石
    :VENUSAUR     => [:VENUSAURITE, :VENUSAURITEG],
    :CHARIZARD    => [:CHARIZARDITEX, :CHARIZARDITEY, :CHARIZARDITEG],
    :BLASTOISE    => [:BLASTOISINITE, :BLASTOISINITEG],
    :BEEDRILL     => [:BEEDRILLITE],
    :PIDGEOT      => [:PIDGEOTITE],
    :ALAKAZAM     => [:ALAKAZITE],
    :SLOWBRO      => [:SLOWBRONITE],
    :GENGAR       => [:GENGARITE, :GENGARITEG],
    :KANGASKHAN   => [:KANGASKHANITE],
    :PINSIR       => [:PINSIRITE],
    :GYARADOS     => [:GYARADOSITE],
    :AERODACTYL   => [:AERODACTYLITE],
    :MEWTWO       => [:MEWTWONITEX, :MEWTWONITEY],
    :AMPHAROS     => [:AMPHAROSITE],
    :STEELIX      => [:STEELIXITE],
    :SCIZOR       => [:SCIZORITE],
    :HERACROSS    => [:HERACRONITE],
    :HOUNDOOM     => [:HOUNDOOMINITE],
    :TYRANITAR    => [:TYRANITARITE],
    :SCEPTILE     => [:SCEPTILITE],
    :BLAZIKEN     => [:BLAZIKENITE, :BLAZIKENITER],
    :SWAMPERT     => [:SWAMPERTITE],
    :GARDEVOIR    => [:GARDEVOIRITE],
    :SABLEYE      => [:SABLENITE],
    :MAWILE       => [:MAWILITE],
    :AGGRON       => [:AGGRONITE],
    :MEDICHAM     => [:MEDICHAMITE],
    :MANECTRIC    => [:MANECTITE],
    :SHARPEDO     => [:SHARPEDONITE],
    :CAMERUPT     => [:CAMERUPTITE],
    :ALTARIA      => [:ALTARIANITE],
    :BANETTE      => [:BANETTITE],
    :ABSOL        => [:ABSOLITE, :ABSOLITEZ],
    :GLALIE       => [:GLALITITE],
    :SALAMENCE    => [:SALAMENCITE],
    :METAGROSS    => [:METAGROSSITE],
    :LATIAS       => [:LATIASITE],
    :LATIOS       => [:LATIOSITE],
    :LOPUNNY      => [:LOPUNNITE],
    :GARCHOMP     => [:GARCHOMPITE, :GARCHOMPITEZ],
    :LUCARIO      => [:LUCARIONITE, :LUCARIONITEZ],
    :ABOMASNOW    => [:ABOMASITE],
    :GALLADE      => [:GALLADITE],
    :AUDINO       => [:AUDINITE],
    :DIANCIE      => [:DIANCITE],
    :BUTTERFREE   => [:BUTTERFREEITE, :BUTTERFREEITEG],
    :GOODRA       => [:GOODRAITE],
    :MILOTIC      => [:MILOTICITE],
    :GOTHITELLE   => [:GOTHITELLEITE],
    :TORTERRA     => [:TORTERRAITE],
    :INFERNAPE    => [:INFERNAPEITE],
    :EMPOLEON     => [:EMPOLEONITE],
    :LILLIGANT    => [:LILLIGANTITE],
    :PORYGONZ     => [:PORYGONPATCH],
    :ESCAVALIER   => [:ESCAVALIERITE],
    :ACCELGOR     => [:ACCELGORITE],
    :RAPIDASH     => [:RAPIDASHITE],
    :TOXICROAK    => [:TOXICROAKITE],
    :KINGDRA      => [:KINGDRAITE],
    :CHANDELURE   => [:CHANDELUREITER, :CHANDELURITE],
    :WEAVILE      => [:WEAVILEITE],
    :MEGANIUM     => [:MEGANIUMITER, :MEGANIUMITE],
    :PRIMARINA    => [:PRIMARINAITER],
    :FLYGON       => [:FLYGONITE],
    :TYPHLOSION   => [:TYPHLOSIONITE, :HISUITYPHLOSIONITE],
    :DARKMEWTWO   => [:DARKMEWTWONITE],
    :SPIRITHITE   => [:SPIRITHITITE],
    :CHANTEFLEUR  => [:CHANTEFLEURITE],
    :DRAGONITE    => [:DRAGONITEITE],
    :VICTREEBEL   => [:VICTREEBELITE],
    :MALAMAR      => [:MALAMARITE],
    :HAWLUCHA     => [:HAWLUCHAITE],
    :STARMIE      => [:STARMIEITE],
    :CLEFABLE     => [:CLEFABLEITE],
    :FERALIGATR   => [:FERALIGATRITE],
    :SKARMORY     => [:SKARMORYITE],
    :FROSLASS     => [:FROSLASSITE],
    :EMBOAR       => [:EMBOARITE],
    :EXCADRILL    => [:EXCADRILLITE],
    :SCOLIPEDE    => [:SCOLIPEDEITE],
    :SCRAFTY      => [:SCRAFTYITE],
    :EELEKTROSS   => [:EELEKTROSSITE],
    :CHESNAUGHT   => [:CHESNAUGHTITE],
    :DELPHOX      => [:DELPHOXITE],
    :GRENINJA     => [:GRENINJITE],
    :PYROAR       => [:PYROARITE],
    :FLORGES      => [:FLORGESITE],
    :BARBARACLE   => [:BARBARACLEITE],
    :DRAGALGE     => [:DRAGALGEITE],
    :DRAMPA       => [:DRAMPAITE],
    :FALINKS      => [:FALINKSITE],
    :RAICHU       => [:RAICHUNITEX, :RAICHUNITEY],
    :CHIMECHO     => [:CHIMECHONITE],
    :STARAPTOR    => [:STARAPTORITE],
    :HEATRAN      => [:HEATRANITE],
    :DARKRAI      => [:DARKRAINITE],
    :MAGEARNA     => [:MAGEARNAITE],
    :ZERAORA      => [:ZERAORANITE],
    :SCOVILLAIN   => [:SCOVILLAINITE],
    :GLIMMORA     => [:GLIMMORANITE],
    :MEOWSTIC     => [:MEOWSTICITE],
    :GOLURK       => [:GOLURKNITE],
    :CRABOMINABLE => [:CRABOMINABLENITE],
    :GOLISOPOD    => [:GOLISOPODITE],
    :BAXCALIBUR   => [:BAXCALIBURNITE],
    :ZYGARDE      => [:ZYGARDENITE],
    :TATSUGIRI    => [:TATSUGIRINITE],
    :MACHAMP      => [:MACHAMPITEG],
    :LAPRAS       => [:LAPRASITEG],
    :KINGLER      => [:KINGLERITEG],
    :GARBODOR     => [:GARBODORITEG],
    :HATTERENE    => [:HATTERENITEG],
    :GRIMMSNARL   => [:GRIMMSNARLITEG],
    :INTELEON     => [:INTELEONITEG],
    :RILLABOOM    => [:RILLABOOMITEG],
    :CINDERACE    => [:CINDERACITEG],
    :TOXTRICITY   => [:TOXTRICITYITEG],
    :CENTISKORCH  => [:CENTISKORCHITEG],
    :COALOSSAL    => [:COALOSSALITEG],
    :CORVIKNIGHT  => [:CORVIKNIGHTITEG],
    :ORBEETLE     => [:ORBEETLEITEG],
    :DREDNAW      => [:DREDNAWITEG],
    :COPPERAJAH   => [:COPPERAJAHITEG],
     :ARCEUS   => [:FISTPLATE,
                   :SKYPLATE,
                   :TOXICPLATE,
                   :EARTHPLATE,
                   :STONEPLATE,
                   :INSECTPLATE,
                   :SPOOKYPLATE,
                   :IRONPLATE,
                   :FLAMEPLATE,
                   :SPLASHPLATE,
                   :MEADOWPLATE,
                   :ZAPPLATE,
                   :MINDPLATE,
                   :ICICLEPLATE,
                   :DRACOPLATE,
                   :DREADPLATE,
                   :PIXIEPLATE,
                   :LIGHTPLATE,
                   :SHADOWPLATE,
                   :BLANKPLATE,
                   :LEGENDPLATE],
     :GIRATINA       => [:GRISEOUSORB],
     :GENESECT       => [:BURNDRIVE,:CHILLDRIVE,:DOUSEDRIVE,:SHOCKDRIVE],
     :GIRATINA       => [:GRISEOUSORB],
     :DIALGA         => [:ADAMANTORB],
     :PALKIA         => [:LUSTROUSORB],
     :ZACIAN         => [:RUSTEDSWORD],
     :ZAMAZENTA      => [:RUSTEDSHIELD],
     :KYOGRE         => [:BLUEORB],
     :GROUDON        => [:REDORB],
     :VALKYRIE       => [:BRIGHTSTONE],
     :BLACKKNIGHT    => [:DAWNISTONE],
     :SUGARDEVOIR    => [:TEMPLESCEPTER,:WMDCX,:SQHL,:RADIANTSHARD,:SANCTFEATHER],
     :SUJINRAKU      => [:ABYSSSWORD],
     :SIRFETCHD      => [:HOLYCREST],
     :CHALLEN        => [:CHARLENEITE,:ZEROSUMHEART],
     :OGERPON        => [:HEARTHFLAMEMASK, :WELLSPRINGMASK, :CORNERSTONEMASK]
     }
  combos.each do |comboSpecies, items|
    next if !isConst?(species,PBSpecies,comboSpecies)
    items.each { |i| return true if isConst?(item,PBItems,i) }
    break
  end
  return false
end



#===============================================================================
# ItemHandlers
#===============================================================================
module ItemHandlers
  UseText            = ItemHandlerHash.new
  UseFromBag         = ItemHandlerHash.new
  ConfirmUseInField  = ItemHandlerHash.new
  UseInField         = ItemHandlerHash.new
  UseOnPokemon        = ItemHandlerHash.new
  UseOnPokemonMaximum = ItemHandlerHash.new
  CanUseInBattle     = ItemHandlerHash.new
  UseInBattle        = ItemHandlerHash.new
  BattleUseOnBattler = ItemHandlerHash.new
  BattleUseOnPokemon = ItemHandlerHash.new

  def self.hasUseText(item)
    return UseText[item]!=nil
  end

  def self.hasOutHandler(item)                       # Shows "Use" option in Bag
    return UseFromBag[item]!=nil || UseInField[item]!=nil || UseOnPokemon[item]!=nil
  end

  def self.hasUseInFieldHandler(item)           # Shows "Register" option in Bag
    return UseInField[item]!=nil
  end

  def self.hasUseOnPokemon(item)
    return UseOnPokemon[item]!=nil
  end

  def self.hasUseInBattle(item)
    return UseInBattle[item]!=nil
  end

  def self.hasBattleUseOnBattler(item)
    return BattleUseOnBattler[item]!=nil
  end

  def self.hasBattleUseOnPokemon(item)
    return BattleUseOnPokemon[item]!=nil
  end

  # Returns text to display instead of "Use"
  def self.getUseText(item)
    return UseText.trigger(item)
  end

  # Return value:
  # 0 - Item not used
  # 1 - Item used, don't end screen
  # 2 - Item used, end screen
  # 3 - Item used, don't end screen, consume item
  # 4 - Item used, end screen, consume item
  def self.triggerUseFromBag(item)
    return UseFromBag.trigger(item) if UseFromBag[item]
    # No UseFromBag handler exists; check the UseInField handler if present
    return UseInField.trigger(item) if UseInField[item]
    return 0
  end

  # Returns whether item can be used
  def self.triggerConfirmUseInField(item)
    return true if !ConfirmUseInField[item]
    return ConfirmUseInField.trigger(item)
  end

  # Return value:
  # -1 - Item effect not found
  # 0  - Item not used
  # 1  - Item used
  # 3  - Item used, consume item
  def self.triggerUseInField(item)
    return -1 if !UseInField[item]
    return UseInField.trigger(item)
  end

  # Returns whether item was used
  def self.triggerUseOnPokemon(item,pkmn,scene)
    return false if !UseOnPokemon[item]
    return UseOnPokemon.trigger(item,pkmn,scene)
  end
  
  def self.triggerUseOnPokemonMaximum(item, pkmn)
    return 1 if !UseOnPokemonMaximum[item]
    return [UseOnPokemonMaximum.trigger(item, pkmn), 1].max
  end
  
  def self.triggerCanUseInBattle(item,pkmn,battler,move,firstAction,battle,scene,showMessages=true)
    return true if !CanUseInBattle[item]   # Can use the item by default
    return CanUseInBattle.trigger(item,pkmn,battler,move,firstAction,battle,scene,showMessages)
  end

  def self.triggerUseInBattle(item,battler,battle)
    UseInBattle.trigger(item,battler,battle)
  end

  # Returns whether item was used
  def self.triggerBattleUseOnBattler(item,battler,scene)
    return false if !BattleUseOnBattler[item]
    return BattleUseOnBattler.trigger(item,battler,scene)
  end

  # Returns whether item was used
  def self.triggerBattleUseOnPokemon(item,pkmn,battler,choices,scene)
    return false if !BattleUseOnPokemon[item]
    return BattleUseOnPokemon.trigger(item,pkmn,battler,choices,scene)
  end
end



#===============================================================================
# Change a Pokémon's level
#===============================================================================
def pbChangeLevel(pkmn,newlevel,scene,should_learn=true)
  newlevel = 1 if newlevel<1
  mLevel = PBExperience.maxLevel
  newlevel = mLevel if newlevel>mLevel
  old_level = pkmn.level
  if pkmn.level==newlevel
    pbMessage(_INTL("{1}的等级没有变化。",pkmn.name))
  elsif pkmn.level>newlevel
    attackdiff  = pkmn.attack
    defensediff = pkmn.defense
    speeddiff   = pkmn.speed
    spatkdiff   = pkmn.spatk
    spdefdiff   = pkmn.spdef
    totalhpdiff = pkmn.totalhp
    pkmn.level = newlevel
    pkmn.calcStats
    scene.pbRefresh
    pbMessage(_INTL("{1}下降到Lv.{2}！",pkmn.name,pkmn.level))
    attackdiff  = pkmn.attack-attackdiff
    defensediff = pkmn.defense-defensediff
    speeddiff   = pkmn.speed-speeddiff
    spatkdiff   = pkmn.spatk-spatkdiff
    spdefdiff   = pkmn.spdef-spdefdiff
    totalhpdiff = pkmn.totalhp-totalhpdiff
    pbTopRightWindow(_INTL("最大HP<r>+{1}\r\n攻击<r>+{2}\r\n防御<r>+{3}\r\n特攻<r>+{4}\r\n特防<r>+{5}\r\n速度<r>+{6}",
       totalhpdiff,attackdiff,defensediff,spatkdiff,spdefdiff,speeddiff))
    pbTopRightWindow(_INTL("最大HP<r>{1}\r\n攻击<r>{2}\r\n防御<r>{3}\r\n特攻<r>{4}\r\n特防<r>{5}\r\n速度<r>{6}",
       pkmn.totalhp,pkmn.attack,pkmn.defense,pkmn.spatk,pkmn.spdef,pkmn.speed))
  else
    attackdiff  = pkmn.attack
    defensediff = pkmn.defense
    speeddiff   = pkmn.speed
    spatkdiff   = pkmn.spatk
    spdefdiff   = pkmn.spdef
    totalhpdiff = pkmn.totalhp
    pkmn.level = newlevel
    pkmn.changeHappiness("vitamin")
    pkmn.calcStats
    scene.pbRefresh
    if scene.is_a?(PokemonPartyScreen)
      scene.pbDisplay(_INTL("{1}升到了{2}级！",pkmn.name,pkmn.level))
    else
      pbMessage(_INTL("{1}升到了{2}级！",pkmn.name,pkmn.level))
    end
    attackdiff  = pkmn.attack-attackdiff
    defensediff = pkmn.defense-defensediff
    speeddiff   = pkmn.speed-speeddiff
    spatkdiff   = pkmn.spatk-spatkdiff
    spdefdiff   = pkmn.spdef-spdefdiff
    totalhpdiff = pkmn.totalhp-totalhpdiff
    pbTopRightWindow(_INTL("最大HP<r>+{1}\r\n攻击<r>+{2}\r\n防御<r>+{3}\r\n特攻<r>+{4}\r\n特防<r>+{5}\r\n速度<r>+{6}",
       totalhpdiff,attackdiff,defensediff,spatkdiff,spdefdiff,speeddiff),scene)
    pbTopRightWindow(_INTL("最大HP<r>{1}\r\n攻击<r>{2}\r\n防御<r>{3}\r\n特攻<r>{4}\r\n特防<r>{5}\r\n速度<r>{6}",
       pkmn.totalhp,pkmn.attack,pkmn.defense,pkmn.spatk,pkmn.spdef,pkmn.speed),scene)
       if should_learn
      # Learn new moves upon level up
      realList = []
      moveList = pkmn.getMoveList
      moveList.each do |m|
        next if m[0] <= old_level || m[0] > pkmn.level
        realList.push(m)
      end
      if realList.length == 1 || realList.length > 1 &&
         scene.pbConfirm(_INTL("要{1}立即学习招式吗？",pkmn.name))
        realList.each { |m| pbLearnMove(pkmn, m[1], true) { scene.pbUpdate } }
      end
    end
    # Check for evolution

    #newspecies = pbCheckEvolution(pkmn)
    #if newspecies>0
     # pbFadeOutInWithMusic {
      #  evo = PokemonEvolutionScene.new
      #  evo.pbStartScreen(pkmn,newspecies)
      #  evo.pbEvolution
      #  evo.pbEndScreen
      #  scene.pbRefresh if scene.is_a?(PokemonPartyScreen)
     # }
    #end
   end
end

def pbTopRightWindow(text, scene = nil)
  window = Window_AdvancedTextPokemon.new(text)
  window.width = 198
  window.x     = Graphics.width-window.width
  window.y     = 0
  window.z     = 99999
  pbPlayDecisionSE
  loop do
    Graphics.update
    Input.update
    window.update
    scene.pbUpdate if scene
    break if Input.trigger?(Input::C)
  end
  window.dispose
end

#===============================================================================
# Restore HP
#===============================================================================
def pbItemRestoreHP(pkmn,restoreHP)
  newHP = pkmn.hp+restoreHP
  newHP = pkmn.totalhp if newHP>pkmn.totalhp
  hpGain = newHP-pkmn.hp
  pkmn.hp = newHP
  return hpGain
end

def pbHPItem(pkmn,restoreHP,scene)
  if !pkmn.able? || pkmn.hp==pkmn.totalhp
    scene.pbDisplay(_INTL("这没有任何效果…"))
    return false
  end
  hpGain = pbItemRestoreHP(pkmn,restoreHP)
  scene.pbRefresh
  scene.pbDisplay(_INTL("{1}的HP恢复了{2}点。",pkmn.name,hpGain))
  return true
end

def pbBattleHPItem(pkmn,battler,restoreHP,scene)
  if battler
    if battler.pbRecoverHP(restoreHP)>0
      scene.pbDisplay(_INTL("{1}的HP回复了。",battler.pbThis))
    end
  else
    if pbItemRestoreHP(pkmn,restoreHP)>0
      scene.pbDisplay(_INTL("{1}的HP回复了。",pkmn.name))
    end
  end
  return true
end

#===============================================================================
# Restore PP
#===============================================================================
def pbRestorePP(pkmn,idxMove,pp)
  return 0 if !pkmn.moves[idxMove] || pkmn.moves[idxMove].id==0
  return 0 if pkmn.moves[idxMove].totalpp<=0
  oldpp = pkmn.moves[idxMove].pp
  newpp = pkmn.moves[idxMove].pp+pp
  newpp = pkmn.moves[idxMove].totalpp if newpp>pkmn.moves[idxMove].totalpp
  pkmn.moves[idxMove].pp = newpp
  return newpp-oldpp
end

def pbBattleRestorePP(pkmn, battler, idxMove, pp)
  return if pbRestorePP(pkmn,idxMove,pp) == 0
  if battler && !battler.effects[PBEffects::Transform] &&
     battler.moves[idxMove] && battler.moves[idxMove].id == pkmn.moves[idxMove].id
    battler.pbSetPP(battler.moves[idxMove], pkmn.moves[idxMove].pp)
  end
end

#===============================================================================
# Change EVs
#===============================================================================
def pbJustRaiseEffortValues(pkmn,ev,evgain)
  totalev = 0
  for i in 0...6
    totalev += pkmn.ev[i]
  end
  if totalev+evgain>PokeBattle_Pokemon::EV_LIMIT
    evgain = PokeBattle_Pokemon::EV_LIMIT-totalev
  end
  if pkmn.ev[ev]+evgain>PokeBattle_Pokemon::EV_STAT_LIMIT
    evgain = PokeBattle_Pokemon::EV_STAT_LIMIT-pkmn.ev[ev]
  end
  if evgain>0
    pkmn.ev[ev] += evgain
    pkmn.calcStats
  end
  return evgain
end

def pbRaiseEffortValues(pkmn,ev,evgain=10,evlimit=true)
  return 0 if evlimit && pkmn.ev[ev]>=100
  totalev = 0
  for i in 0...6
    totalev += pkmn.ev[i]
  end
  if totalev+evgain>PokeBattle_Pokemon::EV_LIMIT
    evgain = PokeBattle_Pokemon::EV_LIMIT-totalev
  end
  if pkmn.ev[ev]+evgain>PokeBattle_Pokemon::EV_STAT_LIMIT
    evgain = PokeBattle_Pokemon::EV_STAT_LIMIT-pkmn.ev[ev]
  end
  if evlimit && pkmn.ev[ev]+evgain>PokeBattle_Pokemon::EV_STAT_LIMIT
    evgain = PokeBattle_Pokemon::EV_STAT_LIMIT-pkmn.ev[ev]
  end
  if evgain>0
    pkmn.ev[ev] += evgain
    pkmn.calcStats
  end
  return evgain
end

def pbRaiseHappinessAndLowerEV(pkmn,scene,ev,messages)
  h = pkmn.happiness<255
  e = pkmn.ev[ev]>0
  if !h && !e
    scene.pbDisplay(_INTL("这没有任何效果…"))
    return false
  end
  if h
    pkmn.changeHappiness("evberry")
  end
  if e
    pkmn.ev[ev] -= 10
    pkmn.ev[ev] = 0 if pkmn.ev[ev]<0
    pkmn.calcStats
  end
  scene.pbRefresh
  scene.pbDisplay(messages[2-(h ? 0 : 2)-(e ? 0 : 1)])
  return true
end

def pbEmptyAllEV(pkmn, scene, message)
  noEV = true
  pkmn.ev.each do |ev|
    if ev > 0
      noEV = false
      break
    end
  end
  if noEV
    scene.pbDisplay(_INTL("没有任何效果。"))
    return false
  end
  pkmn.ev = [0,0,0,0,0,0]
  pkmn.calcStats
  scene.pbRefresh
  scene.pbDisplay(message)
  return true
end

#===============================================================================
# Battle items
#===============================================================================
def pbBattleItemCanCureStatus?(status,pkmn,scene,showMessages)
  if !pkmn.able? || pkmn.status!=status
    scene.pbDisplay(_INTL("这没有任何效果…")) if showMessages
    return false
  end
  return true
end

def pbBattleItemCanRaiseStat?(stat,battler,scene,showMessages)
  if !battler || !battler.pbCanRaiseStatStage?(stat,battler)
    scene.pbDisplay(_INTL("这没有任何效果…")) if showMessages
    return false
  end
  return true
end

#===============================================================================
# Decide whether the player is able to ride/dismount their Bicycle
#===============================================================================
def pbBikeCheck
  if $PokemonGlobal.surfing ||
     (!$PokemonGlobal.bicycle && PBTerrain.onlyWalk?(pbGetTerrainTag))
    pbMessage(_INTL("这里不能使用。"))
    return false
  end
  if $game_player.pbHasDependentEvents?
    pbMessage(_INTL("与他人同行时不能使用。"))
    return false
  end
  if $PokemonGlobal.bicycle
    if pbGetMetadata($game_map.map_id,MetadataBicycleAlways)
      pbMessage(_INTL("你不能在这里下车。"))
      return false
    end
    return true
  end
  val = pbGetMetadata($game_map.map_id,MetadataBicycle)
  val = pbGetMetadata($game_map.map_id,MetadataOutdoor) if val==nil
  if !val
    pbMessage(_INTL("这里不能使用。"))
    return false
  end
  return true
end

#===============================================================================
# Find the closest hidden item (for Iremfinder)
#===============================================================================
def pbClosestHiddenItem
  result = []
  playerX = $game_player.x
  playerY = $game_player.y
  for event in $game_map.events.values
    next if !event.name[/hiddenitem/i]
    next if (playerX-event.x).abs>=8
    next if (playerY-event.y).abs>=6
    next if $game_self_switches[[$game_map.map_id,event.id,"A"]]
    result.push(event)
  end
  return nil if result.length==0
  ret = nil
  retmin = 0
  for event in result
    dist = (playerX-event.x).abs+(playerY-event.y).abs
    next if ret && retmin<=dist
    ret = event
    retmin = dist
  end
  return ret
end

#===============================================================================
# Teach and forget a move
#===============================================================================
def pbLearnMove(pkmn,move,ignoreifknown=false,bymachine=false,&block)
  return false if !pkmn
  movename = PBMoves.getName(move)
  if pkmn.egg? && !$DEBUG
    pbMessage(_INTL("蛋不能学会招式。"),&block)
    return false
  end
  if pkmn.shadowPokemon?
    pbMessage(_INTL("黑暗宝可梦不能学会招式。"),&block)
    return false
  end
  pkmnname = pkmn.name
  if pkmn.hasMove?(move)
    pbMessage(_INTL("{1}已经学会了{2}。",pkmnname,movename),&block) if !ignoreifknown
    return false
  end
  if pkmn.numMoves<4
    pkmn.pbLearnMove(move)
    pbMessage(_INTL("\\se[]{1}学会了{2}！\\se[Pkmn move learnt]",pkmnname,movename),&block)
    return true
  end
  loop do
    pbMessage(_INTL("{1}想要学会{2}\n可是它已经学会四个招式了。\1",pkmnname,movename),&block) if !bymachine
    pbMessage(_INTL("请选择将被{1}替换的招式。",movename),&block)
    forgetmove = pbForgetMove(pkmn,move)
    if forgetmove>=0
      oldmovename = PBMoves.getName(pkmn.moves[forgetmove].id)
      oldmovepp   = pkmn.moves[forgetmove].pp
      pkmn.moves[forgetmove] = PBMove.new(move)   # Replaces current/total PP
      if bymachine && !NEWEST_BATTLE_MECHANICS
        pkmn.moves[forgetmove].pp = [oldmovepp,pkmn.moves[forgetmove].totalpp].min
      end
      pbMessage(_INTL("1,\\wt[16] 2,\\wt[16]...\\wt[16] ...\\wt[16] ... 当当！\\se[Battle ball drop]\1"),&block)
      pbMessage(_INTL("{1}遗忘了{2}。\\n以及……\1",pkmnname,oldmovename),&block)
      pbMessage(_INTL("\\se[]{1}学会了{2}！\\se[Pkmn move learnt]",pkmnname,movename),&block)
      pkmn.changeHappiness("machine") if bymachine
      return true
    else
      pbMessage(_INTL("{1}没有学习{2}。",pkmnname,movename),&block)
      return false
    end
  end
end

def pbForgetMove(pkmn,moveToLearn)
  ret = -1
  pbFadeOutIn {
    scene = PokemonSummary_Scene.new
    screen = PokemonSummaryScreen.new(scene)
    ret = screen.pbStartForgetScreen([pkmn],0,moveToLearn)
  }
  return ret
end

def pbSpeciesCompatible?(species,move)
  return false if species<=0
#    return true if [getConst(PBSpecies,:DARKNIGHT), getConst(PBSpecies,:BRILLIANT)].include?(species)
  data = pbLoadSpeciesTMData
  return false if !data[move]
  return data[move].any? { |item| item==species }
end

#===============================================================================
# Use an item from the Bag and/or on a Pokémon
#===============================================================================
def pbUseItem(bag,item,bagscene=nil)
  found = false
  useType = pbGetItemData(item,ITEM_FIELD_USE)
  if pbIsMachine?(item)    # TM or HM or TR
    if $Trainer.pokemonCount==0
      pbMessage(_INTL("没有宝可梦。"))
      return 0
    end
    machine = pbGetMachine(item)
    return 0 if machine==nil
    movename = PBMoves.getName(machine)
    pbMessage(_INTL("\\se[PC access]启动了{1}。\1",PBItems.getName(item)))
    if !pbConfirmMessage(_INTL("想教{1}给宝可梦吗？",movename))
      return 0
    elsif mon=pbMoveTutorChoose(machine,nil,true)
      bag.pbDeleteItem(item) if pbIsTechnicalMachine?(item) && !INFINITE_TMS
      if pbIsTechnicalRecord?(item)
        bag.pbDeleteItem(item)
        $Trainer.party[mon].trmoves.push(machine) if mon.is_a?(Numeric)
      end
      return 1
    end
    return 0
  elsif useType && (useType==1 || useType==5) # Item is usable on a Pokémon
    if $Trainer.pokemonCount==0
      pbMessage(_INTL("没有宝可梦。"))
      return 0
    end
    ret = false
    annot = nil
    if pbIsEvolutionStone?(item)
      annot = []
      for pkmn in $Trainer.party
        elig = pbCheckEvolution(pkmn,item)>0
        annot.push((elig) ? _INTL("可用") : _INTL("不可用"))
      end
    end
    pbFadeOutIn {
      scene = PokemonParty_Scene.new
      screen = PokemonPartyScreen.new(scene,$Trainer.party)
      screen.pbStartScene(_INTL("要对哪只宝可梦使用？"),false,annot)
      loop do
        scene.pbSetHelpText(_INTL("要对哪只宝可梦使用？"))
        chosen = screen.pbChoosePokemon
        if chosen<0
          ret = false
          break
        end
        pkmn = $Trainer.party[chosen]
        next if !pbCheckUseOnPokemon(item,pkmn,screen)
        qty = 1
        max_at_once = ItemHandlers.triggerUseOnPokemonMaximum(item, pkmn)
        max_at_once = [max_at_once, $PokemonBag.pbQuantity(item)].min
        if max_at_once > 1
          qty = screen.scene.pbChooseNumber(_INTL("你要使用多少{1}？", PBItems.getName(item)), max_at_once)
          screen.scene.pbSetHelpText("") if screen.is_a?(PokemonPartyScreen)
        end
        next if qty <= 0
        if pbCheckUseOnPokemon(item,pkmn,screen)
         ret = ItemHandlers.triggerUseOnPokemon(item,pkmn,screen)
        if ret && useType==1   # Usable on Pokémon, consumed
            bag.pbDeleteItem(item)
            if !bag.pbHasItem?(item)
              pbMessage(_INTL("最后的{1}被使用了。",PBItems.getName(item))) { screen.pbUpdate }
              break
            end
          end
        end
      end
      screen.pbEndScene
      bagscene.pbRefresh if bagscene
    }
    return (ret) ? 1 : 0
  elsif useType && useType==2   # Item is usable from bag
    intret = ItemHandlers.triggerUseFromBag(item)
    case intret
    when 0; return 0
    when 1; return 1   # Item used
    when 2; return 2   # Item used, end screen
    when 3; bag.pbDeleteItem(item); return 1   # Item used, consume item
    when 4; bag.pbDeleteItem(item); return 2   # Item used, end screen and consume item
    end
    pbMessage(_INTL("这里不能使用。"))
    return 0
  end
  pbMessage(_INTL("这里不能使用。"))
  return 0
end

# Only called when in the party screen and having chosen an item to be used on
# the selected Pokémon
def pbUseItemOnPokemon(item,pkmn,scene)
  # TM or HM
  if pbIsMachine?(item)
    machine = pbGetMachine(item)
    return false if machine==nil
    movename = PBMoves.getName(machine)
    if pkmn.shadowPokemon?
      pbMessage(_INTL("黑暗宝可梦不能学会招式。")) { scene.pbUpdate }
    elsif !pkmn.compatibleWithMove?(machine)
      pbMessage(_INTL("{1}不能学习{2}。",pkmn.name,movename)) { scene.pbUpdate }
    else
      pbMessage(_INTL("\\se[PC access]启动了{1}\1",PBItems.getName(item))) { scene.pbUpdate }
      if pbConfirmMessage(_INTL("想教{1}给宝可梦吗？",movename,pkmn.name)) { scene.pbUpdate }
        if pbLearnMove(pkmn,machine,false,true) { scene.pbUpdate }
          $PokemonBag.pbDeleteItem(item) if pbIsTechnicalMachine?(item) && !INFINITE_TMS
          if pbIsTechnicalRecord?(item)
            $PokemonBag.pbDeleteItem(item)
            pkmn.trmoves.push(machine)
          end
          return true
        end
      end
    end
    return false
  end
  # Other item
  ret = ItemHandlers.triggerUseOnPokemon(item,pkmn,scene)
  scene.pbClearAnnotations
  scene.pbHardRefresh
  useType = pbGetItemData(item,ITEM_FIELD_USE)
  if ret && useType && useType==1   # Usable on Pokémon, consumed
    $PokemonBag.pbDeleteItem(item)
    if !$PokemonBag.pbHasItem?(item)
      pbMessage(_INTL("最后的{1}被使用了。",PBItems.getName(item))) { scene.pbUpdate }
    end
  end
  return ret
end

def pbUseKeyItemInField(item)
  ret = ItemHandlers.triggerUseInField(item)
  if ret==-1   # Item effect not found
    pbMessage(_INTL("这里不能使用。"))
  elsif ret==3   # Item was used and consumed
    $PokemonBag.pbDeleteItem(item)
  end
  return ret!=-1 && ret!=0
end

def pbUseItemMessage(item)
  itemname = PBItems.getName(item)
  if itemname.starts_with_vowel?
    pbMessage(_INTL("使用了{1}。",itemname))
  else
    pbMessage(_INTL("使用了{1}。",itemname))
  end
end

def pbCheckUseOnPokemon(_item,pkmn,_screen)
  return pkmn && !pkmn.egg?
end

#===============================================================================
# Give an item to a Pokémon to hold, and take a held item from a Pokémon
#===============================================================================
def pbGiveItemToPokemon(item,pkmn,scene,pkmnid=0)
  newitemname = PBItems.getName(item)
  if pkmn.egg?
    scene.pbDisplay(_INTL("宝可梦蛋不能携带物品。"))
    return false
  elsif pkmn.mail
    scene.pbDisplay(_INTL("必须先拿回{1}身上的邮件，<<n>>然后才能给予道具。",pkmn.name))
    return false if !pbTakeItemFromPokemon(pkmn,scene)
  end
  if pkmn.hasItem?
    olditemname = PBItems.getName(pkmn.item)
    if pkmn.hasItem?(:LEFTOVERS)
      scene.pbDisplay(_INTL("{1}已经携带{2}了。\1",pkmn.name,olditemname))
    elsif newitemname.starts_with_vowel?
      scene.pbDisplay(_INTL("{1}已经携带{2}了。\1",pkmn.name,olditemname))
    else
      scene.pbDisplay(_INTL("{1}已经携带{2}了。\1",pkmn.name,olditemname))
    end
    if scene.pbConfirm(_INTL("想要交换这两个道具吗？"))
      $PokemonBag.pbDeleteItem(item)
      if !$PokemonBag.pbStoreItem(pkmn.item)
        if !$PokemonBag.pbStoreItem(item)
          raise _INTL("无法在包中重新存储删除的道具")
        end
        scene.pbDisplay(_INTL("背包装满了。\n宝可梦身上的道具无法拿回。"))
      else
        if pbIsMail?(item)
          if pbWriteMail(item,pkmn,pkmnid,scene)
            pkmn.setItem(item)
            scene.pbDisplay(_INTL("从{2}身上拿回{1}了，\n给予了{3}。",olditemname,pkmn.name,newitemname))
            return true
          else
            if !$PokemonBag.pbStoreItem(item)
              raise _INTL("无法在包中重新存储删除的道具")
            end
          end
        else
          pkmn.setItem(item)
          scene.pbDisplay(_INTL("从{2}身上拿回{1}了，\n给予了{3}。",olditemname,pkmn.name,newitemname))
          return true
        end
      end
    end
  else
    if !pbIsMail?(item) || pbWriteMail(item,pkmn,pkmnid,scene)
      $PokemonBag.pbDeleteItem(item)
      pkmn.setItem(item)
      scene.pbDisplay(_INTL("{1}现在携带着{2}。",pkmn.name,newitemname))
      return true
    end
  end
  return false
end

def pbTakeItemFromPokemon(pkmn,scene)
  ret = false
  if !pkmn.hasItem?
    scene.pbDisplay(_INTL("{1}现在什么也没携带。",pkmn.name))
  elsif !$PokemonBag.pbCanStore?(pkmn.item)
    scene.pbDisplay(_INTL("背包装满了。\n宝可梦身上的道具无法拿回。"))
  elsif pkmn.mail
    if scene.pbConfirm(_INTL("保存邮件到电脑？"))
      if !pbMoveToMailbox(pkmn)
        scene.pbDisplay(_INTL("电脑的邮箱满了。"))
      else
        scene.pbDisplay(_INTL("邮件保存在你的电脑里了。"))
        pkmn.setItem(0)
        ret = true
      end
    elsif scene.pbConfirm(_INTL("如果邮件被删除，\n它携带的信息将丢失。确定吗？"))
      $PokemonBag.pbStoreItem(pkmn.item)
      itemname = PBItems.getName(pkmn.item)
      scene.pbDisplay(_INTL("从{2}那接收了{1}。",itemname,pkmn.name))
      pkmn.setItem(0)
      pkmn.mail = nil
      ret = true
    end
  else
    $PokemonBag.pbStoreItem(pkmn.item)
    itemname = PBItems.getName(pkmn.item)
    scene.pbDisplay(_INTL("从{2}那接收了{1}。",itemname,pkmn.name))
    pkmn.setItem(0)
    ret = true
  end
  return ret
end

#===============================================================================
# Choose an item from the Bag
#===============================================================================
def pbChooseItem(var=0,*args)
  ret = 0
  pbFadeOutIn {
    scene = PokemonBag_Scene.new
    screen = PokemonBagScreen.new(scene,$PokemonBag)
    ret = screen.pbChooseItemScreen
  }
  $game_variables[var] = ret if var>0
  return ret
end

def pbChooseApricorn(var=0)
  ret = 0
  pbFadeOutIn {
    scene = PokemonBag_Scene.new
    screen = PokemonBagScreen.new(scene,$PokemonBag)
    ret = screen.pbChooseItemScreen(Proc.new { |item| pbIsApricorn?(item) })
  }
  $game_variables[var] = ret if var>0
  return ret
end

def pbChooseFossil(var=0)
  ret = 0
  pbFadeOutIn {
    scene = PokemonBag_Scene.new
    screen = PokemonBagScreen.new(scene,$PokemonBag)
    ret = screen.pbChooseItemScreen(Proc.new { |item| pbIsFossil?(item) })
  }
  $game_variables[var] = ret if var>0
  return ret
end

# Shows a list of items to choose from, with the chosen item's ID being stored
# in the given Global Variable. Only items which the player has are listed.
def pbChooseItemFromList(message,variable,*args)
  commands = []
  itemid   = []
  for item in args
    next if !hasConst?(PBItems,item)
    id = getConst(PBItems,item)
    next if !$PokemonBag.pbHasItem?(id)
    commands.push(PBItems.getName(id))
    itemid.push(id)
  end
  if commands.length==0
    $game_variables[variable] = 0
    return 0
  end
  commands.push(_INTL("取消"))
  itemid.push(0)
  ret = pbMessage(message,commands,-1)
  if ret<0 || ret>=commands.length-1
    $game_variables[variable] = -1
    return -1
  end
  $game_variables[variable] = itemid[ret]
  return itemid[ret]
end