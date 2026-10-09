package pokemon.runtime.battle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Random;

class AiBodyPressNullTest {
    @Test
    @org.junit.jupiter.api.DisplayName("Defense raised without Body Press in the moveset must not NPE (crash 2026-10-09 23:00)")
    void defenseRaisedWithoutBodyPress() throws Exception {
        File root = new File("../../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("plugin-src");
        Assumptions.assumeTrue(new File(root, "pbs/moves.json").isFile());
        PbsData pbs = PbsData.parse(root);
        Battle b = new Battle(pbs, new Random(5), (user, target, moves) -> 0);
        b.trainerBattle = true;
        b.setSideSizes(1, 1);
        b.addPlayer(new Pokemon(pbs.species("CHARIZARD"), 50, pbs));
        b.addFoe(new Pokemon(pbs.species("SNORLAX"), 50, pbs));
        Battler user = b.battlerAt(1);
        Battler foe = b.battlerAt(0);
        user.stages[1] = 1;
        AiCtx ctx = AiMaster.prepare(b, user, foe, new Random(1));
        Method m = AiSwitching.class.getDeclaredMethod("anyUsefulOffensiveStatRaised", AiCtx.class, Battler.class);
        m.setAccessible(true);
        Assertions.assertDoesNotThrow(() -> m.invoke(null, ctx, user));
    }
}
