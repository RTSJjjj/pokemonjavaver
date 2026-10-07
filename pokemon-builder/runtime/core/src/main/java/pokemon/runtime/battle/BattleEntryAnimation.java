package pokemon.runtime.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.GameState;
import pokemon.runtime.ui.menu.MenuAssets;
import pokemon.runtime.ui.menu.MenuFont;
import pokemon.runtime.ui.menu.MenuPanel;

import java.io.File;

/**
 * The battle entry animation. {@code PField_Visuals:18-133 pbBattleAnimation}
 * plays a transition before the battle scene appears, and the project's
 * {@code rocket} section ({@code rocket:6-101}) overrides it with an evil-team
 * VS animation:
 *
 * <ol>
 *   <li>an active {@code EVIL_TEAMS} switch plays the evil-team VS
 *       ({@code rocket:31-101});</li>
 *   <li>otherwise a single trainer battle with {@code vsBar}/{@code vsTrainer}
 *       graphics plays the built-in VS screen
 *       ({@code PField_Visuals:135-291});</li>
 *   <li>otherwise the default transition ({@code PField_Visuals:77-106}): a
 *       32-frame white/black flash, then the project's own KGC special
 *       transition ({@code BattleEntryTransition}) chosen by
 *       {@code PField_Visuals:44-76}, then a short black pause.</li>
 * </ol>
 *
 * <p>It runs over the live map (the plugin plays it in a transition viewport
 * above the map) and ends on a black screen, which the battle scene's own intro
 * then fades out.</p>
 */
public final class BattleEntryAnimation {

    /** One frame at the project's 40 fps. */
    private static final float TICK = 1f / 40f;
    /** rocket:6-14 EVIL_TEAMS: switch id -> Graphics/Transitions image. */
    private static final int[] EVIL_SWITCHES = { 211, 210, 212, 213, 214, 215, 216 };
    private static final String[] EVIL_IMAGES = { "001_Rocket", "000_Shadow", "002_Magma",
            "003_Aqua", "005_Plasma", "006_Hyacinth", "007_Galactic" };

    private enum Mode { EVIL_VS, VS, DEFAULT }

    private final RuntimeContext context;
    private final MenuAssets assets;
    private final MenuFont font;
    private final Mode mode;
    private final String evilImage;
    private final String playerBar;
    private final String foeBar;
    private final String playerTrainer;
    private final String foeTrainer;
    private final String playerName;
    private final String foeName;
    private final boolean whiteFlash;

    private float elapsed;
    private int frame;
    private boolean done;

    // The VS sprite (PField_Visuals:167-183 / rocket:42-49).
    private float vsX;
    private float vsY;
    private float vsZoom = 1f;
    private boolean logoVisible;
    // The flash sprite.
    private float flashAlpha;
    private boolean flashBlack;
    // The bars / trainers (PField_Visuals:168-228).
    private float xOffset = 330f;
    private float playerBarX;
    private float foeBarX;
    private float playerBarOx;
    private float foeBarOx;
    private float playerSlide;
    private float foeSlide;
    // PField_Visuals:58-98: the default (non-VS) entry plays a KGC transition.
    private BattleEntryTransition transition;
    private boolean transitionStarted;
    private Texture snapshot;

    public BattleEntryAnimation(RuntimeContext context, InteractiveBattlePort.Session session, int location) {
        this.context = context;
        GraphicsLocator locator = new GraphicsLocator(context.database());
        this.assets = new MenuAssets(locator);
        String fontName = context.database().project().runtime.messageFont;
        File file = fontName == null ? null : locator.font(fontName);
        this.font = new MenuFont(file);

        GameState state = context.gameState();
        PbsData pbs = context.pbsData();
        float w = pokemon.runtime.app.ScreenMetrics.logicalWidth();
        float h = pokemon.runtime.app.ScreenMetrics.logicalHeight();
        this.vsX = w / 2f;
        this.vsY = h / 2f;
        this.flashAlpha = 0f;
        this.xOffset = ((w / 2f) / 10f) * 10f;

        // rocket:19-29: an active evil-team switch takes priority.
        String evil = null;
        for (int i = 0; i < EVIL_SWITCHES.length; i++) {
            if (state.switches().get(EVIL_SWITCHES[i])) {
                evil = EVIL_IMAGES[i];
                break;
            }
        }
        this.evilImage = evil;

        // PField_Visuals:138-145: the built-in VS needs one trainer battle and
        // both graphics for the player and the opponent.
        String pBar = null;
        String fBar = null;
        String pTra = null;
        String fTra = null;
        if (evil == null && session != null && session.trainerBattle && session.trainerData != null) {
            int playerType = playerTrainerType(pbs, state);
            int foeType = trainerTypeId(pbs, session.trainerData.type);
            if (playerType >= 0 && foeType >= 0) {
                pBar = vsGraphic("vsBar", playerType);
                fBar = vsGraphic("vsBar", foeType);
                pTra = vsGraphic("vsTrainer", playerType);
                fTra = vsGraphic("vsTrainer", foeType);
            }
        }
        this.playerBar = pBar;
        this.foeBar = fBar;
        this.playerTrainer = pTra;
        this.foeTrainer = fTra;
        this.playerName = state.playerName();
        this.foeName = session == null || session.trainerData == null ? "" : session.trainerData.name;
        // PField_Visuals:78-82: a cave / night flashes black, otherwise white.
        boolean night = pokemon.runtime.map.DayNightTone.test("isNight?");
        this.whiteFlash = !(location == 2 || night);
        if (evil != null) {
            mode = Mode.EVIL_VS;
            flashAlpha = 255f;
            playSe("Vs sword");                 // rocket:57
        } else if (pBar != null && fBar != null && pTra != null && fTra != null) {
            mode = Mode.VS;
            playerBarX = -xOffset;
            foeBarX = xOffset;
            playerSlide = -xOffset;
            foeSlide = xOffset;
        } else {
            mode = Mode.DEFAULT;
            // PField_Visuals:40-98: pick and prepare the KGC transition.
            int battletype = session != null && session.trainerBattle ? 1 : 0;
            transition = new BattleEntryTransition(battleAnim(location, battletype, night), 50,
                    assets, null, w, h);
        }
    }

    /** PField_Visuals:44-76: the transition name by location, type and day/night. */
    private static String battleAnim(int location, int battletype, boolean night) {
        int loc = Math.max(0, Math.min(3, location));
        if (battletype == 3) {
            return "FourBallBurst";
        }
        if (night) {
            if (battletype == 1) {
                return new String[] { "SpinBallSplit", "BallDown", "BallDown", "WavySpinBall" }[loc];
            }
            return new String[] { "SnakeSquares", "DiagonalBubbleBR", "DiagonalBubbleBR",
                    "RisingSplash" }[loc];
        }
        if (battletype == 1) {
            return new String[] { "TwoBallPass", "ThreeBallDown", "BallDown",
                    "WavyThreeBallUp" }[loc];
        }
        return new String[] { "SnakeSquares", "DiagonalBubbleTL", "DiagonalBubbleBR",
                "RisingSplash" }[loc];
    }

    /** Whether the frozen screen ({@code Graphics.snap_to_bitmap}) must be taken. */
    public boolean needsTransitionSnapshot() {
        return mode == Mode.DEFAULT && transition != null && transition.needsBuffer();
    }

    /** Hands over the {@code Graphics.snap_to_bitmap} capture; owned by this entry. */
    public void setSnapshot(Texture texture) {
        this.snapshot = texture;
        if (transition != null) {
            transition.setBuffer(texture);
        }
    }

    /** Frees the captured snapshot (called by {@code MapScreen} when done). */
    public void dispose() {
        if (snapshot != null) {
            snapshot.dispose();
            snapshot = null;
        }
    }

    public boolean finished() {
        return done;
    }

    private int playerTrainerType(PbsData pbs, GameState state) {
        if (pbs == null || context.database() == null || context.database().project() == null) {
            return -1;
        }
        pokemon.runtime.data.ProjectInfo.PlayerGraphic graphic =
                context.database().project().runtime.player(state.playerId());
        if (graphic == null || graphic.trainerType == null) {
            return -1;
        }
        PbsData.TrainerType type = pbs.trainerTypes.get(graphic.trainerType);
        return type == null ? -1 : type.id;
    }

    private int trainerTypeId(PbsData pbs, String type) {
        if (pbs == null || type == null) {
            return -1;
        }
        PbsData.TrainerType trainerType = pbs.trainerTypes.get(type);
        return trainerType == null ? -1 : trainerType.id;
    }

    /** PField_Visuals:141-144 / :161-166: vsBarNNN / vsTrainerNNN (numeric fallback). */
    private String vsGraphic(String prefix, int trainerType) {
        String name = prefix + trainerType;
        return assets.graphic("Transitions", name) == null ? null : name;
    }

    private void playSe(String name) {
        if (context.audioManager() != null) {
            context.audioManager().playSe(name, 100, 100);
        }
    }

    // ------------------------------------------------------------------
    // Update
    // ------------------------------------------------------------------

    public void update(float delta) {
        if (done) {
            return;
        }
        elapsed += Math.max(0f, delta);
        int target = (int) (elapsed / TICK);
        while (frame < target) {
            stepFrame(frame);
            frame++;
        }
    }

    private void stepFrame(int i) {
        switch (mode) {
            case EVIL_VS: {
                // rocket:62-93.
                if (i < 48) {
                    flashAlpha = Math.max(0f, flashAlpha - 26f);
                } else if (i == 48) {
                    flashBlack = true;
                } else if (i >= 100) {
                    flashAlpha = Math.min(255f, flashAlpha + 26f);
                }
                shudder(i, 48, 100);
                if (i >= 110) {
                    done = true;
                }
                break;
            }
            case VS: {
                // PField_Visuals:185-272.
                if (i < 10) {
                    playerBarX = xOffset * (i + 1 - 10) / 10f;
                    foeBarX = xOffset * (10 - i - 1) / 10f;
                } else if (i == 10) {
                    playSe("Vs flash");
                    playSe("Vs sword");
                    flashAlpha = 255f;
                } else if (i < 58) {
                    int j = i - 10;
                    flashAlpha = Math.max(0f, flashAlpha - 26f);
                    playerBarOx -= 16f;
                    foeBarOx += 16f;
                    if (j >= 24 && j < 34) {
                        playerSlide = xOffset * (j + 1 - 10 - 24) / 10f;
                        foeSlide = xOffset * (10 - j - 1 + 24) / 10f + 32f;
                    }
                    if (j == 33) {
                        playerSlide = 0f;
                        foeSlide = 32f;
                        flashAlpha = 255f;
                        playSe("Vs sword");
                        logoVisible = true;
                    }
                } else {
                    int j = i - 58;
                    if (j < 70) {
                        flashAlpha = Math.max(0f, flashAlpha - 26f);
                    } else if (j == 70) {
                        flashBlack = true;
                    } else if (j >= 100) {
                        flashAlpha = Math.min(255f, flashAlpha + 26f);
                    }
                    playerBarOx -= 16f;
                    foeBarOx += 16f;
                    shudder(j, 70, 100);
                    if (j >= 110) {
                        done = true;
                    }
                }
                break;
            }
            default: {
                // PField_Visuals:77-106: two 16-frame flashes, the KGC
                // transition (40*1.25 = 50 frames), then a black pause.
                int flash = 32;            // 2 x (8 up + 8 down)
                if (i < flash) {
                    int half = 8;
                    int t = i % (half * 2);
                    float alpha = t < half ? (t + 1) * 32f : (half * 2 - t) * 32f;
                    flashAlpha = Math.min(255f, alpha);
                    flashBlack = !whiteFlash;
                } else if (i < flash + 50) {
                    flashAlpha = 0f;
                    if (transition != null) {
                        transitionStarted = true;
                        transition.update();
                    }
                } else if (i < flash + 54) {
                    flashBlack = true;
                    flashAlpha = 255f;
                } else {
                    done = true;
                }
                break;
            }
        }
        // The black end state (PField_Visuals:99 / :285 / rocket:98).
        if (done) {
            flashBlack = true;
            flashAlpha = 255f;
        }
    }

    /** The shared shudder / zoom of the VS logo. */
    private void shudder(int i, int shudderTime, int zoomTime) {
        if (i < shudderTime) {
            int j = i % 4;
            if (j >= 1 && j < 3) {
                vsX += 2f;
                vsY -= 2f;
            } else {
                vsX -= 2f;
                vsY += 2f;
            }
        } else if (i < zoomTime) {
            vsZoom += 0.2f;
        }
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    public void render(SpriteBatch batch, float w, float h) {
        // PField_Visuals:97-98: the KGC transition draws first, the flash cover
        // (and the final black) sits above it.
        if (mode == Mode.DEFAULT && transitionStarted && transition != null) {
            transition.render(batch, w, h);
        }
        // The flash: white until PField_Visuals:251, black afterwards; the black
        // end state covers everything (the battle intro fades it out).
        if (flashAlpha > 0f) {
            Color color = flashBlack ? Color.BLACK : Color.WHITE;
            MenuPanel.fill(batch, assets, 0f, 0f, w, h, color.r, color.g, color.b, flashAlpha / 255f);
        }
        if (mode == Mode.EVIL_VS) {
            Texture vs = assets.graphic("Transitions", evilImage);
            if (vs != null) {
                float dw = vs.getWidth() * vsZoom;
                float dh = vs.getHeight() * vsZoom;
                batch.draw(vs, vsX - dw / 2f, vsY - dh / 2f, dw, dh);
            }
            return;
        }
        if (mode == Mode.VS) {
            drawPlane(batch, playerBar, -xOffset, w / 2f, h / 3f, w / 2f, 128f, playerBarX, playerBarOx);
            drawPlane(batch, foeBar, xOffset, w / 2f, h / 3f, w / 2f, 128f, foeBarX, foeBarOx);
            drawSprite(batch, playerTrainer, 0f, w / 2f, h / 3f, playerSlide, Color.WHITE);
            // PField_Visuals:214/234: the opponent is blacked out until the VS
            // logo appears, then its tone returns to normal.
            drawSprite(batch, foeTrainer, w / 2f, w / 2f, h / 3f, foeSlide,
                    logoVisible ? Color.WHITE : Color.BLACK);
            if (logoVisible) {
                Texture vs = assets.graphic("Transitions", "vs");
                if (vs != null) {
                    float dw = vs.getWidth() * vsZoom;
                    float dh = vs.getHeight() * vsZoom;
                    batch.draw(vs, vsX - dw / 2f, vsY - dh / 2f, dw, dh);
                }
                font.drawCentered(batch, playerName, w / 4f, h - (h / 1.5f + 10f),
                        new Color(248 / 255f, 248 / 255f, 248 / 255f, 1f),
                        new Color(72 / 255f, 72 / 255f, 72 / 255f, 1f));
                font.drawCentered(batch, foeName, w / 4f + w / 2f, h - (h / 1.5f + 10f),
                        new Color(248 / 255f, 248 / 255f, 248 / 255f, 1f),
                        new Color(72 / 255f, 72 / 255f, 72 / 255f, 1f));
            }
        }
    }

    /** AnimatedPlane: a 128px strip tiled across the half-screen viewport. */
    private void drawPlane(SpriteBatch batch, String name, float anchorX, float viewX, float viewY,
            float viewW, float viewH, float spriteX, float ox) {
        Texture tex = name == null ? null : assets.graphic("Transitions", name);
        if (tex == null) {
            return;
        }
        float start = viewX + spriteX;
        float offset = ox % tex.getWidth();
        for (float x = start - tex.getWidth() + offset; x < viewX + viewW; x += tex.getWidth()) {
            float drawX = Math.max(viewX, x);
            int srcX = (int) (drawX - x);
            int srcW = (int) Math.min(tex.getWidth() - srcX, viewX + viewW - drawX);
            if (srcW <= 0) {
                continue;
            }
            batch.draw(tex, drawX, viewY, srcW, viewH, srcX, 0, srcW, tex.getHeight(), false, false);
        }
    }

    private void drawSprite(SpriteBatch batch, String name, float viewX, float viewW, float viewY,
            float spriteX, Color tint) {
        Texture tex = name == null ? null : assets.graphic("Transitions", name);
        if (tex == null) {
            return;
        }
        float drawX = viewX + spriteX;
        if (tint != Color.WHITE) {
            batch.setColor(tint.r * 0.35f, tint.g * 0.35f, tint.b * 0.35f, 1f);
        }
        batch.draw(tex, drawX, viewY, tex.getWidth(), tex.getHeight());
        batch.setColor(Color.WHITE);
    }
}
