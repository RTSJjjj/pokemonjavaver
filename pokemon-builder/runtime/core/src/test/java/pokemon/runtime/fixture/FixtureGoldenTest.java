package pokemon.runtime.fixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.CommonEventData;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.SystemData;
import pokemon.runtime.event.EventInterpreter;
import pokemon.runtime.event.MapPort;
import pokemon.runtime.event.MessageService;
import pokemon.runtime.event.PictureService;
import pokemon.runtime.event.ScriptIr;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.Collision;
import pokemon.runtime.map.EventTriggers;
import pokemon.runtime.map.MapCharacter;
import pokemon.runtime.map.TestData;
import pokemon.runtime.map.TileMap;
import pokemon.runtime.state.GameState;
import pokemon.runtime.state.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * R15 golden tests (project3 section 57): the Small Test Project must run
 * through the real runtime logic headlessly and end in the documented state -
 * player position, switches, variables, transfer target and event result.
 *
 * <p>Skipped when the fixture is not next to the runtime checkout.</p>
 */
class FixtureGoldenTest {

    private static final float FRAME = 1f / 40f;

    private final List<String> transfers = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    private EventInterpreter interpreter(GameDatabase database, GameState state,
                                         MessageService messages, InputManager input,
                                         Inventory inventory, ScriptIr scriptIr) {
        transfers.clear();
        warnings.clear();
        EventInterpreter interpreter = new EventInterpreter(state, messages, input, null,
                id -> {
                    CommonEventData event = database.commonEvent(id);
                    return event == null ? null : event.commands;
                },
                new MapPort() {
                    @Override
                    public void transfer(int mapId, int x, int y, int direction) {
                        transfers.add(mapId + "," + x + "," + y + "," + direction);
                    }

                    @Override
                    public void transfer(int mapId, int x, int y, int direction, int fade) {
                        transfers.add(mapId + "," + x + "," + y + "," + direction + "," + fade);
                    }
                },
                new PictureService(), warnings::add);
        if (scriptIr != null) {
            interpreter.attachScriptIr(scriptIr);
        }
        if (inventory != null) {
            interpreter.attachInventory(inventory);
        }
        return interpreter;
    }

    private void press(EventInterpreter interpreter, InputManager input, GameAction action) {
        input.beginFrame();
        input.press(action);
        interpreter.update(0f);
        input.endFrame();
        input.release(action);
    }

    @Test
    @DisplayName("the fixture has the two documented maps and the start position (R15)")
    void mapsAndStartAreTheDocumentedOnes() {
        GameDatabase database = FixtureData.load();
        assumeTrue(database != null, "fixture data (tests/fixture-project/generated) not available");
        try {
            assertEquals(2, database.mapCount());
            SystemData system = database.system();
            assertEquals(1, system.startMapId);
            assertEquals(5, system.startX);
            assertEquals(5, system.startY);
            assertNotNull(database.map(1));
            assertNotNull(database.map(2));

            MapData map1 = database.map(1);
            MapData.EventData door = FixtureData.eventByName(map1, "Door");
            assertNotNull(door, "the fixture door exists");
            assertArrayEquals(new int[] {2, 10, 7}, TestData.transferTarget(door),
                    "the door transfers to map 2 (10, 7)");
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("NPC talk sets SW1 and VAR1 and shows the fixture text (R15 golden)")
    void npcTalkGolden() {
        GameDatabase database = FixtureData.load();
        assumeTrue(database != null, "fixture data not available");
        try {
            MapData map1 = database.map(1);
            MapData.EventData npc = FixtureData.eventByName(map1, "NPC");
            assertNotNull(npc);

            GameState state = new GameState();
            state.enterMap(1, 6, 5);
            MessageService messages = new MessageService();
            InputManager input = new InputManager();
            EventInterpreter interpreter = interpreter(database, state, messages, input, null, null);

            interpreter.start(npc.pages.first().commands, 1, npc.id);
            interpreter.update(0f);
            assertTrue(messages.visible(), "the NPC opens the message window");
            assertEquals("Hello from the fixture!", messages.lines().first());

            press(interpreter, input, GameAction.CONFIRM);
            assertFalse(interpreter.running());
            assertTrue(state.switches().get(1), "SW1 is on after the talk");
            assertEquals(5, state.variables().get(1), "VAR1 is 5 after the talk");
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("the choice event runs the selected branch (R15 golden)")
    void choiceGolden() {
        GameDatabase database = FixtureData.load();
        assumeTrue(database != null, "fixture data not available");
        try {
            MapData map1 = database.map(1);
            MapData.EventData chooser = FixtureData.eventByName(map1, "Chooser");
            assertNotNull(chooser);

            GameState state = new GameState();
            state.enterMap(1, 11, 6);
            MessageService messages = new MessageService();
            InputManager input = new InputManager();
            EventInterpreter interpreter = interpreter(database, state, messages, input, null, null);

            interpreter.start(chooser.pages.first().commands, 1, chooser.id);
            interpreter.update(0f);
            assertTrue(messages.choiceMode(), "the choice window is open");
            assertTrue(messages.choices().size >= 2);

            // Move to the second option ("No") and confirm it.
            press(interpreter, input, GameAction.DOWN);
            press(interpreter, input, GameAction.CONFIRM);
            assertFalse(interpreter.running());
            assertEquals(9, state.variables().get(1), "the second option sets VAR1 = 9");
            assertFalse(state.switches().get(2), "the first option branch did not run");
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("the door transfers to the documented target (R15 golden)")
    void doorTransferGolden() {
        GameDatabase database = FixtureData.load();
        assumeTrue(database != null, "fixture data not available");
        try {
            MapData map1 = database.map(1);
            GameState state = new GameState();
            state.enterMap(1, 5, 4);
            MessageService messages = new MessageService();
            InputManager input = new InputManager();
            EventInterpreter interpreter = interpreter(database, state, messages, input, null, null);

            MapData.EventPageData page =
                    EventTriggers.pageAt(state, map1, 5, 3, EventTriggers.PLAYER_TOUCH);
            assertNotNull(page, "the door answers the player touch trigger");
            interpreter.start(page.commands, 1, 2);
            interpreter.update(0f);

            assertEquals(1, transfers.size(), () -> "expected one transfer, got " + transfers);
            assertTrue(transfers.get(0).startsWith("2,10,7,"), transfers.get(0));
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("Call Common Event 1 runs the shared event (R15 golden)")
    void commonEventGolden() {
        GameDatabase database = FixtureData.load();
        assumeTrue(database != null, "fixture data not available");
        try {
            MapData map1 = database.map(1);
            MapData.EventData caller = FixtureData.eventByName(map1, "CallCommon");
            assertNotNull(caller);

            GameState state = new GameState();
            state.enterMap(1, 13, 6);
            MessageService messages = new MessageService();
            InputManager input = new InputManager();
            EventInterpreter interpreter = interpreter(database, state, messages, input, null, null);

            interpreter.start(caller.pages.first().commands, 1, caller.id);
            interpreter.update(0f);
            interpreter.update(FRAME);
            assertFalse(interpreter.running());
            assertEquals(7, state.variables().get(1), "the common event sets VAR1 = 7");
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("pbItemBall(:POTION, 3) gives three potions through the IR (R15 section 58)")
    void itemBallGolden() throws IOException {
        GameDatabase database = FixtureData.load();
        File root = FixtureData.root();
        assumeTrue(database != null && root != null, "fixture data not available");
        try {
            MapData map1 = database.map(1);
            MapData.EventData giver = FixtureData.eventByName(map1, "ItemGiver");
            assertNotNull(giver);

            GameState state = new GameState();
            state.enterMap(1, 3, 6);
            MessageService messages = new MessageService();
            InputManager input = new InputManager();
            Inventory inventory = new Inventory();
            ScriptIr scriptIr = ScriptIr.load(new File(root, "scripts/ir.json"));
            EventInterpreter interpreter = interpreter(database, state, messages, input, inventory, scriptIr);

            interpreter.start(giver.pages.first().commands, 1, giver.id);
            interpreter.update(0f);
            interpreter.update(FRAME);
            assertFalse(interpreter.running());
            assertEquals(3, inventory.count("POTION"), "GIVE_ITEM adds three potions");
        } finally {
            database.dispose();
        }
    }

    @Test
    @DisplayName("walking respects open floor, the fixture wall and the NPC (R15 golden)")
    void walkingGolden() {
        GameDatabase database = FixtureData.load();
        assumeTrue(database != null, "fixture data not available");
        try {
            MapData map1 = database.map(1);
            TileMap map = new TileMap(map1, database.tileset(map1.tilesetId));
            GameState state = new GameState();
            state.enterMap(1, 5, 7);

            MapCharacter player = new MapCharacter(5, 7, map1.width, map1.height, "player");
            assertTrue(Collision.canStep(state, map, map1, player, 6), "right is open at (5,7)");
            player.startMove(6, 7, 6);
            pumpCharacter(player);
            assertEquals(6, player.x());
            assertEquals(7, player.y());

            player.startMove(7, 7, 6);
            pumpCharacter(player);
            assertEquals(7, player.x());
            assertFalse(Collision.canStep(state, map, map1, player, 6),
                    "the wall column at x=8 blocks the next step");

            // The NPC at (7,5) blocks the tile behind it.
            MapCharacter walker = new MapCharacter(5, 5, map1.width, map1.height, "player");
            assertTrue(Collision.canStep(state, map, map1, walker, 6), "right is open at (5,5)");
            walker.startMove(6, 5, 6);
            pumpCharacter(walker);
            assertEquals(6, walker.x());
            assertFalse(Collision.canStep(state, map, map1, walker, 6), "the NPC blocks (7,5)");
        } finally {
            database.dispose();
        }
    }

    private static void pumpCharacter(MapCharacter character) {
        int frames = 0;
        while (character.isMoving() && frames < 120) {
            character.update(1f / 60f);
            frames++;
        }
        assertFalse(character.isMoving(), "a single tile step finishes");
    }
}
