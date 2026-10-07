package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code PBBattleTerrains} (PBBattleTerrains.rb:1-19) - the
 * in-battle terrains caused by moves like Electric Terrain. Distinct from
 * {@code PBEnvironment} (the map's terrain, used for wild encounters) and from
 * the field's {@code terrain} slot.
 */
public final class PBBattleTerrains {

    private PBBattleTerrains() {
    }

    public static final int None = 0;
    public static final int Electric = 1;
    public static final int Grassy = 2;
    public static final int Misty = 3;
    public static final int Psychic = 4;

    /**
     * {@code BugLure = 5} - NOT in {@code PBBattleTerrains.rb:1-19}; the plugin
     * REOPENS {@code module PBBattleTerrains} in the {@code 场地} section and
     * adds it there ({@code 场地:6}). Kept here (rather than as a private
     * constant in a handler file) so there is a single definition.
     */
    public static final int BugLure = 5;

    /** {@code Cold = 6} - added by the {@code 场地} section ({@code 场地:255}), same reason as {@link #BugLure}. */
    public static final int Cold = 6;

    /** {@code PBBattleTerrains.animationName} (PBBattleTerrains.rb:10-18); {@code null} for {@code None}. */
    public static String animationName(int terrain) {
        switch (terrain) {
            case Electric: return "ElectricTerrain";
            case Grassy: return "GrassyTerrain";
            case Misty: return "MistyTerrain";
            case Psychic: return "PsychicTerrain";
            default: return null;
        }
    }
}
