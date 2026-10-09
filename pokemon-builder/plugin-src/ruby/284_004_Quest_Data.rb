module QuestModule
  
  # You don't actually need to add any information, but the respective fields in the UI will be blank or "???"
  # I included this here mostly as an example of what not to do, but also to show it's a thing that exists
  #任务表介绍十八个字一行
  Quest0 = {
  
  }
  
  # Here's the simplest example of a single-stage quest with everything specified
  Quest1 = {
    :ID => "1",
    :Name => "【风鸣之章】踏上旅行的第一步",
    :QuestGiver => "无",
    :Stage1 => "出门看看吧。",
    :Stage2 => "去研究所找博士吧！",
    :Location1 => "茶月镇",
    :Location2 => "茶月研究所",
    :QuestDescription => [
   "终于到了可以去冒险的日子了。\n去找博士领取伙伴吧！",
   "去研究所找博士吧！"
    ],
    :RewardString => "初始的伙伴！"
  }
  
  Quest2 = {
    :ID => "2",
    :Name => "【风鸣之章】伊娟博士的委托",
    :QuestGiver => "伊娟博士",
    :Stage1 => "把东西送到朱羽博士手上吧。",
    :Stage2 => "去俱乐部找朱羽博士吧！",
    :Stage3 => "回去告诉伊娟博士吧！",
    :Location1 => "荼蘼镇",
    :Location2 => "荼蘼俱乐部",
    :Location3 => "茶月研究所",
    :QuestDescription =>[
    "受伊娟博士的拜托，要去荼蘼镇给\n朱羽博士送伊娟博士的研究手册。",
    "原来索伽见到过博士，就在中间\n的俱乐部；准备好就前往吧。", 
    "事情结束了，回去交差吧。"
    ],
    :RewardString => "TM69-精神利刃"
  }
  
  Quest3 = {
    :ID => "3",
    :Name => "【风鸣之章】异变森林事件",
    :QuestGiver => "朱羽博士",
    :Stage1 => "从六号道路进森林吧。",
    :Stage2 => "跟上朱羽博士的步伐。",
    :Stage3 => "跟上神秘人！",
    :Location1 => "六号道路",
    :Location2 => "桃苑森林",
    :Location3 => "桃苑森林深处",
    :QuestDescription => "突然来临的事件，没想到森林里面居然\n出事了，快去和博士一起去调查一下吧！",
    :RewardString => "HM01，零花钱+1000"
  }
  
  Quest4 = {
    :ID => "4",
    :Name => "【风鸣之章】首次道馆挑战",
    :QuestGiver => "无",
    :Stage1 => "从森林出去前往茸舒镇吧！",
    :Location1 => "茸舒道馆",
    :QuestDescription => "离这里最近的是茸舒镇道馆，\n先去挑战茸舒镇道馆吧。",
    :RewardString => "零花钱+1000"
  }
  
  Quest5 = {
    :ID => "5",
    :Name => "【风鸣之章】回到茶月镇找伊娟博士",
    :QuestGiver => "茶月镇",
    :Stage1 => "回到茶月镇吧！",
    :Stage2 => "给妈妈看看我的徽章吧！",
    :Location1 => "茶月镇",
    :Location2 => "茶月镇",
    :QuestDescription => "事情都办完了，该回去\n告诉伊娟博士啦！",
    :RewardString => "宝可梦盒"
  }
  
  Quest6 = {
    :ID => "6",
    :Name => "【风鸣之章】继续旅行",
    :QuestGiver => "无",
    :Stage1 => "前往下一个镇子吧。",
    :Location1 => "格诺镇",
    :QuestDescription => "事情都办完了，该继续踏上旅行了。",
    :RewardString => "无"
  }
  
  Quest7 = {
    :ID => "7",
    :Name => "【风鸣之章】目标！择武徽章！",
    :QuestGiver => "无",
    :Stage1 => "前往格斗道馆吧！",
    :Stage2 => "挑战馆主吧！",
    :Location1 => "格诺镇",   
    :Location2 => "格诺森林",
    :QuestDescription => "关于其他的事情就先多留意一下\n吧，接下来是格斗道馆的挑战了！",
     :RewardString => "择武徽章"
  }
  
    Quest8 = {
    :ID => "8",
    :Name => "【风鸣之章】前往下一个镇子",
    :QuestGiver => "无",
    :Stage1 => "前往苜蓿镇。",
    :Location1 => "苜蓿镇",   
    :QuestDescription => "获得了择武徽章，下一个道馆是在苜蓿镇\n的岩石道馆！",
     :RewardString => "无"
   }
  
  Quest9 = {
    :ID => "9",
    :Name => "【风鸣之章】苜蓿镇挑战",
    :QuestGiver => "无",
    :Stage1 => "前往苜蓿镇吧。",
    :Stage2 => "去寻找馆主吧。",
    :Stage3 => "前往道馆吧。",
    :Location1 => "苜蓿镇",
    :Location2 => "苜蓿博物馆",
    :Location3 => "岩石道馆",
    :QuestDescription => "遇见了奇怪的人，先不管他了；\n目前已经获得了两枚徽章了，继\n续加油道馆挑战吧！",
    :RewardString => "无" 
  }
    
   Quest10 = {
    :ID => "10",
    :Name => "【风鸣之章】前往枫弦道馆",
    :QuestGiver => "无",
    :Stage1 => "前往枫弦镇。",
    :Location1 => "枫弦镇",
    :QuestDescription => "继续旅途的路程，获取下一枚徽章吧。",
     :RewardString => "无"
  }
    
   Quest11 = {
    :ID => "11",
    :Name => "【风鸣之章】影宿团的再度现身",
    :QuestGiver => "无",
    :Stage1 => "影宿团踪迹",
    :Location1 => "紫罗洞穴",
    :QuestDescription => "没想到居然碰到了影宿团，\n以防万一还是去看看吧",
    :RewardString => "无"
  }

    Quest12 = {
    :ID => "12",
    :Name => "【风鸣之章】继续冒险，前往枫弦道馆",
    :QuestGiver => "无",
    :Stage1 => "前往枫弦镇。",
    :Stage2 => "挑战馆主吧！",
    :Location1 => "枫弦镇",   
    :Location2 => "枫弦镇道馆",
    :QuestDescription => "影宿团没什么动静是最好的，\n那么就去枫弦镇去找馆主吧！",
     :RewardString => "无"
  }

   Quest13 = {
    :ID => "13",
    :Name => "【风鸣之章】挑战普兰特馆主",
    :QuestGiver => "无",
    :Stage1 => "枫弦镇道馆徽章",
    :Location1 => "枫弦镇道馆",
    :QuestDescription => "原来薇江的哥哥就是枫弦馆主，\n那么正好可以挑战道馆了。",
    :RewardString => "芝梦徽章"
  }
  
   Quest14 = {
    :ID => "14",
    :Name => "【风鸣之章】前往月央市",
    :QuestGiver => "无",
    :Stage1 => "月央市",
    :Stage2 => "找到索伽的朋友",
    :Location1 => "月央道馆",
    :Location2 => "沃绕镇",
    :QuestDescription => "成功获得芝梦徽章，\n乘胜追击前往月央市吧！",
    :RewardString => "无"
  }  
    
   Quest15 = {
    :ID => "15",
    :Name => "【风鸣之章】挑战灰羽馆主",
    :QuestGiver => "无",
    :Stage1 => "月央市",
    :Location1 => "月央道馆",
    :QuestDescription => "虽然和索伽忙了别的事，但是现在结束了，\n是时候挑战道馆了！",
    :RewardString => "拢冶徽章"
  }  
  
    Quest16 = {
    :ID => "16",
    :Name => "【风鸣之章】银月桥事件",
    :QuestGiver => "无",
    :Stage1 => "前往银月桥。",
    :Location1 => "银月桥",
    :QuestDescription => "前往银月桥查看情况吧。",
    :RewardString => "无"
  } 
  
   Quest17 = {
    :ID => "17",
    :Name => "【风鸣之章】隐龙道馆",
    :QuestGiver => "无",
    :Stage1 => "前往隐龙道馆",
    :Location1 => "隐龙市",
    :QuestDescription => "前往隐龙道馆吧！",
    :RewardString => "无"
    }
  
    Quest18 = {
    :ID => "18",
    :Name => "【风鸣之章】影宿团再现",
    :QuestGiver => "无",
    :Stage1 => "前往静隐林",
    :Location1 => "静隐林",
    :QuestDescription => "影宿团怎么又出来了\n快去西边看看吧！",
    :RewardString => "无"
  }
    
    Quest19 = {
    :ID => "19",
    :Name => "【风鸣之章】前往绯雷市！",
    :QuestGiver => "未明",
    :Stage1 => "前往绯雷市！",
    :Location1 => "绯雷市",
    :QuestDescription => "解决了影宿团事件，既然道馆\n徽章不够，那么就先去绯雷市吧！",
    :RewardString => "无"
  }
  
  Quest20 = {
    :ID => "20",
    :Name => "【风鸣之章】弥留之塔",
    :QuestGiver => "无",
    :Stage1 => "前往七号道路的弥留之塔。",
    :Stage2 => "探索塔内状况。",
    :Stage3 => "前往塔顶",
    :Stage4 => "和百合对战吧",
    :Location1 => "七号道路",
    :Location2 => "弥留之塔",
    :Location3 => "弥留之塔5F",
    :Location4 => "弥留之塔5F",
    :QuestDescription => [
    "没想到又碰到事件了...\n难道又是影宿团的成员？还是\n去七号道路确认一下吧。",
    "和冠军一起前往塔里面调查吧。",
    "前往塔顶查看一下。",
    "和百合对战吧！",
        ],
    :RewardString => "MEGA手环x1、御三家进化石x1"
  }
  
    Quest21 = {
    :ID => "21",
    :Name => "【风鸣之章】离开弥留塔，前往绯雷市",
    :QuestGiver => "无",
    :Stage1 => "前往绯雷市！",
    :Location1 => "绯雷市",
    :QuestDescription => "没想到时空裂缝的事情这么严重，\n不过有联盟在处理，目前目标是绯雷市，\n准备好就前往吧。",
    :RewardString => "无"
  }
  
    Quest22 = {
    :ID => "22",
    :Name => "【风鸣之章】前往南晓家",
    :QuestGiver => "无",
    :Stage1 => "查看南晓状态",
    :Stage2 => "前往南晓的家",
    :Location1 => "群岛地区",
    :Location2 => "晴云镇",
    :QuestDescription => "与南晓再度相遇，南晓显得\n有些疲惫，去看看他吧……\n（群岛地区可以从铃兰市乘坐游船前往）",
    :RewardString => "经验护符x1"
  }
    Quest23 = {
    :ID => "23",
    :Name => "【风鸣之章】前往雾绒镇",
    :QuestGiver => "莫尔特",
    :Stage1 => "莫尔特的家",
    :Location1 => "雾绒镇",
    :QuestDescription => "没想到莫尔特家居然也在群岛地区，\n跟他去看看。",
    :RewardString => "闪耀护符x1"
  }
  
   Quest24 = {
    :ID => "24",
    :Name => "【风鸣之章】回到主地区，前往绯雷市",
    :QuestGiver => "无",
    :Stage1 => "前往绯雷市",
    :Stage2 => "前往绯雷星泉",
    :Location1 => "绯雷市",
    :Location2 => "绯雷星泉",
    :QuestDescription => "结束群岛地区的事情，\n回到主地区挑战道馆！",
    :RewardString => "无"
  } 
  
    Quest25 = {
    :ID => "25",
    :Name => "【风鸣之章】挑战芙蕾馆主",
    :QuestGiver => "无",
    :Stage1 => "绯雷市",
    :Location1 => "绯雷道馆",
    :QuestDescription => "了解了一些星泉的历史，目前还是\n先去挑战芙蕾馆主吧。",
    :RewardString => "耀霆徽章"
  } 
  
   Quest26 = {
    :ID => "26",
    :Name => "【风鸣之章】挑战未明馆主",
    :QuestGiver => "无",
    :Stage1 => "隐龙市",
    :Location1 => "隐龙道馆",
    :QuestDescription => "没想到居然知道了影宿团的事情，如果\n能碰到联盟其他人的话和他们说一下，\n现在目标是隐龙道馆。",
    :RewardString => "荒烛徽章"
  }  
     Quest27 = {
    :ID => "27",
    :Name => "【风鸣之章】最后的道馆",
    :QuestGiver => "无",
    :Stage1 => "曦寒镇",
    :Location1 => "曦寒道馆",
    :QuestDescription => "已经七枚徽章了，最后的徽章\n在北边的曦寒道馆。 （从绯雷市东边即可前往）",
    :RewardString => "漪雪徽章"
  }  
  
    Quest28 = {
    :ID => "28",
    :Name => "【风鸣之章】奇怪的博士",
    :QuestGiver => "伊娟博士",
    :Stage1 => "前往朱羽博士的研究所",
    :Location1 => "时风镇研究所",
    :QuestDescription => "神神秘秘的博士，不知道\n是什么东西呢，去看看。",
    :RewardString => "部分MEGA进化石"
  } 
  
  Quest29 = {
    :ID => "29",
    :Name => "【风鸣之章】继续前往曦寒镇",
    :QuestGiver => "无",
    :Stage1 => "继续向着曦寒镇行动",
    :Location1 => "曦寒镇",
    :QuestDescription => "原来莫尔特就是影宿团的霜冻……\n先去曦寒镇吧……",
    :RewardString => "无"
  } 
  
  Quest30 = {
    :ID => "30",
    :Name => "【风鸣之章】挑战墨云馆主",
    :QuestGiver => "无",
    :Stage1 => "曦寒镇",
    :Location1 => "曦寒道馆",
    :QuestDescription => "虽然现在还是有很多没搞明白的事情，\n不过墨云就在道馆等着，\n去进行挑战吧。",
    :RewardString => "漪雪徽章"
  } 
  Quest31 = {
    :ID => "31",
    :Name => "【风鸣之章】最后的挑战……？",
    :QuestGiver => "无",
    :Stage1 => "前往瑞乡镇",
    :Stage2 => "前往柊埃联盟",
    :Stage3 => "前往天空城祭坛",
    :Location1 => "瑞乡镇",   
    :Location2 => "柊埃联盟",
    :Location3 => "天空城祭坛",
    :QuestDescription => [
    "最后的徽章到手，既然目前没什么大事，\n那就前往联盟看看吧！",
    "虽然碰到了索伽，但还是赢了，继续前\n进吧！",
    "前往天空城阻止影宿团夺取霄穹玉！",
    ],
    :RewardString => "无"
  } 
  Quest32 = {
    :ID => "32",
    :Name => "【风鸣之章】前往曦寒山",
    :QuestGiver => "希丝娜",
    :Stage1 => "前往曦寒山",
    :Stage2 => "前往曦寒山顶",
    :Location1 => "曦寒镇",
    :Location2 => "曦寒山顶",
    :QuestDescription => "没想到影宿团居然行动了，\n赶快去曦寒山支援墨云他们吧。",
    :RewardString => "无"
  }
  Quest33 = {
    :ID => "33",
    :Name => "【风鸣之章】影宿团的目的",
    :QuestGiver => "清勉",
    :Stage1 => "前往传召室",
    :Stage2 => "前往花影祭坛",
    :Location1 => "柊埃联盟",
    :Location2 => "花影祭坛",
    :QuestDescription => "坏消息一件接着一件，已经没有\n时间思考了，赶快跟着清勉前往花影祭坛。",
    :RewardString => "无"
  } 
   Quest34 = {
    :ID => "34",
    :Name => "【风鸣之章】时空裂缝",
    :QuestGiver => "无",
    :Stage1 => "找到奉",
    :Stage2 => "前往裂缝边缘",
    :Stage2 => "回到柊埃地区",
    :Location1 => "？？？",
    :Location2 => "裂缝边缘",
    :Location3 => "柊埃地区",
    :QuestDescription => "贸然进到了裂缝，赶紧想办法\n找到奉离开这里",
    :RewardString => "无"
  }  
    Quest35 = {
    :ID => "35",
    :Name => "【风鸣之章】送回瀚溟琉",
    :QuestGiver => "清勉",
    :Stage1 => "前往海底城",
    :Stage2 => "和静海对战",
    :Location1 => "海底遗迹",
    :Location2 => "海底祭坛", 
    :QuestDescription => "事情告一段落，去把守护水晶还给守护者\n吧，之后就可以去联盟了。",
    :RewardString => "部分进化石"
  } 
  
  Quest35_1 = {
    :ID => "35_1",
    :Name => "【风鸣之章】送回霄穹玉",
    :QuestGiver => "清勉",
    :Stage1 => "前往天空城",
    :Stage2 => "和希丝娜对战",
    :Location1 => "天空城",
    :Location2 => "天空祭坛", 
    :QuestDescription => "事情告一段落，去把守护水晶还给守护者\n吧，之后就可以去联盟了。",
    :RewardString => "部分进化石"
  } 
   Quest36 = {
    :ID => "36",
    :Name => "【风鸣之章】挑战联盟",
    :QuestGiver => "\pn",
    :Stage1 => "前往联盟",
    :Stage2 => "挑战联盟",
    :Stage3 => "联盟登记",
    :Location1 => "柊埃联盟",
    :Location2 => "柊埃联盟",
    :Location3 => "冠军殿堂",
    :QuestDescription => "终于尘埃落定，该进行冠军挑战了！",
    :RewardString => "冠军奖杯"
  }
  
  Quest37 = {
    :ID => "37",
    :Name => "【回响之章】新的冒险。",
    :QuestGiver => "清勉",
    :Stage1 => "总之先出门吧",
    :Stage2 => "和南晓索伽对战",
    :Location1 => "茶月镇",
    :Location2 => "一号道路",
    :QuestDescription => "全新旅程即将启程\n不如先出门走走，开启冒险第一步。",
    :RewardString => "无"
  }
  Quest38 = {
    :ID => "38",
    :Name => "【回响之章】再次前往天空城",
    :QuestGiver => "清勉",
    :Stage1 => "前往天空城祭坛",
    :Location1 => "天空城",
    :QuestDescription => "与南晓索伽道别后，是时候前往天\n空城祭坛，与希丝娜会合了。",
    :RewardString => "天空晶石"
  }
  
Quest39 = {
  :ID => "39",
  :Name => "【回响之章】茉克道馆",
  :QuestGiver => "清勉",
  :Stage1 => "与特娅会面",
  :Stage2 => "挑战茉克道馆",
  :Location1 => "天空城",
  :Location2 => "茉克道馆",
  :QuestDescription => "初临天空城，首场挑战即将展开，\n迎战妖精属性道馆馆主——特娅。",
  :RewardString => "月铃徽章"
}
Quest40 = {
  :ID => "40",
  :Name => "【回响之章】厄季斯道馆",
  :QuestGiver => "清勉",
  :Stage1 => "前往厄季斯岛",
  :Stage2 => "探查精灵中心情况",
  :Stage3 => "前往厄季斯道馆",
  :Location1 => "厄季斯岛",
  :Location2 => "精灵中心",
  :Location3 => "厄季斯道馆",
  :QuestDescription => [
   "天空城由数座浮空岛组成，获得首枚徽章\n后，继续向下一座岛出发吧。",
   "奇怪的家伙，还是去精灵中心查看一下吧。",
   "这个人怪怪的，但是不管了，道馆挑战\n是首要！"
    ],
  :RewardString => "冥荼徽章"
}
Quest41 = {
  :ID => "41",
  :Name => "【回响之章】灵诺森道馆",
  :QuestGiver => "无",
  :Stage1 => "前往灵诺森岛",
  :Stage2 => "前往灵诺森博物馆",
  :Stage3 => "在博物馆调查可疑事件",
  :Stage4 => "前往灵诺森道馆",
  :Location1 => "灵诺森岛",
  :Location2 => "灵诺森博物馆",
  :Location3 => "灵诺森博物馆",
  :Location4 => "灵诺森道馆",
  :QuestDescription =>[
  "目标是超能属性的灵诺森道馆，\n前往下一座空岛，展开挑战。",
  "刚说完没意思，没想到就有人要搞事，\n正好和我们的目标一致，前往看一下吧。",
  "调查一下博物馆的人吧",
  "事件告一段落后，是时候向灵诺\n森道馆馆主拉伯克发起挑战了。"
    ],
  :RewardString => "明心徽章"
}
Quest42 = {
  :ID => "42",
  :Name => "【回响之章】绯焰道馆",
  :QuestGiver => "清勉",
  :Stage1 => "前往绯焰岛",
  :Stage2 => "挑战绯焰道馆",
  :Location1 => "绯焰岛",
  :Location2 => "绯焰道馆",
  :QuestDescription => "在表演中被若晴注意到，\n这一次，将在火属性道馆展开激烈对决。",
  :RewardString => "灼灵徽章"
}
Quest43 = {
  :ID => "43",
  :Name => "【回响之章】虹兰道馆",
  :QuestGiver => "清勉",
  :Stage1 => "前往虹兰岛",
  :Stage2 => "挑战虹兰道馆",
  :Location1 => "虹兰岛",
  :Location2 => "虹兰道馆",
  :QuestDescription => "准备挑战安希斯博士掌管的虹兰道馆。\n出发前记得做好万全准备。",
  :RewardString => "浮梦徽章"
}
  
  Quest44 = {
  :ID => "44",
  :Name => "【回响之章】莫尔特的消息",
  :QuestGiver => "莫尔特",
  :Stage1 => "前往泷歧落花田",
  :Stage2 => "前往千夜岛",
  :Location1 => "泷歧落花田",
  :Location2 => "千夜岛",
  :QuestDescription => [
    "坏消息总是接二连三，这次又接到了索伽\n的电话。先去泷歧落花田和南晓会合\n吧。" ,
    "南晓告诉你索伽曾在西边出现，前往西边\n调查，并获得前往千夜岛的情报。"
  ],
  :RewardString => "。"
}
  
  Quest45 = {
  :ID => "45",
  :Name => "【支线】索伽的电话",
  :QuestGiver => "索伽",
  :Stage1 => "前往泷歧落花田",
  :Stage2 => "前往千夜岛",
  :Stage3 => "打倒水舰队",
  :Stage4 => "前往千夜洞穴",
  :Stage5 => "阻止盖欧卡",
  :Location1 => "泷歧落花田",
  :Location2 => "千夜岛",
  :Location3 => "千夜岛",
  :Location4 => "千夜洞穴",
  :Location5 => "千夜隐湖",
  :QuestDescription => [
    "坏消息总是接二连三，这次又接到了索伽\n的电话。先去泷歧落花田和南晓会合\n吧。" ,
    "南晓告诉你索伽曾在西边出现，前往西边\n调查，并获得前往千夜岛的情报。",
    "千夜岛被水舰队封锁了入口，先打倒守在\n门口的小兵吧！",
    "千夜洞穴内传出异动，快去调查他们的据点。",
    "水舰队已经消失无踪，但麻烦并未结束\n——盖欧卡正在苏醒，必须阻止它！"
  ],
  :RewardString => "盖欧卡x1"
}

  
  Quest46 = {
  :ID => "46",
  :Name => "【支线】索伽的电话",
  :QuestGiver => "索伽",
  :Stage1 => "前往泷歧落花田",
  :Stage2 => "前往千夜岛",
  :Stage3 => "打倒水舰队",
  :Stage4 => "前往千夜洞穴",
  :Stage5 => "阻止盖欧卡",
  :Location1 => "泷歧落花田",
  :Location2 => "千夜岛",
  :Location3 => "千夜岛",
  :Location4 => "千夜洞穴",
  :Location5 => "千夜隐湖",
  :QuestDescription => [
    "坏消息总是接二连三，这次又接到了索伽\n的电话。先去泷歧落花田和南晓会合\n吧。" ,
    "南晓告诉你索伽曾在西边出现，前往西边\n调查，并获得前往千夜岛的情报。",
    "千夜岛被水舰队封锁了入口，先打倒守在\n门口的小兵吧！",
    "千夜洞穴内传出异动，快去调查他们的据点。",
    "水舰队已经消失无踪，但麻烦并未结束\n——盖欧卡正在苏醒，必须阻止它！"
  ],
  :RewardString => "盖欧卡x1"
}

 Quest47 = {
  :ID => "47",
  :Name => "【回响之章】落英道馆",
  :QuestGiver => "清勉",
  :Stage1 => "前往落英岛",
  :Stage2 => "挑战落英道馆",
  :Location1 => "落英岛",
  :Location2 => "落英道馆",
  :QuestDescription => "空岛挑战继续，\n下一个目标是被落英花海环绕的落英道馆。",
  :RewardString => "芳英徽章"
}
 Quest48 = {
  :ID => "48",
  :Name => "【回响之章】规盈岛挑战",
  :QuestGiver => "？？？",
  :Stage1 => "前往规盈岛",
  :Location1 => "规盈岛",
  :QuestDescription => "空岛挑战继续，下一所是天空中的海岛，\n距离结束就剩下两座岛屿了！",
  :RewardString => "泠渊徽章"
}
 Quest49 = {
  :ID => "49",
  :Name => "【回响之章】裂缝追踪",
  :QuestGiver => "希斯娜",
  :Stage1 => "调查规盈岛",
  :Stage2 => "追回阿青",
  :Location1 => "规盈岛",
  :Location2 => "裂缝区域",
  :QuestDescription =>  [
  "希丝娜察觉裂缝现象，委托你前往规盈岛\n调查，并追踪失踪的阿青。",
  "在裂缝里面调查一下，追回阿青吧！",
      ],
  :RewardString => "无"
}
 Quest50 = {
  :ID => "50",
  :Name => "【回响之章】规盈道馆",
  :QuestGiver => "米娅",
  :Stage1 => "前往规盈道馆",
  :Location1 => "规盈道馆",
  :QuestDescription => "在阿青的事件告一段落后，是时候前往\n规盈道馆，继续挑战之路了。",
  :RewardString => "泠渊徽章"
}
 Quest51 = {
  :ID => "51",
  :Name => "【回响之章】伊娟博士的消息",
  :QuestGiver => "伊娟",
  :Stage1 => "前往莲心湖",
  :Stage2 => "和生彩对战吧",
  :Location1 => "泷歧落地区→莲心湖",
  :Location2 => "莲心湖",
  :QuestDescription => [
  "没想到又出现奇特的现象了，听说\n在莲心湖，赶紧前往查看一下吧。",
  "看起来这里的异动就是面前的训\n练师了，和她对战一下看看吧。",
    ],
  :RewardString => "？？？"
}

 Quest53 = {
  :ID => "53",
  :Name => "【回响之章】回去报告消息",
  :QuestGiver => "伊娟",
  :Stage1 => "回去和伊娟博士报告吧。",
  :Location1 => "茶月镇→伊娟博士研究所",
  :QuestDescription => "事情告一段落，该回去和\n博士报告一下这次事情了。",
  :RewardString => "？？？"
}
Quest54 = {
  :ID => "54",
  :Name => "【回响之章】最后的道馆",
  :QuestGiver => "清勉",
  :Stage1 => "挑战狱怜道馆",
  :Location1 => "狱怜道馆",
  :QuestDescription => "忙碌的事情告一段落，向天空城最后的道馆，\n狱怜道馆发起最终挑战！",
  :RewardString => "祸尾徽章"
}
  
 Quest55 = {
  :ID => "55",
  :Name => "【回响之章】联盟会议",
  :QuestGiver => "奕",
  :Stage1 => "联盟会议",
  :Location1 => "柊埃联盟会议厅",
  :QuestDescription => "天空道馆试炼圆满结束。清勉召集联盟\n会议，快前往联盟大厅报道。",
  :RewardString => "无"
 }
 Quest56 = {
  :ID => "56",
  :Name => "【回响之章】异界裂缝的引导",
  :QuestGiver => "苍泽",
  :Stage1 => "前往四号道路",
  :Stage2 => "回去报告苍泽",
  :Location1 => "紫罗洞穴",
  :Location2 => "时风镇",
  :QuestDescription => "来到时风镇，遇见了研究异界能量的学者。\n苍泽察觉到你可能与近期能量波动有关，\n请求你的协助。",
  :RewardString => "铁甲将军x1"
 }
  Quest57 = {
  :ID => "57",
  :Name => "【回响之章】异界裂缝的出现",
  :QuestGiver => "苍泽",
  :Stage1 => "前往调查",
  :Stage2 => "回去报告苍泽",
  :Location1 => "静隐路和十五号道路",
  :Location2 => "时风镇",
  :QuestDescription => "静隐路与十五号道路出现裂缝的异象，\n苍泽请你前往实地探查状况。",
  :RewardString => "草果x1、小蓝鲸x1"
 }
  Quest58 = {
  :ID => "58",
  :Name => "【回响之章】朱羽的委托",
  :QuestGiver => "静海",
  :Stage1 => "去找朱羽博士",
  :Location1 => "时风镇研究所",
  :QuestDescription => "苍泽的委托结束了，\n听说朱羽在找你，去看看吧。",
  :RewardString => "道具：群青色宝珠x1"
}
 Quest59 = {
  :ID => "59",
  :Name => "【回响之章】海之低语",
  :QuestGiver => "静海",
  :Stage1 => "前往调查海魔物",
  :Stage2 => "回去报告静海",
  :Location1 => "曦寒山→海啸之间",
  :Location2 => "时风镇",
  :QuestDescription => "静海因守护职责无法前往深海，\n请你代为调查海之魔物的异常现象。",
  :RewardString => "玛娜霏之蛋x1"
}
 Quest60 = {
  :ID => "60",
  :Name => "【回响之章】传说的唤醒",
  :QuestGiver => "山茶",
  :Stage1 => "前往绯雷星泉",
  :Stage2 => "唤醒圣兽",
  :Location1 => "绯雷市→绯雷星泉",
  :Location2 => "星夜长河",
  :QuestDescription => "静海建议向山茶请教传说之石之事。前往\n绯雷星泉，寻求唤醒圣兽的线索。",
  :RewardString => "璨星之御x1、拂晓之刃x1"
 }
  Quest61 = {
  :ID => "61",
  :Name => "【回响之章】花影祭坛的契约",
  :QuestGiver => "清勉",
  :Stage1 => "再次前往花影祭坛",
  :Location1 => "春叶市→花影祭坛",
  :QuestDescription => "清勉拜托你前往花影祭坛，她有事情\n要交托给你。",
  :RewardString => "瑞斯克因x1"
 }
 
   Quest62 = {
  :ID => "62",
  :Name => "【回响之章】短暂的休息",
  :QuestGiver => "清勉",
  :Stage1 => "前往夕墨镇",
  :Stage2 => "追踪沁奏心悦",
  :Stage3 => "完成试炼",
  :Location1 => "泷歧落地区→夕墨镇",
  :Location2 => "歌舞森林→坠落遗迹",
  :Location3 => "坠落遗迹",
  :QuestDescription => [
    "联盟难得给我休了个假，清勉推荐我去泷\n歧落地区的夕墨镇放松一下，不知道在那\n里会有什么有趣的事",
    "沁奏和心悦跑出去了！赶快去追上去看看！",
    "休假失败……和歌舞神对战吧！"
  ],
  :RewardString => "韵进舞x1、声韵仙x1、？？？x1"
 }
 
Quest65 = {
  :ID => "65",
  :Name => "【影辞之章】风信队再现",
  :QuestGiver => "艾琳",
  :Stage1 => "和风信队对战",
  :Stage2 => "追上风信队",
  :Stage3 => "前往洞穴",
  :Stage4 => "阻止波尔凯尼恩",
  :Location1 => "清缘道路",
  :Location2 => "黄沙之地",
  :Location3 => "暮煦山",
  :Location4 => "暮煦山",
  :QuestDescription => [
    "风信队居然又出现了，先和他们正面\n交锋，看看他们到底在打什么算盘！",
    "风信队被打跑了，可是总感觉哪里\n怪怪的……算了，先把艾琳的包拿回来吧！",
    "没想到风信队居然打开了时空裂缝，先\n不管艾琳的奇怪行为了，看这样他们是\n要对暮煦山里面的东西动手，先阻止他\n们吧！",
    "居然要和风信队一起战斗吗，虽然很不\n想接受，但为了暮煦山，先阻止失控的\n波尔凯尼恩吧！",
  ],
  :RewardString => "波尔凯尼恩x1、道具：无限绿宝石x1"
}
Quest66 = {
  :ID => "66",
  :Name => "【影辞之章】白龙之境",
  :QuestGiver => "无",
  :Stage1 => "调查白龙之境",
  :Location1 => "白龙之境",
   :QuestDescription => "突然被传送到这里，这里好像之前的龙\n之栖巢啊，总之先调查一下看看吧……",
  :RewardString => "无"
}

Quest67 = {
  :ID => "67",
  :Name => "【影辞之章】帕底亚来的挑战者",
  :QuestGiver => "无",
  :Stage1 => "前往联盟接受挑战",
  :Stage2 => "前往时风镇研究所",
  :Stage3 => "前往花影祭坛",
  :Location1 => "柊埃联盟",
  :Location2 => "时风镇研究所",
  :Location3 => "花影祭坛",
  :QuestDescription => [
  "休息了七天后总算缓过来了。联盟打来\n电话说帕底亚的四天王指名对战，正好\n活动活动筋骨，去联盟一趟吧。",
  "和帕底亚天王的对战告一段落，该办正\n事了。去时风镇研究所找苍泽，把白龙\n吊坠的异象详细告诉他。",
  "苍泽建议你去花影祭坛找安希斯博士，\n他们正准备进入时空裂缝寻找奉的残魂。\n赶紧过去和他们汇合吧。",
],
  :RewardString => "赤铜蛇x1、奇异兽x1、铁头x1"
}

Quest68 = {
  :ID => "68",
  :Name => "【影辞之章】零区研究所",
  :QuestGiver => "安希斯",
  :Stage1 => "调查研究所中厅",
  :Stage2 => "阻止机械毕力吉翁",
  :Stage3 => "调查研究所西区",
  :Stage4 => "阻止机械代拉基翁",
  :Stage5 => "调查研究区东区",
  :Stage6 => "阻止机械勾帕路翁",
  :Stage7 => "阻止密勒顿",
  :Stage8 => "和弗图博士对战",
  :Location1 => "零区研究所·中厅",
  :Location2 => "零区研究所·中厅",
  :Location3 => "零区研究所·西区",  
  :Location4 => "零区研究所·西区",
  :Location5 => "零区研究所·东区",  
  :Location6 => "零区研究所·后室",
  :Location7 => "零区研究所·后室",
  :Location8 => "零区研究所·后室",
  :QuestDescription => [
    "和安希斯博士穿过时空裂缝，来到了\n废弃研究所的中厅。这里已成废墟，\n四处搜索一下，看看能找到什么线索。",
    "找到了研究员留下的实验记录，这里\n在进行宝可梦的机械化改造。还没等\n深入调查，机械化的毕力吉翁——铁\n斑叶就杀了过来，打倒它！",
    "铁斑叶倒下了，继续前往西区搜索。\n从日记来看，研究所的人似乎打算通\n过机械化实现所谓的「机械飞升」。",
    "代拉基翁——铁磐岩果然也被改造成\n了机械体。打倒它，真相就在前方。",
    "已经阻止了铁磐岩，该好好调查一下\n东区了，看看这里还藏着什么秘密。",
    "勾帕路翁——铁头壳镇守在后室入口\n，打倒它，终点的密勒顿就在里面。",
    "终于见到了密勒顿，它就是这场智械\n危机的元凶。用尽全力打倒它，结束\n这一切。",
    "密勒顿被收服后，来自帕底亚的弗图\n博士突然穿越而来。他自称研究未来\n种，想知道更多的话，就先和他对战。",
  ],
  :RewardString => "铁斑叶x1、属性：空x1、铁磐岩x1、铁头壳x1、密勒顿x1"
}

Quest69 = {
  :ID => "69",
  :Name => "【影辞之章】松铭的对战",
  :QuestGiver => "无",
  :Stage1 => "和松铭对战",
  :Stage2 => "前往桃苑森林",
  :Location1 => "柊埃联盟",
  :Location2 => "桃苑森林",
  :QuestDescription => [
    "刚从传召室出来就遇到了松铭。他听\n说奉的事情后，执意要加入寻找灵魂\n碎片的行列。劝不住他，只好用对战\n来让他死心了。",
    "松铭总算被劝住了。这时阿青带来了\n消息——风信队在桃苑森林出没，似\n乎在寻找什么。赶紧过去阻止他们！",
  ],
  :RewardString => "无"
}

Quest70 = {
  :ID => "70",
  :Name => "【影辞之章】桃苑森林的骚动",
  :QuestGiver => "阿青",
  :Stage1 => "深入桃苑森林",
  :Stage2 => "阻止风信队",
  :Stage3 => "阻止失控的时拉比",
  :Location1 => "桃苑森林",
  :Location2 => "桃苑森林·深处",
  :Location3 => "桃苑森林·深处",
  :QuestDescription => [
    "和阿青一起来到桃苑森林，这里已经\n被风信队的小兵占据了。森林深处传\n来异常的能量反应，赶紧过去看看他\n们到底在搞什么鬼。",
    "一路打到深处，风信队正在试图操控\n时拉比！阻止他们，不能让时拉比落\n入他们手中！",
    "风信队启动了装置，时拉比陷入了失\n控和狂暴状态。得先让它平静下来，\n不能让它继续痛苦下去了。",
  ],
  :RewardString => "时拉比x1"
}

Quest71 = {
  :ID => "71",
  :Name => "【影辞之章】永恒花田的守护者",
  :QuestGiver => "南晓",
  :Stage1 => "深入静隐林",
  :Stage2 => "前往永恒花田",
  :Stage3 => "阻止风信队干部羽佑",
  :Stage4 => "收服失控的永恒之花",
  :Location1 => "静隐林",
  :Location2 => "永恒花田",
  :Location3 => "永恒花田·深处",
  :Location4 => "永恒花田·深处",
  :QuestDescription => [
    "南晓在静隐林发现了风信队的踪迹，\n和阿青一起过去看看。",
    "静隐林的深处是一片美丽的花田，这\n里栖息着许多花叶蒂。花田深处传来\n异常的能量波动，赶紧过去看看。",
    "风信队的干部羽佑正站在永恒之花面\n前。她说自己不是在操控宝可梦，而\n是在照顾它，但不管怎样先阻止她！",
    "羽佑战败后逃走了，但永恒之花确实\n陷入了失控状态。和之前波尔凯尼恩\n一样，先让它平静下来再说。",
  ],
  :RewardString => "永恒之花x1"
}

Quest72 = {
  :ID => "72",
  :Name => "【影辞之章】深流洞穴的纷争",
  :QuestGiver => "墨云",
  :Stage1 => "进入深流洞穴",
  :Stage2 => "追上阿青",
  :Stage3 => "阻止风信队",
  :Location1 => "14号道路·深流洞穴",
  :Location2 => "深流洞穴·海眼洞窟",
  :Location3 => "深流洞穴·海眼洞窟",
  :QuestDescription => [
    "收到消息，幽暗结社在14号道路\n附近出现了！以他们的残忍程度，后\n果不堪设想，赶紧过去阻止他们！",    "静隐林的深处是一片美丽的花田，这\n里栖息着许多花叶蒂。花田深处传来\n异常的能量波动，赶紧过去看看。",
    "幽暗结社和风信队都聚集在深流洞穴\n里，阿青已经先进去了，赶紧跟上！",
    "前往海眼洞穴深处阻止风信队吧！",
  ],
  :RewardString => "无"
}

Quest73 = {
  :ID => "73",
  :Name => "【影辞之章】格诺森林的裂缝",
  :QuestGiver => "洛尔",
  :Stage1 => "前往格诺森林",
  :Stage2 => "阻止徘徊者",
  :Location1 => "格诺森林",
  :Location2 => "格诺森林·格诺森泉",
  :QuestDescription => [
    "相诗来电说格诺森林出现了时空裂缝\n暴动，疑似与时之圣树受干扰有关。\n洛尔已经先过去了，赶紧去支援！",
    "裂缝中涌出了大量徘徊者，武道熊师\n正在苦苦支撑。上前帮忙，先解决掉\n这些徘徊者！",
  ],
  :RewardString => "熊徒弟x1、道具：水之挂轴x1、恶之挂轴x1"
}

Quest74 = {
  :ID => "74",
  :Name => "【影辞之章】过去的回响",
  :QuestGiver => "无",
  :Stage1 => "探索过去的茶月镇",
  :Stage2 => "前往花影祭坛",
  :Location1 => "茶月镇",
  :Location2 => "花影祭坛",
  :QuestDescription => [
    "被那道神秘的门卷入后，竟然来到了\n数年前的茶月镇。先调查一下吧……",
    "离开了茶月镇，前方是花影祭坛……\n可这里似乎和记忆中的不太一样，过\n去看看。",
  ],
  :RewardString => "无"
}

Quest75 = {
  :ID => "75",
  :Name => "【影辞之章】蛮荒地带",
  :QuestGiver => "无",
  :Stage1 => "探索蛮荒地带",
  :Stage2 => "解除三个镇点",
  :Stage3 => "收服故勒顿",
  :Stage4 => "和奥琳博士对战",
  :Location1 => "晦木古林",
  :Location2 => "蛮荒地带·各处",
  :Location3 => "蛮荒地带·烬启之墟",
  :Location5 => "蛮荒地带·烬启之墟",
  :QuestDescription => [
    "穿过花影祭坛的裂缝，来到了环境极\n其恶劣的蛮荒地带。瑞思克因施加了\n庇护，先探索一下这片区域吧。",
    "萨戮德被打倒了。结界有三个镇点，\n分别位于水潭、火山和雷原，全部解\n除后才能继续前进。",
    "三个镇点全部解除，前往最终场地看看吧！",
    "故勒顿被收服后，吊坠中的火凤凰苏\n醒并补全了灵魂碎片。奥琳博士出现\n，想了解更多的话，就先和她对战。",
  ],
  :RewardString => "波荡水x1、破空焰x1、萨戮德x1、猛雷鼓x1、故勒顿x1、火凤凰x1"
}
Quest76 = {
  :ID => "76",
  :Name => "【影辞之章】联盟二次会议",
  :QuestGiver => "清勉",
  :Stage1 => "前往联盟会议室",
  :Location1 => "柊埃联盟",
  :QuestDescription => [
    "刚从裂缝回来，联盟又召开了紧急会议\n。听说风信队和幽暗结社的行动越来越\n频繁，赶紧去会议室吧。",
  ],
  :RewardString => "道具：群青色宝珠x1"
}

Quest77 = {
  :ID => "77",
  :Name => "【影辞之章】地魔物的苏醒",
  :QuestGiver => "纹渊",
  :Stage1 => "前往暮煦山",
  :Stage2 => "深入暮煦山深处",
  :Stage3 => "阻止地魔物",
  :Stage4 => "回联盟报告",
  :Location1 => "暮煦山",
  :Location2 => "暮煦山·地底",
  :Location3 => "暮煦山·最深处",
  :Location4 => "柊埃联盟",
  :QuestDescription => [
    "会议开到一半，暮煦山下突然发生时\n空乱流和剧烈地震！风信队果然对地\n魔物下手了，赶紧和纹渊一起过去！",
    "兵分两路，继续前往深处阻止风信队吧！",
    "艾琳和羽佑也来阻拦，击败她们后赶\n快前往阻止地魔物吧！",
    "地魔物被成功封印，回去向清勉报告吧。",
  ],
  :RewardString => "地之魔物"
}

Quest78 = {
  :ID => "78",
  :Name => "【影辞之章】魂兮归来",
  :QuestGiver => "清勉",
  :Stage1 => "前往时风镇研究所",
  :Location1 => "时风镇研究所-苍泽",
  :QuestDescription => [
    "地魔物事件结束后，奉的灵魂收集率\n已达99%。前往研究所，是时候唤醒\n奉了。",
  ],
  :RewardString => "无"
}

  Quest200 = {
    :ID => "200",
    :Name => "【支线】帮助少女购买熏香",
    :QuestGiver => "月央市少女",
    :Stage1 => "帮助少女购买熏香",
    :Location1 => "铃兰市",
    :QuestDescription => "帮助少女去铃兰市买一个熏香\n（铃兰市就在月央市南方。）",
    :RewardString => "TR16-水之波动"
  }
  
  Quest201 = {
    :ID => "201",
    :Name => "【支线】查询歌声的来源",
    :QuestGiver => "伊末镇少年",
    :Stage1 => "前往海岛吧。",
    :Stage2 => "回去报告吧！",
    :Location1 => "无名海岛",
    :Location2 => "伊末镇",
    :QuestDescription => "前往海岛上去探索歌声的来源吧！",
    :RewardString => "TM83-贝壳刃"
  }
  
    Quest202 = {
    :ID => "202",
    :Name => "【支线】无限之笛",
    :QuestGiver => "佩德",
    :Stage1 => "前往曦寒山的洞穴",
    :Stage2 => "把水晶球带回去",
    :Location1 => "曦寒山",
    :Location2 => "曦寒镇",
    :QuestDescription => "去曦寒山深处找寻水晶球\n（任务结束后可获得飞空道具）",
    :RewardString => "沧渊、道具：无限之笛。"
  }
  
    Quest203 = {
    :ID => "203",
    :Name => "【支线】奇怪的影子",
    :QuestGiver => "弗里",
    :Stage1 => "前往弥留之塔",
    :Stage2 => "回去交差",
    :Location1 => "弥留之塔4F",
    :Location2 => "绯雷市",
    :QuestDescription => "弥留之塔4F的墓碑听说\n有奇怪的影子，去调查看看。",
    :RewardString => "妖精石板"
  }
  
  Quest204= {
    :ID => "204",
    :Name => "【支线】化石",
    :QuestGiver => "穗野",
    :Stage1 => "前往紫罗洞穴1F",
    :Stage2 => "回去把化石带给穗野",
    :Location1 => "紫罗洞穴1F",
    :Location1 => "苜蓿镇",
    :QuestDescription => "穗野说紫罗洞穴发现了新的化石，\n拜托我们去拿回来。",
    :RewardString => "化石宝可梦"
  }
  
    Quest205= {
    :ID => "205",
    :Name => "【支线】埃德尔遗迹",
    :QuestGiver => "雾绒村长",
    :Stage1 => "获取冰霜之羽",
    :Stage2 => "前往遗迹",
    :Location1 => "冰绒森林",
    :Location1 => "埃德尔遗迹",
    :QuestDescription => "听说埃德尔遗迹里面可以探索，\n不过要先完成村长的要求。",
    :RewardString => "神柱系列"
  }
  
    Quest206= {
    :ID => "206",
    :Name => "【支线】神秘的身影",
    :QuestGiver => "？？？",
    :Stage1 => "梦境相会是……",
    :Location1 => "\pn的床",
    :QuestDescription => "那个人好像说，梦里？\n总不会是逝者托梦吧……\n（成为冠军后回到家里睡一觉）",
    :RewardString => "红枫雪域入口"
  }
  
   Quest207= {
    :ID => "207",
    :Name => "【支线】红枫雪域",
    :QuestGiver => "酒衣镜",
    :Stage1 => "寻找美梦神/噩梦神",
    :Stage2 => "阻止酋雷姆",
    :Location1 => "红枫之林",
    :Location2 => "雪域山顶",
    :QuestDescription => "要回去吗？那去找找美梦神和噩梦神吧。",
    :RewardString => "红枫种藤藤蛇x1"
  }
  
  Quest208= {
    :ID => "208",
    :Name => "【支线】暗黑超梦",
    :QuestGiver => "未悠",
    :Stage1 => "前往研究所",
    :Stage2 => "阻止铁武者",
    :Location1 => "远方孤岛",
    :Location2 => "神秘洞穴",
    :QuestDescription => "没想到居然有人做克隆产物，\n赶快去查看一下。",
    :RewardString => "暗黑超梦x1"
  }
  
    Quest209= {
    :ID => "209",
    :Name => "【支线】环彩羽的请求",
    :QuestGiver => "环彩羽",
    :Stage1 => "寻找佐鸟笼目",
    :Stage2 => "回去找环彩羽",
    :Location1 => "落英岛",
    :Location2 => "灵诺森岛",
    :QuestDescription => "受彩羽小姐的委托，\n前往落英岛寻找佐鸟笼目。",
    :RewardString => "伊布（丘比形态）x1"
  }
  
    Quest210= {
    :ID => "210",
    :Name => "【支线】心鸣岛",
    :QuestGiver => "竹兰",
    :Stage1 => "前往心鸣岛",
    :Stage2 => "调查三圣姑",
    :Location1 => "铃兰市港口",
    :Location2 => "心鸣岛",
    :QuestDescription => "受到神奥地区冠军的邀请，\n前往心鸣岛查看一下。\n（心鸣岛可以从铃兰市乘坐游船前往）",
    :RewardString => "艾姆利多x1、亚克诺姆x1、由克希x1。"
  }
  
    Quest211= {
    :ID => "211",
    :Name => "【支线】云之神",
    :QuestGiver => "多尔克",
    :Stage1 => "阻止传说中的云",
    :Stage2 => "回去找多尔克",
    :Location1 => "西风海岛",
    :Location2 => "影辞镇",
    :QuestDescription => "西边的海岛上出现了传说中的云\n赶快去查看一下。",
    :RewardString => "道具：显形镜x1"
  }
    Quest212 = {
    :ID => "212",
    :Name => "【支线】讲故事的人",
    :QuestGiver => "清勉",
    :Stage1 => "讲故事的老人",
    :Location1 => "曦寒镇北边屋子",
    :QuestDescription => "清勉说曦寒镇北边屋子的老人知道一些历\n史故事，或许可以去看看。",
    :RewardString => "道具：刺龙王进化石x1"
  }
  
    Quest213 = {
    :ID => "213",
    :Name => "【支线】永恒之焱",
    :QuestGiver => "暗夜",
    :Stage1 => "和老者了解一下情报",
    :Stage2 => "找到暗夜",
    :Location1 => "伊甸园",
    :Location2 => "伊甸园",
    :QuestDescription => "莫名其妙来到了这里\n先想办法回去吧。",
    :RewardString => "达克奈特x1、布洛耶特x1。"
  }
  
   Quest214 = {
    :ID => "214",
    :Name => "【支线】银色U盘",
    :QuestGiver => "未悠",
    :Stage1 => "前往医疗站，寻找知月医生",
    :Stage2 => "将U盘交给知月",
    :Stage3 => "调查清澈湖，传召室确认坐标",
    :Stage4 => "面对超梦的考验",
    :Location1 => "和平希望中心",
    :Location2 => "202诊室",
    :Location3 => "传召室",
    :Location4 => "清澈湖",
    :QuestDescription => "未悠托付的神秘U盘指引你前往知月医生，\n一段围绕超梦与梦二号的隐秘往事即将揭晓。",
    :RewardString => "超梦x1、道具：暗黑超梦进化石x1"
  }
  Quest215 = {
    :ID => "215",
    :Name => "【支线】寻找失踪的教授",
    :QuestGiver => "心仪",
    :Stage1 => "要是看见教授留意一下",
    :Stage2 => "向心仪报告教授的消息。",
    :Location1 => "天空城·狱怜岛",
    :Location2 => "时风镇",
    :QuestDescription => "唐玖教授失踪了！\n她的学生心仪正在焦急地寻找她。\n据说有人在天空城见过这位不靠谱的教授...",
    :RewardString => "石蜗牛x1"
  }
    Quest216 = {
    :ID => "216",
    :Name => "【支线】寻找圣物",
    :QuestGiver => "和服女孩",
    :Stage1 => "去寻找圣物吧。",
    :Stage2 => "去寻找圣物吧。",
    :Stage3 => "回去报告吧。",
    :Location1 => "7号道路→弥留之塔",
    :Location2 => "曦寒山→山顶",
    :Location3 => "天空城→狱怜岛",
    :QuestDescription => "被一个冒失鬼和服女孩强塞了任务。\n去找回丢失的圣物吧。",
    :RewardString => "雷煌x1、焚影x1、道具：虹色之羽x1、银色之羽x1。"
  }
  Quest217 = {
    :ID => "217",
    :Name => "【支线】魁奇思的野心",
    :QuestGiver => "阿克罗马",
    :Stage1 => "调查这里",
    :Stage2 => "阻止等离子团",
    :Location1 => "幽寂遗迹",
    :Location2 => "隐龙塔",
    :QuestDescription => "突然出现的魁奇思……\n总之先调查一下吧。",
    :RewardString => "捷克罗姆x1、莱希拉姆x1、道具：基因之楔x1"
  }
   Quest218 = {
  :ID => "218",
  :Name => "【支线】异界的来访者",
  :QuestGiver => "大木博士",
  :Stage1 => "前往黎明洞穴",
  :Stage2 => "前往黎明洞穴深处",
  :Location1 => "黎明洞穴",
  :Location2 => "黎明洞穴深处",
  :QuestDescription => "收到来自大木博士的请求。\n他在泷歧落地区探测到奇怪的能量波动，\n前往黎明洞穴调查。",
  :RewardString => "？？？？"
}
   Quest219 = {
  :ID => "219",
  :Name => "【支线】皮卡丘测试题",
  :QuestGiver => "茶月镇居民",
  :Stage1 => "测试皮卡丘问题",
  :Location1 => "茶月镇",
  :QuestDescription => "参加皮卡丘测试吧。",
  :RewardString => "道具：电气球x1"
}
   Quest220 = {
  :ID => "220",
  :Name => "【支线】邻居委托",
  :QuestGiver => "茶月镇邻居阿姨",
  :Stage1 => "荼蘼镇警视厅",
  :Location1 => "荼蘼镇",
  :QuestDescription => "邻居阿姨拜托你前往荼蘼镇的\n警视厅，将她的丈夫叫回家。",
  :RewardString => "道具：神奇糖果x2"
}

  Quest221 = {
  :ID => "221",
  :Name => "【支线】金银交织",
  :QuestGiver => "克丽丝",
  :Stage1 => "前往曦寒镇甜品店",
  :Stage2 => "和琴音汇合",
  :Stage3 => "前往凤王所在地",
  :Stage4 => "和凤王对战。",
  :Location1 => "曦寒镇→甜品店",
  :Location2 => "暴风之巅",
  :Location3 => "彩虹山脉",
  :Location4 => "彩虹山脉",
  :QuestDescription => [
  "克丽丝说她的朋友们在甜品店。或许能在\n当地那家著名的甜品店里，找到关于虹色\n与银色羽毛的线索。",
  "岛上的能量开始分化，银色的波动引导着\n你深入。去和琴音汇合吧。",
  "暴风之巅告一段落，前往彩虹山脉和其他\n人汇合。",
  "最后的光芒聚集在彩虹山脉之巅。带着\n恢复生机的虹色之羽，去迎接那位\n传说中的神明所发出的虹色试炼。"
  ],
   :RewardString => "洛奇亚x1、凤王x1。"
  }

  Quest222 = {
  :ID => "222",
  :Name => "【支线】模因重叠",
  :QuestGiver => "索伽",
  :Stage1 => "在月央市寻找南晓的踪迹",
  :Stage2 => "潜入“曼波神殿”",
  :Stage3 => "在地下室击败狂热变态和坤",
  :Location1 => "月央市",
  :Location2 => "曼波神殿→旧仓库",
  :Location3 => "神殿地下室",
  :QuestDescription => [
    "南晓竟然被一群穿着猫耳装的怪人当众抬\n走了！那群人口中高喊着“哈基米”……\n虽然很离谱，但为了南晓的自尊，还是去\n月央市打听下吧。",
    "跟随着猫草的香气，终于找到了所谓的\n“曼波神殿”。索伽看起来异常兴奋……",
    "真相竟然是一个名为和坤的变态在背后操\n纵！他企图用烂梗让世界的时间静止。快\n点打醒这个精神病，把南晓救出来！",
    ],
  :RewardString => "？？？"
  }
  
  Quest223 = {
  :ID => "223",
  :Name => "【支线】剑与盾",
  :QuestGiver => "丹帝",
  :Stage1 => "前往泷歧落地区",
  :Stage2 => "回复小春",
  :Location1 => "泷歧落地区",
  :Location2 => "幻喻岛101房间",
  :QuestDescription => [
  "在幻喻岛遇到来自伽勒尔地区的丹帝一行\n人，他们正在寻找传说中的剑盾勇者苍响\n和藏玛然特。根据信物指引，分别在泷歧\n落地区的莲心湖和影之湖有所反应。",
  "回到幻喻旅馆向小春一行人报告，\n告知已找到两位传说勇者。"
  ],
  :RewardString => "苍响x1、藏马然特x1。"
} 
  
  Quest224 = {
  :ID => "224",
  :Name => "【支线】无极之威胁",
  :QuestGiver => "朱羽博士",
  :Stage1 => "前往时风镇研究所",
  :Stage2 => "前往龙之栖巢",
  :Stage3 => "向朱羽复命",
  :Stage4 => "前往幻喻岛报告",
  :Location1 => "时风镇",
  :Location2 => "龙之栖巢",
  :Location3 => "时风镇研究所",
  :Location4 => "幻喻岛旅馆",
  :QuestDescription => [
    "接到朱羽博士的紧急电话，龙巢的时空\n裂缝出现异常。一只名为无极汰那的巨大\n骨龙正盘踞其中，威胁着两个地区的安危。",
    "在研究所了解了详细情况后，必须进入\n裂缝阻止无极汰那。",
    "穿越裂缝，在暗红天空下的龙巢坑中与\n无极汰那交战。",
    "事件解决后前往幻喻岛，向丹帝一行人\n告知危机解除。"
  ],
  :RewardString => "无极汰那x1"
}
  Quest225 = {
  :ID => "225",
  :Name => "【支线】失控的宝可梦",
  :QuestGiver => "伊娟博士",
  :Stage1 => "前往阻止失控的宝可梦",
  :Stage2 => "回去报告伊娟博士",
  :Location1 => "查看介绍",
  :Location2 => "茶月镇研究所",
  :QuestDescription => [
  "在幻夜之林-紫罗洞穴-凌月桥-弥留之\n塔，出现了失控的宝可梦，前往阻止它们\n吧！",
  "回去报告伊娟博士吧。"
  ],
  :RewardString => "道具：各种进化石"
} 
  Quest226 = {
  :ID => "226",
  :Name => "【支线】失控的宝可梦2",
  :QuestGiver => "伊娟博士",
  :Stage1 => "前往阻止失控的宝可梦",
  :Stage2 => "再次前往平衡之森",
  :Stage3 => "回去报告AZ",
  :Stage4 => "回去报告伊娟博士",
  :Location1 => "查看介绍",
  :Location2 => "传召室→平衡之森",
  :Location3 => "平衡之森",
  :Location4 => "茶月镇研究所",
  :QuestDescription => [
  "在清缘小路-曦寒山山顶-花影祭坛-格\n诺小径，出现了失控的宝可梦，前往阻止它\n们吧！",
  "基格尔德感受到了特殊的能量，回去平衡\n之森找AZ了解一下情况吧。",
  "基格尔德事件结束了，去找AZ报告一下吧。",
  "回去报告伊娟博士吧。"
  ],
  :RewardString => "道具：各种进化石"
} 
  Quest227= {
    :ID => "227",
    :Name => "【支线】查伦·续",
    :QuestGiver => "阿特拉",
    :Stage1 => "前往西风海岛",
    :Stage2 => "调查查伦之家",
    :Stage3 => "前往尘封山深处",
    :Stage4 => "前往查伦所在的裂缝",
    :Stage5 => "和阿特拉对话吧",
    :Location1 => "西风海岛→查伦之家",
    :Location2 => "查伦之家",
    :Location3 => "调查尘封山",
    :Location4 => "前往裂缝",
    :Location5 => "阿特拉的家",
    :QuestDescription =>[
  "原来查伦还有这样的过去，和除灵师阿特\n拉前往西风海岛调查一下。",
  "这里已经很久没人住了，调查一下查伦主\n人的家看看有没有什么线索。\n（或许携带查伦调查会有不一样的答案。）",
  "不知道来到了哪里，总之先和阿特拉一起\n离开这里吧。",
  "前往裂缝查看查伦的记忆吧。",
  "和阿特拉对话吧。"
  ],
  :RewardString => "？？？"
  }
  
  Quest228 = {
  :ID => "228",
  :Name => "【支线】前往莱法岛看看",
  :QuestGiver => "无",
  :Stage1 => "铃兰市→莱法岛",
  :Location1 => "莱法岛",
  :QuestDescription => "前往莱法岛看看吧。",
  :RewardString => "道具：神奇糖果x5"
} 
  
  Quest229 = {
  :ID => "229",
  :Name => "【支线】前往森楠岛看看",
  :QuestGiver => "无",
  :Stage1 => "铃兰市→森楠岛",
  :Location1 => "森楠岛",
  :QuestDescription => "前往森楠岛看看吧。",
  :RewardString => "道具：神奇糖果x5"
} 

  Quest230 = {
    :ID => "230",
    :Name => "【支线】阿尔宙斯的召唤",
    :QuestGiver => "阿尔宙斯",
    :Stage1 => "调查反转世界",
    :Stage2 => "和骑拉帝纳对战",
    :Stage3 => "阻止银河团",
    :Stage4 => "解放时空双神",
    :Stage5 => "和望罗对话",
    :Stage6 => "吹响天界之笛",
    :Location1 => "反转世界深处",
    :Location2 => "暗影殿堂",
    :Location3 => "枪之柱",
    :Location4 => "枪之柱",
    :Location5 => "枪之柱",
    :Location6 => "创世之巅",
    :QuestDescription =>[
  "被阿尔宙斯传送到了一片陌生的空间，周\n围荒芜破败，时空间极不稳定。",
  "在反转世界深处遇见了骑拉帝纳，还有\n一位自称望罗的神秘旅商。望罗将这场试炼\n交由你完成。战胜骑拉帝纳，获得它\n的认可。",
  "与望罗一同抵达天冠山顶的枪之柱，发现\n银河队首领赤日正试图操控帝牙卢卡与\n帕路奇亚。赤日扬言要抹消心灵、重塑\n世界。必须阻止他的疯狂计划。",
  "击败赤日后，解开束缚帝牙卢卡与帕路\n奇亚的红色锁链，时空双神选中了你作\n为试炼者。依次战胜起源形态的帝牙卢\n卡和帕路奇亚，获得祂们的认可。",
  "一切都结束了……去和望罗对话吧……",
  "吹响天界之笛，登上传说的阶梯，抵达创世之间。"
  ],
  :RewardString => "阿尔宙斯x1"
  }
  Quest231 = {
    :ID => "231",
    :Name => "【支线】过去的超梦",
    :QuestGiver => "帅哥",
    :Stage1 => "通过时空裂缝吧",
    :Stage2 => "前往研究室",
    :Stage3 => "阻止超梦",
    :Stage4 => "追捕火箭队",
    :Location1 => "神秘洞穴",
    :Location2 => "神秘研究室·过去",
    :Location3 => "神秘研究室·过去",
    :Location4 => "神秘洞穴",
    :QuestDescription =>[
  "没想到碰见国际刑警了，赶紧通过裂\n缝去看看吧。",
  "帅哥大叔说机密都在左边的研究所，\n赶快阻止火箭队吧。",
  "没想到火箭队已经研究出超梦了！先\n不管火箭队了，赶快去阻止超梦！",
  "超梦已经冷静下来了，该去和帅哥\n大叔汇合了。"
  ],
  :RewardString => "道具：拘束装甲x1，R装甲x1"
  }  
  
  Quest232 = {
  :ID => "232",
  :Name => "【支线】比克提尼A",
  :QuestGiver => "佑树",
  :Stage1 => "前往桃苑森林",
  :Stage2 => "回去报告",
  :Location1 => "桃苑森林",
  :Location2 => "格诺镇",
  :QuestDescription => "传说中的胜利之星比克提尼出现了，据说\n在桃苑森林里面发现了踪迹！",
  :RewardString => "道具：神奇糖果x2"
} 
   Quest233 = {
  :ID => "233",
  :Name => "【支线】比克提尼B",
  :QuestGiver => "灵境",
  :Stage1 => "前往静隐林",
  :Stage2 => "回去报告",
  :Location1 => "静隐林",
  :Location2 => "沃绕镇",
  :QuestDescription => "又是比克提尼的踪迹，不知道真的假\n的，还是去看看吧。",
  :RewardString => "道具：神奇糖果x2，银色王冠x4"
}  
  Quest234 = {
  :ID => "234",
  :Name => "【支线】比克提尼C",
  :QuestGiver => "骇浪岛大叔",
  :Stage1 => "前往花影之路",
  :Location1 => "花影之路",
  :QuestDescription => "反正20w已经给出去了，还是去看看吧……",
  :RewardString => "比克提尼x1"
}  

Quest235 = {
  :ID => "235",
  :Name => "【支线】咭呗咭呗团团乱plus",
  :QuestGiver => "普兰特",
  :Stage1 => "前往普兰特的家",
  :Stage2 => "前往迷迭之路调查裂缝",
  :Stage3 => "调查裂缝",
  :Stage4 => "和面前二人对话",
  :Stage5 => "带丹瑜和乌栗回去",
  :Stage6 => "前往迷迭之路",
  :Stage7 => "寻找丹瑜和乌栗",
  :Stage8 => "寻找三宝伴的踪迹",
  :Stage9 => "回去找丹瑜和乌栗",
  :Stage10 => "击败桃歹郎",
  :Stage11 => "和乌栗对战",
  :Stage12 => "回去查看薇江和普兰特",
  :Location1 => "枫弦镇",
  :Location2 => "枫弦镇→迷迭之路",
  :Location3 => "迷迭之路",
  :Location4 => "迷迭之路",
  :Location5 => "枫弦民居",
  :Location6 => "迷迭之路",
  :Location7 => "迷迭之路",
  :Location8 => "枫弦镇周围各处",
  :Location9 => "迷迭之路",
  :Location10 => "迷迭之路",
  :Location11 => "迷迭之路",
  :Location12 => "枫弦民居",
  :QuestDescription => [
    "普兰特突然来电说有急事，火速赶到他家看看\n究竟发生了什么。",
    "先不管这俩跳舞的笨蛋了，东边的森林出现\n了时空裂缝，过去探查一下。",
    "三只宝可梦逃得没影了，但裂缝还在，先调\n查一下裂缝的情况吧。",
    "裂缝里又掉出来两个人？先和TA们聊聊，搞\n清楚来路再说。",
    "原来那三只是宝伴，桃歹郎才是幕后元凶。\n先带丹瑜和乌栗回去看看那俩\n笨蛋的状况。",
    "回到迷迭之路，看看有没有新的线索。",
    "被奇怪的宝可梦缠上了，先去找丹瑜和乌栗\n汇合吧。",
    "跟着厄诡椪的指引，去枫弦镇周边寻找三只\n宝伴的藏身之处！\n（位置分别在幻夜森林、樱祭公园、五号道路。）",
    "三宝伴全部解决，该去找幕后的桃歹郎了。\n先回去找丹瑜和乌栗商量对策。",
    "桃歹郎终于现身了，就是这家伙在搞鬼，\n击败它！",
    "桃歹郎被击败后，乌栗突然发起了对战挑战……\n陪他打一场吧。",
    "乌栗和丹瑜回北上乡了，回去看看薇江和\n普兰特恢复得怎么样了。",
  ],

  :RewardString => "桃歹郎x1、厄诡椪x1、愿增猿x1、吉雉鸡x1、够赞狗x1"
}

Quest236 = {
  :ID => "236",
  :Name => "【支线】鲨鱼的委托",
  :QuestGiver => "灾锚鲨龙",
  :Stage1 => "寻找五个鲨鱼小弟",
  :Stage2 => "回去向灾锚鲨龙报告",
  :Location1 => "海域附近",
  :Location2 => "19号道路",
  :QuestDescription => [
    "一条会说话的鲨鱼给你派了任务，说它的五\n个小弟跑出去好久没回来，帮忙把它们\n找回来吧。",
    "五个小弟都找齐了，回去向灾祸鲨龙\n报告吧。",
  ],
  :RewardString => "小鲨鱼x1"
}

  Quest237 = {
    :ID => "237",
    :Name => "【支线】南晓的电话",
    :QuestGiver => "南晓",
    :Stage1 => "前往铃兰市",
    :Stage2 => "打倒熔岩队",
    :Stage3 => "阻止熔岩队",
    :Stage3 => "阻止固拉多",
    :Location1 => "铃兰市港口",
    :Location2 => "115道路",
    :Location3 => "墨痕洞穴",
    :Location4 => "熔岩洞穴",
    :QuestDescription => [
  "居然会被电话联系，看来事态十分紧急。\n前往铃兰市去泷歧落地区吧。",
  "奇怪的组织，和南晓一起打倒他们吧！",
  "没想到居然是新的组织，看来他们\n已经占据了墨痕洞穴，前往深处阻止他们吧！",
  "熔岩队回到了他们的时空，但是固\n拉多还在，阻止固拉多吧！"
    ],
    :RewardString => "固拉多x1"
  }
  
  Quest238 = {
  :ID => "238",
  :Name => "【支线】索伽的电话",
  :QuestGiver => "索伽",
  :Stage1 => "前往泷歧落花田",
  :Stage2 => "前往千夜岛",
  :Stage3 => "打倒水舰队",
  :Stage4 => "前往千夜洞穴",
  :Stage5 => "阻止盖欧卡",
  :Location1 => "泷歧落花田",
  :Location2 => "千夜岛",
  :Location3 => "千夜岛",
  :Location4 => "千夜洞穴",
  :Location5 => "千夜隐湖",
  :QuestDescription => [
    "坏消息总是接二连三，这次又接到了索伽\n的电话。先去泷歧落花田和南晓会合\n吧。" ,
    "南晓告诉你索伽曾在西边出现，前往西边\n调查，并获得前往千夜岛的情报。",
    "千夜岛被水舰队封锁了入口，先打倒守在\n门口的小兵吧！",
    "千夜洞穴内传出异动，快去调查他们的据点。",
    "水舰队已经消失无踪，但麻烦并未结束\n——盖欧卡正在苏醒，必须阻止它！"
  ],
  :RewardString => "盖欧卡x1"
}
  
  Quest239 = {
  :ID => "239",
  :Name => "【支线】裂缝谜语人",
  :QuestGiver => "无",
  :Stage1 => "前往时风镇吧",
  :Stage2 => "调查陨石",
  :Stage3 => "去找希嘉娜吧",
  :Location1 => "时风镇",
  :Location2 => "陨石",
  :Location3 => "时风镇→龙之栖巢",
  :QuestDescription => 
  [
    "神秘人把我们的超级石拿走了\n先去时风镇找朱羽博士商量对策。",
    "裂空座把陨石撞了个大坑，\n调查一下陨石吧。",
    "一切尘埃落定，和希嘉娜对战吧！",
    ],
  :RewardString => "裂空座x1"
}
  Quest240 = {
  :ID => "240",
  :Name => "【支线】平衡之森",
  :QuestGiver => "？？？",
  :Stage1 => "调查森林中的异常气息",
  :Stage2 => "前往黯影裂谷深处",
  :Stage3 => "寻找生命圣泉的源头",
  :Stage4 => "前往界隙之核阻止裂缝扩散",
  :Location1 => "平衡之森",
  :Location2 => "黯影裂谷",
  :Location3 => "生命圣泉",
  :Location4 => "界隙之核",
  :QuestDescription => [
  "森林中似乎出现了异样的能量波动……\n不如先去探查一番，也许能发现什么线索。",
  "在森林深处发现了通往黯影裂谷的隐秘路\n径。也许答案，就藏在更加幽暗的地方。",
  "泉水微光指引着你深入古老地脉，\n圣泉之下，似乎藏着不为人知的秘密。",
  "所有线索最终指向了界隙之核。\n不前去一探究竟，就无法揭开森林的真相。"
  ],
   :RewardString => "伊裴尔塔尔x1、哲尔尼亚斯x1、基格尔德x1"
  }
  
  Quest241 = {
  :ID => "241",
  :Name => "【支线】空域秘辛",
  :QuestGiver => "无",
  :Stage1 => "前往厄季斯岛",
  :Stage2 => "和阿罗拉的训练师对战",
  :Stage3 => "镇压灾祸之简",
  :Stage4 => "镇压灾祸之玉",
  :Stage5 => "镇压灾祸之鼎",
  :Stage6 => "镇压灾祸之剑",
  :Location1 => "天空城地区→厄季斯岛",
  :Location2 => "厄季斯岛",
  :Location3 => "翼霄岛→春之岛",
  :Location4 => "春之岛→夏之岛",
  :Location5 => "夏之岛→秋之岛",
  :Location6 => "秋之岛→冬之岛",
  :QuestDescription => [
    "联盟难得给我休了个假，前往厄季斯岛看看吧。",
    "来自阿罗拉的训练家们发起了挑战，接受\n对战试试看。",
    "前往春之岛，在卡璞·鸣鸣的协助下镇压\n古简蜗。",
    "前往夏之岛，在卡璞·蝶蝶的协助下镇压\n古玉鱼。",
    "前往秋之岛，在卡璞·哞哞的协助下镇压\n古鼎鹿。",
    "前往冬之岛，在卡璞·鳍鳍的协助下镇压\n古剑豹。"
],
  :RewardString => "科斯莫古x1"
}

Quest242 = {
  :ID => "242",
  :Name => "【支线】光辉大神",
  :QuestGiver => "山茶？",
  :Stage1 => "迎战奈克洛兹玛",
  :Stage2 => "阻止合体奈克洛兹玛",
  :Stage3 => "迎战最终奈克洛兹玛",
  :Location1 => "星夜长河",
  :Location2 => "星夜长河",
  :Location3 => "星夜长河",
  :QuestDescription => [
    "刚处理完四灾事件，却被山茶的\n拖入了神秘的星夜长河。周围的光芒\n异常诡异，山茶变成了奈克洛兹玛，\n准备迎战！",
    "奈克洛兹玛强行融合了索尔迦雷欧和\n露奈雅拉，想办法击败它并逃离这里。",
    "奈克洛兹玛进化为究极形态，希丝娜与\n静海的远程支援及时到达，拂晓之刃与\n璨星之御感应到外界的力量现身助战。\n彻底击败这位不怀好意的客人吧！",
  ],
  :RewardString => "奈克洛兹玛x1、科斯莫古x1"
}

  Quest300= {
    :ID => "300",
    :Name => "【通告】当前版本开放剧情已完结",
    :QuestGiver => "叶昕苍",
    :Stage1 => "游戏官方群",
    :Location1 => "群号：537650384",
    :QuestDescription => "当前版本剧情截止于此，\n请期待后续版本更新。\n一群群号：537650384\n二群群号：649838988",
    :RewardString => "？？？"
  }
  
end