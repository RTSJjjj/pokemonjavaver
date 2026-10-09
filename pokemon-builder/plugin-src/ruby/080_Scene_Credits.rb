# Backgrounds to show in credits. Found in Graphics/Titles/ folder
CreditsBackgroundList = ["credits1","credits2","credits3","credits4","credits5"]
CreditsMusic          = "Credits"
CreditsScrollSpeed    = 4
CreditsFrequency      = 9   # Number of seconds per credits slide
CREDITS_OUTLINE       = Color.new(0,0,128, 255)
CREDITS_SHADOW        = Color.new(0,0,0, 100)
CREDITS_FILL          = Color.new(255,255,255, 255)

#==============================================================================
# * Scene_Credits
#------------------------------------------------------------------------------
# Scrolls the credits you make below. Original Author unknown.
#
## Edited by MiDas Mike so it doesn't play over the Title, but runs by calling
# the following:
#    $scene = Scene_Credits.new
#
## New Edit 3/6/2007 11:14 PM by AvatarMonkeyKirby.
# Ok, what I've done is changed the part of the script that was supposed to make
# the credits automatically end so that way they actually end! Yes, they will
# actually end when the credits are finished! So, that will make the people you
# should give credit to now is: Unknown, MiDas Mike, and AvatarMonkeyKirby.
#                                             -sincerly yours,
#                                               Your Beloved
# Oh yea, and I also added a line of code that fades out the BGM so it fades
# sooner and smoother.
#
## New Edit 24/1/2012 by Maruno.
# Added the ability to split a line into two halves with <s>, with each half
# aligned towards the centre. Please also credit me if used.
#
## New Edit 22/2/2012 by Maruno.
# Credits now scroll properly when played with a zoom factor of 0.5. Music can
# now be defined. Credits can't be skipped during their first play.
#
## New Edit 25/3/2020 by Maruno.
# Scroll speed is now independent of frame rate. Now supports non-integer values
# for CreditsScrollSpeed.
#
## New Edit 21/8/2020 by Marin.
# Now automatically inserts the credits from the plugins that have been
# registered through the PluginManager module.
#==============================================================================

class Scene_Credits

# This next piece of code is the credits.
#Start Editing
CREDIT=<<_END_

=昕纪元=
影辞

（以下所有排名不分前后）

主作者

叶昕苍_Rice

=团队成员=

咪啪<s>子鹓
假队<s>Esplus
菲仔<s>Mars of Dimension
莫天<s>赤光
京墨-龙子御<s>Nana
猫漏爵士<s>东方
蛇克曼的模特<s>晓之将至
又兵衛<s>槟榔
Jince<s>源泉
山崖<s>赖泓宇
森咕咕<s>Fiooona
唯有沉默<s>银羽

=友情感谢=

Coco<s>草叶鸟
长条毛绒大尾立<s>多尔克
蔚然成风<s>默默无名的小鬼
飓<s>楠
鸭灭骡<s>over the Rainbow
Tang<s>幼芙利特
莉露·梅亚<s>向死而生
星空<s>リーリエ 
小源<s>猫虫
凌空羽路<s>Parasomnia
风衣暗夜<s>祈灵
虚空<s>小栾钓到鱼了
Little Sun<s>38888 
Jince<s>我狂故我在
南鸢<s>眷漓
无限<s>咖啡老爷
永远的祈祷<s>XLZ
落日余晖<s>维多
晚归<s>乌鸫
靖宇rainy<s>刨根问底的70君
洛梦缘<s>拾月
故雨<s>零零零零一二三
明月<s>玖肆一只猫猫
诡域<s>乌冬
花花<s>RoundAbout
史莱姆<s>米达糊

=感谢以下制作组的支持=

究极绿宝石制作组
零维E界制作组
水银制作组
命运制作组
宠物王国阴阳 制作组


=特别感谢=
永恒之焱-晓舟·DarkNight
超级绿宝石-海のLUGIA
漆黑的魅影-EbonyPhantom

=赞助=
九天后
墨染
。。。。。
零零零零一二三
旧日之人


◎感谢以下素材，脚本作者◎
AMVictory<s>AuthorBiggusWeeabus
Kyledove<s>Blaquaza
aXI<s>Turner1940
AuthorBiggusWeeabus<s>Wesley FG
wesleyfg<s>Espeon Scientist
phyromatical<s>BiggusWeeabus
pkmnalexandrite<s>fishbowlsoul90
DarkDragonn<s>LotusKing
Magiscarf<s>carchagui
Kymotonian<s>Wattpad
pano26_ddz9q0k<s>loppy654
seraawah-d4blk04<s>Avery
Vazquinho25<s>kiriaura
Akixakura16<s>AndyLinhares
shiney570<s>Articuno
Liltumospriter<s>DiegoWT
Lucidious89<s>KryptonLion
Boonzeet<s>badmanrhys123
ardicoozer<s>RadicalCharizard
Taiga<s>Spook
NeoriceisGood<s>TheAetherPlayer
Eskiss<s>Wobblebuns
0rcv0<s>wolfPP
Ploaj<s>Zerudez
KyleDove<s>Z-nogyroP
carchagui<s>DarkusShadow
Amacorala<s>LuigiTKo
Solacor<s>arclart
Dunymph<s>Ezerart
Dunrago<s>mej71
Gireamer<s>Vendily
Nitemarig<s>theo7722
Egoelk<s>princess-phoenix
Moosid<s>Scotsman333
Suprago<s>anonscribbler
Brumirage<s>JWNutz
rukarioruki94<s>TheRissingHootHoot
kyledove<s>larshadow
MultiDiegoDani<s>ghostzarc

插件作者。

{INSERTS_PLUGIN_CREDITS_DO_NOT_REMOVE}
"Pokémon Essentials" was created by:
Flameguru
Poccil (Peter O.)
Maruno

With contributions from:
AvatarMonkeyKirby<s>Marin
Boushy<s>MiDas Mike
Brother1440<s>Near Fantastica
FL.<s>PinkMan
Genzai Kawakami<s>Popper
help-14<s>Rataime
IceGod64<s>SoundSpawn
Jacob O. Wobbrock<s>the__end
KitsuneKouta<s>Venom12
Lisa Anthony<s>Wachunga
Luka S.J.<s>
and everyone else who helped out

"RPG Maker XP" by:
Enterbrain

Pokémon is owned by:
The Pokémon Company
Nintendo
Affiliated with Game Freak

这是一个非营利的粉丝自制游戏。
无任何侵权意图。
请支持正版游戏！


最后的最后
感谢游玩的您。


_END_
#Stop Editing


  def main
#-------------------------------
# Animated Background Setup
#-------------------------------
    @sprite = IconSprite.new(0,0)
    @sprite.z = ESMM_Config::MAP_Z + 1
    @backgroundList = CreditsBackgroundList
    @frameCounter = 0
    # Number of game frames per background frame
    @framesPerBackground = CreditsFrequency * 40
    @sprite.setBitmap("Graphics/Titles/"+@backgroundList[0])
#------------------
# Credits text Setup
#------------------
    plugin_credits = ""
    PluginManager.plugins.each do |plugin|
      pcred = PluginManager.credits(plugin)
      plugin_credits << "\"#{plugin}\" version #{PluginManager.version(plugin)}\n"
      if pcred.size >= 5
        plugin_credits << pcred[0] + "\n"
        i = 1
        until i >= pcred.size
          plugin_credits << pcred[i] + "<s>" + (pcred[i + 1] || "") + "\n"
          i += 2
        end
      else
        pcred.each do |name|
          plugin_credits << name + "\n"
        end
      end
      plugin_credits << "\n"
    end
    CREDIT.gsub!(/\{INSERTS_PLUGIN_CREDITS_DO_NOT_REMOVE\}/, plugin_credits)
    credit_lines = CREDIT.split(/\n/)
    credit_bitmap = Bitmap.new(Graphics.width,32 * credit_lines.size)
    credit_lines.each_index do |i|
      line = credit_lines[i]
      line = line.split("<s>")
      # LINE ADDED: If you use in your own game, you should remove this line
      pbSetSystemFont(credit_bitmap) # <--- This line was added
      xpos = 0
      align = 1 # Centre align
      linewidth = Graphics.width
      for j in 0...line.length
        if line.length>1
          xpos = (j==0) ? 0 : 20 + Graphics.width/2
          align = (j==0) ? 2 : 0 # Right align : left align
          linewidth = Graphics.width/2 - 20
        end
        credit_bitmap.font.color = CREDITS_SHADOW
        credit_bitmap.draw_text(xpos,i * 32 + 8,linewidth,32,line[j],align)
        credit_bitmap.font.color = CREDITS_OUTLINE
        credit_bitmap.draw_text(xpos + 2,i * 32 - 2,linewidth,32,line[j],align)
        credit_bitmap.draw_text(xpos,i * 32 - 2,linewidth,32,line[j],align)
        credit_bitmap.draw_text(xpos - 2,i * 32 - 2,linewidth,32,line[j],align)
        credit_bitmap.draw_text(xpos + 2,i * 32,linewidth,32,line[j],align)
        credit_bitmap.draw_text(xpos - 2,i * 32,linewidth,32,line[j],align)
        credit_bitmap.draw_text(xpos + 2,i * 32 + 2,linewidth,32,line[j],align)
        credit_bitmap.draw_text(xpos,i * 32 + 2,linewidth,32,line[j],align)
        credit_bitmap.draw_text(xpos - 2,i * 32 + 2,linewidth,32,line[j],align)
        credit_bitmap.font.color = CREDITS_FILL
        credit_bitmap.draw_text(xpos,i * 32,linewidth,32,line[j],align)
      end
    end
    @trim = Graphics.height/10
    @realOY = -(Graphics.height-@trim)   # -430
    @oyChangePerFrame = CreditsScrollSpeed*20.0/40
    @viewport = Viewport.new(0,@trim,Graphics.width,Graphics.height-(@trim*2))
    @viewport.z = @sprite.z + 1
    @credit_sprite = Sprite.new(@viewport)
    @credit_sprite.bitmap = credit_bitmap
    @credit_sprite.z      = 9998
    @credit_sprite.oy     = @realOY
    @bg_index = 0
    @zoom_adjustment = 1.0/$ResizeFactor
    @last_flag = false
#--------
# Setup
#--------
    # Stops all audio but background music
    previousBGM = $game_system.getPlayingBGM
    pbMEStop
    pbBGSStop
    pbSEStop
    pbBGMFade(2.0)
    pbBGMPlay(CreditsMusic)
    Graphics.transition(20)
    loop do
      Graphics.update
      Input.update
      update
      break if $scene != self
    end
    Graphics.freeze
    @sprite.dispose
    @credit_sprite.dispose
    $PokemonGlobal.creditsPlayed = true
    pbBGMPlay(previousBGM)
  end

  # Check if the credits should be cancelled
  def cancel?
    if Input.trigger?(Input::C) && $PokemonGlobal.creditsPlayed
      $scene = Scene_Map.new
      pbBGMFade(1.0)
      return true
    end
    return false
  end

  # Checks if credits bitmap has reached its ending point
  def last?
    if @realOY > @credit_sprite.bitmap.height + @trim
      $scene = ($game_map) ? Scene_Map.new : nil
      pbBGMFade(2.0)
      return true
    end
    return false
  end

  def update
    @frameCounter += 1
    # Go to next slide
    if @frameCounter >= @framesPerBackground
      @frameCounter -= @framesPerBackground
      @bg_index += 1
      @bg_index = 0 if @bg_index >= @backgroundList.length
      @sprite.setBitmap("Graphics/Titles/"+@backgroundList[@bg_index])
    end
    return if cancel?
    return if last?
    @realOY += @oyChangePerFrame
    @credit_sprite.oy = @realOY
  end
end
