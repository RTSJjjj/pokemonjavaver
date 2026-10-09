pbCompiler

class Scene_DebugIntro
  def main
    Graphics.transition(0)
    sscene = PokemonLoad_Scene.new
    sscreen = PokemonLoadScreen.new(sscene)
    sscreen.pbStartLoadScreen
    Graphics.freeze
  end
end

def pbCallTitle
  $miniMap.dispose if $miniMap
  $miniMap = nil
  return Scene_DebugIntro.new if $DEBUG && !ModularTitle::SHOW_IN_DEBUG
  # First parameter is an array of images in the Titles
  # directory without a file extension, to show before the
  # actual title screen.  Second parameter is the actual
  # title screen filename, also in Titles with no extension.
  return Scene_Intro.new
end

def mainFunction
    if defined?($CHEATS)
    if $CHEATS == true
      $CHEATS = false
    end
  end

  if $DEBUG
    pbCriticalCode { mainFunctionDebug }
  else
    mainFunctionDebug
  end
  return 1
end

def mainFunctionDebug
  check_font
  check_update
  #fuckyou
  begin
    getCurrentProcess = Win32API.new("kernel32.dll", "GetCurrentProcess", "", "l")
    setPriorityClass  = Win32API.new("kernel32.dll", "SetPriorityClass", %w(l i), "")
    setPriorityClass.call(getCurrentProcess.call(), 32768)   # "Above normal" priority class
    $data_animations    = pbLoadRxData("Data/Animations")
    $data_tilesets      = pbLoadRxData("Data/Tilesets")
    $data_common_events = pbLoadRxData("Data/CommonEvents")
    $data_system        = pbLoadRxData("Data/System")
    $game_system        = Game_System.new
    setScreenBorderName("border")   # Sets image file for the border
    Graphics.update
    Graphics.freeze
    $scene = pbCallTitle
    $scene.main until $scene.nil?
    Graphics.transition(20)
  rescue Hangup
    pbPrintException($!) if !$DEBUG
    pbEmergencySave
    raise
  end
end

def fuckyou
  $game_system.message_position = 1
  $game_system.message_frame = 1
  pbMessage("<ac>\\l[12]昕纪元影辞 正式官宣惹～！\n"+
          "本游戏只在\\c[2]叶昕苍B站、宝可饭堂、\n"+
          "贴吧与官方QQ群\\c[0]免费派送噜～！\n"+
          "如果你是在葫什么侠、二三几盒、\n"+
          "还有什么鱼啊猫啊PDD啊买来的…\n"+
          "\\c[0]那只能说一句：\n\\c[2]本可不是让你花钱当冤种的惹！\n"+
          "\\c[0]你那盘“正版”，连我的羽毛都碰不到厚！\n"+
          "还有那些倒卖、偷包、二改的憋池们\n"+
          "靠偷老娘练习带来的舞台光来卖钱？\n"+
          "\\c[2]拜托～你妈知道你在网上卖野改蹭热度嘛？\n"+
          "想妈了就看片，想爹了看看街，\n"+
          "\\c[0]别整天惦记昕纪元的排面惹，老娘不约～！</ac>")
  $game_system.message_position = 2
  $game_system.message_frame = 0
end

loop do
  retval = mainFunction
  if retval == 0   # failed
    loop do
      Graphics.update
    end
  elsif retval == 1   # ended successfully
    break
  end
end