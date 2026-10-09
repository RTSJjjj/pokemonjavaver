#===============================================================================
# PC menus
#===============================================================================
def pbPCItemStorage
  command = 0
  loop do
    command = pbShowCommandsWithHelp(nil,
       [_INTL("取出道具"),
       _INTL("储存道具"),
       _INTL("扔掉道具"),
       _INTL("退出")],
       [_INTL("从电脑中取出物品。"),
       _INTL("将道具储存在电脑中。"),
       _INTL("扔掉电脑里储存的道具。"),
       _INTL("返回上一级。")],-1,command
    )
    case command
    when 0   # Withdraw Item
      if !$PokemonGlobal.pcItemStorage
        $PokemonGlobal.pcItemStorage = PCItemStorage.new
      end
      if $PokemonGlobal.pcItemStorage.empty?
        pbMessage(_INTL("没有道具。"))
      else
        pbFadeOutIn {
          scene = WithdrawItemScene.new
          screen = PokemonBagScreen.new(scene,$PokemonBag)
          screen.pbWithdrawItemScreen
        }
      end
    when 1   # Deposit Item
      pbFadeOutIn {
        scene = PokemonBag_Scene.new
        screen = PokemonBagScreen.new(scene,$PokemonBag)
        screen.pbDepositItemScreen
      }
    when 2   # Toss Item
      if !$PokemonGlobal.pcItemStorage
        $PokemonGlobal.pcItemStorage = PCItemStorage.new
      end
      if $PokemonGlobal.pcItemStorage.empty?
        pbMessage(_INTL("没有道具。"))
      else
        pbFadeOutIn {
          scene = TossItemScene.new
          screen = PokemonBagScreen.new(scene,$PokemonBag)
          screen.pbTossItemScreen
        }
      end
    else
      break
    end
  end
end

def pbPCMailbox
  if !$PokemonGlobal.mailbox || $PokemonGlobal.mailbox.length==0
    pbMessage(_INTL("电脑里没有邮件。"))
  else
    loop do
      command = 0
      commands=[]
      for mail in $PokemonGlobal.mailbox
        commands.push(mail.sender)
      end
      commands.push(_INTL("退出"))
      command = pbShowCommands(nil,commands,-1,command)
      if command>=0 && command<$PokemonGlobal.mailbox.length
        mailIndex = command
        commandMail = pbMessage(_INTL("如何处理{1}的邮件？",
           $PokemonGlobal.mailbox[mailIndex].sender),[
           _INTL("阅读邮件"),
           _INTL("存放至背包"),
           _INTL("给予"),
           _INTL("取消")
           ],-1)
        case commandMail
        when 0   # Read
          pbFadeOutIn {
            pbDisplayMail($PokemonGlobal.mailbox[mailIndex])
          }
        when 1   # Move to Bag
          if pbConfirmMessage(_INTL("邮件上的信息将会被抹去。\n确定吗？"))
            if $PokemonBag.pbStoreItem($PokemonGlobal.mailbox[mailIndex].item)
              pbMessage(_INTL("邮件放到背包里了，\n邮件上的信息被抹去了。"))
              $PokemonGlobal.mailbox.delete_at(mailIndex)
            else
              pbMessage(_INTL("背包已经满了。"))
            end
          end
        when 2   # Give
          pbFadeOutIn {
            sscene = PokemonParty_Scene.new
            sscreen = PokemonPartyScreen.new(sscene,$Trainer.party)
            sscreen.pbPokemonGiveMailScreen(mailIndex)
          }
        end
      else
        break
      end
    end
  end
end

def pbTrainerPCMenu
  command = 0
  loop do
    command = pbMessage(_INTL("要做什么？"),[
       _INTL("整理道具"),
       _INTL("邮件"),
       _INTL("关闭电脑")
       ],-1,nil,command)
    case command
    when 0; pbPCItemStorage
    when 1; pbPCMailbox
    else; break
    end
  end
end



class TrainerPC
  def shouldShow?
    return true
  end

  def name
    return _INTL("{1}的电脑",$Trainer.name)
  end

  def access
    pbMessage(_INTL("\\se[PC access]访问了{1}的电脑。",$Trainer.name))
    pbTrainerPCMenu
  end
end



def pbGetStorageCreator
  creator = pbStorageCreator
  creator = _INTL("Bill") if !creator || creator==""
  return creator
end



class StorageSystemPC
  def shouldShow?
    return true
  end

  def name
    if $PokemonGlobal.seenStorageCreator
      return _INTL("{1}'的电脑",pbGetStorageCreator)
    else
      return _INTL("精灵寄存系统")
    end
  end

  def access
    pbMessage(_INTL("\\se[PC access]登录了精灵寄存服务！"))
    command = 0
    loop do
      command = pbShowCommandsWithHelp(nil,
         [_INTL("整理盒子"),
         _INTL("取出宝可梦"),
         _INTL("存放宝可梦"),
         _INTL("登出")],
         [_INTL("整理您存在盒子和队伍里的宝可梦。"),
         _INTL("取出您存在盒子里的宝可梦。"),
         _INTL("把您队伍里的宝可梦存放在盒子里。"),
         _INTL("关闭服务")],-1,command
      )
      if command>=0 && command<3
        if command==1   # Withdraw
          if $PokemonStorage.party.length>=6
            pbMessage(_INTL("您的队伍满了！"))
            next
          end
        elsif command==2   # Deposit
          count=0
          for p in $PokemonStorage.party
            count += 1 if p && !p.egg? && p.hp>0
          end
          if count<=1
            pbMessage(_INTL("不能存放最后的宝可梦！"))
            next
          end
        end
        pbFadeOutIn {
          scene = PokemonStorageScene.new
          screen = PokemonStorageScreen.new(scene,$PokemonStorage)
          screen.pbStartScreen(command)
        }
      else
        break
      end
    end
  end
end



def pbTrainerPC
  pbMessage(_INTL("\\se[PC open]{1}打开了电脑",$Trainer.name))
  pbTrainerPCMenu
  pbSEPlay("PC close")
end

def pbPokeCenterPC
  pbMessage(_INTL("\\se[PC open]{1}打开了电脑",$Trainer.name))
  command = 0
  loop do
    commands = PokemonPCList.getCommandList
    command = pbMessage(_INTL("要登录哪一个服务？"),commands,
       commands.length,nil,command)
    break if !PokemonPCList.callCommand(command)
  end
  pbSEPlay("PC close")
end



module PokemonPCList
  @@pclist = []

  def self.registerPC(pc)
    @@pclist.push(pc)
  end

  def self.getCommandList
    commands = []
    for pc in @@pclist
      commands.push(pc.name) if pc.shouldShow?
    end
    commands.push(_INTL("关闭电脑"))
    return commands
  end

  def self.callCommand(cmd)
    return false if cmd<0 || cmd>=@@pclist.length
    i = 0
    for pc in @@pclist
      next if !pc.shouldShow?
      if i==cmd
        pc.access
        return true
      end
      i += 1
    end
    return false
  end
end



PokemonPCList.registerPC(StorageSystemPC.new)
PokemonPCList.registerPC(TrainerPC.new)
