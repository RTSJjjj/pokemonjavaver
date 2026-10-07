package pokemon.runtime.field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PField_BerryPlants (NEW_BERRY_PLANTS) growth: the numbers below come from the
 * plugin's own branches - one stage per {@code hoursPerStage}, the drying rate
 * per hour, and the yield penalty of a plant that ran dry.
 */
class BerryPlantsTest {

    private static final int[] DATA = BerryPlants.DEFAULT_PLANT_DATA;   // [3,15,2,5]

    @Test
    @DisplayName("a planted berry advances one stage per hoursPerStage")
    void advancesOneStagePerPeriod() {
        long now = 1_000_000L;
        int[] plant = {1, 100, 0, (int) now, 100, 0, 0, 0};
        BerryPlants.update(plant, now + 3 * 3600L, DATA, null);
        assertEquals(2, plant[0], "stage 1 -> 2 after three hours");
        assertEquals(3 * 3600, plant[2], "seconds alive");
        assertEquals(55, plant[4], "dampness drops by the drying rate every hour (100 - 3*15)");
        assertEquals(0, plant[6], "a damp plant keeps its yield");
    }

    @Test
    @DisplayName("a ripe plant stops at stage 5 and a dry one loses yield")
    void ripensAndRunsDry() {
        long now = 1_000_000L;
        int[] plant = {1, 100, 0, (int) now, 100, 0, 0, 0};
        BerryPlants.update(plant, now + 5 * 3 * 3600L, DATA, null);
        assertEquals(5, plant[0], "growth is capped at stage 5");
        assertEquals(0, plant[4], "the plant dries out");
        assertEquals(5, plant[6], "the hours past empty add the yield penalty");
        assertEquals(2, BerryPlants.yield(plant, DATA), "max(5 - 5, 2)");
    }

    @Test
    @DisplayName("an empty plot stays empty and mulch is recognised")
    void emptyPlotAndMulch() {
        assertTrue(BerryPlants.isMulch("GROWTHMULCH"));
        assertFalse(BerryPlants.isMulch("POTION"));
        int[] empty = {0, 0, 0, 0, 0, 0, 0, 0};
        BerryPlants.update(empty, 9_999_999L, DATA, "DAMPMULCH");
        assertEquals(0, empty[0]);
    }

    @Test
    @DisplayName("a plant that replanted too often resets to an empty plot")
    void tooManyReplants() {
        long now = 1_000_000L;
        // At the replant cap: one full life (3 growing + 4 ripe stages) has passed.
        int timePerStage = 3 * 3600;
        int numLifeStages = 3 + 4;                       // replants > 0 -> 3 growing stages
        int secondsAlive = 4 * timePerStage;
        int[] plant = {5, 100, secondsAlive, (int) now, 100, BerryPlants.REPLANTS, 0, 0};
        int[] after = BerryPlants.update(plant, now + (long) secondsAlive + (long) timePerStage * numLifeStages,
                DATA, null);
        assertEquals(0, after[0], "too many replants -> empty soil");
    }
}
