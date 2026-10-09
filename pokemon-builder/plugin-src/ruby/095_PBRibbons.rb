module PBRibbons
  HOENNCOOL          = 1
  HOENNCOOLSUPER     = 2
  HOENNCOOLHYPER     = 3
  HOENNCOOLMASTER    = 4
  HOENNBEAUTY        = 5
  HOENNBEAUTYSUPER   = 6
  HOENNBEAUTYHYPER   = 7
  HOENNBEAUTYMASTER  = 8
  HOENNCUTE          = 9
  HOENNCUTESUPER     = 10
  HOENNCUTEHYPER     = 11
  HOENNCUTEMASTER    = 12
  HOENNSMART         = 13
  HOENNSMARTSUPER    = 14
  HOENNSMARTHYPER    = 15
  HOENNSMARTMASTER   = 16
  HOENNTOUGH         = 17
  HOENNTOUGHSUPER    = 18
  HOENNTOUGHHYPER    = 19
  HOENNTOUGHMASTER   = 20
  SINNOHCOOL         = 21
  SINNOHCOOLSUPER    = 22
  SINNOHCOOLHYPER    = 23
  SINNOHCOOLMASTER   = 24
  SINNOHBEAUTY       = 25
  SINNOHBEAUTYSUPER  = 26
  SINNOHBEAUTYHYPER  = 27
  SINNOHBEAUTYMASTER = 28
  SINNOHCUTE         = 29
  SINNOHCUTESUPER    = 30
  SINNOHCUTEHYPER    = 31
  SINNOHCUTEMASTER   = 32
  SINNOHSMART        = 33
  SINNOHSMARTSUPER   = 34
  SINNOHSMARTHYPER   = 35
  SINNOHSMARTMASTER  = 36
  SINNOHTOUGH        = 37
  SINNOHTOUGHSUPER   = 38
  SINNOHTOUGHHYPER   = 39
  SINNOHTOUGHMASTER  = 40
  WINNING            = 41
  VICTORY            = 42
  ABILITY            = 43
  GREATABILITY       = 44
  DOUBLEABILITY      = 45
  MULTIABILITY       = 46
  PAIRABILITY        = 47
  WORLDABILITY       = 48
  CHAMPION           = 49
  SINNOHCHAMP        = 50
  RECORD             = 51
  EVENT              = 52
  LEGEND             = 53
  GORGEOUS           = 54
  ROYAL              = 55
  GORGEOUSROYAL      = 56
  ALERT              = 57
  SHOCK              = 58
  DOWNCAST           = 59
  CARELESS           = 60
  RELAX              = 61
  SNOOZE             = 62
  SMILE              = 63
  FOOTPRINT          = 64
  ARTIST             = 65
  EFFORT             = 66
  BIRTHDAY           = 67
  SPECIAL            = 68
  CLASSIC            = 69
  PREMIER            = 70
  SOUVENIR           = 71
  WISHING            = 72
  NATIONAL           = 73
  COUNTRY            = 74
  BATTLECHAMPION     = 75
  REGIONALCHAMPION   = 76
  EARTH              = 77
  WORLD              = 78
  NATIONALCHAMPION   = 79
  WORLDCHAMPION      = 80

  def self.maxValue; 80; end
  def self.getCount; 80; end

  def self.getName(id)
    id = getID(PBRibbons,id)
    names = ["",
       _INTL("酷缎带"),
       _INTL("酷缎带超级"),
       _INTL("酷缎带超极"),
       _INTL("酷缎带大师"),
       _INTL("美丽缎带"),
       _INTL("美丽缎带超级"),
       _INTL("美丽缎带超极"),
       _INTL("美丽缎带大师"),
       _INTL("可爱缎带"),
       _INTL("可爱缎带超级"),
       _INTL("可爱缎带超极"),
       _INTL("可爱缎带大师"),
       _INTL("聪明缎带"),
       _INTL("聪明缎带超级"),
       _INTL("聪明缎带超极"),
       _INTL("聪明缎带大师"),
       _INTL("坚韧缎带"),
       _INTL("坚韧缎带超级"),
       _INTL("坚韧缎带超极"),
       _INTL("坚韧缎带大师"),
       _INTL("酷缎带"),
       _INTL("酷缎带极好"),
       _INTL("酷缎带极致"),
       _INTL("酷缎带大师"),
       _INTL("美丽缎带"),
       _INTL("美丽缎带极好"),
       _INTL("美丽缎带极致"),
       _INTL("美丽缎带大师"),
       _INTL("可爱缎带"),
       _INTL("可爱缎带极好"),
       _INTL("可爱缎带极致"),
       _INTL("可爱缎带大师"),
       _INTL("聪明缎带"),
       _INTL("聪明缎带极好"),
       _INTL("聪明缎带极致"),
       _INTL("聪明缎带大师"),
       _INTL("坚韧缎带"),
       _INTL("坚韧缎带极好"),
       _INTL("坚韧缎带极致"),
       _INTL("坚韧缎带大师"),
       _INTL("获胜缎带"),
       _INTL("胜利缎带"),
       _INTL("能力缎带"),
       _INTL("伟大能力缎带"),
       _INTL("双重能力缎带"),
       _INTL("多重能力缎带"),
       _INTL("双人能力缎带"),
       _INTL("世界能力缎带"),
       _INTL("冠军缎带"),
       _INTL("新奥冠军缎带"),
       _INTL("记录缎带"),
       _INTL("活动缎带"),
       _INTL("传说缎带"),
       _INTL("华丽缎带"),
       _INTL("皇家缎带"),
       _INTL("华丽皇家缎带"),
       _INTL("警觉缎带"),
       _INTL("震惊缎带"),
       _INTL("沮丧缎带"),
       _INTL("粗心缎带"),
       _INTL("放松缎带"),
       _INTL("小憩缎带"),
       _INTL("微笑缎带"),
       _INTL("足迹缎带"),
       _INTL("艺术家缎带"),
       _INTL("努力缎带"),
       _INTL("生日缎带"),
       _INTL("特别缎带"),
       _INTL("经典缎带"),
       _INTL("优先缎带"),
       _INTL("纪念缎带"),
       _INTL("愿望缎带"),
       _INTL("国家缎带"),
       _INTL("国家缎带"),
       _INTL("战斗冠军缎带"),
       _INTL("区域冠军缎带"),
       _INTL("大地缎带"),
       _INTL("世界缎带"),
       _INTL("国家冠军缎带"),
       _INTL("世界冠军缎带")
    ]
    return names[id]
  end

  def self.getDescription(id)
    id = getID(PBRibbons,id)
    desc = ["",
       _INTL("普通酷缎带普通排名获胜者！"),
       _INTL("普通酷缎带超级排名获胜者！"),
       _INTL("普通酷缎带超极排名获胜者！"),
       _INTL("普通酷缎带大师排名获胜者！"),
       _INTL("普通美丽缎带普通排名获胜者！"),
       _INTL("普通美丽缎带超级排名获胜者！"),
       _INTL("普通美丽缎带超极排名获胜者！"),
       _INTL("普通美丽缎带大师排名获胜者！"),
       _INTL("普通可爱缎带普通排名获胜者！"),
       _INTL("普通可爱缎带超级排名获胜者！"),
       _INTL("普通可爱缎带超极排名获胜者！"),
       _INTL("普通可爱缎带大师排名获胜者！"),
       _INTL("普通聪明缎带普通排名获胜者！"),
       _INTL("普通聪明缎带超级排名获胜者！"),
       _INTL("普通聪明缎带超极排名获胜者！"),
       _INTL("普通聪明缎带大师排名获胜者！"),
       _INTL("普通坚韧缎带普通排名获胜者！"),
       _INTL("普通坚韧缎带超级排名获胜者！"),
       _INTL("普通坚韧缎带超极排名获胜者！"),
       _INTL("普通坚韧缎带大师排名获胜者！"),
       _INTL("超级缎带酷类普通排名获胜者！"),
       _INTL("超级缎带酷类极好排名获胜者！"),
       _INTL("超级缎带酷类极致排名获胜者！"),
       _INTL("超级缎带酷类大师排名获胜者！"),
       _INTL("超级缎带美丽类普通排名获胜者！"),
       _INTL("超级缎带美丽类极好排名获胜者！"),
       _INTL("超级缎带美丽类极致排名获胜者！"),
       _INTL("超级缎带美丽类大师排名获胜者！"),
       _INTL("超级缎带可爱类普通排名获胜者！"),
       _INTL("超级缎带可爱类极好排名获胜者！"),
       _INTL("超级缎带可爱类极致排名获胜者！"),
       _INTL("超级缎带可爱类大师排名获胜者！"),
       _INTL("超级缎带聪明类普通排名获胜者！"),
       _INTL("超级缎带聪明类极好排名获胜者！"),
       _INTL("超级缎带聪明类极致排名获胜者！"),
       _INTL("超级缎带聪明类大师排名获胜者！"),
       _INTL("超级缎带坚韧类普通排名获胜者！"),
       _INTL("超级缎带坚韧类极好排名获胜者！"),
       _INTL("超级缎带坚韧类极致排名获胜者！"),
       _INTL("超级缎带坚韧类大师排名获胜者！"),
       _INTL("获得的缎带奖励，来自霍恩战塔Lv.50挑战。"),
       _INTL("获得的缎带奖励，来自霍恩战塔Lv.100挑战。"),
       _INTL("获得的缎带奖励，来自战塔击败塔主。"),
       _INTL("获得的缎带奖励，来自战塔击败塔主。"),
       _INTL("获得的缎带奖励，来自战塔双打挑战。"),
       _INTL("获得的缎带奖励，来自战塔多重挑战。"),
       _INTL("获得的缎带奖励，来自战塔联机多重挑战。"),
       _INTL("获得的缎带奖励，来自Wi-Fi战塔挑战。"),
       _INTL("在另一个区域通过宝可梦联盟并进入名人堂时获得的缎带。"),
       _INTL("击败新奥冠军并进入名人堂时获得的缎带。"),
       _INTL("为创下令人惊叹的记录获得的缎带。"),
       _INTL("宝可梦活动参与缎带。"),
       _INTL("创下传说般记录获得的缎带。"),
       _INTL("一条极其华丽与奢华的缎带。"),
       _INTL("一条极其皇家且充满贵族气息的缎带。"),
       _INTL("一条华丽的皇家缎带，是华丽的巅峰之作。"),
       _INTL("一条回忆振奋人心事件的缎带，充满生命的能量。"),
       _INTL("一条回忆令人激动的事件的缎带，让生活充满兴奋。"),
       _INTL("一条回忆带来悲伤的事件的缎带，为生活增添了风味。"),
       _INTL("一条回忆粗心错误的缎带，帮助调整生活决策。"),
       _INTL("一条回忆清新事件的缎带，为生活增添光彩。"),
       _INTL("一条回忆深沉熟睡的缎带，使生活变得宁静。"),
       _INTL("一条回忆微笑的缎带，丰富了生活的品质。"),
       _INTL("被认为拥有最佳质量足迹的宝可梦获得的缎带。"),
       _INTL("在霍恩担任超级速写模特时获得的缎带。"),
       _INTL("为一位异常努力的工作者颁发的缎带。"),
       _INTL("庆祝生日的缎带。"),
       _INTL("为特殊日子准备的特别缎带。"),
       _INTL("表达对宝可梦的爱的缎带。"),
       _INTL("特别假日缎带。"),
       _INTL("珍惜特殊回忆的缎带。"),
       _INTL("传说愿望成真的缎带。"),
       _INTL("为克服所有艰难挑战所获得的缎带。"),
       _INTL("宝可梦联盟冠军缎带。"),
       _INTL("战斗大赛冠军缎带。"),
       _INTL("宝可梦世界锦标赛区域冠军缎带。"),
       _INTL("获得100场连胜的缎带。"),
       _INTL("宝可梦联盟冠军缎带。"),
       _INTL("宝可梦世界锦标赛国家冠军缎带。"),
       _INTL("宝可梦世界锦标赛世界冠军缎带。")
    ]
    return desc[id]
  end
end
