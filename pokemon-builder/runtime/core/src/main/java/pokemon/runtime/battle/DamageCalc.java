package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;

import java.util.Random;

/**
 * Stage 3 / P2: the standard Essentials singles damage formula:
 * {@code base = floor(floor(floor((2*level/5 + 2) * power * A / D) / 50) + 2)},
 * then STAB, type effectiveness and the 0.85..1.0 random factor. Critical hits,
 * abilities and the {@code Move_Effects_*} branches are deliberately left to the
 * plugin features (P5); the arithmetic matches the project for a normal hit.
 */
public final class DamageCalc {

    private DamageCalc() {
    }

    /** Damage dealt (0 when immune or a status move); never negative. */
    public static int compute(Battler user, Battler target, BattleMove move, PbsData pbs,
                              Random random) {
        if (move.statusMove() || move.power() <= 0) {
            return 0;
        }
        boolean physical = move.physical();
        int attack = physical ? user.attack() : user.spAtk();
        int defense = physical ? target.defense() : target.spDef();
        if (defense <= 0) {
            defense = 1;
        }
        double base = Math.floor(Math.floor(
                Math.floor((2.0 * user.level() / 5 + 2) * move.power() * attack / defense) / 50)
                + 2);
        float typeMod = pbs == null ? 1f : pbs.effectiveness(move.type(), target.types());
        if (typeMod <= 0f) {
            return 0;
        }
        double modifier = typeMod;
        if (user.hasType(move.type())) {
            modifier *= 1.5; // STAB
        }
        modifier *= 0.85 + random.nextInt(16) / 100.0;
        return Math.max(1, (int) Math.floor(base * modifier));
    }
}
