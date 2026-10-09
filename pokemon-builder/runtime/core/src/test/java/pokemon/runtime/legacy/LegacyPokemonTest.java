package pokemon.runtime.legacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.legacy.MarshalWriter.Obj;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Ruby {@code PokeBattle_Pokemon} (as {@code Marshal} writes it) to this runtime's {@link Pokemon}. */
class LegacyPokemonTest {

    @TempDir
    Path tempDir;
    PbsData pbs;

    private void write(String file, String json) throws IOException {
        Path pbsDir = tempDir.resolve("pbs");
        Files.createDirectories(pbsDir);
        Files.write(pbsDir.resolve(file), json.getBytes(StandardCharsets.UTF_8));
    }

    private static String move(String id, int number, int pp) {
        return "\"" + id + "\":{\"id\":" + number + ",\"internalName\":\"" + id + "\",\"name\":\"" + id
                + "\",\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\",\"accuracy\":100,\"pp\":" + pp
                + ",\"effectChance\":0,\"target\":\"NearOther\",\"priority\":0,\"flags\":\"\"}";
    }

    @BeforeEach
    void setUp() throws IOException {
        write("index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write("pokemon.json", "{\"total\":2,\"byId\":{\"1\":\"BULBASAUR\",\"2\":\"TAUROS\"},\"species\":{"
                + "\"BULBASAUR\":{\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"妙蛙种子\",\"types\":[\"GRASS\"],"
                + "\"baseStats\":[45,49,49,45,65,65],\"rareness\":45,\"genderRate\":\"Female50Percent\","
                + "\"abilities\":[\"OVERGROW\",\"CHLOROPHYLL\"],\"hiddenAbility\":\"SOLARPOWER\",\"evolutions\":[]},"
                + "\"TAUROS\":{\"id\":2,\"internalName\":\"TAUROS\",\"name\":\"肯泰罗\",\"types\":[\"NORMAL\"],"
                + "\"baseStats\":[75,100,95,110,40,70],\"rareness\":45,\"genderRate\":\"AlwaysMale\","
                + "\"abilities\":[\"INTIMIDATE\"],\"evolutions\":[]}}}");
        write("moves.json", "{\"total\":2,\"moves\":{" + move("TACKLE", 33, 35) + "," + move("GROWL", 45, 40) + "}}");
        write("items.json", "{\"total\":1,\"items\":{\"POTION\":{\"id\":17,\"internalName\":\"POTION\",\"name\":\"伤药\"}}}");
        write("natures.json", "{\"total\":3,\"natures\":[{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"},"
                + "{\"id\":1,\"internalName\":\"LONELY\",\"name\":\"Lonely\",\"statUp\":\"ATTACK\",\"statDown\":\"DEFENSE\"},"
                + "{\"id\":2,\"internalName\":\"BRAVE\",\"name\":\"Brave\",\"statUp\":\"ATTACK\",\"statDown\":\"SPEED\"}]}");
        pbs = PbsData.parse(tempDir.toFile());
    }

    private Obj bulbasaur() {
        // personalID 26: 26 % 25 = 1 (LONELY), low bit 0 (first ability), low byte 26 < 127 (female)
        return new Obj("PokeBattle_Pokemon").set("species", 1).set("level", 30).set("exp", 27000).set("personalID", 26)
                .set("trainerID", 100000).set("ot", "沐桐").set("name", "小绿").set("happiness", 70)
                .set("iv", List.of(31, 30, 29, 28, 27, 26)).set("ev", List.of(252, 6, 0, 0, 252, 0));
    }

    private Pokemon convert(Obj obj, LegacyPokemon[] reader) {
        Object raw = new RubyMarshal(MarshalWriter.dump(obj)).load();
        reader[0] = new LegacyPokemon(pbs);
        return reader[0].convert((RubyMarshal.RObject) raw);
    }

    private Pokemon convert(Obj obj) {
        return convert(obj, new LegacyPokemon[1]);
    }

    @Test
    void theDerivedValuesFollowThePersonalId() {
        Pokemon p = convert(bulbasaur());
        assertEquals("BULBASAUR", p.species.internalName);
        assertEquals("小绿", p.name);
        assertEquals(30, p.level);
        assertEquals(27000, p.exp);
        assertEquals("LONELY", p.nature.internalName);                 // personalID % 25
        assertEquals("OVERGROW", p.ability);                           // personalID & 1
        assertEquals(PokemonStats.FEMALE, p.effectiveGender());        // personalID & 0xFF below the Female50Percent threshold
        assertArrayEquals(new int[] {31, 30, 29, 28, 27, 26}, p.ivs);
        assertArrayEquals(new int[] {252, 6, 0, 0, 252, 0}, p.evs);
        assertEquals(100000, p.trainerID);
        assertEquals(100000 & 0xFFFF, p.publicID);
        assertEquals("沐桐", p.originalTrainer);
        assertEquals(70, p.happiness);
    }

    @Test
    void theFlagsOverrideWhatThePersonalIdWouldGive() {
        Pokemon p = convert(bulbasaur().set("natureflag", 2).set("abilityflag", 2).set("genderflag", 0)
                .set("shinyflag", true).set("supershinyflag", true));
        assertEquals("BRAVE", p.nature.internalName);
        assertEquals("SOLARPOWER", p.ability);                         // abilityflag 2: the hidden ability
        assertEquals(PokemonStats.MALE, p.effectiveGender());
        assertTrue(p.shiny);
        assertTrue(p.superShiny);
        Pokemon notShiny = convert(bulbasaur().set("shinyflag", false).set("supershinyflag", true));
        assertFalse(notShiny.shiny);
        assertFalse(notShiny.superShiny);                              // super shiny only counts on a shiny
    }

    @Test
    void anUnsetShinyFlagIsDerivedFromThePersonalAndTrainerIds() {
        // personalID ^ trainerID = 5 -> (5 & 0xFFFF) ^ (5 >> 16) = 5 < 32
        Pokemon p = convert(bulbasaur().set("personalID", 100005).set("trainerID", 100000));
        assertTrue(p.shiny);
        assertFalse(convert(bulbasaur().set("personalID", 26).set("trainerID", 100000)).shiny);
    }

    @Test
    void aSingleGenderSpeciesIgnoresTheGenderFlag() {
        Pokemon p = convert(new Obj("PokeBattle_Pokemon").set("species", 2).set("level", 5).set("personalID", 7).set("genderflag", 1));
        assertEquals(PokemonStats.MALE, p.effectiveGender());
    }

    @Test
    void anAbilityFlagWithoutAHiddenAbilityFallsBackToTheNaturalSlot() {
        Pokemon p = convert(new Obj("PokeBattle_Pokemon").set("species", 2).set("level", 5).set("personalID", 7).set("abilityflag", 2));
        assertEquals("INTIMIDATE", p.ability);
    }

    @Test
    void movesKeepTheirPpAndPpUpsAndUnknownOnesAreNoted() {
        Obj m1 = new Obj("PBMove").set("id", 33).set("pp", 99).set("ppup", 3);
        Obj m2 = new Obj("PBMove").set("id", 45).set("pp", 7).set("ppup", 0);
        Obj m3 = new Obj("PBMove").set("id", 999).set("pp", 5).set("ppup", 0);
        Obj m4 = new Obj("PBMove").set("id", 0).set("pp", 0).set("ppup", 0);
        LegacyPokemon[] reader = new LegacyPokemon[1];
        Pokemon p = convert(bulbasaur().set("moves", List.of(m1, m2, m3, m4)).set("firstmoves", List.of(33, 45)), reader);
        assertEquals(2, p.moves.size);
        assertEquals("TACKLE", p.moves.get(0).move.internalName);
        assertEquals(3, p.moves.get(0).ppUp);
        assertEquals(35 + 35 * 3 / 5, p.moves.get(0).maxPp);          // PBMove#totalpp
        assertEquals(35 + 35 * 3 / 5, p.moves.get(0).pp);             // 99 is capped to the maximum
        assertEquals(7, p.moves.get(1).pp);
        assertTrue(reader[0].notes.contains("unknown move id 999"));
        assertEquals("TACKLE", p.firstMoves.get(0));
        assertEquals("GROWL", p.firstMoves.get(1));
    }

    @Test
    void itemsAndRibbonsAreResolvedFromTheirIds() {
        LegacyPokemon[] reader = new LegacyPokemon[1];
        Pokemon p = convert(bulbasaur().set("item", 17).set("ribbons", List.of(49, 66)), reader);
        assertEquals("POTION", p.item);
        assertTrue(p.hasRibbon("49"));
        assertTrue(p.hasRibbon("66"));
        Pokemon unknown = convert(bulbasaur().set("item", 4444), reader);
        assertNull(unknown.item);
        assertTrue(reader[0].notes.contains("unknown item id 4444"));
    }

    @Test
    void anUnknownSpeciesIsNotConverted() {
        LegacyPokemon[] reader = new LegacyPokemon[1];
        assertNull(convert(new Obj("PokeBattle_Pokemon").set("species", 4242).set("level", 5), reader));
        assertTrue(reader[0].notes.contains("unknown species id 4242"));
    }

    @Test
    void eggsStatusAndTheFusedPokemonAreCarried() {
        Obj fused = bulbasaur().set("level", 12);
        Pokemon p = convert(bulbasaur().set("eggsteps", 1200).set("status", 4).set("statusCount", 2).set("fused", fused)
                .set("hp", 5).set("obtainMap", 36).set("obtainMode", 1).set("obtainText", "抚养夫妇").set("hatchedMap", 38));
        assertTrue(p.egg);
        assertEquals(1200, p.stepsToHatch);
        assertEquals("PARALYSIS", p.status);
        assertEquals(2, p.statusCount);
        assertEquals(5, p.hp);
        assertEquals(36, p.obtainMap);
        assertEquals("抚养夫妇", p.obtainText);
        assertEquals(38, p.hatchedMap);
        assertNotNull(p.fused);
        assertEquals(12, p.fused.level);
    }

    @Test
    void aTimeIsReadAsEpochSeconds() {
        // 2026-10-09 12:34:56 UTC in Ruby's packed form
        long p = (1L << 31) | (0L << 30) | ((2026L - 1900) << 14) | (9L << 10) | (9L << 5) | 12L;
        long s = (34L << 26) | (56L << 20);
        byte[] data = new byte[8];
        for (int i = 0; i < 4; i++) {
            data[i] = (byte) (p >> (8 * i));
            data[4 + i] = (byte) (s >> (8 * i));
        }
        long epoch = LegacyPokemon.epochSeconds(readUser(data));
        assertEquals(java.time.LocalDateTime.of(2026, 10, 9, 12, 34, 56).toEpochSecond(java.time.ZoneOffset.UTC), epoch);
    }

    private static Object readUser(byte[] data) {
        return new RubyMarshal(MarshalWriter.dump(new MarshalWriter.User("Time", data))).load();
    }
}
