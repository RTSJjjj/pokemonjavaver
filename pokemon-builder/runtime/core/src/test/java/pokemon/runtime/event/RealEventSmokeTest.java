package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.TestData;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Runs a real event of the exported project against the interpreter and reports what it said and what it skipped. */
class RealEventSmokeTest {
    static final class Transcript {
        final List<String> lines = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        boolean finished;
        GameState state;
    }

    static Transcript play(int mapId, int eventId, int page, int[] choices, int maxFrames) throws Exception {
        File root = TestData.runtimeDataRoot();
        Assumptions.assumeTrue(root != null, "runtime data (generated/) not available");
        GameDatabase db = GameDatabase.load(root.getPath(), null);
        MapData map = db.map(mapId);
        Array<EventCommand> commands = null;
        for (MapData.EventData event : map.events) {
            if (event.id == eventId) commands = event.pages.get(page).commands;
        }
        assertNotNull(commands);
        Transcript out = new Transcript();
        GameState state = new GameState();
        out.state = state;
        state.enterMap(mapId, 0, 0);
        PictureService pics = new PictureService();
        MessageService messages = new MessageService();
        InputManager input = new InputManager();
        EventInterpreter interpreter = new EventInterpreter(state, messages, input, null, id -> null, null,
                pics, out.warnings::add);
        File irFile = new File(root, "scripts/ir.json");
        if (irFile.isFile()) interpreter.attachScriptIr(ScriptIr.load(irFile));
        interpreter.attachInventory(state.inventory());
        interpreter.attachPbs(PbsData.parse(new File(root, "pbs")));
        interpreter.start(commands, mapId, eventId);
        int choiceIndex = 0;
        String last = "";
        for (int frame = 0; frame < maxFrames && interpreter.running(); frame++) {
            input.beginFrame();
            if (messages.choiceMode()) {
                if (choiceIndex < choices.length) {
                    int want = choices[choiceIndex++];
                    for (int i = 0; i < want; i++) { messages.moveCursor(1); }
                    out.lines.add("CHOICE " + want + " of " + messages.choices());
                }
                input.press(GameAction.CONFIRM);
            } else if (messages.visible() && messages.waiting()) {
                StringBuilder shown = new StringBuilder();
                for (PictureService.Picture pic : pics.values()) {
                    shown.append(" [pic ").append(pic.id).append(' ').append(pic.name).append(" @").append(pic.x).append(',')
                            .append(pic.y).append(" o").append(pic.opacity).append(']');
                }
                String text = String.join("/", messages.lines()) + shown;
                if (!text.equals(last)) { out.lines.add(text); last = text; }
                input.press(GameAction.CONFIRM);
            }
            pics.update(1f / 40f);
            interpreter.update(1f / 40f);
            input.endFrame();
            input.release(GameAction.CONFIRM);
        }
        out.finished = !interpreter.running();
        return out;
    }

    /**
     * Ad-hoc inspection: {@code SMOKE=map,event,page,choice0,choice1,...} writes the transcript to the file named by
     * {@code SMOKE_OUT} (UTF-8). Does nothing without the variable.
     */
    @Test
    @DisplayName("diagnostic transcript of one real event (SMOKE=map,event,page,choices...)")
    void transcript() throws Exception {
        String spec = System.getenv("SMOKE");
        Assumptions.assumeTrue(spec != null && !spec.isEmpty(), "SMOKE not set");
        String[] parts = spec.split(",");
        int[] choices = new int[Math.max(0, parts.length - 3)];
        for (int i = 3; i < parts.length; i++) choices[i - 3] = Integer.parseInt(parts[i].trim());
        Transcript t = play(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                Integer.parseInt(parts[2].trim()), choices, 20000);
        List<String> out = new ArrayList<>();
        out.add("finished=" + t.finished + " var100=" + t.state.variables().get(100) + " sw35=" + t.state.switches().get(35)
                + " sw199=" + t.state.switches().get(199) + " sw42=" + t.state.switches().get(42));
        for (String line : t.lines) out.add("| " + line);
        for (String w : t.warnings) out.add("! " + w);
        java.nio.file.Files.write(new File(System.getenv().getOrDefault("SMOKE_OUT", "smoke.txt")).toPath(), out,
                java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("the Intro event runs to its end")
    void intro() throws Exception {
        Transcript t = play(1, 1, 0, new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 4000);
        assertNotNull(t);
    }
}
