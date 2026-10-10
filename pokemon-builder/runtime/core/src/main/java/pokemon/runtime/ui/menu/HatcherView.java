package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.field.ItemScene;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.ui.WindowSkin;

import java.util.Arrays;

/**
 * 323_Egg_Hatcher {@code Hatcher} (the screen of {@code openHatcher}): six slots for eggs that hatch while the player walks.
 * The arrow keys move the cursor ({@code Choose} SE), C on an empty slot asks where to take an egg from (the party screen or
 * the boxes), B closes. Each slot shows the egg's icon (animated faster the closer it is to hatching) and the steps left;
 * the text at the right says how the selected egg is doing. Everything counts in 40 fps ticks.
 */
public final class HatcherView {
    private static final Color STEP_BASE = new Color(6f / 255f, 35f / 255f, 52f / 255f, 1f);          // :56
    private static final Color STEP_SHADOW = new Color(169f / 255f, 179f / 255f, 184f / 255f, 1f);
    private static final Color TEXT_BASE = new Color(96f / 255f, 96f / 255f, 96f / 255f, 1f);        // drawFormattedTextEx's default
    private static final Color TEXT_SHADOW = new Color(208f / 255f, 208f / 255f, 200f / 255f, 1f);

    private final RuntimeContext context;
    private final TrainerState trainer;
    private final MenuClock clock = new MenuClock();
    private final PbMessage pbMessage;
    private int index;
    private int tick;
    private boolean finished;
    private PartyView partyView;
    private StorageView storageView;
    private int[] partyChoice = {-1};

    public HatcherView(RuntimeContext context) {
        this.context = context;
        this.trainer = context.gameState().trainer();
        this.pbMessage = new PbMessage(context);
    }

    private AudioManager audio() {
        return context.audioManager();
    }

    private void se(String name) {
        if (audio() != null) audio().playSe(name, 100, 100);
    }

    // =====================================================================
    // update
    // =====================================================================

    /** @return true when the screen is closed */
    public boolean update(InputManager input) {
        int ticks = clock.advance();
        if (partyView != null) {
            if (partyView.update(input)) partyClosed();
            return finished;
        }
        if (storageView != null) {
            if (storageView.update(input)) storageClosed();
            return finished;
        }
        if (pbMessage.active()) {
            pbMessage.update(input, ticks);
            return finished;
        }
        tick += ticks;
        if (input.wasPressed(GameAction.RIGHT) && (index + 1) % 3 != 0) {                          // :144
            index += 1;
            se("Choose");
        }
        if (input.wasPressed(GameAction.LEFT) && (index != 0 && index != 3)) {                     // :150
            index -= 1;
            se("Choose");
        }
        if (input.wasPressed(GameAction.UP) && index >= 3) {                                       // :156
            index -= 3;
            se("Choose");
        }
        if (input.wasPressed(GameAction.DOWN) && index <= 2) {                                     // :162
            index += 3;
            se("Choose");
        }
        if (input.wasPressed(GameAction.CONFIRM)) {                                                // :168
            if (trainer.hatcherEggs[index] == null) {
                ask();
            }
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {            // :216
            finished = true;
        }
        return finished;
    }

    /** :170-173 the question for an empty slot. */
    private void ask() {
        pbMessage.start("孵化器是空的，\n要从哪里选择蛋并放入？", Arrays.asList("队伍", "盒子", "取消"), -1, 0, ret -> {
            if (ret == 0) {                                                    // :174 the party
                partyChoice = new int[] {-1};
                partyView = PartyView.forItem(context, (ItemScene scene) -> {  // :177-182 pbFadeOutIn { PokemonParty_Scene ... }
                    scene.pbStartScene("请选择一颗蛋。", null);                    // :181
                    partyChoice[0] = scene.pbChoosePokemon("请选择一颗蛋。");        // :182
                }, null);
            } else if (ret == 1) {                                             // :198 the boxes
                storageView = StorageView.chooseEggToHatch(context);           // :203-206
            }
        });
    }

    /** :184-196 */
    private void partyClosed() {
        partyView = null;
        int chosen = partyChoice[0];
        if (chosen >= 0 && chosen <= 5) {
            Pokemon pkmn = trainer.party.get(chosen);
            if (!pkmn.egg) {
                pbMessage.start("选择的宝可梦不是一颗蛋。", null, 0, 0, ignored -> { });   // :188 Kernel.pbMessage
            } else {
                trainer.hatcherEggs[index] = pkmn;                              // :190
                trainer.party.members().removeIndex(chosen);                    // :191 party.delete_at(chosen)
            }
        }
    }

    /** :207-211 */
    private void storageClosed() {
        int[] chosen = storageView.eggChoice();
        storageView.dispose();
        storageView = null;
        if (chosen != null) {
            trainer.hatcherEggs[index] = trainer.currentStorage().get(chosen[0], chosen[1]);   // :208
            trainer.currentStorage().pbDelete(chosen[0], chosen[1]);                           // :209
        }
    }

    // =====================================================================
    // render
    // =====================================================================

    private Texture icon(MenuAssets a, Pokemon p) {
        if (p.species == null) return a.icon("iconEgg");
        Texture icon = PokemonIcons.of(a, p.species, p.gender == pokemon.runtime.pokemon.PokemonStats.FEMALE, p.shiny, p.superShiny, p.formIndex(), false, true);
        if (icon == null) icon = a.icon("iconEgg");
        return icon;
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech, MenuFont smallFont) {
        if (partyView != null) {
            partyView.render(b, a, f, skin, smallFont);
            return;
        }
        if (storageView != null) {
            storageView.render(b, a, f, skin, speech, smallFont);
            return;
        }
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        Texture bg = a.graphic("Pictures/Egg Hatcher", "hatcherbg");              // :113
        if (bg != null) b.draw(bg, 0f, h - bg.getHeight());
        for (int i = 0; i < 6; i++) {                                              // :135-148 EggSprite for every slot
            float x = 46f + 80f * (i % 3);
            float y = i < 3 ? 46f : 158f;
            Pokemon egg = trainer.hatcherEggs[i];
            if (egg != null) {
                int frameskip = 20;                                                // :50-54 the closer to hatching, the faster
                if (egg.stepsToHatch < 10200) frameskip = 15;
                if (egg.stepsToHatch < 2550) frameskip = 10;
                if (egg.stepsToHatch < 1275) frameskip = 5;
                Texture icon = icon(a, egg);
                if (icon != null) {
                    int frameWidth = icon.getWidth() / 2;                          // AnimatedSprite.create(file, 2, frameskip)
                    int frame = (tick / frameskip) % 2;
                    b.draw(icon, x + 2f, h - (y - 5f) - icon.getHeight(), frameWidth, icon.getHeight(),
                            frame * frameWidth, 0, frameWidth, icon.getHeight(), false, false);
                }
                f.drawCentered(b, String.valueOf(egg.stepsToHatch), x + 35f, h - (y + 65f), STEP_BASE, STEP_SHADOW);   // :57 [steps, 35, 65, 2]
            }
            if (i == index) {
                Texture sel = a.graphic("Pictures/Egg Hatcher", "selection");      // :61-62
                if (sel != null) b.draw(sel, x, h - y - sel.getHeight());
            }
        }
        Pokemon current = trainer.hatcherEggs[index];
        if (current != null) {                                                     // :120-130 how the selected egg is doing
            String eggstate = "看来这个蛋需\n要很长时间才\n能孵化。";
            if (current.stepsToHatch < 10200) eggstate = "会孵化出什么\n宝可梦呢？似乎\n还需要一些时间。";
            if (current.stepsToHatch < 2550) eggstate = "蛋偶尔会摇动，\n应该是快\n要孵化了。";
            if (current.stepsToHatch < 1275) eggstate = "可以听到从里\n面传出的声音！\n似乎要孵化了！";
            String[] lines = eggstate.split("\n");
            for (int i = 0; i < lines.length; i++) {
                f.draw(b, lines[i], 356f, h - (84f + i * 32f + (32f - f.lineHeight()) / 2f), TEXT_BASE, TEXT_SHADOW);
            }
        }
        if (pbMessage.active()) pbMessage.render(b, a, f, skin, speech, w, h);
    }
}
