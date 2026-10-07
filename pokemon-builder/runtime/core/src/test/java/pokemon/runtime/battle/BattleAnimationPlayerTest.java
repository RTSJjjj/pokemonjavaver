package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.data.BattleAnimationData;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.ui.menu.HueShift;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code PBAnimationPlayerX} (PokeBattle_AnimationPlayer:704-877), the data it
 * reads ({@code generated/battle-animations}), the move lookup
 * ({@code Scene_Animations:427-504}) and the engine's animation events
 * ({@code PokeBattle_Battle:793-799}).
 */
class BattleAnimationPlayerTest {

    @TempDir
    Path tempDir;

    private PbsData pbs;
    private Battle battle;

    private static void write(Path file, String json) throws Exception {
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static String move(String name, int id, String type, String category, String target) {
        return "\"" + name + "\":{\"id\":" + id + ",\"internalName\":\"" + name + "\",\"name\":\"" + name
                + "\",\"function\":\"000\",\"power\":40,\"type\":\"" + type + "\",\"category\":\""
                + category + "\",\"accuracy\":100,\"pp\":10,\"target\":\"" + target + "\"}";
    }

    @BeforeEach
    void setUp() throws Exception {
        Path pbsDir = tempDir.resolve("pbs");
        write(pbsDir.resolve("index.json"), "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(pbsDir.resolve("pokemon.json"), "{\"total\":1,\"byId\":{},\"species\":{"
                + "\"BULBASAUR\":{\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"B\","
                + "\"types\":[\"GRASS\"],\"baseStats\":[45,49,49,45,65,65],\"rareness\":45,"
                + "\"weight\":6.9,\"genderRate\":\"Female50Percent\",\"evolutions\":[]}}}");
        write(pbsDir.resolve("moves.json"), "{\"total\":6,\"moves\":{"
                + move("TACKLE", 1, "NORMAL", "Physical", "NearOther") + ","
                + move("EMBER", 2, "FIRE", "Special", "NearOther") + ","
                + move("FIREPUNCH", 3, "FIRE", "Physical", "NearOther") + ","
                + move("SUNNYDAY", 4, "FIRE", "Status", "BothSides") + ","
                + move("SCRATCHY", 5, "FIRE", "Physical", "NearOther") + ","
                + move("TAILWHIP", 6, "NORMAL", "Status", "AllNearFoes") + "}}");
        pbs = PbsData.parse(tempDir.toFile());
        battle = new Battle(pbs, new Random(1), (user, foe, moves) -> 0);
        battle.addPlayer(new Pokemon(pbs.species("BULBASAUR"), 5, pbs));
        battle.addFoe(new Pokemon(pbs.species("BULBASAUR"), 5, pbs));
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    private BattleAnimationData data() throws Exception {
        Path dir = tempDir.resolve("battle-animations");
        // move2anim: moveToAnim[ember]=1 ; oppMoveToAnim[ember]=2 ; last entry (Ruby -1) = 0
        write(dir.resolve("index.json"), "{\"moveToAnim\":[null,null,1,null,null,null,null,0],"
                + "\"oppMoveToAnim\":[null,null,2],\"animations\":["
                + "{\"id\":0,\"name\":\"Common:StatUp\",\"file\":\"anim-0.json\"},"
                + "{\"id\":1,\"name\":\"Move:EMBER\",\"file\":\"anim-1.json\"},"
                + "{\"id\":2,\"name\":\"Move:EMBER opp\",\"file\":\"anim-2.json\"}]}");
        write(dir.resolve("anim-0.json"), "{\"id\":0,\"name\":\"Common:StatUp\",\"graphic\":\"PRAS- Stats.png\","
                + "\"hue\":0,\"position\":2,\"frames\":[[[ -1,128,224,100,100,0,0,255,0,1,2,1 ]]],\"timings\":[]}");
        write(dir.resolve("anim-1.json"), ANIMATION);
        write(dir.resolve("anim-2.json"), "{\"id\":2,\"name\":\"Move:EMBER opp\",\"graphic\":\"\",\"frames\":[],\"timings\":[]}");
        return BattleAnimationData.load(tempDir.toFile());
    }

    /**
     * Two animation frames (four ticks): cel 0 user, cel 1 target, cel 2 a
     * sheet cel (pattern 7 = column 2, row 1) focused on the target at the
     * default focus point with tone, cel 3 a screen cel with blend 1; an SE on
     * frame 0, a background set on frame 1.
     */
    private static final String ANIMATION = "{\"id\":1,\"name\":\"Move:EMBER\",\"graphic\":\"PRAS- Fire.png\","
            + "\"hue\":30,\"position\":3,\"frames\":["
            + "[[-1,128,224,100,100,0,0,255,0,1,2,1],[-2,384,96,100,100,0,0,255,0,1,1,1],"
            + "[7,384,96,200,100,45,1,200,0,1,1,1,1,2,3,4,10,20,30,40],"
            + "[0,100.9,50.5,100,100,0,0,255,1,3,2,1]],"
            + "[[-1,128,224,100,100,0,0,255,0,1,2,1],null,[7,384,96,100,100,0,0,255,0,1,3,1]]],"
            + "\"timings\":[{\"frame\":0,\"type\":0,\"name\":\"PRSFX- Ember.wav\",\"volume\":90,\"pitch\":110,"
            + "\"duration\":5},{\"frame\":1,\"type\":1,\"name\":\"PRAS- Fire BG.png\",\"volume\":80,\"pitch\":100,"
            + "\"bgX\":-32,\"bgY\":8,\"opacity\":128,\"colorRed\":10,\"colorGreen\":20,\"colorBlue\":30,"
            + "\"colorAlpha\":40,\"duration\":5}]}";

    @Test
    @DisplayName("the data keeps nil cels, the colour/tone tail and Ruby's negative array index")
    void dataParsing() throws Exception {
        BattleAnimationData data = data();
        assertTrue(data.available());
        assertEquals(1, data.moveAnim(2, false));
        assertEquals(2, data.moveAnim(2, true));
        assertEquals(-1, data.moveAnim(3, false), "nil entry");
        assertEquals(-1, data.moveAnim(50, false), "past the end is nil");
        assertEquals(0, data.moveAnim(-1, false), "Struggle's id -1 reads the last element like Ruby");
        BattleAnimationData.Animation anim = data.animation(1);
        assertEquals("PRAS- Fire.png", anim.graphic);
        assertEquals(30, anim.hue);
        assertEquals(2, anim.length());
        assertNull(anim.frames[1][1], "a nil cel stays nil");
        BattleAnimationData.Cel cel = anim.frames[0][2];
        assertEquals(7, cel.pattern);
        assertEquals(200, cel.zoomX);
        assertEquals(1, cel.mirror);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, cel.color);
        assertArrayEquals(new int[] { 10, 20, 30, 40 }, cel.tone);
        assertEquals(100.9f, anim.frames[0][3].x, 1e-3f);
        assertEquals("PRSFX- Ember.wav", anim.timings[0].name);
        assertNull(anim.timings[0].bgX, "nil bgX");
        assertEquals(Integer.valueOf(-32), anim.timings[1].bgX);
        assertSame(data.animation(0), data.common("StatUp"));
        assertNull(data.common("Nope"));
    }

    // ------------------------------------------------------------------
    // Move lookup (Scene_Animations:427-504)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("pbFindMoveAnimDetails: the opposing side tries OppMove first and then Move")
    void findDetails() throws Exception {
        BattleAnimationData data = data();
        assertArrayEquals(new int[] { 1, 0 }, MoveAnimationFinder.findMoveAnimDetails(data, 2, 0, 0));
        assertArrayEquals(new int[] { 3, 0 }, MoveAnimationFinder.findMoveAnimDetails(data, 2, 0, 2), "+hitNum");
        assertArrayEquals(new int[] { 2, 1 }, MoveAnimationFinder.findMoveAnimDetails(data, 2, 1, 0), "OppMove found: noFlip");
        assertNull(MoveAnimationFinder.findMoveAnimDetails(data, 3, 1, 0));
    }

    @Test
    @DisplayName("pbFindMoveAnimation falls back to the type's default move, then Tackle")
    void findFallbacks() throws Exception {
        // A data set where only EMBER (id 2) and TACKLE (id 1) have animations.
        Path dir = tempDir.resolve("battle-animations");
        write(dir.resolve("index.json"), "{\"moveToAnim\":[null,7,8],\"oppMoveToAnim\":[],\"animations\":[]}");
        BattleAnimationData data = BattleAnimationData.load(tempDir.toFile());
        // SCRATCHY: FIRE physical single target -> :FIREPUNCH (id 3, no anim) -> kind<3 so
        // falls to anims[2]=:SUNNYDAY (no anim) -> Tackle (id 1 -> 7).
        assertArrayEquals(new int[] { 7, 0 }, MoveAnimationFinder.findMoveAnimation(data, pbs, 5, 0, 0));
        // FIREPUNCH itself is also FIRE physical: same path.
        assertArrayEquals(new int[] { 7, 0 }, MoveAnimationFinder.findMoveAnimation(data, pbs, 3, 0, 0));
        // TAILWHIP: NORMAL status AllNearFoes -> moveKind 2+3 = 5 -> :TAILWHIP is itself (id 6, none)
        // then moveKind-3=2 -> :DEFENSECURL does not exist -> Tackle.
        assertArrayEquals(new int[] { 7, 0 }, MoveAnimationFinder.findMoveAnimation(data, pbs, 6, 0, 0));
        // SUNNYDAY: FIRE status BothSides -> 2+3 -> anims[5]=:WILLOWISP missing -> anims[2]=:SUNNYDAY (id 4, none) -> Tackle
        assertArrayEquals(new int[] { 7, 0 }, MoveAnimationFinder.findMoveAnimation(data, pbs, 4, 0, 0));
        // An unknown move id has no PBS data: the Ruby rescue returns nil.
        assertNull(MoveAnimationFinder.findMoveAnimation(data, pbs, 99, 0, 0));
        // A move with its own animation wins over every default.
        assertArrayEquals(new int[] { 8, 0 }, MoveAnimationFinder.findMoveAnimation(data, pbs, 2, 0, 0));
    }

    // ------------------------------------------------------------------
    // Player
    // ------------------------------------------------------------------

    private final BattleSprites sprites = new BattleSprites();
    private final List<String> sounds = new ArrayList<>();

    private final BattleAnimationPlayer.Host host = new BattleAnimationPlayer.Host() {
        @Override public BattleSprites sprites() { return sprites; }
        @Override public PictureEx.SePlayer sePlayer() {
            return (name, volume, pitch) -> sounds.add(name + ":" + volume + ":" + pitch);
        }
        @Override public String cryFile(Pokemon pokemon) { return "CRY"; }
        @Override public int[] graphicSize(String path) {
            return path.contains("BG") ? new int[] { 64, 64 } : new int[] { 32, 32 };
        }
        @Override public int[] battlerBitmapSize(BattleSprite sprite) {
            return sprite.kind == BattleSprite.Kind.POKEMON ? new int[] { 96, 96 } : null;
        }
    };

    private BattleSprite pokemonSprite(int index, float x, float y) {
        BattleSprite sprite = sprites.add("pokemon_" + index, BattleSprite.Kind.POKEMON);
        sprite.battler = BattleSendOut.battler(battle, index);
        sprite.back = index % 2 == 0;
        sprite.bitmapWidth = 96;
        sprite.bitmapHeight = 96;
        sprite.origin = PictureEx.Origin.BOTTOM;
        sprite.x = x;
        sprite.y = y;
        sprite.updateOrigin();
        return sprite;
    }

    @Test
    @DisplayName("update draws each frame's cels, focus 1 follows the target, the SE and the background fire on their frames")
    void playerFrames() throws Exception {
        BattleAnimationData.Animation anim = data().animation(1);
        BattleSprite userSprite = pokemonSprite(0, 128, 224 + 48);   // centre = (128, 224)
        BattleSprite targetSprite = pokemonSprite(1, 300, 200);       // centre = (300, 152)
        Battler user = BattleSendOut.battler(battle, 0);
        Battler target = BattleSendOut.battler(battle, 1);
        BattleAnimationPlayer player = new BattleAnimationPlayer(anim, user, target, host, false);
        player.setLineTransform(128, 224, 384, 96, 128, 224, 300, 152);
        player.start();

        player.update();                                  // frame 0 -> animFrame 0
        assertEquals(List.of("Anim/PRSFX- Ember.wav:90:110"), sounds, "the SE of frame 0");
        // pbSpriteSetAnimFrame, user cel: pattern -1, src rect = the whole bitmap, focus 2 = user
        assertEquals(0, userSprite.srcX);
        assertEquals(96, userSprite.srcWidth);
        assertEquals(48f, userSprite.ox);
        assertEquals(48f, userSprite.oy);
        assertEquals(128f, userSprite.x, "cel.x + userOrig.x - FOCUSUSER_X = centre x");
        assertEquals(224f, userSprite.y);
        assertNull(userSprite.sheetName);
        // target cel
        assertEquals(300f, targetSprite.x);
        assertEquals(152f, targetSprite.y);
        // cel 2: a sheet cel, pattern 7 = column 2 row 1, 192x192, zoom 200%/100%, tone/colour copied
        BattleSprite cel2 = player.celSprite(2);
        assertTrue(cel2.visible);
        assertEquals("Graphics/Animations/PRAS- Fire.png", cel2.sheetName);
        assertEquals(30, cel2.sheetHue);
        assertEquals(2 * 192, cel2.srcX);
        assertEquals(192, cel2.srcY);
        assertEquals(96f, cel2.ox);
        assertEquals(2f, cel2.zoomX);
        assertEquals(1f, cel2.zoomY);
        assertEquals(45f, cel2.angle);
        assertTrue(cel2.mirror);
        assertEquals(200f, cel2.opacity);
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, cel2.color);
        assertArrayEquals(new float[] { 10, 20, 30, 40 }, cel2.tone);
        // focus 1: x = 384 + targetOrig.x(300) - 384
        assertEquals(300f, cel2.x);
        assertEquals(152f, cel2.y);
        assertEquals(80, cel2.z, "priority 1 = in front of everything");
        // cel 3: a Float position truncates (RGSS Integer x/y), blend 1, priority 3 focused on the user
        BattleSprite cel3 = player.celSprite(3);
        assertEquals(1, cel3.blendType);
        assertEquals(100f, cel3.x);
        assertEquals(50f, cel3.y);
        assertEquals(userSprite.z + 1, cel3.z, "priority 3 focus 2: just in front of the user");

        player.update();                                  // frame 1: odd tick, nothing redrawn
        assertTrue(cel2.visible);
        player.update();                                  // frame 2 -> animFrame 1: cel 1 is nil, cel 2 redrawn
        assertFalse(targetSprite.visible, "every cel is hidden first and a nil cel stays hidden (:828-834)");
        // the background timing (frame 1): the plane waits for the next update to get its bitmap
        BattleSprite bg = player.bgGraphicSprite();
        assertFalse(bg.visible, "AnimatedPlane gets @__bitmap in the next update (Planes:201-207)");
        assertEquals(128f, bg.opacity);
        assertArrayEquals(new float[] { 10, 20, 30, 40 }, bg.color);
        assertEquals(0f, player.bgColorSprite().opacity);
        player.update();                                  // frame 3
        assertTrue(bg.visible);
        assertEquals("Graphics/Animations/PRAS- Fire BG.png", bg.name);
        assertEquals(32f, bg.ox, "ox = -bgX = 32 within the 64 px bitmap");
        assertEquals(56f, bg.oy, "oy = -bgY = -8, wrapped into the bitmap by LargePlane#refresh (:117)");
        assertFalse(player.animDone());
        player.update();                                  // frame 4 -> animFrame 2 >= length
        assertTrue(player.animDone());

        int before = sprites.size();
        player.dispose();
        assertEquals(before - 58 - 4, sprites.size(), "58 cel sprites and the four planes go");
    }

    @Test
    @DisplayName("transformPoint maps a point of the source line onto the destination line (:37-59)")
    void transformPoint() {
        // (-160,80)-(160,-80) onto (128,224)-(384,96): the middle of the source line is the middle of the destination
        double[] mid = BattleAnimationPlayer.transformPoint(-160, 80, 160, -80, 128, 224, 384, 96, 0, 0);
        assertEquals(256.0, mid[0], 1e-9);
        assertEquals(160.0, mid[1], 1e-9);
        // a degenerate source axis keeps that coordinate at 0 -> the start of the destination line
        double[] flat = BattleAnimationPlayer.transformPoint(0, 0, 0, 0, 10, 20, 30, 40, 5, 5);
        assertEquals(10.0, flat[0], 1e-9);
        assertEquals(20.0, flat[1], 1e-9);
        assertTrue(BattleAnimationPlayer.isReversed(0, 10, 20, 5));
        assertFalse(BattleAnimationPlayer.isReversed(0, 10, 5, 20));
        assertFalse(BattleAnimationPlayer.isReversed(3, 3, 20, 5));
        assertTrue(BattleAnimationPlayer.isReversed(10, 0, 5, 20));
    }

    @Test
    @DisplayName("an empty SE name plays the user's cry (:508-510); focus 3 mirrors a reversed line")
    void cryAndFocusThree() {
        BattleAnimationData.Animation anim = new BattleAnimationData.Animation();
        anim.graphic = "";
        BattleAnimationData.Cel cel = new BattleAnimationData.Cel();
        cel.pattern = 0;
        cel.focus = 3;
        cel.x = 128;
        cel.y = 224;
        anim.frames = new BattleAnimationData.Cel[][] { { null, null, cel } };
        BattleAnimationData.Timing timing = new BattleAnimationData.Timing();
        timing.frame = 0;
        timing.name = "";
        timing.volume = 70;
        timing.pitch = 95;
        anim.timings = new BattleAnimationData.Timing[] { timing };
        pokemonSprite(0, 128, 272);
        pokemonSprite(1, 384, 144);
        Battler user = BattleSendOut.battler(battle, 0);
        BattleAnimationPlayer player = new BattleAnimationPlayer(anim, user, BattleSendOut.battler(battle, 1), host, false);
        // source line runs left to right, destination right to left: reversed
        player.setLineTransform(128, 224, 384, 96, 400, 100, 100, 300);
        player.start();
        player.update();
        assertEquals(List.of("CRY:70:95"), sounds);
        BattleSprite cel2 = player.celSprite(2);
        assertTrue(cel2.mirror, "isReversed flips a sheet cel (:863-866)");
        assertEquals(400f, cel2.x, "the source point (128,224) is the start of the line");
        assertEquals(100f, cel2.y);
    }

    // ------------------------------------------------------------------
    // Engine events
    // ------------------------------------------------------------------

    @Test
    @DisplayName("pbAnimation / pbCommonAnimation queue an ANIMATION event unless showAnims is off (PokeBattle_Battle:793-799)")
    void battleEvents() {
        Battler player = battle.player();
        Battler foe = battle.foe();
        Array<Battler> targets = Array.with(foe);
        battle.animation(2, player, targets, 1);
        battle.commonAnimation("StatUp", foe);
        assertEquals(2, battle.roundEvents.size);
        Battle.AnimationCall move = battle.roundEvents.get(0).anim;
        assertEquals(Battle.RoundEvent.Kind.ANIMATION, battle.roundEvents.get(0).kind);
        assertFalse(move.common);
        assertEquals(2, move.moveId);
        assertEquals(player.index, move.idxUser);
        assertEquals(foe.index, move.idxTarget);
        assertEquals(1, move.hitNum);
        Battle.AnimationCall common = battle.roundEvents.get(1).anim;
        assertTrue(common.common);
        assertEquals("StatUp", common.name);
        assertEquals(-1, common.idxTarget, "targets=nil");
        battle.roundEvents.clear();
        battle.showAnims = false;
        battle.animation(2, player, targets, 0);
        battle.commonAnimation("StatUp", foe);
        assertEquals(0, battle.roundEvents.size);
    }

    @Test
    @DisplayName("HueShift rotates the hue and leaves grey and transparent pixels alone")
    void hueShift() {
        int red = 0xFF0000FF;
        assertEquals(0x00FF00FF, HueShift.shift(red, 120));
        assertEquals(0x0000FFFF, HueShift.shift(red, 240));
        assertEquals(red, HueShift.shift(red, 360));
        assertEquals(0x808080FF, HueShift.shift(0x808080FF, 90));
        assertEquals(0xFF000000, HueShift.shift(0xFF000000, 90));
        assertEquals(0xFF000080, HueShift.shift(0xFF000080, 0), "alpha survives");
    }
}
