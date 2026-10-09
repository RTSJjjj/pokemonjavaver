package pokemon.runtime.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 297_Follower_Main pbStartOver: the white-out goes to the last Pokemon Center, or home. */
class StartOverTest {
    private final List<String> transfers = new ArrayList<>();
    private final GameState state = new GameState();
    private final MessageService messages = new MessageService();
    private final InputManager input = new InputManager();
    private EventInterpreter interpreter;
    private PbsData pbs;

    private void setUp() {
        state.enterMap(9, 0, 0);
        MapPort port = (mapId, x, y, direction) -> transfers.add(mapId + "," + x + "," + y + "," + direction);
        interpreter = new EventInterpreter(state, messages, input, null, id -> null, port, new PictureService(), w -> { });
        pbs = PbsData.parse(new java.io.File("__no_pbs__"));
        interpreter.attachPbs(pbs);
        PbsData.Species species = new PbsData.Species();
        species.internalName = "TEST";
        species.name = "test";
        species.baseStats = new int[] {50, 50, 50, 50, 50, 50};
        pbs.species.put("TEST", species);
        Pokemon mon = new Pokemon(species, 10, pbs);
        mon.hp = 1;
        state.trainer().party.add(mon);
    }

    private void run() {
        for (int i = 0; i < 200 && interpreter.running(); i++) {
            input.beginFrame();
            if (messages.visible() && messages.waiting()) {
                input.press(GameAction.CONFIRM);
            }
            interpreter.update(1f / 40f);
            input.endFrame();
            input.release(GameAction.CONFIRM);
        }
    }

    @Test
    @DisplayName("without a Pokemon Center the player goes home (Home metadata), says so, and the recovery switch is on")
    void home() {
        setUp();
        pbs.globalMetadata().home = new int[] {3, 17, 14, 8}; // 战败回家固定 3,17,12,8（用户指定），与 PBS 的 Home 无关
        interpreter.startOver();
        run();
        assertEquals(List.of("3,17,12,8"), transfers);
        assertTrue(state.switches().get(1));
        assertEquals(state.trainer().party.first().maxHp(), state.trainer().party.first().hp);
    }

    @Test
    @DisplayName("with a Pokemon Center the player goes there")
    void center() {
        setUp();
        state.trainer().healMapId = 5;
        state.trainer().healX = 4;
        state.trainer().healY = 6;
        state.trainer().healDirection = 2;
        interpreter.startOver();
        run();
        assertEquals(List.of("5,4,6,2"), transfers);
    }
}
