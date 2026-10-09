#===============================================================================
# DO NOT TOUCH THIS UNDER ANY CIRCUMSTANCES
#===============================================================================
class FollowerEvent < Event
  def trigger(*arg)
    for callback in @callbacks
      ret = callback.call(*arg)
      return ret if ret == true || ret == false
    end
    return -1
  end
end

module Events
  @@OnTalkToFollower = FollowerEvent.new
  def self.OnTalkToFollower;     @@OnTalkToFollower;     end
  def self.OnTalkToFollower=(v); @@OnTalkToFollower = v; end

  @@FollowerRefresh = FollowerEvent.new
  def self.FollowerRefresh;     @@FollowerRefresh;     end
  def self.FollowerRefresh=(v); @@FollowerRefresh = v; end
end

#===============================================================================
# Config Script For Your Game starts Here.
# V  V  V  V  V  V  V  V  V  V  V  V  V  V  V  V  V  V  V  V  V  V  V  V  V  V
#===============================================================================
# Common event that contains "pbTalkToFollower" in  a script command
Follower_Common_Event         = 5

# Animation IDs from followers, change this if you are not using the
# Animations.rxdata provided in the script
Animation_Come_Out = 93
Animation_Come_In = 94
Emo_Happy = 95
Emo_Normal = 96
Emo_Hate = 97
Emo_Poison = 98
Emo_sing = 99
Emo_love = 100

# Allow the player to toggle followers on/off using the Key specified below
ALLOWTOGGLEFOLLOW = true
TOGGLEFOLLOWERKEY = :CTRL

#Status tones to be used, if this is true (Red,Green,Blue,Gray)
APPLYSTATUSTONES = false
BURNTONE = [204,51,51,50]
POISONTONE = [153,102,204,50]
PARALYSISTONE = [255,255,153,50]
FREEZETONE = [153,204,204,50]
SLEEPTONE = [0,0,0,50]

#Follower sprite will always animated while standing still
ALWAYS_ANIMATE  = true

#Regardless of the above setting,the species in this array will always animate
ALWAYS_ANIMATED_FOLLOWERS = [
  # GEN I
  12,15,17,18,22,41,42,49,63,74,81,92,93,109,110,120,121,137,142,144,145,
  146,149,150,151,
  # GEN II
  164,165,166,169,176,187,188,189,193,200,201,207,226,227,233,
  249,250,251,
  # GEN III
  267,269,277,278,279,284,291,292,307,313,314,330,333,334,
  337,338,343,344,351,353,355,358,362,374,375,
  380,381,384,385,
  # GEN IV
  397,398,414,415,416,425,426,429,433,436,437,442,455,458,
  462,468,469,472,474,476,477,478,479,480,481,482,487,488,489,490,491,
  # GEN V
  517,518,520,521,527,528,561,562,563,567,577,578,
  579,581,582,583,584,592,593,605,606,608,609,
  615,628,630,635,637,641,642,643,644,
  # GEN VI
  662,663,666,682,691,703,707,708,714,715,717,719,720,
  # GEN VII
  738,742,743,764,774,781,785,786,787,788,789,790,792,
  793,797,798,800,801,803,804,
  #GEN VIII
  822,823,826,841,845,854,855,873,885,886,887,890,894,895,898
]

#===============================================================================
# These are used to define whether the follower should appear or disappear when
# refreshing it. "next true" will let it stay and "next false" will make it disappear
#===============================================================================
Events.FollowerRefresh += proc{|pkmn|
# The Pokemon disappears if the player is cycling
  next false if $PokemonGlobal.bicycle
# Pokeride Compatibility
  next false if $PokemonGlobal.mount if defined?($PokemonGlobal.mount)
}

Events.FollowerRefresh += proc{|pkmn|
# The Pokemon disappears if the name of the map is Cedolan Gym
  next false if $game_map.name == "Cedolan Gym"
}

Events.FollowerRefresh += proc{|pkmn|
  if $PokemonGlobal.surfing
    next false if pkmn.hasType?(:WATER)
    next false if pkmn.hasType?(:FLYING) || pkmn.hasAbility?(:LEVITATE)
    next true if ALWAYS_ANIMATED_FOLLOWERS.include?(pkmn.species)
    next false
  end
}

Events.FollowerRefresh += proc{|pkmn|
  if $PokemonGlobal.diving
    next true if pkmn.hasType?(:WATER)
    next false
  end
}

Events.FollowerRefresh += proc{|pkmn|
  if pbGetMetadata($game_map.map_id,MetadataOutdoor) != true
# The Pokemon disappears if it's height is greater than 2.5 meters and there are no encounters ie a building or something
    height =  pbGetSpeciesData(pkmn.species,pkmn.form)[SpeciesHeight]
    next false if (height/10.0) > 2.5 && !$PokemonEncounters.isEncounterPossibleHere?
  end
}

# Animate if has Levitate, is a flying type or always animates
Events.FollowerRefresh += proc{|pkmn|
  next true if pkmn.hasType?(:FLYING)
  next true if pkmn.hasAbility?(:LEVITATE)
  next true if ALWAYS_ANIMATED_FOLLOWERS.include?(pkmn.species)
}

#-------------------------------------------------------------------------------
# These are used to define what the Follower will say when spoken to
#-------------------------------------------------------------------------------

# Amie Compatibility
if defined?(pokemonAmieRefresh)
  Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
    cmd = pbMessage("What would you like to do?",["Play","Talk","Cancel"])
    pokemonAmieRefresh if cmd == 0
    next true if [0,2].include?(cmd)
  }
end

Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
# Special Dialogue when statused
  case pkmn.status
  when PBStatuses::POISON
    $scene.spriteset.addUserAnimation(Emo_Poison,x,y)
    pbWait(120)
    pbMessage(_INTL("{1}因为中毒而颤抖...",pkmn.name))
  when PBStatuses::BURN
    $scene.spriteset.addUserAnimation(Emo_Hate,x,y)
    pbWait(70)
    pbMessage(_INTL("{1}因为烧伤在颤抖",pkmn.name))
  when PBStatuses::FROZEN
    $scene.spriteset.addUserAnimation(Emo_Normal,x,y)
    pbWait(100)
    pbMessage(_INTL("{1}被冰封了。",pkmn.name))
  when PBStatuses::SLEEP
    $scene.spriteset.addUserAnimation(Emo_Normal, x, y)
    pbWait(100)
    pbMessage(_INTL("{1}看起来真的很累。",pkmn.name))
  when PBStatuses::PARALYSIS
    $scene.spriteset.addUserAnimation(Emo_Normal,x,y)
    pbWait(100)
    pbMessage(_INTL("{1}站立不动并抽搐。",pkmn.name))
  when PBStatuses::FROSTBITE
    $scene.spriteset.addUserAnimation(Emo_Hate,x,y)
    pbWait(70)
    pbMessage(_INTL("{1}的冻伤看起来很痛。",pkmn.name))
  when PBStatuses::DROWSY
    $scene.spriteset.addUserAnimation(Emo_Normal, x, y)
    pbWait(100)
    pbMessage(_INTL("{1}感到困倦……",pkmn.name))
  end
  next true if pkmn.status != PBStatuses::NONE
}



Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
# Special hold item on a map which includes battle in the name
  if $game_map.name.include?("Battle")
    items=[:POKEBALL,:POKEBALL,:POKEBALL,:GREATBALL,:GREATBALL,:ULTRABALL] # This array can be edited and extended. Look at the one below for a guide
    # Choose a random item from the items array, give the player 2 of the item with the message "{1} is holding a round object..."
    next true if pbPokemonFound(items[rand(items.length)],2,"{1}正拿着一个圆形物体……")
  end
}


Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if $PokemonGlobal.followerHoldItem
    items=[:POTION,:SUPERPOTION,:FULLRESTORE,:REVIVE,:PPUP,
         :PPMAX,:RARECANDY,:REPEL,:MAXREPEL,
         :HONEY,:TINYMUSHROOM,:PEARL,:NUGGET,:GREATBALL,
         :ULTRABALL,:THUNDERSTONE,:MOONSTONE,:SUNSTONE,:DUSKSTONE,
         :REDAPRICORN,:BLUAPRICORN,:YLWAPRICORN,:GRNAPRICORN,:PNKAPRICORN,
         :BLKAPRICORN,:WHTAPRICORN
    ]
    # If no message or quantity is specified the default message is used and the quantity of item is 1
    next true if pbPokemonFound(rand(items.length))
  end
}

Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
# Specific message if the Pokemon is a bug type and the map's name is route 3
  if $game_map.name == "Route 3" && pkmn.hasType?(:BUG)
    $scene.spriteset.addUserAnimation(Emo_sing,x,y)
    pbWait(50)
    messages = [
      "{1}似乎对树木很感兴趣",
      "{1}似乎很喜欢虫宝可梦的嗡嗡声。",
      "{1}在森林里不安地跳来跳去。"
    ]
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Specific message if the map name is Pokemon Lab
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if $game_map.name == "Pokémon Lab"
    $scene.spriteset.addUserAnimation(Emo_Normal,x,y)
    pbWait(100)
    messages = [
      "{1}正在触摸某种开关。",
      "{1}嘴里叼着一根绳子！",
      "{1}似乎想触摸机器。"
    ]
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Specific message if the map name has the players name in it ie the Player's Hpuse
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if $game_map.name.include?($Trainer.name)
    $scene.spriteset.addUserAnimation(Emo_Happy,x,y)
    pbWait(70)
    messages = [
      "{1}正在房间里四处嗅探。",
      "{1}注意到{2}的妈妈在附近。",
      "{1}似乎想在家里安顿下来。"
    ]
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Specific message if the map name has Pokecenter or Pokemon Center
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if $game_map.name.include?("Poké Center") || $game_map.name.include?("Pokémon Center")
    $scene.spriteset.addUserAnimation(Emo_Happy,x,y)
    pbWait(70)
    messages = [
      "{1}很高兴见到护士。",
      "{1}在宝可梦中心看起来更好一些。",
      "{1}似乎对治疗机器很感兴趣。”,",
      "{1}看起来想小睡一会儿。",
      "{1}对护士轻声问候。",
      "{1}正用一种俏皮的目光注视着{2}。",
      "{1}似乎完全自在。",
      "{1}完全放松了。",
      "{1}的脸上有一种满足的表情。"
    ]
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Specific message if the map name has Forest
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if $game_map.name.include?("Forest")
    $scene.spriteset.addUserAnimation(Emo_sing,x,y)
    pbWait(50)
    messages = [
      "{1}似乎对树木非常感兴趣。",
      "{1}似乎喜欢宝可梦的嗡嗡声。",
      "{1}在森林中不安地跳来跳去。",
      "{1}在到处徘徊，聆听不同的声音。",
      "{1}在草地上发呆。",
      "{1}到处奔跑，欣赏森林风光。",
      "{1}在草地上玩耍，拔拔草。",
      "{1}凝视着穿过树木的光。",
      "{1}正在玩一片叶子！",
      "{1}似乎正在听沙沙作响的声音。",
      "{1}一动不动，可能在扮演棵树。",
      "{1}被树枝缠住了，差点摔倒了！",
      "{1}感到惊讶！"
    ]
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Specific message if the map name has Gym in it
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if $game_map.name.include?("Gym")
    $scene.spriteset.addUserAnimation(Emo_Hate,x,y)
    pbWait(70)
    messages = [
      "{1}看起来渴望战斗！",
      "{1}正用坚定的眼神看着{2}。",
      "{1}试图恐吓其他训练家。",
      "{1}相信{2}会提出制胜策略。",
      "{1}正在关注健身房的领导者。",
      "{1}已准备好与某人打架。",
      "{1}看起来可能正在准备一场大决战！",
      "{1}想炫耀它有多强大！",
      "{1}正在……做热身运动？",
      "{1}正在沉思中低声咆哮……"
    ]
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Specific message if the map name has Beach in it
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if $game_map.name.include?("Beach")
    $scene.spriteset.addUserAnimation(Emo_Happy,x,y)
    pbWait(70)
    messages = [
      "{1}似乎很享受风景。",
      "{1}似乎很享受海浪拍打沙滩的声音。",
      "{1}看起来它想游泳！",
      "{1}几乎无法将目光移开海洋。",
      "{1}正渴望地盯着水面。",
      "{1}一直试图将{2}推向水面。",
      "{1}很高兴看到大海！",
      "{1}正在快乐地看海浪！",
      "{1}正在沙滩上玩耍！",
      "{1}正盯着{2}在沙滩上的脚印。",
      "{1}正在沙滩上打滚。"
    ]
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Rain specific message for multiple types
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if [PBFieldWeather::Rain,PBFieldWeather::HeavyRain].include?($game_screen.weather_type)
    if pkmn.hasType?(:FIRE) || pkmn.hasType?(:GROUND) || pkmn.hasType?(:ROCK)
      $scene.spriteset.addUserAnimation(Emo_Hate,x,y)
      pbWait(70)
      messages = [
        "{1}似乎很不舒服。",
        "{1}在发抖……",
        "{1}似乎不喜欢全身湿透……",
        "{1}一直试图让自己变干...",
        "{1}靠近{2}以获得舒适感。",
        "{1}抬头看着天空，皱着眉头。",
        "{1}似乎很难移动它的身体。"
      ]
    elsif pkmn.hasType?(:WATER) || pkmn.hasType?(:GRASS)
      $scene.spriteset.addUserAnimation(Emo_Happy,x,y)
      pbWait(70)
      messages = [
        "{1}似乎很享受天气。",
        "{1}似乎对下雨很高兴！",
        "{1}似乎很惊讶下雨了！",
        "{1}在{2}身边开心地笑了！",
        "{1}正凝视着雨云。",
        "雨滴不断落在{1}头上。",
        "{1}张着嘴抬头仰望。"
      ]
    else
      $scene.spriteset.addUserAnimation(Emo_Normal,x,y)
      pbWait(100)
      messages = [
        "{1}正在仰望天空。",
        "{1}看到下雨看起来有点惊讶。",
        "{1}一直试图让自己变干。",
        "下雨似乎不太打扰{1}。",
        "{1}在水坑里玩！",
        "{1}在水中滑了一下，差点摔倒！"
      ]
    end
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Storm Weather specific message for multiple types
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if PBFieldWeather::Storm == $game_screen.weather_type
    if pkmn.hasType?(:ELECTRIC)
      $scene.spriteset.addUserAnimation(Emo_Happy,x,y)
      pbWait(70)
      messages = [
         "{1}正在仰望天空。",
         "风暴似乎让{1}兴奋不已。",
         "{1}抬头望天，大声喊道！",
         "风暴似乎只是在给 {1} 注入活力！",
         "{1}高兴地跳着跳圈！",
         "闪电根本不会打扰{1}。",
      ]
    else
      $scene.spriteset.addUserAnimation(Emo_Normal,x,y)
      pbWait(100)
      messages = [
        "{1}正在仰望天空。",
        "风暴似乎让{1}有点紧张。",
        "闪电惊了{1}！",
        "下雨似乎不太打扰{1}。",
        "天气似乎让{1}处于紧张状态。",
        "{1}被闪电吓了一跳，依偎在{2}身边！"
      ]
    end
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Snow Weather specific message for multiple types
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if PBFieldWeather::Snow == $game_screen.weather_type
    if pkmn.hasType?(:ICE)
      $scene.spriteset.addUserAnimation(Emo_Happy,x,y)
      pbWait(70)
      messages = [
        "{1}正在看着雪落。",
        "{1}被雪惊呆了！",
        "{1}微笑着仰望天空。",
        "雪似乎让{1}心情愉快。",
        "{1}因为冷而开朗！",
      ]
    else
      $scene.spriteset.addUserAnimation(Emo_Normal,x,y)
      pbWait(100)
      messages = [
        "{1}嘴里叼着雪花。",
        "{1}正在看着雪落。",
        "{1}正在捕捉飘落的雪花。",
        "{1}想在它的嘴里接一片雪花。",
        "{1}被雪迷住了。",
        "{1}的牙齿在打颤！",
        "{1}因为寒冷使身体稍微变小了……"
      ]
    end
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Blizzard Weather specific message for multiple types
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if PBFieldWeather::Blizzard == $game_screen.weather_type
    if pkmn.hasType?(:ICE)
      $scene.spriteset.addUserAnimation(Emo_Happy,x,y)
      pbWait(70)
      messages = [
        "{1}正在观看冰雹。",
        "{1}完全不受冰雹的困扰。",
        "{1}微笑着仰望天空。",
        "冰雹似乎让{1}心情愉快。",
        "{1}正在啃一块冰雹。"
      ]
    else
      $scene.spriteset.addUserAnimation(Emo_Hate,x,y)
      pbWait(70)
      messages = [
        "{1}被冰雹击中！",
        "{1}想避开冰雹。",
        "冰雹正在击中{1}。",
        "{1}看起来不高兴。",
        "{1}像树叶一样颤抖！"
      ]
    end
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Sandstorm Weather specific message for multiple types
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if PBFieldWeather::Sandstorm == $game_screen.weather_type
    if pkmn.hasType?(:ROCK) || pkmn.hasType?(:GROUND)
      $scene.spriteset.addUserAnimation(Emo_Happy,x,y)
      pbWait(70)
      messages = [
        "{1}被沙子覆盖。",
        "天气似乎根本不影响{1}！",
        "沙子似乎不能让{1}慢下来！",
        "{1}正在享受天气。",
      ]
    elsif pkmn.hasType?(:STEEL)
      $scene.spriteset.addUserAnimation(Emo_Normal,x,y)
      pbWait(100)
      messages = [
        "{1}被沙子覆盖，但似乎并不介意。",
        "{1}似乎不受沙尘暴的困扰。",
        "沙子不会减慢{1}的速度。",
        "{1}似乎并不介意天气。",
      ]
    else
      $scene.spriteset.addUserAnimation(Emo_Hate,x,y)
      pbWait(70)
      messages = [
        "{1}被沙子覆盖...",
        "{1}吐了一口沙子！",
        "{1}在沙尘暴中眯着眼睛。",
        "沙子似乎困扰着{1}。"
      ]
    end
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# Sunny Weather specific message for multiple types
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if PBFieldWeather::Sun == $game_screen.weather_type
    if pkmn.hasType?(:GRASS)
      $scene.spriteset.addUserAnimation(Emo_Happy,x,y)
      pbWait(70)
      messages = [
        "{1}似乎很高兴在阳光下。",
        "{1}正在沐浴阳光。",
        "明亮的阳光似乎根本不会打扰{1}。",
        "{1}向空中发射了一团环状孢子云！",
        "{1}伸展着身体，在阳光下放松。",
        "{1}散发出花香。"
      ]
    elsif pkmn.hasType?(:FIRE)
      $scene.spriteset.addUserAnimation(Emo_Happy,x,y)
      pbWait(70)
      messages = [
        "{1}似乎对好天气很高兴！",
        "明亮的阳光似乎根本不会打扰{1}。",
        "{1}看着阳光很激动！",
        "{1}吹出一个火球。",
        "{1}正在吐火！",
        "{1}又热又开朗！"
      ]
    elsif pkmn.hasType?(:DARK)
      $scene.spriteset.addUserAnimation(Emo_Hate,x,y)
      pbWait(70)
      messages = [
        "{1}正在抬头看着天空",
        "{1}似乎感觉被阳光冒犯了。",
        "明媚的阳光似乎困扰着{1}。",
        "{1}出于某种原因看起来很沮丧。",
        "{1}正试图留在{2}的影子中。",
        "{1}一直在寻找避光处。",
      ]
    else
      $scene.spriteset.addUserAnimation(Emo_Normal,x,y)
      pbWait(100)
      messages = [
        "{1}在明媚的阳光下眯着眼睛。",
        "{1}开始出汗了。",
        "{1}在这种天气下似乎有点不舒服。",
        "{1}看起来有点过热。",
        "{1}看起来很热...",
        "{1}挡住了它的视线，挡住了闪烁的光芒！",
       ]
    end
    pbMessage(_INTL(messages[rand(messages.length)],pkmn.name,$Trainer.name))
    next true
  end
}

# All dialogues with the Music Note animation
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if randomVal == 0
    $scene.spriteset.addUserAnimation(Emo_sing,x,y)
    pbWait(50)
    messages = [
      "{1}似乎想和{2}一起玩。",
      "{1}在轻微小声哼哼，似乎是在\n唱歌。",
      "{1}十分高兴的抬头看着{2}。",
      "{1}随其摇摆而跳舞。",
      "{1}正在无忧无虑地跳来跳去！",
      "{1}正在展示其敏捷性！",
      "{1}正在愉快地移动！",
      "哇！{1}在十分快乐的跳舞！！",
      "{1}稳步跟上{2}!",
      "{1}很高兴的跳来跳去。",
      "{1}正在嬉戏地走来走去。",
      "{1}正在嬉戏地抓住{2}的脚。",
      "{1}非常接近{2}！",
      "{1}转过身来，看着{2}。",
      "{1}正在努力炫耀其强大的\n力量！",
      "{1}到处在跑来跑去！",
      "{1}到处游荡欣赏风景。",
      "{1}似乎很喜欢这里！",
      "{1}很高兴！",
      "{1}似乎在唱歌？",
      "{1}正在快乐地跳舞！",
      "{1}跳着活泼的舞蹈十分\n开心！",
      "{1}十分高兴，正在唱歌！",
      "{1}抬起头大叫！",
      "{1}的心情似乎很乐观。",
      "看起来{1}好像在跳舞！",
      "{1}突然开始唱歌！感觉\n很棒。",
      "看来{1}想和{2}跳舞！"
    ]
    value = rand(messages.length)
    case value
    # Special move route to go along with some of the dialogue
    when 3, 9
        pbMoveRoute($game_player,[PBMoveRoute::Wait,65])
        followingMoveRoute([
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,4,
        PBMoveRoute::Jump,0,0,PBMoveRoute::Wait,10,
        PBMoveRoute::TurnUp,PBMoveRoute::Wait,4,
        PBMoveRoute::Jump,0,0,PBMoveRoute::Wait,10,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,4,
        PBMoveRoute::Jump,0,0,PBMoveRoute::Wait,10,
        PBMoveRoute::TurnDown,PBMoveRoute::Wait,4,PBMoveRoute::Jump,0,0])
    when 4, 5
        pbMoveRoute($game_player,[PBMoveRoute::Wait,40])
        followingMoveRoute([
        PBMoveRoute::Jump,0,0,PBMoveRoute::Wait,10,
        PBMoveRoute::Jump,0,0,PBMoveRoute::Wait,10,PBMoveRoute::Jump,0,0])
    when 6, 17
        pbMoveRoute($game_player,[PBMoveRoute::Wait,20])
        followingMoveRoute([
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnDown,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,4,PBMoveRoute::TurnUp])
    when 7, 28
        pbMoveRoute($game_player,[PBMoveRoute::Wait,60])
        followingMoveRoute([
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnUp,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnDown,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnUp,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnDown,PBMoveRoute::Wait,4,
        PBMoveRoute::Jump,0,0,PBMoveRoute::Wait,10,PBMoveRoute::Jump,0,0])
    when 21, 22
        pbMoveRoute($game_player,[PBMoveRoute::Wait,50])
        followingMoveRoute([
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnUp,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnDown,PBMoveRoute::Wait,4,
        PBMoveRoute::Jump,0,0,PBMoveRoute::Wait,10,PBMoveRoute::Jump,0,0])
    end
    pbMessage(_INTL(messages[value],pkmn.name,$Trainer.name))
    next true
  end
}

# All dialogues with the Angry animation
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if randomVal == 1
    $scene.spriteset.addUserAnimation(Emo_Hate,x,y)
    pbWait(70)
    messages = [
      "{1}发出一声怒吼！",
      "{1}做了个生气的表情！",
      "{1}似乎出于某种原因生气了。",
      "{1}踩住了{2}的脚。",
      "{1}把脸凑过来，露出挑衅的表情。",
      "{1}正试图恐吓{2}的敌人！",
      "{1}想挑架！",
      "{1}正在准备战斗！",
      "看起来{1}现在几乎会与任何人战斗！",
      "{1}的咆哮声听起来几乎像说话……"
    ]
    value = rand(messages.length)
    # Special move route to go along with some of the dialogue
    case value
    when 6, 7, 8
      pbMoveRoute($game_player,[PBMoveRoute::Wait,25])
      followingMoveRoute([
        PBMoveRoute::Jump,0,0,PBMoveRoute::Wait,10,PBMoveRoute::Jump,0,0])
    end
    pbMessage(_INTL(messages[value],pkmn.name,$Trainer.name))
    next true
  end
}

# All dialogues with the Neutral Animation
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if randomVal == 2
    $scene.spriteset.addUserAnimation(Emo_Normal,x,y)
    pbWait(100)
    messages = [
      "{1}一直在往下看。",
      "{1}正在四处嗅探。",
      "{1}正在集中注意力。",
      "{1}面对{2}点了点头。",
      "{1}直视{2}的眼睛。",
      "{1}正在调查该区域。",
      "{1}用锐利的目光聚焦！",
      "{1}心不在焉地环顾四周。",
      "{1}打哈欠的声音很大！",
      "{1}正在舒适地放松。",
      "{1}将注意力集中在{2}。",
      "{1}无所事事地盯着周围。",
      "{1}正在集中注意力。",
      "{1}面对{2}点了点头。",
      "{1}正在查看 {2} 的脚印。",
      "{1}似乎想玩，并期待地注视着{2}。",
      "{1}似乎在深入思考某件事。",
      "{1}没有关注{2}... 它似乎在考虑其他事情。",
      "{1}似乎很严肃。",
      "{1}似乎不感兴趣",
      "{1}的心思似乎在别处。",
      "{1}似乎在观察周围环境，而不是看着{2}。",
      "{1}看起来有点无聊。",
      "{1}的表情很严肃。",
      "{1}正盯着远方。",
      "{1}似乎在仔细检查{2}的脸。",
      "{1}似乎试图用它的眼睛交流。",
      "...{1}好像打喷嚏了！",
      "...{1}注意到{2}的鞋子有点脏。",
      "{1}好像吃了什么奇怪的东西，脸色很奇怪……",
      "{1}似乎闻到什么了，看起来很香。",
      "{1}注意到{2}的背包上有一点污垢...",
      "...... ...... ...... ...... ...... ...... ...... ...... ...... ...... ...... {1}默默点头。"
    ]
    value = rand(messages.length)
    # Special move route to go along with some of the dialogue
    case value
    when  1, 5, 7, 20, 21
      pbMoveRoute($game_player,[PBMoveRoute::Wait,35])
      followingMoveRoute([
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,10,
        PBMoveRoute::TurnUp,PBMoveRoute::Wait,10,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,10,
        PBMoveRoute::TurnDown])
    end
    pbMessage(_INTL(messages[value],pkmn.name,$Trainer.name))
    next true
  end
}

# All dialogues with the Happy animation
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if randomVal == 3
    $scene.spriteset.addUserAnimation(Emo_Happy,x,y)
    pbWait(70)
    messages = [
      "{1}开始戳 {2}。",
      "{1}看起来很开心。",
      "{1}高兴地拥抱了{2}。",
      "{1}高兴得停不下来。",
      "{1}看起来想要领先！",
      "{1}很开心。",
      "{1}和{2}一起走路似乎感觉很棒！",
      "{1}健康焕发。",
      "{1}看起来很开心。",
      "{1}为{2}付出了额外的努力！",
      "{1}正在闻周围空气的气味。”",
      "{1}高兴得跳了起来！",
      "{1}仍然感觉很棒！”",
      "{1}伸展着身体，正在放松。",
      "{1}正在尽最大努力跟上 {2}。",
      "{1}很高兴地拥抱 {2}！”",
      "{1}充满活力！",
      "{1}高兴得停不下来！",
      "{1}四处游荡，聆听不同的声音。",
      "{1}给了{2}一个快乐的表情和微笑。",
      "{1}兴奋地开始用鼻子粗鲁地呼吸！",
      "{1}急得发抖！",
      "{1}非常高兴，它开始四处游荡。",
      "{1}看到{2}的关注看起来很兴奋。",
      "{1}似乎很高兴{2}注意到了这一点！",
      "{1}开始兴奋地扭动整个身体！",
      "似乎{1}几乎无法阻止自己拥抱{2}！",
      "{1}靠近{2}的脚。"
    ]
    value = rand(messages.length)
    # Special move route to go along with some of the dialogue
    case value
    when 3
      pbMoveRoute($game_player,[PBMoveRoute::Wait,45])
      followingMoveRoute([
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnUp,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnDown,PBMoveRoute::Wait,4,
        PBMoveRoute::Jump,0,0,PBMoveRoute::Wait,10,PBMoveRoute::Jump,0,0])
    when 11, 16, 17, 24
      pbMoveRoute($game_player,[PBMoveRoute::Wait,40])
      followingMoveRoute([
        PBMoveRoute::Jump,0,0,PBMoveRoute::Wait,10,
        PBMoveRoute::Jump,0,0,PBMoveRoute::Wait,10,PBMoveRoute::Jump,0,0])
    end
    pbMessage(_INTL(messages[value],pkmn.name,$Trainer.name))
    next true
  end
}

# All dialogues with the Heart animation
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if randomVal == 4
    $scene.spriteset.addUserAnimation(Emo_love,x,y)
    pbWait(70)
    messages = [
      "{1}突然开始走近{2}。",
      "哇哦！{1}突然抱住了{2}。",
      "{1}与{2}擦肩而过。",
      "{1}接近{2}.",
      "{1}脸红了。",
      "{1}喜欢和{2}一起度过一天！",
      "{1}突然玩了起来！！",
      "{1}正在摩擦{2}的腿！",
      "{1}对{2}表示崇拜！",
      "{1}似乎想要从{2}那里得到一些夸奖。",
      "{1}似乎希望得到{2}的关注。",
      "{1}与{2}一起旅行似乎很开心。",
      "{1}似乎对{2}深情注视。",
      "{1}正用慈爱的眼睛看着{2}。",
      "{1}看起来想要{2}的款待。",
      "{1}看起来想要{2}的抚摸。",
      "{1}深情地摩擦着{2}。",
      "{1}的头轻轻地撞在了{2}的手上。",
      "{1}翻了个身，期待地看着{2}。",
      "{1}正用信任的眼神看着{2}。",
      "{1}似乎在向{2}求情！",
      "{1}模仿了{2}！"
    ]
    value = rand(messages.length)
    case value
    when 1, 6,
      pbMoveRoute($game_player,[PBMoveRoute::Wait,10])
      followingMoveRoute([
        PBMoveRoute::Jump,0,0])
    end
    pbMessage(_INTL(messages[value],pkmn.name,$Trainer.name))
    next true
  end
}

# All dialogues with no animation
Events.OnTalkToFollower += proc {|pkmn,x,y,randomVal|
  if randomVal == 5
    messages = [
      "{1}对你转了一圈！",
      "{1}发出战斗声。",
      "{1}正在监视中！",
      "{1}耐心地站着。",
      "{1}不安地环顾四周。",
      "{1}在四处游荡。",
      "{1}大声打哈欠！",
      "{1}稳定地站在{2}脚周围的地面上。",
      "{1}看着{2}并可爱的笑着。",
      "{1}凝视着远方。",
      "{1}跟上{2}。",
      "{1}看起来很满意。",
      "{1}装着很强大！",
      "{1}跟随者{2}的步伐。",
      "{1}开始绕圈旋转。",
      "{1}满怀期待地看着{2}。",
      "{1}摔倒了，看上去有些尴尬。",
      "{1}正在等待查看{2}会做什么。",
      "{1}正在默默地观看{2}。",
      "{1}正在寻找{2}的某种提示。",
      "{1}留在原地，等待{2}采取行动。",
      "{1}乖乖地坐在{2}的脚下。",
      "{1}被吓到了一下！",
      "{1}跳了一下！"
    ]
    value = rand(messages.length)
    # Special move route to go along with some of the dialogue
    case value
    when 0
      pbMoveRoute($game_player,[PBMoveRoute::Wait,15])
      followingMoveRoute([
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnUp,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,4,PBMoveRoute::TurnDown])
    when 2,4
      pbMoveRoute($game_player,[PBMoveRoute::Wait,35])
      followingMoveRoute([
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,10,
        PBMoveRoute::TurnUp,PBMoveRoute::Wait,10,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,10,PBMoveRoute::TurnDown])
    when 14
      pbMoveRoute($game_player,[PBMoveRoute::Wait,50])
      followingMoveRoute([
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnUp,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnDown,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnUp,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnDown,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnRight,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnUp,PBMoveRoute::Wait,4,
        PBMoveRoute::TurnLeft,PBMoveRoute::Wait,4,PBMoveRoute::TurnDown])
    when 22, 23
      pbMoveRoute($game_player,[PBMoveRoute::Wait,10])
      followingMoveRoute([
        PBMoveRoute::Jump,0,0])
    end
    pbMessage(_INTL(messages[value],pkmn.name,$Trainer.name))
    next true
  end
}
