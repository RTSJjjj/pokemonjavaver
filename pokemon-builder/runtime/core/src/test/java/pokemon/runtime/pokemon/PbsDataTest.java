package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.map.TestData;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Stage 3 / P0: generated/pbs/*.json loading, lookups and the type chart.
 * The unit test writes a minimal PBS folder; a second test uses the real
 * project data when it is checked out next to the builder.
 */
class PbsDataTest {

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static File syntheticPbs(Path tempDir) throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(tempDir, "pokemon.json", "{\"total\":1,\"byId\":{\"1\":\"BULBASAUR\"},\"species\":{"
                + "\"BULBASAUR\":{\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"Bulbasaur\","
                + "\"types\":[\"GRASS\",\"POISON\"],\"baseStats\":[45,49,49,45,65,65],"
                + "\"genderRate\":\"Female50Percent\",\"growthRate\":\"Parabolic\","
                + "\"abilities\":[\"OVERGROW\"],\"hiddenAbility\":\"CHLOROPHYLL\","
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"},{\"level\":7,\"move\":\"VINEWHIP\"}],"
                + "\"evolutions\":[{\"species\":\"IVYSAUR\",\"method\":\"Level\",\"parameter\":\"16\"}],"
                + "\"wildItems\":{\"common\":\"LEFTOVERS\",\"uncommon\":null,\"rare\":null}}}}");
        write(tempDir, "pokemonforms.json", "{\"total\":1,\"forms\":{\"BULBASAUR_1\":{"
                + "\"species\":\"BULBASAUR\",\"form\":1,\"key\":\"BULBASAUR_1\",\"formName\":\"Mega\","
                + "\"baseStats\":[80,100,123,80,122,120],\"types\":[\"GRASS\",\"POISON\"],"
                + "\"abilities\":[\"THICKFAT\"],\"megaStone\":\"VENUSAURITE\"}}}");
        write(tempDir, "moves.json", "{\"total\":2,\"moves\":{"
                + "\"TACKLE\":{\"id\":33,\"internalName\":\"TACKLE\",\"name\":\"Tackle\",\"power\":40,"
                + "\"type\":\"NORMAL\",\"category\":\"Physical\",\"pp\":35,\"accuracy\":100},"
                + "\"VINEWHIP\":{\"id\":22,\"internalName\":\"VINEWHIP\",\"name\":\"Vine Whip\",\"power\":45,"
                + "\"type\":\"GRASS\",\"category\":\"Physical\",\"pp\":25,\"accuracy\":100}}}");
        write(tempDir, "items.json", "{\"total\":1,\"items\":{\"LEFTOVERS\":{\"id\":211,\"internalName\":\"LEFTOVERS\","
                + "\"name\":\"Leftovers\",\"namePlural\":\"Leftovers\",\"pocket\":1,\"price\":4000}}}");
        write(tempDir, "abilities.json", "{\"total\":2,\"abilities\":{"
                + "\"OVERGROW\":{\"id\":65,\"internalName\":\"OVERGROW\",\"name\":\"Overgrow\"},"
                + "\"CHLOROPHYLL\":{\"id\":34,\"internalName\":\"CHLOROPHYLL\",\"name\":\"Chlorophyll\"}}}");
        write(tempDir, "types.json", "{\"total\":5,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"FIGHTING\":{\"id\":1,\"internalName\":\"FIGHTING\",\"name\":\"Fighting\"},"
                + "\"GHOST\":{\"id\":7,\"internalName\":\"GHOST\",\"name\":\"Ghost\",\"immunities\":[\"NORMAL\"]},"
                + "\"FLYING\":{\"id\":2,\"internalName\":\"FLYING\",\"name\":\"Flying\",\"weaknesses\":[\"ELECTRIC\"]},"
                + "\"WATER\":{\"id\":10,\"internalName\":\"WATER\",\"name\":\"Water\",\"weaknesses\":[\"ELECTRIC\",\"GRASS\"]}}}");
        write(tempDir, "natures.json", "{\"total\":2,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\",\"statUp\":null,\"statDown\":null},"
                + "{\"id\":1,\"internalName\":\"LONELY\",\"name\":\"Lonely\",\"statUp\":\"ATTACK\",\"statDown\":\"DEFENSE\"}]}");
        write(tempDir, "trainertypes.json", "{\"total\":1,\"trainerTypes\":{\"POKEMONTRAINER_Red\":{"
                + "\"id\":0,\"internalName\":\"POKEMONTRAINER_Red\",\"name\":\"Trainer\",\"baseMoney\":60}}}");
        write(tempDir, "tm.json", "{\"total\":1,\"compatibility\":{\"VINEWHIP\":[\"BULBASAUR\"]}}");
        return tempDir.toFile();
    }

    @Test
    @DisplayName("P0: generated/pbs/*.json loads into the model tables")
    void loadsSyntheticPbs(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        assertTrue(data.loaded);
        assertEquals("pokemon-builder/pbs/1", data.version);
        assertEquals(1, data.species.size);
        assertEquals("BULBASAUR", data.speciesById.get("1"));

        PbsData.Species bulbasaur = data.species("BULBASAUR");
        assertNotNull(bulbasaur);
        assertEquals("Bulbasaur", bulbasaur.name);
        assertEquals(2, bulbasaur.types.size);
        assertEquals(45, bulbasaur.baseStat(0));
        assertEquals("Parabolic", bulbasaur.growthRate);
        assertEquals(2, bulbasaur.moves.size);
        assertEquals("IVYSAUR", bulbasaur.evolutions.get(0).species);
        assertEquals("LEFTOVERS", bulbasaur.wildItems.common);

        // SPECIES_N resolves to the base species; the form keeps the overrides.
        assertSame(bulbasaur, data.species("BULBASAUR_1"));
        PbsData.SpeciesForm mega = data.form("BULBASAUR", 1);
        assertNotNull(mega);
        assertEquals(80, mega.baseStat(0));
        assertEquals("VENUSAURITE", mega.megaStone);

        assertEquals(40, data.move("TACKLE").power);
        assertEquals(4000, data.item("LEFTOVERS").price);
        assertEquals("Overgrow", data.ability("OVERGROW").name);
        assertEquals("Lonely", data.nature(1).name);
        assertEquals("Lonely", data.nature("lonely").name);
        assertTrue(data.nature("Hardy").neutral());
        assertArrayEquals(new String[] {"BULBASAUR"}, data.tmCompatibility.get("VINEWHIP").toArray(String.class));
        assertTrue(data.tmBySpecies.get("BULBASAUR").contains("VINEWHIP", false));
    }

    @Test
    @DisplayName("P0: the type chart multiplies weaknesses, resistances and immunities")
    void typeChart(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        assertEquals(2f, data.effectiveness("ELECTRIC", types("WATER")));
        assertEquals(4f, data.effectiveness("ELECTRIC", types("WATER", "FLYING")));
        assertEquals(0f, data.effectiveness("NORMAL", types("GHOST")));
        assertEquals(1f, data.effectiveness("GRASS", types("NORMAL")), "an unlisted matchup is neutral");
    }

    private static com.badlogic.gdx.utils.Array<String> types(String... names) {
        com.badlogic.gdx.utils.Array<String> out = new com.badlogic.gdx.utils.Array<>();
        for (String name : names) {
            out.add(name);
        }
        return out;
    }

    @Test
    @DisplayName("P0: the real project PBS loads when it is available")
    void loadsRealPbs() {
        File root = TestData.runtimeDataRoot();
        assumeTrue(root != null, "runtime data (generated/) not available");
        PbsData data = PbsData.parse(root);
        assertTrue(data.loaded);
        assertTrue(data.species.size >= 1000, "species: " + data.species.size);
        assertTrue(data.forms.size >= 500, "forms: " + data.forms.size);
        assertTrue(data.moves.size >= 900, "moves: " + data.moves.size);
        assertTrue(data.items.size >= 900, "items: " + data.items.size);
        assertTrue(data.abilities.size >= 300, "abilities: " + data.abilities.size);
        assertTrue(data.types.size >= 18, "types: " + data.types.size);
        assertEquals(25, data.natures.size);

        PbsData.Species bulbasaur = data.species("BULBASAUR");
        assertNotNull(bulbasaur);
        assertArrayEquals(new String[] {"GRASS", "POISON"}, bulbasaur.types.toArray(String.class));
        assertEquals(120, PokemonStats.maxHp(bulbasaur.baseStat(0), 31, 0, 50));
        assertEquals(120, data.move("MEGAHORN").power);
        assertNotNull(data.form("VENUSAUR", 1), "mega forms are keyed SPECIES_FORM");
        assertEquals(2f, data.effectiveness("FIRE", types("GRASS", "POISON")), "grass is weak to fire");
    }
}
