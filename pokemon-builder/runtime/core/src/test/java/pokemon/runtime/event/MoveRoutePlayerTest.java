package pokemon.runtime.event;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.map.Collision;
import pokemon.runtime.map.MapCharacter;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.3b-2: the move route player walks one step at a time, waits, turns and
 * reports its side effects - all headless (project3 section 61).
 */
class MoveRoutePlayerTest {

    private final List<String> sideEffects = new ArrayList<>();

    private MoveRoute route(String json) {
        return MoveRoute.parse(new JsonReader().parse(json));
    }

    private MoveRoutePlayer.Context context(boolean canStep) {
        return new MoveRoutePlayer.Context() {
            @Override
            public boolean step(MapCharacter character, int direction) {
                return canStep && character.startMove(Collision.targetX(character.x(), direction),
                        Collision.targetY(character.y(), direction), direction);
            }

            @Override
            public boolean stepDiagonal(MapCharacter character, int horz, int vert) {
                return canStep && character.startDiagonalMove(
                        character.x() + (horz == 4 ? -1 : 1),
                        character.y() + (vert == 8 ? -1 : 1));
            }

            @Override
            public int playerX() {
                return 0;
            }

            @Override
            public int playerY() {
                return 0;
            }

            @Override
            public void playSe(String name) {
                sideEffects.add("se:" + name);
            }

            @Override
            public void setSwitch(int id, boolean value) {
                sideEffects.add("switch:" + id + "=" + value);
            }

            @Override
            public void warn(String message) {
                sideEffects.add("warn:" + message);
            }
        };
    }

    /** Context with a configurable player tile (codes 10/11/25/26 read it). */
    private MoveRoutePlayer.Context contextFor(int playerX, int playerY, boolean canStep) {
        return new MoveRoutePlayer.Context() {
            @Override
            public boolean step(MapCharacter character, int direction) {
                return canStep && character.startMove(Collision.targetX(character.x(), direction),
                        Collision.targetY(character.y(), direction), direction);
            }

            @Override
            public boolean stepDiagonal(MapCharacter character, int horz, int vert) {
                return canStep && character.startDiagonalMove(
                        character.x() + (horz == 4 ? -1 : 1),
                        character.y() + (vert == 8 ? -1 : 1));
            }

            @Override
            public int playerX() {
                return playerX;
            }

            @Override
            public int playerY() {
                return playerY;
            }

            @Override
            public void warn(String message) {
                sideEffects.add("warn:" + message);
            }
        };
    }

    @Test
    @DisplayName("turning commands change the facing and finish the route")
    void turnsAndFinishes() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":17},{\"code\":19},{\"code\":0}],\"repeat\":false,\"skippable\":true}"));
        player.update(0f, character, context(true));
        assertTrue(player.finished());
        assertEquals(8, character.direction());
    }

    @Test
    @DisplayName("wait commands delay the route by their 1/20 s amount")
    void waits() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":15,\"parameters\":[4]},{\"code\":0}],\"skippable\":true}"));
        player.update(0.1f, character, context(true));
        assertFalse(player.finished());
        player.update(0.2f, character, context(true));
        assertTrue(player.finished());
    }

    @Test
    @DisplayName("a step is started once and the route waits for it to land")
    void stepsWaitForTheCharacter() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":3},{\"code\":0}],\"skippable\":true}"));
        player.update(0f, character, context(true));
        assertTrue(character.isMoving());
        assertEquals(3, character.x()); // the tile only changes when the step lands
        assertFalse(player.finished());

        character.update(0.5f);
        assertEquals(4, character.x());
        player.update(0f, character, context(true));
        assertTrue(player.finished());
    }

    @Test
    @DisplayName("blocked steps are skipped when the route is skippable")
    void blockedStepsAreSkipped() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":3},{\"code\":0}],\"skippable\":true}"));
        player.update(0f, character, context(false));
        assertTrue(player.finished());
        assertEquals(3, character.x());
    }

    @Test
    @DisplayName("jump (14) lands on the target tile and blocks the route until it does")
    void jumps() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":14,\"parameters\":[0,1]},{\"code\":0}],\"skippable\":true}"));
        player.update(0f, character, jumpContext(true));
        assertTrue(character.isJumping(), "the jump started");
        assertEquals(3, character.x());
        assertEquals(4, character.y(), "RMXP moves the tile right away");
        assertEquals(2, character.direction(), "and faces the jump direction");
        assertFalse(player.finished(), "the route waits for the landing");

        character.update(0.6f); // a vertical tile takes 0.5 s in this engine
        assertFalse(character.isJumping());
        player.update(0f, character, jumpContext(true));
        assertTrue(player.finished());
        assertEquals(4, character.y());
    }

    @Test
    @DisplayName("a jump in place only bobs and keeps the tile")
    void jumpInPlace() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":14,\"parameters\":[0,0]},{\"code\":0}],\"skippable\":true}"));
        player.update(0f, character, jumpContext(true));
        assertTrue(character.isJumping());
        assertEquals(3, character.x());
        assertEquals(3, character.y());
        character.update(0.3f); // 10 frames at 40 fps = 0.25 s
        assertFalse(character.isJumping());
        player.update(0f, character, jumpContext(true));
        assertTrue(player.finished());
    }

    @Test
    @DisplayName("a blocked jump is skipped when the route is skippable")
    void blockedJumpIsSkipped() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":14,\"parameters\":[0,-2]},{\"code\":0}],\"skippable\":true}"));
        player.update(0f, character, jumpContext(false));
        assertTrue(player.finished());
        assertFalse(character.isJumping());
        assertEquals(3, character.y());
    }

    @Test
    @DisplayName("change graphic (41) swaps the sheet, facing and pattern")
    void changeGraphic() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "BW 071");
        character.basePattern(2);
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":41,\"parameters\":[\"BW 072\",0,2,0]},{\"code\":0}],"
                        + "\"skippable\":true}"));
        player.update(0f, character, context(true));
        assertTrue(player.finished());
        assertEquals("BW 072", character.characterName);
        assertEquals(2, character.direction());
        assertEquals(0, character.basePattern);
    }

    /** Jump context: canLand mirrors the map side (Collision.canLand). */
    private MoveRoutePlayer.Context jumpContext(boolean canLand) {
        return new MoveRoutePlayer.Context() {
            @Override
            public boolean step(MapCharacter character, int direction) {
                return false;
            }

            @Override
            public boolean canLand(MapCharacter character, int x, int y) {
                return canLand;
            }
        };
    }

    @Test
    @DisplayName("play SE and switch commands reach the map side")
    void sideEffects() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":44,\"parameters\":[\"@{class=RPG::AudioFile; volume=100; "
                        + "name=Door enter; pitch=100}\"]},{\"code\":27,\"parameters\":[5]},"
                        + "{\"code\":0}],\"skippable\":true}"));
        player.update(0f, character, context(true));
        assertEquals(List.of("se:Door enter", "switch:5=true"), sideEffects);
    }

    @Test
    @DisplayName("move toward player (10) walks the dominant axis (R6.32)")
    void towardThePlayer() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        // Player at (0,1): dx = -3 dominates dy = -2, so the character walks
        // left. (A tie is randomised by RGSS, so the tests stay off the tie.)
        MoveRoutePlayer towards = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":10},{\"code\":0}],\"skippable\":true}"));
        towards.update(0f, character, contextFor(0, 1, true));
        assertEquals(4, character.direction());
        assertTrue(character.isMoving());
        character.update(0.5f);
        assertEquals(2, character.x());

        // Move away (11) mirrors it.
        MapCharacter awayChar = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer away = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":11},{\"code\":0}],\"skippable\":true}"));
        away.update(0f, awayChar, contextFor(0, 1, true));
        assertEquals(6, awayChar.direction());
        awayChar.update(0.5f);
        assertEquals(4, awayChar.x());
    }

    @Test
    @DisplayName("code 10 falls back to the other axis when the dominant one is blocked (R6.32)")
    void towardThePlayerFallsBack() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        // Player at (0,1): left dominates, but only the vertical way is open.
        MoveRoutePlayer.Context onlyUp = new MoveRoutePlayer.Context() {
            @Override
            public boolean step(MapCharacter moved, int direction) {
                return direction == 8 && moved.startMove(moved.x(), moved.y() - 1, 8);
            }

            @Override
            public int playerX() {
                return 0;
            }

            @Override
            public int playerY() {
                return 1;
            }
        };
        MoveRoutePlayer towards = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":10},{\"code\":0}],\"skippable\":true}"));
        towards.update(0f, character, onlyUp);
        assertEquals(8, character.direction(), "the fallback axis was taken");
        assertTrue(character.isMoving());
    }

    @Test
    @DisplayName("turn toward / away from the player (25/26) and the direction fix (R6.32)")
    void turnTowardAndAway() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        character.face(2);
        // Player at (3,1): the character is south-east of it -> face up (8).
        MoveRoutePlayer towards = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":25},{\"code\":0}],\"skippable\":true}"));
        towards.update(0f, character, contextFor(3, 1, true));
        assertEquals(8, character.direction());

        MoveRoutePlayer away = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":26},{\"code\":0}],\"skippable\":true}"));
        away.update(0f, character, contextFor(3, 1, true));
        assertEquals(2, character.direction());

        // RGSS turn_generic: a fixed direction refuses to turn.
        MoveRoutePlayer fixed = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":35},{\"code\":25},{\"code\":0}],\"skippable\":true}"));
        fixed.update(0f, character, contextFor(3, 1, true));
        assertEquals(2, character.direction(), "direction fix keeps the facing");
    }

    @Test
    @DisplayName("move backward (13) keeps the facing while walking (R6.32)")
    void backwardKeepsFacing() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        character.face(6); // right
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":13},{\"code\":0}],\"skippable\":true}"));
        player.update(0f, character, context(true));
        assertTrue(character.isMoving());
        character.update(0.5f);
        assertEquals(2, character.x(), "walked one tile backwards");
        assertEquals(6, character.direction(), "without turning around");
        player.update(0f, character, context(true));
        assertTrue(player.finished());
    }

    @Test
    @DisplayName("diagonal steps (5-8) move both axes and apply the facing rule (R6.32)")
    void diagonalSteps() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        character.face(6); // right: upper-left flips the facing to left
        MoveRoutePlayer upperLeft = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":7},{\"code\":0}],\"skippable\":true}"));
        upperLeft.update(0f, character, context(true));
        assertEquals(4, character.direction());
        assertTrue(character.isMoving());
        character.update(0.5f);
        assertEquals(2, character.x());
        assertEquals(2, character.y());
        upperLeft.update(0f, character, context(true));
        assertTrue(upperLeft.finished());

        // lower-right from a down-facing character keeps the facing (RGSS).
        MapCharacter second = new MapCharacter(3, 3, 10, 10, "npc");
        second.face(2);
        MoveRoutePlayer lowerRight = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":6},{\"code\":0}],\"skippable\":true}"));
        lowerRight.update(0f, second, context(true));
        assertEquals(2, second.direction());
        second.update(0.5f);
        assertEquals(4, second.x());
        assertEquals(4, second.y());
    }

    @Test
    @DisplayName("a blocked diagonal is skipped when the route is skippable (R6.32)")
    void blockedDiagonalIsSkipped() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":7},{\"code\":0}],\"skippable\":true}"));
        player.update(0f, character, context(false));
        assertTrue(player.finished());
        assertEquals(3, character.x());
        assertEquals(3, character.y());
    }

    @Test
    @DisplayName("through, always-on-top and opacity codes change the character")
    void visibilityCodes() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":37},{\"code\":39},{\"code\":42,\"parameters\":[0]},"
                        + "{\"code\":38},{\"code\":40},{\"code\":42,\"parameters\":[255]},"
                        + "{\"code\":0}],\"skippable\":true}"));
        player.update(0f, character, context(true));
        assertTrue(player.finished());
        assertFalse(character.through);
        assertFalse(character.alwaysOnTop);
        assertEquals(1f, character.opacity);
    }

    @Test
    @DisplayName("turn toward player, direction fix and the flag codes reach the character")
    void turnAndFlagCodes() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        character.face(2);
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":25},{\"code\":35},{\"code\":33},{\"code\":3},"
                        + "{\"code\":34},{\"code\":30,\"parameters\":[6]},"
                        + "{\"code\":43,\"parameters\":[1]},{\"code\":0}],\"skippable\":true}"));
        player.update(0f, character, context(true));
        // The test player sits at (0,0): the character turns up and walks right
        // without turning, because code 35 fixed the facing.
        assertEquals(8, character.direction());
        assertTrue(character.directionFix);
        character.update(1f);
        assertEquals(4, character.x());
        assertEquals(8, character.direction(), "direction fix keeps the facing while walking");
        player.update(0f, character, context(true)); // the rest of the route runs now
        assertTrue(player.finished());
        assertFalse(character.stepAnime);
        assertEquals(6, character.moveFrequency);
        assertEquals(1, character.blendType);
    }

    @Test
    @DisplayName("a blocked non-skippable step gives up instead of freezing the player")
    void blockedRouteDoesNotFreeze() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":3},{\"code\":0}],\"skippable\":false}"));
        for (int i = 0; i < 40 && !player.finished(); i++) {
            player.update(0.11f, character, context(false));
        }
        assertTrue(player.finished(), "the route must not block the player forever");
        assertTrue(sideEffects.stream().anyMatch(effect -> effect.startsWith("warn:")),
                sideEffects.toString());
    }

    @Test
    @DisplayName("a repeating route runs one pass per frame, not a spin")
    void repeatingRouteDoesNotSpin() {
        MapCharacter character = new MapCharacter(3, 3, 10, 10, "npc");
        // A turn-only repeating route (the "idle NPC looks around" pattern).
        MoveRoutePlayer player = new MoveRoutePlayer(route(
                "{\"list\":[{\"code\":24},{\"code\":0}],\"repeat\":true,\"skippable\":true}"));
        player.update(1f / 60f, character, context(true));
        assertTrue(player.cycleDone(), "the pass wrapped around at End");
        assertFalse(player.finished(), "a repeating route never finishes");
        // One update must not turn the character hundreds of times: record the
        // facing of every single pass by watching the route's own loop guard.
        MoveRoutePlayer.Context counting = new MoveRoutePlayer.Context() {
            int turns;

            @Override
            public boolean step(MapCharacter moved, int direction) {
                return false;
            }

            @Override
            public void warn(String message) {
                sideEffects.add("warn:" + message);
            }
        };
        for (int i = 0; i < 10; i++) {
            player.update(1f / 60f, character, counting);
        }
        assertTrue(sideEffects.stream().noneMatch(effect -> effect.contains("256 commands")),
                "the guard must not trip: " + sideEffects);
    }
}
