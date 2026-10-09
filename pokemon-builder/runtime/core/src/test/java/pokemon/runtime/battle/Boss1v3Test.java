package pokemon.runtime.battle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

/** Boss_Battles.rb battleMelodicricket (319:2732): a 1v3 boss fight must run its AI without any caught failure. */
class Boss1v3Test {
    @Test
    @org.junit.jupiter.api.DisplayName("1v3 Melodicricket boss: the AI never fails (crash 2026-10-09 23:42)")
    void melodicricketVsThree() {
        File root = new File("../../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("plugin-src");
        Assumptions.assumeTrue(new File(root, "pbs/moves.json").isFile());
        PbsData pbs = PbsData.parse(root);
        String[][] teams = {{"CHARIZARD", "BLASTOISE", "VENUSAUR"}, {"GENGAR", "SNORLAX", "PIKACHU"}, {"MACHAMP", "STARMIE", "DRAGONITE"}};
        for (String[] team : teams) for (int seed = 1; seed <= 8; seed++) {
            Battle b = new Battle(pbs, new Random(seed), (user, target, moves) -> 0);
            b.setSideSizes(3, 1);
            for (String s : team) b.addPlayer(new Pokemon(pbs.species(s), 70, pbs));
            Pokemon boss = new Pokemon(pbs.species("MELODICRICKET"), 70, pbs);
            for (String m : new String[] {"BOOMBURST", "BUGBUZZ", "SONICSLASH", "NASTYPLOT"}) boss.learnMoveSilently(pbs.move(m));
            boss.battleRank = 4;
            boss.item = "METRONOME";
            b.addFoe(boss);
            b.run(60);
            Assertions.assertTrue(b.aiFailures().isEmpty(), String.join(team[0], "seed " + seed + " ") + b.aiFailures());
        }
    }
}
