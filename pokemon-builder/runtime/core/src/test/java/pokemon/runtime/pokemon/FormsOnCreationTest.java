package pokemon.runtime.pokemon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 198_Pokemon_Forms getFormOnCreation and 176_PField_EncounterModifiers for wild Pokemon. */
class FormsOnCreationTest {
    @TempDir Path temp;
    private PbsData data;
    private TrainerState trainer;
    private final Random random = new Random(7);

    /** A world with a fixed environment / region and the given switches on. */
    private static FormsOnCreation.World world(int environment, int region, int... switches) {
        return new FormsOnCreation.World() {
            @Override public int environment() { return environment; }
            @Override public int region() { return region; }
            @Override public boolean evening() { return false; }
            @Override public boolean night() { return false; }
            @Override public boolean switchOn(int id) {
                for (int s : switches) if (s == id) return true;
                return false;
            }
        };
    }

    private PbsData.Species species(String name, String... moves) {
        PbsData.Species s = new PbsData.Species();
        s.internalName = name;
        s.name = name;
        s.baseStats = new int[] {50, 50, 50, 50, 50, 50};
        s.genderRate = "Female50Percent";
        s.abilities.add("BASE");
        for (String m : moves) {
            PbsData.LearnMove learn = new PbsData.LearnMove();
            learn.level = 1;
            learn.move = m;
            s.moves.add(learn);
        }
        data.species.put(name, s);
        return s;
    }

    private void form(String name, int index, String ability, String... moves) {
        PbsData.SpeciesForm f = new PbsData.SpeciesForm();
        f.species = name;
        f.form = index;
        f.key = name + "_" + index;
        f.baseStats = new int[] {100, 50, 50, 50, 50, 50};
        if (ability != null) {
            f.abilities = new com.badlogic.gdx.utils.Array<>();
            f.abilities.add(ability);
        }
        if (moves.length > 0) {
            f.moves = new com.badlogic.gdx.utils.Array<>();
            for (String m : moves) {
                PbsData.LearnMove learn = new PbsData.LearnMove();
                learn.level = 1;
                learn.move = m;
                f.moves.add(learn);
            }
        }
        data.forms.put(f.key, f);
    }

    private void move(String name) {
        PbsData.Move m = new PbsData.Move();
        m.internalName = name;
        m.name = name;
        m.pp = 10;
        data.moves.put(name, m);
    }

    private Pokemon wild(String name, FormsOnCreation.World world) {
        return WildGenerator.generate(data, data.species(name), 10, trainer, null, 1, false, false, world, random);
    }

    @BeforeEach void setUp() {
        data = PbsData.parse(temp.toFile());
        trainer = new TrainerState();
        trainer.id = 0x00050001;                                            // secretID 5
        move("TACKLE");
        move("HIDDENPOWER");
        species("UNOWN", "HIDDENPOWER");
        for (int i = 1; i < 28; i++) form("UNOWN", i, null);
        species("BURMY", "TACKLE");
        form("BURMY", 1, null);
        form("BURMY", 2, null);
        species("ORICORIO", "TACKLE");
        for (int i = 1; i < 4; i++) form("ORICORIO", i, "FORMABILITY", "HIDDENPOWER");
        species("TAUROS", "TACKLE");
        for (int i = 1; i < 4; i++) form("TAUROS", i, null);
        species("VIVILLON", "TACKLE");
        for (int i = 1; i < 18; i++) form("VIVILLON", i, null);
        species("PIDGEY", "TACKLE");
        form("PIDGEY", 1, null);
    }

    @Test @DisplayName("a wild Unown is not always form A (rand(28))")
    void unownRandom() {
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 300; i++) seen.add(wild("UNOWN", null).formIndex());
        assertTrue(seen.size() > 10, "forms seen: " + seen);
    }

    @Test @DisplayName("Burmy's cloak follows pbGetEnvironment: cave -> Sandy, none -> Trash, grass -> Plant")
    void burmyCloak() {
        assertEquals(1, wild("BURMY", world(7, 0)).formIndex());
        assertEquals(2, wild("BURMY", world(0, 0)).formIndex());
        assertEquals(0, wild("BURMY", world(1, 0)).formIndex());
    }

    @Test @DisplayName("a form made on creation takes the form's abilities, moves and stats")
    void formDataFollows() {
        Pokemon p = null;
        for (int i = 0; i < 100 && (p == null || p.formIndex() == 0); i++) p = wild("ORICORIO", null);
        assertNotEquals(0, p.formIndex());
        assertEquals("FORMABILITY", p.ability);
        assertEquals("HIDDENPOWER", p.moves.get(0).move.internalName);
        assertEquals(p.maxHp(), p.hp);
        assertEquals(100, p.baseStat(0));
    }

    @Test @DisplayName("Vivillon's pattern is the player's secret ID % 18")
    void vivillonPattern() {
        assertEquals(5, wild("VIVILLON", null).formIndex());
    }

    @Test @DisplayName("Tauros / Wooper: Paldean forms only in region 0")
    void taurosRegion() {
        for (int i = 0; i < 50; i++) assertEquals(0, wild("TAUROS", world(1, 1)).formIndex());
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) seen.add(wild("TAUROS", world(1, 0)).formIndex());
        assertTrue(seen.size() > 1);
    }

    @Test @DisplayName("switch 36 turns every wild Pokemon into form 1; switch 31 makes it super shiny")
    void encounterModifiers() {
        assertEquals(0, wild("PIDGEY", world(1, 0)).formIndex());
        assertEquals(1, wild("PIDGEY", world(1, 0, 36)).formIndex());
        Pokemon shiny = wild("PIDGEY", world(1, 0, 31));
        assertTrue(shiny.shiny);
        assertTrue(shiny.superShiny);
    }

    @Test @DisplayName("a species without a getFormOnCreation proc stays in form 0")
    void noProc() {
        for (int i = 0; i < 50; i++) assertEquals(0, wild("PIDGEY", null).formIndex());
    }
}
