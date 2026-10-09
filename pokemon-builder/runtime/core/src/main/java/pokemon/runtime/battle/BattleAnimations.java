package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.Pokemon;

import java.util.ArrayList;
import java.util.List;

/**
 * The battle scene's {@code PokeBattle_Animation} subclasses, transcribed from
 * {@code PokeBattle_SceneAnimations} (934 lines) and, for the player's send-out,
 * this project's {@code Follower_Main} override.
 *
 * <p>Each class documents its exact source range; {@code createProcesses} is a
 * line-for-line port of the Ruby, using {@link PictureEx} for the scripted
 * sprite changes.</p>
 */
final class BattleAnimations {

    private BattleAnimations() {
    }

    /** {@code able?} (PokeBattle_Pokemon:735-737): {@code !egg? && @hp>0}. */
    private static boolean able(Pokemon pokemon) {
        return pokemon != null && !pokemon.egg && pokemon.hp > 0;
    }

    // ==================================================================
    // BattleIntroAnimation (PokeBattle_SceneAnimations:4-58)
    // ==================================================================

    /** {@code Scene_Initialize:22 pbBattleIntroAnimation}'s first animation. */
    static final class BattleIntroAnimation extends BattleAnimation {
        BattleIntroAnimation(Scene scene) {
            super(scene);
            start();
        }

        /** PokeBattle_SceneAnimations:10-48. */
        @Override
        protected void createProcesses() {
            int appearTime = 20;                                    // :11 This is in 1/20 seconds
            // Background
            if (sprites().get("battle_bg2") != null) {               // :13
                makeSlideSprite("battle_bg", 0.5f, appearTime, PictureEx.Origin.NONE);      // :14
                makeSlideSprite("battle_bg2", 0.5f, appearTime, PictureEx.Origin.NONE);     // :15
            }
            // Bases
            makeSlideSprite("base_0", -1f, appearTime, PictureEx.Origin.BOTTOM);            // :18
            makeSlideSprite("base_1", 1f, appearTime, PictureEx.Origin.CENTER);             // :19
            // Player sprite, partner trainer sprite
            for (int i = 0; i < scene().playerCount(); i++) {                               // :21
                makeSlideSprite("player_" + (i + 1), -1f, appearTime, PictureEx.Origin.BOTTOM);   // :22
            }
            // Opposing trainer sprite(s) or wild Pokémon sprite(s)
            if (scene().trainerBattle()) {                                                  // :25
                for (int i = 0; i < scene().opponentCount(); i++) {                         // :26
                    makeSlideSprite("trainer_" + (i + 1), 1f, appearTime,
                            PictureEx.Origin.BOTTOM);                                       // :27
                }
            } else {   // Wild battle
                Array<Battler> wild = scene().party(1);                                     // :30
                for (int i = 0; i < wild.size; i++) {
                    int idxBattler = 2 * i + 1;                                             // :31
                    makeSlideSprite("pokemon_" + idxBattler, 1f, appearTime,
                            PictureEx.Origin.BOTTOM);                                       // :32
                }
            }
            // Shadows
            for (int i = 0; i < scene().battlerCount(); i++) {                              // :36
                makeSlideSprite("shadow_" + i, (i % 2) == 0 ? -1f : 1f, appearTime,
                        PictureEx.Origin.CENTER);                                           // :37
            }
            // Fading blackness over whole screen
            PictureEx blackScreen = addNewSprite(0, 0,
                    "Graphics/Battle animations/black_screen", PictureEx.Origin.TOP_LEFT);  // :40
            blackScreen.setZ(0, 999);                                                       // :41
            blackScreen.moveOpacity(0, 8, 0);                                               // :42
            // Fading blackness over command bar
            BattleSprite cmdBar = sprites().get("cmdBar_bg");                               // :44
            PictureEx blackBar = addNewSprite(cmdBar == null ? 0 : cmdBar.x,
                    cmdBar == null ? 0 : cmdBar.y,
                    "Graphics/Battle animations/black_bar", PictureEx.Origin.TOP_LEFT);     // :44-45
            blackBar.setZ(0, 998);                                                          // :46
            blackBar.moveOpacity(appearTime * 3 / 4, appearTime / 4, 0);                    // :47
        }

        /** {@code makeSlideSprite} (PokeBattle_SceneAnimations:50-57). */
        private void makeSlideSprite(String spriteName, float deltaMult, int appearTime,
                int origin) {
            BattleSprite sprite = sprites().get(spriteName);        // :53
            if (sprite == null) {
                return;
            }
            PictureEx s = addSprite(sprite, origin);                // :54
            s.setDelta(0, (float) Math.floor(PictureEx.Graphics.WIDTH * deltaMult), 0);       // :55
            s.moveDelta(0, appearTime,
                    (float) Math.floor(-PictureEx.Graphics.WIDTH * deltaMult), 0);            // :56
        }
    }

    // ==================================================================
    // BattleIntroAnimation2 (PokeBattle_SceneAnimations:66-81)
    // ==================================================================

    /** Wild battle: the Pokémon fades back to normal colour and cries. */
    static final class BattleIntroAnimation2 extends BattleAnimation {
        private final int sideSize;

        BattleIntroAnimation2(Scene scene, int sideSize) {
            super(scene);
            this.sideSize = sideSize;
            start();
        }

        /** PokeBattle_SceneAnimations:72-80. */
        @Override
        protected void createProcesses() {
            for (int i = 0; i < sideSize; i++) {                    // :73
                int idxBattler = 2 * i + 1;                         // :74
                BattleSprite sprite = sprites().get("pokemon_" + idxBattler);
                if (sprite == null) {                               // :75
                    continue;
                }
                PictureEx battler = addSprite(sprite, PictureEx.Origin.BOTTOM);   // :76
                battler.moveTone(0, 4, new float[] { 0, 0, 0, 0 });               // :77
                // setCallback(10*i,[@sprites["pokemon_#{idxBattler}"],:pbPlayIntroAnimation])
                battler.setCallback(10 * i, p -> playIntroAnimation(sprite));     // :78
            }
        }
    }

    // ==================================================================
    // LineupAppearAnimation (PokeBattle_SceneAnimations:88-182)
    // ==================================================================

    /** Makes a side's party bar and balls appear. */
    static final class LineupAppearAnimation extends BattleAnimation {
        /** {@code BAR_DISPLAY_WIDTH} (:89). */
        private static final int BAR_DISPLAY_WIDTH = 248;

        private final int side;
        private final Array<Pokemon> party;
        private final int[] partyStarts;
        private final boolean fullAnim;
        /** The bar's bitmap width, read by resetGraphics:113 ({@code bar.bitmap.width}). */
        private final int barBitmapWidth;

        LineupAppearAnimation(Scene scene, int side, Array<Pokemon> party, int[] partyStarts,
                boolean fullAnim) {
            super(scene);
            this.side = side;
            this.party = party;
            this.partyStarts = partyStarts;
            this.fullAnim = fullAnim;
            this.barBitmapWidth = bitmapWidthOf("Graphics/Pictures/Battle/overlay_lineup");
            resetGraphics();                                        // :96
            start();                                               // :97 super
        }

        private int bitmapWidthOf(String name) {
            int[] size = scene().bitmapSize().size(name);
            return size == null ? 0 : size[0];
        }

        /** {@code resetGraphics} (PokeBattle_SceneAnimations:100-128). */
        private void resetGraphics() {
            BattleSprite bar = sprites().get("partyBar_" + side);    // :101
            float barX = 0;
            float barY = 0;
            float ballX = 0;
            float ballY = 0;
            switch (side) {
                case 1:   // Opposing lineup
                    barX = PictureEx.Graphics.WIDTH - BAR_DISPLAY_WIDTH;      // :104
                    barY = 50;                                               // :105
                    ballX = PictureEx.Graphics.WIDTH / 2f + 35 + 64;          // :106
                    ballY = barY - 30;                                       // :107
                    break;
                case 0:   // Player's lineup
                    barX = BAR_DISPLAY_WIDTH;                                // :109
                    barY = 50;                                               // :110
                    ballX = PictureEx.Graphics.WIDTH / 2f - 65 - 64;          // :111 30 is width of ball icon
                    ballY = barY - 30;                                       // :112
                    barX -= barBitmapWidth;                                  // :113
                    break;
                default:
                    break;
            }
            float ballXdiff = 32 * (1 - 2 * side);                       // :115
            if (bar != null) {
                bar.x = barX;                                            // :116
                bar.y = barY;                                            // :117
                bar.opacity = 255;                                       // :118
                bar.visible = false;                                     // :119
            }
            for (int i = 0; i < PokeBattle_SceneConstants.NUM_BALLS; i++) {   // :120
                BattleSprite ball = sprites().get("partyBall_" + side + "_" + i);   // :121
                if (ball == null) {
                    continue;
                }
                ball.x = ballX;                                          // :122
                ball.y = ballY;                                          // :123
                ball.opacity = 255;                                      // :124
                ball.visible = false;                                    // :125
                ballX -= ballXdiff;                                      // :126
            }
        }

        /** {@code getPartyIndexFromBallIndex} (PokeBattle_SceneAnimations:130-148). */
        private int getPartyIndexFromBallIndex(int idxBall) {
            // Player's lineup (just show balls for player's party)
            if (side == 0) {                                             // :132
                if (partyStarts.length < 2) {
                    return idxBall;                                      // :133
                }
                if (idxBall < partyStarts[1]) {
                    return idxBall;                                      // :134
                }
                return -1;                                               // :135
            }
            // Opposing lineup
            int ballsPerTrainer = PokeBattle_SceneConstants.NUM_BALLS / partyStarts.length;  // :139 6/3/2
            int startsIndex = idxBall / ballsPerTrainer;                 // :140
            int teamIndex = idxBall % ballsPerTrainer;                   // :141
            int ret = partyStarts[startsIndex] + teamIndex;              // :142
            if (startsIndex < partyStarts.length - 1) {                  // :143
                // There is a later trainer, don't spill over into its team
                if (ret >= partyStarts[startsIndex + 1]) {
                    return -1;                                           // :145
                }
            }
            return ret;                                                 // :147
        }

        /** PokeBattle_SceneAnimations:150-160. */
        @Override
        protected void createProcesses() {
            BattleSprite barSprite = sprites().get("partyBar_" + side);   // :151
            if (barSprite == null) {
                return;
            }
            PictureEx bar = addSprite(barSprite);                        // :151
            bar.setVisible(0, true);                                     // :152
            float dir = (side == 0) ? 1f : -1f;                           // :153
            bar.setDelta(0, -dir * PictureEx.Graphics.WIDTH / 2f, 0);     // :154
            bar.moveDelta(0, 8, dir * PictureEx.Graphics.WIDTH / 2f, 0);  // :155
            int delay = bar.totalDuration();                             // :156
            for (int i = 0; i < PokeBattle_SceneConstants.NUM_BALLS; i++) {   // :157
                createBall(i, fullAnim ? delay + i * 2 : 0, dir);        // :158
            }
        }

        /** {@code createBall} (PokeBattle_SceneAnimations:162-181). */
        private void createBall(int idxBall, int delay, float dir) {
            // Choose ball's graphic
            int idxParty = getPartyIndexFromBallIndex(idxBall);          // :164
            String graphicFilename = "Graphics/Pictures/Battle/icon_ball_empty";   // :165
            if (idxParty >= 0 && idxParty < party.size && party.get(idxParty) != null) {   // :166
                Pokemon member = party.get(idxParty);
                if (!able(member)) {                                     // :167
                    graphicFilename = "Graphics/Pictures/Battle/icon_ball_faint";      // :168
                } else if (member.status != null && !member.status.isEmpty()) {   // :169 PBStatuses::NONE
                    graphicFilename = "Graphics/Pictures/Battle/icon_ball_status";     // :170
                } else {
                    graphicFilename = "Graphics/Pictures/Battle/icon_ball";            // :172
                }
            }
            // Set up ball sprite
            BattleSprite ballSprite = sprites().get("partyBall_" + side + "_" + idxBall);   // :176
            if (ballSprite == null) {
                return;
            }
            PictureEx ball = addSprite(ballSprite);
            ball.setVisible(delay, true);                                // :177
            ball.setName(delay, graphicFilename);                        // :178
            ball.setDelta(delay, -dir * PictureEx.Graphics.WIDTH / 2f, 0);   // :179
            ball.moveDelta(delay, 8, dir * PictureEx.Graphics.WIDTH / 2f, 0);   // :180
        }
    }

    // ==================================================================
    // DataBoxAppearAnimation / DataBoxDisappearAnimation
    // (PokeBattle_SceneAnimations:189-203 / :210-223)
    // ==================================================================

    /** Makes a Pokémon's data box appear. */
    static final class DataBoxAppearAnimation extends BattleAnimation {
        private final int idxBox;

        DataBoxAppearAnimation(Scene scene, int idxBox) {
            super(scene);
            this.idxBox = idxBox;
            start();
        }

        /** PokeBattle_SceneAnimations:195-202. */
        @Override
        protected void createProcesses() {
            BattleSprite sprite = sprites().get("dataBox_" + idxBox);     // :196
            if (sprite == null) {
                return;
            }
            // PokemonDataBox#updatePositions (PokeBattle_SceneElements:376) puts the
            // box back on @spriteX every frame, so a box that DataBoxDisappearAnimation
            // slid away starts from its resting place again.
            if (!Float.isNaN(sprite.homeX)) {
                sprite.x = sprite.homeX;
            }
            PictureEx box = addSprite(sprite);                           // :197
            box.setVisible(0, true);                                     // :198
            float dir = ((idxBox % 2) == 0) ? -1f : 1f;                   // :199
            box.setDelta(0, dir * PictureEx.Graphics.WIDTH / 2f, 0);      // :200
            box.moveDelta(0, 8, -dir * PictureEx.Graphics.WIDTH / 2f, 0); // :201
        }
    }

    /** Makes a Pokémon's data box disappear. */
    static final class DataBoxDisappearAnimation extends BattleAnimation {
        private final int idxBox;

        DataBoxDisappearAnimation(Scene scene, int idxBox) {
            super(scene);
            this.idxBox = idxBox;
            start();
        }

        /** PokeBattle_SceneAnimations:216-222. */
        @Override
        protected void createProcesses() {
            BattleSprite sprite = sprites().get("dataBox_" + idxBox);     // :217
            if (sprite == null || !sprite.visible) {
                return;
            }
            PictureEx box = addSprite(sprite);                           // :218
            float dir = ((idxBox % 2) == 0) ? -1f : 1f;                   // :219
            box.moveDelta(0, 8, dir * PictureEx.Graphics.WIDTH / 2f, 0);  // :220
            box.setVisible(8, false);                                    // :221
        }
    }

    // ==================================================================
    // PlayerFadeAnimation (PokeBattle_SceneAnimations:306-351)
    // ==================================================================

    /** The player and the player's party lineup sliding off screen. */
    static final class PlayerFadeAnimation extends BattleAnimation {
        /** {@code @fullAnim} (:308): true at the start of battle, false on a switch. */
        private final boolean fullAnim;

        PlayerFadeAnimation(Scene scene, boolean fullAnim) {
            super(scene);
            this.fullAnim = fullAnim;
            start();
        }

        /** PokeBattle_SceneAnimations:312-350. */
        @Override
        protected void createProcesses() {
            // NOTE: The movement speeds of trainers/bar/balls are all different.
            // Move trainer sprite(s) off-screen
            String spriteNameBase = "player";                            // :315
            int i = 1;                                                  // :316
            while (sprites().get(spriteNameBase + "_" + i) != null) {     // :317
                BattleSprite pl = sprites().get(spriteNameBase + "_" + i);   // :318
                i += 1;                                                 // :319
                if (!pl.visible || pl.x < 0) {                           // :320
                    continue;
                }
                PictureEx trainer = addSprite(pl, PictureEx.Origin.BOTTOM);   // :321
                trainer.moveDelta(0, 16, -PictureEx.Graphics.WIDTH / 2f, 0);  // :322
                // Animate trainer sprite(s) if they have multiple frames
                if (pl.bitmapWidth() > 0 && pl.bitmapWidth() >= pl.bitmapHeight() * 2) {   // :324
                    int size = pl.sourceWidth();                        // :325 Width per frame
                    trainer.setSrc(0, size, 0);                         // :326
                    trainer.setSrc(5, size * 2, 0);                     // :327
                    trainer.setSrc(7, size * 3, 0);                     // :328
                    trainer.setSrc(9, size * 4, 0);                     // :329
                }
                trainer.setVisible(16, false);                          // :331
            }
            // Move and fade party bar/balls
            int delay = 3;                                              // :334
            BattleSprite barSprite = sprites().get("partyBar_0");        // :335
            if (barSprite != null && barSprite.visible) {
                PictureEx partyBar = addSprite(barSprite);              // :336
                if (fullAnim) {
                    partyBar.moveDelta(delay, 16, -PictureEx.Graphics.WIDTH / 4f, 0);   // :337
                }
                partyBar.moveOpacity(delay, 12, 0);                     // :338
                partyBar.setVisible(delay + 12, false);                 // :339
                partyBar.setOpacity(delay + 12, 255);                   // :340
            }
            for (int b = 0; b < PokeBattle_SceneConstants.NUM_BALLS; b++) {   // :342
                BattleSprite ballSprite = sprites().get("partyBall_0_" + b);  // :343
                if (ballSprite == null || !ballSprite.visible) {
                    continue;
                }
                PictureEx partyBall = addSprite(ballSprite);            // :344
                if (fullAnim) {
                    partyBall.moveDelta(delay + 2 * (PokeBattle_SceneConstants.NUM_BALLS - b), 16,
                            -PictureEx.Graphics.WIDTH, 0);              // :345
                }
                partyBall.moveOpacity(delay, 12, 0);                    // :346
                partyBall.setVisible(delay + 12, false);                // :347
                partyBall.setOpacity(delay + 12, 255);                  // :348
            }
        }
    }

    // ==================================================================
    // TrainerFadeAnimation (PokeBattle_SceneAnimations:359-396)
    // ==================================================================

    /** The enemy trainer(s) and the enemy party lineup sliding off screen. */
    static final class TrainerFadeAnimation extends BattleAnimation {
        private final boolean fullAnim;

        TrainerFadeAnimation(Scene scene, boolean fullAnim) {
            super(scene);
            this.fullAnim = fullAnim;
            start();
        }

        /** PokeBattle_SceneAnimations:365-395. */
        @Override
        protected void createProcesses() {
            String spriteNameBase = "trainer";                           // :368
            int i = 1;                                                  // :369
            while (sprites().get(spriteNameBase + "_" + i) != null) {     // :370
                BattleSprite trSprite = sprites().get(spriteNameBase + "_" + i);   // :371
                i += 1;                                                 // :372
                if (!trSprite.visible || trSprite.x > PictureEx.Graphics.WIDTH) {   // :373
                    continue;
                }
                PictureEx trainer = addSprite(trSprite, PictureEx.Origin.BOTTOM);   // :374
                trainer.moveDelta(0, 16, PictureEx.Graphics.WIDTH / 2f, 0);         // :375
                trainer.setVisible(16, false);                          // :376
            }
            int delay = 3;                                              // :379
            BattleSprite barSprite = sprites().get("partyBar_1");        // :380
            if (barSprite != null && barSprite.visible) {
                PictureEx partyBar = addSprite(barSprite);              // :381
                if (fullAnim) {
                    partyBar.moveDelta(delay, 16, PictureEx.Graphics.WIDTH / 4f, 0);   // :382
                }
                partyBar.moveOpacity(delay, 12, 0);                     // :383
                partyBar.setVisible(delay + 12, false);                 // :384
                partyBar.setOpacity(delay + 12, 255);                   // :385
            }
            for (int b = 0; b < PokeBattle_SceneConstants.NUM_BALLS; b++) {   // :387
                BattleSprite ballSprite = sprites().get("partyBall_1_" + b);  // :388
                if (ballSprite == null || !ballSprite.visible) {
                    continue;
                }
                PictureEx partyBall = addSprite(ballSprite);            // :389
                if (fullAnim) {
                    partyBall.moveDelta(delay + 2 * (PokeBattle_SceneConstants.NUM_BALLS - b), 16,
                            PictureEx.Graphics.WIDTH, 0);               // :390
                }
                partyBall.moveOpacity(delay, 12, 0);                    // :391
                partyBall.setVisible(delay + 12, false);                // :392
                partyBall.setOpacity(delay + 12, 255);                  // :393
            }
        }
    }

    // ==================================================================
    // TrainerAppearAnimation (PokeBattle_SceneAnimations:272-297)
    // ==================================================================

    /** An enemy trainer sliding on-screen from the right (used at the end of battle). */
    static final class TrainerAppearAnimation extends BattleAnimation {
        private final int idxTrainer;

        TrainerAppearAnimation(Scene scene, int idxTrainer) {
            super(scene);
            this.idxTrainer = idxTrainer;
            start();
        }

        /** PokeBattle_SceneAnimations:278-296. */
        @Override
        protected void createProcesses() {
            int delay = 0;                                              // :279
            // :281-286 the previous trainer slides off to the right first.
            BattleSprite old = idxTrainer > 0 ? sprites().get("trainer_" + idxTrainer) : null;
            if (old != null && old.visible) {
                PictureEx oldTrainer = addSprite(old, PictureEx.Origin.BOTTOM);
                oldTrainer.moveDelta(delay, 8, PictureEx.Graphics.WIDTH / 4f, 0);   // :283
                oldTrainer.setVisible(delay + 8, false);                            // :284
                delay = oldTrainer.totalDuration();                                 // :285
            }
            // :288-295 the new trainer slides on-screen.
            BattleSprite next = sprites().get("trainer_" + (idxTrainer + 1));
            if (next != null) {
                float[] pos = PokeBattle_SceneConstants.trainerPosition(1, 0, 1);   // :289
                float trainerX = pos[0] + 64 + PictureEx.Graphics.WIDTH / 4f;       // :290
                PictureEx newTrainer = addSprite(next, PictureEx.Origin.BOTTOM);    // :291
                newTrainer.setVisible(delay, true);                                 // :292
                newTrainer.setXY(delay, trainerX, pos[1]);                          // :293
                newTrainer.moveDelta(delay, 8, -PictureEx.Graphics.WIDTH / 4f, 0);  // :294
            }
        }
    }

    // ==================================================================
    // pbSendOutBattlers (Scene_Animations:85-143)
    // ==================================================================

    /**
     * The animation half of {@code pbSendOut}: {@code pbSendOutBattlers}
     * (Scene_Animations:85-143). It owns the fade animation and one
     * send-out + data box pair per battler, and reproduces the Ruby update loop
     * (:119-131).
     */
    static final class SendOutSequence {
        /** {@code sendOutAnims.push([sendOutAnim,dataBoxAnim,false])} (:116). */
        private static final class Entry {
            BattleAnimation sendOut;
            BattleAnimation dataBox;
            boolean finished;
        }

        private final BattleAnimation fadeAnim;
        private final List<Entry> sendOutAnims = new ArrayList<>();

        SendOutSequence(BattleAnimation.Scene scene, List<int[]> sendOuts, boolean startBattle) {
            // :93-97 Make all trainers and party lineups disappear
            fadeAnim = scene.opposes(sendOuts.get(0)[0])
                    ? new TrainerFadeAnimation(scene, startBattle)
                    : new PlayerFadeAnimation(scene, startBattle);
            // :101-117 One send-out animation and one data box per battler
            for (int i = 0; i < sendOuts.size(); i++) {
                int idxBattler = sendOuts.get(i)[0];
                Battler battler = scene.battler(idxBattler);
                if (battler == null) {
                    continue;
                }
                // :103 pkmn = @battle.battlers[b[0]].effects[PBEffects::Illusion] || b[1]: the sprite takes
                // Battler#visiblePokemon (the Illusion when it is up, else the Pokemon itself).
                scene.changePokemon(idxBattler, battler);            // :104-105
                Entry entry = new Entry();
                int idxTrainer = scene.ownerIndex(idxBattler) + 1;   // :108/:112
                entry.sendOut = scene.opposes(idxBattler)
                        ? new PokeballTrainerSendOutAnimation(scene, battler, startBattle, i)   // :107-109
                        : new PokeballPlayerSendOutAnimation(scene, idxTrainer, battler, startBattle, i);   // :111-113
                entry.dataBox = new DataBoxAppearAnimation(scene, idxBattler);   // :115
                sendOutAnims.add(entry);
            }
        }

        /** One {@code pbUpdate} of the loop at Scene_Animations:119-131. */
        void update() {
            fadeAnim.update();                                       // :120
            for (Entry entry : sendOutAnims) {                       // :121
                if (entry.finished) {                                // :122
                    continue;
                }
                entry.sendOut.update();                              // :123
                if (entry.sendOut.animDone()) {                      // :124
                    entry.dataBox.update();
                }
                if (entry.dataBox.animDone()) {                      // :125
                    entry.finished = true;
                }
            }
        }

        /**
         * {@code break if !sendOutAnims.any? { |a| !a[2] } } (:128-130). The
         * outer {@code if !inPartyAnimation?} guard is the scene's
         * {@code @animations} list, which the screen checks separately.
         */
        boolean done() {
            for (Entry entry : sendOutAnims) {
                if (!entry.finished) {
                    return false;
                }
            }
            return true;
        }

        /** {@code fadeAnim.dispose; sendOutAnims.each { |a| a[0].dispose; a[1].dispose } } (:132-133). */
        void dispose() {
            fadeAnim.dispose();
            for (Entry entry : sendOutAnims) {
                entry.sendOut.dispose();
                entry.dataBox.dispose();
            }
        }
    }

    // ==================================================================
    // AbilitySplashAppearAnimation / AbilitySplashDisappearAnimation (PokeBattle_SceneAnimations:230-267)
    // ==================================================================

    /** Makes a Pokemon's ability bar slide in (:230-247). */
    static final class AbilitySplashAppearAnimation extends BattleAnimation {
        private final int side;

        AbilitySplashAppearAnimation(Scene scene, int side) {
            super(scene);
            this.side = side;                                           // :232
            start();
        }

        @Override
        protected void createProcesses() {
            BattleSprite barSprite = sprites().get("abilityBar_" + side);   // :237
            if (barSprite == null) {
                return;
            }
            PictureEx bar = addSprite(barSprite);                       // :238
            bar.setVisible(0, true);                                    // :239
            int dir = side == 0 ? 1 : -1;                               // :240
            bar.moveDelta(0, 8, dir * PictureEx.Graphics.WIDTH / 2f, 0);   // :241
        }
    }

    /** Makes a Pokemon's ability bar slide out (:250-267). */
    static final class AbilitySplashDisappearAnimation extends BattleAnimation {
        private final int side;

        AbilitySplashDisappearAnimation(Scene scene, int side) {
            super(scene);
            this.side = side;                                           // :252
            start();
        }

        @Override
        protected void createProcesses() {
            BattleSprite barSprite = sprites().get("abilityBar_" + side);   // :257
            if (barSprite == null) {
                return;
            }
            PictureEx bar = addSprite(barSprite);                       // :258
            int dir = side == 0 ? -1 : 1;                               // :259
            bar.moveDelta(0, 8, dir * PictureEx.Graphics.WIDTH / 2f, 0);   // :260
            bar.setVisible(8, false);                                   // :261
        }
    }

    // ==================================================================
    // PokeballPlayerSendOutAnimation (Follower_Main:727-813 — the project
    // overrides PokeBattle_SceneAnimations:404-477 with this class)
    // ==================================================================

    /** A Pokémon being sent out on the player's side, including the throw. */
    static final class PokeballPlayerSendOutAnimation extends BattleAnimation {
        private final int idxTrainer;
        private final Battler battler;
        private final boolean showingTrainer;
        private final int idxOrder;
        /** Follower_Main:736-737. */
        private final boolean followAnim;
        private final boolean shadowVisible;

        PokeballPlayerSendOutAnimation(Scene scene, int idxTrainer, Battler battler,
                boolean startBattle, int idxOrder) {
            super(scene);
            this.idxTrainer = idxTrainer;                           // :731
            this.battler = battler;                                 // :732
            this.showingTrainer = startBattle;                      // :733
            this.idxOrder = idxOrder;                               // :734
            // :737 refresh_sprite(false,true) && battler.index == 0 && startBattle.
            this.followAnim = scene.followerSprite() && battler.index == 0 && startBattle;
            BattleSprite pokemon = sprites().get("pokemon_" + battler.index);
            if (pokemon != null) {
                pokemon.visible = false;                            // :738
            }
            BattleSprite shadow = sprites().get("shadow_" + battler.index);
            this.shadowVisible = shadow != null && shadow.visible;   // :739
            if (shadow != null) {
                shadow.visible = false;                             // :740
            }
            start();
        }

        /** Follower_Main:744-812. */
        @Override
        protected void createProcesses() {
            BattleSprite batSprite = sprites().get("pokemon_" + battler.index);   // :745
            BattleSprite shaSprite = sprites().get("shadow_" + battler.index);    // :746
            BattleSprite traSprite = sprites().get("player_" + idxTrainer);       // :747
            if (batSprite == null) {
                return;
            }
            // Calculate the Poké Ball graphic to use
            int ballType = 0;                                       // :749
            if (battler.pokemon != null) {                          // :750
                ballType = Math.max(0, battler.pokemon.ballused);   // :751
            }
            // Calculate the color to turn the battler sprite
            float[] col = getBattlerColorFromBallType(ballType);    // :754
            col[3] = 255;                                           // :755
            // Calculate start and end coordinates for battler sprite movement
            float[] ballPos = PokeBattle_SceneConstants.battlerPosition(battler.index,
                    scene().sideSize(battler.index));               // :757
            float battlerStartX = ballPos[0];                       // :758 Is also where the Ball needs to end
            float battlerStartY = ballPos[1];                       // :759 Is also where the Ball needs to end + 18
            float battlerEndX = batSprite.x;                        // :760
            float battlerEndY = batSprite.y;                        // :761
            // Calculate start and end coordinates for Poké Ball sprite movement
            float ballStartX = -6;                                  // :763
            float ballStartY = 202;                                 // :764
            float ballMidX = 0;                                     // :765 Unused in trajectory calculation
            float ballMidY = battlerStartY - 144;                   // :766
            // Set up Poké Ball sprite
            PictureEx ball = addBallSprite(ballStartX, ballStartY, ballType);   // :768
            ball.setZ(0, 25);                                       // :769
            ball.setVisible(0, false);                              // :770
            // Poké Ball tracking the player's hand animation (if trainer is visible)
            if (showingTrainer && !followAnim && traSprite != null && traSprite.x > 0) {   // :772
                ball.setZ(0, traSprite.z - 1);                      // :773
                float[] start = ballTracksHand(ball, traSprite);     // :774
                ballStartX = start[0];
                ballStartY = start[1];
            }
            int delay = ball.totalDuration();                       // :776 0 or 7
            // Poké Ball trajectory animation
            if (!followAnim) {                                      // :779 (if modifier)
                createBallTrajectory(ball, delay, 12, ballStartX, ballStartY, ballMidX, ballMidY,
                        battlerStartX, battlerStartY - 18);         // :778-779
            }
            ball.setZ(9, batSprite.z - 1);                          // :780
            delay = ball.totalDuration() + 4;                       // :781
            delay += 10 * idxOrder;                                 // :782 Stagger appearances
            if (!followAnim) {                                      // :783
                ballOpenUp(ball, delay - 2, ballType);              // :784
                ballBurst(delay, battlerStartX, battlerStartY - 18, ballType);   // :785
                ball.moveOpacity(delay + 2, 2, 0);                  // :786
            }
            // Set up battler sprite
            PictureEx battlerPicture = addSprite(batSprite, PictureEx.Origin.BOTTOM);   // :789
            if (!followAnim) {                                      // :790
                battlerPicture.setXY(0, battlerStartX, battlerStartY);   // :791
                battlerPicture.setZoom(0, 0);                       // :792
                battlerPicture.setColor(0, col);                    // :793
                // Battler animation
                battlerAppear(battlerPicture, delay, battlerEndX, battlerEndY, batSprite, col,
                        p -> playIntroAnimation(batSprite));        // :795
            } else {
                // Follower_Main:797-802 - needs the follower subsystem.
                battlerPicture.setVisible(delay - ball.totalDuration(), true);   // :797
                battlerPicture.setOpacity(delay - ball.totalDuration(), 255);    // :798
                battlerPicture.setXY(0, -192, battlerEndY);         // :799
                battlerPicture.moveXY(delay - ball.totalDuration() + 1, 16, battlerStartX,
                        battlerEndY);                               // :800
                battlerPicture.setSE(delay - ball.totalDuration() + 18,
                        "GUI naming tab swap start", 100, null);    // :801
                battlerPicture.setCallback(delay - ball.totalDuration() + 18,
                        p -> playIntroAnimation(batSprite));        // :802
            }
            if (shadowVisible && shaSprite != null) {               // :804
                // Set up shadow sprite
                PictureEx shadow = addSprite(shaSprite, PictureEx.Origin.CENTER);   // :806
                shadow.setOpacity(0, 0);                            // :807
                // Shadow animation
                shadow.setVisible(delay, shadowVisible);            // :809
                shadow.moveOpacity(delay + 5, 10, 255);             // :810
            }
        }
    }

    // ==================================================================
    // PokeballTrainerSendOutAnimation (PokeBattle_SceneAnimations:486-551)
    // ==================================================================

    /** A Pokémon being sent out on the opposing side. */
    static final class PokeballTrainerSendOutAnimation extends BattleAnimation {
        private final Battler battler;
        private final boolean showingTrainer;
        private final int idxOrder;
        private final boolean shadowVisible;

        PokeballTrainerSendOutAnimation(Scene scene, Battler battler, boolean startBattle,
                int idxOrder) {
            super(scene);
            this.battler = battler;                                 // :491
            this.showingTrainer = startBattle;                      // :492
            this.idxOrder = idxOrder;                               // :493
            BattleSprite pokemon = sprites().get("pokemon_" + battler.index);
            if (pokemon != null) {
                pokemon.visible = false;                            // :494
            }
            BattleSprite shadow = sprites().get("shadow_" + battler.index);
            this.shadowVisible = shadow != null && shadow.visible;   // :495
            if (shadow != null) {
                shadow.visible = false;                             // :496
            }
            start();
        }

        /** PokeBattle_SceneAnimations:500-543. */
        @Override
        protected void createProcesses() {
            BattleSprite batSprite = sprites().get("pokemon_" + battler.index);   // :501
            BattleSprite shaSprite = sprites().get("shadow_" + battler.index);    // :502
            if (batSprite == null) {
                return;
            }
            // Calculate the Poké Ball graphic to use
            int ballType = 0;                                       // :504
            if (battler.pokemon != null) {                          // :505
                ballType = Math.max(0, battler.pokemon.ballused);   // :506
            }
            // Calculate the color to turn the battler sprite
            float[] col = getBattlerColorFromBallType(ballType);    // :509
            col[3] = 255;                                           // :510
            // Calculate start and end coordinates for battler sprite movement
            float[] ballPos = PokeBattle_SceneConstants.battlerPosition(battler.index,
                    scene().sideSize(battler.index));               // :512
            float battlerStartX = ballPos[0];                       // :513
            float battlerStartY = ballPos[1];                       // :514
            float battlerEndX = batSprite.x;                        // :515
            float battlerEndY = batSprite.y;                        // :516
            // Set up Poké Ball sprite
            PictureEx ball = addBallSprite(0, 0, ballType);         // :518
            ball.setZ(0, batSprite.z - 1);                          // :519
            // Poké Ball animation
            createBallTrajectory(ball, battlerStartX, battlerStartY);   // :521
            int delay = ball.totalDuration() + 6;                   // :522
            if (showingTrainer) {
                delay += 10;                                        // :523
            }
            delay += 10 * idxOrder;                                 // :524
            ballOpenUp(ball, delay - 2, ballType);                  // :525
            ballBurst(delay, battlerStartX, battlerStartY - 18, ballType);   // :526
            ball.moveOpacity(delay + 2, 2, 0);                      // :527
            // Set up battler sprite
            PictureEx battlerPicture = addSprite(batSprite, PictureEx.Origin.BOTTOM);   // :529
            battlerPicture.setXY(0, battlerStartX, battlerStartY);   // :530
            battlerPicture.setZoom(0, 0);                           // :531
            battlerPicture.setColor(0, col);                        // :532
            // Battler animation
            battlerAppear(battlerPicture, delay, battlerEndX, battlerEndY, batSprite, col,
                    p -> playIntroAnimation(batSprite));            // :534
            if (shadowVisible && shaSprite != null) {               // :535
                PictureEx shadow = addSprite(shaSprite, PictureEx.Origin.CENTER);   // :537
                shadow.setOpacity(0, 0);                            // :538
                shadow.setVisible(delay, shadowVisible);            // :540
                shadow.moveOpacity(delay + 5, 10, 255);             // :541
            }
        }

        /**
         * {@code PokeballTrainerSendOutAnimation#createBallTrajectory}
         * (PokeBattle_SceneAnimations:545-550): no arc - the ball simply appears
         * where it opens.
         */
        private void createBallTrajectory(PictureEx ball, float destX, float destY) {
            ball.setXY(0, destX, destY - 4);                        // :549
        }
    }

    // ==================================================================
    // BattlerRecallAnimation (PokeBattle_SceneAnimations:558-603)
    // ==================================================================

    /**
     * {@code pbRecall} (Scene_Animations:148-157) plays this first, then a
     * {@link DataBoxDisappearAnimation}.
     */
    static final class BattlerRecallAnimation extends BattleAnimation {
        private final int idxBattler;

        BattlerRecallAnimation(Scene scene, int idxBattler) {
            super(scene);
            this.idxBattler = idxBattler;
            start();
        }

        /** PokeBattle_SceneAnimations:566-602. */
        @Override
        protected void createProcesses() {
            BattleSprite batSprite = sprites().get("pokemon_" + idxBattler);      // :567
            BattleSprite shaSprite = sprites().get("shadow_" + idxBattler);       // :568
            // Calculate the Poké Ball graphic to use (:570-573)
            int ballType = 0;
            if (batSprite != null && batSprite.battler != null && batSprite.battler.pokemon != null) {
                ballType = Math.max(0, batSprite.battler.pokemon.ballused);
            }
            // Calculate the color to turn the battler sprite (:575-576)
            float[] col = getBattlerColorFromBallType(ballType);
            col[3] = 0;
            // Calculate end coordinates for battler sprite movement (:578-580)
            float[] ballPos = PokeBattle_SceneConstants.battlerPosition(idxBattler,
                    scene().sideSize(idxBattler));
            float battlerEndX = ballPos[0];
            float battlerEndY = ballPos[1];
            // Set up battler sprite (:582-584)
            PictureEx battler = addSprite(batSprite, PictureEx.Origin.BOTTOM);
            battler.setVisible(0, true);
            battler.setColor(0, col);
            // Set up Poké Ball sprite (:586-587)
            PictureEx ball = addBallSprite(battlerEndX, battlerEndY, ballType);
            ball.setZ(0, batSprite.z + 1);
            // Poké Ball animation (:589-592)
            ballOpenUp(ball, 0, ballType);
            int delay = ball.totalDuration();                       // :590
            ballBurstRecall(delay, battlerEndX, battlerEndY, ballType);   // :591
            ball.moveOpacity(10, 2, 0);                             // :592
            // Battler animation (:594)
            battlerAbsorb(battler, delay, battlerEndX, battlerEndY, col);
            if (shaSprite != null && shaSprite.visible) {            // :595
                PictureEx shadow = addSprite(shaSprite, PictureEx.Origin.CENTER);   // :597
                shadow.moveOpacity(0, 10, 0);                       // :599
                shadow.setVisible(delay, false);                    // :600
            }
        }
    }

    // ==================================================================
    // BattlerFaintAnimation (PokeBattle_SceneAnimations:648-684)
    // ==================================================================

    /**
     * {@code pbFaintBattler} (Scene_Animations:299-312) plays this when a
     * battler's HP reaches 0: the battler cries, then drops down, fades out and
     * is hidden while its shadow disappears.
     */
    static final class BattlerFaintAnimation extends BattleAnimation {
        /** {@code @idxBattler} (:650); {@code @battle} (:651) is the Scene, which answers {@code pbSideSize} (:663-664). */
        private final int idxBattler;

        BattlerFaintAnimation(Scene scene, int idxBattler) {
            super(scene);
            this.idxBattler = idxBattler;
            start();
        }

        /** PokeBattle_SceneAnimations:655-684. */
        @Override
        protected void createProcesses() {
            BattleSprite batSprite = sprites().get("pokemon_" + idxBattler);      // :656
            BattleSprite shaSprite = sprites().get("shadow_" + idxBattler);       // :657
            if (batSprite == null || shaSprite == null) {
                // Ruby raises NoMethodError on the nil sprite (:662 batSprite.y).
                // pbInitSprites (Scene_Initialize:107-174) creates both slots for
                // every battler, so this branch is unreachable; it only keeps a
                // missing sprite from crashing the render loop.
                return;
            }
            // Set up battler/shadow sprite
            PictureEx battler = addSprite(batSprite, PictureEx.Origin.BOTTOM);    // :659
            PictureEx shadow = addSprite(shaSprite, PictureEx.Origin.CENTER);     // :660
            // Get approx duration depending on sprite's position/size. Min 20 frames.
            float battlerTop = batSprite.y - batSprite.bitmapHeight();            // :662
            float[] position = PokeBattle_SceneConstants.battlerPosition(idxBattler,
                    scene().sideSize(idxBattler));                                // :663-664
            // p[1] is an Integer in the plugin (PLAYER_BASE_Y/FOE_BASE_Y and the
            // sideSize shifts are all Integers, PokeBattle_SceneConstants:12-37).
            int cropY = (int) position[1];                                        // :664
            cropY += 8;                                                           // :665
            // Ruby divides two Integers here, so the result truncates (:666).
            int duration = (int) Math.floor((cropY - battlerTop) / 8);
            if (duration < 10) {
                duration = 10;   // :667 Min 0.5 seconds
            }
            // Animation
            // Play cry
            int delay = 10;                                                       // :670
            Pokemon pkmn = batSprite.battler == null
                    ? null : batSprite.battler.visiblePokemon();                  // batSprite.pkmn (:671): the Illusion / Transform picture
            String cry = scene().cryFile(pkmn);                                   // :671
            if (cry != null) {                                                    // :672
                battler.setSE(0, scene().cryFile(pkmn), null, 75);                // :673 75 is pitch
                // :674 calls pbCryFrameLength(pkmn) with no third argument, so
                // PSystem_FileUtilities:450's `pitch = 100 if !pitch` applies -
                // the 75 above is the pitch the cry is played at, not this one.
                delay = scene().cryFrameLength(pkmn, 100) * 20 / 40;              // :674
            }
            // Sprite drops down
            shadow.setVisible(delay, false);                                      // :677
            battler.setSE(delay, "Pkmn faint");                                   // :678
            battler.moveOpacity(delay, duration, 0);                              // :679
            battler.moveDelta(delay, duration, 0, cropY - battlerTop);            // :680
            battler.setCropBottom(delay, cropY);                                  // :681
            battler.setVisible(delay + duration, false);                          // :682
            battler.setOpacity(delay + duration, 255);                            // :683
        }
    }

    // ==================================================================
    // BattlerDamageAnimation (PokeBattle_SceneAnimations:610-641)
    // ==================================================================

    /**
     * Shows a battler flashing four times after taking damage, with the SE that
     * matches how effective the hit was. Started by
     * {@code Scene_Animations:239-256 pbHitAndHPLossAnimation} (several targets at
     * once) and by {@code Scene_Animations:224-234 pbDamageAnimation} (one
     * target, effectiveness 0).
     */
    static final class BattlerDamageAnimation extends BattleAnimation {
        /** {@code @idxBattler} (:612). */
        private final int idxBattler;
        /** {@code @effectiveness} (:613): 0 normal, 1 resisted, 2 super effective. */
        private final int effectiveness;

        BattlerDamageAnimation(Scene scene, int idxBattler, int effectiveness) {
            super(scene);
            this.idxBattler = idxBattler;
            this.effectiveness = effectiveness;
            start();
        }

        /** PokeBattle_SceneAnimations:617-640. */
        @Override
        protected void createProcesses() {
            BattleSprite batSprite = sprites().get("pokemon_" + idxBattler);      // :618
            BattleSprite shaSprite = sprites().get("shadow_" + idxBattler);       // :619
            if (batSprite == null || shaSprite == null) {
                // Ruby raises NoMethodError reading the nil sprite's .visible
                // (:633-634/:638-639). pbInitSprites (Scene_Initialize:107-174)
                // creates both slots for every battler, so this branch is
                // unreachable; it only keeps a missing sprite from crashing the
                // render loop - the same guard BattlerFaintAnimation uses.
                return;
            }
            // Set up battler/shadow sprite
            PictureEx battler = addSprite(batSprite, PictureEx.Origin.BOTTOM);    // :621
            PictureEx shadow = addSprite(shaSprite, PictureEx.Origin.CENTER);     // :622
            // Animation
            int delay = 0;                                                        // :624
            // :625-629 selects the SE by effectiveness; a value outside 0/1/2
            // matches no branch and therefore plays no sound.
            if (effectiveness == 0) {
                battler.setSE(delay, "Battle damage normal");                     // :626
            } else if (effectiveness == 1) {
                battler.setSE(delay, "Battle damage weak");                       // :627
            } else if (effectiveness == 2) {
                battler.setSE(delay, "Battle damage super");                      // :628
            }
            for (int i = 0; i < 4; i++) {   // :630 4 flashes, each lasting 0.2 (4/20) seconds
                battler.setVisible(delay, false);                                 // :631
                shadow.setVisible(delay, false);                                  // :632
                if (batSprite.visible) {
                    battler.setVisible(delay + 2, true);                          // :633
                }
                if (shaSprite.visible) {
                    shadow.setVisible(delay + 2, true);                           // :634
                }
                delay += 4;                                                       // :635
            }
            // Restore original battler/shadow sprites visibilities
            battler.setVisible(delay, batSprite.visible);                         // :638
            shadow.setVisible(delay, shaSprite.visible);                          // :639
        }
    }

    // ==================================================================
    // PokeballThrowCaptureAnimation (PokeBattle_SceneAnimations:748-895)
    // ==================================================================

    /** The player's Poké Ball being thrown at a wild Pokémon to capture it. */
    static final class PokeballThrowCaptureAnimation extends BattleAnimation {
        private final int ballType;
        private final int numShakes;
        private final boolean critCapture;
        private final Battler battler;
        private final boolean shadowVisible;
        private BattleSprite capturedBall;
        /** {@code @showingTrainer} (:752): only true in a Safari Zone battle. */
        private final boolean showingTrainer;

        PokeballThrowCaptureAnimation(Scene scene, int ballType, int numShakes, boolean critCapture,
                Battler battler) {
            this(scene, ballType, numShakes, critCapture, battler, false);
        }

        PokeballThrowCaptureAnimation(Scene scene, int ballType, int numShakes, boolean critCapture,
                Battler battler, boolean showingTrainer) {
            super(scene);
            this.showingTrainer = showingTrainer;
            this.ballType = ballType;                               // :753
            this.numShakes = critCapture ? 1 : numShakes;           // :754
            this.critCapture = critCapture;                         // :755
            this.battler = battler;                                 // :756
            BattleSprite shadow = sprites().get("shadow_" + battler.index);
            this.shadowVisible = shadow != null && shadow.visible;   // :758
            start();
        }

        /** PokeBattle_SceneAnimations:763-884 (the Safari trainer frames are not used). */
        @Override
        protected void createProcesses() {
            BattleSprite batSprite = sprites().get("pokemon_" + battler.index);   // :765
            BattleSprite shaSprite = sprites().get("shadow_" + battler.index);    // :766
            if (batSprite == null) {
                return;
            }
            float[] ballPos = PokeBattle_SceneConstants.battlerPosition(battler.index,
                    scene().sideSize(battler.index));               // :768
            float battlerStartX = batSprite.x;                      // :769
            float battlerStartY = batSprite.y;                      // :770
            float ballStartX = -6;                                  // :771
            float ballStartY = 246;                                 // :772
            float ballMidX = 0;                                     // :773
            float ballMidY = 78;                                    // :774
            float ballEndX = ballPos[0];                            // :775
            float ballEndY = 112;                                   // :776
            float ballGroundY = ballPos[1] - 4;                     // :777
            // Set up Poké Ball sprite
            PictureEx ball = addBallSprite(ballStartX, ballStartY, ballType);     // :779
            ball.setZ(0, batSprite.z + 1);                          // :780
            capturedBall = (numShakes >= 4 || critCapture) ? ballSprite() : null;  // :781
            // Set up trainer sprite (only visible in Safari Zone battles)
            BattleSprite traSprite = sprites().get("player_1");     // :768
            if (showingTrainer && traSprite != null) {              // :782
                if (traSprite.bitmapWidth() >= traSprite.bitmapHeight() * 2) {   // :783
                    PictureEx trainer = addSprite(traSprite, PictureEx.Origin.BOTTOM);   // :784
                    // Trainer animation
                    float[] start = trainerThrowingFrames(ball, trainer, traSprite);     // :786
                    ballStartX = start[0];
                    ballStartY = start[1];
                }
            }
            int delay = ball.totalDuration();                       // :790
            // Poké Ball arc animation
            ball.setSE(delay, "Battle throw");                      // :792
            createBallTrajectory(ball, delay, 16, ballStartX, ballStartY, ballMidX, ballMidY,
                    ballEndX, ballEndY);                            // :793-794
            ball.setZ(9, batSprite.z + 1);                          // :795
            ball.setSE(delay + 16, "Battle ball hit");              // :796
            // Poké Ball opens up
            delay = ball.totalDuration() + 6;                       // :798
            ballOpenUp(ball, delay, ballType, true, false);         // :799
            // Set up battler sprite
            PictureEx battlerPic = addSprite(batSprite, PictureEx.Origin.BOTTOM);  // :801
            // Poké Ball absorbs battler
            delay = ball.totalDuration();                           // :803
            ballBurstCapture(delay, ballEndX, ballEndY, ballType);  // :804
            delay = ball.totalDuration() + 4;                       // :805
            battlerPic.setSE(delay, "Battle jump to ball");         // :808
            battlerPic.moveXY(delay, 5, ballEndX, ballEndY);        // :809
            battlerPic.moveZoom(delay, 5, 0);                       // :810
            battlerPic.setVisible(delay + 5, false);                // :811
            PictureEx shadow = null;
            if (shadowVisible && shaSprite != null) {               // :812
                shadow = addSprite(shaSprite, PictureEx.Origin.CENTER);           // :814
                shadow.moveOpacity(delay, 5, 0);                    // :816
                shadow.moveZoom(delay, 5, 0);                       // :817
                shadow.setVisible(delay + 5, false);                // :818
            }
            // Poké Ball closes
            delay = battlerPic.totalDuration();                     // :821
            ballSetClosed(ball, delay, ballType);                   // :822
            ball.moveTone(delay, 3, new float[] { 96, 64, -160, 160 });           // :823
            ball.moveTone(delay + 5, 3, new float[] { 0, 0, 0, 0 });              // :824
            // Poké Ball critical capture animation
            delay = ball.totalDuration() + 3;                       // :826
            if (critCapture) {                                      // :827
                ball.setSE(delay, "Battle ball shake");
                ball.moveXY(delay, 1, ballEndX + 4, ballEndY);
                ball.moveXY(delay + 1, 2, ballEndX - 4, ballEndY);
                ball.moveXY(delay + 3, 2, ballEndX + 4, ballEndY);
                ball.setSE(delay + 4, "Battle ball shake");
                ball.moveXY(delay + 5, 2, ballEndX - 4, ballEndY);
                ball.moveXY(delay + 7, 1, ballEndX, ballEndY);
                delay = ball.totalDuration() + 3;                   // :835
            }
            // Poké Ball drops to the ground
            int[] ts = { 4, 4, 3, 2 };                              // :839 time per rise or fall
            int[] ds = { 1, 2, 4, 8 };                              // :840 fraction of the start height
            for (int i = 0; i < 4; i++) {
                int t = ts[i];
                int d = ds[i];
                if (i == 0) {
                    delay -= t;                                     // :841
                }
                if (i > 0) {                                        // :842
                    ball.setZoomXY(delay, 100 + 5 * (5 - i), 100 - 5 * (5 - i));   // :843 Squish
                    ball.moveZoom(delay, 2, 100);                   // :844 Unsquish
                    ball.moveXY(delay, t, ballEndX, ballGroundY - (ballGroundY - ballEndY) / d);   // :845
                }
                ball.moveXY(delay + t, t, ballEndX, ballGroundY);   // :847
                ball.setSE(delay + 2 * t, "Battle ball drop", 100 - i * 7, null);   // :848
                delay = ball.totalDuration();                       // :849
            }
            battlerPic.setXY(ball.totalDuration(), ballEndX, ballGroundY);        // :851
            // Poké Ball shakes
            delay = ball.totalDuration() + 12;                      // :853
            for (int i = 0; i < Math.min(numShakes, 3); i++) {      // :854
                ball.setSE(delay, "Battle ball shake");             // :855
                ball.moveXY(delay, 2, ballEndX - 2 * (4 - i), ballGroundY);       // :856
                ball.moveAngle(delay, 2, 5 * (4 - i));              // :857
                ball.moveXY(delay + 2, 4, ballEndX + 2 * (4 - i), ballGroundY);   // :858
                ball.moveAngle(delay + 2, 4, -5 * (4 - i));         // :859
                ball.moveXY(delay + 6, 2, ballEndX, ballGroundY);   // :860
                ball.moveAngle(delay + 6, 2, 0);                    // :861
                delay = ball.totalDuration() + 8;                   // :862
            }
            if (numShakes == 0 || (numShakes < 4 && !critCapture)) {   // :864
                // Poké Ball opens
                ball.setZ(delay, batSprite.z - 1);                  // :866
                ballOpenUp(ball, delay, ballType, false, true);     // :867
                ballBurst(delay, ballEndX, ballGroundY, ballType);  // :868
                ball.moveOpacity(delay + 2, 2, 0);                  // :869
                // Battler emerges
                float[] col = getBattlerColorFromBallType(ballType);   // :871
                col[3] = 255;                                       // :872
                battlerPic.setColor(delay, col);                    // :873
                battlerAppear(battlerPic, delay, battlerStartX, battlerStartY, batSprite, col,
                        p -> playIntroAnimation(batSprite));        // :874
                if (shadow != null) {                               // :875
                    shadow.setVisible(delay + 5, true);             // :876
                    shadow.setZoom(delay + 5, 100);                 // :877
                    shadow.moveOpacity(delay + 5, 10, 255);         // :878
                }
            } else {
                // Pokémon was caught
                ballCaptureSuccess(ball, delay, ballEndX, ballGroundY);           // :882
            }
        }

        /** :886-894: a successful capture's ball stays around as {@code @sprites["captureBall"]}. */
        @Override
        void dispose() {
            if (capturedBall != null) {
                keepTempSprite(capturedBall, "captureBall");
            }
            super.dispose();
        }
    }

    // ==================================================================
    // ThrowBaitAnimation / ThrowRockAnimation (160_PokeBattle_SafariZone:50-131)
    // ==================================================================

    /** The player throwing bait at a wild Pokemon in a Safari battle. */
    static final class ThrowBaitAnimation extends BattleAnimation {
        private final Battler battler;

        ThrowBaitAnimation(Scene scene, Battler battler) {
            super(scene);
            this.battler = battler;                                 // :54
            start();
        }

        /** :59-107 */
        @Override
        protected void createProcesses() {
            BattleSprite batSprite = sprites().get("pokemon_" + battler.index);   // :61
            BattleSprite traSprite = sprites().get("player_1");                   // :62
            if (batSprite == null || traSprite == null) {
                return;
            }
            float[] ballPos = PokeBattle_SceneConstants.battlerPosition(battler.index,
                    scene().sideSize(battler.index));               // :63
            float ballStartX = traSprite.x;                         // :64
            float ballStartY = traSprite.y - traSprite.bitmapHeight() / 2f;       // :65
            float ballMidX = 0;                                     // :66 Unused in arc calculation
            float ballMidY = 122;                                   // :67
            float ballEndX = ballPos[0] - 40;                       // :68
            float ballEndY = ballPos[1] - 4;                        // :69
            // Set up trainer sprite
            PictureEx trainer = addSprite(traSprite, PictureEx.Origin.BOTTOM);   // :71
            // Set up bait sprite
            PictureEx ball = addNewSprite(ballStartX, ballStartY,
                    "Graphics/Battle animations/safari_bait", PictureEx.Origin.CENTER);   // :73-74
            ball.setZ(0, batSprite.z + 1);                          // :75
            // Trainer animation
            if (traSprite.bitmapWidth() >= traSprite.bitmapHeight() * 2) {        // :77
                float[] start = trainerThrowingFrames(ball, trainer, traSprite);  // :78
                ballStartX = start[0];
                ballStartY = start[1];
            }
            int delay = ball.totalDuration();                       // :80 0 or 7
            // Bait arc animation
            ball.setSE(delay, "Battle throw");                      // :82
            createBallTrajectory(ball, delay, 12, ballStartX, ballStartY, ballMidX, ballMidY,
                    ballEndX, ballEndY);                            // :83-84
            ball.setZ(9, batSprite.z + 1);                          // :85
            delay = ball.totalDuration();                           // :86
            ball.moveOpacity(delay + 8, 2, 0);                      // :87
            ball.setVisible(delay + 10, false);                     // :88
            // Set up battler sprite
            PictureEx battlerPic = addSprite(batSprite, PictureEx.Origin.BOTTOM);   // :90
            // Show Pokémon jumping before eating the bait
            delay = ball.totalDuration() + 3;                       // :92
            for (int i = 0; i < 2; i++) {                           // :93
                battlerPic.setSE(delay, "player jump");             // :94
                battlerPic.moveDelta(delay, 3, 0, -16);             // :95
                battlerPic.moveDelta(delay + 4, 3, 0, 16);          // :96
                delay = battlerPic.totalDuration() + 1;             // :97
            }
            // Show Pokémon eating the bait
            delay = battlerPic.totalDuration() + 3;                 // :100
            for (int i = 0; i < 2; i++) {                           // :101
                battlerPic.moveAngle(delay, 7, 5);                  // :102
                battlerPic.moveDelta(delay, 7, 0, 6);               // :103
                battlerPic.moveAngle(delay + 7, 7, 0);              // :104
                battlerPic.moveDelta(delay + 7, 7, 0, -6);          // :105
                delay = battlerPic.totalDuration();                 // :106
            }
        }
    }

    /** The player throwing a rock at a wild Pokemon in a Safari battle. */
    static final class ThrowRockAnimation extends BattleAnimation {
        private final Battler battler;

        ThrowRockAnimation(Scene scene, Battler battler) {
            super(scene);
            this.battler = battler;                                 // :117
            start();
        }

        /** :122-173 */
        @Override
        protected void createProcesses() {
            BattleSprite batSprite = sprites().get("pokemon_" + battler.index);   // :124
            BattleSprite traSprite = sprites().get("player_1");                   // :125
            if (batSprite == null || traSprite == null) {
                return;
            }
            float ballStartX = traSprite.x;                         // :126
            float ballStartY = traSprite.y - traSprite.bitmapHeight() / 2f;       // :127
            float ballMidX = 0;                                     // :128 Unused in arc calculation
            float ballMidY = 122;                                   // :129
            float ballEndX = batSprite.x;                           // :130
            float ballEndY = batSprite.y - batSprite.bitmapHeight() / 2f;         // :131
            // Set up trainer sprite
            PictureEx trainer = addSprite(traSprite, PictureEx.Origin.BOTTOM);   // :133
            // Set up rock sprite
            PictureEx ball = addNewSprite(ballStartX, ballStartY,
                    "Graphics/Battle animations/safari_rock", PictureEx.Origin.CENTER);   // :135-136
            ball.setZ(0, batSprite.z + 1);                          // :137
            // Trainer animation
            if (traSprite.bitmapWidth() >= traSprite.bitmapHeight() * 2) {        // :139
                float[] start = trainerThrowingFrames(ball, trainer, traSprite);  // :140
                ballStartX = start[0];
                ballStartY = start[1];
            }
            int delay = ball.totalDuration();                       // :142 0 or 7
            // Rock arc animation
            ball.setSE(delay, "Battle throw");                      // :144
            createBallTrajectory(ball, delay, 12, ballStartX, ballStartY, ballMidX, ballMidY,
                    ballEndX, ballEndY);                            // :145-146
            ball.setZ(9, batSprite.z + 1);                          // :147
            delay = ball.totalDuration();                           // :148
            ball.setSE(delay, "Battle damage weak");                // :149
            ball.moveOpacity(delay + 2, 2, 0);                      // :150
            ball.setVisible(delay + 4, false);                      // :151
            // Set up anger sprite
            PictureEx anger = addNewSprite(ballEndX - 42, ballEndY - 36,
                    "Graphics/Battle animations/safari_anger", PictureEx.Origin.CENTER);   // :153-154
            anger.setVisible(0, false);                             // :155
            anger.setZ(0, batSprite.z + 1);                         // :156
            // Show anger appearing
            delay = ball.totalDuration() + 5;                       // :158
            for (int i = 0; i < 2; i++) {                           // :159
                anger.setSE(delay, "Player jump");                  // :160
                anger.setVisible(delay, true);                      // :161
                anger.moveZoom(delay, 3, 130);                      // :162
                anger.moveZoom(delay + 3, 3, 100);                  // :163
                anger.setVisible(delay + 6, false);                 // :164
                anger.setDelta(delay + 6, 96, -16);                 // :165
                delay = anger.totalDuration() + 3;                  // :166
            }
        }
    }

    // ==================================================================
    // PokeballThrowDeflectAnimation (PokeBattle_SceneAnimations:902-934)
    // ==================================================================

    /** The player's Poké Ball being thrown and batted away by a trainer. */
    static final class PokeballThrowDeflectAnimation extends BattleAnimation {
        private final int ballType;
        private final Battler battler;

        PokeballThrowDeflectAnimation(Scene scene, int ballType, Battler battler) {
            super(scene);
            this.ballType = ballType;                               // :906
            this.battler = battler;                                 // :907
            start();
        }

        /** PokeBattle_SceneAnimations:911-933. */
        @Override
        protected void createProcesses() {
            BattleSprite batSprite = sprites().get("pokemon_" + battler.index);   // :913
            if (batSprite == null) {
                return;
            }
            float[] ballPos = PokeBattle_SceneConstants.battlerPosition(battler.index,
                    scene().sideSize(battler.index));               // :914
            float ballStartX = -6;                                  // :915
            float ballStartY = 246;                                 // :916
            float ballMidX = 190;                                   // :917
            float ballMidY = 78;                                    // :918
            float ballEndX = ballPos[0];                            // :919
            float ballEndY = 112;                                   // :920
            PictureEx ball = addBallSprite(ballStartX, ballStartY, ballType);     // :922
            ball.setZ(0, 90);                                       // :923
            ball.setSE(0, "Battle throw");                          // :925
            createBallTrajectory(ball, 0, 16, ballStartX, ballStartY, ballMidX, ballMidY,
                    ballEndX, ballEndY);                            // :926-927
            // Poké Ball knocked back
            int delay = ball.totalDuration();                       // :929
            ball.setSE(delay, "Battle ball drop");                  // :930
            ball.moveXY(delay, 8, -32, PictureEx.Graphics.HEIGHT - 96 + 32);      // :931
            createBallTumbling(ball, delay, 8);                     // :932
        }
    }

    // ==================================================================
    // pbHideCaptureBall (Scene_Animations:373-387)
    // ==================================================================

    /**
     * The kept capture ball fading out ({@code ball.opacity -= 12*20/40} per
     * frame, i.e. 6 per frame) next to the data box's
     * {@link DataBoxDisappearAnimation}.
     */
    static final class CaptureBallFadeAnimation extends BattleAnimation {
        private final BattleSprite ballSprite;

        CaptureBallFadeAnimation(Scene scene, BattleSprite ballSprite) {
            super(scene);
            this.ballSprite = ballSprite;
            start();
        }

        @Override
        protected void createProcesses() {
            if (ballSprite == null) {
                return;
            }
            PictureEx ball = addSprite(ballSprite, PictureEx.Origin.CENTER);
            ball.moveOpacity(0, 43, 0);                             // 255 / 6 per frame
        }
    }
}
