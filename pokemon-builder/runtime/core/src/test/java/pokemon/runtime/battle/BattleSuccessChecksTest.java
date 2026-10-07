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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Stage 4 / &sect;4 Q1: the powder immunity actually runs.
 *
 * <p>The plugin blocks a powder move against a Grass-type (and against Overcoat /
 * Safety Goggles) at {@code Battler_UseMove_SuccessChecks:567-587}. This runtime had
 * transcribed {@link Battler#affectedByPowder(boolean)} - with all three checks - but
 * nothing called it, so Stun Spore paralysed Grass-types. These tests drive a real
 * {@code battle.step()} and assert the status, so the call site cannot silently
 * disappear again.</p>
 */
class BattleSuccessChecksTest {

    /** HERO (NORMAL, the attacker) vs GRASSMON (GRASS) / BUGMON (BUG); STUNSPORE for all. */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":3,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"STUNSPORE\"}]},"
                + "\"GRASSMON\":{\"id\":2,\"internalName\":\"GRASSMON\",\"name\":\"Grassmon\","
                + "\"types\":[\"GRASS\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]},"
                + "\"BUGMON\":{\"id\":3,\"internalName\":\"BUGMON\",\"name\":\"Bugmon\","
                + "\"types\":[\"BUG\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        // Stun Spore: function 007, Status, and the /l/ powder flag (flags "bcel" in the
        // project's own moves.json - PokeBattle_Move.rb:124 maps /l/ to powderMove?).
        write(root, "moves.json", "{\"total\":2,\"moves\":{"
                + "\"STUNSPORE\":{\"id\":1,\"internalName\":\"STUNSPORE\",\"name\":\"Stun Spore\","
                + "\"function\":\"007\",\"power\":0,\"type\":\"GRASS\",\"category\":\"Status\","
                + "\"accuracy\":100,\"pp\":30,\"flags\":\"bcel\",\"target\":\"NearOther\"},"
                + "\"TACKLE\":{\"id\":2,\"internalName\":\"TACKLE\",\"name\":\"Tackle\","
                + "\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"flags\":\"a\",\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":1,\"abilities\":{"
                + "\"OVERCOAT\":{\"id\":1,\"internalName\":\"OVERCOAT\",\"name\":\"Overcoat\"}}}");
        write(root, "types.json", "{\"total\":3,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"GRASS\":{\"id\":4,\"internalName\":\"GRASS\",\"name\":\"Grass\"},"
                + "\"BUG\":{\"id\":6,\"internalName\":\"BUG\",\"name\":\"Bug\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":1,\"items\":{"
                + "\"SAFETYGOGGLES\":{\"id\":1,\"internalName\":\"SAFETYGOGGLES\",\"name\":\"Safety Goggles\"}}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }

    private static void write(Path root, String name, String content) throws Exception {
        Path pbs = root.resolve("pbs");
        Files.createDirectories(pbs);
        Files.write(pbs.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    private static Battle battle(PbsData data, String foe, Random random) {
        return new Battle(data, random, (user, target, moves) -> 0)   // always slot 0 (STUNSPORE)
                .addPlayer(new Pokemon(data.species("HERO"), 20, data))
                .addFoe(new Pokemon(data.species(foe), 20, data));
    }

    @Test
    @DisplayName("Q1: Stun Spore does not paralyse a Grass-type (Battler_UseMove_SuccessChecks:569-573)")
    void powderDoesNotAffectGrassTypes(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "GRASSMON", new Random(3));

        battle.step();

        assertEquals("", battle.foe().status,
                "a Grass-type must not be paralysed by a powder move, got " + battle.foe().status);
    }

    @Test
    @DisplayName("Q1: Stun Spore still paralyses a non-Grass target (the rule is not over-applied)")
    void powderStillAffectsOtherTypes(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "BUGMON", new Random(3));

        battle.step();

        assertNotEquals("", battle.foe().status,
                "Stun Spore should paralyse a Bug-type (paralysis only spares ELECTRIC)");
    }

    @Test
    @DisplayName("Q1: Safety Goggles block a powder move (Battler_UseMove_SuccessChecks:584-587)")
    void safetyGogglesBlockPowder(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "BUGMON", new Random(3));
        battle.foe().item = "SAFETYGOGGLES";

        battle.step();

        assertEquals("", battle.foe().status,
                "Safety Goggles must block powder moves, got " + battle.foe().status);
    }
}
