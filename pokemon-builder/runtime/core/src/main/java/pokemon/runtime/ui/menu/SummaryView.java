package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PBExperience;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.Ribbons;
import pokemon.runtime.ui.WindowSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * BW PScreen_Summary: the detail pages opened by "查看详情" in the party screen,
 * including the page sub-modes from the plugin ({@code pbOptions},
 * {@code pbMoveSelection}, {@code pbRibbonSelection}, {@code pbMarking}).
 *
 * <p>Every coordinate is the plugin's top-origin value and is converted with
 * {@code screenHeight - y} at draw time. Text alignment follows this project's
 * {@code pbDrawTextPositions}: 1 = right, 2 = centre, otherwise left.</p>
 */
public final class SummaryView {

    private static final int PAGES = 0;
    private static final int OPTIONS = 1;
    private static final int MOVE_DETAIL = 2;
    private static final int RIBBON_SELECT = 3;
    private static final int MARKING = 4;
    /** pbStartForgetScene / pbChooseMoveToForget (303_BW_PScreen_Summary:229-262, 1449-1481). */
    private static final int FORGET = 5;

    private static final Color WHITE = new Color(1f, 1f, 1f, 1f);
    private static final Color HEAD_SHADOW = new Color(132f / 255f, 132f / 255f, 132f / 255f, 1f);
    private static final Color BASE = new Color(90f / 255f, 82f / 255f, 82f / 255f, 1f);
    private static final Color SHADOW = new Color(165f / 255f, 165f / 255f, 173f / 255f, 1f);
    private static final Color WHITE_SHADOW = new Color(165f / 255f, 165f / 255f, 173f / 255f, 1f);
    private static final Color MEMO = new Color(0x40 / 255f, 0x40 / 255f, 0x40 / 255f, 1f);
    private static final Color MEMO_SHADOW = new Color(0xB0 / 255f, 0xB0 / 255f, 0xB0 / 255f, 1f);
    private static final Color MEMO_BLUE = new Color(0x00 / 255f, 0x00 / 255f, 0xD6 / 255f, 1f);
    private static final Color MEMO_BLUE_SHADOW = new Color(0x73 / 255f, 0x94 / 255f, 0xFF / 255f, 1f);
    private static final Color MALE = new Color(0f, 0f, 214f / 255f, 1f);
    private static final Color MALE_SHADOW = new Color(15f / 255f, 148f / 255f, 255f / 255f, 1f);
    private static final Color FEMALE = new Color(198f / 255f, 0f, 0f, 1f);
    private static final Color FEMALE_SHADOW = new Color(1f, 155f / 255f, 155f / 255f, 1f);
    private static final Color DEAD_BASE = new Color(198f / 255f, 0f, 0f, 1f);
    private static final Color DEAD_SHADOW = new Color(1f, 155f / 255f, 155f / 255f, 1f);
    private static final Color NAT_UP = new Color(206f / 255f, 148f / 255f, 156f / 255f, 1f);
    private static final Color NAT_DOWN = new Color(148f / 255f, 148f / 255f, 214f / 255f, 1f);
    private static final Color MOVE_SHADOW = new Color(123f / 255f, 123f / 255f, 123f / 255f, 1f);
    private static final Color RIBBON_NAME = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color RIBBON_NAME_SHADOW = new Color(104f / 255f, 104f / 255f, 104f / 255f, 1f);
    private static final Color CURSOR_MAIN = new Color(1f, 216f / 255f, 0f, 1f);

    private final RuntimeContext context;
    private final Array<Pokemon> party;
    private final Consumer<Pokemon> bagHost;
    private int index;
    private int page = 1;
    private float scrollX = 6f;
    private float scrollY = -36f + 512f;

    private int mode = PAGES;
    // Move selection (page 4).
    private int moveIndex;
    private int movePresel = -1;
    // Ribbon selection (page 5).
    private int ribbonSel;
    private int ribbonOffset;
    private int ribbonPresel = -1;
    // Options / marking.
    private int optionIndex;
    private int markingIndex;
    private int markingValue;
    private boolean markingDirty;
    private String notice = "";
    // Forget mode: the move to learn (null = pbStartChooseMoveScreen-like plain choice of one of the known moves).
    private PbsData.Move moveToLearn;
    private boolean forgetDone;
    private int forgetResult = -1;

    /** {@code pbStartForgetScreen(party, partyindex, moveToLearn)}: choose the move that makes room for {@code moveToLearn}. */
    public static SummaryView forForget(RuntimeContext context, Pokemon pokemon, PbsData.Move moveToLearn) {
        Array<Pokemon> single = new Array<>();
        single.add(pokemon);
        SummaryView view = new SummaryView(context, single, 0);
        view.page = 4;
        view.mode = FORGET;
        view.moveToLearn = moveToLearn;
        view.moveIndex = 0;
        return view;
    }

    /** True once the player chose (or backed out of) the forget screen. */
    public boolean forgetDone() {
        return forgetDone;
    }

    /** {@code pbChooseMoveToForget}'s answer: the slot to forget, or -1 (cancelled / the new move itself chosen). */
    public int forgetResult() {
        return forgetResult;
    }

    public SummaryView(RuntimeContext context, Array<Pokemon> party, int index) {
        this(context, party, index, null);
    }

    public SummaryView(RuntimeContext context, Array<Pokemon> party, int index, Consumer<Pokemon> bagHost) {
        this.context = context;
        this.party = party;
        this.bagHost = bagHost;
        this.index = Math.max(0, Math.min(index, party.size - 1));
    }

    /** PokemonSummaryScreen#pbStartScreen returns the index the summary ended on (B2W2 PC:1303/1307). */
    public int index() {
        return index;
    }

    /** @return true when the player closed the summary (B / X). */
    public boolean update(InputManager input) {
        scrollX += 1f;
        scrollY += 1f;
        Pokemon p = current();
        if (p == null) {
            return true;
        }
        switch (mode) {
            case MOVE_DETAIL: return updateMoveDetail(input, p);
            case FORGET: return updateForget(input, p);
            case RIBBON_SELECT: return updateRibbonSelect(input, p);
            case OPTIONS: return updateOptions(input, p);
            case MARKING: return updateMarking(input, p);
            default: return updatePages(input, p);
        }
    }

    private boolean updatePages(InputManager input, Pokemon p) {
        // BW PScreen_Summary#pbScene: Input::A stops the SE and replays the cry.
        if (input.wasPressed(GameAction.SPECIAL)) {
            context.audioManager().playCry(p.species == null ? 0 : p.species.id);
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            return true;
        }
        if (!p.egg) {
            if (input.wasPressed(GameAction.LEFT)) {
                page--;
                if (page < 1) page = 5;
            }
            if (input.wasPressed(GameAction.RIGHT)) {
                page++;
                if (page > 5) page = 1;
            }
        }
        if (input.wasPressed(GameAction.UP) && index > 0) {
            index--;
        }
        if (input.wasPressed(GameAction.DOWN) && index < party.size - 1) {
            index++;
        }
        if (input.wasPressed(GameAction.CONFIRM) && !p.egg) {
            if (page == 4) {
                moveIndex = 0;
                movePresel = -1;
                mode = MOVE_DETAIL;
            } else if (page == 5) {
                ribbonSel = 0;
                ribbonOffset = 0;
                ribbonPresel = -1;
                mode = RIBBON_SELECT;
            } else {
                optionIndex = 0;
                mode = OPTIONS;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // pbOptions (pages 1..3): 携带道具 / 拿回道具 / 标记 / 取消
    // ------------------------------------------------------------------
    private List<String> optionEntries(Pokemon p) {
        List<String> entries = new ArrayList<>();
        if (!p.egg) {
            entries.add("携带道具");
            if (p.item != null && !p.item.isEmpty()) {
                entries.add("拿回道具");
            }
            entries.add("标记");
        }
        entries.add("取消");
        return entries;
    }

    private boolean updateOptions(InputManager input, Pokemon p) {
        List<String> entries = optionEntries(p);
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            mode = PAGES;
            return false;
        }
        if (input.wasPressed(GameAction.UP)) optionIndex = Math.floorMod(optionIndex - 1, entries.size());
        if (input.wasPressed(GameAction.DOWN)) optionIndex = Math.floorMod(optionIndex + 1, entries.size());
        if (input.wasPressed(GameAction.CONFIRM)) {
            String choice = entries.get(optionIndex);
            if ("携带道具".equals(choice)) {
                if (bagHost != null) {
                    mode = PAGES;
                    bagHost.accept(p);
                }
            } else if ("拿回道具".equals(choice)) {
                notice = takeItem(p) ? "已放回背包。" : "没有携带道具。";
                mode = PAGES;
            } else if ("标记".equals(choice)) {
                markingValue = p.markings;
                markingIndex = 0;
                markingDirty = false;
                mode = MARKING;
            } else if ("取消".equals(choice)) {
                mode = PAGES;
            }
        }
        return false;
    }

    private boolean takeItem(Pokemon p) {
        if (p.item == null || p.item.isEmpty()) {
            return false;
        }
        context.gameState().inventory().add(p.item, 1);
        p.item = null;
        return true;
    }

    // ------------------------------------------------------------------
    // pbMoveSelection (page 4 Z).
    // ------------------------------------------------------------------
    private boolean updateMoveDetail(InputManager input, Pokemon p) {
        int numMoves = p.moves.size;
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            if (movePresel >= 0) {
                movePresel = -1;
            } else {
                mode = PAGES;
            }
            return false;
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            if (movePresel < 0) {
                if (numMoves > 0) {
                    movePresel = moveIndex;
                }
            } else {
                swapMoves(p, movePresel, moveIndex);
                movePresel = -1;
            }
            return false;
        }
        if (numMoves == 0) {
            return false;
        }
        if (input.wasPressed(GameAction.UP)) {
            moveIndex--;
            if (moveIndex < 0 || moveIndex >= numMoves) {
                moveIndex = numMoves - 1;
            }
        }
        if (input.wasPressed(GameAction.DOWN)) {
            moveIndex++;
            if (moveIndex >= numMoves) {
                moveIndex = 0;
            }
        }
        return false;
    }

    /** pbChooseMoveToForget (303:1449-1481): the five rows are the four moves and the one to learn. */
    private boolean updateForget(InputManager input, Pokemon p) {
        int maxmove = moveToLearn != null ? 4 : 3;
        int numMoves = p.moves.size;
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            if (moveToLearn != null) MenuSe.close(context.audioManager());      // :1458 pbPlayCloseMenuSE if moveToLearn>0
            forgetDone = true;
            forgetResult = -1;                                                  // selmove = 4
            return true;
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            MenuSe.decision(context.audioManager());                            // :1461
            forgetDone = true;
            forgetResult = moveIndex == 4 ? -1 : moveIndex;                     // :1480
            return true;
        }
        if (input.wasPressed(GameAction.UP)) {
            moveIndex -= 1;
            if (moveIndex < 0) moveIndex = maxmove;
            if (moveIndex < 4 && moveIndex >= numMoves) moveIndex = numMoves - 1;
            MenuSe.cursor(context.audioManager());
        } else if (input.wasPressed(GameAction.DOWN)) {
            moveIndex += 1;
            if (moveIndex > maxmove) moveIndex = 0;
            if (moveIndex < 4 && moveIndex >= numMoves) moveIndex = moveToLearn != null ? maxmove : 0;
            MenuSe.cursor(context.audioManager());
        }
        return false;
    }

    private static void swapMoves(Pokemon p, int a, int b) {
        if (a < 0 || b < 0 || a >= p.moves.size || b >= p.moves.size || a == b) {
            return;
        }
        Pokemon.MoveSlot tmp = p.moves.get(a);
        p.moves.set(a, p.moves.get(b));
        p.moves.set(b, tmp);
    }

    // ------------------------------------------------------------------
    // pbRibbonSelection (page 5 Z).
    // ------------------------------------------------------------------
    private boolean updateRibbonSelect(InputManager input, Pokemon p) {
        int numRibbons = p.ribbons.size;
        int numRows = Math.max((numRibbons + 3) / 4, 3);
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            if (ribbonPresel >= 0) {
                ribbonPresel = -1;
            } else {
                mode = PAGES;
            }
            return false;
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            if (ribbonPresel < 0) {
                if (ribbonSel < numRibbons) {
                    ribbonPresel = ribbonSel;
                }
            } else {
                swapRibbons(p, ribbonPresel, ribbonSel);
                if (ribbonSel >= p.ribbons.size) {
                    ribbonSel = Math.max(0, p.ribbons.size - 1);
                }
                ribbonPresel = -1;
            }
        } else if (input.wasPressed(GameAction.UP)) {
            ribbonSel -= 4;
            if (ribbonSel < 0) ribbonSel += numRows * 4;
        } else if (input.wasPressed(GameAction.DOWN)) {
            ribbonSel += 4;
            if (ribbonSel >= numRows * 4) ribbonSel -= numRows * 4;
        } else if (input.wasPressed(GameAction.LEFT)) {
            ribbonSel -= 1;
            if (ribbonSel % 4 == 3) ribbonSel += 4;
        } else if (input.wasPressed(GameAction.RIGHT)) {
            ribbonSel += 1;
            if (ribbonSel % 4 == 0) ribbonSel -= 4;
        } else {
            return false;
        }
        if (ribbonSel < ribbonOffset * 4) ribbonOffset = ribbonSel / 4;
        if (ribbonSel >= (ribbonOffset + 3) * 4) ribbonOffset = ribbonSel / 4 - 2;
        ribbonOffset = Math.max(0, Math.min(ribbonOffset, numRows - 3));
        return false;
    }

    private static void swapRibbons(Pokemon p, int a, int b) {
        if (a < 0 || b < 0 || a >= p.ribbons.size || b >= p.ribbons.size || a == b) {
            return;
        }
        String tmp = p.ribbons.get(a);
        p.ribbons.set(a, p.ribbons.get(b));
        p.ribbons.set(b, tmp);
    }

    // ------------------------------------------------------------------
    // pbMarking.
    // ------------------------------------------------------------------
    private boolean updateMarking(InputManager input, Pokemon p) {
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {
            mode = PAGES;
            return false;
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            if (markingIndex == 6) {
                if (markingValue != p.markings) {
                    p.markings = markingValue;
                    markingDirty = true;
                }
                mode = PAGES;
            } else if (markingIndex == 7) {
                mode = PAGES;
            } else {
                int mask = 1 << markingIndex;
                markingValue = (markingValue & mask) == 0 ? markingValue | mask : markingValue & ~mask;
            }
            return false;
        }
        if (input.wasPressed(GameAction.UP)) {
            if (markingIndex == 7) markingIndex = 6;
            else if (markingIndex == 6) markingIndex = 4;
            else if (markingIndex < 3) markingIndex = 7;
            else markingIndex -= 3;
        } else if (input.wasPressed(GameAction.DOWN)) {
            if (markingIndex == 7) markingIndex = 1;
            else if (markingIndex == 6) markingIndex = 7;
            else if (markingIndex >= 3) markingIndex = 6;
            else markingIndex += 3;
        } else if (input.wasPressed(GameAction.LEFT)) {
            if (markingIndex < 6) {
                markingIndex -= 1;
                if (markingIndex % 3 == 2) markingIndex += 3;
            }
        } else if (input.wasPressed(GameAction.RIGHT)) {
            if (markingIndex < 6) {
                markingIndex += 1;
                if (markingIndex % 3 == 0) markingIndex -= 3;
            }
        }
        return false;
    }

    private Pokemon current() {
        return index >= 0 && index < party.size ? party.get(index) : null;
    }

    // ==================================================================
    // rendering
    // ==================================================================

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        Pokemon p = current();
        if (p == null) {
            return;
        }
        drawBackground(b, a, w, h);
        if (mode == MOVE_DETAIL || mode == FORGET) {
            drawMoveDetail(b, a, f, p, h);
            return;
        }
        String bg = p.egg ? "bg_egg" : "bg_" + page;
        Texture menu = a.graphic("Pictures/Summary", bg);
        if (menu != null) {
            b.draw(menu, 0f, 0f, w, h);
        }
        if (p.egg) {
            drawPageOneEgg(b, a, f, p, h);
            return;
        }
        // Z ORDER (BW PScreen_Summary#pbStartScene): "menuoverlay" - the page
        // graphic drawn just above - is created BEFORE "pokemon", so the
        // standing sprite sits ON TOP of the page GUI; the text "overlay" is
        // created last and stays over both.
        drawPokemon(b, a, p, h);
        drawPageCommon(b, a, f, p, h);
        switch (page) {
            case 1: drawPageOne(b, a, f, p, h); break;
            case 2: drawPageTwo(b, f, p, h); break;
            case 3: drawPageThree(b, a, f, p, h); break;
            case 4: drawPageFour(b, a, f, p, h); break;
            case 5: drawPageFive(b, a, f, p, h); break;
            default: break;
        }
        if (mode == RIBBON_SELECT) {
            drawRibbonDetail(b, a, f, p, h);
        } else if (mode == OPTIONS) {
            drawOptions(b, a, f, skin, p, w, h);
        } else if (mode == MARKING) {
            drawMarking(b, a, f, p, h);
        }
        if (!notice.isEmpty()) {
            f.draw(b, notice, 20f, h - 410f, BASE, SHADOW);
        }
    }

    /** The scrolling Summary/background plane (LargePlane#tileBitmap). */
    private void drawBackground(SpriteBatch b, MenuAssets a, float w, float h) {
        Texture bg = a.graphic("Pictures/Summary", "background");
        if (bg == null) {
            return;
        }
        float tw = bg.getWidth();
        float th = bg.getHeight();
        float px = scrollX % tw;
        if (px < 0) px += tw;
        float py = scrollY % th;
        if (py < 0) py += th;
        for (float x = -px; x < w; x += tw) {
            for (float y = -py; y < h; y += th) {
                b.draw(bg, x, y, tw, th);
            }
        }
    }

    private void drawPokemon(SpriteBatch b, MenuAssets a, Pokemon p, float h) {
        Texture sprite = battler(a, p);
        if (sprite == null) {
            return;
        }
        float size = Math.min(sprite.getWidth(), sprite.getHeight());
        b.draw(sprite, 524f - size / 2f, h - 240f - size / 2f, size, size);
    }

    private void drawPokeIcon(SpriteBatch b, MenuAssets a, Pokemon p, float h) {
        Texture icon = pokemonIcon(a, p);
        if (icon == null) {
            return;
        }
        int size = icon.getHeight();
        // PokemonIconSprite Center origin at (46, 92).
        b.draw(icon, 46f - size / 2f, h - 92f - size / 2f + size * 5f / 8f - size,
                size, size, 0, 0, size, size, false, false);
    }

    // ------------------------------------------------------------------
    // drawPage: the shared header.
    // ------------------------------------------------------------------
    private void drawPageCommon(SpriteBatch b, MenuAssets a, MenuFont f, Pokemon p, float h) {
        Texture ball = a.graphic("Pictures/Summary", String.format("icon_ball_%02d", Math.max(0, p.ballused)));
        if (ball == null) {
            ball = a.graphic("Pictures/Summary", "icon_ball_00");
        }
        if (ball != null) {
            drawImg(b, ball, h, 390 + 64 + 32, 44);
        }
        int status = statusIndex(p);
        Texture statuses = a.graphic("Pictures", "statuses");
        if (status >= 0 && statuses != null) {
            drawImg(b, statuses, h, 476 + 64, 88, 44, 16, 0, status * 16);
        }
        if (p.pokerusStage() == 2) {
            Texture cured = a.graphic("Pictures/Summary", "icon_pokerus");
            if (cured != null) {
                drawImg(b, cured, h, 376, 305);
            }
        }
        if (p.shiny) {
            Texture star = a.graphic("Pictures", "shiny");
            if (star != null) {
                drawImg(b, star, h, 450 + 64, 300 + 64);
            }
        }
        String[] pageNames = {"宝可梦信息", "详细信息", "能力", "招式", "缎带"};
        f.draw(b, pageNames[Math.max(0, Math.min(page - 1, 4))], 26f, h - 8f, WHITE, HEAD_SHADOW);
        f.draw(b, displayName(p), 424f + 64f + 32f, h - 46f, BASE, SHADOW);
        f.draw(b, String.valueOf(p.level), 410f + 64f + 32f, h - 84f, BASE, SHADOW);
        f.draw(b, "携带道具", 366f + 64f + 32f, h - (325f + 64f), BASE, SHADOW);
        f.draw(b, heldItemName(p), 366f + 64f + 32f, h - (354f + 64f), BASE, SHADOW);
        if (p.gender == PokemonStats.MALE) {
            f.draw(b, "\u2642", 550f + 96f, h - 46f, MALE, MALE_SHADOW);
        } else if (p.gender == PokemonStats.FEMALE) {
            f.draw(b, "\u2640", 550f + 96f, h - 46f, FEMALE, FEMALE_SHADOW);
        }
        drawMarkings(b, a, h, 480 + 64 + 32, 306 + 64, p.markings);
        Texture item = itemIcon(a, p.item);
        if (item != null && p.item != null && !p.item.isEmpty()) {
            float size = 48f;
            drawImg(b, item, h, 552f + 96f - size / 2f, 360f + 64f - size / 2f, size, size, 0, 0);
        }
    }

    private void drawMarkings(SpriteBatch b, MenuAssets a, float h, float x, float y, int value) {
        Texture marks = a.graphic("Pictures/Summary", "markings");
        if (marks == null) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            int srcY = (value & (1 << i)) != 0 ? 16 : 0;
            drawImg(b, marks, h, x + i * 16f, y, 16, 16, i * 16, srcY);
        }
    }

    // ------------------------------------------------------------------
    // drawPageOne (page 1).
    // ------------------------------------------------------------------
    private void drawPageOne(SpriteBatch b, MenuAssets a, MenuFont f, Pokemon p, float h) {
        Color dexBase = p.shiny ? DEAD_BASE : BASE;
        Color dexShadow = p.shiny ? DEAD_SHADOW : SHADOW;
        f.draw(b, "图鉴ID", 34f, h - 68f, WHITE, WHITE_SHADOW);
        f.draw(b, "种族名称", 34f, h - 102f, WHITE, WHITE_SHADOW);
        f.draw(b, p.species == null ? "—" : p.species.name, 164f, h - 102f, BASE, SHADOW);
        f.draw(b, formName(p), 134f + 32f, h - 134f, BASE, SHADOW);
        f.draw(b, "形态", 34f, h - 134f, WHITE, WHITE_SHADOW);
        f.draw(b, "属性", 34f, h - 166f, WHITE, WHITE_SHADOW);
        f.draw(b, "训练家", 34f, h - 198f, WHITE, WHITE_SHADOW);
        f.draw(b, "ID No.", 34f, h - 230f, WHITE, WHITE_SHADOW);
        String dex = p.species == null ? "???" : String.format("%03d", p.species.id);
        f.draw(b, dex, 164f, h - 68f, dexBase, dexShadow);
        if (p.originalTrainer == null || p.originalTrainer.isEmpty()) {
            f.draw(b, "租借", 164f, h - 164f, BASE, SHADOW);
            f.draw(b, "?????", 164f, h - 194f, BASE, SHADOW);
        } else {
            Color ownerBase = BASE;
            Color ownerShadow = SHADOW;
            if (p.otGender == 0) {
                ownerBase = MALE;
                ownerShadow = MALE_SHADOW;
            } else if (p.otGender == 1) {
                ownerBase = FEMALE;
                ownerShadow = FEMALE_SHADOW;
            }
            f.draw(b, p.originalTrainer, 164f, h - 198f, ownerBase, ownerShadow);
            f.draw(b, String.format("%05d", p.publicID), 164f, h - 230f, BASE, SHADOW);
        }
        f.draw(b, "亲密度", 34f, h - 262f, WHITE, WHITE_SHADOW);
        f.draw(b, String.format("%3d/%3d", p.happiness, 255), 164f, h - 262f, BASE, SHADOW);
        f.draw(b, "经验值", 34f, h - (228f + 64f), WHITE, WHITE_SHADOW);
        f.drawCentered(b, format(p.exp), 215f, h - (260f + 64f), BASE, SHADOW);
        f.draw(b, "下个等级所需要的经验", 34f, h - (292f + 64f), WHITE, WHITE_SHADOW);
        f.drawCentered(b, format(p.experienceToNextLevel()), 177f, h - (324f + 64f), BASE, SHADOW);
        drawTypes(b, a, p, h, 164f, 232f);
        if (p.level < PBExperience.maxLevel()) {
            Texture exp = a.graphic("Pictures/Summary", "overlay_exp");
            float frac = expFraction(p);
            float ew = Math.round(frac * 128f / 2f) * 2f;
            if (exp != null && ew > 0) {
                drawImg(b, exp, h, 140f, 360f + 64f, ew, 6, 0, 0);
            }
        }
    }

    // ------------------------------------------------------------------
    // drawPageTwo (page 2).
    // ------------------------------------------------------------------
    private void drawPageTwo(SpriteBatch b, MenuFont f, Pokemon p, float h) {
        float line = f.lineHeight();
        float y = 64f;
        if (p.nature != null && p.nature.name != null) {
            f.draw(b, p.nature.name + "性格。", 22f, h - y, MEMO_BLUE, MEMO_BLUE_SHADOW);
            y += line;
        }
        String mapname = p.obtainText != null && !p.obtainText.isEmpty() ? p.obtainText : "遥远的地方";
        f.draw(b, mapname, 22f, h - y, MEMO_BLUE, MEMO_BLUE_SHADOW);
        y += line;
        String met = null;
        switch (p.obtainMode) {
            case 0: met = "等级" + (p.obtainLevel > 0 ? p.obtainLevel : p.level) + "时遇见"; break;
            case 1: met = "获得的宝可梦的蛋。"; break;
            case 2: met = "等级" + (p.obtainLevel > 0 ? p.obtainLevel : p.level) + "时交换获得。"; break;
            case 4: met = "等级" + (p.obtainLevel > 0 ? p.obtainLevel : p.level) + "时有命运般的相遇。"; break;
            default: met = null; break;
        }
        if (met != null) {
            f.draw(b, met, 22f, h - y, MEMO, MEMO_SHADOW);
            y += line;
        }
        String characteristic = characteristic(p);
        if (characteristic != null) {
            f.draw(b, characteristic, 22f, h - y, MEMO, MEMO_SHADOW);
        }
    }

    // ------------------------------------------------------------------
    // drawPageThree (page 3).
    // ------------------------------------------------------------------
    private void drawPageThree(SpriteBatch b, MenuAssets a, MenuFont f, Pokemon p, float h) {
        Color[] statShadows = new Color[] {WHITE_SHADOW, WHITE_SHADOW, WHITE_SHADOW,
                WHITE_SHADOW, WHITE_SHADOW, WHITE_SHADOW};
        if (p.nature != null && !p.nature.neutral()) {
            int up = PokemonStats.statIndexOf(p.nature.statUp);
            int down = PokemonStats.statIndexOf(p.nature.statDown);
            if (up != down) {
                if (up >= 0 && up < 6) statShadows[up] = NAT_UP;
                if (down >= 0 && down < 6) statShadows[down] = NAT_DOWN;
            }
        }
        int[] baseOrder = new int[] {0, 1, 2, 4, 5, 3};
        String[] names = {"HP", "攻击", "防御", "特攻", "特防", "速度"};
        int[] statValue = new int[] {p.hp, p.attack(), p.defense(), p.spAtk(), p.spDef(), p.speed()};
        int[] yRow = {100, 130, 162, 194, 226, 258};
        for (int i = 0; i < 6; i++) {
            int y = yRow[i];
            f.draw(b, names[i], 16f, h - y, WHITE, statShadows[i]);
            f.drawCentered(b, String.valueOf(statValue[i]), 268f + 88f, h - y, BASE, SHADOW);
            f.drawCentered(b, String.valueOf(p.evs[baseOrder[i]]), 194f + 84f, h - y, BASE, SHADOW);
            f.drawCentered(b, String.valueOf(p.ivs[ivOrder(i)]), 152f + 58f, h - y, BASE, SHADOW);
            f.drawCentered(b, String.valueOf(p.baseStat(baseOrder[i])), 152f + 10f, h - y, BASE, SHADOW);
        }
        for (int i = 0; i < 6; i++) {
            Texture rating = a.graphic("Pictures/Summary", ratingFile(p.ivs[ivOrder(i)]));
            if (rating != null) {
                drawImg(b, rating, h, 110f + 128f, yRow[i], 14, 20, 0, 0);
            }
        }
        int sum = 0;
        for (int i = 0; i < 6; i++) {
            sum += p.baseStat(i);
        }
        f.draw(b, "总和", 16f, h - 290f, WHITE, WHITE_SHADOW);
        f.drawCentered(b, String.valueOf(sum), 152f + 10f, h - 290f, WHITE, WHITE_SHADOW);
        f.draw(b, "特性", 12f, h - (294f + 32f), WHITE, WHITE_SHADOW);
        f.drawCentered(b, abilityName(p), 240f, h - (294f + 32f), BASE, SHADOW);
        String desc = abilityDescription(p);
        if (desc != null) {
            drawWrapped(b, f, desc, 12f, 320f + 32f, 282f, 3, h, BASE, SHADOW);
        }
        Texture hpBar = a.graphic("Pictures/Summary", "overlay_hp");
        if (hpBar != null && p.maxHp() > 0 && p.hp > 0) {
            float hpw = p.hp * 96f / p.maxHp();
            if (hpw < 1f) hpw = 1f;
            hpw = Math.round(hpw / 2f) * 2f;
            int zone = 0;
            if (p.hp <= p.maxHp() / 2) zone = 1;
            if (p.hp <= p.maxHp() / 4) zone = 2;
            drawImg(b, hpBar, h, 32f, 80f, hpw, 6, 0, zone * 8);
        }
    }

    // ------------------------------------------------------------------
    // drawPageFour (page 4).
    // ------------------------------------------------------------------
    private void drawPageFour(SpriteBatch b, MenuAssets a, MenuFont f, Pokemon p, float h) {
        Texture types = a.graphic("Pictures", "types");
        float yPos = 82f;
        for (int i = 0; i < p.moves.size; i++) {
            Pokemon.MoveSlot slot = p.moves.get(i);
            PbsData.Move move = slot.move;
            if (move != null) {
                PbsData.TypeInfo info = typeInfo(move.type);
                if (types != null && info != null) {
                    drawImg(b, types, h, 32f, yPos + 2f, 64, 28, 0, info.id * 28);
                }
                f.draw(b, move.name, 100f, h - yPos, WHITE, MOVE_SHADOW);
                if (slot.maxPp > 0) {
                    f.draw(b, "PP", 126f, h - (yPos + 32f), WHITE, MOVE_SHADOW);
                    int fraction = ppFraction(slot);
                    f.drawRight(b, slot.pp + "/" + slot.maxPp, 244f, h - (yPos + 32f),
                            ppBase(fraction), ppShadow(fraction));
                }
            } else {
                f.draw(b, "-", 100f, h - yPos, WHITE, MOVE_SHADOW);
                f.drawRight(b, "--", 226f, h - (yPos + 32f), WHITE, MOVE_SHADOW);
            }
            yPos += 64f;
        }
    }

    // ------------------------------------------------------------------
    // drawPageFive (page 5).
    // ------------------------------------------------------------------
    private void drawPageFive(SpriteBatch b, MenuAssets a, MenuFont f, Pokemon p, float h) {
        f.draw(b, "缎带数量", 38f, h - 303f, WHITE, WHITE_SHADOW);
        f.drawRight(b, String.valueOf(p.ribbons.size), 157f, h - 334f, BASE, SHADOW);
        Texture ribbons = a.graphic("Pictures", "ribbons");
        if (ribbons == null) {
            return;
        }
        int coord = 0;
        for (int i = ribbonOffset * 4; i < p.ribbons.size && coord < 12; i++) {
            Integer id = ribbonId(p.ribbons.get(i));
            if (id == null) {
                coord++;
                continue;
            }
            int ribn = id - 1;
            drawImg(b, ribbons, h, 2f + 68f * (coord % 4), 74f + 68f * (coord / 4),
                    64, 64, 64 * (ribn % 8), 64 * (ribn / 8));
            coord++;
        }
    }

    // ------------------------------------------------------------------
    // drawSelectedMove / drawMoveSelection (page 4 Z).
    // ------------------------------------------------------------------
    private void drawMoveDetail(SpriteBatch b, MenuAssets a, MenuFont f, Pokemon p, float h) {
        boolean learn = mode == FORGET && moveToLearn != null;
        Texture menu = a.graphic("Pictures/Summary", learn ? "bg_learnmove" : "bg_movedetail");   // drawMoveSelection:1019-1023
        if (menu != null) {
            b.draw(menu, 0f, 0f, ScreenMetrics.logicalWidth(), h);
        }
        Texture types = a.graphic("Pictures", "types");
        Texture category = a.graphic("Pictures", "category");
        f.draw(b, "招式", 26f, h - 8f, WHITE, MOVE_SHADOW);
        f.draw(b, "威力", 20f, h - 122f, WHITE, MOVE_SHADOW);
        f.draw(b, "命中", 20f, h - 154f, WHITE, MOVE_SHADOW);
        f.draw(b, "标签", 20f, h - 186f, WHITE, MOVE_SHADOW);
        float yPos = learn ? 98f - 76f : 98f;                                    // :1042-1043
        for (int i = 0; i < (learn ? 5 : 4); i++) {
            Pokemon.MoveSlot slot = i < p.moves.size ? p.moves.get(i) : null;
            if (i == 4) {
                slot = new Pokemon.MoveSlot(moveToLearn);                         // :1046-1048 the move to learn
                yPos += 20f;
            }
            PbsData.Move move = slot == null ? null : slot.move;
            if (move != null) {
                PbsData.TypeInfo info = typeInfo(move.type);
                if (types != null && info != null) {
                    drawImg(b, types, h, 310f, yPos + 2f, 64, 28, 0, info.id * 28);
                }
                f.draw(b, move.name, 380f, h - yPos, WHITE, MOVE_SHADOW);
                if (slot.maxPp > 0) {
                    f.draw(b, "PP", 380f, h - (yPos + 32f), WHITE, MOVE_SHADOW);
                    int fraction = ppFraction(slot);
                    f.drawRight(b, slot.pp + "/" + slot.maxPp, 492f, h - (yPos + 32f),
                            ppBase(fraction), ppShadow(fraction));
                }
                if (category != null) {
                    int cat = categoryIndex(move.category);
                    drawImg(b, category, h, 310f, yPos + 32f, 64, 28, 0, cat * 28);
                }
            } else {
                f.draw(b, "-", 328f, h - yPos, WHITE, MOVE_SHADOW);
                f.drawRight(b, "--", 454f, h - (yPos + 32f), WHITE, MOVE_SHADOW);
            }
            yPos += 64f;
        }
        // Pokemon type icon(s) at (130,78) or (96,78)/(166,78).
        Array<String> list = p.types();
        if (types != null && list.size > 0) {
            PbsData.TypeInfo t1 = typeInfo(list.get(0));
            if (list.size == 1 || list.get(0).equals(list.get(1))) {
                if (t1 != null) drawImg(b, types, h, 130f, 78f, 64, 28, 0, t1.id * 28);
            } else {
                PbsData.TypeInfo t2 = typeInfo(list.get(1));
                if (t1 != null) drawImg(b, types, h, 96f, 78f, 64, 28, 0, t1.id * 28);
                if (t2 != null) drawImg(b, types, h, 166f, 78f, 64, 28, 0, t2.id * 28);
            }
        }
        drawSelectedMove(b, a, f, p, h);
        // Movesel cursor (286, 91+64*index) + pokeicon.
        drawPokeIcon(b, a, p, h);
        Texture cursor = a.graphic("Pictures/Summary", "cursor_move");
        if (cursor != null && p.moves.size > 0) {
            int frameH = cursor.getHeight() / 2;
            int srcY = mode != FORGET && moveIndex == movePresel ? frameH : 0;
            float cursorY = 91f + 64f * moveIndex;                                // MoveSelectionSprite#refresh (:69-83)
            if (learn) {
                cursorY -= 76f;
                if (moveIndex == 4) cursorY += 20f;
            }
            drawImg(b, cursor, h, 286f, cursorY, 272, frameH, 0, srcY);
        }
    }

    private void drawSelectedMove(SpriteBatch b, MenuAssets a, MenuFont f, Pokemon p, float h) {
        Pokemon.MoveSlot slot = moveIndex < p.moves.size ? p.moves.get(moveIndex) : null;
        PbsData.Move move = mode == FORGET && moveIndex == 4 ? moveToLearn : slot == null ? null : slot.move;
        int power = move == null ? 0 : move.power;
        int accuracy = move == null ? 0 : move.accuracy;
        String powerText = power == 0 ? "---" : power == 1 ? "???" : String.valueOf(power);
        f.drawCentered(b, powerText, 216f, h - 122f, BASE, SHADOW);
        f.drawCentered(b, accuracy == 0 ? "---" : accuracy + "%", 216f, h - 155f, BASE, SHADOW);
        // Move flag icons at (120,188), step 26 (b/e are inverted).
        String moveFlags = move == null || move.flags == null ? "" : move.flags;
        String letters = "abcdefghijklmnopqr";
        float flagsX = 120f;
        for (int i = 0; i < letters.length(); i++) {
            char flag = letters.charAt(i);
            boolean present = moveFlags.indexOf(flag) >= 0;
            if (flag == 'b' || flag == 'e') {
                present = !present;
            }
            if (!present) {
                continue;
            }
            Texture icon = a.graphic("Pictures/Move Flags", String.valueOf(flag));
            if (icon != null) {
                drawImg(b, icon, h, flagsX, 188f, 26, 28, 0, 0);
            }
            flagsX += 26f;
        }
        String desc = move == null ? "" : move.description;
        if (desc != null) {
            drawWrapped(b, f, desc, 4f, 220f, 230f, 5, h, BASE, SHADOW);
        }
    }

    // ------------------------------------------------------------------
    // drawSelectedRibbon (page 5 Z).
    // ------------------------------------------------------------------
    private void drawRibbonDetail(SpriteBatch b, MenuAssets a, MenuFont f, Pokemon p, float h) {
        Integer id = ribbonSel < p.ribbons.size ? ribbonId(p.ribbons.get(ribbonSel)) : null;
        String name = id != null ? Ribbons.name(id) : "";
        String desc = id != null ? Ribbons.description(id) : "";
        Texture box = a.graphic("Pictures/Summary", "overlay_ribbon");
        if (box != null) {
            drawImg(b, box, h, 0f, 280f);
        }
        f.draw(b, name, 30f, h - 286f, RIBBON_NAME, RIBBON_NAME_SHADOW);
        drawWrapped(b, f, desc, 30f, 318f, 480f, 3, h, BASE, SHADOW);
        // Up/down arrows.
        Texture up = a.graphic("Pictures", "uparrow");
        Texture down = a.graphic("Pictures", "downarrow");
        int numRows = Math.max((p.ribbons.size + 3) / 4, 3);
        if (up != null && ribbonOffset > 0) {
            b.draw(up, 260f, h - 56f - 40f, 28f, 40f, 0, 0, 28, 40, false, false);
        }
        if (down != null && ribbonOffset < numRows - 3) {
            b.draw(down, 260f, h - 260f - 40f, 28f, 40f, 0, 0, 28, 40, false, false);
        }
        // Ribbon cursor at (0+(index%4)*68, 72+((index/4))*68).
        Texture cursor = a.graphic("Pictures/Summary", "cursor_ribbon");
        if (cursor != null) {
            int local = ribbonSel - ribbonOffset * 4;
            if (local >= 0 && local < 12) {
                int frameH = cursor.getHeight() / 2;
                int srcY = ribbonSel == ribbonPresel ? frameH : 0;
                drawImg(b, cursor, h, 0f + 68f * (local % 4), 72f + 68f * (local / 4), 68, frameH, 0, srcY);
            }
        }
    }

    // ------------------------------------------------------------------
    // pbShowCommands window (pages 1..3 Z).
    // ------------------------------------------------------------------
    private void drawOptions(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin,
                             Pokemon p, float w, float h) {
        List<String> entries = optionEntries(p);
        float rowH = 32f;
        float winH = entries.size() * rowH + 32f;
        float winW = 160f;
        for (String s : entries) {
            winW = Math.max(winW, f.width(s) + 64f);
        }
        float wx = w - winW;      // pbBottomRight: x = width - window.width
        float wy = 0f;            //              y = height - window.height (libGDX bottom)
        MenuPanel.window(b, a, skin, wx, wy, winW, winH);
        Color[] text = MenuPanel.textColors(skin);
        for (int i = 0; i < entries.size(); i++) {
            MenuPanel.cursor(b, a, skin, wx, wy, winH, i, optionIndex, rowH);
            float ty = MenuPanel.rowY(wy, winH, i, rowH, f.lineHeight());
            f.draw(b, entries.get(i), MenuPanel.rowX(wx), ty, text[0], text[1]);
        }
    }

    // ------------------------------------------------------------------
    // pbMarking.
    // ------------------------------------------------------------------
    private void drawMarking(SpriteBatch b, MenuAssets a, MenuFont f, Pokemon p, float h) {
        Texture bg = a.graphic("Pictures/Summary", "overlay_marking");
        if (bg != null) {
            drawImg(b, bg, h, 260f, 88f);
        }
        Texture marks = a.graphic("Pictures/Summary", "markings");
        if (marks != null) {
            for (int i = 0; i < 6; i++) {
                int srcY = (markingValue & (1 << i)) != 0 ? 16 : 0;
                drawImg(b, marks, h, 300f + 58f * (i % 3), 154f + 50f * (i / 3), 16, 16, i * 16, srcY);
            }
        }
        f.drawRight(b, "标记" + displayName(p), 366f, h - 96f, RIBBON_NAME, RIBBON_NAME_SHADOW);
        f.drawRight(b, "OK", 366f, h - 248f, RIBBON_NAME, RIBBON_NAME_SHADOW);
        f.drawRight(b, "取消", 366f, h - 298f, RIBBON_NAME, RIBBON_NAME_SHADOW);
        Texture cursor = a.graphic("Pictures/Summary", "cursor_marking");
        if (cursor != null) {
            int frameH = cursor.getHeight() / 2;
            int srcY = 0;
            float cx;
            float cy;
            if (markingIndex == 6) {
                cx = 284f;
                cy = 244f;
                srcY = frameH;
            } else if (markingIndex == 7) {
                cx = 284f;
                cy = 294f;
                srcY = frameH;
            } else {
                cx = 284f + 58f * (markingIndex % 3);
                cy = 144f + 50f * (markingIndex / 3);
            }
            drawImg(b, cursor, h, cx, cy, 162, frameH, 0, srcY);
        }
    }

    // ------------------------------------------------------------------
    // drawPageOneEgg.
    // ------------------------------------------------------------------
    private void drawPageOneEgg(SpriteBatch b, MenuAssets a, MenuFont f, Pokemon p, float h) {
        Texture ball = a.graphic("Pictures/Summary", String.format("icon_ball_%02d", Math.max(0, p.ballused)));
        if (ball == null) {
            ball = a.graphic("Pictures/Summary", "icon_ball_00");
        }
        if (ball != null) {
            drawImg(b, ball, h, 390 + 128, 44);
        }
        f.draw(b, "面板", 26f, h - 8f, WHITE, HEAD_SHADOW);
        f.draw(b, displayName(p), 424f, h - 46f, BASE, SHADOW);
        f.draw(b, "道具", 366f, h - 322f, BASE, SHADOW);
        String item = heldItemName(p);
        if ("无".equals(item)) {
            f.draw(b, item, 360f, h - 350f, BASE, SHADOW);
        } else {
            f.draw(b, item, 290f, h - 350f, BASE, SHADOW);
        }
        float line = f.lineHeight();
        float y = 82f;
        f.draw(b, "\"状态\"", 10f, h - y, MEMO, MEMO_SHADOW);
        y += line;
        f.draw(b, eggState(p), 10f, h - y, MEMO, MEMO_SHADOW);
        drawMarkings(b, a, h, 480 + 64 + 32, 306 + 64, p.markings);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static Color ppBase(int fraction) {
        switch (fraction) {
            case 1: return new Color(1f, 214f / 255f, 0f, 1f);
            case 2: return new Color(1f, 115f / 255f, 0f, 1f);
            case 3: return new Color(1f, 8f / 255f, 72f / 255f, 1f);
            default: return WHITE;
        }
    }

    private static Color ppShadow(int fraction) {
        switch (fraction) {
            case 1: return new Color(123f / 255f, 99f / 255f, 0f, 1f);
            case 2: return new Color(115f / 255f, 57f / 255f, 0f, 1f);
            case 3: return new Color(123f / 255f, 8f / 255f, 49f / 255f, 1f);
            default: return MOVE_SHADOW;
        }
    }

    private static int categoryIndex(String category) {
        if (category == null) return 2;
        switch (category) {
            case "Physical": return 0;
            case "Special": return 1;
            default: return 2;
        }
    }

    private static Integer ribbonId(String ribbon) {
        int id = Ribbons.idOf(ribbon);
        return id > 0 ? id : null;
    }

    private static String displayName(Pokemon p) {
        return p.egg ? "神秘的蛋" : (p.name == null ? "—" : p.name);
    }

    private static String eggState(Pokemon p) {
        if (p.stepsToHatch < 1275) return "可以听到从里面传出的声音！似乎要孵化了！";
        if (p.stepsToHatch < 2550) return "偶尔会摇动，应该是快要孵化了。";
        if (p.stepsToHatch < 10200) return "会孵化出什么宝可梦呢？似乎还需要一些时间。";
        return "看起来，这个蛋需要很长时间才能孵化。";
    }

    private void drawTypes(SpriteBatch b, MenuAssets a, Pokemon p, float h, float x1, float x2) {
        Texture types = a.graphic("Pictures", "types");
        if (types == null) {
            return;
        }
        Array<String> list = p.types();
        if (list.size > 0) {
            PbsData.TypeInfo info = typeInfo(list.get(0));
            if (info != null) {
                drawImg(b, types, h, x1, 164f, 64, 28, 0, info.id * 28);
            }
        }
        if (list.size > 1 && !list.get(0).equals(list.get(1))) {
            PbsData.TypeInfo info = typeInfo(list.get(1));
            if (info != null) {
                drawImg(b, types, h, x2, 164f, 64, 28, 0, info.id * 28);
            }
        }
    }

    private PbsData.TypeInfo typeInfo(String type) {
        PbsData data = context.pbsData();
        return data == null ? null : data.type(type);
    }

    private static int ivOrder(int i) {
        switch (i) {
            case 0: return 0;
            case 1: return 1;
            case 2: return 2;
            case 3: return 4;
            case 4: return 5;
            default: return 3;
        }
    }

    private static int ppFraction(Pokemon.MoveSlot slot) {
        if (slot.pp == 0) return 3;
        if (slot.pp * 4 <= slot.maxPp) return 2;
        if (slot.pp * 2 <= slot.maxPp) return 1;
        return 0;
    }

    private float expFraction(Pokemon p) {
        String growth = p.growthRate();
        int cur = PokemonStats.experienceForLevel(growth, p.level);
        int next = PokemonStats.experienceForLevel(growth, p.level + 1);
        if (next <= cur) {
            return 1f;
        }
        float frac = (p.exp - cur) / (float) (next - cur);
        return Math.max(0f, Math.min(1f, frac));
    }

    private String abilityName(Pokemon p) {
        PbsData.Ability ability = context.pbsData() == null ? null : context.pbsData().ability(p.ability);
        if (ability != null && ability.name != null) {
            return ability.name;
        }
        return p.ability == null || p.ability.isEmpty() ? "—" : p.ability;
    }

    private String abilityDescription(Pokemon p) {
        PbsData.Ability ability = context.pbsData() == null ? null : context.pbsData().ability(p.ability);
        return ability == null ? null : ability.description;
    }

    private String heldItemName(Pokemon p) {
        if (p.item == null || p.item.isEmpty()) {
            return "无";
        }
        PbsData.Item item = context.pbsData() == null ? null : context.pbsData().item(p.item);
        return item != null && item.name != null ? item.name : "无";
    }

    private String formName(Pokemon p) {
        if (p.form != null && p.form.formName != null && !p.form.formName.isEmpty()) {
            return p.form.formName;
        }
        if (p.species != null && p.species.formName != null && !p.species.formName.isEmpty()) {
            return p.species.formName;
        }
        return "默认形态";
    }

    private static String characteristic(Pokemon p) {
        int best = 0;
        int tiebreaker = Math.abs(p.personalID) % 6;
        for (int i = 0; i < 6; i++) {
            if (p.ivs[i] == p.ivs[best]) {
                if (i >= tiebreaker && best < tiebreaker) best = i;
            } else if (p.ivs[i] > p.ivs[best]) {
                best = i;
            }
        }
        int idx = best * 5 + p.ivs[best] % 5;
        return idx >= 0 && idx < CHARACTERISTICS.length ? CHARACTERISTICS[idx] : null;
    }

    private static final String[] CHARACTERISTICS = {
            "非常喜欢吃东西。", "经常睡午觉。", "常常打睡意。", "经常乱扔东西。", "喜欢悠然自在。",
            "以力气大为傲。", "喜欢胡闹。", "有点容易生气。", "喜欢打架。", "血气方刚。",
            "身体强壮。", "抗打能力强。", "顽强不屈。", "能吃苦耐劳。", "善于忍耐。",
            "喜欢比谁跑得快。", "对声音敏感。", "冒冒失失。", "有点容易得意忘形。", "逃得快。",
            "好奇心强。", "喜欢恶作剧。", "做事万无一失。", "经常思考。", "一丝不苟。",
            "性格强势。", "有一点点爱慕虚荣。", "争强好胜。", "不服输。", "有一点点固执。"
    };

    private static String ratingFile(int iv) {
        if (iv > 30) return "RatingS";
        if (iv > 22) return "RatingA";
        if (iv > 15) return "RatingB";
        if (iv > 7) return "RatingC";
        if (iv > 0) return "RatingD";
        return "RatingF";
    }

    private static int statusIndex(Pokemon p) {
        if (p.hp <= 0) return 7;
        if (p.status == null || p.status.isEmpty()) return -1;
        switch (p.status) {
            case "SLEEP": return 0;
            case "POISON": return 1;
            case "BURN": return 2;
            case "PARALYSIS": return 3;
            case "FROZEN": return 4;
            default: return -1;
        }
    }

    private Texture battler(MenuAssets a, Pokemon p) {
        if (p.species == null) return null;
        // The plugin's setPokemonBitmap uses the shiny / form file when it exists.
        String base = String.format("%03d", p.species.id) + (p.shiny ? "s" : "");
        int form = p.form == null ? 0 : p.form.form;
        Texture sprite = a.graphic("Battlers", form > 0 ? base + "_" + form : base);
        if (sprite == null) sprite = a.graphic("Battlers", base);
        if (sprite == null) sprite = a.graphic("Battlers", String.format("%03d", p.species.id));
        if (sprite == null) sprite = a.graphic("Battlers", p.species.internalName);
        return sprite;
    }

    private Texture pokemonIcon(MenuAssets a, Pokemon p) {
        if (p.species == null) return null;
        String suffix = (p.shiny ? "s" : "") + (p.egg ? "egg" : "");
        Texture icon = a.icon(String.format("icon%03d%s", p.species.id, suffix));
        if (icon == null) icon = a.icon("icon" + p.species.internalName + suffix);
        if (icon == null && p.egg) icon = a.icon("iconEgg");
        return icon;
    }

    private Texture itemIcon(MenuAssets a, String id) {
        return ItemIcons.of(a, context.pbsData(), id);
    }

    private static void drawImg(SpriteBatch b, Texture t, float h, float x, float topY) {
        b.draw(t, x, h - topY - t.getHeight(), t.getWidth(), t.getHeight());
    }

    private static void drawImg(SpriteBatch b, Texture t, float h, float x, float topY,
                                float w, float hh, int sx, int sy) {
        b.draw(t, x, h - topY - hh, w, hh, sx, sy, (int) w, (int) hh, false, false);
    }

    private void drawWrapped(SpriteBatch b, MenuFont f, String text, float x, float topY,
                             float width, int maxLines, float h, Color main, Color shadow) {
        if (text == null) {
            return;
        }
        String cleaned = text.replace("\r", "").replace("\n", " ");
        StringBuilder line = new StringBuilder();
        float lineHeight = f.lineHeight();
        int drawn = 0;
        for (int i = 0; i < cleaned.length() && drawn < maxLines; i++) {
            char c = cleaned.charAt(i);
            if (f.width(line.toString() + c) > width && line.length() > 0) {
                f.draw(b, line.toString(), x, h - (topY + drawn * lineHeight), main, shadow);
                drawn++;
                line.setLength(0);
            }
            line.append(c);
        }
        if (drawn < maxLines && line.length() > 0) {
            f.draw(b, line.toString(), x, h - (topY + drawn * lineHeight), main, shadow);
        }
    }

    private static String format(int value) {
        return String.format("%,d", value);
    }
}
