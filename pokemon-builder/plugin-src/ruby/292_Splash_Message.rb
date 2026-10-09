SPLASH_MESSAGE = [
  "昕纪元·影辞！",
  "访问频道查看资料！",
  "也逛逛宝可饭堂！",
  "也尝试玩玩零维E界！",
  "也尝试玩玩寻猎！",
  "也尝试玩玩星晶！",
  "也试试漆黑的魅影！",
  "也试试究极绿宝石！",
  "也试试永恒之焱！",
  "也试试绿宝石改！",
  "也试试水银！",
  "不要盯着BUG不放！",
  "已修复90%的BUG！",
  "真的有人看这个吗？",
  "所以，你看了这个。",
  "100%免费！",
  "中国制造！",
  "更多马赛克！",
  "找不到从良的理由。",
  "像素！",
  "闪烁的文字！",
  "随机的闪烁标语！",
  "玩一辈子昕纪元好吗？",
  "这就只是个游戏！",
  "单人游戏！",
  "耶！",
  "哇哦！",
  "一次买够！",
  "咕咕嘎嘎！",
  "曼波，保姆曼波~",
  "缭乱！虹咲！！",
  "不在电商平台售卖！",
  "由昕纪元制作组制作！",
  "吃我基因光线！",
  "锟斤拷烫烫烫！",
  "MissingNo.",
  "包含99%的宝可梦！",
  "玩家就是你！",
  "1+1＝？",
  "这里没有五分钟冠军！",
  "千里击涛！！",
  "天上的星星地上的河！",
  "女尸们鲜生们欢迎游玩！",
  "试试物理驱鬼！",
  "俺寻思之力！",
  "是啊，吃什么？",
  "复制粘贴，启动！",
  "面向搜索编程。",
  "面向玄学编程。",
  "重启解决90%问题。",
  "重装解决99%问题。",
  "换电脑解决100%问题。",
  "不要动屎山。",
  "代码能跑就行。",
  "能跑就别改。",
  "改了就跑不起来。",
  "再改剁手。",
  "真香。",
  "这不是BUG，是特性。",
  "已修复99%的BUG！",
  "剩下的1%是特性。",
  "警告：本游戏包含BUG。",
  "警告：本游戏没有BUG。",
  "以上警告是假的。",
  "本标语是随机出现的。",
  "你看到这条说明运气不错。",
  "恭喜你浪费了0.5秒。",
  "继续按A。",
  "按A没用。",
  "按B也没用。",
  "游戏开始。",
  "游戏结束。",
  "再来一次。",
  "这次一定。",
  "下次一定。",
  "鸽了。",
  "在做了。",
  "已覆盖存档。",
  "恭喜你。",
  "你失去了所有进度。",
  "这就是人生。",
  "祖传代码，启动！",
  "注释：我也不知道为什么能跑。",
  "为什么要演奏春日影！！！",
]

class Splash_Message
  attr_accessor :x, :y
  def id; return "splash"; end
  def id?(val); return self.id == val; end
  # disposes of everything
  def dispose
    @disposed = true
    @copyright.dispose if @copyright
    @splash_message.dispose if @splash_message
  end
  # visibility (not applicable)
  def visible
    return @copyright && @copyright.visible
  end
  def visible=(val)
    @copyright.visible = val if @copyright
    @splash_message.visible = val if @splash_message
  end
  # checks if disposed
  def disposed?; return @disposed; end
  # end
  def initialize(viewport)
    @viewport     = viewport
    @font_color   = Color.new(248, 248, 248)
    @shadow_color = Color.new( 64,  64,  64)
    
    @copyright         = BitmapSprite.new(Graphics.width, 22)
    @copyright.x       = 0
    @copyright.y       = Graphics.height - 22
    @copyright.z       = @viewport.z
    @copyright.visible = false
    pbSetSmallFont(@copyright.bitmap)
    cprtpos = [
      [_INTL("{1}", CURRENT_NAME), 0, 0, 0, @font_color, @shadow_color],
      [_INTL("Rx昕纪元制作组. 请勿破解盈利"), Graphics.width, 0, 1, @font_color, @shadow_color]
    ]
    pbDrawTextPositions(@copyright.bitmap, cprtpos)
    
    @x = @viewport.rect.width * 4 / 5 - 10
    @y = @viewport.rect.height / 2 - 44
    @splash_message         = BitmapSprite.new(240, 22)
    @splash_message.x       = @x
    @splash_message.y       = @y
    @splash_message.z       = @viewport.z
    @splash_message.ox      = 120
    @splash_message.oy      = 11
    @splash_message.angle   = 15
    @splash_message.visible = false
    pbSetSmallFont(@splash_message.bitmap)
    textpos = [
      [_INTL("{1}", getMessage), 120, 0, 2, Color.new(255, 216, 0), Color.new(216, 128, 0)]
    ]
    pbDrawTextPositions(@splash_message.bitmap, textpos)
    
    @zooms = [1.00, 1.01, 1.02, 1.03, 1.04, 1.05, 1.06, 1.07, 1.08, 1.09, 1.10,
              1.09, 1.08, 1.07, 1.06, 1.05, 1.04, 1.03, 1.02, 1.01]
    @zoomi = 0
  end
  
  def getMessage
    message = SPLASH_MESSAGE.sample
    return message
  end
  
  def update
    @zoomi += 1
    @zoomi = 0 if @zoomi >= @zooms.length * 3
    return if @zoomi % 3 != 0
    i = @zoomi / 3
    @splash_message.zoom_x = @zooms[i]
    @splash_message.zoom_y = @zooms[i]
  end
  
  # method to reposition the logo
  def position(x=nil,y=nil)
    @x = x if !x.nil?
    @y = y if !y.nil?
    @splash_message.x = x.nil? ? self.x : x
    @splash_message.y = y.nil? ? self.y : y
  end
end