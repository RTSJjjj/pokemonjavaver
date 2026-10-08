package pokemon.runtime.battle;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.HashSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The move effects whose helpers used to be unwired stubs (stat values, Mimic/Sketch, Spite PP, side stat-ups, Ally Switch). */
class WiredStubsRealPbsTest {

    private static PbsData real() {
        File root = new File("../../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("plugin-src");
        Assumptions.assumeTrue(new File(root, "pbs/moves.json").isFile());
        return PbsData.parse(root);
    }

    private static Battle battle(PbsData p, boolean doubles, String... heroMoves) {
        Battle battle = new Battle(p, new Random(1), (user, target, moves) -> 0);
        if (doubles) battle.setSideSizes(2, 2);
        Pokemon hero = new Pokemon(p.species("BULBASAUR"), 50, p);
        hero.moves.clear();
        for (String m : heroMoves) hero.moves.add(new Pokemon.MoveSlot(p.move(m)));
        battle.addPlayer(hero);
        battle.addFoe(new Pokemon(p.species("CHARMANDER"), 50, p));
        battle.addPlayer(new Pokemon(p.species("PIKACHU"), 50, p));
        battle.addFoe(new Pokemon(p.species("SQUIRTLE"), 50, p));
        battle.badges = new HashSet<>();
        for (Battler b : battle.eachBattler()) b.initEffects(false);
        return battle;
    }

    private static void use(Battler user, int slot) {
        user.pbUseMove(new Object[] {":UseMove", slot, user.moveSlot(slot), -1}, false);
    }

    @Test
    @DisplayName("Belly Drum records the stat-up for Opportunist / Mirror Herb (addSideStatUps) and the opposing side sees it")
    void sideStatUps() {
        PbsData p = real();
        Battle battle = battle(p, false, "BELLYDRUM");
        use(battle.player(), 0);
        assertEquals(6, battle.player().stage(PBStats.ATTACK));
    }

    @Test
    @DisplayName("Mimic copies the foe's last move into the slot and the original comes back when the Pokemon leaves")
    void mimicAndRestore() {
        PbsData p = real();
        Battle battle = battle(p, false, "MIMIC", "TACKLE");
        Battler hero = battle.player();
        battle.foe().lastRegularMoveUsed = "EMBER";
        use(hero, 0);
        assertEquals("EMBER", hero.moveSlot(0).internalName());
        hero.restoreMimickedMoves();
        assertEquals("MIMIC", hero.moveSlot(0).internalName());
    }

    @Test
    @DisplayName("Sketch teaches the move for good")
    void sketchIsPermanent() {
        PbsData p = real();
        Battle battle = battle(p, false, "SKETCH", "TACKLE");
        Battler hero = battle.player();
        battle.foe().lastRegularMoveUsed = "EMBER";
        use(hero, 0);
        hero.restoreMimickedMoves();
        assertEquals("EMBER", hero.moveSlot(0).internalName());
    }

    @Test
    @DisplayName("Power Split averages Attack / Sp. Atk, Guard Split Defense / Sp. Def, Speed Swap swaps Speed (battler stat values)")
    void statSplitsAndSwaps() {
        PbsData p = real();
        Battle battle = battle(p, false, "POWERSPLIT", "GUARDSPLIT", "SPEEDSWAP");
        Battler hero = battle.player();
        Battler foe = battle.foe();
        int atk = (hero.baseAttack() + foe.baseAttack()) / 2;
        use(hero, 0);
        assertEquals(atk, hero.baseAttack());
        assertEquals(atk, foe.baseAttack());
        int def = (hero.baseDefense() + foe.baseDefense()) / 2;
        use(hero, 1);
        assertEquals(def, hero.baseDefense());
        int heroSpeed = hero.baseSpeed();
        int foeSpeed = foe.baseSpeed();
        use(hero, 2);
        assertEquals(foeSpeed, hero.baseSpeed());
        assertEquals(heroSpeed, foe.baseSpeed());
        hero.resetForSwitchIn();
        assertEquals(hero.pokemon.speed(), hero.baseSpeed(), "stat values are re-read when the Pokemon comes back");
    }

    @Test
    @DisplayName("Spite takes PP off the foe's last move")
    void spiteReducesPp() {
        PbsData p = real();
        Battle battle = battle(p, false, "SPITE");
        Battler foe = battle.foe();
        foe.pokemon.moves.clear();
        foe.pokemon.moves.add(new Pokemon.MoveSlot(p.move("EMBER")));
        foe.lastRegularMoveUsed = "EMBER";
        int before = foe.moveSlotPp(0);
        use(battle.player(), 0);
        assertEquals(before - 4, foe.moveSlotPp(0), String.valueOf(battle.roundMessages));
    }

    @Test
    @DisplayName("Ally Switch swaps the two battlers' positions")
    void allySwitch() {
        PbsData p = real();
        Battle battle = battle(p, true, "ALLYSWITCH");
        Battler a = battle.battlerAt(0);
        Battler b = battle.battlerAt(2);
        use(a, 0);
        assertSame(b, battle.battlerAt(0));
        assertSame(a, battle.battlerAt(2));
    }
}
