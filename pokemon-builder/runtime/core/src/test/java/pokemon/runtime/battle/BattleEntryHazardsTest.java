package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import com.badlogic.gdx.utils.Array;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Battle_Action_Switching:363-476: entry hazards and Healing Wish when a Pokemon comes in.
 */
class BattleEntryHazardsTest {

    private static void write(Path root, String name, String content) throws Exception {
        Path pbs = root.resolve("pbs");
        Files.createDirectories(pbs);
        Files.write(pbs.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    /** HERO knows STUNSPORE (30 PP); FOE knows TACKLE. */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":2,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"STUNSPORE\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,200,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        write(root, "moves.json", "{\"total\":2,\"moves\":{"
                + "\"STUNSPORE\":{\"id\":1,\"internalName\":\"STUNSPORE\",\"name\":\"Stun Spore\","
                + "\"function\":\"007\",\"power\":0,\"type\":\"GRASS\",\"category\":\"Status\","
                + "\"accuracy\":100,\"pp\":30,\"flags\":\"bcel\",\"target\":\"NearOther\"},"
                + "\"TACKLE\":{\"id\":2,\"internalName\":\"TACKLE\",\"name\":\"Tackle\","
                + "\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"flags\":\"a\",\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(root, "types.json", "{\"total\":2,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"GRASS\":{\"id\":4,\"internalName\":\"GRASS\",\"name\":\"Grass\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }


    private static Battle battle(PbsData data) {
        Battle b = new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(new Pokemon(data.species("HERO"), 20, data))
                .addFoe(new Pokemon(data.species("FOE"), 20, data));
        b.pbCalculatePriority(true, null);
        return b;
    }

    private static boolean said(Battle b, String part) {
        for (String m : b.roundMessages) if (m.contains(part)) return true;
        return false;
    }

    @Test
    @DisplayName("Stealth Rock hurts the Pokemon that comes in by 1/8 x effectiveness (:404-418)")
    void stealthRock(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.field.sides[0].effects.set(PBEffects.Side.StealthRock, true);
        int hp = b.player().hp;
        assertTrue(b.pbOnActiveOne(b.player()));
        assertTrue(hp - b.player().hp > 0, "took damage");
        assertTrue(said(b, "尖锐的岩石伤害了"));
    }

    @Test
    @DisplayName("Spikes: 1 layer is 1/8 (:422-432)")
    void spikes(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.field.sides[0].effects.set(PBEffects.Side.Spikes, 1);
        int hp = b.player().hp;
        b.pbOnActiveOne(b.player());
        assertEquals(hp - b.player().maxHp() / 8, b.player().hp);
        assertTrue(said(b, "被地菱伤害了！"));
    }

    @Test
    @DisplayName("Toxic Spikes: 2 layers badly poison (:434-446)")
    void toxicSpikes(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.field.sides[0].effects.set(PBEffects.Side.ToxicSpikes, 2);
        b.pbOnActiveOne(b.player());
        assertEquals("POISON", b.player().status);
        assertTrue(b.player().statusCount > 0, "badly poisoned");
    }

    @Test
    @DisplayName("Healing Wish heals and cures the Pokemon that comes in (:363-372)")
    void healingWish(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.player().pbReduceHP(50);
        b.field.positions[0].effects.set(PBEffects.Position.HealingWish, true);
        b.pbOnActiveOne(b.player());
        assertEquals(b.player().maxHp(), b.player().hp);
        assertFalse(b.field.positions[0].effects.truthy(PBEffects.Position.HealingWish));
    }

    @Test
    @DisplayName("pbEffectsOnSwitchIn(true) now runs the entry effects instead of throwing (Battler_AbilityAndItem:5-35)")
    void switchInRuns(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.field.sides[0].effects.set(PBEffects.Side.Spikes, 1);
        b.player().pbEffectsOnSwitchIn(true);
        assertTrue(said(b, "被地菱伤害了！"));
    }
}
