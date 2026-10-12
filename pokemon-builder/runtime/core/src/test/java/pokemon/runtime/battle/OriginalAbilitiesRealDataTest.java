package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** The project's own abilities ADAPTARMOR / RUYIBLADE / STEELDYNASTY on the real data (skipped without generated/pbs). */
class OriginalAbilitiesRealDataTest {

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

    private static int damage(PbsData data, String atkSpecies, String atkAbility, String defSpecies, String defAbility, String move) {
        Pokemon atkMon = new Pokemon(data.species(atkSpecies), 50, data);
        atkMon.ability = atkAbility;
        Pokemon defMon = new Pokemon(data.species(defSpecies), 50, data);
        defMon.ability = defAbility;
        Battle battle = new Battle(data, new Random(0), (u, t, m) -> 0);
        Battler atk = new Battler(atkMon, false);
        Battler def = new Battler(defMon, true);
        atk.battle = battle;
        def.battle = battle;
        BattleMove bm = new BattleMove(data.move(move));
        // Battler_UseMove_SuccessChecks:389-390 sets typeMod before the damage is calculated
        def.damageState.typeMod = PBTypes.getCombinedEffectiveness(data, bm.type(), defMon.types().get(0),
                defMon.types().size > 1 ? defMon.types().get(1) : null, null);
        long sum = 0;
        for (int seed = 0; seed < 40; seed++) {
            sum += DamageCalc.compute(atk, def, bm, data, new Random(seed));
        }
        return (int) (sum / 40);
    }

    @Test
    @DisplayName("Adaptive Armor: a super effective hit does regular damage")
    void adaptArmor() {
        PbsData data = data();
        int plain = damage(data, "MACHAMP", "NOGUARD", "BIDOOF", "SIMPLE", "BRICKBREAK");
        int armored = damage(data, "MACHAMP", "NOGUARD", "BIDOOF", "ADAPTARMOR", "BRICKBREAK");
        assertTrue(plain > 0 && armored > 0);
        double ratio = (double) armored / plain;
        assertTrue(ratio > 0.4 && ratio < 0.6, "about half of a super effective hit, was " + ratio);
    }

    @Test
    @DisplayName("Ruyi Blade: a resisted hit is not halved")
    void ruyiBlade() {
        PbsData data = data();
        int plain = damage(data, "BIDOOF", "SIMPLE", "REGISTEEL", "CLEARBODY", "TACKLE");
        int ruyi = damage(data, "BIDOOF", "RUYIBLADE", "REGISTEEL", "CLEARBODY", "TACKLE");
        assertTrue(plain > 0 && ruyi > 0);
        double ratio = (double) ruyi / plain;
        assertTrue(ratio > 1.8 && ratio < 2.2, "twice a resisted hit, was " + ratio);
    }

    @Test
    @DisplayName("Steel Dynasty: other Pokemon's abilities stop working while it is on the field; its own works and it cannot be lowered")
    void steelDynasty() {
        PbsData data = data();
        Pokemon mine = new Pokemon(data.species("GARCHOMP"), 50, data);
        mine.ability = "SANDVEIL";
        Pokemon foe = new Pokemon(data.species("BIDOOF"), 50, data);
        foe.ability = "STEELDYNASTY";
        Battle b = new Battle(data, new Random(3), (user, target, moves) -> 0).addPlayer(mine).addFoe(foe);
        assertTrue(b.player().hasActiveAbility("SANDVEIL"), "active before it enters");
        b.foe().pbEffectsOnSwitchIn(true);
        assertFalse(b.player().hasActiveAbility("SANDVEIL"), "suppressed while Steel Dynasty is out");
        assertTrue(b.foe().hasActiveAbility("STEELDYNASTY"), "its own ability still counts");
        assertFalse(b.foe().pbCanLowerStatStage(PBStats.ATTACK, b.player()), "its stats cannot be lowered");
    }
}
