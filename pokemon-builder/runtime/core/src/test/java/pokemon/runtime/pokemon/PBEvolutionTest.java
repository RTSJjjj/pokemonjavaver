package pokemon.runtime.pokemon;

import org.junit.jupiter.api.Test;
import pokemon.runtime.field.EvolutionWorld;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** 201_Pokemon_Evolution on the project's real species data (skipped when generated/ is not there). */
class PBEvolutionTest {

    private static PbsData data() throws Exception {
        File root = new File("E:/仓库/范例/929/pokemon-builder/generated");
        return root.isDirectory() ? PbsData.parse(root) : null;
    }

    @Test
    void levelMethodNeedsTheLevel() throws Exception {
        PbsData data = data();
        if (data == null) return;
        PBEvolution.Env env = new EvolutionWorld(new GameState(), data, null, () -> LocalTime.NOON);
        Pokemon bulbasaur = new Pokemon(data.species("BULBASAUR"), 15, data);
        assertNull(PBEvolution.checkEvolution(bulbasaur, null, env));
        bulbasaur.level = 16;
        assertEquals("IVYSAUR", PBEvolution.checkEvolution(bulbasaur, null, env));
    }

    @Test
    void everstoneBlocksEvolution() throws Exception {
        PbsData data = data();
        if (data == null) return;
        PBEvolution.Env env = new EvolutionWorld(new GameState(), data, null, () -> LocalTime.NOON);
        Pokemon bulbasaur = new Pokemon(data.species("BULBASAUR"), 20, data);
        bulbasaur.item = "EVERSTONE";
        assertNull(PBEvolution.checkEvolution(bulbasaur, null, env));
    }

    @Test
    void babyAndFamily() throws Exception {
        PbsData data = data();
        if (data == null) return;
        assertEquals("BULBASAUR", PBEvolution.babySpecies(data, data.species("VENUSAUR")).internalName);
        assertEquals("BULBASAUR", PBEvolution.previousForm(data, data.species("IVYSAUR")).internalName);
        assertTrue(PBEvolution.familyData(data, data.species("BULBASAUR")).size() >= 2);
    }

    @Test
    void speciesChangeKeepsNicknameAndMissingHp() throws Exception {
        PbsData data = data();
        if (data == null) return;
        Pokemon p = new Pokemon(data.species("BULBASAUR"), 16, data);
        p.name = "Bulby";
        int missing = 5;
        p.hp = p.maxHp() - missing;
        p.changeSpecies(data, data.species("IVYSAUR"));
        assertEquals("Bulby", p.name);
        assertEquals("IVYSAUR", p.species.internalName);
        assertEquals(p.maxHp() - missing, p.hp);
    }

    @Test
    void hiddenAbilitySurvivesEvolution() throws Exception {
        PbsData data = data();
        if (data == null) return;
        Pokemon torchic = new Pokemon(data.species("TORCHIC"), 16, data);
        torchic.ability = data.species("TORCHIC").hiddenAbility;
        assertEquals("DEFIANT", torchic.ability);
        torchic.changeSpecies(data, data.species("COMBUSKEN"));
        assertEquals("DEFIANT", torchic.ability, "the hidden slot follows the species");
        PokemonGrowth.evolve(torchic, data.species("BLAZIKEN"), data);
        assertEquals("DEFIANT", torchic.ability);
    }

    @Test
    void formSetBeforeEvolvingSurvivesIt() throws Exception {
        PbsData data = data();
        if (data == null) return;
        PBEvolution.Env env = new EvolutionWorld(new GameState(), data, null, () -> LocalTime.NOON);
        Pokemon magikarp = new Pokemon(data.species("MAGIKARP"), 30, data);
        assertEquals("GYARADOS", PBEvolution.checkEvolution(magikarp, "THUNDERSTONE", env));
        assertEquals(2, magikarp.formIndex(), "pkmn.form = 2 is kept although MAGIKARP_2 has no entry");
        magikarp.changeSpecies(data, data.species("GYARADOS"));
        assertNotNull(magikarp.form);
        assertEquals("GYARADOS_2", magikarp.form.key);
    }

    @Test
    void settingTheFormDoesNotResetTheAbility() throws Exception {
        PbsData data = data();
        if (data == null) return;
        Pokemon torchic = new Pokemon(data.species("TORCHIC"), 16, data);
        torchic.ability = data.species("TORCHIC").hiddenAbility;
        torchic.setForm(data, 0);
        assertEquals("DEFIANT", torchic.ability);
    }
}
