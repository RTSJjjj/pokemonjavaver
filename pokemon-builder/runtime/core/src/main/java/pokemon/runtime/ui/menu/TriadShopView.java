package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.audio.UiSounds;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.TriadCard;
import pokemon.runtime.state.TriadStorage;
import pokemon.runtime.ui.WindowSkin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 235_PMinigame_TripleTriad {@code pbBuyTriads} (:1065-1157) and {@code pbSellTriads} (:1159-1261): the card shop. The map
 * scrolls three tiles to the right ({@code pbScrollMap(4,3,5)}: 8 px per 40 fps tick), a command window lists the cards on the
 * left half, the money window sits at the top right and a preview of the highlighted card at the right. The plugin's own
 * English lines are kept as they are.
 */
public final class TriadShopView implements MiniGame {
    private static final float ROW = 32f;
    private static final float BORDER = 32f;
    private static final float SCROLL_STEP = 8f;                                 // 2**5 / 128 tile per frame
    private static final float SCROLL_DISTANCE = 96f;                            // 3 tiles

    /** One line of the command window. */
    private static final class Entry {
        final String label;
        final int price;
        final String species;
        final String name;

        Entry(String label, int price, String species, String name) {
            this.label = label;
            this.price = price;
            this.species = species;
            this.name = name;
        }
    }

    private enum Phase { INTRO_MESSAGE, SCROLL_IN, LIST, SCROLL_OUT, DONE }

    private final RuntimeContext context;
    private final PbsData pbs;
    private final boolean buying;
    private final PbMessage pbMessage;
    private final MenuClock clock = new MenuClock();
    private List<Entry> entries = new ArrayList<>();
    private Phase phase = Phase.SCROLL_IN;
    private float shift;
    private int index;
    private int top;
    private boolean finished;
    private boolean listActive = true;

    // pbMessageChooseNumber
    private boolean numberActive;
    private String numberText = "";
    private int numberDigits;
    private int numberValue;
    private int numberIndex;
    private int numberMax;
    private int numberFrame;
    private IntConsumer numberDone;

    public TriadShopView(RuntimeContext context, boolean buying) {
        this.context = context;
        this.pbs = context.pbsData();
        this.buying = buying;
        this.pbMessage = new PbMessage(context);
        if (context.gameState().fieldGlobals().triads == null) {
            context.gameState().fieldGlobals().triads = new TriadStorage(pbs == null ? 1000 : pbs.species.size);
        }
        buildEntries();
        if (buying && entries.isEmpty()) {
            phase = Phase.INTRO_MESSAGE;
            pbMessage.start("There are no cards that you can buy.", null, 0, 0, ignored -> finished = true);   // :1080
        } else if (!buying && storage().length() == 0) {
            phase = Phase.INTRO_MESSAGE;
            pbMessage.start("You have no cards.", null, 0, 0, ignored -> finished = true);                // :1174
        }
    }

    private TriadStorage storage() {
        return context.gameState().fieldGlobals().triads;
    }

    private AudioManager audio() {
        return context.audioManager();
    }

    private static String formatted(int value) {
        return String.format("%,d", value);                                      // Integer#to_s_formatted
    }

    private String speciesName(String internal) {
        PbsData.Species species = pbs == null ? null : pbs.species(internal);
        return species == null || species.name == null ? internal : species.name;
    }

    /** The command lines (:1066-1092 / :1160-1171). */
    private void buildEntries() {
        entries = new ArrayList<>();
        if (pbs == null) return;
        if (buying) {
            List<PbsData.Species> list = new ArrayList<>();
            for (PbsData.Species species : pbs.species.values()) list.add(species);
            list.sort((a, b) -> Integer.compare(a.id, b.id));                    // for i in 1..PBSpecies.maxValue
            for (PbsData.Species species : list) {
                if (!context.gameState().trainer().owned.contains(species.internalName)) continue;   // next if !$Trainer.owned[i]
                if (species.name == null) continue;
                int price = new TriadCard(pbs, species).price();
                entries.add(new Entry(species.name + " - $" + formatted(price), price, species.internalName, species.name));
            }
            entries.sort((a, b) -> a.name.compareTo(b.name));                    // commands.sort! by name
        } else {
            for (int i = 0; i < storage().length(); i++) {
                TriadStorage.Slot slot = storage().get(i);
                entries.add(new Entry(speciesName(slot.species) + " x" + slot.count, 0, slot.species, speciesName(slot.species)));
            }
            entries.add(new Entry("CANCEL", 0, null, "CANCEL"));
        }
        index = Math.max(0, Math.min(index, entries.size() - 1));
    }

    // =====================================================================
    // update
    // =====================================================================

    @Override
    public boolean update(InputManager input) {
        int ticks = clock.advance();
        if (pbMessage.active()) {
            pbMessage.update(input, ticks);
            return finished;
        }
        if (numberActive) {
            updateNumber(input, ticks);
            return finished;
        }
        switch (phase) {
            case SCROLL_IN:                                                      // pbScrollMap(4,3,5)
                shift = Math.min(SCROLL_DISTANCE, shift + SCROLL_STEP * ticks);
                if (shift >= SCROLL_DISTANCE) phase = Phase.LIST;
                break;
            case LIST:
                updateList(input);
                break;
            case SCROLL_OUT:                                                     // pbScrollMap(6,3,5)
                shift = Math.max(0f, shift - SCROLL_STEP * ticks);
                if (shift <= 0f) {
                    phase = Phase.DONE;
                    finished = true;
                }
                break;
            default:
                break;
        }
        return finished;
    }

    private void updateList(InputManager input) {
        int max = entries.size();
        if (input.wasRepeated(GameAction.DOWN) && max > 0) {                     // Window_DrawableCommand#update
            if (index < max - 1 || input.wasPressed(GameAction.DOWN)) {
                index = (index + 1) % max;
                MenuSe.cursor(audio());
            }
        } else if (input.wasRepeated(GameAction.UP) && max > 0) {
            if (index > 0 || input.wasPressed(GameAction.UP)) {
                index = (index + max - 1) % max;
                MenuSe.cursor(audio());
            }
        }
        int rows = (int) ((ScreenMetrics.logicalHeight() - BORDER) / ROW);
        if (index < top) top = index;
        if (index >= top + rows) top = index - rows + 1;
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {   // Input::B
            UiSounds.cancel(audio());
            phase = Phase.SCROLL_OUT;
            return;
        }
        if (!input.wasPressed(GameAction.CONFIRM)) return;
        MenuSe.decision(audio());
        if (buying) buy(); else sell();
    }

    private int money() {
        return context.gameState().trainer().money;
    }

    private void message(String text) {
        listActive = false;
        pbMessage.start(text, null, 0, 0, ignored -> listActive = true);
    }

    private void confirm(String text, java.util.function.Consumer<Boolean> result) {
        listActive = false;
        pbMessage.start(text, Arrays.asList("是", "否"), 2, 0, i -> {              // pbConfirmMessage
            listActive = true;
            result.accept(i == 0);
        });
    }

    /** :1105-1151 */
    private void buy() {
        Entry entry = entries.get(index);
        int price = entry.price;
        if (money() < price) {
            message("You don't have enough money.");                             // :1109
            return;
        }
        int maxafford = price <= 0 ? 99 : money() / price;                       // :1112
        if (maxafford > 99) maxafford = 99;
        startNumber("The " + entry.name + " card? Certainly. How many would you like?", maxafford, quantity -> {   // :1119-1120
            if (quantity <= 0) return;                                           // :1122
            int total = price * quantity;
            confirm(entry.name + ", and you want " + quantity + ". That will be $" + formatted(total) + ". OK?", yes -> {   // :1124
                if (!yes) return;
                if (money() < total) {                                           // :1125
                    message("You don't have enough money.");
                    return;
                }
                if (!storage().pbCanStore(entry.species, quantity)) {            // :1129
                    message("You have no room for more cards.");                 // :1130
                    return;
                }
                storage().pbStoreItem(entry.species, quantity);                  // :1133
                context.gameState().trainer().money -= total;                    // :1134
                message("Here you are! Thank you!\\se[Mart buy item]");          // :1136
            });
        });
    }

    /** :1197-1257 */
    private void sell() {
        if (index >= storage().length()) {                                       // CANCEL
            phase = Phase.SCROLL_OUT;
            return;
        }
        Entry entry = entries.get(index);
        PbsData.Species species = pbs.species(entry.species);
        int quantity = storage().pbQuantity(entry.species);
        int price = new TriadCard(pbs, species).price();
        if (price == 0) {                                                        // :1206
            message("The " + entry.name + " card? Oh, no. I can't buy that.");
            return;
        }
        IntConsumer sellQuantity = qty -> {
            if (qty <= 0) return;                                                // :1219
            int total = price / 4 * qty;                                         // :1220-1221
            confirm("I can pay $" + formatted(total) + ". Would that be OK?", yes -> {   // :1222
                if (!yes) return;
                context.gameState().trainer().money += total;                    // :1223
                storage().pbDeleteItem(entry.species, qty);                      // :1225
                buildEntries();                                                  // :1227-1233 the commands again
                message("Turned over the " + entry.name + " card and received $" + formatted(total) + ".\\se[Mart buy item]");   // :1226
            });
        };
        if (quantity > 1) {                                                      // :1210
            startNumber("The " + entry.name + " card? How many would you like to sell?", quantity, sellQuantity);   // :1216
        } else {
            sellQuantity.accept(quantity);
        }
    }

    // =====================================================================
    // pbMessageChooseNumber (071_Messages:~770-800, Window_InputNumberPokemon)
    // =====================================================================

    private void startNumber(String text, int max, IntConsumer done) {
        numberText = text;
        numberMax = max;
        numberDigits = String.valueOf(Math.max(1, max)).length();               // ChooseNumberParams#setRange
        numberValue = Math.max(0, Math.min(1, (int) Math.pow(10, numberDigits) - 1));   // setInitialValue(1)
        numberIndex = numberDigits - 1;
        numberFrame = 0;
        numberDone = done;
        numberActive = true;
        listActive = false;
    }

    private void updateNumber(InputManager input, int ticks) {
        numberFrame = (numberFrame + ticks) % 30;
        if (input.wasRepeated(GameAction.UP) || input.wasRepeated(GameAction.DOWN)) {
            MenuSe.cursor(audio());
            int place = (int) Math.pow(10, numberDigits - 1 - numberIndex);
            int n = numberValue / place % 10;
            numberValue -= n * place;
            n = input.wasRepeated(GameAction.UP) ? (n + 1) % 10 : (n + 9) % 10;
            numberValue += n * place;
        } else if (input.wasRepeated(GameAction.RIGHT)) {
            if (numberDigits >= 2) {
                MenuSe.cursor(audio());
                numberIndex = (numberIndex + 1) % numberDigits;
                numberFrame = 0;
            }
        } else if (input.wasRepeated(GameAction.LEFT)) {
            if (numberDigits >= 2) {
                MenuSe.cursor(audio());
                numberIndex = (numberIndex + numberDigits - 1) % numberDigits;
                numberFrame = 0;
            }
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            if (numberValue > numberMax || numberValue < 1) {
                MenuSe.buzzer(audio());
            } else {
                MenuSe.decision(audio());
                finishNumber(numberValue);
            }
        } else if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            UiSounds.cancel(audio());
            finishNumber(0);                                                     // setCancelValue(0)
        }
    }

    private void finishNumber(int value) {
        numberActive = false;
        listActive = true;
        IntConsumer done = numberDone;
        numberDone = null;
        if (done != null) done.accept(value);
    }

    // =====================================================================
    // render
    // =====================================================================

    @Override
    public boolean overMap() {
        return true;
    }

    @Override
    public float mapShift() {
        return shift;
    }

    @Override
    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        if (phase == Phase.INTRO_MESSAGE) {
            if (pbMessage.active()) pbMessage.render(b, a, f, skin, skin, w, h);
            return;
        }
        Color[] c = MenuPanel.textColors(skin);
        // the command window (Window_CommandPokemonEx 0,0,W/2,H)
        float cw = w / 2f;
        MenuPanel.window(b, a, skin, 0f, 0f, cw, h);
        int rows = (int) ((h - BORDER) / ROW);
        for (int i = top; i < entries.size() && i < top + rows; i++) {
            float rowTop = 16f + (i - top) * ROW;
            if (i == index && listActive) {                                      // drawCursor
                Texture arrow = a.graphic("Pictures", skin != null && skin.dark ? "selarrow_white" : "selarrow");
                if (arrow != null) b.draw(arrow, 16f, h - rowTop - arrow.getHeight());
            }
            MenuPanel.text(b, f, entries.get(i).label, 16f + 16f, h - (rowTop + (ROW - f.lineHeight()) / 2f), cw - 64f);
        }
        // the money window (Window_UnformattedTextPokemon, resizeToFit)
        String money = "$" + formatted(money());
        float gw = Math.max(f.width("Money:"), f.width(money)) + BORDER;
        float gh = 2 * ROW + BORDER;
        MenuPanel.window(b, a, skin, w - gw, h - gh, gw, gh);
        f.draw(b, "Money:", w - gw + 16f, h - (16f + (ROW - f.lineHeight()) / 2f), c[0], c[1]);
        f.draw(b, money, w - gw + 16f, h - (16f + ROW + (ROW - f.lineHeight()) / 2f), c[0], c[1]);
        // the preview card
        TriadCard card = null;
        if (pbs != null && !entries.isEmpty()) {
            Entry current = entries.get(Math.min(index, entries.size() - 1));
            PbsData.Species species = current.species == null ? null : pbs.species(current.species);
            if (species != null) card = new TriadCard(pbs, species);
        }
        if (card != null) drawCard(b, a, card, w * 3f / 4f - 40f, h / 2f - 48f, h);
        if (numberActive) {
            PbMessage.renderResting(b, a, f, skin, numberText, w, h);
            drawNumber(b, a, f, skin, c, w, h);
        }
        if (pbMessage.active()) pbMessage.render(b, a, f, skin, skin, w, h);
    }

    /** {@code TriadCard#createBitmap(1)} (:99-134): the card of the player, 80x96. */
    private void drawCard(SpriteBatch b, MenuAssets a, TriadCard card, float x, float y, float h) {
        Texture back = a.graphic("Pictures", "triad_card_player");
        if (back != null) b.draw(back, x, h - y - back.getHeight());
        Texture types = a.graphic("Pictures", "types");
        if (types != null && card.type >= 0 && card.type * 28 + 28 <= types.getHeight()) {
            b.setColor(1f, 1f, 1f, 192f / 255f);                                 // blt(8,50,typebitmap,typerect,192)
            b.draw(types, x + 8f, h - (y + 50f) - 28f, 64f, 28f, 0, card.type * 28, 64, 28, false, false);
            b.setColor(Color.WHITE);
        }
        Texture icon = a.icon(String.format("icon%03d", card.species.id));
        if (icon == null) icon = a.icon("icon" + card.species.internalName);
        if (icon != null) b.draw(icon, x + 8f, h - (y + 24f) - 64f, 64f, 64f, 0, 0, 64, 64, false, false);
        Texture numbers = a.graphic("Pictures", "triad_numbers");
        if (numbers != null) {
            number(b, numbers, card.west, x + 8f, y + 16f, h);
            number(b, numbers, card.north, x + 22f, y + 6f, h);
            number(b, numbers, card.east, x + 36f, y + 16f, h);
            number(b, numbers, card.south, x + 22f, y + 26f, h);
        }
    }

    private void number(SpriteBatch b, Texture numbers, int value, float x, float y, float h) {
        b.draw(numbers, x, h - y - 16f, 16f, 16f, value * 16, 0, 16, 16, false, false);
    }

    private void drawNumber(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, Color[] c, float w, float h) {
        float width = numberDigits * 24 + 8 + BORDER;                            // Window_InputNumberPokemon :622
        float height = 32 + BORDER;
        float x = w - width;
        float topY = h - (BORDER + 2 * ROW) - height;                            // pbPositionNearMsgWindow(:right)
        MenuPanel.window(b, a, skin, x, h - topY - height, width, height);
        String digits = String.format("%0" + numberDigits + "d", numberValue);
        for (int i = 0; i < numberDigits; i++) {
            float cx = x + 16f + i * 24f + 12f;
            String d = digits.substring(i, i + 1);
            f.drawCentered(b, d, cx, h - (topY + 16f + (ROW - f.lineHeight()) / 2f), c[0], c[1]);
            if (i == numberIndex && numberFrame / 15 == 0) {                     // the cursor underline
                float tw = f.width(d);
                b.setColor(c[0]);
                b.draw(a.pixel(), cx - tw / 2f, h - (topY + 16f + 30f) - 2f, tw, 2f);
                b.setColor(Color.WHITE);
            }
        }
    }
}
