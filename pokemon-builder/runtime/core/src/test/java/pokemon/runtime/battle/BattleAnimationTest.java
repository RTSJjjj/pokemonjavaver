package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B1: the transcribed battle animation classes
 * ({@code PokeBattle_SceneAnimations:4-551} and the project's
 * {@code Follower_Main:727-813} override). Frame counts, positions and the
 * sprite state after each {@code pbUpdate} are the evidence that the Ruby was
 * ported line by line.
 */
class BattleAnimationTest {

    @TempDir
    Path tempDir;

    private PbsData pbs;
    private Battle battle;

    /** A headless stand-in for PokeBattle_Scene's sprite table and battle view. */
    private final class FakeScene implements BattleAnimation.Scene {
        final BattleSprites sprites = new BattleSprites();
        final Map<String, int[]> sizes = new HashMap<>();

        @Override public BattleSprites sprites() { return sprites; }
        @Override public PictureEx.SePlayer sePlayer() { return (name, volume, pitch) -> { }; }
        @Override public BattleAnimation.BitmapSize bitmapSize() { return sizes::get; }
        @Override public boolean trainerBattle() { return battle.trainerBattle; }
        @Override public int playerCount() { return 1; }
        @Override public int opponentCount() { return battle.trainerBattle ? 1 : 0; }
        @Override public Array<Battler> party(int side) {
            return side == 0 ? battle.playerParty() : battle.foeParty();
        }
        @Override public int[] partyStarts(int side) { return new int[] { 0 }; }
        @Override public int battlerCount() { return 2; }
        @Override public Battler battler(int index) { return BattleSendOut.battler(battle, index); }
        @Override public boolean opposes(int idxBattler) { return (idxBattler & 1) != 0; }
        @Override public int ownerIndex(int idxBattler) { return 0; }
        @Override public int sideSize(int idxBattler) { return 1; }
        @Override public String playerTrainerType() { return "POKEMONTRAINER_Red"; }
        @Override public boolean followerSprite() { return false; }
        @Override public String cryFile(Pokemon pokemon) { return null; }
        @Override public void changePokemon(int idxBattler, Battler battler) {
            BattleSprite sprite = sprites.get("pokemon_" + idxBattler);
            if (sprite != null) {
                sprite.battler = battler;
                sprite.bitmapWidth = 160;
                sprite.bitmapHeight = 160;
            }
        }
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(tempDir, "pokemon.json", "{\"total\":1,\"byId\":{},\"species\":{"
                + "\"BULBASAUR\":{\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"妙蛙种子\","
                + "\"types\":[\"GRASS\"],\"baseStats\":[45,49,49,45,65,65],\"rareness\":45,"
                + "\"weight\":6.9,\"genderRate\":\"Female50Percent\",\"evolutions\":[]}}}");
        write(tempDir, "moves.json", "{\"total\":0,\"moves\":{}}");
        pbs = PbsData.parse(tempDir.toFile());
        battle = new Battle(pbs, new Random(1), (user, foe, moves) -> 0);
        battle.addPlayer(new Pokemon(pbs.species("BULBASAUR"), 5, pbs));
        battle.addFoe(new Pokemon(pbs.species("BULBASAUR"), 5, pbs));
    }

    private FakeScene scene() {
        FakeScene scene = new FakeScene();
        scene.sizes.put("Graphics/Pictures/Battle/overlay_lineup", new int[] { 440, 8 });
        scene.sizes.put("Graphics/Pictures/Battle/icon_ball", new int[] { 32, 32 });
        scene.sizes.put("Graphics/Pictures/Battle/icon_ball_empty", new int[] { 32, 32 });
        scene.sizes.put("Graphics/Pictures/Battle/icon_ball_faint", new int[] { 32, 32 });
        scene.sizes.put("Graphics/Pictures/Battle/icon_ball_status", new int[] { 32, 32 });
        for (int i = 0; i < 28; i++) {
            scene.sizes.put(String.format(java.util.Locale.ROOT,
                    "Graphics/Battle animations/ball_%02d", i), new int[] { 256, 64 });
            scene.sizes.put(String.format(java.util.Locale.ROOT,
                    "Graphics/Battle animations/ball_%02d_open", i), new int[] { 32, 64 });
        }
        scene.sizes.put("Graphics/Trainers/trainer000", new int[] { 160, 160 });
        return scene;
    }

    /**
     * Runs exactly {@code frames} {@code pbUpdate}s. Note that
     * {@code PokeBattle_Animation#animDone?} only becomes true one frame after
     * the last process ends: {@code finished} is already false on the frame a
     * process completes (PokeBattle_Animation:50-57).
     */
    private static void run(BattleAnimation animation, int frames) {
        for (int i = 0; i < frames; i++) {
            animation.update();
        }
    }

    private BattleSprite add(FakeScene scene, String key, BattleSprite.Kind kind, float x, float y) {
        BattleSprite sprite = scene.sprites.add(key, kind);
        sprite.x = x;
        sprite.y = y;
        return sprite;
    }

    @Test
    @DisplayName("DataBoxAppearAnimation slides the box in over 16 frames (:189-203)")
    void dataBoxAppear() {
        FakeScene scene = scene();
        BattleSprite box = add(scene, "dataBox_1", BattleSprite.Kind.DATA_BOX, 440, 5);
        box.visible = false;
        BattleAnimation animation = new BattleAnimations.DataBoxAppearAnimation(scene, 1);

        animation.update();                    // frame 1: setVisible(0,true) + setDelta(0,+336)
        assertTrue(box.visible);
        assertEquals(440 + 336, box.x, 1e-3f, "dir = +1 for an odd index (:199)");
        run(animation, 16);                    // moveDelta(0,8,...) is 16 frames long
        assertEquals(440, box.x, 1e-3f, "back at its own x after the 16-frame slide (:201)");
        run(animation, 1);
        assertTrue(animation.animDone());
    }

    @Test
    @DisplayName("DataBoxAppearAnimation slides the player's box in from the left (:199-200)")
    void dataBoxAppearPlayerSide() {
        FakeScene scene = scene();
        BattleSprite box = add(scene, "dataBox_0", BattleSprite.Kind.DATA_BOX, 0, 5);
        BattleAnimation animation = new BattleAnimations.DataBoxAppearAnimation(scene, 0);
        animation.update();
        assertEquals(-336, box.x, 1e-3f, "dir = -1 for an even index");
        run(animation, 20);
        assertEquals(0, box.x, 1e-3f);
    }

    @Test
    @DisplayName("DataBoxDisappearAnimation slides out and hides at frame 16 (:210-223)")
    void dataBoxDisappear() {
        FakeScene scene = scene();
        BattleSprite box = add(scene, "dataBox_0", BattleSprite.Kind.DATA_BOX, 0, 5);
        box.visible = true;
        BattleAnimation animation = new BattleAnimations.DataBoxDisappearAnimation(scene, 0);
        run(animation, 20);
        assertEquals(-336, box.x, 1e-3f);
        assertFalse(box.visible, "setVisible(8,false) -> frame 16 (:221)");
    }

    @Test
    @DisplayName("PlayerFadeAnimation slides the player off and hides it (:306-351)")
    void playerFade() {
        FakeScene scene = scene();
        BattleSprite trainer = add(scene, "player_1", BattleSprite.Kind.TRAINER_BACK, 128, 320);
        BattleSprite bar = add(scene, "partyBar_0", BattleSprite.Kind.IMAGE, -192, 50);
        BattleSprite ball = add(scene, "partyBall_0_0", BattleSprite.Kind.IMAGE, 207, 20);
        bar.visible = true;
        ball.visible = true;
        ball.name = "Graphics/Pictures/Battle/icon_ball";
        BattleAnimation animation = new BattleAnimations.PlayerFadeAnimation(scene, true);

        // moveDelta(0,16,-336,0) -> 32 frames; setVisible(16,false) -> frame 32.
        run(animation, 30);
        assertTrue(trainer.visible, "the trainer is hidden on frame 32, i.e. the 33rd update (:331)");
        assertTrue(bar.visible, "the bar is hidden on frame 30, i.e. the 31st update (:339)");
        assertEquals(Math.round(128 - 336f * 29f / 32f), trainer.x, 1e-3f,
                "sprite.x = picture.x.round (PictureEx:459)");
        for (int i = 0; i < 200 && !animation.animDone(); i++) {
            animation.update();
        }
        assertFalse(trainer.visible);
        assertEquals(128 - 336f, trainer.x, 1e-3f);
        assertFalse(bar.visible);
        assertFalse(ball.visible);
        assertEquals(207 - 672f, ball.x, 1e-3f, "moveDelta(delay+2*(6-i),16,-672,0) (:345)");
    }

    @Test
    @DisplayName("TrainerFadeAnimation slides the opponent off to the right (:359-396)")
    void trainerFade() {
        FakeScene scene = scene();
        battle.trainerBattle = true;
        BattleSprite trainer = add(scene, "trainer_1", BattleSprite.Kind.TRAINER_FRONT, 544, 318);
        BattleAnimation animation = new BattleAnimations.TrainerFadeAnimation(scene, true);
        run(animation, 40);
        assertEquals(544 + 336f, trainer.x, 1e-3f, "moveDelta(0,16,+336,0) (:375)");
        assertFalse(trainer.visible);
    }

    @Test
    @DisplayName("LineupAppearAnimation places the bar and balls, then slides them in (:88-182)")
    void lineupAppear() {
        FakeScene scene = scene();
        for (int side = 0; side < 2; side++) {
            add(scene, "partyBar_" + side, BattleSprite.Kind.IMAGE, 0, 0);
            for (int i = 0; i < 6; i++) {
                add(scene, "partyBall_" + side + "_" + i, BattleSprite.Kind.IMAGE, 0, 0);
            }
        }
        Array<Pokemon> party = new Array<>();
        party.add(battle.player().pokemon);
        BattleAnimation animation = new BattleAnimations.LineupAppearAnimation(scene, 0, party,
                new int[] { 0 }, true);

        // resetGraphics (:100-128): side 0's bar sits at 248 - 440 and its balls
        // walk left from 207 by 32 each.
        BattleSprite bar = scene.sprites.get("partyBar_0");
        assertEquals(248 - 440, bar.x, 1e-3f);
        assertEquals(50, bar.y, 1e-3f);
        assertEquals(207, scene.sprites.get("partyBall_0_0").x, 1e-3f);
        assertEquals(207 - 32, scene.sprites.get("partyBall_0_1").x, 1e-3f);
        assertEquals(20, scene.sprites.get("partyBall_0_0").y, 1e-3f);
        assertFalse(bar.visible, "resetGraphics hides them (:119/:125)");

        // A side-1 animation resets the opposing bar instead (:102-107).
        BattleAnimation foeLineup = new BattleAnimations.LineupAppearAnimation(scene, 1,
                new Array<>(), new int[] { 0 }, true);
        assertEquals(424, scene.sprites.get("partyBar_1").x, 1e-3f, "Graphics.width-248 (:104)");
        assertEquals(435, scene.sprites.get("partyBall_1_0").x, 1e-3f, "width/2+35+64 (:106)");
        assertNotNull(foeLineup);

        animation.update();
        assertTrue(bar.visible, "createProcesses shows the bar at delay 0 (:152)");
    }

    @Test
    @DisplayName("LineupAppearAnimation names each ball by its party member (:162-181)")
    void lineupBallGraphics() {
        FakeScene scene = scene();
        for (int side = 0; side < 2; side++) {
            add(scene, "partyBar_" + side, BattleSprite.Kind.IMAGE, 0, 0);
            for (int i = 0; i < 6; i++) {
                add(scene, "partyBall_" + side + "_" + i, BattleSprite.Kind.IMAGE, 0, 0);
            }
        }
        Array<Pokemon> party = new Array<>();
        party.add(battle.player().pokemon);
        // fullAnim=false puts every ball at delay 0, like a switch does (:158).
        BattleAnimation animation = new BattleAnimations.LineupAppearAnimation(scene, 0, party,
                new int[] { 0 }, false);
        animation.update();
        // createBall (:162-181): the party's only able Pokemon is icon_ball.
        assertEquals("Graphics/Pictures/Battle/icon_ball",
                scene.sprites.get("partyBall_0_0").name);
        assertEquals("Graphics/Pictures/Battle/icon_ball_empty",
                scene.sprites.get("partyBall_0_1").name, "no party member behind it (:165)");
    }

    @Test
    @DisplayName("LineupAppearAnimation marks a fainted member (:167-168)")
    void lineupFaintedBall() {
        FakeScene scene = scene();
        for (int side = 0; side < 2; side++) {
            add(scene, "partyBar_" + side, BattleSprite.Kind.IMAGE, 0, 0);
            for (int i = 0; i < 6; i++) {
                add(scene, "partyBall_" + side + "_" + i, BattleSprite.Kind.IMAGE, 0, 0);
            }
        }
        Pokemon fainted = battle.player().pokemon;
        fainted.hp = 0;
        Array<Pokemon> party = new Array<>();
        party.add(fainted);
        BattleAnimation animation = new BattleAnimations.LineupAppearAnimation(scene, 0, party,
                new int[] { 0 }, false);
        animation.update();
        assertEquals("Graphics/Pictures/Battle/icon_ball_faint",
                scene.sprites.get("partyBall_0_0").name);
    }

    @Test
    @DisplayName("PokeballTrainerSendOutAnimation puts the ball where it opens (:486-551)")
    void pokeballTrainerSendOut() {
        FakeScene scene = scene();
        BattleSprite pokemon = add(scene, "pokemon_1", BattleSprite.Kind.POKEMON,
                PokeBattle_SceneConstants.FOE_BASE_X, PokeBattle_SceneConstants.FOE_BASE_Y);
        pokemon.battler = battle.foe();
        pokemon.z = 52;
        add(scene, "shadow_1", BattleSprite.Kind.SHADOW, 0, 0);
        BattleAnimation animation = new BattleAnimations.PokeballTrainerSendOutAnimation(scene,
                battle.foe(), true, 0);
        assertFalse(pokemon.visible, "initialize hides the battler (:494)");

        // createBallTrajectory is overridden to setXY(0,destX,destY-4) (:545-550).
        BattleSprite ball = tempSprite(scene, "ball_00");
        assertNotNull(ball, "the ball sprite is created by addBallSprite (:518)");
        animation.update();
        assertEquals(PokeBattle_SceneConstants.FOE_BASE_X, ball.x, 1e-3f);
        assertEquals(PokeBattle_SceneConstants.FOE_BASE_Y - 4, ball.y, 1e-3f);
        assertEquals(51, ball.z, "ball.setZ(0,batSprite.z-1) (:519)");
        // The opponent's ball is never explicitly shown: addNewSprite's
        // IconSprite starts visible and the override queues no Visible step.
        assertTrue(ball.visible);
    }

    @Test
    @DisplayName("PokeballPlayerSendOutAnimation tracks the player's hand (Follower_Main:763-787)")
    void pokeballPlayerSendOut() {
        FakeScene scene = scene();
        BattleSprite trainer = add(scene, "player_1", BattleSprite.Kind.TRAINER_BACK, 128, 320);
        trainer.name = "Graphics/Trainers/trainer000";
        trainer.bitmapWidth = 160;
        trainer.bitmapHeight = 160;
        BattleSprite pokemon = add(scene, "pokemon_0", BattleSprite.Kind.POKEMON,
                PokeBattle_SceneConstants.PLAYER_BASE_X, PokeBattle_SceneConstants.PLAYER_BASE_Y);
        pokemon.battler = battle.player();
        pokemon.z = 50;
        add(scene, "shadow_0", BattleSprite.Kind.SHADOW, 0, 0);
        BattleAnimation animation = new BattleAnimations.PokeballPlayerSendOutAnimation(scene, 1,
                battle.player(), true, 0);
        assertFalse(pokemon.visible);

        // ballTracksHand (:98-105): a 160x160 back sprite is not animated, so the
        // ball starts at the trainer's own position minus its 7 (1/20 s) delay
        // scaled by Graphics.width/(2*16) = 21, i.e. 128 - 147 = -19.
        BattleSprite ball = tempSprite(scene, "ball_00");
        assertNotNull(ball);
        animation.update();
        assertEquals(-6, ball.x, 1e-3f, "addBallSprite's own start position (:768)");
        assertEquals(202, ball.y, 1e-3f);
        assertFalse(ball.visible, "ball.setVisible(0,false) (:770)");
        // createBallTrajectory's first step starts on frame 14 (delay 7 in
        // 1/20 s) and interpolates from the hand-tracked start to the battler;
        // the value it lands on is that start (an XY process shows its own
        // start value on its first frame), reached on the frame after.
        run(animation, 16);
        assertTrue(ball.visible, "ball.setVisible(delay,true) (:166)");
        float startX = -19f;
        float endX = PokeBattle_SceneConstants.PLAYER_BASE_X;
        assertEquals(Math.round(startX + (endX - startX) * 1f / 12f), ball.x, 1e-3f);
    }

    @Test
    @DisplayName("SendOutSequence runs the fade and the data box until both are done (:119-131)")
    void sendOutSequence() {
        FakeScene scene = scene();
        BattleSprite pokemon = add(scene, "pokemon_1", BattleSprite.Kind.POKEMON,
                PokeBattle_SceneConstants.FOE_BASE_X, PokeBattle_SceneConstants.FOE_BASE_Y);
        pokemon.battler = battle.foe();
        pokemon.z = 52;
        add(scene, "shadow_1", BattleSprite.Kind.SHADOW, 0, 0);
        BattleSprite box = add(scene, "dataBox_1", BattleSprite.Kind.DATA_BOX, 440, 5);
        box.visible = false;
        add(scene, "player_1", BattleSprite.Kind.TRAINER_BACK, 128, 320);

        BattleAnimations.SendOutSequence sequence = new BattleAnimations.SendOutSequence(scene,
                new ArrayList<>(List.of(new int[] { 1 })), true);
        assertFalse(sequence.done(), "the animations have not run yet");
        for (int i = 0; i < 200 && !sequence.done(); i++) {
            sequence.update();
        }
        assertTrue(sequence.done());
        assertTrue(pokemon.visible, "the battler is out");
        assertTrue(box.visible, "and its data box is on screen");
    }

    @Test
    @DisplayName("a wild battle's data box is already out, so only the battler animates (:164-173)")
    void wildIntroDataBox() {
        FakeScene scene = scene();
        BattleSprite box = add(scene, "dataBox_1", BattleSprite.Kind.DATA_BOX, 440, 5);
        box.visible = false;
        BattleAnimation animation = new BattleAnimations.DataBoxAppearAnimation(scene, 1);
        run(animation, 20);
        assertTrue(box.visible);
        assertEquals(440, box.x, 1e-3f);
    }

    @Test
    @DisplayName("BattleIntroAnimation2 fades the wild battler's tone back to normal (:66-81)")
    void battleIntroAnimation2() {
        FakeScene scene = scene();
        BattleSprite pokemon = add(scene, "pokemon_1", BattleSprite.Kind.POKEMON,
                PokeBattle_SceneConstants.FOE_BASE_X, PokeBattle_SceneConstants.FOE_BASE_Y);
        pokemon.battler = battle.foe();
        pokemon.tone = new float[] { -80, -80, -80, 0 };
        BattleAnimation animation = new BattleAnimations.BattleIntroAnimation2(scene, 1);
        run(animation, 10);
        assertArrayEquals(new float[] { 0, 0, 0, 0 }, pokemon.tone, 1e-3f,
                "moveTone(0,4,Tone.new(0,0,0,0)) (:77)");
    }

    private static BattleSprite tempSprite(FakeScene scene, String bitmapName) {
        for (BattleSprite sprite : scene.sprites.all()) {
            if (sprite.name != null && sprite.name.endsWith(bitmapName)) {
                return sprite;
            }
        }
        return null;
    }

    @Test
    @DisplayName("the thrown ball turns around its own centre (addNewSprite:41 setBitmap + PictureEx:487/495)")
    void ballOriginIsItsCentre() {
        FakeScene scene = scene();
        BattleSprite pokemon = add(scene, "pokemon_0", BattleSprite.Kind.POKEMON,
                PokeBattle_SceneConstants.PLAYER_BASE_X, PokeBattle_SceneConstants.PLAYER_BASE_Y);
        pokemon.battler = battle.player();
        add(scene, "shadow_0", BattleSprite.Kind.SHADOW, 0, 0);
        BattleAnimation animation = new BattleAnimations.PokeballPlayerSendOutAnimation(scene, 1,
                battle.player(), false, 0);
        animation.update();
        BattleSprite ball = null;
        for (BattleSprite sprite : scene.sprites.all()) {
            if ("Graphics/Battle animations/ball_00".equals(sprite.name)) ball = sprite;
        }
        assertNotNull(ball);
        // ball_00 is 256x64: one frame is 32x64 (addBallSprite:91-93), so the
        // Center origin is (16,32) - not (16,0), which spun the ball around its top edge.
        assertEquals(16f, ball.ox, 1e-3f);
        assertEquals(32f, ball.oy, 1e-3f);
    }
}
