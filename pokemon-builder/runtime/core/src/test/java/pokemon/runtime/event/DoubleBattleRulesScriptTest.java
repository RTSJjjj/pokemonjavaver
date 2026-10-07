package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.battle.BattlePort;
import pokemon.runtime.battle.BattleResult;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PField_Battles:88-93 (the forced-double switch), :534-577 (two trainer events that spot the player at once).
 */
class DoubleBattleRulesScriptTest {

    private GameState state;
    private MessageService messages;
    private ScriptIr scriptIr;
    private PbsData pbs;
    private final List<String> calls = new ArrayList<>();
    private final BattleResult[] finished = new BattleResult[1];
    private int[] triggered = new int[0];

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        state = new GameState();
        state.enterMap(1, 0, 0);
        calls.clear();
        finished[0] = null;
        messages = new MessageService();
        scriptIr = ScriptIr.empty();
        pbs = PbsData.parse(syntheticPbs(tempDir));
    }

    private EventInterpreter interpreter() {
        EventInterpreter interpreter = new EventInterpreter(state, messages, new InputManager(), null,
                id -> null, new MapPort() {
                    @Override public void transfer(int mapId, int x, int y, int direction) { }
                    @Override public int[] triggeredTrainerEvents() { return triggered; }
                }, new PictureService(), message -> { });
        interpreter.attachScriptIr(scriptIr);
        interpreter.attachPbs(pbs);
        interpreter.attachBattlePort(new BattlePort() {
            @Override public BattleResult wildBattle(String species, int level) {
                calls.add("wild:" + species);
                return finish(new BattleResult(BattleResult.Outcome.WIN, 1, null));
            }
            @Override public BattleResult freeWildBattle(Pokemon foe) { return null; }
            @Override public BattleResult trainerBattle(PbsData.TrainerData trainer) {
                calls.add("trainers:1");
                return finish(new BattleResult(BattleResult.Outcome.WIN, 1, null));
            }
            @Override public BattleResult trainerBattle(List<PbsData.TrainerData> trainers) {
                calls.add("trainers:" + trainers.size());
                return finish(new BattleResult(BattleResult.Outcome.WIN, 1, null));
            }
            @Override public BattleResult lastResult() { return finished[0]; }
        });
        return interpreter;
    }

    private BattleResult finish(BattleResult result) {
        finished[0] = result;
        return result;
    }

    private void addAblePokemon(int count) {
        for (int i = 0; i < count; i++) {
            state.trainer().addToParty(new Pokemon(pbs.species("FOE"), 5, pbs));
        }
    }

    private void run(EventInterpreter interpreter, int eventId, String steps) {
        scriptIr.put("block1", new JsonReader().parse("{\"command\":\"SEQUENCE\",\"steps\":[" + steps + "]}"));
        interpreter.start(program(script("block1")), 1, eventId);
        interpreter.update(0f);
        interpreter.update(0f);
    }

    private static final String TRAINER_STEP =
            "{\"command\":\"TRAINER_BATTLE\",\"trainerType\":\"RIVAL\",\"trainerName\":\"Blue\",\"version\":1}";

    @Test
    @DisplayName("switch 41 with too few Pokemon shows the refusal first, then the battle goes on")
    void forcedDoubleRefusal() {
        state.switches().set(41, true);
        addAblePokemon(1);
        run(interpreter(), 5, "{\"command\":\"WILD_BATTLE\",\"species\":\"FOE\",\"level\":5}");
        assertTrue(calls.isEmpty(), "the battle waits for the line to be closed: " + calls);
        assertTrue(messages.visible());
    }

    @Test
    @DisplayName("switch 41 with two able Pokemon shows no refusal")
    void forcedDoubleWithEnoughPokemon() {
        state.switches().set(41, true);
        addAblePokemon(2);
        run(interpreter(), 5, "{\"command\":\"WILD_BATTLE\",\"species\":\"FOE\",\"level\":5}");
        assertEquals(List.of("wild:FOE"), calls);
    }

    @Test
    @DisplayName("the first of two spotting trainers waits, the second fights both and marks the first one")
    void waitingTrainer() {
        addAblePokemon(2);
        triggered = new int[] { 5, 8 };
        EventInterpreter first = interpreter();
        run(first, 5, TRAINER_STEP);
        assertTrue(calls.isEmpty(), "no battle for the first trainer alone: " + calls);
        assertNotNull(state.battleRules().waitingTrainer);

        EventInterpreter second = interpreter();
        run(second, 8, TRAINER_STEP);
        assertEquals(List.of("trainers:2"), calls);
        assertNull(state.battleRules().waitingTrainer);
        second.update(0f);
        assertTrue(state.selfSwitches().get(1, 5, "A"), "PField_Battles:574-576");
    }

    @Test
    @DisplayName("a single spotting trainer, or a player with one Pokemon, fights alone")
    void noWaitingWhenAlone() {
        addAblePokemon(2);
        triggered = new int[] { 5 };
        run(interpreter(), 5, TRAINER_STEP);
        assertEquals(List.of("trainers:1"), calls);
        calls.clear();
        triggered = new int[] { 5, 8 };
        state.trainer().party.members().get(1).hp = 0;
        run(interpreter(), 5, TRAINER_STEP);
        assertEquals(List.of("trainers:1"), calls);
    }

    private static EventCommand script(String blockId) {
        EventCommand command = cmd(0, 355, 0, array("pbWildBattle(:FOE,12)"));
        command.scriptBlockId = blockId;
        return command;
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":1,\"species\":{\"FOE\":{"
                + "\"id\":1,\"internalName\":\"FOE\",\"name\":\"Foe\",\"types\":[\"NORMAL\"],"
                + "\"baseStats\":[45,49,49,45,65,65],\"growthRate\":\"Medium\",\"baseExp\":60,"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        write(root, "moves.json", "{\"total\":1,\"moves\":{\"TACKLE\":{\"id\":33,\"internalName\":\"TACKLE\","
                + "\"name\":\"Tackle\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\",\"pp\":35,\"target\":\"NearOther\"}}}");
        write(root, "trainers.json", "{\"total\":1,\"order\":[\"RIVAL,Blue,1\"],\"trainers\":{"
                + "\"RIVAL,Blue,1\":{\"key\":\"RIVAL,Blue,1\",\"type\":\"RIVAL\",\"name\":\"Blue\","
                + "\"version\":1,\"party\":[{\"species\":\"FOE\",\"level\":12,\"moves\":[\"TACKLE\"]}]}}}");
        write(root, "encounters.json", "{\"total\":0,\"byMap\":{}}");
        write(root, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(root, "types.json", "{\"total\":1,\"types\":{\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":[{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
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
            array.addChild(new JsonValue(String.valueOf(value)));
        }
        return array;
    }
}
