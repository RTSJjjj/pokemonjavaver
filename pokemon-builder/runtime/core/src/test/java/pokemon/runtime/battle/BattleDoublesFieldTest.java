package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The slot table of a double battle: PokeBattle_Battle:112-125, :211-243, :281-302, :496-624 and Battle_StartAndEnd:116-189. */
class BattleDoublesFieldTest {

    private static Pokemon pokemon(PbsData data, String species) {
        return new Pokemon(data.species(species), 20, data);
    }

    private static Battle doubles(PbsData data) {
        Battle b = new Battle(data, new Random(1), null);
        b.trainerBattle = true;
        b.setSideSizes(2, 2);
        for (int i = 0; i < 3; i++) b.addPlayer(pokemon(data, "HERO"));
        for (int i = 0; i < 3; i++) b.addFoe(pokemon(data, "FOE"));
        return b;
    }

    @Test
    @DisplayName("pbSetUpSides puts two Pokemon per side on the even/odd battler indices (:116-189)")
    void setUpSides(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = doubles(data);
        assertEquals(2, b.pbSideSize(0));
        assertEquals(3, b.maxBattlerIndex());
        for (int idx = 0; idx < 4; idx++) {
            assertNotNull(b.battlerAt(idx), "battler " + idx);
            assertEquals(idx, b.battlerAt(idx).index);
        }
        assertEquals(0, b.fieldPartyIndex(0));
        assertEquals(1, b.fieldPartyIndex(2));            // :180 2*battlerNumber+side
        assertEquals(1, b.fieldPartyIndex(3));
        assertEquals(4, b.eachBattler().size);
        assertEquals(2, b.eachSameSideBattler(2).size);
        assertEquals(2, b.eachOtherSideBattler(0).size);
        assertFalse(b.singleBattle());
        assertEquals(2, b.pbSideBattlerCount(0));
    }

    @Test
    @DisplayName("pbGetOpposingIndicesInOrder / pbDirectOpposing / near? in 2v2 (:515-516, :790-793, :842-854)")
    void opposites(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = doubles(data);
        assertArrayEquals(new int[] {3, 1}, b.pbGetOpposingIndicesInOrder(0));
        assertArrayEquals(new int[] {2, 0}, b.pbGetOpposingIndicesInOrder(1));
        assertArrayEquals(new int[] {1, 3}, b.pbGetOpposingIndicesInOrder(2));
        assertSame(b.battlerAt(3), b.battlerAt(0).pbDirectOpposing(false));
        assertSame(b.battlerAt(2), b.battlerAt(1).pbDirectOpposing(false));
        b.battlerAt(3).hp = 0;
        // :845 `break if unfaintedOnly && fainted?` ends the first loop at the fainted first choice, and the second
        // loop (:850-852) then answers the first battler that exists - the fainted one.
        assertSame(b.battlerAt(3), b.battlerAt(0).pbDirectOpposing(true));
        assertTrue(b.battlerAt(0).near(1));
        assertFalse(b.battlerAt(0).near(0));
    }

    @Test
    @DisplayName("a partner trainer owns position 2: pbOwnedByPlayer?, owner names and who may switch in (:232-243, :266-290, :9-23)")
    void partner(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = new Battle(data, new Random(1), null);
        b.trainerBattle = true;
        b.setSideSizes(2, 2);
        b.playerName = "Me";
        b.partnerName = "Friend";
        b.addPlayer(pokemon(data, "HERO")).addPlayer(pokemon(data, "HERO"));
        b.addPartner(pokemon(data, "FOE")).addPartner(pokemon(data, "FOE"));
        b.addFoe(pokemon(data, "FOE")).addFoe(pokemon(data, "FOE"));
        assertEquals(2, b.pbTrainerCount(0));
        assertEquals(0, b.pbGetOwnerIndexFromBattlerIndex(0));
        assertEquals(1, b.pbGetOwnerIndexFromBattlerIndex(2));
        assertTrue(b.pbOwnedByPlayer(0));
        assertFalse(b.pbOwnedByPlayer(2), ":289 the partner's position is not the player's");
        assertFalse(b.pbOwnedByPlayer(1));
        assertEquals("Me", b.pbGetOwnerName(0));
        assertEquals("Friend", b.pbGetOwnerName(2));
        assertSame(b.playerParty().get(0), b.battlerAt(0), "the player's own Pokemon fill position 0");
        assertSame(b.playerParty().get(2), b.battlerAt(2), "the partner's first Pokemon fills position 2");
        assertEquals(1, b.pbNumPositions(0, 1));
        assertArrayEquals(new int[] {2, 4}, b.pbTeamIndexRangeFromBattlerIndex(2));
        assertEquals("不能将宝可梦与Friend的宝可梦替换！", b.canSwitchLax(0, 3));
    }

    @Test
    @DisplayName("pbReplace changes one position and leaves the other alone; pbAbleNonActiveCount counts the side's benched Pokemon (:340-351)")
    void replaceOnePosition(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = doubles(data);
        assertEquals(1, b.pbAbleNonActiveCount(0));
        Battler third = b.playerParty().get(2);
        assertTrue(b.replace(2, 2));
        assertSame(third, b.battlerAt(2));
        assertEquals(2, third.index);
        assertEquals(0, b.battlerAt(0).index);
        assertEquals(1, b.pbAbleNonActiveCount(0));
    }

    @Test
    @DisplayName("pbSwapBattlers swaps the positions of two Pokemon of the same trainer and retargets the effects that point at them (:593-624)")
    void swap(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = doubles(data);
        Battler a = b.battlerAt(0);
        Battler c = b.battlerAt(2);
        b.battlerAt(1).effects.set(PBEffects.Battler.LockOnPos, 0);
        assertTrue(b.pbSwapBattlers(0, 2));
        assertSame(c, b.battlerAt(0));
        assertSame(a, b.battlerAt(2));
        assertEquals(0, c.index);
        assertEquals(2, a.index);
        assertEquals(2, b.battlerAt(1).effects.intVal(PBEffects.Battler.LockOnPos));
        assertFalse(b.pbSwapBattlers(0, 1), ":596 not across the field");
    }

    @Test
    @DisplayName("a wild double battle puts every wild Pokemon on its own position (:120-128)")
    void wildDouble(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = new Battle(data, new Random(1), null);
        b.setSideSizes(2, 2);
        b.addPlayer(pokemon(data, "HERO")).addPlayer(pokemon(data, "HERO"));
        b.addFoe(pokemon(data, "FOE")).addFoe(pokemon(data, "FOE"));
        assertEquals(0, b.pbGetOwnerIndexFromBattlerIndex(3), ":234 a wild side has no trainer");
        assertSame(b.foeParty().get(0), b.battlerAt(1));
        assertSame(b.foeParty().get(1), b.battlerAt(3));
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    /** HERO (strong, NORMAL), FOE (weak, NORMAL), SPOOK (GHOST, immune to NORMAL). */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":3,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[100,100,100,100,100,100],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[1,1,1,1,1,1],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"SPOOK\":{\"id\":3,\"internalName\":\"SPOOK\",\"name\":\"Spook\","
                + "\"types\":[\"GHOST\"],\"baseStats\":[50,50,50,50,50,50],"
                + "\"growthRate\":\"Medium\",\"baseExp\":50,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]}}}");
        write(root, "moves.json", "{\"total\":1,\"moves\":{"
                + "\"SLASH\":{\"id\":1,\"internalName\":\"SLASH\",\"name\":\"Slash\","
                + "\"function\":\"000\",\"power\":70,\"type\":\"NORMAL\",\"category\":\"Physical\",\"accuracy\":100,\"pp\":20,\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(root, "types.json", "{\"total\":2,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"GHOST\":{\"id\":7,\"internalName\":\"GHOST\",\"name\":\"Ghost\",\"immunities\":[\"NORMAL\"]}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }
}
