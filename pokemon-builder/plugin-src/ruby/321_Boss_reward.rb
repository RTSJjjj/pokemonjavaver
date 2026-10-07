
# 通用Boss奖励
def boss_reward(rank = 5)
  rank = 2 if rank < 2
  rank = 7 if rank > 7
  case rank
  when 2  # 精英怪
    rewards = {
      1 => [[:EXPCANDYS, 10], [:SUPERPOTION, 2], [:BOTTLECAP, 1]],
      2 => [[:EXPCANDYS, 15], [:SUPERPOTION, 2], [:BOTTLECAP, 1]],
      3 => [[:EXPCANDYM, 5], [:FULLRESTORE, 1], [:RARECANDY, 1], [:BOTTLECAP, 1]],
      4 => [[:EXPCANDYM, 10], [:FULLRESTORE, 1], [:RARECANDY, 2], [:BOTTLECAP, 1]],
      5 => [[:EXPCANDYL, 5], [:RARECANDY, 3], [:BOTTLECAP, 2], [:MAXREVIVE, 1]]
    }
    
  when 3  # 初级BOSS（约50级）
    rewards = {
      1 => [[:EXPCANDYL, 8], [:SUPERPOTION, 3], [:RARECANDY, 1], [:BOTTLECAP, 1]],
      2 => [[:EXPCANDYL, 10], [:RARECANDY, 1], [:BOTTLECAP, 1], [:SUPERPOTION, 3]],
      3 => [[:EXPCANDYL, 10], [:RARECANDY, 1], [:BOTTLECAP, 1], [:FULLRESTORE, 1]],
      4 => [[:EXPCANDYM, 15], [:FULLRESTORE, 1], [:RARECANDY, 4], [:BOTTLECAP, 2]],
      5 => [[:EXPCANDYXL, 15], [:RARECANDY, 5], [:BOTTLECAP, 2], [:MAXREVIVE, 2]]
    }
    
  when 4  # 中级BOSS（约75级）
    rewards = {
      1 => [[:EXPCANDYXL, 10], [:SUPERPOTION, 2], [:RARECANDY, 2], [:BOTTLECAP, 2]],
      2 => [[:EXPCANDYM, 10], [:RARECANDY, 2], [:BOTTLECAP, 2], [:SUPERPOTION, 2]],
      3 => [[:EXPCANDYM, 10], [:RARECANDY, 2], [:BOTTLECAP, 2], [:FULLRESTORE, 2]],
      4 => [[:EXPCANDYM, 20], [:FULLRESTORE, 2], [:RARECANDY, 7], [:BOTTLECAP, 3]],
      5 => [[:EXPCANDYXL, 30], [:RARECANDY, 8], [:GOLDBOTTLECAP, 1], [:MAXREVIVE, 3]]
    }
    
  when 5  # 高级BOSS（约100级）
    rewards = {
      1 => [[:EXPCANDYXL, 15], [:FULLRESTORE, 2], [:RARECANDY, 3], [:BOTTLECAP, 3]],
      2 => [[:EXPCANDYXL, 15], [:RARECANDY, 3], [:BOTTLECAP, 3], [:FULLRESTORE, 2]],
      3 => [[:EXPCANDYXL, 20], [:RARECANDY, 3], [:BOTTLECAP, 3], [:FULLRESTORE, 2]],
      4 => [[:EXPCANDYXL, 25], [:FULLRESTORE, 3], [:RARECANDY, 8], [:BOTTLECAP, 4]],
      5 => [[:EXPCANDYXL, 50], [:RARECANDY, 10], [:GOLDBOTTLECAP, 2], [:MAXREVIVE, 4]]
    }
    
  when 6  # 超级BOSS（约150级）
    rewards = {
      1 => [[:EXPCANDYXL, 25], [:FULLRESTORE, 3], [:RARECANDY, 5], [:BOTTLECAP, 5]],
      2 => [[:EXPCANDYXL, 30], [:RARECANDY, 5], [:BOTTLECAP, 5], [:FULLRESTORE, 3]],
      3 => [[:EXPCANDYXL, 30], [:RARECANDY, 5], [:BOTTLECAP, 5], [:FULLRESTORE, 3]],
      4 => [[:EXPCANDYXL, 40], [:FULLRESTORE, 4], [:RARECANDY, 12], [:GOLDBOTTLECAP, 1], [:BOTTLECAP, 4]],
      5 => [[:EXPCANDYXL, 80], [:RARECANDY, 15], [:GOLDBOTTLECAP, 3], [:MAXREVIVE, 6]]
    }
    
  when 7  # 终极BOSS（约200级）
    rewards = {
      1 => [[:EXPCANDYXL, 40], [:FULLRESTORE, 4], [:RARECANDY, 8], [:BOTTLECAP, 8]],
      2 => [[:EXPCANDYXL, 50], [:RARECANDY, 8], [:BOTTLECAP, 8], [:FULLRESTORE, 4]],
      3 => [[:EXPCANDYXL, 50], [:RARECANDY, 8], [:BOTTLECAP, 8], [:FULLRESTORE, 4]],
      4 => [[:EXPCANDYXL, 60], [:FULLRESTORE, 5], [:RARECANDY, 15], [:GOLDBOTTLECAP, 2], [:BOTTLECAP, 6]],
      5 => [[:EXPCANDYXL, 100], [:RARECANDY, 20], [:GOLDBOTTLECAP, 5], [:MAXREVIVE, 8]]
    }
  end
  
  # 随机选择奖励
  reward_set = rewards[rand(1..5)]
  reward_set.each { |item, qty| pbReceiveItem(item, qty) }
  
  # 根据等级显示不同消息
  rank_names = {
    2 => "精英", 3 => "初级BOSS", 4 => "中级BOSS",
    5 => "高级BOSS", 6 => "超级BOSS", 7 => "终极BOSS"
  }
  pbMessage("\\se[ItemGet]击败了#{rank_names[rank]}，获得了奖励道具！")
end

#===============================================================================
# Boss 奖励系统
#===============================================================================
module BossRewards
  
  # 幸福蛋Boss奖励
  def self.blissey
    exp_added = 1
    case rand(1..5)
    when 1
      exp_added = 3000 * 100
    when 2
      exp_added = 3000 * 150
    when 3
      exp_added = 10000 * 100
    when 4
      exp_added = 10000 * 150
    when 5
      exp_added = 30000 * 100
    end
    add_exp_pot(exp_added, true)
  end
  
  # 赛富豪Boss奖励（随机硬币）
  def self.gholdengo
    $gimmighoul_coins = 0 if !$gimmighoul_coins
    case rand(1..5)
    when 1, 2
      coins = 50
      pbMessage("\\se[ItemGet]赛富豪送了你50个索财灵硬币！")
    when 3
      coins = 100
      pbMessage("\\se[ItemGet]赛富豪送了你100个索财灵硬币！")
    when 4
      coins = 150
      pbMessage("\\se[ItemGet]赛富豪送了你150个索财灵硬币！")
    when 5
      coins = 200
      pbMessage("\\se[ItemGet]赛富豪送了你200个索财灵硬币！")
    end
    
    $gimmighoul_coins += coins
    $gimmighoul_coins = 999 if $gimmighoul_coins > 999
    pbMessage("当前拥有索财灵硬币：#{$gimmighoul_coins}/999")
  end
  
  # 赛富豪Boss奖励（随机金钱）
  def self.gholdengo_money
    case rand(1..5)
    when 1, 2
      money = 5000
    when 3
      money = 40000
    when 4
      money = 25000
    when 5
      money = 500000
    end
    
    $Trainer.money += money
    pbMessage("\\se[ItemGet]赛富豪送了你#{money}元钱！")
    pbMessage("当前金钱：#{$Trainer.money}元")
  end
end

module BossRewards

  # ========== 宝可梦奖池（[种族ID, 形态编号]）==========
  
  COMMON_POOL = [
    [:QINGNIAO, 0], [:QINGNIAO, 1],                          # 青鸟 / 灾雀
    [:TREBARK, 0], [:TREBARK, 1], [:TREBARK, 2],             # 憨憨树 / 荆棘树 / 液毒树
    [:CACTUS, 0], [:CACTUS, 1], [:CACTUS, 2],                # 仙人掌 / 有刺 / 毒刺
    [:LEAFFROG, 0], [:LETBURSTDR, 0], [:SAILORPENGU, 0],
    [:GRASSFRUIT, 0], [:ARMOREDSOLDIER, 0], [:STORMEOW, 0],
    [:VOLTCAT, 0], [:BLUNIB, 0], [:XIANRENQIU, 0],
    [:BEAGLE, 0], [:COPPERNAKE, 0], [:FERROHEAD, 0],
    [:TIGERBEAST, 0], [:HAHATREE, 0]
  ]
  
  RARE_POOL = [
    [:GOLDENSEED, 0], [:GOLDENSEED, 1],                      # 金色种子 / 雌性
    [:MANDRAKE, 0], [:MANDRAKE, 1],                          # 曼陀罗 / 雌性
    [:THUNDERMOUSE, 0], [:THUNDERMOUSE, 1],                  # 雷鸣电鼠 / 负电
    [:LEAFSHROOM, 0], [:LEAFSHROOM, 1],                      # 草叶菇 / 钢伞的样子
    [:WHIZ, 0], [:WHIZ, 1],                                  # 飕鸣龙 / 呜飒龙
    [:TREEFROG, 0], [:FIREEATER, 0], [:COUNTPENGU, 0],
    [:DATURA, 0], [:ARMOREDCAPTAIN, 0], [:VOLTEYE, 0],
    [:CANTARUN, 0], [:XIANRENZHANG, 0], [:FERROGIANT, 0],
    [:TIGERCHIEFTAIN, 0], [:CRYINGWOOD, 0], [:YAK, 0],
    [:SQUASHBRO, 0], [:SEAGULL, 0]
  ]
  
  EPIC_POOL = [
    [:VINELADY, 0], [:VINELADY, 1], [:VINELADY, 2],          # 翠萝儿 / 四叶草 / 爱心草
    [:WOLFSPIDER, 0], [:WOLFSPIDER, 1],                      # 狼蛛 / 狼蛛-毒
    [:FLOATDRAGON, 0], [:FLOATDRAGON, 1],                    # 浮游龙 / 鬼龙
    [:WINGEDCICADA, 0], [:WINGEDCICADA, 1],                  # 飞翅蝉 / 脱壳后
    [:NIGHTFAIRY, 0], [:NIGHTFAIRY, 1], [:NIGHTFAIRY, 2],    # 夜精灵 / 黑暗 / 女王
    [:DATURAFLOS, 0], [:OVERLORDFLOS, 0], [:ARISTOCAT, 0],
    [:LIGHTNINGCAT, 0], [:THOUSANDTREE, 0], [:FIERCEAGLE, 0],
    [:CHOCOBO, 0], [:KUNGFUTIGER, 0], [:TIGERWARRIOR, 0],
    [:OCTOPUSBABY, 0], [:POCKETGRASS, 0]
  ]
  
  LEGEND_POOL = [
    [:SHENYUNQUAN, 0], [:SHENYUNQUAN, 1],                    # 神云犬 / 邪云
    [:ARMOREDGENERAL, 0], [:ARMOREDGENERAL, 1], [:ARMOREDGENERAL, 2],  # 铁甲将军 / 双刀 / 刀枪
    [:JINGDOUQUAN, 0], [:JINGDOUQUAN, 1],                    # 竞斗犬 / 绝对零度
    [:REDGOLDKING, 0], [:REDGOLDKING, 1],                    # 赤金蛇 / 赤金蛇后
    [:PLATINUM, 0], [:TIEBIBAWANGSHU, 0], [:ORCALITH, 0],
    [:DREADENDRON, 0], [:GOLDENSLUG, 0], [:WARRIORPENGU, 0],
    [:BLASTER, 0], [:DRAGONFROG, 0], [:DIANCIREN, 0],
    [:STRANGEBEAST, 0], [:TRUESTRANGEBEAST, 0], [:NARVALIS, 0],
    [:GOLDEN, 0], [:IRONARMSNAKE, 0], [:YAKTANK, 0],
    [:FERRODRUN, 0], [:YISHEN, 0]
  ]
  
  # ========== 中文名映射表（含形态）==========
  SPECIES_NAME_MAP = {
    # ---- 普通 ----
    [:QINGNIAO, 0]         => "青鸟",
    [:QINGNIAO, 1]         => "灾雀",
    [:TREBARK, 0]          => "憨憨树",
    [:TREBARK, 1]          => "荆棘树",
    [:TREBARK, 2]          => "液毒树",
    [:CACTUS, 0]           => "仙人掌",
    [:CACTUS, 1]           => "有刺仙人掌",
    [:CACTUS, 2]           => "毒刺仙人掌",
    [:LEAFFROG, 0]         => "叶伞蛙",
    [:LETBURSTDR, 0]       => "小爆龙",
    [:SAILORPENGU, 0]      => "水手企鹅",
    [:GRASSFRUIT, 0]       => "草果",
    [:ARMOREDSOLDIER, 0]   => "铁甲士兵",
    [:STORMEOW, 0]         => "雷云猫",
    [:VOLTCAT, 0]          => "伏特猫",
    [:BLUNIB, 0]           => "小蓝鲸",
    [:XIANRENQIU, 0]       => "仙人球宝宝",
    [:BEAGLE, 0]           => "豆鹰",
    [:COPPERNAKE, 0]       => "赤铜蛇",
    [:FERROHEAD, 0]        => "铁头",
    [:TIGERBEAST, 0]       => "虎兽",
    [:HAHATREE, 0]         => "哈哈树",
    
    # ---- 稀有 ----
    [:GOLDENSEED, 0]       => "金色种子",
    [:GOLDENSEED, 1]       => "金色种子（雌性）",
    [:MANDRAKE, 0]         => "曼陀罗",
    [:MANDRAKE, 1]         => "曼陀罗（雌性）",
    [:THUNDERMOUSE, 0]     => "雷鸣电鼠",
    [:THUNDERMOUSE, 1]     => "雷鸣电鼠（负电）",
    [:LEAFSHROOM, 0]       => "草叶菇",
    [:LEAFSHROOM, 1]       => "草叶菇（钢伞）",
    [:WHIZ, 0]             => "飕鸣龙",
    [:WHIZ, 1]             => "呜飒龙",
    [:TREEFROG, 0]         => "树伞蛙",
    [:FIREEATER, 0]        => "噬火兽",
    [:COUNTPENGU, 0]       => "伯爵企鹅",
    [:DATURA, 0]           => "曼陀罗",
    [:ARMOREDCAPTAIN, 0]   => "铁甲队长",
    [:VOLTEYE, 0]          => "电眼猫",
    [:CANTARUN, 0]         => "巨头鲸",
    [:XIANRENZHANG, 0]     => "仙人掌兽",
    [:FERROGIANT, 0]       => "铁巨人",
    [:TIGERCHIEFTAIN, 0]   => "虎酋长",
    [:CRYINGWOOD, 0]       => "哭泣木灵",
    [:YAK, 0]              => "牦牛",
    [:SQUASHBRO, 0]        => "倭瓜弟弟",
    [:SEAGULL, 0]          => "海鸥",
    
    # ---- 史诗 ----
    [:VINELADY, 0]         => "翠萝儿",
    [:VINELADY, 1]         => "四叶草",
    [:VINELADY, 2]         => "爱心草",
    [:WOLFSPIDER, 0]       => "狼蛛",
    [:WOLFSPIDER, 1]       => "狼蛛（毒）",
    [:FLOATDRAGON, 0]      => "浮游龙",
    [:FLOATDRAGON, 1]      => "鬼龙",
    [:WINGEDCICADA, 0]     => "飞翅蝉",
    [:WINGEDCICADA, 1]     => "飞翅蝉（脱壳后）",
    [:NIGHTFAIRY, 0]       => "夜精灵",
    [:NIGHTFAIRY, 1]       => "夜精灵（黑暗）",
    [:NIGHTFAIRY, 2]       => "夜精灵（女王）",
    [:DATURAFLOS, 0]       => "曼陀罗花",
    [:OVERLORDFLOS, 0]     => "龙爪兰",
    [:ARISTOCAT, 0]        => "绅士猫",
    [:LIGHTNINGCAT, 0]     => "闪电猫",
    [:THOUSANDTREE, 0]     => "千年树",
    [:FIERCEAGLE, 0]       => "烈鹰",
    [:CHOCOBO, 0]          => "陆行鸟",
    [:KUNGFUTIGER, 0]      => "功夫虎",
    [:TIGERWARRIOR, 0]     => "虎勇士",
    [:OCTOPUSBABY, 0]      => "章鱼宝宝",
    [:POCKETGRASS, 0]      => "口袋草",
    
    # ---- 传说 ----
    [:SHENYUNQUAN, 0]      => "神云犬",
    [:SHENYUNQUAN, 1]      => "邪云",
    [:ARMOREDGENERAL, 0]   => "铁甲将军",
    [:ARMOREDGENERAL, 1]   => "双刀将军",
    [:ARMOREDGENERAL, 2]   => "刀枪将军",
    [:JINGDOUQUAN, 0]      => "竞斗犬",
    [:JINGDOUQUAN, 1]      => "绝对零度",
    [:REDGOLDKING, 0]      => "赤金蛇",
    [:REDGOLDKING, 1]      => "赤金蛇后",
    [:PLATINUM, 0]         => "白金龙",
    [:TIEBIBAWANGSHU, 0]   => "铁壁霸王树",
    [:ORCALITH, 0]         => "要塞鲸",
    [:DREADENDRON, 0]      => "古树之王",
    [:GOLDENSLUG, 0]       => "金尖蜗牛",
    [:WARRIORPENGU, 0]     => "武神企鹅",
    [:BLASTER, 0]          => "爆龙兽",
    [:DRAGONFROG, 0]       => "成龙蛙",
    [:DIANCIREN, 0]        => "电磁人",
    [:STRANGEBEAST, 0]     => "奇异兽",
    [:TRUESTRANGEBEAST, 0] => "真奇异兽",
    [:NARVALIS, 0]         => "独角鲸",
    [:GOLDEN, 0]           => "金翼龙",
    [:IRONARMSNAKE, 0]     => "铁甲蛇",
    [:YAKTANK, 0]          => "牦牛坦克",
    [:FERRODRUN, 0]        => "铁巨灵",
    [:YISHEN, 0]           => "翼神"
  }
  
  # ========== 获取中文名 ==========
  def self.species_name(species, form = 0)
    SPECIES_NAME_MAP[[species, form]] || species.to_s
  end
  
  # ========== 随机赠送 1-3 只宝可梦 ==========
  def self.pokemon_reward
    count = rand(1..3)
    
    pbMessage("\\se[ItemGet]击败了BOSS！获得了#{count}只宝可梦！")
    
    count.times do |i|
      roll = rand(1000)
      if roll < 400          # 40% 普通
        species, form = COMMON_POOL.sample
        level = rand(5..15)
        rarity = "普通"
      elsif roll < 700       # 30% 稀有
        species, form = RARE_POOL.sample
        level = rand(10..25)
        rarity = "稀有"
      elsif roll < 900       # 20% 史诗
        species, form = EPIC_POOL.sample
        level = rand(20..40)
        rarity = "史诗"
      else                   # 10% 传说
        species, form = LEGEND_POOL.sample
        level = rand(40..60)
        rarity = "传说"
      end
      
      name = species_name(species, form)
      pbMessage("\\se[ItemGet]【#{rarity}】第#{i+1}只：获得了 #{name} Lv.#{level}")

      # 先创建宝可梦对象
      pkmn = PokeBattle_Pokemon.new(species, level, $Trainer)
      
      # 固定精灵球为「封印球」
      pkmn.ballused = 26
      
      # 设置形态
      if form > 0
        pkmn.form = form
      end
      
      # 再添加到队伍
      pbAddPokemon(pkmn)
    end
  end

end