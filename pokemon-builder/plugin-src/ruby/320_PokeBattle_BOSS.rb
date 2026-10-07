
# 根据BOSS分级决定BOSS的HP倍数
BOSS_HP_RANK          = [1, 1, 2, 5, 10, 15, 15, 20]
class PokeBattle_Pokemon
  # BOSS分级，
  # 默认1，精英怪填2，BOSS填3/4/5/6/7，不要填小于1或大于7的数
  # 使用方法：在定点pkmn战斗的脚本写pkmn.battleRank=X
  attr_accessor :battleRank
  
  alias boss_initialize initialize
  def initialize(species,level,player=nil,withMoves=true)
    @battleRank = 1
    boss_initialize(species,level,player,withMoves)
  end
  
  def battleRank
    return @battleRank || 1
  end
  
  def battleRank=(value)
    @battleRank = value
  end
end

class PokeBattle_DamageState
  attr_accessor :bossHP       # BOSS单次最多承受基础最大HP的伤害
  
  alias boss_initialize initialize
  def initialize
    boss_initialize
    @bossHP             = 0
  end
end  

class PokeBattle_Battle
  
  def pbBossBuffPhase
    eachBattler do |b|
      next if !b.is_a?(PokeBattle_Battler)
      next if b.pbOwnedByPlayer?
      # BOSS恢复异常状态和清强化
      rank = b.pokemon.battleRank
      if rank > 2
        if @turnCount == 0
          randomUp = []
          PBStats.eachMainBattleStat do |s|
            randomUp.push(s) if b.pbCanRaiseStatStage?(s,b)
          end
          next if randomUp.length==0
          r = pbRandom(randomUp.length)
          stat = randomUp[r]
          b.pbRaiseStatStageBasic(stat, 1, true)
          pbCommonAnimation("StatUp", b)
          pbDisplayBrief(_INTL("<c3=FFEE88,FF6600>{1}凭借它的力量\n提高了{2}！</c3>",
                                b.pbThis, PBStats.getName(stat)))
          pbWait(80)
          next
        end
        ret = rand(9000)
        ret = 3999 if [1, 2].include?(b.effects[PBEffects::PerishSong])
        if ret < 4000
          cry = pbCryFile(b.pokemon)
          pbSEPlay(cry) if cry
          if ret < 1000
            randomUp = []
            PBStats.eachMainBattleStat do |s|
              randomUp.push(s) if b.pbCanRaiseStatStage?(s,b)
            end
            next if randomUp.length==0
            r = pbRandom(randomUp.length)
            stat = randomUp[r]
            b.pbRaiseStatStageBasic(stat, 1, true)
            pbCommonAnimation("StatUp", b)
            pbDisplayBrief(_INTL("<c3=FFEE88,FF6600>{1}凭借它的力量\n提高了{2}！</c3>",
                                  b.pbThis, PBStats.getName(stat)))
          elsif ret < 3000
            b.eachOpposing do |t|
              PBStats.eachBattleStat { |s| t.stages[s] = 0 if t.stages[s] > 0 }
              t.effects[PBEffects::FocusEnergy] = 0
            end
            pbDisplay(_INTL("<c3=FFEE88,FF6600>{1}凭借它的力量\n清除了对手的能力提升！</c3>",b.pbThis))
          elsif b.hasAnyNegativeEffects
            b.removeAllNegativeEffects(true)
            pbDisplayBrief(_INTL("<c3=FFEE88,FF6600>{1}凭借它的力量\n移除了受到的不好效果！</c3>",b.pbThis))
          end
          pbWait(80)
        end
      end
    end
  end
end

class PokeBattle_Battler
  
  def hasAnyNegativeEffects
    return !(@status == 0                         &&  # 异常状态
        @effects[PBEffects::Confusion]   == 0     &&  # 混乱
        @effects[PBEffects::Attract]     == -1    &&  # 着迷
        @effects[PBEffects::Nightmare]   == false &&  # 恶梦
        @effects[PBEffects::SaltCure]    == false &&  # 盐腌
        @effects[PBEffects::Yawn]        == 0     &&  # 瞌睡
        @effects[PBEffects::Encore]      == 0     &&  # 再来一次
        @effects[PBEffects::EncoreMove]  == 0     &&  # 再来一次
        @effects[PBEffects::GastroAcid]  == false &&  # 无特性
        @effects[PBEffects::Torment]     == false &&  # 无理取闹
        @effects[PBEffects::HealBlock]   == 0     &&  # 回复封锁
        @effects[PBEffects::Foresight]   == false &&  # 被识破
        @effects[PBEffects::Disable]     == 0     &&  # 定身法
        @effects[PBEffects::LockOn]      == 0     &&  # 锁定
        @effects[PBEffects::LockOnPos]   == -1    &&  # 锁定
        @effects[PBEffects::Taunt]       == 0     &&  # 挑衅
        @effects[PBEffects::Telekinesis] == 0     &&  # 意念移物
        @effects[PBEffects::Curse]       == false &&  # 诅咒
        @effects[PBEffects::PerishSong]  == 0     &&  # 灭亡之歌
        @effects[PBEffects::LeechSeed]   == -1    &&  # 寄生种子
        @effects[PBEffects::Trapping]    == 0     &&  # 束缚
        @effects[PBEffects::SmackDown]   == false &&  # 击落
        @effects[PBEffects::ThroatChop]  == 0     &&  # 地狱突刺
        @effects[PBEffects::TarShot]     == false &&  # 沥青射击
        @effects[PBEffects::Octolock]    == false &&  # 蛸固
        @effects[PBEffects::OctolockUser]== -1)       # 蛸固
  end
  def removeAllNegativeEffects(contains_stages=false)
    pbCureStatus(false)                        # 异常状态
    @effects[PBEffects::Confusion]   = 0       # 混乱
    @effects[PBEffects::Attract]     = -1      # 着迷
    @effects[PBEffects::Nightmare]   = false   # 恶梦
    @effects[PBEffects::SaltCure]    = false   # 盐腌
    @effects[PBEffects::Yawn]        = 0       # 瞌睡
    @effects[PBEffects::Encore]      = 0       # 再来一次
    @effects[PBEffects::EncoreMove]  = 0       # 再来一次
    @effects[PBEffects::GastroAcid]  = false   # 无特性
    @effects[PBEffects::Torment]     = false   # 无理取闹
    @effects[PBEffects::HealBlock]   = 0       # 回复封锁
    @effects[PBEffects::Foresight]   = false   # 被识破
    @effects[PBEffects::Disable]     = 0       # 定身法
    @effects[PBEffects::LockOn]      = 0       # 锁定
    @effects[PBEffects::LockOnPos]   = -1      # 锁定
    @effects[PBEffects::Taunt]       = 0       # 挑衅
    @effects[PBEffects::Telekinesis] = 0       # 意念移物
    @effects[PBEffects::Curse]       = false   # 诅咒
    @effects[PBEffects::PerishSong]  = 0       # 灭亡之歌
    @effects[PBEffects::LeechSeed]   = -1      # 寄生种子
    @effects[PBEffects::Trapping]    = 0       # 束缚
    @effects[PBEffects::SmackDown]   = false   # 击落
    @effects[PBEffects::ThroatChop]  = 0       # 地狱突刺
    @effects[PBEffects::TarShot]     = false   # 沥青射击
    @effects[PBEffects::Octolock]    = false   # 蛸固
    @effects[PBEffects::OctolockUser]= -1      # 蛸固
    if contains_stages
      PBStats.eachBattleStat { |s| @stages[s] = 0 if @stages[s] < 0 }
    end
  end
end

class PokeBattle_Battler
  def pbCatchBossPokemon(target)
    if $game_switches[196]
      @battle.pbDisplayPaused(_INTL("{1}逃走了...",target.pbThis))
      pbSEPlay("Battle flee")
      @battle.decision=1
      return
    end
    pkmn = target.pokemon
    if @battle.pbDisplayConfirm(_INTL("{1}现在很虚弱！\n要扔出球捕捉吗？", target.pbThis))
      resetWindowSize if defined?(PCV) # Compatibility for Modular Battle Scene
      scene  = PokemonBag_Scene.new
      screen = PokemonBagScreen.new(scene,$PokemonBag)
      ball   = screen.pbChooseItemScreen(Proc.new{|item| pbIsPokeBall?(item) })
      if ball>0
        if pbIsPokeBall?(ball)
          $PokemonBag.pbDeleteItem(ball,1)
          target.pokemon.resetMoves
          target.hp = 1
          @battle.pbThrowPokeBall(target.index,ball,255,false)
        end
      else # Choose not to capture
        @battle.pbDisplayPaused(_INTL("{1}逃走了...",target.pbThis))
        pbSEPlay("Battle flee")
        @battle.decision=1
      end
    else
      @battle.pbDisplayPaused(_INTL("{1}逃走了...",target.pbThis))
      pbSEPlay("Battle flee")
      @battle.decision=1
    end
  end
end