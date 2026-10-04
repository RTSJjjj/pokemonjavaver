package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.MapCharacter;
import pokemon.runtime.map.TestData;
import pokemon.runtime.state.GameState;
import pokemon.runtime.state.Inventory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R7.2b: the second tier of IR commands - SHOW_TEXT (pbMessage) and the item
 * commands that back pbItemBall / pbReceiveItem / pbDeleteItem.
 */
class ScriptIrExecutionTest {

    private GameState state;
    private MessageService messages;
    private InputManager input;
    private ScriptIr scriptIr;
    private Inventory inventory;
    private List<String> erased;
    private List<String> warnings;
    private final List<String> caveCalls = new ArrayList<>();
    private final List<String> exclaims = new ArrayList<>();
    private final List<String> boulders = new ArrayList<>();
    private boolean platesToggled;
    private float caveSeconds = 1.1f;
    private EventInterpreter interpreter;

    @BeforeEach
    void setUp() {
        state = new GameState();
        state.enterMap(2, 0, 0);
        messages = new MessageService();
        input = new InputManager();
        scriptIr = ScriptIr.empty();
        inventory = new Inventory();
        erased = new ArrayList<>();
        warnings = new ArrayList<>();
        exclaims.clear();
        boulders.clear();
        platesToggled = false;
        MapPort port = new MapPort() {
            @Override
            public void transfer(int mapId, int x, int y, int direction) {
            }

            @Override
            public void eraseEvent(int eventId) {
                erased.add(String.valueOf(eventId));
            }

            @Override
            public void setEventLocation(int eventId, int x, int y, int direction) {
                warnings.add("loc:" + eventId + "," + x + "," + y + "," + direction);
            }

            @Override
            public void setTransparent(int eventId, boolean transparent) {
                warnings.add("transparent:" + eventId + "=" + transparent);
            }

            @Override
            public float caveEntrance(boolean exiting) {
                caveCalls.add("cave:" + exiting);
                return caveSeconds;
            }

            @Override
            public void showAnimation(int characterId, int animationId) {
                warnings.add("animation:" + characterId + "," + animationId);
            }

            @Override
            public float exclaim(int characterId, int animationId) {
                exclaims.add(characterId + "," + animationId);
                return 0.45f;
            }

            @Override
            public float pushBoulder(int eventId) {
                boulders.add(String.valueOf(eventId));
                return 0.25f;
            }

            @Override
            public void togglePlateSwitches() {
                platesToggled = true;
            }
        };
        interpreter = new EventInterpreter(state, messages, input, null, id -> null, port,
                new PictureService(), warnings::add);
        interpreter.attachScriptIr(scriptIr);
        interpreter.attachInventory(inventory);
    }

    private static Array<EventCommand> program(EventCommand... commands) {
        Array<EventCommand> list = new Array<>();
        for (EventCommand command : commands) {
            list.add(command);
        }
        return list;
    }

    private static EventCommand block(int index, String blockId) {
        EventCommand command = new EventCommand();
        command.index = index;
        command.code = 355;
        command.scriptBlockId = blockId;
        return command;
    }

    private void ir(String id, String json) {
        scriptIr.put(id, new JsonReader().parse(json));
    }

    @Test
    @DisplayName("SHOW_TEXT opens a message page, keeps the speaker and waits")
    void showText() {
        ir("map2/event5/page1/cmd30", "{\"command\":\"SHOW_TEXT\",\"text\":\"\\\\xn[南晓]拿到了树果！\"}");
        EventCommand after = new EventCommand();
        after.index = 1;
        after.code = 121;
        after.parameters = new JsonReader().parse("[1,1,0]");
        interpreter.start(program(block(0, "map2/event5/page1/cmd30"), after), 2, 5);
        interpreter.update(0f);

        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
        assertEquals("南晓", messages.speaker());
        assertEquals("拿到了树果！", messages.lines().first());

        input.beginFrame();
        input.press(GameAction.CONFIRM);
        interpreter.update(0f);
        input.endFrame();
        assertTrue(state.switches().get(1), "the event continues after the message");
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("GIVE_ITEM adds and REMOVE_ITEM subtracts without going below zero")
    void itemCommands() {
        ir("map2/event5/page1/cmd20", "{\"command\":\"GIVE_ITEM\",\"item\":\"ORANBERRY\",\"amount\":3}");
        ir("map2/event5/page1/cmd21", "{\"command\":\"REMOVE_ITEM\",\"item\":\"ORANBERRY\",\"amount\":5}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd20"), block(1, "map2/event5/page1/cmd21")), 2, 5);
        interpreter.update(0f);

        assertEquals(0, inventory.count("ORANBERRY"));
        assertEquals(0, inventory.size());
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("ERASE_EVENT still reaches the map port through IR")
    void eraseEvent() {
        ir("map2/event5/page1/cmd40", "{\"command\":\"ERASE_EVENT\"}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd40")), 2, 5);
        interpreter.update(0f);
        assertEquals(List.of("5"), erased);
    }

    @Test
    @DisplayName("pbSetSelfSwitch sets the named event, not the running one (R6.19)")
    void selfSwitchTargetsAnotherEvent() {
        ir("map2/event8/page2/cmd65",
                "{\"command\":\"SET_SELF_SWITCH\",\"eventId\":10,\"channel\":\"A\",\"value\":true}");
        interpreter.start(program(block(0, "map2/event8/page2/cmd65")), 2, 8);
        interpreter.update(0f);

        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(state.selfSwitches().get(2, 10, "A"), "the target event 10 must be set");
        assertFalse(state.selfSwitches().get(2, 8, "A"), "the running event 8 must stay untouched");
    }

    @Test
    @DisplayName("a self switch for another map follows its map id (R6.19)")
    void selfSwitchTargetsAnotherMap() {
        ir("blk", "{\"command\":\"SET_SELF_SWITCH\",\"eventId\":4,\"channel\":\"B\","
                + "\"value\":true,\"mapId\":9}");
        interpreter.start(program(block(0, "blk")), 2, 8);
        interpreter.update(0f);
        assertTrue(state.selfSwitches().get(9, 4, "B"), "map 9 event 4 channel B");
        assertFalse(state.selfSwitches().get(2, 4, "B"), "not the current map");
    }

    @Test
    @DisplayName("a malformed self switch payload never crashes the interpreter (R6.19)")
    void selfSwitchMalformedPayload() {
        ir("blk", "{\"command\":\"SET_SELF_SWITCH\",\"eventId\":10,\"channel\":\"A\","
                + "\"value\":{\"script\":\"true) \\npbSetSelfSwitch(24\"}}");
        interpreter.start(program(block(0, "blk")), 2, 8);
        interpreter.update(0f);

        assertEquals(InterpreterState.FINISHED, interpreter.state(),
                "the interpreter must keep running");
        assertTrue(state.selfSwitches().get(2, 10, "A"),
                "the boolean falls back to true instead of throwing");
    }

    @Test
    @DisplayName("a real cross-event pbSetSelfSwitch block targets the named event (R6.20)")
    void realEventBlock() throws Exception {
        JsonValue entries = realIrEntries();
        org.junit.jupiter.api.Assumptions.assumeTrue(entries != null,
                "runtime data (generated/) not available");
        // R6.20: find the block by shape instead of the old command index - the
        // author inserts commands and the index moves (cmd65 -> cmd71).
        JsonValue found = null;
        int owner = -1;
        int target = -1;
        int mapId = 2;
        String channel = "A";
        for (JsonValue entry = entries.child; entry != null; entry = entry.next) {
            JsonValue ir = entry.get("ir");
            if (ir == null || !"SET_SELF_SWITCH".equals(ir.getString("command", ""))) {
                continue;
            }
            JsonValue source = entry.get("source");
            int sourceEvent = source == null ? -1 : source.getInt("eventId", -1);
            int targetEvent = ir.getInt("eventId", -1);
            if (sourceEvent < 0 || targetEvent < 0 || targetEvent == sourceEvent) {
                continue;
            }
            found = entry;
            owner = sourceEvent;
            target = targetEvent;
            mapId = source == null ? 2 : source.getInt("mapId", 2);
            channel = ir.getString("channel", "A");
            break;
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(found != null,
                "the project has a cross-event pbSetSelfSwitch block");
        ScriptIr real = ScriptIr.load(new java.io.File(TestData.runtimeDataRoot(), "scripts/ir.json"));
        interpreter.attachScriptIr(real);
        interpreter.start(program(block(0, found.getString("id", ""))), mapId, owner);
        interpreter.update(0f);
        assertTrue(state.selfSwitches().get(mapId, target, channel), "the named event's switch");
        assertFalse(state.selfSwitches().get(mapId, owner, channel), "not the running event");
    }

    @Test
    @DisplayName("a real SEQUENCE block runs every step (R6.20)")
    void realSequenceBlock() throws Exception {
        JsonValue entries = realIrEntries();
        org.junit.jupiter.api.Assumptions.assumeTrue(entries != null,
                "runtime data (generated/) not available");
        JsonValue found = null;
        int owner = -1;
        int mapId = 2;
        List<int[]> targets = new ArrayList<>();
        List<String> channels = new ArrayList<>();
        for (JsonValue entry = entries.child; entry != null && found == null; entry = entry.next) {
            JsonValue ir = entry.get("ir");
            if (ir == null || !"SEQUENCE".equals(ir.getString("command", ""))) {
                continue;
            }
            JsonValue steps = ir.get("steps");
            if (steps == null || !steps.isArray()) {
                continue;
            }
            List<int[]> stepTargets = new ArrayList<>();
            List<String> stepChannels = new ArrayList<>();
            for (JsonValue step = steps.child; step != null; step = step.next) {
                if (!"SET_SELF_SWITCH".equals(step.getString("command", ""))
                        || !step.getBoolean("value", true)) {
                    continue;
                }
                int event = step.getInt("eventId", -1);
                if (event < 0) {
                    continue;
                }
                stepTargets.add(new int[] {event});
                stepChannels.add(step.getString("channel", "A"));
            }
            if (stepTargets.size() < 2) {
                continue;
            }
            JsonValue source = entry.get("source");
            found = entry;
            owner = source == null ? -1 : source.getInt("eventId", -1);
            mapId = source == null ? 2 : source.getInt("mapId", 2);
            targets = stepTargets;
            channels = stepChannels;
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(found != null,
                "the project has a multi-statement self switch block");
        ScriptIr real = ScriptIr.load(new java.io.File(TestData.runtimeDataRoot(), "scripts/ir.json"));
        interpreter.attachScriptIr(real);
        interpreter.start(program(block(0, found.getString("id", ""))), mapId, owner);
        interpreter.update(0f);
        for (int i = 0; i < targets.size(); i++) {
            assertTrue(state.selfSwitches().get(mapId, targets.get(i)[0], channels.get(i)),
                    "step " + i + " must have run (event " + targets.get(i)[0] + ")");
        }
    }

    /** The compiled script entries of the real project, or null when absent. */
    private static JsonValue realIrEntries() {
        java.io.File dataRoot = TestData.runtimeDataRoot();
        if (dataRoot == null) {
            return null;
        }
        java.io.File irFile = new java.io.File(dataRoot, "scripts/ir.json");
        if (!irFile.isFile()) {
            return null;
        }
        return new JsonReader().parse(new FileHandle(irFile)).get("commands");
    }

    @Test
    @DisplayName("a SEQUENCE runs every step in order (R6.19)")
    void sequenceSteps() {
        ir("blk", "{\"command\":\"SEQUENCE\",\"steps\":["
                + "{\"command\":\"SET_SELF_SWITCH\",\"eventId\":23,\"channel\":\"A\",\"value\":true},"
                + "{\"command\":\"COMPLETE_QUEST\",\"quest\":\"Quest5\"},"
                + "{\"command\":\"SET_SELF_SWITCH\",\"eventId\":24,\"channel\":\"B\",\"value\":true}]}");
        state.quests().activate("Quest5");
        interpreter.start(program(block(0, "blk")), 2, 8);
        interpreter.update(0f);

        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(state.selfSwitches().get(2, 23, "A"));
        assertTrue(state.selfSwitches().get(2, 24, "B"));
        assertEquals(pokemon.runtime.state.QuestLog.Status.COMPLETED, state.quests().status("Quest5"));
    }

    @Test
    @DisplayName("a waiting step inside a SEQUENCE resumes the rest (R6.19)")
    void sequenceResumesAfterAMessage() {
        ir("blk", "{\"command\":\"SEQUENCE\",\"steps\":["
                + "{\"command\":\"SHOW_TEXT\",\"text\":\"拿到的道具\"},"
                + "{\"command\":\"SET_SELF_SWITCH\",\"eventId\":30,\"channel\":\"A\",\"value\":true}]}");
        interpreter.start(program(block(0, "blk")), 2, 8);
        interpreter.update(0f);
        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
        assertFalse(state.selfSwitches().get(2, 30, "A"), "the rest waits for the message");

        input.beginFrame();
        input.press(GameAction.CONFIRM);
        interpreter.update(0f);
        input.endFrame();
        assertTrue(state.selfSwitches().get(2, 30, "A"), "the sequence continues");
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }


    @Test
    @DisplayName("IR commands without a service are reported, not silently skipped")
    void missingService() {
        ir("map2/event5/page1/cmd50", "{\"command\":\"OPEN_MART\",\"items\":[\"POKEBALL\"]}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd50")), 2, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("202 and 208 reach the map port (no more 'not implemented' logs)")
    void locationAndTransparency() {
        EventCommand move = new EventCommand();
        move.index = 0;
        move.code = 202;
        move.parameters = new JsonReader().parse("[0,0,14,15,6]");
        EventCommand hide = new EventCommand();
        hide.index = 1;
        hide.code = 208;
        hide.parameters = new JsonReader().parse("[0]");
        interpreter.start(program(move, hide), 2, 5);
        interpreter.update(0f);

        assertTrue(warnings.contains("loc:5,14,15,6"), () -> "port calls: " + warnings);
        // 208 changes the hero (RMXP), not the event: -1 is the player.
        assertTrue(warnings.contains("transparent:-1=true"), () -> "port calls: " + warnings);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("509 move-route payloads are not mistaken for scripts")
    void moveRouteCommandsStaySilent() {
        EventCommand moveCommand = new EventCommand();
        moveCommand.index = 1;
        moveCommand.code = 509;
        moveCommand.parameters = new JsonReader().parse(
                "{\"class\":\"RPG::MoveCommand\",\"parameters\":[2],\"code\":15}");
        interpreter.start(program(moveCommand), 2, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(warnings.isEmpty(), () -> "no script warning expected, got " + warnings);
    }

    @Test
    @DisplayName("pbWait waits frames/40 s and then resumes the block (R6.26)")
    void waitCommand() {
        ir("map2/event5/page1/cmd60", "{\"command\":\"WAIT\",\"frames\":20}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd60")), 2, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state(),
                "pbWait(20) blocks for 20/40 s");

        interpreter.update(0.4f);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state(), "still waiting at 0.4 s");
        interpreter.update(0.15f);
        assertEquals(InterpreterState.FINISHED, interpreter.state(), "resumes after 0.5 s");
    }

    @Test
    @DisplayName("a sequence keeps running after pbWait (R6.26)")
    void waitInsideSequence() {
        ir("map2/event5/page1/cmd61",
                "{\"command\":\"SEQUENCE\",\"steps\":[{\"command\":\"WAIT\",\"frames\":8},"
                        + "{\"command\":\"SET_SELF_SWITCH\",\"channel\":\"A\",\"value\":true}]}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd61")), 2, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state());
        assertFalse(state.selfSwitches().get(2, 5, "A"), "the step after the wait is pending");
        interpreter.update(0.25f);
        assertTrue(state.selfSwitches().get(2, 5, "A"), () -> "sequence resumed: " + warnings);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("pbCaveEntrance runs the band animation and the event waits for it (R6.26)")
    void caveEntranceCommand() {
        ir("map2/event5/page1/cmd62", "{\"command\":\"CAVE_ENTRANCE\",\"exiting\":false}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd62")), 2, 5);
        interpreter.update(0f);
        assertEquals(List.of("cave:false"), caveCalls);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state());

        interpreter.update(0.5f);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state(),
                "the band animation holds the event");
        interpreter.update(0.7f);
        assertEquals(InterpreterState.FINISHED, interpreter.state(),
                "the event continues when the animation is over");
    }

    @Test
    @DisplayName("PLAY_SE without an audio runtime is reported, not fatal (R6.26)")
    void playSeCommand() {
        ir("map2/event5/page1/cmd63",
                "{\"command\":\"PLAY_SE\",\"name\":\"Door enter\",\"volume\":80,\"pitch\":100}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd63")), 2, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("PLAY_SE Door enter")),
                () -> "warnings: " + warnings);
    }

    @Test
    @DisplayName("207 Show Animation targets the character and reaches the screen (R6.27)")
    void showAnimationCommand() {
        EventCommand onPlayer = new EventCommand();
        onPlayer.index = 0;
        onPlayer.code = 207;
        onPlayer.parameters = new JsonReader().parse("[-1,3]");
        EventCommand onSelf = new EventCommand();
        onSelf.index = 1;
        onSelf.code = 207;
        onSelf.parameters = new JsonReader().parse("[0,4]");
        interpreter.start(program(onPlayer, onSelf), 2, 5);
        interpreter.update(0f);

        assertTrue(warnings.contains("animation:-1,3"), () -> "port calls: " + warnings);
        assertTrue(warnings.contains("animation:5,4"),
                "character 0 means the running event: " + warnings);
        assertEquals(InterpreterState.FINISHED, interpreter.state(),
                "RMXP's command_207 continues immediately (no wait)");
    }

    @Test
    @DisplayName("EXCLAIM resolves character 0 to the event and waits for the bubble (R6.30)")
    void exclaimCommand() {
        ir("map2/event5/page1/cmd70",
                "{\"command\":\"EXCLAIM\",\"character\":0,\"animationId\":3}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd70")), 2, 5);
        interpreter.update(0f);

        assertEquals(List.of("5,3"), exclaims, "character 0 is the running event 5");
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state());
        interpreter.update(0.3f);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state(), "still animating");
        interpreter.update(0.2f);
        assertEquals(InterpreterState.FINISHED, interpreter.state(),
                "pbExclaim resumes when the bubble is disposed");
    }

    @Test
    @DisplayName("PUSH_BOULDER only moves while $PokemonMap.strengthUsed is set (R6.30)")
    void pushBoulderCommand() {
        ir("map2/event5/page1/cmd71", "{\"command\":\"PUSH_BOULDER\"}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd71")), 2, 5);
        interpreter.update(0f);
        assertTrue(boulders.isEmpty(), "without the Strength field move nothing moves");
        assertEquals(InterpreterState.FINISHED, interpreter.state());

        state.pokemonMapStrengthUsed(true);
        interpreter.start(program(block(0, "map2/event5/page1/cmd71")), 2, 5);
        interpreter.update(0f);
        assertEquals(List.of("5"), boulders, "the running event is the boulder");
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state());
        interpreter.update(0.3f);
        assertEquals(InterpreterState.FINISHED, interpreter.state(),
                "the event resumes when the push step lands");
    }

    @Test
    @DisplayName("TOGGLE_PLATE_SWITCHES reaches the map port (R6.30)")
    void togglePlateSwitchesCommand() {
        ir("map2/event5/page1/cmd72", "{\"command\":\"TOGGLE_PLATE_SWITCHES\"}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd72")), 2, 5);
        interpreter.update(0f);
        assertTrue(platesToggled);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("PLAY_CRY without an audio runtime is reported, not fatal (R6.30)")
    void playCryWithoutAudio() {
        ir("map2/event5/page1/cmd73", "{\"command\":\"PLAY_CRY\",\"species\":251}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd73")), 2, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("PLAY_CRY")),
                () -> "warnings: " + warnings);
    }
}
