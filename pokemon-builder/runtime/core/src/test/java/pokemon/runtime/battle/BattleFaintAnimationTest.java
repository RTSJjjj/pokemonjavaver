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
 * B3: {@code BattlerFaintAnimation} ({@code PokeBattle_SceneAnimations:648-684}),
 * the animation {@code pbFaintBattler} (Scene_Animations:299-312) plays when a
 * battler's HP reaches 0.
 *
 * <p>The evidence is the animation's observable timeline: which SE plays on
 * which {@code pbUpdate}, when the shadow disappears, how far the battler drops
 * and when it is hidden again. {@code ensureDelay} doubles the animation's
 * 1/20 s delays into 40 fps frames (PictureEx:147), so a process passed
 * {@code delay=d} runs on update {@code 2d+1}.</p>
 */
class BattleFaintAnimationTest {

    @TempDir
    Path tempDir;

    private PbsData pbs;
    private Battle battle;

    /** A headless stand-in for PokeBattle_Scene's sprite table and battle view. */
    private final class FakeScene implements BattleAnimation.Scene {
        final BattleSprites sprites = new BattleSprites();
        final Map<String, int[]> sizes = new HashMap<>();
        /** Every queued pbSEPlay, as "update:name/volume/pitch". */
        final List<String> se = new ArrayList<>();
        /** pbCryFile's answer for a real Pokemon; null means "no cry file". */
        String cryKey;
        /** pbCryFrameLength's answer. */
        int cryFrameLength;
        int cryFrameLengthCalls;
        int cryFrameLengthPitch = -1;
        /** Every pbSideSize argument (:663-664). */
        final List<Integer> sideSizeQueries = new ArrayList<>();
        /** {@code animation.update()} calls so far. */
        int frames;

        @Override public BattleSprites sprites() { return sprites; }
        @Override public PictureEx.SePlayer sePlayer() {
            return (name, volume, pitch) -> se.add(frames + ":" + name + "/" + volume + "/" + pitch);
        }
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
        @Override public int sideSize(int idxBattler) {
            sideSizeQueries.add(idxBattler);
            return 1;
        }
        @Override public String playerTrainerType() { return "POKEMONTRAINER_Red"; }
        @Override public boolean followerSprite() { return false; }
        @Override public String cryFile(Pokemon pokemon) { return pokemon == null ? null : cryKey; }
        @Override public int cryFrameLength(Pokemon pokemon, int pitch) {
            cryFrameLengthCalls++;
            cryFrameLengthPitch = pitch;
            return pokemon == null ? 0 : cryFrameLength;
        }
        @Override public void changePokemon(int idxBattler, Battler battler) { }
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

    private BattleSprite add(FakeScene scene, String key, BattleSprite.Kind kind, float x, float y) {
        BattleSprite sprite = scene.sprites.add(key, kind);
        sprite.x = x;
        sprite.y = y;
        return sprite;
    }

    /** Runs exactly {@code frames} {@code pbUpdate}s, counting them for the SE log. */
    private static void run(FakeScene scene, BattleAnimation animation, int frames) {
        for (int i = 0; i < frames; i++) {
            scene.frames++;
            animation.update();
        }
    }

    @Test
    @DisplayName("BattlerFaintAnimation cries, then drops and hides the battler (:655-684)")
    void faintWithCry() {
        FakeScene scene = new FakeScene();
        scene.cryKey = "001Cry";
        scene.cryFrameLength = 33;                       // 33*20/40 = 16, a truncated Integer division (:674)
        BattleSprite pokemon = add(scene, "pokemon_0", BattleSprite.Kind.POKEMON,
                PokeBattle_SceneConstants.PLAYER_BASE_X, PokeBattle_SceneConstants.PLAYER_BASE_Y);
        pokemon.battler = battle.player();
        pokemon.bitmapHeight = 165;                      // batSprite.height (:662)
        BattleSprite shadow = add(scene, "shadow_0", BattleSprite.Kind.SHADOW,
                PokeBattle_SceneConstants.PLAYER_BASE_X, PokeBattle_SceneConstants.PLAYER_BASE_Y - 8);
        BattleAnimation animation = new BattleAnimations.BattlerFaintAnimation(scene, 0);

        // :656-660: the animation drives the battler (Bottom) and its shadow (Center).
        assertEquals(2, animation.pictures().size());
        assertEquals(pokemon, animation.pictures().get(0).sprite());
        assertEquals(shadow, animation.pictures().get(1).sprite());
        // addSprite only *queues* setOrigin(0,origin) as a process
        // (PictureEx.forSprite:832); the field is written when update() runs it
        // (PictureEx:655-657, Ruby :418), so before the first update the picture
        // still reports its constructor default TOP_LEFT.
        assertEquals(PictureEx.Origin.TOP_LEFT, animation.pictures().get(0).origin(),
                "setOrigin(0,origin) has not run yet");
        run(scene, animation, 1);            // update 1 runs :659-660's origins and :673's cry
        assertEquals(PictureEx.Origin.BOTTOM, animation.pictures().get(0).origin(), ":659");
        assertEquals(PictureEx.Origin.CENTER, animation.pictures().get(1).origin(), ":660");
        // ...and applyTo copies it onto the sprite (PictureEx:781-785).
        assertEquals(PictureEx.Origin.BOTTOM, pokemon.origin);
        assertEquals(PictureEx.Origin.CENTER, shadow.origin);

        // :662-667
        float battlerTop = PokeBattle_SceneConstants.PLAYER_BASE_Y - 165;                    // 151
        int cropY = (int) PokeBattle_SceneConstants.battlerPosition(0, 1)[1] + 8;            // 324
        int duration = (int) Math.floor((cropY - battlerTop) / 8);                          // 173/8
        assertEquals(151f, battlerTop, 1e-3f, "battlerTop = batSprite.y-batSprite.height (:662)");
        assertEquals(324, cropY, "pbBattlerPosition(idx,sideSize)[1] + 8 (:663-665)");
        assertEquals(21, duration, "Ruby divides two Integers: 173/8 = 21, not 21.6 (:666)");
        assertEquals(List.of(0), scene.sideSizeQueries, "pbSideSize(@idxBattler) (:663-664)");

        // :673-674: the cry starts at once, the drop waits for it. Every delay
        // goes through PictureEx.ensureDelay (:145-147 / Ruby :99-105), which
        // doubles the animation's 1/20 s units: delay 16 -> frame 32, and a
        // process with frame delay d first runs on update d+1.
        int delay = 33 * 20 / 40;                                                            // 16
        assertEquals(16, delay, "pbCryFrameLength(pkmn)*20/40 (:674), an Integer division");
        assertEquals(100, scene.cryFrameLengthPitch,
                ":674 passes no pitch, so PSystem_FileUtilities:450 uses 100");

        run(scene, animation, delay * 2 - 1);                   // updates 2-32 = frames 1-31 of the delay
        assertEquals(List.of("1:001Cry/100/75"), scene.se,
                ":673 setSE(0,cry,nil,75) plays on the first update");
        assertTrue(pokemon.visible);
        assertTrue(shadow.visible, "delay 16 -> frame 32, so update 33 is the first one to run :677-681");
        assertEquals(-1, pokemon.cropBottom, ":681 has not run yet");

        run(scene, animation, 1);                               // update 33 = frame delay 32
        assertEquals(List.of("1:001Cry/100/75", "33:Pkmn faint/100/100"), scene.se,
                ":678 setSE(delay,\"Pkmn faint\") with pbSEPlay's 100/100 defaults");
        assertFalse(shadow.visible, ":677 shadow.setVisible(delay,false)");
        assertEquals(cropY, pokemon.cropBottom, ":681 battler.setCropBottom(delay,cropY)");
        assertEquals(PokeBattle_SceneConstants.PLAYER_BASE_Y, pokemon.y, 1e-3f,
                ":680 moveDelta's first frame still has delta 0");

        run(scene, animation, 41);                              // updates 34-74
        assertEquals(6f, pokemon.opacity, 1e-3f, ":679 moveOpacity(delay,duration,0) is fading");
        assertTrue(pokemon.visible, ":682 hides on update (delay+duration)*2+1 = 75");

        run(scene, animation, 1);                               // update 75 = delay+duration
        assertEquals(255f, pokemon.opacity, 1e-3f, ":683 setOpacity(delay+duration,255)");
        assertFalse(pokemon.visible, ":682 setVisible(delay+duration,false)");
        assertEquals(Math.round(PokeBattle_SceneConstants.PLAYER_BASE_Y + (cropY - battlerTop)),
                pokemon.y, 1e-3f, ":680 moveDelta(delay,duration,0,cropY-battlerTop)");
        run(scene, animation, 2);
        assertTrue(animation.animDone());
    }

    @Test
    @DisplayName("without a cry file the faint starts after the default 10 frames (:670-675)")
    void faintWithoutCry() {
        FakeScene scene = new FakeScene();
        scene.cryKey = null;                                    // pbCryFile -> nil
        BattleSprite pokemon = add(scene, "pokemon_1", BattleSprite.Kind.POKEMON,
                PokeBattle_SceneConstants.FOE_BASE_X, PokeBattle_SceneConstants.FOE_BASE_Y);
        pokemon.battler = battle.foe();
        pokemon.bitmapHeight = 165;
        BattleSprite shadow = add(scene, "shadow_1", BattleSprite.Kind.SHADOW,
                PokeBattle_SceneConstants.FOE_BASE_X, PokeBattle_SceneConstants.FOE_BASE_Y - 8);
        BattleAnimation animation = new BattleAnimations.BattlerFaintAnimation(scene, 1);

        float battlerTop = PokeBattle_SceneConstants.FOE_BASE_Y - 165;                       // 147
        int cropY = (int) PokeBattle_SceneConstants.battlerPosition(1, 1)[1] + 8;            // 320
        assertEquals(320, cropY);
        assertEquals(21, (int) Math.floor((cropY - battlerTop) / 8));

        run(scene, animation, 20);                              // delay stays 10 (:670)
        assertTrue(scene.se.isEmpty(), "no cry SE, and :678 runs no earlier than delay");
        assertTrue(pokemon.visible);
        assertEquals(List.of(1), scene.sideSizeQueries);

        run(scene, animation, 1);                               // update 21 = delay 10
        assertEquals(List.of("21:Pkmn faint/100/100"), scene.se, ":678");
        assertFalse(shadow.visible, ":677");
        assertEquals(320, pokemon.cropBottom, ":681");
        assertEquals(0, scene.cryFrameLengthCalls,
                ":672-675 only measures the cry when pbCryFile returned a file");
    }

    @Test
    @DisplayName("the drop duration has a 10-frame floor (:667)")
    void durationFloor() {
        FakeScene scene = new FakeScene();
        scene.cryKey = null;
        BattleSprite pokemon = add(scene, "pokemon_0", BattleSprite.Kind.POKEMON,
                PokeBattle_SceneConstants.PLAYER_BASE_X, PokeBattle_SceneConstants.PLAYER_BASE_Y);
        pokemon.battler = battle.player();
        pokemon.bitmapHeight = 64;                              // (324-252)/8 = 9
        add(scene, "shadow_0", BattleSprite.Kind.SHADOW,
                PokeBattle_SceneConstants.PLAYER_BASE_X, PokeBattle_SceneConstants.PLAYER_BASE_Y - 8);
        BattleAnimation animation = new BattleAnimations.BattlerFaintAnimation(scene, 0);

        float battlerTop = PokeBattle_SceneConstants.PLAYER_BASE_Y - 64;                     // 252
        int cropY = (int) PokeBattle_SceneConstants.battlerPosition(0, 1)[1] + 8;            // 324
        assertEquals(9, (int) Math.floor((cropY - battlerTop) / 8));

        run(scene, animation, 40);                              // updates 1-40: duration was raised to 10
        assertTrue(pokemon.visible, "setVisible(delay+10,false) lands on update 41 (:667/:682)");
        run(scene, animation, 1);                               // update 41
        assertFalse(pokemon.visible);
        assertEquals(Math.round(PokeBattle_SceneConstants.PLAYER_BASE_Y + (cropY - battlerTop)),
                pokemon.y, 1e-3f, ":680 still drops by cropY-battlerTop = 72");
    }

    @Test
    @DisplayName("a battler without sprites builds no processes instead of crashing")
    void missingSprites() {
        FakeScene scene = new FakeScene();
        BattleAnimation animation = new BattleAnimations.BattlerFaintAnimation(scene, 0);
        assertTrue(animation.pictures().isEmpty(), ":656-657 found no sprites");
        animation.update();
        assertTrue(animation.animDone());
    }

    /**
     * Cross-check requested by the Lead: {@code BattlerRecallAnimation} uses the
     * same {@code addSprite(batSprite, PictureEx.Origin.BOTTOM)} call, so its
     * origin is queued the same way and lands on the first update as well. The
     * delayed write is how {@code PictureEx} works (PictureEx:655-657), not a
     * defect in either animation class.
     */
    @Test
    @DisplayName("BattlerRecallAnimation's origin lands on the first update too (:582)")
    void recallOriginLandsOnFirstUpdate() {
        FakeScene scene = new FakeScene();
        BattleSprite pokemon = add(scene, "pokemon_0", BattleSprite.Kind.POKEMON,
                PokeBattle_SceneConstants.PLAYER_BASE_X, PokeBattle_SceneConstants.PLAYER_BASE_Y);
        pokemon.battler = battle.player();
        pokemon.bitmapHeight = 160;
        BattleSprite shadow = add(scene, "shadow_0", BattleSprite.Kind.SHADOW,
                PokeBattle_SceneConstants.PLAYER_BASE_X, PokeBattle_SceneConstants.PLAYER_BASE_Y - 8);
        shadow.visible = false;
        BattleAnimation animation = new BattleAnimations.BattlerRecallAnimation(scene, 0);

        assertEquals(pokemon, animation.pictures().get(0).sprite(), ":582");
        assertEquals(PictureEx.Origin.TOP_LEFT, animation.pictures().get(0).origin(),
                "the ORIGIN process has not run yet");
        run(scene, animation, 1);
        assertEquals(PictureEx.Origin.BOTTOM, animation.pictures().get(0).origin(), ":582");
        assertEquals(PictureEx.Origin.BOTTOM, pokemon.origin);
    }
}
