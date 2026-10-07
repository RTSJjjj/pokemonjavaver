package pokemon.runtime.field;

/**
 * PField_BerryPlants with {@code NEW_BERRY_PLANTS = true} (Settings:69): the
 * Gen 4 berry growth. The plant state is the event variable read/written through
 * {@code Interpreter#getVariable} / {@code #setVariable}:
 *
 * <pre>
 *   [0] growth stage 0-5  (1 planted, 2 sprouted, 3 taller, 4 flowering, 5 berries)
 *   [1] berry item id
 *   [2] seconds alive
 *   [3] timestamp of the last checkup
 *   [4] dampness 0-100
 *   [5] replant count
 *   [6] yield penalty
 *   [7] mulch item id
 * </pre>
 *
 * <p>Every number here is transcribed from {@code updatePlantDetails}
 * (PField_BerryPlants:147-217) and the picking branch (line 480-495).</p>
 */
public final class BerryPlants {

    /** {@code REPLANTS} (PField_BerryPlants:112). */
    public static final int REPLANTS = 9;
    /** {@code pbGetBerryPlantData} fallback: hours/stage, drying/hour, min, max. */
    public static final int[] DEFAULT_PLANT_DATA = { 3, 15, 2, 5 };

    private BerryPlants() { }

    /** {@code pbGetTimeNow.to_i} - the plugin grows plants from wall-clock time. */
    public static long now() {
        return System.currentTimeMillis() / 1000L;
    }

    /**
     * {@code updatePlantDetails(berryData)} for the Gen 4 branch. Mutates and
     * returns {@code berryData}; a plant that replanted too often becomes the
     * all-zero array.
     *
     * @param mulch the internal name of {@code berryData[7]} (the caller resolves
     *              the id through PbsData, mirroring {@code isConst?})
     */
    public static int[] update(int[] berryData, long nowSeconds, int[] values, String mulch) {
        if (berryData == null || berryData[0] == 0) {
            return berryData;
        }
        long timePerStage = values[0] * 3600L;
        int dryingRate = values[1];
        long timeDiff = nowSeconds - berryData[3];
        if (timeDiff <= 0) {
            return berryData;
        }
        berryData[3] = (int) nowSeconds;
        // Mulch modifiers (PField_BerryPlants:159-172).
        int maxReplants = REPLANTS;
        int ripeStages = 4;
        if ("GROWTHMULCH".equals(mulch)) {
            timePerStage = (long) (timePerStage * 0.75);
            dryingRate = (int) Math.ceil(dryingRate * 1.5);
        } else if ("DAMPMULCH".equals(mulch)) {
            timePerStage = (long) (timePerStage * 1.25);
            dryingRate = (int) Math.floor(dryingRate * 0.5);
        } else if ("GOOEYMULCH".equals(mulch)) {
            maxReplants = (int) Math.ceil(maxReplants * 1.5);
        } else if ("STABLEMULCH".equals(mulch)) {
            ripeStages = 6;
        }
        // Cycle through all replants since the last check (line 174-193).
        while (true) {
            int secondsAlive = berryData[2];
            int growingLife = berryData[5] > 0 ? 3 : 4;
            int numLifeStages = growingLife + ripeStages;
            if (secondsAlive + timeDiff < timePerStage * numLifeStages) {
                break;
            }
            if (berryData[5] >= maxReplants) {
                return new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
            }
            berryData[0] = 2;   // replants start in the sprouting stage
            berryData[2] = 0;
            berryData[5] += 1;
            berryData[6] = 0;
            timeDiff -= timePerStage * numLifeStages - secondsAlive;
        }
        // Update the current stage and the dampness (line 194-217).
        if (berryData[0] > 0) {
            int oldLifetime = berryData[2];
            long newLifetime = oldLifetime + timeDiff;
            if (berryData[0] < 5) {
                berryData[0] = (int) (1 + newLifetime / timePerStage);
                if (berryData[5] > 0) {
                    berryData[0] += 1;   // replants start at stage 2
                }
                if (berryData[0] > 5) {
                    berryData[0] = 5;
                }
            }
            berryData[2] = (int) newLifetime;
            int growingLife = berryData[5] > 0 ? 3 : 4;
            int oldHourTick = oldLifetime / 3600;
            int newHourTick = (int) (Math.min(newLifetime, timePerStage * growingLife) / 3600);
            for (int i = 0; i < newHourTick - oldHourTick; i++) {
                if (berryData[4] > 0) {
                    berryData[4] = Math.max(berryData[4] - dryingRate, 0);
                } else {
                    berryData[6] += 1;   // dry plants lose yield
                }
            }
        }
        return berryData;
    }

    /**
     * The berry count of a ripe plant (PField_BerryPlants:484-486):
     * {@code [maxYield - penalty, minYield].max}.
     */
    public static int yield(int[] berryData, int[] values) {
        return Math.max(values[3] - berryData[6], values[2]);
    }

    /** {@code pbIsMulch?}: the four mulch items (PItem_ItemEffects). */
    public static boolean isMulch(String internalName) {
        return "GROWTHMULCH".equals(internalName) || "DAMPMULCH".equals(internalName)
                || "GOOEYMULCH".equals(internalName) || "STABLEMULCH".equals(internalName);
    }
}
