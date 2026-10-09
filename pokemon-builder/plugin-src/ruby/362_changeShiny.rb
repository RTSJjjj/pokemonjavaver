def pbChangeShinyByNPC
  money_cost = 250000
  pbMessage(_INTL("\\G想改变哪只宝可梦的颜色呢？"))
  pbChooseNonEggPokemon(1, 3)
  var = $game_variables[1]
  if var < 0 || var > 5
    pbMessage(_INTL("\\G欢迎下次再来。"))
    return
  end

  pkmn = pbGetPokemon(1)

  if pkmn.shiny?
    confirm = pbConfirmMessage(_INTL("这已经是一只异色宝可梦了。\n要将其变回普通颜色吗？（免费）"))
    if confirm
      pkmn.makeNotShiny
      pbMessage(_INTL("\\me[Pkmn get]\\G{1}已恢复为普通颜色！", pkmn.name))
    else
      pbMessage(_INTL("\\G没有进行任何更改。"))
    end
  else
    if $Trainer.money < money_cost
      pbMessage(_INTL("\\G抱歉，您的金钱不足（需要{1}）。", money_cost))
      return
    end
    confirm = pbConfirmMessage(_INTL("是否花费{1}将{2}变为异色？", money_cost, pkmn.name))
    if confirm
      pkmn.makeShiny
      $Trainer.money -= money_cost
      pbMEPlay("Pkmn get")
      pbMessage(_INTL("\\me[Pkmn get]\\G{1}现在变为异色宝可梦了！", pkmn.name))
    else
      pbMessage(_INTL("\\G没有进行任何更改。"))
    end
  end
end
#===========================================================================
  def teachEggMoves
  price = 5000      # 价格
  if $Trainer.money < price
    pbMessage(sprintf("\\G对不起，您的金钱不足。"))
    return
  end
  if pbConfirmMessage(_INTL("\\G确定要支付{1}金钱教给宝可梦一个\n蛋招式吗？", price))
    pbChooseNonEggPokemon(1,3)
    var = $game_variables[1]
    if var < 0 || var> 5
      pbMessage(_INTL("\\G欢迎下次光临。"))
      return
    end
    pkmn = pbGetPokemon(1)
    babyspecies = pbGetBabySpecies(pkmn.species)
    o_eggMoves = pbGetSpeciesEggMoves(babyspecies, pkmn.form)
    loop do
      eggMoves = o_eggMoves.clone
      eggMoves.each_index do |i|
        eggMoves[i] = nil if pkmn.hasMove?(eggMoves[i])
      end
      eggMoves.compact!
      if eggMoves.empty?
        pbMessage(_INTL("\\G{1}没有可以学习的蛋招式。", pkmn.name))
        return
      end
      moveNames = []
      eggMoves.each do |m|
        moveNames.push(PBMoves.getName(m))
      end
      choice = pbMessage(_INTL("\\G要教给{1}哪一个蛋招式？", pkmn.name), moveNames, -1)
      if choice == -1
        pbMessage(_INTL("\\G欢迎下次光临。"))
        break
      end
      if pbLearnMove(pkmn, eggMoves[choice], false, true)
        eggMoves.delete_at(choice)
        $Trainer.money -= price
        pbMessage(_INTL("\\G\\se[Mart buy item]你支付了{1}金钱。\\wtnp[30]", price))
        break if eggMoves.empty? || $Trainer.money < price
      end
    end
  else
    pbMessage(_INTL("\\G欢迎下次光临。"))
  end
end