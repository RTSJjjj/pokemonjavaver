package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Code 111 type 12 (Script): the parsed Ruby conditions and the ones that talk to the player (pbCut ...). */
class ScriptConditionTest {
    private final List<String> warnings = new ArrayList<>();
    private GameState state;
    private MessageService messages;
    private InputManager input;
    private EventInterpreter interpreter;

    @BeforeEach
    void setUp() {
        state = new GameState();
        state.enterMap(7, 0, 0);
        messages = new MessageService();
        input = new InputManager();
        interpreter = new EventInterpreter(state, messages, input, null, id -> null, null, new PictureService(), warnings::add);
    }

    private Array<EventCommand> conditionProgram(String script) {
        return program(
                cmd(0, 111, 0, array(12, script)),
                cmd(1, 121, 1, array(1, 1, 0)),
                cmd(2, 0, 1, null),
                cmd(3, 412, 0, null),
                cmd(4, 0, 0, null));
    }

    /** Runs {@code if <script> then switch 1 ON end} to its end and returns switch 1. */
    private boolean branch(String script) {
        interpreter.start(conditionProgram(script), 7, 3);
        interpreter.update(0f);
        return state.switches().get(1);
    }

    @Test
    @DisplayName("badge counts, the bag, the party and the game variables")
    void pureAtoms() {
        assertFalse(branch("$Trainer.numbadges>=3"));
        state.trainer().badges.addAll(java.util.Arrays.asList(0, 1, 2, 3));
        assertTrue(branch("$Trainer.numbadges>=3"));
        state.switches().set(1, false);
        assertFalse(branch("$PokemonBag.pbHasItem?(:TICKET)"));
        state.inventory().add("TICKET", 1);
        assertTrue(branch("$PokemonBag.pbHasItem?(:TICKET)"));
        state.switches().set(1, false);
        assertTrue(branch("$PokemonBag.pbCanStore?(:FRESHWATER)"));
        state.switches().set(1, false);
        state.variables().set(2, 40);
        state.fieldGlobals().coins = 30;
        assertTrue(branch("pbGet(2)>$PokemonGlobal.coins"));
        state.switches().set(1, false);
        assertTrue(branch("$Trainer.ablePokemonCount<=1 && !$game_switches[5] || false"));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    @DisplayName("rescue, not, or, arithmetic, constants and a self switch")
    void operators() {
        assertTrue(branch("(pbGetPokemon(1).isEgg? rescue false) || !false"));
        state.switches().set(1, false);
        state.selfSwitches().set(7, 3, "A", true);
        assertTrue(branch("pbGetSelfSwitch(3,\"A\") && $game_map.map_id == 7 && 2 + 3 * 2 == 8"));
        state.switches().set(1, false);
        state.fieldGlobals().coins = 99_990;
        assertTrue(branch("$PokemonGlobal.coins+10>=MAX_COINS"));
    }

    @Test
    @DisplayName("an atom the runtime does not know is reported and the condition is false")
    void unknownAtom() {
        assertFalse(branch("pbMirrorBattle"));
        assertFalse(warnings.isEmpty());
    }

    private void tap(GameAction action) {
        input.beginFrame();
        input.press(action);
        interpreter.update(0f);
        input.endFrame();
        input.release(action);
    }

    private Pokemon knowing(String moveName) {
        Pokemon pkmn = new Pokemon(null, 20, null);
        pkmn.name = "Cutter";
        PbsData.Move move = new PbsData.Move();
        move.internalName = moveName;
        move.name = moveName;
        pkmn.moves.add(new Pokemon.MoveSlot(move));
        state.trainer().party.add(pkmn);
        return pkmn;
    }

    @Test
    @DisplayName("pbCut without the badge only says the tree could be cut (179:194-199)")
    void cutNeedsTheBadge() {
        knowing("CUT");
        interpreter.start(conditionProgram("pbCut"), 7, 3);
        interpreter.update(0f);
        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
        tap(GameAction.CONFIRM);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertFalse(state.switches().get(1));
    }

    @Test
    @DisplayName("pbCut with the badge and the move asks, and answering Yes runs the branch (179:194-207)")
    void cutAnswerYes() {
        state.trainer().badges.add(0);
        knowing("CUT");
        interpreter.start(conditionProgram("pbCut"), 7, 3);
        interpreter.update(0f);
        tap(GameAction.CONFIRM);                          // the first message; the question follows
        interpreter.update(0f);                           // its page needs no confirm: the choices open
        assertTrue(messages.choiceMode(), "the confirm question is open");
        tap(GameAction.CONFIRM);                          // Yes
        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());   // "{1}使用了{2}！"
        tap(GameAction.CONFIRM);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(state.switches().get(1));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    @DisplayName("pbSurf without the badges is false and says nothing (179:707-709)")
    void surfNeedsBadges() {
        interpreter.start(conditionProgram("pbSurf"), 7, 3);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertFalse(state.switches().get(1));
        assertFalse(messages.visible());
    }

    @Test
    @DisplayName("pbSurf with the badges asks, and Yes ends any vehicle and runs the branch (179:702-720)")
    void surfAnswerYes() {
        state.trainer().badges.addAll(java.util.Arrays.asList(0, 1, 2, 3, 4, 5, 6));
        state.fieldGlobals().bicycle = true;
        interpreter.start(conditionProgram("pbSurf"), 7, 3);
        interpreter.update(0f);
        interpreter.update(0f);
        assertTrue(messages.choiceMode(), "the question is open");
        tap(GameAction.CONFIRM);                          // Yes
        tap(GameAction.CONFIRM);                          // the "used Surf" line
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(state.switches().get(1));
        assertFalse(state.fieldGlobals().bicycle, "pbCancelVehicles");
    }

    @Test
    @DisplayName("pbCut answered No leaves the tree and skips the branch")
    void cutAnswerNo() {
        state.trainer().badges.add(0);
        knowing("CUT");
        interpreter.start(conditionProgram("pbCut"), 7, 3);
        interpreter.update(0f);
        tap(GameAction.CONFIRM);
        interpreter.update(0f);
        tap(GameAction.CANCEL);                           // B = No
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertFalse(state.switches().get(1));
    }

    @Test
    @DisplayName("pbMoveTutorChoose opens the tutor's party screen as a menu request; the learner's index is the answer")
    void moveTutor() {
        MenuService menus = new MenuService();
        interpreter.attachMenuService(menus);
        interpreter.start(conditionProgram("pbMoveTutorChoose(PBMoves::RISINGVOLTAGE)"), 7, 3);
        interpreter.update(0f);
        MenuService.Request request = menus.pending();
        assertNotNull(request);
        assertEquals(MenuService.Kind.TUTOR, request.kind);
        assertEquals("RISINGVOLTAGE", request.move);
        assertTrue(request.byMachine == false);
        request.complete(0, "");                          // the first Pokemon learned it: index 0 is true in Ruby
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertTrue(state.switches().get(1));
    }

    @Test
    @DisplayName("pbMoveTutorChoose answered with no learner is false")
    void moveTutorNobody() {
        MenuService menus = new MenuService();
        interpreter.attachMenuService(menus);
        interpreter.start(conditionProgram("pbMoveTutorChoose(PBMoves::POLTERGEIST)"), 7, 3);
        interpreter.update(0f);
        menus.pending().complete(-1, "");
        interpreter.update(0f);
        assertFalse(state.switches().get(1));
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
                array.addChild(new JsonValue((long) (Integer) value));
            } else if (value instanceof String) {
                array.addChild(new JsonValue((String) value));
            } else if (value instanceof Boolean) {
                array.addChild(new JsonValue((Boolean) value));
            }
        }
        return array;
    }
}
