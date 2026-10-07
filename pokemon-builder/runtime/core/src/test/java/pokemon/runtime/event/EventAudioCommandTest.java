package pokemon.runtime.event;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.data.AudioManifestData;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.8 regression: Play BGM / BGS / ME / SE (241/245/249/250) carry one
 * {@code RPG::AudioFile} ({@code name}, {@code volume}, {@code pitch}) at
 * parameter 0. The first version only accepted a bare string, so every door and
 * stairs SE was dropped without a word.
 */
class EventAudioCommandTest {

    private final List<String> calls = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final GameState state = new GameState();
    private AudioManager audio;
    private EventInterpreter interpreter;

    /** Minimal Sound/Music pair that records what the manager asked for. */
    private void setUpAudio(File dataRoot) {
        audio = new AudioManager();
        AudioManifestData manifest = new AudioManifestData();
        manifest.loaded = true;
        manifest.preset = "standard";
        for (String[] entry : new String[][] {
                {"SE", "Door exit", "audio/SE/Door exit.ogg"},
                {"SE", "Door enter", "audio/SE/Door enter.ogg"},
                {"BGM", "Mom Theme", "audio/BGM/Mom Theme.ogg"},
                {"BGS", "Rain", "audio/BGS/Rain.ogg"},
                {"ME", "Victory", "audio/ME/Victory.ogg"},
        }) {
            AudioManifestData.Entry parsed = new AudioManifestData.Entry();
            parsed.type = entry[0];
            parsed.file = entry[2];
            manifest.entries.put(entry[1], parsed);
        }
        audio.attach(manifest, dataRoot);
        audio.setBackend(new AudioManager.Backend() {
            @Override
            public Music newMusic(File file) {
                calls.add("music:" + file.getName());
                return new Music() {
                    public void play() { }
                    public void pause() { }
                    public void stop() { }
                    public boolean isPlaying() { return true; }
                    public void setLooping(boolean looping) { }
                    public boolean isLooping() { return true; }
                    public void setVolume(float volume) { calls.add("volume=" + volume); }
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
                calls.add("sound:" + file.getName());
                return new Sound() {
                    public long play() { return 1L; }
                    public long play(float volume) { calls.add("play=" + volume); return 1L; }
                    public long play(float volume, float pitch, float pan) {
                        calls.add("play=" + volume + "@" + pitch);
                        return 1L;
                    }
                    public long loop() { return 1L; }
                    public long loop(float volume) { return 1L; }
                    public long loop(float volume, float pitch, float pan) { return 1L; }
                    public void stop() { }
                    public void pause() { }
                    public void resume() { }
                    public void dispose() { }
                    public void stop(long soundId) { }
                    public void pause(long soundId) { }
                    public void resume(long soundId) { }
                    public void setLooping(long soundId, boolean looping) { }
                    public void setPitch(long soundId, float pitch) { }
                    public void setVolume(long soundId, float volume) { }
                    public void setPan(long soundId, float pan, float volume) { }
                };
            }
        });
    }

    @BeforeEach
    void setUp() throws Exception {
        calls.clear();
        warnings.clear();
        File dataRoot = Files.createTempDirectory("event-audio").toFile();
        for (String path : new String[] {
                "audio/SE/Door exit.ogg", "audio/SE/Door enter.ogg",
                "audio/BGM/Mom Theme.ogg", "audio/BGS/Rain.ogg", "audio/ME/Victory.ogg"}) {
            File file = new File(dataRoot, path);
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), "fake".getBytes(StandardCharsets.UTF_8));
        }
        setUpAudio(dataRoot);
        interpreter = new EventInterpreter(state, new MessageService(), new InputManager(),
                audio, id -> null, null, new PictureService(), warnings::add);
    }

    @Test
    @DisplayName("Play SE reads name, volume and pitch out of the audio file object")
    void playsSeFromAudioFileObject() {
        interpreter.start(program(cmd(0, 250, 0, array(audioFile("Door exit", 80, 100)))), 3, 1);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertEquals(List.of("sound:Door exit.ogg", "play=0.8@1.0"), calls);
    }

    @Test
    @DisplayName("Play BGM, BGS and ME use the same object")
    void playsOtherAudioCommands() {
        interpreter.start(program(
                cmd(0, 241, 0, array(audioFile("Mom Theme", 70, 100))),
                cmd(1, 245, 0, array(audioFile("Rain", 60, 100))),
                cmd(2, 249, 0, array(audioFile("Victory", 100, 100)))), 3, 1);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        // BGM and BGS stream (Music), the ME is a preloaded sample (Sound).
        assertEquals(2, calls.stream().filter(call -> call.startsWith("music:")).count());
        assertTrue(calls.contains("sound:Victory.ogg"), calls.toString());
        assertFalse(calls.contains("sound:Door exit.ogg"));
    }

    @Test
    @DisplayName("a sound command without a file name is reported instead of failing silently")
    void complainsAboutEmptySound() {
        interpreter.start(program(cmd(0, 250, 0, array(audioFile("", 80, 100)))), 3, 1);
        interpreter.update(0f);
        assertTrue(calls.isEmpty());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("Play SE without an audio file"), warnings.toString());
    }

    @Test
    @DisplayName("R12: Change Battle BGM (132) / ME (133) set $PokemonGlobal.nextBattleBGM")
    void changeBattleAudioCommands() {
        // PField_Field:882-890 keeps the parameter in the global metadata and
        // plays nothing now; the next battle start reads it back.
        interpreter.start(program(
                cmd(0, 132, 0, array(audioFile("Battle roaming", 100, 100))),
                cmd(1, 133, 0, array(audioFile("Roaming ME", 100, 100)))), 3, 1);
        interpreter.update(0f);
        assertEquals(InterpreterState.FINISHED, interpreter.state());
        assertEquals("Battle roaming", state.nextBattleBGM());
        assertEquals("Roaming ME", state.nextBattleME());
        assertEquals(List.of(), calls, "neither command plays anything");
    }

    @Test
    @DisplayName("R12: an empty battle audio parameter clears it, like assigning nil")
    void changeBattleAudioClear() {
        state.nextBattleBGM("Battle roaming");
        interpreter.start(program(cmd(0, 132, 0, array(audioFile("", 100, 100)))), 3, 1);
        interpreter.update(0f);
        assertNull(state.nextBattleBGM());
    }

    /** {"class":"RPG::AudioFile","name":..,"volume":..,"pitch":..} */
    private static JsonValue audioFile(String name, int volume, int pitch) {
        return new JsonReader().parse("{\"class\":\"RPG::AudioFile\",\"name\":\"" + name
                + "\",\"volume\":" + volume + ",\"pitch\":" + pitch + "}");
    }

    private static Array<EventCommand> program(EventCommand... commands) {
        Array<EventCommand> program = new Array<>();
        for (EventCommand command : commands) {
            program.add(command);
        }
        return program;
    }

    /** Command parameters are always an array; index 0 holds the audio file. */
    private static JsonValue array(JsonValue... values) {
        JsonValue array = new JsonValue(JsonValue.ValueType.array);
        for (JsonValue value : values) {
            array.addChild(value);
        }
        return array;
    }

    private static EventCommand cmd(int index, int code, int indent, JsonValue parameters) {
        EventCommand command = new EventCommand();
        command.index = index;
        command.code = code;
        command.indent = indent;
        command.parameters = parameters;
        return command;
    }
}
