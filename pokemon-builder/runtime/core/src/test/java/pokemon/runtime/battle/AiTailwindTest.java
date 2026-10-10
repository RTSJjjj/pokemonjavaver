package pokemon.runtime.battle;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** A trainer's Butterfree (pbTrainerBattle(:NURSE, ...)) kept using Tailwind every turn. */
class AiTailwindTest {

    private static PbsData real() {
        File root = new File("../../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("plugin-src");
        Assumptions.assumeTrue(new File(root, "pbs/moves.json").isFile(), "plugin-src/pbs not found");
        return PbsData.parse(root);
    }

    private Battler setup(PbsData p) {
        Battle battle = new Battle(p, new Random(5), (user, target, moves) -> 0);
        battle.trainerBattle = true;
        battle.addPlayer(new Pokemon(p.species("CHARMANDER"), 31, p));
        Pokemon butterfree = new Pokemon(p.species("BUTTERFREE"), 31, p);
        butterfree.moves.clear();
        for (String m : new String[] {"POLLENPUFF", "SLEEPPOWDER", "TAILWIND", "PROTECT"}) {
            butterfree.moves.add(new Pokemon.MoveSlot(p.move(m)));
        }
        battle.addFoe(butterfree);
        return battle.foe();
    }

    @Test
    @DisplayName("Tailwind that is already up is not chosen again")
    void tailwindAlreadyUp() {
        PbsData p = real();
        Battler foe = setup(p);
        foe.pbOwnSide().effects.set(PBEffects.Side.Tailwind, 3);
        for (int seed = 0; seed < 30; seed++) {
            int choice = AiMaster.chooseMove(foe.battle, foe, new Random(seed));
            assertNotEquals(2, choice, "seed " + seed + " chose Tailwind while it is active");
        }
    }

    @Test
    @DisplayName("without Tailwind up its score is not penalised")
    void tailwindNotUp() {
        PbsData p = real();
        Battler foe = setup(p);
        AiCtx ctx = new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST);
        int score = AiNegatives.score(ctx, foe, foe.battle.player(), foe.moveSlot(2), 100);
        assertTrue(score >= 100, "score " + score);
        foe.pbOwnSide().effects.set(PBEffects.Side.Tailwind, 3);
        assertTrue(AiNegatives.score(ctx, foe, foe.battle.player(), foe.moveSlot(2), 100) <= 90);
    }
}
