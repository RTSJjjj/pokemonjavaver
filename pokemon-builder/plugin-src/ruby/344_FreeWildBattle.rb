#定义形态战斗
def pbFreeWildBattle(pkmn, outcomeVar=1, canRun=true, canLose=false)
  setBattleRule("outcomeVar",outcomeVar) if outcomeVar!=1
  setBattleRule("cannotRun") if !canRun
  setBattleRule("canLose") if canLose
  decision = pbWildBattleCore(pkmn)
  return (decision!=2 && decision!=5)
end

# 查询种族值
def showBV
  pbChooseNonEggPokemon(1,3)
  var = $game_variables[1]
  if var>=0 && var<=5
    pkmn=pbGetPokemon(1)
    hp      = pkmn.baseStats[PBStats::HP]
    attack  = pkmn.baseStats[PBStats::ATTACK]
    defense = pkmn.baseStats[PBStats::DEFENSE]
    spAtk   = pkmn.baseStats[PBStats::SPATK]
    spDef   = pkmn.baseStats[PBStats::SPDEF]
    speed   = pkmn.baseStats[PBStats::SPEED]
    pbMessage(_INTL("<ac>\\l[3]{1}的种族值\nHP:{2}    物攻:{3}    物防:{4}\n速度:{5}    特攻:{6}    特防:{7}",pkmn.name,hp,attack,defense,speed,spAtk,spDef))
    pbMessage(_INTL("不同种族值的宝可梦没有高低贵贱之分，\n就像这个世界上的所有生物一样，\n都是平等的，我们要敬畏生命。"))
  else
    pbMessage(_INTL("好吧，我随时可以帮你看看宝可梦的种族值。"))
  end
end

#赠送精灵
def battleLightDim(species, level=100, item=0, outcome=50)
  pkmn = pbGenPkmn(species, level)
  #pkmn.setAbility(2)  #梦特
  #pkmn.iv = [31,31,31,31,31,31]  #6V
  pkmn.setItem(item)  #道具
  pkmn.setForm(1)  #形态1
  pkmn.calcStats
  pbFreeWildBattle(pkmn, outcome)
  return pbGet(outcome)==1 || pbGet(outcome)==4
end
