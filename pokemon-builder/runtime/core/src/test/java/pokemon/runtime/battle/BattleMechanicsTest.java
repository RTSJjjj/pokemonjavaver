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
 * Stage 3 / P2: the battle mechanics added on top of the damage core - status
 * conditions, stat stages, priority and the move function-code table.
 */
class BattleMechanicsTest {

    @Test
    @DisplayName("P2: a status move poisons the target and damages it each turn")
    void toxicDamage(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 20, data);
        Pokemon foe = new Pokemon(data.species("FOE"), 20, data);
        // The player always uses TOXIC (function 006, status).
        Battle battle = new Battle(data, new Random(1), (user, target, moves) -> indexOf(moves, "TOXIC"))
                .addPlayer(hero).addFoe(foe);
        battle.step();
        assertEquals("POISON", battle.foe().status, "the foe is badly poisoned");
        int hpAfterToxic = foe.hp;
        battle.step();
        assertTrue(foe.hp < hpAfterToxic, "poison chipped " + hpAfterToxic + " -> " + foe.hp);
    }

    @Test
    @DisplayName("P2: a stat-up move raises the user's stage and its Attack")
    void statStages(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 20, data);
        Pokemon foe = new Pokemon(data.species("FOE"), 20, data);
        Battler battler = new Battler(hero, false);
        int before = battler.attack();
        Battle battle = new Battle(data, new Random(2), (user, target, moves) -> indexOf(moves, "SWORDSDANCE"))
                .addPlayer(hero).addFoe(foe);
        battle.step();
        assertEquals(2, battle.player().stage(PBStats.ATTACK), "Swords Dance is +2 Attack");
        assertTrue(battle.player().attack() > before, "Attack rose");
    }

    @Test
    @DisplayName("P2: a priority move acts before a faster foe")
    void priorityOrder(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        // The player is slower but QUICKATTACK has priority 1, so it strikes
        // first and the faster foe never gets to act.
        Pokemon hero = new Pokemon(data.species("SLOW"), 50, data);
        Pokemon foe = new Pokemon(data.species("HERO"), 20, data);
        Battle battle = new Battle(data, new Random(3),
                (user, target, moves) -> indexOf(moves, "QUICKATTACK"))
                .addPlayer(hero).addFoe(foe);
        battle.step();
        assertEquals(0, foe.hp, "the priority move KO'd the foe before it could act");
        assertEquals(battle.player().maxHp(), battle.player().hp, "the foe never acted");
    }

    private static int indexOf(com.badlogic.gdx.utils.Array<BattleMove> moves, String name) {
        for (int i = 0; i < moves.size; i++) {
            if (name.equals(moves.get(i).internalName())) {
                return i;
            }
        }
        return 0;
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    /** HERO (strong), FOE (weak), SLOW (weak + slow), with the effect moves. */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":3,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[100,120,80,120,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"},{\"level\":1,\"move\":\"TOXIC\"},"
                + "{\"level\":1,\"move\":\"SWORDSDANCE\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[40,40,40,40,40,40],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"SLOW\":{\"id\":3,\"internalName\":\"SLOW\",\"name\":\"Slow\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[40,40,40,40,40,1],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"QUICKATTACK\"}]}}}");
        write(root, "moves.json", "{\"total\":4,\"moves\":{"
                + "\"SLASH\":{\"id\":1,\"internalName\":\"SLASH\",\"name\":\"Slash\","
                + "\"function\":\"000\",\"power\":70,\"type\":\"NORMAL\",\"category\":\"Physical\",\"accuracy\":100,\"pp\":20,\"target\":\"NearOther\"},"
                + "\"TOXIC\":{\"id\":2,\"internalName\":\"TOXIC\",\"name\":\"Toxic\","
                + "\"function\":\"006\",\"power\":0,\"type\":\"POISON\",\"category\":\"Status\",\"accuracy\":0,\"pp\":10,\"target\":\"NearOther\"},"
                + "\"SWORDSDANCE\":{\"id\":3,\"internalName\":\"SWORDSDANCE\",\"name\":\"Swords Dance\","
                + "\"function\":\"02E\",\"power\":0,\"type\":\"NORMAL\",\"category\":\"Status\",\"accuracy\":0,\"pp\":20,\"target\":\"NearOther\"},"
                + "\"QUICKATTACK\":{\"id\":4,\"internalName\":\"QUICKATTACK\",\"name\":\"Quick Attack\","
                + "\"function\":\"000\",\"power\":250,\"type\":\"NORMAL\",\"category\":\"Physical\",\"accuracy\":100,\"pp\":30,\"priority\":1,\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(root, "types.json", "{\"total\":2,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"POISON\":{\"id\":3,\"internalName\":\"POISON\",\"name\":\"Poison\",\"immunities\":[\"STEEL\"]}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }
}
