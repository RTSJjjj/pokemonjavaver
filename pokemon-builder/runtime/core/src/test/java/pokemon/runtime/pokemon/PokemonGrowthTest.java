package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 3 / P3: level-up move learning and level / happiness evolution.
 */
class PokemonGrowthTest {

    @Test
    @DisplayName("P3: a level-up learns the move of the new level")
    void learnsMoveOnLevelUp(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon pokemon = new Pokemon(data.species("STAGE1"), 4, data);
        assertTrue(hasMove(pokemon, "TACKLE"));
        assertFalse(hasMove(pokemon, "EMBER"));

        int oldLevel = pokemon.level;
        assertTrue(pokemon.gainExperience(pokemon.experienceToNextLevel()));
        assertEquals(5, pokemon.level);
        assertTrue(PokemonGrowth.afterLevelUp(pokemon, data, oldLevel));
        assertTrue(hasMove(pokemon, "EMBER"), "the level-5 move was learned");
    }

    @Test
    @DisplayName("P3: a level condition evolves the Pokemon and raises its HP")
    void evolvesOnLevel(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon pokemon = new Pokemon(data.species("STAGE1"), 9, data);
        int oldHp = pokemon.hp;

        int oldLevel = pokemon.level;
        assertTrue(pokemon.gainExperience(pokemon.experienceToNextLevel()));
        assertEquals(10, pokemon.level);
        assertTrue(PokemonGrowth.afterLevelUp(pokemon, data, oldLevel));

        assertEquals("STAGE2", pokemon.species.internalName);
        assertEquals("Stage Two", pokemon.name);
        assertTrue(pokemon.hp >= oldHp, "the maximum-HP increase also raised the current HP");
        assertEquals(120, pokemon.baseStat(PokemonStats.HP), "the evolved base stats apply");
    }

    @Test
    @DisplayName("P3: happiness evolution and the four-move cap")
    void happinessAndMoveCap(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon happy = new Pokemon(data.species("FRIEND"), 20, data);
        happy.happiness = 220;
        assertTrue(PokemonGrowth.evolveOnCondition(happy, data));
        assertEquals("STAGE2", happy.species.internalName);

        Pokemon full = new Pokemon(data.species("STAGE1"), 4, data);
        assertFalse(PokemonGrowth.learnMove(full, data.move("TACKLE")),
                "an already known move is not learned twice");
        full.moves.add(new Pokemon.MoveSlot(data.move("EMBER")));
        full.moves.add(new Pokemon.MoveSlot(data.move("GROWL")));
        full.moves.add(new Pokemon.MoveSlot(data.move("GROWL")));
        assertEquals(4, full.moves.size);
        assertFalse(PokemonGrowth.learnMove(full, data.move("QUICKATTACK")),
                "a full move list keeps the replace prompt for P4");
    }

    private static boolean hasMove(Pokemon pokemon, String internalName) {
        for (int i = 0; i < pokemon.moves.size; i++) {
            PbsData.Move move = pokemon.moves.get(i).move;
            if (move != null && internalName.equals(move.internalName)) {
                return true;
            }
        }
        return false;
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":3,\"species\":{"
                + "\"STAGE1\":{\"id\":1,\"internalName\":\"STAGE1\",\"name\":\"Stage One\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[45,49,49,45,65,65],\"growthRate\":\"Medium\","
                + "\"baseExp\":60,\"abilities\":[\"OVERGROW\"],\"happiness\":70,"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"},{\"level\":5,\"move\":\"EMBER\"}],"
                + "\"evolutions\":[{\"species\":\"STAGE2\",\"method\":\"Level\",\"parameter\":\"10\"}]},"
                + "\"STAGE2\":{\"id\":2,\"internalName\":\"STAGE2\",\"name\":\"Stage Two\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[120,100,100,100,100,100],\"growthRate\":\"Medium\","
                + "\"baseExp\":120,\"abilities\":[\"OVERGROW\"]},"
                + "\"FRIEND\":{\"id\":3,\"internalName\":\"FRIEND\",\"name\":\"Friend\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[50,50,50,50,50,50],\"growthRate\":\"Medium\","
                + "\"baseExp\":60,\"abilities\":[\"OVERGROW\"],\"happiness\":70,"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}],"
                + "\"evolutions\":[{\"species\":\"STAGE2\",\"method\":\"Happiness\",\"parameter\":\"220\"}]}}}");
        write(root, "moves.json", "{\"total\":4,\"moves\":{"
                + "\"TACKLE\":{\"id\":33,\"internalName\":\"TACKLE\",\"name\":\"Tackle\",\"power\":40,"
                + "\"type\":\"NORMAL\",\"category\":\"Physical\",\"pp\":35},"
                + "\"EMBER\":{\"id\":52,\"internalName\":\"EMBER\",\"name\":\"Ember\",\"power\":40,"
                + "\"type\":\"NORMAL\",\"category\":\"Special\",\"pp\":25},"
                + "\"GROWL\":{\"id\":45,\"internalName\":\"GROWL\",\"name\":\"Growl\",\"power\":0,"
                + "\"type\":\"NORMAL\",\"category\":\"Status\",\"pp\":40},"
                + "\"QUICKATTACK\":{\"id\":98,\"internalName\":\"QUICKATTACK\",\"name\":\"Quick Attack\","
                + "\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\",\"pp\":30}}}");
        write(root, "abilities.json", "{\"total\":1,\"abilities\":{"
                + "\"OVERGROW\":{\"id\":65,\"internalName\":\"OVERGROW\",\"name\":\"Overgrow\"}}}");
        write(root, "types.json", "{\"total\":1,\"types\":{\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":[{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }
}
