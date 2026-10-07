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
 * Stage 3 / P2: the event interpreter hands WILD_BATTLE / FREE_WILD_BATTLE /
 * TRAINER_BATTLE to the attached {@link BattlePort}.
 */
class BattleScriptTest {

    private GameState state;
    private EventInterpreter interpreter;
    private ScriptIr scriptIr;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> calls = new ArrayList<>();
    /** The finished battle, like a real port's lastResult (BattlePort:30-32). */
    private final BattleResult[] finished = new BattleResult[1];

    @BeforeEach
    void setUp() {
        state = new GameState();
        state.enterMap(1, 0, 0);
        warnings.clear();
        calls.clear();
        finished[0] = null;
        scriptIr = ScriptIr.empty();
        interpreter = new EventInterpreter(state, new MessageService(), new InputManager(), null,
                id -> null, null, new PictureService(), warnings::add);
        interpreter.attachScriptIr(scriptIr);
        interpreter.attachBattlePort(new BattlePort() {
            @Override
            public BattleResult wildBattle(String species, int level) {
                calls.add("wild:" + species + ":" + level);
                return finish(new BattleResult(BattleResult.Outcome.WIN, 1, null));
            }

            @Override
            public BattleResult freeWildBattle(Pokemon foe) {
                calls.add("free:" + foe.species.internalName);
                return finish(new BattleResult(BattleResult.Outcome.WIN, 1, null));
            }

            @Override
            public BattleResult trainerBattle(PbsData.TrainerData trainer) {
                calls.add("trainer:" + trainer.type + ":" + trainer.name + ":" + trainer.version);
                return finish(new BattleResult(BattleResult.Outcome.WIN, 1, null));
            }

            @Override
            public BattleResult lastResult() {
                return finished[0];
            }

            @Override
            public void setCanRun(boolean value) {
                calls.add("canRun:" + value);
            }

            @Override
            public void setCanLose(boolean value) {
                calls.add("canLose:" + value);
            }

            @Override
            public void setDisablePokeBalls(boolean value) {
                calls.add("disablePokeBalls:" + value);
            }
        });
    }

    /** Records the battle a synchronous test port just finished. */
    private BattleResult finish(BattleResult result) {
        finished[0] = result;
        return result;
    }

    @Test
    @DisplayName("R14: a wild battle writes its outcome variable and applies its arguments")
    void wildBattleShell() {        scriptIr.put("block1", new JsonReader().parse(
                "{\"command\":\"SEQUENCE\",\"steps\":["
                        + "{\"command\":\"WILD_BATTLE\",\"species\":\"FOE\",\"level\":12,"
                        + "\"outcomeVar\":7,\"canRun\":false,\"canLose\":true}]}"));
        interpreter.start(program(script("block1")), 1, 5);
        interpreter.update(0f);
        // The fake port finishes immediately, so the decision is written on the
        // next update (PField_Battles:510-516).
        interpreter.update(0f);

        assertTrue(calls.contains("canRun:false"), calls.toString());
        assertTrue(calls.contains("canLose:true"), calls.toString());
        assertEquals(1, state.variables().get(7),
                "PField_Battles:343 writes the decision into the outcome variable");
    }

    @Test
    @DisplayName("R14: a plain wild battle still writes variable 1, the plugin's default")
    void wildBattleDefaultOutcomeVariable() {
        scriptIr.put("block1", new JsonReader().parse(
                "{\"command\":\"SEQUENCE\",\"steps\":["
                        + "{\"command\":\"WILD_BATTLE\",\"species\":\"FOE\",\"level\":12}]}"));
        interpreter.start(program(script("block1")), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);
        assertEquals(1, state.variables().get(1));
    }

    @Test
    @DisplayName("P2: WILD_BATTLE / TRAINER_BATTLE reach the battle port")
    void wildAndTrainerBattles(@TempDir Path tempDir) throws Exception {
        interpreter.attachPbs(PbsData.parse(syntheticPbs(tempDir)));
        scriptIr.put("block1", new JsonReader().parse(
                "{\"command\":\"SEQUENCE\",\"steps\":["
                        + "{\"command\":\"WILD_BATTLE\",\"species\":\"FOE\",\"level\":12},"
                        + "{\"command\":\"TRAINER_BATTLE\",\"trainerType\":\"RIVAL\",\"trainerName\":\"Blue\",\"version\":1}]}"));
        interpreter.start(program(script("block1")), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);

        assertFalse(interpreter.running());
        assertEquals(List.of("wild:FOE:12", "trainer:RIVAL:Blue:1"), calls);
    }

    @Test
    @DisplayName("P2: FREE_WILD_BATTLE battles the Pokemon built in the same script")
    void freeWildBattle(@TempDir Path tempDir) throws Exception {
        interpreter.attachPbs(PbsData.parse(syntheticPbs(tempDir)));
        scriptIr.put("block1", new JsonReader().parse(
                "{\"command\":\"SEQUENCE\",\"steps\":["
                        + "{\"command\":\"POKEMON_CREATE\",\"local\":\"p\",\"species\":\"FOE\",\"level\":7},"
                        + "{\"command\":\"FREE_WILD_BATTLE\",\"local\":\"p\"}]}"));
        interpreter.start(program(script("block1")), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);

        assertEquals(List.of("free:FOE"), calls);
    }

    @Test
    @DisplayName("P2: an unknown trainer is skipped without calling the port")
    void unknownTrainerSkipped(@TempDir Path tempDir) throws Exception {
        interpreter.attachPbs(PbsData.parse(syntheticPbs(tempDir)));
        scriptIr.put("block1", new JsonReader().parse(
                "{\"command\":\"TRAINER_BATTLE\",\"trainerType\":\"NOPE\",\"trainerName\":\"Ghost\"}"));
        interpreter.start(program(script("block1")), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);
        assertTrue(calls.isEmpty());
    }

    @Test
    @DisplayName("P2: ROCK_SMASH_ENCOUNTER picks from the map's RockSmash table")
    void rockSmashEncounter(@TempDir Path tempDir) throws Exception {
        interpreter.attachPbs(PbsData.parse(syntheticPbs(tempDir)));
        scriptIr.put("block1", new JsonReader().parse("{\"command\":\"ROCK_SMASH_ENCOUNTER\"}"));
        interpreter.start(program(script("block1")), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);
        assertEquals(List.of("wild:FOE:5"), calls);
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
                + "\"name\":\"Tackle\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\",\"pp\":35}}}");
        write(root, "trainers.json", "{\"total\":1,\"order\":[\"RIVAL,Blue,1\"],\"trainers\":{"
                + "\"RIVAL,Blue,1\":{\"key\":\"RIVAL,Blue,1\",\"type\":\"RIVAL\",\"name\":\"Blue\","
                + "\"version\":1,\"party\":[{\"species\":\"FOE\",\"level\":12,\"moves\":[\"TACKLE\"]}]}}}");
        write(root, "encounters.json", "{\"total\":1,\"byMap\":{\"1\":{\"id\":1,"
                + "\"densities\":{\"RockSmash\":10},"
                + "\"methods\":{\"RockSmash\":[{\"species\":\"FOE\",\"min\":5,\"max\":5}]}}}}");
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
