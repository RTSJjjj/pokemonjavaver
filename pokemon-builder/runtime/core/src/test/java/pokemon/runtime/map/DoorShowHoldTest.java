package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R6.15: the arrival door show-hold. The door page hides the hero, opens the
 * door, then shows the hero right before the forced walk-out; the hero must
 * stay invisible until that step really starts.
 */
class DoorShowHoldTest {

    @Test
    @DisplayName("the hero stays hidden through the whole door opening")
    void holdsThroughTheDoorOpening() {
        DoorShowHold hold = new DoorShowHold();
        hold.hide();
        hold.requestShow();

        assertFalse(hold.update(1f / 60f, true, false), "still opening the door");
        assertFalse(hold.update(0.5f, true, false), "the page is still running");
        assertTrue(hold.isHolding(), "the hero is never shown early");
    }

    @Test
    @DisplayName("the walk-out step releases the hold")
    void walkOutStepReleases() {
        DoorShowHold hold = new DoorShowHold();
        hold.hide();
        hold.requestShow();

        assertFalse(hold.update(1f / 60f, true, false));
        assertTrue(hold.update(1f / 60f, true, true), "the step starts -> show");
        assertFalse(hold.isHolding());
    }

    @Test
    @DisplayName("a hide without a show request keeps holding (door still opening)")
    void hideWithoutShowRequestKeepsHolding() {
        DoorShowHold hold = new DoorShowHold();
        hold.hide();

        assertFalse(hold.update(1f / 60f, true, true),
                "the door page itself may still be opening; 208[1] not seen yet");
        assertTrue(hold.isHolding());
    }

    @Test
    @DisplayName("the page ending always releases the hero")
    void pageEndReleases() {
        DoorShowHold hold = new DoorShowHold();
        hold.hide();

        assertTrue(hold.update(1f / 60f, false, false), "never strand the hero");
        assertFalse(hold.isHolding());
    }

    @Test
    @DisplayName("a stuck page times out and gives the hero back")
    void timeoutReleases() {
        DoorShowHold hold = new DoorShowHold();
        hold.hide();

        assertFalse(hold.update(DoorShowHold.MAX_HOLD_SECONDS - 0.01f, true, false));
        assertTrue(hold.update(0.02f, true, false));
    }

    @Test
    @DisplayName("a show request without a hide is ignored")
    void showWithoutHideIsIgnored() {
        DoorShowHold hold = new DoorShowHold();
        hold.requestShow();

        assertFalse(hold.isHolding());
        assertFalse(hold.update(1f, true, true));
    }
}
