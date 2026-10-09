package pokemon.runtime.pokemon;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 181_PField_DayCare on the project's real species data (skipped when generated/ is missing). */
class DayCareTest {
    private static final DayCare.World WORLD = new DayCare.World() {
        public int region() { return 0; }
        public boolean hasItem(String item) { return false; }
        public int mapId() { return 100; }
    };

    private static PbsData data() throws Exception {
        File root = pokemon.runtime.map.TestData.runtimeDataRoot();
        return root != null ? PbsData.parse(root) : null;
    }

    private static Pokemon parent(PbsData data, String species, int gender, int level) {
        Pokemon p = new Pokemon(data.species(species), level, data);
        p.gender = gender;
        p.personalID = 12345;
        return p;
    }

    @Test
    void depositCostAndWithdraw() throws Exception {
        PbsData data = data();
        if (data == null) return;
        TrainerState trainer = new TrainerState();
        trainer.party.add(parent(data, "TORCHIC", PokemonStats.MALE, 10));
        trainer.party.add(parent(data, "TORCHIC", PokemonStats.FEMALE, 10));
        Pokemon first = trainer.party.get(0);
        first.hp = 1;
        DayCare dc = trainer.dayCare;
        dc.deposit(trainer, 0);
        assertEquals(1, dc.deposited());
        assertEquals(1, trainer.party.size());
        assertEquals(first.maxHp(), first.hp, "the Day Care heals it");
        first.gainExperience(PokemonStats.experienceForLevel(first.growthRate(), 13) - first.exp);
        assertEquals(400, dc.cost(0), "(13-10+1)*100");
        assertEquals(first, dc.get(-1 + 1));
        assertNull(dc.get(-1), "daycare[-1] is the second slot");
        dc.withdraw(trainer, 0);
        assertEquals(0, dc.deposited());
        assertEquals(2, trainer.party.size());
    }

    @Test
    void twoCompatibleParentsMakeAnEgg() throws Exception {
        PbsData data = data();
        if (data == null) return;
        TrainerState trainer = new TrainerState();
        trainer.party.add(parent(data, "TORCHIC", PokemonStats.MALE, 10));
        trainer.party.add(parent(data, "TORCHIC", PokemonStats.FEMALE, 10));
        trainer.party.add(parent(data, "PIDGEY", PokemonStats.MALE, 10));
        DayCare dc = trainer.dayCare;
        dc.deposit(trainer, 0);
        dc.deposit(trainer, 0);
        assertEquals(2, dc.deposited());
        assertTrue(dc.compat() >= 2, "the same species");
        Pokemon egg = dc.generateEgg(trainer, data, new Random(5), WORLD);
        assertNotNull(egg);
        assertTrue(egg.egg);
        assertEquals("宝可梦蛋", egg.name);
        assertEquals("抚养夫妇", egg.obtainText);
        assertEquals(1, egg.level);
        assertTrue(egg.stepsToHatch > 0);
        assertTrue(egg.moves.size > 0 && egg.moves.size <= 4);
        assertEquals(2, trainer.party.size(), "the third Pokemon and the egg");
    }

    @Test
    void anEggAppearsAfter256Steps() throws Exception {
        PbsData data = data();
        if (data == null) return;
        TrainerState trainer = new TrainerState();
        trainer.party.add(parent(data, "TORCHIC", PokemonStats.MALE, 10));
        trainer.party.add(parent(data, "TORCHIC", PokemonStats.FEMALE, 10));
        trainer.party.add(parent(data, "PIDGEY", PokemonStats.MALE, 10));
        DayCare dc = trainer.dayCare;
        dc.deposit(trainer, 0);
        dc.deposit(trainer, 0);
        Random alwaysLow = new Random() { @Override public int nextInt(int bound) { return 0; } };
        for (int i = 0; i < 255; i++) dc.onStep(data, alwaysLow, false, false, j -> true);
        assertEquals(0, dc.egg);
        dc.onStep(data, alwaysLow, false, false, j -> true);
        assertEquals(1, dc.egg, "the 256th step rolls the egg");
        assertTrue(dc.eggGenerated());
    }
}
