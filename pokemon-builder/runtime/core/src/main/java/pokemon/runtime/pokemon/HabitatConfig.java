package pokemon.runtime.pokemon;

/**
 * 294_Boonzeet_s_Habitat_List {@code HabitatConfig}: the habitats of the game - each one the map ids that make up the area and the
 * encounter kinds the list shows for it - and the region map tiles whose habitat is a different map than the one the tile is
 * registered for. Generated from the plugin's own tables ({@value #SOURCE_LINES}); {@code Habitats.update} relies on this
 * order, so entries are only ever appended.
 */
public final class HabitatConfig {

    static final String SOURCE_LINES = "294:77-245, :253-273";

    private HabitatConfig() {
    }

    /** {@code HabitatConfig::TypeOrder} (:70): the order the kinds are shown in. */
    public static final String[] TYPE_ORDER = {"grass", "place", "surf", "fish"};

    /** {@code HabitatConfig::Types} (:42-69): the encounter types (PBS method names) behind each kind. */
    public static String[] encounterTypes(String kind) {
        switch (kind) {
            case "grass": return new String[] {"Land", "LandMorning", "LandDay", "LandNight"};
            case "surf": return new String[] {"Water"};
            case "fish": return new String[] {"OldRod", "GoodRod", "SuperRod"};
            case "place": return new String[] {"Cave"};
            default: return new String[0];
        }
    }
    /** {@code HabitatConfig::Habitats} (:79-245): the map ids of each habitat. */
    public static final int[][] HABITAT_MAPS = {
        {44},   // 樱祭公园
        {343},   // 茶月保护区
        {483},   // 迷迭之路
        {10},   // 一号道路
        {43},   // 桃苑森林
        {132},   // 桃苑森林（后期）
        {387},   // 格诺小径
        {18},   // 二号道路
        {36},   // 三号道路
        {37},   // 四号道路
        {88},   // 紫罗洞穴1F
        {179},   // 紫罗洞穴2F
        {180},   // 紫罗洞穴3F
        {284},   // 幻夜之林
        {38},   // 五号道路
        {42},   // 六号道路
        {45},   // 静隐林外
        {346},   // 静隐林
        {463},   // 静隐林
        {288},   // 静隐路
        {160},   // 隐林
        {265},   // 永恒花田
        {161},   // 曦寒山
        {162},   // 曦寒山
        {115},   // 绯隐牧场
        {46},   // 七号道路
        {144},   // 弥留之塔2F
        {145},   // 弥留之塔3F
        {146},   // 弥留之塔4F
        {147},   // 弥留之塔5F
        {47},   // 八号道路
        {20},   // 沃饶洞
        {21},   // 沃饶洞
        {22},   // 沃饶遗迹
        {48},   // 九号道路
        {49},   // 十号道路
        {508},   // 奇幻森林-萤语林
        {509},   // 沉音沼
        {510},   // 绯云林
        {50},   // 十一号道路
        {51},   // 十二号道路
        {494},   // 12号道路-深海
        {52},   // 十三号道路
        {54},   // 十四号道路
        {297},   // 深流洞穴
        {56},   // 十五号道路
        {61},   // 十六号道路
        {28},   // 17号道路
        {429},   // 18号道路
        {430},   // 19号道路
        {431},   // 20号道路
        {495},   // 20号道路-深海
        {249},   // 绯雷星泉
        {62},   // 花影小路
        {286},   // 试炼之丘
        {287},   // 试炼之路
        {125},   // 曦寒山
        {129},   // 曦寒山
        {130},   // 曦寒山
        {134},   // 曦寒山
        {155},   // 曦寒山
        {186},   // 曦寒山
        {156},   // 曦寒山腰
        {302},   // 曦寒山洞
        {432},   // 暮煦山
        {433},   // 暮煦山
        {434},   // 暮煦山腰
        {436},   // 暮煦山
        {437},   // 暮煦山
        {324},   // 暮煦山洞
        {228},   // 暮煦山地心
        {53},   // 骇浪海底
        {55},   // 骇浪岛
        {428},   // 海底祭坛
        {27},   // 原野区中心
        {30},   // 原野区东1区
        {521},   // 原野区东2区
        {29},   // 原野区西1区
        {522},   // 原野区西2区
        {31},   // 原野区北1区
        {327},   // 原野区北2区
        {334},   // 体力之岛
        {335},   // 攻击之岛
        {336},   // 防御之岛
        {337},   // 速度之岛
        {338},   // 特攻之岛
        {339},   // 特防之岛
        {90},   // 101号道路
        {107},   // 102号道路
        {169},   // 103号道路
        {190},   // 104号道路
        {195},   // 106号道路
        {200},   // 埃德尔洞穴
        {392},   // 埃德尔遗迹
        {395},   // 埃德尔遗迹
        {197},   // 埃德尔遗迹
        {196},   // 冰绒森林
        {208},   // 110道路
        {213},   // 112号道路
        {174},   // 西风海岛
        {173},   // 无名海岛
        {215},   // 南方水路
        {216},   // 黎明洞穴1F
        {217},   // 黎明洞穴2F
        {218},   // 黎明洞穴3F
        {219},   // 黎明洞穴4F
        {283},   // 113号道路
        {427},   // 清缘道路
        {470},   // 索拉洞穴
        {471},   // 索拉洞穴
        {472},   // 索拉洞穴
        {345},   // 黄沙之地
        {300},   // 114号道路
        {304},   // 115号道路
        {137},   // 墨痕洞穴·外层
        {320},   // 墨痕洞穴·中层
        {393},   // 墨痕洞穴·深层
        {307},   // 116号道路
        {308},   // 舞墨湖畔
        {309},   // 117号道路
        {312},   // 118号道路
        {314},   // 119号道路
        {315},   // 120号道路
        {316},   // 绊风之森
        {311},   // 莲心湖
        {384},   // 微冰洞穴
        {386},   // 微冰小径
        {306},   // 歌舞森林
        {407},   // 歌舞森林
        {406},   // 坠落遗迹
        {372},   // 千夜岛
        {380},   // 千夜洞穴1F
        {382},   // 千夜洞穴2F
        {383},   // 千夜洞穴3F
        {230},   // 和荀平原
        {226},   // 祥云之地
        {517},   // 雷鸣洞穴
        {520},   // 雷鸣洞穴-2F
        {227},   // 和荀小径
        {473},   // 和荀小径
        {474},   // 和荀小径
        {412},   // 春之岛
        {413},   // 夏之岛
        {414},   // 秋之岛
        {415},   // 冬之岛
        {457},   // 信心路
        {224},   // 红枫雪域
        {292},   // 红枫之林
        {60},   // 裂隙边廊
        {207},   // 时隐灵根
        {209},   // 破界外环
        {210},   // 破时幽廊
        {233},   // 心魂之地
        {370},   // 心之洞穴
        {373},   // 灵之洞穴
        {349},   // 清澈湖
        {507},   // 芳草岛
        {140},   // 尘封山
        {441},   // 尘封山崖
        {442},   // 尘封山路
        {444},   // 尘封坟
        {468},   // 零区研究所-中厅
        {479},   // 零区研究所-西区
        {487},   // 晦木古林
        {488},   // 沸泉谷
        {489},   // 烬骨火山
        {490},   // 断脊岩台
    };

    /** {@code HabitatConfig::Habitats}: the kinds (grass / place / surf / fish) of each habitat, same order as {@link #HABITAT_MAPS}. */
    public static final String[][] HABITAT_KINDS = {
        {"grass"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass", "fish"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"place"},
        {"place"},
        {"place"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass", "surf"},
        {"surf", "fish"},
        {"place"},
        {"place"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"place"},
        {"place"},
        {"place"},
        {"place"},
        {"grass"},
        {"place"},
        {"place", "surf", "fish"},
        {"place", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"surf"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"place", "surf"},
        {"surf", "fish"},
        {"grass"},
        {"surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass", "surf"},
        {"grass"},
        {"surf", "fish"},
        {"grass"},
        {"grass"},
        {"grass"},
        {"place"},
        {"place"},
        {"place"},
        {"place", "surf"},
        {"place"},
        {"place"},
        {"grass"},
        {"place"},
        {"place"},
        {"place"},
        {"grass"},
        {"place"},
        {"place"},
        {"place"},
        {"grass"},
        {"grass"},
        {"surf", "fish"},
        {"grass"},
        {"grass", "fish"},
        {"grass"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"grass"},
        {"grass"},
        {"grass"},
        {"grass"},
        {"grass"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"surf", "fish"},
        {"grass"},
        {"grass"},
        {"place"},
        {"place"},
        {"place"},
        {"place"},
        {"grass"},
        {"grass"},
        {"grass", "surf"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"surf", "fish"},
        {"place"},
        {"place", "surf", "fish"},
        {"place", "surf", "fish"},
        {"place", "surf", "fish"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"place"},
        {"place"},
        {"place"},
        {"place"},
        {"grass"},
        {"grass"},
        {"place"},
        {"place", "surf", "fish"},
        {"place"},
        {"grass", "surf"},
        {"grass", "surf", "fish"},
        {"grass", "surf"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"surf", "fish"},
        {"place"},
        {"place"},
        {"grass"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"place"},
        {"place", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"place", "surf", "fish"},
        {"place", "surf", "fish"},
        {"place"},
        {"place"},
        {"place"},
        {"grass", "surf"},
        {"grass"},
        {"grass", "surf"},
        {"grass"},
        {"grass"},
        {"grass"},
        {"grass"},
        {"place"},
        {"place"},
        {"place"},
        {"place"},
        {"place"},
        {"place"},
        {"place"},
        {"grass", "surf", "fish"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"grass"},
        {"grass"},
        {"grass"},
        {"place"},
        {"place"},
        {"grass"},
        {"grass", "surf", "fish"},
        {"grass"},
        {"place", "surf", "fish"},
    };

    /** {@code HabitatConfig::RegionOverride} (:253-273): [region, x, y] of a region map tile -> the maps of the habitat to show for it. */
    public static final int[][] REGION_OVERRIDE_KEYS = {
        {0, 3, 1},
        {0, 3, 16},
        {0, 6, 5},
        {0, 10, 0},
        {0, 12, 8},
        {0, 17, 6},
        {0, 28, 18},
        {0, 26, 7},
        {0, 27, 14},
        {0, 13, 4},
        {0, 26, 14},
        {1, 3, 8},
        {1, 14, 8},
        {1, 16, 17},
        {1, 18, 11},
        {1, 21, 1},
        {1, 25, 2},
        {1, 25, 13},
        {1, 26, 10},
    };

    public static final int[][] REGION_OVERRIDE_MAPS = {
        {140, 441, 442, 444},
        {27, 30, 521, 29, 522, 31, 327},
        {508, 509, 510},
        {76, 125, 129, 130, 134, 155, 156, 161, 162, 186, 187, 302},
        {20, 21, 22},
        {88, 179, 180, 223},
        {53, 428},
        {43, 132},
        {228, 320, 433, 434, 436, 437},
        {51, 494},
        {431, 495},
        {216, 217, 218, 219},
        {372, 380, 382, 383},
        {349, 507},
        {470, 471, 472},
        {196, 224, 292},
        {200, 195, 197, 392, 395},
        {227, 473, 474},
        {137, 320, 324, 393},
    };
}
