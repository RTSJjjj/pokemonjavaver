package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression for map6/event29 page 2: its autorun cutscene transfers the hero
 * with Transfer Player (201) to another tile of the SAME map and only then
 * flips self switch B. Essentials' {@code Scene_Map#transfer_player} sets the
 * map up only when the map id changes ({@code 0047.rb:71-73}), so an in-map
 * transfer must keep the current screen - rebuilding it drops the running
 * interpreter and restarts the still-active autorun page from command 0.
 */
class InMapTransferTest {

    @Test
    @DisplayName("a transfer inside the current map keeps the running screen")
    void transferInsideTheMapKeepsTheScreen() {
        assertTrue(MapScreen.transferStaysInMap(6, 6),
                "map6/event29 moves the hero to another tile of map 6");
    }

    @Test
    @DisplayName("a transfer to another map still rebuilds the screen")
    void transferToAnotherMapRebuilds() {
        assertFalse(MapScreen.transferStaysInMap(6, 7));
    }
}
