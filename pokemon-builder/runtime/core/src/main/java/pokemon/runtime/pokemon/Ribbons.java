package pokemon.runtime.pokemon;

/**
 * The project's {@code PBRibbons} table (name / description), used by the
 * summary's ribbon page. Ribbon ids are 1-based; index 0 is the empty string
 * entry, exactly like the plugin's arrays.
 */
public final class Ribbons {

    private Ribbons() {
    }

    /** The constants of {@code module PBRibbons} (095_PBRibbons:1-80) in id order; id = index + 1. */
    private static final String[] CONSTANTS = {"HOENNCOOL","HOENNCOOLSUPER","HOENNCOOLHYPER","HOENNCOOLMASTER","HOENNBEAUTY","HOENNBEAUTYSUPER","HOENNBEAUTYHYPER","HOENNBEAUTYMASTER","HOENNCUTE","HOENNCUTESUPER","HOENNCUTEHYPER","HOENNCUTEMASTER","HOENNSMART","HOENNSMARTSUPER","HOENNSMARTHYPER","HOENNSMARTMASTER","HOENNTOUGH","HOENNTOUGHSUPER","HOENNTOUGHHYPER","HOENNTOUGHMASTER","SINNOHCOOL","SINNOHCOOLSUPER","SINNOHCOOLHYPER","SINNOHCOOLMASTER","SINNOHBEAUTY","SINNOHBEAUTYSUPER","SINNOHBEAUTYHYPER","SINNOHBEAUTYMASTER","SINNOHCUTE","SINNOHCUTESUPER","SINNOHCUTEHYPER","SINNOHCUTEMASTER","SINNOHSMART","SINNOHSMARTSUPER","SINNOHSMARTHYPER","SINNOHSMARTMASTER","SINNOHTOUGH","SINNOHTOUGHSUPER","SINNOHTOUGHHYPER","SINNOHTOUGHMASTER","WINNING","VICTORY","ABILITY","GREATABILITY","DOUBLEABILITY","MULTIABILITY","PAIRABILITY","WORLDABILITY","CHAMPION","SINNOHCHAMP","RECORD","EVENT","LEGEND","GORGEOUS","ROYAL","GORGEOUSROYAL","ALERT","SHOCK","DOWNCAST","CARELESS","RELAX","SNOOZE","SMILE","FOOTPRINT","ARTIST","EFFORT","BIRTHDAY","SPECIAL","CLASSIC","PREMIER","SOUVENIR","WISHING","NATIONAL","COUNTRY","BATTLECHAMPION","REGIONALCHAMPION","EARTH","WORLD","NATIONALCHAMPION","WORLDCHAMPION"};

    /** {@code getID(PBRibbons, ribbon)}: an id, a number given as text, or a constant name ({@code CHAMPION}); 0 when unknown. */
    public static int idOf(String ribbon) {
        if (ribbon == null) return 0;
        String text = ribbon.trim();
        try {
            int value = Integer.parseInt(text);
            return value >= 1 && value <= count() ? value : 0;
        } catch (NumberFormatException ignored) {
            // a constant name
        }
        for (int i = 0; i < CONSTANTS.length; i++) {
            if (CONSTANTS[i].equalsIgnoreCase(text)) return i + 1;
        }
        return 0;
    }

    public static int count() {
        return 80;
    }

    public static String name(int id) {
        return id >= 0 && id < NAMES.length ? NAMES[id] : "";
    }

    public static String description(int id) {
        return id >= 0 && id < DESCRIPTIONS.length ? DESCRIPTIONS[id] : "";
    }

    private static final String[] NAMES = {
            "",
            "酷缎带", "酷缎带超级", "酷缎带超极", "酷缎带大师",
            "美丽缎带", "美丽缎带超级", "美丽缎带超极", "美丽缎带大师",
            "可爱缎带", "可爱缎带超级", "可爱缎带超极", "可爱缎带大师",
            "聪明缎带", "聪明缎带超级", "聪明缎带超极", "聪明缎带大师",
            "坚韧缎带", "坚韧缎带超级", "坚韧缎带超极", "坚韧缎带大师",
            "酷缎带", "酷缎带极好", "酷缎带极致", "酷缎带大师",
            "美丽缎带", "美丽缎带极好", "美丽缎带极致", "美丽缎带大师",
            "可爱缎带", "可爱缎带极好", "可爱缎带极致", "可爱缎带大师",
            "聪明缎带", "聪明缎带极好", "聪明缎带极致", "聪明缎带大师",
            "坚韧缎带", "坚韧缎带极好", "坚韧缎带极致", "坚韧缎带大师",
            "获胜缎带", "胜利缎带", "能力缎带", "伟大能力缎带",
            "双重能力缎带", "多重能力缎带", "双人能力缎带", "世界能力缎带",
            "冠军缎带", "新奥冠军缎带", "记录缎带", "活动缎带", "传说缎带",
            "华丽缎带", "皇家缎带", "华丽皇家缎带", "警觉缎带", "震惊缎带",
            "沮丧缎带", "粗心缎带", "放松缎带", "小憩缎带", "微笑缎带",
            "足迹缎带", "艺术家缎带", "努力缎带", "生日缎带", "特别缎带",
            "经典缎带", "优先缎带", "纪念缎带", "愿望缎带", "国家缎带",
            "国家缎带", "战斗冠军缎带", "区域冠军缎带", "大地缎带", "世界缎带",
            "国家冠军缎带", "世界冠军缎带"
    };

    private static final String[] DESCRIPTIONS = {
            "",
            "普通酷缎带普通排名获胜者！", "普通酷缎带超级排名获胜者！",
            "普通酷缎带超极排名获胜者！", "普通酷缎带大师排名获胜者！",
            "普通美丽缎带普通排名获胜者！", "普通美丽缎带超级排名获胜者！",
            "普通美丽缎带超极排名获胜者！", "普通美丽缎带大师排名获胜者！",
            "普通可爱缎带普通排名获胜者！", "普通可爱缎带超级排名获胜者！",
            "普通可爱缎带超极排名获胜者！", "普通可爱缎带大师排名获胜者！",
            "普通聪明缎带普通排名获胜者！", "普通聪明缎带超级排名获胜者！",
            "普通聪明缎带超极排名获胜者！", "普通聪明缎带大师排名获胜者！",
            "普通坚韧缎带普通排名获胜者！", "普通坚韧缎带超级排名获胜者！",
            "普通坚韧缎带超极排名获胜者！", "普通坚韧缎带大师排名获胜者！",
            "超级缎带酷类普通排名获胜者！", "超级缎带酷类极好排名获胜者！",
            "超级缎带酷类极致排名获胜者！", "超级缎带酷类大师排名获胜者！",
            "超级缎带美丽类普通排名获胜者！", "超级缎带美丽类极好排名获胜者！",
            "超级缎带美丽类极致排名获胜者！", "超级缎带美丽类大师排名获胜者！",
            "超级缎带可爱类普通排名获胜者！", "超级缎带可爱类极好排名获胜者！",
            "超级缎带可爱类极致排名获胜者！", "超级缎带可爱类大师排名获胜者！",
            "超级缎带聪明类普通排名获胜者！", "超级缎带聪明类极好排名获胜者！",
            "超级缎带聪明类极致排名获胜者！", "超级缎带聪明类大师排名获胜者！",
            "超级缎带坚韧类普通排名获胜者！", "超级缎带坚韧类极好排名获胜者！",
            "超级缎带坚韧类极致排名获胜者！", "超级缎带坚韧类大师排名获胜者！",
            "获得的缎带奖励，来自霍恩战塔Lv.50挑战。",
            "获得的缎带奖励，来自霍恩战塔Lv.100挑战。",
            "获得的缎带奖励，来自战塔击败塔主。",
            "获得的缎带奖励，来自战塔击败塔主。",
            "获得的缎带奖励，来自战塔双打挑战。",
            "获得的缎带奖励，来自战塔多重挑战。",
            "获得的缎带奖励，来自战塔联机多重挑战。",
            "获得的缎带奖励，来自Wi-Fi战塔挑战。",
            "在另一个区域通过宝可梦联盟并进入名人堂时获得的缎带。",
            "击败新奥冠军并进入名人堂时获得的缎带。",
            "为创下令人惊叹的记录获得的缎带。",
            "宝可梦活动参与缎带。",
            "创下传说般记录获得的缎带。",
            "一条极其华丽与奢华的缎带。",
            "一条极其皇家且充满贵族气息的缎带。",
            "一条华丽的皇家缎带，是华丽的巅峰之作。",
            "一条回忆振奋人心事件的缎带，充满生命的能量。",
            "一条回忆令人激动的事件的缎带，让生活充满兴奋。",
            "一条回忆带来悲伤的事件的缎带，为生活增添了风味。",
            "一条回忆粗心错误的缎带，帮助调整生活决策。",
            "一条回忆清新事件的缎带，为生活增添光彩。",
            "一条回忆深沉熟睡的缎带，使生活变得宁静。",
            "一条回忆微笑的缎带，丰富了生活的品质。",
            "被认为拥有最佳质量足迹的宝可梦获得的缎带。",
            "在霍恩担任超级速写模特时获得的缎带。",
            "为一位异常努力的工作者颁发的缎带。",
            "庆祝生日的缎带。",
            "为特殊日子准备的特别缎带。",
            "表达对宝可梦的爱的缎带。",
            "特别假日缎带。",
            "珍惜特殊回忆的缎带。",
            "传说愿望成真的缎带。",
            "为克服所有艰难挑战所获得的缎带。",
            "宝可梦联盟冠军缎带。",
            "战斗大赛冠军缎带。",
            "宝可梦世界锦标赛区域冠军缎带。",
            "获得100场连胜的缎带。",
            "宝可梦联盟冠军缎带。",
            "宝可梦世界锦标赛国家冠军缎带。",
            "宝可梦世界锦标赛世界冠军缎带。"
    };
}
