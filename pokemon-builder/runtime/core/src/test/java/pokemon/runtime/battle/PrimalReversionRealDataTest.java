package pokemon.runtime.battle;

import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Primal Reversion (316_Mega_evolution:463-502) on the real data: the battler takes the primal form's ability. */
class PrimalReversionRealDataTest {

    private static PbsData data() {
        for (File d = new File("").getAbsoluteFile(); d != null; d = d.getParentFile()) {
            for (String candidate : new String[] {"generated", "pokemon-builder/generated"}) {
                File root = new File(d, candidate);
                if (new File(root, "pbs").isDirectory()) return PbsData.parse(root);
            }
        }
        assumeTrue(false, "generated/pbs not found");
        return null;
    }

    private static void check(PbsData d, String species, String orb, String primalAbility, int weather) {
        PbsData.Species s = d.species(species);
        java.util.List<String> abilities = new java.util.ArrayList<>();
        for (String a : s.abilities) abilities.add(a);
        if (s.hiddenAbility != null) abilities.add(s.hiddenAbility);
        for (String ability : abilities) {
            Pokemon mine = new Pokemon(s, 70, d);
            mine.ability = ability;
            mine.item = orb;
            Battle b = new Battle(d, new Random(3), (u, t, moves) -> 0)
                    .addPlayer(mine)
                    .addFoe(new Pokemon(d.species("PIKACHU"), 50, d));
            b.foe().ability = "";
            b.player().pbEffectsOnSwitchIn(true);
            assertEquals(1, b.player().form(), species + " with " + ability + " reverts");
            assertEquals(primalAbility, mine.ability, species + " Pokemon ability from " + ability);
            assertEquals(primalAbility, b.player().ability, species + " battler ability from " + ability);
            assertEquals(weather, b.weather(), species + " weather from " + ability);
        }
    }

    @Test void primalGroudonBringsHarshSun() {
        check(data(), "GROUDON", "REDORB", "DESOLATELAND", PBWeather.HarshSun);
    }

    @Test void primalKyogreBringsHeavyRain() {
        check(data(), "KYOGRE", "BLUEORB", "PRIMORDIALSEA", PBWeather.HeavyRain);
    }
}
