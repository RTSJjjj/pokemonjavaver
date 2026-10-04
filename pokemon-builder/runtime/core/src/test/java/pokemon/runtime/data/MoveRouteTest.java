package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.3b-1: the real door route of Map002 event 1 must parse into the nine
 * commands the source project stores (play SE, four turns with waits, end).
 */
class MoveRouteTest {

    private static final String DOOR_ROUTE =
            "{\"class\":\"RPG::MoveRoute\",\"list\":["
                    + "{\"class\":\"RPG::MoveCommand\",\"parameters\":["
                    + "\"@{class=RPG::AudioFile; volume=100; name=Door enter; pitch=100}\"],\"code\":44},"
                    + "{\"class\":\"RPG::MoveCommand\",\"parameters\":[2],\"code\":15},"
                    + "{\"class\":\"RPG::MoveCommand\",\"parameters\":[],\"code\":17},"
                    + "{\"class\":\"RPG::MoveCommand\",\"parameters\":[2],\"code\":15},"
                    + "{\"class\":\"RPG::MoveCommand\",\"parameters\":[],\"code\":18},"
                    + "{\"class\":\"RPG::MoveCommand\",\"parameters\":[2],\"code\":15},"
                    + "{\"class\":\"RPG::MoveCommand\",\"parameters\":[],\"code\":19},"
                    + "{\"class\":\"RPG::MoveCommand\",\"parameters\":[2],\"code\":15},"
                    + "{\"class\":\"RPG::MoveCommand\",\"parameters\":[],\"code\":0}],"
                    + "\"skippable\":true,\"repeat\":false}";

    @Test
    @DisplayName("a real door route parses with its codes, flags and SE name")
    void parsesDoorRoute() {
        MoveRoute route = MoveRoute.parse(new JsonReader().parse(DOOR_ROUTE));
        assertEquals(9, route.size());
        assertFalse(route.repeat);
        assertTrue(route.skippable);
        assertEquals(44, route.commands.get(0).code);
        assertEquals("Door enter", route.commands.get(0).audioName());
        assertEquals(15, route.commands.get(1).code);
        assertEquals(2, route.commands.get(1).intParam(0, -1));
        assertEquals(17, route.commands.get(2).code);
        assertEquals(0, route.commands.get(8).code);
    }

    @Test
    @DisplayName("a missing route stays empty instead of failing")
    void toleratesMissingRoute() {
        assertEquals(0, MoveRoute.parse(null).size());
    }
}
