package pokemon.runtime.battle;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/** Uses every move of the real PBS once, in a 1v1 and a 2v2 battle, and reports the ones whose effect throws (unwired stubs, NPEs). */
class MoveSmokeRealPbsTest {

    private static PbsData real() {
        File root = new File("../../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("plugin-src");
        Assumptions.assumeTrue(new File(root, "pbs/moves.json").isFile());
        return PbsData.parse(root);
    }

    private static Battle battle(PbsData p, String move, boolean doubles, int variant) {
        Battle battle = new Battle(p, new Random(1), (user, target, moves) -> 0);
        if (doubles) battle.setSideSizes(2, 2);
        Pokemon hero = new Pokemon(p.species("BULBASAUR"), 50, p);
        hero.moves.clear();
        hero.moves.add(new Pokemon.MoveSlot(p.move(move)));
        hero.moves.add(new Pokemon.MoveSlot(p.move("TACKLE")));
        battle.addPlayer(hero);
        battle.addFoe(new Pokemon(p.species("CHARMANDER"), 50, p));
        if (doubles) {
            battle.addPlayer(new Pokemon(p.species("PIKACHU"), 50, p));
            battle.addFoe(new Pokemon(p.species("SQUIRTLE"), 50, p));
        } else {
            battle.addFoe(new Pokemon(p.species("SQUIRTLE"), 50, p));
            battle.addPlayer(new Pokemon(p.species("PIKACHU"), 50, p));
        }
        battle.badges = new HashSet<>();
        for (Battler b : battle.eachBattler()) b.initEffects(false);
        if (variant == 1) {
            battle.foe().setStatus("POISON");
            battle.player().setHp(battle.player().maxHp() / 2);
            battle.foe().stages[0] = 2;
            battle.player().stages[1] = -1;
        }
        return battle;
    }

    @Test
    @DisplayName("every real move can be used without an unwired stub or an exception")
    void everyMoveRuns() {
        PbsData p = real();
        Map<String, String> failures = new TreeMap<>();
        for (pokemon.runtime.pokemon.PbsData.Move data : p.moves.values()) {
            if (data.internalName.equals("STRUGGLE") || data.internalName.startsWith("QMARK")) continue;
            for (int mode = 0; mode < 4; mode++) {
                try {
                    Battle battle = battle(p, data.internalName, (mode & 1) == 1, mode >> 1);
                    Battler hero = battle.player();
                    hero.pbUseMove(new Object[] {":UseMove", 0, hero.moveSlot(0), -1}, false);
                } catch (Throwable e) {
                    StackTraceElement[] st = e.getStackTrace();
                    String where = st.length > 0 ? st[0].getClassName().replace("pokemon.runtime.battle.", "") + ":" + st[0].getLineNumber() : "";
                    failures.putIfAbsent(data.internalName, e.getClass().getSimpleName() + " " + e.getMessage() + " @ " + where);
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.size() + " moves fail:\n" + String.join("\n", failures.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue()).toList()));
    }
}
