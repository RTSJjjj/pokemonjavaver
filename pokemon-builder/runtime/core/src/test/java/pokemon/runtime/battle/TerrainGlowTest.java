package pokemon.runtime.battle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The platform glow colours of the battle terrains (BattleScreen#terrainGlowRgb). */
class TerrainGlowTest {

    @Test
    void everyTerrainHasItsOwnColourAndNoneHasNone() {
        assertNull(BattleScreen.terrainGlowRgb(PBBattleTerrains.None));
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int terrain = PBBattleTerrains.Electric; terrain <= PBBattleTerrains.Cold; terrain++) {
            float[] rgb = BattleScreen.terrainGlowRgb(terrain);
            assertNotNull(rgb, "terrain " + terrain);
            assertEquals(3, rgb.length);
            assertTrue(seen.add(java.util.Arrays.toString(rgb)), "terrain " + terrain + " repeats a colour");
        }
    }

    @Test
    void bugLureIsTealAndColdIsBlueWhite() {
        float[] bug = BattleScreen.terrainGlowRgb(PBBattleTerrains.BugLure);
        assertTrue(bug[1] > bug[0] && bug[2] > bug[0], "green and blue over red");
        float[] cold = BattleScreen.terrainGlowRgb(PBBattleTerrains.Cold);
        assertTrue(cold[2] >= cold[1] && cold[1] >= cold[0] && cold[0] > 128f, "light, leaning blue");
    }
}
