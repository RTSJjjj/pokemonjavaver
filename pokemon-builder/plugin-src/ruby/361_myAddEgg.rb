def myAddEgg(egg,text="")
  return false if !egg
  egg = getID(PBSpecies,egg)
  if egg.is_a?(Integer)
    egg = pbNewPkmn(egg,EGG_LEVEL)
  end
  # Get egg steps
  eggSteps = pbGetSpeciesData(egg.species,egg.form,SpeciesStepsToHatch)
  # Set egg's details
  egg.name       = _INTL("神秘的蛋")
  egg.eggsteps   = eggSteps
  egg.obtainText = text
  egg.calcStats
  if $Trainer.party.length<6
    $Trainer.party[$Trainer.party.length] = egg
    pbMessage(_INTL("{1}加入了队伍。",egg.name))
  else
    oldcurbox = $PokemonStorage.currentBox
    storedbox = $PokemonStorage.pbStoreCaught(egg)
    curboxname = $PokemonStorage[oldcurbox].name
    boxname = $PokemonStorage[storedbox].name
    creator = nil
    creator = pbGetStorageCreator if $PokemonGlobal.seenStorageCreator
    if storedbox!=oldcurbox
      if creator
        pbMessage(_INTL("{2}的电脑上的盒子 \"{1}\"已经满了。\1",curboxname,creator))
      else
        pbMessage(_INTL("某人的电脑上的盒子 \"{1}\"已经满了。\1",curboxname))
      end
      pbMessage(_INTL("{1}被传送到了盒子 \"{2}.\"",egg.name,boxname))
    else
      if creator
        pbMessage(_INTL("{1}被传送到了{2}的电脑。\1",egg.name,creator))
      else
        pbMessage(_INTL("{1}被传送到了某人的电脑。\1",egg.name))
      end
      pbMessage(_INTL("被存放在盒子\"{1}.\"",boxname))
    end
  end
end