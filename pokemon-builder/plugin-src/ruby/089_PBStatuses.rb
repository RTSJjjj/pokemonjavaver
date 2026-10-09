#70925035
begin
  module PBStatuses
    NONE      = 0
    SLEEP     = 1
    POISON    = 2
    BURN      = 3
    PARALYSIS = 4
    FROZEN    = 5
    FROSTBITE = 6 # Pokémon Legends: Arceus
    DROWSY    = 7 # Pokémon Legends: Arceus

    def self.getName(id)
      id = getID(PBStatuses,id)
      names = [
        _INTL("healthy"),
        _INTL("asleep"),
        _INTL("poisoned"),
        _INTL("burned"),
        _INTL("paralyzed"),
        _INTL("frozen"),
        _INTL("frostbitten"), # Pokémon Legends: Arceus
        _INTL("drowsy") # Pokémon Legends: Arceus
      ]
      return names[id]
    end
  end

rescue Exception
  if $!.is_a?(SystemExit) || "#{$!.class}"=="Reset"
    raise $!
  end
end
