package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pokemon.runtime.map.TestData;
import pokemon.runtime.pokemon.PbsData;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Stage 4 / M0: {@link PBTypes} (PBTypes_Extra.rb:11-91) against the type data
 * the Compiler builds (Compiler_PBS:426-454).
 *
 * <p>The synthetic table reuses the <em>real</em> lists of this project's
 * {@code generated/pbs/types.json} for the pairs under test (NORMAL, FIGHTING,
 * GROUND, ROCK, GHOST, QMARKS, FIRE, WATER, GRASS, STELLAR), so the expected
 * values are the project's, not invented ones. This project's chart lists one
 * attack type as both a weakness and a resistance exactly once (PSYCHIC is weak
 * to DIM and resists DIM, asserted against the real data below), but never as a
 * weakness/immunity pair, so {@code OVERLAP} / {@code OVERLAP2} pin that
 * compiler write order (Compiler_PBS:434-436, last write wins) for the whole
 * 3-list case.</p>
 */
class PBTypesTest {

    @Test
    @DisplayName("M0: maxValue is the highest type id and regularTypesCount skips pseudo and gaps")
    void maxValueAndCount(@TempDir Path tempDir) throws Exception {
        PbsData pbs = synthetic(tempDir);
        // 13 entries, ids 0,1,4,5,7,9,10,11,12,13,19,20,21 (Compiler_PBS:389-390).
        assertEquals(21, PBTypes.maxValue(pbs));
        // 0..21 = 22 ids, minus QMARKS (IsPseudoType) minus the 9 numbering gaps
        // (Compiler_PBS:397 + :420-422) = 12 regular types (PBTypes_Extra.rb:25-32).
        assertEquals(12, PBTypes.regularTypesCount(pbs));
    }

    @Test
    @DisplayName("M0: isPseudoType / isSpecialType follow the types.txt flags (PBTypes_Extra:34-40)")
    void pseudoAndSpecial(@TempDir Path tempDir) throws Exception {
        PbsData pbs = synthetic(tempDir);
        assertTrue(PBTypes.isPseudoType(pbs, "QMARKS"), "IsPseudoType = true (PBS/types.txt line 64)");
        assertFalse(PBTypes.isPseudoType(pbs, "FIRE"));
        assertTrue(PBTypes.isPseudoType(pbs, "NOSUCHTYPE"),
                "an id missing from types.txt is pseudo too (Compiler_PBS:420-422)");
        assertTrue(PBTypes.isPseudoType(pbs, null));

        assertTrue(PBTypes.isSpecialType(pbs, "FIRE"), "IsSpecialType = true (types.txt FIRE)");
        assertTrue(PBTypes.isSpecialType(pbs, "WATER"));
        assertFalse(PBTypes.isSpecialType(pbs, "NORMAL"));
        assertFalse(PBTypes.isSpecialType(pbs, "QMARKS"));
        assertFalse(PBTypes.isSpecialType(pbs, "NOSUCHTYPE"));
        assertFalse(PBTypes.isSpecialType(pbs, null));
    }

    @Test
    @DisplayName("M0: getName resolves the types message table (Compiler_PBS:446-448)")
    void typeName(@TempDir Path tempDir) throws Exception {
        PbsData pbs = synthetic(tempDir);
        assertEquals("Grass", PBTypes.getName(pbs, "GRASS"));
        assertEquals("Fire", PBTypes.getName(pbs, "FIRE"));
        // getID (PSystem_Utilities:226-231) returns 0 for a name the module does
        // not know, so the unknown name reads the id-0 message.
        assertEquals("Normal", PBTypes.getName(pbs, "NOSUCHTYPE"));
        // getID returns nil for nil/empty (:225) and pbGetMessage is "" then
        // (Intl_Messages:555-561), never null.
        assertEquals("", PBTypes.getName(pbs, null));
        assertEquals("", PBTypes.getName(pbs, ""));
        assertEquals("", PBTypes.getName(null, "GRASS"));
    }

    @Test
    @DisplayName("M0: getEffectiveness returns the 0/1/2/4 chart cell (PBTypes_Extra:42-45)")
    void singleEffectiveness(@TempDir Path tempDir) throws Exception {
        PbsData pbs = synthetic(tempDir);
        assertEquals(4, PBTypes.getEffectiveness(pbs, "FIRE", "GRASS"), "GRASS is weak to FIRE");
        assertEquals(1, PBTypes.getEffectiveness(pbs, "FIRE", "WATER"), "WATER resists FIRE");
        assertEquals(0, PBTypes.getEffectiveness(pbs, "NORMAL", "GHOST"), "GHOST is immune to NORMAL");
        assertEquals(2, PBTypes.getEffectiveness(pbs, "NORMAL", "NORMAL"), "unlisted matchup is neutral");
        assertEquals(0, PBTypes.getEffectiveness(pbs, "ELECTRIC", "GROUND"));
        assertEquals(2, PBTypes.getEffectiveness(pbs, "NORMAL", null), "!targetType (:43)");
        assertEquals(2, PBTypes.getEffectiveness(pbs, "NORMAL", "NOSUCHTYPE"),
                "a target type outside the chart stays neutral (Compiler_PBS:433)");
        assertEquals(2, PBTypes.getEffectiveness(pbs, "NOSUCHTYPE", "GRASS"),
                "an attack type outside the chart stays neutral (Compiler_PBS:433)");
        assertEquals(2, PBTypes.getEffectiveness(null, "FIRE", "GRASS"),
                "no table at all: every cell is NORMAL_EFFECTIVE_ONE");
    }

    @Test
    @DisplayName("M0: the immunity write wins over weakness and resistance (Compiler_PBS:434-436)")
    void effectivenessWriteOrder(@TempDir Path tempDir) throws Exception {
        PbsData pbs = synthetic(tempDir);
        assertEquals(2, PBTypes.getEffectiveness(pbs, "WATER", "OVERLAP"), "WATER is in none of OVERLAP's lists");
        assertEquals(0, PBTypes.getEffectiveness(pbs, "FIRE", "OVERLAP"),
                "the same attack is a weakness, then a resistance, then an immunity -> 0 (Compiler_PBS:434-436)");
        assertEquals(1, PBTypes.getEffectiveness(pbs, "WATER", "OVERLAP2"),
                "resistance overwrites the same attack's weakness (Compiler_PBS:435)");
    }

    @Test
    @DisplayName("M0: getCombinedEffectiveness multiplies and de-duplicates the target types (PBTypes_Extra:47-59)")
    void combinedEffectiveness(@TempDir Path tempDir) throws Exception {
        PbsData pbs = synthetic(tempDir);
        assertEquals(8, PBTypes.getCombinedEffectiveness(pbs, "NORMAL", "NORMAL", null, null),
                "2*2*2: the plugin's NORMAL_EFFECTIVE is 8 (PBTypes_Extra.rb:6)");
        assertEquals(16, PBTypes.getCombinedEffectiveness(pbs, "FIRE", "GRASS", null, null), "4*2*2");
        assertEquals(32, PBTypes.getCombinedEffectiveness(pbs, "WATER", "GROUND", "ROCK", null), "4*4*2");
        assertEquals(4, PBTypes.getCombinedEffectiveness(pbs, "FIRE", "WATER", null, null), "1*2*2");
        assertEquals(0, PBTypes.getCombinedEffectiveness(pbs, "NORMAL", "GHOST", null, null), "0*2*2");
        assertEquals(16, PBTypes.getCombinedEffectiveness(pbs, "FIRE", "GRASS", "GRASS", null),
                "targetType1!=targetType2 (:51): the duplicate contributes 2");
        assertEquals(8, PBTypes.getCombinedEffectiveness(pbs, "FIRE", "GRASS", "WATER", "GRASS"),
                "targetType3==targetType1 (:54-55): the duplicate contributes 2 -> 4*1*2");
        assertEquals(8, PBTypes.getCombinedEffectiveness(pbs, "FIRE", "GRASS", "WATER", "WATER"),
                "targetType3==targetType2 (:55): 4*1*2, the duplicate is neutral");
        assertEquals(0, PBTypes.getCombinedEffectiveness(pbs, "NORMAL", "GHOST", "GHOST", null),
                "an immune duplicate stays out of the product: 0*2*2");
    }

    @Test
    @DisplayName("M0: the one-argument predicates compare damageState.typeMod on the 8 scale")
    void singleArgumentPredicates() {
        assertTrue(PBTypes.ineffective(0));                    // PBTypes_Extra.rb:61-65
        assertFalse(PBTypes.ineffective(1));
        assertFalse(PBTypes.ineffective(8));

        assertTrue(PBTypes.notVeryEffective(1));               // :67-71
        assertTrue(PBTypes.notVeryEffective(4), "0.5x is 1*2*2 = 4");
        assertFalse(PBTypes.notVeryEffective(0));
        assertFalse(PBTypes.notVeryEffective(8));

        assertTrue(PBTypes.resistant(0));                      // :73-77
        assertTrue(PBTypes.resistant(4));
        assertFalse(PBTypes.resistant(8));
        assertFalse(PBTypes.resistant(16));

        assertTrue(PBTypes.normalEffective(8));                // :79-83
        assertFalse(PBTypes.normalEffective(4));
        assertFalse(PBTypes.normalEffective(16));

        assertTrue(PBTypes.superEffective(16), "2x is 4*2*2 = 16");   // :85-89
        assertTrue(PBTypes.superEffective(32));
        assertFalse(PBTypes.superEffective(8));
    }

    @Test
    @DisplayName("M0: the type-name predicates use getCombinedEffectiveness (PBTypes_Extra:61-89)")
    void typeNamePredicates(@TempDir Path tempDir) throws Exception {
        PbsData pbs = synthetic(tempDir);
        assertTrue(PBTypes.ineffective(pbs, "NORMAL", "GHOST", null, null));
        assertFalse(PBTypes.ineffective(pbs, "NORMAL", "NORMAL", null, null));

        assertTrue(PBTypes.notVeryEffective(pbs, "FIRE", "WATER", null, null));
        assertFalse(PBTypes.notVeryEffective(pbs, "FIRE", "GRASS", null, null));

        assertTrue(PBTypes.resistant(pbs, "FIRE", "WATER", null, null));
        assertFalse(PBTypes.resistant(pbs, "FIRE", "GRASS", null, null));

        assertTrue(PBTypes.normalEffective(pbs, "NORMAL", "NORMAL", null, null));
        assertFalse(PBTypes.normalEffective(pbs, "FIRE", "GRASS", null, null));

        assertTrue(PBTypes.superEffective(pbs, "FIRE", "GRASS", null, null));
        assertTrue(PBTypes.superEffective(pbs, "WATER", "GROUND", "ROCK", null), "2x on both types: 32");
        assertFalse(PBTypes.superEffective(pbs, "FIRE", "WATER", null, null));

        // Battler_UseMove_Targeting:177 / Move_Usage_Calculations:67-68 pass the
        // second type as nil for single-typed defenders.
        assertTrue(PBTypes.superEffective(pbs, "FIRE", "GRASS", null, null));
    }

    @Test
    @DisplayName("M0: the real project types.json drives PBTypes")
    void realProjectTypes() {
        File root = TestData.runtimeDataRoot();
        assumeTrue(root != null, "runtime data (generated/) not available");
        PbsData pbs = PbsData.parse(root);
        assumeTrue(pbs.types.size > 0, "generated/pbs/types.json not available");

        // PBS/types.txt [0]..[22], 23 types, QMARKS flagged IsPseudoType (line 64).
        assertEquals(23, pbs.types.size, "types.txt entries");
        assertEquals(22, PBTypes.maxValue(pbs), "the highest id is VOID = 22 (Compiler_PBS:389-390,450)");
        assertEquals(22, PBTypes.regularTypesCount(pbs), "23 types minus the pseudo QMARKS");

        assertTrue(PBTypes.isPseudoType(pbs, "QMARKS"));
        assertFalse(PBTypes.isPseudoType(pbs, "NORMAL"));
        assertTrue(PBTypes.isSpecialType(pbs, "FIRE"));
        assertTrue(PBTypes.isSpecialType(pbs, "VOID"));
        assertFalse(PBTypes.isSpecialType(pbs, "NORMAL"));
        assertFalse(PBTypes.isSpecialType(pbs, "STELLAR"));

        // The four pairs of the acceptance list, taken from generated/pbs/types.json.
        assertEquals(4, PBTypes.getEffectiveness(pbs, "FIRE", "GRASS"));
        assertEquals(1, PBTypes.getEffectiveness(pbs, "FIRE", "WATER"));
        assertEquals(0, PBTypes.getEffectiveness(pbs, "NORMAL", "GHOST"));
        assertEquals(2, PBTypes.getEffectiveness(pbs, "NORMAL", "NORMAL"));

        assertEquals(8, PBTypes.getCombinedEffectiveness(pbs, "NORMAL", "NORMAL", null, null));
        assertEquals(16, PBTypes.getCombinedEffectiveness(pbs, "FIRE", "GRASS", null, null));
        assertEquals(32, PBTypes.getCombinedEffectiveness(pbs, "WATER", "GROUND", "ROCK", null));
        assertEquals(8, PBTypes.getCombinedEffectiveness(pbs, "FIRE", "GRASS", "WATER", null),
                "FIRE vs WATER is 1 and the empty third slot stays neutral -> 4*1*2");

        // The one real case where the compiler's write order matters: PSYCHIC
        // lists DIM under Weaknesses *and* Resistances, and the resistance is
        // written last (Compiler_PBS:434-435).
        assertTrue(pbs.types.get("PSYCHIC").weaknesses.contains("DIM", false));
        assertTrue(pbs.types.get("PSYCHIC").resistances.contains("DIM", false));
        assertEquals(1, PBTypes.getEffectiveness(pbs, "DIM", "PSYCHIC"), "resistance wins over weakness");

        // getName reads the types message table (Compiler_PBS:441,446-448).
        assertEquals(pbs.types.get("FIRE").name, PBTypes.getName(pbs, "FIRE"));
        assertFalse(PBTypes.getName(pbs, "FIRE").isEmpty(), "the real types.json carries the localised name");
    }

    // ------------------------------------------------------------------
    // A synthetic generated/pbs/types.json with the project's real lists.
    // ------------------------------------------------------------------

    private static PbsData synthetic(Path tempDir) throws Exception {
        Path pbs = tempDir.resolve("pbs");
        Files.createDirectories(pbs);
        Files.write(pbs.resolve("types.json"), TYPES_JSON.getBytes(StandardCharsets.UTF_8));
        return PbsData.parse(tempDir.toFile());
    }

    private static final String TYPES_JSON = """
            {"format":"pokemon-builder/pbs/1","kind":"pbsTypes","total":13,"types":{
              "NORMAL":{"id":0,"internalName":"NORMAL","name":"Normal","pseudoType":false,"specialType":false,
                "weaknesses":["FIGHTING","DIM","VOID"],"resistances":[],"immunities":["GHOST"]},
              "FIGHTING":{"id":1,"internalName":"FIGHTING","name":"Fighting","pseudoType":false,"specialType":false,
                "weaknesses":["FLYING","PSYCHIC","FAIRY","VOID"],"resistances":["ROCK","BUG","DARK"],"immunities":[]},
              "GROUND":{"id":4,"internalName":"GROUND","name":"Ground","pseudoType":false,"specialType":false,
                "weaknesses":["WATER","GRASS","ICE","VOID"],"resistances":["POISON","ROCK","DIM","LIGHT"],
                "immunities":["ELECTRIC"]},
              "ROCK":{"id":5,"internalName":"ROCK","name":"Rock","pseudoType":false,"specialType":false,
                "weaknesses":["FIGHTING","GROUND","STEEL","WATER","GRASS"],"resistances":["NORMAL","FLYING","POISON","FIRE"],
                "immunities":[]},
              "GHOST":{"id":7,"internalName":"GHOST","name":"Ghost","pseudoType":false,"specialType":false,
                "weaknesses":["GHOST","DARK","LIGHT","DIM","VOID"],"resistances":["POISON","BUG"],
                "immunities":["NORMAL","FIGHTING"]},
              "QMARKS":{"id":9,"internalName":"QMARKS","name":"???","pseudoType":true,"specialType":false,
                "weaknesses":[],"resistances":[],"immunities":["QMARKS"]},
              "FIRE":{"id":10,"internalName":"FIRE","name":"Fire","pseudoType":false,"specialType":true,
                "weaknesses":["GROUND","ROCK","WATER"],"resistances":["BUG","STEEL","FIRE","GRASS","ICE","FAIRY","DIM"],
                "immunities":["LIGHT"]},
              "WATER":{"id":11,"internalName":"WATER","name":"Water","pseudoType":false,"specialType":true,
                "weaknesses":["GRASS","ELECTRIC"],"resistances":["STEEL","FIRE","WATER","ICE"],"immunities":[]},
              "GRASS":{"id":12,"internalName":"GRASS","name":"Grass","pseudoType":false,"specialType":true,
                "weaknesses":["FLYING","POISON","BUG","FIRE","ICE","LIGHT","DIM"],
                "resistances":["GROUND","WATER","GRASS","ELECTRIC","VOID"],"immunities":[]},
              "ELECTRIC":{"id":13,"internalName":"ELECTRIC","name":"Electric","pseudoType":false,"specialType":true,
                "weaknesses":["GROUND","LIGHT"],"resistances":["FLYING","STEEL","ELECTRIC","DIM","VOID"],
                "immunities":[]},
              "STELLAR":{"id":19,"internalName":"STELLAR","name":"Stellar","pseudoType":false,"specialType":false,
                "weaknesses":[],"resistances":[],"immunities":[]},
              "OVERLAP":{"id":20,"internalName":"OVERLAP","name":"Overlap","pseudoType":false,"specialType":false,
                "weaknesses":["FIRE"],"resistances":["FIRE"],"immunities":["FIRE"]},
              "OVERLAP2":{"id":21,"internalName":"OVERLAP2","name":"Overlap2","pseudoType":false,"specialType":false,
                "weaknesses":["WATER"],"resistances":["WATER"],"immunities":[]}
            }}
            """;
}
