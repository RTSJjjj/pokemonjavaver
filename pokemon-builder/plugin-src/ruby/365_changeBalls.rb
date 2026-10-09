def changeBalls
  #===================================================
  # 钱不够就直接结束
  pay_money = 1200
  if $Trainer.money < pay_money
    pbMessage(_INTL("\\G对不起，您的金钱不足{1}。", pay_money))
    return
  end
  #===================================================
  # 手动选择宝可梦，获取原球种
  pbChooseNonEggPokemon(1, 3)
  var = $game_variables[1]
  if var < 0 || var > 5     # 如果取消就直接结束
    pbMessage(_INTL("\\G欢迎下次光临。"))
    return
  end
  pkmn=pbGetPokemon(1)
  oldball = pbBallTypeToItem(pkmn.ballused)
  oldballname = PBItems.getName(oldball)
  #===================================================
  # 大师球和封印球不允许被换下和换上
  bannedBalls = [
    getConst(PBItems, :MASTERBALL), 
    getConst(PBItems, :PETBALL)
  ]
  if bannedBalls.include?(oldball)
    pbMessage(_INTL("\\G{1}的{2}是不能被换下的！", pkmn.name, oldballname))
    return
  end
  #===================================================
  # 准备新球种选项
  ballTypes = []
  ballNames = []
  for i in 0...$BallTypes.size
    ball = pbBallTypeToItem(i)
    next if bannedBalls.include?(ball)    # 去掉大师球和封印球
    ballTypes.push(ball)
    ballNames.push(PBItems.getName(ball))
  end
  #===================================================
  # 手动选择新球种
  loop do
    c = pbMessage(_INTL("\\G请选择新的球种"), ballNames, -1)
    # 如果取消就重新选择
    if c == -1
      pbMessage(_INTL("\\G欢迎下次光临。"))
      return
    end
    newball = ballTypes[c]
    newballname = PBItems.getName(newball)
    # 如果新球种和旧球种一样就重新选择
    if oldball == newball
      pbMessage(_INTL("\\G你选择的新球种和旧球种一样，\n无需更换哦。"))
      next
    end
    #===================================================
    # 扣钱
    $Trainer.money -= pay_money
    #===================================================
    # 执行更换球种
    pkmn.ballused = pbGetBallType(newball)
    pbMessage(_INTL("\\me[Pkmn get]\\G{1}的球种更换为{2}了！\\wtnp[30]", pkmn.name, newballname))
    #===================================================
    # 将原来的球放入背包，不需要可删除
    $PokemonBag.pbStoreItem(oldball)
    pbMessage(_INTL("{1}将换下来的{2}放进了背包。", $Trainer.name, oldballname))
    #===================================================
    break
  end
end
