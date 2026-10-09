package pokemon.runtime.field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 189_PItem_ItemEffects:55-63 / :318-323: where the rod may be used. */
class FishingRodTest {

    @Test
    @DisplayName("the rod needs water in front; a cliff edge only blocks it on foot")
    void whereTheRodWorks() {
        assertTrue(ItemHandlers.canFish(true, true, false), "water ahead, standing on land");
        assertFalse(ItemHandlers.canFish(false, true, false), "no water ahead");
        assertFalse(ItemHandlers.canFish(true, false, false), "a cliff edge: !notCliff && !surfing");
        assertTrue(ItemHandlers.canFish(true, false, true), "surfing ignores the cliff check");
        assertFalse(ItemHandlers.canFish(false, false, true));
    }

    @Test
    @DisplayName("the rod is a map item: the bag closes and UseInField runs on the map")
    void rodIsAMapItem() {
        assertTrue(ItemHandlers.isMapItem("SUPERROD"));
        assertFalse(ItemHandlers.isMapItem("REPEL"));
        assertFalse(ItemHandlers.isMapItem(null));
    }

    @Test
    @DisplayName("the water tags are the plugin's isWater?")
    void waterTags() {
        assertTrue(PBTerrain.isWater(PBTerrain.WATER));
        assertTrue(PBTerrain.isWater(PBTerrain.DEEP_WATER));
        assertFalse(PBTerrain.isWater(PBTerrain.GRASS));
    }
}
