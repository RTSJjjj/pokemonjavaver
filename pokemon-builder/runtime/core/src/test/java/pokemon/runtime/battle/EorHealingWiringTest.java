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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage 4 / &sect;4 B4 (EOR): the end-of-round healing stage actually runs.
 *
 * <p>{@code pbEndOfRoundPhase:287-300} heals through
 * {@code BattleHandlers.triggerEORHealingAbility/Item} - Leftovers, Black Sludge,
 * Healer, Hydration, Shed Skin. Those two tables had no call site at all, so
 * Leftovers never healed. These tests drive a real {@code battle.step()} with a foe
 * that only uses a self-targeting status move, so the only HP change in the round is
 * the healing stage.</p>
 */
class EorHealingWiringTest {

    /** HERO (bulky, TACKLE) vs FOE (bulky, SWORDSDANCE - deals no damage). */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":2,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SWORDSDANCE\"}]}}}");
        write(root, "moves.json", "{\"total\":2,\"moves\":{"
                + "\"TACKLE\":{\"id\":1,\"internalName\":\"TACKLE\",\"name\":\"Tackle\","
                + "\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"flags\":\"a\",\"target\":\"NearOther\"},"
                + "\"SWORDSDANCE\":{\"id\":2,\"internalName\":\"SWORDSDANCE\",\"name\":\"Swords Dance\","
                + "\"function\":\"02E\",\"power\":0,\"type\":\"NORMAL\",\"category\":\"Status\","
                + "\"accuracy\":0,\"pp\":20,\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(root, "types.json", "{\"total\":1,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":1,\"items\":{"
                + "\"LEFTOVERS\":{\"id\":1,\"internalName\":\"LEFTOVERS\",\"name\":\"Leftovers\"}}}");
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

    private static Battle battle(PbsData data, Random random) {
        return new Battle(data, random, (user, target, moves) -> 0)   // player always TACKLE
                .addPlayer(new Pokemon(data.species("HERO"), 20, data))
                .addFoe(new Pokemon(data.species("FOE"), 20, data));
    }

    @Test
    @DisplayName("B4: Leftovers heals 1/16 max HP at the end of the round (Battle_Phase_EndOfRound:298)")
    void leftoversHealsAtEndOfRound(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, new Random(5));
        battle.player().item = "LEFTOVERS";
        int maxHp = battle.player().maxHp();
        battle.player().hp = maxHp - 100;                       // room to heal
        battle.player().syncHp();

        int before = battle.player().hp;
        battle.step();

        assertTrue(battle.player().hp > before,
                "Leftovers did not heal: " + before + " -> " + battle.player().hp);
        assertTrue(battle.player().hp <= maxHp, "healing must not exceed max HP");
    }

    @Test
    @DisplayName("B4: a battler with no healing item does not gain HP at the end of the round")
    void noItemNoHealing(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, new Random(5));
        battle.player().hp = battle.player().maxHp() - 100;
        battle.player().syncHp();

        int before = battle.player().hp;
        battle.step();

        assertTrue(battle.player().hp <= before,
                "no item should mean no end-of-round healing: " + before + " -> " + battle.player().hp);
    }
}
