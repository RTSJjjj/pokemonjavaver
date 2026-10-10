package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.UiSounds;
import pokemon.runtime.field.ItemHandlers;
import pokemon.runtime.field.ItemTask;
import pokemon.runtime.field.TaskItemScene;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.MoveRelearner;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.ui.WindowSkin;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 228_PScreen_MoveRelearner: {@code pbRelearnMoveScreen(pokemon)} - {@code MoveRelearner_Scene} (the list of the moves on
 * {@code reminderbg}) and the loop of {@code MoveRelearnerScreen#pbStartScreen}, both inside {@code pbFadeOutIn}.
 *
 * <p>Teaching a move is the plugin's global {@code pbLearnMove(pokemon, move)} ({@link ItemHandlers#pbLearnMove}), run as a
 * blocking handler: its messages are the shared {@link PbMessage}, its forget screen is {@link SummaryView}.</p>
 *
 * <p>登记: {@code UIHelper.pbConfirm}'s yes/no window is the shared {@link PbMessage}'s command window; the pokemon icon is
 * drawn still (no icon animation); the {@code commands} window (invisible, it only keeps the index) is {@link MenuListModel}.</p>
 */
public final class RelearnerView {
    private enum Phase { OUTER_OUT, SPRITES_IN, CHOOSE, BUSY, SPRITES_OUT, OUTER_IN, DONE }

    private static final int VISIBLEMOVES = 4;                                  // :48
    private static final Color WHITE = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color SHADOW = new Color(88f / 255f, 88f / 255f, 80f / 255f, 1f);
    private static final Color BLACK = new Color(0f, 0f, 0f, 1f);
    private static final Color VALUE = new Color(64f / 255f, 64f / 255f, 64f / 255f, 1f);
    private static final Color VALUE_SHADOW = new Color(176f / 255f, 176f / 255f, 176f / 255f, 1f);

    private final RuntimeContext context;
    private final Pokemon pokemon;
    private final List<String> moves;
    private final MenuListModel cursor = new MenuListModel(VISIBLEMOVES);
    private final PbMessage pbMessage;
    private final MenuClock clock = new MenuClock();
    private final ItemHandlers handlers;
    private Phase phase = Phase.OUTER_OUT;
    private int tick;
    private float blackAlpha;
    private boolean finished;
    private boolean learned;
    private ItemTask task;
    private SummaryView forget;

    public RelearnerView(RuntimeContext context, Pokemon pokemon) {
        this.context = context;
        this.pokemon = pokemon;
        this.moves = MoveRelearner.pbGetRelearnableMoves(pokemon, context.pbsData());   // :197 pbStartScreen
        this.pbMessage = new PbMessage(context);
        this.handlers = new ItemHandlers(context.pbsData(), context.gameState(), java.time.LocalTime::now);
        cursor.size(moves.size());
        if (context.audioManager() != null) {
            context.audioManager().memorizeBgmAndBgs();                         // pbFadeOutIn keeps the music
        }
    }

    /** True while the screen below (the host's) is what the player sees: the two outer black fades. */
    public boolean hostVisible() {
        return phase == Phase.OUTER_OUT || phase == Phase.OUTER_IN || phase == Phase.DONE;
    }

    /** {@code pbRelearnMoveScreen}'s return value: true when a move was taught. */
    public boolean learned() {
        return learned;
    }

    private PbsData.Move move(String id) {
        return context.pbsData() == null ? null : context.pbsData().move(id);
    }

    private String moveName(String id) {
        PbsData.Move data = move(id);
        return data == null || data.name == null ? id : data.name;
    }

    private void say(String text, Runnable then) {
        pbMessage.start(text, null, 0, 0, ignored -> then.run());
    }

    private void confirm(String text, java.util.function.Consumer<Boolean> then) {
        pbMessage.start(text, Arrays.asList("是", "否"), 2, 0, index -> then.accept(index == 0));   // UIHelper.pbConfirm
    }

    // =====================================================================
    // Update
    // =====================================================================

    /** @return true when the screen is over and the host may go on */
    public boolean update(InputManager input) {
        if (forget != null) {
            if (forget.update(input)) {
                int result = forget.forgetResult();
                forget = null;
                answerTask(result);
            }
            return finished;
        }
        int ticks = clock.advance();
        if (pbMessage.active()) {
            pbMessage.update(input, ticks);
            return finished;
        }
        if (phase == Phase.CHOOSE) {
            chooseMove(input);
        }
        for (int i = 0; i < ticks && !finished; i++) {
            step();
        }
        return finished;
    }

    private void step() {
        switch (phase) {
            case OUTER_OUT:                                                      // pbFadeOutIn: j in 0..16
                blackAlpha = Math.min(255f, tick * 16f);
                if (++tick > 16) {
                    phase = Phase.SPRITES_IN;                                    // pbStartScene: pbFadeInAndShow
                    tick = 0;
                }
                break;
            case SPRITES_IN:
                blackAlpha = Math.max(0f, Math.min(255f, (16 - tick) * 16f));
                if (++tick > 16) {
                    phase = Phase.CHOOSE;
                    blackAlpha = 0f;
                }
                break;
            case SPRITES_OUT:                                                    // pbEndScene: pbFadeOutAndHide
                blackAlpha = Math.max(0f, Math.min(255f, tick * 16f));
                if (++tick > 16) {
                    phase = Phase.OUTER_IN;
                    tick = 0;
                }
                break;
            case OUTER_IN:
                blackAlpha = Math.max(0f, Math.min(255f, (16 - tick) * 16f));
                if (++tick > 16) {
                    finish();
                }
                break;
            default:
                break;
        }
    }

    /** {@code pbChooseMove} (:157-178): the list moves with up/down; B answers 0, C the move. */
    private void chooseMove(InputManager input) {
        int before = cursor.index();
        if (input.wasRepeated(GameAction.UP)) cursor.move(-1);
        if (input.wasRepeated(GameAction.DOWN)) cursor.move(1);
        if (cursor.index() != before) UiSounds.cursor(context.audioManager());   // Window_CommandPokemon: pbPlayCursorSE
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {   // :172
            giveUp();
        } else if (input.wasPressed(GameAction.CONFIRM)) {                       // :174
            teach(moves.get(cursor.index()));
        }
    }

    /** :201-206 move<=0. */
    private void giveUp() {
        phase = Phase.BUSY;
        confirm("放弃" + pokemon.name + "的新招式？", yes -> {                         // :203
            if (yes) {
                endScene(false);                                                 // :204-205 pbEndScene; return false
            } else {
                phase = Phase.CHOOSE;
            }
        });
    }

    /** :207-215. */
    private void teach(String move) {
        phase = Phase.BUSY;
        confirm("教学" + moveName(move) + "？", yes -> {                             // :208
            if (!yes) {
                phase = Phase.CHOOSE;
                return;
            }
            PbsData.Move data = move(move);
            task = ItemTask.start(scene -> learnedResult = handlers.pbLearnMove(pokemon, data, false, false, scene));   // :209
            learnedResult = false;
            pumpTask();
        });
    }

    private boolean learnedResult;

    /** Runs the learn handler up to its next scene call and starts that call. */
    private void pumpTask() {
        while (task != null) {
            if (task.resume()) {                                                 // pbLearnMove returned
                task = null;
                if (learnedResult) {
                    endScene(true);                                              // :210-211 pbEndScene; return true
                } else {
                    phase = Phase.CHOOSE;                                        // the loop asks again
                }
                return;
            }
            TaskItemScene.Request r = task.pending();
            switch (r.kind) {
                case MESSAGE:
                case DISPLAY:
                    say(r.text, () -> answerTask(null));
                    return;
                case SE:
                    if (!r.text.isEmpty() && context.audioManager() != null) {
                        context.audioManager().playSe(r.text, 100, 100);
                    }
                    task.answer(null);
                    break;
                case FORGET_MOVE:                                                // 188:823-830 pbForgetMove
                    forget = SummaryView.forForget(context, r.pokemon, r.move);
                    return;
                default:
                    task.answer(null);
                    break;
            }
        }
    }

    private void answerTask(Object value) {
        if (task != null) {
            task.answer(value);
            pumpTask();
        }
    }

    private void endScene(boolean result) {
        learned = result;
        phase = Phase.SPRITES_OUT;
        tick = 0;
    }

    private void finish() {
        if (context.audioManager() != null) {
            context.audioManager().restoreBgmAndBgs();
        }
        phase = Phase.DONE;
        finished = true;
    }

    // =====================================================================
    // Render
    // =====================================================================

    /** MessageConfig::pbGetSpeechFrame / $SpeechFrames[$PokemonSystem.textskin]. */
    private WindowSkin speechFrame(MenuAssets a, WindowSkin fallback) {
        int index = context.settings().textskin;
        String name = index >= 0 && index < GameSettings.SPEECH_FRAMES.length ? GameSettings.SPEECH_FRAMES[index] : "speech bw 1";
        WindowSkin speech = a.skin(name);
        return speech != null ? speech : fallback;
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        if (forget != null) {
            forget.render(b, a, f, skin);
            return;
        }
        if (!hostVisible()) {
            drawScene(b, a, f, w, h);
            if (pbMessage.active()) pbMessage.render(b, a, f, skin, speechFrame(a, skin), w, h);
        }
        if (blackAlpha > 0f) {
            b.setColor(0f, 0f, 0f, Math.min(1f, blackAlpha / 255f));
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(Color.WHITE);
        }
    }

    private static void drawImg(SpriteBatch b, Texture t, float h, float x, float topY, float width, float height, int sx, int sy) {
        b.draw(t, x, h - topY - height, width, height, sx, sy, (int) width, (int) height, false, false);
    }

    private Texture icon(MenuAssets a) {
        if (pokemon.species == null) return null;
        Texture icon = PokemonIcons.of(a, pokemon);
        return icon;
    }

    private int typeId(String type) {
        PbsData.TypeInfo info = context.pbsData() == null ? null : context.pbsData().type(type);
        return info == null ? -1 : info.id;
    }

    /** {@code pbDrawMoveList} (:88-155) over the background planes of {@code pbStartScene} (:55-86). */
    private void drawScene(SpriteBatch b, MenuAssets a, MenuFont f, float w, float h) {
        Texture bg = a.graphic("Pictures", "reminderbg");                         // :61 addBackgroundPlane
        if (bg != null) b.draw(bg, 0f, 0f, w, h);
        Texture icon = icon(a);                                                   // :62-65 centred at (320,84)
        if (icon != null) {
            int size = icon.getHeight();
            b.draw(icon, 320f - size / 2f, h - 84f - size / 2f, size, size, 0, 0, size, size, false, false);
        }
        Texture sel = a.graphic("Pictures", "reminderSel");
        int row = cursor.index() - cursor.first();
        if (sel != null && moves.size() > 0) {
            drawImg(b, sel, h, 0f, 78f + row * 64f, 258f, 72f, 0, 72);           // :66-69 the background sprite, src (0,72,258,72)
        }
        Texture types = a.graphic("Pictures", "types");
        com.badlogic.gdx.utils.Array<String> own = pokemon.types();                                    // :92-98 type1 / type2
        if (types != null && own.size > 0) {
            int t1 = typeId(own.get(0));
            if (own.size == 1 || own.get(0).equals(own.get(1))) {
                if (t1 >= 0) drawImg(b, types, h, 400f, 70f, 64f, 28f, 0, t1 * 28);
            } else {
                int t2 = typeId(own.get(1));
                if (t1 >= 0) drawImg(b, types, h, 366f, 70f, 64f, 28f, 0, t1 * 28);
                if (t2 >= 0) drawImg(b, types, h, 436f, 70f, 64f, 28f, 0, t2 * 28);
            }
        }
        f.draw(b, "教什么招式？", 16f, h - 8f, WHITE, SHADOW);                       // :100-101
        float yPos = 82f;                                                         // :104
        for (int i = 0; i < VISIBLEMOVES; i++) {
            int at = cursor.first() + i;
            PbsData.Move data = at < moves.size() ? move(moves.get(at)) : null;
            if (at < moves.size()) {
                if (data != null) {
                    int type = typeId(data.type);
                    if (types != null && type >= 0) drawImg(b, types, h, 12f, yPos + 2f, 64f, 28f, 0, type * 28);   // :110-111
                    f.draw(b, data.name, 80f, h - yPos, WHITE, SHADOW);           // :112-113
                    if (data.pp > 0) {                                            // :114
                        f.draw(b, "PP", 112f, h - (yPos + 32f), WHITE, SHADOW);
                        f.drawRight(b, data.pp + "/" + data.pp, 230f, h - (yPos + 32f), WHITE, SHADOW);   // :117-118
                    }
                } else {
                    f.draw(b, "-", 80f, h - yPos, WHITE, SHADOW);                 // :122
                    f.drawRight(b, "--", 228f, h - (yPos + 32f), WHITE, SHADOW);  // :123
                }
            }
            yPos += 64f;
        }
        if (sel != null && moves.size() > 0) {
            drawImg(b, sel, h, 0f, 78f + row * 64f, 258f, 72f, 0, 0);            // :128-130 the selection frame
        }
        PbsData.Move current = moves.isEmpty() ? null : move(moves.get(cursor.index()));
        int basedamage = current == null ? 0 : current.power;                     // :131-134
        int accuracy = current == null ? 0 : current.accuracy;
        f.draw(b, "分类", 272f, h - 114f, WHITE, BLACK);                           // :135
        f.draw(b, "威力", 272f, h - 146f, WHITE, BLACK);                           // :136
        f.drawCentered(b, basedamage <= 1 ? basedamage == 1 ? "???" : "---" : String.valueOf(basedamage),
                468f, h - 146f, VALUE, VALUE_SHADOW);                             // :137-138
        f.draw(b, "命中", 272f, h - 178f, WHITE, BLACK);                           // :139
        f.drawCentered(b, accuracy == 0 ? "---" : accuracy + "%", 468f, h - 178f, VALUE, VALUE_SHADOW);   // :140-141
        Texture category = a.graphic("Pictures", "category");                     // :143
        if (category != null && current != null) {
            drawImg(b, category, h, 436f, 116f, 64f, 28f, 0, categoryIndex(current.category) * 28);
        }
        Texture buttons = a.graphic("Pictures", "reminderButtons");
        if (buttons != null && cursor.index() < moves.size() - 1) {
            drawImg(b, buttons, h, 48f, 350f, 76f, 32f, 0, 0);                   // :144-146
        }
        if (buttons != null && cursor.index() > 0) {
            drawImg(b, buttons, h, 134f, 350f, 76f, 32f, 76, 0);                 // :147-149
        }
        if (current != null && current.description != null) {
            drawWrapped(b, f, current.description, 272f, 210f, 230f, 5, h);      // :151-154 drawTextEx(.., 230, 5, ..)
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

    private static final Pattern CONTROL = Pattern.compile("\\\\[A-Za-z]+(\\[[^\\]]*\\])?");

    private static void drawWrapped(SpriteBatch b, MenuFont f, String text, float x, float topY, float width, int maxLines, float h) {
        Matcher m = CONTROL.matcher(text.replace("\r", "").replace("\n", " "));
        String cleaned = m.replaceAll("");
        StringBuilder line = new StringBuilder();
        float lineHeight = f.lineHeight();
        int drawn = 0;
        for (int i = 0; i < cleaned.length() && drawn < maxLines; i++) {
            char c = cleaned.charAt(i);
            if (f.width(line.toString() + c) > width && line.length() > 0) {
                f.draw(b, line.toString(), x, h - (topY + drawn * lineHeight), VALUE, VALUE_SHADOW);
                drawn++;
                line.setLength(0);
            }
            line.append(c);
        }
        if (drawn < maxLines && line.length() > 0) {
            f.draw(b, line.toString(), x, h - (topY + drawn * lineHeight), VALUE, VALUE_SHADOW);
        }
    }
}
