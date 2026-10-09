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