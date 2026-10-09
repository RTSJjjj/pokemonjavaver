package pokemon.runtime.event;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.data.AudioManifestData;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Roadmap stage 1: the RMXP event commands the project uses but the interpreter
 * ignored: Label / Jump to Label (118/119), Change Gold (125), Recover All
 * (314), Memorize / Restore BGM and BGS (247/248) and BGS fade (246).
 */
class EventStage1CommandsTest {

    private final List<String> warnings = new ArrayList<>();
    private final List<String> calls = new ArrayList<>();
    private GameState state;
    private AudioManager audio;
    private EventInterpreter interpreter;
    private PictureService pictures;
    private MessageService messages;
    private InputManager input;

    @BeforeEach
    void setUp() throws Exception {
        warnings.clear();
        calls.clear();
        pictures = new PictureService();
        messages = new MessageService();
        input = new InputManager();
        state = new GameState();
        state.enterMap(1, 0, 0);
        File root = Files.createTempDirectory("stage1-audio").toFile();
        for (String path : new String[] {"audio/BGM/Town.ogg", "audio/BGM/Cave.ogg", "audio/BGS/Rain.ogg"}) {
            File file = new File(root, path);
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), "fake".getBytes(StandardCharsets.UTF_8));
        }
        audio = new AudioManager();
        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = true;
        manifest.preset = "standard";
        for (String[] entry : new String[][] {
                {"BGM", "Town", "audio/BGM/Town.ogg"}, {"BGM", "Cave", "audio/BGM/Cave.ogg"},
                {"BGS", "Rain", "audio/BGS/Rain.ogg"}}) {
            AudioManifestData.Entry parsed = new AudioManifestData.Entry();
            parsed.type = entry[0];
            parsed.file = entry[2];
            manifest.entries.put(entry[1], parsed);
        }
        audio.attach(manifest, root);
        audio.setBackend(new AudioManager.Backend() {
            @Override
            public Music newMusic(File file) {
                final String name = file.getName();
                return new Music() {
                    public void play() { calls.add("play:" + name); }
                    public void pause() { }
                    public void stop() { calls.add("stop:" + name); }
                    public boolean isPlaying() { return true; }
                    public void setLooping(boolean looping) { }
                    public boolean isLooping() { return true; }
                    public void setVolume(float volume) { }
                    public float getVolume() { return 1f; }
                    public void setPan(float pan, float volume) { }
                    public void setPosition(float position) { }
                    public float getPosition() { return 0f; }
                    public void dispose() { }
                    public void setOnCompletionListener(OnCompletionListener listener) { }
                };
            }

            @Override
            public Sound newSound(File file) {
                return null;
            }
        });
        interpreter = new EventInterpreter(state, messages, input,
                audio, id -> null, null, pictures, warnings::add);
    }

    private void run(Array<EventCommand> list) {
        interpreter.start(list, 1, 1);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    // ---------------------------------------------------------------- 118 / 119

    @Test
    @DisplayName("119 jumps to the label (118) and continues behind it, skipping what lies between")
    void jumpToLabelSkipsForward() {
        run(program(
                cmd(0, 121, 0, array(1, 1, 0)),              // switch 1 ON
                cmd(1, 119, 0, array("skip")),
                cmd(2, 121, 0, array(2, 2, 0)),              // skipped
                cmd(3, 118, 0, array("skip")),
                cmd(4, 121, 0, array(3, 3, 0)),              // switch 3 ON
                cmd(5, 0, 0, null)));
        assertTrue(state.switches().get(1));
        assertFalse(state.switches().get(2), "the command between the jump and the label must not run");
        assertTrue(state.switches().get(3));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    @DisplayName("119 can jump backwards: a counted loop built from a label and a conditional")
    void jumpToLabelLoopsBackwards() {
        // var1 += 1 ; if var1 < 3 then jump back
        run(program(
                cmd(0, 118, 0, array("again")),
                cmd(1, 122, 0, array(1, 1, 1, 0, 1)),                 // var1 += 1
                cmd(2, 111, 0, array(1, 1, 0, 3, 4)),                 // if var1 < 3 (constant 3, op 4 = "<")
                cmd(3, 119, 1, array("again")),
                cmd(4, 412, 0, null),
                cmd(5, 0, 0, null)));
        assertEquals(3, state.variables().get(1));
    }

    @Test
    @DisplayName("119 with an unknown label just continues (048_Interpreter:793-795)")
    void unknownLabelContinues() {
        run(program(
                cmd(0, 119, 0, array("nowhere")),
                cmd(1, 121, 0, array(4, 4, 0)),
                cmd(2, 0, 0, null)));
        assertTrue(state.switches().get(4));
    }

    @Test
    @DisplayName("the label scan never looks at the final command of the list")
    void lastCommandIsNotScanned() {
        // temp_index < list.size-1: a label that is the very last command is not found.
        run(program(
                cmd(0, 119, 0, array("last")),
                cmd(1, 121, 0, array(5, 5, 0)),
                cmd(2, 118, 0, array("last"))));
        assertTrue(state.switches().get(5));
    }

    // ---------------------------------------------------------------- 111 conditions

    /** Runs {@code if <condition> then switch 9 ON} and reports whether the branch ran. */
    private boolean branchRuns(JsonValue condition) {
        state.switches().set(9, false);
        interpreter.start(program(
                cmd(0, 111, 0, condition),
                cmd(1, 121, 1, array(9, 9, 0)),
                cmd(2, 412, 0, null),
                cmd(3, 0, 0, null)), 1, 1);
        interpreter.update(0f);
        return state.switches().get(9);
    }

    @Test
    @DisplayName("111 type 1 honours the comparison operator (048_Interpreter:610-623), not just >=")
    void variableConditionOperators() {
        state.variables().set(1, 5);
        // [1, variable, operandType, operand, operator]
        assertTrue(branchRuns(array(1, 1, 0, 5, 0)), "5 == 5");
        assertFalse(branchRuns(array(1, 1, 0, 4, 0)), "5 == 4 must be false (it used to read as 5 >= 4)");
        assertTrue(branchRuns(array(1, 1, 0, 5, 1)), "5 >= 5");
        assertFalse(branchRuns(array(1, 1, 0, 6, 1)), "5 >= 6");
        assertTrue(branchRuns(array(1, 1, 0, 5, 2)), "5 <= 5");
        assertFalse(branchRuns(array(1, 1, 0, 4, 2)), "5 <= 4");
        assertTrue(branchRuns(array(1, 1, 0, 4, 3)), "5 > 4");
        assertFalse(branchRuns(array(1, 1, 0, 5, 3)), "5 > 5");
        assertTrue(branchRuns(array(1, 1, 0, 6, 4)), "5 < 6");
        assertFalse(branchRuns(array(1, 1, 0, 5, 4)), "5 < 5");
        assertTrue(branchRuns(array(1, 1, 0, 4, 5)), "5 != 4");
        assertFalse(branchRuns(array(1, 1, 0, 5, 5)), "5 != 5");
    }

    @Test
    @DisplayName("111 type 1 compares against another variable when the operand type is 1")
    void variableConditionAgainstVariable() {
        state.variables().set(1, 5);
        state.variables().set(2, 5);
        assertTrue(branchRuns(array(1, 1, 1, 2, 0)), "var1 == var2");
        state.variables().set(2, 6);
        assertFalse(branchRuns(array(1, 1, 1, 2, 0)));
        assertTrue(branchRuns(array(1, 1, 1, 2, 4)), "var1 < var2");
    }

    @Test
    @DisplayName("111 type 7 compares the gold (048_Interpreter:648-653): 0 = at least, 1 = at most")
    void goldCondition() {
        state.trainer().money = 1000;
        assertTrue(branchRuns(array(7, 1000, 0)));
        assertFalse(branchRuns(array(7, 1001, 0)));
        assertTrue(branchRuns(array(7, 1000, 1)));
        assertFalse(branchRuns(array(7, 999, 1)));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    @DisplayName("122 with operand type 7 reads map id / gold / play time (048_Interpreter:853-861)")
    void controlVariableFromOther() {
        state.trainer().money = 4321;
        state.trainer().playSeconds = 125.9;
        interpreter.start(program(
                cmd(0, 122, 0, array(5, 5, 0, 7, 2)),     // var5 = gold
                cmd(1, 122, 0, array(6, 6, 0, 7, 0)),     // var6 = map id
                cmd(2, 122, 0, array(7, 7, 0, 7, 4)),     // var7 = play time in seconds
                cmd(3, 0, 0, null)), 1, 1);
        interpreter.update(0f);
        assertEquals(4321, state.variables().get(5));
        assertEquals(state.currentMapId(), state.variables().get(6));
        assertEquals(125, state.variables().get(7));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    // ---------------------------------------------------------------- 125

    @Test
    @DisplayName("125 adds and subtracts constants and variable operands")
    void changeGold() {
        state.trainer().money = 100;
        state.variables().set(7, 50);
        run(program(
                cmd(0, 125, 0, array(0, 0, 400)),          // + 400
                cmd(1, 125, 0, array(1, 1, 7)),            // - var7 (50)
                cmd(2, 0, 0, null)));
        assertEquals(450, state.trainer().money);
    }

    @Test
    @DisplayName("125 clamps the money to 0 .. MAX_MONEY like PokeBattle_Trainer#money=")
    void changeGoldClamps() {
        state.trainer().money = 100;
        run(program(cmd(0, 125, 0, array(1, 0, 5000)), cmd(1, 0, 0, null)));
        assertEquals(0, state.trainer().money);
        run(program(cmd(0, 125, 0, array(0, 0, 999_999_999)), cmd(1, 125, 0, array(0, 0, 999_999_999)),
                cmd(2, 0, 0, null)));
        assertEquals(999_999_999, state.trainer().money);
    }

    // ---------------------------------------------------------------- 314

    private Pokemon hurt(int hpLeft, boolean egg) {
        Pokemon p = new Pokemon(null, 5, null);
        p.hp = hpLeft;
        p.status = "PARALYSIS";
        p.egg = egg;
        return p;
    }

    @Test
    @DisplayName("314 (actor 0) heals the whole party and every PC box, leaving eggs alone")
    void recoverAllHealsPartyAndBoxes() {
        Pokemon inParty = hurt(0, false);
        Pokemon inBox = hurt(0, false);
        Pokemon egg = hurt(0, true);
        state.trainer().party.add(inParty);
        state.trainer().currentStorage().set(3, 5, inBox);
        state.trainer().currentStorage().set(3, 6, egg);
        run(program(cmd(0, 314, 0, array(0)), cmd(1, 0, 0, null)));
        assertEquals(inParty.maxHp(), inParty.hp);
        assertEquals("", inParty.status);
        assertEquals(inBox.maxHp(), inBox.hp, "170_PField_Field:900 uses pbEachPokemon, which includes the boxes");
        assertEquals("", inBox.status);
        assertEquals(0, egg.hp, "PokeBattle_Pokemon#heal returns at once for an egg");
        assertEquals("PARALYSIS", egg.status);
    }

    @Test
    @DisplayName("314 for any other actor id does nothing (170_PField_Field:899)")
    void recoverAllOtherActorIsIgnored() {
        Pokemon p = hurt(0, false);
        state.trainer().party.add(p);
        run(program(cmd(0, 314, 0, array(2)), cmd(1, 0, 0, null)));
        assertEquals(0, p.hp);
    }

    // ---------------------------------------------------------------- 247 / 248 / 246

    @Test
    @DisplayName("247 memorizes and 248 restores the BGM and BGS that were playing")
    void memorizeAndRestoreAudio() {
        audio.playBgm("Town", 100, 100);
        audio.playBgs("Rain", 100, 100);
        run(program(
                cmd(0, 247, 0, null),
                cmd(1, 0, 0, null)));
        audio.playBgm("Cave", 100, 100);                  // something else takes over
        audio.stopBgs();
        assertEquals("Cave", audio.currentBgmId());
        run(program(cmd(0, 248, 0, null), cmd(1, 0, 0, null)));
        assertEquals("Town", audio.currentBgmId());
        assertTrue(calls.contains("play:Rain.ogg"));
    }

    @Test
    @DisplayName("248 after memorizing silence stops the music (bgm_play(nil) stops, :73-76)")
    void restoreOfSilenceStops() {
        run(program(cmd(0, 247, 0, null), cmd(1, 0, 0, null)));
        audio.playBgm("Town", 100, 100);
        run(program(cmd(0, 248, 0, null), cmd(1, 0, 0, null)));
        assertNull(audio.currentBgmId());
    }

    @Test
    @DisplayName("246 stops the BGS (no volume ramp in this runtime, registered)")
    void bgsFadeStops() {
        audio.playBgs("Rain", 100, 100);
        run(program(cmd(0, 246, 0, array(1)), cmd(1, 0, 0, null)));
        assertTrue(calls.contains("stop:Rain.ogg"));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    // ---------------------------------------------------------------- 234

    private static JsonValue tone(int red, int green, int blue, int gray) {
        JsonValue object = new JsonValue(JsonValue.ValueType.object);
        object.addChild("red", new JsonValue((long) red));
        object.addChild("green", new JsonValue((long) green));
        object.addChild("blue", new JsonValue((long) blue));
        object.addChild("gray", new JsonValue((long) gray));
        return object;
    }

    @Test
    @DisplayName("234 with duration 0 sets the picture's tone at once (021_Game_Picture:117-119)")
    void pictureToneImmediate() {
        pictures.show(3, "pic", 0, 0f, 0f, 100f, 100f, 255f, 0);
        run(program(cmd(0, 234, 0, array(3, tone(-90, -90, 30, 55), 0)), cmd(1, 0, 0, null)));
        PictureService.Picture picture = pictures.get(3);
        assertEquals(-90f, picture.toneRed);
        assertEquals(30f, picture.toneBlue);
        assertEquals(55f, picture.toneGray);
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    @DisplayName("234 eases towards the target by (t*(d-1)+target)/d each of 40 frames a second")
    void pictureToneEases() {
        pictures.show(3, "pic", 0, 0f, 0f, 100f, 100f, 255f, 0);
        run(program(cmd(0, 234, 0, array(3, tone(100, 0, 0, 0), 10)), cmd(1, 0, 0, null)));   // 10/20 s = 20 frames
        PictureService.Picture picture = pictures.get(3);
        assertEquals(0f, picture.toneRed, "nothing has elapsed yet");
        pictures.update(1f / 40f);                         // one frame: (0*19 + 100)/20 = 5
        assertEquals(5f, picture.toneRed, 0.001f);
        pictures.update(1f / 40f);                         // (5*18 + 100)/19
        assertEquals((5f * 18f + 100f) / 19f, picture.toneRed, 0.001f);
        pictures.update(2f);                               // far past the end
        assertEquals(100f, picture.toneRed, 0.01f);
    }

    @Test
    @DisplayName("234 for a picture that is not shown is ignored")
    void pictureToneMissingPicture() {
        run(program(cmd(0, 234, 0, array(9, tone(1, 2, 3, 4), 0)), cmd(1, 0, 0, null)));
        assertNull(pictures.get(9));
    }

    // ---------------------------------------------------------------- 236 / 354

    @Test
    @DisplayName("236 immediate weather sets type and sprite count (power+1)*4 (019_Game_Screen:78-93)")
    void weatherImmediate() {
        run(program(cmd(0, 236, 0, array(1, 3, 0)), cmd(1, 0, 0, null)));
        assertEquals(1, state.weather().type());
        assertEquals(16f, state.weather().max());
    }

    @Test
    @DisplayName("236 with a duration ramps the sprite count and clearing the weather keeps the type until the end")
    void weatherRamp() {
        run(program(cmd(0, 236, 0, array(3, 7, 20)), cmd(1, 0, 0, null)));
        assertEquals(3, state.weather().type(), "a non-zero type is applied at once");
        assertEquals(0f, state.weather().max());
        state.weather().update(10f / 40f);
        assertTrue(state.weather().max() > 0f && state.weather().max() < 32f);
        state.weather().update(2f);
        assertEquals(32f, state.weather().max(), 0.001f);

        run(program(cmd(0, 236, 0, array(0, 0, 10)), cmd(1, 0, 0, null)));
        assertEquals(3, state.weather().type(), "type 0 only takes over once the fade is done");
        state.weather().update(0.1f);
        assertEquals(3, state.weather().type());
        state.weather().update(2f);
        assertEquals(0, state.weather().type());
        assertEquals(0f, state.weather().max(), 0.001f);
    }

    @Test
    @DisplayName("354 raises the return-to-title flag once (048_Interpreter:1449-1454)")
    void returnToTitle() {
        run(program(cmd(0, 354, 0, null), cmd(1, 121, 0, array(8, 8, 0)), cmd(2, 0, 0, null)));
        assertTrue(interpreterConsumedTitle());
        assertFalse(state.switches().get(8), "the interpreter stops: nothing after the command runs");
    }

    private boolean interpreterConsumedTitle() {
        boolean first = interpreter.consumeTitleRequest();
        return first && !interpreter.consumeTitleRequest();
    }

    // ---------------------------------------------------------------- 102

    @Test
    @DisplayName("102 cancel type 2 answers the second choice when B is pressed (071_Messages:1370-1377)")
    void choiceCancelAnswersCancelTypeMinusOne() {
        interpreter.start(program(
                cmd(0, 102, 0, array(array("是", "否"), 2)),
                cmd(1, 402, 0, array(0)),
                cmd(2, 121, 1, array(1, 1, 0)),               // Yes
                cmd(3, 402, 0, array(1)),
                cmd(4, 121, 1, array(2, 2, 0)),               // No
                cmd(5, 404, 0, null),
                cmd(6, 0, 0, null)), 1, 1);
        interpreter.update(0f);
        assertTrue(messages.choiceMode());
        tap(GameAction.CANCEL);
        assertFalse(state.switches().get(1));
        assertTrue(state.switches().get(2), "B on a Yes/No question with cancel type 2 is \"No\"");
    }

    @Test
    @DisplayName("102 cancel type 0 ignores B: the question stays open")
    void choiceCancelTypeZeroIgnoresB() {
        interpreter.start(program(
                cmd(0, 102, 0, array(array("是", "否"), 0)),
                cmd(1, 404, 0, null),
                cmd(2, 0, 0, null)), 1, 1);
        interpreter.update(0f);
        tap(GameAction.CANCEL);
        assertTrue(messages.choiceMode());
        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
    }

    // ---------------------------------------------------------------- 103

    private void tap(GameAction action) {
        input.beginFrame();
        input.press(action);
        interpreter.update(0f);
        input.endFrame();
        input.release(action);
    }

    @Test
    @DisplayName("103 opens a digit window seeded from the variable and writes the confirmed number back")
    void inputNumberConfirm() {
        state.variables().set(4, 12);
        interpreter.start(program(cmd(0, 103, 0, array(4, 3)), cmd(1, 121, 0, array(8, 8, 0)), cmd(2, 0, 0, null)), 1, 1);
        interpreter.update(0f);
        assertEquals(InterpreterState.WAIT_MESSAGE, interpreter.state());
        assertTrue(messages.numberMode());
        assertEquals(3, messages.numberDigits());
        assertEquals(12, messages.number());
        assertEquals(2, messages.numberIndex(), "the cursor starts on the last digit");

        tap(GameAction.UP);                                   // ones digit 2 -> 3
        assertEquals(13, messages.number());
        tap(GameAction.LEFT);                                 // tens digit
        tap(GameAction.UP);
        assertEquals(23, messages.number());
        tap(GameAction.LEFT);                                 // hundreds digit
        tap(GameAction.DOWN);                                 // 0 -> 9 wraps
        assertEquals(923, messages.number());
        tap(GameAction.CONFIRM);
        assertEquals(923, state.variables().get(4));
        assertFalse(messages.visible());
        assertTrue(state.switches().get(8), "the event continues after the number was chosen");
    }

    @Test
    @DisplayName("103 cancel keeps the variable's value (ChooseNumberParams#cancelNumber = the default)")
    void inputNumberCancel() {
        state.variables().set(4, 7);
        interpreter.start(program(cmd(0, 103, 0, array(4, 2)), cmd(1, 0, 0, null)), 1, 1);
        interpreter.update(0f);
        tap(GameAction.UP);
        assertEquals(8, messages.number());
        tap(GameAction.CANCEL);
        assertEquals(7, state.variables().get(4));
        assertEquals(InterpreterState.FINISHED, interpreter.state());
    }

    @Test
    @DisplayName("103 digit cursor wraps and a one digit window has nowhere to move")
    void inputNumberCursor() {
        interpreter.start(program(cmd(0, 103, 0, array(4, 2)), cmd(1, 0, 0, null)), 1, 1);
        interpreter.update(0f);
        tap(GameAction.RIGHT);                                // 1 -> 0 (wraps)
        assertEquals(0, messages.numberIndex());
        tap(GameAction.LEFT);
        assertEquals(1, messages.numberIndex());
        tap(GameAction.CONFIRM);

        interpreter.start(program(cmd(0, 103, 0, array(5, 1)), cmd(1, 0, 0, null)), 1, 1);
        interpreter.update(0f);
        tap(GameAction.LEFT);
        assertEquals(0, messages.numberIndex());
    }

    // ---------------------------------------------------------------- helpers

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
                array.addChild(new JsonValue(((Integer) value).longValue()));
            } else {
                array.addChild(new JsonValue(String.valueOf(value)));
            }
        }
        return array;
    }
}
