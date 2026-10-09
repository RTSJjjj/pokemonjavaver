package pokemon.runtime.map;

/**
 * 311_BW_SignPosts {@code LocationWindow} with the lists of 312_BW_SignPosts_Config: the board that slides down from the
 * top of the screen with the map's name when the player arrives on a map ({@code Events.onMapSceneChange},
 * 170_PField_Field:568-600, for maps whose metadata has {@code ShowArea}). The board's graphic comes from the words in the
 * map's name; a route's number is drawn with {@code Location/icon_numbers}; {@code SHOW_SEASONS} adds the season strip
 * that rises from the bottom.
 *
 * <p>登记: the mini map ({@code $miniMap}, 311:2-5 and :234-236) is not part of the game yet.</p>
 */
public final class LocationSignpost {
    /** 312_BW_SignPosts_Config:9-31. */
    static final String[] TOWN = {"镇"};
    static final String[] CITY = {"市", "城", "联盟", "岛", "幻谕岛"};
    static final String[] ROUTE = {"道路", "路", "小径", "之地"};
    static final String[] BRIDGE = {"桥"};
    static final String[] FOREST = {"森林", "林", "平原", "园", "之森", "祭坛", "红枫", "千夜岛", "远方孤岛", "心鸣岛"};
    static final String[] CAVE = {"洞", "穴", "地下", "曦寒山", "暮煦山", "遗迹", "之间", "微冰"};
    static final String[] PORT = {"塔", "幽寂遗迹", "彩虹山脉", "曦寒山腰", "曦寒山脚", "曦寒山顶"};
    static final String[] DESERT = {"原野", "牧场", "公园"};
    static final String[] WATER = {"湖", "海", "泉", "水路"};
    static final String[] MOUNTAIN = {"山脉", "丘", "巅"};
    /** 312_BW_SignPosts_Config:37. */
    static final boolean SHOW_SEASONS = true;

    private final String name;
    private final String board;
    private final boolean extendedRoute;
    /** {@code @route_number} (a String of digits, possibly empty), or null when the name is not a route. */
    private final String routeNumber;
    private final String season;
    private int frames;
    private boolean finished;
    /** The board's top edge, RGSS y (down). */
    private float windowY;
    /** The season strip's top edge, RGSS y (down). */
    private float seasonY;
    private final float boardHeight;
    private final float seasonHeight;

    /**
     * @param name         {@code $game_map.name}
     * @param month        1..12 ({@code pbGetSeason}: 3-5 spring, 6-8 summer, 9-11 autumn, else winter)
     * @param screenHeight {@code SCREEN_HEIGHT}
     */
    public LocationSignpost(String name, int month, float boardHeight, float seasonHeight, float screenHeight) {
        this.name = name == null ? "" : name;
        String found = null;
        boolean extended = false;
        String number = null;
        found = match(found, "town", TOWN, this.name);                      // :135-139
        found = match(found, "city", CITY, this.name);
        found = match(found, "bridge", BRIDGE, this.name);
        found = match(found, "forest", FOREST, this.name);
        found = match(found, "cave", CAVE, this.name);
        found = match(found, "port", PORT, this.name);
        found = match(found, "desert", DESERT, this.name);
        found = match(found, "water", WATER, this.name);
        found = match(found, "mountain", MOUNTAIN, this.name);
        if (contains(ROUTE, this.name)) {                                    // :170-181
            number = this.name.replaceAll("[^0-9]", "");
            int value = number.isEmpty() ? 0 : Integer.parseInt(number);
            extended = value >= 100;
            found = extended ? "route_extended" : "route";
        }
        this.board = found == null ? "none" : found;                         // :182-184
        this.extendedRoute = extended;
        this.routeNumber = number;
        this.season = month >= 3 && month <= 5 ? "Spring" : month >= 6 && month <= 8 ? "Summer"
                : month >= 9 && month <= 11 ? "Autumn" : "Winter";
        this.boardHeight = boardHeight;
        this.seasonHeight = seasonHeight;
        this.windowY = -boardHeight - 4;                                     // :185
        this.seasonY = screenHeight;                                         // :106
    }

    private static String match(String current, String board, String[] words, String name) {
        return contains(words, name) ? board : current;
    }

    private static boolean contains(String[] words, String name) {
        for (String word : words) {
            if (name.contains(word)) {
                return true;
            }
        }
        return false;
    }

    public String name() {
        return name;
    }

    /** The graphic under Graphics/Pictures/Location. */
    public String board() {
        return board;
    }

    public boolean extendedRoute() {
        return extendedRoute;
    }

    public String routeNumber() {
        return routeNumber;
    }

    /** The season strip's graphic, or null when {@code SHOW_SEASONS} is off. */
    public String season() {
        return SHOW_SEASONS ? season : null;
    }

    public float windowY() {
        return windowY;
    }

    public float seasonY() {
        return seasonY;
    }

    public boolean finished() {
        return finished;
    }

    /** One frame of {@code LocationWindow#update} (:253-285): slide down 10 frames, rest, slide up on frames 90-100. */
    public void tick() {
        if (finished) {
            return;
        }
        if (frames < 10) {
            windowY += (boardHeight + 0.1f) / 10f;
            seasonY -= seasonHeight / 10f;
        } else if (frames >= 90 && frames <= 100) {
            windowY -= (boardHeight + 0.1f) / 10f;
            seasonY += seasonHeight / 10f;
        } else if (frames > 101) {
            finished = true;
        }
        frames++;
    }

    /**
     * 311:211-228 {@code pbDrawRouteNumber}: the digits of {@code number.to_i.digits} - least significant first, as the
     * plugin draws them - as icon indexes.
     */
    public static int[] routeDigits(String number) {
        int value = number == null || number.isEmpty() ? 0 : Integer.parseInt(number);
        String text = Integer.toString(value);
        int[] digits = new int[text.length()];
        for (int i = 0; i < digits.length; i++) {
            digits[i] = text.charAt(text.length() - 1 - i) - '0';
        }
        return digits;
    }

    /** The first digit's x in the board (:186-205): 10 (+4 on the wide board) from two digits up, 20 for one. */
    public float routeNumberX() {
        int value = routeNumber == null || routeNumber.isEmpty() ? 0 : Integer.parseInt(routeNumber);
        float ox = extendedRoute ? 4f : 0f;
        return value >= 10 ? 10f + ox : 20f + ox;
    }

    /** The name's x in the board (:189-202): 72 on the wide board, else 56. */
    public float textX() {
        return extendedRoute ? 72f : 56f;
    }
}
