# 用于测试，事件里用条件分歧-脚本 battleBoss(species, level, rank)
# 例如：battleBoss(:ARCEUS, 100, 4)
# 这种方式好处是比较通用，
# 缺点是不能更一步的个性化，比如配招、异色之类的
def battleBoss(species, level=100, rank=3)
  if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(species, level)
  if rand(65536)<64
    pkmn.makeShiny
    pkmn.makeSuperShiny if rand(16)==1
  end
  pkmn.battleRank = rank
  pkmn.iv = pbRandomIV(rank - 1)
  changeEVandNature(pkmn)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end
# 用于测试，事件里用条件分歧-脚本 battleArceus
# 这种方式好处是能更一步的个性化，比如配招、异色之类的，
# 缺点是不通用，每一只都要单独写

# 赫月
def battleUrsalunaBloodmoon
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:URSALUNA, 150)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0]
  pkmn.battleRank = 5
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:CALM)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:BLOODMOON)
  pkmn.pbLearnMove(:EARTHPOWER)
  pkmn.pbLearnMove(:MOONBLAST)
  pkmn.pbLearnMove(:MOONLIGHT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 鏖魔
def battleHaxorusDemon
  if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true   
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:HAXORUS,210)
  pkmn.form = 2
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,0,4,252]
  pkmn.battleRank = 5
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:FIERCEKILLING)
  pkmn.pbLearnMove(:OUTRAGE)
  pkmn.pbLearnMove(:CRUNCH)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false 
  return decision==1
end

# 诡幽岩铠（幽岩儿进化）
def battleSpiritHite
  if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true   
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SPIRITHITE, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,0,4,252]
  pkmn.battleRank = 4
  pkmn.setItem(:HARDSTONE)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:BLACKSANDCANNON)
  pkmn.pbLearnMove(:STONEEDGE)
  pkmn.pbLearnMove(:SHADOWCLAW)
  pkmn.pbLearnMove(:SHELLSMASH)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false 
 return decision==1
end

# 妖雷云龙（雷龙）
def battleDrathunlin
  if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:DRATHUNLIN, 100)
  pkmn.iv = [31,0,31,31,31,31]
  pkmn.ev = [0,0,0,252,252,4]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(2)
  pkmn.pbLearnMove(:DRAGONSPEAR)
  pkmn.pbLearnMove(:FLASHCANNON)
  pkmn.pbLearnMove(:AEROBLAST)
  pkmn.pbLearnMove(:THUNDER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false 
 return decision==1
end


# 开心蛋
def battleHappun
  if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true   
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:HAPPUN, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SOFTBOILED)
  pkmn.pbLearnMove(:HEALINGWISH)
  pkmn.pbLearnMove(:DOUBLEEDGE)
  pkmn.pbLearnMove(:HEALPULSE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false 
 return decision==1
end

# 败露球菇
def battleAmoonguss
if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true   
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:AMOONGUSS, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0]
  pkmn.battleRank = 4
  pkmn.setItem(:BLACKSLUDGE)
  pkmn.setNature(:SASSY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SPORE)
  pkmn.pbLearnMove(:GIGADRAIN)
  pkmn.pbLearnMove(:SLUDGEBOMB)
  pkmn.pbLearnMove(:CLEARSMOG)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false 
 return decision==1
end

# 噬沙堡爷
def battlePalossand
if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true   
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:PALOSSAND, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SHADOWBALL)
  pkmn.pbLearnMove(:EARTHPOWER)
  pkmn.pbLearnMove(:STEALTHROCK)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false 
 return decision==1
end

# 雷电兽
def battleManectric
  if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true  
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MANECTRIC, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,0,0,252,4,252]
  pkmn.battleRank = 4
  pkmn.setItem(:MANECTITE)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:FLAMETHROWER)
  pkmn.pbLearnMove(:VOLTSWITCH)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false 
 return decision==1
end

# 鬼盆栽（花岩怪）
def battleSpiritomb
  if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true   
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SPIRITOMB, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,0,0,252,4]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:CALM)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:SHADOWBALL)
  pkmn.pbLearnMove(:WILLOWISP)
  pkmn.pbLearnMove(:PAINSPLIT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false 
 return decision==1
end

# 天蝎王
def battleGliscor
if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true   
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GLISCOR, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,0,0,4,252]
  pkmn.battleRank = 4
  pkmn.setItem(:TOXICORB)
  pkmn.setNature(:IMPISH)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:KNOCKOFF)
  pkmn.pbLearnMove(:ROOST)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false 
 return decision==1
end

# 果然翁
def battleWobbuffet
if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true   
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:WOBBUFFET, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:COUNTER)
  pkmn.pbLearnMove(:MIRRORCOAT)
  pkmn.pbLearnMove(:ENCORE)
  pkmn.pbLearnMove(:DESTINYBOND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false 
 return decision==1
end

# 阿罗拉呆呆王
def battleSlowkingAlola
if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SLOWKING, 100)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SCALD)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:SLACKOFF)
  pkmn.pbLearnMove(:TOXIC)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 月伊布
def battleUmbreon
if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:UMBREON, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:CAREFUL)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:FOULPLAY)
  pkmn.pbLearnMove(:TOXIC)
  pkmn.pbLearnMove(:MOONLIGHT)
  pkmn.pbLearnMove(:PROTECT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 进化启石多边兽2
def battlePorygon2
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:PORYGON2, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:EVIOLITE)
  pkmn.setNature(:CALM)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:TRIATTACK)
  pkmn.pbLearnMove(:ICEBEAM)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 盐石巨灵
def battleGarganacl
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GARGANACL, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:IMPISH)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SALTURE)
  pkmn.pbLearnMove(:BODYPRESS)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 赛富豪
def battleGholdengo
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GHOLDENGO, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:CHOICESPECS)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:MAKESHIFT)
  pkmn.pbLearnMove(:SHADOWBALL)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:FOCUSBLAST)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end


# 赛富豪
def battleGholdengo2
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GHOLDENGO, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:CHOICESPECS)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:MAKESHIFT)
  pkmn.pbLearnMove(:SHADOWBALL)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:FOCUSBLAST)
  pkmn.makeShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# BOSS大竺葵
def battleBossMeganium
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MEGANIUM,210)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0]
  pkmn.battleRank = 7
  pkmn.setItem(:MEGANIUMITER)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:BLOOMINGROSE)
  pkmn.pbLearnMove(:DRACOMETEOR)
  pkmn.pbLearnMove(:JUNGLEHEALING)
  pkmn.pbLearnMove(:SYNTHESIS)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 西狮海壬
def battleBossPrimarina
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:PRIMARINA,210)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 7
  pkmn.setItem(:PRIMARINAITER)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:TEARALAMENT)
  pkmn.pbLearnMove(:SHADOWBALL)
  pkmn.pbLearnMove(:HYDROPUMP)
  pkmn.pbLearnMove(:PERISHSONG)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# BOSS火焰鸡
def battleBossBlaziken
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:BLAZIKEN,210)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,0,4,252]
  pkmn.battleRank = 7
  pkmn.setItem(:BLAZIKENITER)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(1)
  pkmn.pbLearnMove(:SPECTRALFLARE)
  pkmn.pbLearnMove(:FLAREBLITZ)
  pkmn.pbLearnMove(:NIGHTSLASH)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 土台龟（可超级进化）
def battleBossTorterra
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TORTERRA,210)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 7
  pkmn.setItem(:TORTERRAITE)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:HEAVYSLAM)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:WOODHAMMER)
  pkmn.pbLearnMove(:STONEEDGE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 200级 20倍HP 幸福蛋 BOSS
def battleBlisseyBoss
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:BLISSEY,210)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0] 
  pkmn.battleRank = 7
  pkmn.setItem(:LEFTOVERS)      
  pkmn.setNature(:BOLD)  
  pkmn.setAbility(0)          
  pkmn.pbLearnMove(:SOFTBOILED)    # 生蛋 
  pkmn.pbLearnMove(:SEISMICTOSS)   # 地球上投 
  pkmn.pbLearnMove(:TOXIC)         # 剧毒
  pkmn.pbLearnMove(:MINIMIZE)      # 变小 
  pkmn.makeNotShiny
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 超级路卡利欧
def battleMegaLucario
  count = $Trainer.ablePokemonCount
  size = (count >= 1) ? 1 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:LUCARIO, 80)
  pkmn.setItem(:LUCARIONITE)
  pkmn.makeShiny
  pkmn.form = 0
  pkmn.makeMale
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:AURASPHERE)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:BULLETPUNCH)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 水晶大岩蛇
def battleOnixCrystal
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ONIX, 60)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:IMPISH)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:BLIZZARD)
  pkmn.pbLearnMove(:STONEEDGE)
  pkmn.pbLearnMove(:AVALANCHE)
  pkmn.pbLearnMove(:ROCKPOLISH)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end
# 鼠头地龙
def battleExcadrillDragon
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:EXCADRILL, 70)
  pkmn.form = 2
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:DRAGONFANG)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:DRAGONCLAW)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:DRAGONDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 以欧路普
def battleOrbeetle
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ORBEETLE,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:ORBEETLEITEG)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:BUGBUZZ)
  pkmn.pbLearnMove(:SHADOWBALL)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 巨炭山
def battleCoalossal
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:COALOSSAL, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:COALOSSALITEG)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:OVERHEAT)
  pkmn.pbLearnMove(:POWERGEM)
  pkmn.pbLearnMove(:EARTHPOWER)
  pkmn.pbLearnMove(:STEALTHROCK)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 钢铠鸦
def battleCorviknight
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CORVIKNIGHT, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:CORVIKNIGHTITEG)
  pkmn.setNature(:IMPISH)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:BRAVEBIRD)
  pkmn.pbLearnMove(:IRONHEAD)
  pkmn.pbLearnMove(:ROOST)
  pkmn.pbLearnMove(:BULKUP)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 耿鬼
def battleGengar
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GENGAR, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:GENGARITEG)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SHADOWBALL)
  pkmn.pbLearnMove(:SLUDGEBOMB)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:FOCUSBLAST)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 龙头地鼠
def battleExcadrill
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:EXCADRILL, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:EXCADRILLITE)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:IRONHEAD)
  pkmn.pbLearnMove(:ROCKSLIDE)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 阿勃梭鲁
def battleAbsol
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ABSOL, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:ABSOLITEZ)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:KNOCKOFF)
  pkmn.pbLearnMove(:SUCKERPUNCH)
  pkmn.pbLearnMove(:PSYCHOCUT)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 列阵兵
def battleFalinks
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:FALINKS, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:FALINKSITE)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:MEGAHORN)
  pkmn.pbLearnMove(:POISONJAB)
  pkmn.pbLearnMove(:NOBOLEROAR)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 晶光花
def battleGlimmora
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GLIMMORA, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:GLIMMORANITE)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:POWERGEM)
  pkmn.pbLearnMove(:SLUDGEWAVE)
  pkmn.pbLearnMove(:EARTHPOWER)
  pkmn.pbLearnMove(:STEALTHROCK)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end


# 阿尔宙斯
def battleArceus
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ARCEUS,210)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.setItem(:LEGENDPLATE)
  pkmn.form = 21
  pkmn.battleRank = 7
  pkmn.setNature(:MODEST)
  pkmn.pbLearnMove(:JUDGMENT)
  pkmn.pbLearnMove(:HYPERVOICE)
  pkmn.pbLearnMove(:EXTREMESPEED)
  pkmn.pbLearnMove(:EARTHPOWER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 火焰鸟
def battleMoltres
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MOLTRES, 80)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:CHARCOAL)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:FIERYWRATH)
  pkmn.pbLearnMove(:AIRSLASH)
  pkmn.pbLearnMove(:SOLARBEAM)
  pkmn.pbLearnMove(:ROOST)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 急冻鸟
def battleArticuno
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ARTICUNO, 80)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:NEVERMELTICE)
  pkmn.setNature(:CALM)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:FREEZINGGLARE)
  pkmn.pbLearnMove(:ICEBEAM)
  pkmn.pbLearnMove(:HURRICANE)
  pkmn.pbLearnMove(:ROOST)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 闪电鸟
def battleZapdos
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZAPDOS,80)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:MAGNET)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:THUNDEROUSKICK)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:HEATWAVE)
  pkmn.pbLearnMove(:ROOST)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 伽勒尔火焰鸟
def battleMoltresGalar
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MOLTRES,100)
  pkmn.form = 1  # 伽勒尔形态
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:BLACKGLASSES)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:FIERYWRATH)
  pkmn.pbLearnMove(:AIRSLASH)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 伽勒尔急冻鸟
def battleArticunoGalar
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ARTICUNO,100)
  pkmn.form = 1  # 伽勒尔形态
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:TWISTEDSPOON)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:FREEZINGGLARE)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:HURRICANE)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 伽勒尔闪电鸟
def battleZapdosGalar
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZAPDOS,100)
  pkmn.form = 1  # 伽勒尔形态
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:BLACKBELT)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:THUNDEROUSKICK)
  pkmn.pbLearnMove(:DRILLPECK)
  pkmn.pbLearnMove(:BRAVEBIRD)
  pkmn.pbLearnMove(:BULKUP)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 凤王
def battleHoOh
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:HOOH,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:SACREDASH)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SACREDFIRE)
  pkmn.pbLearnMove(:BRAVEBIRD)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 洛奇亚
def battleLugia
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:LUGIA,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:AEROBLAST)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 暗黑洛奇亚
def battleDimLugia
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:DIMLUGIA,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:OBLIVIONWING)
  pkmn.pbLearnMove(:SHADOWBLAST)
  pkmn.pbLearnMove(:HYDROPUMP)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 梦幻
def battleMew
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MEW, 60)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:AURASPHERE)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.pbLearnMove(:ROOST)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 基拉祈
def battleJirachi
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:JIRACHI,30)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:DOOMDESIRE)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:MOONBLAST)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# MEGA达克莱伊
def battleMegaDarkrai
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:DARKRAI,170)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 5
  pkmn.setItem(:DARKRAINITE)
  pkmn.setNature(:TIMID)
  pkmn.pbLearnMove(:DARKVOID)
  pkmn.pbLearnMove(:DREAMEATER)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 达克莱伊
def battleDarkrai
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:DARKRAI, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:BLACKGLASSES)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:DARKVOID)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 美梦神
def battleCresselia
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CRESSELIA, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:MOONLIGHT)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:ICEBEAM)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 凯路迪欧
def battleKeldeo
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:KELDEO,100)
  pkmn.form = 0
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:MYSTICWATER)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SECRETSWORD)
  pkmn.pbLearnMove(:HYDROPUMP)
  pkmn.pbLearnMove(:ICEBEAM)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 毕力吉翁
def battleVirizion
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:VIRIZION,140)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:MIRACLESEED)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:LEAFBLADE)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:STONEEDGE)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 代拉基翁
def battleTerrakion
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TERRAKION, 140)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:HARDSTONE)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:STONEEDGE)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 勾帕路翁
def battleCobalion
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:COBALION,140)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:METALCOAT)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:IRONHEAD)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:STONEEDGE)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 雷公
def battleRaikou
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:RAIKOU,70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:FOCUSSASH)
  pkmn.setNature(:TIMID)
  pkmn.pbLearnMove(:THUNDER)        # 打雷
  pkmn.pbLearnMove(:EXTRASENSORY)   # 神通力
  pkmn.pbLearnMove(:CALMMIND)       # 冥想
  pkmn.pbLearnMove(:EXTREMESPEED)   # 神速
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 炎帝
def battleEntei
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ENTEI,70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:FOCUSSASH)
  pkmn.setNature(:ADAMANT)
  pkmn.pbLearnMove(:SACREDFIRE)     # 圣火
  pkmn.pbLearnMove(:EXTREMESPEED)   # 神速
  pkmn.pbLearnMove(:STONEEDGE)      # 尖石攻击
  pkmn.pbLearnMove(:ERUPTION)       # 喷火
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 水君
def battleSuicune
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SUICUNE,70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:FOCUSSASH)
  pkmn.setNature(:BOLD)
  pkmn.pbLearnMove(:HYDROPUMP)      # 水炮
  pkmn.pbLearnMove(:SHEERCOLD)      # 绝对零度
  pkmn.pbLearnMove(:MIRRORCOAT)     # 镜面反射
  pkmn.pbLearnMove(:CALMMIND)       # 冥想
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 哲尔尼亚斯
def battleXerneas
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:XERNEAS,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:POWERHERB)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:GEOMANCY)
  pkmn.pbLearnMove(:MOONBLAST)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:FOCUSBLAST)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 伊裴尔塔尔
def battleYveltal
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:YVELTAL,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:OBLIVIONWING)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:FOCUSBLAST)
  pkmn.pbLearnMove(:SNARL)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 基格尔德
def battleZygarde
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZYGARDE,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 5
  pkmn.form = 2
  pkmn.setItem(:ZYGARDENITE)
  pkmn.setNature(:ADAMANT)
  pkmn.pbLearnMove(:THOUSANDARROWS)
  pkmn.pbLearnMove(:THOUSANDWAVES)
  pkmn.pbLearnMove(:LANDSWRATH)
  pkmn.pbLearnMove(:COREENFORCER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 雷吉洛克
def battleRegirock
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:REGIROCK,110)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:IMPISH)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:STONEEDGE)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:HAMMERARM)
  pkmn.pbLearnMove(:CURSE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 雷吉艾斯
def battleRegice
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:REGICE,110)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:CALM)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:ICEBEAM)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:FOCUSBLAST)
  pkmn.pbLearnMove(:AMNESIA)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 雷吉斯奇鲁
def battleRegisteel
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:REGISTEEL,110)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:CAREFUL)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:IRONHEAD)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:BODYPRESS)
  pkmn.pbLearnMove(:IRONDEFENSE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 雷吉艾勒奇
def battleRegieleki
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:REGIELEKI,110)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:MAGNET)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:VOLTSWITCH)
  pkmn.pbLearnMove(:ELECTRICTERRAIN)
  pkmn.pbLearnMove(:EXPLOSION)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 雷吉铎拉戈
def battleRegidrago
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:REGIDRAGO,110)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:DRAGONFANG)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:DRAGONENERGY)
  pkmn.pbLearnMove(:HAMMERARM)
  pkmn.pbLearnMove(:CRUNCH)
  pkmn.pbLearnMove(:DRAGONDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 雷吉奇卡斯
def battleRegigigas
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:REGIGIGAS, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 5
  pkmn.setItem(:FOCUSSASH)   
  pkmn.setNature(:ADAMANT) 
  pkmn.pbLearnMove(:CRUSHGRIP)   
  pkmn.pbLearnMove(:DRAINPUNCH)  
  pkmn.pbLearnMove(:EARTHQUAKE)  
  pkmn.pbLearnMove(:ZENHEADBUTT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 固拉多
def battleGroudon
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GROUDON,120)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:REDORB)
  pkmn.setItem(:REDORB)
  pkmn.setNature(:ADAMANT)
  pkmn.pbLearnMove(:PRECIPICEBLADES) 
  pkmn.pbLearnMove(:FIREPUNCH)
  pkmn.pbLearnMove(:DRAGONCLAW)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 盖欧卡
def battleKyogre
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:KYOGRE,120)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:DARKRAINITE)
  pkmn.setItem(:BLUEORB)
  pkmn.setNature(:MODEST)
  pkmn.pbLearnMove(:ORIGINPULSE)  
  pkmn.pbLearnMove(:WATERSPOUT)  
  pkmn.pbLearnMove(:ICEBEAM)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 超梦y
def battleMewtwo
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MEWTWO,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 5
  pkmn.setItem(:MEWTWONITEY) 
  pkmn.setNature(:TIMID)
  pkmn.pbLearnMove(:PSYSTRIKE) 
  pkmn.pbLearnMove(:CALMMIND)   
  pkmn.pbLearnMove(:SHADOWBALL)   
  pkmn.pbLearnMove(:FOCUSBLAST)     
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 超梦X
def battleMewtwoX
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MEWTWO,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 5
  pkmn.setItem(:MEWTWONITEX)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:ZENHEADBUTT)
  pkmn.pbLearnMove(:DRAINPUNCH)
  pkmn.pbLearnMove(:STONEEDGE)
  pkmn.pbLearnMove(:BULKUP)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 拉帝亚斯 
def battleLatias
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:LATIAS,80)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:LATIASITE)  
  pkmn.setNature(:TIMID) 
  pkmn.pbLearnMove(:MISTBALL)    
  pkmn.pbLearnMove(:DRAGONPULSE) 
  pkmn.pbLearnMove(:CALMMIND)   
  pkmn.pbLearnMove(:RECOVER) 
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 拉帝欧斯
def battleLatios
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:LATIOS,80)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:LATIOSITE) 
  pkmn.setNature(:TIMID) 
  pkmn.pbLearnMove(:LUSTERPURGE) 
  pkmn.pbLearnMove(:DRACOMETEOR)  
  pkmn.pbLearnMove(:PSYCHIC) 
  pkmn.pbLearnMove(:AURASPHERE) 
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

#龙卷云
def battleTornadus
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TORNADUS,70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:FOCUSSASH)
  pkmn.setNature(:TIMID)
  pkmn.pbLearnMove(:BLEAKWINDSTORM) # 枯叶风暴 (专属)
  pkmn.pbLearnMove(:HURRICANE)       # 暴风
  pkmn.pbLearnMove(:HEATWAVE)        # 热风
  pkmn.pbLearnMove(:FOCUSBLAST)      # 气合弹
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

#雷电云
def battleThundurus
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:THUNDURUS,70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:FOCUSSASH)
  pkmn.setNature(:TIMID)
  pkmn.pbLearnMove(:WILDBOLTSTORM) # 鸣雷风暴 (专属)
  pkmn.pbLearnMove(:THUNDERBOLT)    # 十万伏特
  pkmn.pbLearnMove(:PSYCHIC)        # 精神强念
  pkmn.pbLearnMove(:NASTYPLOT)      # 诡计
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end
#土地云
def battleLandorus
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:LANDORUS,70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:FOCUSSASH)
  pkmn.setNature(:ADAMANT)
  pkmn.pbLearnMove(:SANDSEARSTORM) # 热沙风暴 (专属)
  pkmn.pbLearnMove(:EARTHQUAKE)     # 地震
  pkmn.pbLearnMove(:STONEEDGE)      # 尖石攻击
  pkmn.pbLearnMove(:SWORDSDANCE)    # 剑舞
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

#眷恋云
def battleEnamorus
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ENAMORUS,70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:FOCUSSASH)
  pkmn.setNature(:MODEST)
  pkmn.pbLearnMove(:SPRINGTIDESTORM) # 阳春风暴 (专属)
  pkmn.pbLearnMove(:MOONBLAST)        # 月亮之力
  pkmn.pbLearnMove(:EARTHPOWER)       # 大地之力
  pkmn.pbLearnMove(:MYSTICALFIRE)     # 魔法火焰
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 骑拉帝纳-起源形态
def battleGiratina
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GIRATINA,140)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:GRISEOUSORB)  # 白金宝珠
  pkmn.form = 1  # 起源形态
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SHADOWFORCE)
  pkmn.pbLearnMove(:DRAGONPULSE)
  pkmn.pbLearnMove(:AURASPHERE)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end
# 帝牙卢卡
def battleDialga
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:DIALGA,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 6
  pkmn.setItem(:ADAMANTORB)  # 金刚宝珠
  pkmn.form = 1 
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:ROAROFTIME)
  pkmn.pbLearnMove(:FLASHCANNON)
  pkmn.pbLearnMove(:EARTHPOWER)
  pkmn.pbLearnMove(:DRAGONPULSE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 帕路奇犽
def battlePalkia
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:PALKIA,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 6
  pkmn.setItem(:LUSTROUSORB)
  pkmn.form = 1 
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SPACIALREND)
  pkmn.pbLearnMove(:HYDROPUMP)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:DRAGONPULSE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 捷克罗姆
def battleZekrom
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZEKROM,140)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 6
  pkmn.setItem(:DRAGONGEM) 
  pkmn.setNature(:ADAMANT) 
  pkmn.pbLearnMove(:BOLTSTRIKE)  
  pkmn.pbLearnMove(:FUSIONBOLT) 
  pkmn.pbLearnMove(:DRAGONCLAW)
  pkmn.pbLearnMove(:EARTHQUAKE)  
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 莱希拉姆
def battleReshiram
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:RESHIRAM,140)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 6
  pkmn.setItem(:FIREGEM)  
  pkmn.setNature(:MODEST)
  pkmn.pbLearnMove(:BLUEFLARE)    
  pkmn.pbLearnMove(:FUSIONFLARE)  
  pkmn.pbLearnMove(:DRACOMETEOR)
  pkmn.pbLearnMove(:EARTHPOWER)  
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 酋雷姆
def battleKyurem
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:KYUREM,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 6
  pkmn.form = 0 
  pkmn.setItem(:ICEBERRY) 
  pkmn.setNature(:MODEST)
  pkmn.pbLearnMove(:DRAGONPULSE)  
  pkmn.pbLearnMove(:ICEBEAM)      
  pkmn.pbLearnMove(:GLACIATE) 
  pkmn.pbLearnMove(:EARTHPOWER)    
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 花叶蒂-永恒之花
def battleEternalFloette
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:FLOETTE,150)
  pkmn.form = 5
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:FLORGESITE) 
  pkmn.setNature(:MODEST)  
  pkmn.setAbility(0)   
  pkmn.pbLearnMove(:LIGHTOFRUIN)  
  pkmn.pbLearnMove(:MOONBLAST)  
  pkmn.pbLearnMove(:PSYCHIC)    
  pkmn.pbLearnMove(:CALMMIND) 
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 席多蓝恩
def battleHeatran
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:HEATRAN,210)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:HEATRANITE) 
  pkmn.setNature(:MODEST)  
  pkmn.pbLearnMove(:MAGMASTORM)    # 熔岩风暴（专属技能）
  pkmn.pbLearnMove(:FLASHCANNON)   # 加农光炮
  pkmn.pbLearnMove(:EARTHPOWER)    # 大地之力
  pkmn.pbLearnMove(:HIDDENPOWER)   # 觉醒力量
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 捷拉奥拉
def battleZeraora
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZERAORA,170)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:ZERAORANITE)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:PLASMAFISTS)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:VOLTSWITCH)
  pkmn.pbLearnMove(:FAKEOUT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 苍响-百战勇者形态
def battleZacianHero
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZACIAN, 70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LIFEORB)       
  pkmn.setNature(:JOLLY)   
  pkmn.setAbility(0)         
  pkmn.pbLearnMove(:IRONHEAD)   
  pkmn.pbLearnMove(:CLOSECOMBAT) 
  pkmn.pbLearnMove(:CRUNCH)    
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 苍响-剑之王形态
def battleZacianCrowned
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZACIAN, 110)
  pkmn.setItem(:RUSTEDSWORD)
  pkmn.form = 1  
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(1)
  pkmn.pbLearnMove(:BEHEMOTHBLADE)  
  pkmn.pbLearnMove(:SACREDSWORD)  
  pkmn.pbLearnMove(:CRUNCH)
  pkmn.pbLearnMove(:IRONDEFENSE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 藏马然特-百战勇者形态
def battleZamazentaHero
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZAMAZENTA, 70)
  pkmn.form = 0 
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)    
  pkmn.setNature(:IMPISH)    
  pkmn.setAbility(0)        
  pkmn.pbLearnMove(:IRONHEAD)   
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:CRUNCH)    
  pkmn.pbLearnMove(:BODYPRESS)  
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 藏马然特-盾之王形态
def battleZamazentaCrowned
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZAMAZENTA,110)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:RUSTEDSHIELD)  
  pkmn.form = 1 
  pkmn.setNature(:IMPISH)    
  pkmn.setAbility(1)         
  pkmn.pbLearnMove(:BEHEMOTHBASH)
  pkmn.pbLearnMove(:WIDEGUARD)  
  pkmn.pbLearnMove(:CRUNCH)        
  pkmn.pbLearnMove(:KINGSSHIELD)  
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 无极汰那-普通形态
def battleEternatus
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ETERNATUS,100)
  pkmn.form = 0
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LIFEORB)     
  pkmn.setNature(:MODEST) 
  pkmn.setAbility(0)     
  pkmn.pbLearnMove(:DYNAMAXCANNON) 
  pkmn.pbLearnMove(:FLAMETHROWER)   
  pkmn.pbLearnMove(:SLUDGEBOMB)
  pkmn.pbLearnMove(:COSMICPOWER)  
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 无极汰那-无极巨化形态
def battleEternatusEternamax
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ETERNATUS,150)
  pkmn.form = 1 
  pkmn.setItem(:BLACKSLUDGE)     
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 6
  pkmn.setNature(:TIMID)     
  pkmn.setAbility(0)    
  pkmn.pbLearnMove(:DYNAMAXCANNON)      
  pkmn.pbLearnMove(:FLAMETHROWER)     
  pkmn.pbLearnMove(:SLUDGEWAVE)     
  pkmn.pbLearnMove(:ETERNALBEAM)    
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end




# 铁甲将军
def battleArmoredGeneral
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ARMOREDGENERAL,110)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:FOCUSSASH) 
  pkmn.setNature(:ADAMANT) 
  pkmn.setAbility(2)  
  pkmn.pbLearnMove(:IRONHEAD)    
  pkmn.pbLearnMove(:GLACIALLANCE) 
  pkmn.pbLearnMove(:SACREDSWORD)  
  pkmn.pbLearnMove(:SWORDSDANCE)   
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 龙爪兰
def battleOverlordflos
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:OVERLORDFLOS,55)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 2
  pkmn.setItem(:BIGROOT) 
  pkmn.setNature(:ADAMANT)  
  pkmn.pbLearnMove(:POWERWHIP) 
  pkmn.pbLearnMove(:SWORDSDANCE) 
  pkmn.pbLearnMove(:LEECHSEED) 
  pkmn.pbLearnMove(:IRONHEAD) 
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 海之魔物
def battleSeaMonster
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SEAMONSTER, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:FOCUSSASH)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SEADEMONRAIN)
  pkmn.pbLearnMove(:HYDROPUMP)
  pkmn.pbLearnMove(:BLIZZARD)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 地之魔物
def battleGroundMonster
  count = $Trainer.ablePokemonCount
  size = (count >= 2) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GROUNDMONSTER, 180)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:FOCUSSASH)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:EARTHDEMONSWORD)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:GUNKSHOT)
  pkmn.pbLearnMove(:BULKUP)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 璨星之御（BOSS）
def battleValkyrie
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:VALKYRIE, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,0,252,4,0]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:ASTERISM)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:STARLIGHT)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 拂晓之刃（BOSS）
def battleBlackKnight
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:BLACKKNIGHT, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,0,4,252]
  pkmn.battleRank = 4
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SUNSOWRD)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:PSYCHOCUT)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end


# 璨星之御解放
def battleValkyrieLiberation
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:VALKYRIE, 160)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 5
  pkmn.setItem(:BRIGHTSTONE)
  pkmn.form = 1
  pkmn.setNature(:TIMID)
  pkmn.pbLearnMove(:STARLIGHT)
  pkmn.pbLearnMove(:ASTERISM)
  pkmn.pbLearnMove(:MOONBLAST)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 拂晓之刃解放
def battleBlackKnightLiberation
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:BLACKKNIGHT,160)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 5
  pkmn.setItem(:DAWNISTONE)
  pkmn.form = 1
  pkmn.setNature(:ADAMANT)
  pkmn.pbLearnMove(:STARFALL)
  pkmn.pbLearnMove(:NIGHTHIDDEN)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end
# 海星超力霸
def battleStarmieHero
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:STARMIE, 70)
  pkmn.form = 2
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:MYSTICWATER)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:STARSLASH)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:HYDROPUMP)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 查伦-普通形态
def battleCharlen
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CHALLEN, 70)
  pkmn.form = 0
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SHADOWCLAW)
  pkmn.pbLearnMove(:PLAYROUGH)
  pkmn.pbLearnMove(:KNOCKOFF)
  pkmn.pbLearnMove(:WILLOWISP)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 查伦-怨念解放形态 BOSS战
def battleCharleneReleased
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CHALLEN,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,0,0,252]  
  pkmn.battleRank = 4
  pkmn.setItem(:SITRUSBERRY) 
  pkmn.name="怨念秽影"
  pkmn.form = 3  
  pkmn.setNature(:JOLLY)   
  pkmn.setAbility(1)         
  pkmn.pbLearnMove(:SCREAMSOULS)  
  pkmn.pbLearnMove(:PLAYROUGH)     
  pkmn.pbLearnMove(:PSYCHOCUT)  
  pkmn.pbLearnMove(:NIGHTDAZE)   
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 查伦-怨念解放形态（BOSS）
def battleCharleneGrudge
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CHALLEN, 210)
  pkmn.setItem(:CHARLENEITE)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,0,0,252]
  pkmn.battleRank = 6
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(1)
  pkmn.pbLearnMove(:SCREAMSOULS)
  pkmn.pbLearnMove(:PLAYROUGH)
  pkmn.pbLearnMove(:PSYCHOCUT)
  pkmn.pbLearnMove(:NIGHTDAZE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 查伦-零和归终形态（BOSS）
def battleCharleneZero
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CHALLEN, 210)
  pkmn.setItem(:ZEROSUMHEART)
  pkmn.form = 2
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,0,252,4,0]
  pkmn.battleRank = 5
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SUNBEAMG)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 歌
def battleS0misaia
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SOMISAIA, 180)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:HARUKIKAGE)
  pkmn.pbLearnMove(:BOOMBURST)
  pkmn.pbLearnMove(:HYPERVOICE)
  pkmn.pbLearnMove(:REST)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end
# 舞
def battleRhythv0lve
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:RHYTHVOLVE, 180)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:TEETERDANCE)
  pkmn.pbLearnMove(:EMBERDANCE)
  pkmn.pbLearnMove(:REST)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 盖诺赛克特 BOSS战
def battleGENESECT
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GENESECT, 80)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,252,0,0]  
  pkmn.battleRank = 2
  pkmn.setItem(:OCCABERRY) 
  pkmn.setNature(:JOLLY)   
  pkmn.setAbility(1)         
  pkmn.pbLearnMove(:IRONHEAD)  
  pkmn.pbLearnMove(:SHIFTGEAR)     
  pkmn.pbLearnMove(:TECHNOBLAST)  
  pkmn.pbLearnMove(:STRUGGLEBUG)   
  pkmn.makeShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
    $game_switches[196] = false
  return decision==1
end

# 雷煌
def battleVoltstrike
  count = $Trainer.ablePokemonCount
  size = (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:VOLTSTRIKE, 70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:MAGNET)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:THUNDER)
  pkmn.pbLearnMove(:CRUNCH)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.pbLearnMove(:RAINDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 焚影
def battleBlazehowl
  count = $Trainer.ablePokemonCount
  size = (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:BLAZEHOWL, 70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:CHARCOAL)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SACREDFIRE)
  pkmn.pbLearnMove(:EXTREMESPEED)
  pkmn.pbLearnMove(:CRUNCH)
  pkmn.pbLearnMove(:SUNNYDAY)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 沧渊
def battleTiderun
  count = $Trainer.ablePokemonCount
  size = (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TIDERUN, 70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:MYSTICWATER)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SHEERCOLD)
  pkmn.pbLearnMove(:EXTREMESPEED)
  pkmn.pbLearnMove(:CRUNCH)
  pkmn.pbLearnMove(:RAINDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 灾锚鲨龙（BOSS）
def battleCalamityShark
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CALAMITYSHARK, 100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,252,0,0,4,0]
  pkmn.battleRank = 5
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:FISHIOUSREND)
  pkmn.pbLearnMove(:ANCHORSHOT)
  pkmn.pbLearnMove(:IRONHEAD)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 灾锚鲨龙（7倍血量BOSS）
def battleCalamitySharkBoss
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CALAMITYSHARK, 210)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,252,0,0,4,0]
  pkmn.battleRank = 7
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:FISHIOUSREND)
  pkmn.pbLearnMove(:ANCHORSHOT)
  pkmn.pbLearnMove(:IRONHEAD)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  pkmn.totalhp = pkmn.totalhp * 7
  pkmn.hp = pkmn.totalhp
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end
# 音箱蟀侠
def battleMelodicricket
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MELODICRICKET,70)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:METRONOME)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:BOOMBURST)
  pkmn.pbLearnMove(:BUGBUZZ)
  pkmn.pbLearnMove(:SONICSLASH)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 英卡洛斯
def battleMelodicricketLight
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MELODICRICKET,110)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 5
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(1)
  pkmn.pbLearnMove(:GRANDEUR)
  pkmn.pbLearnMove(:BOOMBURST)
  pkmn.pbLearnMove(:SONICSLASH)
  pkmn.pbLearnMove(:SUNSOWRD)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 冥焰之战骑
def battleSujinrakuNether
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SUJINRAKU, 150)
  pkmn.form = 2
  pkmn.name="冥焰之战骑"
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 6
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:GIANTSHADOWNAIL)
  pkmn.pbLearnMove(:OVERHEAT)
  pkmn.pbLearnMove(:ANTIMATTERRAY)
  pkmn.pbLearnMove(:MURKYMIST)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 厉鬼之凄厉
def battleSugardevoirGhostly
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SUGARDEVOIR, 150)
  pkmn.setItem(:LEFTOVERS)
  pkmn.form = 6
  pkmn.name="厉鬼之凄厉"
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,0,0,252,4,252]
  pkmn.battleRank = 5
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:GHOSTLYHOWL)
  pkmn.pbLearnMove(:ANTIMATTERRAY)
  pkmn.pbLearnMove(:FIERYWRATH)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 古简蜗
def battleWoChien
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:WOCHIEN, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:RUINATION)
  pkmn.pbLearnMove(:KNOCKOFF)
  pkmn.pbLearnMove(:FOULPLAY)
  pkmn.pbLearnMove(:LEECHSEED)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 古剑豹
def battleChienPao
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CHIENPAO, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,0,4,252]
  pkmn.battleRank = 4
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:RUINATION)
  pkmn.pbLearnMove(:ICICLECRASH)
  pkmn.pbLearnMove(:SACREDSWORD)
  pkmn.pbLearnMove(:SUCKERPUNCH)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 古鼎鹿
def battleTingLu
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TINGLU, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:IMPISH)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:RUINATION)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:STEALTHROCK)
  pkmn.pbLearnMove(:WHIRLWIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 古玉鱼
def battleChiYu
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CHIYU,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,0,0,252,4,252]
  pkmn.battleRank = 4
  pkmn.setItem(:ASSAULTVEST)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:RUINATION)
  pkmn.pbLearnMove(:FLAMETHROWER)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 奈克洛兹玛-普通形态
def battleNecrozma
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:NECROZMA,110)
  pkmn.form = 0
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:PRISMATICLASER)
  pkmn.pbLearnMove(:PHOTONGEYSER)
  pkmn.pbLearnMove(:DRAGONPULSE)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end


# 奈克洛兹玛-黄昏之鬃（合体索尔迦雷欧）
def battleNecrozmaDuskMane
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:NECROZMA,140)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SUNSTEELSTRIKE)
  pkmn.pbLearnMove(:PHOTONGEYSER)
  pkmn.pbLearnMove(:EARTHQUAKE)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.form = 1
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 奈克洛兹玛-拂晓之翼（合体露奈雅拉）
def battleNecrozmaDawnWings
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:NECROZMA,140)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:MOONGEISTBEAM)
  pkmn.pbLearnMove(:PHOTONGEYSER)
  pkmn.pbLearnMove(:HEATWAVE)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.form = 2
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 奈克洛兹玛-究极奈克洛兹玛
def battleUltraNecrozma
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:NECROZMA,170)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:HASTY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:LIGHTSABER)
  pkmn.pbLearnMove(:PHOTONGEYSER)
  pkmn.pbLearnMove(:DRAGONPULSE)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.form = 3
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end
# 卡璞・鸣鸣
def battleTapuKoko
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TAPUKOKO, 170)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:MAGNET)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:GUARDIANOFALOLA)
  pkmn.pbLearnMove(:THUNDERBOLT)
  pkmn.pbLearnMove(:VOLTSWITCH)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 卡璞・蝶蝶
def battleTapuLele
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TAPULELE, 170)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:TWISTEDSPOON)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:GUARDIANOFALOLA)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:MOONBLAST)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 卡璞・哞哞
def battleTapuBulu
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TAPUBULU, 170)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:MIRACLESEED)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:GUARDIANOFALOLA)
  pkmn.pbLearnMove(:WOODHAMMER)
  pkmn.pbLearnMove(:HORNLEECH)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 卡璞・鳍鳍
def battleTapuFini
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TAPUFINI, 170)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:MYSTICWATER)
  pkmn.setNature(:CALM)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:GUARDIANOFALOLA)
  pkmn.pbLearnMove(:SURF)
  pkmn.pbLearnMove(:MOONBLAST)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end
# 波尔凯尼恩
def battleVolcanion
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:VOLCANION,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:STEAMERUPTION)
  pkmn.pbLearnMove(:FLAMETHROWER)
  pkmn.pbLearnMove(:EARTHPOWER)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 粉碎钻
def battleCarbinkDrill
  setBattleRule("1v1")
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CARBINK, 45)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:IMPISH)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:STEALTHROCK)
  pkmn.pbLearnMove(:MOONBLAST)
  pkmn.pbLearnMove(:BODYPRESS)
  pkmn.pbLearnMove(:IRONDEFENSE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 比克提尼
def battleVictini
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:VICTINI,80)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:VCREATE)
  pkmn.pbLearnMove(:ZENHEADBUTT)
  pkmn.pbLearnMove(:BOLTSTRIKE)
  pkmn.pbLearnMove(:UTURN)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 霏欧纳
def battlePhione
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:PHIONE,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:MYSTICWATER)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SURF)
  pkmn.pbLearnMove(:ICEBEAM)
  pkmn.pbLearnMove(:GRASSKNOT)
  pkmn.pbLearnMove(:RAINDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 谢米-陆上形态
def battleShaymin
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SHAYMIN,80)
  pkmn.form = 0
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:MIRACLESEED)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SEEDFLARE)
  pkmn.pbLearnMove(:AIRSLASH)
  pkmn.pbLearnMove(:EARTHPOWER)
  pkmn.pbLearnMove(:LEECHSEED)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 时拉比
def battleCelebi
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CELEBI,150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 3
  pkmn.setItem(:LUMBERRY)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:LEAFSTORM)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 斯珀洛克
def battleSperlock
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SPERLOCK,210)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 7
  pkmn.setItem(:LUMBERRY)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:PSYCHOBOOST)
  pkmn.pbLearnMove(:DRAGONPULSE)
  pkmn.pbLearnMove(:POWERGEM)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 铁脖颈
def battleIronJugulis
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:IRONJUGULIS,100)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 2
  pkmn.setItem(:BOOSTERENERGY)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:AIRSLASH)
  pkmn.pbLearnMove(:DRAGONPULSE)
  pkmn.pbLearnMove(:HYPERVOICE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 波荡水
def battleWalkingWake
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:WALKINGWAKE, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:HYDROPUMP)
  pkmn.pbLearnMove(:DRAGONPULSE)
  pkmn.pbLearnMove(:FLAMETHROWER)
  pkmn.pbLearnMove(:SUNNYDAY)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 破空焰
def battleGougingFire
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GOUGINGFIRE, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:FLAREBLITZ)
  pkmn.pbLearnMove(:DRAGONCLAW)
  pkmn.pbLearnMove(:RAGINGFURY)
  pkmn.pbLearnMove(:SUNNYDAY)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 猛雷鼓
def battleRagingBolt
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:RAGINGBOLT, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:THUNDER)
  pkmn.pbLearnMove(:DRAGONPULSE)
  pkmn.pbLearnMove(:DRAGONENERGY)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 铁斑叶
def battleIronLeaves
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:IRONLEAVES, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:BOOSTERENERGY)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:PSYBLADE)
  pkmn.pbLearnMove(:LEAFBLADE)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 铁磐岩
def battleIronBoulder
  $game_switches[196] = true  
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:IRONBOULDER, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:BOOSTERENERGY)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:MIGHTYCLEAVE)
  pkmn.pbLearnMove(:STONEEDGE)
  pkmn.pbLearnMove(:PSYCHOCUT)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false 
  return decision==1
end

# 铁头壳
def battleIronCrown
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:IRONCROWN, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:BOOSTERENERGY)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:TACHYONCUTTER)
  pkmn.pbLearnMove(:FLASHCANNON)
  pkmn.pbLearnMove(:PSYSHOCK)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 玛机雅娜（携带Mega石）
def battleMagearna
  if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MAGEARNA, 140)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:MAGEARNAITE)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:FLEURCANNON)
  pkmn.pbLearnMove(:FLASHCANNON)
  pkmn.pbLearnMove(:AURASPHERE)
  pkmn.pbLearnMove(:SHIFTGEAR)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 属性：空
def battleTypeNull
  if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TYPENULL, 140)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:IRONHEAD)
  pkmn.pbLearnMove(:CRUSHCLAW)
  pkmn.pbLearnMove(:TRIPLEKICK)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end



# 故勒顿
def battleKoraidon
if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:KORAIDON, 160)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:COLLISIONCOURSE)
  pkmn.pbLearnMove(:DRAGONCLAW)
  pkmn.pbLearnMove(:FLAREBLITZ)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 密勒顿
def battleMiraidon
if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MIRAIDON, 160)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:ELECTRODRIFT)
  pkmn.pbLearnMove(:DRAGONPULSE)
  pkmn.pbLearnMove(:OVERHEAT)
  pkmn.pbLearnMove(:METALSOUND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 铁武者（闪光）
def battleIronValiant
  if $game_switches[197]
    pbMessage(_INTL("懒狗模式跳过BOSS战并默认胜利。"))
    return true
  end
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:IRONVALIANT,110)
  pkmn.makeShiny
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:BOOSTERENERGY)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:SPIRITBREAK)
  pkmn.pbLearnMove(:PSYCHOCUT)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 飞翔固拉多（20倍血量BOSS）
def battleFlyingGroudon
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GROUDON, 150)
  pkmn.form = 2
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,0,0,252,4]
  pkmn.battleRank = 5
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:PRECIPICEBLADES)
  pkmn.pbLearnMove(:FLY)
  pkmn.pbLearnMove(:FIREBLAST)
  pkmn.pbLearnMove(:BULKUP)
  pkmn.hp = pkmn.totalhp
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 萨戮德-普通形态
def battleZarude
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZARUDE, 150)
  pkmn.form = 0
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:JUNGLEHEALING)
  pkmn.pbLearnMove(:POWERWHIP)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:UTURN)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 萨戮德-阿爸形态
def battleZarudeDada
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZARUDE, 150)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 5
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:JUNGLEHEALING)
  pkmn.pbLearnMove(:POWERWHIP)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:UTURN)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 时拉比（闪光）
def battleCelebiShiny
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CELEBI, 150)
  pkmn.makeShiny
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LUMBERRY)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:LEAFSTORM)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 美录坦
def battleMeltan
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MELTAN, 60)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 2
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:FLASHCANNON)
  pkmn.pbLearnMove(:HEADBUTT)
  pkmn.pbLearnMove(:THUNDERWAVE)
  pkmn.pbLearnMove(:ACIDARMOR)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 大剑鬼-玲珑星的样子（BOSS）
def battleSamurottStar
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SAMUROTT, 150)
  pkmn.form = 2
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,0,4,252]
  pkmn.battleRank = 4
  pkmn.setItem(:MYSTICWATER)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:CEASELESSEDGE)
  pkmn.pbLearnMove(:WARTESTRIKE)
  pkmn.pbLearnMove(:DRAGONDANCE)
  pkmn.pbLearnMove(:SCALESHOT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 苍炎刃鬼-英雄圣殿的样子（BOSS）
def battleCeruledgeHero
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CERULEDGE, 150)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,0,4,252]
  pkmn.battleRank = 4
  pkmn.setItem(:SPELLTAG)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:UNFOUNDED)
  pkmn.pbLearnMove(:BITTERBLADE)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.pbLearnMove(:SHADOWSNEAK)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 布莉姆温-命运之轮的样子（BOSS）
def battleHattereneFate
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:HATTERENE, 150)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,0,252,4,0]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:YINYANGUNITY)
  pkmn.pbLearnMove(:MOONBLAST)
  pkmn.pbLearnMove(:STOREDPOWER)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 路卡亚克
def battleLucayak
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:LUCAYAK, 210)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,0,4,252]
  pkmn.battleRank = 4
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SOULCRUSH)
  pkmn.pbLearnMove(:CLOSECOMBAT)
  pkmn.pbLearnMove(:NIGHTUNENDING)
  pkmn.pbLearnMove(:SWORDSDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 辉霆（BOSS）
def battleGyaradosThunder
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GYARADOS, 210)
  pkmn.form = 2
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,0,252,4,252]
  pkmn.battleRank = 7
  pkmn.setItem(:MAGNET)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:RISINGVOLTAGE)
  pkmn.pbLearnMove(:HYDROPUMP)
  pkmn.pbLearnMove(:HURRICANE)
  pkmn.pbLearnMove(:DRAGONDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end

# 红枫雪域形态（BOSS）
def battleSerperiorMaple
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:SERPERIOR, 210)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,0,252,4,0]
  pkmn.battleRank = 7
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:MAPLESONG)
  pkmn.pbLearnMove(:DRACOMETEOR)
  pkmn.pbLearnMove(:LEAFSTORM)
  pkmn.pbLearnMove(:STRENGTHSAP)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end


# 磷火
def battleTyphlosionPhosphor
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TYPHLOSION, 75)
  pkmn.form = 4
  pkmn.name="磷火兽"
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,0,0,252,4,252]
  pkmn.battleRank = 5
  pkmn.setItem(:CHARCOAL)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:ERUPTION)
  pkmn.pbLearnMove(:FIERYWRATH)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:INFERNALPARADE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 费洛美螂
def battlePheromosa1
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:PHEROMOSA, 170)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,252,0,0,0,252]
  pkmn.battleRank = 6
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:HIGHJUMPKICK)
  pkmn.pbLearnMove(:LUNGE)
  pkmn.pbLearnMove(:BOUNCE)
  pkmn.pbLearnMove(:QUIVERDANCE)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end
# 铁火辉夜
def battleCelesteela1
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CELESTEELA, 170)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0]
  pkmn.battleRank = 6
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:IMPISH)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:HEAVYSLAM)
  pkmn.pbLearnMove(:LEECHSEED)
  pkmn.pbLearnMove(:FLASHCANNON)
  pkmn.pbLearnMove(:PROTECT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 基格尔德-红核心形态
def battleZygardeRedCore
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZYGARDE, 100)
  pkmn.form = 6
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:HYPERBEAM)
  pkmn.pbLearnMove(:SOLARBEAM)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 基格尔德-蓝核心形态
def battleZygardeBlueCore
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZYGARDE, 100)
  pkmn.form = 7
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:HYPERBEAM)
  pkmn.pbLearnMove(:SOLARBEAM)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 基格尔德-细胞形态
def battleZygardeCell
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ZYGARDE, 100)
  pkmn.form = 8
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:HYPERBEAM)
  pkmn.pbLearnMove(:SOLARBEAM)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 妙蛙种子-奇幻森林形态
def battleBulbasaurForest
  setBattleRule("1v1")
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:BULBASAUR, 20)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 2
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:TACKLE)
  pkmn.pbLearnMove(:GROWL)
  pkmn.pbLearnMove(:VINEWHIP)
  pkmn.pbLearnMove(:GROWTH)
  pkmn.makeShiny
  pkmn.makeSuperShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 火稚鸡-奇幻森林形态
def battleTorchicForest
  setBattleRule("1v1")
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TORCHIC, 20)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 2
  pkmn.setItem(:CHARCOAL)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SCRATCH)
  pkmn.pbLearnMove(:GROWL)
  pkmn.pbLearnMove(:EMBER)
  pkmn.pbLearnMove(:QUICKATTACK)
  pkmn.makeShiny
  pkmn.makeSuperShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 木守宫-奇幻森林形态
def battleTreeckoForest
  setBattleRule("1v1")
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:TREECKO, 20)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 2
  pkmn.setItem(:MIRACLESEED)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:POUND)
  pkmn.pbLearnMove(:LEER)
  pkmn.pbLearnMove(:ABSORB)
  pkmn.pbLearnMove(:QUICKATTACK)
  pkmn.makeShiny
  pkmn.makeSuperShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 菊草叶-奇幻森林形态
def battleChikoritaForest
  setBattleRule("1v1")
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CHIKORITA, 22)
  pkmn.form = 2
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 2
  pkmn.setItem(:MIRACLESEED)
  pkmn.setNature(:CALM)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:TACKLE)
  pkmn.pbLearnMove(:GROWL)
  pkmn.pbLearnMove(:RAZORLEAF)
  pkmn.pbLearnMove(:POISONPOWDER)
  pkmn.makeShiny
  pkmn.makeSuperShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 刺刺耳皮丘
def battlePichuSpiky
  setBattleRule("1v1")
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:PICHU, 20)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 2
  pkmn.setItem(:MAGNET)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:THUNDERSHOCK)
  pkmn.pbLearnMove(:CHARM)
  pkmn.pbLearnMove(:TAILWHIP)
  pkmn.pbLearnMove(:SWEETKISS)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 超级悦歌纤纤（BOSS）
def battleChantefleurMega
  $game_switches[196] = true
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:CHANTEFLEUR, 150)
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,0,252,4,0]
  pkmn.battleRank = 6
  pkmn.setItem(:CHANTEFLEURITE)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:MOONBLAST)
  pkmn.pbLearnMove(:ENERGYBALL)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  $game_switches[196] = false
  return decision==1
end


# 超古代胡地
def battleAlakazamAncient
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:ALAKAZAM, 100)
  pkmn.form = 2
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,0,0,252,4,252]
  pkmn.battleRank = 4
  pkmn.setItem(:TWISTEDSPOON)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 超古代胖丁
def battleJigglypuffAncient
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:JIGGLYPUFF, 100)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0]
  pkmn.battleRank = 3
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:HYPERVOICE)
  pkmn.pbLearnMove(:PLAYROUGH)
  pkmn.pbLearnMove(:BODYSLAM)
  pkmn.pbLearnMove(:REST)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 超古代耿鬼
def battleGengarAncient
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:GENGAR, 100)
  pkmn.form = 3
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [0,0,0,252,4,252]
  pkmn.battleRank = 4
  pkmn.setItem(:SPELLTAG)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:SHADOWBALL)
  pkmn.pbLearnMove(:DARKPULSE)
  pkmn.pbLearnMove(:DESTINYBOND)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end


# 狗
def battleOkidogiBoss
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1", size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:OKIDOGI, 100)
  pkmn.makeNotShiny
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LUMBERRY)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:BULKUP)
  pkmn.pbLearnMove(:GUNKSHOT)
  pkmn.pbLearnMove(:DRAINPUNCH)
  pkmn.pbLearnMove(:CRUNCH)
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision == 4
end
# 猴
def battleMunkidoriBoss
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1", size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:MUNKIDORI, 100)
  pkmn.makeNotShiny
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LUMBERRY)
  pkmn.setNature(:TIMID)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.pbLearnMove(:PSYCHIC)
  pkmn.pbLearnMove(:SLUDGEWAVE)
  pkmn.pbLearnMove(:FUTURESIGHT)
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision == 4
end
#鸡
def battleFezandipitiBoss
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1", size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:FEZANDIPITI, 100)
  pkmn.makeNotShiny
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LUMBERRY)
  pkmn.setNature(:JOLLY)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:MOONBLAST)
  pkmn.pbLearnMove(:CROSSPOISON)
  pkmn.pbLearnMove(:ROOST)
  pkmn.pbLearnMove(:TAILWIND)
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision == 4
end
#桃
def battlePecharuntBoss
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1", size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:PECHARUNT, 100)
  pkmn.makeNotShiny
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.battleRank = 4
  pkmn.setItem(:LUMBERRY)
  pkmn.setNature(:BOLD)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:NASTYPLOT)
  pkmn.pbLearnMove(:MALIGNANTCHAIN)
  pkmn.pbLearnMove(:SHADOWBALL)
  pkmn.pbLearnMove(:RECOVER)
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision == 4
end

# 无为觉者
def battleDrampaEnlightened
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:DRAMPA, 150)
  pkmn.form = 2
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,0,252,0,4,0]
  pkmn.battleRank = 5
  pkmn.setItem(:LEFTOVERS)
  pkmn.setNature(:MODEST)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:TRUTHCRASH)
  pkmn.pbLearnMove(:ETERNALNIGHT)
  pkmn.pbLearnMove(:STARFALL)
  pkmn.pbLearnMove(:CALMMIND)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end

# 灾厄咆哮虎
def battleIncineroarCalamity
  count = $Trainer.ablePokemonCount
  size = (count > 2) ? 3 : (count > 1) ? 2 : 1
  setBattleRule(sprintf("%dv1",size))
  setBattleRule("canlose")
  setBattleRule("noexp")
  pkmn = pbGenPkmn(:INCINEROAR, 150)
  pkmn.form = 1
  pkmn.iv = [31,31,31,31,31,31]
  pkmn.ev = [252,252,0,0,4,0]
  pkmn.battleRank = 5
  pkmn.setItem(:LIFEORB)
  pkmn.setNature(:ADAMANT)
  pkmn.setAbility(0)
  pkmn.pbLearnMove(:DARKESTLARIAT)
  pkmn.pbLearnMove(:MALICIOUSMOONSAULT)
  pkmn.pbLearnMove(:FLAREBLITZ)
  pkmn.pbLearnMove(:BULKUP)
  pkmn.makeNotShiny
  pkmn.calcStats
  decision = pbWildBattleCore(pkmn)
  return decision==4
end