#===============================================================================
# Mystery Gift system
# By Maruno
#===============================================================================
# This url is the location of an example Mystery Gift file.
# You should change it to your file's url once you upload it.
# NOTE: Essentials cannot handle https addresses. You must use a http address.
#===============================================================================
MYSTERY_GIFT_URL = "http://images1.wikia.nocookie.net/pokemonessentials/images/e/e7/MysteryGift.txt"
# MYSTERY_GIFT_URL = "http://pastebin.com/raw/w6BqqUsm"



class PokeBattle_Trainer
  attr_writer :mysterygiftaccess   # Whether MG can be used from load screen
  attr_writer :mysterygift         # Variable that stores downloaded MG data

  def mysterygiftaccess
    return @mysterygiftaccess || false
  end

  def mysterygift
    return @mysterygift || []
  end
end



#===============================================================================
# Creating a new Mystery Gift for the Master file, and editing an existing one.
#===============================================================================
# type: 0=Pokémon; 1 or higher=item (is the item's quantity).
# item: The thing being turned into a Mystery Gift (Pokémon object or item ID).
def pbEditMysteryGift(type,item,id=0,giftname="")
  begin
    if type==0   # Pokémon
      commands=[_INTL("神秘礼物"),
                _INTL("遥远的地方")]
      commands.push(item.obtainText) if item.obtainText && item.obtainText!=""
      commands.push(_INTL("[Custom]"))
      loop do
        command=pbMessage(
           _INTL("选择短语作为礼物宝可梦的来源。"),commands,-1)
        if command<0
          return nil if pbConfirmMessage(_INTL("停止编辑这个礼物？"))
        elsif command<commands.length-1
          item.obtainText=commands[command]
          break
        elsif command==commands.length-1
          obtainname=pbMessageFreeText(_INTL("输入短语。"),"",false,30)
          if obtainname!=""
            item.obtainText=obtainname
            break
          end
          return nil if pbConfirmMessage(_INTL("停止编辑这个礼物？"))
        end
      end
    elsif type>0   # Item
      params=ChooseNumberParams.new
      params.setRange(1,99999)
      params.setDefaultValue(type)
      params.setCancelValue(0)
      loop do
        newtype=pbMessageChooseNumber(_INTL("选择数量。"),params)
        if newtype==0
          return nil if pbConfirmMessage(_INTL("停止编辑这个礼物？"))
        else
          type=newtype
          break
        end
      end
    end
    if id==0
      master=[]; idlist=[]
      if safeExists?("MysteryGiftMaster.txt")
        master=IO.read("MysteryGiftMaster.txt")
        master=pbMysteryGiftDecrypt(master)
      end
      for i in master; idlist.push(i[0]); end
      params=ChooseNumberParams.new
      params.setRange(0,99999)
      params.setDefaultValue(id)
      params.setCancelValue(0)
      loop do
        newid=pbMessageChooseNumber(_INTL("请为此礼物选择唯一ID。"),params)
        if newid==0
          return nil if pbConfirmMessage(_INTL("停止编辑这个礼物？"))
        else
          if idlist.include?(newid)
            pbMessage(_INTL("ID已被神秘礼物使用了。"))
          else
            id=newid
            break
          end
        end
      end
    end
    loop do
      newgiftname=pbMessageFreeText(_INTL("输入礼物的名称。"),giftname,false,250)
      if newgiftname!=""
        giftname=newgiftname
        break
      end
      return nil if pbConfirmMessage(_INTL("停止编辑这个礼物？"))
    end
    return [id,type,item,giftname]
  rescue
    pbMessage(_INTL("无法编辑礼物。"))
    return nil
  end
end

def pbCreateMysteryGift(type,item)
  gift=pbEditMysteryGift(type,item)
  if !gift
    pbMessage(_INTL("没有创建礼物。"))
  else
    begin
      if safeExists?("MysteryGiftMaster.txt")
        master=IO.read("MysteryGiftMaster.txt")
        master=pbMysteryGiftDecrypt(master)
        master.push(gift)
      else
        master=[gift]
      end
      string=pbMysteryGiftEncrypt(master)
      File.open("MysteryGiftMaster.txt","wb") { |f| f.write(string) }
      pbMessage(_INTL("礼物保存到\nMysteryGiftMaster.txt。"))
    rescue
      pbMessage(_INTL("没有把礼物保存到\nMysteryGiftMaster.txt。"))
    end
  end
end

#===============================================================================
# Debug option for managing gifts in the Master file and exporting them to a
# file to be uploaded.
#===============================================================================
def pbManageMysteryGifts
  if !safeExists?("MysteryGiftMaster.txt")
    pbMessage(_INTL("没有定义神秘礼物。"))
    return
  end
  # Load all gifts from the Master file.
  master=IO.read("MysteryGiftMaster.txt")
  master=pbMysteryGiftDecrypt(master)
  if !master || !master.is_a?(Array) || master.length==0
    pbMessage(_INTL("没有定义神秘礼物。"))
    return
  end
  # Download all gifts from online
  msgwindow=pbCreateMessageWindow
  pbMessageDisplay(msgwindow,_INTL("Searching for online gifts...\\wtnp[0]"))
  online=pbDownloadToString(MYSTERY_GIFT_URL)
  pbDisposeMessageWindow(msgwindow)
  if online==""
    pbMessage(_INTL("No online Mystery Gifts found.\\wtnp[20]"))
    online=[]
  else
    pbMessage(_INTL("Online Mystery Gifts found.\\wtnp[20]"))
    online=pbMysteryGiftDecrypt(online)
    t=[]
    for gift in online; t.push(gift[0]); end
    online=t
  end
  # Show list of all gifts.
  command=0
  loop do
    commands=pbRefreshMGCommands(master,online)
    command=pbMessage(_INTL("\\ts[]Manage Mystery Gifts (X=online)."),commands,-1,nil,command)
    # Gift chosen
    if command==-1 || command==commands.length-1
      break
    elsif command==commands.length-2
      begin
        newfile=[]
        for gift in master
          newfile.push(gift) if online.include?(gift[0])
        end
        string=pbMysteryGiftEncrypt(newfile)
        File.open("MysteryGift.txt","wb") { |f| f.write(string) }
        pbMessage(_INTL("礼物被保存到\nMysteryGift.txt。"))
        pbMessage(_INTL("上传MysteryGift.txt\n到互联网上了。"))
      rescue
        pbMessage(_INTL("没有把礼物保存到\nMysteryGift.txt."))
      end
    elsif command>=0 && command<commands.length-2
      cmd=0
      loop do
        commands=pbRefreshMGCommands(master,online)
        gift=master[command]
        cmds=[_INTL("切换 开启/离线的"),
              _INTL("编辑"),
              _INTL("收到"),
              _INTL("删除"),
              _INTL("取消")]
        cmd=pbMessage("\\ts[]"+commands[command],cmds,-1,nil,cmd)
        if cmd==-1 || cmd==cmds.length-1
          break
        elsif cmd==0   # Toggle on/offline
          if online.include?(gift[0])
            for i in 0...online.length
              online[i]=nil if online[i]==gift[0]
            end
            online.compact!
          else
            online.push(gift[0])
          end
        elsif cmd==1   # Edit
          newgift=pbEditMysteryGift(gift[1],gift[2],gift[0],gift[3])
          master[command]=newgift if newgift
        elsif cmd==2   # Receive
          replaced=false
          for i in 0...$Trainer.mysterygift.length
            if $Trainer.mysterygift[i][0]==gift[0]
              $Trainer.mysterygift[i]=gift; replaced=true
            end
          end
          $Trainer.mysterygift.push(gift) if !replaced
          pbReceiveMysteryGift(gift[0])
        elsif cmd==3   # Delete
          if pbConfirmMessage(_INTL("确定要删除此礼物吗？"))
            master[command]=nil
            master.compact!
          end
          break
        end
      end
    end
  end
end

def pbRefreshMGCommands(master,online)
  commands=[]
  for gift in master
    itemname="BLANK"
    if gift[1]==0
      itemname=PBSpecies.getName(gift[2].species)
    elsif gift[1]>0
      itemname=PBItems.getName(gift[2])+sprintf(" x%d",gift[1])
    end
    ontext=["[  ]","[X]"][(online.include?(gift[0])) ? 1 : 0]
    commands.push(_INTL("{1} {2}：{3} ({4})",ontext,gift[0],gift[3],itemname))
  end
  commands.push(_INTL("导出选定的文件"))
  commands.push(_INTL("取消"))
  return commands
end

#===============================================================================
# Downloads all available Mystery Gifts that haven't been downloaded yet.
#===============================================================================
# Called from the Continue/New Game screen.
def pbDownloadMysteryGift(trainer)
  sprites={}
  viewport=Viewport.new(0,0,Graphics.width,Graphics.height)
  viewport.z=99999
  addBackgroundPlane(sprites,"background","mysteryGiftbg",viewport)
  pbFadeInAndShow(sprites)
  sprites["msgwindow"]=pbCreateMessageWindow
  pbMessageDisplay(sprites["msgwindow"],_INTL("Searching for a gift.\nPlease wait...\\wtnp[0]"))
  string=pbDownloadToString(MYSTERY_GIFT_URL)
  if string==""
    pbMessageDisplay(sprites["msgwindow"],_INTL("没有新的可用礼物。"))
  else
    online=pbMysteryGiftDecrypt(string)
    pending=[]
    for gift in online
      notgot=true
      for j in trainer.mysterygift
        notgot=false if j[0]==gift[0]
      end
      pending.push(gift) if notgot
    end
    if pending.length==0
      pbMessageDisplay(sprites["msgwindow"],_INTL("没有新的可用礼物。"))
    else
      loop do
        commands=[]
        for gift in pending; commands.push(gift[3]); end
        commands.push(_INTL("取消"))
        pbMessageDisplay(sprites["msgwindow"],_INTL("Choose the gift you want to receive.\\wtnp[0]"))
        command=pbShowCommands(sprites["msgwindow"],commands,-1)
        if command==-1 || command==commands.length-1
          break
        else
          gift=pending[command]
          sprites["msgwindow"].visible=false
          if gift[1]==0
            sprite=PokemonSprite.new(viewport)
            sprite.setOffset(PictureOrigin::Center)
            sprite.setPokemonBitmap(gift[2])
            sprite.x=Graphics.width/2
            sprite.y=-sprite.bitmap.height/2
          else
            sprite=ItemIconSprite.new(0,0,gift[2],viewport)
            sprite.x=Graphics.width/2
            sprite.y=-sprite.height/2
          end
          distanceDiff = 8*20/Graphics.frame_rate
          loop do
            Graphics.update
            Input.update
            sprite.update
            sprite.y+=distanceDiff
            break if sprite.y>=Graphics.height/2
          end
          pbMEPlay("Battle capture success")
          (Graphics.frame_rate*3).times do
            Graphics.update
            Input.update
            sprite.update
            pbUpdateSceneMap
          end
          sprites["msgwindow"].visible=true
          pbMessageDisplay(sprites["msgwindow"],_INTL("收到礼物了！")) { sprite.update }
          pbMessageDisplay(sprites["msgwindow"],_INTL("请到售卖道具的售货员那里领取你\n的礼物。")) { sprite.update }
          trainer.mysterygift.push(gift)
          pending[command]=nil; pending.compact!
          opacityDiff = 16*20/Graphics.frame_rate
          loop do
            Graphics.update
            Input.update
            sprite.update
            sprite.opacity-=opacityDiff
            break if sprite.opacity<=0
          end
          sprite.dispose
        end
        if pending.length==0
          pbMessageDisplay(sprites["msgwindow"],_INTL("没有新的可用礼物。"))
          break
        end
      end
    end
  end
  pbFadeOutAndHide(sprites)
  pbDisposeMessageWindow(sprites["msgwindow"])
  pbDisposeSpriteHash(sprites)
  viewport.dispose
  return trainer
end

#===============================================================================
# Converts an array of gifts into a string and back.
#===============================================================================
def pbMysteryGiftEncrypt(gift)
  ret=[Zlib::Deflate.deflate(Marshal.dump(gift))].pack("m")
  return ret
end

def pbMysteryGiftDecrypt(gift)
  return [] if gift==""
  ret=Marshal.restore(Zlib::Inflate.inflate(gift.unpack("m")[0]))
  return ret
end

#===============================================================================
# Collecting a Mystery Gift from the deliveryman.
#===============================================================================
def pbNextMysteryGiftID
  for i in $Trainer.mysterygift
    return i[0] if i.length>1
  end
  return 0
end

def pbReceiveMysteryGift(id)
  index=-1
  for i in 0...$Trainer.mysterygift.length
    if $Trainer.mysterygift[i][0]==id && $Trainer.mysterygift[i].length>1
      index=i
      break
    end
  end
  if index==-1
    pbMessage(_INTL("找不到ID为{1}的无主神秘礼物。",id))
    return false
  end
  gift=$Trainer.mysterygift[index]
  if gift[1]==0
    pID=rand(256)
    pID|=rand(256)<<8
    pID|=rand(256)<<16
    pID|=rand(256)<<24
    gift[2].personalID=pID
    gift[2].calcStats
    time=pbGetTimeNow
    gift[2].timeReceived=time.getgm.to_i
    gift[2].obtainMode=4   # Fateful encounter
    gift[2].pbRecordFirstMoves
    if $game_map
      gift[2].obtainMap=$game_map.map_id
      gift[2].obtainLevel=gift[2].level
    else
      gift[2].obtainMap=0
      gift[2].obtainLevel=gift[2].level
    end
    if pbAddPokemonSilent(gift[2])
      pbMessage(_INTL("\\me[Pkmn get]{1} received {2}!",$Trainer.name,gift[2].name))
      $Trainer.mysterygift[index]=[id]
      return true
    end
  elsif gift[1]>0
    if $PokemonBag.pbCanStore?(gift[2],gift[1])
      $PokemonBag.pbStoreItem(gift[2],gift[1])
      item=gift[2]; qty=gift[1]
      itemname=(qty>1) ? PBItems.getNamePlural(item) : PBItems.getName(item)
      if isConst?(item,PBItems,:LEFTOVERS)
        pbMessage(_INTL("\\me[Item get]You obtained some \\c[1]{1}\\c[0]!\\wtnp[30]",itemname))
      elsif pbIsMachine?(item)   # TM or HM
        pbMessage(_INTL("\\me[Item get]You obtained \\c[1]{1} {2}\\c[0]!\\wtnp[30]",itemname,PBMoves.getName(pbGetMachine(item))))
      elsif qty>1
        pbMessage(_INTL("\\me[Item get]You obtained {1} \\c[1]{2}\\c[0]!\\wtnp[30]",qty,itemname))
      elsif itemname.starts_with_vowel?
        pbMessage(_INTL("\\me[Item get]You obtained an \\c[1]{1}\\c[0]!\\wtnp[30]",itemname))
      else
        pbMessage(_INTL("\\me[Item get]You obtained a \\c[1]{1}\\c[0]!\\wtnp[30]",itemname))
      end
      $Trainer.mysterygift[index]=[id]
      return true
    end
  end
  return false
end
