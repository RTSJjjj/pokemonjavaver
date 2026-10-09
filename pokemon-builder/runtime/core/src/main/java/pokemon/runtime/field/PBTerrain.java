package pokemon.runtime.field;

/** 169_PBTerrain: the terrain tags and their predicates. */
public final class PBTerrain {
    public static final int LEDGE = 1;
    public static final int GRASS = 2;
    public static final int SAND = 3;
    public static final int ROCK = 4;
    public static final int DEEP_WATER = 5;
    public static final int STILL_WATER = 6;
    public static final int WATER = 7;
    public static final int WATERFALL = 8;
    public static final int WATERFALL_CREST = 9;
    public static final int TALL_GRASS = 10;
    public static final int UNDERWATER_GRASS = 11;
    public static final int ICE = 12;
    public static final int NEUTRAL = 13;
    public static final int SOOT_GRASS = 14;
    public static final int BRIDGE = 15;
    public static final int PUDDLE = 16;

    private PBTerrain() {
    }

    /** {@code isWater?} (:22-28). */
    public static boolean isWater(int tag) {
        return tag == WATER || tag == STILL_WATER || tag == DEEP_WATER || tag == WATERFALL_CREST || tag == WATERFALL;
    }

    /** {@code isSurfable?} (:19-21). */
    public static boolean isSurfable(int tag) {
        return isWater(tag);
    }

    /** {@code isPassableWater?} (:30-35). */
    public static boolean isPassableWater(int tag) {
        return tag == WATER || tag == STILL_WATER || tag == DEEP_WATER || tag == WATERFALL_CREST;
    }

    /** {@code isJustWater?} (:37-41). */
    public static boolean isJustWater(int tag) {
        return tag == WATER || tag == STILL_WATER || tag == DEEP_WATER;
    }

    public static boolean isDeepWater(int tag) {
        return tag == DEEP_WATER;
    }

    /** {@code isWaterfall?} (:47-50). */
    public static boolean isWaterfall(int tag) {
        return tag == WATERFALL_CREST || tag == WATERFALL;
    }

    /** {@code isGrass?} (:52-56). */
    public static boolean isGrass(int tag) {
        return tag == GRASS || tag == TALL_GRASS || tag == UNDERWATER_GRASS || tag == SOOT_GRASS;
    }

    public static boolean isLedge(int tag) {
        return tag == LEDGE;
    }

    public static boolean isIce(int tag) {
        return tag == ICE;
    }

    public static boolean isBridge(int tag) {
        return tag == BRIDGE;
    }

    /** {@code hasReflections?} (:79-82). */
    public static boolean hasReflections(int tag) {
        return tag == STILL_WATER || tag == PUDDLE;
    }

    /** {@code onlyWalk?} (:84-87): tall grass and ice cannot be cycled over. */
    public static boolean onlyWalk(int tag) {
        return tag == TALL_GRASS || tag == ICE;
    }
}
