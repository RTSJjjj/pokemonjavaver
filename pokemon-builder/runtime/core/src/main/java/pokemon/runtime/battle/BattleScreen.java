package pokemon.runtime.battle;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.FitViewport;
import pokemon.runtime.app.*;
import pokemon.runtime.input.*;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.pokemon.*;
import pokemon.runtime.ui.WindowSkin;
import pokemon.runtime.ui.menu.*;
import java.io.File;
import java.util.*;

/**
 * P2d: the battle scene, transcribed from the project's {@code PokeBattle_Scene}
 * (PokeBattle_SceneConstants / Scene_Initialize / PokeBattle_SceneMenus /
 * PokeBattle_SceneElements) and its animation classes
 * ({@code PokeBattle_SceneAnimations} / {@code Follower_Main}).
 *
 * <p>It follows the plugin's window model: exactly one of the message / command /
 * fight windows is visible at a time ({@code pbShowWindow}), and messages are
 * shown one at a time by {@code pbDisplayMessage} / {@code pbDisplayPausedMessage}
 * and then hidden again. Coordinates are the plugin's top-origin values,
 * converted with {@code screenHeight - y}.</p>
 *
 * <p>The battlefield itself is a sprite table ({@code @sprites},
 * {@link BattleSprites}) drawn in z order, exactly like RMXP; the animations in
 * {@code @animations} ({@link BattleAnimation}) drive it.</p>
 */
public final class BattleScreen extends ScreenAdapter implements BattleAnimation.Scene {
    // PokeBattle_Scene: BLANK / MESSAGE_BOX / COMMAND_BOX / FIGHT_BOX / TARGET_BOX
    private static final int BLANK = 0;
    private static final int MESSAGE_BOX = 1;
    private static final int COMMAND_BOX = 2;
    private static final int FIGHT_BOX = 3;
    private static final int TARGET_BOX = 4;
    /** {@link #page} while the target menu is up (Scene_Commands:419-474 pbChooseTarget). */
    private static final int PAGE_TARGET = 5;

    /** PokeBattle_Scene::MESSAGE_PAUSE_TIME (frames at 40 fps). */
    private static final int MESSAGE_PAUSE_TIME = 40;
    private static final int PAUSED_PAUSE_TIME = 120;

    /** PokeBattle_SceneConstants: the bar top is Graphics.height-96. */
    private static final float BAR_Y = PictureEx.Graphics.HEIGHT - 96f;
    /** {@code @sprites["messageWindow"]} height (Scene_Initialize:116-117 newWithSize(...,96)). */
    private static final float MESSAGE_WINDOW_HEIGHT = 96f;
    /**
     * PokemonDataBox colours (PokeBattle_SceneElements:16-21).
     */
    private static final Color NAME_BASE = new Color(255 / 255f, 255 / 255f, 255 / 255f, 1f);
    private static final Color NAME_SHADOW = new Color(112 / 255f, 112 / 255f, 112 / 255f, 1f);
    private static final Color MALE_BASE = new Color(48 / 255f, 96 / 255f, 216 / 255f, 1f);
    private static final Color FEMALE_BASE = new Color(248 / 255f, 88 / 255f, 40 / 255f, 1f);
    /** The command / fight windows (PokeBattle_SceneMenus:16-17). */
    private static final Color TEXT_BASE = new Color(248 / 255f, 248 / 255f, 248 / 255f, 1f);
    private static final Color TEXT_SHADOW = new Color(104 / 255f, 104 / 255f, 104 / 255f, 1f);
    /** PokeBattle_SceneMenus:356: the fight menu's move names draw no shadow. */
    private static final Color NO_SHADOW = new Color(0f, 0f, 0f, 0f);

    /**
     * FightMenuDisplay::PP_COLORS (PokeBattle_SceneMenus:216-221): four
     * base/shadow pairs for the selected move's PP text - 0 "Red, zero PP",
     * 1 "Orange, 1/4 of total PP or less", 2 "Yellow, 1/2 of total PP or less"
     * and 3 "Black, more than 1/2 of total PP" (:220), which is the menu's own
     * TEXT_BASE_COLOR/TEXT_SHADOW_COLOR (:16-17).
     */
    private static final Color[] PP_COLORS = {
            new Color(248 / 255f, 72 / 255f, 72 / 255f, 1f), new Color(136 / 255f, 48 / 255f, 48 / 255f, 1f),
            new Color(248 / 255f, 136 / 255f, 32 / 255f, 1f), new Color(144 / 255f, 72 / 255f, 24 / 255f, 1f),
            new Color(248 / 255f, 192 / 255f, 0f, 1f), new Color(144 / 255f, 104 / 255f, 0f, 1f),
            TEXT_BASE, TEXT_SHADOW
    };

    /** PokemonDataBox::STATUS_ICON_HEIGHT (PokeBattle_SceneElements:15). */
    private static final int STATUS_ICON_HEIGHT = 18;
    /** PokemonDataBox:232-233: the battleRank multiplier colours. */
    private static final Color RANK_BASE = new Color(255 / 255f, 255 / 255f, 0f, 1f);
    private static final Color RANK_SHADOW = new Color(255 / 255f, 128 / 255f, 0f, 1f);
    /** PokemonDataBox:241: the "???" a level above 200 draws. */
    private static final Color LEVEL_UNKNOWN_BASE = new Color(248 / 255f, 248 / 255f, 248 / 255f, 1f);
    private static final Color LEVEL_UNKNOWN_SHADOW = new Color(66 / 255f, 66 / 255f, 66 / 255f, 1f);
    /**
     * {@code BOSS_HP_RANK} (PokeBattle_BOSS:3) - indexed by
     * {@code pokemon.battleRank}. This project's PBS has no battleRank field;
     * only battle scripts assign one (PokeBattle_BOSS:7
     * "在定点pkmn战斗的脚本写pkmn.battleRank=X"), defaulting to 1
     * (PokeBattle_BOSS:12/16-18).
     */
    private static final int[] BOSS_HP_RANK = { 1, 1, 2, 5, 10, 15, 15, 20 };

    /**
     * {@code Window_AdvancedTextPokemon::@lineHeight} (SpriteWindow_text:128):
     * the plugin never uses the font's own line height for a text window - every
     * formatted character carries this height (DrawText:227) and every line
     * starts at a multiple of it (DrawText:221). {@code pbTopRightWindow}'s
     * window is sized from it.
     */
    private static final int ADVANCED_TEXT_LINE_HEIGHT = 32;
    /** {@code SpriteWindow#startX/startY} for {@code skinformat==0} (SpriteWindow:466-472). */
    private static final float WINDOW_CONTENTS_ORIGIN = 16f;
    /** {@code SpriteWindow#borderX/borderY} for {@code skinformat==0} (SpriteWindow:445-459). */
    private static final float WINDOW_BORDER = 32f;

    // ---------------------------------------------------------------------
    // The plugin's sprite table (@sprites) and animation list (@animations)
    // ---------------------------------------------------------------------
    private final BattleSprites sprites = new BattleSprites();
    /** {@code @animations} (PokeBattle_Scene:10), updated by pbGraphicsUpdate (:24-37). */
    private final List<BattleAnimation> animations = new ArrayList<>();
    private BattleSpriteShader spriteShader;
    private boolean spriteShaderWarned;
    /** {@code @frameCounter} (PokeBattle_Scene:11/41-42). */
    private float frameCounter;
    /** Fractions of a 40 fps frame not yet stepped. */
    private float frameAccum;

    private final RuntimeContext context;
    private final InteractiveBattlePort port;
    private final InteractiveBattlePort.Session session;
    private final MenuAssets assets;
    private final MenuFont font;
    private final MenuFont narrowFont;
    private final MenuFont smallFont;
    private final WindowSkin skin;
    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final MenuListModel cursor = new MenuListModel(4);
    private final List<String> itemIds = new ArrayList<>();
    private int page;                 // 0 command, 1 fight, 2 bag, 3 party, 4 item target
    private int window = BLANK;
    private String item;
    private boolean finished;
    private List<String> rows = new ArrayList<>();
    /** {@code pbFightMenu(idxBattler,canMegaEvolve)}'s argument, fixed when the menu opens (Battle_Phase_Command:73). */
    private boolean megaButton;
    /**
     * {@code pbFightMenu}'s {@code next false} (Battle_Phase_Command:84-86): the
     * move was refused, so the paused line it produced closes back into the fight
     * menu instead of starting the round.
     */
    private boolean fightMenuRefusal;
    private PartyView partyView;
    private BagView bagView;
    private String bagItem;
    private int bagTarget = -1;

    // ---------------------------------------------------------------------
    // pbCommandPhaseLoop(true) (Battle_Phase_Command:197-263) for a battle with more than one position per side:
    // the player's battlers choose one after the other.
    // ---------------------------------------------------------------------
    /** The battlers the player chooses for this round, in battler order (:204-208). */
    private int[] cmdOrder = new int[0];
    /** Index into {@link #cmdOrder} of the battler being asked. */
    private int cmdPos;
    /** {@code idxBattler} being asked, or -1 outside a double battle's command phase. */
    private int cmdBattler = -1;
    /** {@code actioned} (:200): the battlers that have been asked so far. */
    private final List<Integer> actioned = new ArrayList<>();
    /** {@code @lastCmd} / {@code @lastMove} (Scene_Commands:30/94). */
    private final int[] lastCmd = new int[6];
    private final int[] lastMove = new int[6];
    /** Scene_Commands:239-243 {@code @bagLastPocket} / {@code @bagChoices}: the battle bag's pocket and cursor, kept for the battle. */
    private pokemon.runtime.state.BagMemory bagMemory;
    /** The target menu (Scene_Commands:419-474): {@code texts}, {@code mode} (0 one target, 1 all with text) and {@code cw.index}. */
    private String[] targetTexts;
    private int targetMode;
    private int targetIndex;
    /** The fight-menu slot the target is for (-1: a Poke Ball from the Bag). */
    private int targetSlot = -1;
    private String targetBall;
    /** The player's switches of the round, played in {@code pbPriority} order (Battle_Phase_Attack:50-71). */
    private final java.util.ArrayDeque<int[]> switchersToPlay = new java.util.ArrayDeque<>();

    // pbDisplayMessage / pbDisplayPausedMessage: one message at a time, shown in
    // the MESSAGE_BOX and hidden when it is done (auto after 1s, or on input).
    private final java.util.ArrayDeque<Message> queue = new java.util.ArrayDeque<>();
    private Message message;
    private int revealedChars;
    private int holdFrames;
    /**
     * {@code @briefMessage} (PokeBattle_Scene:96-111/133): a brief message is
     * fully shown and lingering; the next message first waits
     * {@code MESSAGE_PAUSE_TIME} frames ({@code pbWaitMessage}) unless an
     * animation cleared this (Scene_Animations:212/225/240/300).
     */
    private boolean briefMessage;
    /** The shown {@link #message} no longer blocks the queue (a brief one that returned). */
    private boolean messageReleased;
    /** {@code pbWaitMessage}'s {@code MESSAGE_PAUSE_TIME.times} counter (:105-107). */
    private int waitMessageFrames;
    /** True while a round's events are being played (the message box stays up). */
    private boolean roundPlayback;
    /**
     * The HP each side's data box shows while a round's events play (-1 = the
     * battler's live HP). The engine has already run the whole round, so the
     * bars follow the events ({@code animateHP(oldHP,newHP)}) instead.
     */
    private final int[] heldHp = { -1, -1, -1, -1, -1, -1 };
    /**
     * The player's exp bar fill while the round plays: the engine applies the
     * exp when the foe faints / is captured, but the bar only moves in the exp
     * stage ({@code pbGainExp}, after the round's animations). -1 = follow the
     * Pokemon.
     */
    private float heldExp = -1f;
    /** The battler index whose exp bar the exp stage fills (the receiving Pokemon's position; 0 in a single battle). */
    private int expSlot;

    private static final class Message {
        final String text;
        final boolean paused;
        /** pbDisplayBrief (PokeBattle_Scene:130-134): it lingers once fully shown. */
        final boolean brief;
        /** A non-text round event (HIT / HP_CHANGE / FAINT / BGM), or null. */
        final Battle.RoundEvent event;
        /** The statuses to show once this message / event starts (null = leave them). */
        String[] statuses;
        Message(String text, boolean paused) { this(text, paused, false, null); }
        Message(String text, boolean paused, boolean brief, Battle.RoundEvent event) {
            this.text = text; this.paused = paused; this.brief = brief; this.event = event;
        }
    }

    // PokemonDataBox#animateHP / #animateExp.
    private static final float HP_BAR_CHANGE_TIME = 1.0f;
    private static final float EXP_BAR_FILL_TIME = 1.75f;
    // One entry per battler index (the plugin's data boxes: {@code dataBox_i}).
    /** The status each data box shows during a round's playback ("POISON!" = badly poisoned), see {@link #statusHeld}. */
    private final String[] shownStatus = { "", "", "", "", "", "" };
    /** True while the boxes follow {@link #shownStatus} instead of the engine's already final statuses. */
    private boolean statusHeld;
    private final float[] hpShown = { 1f, 1f, 1f, 1f, 1f, 1f };
    private final float[] hpFrom = { 1f, 1f, 1f, 1f, 1f, 1f };
    private final float[] hpTo = { 1f, 1f, 1f, 1f, 1f, 1f };
    private final float[] hpT = { 1f, 1f, 1f, 1f, 1f, 1f };
    private final float[] expShown = { 1f, 1f, 1f, 1f, 1f, 1f };
    private final float[] expFrom = { 1f, 1f, 1f, 1f, 1f, 1f };
    private final float[] expTo = { 1f, 1f, 1f, 1f, 1f, 1f };
    private final float[] expT = { 1f, 1f, 1f, 1f, 1f, 1f };
    private boolean barsReady;

    // ---------------------------------------------------------------------
    // Battle intro / send-out (Scene_Initialize:15 pbStartBattle,
    // Scene_Animations:5 pbBattleIntroAnimation / :85 pbSendOutBattlers,
    // Battle_StartAndEnd:194 pbStartBattleSendOut)
    // ---------------------------------------------------------------------
    /** One game frame at the project's 40 fps (pbUpdate). */
    private static final float TICK = 1f / 40f;
    private enum Stage {
        INTRO, OPENING, BATTLE, EXP_GAIN, TRAINER_END,
        /** The full Mega Evolution scene (Mega evolution:9-343). */
        MEGA_SCENE,
        /**
         * Scene_Animations:239-268 pbHitAndHPLossAnimation / :224-234
         * pbDamageAnimation: the battler flashes and its data box's HP bar runs.
         * Move_Usage:280 plays it inside the move, i.e. before pbFaint
         * (Battler_ChangeSelf:73).
         */
        HIT,
        /** Scene_Animations:299-312 pbFaintBattler: the cry, the drop, the data box. */
        FAINT,
        /** Scene_Animations:211-222 pbHPChanged: a data box's HP bar runs (no flash). */
        HP_CHANGE,
        /**
         * Scene_Animations:510-586 pbAnimation / pbCommonAnimation / pbAnimationCore:
         * a {@code PBAnimationPlayerX} plays a move or common animation.
         */
        MOVE_ANIM,
        /**
         * Scene_Animations:348-399 pbThrow / pbThrowAndDeflect / pbThrowSuccess /
         * pbHideCaptureBall: the Poké Ball thrown at the foe, and the capture's end.
         */
        BALL,
        /** PokeBattle_BattleCommon:43-63 pbRecordAndStoreCaughtPokemon: the Pokedex lines and entry page, the storing lines. */
        CAUGHT_STORE,
        /** PokeBattle_Scene:296-303 pbEndBattle's fade before the scene is dropped. */
        END_BATTLE,
        /** Scene_Animations:171-198 pbShowAbilitySplash / pbHideAbilitySplash: the ability bar slides in or out. */
        ABILITY_SPLASH
    }
    private Stage stage = Stage.INTRO;
    /** Scene_Animations:5-53: 0 = BattleIntroAnimation, 1 = its post-appearance work. */
    private int introPhase;

    /**
     * {@code pbFadeOutAndHide} (MessageConfig:602-619): {@code numFrames =
     * (40*0.4).floor} and {@code alphaDiff = (255.0/numFrames).ceil}, then
     * {@code for j in 0..numFrames} sets every sprite's colour to
     * {@code Color.new(0,0,0,j*alphaDiff)} before hiding them all (:612-617).
     */
    private static final int END_FADE_FRAMES = (int) Math.floor(40 * 0.4);
    private static final int END_FADE_ALPHA_STEP = (int) Math.ceil(255.0 / END_FADE_FRAMES);
    /** {@code pbFadeOutAndHide}'s loop counter j, or -1 when the battle is not ending. */
    private int endBattleFadeFrame = -1;

    /**
     * {@code pbShowWindow} (PokeBattle_Scene:85-94): the runtime draws the
     * windows itself, so this only records which one is up.
     */
    private void pbShowWindow(int windowType) {
        window = windowType;
    }

    /**
     * Battle_StartAndEnd:194-275 pbStartBattleSendOut, transcribed into a
     * straight-line list of pbDisplay/pbSendOut calls (see {@link BattleSendOut}).
     */
    private List<BattleSendOut.Step> plan = new ArrayList<>();
    private int planIndex;
    /** The pbSendOut call currently animating (Scene_Animations:85-143). */
    private BattleAnimations.SendOutSequence sendOut;

    /**
     * Battle_ExpAndMoveLearning:99-308 pbGainExpOne, played out as
     * {@link Stage#EXP_GAIN}: message -> one bar segment per level -> level-up
     * message -> {@code pbLevelUp}'s stat window.
     */
    private enum ExpPhase {
        MESSAGE, SEGMENTS, LEVEL_MESSAGE,
        /** Scene_Animations:287-291 pbLevelUp's first window (the gains). */
        LEVEL_GAIN_WINDOW,
        /** Scene_Animations:291-293 pbLevelUp's second window (the totals). */
        LEVEL_TOTAL_WINDOW,
        /** Battle_ExpAndMoveLearning:293-294 "要{1}立即学习招式吗？". */
        LEARN_QUESTION,
        LEARN_MOVE, DONE
    }
    private ExpPhase expPhase = ExpPhase.MESSAGE;
    /** pbLearnMove's four-move dialogue: 0 start, 1 ask, 2-6 the lines and screens, -1 waiting for an answer. */
    private int learnStep;
    private PbsData.Move learnMove;
    private int forgetSlot = -1;
    private String oldMoveName = "";
    private SummaryView forgetView;
    private int expAwardIndex;
    private int expSegmentIndex;
    private int expMoveIndex;
    private float expAnimFrom, expAnimTo, expAnimT = 1f;
    /** updateExpAnimation:354-365: the 8-frame level-up flash of the data box. */
    private static final float EXP_FLASH_TIME = 8f * TICK;
    private float expFlash;
    /**
     * The fill always plays at least this long. The plugin's fill time is
     * proportional to the exp gained (PokeBattle_SceneElements:195), so a tiny
     * gain would finish in one frame and cut the 2.68s "Pkmn exp gain" sound
     * into a click; a user-requested minimum keeps it audible.
     */
    private static final float MIN_EXP_SEGMENT_SECONDS = 0.5f;
    private final int[] levelUpStats = new int[6];
    /**
     * Scene_Animations:287-293 pbLevelUp's two windows wait for a confirm each
     * ({@code pbTopRightWindow}, PItem_Items:547-562, loops until
     * {@code Input.trigger?(Input::C)}); this holds which one is on screen.
     */
    private int levelUpWindow;

    /**
     * Battle_StartAndEnd:444-473 pbEndOfBattle's WIN branch: the victory line,
     * the opponent sliding back in (Scene_Animations:272-297 TrainerAppear-
     * Animation), its LoseText and the prize money, then pbEndBattle's fade.
     */
    private enum EndPhase { WIN_MESSAGE, APPEAR, LOSE_MESSAGE, MONEY_MESSAGE, DONE }
    private EndPhase endPhase = EndPhase.WIN_MESSAGE;
    private static final float END_APPEAR_SECONDS = 8f * TICK;
    private float endAppearTimer;
    private boolean endAppearStarted;
    /** {@code @opponent.each_with_index}'s i (Battle_StartAndEnd:464-468). */
    private int endOpponent;
    private final boolean trainerBattle;
    private final int foeBallType;
    private final int playerBallType;

    // ---------------------------------------------------------------------
    // Battle_Action_Switching:165-332 - switching and replacing battlers
    // ---------------------------------------------------------------------

    /**
     * {@code pbRecallAndReplace} (Battle_Action_Switching:256-262): every step
     * of the Ruby blocks on an animation or a message, so it is played one step
     * per update.
     */
    private enum SwitchStep {
        /** :257 {@code @scene.pbRecall(idxBattler) if !fainted?} - two animations. */
        RECALL, RECALL_BOX,
        /** :259 {@code pbShowPartyLineup(idxBattler&1) if pbSideSize==1}. */
        LINEUP,
        /** :260 {@code pbMessagesOnReplace}. */
        MESSAGE,
        /** :261 {@code pbReplace} -> :318 {@code pbSendOut}. */
        REPLACE, SEND,
        /** Nothing switching. */
        NONE
    }

    /** One {@code pbRecallAndReplace} with the line its caller prints first (:57 / :197). */
    private static final class SwitchRequest {
        final int idxBattler;
        final int idxParty;
        final boolean recallMessage;
        final Runnable done;
        SwitchRequest(int idxBattler, int idxParty, boolean recallMessage, Runnable done) {
            this.idxBattler = idxBattler;
            this.idxParty = idxParty;
            this.recallMessage = recallMessage;
            this.done = done;
        }
    }

    private SwitchStep switchStep = SwitchStep.NONE;
    /** The switches still to play, in the order the Ruby calls them. */
    private final java.util.ArrayDeque<SwitchRequest> switchQueue = new java.util.ArrayDeque<>();
    private SwitchRequest switchCurrent;
    private int switchIdxBattler = -1;
    private int switchIdxParty = -1;
    /** Which of {@code pbRecall}'s two animations (Scene_Animations:148-166) is running. */
    private int recallPhase;

    /**
     * The engine's scene call that is being played as a single step of {@code pbRecallAndReplace}
     * (Battle_Action_Switching:257/259/326): RECALL, SHOW_PARTY_LINEUP or SEND_OUT; null otherwise.
     */
    private Battle.SceneCall.Kind requestKind;
    /** The engine's {@code pbPartyScreen} block (Battle_Action_Switching:138-149) while its party screen is open. */
    private java.util.function.IntFunction<String> partyValidator;
    /** The party screen's answer went to the engine; the update must not return to the command menu. */
    private boolean partyHandedOver;

    /**
     * {@code pbShowCommands} (PokeBattle_Scene:202-237): the message window stays
     * up while a two-entry command window takes the input. Non-null while it is
     * waiting for the player.
     */
    private String[] choiceOptions;
    private int choiceDefault = -1;
    private final MenuListModel choiceCursor = new MenuListModel(2);
    private java.util.function.IntConsumer choiceResult;
    /**
     * {@code pbPartyScreen}'s flags (Battle_Action_Switching:136-151) for the
     * screen's own {@code pbPartyScreen} call: checkLaxOnly / canCancel /
     * shouldRegister.
     */
    private boolean partyCheckLaxOnly;
    private boolean partyCanCancel = true;
    private boolean partyRegister;

    public BattleScreen(RuntimeContext context, InteractiveBattlePort port) {
        this.context = context; this.port = port; session = port.session();
        trainerBattle = session.trainerBattle;
        Battler player = session.battle.player();
        Battler foe = session.battle.foe();
        playerBallType = player == null || player.pokemon == null ? 0 : Math.max(0, player.pokemon.ballused);
        foeBallType = foe == null || foe.pokemon == null ? 0 : Math.max(0, foe.pokemon.ballused);
        GraphicsLocator locator = new GraphicsLocator(context.database()); assets = new MenuAssets(locator);
        String fontName = context.database().project().runtime.messageFont;
        File file = fontName == null ? null : locator.font(fontName);
        font = new MenuFont(file);
        narrowFont = new MenuFont(file, 23);   // pbSetNarrowFont: fight menu names / PP
        smallFont = new MenuFont(file, 20);    // pbSetSmallFont
        skin = assets.skin("choice 1");
        camera = new OrthographicCamera(); viewport = new FitViewport(ScreenMetrics.logicalWidth(), ScreenMetrics.logicalHeight(), camera);
        viewport.update(com.badlogic.gdx.Gdx.graphics.getWidth(), com.badlogic.gdx.Gdx.graphics.getHeight(), true);
        spriteShader = new BattleSpriteShader();
        // pbStartBattle (Scene_Initialize:15) = pbInitSprites + pbBattleIntroAnimation
        // + pbShowExtraInfo; the "wants to battle" line and the send-outs follow
        // (Battle_StartAndEnd:194).
        pbInitSprites();
        animations.add(new BattleAnimations.BattleIntroAnimation(this));
        String[] opponentNames = new String[opponentCount()];
        for (int i = 0; i < opponentNames.length; i++) {
            opponentNames[i] = session.trainerFullname(i);                                   // @opponent[i].fullname
        }
        String[] playerNames = new String[playerCount()];
        for (int i = 1; i < playerNames.length; i++) {
            playerNames[i] = session.partnerFullname;                                         // @player[i].fullname
        }
        plan = session.safari ? BattleSendOut.safariPlan(session.safariAppearMessage())
                : BattleSendOut.plan(session.battle, trainerBattle, opponentNames, playerNames,
                        id -> context.gameState().switches().get(id));
        go(0);
    }

    public boolean finished() { return finished; }

    // ---------------------------------------------------------------------
    // Probe hooks: the lwjgl3 capture tool samples the send-out, the command
    // menu, the switch lines, the exp window and the end fade by state rather
    // than by guessing render frames (the animations run at 40 fps while the
    // capture renders at 60).
    // ---------------------------------------------------------------------

    /** The current {@code Stage} name. */
    public String debugStage() { return stage.name(); }

    /** PokeBattle_Scene's window constant (0 BLANK / 1 MESSAGE / 2 COMMAND / 3 FIGHT). */
    public int debugWindow() { return window; }

    /** True while {@code pbSendOutBattlers} (Scene_Animations:85-143) is running. */
    public boolean debugSendingOut() { return sendOut != null; }

    /** The message on screen, or null. */
    public String debugMessage() { return message == null ? null : message.text; }

    /** Whether the current message has finished revealing its characters. */
    public boolean debugMessageComplete() {
        return message != null && revealedChars >= message.text.length();
    }

    /** The command menu's selected row (0..3). */
    public int debugCursor() { return cursor.index(); }

    /** The battler whose command is being chosen in a double battle (-1 outside the command phase). */
    public int debugCommandBattler() { return cmdBattler; }

    /** The target menu's selected battler index, or -1 when it is not up. */
    public int debugTarget() { return page == PAGE_TARGET ? targetIndex : -1; }

    /** The fielded battlers, for the probe ("name/hp@slot vs name/hp@slot"). */
    public String debugBattlers() {
        Battler player = session.battle.player();
        Battler foe = session.battle.foe();
        return (player == null || player.pokemon == null ? "-"
                    : player.name() + "/" + player.hp + "@" + session.battle.playerFieldIndex())
                + " vs "
                + (foe == null || foe.pokemon == null ? "-"
                    : foe.name() + "/" + foe.hp + "@" + session.battle.foeFieldIndex());
    }

    /** The {@code pbShowCommands} entry under the cursor, or null when no question is up. */
    public String debugChoice() {
        return choiceOptions == null ? null : choiceOptions[choiceCursor.index()];
    }

    /** {@code pbEORSwitch}'s current phase (Battle_Action_Switching:165-239). */
    public String debugEor() {
        return session.suspended() ? "ENGINE:" + session.request().kind : (partyScreenOpen ? "PARTY" : "none");
    }

    /** {@code pbFadeOutAndHide}'s frame counter, for the probe (-1 when idle). */
    public int debugEndFadeFrame() { return endBattleFadeFrame; }

    /** {@code pbRecallAndReplace}'s current step (Battle_Action_Switching:256-262). */
    public String debugSwitch() {
        return switchStep == SwitchStep.NONE ? "none" : switchStep.name() + ":" + switchIdxBattler;
    }

    /** The command/fight page (0 command, 1 fight, 2 bag, 3 party). */
    public int debugPage() { return page; }

    /** Dumps the sprite table (probe diagnostic). */
    public String debugSprites() {
        StringBuilder out = new StringBuilder();
        for (BattleSprite sprite : sprites.drawOrder()) {
            out.append(sprite.key).append(" kind=").append(sprite.kind)
               .append(" xy=").append(sprite.x).append(',').append(sprite.y)
               .append(" z=").append(sprite.z)
               .append(" vis=").append(sprite.visible)
               .append(" op=").append(sprite.opacity)
               .append(" zoom=").append(sprite.zoomX).append(',').append(sprite.zoomY)
               .append(" oxy=").append(sprite.ox).append(',').append(sprite.oy)
               .append(" name=").append(sprite.name)
               .append(" tex=").append(spriteTexture(sprite) != null)
               .append('\n');
        }
        return out.toString();
    }

    // =====================================================================
    // pbInitSprites (Scene_Initialize:107-174)
    // =====================================================================

    /** {@code Scene_Initialize:107-174}. */
    private void pbInitSprites() {
        // The background image and each side's base graphic
        pbCreateBackdropSprites();                               // :110
        // Create message box graphic (:112-114) - the runtime draws the message
        // window itself, but the intro's black_bar needs cmdBar_bg (:231-232),
        // which pbCreateBackdropSprites creates.
        // Create command/fight/target windows (:125-129) - drawn by the renderer.
        // The party lineup graphics (bar and balls) for both sides (:131-147)
        for (int side = 0; side < 2; side++) {
            BattleSprite partyBar = pbAddSprite("partyBar_" + side, -PictureEx.Graphics.WIDTH, 0,
                    "Graphics/Pictures/Battle/overlay_lineup");                // :133-134
            partyBar.z = 120;                                                   // :135
            partyBar.mirror = side == 1;                                        // :136 Player's lineup bar only
            partyBar.visible = false;                                           // :137
            for (int i = 0; i < PokeBattle_SceneConstants.NUM_BALLS; i++) {      // :138
                BattleSprite ball = pbAddSprite("partyBall_" + side + "_" + i,
                        -PictureEx.Graphics.WIDTH, 0, null);                    // :139
                ball.z = 121;                                                   // :140
                ball.visible = false;                                           // :141
            }
            // Ability splash bars (:143-145)
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {
                BattleSprite bar = sprites.add("abilityBar_" + side, BattleSprite.Kind.ABILITY_BAR);
                bar.name = "Graphics/Pictures/Battle/ability_bar";
                int[] barSize = bitmapSize(bar.name);
                bar.bitmapWidth = barSize == null ? -1 : barSize[0];
                bar.bitmapHeight = barSize == null ? -1 : barSize[1];
                bar.barSide = side;
                bar.srcY = side == 0 ? 0 : (barSize == null ? 0 : barSize[1] / 2);   // AbilitySplashBar#initialize
                bar.srcHeight = barSize == null ? -1 : barSize[1] / 2;
                bar.x = side == 0 ? -PictureEx.Graphics.WIDTH / 2f : PictureEx.Graphics.WIDTH;
                bar.y = 220 + 16;
                bar.z = 120;
                bar.visible = false;
            }
        }
        // Player's and partner trainer's back sprite (:148-151)
        for (int i = 0; i < playerCount(); i++) {
            pbCreateTrainerBackSprite(i, i == 0 ? playerTrainerType() : session.partnerTrainerType,
                    playerCount());                                             // :150
        }
        // Opposing trainer(s) sprites (:152-157)
        if (trainerBattle) {                                                    // :153
            for (int i = 0; i < opponentCount(); i++) {
                PbsData.TrainerData opponent = session.trainerData(i);
                pbCreateTrainerFrontSprite(i, opponent == null ? null : opponent.type, opponentCount());   // :155
            }
        }
        // Data boxes and Pokemon sprites (:158-163)
        for (int i = 0; i < battlerCount(); i++) {                               // :159
            Battler battler = battler(i);
            if (battler == null) {
                continue;                                                       // :160
            }
            BattleSprite box = sprites.add("dataBox_" + i, BattleSprite.Kind.DATA_BOX);   // :161
            box.battler = battler;
            initializeDataBoxGraphic(box, battler, sideSize(i));
            box.visible = false;                                                // :99 self.visible = false
            box.z = 150 + (i / 2) * 5;                                          // :100
            pbCreatePokemonSprite(i);                                           // :162
        }
        // Wild battle, so set up the Pokemon sprite(s) accordingly (:164-173)
        if (!trainerBattle) {                                                   // :165
            com.badlogic.gdx.utils.Array<Battler> wild = party(1);
            for (int i = 0; i < wild.size; i++) {                                // :166
                int index = i * 2 + 1;                                          // :167
                pbChangePokemon(index, wild.get(i));                            // :168
                BattleSprite sprite = sprites.get("pokemon_" + index);          // :169
                sprite.tone = new float[] { -80, -80, -80, 0 };                 // :170
                sprite.visible = true;                                          // :171
            }
        }
    }

    /** {@code PokemonDataBox#initializeDataBoxGraphic} (PokeBattle_SceneElements:43-76). */
    private void initializeDataBoxGraphic(BattleSprite box, Battler battler, int sideSize) {
        boolean onPlayerSide = (battler.index % 2) == 0;                        // :44
        String bg = sideSize == 1
                ? (onPlayerSide ? "Graphics/Pictures/Battle/databox_normal"
                                : "Graphics/Pictures/Battle/databox_normal_foe")
                : (onPlayerSide ? "Graphics/Pictures/Battle/databox_thin"
                                : "Graphics/Pictures/Battle/databox_thin_foe");  // :46-56
        box.name = bg;                                                          // :57
        int[] size = bitmapSize(bg);
        box.bitmapWidth = size == null ? -1 : size[0];
        box.bitmapHeight = size == null ? -1 : size[1];
        if (onPlayerSide) {                                                     // :59
            box.x = 0;                                                          // :60
            box.y = 5;                                                          // :61
        } else {
            box.x = PictureEx.Graphics.WIDTH - 232;                             // :64
            box.y = 5;                                                          // :65
        }
        switch (sideSize) {                                                     // :68
            case 2:
                box.x += new int[] { -8, 0, 0, -8 }[battler.index];              // :70
                box.y += new int[] { -8, -8, 40, 40 }[battler.index];            // :71
                break;
            case 3:
                box.x += new int[] { -8, 0, -4, -4, 0, -8 }[battler.index];      // :73
                box.y += new int[] { -14, -14, 20, 20, 54, 54 }[battler.index];  // :74
                break;
            default:
                break;
        }
        box.homeX = box.x;
    }

    /** {@code pbAddSprite} (PokeBattle_Scene:243-250). */
    private BattleSprite pbAddSprite(String id, float x, float y, String filename) {
        return pbAddSprite(id, x, y, filename, BattleSprite.Kind.IMAGE);
    }

    private BattleSprite pbAddSprite(String id, float x, float y, String filename,
            BattleSprite.Kind kind) {
        BattleSprite sprite = sprites.add(id, kind);
        sprite.x = x;
        sprite.y = y;
        if (filename != null) {
            sprite.name = filename;
            int[] size = bitmapSize(filename);
            sprite.bitmapWidth = size == null ? -1 : size[0];
            sprite.bitmapHeight = size == null ? -1 : size[1];
        }
        return sprite;
    }

    /** {@code @battle.backdrop} (PField_Battles:150): the map's MetadataBattleBack. */
    private String backdropName() {
        int mapId = context.gameState().currentMapId();
        pokemon.runtime.data.MapData map =
                context.database() == null || mapId < 0 ? null : context.database().map(mapId);
        String name = map == null ? null : map.battleBack;
        return name == null || name.isEmpty() ? "cave1" : name;
    }

    /** {@code pbCreateBackdropSprites} (Scene_Initialize:176-233). */
    private void pbCreateBackdropSprites() {
        String backdrop = backdropName();
        // Finalise filenames (:211-214)
        String battleBG = "Graphics/Battlebacks/" + backdrop + "_bg";
        String baseFile = backdrop;                              // :183/200-209 (base1 for both sides, :212-213)
        String playerBase = "Graphics/Battlebacks/" + baseFile + "_base1";
        String messageBG = "Graphics/Battlebacks/" + backdrop + "_message";
        // Apply graphics
        BattleSprite bg = pbAddSprite("battle_bg", 0, 0, battleBG);              // :216
        bg.z = 0;                                                               // :217
        BattleSprite bg2 = pbAddSprite("battle_bg2", -PictureEx.Graphics.WIDTH, 0, battleBG);   // :218
        bg2.z = 0;                                                              // :219
        bg2.mirror = true;                                                      // :220
        for (int side = 0; side < 2; side++) {                                  // :221
            float[] pos = PokeBattle_SceneConstants.battlerPosition(side, 1);    // :222
            BattleSprite base = pbAddSprite("base_" + side, pos[0],
                    side == 0 ? pos[1] + 60 : pos[1],
                    side == 0 ? playerBase : playerBase);                        // :223-224
            base.z = 1;                                                          // :225
            // :226-229 base.ox = width/2; base.oy = side==0 ? height : height/2
            base.origin = side == 0 ? PictureEx.Origin.BOTTOM : PictureEx.Origin.CENTER;
            base.updateOrigin();
        }
        BattleSprite cmdBar = pbAddSprite("cmdBar_bg", 0, BAR_Y, messageBG);     // :231
        cmdBar.z = 180;                                                         // :232
    }

    /**
     * {@code pbCreateTrainerBackSprite} (Scene_Initialize:235-251), called from
     * {@code :150} for each trainer on the player's side.
     */
    private void pbCreateTrainerBackSprite(int idxTrainer, String trainerType, int numTrainers) {
        String trainerFile = pbTrainerSpriteFile(trainerType);                   // :236
        float[] pos = PokeBattle_SceneConstants.trainerPosition(0, idxTrainer, numTrainers);   // :237
        float spriteX = pos[0];
        float spriteY = pos[1] + 20;                                             // :238
        BattleSprite trainer = pbAddSprite("player_" + (idxTrainer + 1), spriteX, spriteY,
                trainerFile, BattleSprite.Kind.TRAINER_BACK);                   // :240
        trainer.idxTrainer = idxTrainer + 1;
        trainer.trainerFile = trainerFile;
        trainer.origin = PictureEx.Origin.BOTTOM;                                // pbSetOrigin (bottom middle)
        trainer.mirror = true;                                                   // :241
        int[] size = bitmapSize(trainerFile);
        if (size == null) {                                                      // :242 return if !trainer.bitmap
            return;
        }
        trainer.bitmapWidth = size[0];
        trainer.bitmapHeight = size[1];
        // Alter position of sprite (:243-250)
        trainer.z = 30 + idxTrainer;                                             // :244
        if (size[0] > size[1] * 2) {                                             // :245
            trainer.srcX = 0;                                                    // :246
            trainer.srcWidth = size[0] / 5;                                      // :247
        }
        trainer.updateOrigin();                                                  // :249-250
    }

    /** {@code pbCreateTrainerFrontSprite} (Scene_Initialize:253-262). */
    private void pbCreateTrainerFrontSprite(int idxTrainer, String trainerType, int numTrainers) {
        String trainerFile = pbTrainerSpriteFile(trainerType);                   // :254
        float[] pos = PokeBattle_SceneConstants.trainerPosition(1, idxTrainer, numTrainers);   // :255
        BattleSprite trainer = pbAddSprite("trainer_" + (idxTrainer + 1), pos[0], pos[1],
                trainerFile, BattleSprite.Kind.TRAINER_FRONT);                   // :256
        trainer.idxTrainer = idxTrainer + 1;
        trainer.trainerFile = trainerFile;
        int[] size = bitmapSize(trainerFile);
        if (size == null) {                                                      // :257 return if !trainer.bitmap
            return;
        }
        trainer.bitmapWidth = size[0];
        trainer.bitmapHeight = size[1];
        trainer.z = 7 + idxTrainer;                                              // :259
        trainer.origin = PictureEx.Origin.BOTTOM;                                // :260-261 ox=src_rect.width/2, oy=bitmap.height
        trainer.updateOrigin();
    }

    /** {@code pbCreatePokemonSprite} (Scene_Initialize:264-272). */
    private void pbCreatePokemonSprite(int idxBattler) {
        int sideSize = sideSize(idxBattler);                                     // :265
        BattleSprite batSprite = sprites.add("pokemon_" + idxBattler, BattleSprite.Kind.POKEMON);   // :266
        batSprite.battler = battler(idxBattler);
        batSprite.back = !opposes(idxBattler);
        batSprite.origin = PictureEx.Origin.BOTTOM;                              // pbSetOrigin:562-566
        batSprite.visible = false;                                               // PokemonBattlerSprite:530
        BattleSprite shaSprite = sprites.add("shadow_" + idxBattler, BattleSprite.Kind.SHADOW);     // :269
        shaSprite.battler = battler(idxBattler);
        shaSprite.visible = false;                                               // :270
        shaSprite.z = 3;                                                         // PokemonBattlerShadowSprite:672
        shaSprite.origin = PictureEx.Origin.CENTER;                              // :665-666
        shaSprite.updateOrigin();
        if (batSprite.battler != null) {
            pbSetPokemonSpritePosition(batSprite, sideSize);
        }
    }

    /**
     * {@code PokemonBattlerShadowSprite#setPokemonBitmap / #pbSetPosition}
     * (PokeBattle_SceneElements:639-688): the species' shadow picture
     * ({@code pbCheckPokemonShadowBitmapFiles} falls back to
     * {@code battler_shadow_<MetricBattlerShadowSize or 2>}), centred on the
     * battler's base position; a foe's x also takes {@code MetricBattlerShadowX*2}
     * (Pokemon_Sprites:352-355).
     */
    private void pbSetShadowPosition(BattleSprite shadow, int idxBattler, int sideSize) {
        Battler battler = shadow.battler;
        PbsData.BattlerOffsets m = null;
        if (battler != null && battler.pokemon != null && battler.pokemon.species != null) {
            m = battler.pokemon.form != null && battler.pokemon.form.battler != null
                    ? battler.pokemon.form.battler : battler.pokemon.species.battler;
        }
        int size = m != null && m.shadowSize > 0 ? m.shadowSize : 2;            // :147 || 2
        shadow.name = "Graphics/Pictures/Battle/battler_shadow_" + size;         // :148
        int[] bmp = bitmapSize(shadow.name);
        shadow.bitmapWidth = bmp == null ? -1 : bmp[0];
        shadow.bitmapHeight = bmp == null ? -1 : bmp[1];
        shadow.origin = PictureEx.Origin.CENTER;                                 // :666-668 pbSetOrigin
        shadow.z = 3;                                                            // :673
        float[] pos = PokeBattle_SceneConstants.battlerPosition(idxBattler, sideSize);   // :675
        shadow.x = pos[0];
        shadow.y = pos[1];
        if (m != null && (idxBattler & 1) == 1) {                                // Pokemon_Sprites:353-355
            shadow.x += m.shadowX * 2;
        }
        shadow.updateOrigin();
    }

    /** {@code PokemonBattlerSprite#pbSetPosition} (PokeBattle_SceneElements:568-582). */
    private void pbSetPokemonSpritePosition(BattleSprite sprite, int sideSize) {
        int index = sprite.battler == null ? 0 : sprite.battler.index;           // @index
        sprite.origin = PictureEx.Origin.BOTTOM;                                 // :570 pbSetOrigin
        sprite.z = index < 2 ? 50 + 5 * index / 2 : 50 - 5 * (index + 1) / 2;    // :571-575
        float[] pos = PokeBattle_SceneConstants.battlerPosition(index, sideSize);   // :577
        sprite.x = pos[0];                                                       // :578
        sprite.y = pos[1];                                                       // :579
        // :581 pbApplyBattlerMetricsToSprite(self,@index,@pkmn.fSpecies)
        float[] metrics = battlerMetrics(sprite.battler, !opposes(index));
        sprite.x += metrics[0];
        sprite.y += metrics[1];
        sprite.updateOrigin();
    }

    // =====================================================================
    // BattleAnimation.Scene
    // =====================================================================

    @Override public BattleSprites sprites() { return sprites; }

    @Override public PictureEx.SePlayer sePlayer() {
        return (name, volume, pitch) -> {
            if (context.audioManager() != null) {
                context.audioManager().playSe(name, volume, pitch);
            }
        };
    }

    @Override public BattleAnimation.BitmapSize bitmapSize() { return this::bitmapSize; }

    @Override public boolean trainerBattle() { return trainerBattle; }

    /** {@code @battle.player.length}: the player, and the partner trainer in a battle with one. */
    @Override public int playerCount() { return Math.max(1, session.battle.pbTrainerCount(0)); }

    /** {@code @battle.opponent.length}: one or two opposing trainers in a trainer battle. */
    @Override public int opponentCount() {
        return trainerBattle && session.trainerData != null ? Math.max(1, session.battle.pbTrainerCount(1)) : 0;
    }

    /** {@code @battle.pbParty(side)} (PokeBattle_Battle:307). */
    @Override public com.badlogic.gdx.utils.Array<Battler> party(int side) {
        return side == 0 ? session.battle.playerParty() : session.battle.foeParty();
    }

    /** {@code @battle.pbPartyStarts(side)} (PokeBattle_Battle:319): where each trainer's team starts. */
    @Override public int[] partyStarts(int side) {
        return session.battle.partyStarts(side);
    }

    /** {@code @battle.battlers.length}: 2 in a single battle, 4 in a double one. */
    @Override public int battlerCount() { return session.battle.maxBattlerIndex() + 1; }

    /** {@code @battle.battlers[i]}. */
    @Override public Battler battler(int index) { return BattleSendOut.battler(session.battle, index); }

    /** {@code @battle.opposes?(idxBattler)} (PokeBattle_Battle:538-542). */
    @Override public boolean opposes(int idxBattler) { return (idxBattler & 1) != 0; }

    /** {@code @battle.pbGetOwnerIndexFromBattlerIndex} (PokeBattle_Battle:232-243). */
    @Override public int ownerIndex(int idxBattler) {
        return session.battle.pbGetOwnerIndexFromBattlerIndex(idxBattler);
    }

    /** {@code @battle.pbSideSize(idxBattler)} (PokeBattle_Battle:215-217). */
    @Override public int sideSize(int idxBattler) { return session.battle.pbSideSize(idxBattler); }

    /** {@code $Trainer.trainertype}, for pbTrainerSpriteFile. */
    @Override public String playerTrainerType() {
        if (context.database() == null || context.database().project() == null) {
            return null;
        }
        pokemon.runtime.data.ProjectInfo.PlayerGraphic graphic =
                context.database().project().runtime.player(context.gameState().playerId());
        return graphic == null ? null : graphic.trainerType;
    }

    /**
     * {@code refresh_sprite(false,true)} (Follower_Main:174-193): the follower walks in from the side instead of leaving
     * a Poke Ball. 登记: {@code isEncounterPossibleHere?} needs the player's tile, which the battle does not know.
     */
    @Override public boolean followerSprite() {
        pokemon.runtime.state.GameState state = context.gameState();
        boolean has = false;
        for (pokemon.runtime.state.Dependent entry : state.fieldGlobals().dependents) {
            has |= entry.isFollower();
        }
        Pokemon first = state.trainer().party.firstAble();
        if (!has || !state.followerToggled() || first == null) {
            return false;                                           // :175-178
        }
        int mapId = state.currentMapId();
        pokemon.runtime.data.MapData map =
                context.database() == null || mapId < 0 ? null : context.database().map(mapId);
        pokemon.runtime.field.FollowerRules.Context rules = new pokemon.runtime.field.FollowerRules.Context();
        rules.bicycle = state.fieldGlobals().bicycle;
        rules.surfing = state.fieldGlobals().surfing;
        rules.diving = state.fieldGlobals().diving;
        rules.outdoor = map != null && Boolean.TRUE.equals(map.outdoor);
        rules.mapName = map == null || map.name == null ? "" : map.name;
        Boolean refresh = pokemon.runtime.field.FollowerRules.refresh(first, rules);   // :179
        return refresh == null || refresh;                           // :180 -1 counts as true with check
    }

    /** {@code pbCryFile} (PSystem_FileUtilities:509-539). */
    @Override public String cryFile(Pokemon pokemon) {
        if (pokemon == null || pokemon.species == null || pokemon.egg) {          // :510/524
            return null;
        }
        if (context.audioManager() == null) {
            return null;
        }
        int form = pokemon.form == null ? 0 : pokemon.form.form;                // :525
        return context.audioManager().resolveCryFile(pokemon.species.id,
                pokemon.species.internalName, form);
    }

    /**
     * {@code pbCryFrameLength} (PSystem_FileUtilities:448-469): the cry's length
     * in 40 fps frames. {@code playtime/pitch}, then {@code (playtime*40).ceil+4}.
     */
    @Override public int cryFrameLength(Pokemon pokemon, int pitch) {
        if (pokemon == null) {                                                  // :449
            return 0;
        }
        float rate = (pitch <= 0 ? 100 : pitch) / 100f;                          // :450-451
        if (rate <= 0f) {                                                       // :452
            return 0;
        }
        // :457-465 a Pokemon's cry; the project has no Chatter.
        String cry = cryFile(pokemon);
        float playtime = cry == null || context.audioManager() == null
                ? 0f : context.audioManager().sePlayTime(cry);
        if (playtime < 0f) {
            playtime = 0f;
        }
        playtime /= rate;                                                       // :466
        return (int) Math.ceil(playtime * 40.0) + 4;                            // :468
    }

    /** {@code pbTrainerSpriteFile} (PSystem_FileUtilities:359-367). */
    private String pbTrainerSpriteFile(String trainerType) {
        if (trainerType == null) {
            return null;                                                        // :360
        }
        PbsData pbs = context.pbsData();
        PbsData.TrainerType type = pbs == null ? null : pbs.trainerTypes.get(trainerType);
        int id = type == null ? -1 : type.id;
        if (graphicExists("Trainers", "trainer" + trainerType)) {                // :361-363
            return "Graphics/Trainers/trainer" + trainerType;
        }
        if (id >= 0) {                                                          // :364
            return String.format(java.util.Locale.ROOT, "Graphics/Trainers/trainer%03d", id);
        }
        return null;
    }

    private boolean graphicExists(String directory, String name) {
        return assets.graphic(directory, name) != null;
    }

    /** Resolves a {@code Graphics/...} path to {width,height}, or null. */
    private int[] bitmapSize(String name) {
        Texture texture = texture(name);
        return texture == null ? null : new int[] { texture.getWidth(), texture.getHeight() };
    }

    /** Loads the texture behind a plugin {@code Graphics/...} path. */
    private Texture texture(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String path = name.startsWith("Graphics/") ? name.substring("Graphics/".length()) : name;
        int slash = path.lastIndexOf('/');
        if (slash < 0) {
            return null;
        }
        return assets.graphic(path.substring(0, slash), path.substring(slash + 1));
    }

    // =====================================================================
    // pbBattleIntroAnimation (Scene_Animations:5-53)
    // =====================================================================

    /**
     * Drives {@code pbBattleIntroAnimation}: the intro animation itself, then
     * (trainer battle) the two party lineups, or (wild battle) the data boxes
     * and the return to normal colour.
     *
     * @return true once the whole method has finished
     */
    private boolean stepIntro() {
        if (introPhase == 0) {
            if (!animations.isEmpty()) {                                        // :8-12
                return false;
            }
            // Post-appearance activities (:14-25)
            if (trainerBattle) {                                                // :17
                pbShowPartyLineup(0, true);                                     // :22
                pbShowPartyLineup(1, true);                                     // :23
                return true;                                                    // :24 return
            }
            // Wild battle (:29-39)
            for (int i = 0; i < sideSize(1); i++) {                              // :30
                int idxBattler = 2 * i + 1;                                     // :31
                if (battler(idxBattler) == null) {                               // :32
                    continue;
                }
                animations.add(new BattleAnimations.DataBoxAppearAnimation(this, idxBattler));   // :33-34
            }
            // :36-38 Set up wild Pokemon returning to normal colour and playing cry
            animations.add(new BattleAnimations.BattleIntroAnimation2(this, sideSize(1)));
            introPhase = 1;
            return false;                                                       // :40 while inPartyAnimation?
        }
        if (!animations.isEmpty()) {
            return false;
        }
        // Scene_Animations:41-52 show the shiny animation for wild Pokemon: pbCommonAnimation("SuperShiny" / "Shiny", battler).
        if (!introShinyBuilt) {
            introShinyBuilt = true;
            if (session.battle.showAnims) {                                         // :42 if @battle.showAnims
                for (int i = 0; i < sideSize(1); i++) {                             // :43
                    int idxBattler = 2 * i + 1;                                     // :44
                    Battler foe = battler(idxBattler);
                    if (foe == null || foe.pokemon == null || !foe.pokemon.shiny) { // :45 next if !battler || !shiny?
                        continue;
                    }
                    introShinyQueue.add(idxBattler);
                }
            }
        }
        if (!introShinyQueue.isEmpty()) {
            int idxBattler = introShinyQueue.poll();
            Battler foe = battler(idxBattler);
            introShinyActive = true;
            beginAnimation(new Battle.AnimationCall(true, -1, foe.pokemon.superShiny ? "SuperShiny" : "Shiny", idxBattler, -1, 0));   // :46-50
            return false;
        }
        introShinyBuilt = false;
        return true;
    }

    /** {@code pbShowPartyLineup} (Scene_Animations:58-64). */
    private void pbShowPartyLineup(int side, boolean fullAnim) {
        animations.add(new BattleAnimations.LineupAppearAnimation(this, side,
                partyPokemon(side), partyStarts(side), fullAnim));              // :59-60
        // fullAnim == true does not wait (:61-63); fullAnim == false does, and
        // the frame loop below runs the animation to completion.
    }

    /** {@code @battle.pbParty(side)} as Pokemon, for the lineup graphics. */
    private com.badlogic.gdx.utils.Array<Pokemon> partyPokemon(int side) {
        com.badlogic.gdx.utils.Array<Pokemon> list = new com.badlogic.gdx.utils.Array<>();
        for (Battler battler : party(side)) {
            list.add(battler.pokemon);
        }
        return list;
    }

    /** {@code inPartyAnimation?} (PokeBattle_Scene:78-80). */
    private boolean inPartyAnimation() {
        return !animations.isEmpty();
    }

    /** {@code pbGraphicsUpdate}'s animation pass (PokeBattle_Scene:24-37). */
    private void updateAnimations() {
        if (!animations.isEmpty()) {
            boolean compact = false;
            for (int i = 0; i < animations.size(); i++) {
                BattleAnimation animation = animations.get(i);
                animation.update();                                             // :29
                if (animation.animDone()) {                                     // :30
                    animation.dispose();                                        // :31
                    animations.set(i, null);
                    compact = true;
                }
            }
            if (compact) {
                animations.removeIf(Objects::isNull);                           // :36
            }
        }
        frameCounter += 1;                                                      // :41
        frameCounter = frameCounter % (40 * 12 / 20);                           // :42
        // pbFrameUpdate's per-battler sprite updates (PokeBattle_Scene:55-60):
        // the selected battler's data box and Pokemon bob.
        updateSelectedBob();
    }

    /** PokeBattle_SceneElements::QUARTER_ANIM_PERIOD (PokeBattle_SceneElements:374). */
    private static final int QUARTER_ANIM_PERIOD = 40 * 3 / 20;

    /**
     * {@code PokemonDataBox#updatePositions} (PokeBattle_SceneElements:376-386)
     * and {@code PokemonBattlerSprite#update} (:605-618): while a battler is
     * {@code @selected == 1} (its commands are being chosen) its data box moves
     * 2px up on phase 1 and down on phase 3, and the Pokemon sprite moves the
     * opposite way. Only the player's battler is ever selected in this runtime
     * (pbCommandMenuEx selects the side that is choosing).
     */
    private void updateSelectedBob() {
        // pbSelectBattler (PokeBattle_Scene:308-316): the battler whose command is chosen (1: bobbing) or the targets
        // being chosen (2: blinking). Scene_Commands:31/:105/:428/:459.
        int[] selected = new int[6];
        if (stage == Stage.BATTLE && (page == 0 || page == 1)) {
            selected[actingIndex()] = 1;
        } else if (stage == Stage.BATTLE && page == PAGE_TARGET && targetTexts != null) {
            for (int i = 0; i < selected.length && i < targetTexts.length; i++) {
                boolean sel = targetMode == 0 ? i == targetIndex : targetTexts[i] != null;
                if (sel) selected[i] = 2;
            }
        }
        int phase = (int) (frameCounter / QUARTER_ANIM_PERIOD);
        float boxBob = phase == 1 ? -2f : (phase == 3 ? 2f : 0f);
        float spriteBob = phase == 1 ? 2f : (phase == 3 ? -2f : 0f);
        int sixth = (int) (frameCounter / SIXTH_ANIM_PERIOD);
        for (int i = 0; i < 6; i++) {
            BattleSprite box = sprites.get("dataBox_" + i);
            if (box != null) {
                box.bobOffsetY = selected[i] != 0 ? boxBob : 0f;               // :380-385
            }
            BattleSprite pokemon = sprites.get("pokemon_" + i);
            if (pokemon != null) {
                pokemon.bobOffsetY = selected[i] == 1 ? spriteBob : 0f;         // :613-618
                pokemon.blinkHidden = selected[i] == 2 && (sixth == 2 || sixth == 5);   // :623-628
            }
        }
    }

    /** PokeBattle_SceneElements:603 {@code SIXTH_ANIM_PERIOD}. */
    private static final int SIXTH_ANIM_PERIOD = 40 * 2 / 20;

    // =====================================================================
    // pbSendOutBattlers (Scene_Animations:85-143) / pbSendOut
    // (Battle_Action_Switching:324-332)
    // =====================================================================

    /** {@code pbChangePokemon} (PokeBattle_Scene:318-329). */
    @Override public void changePokemon(int idxBattler, Battler battler) {
        pbChangePokemon(idxBattler, battler);
        pbRefresh();                                                             // :105
    }

    /**
     * {@code PokemonDataBox#refresh} (PokeBattle_Scene:63-68) re-reads
     * {@code @battler}. The plugin's battler object never changes - a switch
     * swaps its {@code @pokemon} (Battle_Action_Switching:313) - while this
     * runtime's party holds one {@link Battler} per Pokemon, so the data box has
     * to be re-pointed at the battler now occupying the slot.
     */
    private void refreshDataBoxes() {
        for (int i = 0; i < battlerCount(); i++) {
            BattleSprite box = sprites.get("dataBox_" + i);
            Battler battler = battler(i);
            if (box != null && battler != null) {
                box.battler = battler;
            }
        }
    }

    /** {@code PokemonBattlerSprite#setPokemonBitmap} (PokeBattle_SceneElements:584-590). */
    private void pbChangePokemon(int idxBattler, Battler battler) {
        if (battler == null) {
            return;
        }
        boolean back = !opposes(idxBattler);                                     // :322
        BattleSprite pokemon = sprites.get("pokemon_" + idxBattler);             // :320
        if (pokemon != null) {
            pokemon.battler = battler;                                          // :323 setPokemonBitmap
            pokemon.back = back;
            int[] size = pokemonBitmapSize(battler.pokemon, back);
            pokemon.bitmapWidth = size == null ? -1 : size[0];
            pokemon.bitmapHeight = size == null ? -1 : size[1];
            pbSetPokemonSpritePosition(pokemon, sideSize(idxBattler));           // :589 pbSetPosition
        }
        BattleSprite shadow = sprites.get("shadow_" + idxBattler);               // :321
        if (shadow != null) {
            shadow.battler = battler;
            pbSetShadowPosition(shadow, idxBattler, sideSize(idxBattler));      // :686-688 setPokemonBitmap
            // showShadow? (Pokemon_Sprites:369-373) returns true unconditionally.
            shadow.visible = true;                                              // :327
        }
    }

    /**
     * {@code pbSwapBattlerSprites(idxA,idxB)} (PokeBattle_Scene:266-279), used by Ally Switch and the end-of-round
     * shift: the two positions exchange their sprites, command memory and the state of their sprites and bars
     * (the data boxes' visibility goes with their Pokemon, so a fainted position stays without a box).
     */
    private void swapBattlerSprites(int a, int b) {
        int tmp = lastCmd[a]; lastCmd[a] = lastCmd[b]; lastCmd[b] = tmp;          // :269
        tmp = lastMove[a]; lastMove[a] = lastMove[b]; lastMove[b] = tmp;           // :270
        if (battler(a) != null) pbChangePokemon(a, battler(a));                    // :272-275 index + pbSetPosition
        if (battler(b) != null) pbChangePokemon(b, battler(b));
        for (String prefix : new String[] { "pokemon_", "shadow_", "dataBox_" }) { // :267-268 the sprites trade places
            BattleSprite x = sprites.get(prefix + a);
            BattleSprite y = sprites.get(prefix + b);
            if (x == null || y == null) continue;
            boolean visible = x.visible; x.visible = y.visible; y.visible = visible;
            float opacity = x.opacity; x.opacity = y.opacity; y.opacity = opacity;
            float zoomX = x.zoomX; x.zoomX = y.zoomX; y.zoomX = zoomX;
            float zoomY = x.zoomY; x.zoomY = y.zoomY; y.zoomY = zoomY;
            float[] tone = x.tone; x.tone = y.tone; y.tone = tone;
            float[] color = x.color; x.color = y.color; y.color = color;
        }
        float t;
        t = hpShown[a]; hpShown[a] = hpShown[b]; hpShown[b] = t;
        t = hpFrom[a]; hpFrom[a] = hpFrom[b]; hpFrom[b] = t;
        t = hpTo[a]; hpTo[a] = hpTo[b]; hpTo[b] = t;
        t = hpT[a]; hpT[a] = hpT[b]; hpT[b] = t;
        t = expShown[a]; expShown[a] = expShown[b]; expShown[b] = t;
        t = expFrom[a]; expFrom[a] = expFrom[b]; expFrom[b] = t;
        t = expTo[a]; expTo[a] = expTo[b]; expTo[b] = t;
        t = expT[a]; expT[a] = expT[b]; expT[b] = t;
        int held = heldHp[a]; heldHp[a] = heldHp[b]; heldHp[b] = held;
        pbRefresh();                                                               // :278
    }

    /** {@code pbRefresh} (PokeBattle_Scene:63-68): the data boxes re-read the battler. */
    private void pbRefresh() {
        // The runtime's data boxes are drawn from the battler every frame, so
        // there is nothing to cache beyond the battler reference itself.
        refreshDataBoxes();
    }

    /**
     * {@code PokemonBattlerSprite#width/height} (PokeBattle_SceneElements:553-554):
     * the square battler graphic the renderer draws.
     */
    private int[] pokemonBitmapSize(Pokemon pokemon, boolean back) {
        if (pokemon == null || pokemon.species == null) {
            return null;
        }
        Texture texture = pokemonTexture(pokemon, back);
        if (texture == null) {
            return null;
        }
        int size = Math.min(texture.getWidth(), texture.getHeight());
        return new int[] { size, size };
    }

    private Texture pokemonTexture(Pokemon pokemon, boolean back) {
        String id = battlerSpriteId(pokemon, back);
        Texture sprite = assets.graphic("Battlers", id);
        if (sprite == null) {
            sprite = assets.graphic("Battlers",
                    String.format(java.util.Locale.ROOT, "%03d", pokemon.species.id) + (back ? "b" : ""));
        }
        return sprite;
    }

    /** {@code pbApplyBattlerMetricsToSprite} (Pokemon_Sprites:349-365). */
    private float[] battlerMetrics(Battler battler, boolean playerSide) {
        float x = 0f;
        float y = 0f;
        if (battler != null && battler.pokemon != null && battler.pokemon.species != null) {
            PbsData.BattlerOffsets m = battler.pokemon.form != null && battler.pokemon.form.battler != null
                    ? battler.pokemon.form.battler : battler.pokemon.species.battler;
            if (m != null) {
                if (playerSide) {                                               // :356
                    x += m.playerX * 2;                                         // :357
                    y += m.playerY * 2;                                         // :358
                } else {                                                        // :359
                    x += m.enemyX * 2;                                          // :360
                    y += m.enemyY * 2;                                          // :361
                    // :362 sprite.y -= (metrics[MetricBattlerAltitude][species] || 0)*2.
                    // No species in this project declares BattlerAltitude
                    // (PBS/pokemon.txt and pokemonforms.txt contain no such
                    // line), so every lookup is 0 and the line is a no-op.
                }
            }
        }
        return new float[] { x, y };
    }

    /** The old single-argument form, still used by the steady-state renderer. */
    private float[] battlerPosition(Battler b, boolean player) {
        int index = player ? 0 : 1;
        float[] pos = PokeBattle_SceneConstants.battlerPosition(index, 1);
        float[] metrics = battlerMetrics(b, player);
        return new float[] { pos[0] + metrics[0], pos[1] + metrics[1] };
    }

    // =====================================================================
    // pbStartBattleSendOut's step runner (Battle_StartAndEnd:194-275)
    // =====================================================================

    // =====================================================================
    // The full Mega Evolution scene (Mega evolution:400-417)
    // =====================================================================

    /** {@code @battle.pbMegaEvolve}'s Pokemon whose sprite still shows its old form: Pokemon -> form. */
    private final java.util.IdentityHashMap<Pokemon, Integer> heldForm = new java.util.IdentityHashMap<>();
    private MegaEvolutionScene megaScene;
    private int megaSceneBattler = -1;

    private void beginMegaScene(Battle.RoundEvent event) {
        Battler battler = session.battle.battlerAt(event.idxBattler);
        if (battler == null || battler.pokemon == null) {
            resumeRound();
            return;
        }
        megaSceneBattler = event.idxBattler;
        megaScene = new MegaEvolutionScene(new MegaEvolutionScene.Host() {
            @Override public BattleSprite addSprite(String id, float x, float y, String file) {
                BattleSprite sprite = pbAddSprite(id, x, y, file);
                sprite.origin = PictureEx.Origin.TOP_LEFT;
                return sprite;
            }

            @Override public void removeSprite(String id) {
                sprites.remove(id);
            }

            @Override public boolean bitmapExists(String file) {
                return bitmapSize(file) != null;
            }

            @Override public int[] bitmapSizeOf(String file) {
                return bitmapSize(file);
            }

            @Override public String pokemonFile(Pokemon pkmn, int form) {
                String id = battlerSpriteId(pkmn, false, form);
                String file = "Graphics/Battlers/" + id;
                if (bitmapSize(file) == null) {
                    file = "Graphics/Battlers/" + String.format(java.util.Locale.ROOT, "%03d", pkmn.species.id);
                }
                return file;
            }

            @Override public void playCry(Pokemon pkmn) {
                session.battle.playCry(pkmn);
            }

            @Override public void playBgm(String name) {
                playBattleBgm(name);
            }

            @Override public String playingBgm() {
                pokemon.runtime.audio.AudioManager audio = context.audioManager();
                return audio == null ? null : audio.currentBgmId();
            }

            @Override public int time() {
                return session.battle.time;
            }

            @Override public String backdrop() {
                return backdropName();
            }
        }, battler.pokemon, event.oldHp, event.megaOldForm(), event.megaNewForm());
        stage = Stage.MEGA_SCENE;
    }

    private void updateMegaScene() {
        if (megaScene != null && !megaScene.done()) {
            return;
        }
        if (megaScene != null) {
            megaScene.dispose();
        }
        megaScene = null;
        Battler battler = session.battle.battlerAt(megaSceneBattler);
        if (battler != null) {
            heldForm.remove(battler.pokemon);
            changePokemon(megaSceneBattler, battler);        // :415-416 @scene.pbChangePokemon + pbRefreshOne
        }
        megaSceneBattler = -1;
        resumeRound();
    }

    /** {@code pbOnActiveAll} has run for this battle. */
    private boolean openingEffectsDone;
    /** {@code pbSafariStart} has begun (the Safari data box replaced {@code dataBox_0}). */
    private boolean safariStarted;

    /** Runs the plan built by {@link BattleSendOut#plan}. */
    private void stepOpening() {
        InputManager input = context.inputManager();
        // pbDisplayPaused blocks until the player confirms (:203/208/220).
        if (message != null && message.paused) {
            tickMessage(input);
            return;
        }
        // pbSendOut blocks until its animation is done (:273 ->
        // Scene_Animations:119-131); a brief line lingers through it.
        if (sendOut != null) {
            if (message != null) {
                tickBriefMessage(input);
            }
            if (!sendOut.done()) {
                return;
            }
            sendOut.dispose();
            sendOut = null;
            planIndex++;
            return;
        }
        if (planIndex >= plan.size() && session.safari) {
            // 160_PokeBattle_SafariZone:228-239 pbSafariStart: the Safari data box slides in, then the loop starts
            if (!safariStarted) {
                safariStarted = true;
                sprites.remove("dataBox_0");
                BattleSprite box = sprites.add("dataBox_0", BattleSprite.Kind.SAFARI_BOX);
                box.name = "Graphics/Pictures/Battle/databox_safari";
                int[] size = bitmapSize(box.name);
                box.bitmapWidth = size == null ? -1 : size[0];
                box.bitmapHeight = size == null ? -1 : size[1];
                box.x = 0;
                box.y = 0;
                box.z = 50;
                box.homeX = 0;
                box.visible = false;
                briefMessage = false;
                message = null;
                animations.add(new BattleAnimations.DataBoxAppearAnimation(this, 0));
                return;
            }
            if (!animations.isEmpty()) return;
            stage = Stage.BATTLE;
            openingEffectsDone = true;
            goCommandPhase();
            return;
        }
        if (planIndex >= plan.size()) {
            stage = Stage.BATTLE;                                                // pbBattleLoop
            message = null;
            if (!openingEffectsDone) {
                openingEffectsDone = true;
                if (session.onActiveAll()) {                                     // Battle_StartAndEnd:354 pbOnActiveAll
                    pursuitContinuation = this::goCommandPhase;
                    queueRoundMessages();
                    return;
                }
            }
            goCommandPhase();
            return;
        }
        BattleSendOut.Step step = plan.get(planIndex);
        switch (step.kind) {
            case DISPLAY_PAUSED:
                showBattleMessage(step.message, true);
                planIndex++;
                return;
            case DISPLAY_BRIEF:
                // pbDisplayBrief(msg) shows the line and returns at once (:267).
                showBattleMessage(step.message, false);
                planIndex++;
                return;
            case SEND_OUT:
            default:
                startSendOut(step.battlers, true);
                return;
        }
    }

    /**
     * {@code pbSendOut} (Battle_Action_Switching:324-332) followed by
     * {@code pbSendOutBattlers} (Scene_Animations:85-143).
     */
    private void startSendOut(int[] battlers, boolean startBattle) {
        List<int[]> sendOuts = new ArrayList<>();
        for (int idxBattler : battlers) {
            sendOuts.add(new int[] { idxBattler });                              // :325/[idxBattler,pkmn]
        }
        if (sendOuts.isEmpty()) {                                                // Scene_Animations:86
            planIndex++;
            return;
        }
        // Scene_Animations:87-89 "If party balls are still appearing, wait for
        // them to finish showing up, as the FadeAnimation will make them
        // disappear": while inPartyAnimation?; pbUpdate; end. The fade picks the
        // bar/balls it hides when it is created (PokeBattle_SceneAnimations:
        // 380/388 next if !...visible), so starting it while the lineup is still
        // appearing leaves those balls on screen. This step is retried next frame.
        if (inPartyAnimation()) {
            return;
        }
        sendOut = new BattleAnimations.SendOutSequence(this, sendOuts, startBattle);
        // Scene_Animations:119-131 is entered right after the animations are
        // created, so its first iteration runs in the same beat; without it a
        // freshly created sprite would be drawn at its creation position
        // (PokeballTrainerSendOutAnimation:518 builds the ball at (0,0)).
        sendOut.update();
    }

    // =====================================================================
    // Battle_Action_Switching:256-262 pbRecallAndReplace
    // =====================================================================

    /**
     * Queues one {@code pbRecallAndReplace}. Several can be queued at once
     * because {@code pbEORSwitch} replaces the player's Pokemon before the
     * opponent's when it offers the Switch-style change (:196-203).
     */
    private void queueRecallAndReplace(int idxBattler, int idxParty, boolean recallMessage,
            Runnable done) {
        switchQueue.add(new SwitchRequest(idxBattler, idxParty, recallMessage, done));
        if (switchStep == SwitchStep.NONE) {
            beginNextSwitch();
        }
    }

    private void beginNextSwitch() {
        switchCurrent = switchQueue.poll();
        if (switchCurrent == null) {
            switchStep = SwitchStep.NONE;
            return;
        }
        switchIdxBattler = switchCurrent.idxBattler;
        switchIdxParty = switchCurrent.idxParty;
        recallPhase = 0;
        switchStep = SwitchStep.RECALL;
        if (switchCurrent.recallMessage) {
            // :57 / :197 pbMessageOnRecall(battler): a brief line that lingers
            // while the recall animation runs.
            showBattleMessage(session.recallMessage(switchIdxBattler), false);
        }
    }

    /**
     * One update of {@code pbRecallAndReplace} (:256-262).
     *
     * @return true while a switch is still playing
     */
    private boolean updateSwitch() {
        if (switchStep == SwitchStep.NONE) {
            return false;
        }
        if (message != null) {
            tickBriefMessage(context.inputManager());
        }
        Battler outgoing = session.battle.battlerAt(switchIdxBattler);
        switch (switchStep) {
            case RECALL: {
                // :257 @scene.pbRecall(idxBattler) if !@battlers[idxBattler].fainted?
                if (recallPhase == 0) {
                    recallPhase = 1;
                    if (outgoing != null && !outgoing.fainted()) {
                        animations.add(new BattleAnimations.BattlerRecallAnimation(this,
                                switchIdxBattler));               // Scene_Animations:151
                        return true;
                    }
                    return true;
                }
                if (inPartyAnimation()) {
                    return true;                                  // :152-156 the recall loop
                }
                if (recallPhase == 1) {
                    recallPhase = 2;
                    if (outgoing != null && !outgoing.fainted()) {
                        // Scene_Animations:159-165 DataBoxDisappearAnimation
                        animations.add(new BattleAnimations.DataBoxDisappearAnimation(this,
                                switchIdxBattler));
                    }
                    return true;
                }
                if (requestKind != null) {                        // @scene.pbRecall(idxBattler) alone (:257)
                    finishRequestStep(null);
                    return true;
                }
                switchStep = SwitchStep.LINEUP;
                recallPhase = 0;
                return true;
            }
            case LINEUP: {
                // :259 @scene.pbShowPartyLineup(idxBattler&1) if pbSideSize(idxBattler)==1,
                // which waits for the animation (Scene_Animations:61-63).
                if (recallPhase == 0) {
                    recallPhase = 1;
                    if (sideSize(switchIdxBattler) == 1) {
                        pbShowPartyLineup(switchIdxBattler & 1, false);
                    }
                    return true;
                }
                if (inPartyAnimation()) {
                    return true;
                }
                if (requestKind != null) {                        // @scene.pbShowPartyLineup alone (:259)
                    finishRequestStep(null);
                    return true;
                }
                switchStep = SwitchStep.MESSAGE;
                recallPhase = 0;
                return true;
            }
            case MESSAGE: {
                // :260 pbMessagesOnReplace(idxBattler,idxParty) (":284-305")
                if (recallPhase == 0) {
                    recallPhase = 1;
                    showBattleMessage(session.replaceMessage(switchIdxBattler, switchIdxParty), false);
                    return true;
                }
                switchStep = SwitchStep.REPLACE;
                recallPhase = 0;
                return true;
            }
            case REPLACE: {
                // :261 pbReplace -> :313 pbInitialize(party[idxParty],idxParty,batonPass)
                session.battle.replace(switchIdxBattler, switchIdxParty);
                refreshDataBoxes();
                switchStep = SwitchStep.SEND;
                // :318 pbSendOut([[idxBattler,party[idxParty]]]), startBattle=false
                sendOut = new BattleAnimations.SendOutSequence(this,
                        java.util.Collections.singletonList(new int[] { switchIdxBattler }), false);
                sendOut.update();
                return true;
            }
            case SEND:
            default: {
                if (sendOut != null && !sendOut.done()) {
                    return true;                                  // :326 pbSendOut blocks
                }
                if (sendOut != null) {
                    sendOut.dispose();
                    sendOut = null;
                }
                if (switchIdxBattler >= 0 && switchIdxBattler < lastMove.length) {
                    lastMove[switchIdxBattler] = 0;                   // :328 @scene.pbResetMoveIndex(b[0])
                }
                message = null;
                // :325-331 @peer.pbOnEnteringBattle / pbResetMoveIndex / pbSetSeen /
                // @usedInBattle have no counterpart here; the send-out sequence
                // itself called pbChangePokemon + pbRefresh (:104-105).
                if (requestKind != null) {                        // @scene.pbSendOutBattlers alone (:326)
                    finishRequestStep(null);
                    return true;
                }
                Runnable done = switchCurrent == null ? null : switchCurrent.done;
                switchCurrent = null;
                switchStep = SwitchStep.NONE;
                if (done != null) {
                    done.run();
                }
                if (switchStep == SwitchStep.NONE) {
                    beginNextSwitch();
                }
                return switchStep != SwitchStep.NONE;
            }
        }
    }

    // =====================================================================
    // Battle_Action_Switching:136-332: the engine's scene calls
    // =====================================================================

    /**
     * The engine ({@code pbEORSwitch}, {@code pbSwitchInBetween}, {@code pbRecallAndReplace}, ...) waits on
     * {@code call}; the events it produced before are already played. {@link InteractiveBattlePort.Session#answer}
     * lets it carry on.
     */
    private void beginRequest(Battle.SceneCall call) {
        switch (call.kind) {
            case PARTY_SCREEN:
                // @scene.pbPartyScreen(idxBattler,canCancel){ |idxParty,partyScene| ... } (:138)
                partyValidator = call.validator;
                openPartyScreen(call.idxBattler, false, call.canCancel, false);
                return;
            case CONFIRM:
                // @scene.pbDisplayConfirmMessage(msg) -> PokeBattle_Scene:198-200 pbShowCommands(msg,[是,否],1): B answers "否".
                showChoice(call.text, 1, pick -> {
                    session.answer(pick == 0);
                    queueRoundMessages();
                });
                return;
            case CHOOSE_BALL:
                // PokemonBagScreen#pbChooseItemScreen(Proc{|item| pbIsPokeBall?(item)}) (PokeBattle_BOSS:168-169):
                // the real BW Bag, opened only to pick one Poke Ball.
                if (bagView == null) bagView = new BagView(context);
                bagView.chooseItem(item -> {
                    pokemon.runtime.pokemon.PbsData pbs = context.pbsData();
                    pokemon.runtime.pokemon.PbsData.Item data = pbs == null ? null : pbs.item(item);
                    return data != null && data.isPokeBall();
                });
                ballBagOpen = true;
                page = 2;
                window = COMMAND_BOX;
                return;
            case RECALL:
                // @scene.pbRecall(idxBattler) (:257): the recall animation and the data box leaving
                requestKind = call.kind;
                switchIdxBattler = call.idxBattler;
                recallPhase = 0;
                switchStep = SwitchStep.RECALL;
                return;
            case SHOW_PARTY_LINEUP:
                // @scene.pbShowPartyLineup(idxBattler&1) (:259)
                requestKind = call.kind;
                switchIdxBattler = call.idxBattler;
                recallPhase = 0;
                switchStep = SwitchStep.LINEUP;
                return;
            case SEND_OUT:
            default:
                // @scene.pbSendOutBattlers(sendOuts,startBattle) (:326), after pbReplace (:313) swapped the battler
                requestKind = call.kind;
                switchIdxBattler = call.idxBattlers[0];
                refreshDataBoxes();
                switchStep = SwitchStep.SEND;
                sendOut = new BattleAnimations.SendOutSequence(this,
                        java.util.Collections.singletonList(call.idxBattlers), call.startBattle);
                sendOut.update();
                return;
        }
    }

    /** One of the engine's animation calls has played: it carries on and its events are queued. */
    private void finishRequestStep(Object answer) {
        requestKind = null;
        switchStep = SwitchStep.NONE;
        recallPhase = 0;
        session.answer(answer);
        queueSessionEvents();
        // A brief line (pbMessageOnRecall's) keeps lingering while the next scene call runs - it must not hold the
        // engine's next request back (nothing would start it once the line is gone).
        if (queue.isEmpty() && (message == null || message.brief)) {
            afterRound();
        }
    }

    // ---------------------------------------------------------------------
    // pbPartyScreen / pbShowCommands (Battle_Action_Switching:136-151,
    // PokeBattle_Scene:202-237)
    // ---------------------------------------------------------------------

    /** Opens the party screen for the battle's own {@code pbPartyScreen} call. */
    private void openPartyScreen(int idxBattler, boolean checkLaxOnly, boolean canCancel,
            boolean register) {
        partyCheckLaxOnly = checkLaxOnly;
        partyCanCancel = canCancel;
        partyRegister = register;
        partyScreenBattler = idxBattler;
        if (partyView == null) {
            partyView = new PartyView(context);
        }
        partyView.battleChoose(true, canCancel);
        partyView.displayOrder(playerDisplayOrder());              // Battle_Phase_Command:109 pbPlayerDisplayParty
        page = 3;
        window = COMMAND_BOX;
        partyScreenOpen = true;
    }

    /**
     * {@code pbPlayerDisplayParty} (the party screen's order in battle): the battlers on the field come first, then
     * the rest of the party in order. Entries are indexes into the trainer's party.
     */
    private int[] playerDisplayOrder() {
        int count = Math.min(context.gameState().trainer().party.size(), session.battle.playerParty().size);
        boolean[] used = new boolean[count];
        int[] order = new int[count];
        int next = 0;
        for (int position = 0; position < 6; position += 2) {
            Battler active = session.battle.battlerAt(position);
            if (active != null && active.pokemonIndex >= 0 && active.pokemonIndex < count && !used[active.pokemonIndex]) {
                used[active.pokemonIndex] = true;
                order[next++] = active.pokemonIndex;
            }
        }
        for (int i = 0; i < count; i++) {
            if (!used[i]) {
                order[next++] = i;
            }
        }
        return order;
    }

    /** The party screen of a bag item (Scene_Commands:263-346): "要对哪只宝可梦使用？" and the chosen Pokemon. */
    private PartyView itemParty;
    private String itemPartyItem;
    private int itemPartyResult = -1;

    private void openItemParty(String item) {
        final int[] order = playerDisplayOrder();
        PbsData.Item data = context.pbsData().item(item);
        final int useType = data == null ? 1 : data.battleUse;
        itemPartyItem = item;
        itemPartyResult = -1;
        if ((useType == 1 || useType == 6) && order.length == 1) {   // :276-279 the only Pokemon is used at once
            itemPartyResult = order[0];
            finishItemParty();
            return;
        }
        final int[] chosen = {-1};
        itemParty = PartyView.forItem(context, scene -> {
            scene.pbStartScene("要对哪只宝可梦使用？", null);       // :290 pkmnScreen.pbStartScene
            while (true) {
                scene.pbSetHelpText("要对哪只宝可梦使用？");          // :296
                int shown = scene.pbChoosePokemon("要对哪只宝可梦使用？");   // :297 pbChoosePokemon
                if (shown < 0) {
                    break;                                          // :298 cancelled: the Bag again
                }
                int real = order[shown];
                Pokemon pkmn = context.gameState().trainer().party.get(real);
                if (pkmn == null || pkmn.egg) {
                    continue;                                       // :308 next if !pkmn || pkmn.egg?
                }
                if (useType == 2 || useType == 7) {                  // :310-313 an Ether asks for the move
                    if (scene.pbChooseMove(pkmn, "要回复哪个招式？") < 0) {
                        continue;
                    }
                }
                chosen[0] = real;
                break;
            }
            itemPartyResult = chosen[0];
        }, null, order);
    }

    /** The party screen of the bag item ended: use the item on the chosen Pokemon, or go back to the Bag. */
    private void finishItemParty() {
        itemParty = null;
        if (itemPartyResult < 0) {
            return;                                                 // :326-327 the Bag is shown again
        }
        bagItem = itemPartyItem;
        bagTarget = itemPartyResult;
        completeBagItem();
    }

    /** The Bag ended with an item to use (or none): {@code yield item, useType, idxParty, ...} of pbItemMenu. */
    private void completeBagItem() {
        if (bagItem != null && doubles()) {
            String id = bagItem;
            int target = Math.max(0, bagTarget);
            bagItem = null; bagTarget = -1;
            useBagItemInDoubles(id, target);
            return;
        }
        if (bagItem != null) {
            if (!session.item(bagItem, Math.max(0, bagTarget))) session.message = "现在无法使用这个道具。";
            queueSessionEvents();
            bagItem = null; bagTarget = -1;
        }
        go(0);
    }

    /** The engine's {@code pbChooseItemScreen} (a Poke Ball from the Bag, PokeBattle_BOSS:168) is on screen. */
    private boolean ballBagOpen;

    /** {@code idxBattler} of the {@code pbPartyScreen} call in progress. */
    private int partyScreenBattler;
    /** {@code pbPartyScreen}'s while-loop is on screen (Battle_Action_Switching:136-150). */
    private boolean partyScreenOpen;

    /** Closes the party screen and applies the choice (:139-149). */
    private void finishPartyScreen(int chosen) {
        int idxBattler = partyScreenBattler;
        boolean register = partyRegister;
        boolean laxOnly = partyCheckLaxOnly;
        java.util.function.IntFunction<String> validator = partyValidator;
        if (chosen < 0) {                                          // pbPartyScreen canCancel
            partyScreenOpen = false;
            if (validator != null) {
                // The engine's pbPartyScreen returns -1 (:137 ret): the block never accepted a Pokemon.
                partyValidator = null;
                partyHandedOver = true;
                page = 0;
                session.answer(null);
                queueRoundMessages();
                return;
            }
            go(0);                                                 // pbPartyMenu returned false
            return;
        }
        String refusal;
        if (validator != null) {
            refusal = validator.apply(chosen);                     // the engine's block (:139-148)
        } else {
            refusal = laxOnly
                    ? session.battle.canSwitchLax(idxBattler, chosen)       // :140
                    : session.battle.canSwitch(idxBattler, chosen);         // :142
            if (refusal == null && register && !session.battle.registerSwitch(idxBattler, chosen)) {
                refusal = "";                                      // :145
            }
        }
        if (refusal != null) {
            // partyScene.pbDisplay(...) - the party screen stays open underneath.
            // Battle_Action_Switching:136-150 loops: the chooser starts again after the line.
            partyView.battleChoose(true, partyCanCancel);
            showBattleMessage(refusal.isEmpty() ? "不能换上这只宝可梦。" : refusal, true);
            return;
        }
        partyScreenOpen = false;
        if (validator != null) {
            // :147 ret = idxParty (set by the block): the engine carries on with the choice.
            partyValidator = null;
            partyHandedOver = true;
            page = 0;
            session.answer(null);
            queueRoundMessages();
            return;
        }
        if (register) {
            if (doubles()) {
                // pbPartyMenu true (Battle_Phase_Command:233 break): the switch is played in the attack phase
                partyHandedOver = true;
                page = 0;
                afterBattlerCommand();
                return;
            }
            // pbAttackPhaseSwitch:57 pbMessageOnRecall, :59 pbPursuit, :68 pbRecallAndReplace
            go(0);
            beginPlayerSwitch(idxBattler, chosen);
            return;
        }
        go(0);
    }

    /** {@code pbShowCommands(msg,[是否],defaultValue)} (PokeBattle_Scene:202-237). */
    private void showChoice(String question, int defaultValue,
            java.util.function.IntConsumer result) {
        message = new Message(cleanMessage(question), false);
        revealedChars = message.text.length();
        holdFrames = 0;
        queue.clear();
        window = MESSAGE_BOX;
        choiceOptions = new String[] { "是", "否" };
        choiceDefault = defaultValue;
        choiceCursor.size(choiceOptions.length);
        choiceCursor.select(0);                                    // :211 cw.index = 0
        choiceResult = result;
    }

    /** @return true while the command window owns the input */
    private boolean updateChoice() {
        if (choiceOptions == null) {
            return false;
        }
        InputManager input = context.inputManager();
        // SpriteWindow_Selectable#update (SpriteWindow_text:846-868), one column:
        // on a trigger UP/DOWN wrap around (:851/:861 item_max%column_max==0)
        // and play pbPlayCursorSE when the index moved (:854-856/:864-866).
        int count = choiceOptions.length;
        int oldIndex = choiceCursor.index();
        if (input.wasPressed(GameAction.UP)) {
            choiceCursor.select((oldIndex - 1 + count) % count);            // :853
        } else if (input.wasPressed(GameAction.DOWN)) {
            choiceCursor.select((oldIndex + 1) % count);                    // :863
        }
        if (choiceCursor.index() != oldIndex) playCursorSe();
        // :218-226 B returns the default; C returns the highlighted entry.
        if (input.wasPressed(GameAction.CANCEL) && choiceDefault >= 0) {
            finishChoice(choiceDefault);
            return true;
        }
        if (input.wasPressed(GameAction.CONFIRM)) {
            finishChoice(choiceCursor.index());
            return true;
        }
        return true;
    }

    private void finishChoice(int index) {
        java.util.function.IntConsumer result = choiceResult;
        choiceResult = null;
        choiceOptions = null;
        message = null;
        if (result != null) {
            result.accept(index);
        }
    }

    // ---------------------------------------------------------------------
    // The round's tail
    // ---------------------------------------------------------------------

    /** What {@link #afterRound()} runs once the events of {@code pbPursuit} (Battle_Phase_Attack:59) have played. */
    private Runnable pursuitContinuation;

    /**
     * {@code pbAttackPhaseSwitch} (Battle_Phase_Attack:56-68): the recall line, then a
     * Pursuit aimed at the switcher, then the recall and the send-out. Without a Pursuit
     * (the usual case) it is the plain recall-and-replace.
     */
    private void beginPlayerSwitch(int idxBattler, int idxParty) {
        String recall = session.recallMessage(idxBattler);                 // :57 (before the Pursuit hits)
        if (!session.pursuitOnSwitch()) {                                  // :59
            queueRecallAndReplace(idxBattler, idxParty, true, this::afterPlayerSwitch);
            return;
        }
        pursuitContinuation = () -> queueRecallAndReplace(idxBattler, idxParty, false,
                this::afterPlayerSwitch);                                  // :68
        pushMessages(recall, false);
        queueRoundMessages();
    }

    /**
     * {@code pbAttackPhase}'s remainder after the player's switch
     * (Battle_Phase_Attack:186-193): the opponent still acts this round.
     */
    private void afterPlayerSwitch() {
        session.message = null;
        // Battle_Phase_Attack:69 b.pbEffectsOnSwitchIn(true): entry hazards, Healing Wish, abilities
        if (session.switchInEffects(new int[] { 0 })) {
            pursuitContinuation = () -> {
                session.foeTurn();                                 // pbAttackPhaseMoves
                queueRoundMessages();
            };
            queueRoundMessages();
            return;
        }
        session.foeTurn();                                         // pbAttackPhaseMoves
        queueRoundMessages();
    }

    /**
     * Queues the round's lines; with none the tail runs immediately (a round
     * with no messages at all still has to reach {@code pbEORSwitch}).
     */
    private void queueRoundMessages() {
        queueSessionEvents();
        // A lingering brief line (the faint line before pbEORSwitch's party screen) must not hold back the scene call
        // the engine waits on: nothing would start it and the command menu would open over a suspended engine.
        if (queue.isEmpty() && (message == null || (message.brief && session.suspended()))) {
            afterRound();
        }
    }

    /**
     * Queues what the action that just ran showed, in the plugin's order
     * ({@link InteractiveBattlePort.Session#takeEvents()}: the port's lines, the
     * moves' and the end of round's events, the closing line). An action that
     * produced no events (a refused item, pbRun's own line) still has its
     * {@code session.message} text.
     */
    private void queueSessionEvents() {
        com.badlogic.gdx.utils.Array<Battle.RoundEvent> events = session.takeEvents();
        if (events.size == 0) {
            pushMessages(session.message);
            return;
        }
        roundPlayback = true;
        BattleEventLog.round(session.battle, events);
        holdPlayerExp();
        // The status icons keep what the boxes showed before the round (the engine already applied them all) until the
        // events that set them play.
        statusHeld = true;
        // The data boxes start from the HP each battler had before its first
        // HP change of the round (the engine already applied them all).
        for (Battle.RoundEvent event : events) {
            if (event.kind == Battle.RoundEvent.Kind.HIT) {
                for (Battle.HitEvent hit : event.hits) holdHp(hit.idxBattler, hit.oldHp);
            } else if (event.kind == Battle.RoundEvent.Kind.HP_CHANGE) {
                holdHp(event.idxBattler, event.oldHp);
            } else if (event.kind == Battle.RoundEvent.Kind.MEGA_SCENE
                    || event.kind == Battle.RoundEvent.Kind.CHANGE_POKEMON) {
                // The engine already changed the form; the sprite keeps showing the old one until the event plays.
                Battler changed = session.battle.battlerAt(event.idxBattler);
                if (changed != null && changed.pokemon != null && !heldForm.containsKey(changed.pokemon)) {
                    heldForm.put(changed.pokemon, event.kind == Battle.RoundEvent.Kind.MEGA_SCENE
                            ? event.megaOldForm() : event.oldHp);
                }
            }
        }
        for (Battle.RoundEvent event : events) {
            Message entry;
            if (event.kind == Battle.RoundEvent.Kind.MESSAGE) {
                if (event.text == null || event.text.isEmpty()) continue;
                entry = new Message(cleanMessage(event.text), event.paused, event.brief, null);
            } else {
                entry = new Message("", false, false, event);
            }
            entry.statuses = event.statuses;
            queue.add(entry);
        }
    }

    /** Pins the player's exp bar to the fill it had before this round's exp award. */
    private void holdPlayerExp() {
        heldExp = -1f;
        com.badlogic.gdx.utils.Array<Battle.ExpAward> awards = session.battle.lastExpAwards;
        Battler player = session.battle.player();
        if (awards == null || player == null) return;
        for (Battle.ExpAward award : awards) {
            if (award.pokemon == player.pokemon && award.segments.size > 0) {
                int[] first = award.segments.get(0);
                int range = first[1] - first[0];
                heldExp = range <= 0 ? 0f : Math.max(0f, Math.min(1f, (first[2] - first[0]) / (float) range));
                return;
            }
        }
    }

    /** Pins a side's bar to the HP it had before the round's first change. */
    private void holdHp(int idxBattler, int hp) {
        Battler b = battler(idxBattler);
        if (b == null) return;
        int side = idxBattler;
        if (heldHp[side] < 0) {
            heldHp[side] = hp;
            int maxHp = Math.max(1, b.maxHp());
            hpFrom[side] = hpTo[side] = hpShown[side] = Math.max(0f, Math.min(1f, hp / (float) maxHp));
            hpT[side] = 1f;
        }
    }

    /**
     * {@code pbEndBattle} (PokeBattle_Scene:296-303): hide the windows, fade the
     * BGM out over a second, fade every sprite to black, then drop them. The
     * event layer only resumes once this has finished, so the map comes back on
     * a black screen - which {@code pbBattleAnimation}'s own fade-in then lifts
     * (PField_Visuals:121-131).
     */
    private void beginEndBattle() {
        if (stage == Stage.END_BATTLE) {
            return;
        }
        if (beginCaughtStore()) {                                  // Battle_StartAndEnd:510 pbRecordAndStoreCaughtPokemon
            return;
        }
        stage = Stage.END_BATTLE;
        pbShowWindow(BLANK);                                       // :298
        message = null;
        queue.clear();
        choiceOptions = null;
        levelUpWindow = 0;
        // :300 pbBGMFade(1.0) -> Audio_Play:71 pbBGMStop(x) -> Game_System:111-115.
        if (context.audioManager() != null) {
            context.audioManager().fadeBgm(1.0f);
        }
        endBattleFadeFrame = 0;                                    // :301 pbFadeOutAndHide
    }

    private boolean caughtDone;
    private java.util.List<InteractiveBattlePort.CaughtStep> caughtSteps;
    private int caughtIndex;
    private pokemon.runtime.ui.menu.DexEntryView caughtDex;

    /** Starts the Pokedex / storing lines of the Pokemon caught this battle; false when there is nothing to show. */
    private boolean beginCaughtStore() {
        if (caughtDone) {
            return false;
        }
        caughtDone = true;
        caughtSteps = session.storeCaught();
        if (caughtSteps.isEmpty()) {
            return false;
        }
        stage = Stage.CAUGHT_STORE;
        caughtIndex = 0;
        stepCaughtStore();
        return true;
    }

    private void stepCaughtStore() {
        if (caughtIndex >= caughtSteps.size()) {
            beginEndBattle();                                      // caughtDone is set: goes on to pbEndBattle
            return;
        }
        InteractiveBattlePort.CaughtStep step = caughtSteps.get(caughtIndex++);
        if (step.dex != null) {                                    // :51 @scene.pbShowPokedex(species)
            message = null;
            caughtDex = new pokemon.runtime.ui.menu.DexEntryView(context,
                    java.util.Collections.singletonList(step.dex), 0);
            context.audioManager().playCry(step.dex.id);
            return;
        }
        showBattleMessage(step.text, true);                        // pbDisplayPaused
    }

    private void updateCaughtStore() {
        if (caughtDex != null) {
            if (caughtDex.update(context.inputManager())) {
                caughtDex = null;
                stepCaughtStore();
            }
            return;
        }
        if (message != null) {
            tickMessage(context.inputManager());
            return;
        }
        if (!queue.isEmpty()) {
            startNextMessage();
            return;
        }
        stepCaughtStore();
    }

    /** One frame of {@code pbFadeOutAndHide} (MessageConfig:607-609). */
    private void updateEndBattle() {
        // :608 pbSetSpritesToColor(sprites,Color.new(0,0,0,j*alphaDiff))
        int alpha = Math.min(255, endBattleFadeFrame * END_FADE_ALPHA_STEP);
        for (BattleSprite sprite : sprites.drawOrder()) {
            sprite.color[0] = 0f;
            sprite.color[1] = 0f;
            sprite.color[2] = 0f;
            sprite.color[3] = alpha;
        }
        endBattleFadeFrame++;
        if (endBattleFadeFrame > END_FADE_FRAMES) {
            // :612-617 every sprite that was visible is hidden.
            for (BattleSprite sprite : sprites.drawOrder()) {
                sprite.visible = false;
            }
            // :302 pbDisposeSprites; the event layer resumes (PField_Visuals:109).
            finished = true;
            port.finish();
        }
    }

    /**
     * The end of a round: {@code pbEORSwitch} (Battle_Phase_EndOfRound:744), then
     * {@code pbEndOfBattle} when it is over (Battle_StartAndEnd:444-473), else
     * the next command phase.
     */
    private void afterRound() {
        // pbGainExp / the round's scene calls have returned by now; the battle
        // loop goes on to pbEORSwitch (Battle_Phase_EndOfRound:739/744). Leave
        // EXP_GAIN (or any animation stage) first, otherwise its own update
        // keeps the input and pbShowCommands' window never sees it.
        stage = Stage.BATTLE;
        heldExp = -1f;
        if (session.suspended()) {
            // The engine (pbEORSwitch, a move's pbSwitchInBetween, ...) waits on a scene call.
            beginRequest(session.request());
            return;
        }
        if (pursuitContinuation != null) {
            Runnable continuation = pursuitContinuation;
            pursuitContinuation = null;
            if (session.result == null) {                             // :60 return if @decision>0
                continuation.run();
                return;
            }
        }
        if (beginTrainerEnd()) {
            return;
        }
        if (session.result != null) {
            beginEndBattle();                                      // :530 pbEndBattle
            return;
        }
        stage = Stage.BATTLE;
        goCommandPhase();
    }

    /**
     * The start of a round: {@code pbBossBuffPhase} (Battle_StartAndEnd:376), then the command phase
     * ({@code pbCommandPhase}, :378).
     */
    private void goCommandPhase() {
        if (session.safari) {                                    // PokeBattle_SafariZone#pbStartBattle's loop: always the menu
            go(0);
            return;
        }
        if (session.bossBuffPhase()) {
            pursuitContinuation = this::enterCommandMenu;
            queueRoundMessages();
            return;
        }
        enterCommandMenu();
    }

    /**
     * {@code pbCommandPhaseLoop} (Battle_Phase_Command:197-263) for the player: a battler whose commands
     * cannot be shown (:208) keeps its forced choice and the round goes on without a menu.
     */
    private void enterCommandMenu() {
        if (doubles()) {
            beginDoublesCommandPhase();
            return;
        }
        if (!session.canShowCommands()) {
            session.forcedRound();
            queueSessionEvents();
            if (message == null && queue.isEmpty()) {
                afterRound();
            }
            return;
        }
        go(0);
    }

    // =====================================================================
    // pbCommandPhaseLoop(true) for a battle with several positions per side (Battle_Phase_Command:197-263)
    // =====================================================================

    /** {@code !singleBattle?}. */
    private boolean doubles() {
        return !session.battle.singleBattle();
    }

    /** The battler whose command is being chosen; a single battle's player is battler 0. */
    private int actingIndex() {
        return cmdBattler >= 0 ? cmdBattler : 0;
    }

    /** {@code @battlers[idxBattler]} of {@link #actingIndex()}. */
    private Battler fighter() {
        return session.battle.battlerAt(actingIndex());
    }

    /** The start of the player's loop (:204-208): the battlers it asks, in order. */
    private void beginDoublesCommandPhase() {
        cmdOrder = session.commandBattlers();
        cmdPos = 0;
        actioned.clear();
        nextBattlerCommand();
    }

    /** One pass of the loop's top (:204-215), then the main command menu of that battler (:218). */
    private void nextBattlerCommand() {
        if (cmdPos >= cmdOrder.length) {
            finishDoublesCommands();
            return;
        }
        cmdBattler = cmdOrder[cmdPos];
        actioned.add(cmdBattler);                                  // :215
        stage = Stage.BATTLE;
        go(0);
    }

    /** The inner {@code loop} was left with a made choice (:226 / :233 ... {@code break}): the next battler is asked. */
    private void afterBattlerCommand() {
        cmdPos++;
        nextBattlerCommand();
    }

    /** {@code when -1} (:248-254): back to the previous battler's choice. */
    private void backToPreviousBattler() {
        if (actioned.size() <= 1) {                                // :249
            return;
        }
        int previous = actioned.get(actioned.size() - 2);
        Object[] choice = session.battle.choices(previous);
        if (":UseItem".equals(choice[0])) {
            return;                                                // an item is used at command time here: it cannot be taken back
        }
        playCancelSe();
        actioned.remove(actioned.size() - 1);                      // :250
        session.cancelChoice(previous);                            // :252
        actioned.remove(actioned.size() - 1);                      // :253
        for (int i = 0; i < cmdOrder.length; i++) {
            if (cmdOrder[i] == previous) cmdPos = i;
        }
        nextBattlerCommand();
    }

    /** The loop is over (:261-262): the switches registered this round are played first, then the moves. */
    private void finishDoublesCommands() {
        cmdBattler = -1;
        page = 0;
        window = MESSAGE_BOX;
        boolean anySwitch = false;
        for (int idx = 0; idx <= session.battle.maxBattlerIndex(); idx++) {
            if (session.battle.battlerAt(idx) != null && session.battle.pbOwnedByPlayer(idx) && session.switching(idx)) {
                anySwitch = true;
            }
        }
        if (!anySwitch) {
            session.startRound();                                  // pbAttackPhase (moves) and pbEndOfRoundPhase
            queueRoundMessages();
            return;
        }
        if (session.beginSwitchPhase()) {                          // Battle_Phase_Attack:171-184
            pursuitContinuation = this::startSwitches;
            queueRoundMessages();
            return;
        }
        startSwitches();
    }

    /** {@code pbAttackPhaseSwitch} (Battle_Phase_Attack:50-71) over the player's switchers in priority order. */
    private void startSwitches() {
        switchersToPlay.clear();
        for (int idx : session.switchOrder()) {                    // :51-52
            switchersToPlay.add(new int[] { idx, session.switchParty(idx) });
        }
        playNextSwitcher();
    }

    private void playNextSwitcher() {
        int[] next = switchersToPlay.poll();
        if (next == null) {
            session.foeTurn();                                     // pbAttackPhaseMoves
            queueRoundMessages();
            return;
        }
        final int idxBattler = next[0];
        final int idxParty = next[1];
        String recall = session.recallMessage(idxBattler);         // :57 (before the Pursuit hits)
        Runnable after = () -> afterSwitcher(idxBattler);
        if (!session.pursuitOnSwitch(idxBattler)) {                // :59
            queueRecallAndReplace(idxBattler, idxParty, true, after);
            return;
        }
        pursuitContinuation = () -> queueRecallAndReplace(idxBattler, idxParty, false, after);   // :68
        pushMessages(recall, false);
        queueRoundMessages();
    }

    /** {@code b.pbEffectsOnSwitchIn(true)} (:69), then the next switcher. */
    private void afterSwitcher(int idxBattler) {
        session.message = null;
        if (session.switchInEffects(new int[] { idxBattler })) {
            pursuitContinuation = this::playNextSwitcher;
            queueRoundMessages();
            return;
        }
        playNextSwitcher();
    }

    /** {@code pbChooseTarget} (Scene_Commands:419-474): opens the target menu for a move or a Poke Ball. */
    private void openTargetMenu(int slot, int targetType, String ball) {
        int idxBattler = actingIndex();
        targetSlot = slot;
        targetBall = ball;
        pbShowWindow(TARGET_BOX);                                  // :420
        targetTexts = session.targetTexts(idxBattler, targetType); // :423
        targetMode = PBTargets.oneTarget(targetType) ? 0 : 1;      // :425
        targetIndex = session.firstTarget(idxBattler, targetType); // :427
        page = PAGE_TARGET;
        fightMenuRefusal = false;
    }

    /** The loop of {@code pbChooseTarget} (:431-471). */
    private void updateTargetMenu() {
        InputManager input = context.inputManager();
        int oldIndex = targetIndex;                                // :432
        if (targetMode == 0) {                                     // :435 choosing just one target, can change index
            boolean left = input.wasPressed(GameAction.LEFT);
            boolean right = input.wasPressed(GameAction.RIGHT);
            if (left || right) {                                   // :436
                int inc = ((targetIndex % 2) == 0) ? -2 : 2;       // :437
                if (right) inc *= -1;                              // :438
                int indexLength = session.battle.pbSideSize(targetIndex % 2) * 2;   // :439
                int newIndex = targetIndex;
                while (true) {                                     // :441
                    newIndex += inc;
                    if (newIndex < 0 || newIndex >= indexLength) break;                         // :443
                    if (newIndex >= targetTexts.length || targetTexts[newIndex] == null) continue;   // :444
                    targetIndex = newIndex;                        // :445
                    break;
                }
            } else if ((input.wasPressed(GameAction.UP) && (targetIndex % 2) == 0)
                    || (input.wasPressed(GameAction.DOWN) && (targetIndex % 2) == 1)) {         // :448-449
                for (int idxTry : session.battle.pbGetOpposingIndicesInOrder(targetIndex)) {    // :450-451
                    if (idxTry >= targetTexts.length || targetTexts[idxTry] == null) continue;  // :452
                    targetIndex = idxTry;                          // :453
                    break;
                }
            }
            if (targetIndex != oldIndex) {                         // :457
                playCursorSe();                                    // :458
            }
        }
        if (input.wasPressed(GameAction.CONFIRM)) {                // :462
            playDecisionSe();                                      // :464
            finishTargetMenu(targetIndex);
        } else if (input.wasPressed(GameAction.CANCEL)) {          // :466
            playCancelSe();                                        // :468
            finishTargetMenu(-1);
        }
    }

    /** {@code pbSelectBattler(-1)} (:472) and the answer of {@code pbChooseTarget}. */
    private void finishTargetMenu(int idxTarget) {
        int slot = targetSlot;
        String ball = targetBall;
        targetTexts = null;
        targetSlot = -1;
        targetBall = null;
        if (ball != null) {
            if (idxTarget < 0) {                                   // Scene_Commands:345-347 back to the Bag
                go(2);
                return;
            }
            useCommandItem(ball, 0, idxTarget);
            return;
        }
        if (idxTarget < 0) {                                       // pbChooseTarget false: the fight menu is shown again
            page = 1;
            window = FIGHT_BOX;
            return;
        }
        session.registerTarget(actingIndex(), idxTarget);          // Battle_Phase_Command:102
        lastMove[actingIndex()] = slot;                            // Scene_Commands:174
        page = 0;
        afterBattlerCommand();                                     // :89 ret = true: the fight menu is left
    }

    /** A move chosen in a double battle's fight menu (Battle_Phase_Command:83-89). */
    private void chooseMoveInDoubles(int slot) {
        int idxBattler = actingIndex();
        String refusal = session.registerMove(idxBattler, slot);   // :86
        if (refusal != null) {
            session.message = refusal.isEmpty() ? null : refusal;
            refuseMove();
            return;
        }
        openTargetMenu(slot, session.targetType(idxBattler, slot), null);   // :87-88
    }

    /** An item from the Bag in a double battle (Battle_Phase_Command:106-149). */
    private void useBagItemInDoubles(String id, int target) {
        PbsData.Item data = context.pbsData().item(id);
        if (data != null && data.isPokeBall()) {                   // useType 4/9 (Scene_Commands:328-349)
            int[] foes = new int[6];
            int count = 0;
            for (Battler foe : session.battle.eachOtherSideBattler(actingIndex())) {
                foes[count++] = foe.index;
            }
            if (count == 1) {                                      // :330-332 the only opposing battler
                useCommandItem(id, 0, foes[0]);
                return;
            }
            openTargetMenu(-1, PBTargets.Foe, id);                 // :341
            return;
        }
        useCommandItem(id, target, -1);
    }

    /** {@code $BallTypes} (105_PokeBall_CatchEffects:1-30): the order {@code fastCatching} lists the balls in. */
    private static final String[] BALL_TYPES = {"POKEBALL", "GREATBALL", "SAFARIBALL", "ULTRABALL", "MASTERBALL", "NETBALL",
            "DIVEBALL", "NESTBALL", "REPEATBALL", "TIMERBALL", "LUXURYBALL", "PREMIERBALL", "DUSKBALL", "HEALBALL", "QUICKBALL",
            "CHERISHBALL", "FASTBALL", "LEVELBALL", "LUREBALL", "HEAVYBALL", "LOVEBALL", "FRIENDBALL", "MOONBALL", "SPORTBALL",
            "DREAMBALL", "BEASTBALL", "PETBALL", "WILDERNESSBALL"};
    private pokemon.runtime.ui.menu.FastCatchView fastCatch;
    /** {@code @battle.esfc_ball_index}: the ball the last quick catch of this battle stopped on. */
    private int fastCatchIndex;

    /**
     * 156_Scene_Commands:64-76 and 355_ES_s_Fast_Catching:142-160 {@code fastCatching}. Nothing happens in a trainer
     * battle, with switch 196 on, in the Safari Zone, against a Boss (battleRank > 1) or with {@code disablePokeBalls}.
     *
     * @return whether the key was taken (the picker opened, or the "no balls" line was queued)
     */
    private boolean openFastCatch() {
        if (session.battle.trainerBattle || context.gameState().switches().get(196)) return false;      // :65-66
        Battler me = fighter();
        boolean[] boss = {false};
        me.eachOpposing(o -> { if (o.pokemon != null && o.pokemon.battleRank > 1) boss[0] = true; });  // :68-75
        if (boss[0] || session.ballsDisabled()) return false;                                           // :76-77
        java.util.List<String> balls = new ArrayList<>();
        for (String ball : BALL_TYPES) {                                                                // 355:146-150
            if (context.pbsData().item(ball) != null && context.gameState().inventory().has(ball)) balls.add(ball);
        }
        if (balls.isEmpty()) {
            session.message = "没有可供捕捉的精灵球。";                                                       // 355:152-153 pbDisplay
            queueSessionEvents();
            go(0);
            return true;
        }
        fastCatch = new pokemon.runtime.ui.menu.FastCatchView(context, balls, fastCatchIndex,
                context.gameState().trainer().gender == pokemon.runtime.pokemon.PokemonStats.FEMALE);
        return true;
    }

    private void updateFastCatch(InputManager input) {
        pokemon.runtime.ui.menu.FastCatchView.Result result = fastCatch.update(input);
        fastCatchIndex = fastCatch.index();
        if (result == pokemon.runtime.ui.menu.FastCatchView.Result.OPEN) return;
        String ball = fastCatch.ball();
        boolean thrown = result == pokemon.runtime.ui.menu.FastCatchView.Result.THROW;
        fastCatch = null;
        if (thrown) {                                                  // 355:81-87 pbThrowPokeBall(@battler.index, ball): the direct opponent
            Battler target = fighter().pbDirectOpposing(true);
            useCommandItem(ball, 0, target.index);
        }
    }

    private void useCommandItem(String id, int target, int idxTarget) {
        int idxBattler = actingIndex();
        PbsData.Item data = context.pbsData().item(id);
        boolean ball = data != null && data.isPokeBall();
        if (!session.commandItem(id, target, idxBattler, idxTarget)) {
            session.message = "现在无法使用这个道具。";
            queueSessionEvents();
            go(0);
            return;
        }
        page = 0;
        if (ball) {                                                // Battle_Action_UseItem:19-25: it takes all the actions
            queueSessionEvents();
            window = MESSAGE_BOX;
            if (message == null && queue.isEmpty()) afterRound();
            return;
        }
        pursuitContinuation = this::afterBattlerCommand;           // the battler's action is spent: the next one is asked
        queueRoundMessages();
    }

    // =====================================================================
    // Command / menu plumbing (unchanged)
    // =====================================================================

    private void refresh() {
        rows = new ArrayList<>();
        if (page == 0) rows.addAll(Arrays.asList(session.safari
                ? new String[] { "精灵球", "诱饵", "石头", "逃跑" }          // 156_Scene_Commands / 160:pbSafariCommandMenu
                : commandLabels()));
        if (page == 1 && fighter() != null) {
            // FightMenuDisplay#refreshButtonNames (PokeBattle_SceneMenus:337-365)
            // walks "@battler.moves", which is a FIXED four-slot array
            // (Battler_Initialize:98-101 pads it through PokeBattle_Pokemon:498-521;
            // MAX_MOVES = 4 at :222). A blank slot (m.id==0) still owns its cell -
            // the plugin never filters by PP - so the row list is always four long
            // and the cursor index is the SLOT index (:350-352, Scene_Commands:136).
            Battler fighter = fighter();
            for (int i = 0; i < Battler.MOVES_MAX; i++) {
                BattleMove move = fighter.moveSlot(i);
                rows.add(move == null ? "" : move.name());           // :361
            }
        }
        if (page == 2) {
            itemIds.clear();
            for (com.badlogic.gdx.utils.ObjectIntMap.Entry<String> entry : context.gameState().inventory().counts()) {
                PbsData.Item data = context.pbsData().item(entry.key);
                if (entry.value > 0 && data != null && data.battleUse > 0) itemIds.add(entry.key);
            }
            Collections.sort(itemIds);
            for (String id : itemIds) rows.add(context.pbsData().item(id).name + " ×" + context.gameState().inventory().count(id));
        }
        if (page == 3 || page == 4) for (Battler member : session.battle.playerParty()) rows.add(member.name() + " " + member.hp + "/" + member.maxHp());
        cursor.size(rows.size());
    }

    private void go(int next) {
        if (page == 1 && next != 1) session.unregisterMega(actingIndex());        // Battle_Phase_Command:80 (the fight menu was left)
        page = next; cursor.select(0); refresh();
        {
            int remembered = actingIndex();
            if (next == 0) {
                cursor.select(lastCmd[remembered]);                               // Scene_Commands:30 setIndexAndMode(@lastCmd[idxBattler],mode)
            } else if (next == 1 && moveSlotFilled(lastMove[remembered])) {
                cursor.select(lastMove[remembered]);                              // Scene_Commands:94-96 @lastMove[idxBattler]
            }
        }
        megaButton = next == 1 && session.canMega(actingIndex());                 // :73 pbCanMegaEvolve?
        window = next == 1 ? FIGHT_BOX : COMMAND_BOX;
        fightMenuRefusal = false;
        partyScreenOpen = false;
        if (partyView != null) partyView.battleChoose(false);
        // pbShowWindow(COMMAND_BOX) -> the real PScreen_Party for the party row.
        if (next == 3) {
            // pbPartyMenu (Battle_Phase_Command:151-159): pbPartyScreen(idxBattler,
            // false,true,true) - cancel is allowed and a switch is registered.
            openPartyScreen(actingIndex(), false, true, true);
        }
        // pbItemMenu -> the real BW Bag (PokemonBag_Scene, battle=true).
        if (next == 2) {
            if (bagView == null) bagView = new BagView(context);
            if (bagMemory == null) bagMemory = context.gameState().inventory().bagMemory().copy();   // Scene_Commands:239-243 @bagLastPocket/@bagChoices start as the bag's
            bagView.useMemory(bagMemory);
            bagView.battleMode((id, target) -> { bagItem = id; bagTarget = target; });
            bagView.battleItemHost(this::openItemParty);
            itemParty = null;
        }
    }

    /**
     * Queues one message per pbDisplay call; the last one waits for the player.
     * {@link InteractiveBattlePort#MESSAGE_BREAK} separates the calls, while a
     * "\n" inside one call is a line break in the same window
     * (Battle_StartAndEnd:220 "{fullname}\n向你发起挑战！"). Escape codes
     * (\PN, \se[...], colours) are resolved like the event messages.
     */
    private void pushMessages(String text) {
        pushMessages(text, true);
    }

    private void pushMessages(String text, boolean lastPaused) {
        if (text == null || text.isEmpty()) return;
        List<Message> parts = new ArrayList<>();
        for (String part : text.split(InteractiveBattlePort.MESSAGE_BREAK, -1)) {
            if (!part.isEmpty()) parts.add(new Message(cleanMessage(part), false));
        }
        if (parts.isEmpty()) return;
        parts.set(parts.size() - 1, new Message(parts.get(parts.size() - 1).text, lastPaused));
        queue.addAll(parts);
    }

    /** MessageText.clean: resolves \PN and drops the codes the window ignores. */
    private String cleanMessage(String text) {
        return pokemon.runtime.event.MessageText.clean(text, context.gameState());
    }

    // ---------------------------------------------------------------------
    // The command menu's labels (Scene_Commands:6-19)
    // ---------------------------------------------------------------------

    /**
     * {@code pbCommandMenu}'s four entries (Scene_Commands:8-14):
     * {@code _INTL("战斗")} / {@code _INTL("背包")} / {@code _INTL("宝可梦")}
     * and a dynamic fourth one.
     *
     * <p>The fourth entry is {@code _INTL("呼唤")} in a Shadow battle, the
     * plain literal {@code "Run"} while it is the round's first action and
     * {@code "Cancel"} for any later one (:13, converted at :16-17). This
     * project's {@code Data/intl.dat} carries no translation for either literal
     * (the ScriptTexts table has no "Run"/"Cancel" key at all), so the menu
     * shows them in English exactly like the plugin does - {@code "逃跑"} only
     * exists as the Safari Zone's own label (PokeBattle_SafariZone:259).</p>
     */
    private String[] commandLabels() {
        // A Shadow battle needs the SHADOW type and a trainer opponent
        // (Scene_Commands:7); this runtime has no Shadow type, so it is false.
        boolean shadowTrainer = false;
        // A single battle's player chooses one action per round, so it is always the first
        // (pbCommandPhase's firstAction); in a double battle it is the first one asked (:218 actioned.length==1).
        boolean firstAction = actioned.size() <= 1;
        return commandLabels(shadowTrainer, firstAction);
    }

    /** The label set itself, so it can be checked without a GL context. */
    static String[] commandLabels(boolean shadowTrainer, boolean firstAction) {
        return new String[] { "战斗", "背包", "宝可梦",
                shadowTrainer ? "呼唤" : (firstAction ? "Run" : "Cancel") };
    }

    // ---------------------------------------------------------------------
    // Battle music (PokeBattle_Scene:340-350, Scene_Animations:360-371)
    // ---------------------------------------------------------------------

    /**
     * {@code pbWildBattleSuccess} / {@code pbTrainerBattleSuccess}
     * (PokeBattle_Scene:340-350). Both play the winning ME through
     * {@code pbBGMPlay}, so it replaces the battle BGM (streamed, like RMXP's
     * {@code Audio.bgm_play}) until {@code pbBattleAnimation} resumes the
     * overworld track (PField_Visuals:112-114).
     */
    private void playVictoryMe(boolean trainer) {
        pokemon.runtime.audio.AudioManager audio = context.audioManager();
        if (audio == null) {
            return;
        }
        PbsData pbs = context.pbsData();
        int mapId = context.gameState().currentMapId();
        String next = context.gameState().nextBattleME();
        pokemon.runtime.audio.BattleMusic.Track me = trainer
                ? pokemon.runtime.audio.BattleMusic.trainerVictoryMe(pbs, mapId, next, session.trainerData)
                : pokemon.runtime.audio.BattleMusic.wildVictoryMe(pbs, mapId, next);
        if (me != null && me.playable()) {
            context.game().log((trainer ? "trainer" : "wild") + " victory ME: " + me.name);
            audio.playBgm(me.name, me.volume, me.pitch);
        }
    }

    /**
     * {@code pbThrowSuccess} (Scene_Animations:360-371): the capture jingle,
     * played with {@code pbMEPlay} right after the "捉到了{X}！" line
     * (PokeBattle_BattleCommon:129-131).
     */
    private void playCaptureMe() {
        pokemon.runtime.audio.AudioManager audio = context.audioManager();
        if (audio == null) {
            return;
        }
        pokemon.runtime.audio.BattleMusic.Track me = pokemon.runtime.audio.BattleMusic.wildCaptureMe(
                context.pbsData(), context.gameState().currentMapId(),
                context.gameState().nextBattleCaptureME());
        if (me != null && me.playable()) {
            context.game().log("capture ME: " + me.name);
            audio.playMe(me.name, me.volume, me.pitch);
        }
    }

    /** {@code pbAllFainted?(idx)} (PokeBattle_Battle:632): every battler is down. */
    private static boolean allFainted(com.badlogic.gdx.utils.Array<Battler> party) {
        for (Battler battler : party) {
            if (battler != null && !battler.fainted()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shows one battle message right away (pbDisplayPaused when {@code paused},
     * pbDisplayBrief otherwise) and replaces whatever was on screen.
     */
    private void showBattleMessage(String text, boolean paused) {
        message = null;
        holdFrames = 0;
        revealedChars = 0;
        queue.clear();
        pushMessages(text, paused);
        if (!queue.isEmpty()) startNextMessage();
        window = MESSAGE_BOX;
    }

    private void startNextMessage() {
        Message next = queue.peek();
        if (next != null && next.statuses != null && statusHeld) {
            System.arraycopy(next.statuses, 0, shownStatus, 0, Math.min(shownStatus.length, next.statuses.length));
        }
        if (next != null && next.event != null) {
            queue.poll();
            startRoundEvent(next.event);
            return;
        }
        // PokeBattle_Scene:116 pbDisplayMessage -> :101-111 pbWaitMessage: a
        // lingering brief message stays up MESSAGE_PAUSE_TIME more frames first.
        if (briefMessage) {
            pbShowWindow(MESSAGE_BOX);                             // :103
            if (waitMessageFrames < MESSAGE_PAUSE_TIME) {          // :105-107
                waitMessageFrames++;
                return;
            }
            message = null;                                        // :108-109 cw.text = ""; visible = false
            briefMessage = false;                                  // :110
        }
        waitMessageFrames = 0;
        messageReleased = false;
        message = queue.poll();
        revealedChars = 0; holdFrames = 0;
        window = MESSAGE_BOX;
    }

    private void closeMessage() {
        message = null;
        if (!queue.isEmpty()) return;      // startNextMessage runs on the next update
        // Battle_StartAndEnd:194-275 is a straight line: closing one of its
        // messages returns to the step runner (:267 pbDisplayBrief does not even
        // block), not to the command phase.
        if (stage == Stage.OPENING) return;
        if (stage == Stage.EXP_GAIN) {     // the exp sequence advances on each confirm
            stepExpGain();
            return;
        }
        if (stage == Stage.TRAINER_END) {  // the victory lines advance on each confirm
            stepTrainerEnd();
            return;
        }
        if (stage == Stage.CAUGHT_STORE) {
            stepCaughtStore();
            return;
        }
        // A message shown over the party screen is partyScene.pbDisplay
        // (Battle_Action_Switching:138-143): it closes back into that screen.
        if (partyScreenOpen) {
            window = COMMAND_BOX;          // the party screen is drawn only over the command box: show it again
            return;
        }
        // A refused move's line closes back into the fight menu (Scene_Commands:104
        // pbShowWindow(FIGHT_BOX)); the round has not started, so nothing follows.
        if (fightMenuRefusal && page == 1) {
            fightMenuRefusal = false;
            window = FIGHT_BOX;
            return;
        }
        // Battler_ChangeSelf:73 pbFaintBattler runs before the round's exp award
        // (Battler_UseMove:347), so the faint animations come first; then
        // Battle_ExpAndMoveLearning's pbGainExp, which plays before pbEORSwitch
        // (Battle_Phase_EndOfRound:739/744).
        afterMessages();
    }

    /** Enters {@link Stage#TRAINER_END} after a won trainer battle (pbEndOfBattle). */
    private boolean beginTrainerEnd() {
        if (session.result == null || session.result.outcome != BattleResult.Outcome.WIN
                || !session.trainerBattle) {
            return false;
        }
        // Battle_StartAndEnd:453 pbTrainerBattleSuccess: the victory ME starts
        // before the "你打败了" line (PokeBattle_Scene:347-350).
        playVictoryMe(true);
        stage = Stage.TRAINER_END;
        endPhase = EndPhase.WIN_MESSAGE;
        endAppearTimer = 0f;
        endOpponent = 0;
        stepTrainerEnd();
        return true;
    }

    /** The message steps of the trainer-end sequence. */
    private void stepTrainerEnd() {
        switch (endPhase) {
            case WIN_MESSAGE: {
                // Battle_StartAndEnd:456 "你打败了\n{fullname}！".
                endPhase = EndPhase.APPEAR;
                endAppearTimer = 0f;
                endAppearStarted = false;
                // :454-463 one, two or three opponents
                StringBuilder names = new StringBuilder();
                for (int i = 0; i < opponentCount(); i++) {                      // :454-463
                    if (i > 0) names.append(opponentCount() == 3 && i == 1 ? "、" : "和");
                    names.append(session.trainerFullname(i));
                }
                showBattleMessage("你打败了\n" + names + "！", true);
                return;
            }
            case LOSE_MESSAGE: {
                // Lines 466-467: the opponent's LoseText ("..." when empty).
                if (endOpponent + 1 < opponentCount()) {                         // :464 the next opponent slides in
                    endOpponent++;
                    endPhase = EndPhase.APPEAR;
                    endAppearStarted = false;
                    showBattleMessage(session.endSpeech(endOpponent - 1), true);
                    return;
                }
                if (session.prizeMoney > 0) {
                    endPhase = EndPhase.MONEY_MESSAGE;
                } else {
                    endPhase = EndPhase.DONE;
                }
                showBattleMessage(session.endSpeech(endOpponent), true);
                return;
            }
            case MONEY_MESSAGE: {
                // Lines 407-409: "你赢得了${1}！".
                endPhase = EndPhase.DONE;
                showBattleMessage("你赢得了$" + String.format(java.util.Locale.ROOT, "%,d", session.prizeMoney) + "！", true);
                return;
            }
            case DONE:
                // Battle_StartAndEnd:530 @scene.pbEndBattle(@decision): the fade
                // that returns the game to the overworld.
                beginEndBattle();
                return;
            default:
                return;
        }
    }

    /** Drives the trainer-end sequence: the slide, the messages, the fade. */
    private void updateTrainerEnd(float delta) {
        if (message != null) {                     // a victory line waits for input
            tickMessage(context.inputManager());
            return;
        }
        if (endPhase == EndPhase.APPEAR) {
            // Battle_StartAndEnd:465 @scene.pbShowOpponent(i) runs once the
            // "you defeated" line is confirmed: the trainer slides back on-screen
            // (Scene_Animations:71-77) and the loop waits for it (:76).
            if (!endAppearStarted) {
                endAppearStarted = true;
                animations.add(new BattleAnimations.TrainerAppearAnimation(this, endOpponent));   // :465 pbShowOpponent(i)
                return;
            }
            if (animations.isEmpty()) {
                endPhase = EndPhase.LOSE_MESSAGE;
                stepTrainerEnd();
            }
            return;
        }
        stepTrainerEnd();
    }

    // ---------------------------------------------------------------------
    // Scene_Animations:206-268 HP change / damage animations
    // ---------------------------------------------------------------------

    /**
     * One battler taking a damage flash. {@code oldHp} is the HP the data box's
     * bar has to start from ({@code :246 animateHP(t[1],...)}); a negative value
     * means "flash only", which is what {@code pbDamageAnimation} (:224-234) does.
     */
    private static final class DamageFlash {
        final int idxBattler;
        final int effectiveness;
        final int oldHp;
        final int newHp;
        DamageFlash(int idxBattler, int effectiveness, int oldHp, int newHp) {
            this.idxBattler = idxBattler;
            this.effectiveness = effectiveness;
            this.oldHp = oldHp;
            this.newHp = newHp;
        }
    }

    /** {@code damageAnims} (:242): every target of the hit being played. */
    private final java.util.ArrayDeque<DamageFlash> damageFlashes = new java.util.ArrayDeque<>();
    /** True while {@link Stage#HIT} is running (:249-266). */
    private boolean damageFlashRunning;

    /**
     * Plays one non-text round event ({@link Battle.RoundEvent}) - each is a
     * blocking scene call in the plugin; the queue resumes when it is done
     * ({@link #resumeRound()}).
     */
    private void startRoundEvent(Battle.RoundEvent event) {
        switch (event.kind) {
            case HIT:
                // Move_Usage:280 @battle.scene.pbHitAndHPLossAnimation(animArray)
                beginHitAndHpLossAnimation(event.hits);
                return;
            case HP_CHANGE:
                beginHpChanged(event.idxBattler, event.oldHp, event.newHp);
                return;
            case FAINT:
                // Battler_ChangeSelf:73 @battle.scene.pbFaintBattler(self)
                beginFaintBattler(event.idxBattler);
                return;
            case ANIMATION:
                // PokeBattle_Battle:793-799 @scene.pbAnimation / @scene.pbCommonAnimation
                beginAnimation(event.anim);
                return;
            case SE:
                if (context.audioManager() != null) context.audioManager().playSe(event.text, 100, 100);   // pbSEPlay
                resumeRound();
                return;
            case SAFARI_BAIT:
            case SAFARI_ROCK: {
                // @scene.pbThrowBait / pbThrowRock (160_PokeBattle_SafariZone:~262-283)
                briefMessage = false;
                Battler foeBattler = battler(1);
                if (foeBattler == null) {
                    resumeRound();
                    return;
                }
                animations.add(event.kind == Battle.RoundEvent.Kind.SAFARI_BAIT
                        ? new BattleAnimations.ThrowBaitAnimation(this, foeBattler)
                        : new BattleAnimations.ThrowRockAnimation(this, foeBattler));
                ballPhase = 0;
                stage = Stage.BALL;
                return;
            }
            case BALL_THROW:
                beginBallThrow(event.ball, false);
                return;
            case BALL_DEFLECT:
                beginBallThrow(event.ball, true);
                return;
            case BALL_SUCCESS:
                beginBallSuccess(event.ball);
                return;
            case CHANGE_POKEMON: {
                // @scene.pbChangePokemon(battler,battler.pokemon) + pbRefreshOne (Mega evolution:426-427)
                Battler changed = session.battle.battlerAt(event.idxBattler);
                if (changed != null) {
                    heldForm.remove(changed.pokemon);
                    changePokemon(event.idxBattler, changed);
                }
                resumeRound();
                return;
            }
            case SWAP_SPRITES:
                // @scene.pbSwapBattlerSprites(idxA,idxB) (PokeBattle_Battle:601)
                swapBattlerSprites(event.idxBattler, event.oldHp);
                resumeRound();
                return;
            case MEGA_SCENE:
                beginMegaScene(event);
                return;
            case ABILITY_SPLASH_SHOW:
                beginAbilitySplashShow(event);
                return;
            case ABILITY_SPLASH_HIDE:
                beginAbilitySplashHide(event.idxBattler % 2);
                return;
            case BGM:
                // Move_Usage:288 pbBGMPlay(name) -> Audio_Play:52-58 ->
                // Game_System:50-52/65-83 bgm_play_internal: :69 only plays when
                // FileTest.audio_exist?, otherwise the current BGM keeps going.
                playBattleBgm(event.text);
                resumeRound();
                return;
            default:
                resumeRound();
        }
    }

    /** 0 = the throw animation runs, 1 = the capture jingle's wait, 2 = the ball and data box hiding. */
    private int ballPhase;
    /** {@code pbThrowSuccess}'s frame counter (Scene_Animations:364-369). */
    private int ballWaitTicks;
    private int ballTargetIdx;

    /**
     * {@code pbThrow} (Scene_Animations:348-358) / {@code pbThrowAndDeflect}
     * (:389-399): the Poké Ball animation plays until it is done.
     */
    private void beginBallThrow(Battle.RoundEvent.BallCall call, boolean deflect) {
        briefMessage = false;                                      // :349 / :390
        Battler target = battler(call.idxTarget);
        if (target == null) {
            resumeRound();
            return;
        }
        animations.add(deflect
                ? new BattleAnimations.PokeballThrowDeflectAnimation(this, call.ballType, target)
                : new BattleAnimations.PokeballThrowCaptureAnimation(this, call.ballType,
                        call.shakes, call.critical, target, call.showTrainer));
        ballPhase = 0;
        stage = Stage.BALL;
    }

    /**
     * {@code pbThrowSuccess} (Scene_Animations:360-371) and then
     * {@code pbHideCaptureBall} (:373-387).
     */
    private void beginBallSuccess(Battle.RoundEvent.BallCall call) {
        briefMessage = false;                                      // :362
        ballTargetIdx = call.idxTarget;
        playCaptureMe();                                           // :363 pbMEPlay(pbGetWildCaptureME)
        ballWaitTicks = 0;
        ballPhase = 1;
        stage = Stage.BALL;
    }

    private void updateBall() {
        if (ballPhase == 1) {
            if (ballWaitTicks < 40 * 7 / 2) {                      // :368 3.5 seconds
                return;
            }
            // :373-387 the data box and the capture ball disappear together.
            animations.add(new BattleAnimations.DataBoxDisappearAnimation(this, ballTargetIdx));
            animations.add(new BattleAnimations.CaptureBallFadeAnimation(this,
                    sprites.get("captureBall")));
            ballPhase = 2;
            return;
        }
        if (!animations.isEmpty()) {
            return;                                                // :353-356 / :381-385
        }
        if (ballPhase == 2) {
            sprites.remove("captureBall");
        }
        ballPhase = 0;
        resumeRound();
    }

    /** Game_System:65-72 for a name from the battle (volume/pitch 100, Audio_Play:27-28). */
    private void playBattleBgm(String name) {
        pokemon.runtime.audio.AudioManager audio = context.audioManager();
        if (audio == null || name == null || name.isEmpty()) {
            return;
        }
        if (audio.bgmExists(name)) {                               // :69 FileTest.audio_exist?
            audio.playBgm(name, 100, 100);                         // :70-71
        }
    }

    /** Back to the event queue after a blocking round event. */
    /** pbBattleIntroAnimation:41-52: the shiny animations of the wild foes, one after the other. */
    private final java.util.ArrayDeque<Integer> introShinyQueue = new java.util.ArrayDeque<>();
    private boolean introShinyBuilt;
    private boolean introShinyActive;

    /** The level the data box of {@link #expSlot} shows while the exp bar fills (-1 = the live level). */
    private int expLevelShown = -1;
    /** The "LevelUp" animation of the exp sequence is playing. */
    private boolean expAnimActive;

    private void resumeRound() {
        if (expAnimActive) {                                       // back to pbGainExpOne's level loop
            expAnimActive = false;
            stage = Stage.EXP_GAIN;
            if (expLevelShown >= 0) {
                expLevelShown++;                                   // :260-265 the new level shows
            }
            stepExpGain();
            return;
        }
        if (introShinyActive) {                                    // back to pbBattleIntroAnimation
            introShinyActive = false;
            stage = Stage.INTRO;
            return;
        }
        stage = Stage.BATTLE;
        if (queue.isEmpty()) {
            afterMessages();
        }
    }

    // ---------------------------------------------------------------------
    // Scene_Animations:171-203 pbShowAbilitySplash / pbHideAbilitySplash
    // ---------------------------------------------------------------------

    private Battle.RoundEvent splashPending;
    private int splashSide;
    private int splashDelayTicks;
    private int splashPhase;

    private BattleSprite abilityBar(int side) {
        return sprites.get("abilityBar_" + side);
    }

    /** {@code pbShowAbilitySplash(battler,ability)} (:171-184). */
    private void beginAbilitySplashShow(Battle.RoundEvent event) {
        int side = event.idxBattler % 2;
        BattleSprite bar = abilityBar(side);
        if (bar == null) {
            resumeRound();
            return;
        }
        splashPending = event;
        splashSide = side;
        splashDelayTicks = event.oldHp > 0 ? 40 : 0;                       // :181-183 40.times { pbUpdate }
        stage = Stage.ABILITY_SPLASH;
        if (bar.visible) {                                                  // :174 pbHideAbilitySplash(battler) if visible
            splashPhase = 0;
            animations.add(new BattleAnimations.AbilitySplashDisappearAnimation(this, side));
        } else {
            startSplashAppear();
        }
    }

    private void startSplashAppear() {
        BattleSprite bar = abilityBar(splashSide);
        String[] lines = splashPending.text.split("\n", 2);               // :175-176 battler=, ability=
        bar.barLine1 = lines[0] + "发动了特性";                              // AbilitySplashBar#refresh
        bar.barLine2 = lines.length > 1 ? lines[1] : "";
        splashPhase = 1;
        animations.add(new BattleAnimations.AbilitySplashAppearAnimation(this, splashSide));   // :177
    }

    /** {@code pbHideAbilitySplash(battler)} (:186-198). */
    private void beginAbilitySplashHide(int side) {
        BattleSprite bar = abilityBar(side);
        if (bar == null || !bar.visible) {                                  // :188
            resumeRound();
            return;
        }
        splashPending = null;
        splashSide = side;
        splashPhase = 3;
        stage = Stage.ABILITY_SPLASH;
        animations.add(new BattleAnimations.AbilitySplashDisappearAnimation(this, side));
    }

    private void updateAbilitySplash() {
        if (!animations.isEmpty()) {
            return;
        }
        switch (splashPhase) {
            case 0:                                                         // the old bar is gone, show the new text
                startSplashAppear();
                return;
            case 1:                                                         // appeared; delay=true waits a second
                splashPhase = 2;
                return;
            case 2:
                if (splashDelayTicks > 0) {
                    splashDelayTicks--;
                    return;
                }
                splashPending = null;
                resumeRound();
                return;
            default:                                                        // hidden
                resumeRound();
        }
    }

    /**
     * {@code pbHitAndHPLossAnimation} (Scene_Animations:239-268), played by
     * {@code Move_Usage:280} for the targets of one hit. Every target's flash and
     * HP-bar animation start together (:242-247); the stage ends once both the
     * animations and every data box's HP bar are done (:248-266).
     */
    private void beginHitAndHpLossAnimation(Battle.HitEvent[] hits) {
        briefMessage = false;                                      // :240 @briefMessage = false
        // :243-247 "Set up animations": [battler, old HP, effectiveness].
        for (Battle.HitEvent hit : hits) {
            damageFlashes.add(new DamageFlash(hit.idxBattler, hit.effectiveness, hit.oldHp, hit.newHp));
        }
        startDamageFlashes();
    }

    /** {@code pbHPChanged} (Scene_Animations:211-222) for HP lost outside a move's hit. */
    private void beginHpChanged(int idxBattler, int oldHp, int newHp) {
        briefMessage = false;                                      // :212 @briefMessage = false
        // :213-217 the HealthUp/HealthDown common animation only plays with
        // showAnim, which pbReduceHP(dmg,false) passes as false.
        restartHpBar(idxBattler, oldHp, newHp);                    // :218 animateHP(oldHP,battler.hp,...)
        stage = Stage.HP_CHANGE;
        updateHpChanged();
    }

    /** {@code :219-221 while animatingHP; pbUpdate; end}. */
    private void updateHpChanged() {
        if (hpBarsAnimating()) {
            return;
        }
        resumeRound();
    }

    /**
     * {@code pbDamageAnimation} (Scene_Animations:224-234): ONE battler's flash,
     * with no HP-bar animation of its own - the caller's HP change already ran
     * {@code pbHPChanged} (:218-221). The end-of-round weather and status damage
     * (:90/:97/:104/:279/:349/:518) and the ability/item handlers call it; those
     * call sites belong to other batches, this is the prepared entry point.
     */
    private void playDamageAnimation(int idxBattler, int effectiveness) {
        if (damageFlashRunning) {
            return;
        }
        // :227 BattlerDamageAnimation.new(@sprites,@viewport,battler.index,effectiveness)
        damageFlashes.add(new DamageFlash(idxBattler, effectiveness, -1, -1));
        startDamageFlashes();
    }

    /** {@code :242-247} of {@code pbHitAndHPLossAnimation}: start every target at once. */
    private void startDamageFlashes() {
        for (DamageFlash flash : damageFlashes) {
            // :244 BattlerDamageAnimation.new(@sprites,@viewport,t[0].index,t[2])
            animations.add(new BattleAnimations.BattlerDamageAnimation(this,
                    flash.idxBattler, flash.effectiveness));
            if (flash.oldHp >= 0) {
                // :246 @sprites["dataBox_#{t[0].index}"].animateHP(t[1],t[0].hp,t[0].totalhp)
                restartHpBar(flash.idxBattler, flash.oldHp, flash.newHp);
            }
        }
        damageFlashRunning = true;
        stage = Stage.HIT;
        updateDamageFlashes();
    }

    /**
     * The update loop of {@code :248-266}: it only ends when no data box is still
     * animating its HP bar (:252-257) AND every {@code BattlerDamageAnimation} is
     * done (:258-264). {@code :267} then disposes them - the screen's own
     * {@code updateAnimations} already drops finished animations.
     */
    private void updateDamageFlashes() {
        if (!animations.isEmpty() || hpBarsAnimating()) {
            return;
        }
        damageFlashes.clear();
        damageFlashRunning = false;
        resumeRound();
    }

    /** {@code @sprites["dataBox_#{...}"].animatingHP} (:254): a bar still moving. */
    private boolean hpBarsAnimating() {
        for (float t : hpT) {
            if (t < 1f) return true;
        }
        return false;
    }

    /**
     * {@code PokemonDataBox#animateHP} (PokeBattle_SceneElements:175-187) started
     * from the pre-hit HP: the bar begins where it was and reaches the battler's
     * current HP after {@code HP_BAR_CHANGE_TIME} (:181). The engine applies the
     * damage while the round's messages are still on screen, so without this the
     * bar would already have settled before the flash plays.
     */
    private void restartHpBar(int idxBattler, int oldHp, int newHp) {
        Battler b = battler(idxBattler);
        if (b == null) {
            return;
        }
        int side = idxBattler;
        int maxHp = Math.max(1, b.maxHp());
        heldHp[side] = newHp;                                      // the bar's target until the round ends
        hpFrom[side] = Math.max(0f, Math.min(1f, oldHp / (float) maxHp));
        hpTo[side] = Math.max(0f, Math.min(1f, newHp / (float) maxHp));
        hpShown[side] = hpFrom[side];
        hpT[side] = 0f;
    }

    // ---------------------------------------------------------------------
    // Scene_Animations:401-586 pbSaveShadows / pbAnimation / pbCommonAnimation /
    // pbAnimationCore (the PBAnimationPlayerX host)
    // ---------------------------------------------------------------------

    /** {@code pbLoadBattleAnimations} / {@code pbLoadMoveToAnim} (Data_Cache:265-281), cached across battles. */
    private static pokemon.runtime.data.BattleAnimationData cachedAnimationData;
    private static File cachedAnimationRoot;

    private pokemon.runtime.data.BattleAnimationData animationData() {
        File root = context.database().dataRoot();
        if (cachedAnimationData == null || !root.equals(cachedAnimationRoot)) {
            cachedAnimationData = pokemon.runtime.data.BattleAnimationData.load(root);
            cachedAnimationRoot = root;
        }
        return cachedAnimationData;
    }

    /** The animation player running now ({@code animPlayer}, Scene_Animations:553). */
    private BattleAnimationPlayer moveAnim;
    private BattleSprite moveAnimUserSprite;
    private BattleSprite moveAnimTargetSprite;
    /** {@code oldUserX}.. (Scene_Animations:548-551). */
    private float oldUserX;
    private float oldUserY;
    private float oldTargetX;
    private float oldTargetY;
    /** {@code shadows} of pbSaveShadows (:407-411), null when the animation is a common one. */
    private boolean[] savedShadows;

    private final BattleAnimationPlayer.Host animationHost = new BattleAnimationPlayer.Host() {
        @Override public BattleSprites sprites() {
            return sprites;
        }

        @Override public PictureEx.SePlayer sePlayer() {
            return BattleScreen.this.sePlayer();
        }

        @Override public String cryFile(Pokemon pokemon) {
            return BattleScreen.this.cryFile(pokemon);
        }

        @Override public int[] graphicSize(String path) {
            Texture texture = sheetTexture(path, 0);
            return texture == null ? new int[] { 32, 32 } : new int[] { texture.getWidth(), texture.getHeight() };
        }

        @Override public int[] battlerBitmapSize(BattleSprite sprite) {
            Texture texture = ownTexture(sprite);
            return texture == null ? null : new int[] { texture.getWidth(), texture.getHeight() };
        }
    };

    /**
     * {@code pbAnimation} (:510-524) and {@code pbCommonAnimation} (:527-540):
     * resolve the animation, then {@code pbAnimationCore} plays it.
     */
    private void beginAnimation(Battle.AnimationCall call) {
        pokemon.runtime.data.BattleAnimationData data = animationData();
        Battler user = call.idxUser >= 0 ? battler(call.idxUser) : null;
        Battler target = call.idxTarget >= 0 ? battler(call.idxTarget) : null;
        if (call.common) {
            // :528/530 isCommander? is not translated (no commander state on Battler).
            if (call.name == null || call.name.isEmpty()) {                       // :531
                resumeRound();
                return;
            }
            if (!data.available()) {                                              // :534
                resumeRound();
                return;
            }
            pokemon.runtime.data.BattleAnimationData.Animation animation = data.common(call.name);   // :535-539
            if (animation == null) {
                resumeRound();
                return;
            }
            savedShadows = null;
            beginAnimationCore(animation, user, target != null ? target : user, false);   // :537
            return;
        }
        int[] animID = MoveAnimationFinder.findMoveAnimation(data, context.pbsData(),
                call.moveId, call.idxUser, call.hitNum);                          // :511
        if (animID == null || !data.available()) {                                // :512 / :516
            resumeRound();
            return;
        }
        pokemon.runtime.data.BattleAnimationData.Animation animation = data.animation(animID[0]);   // :519/521
        saveShadows();                                                            // :517
        if (animID[1] != 0) {                                                     // :518 On opposing side and using OppMove animation
            beginAnimationCore(animation, target, user, true);                    // :519
        } else {                                                                  // :520
            beginAnimationCore(animation, user, target, false);                   // :521
        }
    }

    /** {@code pbSaveShadows}' first half (:405-412): remember and hide every shadow. */
    private void saveShadows() {
        savedShadows = new boolean[battlerCount()];                               // :407
        for (int i = 0; i < savedShadows.length; i++) {
            BattleSprite shadow = sprites.get("shadow_" + i);                     // :408
            savedShadows[i] = shadow != null && shadow.visible;                   // :409
            if (shadow != null) {
                shadow.visible = false;                                           // :410
            }
        }
    }

    /** {@code pbSaveShadows}' second half (:415-419): restore the shadows' visibility. */
    private void restoreShadows() {
        if (savedShadows == null) {
            return;
        }
        for (int i = 0; i < savedShadows.length; i++) {
            BattleSprite shadow = sprites.get("shadow_" + i);                     // :417
            if (shadow != null) {
                shadow.visible = savedShadows[i];                                 // :418
            }
        }
        savedShadows = null;
    }

    /** {@code pbAnimationCore} up to {@code animPlayer.start} (:542-568). */
    private void beginAnimationCore(pokemon.runtime.data.BattleAnimationData.Animation animation,
                                    Battler user, Battler target, boolean oppMove) {
        if (animation == null) {                                                  // :543
            restoreShadows();
            resumeRound();
            return;
        }
        briefMessage = false;                                                     // :544
        BattleSprite userSprite = user != null ? sprites.get("pokemon_" + user.index) : null;        // :545
        BattleSprite targetSprite = target != null ? sprites.get("pokemon_" + target.index) : null;  // :546
        // Remember the original positions of Pokemon sprites
        oldUserX = userSprite != null ? userSprite.x : 0;                         // :548
        oldUserY = userSprite != null ? userSprite.y : 0;                         // :549
        oldTargetX = targetSprite != null ? targetSprite.x : oldUserX;            // :550
        oldTargetY = targetSprite != null ? targetSprite.y : oldUserY;            // :551
        // Create the animation player
        BattleAnimationPlayer player = new BattleAnimationPlayer(animation, user, target, animationHost, oppMove);   // :553
        // Apply a transformation to the animation based on where the user and
        // target actually are. Get the centres of each sprite.
        int[] userSize = userSprite != null ? animationHost.battlerBitmapSize(userSprite) : null;
        int userHeight = userSize != null ? userSize[1] : 128;                    // :556
        int targetHeight;
        if (targetSprite != null) {                                               // :557
            int[] targetSize = animationHost.battlerBitmapSize(targetSprite);
            targetHeight = targetSize != null ? targetSize[1] : 128;              // :558
        } else {
            targetHeight = userHeight;                                            // :560
        }
        player.setLineTransform(
                PokeBattle_SceneConstants.FOCUSUSER_X, PokeBattle_SceneConstants.FOCUSUSER_Y,
                PokeBattle_SceneConstants.FOCUSTARGET_X, PokeBattle_SceneConstants.FOCUSTARGET_Y,
                oldUserX, oldUserY - userHeight / 2,
                oldTargetX, oldTargetY - targetHeight / 2);                       // :562-566
        player.start();                                                           // :568
        moveAnim = player;
        moveAnimUserSprite = userSprite;
        moveAnimTargetSprite = targetSprite;
        stage = Stage.MOVE_ANIM;
    }

    /**
     * One pass of {@code pbAnimationCore}'s loop (:569-573): the player's update,
     * then {@code pbUpdate}. {@code pbUpdate}'s {@code pbFrameUpdate} runs
     * {@code PokemonBattlerSprite#update} (PokeBattle_Scene:58,
     * PokeBattle_SceneElements:610) AFTER the frame was drawn, so its
     * {@code self.bitmap = @_iconBitmap.bitmap} is applied at the start of the
     * next tick here.
     */
    private void moveAnimTick() {
        resetBattlerBitmaps();
        moveAnim.update();                                                        // :570
    }

    /**
     * {@code self.bitmap = @_iconBitmap.bitmap} (PokeBattle_SceneElements:610):
     * a battler sprite whose bitmap the player swapped goes back to its own, and
     * RGSS resets {@code src_rect} to the whole bitmap when the bitmap changes.
     */
    private void resetBattlerBitmaps() {
        for (BattleSprite sprite : new BattleSprite[] { moveAnimUserSprite, moveAnimTargetSprite }) {
            if (sprite != null && (sprite.sheetName != null || sprite.celBattler != null)) {
                sprite.sheetName = null;
                sprite.celBattler = null;
                sprite.srcX = 0;
                sprite.srcY = 0;
                sprite.srcWidth = -1;
                sprite.srcHeight = -1;
            }
        }
    }

    /** {@code break if animPlayer.animDone?} (:572) and everything after the loop (:574-586). */
    private void updateMoveAnim() {
        if (moveAnim != null && !moveAnim.animDone()) {
            return;
        }
        resetBattlerBitmaps();                                                    // the last frame's pbFrameUpdate
        if (moveAnim != null) {
            moveAnim.dispose();                                                   // :574
        }
        // Return Pokemon sprites to their original positions
        if (moveAnimUserSprite != null) {                                         // :576
            moveAnimUserSprite.x = oldUserX;                                      // :577
            moveAnimUserSprite.y = oldUserY;                                      // :578
            moveAnimUserSprite.updateOrigin();                                    // :579 pbSetOrigin
        }
        if (moveAnimTargetSprite != null) {                                       // :581
            moveAnimTargetSprite.x = oldTargetX;                                  // :582
            moveAnimTargetSprite.y = oldTargetY;                                  // :583
            moveAnimTargetSprite.updateOrigin();                                  // :584
        }
        moveAnim = null;
        moveAnimUserSprite = null;
        moveAnimTargetSprite = null;
        restoreShadows();                                                         // :523 the pbSaveShadows block ends
        resumeRound();
    }

    // ---------------------------------------------------------------------
    // Scene_Animations:299-312 pbFaintBattler
    // ---------------------------------------------------------------------

    /** The battlers whose faint animation is still to play, in order. */
    private final java.util.ArrayDeque<Integer> faintQueue = new java.util.ArrayDeque<>();
    /** 0 = start this battler's pair, 1 = waiting for it. */
    private int faintPhase;

    /**
     * {@code pbFaintBattler} (Scene_Animations:299-312): {@code BattlerFaintAnimation}
     * and {@code DataBoxDisappearAnimation} run together for one battler, called
     * by {@code pbFaint} (Battler_ChangeSelf:73).
     */
    private void beginFaintBattler(int idxBattler) {
        briefMessage = false;                                      // :300 @briefMessage = false
        faintQueue.add(idxBattler);
        stage = Stage.FAINT;
        faintPhase = 0;
        updateFaint();
    }

    /** One update of {@code pbFaintBattler}'s two-animation loop (:304-309). */
    private void updateFaint() {
        if (!animations.isEmpty()) {
            return;                                                // :305-308 both still running
        }
        if (faintPhase == 0) {
            int idxBattler = faintQueue.peek();
            animations.add(new BattleAnimations.BattlerFaintAnimation(this, idxBattler));      // :302
            animations.add(new BattleAnimations.DataBoxDisappearAnimation(this, idxBattler));  // :303
            faintPhase = 1;
            return;
        }
        faintQueue.poll();                                         // :310-311 dispose
        faintPhase = 0;
        if (faintQueue.isEmpty()) {
            resumeRound();
        }
    }

    /**
     * The tail of a round once its messages are done: the damage flash and the
     * HP bars first ({@code Move_Usage:280} runs
     * {@code pbHitAndHPLossAnimation} inside the move), then {@code pbFaint}
     * ({@code Battler_ChangeSelf:73}), then {@code pbGainExp}
     * (Battle_ExpAndMoveLearning's, before {@code pbEORSwitch}).
     */
    private void afterMessages() {
        endRoundPlayback();
        if (beginExpGain()) {
            return;
        }
        afterRound();
    }

    /**
     * The round's events are done: the bars follow the live HP again and a
     * lingering brief line is replaced by whatever the next window shows.
     */
    private void endRoundPlayback() {
        roundPlayback = false;
        statusHeld = false;
        java.util.Arrays.fill(heldHp, -1);
        if (messageReleased && !session.suspended()) {
            message = null;
            messageReleased = false;
        }
        if (!session.suspended()) {
            briefMessage = false;         // the engine's next scene call keeps the lingering brief line
        }
        waitMessageFrames = 0;
    }

    /** Enters {@link Stage#EXP_GAIN} when the last faint awarded exp (pbGainExp). */
    private boolean beginExpGain() {
        if (session.battle.lastExpAwards.size == 0) {
            return false;
        }
        // Battle_ExpAndMoveLearning:5-7: pbGainExp opens with the wild victory
        // ME when the whole opposing side is down and the player's is not
        // (PokeBattle_Scene:340-343 pbWildBattleSuccess).
        if (!session.trainerBattle && allFainted(session.battle.foeParty())
                && !allFainted(session.battle.playerParty())) {
            playVictoryMe(false);
        }
        stage = Stage.EXP_GAIN;
        learnStep = 0;
        forgetView = null;
        heldExp = -1f;
        expPhase = ExpPhase.MESSAGE;
        expAwardIndex = 0;
        expSegmentIndex = 0;
        expAnimT = 1f;
        levelUpWindow = 0;
        return true;
    }

    /** Drives the exp sequence: bar segments, messages and the level-up windows. */
    private void updateExpGain(float delta) {
        if (forgetView != null) {                          // the summary screen's forget mode owns the input
            if (forgetView.update(context.inputManager())) {
                forgetSlot = forgetView.forgetResult();
                forgetView = null;
                learnStep = 4;
            }
            return;
        }
        if (updateChoice()) {                              // pbShowCommands of the move-learning questions
            return;
        }
        if (message != null) {
            if (message.paused) {                  // a line waits for its confirm
                tickMessage(context.inputManager());
                return;
            }
            // A brief exp line lingers while the bar fills: tick it and keep the
            // animation running.
            tickBriefMessage(context.inputManager());
        }
        // Scene_Animations:286-294 pbLevelUp's windows own the input until the
        // player confirms each of them (PItem_Items:554-560).
        if (levelUpWindow > 0) {
            InputManager input = context.inputManager();
            if (input.wasPressed(GameAction.CONFIRM)) {
                levelUpWindow = 0;
                if (expPhase == ExpPhase.LEVEL_GAIN_WINDOW) {
                    expPhase = ExpPhase.LEVEL_TOTAL_WINDOW;
                } else {
                    Battle.ExpAward award = expAwardIndex < session.battle.lastExpAwards.size
                            ? session.battle.lastExpAwards.get(expAwardIndex) : null;
                    // Battle_ExpAndMoveLearning:286-296: the moves of this level
                    // are learned after both windows.
                    expPhase = award != null && !award.movesToLearn.isEmpty()
                            ? ExpPhase.LEARN_QUESTION : ExpPhase.DONE;
                }
                stepExpGain();
            }
            return;
        }
        if (expFlash > 0f) {
            // updateExpAnimation:363-365: the animation ends when the flash does.
            expFlash -= delta;
            if (expFlash <= 0f) {
                expFlash = 0f;
                // Battle_ExpAndMoveLearning:249-259: a filled bar means a level was gained - "LevelUp" plays on the battler,
                // and only then does its level number change (:260-265 calcStats / pbRefreshOne).
                if (expSlot >= 0 && expLevelShown >= 0) {
                    expAnimActive = true;
                    beginAnimation(new Battle.AnimationCall(true, -1, "LevelUp", expSlot, expSlot, 0));
                } else {
                    stepExpGain();
                }
            }
            return;
        }
        if (expAnimT < 1f) {
            // PokemonDataBox#animateExp:195: EXP_BAR_FILL_TIME fills the whole
            // level, so a small gain fills proportionally faster - but never
            // faster than the minimum that keeps the fill sound audible.
            float span = Math.max(1e-4f, Math.abs(expAnimTo - expAnimFrom));
            float duration = Math.max(MIN_EXP_SEGMENT_SECONDS, EXP_BAR_FILL_TIME * span);
            expAnimT = Math.min(1f, expAnimT + delta / duration);
            expShown[expSlot] = expAnimFrom + (expAnimTo - expAnimFrom) * expAnimT;
            if (expAnimT >= 1f) {
                // updateExpAnimation:352-371: the bar is done, stop the fill SE;
                // a full bar flashes and plays the full sound instead.
                stopSe();
                if (expAnimTo >= 1f) {
                    playSe("Exp full");                            // :358
                    expFlash = EXP_FLASH_TIME;
                } else {
                    stepExpGain();
                }
            }
            return;
        }
        stepExpGain();
    }

    /** One step of pbGainExpOne: the line, the bar segments, the level-up. */
    private void stepExpGain() {
        com.badlogic.gdx.utils.Array<Battle.ExpAward> awards = session.battle.lastExpAwards;
        switch (expPhase) {
            case MESSAGE: {
                if (expAwardIndex >= awards.size) {
                    // pbGainExp:58-61: the exp pot line, then the battle ends.
                    if (session.expPotMessage != null) {
                        String line = session.expPotMessage;
                        session.expPotMessage = null;
                        showExpMessage(line);
                        return;
                    }
                    finishExpGain();
                    return;
                }
                Battle.ExpAward award = awards.get(expAwardIndex);
                expSlot = fieldSlotOf(award.pokemon);                        // :220 pbFindBattler(idxParty)
                if (award.expGained <= 0) {
                    // A level-locked gain only fed the exp pot (lines 167-186);
                    // the plugin returns before the exp line.
                    expPhase = ExpPhase.DONE;
                    stepExpGain();
                    return;
                }
                if (award.announce != null) {                                // :51 the Exp All's line, once, paused
                    String line = award.announce;
                    award.announce = null;
                    showExpMessage(line);
                    return;
                }
                expPhase = ExpPhase.SEGMENTS;
                expSegmentIndex = 0;
                expMoveIndex = 0;
                expAnimT = 1f;
                expLevelShown = award.oldLevel;                              // the box keeps the old level until each level-up plays
                // Lines 193-197 (the outsider wording differs). The line lingers
                // while the bar fills, so it is a brief message and the bar
                // starts in the same beat.
                if (award.showMessage) {                                     // :195 showMessages (false for the Exp All's others)
                    showExpBriefMessage(award.outsider
                            ? award.pokemon.name + "获得了得到增幅的" + award.expGained + "点经验值！"
                            : award.pokemon.name + "获得了" + award.expGained + "点经验值！");
                }
                stepExpGain();
                return;
            }
            case SEGMENTS: {
                Battle.ExpAward award = awards.get(expAwardIndex);
                if (expSlot < 0) {
                    expSegmentIndex = award.segments.size;                   // Scene_Animations:274 pbEXPBar returns without a battler
                }
                if (expSegmentIndex < award.segments.size) {
                    if (expAnimT >= 1f) {
                        int[] segment = award.segments.get(expSegmentIndex++);
                        int range = segment[1] - segment[0];
                        expAnimFrom = range <= 0 ? 0f : (segment[2] - segment[0]) / (float) range;
                        expAnimTo = range <= 0 ? 0f : (segment[3] - segment[0]) / (float) range;
                        expAnimT = 0f;
                        // PokemonDataBox#animateExp:197.
                        playSe("Pkmn exp gain");
                        // updateExpAnimation:354-358: the bar reached the level's
                        // end (a level-up) flashes and plays "Exp full". This
                        // project ships no Audio/SE/Exp full, so Game_System#se_play
                        // (Game_System:236) plays nothing - the plugin line is
                        // transcribed as-is rather than pointed at another file.
                        if (expAnimTo >= 1f) {
                            playSe("Exp full");
                        }
                    }
                    return;
                }
                expLevelShown = -1;                                          // every level is in: the live level
                expPhase = award.pokemon.level > award.oldLevel ? ExpPhase.LEVEL_MESSAGE : ExpPhase.DONE;
                stepExpGain();
                return;
            }
            case LEVEL_MESSAGE: {
                Battle.ExpAward award = awards.get(expAwardIndex);
                System.arraycopy(award.oldStats, 0, levelUpStats, 0, levelUpStats.length);
                expPhase = ExpPhase.LEVEL_GAIN_WINDOW;
                // :283 pbDisplayPaused("{1}升到了{2}级！") { pbSEPlay("Pkmn level up") }.
                // The project ships no Audio/SE/Pkmn level up, so Game_System#se_play
                // (Game_System:236) plays nothing; the call is transcribed as-is
                // instead of being pointed at another file.
                playSe("Pkmn level up");
                showExpMessage(award.pokemon.name + "升到了" + award.pokemon.level + "级！");
                return;
            }
            case LEVEL_GAIN_WINDOW:
            case LEVEL_TOTAL_WINDOW: {
                // Scene_Animations:287-293 pbLevelUp: the first window shows the
                // gains ("最大HP<r>+{1}..."), the second the new totals. Each plays
                // pbPlayDecisionSE and waits for a confirm (PItem_Items:547-562
                // pbTopRightWindow loops until Input::C).
                levelUpWindow = expPhase == ExpPhase.LEVEL_GAIN_WINDOW ? 1 : 2;
                playDecisionSe();
                return;
            }
            case LEARN_QUESTION: {
                Battle.ExpAward award = awards.get(expAwardIndex);
                expMoveIndex = 0;
                if (award.movesToLearn.size == 1) {
                    // :293-294 one move is learned without asking.
                    expPhase = ExpPhase.LEARN_MOVE;
                    stepExpGain();
                    return;
                }
                expPhase = ExpPhase.LEARN_MOVE;
                // :293-295 with several moves the player is asked first.
                // Battle_ExpAndMoveLearning:294 pbDisplayConfirm -> PokeBattle_Scene:199 default 1.
                showChoice("要" + award.pokemon.name + "立即学习招式吗？", 1, answer -> {
                    if (answer != 0) {
                        expPhase = ExpPhase.DONE;
                    }
                    stepExpGain();
                });
                return;
            }
            case LEARN_MOVE: {
                // pbLearnMove (Battle_ExpAndMoveLearning:312-343).
                Battle.ExpAward award = awards.get(expAwardIndex);
                if (learnStep == -1) {
                    return;                                       // waiting for the question / the forget screen
                }
                if (learnStep == 0) {
                    if (expMoveIndex >= award.movesToLearn.size) {
                        expPhase = ExpPhase.DONE;
                        stepExpGain();
                        return;
                    }
                    learnMove = award.movesToLearn.get(expMoveIndex++);
                    expPhase = ExpPhase.LEARN_MOVE;
                    for (Pokemon.MoveSlot known : award.pokemon.moves) {          // :317 return if m.id==newMove
                        if (known.move != null && known.move.internalName.equals(learnMove.internalName)) {
                            stepExpGain();
                            return;
                        }
                    }
                    // :319-327 a blank slot learns it at once.
                    if (award.pokemon.moves.size < 4) {
                        award.pokemon.moves.add(new Pokemon.MoveSlot(learnMove));
                        playSe("Pkmn move learnt");                   // :324
                        showExpBriefMessage(award.pokemon.name + "学会了" + learnMove.name + "！");
                        return;
                    }
                    // :329 four moves already.
                    showExpMessage(award.pokemon.name + "想要学会" + learnMove.name
                            + "。\n可是它已经学会四个招式了。");
                    learnStep = 1;
                    return;
                }
                switch (learnStep) {
                    case 1:                                       // :330 pbDisplayConfirm
                        learnStep = -1;
                        showChoice("遗忘一个招式来学习" + learnMove.name + "吗？", 1, answer -> {
                            learnStep = answer == 0 ? 2 : 0;      // declining leaves the move unlearned (no line in the plugin)
                        });
                        return;
                    case 2:                                       // :331
                        showExpMessage("要忘记哪一个招式？");
                        learnStep = 3;
                        return;
                    case 3:                                       // :332 @scene.pbForgetMove -> pbStartForgetScreen
                        forgetView = SummaryView.forForget(context, award.pokemon, learnMove);
                        learnStep = -1;
                        return;
                    case 4: {                                     // :333-340
                        if (forgetSlot < 0) {
                            learnStep = 0;
                            stepExpGain();
                            return;
                        }
                        oldMoveName = award.pokemon.moves.get(forgetSlot).move.name;
                        award.pokemon.moves.set(forgetSlot, new Pokemon.MoveSlot(learnMove));   // :336 PBMove.new: full PP
                        showExpMessage("一，二…………当当!");           // :337
                        learnStep = 5;
                        return;
                    }
                    case 5:
                        showExpMessage(award.pokemon.name + "遗忘了" + oldMoveName + "。\n以及……");   // :338
                        learnStep = 6;
                        return;
                    default:
                        playSe("Pkmn move learnt");               // :339
                        showExpBriefMessage(award.pokemon.name + "学会了" + learnMove.name + "！");
                        learnStep = 0;
                        return;
                }
            }
            case DONE:
            default: {
                expAwardIndex++;
                expPhase = ExpPhase.MESSAGE;
                stepExpGain();
                return;
            }
        }
    }

    /**
     * The player-side position of the Pokemon that gains exp ({@code pbFindBattler(idxParty)}, :220), -1 for a party member
     * that is not on the field: it gets no bar and no level change on the box of the one that fights (Exp All / Exp Share).
     */
    private int fieldSlotOf(Pokemon pokemon) {
        for (int idx = 0; idx < battlerCount(); idx += 2) {
            Battler b = battler(idx);
            if (b != null && b.pokemon == pokemon) return idx;
        }
        return -1;                                              // pbFindBattler(idxParty) is nil for a party member that is not on the field
    }

    /** Clears the played awards and leaves the settlement. */
    private void finishExpGain() {
        expLevelShown = -1;
        session.battle.lastExpAwards.clear();
        afterRound();
    }

    /** Shows one {@code pbDisplayPaused} line of the exp sequence. */
    private void showExpMessage(String text) {
        showBattleMessage(text, true);
    }

    /**
     * The "exp gained" line is a brief message: it appears while the bar fills
     * (PokeBattle_Scene#pbDisplayMessage's {@code @briefMessage}, "a brief
     * message lingers on-screen while other things happen").
     */
    private void showExpBriefMessage(String text) {
        showBattleMessage(text, false);
    }

    /** pbSEPlay for the settlement sounds (Battle_ExpAndMoveLearning:197/358). */
    private void playSe(String name) {
        if (context.audioManager() != null) {
            context.audioManager().playSe(name, 100, 100);
        }
    }

    /**
     * {@code pbPlayDecisionSE} (Audio_Play:249-259). This project's
     * System.rxdata has an empty {@code decision_se} name and RMXP's
     * RPG::System has no {@code sounds}, so :256-257 plays
     * {@code pbSEPlay("GUI sel decision",80)} (Audio/SE/GUI sel decision.ogg
     * exists). pbTopRightWindow (PItem_Items:553) plays it as each level-up
     * window appears.
     */
    private void playDecisionSe() {
        if (context.audioManager() != null) {
            context.audioManager().playSe("GUI sel decision", 80, 100);   // :257
        }
    }

    /**
     * {@code pbPlayCursorSE} (Audio_Play:236-246): {@code cursor_se} is empty in
     * System.rxdata, so :243-244 {@code pbSEPlay("GUI sel cursor",80)}
     * (Audio/SE/GUI sel cursor.ogg exists).
     */
    private void playCursorSe() {
        if (context.audioManager() != null) {
            context.audioManager().playSe("GUI sel cursor", 80, 100);     // :244
        }
    }

    /** {@code pbPlayCancelSE} (Audio_Play:262-272): no {@code cancel_se} in System.rxdata, so {@code GUI sel cancel}. */
    private void playCancelSe() {
        if (context.audioManager() != null) {
            context.audioManager().playSe("GUI sel cancel", 80, 100);     // :270
        }
    }

    /** pbSEStop (Audio_Play:223): the fill sound stops when the bar is done. */
    private void stopSe() {
        if (context.audioManager() != null) {
            context.audioManager().stopSe();
        }
    }

    /**
     * updateExpAnimation:354-362: the level-up flash over the player's data box
     * ({@code self.flash(Color.new(64,200,248,192),8)} and the same on its
     * sub-sprites).
     */
    private void drawExpFlash(SpriteBatch batch, float h) {
        if (expFlash <= 0f) {
            return;
        }
        float alpha = (expFlash / EXP_FLASH_TIME) * (192f / 255f);
        BattleSprite box = expSlot > 0 || doubles() ? sprites.get("dataBox_" + Math.max(0, expSlot)) : null;
        if (box != null && box.bitmapWidth > 0 && box.bitmapHeight > 0) {
            MenuPanel.fill(batch, assets, box.x, h - box.y - box.bitmapHeight, box.bitmapWidth, box.bitmapHeight,
                    64 / 255f, 200 / 255f, 248 / 255f, alpha);                // the flash is the receiving data box's own
            return;
        }
        MenuPanel.fill(batch, assets, 0f, h - 5f - 84f, 260f, 84f,
                64 / 255f, 200 / 255f, 248 / 255f, alpha);
    }

    /**
     * {@code pbLevelUp} (Scene_Animations:286-294): two
     * {@code pbTopRightWindow} calls. The first is
     * {@code "最大HP<r>+{1}\r\n攻击<r>+{2}..."} with the gains, the second the
     * same labels with the new totals; {@code <r>} right-aligns the rest of the
     * line. The window is 198 wide at {@code x = Graphics.width-width, y = 0}
     * (PItem_Items:548-552).
     */
    private void drawLevelUpWindow(SpriteBatch batch, float w, float h) {
        if (levelUpWindow == 0) {
            return;
        }
        String[] labels = { "最大HP", "攻击", "防御", "特攻", "特防", "速度" };
        int[] now = new int[6];
        Battle.ExpAward award = expAwardIndex < session.battle.lastExpAwards.size
                ? session.battle.lastExpAwards.get(expAwardIndex) : null;
        Pokemon pokemon = award == null ? null : award.pokemon;
        if (pokemon != null) {
            now = new int[] { pokemon.maxHp(), pokemon.attack(), pokemon.defense(),
                    pokemon.spAtk(), pokemon.spDef(), pokemon.speed() };
        }
        float width = 198f;                                     // :549
        // Window_AdvancedTextPokemon#resizeToFit (:213-221) sets the height to
        // dims[1]+borderY, and resizeToFitInternal (:233-243) makes dims[1] the
        // bottom of the last character - six lines at @lineHeight 32, so 192.
        // With borderY 32 (SpriteWindow:453-459) that is 224.
        float height = labels.length * ADVANCED_TEXT_LINE_HEIGHT + WINDOW_BORDER;
        float x = w - width;                                    // :550
        float top = 0f;                                         // :551
        MenuPanel.window(batch, assets, skin, x, h - top - height, width, height);
        Color[] colors = MenuPanel.textColors(skin);
        // Window_AdvancedTextPokemon#refresh (SpriteWindow_text:101-105) draws
        // into a contents bitmap of (width-borderX) x (height-borderY) placed at
        // the window's startX/startY (SpriteWindow:695-696), so line i starts at
        // 16+32*i and a "<r>" line is right-aligned to 16 + (198-32).
        for (int i = 0; i < labels.length; i++) {
            // :288-290 the first window's values are the differences, :292-293
            // the second window's are the new stats themselves.
            String value = levelUpWindow == 1
                    ? "+" + (now[i] - levelUpStats[i])
                    : String.valueOf(now[i]);
            float textY = h - (top + WINDOW_CONTENTS_ORIGIN + i * ADVANCED_TEXT_LINE_HEIGHT);
            font.draw(batch, labels[i], x + WINDOW_CONTENTS_ORIGIN, textY, colors[0], colors[1]);
            font.drawRight(batch, value, x + width - WINDOW_CONTENTS_ORIGIN, textY, colors[0], colors[1]);
        }
    }

    /** MessageConfig::pbGetSystemTextSpeed reveal step (per update). */
    private int revealStep() {
        int speed = context.settings() == null ? 1 : context.settings().textspeed;
        switch (speed) {
            case 0: return (com.badlogic.gdx.Gdx.graphics.getFrameId() % 3 == 0) ? 1 : 0;
            case 2: return 3;
            default: return 1;
        }
    }

    /**
     * One update of the message window: reveal the text, hold it, then close it
     * (pbDisplayMessage's 1s / pbDisplayPausedMessage's 3s, or on input).
     */
    private void tickMessage(InputManager input) {
        int total = message.text.length();
        boolean skip = input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL);
        if (revealedChars < total) {
            revealedChars = Math.min(total, revealedChars + revealStep());
            if (skip) revealedChars = total;
            return;
        }
        if (message.brief) {
            // PokeBattle_Scene:130-134: "A brief message lingers on-screen while
            // other things happen" - @briefMessage = true and pbDisplayMessage
            // returns, the text staying in the window.
            briefMessage = true;
            waitMessageFrames = 0;
            messageReleased = true;
            if (queue.isEmpty()) afterMessages();
            return;
        }
        holdFrames++;
        int limit = message.paused ? PAUSED_PAUSE_TIME : MESSAGE_PAUSE_TIME;
        if (skip || holdFrames >= limit) closeMessage();
    }

    /**
     * A brief message (PokeBattle_Scene#pbDisplayMessage with brief=true): it
     * lingers "while other things happen", so it only closes on a confirm or
     * when the next message replaces it - it never gates the caller.
     */
    private void tickBriefMessage(InputManager input) {
        int total = message.text.length();
        boolean skip = input.wasPressed(GameAction.CONFIRM) || input.wasPressed(GameAction.CANCEL);
        if (revealedChars < total) {
            revealedChars = Math.min(total, revealedChars + revealStep());
            if (skip) revealedChars = total;
            return;
        }
        if (skip) {
            message = null;
            if (!queue.isEmpty()) startNextMessage();
        }
    }

    /** The last {@link #stateSignature()} written to the log. */
    private String lastStateSignature;

    /**
     * Diagnostic only (not plugin behaviour): one log line whenever the
     * screen's flow state changes, so a hang can be located from the log.
     */
    private String stateSignature() {
        BattleSprite trainer = sprites.get("trainer_1");
        return "stage=" + stage
                + " page=" + page + " window=" + window
                + " msg=" + (message == null ? "-" : (messageReleased ? "released" : (message.paused ? "paused" : "shown")))
                + " queue=" + queue.size()
                + " choice=" + (choiceOptions != null)
                + " switch=" + switchStep
                + " engine=" + (session.suspended() ? session.request().kind.name() : "off")
                + " anims=" + animations.size()
                + " sendOut=" + (sendOut == null ? "-" : (sendOut.done() ? "done" : "running"))
                + " party=" + partyScreenOpen
                + " result=" + (session.result == null ? "-" : session.result.outcome)
                + " trainer1=" + (trainer == null ? "-" : (trainer.visible ? "v" : "h") + Math.round(trainer.x))
                + " box0=" + boxSignature(sprites.get("dataBox_0"))
                + " box1=" + boxSignature(sprites.get("dataBox_1"));
    }

    /** Diagnostic: a data box's visibility, opacity, x and whether its battler is fainted. */
    private static String boxSignature(BattleSprite box) {
        if (box == null) return "-";
        return (box.visible ? "v" : "h") + Math.round(box.opacity) + "@" + Math.round(box.x)
                + (box.battler != null && box.battler.fainted() ? "F" : "");
    }

    private void logStateIfChanged() {
        String sig = stateSignature();
        if (!sig.equals(lastStateSignature)) {
            lastStateSignature = sig;
            context.game().log("[battle-state] " + sig);
        }
    }

    private void update(float delta) {
        logStateIfChanged();
        float safeDelta = Math.max(0f, delta);
        updateBars(safeDelta);
        // One tick per 40fps frame, like PokeBattle_Scene#pbUpdate: every call
        // runs pbGraphicsUpdate, which advances @frameCounter and @animations.
        frameAccum += safeDelta;
        int steps = 0;
        while (frameAccum >= TICK && steps < 8) {
            frameAccum -= TICK;
            steps++;
            updateAnimations();
            if (sendOut != null) {
                sendOut.update();
            }
            if (moveAnim != null && stage == Stage.MOVE_ANIM) {
                moveAnimTick();
            }
            if (megaScene != null && stage == Stage.MEGA_SCENE) {
                megaScene.tick();
            }
            if (stage == Stage.BALL && ballPhase == 1) {
                ballWaitTicks++;
            }
        }

        // ---- battle intro (Scene_Animations:5 pbBattleIntroAnimation) ----
        if (stage == Stage.INTRO) {
            if (stepIntro()) {
                stage = Stage.OPENING;
                planIndex = 0;
            }
            return;
        }
        // ---- the full Mega Evolution scene (Mega evolution:400-417) ----
        if (stage == Stage.MEGA_SCENE) {
            updateMegaScene();
            return;
        }
        // ---- Battle_StartAndEnd:194-275 pbStartBattleSendOut ----
        if (stage == Stage.OPENING) {
            stepOpening();
            return;
        }
        // ---- pbGainExpOne's exp award sequence (Battle_ExpAndMoveLearning:99-308) ----
        if (stage == Stage.EXP_GAIN) {
            updateExpGain(safeDelta);
            return;
        }
        // ---- pbEndOfBattle's WIN branch (Battle_StartAndEnd:444-473) ----
        if (stage == Stage.TRAINER_END) {
            updateTrainerEnd(safeDelta);
            return;
        }
        if (stage == Stage.CAUGHT_STORE) {
            updateCaughtStore();
            return;
        }
        // ---- pbEndBattle's fade (PokeBattle_Scene:296-303) ----
        if (stage == Stage.END_BATTLE) {
            updateEndBattle();
            return;
        }
        // ---- pbHitAndHPLossAnimation (Scene_Animations:239-268) ----
        if (stage == Stage.HIT) {
            updateDamageFlashes();
            return;
        }
        // ---- pbShowAbilitySplash / pbHideAbilitySplash (Scene_Animations:171-198) ----
        if (stage == Stage.ABILITY_SPLASH) {
            updateAbilitySplash();
            return;
        }
        // ---- pbFaintBattler (Scene_Animations:299-312) ----
        if (stage == Stage.FAINT) {
            updateFaint();
            return;
        }
        // ---- pbHPChanged (Scene_Animations:211-222) ----
        if (stage == Stage.HP_CHANGE) {
            updateHpChanged();
            return;
        }
        // ---- pbAnimationCore's loop (Scene_Animations:568-574) ----
        if (stage == Stage.MOVE_ANIM) {
            updateMoveAnim();
            return;
        }
        // ---- pbThrow / pbThrowSuccess (Scene_Animations:348-387) ----
        if (stage == Stage.BALL) {
            updateBall();
            return;
        }

        InputManager input = context.inputManager();

        // ---- pbShowCommands (PokeBattle_Scene:202-237) owns the input ----
        if (updateChoice()) {
            return;
        }
        // ---- Battle_Action_Switching:256-262 pbRecallAndReplace ----
        if (updateSwitch()) {
            return;
        }
        // ---- message window (pbDisplayMessage / pbDisplayPausedMessage) ----
        if (message != null && !messageReleased) {
            tickMessage(input);
            return;
        }
        if (sendOut != null) {
            return;                                                // a send-out is still playing
        }
        if (!queue.isEmpty()) { startNextMessage(); return; }
        if (session.result != null) { beginEndBattle(); return; }   // :530 pbEndBattle
        if (stage != Stage.BATTLE) return;   // still sending out / waiting

        // ---- the battle party screen is the real PScreen_Party ----
        if (page == 3 && partyView != null) {
            if (partyView.update(input)) {
                finishPartyScreen(partyView.chosenIndex());
                if (!partyScreenOpen) {
                    // The command-menu form of pbPartyScreen returns to the
                    // command phase unless a switch was registered (which
                    // finishPartyScreen has already taken over) or the answer went to the engine.
                    if (partyHandedOver) {
                        partyHandedOver = false;
                        return;
                    }
                    if (switchStep != SwitchStep.NONE) {
                        return;
                    }
                    go(0);
                }
            }
            return;
        }

        // ---- the Bag opened by the engine to pick a Poke Ball (PokeBattle_BOSS:168-170) ----
        if (page == 2 && bagView != null && ballBagOpen) {
            if (bagView.update(input)) {
                ballBagOpen = false;
                page = 0;
                session.answer(bagView.pickedItem());              // the item picked, or null: ball>0 is false (:170)
                queueRoundMessages();
            }
            return;
        }

        // ---- the battle bag is the real BW Bag (pbItemMenu) ----
        if (page == 2 && bagView != null) {
            if (itemParty != null) {                                  // the party screen of a bag item owns the input
                if (itemParty.update(input)) {
                    finishItemParty();
                }
                return;
            }
            if (bagView.update(input)) {
                completeBagItem();
            }
            return;
        }

        // ---- the target menu (Scene_Commands:419-474) ----
        if (page == PAGE_TARGET) {
            updateTargetMenu();
            return;
        }

        // ---- 355_ES_s_Fast_Catching: the Poke Ball picker, opened by Input::A on the command menu (156_Scene_Commands:64-76) ----
        if (fastCatch != null) {
            updateFastCatch(input);
            return;
        }
        if (page == 0 && !session.safari && input.wasPressed(GameAction.SPECIAL) && openFastCatch()) {
            return;
        }

        // ---- command / fight menus ----
        if (input.wasPressed(GameAction.CANCEL) && page != 0) {
            if (page == 1) lastMove[actingIndex()] = cursor.index();                 // Scene_Commands:174
            go(0);
            return;
        }
        if (input.wasPressed(GameAction.CANCEL) && page == 0 && doubles() && actioned.size() > 1) {
            backToPreviousBattler();                                               // Scene_Commands:53 mode==1 Cancel
            return;
        }
        if (page == 1) {
            // FightMenuDisplay: a 2x2 grid over the four slots (LEFT/RIGHT +/-1,
            // UP/DOWN +/-2). Scene_Commands:119-131: LEFT and UP always move,
            // RIGHT and DOWN only when the destination slot holds a move
            // ("battler.moves[cw.index+1] && ...id>0" at :122/:128).
            int oldIndex = cursor.index();                                  // :116 oldIndex = cw.index
            // :119-131 if/elsif: one direction per frame.
            if (input.wasPressed(GameAction.LEFT)) {
                if ((cursor.index() & 1) == 1) cursor.select(cursor.index() - 1);
            } else if (input.wasPressed(GameAction.RIGHT)) {
                if ((cursor.index() & 1) == 0 && moveSlotFilled(cursor.index() + 1)) cursor.select(cursor.index() + 1);
            } else if (input.wasPressed(GameAction.UP)) {
                if ((cursor.index() & 2) == 2) cursor.select(cursor.index() - 2);
            } else if (input.wasPressed(GameAction.DOWN)) {
                if ((cursor.index() & 2) == 0 && moveSlotFilled(cursor.index() + 2)) cursor.select(cursor.index() + 2);
            }
            if (cursor.index() != oldIndex) playCursorSe();                  // :132
            // Input::A toggles the registered Mega Evolution (Battle_Phase_Command:77); it is performed in the attack phase.
            if (input.wasPressed(GameAction.SPECIAL) && megaButton) session.toggleMega(actingIndex());
        } else {
            // pbCommandMenuEx (Scene_Commands:33-46): the four commands are a 2x2
            // grid - LEFT/RIGHT move within a row, UP/DOWN between the rows.
            int oldIndex = cursor.index();                                  // :34
            if (input.wasPressed(GameAction.LEFT)) {
                if ((cursor.index() & 1) == 1) cursor.select(cursor.index() - 1);   // :37-38
            } else if (input.wasPressed(GameAction.RIGHT)) {
                if ((cursor.index() & 1) == 0) cursor.select(cursor.index() + 1);   // :39-40
            } else if (input.wasPressed(GameAction.UP)) {
                if ((cursor.index() & 2) == 2) cursor.select(cursor.index() - 2);   // :41-42
            } else if (input.wasPressed(GameAction.DOWN)) {
                if ((cursor.index() & 2) == 0) cursor.select(cursor.index() + 2);   // :43-44
            }
            if (cursor.index() != oldIndex) playCursorSe();                  // :46
        }
        if (!input.wasPressed(GameAction.CONFIRM) || rows.isEmpty()) return;
        int pick = cursor.index();
        if (page == 0 || page == 1) {
            playDecisionSe();                       // Scene_Commands:49 / :135 pbPlayDecisionSE
        }
        if (page == 0 && session.safari) {
            lastCmd[0] = pick;                                                     // Scene_Commands:51
            session.safariCommand(pick);                                           // 160_PokeBattle_SafariZone:388-452
            queueSessionEvents();
            if (message == null && queue.isEmpty()) {
                afterRound();
            }
            return;
        }
        if (page == 0) {
            lastCmd[actingIndex()] = pick;                                         // Scene_Commands:51
            if (pick == 3 && doubles() && actioned.size() > 1) {
                backToPreviousBattler();                                           // Scene_Commands:17 "Run" converted to "Cancel"
                return;
            }
            if (pick == 3) {
                // pbRunMenu (Battle_Phase_Command:161-164): pbRun's 0 leaves the
                // command phase running, -1/1 spend the round (:234-242).
                int ran = session.escape();
                if (ran == -1) {
                    session.foeTurn();             // pbAttackPhaseMoves / pbEndOfRoundPhase
                }
                queueSessionEvents();
                if (message == null && queue.isEmpty()) {
                    afterRound();
                }
                return;
            }
            // pbFightMenu:68: "return pbAutoChooseMove(idxBattler) if
            // !pbCanShowFightMenu?(idxBattler)" - with no slot left to choose the
            // plugin never shows the fight menu, it announces
            // "{1}没有招式可使用了！" and struggles (Battle_Action_AttacksPriority:36-67).
            if (pick == 0 && doubles() && !session.canShowFightMenu(actingIndex())) {
                session.autoChooseMoveOnly(actingIndex());          // :68 pbAutoChooseMove: registers it, the round does not start
                queueSessionEvents();
                pursuitContinuation = this::afterBattlerCommand;    // :70 / :226 break
                if (message == null && queue.isEmpty()) {
                    afterRound();
                }
                return;
            }
            if (pick == 0 && !session.canShowFightMenu()) {
                session.autoChooseMove();
                queueSessionEvents();
                if (message == null && queue.isEmpty()) {
                    afterRound();
                }
                return;
            }
            go(pick + 1);
        } else if (page == 1) {
            // Battle_Phase_Command:84-86: a blank slot is refused outright and the
            // menu stays open ("next false"), while a slot pbRegisterMove refuses
            // (no PP left) shows its pbDisplayPaused line over the menu.
            if (!moveSlotFilled(pick)) return;
            if (doubles()) {
                chooseMoveInDoubles(pick);
                return;
            }
            if (session.chooseMove(pick)) {
                lastMove[actingIndex()] = pick;                                    // Scene_Commands:174 @lastMove[idxBattler] = cw.index
                queueSessionEvents();
            } else {
                refuseMove();
            }
        }
    }

    /**
     * {@code @battler.moves[slot] && @battler.moves[slot].id>0}
     * (Scene_Commands:122/128, Battle_Phase_Command:84-85): whether a slot holds a
     * move. A slot past the last real move is the plugin's {@code PBMove.new(0)}
     * blank entry, and one past slot 3 is the {@code nil} Ruby's {@code moves[4]}
     * reads.
     */
    private boolean moveSlotFilled(int slot) {
        Battler fighter = session.battle == null ? null : fighter();
        return fighter != null && fighter.moveSlot(slot) != null;
    }

    /**
     * {@code pbFightMenu}'s refusal branch (Battle_Phase_Command:84-86): the menu
     * does not close and the round does not start. {@code pbRegisterMove}'s own
     * refusal is a {@code pbDisplayPaused} line
     * (Battle_Action_AttacksPriority:9-11), i.e. a message box that closes back
     * into the menu (Scene_Commands:104/137 {@code pbShowWindow(FIGHT_BOX)} after
     * {@code needFullRefresh = true}).
     */
    private void refuseMove() {
        if (session.message == null || session.message.isEmpty()) return;
        pushMessages(session.message);
        fightMenuRefusal = true;
    }

    @Override public void render(float delta) {
        update(delta);
        if (finished) return;
        viewport.apply(); camera.update(); batch.setProjectionMatrix(camera.combined);
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        batch.begin();
        renderSprites(w, h);
        // The lineups were hidden by the send-out's fade animation; they only
        // come back on a switch (Battle_Action_Switching:259). PokeBattle_Scene
        // Elements:374-386/602-618: while the player is choosing a command the
        // data box and the Pokemon bob 2px in opposite directions.
        if (stage == Stage.EXP_GAIN) {
            drawExpFlash(batch, h);
            if (message != null) drawMessageWindow(batch, w, h);
            if (levelUpWindow > 0) drawLevelUpWindow(batch, w, h);
        }
        // PokeBattle_Scene:298 pbShowWindow(BLANK): during pbEndBattle no window
        // is drawn, only the fading sprites.
        if ((stage == Stage.TRAINER_END || stage == Stage.CAUGHT_STORE) && message != null) {
            drawMessageWindow(batch, w, h);
        }
        // pbHitAndHPLossAnimation / pbHPChanged / pbFaintBattler run with the
        // message box up, a brief line lingering in it (PokeBattle_Scene:130-134).
        if ((stage == Stage.HIT || stage == Stage.HP_CHANGE || stage == Stage.FAINT || stage == Stage.ABILITY_SPLASH
                || stage == Stage.MOVE_ANIM || stage == Stage.BALL) && window == MESSAGE_BOX) {
            drawMessageWindow(batch, w, h);
        }
        if (stage == Stage.BATTLE || stage == Stage.OPENING || stage == Stage.EXP_GAIN) {
            if (stage == Stage.BATTLE && partyScreenOpen && partyView != null
                    && window == COMMAND_BOX) {
                partyView.render(batch, assets, font, skin, smallFont);
            } else if (stage == Stage.BATTLE && page == 2 && itemParty != null && window == COMMAND_BOX) {
                itemParty.render(batch, assets, font, skin, smallFont);
            } else if (stage == Stage.BATTLE && page == 2 && bagView != null && window == COMMAND_BOX) {
                bagView.render(batch, assets, font, skin, smallFont);
            } else if (window == MESSAGE_BOX) {
                // PokeBattle_Scene:89: the message box stays up while the round's
                // events play, even between two lines (:138 only hides the text).
                if (message != null || roundPlayback) drawMessageWindow(batch, w, h);
            } else if (stage == Stage.BATTLE && window == TARGET_BOX) {
                drawTargetMenu(batch, w, h);
            } else if (stage == Stage.BATTLE && window == FIGHT_BOX) {
                drawFightMenu(batch, w, h);
            } else if (stage == Stage.BATTLE && window == COMMAND_BOX) {
                drawCommandWindow(batch, w, h);
            }
            if (fastCatch != null && stage == Stage.BATTLE) {
                fastCatch.render(batch, assets, font, skin, smallFont);
            }
            // pbShowCommands's command window (PokeBattle_Scene:202-237).
            if (choiceOptions != null) {
                drawChoiceWindow(batch, w, h);
            }
        }
        if (forgetView != null) {
            forgetView.render(batch, assets, font, skin);          // PokemonSummaryScreen#pbStartForgetScreen
        }
        if (caughtDex != null) {
            caughtDex.render(batch, assets, font, smallFont);      // PokemonPokedexInfoScreen#pbDexEntry
        }
        batch.end();
    }

    // =====================================================================
    // The sprite table (RMXP's viewport draw order)
    // =====================================================================

    /**
     * Draws {@code @sprites} in ascending z - RMXP's own order - applying each
     * sprite's origin ({@code setPictureSprite:483-498}), zoom, angle, opacity
     * and RGSS tone/color.
     */
    private void renderSprites(float w, float h) {
        boolean shaderBound = false;
        boolean toneSet = false;
        boolean colorSet = false;
        int blend = 0;
        for (BattleSprite sprite : sprites.drawOrder()) {
            if (!sprite.visible || sprite.opacity <= 0f || sprite.blinkHidden) {
                continue;
            }
            // RGSS blend_type: 0 normal, 1 additive, 2 subtractive.
            int wantedBlend = sprite.kind == BattleSprite.Kind.DATA_BOX || sprite.kind == BattleSprite.Kind.PLANE_COLOR
                    ? 0 : sprite.blendType;
            if (wantedBlend != blend) {
                applyBlend(wantedBlend);
                blend = wantedBlend;
            }
            if (sprite.kind == BattleSprite.Kind.PLANE_COLOR) {
                // ColoredPlane (Planes:165-183): a black bitmap that sprite.color
                // blends towards its rgb by alpha/255, drawn at sprite.opacity.
                if (shaderBound) { batch.setShader(null); shaderBound = false; }
                float amount = sprite.color[3] / 255f;
                batch.setColor(sprite.color[0] / 255f * amount, sprite.color[1] / 255f * amount,
                        sprite.color[2] / 255f * amount, sprite.opacity / 255f);
                batch.draw(assets.pixel(), 0f, 0f, w, h);
                batch.setColor(Color.WHITE);
                continue;
            }
            if (sprite.kind == BattleSprite.Kind.DATA_BOX) {
                if (shaderBound) { batch.setShader(null); shaderBound = false; }
                // A data box is drawn by this runtime's own code, not by the
                // sprite loop below, so it must not inherit the previous
                // sprite's batch colour: RMXP tints each sprite independently
                // (PokemonDataBox is its own Sprite), and a battler fading out
                // (BattlerFaintAnimation's moveOpacity) would otherwise fade the
                // data box with it.
                batch.setColor(Color.WHITE);
                drawDataBox(batch, sprite, h);
                continue;
            }
            if (sprite.kind == BattleSprite.Kind.ABILITY_BAR) {
                if (shaderBound) { batch.setShader(null); shaderBound = false; }
                drawAbilityBar(sprite, h);
                continue;
            }
            if (sprite.kind == BattleSprite.Kind.SAFARI_BOX) {
                if (shaderBound) { batch.setShader(null); shaderBound = false; }
                batch.setColor(Color.WHITE);
                Texture box = texture(sprite.name);                                // SafariDataBox#refresh (160:~60-74)
                if (box != null) batch.draw(box, sprite.x, h - sprite.y - box.getHeight());
                font.draw(batch, "狩猎球", sprite.x + 40f, h - (sprite.y + 8f), SAFARI_BASE, SAFARI_SHADOW);
                font.draw(batch, "剩余：" + session.ballCount, sprite.x + 40f, h - (sprite.y + 38f), SAFARI_BASE, SAFARI_SHADOW);
                continue;
            }
            Texture texture = spriteTexture(sprite);
            if (texture == null) {
                continue;
            }
            int srcW = sprite.srcWidth >= 0 ? sprite.srcWidth : texture.getWidth();
            int srcH = sprite.srcHeight >= 0 ? sprite.srcHeight : texture.getHeight();
            // RGSS draws only the part of src_rect that lies inside the bitmap
            // (an animation cel may point past a cropped sheet).
            srcW = Math.min(srcW, texture.getWidth() - sprite.srcX);
            srcH = Math.min(srcH, texture.getHeight() - sprite.srcY);
            float drawW = srcW * sprite.zoomX;
            float drawH = srcH * sprite.zoomY;
            if ((drawW <= 0f || drawH <= 0f) && sprite.kind != BattleSprite.Kind.PLANE_BITMAP) {
                continue;
            }
            boolean needsTone = sprite.tone[0] != 0f || sprite.tone[1] != 0f
                    || sprite.tone[2] != 0f || sprite.tone[3] != 0f;
            boolean needsColor = sprite.color[3] != 0f;
            if ((needsTone || needsColor) && spriteShader != null && spriteShader.isCompiled()) {
                if (!shaderBound) {
                    batch.setShader(spriteShader.program());
                    shaderBound = true;
                    toneSet = false;
                    colorSet = false;
                }
                if (!toneSet || needsTone) {
                    batch.flush();
                    spriteShader.setTone(sprite.tone);
                    toneSet = true;
                }
                if (!colorSet || needsColor) {
                    batch.flush();
                    spriteShader.setColor(sprite.color);
                    colorSet = true;
                }
            } else {
                if (shaderBound) {
                    batch.setShader(null);
                    shaderBound = false;
                }
                if ((needsTone || needsColor) && !spriteShaderWarned) {
                    spriteShaderWarned = true;
                    context.game().log("battle sprite shader unavailable; tone/color ignored");
                }
            }
            batch.setColor(1f, 1f, 1f, sprite.opacity / 255f);
            if (sprite.kind == BattleSprite.Kind.PLANE_BITMAP) {
                // AnimatedPlane (Planes:190-232) / LargePlane#tileBitmap (:141-157):
                // the bitmap tiled over the screen from (-ox,-oy).
                int tileW = texture.getWidth();
                int tileH = texture.getHeight();
                int left = (int) (0 - sprite.ox);
                int top = (int) (0 - sprite.oy);
                while (left > 0) left -= tileW;
                while (top > 0) top -= tileH;
                for (int tileY = top; tileY < h; tileY += tileH) {
                    for (int tileX = left; tileX < w; tileX += tileW) {
                        batch.draw(texture, tileX, h - tileY - tileH);
                    }
                }
                continue;
            }
            float x = sprite.x - sprite.ox * sprite.zoomX;
            float y = h - (sprite.y + sprite.bobOffsetY - sprite.oy * sprite.zoomY) - drawH;
            batch.draw(texture, x, y, sprite.ox * sprite.zoomX, drawH - sprite.oy * sprite.zoomY,
                    drawW, drawH, 1f, 1f, -sprite.angle,
                    sprite.srcX, sprite.srcY, srcW, srcH, sprite.mirror, false);
        }
        if (shaderBound) {
            batch.setShader(null);
        }
        if (blend != 0) {
            applyBlend(0);
        }
        batch.setColor(Color.WHITE);
    }

    private static final Color BAR_TEXT = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color BAR_SHADOW = new Color(48f / 255f, 48f / 255f, 48f / 255f, 1f);

    /** AbilitySplashBar (PokeBattle_SceneElements:405-497): the bar graphic and its two lines of text. */
    private void drawAbilityBar(BattleSprite bar, float h) {
        Texture texture = texture(bar.name);
        if (texture == null) {
            return;
        }
        int srcH = bar.srcHeight >= 0 ? bar.srcHeight : texture.getHeight() / 2;
        batch.setColor(1f, 1f, 1f, bar.opacity / 255f);
        batch.draw(texture, bar.x, h - bar.y - srcH, texture.getWidth(), srcH,
                0, bar.srcY, texture.getWidth(), srcH, false, false);
        batch.setColor(Color.WHITE);
        // :476-490 left aligned at x=10 for our side, right aligned at width-24 for the foe
        if (bar.barSide == 0) {
            smallFont.draw(batch, bar.barLine1, bar.x + 10f, h - (bar.y + 2f), BAR_TEXT, BAR_SHADOW);
            smallFont.draw(batch, bar.barLine2, bar.x + 10f, h - (bar.y + 32f), BAR_TEXT, BAR_SHADOW);
        } else {
            float right = bar.x + texture.getWidth() - 8f - 16f;
            smallFont.drawRight(batch, bar.barLine1, right, h - (bar.y + 2f), BAR_TEXT, BAR_SHADOW);
            smallFont.drawRight(batch, bar.barLine2, right, h - (bar.y + 32f), BAR_TEXT, BAR_SHADOW);
        }
    }

    /** RGSS {@code Sprite#blend_type}: 0 normal, 1 additive, 2 subtractive (dst - src*alpha). */
    private void applyBlend(int blendType) {
        batch.flush();
        if (blendType == 0) {
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                    com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
            com.badlogic.gdx.Gdx.gl.glBlendEquation(com.badlogic.gdx.graphics.GL20.GL_FUNC_ADD);
        } else {
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
            com.badlogic.gdx.Gdx.gl.glBlendEquation(blendType == 2
                    ? com.badlogic.gdx.graphics.GL20.GL_FUNC_REVERSE_SUBTRACT
                    : com.badlogic.gdx.graphics.GL20.GL_FUNC_ADD);
        }
    }

    /** The texture behind a sprite, by kind. */
    private Texture spriteTexture(BattleSprite sprite) {
        switch (sprite.kind) {
            case BACKDROP:
            case BASE:
            case IMAGE:
            case TRAINER_BACK:
            case TRAINER_FRONT:
            case SHADOW:
                return texture(sprite.name);
            case POKEMON:
                // PBAnimationPlayerX swaps the sprite's bitmap (:815/:840-844).
                if (sprite.sheetName != null) {
                    return sheetTexture(sprite.sheetName, sprite.sheetHue);
                }
                if (sprite.celBattler != null) {
                    return battlerTexture(sprite.celBattler, sprite.celBack);
                }
                return ownTexture(sprite);
            case ANIM_CEL:
                if (sprite.sheetName != null) {
                    return sheetTexture(sprite.sheetName, sprite.sheetHue);
                }
                return sprite.celBattler == null ? null : battlerTexture(sprite.celBattler, sprite.celBack);
            case PLANE_BITMAP:
                return sprite.name == null || sprite.name.isEmpty() ? null : sheetTexture(sprite.name, 0);
            default:
                return null;
        }
    }

    /** A POKEMON sprite's own bitmap ({@code @_iconBitmap.bitmap}), or null. */
    private Texture ownTexture(BattleSprite sprite) {
        if (sprite.battler == null || sprite.battler.pokemon == null) {
            return null;
        }
        return pokemonTexture(sprite.battler.pokemon, sprite.back);
    }

    private Texture battlerTexture(Battler battler, boolean back) {
        return battler.pokemon == null ? null : pokemonTexture(battler.pokemon, back);
    }

    /**
     * {@code AnimatedBitmap.new("Graphics/Animations/"+name, hue)}: the path loses
     * its image extension like {@code pbResolveBitmap} (FileTests:47) strips it.
     */
    private Texture sheetTexture(String path, int hue) {
        if (path == null) {
            return null;
        }
        String logical = path.startsWith("Graphics/") ? path.substring("Graphics/".length()) : path;
        int slash = logical.lastIndexOf('/');
        if (slash < 0) {
            return null;
        }
        String directory = logical.substring(0, slash);
        String file = logical.substring(slash + 1).replaceAll("(?i)\\.(bmp|png|gif|jpg|jpeg)$", "");
        if (file.isEmpty()) {
            return null;
        }
        return hue == 0 ? assets.graphic(directory, file) : assets.graphicHue(directory, file, hue);
    }

    // =====================================================================
    // Data boxes / messages / menus (unchanged rendering)
    // =====================================================================

    /** PokemonDataBox#animateHP / #animateExp stepping. */
    private void updateBars(float delta) {
        for (int side = 0; side < heldHp.length; side++) {
            // The exp settlement drives the player's exp bar itself
            // (pbEXPBar's per-level segments).
            if (side == expSlot && stage == Stage.EXP_GAIN) {
                continue;
            }
            Battler b = session.battle.battlerAt(side);
            int shownHp = b == null ? 0 : (heldHp[side] >= 0 ? heldHp[side] : b.hp);
            float hpTarget = b == null || b.maxHp() <= 0 ? 1f : Math.max(0f, shownHp / (float) b.maxHp());
            float expTarget = b == null || b.pokemon == null ? 0f : expFraction(b.pokemon);
            if (side == expSlot && heldExp >= 0f) {
                expTarget = heldExp;
            }
            if (!barsReady) { hpShown[side] = hpTarget; expShown[side] = expTarget; hpTo[side] = hpTarget; expTo[side] = expTarget; continue; }
            if (Math.abs(hpTarget - hpTo[side]) > 1e-4f) { hpFrom[side] = hpShown[side]; hpTo[side] = hpTarget; hpT[side] = 0f; }
            if (hpT[side] < 1f) {
                hpT[side] = Math.min(1f, hpT[side] + delta / HP_BAR_CHANGE_TIME);
                hpShown[side] = hpFrom[side] + (hpTo[side] - hpFrom[side]) * hpT[side];
            } else hpShown[side] = hpTarget;
            if (Math.abs(expTarget - expTo[side]) > 1e-4f) { expFrom[side] = expShown[side]; expTo[side] = expTarget; expT[side] = 0f; }
            if (expT[side] < 1f) {
                expT[side] = Math.min(1f, expT[side] + delta / EXP_BAR_FILL_TIME);
                expShown[side] = expFrom[side] + (expTo[side] - expFrom[side]) * expT[side];
            } else expShown[side] = expTarget;
        }
        barsReady = true;
    }

    /** The battler index of a {@code dataBox_i} sprite. */
    private static int slotOf(BattleSprite box) {
        try {
            return Integer.parseInt(box.key.substring(box.key.lastIndexOf('_') + 1));
        } catch (RuntimeException e) {
            return box.battler == null ? 0 : Math.max(0, box.battler.index);
        }
    }

    /** PokemonDataBox: databox_normal[_foe] + overlay_hp/exp/lv. */
    private void drawDataBox(SpriteBatch batch, BattleSprite box, float h) {
        Battler b = box.battler;
        // During a round's events the box stays until pbFaintBattler's
        // DataBoxDisappearAnimation hides it (PokeBattle_SceneAnimations:221).
        int slot = slotOf(box);
        if (b == null || b.pokemon == null || (b.fainted() && heldHp[slot] < 0)) return;
        boolean foe = (slot & 1) != 0;
        // initializeDataBoxGraphic (PokeBattle_SceneElements:43-56): the HP numbers and the Exp bar only exist in the
        // regular data box, which a side of one Pokemon uses on the player's side.
        boolean showHpAndExp = sideSize(slot) == 1 && !foe;
        Texture texture = texture(box.name);
        float x = box.x;
        float topY = box.y + box.bobOffsetY;
        if (texture != null) batch.draw(texture, x, h - topY - texture.getHeight(),
                texture.getWidth(), texture.getHeight());
        float baseX = x + 8f;
        // PokemonDataBox#refresh blits its textPos list at :243 and its imagePos
        // list at :276, so every text line below is drawn before the icons.
        // Draw Pokémon's name (:218-222): a name wider than 116px is shifted left
        // by the overflow, so a long name stays inside the box.
        String boxName = b.name();
        float nameWidth = font.width(boxName);
        float nameOffset = nameWidth > 116f ? nameWidth - 116f : 0f;
        font.draw(batch, boxName, baseX + 24f - nameOffset, h - (topY + 12f), NAME_BASE, NAME_SHADOW);
        // PokeBattle_SceneElements:223-229: the ♂♀ carries the gender colour
        // (both sides; genderless species draw nothing).
        int gender = b.pokemon.displayGender();
        if (gender == PokemonStats.MALE) {
            font.draw(batch, "\u2642", baseX + 1f, h - (topY + 12f), MALE_BASE, NAME_SHADOW);
        } else if (gender == PokemonStats.FEMALE) {
            font.draw(batch, "\u2640", baseX + 1f, h - (topY + 12f), FEMALE_BASE, NAME_SHADOW);
        }
        // Foe battle rank multiplier (:230-235); opposes?(0) is this side's index
        // parity (PokeBattle_Battler:784-787), i.e. the foe side here.
        if (foe && b.pokemon.battleRank > 1) {
            // Ruby's BOSS_HP_RANK[rank] is nil past the table (PokeBattle_BOSS:6
            // tells authors to stay within 1..7), which _INTL renders as nothing.
            int rank = b.pokemon.battleRank;
            int mult = rank < BOSS_HP_RANK.length ? BOSS_HP_RANK[rank] : 0;
            font.draw(batch, "×" + mult, baseX + 180f, h - (topY + 35f), RANK_BASE, RANK_SHADOW);
        }
        drawImg(batch, "Pictures/Battle", "overlay_lv", baseX + 138f, topY + 22f, h);
        // Draw Pokémon's level (:236-242): the digits at (+158, 20), or a "???"
        // above level 200.
        if (b.level() <= 200) {
            drawBattleNumber(batch, expLevelShown >= 0 && slot == expSlot ? expLevelShown : b.level(),
                    baseX + 158f, topY + 20f, h, false);   // :239
        } else {
            font.draw(batch, "???", baseX + 158f, h - (topY + 15f),
                    LEVEL_UNKNOWN_BASE, LEVEL_UNKNOWN_SHADOW);                        // :241
        }
        // PokemonDataBox#refreshHP:281-289: HP numbers only on the player's side,
        // drawn with icon_numbers at (baseX+84, topY+44), y+2 inside.
        if (showHpAndExp) {
            int maxHp = Math.max(1, b.maxHp());
            int hpValue = Math.round(hpShown[slot] * maxHp);
            hpValue = Math.max(0, Math.min(maxHp, hpValue));
            drawBattleNumber(batch, hpValue, baseX + 138f, topY + 46f, h, true);
            drawBattleNumber(batch, -1, baseX + 138f, topY + 46f, h, false);
            drawBattleNumber(batch, maxHp, baseX + 150f, topY + 46f, h, false);
        }
        int side = slot;
        Texture hpBar = assets.graphic("Pictures/Battle", "overlay_hp");
        // At 0 HP the gauge is empty: the 1px minimum below only applies while
        // HP remains.
        if (hpBar != null && hpShown[side] > 1e-4f) {
            float hpFrac = hpShown[side];
            float hpw = Math.max(1f, hpFrac * 96f);
            hpw = Math.round(hpw / 2f) * 2f;
            int zone = 0;
            if (hpFrac <= 0.5f) zone = 1;
            if (hpFrac <= 0.25f) zone = 2;
            float barX = foe ? baseX + 77f : baseX + 96f;
            if (foe) {
                batch.draw(hpBar, barX + 96f - hpw, h - (topY + 40f) - 6f, hpw, 6f, 0, zone * 6, (int) hpw, 6, false, false);
            } else {
                batch.draw(hpBar, barX, h - (topY + 40f) - 6f, hpw, 6f, 0, zone * 6, (int) hpw, 6, false, false);
            }
        }
        Texture expBar = assets.graphic("Pictures/Battle", "overlay_exp");
        if (expBar != null && showHpAndExp) {
            float frac = expShown[side];
            float ew = Math.round(frac * 160f / 2f) * 2f;
            // Crop the 160x4 strip like the HP bar does, so the cells keep their width.
            if (ew > 0) batch.draw(expBar, baseX + 48f, h - (topY + 66f) - 4f, ew, 4f,
                    0, 0, (int) ew, expBar.getHeight(), false, false);
        }
        // ---- the rest of imagePos, blitted by pbDrawImagePositions (:276) ----
        Pokemon iconPkmn = b.pokemon;
        // Draw shiny icon (:244-253); the padding differs per side.
        if (iconPkmn.shiny) {
            int shinyX = foe ? 6 : 10;
            int shinyY = foe ? -32 : -26;
            drawImg(batch, "Pictures", iconPkmn.superShiny ? "superShiny" : "shiny",
                    baseX + shinyX + 190f, topY + shinyY + 52f, h);
        }
        // Draw Mega Evolution/Primal Reversion icon (:255-264)
        if (iconPkmn.isMega()) {
            drawImg(batch, "Pictures/Battle", "icon_mega", baseX + 202f, topY + 14f, h);
        } else if (iconPkmn.isPrimal()) {
            // :258 computes primalX but never reads it, so both pushes use +202.
            if (isSpecies(iconPkmn, "KYOGRE")) {
                drawImg(batch, "Pictures/Battle", "icon_primal_Kyogre", baseX + 202f, topY + 14f, h);
            } else if (isSpecies(iconPkmn, "GROUDON")) {
                drawImg(batch, "Pictures/Battle", "icon_primal_Groudon", baseX + 202f, topY + 14f, h);
            }
        }
        // Draw owned icon (foe Pokémon only) (:266-268)
        if (foe && registered(b)) {
            drawImg(batch, "Pictures/Battle", "icon_own", baseX + 4f, topY + 38f, h);
        }
        // Draw status icon (:269-275): one 18px row of the 8-row
        // Graphics/Pictures/Battle/icon_statuses strip, at (+3, 36). The width
        // argument -1 of :274 means "the whole bitmap", i.e. all 18 columns.
        if (!statusHeld) {
            shownStatus[slot] = b.status == null ? "" : "POISON".equals(b.status) && b.toxic > 0 ? "POISON!" : b.status;
        }
        int statusRow = statusIconRow(shownStatus[slot]);
        if (statusRow > 0) {
            Texture statusIcons = assets.graphic("Pictures/Battle", "icon_statuses");
            if (statusIcons != null) {
                batch.draw(statusIcons, baseX + 3f, h - (topY + 36f) - STATUS_ICON_HEIGHT,
                        STATUS_ICON_HEIGHT, STATUS_ICON_HEIGHT,
                        0, (statusRow - 1) * STATUS_ICON_HEIGHT, STATUS_ICON_HEIGHT, STATUS_ICON_HEIGHT,
                        false, false);
            }
        }
        // pbSetSpritesToColor (MessageConfig:436/608) colours every sprite,
        // including the data boxes; those do not go through BattleSpriteShader
        // (they are drawn by this method), so the same black is laid over the
        // panel's rectangle instead.
        if (box.color[3] > 0f) {
            float boxW = texture != null ? texture.getWidth() : 260f;
            float boxH = texture != null ? texture.getHeight() : 84f;
            MenuPanel.fill(batch, assets, x, h - topY - boxH, boxW, boxH,
                    0f, 0f, 0f, Math.min(1f, box.color[3] / 255f));
        }
    }

    /**
     * The {@code icon_statuses} row the data box blits: {@code @battler.status}
     * as a PBStatuses id (:4-11 NONE0/SLEEP1/POISON2/BURN3/PARALYSIS4/FROZEN5),
     * drawn from row {@code s-1} (:274), with row 8 standing in for badly
     * poisoned (:272, {@code statusCount>0} i.e. this runtime's
     * {@link Battler#toxic}). The plugin's remaining ids - FROSTBITE 6 and
     * DROWSY 7 - have no producer anywhere in this runtime (the only
     * {@code setStatus} calls are Battle.java:819-841), so their rows never show.
     *
     * @return the 1-based sheet row, or 0 when the battler has no status (:270)
     */
    private static int statusIconRow(String shown) {
        if (shown == null || shown.isEmpty()) return 0;
        boolean toxic = shown.endsWith("!");
        String status = toxic ? shown.substring(0, shown.length() - 1) : shown;
        int row;
        if ("SLEEP".equals(status)) row = 1;
        else if ("POISON".equals(status)) row = 2;
        else if ("BURN".equals(status)) row = 3;
        else if ("PARALYSIS".equals(status)) row = 4;
        else if ("FROZEN".equals(status)) row = 5;
        else return 0;
        return row == 2 && toxic ? 8 : row;
    }

    private static int statusIconRow(Battler b) {
        int status;
        if ("SLEEP".equals(b.status)) status = 1;
        else if ("POISON".equals(b.status)) status = 2;
        else if ("BURN".equals(b.status)) status = 3;
        else if ("PARALYSIS".equals(b.status)) status = 4;
        else if ("FROZEN".equals(b.status)) status = 5;
        else return 0;
        return status == 2 && b.toxic > 0 ? 8 : status;            // :272
    }

    /** {@code @pokemon.isSpecies?(:X)} (PokeBattle_Pokemon:668). */
    private static boolean isSpecies(Pokemon pkmn, String internalName) {
        return pkmn != null && pkmn.species != null && pkmn.species.internalName != null
                && internalName.equalsIgnoreCase(pkmn.species.internalName);
    }

    /**
     * {@code @battler.owned?} (PokeBattle_Battler:205-208): only a wild battle
     * shows the "already registered" icon, and only once the player owns the
     * species. {@code displaySpecies} (:189-192) is the Illusion-aware species;
     * this runtime has no Illusion, so it is the real one.
     */
    private boolean registered(Battler b) {
        if (trainerBattle || b.pokemon == null || b.pokemon.species == null
                || b.pokemon.species.internalName == null) {
            return false;                                          // :206
        }
        return context.gameState().trainer().owned.contains(b.pokemon.species.internalName);   // :207
    }

    private static float expFraction(Pokemon p) {
        String growth = p.growthRate();
        int cur = PokemonStats.experienceForLevel(growth, p.level);
        int next = PokemonStats.experienceForLevel(growth, p.level + 1);
        if (next <= cur) return 0f;
        return Math.max(0f, Math.min(1f, (p.exp - cur) / (float) (next - cur)));
    }

    /**
     * pbShowWindow(MESSAGE_BOX): overlay_message + the message text. A message
     * may hold two lines (Battle_StartAndEnd:220 "{fullname}\n向你发起挑战！"),
     * drawn like the plugin's 96px window (Scene_Initialize:116-121).
     */
    private void drawMessageWindow(SpriteBatch batch, float w, float h) {
        Texture bar = assets.graphic("Pictures/Battle", "overlay_message");
        if (bar != null) batch.draw(bar, 0f, h - BAR_Y - bar.getHeight(), bar.getWidth(), bar.getHeight());
        if (message == null) return;
        String shown = message.text.substring(0, Math.min(revealedChars, message.text.length()));
        String[] lines = shown.split("\n", -1);
        float lineHeight = font.lineHeight();
        float top = BAR_Y + 2f + Math.max(0f, (96f - lines.length * lineHeight) / 2f);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isEmpty()) continue;
            font.draw(batch, lines[i], 16f, h - (top + i * lineHeight), TEXT_BASE, TEXT_SHADOW);
        }
        if (message.paused && revealedChars >= message.text.length()) {
            Texture pause = assets.graphic("Pictures", "pause");
            if (pause != null) batch.draw(pause, 628f, h - 432f - pause.getHeight());
        }
    }

    /** CommandMenuDisplay::MODES[0] (a regular battle). */
    private static final int[] COMMAND_MODES = {0, 2, 1, 3};
    /** CommandMenuDisplay::MODES[3] (a Safari Zone battle). */
    private static final int[] SAFARI_MODES = {5, 7, 6, 3};

    /** {@code Window_DrawableCommand#rowHeight} (SpriteWindow_text:759-761): 32 unless set. */
    private static final float CHOICE_ROW_HEIGHT = 32f;
    /** {@code SpriteWindow_Base::TEXTPADDING} (SpriteWindow:887). */
    private static final float TEXTPADDING = 4f;

    /**
     * {@code pbShowCommands}'s command window (PokeBattle_Scene:207-212), a
     * {@code Window_CommandPokemon} sized by {@code getAutoDims}
     * (SpriteWindow_text:1080-1099) and placed at {@code cw.x =
     * Graphics.width-cw.width}, {@code cw.y = Graphics.height-cw.height-dw.height}
     * - directly above the 96px message window. libGDX y grows upwards, so the
     * window's bottom edge sits at y = dw.height.
     */
    private void drawChoiceWindow(SpriteBatch batch, float w, float h) {
        WindowSkin.Geometry g = skin == null ? null : skin.geometry;
        float borderX = g == null ? 32f : g.borderX;                   // SpriteWindow:445-451
        float borderY = g == null ? 32f : g.borderY;                   // :453-459
        float startX = g == null ? 16f : g.trimStartX;                 // leftEdge = startX (:461)
        float startY = g == null ? 16f : g.trimStartY;                 // topEdge = startY (:462)
        // :1081-1083 windowheight = rowMax*rowHeight + borderY (one column).
        float height = choiceOptions.length * CHOICE_ROW_HEIGHT + borderY;
        // :1085-1092 the widest command in the system font, + 16 + 16 + TEXTPADDING.
        float textWidth = 0f;
        for (String option : choiceOptions) {
            textWidth = Math.max(textWidth, font.width(option));
        }
        float width = Math.max(borderX + 1f, textWidth + 16f + 16f + TEXTPADDING + borderX);   // :1096-1097
        height = Math.min(Math.max(borderY + 1f, height), h);              // :1098-1099
        float x = w - width;                                               // PokeBattle_Scene:208
        float y = MESSAGE_WINDOW_HEIGHT;                                   // :209 (bottom edge in libGDX y)
        MenuPanel.window(batch, assets, skin, x, y, width, height);
        Color[] colors = MenuPanel.textColors(skin);                       // :1055-1057 getDefaultTextColors
        float lineHeight = font.lineHeight();
        float contentsTop = y + height - startY;
        for (int i = 0; i < choiceOptions.length; i++) {
            float rowTop = contentsTop - i * CHOICE_ROW_HEIGHT;            // itemRect(i)
            if (i == choiceCursor.index()) {
                // :1110-1113 drawCursor copies selarrow(_white) at rect.x, rect.y.
                Texture arrow = assets.graphic("Pictures",
                        skin != null && skin.dark ? "selarrow_white" : "selarrow");
                if (arrow != null) {
                    batch.draw(arrow, x + startX, rowTop - arrow.getHeight());
                }
            }
            // :1114 / :1229-1231 the text starts 16px right of the cursor column,
            // centred in the row by pbDrawShadowText.
            float textTop = rowTop - (CHOICE_ROW_HEIGHT - lineHeight) / 2f;
            font.draw(batch, choiceOptions[i], x + startX + 16f, textTop, colors[0], colors[1]);
        }
    }

    /** pbShowWindow(COMMAND_BOX): overlay_command + prompt + the 4 buttons. */
    private void drawCommandWindow(SpriteBatch batch, float w, float h) {
        Texture overlay = assets.graphic("Pictures/Battle", "overlay_command");
        if (overlay != null) batch.draw(overlay, 0f, h - BAR_Y - overlay.getHeight(), overlay.getWidth(), overlay.getHeight());
        if (page != 0) {
            return;
        }
        Battler player = fighter();
        if (session.safari) {                                                      // 160:pbSafariCommandMenu {1}\n准备使用什么？
            String[] lines = { context.gameState().trainer().name, "准备使用什么？" };
            float top = BAR_Y + 2f + (96f - lines.length * font.lineHeight()) / 2f;
            for (int i = 0; i < lines.length; i++) {
                font.draw(batch, lines[i], 16f, h - (top + i * font.lineHeight()), TEXT_BASE, TEXT_SHADOW);
            }
        } else {
            String prompt = (player == null ? "" : player.name()) + "要做什么？";
            font.draw(batch, prompt, 16f, h - (BAR_Y + 2f + (96f - font.lineHeight()) / 2f),
                    TEXT_BASE, TEXT_SHADOW);
        }
        Texture cursorTex = assets.graphic("Pictures/Battle", "cursor_command");
        if (cursorTex == null) return;
        int buttonW = cursorTex.getWidth() / 2;
        for (int i = 0; i < 4 && i < rows.size(); i++) {
            float cx = w - 260f + ((i % 2 == 0) ? 0f : buttonW - 4f);
            float cy = BAR_Y + 6f + ((i / 2 == 0) ? 0f : 46f - 4f);
            int srcX = i == cursor.index() ? buttonW : 0;
            // CommandMenuDisplay::MODES: [0,2,1,3] with "Run", [0,2,1,9] with "Cancel" (a later battler's menu)
            int srcY = session.safari ? SAFARI_MODES[i] * 46                      // CommandMenuDisplay::MODES[3]
                    : (i == 3 && actioned.size() > 1 ? 9 : COMMAND_MODES[i]) * 46;
            batch.draw(cursorTex, cx, h - cy - 46f, buttonW, 46f, srcX, srcY, buttonW, 46, false, false);
        }
    }

    /**
     * pbShowWindow(TARGET_BOX): {@code TargetMenuDisplay} (PokeBattle_SceneMenus:445-552). The window has no background
     * of its own - the battle's message bar shows through - and its buttons are laid out per side size.
     */
    private void drawTargetMenu(SpriteBatch batch, float w, float h) {
        Texture cursorTex = assets.graphic("Pictures/Battle", "cursor_target");
        if (cursorTex == null || targetTexts == null) return;
        int buttonW = cursorTex.getWidth() / 2;                                 // @buttonBitmap.width/2
        int sideA = session.battle.pbSideSize(0);
        int sideB = session.battle.pbSideSize(1);
        int maxIndex = sideA > sideB ? (sideA - 1) * 2 : sideB * 2 - 1;        // :463
        boolean small = Math.max(sideA, sideB) > 2;                             // :464
        int srcW = small ? 170 : buttonW;                                       // :482 CMD_BUTTON_WIDTH_SMALL
        for (int pass = 0; pass < 2; pass++) {                                  // selected buttons are drawn above the others (:535)
            for (int i = 0; i <= maxIndex; i++) {
                int numButtons = i % 2 == 0 ? sideA : sideB;                    // :474
                if (numButtons <= i / 2) continue;                              // :475
                boolean named = i < targetTexts.length && targetTexts[i] != null;
                boolean sel = named && ((targetMode == 0 && i == targetIndex) || targetMode == 1);   // :527-529
                if ((pass == 1) != sel) continue;
                int inc = (i % 2) == 0 ? i / 2 : numButtons - 1 - i / 2;         // :479
                float bx = small ? 170 - new int[] { 0, 82, 166 }[numButtons - 1]
                                 : 138 - new int[] { 0, 116 }[numButtons - 1];  // :485/:487
                bx += (srcW - 4) * inc;                                         // :489
                float by = BAR_Y + 6f + (46 - 4) * ((i + 1) % 2);               // :490-491
                int buttonType = named ? ((i % 2) == 0 ? 1 : 2) : 0;            // :530
                buttonType = 2 * buttonType + (small ? 1 : 0);                  // :532
                batch.draw(cursorTex, bx, h - by - 46f, srcW, 46f, sel ? buttonW : 0, buttonType * 46,
                        srcW, 46, false, false);                                 // :533-534
            }
        }
        for (int i = 0; i <= maxIndex; i++) {                                   // :540-545 the target names
            int numButtons = i % 2 == 0 ? sideA : sideB;
            if (numButtons <= i / 2 || i >= targetTexts.length) continue;
            String name = targetTexts[i];
            if (name == null || name.isEmpty()) continue;                       // :541
            int inc = (i % 2) == 0 ? i / 2 : numButtons - 1 - i / 2;
            float bx = small ? 170 - new int[] { 0, 82, 166 }[numButtons - 1]
                             : 138 - new int[] { 0, 116 }[numButtons - 1];
            bx += (srcW - 4) * inc;
            float by = BAR_Y + 6f + (46 - 4) * ((i + 1) % 2);
            narrowFont.drawCentered(batch, name, bx + srcW / 2f, h - (by + 8f), TARGET_TEXT_BASE, TARGET_TEXT_SHADOW);   // :542-544
        }
    }

    /** {@code SafariDataBox#refresh}'s text colours (160_PokeBattle_SafariZone). */
    private static final Color SAFARI_BASE = new Color(248 / 255f, 248 / 255f, 248 / 255f, 1f);
    private static final Color SAFARI_SHADOW = new Color(104 / 255f, 104 / 255f, 104 / 255f, 1f);

    /** {@code TargetMenuDisplay::TEXT_BASE_COLOR / TEXT_SHADOW_COLOR} (PokeBattle_SceneMenus:457-458). */
    private static final Color TARGET_TEXT_BASE = new Color(240 / 255f, 248 / 255f, 224 / 255f, 1f);
    private static final Color TARGET_TEXT_SHADOW = new Color(64 / 255f, 64 / 255f, 64 / 255f, 1f);

    /** pbShowWindow(FIGHT_BOX): FightMenuDisplay. */
    private void drawFightMenu(SpriteBatch batch, float w, float h) {
        Texture overlay = assets.graphic("Pictures/Battle", "overlay_fight");
        if (overlay != null) batch.draw(overlay, 0f, h - BAR_Y - overlay.getHeight(), overlay.getWidth(), overlay.getHeight());
        Texture cursorTex = assets.graphic("Pictures/Battle", "cursor_fight");
        if (cursorTex == null) return;
        int buttonW = cursorTex.getWidth() / 2;
        Battler player = fighter();
        // ---- refreshSelection (:367-383): the four fixed slots, in order ----
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove move = player == null ? null : player.moveSlot(i);
            if (move == null) continue;            // :372-374 @visibility["button_i"] = false
            float cx = (i % 2 == 0) ? 8f : w - 8f - buttonW;            // :248
            float cy = BAR_Y + 6f + ((i / 2 == 0) ? 0f : 46f - 4f);     // :249-250
            int srcX = i == cursor.index() ? buttonW : 0;               // :377
            int srcY = Math.max(0, typeRow(move.type())) * 46;          // :378
            batch.draw(cursorTex, cx, h - cy - 46f, buttonW, 46f, srcX, srcY, buttonW, 46, false, false);
            // FightMenuDisplay#refreshButtonNames: centred at (button.x+buttonW/2, button.y+10),
            // TEXT_BASE_COLOR with a transparent shadow (PokeBattle_SceneMenus:355-361).
            narrowFont.drawCentered(batch, move.name(), cx + buttonW / 2f, h - (cy + 10f),
                    TEXT_BASE, NO_SHADOW);
        }
        if (player == null) return;
        // ---- refreshMoveData(moves[@index]) (:385-413) ----
        // A blank selected slot only hides the type icon (:398-401); the mega and
        // shift buttons below are refreshed either way (:415-427).
        BattleMove selected = player.moveSlot(cursor.index());
        if (selected != null) {
            // Type icon: src_rect.y = move.type*TYPE_ICON_HEIGHT (:404); the sprite
            // sits at (Graphics.width/2 - bitmap.width/2, barY+24) (:271-273).
            Texture types = assets.graphic("Pictures", "types");
            if (types != null) {
                int row = Math.max(0, typeRow(selected.type()));
                batch.draw(types, w / 2f - types.getWidth() / 2f, h - (BAR_Y + 24f) - 28f,
                        types.getWidth(), 28f, 0, row * 28, types.getWidth(), 28, false, false);
            }
            // PP text (:405-412): centred on (Graphics.width/2, 52) of the info
            // overlay, which starts at barY, coloured by the PP band of :216-221.
            int maxPp = player.moveSlotMaxPp(cursor.index());
            if (maxPp > 0) {                                                 // :407
                int pp = player.moveSlotPp(cursor.index());
                int ppFraction = Math.min((int) Math.ceil(4.0 * pp / maxPp), 3);   // :408
                narrowFont.drawCentered(batch, "PP:" + pp + "/" + maxPp, w / 2f, h - (BAR_Y + 52f),
                        PP_COLORS[ppFraction * 2], PP_COLORS[ppFraction * 2 + 1]); // :409-410
            }
        }
        // Mega Evolution button: centred horizontally (user-requested fix; the
        // plugin puts it at x=210, PokeBattle_SceneMenus:275-281), y = barY-h/2.
        if (megaButton) {
            Texture mega = assets.graphic("Pictures/Battle", "cursor_mega");
            if (mega != null) {
                int half = mega.getHeight() / 2;
                float megaX = (w - mega.getWidth()) / 2f;
                batch.draw(mega, megaX, h - (BAR_Y - half) - half, mega.getWidth(), half,
                        0, (session.megaRegistered(actingIndex()) ? half : 0), mega.getWidth(), half, false, false);
            }
        }
    }

    private int typeRow(String type) {
        PbsData.TypeInfo info = context.pbsData() == null ? null : context.pbsData().type(type);
        return info == null ? 0 : info.id;
    }

    private void drawImg(SpriteBatch batch, String dir, String name, float x, float topY, float h) {
        Texture t = assets.graphic(dir, name);
        if (t != null) batch.draw(t, x, h - topY - t.getHeight(), t.getWidth(), t.getHeight());
    }

    /**
     * PokemonDataBox#pbDrawNumber (PokeBattle_SceneElements:200-209): draws a
     * number with the {@code icon_numbers} strip (11 frames of 12x16; frame 10
     * is the "/" of the HP display). {@code alignRight} puts the last digit's
     * right edge on {@code x}.
     */
    private void drawBattleNumber(SpriteBatch batch, int value, float x, float topY, float h, boolean alignRight) {
        Texture numbers = assets.graphic("Pictures/Battle", "icon_numbers");
        if (numbers == null) return;
        int charWidth = numbers.getWidth() / 11;
        int charHeight = numbers.getHeight();
        String digits = value < 0 ? "/" : String.valueOf(value);
        float drawX = alignRight ? x - charWidth * digits.length() : x;
        for (int i = 0; i < digits.length(); i++) {
            int glyph = digits.charAt(i) == '/' ? 10 : digits.charAt(i) - '0';
            batch.draw(numbers, drawX, h - topY - charHeight, charWidth, charHeight,
                    glyph * charWidth, 0, charWidth, charHeight, false, false);
            drawX += charWidth;
        }
    }

    /** {@code PokemonBattlerSprite#pbPlayIntroAnimation} callbacks reach this. */
    String battlerSpriteId(Pokemon p, boolean back) {
        Integer held = heldForm.get(p);
        return battlerSpriteId(p, back, held != null ? held : (p.form == null ? 0 : p.form.form));
    }

    static String battlerSpriteId(Pokemon p, boolean back, int form) {
        String base = String.format("%03d", p.species.id) + (p.shiny ? "s" : "") + (back ? "b" : "");
        return form > 0 ? base + "_" + form : base;
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void dispose() {
        session.abortEngine();
        batch.dispose(); assets.dispose(); font.dispose(); narrowFont.dispose(); smallFont.dispose();
        if (spriteShader != null) spriteShader.dispose();
    }
}
