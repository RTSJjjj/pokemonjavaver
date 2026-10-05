package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.battle.BattlePort;
import pokemon.runtime.battle.BattleResult;
import pokemon.runtime.battle.WildEncounters;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.state.GameState;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cross-frame RMXP event interpreter (project3 sections 16-20, 27, 29).
 *
 * <p>One command list runs at a time; CALL_COMMON_EVENT pushes another frame
 * and control returns to the caller when that frame ends. Text and choices
 * block in WAIT_MESSAGE and only continue when the message service reports a
 * confirm, so an event never finishes inside one update.</p>
 *
 * <p>Commands outside the R6 first batch are logged and skipped instead of
 * being ignored silently: the log names the code and the command index so the
 * later stages (R6.3 map commands, R6.4 screen / picture commands, R7 scripts,
 * R9 audio) can pick them up one by one.</p>
 */
public final class EventInterpreter {

    /** Malformed data protection: at most this many commands per update. */
    private static final int MAX_STEPS_PER_UPDATE = 1000;
    /** Nested CALL_COMMON_EVENT frames; deeper calls are refused and logged. */
    private static final int MAX_CALL_DEPTH = 16;

    /** Looks up a common event's command list (GameDatabase in the game). */
    public interface CommonEventSource {
        Array<EventCommand> commonEvent(int id);
    }

    /** Where interpreter diagnostics go; the game passes PokemonGame::log. */
    public interface WarningLog {
        void warn(String message);
    }

    private static final Pattern SCRIPT_SWITCH =
            Pattern.compile("^\\$game_switches\\[(\\d+)\\]\\s*(?:==\\s*(true|false))?$");
    private static final Pattern SCRIPT_VARIABLE =
            Pattern.compile("^\\$game_variables\\[(\\d+)\\]\\s*(==|>=|<=|>|<|!=)\\s*(-?\\d+)$");
    /** Essentials {@code get_character(N).onEvent?}: the player stands on N. */
    private static final Pattern SCRIPT_ON_EVENT =
            Pattern.compile("^get_character\\(\\s*(-?\\d+)\\s*\\)\\.onEvent\\?$");
    /** Essentials {@code isTempSwitchOn?("A")} / {@code isTempSwitchOff?("A")}. */
    private static final Pattern SCRIPT_TEMP_SWITCH =
            Pattern.compile("^isTempSwitch(On|Off)\\?\\(\\s*\"([A-D])\"\\s*\\)$");

    private final GameState state;
    private final MessageService messages;
    private final InputManager input;
    private final AudioManager audio;
    private final CommonEventSource commonEvents;
    private final MapPort mapPort;
    private final PictureService pictures;
    private ScreenEffects screenEffects;
    private ScriptIr scriptIr;
    private pokemon.runtime.state.Inventory inventory;
    /** P0c: PBS data used by the Pokemon construction IR commands. */
    private PbsData pbs;
    /** P2: runs wild / trainer battles started by event scripts. */
    private BattlePort battlePort;
    /** P2: the random source for triggered encounters (RockSmash). */
    private final java.util.Random encounterRandom = new java.util.Random();
    /**
     * P0c: local variables of the Pokemon construction scripts
     * ({@code p = pbGenPkmn(...)}). They live for one compiled SEQUENCE, which
     * is one event script block.
     */
    private final ObjectMap<String, Object> scriptLocals = new ObjectMap<>();
    private final WarningLog log;

    private final Array<Frame> stack = new Array<>();
    private final Array<Integer> activeCommonEvents = new Array<>();
    private final Array<String> rawLines = new Array<>();

    private InterpreterState interpreterState = InterpreterState.FINISHED;
    private int mapId = -1;
    private int eventId = -1;
    private float waitTimer;
    /**
     * R6.19: remaining steps of a compiled SEQUENCE (several statements in one
     * Ruby block). A waiting step leaves them queued until the wait ends.
     */
    private JsonValue pendingSteps;
    private int pendingStepIndex;

    private Array<String> messageLines = new Array<>();
    private int messagePage;
    private boolean messageWaits = true;
    private String messageSpeaker;
    /** R6.31: the current message's window size (\l[n]) and skin (\w[skin]). */
    private int messageLinesPerPage = MessageService.LINES_PER_PAGE;
    private String messageSkin;
    /** R6.31: a 102 immediately after the text keeps the page on screen. */
    private boolean choicesFollow;

    private int choiceResult = Integer.MIN_VALUE;
    /** Event watched by a running Wait for Move's Completion (-1 = player). */
    /** Command positions already reported, so looping events cannot flood the log. */
    private final com.badlogic.gdx.utils.IntSet reportedOnce = new com.badlogic.gdx.utils.IntSet();
    /** Message window width in half-width units (R6.31): the 592px content
     *  area of the 672px speech frame at the 22px pixel font. */
    private int messageColumns = 54;

    public EventInterpreter(GameState state, MessageService messages, InputManager input,
                            AudioManager audio, CommonEventSource commonEvents, MapPort mapPort,
                            PictureService pictures, WarningLog log) {
        this.state = state;
        this.messages = messages;
        this.input = input;
        this.audio = audio;
        this.commonEvents = commonEvents;
        this.mapPort = mapPort;
        this.pictures = pictures;
        this.log = log == null ? message -> { } : log;
    }

    /** Starts one event page; replaces whatever was running. */
    public void start(Array<EventCommand> commands, int mapId, int eventId) {
        stop();
        this.mapId = mapId;
        this.eventId = eventId;
        stack.add(new Frame(new EventProgram(commands), -1));
        interpreterState = InterpreterState.RUNNING;
    }

    /** Aborts the current event (map change, event erased, new interaction). */
    public void stop() {
        stack.clear();
        activeCommonEvents.clear();
        pendingSteps = null;
        pendingStepIndex = 0;
        scriptLocals.clear();
        // Never close the shared message window here: a parallel event restarts
        // its own interpreter every few frames, and that used to dismiss the
        // message the player was reading (reported as "all messages auto
        // confirm at once"). The map screen closes a stale window instead.
        rawLines.clear();
        messageLines = new Array<>();
        messagePage = 0;
        messageWaits = true;
        messageSpeaker = null;
        messageLinesPerPage = MessageService.LINES_PER_PAGE;
        messageSkin = null;
        choicesFollow = false;
        choiceResult = Integer.MIN_VALUE;
        waitTimer = 0f;
        interpreterState = InterpreterState.FINISHED;
    }

    public boolean running() {
        return interpreterState != InterpreterState.FINISHED;
    }

    public InterpreterState state() {
        return interpreterState;
    }

    public MessageService messages() {
        return messages;
    }

    public void messageColumns(int columns) {
        this.messageColumns = Math.max(1, columns);
    }

    /** Optional screen effect service (Fade / Flash / Shake, R6.4b). */
    public void attachScreenEffects(ScreenEffects effects) {
        this.screenEffects = effects;
    }

    /** IR compiled by the Builder (R7.1); without it script blocks stay skipped. */
    public void attachScriptIr(ScriptIr ir) {
        this.scriptIr = ir;
    }

    /** Item container used by GIVE_ITEM / REMOVE_ITEM IR (R7.2b). */
    public void attachInventory(pokemon.runtime.state.Inventory items) {
        this.inventory = items;
    }

    /**
     * P0c: PBS data (species / moves / natures) used by the Pokemon
     * construction IR commands ({@code POKEMON_CREATE}, {@code POKEMON_CALL},
     * {@code POKEMON_SET}). Without it those steps are logged and skipped.
     */
    public void attachPbs(PbsData data) {
        this.pbs = data;
    }

    /** P2: the battle runtime the WILD_BATTLE / TRAINER_BATTLE steps call. */
    public void attachBattlePort(BattlePort port) {
        this.battlePort = port;
    }

    /** Called once per frame by the map screen. */
    public void update(float delta) {
        if (interpreterState == InterpreterState.FINISHED) {
            return;
        }
        advanceWait(delta);
        if (interpreterState == InterpreterState.RUNNING) {
            execute();
        }
    }

    // ------------------------------------------------------------------
    // Running the command list
    // ------------------------------------------------------------------

    private void execute() {
        int steps = 0;
        while (interpreterState == InterpreterState.RUNNING
                && (pendingSteps != null || stack.size > 0)) {
            if (pendingSteps != null && pendingStepIndex >= pendingSteps.size) {
                // The whole sequence played; only now may the frame finish.
                pendingSteps = null;
                pendingStepIndex = 0;
                if (stack.size == 0) {
                    interpreterState = InterpreterState.FINISHED;
                }
                continue;
            }
            if (pendingSteps != null) {
                if (++steps > MAX_STEPS_PER_UPDATE) {
                    log.warn("event interpreter paused after " + MAX_STEPS_PER_UPDATE
                            + " commands in one update (possible loop)");
                    break;
                }
                executeIrStep(pendingSteps.get(pendingStepIndex++));
                continue;
            }
            EventProgram program = stack.peek().program;
            if (program.finished()) {
                popFrame();
                continue;
            }
            if (++steps > MAX_STEPS_PER_UPDATE) {
                log.warn("event interpreter paused after " + MAX_STEPS_PER_UPDATE
                        + " commands in one update (possible loop) at index " + program.index());
                break;
            }
            executeCommand(program, program.current());
        }
    }

    private void popFrame() {
        Frame frame = stack.pop();
        if (frame.commonEventId >= 0) {
            activeCommonEvents.removeValue(frame.commonEventId, false);
        }
        if (stack.size == 0) {
            interpreterState = InterpreterState.FINISHED;
        }
    }

    private void executeCommand(EventProgram program, EventCommand command) {
        if (Boolean.getBoolean("pokemon.debug.flow")) {
            System.out.println("[cmd] code=" + command.code + " index=" + command.index
                    + " event=" + eventId + " map=" + mapId);
        }
        switch (command.code) {
            case 0:
            case 104: // Change Text Options: cosmetics only in R6.
            case 108: // Comment
            case 401: // Text line already consumed by the 101 handler.
            case 412: // Branch end
                program.advance();
                break;
            case 404: // End of choices: the selection is no longer needed.
                choiceResult = Integer.MIN_VALUE;
                program.advance();
                break;
            case 101:
                showText(program);
                break;
            case 102:
                showChoices(program);
                break;
            case 106:
                waitFrames(program, intParam(command.parameters, 0, 0));
                break;
            case 111:
                conditionalBranch(program, command);
                break;
            case 115:
                program.finish();
                break;
            case 116:
                callCommonEvent(program, command);
                break;
            case 121:
                controlSwitches(program, command);
                break;
            case 122:
                controlVariables(program, command);
                break;
            case 123:
                controlSelfSwitch(program, command);
                break;
            case 201:
                transferPlayer(program, command);
                break;
            case 202:
                setEventLocation(program, command);
                break;
            case 203:
                scrollMap(program, command);
                break;
            case 204:
                changeMapSettings(program, command);
                break;
            case 206:
                changeFogOpacity(program, command);
                break;
            case 207:
                showAnimation(program, command);
                break;
            case 208:
                changeTransparentFlag(program, command);
                break;
            case 209:
                setMoveRoute(program, command);
                break;
            case 210:
                waitForMovement(program, command);
                break;
            case 221:
                fadeScreen(program, command, true);
                break;
            case 222:
                fadeScreen(program, command, false);
                break;
            case 223:
                tintScreen(program, command);
                break;
            case 224:
                flashScreen(program, command);
                break;
            case 225:
                shakeScreen(program, command);
                break;
            case 234:
                log.warn("Change Picture Color Tone (234) is not implemented yet; skipped");
                program.advance();
                break;
            case 355: // Script (single line) - compiled to IR by the Builder (R7)
            case 655: // End of a multi line script
                runScriptBlock(program, command);
                break;
            case 509:
                // The Builder also emits every command of a Move Route as 509
                // right after its 209; those belong to that route (the map side
                // plays them) and are not scripts.
                if (isMoveRouteCommand(command)) {
                    program.advance();
                } else {
                    runScriptBlock(program, command);
                }
                break;
            case 231:
                showPicture(program, command);
                break;
            case 232:
                movePicture(program, command);
                break;
            case 235:
                erasePicture(program, command);
                break;
            case 241:
                playAudio(program, command, "BGM");
                break;
            case 242:
                if (audio != null) {
                    audio.stopBgm();
                }
                program.advance();
                break;
            case 245:
                playAudio(program, command, "BGS");
                break;
            case 249:
                playAudio(program, command, "ME");
                break;
            case 250:
                playAudio(program, command, "SE");
                break;
            case 402:
                choiceBranch(program, command);
                break;
            case 403:
                cancelBranch(program);
                break;
            case 411:
                elseBranch(program);
                break;
            default:
                log.warn("event command " + command.code + " (index " + command.index
                        + ") is not implemented yet; skipped");
                program.advance();
                break;
        }
    }

    private void advanceWait(float delta) {
        switch (interpreterState) {
            case WAIT_TIME:
                waitTimer -= Math.max(0f, delta);
                if (waitTimer <= 0f) {
                    interpreterState = InterpreterState.RUNNING;
                }
                break;
            case WAIT_MESSAGE:
                updateMessage();
                break;
            case WAIT_MOVEMENT:
                if (mapPort == null || !mapPort.anyRouteActive()) {
                    interpreterState = InterpreterState.RUNNING;
                }
                break;
            case WAIT_INPUT:
                if (input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL)) {
                    interpreterState = InterpreterState.RUNNING;
                }
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------
    // Show Text / Show Choices (sections 18-20)
    // ------------------------------------------------------------------

    private void showText(EventProgram program) {
        rawLines.clear();
        collectText(program.current());
        program.advance();
        while (!program.finished() && program.current().code == 401) {
            collectText(program.current());
            program.advance();
        }
        MessageText.Parsed parsed = MessageText.parse(rawLines, state, messageColumns);
        messageLines = parsed.lines;
        messagePage = 0;
        messageWaits = parsed.waitForInput;
        messageSpeaker = parsed.speaker;
        messageLinesPerPage = parsed.lineCount > 0 ? parsed.lineCount : MessageService.LINES_PER_PAGE;
        messageSkin = parsed.skin;
        // R6.31: RMXP shows a following Show Choices while the last text page
        // is still on screen, so that page must not wait for its own confirm.
        choicesFollow = !program.finished() && program.current().code == 102;
        showMessagePage();
        interpreterState = InterpreterState.WAIT_MESSAGE;
    }

    private void collectText(EventCommand command) {
        if (command.code == 101) {
            JsonValue text = command.parameter(0);
            if (text != null && text.isString()) {
                rawLines.add(text.asString());
            }
            return;
        }
        if (command.parameters == null) {
            return;
        }
        for (JsonValue value = command.parameters.child; value != null; value = value.next) {
            if (value.isString()) {
                rawLines.add(value.asString());
            }
        }
    }

    private void showMessagePage() {
        int start = messagePage * messageLinesPerPage;
        Array<String> page = new Array<>();
        for (int i = start; i < Math.min(start + messageLinesPerPage, messageLines.size); i++) {
            page.add(messageLines.get(i));
        }
        boolean lastPage = start + messageLinesPerPage >= messageLines.size;
        boolean pageWaits = messageWaits && !(choicesFollow && lastPage);
        messages.showLines(page, messageSpeaker, pageWaits, messageLinesPerPage, messageSkin);
    }

    private void updateMessage() {
        if (!messages.visible()) {
            interpreterState = InterpreterState.RUNNING;
            return;
        }
        if (messages.choiceMode()) {
            if (input.wasPressed(GameAction.UP)) {
                messages.moveCursor(-1);
            } else if (input.wasPressed(GameAction.DOWN)) {
                messages.moveCursor(1);
            }
            if (input.wasPressed(GameAction.CONFIRM)) {
                messages.confirm();
                choiceResult = messages.selected();
                interpreterState = InterpreterState.RUNNING;
            } else if (input.wasPressed(GameAction.CANCEL)) {
                messages.cancel();
                if (!messages.visible()) {
                    choiceResult = -1;
                    interpreterState = InterpreterState.RUNNING;
                }
            }
            return;
        }
        if (!messages.waiting()) { // "\^" page: continue without a confirm
            nextMessagePage();
            return;
        }
        if (input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL)) {
            messages.confirm();
            nextMessagePage();
        }
    }

    private void nextMessagePage() {
        messagePage++;
        if (messagePage * messageLinesPerPage >= messageLines.size) {
            if (choicesFollow) {
                // R6.31: keep the last page visible - the 102 command opens
                // the choice window on top of it, like RMXP.
                interpreterState = InterpreterState.RUNNING;
            } else {
                messages.close();
                interpreterState = InterpreterState.RUNNING;
            }
        } else {
            showMessagePage();
        }
    }

    private void showChoices(EventProgram program) {
        JsonValue parameters = program.current().parameters;
        Array<String> options = new Array<>();
        JsonValue list = parameters == null ? null : parameters.get(0);
        if (list != null && list.isArray()) {
            for (JsonValue option = list.child; option != null; option = option.next) {
                options.add(MessageText.clean(option.asString(), state));
            }
        }
        if (options.size == 0) {
            log.warn("choice command without options at index " + program.current().index);
            program.advance();
            return;
        }
        choiceResult = Integer.MIN_VALUE;
        messages.showChoices(options, intParam(parameters, 1, 0));
        program.advance();
        interpreterState = InterpreterState.WAIT_MESSAGE;
    }

    private void choiceBranch(EventProgram program, EventCommand command) {
        int option = intParam(command.parameters, 0, -1);
        if (choiceResult == option) {
            program.advance();
        } else {
            program.skipBlock();
        }
    }

    private void cancelBranch(EventProgram program) {
        if (choiceResult == -1) {
            program.advance();
        } else {
            program.skipBlock();
        }
    }

    // ------------------------------------------------------------------
    // Control Switches / Variables / Self Switches (section 28)
    // ------------------------------------------------------------------

    private void controlSwitches(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        int start = Math.max(1, intParam(p, 0, 0));
        int end = intParam(p, 1, start);
        boolean on = intParam(p, 2, 0) == 0;
        for (int id = start; id <= end; id++) {
            state.switches().set(id, on);
        }
        program.advance();
    }

    private void controlVariables(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        int start = Math.max(1, intParam(p, 0, 0));
        int end = intParam(p, 1, start);
        int operation = intParam(p, 2, 0);
        int operandType = intParam(p, 3, 0);
        int operand;
        if (operandType == 0) {
            operand = intParam(p, 4, 0);
        } else if (operandType == 1) {
            operand = state.variables().get(Math.max(1, intParam(p, 4, 1)));
        } else {
            unsupported(command, "variable operand type " + operandType + " (needs party / script data)");
            program.advance();
            return;
        }
        for (int id = start; id <= end; id++) {
            state.variables().set(id, applyOperation(operation, state.variables().get(id), operand));
        }
        program.advance();
    }

    private void controlSelfSwitch(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        String channel = p != null && p.get(0) != null ? p.get(0).asString() : "A";
        boolean on = intParam(p, 1, 0) == 0;
        if (eventId < 0) {
            log.warn("Control Self Switch outside an event; ignored");
        } else {
            state.selfSwitches().set(mapId, eventId, channel, on);
        }
        program.advance();
    }

    /**
     * Transfer Player (201). RMXP ends the running event at this point and
     * waits for the map change, so the interpreter stops here and the screen
     * performs the switch at the end of the frame.
     */
    private void transferPlayer(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        // RMXP 201: [type, map id, x, y, direction, fade]; type 1 reads map/x/y
        // from variables instead of the literal values.
        int type = intParam(p, 0, 0);
        int mapId = intParam(p, 1, -1);
        int x = intParam(p, 2, 0);
        int y = intParam(p, 3, 0);
        int direction = intParam(p, 4, 0);
        int fade = intParam(p, 5, 0);
        if (type == 1) {
            mapId = state.variables().get(Math.max(1, mapId));
            x = state.variables().get(Math.max(1, x));
            y = state.variables().get(Math.max(1, y));
        }
        if (mapPort == null || mapId < 1) {
            log.warn("Transfer Player ignored (map port=" + (mapPort != null) + ", map id " + mapId + ")");
            program.advance();
            return;
        }
        program.finish();
        mapPort.transfer(mapId, x, y, direction, fade);
        interpreterState = InterpreterState.WAIT_TRANSFER;
    }

    /** Set Move Route (209): parse the route and hand it to the map side. */
    private void setMoveRoute(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        int target = intParam(p, 0, 0);
        MoveRoute route = MoveRoute.parse(p == null ? null : p.get(1));
        if (route.size() == 0) {
            log.warn("Set Move Route without commands at index " + command.index);
        } else if (mapPort != null) {
            mapPort.setMoveRoute(resolveCharacter(target), route);
        }
        program.advance();
    }

    /** Set Event Location (202): [event id, type, x, y, direction]. */
    private void setEventLocation(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        int target = resolveCharacter(intParam(p, 0, 0));
        int type = intParam(p, 1, 0);
        int x = intParam(p, 2, 0);
        int y = intParam(p, 3, 0);
        int direction = intParam(p, 4, 0);
        if (type == 1) { // designated with variables
            x = state.variables().get(Math.max(1, x));
            y = state.variables().get(Math.max(1, y));
        }
        if (mapPort == null) {
            log.warn("Set Event Location without a map port (index " + command.index + "); skipped");
        } else {
            mapPort.setEventLocation(target, x, y, direction);
        }
        program.advance();
    }

    /**
     * Change Transparent Flag (208). RMXP always changes the hero
     * ({@code $game_player.transparent}); the door and stairs scripts use it to
     * hide the player while they stand inside the doorway. Parameter 0 =
     * transparent on, 1 = off.
     */
    private void changeTransparentFlag(EventProgram program, EventCommand command) {
        boolean transparent = intParam(command.parameters, 0, 0) == 0;
        if (mapPort == null) {
            log.warn("Change Transparent Flag without a map port (index " + command.index + "); skipped");
        } else {
            mapPort.setTransparent(-1, transparent);
        }
        program.advance();
    }

    /**
     * Change Map Settings (204): the map side owns the panorama / fog state
     * (R6.22); types it does not draw are reported there, once.
     */
    private void changeMapSettings(EventProgram program, EventCommand command) {
        int type = intParam(command.parameters, 0, -1);
        if (mapPort == null) {
            log.warn("Change Map Settings without a map port (index " + command.index + "); skipped");
        } else {
            mapPort.changeMapSettings(type, command.parameters);
        }
        program.advance();
    }

    /**
     * Show Animation (207): {@code [character, animation_id, wait]}. RMXP's own
     * command_207 only assigns {@code character.animation_id} and continues -
     * the wait checkbox is not honoured by the engine either - so the event
     * keeps running while the sprite plays the animation.
     */
    private void showAnimation(EventProgram program, EventCommand command) {
        int target = resolveCharacter(intParam(command.parameters, 0, 0));
        int animationId = intParam(command.parameters, 1, 0);
        if (animationId > 0 && mapPort != null) {
            mapPort.showAnimation(target, animationId);
        }
        program.advance();
    }

    /** Change Fog Opacity (206): the map side owns the fog layer (R6.22). */
    private void changeFogOpacity(EventProgram program, EventCommand command) {
        int opacity = intParam(command.parameters, 1, 255);
        if (mapPort == null) {
            log.warn("Change Fog Opacity without a map port (index " + command.index + "); skipped");
        } else {
            mapPort.changeFogOpacity(opacity);
        }
        program.advance();
    }

    /** Scroll Map (203): direction, distance in tiles, speed 1..6. */
    private void scrollMap(EventProgram program, EventCommand command) {
        int direction = intParam(command.parameters, 0, 0);
        int distance = intParam(command.parameters, 1, 0);
        int speed = intParam(command.parameters, 2, 4);
        if (mapPort == null) {
            log.warn("Scroll Map without a map port (index " + command.index + "); skipped");
        } else {
            mapPort.scrollMap(direction, distance, speed);
        }
        program.advance();
    }

    /**
     * Wait for Move's Completion (210). The command carries no parameters: it
     * waits for the route the running event just started (that is what doors
     * use), so it resolves through the same character addressing as 209.
     */
    /**
     * Wait for Move's Completion (210). RMXP's command has no target parameter:
     * the interpreter waits until the player's forced route and every event's
     * forced route have finished (the door scripts depend on it).
     */
    private void waitForMovement(EventProgram program, EventCommand command) {
        program.advance();
        if (mapPort != null && mapPort.anyRouteActive()) {
            interpreterState = InterpreterState.WAIT_MOVEMENT;
        }
    }

    /** Fade Out (221) / Fade In (222): duration in frames, then optionally wait. */
    private void fadeScreen(EventProgram program, EventCommand command, boolean out) {
        int duration = intParam(command.parameters, 0, 20);
        if (screenEffects != null) {
            screenEffects.fade(duration, out);
        }
        program.advance();
        if (intParam(command.parameters, 1, 0) == 1 && duration > 0) {
            waitTimer = duration / 20f;
            interpreterState = InterpreterState.WAIT_TIME;
        }
    }

    /**
     * Runs the IR of one script block (project3 sections 4, 21-25). Blocks the
     * Builder did not compile are reported; IR commands whose runtime service
     * does not exist yet (items, battles, shop) are reported the same way, so
     * the log always names what is missing instead of silently skipping.
     */
    private void runScriptBlock(EventProgram program, EventCommand command) {
        String id = command.scriptBlockId;
        // R15: RMXP merges a 355 and its following 655 continuations into ONE
        // script, and the Builder marks every command of that block with the
        // same scriptBlockId. The continuations must not run the block a second
        // time (the fixture golden test caught a doubled GIVE_ITEM).
        // L6c: the Builder also merges *consecutive 355s* into one block (a
        // trainer page's pbTrainerIntro + pbNoticePlayer share cmd0), so a
        // following 355 with the same id is a continuation of the same block
        // too - without this the "!" and the walk-up ran twice per trainer.
        program.advance();
        if (command.code == 355) {
            while (!program.finished() && program.current() != null
                    && java.util.Objects.equals(program.current().scriptBlockId, id)
                    && (program.current().code == 655
                        || (id != null && program.current().code == 355))) {
                program.advance();
            }
        }
        JsonValue ir = scriptIr == null ? null : scriptIr.command(id);
        if (ir == null) {
            log.warn("script block " + (id == null ? "(no id)" : id) + " at index " + command.index
                    + " has no IR yet; skipped");
            return;
        }
        // The block never comes back to this command, even when one of its
        // steps waits (SHOW_TEXT); R6.19 sequences resume from their own list.
        executeIrStep(ir);
    }

    /**
     * R6.19: runs one compiled script step. A SEQUENCE (several statements in
     * one Ruby block, such as two pbSetSelfSwitch calls or a quest chain) queues
     * its remaining steps; a waiting step leaves them pending until the wait
     * ends, so {@code pbMessage(...)\npbSetSelfSwitch(...)} also works.
     */
    private void executeIrStep(JsonValue ir) {
        String name = ir.getString("command", "");
        switch (name) {
            case "SEQUENCE": {
                JsonValue steps = ir.get("steps");
                if (steps == null || !steps.isArray() || steps.size == 0) {
                    log.warn("SEQUENCE without steps; skipped");
                } else {
                    // P0c: local variables are scoped to one compiled block.
                    scriptLocals.clear();
                    pendingSteps = steps;
                    pendingStepIndex = 0;
                }
                break;
            }
            case "SET_SELF_SWITCH": {
                // R6.19: pbSetSelfSwitch(event, channel, value[, mapid]) sets
                // *that* event's self switch - the IR carries the target, and
                // the running event is only the fallback. Reading it with the
                // type guards keeps older / malformed IR from crashing.
                int targetMap = irInt(ir, "mapId", mapId);
                int target = irInt(ir, "eventId", eventId);
                if (target < 0) {
                    target = eventId;
                }
                if (target < 0) {
                    log.warn("SET_SELF_SWITCH outside an event; skipped");
                } else {
                    state.selfSwitches().set(targetMap, target, irString(ir, "channel", "A"),
                            irBoolean(ir, "value", true));
                }
                break;
            }
            case "SET_TEMP_SWITCH":
                // Essentials temp switches (tsOn?/tsOff?) live for the map visit
                // and drive the 605 arrival door pages (R6.11).
                if (eventId < 0) {
                    log.warn("SET_TEMP_SWITCH outside an event; skipped");
                } else {
                    state.tempSwitches().set(mapId, eventId, irString(ir, "channel", "A"),
                            irBoolean(ir, "value", true));
                }
                break;
            case "ERASE_EVENT":
                if (mapPort == null || eventId < 0) {
                    log.warn("ERASE_EVENT without an event or map port; skipped");
                } else {
                    mapPort.eraseEvent(eventId);
                }
                break;
            // ---- plugin batch 1: quests, key items, following Pokemon ----
            case "ACTIVATE_QUEST": {
                String quest = ir.getString("quest", "");
                if (state.quests().activate(quest)) {
                    log.warn("quest activated: " + quest);
                }
                break;
            }
            case "ADVANCE_QUEST_TO_STAGE": {
                String quest = ir.getString("quest", "");
                JsonValue stage = ir.get("stage");
                int number = stage == null || !stage.isNumber()
                        ? state.quests().stage(quest) + 1 : stage.asInt();
                if (!state.quests().advance(quest, number)) {
                    log.warn("quest " + quest + " is not active; stage change ignored");
                }
                break;
            }
            case "COMPLETE_QUEST": {
                String quest = ir.getString("quest", "");
                if (state.quests().complete(quest)) {
                    log.warn("quest completed: " + quest);
                }
                break;
            }
            case "GIVE_KEY_ITEM": {
                String item = ir.getString("item", "");
                int amount = ir.getInt("amount", 1);
                if (inventory == null) {
                    log.warn("GIVE_KEY_ITEM " + item + " without an inventory; skipped");
                } else {
                    inventory.add(item, amount);
                    log.warn("received key item " + item + " x" + amount);
                }
                break;
            }
            case "TOGGLE_FOLLOWING_POKEMON": {
                // Essentials toggles $PokemonGlobal.followerToggled; the sprite
                // itself needs the party (stage 3), so only the flag changes here.
                String forced = ir.getString("forced", null);
                if ("on".equalsIgnoreCase(forced)) {
                    state.followerToggled(true);
                } else if ("off".equalsIgnoreCase(forced)) {
                    state.followerToggled(false);
                } else {
                    state.followerToggled(!state.followerToggled());
                }
                log.warn("following Pokemon " + (state.followerToggled() ? "on" : "off")
                        + " (sprite needs the party, stage 3)");
                break;
            }
            case "POKEMON_FOLLOW": {
                // pbPokemonFollow makes a specific character follow the player;
                // that is part of the follower sprite work (stage 3).
                log.warn("POKEMON_FOLLOW recorded but the follower sprite is not rendered yet");
                break;
            }
            case "GIVE_ITEM": {
                String item = ir.getString("item", "");
                int amount = ir.getInt("amount", 1);
                if (inventory == null) {
                    log.warn("GIVE_ITEM " + item + " without an inventory; skipped");
                } else {
                    inventory.add(item, amount);
                    log.warn("received " + item + " x" + amount);
                }
                break;
            }
            case "REMOVE_ITEM": {
                String item = ir.getString("item", "");
                if (inventory == null) {
                    log.warn("REMOVE_ITEM " + item + " without an inventory; skipped");
                } else {
                    inventory.remove(item, ir.getInt("amount", 1));
                }
                break;
            }
            case "SHOW_TEXT": {
                String text = showTextValue(ir);
                if (text == null || text.isEmpty()) {
                    log.warn("SHOW_TEXT without usable text; skipped");
                    break;
                }
                Array<String> raw = new Array<>();
                raw.add(text);
                MessageText.Parsed parsed = MessageText.parse(raw, state, messageColumns);
                messageLines = parsed.lines;
                messagePage = 0;
                messageWaits = parsed.waitForInput;
                messageSpeaker = parsed.speaker;
                showMessagePage();
                interpreterState = InterpreterState.WAIT_MESSAGE;
                return; // WAIT_MESSAGE owns the cursor now
            }
            // ---- handler batch 1: field audio / waits / cave transitions (R6.26) ----
            case "PLAY_SE": {
                String seName = ir.getString("name", "");
                int volume = ir.getInt("volume", 100);
                int pitch = ir.getInt("pitch", 100);
                if (audio == null) {
                    log.warn("PLAY_SE " + seName + " without an audio runtime; skipped");
                } else if (seName.isEmpty()) {
                    log.warn("PLAY_SE without a name; skipped");
                } else {
                    audio.playSe(seName, Math.max(0, Math.min(100, volume)),
                            Math.max(0, Math.min(200, pitch)));
                }
                break;
            }
            case "WAIT": {
                // pbWait(counts) counts frames at the project's 40 fps.
                float seconds = ir.getFloat("frames", 0f) / 40f;
                if (seconds > 0f) {
                    waitTimer = seconds;
                    interpreterState = InterpreterState.WAIT_TIME;
                    return; // the wait owns the cursor; pending steps resume after it
                }
                break;
            }
            case "CAVE_ENTRANCE": {
                boolean exiting = ir.getBoolean("exiting", false);
                if (mapPort == null) {
                    break;
                }
                float seconds = mapPort.caveEntrance(exiting);
                if (seconds > 0f) {
                    waitTimer = seconds;
                    interpreterState = InterpreterState.WAIT_TIME;
                    return;
                }
                break;
            }
            // ---- handler batch 2: cries / exclaim / boulders / plates (R6.30) ----
            case "PLAY_CRY": {
                // pbCryFile resolves the numeric cry file; a species without a
                // file stays silent, exactly like the Ruby version.
                if (audio == null) {
                    log.warn("PLAY_CRY without an audio runtime; skipped");
                    break;
                }
                JsonValue species = ir.get("species");
                String cry = species == null ? null
                        : species.isNumber() ? audio.resolveCry(species.asInt())
                        : audio.resolveCry(species.asString());
                if (cry != null) {
                    audio.playSe(cry, 100, 100);
                } else if (Boolean.getBoolean("pokemon.debug.flow")) {
                    log.warn("PLAY_CRY has no audio file for " + species + "; silent");
                }
                break;
            }
            case "EXCLAIM": {
                // pbExclaim waits until the bubble sprite is disposed; the tile
                // animation reports its exact duration from the animation data.
                int target = resolveCharacter(irInt(ir, "character", 0));
                int animationId = irInt(ir, "animationId", 3);
                if (mapPort == null) {
                    break;
                }
                float seconds = mapPort.exclaim(target, animationId);
                if (seconds > 0f) {
                    waitTimer = seconds;
                    interpreterState = InterpreterState.WAIT_TIME;
                    return;
                }
                break;
            }
            case "NOTICE_PLAYER": {
                // L6: pbNoticePlayer - facing check + exclaim, the player turns
                // and the event walks over; the port reports the combined wait.
                int target = resolveCharacter(irInt(ir, "character", 0));
                if (mapPort == null) {
                    break;
                }
                float seconds = mapPort.noticePlayer(target);
                if (seconds > 0f) {
                    waitTimer = seconds;
                    interpreterState = InterpreterState.WAIT_TIME;
                    return;
                }
                break;
            }
            case "SAVE_GAME": {
                // L6: pbSave - the project's quiet save; the port writes the
                // quick slot (the F5/F9 pair) and reports failures.
                if (mapPort != null && !mapPort.saveGame()) {
                    log.warn("pbSave could not write the save file");
                }
                break;
            }
            case "PUSH_BOULDER": {
                // pbPushThisBoulder only moves the boulder while the Strength
                // field move is active ($PokemonMap.strengthUsed).
                if (!state.pokemonMapStrengthUsed() || mapPort == null || eventId < 0) {
                    break;
                }
                float seconds = mapPort.pushBoulder(eventId);
                if (seconds > 0f) {
                    waitTimer = seconds;
                    interpreterState = InterpreterState.WAIT_TIME;
                    return;
                }
                break;
            }
            case "TOGGLE_PLATE_SWITCHES": {
                if (mapPort != null) {
                    mapPort.togglePlateSwitches();
                }
                break;
            }
            // ---- P0c: Pokemon construction scripts (locals + object methods) ----
            case "SET_VARIABLE": {
                // pbSet(id, value) is $game_variables[id] = value; the runtime
                // variables are integer only, so other values are logged.
                Object idValue = resolveIrValue(ir.get("id"));
                if (!(idValue instanceof Number) || ((Number) idValue).intValue() < 1) {
                    log.warn("SET_VARIABLE with an unusable id; skipped");
                    break;
                }
                int variableId = ((Number) idValue).intValue();
                Object value = resolveIrValue(ir.get("value"));
                if (value instanceof Number) {
                    state.variables().set(variableId, ((Number) value).intValue());
                } else if (value instanceof Boolean) {
                    state.variables().set(variableId, ((Boolean) value) ? 1 : 0);
                } else {
                    log.warn("SET_VARIABLE " + variableId + " ignores non-numeric value "
                            + value);
                }
                break;
            }
            case "LOCAL_SET": {
                setLocal(ir.getString("local", ""), resolveIrValue(ir.get("value")));
                break;
            }
            case "POKEMON_CREATE": {
                String local = ir.getString("local", "");
                String speciesName = ir.getString("species", "");
                int level = ir.getInt("level", 1);
                if (pbs == null) {
                    log.warn("POKEMON_CREATE without the PBS data; skipped");
                    setLocal(local, null);
                    break;
                }
                PbsData.Species species = pbs.species(speciesName);
                if (species == null) {
                    log.warn("POKEMON_CREATE unknown species " + speciesName + "; skipped");
                    setLocal(local, null);
                    break;
                }
                setLocal(local, new Pokemon(species, level, pbs));
                break;
            }
            case "POKEMON_CALL": {
                Pokemon pokemon = localPokemon(ir.getString("local", ""));
                if (pokemon == null) {
                    log.warn("POKEMON_CALL on an unknown local; skipped");
                    break;
                }
                pokemonCall(pokemon, ir);
                break;
            }
            case "POKEMON_SET": {
                Pokemon pokemon = localPokemon(ir.getString("local", ""));
                if (pokemon == null) {
                    log.warn("POKEMON_SET on an unknown local; skipped");
                    break;
                }
                pokemonSet(pokemon, ir.getString("property", ""), resolveIrValue(ir.get("value")));
                break;
            }
            case "PARTY_ADD": {
                Pokemon pokemon = localPokemon(ir.getString("local", ""));
                if (pokemon == null) {
                    log.warn("PARTY_ADD of an unknown local; skipped");
                    break;
                }
                boolean toParty = state.trainer().addToParty(pokemon);
                String label = pokemon.name == null ? pokemon.internalName : pokemon.name;
                log.warn("received " + label + (toParty ? " (party)" : " (box)"));
                break;
            }
            // ---- P1: party / bag / PokeCenter data handlers ----
            case "SET_POKEMON_CENTER": {
                // pbSetPokemonCenter stores the live map / player position.
                state.trainer().setPokemonCenter(mapId, state.playerX(), state.playerY(),
                        state.playerDirection());
                log.warn("PokeCenter set to map " + mapId + " (" + state.playerX() + ","
                        + state.playerY() + ")");
                break;
            }
            case "ADD_EGG": {
                String speciesName = ir.getString("species", "");
                if (pbs == null) {
                    log.warn("ADD_EGG without the PBS data; skipped");
                    break;
                }
                PbsData.Species species = pbs.species(speciesName);
                if (species == null) {
                    log.warn("ADD_EGG unknown species " + speciesName + "; skipped");
                    break;
                }
                Pokemon egg = new Pokemon(species, 1, pbs); // EGG_LEVEL
                egg.egg = true;
                egg.name = "神秘的蛋";
                egg.stepsToHatch = species.stepsToHatch;
                egg.hp = egg.maxHp();
                boolean toParty = state.trainer().addToParty(egg);
                log.warn("received an egg of " + speciesName
                        + (toParty ? " (party)" : " (box)"));
                break;
            }
            // ---- P2: wild / trainer battles ----
            case "WILD_BATTLE": {
                if (battlePort == null) {
                    log.warn("WILD_BATTLE without a battle runtime; skipped");
                    break;
                }
                String species = ir.getString("species", "");
                int level = ir.getInt("level", 5);
                BattleResult result = battlePort.wildBattle(species, level);
                log.warn("wild battle vs " + species + " L" + level + " -> "
                        + (result == null ? "not started" : result.outcome));
                break;
            }
            case "FREE_WILD_BATTLE": {
                if (battlePort == null) {
                    log.warn("FREE_WILD_BATTLE without a battle runtime; skipped");
                    break;
                }
                Pokemon foe = localPokemon(ir.getString("local", ""));
                if (foe == null) {
                    log.warn("FREE_WILD_BATTLE of an unknown local; skipped");
                    break;
                }
                BattleResult result = battlePort.freeWildBattle(foe);
                log.warn("free wild battle -> " + (result == null ? "not started" : result.outcome));
                break;
            }
            case "TRAINER_BATTLE": {
                if (battlePort == null || pbs == null) {
                    log.warn("TRAINER_BATTLE without a battle runtime or PBS data; skipped");
                    break;
                }
                PbsData.TrainerData trainer = pbs.trainer(
                        ir.getString("trainerType", ""), ir.getString("trainerName", ""),
                        ir.getInt("version", 0));
                if (trainer == null) {
                    log.warn("TRAINER_BATTLE unknown trainer; skipped");
                    break;
                }
                BattleResult result = battlePort.trainerBattle(trainer);
                log.warn("trainer battle vs " + trainer.name + " -> "
                        + (result == null ? "not started" : result.outcome));
                break;
            }
            case "ROCK_SMASH_ENCOUNTER": {
                if (battlePort == null || pbs == null) {
                    log.warn("ROCK_SMASH_ENCOUNTER without a battle runtime or PBS data; skipped");
                    break;
                }
                PbsData.EncounterMap map = pbs.encounterMap(mapId);
                WildEncounters.WildEncounter encounter = map == null ? null
                        : WildEncounters.pick(map, "RockSmash", encounterRandom);
                if (encounter == null) {
                    log.warn("ROCK_SMASH_ENCOUNTER: no RockSmash table on map " + mapId);
                    break;
                }
                BattleResult smash = battlePort.wildBattle(encounter.species, encounter.level);
                log.warn("rock smash encounter -> "
                        + (smash == null ? "not started" : smash.outcome));
                break;
            }
            default:
                log.warn("IR command " + name
                        + " needs a runtime service that does not exist yet; skipped");
                break;
        }
    }

    /**
     * 509 carrying an RPG::MoveCommand payload belongs to the preceding 209.
     * L6d: the payload marker decides, not the scriptBlockId - a stale block id
     * on a 509 (the Builder used to leak page-1 ids onto same-index commands of
     * later pages) must never replay a script like pbCaveEntrance.
     */
    private static boolean isMoveRouteCommand(EventCommand command) {
        JsonValue parameters = command.parameters;
        return parameters != null && parameters.isObject()
                && "RPG::MoveCommand".equals(parameters.getString("class", ""));
    }

    /**
     * Text of a SHOW_TEXT IR command. The compiler unwraps {@code _INTL(...)}
     * already; this also accepts a leftover {@code {script:"..."}} argument so
     * an unwrapped call never breaks the event (the quoted literal is used).
     */
    private static String showTextValue(JsonValue ir) {
        JsonValue value = ir.get("text");
        if (value == null) {
            return null;
        }
        if (value.isString()) {
            return value.asString();
        }
        JsonValue script = value.isObject() ? value.get("script") : null;
        if (script == null || !script.isString()) {
            return null;
        }
        String raw = script.asString();
        int start = raw.indexOf('"');
        int end = raw.lastIndexOf('"');
        return start >= 0 && end > start ? raw.substring(start + 1, end) : raw;
    }

    /** Flash Screen (224): [colour, strength, duration, wait]. */
    private void flashScreen(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        JsonValue colour = p == null ? null : p.get(0);
        float red = 1f, green = 1f, blue = 1f;
        if (colour != null && colour.isObject()) {
            red = colour.getFloat("red", 255f) / 255f;
            green = colour.getFloat("green", 255f) / 255f;
            blue = colour.getFloat("blue", 255f) / 255f;
        }
        int power = intParam(p, 1, 255);
        int duration = intParam(p, 2, 8);
        if (screenEffects != null) {
            screenEffects.flash(red, green, blue, power, duration);
        }
        program.advance();
        if (intParam(p, 3, 0) == 1 && duration > 0) {
            waitTimer = duration / 20f;
            interpreterState = InterpreterState.WAIT_TIME;
        }
    }

    /** Change Screen Color Tone (223): [Tone(red,green,blue,gray), duration, wait]. */
    private void tintScreen(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        JsonValue tone = p == null ? null : p.get(0);
        int duration = intParam(p, 1, 0);
        if (screenEffects != null && tone != null && tone.isObject()) {
            screenEffects.tint(tone.getFloat("red", 0f), tone.getFloat("green", 0f),
                    tone.getFloat("blue", 0f), tone.getFloat("gray", 0f), duration);
        } else {
            log.warn("Change Screen Color Tone without tone values (index "
                    + command.index + "); skipped");
        }
        program.advance();
        if (intParam(p, 2, 0) == 1 && duration > 0) {
            waitTimer = duration / 20f;
            interpreterState = InterpreterState.WAIT_TIME;
        }
    }

    /** Shake Screen (225): [strength, speed, duration, wait]. */
    private void shakeScreen(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        int power = intParam(p, 0, 5);
        int speed = intParam(p, 1, 5);
        int duration = intParam(p, 2, 20);
        if (screenEffects != null) {
            screenEffects.shake(power, speed, duration);
        }
        program.advance();
        if (intParam(p, 3, 0) == 1 && duration > 0) {
            waitTimer = duration / 20f;
            interpreterState = InterpreterState.WAIT_TIME;
        }
    }

    /**
     * RMXP character addressing used by 202/209/210: 0 = this event, -1 = the
     * player, anything else is an event id on the current map.
     */
    private int resolveCharacter(int target) {
        return target == 0 ? eventId : target;
    }

    // ------------------------------------------------------------------
    // Show / Move / Erase Picture (section 18, R6.4)
    //
    // This project's data carries one extra parameter at index 5 (visible in
    // every 231/232 sample: [number, name, origin, x, y, extra, zoomX, zoomY,
    // opacity, blend, wait]); reading zoom/opacity straight after y produces
    // zoom 0 / opacity 100 and an invisible picture, so the extra field is
    // skipped. Move Picture keeps its optional wait flag at index 10.
    // ------------------------------------------------------------------

    private void showPicture(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        String name = stringParam(p, 1, null);
        if (pictures != null && name != null && !name.isEmpty()) {
            pictures.show(intParam(p, 0, 1), name, intParam(p, 2, 0),
                    floatParam(p, 3, 0f), floatParam(p, 4, 0f),
                    floatParam(p, 6, 100f), floatParam(p, 7, 100f),
                    floatParam(p, 8, 255f), intParam(p, 9, 0));
        }
        program.advance();
    }

    private void movePicture(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        int duration = intParam(p, 1, 0);
        if (pictures != null) {
            pictures.move(intParam(p, 0, 1), duration, intParam(p, 2, 0),
                    floatParam(p, 3, 0f), floatParam(p, 4, 0f),
                    floatParam(p, 6, 100f), floatParam(p, 7, 100f),
                    floatParam(p, 8, 255f), intParam(p, 9, 0));
        }
        program.advance();
        if (intParam(p, 10, 0) == 1 && duration > 0) {
            waitTimer = duration / 20f; // "wait for completion" flag
            interpreterState = InterpreterState.WAIT_TIME;
        }
    }

    private void erasePicture(EventProgram program, EventCommand command) {
        if (pictures != null) {
            pictures.erase(intParam(command.parameters, 0, 1));
        }
        program.advance();
    }

    private static int applyOperation(int operation, int current, int operand) {
        switch (operation) {
            case 1:
                return current + operand;
            case 2:
                return current - operand;
            case 3:
                return current * operand;
            case 4:
                return operand == 0 ? current : current / operand;
            case 5:
                return operand == 0 ? current : current % operand;
            default:
                return operand;
        }
    }

    // ------------------------------------------------------------------
    // Conditional Branch (section 29)
    // ------------------------------------------------------------------

    private void conditionalBranch(EventProgram program, EventCommand command) {
        boolean result = evaluate(command);
        program.branchResult(program.indent(), result);
        if (result) {
            program.advance();
        } else {
            program.skipBlock();
        }
    }

    private void elseBranch(EventProgram program) {
        if (program.branchResult(program.indent())) {
            program.skipBlock();
        } else {
            program.advance();
        }
    }

    private boolean evaluate(EventCommand command) {
        JsonValue p = command.parameters;
        int type = intParam(p, 0, -1);
        switch (type) {
            case 0: { // Switch: params [0, switch id, 0 = on / 1 = off]
                int id = intParam(p, 1, 0);
                return state.switches().get(Math.max(1, id)) == (intParam(p, 2, 0) == 0);
            }
            case 1: { // Variable: params [1, variable id, operand type, value]
                int id = Math.max(1, intParam(p, 1, 0));
                int operandType = intParam(p, 2, 0);
                int value = intParam(p, 3, 0);
                if (operandType == 1) {
                    value = state.variables().get(Math.max(1, value));
                } else if (operandType != 0) {
                    unsupported(command, "variable operand type " + operandType);
                    return false;
                }
                return state.variables().get(id) >= value;
            }
            case 2: { // Self switch: params [2, channel, 0 = on / 1 = off]
                String channel = p != null && p.get(1) != null ? p.get(1).asString() : "A";
                if (eventId < 0) {
                    unsupported(command, "self switch without an event");
                    return false;
                }
                return state.selfSwitches().get(mapId, eventId, channel) == (intParam(p, 2, 0) == 0);
            }
            case 6: { // Character: params [6, character id, direction]
                int characterId = intParam(p, 1, -1);
                int direction = intParam(p, 2, 0);
                if (characterId == -1) {
                    return state.playerDirection() == direction;
                }
                unsupported(command, "character condition for event " + characterId);
                return false;
            }
            case 11: { // Button: params [11, button]
                GameAction action = buttonAction(intParam(p, 1, 0));
                if (action == null) {
                    unsupported(command, "button " + intParam(p, 1, 0));
                    return false;
                }
                return input.isDown(action);
            }
            case 12: { // Simple script condition (complex Ruby: script translator, R7)
                JsonValue script = p != null ? p.get(1) : null;
                return scriptCondition(script == null ? "" : script.asString(), command);
            }
            default:
                unsupported(command, "condition type " + type);
                return false;
        }
    }

    /**
     * Evaluates the script conditions the runtime can answer without a Ruby
     * translator: {@code $game_switches[n]}, {@code $game_variables[n]} and
     * self switches. Everything else is reported and treated as false until
     * R7 translates it (project3 section 29).
     */
    private boolean scriptCondition(String script, EventCommand command) {
        String text = script.trim();
        Matcher variable = SCRIPT_VARIABLE.matcher(text);
        if (variable.matches()) {
            int id = Math.max(1, Integer.parseInt(variable.group(1)));
            int value = Integer.parseInt(variable.group(3));
            return compare(variable.group(2), state.variables().get(id), value);
        }
        Matcher switchCondition = SCRIPT_SWITCH.matcher(text);
        if (switchCondition.matches()) {
            boolean value = state.switches().get(Math.max(1, Integer.parseInt(switchCondition.group(1))));
            if (switchCondition.group(2) == null) {
                return value;
            }
            return value == Boolean.parseBoolean(switchCondition.group(2));
        }
        // R6.11: the door arrival pages ask "did the player just arrive on me?".
        Matcher onEvent = SCRIPT_ON_EVENT.matcher(text);
        if (onEvent.matches()) {
            int target = resolveCharacter(Integer.parseInt(onEvent.group(1)));
            return mapPort != null && mapPort.playerOnCharacter(target);
        }
        Matcher tempSwitch = SCRIPT_TEMP_SWITCH.matcher(text);
        if (tempSwitch.matches()) {
            boolean on = state.tempSwitches().get(mapId, Math.max(0, eventId), tempSwitch.group(2));
            return "On".equals(tempSwitch.group(1)) == on;
        }
        unsupported(command, "script condition \"" + text + "\" (needs the script translator, R7)");
        return false;
    }

    private static boolean compare(String operator, int left, int right) {
        switch (operator) {
            case "==":
                return left == right;
            case ">=":
                return left >= right;
            case "<=":
                return left <= right;
            case ">":
                return left > right;
            case "<":
                return left < right;
            case "!=":
                return left != right;
            default:
                return false;
        }
    }

    private static GameAction buttonAction(int button) {
        switch (button) {
            case 2:
                return GameAction.DOWN;
            case 4:
                return GameAction.LEFT;
            case 6:
                return GameAction.RIGHT;
            case 8:
                return GameAction.UP;
            case 11:
                return GameAction.CONFIRM;
            case 12:
                return GameAction.CANCEL;
            case 13:
                return GameAction.MENU;
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------
    // Common events, waits and audio (sections 27, 18)
    // ------------------------------------------------------------------

    private void callCommonEvent(EventProgram program, EventCommand command) {
        int id = intParam(command.parameters, 0, -1);
        program.advance();
        if (commonEvents == null) {
            log.warn("CALL_COMMON_EVENT " + id + " without a common event source; skipped");
            return;
        }
        if (activeCommonEvents.contains(id, false) || stack.size >= MAX_CALL_DEPTH) {
            log.warn("refused recursive CALL_COMMON_EVENT " + id);
            return;
        }
        Array<EventCommand> commands = commonEvents.commonEvent(id);
        if (commands == null || commands.size == 0) {
            log.warn("common event " + id + " not found; skipped");
            return;
        }
        activeCommonEvents.add(id);
        stack.add(new Frame(new EventProgram(commands), id));
    }

    private void waitFrames(EventProgram program, int frames) {
        program.advance();
        waitTimer = frames / 20f;
        if (waitTimer <= 0f) {
            return;
        }
        interpreterState = InterpreterState.WAIT_TIME;
    }

    /**
     * Play BGM/BGS/ME/SE (241/245/249/250). RMXP stores one
     * {@code RPG::AudioFile} at parameter 0 ({@code name}, {@code volume},
     * {@code pitch}); reading it as a bare string silently dropped every event
     * sound, which is why doors and stairs had no SE.
     */
    private void playAudio(EventProgram program, EventCommand command, String kind) {
        JsonValue file = command.parameter(0);
        String id = null;
        int volume = 100;
        int pitch = 100;
        if (file != null && file.isObject()) {
            JsonValue name = file.get("name");
            if (name != null && name.isString()) {
                id = name.asString();
            }
            volume = file.getInt("volume", 100);
            pitch = file.getInt("pitch", 100);
        } else if (file != null && file.isString()) {
            id = file.asString();
            volume = intParam(command.parameters, 1, 100);
            pitch = intParam(command.parameters, 2, 100);
        }
        if (audio != null && id != null && !id.isEmpty()) {
            volume = Math.max(0, Math.min(100, volume));
            pitch = Math.max(0, Math.min(200, pitch));
            switch (kind) {
                case "BGM":
                    audio.playBgm(id, volume, pitch);
                    break;
                case "BGS":
                    audio.playBgs(id, volume, pitch);
                    break;
                case "ME":
                    audio.playMe(id, volume, pitch);
                    break;
                default:
                    audio.playSe(id, volume, pitch);
                    break;
            }
        } else {
            log.warn("Play " + kind + " without an audio file (index " + command.index + "); skipped");
        }
        program.advance();
    }

    private void unsupported(EventCommand command, String detail) {
        log.warn("event command " + command.code + " (index " + command.index + ") uses " + detail
                + "; treated as not met");
    }

    private static int intParam(JsonValue parameters, int position, int fallback) {
        JsonValue value = parameters == null ? null : parameters.get(position);
        return value == null ? fallback : value.asInt();
    }

    private static float floatParam(JsonValue parameters, int position, float fallback) {
        JsonValue value = parameters == null ? null : parameters.get(position);
        return value == null ? fallback : value.asFloat();
    }

    private static String stringParam(JsonValue parameters, int position, String fallback) {
        JsonValue value = parameters == null ? null : parameters.get(position);
        return value == null || !value.isString() ? fallback : value.asString();
    }

    // ------------------------------------------------------------------
    // P0c: Pokemon construction scripts (locals + object methods)
    // ------------------------------------------------------------------

    /**
     * Resolves one value of the Pokemon construction IR: a literal, an integer
     * array (IVs / EVs), a game variable ({@code pbGet(n)}), a local variable, a
     * field of a local Pokemon, or {@code $Trainer.pokemonCount}.
     */
    private Object resolveIrValue(JsonValue value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asInt();
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isArray()) {
            if (value.size > 0 && value.get(0).isNumber()) {
                return value.asIntArray();
            }
            String[] strings = new String[value.size];
            for (int i = 0; i < value.size; i++) {
                strings[i] = value.get(i).asString();
            }
            return strings;
        }
        if (value.isObject()) {
            if (value.has("variable")) {
                int id = value.getInt("variable", 0);
                return id < 1 ? 0 : state.variables().get(id);
            }
            if (value.has("trainerPokemonCount")) {
                return state.trainer().partyCount();
            }
            if (value.has("local")) {
                Object stored = scriptLocals.get(value.getString("local", ""), null);
                if (!value.has("property")) {
                    return stored;
                }
                return pokemonProperty(stored, value.getString("property", ""));
            }
        }
        return null;
    }

    /** Reads one field of a local Pokemon used as an argument ({@code p.name}). */
    private Object pokemonProperty(Object stored, String property) {
        if (!(stored instanceof Pokemon)) {
            return null;
        }
        Pokemon pokemon = (Pokemon) stored;
        switch (property) {
            case "name":
                return pokemon.name;
            case "ot":
                return pokemon.originalTrainer;
            case "level":
                return pokemon.level;
            case "species":
                return pokemon.internalName;
            case "item":
                return pokemon.item;
            case "shiny":
                return pokemon.shiny;
            case "form":
                return pokemon.form == null ? 0 : pokemon.form.form;
            default:
                return null;
        }
    }

    private void setLocal(String local, Object value) {
        if (local == null || local.isEmpty()) {
            return;
        }
        if (value == null) {
            scriptLocals.remove(local);
        } else {
            scriptLocals.put(local, value);
        }
    }

    private Pokemon localPokemon(String local) {
        Object stored = scriptLocals.get(local, null);
        return stored instanceof Pokemon ? (Pokemon) stored : null;
    }

    private void pokemonCall(Pokemon pokemon, JsonValue ir) {
        String action = ir.getString("action", "");
        JsonValue args = ir.get("args");
        Object first = args != null && args.isArray() && args.size > 0
                ? resolveIrValue(args.get(0)) : null;
        switch (action) {
            case "makeShiny":
            case "makeSuperShiny":
                pokemon.shiny = true;
                break;
            case "makeFemale":
                pokemon.gender = PokemonStats.FEMALE;
                break;
            case "makeMale":
                pokemon.gender = PokemonStats.MALE;
                break;
            case "calcStats":
                pokemon.hp = pokemon.maxHp();
                break;
            case "setAbility":
                applyAbility(pokemon, first);
                break;
            case "setItem":
                pokemon.item = asName(first);
                break;
            case "setNature":
                applyNature(pokemon, first);
                break;
            case "pbLearnMove":
                learnMove(pokemon, asName(first));
                break;
            case "pbRecordFirstMoves":
                // The level-up move list is already recorded by POKEMON_CREATE.
                break;
            default:
                log.warn("POKEMON_CALL " + action + " is not implemented yet; skipped");
                break;
        }
    }

    /** {@code p.setAbility(n)}: 0/1 from the species list, 2 the hidden one. */
    private void applyAbility(Pokemon pokemon, Object value) {
        if (value instanceof Number) {
            int index = ((Number) value).intValue();
            if (pokemon.species != null && index >= 0 && index < pokemon.species.abilities.size) {
                pokemon.ability = pokemon.species.abilities.get(index);
            } else if (index == 2 && pokemon.species != null) {
                pokemon.ability = pokemon.species.hiddenAbility;
            } else {
                log.warn("setAbility(" + index + ") has no such ability; skipped");
            }
        } else if (value instanceof String) {
            pokemon.ability = (String) value;
        }
    }

    private void applyNature(Pokemon pokemon, Object value) {
        if (pbs == null) {
            return;
        }
        PbsData.Nature nature = value instanceof Number
                ? pbs.nature(((Number) value).intValue())
                : pbs.nature(asName(value));
        if (nature == null) {
            log.warn("setNature(" + value + ") is unknown; skipped");
            return;
        }
        pokemon.nature = nature;
    }

    private void applyForm(Pokemon pokemon, int form) {
        if (pokemon.species == null) {
            return;
        }
        if (form <= 0) {
            pokemon.form = null;
            pokemon.internalName = pokemon.species.internalName;
            return;
        }
        PbsData.SpeciesForm resolved = pbs == null ? null : pbs.form(pokemon.species.internalName, form);
        if (resolved == null) {
            log.warn("form " + form + " of " + pokemon.species.internalName + " is unknown; skipped");
            return;
        }
        pokemon.form = resolved;
        pokemon.internalName = resolved.key;
    }

    private void learnMove(Pokemon pokemon, String moveName) {
        if (pbs == null || moveName == null) {
            return;
        }
        PbsData.Move move = pbs.move(moveName);
        if (move == null) {
            log.warn("pbLearnMove(" + moveName + ") is unknown; skipped");
            return;
        }
        for (int i = 0; i < pokemon.moves.size; i++) {
            PbsData.Move known = pokemon.moves.get(i).move;
            if (known != null && moveName.equals(known.internalName)) {
                return;
            }
        }
        if (pokemon.moves.size >= 4) {
            log.warn("pbLearnMove(" + moveName + "): the move list is full; skipped");
            return;
        }
        pokemon.moves.add(new Pokemon.MoveSlot(move));
    }

    private void pokemonSet(Pokemon pokemon, String property, Object value) {
        switch (property) {
            case "iv":
                copyStats(value, pokemon.ivs, "iv");
                pokemon.hp = pokemon.maxHp();
                break;
            case "ev":
                copyStats(value, pokemon.evs, "ev");
                pokemon.hp = pokemon.maxHp();
                break;
            case "form":
                applyForm(pokemon, value instanceof Number ? ((Number) value).intValue() : 0);
                pokemon.hp = pokemon.maxHp();
                break;
            case "ot":
                pokemon.originalTrainer = asName(value);
                break;
            case "name":
                pokemon.name = asName(value);
                break;
            case "battleRank":
                pokemon.battleRank = value instanceof Number ? ((Number) value).intValue() : 0;
                break;
            case "shiny":
                pokemon.shiny = Boolean.TRUE.equals(value);
                break;
            case "item":
                pokemon.item = asName(value);
                break;
            case "nature":
                applyNature(pokemon, value);
                break;
            case "happiness":
                pokemon.happiness = value instanceof Number ? ((Number) value).intValue() : 0;
                break;
            case "stepsToHatch":
                pokemon.stepsToHatch = value instanceof Number ? ((Number) value).intValue() : 0;
                break;
            default:
                log.warn("Pokemon property " + property + " is not implemented yet; skipped");
                break;
        }
    }

    private void copyStats(Object value, int[] target, String label) {
        if (!(value instanceof int[]) || ((int[]) value).length != target.length) {
            log.warn(label + " expects " + target.length + " numbers; skipped");
            return;
        }
        System.arraycopy(value, 0, target, 0, target.length);
    }

    private static String asName(Object value) {
        return value instanceof String ? (String) value : null;
    }

    /**
     * R6.19: typed reads of a compiled script block. A Builder that could not
     * resolve an argument (or an IR file from an older build) may carry a
     * string / object where a number is expected; the guards keep the
     * interpreter running with the documented fallback instead of throwing.
     */
    private static int irInt(JsonValue ir, String name, int fallback) {
        JsonValue value = ir.get(name);
        return value == null || !value.isNumber() ? fallback : value.asInt();
    }

    private static boolean irBoolean(JsonValue ir, String name, boolean fallback) {
        JsonValue value = ir.get(name);
        return value == null || !value.isBoolean() ? fallback : value.asBoolean();
    }

    private static String irString(JsonValue ir, String name, String fallback) {
        JsonValue value = ir.get(name);
        return value == null || !value.isString() ? fallback : value.asString();
    }

    /** One command list on the call stack; common event frames remember their id. */
    private static final class Frame {
        final EventProgram program;
        final int commonEventId;

        Frame(EventProgram program, int commonEventId) {
            this.program = program;
            this.commonEventId = commonEventId;
        }
    }
}
