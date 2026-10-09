package pokemon.runtime.ui.menu;

import pokemon.runtime.pokemon.PbsData;

/**
 * PScreen_RegionMap: the state of {@code PokemonRegionMap_Scene}. The cursor,
 * the region switch, the location lookups and the healing spots are pure logic
 * so they stay headless-testable; {@link TownMapView} renders and feeds input.
 *
 * <p>Region switching follows the active (004_ESMM_Overwrite) {@code pbMapScene}:
 * L/R wrap inside {@code max_length = $game_switches[81] ? @mapdata.length :
 * @mapdata.length - 1} and never switch while the player's own region is 3
 * (PScreen_RegionMap:325,337-360).</p>
 */
public final class TownMapModel {

    /** PScreen_RegionMap:63-68. */
    public static final int LEFT = 0;
    public static final int TOP = 0;
    public static final int RIGHT = 29;
    public static final int BOTTOM = 19;

    private final PbsData.TownMap data;
    private final boolean wallmap;
    private final java.util.function.IntPredicate switchOn;
    private final int playerRegion;
    private int region;
    private int mapX;
    private int mapY;

    /**
     * @param data            generated/pbs/townmap.json (may be null)
     * @param requestedRegion pbShowMap's region argument (-1 = the player's)
     * @param wallmap         pbShowMap's wallmap argument
     * @param playerPosition  MetadataMapPosition of the current map
     *                        ([region,x,y]), or null
     * @param switchOn        {@code $game_switches} lookup
     */
    public TownMapModel(PbsData.TownMap data, int requestedRegion, boolean wallmap,
                        int[] playerPosition, java.util.function.IntPredicate switchOn) {
        this.data = data;
        this.wallmap = wallmap;
        this.switchOn = switchOn == null ? id -> false : switchOn;
        this.playerRegion = playerPosition == null || playerPosition.length < 3 ? -1 : playerPosition[0];
        // PScreen_RegionMap#pbStartScene:85-112.
        if (playerPosition == null || playerPosition.length < 3) {
            region = 0;
            mapX = LEFT;
            mapY = TOP;
        } else if (requestedRegion >= 0 && requestedRegion != playerPosition[0]
                && data != null && data.region(requestedRegion) != null) {
            region = requestedRegion;
            mapX = LEFT;
            mapY = TOP;
        } else {
            region = playerPosition[0];
            mapX = playerPosition[1];
            mapY = playerPosition[2];
        }
        if (data != null && data.size() > 0 && data.region(region) == null) {
            region = 0;
            mapX = LEFT;
            mapY = TOP;
        }
        mapX = Math.max(LEFT, Math.min(RIGHT, mapX));
        mapY = Math.max(TOP, Math.min(BOTTOM, mapY));
    }

    public int region() {
        return region;
    }

    public int mapX() {
        return mapX;
    }

    public int mapY() {
        return mapY;
    }

    /** {@code pbGetCurrentRegion} of the map the player stands on. */
    public int playerRegion() {
        return playerRegion;
    }

    public PbsData.TownMapRegion currentRegion() {
        return data == null ? null : data.region(region);
    }

    public boolean hasData() {
        return currentRegion() != null;
    }

    /** One Input.dir8 step (PScreen_RegionMap:87-108 clamps to the grid). */
    public void move(int dx, int dy) {
        mapX = Math.max(LEFT, Math.min(RIGHT, mapX + dx));
        mapY = Math.max(TOP, Math.min(BOTTOM, mapY + dy));
    }

    /** {@code next if pbGetCurrentRegion == 3} (PScreen_RegionMap:338/350). */
    public boolean canSwitchRegion() {
        return playerRegion != 3;
    }

    /**
     * {@code max_length = $game_switches[81] ? @mapdata.length :
     * @mapdata.length - 1} (PScreen_RegionMap:325): the last region only opens
     * when switch 81 is on.
     */
    public int regionLimit() {
        int count = data == null ? 0 : data.size();
        return switchOn.test(81) ? count : count - 1;
    }

    /** L/R: wraps inside {@link #regionLimit()} and resets the cursor (337-360). */
    public boolean switchRegion(int delta) {
        if (!canSwitchRegion()) {
            return false;
        }
        int limit = Math.max(1, regionLimit());
        int target = region + delta;
        if (target < 0) {
            target = limit - 1;
        } else if (target > limit - 1) {
            target = 0;
        }
        region = target;
        mapX = LEFT;
        mapY = TOP;
        return true;
    }

    /** {@code pbGetMapLocation} (PScreen_RegionMap:204-217). */
    public String location() {
        PbsData.TownMapPoint point = pointAtCursor();
        return point == null ? "" : point.name;
    }

    /** {@code pbGetMapDetails} (PScreen_RegionMap:243-256). */
    public String details() {
        PbsData.TownMapPoint point = pointAtCursor();
        return point == null ? "" : point.description;
    }

    /** {@code pbGetHealingSpot} (PScreen_RegionMap:258-270): [mapId,x,y]. */
    public int[] healingSpot() {
        PbsData.TownMapPoint point = pointAtCursor();
        return point == null || !point.isHealingSpot()
                ? null
                : new int[] { point.healMapId, point.healX, point.healY };
    }

    /**
     * {@code pbGetHealingSpot} as the fly mode reads it (PScreen_RegionMap:258-270): the point's own heal fields, without
     * the switch check the name lookup applies.
     */
    public int[] flySpot() {
        PbsData.TownMapRegion regionData = currentRegion();
        PbsData.TownMapPoint point = regionData == null ? null : regionData.pointAt(mapX, mapY);
        return point == null || !point.isHealingSpot() ? null : new int[] {point.healMapId, point.healX, point.healY};
    }

    /** Every healing spot of the current region (for the fly mode's markers). */
    public java.util.List<int[]> flySpots() {
        java.util.List<int[]> spots = new java.util.ArrayList<>();
        PbsData.TownMapRegion regionData = currentRegion();
        if (regionData != null) {
            for (PbsData.TownMapPoint point : regionData.points) {
                if (point.isHealingSpot()) {
                    spots.add(new int[] {point.x, point.y, point.healMapId, point.healX, point.healY});
                }
            }
        }
        return spots;
    }

    /**
     * The visible point at the cursor: the plugin hides a point with a switch
     * unless {@code !loc[7] || (!@wallmap && $game_switches[loc[7]])}
     * (PScreen_RegionMap:208,247).
     */
    private PbsData.TownMapPoint pointAtCursor() {
        PbsData.TownMapRegion regionData = currentRegion();
        if (regionData == null) {
            return null;
        }
        PbsData.TownMapPoint point = regionData.pointAt(mapX, mapY);
        if (point == null) {
            return null;
        }
        if (point.switchId != null && !(!wallmap && switchOn.test(point.switchId))) {
            return null;
        }
        return point;
    }
}
