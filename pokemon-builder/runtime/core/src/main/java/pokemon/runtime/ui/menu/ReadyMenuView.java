package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.BagMemory;

import java.util.List;

/**
 * 221_PScreen_ReadyMenu {@code ReadyMenuButton} and {@code PokemonReadyMenu_Scene}: F5 on the map shows the hidden moves the party
 * can use (left) and the registered items (right) as buttons that slide around the selected one. Up / Down choose, Left / Right
 * switch the side, C picks, B closes. The map stays visible behind the buttons.
 */
public final class ReadyMenuView {

    /** One button: a move of the party ({@code [move, name, true, party index]}) or an item ({@code [item, name, false]}). */
    public static final class Command {
        public final String id;
        public final String name;
        public final boolean move;
        public final int partyIndex;

        public Command(String id, String name, boolean move, int partyIndex) {
            this.id = id;
            this.name = name;
            this.move = move;
            this.partyIndex = partyIndex;
        }
    }

    /** What a frame of {@link #update} decided. */
    public static final class Result {
        /** 0 = a move, 1 = an item, -1 = closed with B. */
        public final int side;
        public final Command command;

        Result(int side, Command command) {
            this.side = side;
            this.command = command;
        }
    }

    private static final Color BASE = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color SHADOW = new Color(40f / 255f, 40f / 255f, 40f / 255f, 1f);

    private final RuntimeContext context;
    private final List<Command> moves;
    private final List<Command> items;
    private final int[] index;                    // @index: [move cursor, item cursor, side]
    private final int[] selected = new int[2];    // each button list's @selected
    private boolean visible = true;
    private int frame;

    public ReadyMenuView(RuntimeContext context, List<Command> moves, List<Command> items) {
        this.context = context;
        this.moves = moves;
        this.items = items;
        this.index = context.gameState().inventory().bagMemory().registeredIndex;
        if (index[0] >= moves.size() && moves.size() > 0) {                  // :124-129
            index[0] = moves.size() - 1;
        }
        if (index[1] >= items.size() && items.size() > 0) {
            index[1] = items.size() - 1;
        }
        if (index[2] == 0 && moves.isEmpty()) {
            index[2] = 1;
        } else if (index[2] == 1 && items.isEmpty()) {
            index[2] = 0;
        }
        selected[0] = index[0];
        selected[1] = index[1];
    }

    public boolean visible() {
        return visible;
    }

    /** {@code pbHideMenu} / {@code pbShowMenu}. */
    public void visible(boolean value) {
        visible = value;
    }

    private List<Command> current() {
        return index[2] == 0 ? moves : items;
    }

    /** One frame while the menu is shown (pbShowCommands' loop); null while nothing was decided. */
    public Result update(InputManager input, pokemon.runtime.audio.AudioManager audio) {
        frame++;
        if (!visible) {
            return null;
        }
        List<Command> list = current();
        int old = index[index[2]];
        if (!list.isEmpty()) {                                                // Window_CommandPokemon: Up / Down move the index, wrapping on a press
            if (input.wasPressed(GameAction.UP) || input.wasRepeated(GameAction.UP)) {
                if (index[index[2]] > 0) index[index[2]]--;
                else if (input.wasPressed(GameAction.UP)) index[index[2]] = list.size() - 1;
            } else if (input.wasPressed(GameAction.DOWN) || input.wasRepeated(GameAction.DOWN)) {
                if (index[index[2]] < list.size() - 1) index[index[2]]++;
                else if (input.wasPressed(GameAction.DOWN)) index[index[2]] = 0;
            }
        }
        if (index[index[2]] != old) {
            MenuSe.cursor(audio);
            selected[index[2]] = index[index[2]];                             // pbUpdate: the buttons of this side follow
        }
        if (input.wasPressed(GameAction.LEFT) && index[2] == 1 && !moves.isEmpty()) {   // :171-176
            index[2] = 0;
            MenuSe.cursor(audio);
        } else if (input.wasPressed(GameAction.RIGHT) && index[2] == 0 && !items.isEmpty()) {
            index[2] = 1;
            MenuSe.cursor(audio);
        } else if (input.wasPressed(GameAction.CANCEL)) {
            MenuSe.close(audio);                                              // pbPlayCloseMenuSE
            return new Result(-1, null);
        } else if (input.wasPressed(GameAction.CONFIRM) && !list.isEmpty()) {
            return new Result(index[2], list.get(index[index[2]]));
        }
        return null;
    }

    // ---------------------------------------------------------------- drawing

    public void render(SpriteBatch b, MenuAssets a, MenuFont f) {
        if (!visible) {
            return;
        }
        float h = ScreenMetrics.logicalHeight();
        for (int i = 0; i < moves.size(); i++) {
            drawButton(b, a, f, h, moves.get(i), i, selected[0]);
        }
        for (int i = 0; i < items.size(); i++) {
            drawButton(b, a, f, h, items.get(i), i, selected[1]);
        }
    }

    /** {@code ReadyMenuButton#refresh} (:62-96): the position, the half of the button picture, the icon and the text. */
    private void drawButton(SpriteBatch b, MenuAssets a, MenuFont f, float h, Command command, int i, int sel) {
        Texture button = a.graphic("Pictures/Ready Menu", command.move ? "icon_movebutton" : "icon_itembutton");
        if (button == null) {
            return;
        }
        float w = ScreenMetrics.logicalWidth();
        int bw = button.getWidth();
        int half = button.getHeight() / 2;
        boolean on = sel == i && (index[2] == 0) == command.move;
        float y = (h - half) / 2f - (sel - i) * (half + 4f);
        float x;
        if (command.move) {
            x = on ? 0f : -16f;
        } else {
            x = on ? w - bw : w + 16f - bw;
        }
        b.setColor(Color.WHITE);
        b.draw(button, x, h - y - half, bw, half, 0, on ? half : 0, bw, half, false, false);
        if (command.move) {
            drawPokemonIcon(b, a, h, command, x + 52f, y + 32f);
        } else {
            drawItemIcon(b, a, h, command, x + 50f, y + half / 2f);
        }
        float textX = command.move ? 164f : important(command.id) ? 146f : 124f;
        outlined(b, f, command.name, x + textX, y + 18f, 2, h);
        if (!command.move && !important(command.id)) {
            long qty = context.gameState().inventory().count(command.id);
            outlined(b, f, qty > 99 ? ">99" : "x" + qty, x + 230f, y + 18f, 1, h);
        }
    }

    private boolean important(String item) {
        PbsData.Item data = context.pbsData() == null ? null : context.pbsData().item(item);
        return data != null && (data.pocket == 8 || data.fieldUse == 4);       // pbIsImportantItem?
    }

    /** {@code ItemIconSprite} with the Center offset: the 48x48 picture around (x, y). */
    private void drawItemIcon(SpriteBatch b, MenuAssets a, float h, Command command, float x, float y) {
        Texture icon = ItemIcons.of(a, context.pbsData(), command.id);
        if (icon == null) {
            return;
        }
        int size = Math.min(48, icon.getHeight());
        b.draw(icon, x - size / 2f, h - (y - size / 2f) - size, size, size, 0, 0, size, size, false, false);
    }

    /** {@code PokemonIconSprite} with the Center offset ({@code ox = w/2, oy = h*5/8}): the party icon, animated by its HP. */
    private void drawPokemonIcon(SpriteBatch b, MenuAssets a, float h, Command command, float x, float y) {
        com.badlogic.gdx.utils.Array<Pokemon> party = context.gameState().trainer().party.members();
        Pokemon p = command.partyIndex >= 0 && command.partyIndex < party.size ? party.get(command.partyIndex) : null;
        if (p == null || p.species == null) {
            return;
        }
        String suffix = (p.shiny ? "s" : "") + (p.egg ? "egg" : "");
        Texture icon = a.icon(String.format("icon%03d%s", p.species.id, suffix));
        if (icon == null) icon = a.icon("icon" + p.species.internalName + suffix);
        if (icon == null) {
            return;
        }
        int size = icon.getHeight();
        int frames = Math.max(1, icon.getWidth() / size);
        int shown = frames == 1 ? 0 : (frame / counterLimit(p, frames)) % frames;   // PokemonIconSprite#update / counterLimit (:166-180)
        float left = x - size / 2f;
        float top = y - size * 5f / 8f;
        b.draw(icon, left, h - top - size, size, size, shown * size, 0, size, size, false, false);
    }

    private static int counterLimit(Pokemon p, int frames) {
        if (p.hp <= 0) return Integer.MAX_VALUE / 2;                            // fainted: no animation
        int ret = 40 / 4;
        if (p.hp <= p.maxHp() / 4) ret *= 4;
        else if (p.hp <= p.maxHp() / 2) ret *= 2;
        ret /= frames;
        return Math.max(1, ret);
    }

    /** {@code pbDrawTextPositions} with the outline flag: the shadow colour in the eight directions, then the base. */
    private void outlined(SpriteBatch b, MenuFont f, String text, float x, float top, int align, float h) {
        float baseline = h - (top + (32f - f.lineHeight()) / 2f);
        float left = x;
        float width = f.width(text);
        if (align == 2) left = x - width / 2f;
        else if (align == 1) left = x - width;
        int[][] around = {{2, -2}, {0, -2}, {-2, -2}, {2, 0}, {-2, 0}, {2, 2}, {0, 2}, {-2, 2}};
        com.badlogic.gdx.graphics.g2d.BitmapFont font = f.font();
        if (font == null) {
            return;
        }
        font.setColor(SHADOW);
        for (int[] d : around) {
            font.draw(b, text, left + d[0], baseline + d[1]);
        }
        font.setColor(BASE);
        font.draw(b, text, left, baseline);
        font.setColor(Color.WHITE);
    }

    /** The cursors the menu remembers ({@code $PokemonBag.registeredIndex}), kept by reference. */
    public BagMemory memory() {
        return context.gameState().inventory().bagMemory();
    }
}
