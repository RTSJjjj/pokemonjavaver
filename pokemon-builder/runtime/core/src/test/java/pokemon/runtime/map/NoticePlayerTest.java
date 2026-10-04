package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * L6: {@code pbNoticePlayer}'s turn rule (Essentials {@code pbTurnTowardEvent}):
 * the dominant axis wins, ties go vertical, the same tile keeps the direction.
 */
class NoticePlayerTest {

    private static MapCharacter at(int x, int y, int direction) {
        MapCharacter character = new MapCharacter(x, y, 20, 20, null);
        character.face(direction);
        return character;
    }

    @Test
    @DisplayName("the player turns toward the event's dominant axis (L6)")
    void turnsToward() {
        MapCharacter player = at(5, 5, 2);
        assertEquals(6, MapScreen.directionToward(player, at(8, 5, 2)), "event to the east");
        assertEquals(4, MapScreen.directionToward(player, at(1, 5, 2)), "event to the west");
        assertEquals(2, MapScreen.directionToward(player, at(5, 9, 2)), "event to the south");
        assertEquals(8, MapScreen.directionToward(player, at(5, 1, 2)), "event to the north");
    }

    @Test
    @DisplayName("ties go vertical like pbTurnTowardEvent (L6)")
    void tiesGoVertical() {
        MapCharacter player = at(5, 5, 6);
        assertEquals(2, MapScreen.directionToward(player, at(7, 7, 2)), "event to the south-east");
        assertEquals(8, MapScreen.directionToward(player, at(3, 3, 2)), "event to the north-west");
        assertEquals(8, MapScreen.directionToward(player, at(7, 3, 2)), "equal distance -> vertical axis");
    }

    @Test
    @DisplayName("the same tile keeps the current direction (L6)")
    void sameTileKeepsDirection() {
        assertEquals(4, MapScreen.directionToward(at(5, 5, 4), at(5, 5, 2)));
    }
}
