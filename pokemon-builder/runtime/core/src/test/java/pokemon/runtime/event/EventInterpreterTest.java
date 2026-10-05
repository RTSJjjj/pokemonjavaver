package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.state.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless R6 tests for the cross-frame event interpreter (project3 sections
 * 16-20, 27, 29, 61): message paging, choices, control commands, conditional
 * branches, common events and the "not implemented yet" policy.
 */
class EventInterpreterTest {

    private GameState state;
    private MessageService messages;
    private InputManager input;
    private EventInterpreter interpreter;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> transfers = new ArrayList<>();
    private final List<String> routes = new ArrayList<>();
    private boolean routeMoving;
    private PictureService pictures;
    private ScreenEffects effects;
    private ScriptIr scriptIr;
    private final List<String> erased = new ArrayList<>();
    private final List<String> mapEnvironment = new ArrayList<>();
    private final Map<Integer, Array<EventCommand>> commonEvents = new HashMap<>();

    @BeforeEach
    void setUp() {
        state = new GameState();
        state.enterMap(1, 0, 0);
        messages = new MessageService();
        pictures = new PictureService();
        effects = new ScreenEffects();
        scriptIr = ScriptIr.empty();
        erased.clear();
        mapEnvironment.clear();
        input = new InputManager();
        warnings.clear();
        transfers.clear();
        routes.clear();
        routeMoving = false;
        commonEvents.clear();
        pokemon.runtime.event.MapPort port = new pokemon.runtime.event.MapPort() {
            @Override
            public void transfer(int mapId, int x, int y, int direction) {
                transfers.add(mapId + "," + x + "," + y + "," + direction);
            }

            @Override
            public void setMoveRoute(int eventId, MoveRoute route) {
                routes.add(eventId + ":" + route.size());
            }

            @Override
            public boolean isMoving(int eventId) {
                return routeMoving;
            }

            @Override
            public void eraseEvent(int eventId) {
                erased.add(String.valueOf(eventId));
            }

            @Override
            public void changeMapSettings(int type, JsonValue parameters) {
                mapEnvironment.add("settings:" + type + ":" + parameters);
            }

            @Override
            public void changeFogOpacity(int opacity) {
                mapEnvironment.add("fogOpacity:" + opacity);
            }

            @Override
            public void scrollMap(int direction, int distance, int speed) {
                mapEnvironment.add("scroll:" + direction + "," + distance + "," + speed);
            }
        };
        interpreter = new EventInterpreter(state, messages, input, null,
                commonEvents::get, port, pictures, warnings::add);
        interpreter.attachScreenEffects(effects);
        interpreter.attachScriptIr(scriptIr);
    }

    // ------------------------------------------------------------------

    @Test
    @DisplayName("Show Text waits for confirm and pages two lines at a time (R6.31)")
    void textPaging() {
        interpreter.start(program(
                cmd(0, 101, 0, array("第一行")),
                cmd(1, 401, 0, array("第二行")),
                cmd(2, 401, 0, array("第三行")),
                cmd(3, 401, 0, array("第四行")),
                cmd(4, 401, 0, array("第五行"))), 1, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
        assertTrue(messages.visible());
        assertTrue(messages.waiting());
        assertEquals(2, messages.lines().size);
        assertEquals("第一行", messages.lines().first());

        press(GameAction.CONFIRM);
        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
        assertEquals(2, messages.lines().size);
        assertEquals("第三行", messages.lines().first());

        press(GameAction.CONFIRM);
        assertEquals(1, messages.lines().size);
        assertEquals("第五行", messages.lines().first());

        press(GameAction.CONFIRM);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertFalse(messages.visible());
    }

    @Test
    @DisplayName("Essentials markup becomes clean text plus a speaker name")
    void messageMarkup() {
        interpreter.start(program(
                cmd(0, 101, 0, array("\\xn[伊娟]\\PN, 准备好了吗？\\c[2]<ac>"))), 1, 5);
        interpreter.update(0f);
        assertEquals("伊娟", messages.speaker());
        // R6.31 keeps the drawable styling: the colour and centring tags stay.
        assertTrue(messages.lines().first().startsWith("训练家, 准备好了吗？"),
                messages.lines().first());
        assertTrue(messages.lines().first().contains("\\c[2]"), messages.lines().first());
        assertTrue(messages.lines().first().contains("<ac>"), messages.lines().first());
    }

    @Test
    @DisplayName("\\^ pages continue without waiting for input")
    void noWaitSuffix() {
        interpreter.start(program(
                cmd(0, 101, 0, array("快进\\^")),
                cmd(1, 121, 0, array(1, 1, 0))), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);
        assertFalse(messages.visible());
        assertTrue(state.switches().get(1));
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("choices run only the block of the selected option")
    void choicesRunSelectedBlock() {
        interpreter.start(program(
                cmd(0, 102, 0, array(array("是", "否"), 0)),
                cmd(1, 402, 0, array(0, "是")),
                cmd(2, 121, 1, array(1, 1, 0)),
                cmd(3, 402, 0, array(1, "否")),
                cmd(4, 121, 1, array(2, 2, 0)),
                cmd(5, 404, 0, null)), 1, 5);
        interpreter.update(0f);
        assertTrue(messages.choiceMode());
        assertEquals(2, messages.choices().size);

        press(GameAction.DOWN);
        press(GameAction.CONFIRM);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertFalse(state.switches().get(1));
        assertTrue(state.switches().get(2));
    }

    @Test
    @DisplayName("a 355 script and its 655 continuations run the block exactly once (R15)")
    void scriptContinuationsRunOnce() {
        scriptIr.put("block1", new com.badlogic.gdx.utils.JsonReader()
                .parse("{\"command\":\"GIVE_ITEM\",\"item\":\"POTION\",\"amount\":3}"));
        pokemon.runtime.state.Inventory inventory = new pokemon.runtime.state.Inventory();
        interpreter.attachInventory(inventory);
        EventCommand first = cmd(0, 355, 0, array("pbItemBall(:POTION, 3)"));
        first.scriptBlockId = "block1";
        EventCommand continuation = cmd(1, 655, 0, array(""));
        continuation.scriptBlockId = "block1";
        interpreter.start(program(first, continuation), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);
        assertFalse(interpreter.running());
        assertEquals(3, inventory.count("POTION"), "the block must not run twice");
    }

    @Test
    @DisplayName("two 355s merged into one block id run the block once (L6c)")
    void mergedScriptCommandsRunOnce() {
        scriptIr.put("block1", new com.badlogic.gdx.utils.JsonReader()
                .parse("{\"command\":\"GIVE_ITEM\",\"item\":\"POTION\",\"amount\":3}"));
        pokemon.runtime.state.Inventory inventory = new pokemon.runtime.state.Inventory();
        interpreter.attachInventory(inventory);
        EventCommand first = cmd(0, 355, 0, array("pbReceiveItem(:POTION, 3)"));
        first.scriptBlockId = "block1";
        EventCommand second = cmd(1, 355, 0, array("pbNoticePlayer(get_character(0))"));
        second.scriptBlockId = "block1";
        interpreter.start(program(first, second), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);
        assertFalse(interpreter.running());
        assertEquals(3, inventory.count("POTION"),
                "the merged block must not run twice for the second 355");
    }

    @Test
    @DisplayName("conditional branch runs the else block when the condition fails")
    void conditionalBranch() {
        Array<EventCommand> list = program(
                cmd(0, 111, 0, array(0, 5, 0)),
                cmd(1, 121, 1, array(1, 1, 0)),
                cmd(2, 411, 0, null),
                cmd(3, 121, 1, array(2, 2, 0)),
                cmd(4, 412, 0, null));
        interpreter.start(list, 1, 5);
        interpreter.update(0f);
        assertFalse(state.switches().get(1));
        assertTrue(state.switches().get(2));

        setUp();
        state.switches().set(5, true);
        interpreter.start(list, 1, 5);
        interpreter.update(0f);
        assertTrue(state.switches().get(1));
        assertFalse(state.switches().get(2));
    }

    @Test
    @DisplayName("character and button conditions read the live state")
    void characterAndButtonConditions() {
        state.setPlayerPosition(0, 0, 8); // facing up
        interpreter.start(program(
                cmd(0, 111, 0, array(6, -1, 8)),
                cmd(1, 121, 1, array(1, 1, 0)),
                cmd(2, 412, 0, null)), 1, 5);
        interpreter.update(0f);
        assertTrue(state.switches().get(1));

        interpreter.start(program(
                cmd(0, 111, 0, array(11, 2)), // RMXP button 2 = down
                cmd(1, 121, 1, array(2, 2, 0)),
                cmd(2, 412, 0, null)), 1, 5);
        input.press(GameAction.DOWN);
        interpreter.update(0f);
        assertTrue(state.switches().get(2));
    }

    @Test
    @DisplayName("simple script conditions answer switches and variables")
    void simpleScriptConditions() {
        state.switches().set(4, true);
        state.variables().set(6, 9);
        interpreter.start(program(
                cmd(0, 111, 0, array(12, "$game_switches[4]")),
                cmd(1, 121, 1, array(1, 1, 0)),
                cmd(2, 411, 0, null),
                cmd(3, 121, 1, array(2, 2, 0)),
                cmd(4, 412, 0, null),
                cmd(5, 111, 0, array(12, "$game_variables[6] >= 5")),
                cmd(6, 121, 1, array(3, 3, 0)),
                cmd(7, 412, 0, null)), 1, 5);
        interpreter.update(0f);
        assertTrue(state.switches().get(1));
        assertFalse(state.switches().get(2));
        assertTrue(state.switches().get(3));
        assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);
    }

    @Test
    @DisplayName("complex script conditions are reported and treated as false")
    void complexScriptConditionIsReported() {
        interpreter.start(program(
                cmd(0, 111, 0, array(12, "$PokemonBag.pbHasItem?(:TICKET)")),
                cmd(1, 121, 1, array(1, 1, 0)),
                cmd(2, 411, 0, null),
                cmd(3, 121, 1, array(2, 2, 0)),
                cmd(4, 412, 0, null)), 1, 5);
        interpreter.update(0f);
        assertTrue(state.switches().get(2));
        assertFalse(state.switches().get(1));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("R7")));
    }

    @Test
    @DisplayName("control switches, variables and self switches update the state")
    void controlCommands() {
        state.variables().set(11, 4);
        interpreter.start(program(
                cmd(0, 121, 0, array(1, 3, 0)),          // switches 1..3 on
                cmd(1, 122, 0, array(10, 10, 0, 0, 7)),  // variable 10 = 7
                cmd(2, 122, 0, array(10, 10, 1, 1, 11)), // variable 10 += variable 11
                cmd(3, 123, 0, array("A", 0))),          // self switch A on
                3, 9);
        interpreter.update(0f);
        assertTrue(state.switches().get(1));
        assertTrue(state.switches().get(3));
        assertEquals(11, state.variables().get(10));
        assertTrue(state.selfSwitches().get(3, 9, "A"));
        assertFalse(state.selfSwitches().get(3, 10, "A"));
    }

    @Test
    @DisplayName("common events nest and a self call is refused")
    void commonEvents() {
        commonEvents.put(7, program(
                cmd(0, 121, 0, array(7, 7, 0)),
                cmd(1, 116, 0, array(8))));
        commonEvents.put(8, program(cmd(0, 121, 0, array(8, 8, 0))));
        commonEvents.put(9, program(cmd(0, 116, 0, array(9))));

        interpreter.start(program(cmd(0, 116, 0, array(7))), 1, 5);
        interpreter.update(0f);
        assertTrue(state.switches().get(7));
        assertTrue(state.switches().get(8));
        assertEquals(InterpreterState.FINISHED, interpreter.state());

        warnings.clear();
        interpreter.start(program(cmd(0, 116, 0, array(9))), 1, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("recursive")));
    }

    @Test
    @DisplayName("Wait blocks the event until the timer elapses")
    void waitBlocksFrames() {
        interpreter.start(program(
                cmd(0, 106, 0, array(10)),
                cmd(1, 121, 0, array(1, 1, 0))), 1, 5);
        interpreter.update(0.25f);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state());
        assertFalse(state.switches().get(1));

        interpreter.update(0.6f); // the wait started during the previous frame
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(state.switches().get(1));
    }

    @Test
    @DisplayName("commands outside the first batch are logged and skipped")
    void unimplementedCommandIsSkipped() {
        interpreter.start(program(
                cmd(0, 205, 0, array(0, 5, 5, 0, 0)),   // Change Fog Colour: R6.4b
                cmd(1, 121, 0, array(1, 1, 0))), 1, 5);
        interpreter.update(0f);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("205")));
        assertTrue(state.switches().get(1));
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("Set Move Route hands the parsed route to the map port")
    void setMoveRoute() {
        JsonValue route = new com.badlogic.gdx.utils.JsonReader().parse(
                "{\"list\":[{\"code\":17},{\"code\":15,\"parameters\":[2]},"
                        + "{\"code\":0}],\"repeat\":false,\"skippable\":true}");
        interpreter.start(program(cmd(0, 209, 0, array(0, route))), 1, 5);
        interpreter.update(0f);
        assertEquals(List.of("5:3"), routes, "character 0 means the running event");
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("a named event id still routes to that event")
    void moveRouteTargetsAnotherEvent() {
        JsonValue route = new com.badlogic.gdx.utils.JsonReader().parse(
                "{\"list\":[{\"code\":17},{\"code\":0}],\"skippable\":true}");
        interpreter.start(program(cmd(0, 209, 0, array(7, route))), 1, 5);
        interpreter.update(0f);
        assertEquals(List.of("7:2"), routes);
    }

    @Test
    @DisplayName("Wait for Move's Completion blocks until the map port reports done")
    void waitForMovement() {
        routeMoving = true;
        interpreter.start(program(
                cmd(0, 210, 0, null),
                cmd(1, 121, 0, array(1, 1, 0))), 1, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.WAIT_MOVEMENT, interpreter.state());
        assertFalse(state.switches().get(1));

        routeMoving = false;
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(state.switches().get(1));
    }

    @Test
    @DisplayName("Fade Out and Fade In drive the screen effect state")
    void fadeCommands() {
        interpreter.start(program(cmd(0, 221, 0, array(20, 0))), 1, 5);
        interpreter.update(0f);
        effects.update(2f);
        assertEquals(255f, effects.fade());

        interpreter.start(program(cmd(0, 222, 0, array(20, 0))), 1, 5);
        interpreter.update(0f);
        effects.update(2f);
        assertEquals(0f, effects.fade());
    }

    @Test
    @DisplayName("a compiled script block runs its IR (self switch, erase event)")
    void scriptBlockRunsIr() {
        scriptIr.put("map2/event5/page1/cmd3", new com.badlogic.gdx.utils.JsonReader().parse(
                "{\"command\":\"SET_SELF_SWITCH\",\"eventId\":5,\"channel\":\"B\",\"value\":true}"));
        EventCommand selfSwitch = cmd(0, 355, 0, array("pbSetSelfSwitch(5,\"B\",true)"));
        selfSwitch.scriptBlockId = "map2/event5/page1/cmd3";
        EventCommand erase = cmd(1, 655, 0, null);
        erase.scriptBlockId = "map2/event5/page1/cmd4";
        scriptIr.put("map2/event5/page1/cmd4", new com.badlogic.gdx.utils.JsonReader().parse("{\"command\":\"ERASE_EVENT\"}"));
        interpreter.start(program(selfSwitch, erase), 2, 5);
        interpreter.update(0f);

        assertTrue(state.selfSwitches().get(2, 5, "B"));
        assertEquals(List.of("5"), erased);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("script blocks without IR and IR without a service are reported")
    void scriptBlockReportingCases() {
        EventCommand unknown = cmd(0, 355, 0, array("pbGenPkmn(:X,5)"));
        unknown.scriptBlockId = "map2/event5/page1/cmd9";
        EventCommand noIr = cmd(1, 355, 0, array("somethingElse()"));
        noIr.scriptBlockId = "map2/event5/page1/cmd10";
        scriptIr.put("map2/event5/page1/cmd9", new com.badlogic.gdx.utils.JsonReader().parse(
                "{\"command\":\"GIVE_ITEM\",\"item\":\"ORANBERRY\",\"amount\":1}"));
        interpreter.start(program(unknown, noIr), 2, 5);
        interpreter.update(0f);

        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("GIVE_ITEM")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("has no IR yet")));
    }

    @Test
    @DisplayName("Flash and Shake start their effects and can wait for them")
    void flashAndShake() {
        interpreter.start(program(cmd(0, 224, 0, array(0, 255, 8, 1))), 1, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state());
        assertTrue(effects.flashing());
        interpreter.update(0.5f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());

        interpreter.start(program(cmd(0, 225, 0, array(6, 5, 20, 0))), 1, 5);
        interpreter.update(0f);
        effects.update(0.05f);
        assertNotEquals(0f, effects.shakeX() + effects.shakeY());
    }

    @Test
    @DisplayName("Transfer Player asks the map port and ends the event")
    void transferPlayer() {
        interpreter.start(program(
                cmd(0, 201, 0, array(0, 3, 5, 7, 2, 0)),
                cmd(1, 121, 0, array(1, 1, 0))), 1, 5);
        interpreter.update(0f);
        assertEquals(List.of("3,5,7,2"), transfers);
        assertEquals(InterpreterState.WAIT_TRANSFER, interpreter.state());
        assertFalse(state.switches().get(1)); // RMXP ends the event at the transfer

        interpreter.stop();
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("Transfer Player keeps the facing when direction is zero")
    void transferKeepsDirection() {
        interpreter.start(program(cmd(0, 201, 0, array(0, 4, 1, 2, 0, 0))), 1, 5);
        interpreter.update(0f);
        assertEquals(List.of("4,1,2,0"), transfers);
    }

    @Test
    @DisplayName("a variable-appointed transfer reads map, x and y from variables")
    void transferThroughVariables() {
        state.variables().set(10, 5);
        state.variables().set(11, 6);
        state.variables().set(12, 7);
        interpreter.start(program(cmd(0, 201, 0, array(1, 10, 11, 12, 8, 0))), 1, 5);
        interpreter.update(0f);
        assertEquals(List.of("5,6,7,8"), transfers);
    }

    @Test
    @DisplayName("an invalid transfer target is reported instead of crashing")
    void invalidTransferIsReported() {
        interpreter.start(program(
                cmd(0, 201, 0, array(0, 0, 1, 1, 0, 0)),
                cmd(1, 121, 0, array(1, 1, 0))), 1, 5);
        interpreter.update(0f);
        assertTrue(transfers.isEmpty());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("Transfer Player ignored")));
        assertTrue(state.switches().get(1)); // the event continues instead of dying
    }

    @Test
    @DisplayName("Show Picture and Erase Picture drive the picture service")
    void showAndErasePicture() {
        interpreter.start(program(
                cmd(0, 231, 0, array(1, "【立绘】南晓", 0, 0, 0, 100, 100, 255, 0)),
                cmd(1, 101, 0, array("你好")),
                cmd(2, 235, 0, array(1))), 1, 5);
        interpreter.update(0f);
        assertEquals(1, pictures.size());
        assertEquals("【立绘】南晓", pictures.get(1).name);
        assertTrue(messages.visible());

        press(GameAction.CONFIRM);
        assertTrue(pictures.isEmpty());
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("Move Picture with the wait flag blocks the event until it lands")
    void movePictureWithWait() {
        pictures.show(4, "pic", 0, 0, 0, 100, 100, 255, 0);
        interpreter.start(program(
                cmd(0, 232, 0, array(4, 10, 0, 50, 60, 0, 100, 100, 255, 0, 1)),
                cmd(1, 121, 0, array(1, 1, 0))), 1, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state());
        assertFalse(state.switches().get(1));

        interpreter.update(0.6f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(state.switches().get(1));
        pictures.update(0.6f); // the frame loop drives the picture animation (MapScreen)
        assertEquals(50f, pictures.get(4).x);
    }

    @Test
    @DisplayName("long lines wrap at the message window width (R6.31: half-width units)")
    void longLinesWrap() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            longText.append('字');
        }
        interpreter.start(program(cmd(0, 101, 0, array(longText.toString()))), 1, 5);
        interpreter.update(0f);
        // 54 half-width units fit; a CJK glyph counts two, so 27 per line.
        assertEquals(2, messages.lines().size);
        assertEquals(27, messages.lines().first().length());
        assertEquals(27, messages.lines().get(1).length());
        press(GameAction.CONFIRM);
        assertEquals(1, messages.lines().size);
        assertEquals(6, messages.lines().first().length());
        press(GameAction.CONFIRM);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("\\v[n] substitutes the live variable value")
    void variableSubstitution() {
        state.variables().set(1, 42);
        interpreter.start(program(cmd(0, 101, 0, array("数量：\\v[1] 个"))), 1, 5);
        interpreter.update(0f);
        assertEquals("数量：42 个", messages.lines().first());
    }

    @Test
    @DisplayName("Exit Event Processing ends the running list")
    void exitEventProcessing() {
        interpreter.start(program(
                cmd(0, 115, 0, null),
                cmd(1, 121, 0, array(1, 1, 0))), 1, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertFalse(state.switches().get(1));
    }

    @Test
    @DisplayName("P2: an empty numeric command parameter falls back instead of crashing")
    void emptyNumericParameter() {
        // The intro's Fade Screen carries an empty duration string; asInt()
        // used to throw NumberFormatException and close the game.
        interpreter.start(program(cmd(0, 221, 0, array(""))), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);
        assertFalse(interpreter.running());
    }

    // ------------------------------------------------------------------

    private void press(GameAction action) {
        input.beginFrame();
        input.press(action);
        interpreter.update(0f);
        input.endFrame();
        input.release(action); // a tap releases the key before the next frame
    }

    @Test
    @DisplayName("Scroll Map / Change Map Settings / fog opacity reach the map (R6.22)")
    void mapEnvironmentCommands() {
        interpreter.start(program(
                cmd(0, 203, 0, array(8, 4, 4)),
                cmd(1, 204, 0, array(1, "clouds2", 0, 30, 0, 100, 2, 2)),
                cmd(2, 204, 0, array(0, "CloudySky2", 0)),
                cmd(3, 206, 0, array(0, 10))), 1, 2);
        interpreter.update(0f);

        assertEquals(4, mapEnvironment.size(), () -> "got " + mapEnvironment);
        assertEquals("scroll:8,4,4", mapEnvironment.get(0));
        assertTrue(mapEnvironment.get(1).contains("clouds2"), mapEnvironment.get(1));
        assertTrue(mapEnvironment.get(2).startsWith("settings:0:"), mapEnvironment.get(2));
        assertEquals("fogOpacity:10", mapEnvironment.get(3));
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    private static Array<EventCommand> program(EventCommand... commands) {
        Array<EventCommand> list = new Array<>();
        for (EventCommand command : commands) {
            list.add(command);
        }
        return list;
    }

    private static EventCommand cmd(int index, int code, int indent, JsonValue parameters) {
        EventCommand command = new EventCommand();
        command.index = index;
        command.code = code;
        command.indent = indent;
        command.parameters = parameters;
        return command;
    }

    private static JsonValue array(Object... values) {
        JsonValue array = new JsonValue(JsonValue.ValueType.array);
        for (Object value : values) {
            if (value instanceof JsonValue) {
                array.addChild((JsonValue) value);
            } else if (value instanceof Integer) {
                array.addChild(new JsonValue(((Integer) value).longValue()));
            } else {
                array.addChild(new JsonValue(String.valueOf(value)));
            }
        }
        return array;
    }
}
