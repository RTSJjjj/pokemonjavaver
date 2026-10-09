package pokemon.runtime.pokemon;

/**
 * 235_PMinigame_TripleTriad {@code TriadCard} (:8-80): a species as a card - four numbers from its base stats, its type, and
 * the price the card shop asks.
 */
public final class TriadCard {
    public final PbsData.Species species;
    public final int north;
    public final int east;
    public final int south;
    public final int west;
    /** The type id of the card (the type icon row), or -1. */
    public final int type;

    public TriadCard(PbsData pbs, PbsData.Species species) {
        this.species = species;
        int hp = species.baseStat(0);                                           // PBStats::HP
        int attack = species.baseStat(1);
        int defense = species.baseStat(2);
        int speed = species.baseStat(3);
        int spAtk = species.baseStat(4);
        int spDef = species.baseStat(5);
        String type1 = species.type(0);                                         // :23
        if ("NORMAL".equals(type1)) {                                           // :24
            String type2 = species.type(1);
            if (type2 != null) type1 = type2;                                   // :26 @type = type2 if type2
        }
        PbsData.TypeInfo info = type1 == null || pbs == null ? null : pbs.type(type1);
        this.type = info == null ? -1 : info.id;
        this.west = baseStatToValue(attack + speed / 3);                        // :28
        this.east = baseStatToValue(defense + hp / 3);                          // :29
        this.north = baseStatToValue(spAtk + speed / 3);                        // :30
        this.south = baseStatToValue(spDef + hp / 3);                           // :31
    }

    /** :34-45 */
    static int baseStatToValue(int stat) {
        if (stat >= 189) return 10;
        if (stat >= 160) return 9;
        if (stat >= 134) return 8;
        if (stat >= 115) return 7;
        if (stat >= 100) return 6;
        if (stat >= 86) return 5;
        if (stat >= 73) return 4;
        if (stat >= 60) return 3;
        if (stat >= 45) return 2;
        return 1;
    }

    /** :66-80 */
    public int price() {
        int maxValue = Math.max(Math.max(north, east), Math.max(south, west));
        int ret = north * north + east * east + south * south + west * west;
        ret += maxValue * maxValue * 2;
        ret *= maxValue;
        ret *= (north + east + south + west);
        ret /= 10;                                                              // Ranges from 2 to 24,000
        // Quantize prices to the next highest "unit"
        if (ret > 10000) ret = (1 + ret / 1000) * 1000;
        else if (ret > 5000) ret = (1 + ret / 500) * 500;
        else if (ret > 1000) ret = (1 + ret / 100) * 100;
        else if (ret > 500) ret = (1 + ret / 50) * 50;
        else ret = (1 + ret / 10) * 10;
        return ret;
    }
}
