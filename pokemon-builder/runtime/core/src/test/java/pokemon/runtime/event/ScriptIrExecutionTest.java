package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.CommonEventData;
import pokemon.runtime.data.MapData;
import pokemon.runtime.battle.BattleResult;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
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
    @Test void menuBlocksTheRestOfAnIrSequenceUntilItsOwnRequestCompletes() {
        MenuService menus = new MenuService(); interpreter.attachMenuService(menus);
        ir("p4-menu", "{\"command\":\"SEQUENCE\",\"steps\":[{\"command\":\"CHOOSE_TRADE\",\"variable\":1,\"nameVariable\":2,\"wanted\":\"A\"},{\"command\":\"GIVE_ITEM\",\"item\":\"POTION\",\"amount\":1}]}");
        interpreter.start(program(block(0, "p4-menu")), 2, 5);
        interpreter.update(0); assertEquals(0, inventory.count("POTION")); assertEquals(-1, state.variables().get(1));
        MenuService.Request request = menus.pending(); assertNotNull(request);
        interpreter.update(1); assertEquals(0, inventory.count("POTION"));
        request.complete(3, "名字$\\"); interpreter.update(0);
        assertEquals(1, inventory.count("POTION")); assertEquals(3, state.variables().get(1));
        assertEquals("名字$\\", state.variables().text(2)); assertNull(menus.pending());
    }

    @Test void setWeatherIrWritesTheScreenWeather() {
        ir("stage1-weather", "{\"command\":\"SET_WEATHER\",\"type\":7,\"power\":3,\"duration\":0}");
        interpreter.start(program(block(0, "stage1-weather")), 2, 5);
        interpreter.update(0);
        assertEquals(7, state.weather().type());
        assertEquals(16f, state.weather().max());
    }

    @Test void ribbonLoopGivesTheRibbonToEveryPartyMemberOnce() {
        pokemon.runtime.pokemon.Pokemon mon = new pokemon.runtime.pokemon.Pokemon(null, 5, null);
        pokemon.runtime.pokemon.Pokemon egg = new pokemon.runtime.pokemon.Pokemon(null, 5, null);
        egg.egg = true;
        state.trainer().party.add(mon);
        state.trainer().party.add(egg);
        ir("stage1-ribbon", "{\"command\":\"GIVE_RIBBON_PARTY\",\"ribbon\":\"CHAMPION\"}");
        for (int i = 0; i < 2; i++) {
            interpreter.start(program(block(0, "stage1-ribbon")), 2, 5);
            interpreter.update(0);
        }
        assertEquals(1, mon.ribbons.size);
        assertTrue(mon.hasRibbon("CHAMPION"));
        assertEquals(0, egg.ribbons.size);
    }

    @Test void tempSwitchIrCanTargetAnotherEventOfTheMap() {
        ir("stage1-temp", "{\"command\":\"SET_TEMP_SWITCH\",\"channel\":\"A\",\"value\":true,\"eventId\":9}");
        interpreter.start(program(block(0, "stage1-temp")), 2, 5);
        interpreter.update(0);
        assertTrue(state.tempSwitches().get(2, 9, "A"));
        assertFalse(state.tempSwitches().get(2, 5, "A"), "the running event's own switch is untouched");
    }

    @Test void stoppingAnInterpreterCancelsItsPendingMenu() {
        MenuService menus = new MenuService(); interpreter.attachMenuService(menus);
        ir("p4-pc", "{\"command\":\"OPEN_PC\"}");
        interpreter.start(program(block(0, "p4-pc")), 2, 5); interpreter.update(0);
        assertNotNull(menus.pending()); interpreter.stop(); assertNull(menus.pending());
    }

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
    @DisplayName("R8: GIVE_KEY_ITEM stores real PBS items and skips fake icons (BW Get Key Item:174)")
    void keyItemGivesOnlyRealItems() {
        java.io.File dataRoot = TestData.runtimeDataRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(dataRoot != null,
                "runtime data (generated/) not available");
        interpreter.attachPbs(PbsData.parse(dataRoot));
        ir("blk", "{\"command\":\"SEQUENCE\",\"steps\":["
                + "{\"command\":\"GIVE_KEY_ITEM\",\"item\":\"BICYCLE\",\"amount\":1},"
                + "{\"command\":\"GIVE_KEY_ITEM\",\"item\":\"itemBadge0Key\",\"amount\":1}]}");
        interpreter.start(program(block(0, "blk")), 2, 5);
        interpreter.update(0f);
        assertNotNull(interpreter.keyItemAnimation(), "the animation plays first");
        assertEquals(0, inventory.count("BICYCLE"), "the item arrives after the animation (302:170)");
        interpreter.update(3f);                                           // the animation ends, pbReceiveItem's message follows
        assertNull(interpreter.keyItemAnimation());
        assertEquals(1, inventory.count("BICYCLE"));
        for (int i = 0; i < 6 && interpreter.state() != InterpreterState.FINISHED; i++) {
            interpreter.update(1f);                                       // \wtnp[30] closes the first message by itself
            messages.close();
        }
        interpreter.update(3f);
        assertEquals(0, inventory.count("itemBadge0Key"), "a fake icon item is never stored");
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("R8: SET_BADGE / SET_TRAINER_FLAG write the $Trainer state")
    void trainerFlagsAndBadges() {
        ir("blk", "{\"command\":\"SEQUENCE\",\"steps\":["
                + "{\"command\":\"SET_TRAINER_FLAG\",\"flag\":\"pokedex\",\"value\":true},"
                + "{\"command\":\"SET_TRAINER_FLAG\",\"flag\":\"pokepc\",\"value\":true},"
                + "{\"command\":\"SET_BADGE\",\"badge\":1,\"value\":true}]}");
        interpreter.start(program(block(0, "blk")), 2, 5);
        interpreter.update(0f);

        assertTrue(state.trainer().pokedex);
        assertTrue(state.trainer().pokepc);
        assertTrue(state.trainer().badges.contains(1));
        assertEquals(1, state.trainer().badges.size());
    }

    @Test
    @DisplayName("R8: SET_VARIABLE reads the $Trainer dex counters")
    void dexCounters() {
        state.trainer().seen.add("PIKACHU");
        state.trainer().seen.add("CATERPIE");
        state.trainer().owned.add("PIKACHU");
        ir("blk", "{\"command\":\"SEQUENCE\",\"steps\":["
                + "{\"command\":\"SET_VARIABLE\",\"id\":1,\"value\":{\"trainerStat\":\"pokedexSeen\"}},"
                + "{\"command\":\"SET_VARIABLE\",\"id\":2,\"value\":{\"trainerStat\":\"pokedexOwned\"}}]}");
        interpreter.start(program(block(0, "blk")), 2, 5);
        interpreter.update(0f);

        assertEquals(2, state.variables().get(1));
        assertEquals(1, state.variables().get(2));
    }

    @Test
    @DisplayName("R8: BOSS_REWARD stores one rank set and shows the plugin line (Boss_reward:62-71)")
    void bossReward() {
        java.io.File dataRoot = TestData.runtimeDataRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(dataRoot != null,
                "runtime data (generated/) not available");
        interpreter.attachPbs(PbsData.parse(dataRoot));
        interpreter.attachRandom(new java.util.Random(7));
        ir("blk", "{\"command\":\"BOSS_REWARD\",\"rank\":5}");
        interpreter.start(program(block(0, "blk")), 2, 5);
        interpreter.update(0f);

        assertTrue(inventory.size() > 0, "one reward set is stored");
        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
        assertEquals("击败了高级BOSS，获得了奖励道具！", messages.lines().first());
    }

    @Test
    @DisplayName("R8: BOSS_POKEMON_REWARD adds pool Pokemon and queues their lines (Boss_reward:304-346)")
    void bossPokemonReward() {
        java.io.File dataRoot = TestData.runtimeDataRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(dataRoot != null,
                "runtime data (generated/) not available");
        interpreter.attachPbs(PbsData.parse(dataRoot));
        interpreter.attachRandom(new java.util.Random(3));
        ir("blk", "{\"command\":\"BOSS_POKEMON_REWARD\"}");
        interpreter.start(program(block(0, "blk")), 2, 5);
        interpreter.update(0f);

        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
        assertTrue(messages.lines().first().startsWith("击败了BOSS！获得了"));
        int gained = state.trainer().partyCount() + state.trainer().currentStorage().count();
        assertTrue(gained >= 1 && gained <= 3, "1-3 Pokemon arrive (party or box)");
        // Confirming walks through the queued lines and then finishes the block.
        for (int i = 0; i < 4; i++) {
            input.beginFrame();
            input.press(GameAction.CONFIRM);
            interpreter.update(0f);
            input.endFrame();
            input.release(GameAction.CONFIRM);
        }
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("R8: REGISTER_PARTNER loads the partner party (PField_Field:1399-1412)")
    void registerPartner() {
        java.io.File dataRoot = TestData.runtimeDataRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(dataRoot != null,
                "runtime data (generated/) not available");
        PbsData pbs = PbsData.parse(dataRoot);
        org.junit.jupiter.api.Assumptions.assumeTrue(pbs.trainer("CYAN", "阿青", 0) != null,
                "the real partner trainer is present");
        interpreter.attachPbs(pbs);
        ir("blk", "{\"command\":\"REGISTER_PARTNER\",\"trainerType\":\"CYAN\","
                + "\"name\":\"阿青\",\"partyId\":0}");
        interpreter.start(program(block(0, "blk")), 2, 5);
        interpreter.update(0f);

        GameState.Partner partner = state.partner();
        assertNotNull(partner);
        assertEquals("阿青", partner.name);
        assertTrue(partner.party.size >= 1, "the partner party is loaded");
        // PField_Field:1406-1410 stamps the party with the partner's id / OT:
        // the full trainerID, whose low half is what the summary shows.
        assertEquals(partner.id, partner.party.first().trainerID);
        assertEquals(partner.id & 0xFFFF, partner.party.first().publicID);
        assertEquals("阿青", partner.party.first().originalTrainer);
        assertEquals(partner.party.first().maxHp(), partner.party.first().hp);

        ir("blk2", "{\"command\":\"DEREGISTER_PARTNER\"}");
        interpreter.start(program(block(0, "blk2")), 2, 5);
        interpreter.update(0f);
        assertNull(state.partner());
    }

    @Test
    @DisplayName("R8: GIVE_POKEMON adds the species and shows the line (PSystem_PokemonUtilities:67-83)")
    void givePokemon() {
        java.io.File dataRoot = TestData.runtimeDataRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(dataRoot != null,
                "runtime data (generated/) not available");
        interpreter.attachPbs(PbsData.parse(dataRoot));
        ir("blk", "{\"command\":\"GIVE_POKEMON\",\"species\":\"TURTWIG\",\"level\":5}");
        interpreter.start(program(block(0, "blk")), 2, 5);
        interpreter.update(0f);

        assertEquals(1, state.trainer().partyCount());
        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
        assertTrue(messages.lines().first().contains("得到了"));
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
    @DisplayName("activateQuest / advanceQuestToStage show the plugin's message and wait (282_002_Quest_Main:63, :150)")
    void questMessages() {
        ir("blk", "{\"command\":\"SEQUENCE\",\"steps\":["
                + "{\"command\":\"ACTIVATE_QUEST\",\"quest\":\"Quest7\"},"
                + "{\"command\":\"ADVANCE_QUEST_TO_STAGE\",\"quest\":\"Quest7\",\"stage\":2}]}");
        interpreter.start(program(block(0, "blk")), 2, 8);
        interpreter.update(0f);
        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
        assertTrue(messages.lines().first().contains("接受到了新的任务！"));
        assertTrue(state.quests().isActive("Quest7"));
        for (int i = 0; i < 4 && interpreter.state() == InterpreterState.WAIT_MESSAGE; i++) {
            input.beginFrame();
            input.press(GameAction.CONFIRM);
            interpreter.update(0f);
            input.endFrame();
            interpreter.update(0f);
        }
        assertEquals(2, state.quests().stage("Quest7"));
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

        // completeQuest's pbMessage (282_002_Quest_Main:129) blocks the sequence until it is dismissed.
        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
        assertTrue(messages.lines().first().contains("任务完成！"));
        assertTrue(state.selfSwitches().get(2, 23, "A"));
        assertFalse(state.selfSwitches().get(2, 24, "B"));
        input.beginFrame();
        input.press(GameAction.CONFIRM);
        interpreter.update(0f);
        input.endFrame();
        interpreter.update(0f);

        assertEquals(InterpreterState.FINISHED, interpreter.state());
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
    @DisplayName("REPEAT runs the ball-shake loop and resumes after each pbWait (P0d)")
    void repeatLoopCommand() {
        ir("map2/event5/page1/cmd74", "{\"command\":\"SEQUENCE\",\"steps\":["
                + "{\"command\":\"REPEAT\",\"local\":\"i\",\"from\":1,\"to\":3,\"steps\":["
                + "{\"command\":\"SET_VARIABLE\",\"id\":6,\"value\":{\"local\":\"i\"}},"
                + "{\"command\":\"WAIT\",\"frames\":16}]}]}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd74")), 2, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state());
        assertEquals(1, state.variables().get(6), "the first shake sets variable 6");

        interpreter.update(0.5f);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state());
        assertEquals(2, state.variables().get(6), "the loop resumes after the wait");

        interpreter.update(0.5f);
        assertEquals(InterpreterState.WAIT_TIME, interpreter.state());
        assertEquals(3, state.variables().get(6));

        interpreter.update(0.5f);
        assertEquals(InterpreterState.FINISHED, interpreter.state(), "three shakes then done");
        assertEquals(3, state.variables().get(6), "the loop leaves the last value");
    }

    @Test
    @DisplayName("REPEAT with '...' excludes the last value (P0d)")
    void repeatExclusiveRange() {
        ir("map2/event5/page1/cmd75", "{\"command\":\"REPEAT\",\"local\":\"i\",\"from\":0,"
                + "\"to\":2,\"exclusive\":true,"
                + "\"steps\":[{\"command\":\"SET_VARIABLE\",\"id\":6,\"value\":{\"local\":\"i\"}}]}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd75")), 2, 5);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertEquals(1, state.variables().get(6), "0...2 runs 0 and 1");
    }

    @Test
    @DisplayName("stopping the interpreter cancels a running REPEAT loop (P0d)")
    void stopCancelsRepeatLoop() {
        ir("map2/event5/page1/cmd76", "{\"command\":\"REPEAT\",\"local\":\"i\",\"from\":1,\"to\":5,"
                + "\"steps\":[{\"command\":\"SET_VARIABLE\",\"id\":6,\"value\":{\"local\":\"i\"}},"
                + "{\"command\":\"WAIT\",\"frames\":16}]}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd76")), 2, 5);
        interpreter.update(0f);
        assertEquals(1, state.variables().get(6), "the loop started");

        interpreter.stop();
        interpreter.update(1f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertEquals(1, state.variables().get(6), "the cancelled loop must not resume");
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

    @Test
    @DisplayName("116 Erase Event reaches the map port for the running event")
    void eraseEventCommand() {
        interpreter.start(program(command(0, 116, "[]")), 2, 5);
        interpreter.update(0f);
        assertEquals(List.of("5"), erased);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("117 Call Common Event runs the common event before the caller continues")
    void callCommonEventCommand() {
        Array<EventCommand> commonEvent = program(command(0, 121, "[1,1,0]"));
        EventInterpreter caller = new EventInterpreter(state, messages, input, null,
                id -> id == 20 ? commonEvent : null, new MapPort() {
                    @Override
                    public void transfer(int mapId, int x, int y, int direction) {
                    }
                }, new PictureService(), warnings::add);
        caller.start(program(command(0, 117, "[20]"), command(1, 123, "[\"A\",0]")), 2, 5);
        caller.update(0f);

        assertTrue(state.switches().get(1), "the common event ran first");
        assertTrue(state.selfSwitches().get(2, 5, "A"), "the caller resumed afterwards");
        assertEquals(InterpreterState.FINISHED, caller.state());
    }

    @Test
    @DisplayName("map005/EV006 -> common event 20 -> pbShowMap (117 regression)")
    void realCommonEventCallChain() throws Exception {
        java.io.File dataRoot = TestData.runtimeDataRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(dataRoot != null,
                "runtime data (generated/) not available");
        MapData mapData = MapData.parse(new JsonReader().parse(
                new FileHandle(new java.io.File(dataRoot, "maps/map-005.json"))));
        MapData.EventData event = null;
        for (MapData.EventData candidate : mapData.events) {
            if (candidate.id == 6) {
                event = candidate;
                break;
            }
        }
        assertNotNull(event, "map 5 event 6");
        CommonEventData commonEvent = CommonEventData.parse(new JsonReader().parse(
                new FileHandle(new java.io.File(dataRoot, "common-events/common-event-020.json"))));

        state.enterMap(5, 0, 0);
        state.setPlayerPosition(0, 0, 8); // the event's condition asks for facing up
        MenuService menus = new MenuService();
        EventInterpreter caller = new EventInterpreter(state, messages, input, null,
                id -> id == 20 ? commonEvent.commands : null, new MapPort() {
                    @Override
                    public void transfer(int mapId, int x, int y, int direction) {
                    }
                }, new PictureService(), warnings::add);
        caller.attachScriptIr(ScriptIr.load(new java.io.File(dataRoot, "scripts/ir.json")));
        caller.attachMenuService(menus);
        caller.start(event.pages.first().commands, 5, 6);
        caller.update(0f);
        assertEquals(InterpreterState.WAIT_MESSAGE, caller.state(),
                "common event 20's message page");

        input.beginFrame();
        input.press(GameAction.CONFIRM);
        caller.update(0f);
        input.endFrame();
        assertNotNull(menus.pending(), "pbShowMap opened the region map request");
        assertEquals(MenuService.Kind.SHOW_MAP, menus.pending().kind);
    }

    @Test
    @DisplayName("pbTrainerIntro / setBattleRule / pbTrainerEnd lock, record and unlock (P2)")
    void trainerIntroAndBattleRule() {
        List<String> locks = new ArrayList<>();
        EventInterpreter call = new EventInterpreter(state, messages, input, null, id -> null,
                new MapPort() {
                    @Override
                    public void transfer(int mapId, int x, int y, int direction) {
                    }

                    @Override
                    public void lockEvents(boolean locked) {
                        locks.add(String.valueOf(locked));
                    }

                    @Override
                    public void eraseRoute(int eventId) {
                        locks.add("route:" + eventId);
                    }
                }, new PictureService(), warnings::add);
        call.attachScriptIr(scriptIr);
        ir("intro", "{\"command\":\"TRAINER_INTRO\",\"trainerType\":\"BLACKBELT\"}");
        ir("rule", "{\"command\":\"BATTLE_RULE\",\"rules\":[{\"rule\":\"double\"}]}");
        ir("end", "{\"command\":\"TRAINER_END\"}");
        call.start(program(block(0, "intro"), block(1, "rule"), block(2, "end")), 2, 5);
        call.update(0f);

        assertEquals(List.of("true", "false", "route:5"), locks);
        assertEquals("double", state.battleRules().size);
        assertEquals(InterpreterState.FINISHED, call.state());
    }

    @Test
    @DisplayName("pbTrainerBattle as a script condition branches on the win (PField_Battles:526)")
    void trainerBattleCondition() {
        java.io.File dataRoot = TestData.runtimeDataRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(dataRoot != null,
                "runtime data (generated/) not available");
        PbsData pbs = PbsData.parse(dataRoot);
        org.junit.jupiter.api.Assumptions.assumeTrue(pbs.trainer("YOUNGSTER", "阿伟", 0) != null,
                "the real trainer data is present");
        final BattleResult won = new BattleResult(BattleResult.Outcome.WIN, 1, null);
        pokemon.runtime.battle.BattlePort battle = new pokemon.runtime.battle.BattlePort() {
            @Override
            public BattleResult wildBattle(String species, int level) { return won; }
            @Override
            public BattleResult freeWildBattle(Pokemon foe) { return won; }
            @Override
            public BattleResult trainerBattle(PbsData.TrainerData trainer) { return won; }
            @Override
            public BattleResult lastResult() { return won; }
        };
        EventInterpreter call = new EventInterpreter(state, messages, input, null, id -> null,
                new MapPort() {
                    @Override
                    public void transfer(int mapId, int x, int y, int direction) {
                    }
                }, new PictureService(), warnings::add);
        call.attachPbs(pbs);
        call.attachBattlePort(battle);

        EventCommand condition = command(0, 111,
                "[12,\"pbTrainerBattle(:YOUNGSTER,\\\"阿伟\\\")\"]");
        EventCommand selfSwitch = command(1, 123, "[\"A\",0]");
        call.start(program(condition, selfSwitch), 10, 7);
        call.update(0f);

        assertTrue(state.selfSwitches().get(10, 7, "A"), "the win branch sets self switch A");
        assertEquals(1, state.variables().get(1), "the decision lands in variable 1");
        assertEquals(InterpreterState.FINISHED, call.state());
    }

    @Test
    @DisplayName("pbWildBattle as a script condition branches on the decision (PField_Battles:367)")
    void wildBattleCondition() {
        java.io.File dataRoot = TestData.runtimeDataRoot();
        org.junit.jupiter.api.Assumptions.assumeTrue(dataRoot != null,
                "runtime data (generated/) not available");
        PbsData pbs = PbsData.parse(dataRoot);
        org.junit.jupiter.api.Assumptions.assumeTrue(pbs.species("CATERPIE") != null,
                "the real species data is present");
        final BattleResult won = new BattleResult(BattleResult.Outcome.WIN, 1, null);
        pokemon.runtime.battle.BattlePort battle = new pokemon.runtime.battle.BattlePort() {
            @Override
            public BattleResult wildBattle(String species, int level) { return won; }
            @Override
            public BattleResult freeWildBattle(Pokemon foe) { return won; }
            @Override
            public BattleResult trainerBattle(PbsData.TrainerData trainer) { return won; }
            @Override
            public BattleResult lastResult() { return won; }
        };
        EventInterpreter call = new EventInterpreter(state, messages, input, null, id -> null,
                new MapPort() {
                    @Override
                    public void transfer(int mapId, int x, int y, int direction) {
                    }
                }, new PictureService(), warnings::add);
        call.attachPbs(pbs);
        call.attachBattlePort(battle);

        // pbWildBattle returns (decision!=2 && decision!=5), so a win takes the
        // branch; the decision itself lands in variable 1 (PField_Battles:343).
        EventCommand condition = command(0, 111, "[12,\"pbWildBattle(:CATERPIE,5)\"]");
        EventCommand selfSwitch = command(1, 123, "[\"A\",0]");
        call.start(program(condition, selfSwitch), 10, 7);
        call.update(0f);

        assertTrue(state.selfSwitches().get(10, 7, "A"), "the win branch sets self switch A");
        assertEquals(1, state.variables().get(1), "the decision lands in variable 1");
        assertEquals(InterpreterState.FINISHED, call.state());
    }

    @Test
    @DisplayName("pbBridgeOn sets $PokemonGlobal.bridge to the Ruby default 2, pbBridgeOff to 0")
    void bridgeCommands() {
        ir("map2/event5/page1/cmd80", "{\"command\":\"SET_BRIDGE\",\"on\":true}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd80")), 2, 5);
        interpreter.update(0f);
        assertEquals(EventInterpreter.DEFAULT_BRIDGE_HEIGHT, state.bridge(),
                "PField_Field:1363 - def pbBridgeOn(height=2)");
        assertEquals(InterpreterState.FINISHED, interpreter.state());

        ir("map2/event5/page1/cmd81", "{\"command\":\"SET_BRIDGE\",\"on\":false}");
        interpreter.start(program(block(0, "map2/event5/page1/cmd81")), 2, 5);
        interpreter.update(0f);
        assertEquals(0, state.bridge(), "pbBridgeOff stores 0");
    }

    @Test
    @DisplayName("Control Variable random operand rolls from + rand(to-from+1) (Interpreter:840)")
    void randomVariableOperand() {
        // The float_plate pages use [26,26,0,2,1,3]: variable 26 = 1..3, which
        // picks one of the three SE pitches. Before this the operand type was
        // reported as unsupported and no branch ever matched.
        final long seed = 20261231L;
        interpreter.attachRandom(new java.util.Random(seed));
        java.util.Random reference = new java.util.Random(seed);
        EventCommand roll = command(0, 122, "[26,26,0,2,1,3]");

        boolean sawAllThree = true;
        boolean[] seen = new boolean[4];
        for (int i = 0; i < 40; i++) {
            interpreter.start(program(roll), 2, 5);
            interpreter.update(0f);
            int expected = 1 + reference.nextInt(3);
            assertEquals(expected, state.variables().get(26),
                    "roll " + i + " must match 1 + rand(3)");
            assertTrue(expected >= 1 && expected <= 3);
            seen[expected] = true;
        }
        for (int value = 1; value <= 3; value++) {
            sawAllThree &= seen[value];
        }
        assertTrue(sawAllThree, "all three pitches occur in 40 rolls");
    }

    @Test
    @DisplayName("Control Variable remainder by 1 leaves the variable alone (Interpreter:883-885)")
    void remainderByOneKeepsTheValue() {
        state.variables().set(5, 7);
        interpreter.start(program(command(0, 122, "[5,5,5,0,1]")), 2, 5);
        interpreter.update(0f);
        assertEquals(7, state.variables().get(5),
                "`next if value == 1 || value == 0` - not 7 % 1");
    }

    @Test
    @DisplayName("Control Variable clamps to +/-99999999 (Interpreter:888-890)")
    void variableClamp() {
        state.variables().set(9, 99999999);
        interpreter.start(program(command(0, 122, "[9,9,1,0,5]")), 2, 5);
        interpreter.update(0f);
        assertEquals(99999999, state.variables().get(9), "the add at the cap is skipped");

        state.variables().set(9, -99999999);
        interpreter.start(program(command(0, 122, "[9,9,2,0,5]")), 2, 5);
        interpreter.update(0f);
        assertEquals(-99999999, state.variables().get(9), "the subtract at the cap is skipped");
    }

    private static EventCommand command(int index, int code, String parameters) {        EventCommand command = new EventCommand();
        command.index = index;
        command.code = code;
        command.parameters = parameters == null ? null : new JsonReader().parse(parameters);
        return command;
    }
}
