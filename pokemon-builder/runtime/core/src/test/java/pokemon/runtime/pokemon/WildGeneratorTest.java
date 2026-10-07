package pokemon.runtime.pokemon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.state.Inventory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R15: {@code pbGenerateWildPokemon} (PField_Encounters:417-463) - the identity
 * a wild Pokemon rolls, which the runtime previously skipped entirely.
 *
 * <p>Every test draws from one long-lived {@link Random}, the way the game does:
 * a freshly seeded {@code Random} has a constant first byte for small seeds
 * (a JDK seeding artefact), which would hide the personality value's low byte
 * from a per-sample seed.</p>
 */
class WildGeneratorTest {

    @TempDir
    Path tempDir;

    private PbsData pbs;
    private TrainerState trainer;
    private final Random random = new Random(20251006);

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(tempDir, "pokemon.json", "{\"total\":4,\"byId\":{},\"species\":{"
                + "\"CATERPIE\":{\"id\":10,\"internalName\":\"CATERPIE\",\"name\":\"绿毛虫\","
                + "\"types\":[\"BUG\"],\"baseStats\":[45,30,35,45,20,20],\"rareness\":255,"
                + "\"weight\":2.9,\"genderRate\":\"Female50Percent\",\"happiness\":70,"
                + "\"abilities\":[\"SHIELDDUST\",\"RUNAWAY\"],\"hiddenAbility\":\"OVERGROW\","
                + "\"wildItems\":{\"common\":\"ORANBERRY\",\"uncommon\":\"SITRUSBERRY\","
                + "\"rare\":\"LEFTOVERS\"},\"evolutions\":[]},"
                + "\"PIKACHU\":{\"id\":25,\"internalName\":\"PIKACHU\",\"name\":\"皮卡丘\","
                + "\"types\":[\"ELECTRIC\"],\"baseStats\":[35,55,40,90,50,50],\"rareness\":190,"
                + "\"weight\":6.0,\"genderRate\":\"AlwaysMale\",\"happiness\":70,"
                + "\"abilities\":[\"STATIC\"],"
                + "\"wildItems\":{\"common\":\"ORANBERRY\",\"uncommon\":\"ORANBERRY\","
                + "\"rare\":\"ORANBERRY\"},\"evolutions\":[]},"
                + "\"MAGIKARP\":{\"id\":129,\"internalName\":\"MAGIKARP\",\"name\":\"鲤鱼王\","
                + "\"types\":[\"WATER\"],\"baseStats\":[20,10,55,80,15,20],\"rareness\":255,"
                + "\"weight\":10.0,\"genderRate\":\"Female50Percent\",\"happiness\":70,"
                + "\"abilities\":[\"SWIFTSWIM\"],\"evolutions\":[]},"
                + "\"GASTLY\":{\"id\":92,\"internalName\":\"GASTLY\",\"name\":\"鬼斯\","
                + "\"types\":[\"GHOST\",\"POISON\"],\"baseStats\":[30,35,30,80,100,35],"
                + "\"rareness\":190,\"weight\":0.1,\"genderRate\":\"Genderless\","
                + "\"happiness\":70,\"abilities\":[\"LEVITATE\"],\"evolutions\":[]}}}");
        write(tempDir, "items.json", "{\"total\":4,\"items\":{"
                + "\"ORANBERRY\":{\"id\":1,\"internalName\":\"ORANBERRY\",\"name\":\"橙橙果\",\"type\":5},"
                + "\"SITRUSBERRY\":{\"id\":2,\"internalName\":\"SITRUSBERRY\",\"name\":\"文柚果\",\"type\":5},"
                + "\"LEFTOVERS\":{\"id\":3,\"internalName\":\"LEFTOVERS\",\"name\":\"剩饭\",\"pocket\":1},"
                + "\"SHINYCHARM\":{\"id\":4,\"internalName\":\"SHINYCHARM\",\"name\":\"闪耀护符\",\"pocket\":5}}}");
        write(tempDir, "natures.json", "{\"total\":25,\"natures\":[" + natures() + "]}");
        pbs = PbsData.parse(tempDir.toFile());
        trainer = new TrainerState();
        trainer.newId(new Random(4242));
    }

    /** The 25 natures in PBNatures order. */
    private static String natures() {
        String[][] table = {
            {"HARDY", null, null}, {"LONELY", "ATTACK", "DEFENSE"}, {"BRAVE", "ATTACK", "SPEED"},
            {"ADAMANT", "ATTACK", "SPATK"}, {"NAUGHTY", "ATTACK", "SPDEF"},
            {"BOLD", "DEFENSE", "ATTACK"}, {"DOCILE", null, null}, {"RELAXED", "DEFENSE", "SPEED"},
            {"IMPISH", "DEFENSE", "SPATK"}, {"LAX", "DEFENSE", "SPDEF"},
            {"TIMID", "SPEED", "ATTACK"}, {"HASTY", "SPEED", "DEFENSE"}, {"SERIOUS", null, null},
            {"JOLLY", "SPEED", "SPATK"}, {"NAIVE", "SPEED", "SPDEF"},
            {"MODEST", "SPATK", "ATTACK"}, {"MILD", "SPATK", "DEFENSE"}, {"QUIET", "SPATK", "SPEED"},
            {"BASHFUL", null, null}, {"RASH", "SPATK", "SPDEF"},
            {"CALM", "SPDEF", "ATTACK"}, {"GENTLE", "SPDEF", "DEFENSE"}, {"SASSY", "SPDEF", "SPEED"},
            {"CAREFUL", "SPDEF", "SPATK"}, {"QUIRKY", null, null},
        };
        StringBuilder json = new StringBuilder();
        for (int i = 0; i < table.length; i++) {
            if (i > 0) json.append(",");
            json.append("{\"id\":").append(i)
                .append(",\"internalName\":\"").append(table[i][0])
                .append("\",\"name\":\"").append(table[i][0])
                .append("\",\"statUp\":").append(json(table[i][1]))
                .append(",\"statDown\":").append(json(table[i][2])).append("}");
        }
        return json.toString();
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private Pokemon wild(String species, int level) {
        return WildGenerator.generate(pbs, pbs.species(species), level, trainer, null, 42, false, random);
    }

    @Test
    @DisplayName("the personality value is rolled, so gender and nature vary (PokeBattle_Pokemon:919-922)")
    void rollsPersonalityValue() {
        Set<Integer> ids = new HashSet<>();
        Set<Integer> natureIds = new HashSet<>();
        Set<Integer> genders = new HashSet<>();
        for (int i = 0; i < 120; i++) {
            Pokemon pokemon = wild("CATERPIE", 10);
            ids.add(pokemon.personalID);
            natureIds.add(pokemon.nature == null ? -1 : pokemon.nature.id);
            genders.add(pokemon.effectiveGender());
        }
        assertEquals(120, ids.size(), "every wild Pokemon gets its own personality value");
        assertTrue(natureIds.size() > 15, "nature follows personalID % 25: " + natureIds.size());
        assertEquals(Set.of(PokemonStats.MALE, PokemonStats.FEMALE), genders,
                "a Female50Percent species produces both genders");
    }

    @Test
    @DisplayName("nature is personalID % 25 unless something set it (PokeBattle_Pokemon:286-288)")
    void natureFromPersonalID() {
        for (int i = 0; i < 60; i++) {
            Pokemon pokemon = wild("CATERPIE", 10);
            assertEquals(Math.floorMod(pokemon.personalID, 25), pokemon.nature.id);
        }
    }

    @Test
    @DisplayName("the ability slot comes from personalID & 1 (PokeBattle_Pokemon:219-245)")
    void abilitySlot() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 400; i++) {
            Pokemon pokemon = wild("CATERPIE", 10);
            int slot = pokemon.personalID & 1;
            seen.add(pokemon.ability);
            if ("SHIELDDUST".equals(pokemon.ability)) {
                assertEquals(0, slot, "the first natural ability needs slot 0");
            }
            if ("RUNAWAY".equals(pokemon.ability)) {
                assertEquals(1, slot, "the second natural ability needs slot 1");
            }
            assertTrue(Set.of("SHIELDDUST", "RUNAWAY", "OVERGROW").contains(pokemon.ability),
                    "unexpected ability " + pokemon.ability);
        }
        assertEquals(Set.of("SHIELDDUST", "RUNAWAY", "OVERGROW"), seen,
                "both natural slots and the hidden ability appear");
    }

    @Test
    @DisplayName("the hidden ability roll is rand(4096) < 256 (:419-421)")
    void hiddenAbilityRoll() {
        int hidden = 0;
        for (int i = 0; i < 4000; i++) {
            if ("OVERGROW".equals(wild("CATERPIE", 10).ability)) {
                hidden++;
            }
        }
        // 256/4096 = 6.25 %: ~250 of 4000 (a per-slot reading would give ~2000).
        assertTrue(hidden > 150 && hidden < 400, "hidden ability count: " + hidden);
    }

    @Test
    @DisplayName("a genderless species ignores the personality value (PokeBattle_Pokemon:166-172)")
    void genderlessSpecies() {
        for (int i = 0; i < 20; i++) {
            assertEquals(PokemonStats.GENDERLESS, wild("GASTLY", 10).effectiveGender());
            assertEquals(PokemonStats.MALE, wild("PIKACHU", 10).effectiveGender(),
                    "an AlwaysMale species is male whatever the personality value says");
        }
    }

    @Test
    @DisplayName("wild hold items follow the 50/5/1 chances (PField_Encounters:422-435)")
    void heldItems() {
        int common = 0;
        int uncommon = 0;
        int rare = 0;
        int none = 0;
        for (int i = 0; i < 2000; i++) {
            Pokemon pokemon = wild("CATERPIE", 10);
            if (pokemon.item == null) none++;
            else if ("ORANBERRY".equals(pokemon.item)) common++;
            else if ("SITRUSBERRY".equals(pokemon.item)) uncommon++;
            else if ("LEFTOVERS".equals(pokemon.item)) rare++;
        }
        assertTrue(common > 900 && common < 1100, "common ~50 %: " + common);
        assertTrue(uncommon > 50 && uncommon < 160, "uncommon ~5 %: " + uncommon);
        assertTrue(rare > 5 && rare < 45, "rare ~1 %: " + rare);
        assertTrue(none > 800, "the rest hold nothing: " + none);
    }

    @Test
    @DisplayName("three identical wild items are always held (:429)")
    void identicalItemsAlwaysHeld() {
        for (int i = 0; i < 60; i++) {
            assertEquals("ORANBERRY", wild("PIKACHU", 10).item);
        }
    }

    @Test
    @DisplayName("the lead Pokemon's ability changes the item odds and the nature (:426-427, :456-458)")
    void leadAbilities() {
        Pokemon compoundEyes = new Pokemon(pbs.species("MAGIKARP"), 20, pbs);
        compoundEyes.ability = "COMPOUNDEYES";
        compoundEyes.nature = pbs.natures.get(3);
        trainer.party.add(compoundEyes);
        int common = 0;
        for (int i = 0; i < 2000; i++) {
            if ("ORANBERRY".equals(wild("CATERPIE", 10).item)) common++;
        }
        assertTrue(common > 1100 && common < 1300, "CompoundEyes raises it to 60 %: " + common);

        // Synchronize copies the lead's nature instead (:456-458).
        trainer.party.members().clear();
        Pokemon synchronize = new Pokemon(pbs.species("MAGIKARP"), 20, pbs);
        synchronize.ability = "SYNCHRONIZE";
        synchronize.nature = pbs.natures.get(3);
        trainer.party.add(synchronize);
        for (int i = 0; i < 40; i++) {
            assertEquals(3, wild("CATERPIE", 10).nature.id);
        }
    }

    @Test
    @DisplayName("Cute Charm skews the gender two thirds of the time (:450-456)")
    void cuteCharmGender() {
        Pokemon lead = new Pokemon(pbs.species("MAGIKARP"), 20, pbs);
        lead.ability = "CUTECHARM";
        lead.gender = PokemonStats.MALE;
        trainer.party.add(lead);
        int female = 0;
        for (int i = 0; i < 600; i++) {
            if (wild("CATERPIE", 10).effectiveGender() == PokemonStats.FEMALE) female++;
        }
        assertTrue(female > 340 && female < 460, "2/3 female against a male lead: " + female);
    }

    @Test
    @DisplayName("a shiny is personalID ^ trainerID with the 32 threshold (:314-321)")
    void shinyFormula() {
        assertTrue(Pokemon.isShiny(0x00000000, 0x00000001), "d = 1");
        assertFalse(Pokemon.isShiny(0x00000000, 0x00000020), "d = 32 is not shiny");
        assertTrue(Pokemon.isShiny(0x12345678, 0x12345678), "identical halves give d = 0");

        int shiny = 0;
        for (int i = 0; i < 20000; i++) {
            if (wild("CATERPIE", 10).shiny) shiny++;
        }
        // 32/65536 = 1/2048, so ~10 of 20000.
        assertTrue(shiny > 2 && shiny < 32, "shiny count: " + shiny);
    }

    @Test
    @DisplayName("the Shiny Charm re-rolls twice, raising the odds (:436-442)")
    void shinyCharm() {
        Inventory bag = new Inventory();
        bag.add("SHINYCHARM", 1);
        Random plain = new Random(777);
        Random charmed = new Random(777);
        int without = 0;
        int with = 0;
        for (int i = 0; i < 20000; i++) {
            if (WildGenerator.generate(pbs, pbs.species("CATERPIE"), 10, trainer, null, 42, false,
                    plain).shiny) without++;
            if (WildGenerator.generate(pbs, pbs.species("CATERPIE"), 10, trainer, bag, 42, false,
                    charmed).shiny) with++;
        }
        assertTrue(with > without, "the charm can only add shinies: " + with + " vs " + without);
        assertTrue(with >= 10, "and reaches the ~1/683 rate: " + with);
    }

    @Test
    @DisplayName("Pokérus follows rand(65536) < POKERUS_CHANCE (:443-446)")
    void pokerus() {
        int infected = 0;
        for (int i = 0; i < 20000; i++) {
            Pokemon pokemon = wild("CATERPIE", 10);
            if (pokemon.pokerus != 0) {
                infected++;
                int strain = (pokemon.pokerus >> 4) & 0xF;
                assertEquals(1 + (strain % 4), pokemon.pokerus & 0xF, "time follows the strain");
                assertTrue(strain >= 1 && strain <= 15, "strain " + strain);
            }
        }
        assertTrue(infected > 0 && infected < 12, "POKERUS_CHANCE is 3/65536: " + infected);
    }

    @Test
    @DisplayName("the OT data and obtain fields come from the trainer and the map (:941-955)")
    void ownership() {
        trainer.name = "小智";
        trainer.gender = PokemonStats.MALE;
        Pokemon pokemon = WildGenerator.generate(pbs, pbs.species("CATERPIE"), 12, trainer, null,
                42, true, random);
        assertEquals(trainer.id, pokemon.trainerID);
        assertEquals(trainer.id & 0xFFFF, pokemon.publicID, "publicID is the low half (:67-69)");
        assertEquals("小智", pokemon.originalTrainer);
        assertEquals(PokemonStats.MALE, pokemon.otGender);
        assertEquals(42, pokemon.obtainMap);
        assertEquals(12, pokemon.obtainLevel);
        assertEquals(4, pokemon.obtainMode, "a fateful encounter");
        assertEquals(0, wild("CATERPIE", 12).obtainMode, "a normal encounter is mode 0");
    }

    @Test
    @DisplayName("IVs are rand(IV_STAT_LIMIT+1) = 0..31 with no EVs (:925-931)")
    void ivsAndEvs() {
        boolean sawMax = false;
        boolean sawZero = false;
        for (int i = 0; i < 300; i++) {
            Pokemon pokemon = wild("CATERPIE", 10);
            for (int iv : pokemon.ivs) {
                assertTrue(iv >= 0 && iv <= 31, "IV out of range: " + iv);
                sawMax |= iv == 31;
                sawZero |= iv == 0;
            }
            for (int ev : pokemon.evs) {
                assertEquals(0, ev);
            }
        }
        assertTrue(sawMax && sawZero, "the full 0..31 range is reachable");
    }
}
