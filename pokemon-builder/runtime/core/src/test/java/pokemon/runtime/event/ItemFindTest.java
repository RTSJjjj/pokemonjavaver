package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.GameState;
import pokemon.runtime.state.Inventory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 309_Item_Find pbItemBall / pbReceiveItem: jingle, messages, the pocket line and the box for repeat finds. */
class ItemFindTest {
    private GameState state;
    private MessageService messages;
    private EventInterpreter interpreter;
    private Inventory inventory;
    private ScriptIr scriptIr;
    private final List<String> jingles = new ArrayList<>();

    @BeforeEach
    void setUp() {
        state = new GameState();
        state.enterMap(1, 0, 0);
        messages = new MessageService();
        scriptIr = ScriptIr.empty();
        interpreter = new EventInterpreter(state, messages, new InputManager(), null, id -> null, null, new PictureService(), w -> { });
        interpreter.attachScriptIr(scriptIr);
        inventory = new Inventory();
        interpreter.attachInventory(inventory);
        PbsData pbs = PbsData.parse(new java.io.File("__no_pbs__"));
        PbsData.Item potion = new PbsData.Item();
        potion.id = 17;
        potion.internalName = "POTION";
        potion.name = "伤药";
        potion.namePlural = "伤药";
        potion.pocket = 2;
        pbs.items.put("POTION", potion);
        interpreter.attachPbs(pbs);
    }

    private void run(String mode, int amount) {
        scriptIr.put("b", new JsonReader().parse(
                "{\"command\":\"GIVE_ITEM\",\"item\":\"POTION\",\"amount\":" + amount + ",\"mode\":\"" + mode + "\"}"));
        EventCommand command = new EventCommand();
        command.index = 0;
        command.code = 355;
        command.indent = 0;
        command.scriptBlockId = "b";
        command.parameters = new JsonValue(JsonValue.ValueType.array);
        command.parameters.addChild(new JsonValue("pbItemBall(:POTION)"));
        EventCommand end = new EventCommand();
        end.index = 1;
        end.code = 0;
        Array<EventCommand> list = new Array<>();
        list.add(command);
        list.add(end);
        interpreter.start(list, 1, 5);
        interpreter.update(0f);
    }

    private String text() {
        return String.join("|", messages.lines());
    }

    @Test
    @DisplayName("the first ground item shows the find message, then the pocket line; both close by themselves")
    void firstFind() {
        run("ball", 1);
        assertTrue(messages.visible());
        assertTrue(text().contains("你发现了1个"), text());
        assertFalse(messages.waiting(), "\\wtnp[30]: no confirm");
        interpreter.update(30 / 40f + 0.01f);                       // 30 frames at 40 fps
        assertTrue(text().contains("你将伤药放进了"), text());
        assertTrue(text().contains("口袋。"), text());
        assertEquals(1, inventory.count("POTION"));
        assertTrue(state.fieldGlobals().foundItems.contains("POTION"));
        assertTrue(interpreter.itemToasts().list().isEmpty());
    }

    @Test
    @DisplayName("a repeat find shows the box at the right edge and no message")
    void repeatFind() {
        state.fieldGlobals().foundItems.add("POTION");
        run("ball", 2);
        assertFalse(messages.visible());
        assertEquals(2, inventory.count("POTION"));
        assertEquals(1, interpreter.itemToasts().list().size());
        assertEquals(192, interpreter.itemToasts().list().get(0).y);
        assertEquals(2, interpreter.itemToasts().list().get(0).qty);
        interpreter.itemToasts().update(3.1f);          // the map screen ages the boxes every frame
        assertTrue(interpreter.itemToasts().list().isEmpty(), "gone after three seconds");
    }

    @Test
    @DisplayName("a gift always shows its messages, even when the item was found before")
    void gift() {
        state.fieldGlobals().foundItems.add("POTION");
        run("receive", 3);
        assertTrue(messages.visible());
        assertTrue(text().contains("获得了3个"), text());
        assertTrue(interpreter.itemToasts().list().isEmpty());
    }
}
