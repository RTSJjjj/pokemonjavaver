package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 3 / P0c: the Pokemon construction scripts the compiler turns into
 * POKEMON_CREATE / POKEMON_CALL / POKEMON_SET / PARTY_ADD / SET_VARIABLE IR
 * ({@code p = pbGenPkmn(:BULBASAUR, 5); p.makeShiny; p.iv = [...];
 * pbAddPokemon(p, 1)}).
 */
class PokemonScriptTest {

    private GameState state;
    private EventInterpreter interpreter;
    private ScriptIr scriptIr;
    private final List<String> warnings = new ArrayList<>();

    @BeforeEach
    void setUp() {
        state = new GameState();
        state.enterMap(1, 0, 0);
        warnings.clear();
        scriptIr = ScriptIr.empty();
        interpreter = new EventInterpreter(state, new MessageService(), new InputManager(), null,
                id -> null, null, new PictureService(), warnings::add);
        interpreter.attachScriptIr(scriptIr);
    }

    @Test
    @DisplayName("P0c: a pbGenPkmn construction script fills the party and applies its setters")
    void constructionScript(@TempDir Path tempDir) throws Exception {
        interpreter.attachPbs(PbsData.parse(syntheticPbs(tempDir)));
        scriptIr.put("block1", new JsonReader().parse(
                "{\"command\":\"SEQUENCE\",\"steps\":["
                        + "{\"command\":\"POKEMON_CREATE\",\"local\":\"p\",\"species\":\"BULBASAUR\",\"level\":5},"
                        + "{\"command\":\"POKEMON_SET\",\"local\":\"p\",\"property\":\"iv\","
                        + "\"value\":[31,31,31,31,31,31]},"
                        + "{\"command\":\"POKEMON_CALL\",\"local\":\"p\",\"action\":\"makeShiny\",\"args\":[]},"
                        + "{\"command\":\"POKEMON_CALL\",\"local\":\"p\",\"action\":\"setAbility\",\"args\":[2]},"
                        + "{\"command\":\"POKEMON_CALL\",\"local\":\"p\",\"action\":\"setNature\",\"args\":[\"LONELY\"]},"
                        + "{\"command\":\"POKEMON_CALL\",\"local\":\"p\",\"action\":\"setItem\",\"args\":[\"LEFTOVERS\"]},"
                        + "{\"command\":\"POKEMON_SET\",\"local\":\"p\",\"property\":\"ot\",\"value\":\"阿辽\"},"
                        + "{\"command\":\"POKEMON_CALL\",\"local\":\"p\",\"action\":\"pbLearnMove\",\"args\":[\"VINEWHIP\"]},"
                        + "{\"command\":\"POKEMON_CALL\",\"local\":\"p\",\"action\":\"calcStats\",\"args\":[]},"
                        + "{\"command\":\"PARTY_ADD\",\"local\":\"p\"},"
                        + "{\"command\":\"SET_VARIABLE\",\"id\":10,\"value\":7}]}"));
        interpreter.start(program(script("block1")), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);

        assertFalse(interpreter.running());
        assertEquals(1, state.trainer().partyCount(), () -> "warnings: " + warnings);
        Pokemon pokemon = state.trainer().first();
        assertEquals("BULBASAUR", pokemon.species.internalName);
        assertEquals(5, pokemon.level);
        assertTrue(pokemon.shiny);
        assertArrayEquals(new int[] {31, 31, 31, 31, 31, 31}, pokemon.ivs);
        assertEquals("CHLOROPHYLL", pokemon.ability, "setAbility(2) picks the hidden ability");
        assertEquals("Lonely", pokemon.nature.name);
        assertEquals("LEFTOVERS", pokemon.item);
        assertEquals("阿辽", pokemon.originalTrainer);
        assertTrue(hasMove(pokemon, "TACKLE") && hasMove(pokemon, "VINEWHIP"),
                () -> "moves: " + pokemon.moves.size);
        assertEquals(7, state.variables().get(10));
    }

    @Test
    @DisplayName("P0c: p.form switches to the form data and pbSet reads the variables back")
    void formAndVariables(@TempDir Path tempDir) throws Exception {
        interpreter.attachPbs(PbsData.parse(syntheticPbs(tempDir)));
        scriptIr.put("block1", new JsonReader().parse(
                "{\"command\":\"SEQUENCE\",\"steps\":["
                        + "{\"command\":\"POKEMON_CREATE\",\"local\":\"p\",\"species\":\"BULBASAUR\",\"level\":5},"
                        + "{\"command\":\"POKEMON_SET\",\"local\":\"p\",\"property\":\"form\",\"value\":1},"
                        + "{\"command\":\"POKEMON_CALL\",\"local\":\"p\",\"action\":\"calcStats\",\"args\":[]},"
                        + "{\"command\":\"PARTY_ADD\",\"local\":\"p\"},"
                        + "{\"command\":\"SET_VARIABLE\",\"id\":5,\"value\":42},"
                        + "{\"command\":\"SET_VARIABLE\",\"id\":6,\"value\":{\"variable\":5}}]}"));
        interpreter.start(program(script("block1")), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);

        Pokemon pokemon = state.trainer().first();
        assertNotNull(pokemon.form);
        assertEquals(1, pokemon.form.form);
        assertEquals("BULBASAUR_1", pokemon.internalName);
        assertEquals(pokemon.maxHp(), pokemon.hp, "calcStats refreshes the current HP");
        assertEquals(80, pokemon.baseStat(PokemonStats.HP), "the form's base stats win");
        assertEquals(42, state.variables().get(6), "pbGet(id) reads the variable back");
    }

    @Test
    @DisplayName("P0c: the seventh party member overflows to the box instead of being lost")
    void partyOverflow(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        TrainerState trainer = new TrainerState();
        for (int i = 0; i < TrainerState.PARTY_LIMIT; i++) {
            assertTrue(trainer.addToParty(new Pokemon(data.species("BULBASAUR"), 5, data)));
        }
        assertFalse(trainer.addToParty(new Pokemon(data.species("BULBASAUR"), 5, data)));
        assertEquals(TrainerState.PARTY_LIMIT, trainer.partyCount());
        assertEquals(1, trainer.storage.size);
    }

    @Test
    @DisplayName("P0c: without PBS data the construction script is skipped, not fatal")
    void missingPbsIsSkipped() {
        scriptIr.put("block1", new JsonReader().parse(
                "{\"command\":\"SEQUENCE\",\"steps\":["
                        + "{\"command\":\"POKEMON_CREATE\",\"local\":\"p\",\"species\":\"BULBASAUR\",\"level\":5},"
                        + "{\"command\":\"PARTY_ADD\",\"local\":\"p\"}]}"));
        interpreter.start(program(script("block1")), 1, 5);
        interpreter.update(0f);
        interpreter.update(0f);
        assertFalse(interpreter.running());
        assertEquals(0, state.trainer().partyCount());
    }

    private static boolean hasMove(Pokemon pokemon, String internalName) {
        for (int i = 0; i < pokemon.moves.size; i++) {
            PbsData.Move move = pokemon.moves.get(i).move;
            if (move != null && internalName.equals(move.internalName)) {
                return true;
            }
        }
        return false;
    }

    private static EventCommand script(String blockId) {
        EventCommand command = cmd(0, 355, 0, array("p=pbGenPkmn(:BULBASAUR,5)"));
        command.scriptBlockId = blockId;
        return command;
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    /** Minimal generated/pbs tree (the same shape pbs.js writes). */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":1,\"byId\":{\"1\":\"BULBASAUR\"},\"species\":{"
                + "\"BULBASAUR\":{\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"Bulbasaur\","
                + "\"types\":[\"GRASS\",\"POISON\"],\"baseStats\":[45,49,49,45,65,65],"
                + "\"growthRate\":\"Medium\",\"abilities\":[\"OVERGROW\"],\"hiddenAbility\":\"CHLOROPHYLL\","
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        write(root, "pokemonforms.json", "{\"total\":1,\"forms\":{\"BULBASAUR_1\":{"
                + "\"species\":\"BULBASAUR\",\"form\":1,\"key\":\"BULBASAUR_1\",\"formName\":\"Mega\","
                + "\"baseStats\":[80,100,123,80,122,120],\"types\":[\"GRASS\",\"POISON\"],"
                + "\"abilities\":[\"THICKFAT\"]}}}");
        write(root, "moves.json", "{\"total\":2,\"moves\":{"
                + "\"TACKLE\":{\"id\":33,\"internalName\":\"TACKLE\",\"name\":\"Tackle\",\"pp\":35},"
                + "\"VINEWHIP\":{\"id\":22,\"internalName\":\"VINEWHIP\",\"name\":\"Vine Whip\",\"pp\":25}}}");
        write(root, "items.json", "{\"total\":1,\"items\":{"
                + "\"LEFTOVERS\":{\"id\":211,\"internalName\":\"LEFTOVERS\",\"name\":\"Leftovers\"}}}");
        write(root, "abilities.json", "{\"total\":2,\"abilities\":{"
                + "\"OVERGROW\":{\"id\":65,\"internalName\":\"OVERGROW\",\"name\":\"Overgrow\"},"
                + "\"CHLOROPHYLL\":{\"id\":34,\"internalName\":\"CHLOROPHYLL\",\"name\":\"Chlorophyll\"},"
                + "\"THICKFAT\":{\"id\":47,\"internalName\":\"THICKFAT\",\"name\":\"Thick Fat\"}}}");
        write(root, "types.json", "{\"total\":2,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"GRASS\":{\"id\":1,\"internalName\":\"GRASS\",\"name\":\"Grass\"}}}");
        write(root, "natures.json", "{\"total\":2,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"},"
                + "{\"id\":1,\"internalName\":\"LONELY\",\"name\":\"Lonely\","
                + "\"statUp\":\"ATTACK\",\"statDown\":\"DEFENSE\"}]}");
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
