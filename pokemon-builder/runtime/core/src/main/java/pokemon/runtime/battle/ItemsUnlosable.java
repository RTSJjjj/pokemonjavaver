package pokemon.runtime.battle;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code pbIsUnlosableItem?(item,species,ability)} (188_PItem_Items.rb:172-346): items bound to a species that cannot be taken away
 * (Knock Off / Thief / Trick ...). The Ruby hash is transcribed in order (a later duplicate key replaces the earlier one, as in Ruby).
 * Project addition (the plugin author left it out): REGIGIGAS holds REGISPELL, which changes its form, so it must not be removable.
 */
final class ItemsUnlosable {
    private static final Map<String, String[]> COMBOS = new HashMap<>();
    static {
        COMBOS.put("VENUSAUR", new String[] {"VENUSAURITE", "VENUSAURITEG"});
        COMBOS.put("CHARIZARD", new String[] {"CHARIZARDITEX", "CHARIZARDITEY", "CHARIZARDITEG"});
        COMBOS.put("BLASTOISE", new String[] {"BLASTOISINITE", "BLASTOISINITEG"});
        COMBOS.put("BEEDRILL", new String[] {"BEEDRILLITE"});
        COMBOS.put("PIDGEOT", new String[] {"PIDGEOTITE"});
        COMBOS.put("ALAKAZAM", new String[] {"ALAKAZITE"});
        COMBOS.put("SLOWBRO", new String[] {"SLOWBRONITE"});
        COMBOS.put("GENGAR", new String[] {"GENGARITE", "GENGARITEG"});
        COMBOS.put("KANGASKHAN", new String[] {"KANGASKHANITE"});
        COMBOS.put("PINSIR", new String[] {"PINSIRITE"});
        COMBOS.put("GYARADOS", new String[] {"GYARADOSITE"});
        COMBOS.put("AERODACTYL", new String[] {"AERODACTYLITE"});
        COMBOS.put("MEWTWO", new String[] {"MEWTWONITEX", "MEWTWONITEY"});
        COMBOS.put("AMPHAROS", new String[] {"AMPHAROSITE"});
        COMBOS.put("STEELIX", new String[] {"STEELIXITE"});
        COMBOS.put("SCIZOR", new String[] {"SCIZORITE"});
        COMBOS.put("HERACROSS", new String[] {"HERACRONITE"});
        COMBOS.put("HOUNDOOM", new String[] {"HOUNDOOMINITE"});
        COMBOS.put("TYRANITAR", new String[] {"TYRANITARITE"});
        COMBOS.put("SCEPTILE", new String[] {"SCEPTILITE"});
        COMBOS.put("BLAZIKEN", new String[] {"BLAZIKENITE", "BLAZIKENITER"});
        COMBOS.put("SWAMPERT", new String[] {"SWAMPERTITE"});
        COMBOS.put("GARDEVOIR", new String[] {"GARDEVOIRITE"});
        COMBOS.put("SABLEYE", new String[] {"SABLENITE"});
        COMBOS.put("MAWILE", new String[] {"MAWILITE"});
        COMBOS.put("AGGRON", new String[] {"AGGRONITE"});
        COMBOS.put("MEDICHAM", new String[] {"MEDICHAMITE"});
        COMBOS.put("MANECTRIC", new String[] {"MANECTITE"});
        COMBOS.put("SHARPEDO", new String[] {"SHARPEDONITE"});
        COMBOS.put("CAMERUPT", new String[] {"CAMERUPTITE"});
        COMBOS.put("ALTARIA", new String[] {"ALTARIANITE"});
        COMBOS.put("BANETTE", new String[] {"BANETTITE"});
        COMBOS.put("ABSOL", new String[] {"ABSOLITE", "ABSOLITEZ"});
        COMBOS.put("GLALIE", new String[] {"GLALITITE"});
        COMBOS.put("SALAMENCE", new String[] {"SALAMENCITE"});
        COMBOS.put("METAGROSS", new String[] {"METAGROSSITE"});
        COMBOS.put("LATIAS", new String[] {"LATIASITE"});
        COMBOS.put("LATIOS", new String[] {"LATIOSITE"});
        COMBOS.put("LOPUNNY", new String[] {"LOPUNNITE"});
        COMBOS.put("GARCHOMP", new String[] {"GARCHOMPITE", "GARCHOMPITEZ"});
        COMBOS.put("LUCARIO", new String[] {"LUCARIONITE", "LUCARIONITEZ"});
        COMBOS.put("ABOMASNOW", new String[] {"ABOMASITE"});
        COMBOS.put("GALLADE", new String[] {"GALLADITE"});
        COMBOS.put("AUDINO", new String[] {"AUDINITE"});
        COMBOS.put("DIANCIE", new String[] {"DIANCITE"});
        COMBOS.put("BUTTERFREE", new String[] {"BUTTERFREEITE", "BUTTERFREEITEG"});
        COMBOS.put("GOODRA", new String[] {"GOODRAITE"});
        COMBOS.put("MILOTIC", new String[] {"MILOTICITE"});
        COMBOS.put("GOTHITELLE", new String[] {"GOTHITELLEITE"});
        COMBOS.put("TORTERRA", new String[] {"TORTERRAITE"});
        COMBOS.put("INFERNAPE", new String[] {"INFERNAPEITE"});
        COMBOS.put("EMPOLEON", new String[] {"EMPOLEONITE"});
        COMBOS.put("LILLIGANT", new String[] {"LILLIGANTITE"});
        COMBOS.put("PORYGONZ", new String[] {"PORYGONPATCH"});
        COMBOS.put("ESCAVALIER", new String[] {"ESCAVALIERITE"});
        COMBOS.put("ACCELGOR", new String[] {"ACCELGORITE"});
        COMBOS.put("RAPIDASH", new String[] {"RAPIDASHITE"});
        COMBOS.put("TOXICROAK", new String[] {"TOXICROAKITE"});
        COMBOS.put("KINGDRA", new String[] {"KINGDRAITE"});
        COMBOS.put("CHANDELURE", new String[] {"CHANDELUREITER", "CHANDELURITE"});
        COMBOS.put("WEAVILE", new String[] {"WEAVILEITE"});
        COMBOS.put("MEGANIUM", new String[] {"MEGANIUMITER", "MEGANIUMITE"});
        COMBOS.put("PRIMARINA", new String[] {"PRIMARINAITER"});
        COMBOS.put("FLYGON", new String[] {"FLYGONITE"});
        COMBOS.put("TYPHLOSION", new String[] {"TYPHLOSIONITE", "HISUITYPHLOSIONITE"});
        COMBOS.put("DARKMEWTWO", new String[] {"DARKMEWTWONITE"});
        COMBOS.put("SPIRITHITE", new String[] {"SPIRITHITITE"});
        COMBOS.put("CHANTEFLEUR", new String[] {"CHANTEFLEURITE"});
        COMBOS.put("DRAGONITE", new String[] {"DRAGONITEITE"});
        COMBOS.put("VICTREEBEL", new String[] {"VICTREEBELITE"});
        COMBOS.put("MALAMAR", new String[] {"MALAMARITE"});
        COMBOS.put("HAWLUCHA", new String[] {"HAWLUCHAITE"});
        COMBOS.put("STARMIE", new String[] {"STARMIEITE"});
        COMBOS.put("CLEFABLE", new String[] {"CLEFABLEITE"});
        COMBOS.put("FERALIGATR", new String[] {"FERALIGATRITE"});
        COMBOS.put("SKARMORY", new String[] {"SKARMORYITE"});
        COMBOS.put("FROSLASS", new String[] {"FROSLASSITE"});
        COMBOS.put("EMBOAR", new String[] {"EMBOARITE"});
        COMBOS.put("EXCADRILL", new String[] {"EXCADRILLITE"});
        COMBOS.put("SCOLIPEDE", new String[] {"SCOLIPEDEITE"});
        COMBOS.put("SCRAFTY", new String[] {"SCRAFTYITE"});
        COMBOS.put("EELEKTROSS", new String[] {"EELEKTROSSITE"});
        COMBOS.put("CHESNAUGHT", new String[] {"CHESNAUGHTITE"});
        COMBOS.put("DELPHOX", new String[] {"DELPHOXITE"});
        COMBOS.put("GRENINJA", new String[] {"GRENINJITE"});
        COMBOS.put("PYROAR", new String[] {"PYROARITE"});
        COMBOS.put("FLORGES", new String[] {"FLORGESITE"});
        COMBOS.put("BARBARACLE", new String[] {"BARBARACLEITE"});
        COMBOS.put("DRAGALGE", new String[] {"DRAGALGEITE"});
        COMBOS.put("DRAMPA", new String[] {"DRAMPAITE"});
        COMBOS.put("FALINKS", new String[] {"FALINKSITE"});
        COMBOS.put("RAICHU", new String[] {"RAICHUNITEX", "RAICHUNITEY"});
        COMBOS.put("CHIMECHO", new String[] {"CHIMECHONITE"});
        COMBOS.put("STARAPTOR", new String[] {"STARAPTORITE"});
        COMBOS.put("HEATRAN", new String[] {"HEATRANITE"});
        COMBOS.put("DARKRAI", new String[] {"DARKRAINITE"});
        COMBOS.put("MAGEARNA", new String[] {"MAGEARNAITE"});
        COMBOS.put("ZERAORA", new String[] {"ZERAORANITE"});
        COMBOS.put("SCOVILLAIN", new String[] {"SCOVILLAINITE"});
        COMBOS.put("GLIMMORA", new String[] {"GLIMMORANITE"});
        COMBOS.put("MEOWSTIC", new String[] {"MEOWSTICITE"});
        COMBOS.put("GOLURK", new String[] {"GOLURKNITE"});
        COMBOS.put("CRABOMINABLE", new String[] {"CRABOMINABLENITE"});
        COMBOS.put("GOLISOPOD", new String[] {"GOLISOPODITE"});
        COMBOS.put("BAXCALIBUR", new String[] {"BAXCALIBURNITE"});
        COMBOS.put("ZYGARDE", new String[] {"ZYGARDENITE"});
        COMBOS.put("TATSUGIRI", new String[] {"TATSUGIRINITE"});
        COMBOS.put("MACHAMP", new String[] {"MACHAMPITEG"});
        COMBOS.put("LAPRAS", new String[] {"LAPRASITEG"});
        COMBOS.put("KINGLER", new String[] {"KINGLERITEG"});
        COMBOS.put("GARBODOR", new String[] {"GARBODORITEG"});
        COMBOS.put("HATTERENE", new String[] {"HATTERENITEG"});
        COMBOS.put("GRIMMSNARL", new String[] {"GRIMMSNARLITEG"});
        COMBOS.put("INTELEON", new String[] {"INTELEONITEG"});
        COMBOS.put("RILLABOOM", new String[] {"RILLABOOMITEG"});
        COMBOS.put("CINDERACE", new String[] {"CINDERACITEG"});
        COMBOS.put("TOXTRICITY", new String[] {"TOXTRICITYITEG"});
        COMBOS.put("CENTISKORCH", new String[] {"CENTISKORCHITEG"});
        COMBOS.put("COALOSSAL", new String[] {"COALOSSALITEG"});
        COMBOS.put("CORVIKNIGHT", new String[] {"CORVIKNIGHTITEG"});
        COMBOS.put("ORBEETLE", new String[] {"ORBEETLEITEG"});
        COMBOS.put("DREDNAW", new String[] {"DREDNAWITEG"});
        COMBOS.put("COPPERAJAH", new String[] {"COPPERAJAHITEG"});
        COMBOS.put("ARCEUS", new String[] {"FISTPLATE", "SKYPLATE", "TOXICPLATE", "EARTHPLATE", "STONEPLATE", "INSECTPLATE", "SPOOKYPLATE", "IRONPLATE", "FLAMEPLATE", "SPLASHPLATE", "MEADOWPLATE", "ZAPPLATE", "MINDPLATE", "ICICLEPLATE", "DRACOPLATE", "DREADPLATE", "PIXIEPLATE", "LIGHTPLATE", "SHADOWPLATE", "BLANKPLATE", "LEGENDPLATE"});
        COMBOS.put("GIRATINA", new String[] {"GRISEOUSORB"});
        COMBOS.put("GENESECT", new String[] {"BURNDRIVE", "CHILLDRIVE", "DOUSEDRIVE", "SHOCKDRIVE"});
        COMBOS.put("DIALGA", new String[] {"ADAMANTORB"});
        COMBOS.put("PALKIA", new String[] {"LUSTROUSORB"});
        COMBOS.put("ZACIAN", new String[] {"RUSTEDSWORD"});
        COMBOS.put("ZAMAZENTA", new String[] {"RUSTEDSHIELD"});
        COMBOS.put("KYOGRE", new String[] {"BLUEORB"});
        COMBOS.put("GROUDON", new String[] {"REDORB"});
        COMBOS.put("VALKYRIE", new String[] {"BRIGHTSTONE"});
        COMBOS.put("BLACKKNIGHT", new String[] {"DAWNISTONE"});
        COMBOS.put("SUGARDEVOIR", new String[] {"TEMPLESCEPTER", "WMDCX", "SQHL", "RADIANTSHARD", "SANCTFEATHER"});
        COMBOS.put("SUJINRAKU", new String[] {"ABYSSSWORD"});
        COMBOS.put("SIRFETCHD", new String[] {"HOLYCREST"});
        COMBOS.put("CHALLEN", new String[] {"CHARLENEITE", "ZEROSUMHEART"});
        COMBOS.put("OGERPON", new String[] {"HEARTHFLAMEMASK", "WELLSPRINGMASK", "CORNERSTONEMASK"});
        COMBOS.put("REGIGIGAS", new String[] {"REGISPELL"});
    }

    private ItemsUnlosable() {}

    /** @param species the battler's species id; @param ability its ability id */
    static boolean isUnlosable(String item, String species, String ability) {
        if ("ARCEUS".equals(species) && !"MULTITYPE".equals(ability)) return false;       // :173
        if ("SILVALLY".equals(species) && !"RKSSYSTEM".equals(ability)) return false;     // :175
        String[] items = COMBOS.get(species);                                              // :340-345
        if (items == null) return false;
        for (String i : items) if (i.equals(item)) return true;
        return false;
    }
}
