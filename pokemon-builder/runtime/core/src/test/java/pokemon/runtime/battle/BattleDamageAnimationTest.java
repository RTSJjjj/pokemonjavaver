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
 * B7: {@code BattlerDamageAnimation} ({@code PokeBattle_SceneAnimations:610-641}),
 * the four-flash animation {@code pbHitAndHPLossAnimation} (Scene_Animations:
 * 239-268) and {@code pbDamageAnimation} (:224-234) play.
 *
 * <p>The evidence is the animation's observable timeline: which SE it queues for
 * each effectiveness (:625-629) and when the battler/shadow are hidden and shown
 * again (:630-639). {@code ensureDelay} doubles the animation's 1/20 s delays
 * into 40 fps frames (PictureEx:147), so a process passed {@code delay=d} runs on
 * update {@code 2d+1}.</p>
 */
class BattleDamageAnimationTest {

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
        @Override public int sideSize(int idxBattler) { return 1; }
        @Override public String playerTrainerType() { return "POKEMONTRAINER_Red"; }
        @Override public boolean followerSprite() { return false; }
        @Override public String cryFile(Pokemon pokemon) { return null; }
        @Override public int cryFrameLength(Pokemon pokemon, int pitch) { return 0; }
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

    /** The two sprites every battler has (Scene_Initialize:107-174). */
    private void addSprites(FakeScene scene, boolean visible) {
        BattleSprite pokemon = scene.sprites.add("pokemon_0", BattleSprite.Kind.POKEMON);   // :618
        pokemon.visible = visible;
        BattleSprite shadow = scene.sprites.add("shadow_0", BattleSprite.Kind.SHADOW);      // :619
        shadow.visible = visible;
    }

    private static BattleAnimation animation(FakeScene scene, int effectiveness) {
        return new BattleAnimations.BattlerDamageAnimation(scene, 0, effectiveness);        // :611-614
    }

    /** Runs {@code frames} updates, recording one '0'/'1' per update for both pictures. */
    private static String[] timeline(FakeScene scene, BattleAnimation animation,
            int frames) {
        PictureEx battler = animation.pictures().get(0);        // :621 addSprite(batSprite,...)
        PictureEx shadow = animation.pictures().get(1);         // :622 addSprite(shaSprite,...)
        StringBuilder battlerTrace = new StringBuilder();
        StringBuilder shadowTrace = new StringBuilder();
        for (int i = 0; i < frames; i++) {
            scene.frames++;                                     // pbUpdate
            animation.update();
            battlerTrace.append(battler.visible() ? '1' : '0');
            shadowTrace.append(shadow.visible() ? '1' : '0');
        }
        return new String[] { battlerTrace.toString(), shadowTrace.toString() };
    }

    @Test
    @DisplayName("the SE follows effectiveness: 0 normal, 1 weak, 2 super (:625-629)")
    void seFollowsEffectiveness() {
        String[] expected = { "Battle damage normal", "Battle damage weak", "Battle damage super" };
        for (int effectiveness = 0; effectiveness <= 2; effectiveness++) {
            FakeScene scene = new FakeScene();
            addSprites(scene, true);
            BattleAnimation animation = animation(scene, effectiveness);
            scene.frames++;
            animation.update();                                 // delay 0 runs on update 1
            assertEquals(1, scene.se.size(), "exactly one SE, effectiveness=" + effectiveness);
            assertEquals("1:" + expected[effectiveness] + "/100/100", scene.se.get(0),
                    "effectiveness=" + effectiveness);
        }
    }

    @Test
    @DisplayName("an effectiveness outside 0/1/2 matches no branch and plays no SE (:625-629)")
    void unknownEffectivenessPlaysNoSe() {
        FakeScene scene = new FakeScene();
        addSprites(scene, true);
        BattleAnimation animation = animation(scene, 3);
        timeline(scene, animation, 40);
        assertTrue(scene.se.isEmpty(), "the case statement has no else branch");
    }

    @Test
    @DisplayName("4 flashes of 4 frames each, then the original visibilities are restored (:630-639)")
    void fourFlashesRestoreVisibility() {
        FakeScene scene = new FakeScene();
        addSprites(scene, true);
        String[] trace = timeline(scene, animation(scene, 0), 40);
        // setVisible(0/4/8/12,false) -> updates 1/9/17/25,
        // setVisible(2/6/10/14,true) -> updates 5/13/21/29, and the restore
        // setVisible(16,...) -> update 33 (:638-639).
        String expected = "00001111000011110000111100001111"        // :630-636 x4
                + "11111111";                                        // :638-639
        assertEquals(expected, trace[0], "battler flashes");
        assertEquals(expected, trace[1], "the shadow flashes with it (:632/:634)");
    }

    @Test
    @DisplayName("a hidden sprite is never shown again: the delay+2 restore is guarded (:633-634)")
    void hiddenSpritesStayHidden() {
        FakeScene scene = new FakeScene();
        addSprites(scene, false);
        String[] trace = timeline(scene, animation(scene, 0), 40);
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            expected.append('0');                                // "if batSprite.visible" is false
        }
        assertEquals(expected.toString(), trace[0]);
        assertEquals(expected.toString(), trace[1]);
    }
}
