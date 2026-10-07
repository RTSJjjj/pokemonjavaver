package pokemon.runtime.event;

/**
 * The boss reward tables of the project's {@code Boss_reward} script section
 * (Data/Scripts.rxdata): {@code boss_reward} rank tables (:6-60), the four
 * Pokemon pools (:143-190) and the Chinese name map (:193-296). Generated
 * from the section source so the numbers stay the plugin's.
 */
final class BossRewardsData {
    private BossRewardsData() {
    }

    /** One reward: {item internal name, quantity}. */
    static final class Reward {
        final String item;
        final int quantity;

        Reward(String item, int quantity) {
            this.item = item;
            this.quantity = quantity;
        }
    }

    /** One pool entry: {species internal name, form}. */
    static final class PoolEntry {
        final String species;
        final int form;

        PoolEntry(String species, int form) {
            this.species = species;
            this.form = form;
        }
    }

    /** Rank -> five reward sets (rand(1..5) picks one). */
    static Reward[][] rankRewards(int rank) {
        switch (rank) {
            case 2:
                return new Reward[][] {
                    { new Reward("EXPCANDYS", 10), new Reward("SUPERPOTION", 2), new Reward("BOTTLECAP", 1) },
                    { new Reward("EXPCANDYS", 15), new Reward("SUPERPOTION", 2), new Reward("BOTTLECAP", 1) },
                    { new Reward("EXPCANDYM", 5), new Reward("FULLRESTORE", 1), new Reward("RARECANDY", 1), new Reward("BOTTLECAP", 1) },
                    { new Reward("EXPCANDYM", 10), new Reward("FULLRESTORE", 1), new Reward("RARECANDY", 2), new Reward("BOTTLECAP", 1) },
                    { new Reward("EXPCANDYL", 5), new Reward("RARECANDY", 3), new Reward("BOTTLECAP", 2), new Reward("MAXREVIVE", 1) },
                };
            case 3:
                return new Reward[][] {
                    { new Reward("EXPCANDYL", 8), new Reward("SUPERPOTION", 3), new Reward("RARECANDY", 1), new Reward("BOTTLECAP", 1) },
                    { new Reward("EXPCANDYL", 10), new Reward("RARECANDY", 1), new Reward("BOTTLECAP", 1), new Reward("SUPERPOTION", 3) },
                    { new Reward("EXPCANDYL", 10), new Reward("RARECANDY", 1), new Reward("BOTTLECAP", 1), new Reward("FULLRESTORE", 1) },
                    { new Reward("EXPCANDYM", 15), new Reward("FULLRESTORE", 1), new Reward("RARECANDY", 4), new Reward("BOTTLECAP", 2) },
                    { new Reward("EXPCANDYXL", 15), new Reward("RARECANDY", 5), new Reward("BOTTLECAP", 2), new Reward("MAXREVIVE", 2) },
                };
            case 4:
                return new Reward[][] {
                    { new Reward("EXPCANDYXL", 10), new Reward("SUPERPOTION", 2), new Reward("RARECANDY", 2), new Reward("BOTTLECAP", 2) },
                    { new Reward("EXPCANDYM", 10), new Reward("RARECANDY", 2), new Reward("BOTTLECAP", 2), new Reward("SUPERPOTION", 2) },
                    { new Reward("EXPCANDYM", 10), new Reward("RARECANDY", 2), new Reward("BOTTLECAP", 2), new Reward("FULLRESTORE", 2) },
                    { new Reward("EXPCANDYM", 20), new Reward("FULLRESTORE", 2), new Reward("RARECANDY", 7), new Reward("BOTTLECAP", 3) },
                    { new Reward("EXPCANDYXL", 30), new Reward("RARECANDY", 8), new Reward("GOLDBOTTLECAP", 1), new Reward("MAXREVIVE", 3) },
                };
            case 5:
                return new Reward[][] {
                    { new Reward("EXPCANDYXL", 15), new Reward("FULLRESTORE", 2), new Reward("RARECANDY", 3), new Reward("BOTTLECAP", 3) },
                    { new Reward("EXPCANDYXL", 15), new Reward("RARECANDY", 3), new Reward("BOTTLECAP", 3), new Reward("FULLRESTORE", 2) },
                    { new Reward("EXPCANDYXL", 20), new Reward("RARECANDY", 3), new Reward("BOTTLECAP", 3), new Reward("FULLRESTORE", 2) },
                    { new Reward("EXPCANDYXL", 25), new Reward("FULLRESTORE", 3), new Reward("RARECANDY", 8), new Reward("BOTTLECAP", 4) },
                    { new Reward("EXPCANDYXL", 50), new Reward("RARECANDY", 10), new Reward("GOLDBOTTLECAP", 2), new Reward("MAXREVIVE", 4) },
                };
            case 6:
                return new Reward[][] {
                    { new Reward("EXPCANDYXL", 25), new Reward("FULLRESTORE", 3), new Reward("RARECANDY", 5), new Reward("BOTTLECAP", 5) },
                    { new Reward("EXPCANDYXL", 30), new Reward("RARECANDY", 5), new Reward("BOTTLECAP", 5), new Reward("FULLRESTORE", 3) },
                    { new Reward("EXPCANDYXL", 30), new Reward("RARECANDY", 5), new Reward("BOTTLECAP", 5), new Reward("FULLRESTORE", 3) },
                    { new Reward("EXPCANDYXL", 40), new Reward("FULLRESTORE", 4), new Reward("RARECANDY", 12), new Reward("GOLDBOTTLECAP", 1), new Reward("BOTTLECAP", 4) },
                    { new Reward("EXPCANDYXL", 80), new Reward("RARECANDY", 15), new Reward("GOLDBOTTLECAP", 3), new Reward("MAXREVIVE", 6) },
                };
            case 7:
                return new Reward[][] {
                    { new Reward("EXPCANDYXL", 40), new Reward("FULLRESTORE", 4), new Reward("RARECANDY", 8), new Reward("BOTTLECAP", 8) },
                    { new Reward("EXPCANDYXL", 50), new Reward("RARECANDY", 8), new Reward("BOTTLECAP", 8), new Reward("FULLRESTORE", 4) },
                    { new Reward("EXPCANDYXL", 50), new Reward("RARECANDY", 8), new Reward("BOTTLECAP", 8), new Reward("FULLRESTORE", 4) },
                    { new Reward("EXPCANDYXL", 60), new Reward("FULLRESTORE", 5), new Reward("RARECANDY", 15), new Reward("GOLDBOTTLECAP", 2), new Reward("BOTTLECAP", 6) },
                    { new Reward("EXPCANDYXL", 100), new Reward("RARECANDY", 20), new Reward("GOLDBOTTLECAP", 5), new Reward("MAXREVIVE", 8) },
                };
            default:
                return rankRewards(5);
        }
    }

    /** The rank display names (Boss_reward:67-70). */
    static String rankName(int rank) {
        switch (rank) {
            case 2: return "精英";
            case 3: return "初级BOSS";
            case 4: return "中级BOSS";
            case 5: return "高级BOSS";
            case 6: return "超级BOSS";
            case 7: return "终极BOSS";
            default: return "BOSS";
        }
    }

    static final PoolEntry[] COMMON_POOL = {
        new PoolEntry("QINGNIAO", 0), new PoolEntry("QINGNIAO", 1), new PoolEntry("TREBARK", 0), new PoolEntry("TREBARK", 1), new PoolEntry("TREBARK", 2), new PoolEntry("CACTUS", 0),
        new PoolEntry("CACTUS", 1), new PoolEntry("CACTUS", 2), new PoolEntry("LEAFFROG", 0), new PoolEntry("LETBURSTDR", 0), new PoolEntry("SAILORPENGU", 0), new PoolEntry("GRASSFRUIT", 0),
        new PoolEntry("ARMOREDSOLDIER", 0), new PoolEntry("STORMEOW", 0), new PoolEntry("VOLTCAT", 0), new PoolEntry("BLUNIB", 0), new PoolEntry("XIANRENQIU", 0), new PoolEntry("BEAGLE", 0),
        new PoolEntry("COPPERNAKE", 0), new PoolEntry("FERROHEAD", 0), new PoolEntry("TIGERBEAST", 0), new PoolEntry("HAHATREE", 0),
    };

    static final PoolEntry[] RARE_POOL = {
        new PoolEntry("GOLDENSEED", 0), new PoolEntry("GOLDENSEED", 1), new PoolEntry("MANDRAKE", 0), new PoolEntry("MANDRAKE", 1), new PoolEntry("THUNDERMOUSE", 0), new PoolEntry("THUNDERMOUSE", 1),
        new PoolEntry("LEAFSHROOM", 0), new PoolEntry("LEAFSHROOM", 1), new PoolEntry("WHIZ", 0), new PoolEntry("WHIZ", 1), new PoolEntry("TREEFROG", 0), new PoolEntry("FIREEATER", 0),
        new PoolEntry("COUNTPENGU", 0), new PoolEntry("DATURA", 0), new PoolEntry("ARMOREDCAPTAIN", 0), new PoolEntry("VOLTEYE", 0), new PoolEntry("CANTARUN", 0), new PoolEntry("XIANRENZHANG", 0),
        new PoolEntry("FERROGIANT", 0), new PoolEntry("TIGERCHIEFTAIN", 0), new PoolEntry("CRYINGWOOD", 0), new PoolEntry("YAK", 0), new PoolEntry("SQUASHBRO", 0), new PoolEntry("SEAGULL", 0),
    };

    static final PoolEntry[] EPIC_POOL = {
        new PoolEntry("VINELADY", 0), new PoolEntry("VINELADY", 1), new PoolEntry("VINELADY", 2), new PoolEntry("WOLFSPIDER", 0), new PoolEntry("WOLFSPIDER", 1), new PoolEntry("FLOATDRAGON", 0),
        new PoolEntry("FLOATDRAGON", 1), new PoolEntry("WINGEDCICADA", 0), new PoolEntry("WINGEDCICADA", 1), new PoolEntry("NIGHTFAIRY", 0), new PoolEntry("NIGHTFAIRY", 1), new PoolEntry("NIGHTFAIRY", 2),
        new PoolEntry("DATURAFLOS", 0), new PoolEntry("OVERLORDFLOS", 0), new PoolEntry("ARISTOCAT", 0), new PoolEntry("LIGHTNINGCAT", 0), new PoolEntry("THOUSANDTREE", 0), new PoolEntry("FIERCEAGLE", 0),
        new PoolEntry("CHOCOBO", 0), new PoolEntry("KUNGFUTIGER", 0), new PoolEntry("TIGERWARRIOR", 0), new PoolEntry("OCTOPUSBABY", 0), new PoolEntry("POCKETGRASS", 0),
    };

    static final PoolEntry[] LEGEND_POOL = {
        new PoolEntry("SHENYUNQUAN", 0), new PoolEntry("SHENYUNQUAN", 1), new PoolEntry("ARMOREDGENERAL", 0), new PoolEntry("ARMOREDGENERAL", 1), new PoolEntry("ARMOREDGENERAL", 2), new PoolEntry("JINGDOUQUAN", 0),
        new PoolEntry("JINGDOUQUAN", 1), new PoolEntry("REDGOLDKING", 0), new PoolEntry("REDGOLDKING", 1), new PoolEntry("PLATINUM", 0), new PoolEntry("TIEBIBAWANGSHU", 0), new PoolEntry("ORCALITH", 0),
        new PoolEntry("DREADENDRON", 0), new PoolEntry("GOLDENSLUG", 0), new PoolEntry("WARRIORPENGU", 0), new PoolEntry("BLASTER", 0), new PoolEntry("DRAGONFROG", 0), new PoolEntry("DIANCIREN", 0),
        new PoolEntry("STRANGEBEAST", 0), new PoolEntry("TRUESTRANGEBEAST", 0), new PoolEntry("NARVALIS", 0), new PoolEntry("GOLDEN", 0), new PoolEntry("IRONARMSNAKE", 0), new PoolEntry("YAKTANK", 0),
        new PoolEntry("FERRODRUN", 0), new PoolEntry("YISHEN", 0),
    };

    /** The Chinese display name of one (species, form) (Boss_reward:193-296). */
    static String speciesName(String species, int form) {
        switch (species + "|" + form) {
            case "QINGNIAO|0": return "青鸟";
            case "QINGNIAO|1": return "灾雀";
            case "TREBARK|0": return "憨憨树";
            case "TREBARK|1": return "荆棘树";
            case "TREBARK|2": return "液毒树";
            case "CACTUS|0": return "仙人掌";
            case "CACTUS|1": return "有刺仙人掌";
            case "CACTUS|2": return "毒刺仙人掌";
            case "LEAFFROG|0": return "叶伞蛙";
            case "LETBURSTDR|0": return "小爆龙";
            case "SAILORPENGU|0": return "水手企鹅";
            case "GRASSFRUIT|0": return "草果";
            case "ARMOREDSOLDIER|0": return "铁甲士兵";
            case "STORMEOW|0": return "雷云猫";
            case "VOLTCAT|0": return "伏特猫";
            case "BLUNIB|0": return "小蓝鲸";
            case "XIANRENQIU|0": return "仙人球宝宝";
            case "BEAGLE|0": return "豆鹰";
            case "COPPERNAKE|0": return "赤铜蛇";
            case "FERROHEAD|0": return "铁头";
            case "TIGERBEAST|0": return "虎兽";
            case "HAHATREE|0": return "哈哈树";
            case "GOLDENSEED|0": return "金色种子";
            case "GOLDENSEED|1": return "金色种子（雌性）";
            case "MANDRAKE|0": return "曼陀罗";
            case "MANDRAKE|1": return "曼陀罗（雌性）";
            case "THUNDERMOUSE|0": return "雷鸣电鼠";
            case "THUNDERMOUSE|1": return "雷鸣电鼠（负电）";
            case "LEAFSHROOM|0": return "草叶菇";
            case "LEAFSHROOM|1": return "草叶菇（钢伞）";
            case "WHIZ|0": return "飕鸣龙";
            case "WHIZ|1": return "呜飒龙";
            case "TREEFROG|0": return "树伞蛙";
            case "FIREEATER|0": return "噬火兽";
            case "COUNTPENGU|0": return "伯爵企鹅";
            case "DATURA|0": return "曼陀罗";
            case "ARMOREDCAPTAIN|0": return "铁甲队长";
            case "VOLTEYE|0": return "电眼猫";
            case "CANTARUN|0": return "巨头鲸";
            case "XIANRENZHANG|0": return "仙人掌兽";
            case "FERROGIANT|0": return "铁巨人";
            case "TIGERCHIEFTAIN|0": return "虎酋长";
            case "CRYINGWOOD|0": return "哭泣木灵";
            case "YAK|0": return "牦牛";
            case "SQUASHBRO|0": return "倭瓜弟弟";
            case "SEAGULL|0": return "海鸥";
            case "VINELADY|0": return "翠萝儿";
            case "VINELADY|1": return "四叶草";
            case "VINELADY|2": return "爱心草";
            case "WOLFSPIDER|0": return "狼蛛";
            case "WOLFSPIDER|1": return "狼蛛（毒）";
            case "FLOATDRAGON|0": return "浮游龙";
            case "FLOATDRAGON|1": return "鬼龙";
            case "WINGEDCICADA|0": return "飞翅蝉";
            case "WINGEDCICADA|1": return "飞翅蝉（脱壳后）";
            case "NIGHTFAIRY|0": return "夜精灵";
            case "NIGHTFAIRY|1": return "夜精灵（黑暗）";
            case "NIGHTFAIRY|2": return "夜精灵（女王）";
            case "DATURAFLOS|0": return "曼陀罗花";
            case "OVERLORDFLOS|0": return "龙爪兰";
            case "ARISTOCAT|0": return "绅士猫";
            case "LIGHTNINGCAT|0": return "闪电猫";
            case "THOUSANDTREE|0": return "千年树";
            case "FIERCEAGLE|0": return "烈鹰";
            case "CHOCOBO|0": return "陆行鸟";
            case "KUNGFUTIGER|0": return "功夫虎";
            case "TIGERWARRIOR|0": return "虎勇士";
            case "OCTOPUSBABY|0": return "章鱼宝宝";
            case "POCKETGRASS|0": return "口袋草";
            case "SHENYUNQUAN|0": return "神云犬";
            case "SHENYUNQUAN|1": return "邪云";
            case "ARMOREDGENERAL|0": return "铁甲将军";
            case "ARMOREDGENERAL|1": return "双刀将军";
            case "ARMOREDGENERAL|2": return "刀枪将军";
            case "JINGDOUQUAN|0": return "竞斗犬";
            case "JINGDOUQUAN|1": return "绝对零度";
            case "REDGOLDKING|0": return "赤金蛇";
            case "REDGOLDKING|1": return "赤金蛇后";
            case "PLATINUM|0": return "白金龙";
            case "TIEBIBAWANGSHU|0": return "铁壁霸王树";
            case "ORCALITH|0": return "要塞鲸";
            case "DREADENDRON|0": return "古树之王";
            case "GOLDENSLUG|0": return "金尖蜗牛";
            case "WARRIORPENGU|0": return "武神企鹅";
            case "BLASTER|0": return "爆龙兽";
            case "DRAGONFROG|0": return "成龙蛙";
            case "DIANCIREN|0": return "电磁人";
            case "STRANGEBEAST|0": return "奇异兽";
            case "TRUESTRANGEBEAST|0": return "真奇异兽";
            case "NARVALIS|0": return "独角鲸";
            case "GOLDEN|0": return "金翼龙";
            case "IRONARMSNAKE|0": return "铁甲蛇";
            case "YAKTANK|0": return "牦牛坦克";
            case "FERRODRUN|0": return "铁巨灵";
            case "YISHEN|0": return "翼神";
            default: return species;
        }
    }
}
