package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.battle.BattlePort;
import pokemon.runtime.battle.BattleResult;
import pokemon.runtime.battle.HeadlessBattlePort;
import pokemon.runtime.field.PokemonEncounters;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.Storage;
import pokemon.runtime.state.GameState;
import pokemon.runtime.state.QuestLog;

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
    /**
     * {@code $game_player.x == 10} / {@code $game_player.y < 18}: cutscene pages
     * pick a move route from the hero's tile (map36 event 6 branches on it).
     */
    private static final Pattern SCRIPT_PLAYER_POSITION =
            Pattern.compile("^\\$game_player\\.(x|y)\\s*(==|>=|<=|>|<|!=)\\s*(-?\\d+)$");
    /** Essentials {@code get_character(N).onEvent?}: the player stands on N. */
    private static final Pattern SCRIPT_ON_EVENT =
            Pattern.compile("^get_character\\(\\s*(-?\\d+)\\s*\\)\\.onEvent\\?$");
    /**
     * PField_Field:1363: {@code def pbBridgeOn(height=2)} - the Ruby default is
     * what an argument-less {@code pbBridgeOn} stores in
     * {@code $PokemonGlobal.bridge}.
     */
    public static final int DEFAULT_BRIDGE_HEIGHT = 2;

    /** Essentials {@code isTempSwitchOn?("A")} / {@code isTempSwitchOff?("A")}. */
    private static final Pattern SCRIPT_TEMP_SWITCH =
            Pattern.compile("^isTempSwitch(On|Off)\\?\\(\\s*\"([A-D])\"\\s*\\)$");
    /** {@code $Trainer.party.length > 0} / {@code $Trainer.partyCount >= 6} etc. */
    private static final Pattern SCRIPT_PARTY =
            Pattern.compile("^\\$Trainer\\.(?:party\\.(?:length|size)|partyCount)\\s*(?:([=!<>]=|[<>])\\s*(-?\\d+))?$");

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
    private MenuService menuService;
    private MenuService.Request menuRequest;
    public void attachMenuService(MenuService service) { menuService = service; }
    /** P3: the berry plant stepper ({@code pbBerryPlant} / {@code pbPickBerry}). */
    private BerryInteraction berry;
    /** P2: the random source for triggered encounters (RockSmash). */
    private java.util.Random encounterRandom = new java.util.Random();

    /** Test seam: the random source for the rock smash roll and its encounter table. */
    public void setEncounterRandom(java.util.Random random) {
        this.encounterRandom = random;
    }
    /**
     * P0c: local variables of the Pokemon construction scripts
     * ({@code p = pbGenPkmn(...)}). They live for one compiled SEQUENCE, which
     * is one event script block.
     */
    private final ObjectMap<String, Object> scriptLocals = new ObjectMap<>();
    private final WarningLog log;
    /**
     * PField_Battles:510-516: the variable that receives the next battle's
     * decision (1 = won, 2 = lost, 3 = fled). Written when the interpreter
     * resumes after the battle.
     */
    private int pendingOutcomeVar = -1;
    /** A code-111 script condition that started a trainer battle (pbTrainerBattle). */
    private BattleCondition pendingBattleCondition;
    /** R8: the random source of the boss rewards (rand(1..5) / rand(1000) / sample). */
    private java.util.Random random = new java.util.Random();
    /** R8: messages queued by one IR step (the boss rewards show several). */
    private final java.util.ArrayDeque<String> pendingMessages = new java.util.ArrayDeque<>();

    public void attachRandom(java.util.Random value) {
        random = value == null ? new java.util.Random() : value;
    }

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
    /**
     * P0d: running compiled Ruby {@code for <var> in <from>..<to> ... end}
     * loops (the catch scripts' ball-shake glue). The body may wait (pbWait)
     * between iterations, so each loop keeps its own cursor; frames nest.
     */
    private final Array<RepeatFrame> repeatFrames = new Array<>();

    private Array<String> messageLines = new Array<>();
    private int messagePage;
    private boolean messageWaits = true;
    private String messageSpeaker;
    /** R6.31: the current message's window size (\l[n]) and skin (\w[skin]). */
    private int messageLinesPerPage = MessageService.LINES_PER_PAGE;
    private String messageSkin;
    private boolean messagePositionSet;
    private int messagePosition;
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

    /**
     * Starts a common event as the running event (the common event of a dependent event, 182_PField_DependentEvents:
     * 406-422 {@code pbMapInterpreter.setup(e.list, ...)}). False when there is none to run.
     */
    public boolean startCommonEvent(int id, int mapId) {
        Array<EventCommand> commands = commonEvents == null ? null : commonEvents.commonEvent(id);
        if (commands == null || commands.size == 0) {
            return false;
        }
        start(commands, mapId, 0);
        return true;
    }

    /** Aborts the current event (map change, event erased, new interaction). */
    public void stop() {
        if (scriptTask != null) {
            scriptTask.abort();
            scriptTask = null;
            scriptTaskDone = null;
            scriptTaskAnswer = null;
            scriptQuestion = null;
        }
        if (menuRequest != null) menuRequest.complete(-1, "");
        menuRequest = null;
        keyItem = null;
        keyItemAfter = null;
        berry = null;
        stack.clear();
        activeCommonEvents.clear();
        pendingSteps = null;
        pendingStepIndex = 0;
        repeatFrames.clear();
        pendingOutcomeVar = -1;
        pendingBattleCondition = null;
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
        messagePosition = 0;
        messagePositionSet = false;
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

    /**
     * The tail of {@code DiegoWTsStarterSelection#pbChooseBall} (333_DiegoWT:408-419): the chosen slot is
     * remembered in variable 7 when that is still 0, then {@code pbGenPkmn(dex,STARTERL)}, all IVs 31,
     * {@code setAbility(2)}, {@code calcStats} and {@code pbAddPokemon(p,STARTERL)}.
     */
    private void starterChosen(MenuService.Request request) {
        int slot = request.result;                                          // @select
        if (state.variables().get(7) == 0) {                                // :409
            state.variables().set(7, slot);
        }
        if (pbs == null || request.dex == null || slot < 1 || slot > request.dex.length) {
            log.warn("starter selection without PBS data; nothing given");
            return;
        }
        String speciesName = pbs.speciesById.get(String.valueOf(request.dex[slot - 1]));
        PbsData.Species species = speciesName == null ? null : pbs.species(speciesName);
        if (species == null) {
            log.warn("starter dex number " + request.dex[slot - 1] + " is unknown; nothing given");
            return;
        }
        Pokemon starter = newPkmn(species, STARTER_LEVEL);         // :412 pbGenPkmn(..., STARTERL)
        starter.ivs = new int[] {31, 31, 31, 31, 31, 31};                    // :413
        applyAbility(starter, 2);                                           // :414
        starter.hp = starter.maxHp();                                       // :415 calcStats
        pbAddPokemon(starter, false);                                        // :416 pbAddPokemon
        log.warn("received starter " + species.internalName + " L" + STARTER_LEVEL);
    }

    /**
     * {@code pbAddPokemon(pokemon)} (252_PSystem_PokemonUtilities:67-83) and {@code pbAddPokemonSilent} (:85-101): the
     * Pokemon joins the party, or goes to the PC when the party is full. The loud form says so with the
     * "Pkmn get" jingle first (:79) and tells where it went when it was boxed ({@code pbStorePokemon}, :17-49);
     * {@code pbNickname} (:8-15) is commented out in the plugin. 登记: {@code seenStorageCreator} (the PC's owner name).
     *
     * @return false when party and boxes are full
     */
    private boolean pbAddPokemon(Pokemon pkmn, boolean silent) {
        return pbAddPokemon(pkmn, silent, null);
    }

    /** {@code say}: a script task's own message path (the lines are shown there); null = the interpreter's message window. */
    private boolean pbAddPokemon(Pokemon pkmn, boolean silent, java.util.function.Consumer<String> say) {
        pokemon.runtime.pokemon.TrainerState trainer = state.trainer();
        boolean boxesFull = trainer.party.isFull() && trainer.currentStorage().full();                  // :4-6 pbBoxesFull?
        if (boxesFull) {
            if (!silent) {
                if (say != null) {
                    say.accept("There's no more room for Pokémon!");                                    // :70
                    say.accept("盒子都满了，不能接受！");                                                   // :71
                } else {
                    pendingMessages.add("盒子都满了，不能接受！");                                          // :71
                    showHandlerMessage("There's no more room for Pokémon!");                                // :70
                }
            }
            return false;
        }
        String speciesName = pkmn.species == null ? pkmn.name : pkmn.species.name;
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (!silent) {
            lines.add(trainer.name + "得到了" + speciesName + "!\\me[Pkmn get]\\wtnp[80]");               // :79
        }
        trainer.registerOwned(pkmn);                                                                    // :57-58 / :91-92 seen + owned
        pkmn.recordFirstMoves();                                                                        // :23 / :94 pbRecordFirstMoves
        if (trainer.party.size() < 6) {
            trainer.party.add(pkmn);                                                                    // :24-25
        } else {
            pokemon.runtime.pokemon.Storage storage = trainer.currentStorage();
            int oldBox = storage.currentBox;                                                            // :27
            int storedBox = storage.pbStoreCaught(pkmn);                                                // :28
            if (!silent && storedBox >= 0) {
                String current = storage.box(oldBox).name;                                              // :29
                String stored = storage.box(storedBox).name;                                            // :30
                if (storedBox != oldBox) {                                                              // :33
                    lines.add("寄存系统中的箱子\"" + current + "\"已经满了！");                            // :37
                    lines.add(pkmn.name + "被送到了箱子\"" + stored + "。\"");                             // :39
                } else {
                    lines.add(pkmn.name + "被送到了某人的电脑中。");                                       // :44
                    lines.add("它被存放在了箱子\"" + stored + "中。\"");                                   // :46
                }
            }
        }
        if (say != null) {
            for (String line : lines) say.accept(line);
            return true;
        }
        for (int i = 1; i < lines.size(); i++) {
            pendingMessages.add(lines.get(i));
        }
        if (!lines.isEmpty()) {
            showHandlerMessage(lines.get(0));
        }
        return true;
    }

    /** 333_DiegoWT:7 STARTERL. */
    private static final int STARTER_LEVEL = 5;

    /** Called once per frame by the map screen. */
    public void update(float delta) {
        if (battlePort != null && battlePort.pending()) return;
        if (menuRequest != null) {
            if (!menuRequest.done) return;
            if (menuRequest.kind == MenuService.Kind.CHOOSE_TRADE || menuRequest.kind == MenuService.Kind.CHOOSE_ABLE) {
                if (menuRequest.variable > 0) state.variables().set(menuRequest.variable, menuRequest.result);
                if (menuRequest.nameVariable > 0) state.variables().setText(menuRequest.nameVariable, menuRequest.text);
            } else if (menuRequest.kind == MenuService.Kind.CHOOSE_ITEM && menuRequest.variable > 0) {
                state.variables().set(menuRequest.variable, Math.max(0, menuRequest.result));   // pbChooseFossil: the item id, 0 = none
            } else if (menuRequest.kind == MenuService.Kind.CHOOSE_ITEM && berry != null) {
                berry.itemChosen(menuRequest); // pbChooseItemScreen result
            } else if (menuRequest.kind == MenuService.Kind.STARTER && menuRequest.result > 0) {
                starterChosen(menuRequest);
            }
            menuRequest = null;
        }
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
                && menuRequest == null
                && (battlePort == null || !battlePort.pending())
                && (repeatFrames.size > 0 || pendingSteps != null || stack.size > 0)) {
            if (scriptTask != null) {
                pumpScriptTask();                                // a script condition that talks to the player
                continue;
            }
            if (waitingFinishEvent >= 0 && (battlePort == null || !battlePort.pending())) {
                // PField_Battles:574-576: the waiting trainer is done once the battle is won.
                BattleResult finished = battlePort == null ? null : battlePort.lastResult();
                if (finished != null && finished.won()) {
                    state.selfSwitches().set(mapId, waitingFinishEvent, "A", true);
                }
                waitingFinishEvent = -1;
            }
            if (pendingOutcomeVar > 0 && (battlePort == null || !battlePort.pending())) {
                // PField_Battles:510-516: the finished battle's decision lands
                // in its outcome variable before the event continues.
                int variable = pendingOutcomeVar;
                pendingOutcomeVar = -1;
                BattleResult finished = battlePort == null ? null : battlePort.lastResult();
                state.variables().set(variable, battleDecision(finished));
            }
            if (pendingBattleCondition != null && (battlePort == null || !battlePort.pending())) {
                // pbTrainerBattle as a script condition (PField_Battles:526-580):
                // the branch resolves on the battle result.
                BattleCondition pending = pendingBattleCondition;
                pendingBattleCondition = null;
                BattleResult finished = battlePort == null ? null : battlePort.lastResult();
                boolean won;
                if (pending.fixedResult != null) {
                    won = pending.fixedResult;                                    // no battle took place
                } else {
                    state.variables().set(pending.outcomeVar, battleDecision(finished));
                    if (pending.resetSwitch196) state.switches().set(196, false); // Boss_Battles: $game_switches[196] = false
                    won = pending.requiredDecision >= 0
                            ? battleDecision(finished) == pending.requiredDecision   // return decision==N
                            : finished != null && finished.won();
                }
                if (pending.program != null) {                                   // a statement (no branch) has no program
                    pending.program.branchResult(pending.program.indent(), won);
                    if (won) {
                        pending.program.advance();
                    } else {
                        pending.program.skipBlock();
                    }
                }
            }
            if (repeatFrames.size > 0) {
                // P0d: one iteration of a compiled `for` loop. The body may wait
                // (pbWait), so the cursor resumes here after the wait.
                RepeatFrame frame = repeatFrames.peek();
                if (frame.index < frame.steps.size) {
                    if (++steps > MAX_STEPS_PER_UPDATE) {
                        log.warn("event interpreter paused after " + MAX_STEPS_PER_UPDATE
                                + " commands in one update (possible loop)");
                        break;
                    }
                    executeIrStep(frame.steps.get(frame.index++));
                    continue;
                }
                frame.current++;
                if (frame.current <= frame.last) {
                    frame.index = 0;
                    setLocal(frame.local, frame.current);
                    continue;
                }
                repeatFrames.pop();
                continue;
            }
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

    /** {@code $game_temp.to_title} (014_Game_Temp:50): Return to Title Screen (354) was run. */
    private boolean toTitleRequested;

    /**
     * Reads and clears {@code $game_temp.to_title}; the map scene checks it after
     * every update ({@code Scene_Map#update}, 049_Scene_Map:171-174) and calls the
     * title.
     */
    public boolean consumeTitleRequest() {
        boolean requested = toTitleRequested;
        toTitleRequested = false;
        return requested;
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
            case 104: {                                                            // 048_Interpreter:556-567 command_104
                JsonValue position = command.parameter(0);
                JsonValue frame = command.parameter(1);
                state.messageOptions(position == null ? 2 : position.asInt(), frame == null ? 0 : frame.asInt());
                program.advance();
                break;
            }
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
            case 116: // Erase Event (RMXP Game_Event#erase)
                if (mapPort != null && eventId >= 0) {
                    mapPort.eraseEvent(eventId);
                }
                program.advance();
                break;
            case 117: // Call Common Event (the common event runs before this one)
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
                changePictureTone(program, command);
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
            case 132:
                changeBattleAudio(program, command, true);
                break;
            case 133:
                changeBattleAudio(program, command, false);
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
            case 103:
                inputNumber(program, command);
                break;
            case 118: // Label: 048_Interpreter:778-781 command_118 only continues.
                program.advance();
                break;
            case 119:
                jumpToLabel(program, command);
                break;
            case 125:
                changeGold(program, command);
                break;
            case 246: // 071_Messages:354-357 command_246: pbBGSFade(pbParams[0])
                if (audio != null) {
                    audio.fadeBgs(floatParam(command.parameters, 0, 0f));
                }
                program.advance();
                break;
            case 354: // 048_Interpreter:1449-1454 $game_temp.to_title = true; return false
                toTitleRequested = true;
                program.finish();
                break;
            case 236: // 048_Interpreter:1354-1359 $game_screen.weather(@parameters[0..2])
                state.weather().set(intParam(command.parameters, 0, 0), intParam(command.parameters, 1, 0),
                        intParam(command.parameters, 2, 0));
                program.advance();
                break;
            case 247: // 048_Interpreter:1361-1367 Memorize BGM/BGS
                if (audio != null) {
                    audio.memorizeBgmAndBgs();
                }
                program.advance();
                break;
            case 248: // 048_Interpreter:1371-1377 Restore BGM/BGS
                if (audio != null) {
                    audio.restoreBgmAndBgs();
                }
                program.advance();
                break;
            case 314:
                recoverAll(program, command);
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
                if (waitCancelable && (input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL))) {
                    waitCancelled = true;                                              // pbWaitMessage: Input.trigger?(C) || (B)
                    waitCancelable = false;
                    waitTimer = 0f;
                }
                if (waitCancelable && waitTimer <= 0f) {
                    waitCancelable = false;
                }
                if (keyItem != null) {
                    keyItem.update(delta);
                    if (keyItem.takeJingle() && audio != null) {
                        audio.playMe("Key item get", 100, 100);                     // 302:132
                    }
                }
                if (waitTimer <= 0f) {
                    interpreterState = InterpreterState.RUNNING;
                    if (keyItem != null) {
                        keyItem = null;
                        Runnable follow = keyItemAfter;
                        keyItemAfter = null;
                        if (follow != null) {
                            follow.run();                                            // :170 pbReceiveItem
                        }
                    }
                }
                break;
            case WAIT_MESSAGE:
                updateMessage(delta);
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
            case WAIT_BERRY:
                // P3: the berry interaction owns the message / choice / bag
                // sequence (PField_BerryPlants:313-590) until it ends.
                if (berry == null || berry.update()) {
                    berry = null;
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
        noteMessageExtras(parsed);
        messageSpeaker = parsed.speaker;
        messageLinesPerPage = parsed.lineCount > 0 ? parsed.lineCount : MessageService.LINES_PER_PAGE;
        messageSkin = parsed.skin;
        messagePosition = parsed.position;
        // R6.31: RMXP shows a following Show Choices while the last text page
        // is still on screen, so that page must not wait for its own confirm.
        choicesFollow = !program.finished()
                && (program.current().code == 102 || program.current().code == 103);
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
        if (messageMe != null && messagePage == 0) {                       // \me[name]
            if (audio != null && !messageMe.isEmpty()) {
                audio.playMe(messageMe, 100, 100);
            }
            messageMe = null;
        }
        if (messageSeDue && messagePage == 0) {                             // 071_Messages:1150-1154
            messageSeDue = false;
            if (messageStartSe != null) {
                if (audio != null && !messageStartSe.isEmpty()) {
                    audio.playSe(messageStartSe, 100, 100);                 // pbSEPlay(pbStringToAudioFile(startSE))
                }
            } else if (!messageOpen) {
                pokemon.runtime.audio.UiSounds.decision(audio);             // pbPlayDecisionSE() (letterbyletter, no sign)
            }
        }
        messageAutoTimer = 0f;
        if (messageAutoFrames > 0 && lastPage && !choicesFollow) {         // \wtnp[n]: no confirm, n frames at 40 fps
            messageAutoTimer = messageAutoFrames / 40f;
            pageWaits = false;
        }
        // $game_system.message_frame != 0 -> msgwindow.opacity = 0 (062_MessageConfig:198-202): no window, white text, like \w[]
        String skin = state.messageFrame() != 0 ? "" : messageSkin;
        // pbRepositionMessageWindow (:187-197): the system position (0 top, 1 middle, 2 bottom) unless a \wd/\wm/\wu chose
        int position = messagePositionSet ? messagePosition : state.messagePosition() == 0 ? 2 : state.messagePosition() == 1 ? 1 : 0;
        messages.showLines(page, messageSpeaker, pageWaits, messageLinesPerPage, skin, position);
    }

    /**
     * 297_Follower_Main:815-?? {@code pbStartOver} (the live one; it replaces 171_PField_Visuals:634): heal the party, say
     * where the player is going, and send the player to the last Pokemon Center - or home when there is none - with the
     * "starting over" switch on, so the map's recovery event runs. The lines and the transfer run as a small event
     * program: pushed on top of the running one (the battle's event goes on after it) or started when nothing runs.
     * 登记: pbRemoveDependenciesExceptFollower (no followers / dependent events yet) and pbEraseEscapePoint
     * (no escape rope data yet); the bug-contest branch (not part of the game).
     */
    public void startOver() {
        state.trainer().healParty();                                              // pbHealAll
        int[] target;
        String text;
        if (state.trainer().hasPokemonCenter()) {
            text = "\\w[]\\wm\\c[8]\\l[3]您急忙回到宝可梦中心，保护疲惫的宝可梦\\n免受任何进一步的伤害。";
            target = new int[] {state.trainer().healMapId, state.trainer().healX, state.trainer().healY,
                    state.trainer().healDirection};
        } else {
            // 用户指定：原工程作者把妈妈的坐标换了，战败回家固定为 3,17,12,8（PBS 的 Home 仍是 3,17,14,8）
            int[] home = {3, 17, 12, 8};
            text = "\\w[]\\wm\\c[8]\\l[3]您匆匆回到家中，避免疲惫的宝可梦\\n免受进一步的伤害...";
            target = new int[] {home[0], home[1], home[2], home.length > 3 ? home[3] : 2};
        }
        if (mapPort != null) {
            mapPort.removeDependencies(true);                                    // pbRemoveDependenciesExceptFollower
        }
        state.fieldGlobals().surfing = false;                                    // pbCancelVehicles
        state.fieldGlobals().bicycle = false;
        state.fieldGlobals().diving = false;
        Array<EventCommand> list = new Array<>();
        list.add(syntheticCommand(0, 101, text));
        list.add(syntheticCommand(1, 121, 1, 1, 0));                              // $game_switches[STARTING_OVER_SWITCH] = true
        // transfer_player: no fade of its own; after a battle the screen is already black and the new map fades in
        int fade = screenEffects != null && screenEffects.fade() >= 250f ? 0 : 1;
        list.add(syntheticCommand(2, 201, 0, target[0], target[1], target[2], target[3], fade));
        list.add(syntheticCommand(3, 0));
        if (running()) {
            stack.add(new Frame(new EventProgram(list), -1));
        } else {
            start(list, mapId, 0);
        }
    }

    private static EventCommand syntheticCommand(int index, int code, Object... values) {
        EventCommand command = new EventCommand();
        command.index = index;
        command.code = code;
        command.indent = 0;
        JsonValue parameters = new JsonValue(JsonValue.ValueType.array);
        for (Object value : values) {
            parameters.addChild(value instanceof Integer ? new JsonValue((long) (Integer) value)
                    : new JsonValue(String.valueOf(value)));
        }
        command.parameters = parameters;
        return command;
    }

    /**
     * 297_Follower_Main:68-82 pbTalkToFollower after the map side started the cry, the emote and the routes: the wait
     * ({@code pbWait}) and the line - or the item the follower found ({@code pbPokemonFound}, :95-140) - as a small event
     * program pushed on top of the running one.
     */
    private void startFollowerTalk(MapPort.FollowerTalkPlan plan) {
        Array<EventCommand> list = new Array<>();
        int index = 0;
        if (plan.waitFrames > 0) {
            list.add(syntheticCommand(index++, 106, (plan.waitFrames + 1) / 2));
        }
        if (plan.foundItem != null && pbs != null && pbs.item(plan.foundItem) != null && inventory != null) {
            list.add(syntheticCommand(index++, 101, plan.foundMessage));                 // :99
            PbsData.Item item = pbs.item(plan.foundItem);
            int quantity = plan.foundQuantity;
            String itemName = quantity > 1 && item.namePlural != null && !item.namePlural.isEmpty() ? item.namePlural : item.name;
            boolean machine = item.fieldUse == 3 || item.fieldUse == 4 || item.fieldUse == 6;           // pbIsMachine?
            String meName = item.type == 6 ? "Key item get" : "Item get";                                // :108
            String pokename = plan.pokemonName;
            String found;
            if ("LEFTOVERS".equals(plan.foundItem)) {
                found = "\\me[" + meName + "]" + pokename + "找到了一些\\c[1]" + itemName + "\\c[0]!\\wtnp[30]";       // :110
            } else if (machine) {
                PbsData.Move move = item.machine != null && !item.machine.isEmpty() ? pbs.move(item.machine)
                        : pokemon.runtime.pokemon.ItemUse.machineMove(item, pbs);
                found = "\\me[" + meName + "]" + pokename + "找到了\\c[1]" + itemName + " " + (move == null ? "" : move.name)
                        + "\\c[0]!\\wtnp[30]";                                                         // :112
            } else if (quantity > 1) {
                found = "\\me[" + meName + "]" + pokename + "找到了" + quantity + " \\c[1]" + itemName + "\\c[0]!\\wtnp[30]";   // :114
            } else {
                found = "\\me[" + meName + "]" + pokename + "找到了一个\\c[1]" + itemName + "\\c[0]!\\wtnp[30]";        // :116/:118
            }
            inventory.add(plan.foundItem, quantity);                                                     // :107 pbStoreItem
            list.add(syntheticCommand(index++, 101, found));
            String pocketName = item.pocket >= 0 && item.pocket < POCKET_NAMES.length ? POCKET_NAMES[item.pocket] : "";
            list.add(syntheticCommand(index++, 101, "把" + itemName + " 放入了<icon=bagPocket" + item.pocket
                    + ">\\c[1]" + pocketName + "背包\\c[0]."));                                            // :120
            state.fieldGlobals().followerHoldItem = false;                                               // :122
            state.fieldGlobals().timeTaken = 0;                                                          // :123
        } else {
            for (String message : plan.messages) {
                list.add(syntheticCommand(index++, 101, message));
            }
        }
        list.add(syntheticCommand(index, 0));
        stack.add(new Frame(new EventProgram(list), -1));
    }

    private final ItemFindToasts itemToasts = new ItemFindToasts();
    /** Set by the first item received in this process; the map screen then preloads the message glyphs once. */
    private static boolean messageWarmUpRequested;
    private static boolean itemMessagesSeen;

    /** True once, for the first item received (the UI layer runs the one-off message preload). */
    public boolean consumeMessageWarmUpRequest() {
        boolean requested = messageWarmUpRequested;
        messageWarmUpRequested = false;
        return requested;
    }
    private KeyItemAnimation keyItem;
    private Runnable keyItemAfter;

    /** The running pbGetKeyItem animation, or null. */
    public KeyItemAnimation keyItemAnimation() {
        return keyItem;
    }

    /** The boxes at the right edge for items picked up before (308_ItemFindSimple_Scene). */
    public ItemFindToasts itemToasts() {
        return itemToasts;
    }

    /** {@code pocketNames} of the plugin (Settings:176). */
    private static final String[] POCKET_NAMES = {
            "", "道具", "回复道具", "精灵球", "招式学习机", "树果", "超级石", "对战道具", "重要道具", "特殊道具",
    };

    /**
     * 309_Item_Find:62-205 pbItemBall (a ground item, {@code ball}) / pbReceiveItem (a gift): the first time an item is
     * found the jingle and the messages play; afterwards a ground item only plays its jingle and shows the box at the
     * right edge. The bag has no pocket limit here (Settings:187), so the "bag is full" branches never run.
     */
    private void giveItemWithMessages(String internalName, int quantity, boolean ground) {
        giveItemWithMessages(internalName, quantity, ground, null);
    }

    /** {@code say}: a script task's message path; null = the interpreter's message window. */
    private void giveItemWithMessages(String internalName, int quantity, boolean ground, java.util.function.Consumer<String> say) {
        if (!itemMessagesSeen) {
            itemMessagesSeen = true;
            messageWarmUpRequested = true;
        }
        PbsData.Item data = pbs.item(internalName);
        String itemName = quantity > 1 && data.namePlural != null && !data.namePlural.isEmpty() ? data.namePlural : data.name;
        boolean machine = data.fieldUse == 3 || data.fieldUse == 4 || data.fieldUse == 6;     // pbIsMachine?
        String meName = data.type == 6 ? "Key item get" : machine && ground ? "Machine get" : "Item get";
        String moveName = "";
        if (machine) {
            PbsData.Move move = data.machine != null && !data.machine.isEmpty() ? pbs.move(data.machine)
                    : pokemon.runtime.pokemon.ItemUse.machineMove(data, pbs);
            moveName = move == null ? "" : move.name;
        }
        java.util.Set<String> found = state.fieldGlobals().foundItems;
        boolean first = !found.contains(internalName);
        if (ground && !first) {                                                  // :168-178 seen before: jingle + box
            if (audio != null) {
                audio.playMe(meName, 100, 100);
            }
            inventory.add(internalName, quantity);
            itemToasts.add(internalName, data.name, quantity);
            return;
        }
        String verb = ground ? "你发现了" : "获得了";
        String text;
        if ("LEFTOVERS".equals(internalName)) {
            text = "\\me[" + meName + "]" + verb.replace("了", "了一些") + "\\c[1]" + itemName + "\\c[0]！\\wtnp[30]";
        } else if (machine) {
            text = "\\me[" + meName + "]" + verb + "\\c[1]" + itemName + " " + moveName + "\\c[0]！\\wtnp[30]";
        } else if (quantity > 1) {
            text = "\\me[" + meName + "]" + verb + quantity + "个\\c[1]" + itemName + "\\c[0]！\\wtnp[30]";
        } else {
            text = "\\me[" + meName + "]" + verb + "1个\\c[1]" + itemName + "\\c[0]！\\wtnp[30]";
        }
        inventory.add(internalName, quantity);
        found.add(internalName);
        String pocketName = data.pocket >= 0 && data.pocket < POCKET_NAMES.length ? POCKET_NAMES[data.pocket] : "";
        String pocketLine = ground
                ? "你将" + itemName + "放进了<icon=bagPocket" + data.pocket + ">\\c[1]" + pocketName + "\\c[0]口袋。"
                : "你将" + itemName + "放进了<icon=bagPocket" + data.pocket + ">\\c[1]" + pocketName + "口袋\\c[0]。";
        if (say != null) {
            say.accept(text);
            say.accept(pocketLine);
            return;
        }
        pendingMessages.add(pocketLine);
        showHandlerMessage(text);
    }

    /** \me[..] / \wtnp[..] of the message being shown. */
    private String messageMe;
    /** \se[..] opening the message, and whether its opening sound is still due (071_Messages:1145-1154). */
    private String messageStartSe;
    private boolean messageOpen;
    private boolean messageSeDue;
    private int messageAutoFrames;
    private float messageAutoTimer;

    private void noteMessageExtras(MessageText.Parsed parsed) {
        messages.gold(parsed.gold);                                      // \g: pbDisplayGoldWindow
        messageMe = parsed.me;
        messageStartSe = parsed.startSe;
        messageOpen = parsed.open;
        messageSeDue = true;
        messageAutoFrames = parsed.autoFrames;
        messagePosition = parsed.position;
        messagePositionSet = parsed.positionSet;
    }

    private void updateMessage(float delta) {
        if (!messages.visible()) {
            interpreterState = InterpreterState.RUNNING;
            return;
        }
        if (messages.numberMode()) {
            updateNumberInput();
            return;
        }
        if (messages.choiceMode()) {
            int before = messages.cursor();
            if (input.wasPressed(GameAction.UP)) {
                messages.moveCursor(-1);
            } else if (input.wasPressed(GameAction.DOWN)) {
                messages.moveCursor(1);
            }
            if (messages.cursor() != before) {
                pokemon.runtime.audio.UiSounds.cursor(audio);               // 065_SpriteWindow_text:855-865 pbPlayCursorSE
            }
            if (input.wasPressed(GameAction.CONFIRM)) {
                messages.confirm();
                choiceResult = messages.selected();
                interpreterState = InterpreterState.RUNNING;
            } else if (input.wasPressed(GameAction.CANCEL)) {
                int cancelType = messages.cancelType();
                messages.cancel();
                if (!messages.visible()) {
                    // 071_Messages:1370-1377 pbShowCommands: B with cmdIfCancel > 0 answers
                    // cmdIfCancel-1 (a Yes/No question with cancel type 2 answers "No"); the
                    // "When Cancel" branch (403) only matches the RMXP branch value 4.
                    choiceResult = cancelType > 0 ? cancelType - 1 : -1;
                    interpreterState = InterpreterState.RUNNING;
                }
            }
            return;
        }
        if (messageAutoTimer > 0f) {                                        // \wtnp[n]: the page closes by itself
            messageAutoTimer -= Math.max(0f, delta);
            if (messageAutoTimer <= 0f) {
                nextMessagePage();
            }
            return;
        }
        if (!messages.waiting()) { // "\^" page: continue without a confirm
            nextMessagePage();
            return;
        }
        if (input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL)) {
            pokemon.runtime.audio.UiSounds.decision(audio);                 // 071_Messages:1257 pbPlayDecisionSE if msgwindow.pausing?
            messages.confirm();
            nextMessagePage();
        }
    }

    /** Window_InputNumberPokemon#update + pbChooseNumber's loop (071_Messages:761-800). */
    private void updateNumberInput() {
        if (input.wasRepeated(GameAction.UP)) {
            pokemon.runtime.audio.UiSounds.cursor(audio);                   // 065_SpriteWindow_text:680 pbPlayCursorSE
            messages.changeDigit(1);
        } else if (input.wasRepeated(GameAction.DOWN)) {
            pokemon.runtime.audio.UiSounds.cursor(audio);
            messages.changeDigit(-1);
        } else if (input.wasRepeated(GameAction.RIGHT)) {
            if (messages.numberDigits() >= 2) {                             // :695-697
                pokemon.runtime.audio.UiSounds.cursor(audio);
            }
            messages.moveDigitCursor(1);
        } else if (input.wasRepeated(GameAction.LEFT)) {
            if (messages.numberDigits() >= 2) {                             // :702-704
                pokemon.runtime.audio.UiSounds.cursor(audio);
            }
            messages.moveDigitCursor(-1);
        }
        if (input.wasPressed(GameAction.CONFIRM)) {                         // :784 Input::C -> ret = number
            pokemon.runtime.audio.UiSounds.decision(audio);                 // :789
            numberInputResult = messages.number();
            messages.close();
            finishNumberInput();
        } else if (input.wasPressed(GameAction.CANCEL)) {                   // :792 Input::B -> ret = cancelNumber
            pokemon.runtime.audio.UiSounds.cancel(audio);                   // :793 pbPlayCancelSE
            numberInputResult = numberInputInitial;                         // cancelNumber = the initial value
            messages.close();
            finishNumberInput();
        }
    }

    private void finishNumberInput() {
        state.variables().set(numberInputVariable, numberInputResult);      // 071_Messages:499
        interpreterState = InterpreterState.RUNNING;
    }

    /**
     * Input Number (103), 071_Messages:493-503: {@code ChooseNumberParams} with
     * {@code setMaxDigits(@parameters[1])} and the variable's current value as the
     * default, then {@code $game_variables[@parameters[0]] = pbChooseNumber(nil, params)}.
     */
    private void inputNumber(EventProgram program, EventCommand command) {
        numberInputVariable = Math.max(1, intParam(command.parameters, 0, 1));
        int digits = intParam(command.parameters, 1, 1);
        numberInputInitial = state.variables().get(numberInputVariable);
        int limit = 1;
        for (int i = 0; i < Math.max(1, Math.min(9, digits)); i++) {
            limit *= 10;
        }
        numberInputInitial = Math.max(0, Math.min(limit - 1, numberInputInitial));
        messages.showNumberInput(digits, numberInputInitial);
        program.advance();
        interpreterState = InterpreterState.WAIT_MESSAGE;
    }

    private int numberInputVariable;
    private int numberInputInitial;
    private int numberInputResult;

    private void nextMessagePage() {
        messagePage++;
        if (messagePage * messageLinesPerPage >= messageLines.size) {
            if (choicesFollow) {
                // R6.31: keep the last page visible - the 102 command opens
                // the choice window on top of it, like RMXP.
                interpreterState = InterpreterState.RUNNING;
            } else if (!pendingMessages.isEmpty()) {
                // R8: a handler queued more pbMessage lines (boss rewards).
                showHandlerMessage(pendingMessages.poll());
            } else {
                messages.close();
                interpreterState = InterpreterState.RUNNING;
            }
        } else {
            showMessagePage();
        }
    }

    private pokemon.runtime.data.QuestTable questTable = pokemon.runtime.data.QuestTable.empty();
    private java.util.function.IntFunction<String> mapNames = id -> "";

    /** The Quest plugin's definitions and {@code $game_map.name} (a quest remembers the map it began on). */
    public void attachQuests(pokemon.runtime.data.QuestTable table, java.util.function.IntFunction<String> names) {
        this.questTable = table == null ? pokemon.runtime.data.QuestTable.empty() : table;
        this.mapNames = names == null ? id -> "" : names;
    }

    private String questMapName() {
        return mapNames.apply(state.currentMapId());
    }

    /** R8: shows one message from an IR handler and waits for it (pbMessage). */
    private boolean waitCancelable;
    private boolean waitCancelled;

    /** The text of a message that does not wait ({@code pbMessageDisplay(..., false)}): shown at once, the script goes on. */
    private void showFlashMessage(String text) {
        Array<String> raw = new Array<>();
        raw.add(text);
        MessageText.Parsed parsed = MessageText.parse(raw, state, messageColumns);
        messageLines = parsed.lines;
        messagePage = 0;
        messageWaits = false;
        noteMessageExtras(parsed);
        messageSpeaker = parsed.speaker;
        messageLinesPerPage = parsed.lineCount > 0 ? parsed.lineCount : MessageService.LINES_PER_PAGE;
        choicesFollow = false;
        showMessagePage();
    }

    private void showHandlerMessage(String text) {
        Array<String> raw = new Array<>();
        raw.add(text);
        MessageText.Parsed parsed = MessageText.parse(raw, state, messageColumns);
        messageLines = parsed.lines;
        messagePage = 0;
        messageWaits = parsed.waitForInput;
        noteMessageExtras(parsed);
        messageSpeaker = parsed.speaker;
        messageLinesPerPage = parsed.lineCount > 0 ? parsed.lineCount : MessageService.LINES_PER_PAGE;
        choicesFollow = false;
        showMessagePage();
        interpreterState = InterpreterState.WAIT_MESSAGE;
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
        } else if (operandType == 2) {
            // Interpreter:840 `@parameters[4] + rand(@parameters[5] - @parameters[4] + 1)`.
            // The float_plate puzzle pages depend on this: they roll variable 26
            // between 1 and 3 and branch on it to pick the SE pitch.
            int from = intParam(p, 4, 0);
            int to = intParam(p, 5, from);
            operand = from + random.nextInt(Math.max(1, to - from + 1));
        } else if (operandType == 7) {
            // 048_Interpreter:853-861 `when 7 # other`, keyed by @parameters[4].
            int which = intParam(p, 4, -1);
            if (which == 0) {
                operand = state.currentMapId();                     // :855 $game_map.map_id
            } else if (which == 2) {
                operand = state.trainer().money;                      // :857 $Trainer.money
            } else if (which == 4) {
                operand = (int) state.trainer().playSeconds;          // :858 Graphics.frame_count / 40
            } else {
                // :856 party size / steps are commented out in the plugin, :859-860 timer and
                // save count have no runtime state here.
                unsupported(command, "variable operand type 7 / " + which);
                program.advance();
                return;
            }
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

    /**
     * Jump to Label (119), 048_Interpreter:785-808. The plugin scans the list
     * from the start while {@code temp_index < @list.size-1} (the final empty
     * command is never looked at) for a label (118) with the same name, sets
     * {@code @index} to it and continues; a missing label just continues.
     */
    private void jumpToLabel(EventProgram program, EventCommand command) {
        String labelName = stringParam(command.parameters, 0, "");           // :787
        Array<EventCommand> list = program.commands();
        int tempIndex = 0;                                                    // :789
        while (true) {                                                        // :791
            if (tempIndex >= list.size - 1) {                                 // :793
                break;                                                        // :795 continue
            }
            EventCommand candidate = list.get(tempIndex);
            if (candidate.code == 118                                         // :798
                    && labelName.equals(stringParam(candidate.parameters, 0, null))) {   // :799
                program.jumpTo(tempIndex);                                    // :801
                break;                                                        // :803
            }
            tempIndex += 1;                                                   // :806
        }
        program.advance();                                                    // the interpreter's @index += 1
    }

    /**
     * Change Gold (125), 170_PField_Field:876-880:
     * {@code $Trainer.money += operate_value(pbParams[0], pbParams[1], pbParams[2])}
     * with {@code PokeBattle_Trainer#money=} (185_PokeBattle_Trainer:73-75)
     * clamping the result to 0..MAX_MONEY (000_Settings:96).
     */
    private void changeGold(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        int value = operateValue(intParam(p, 0, 0), intParam(p, 1, 0), intParam(p, 2, 0));
        long money = (long) state.trainer().money + value;
        state.trainer().money = (int) Math.max(0L, Math.min(MAX_MONEY, money));
        program.advance();
    }

    /** 000_Settings:96 {@code MAX_MONEY}. */
    private static final int MAX_MONEY = 999_999_999;

    /** 048_Interpreter:419-432 {@code operate_value(operation, operand_type, operand)}. */
    private int operateValue(int operation, int operandType, int operand) {
        int value = operandType == 0 ? operand : state.variables().get(operand);   // :421-425
        if (operation == 1) {
            value = -value;                                                   // :427-429
        }
        return value;
    }

    /**
     * Recover All (314), 170_PField_Field:898-903: only {@code pbParams[0]==0}
     * does anything, healing every Pokemon of the party and of all PC boxes
     * ({@code pbEachPokemon}, 204_Pokemon_Storage:377-384) with
     * {@code PokeBattle_Pokemon#heal} (197_PokeBattle_Pokemon:769-774).
     */
    private void recoverAll(EventProgram program, EventCommand command) {
        if (intParam(command.parameters, 0, 0) == 0) {                       // :899
            state.trainer().currentStorage().eachPokemon(Storage::heal);      // :900
        }
        program.advance();                                                    // :902 return true
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
     * Transfer Player (201). RMXP's {@code command_201} advances its index and
     * returns false: the event waits for the map swap and then continues with
     * the next command (the screen performs the swap at the end of the frame).
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
        program.advance();
        mapPort.transfer(mapId, x, y, direction, fade);
        interpreterState = InterpreterState.WAIT_TRANSFER;
    }

    /**
     * The queued Transfer Player (201) has been carried out: the event
     * continues with the command after it, exactly like RMXP's
     * {@code command_201} (which only advances {@code @index}). A transfer that
     * keeps the player on the same map does not rebuild the map, so the running
     * page - an autorun cutscene such as map6/event29 - has to continue instead
     * of being restarted from command 0.
     */
    public void resumeAfterTransfer() {
        if (interpreterState == InterpreterState.WAIT_TRANSFER) {
            interpreterState = InterpreterState.RUNNING;
        }
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
            case "ELEVATOR": {                                             // the lifts: pbSet(11, numfloors - pbMessage(..., numfloors+1, nil, cur))
                int floors = ir.getInt("floors", 0);
                int cur = floors - state.variables().get(ir.getInt("currentVariable", 10));   // cur=numfloors-pbGet(10)
                String text = ir.getString("text", "");
                java.util.List<String> choices = new java.util.ArrayList<>();
                JsonValue list = ir.get("choices");
                if (list != null && list.isArray()) {
                    for (JsonValue v = list.child; v != null; v = v.next) choices.add(v.asString());
                }
                int resultVariable = ir.getInt("resultVariable", 11);
                startFieldTask(scene -> {
                    int picked = scene.pbMessage(text, choices, floors + 1, cur);
                    state.variables().set(resultVariable, floors - picked);       // t=pbGet(11); pbSet(11, numfloors-t)
                });
                break;
            }
            case "PARTY_FORM_CHANGE": {                                    // for pkmn in $Trainer.pokemonParty: Deoxys' form (map-152)
                String species = ir.getString("species", "");
                int form = ir.getInt("form", 0);
                for (Pokemon member : state.trainer().party.members()) {
                    if (member == null || member.egg || member.species == null || !species.equals(member.species.internalName)) continue;
                    member.setForm(pbs, form);                                    // pkmn.form=N
                    int nameVariable = ir.getInt("nameVariable", 0);
                    if (nameVariable > 0) state.variables().setText(nameVariable, member.name);
                    break;
                }
                break;
            }
            case "SET_GLOBAL_FLAG": {
                boolean value = ir.getBoolean("value", true);
                String flag = ir.getString("flag", "");
                if ("runningShoes".equals(flag)) state.fieldGlobals().runningShoes = value;       // $PokemonGlobal.runningShoes
                else if ("mysteryGiftAccess".equals(flag)) state.trainer().mysteryGiftAccess = value;   // $Trainer.mysterygiftaccess
                else log.warn("SET_GLOBAL_FLAG " + flag + " is unknown; skipped");
                break;
            }
            case "CHANGE_COINS": {                                         // $PokemonGlobal.coins += / -= n
                Object amount = resolveIrValue(ir.get("amount"));
                int n = amount instanceof Number ? ((Number) amount).intValue() : 0;
                state.fieldGlobals().coins += ir.getInt("sign", 1) * n;
                break;
            }
            case "TRAINER_NAME": {                                         // pbTrainerName(name) (253_PSystem_Utilities:700-): a fresh trainer with that name
                pokemon.runtime.pokemon.TrainerState trainer = state.trainer();
                int gender = trainer.gender;
                trainer.reset();
                trainer.gender = gender;
                trainer.name = ir.getString("name", trainer.name);
                state.playerName(trainer.name);
                break;
            }
            case "REMOVE_DEPENDENCY":                                      // pbRemoveDependency2(name) (182_PField_DependentEvents:34-36)
                if (mapPort != null) mapPort.removeDependency(ir.getString("name", ""));
                break;
            case "UNLOCK_DEX": {                                           // pbUnlockDex(dex=-1) (253_PSystem_Utilities:928-934)
                java.util.List<Boolean> unlocked = state.fieldGlobals().pokedexUnlocked;
                while (unlocked.size() < Math.max(1, regionalDexCount() + 1)) unlocked.add(unlocked.isEmpty());   // @pokedexUnlocked[i] = (i==0)
                int index = ir.getInt("dex", -1);
                if (index < 0 || index > unlocked.size() - 1) index = unlocked.size() - 1;
                unlocked.set(index, true);
                break;
            }
            case "CH_CMD_SET": {                                           // @ch_cmd=["a","b"]
                java.util.List<String> values = new java.util.ArrayList<>();
                JsonValue list = ir.get("values");
                if (list != null && list.isArray()) {
                    for (JsonValue v = list.child; v != null; v = v.next) values.add(v.asString());
                }
                instanceVariables.put("@ch_cmd", values);
                break;
            }
            case "CH_CMD_PUSH": {                                          // @ch_cmd.push("x")
                chCommands().add(ir.getString("value", ""));
                break;
            }
            case "CH_CMD_INSERT": {                                        // @ch_cmd.insert(n, "x")
                java.util.List<String> list = chCommands();
                int at = Math.max(0, Math.min(ir.getInt("index", 0), list.size()));
                list.add(at, ir.getString("value", ""));
                break;
            }
            case "CH_MESSAGE_EX": {                                        // @ch_ret = pbMessage_ex(text, @ch_cmd, cancel) (071_Messages:1339-1341)
                String text = ir.getString("text", "");
                int cancel = ir.getInt("cancel", 0);
                java.util.List<String> commands = new java.util.ArrayList<>(chCommands());
                startFieldTask(scene -> {
                    int picked = scene.pbMessage(text, commands, cancel);
                    // `commands[pbMessage(..)]`: a negative index counts from the end of the list
                    instanceVariables.put("@ch_ret", commands.isEmpty() ? null : commands.get(Math.floorMod(picked, commands.size())));
                });
                break;
            }
            case "IF_SCRIPT": {                                            // if <condition> ... end around the menu statements
                ScriptCondition condition = parseCondition(ir.getString("condition", ""));
                JsonValue steps = ir.get("steps");
                boolean value;
                try {
                    value = condition != null && ScriptCondition.truthy(condition.eval(conditionEnv(null)));
                } catch (ScriptCondition.Unsupported error) {
                    log.warn("IF_SCRIPT atom not supported: " + error.getMessage());
                    value = false;
                }
                if (value && steps != null && steps.isArray() && steps.size > 0) {
                    setLocal("__if", 0);
                    repeatFrames.add(new RepeatFrame(steps, "__if", 0, 0));
                }
                break;
            }
            case "CRYSTAL_WARP": case "MR_HYPER": case "CHANGE_BALLS": case "RESURRECTION2": case "GIVE_ALL_MEMORIES": {
                startPluginScript(ir.getString("command", ""));
                break;
            }
            case "EVENT_TIME": {                                           // pbSetEventTime(*others) (170_PField_Field:815-825)
                long now = System.currentTimeMillis() / 1000L;
                java.util.List<Integer> targets = new java.util.ArrayList<>();
                targets.add(eventId);
                JsonValue others = ir.get("others");
                if (others != null && others.isArray()) {
                    for (JsonValue v = others.child; v != null; v = v.next) targets.add(v.asInt());
                }
                for (int target : targets) {
                    if (target < 0) continue;
                    state.selfSwitches().set(mapId, target, "A", true);        // pbSetSelfSwitch(event, "A", true)
                    state.eventVars().set(mapId, target, new int[] {(int) now});   // eventvars[[map, event]] = time
                }
                break;
            }
            case "EVENT_SET_VARIABLE": {                                   // setVariable(:SYMBOL) / setVariable(id, value) (:836-844)
                if (ir.has("value") && !ir.has("variable")) {
                    if (eventId >= 0) state.eventVars().setText(mapId, eventId, ir.getString("value", ""));
                } else {
                    Object value = resolveIrValue(ir.get("value"));
                    state.variables().set(ir.getInt("variable", 0), value instanceof Number ? ((Number) value).intValue() : 0);
                }
                break;
            }
            case "LOTTERY_NUMBER": {                                       // pbSetLotteryNumber(variable=1) (238_PMinigame_Lottery:5-12)
                java.time.LocalDate today = java.time.LocalDate.now();
                long hash = today.getDayOfMonth() + ((long) today.getMonthValue() << 5) + ((long) today.getYear() << 9);
                int lottery = new java.util.Random(hash).nextInt(65536);       // srand(hash); rand(65536) (registered: Java's generator, not Ruby's)
                state.variables().setText(ir.getInt("variable", 1), String.format("%05d", lottery));
                break;
            }
            case "LOTTERY": {                                              // pbLottery(winnum, nameVar, positionVar, matchedVar) (238:14-54)
                Object winArg = resolveIrValue(ir.get("number"));
                int winnum = 0;
                try {
                    winnum = Integer.parseInt(String.valueOf(winArg).trim());
                } catch (NumberFormatException ignored) {
                    // winnum.to_i of a non-number is 0
                }
                String winpoke = null;
                int winpos = 0;
                int winmatched = 0;
                for (int pass = 0; pass < 2; pass++) {                         // the party, then every box (pbEachPokemon)
                    java.util.List<Pokemon> list = new java.util.ArrayList<>();
                    if (pass == 0) {
                        for (Pokemon member : state.trainer().party.members()) list.add(member);
                    } else {
                        state.trainer().storage.eachPokemon(list::add);
                    }
                    for (Pokemon p : list) {
                        int thismatched = 0;
                        int id = p.publicID & 0xFFFF;
                        for (int j = 0; j < 5; j++) {
                            int pow = (int) Math.pow(10, j);
                            if ((id / pow) % 10 == (winnum / pow) % 10) thismatched++; else break;
                        }
                        if (thismatched > winmatched) {
                            winpoke = p.name;
                            winpos = pass == 0 ? 1 : 2;                       // 1 party, 2 storage
                            winmatched = thismatched;
                        }
                    }
                }
                state.variables().setText(ir.getInt("nameVariable", 2), winpoke == null ? "" : winpoke);
                state.variables().set(ir.getInt("positionVariable", 3), winpos);
                state.variables().set(ir.getInt("matchedVariable", 4), winmatched);
                break;
            }
            case "BOSS_BATTLE": {                                          // `battleXxx` as a plain statement: the def runs, its result is dropped
                BossBattleData.Entry boss = BossBattleData.find(ir.getString("name", ""));
                if (boss == null) {
                    log.warn("BOSS_BATTLE " + ir.getString("name", "") + " has no Boss_Battles def; skipped");
                } else {
                    startBossBattleCondition(null, null, boss);
                }
                break;
            }
            case "GENERATE_EGG": {                                         // pbGenerateEgg(:SPECIES, text="") (323_Egg_Hatcher:259)
                pbGenerateEgg(ir.getString("species", ""), ir.getString("text", ""));
                break;
            }
            case "SAFARI_START": {                                         // pbSafariState.pbStart(ballcount) (242_PBattle_Safari:35-40)
                state.fieldGlobals().safari.begin(state.currentMapId(), state.playerX(), state.playerY(),
                        state.playerDirection(), ir.getInt("balls", 0));
                break;
            }
            case "SAFARI_END": {                                           // pbSafariState.pbEnd (:42-49)
                state.fieldGlobals().safari.end();
                break;
            }
            case "PARTY_OT_SHINY": {                                       // map-205: each_index, next if ot != name; otgender; makeShiny; makeSuperShiny
                String ot = ir.getString("ot", "");
                for (Pokemon member : state.trainer().party.members()) {
                    if (member == null || !ot.equals(member.originalTrainer)) continue;
                    member.otGender = ir.getInt("otgender", 0);
                    member.shiny = true;
                    member.superShiny = true;
                }
                break;
            }
            case "MYSTERY_GIFT": {                                         // id=pbNextMysteryGiftID; pbReceiveMysteryGift(id) (231_PScreen_MysteryGift:361-377)
                // 登记: $Trainer.mysterygift (the downloaded gifts) is not modelled, so no unclaimed gift exists: id 0, nothing found
                showHandlerMessage("找不到ID为0的无主神秘礼物。");
                break;
            }
            case "PRIZE_LOOKUP": {                                         // map-326 prize vendors: item=[...][pbGet(1)] ; price=[...][pbGet(1)] ...
                int choice = state.variables().get(1);
                JsonValue names = ir.get("items"), prices = ir.get("prices"), levels = ir.get("levels");
                String prizeName = names != null && choice >= 0 && choice < names.size ? names.get(choice).asString() : "0";
                boolean species = "species".equals(ir.getString("kind", "item"));
                int id = 0;
                String shown = "";
                if (!"0".equals(prizeName) && pbs != null) {                       // `if item && item!=0: item = getID(...)`
                    if (species) {
                        PbsData.Species data = pbs.species(prizeName);
                        if (data != null) { id = data.id; shown = data.name; }
                    } else {
                        PbsData.Item data = pbs.item(prizeName);
                        if (data != null) { id = data.id; shown = data.name; }
                    }
                }
                state.variables().set(1, id);                                  // pbSet(1, item)
                state.variables().set(2, prices != null && choice >= 0 && choice < prices.size ? prices.get(choice).asInt() : 0);   // pbSet(2, price)
                state.variables().setText(3, shown);                           // pbSet(3, PBItems/PBSpecies.getName(item))
                if (levels != null) {
                    state.variables().set(4, choice >= 0 && choice < levels.size ? levels.get(choice).asInt() : 0);   // pbSet(4, lv)
                }
                break;
            }
            case "GIVE_ITEM_VAR": {                                        // pbReceiveItem(pbGet(n))
                String item = itemById(state.variables().get(ir.getInt("variable", 1)));
                if (item != null && inventory != null && pbs != null && pbs.item(item) != null) {
                    giveItemWithMessages(item, 1, false);
                    if (interpreterState == InterpreterState.WAIT_MESSAGE) {
                        return;
                    }
                }
                break;
            }
            case "CHOOSE_FOSSIL": {                                        // pbChooseFossil(var) (188_PItem_Items:1112-1119)
                MenuService.Request fossil = new MenuService.Request(MenuService.Kind.CHOOSE_ITEM);
                PbsData data = pbs;
                fossil.filter = item -> data != null && data.item(item) != null && data.item(item).type == 8;   // pbIsFossil?
                fossil.variable = Math.max(1, ir.getInt("variable", 1));
                if (menuService != null) menuRequest = menuService.submit(fossil);
                else log.warn("CHOOSE_FOSSIL without a menu service");
                break;
            }
            case "DELETE_ITEM_VAR": {                                      // $PokemonBag.pbDeleteItem(pbGet(n))
                String item = itemById(state.variables().get(ir.getInt("variable", 0)));
                if (item != null) state.inventory().remove(item, 1);
                break;
            }
            case "ITEM_NAME_VAR": {                                        // pbSet(t, PBItems.getName(pbGet(n)))
                String item = itemById(state.variables().get(ir.getInt("variable", 0)));
                PbsData.Item data = item == null || pbs == null ? null : pbs.item(item);
                state.variables().setText(ir.getInt("target", 0), data == null || data.name == null ? "" : data.name);
                break;
            }
            case "SPECIES_NAME_VAR": {                                     // pbSet(t, PBSpecies.getName(pbGet(n)))
                PbsData.Species species = speciesById(state.variables().get(ir.getInt("variable", 0)));
                state.variables().setText(ir.getInt("target", 0), species == null ? "" : species.name);
                break;
            }
            case "CONVERT_ITEM_TO_POKEMON": {                              // pbConvertItemToPokemon(variable, array) (253_PSystem_Utilities:1048-1056)
                int variable = ir.getInt("variable", 0);
                String item = itemById(state.variables().get(variable));
                state.variables().set(variable, 0);
                JsonValue pairs = ir.get("pairs");
                if (pairs != null && pairs.isArray()) {
                    for (JsonValue pair = pairs.child; pair != null; pair = pair.next) {
                        if (item == null || !item.equals(pair.get(0).asString())) continue;
                        PbsData.Species species = pbs == null ? null : pbs.species(pair.get(1).asString());
                        if (species != null) state.variables().set(variable, species.id);
                        break;
                    }
                }
                break;
            }
            case "MINIGAME": {                                             // pbMiningGame / pbVoltorbFlip (239_PMinigame_Mining, 237_PMinigame_VoltorbFlip)
                MenuService.Request game = new MenuService.Request(MenuService.Kind.MINIGAME);
                game.wanted = ir.getString("game", "");
                if ("voltorbflip".equals(game.wanted)                           // pbVoltorbFlip (237_PMinigame_VoltorbFlip:~598-609)
                        && pbs != null && pbs.item("COINCASE") != null && !state.inventory().has("COINCASE")) {
                    showHandlerMessage("除非拥有代币盒，\n否则无法游玩。");
                } else if ("voltorbflip".equals(game.wanted) && state.fieldGlobals().coins == ConditionEnv.MAX_COINS) {
                    showHandlerMessage("代币盒装满了！");
                } else if (menuService != null) {
                    menuRequest = menuService.submit(game);
                } else {
                    log.warn("MINIGAME without a menu service");
                }
                break;
            }
            case "SLOT_MACHINE": {                                         // pbSlotMachine(difficulty) (236_PMinigame_SlotMachine:384-402)
                int coins = state.fieldGlobals().coins;
                if (pbs != null && pbs.item("COINCASE") != null && !state.inventory().has("COINCASE")) {
                    showHandlerMessage("这是老虎机。");
                } else if (coins == 0) {
                    showHandlerMessage("没有代币了！");
                } else if (coins == ConditionEnv.MAX_COINS) {
                    showHandlerMessage("代币盒装满了！");
                } else {
                    MenuService.Request slots = new MenuService.Request(MenuService.Kind.SLOT_MACHINE);
                    slots.index = ir.getInt("difficulty", 1);
                    if (menuService != null) menuRequest = menuService.submit(slots);
                    else log.warn("SLOT_MACHINE without a menu service");
                }
                break;
            }
            case "TRAINER_PC": {                                           // pbTrainerPC (PScreen_PC:205-209)
                MenuService.Request pc = new MenuService.Request(MenuService.Kind.TRAINER_PC);
                if (menuService != null) menuRequest = menuService.submit(pc);
                else log.warn("TRAINER_PC without a menu service");
                break;
            }
            case "TRAINER_CARD_BADGES": {                                  // pbStartBadgeScreen (300_B2W2_Trainer_Card:1084)
                MenuService.Request card = new MenuService.Request(MenuService.Kind.TRAINER_CARD_BADGES);
                if (menuService != null) menuRequest = menuService.submit(card);
                else log.warn("TRAINER_CARD_BADGES without a menu service");
                break;
            }
            case "CREDITS": {                                              // $scene = Scene_Credits.new (080_Scene_Credits)
                MenuService.Request credits = new MenuService.Request(MenuService.Kind.CREDITS);
                if (menuService != null) menuRequest = menuService.submit(credits);
                else log.warn("CREDITS without a menu service");
                break;
            }
            case "HALL_OF_FAME_ENTRY": {                                   // pbHallOfFameEntry (232_PScreen_HallOfFame:524-528)
                MenuService.Request hall = new MenuService.Request(MenuService.Kind.HALL_OF_FAME);
                if (menuService != null) menuRequest = menuService.submit(hall);
                else log.warn("HALL_OF_FAME_ENTRY without a menu service");
                break;
            }
            case "DAYCARE_DEPOSIT": {                                     // 181_PField_DayCare:57-70
                state.trainer().dayCare.deposit(state.trainer(), irNumber(ir.get("index")));
                break;
            }
            case "DAYCARE_WITHDRAW": {                                    // :72-83
                state.trainer().dayCare.withdraw(state.trainer(), irNumber(ir.get("index")));
                break;
            }
            case "DAYCARE_GENERATE_EGG": {                                // :187-465
                if (pbs == null) break;
                state.trainer().dayCare.generateEgg(state.trainer(), pbs, new java.util.Random(), new pokemon.runtime.pokemon.DayCare.World() {
                    public int region() { return regionSupplier.getAsInt(); }
                    public boolean hasItem(String item) { return state.inventory().has(item); }
                    public int mapId() { return mapId; }
                });
                break;
            }
            case "DAYCARE_GET_DEPOSITED": {                               // :14-23
                pokemon.runtime.pokemon.DayCare dayCare = state.trainer().dayCare;
                int index = irNumber(ir.get("index"));
                Pokemon pkmn = dayCare.get(index);
                if (pkmn == null) break;                                  // return false
                int nameVariable = irInt(ir, "nameVariable", -1), costVariable = irInt(ir, "costVariable", -1);
                if (nameVariable >= 0) state.variables().setText(nameVariable, pkmn.name);
                if (costVariable >= 0) state.variables().set(costVariable, dayCare.cost(index));
                break;
            }
            case "DAYCARE_COMPAT": {                                      // :182-184
                state.variables().set(irInt(ir, "variable", 0), state.trainer().dayCare.compat());
                break;
            }
            case "DAYCARE_RESET_EGG": {                                   // $PokemonGlobal.daycareEgg=0; daycareEggSteps=0
                state.trainer().dayCare.egg = 0;
                state.trainer().dayCare.eggSteps = 0;
                break;
            }
            case "DAYCARE_CHOOSE": {                                      // :85-109
                String text = ir.getString("text", "");
                int variable = irInt(ir, "variable", 0);
                startFieldTask(scene -> {
                    pokemon.runtime.pokemon.DayCare dayCare = state.trainer().dayCare;
                    int count = dayCare.deposited();
                    if (count == 0) {
                        throw new IllegalStateException("There's no Pokémon here...");
                    } else if (count == 1) {
                        state.variables().set(variable, dayCare.pokemon[0] != null ? 0 : 1);
                    } else {
                        java.util.List<String> choices = dayCare.choices();
                        int command = scene.pbMessage(text, choices, choices.size());
                        state.variables().set(variable, command == 2 ? -1 : command);
                    }
                });
                break;
            }
            case "TEACH_EGG_MOVES":
                startEggMoveTutor(false);
                break;
            case "CHANGE_SHINY":
                startEggMoveTutor(true);
                break;
            case "OPEN_PC":
                if (menuService != null) menuRequest = menuService.submit(new MenuService.Request(MenuService.Kind.STORAGE));
                else log.warn("OPEN_PC without menu service");
                break;
            case "STARTER_SELECTION": {
                // DiegoWTsStarterSelection.new(pkmn1,pkmn2,pkmn3) (333_DiegoWT:30-).
                MenuService.Request starter = new MenuService.Request(MenuService.Kind.STARTER);
                starter.dex = new int[3];
                JsonValue dexes = ir.get("dex");
                for (int i = 0; i < 3; i++) {
                    JsonValue entry = dexes == null ? null : dexes.get(i);
                    starter.dex[i] = entry == null ? 0 : entry.asInt();
                }
                if (menuService != null) menuRequest = menuService.submit(starter);
                else log.warn("STARTER_SELECTION without a menu service");
                break;
            }
            case "OPEN_MART": {
                // pbPokemonMart(stock, speech, cantsell) (230_PScreen_Mart:807-846).
                MenuService.Request mart = new MenuService.Request(MenuService.Kind.MART);
                mart.items = new java.util.ArrayList<>();
                JsonValue stock = ir.get("items");
                if (stock != null && stock.isArray()) {
                    for (JsonValue item = stock.child; item != null; item = item.next) {
                        if (item.isString()) {
                            mart.items.add(item.asString());
                        }
                    }
                }
                mart.speech = ir.has("speech") ? ir.getString("speech") : null;
                mart.cantSell = irBoolean(ir, "cantSell", false);
                if (menuService != null) menuRequest = menuService.submit(mart);
                else log.warn("OPEN_MART without a menu service");
                break;
            }
            case "SET_MART_PRICE": // setPrice / setSellPrice (230_PScreen_Mart:891-904)
                state.martPrices().setPrice(ir.getString("item", ""), ir.getInt("buy", -1), ir.getInt("sell", -1));
                break;
            case "SHOW_MAP": {
                // pbShowMap(region, wallmap) (PScreen_RegionMap:431-437).
                MenuService.Request mapRequest = new MenuService.Request(MenuService.Kind.SHOW_MAP);
                mapRequest.region = irInt(ir, "region", -1);
                mapRequest.wallmap = irBoolean(ir, "wallmap", true);
                if (menuService != null) menuRequest = menuService.submit(mapRequest);
                else log.warn("SHOW_MAP without a menu service");
                break;
            }
            case "CHOOSE_TRADE": {
                MenuService.Request request = new MenuService.Request(MenuService.Kind.CHOOSE_TRADE);
                request.variable = irInt(ir, "variable", 0); request.nameVariable = irInt(ir, "nameVariable", 0);
                request.wanted = ir.getString("wanted", "");
                if (request.variable > 0) state.variables().set(request.variable, -1);
                if (request.nameVariable > 0) state.variables().setText(request.nameVariable, "");
                if (menuService != null) menuRequest = menuService.submit(request);
                else log.warn("CHOOSE_TRADE without menu service");
                break;
            }
            case "CHOOSE_POKEMON": {
                // pbChoosePokemon(variable, nameVariable, proc, allowIneligible) (252_PSystem_PokemonUtilities:246-266)
                MenuService.Request request = new MenuService.Request(MenuService.Kind.CHOOSE_ABLE);
                request.variable = irInt(ir, "variable", 0); request.nameVariable = irInt(ir, "nameVariable", 0);
                request.ableProc = ir.getString("proc", "");
                request.allowIneligible = irBoolean(ir, "allowIneligible", false);
                if (menuService != null) menuRequest = menuService.submit(request);
                else log.warn("CHOOSE_POKEMON without menu service");
                break;
            }
            case "START_TRADE": {
                // pbStartTrade (227_PScreen_Trading:192-226): the Pokemon the player gets, then the trade scene
                Object index = resolveIrValue(ir.get("index")), offered = resolveIrValue(ir.get("offered"));
                int slot = index instanceof Number ? ((Number) index).intValue() : -1;
                Pokemon mine = state.trainer().party.get(slot);
                if (mine == null || pbs == null || menuService == null) break;
                String trainerName = ir.getString("trainerName", "训练家");
                Pokemon yours = pokemon.runtime.ui.menu.TradeModel.prepare(state.trainer(), mine, offered,
                        ir.getString("nickname", ""), trainerName, pbs, new java.util.Random());
                if (yours == null) { log.warn("START_TRADE: invalid offered Pokemon"); break; }
                MenuService.Request request = new MenuService.Request(MenuService.Kind.TRADE);
                request.index = slot; request.offered = yours;
                request.nickname = state.trainer().name;                    // @trader1
                request.trainerName = trainerName;                          // @trader2
                menuRequest = menuService.submit(request);
                break;
            }
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
                // get_character(n).setTempSwitchOn(c) names another event of this map (048_Interpreter:400-412).
                int tempSwitchEvent = ir.getInt("eventId", eventId);
                if (tempSwitchEvent < 0) {
                    log.warn("SET_TEMP_SWITCH outside an event; skipped");
                } else {
                    state.tempSwitches().set(mapId, tempSwitchEvent, irString(ir, "channel", "A"),
                            irBoolean(ir, "value", true));
                }
                break;
            case "TONE_CHANGE_ALL": { // pbToneChangeAll (171_PField_Visuals:691-696)
                float red = ir.getFloat("red", 0f);
                float green = ir.getFloat("green", 0f);
                float blue = ir.getFloat("blue", 0f);
                float gray = ir.getFloat("gray", 0f);
                int duration = ir.getInt("duration", 0);
                if (screenEffects != null) {
                    screenEffects.tint(red, green, blue, gray, duration);          // :692 $game_screen.start_tone_change
                }
                if (pictures != null) {
                    for (PictureService.Picture picture : pictures.values()) {      // :693-695 every picture
                        pictures.tone(picture.id, red, green, blue, gray, duration);
                    }
                }
                break;
            }
            case "GIVE_RIBBON_PARTY": { // for i in $Trainer.pokemonParty: i.giveRibbon(:X) (197_PokeBattle_Pokemon:571-576)
                String ribbon = ir.getString("ribbon", null);
                for (Pokemon member : state.trainer().party.members()) {
                    if (ribbon != null && !member.egg) {
                        member.giveRibbon(ribbon);
                    }
                }
                break;
            }
            case "GIVE_RIBBON_FIRST": {   // poke=$Trainer.firstPokemon; poke.giveRibbon(:X)
                Pokemon first = state.trainer().first();
                if (first != null) first.giveRibbon(ir.getString("ribbon", ""));
                break;
            }
            case "EV_RIBBON_STATUS": {    // map-025 Effort ribbon giver (the lead's total EVs and whether it has the ribbon)
                Pokemon first = state.trainer().first();
                if (first == null) break;
                int ev = 0;
                for (int i = 0; i < 6; i++) ev += first.evs[i];
                int maxed = ev >= 255 ? 1 : 0;
                if (first.hasRibbon(ir.getString("ribbon", ""))) maxed = 2;
                state.variables().setText(1, first.name);
                state.variables().set(2, maxed);
                break;
            }
            case "SET_WEATHER": // $game_screen.weather(type, power, duration) (019_Game_Screen:78-93)
                state.weather().set(ir.getInt("type", 0), ir.getInt("power", 0), ir.getInt("duration", 0));
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
                String shown = state.quests().activateQuest(quest, QuestLog.DEFAULT_COLOR, false, questMapName(),
                        questTable.maxStages(quest), System.currentTimeMillis());          // 282_002_Quest_Main:42-64
                if (shown != null) {
                    showHandlerMessage(shown);
                    return;
                }
                break;
            }
            case "ADVANCE_QUEST_TO_STAGE": {
                String quest = ir.getString("quest", "");
                JsonValue stage = ir.get("stage");
                int number = stage == null || !stage.isNumber() ? state.quests().stage(quest) + 1 : stage.asInt();
                String shown = state.quests().advanceQuestToStage(quest, number, null, false, questMapName(),
                        questTable.maxStages(quest), System.currentTimeMillis());          // :141-166
                if (shown != null) {
                    showHandlerMessage(shown);
                    return;
                }
                break;
            }
            case "COMPLETE_QUEST": {
                String quest = ir.getString("quest", "");
                String shown = state.quests().completeQuest(quest, null, false, questMapName(),
                        questTable.maxStages(quest), System.currentTimeMillis());          // :103-139
                if (shown != null) {
                    showHandlerMessage(shown);
                    return;
                }
                break;
            }
            case "FAIL_QUEST": {
                String quest = ir.getString("quest", "");
                String shown = state.quests().failQuest(quest, null, false, questMapName(),
                        questTable.maxStages(quest), System.currentTimeMillis());          // :66-101
                if (shown != null) {
                    showHandlerMessage(shown);
                    return;
                }
                break;
            }
            case "GIVE_KEY_ITEM": {
                String item = ir.getString("item", "");
                int amount = ir.getInt("amount", 1);
                // BW Get Key Item:197-201 / :174: a String item is a "fake" item
                // (an icon animation only); pbReceiveItem runs for real PBS items.
                boolean known = pbs != null && pbs.item(item) != null;
                keyItem = new KeyItemAnimation(known ? item : null, known ? null : item);
                keyItemAfter = known && inventory != null ? () -> giveItemWithMessages(item, amount, false) : null;
                waitTimer = keyItem.duration();
                interpreterState = InterpreterState.WAIT_TIME;
                return;
            }
            case "SET_BADGE": {
                // $Trainer.badges[i] = true (PokeBattle_Trainer:7); the badge
                // indices are 0-based (TrainerView:134).
                int badge = ir.getInt("badge", -1);
                boolean value = irBoolean(ir, "value", true);
                if (badge < 0) {
                    log.warn("SET_BADGE without an index; skipped");
                } else if (value) {
                    state.trainer().badges.add(badge);
                } else {
                    state.trainer().badges.remove(badge);
                }
                break;
            }
            case "SET_TRAINER_FLAG": {
                // $Trainer.pokedex / $Trainer.pokepc (PokeBattle_Trainer:15/23);
                // both gate their pause menu entries (Modular Menu:67/82).
                String flag = ir.getString("flag", "");
                boolean value = irBoolean(ir, "value", true);
                if ("pokedex".equals(flag)) {
                    state.trainer().pokedex = value;
                } else if ("pokepc".equals(flag)) {
                    state.trainer().pokepc = value;
                } else {
                    log.warn("SET_TRAINER_FLAG with an unknown flag " + flag + "; skipped");
                }
                break;
            }
            // ---- R8: the boss reward system (Boss_reward:3-348) ----
            case "BOSS_REWARD": {
                // boss_reward(rank): one random reward set of the rank (:62-64).
                int rank = Math.max(2, Math.min(7, ir.getInt("rank", 5)));
                BossRewardsData.Reward[][] sets = BossRewardsData.rankRewards(rank);
                BossRewardsData.Reward[] set = sets[random.nextInt(sets.length)];
                for (BossRewardsData.Reward reward : set) {
                    if (inventory != null) {
                        inventory.add(reward.item, reward.quantity);
                    }
                    log.warn("boss reward " + reward.item + " x" + reward.quantity);
                }
                playItemGetSe();
                showHandlerMessage("击败了" + BossRewardsData.rankName(rank) + "，获得了奖励道具！");
                return; // the message owns the cursor
            }
            case "BOSS_BLISSEY": {
                // BossRewards.blissey (:80-95): random exp into the exp pot.
                int exp;
                switch (random.nextInt(5) + 1) {
                    case 1: exp = 3000 * 100; break;
                    case 2: exp = 3000 * 150; break;
                    case 3: exp = 10000 * 100; break;
                    case 4: exp = 10000 * 150; break;
                    default: exp = 30000 * 100; break;
                }
                addExpPot(exp, true);
                if (interpreterState == InterpreterState.WAIT_MESSAGE) {
                    return;
                }
                break;
            }
            case "BOSS_GHOLDENGO_MONEY": {
                // BossRewards.gholdengo_money (:121-136).
                int money;
                switch (random.nextInt(5) + 1) {
                    case 1: case 2: money = 5000; break;
                    case 3: money = 40000; break;
                    case 4: money = 25000; break;
                    default: money = 500000; break;
                }
                state.trainer().money += money;
                playItemGetSe();
                pendingMessages.add("赛富豪送了你" + money + "元钱！");
                pendingMessages.add("当前金钱：" + state.trainer().money + "元");
                showHandlerMessage(pendingMessages.poll());
                return;
            }
            case "BOSS_POKEMON_REWARD": {
                // BossRewards.pokemon_reward (:304-346): 1-3 Pokemon from the
                // pools; 40/30/20/10% common/rare/epic/legend (:310-327).
                int count = 1 + random.nextInt(3);
                playItemGetSe();
                pendingMessages.add("击败了BOSS！获得了" + count + "只宝可梦！");
                for (int i = 0; i < count; i++) {
                    int roll = random.nextInt(1000);
                    BossRewardsData.PoolEntry[] pool;
                    String rarity;
                    int minLevel, maxLevel;
                    if (roll < 400) {
                        pool = BossRewardsData.COMMON_POOL; rarity = "普通";
                        minLevel = 5; maxLevel = 15;
                    } else if (roll < 700) {
                        pool = BossRewardsData.RARE_POOL; rarity = "稀有";
                        minLevel = 10; maxLevel = 25;
                    } else if (roll < 900) {
                        pool = BossRewardsData.EPIC_POOL; rarity = "史诗";
                        minLevel = 20; maxLevel = 40;
                    } else {
                        pool = BossRewardsData.LEGEND_POOL; rarity = "传说";
                        minLevel = 40; maxLevel = 60;
                    }
                    BossRewardsData.PoolEntry entry = pool[random.nextInt(pool.length)];
                    int level = minLevel + random.nextInt(maxLevel - minLevel + 1);
                    pendingMessages.add("【" + rarity + "】第" + (i + 1) + "只：获得了 "
                            + BossRewardsData.speciesName(entry.species, entry.form) + " Lv." + level);
                    addBossPokemon(entry, level);
                }
                showHandlerMessage(pendingMessages.poll());
                return;
            }
            case "GIVE_POKEMON": {
                // pbAddPokemon(species, level) (PSystem_PokemonUtilities:67-83).
                String speciesName = ir.getString("species", "");
                int level = ir.getInt("level", 5);
                if (pbs == null) {
                    log.warn("GIVE_POKEMON without the PBS data; skipped");
                    break;
                }
                PbsData.Species species = pbs.species(speciesName);
                if (species == null) {
                    log.warn("GIVE_POKEMON unknown species " + speciesName + "; skipped");
                    break;
                }
                Pokemon gifted = newPkmn(species, level);                             // pbNewPkmn
                if (pbAddPokemon(gifted, false)) {                               // :67-83 pbAddPokemon
                    log.warn("received " + speciesName + " L" + level);
                }
                return;
            }
            // ---- R8: partner trainers (PField_Field:1399-1440) ----
            case "REGISTER_PARTNER": {
                // pbRegisterPartner: load the trainer's party, stamp it with the
                // partner's foreign id and store $PokemonGlobal.partner.
                String trainerType = ir.getString("trainerType", "");
                String trainerName = ir.getString("name", "");
                int partyId = ir.getInt("partyId", 0);
                if (pbs == null) {
                    log.warn("REGISTER_PARTNER without the PBS data; skipped");
                    break;
                }
                PbsData.TrainerData partnerData = pbs.trainer(trainerType, trainerName, partyId);
                if (partnerData == null) {
                    log.warn("REGISTER_PARTNER unknown trainer " + trainerType + " "
                            + trainerName + "; skipped");
                    break;
                }
                GameState.Partner partner = new GameState.Partner();
                partner.trainerType = trainerType;
                partner.name = trainerName;
                // PokeBattle_Trainer:43-58 setForeignID: an id different from the player's.
                do {
                    partner.id = random.nextInt();
                } while (partner.id == 0);
                HeadlessBattlePort builder = new HeadlessBattlePort(state.trainer(), () -> pbs, null, random);
                for (PbsData.TrainerPokemon member : partnerData.party) {
                    Pokemon pokemon = builder.trainerPokemon(member, pbs);
                    if (pokemon == null) {
                        continue;
                    }
                    pokemon.setTrainerID(partner.id);       // i.trainerID = trainerobject.id
                    pokemon.originalTrainer = partner.name; // i.ot = trainerobject.name
                    pokemon.hp = pokemon.maxHp();           // i.calcStats
                    partner.party.add(pokemon);
                }
                state.partner(partner);
                log.warn("partner registered: " + trainerName + " (" + partner.party.size + " Pokemon)");
                break;
            }
            case "DEREGISTER_PARTNER":
                state.partner(null);
                break;
            case "TOGGLE_FOLLOWING_POKEMON": {
                // 297_Follower_Main:26-43 pbToggleFollowingPokemon: the map screen owns the follower; without one
                // (a headless run) only the flag changes.
                String forced = ir.getString("forced", null);
                if (mapPort != null && mapPort.toggleFollower(forced)) {
                    break;
                }
                if ("on".equalsIgnoreCase(forced)) {
                    state.followerToggled(true);
                } else if ("off".equalsIgnoreCase(forced)) {
                    state.followerToggled(false);
                } else {
                    state.followerToggled(!state.followerToggled());
                }
                break;
            }
            case "POKEMON_FOLLOW": {
                // 297_Follower_Main:48-63 pbPokemonFollow(x): event x becomes the following Pokemon.
                int target = ir.getInt("target", 0);
                if (target == 0) target = eventId;
                if (mapPort == null || !mapPort.startFollowing(target)) {
                    log.warn("POKEMON_FOLLOW " + target + ": no map screen or no able Pokemon; skipped");
                }
                break;
            }
            case "REMOVE_DEPENDENCIES":
                if (mapPort != null) mapPort.removeDependencies(ir.getBoolean("exceptFollower", false));
                break;
            case "ADD_DEPENDENCY": {
                int target = ir.getInt("event", 0);
                if (target == 0) target = eventId;                                    // @event_id
                if (mapPort == null || !mapPort.addDependency(target, ir.getString("name", ""), ir.getInt("commonEvent", -1))) {
                    log.warn("ADD_DEPENDENCY " + target + " skipped");
                }
                break;
            }
            case "TALK_TO_FOLLOWER": {
                MapPort.FollowerTalkPlan plan = mapPort == null ? null : mapPort.talkToFollower();
                if (plan != null) {
                    startFollowerTalk(plan);
                }
                break;
            }
            case "GIVE_ITEM": {
                String item = ir.getString("item", "");
                int amount = ir.getInt("amount", 1);
                String mode = ir.getString("mode", "");
                if (inventory != null && !mode.isEmpty() && pbs != null && pbs.item(item) != null && amount >= 1) {
                    giveItemWithMessages(item, amount, "ball".equals(mode));
                    if (interpreterState == InterpreterState.WAIT_MESSAGE) {
                        return;                                         // the messages own the cursor now
                    }
                    break;
                }
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
                noteMessageExtras(parsed);
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
                if (exiting) {
                    state.fieldGlobals().escapePoint = new int[0];                   // 171_PField_Visuals:616-619 pbCaveExit: pbEraseEscapePoint
                } else {
                    setEscapePoint();                                                // :611-614 pbCaveEntrance: pbSetEscapePoint
                }
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
            case "SET_BRIDGE": {
                // PField_Field:1363-1369: pbBridgeOn(height=2) sets
                // $PokemonGlobal.bridge to its height, pbBridgeOff to 0.
                // Every event of this project calls pbBridgeOn without an
                // argument (script-event audit: one argument shape), so the IR
                // carries the flag and the Ruby default height applies here.
                state.bridge(ir.getBoolean("on", false) ? DEFAULT_BRIDGE_HEIGHT : 0);
                break;
            }
            // ---- P3: berry plants (PField_BerryPlants:313-590) ----
            case "BERRY_PLANT": {
                if (mapPort == null || eventId < 0 || pbs == null) {
                    log.warn("BERRY_PLANT without a map port, event or PBS data; skipped");
                    break;
                }
                berry = new BerryInteraction(state, messages, input, pbs, inventory, mapPort,
                        mapId, eventId, messageColumns, this::submitItemChoice, log);
                berry.begin();
                if (berry.finished()) {
                    berry = null; // nothing to show (e.g. an unknown preset berry)
                    break;
                }
                interpreterState = InterpreterState.WAIT_BERRY;
                return; // the berry stepper owns the event until it ends
            }
            case "BERRY_PICK": {
                if (mapPort == null || eventId < 0 || pbs == null) {
                    log.warn("BERRY_PICK without a map port, event or PBS data; skipped");
                    break;
                }
                berry = new BerryInteraction(state, messages, input, pbs, inventory, mapPort,
                        mapId, eventId, messageColumns, this::submitItemChoice, log);
                berry.beginPick(ir.getString("berry", ""), ir.getInt("qty", 1));
                if (berry.finished()) {
                    berry = null;
                    break;
                }
                interpreterState = InterpreterState.WAIT_BERRY;
                return;
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
                } else if (value instanceof String) {
                    state.variables().setText(variableId, (String) value);
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
            case "REPEAT": {
                // P0d: Ruby `for <var> in <from>..<to> ... end` (the ball-shake
                // catch loop of the common events). `...` excludes the last
                // value, exactly like the Ruby range.
                JsonValue steps = ir.get("steps");
                Object from = resolveIrValue(ir.get("from"));
                Object to = resolveIrValue(ir.get("to"));
                if (steps == null || !steps.isArray() || steps.size == 0) {
                    break;
                }
                if (!(from instanceof Number) || !(to instanceof Number)) {
                    log.warn("REPEAT with a non-numeric range; skipped");
                    break;
                }
                int first = ((Number) from).intValue();
                int last = ((Number) to).intValue() - (ir.getBoolean("exclusive", false) ? 1 : 0);
                String local = ir.getString("local", "i");
                setLocal(local, first);
                repeatFrames.add(new RepeatFrame(steps, local, first, last));
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
                setLocal(local, newPkmn(species, level));
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
                pbAddPokemon(pokemon, ir.getBoolean("silent", false));          // :67-101 pbAddPokemon / pbAddPokemonSilent
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
                Pokemon egg = newPkmn(species, 1); // 361:5 pbNewPkmn(egg, EGG_LEVEL)
                egg.egg = true;
                egg.name = "神秘的蛋";                                               // 361:10
                egg.stepsToHatch = species.stepsToHatch;                             // 361:8-11
                egg.obtainText = ir.getString("text", "");                           // 361:12
                egg.hp = egg.maxHp();                                                // 361:13 calcStats
                pokemon.runtime.pokemon.TrainerState trainer = state.trainer();
                if (trainer.party.size() < 6) {                                      // 361:14
                    trainer.party.add(egg);                                          // 361:15
                    showHandlerMessage(egg.name + "加入了队伍。");                     // 361:16
                } else {
                    pokemon.runtime.pokemon.Storage storage = trainer.currentStorage();
                    int oldBox = storage.currentBox;                                 // 361:18
                    int storedBox = storage.pbStoreCaught(egg);                      // 361:19
                    if (storedBox < 0) {
                        log.warn("ADD_EGG: the boxes are full");
                        break;
                    }
                    String curboxname = storage.box(oldBox).name;                    // 361:20
                    String boxname = storage.box(storedBox).name;                    // 361:21
                    // 登记: pbGetStorageCreator (the PC owner's name) is not modelled, so the "someone's PC" lines are used
                    if (storedBox != oldBox) {                                       // 361:24
                        pendingMessages.add(egg.name + "被传送到了盒子 \"" + boxname + ".\"");   // 361:30
                        showHandlerMessage("某人的电脑上的盒子 \"" + curboxname + "\"已经满了。");   // 361:28
                    } else {
                        pendingMessages.add("被存放在盒子\"" + boxname + ".\"");        // 361:36
                        showHandlerMessage(egg.name + "被传送到了某人的电脑。");   // 361:34
                    }
                }
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
                if (doubleRefusalFirst()) return;
                applyBattleSwitches();
                // PField_Battles:343 + :359-361: the outcome variable (1 by
                // default) receives the decision when the battle is over.
                pendingOutcomeVar = prepareWildBattle(ir);
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
                if (doubleRefusalFirst()) return;
                applyBattleSwitches();
                pendingOutcomeVar = prepareWildBattle(ir);
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
                // PField_Battles:582-596 pbDoubleTrainerBattle: a second opposing trainer and the double rule.
                java.util.List<PbsData.TrainerData> opponents = new java.util.ArrayList<>();
                opponents.add(trainer);
                com.badlogic.gdx.utils.JsonValue second = ir.get("second");
                if (second != null) {
                    PbsData.TrainerData trainer2 = pbs.trainer(second.getString("trainerType", ""),
                            second.getString("trainerName", ""), second.getInt("version", 0));
                    if (trainer2 == null) {
                        log.warn("TRAINER_BATTLE unknown second trainer; skipped");
                        break;
                    }
                    opponents.add(trainer2);
                }
                // PField_Battles:598-614 pbTripleTrainerBattle: a third opposing trainer and the triple rule.
                com.badlogic.gdx.utils.JsonValue third = ir.get("third");
                if (third != null) {
                    PbsData.TrainerData trainer3 = pbs.trainer(third.getString("trainerType", ""),
                            third.getString("trainerName", ""), third.getInt("version", 0));
                    if (trainer3 == null) {
                        log.warn("TRAINER_BATTLE unknown third trainer; skipped");
                        break;
                    }
                    opponents.add(trainer3);
                }
                if (doubleRefusalFirst()) return;
                if (second == null && deferToSecondTrainer(trainer)) {             // PField_Battles:534-558
                    break;                                                         // pbTrainerBattle returns false
                }
                trainerWithWaiting(opponents);                                     // PField_Battles:564-569
                if (ir.getBoolean("double", false)) {
                    state.battleRules().record("double", null);                    // :588 setBattleRule("double")
                }
                if (ir.getBoolean("triple", false)) {
                    state.battleRules().record("triple", null);                    // :605 setBattleRule("triple")
                }
                if (ir.getBoolean("canLose", false)) {
                    state.battleRules().record("canLose", null);                   // :587 setBattleRule("canLose") if canLose
                }
                if (ir.has("outcomeVar")) {
                    state.battleRules().record("outcomeVar", ir.getInt("outcomeVar", 1));   // :586
                }
                // PField_Battles:497-498 + 510-516: the recorded rules apply to
                // this battle and are consumed when it starts; the decision
                // lands in the outcome variable (1 by default).
                pendingOutcomeVar = prepareTrainerBattle();
                applyBattleSwitches();
                BattleResult result = battlePort.trainerBattle(opponents);
                log.warn("trainer battle vs " + trainer.name + " -> "
                        + (result == null ? "not started" : result.outcome));
                break;
            }
            case "BATTLE_RULE": {
                // PField_Battles:60-77: setBattleRule records rules for the next
                // battle; the value-taking rules carry their value as a literal.
                JsonValue rules = ir.get("rules");
                if (rules == null || !rules.isArray()) {
                    break;
                }
                for (JsonValue rule = rules.child; rule != null; rule = rule.next) {
                    String ruleName = rule.getString("rule", "");
                    Object value = rule.has("value") ? resolveIrValue(rule.get("value")) : null;
                    if (!state.battleRules().record(ruleName, value)) {
                        log.warn("battle rule \"" + ruleName + "\" does not exist; ignored");
                    }
                }
                break;
            }
            case "TRAINER_INTRO": {
                // PField_Field:793-799: pbGlobalLock + the trainer type's intro
                // ME (trainertypes column 6; empty in this project).
                if (mapPort != null) {
                    mapPort.lockEvents(true);
                }
                String type = ir.getString("trainerType", "");
                PbsData.TrainerType trainerType = pbs == null ? null : pbs.trainerTypes.get(type);
                if (trainerType != null && trainerType.introMe != null
                        && !trainerType.introMe.isEmpty() && audio != null) {
                    audio.playMe(trainerType.introMe, 100, 100);
                }
                break;
            }
            case "TRAINER_END": {
                // PField_Field:801-805: pbGlobalUnlock + erase the running event's route.
                if (mapPort != null) {
                    mapPort.lockEvents(false);
                    mapPort.eraseRoute(eventId);
                }
                break;
            }
            case "ROCK_SMASH_ENCOUNTER": {
                // pbRockSmashRandomEncounter (179_PField_FieldMoves:604-609): rand(100)<25 -> pbEncounter(RockSmash).
                if (battlePort == null || pbs == null) {
                    log.warn("ROCK_SMASH_ENCOUNTER without a battle runtime or PBS data; skipped");
                    break;
                }
                if (encounterRandom.nextInt(100) >= 25) {
                    break;                                                    // :605
                }
                pbEncounter("RockSmash");
                break;
            }
            // ---- player graphics (the intro's gender selector) ----
            case "GENDER_SELECTOR": {
                if (state.playerId() < 0 && menuService != null) {
                    menuRequest = menuService.submit(new MenuService.Request(MenuService.Kind.GENDER));
                    break;
                }
                // The BWGenderSelector UI (genderselect.png + name entry) is P4;
                // the stopgap applies the male player so the intro does not leave
                // the hero without a walking graphic. pbChangePlayer sets it.
                if (state.playerId() >= 0) {
                    break; // already chosen (a load or a second page)
                }
                state.playerId(0);
                state.trainer().gender = PokemonStats.MALE;
                log.warn("gender selector: default male player (full UI arrives in P4)");
                break;
            }
            case "CHANGE_PLAYER": {
                int playerId = irInt(ir, "playerId", 0);
                if (playerId < 0 || playerId > 7) {
                    log.warn("CHANGE_PLAYER " + playerId + " is out of range; skipped");
                    break;
                }
                state.playerId(playerId);
                state.trainer().gender = playerId == 1 ? PokemonStats.FEMALE : PokemonStats.MALE;
                log.warn("player graphic -> id " + playerId);
                break;
            }
            default:
                log.warn("IR command " + name
                        + " needs a runtime service that does not exist yet; skipped");
                break;
        }
    }

    /**
     * {@code pbEncounter(enctype)} (175_PField_Encounters:484-499): rolls a wild Pokemon of the map's table for the
     * method and starts the battle (two when a partner walks with the player). {@code "*"} is the Sweet Scent form:
     * the method of the place ({@code pbEncounterType}), nothing when no encounter is possible here.
     *
     * @return true when a battle started
     */
    private boolean pbEncounter(String enctype) {
        if (battlePort == null || pbs == null) {
            log.warn("pbEncounter " + enctype + " without a battle runtime or PBS data; skipped");
            return false;
        }
        PokemonEncounters wild = new PokemonEncounters(pbs, state, encounterRandom, java.time.LocalTime::now);
        wild.setup(state.currentMapId());
        String method = enctype;
        if ("*".equals(enctype)) {                                              // 179_PField_FieldMoves:840-842
            method = wild.pbEncounterType();
            if (method == null || !wild.isEncounterPossibleHere(state.fieldGlobals().playerTerrainTag)) {
                return false;
            }
        }
        PokemonEncounters.Encounter encounter = wild.pbEncounter(method);
        if (encounter == null) {
            return false;                                                       // :490 return false if !encounter1
        }
        BattleResult result;
        if (state.partner() != null) {                                          // :491 $PokemonGlobal.partner
            PokemonEncounters.Encounter second = wild.pbEncounter(method);
            if (second == null) {
                return false;                                                   // :494
            }
            result = battlePort.doubleWildBattle(encounter.species, encounter.level, second.species, second.level);
        } else {
            result = battlePort.wildBattle(encounter.species, encounter.level);
        }
        log.warn("encounter " + method + " -> " + (result == null ? "not started" : result.outcome));
        return true;
    }

    /**
     * 179_PField_FieldMoves {@code pbUseHiddenMove(pokemon, move)}: the move's {@code UseMove} body runs as an event of
     * its own (messages, the banner, fades, battles), started after the menus have closed.
     */
    public void startHiddenMove(Pokemon pkmn, String move) {
        Array<EventCommand> list = new Array<>();
        list.add(syntheticCommand(0, 0));
        start(list, state.currentMapId(), -1);
        pokemon.runtime.field.BlockingTask[] holder = new pokemon.runtime.field.BlockingTask[1];
        TaskFieldScene scene = new TaskFieldScene(request -> holder[0].call(request));
        HiddenMoveTask task = new HiddenMoveTask(state, pbs, mapPort, scene, this::pbEncounter, encounterRandom, audio);
        scriptTaskResult = null;
        holder[0] = new pokemon.runtime.field.BlockingTask(() -> {
            task.use(move, pkmn);
            scriptTaskResult = true;
        });
        scriptTaskDone = result -> {
        };
        scriptTaskAnswer = null;
        scriptQuestion = null;
        scriptTask = holder[0];
    }

    /**
     * {@code pbSetEscapePoint} (170_PField_Field:1370-1381): the tile in front of the cave mouth the player is leaving, seen from
     * the way out - {@code [map, x, y, direction]}. The escape rope, Dig and the Infinite Rope take the player back there.
     */
    private void setEscapePoint() {
        int x = state.playerX();
        int y = state.playerY();
        int dir;
        switch (state.playerDirection()) {
            case 2: y -= 1; dir = 8; break;                                          // Down
            case 4: x += 1; dir = 6; break;                                          // Left
            case 6: x -= 1; dir = 4; break;                                          // Right
            default: y += 1; dir = 2; break;                                         // Up (8)
        }
        state.fieldGlobals().escapePoint = new int[] {state.currentMapId(), x, y, dir};
    }

    /**
     * 189_PItem_ItemEffects {@code pbUseKeyItemInField(item)}: the {@code UseInField} body of an item that acts on the map
     * (the fishing rod), started after the bag and the pause menu have closed.
     */
    public void startFieldItem(String item) {
        Array<EventCommand> list = new Array<>();
        list.add(syntheticCommand(0, 0));
        start(list, state.currentMapId(), -1);
        pokemon.runtime.field.BlockingTask[] holder = new pokemon.runtime.field.BlockingTask[1];
        TaskFieldScene scene = new TaskFieldScene(request -> holder[0].call(request));
        FieldItemTask task = new FieldItemTask(state, pbs, mapPort, scene, new FieldItemTask.Services() {
            @Override
            public boolean hasEncounter(String enctype) {
                if (pbs == null) return false;
                PokemonEncounters wild = new PokemonEncounters(pbs, state, encounterRandom, java.time.LocalTime::now);
                wild.setup(state.currentMapId());
                return wild.hasEncounter(enctype);
            }

            @Override
            public boolean pbEncounter(String enctype) {
                return EventInterpreter.this.pbEncounter(enctype);
            }
        }, encounterRandom);
        scriptTaskResult = null;
        holder[0] = new pokemon.runtime.field.BlockingTask(() -> {
            int ret = task.use(item);
            if (ret == 3 && inventory != null) {
                inventory.remove(item, 1);                                           // 188:978-980 pbUseKeyItemInField: 3 = used and consumed
            }
            scriptTaskResult = true;
        });
        scriptTaskDone = result -> {
        };
        scriptTaskAnswer = null;
        scriptQuestion = null;
        scriptTask = holder[0];
    }

    /**
     * 362_changeShiny {@code teachEggMoves}: the egg-move teacher NPC. The Ruby runs as a blocking script on a task of the
     * running event; the event goes on when it returns.
     */
    private int irNumber(JsonValue value) {
        Object resolved = resolveIrValue(value);
        return resolved instanceof Number ? ((Number) resolved).intValue() : -1;
    }

    /**
     * 189_PItem_ItemEffects:162-188: the repel ran out and the bag holds another one: ask, pick it in the bag and use it
     * ({@code pbUseItem}: a handler that answers 3 consumes the item).
     */
    public void startRepelRenewal() {
        startFieldTask(scene -> {
            pokemon.runtime.field.ItemHandlers handlers = new pokemon.runtime.field.ItemHandlers(pbs, state, java.time.LocalTime::now);
            if (!scene.pbConfirmMessage("使用的喷雾剂失去效果了！\n想再用一个吗？")) return;     // :172
            String item = scene.pbChooseItem(i -> "REPEL".equals(i) || "SUPERREPEL".equals(i) || "MAXREPEL".equals(i));   // :175-181
            if (item == null) return;                                                          // :183 pbUseItem if ret>0
            int ret = handlers.useInField(item, new EggMoveTutor(state, pbs, scene, handlers).itemSceneFor());   // :916-923
            if (ret == 3 || ret == 4) state.inventory().remove(item, 1);                       // 188:922-923
        });
    }

    private java.util.function.IntSupplier regionSupplier = () -> -1;

    /** {@code pbGetCurrentRegion} (253_PSystem_Utilities:840-843) for the Day Care's regional forms. */
    public void attachRegion(java.util.function.IntSupplier supplier) {
        regionSupplier = supplier;
    }

    /** Runs a blocking script of the running event on a task whose requests the interpreter answers. */
    private void startFieldTask(java.util.function.Consumer<TaskFieldScene> body) {
        pokemon.runtime.field.BlockingTask[] holder = new pokemon.runtime.field.BlockingTask[1];
        TaskFieldScene scene = new TaskFieldScene(request -> holder[0].call(request));
        scriptTaskResult = null;
        holder[0] = new pokemon.runtime.field.BlockingTask(() -> {
            body.accept(scene);
            scriptTaskResult = true;
        });
        scriptTaskDone = result -> {
        };
        scriptTaskAnswer = null;
        scriptQuestion = null;
        scriptTask = holder[0];
    }

    /** The internal name of the item with that id, or null ({@code getID}'s inverse). */
    private String itemById(int id) {
        if (pbs == null || id <= 0) return null;
        for (PbsData.Item item : pbs.items.values()) {
            if (item.id == id) return item.internalName;
        }
        return null;
    }

    private PbsData.Species speciesById(int id) {
        if (pbs == null || id <= 0) return null;
        for (String name : pbs.speciesById.values()) {
            PbsData.Species species = pbs.species(name);
            if (species != null && species.id == id) return species;
        }
        return null;
    }

    /** {@code pbLoadRegionalDexes.length}: the regional Dex lists of the PBS. */
    private int regionalDexCount() {
        int count = 0;
        if (pbs != null) {
            for (String name : pbs.speciesById.values()) {
                PbsData.Species species = pbs.species(name);
                if (species != null && species.regionalNumbers != null) count = Math.max(count, species.regionalNumbers.length);
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private java.util.List<String> chCommands() {
        Object value = instanceVariables.get("@ch_cmd");
        if (!(value instanceof java.util.List)) {
            value = new java.util.ArrayList<String>();
            instanceVariables.put("@ch_cmd", value);
        }
        return (java.util.List<String>) value;
    }

    /** 252_PSystem_PokemonUtilities:17-49 {@code pbStorePokemon}: the party, or a box with the plugin's lines. */
    private void pbStorePokemon(Pokemon pkmn, java.util.function.Consumer<String> say) {
        pokemon.runtime.pokemon.TrainerState trainer = state.trainer();
        if (trainer.party.size() == 6 && trainer.currentStorage().full()) {                           // :4-6 pbBoxesFull?
            say.accept("没有存放宝可梦的空间了！\u0001");                                              // :19
            say.accept("盒子都满了，无法接收宝可梦！");                                                 // :20
            return;
        }
        pkmn.recordFirstMoves();                                                                       // :22
        if (trainer.party.size() < 6) {                                                                // :23
            trainer.party.add(pkmn);                                                                   // :24
            return;
        }
        pokemon.runtime.pokemon.Storage storage = trainer.currentStorage();
        int oldBox = storage.currentBox;                                                               // :27
        int storedBox = storage.pbStoreCaught(pkmn);                                                   // :28
        String current = storage.box(oldBox).name;                                                     // :29
        String stored = storage.box(storedBox).name;                                                   // :30
        if (storedBox != oldBox) {                                                                     // :33
            say.accept("寄存系统中的箱子\"" + current + "\"已经满了！\u0001");                             // :37
            say.accept(pkmn.name + "被送到了箱子\"" + stored + "。\"");                                   // :39
        } else {
            say.accept(pkmn.name + "被送到了某人的电脑中。\u0001");                                       // :44
            say.accept("它被存放在了箱子\"" + stored + "中。\"");                                         // :46
        }
    }

    /** 323_Egg_Hatcher:228-257 {@code takeEgg(egg, index)}: where the newborn goes, then the slot is emptied. */
    private void takeEgg(TaskFieldScene scene, Pokemon egg, int index) {
        pokemon.runtime.pokemon.TrainerState trainer = state.trainer();
        boolean sel = scene.pbConfirmMessage("要将新出生的宝可梦放进队伍吗？");                              // :229
        if (sel) {
            if (trainer.party.size() == 6) scene.pbMessage("您的队伍满了！\u0001");                        // :231
            pbStorePokemon(egg, scene::pbMessage);                                                     // :232
        } else {
            if (trainer.party.size() == 6 && trainer.currentStorage().full()) {                        // :234 pbBoxesFull?
                scene.pbMessage("队伍没有更多的空间了！\u0001");                                          // :235
                scene.pbMessage("宝可梦盒子已满，不能再接受了！");                                          // :236
                return;                                                                                // the egg stays in its slot
            }
            pokemon.runtime.pokemon.Storage storage = trainer.currentStorage();
            int oldBox = storage.currentBox;                                                           // :239
            int storedBox = storage.pbStoreCaught(egg);                                                // :240
            String curboxname = storage.box(oldBox).name;                                              // :241
            String boxname = storage.box(storedBox).name;                                              // :242
            if (storedBox != oldBox) {                                                                 // :245
                scene.pbMessage("盒子 \"" + curboxname + "\" 已满。\u0001");                           // :249
                scene.pbMessage(egg.name + " 被转移到了盒子 \"" + boxname + "\"。");                     // :251
            } else {
                scene.pbMessage(egg.name + " 被转移到了盒子 \"" + boxname + "\"。");                     // :253
            }
        }
        trainer.hatcherEggs[index] = null;                                                             // :256
    }

    /** The hatched egg of a hatcher slot has been shown: say where it goes. */
    public void startTakeEgg(Pokemon egg, int slot) {
        Array<EventCommand> list = new Array<>();
        list.add(syntheticCommand(0, 0));
        start(list, state.currentMapId(), -1);
        startFieldTask(scene -> takeEgg(scene, egg, slot));
    }

    /**
     * 323_Egg_Hatcher:259-302 {@code pbGenerateEgg(pokemon, text="")}: with the Egg Hatcher the egg may go into it.
     * 登记: the first branch of the plugin ({@code PokeBattle_Pokemon.new(.., EGGINITIALLEVEL, ..)}) names an undefined
     * constant and always falls into the {@code rescue} branch, which is what runs here.
     */
    private void pbGenerateEgg(String speciesName, String text) {
        PbsData.Species species = pbs == null ? null : pbs.species(speciesName);
        if (species == null || state.trainer() == null) return;
        Pokemon egg = pokemon.runtime.pokemon.WildGenerator.pbNewPkmn(pbs, species, pokemon.runtime.pokemon.DayCare.EGG_LEVEL,
                state.trainer(), mapId, new java.util.Random());                                      // :284 pbNewPkmn(species, EGG_LEVEL)
        egg.egg = true;
        egg.name = "Egg";                                                                              // :288
        egg.stepsToHatch = species.stepsToHatch;                                                       // :289-290
        egg.obtainText = text;                                                                         // :291
        egg.hp = egg.maxHp();                                                                          // :292 calcStats
        boolean hatcherOwned = pbs.item("EGGHATCHER") == null || state.inventory().has("EGGHATCHER");   // :296
        startFieldTask(scene -> {
            if (hatcherOwned || state.switches().get(50)) {                                            // :296 EGGHATCHER_SWITCH
                if (scene.pbConfirmMessage("您想将蛋添加到孵化器中吗？")) {                                  // :297
                    boolean added = false;
                    for (int i = 0; i < state.trainer().hatcherEggs.length && !added; i++) {           // :305-311 addEgg
                        if (state.trainer().hatcherEggs[i] == null) {
                            state.trainer().hatcherEggs[i] = egg;
                            added = true;
                        }
                    }
                    if (added) return;                                                                 // :299 return true
                    scene.pbMessage("孵化器已满。");                                                     // :313
                }
            }
            pbStorePokemon(egg, scene::pbMessage);                                                     // :300
        });
    }

    /** The project's NPC scripts of {@link PluginScripts}. */
    private void startPluginScript(String command) {
        startFieldTask(scene -> {
            PluginScripts scripts = new PluginScripts(state, pbs, scene, mapPort);
            switch (command) {
                case "CRYSTAL_WARP": scripts.pbCrystalWarp(); break;
                case "MR_HYPER": scripts.pbMrHyper(); break;
                case "CHANGE_BALLS": scripts.changeBalls(); break;
                case "RESURRECTION2": scripts.resurrection2(new java.util.Random(), pkmn -> pbAddPokemon(pkmn, false)); break;
                default: scripts.pbGiveAllMemories(); break;
            }
        });
    }

    private void startEggMoveTutor(boolean shiny) {
        pokemon.runtime.field.BlockingTask[] holder = new pokemon.runtime.field.BlockingTask[1];
        TaskFieldScene scene = new TaskFieldScene(request -> holder[0].call(request));
        EggMoveTutor tutor = new EggMoveTutor(state, pbs, scene, new pokemon.runtime.field.ItemHandlers(pbs, state, java.time.LocalTime::now));
        scriptTaskResult = null;
        holder[0] = new pokemon.runtime.field.BlockingTask(() -> {
            if (shiny) tutor.changeShinyByNPC(); else tutor.teachEggMoves();
            scriptTaskResult = true;
        });
        scriptTaskDone = result -> {
        };
        scriptTaskAnswer = null;
        scriptQuestion = null;
        scriptTask = holder[0];
    }

    /**
     * P3: {@code pbChooseItemScreen(proc)} - the berry stepper asks the bag UI
     * for a filtered item and receives the result through {@link #update}'s
     * menuRequest branch.
     *
     * @return false when no bag UI is attached (the pick counts as cancelled)
     */
    private boolean submitItemChoice(java.util.function.Predicate<String> filter) {
        if (menuService == null) {
            return false;
        }
        MenuService.Request request = new MenuService.Request(MenuService.Kind.CHOOSE_ITEM);
        request.filter = filter;
        menuRequest = menuService.submit(request);
        return true;
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

    /**
     * Show Picture (231). The project's own {@code Interpreter#command_231}
     * defines the order as
     * {@code [number, name, origin, appointmentMode, x, y, zoomX, zoomY, opacity, blend]}:
     * x/y live at p[4]/p[5] and p[3] picks direct coordinates (0) or variable
     * ids (1). Reading p[3]/p[4] shifted every picture by one slot (the intro's
     * mapRegion at (336,224) landed at (0,336)).
     */
    private void showPicture(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        String name = stringParam(p, 1, null);
        if (pictures != null && name != null && !name.isEmpty()) {
            pictures.show(intParam(p, 0, 1), name, intParam(p, 2, 0),
                    pictureCoordinate(p, 4), pictureCoordinate(p, 5),
                    floatParam(p, 6, 100f), floatParam(p, 7, 100f),
                    floatParam(p, 8, 255f), intParam(p, 9, 0));
        }
        program.advance();
    }

    /**
     * Change Picture Color Tone (234), 048_Interpreter:1331-1339: picture number
     * {@code @parameters[0] + ($game_temp.in_battle ? 50 : 0)} (no battle here),
     * tone {@code @parameters[1]}, duration {@code @parameters[2]}.
     */
    private void changePictureTone(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        JsonValue tone = p == null ? null : p.get(1);
        if (pictures != null && tone != null && tone.isObject()) {
            pictures.tone(intParam(p, 0, 1), tone.getFloat("red", 0f), tone.getFloat("green", 0f),
                    tone.getFloat("blue", 0f), tone.getFloat("gray", 0f), intParam(p, 2, 0));
        }
        program.advance();
    }

    private void movePicture(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        int duration = intParam(p, 1, 0);
        if (pictures != null) {
            pictures.move(intParam(p, 0, 1), duration, intParam(p, 2, 0),
                    pictureCoordinate(p, 4), pictureCoordinate(p, 5),
                    floatParam(p, 6, 100f), floatParam(p, 7, 100f),
                    floatParam(p, 8, 255f), intParam(p, 9, 0));
        }
        program.advance();
        if (intParam(p, 10, 0) == 1 && duration > 0) {
            waitTimer = duration / 20f; // "wait for completion" flag
            interpreterState = InterpreterState.WAIT_TIME;
        }
    }

    /** x is p[4], y is p[5]; p[3]==1 means the value is a variable id. */
    private float pictureCoordinate(JsonValue p, int position) {
        if (intParam(p, 3, 0) == 0) {
            return floatParam(p, position, 0f);
        }
        int variableId = intParam(p, position, 0);
        return variableId >= 1 ? state.variables().get(variableId) : 0f;
    }

    private void erasePicture(EventProgram program, EventCommand command) {
        if (pictures != null) {
            pictures.erase(intParam(command.parameters, 0, 1));
        }
        program.advance();
    }

    /**
     * Interpreter:865-893 - one group assignment. The source skips a write that
     * cannot change the value (add/subtract at the ±99999999 cap, multiply and
     * divide by 1, remainder by 0 or 1) and clamps to ±99999999 afterwards;
     * {@code next} and "write the same value" are indistinguishable here, so
     * only the cases with a real difference keep the guard.
     */
    private static int applyOperation(int operation, int current, int operand) {
        final int limit = 99999999;
        int value;
        switch (operation) {
            case 1:
                value = current >= limit ? current : current + operand;
                break;
            case 2:
                value = current <= -limit ? current : current - operand;
                break;
            case 3:
                value = operand == 1 ? current : current * operand;
                break;
            case 4:
                value = operand == 0 || operand == 1 ? current : current / operand;
                break;
            case 5:
                // `next if value == 1 || value == 0`: remainder by 1 leaves the
                // variable alone instead of zeroing it.
                value = operand == 0 || operand == 1 ? current : current % operand;
                break;
            default:
                value = operand;
                break;
        }
        return Math.max(-limit, Math.min(limit, value));
    }

    // ------------------------------------------------------------------
    // Conditional Branch (section 29)
    // ------------------------------------------------------------------

    private static final java.util.regex.Pattern TRAINER_BATTLE_CONDITION =
            java.util.regex.Pattern.compile(
                "pbTrainerBattle\\(\\s*:([A-Za-z_][A-Za-z0-9_]*)\\s*,\\s*\"([^\"]*)\"\\s*"
                        + "(?:,\\s*nil\\s*,\\s*nil\\s*,\\s*(\\d+))?\\s*\\)");

    /**
     * PField_Battles:351-368 / FreeWildBattle:2-8: a wild battle used as a
     * script condition branches on {@code decision!=2 && decision!=5}.
     */
    private static final java.util.regex.Pattern WILD_BATTLE_CONDITION =
            java.util.regex.Pattern.compile(
                "pbWildBattle\\(\\s*:([A-Za-z_][A-Za-z0-9_]*)\\s*,\\s*(\\d+)\\s*\\)");

    // ------------------------------------------------------------------
    // Script conditions: a parsed Ruby expression, run on a task when an atom talks to the player
    // ------------------------------------------------------------------

    private final java.util.Map<String, ScriptCondition> conditionCache = new java.util.HashMap<>();
    /** The interpreter instance variables of the Ruby scripts ({@code @ch_ret}, {@code @ch_cmd}). */
    private final java.util.Map<String, Object> instanceVariables = new java.util.HashMap<>();
    private pokemon.runtime.field.BlockingTask scriptTask;
    private volatile Boolean scriptTaskResult;
    private java.util.function.Consumer<Boolean> scriptTaskDone;
    private java.util.function.Supplier<Object> scriptTaskAnswer;
    private java.util.List<String> scriptQuestion;
    private int scriptQuestionCancel;
    private int scriptQuestionDefault;

    private ScriptCondition parseCondition(String text) {
        ScriptCondition cached = conditionCache.get(text);
        if (cached != null || conditionCache.containsKey(text)) {
            return cached;
        }
        ScriptCondition parsed;
        try {
            parsed = ScriptCondition.parse(text);
        } catch (ScriptCondition.Unsupported error) {
            parsed = null;
        }
        conditionCache.put(text, parsed);
        return parsed;
    }

    private ConditionEnv conditionEnv(pokemon.runtime.field.FieldScene scene) {
        return new ConditionEnv(state, pbs, () -> mapId, () -> eventId, instanceVariables, java.time.LocalTime::now, scene)
                .withMapPort(mapPort).withPokemonAdder((pkmn, say) -> pbAddPokemon(pkmn, false, say))
                .withItemReceiver((item, say) -> {
                    if (pbs == null || inventory == null || pbs.item(item) == null) return false;
                    giveItemWithMessages(item, 1, false, say);                   // pbReceiveItem (309_Item_Find)
                    return true;
                });
    }

    private static boolean needsScreen(ScriptCondition condition) {
        for (String call : condition.calls()) {
            if (ConditionEnv.SCREEN_CALLS.contains(call)) {
                return true;
            }
        }
        return false;
    }

    /** Runs a condition whose atoms talk to the player on its own thread; the branch follows when it returns. */
    private void startConditionTask(EventProgram program, ScriptCondition condition) {
        pokemon.runtime.field.BlockingTask[] holder = new pokemon.runtime.field.BlockingTask[1];
        TaskFieldScene scene = new TaskFieldScene(request -> holder[0].call(request));
        ConditionEnv env = conditionEnv(scene);
        scriptTaskResult = null;
        holder[0] = new pokemon.runtime.field.BlockingTask(() -> {
            boolean value;
            try {
                value = ScriptCondition.truthy(condition.eval(env));
            } catch (ScriptCondition.Unsupported error) {
                log.warn("script condition atom not supported: " + error.getMessage());
                value = false;
            }
            scriptTaskResult = value;
        });
        scriptTaskDone = result -> {
            program.branchResult(program.indent(), result);
            if (result) {
                program.advance();
            } else {
                program.skipBlock();
            }
        };
        scriptTaskAnswer = null;
        scriptQuestion = null;
        scriptTask = holder[0];
    }

    /** One step of the running condition task: answer the last request, run to the next one and start it. */
    private void pumpScriptTask() {
        while (scriptTask != null && interpreterState == InterpreterState.RUNNING) {
            if (battlePort != null && battlePort.pending()) {
                return;                                                         // an action started a battle: it runs first
            }
            if (scriptQuestion != null) {                                       // the message is done: open the choices
                Array<String> options = new Array<>();
                for (String option : scriptQuestion) {
                    options.add(option);
                }
                choiceResult = Integer.MIN_VALUE;
                messages.showChoices(options, scriptQuestionCancel, scriptQuestionDefault);
                scriptQuestionDefault = 0;
                scriptQuestion = null;
                interpreterState = InterpreterState.WAIT_MESSAGE;
                return;
            }
            if (scriptTaskAnswer != null) {
                scriptTask.answer(scriptTaskAnswer.get());
                scriptTaskAnswer = null;
            }
            if (scriptTask.resume()) {
                java.util.function.Consumer<Boolean> done = scriptTaskDone;
                boolean result = Boolean.TRUE.equals(scriptTaskResult);
                scriptTask = null;
                scriptTaskDone = null;
                if (done != null) {
                    done.accept(result);
                }
                return;
            }
            TaskFieldScene.Request r = (TaskFieldScene.Request) scriptTask.pending();
            switch (r.kind) {
                case MESSAGE:
                    showHandlerMessage(r.text);
                    scriptTaskAnswer = () -> null;
                    return;
                case CONFIRM:
                    showHandlerQuestion(r.text, java.util.Arrays.asList("是", "否"), 2);
                    scriptTaskAnswer = () -> choiceResult == 0;
                    return;
                case CHOOSE: {
                    final int cancel = r.cancel;
                    scriptQuestionDefault = r.number;
                    showHandlerQuestion(r.text, r.commands, cancel < 0 ? 100 : cancel);
                    scriptTaskAnswer = () -> choiceResult >= 99 ? -1 : choiceResult;
                    return;
                }
                case TUTOR: {
                    MenuService.Request tutor = new MenuService.Request(MenuService.Kind.TUTOR);
                    tutor.move = r.text;
                    tutor.movelist = r.movelist;
                    tutor.byMachine = r.flag;
                    if (menuService == null) {
                        scriptTask.answer(-1);
                        break;
                    }
                    menuRequest = menuService.submit(tutor);
                    scriptTaskAnswer = () -> tutor.result;
                    return;
                }
                case RELEARN: {
                    MenuService.Request relearn = new MenuService.Request(MenuService.Kind.RELEARN);
                    relearn.pokemon = r.pokemon;
                    if (menuService == null) {
                        scriptTask.answer(false);
                        break;
                    }
                    menuRequest = menuService.submit(relearn);
                    scriptTaskAnswer = () -> relearn.result > 0;
                    return;
                }
                case CHOOSE_NON_EGG: {
                    MenuService.Request choose = new MenuService.Request(MenuService.Kind.CHOOSE_NON_EGG);
                    if (menuService == null) {
                        scriptTask.answer(-1);
                        break;
                    }
                    menuRequest = menuService.submit(choose);
                    scriptTaskAnswer = () -> choose.result;
                    return;
                }
                case FORGET: {
                    MenuService.Request forget = new MenuService.Request(MenuService.Kind.FORGET_MOVE);
                    forget.pokemon = r.pokemon;
                    forget.learnMove = r.move;
                    if (menuService == null) {
                        scriptTask.answer(-1);
                        break;
                    }
                    menuRequest = menuService.submit(forget);
                    scriptTaskAnswer = () -> forget.result;
                    return;
                }
                case ACTION: {
                    float seconds = r.action.get();
                    scriptTaskAnswer = () -> null;
                    if (seconds > 0f) {
                        waitTimer = seconds;
                        interpreterState = InterpreterState.WAIT_TIME;
                        return;
                    }
                    break;
                }
                case DIMENSION_WARP: {
                    MenuService.Request warp = new MenuService.Request(MenuService.Kind.DIMENSION_WARP);
                    if (menuService == null) {
                        scriptTask.answer(null);
                        break;
                    }
                    menuRequest = menuService.submit(warp);
                    scriptTaskAnswer = () -> warp.flyResult;
                    return;
                }
                case FLY_MAP: {
                    MenuService.Request fly = new MenuService.Request(MenuService.Kind.SHOW_MAP);
                    fly.fly = true;
                    if (menuService == null) {
                        scriptTask.answer(null);
                        break;
                    }
                    menuRequest = menuService.submit(fly);
                    scriptTaskAnswer = () -> fly.flyResult;
                    return;
                }
                case FLASH:
                    showFlashMessage(r.text);
                    scriptTask.answer(null);
                    break;
                case WAIT_CANCEL:
                    waitCancelled = false;
                    scriptTaskAnswer = () -> waitCancelled;
                    waitCancelable = true;
                    waitTimer = r.number / 40f;
                    if (waitTimer > 0f) {
                        interpreterState = InterpreterState.WAIT_TIME;
                        return;
                    }
                    waitCancelable = false;
                    break;
                case WAIT:
                    scriptTaskAnswer = () -> null;
                    waitTimer = r.number / 40f;
                    if (waitTimer > 0f) {
                        interpreterState = InterpreterState.WAIT_TIME;
                        return;
                    }
                    break;
                case CHOOSE_ITEM: {
                    MenuService.Request choose = new MenuService.Request(MenuService.Kind.CHOOSE_ITEM);
                    choose.filter = r.filter;
                    if (menuService == null) {
                        scriptTask.answer(null);
                        break;
                    }
                    menuRequest = menuService.submit(choose);
                    scriptTaskAnswer = () -> choose.text;
                    return;
                }
                case ME:
                    if (audio != null) {
                        audio.playMe(r.text, 100, 100);
                    }
                    scriptTask.answer(null);
                    break;
                case SE:
                    if (audio != null) {
                        audio.playSe(r.text, r.number, 100);
                    }
                    scriptTask.answer(null);
                    break;
                default:
                    scriptTask.answer(null);
                    break;
            }
        }
    }

    /** pbMessage followed by a command window: the text stays while the choices are open. */
    private void showHandlerQuestion(String text, java.util.List<String> options, int cancelType) {
        Array<String> raw = new Array<>();
        raw.add(text);
        MessageText.Parsed parsed = MessageText.parse(raw, state, messageColumns);
        messageLines = parsed.lines;
        messagePage = 0;
        messageWaits = parsed.waitForInput;
        noteMessageExtras(parsed);
        messageSpeaker = parsed.speaker;
        messageLinesPerPage = parsed.lineCount > 0 ? parsed.lineCount : MessageService.LINES_PER_PAGE;
        choicesFollow = true;
        showMessagePage();
        interpreterState = InterpreterState.WAIT_MESSAGE;
        scriptQuestion = options;
        scriptQuestionCancel = cancelType;
    }

    private void conditionalBranch(EventProgram program, EventCommand command) {
        JsonValue p = command.parameters;
        if (intParam(p, 0, -1) == 12 && p != null && p.get(1) != null) {
            String script = p.get(1).asString().trim();
            java.util.regex.Matcher trainer = TRAINER_BATTLE_CONDITION.matcher(script);
            if (trainer.matches()) {
                startTrainerBattleCondition(program, command, trainer);
                return;
            }
            java.util.regex.Matcher wild = WILD_BATTLE_CONDITION.matcher(script);
            if (wild.matches()) {
                startWildBattleCondition(program, command, wild);
                return;
            }
            // Boss_Battles: `battleXxx` (a def returning decision==N) used as a script condition.
            BossBattleData.Entry boss = BossBattleData.find(script);
            if (boss != null) {
                startBossBattleCondition(program, command, boss);
                return;
            }
            ScriptCondition parsed = parseCondition(script);
            if (parsed != null && needsScreen(parsed)) {
                startConditionTask(program, parsed);                               // pbCut / pbRockSmash / pbStrength
                return;
            }
        }
        boolean result = evaluate(command);
        program.branchResult(program.indent(), result);
        if (result) {
            program.advance();
        } else {
            program.skipBlock();
        }
    }

    /**
     * pbWildBattle as a script condition: the event starts the battle and
     * branches on {@code decision!=2 && decision!=5} (PField_Battles:367) when
     * the interpreter resumes.
     */
    private void startWildBattleCondition(EventProgram program, EventCommand command,
                                          java.util.regex.Matcher wild) {
        if (battlePort == null || pbs == null) {
            unsupported(command, "pbWildBattle condition without a battle runtime");
            program.branchResult(program.indent(), false);
            program.skipBlock();
            return;
        }
        String species = wild.group(1);
        int level = Integer.parseInt(wild.group(2));
        if (doubleRefusalFirst()) return;
        applyBattleSwitches();
        int outcomeVar = prepareWildBattle(null);
        battlePort.wildBattle(species, level);
        pendingBattleCondition = new BattleCondition(program, command, outcomeVar);
    }

    /**
     * One {@code def battleXxx} of Boss_Battles (319, transcribed in {@link BossBattleData}) used as a script condition.
     * The def runs, then {@code return decision==N} is the branch. 登记: {@code battleBoss(species,level,rank)}
     * (319:5-31) is not transcribed (see {@link BossBattleData}).
     */
    private void startBossBattleCondition(EventProgram program, EventCommand command, BossBattleData.Entry boss) {
        if (battlePort == null || pbs == null) {
            if (program != null) {
                unsupported(command, boss.name + " without a battle runtime or PBS data");
                program.branchResult(program.indent(), false);
                program.skipBlock();
            }
            return;
        }
        if (boss.lazyDogSkip && state.switches().get(197)) {                      // 319:6 if $game_switches[197]
            showHandlerMessage("懒狗模式跳过BOSS战并默认胜利。");                      // 319:7 pbMessage
            pendingBattleCondition = BattleCondition.fixed(program, command, true);   // 319:8 return true
            return;
        }
        PbsData.Species species = pbs.species(boss.species);
        if (species == null) {
            if (program != null) {
                unsupported(command, boss.name + ": unknown species " + boss.species);
                program.branchResult(program.indent(), false);
                program.skipBlock();
            }
            return;
        }
        if (doubleRefusalFirst()) return;                                         // PField_Battles:88-93 inside pbPrepareBattle
        if (boss.uncatchable) state.switches().set(196, true);                    // $game_switches[196] = true
        pokemon.runtime.state.BattleRules rules = state.battleRules();
        int size = boss.sizes[Math.min(ableCount(), boss.sizes.length - 1)];      // count = $Trainer.ablePokemonCount; size = ...
        rules.record(size + "v1", null);                                          // setBattleRule(sprintf("%dv1",size))
        rules.record("canlose", null);
        rules.record("noexp", null);
        Pokemon pkmn = newPkmn(species, boss.level);                     // pkmn = pbGenPkmn(:SPECIES, level)
        for (BossBattleData.Op op : boss.ops) {
            applyBossOp(pkmn, op);
        }
        // pbWildBattleCore(pkmn) (PField_Battles:262-345)
        if (ableCount() == 0) {                                                   // :265 $Trainer.ablePokemonCount==0
            if (state.trainer().party.members().size > 0) showHandlerMessage("SKIPPING BATTLE...");   // :266
            int skippedVar = rules.outcomeVar == null ? 1 : rules.outcomeVar;
            state.variables().set(skippedVar, 1);                                 // :267 pbSet(outcomeVar,1)
            rules.clear();                                                        // :268
            if (boss.uncatchable) state.switches().set(196, false);
            pendingBattleCondition = BattleCondition.fixed(program, command, boss.decision == 1);   // :273 return 1; the def's return decision==N
            return;
        }
        applyBattleSwitches();
        int outcomeVar = prepareWildBattle(null);
        battlePort.freeWildBattle(pkmn);
        pendingBattleCondition = new BattleCondition(program, command, outcomeVar);
        pendingBattleCondition.requiredDecision = boss.decision;
        pendingBattleCondition.resetSwitch196 = boss.uncatchable;
    }

    /** One {@code pkmn.*} statement of a Boss_Battles def (319), executed in the def's order. */
    private void applyBossOp(Pokemon pkmn, BossBattleData.Op op) {
        switch (op.name) {
            case "form": pokemonSet(pkmn, "form", op.arg); break;                 // pkmn.form = N
            case "iv": pokemonSet(pkmn, "iv", op.arg); break;                     // pkmn.iv = [..]
            case "ev": pokemonSet(pkmn, "ev", op.arg); break;                     // pkmn.ev = [..]
            case "battleRank": pkmn.battleRank = ((Number) op.arg).intValue(); break;   // PokeBattle_BOSS:20-22
            case "setItem": pkmn.item = (String) op.arg; break;                   // PokeBattle_Pokemon:630
            case "setNature": applyNature(pkmn, op.arg); break;                   // :305
            case "setAbility": applyAbility(pkmn, op.arg); break;                 // :255
            case "pbLearnMove": learnMove(pkmn, (String) op.arg); break;          // :466-495
            case "name": pkmn.name = (String) op.arg; break;
            case "makeShiny": pkmn.shiny = true; break;                           // :325
            case "makeSuperShiny": pkmn.shiny = true; pkmn.superShiny = true; break;   // :343 (superShiny? needs shiny?)
            case "makeNotShiny": pkmn.shiny = false; break;                       // :330
            case "makeMale": pkmn.gender = PokemonStats.MALE; break;              // :212
            case "calcStats": pkmn.totalHpFactor = 1; pkmn.hp = pkmn.maxHp(); break;   // :868-891 (a fresh Pokemon: no HP lost)
            case "totalhpTimes": pkmn.totalHpFactor *= ((Number) op.arg).intValue(); break;   // pkmn.totalhp = pkmn.totalhp * N
            case "hpToTotal": pkmn.hp = pkmn.maxHp(); break;                      // pkmn.hp = pkmn.totalhp
            default: log.warn("Boss_Battles op " + op.name + " is not implemented; skipped"); break;
        }
    }

    /**
     * PField_Battles:526-580: {@code pbTrainerBattle} used as a script
     * condition - the event starts the battle and branches on the win. The
     * battle is interactive, so the branch resolves when the interpreter
     * resumes after it (the decision also lands in the outcome variable).
     */
    private void startTrainerBattleCondition(EventProgram program, EventCommand command,
                                             java.util.regex.Matcher trainer) {
        String type = trainer.group(1);
        String name = trainer.group(2);
        int version = trainer.group(3) == null ? 0 : Integer.parseInt(trainer.group(3));
        if (battlePort == null || pbs == null) {
            unsupported(command, "pbTrainerBattle condition without a battle runtime");
            program.branchResult(program.indent(), false);
            program.skipBlock();
            return;
        }
        PbsData.TrainerData data = pbs.trainer(type, name, version);
        if (data == null) {
            log.warn("pbTrainerBattle condition: trainer " + type + " \"" + name
                    + "\" (party " + version + ") is not in trainers.json");
            program.branchResult(program.indent(), false);
            program.skipBlock();
            return;
        }
        if (doubleRefusalFirst()) return;
        if (deferToSecondTrainer(data)) {                                  // PField_Battles:534-558: returns false
            program.branchResult(program.indent(), false);
            program.skipBlock();
            return;
        }
        java.util.List<PbsData.TrainerData> opponents = new java.util.ArrayList<>();
        opponents.add(data);
        trainerWithWaiting(opponents);                                     // PField_Battles:564-569
        int outcomeVar = prepareTrainerBattle();
        applyBattleSwitches();
        battlePort.trainerBattle(opponents);
        pendingBattleCondition = new BattleCondition(program, command, outcomeVar);
    }

    /** The refusal of {@code pbPrepareBattle} (PField_Battles:92) was shown for the command that is about to retry. */
    private boolean doubleRefusalShown;

    /**
     * PField_Battles:88-93: with the forced-double switch (41) on and fewer than two able Pokemon and no partner,
     * {@code pbPrepareBattle} shows "你的宝可梦数量不足以进行双打！" and the battle goes on as it was. The line is shown
     * first and the battle command runs again once it is closed.
     *
     * @return true when the message was started (the caller returns and is run again)
     */
    private boolean doubleRefusalFirst() {
        if (doubleRefusalShown) {
            doubleRefusalShown = false;
            return false;
        }
        if (!state.switches().get(41) || state.partner() != null || ableCount() >= 2) {
            return false;
        }
        doubleRefusalShown = true;
        if (pendingSteps != null && pendingStepIndex > 0) {
            pendingStepIndex--;                                            // the step is run again after the message
        }
        showHandlerMessage("你的宝可梦数量不足以进行双打！");
        return true;
    }

    /** {@code $Trainer.ablePokemonCount}. */
    private int ableCount() {
        int count = 0;
        for (Pokemon p : state.trainer().party.members()) {
            if (p != null && !p.egg && p.hp > 0) count++;
        }
        return count;
    }

    /** The event whose win marks a waiting trainer's self switch (PField_Battles:574-576), or -1. */
    private int waitingFinishEvent = -1;

    /**
     * PField_Battles:534-558: two trainer events spotted the player at once. The first one records itself in
     * {@code $PokemonTemp.waitingTrainer} and ends without a battle; the second one then fights both.
     *
     * @return true when this trainer was recorded to wait (the battle does not start)
     */
    private boolean deferToSecondTrainer(PbsData.TrainerData trainer) {
        pokemon.runtime.state.BattleRules rules = state.battleRules();
        if (rules.waitingTrainer != null || mapPort == null) {
            return false;                                                  // :534 !waitingTrainer
        }
        int able = ableCount();
        if (!(able > 1 || (able > 0 && state.partner() != null))) {       // :535-536
            return false;
        }
        int others = 0;
        for (int id : mapPort.triggeredTrainerEvents()) {                  // :539 pbTriggeredTrainerEvents([2],false)
            if (id == eventId) continue;                                   // :542
            if (state.selfSwitches().get(mapId, id, "A")) continue;        // :543
            others++;
        }
        if (others == 1 && trainer.party.size <= 6) {                      // :554
            rules.waitingTrainer = new Object[] { trainer, eventId };      // :555
            return true;                                                   // :556
        }
        return false;
    }

    /**
     * PField_Battles:562-569: the recorded waiting trainer fights first, and the double rule applies. Its event gets
     * its self switch when the battle is won (:574-576), which {@link #execute()} does once the battle is over.
     */
    private void trainerWithWaiting(java.util.List<PbsData.TrainerData> opponents) {
        pokemon.runtime.state.BattleRules rules = state.battleRules();
        Object[] waiting = rules.waitingTrainer instanceof Object[] ? (Object[]) rules.waitingTrainer : null;
        if (waiting == null) return;
        opponents.add(0, (PbsData.TrainerData) waiting[0]);
        waitingFinishEvent = (Integer) waiting[1];
        rules.waitingTrainer = null;                                       // :577
        rules.record("double", null);                                      // :562 setBattleRule("double")
    }

    /**
     * PField_Battles:96 {@code battle.setBattleMode(battleRules["size"])} and the partner of
     * pbWildBattleCore:303-318 / pbTrainerBattleCore:459-487: the battle size, the registered partner
     * ({@code $PokemonGlobal.partner}) and the {@code noPartner} rule go to the port, which knows the foe party.
     */
    private void applyBattleSizeAndPartner(pokemon.runtime.state.BattleRules rules) {
        battlePort.setBattleSize(rules.size);
        GameState.Partner partner = state.partner();
        if (partner == null) {
            battlePort.setPartner(null, null, java.util.Collections.<Pokemon>emptyList());
        } else {
            battlePort.setPartner(partner.trainerType, partner.name, partner.party);
        }
        battlePort.setNoPartner(rules.noPartner != null && rules.noPartner);
    }

    /**
     * PField_Battles:497-498 + 510-516: consumes the recorded battle rules for
     * the battle that starts now and returns its outcome variable (default 1).
     */
    private int prepareTrainerBattle() {
        pokemon.runtime.state.BattleRules rules = state.battleRules();
        applyBattleSizeAndPartner(rules);
        if (rules.canLose != null) {
            battlePort.setCanLose(rules.canLose);
        }
        // PField_Battles:105 battle.expGain = battleRules["expGain"] if !battleRules["expGain"].nil?
        if (rules.expGain != null) {
            battlePort.setExpGain(rules.expGain);
        }
        // PField_Battles:112 setBattleRule("switchstyle"/"setstyle").
        if (rules.switchStyle != null) {
            battlePort.setSwitchStyle(rules.switchStyle);
        }
        // PField_Battles:115 setBattleRule("anims"/"noanims").
        if (rules.battleAnims != null) {
            battlePort.setBattleAnims(rules.battleAnims);
        }
        int outcomeVar = rules.outcomeVar == null ? 1 : rules.outcomeVar;
        rules.clear();
        return outcomeVar;
    }

    /**
     * The wild twin of {@link #prepareTrainerBattle()}: pbWildBattle(species,
     * level, outcomeVar=1, canRun=true, canLose=false) turns its optional
     * arguments into battle rules (PField_Battles:359-361) and reads them back
     * here together with any rule an event set beforehand. The decision lands in
     * the outcome variable when the battle is over (PField_Battles:343).
     */
    private int prepareWildBattle(com.badlogic.gdx.utils.JsonValue ir) {
        pokemon.runtime.state.BattleRules rules = state.battleRules();
        applyBattleSizeAndPartner(rules);
        if (rules.canLose != null) {
            battlePort.setCanLose(rules.canLose);
        }
        if (rules.canRun != null) {
            battlePort.setCanRun(rules.canRun);
        }
        // PField_Battles:105 battle.expGain = battleRules["expGain"] if !battleRules["expGain"].nil?
        if (rules.expGain != null) {
            battlePort.setExpGain(rules.expGain);
        }
        if (rules.disablePokeBalls != null) {
            battlePort.setDisablePokeBalls(rules.disablePokeBalls);
        }
        // PField_Battles:112 setBattleRule("switchstyle"/"setstyle").
        if (rules.switchStyle != null) {
            battlePort.setSwitchStyle(rules.switchStyle);
        }
        // PField_Battles:115 setBattleRule("anims"/"noanims").
        if (rules.battleAnims != null) {
            battlePort.setBattleAnims(rules.battleAnims);
        }
        // The written arguments win over the recorded rules, because the plugin
        // writes them into that same rule hash before the battle starts
        // (PField_Battles:359-361).
        if (ir != null && ir.has("outcomeVar")) {
            rules.outcomeVar = ir.getInt("outcomeVar", 1);
        }
        if (ir != null && ir.has("canRun")) {
            battlePort.setCanRun(ir.getBoolean("canRun", true));
        }
        if (ir != null && ir.has("canLose")) {
            battlePort.setCanLose(ir.getBoolean("canLose", false));
        }
        int outcomeVar = rules.outcomeVar == null ? 1 : rules.outcomeVar;
        rules.clear();
        return outcomeVar;
    }

    /**
     * Battle_ExpAndMoveLearning:167-186: the level lock reads
     * {@code $game_switches[199]} (lock on) and {@code [12]} (league pass).
     * PField_Battles:146-151 + :181-188 additionally read the map's
     * MetadataEnvironment for the battle's environment and its Dusk Ball time.
     */
    private void applyBattleSwitches() {
        if (battlePort == null) {
            return;
        }
        battlePort.setLevelLock(state.switches().get(199), state.switches().get(12));
        battlePort.setSwitchSource(id -> state.switches().get(id));
        battlePort.setVariableSource(id -> state.variables().get(id));          // $game_variables[100]: level follow
        // Settings:32 FATEFUL_ENCOUNTER_SWITCH / PField_Encounters:951-955.
        battlePort.setObtainMap(state.currentMapId());
        battlePort.setFatefulEncounter(state.switches().get(32));
        PbsData pbs = this.pbs;
        int mapId = state.currentMapId();
        PbsData.Metadata metadata = pbs == null ? null : pbs.mapMetadata(mapId);
        battlePort.setBattleEnvironment(pokemon.runtime.battle.CaptureCalculator.environmentId(
                metadata == null ? null : metadata.environment));
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
            case 1: { // Variable: params [1, variable id, operand type, operand, comparison]
                // 048_Interpreter:603-623.
                int value1 = state.variables().get(Math.max(1, intParam(p, 1, 0)));    // :604
                int operandType = intParam(p, 2, 0);
                int value2 = intParam(p, 3, 0);
                if (operandType == 0) {                                                // :605-606
                    // value2 = @parameters[3]
                } else {
                    value2 = state.variables().get(Math.max(1, value2));               // :608
                }
                switch (intParam(p, 4, -1)) {                                          // :610
                    case 0: return value1 == value2;                                   // :611-612
                    case 1: return value1 >= value2;                                   // :613-614
                    case 2: return value1 <= value2;                                   // :615-616
                    case 3: return value1 > value2;                                    // :617-618
                    case 4: return value1 < value2;                                    // :619-620
                    case 5: return value1 != value2;                                   // :621-622
                    default: return false;                                             // result stays false
                }
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
            case 7: { // Gold: params [7, amount, 0 = at least / 1 = at most] (048_Interpreter:648-653)
                int amount = intParam(p, 1, 0);
                if (intParam(p, 2, 0) == 0) {                                          // :649
                    return state.trainer().money >= amount;                            // :650
                }
                return state.trainer().money <= amount;                                // :652
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
     * translator: {@code $game_switches[n]}, {@code $game_variables[n]},
     * {@code $game_player.x/.y}, the party size and self switches. Everything
     * else is reported and treated as false until R7 translates it (project3
     * section 29).
     */
    private boolean scriptCondition(String script, EventCommand command) {
        String text = script.trim();
        Matcher variable = SCRIPT_VARIABLE.matcher(text);
        if (variable.matches()) {
            int id = Math.max(1, Integer.parseInt(variable.group(1)));
            int value = Integer.parseInt(variable.group(3));
            return compare(variable.group(2), state.variables().get(id), value);
        }
        Matcher playerPosition = SCRIPT_PLAYER_POSITION.matcher(text);
        if (playerPosition.matches()) {
            int value = "x".equals(playerPosition.group(1))
                    ? state.playerX() : state.playerY();
            return compare(playerPosition.group(2), value,
                    Integer.parseInt(playerPosition.group(3)));
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
        // The project gates doors / NPCs on the party size ("$Trainer.party.length
        // > 0"). Those conditions are code-111 scripts the translator does not
        // cover, so answer them here instead of reporting them unsupported (which
        // made every such page false and blocked the player).
        Matcher party = SCRIPT_PARTY.matcher(text);
        if (party.matches()) {
            int size = state.trainer() == null ? 0 : state.trainer().party.members().size;
            if (party.group(1) == null) {
                return size > 0;
            }
            return compare(party.group(1), size, Integer.parseInt(party.group(2)));
        }
        ScriptCondition general = parseCondition(text);
        if (general != null) {
            try {
                return ScriptCondition.truthy(general.eval(conditionEnv(null)));
            } catch (ScriptCondition.Unsupported error) {
                unsupported(command, "script condition \"" + text + "\": " + error.getMessage());
                return false;
            } catch (RuntimeException error) {
                unsupported(command, "script condition \"" + text + "\" failed: " + error);
                return false;
            }
        }
        unsupported(command, "script condition \"" + text + "\" (not parsed)");
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

    /**
     * PField_Battles:510-516: the battle decision stored in the outcome
     * variable - 0 aborted, 1 won, 2 lost, 3 fled, 4 caught.
     */
    private static int battleDecision(BattleResult result) {
        if (result == null) {
            return 0;
        }
        if (result.decision >= 0) {
            return result.decision;                    // the Safari Zone's own codes (242_PBattle_Safari:121-125)
        }
        switch (result.outcome) {
            case WIN: return 1;
            case LOSS: return 2;
            case ESCAPE: return 3;
            case CAUGHT: return 4;
            default: return 0;
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

    /**
     * "Change Battle BGM" (132) / "Change Battle ME" (133):
     * {@code $PokemonGlobal.nextBattleBGM = pbParams[0]} (PField_Field:882-890).
     * The audio file itself is not played here - the next battle start reads it
     * through {@code pbGetWildBattleBGM} / {@code pbGetTrainerBattleBGM}
     * (PSystem_FileUtilities:547/619).
     */
    private void changeBattleAudio(EventProgram program, EventCommand command, boolean bgm) {
        JsonValue file = command.parameter(0);
        String id = null;
        if (file != null && file.isObject()) {
            JsonValue name = file.get("name");
            if (name != null && name.isString()) {
                id = name.asString();
            }
        } else if (file != null && file.isString()) {
            id = file.asString();
        }
        if (state != null) {
            if (bgm) {
                state.nextBattleBGM(id == null || id.isEmpty() ? null : id);
            } else {
                state.nextBattleME(id == null || id.isEmpty() ? null : id);
            }
        }
        program.advance();
    }

    private void unsupported(EventCommand command, String detail) {
        log.warn("event command " + command.code + " (index " + command.index + ") uses " + detail
                + "; treated as not met");
    }

    private static int intParam(JsonValue parameters, int position, int fallback) {
        JsonValue value = parameters == null ? null : parameters.get(position);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (value.isNumber()) {
            return value.asInt();
        }
        if (value.isString()) {
            // The project data carries empty strings for "unset" numeric
            // parameters; asInt() would throw and close the game (seen on the
            // intro's Fade Screen). Treat them as the documented fallback.
            try {
                return Integer.parseInt(value.asString().trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static float floatParam(JsonValue parameters, int position, float fallback) {
        JsonValue value = parameters == null ? null : parameters.get(position);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (value.isNumber()) {
            return value.asFloat();
        }
        if (value.isString()) {
            try {
                return Float.parseFloat(value.asString().trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
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
                return id < 1 ? 0 : state.variables().value(id);
            }
            if (value.has("trainerPokemonCount")) {
                return state.trainer().pokemonCount();
            }
            if (value.has("trainerStat")) {
                // PokeBattle_Trainer:177/192: $Trainer.pokedexSeen / pokedexOwned
                // are the counts of seen / owned species.
                switch (value.getString("trainerStat", "")) {
                    case "pokedexSeen": return state.trainer().seen.size();
                    case "pokedexOwned": return state.trainer().owned.size();
                    default: return 0;
                }
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
            case "giveRibbon": {
                String ribbon = asName(first);
                if (ribbon != null) {
                    pokemon.giveRibbon(ribbon);
                }
                break;
            }
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
        // PokeBattle_Pokemon:466-490 pbLearnMove: "Silently learns the given move. Will erase the first known move if it has to."
        // The runtime's move list holds only the filled slots (a Ruby slot with id 0 is a missing entry here).
        for (int i = 0; i < pokemon.moves.size; i++) {                      // :471 already knows move
            PbsData.Move known = pokemon.moves.get(i).move;
            if (known == null || !moveName.equals(known.internalName)) {
                continue;
            }
            for (int j = i + 1; j < pokemon.moves.size; j++) {              // :472-480 relocate it to the end of the list
                pokemon.moves.swap(j, j - 1);
            }
            return;                                                         // :481
        }
        if (pokemon.moves.size < 4) {                                       // :484-488 has an empty move slot
            pokemon.moves.add(new Pokemon.MoveSlot(move));
            return;
        }
        pokemon.moves.removeIndex(0);                                       // :490-494 forget the first move, learn the new one
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
            case "trainerID":
                // p.trainerID = 225 (the gift scripts); the summary shows the
                // public id, which is the low 16 bits (PokeBattle_Pokemon:67-69).
                pokemon.setTrainerID(value instanceof Number ? ((Number) value).intValue() : 0);
                break;
            case "otgender":
                pokemon.otGender = value instanceof Number ? ((Number) value).intValue() : -1;
                break;
            case "ballused":
                pokemon.ballused = value instanceof Number ? ((Number) value).intValue() : 0;
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

    /** R8: the {@code \se[ItemGet]} sound the boss rewards play. */
    private void playItemGetSe() {
        if (audio != null) {
            audio.playSe("ItemGet", 100, 100);
        }
    }

    /** goldFinger:56-65 add_exp_pot: the exp pot, capped at EXP_POT_MAX (Settings:28). */
    private void addExpPot(int expAdded, boolean showMessage) {
        final int expMax = 99999999;
        boolean hasPot = pbs != null && pbs.item("EXPPOT") != null
                && inventory != null && inventory.count("EXPPOT") > 0;
        int current = Math.max(0, state.trainer().expPot);
        if (current + expAdded > expMax) {
            expAdded = expMax - current;
        }
        state.trainer().expPot = current + expAdded;
        if (showMessage && hasPot) {
            showHandlerMessage("经验储罐累积的经验值增加了" + expAdded + "点。");
        }
    }

    /**
     * {@code pbNewPkmn(species, level)} (197_PokeBattle_Pokemon:1007-1011 = {@code PokeBattle_Pokemon.new}, :909-964): a random
     * personal id (nature, gender, ability slot, shininess), random IVs and the player as the owner.
     */
    private Pokemon newPkmn(PbsData.Species species, int level) {
        return pokemon.runtime.pokemon.WildGenerator.pbNewPkmn(pbs, species, level, state.trainer(),
                state.currentMapId(), random);
    }

    /** BossRewards.pokemon_reward:333-344: a pool Pokemon in the seal ball. */
    private void addBossPokemon(BossRewardsData.PoolEntry entry, int level) {
        if (pbs == null) {
            log.warn("boss reward Pokemon without the PBS data; skipped");
            return;
        }
        PbsData.Species species = pbs.species(entry.species);
        if (species == null) {
            log.warn("boss reward unknown species " + entry.species + "; skipped");
            return;
        }
        Pokemon pokemon = newPkmn(species, level);
        pokemon.ballused = 26;                  // "封印球" (Boss_reward:336)
        if (entry.form > 0) {
            applyForm(pokemon, entry.form);
        }
        boolean toParty = state.trainer().addToParty(pokemon);
        log.warn("boss reward " + entry.species + " L" + level + (toParty ? " (party)" : " (box)"));
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

    /** One running REPEAT loop: body cursor + inclusive counter range (P0d). */
    private static final class RepeatFrame {        final JsonValue steps;
        final String local;
        final int last;
        int index;
        int current;

        RepeatFrame(JsonValue steps, String local, int from, int last) {
            this.steps = steps;
            this.local = local;
            this.current = from;
            this.last = last;
        }
    }

    /** A pending pbTrainerBattle script condition; resolved after the battle. */
    private static final class BattleCondition {
        final EventProgram program;
        final EventCommand command;
        final int outcomeVar;
        /** A Boss_Battles def: {@code return decision==N}; -1 = branch on a win like pbTrainerBattle. */
        int requiredDecision = -1;
        /** A Boss_Battles def that set {@code $game_switches[196]} resets it after the battle. */
        boolean resetSwitch196;
        /** The branch is decided without a battle (the lazy-dog skip, a skipped battle). */
        Boolean fixedResult;

        BattleCondition(EventProgram program, EventCommand command, int outcomeVar) {
            this.program = program;
            this.command = command;
            this.outcomeVar = outcomeVar;
        }

        static BattleCondition fixed(EventProgram program, EventCommand command, boolean result) {
            BattleCondition condition = new BattleCondition(program, command, -1);
            condition.fixedResult = result;
            return condition;
        }
    }
}
