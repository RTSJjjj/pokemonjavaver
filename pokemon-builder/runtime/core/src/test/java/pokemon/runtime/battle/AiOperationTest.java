package pokemon.runtime.battle;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 手术 (OPERATION, 1A4) fails on a target without a status: the AI must not use it then, and should when there is one. */
class AiOperationTest {

    private static PbsData real() {
        File root = new File("../../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("plugin-src");
        Assumptions.assumeTrue(new File(root, "pbs/moves.json").isFile(), "plugin-src/pbs not found");
        return PbsData.parse(root);
    }

    private Battler setup(PbsData p) {
        Assumptions.assumeTrue(p.move("OPERATION") != null && p.species("HONCHKROW") != null);
        Battle battle = new Battle(p, new Random(5), (user, target, moves) -> 0);
        battle.trainerBattle = true;
        battle.addPlayer(new Pokemon(p.species("CHARMANDER"), 50, p));
        Pokemon crow = new Pokemon(p.species("HONCHKROW"), 50, p);
        crow.moves.clear();
        for (String m : new String[] {"OPERATION", "NIGHTSLASH", "TACKLE"}) {
            crow.moves.add(new Pokemon.MoveSlot(p.move(m)));
        }
        battle.addFoe(crow);
        return battle.foe();
    }

    @Test
    @DisplayName("without a status on the target the move scores below the base and is never picked")
    void noStatus() {
        PbsData p = real();
        Battler foe = setup(p);
        AiCtx ctx = new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST);
        assertTrue(AiNegatives.score(ctx, foe, foe.battle.player(), foe.moveSlot(0), 100) <= 90);
        for (int seed = 0; seed < 30; seed++) {
            assertNotEquals(0, AiMaster.chooseMove(foe.battle, foe, new Random(seed)), "seed " + seed);
        }
    }

    @Test
    @DisplayName("with a status on the target it is not penalised")
    void withStatus() {
        PbsData p = real();
        Battler foe = setup(p);
        Battler target = foe.battle.player();
        target.status = "BURN";
        AiCtx ctx = new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST);
        assertTrue(AiNegatives.score(ctx, foe, target, foe.moveSlot(0), 100) >= 100);
    }
}
