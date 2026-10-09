begin
  module PBStats
    # NOTE: You can change the order that the compiler expects Pokémon base
    #       stats/EV yields (effort points) to be in, by simply renumbering the
    #       stats here. The "main" stats (i.e. not accuracy/evasion) must still
    #       use up numbers 0 to 5 inclusive, though. It's up to you to write the
    #       base stats/EV yields in pokemon.txt and pokemonforms.txt in the
    #       order expected.
    HP       = 0
    ATTACK   = 1
    DEFENSE  = 2
    SPEED    = 3
    SPATK    = 4
    SPDEF    = 5
    ACCURACY = 6
    EVASION  = 7

    def PBStats.getName(id)
      names = [
         _INTL("血量"),
         _INTL("攻击"),
         _INTL("防御"),
         _INTL("速度"),
         _INTL("特攻"),
         _INTL("特防"),
         _INTL("命中"),
         _INTL("回避")
      ]
      return names[id]
    end

    def PBStats.getNameBrief(id)
      names = [   
      _INTL("血量"),
         _INTL("攻击"),
         _INTL("防御"),
         _INTL("速度"),
         _INTL("特攻"),
         _INTL("特防"),
         _INTL("命中"),
         _INTL("回避")
      ]
      return names[id]
    end
    
    def self.eachStat
      [HP,ATTACK,DEFENSE,SPATK,SPDEF,SPEED].each { |s| yield s }
    end

    def self.eachMainBattleStat
      [ATTACK,DEFENSE,SPATK,SPDEF,SPEED].each { |s| yield s }
    end

    def self.eachBattleStat
      [ATTACK,DEFENSE,SPATK,SPDEF,SPEED,ACCURACY,EVASION].each { |s| yield s }
    end

    def self.validBattleStat?(stat)
      self.eachBattleStat { |s| return true if s==stat }
      return false
    end
  end

rescue Exception
  if $!.is_a?(SystemExit) || "#{$!.class}"=="Reset"
    raise $!
  end
end
