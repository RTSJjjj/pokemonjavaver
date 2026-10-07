package pokemon.runtime.app;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.event.MapPort;
import pokemon.runtime.event.ScriptIr;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.14 regression: the interpreter talks to {@code RuntimeContext.mapPort},
 * which has to forward every command to the visible screen. It only forwarded
 * transfers and move routes, so Change Transparent Flag (208), Set Event
 * Location (202), Erase Event and "Wait for Move's Completion" (210) were
 * silently dropped in the real game - the hero never disappeared inside a door.
 */
class RuntimePortForwardingTest {

    private static EventCommand cmd(int index, int code, String parameters) {
        EventCommand command = new EventCommand();
        command.index = index;
        command.code = code;
        command.parameters = parameters == null ? null : new JsonReader().parse(parameters);
        return command;
    }

    private static Array<EventCommand> program(EventCommand... commands) {
        Array<EventCommand> program = new Array<>();
        for (EventCommand command : commands) {
            program.add(command);
        }
        return program;
    }

    @Test
    @DisplayName("208 / 202 / erase event / 210 reach the visible map screen")
    void commandsReachTheScreenPort() {
        RuntimeContext context = new RuntimeContext("test", new PokemonGame(null, -1, null), ".");
        context.loadRuntimeConfig();

        List<String> calls = new ArrayList<>();
        final int[][] screenVariable = new int[1][];
        context.bindScreenPort(new MapPort() {
            @Override
            public void transfer(int mapId, int x, int y, int direction) {
            }

            @Override
            public int[] getEventVariable(int eventId) {
                calls.add("getVar:" + eventId);
                return screenVariable[0];
            }

            @Override
            public void setEventVariable(int eventId, int[] value) {
                calls.add("setVar:" + eventId + ":" + java.util.Arrays.toString(value));
                screenVariable[0] = value == null ? null : value.clone();
            }

            @Override
            public void turnEvent(int eventId, int direction) {
                calls.add("turn:" + eventId + ":" + direction);
            }

            @Override
            public void setTransparent(int eventId, boolean transparent) {
                calls.add("transparent:" + eventId + "=" + transparent);
            }

            @Override
            public void setEventLocation(int eventId, int x, int y, int direction) {
                calls.add("location:" + eventId + "@" + x + "," + y + "," + direction);
            }

            @Override
            public void eraseEvent(int eventId) {
                calls.add("erase:" + eventId);
            }

            @Override
            public boolean anyRouteActive() {
                return true;
            }

            @Override
            public void changeMapSettings(int type, JsonValue parameters) {
                calls.add("settings:" + type + ":" + parameters);
            }

            @Override
            public void changeFogOpacity(int opacity) {
                calls.add("fogOpacity:" + opacity);
            }

            @Override
            public void scrollMap(int direction, int distance, int speed) {
                calls.add("scroll:" + direction + "," + distance + "," + speed);
            }

            @Override
            public float caveEntrance(boolean exiting) {
                calls.add("cave:" + exiting);
                return 1.1f;
            }

            @Override
            public void showAnimation(int characterId, int animationId) {
                calls.add("animation:" + characterId + "," + animationId);
            }

            @Override
            public void showTileAnimation(int animationId, int x, int y, int height) {
                calls.add("tileAnimation:" + animationId + "@" + x + "," + y + ":" + height);
            }

            @Override
            public float exclaim(int characterId, int animationId) {
                calls.add("exclaim:" + characterId + "," + animationId);
                return 7f;
            }

            @Override
            public float noticePlayer(int characterId) {
                calls.add("notice:" + characterId);
                return 3.5f;
            }

            @Override
            public boolean saveGame() {
                calls.add("save");
                return true;
            }

            @Override
            public float pushBoulder(int eventId) {
                calls.add("boulder:" + eventId);
                return 9f;
            }

            @Override
            public void togglePlateSwitches() {
                calls.add("plates");
            }
        });

        context.eventInterpreter().attachScriptIr(ScriptIr.of(new JsonReader().parse(
                "{\"commands\":["
                        + "{\"id\":\"test/cave\",\"ir\":"
                        + "{\"command\":\"CAVE_ENTRANCE\",\"exiting\":true}},"
                        + "{\"id\":\"test/exclaim\",\"ir\":"
                        + "{\"command\":\"EXCLAIM\",\"character\":7,\"animationId\":3}},"
                        + "{\"id\":\"test/notice\",\"ir\":"
                        + "{\"command\":\"NOTICE_PLAYER\",\"character\":0}},"
                        + "{\"id\":\"test/save\",\"ir\":{\"command\":\"SAVE_GAME\"}},"
                        + "{\"id\":\"test/boulder\",\"ir\":{\"command\":\"PUSH_BOULDER\"}},"
                        + "{\"id\":\"test/plates\",\"ir\":{\"command\":\"TOGGLE_PLATE_SWITCHES\"}}]}")));
        context.gameState().pokemonMapStrengthUsed(true);
        context.eventInterpreter().start(program(
                cmd(0, 208, "[0]"),
                cmd(1, 202, "[0,0,7,8,2]"),
                cmd(2, 203, "[8,4,4]"),
                cmd(3, 204, "[1,\"clouds2\",0,30,0,100,2,2]"),
                cmd(4, 206, "[0,10]"),
                cmd(5, 209, "[-1,{\"list\":[{\"code\":0}],\"skippable\":true}]"),
                cmd(6, 207, "[-1,3]"),
                scriptBlock(7, "test/cave")), 4, 6);
        context.eventInterpreter().update(0f);

        assertTrue(calls.contains("transparent:-1=true"), calls.toString());
        assertTrue(calls.contains("location:6@7,8,2"), calls.toString());
        assertTrue(calls.contains("scroll:8,4,4"), calls.toString());
        assertTrue(calls.stream().anyMatch(call ->
                call.startsWith("settings:1:") && call.contains("clouds2")), calls.toString());
        assertTrue(calls.contains("fogOpacity:10"), calls.toString());
        // R6.26: pbCaveEntrance has to reach the screen's band animation too.
        assertTrue(calls.contains("cave:true"), calls.toString());
        // R6.27: 207 has to reach the screen's animation layer as well.
        assertTrue(calls.contains("animation:-1,3"), calls.toString());
        // R6.28: the grass rustle path goes context port -> screen port.
        context.mapPort().showTileAnimation(1, 12, 23, 1);
        assertTrue(calls.contains("tileAnimation:1@12,23:1"), calls.toString());

        // R6.30: the new hooks forward as well. pbExclaim and the boulder push
        // report waits, so each runs as its own event block.
        context.eventInterpreter().start(program(scriptBlock(0, "test/exclaim")), 4, 6);
        context.eventInterpreter().update(0f);
        assertTrue(calls.contains("exclaim:7,3"), calls.toString());

        context.eventInterpreter().start(program(scriptBlock(0, "test/notice")), 4, 6);
        context.eventInterpreter().update(0f);
        assertTrue(calls.contains("notice:6"), calls.toString()); // character 0 = the running event

        context.eventInterpreter().start(program(scriptBlock(0, "test/save")), 4, 6);
        context.eventInterpreter().update(0f);
        assertTrue(calls.contains("save"), calls.toString());

        context.eventInterpreter().start(program(scriptBlock(0, "test/boulder")), 4, 6);
        context.eventInterpreter().update(0f);
        assertTrue(calls.contains("boulder:6"), calls.toString());

        context.eventInterpreter().start(program(scriptBlock(0, "test/plates")), 4, 6);
        context.eventInterpreter().update(0f);
        assertTrue(calls.contains("plates"), calls.toString());

        // The direct port path keeps the screen's return values.
        assertEquals(7f, context.mapPort().exclaim(1, 3), 0.001f);
        assertEquals(3.5f, context.mapPort().noticePlayer(0), 0.001f);
        assertTrue(context.mapPort().saveGame());
        assertEquals(9f, context.mapPort().pushBoulder(6), 0.001f);
        context.mapPort().togglePlateSwitches();

        // 210 must block on the screen's routes, not fall through the empty
        // default of the context port.
        context.eventInterpreter().start(program(cmd(0, 210, null)), 4, 6);
        context.eventInterpreter().update(0f);
        assertEquals(pokemon.runtime.event.InterpreterState.WAIT_MOVEMENT,
                context.eventInterpreter().state(), "210 waits for the screen");

        // P3 regression: the berry plants read / write the event variable and
        // turn the event through the same port; a dropped forward made planting
        // impossible (the write went into the default no-op).
        assertNull(context.mapPort().getEventVariable(9), "no variable yet");
        context.mapPort().setEventVariable(9, new int[] { 1, 395, 0, 7, 100, 0, 0, 0 });
        assertArrayEquals(new int[] { 1, 395, 0, 7, 100, 0, 0, 0 },
                context.mapPort().getEventVariable(9));
        context.mapPort().turnEvent(9, 2);
        assertTrue(calls.contains("getVar:9"), calls.toString());
        assertTrue(calls.contains("setVar:9:[1, 395, 0, 7, 100, 0, 0, 0]"), calls.toString());
        assertTrue(calls.contains("turn:9:2"), calls.toString());
    }

    /** A 355 script command pointing at an IR block, like the compiler emits. */
    private static EventCommand scriptBlock(int index, String blockId) {
        EventCommand command = new EventCommand();
        command.index = index;
        command.code = 355;
        command.scriptBlockId = blockId;
        return command;
    }
}
