package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.TrainerState;

import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 186_PTrainer_NPCTrainers:80-127 pbLoadTrainer: what trainers.txt leaves out. */
class TrainerPartyDefaultsTest {

    private PbsData data(Path dir) {
        PbsData data = PbsData.parse(dir.toFile());
        for (int i = 0; i < 25; i++) {
            PbsData.Nature nature = new PbsData.Nature();
            nature.id = i;
            nature.internalName = "N" + i;
            data.natures.add(nature);
            data.naturesByName.put(nature.internalName, nature);
        }
        PbsData.Species species = new PbsData.Species();
        species.id = 7;
        species.internalName = "TEST";
        species.name = "测试";
        species.baseStats = new int[] {50, 50, 50, 50, 50, 50};
        species.genderRate = "FemaleOneEighth";
        species.abilities.add("FIRSTABILITY");
        species.abilities.add("SECONDABILITY");
        data.species.put("TEST", species);
        PbsData.TrainerType type = new PbsData.TrainerType();
        type.id = 3;
        type.internalName = "LASS";
        type.fields.add("");
        type.fields.add("");
        type.fields.add("Female");
        data.trainerTypes.put("LASS", type);
        return data;
    }

    private PbsData.TrainerData trainer() {
        PbsData.TrainerData trainer = new PbsData.TrainerData();
        trainer.type = "LASS";
        trainer.name = "测试";
        return trainer;
    }

    @Test
    @DisplayName("undefined fields take the plugin's defaults, not zero")
    void defaults(@TempDir Path dir) {
        PbsData data = data(dir);
        HeadlessBattlePort port = new HeadlessBattlePort(new TrainerState(), () -> data, null, new Random(1));
        PbsData.TrainerPokemon member = new PbsData.TrainerPokemon();
        member.species = "TEST";
        member.level = 40;
        Pokemon pokemon = port.trainerPokemon(trainer(), member, data);
        for (int i = 0; i < 6; i++) {
            assertEquals(20, pokemon.ivs[i], ":106 min(level/2, 31)");
            assertEquals(60, pokemon.evs[i], ":111 min(level*3/2, 85)");
        }
        assertEquals(10, pokemon.nature.id, ":100 (species 7 + trainer type 3) % 25");
        assertEquals("FIRSTABILITY", pokemon.ability, ":96 setAbility(0)");
        assertEquals(PokemonStats.FEMALE, pokemon.gender, ":97 a female trainer's Pokemon");
    }

    @Test
    @DisplayName("what trainers.txt defines is kept; a one-number IV list fills all six")
    void defined(@TempDir Path dir) {
        PbsData data = data(dir);
        HeadlessBattlePort port = new HeadlessBattlePort(new TrainerState(), () -> data, null, new Random(1));
        PbsData.TrainerPokemon member = new PbsData.TrainerPokemon();
        member.species = "TEST";
        member.level = 40;
        member.ivs = new int[] {31};
        member.evs = new int[] {252, 0, 4};
        member.nature = "N3";
        member.ability = "1";
        member.gender = "male";
        Pokemon pokemon = port.trainerPokemon(trainer(), member, data);
        assertArrayEquals(new int[] {31, 31, 31, 31, 31, 31}, pokemon.ivs);
        assertArrayEquals(new int[] {252, 0, 4, 252, 252, 252}, pokemon.evs);
        assertEquals(3, pokemon.nature.id);
        assertEquals("SECONDABILITY", pokemon.ability);
        assertEquals(PokemonStats.MALE, pokemon.gender);
    }

    @Test
    @DisplayName("a Form from trainers.txt is used, with the form's abilities, moves and stats; SuperShiny and Ball too")
    void form(@TempDir Path dir) {
        PbsData data = data(dir);
        PbsData.Move move = new PbsData.Move();
        move.internalName = "FORMMOVE";
        move.name = "形态招式";
        move.pp = 10;
        data.moves.put(move.internalName, move);
        PbsData.SpeciesForm form = new PbsData.SpeciesForm();
        form.species = "TEST";
        form.form = 2;
        form.key = "TEST_2";
        form.baseStats = new int[] {100, 50, 50, 50, 50, 50};
        form.abilities = new com.badlogic.gdx.utils.Array<>();
        form.abilities.add("FORMABILITY");
        form.moves = new com.badlogic.gdx.utils.Array<>();
        PbsData.LearnMove learn = new PbsData.LearnMove();
        learn.level = 1;
        learn.move = "FORMMOVE";
        form.moves.add(learn);
        data.forms.put(form.key, form);
        HeadlessBattlePort port = new HeadlessBattlePort(new TrainerState(), () -> data, null, new Random(1));
        PbsData.TrainerPokemon member = new PbsData.TrainerPokemon();
        member.species = "TEST";
        member.level = 40;
        member.form = 2;
        member.formSet = true;
        member.superShiny = true;
        member.ball = "4";
        Pokemon pokemon = port.trainerPokemon(trainer(), member, data);
        assertEquals(2, pokemon.formIndex(), ":86 formSimple = poke[TPFORM]");
        assertEquals("FORMABILITY", pokemon.ability, ":96 setAbility(0) reads the form's table");
        assertEquals("FORMMOVE", pokemon.moves.get(0).move.internalName, ":94 resetMoves: the form's list");
        assertEquals(pokemon.maxHp(), pokemon.hp);
        assertEquals(100, pokemon.baseStat(0));
        assertTrue(pokemon.shiny && pokemon.superShiny, ":121-123 makeSuperShiny");
        assertEquals(4, pokemon.ballused, ":120 ballused = poke[TPBALL]");
    }

    @Test
    @DisplayName("two Pokemon of the same trainer get different personal ids")
    void personalIds(@TempDir Path dir) {
        PbsData data = data(dir);
        HeadlessBattlePort port = new HeadlessBattlePort(new TrainerState(), () -> data, null, new Random(1));
        PbsData.TrainerPokemon member = new PbsData.TrainerPokemon();
        member.species = "TEST";
        member.level = 5;
        assertNotEquals(port.trainerPokemon(trainer(), member, data).personalID,
                port.trainerPokemon(trainer(), member, data).personalID);
    }

    @Test
    @DisplayName("a given Pokemon belongs to the player, so the party menu offers 昵称 ($Trainer.id==pkmn.trainerID, :1334)")
    void givenPokemonIsOwned(@TempDir Path dir) {
        PbsData data = data(dir);
        TrainerState player = new TrainerState();
        player.id = 123456789;
        Pokemon gifted = pokemon.runtime.pokemon.WildGenerator.pbNewPkmn(data, data.species("TEST"), 5, player, 3, new Random(1));
        assertEquals(player.id, gifted.trainerID);
        assertEquals(player.id & 0xFFFF, gifted.publicID);
    }
}
