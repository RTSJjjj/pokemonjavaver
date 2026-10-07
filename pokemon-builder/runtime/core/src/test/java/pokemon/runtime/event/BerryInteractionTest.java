package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.field.BerryPlants;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.TestData;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.GameState;
import pokemon.runtime.state.Inventory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * P3: the berry plant interaction ({@code pbBerryPlant} / {@code pbPickBerry},
 * PField_BerryPlants:313-590) through the interpreter's BERRY_PLANT /
 * BERRY_PICK IR commands: planting menu, mulch, watering and harvesting.
 */
class BerryInteractionTest {

    private GameState state;
    private MessageService messages;
    private InputManager input;
    private MenuService menus;
    private Inventory inventory;
    private PbsData pbs;
    private ScriptIr scriptIr;
    private int[] eventVariable;
    private final List<String> turns = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private EventInterpreter interpreter;

    @BeforeEach
    void setUp() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        pbs = PbsData.parse(dataRoot);
        assumeTrue(pbs.item("ORANBERRY") != null, "generated items.json not available");

        state = new GameState();
        state.enterMap(2, 0, 0);
        messages = new MessageService();
        input = new InputManager();
        menus = new MenuService();
        inventory = state.inventory();
        turns.clear();
        eventVariable = null;
        MapPort port = new MapPort() {
            @Override
            public void transfer(int mapId, int x, int y, int direction) {
            }

            @Override
            public int[] getEventVariable(int eventId) {
                return eventVariable;
            }

            @Override
            public void setEventVariable(int eventId, int[] value) {
                eventVariable = value == null ? null : value.clone();
            }

            @Override
            public void turnEvent(int eventId, int direction) {
                turns.add(eventId + ":" + direction);
            }
        };
        scriptIr = ScriptIr.empty();
        interpreter = new EventInterpreter(state, messages, input, null, id -> null, port,
                new PictureService(), warnings::add);
        interpreter.attachScriptIr(scriptIr);
        interpreter.attachInventory(inventory);
        interpreter.attachPbs(pbs);
        interpreter.attachMenuService(menus);
    }

    private void run(String irJson) {
        scriptIr.put("blk", new JsonReader().parse(irJson));
        interpreter.start(program(block(0, "blk")), 2, 5);
        interpreter.update(0f);
    }

    private void press(GameAction action) {
        input.beginFrame();
        input.press(action);
        interpreter.update(0f);
        input.endFrame();
        input.release(action);
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

    @Test
    @DisplayName("the empty plot offers 施肥/种植/返回 and plants the chosen berry (344-404)")
    void emptyPlotPlantsTheChosenBerry() {
        inventory.add("ORANBERRY", 2);
        run("{\"command\":\"BERRY_PLANT\"}");

        assertEquals(InterpreterState.WAIT_BERRY, interpreter.state());
        assertEquals("泥土看起来相当的松软。", messages.lines().first());
        assertEquals(3, messages.choices().size);
        assertEquals("种植", messages.choices().get(1));

        press(GameAction.DOWN);      // 种植
        press(GameAction.CONFIRM);

        MenuService.Request request = menus.pending();
        assertNotNull(request, "pbChooseItemScreen must open the filtered bag");
        assertEquals(MenuService.Kind.CHOOSE_ITEM, request.kind);
        assertTrue(request.filter.test("ORANBERRY"), "the berry filter accepts berries");
        assertFalse(request.filter.test("POTION"), "the berry filter rejects other items");

        int oran = pbs.item("ORANBERRY").id;
        request.complete(oran, "ORANBERRY");
        interpreter.update(0f);

        assertEquals("橙橙果种在了土里。", messages.lines().first());
        assertEquals(1, inventory.count("ORANBERRY"), "one berry is planted from the bag");
        assertTrue(turns.isEmpty(), "an empty plot does not turn (line 325)");

        press(GameAction.CONFIRM);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertNotNull(eventVariable, "the plant is written after its message (line 403)");
        assertEquals(1, eventVariable[0]);
        assertEquals(oran, eventVariable[1]);
        assertEquals(100, eventVariable[4]);
    }

    @Test
    @DisplayName("the mulch stays in the bag, is recorded and then the berry is planted (348-384)")
    void mulchPathRecordsTheMulch() {
        inventory.add("GROWTHMULCH", 1);
        inventory.add("ORANBERRY", 1);
        run("{\"command\":\"BERRY_PLANT\"}");

        press(GameAction.CONFIRM);   // 施肥
        MenuService.Request mulch = menus.pending();
        assertNotNull(mulch);
        assertTrue(mulch.filter.test("GROWTHMULCH"), "pbIsMulch? accepts the four mulches");
        assertFalse(mulch.filter.test("ORANBERRY"), "mulch and berries are different types");

        int mulchId = pbs.item("GROWTHMULCH").id;
        mulch.complete(mulchId, "GROWTHMULCH");
        interpreter.update(0f);
        assertTrue(messages.lines().first().contains("scattered on the soil"),
                messages.lines().first());
        assertEquals(1, inventory.count("GROWTHMULCH"),
                "the plugin never deletes the mulch item (lines 355-379)");

        press(GameAction.CONFIRM);   // closes the message, opens 想要种植树果吗？
        assertTrue(messages.choiceMode(), messages.lines().first());
        press(GameAction.CONFIRM);   // 是
        MenuService.Request berry = menus.pending();
        assertNotNull(berry, "the confirm asks for a berry next");
        berry.complete(pbs.item("ORANBERRY").id, "ORANBERRY");
        interpreter.update(0f);

        assertEquals("橙橙果种在了土里。", messages.lines().first());
        press(GameAction.CONFIRM);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertEquals(mulchId, eventVariable[7], "the mulch is saved with the plant");
    }

    @Test
    @DisplayName("a growing plant is watered with the first carried tool (457-552)")
    void wateringUsesTheFirstCarriedTool() {
        inventory.add("SPRAYDUCK", 1);
        int oran = pbs.item("ORANBERRY").id;
        eventVariable = new int[] { 2, oran, 0, (int) BerryPlants.now(), 50, 0, 0, 0 };
        run("{\"command\":\"BERRY_PLANT\"}");

        assertEquals("橙橙果已经发芽了。", messages.lines().first());
        assertEquals(List.of("5:2"), turns, "the sprouted plot faces down");
        press(GameAction.CONFIRM);

        assertTrue(messages.choiceMode(), "the watering question follows the stage message");
        assertEquals("想用可达鸭喷壶浇水吗？", messages.lines().first());
        press(GameAction.CONFIRM); // 是

        assertEquals("训练家浇了水。", messages.lines().first());
        assertEquals(100, eventVariable[4], "Gen 4 watering fills the dampness to 100");
        press(GameAction.CONFIRM);
        assertEquals("它看起来很开心！", messages.lines().first());
        press(GameAction.CONFIRM);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("a ripe plot yields max - penalty, goes into the bag and resets (480-525)")
    void harvestStoresTheYield() {
        int oran = pbs.item("ORANBERRY").id;
        eventVariable = new int[] { 5, oran, 0, (int) BerryPlants.now(), 100, 0, 0, 0 };
        run("{\"command\":\"BERRY_PLANT\"}");

        assertEquals(List.of("5:8"), turns, "a ripe plot faces up");
        assertTrue(messages.lines().first().contains("这里有5个"), messages.lines().first());
        assertTrue(messages.lines().first().contains("橙橙果"), messages.lines().first());
        press(GameAction.CONFIRM); // 是

        assertTrue(messages.lines().first().startsWith("摘下了5个"), messages.lines().first());
        press(GameAction.CONFIRM);
        assertTrue(messages.lines().first().contains("放进了"), messages.lines().first());
        assertTrue(messages.lines().first().contains("树果"), messages.lines().first());
        press(GameAction.CONFIRM);
        assertEquals("土壤又变得松软。", messages.lines().first());
        press(GameAction.CONFIRM);

        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertEquals(5, inventory.count("ORANBERRY"));
        assertArrayEquals(new int[8], eventVariable, "the plot is empty again");
    }

    @Test
    @DisplayName("pbPickBerry stores the preset berries and sets self switch A (555-590)")
    void pickBerryStoresAndSetsSelfSwitch() {
        run("{\"command\":\"BERRY_PICK\",\"berry\":\"CHERIBERRY\",\"qty\":2}");

        assertTrue(messages.lines().first().contains("这里有2个"), messages.lines().first());
        press(GameAction.CONFIRM); // 是
        assertTrue(messages.lines().first().startsWith("摘下了2个"), messages.lines().first());
        press(GameAction.CONFIRM);
        assertTrue(messages.lines().first().contains("放进了"), messages.lines().first());
        press(GameAction.CONFIRM);
        assertEquals("土壤又变得松软。", messages.lines().first());
        press(GameAction.CONFIRM);

        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertEquals(2, inventory.count("CHERIBERRY"));
        assertArrayEquals(new int[8], eventVariable, "the variable is reset (line 582)");
        assertTrue(state.selfSwitches().get(2, 5, "A"),
                "pbSetSelfSwitch(thisEvent.id,\"A\",true) - line 588");
    }

    @Test
    @DisplayName("cancelling pbPickBerry stores nothing and leaves the switches alone")
    void pickBerryCancelled() {
        run("{\"command\":\"BERRY_PICK\",\"berry\":\"CHERIBERRY\",\"qty\":2}");
        press(GameAction.CANCEL);

        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertEquals(0, inventory.count("CHERIBERRY"));
        assertFalse(state.selfSwitches().get(2, 5, "A"));
    }
}
