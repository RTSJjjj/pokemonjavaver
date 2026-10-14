package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.battle.BattlePort;
import pokemon.runtime.battle.BattleResult;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.input.GameAction;
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
 * Boss_Battles (section 319): a {@code def battleXxx} used as a script condition builds its Pokemon, records
 * {@code setBattleRule(sprintf("%dv1",size))} / canlose / noexp, calls pbWildBattleCore and returns {@code decision==N}.
 */
class BossBattleScriptTest {

    @TempDir
    Path tempDir;

    private GameState state;
    private EventInterpreter interpreter;
    private final InputManager input = new InputManager();
    private PbsData pbs;
    private final List<String> calls = new ArrayList<>();
    private final List<Pokemon> foes = new ArrayList<>();
    private final boolean[] switch196During = new boolean[1];
    private BattleResult finished;
    private BattleResult.Outcome outcome = BattleResult.Outcome.WIN;

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(tempDir, "pokemon.json", "{\"total\":3,\"species\":{"
                + "\"FOE\":{\"id\":1,\"internalName\":\"FOE\",\"name\":\"Foe\",\"types\":[\"NORMAL\"],"
                + "\"baseStats\":[45,49,49,45,65,65],\"growthRate\":\"Medium\",\"baseExp\":60,"
                + "\"abilities\":[\"BLAZE\"],\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]},"
                + "\"IRONJUGULIS\":{\"id\":2,\"internalName\":\"IRONJUGULIS\",\"name\":\"铁颈羽\",\"types\":[\"DARK\",\"FLYING\"],"
                + "\"baseStats\":[94,80,86,122,80,108],\"growthRate\":\"Slow\",\"baseExp\":300,\"abilities\":[\"QUARKDRIVE\"],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]},"
                + "\"OVERLORDFLOS\":{\"id\":3,\"internalName\":\"OVERLORDFLOS\",\"name\":\"霸王花\",\"types\":[\"GRASS\"],"
                + "\"baseStats\":[80,100,90,70,80,60],\"growthRate\":\"Slow\",\"baseExp\":200,\"abilities\":[\"OVERGROW\"],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        StringBuilder moves = new StringBuilder("{\"total\":9,\"moves\":{");
        String[] names = { "TACKLE", "DARKPULSE", "AIRSLASH", "DRAGONPULSE", "HYPERVOICE", "POWERWHIP", "SWORDSDANCE",
                "LEECHSEED", "IRONHEAD" };
        for (int i = 0; i < names.length; i++) {
            if (i > 0) moves.append(',');
            moves.append("\"").append(names[i]).append("\":{\"id\":").append(i + 1).append(",\"internalName\":\"")
                    .append(names[i]).append("\",\"name\":\"").append(names[i]).append("\",\"power\":40,\"type\":\"NORMAL\","
                            + "\"category\":\"Physical\",\"pp\":35,\"target\":\"NearOther\"}");
        }
        write(tempDir, "moves.json", moves.append("}}").toString());
        write(tempDir, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(tempDir, "types.json", "{\"total\":1,\"types\":{\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\"}}}");
        write(tempDir, "natures.json", "{\"total\":2,\"natures\":[{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"},"
                + "{\"id\":1,\"internalName\":\"TIMID\",\"name\":\"Timid\",\"statUp\":\"SPEED\",\"statDown\":\"ATTACK\"},"
                + "{\"id\":2,\"internalName\":\"ADAMANT\",\"name\":\"Adamant\",\"statUp\":\"ATTACK\",\"statDown\":\"SPATK\"}]}");
        write(tempDir, "items.json", "{\"total\":0,\"items\":{}}");
        write(tempDir, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(tempDir, "trainertypes.json", "{\"total\":1,\"trainerTypes\":{\"POKEMONTRAINER_MIRRORf\":{\"id\":999,"
                + "\"internalName\":\"POKEMONTRAINER_MIRRORf\",\"name\":\"宝可梦训练家\",\"baseMoney\":60}}}");
        write(tempDir, "trainers.json", "{\"total\":2,\"order\":[\"LASS,Amy,0\",\"LADY,Bea,2\"],\"trainers\":{"
                + "\"LASS,Amy,0\":{\"key\":\"LASS,Amy,0\",\"type\":\"LASS\",\"name\":\"Amy\",\"version\":0,"
                + "\"party\":[{\"species\":\"FOE\",\"level\":12}]},"
                + "\"LADY,Bea,2\":{\"key\":\"LADY,Bea,2\",\"type\":\"LADY\",\"name\":\"Bea\",\"version\":2,"
                + "\"party\":[{\"species\":\"FOE\",\"level\":12}]}}}");
        write(tempDir, "encounters.json", "{\"total\":0,\"byMap\":{}}");
        write(tempDir, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        pbs = PbsData.parse(tempDir.toFile());

        state = new GameState();
        state.enterMap(1, 0, 0);
        calls.clear();
        foes.clear();
        finished = null;
        outcome = BattleResult.Outcome.WIN;
        interpreter = new EventInterpreter(state, new MessageService(), input, null,
                id -> null, null, new PictureService(), message -> { });
        interpreter.attachScriptIr(ScriptIr.empty());
        interpreter.attachPbs(pbs);
        interpreter.attachBattlePort(new BattlePort() {
            @Override public BattleResult wildBattle(String species, int level) {
                throw new AssertionError("a boss is a Pokemon, not a species");
            }

            @Override public BattleResult freeWildBattle(Pokemon foe) {
                foes.add(foe);
                switch196During[0] = state.switches().get(196);
                finished = new BattleResult(outcome, 1, null);
                return finished;
            }

            @Override public BattleResult trainerBattle(PbsData.TrainerData trainer) {
                throw new AssertionError("not a trainer battle");
            }

            @Override public BattleResult trainerBattle(List<PbsData.TrainerData> trainers) {
                for (PbsData.TrainerData t : trainers) calls.add("trainer:" + t.type + ":" + t.name + ":" + t.version);
                finished = new BattleResult(outcome, 1, null);
                return finished;
            }

            @Override public BattleResult trainerBattle(PbsData.TrainerData trainer, Array<Pokemon> party) {
                calls.add("mirror:" + trainer.type + ":" + trainer.name + ":" + party.size + ":" + trainer.items.size);
                foes.addAll(java.util.Arrays.asList(party.toArray(Pokemon.class)));
                finished = new BattleResult(outcome, 1, null);
                return finished;
            }

            @Override public void setMoneyGain(boolean value) { calls.add("moneyGain:" + value); }

            @Override public BattleResult lastResult() { return finished; }
            @Override public void setBattleSize(String size) { calls.add("size:" + size); }
            @Override public void setCanLose(boolean value) { calls.add("canLose:" + value); }
            @Override public void setExpGain(boolean value) { calls.add("expGain:" + value); }
        });
    }

    private void ableParty(int count) {
        for (int i = 0; i < count; i++) {
            state.trainer().addToParty(new Pokemon(pbs.species("FOE"), 50, pbs));
        }
    }

    /** {@code if <script>; switch 60 on; else; switch 61 on; end}. */
    private void run(String script) {
        Array<EventCommand> list = new Array<>();
        list.add(cmd(0, 111, 0, array(12, script)));
        list.add(cmd(1, 121, 1, array(60, 60, 0)));
        list.add(cmd(2, 411, 0, null));
        list.add(cmd(3, 121, 1, array(61, 61, 0)));
        list.add(cmd(4, 412, 0, null));
        interpreter.start(list, 1, 5);
        for (int i = 0; i < 4; i++) interpreter.update(0f);
    }

    @Test
    @DisplayName("battleIronJugulis (319:3287): 3 able Pokemon -> 3v1, canlose, noexp; the Boss is built statement by statement; decision==4 is the branch")
    void ironJugulisCaught() {
        ableParty(3);
        outcome = BattleResult.Outcome.CAUGHT;
        run("battleIronJugulis");

        assertEquals(1, foes.size());
        Pokemon boss = foes.get(0);
        assertEquals("IRONJUGULIS", boss.species.internalName);
        assertEquals(100, boss.level);
        assertEquals(2, boss.battleRank);
        assertEquals("BOOSTERENERGY", boss.item);
        assertEquals("TIMID", boss.nature.internalName);
        assertEquals("QUARKDRIVE", boss.ability);
        assertArrayEquals(new int[] { 31, 31, 31, 31, 31, 31 }, boss.ivs);
        // pbLearnMove (PokeBattle_Pokemon:466-495): TACKLE is forgotten when the fourth new move arrives.
        List<String> learned = new ArrayList<>();
        for (Pokemon.MoveSlot slot : boss.moves) learned.add(slot.move.internalName);
        assertEquals(List.of("DARKPULSE", "AIRSLASH", "DRAGONPULSE", "HYPERVOICE"), learned);
        assertFalse(boss.shiny);
        assertEquals(boss.maxHp(), boss.hp);
        assertTrue(calls.contains("size:3v1"), calls.toString());
        assertTrue(calls.contains("canLose:true"), calls.toString());
        assertTrue(calls.contains("expGain:false"), calls.toString());
        assertTrue(state.switches().get(60), "decision==4 is true");
        assertFalse(state.switches().get(61));
        assertFalse(switch196During[0], "battleIronJugulis does not touch $game_switches[196]");
    }

    @Test
    @DisplayName("return decision==4: a win is not a capture, so the branch is false")
    void ironJugulisWonIsFalse() {
        ableParty(1);
        run("battleIronJugulis");
        assertTrue(calls.contains("size:1v1"), calls.toString());     // size = (count > 2) ? 3 : (count > 1) ? 2 : 1
        assertFalse(state.switches().get(60));
        assertTrue(state.switches().get(61));
    }

    @Test
    @DisplayName("battleOverlordflos (319:2245): $game_switches[196] is on during the battle and off after; size = (count >= 2) ? 2 : 1; return decision==1")
    void overlordflos() {
        ableParty(3);
        run("battleOverlordflos");

        assertTrue(switch196During[0]);
        assertFalse(state.switches().get(196));
        assertTrue(calls.contains("size:2v1"), calls.toString());
        Pokemon boss = foes.get(0);
        assertEquals(55, boss.level);
        assertEquals(2, boss.battleRank);
        assertTrue(state.switches().get(60));
    }

    @Test
    @DisplayName("battleBoss(:SPECIES,level,rank) (319:5-31): rank, pbRandomIV(rank-1), not shiny; only a capture is true")
    void genericBattleBoss() {
        ableParty(2);
        outcome = BattleResult.Outcome.CAUGHT;
        run("battleBoss(:IRONJUGULIS,100,3)");
        assertEquals(1, foes.size());
        Pokemon boss = foes.get(0);
        assertEquals("IRONJUGULIS", boss.species.internalName);
        assertEquals(100, boss.level);
        assertEquals(3, boss.battleRank);
        int perfect = 0;
        for (int iv : boss.ivs) if (iv == 31) perfect++;
        assertTrue(perfect >= 2, java.util.Arrays.toString(boss.ivs));     // pbRandomIV(2): two perfect IVs at least
        assertFalse(boss.shiny);
        assertTrue(calls.contains("size:2v1"), calls.toString());
        assertTrue(state.switches().get(60), "decision==4 is true");

        outcome = BattleResult.Outcome.WIN;
        foes.clear();
        state.switches().set(60, false);
        run("battleBoss(:IRONJUGULIS)");
        assertEquals(100, foes.get(0).level);                                  // level=100, rank=3 defaults
        assertEquals(3, foes.get(0).battleRank);
        assertFalse(state.switches().get(60), "a win is not a capture");
    }

    @Test
    @DisplayName("pbDoubleTrainerBattle as a condition (PField_Battles:582-596): both trainers, the double rule, decision==1")
    void doubleTrainerBattleCondition() {
        ableParty(2);
        run("pbDoubleTrainerBattle(:LASS,\"Amy\",0,nil,:LADY,\"Bea\",2,nil,nil)");
        assertTrue(calls.contains("trainer:LASS:Amy:0"), calls.toString());
        assertTrue(calls.contains("trainer:LADY:Bea:2"), calls.toString());
        assertTrue(calls.contains("size:double"), calls.toString());
        assertTrue(state.switches().get(60), "a win is true");

        outcome = BattleResult.Outcome.LOSS;
        state.switches().set(60, false);
        run("pbDoubleTrainerBattle(:LASS,\"Amy\",0,nil,:LADY,\"Bea\",2,nil,nil)");
        assertFalse(state.switches().get(60));
        assertTrue(state.switches().get(61));
    }

    @Test
    @DisplayName("pbMirrorBattle (349): copies of the player's party, MIRRORf for a female player, 6 Full Restores, noMoney/noExp/canLose")
    void mirrorBattleCondition() {
        ableParty(2);
        state.trainer().addToParty(new Pokemon(pbs.species("FOE"), 1, pbs));
        state.trainer().party.get(2).egg = true;
        state.playerId(1);
        state.trainer().name = "Leaf";
        run("pbMirrorBattle");
        assertTrue(calls.contains("mirror:POKEMONTRAINER_MIRRORf:Leaf？:2:6"), calls.toString());
        assertTrue(calls.contains("expGain:false"), calls.toString());
        assertTrue(calls.contains("canLose:true"), calls.toString());
        assertTrue(calls.contains("moneyGain:false"), calls.toString());
        assertNotSame(state.trainer().party.get(0), foes.get(0), "the mirror fights copies");
        assertNotSame(state.trainer().party.get(0).moves.get(0), foes.get(0).moves.get(0), "the moves are not shared");
        assertEquals("Battle 影子", state.nextBattleBGM());
        assertTrue(state.switches().get(60));
    }

    @Test
    @DisplayName("the lazy-dog switch 197 skips the battle and returns true (319:6-9)")
    void lazyDog() {
        ableParty(3);
        state.switches().set(197, true);
        run("battleHaxorusDemon");
        for (int i = 0; i < 4; i++) {                                  // the pbMessage waits for the confirm key
            input.beginFrame();
            input.press(GameAction.CONFIRM);
            interpreter.update(0f);
            input.endFrame();
            input.release(GameAction.CONFIRM);
        }
        assertTrue(foes.isEmpty());
        assertTrue(state.switches().get(60));
        assertFalse(state.switches().get(196), "the skip returns before $game_switches[196] = true");
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
            if (value instanceof Integer) {
                array.addChild(new JsonValue(((Integer) value).longValue()));
            } else {
                array.addChild(new JsonValue(String.valueOf(value)));
            }
        }
        return array;
    }
}
