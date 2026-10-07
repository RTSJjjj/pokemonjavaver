package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage 4 / M0: {@link PBTargets} (PBTargets.rb:1-70) and its PBS spelling map.
 *
 * <p>The round-trip table pins the plugin's deliberately <b>out-of-order</b>
 * numbers (PBTargets.rb:2-3: "These numbers are all over the place because of
 * backwards compatibility"), and the 16 real values are the ones actually
 * present in this project's {@code PBS/moves.txt} / {@code
 * generated/pbs/moves.json} (1010 moves; the only unused constant, {@code Foe},
 * is the Poké Ball throw target created at runtime).</p>
 */
class PBTargetsTest {

    /** {@code PBTargets} constant name -> value, in PBTargets.rb:4-20 order. */
    private static final String[] CONSTANTS = {
            "None", "User", "NearAlly", "UserOrNearAlly", "UserAndAllies", "NearFoe",
            "RandomNearFoe", "AllNearFoes", "Foe", "AllFoes", "NearOther", "AllNearOthers",
            "Other", "AllBattlers", "UserSide", "FoeSide", "BothSides"
    };

    /** The same order's values (PBTargets.rb:4-20); out of order on purpose. */
    private static final int[] VALUES = {
            1, 10, 100, 200, 5, 400, 2, 4, 9, 6, 0, 8, 3, 7, 40, 80, 20
    };

    @Test
    @DisplayName("M0: every PBTargets constant keeps its plugin value (PBTargets.rb:4-20)")
    void constantsKeepTheirNumbers() {
        assertEquals(17, CONSTANTS.length);
        // Spot-check the constant fields themselves, not just the table below.
        assertEquals(1, PBTargets.None);
        assertEquals(10, PBTargets.User);
        assertEquals(100, PBTargets.NearAlly);
        assertEquals(200, PBTargets.UserOrNearAlly);
        assertEquals(5, PBTargets.UserAndAllies);
        assertEquals(400, PBTargets.NearFoe);
        assertEquals(2, PBTargets.RandomNearFoe);
        assertEquals(4, PBTargets.AllNearFoes);
        assertEquals(9, PBTargets.Foe);
        assertEquals(6, PBTargets.AllFoes);
        assertEquals(0, PBTargets.NearOther);
        assertEquals(8, PBTargets.AllNearOthers);
        assertEquals(3, PBTargets.Other);
        assertEquals(7, PBTargets.AllBattlers);
        assertEquals(40, PBTargets.UserSide);
        assertEquals(80, PBTargets.FoeSide);
        assertEquals(20, PBTargets.BothSides);
    }

    @Test
    @DisplayName("M0: nameOf/fromName round-trip for all 17 constants, case-insensitively")
    void roundTrip() {
        for (int i = 0; i < CONSTANTS.length; i++) {
            int value = VALUES[i];
            assertEquals(CONSTANTS[i], PBTargets.nameOf(value), "nameOf for " + CONSTANTS[i]);
            assertEquals(value, PBTargets.fromName(CONSTANTS[i]), "fromName for " + CONSTANTS[i]);
            assertEquals(value, PBTargets.fromName(CONSTANTS[i].toUpperCase()), "upper case");
            assertEquals(value, PBTargets.fromName(CONSTANTS[i].toLowerCase()), "lower case");
        }
    }

    @Test
    @DisplayName("M0: the 16 Target spellings of PBS/moves.txt map 1:1")
    void realMovesFileValues() {
        // distinct Target values of PBS/moves.txt (counts in the comment):
        // NearOther 687, User 107, AllNearFoes 87, Other 32, AllNearOthers 25,
        // BothSides 21, UserSide 13, UserAndAllies 8, RandomNearFoe 7, FoeSide 5,
        // None 5, NearAlly 4, AllBattlers 4, NearFoe 2, AllFoes 2, UserOrNearAlly 1
        assertEquals(PBTargets.NearOther, PBTargets.fromName("NearOther"));
        assertEquals(PBTargets.User, PBTargets.fromName("User"));
        assertEquals(PBTargets.AllNearFoes, PBTargets.fromName("AllNearFoes"));
        assertEquals(PBTargets.Other, PBTargets.fromName("Other"));
        assertEquals(PBTargets.AllNearOthers, PBTargets.fromName("AllNearOthers"));
        assertEquals(PBTargets.BothSides, PBTargets.fromName("BothSides"));
        assertEquals(PBTargets.UserSide, PBTargets.fromName("UserSide"));
        assertEquals(PBTargets.UserAndAllies, PBTargets.fromName("UserAndAllies"));
        assertEquals(PBTargets.RandomNearFoe, PBTargets.fromName("RandomNearFoe"));
        assertEquals(PBTargets.FoeSide, PBTargets.fromName("FoeSide"));
        assertEquals(PBTargets.None, PBTargets.fromName("None"));
        assertEquals(PBTargets.NearAlly, PBTargets.fromName("NearAlly"));
        assertEquals(PBTargets.AllBattlers, PBTargets.fromName("AllBattlers"));
        assertEquals(PBTargets.NearFoe, PBTargets.fromName("NearFoe"));
        assertEquals(PBTargets.AllFoes, PBTargets.fromName("AllFoes"));
        assertEquals(PBTargets.UserOrNearAlly, PBTargets.fromName("UserOrNearAlly"));

        // Foe (PBTargets.rb:12) is the ball-throw target: it has a name but no
        // moves.txt row, so it is the one constant the PBS file never uses.
        assertEquals("Foe", PBTargets.nameOf(PBTargets.Foe));
        assertEquals(PBTargets.Foe, PBTargets.fromName("Foe"));
    }

    @Test
    @DisplayName("M0: an unknown name throws and an unknown value has no name")
    void unknownInputs() {
        assertThrows(IllegalArgumentException.class, () -> PBTargets.fromName("AllNearFoe"));
        assertThrows(IllegalArgumentException.class, () -> PBTargets.fromName("nosuchtarget"));
        assertThrows(IllegalArgumentException.class, () -> PBTargets.fromName(""));
        assertThrows(IllegalArgumentException.class, () -> PBTargets.fromName(null));
        // The compiler accepts a bare number (csvEnumFieldOrInt!, Compiler:404-408);
        // this runtime reads generated/pbs/moves.json, where the Builder has
        // already resolved the name, so a number is not a legal input here.
        assertThrows(IllegalArgumentException.class, () -> PBTargets.fromName("3"));

        assertNull(PBTargets.nameOf(77), "no constant has that value");
        assertNull(PBTargets.nameOf(-1));
    }

    @Test
    @DisplayName("M0: the predicate methods follow PBTargets.rb:22-68")
    void predicates() {
        // noTargets? (:22-28)
        assertTrue(PBTargets.noTargets(PBTargets.None));
        assertTrue(PBTargets.noTargets(PBTargets.User));
        assertTrue(PBTargets.noTargets(PBTargets.UserSide));
        assertTrue(PBTargets.noTargets(PBTargets.FoeSide));
        assertTrue(PBTargets.noTargets(PBTargets.BothSides));
        assertFalse(PBTargets.noTargets(PBTargets.NearOther));

        // multipleTargets? (:36-42)
        assertTrue(PBTargets.multipleTargets(PBTargets.AllNearFoes));
        assertTrue(PBTargets.multipleTargets(PBTargets.AllNearOthers));
        assertTrue(PBTargets.multipleTargets(PBTargets.UserAndAllies));
        assertTrue(PBTargets.multipleTargets(PBTargets.AllFoes));
        assertTrue(PBTargets.multipleTargets(PBTargets.AllBattlers));
        assertFalse(PBTargets.multipleTargets(PBTargets.NearFoe));

        // oneTarget? (:31-34) = neither of the two above
        assertTrue(PBTargets.oneTarget(PBTargets.NearFoe));
        assertTrue(PBTargets.oneTarget(PBTargets.NearOther));
        assertTrue(PBTargets.oneTarget(PBTargets.RandomNearFoe));
        assertFalse(PBTargets.oneTarget(PBTargets.User));
        assertFalse(PBTargets.oneTarget(PBTargets.AllNearFoes));

        // targetsFoeSide? (:45-48)
        assertTrue(PBTargets.targetsFoeSide(PBTargets.FoeSide));
        assertTrue(PBTargets.targetsFoeSide(PBTargets.BothSides));
        assertFalse(PBTargets.targetsFoeSide(PBTargets.UserSide));

        // canChooseDistantTarget? (:50-52)
        assertTrue(PBTargets.canChooseDistantTarget(PBTargets.Other));
        assertFalse(PBTargets.canChooseDistantTarget(PBTargets.NearOther));

        // canChooseOneFoeTarget? (:55-59) and canChooseFoeTarget? (:64-68)
        // have the same body; both are asserted so a future divergence is caught.
        for (int target : new int[] {PBTargets.NearFoe, PBTargets.NearOther, PBTargets.Other,
                PBTargets.RandomNearFoe}) {
            assertTrue(PBTargets.canChooseOneFoeTarget(target), "canChooseOneFoeTarget " + target);
            assertTrue(PBTargets.canChooseFoeTarget(target), "canChooseFoeTarget " + target);
        }
        for (int target : new int[] {PBTargets.NearAlly, PBTargets.User, PBTargets.AllFoes,
                PBTargets.FoeSide}) {
            assertFalse(PBTargets.canChooseOneFoeTarget(target), "canChooseOneFoeTarget " + target);
            assertFalse(PBTargets.canChooseFoeTarget(target), "canChooseFoeTarget " + target);
        }
    }

    @Test
    @DisplayName("M0: the name table is complete and unique")
    void nameTableIsConsistent() {
        for (int i = 0; i < CONSTANTS.length; i++) {
            assertNotNull(PBTargets.nameOf(VALUES[i]), CONSTANTS[i] + " must have a name");
            for (int j = i + 1; j < CONSTANTS.length; j++) {
                assertFalse(VALUES[i] == VALUES[j],
                        CONSTANTS[i] + " and " + CONSTANTS[j] + " share " + VALUES[i]);
            }
        }
    }
}
