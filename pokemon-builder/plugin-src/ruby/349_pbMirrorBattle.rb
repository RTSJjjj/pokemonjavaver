def pbMirrorBattle(outcomeVar = 1)
  trainerid     = $Trainer.trainertype + 998
  data          = pbGetTrainerTypeData(trainerid)
  trainertype   = data && data[1] ? data[1] : :POKEMONTRAINER_MIRRORm
  trainerGender = $Trainer.gender
  trainerName   = $Trainer.name + "？"
  loseText      = trainerName + "：对战结束。本次模拟对战结果为：“失败”。"
  items = []
  6.times do
    items.push(getConst(PBItems, :FULLRESTORE))
  end
  party = []
  $Trainer.party.each { |pkmn| party.push(pkmn.clone) }
  opponent = PokeBattle_Trainer.new(trainerName,trainerid)
  opponent.setForeignID($Trainer)
  trainer = [opponent, party, loseText, items]
  pbTrainerIntro(trainertype)
  setBattleRule("noMoney")
  setBattleRule("noExp")
  setBattleRule("canLose")
  setBattleRule("outcomeVar",outcomeVar) if outcomeVar != 1
  $PokemonGlobal.nextBattleBGM = "Battle 影子"
  $PokemonGlobal.nextBattleME = "Battle victory Boss Pokemon"
  decision = pbTrainerBattleCore(trainer)
  return (decision==1)
end