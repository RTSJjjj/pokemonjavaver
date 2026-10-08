package pokemon.runtime.battle;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

class AiSwitchLoopTest {
    @Test
    @org.junit.jupiter.api.DisplayName("with nothing happening the AI does not keep switching every turn (switchingCooldown)")
    void doesNotPingPong() {
        File root = new File("../../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("../plugin-src");
        Assumptions.assumeTrue(new File(root, "pbs/moves.json").isFile());
        PbsData pbs = PbsData.parse(root);
        String[] ai = {"CHARIZARD", "BLASTOISE", "VENUSAUR", "GENGAR", "SNORLAX", "PIKACHU"};
        String[] pl = {"CHARIZARD", "GYARADOS", "ALAKAZAM", "EXEGGUTOR", "ONIX", "GENGAR", "SNORLAX", "DRAGONITE", "STARMIE", "MACHAMP"};
        for (int lvl : new int[] {30, 50, 80}) for (String foeSpecies : pl) {
            Battle b = new Battle(pbs, new Random(5), (user, target, moves) -> 0);
            b.trainerBattle = true;
            b.setSideSizes(1, 1);
            b.addPlayer(new Pokemon(pbs.species(foeSpecies), lvl, pbs));
            for (String s : ai) b.addFoe(new Pokemon(pbs.species(s), 50, pbs));
            StringBuilder sb = new StringBuilder("vs " + foeSpecies + ": ");
            int switches = 0;
            for (int turn = 1; turn <= 15; turn++) {
                Battler user = b.battlerAt(1);
                int pick = AiSwitching.decide(b, user, new Random(turn));
                if (pick >= 0 && b.canSwitchLax(1, pick) == null) {
                    sb.append(turn).append(":").append(user.name()).append("->").append(b.partyOf(1).get(pick).name()).append("[").append(AiSwitching.lastRule).append("] ");
                    BattleSwitchAction.pbReplace(b, 1, pick, false);
                    switches++;
                }
                for (Battler x : b.eachBattler()) if (x != null && x.aiSwitchCooldown > 0) x.aiSwitchCooldown--;
            }
            org.junit.jupiter.api.Assertions.assertTrue(switches <= 4, "AI keeps switching (" + switches + " in 15 turns, no damage dealt) vs " + foeSpecies + " L" + lvl + ": " + sb);
        }
    }
}
