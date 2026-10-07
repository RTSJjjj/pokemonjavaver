package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PScreen_RegionMap: the region map state - cursor bounds, region switching
 * (the switch-81 and current-region-3 gates) and the location lookups.
 */
class TownMapModelTest {

    private static PbsData.TownMap townMap() {
        PbsData.TownMap map = new PbsData.TownMap();
        PbsData.TownMapRegion region0 = new PbsData.TownMapRegion();
        region0.name = "柊埃地区";
        region0.filename = "mapRegion0.png";
        PbsData.TownMapPoint town = new PbsData.TownMapPoint();
        town.x = 22;
        town.y = 9;
        town.name = "茶月镇";
        town.description = "伊娟博士研究所";
        town.healMapId = 2;
        town.healX = 36;
        town.healY = 7;
        region0.points.add(town);
        PbsData.TownMapPoint gated = new PbsData.TownMapPoint();
        gated.x = 3;
        gated.y = 4;
        gated.name = "隐藏点";
        gated.switchId = 51;
        region0.points.add(gated);
        map.regions.add(region0);
        PbsData.TownMapRegion region1 = new PbsData.TownMapRegion();
        region1.name = "泷歧落地区";
        region1.filename = "mapRegion1.png";
        map.regions.add(region1);
        return map;
    }

    @Test
    @DisplayName("the cursor starts on the player's MapPosition and clamps to the 30x20 grid")
    void cursorStartAndBounds() {
        TownMapModel model = new TownMapModel(townMap(), -1, true, new int[] { 0, 22, 9 }, id -> false);
        assertEquals(0, model.region());
        assertEquals(22, model.mapX());
        assertEquals(9, model.mapY());
        assertEquals("茶月镇", model.location());
        assertEquals("伊娟博士研究所", model.details());
        assertArrayEquals(new int[] { 2, 36, 7 }, model.healingSpot());

        model.move(-30, -20);
        assertEquals(TownMapModel.LEFT, model.mapX());
        assertEquals(TownMapModel.TOP, model.mapY());
        model.move(100, 100);
        assertEquals(TownMapModel.RIGHT, model.mapX());
        assertEquals(TownMapModel.BOTTOM, model.mapY());
    }

    @Test
    @DisplayName("pbShowMap(region) opens another region at 0,0 (pbStartScene:91-95)")
    void requestedRegion() {
        TownMapModel model = new TownMapModel(townMap(), 1, true, new int[] { 0, 22, 9 }, id -> false);
        assertEquals(1, model.region());
        assertEquals(0, model.mapX());
        assertEquals(0, model.mapY());
        assertEquals("", model.location());
    }

    @Test
    @DisplayName("L/R wraps inside max_length; switch 81 unlocks the last region")
    void regionSwitching() {
        TownMapModel model = new TownMapModel(townMap(), -1, true, new int[] { 0, 22, 9 }, id -> false);
        assertTrue(model.switchRegion(-1));
        assertEquals(0, model.region(), "2 regions, max_length 1: 0 wraps to 0");
        assertTrue(model.switchRegion(1));
        assertEquals(0, model.region(), "the last region needs switch 81");

        TownMapModel unlocked = new TownMapModel(townMap(), -1, true,
                new int[] { 0, 22, 9 }, id -> id == 81);
        assertTrue(unlocked.switchRegion(1));
        assertEquals(1, unlocked.region());
        assertEquals(0, unlocked.mapX());
        assertEquals(0, unlocked.mapY());
        assertTrue(unlocked.switchRegion(1));
        assertEquals(0, unlocked.region(), "wraps back to 0");

        TownMapModel locked = new TownMapModel(townMap(), -1, true,
                new int[] { 3, 0, 0 }, id -> id == 81);
        assertFalse(locked.switchRegion(1), "region 3 never switches (PScreen_RegionMap:338)");
    }

    @Test
    @DisplayName("a switch-gated point shows only in the non-wall map mode (208/247)")
    void switchGatedPoint() {
        TownMapModel wall = new TownMapModel(townMap(), -1, true, new int[] { 0, 3, 4 }, id -> true);
        assertEquals("", wall.location(), "wallmap hides gated points even with the switch on");
        TownMapModel map = new TownMapModel(townMap(), -1, false, new int[] { 0, 3, 4 }, id -> true);
        assertEquals("隐藏点", map.location());
        TownMapModel off = new TownMapModel(townMap(), -1, false, new int[] { 0, 3, 4 }, id -> false);
        assertEquals("", off.location());
    }
}
