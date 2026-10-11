package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Intimidate (BattleHandlers_Abilities.rb:2619-2634 + Battler_StatStages:309-373) and the switch-out abilities the player's
 * hand switch used to skip, on the project's real data (skipped when generated/pbs is not there).
 */
class IntimidateRealDataTest {

    private static PbsData data() {
        File dir = new File("").getAbsoluteFile();
        for (File d = dir; d != null; d = d.getParentFile()) {
            for (String candidate : new String[] {"generated", "pokemon-builder/generated"}) {
                File root = new File(d, candidate);
                if (new File(root, "pbs").isDirectory()) return PbsData.parse(root);
            }
        }
        assumeTrue(false, "generated/pbs not found");
        return null;
    }

    private static Battle battle(PbsData data, Pokemon mine, Pokemon theirs) {
        return new Battle(data, new Random(3), (user, target, moves) -> 0).addPlayer(mine).addFoe(theirs);
    }

    @Test
    @DisplayName("Intimidate lowers the opposing Attack by one stage when it comes in")
    void intimidateLowersAttack() {
        PbsData data = data();
        Pokemon mine = new Pokemon(data.species("GARCHOMP"), 50, data);
        mine.ability = "SANDVEIL";
        Pokemon foe = new Pokemon(data.species("GYARADOS"), 50, data);
        foe.ability = "INTIMIDATE";
        Battle b = battle(data, mine, foe);
        assertEquals(0, b.player().stage(PBStats.ATTACK));
        b.foe().pbEffectsOnSwitchIn(true);
        assertEquals(-1, b.player().stage(PBStats.ATTACK), "Intimidate dropped the Attack");
    }

    @Test
    @DisplayName("Inner Focus stops Intimidate")
    void innerFocusBlocksIntimidate() {
        PbsData data = data();
        Pokemon mine = new Pokemon(data.species("GARCHOMP"), 50, data);
        mine.ability = "INNERFOCUS";
        Pokemon foe = new Pokemon(data.species("GYARADOS"), 50, data);
        foe.ability = "INTIMIDATE";
        Battle b = battle(data, mine, foe);
        b.foe().pbEffectsOnSwitchIn(true);
        assertEquals(0, b.player().stage(PBStats.ATTACK));
    }

    @Test
    @DisplayName("a hand switch runs the leaving Pokemon's switch-out ability (Natural Cure) through the screen's replace step")
    void handSwitchRunsSwitchOutAbility() {
        PbsData data = data();
        Pokemon paw = new Pokemon(data.species("PAWMOT"), 50, data);
        paw.ability = "NATURALCURE";
        Pokemon spare = new Pokemon(data.species("GARCHOMP"), 50, data);
        Pokemon foe = new Pokemon(data.species("BIDOOF"), 5, data);
        Battle b = new Battle(data, new Random(1), (user, target, moves) -> 0).addPlayer(paw).addPlayer(spare).addFoe(foe);
        b.player().status = "POISON";
        paw.status = "POISON";
        // what InteractiveBattlePort.Session#switchOutAndReplace does
        Battler leaving = b.battlerAt(0);
        leaving.pbAbilitiesOnSwitchOut();
        b.replace(0, 1);
        leaving.restoreAfterSwitchOut();
        assertEquals("", paw.status, "Natural Cure cured it");
        assertTrue(paw.hp > 0, "and the Pokemon is not left fainted in the party");
    }
    @Test
    @DisplayName("a fainted ally is counted (num_fainted_allies) and Supreme Overlord uses it when it comes in")
    void faintedAlliesPowerSupremeOverlord() {
        PbsData data = data();
        Pokemon hero = new Pokemon(data.species("SHINYGARCHOMP"), 50, data);
        hero.ability = "SUPREMEOVERLORD";
        Pokemon foe = new Pokemon(data.species("BIDOOF"), 5, data);
        Battle b = battle(data, hero, foe);
        assertEquals(0, b.player().numFaintedAllies());
        b.pbAddFaintedAlly(0);
        b.pbAddFaintedAlly(0);
        assertEquals(2, b.player().numFaintedAllies());
        b.player().pbEffectsOnSwitchIn(true);
        assertEquals(2, b.player().effects.intVal(PBEffects.Battler.SupremeOverlord));
    }
}
