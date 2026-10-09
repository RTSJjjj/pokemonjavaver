def pbMrHyper
  pbMessage(_INTL("\\G需要让哪只宝可梦进行特训呢？"))
	# 选择一只非蛋宝可梦
  pbChooseNonEggPokemon(1, 3)
  var = $game_variables[1]
	# 如果取消，直接结束
  if var < 0 || var > 5
    pbMessage(_INTL("\\G欢迎下次光临。"))
		return
	end
	pkmn = pbGetPokemon(1)
	#============================================================================
	# 如果低于50级，则不允许特训，直接结束
	# 如果不要这个限制，可以删掉上下#======之间的这一段
	if pkmn.level < 50
		pbMessage(_INTL("\\G抱歉，需要达到50级后才可以进行特训。"))
		return
	end
	#============================================================================
	# 可以特训的个体位置数组
	index_list = []
	# 选项数组
	choice_list = []
	# 遍历所有个体
	pkmn.iv.each_index do |i|
		# 如果个体值小于31
		if pkmn.iv[i] < 31
			# 将个体位置加入位置数组
			index_list.push(i)
			# 将个体名称加入选项数组
			choice_list.push(PBStats.getName(i))
		end
	end
	# 如果选项数组是空的，说明没有可以特训的个体，直接结束
	if choice_list.empty?
		pbMessage(_INTL("\\G{1}不需要再锻炼了！", pkmn.name))
		return
	end
	#============================================================================
	# 检查是否有银色王冠，有则保留原有选项，无则清空选项数组
  has_silver_cap = $PokemonBag.pbHasItem?(:BOTTLECAP)
  if !has_silver_cap
		choice_list.clear
	end
	
	# 如果背包有金色王冠，将"全部"加入选项数组
	if $PokemonBag.pbHasItem?(:GOLDBOTTLECAP)
		choice_list.push("全部")
	end
	
	# 如果选项数组是空的，说明没有银色王冠和金色王冠，直接结束
	if choice_list.empty?
		pbMessage(_INTL("\\G抱歉，您的银色王冠和金色王冠不足。"))
		return
	end
	#============================================================================
	# 如果选项只有1个而且就是"全部"，说明没有银色王冠，只有金色王冠
	if choice_list.size == 1 && choice_list[0] == "全部"
		# 询问是否要特训全部个体能力
		if pbConfirmMessage(_INTL("\\G需要{1}锻炼所有能力吗？", pkmn.name))
			# 遍历所有个体，将个体值改为31
			pkmn.iv.each_index { |i| pkmn.iv[i] = 31 }
			# 重新计算能力值
			pkmn.calcStats
			# 删除背包中的1个金色王冠
			$PokemonBag.pbDeleteItem(:GOLDBOTTLECAP)
			pbMessage(_INTL("\\me[Pkmn get]\\G{1}的所有能力都锻炼了！\\wtnp[20]", pkmn.name))
		end
		# 问完，无论是否特训全部了，都直接结束
		return
	end
	#============================================================================
	# 如果选项不止一个，进入循环
	loop do
		# 询问需要特训哪一项个体能力
		choice = pbMessage(_INTL("\\G需要{1}锻炼哪一项能力？", pkmn.name), choice_list, -1)
		# 如果取消，直接结束
		if choice == -1
			pbMessage(_INTL("\\G欢迎下次光临。"))
			return
		end
		# 根据选择结果，获取对应的能力名称
		ivName = choice_list[choice]
		#==========================================================================
		# 如果选择的是全部
		if ivName == "全部"
			# 再次检查，如果背包没有金色王冠，进入下一次循环
			if !$PokemonBag.pbHasItem?(:GOLDBOTTLECAP)
				pbMessage(_INTL("\\G抱歉，您的金色王冠不足。"))
				next
			end
			# 遍历所有个体，将个体值改为31
			pkmn.iv.each_index { |i| pkmn.iv[i] = 31 }
			# 重新计算能力值
			pkmn.calcStats
			# 删除背包中的1个金色王冠
			$PokemonBag.pbDeleteItem(:GOLDBOTTLECAP)
			pbMessage(_INTL("\\me[Pkmn get]\\G{1}的所有能力都锻炼了！\\wtnp[30]", pkmn.name))
			# 全部都特训了，那么就不需要继续了，直接结束
			return
		end
		#==========================================================================
		# 如果选择的不是全部
    # 再次检查，如果银色王冠不足，进入下一次循环
    if !$PokemonBag.pbHasItem?(:BOTTLECAP)
			pbMessage(_INTL("\\G抱歉，您的银色王冠不足。"))
			next
		end
		# 根据选择结果，获取需要特训的个体位置
		index = index_list[choice]
		# 如果这一项个体是31，就不需要特训了，进入下一次循环
		if pkmn.iv[index] == 31
			pbMessage(_INTL("\\G{1}的{2}能力不需要锻炼了！", pkmn.name, ivName))
			next
		end
		# 将对应个体改为31
		pkmn.iv[index] = 31
		# 重新计算能力值
		pkmn.calcStats
    # 扣除银色王冠
    $PokemonBag.pbDeleteItem(:BOTTLECAP)
		pbMessage(_INTL("\\me[Pkmn get]\\G{1}的{2}能力锻炼了！\\wtnp[30]", pkmn.name, ivName))
		# 因为这一项已经特训过，不需要再特训了，所以删除选项数组中的这一项
		choice_list.delete_at(choice)
		# 同上，所以删除可以特训的个体位置数组中的这一项
		index_list.delete_at(choice)
		# 经过一系列操作后，如果选项数组是空的，或者只剩下一个全部，说明没有可以特训的个体，直接结束
		return if choice_list.empty? || choice_list.size == 1 && choice_list[0] == "全部"
	end
end