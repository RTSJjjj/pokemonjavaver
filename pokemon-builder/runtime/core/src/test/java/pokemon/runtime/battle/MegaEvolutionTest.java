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

/**
 * Stage 3 / P2: Mega Evolution (a form change) and the ZA mode super-energy
 * system (Mega evolution + ZA模式).
 */
class MegaEvolutionTest {

    @Test
    @DisplayName("P2: a held Mega Stone unlocks the Mega form and its stats")
    void megaForm(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon mon = new Pokemon(data.species("MEGAMON"), 50, data);
        assertFalse(mon.hasMegaForm(data), "no stone yet");
        mon.item = "MEGASTONE";
        assertTrue(mon.hasMegaForm(data));
        assertEquals(1, mon.megaFormIndex(data));
        int before = mon.attack();
        mon.makeMega(data);
        assertTrue(mon.isMega());
        assertNotNull(mon.form);
        assertEquals("超级变形兽", mon.megaName());
        assertTrue(mon.attack() > before, "the Mega form's Attack is higher");
        mon.makeUnmega(data);
        assertFalse(mon.isMega());
        assertEquals(before, mon.attack());
    }

    @Test
    @DisplayName("P2: ZA mode turns a Mega on for three rounds then reverts")
    void zaMode(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("MEGAMON"), 50, data);
        hero.item = "MEGASTONE";
        Pokemon foe = new Pokemon(data.species("TANK"), 100, data);
        Battle battle = new Battle(data, new Random(4), (user, target, moves) -> 0)
                .addPlayer(hero).addFoe(foe);
        battle.zaMode = true;
        assertTrue(battle.canMegaEvolve(battle.player()));
        assertTrue(battle.megaEvolve(battle.player()));
        assertTrue(battle.player().pokemon.isMega());

        // Three end-of-round ticks deplete the energy, then the form reverts.
        battle.step();
        battle.step();
        battle.step();
        assertFalse(battle.player().pokemon.isMega(), "energy ran out");
    }

    @Test
    @DisplayName("P2: a Mega never survives the end of the battle")
    void megaRevertsAfterBattle(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("MEGAMON"), 50, data);
        hero.item = "MEGASTONE";
        Pokemon foe = new Pokemon(data.species("WEAK"), 2, data);
        Battle battle = new Battle(data, new Random(5), (user, target, moves) -> 0)
                .addPlayer(hero).addFoe(foe);
        assertTrue(battle.megaEvolve(battle.player()));
        battle.run(20);
        assertFalse(hero.isMega(), "reverted when the battle ended");
    }

    @Test
    @DisplayName("P2: classic mode keeps the Mega for the whole battle (one per side)")
    void classicMode(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("MEGAMON"), 50, data);
        hero.item = "MEGASTONE";
        Pokemon foe = new Pokemon(data.species("TANK"), 100, data);
        Battle battle = new Battle(data, new Random(6), (user, target, moves) -> 0)
                .addPlayer(hero).addFoe(foe);
        // zaMode stays false (battle_rule == 0): the classic rule.
        assertTrue(battle.canMegaEvolve(battle.player()));
        assertTrue(battle.megaEvolve(battle.player()));
        assertTrue(battle.player().pokemon.isMega());
        assertFalse(battle.canMegaEvolve(battle.player()), "already Mega Evolved");
        battle.step();
        battle.step();
        battle.step();
        assertTrue(battle.player().pokemon.isMega(), "the classic Mega does not run out");
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":2,\"species\":{"
                + "\"MEGAMON\":{\"id\":1,\"internalName\":\"MEGAMON\",\"name\":\"变形兽\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[70,70,70,70,70,70],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[\"OVERGROW\"],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"WEAK\":{\"id\":2,\"internalName\":\"WEAK\",\"name\":\"Weak\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[1,1,1,1,1,1],"
                + "\"growthRate\":\"Medium\",\"baseExp\":1,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"TANK\":{\"id\":3,\"internalName\":\"TANK\",\"name\":\"Tank\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[255,5,255,5,5,5],"
                + "\"growthRate\":\"Slow\",\"baseExp\":1,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]}}}");
        write(root, "moves.json", "{\"total\":1,\"moves\":{"
                + "\"SLASH\":{\"id\":1,\"internalName\":\"SLASH\",\"name\":\"Slash\","
                + "\"function\":\"000\",\"power\":70,\"type\":\"NORMAL\",\"category\":\"Physical\",\"accuracy\":100,\"pp\":20,\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":1,\"abilities\":{"
                + "\"OVERGROW\":{\"id\":65,\"internalName\":\"OVERGROW\",\"name\":\"Overgrow\"}}}");
        write(root, "types.json", "{\"total\":1,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":1,\"items\":{"
                + "\"MEGASTONE\":{\"id\":1,\"internalName\":\"MEGASTONE\",\"name\":\"Mega Stone\"}}}");
        write(root, "pokemonforms.json", "{\"total\":1,\"forms\":{"
                + "\"MEGAMON_1\":{\"species\":\"MEGAMON\",\"form\":1,\"key\":\"MEGAMON_1\","
                + "\"formName\":\"超级变形兽\",\"baseStats\":[70,120,90,120,90,100],\"abilities\":[\"OVERGROW\"],"
                + "\"megaStone\":\"MEGASTONE\",\"unmegaForm\":0}}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }
}
