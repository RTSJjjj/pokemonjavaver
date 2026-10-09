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

/** Abilities that answer a stat drop (Defiant, Competitive) when the drop is a move's additional effect. */
class StatLossAbilityTest {

    private static File pbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":2,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,100,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"ICYWIND\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,20,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SPLASH\"}]}}}");
        write(root, "moves.json", "{\"total\":2,\"moves\":{"
                + "\"ICYWIND\":{\"id\":1,\"internalName\":\"ICYWIND\",\"name\":\"Icy Wind\","
                + "\"function\":\"044\",\"power\":55,\"type\":\"NORMAL\",\"category\":\"Special\","
                + "\"accuracy\":100,\"pp\":15,\"effectChance\":100,\"flags\":\"\",\"target\":\"NearOther\"},"
                + "\"SPLASH\":{\"id\":2,\"internalName\":\"SPLASH\",\"name\":\"Splash\","
                + "\"function\":\"001\",\"power\":0,\"type\":\"NORMAL\",\"category\":\"Status\","
                + "\"accuracy\":0,\"pp\":40,\"flags\":\"\",\"target\":\"User\"}}}");
        write(root, "abilities.json", "{\"total\":1,\"abilities\":{"
                + "\"DEFIANT\":{\"id\":1,\"internalName\":\"DEFIANT\",\"name\":\"Defiant\"}}}");
        write(root, "types.json", "{\"total\":1,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":[{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }

    private static void write(Path root, String name, String content) throws Exception {
        Path dir = root.resolve("pbs");
        Files.createDirectories(dir);
        Files.write(dir.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Defiant answers the Speed drop of Icy Wind's additional effect with +2 Attack")
    void defiantAfterAdditionalEffect(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(pbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 20, data);
        Pokemon foe = new Pokemon(data.species("FOE"), 20, data);
        foe.ability = "DEFIANT";
        Battle battle = new Battle(data, new Random(3), (user, target, moves) -> 0).addPlayer(hero).addFoe(foe);
        battle.step();
        assertEquals(-1, battle.foe().stage(PBStats.SPEED), "the additional effect lowered Speed");
        assertEquals(2, battle.foe().stage(PBStats.ATTACK), "Defiant answered it");
    }

    @Test
    @DisplayName("Defiant on the player's side answers the foe's Icy Wind too")
    void defiantOnThePlayersSide(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(pbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 20, data);
        Pokemon foe = new Pokemon(data.species("HERO"), 20, data);
        hero.ability = "DEFIANT";
        Battle battle = new Battle(data, new Random(3), (user, target, moves) -> 0).addPlayer(hero).addFoe(foe);
        battle.step();
        assertEquals(-1, battle.player().stage(PBStats.SPEED), "the foe's Icy Wind lowered Speed");
        assertEquals(2, battle.player().stage(PBStats.ATTACK), "Defiant answered it");
    }

    @Test
    @DisplayName("Defiant's trigger is announced by the ability bar before the Attack boost")
    void defiantShowsTheAbilityBar(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(pbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 20, data);
        Pokemon foe = new Pokemon(data.species("FOE"), 20, data);
        foe.ability = "DEFIANT";
        Battle battle = new Battle(data, new Random(3), (user, target, moves) -> 0).addPlayer(hero).addFoe(foe);
        battle.step();
        int show = -1;
        int boost = -1;
        for (int i = 0; i < battle.roundEvents.size; i++) {
            Battle.RoundEvent event = battle.roundEvents.get(i);
            if (event.kind == Battle.RoundEvent.Kind.ABILITY_SPLASH_SHOW && event.idxBattler == 1) show = i;
            if (event.kind == Battle.RoundEvent.Kind.MESSAGE && event.text.contains("攻击") && show >= 0) boost = i;
        }
        assertTrue(show >= 0, "the bar appears for the foe's Defiant");
        assertTrue(boost > show, "the Attack boost line follows the bar");
        assertTrue(battle.roundEvents.get(show).text.endsWith("\nDefiant"), battle.roundEvents.get(show).text);
    }

    @Test
    @DisplayName("a status inflicted in battle is written to the Pokemon at once, so it survives a switch out and in")
    void statusReachesThePokemon(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(pbs(tempDir));
        Pokemon hero = new Pokemon(data.species("HERO"), 20, data);
        Pokemon foe = new Pokemon(data.species("FOE"), 20, data);
        Battle battle = new Battle(data, new Random(3), (user, target, moves) -> 0).addPlayer(hero).addFoe(foe);
        battle.player().pbInflictStatus(PBStatuses.BURN, 0, null, null);
        assertEquals("BURN", battle.player().status);
        assertEquals("BURN", hero.status, "PokeBattle_Battler:104 status= writes @pokemon.status too");
    }
}
