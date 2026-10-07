package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.field.BerryPlants;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.GameState;
import pokemon.runtime.state.Inventory;

/**
 * The berry plant interaction behind {@code pbBerryPlant} and
 * {@code pbPickBerry} (PField_BerryPlants:313-590), with the project's
 * {@code NEW_BERRY_PLANTS = true} (Settings:69).
 *
 * <p>The interpreter starts this stepper for one BERRY_PLANT / BERRY_PICK IR
 * command and pumps it from {@code WAIT_BERRY}. Every visible step - message,
 * command window and the {@code pbChooseItemScreen} bag pick - is played here;
 * the wording, the wait order and every number come from the plugin lines
 * named in the method comments.</p>
 *
 * <p>{@code pbBerryPlant} reads and writes the event variable through
 * {@code Interpreter#getVariable} / {@code #setVariable}; the plugin's berry
 * sprite ({@code BerryPlantSprite#update}, lines 138-145) settles the growth
 * before every interaction, so {@link #begin()} runs
 * {@link BerryPlants#update} first instead.</p>
 */
public final class BerryInteraction {

    /** The bag UI hook for {@code pbChooseItemScreen(proc)} (lines 350-354). */
    public interface ItemChoiceHost {
        /**
         * Opens the bag filtered by {@code filter}; the result arrives later
         * through {@link #itemChosen(MenuService.Request)}.
         *
         * @return false when no bag UI exists (the pick counts as cancelled)
         */
        boolean chooseItem(java.util.function.Predicate<String> filter);
    }

    /** {@code watering} (lines 332-337): the project's four watering items. */
    private static final String[] WATERING_ITEMS = {
            "SPRAYDUCK", "SQUIRTBOTTLE", "WAILMERPAIL", "SPRINKLOTAD",
    };

    /** {@code pbPocketNames} (Settings:176); index 5 is the berry pocket. */
    private static final String[] POCKET_NAMES = {
            "", "道具", "回复道具", "精灵球", "招式学习机",
            "树果", "超级石", "对战道具", "重要道具", "特殊道具",
    };

    /** {@code pbConfirmMessage}: [_INTL("是"), _INTL("否")] (Messages:1320-1322). */
    private static final String CONFIRM_YES = "是";
    private static final String CONFIRM_NO = "否";

    /** Empty Gen 4 plot ({@code NEW_BERRY_PLANTS}, lines 318-322). */
    private static final int[] EMPTY_PLOT = { 0, 0, 0, 0, 0, 0, 0, 0 };

    private enum Wait { NONE, MESSAGE, CHOICE }

    private final GameState state;
    private final MessageService messages;
    private final InputManager input;
    private final PbsData pbs;
    private final Inventory inventory;
    private final MapPort mapPort;
    private final int mapId;
    private final int eventId;
    private final int messageColumns;
    private final ItemChoiceHost itemHost;
    private final EventInterpreter.WarningLog log;

    /** The event variable: [stage, berry, seconds alive, checked, damp, replants, penalty, mulch]. */
    private int[] berryData;
    private int stage;
    private int berry;

    private Wait wait = Wait.NONE;
    private Runnable onDone;
    private boolean finished;

    // One message page (lines 344-347 etc.); mirrors the interpreter's pager.
    private Array<String> pageLines;
    private int page;
    private int linesPerPage = MessageService.LINES_PER_PAGE;
    private boolean pageWaits = true;
    private String pageSpeaker;
    private String pageSkin;

    /** Result of the command window currently on screen (pbShowCommands). */
    private int choiceResult = Integer.MIN_VALUE;

    private java.util.function.Consumer<String> itemCallback;
    private boolean itemWaiting;

    public BerryInteraction(GameState state, MessageService messages, InputManager input,
                            PbsData pbs, Inventory inventory, MapPort mapPort,
                            int mapId, int eventId, int messageColumns,
                            ItemChoiceHost itemHost, EventInterpreter.WarningLog log) {
        this.state = state;
        this.messages = messages;
        this.input = input;
        this.pbs = pbs;
        this.inventory = inventory;
        this.mapPort = mapPort;
        this.mapId = mapId;
        this.eventId = eventId;
        this.messageColumns = messageColumns;
        this.itemHost = itemHost;
        this.log = log == null ? message -> { } : log;
    }

    public boolean finished() {
        return finished;
    }

    // ------------------------------------------------------------------
    // pbBerryPlant (lines 313-553)
    // ------------------------------------------------------------------

    /** {@code pbBerryPlant} for the running event. */
    public void begin() {
        berryData = mapPort == null ? null : mapPort.getEventVariable(eventId);
        if (berryData == null) {
            berryData = EMPTY_PLOT.clone();
        } else if (berryData.length <= 6) {
            // NEW_BERRY_PLANTS: a legacy Gen 3 plant array is not used here.
            berryData = EMPTY_PLOT.clone();
        }
        // BerryPlantSprite#update settles the growth before the interaction
        // (lines 138-145 -> updatePlantDetails, lines 147-217).
        berryData = BerryPlants.update(berryData, BerryPlants.now(),
                plantData(berryData[1]), mulchName(berryData[7]));
        saveVariable();
        stage = berryData[0];
        berry = berryData[1];
        turnToStage(); // lines 324-331: stop the event turning towards the player
        switch (stage) {
            case 0:
                if (berryData[7] == 0) {
                    plantMenu(); // lines 340-406
                } else {
                    existingMulch(); // lines 407-431
                }
                break;
            case 1:
            case 2:
            case 3:
            case 4:
                stageMessage(); // lines 457-479, then the watering loop
                break;
            case 5:
                harvestPlant(); // lines 480-525
                break;
            default:
                finish();
                break;
        }
    }

    /** {@code pbPickBerry(berry,qty=1)} for the running event (lines 555-590). */
    public void beginPick(String berryName, int qty) {
        int id = itemId(berryName);
        if (id <= 0) {
            log.warn("pbPickBerry(" + berryName + ") has no such item; skipped");
            finish();
            return;
        }
        berry = id;
        int count = Math.max(1, qty);
        String itemname = count > 1 ? itemPlural(id) : itemName(id);
        showConfirm(hereMessage(count, itemname), yes -> {
            if (!yes) {
                finish();
                return;
            }
            if (!canStore(id, count)) {
                showMessage("太糟糕了……\n背包已经满了……", this::finish);
                return;
            }
            store(id, count);
            String picked = count > 1
                    ? "摘下了" + count + "个\\c[1]" + itemname + "\\c[0]。\\wtnp[30]"
                    : "摘下了\\c[1]" + itemname + "\\c[0]。\\wtnp[30]";
            int pocket = pocketOf(id);
            showMessage(picked, () -> showMessage(
                    pocketMessage(itemname, pocket, true), () -> showMessage( // lines 578-579
                            "土壤又变得松软。", () -> { // line 581
                                berryData = EMPTY_PLOT.clone(); // line 582
                                saveVariable();                 // line 587
                                // pbSetSelfSwitch(thisEvent.id,"A",true) - line 588.
                                state.selfSwitches().set(mapId, eventId, "A", true);
                                finish();
                            })));
        });
    }

    // ------------------------------------------------------------------
    // Stage 0: the planting menu (lines 339-456)
    // ------------------------------------------------------------------

    /** {@code pbMessage("泥土看起来相当的松软。", [施肥, 种植, 返回], -1)} (344-347). */
    private void plantMenu() {
        showChoice("泥土看起来相当的松软。",
                new String[] { "施肥", "种植", "返回" }, 1, cmd -> {
                    if (cmd == 0) {
                        pickMulch(); // cmd==0: Fertilize (line 348)
                    } else if (cmd == 1) {
                        pickBerryToPlant(null); // cmd==1: Plant Berry (line 385)
                    } else {
                        finish(); // cmd 2 / cancel falls past the case into the empty-plot watering check
                    }
                });
    }

    /** The Fertilize branch: {@code pbChooseItemScreen(pbIsMulch?)} (lines 348-384). */
    private void pickMulch() {
        pickItem(this::isMulch, name -> {
            int ret = itemId(name);
            if (ret <= 0) {
                finish(); // ret==0: cancel falls past the whole `if ret>0`
                return;
            }
            if (!isMulch(name)) {
                showMessage("这不能使土壤变得肥沃！", this::finish); // lines 380-382
                return;
            }
            berryData[7] = ret; // line 357
            showMessage("The " + itemName(ret) + " was scattered on the soil.\u0001", // line 358
                    () -> showConfirm("想要种植树果吗？", yes -> { // line 359
                        if (!yes) {
                            saveVariable(); // line 379
                            finish();
                            return;
                        }
                        pickBerryToPlant(this::saveVariable); // line 379
                    }));
        });
    }

    /** Mulch already on the plot (lines 407-431). */
    private void existingMulch() {
        showMessage(itemName(berryData[7]) + " has been laid down.\u0001", // line 408
                () -> showConfirm("想要种植树果吗？", yes -> { // line 409
                    if (!yes) {
                        finish();
                        return;
                    }
                    pickBerryToPlant(null); // line 410-414; plant() saves (line 427)
                }));
    }

    /**
     * {@code pbChooseItemScreen(pbIsBerry?)} and planting (lines 363-377,
     * 387-404, 410-428). {@code onCancelled} runs when the bag pick is
     * cancelled; the mulch path still has to record the mulch then (line 379).
     */
    private void pickBerryToPlant(Runnable onCancelled) {
        pickItem(this::isBerry, name -> {
            int id = itemId(name);
            if (id <= 0) {
                if (onCancelled != null) {
                    onCancelled.run();
                }
                finish();
                return;
            }
            plant(id);
        });
    }

    /**
     * Plants one berry (lines 366-376 / 392-402 / 416-426). The plugin writes
     * the variable after the "种在了土里。" message in every branch (lines 379,
     * 403, 427), so the message continuation saves first.
     */
    private void plant(int berryId) {
        berryData[0] = 1;                    // growth stage
        berryData[1] = berryId;              // item id of the planted berry
        berryData[2] = 0;                    // seconds alive
        berryData[3] = (int) BerryPlants.now(); // time of last checkup
        berryData[4] = 100;                  // dampness value
        berryData[5] = 0;                    // number of replants
        berryData[6] = 0;                    // yield penalty
        remove(berryId, 1);                  // $PokemonBag.pbDeleteItem (line 374 ...)
        showMessage(itemName(berryId) + "种在了土里。", () -> { // lines 375-376
            saveVariable(); // line 379 (mulch) / 403 (menu) / 427 (existing mulch)
            finish();
        });
    }

    // ------------------------------------------------------------------
    // Stages 1-4: the growth message and the watering loop (lines 457-479, 526-552)
    // ------------------------------------------------------------------

    private void stageMessage() {
        String name = itemName(berry);
        switch (stage) {
            case 1:
                showMessage("这里种着" + name + "。", this::watering); // line 458
                break;
            case 2:
                showMessage(name + "已经发芽了。", this::watering); // line 460
                break;
            case 3:
                showMessage(name + "长得很大了。", this::watering); // line 462
                break;
            default:
                showMessage(name + "开花了！", this::watering); // line 465
                break;
        }
    }

    /**
     * The watering loop (lines 526-552): the first watering item the player
     * carries is offered once, then the loop breaks.
     */
    private void watering() {
        for (String item : WATERING_ITEMS) {
            PbsData.Item data = itemData(item);
            if (data == null) {
                continue; // getConst -> 0, watering.compact! (lines 333-337)
            }
            if (inventory == null || !inventory.has(data.internalName)) {
                continue;
            }
            showConfirm("想用" + itemName(data.id) + "浇水吗？", yes -> { // line 530
                if (yes) {
                    berryData[4] = 100; // Gen 4 watering, line 533
                    saveVariable();     // line 541
                    showMessage(playerName() + "浇了水。\\wtnp[40]", // line 542
                            () -> showMessage("它看起来很开心！", this::finish)); // line 544
                } else {
                    finish();
                }
            });
            return; // line 549: break after the first carried watering item
        }
        finish();
    }

    // ------------------------------------------------------------------
    // Stage 5: the ripe plant (lines 480-525)
    // ------------------------------------------------------------------

    private void harvestPlant() {
        int[] values = plantData(berry);
        int berrycount = BerryPlants.yield(berryData, values); // line 486
        String itemname = berrycount > 1 ? itemPlural(berry) : itemName(berry);
        showConfirm(hereMessage(berrycount, itemname), yes -> { // lines 496-502
            if (!yes) {
                finish();
                return;
            }
            if (!canStore(berry, berrycount)) {
                showMessage("太糟糕了……\n背包已经满了……", this::finish); // lines 503-505
                return;
            }
            store(berry, berrycount); // line 507
            String picked = berrycount > 1
                    ? "摘下了" + berrycount + "个\\c[1]" + itemname + "\\c[0]。\\wtnp[30]" // line 509
                    : "摘下了\\c[1]" + itemname + "\\c[0]。\\wtnp[30]"; // line 511
            int pocket = pocketOf(berry); // line 513
            showMessage(picked, () -> showMessage(
                    pocketMessage(itemname, pocket, false), // lines 514-515
                    () -> showMessage("土壤又变得松软。", () -> { // line 517
                        berryData = EMPTY_PLOT.clone(); // line 518
                        saveVariable();                 // line 523
                        finish();
                    })));
        });
    }

    /**
     * The shared {@code pbConfirmMessage} question (lines 497-501 and 561-565):
     * {@code "这里有{N}个 \c[1]{X}\c[0]!\n要摘下吗？"}.
     */
    private static String hereMessage(int count, String itemname) {
        return count > 1
                ? "这里有" + count + "个 \\c[1]" + itemname + "\\c[0]!\n要摘下吗？"
                : "这里有1个\\c[1]" + itemname + "\\c[0]!\n要摘下吗？";
    }

    /**
     * {@code "{trainer}将\c[1]{name}\c[0]放进了 <icon=bagPocket{p}>\c[1]{pocket}\c[0]袋\1"}
     * - {@code pbBerryPlant} line 514 has no space before 袋, {@code pbPickBerry}
     * line 578 has one; both keep the control code.
     */
    private String pocketMessage(String itemname, int pocket, boolean pickBerry) {
        return playerName() + "将\\c[1]" + itemname + "\\c[0]放进了 <icon=bagPocket" + pocket + ">"
                + "\\c[1]" + pocketName(pocket) + "\\c[0]" + (pickBerry ? " 袋" : "袋") + "\u0001";
    }

    // ------------------------------------------------------------------
    // The stepper
    // ------------------------------------------------------------------

    /**
     * One frame while the interpreter is in WAIT_BERRY.
     *
     * @return true when the whole interaction ended and the event may continue
     */
    public boolean update() {
        if (wait != Wait.NONE) {
            boolean done = wait == Wait.MESSAGE ? tickMessage() : tickChoice();
            if (done) {
                wait = Wait.NONE;
                Runnable next = onDone;
                onDone = null;
                if (next != null) {
                    next.run();
                }
            }
        }
        return finished;
    }

    /** The bag UI picked (or cancelled) the item a {@code BERRY} step asked for. */
    public void itemChosen(MenuService.Request request) {
        if (!itemWaiting) {
            return;
        }
        itemWaiting = false;
        java.util.function.Consumer<String> callback = itemCallback;
        itemCallback = null;
        String picked = null;
        if (request != null && request.result > 0) {
            picked = request.text == null || request.text.isEmpty()
                    ? internalName(request.result) : request.text;
        }
        if (callback != null) {
            callback.accept(picked);
        }
    }

    // ------------------------------------------------------------------
    // Message / command window presentation
    // ------------------------------------------------------------------

    /** Shows one message and continues with {@code next} after its confirm. */
    private void showMessage(String text, Runnable next) {
        Array<String> raw = new Array<>();
        raw.add(text);
        MessageText.Parsed parsed = MessageText.parse(raw, state, messageColumns);
        pageLines = parsed.lines;
        page = 0;
        linesPerPage = parsed.lineCount > 0 ? parsed.lineCount : MessageService.LINES_PER_PAGE;
        pageWaits = parsed.waitForInput;
        pageSpeaker = parsed.speaker;
        pageSkin = parsed.skin;
        onDone = next;
        wait = Wait.MESSAGE;
        showPage();
    }

    /** Shows a message plus a command window; {@code next} receives the index. */
    private void showChoice(String text, String[] options, int cancelType,
                            java.util.function.IntConsumer next) {
        Array<String> raw = new Array<>();
        raw.add(text);
        MessageText.Parsed parsed = MessageText.parse(raw, state, messageColumns);
        // pbMessageDisplay keeps the question on screen while the command
        // window is up (Messages:1304-1318).
        messages.showLines(parsed.lines, parsed.speaker, true,
                parsed.lineCount > 0 ? parsed.lineCount : MessageService.LINES_PER_PAGE, parsed.skin);
        Array<String> choices = new Array<>();
        for (String option : options) {
            choices.add(option);
        }
        choiceResult = Integer.MIN_VALUE;
        messages.showChoices(choices, cancelType);
        onDone = () -> next.accept(choiceResult);
        wait = Wait.CHOICE;
    }

    /** pbConfirmMessage: the plugin's 是/否 pair, cancel counts as 否. */
    private void showConfirm(String text, java.util.function.Consumer<Boolean> next) {
        showChoice(text, new String[] { CONFIRM_YES, CONFIRM_NO }, 1,
                selected -> next.accept(selected == 0));
    }

    private void showPage() {
        Array<String> slice = new Array<>();
        int start = page * linesPerPage;
        for (int i = start; i < Math.min(start + linesPerPage, pageLines.size); i++) {
            slice.add(pageLines.get(i));
        }
        messages.showLines(slice, pageSpeaker, pageWaits, linesPerPage, pageSkin);
    }

    private boolean tickMessage() {
        if (!messages.visible()) {
            return true;
        }
        if (!messages.waiting()) { // "\^": continue without a confirm
            return nextMessagePage();
        }
        if (input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL)) {
            messages.confirm();
            return nextMessagePage();
        }
        return false;
    }

    private boolean nextMessagePage() {
        page++;
        if (page * linesPerPage >= pageLines.size) {
            messages.close();
            return true;
        }
        showPage();
        return false;
    }

    private boolean tickChoice() {
        if (!messages.choiceMode()) {
            choiceResult = -1;
            messages.close();
            return true;
        }
        if (input.wasPressed(GameAction.UP)) {
            messages.moveCursor(-1);
        } else if (input.wasPressed(GameAction.DOWN)) {
            messages.moveCursor(1);
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            messages.confirm();
            choiceResult = messages.selected();
            return true;
        }
        if (input.wasPressed(GameAction.CANCEL)) {
            messages.cancel();
            if (!messages.visible()) {
                choiceResult = -1;
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // The bag pick (pbChooseItemScreen)
    // ------------------------------------------------------------------

    private void pickItem(java.util.function.Predicate<String> filter,
                          java.util.function.Consumer<String> next) {
        itemCallback = next;
        itemWaiting = true;
        boolean opened = itemHost != null && itemHost.chooseItem(filter);
        if (!opened) {
            log.warn("pbChooseItemScreen without a bag UI; treated as cancelled");
            itemWaiting = false;
            itemCallback = null;
            next.accept(null);
        }
    }

    // ------------------------------------------------------------------
    // Data helpers
    // ------------------------------------------------------------------

    private void turnToStage() {
        if (mapPort == null) {
            return;
        }
        switch (stage) { // lines 325-331 (2=down, 4=left, 6=right, 8=up)
            case 1:
            case 2:
                mapPort.turnEvent(eventId, 2);
                break;
            case 3:
                mapPort.turnEvent(eventId, 4);
                break;
            case 4:
                mapPort.turnEvent(eventId, 6);
                break;
            case 5:
                mapPort.turnEvent(eventId, 8);
                break;
            default:
                break;
        }
    }

    private void saveVariable() {
        if (mapPort != null) {
            mapPort.setEventVariable(eventId, berryData);
        }
    }

    private int[] plantData(int itemId) {
        return pbs == null ? BerryPlants.DEFAULT_PLANT_DATA : pbs.berryPlantData(itemId);
    }

    private String mulchName(int itemId) {
        PbsData.Item item = itemById(itemId);
        return item == null ? null : item.internalName;
    }

    private PbsData.Item itemById(int id) {
        return pbs == null ? null : pbs.itemById(id);
    }

    private PbsData.Item itemData(String internalName) {
        return pbs == null ? null : pbs.item(internalName);
    }

    private int itemId(String internalName) {
        PbsData.Item item = itemData(internalName);
        return item == null ? 0 : item.id;
    }

    private String internalName(int id) {
        PbsData.Item item = itemById(id);
        return item == null ? "" : item.internalName;
    }

    private String itemName(int id) {
        PbsData.Item item = itemById(id);
        return item == null ? internalName(id) : item.name;
    }

    private String itemPlural(int id) {
        PbsData.Item item = itemById(id);
        return item == null ? itemName(id) : item.namePlural;
    }

    /** {@code pbIsBerry?}: ITEM_TYPE == 5 (PItem_Items:99-102). */
    private boolean isBerry(String internalName) {
        PbsData.Item item = itemData(internalName);
        return item != null && item.extra.size > 0 && "5".equals(item.extra.get(0));
    }

    /** {@code pbIsMulch?}: ITEM_TYPE == 11 (PItem_Items:129-132). */
    private boolean isMulch(String internalName) {
        PbsData.Item item = itemData(internalName);
        return item != null && item.extra.size > 0 && "11".equals(item.extra.get(0));
    }

    /** {@code pbGetPocket(item)} (PItem_Items:44-47). */
    private int pocketOf(int itemId) {
        PbsData.Item item = itemById(itemId);
        return item == null ? 0 : item.pocket;
    }

    private String pocketName(int pocket) {
        return pocket >= 0 && pocket < POCKET_NAMES.length ? POCKET_NAMES[pocket] : "";
    }

    /**
     * {@code $PokemonBag.pbCanStore?}: the project's pockets are unlimited
     * (Settings:187 {@code BAG_MAX_POCKET_SIZE} all -1) and each slot holds
     * {@code BAG_MAX_PER_SLOT} = 999, so any positive quantity fits across new
     * slots (PItem_Bag:105-115, 327-343).
     */
    private boolean canStore(int itemId, int qty) {
        return qty > 0;
    }

    private void store(int itemId, int qty) {
        if (inventory != null) {
            inventory.add(internalName(itemId), qty);
        }
    }

    private void remove(int itemId, int qty) {
        if (inventory != null) {
            inventory.remove(internalName(itemId), qty);
        }
    }

    private String playerName() {
        String name = state == null ? null : state.playerName();
        return name == null || name.isEmpty() ? "训练家" : name;
    }

    private void finish() {
        finished = true;
    }
}
