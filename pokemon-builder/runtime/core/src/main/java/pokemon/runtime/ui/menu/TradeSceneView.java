package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.battle.TradeAnimation;
import pokemon.runtime.field.EvolutionWorld;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PBEvolution;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.ui.WindowSkin;

/**
 * 227_PScreen_Trading: {@code pbStartTrade}'s scene (pbFadeOutInWithMusic { PokemonTrade_Scene: pbStartScreen, pbTrade,
 * pbEndScreen }) and the evolution check that ends it ({@code pbTradeCheckEvolution}). The messages are the scene's own
 * ({@code _INTL}/{@code _ISPRINTF} strings of pbTrade); the animations are {@link TradeAnimation}.
 *
 * <p>The Pokemon the player gets replaces the traded one in the party once the block is over
 * ({@code $Trainer.party[pokemonIndex] = yourPokemon}, :225).</p>
 *
 * <p>登记: {@code pbApplyBattlerMetricsToSprite} (the front sprite's metric offsets) and the pause arrow are not drawn.</p>
 */
public final class TradeSceneView {
    private enum Phase { OUTER_OUT, SPRITES_IN, MESSAGE, INTRO_WAIT, ANIMATION, SPRITES_OUT, EVOLUTION, OUTER_IN, DONE }

    private final RuntimeContext context;
    private final Pokemon mine;
    private final Pokemon yours;
    private final int index;
    private final String trader1;
    private final String trader2;
    private final MenuClock clock = new MenuClock();
    private final SceneMessage message;
    private final TradeAnimation animation;
    private Phase phase = Phase.OUTER_OUT;
    private int tick;
    private float blackAlpha;
    private boolean finished;
    private boolean assetsLoaded;
    private Texture background;
    private Texture texture1;
    private Texture texture2;
    private Runnable afterAnimation;
    private EvolutionView evolution;

    public TradeSceneView(RuntimeContext context, int index, Pokemon yours, String trader1, String trader2) {
        this.context = context;
        this.index = index;
        this.mine = context.gameState().trainer().party.get(index);
        this.yours = yours;
        this.trader1 = trader1;
        this.trader2 = trader2;
        this.message = new SceneMessage(context);
        AudioManager audio = context.audioManager();
        String cry = audio == null || yours.species == null ? null
                : audio.resolveCryFile(yours.species.id, yours.species.internalName, yours.formIndex());
        this.animation = new TradeAnimation(Math.max(0, mine.ballused), Math.max(0, yours.ballused), cry,
                (name, volume, pitch) -> {
                    if (context.audioManager() != null) context.audioManager().playSe(name, volume, pitch);
                });
        if (audio != null) {
            audio.memorizeBgmAndBgs();                       // pbFadeOutInWithMusic (253_PSystem_Utilities:136-148)
        }
    }

    public boolean finished() {
        return finished;
    }

    private static String name(Pokemon pokemon) {
        return pokemon.species == null ? "" : pokemon.species.name;
    }

    /** {@code _ISPRINTF("{1:s}\\r\\nID: {2:05d}   OT: {3:s}")} (:172-173, :183-184). */
    private static String idLine(Pokemon pokemon) {
        return pokemon.name + "\nID: " + String.format("%05d", pokemon.publicID & 0xFFFF) + "   OT: "
                + (pokemon.originalTrainer == null ? "" : pokemon.originalTrainer);
    }

    // =====================================================================
    // Update
    // =====================================================================

    public boolean update(InputManager input) {
        if (evolution != null) {
            if (evolution.update(input)) {
                evolution = null;
                phase = Phase.OUTER_IN;
                tick = 0;
            }
            return finished;
        }
        boolean confirm = input.wasPressed(GameAction.CONFIRM);
        boolean cancel = input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU);
        if (phase == Phase.INTRO_WAIT) {                                     // pbMessageWaitForInput breaks on C / B
            if (confirm || cancel) {
                tick = 100;
            }
        } else {
            message.input(confirm, cancel);
        }
        int ticks = clock.advance();
        for (int i = 0; i < ticks && !finished && evolution == null; i++) {
            step();
        }
        return finished;
    }

    private void step() {
        switch (phase) {
            case OUTER_OUT:                                                  // pbFadeOutIn: j in 0..16
                blackAlpha = Math.min(255f, tick * 16f);
                if (++tick > 16) {
                    phase = Phase.SPRITES_IN;                                // pbStartScreen: pbFadeInAndShow
                    tick = 0;
                }
                break;
            case SPRITES_IN:
                blackAlpha = Math.max(0f, Math.min(255f, (16 - tick) * 16f));
                if (++tick > 16) {
                    startTrade();
                }
                break;
            case MESSAGE:
                message.tick();
                break;
            case INTRO_WAIT:
                message.tick();
                if (++tick >= 100) {
                    endIntroWait();
                }
                break;
            case ANIMATION:
                if (!animation.tick()) {
                    Runnable then = afterAnimation;
                    afterAnimation = null;
                    if (then != null) then.run();
                }
                break;
            case SPRITES_OUT:                                                // pbFadeOutAndHide
                blackAlpha = Math.max(0f, Math.min(255f, tick * 16f));
                if (++tick > 16) {
                    afterSpritesOut();
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

    private void show(String text, Runnable then) {
        phase = Phase.MESSAGE;
        message.show(text, then);
    }

    /** {@code pbTrade} (227:166-187). */
    private void startTrade() {
        context.audioManager().stopBgm();                                    // :167 pbBGMStop
        context.audioManager().playCry(mine.species == null ? 0 : mine.species.id);   // :168 pbPlayCry(@pokemon)
        String species1 = name(mine);
        String species2 = name(yours);
        // :171-173 the ID line goes on without waiting (\wtnp[0]); pbMessageWaitForInput(.., 50, true) follows
        phase = Phase.MESSAGE;
        message.show(idLine(mine) + "\\wtnp[0]", () -> {
            phase = Phase.INTRO_WAIT;                                        // :174
            tick = 0;
        });
        afterIntro = () -> {
            MenuSe.decision(context.audioManager());                         // :175 pbPlayDecisionSE
            runAnimation(true, () ->                                         // :176 pbScene1
                show(trader2 + "选择" + species2 + "\n与" + trader1 + "的" + species1 + "交换。" + SceneMessage.PAUSE, () ->   // :177-178
                    show(trader2 + "向" + species2 + "告别了。", () -> {          // :179-180 (trader2 says goodbye)
                        runAnimation(false, () ->                            // :181 pbScene2
                            show(idLine(yours) + SceneMessage.PAUSE, () ->   // :182-184
                                show("请照顾好" + species2 + "！", this::endScreen)));   // :185-186
                    })));
        };
    }

    private Runnable afterIntro;

    private void endIntroWait() {
        Runnable next = afterIntro;
        afterIntro = null;
        if (next != null) next.run();
    }

    private void runAnimation(boolean first, Runnable then) {
        if (first) animation.startScene1(); else animation.startScene2();
        afterAnimation = then;
        phase = Phase.ANIMATION;
    }

    /** {@code pbEndScreen} (:152-164): the window goes, the sprites fade out, then the evolution check. */
    private void endScreen() {
        message.clear();
        phase = Phase.SPRITES_OUT;
        tick = 0;
    }

    private void afterSpritesOut() {
        blackAlpha = 255f;
        // :157-163 newspecies = pbTradeCheckEvolution(@pokemon2, @pokemon)
        PBEvolution.Env env = new EvolutionWorld(context);
        String evolved = PBEvolution.checkEvolutionEx(yours, (p, method, parameter, species) ->
                PBEvolution.hasTradeCheck(method) && PBEvolution.tradeCheck(method, p, parameter, mine, env) ? species : null);
        PbsData.Species target = evolved == null || context.pbsData() == null ? null : context.pbsData().species(evolved);
        if (target != null) {
            evolution = new EvolutionView(context, yours, target, false, false);   // evo.pbEvolution(false)
            phase = Phase.EVOLUTION;
            return;
        }
        phase = Phase.OUTER_IN;
        tick = 0;
    }

    private void finish() {
        context.gameState().trainer().party.members().set(index, yours);     // :225 $Trainer.party[pokemonIndex] = yourPokemon
        if (context.audioManager() != null) {
            context.audioManager().restoreBgmAndBgs();                       // pbFadeOutInWithMusic's resume
        }
        animation.dispose();
        phase = Phase.DONE;
        finished = true;
    }

    // =====================================================================
    // Render
    // =====================================================================

    private void loadAssets(MenuAssets a) {
        if (assetsLoaded) {
            return;
        }
        assetsLoaded = true;
        background = a.graphic("Pictures", "tradebg");                       // :36 addBackgroundOrColoredPlane "tradebg"
        texture1 = EvolutionView.battler(a, mine.species, mine.shiny, mine.formIndex());
        texture2 = EvolutionView.battler(a, yours.species, yours.shiny, yours.formIndex());
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        float w = ScreenMetrics.logicalWidth();
        float h = ScreenMetrics.logicalHeight();
        if (evolution != null) {
            evolution.render(b, a, f, skin);
            return;
        }
        loadAssets(a);
        boolean scene = phase != Phase.OUTER_OUT && phase != Phase.OUTER_IN && phase != Phase.DONE;
        if (scene) {
            if (background != null) {
                b.draw(background, 0f, h - background.getHeight());
            } else {
                b.setColor(248f / 255f, 248f / 255f, 248f / 255f, 1f);
                b.draw(a.pixel(), 0f, 0f, w, h);
                b.setColor(Color.WHITE);
            }
            animation.render(b, name -> {
                String logical = name.startsWith("Graphics/") ? name.substring("Graphics/".length()) : name;
                int slash = logical.lastIndexOf('/');
                return slash < 0 ? null : a.graphic(logical.substring(0, slash), logical.substring(slash + 1));
            }, texture1, texture2, h);
            message.draw(b, a, f, skin, w, h);
        }
        if (blackAlpha > 0f) {
            b.setColor(0f, 0f, 0f, Math.min(1f, blackAlpha / 255f));
            b.draw(a.pixel(), 0f, 0f, w, h);
            b.setColor(Color.WHITE);
        }
    }
}
