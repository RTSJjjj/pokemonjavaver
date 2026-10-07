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
 * Battle_Phase_EndOfRound: weather, the damage/healing stages, the countdowns and
 * the terrain wrappers of the 场地 section.
 */
class BattleEndOfRoundPhaseTest {

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


    /** A battle whose round order has been calculated (the attack phase does this before the end of round). */
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
    @DisplayName("pbEORWeather: Sandstorm chips 1/16 of a non-immune battler and counts down (:35-110)")
    void sandstorm(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.field.weather = PBWeather.Sandstorm;
        b.field.weatherDuration = 3;
        int hp = b.player().hp;
        b.pbEndOfRoundPhase();
        assertEquals(hp - b.player().maxHp() / 16, b.player().hp);
        assertTrue(said(b, "沙暴正在肆虐！"));
        assertTrue(said(b, "被沙暴伤害了！"));
        assertEquals(2, b.field.weatherDuration);
    }

    @Test
    @DisplayName("pbEORWeather: the weather wears off at 0 with its message (:41-56)")
    void weatherEnds(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.field.weather = PBWeather.Rain;
        b.field.weatherDuration = 1;
        b.pbEndOfRoundPhase();
        assertEquals(PBWeather.None, b.field.weather);
        assertTrue(said(b, "雨停了！"));
    }

    @Test
    @DisplayName("Leech Seed: the seeded battler loses 1/8 and the recipient recovers (:328-344)")
    void leechSeed(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.foe().effects.set(PBEffects.Battler.LeechSeed, 0);        // recipient: battler 0
        b.player().pbReduceHP(40);
        int foeHp = b.foe().hp;
        int playerHp = b.player().hp;
        b.pbEndOfRoundPhase();
        assertEquals(foeHp - b.foe().maxHp() / 8, b.foe().hp);
        assertTrue(b.player().hp > playerHp, "the recipient recovered");
        assertTrue(said(b, "被寄生种子吸取了营养！"));
    }

    @Test
    @DisplayName("Side effects count down and say so (:649-683)")
    void reflectEnds(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.field.sides[0].effects.set(PBEffects.Side.Reflect, 1);
        b.pbEndOfRoundPhase();
        assertEquals(0, b.field.sides[0].effects.intVal(PBEffects.Side.Reflect));
        assertTrue(said(b, "的反射盾消失了！"));
    }

    @Test
    @DisplayName("Taunt counts down and ends with its message (:529-531)")
    void tauntEnds(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.player().effects.set(PBEffects.Battler.Taunt, 1);
        b.pbEndOfRoundPhase();
        assertTrue(said(b, "的挑衅无效了！"));
    }

    @Test
    @DisplayName("pbStartTerrain: Grassy Terrain lasts 5 rounds, heals 1/16 and then wears off (:741-769, :115-146, :290-295)")
    void grassyTerrain(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.pbStartTerrain(b.player(), PBBattleTerrains.Grassy);
        assertEquals(5, b.field.terrainDuration);
        assertTrue(said(b, "青草覆盖了四周！"));
        b.player().pbReduceHP(40);
        int hp = b.player().hp;
        b.pbEndOfRoundPhase();
        assertEquals(hp + b.player().maxHp() / 16, b.player().hp);
        assertEquals(4, b.field.terrainDuration);
        b.field.terrainDuration = 1;
        b.pbEndOfRoundPhase();
        assertEquals(PBBattleTerrains.None, b.field.terrain);
        assertTrue(said(b, "四周的青草枯萎了！"));
    }

    @Test
    @DisplayName("Cold terrain (场地:261-306) hurts grounded non-Ice battlers by 1/16")
    void coldTerrain(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.pbStartTerrain(b.player(), PBBattleTerrains.Cold);
        assertTrue(said(b, "刺骨的寒气笼罩了场地！"));
        int hp = b.player().hp;
        b.pbEndOfRoundPhase();
        assertEquals(hp - b.player().maxHp() / 16, b.player().hp);
        assertTrue(said(b, "刺骨的寒气仍笼罩着场地！"));
    }

    @Test
    @DisplayName("The per-round effect resets run at the end (:751-798)")
    void resets(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle b = battle(data);
        b.player().effects.set(PBEffects.Battler.Protect, true);
        b.player().effects.set(PBEffects.Battler.Flinch, true);
        b.field.sides[0].effects.set(PBEffects.Side.QuickGuard, true);
        b.pbEndOfRoundPhase();
        assertFalse(b.player().effects.truthy(PBEffects.Battler.Protect));
        assertFalse(b.player().effects.truthy(PBEffects.Battler.Flinch));
        assertFalse(b.field.sides[0].effects.truthy(PBEffects.Side.QuickGuard));
    }
}
