#===============================================================================
# 战斗结束后归还已消耗的携带道具
# 适用于 Pokemon Essentials v18 风格脚本
#===============================================================================

class PokeBattle_Battle
  # 记录玩家宝可梦在战斗中消耗的携带道具
  def pbRecordConsumedHeldItem(pokemon, item)
    return if !pokemon || !item || item <= 0
    @consumedHeldItems ||= {}
    @consumedHeldItems[pokemon] ||= item
  end

  # 战斗结束后归还道具
  alias_method :return_consumed_items_pbEndOfBattle, :pbEndOfBattle

  def pbEndOfBattle
    result = return_consumed_items_pbEndOfBattle

    if @consumedHeldItems
      @consumedHeldItems.each do |pokemon, item|
        next if !pokemon
        # 不覆盖宝可梦当前持有的其他道具
        if !pokemon.item || pokemon.item <= 0
          pokemon.setItem(item)
        end
      end
      @consumedHeldItems.clear
    end

    return result
  end
end

class PokeBattle_Battler
  alias_method :return_consumed_items_pbConsumeItem, :pbConsumeItem

  def pbConsumeItem(recoverable=true, symbiosis=true, belch=true)
    consumed_item = @item

    # 只记录玩家拥有的宝可梦
    if consumed_item && consumed_item > 0 &&
       @battle && @battle.pbOwnedByPlayer?(@index)
      @battle.pbRecordConsumedHeldItem(@pokemon, consumed_item)
    end

    return_consumed_items_pbConsumeItem(recoverable, symbiosis, belch)
  end
end