#===============================================================================
#                 B2W2 Trainer Card by KleinStudio
#
#      Modified By LackDeJurane/CharizardThree3 for Essentials v18
#
#    If used, much of the credit goes to KleinStudio, would be kind of you 
#                            to credit me 
#===============================================================================
class PokeBattle_Trainer
  attr_accessor(:cardlevel)
end

#===============================================================================
# Trainer card info for leaders
#===============================================================================
LEADERINFO = [
  #主地区
  [
    ["trainer086", "格诺镇", "洛尔", "武馆年轻的师傅\n“用战斗来诉说一切。”", "择武徽章"],
    ["trainer085", "茸舒镇", "相诗", "热爱自然万物的少女\n“森林的守护者。”", "栖萤徽章"],
    ["trainer087", "苜蓿镇", "穗野", "专注岩石研究的学者\n“岩石的意志不可磨灭。”", "明石徽章"],
    ["trainer088", "枫弦镇", "普兰特", "研究草本的医者\n“医难在药方，贵在诊断。”", "芝梦徽章"],
    ["trainer089", "月央市", "灰羽", "如钢铁般坚韧的狂人\n“训练没有尽头。”", "拢冶徽章"],
    ["trainer090", "绯雷市", "芙蕾", "优雅与雷霆并存的修女\n“聆听万物的心声。”", "耀霆徽章"],
    ["trainer091", "隐龙市", "未明", "探索古代文明的记者\n“追寻遗失的文明印记。”", "荒烛徽章"],
    ["trainer092", "曦寒市", "莫云", "以冰雪为旋律的乐者\n“自然的旋律令人陶醉。”", "漪雪徽章"]
  ],
  #群岛地区
  [
    
  ],
  #天空城地区
  [
    ["trainer093", "茉克岛", "特娅",  "温柔优雅的妖精系使者\n“和平共处才是美好的真谛。”", "月铃徽章"],
    ["trainer094", "厄季斯岛", "卡斯特","喜欢恶作剧的幽灵考察员\n“你害怕鬼故事吗？”", "冥荼徽章"],
    ["trainer095", "灵诺森岛", "拉伯克","不正经的超能力者\n“博物馆和超能力一样有趣！”", "明心徽章"],
    ["trainer096", "绯焰岛", "若晴", "张扬而活力四射的斗士\n“战斗就要燃起来才行！”", "灼灵徽章"],
    ["trainer097", "虹兰岛", "安希丝", "专注研究进化论的学者\n“科学才能揭示宝可梦的奥秘。”", "浮梦徽章"],
    ["trainer099", "落英岛", "安德",  "热爱自然的旅行者\n“大地孕育万物，草木皆有灵。”","芳英徽章"],
    ["trainer098", "规盈岛", "米娅","海洋的追寻者\n“让水的流动成为战斗的旋律。”", "泠渊徽章"],
    ["trainer100", "狱怜岛", "诺埃尔", "曾为天王的狂放之人\n“黑暗中也有属于自己的光。”", "祸尾徽章"]
  ],
  #康特
  [
  
      ],
  #时空裂缝
  [
  
      ]
]
#===============================================================================
#
#Utilities and Methods by Klein and Luka S.J
#
#===============================================================================
class Sprite
  def pos(x2,y2)
    self.x=x2
    self.y=y2
  end
end

def globalIconAnim(sprite, buttom=nil)
  sprite.tone.red+=80
  sprite.tone.green+=80
  sprite.tone.blue+=80
  pbWait(2)
  sprite.tone.red-=80
  sprite.tone.green-=80
  sprite.tone.blue-=80
end

def pbCardLevelIncrease
  $Trainer.cardlevel+=1
end

def pbCardLevelDecrease
  $Trainer.cardlevel-=1 if $Trainer.cardlevel!=0
end

def pbPositionPokemonSprite(sprite,left,top)
  if sprite.bitmap && !sprite.bitmap.disposed?
    sprite.x=left+(128-sprite.bitmap.width)/2
    sprite.y=top+(128-sprite.bitmap.height)/2
  else
    sprite.x=left
    sprite.y=top
  end
end

class PokemonSpriteBW
  attr_accessor :selected
  attr_accessor :shadow
  attr_accessor :sprite
  attr_accessor :src_rect
  attr_accessor :showshadow
  attr_accessor :status
  attr_reader :loaded

  def initialize(viewport=nil)
    @viewport=viewport
    @metrics=load_data("Data/metrics.dat")
    @selected=0
    
    @status=0
    @loaded=false
    @showshadow=true
    @altitude=0
    @yposition=0
    @sprite=Sprite.new(@viewport)
    @overlay=Sprite.new(@viewport)
    @lock=false
  end
  
  def x; @sprite.x; end
  def y; @sprite.y; end
  def z; @sprite.z; end
  def ox; @sprite.ox; end
  def oy; @sprite.oy; end
  def ox=(val);;end
  def oy=(val);;end
  def zoom_x; @sprite.zoom_x; end
  def zoom_y; @sprite.zoom_y; end
  def visible; @sprite.visible; end
  def opacity; @sprite.opacity; end
  def width; @bitmap.width; end
  def height; @bitmap.height; end
  def tone; @sprite.tone; end
  def bitmap; @bitmap.bitmap; end
  def actualBitmap; @bitmap; end
  def disposed?; @sprite.disposed?; end
  def color; @sprite.color; end
  def src_rect; @sprite.src_rect; end
  def blend_type; @sprite.blend_type; end
  def angle; @sprite.angle; end
  def mirror; @sprite.mirror; end
  def lock
    @lock=true
  end
  def unlock
    @lock=false
  end
  def bitmap=(val)
    @bitmap.bitmap=val
  end
  def x=(val)
    @sprite.x=val
  end
  def ox=(val)
    @sprite.ox=val
  end
  def oy=(val)
    @sprite.oy=val
  end
  def y=(val)
    @sprite.y=val
  end
  def z=(val)
    @sprite.z=val
  end
  def zoom_x=(val)
    @sprite.zoom_x=val
  end
  def zoom_y=(val)
    @sprite.zoom_y=val
  end
  def visible=(val)
    @sprite.visible=val
  end
  def opacity=(val)
    @sprite.opacity=val
  end
  def tone=(val)
    @sprite.tone=val
  end
  def color=(val)
    @sprite.color=val
  end
  def blend_type=(val)
    @sprite.blend_type=val
  end
  def angle=(val)
    @sprite.angle=(val)
  end
  def mirror=(val)
    @sprite.mirror=(val)
  end
  def dispose
    @sprite.dispose
  end
    
  def setPokemonBitmap(pokemon,back=false)
    @bitmap=pbLoadPokemonBitmap(pokemon,back)
    @sprite.bitmap=@bitmap.bitmap.clone
    @sprite.ox=@bitmap.width/2
    @sprite.oy=@bitmap.height/2
    @loaded=true
  end
  
  def update
    return if @lock
    if @bitmap
      @bitmap.update
      @sprite.bitmap=@bitmap.bitmap.clone
    end
  end  
  
end

class TrainerSpriteBW 
  
  attr_accessor :selected
  attr_accessor :shadow
  attr_accessor :sprite
  attr_accessor :src_rect
  attr_accessor :showshadow
  attr_accessor :status
  attr_reader :loaded

  def initialize(x,y,viewport=nil)
    @viewport=viewport
    @metrics=load_data("Data/metrics.dat")
    @selected=0
    @status=0
    @loaded=false
    @showshadow=false
    @altitude=0
    @yposition=0
    @shadow=Sprite.new(@viewport)
    @sprite=Sprite.new(@viewport)
    @sprite.x=x
    @sprite.y=y

    @overlay=Sprite.new(@viewport)
    @lock=false
  end
  
  def x; @sprite.x; end
  def y; @sprite.y; end
  def z; @sprite.z; end
  def ox; @sprite.ox; end
  def oy; @sprite.oy; end
  def ox=(val);;end
  def oy=(val);;end
  def zoom_x; @sprite.zoom_x; end
  def zoom_y; @sprite.zoom_y; end
  def visible; @sprite.visible; end
  def opacity; @sprite.opacity; end
  def width; @bitmap.width; end
  def height; @bitmap.height; end
  def tone; @sprite.tone; end
  def bitmap; @bitmap.bitmap; end
  def actualBitmap; @bitmap; end
  def disposed?; @sprite.disposed?; end
  def color; @sprite.color; end
  def src_rect; @sprite.src_rect; end
  def blend_type; @sprite.blend_type; end
  def angle; @sprite.angle; end
  def mirror; @sprite.mirror; end
  def lock
    @lock=true
  end
  def unlock
    @lock=false
  end
  def bitmap=(val)
    @bitmap.bitmap=val
  end
  
  def finished?
    return @bitmap.finished?
  end
  
  def x=(val)
    @sprite.x=val
    @shadow.x=val
  end
  def ox=(val)
    @sprite.ox=val
    self.formatShadow
  end
  def oy=(val)
    @sprite.oy=val
    self.formatShadow
  end
  def y=(val)
    @sprite.y=val
    @shadow.y=val
  end
  def z=(val)
    @shadow.z=10
    @sprite.z=val
  end
  def zoom_x=(val)
    @sprite.zoom_x=val
    self.formatShadow
  end
  def zoom_y=(val)
    @sprite.zoom_y=val
    self.formatShadow
  end
  def visible=(val)
    @sprite.visible=val
    self.formatShadow
  end
  def opacity=(val)
    @sprite.opacity=val
    self.formatShadow
  end
  def tone=(val)
    @sprite.tone=val
    self.formatShadow
  end
  def color=(val)
    @sprite.color=val
    self.formatShadow
  end
  def blend_type=(val)
    @sprite.blend_type=val
    self.formatShadow
  end
  def angle=(val)
    @sprite.angle=(val)
    self.formatShadow
  end
  def mirror=(val)
    @sprite.mirror=(val)
    self.formatShadow
  end
  def dispose
    @sprite.dispose
    @shadow.dispose
  end
  
  def totalFrames; @bitmap.animationFrames; end
  def toLastFrame; @bitmap.toFrame("last"); end
  def selected; end
    
  def setBitmap(file)
    @bitmap=AnimatedBitmapWrapperLast.new(file)
    @sprite.bitmap=@bitmap.bitmap.clone
    @shadow.bitmap=@bitmap.bitmap.clone
    self.formatShadow
  end
  
  def formatShadow
    @shadow.zoom_x=@sprite.zoom_x*1.1-(0.011*@altitude)
    @shadow.zoom_y=@sprite.zoom_y*0.32-(0.0032*@altitude)
    @shadow.ox=@sprite.ox-4
    @shadow.oy=@sprite.oy-@altitude
    @shadow.opacity=@sprite.opacity*0.3
    @shadow.angle=@sprite.angle-6
    @shadow.tone=Tone.new(-255,-255,-255,255)
    @shadow.visible=@sprite.visible
    @shadow.mirror=@sprite.mirror
    
    @shadow.visible=false if !@showshadow
  end
  
  def update
    return if @lock
    if @bitmap
      @bitmap.update
      @sprite.bitmap=@bitmap.bitmap.clone
      @shadow.bitmap=@bitmap.bitmap.clone
    end
    self.formatShadow
  end  
end


class AnimatedBitmapWrapperAnim
  attr_reader :width
  attr_reader :height
  attr_reader :totalFrames
  attr_reader :animationFrames
  attr_reader :currentIndex
  attr_reader :scale
  
  def initialize(file,twoframe=false)
    raise "filename is nil" if file==nil
    @scale = 1
    @width = 0
    @height = 0
    @frame = 0
    @frames = 2
    @direction = +1
    @twoframe = twoframe
    @animationFinish = false
    @totalFrames = 0
    @currentIndex = 0
    @speed = 1
    @finished=false
    @middle=false
      # 0 - not moving at all
      # 1 - normal speed
      # 2 - medium speed
      # 3 - slow speed
    @bitmapFile=BitmapCache.load_bitmap(file)
      # initializes full Pokemon bitmap
    @bitmap=Bitmap.new(@bitmapFile.width,@bitmapFile.height)
    @bitmap.blt(0,0,@bitmapFile,Rect.new(0,0,@bitmapFile.width,@bitmapFile.height))
    @width=@bitmap.height*@scale
    @height=@bitmap.height*@scale
    
    @totalFrames=@bitmap.width/@bitmap.height
    @animationFrames=@totalFrames*@frames
      # calculates total number of frames
    @loop_points=[0,@totalFrames]
      # first value is start, second is end
    
    @actualBitmap=Bitmap.new(@width,@height)
    @actualBitmap.clear
    @actualBitmap.stretch_blt(Rect.new(0,0,@width,@height),@bitmap,Rect.new(@currentIndex*(@width/@scale),0,@width/@scale,@height/@scale))
    
    end
    
  def length; @totalFrames; end
  def disposed?; @actualBitmap.disposed?; end
  def dispose; @actualBitmap.dispose; end
  def copy; @actualBitmap.clone; end
  def bitmap; @actualBitmap; end
  def bitmap=(val); @actualBitmap=val; end
  def each; end
  def alterBitmap(index); return @strip[index]; end
    
  def prepareStrip
    @strip=[]
    for i in 0...@totalFrames
      bitmap=Bitmap.new(@width,@height)
      bitmap.stretch_blt(Rect.new(0,0,@width,@height),@bitmapFile,Rect.new((@width/@scale)*i,0,@width/@scale,@height/@scale))
      @strip.push(bitmap)
    end
  end
  def compileStrip
    @bitmap.clear
    for i in 0...@strip.length
      @bitmap.stretch_blt(Rect.new((@width/@scale)*i,0,@width/@scale,@height/@scale),@strip[i],Rect.new(0,0,@width,@height))
    end
  end
  
  def reverse
    if @direction  >  0
      @direction=-1
    elsif @direction < 0
      @direction=+1
    end
  end
  
  def setLoop(start, finish)
    @loop_points=[start,finish]
  end
  
  def setSpeed(value)
    @speed=value
  end
  
  def update
    return false if @speed < 1
    case @speed
    # frame skip
    when 1
      @frames=2
    when 2
      @frames=4
    when 3
      @frames=5
    end
    @frame+=1
    
    if @frame >=@frames
      # processes animation speed
      if @currentIndex < @totalFrames
      @currentIndex+=@direction 
      end
      @frame=0
    end
    
    @currentIndex=@totalFrames if @currentIndex > @totalFrames
    @finished=true if @currentIndex==@totalFrames
    @middle=true if @currentIndex==@totalFrames/2
    
    @actualBitmap.clear
    @actualBitmap.stretch_blt(Rect.new(0,0,@width,@height),@bitmap,Rect.new(@currentIndex*(@width/@scale),0,@width/@scale,@height/@scale))
      # updates the actual bitmap
    end
    
    def finished?
      return @finished
    end
    
    def middle?
      return @middle
    end
    
  # returns bitmap to original state
  def deanimate
    @frame=0
    @currentIndex=0
    @actualBitmap.clear
    @actualBitmap.stretch_blt(Rect.new(0,0,@width,@height),@bitmap,Rect.new(@currentIndex*(@width/@scale),0,@width/@scale,@height/@scale))
  end
end

class AnimatedBitmapWrapperLast < AnimatedBitmapWrapperAnim
    def update
    return false if @speed < 1
    case @speed
        # frame skip
      when 1
        @frames=2
      when 2
        @frames=4
      when 3
        @frames=5
    end
    @frame+=1
    
    if @frame >=@frames
      # processes animation speed
      if @currentIndex < @totalFrames-1
      @currentIndex+=@direction 
      end
      @frame=0
    end
    
    @currentIndex=@totalFrames-1 if @currentIndex > @totalFrames-1
    @finished=true if @currentIndex==@totalFrames-1
    
    @actualBitmap.clear
    @actualBitmap.stretch_blt(Rect.new(0,0,@width,@height),@bitmap,Rect.new(@currentIndex*(@width/@scale),0,@width/@scale,@height/@scale))
      # updates the actual bitmap
    end
  end  
  
  
def pbCreateAnimatedPicture(species,x,y)
  $pokeani=PokemonSpriteDex.new(@viewport)
  $pokeani.loadPokemonBitmap(species)
  $pokeani.opacity=0
  $pokeani.z=99999
  $pokeani.x=x
  $pokeani.y=y
  $pokeani.ox=$pokeani.sprite.bitmap.width/2
  $pokeani.oy=$pokeani.sprite.bitmap.height/2
end

def pbShowPokemon(species,x,y,cry=true)
  pbCreateAnimatedPicture(species,x,y)
  10.times do 
    Graphics.update
    Input.update
    $pokeani.update
    $pokeani.opacity+=25.5
  end
  pbPlayCry(species) if cry
end

def pbDisposePokemon
  return if !$pokeani
  10.times do 
    Graphics.update
    Input.update
    $pokeani.update
    $pokeani.opacity-=25.5
  end
  $pokeani.dispose
end

def pbUpdatePokemonInMap
  $pokeani.update if $pokeani && !$pokeani.disposed?
end

class GifAnim
  attr_accessor :selected
  attr_accessor :shadow
  attr_accessor :sprite
  attr_accessor :src_rect
  attr_accessor :showshadow
  attr_accessor :status
  attr_reader :loaded

  def initialize(x,y,viewport=nil,repeat=false)
    @viewport=viewport
    @metrics=load_data("Data/metrics.dat")
    @selected=0
    @status=0
    @loaded=false
    @repeat=repeat
    @showshadow=false
    @altitude=0
    @yposition=0
    @sprite=Sprite.new(@viewport)
    @sprite.x=x
    @sprite.y=y

    @overlay=Sprite.new(@viewport)
    @lock=false
  end
  
  def x; @sprite.x; end
  def y; @sprite.y; end
  def z; @sprite.z; end
  def ox; @sprite.ox; end
  def oy; @sprite.oy; end
  def ox=(val);;end
  def oy=(val);;end
  def zoom_x; @sprite.zoom_x; end
  def zoom_y; @sprite.zoom_y; end
  def visible; @sprite.visible; end
  def opacity; @sprite.opacity; end
  def width; @bitmap.width; end
  def height; @bitmap.height; end
  def tone; @sprite.tone; end
  def bitmap; @bitmap.bitmap; end
  def actualBitmap; @bitmap; end
  def disposed?; @sprite.disposed?; end
  def color; @sprite.color; end
  def src_rect; @sprite.src_rect; end
  def blend_type; @sprite.blend_type; end
  def angle; @sprite.angle; end
  def mirror; @sprite.mirror; end
  def lock
    @lock=true
  end
  def unlock
    @lock=false
  end
  def bitmap=(val)
    @bitmap.bitmap=val
  end
  
  def finished?
    return @bitmap.finished?
  end
  
  def middle?
    return @bitmap.middle?
  end
  
  def x=(val)
    @sprite.x=val
  end
  def ox=(val)
    @sprite.ox=val
  end
  def oy=(val)
    @sprite.oy=val
  end
  def y=(val)
    @sprite.y=val
  end
  def zoom_x=(val)
    @sprite.zoom_x=val
  end
  def zoom_y=(val)
    @sprite.zoom_y=val
  end
  def visible=(val)
    @sprite.visible=val
  end
  def opacity=(val)
    @sprite.opacity=val
  end
  def tone=(val)
    @sprite.tone=val
  end
  def color=(val)
    @sprite.color=val
  end
  def blend_type=(val)
    @sprite.blend_type=val
  end
  def angle=(val)
    @sprite.angle=(val)
  end
  def mirror=(val)
    @sprite.mirror=(val)
  end
  def dispose
    @sprite.dispose
  end
  def z=(val)
    @sprite.z=val
  end
  
  def totalFrames; @bitmap.animationFrames; end
  def toLastFrame; @bitmap.toFrame("last"); end
  def selected; end
    
  def setBitmap(file)
    if !@repeat
    @bitmap=AnimatedBitmapWrapperAnim.new(file)
    @sprite.ox=@bitmap.width/2
    @sprite.oy=@bitmap.height/2
    else
    @bitmap=AnimatedBitmapWrapper.new(file)
    @bitmap.setSpeed(2)
    @sprite.oy=@bitmap.height/2
    end
    @sprite.bitmap=@bitmap.bitmap.clone
  end
  
  
  def update
    return if @lock
    if @bitmap
      @bitmap.update
      @sprite.bitmap=@bitmap.bitmap.clone
    end
  end  
end

#===============================================================================
# 
# Starts here
# 
#===============================================================================

class PokemonTrainerCard_Scene
  
  def initialize
    @region = pbGetCurrentRegion(0)
    @region = 0 if @region >= LEADERINFO.length || LEADERINFO[@region].empty?
  end
  
  def update
    pbUpdateSpriteHash(@sprites)
    @sprites["bg"].x-=1.4
    @sprites["bg"].y-=1.4
    @sprites["bg"].x=0 if @sprites["bg"].x<=-64
    @sprites["bg"].y=0 if @sprites["bg"].y<=-64
  end

  def pbStartScene
    @sprites={}
    @viewport=Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z=99999
    @scene=0
    @showleader=false
    
    @sprites["bg"] = Sprite.new(@viewport)
    @sprites["bg"].bitmap = RPG::Cache.picture("trainercardbg")

    @sprites["card"]=IconSprite.new(40+64,32,@viewport)
  if $game_switches[200]
    if $Trainer.isFemale?
      @sprites["card"].setBitmap("Graphics/Pictures/TrainerCard/trainercard king")
    else
      @sprites["card"].setBitmap("Graphics/Pictures/TrainerCard/trainercard king")
    end
  else
  if $Trainer.isFemale?
    @sprites["card"].setBitmap("Graphics/Pictures/TrainerCard/trainercard1")#{$Trainer.cardlevel}")
  else
    @sprites["card"].setBitmap("Graphics/Pictures/TrainerCard/trainercard0")
    end
  end
    @sprites["card"].visible=true
    @sprites["darkbg"] = Sprite.new(@viewport)
    @sprites["darkbg"].bitmap = RPG::Cache.picture("Darkbg")
    @sprites["darkbg"].visible = false
    #图片坐标
  if $Trainer.isFemale?
    @sprites["trainer"]=IconSprite.new(362+64,64,@viewport)
    @sprites["trainer"].setBitmap("Graphics/Pictures/TrainerCard/intro_Girl")#{$Trainer.cardlevel}")
    @sprites["trainer"].zoom_x=1.0; @sprites["trainer"].zoom_y=1.0    
  else
    @sprites["trainer"]=IconSprite.new(362+64,64,@viewport)
    @sprites["trainer"].setBitmap("Graphics/Pictures/TrainerCard/intro_Boy")
    @sprites["trainer"].zoom_x=1.0; @sprites["trainer"].zoom_y=1.0    
  end
    #@totalframe=@sprites["trainer"].bitmap.width/@sprites["trainer"].bitmap.height
  
    #realwidth=@sprites["trainer"].bitmap.width/@totalframe
    
    #@sprites["trainer"].src_rect.set((@totalframe-1)*realwidth, 0,
    #realwidth,@sprites["trainer"].bitmap.height)
    
    @sprites["trainer"].z=2
    @sprites["trainer"].visible=true
    
    @sprites["bgbadge"] = Sprite.new(@viewport)
    @sprites["bgbadge"].bitmap = RPG::Cache.picture("TrainerCard/trainerbadges#{@region}")
    @sprites["bgbadge"].x=0
    @sprites["bgbadge"].y=0
    @sprites["bgbadge"].visible=false
    
    @sprites["blackleader"] = Sprite.new(@viewport)
    @sprites["blackleader"].bitmap = RPG::Cache.picture("TrainerCard/leaderfaces#{@region}")
    @sprites["blackleader"].x=32+48
    @sprites["blackleader"].y=27
    @sprites["blackleader"].src_rect.set(0,150,512,150)
    @sprites["blackleader"].visible=false

    @sprites["overlayfaces"]=BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
    @sprites["overlayfaces"].visible=false

    @sprites["gyminfo"] = Sprite.new(@viewport)
    @sprites["gyminfo"].bitmap = RPG::Cache.picture("TrainerCard/trainercardgyminfo")
    @sprites["gyminfo"].x=32+32
    @sprites["gyminfo"].y=160
    @sprites["gyminfo"].visible=false

    @sprites["normalbar"]=IconSprite.new(0,Graphics.height-48,@viewport)#
    @sprites["normalbar"].setBitmap("Graphics/Pictures/TrainerCard/normalbar")
    @sprites["cancelbuttom"]=Sprite.new(@viewport)
    @sprites["cancelbuttom"].bitmap=Cache.picture("TrainerCard/globalicons")
    @sprites["cancelbuttom"].x=Graphics.width-96
    @sprites["cancelbuttom"].y=Graphics.height-74
    @sprites["cancelbuttom"].src_rect.set(0, 0, 64, 64) 
    @sprites["badgeicon"] = Sprite.new(@viewport)
    @sprites["badgeicon"].bitmap = Cache.picture("TrainerCard/trainercardicons")
    @sprites["badgeicon"].x = 40
    @sprites["badgeicon"].y = 316+64
    @sprites["badgeicon"].src_rect.set(0,64,64,64)  
    
    @sprites["leadersprite"] = TrainerSpriteBW.new(412+32,270,@viewport)
    trainerfile=sprintf("Graphics/Trainers/%s",LEADERINFO[@region][0][0])
    @sprites["leadersprite"].setBitmap(trainerfile)
    @sprites["leadersprite"].visible=false
    @sprites["badgeinfo"] = Sprite.new(@viewport)
    @sprites["badgeinfo"].bitmap = Cache.picture("TrainerCard/badges#{@region}")
    @sprites["badgeinfo"].x=332
    @sprites["badgeinfo"].y=172
    @sprites["badgeinfo"].src_rect.set(0,0,64,147)  
    @sprites["badgeinfo"].zoom_x=0.5
    @sprites["badgeinfo"].zoom_y=0.5
    @sprites["badgeinfo"].visible=false


    @sprites["overlay"]=BitmapSprite.new(Graphics.width,Graphics.height,@viewport)
    @sprites["overlayleader"]=BitmapSprite.new(Graphics.width,Graphics.height,@viewport)

    @sprites["blacktran"] = Sprite.new(@viewport)
    @sprites["blacktran"].bitmap = RPG::Cache.picture("blackscreen")
    @sprites["blacktran"].zoom_y=2.5
    @sprites["blacktran"].y=Graphics.height
    @sprites["blacktran"].z=999999
   
    pbSetSystemFont(@sprites["overlay"].bitmap)
    pbSetSystemFont(@sprites["overlayleader"].bitmap)
    pbSetSystemFont(@sprites["overlayfaces"].bitmap)

    pbDrawTrainerCardFront
    if $PokemonGlobal.trainerRecording
      $PokemonGlobal.trainerRecording.play
    end
    pbFadeInAndShow(@sprites) { update }
  end
  
  def moveUpEffect
   loop do
     Graphics.update
     update
     @sprites["blacktran"].y-=46
     @sprites["blacktran"].y=0 if @sprites["blacktran"].y<0
     break if @sprites["blacktran"].y==0
   end
  end

  def moveDownEffect
   loop do
     Graphics.update
     update
     @sprites["blacktran"].y+=46
     @sprites["blacktran"].y=Graphics.height if @sprites["blacktran"].y>Graphics.height
     break if @sprites["blacktran"].y==Graphics.height
   end
 end
 
  def effectBadges
   10.times do
     Graphics.update
     update
     @sprites["blacktran"].opacity-=255/10
   end
 end
 
  def effectFront
   10.times do
     Graphics.update
     update
     @sprites["blacktran"].opacity+=255/10
   end
 end
 
  def pbDrawTrainerCardFront
    @scene=0
    pbClearLeaderInfo
    @sprites["bg"].bitmap = RPG::Cache.picture("TrainerCard/trainercardbg")
    @sprites["bgbadge"].visible=false
    @sprites["card"].visible=true
    @sprites["trainer"].visible=true
    @sprites["badgeicon"].src_rect.set(0,64,64,64)  
    @sprites["cancelbuttom"].src_rect.set(0, 0, 64, 64) 
    @sprites["blackleader"].visible=false
    @sprites["overlayfaces"].visible=false
    overlay=@sprites["overlay"].bitmap
    overlay.clear
    totalsec = Graphics.frame_count / Graphics.frame_rate
    hour = totalsec / 60 / 60
    min = totalsec / 60 % 60
    time=_ISPRINTF("{1:02d}:{2:02d}",hour,min)
    $PokemonGlobal.startTime=pbGetTimeNow if !$PokemonGlobal.startTime
   # starttime=_ISPRINTF("{1:s} {2:d}, {3:d}",
    starttime=_ISPRINTF("{1:d}年{2:s}{3:d}日",
       $PokemonGlobal.startTime.year,
       pbGetAbbrevMonthName($PokemonGlobal.startTime.mon),
       $PokemonGlobal.startTime.day)
    pubid=sprintf("%05d",$Trainer.publicID($Trainer.id))
    baseColor=Color.new(255,255,255)
    shadowColor=Color.new(181,189,206)
    plus=396
    textPositions=[
       [_INTL("名字"),80+64,67+32,0,baseColor,shadowColor],
       [_INTL("{1}",$Trainer.name),335+64,70+32,1,baseColor,shadowColor],
       [_INTL("ID No."),80+64,99+32,0,baseColor,shadowColor],
       [_INTL("{1}",pubid),335+64,99+32,1,baseColor,shadowColor],
       [_INTL("零花钱"),80+64,131+32,0,baseColor,shadowColor],
       [_INTL("${1}",$Trainer.money),335+64,131,1,baseColor,shadowColor],
       [_INTL("冒险时间"),80+64,245+32,0,baseColor,shadowColor],
       [time,496+64,242+32,1,baseColor,shadowColor],
       [_INTL("起始时间"),80+64,275+32,0,baseColor,shadowColor],
       [starttime,496+64,278+32,1,baseColor,shadowColor]
    ]
    if $Trainer.pokedex
      textPositions.push([_INTL("图鉴"),80+64,163+32,0,baseColor,shadowColor])
      textPositions.push([_ISPRINTF("{1:d}",$Trainer.pokedexOwned),
                          335+64,163+32,1,baseColor,shadowColor])
    end
    textPositions.push([_INTL("[C]打开徽章盒"),96,348+64,0,baseColor,shadowColor])
    pbDrawTextPositions(overlay,textPositions)
  end
  
  def pbDrawTrainerCardBadges
    @scene=1
    pbClearLeaderInfo
    @sprites["bg"].bitmap = RPG::Cache.picture("TrainerCard/trainercardbg2")
    @sprites["bgbadge"].visible=true
    @sprites["card"].visible=false
    @sprites["trainer"].visible=false
    @sprites["badgeicon"].src_rect.set(0, 0, 64, 64)  
    @sprites["cancelbuttom"].src_rect.set(0, 0, 64, 64) 
    @sprites["blackleader"].visible=true
    @sprites["overlayfaces"].visible=true

    overlay=@sprites["overlay"].bitmap
    overlay.clear
    
    totalsec = Graphics.frame_count / Graphics.frame_rate
    hour = totalsec / 60 / 60
    min = totalsec / 60 % 60
    time=_ISPRINTF("{1:02d}:{2:02d}",hour,min)
    $PokemonGlobal.startTime=pbGetTimeNow if !$PokemonGlobal.startTime
    starttime=_ISPRINTF("{1:s} {2:d}, {3:d}",
       pbGetAbbrevMonthName($PokemonGlobal.startTime.mon),
       $PokemonGlobal.startTime.day,
       $PokemonGlobal.startTime.year)
    pubid=sprintf("%05d",$Trainer.publicID($Trainer.id))
    baseColor=Color.new(255,255,255)
    shadowColor=Color.new(181,189,206)
    
    textPositions = [[_INTL("[C]返回训练师卡"),96+64,348+64,0,baseColor,shadowColor]]
    pbDrawTextPositions(overlay,textPositions)
    
    x=32+48
    x2=32+48
    #region= pbGetCurrentRegion(0) # Get the current region
    imagePositions=[]
    leaderPositions=[]
    for i in 0...8
      if $Trainer.badges[i+@region*4]
        imagePositions.push(["Graphics/Pictures/TrainerCard/badges#{@region}",x,180,i*65,0,64,147])
        leaderPositions.push(["Graphics/Pictures/TrainerCard/leaderfaces#{@region}",x2,26,i*64,0,64,150])
      end
      x2+=64
      x+=65
    end
    pbDrawImagePositions(@sprites["overlayfaces"].bitmap,leaderPositions)
    pbDrawImagePositions(overlay,imagePositions)
  end


  def pbShowLeaderInfo(leaders)
    @showleader=true
    leader=leaders.to_i
      # 安全检查
  if @region.nil? || @region < 0 || @region >= LEADERINFO.length
    return
  end
  if leader < 0 || leader >= LEADERINFO[@region].length
    return
  end
    @sprites["darkbg"].visible = true 
    @sprites["cancelbuttom"].src_rect.set(0, 64, 64, 64) 
    @sprites["overlay"].bitmap.clear
    @sprites["overlayleader"].bitmap.clear
    @sprites["leadersprite"].visible=false
    @sprites["badgeinfo"].visible=false
    if $Trainer.badges[leaders]
      trainerfile=sprintf("Graphics/Trainers/%s",LEADERINFO[@region][leader][0])
      @sprites["leadersprite"].setBitmap(trainerfile)
      @sprites["leadersprite"].visible=true
      pbPositionPokemonSprite(@sprites["leadersprite"],412+32,180)
      @sprites["badgeinfo"].src_rect.set(leader*64,0,64,147)  
      @sprites["badgeinfo"].visible=true
    end
    @sprites["gyminfo"].visible=true
    baseColor=Color.new(255,255,255)
    shadowColor=Color.new(181,189,206)
    leaderinfo=_INTL("{1} 道馆馆主\n",LEADERINFO[@region][leader][1])
    leaderinfo+=_INTL("{1}\n",LEADERINFO[@region][leader][2])
    phrase=_INTL("{1}",LEADERINFO[@region][leader][3])
    badgename=_INTL("{1}",LEADERINFO[@region][leader][4])
    
    drawTextEx(@sprites["overlayleader"].bitmap,48+32,178,Graphics.width-(42*2),5,leaderinfo,baseColor,shadowColor)
    drawTextEx(@sprites["overlayleader"].bitmap,48+32,258,Graphics.width-(42*2),5,phrase,baseColor,shadowColor)
    drawTextEx(@sprites["overlayleader"].bitmap,238+32,344+64,Graphics.width-(42*2),1,badgename,baseColor,shadowColor)
  end

  def pbClearLeaderInfo
    @showleader=false
    @sprites["overlay"].bitmap.clear
    @sprites["overlayleader"].bitmap.clear
    @sprites["leadersprite"].visible=false
    @sprites["gyminfo"].visible=false
    @sprites["badgeinfo"].visible=false
    @sprites["cancelbuttom"].src_rect.set(0, 0, 64, 64) 
  end

  def pbTrainerCard
    loop do
      Graphics.update
      Input.update
      self.update
if @scene==1 && $mouse.x>32+64 && $mouse.x<Graphics.width-32+64 &&
   $mouse.y>25 && $mouse.y<175 && Input.triggerex?(Input::Mouse_Left)
  pbShowLeaderInfo(($mouse.x-32-64)/64)
end
      if $mouse.leftClick?(@sprites["badgeicon"], 64, 64)|| Input.trigger?(Input::C)
        if @scene==0
        pbSEPlay("BW2MenuChoose")
        globalIconAnim(@sprites["badgeicon"])
        moveUpEffect
        pbDrawTrainerCardBadges
        effectBadges
      else
        pbSEPlay("BW2MenuChoose")
        globalIconAnim(@sprites["badgeicon"])
        effectFront
        pbDrawTrainerCardFront
        moveDownEffect
        end
      end
      if Input.trigger?(Input::B) || $mouse.leftClick?(@sprites["cancelbuttom"], 64, 64)
        if !@showleader
        globalIconAnim(@sprites["cancelbuttom"],0)
        pbSEPlay("BW2Cancel")
        break
      else
        globalIconAnim(@sprites["cancelbuttom"],1)
        pbDrawTrainerCardBadges
        end
      end
    end 
  end

  def pbEndScene
    pbFadeOutAndHide(@sprites) { update }
    pbDisposeSpriteHash(@sprites)
    @viewport.dispose
  end
end



class PokemonTrainerCardScreen
  def initialize(scene)
    @scene=scene
  end
  
def pbStartBadgeScreen
    @scene.pbStartScene
    @scene.pbDrawTrainerCardBadges
    loop do
      Graphics.update
      Input.update
      @scene.update
      if Input.trigger?(Input::C) || Input.trigger?(Input::B)
        pbSEPlay("BW2Cancel")
        break
      end
    end
    @scene.pbEndScene
  end
  def pbStartScreen
    @scene.pbStartScene
    @scene.pbTrainerCard
    @scene.pbEndScene
  end
end