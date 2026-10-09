package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 111_Battler_ChangeSelf:418-446 {@code pbTransform}, Move_Effects_000-07F:2362-2388 (Transform) and the picture the sprite takes. */
class BattlerTransformTest {

    @TempDir
    Path tempDir;
    private PbsData pbs;
    private Battle battle;
    private Battler user;      // the Pokemon that transforms: Bulbasaur, knows Tackle
    private Battler target;    // Charmander, knows Ember and Scratch

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static String move(String id, int number, String type, int pp) {
        return "\"" + id + "\":{\"id\":" + number + ",\"internalName\":\"" + id + "\",\"name\":\"" + id + "\",\"function\":\""
                + ("TRANSFORM".equals(id) ? "069" : "000") + "\",\"power\":" + ("TRANSFORM".equals(id) ? 0 : 40) + ",\"type\":\"" + type
                + "\",\"category\":\"" + ("TRANSFORM".equals(id) ? "Status" : "Physical") + "\",\"accuracy\":100,\"pp\":" + pp
                + ",\"effectChance\":0,\"target\":\"NearOther\",\"priority\":0,\"flags\":\"\"}";
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(tempDir, "pokemon.json", "{\"total\":2,\"byId\":{},\"species\":{"
                + "\"BULBASAUR\":{\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"妙蛙种子\",\"types\":[\"GRASS\"],"
                + "\"baseStats\":[45,49,49,45,65,65],\"rareness\":45,\"genderRate\":\"Female50Percent\","
                + "\"abilities\":[\"OVERGROW\"],\"evolutions\":[]},"
                + "\"CHARMANDER\":{\"id\":4,\"internalName\":\"CHARMANDER\",\"name\":\"小火龙\",\"types\":[\"FIRE\"],"
                + "\"baseStats\":[39,52,43,65,60,50],\"rareness\":45,\"genderRate\":\"Female50Percent\","
                + "\"abilities\":[\"BLAZE\"],\"evolutions\":[]}}}");
        write(tempDir, "moves.json", "{\"total\":4,\"moves\":{" + move("TACKLE", 33, "NORMAL", 35) + ","
                + move("EMBER", 52, "FIRE", 25) + "," + move("SCRATCH", 10, "NORMAL", 35) + "," + move("TRANSFORM", 144, "NORMAL", 10) + "}}");
        pbs = PbsData.parse(tempDir.toFile());
        battle = new Battle(pbs, new Random(7), (u, f, moves) -> 0);
        Pokemon mine = new Pokemon(pbs.species("BULBASAUR"), 50, pbs);
        mine.moves.clear();
        mine.moves.add(new Pokemon.MoveSlot(pbs.move("TACKLE")));
        Pokemon theirs = new Pokemon(pbs.species("CHARMANDER"), 50, pbs);
        theirs.moves.clear();
        theirs.moves.add(new Pokemon.MoveSlot(pbs.move("EMBER")));
        theirs.moves.add(new Pokemon.MoveSlot(pbs.move("SCRATCH")));
        battle.addPlayer(mine);
        battle.addFoe(theirs);
        user = battle.battlerAt(0);
        target = battle.battlerAt(1);
        target.ability = "BLAZE";
        target.setStage(PBStats.ATTACK, 2);
        target.setStage(PBStats.EVASION, -1);
    }

    @Test
    @DisplayName("the user takes the target's types, ability, stats, stages and moves (5 PP each) and its sprite picture")
    void becomesTheTarget() {
        Pokemon before = user.visiblePokemon();
        assertSame(user.pokemon, before);
        user.pbTransform(target);
        assertTrue(user.effects.truthy(PBEffects.Battler.Transform));
        assertEquals("CHARMANDER", user.effects.stringVal(PBEffects.Battler.TransformSpecies));
        assertEquals("FIRE", user.pbTypes().get(0));
        assertEquals("BLAZE", user.ability);
        assertEquals(target.baseAttack(), user.baseAttack());
        assertEquals(target.baseSpeed(), user.baseSpeed());
        assertEquals(2, user.stage(PBStats.ATTACK));
        assertEquals(-1, user.stage(PBStats.EVASION));
        assertEquals("EMBER", user.moveSlot(0).internalName());
        assertEquals("SCRATCH", user.moveSlot(1).internalName());
        assertNull(user.moveSlot(2));
        assertEquals(5, user.moveSlotPp(0));
        assertEquals(5, user.moveSlotMaxPp(1));
        assertEquals("CHARMANDER", user.visiblePokemon().species.internalName);
        assertEquals("BULBASAUR", user.pokemon.species.internalName);       // the Pokemon itself is still Bulbasaur
    }

    @Test
    @DisplayName("leaving the field or the battle puts the real moves, stats and picture back")
    void restores() {
        int attack = user.baseAttack();
        user.pbTransform(target);
        user.restoreMimickedMoves();
        user.clearStatOverrides();
        assertEquals("TACKLE", user.moveSlot(0).internalName());
        assertEquals(35, user.moveSlotPp(0));
        assertNull(user.moveSlot(1));
        assertSame(user.pokemon, user.visiblePokemon());
        assertEquals(attack, user.baseAttack());
    }

    @Test
    @DisplayName("Transform fails when the user is already transformed or when the target is (and so does Imposter's target check)")
    void failures() {
        MoveEffect effect = MoveEffectRegistry.of("069");
        BattleMove transform = new BattleMove(pbs.move("TRANSFORM"));
        Array<Battler> targets = new Array<>();
        targets.add(target);
        assertFalse(effect.pbMoveFailed(transform, user, targets));
        assertFalse(effect.pbFailsAgainstTarget(transform, user, target));
        user.pbTransform(target);
        assertTrue(effect.pbMoveFailed(transform, user, targets));
        assertTrue(effect.pbFailsAgainstTarget(transform, target, user));
    }

    @Test
    @DisplayName("the sprite change is queued for the scene with the picture it showed before")
    void queuesTheSpriteEvent() {
        int before = battle.roundEvents.size;
        user.queueTransformSprite();
        assertEquals(before + 1, battle.roundEvents.size);
        Battle.RoundEvent event = battle.roundEvents.peek();
        assertEquals(Battle.RoundEvent.Kind.TRANSFORM_SPRITE, event.kind);
        assertEquals(0, event.idxBattler);
        assertSame(user.pokemon, event.oldLook);
    }
}
