class PokemonSystem
  attr_accessor :show_miniMap
  attr_accessor :miniMap_opacity
  attr_accessor :miniMap_position
  attr_accessor :miniMap_size
  attr_accessor :miniMap_zoom
  attr_accessor :miniMap_border

  alias esmm_initialize initialize
  def initialize
    esmm_initialize
    @show_miniMap     = 1
    @miniMap_opacity  = 100
    @miniMap_position = 0
    @miniMap_size     = 1
    @miniMap_zoom     = 1
    @miniMap_border   = 0
  end

  def show_miniMap;     return @show_miniMap     || 1;    end
  def miniMap_opacity;  return @miniMap_opacity  || 100;  end
  def miniMap_position; return @miniMap_position || 0;    end
  def miniMap_size;     return @miniMap_size     || 1;    end
  def miniMap_zoom;     return @miniMap_zoom     || 1;    end
  def miniMap_border;   return @miniMap_border   || 0;    end

end

# 可自行设置的设置项
#===============================================================================
# Main options list
#===============================================================================
class Window_MiniMapOption < Window_DrawableCommand
  attr_reader :mustUpdateOptions

  def initialize(options,x,y,width,height)
    @options = options
    @nameBaseColor   = Color.new(24*8,15*8,0)
    @nameShadowColor = Color.new(31*8,22*8,10*8)
    @selBaseColor    = Color.new(31*8,6*8,3*8)
    @selShadowColor  = Color.new(31*8,17*8,16*8)
    @optvalues = []
    @mustUpdateOptions = false
    for i in 0...@options.length
      @optvalues[i] = 0
    end
    super(x,y,width,height)
  end

  def [](i)
    return @optvalues[i]
  end

  def []=(i,value)
    @optvalues[i] = value
    refresh
  end

  def setValueNoRefresh(i,value)
    @optvalues[i] = value
  end

  def itemCount
    return @options.length+1
  end

  def drawItem(index,_count,rect)
    rect = drawCursor(index,rect)
    optionname = (index==@options.length) ? _INTL("完成") : @options[index].name
    optionwidth = rect.width*9/20
    pbDrawShadowText(self.contents,rect.x,rect.y,optionwidth,rect.height,optionname,
       @nameBaseColor,@nameShadowColor)
    return if index>=@options.length
    if @options[index].is_a?(EnumOption)
      if @options[index].values.length>1
        totalwidth = 0
        for value in @options[index].values
          totalwidth += self.contents.text_size(value).width
        end
        spacing = (optionwidth-totalwidth)/(@options[index].values.length-1)
        spacing = 0 if spacing<0
        xpos = optionwidth+rect.x
        ivalue = 0
        for value in @options[index].values
          pbDrawShadowText(self.contents,xpos,rect.y,optionwidth,rect.height,value,
             (ivalue==self[index]) ? @selBaseColor : self.baseColor,
             (ivalue==self[index]) ? @selShadowColor : self.shadowColor
          )
          xpos += self.contents.text_size(value).width
          xpos += spacing
          ivalue += 1
        end
      else
        pbDrawShadowText(self.contents,rect.x+optionwidth,rect.y,optionwidth,rect.height,
           optionname,self.baseColor,self.shadowColor)
      end
    elsif @options[index].is_a?(NumberOption)
      value = _INTL("类型{1}/{2}",@options[index].optstart+self[index],
         @options[index].optend-@options[index].optstart+1)
      xpos = optionwidth+rect.x
      pbDrawShadowText(self.contents,xpos,rect.y,optionwidth,rect.height,value,
         @selBaseColor,@selShadowColor)
    elsif @options[index].is_a?(SliderOption)
      value = sprintf(" %d",@options[index].optend)
      sliderlength = optionwidth-self.contents.text_size(value).width
      xpos = optionwidth+rect.x
      self.contents.fill_rect(xpos,rect.y-2+rect.height/2,
         optionwidth-self.contents.text_size(value).width,4,self.baseColor)
      self.contents.fill_rect(
         xpos+(sliderlength-8)*(@options[index].optstart+self[index])/@options[index].optend,
         rect.y-8+rect.height/2,
         8,16,@selBaseColor)
      value = sprintf("%d",@options[index].optstart+self[index])
      xpos += optionwidth-self.contents.text_size(value).width
      pbDrawShadowText(self.contents,xpos,rect.y,optionwidth,rect.height,value,
         @selBaseColor,@selShadowColor)
    else
      value = @options[index].values[self[index]]
      xpos = optionwidth+rect.x
      pbDrawShadowText(self.contents,xpos,rect.y,optionwidth,rect.height,value,
         @selBaseColor,@selShadowColor)
    end
  end

  def update
    oldindex = self.index
    @mustUpdateOptions = false
    super
    dorefresh = (self.index!=oldindex)
    if self.active && self.index<=@options.length
      if Input.repeat?(Input::LEFT)
        self[self.index] = @options[self.index].prev(self[self.index]) if @options[self.index]
        dorefresh = true
        @mustUpdateOptions = true
      elsif Input.repeat?(Input::RIGHT)
        self[self.index] = @options[self.index].next(self[self.index]) if @options[self.index]
        dorefresh = true
        @mustUpdateOptions = true
      elsif Input.repeat?(Input::UP) || Input.repeat?(Input::DOWN)
        @mustUpdateOptions = true
      elsif Input.repeat?(Input::L) || Input.repeat?(Input::R)
        @mustUpdateOptions = true
      end
    end
    refresh if dorefresh
  end
end

#===============================================================================
# Options main screen
#===============================================================================
class MiniMapOption_Scene
  def pbUpdate
    pbUpdateSpriteHash(@sprites)
  end

  def pbStartScene
    @descs = {
      -1=> _INTL("选择是否显示小地图，需要拥有地图道具。\n快捷键: [Ctrl] + [M]"),
      0 => _INTL("调整小地图的透明度。"),
      1 => _INTL("选择小地图显示在屏幕中的位置。"),
      2 => _INTL("选择小地图本身的显示尺寸。\n快捷键: [=+] / [-_]"),
      3 => _INTL("选择小地图内容的缩放尺寸。\n快捷键: [Ctrl] + [=+] / [Ctrl] + [-_]"),
      4 => _INTL("调整小地图的边框颜色。"),
      6 => _INTL("完成小地图设置。")
    }
    @indexes = @descs.keys.sort!
    @sprites = {}
    @viewport = Viewport.new(0,0,Graphics.width,Graphics.height)
    @viewport.z = 99999
    @sprites["title"] = Window_UnformattedTextPokemon.newWithSize(
       _INTL("小地图设置"),0,0,Graphics.width,64,@viewport)
    @sprites["textbox"] = pbCreateMessageWindow
    @sprites["textbox"].text           = _INTL("{1}",@descs[-1])
    @sprites["textbox"].letterbyletter = false
    pbSetSystemFont(@sprites["textbox"].contents)
    # These are the different options in the game. To add an option, define a
    # setter and a getter for that option. To delete an option, comment it out
    # or delete it. The game's options may be placed in any order.
    @PokemonOptions = []
    @PokemonOptions.push(
       EnumOption.new(_INTL("显示"),[_INTL("是"),_INTL("否")],
         proc { $PokemonSystem.show_miniMap },
         proc { |value|
           if $PokemonSystem.show_miniMap != value
             $PokemonSystem.show_miniMap = value
             $miniMap = ESMiniMap.new if !$miniMap
             $miniMap.update_visible
           end
        }
      )
    )
    @PokemonOptions.push(
       SliderOption.new(_INTL("透明度"),0,100,10,
         proc { $PokemonSystem.miniMap_opacity },
         proc { |value|
           if $PokemonSystem.miniMap_opacity != value
             $PokemonSystem.miniMap_opacity = value
             $miniMap = ESMiniMap.new if !$miniMap
             $miniMap.update_opacity
           end
         }
       )
    )
    @PokemonOptions.push(
       EnumOption.new(_INTL("位置"),[_INTL("左上"),_INTL("右上"),_INTL("左下"),_INTL("右下")],
         proc { $PokemonSystem.miniMap_position },
         proc { |value|
           if $PokemonSystem.miniMap_position != value
             $PokemonSystem.miniMap_position = value
             $miniMap = ESMiniMap.new if !$miniMap
             $miniMap.update
           end
        }
      )
    )
    @PokemonOptions.push(
       EnumOption.new(_INTL("尺寸"),[_INTL("小"),_INTL("中"),_INTL("大"),_INTL("更大")],
         proc { $PokemonSystem.miniMap_size },
         proc { |value|
           if $PokemonSystem.miniMap_size != value
             $PokemonSystem.miniMap_size = value
             $miniMap = ESMiniMap.new if !$miniMap
             $miniMap.update
           end
        }
      )
    )
    @PokemonOptions.push(
       EnumOption.new(_INTL("缩放"),[_INTL("小"),_INTL("中"),_INTL("大"),_INTL("更大")],
         proc { $PokemonSystem.miniMap_zoom },
         proc { |value|
           if $PokemonSystem.miniMap_zoom != value
             $PokemonSystem.miniMap_zoom = value
             $miniMap = ESMiniMap.new if !$miniMap
             $miniMap.update
           end
        }
      )
    )
    @PokemonOptions.push(
       EnumOption.new(_INTL("边框颜色"),[_INTL("黑"),_INTL("白"),_INTL("红"),_INTL("绿"),_INTL("蓝"),_INTL("黄")],
         proc { $PokemonSystem.miniMap_border },
         proc { |value|
           if $PokemonSystem.miniMap_border != value
             $PokemonSystem.miniMap_border = value
             $miniMap = ESMiniMap.new if !$miniMap
             $miniMap.update
           end
        }
      )
    )
    @PokemonOptions = pbAddOnOptions(@PokemonOptions)
    @sprites["option"] = Window_MiniMapOption.new(@PokemonOptions,0,
       @sprites["title"].height,Graphics.width,
       Graphics.height-@sprites["title"].height-@sprites["textbox"].height)
    @sprites["option"].viewport = @viewport
    @sprites["option"].visible  = true
    # Get the values of each option
    for i in 0...@PokemonOptions.length
      @sprites["option"].setValueNoRefresh(i,(@PokemonOptions[i].get || 0))
    end
    @sprites["option"].refresh
    pbDeactivateWindows(@sprites)
    pbFadeInAndShow(@sprites) { pbUpdate }
  end

  def pbAddOnOptions(options)
    return options
  end

  def pbOptions
    oldSystemSkin = $PokemonSystem.frame      # Menu
    oldTextSkin   = $PokemonSystem.textskin   # Speech
    oldFont       = $PokemonSystem.font
    pbActivateWindow(@sprites,"option") {
      loop do
        Graphics.update
        Input.update
        pbUpdate
        if @sprites["option"].mustUpdateOptions
          # Set the values of each option
          for i in 0...@PokemonOptions.length
            @PokemonOptions[i].set(@sprites["option"][i])
          end
          if $PokemonSystem.textskin!=oldTextSkin
            @sprites["textbox"].setSkin(MessageConfig.pbGetSpeechFrame())
            oldTextSkin = $PokemonSystem.textskin
          end
          if $PokemonSystem.frame!=oldSystemSkin
            frame = MessageConfig.pbGetSystemFrame()
            @sprites["title"].setSkin(frame)
            @sprites["option"].setSkin(frame)
            oldSystemSkin = $PokemonSystem.frame
          end
          if $PokemonSystem.font!=oldFont
            oldFont = $PokemonSystem.font
          end
          if @sprites["option"].index==@PokemonOptions.length
            index = @descs.keys.max
          else
            index = @indexes[@sprites["option"].index]
          end
          @sprites["textbox"].text = _INTL("{1}", @descs[index])
        end
        if Input.trigger?(Input::B)
          break
        elsif Input.trigger?(Input::C)
          break if @sprites["option"].index==@PokemonOptions.length
        end
      end
    }
  end

  def pbEndScene
    pbPlayCloseMenuSE
    pbFadeOutAndHide(@sprites) { pbUpdate }
    # Set the values of each option
    for i in 0...@PokemonOptions.length
      @PokemonOptions[i].set(@sprites["option"][i])
    end
    pbDisposeMessageWindow(@sprites["textbox"])
    pbDisposeSpriteHash(@sprites)
    pbRefreshSceneMap
    @viewport.dispose
  end
end



#===============================================================================
#
#===============================================================================
class MiniMapOptionScreen
  def initialize(scene)
    @scene = scene
  end

  def pbStartScreen
    @scene.pbStartScene
    @scene.pbOptions
    @scene.pbEndScene
  end
end