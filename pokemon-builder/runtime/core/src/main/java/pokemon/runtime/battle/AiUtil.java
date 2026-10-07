package pokemon.runtime.battle;

import java.util.Random;

/**
 * Damage / speed / knock-out helpers for the trainer AI, ported from CFRU's
 * {@code ai_util.c} ({@code CanKnockOut}, {@code Can2HKO},
 * {@code MoveKnocksOutPossiblyGoesFirstWithBestAccuracy}, {@code IsStrongestMove},
 * {@code MoveWouldHitFirst}). Singles only.
 *
 * <p>Damage comes from {@link DamageCalc}, whose only random draw is the
 * {@code 85 + nextInt(16)} roll, so a stub {@link Random} yields the minimum
 * (roll 85), average (92) and maximum (100) without touching the real RNG.
 * {@code DamageCalc} writes several {@code damageState} fields on the target;
 * they are saved and restored here so a prediction never leaks into the battle.</p>
 */
final class AiUtil {

    private AiUtil() {
    }

    /** Minimum / average / maximum damage of one move against one target. */
    static final class Dmg {
        static final Dmg ZERO = new Dmg(0, 0, 0);
        final int min;
        final int avg;
        final int max;

        Dmg(int min, int avg, int max) {
            this.min = min;
            this.avg = avg;
            this.max = max;
        }
    }

    /** A {@link Random} whose {@code nextInt(16)} always answers a fixed roll. */
    private static final class FixedRoll extends Random {
        private final int roll;

        FixedRoll(int roll) {
            this.roll = roll;
        }

        @Override
        public int nextInt(int bound) {
            return Math.min(roll, bound - 1);
        }
    }

    /** Expected damage of {@code move} from {@code atk} to {@code def} (no crit, one target). */
    static Dmg damage(Battle battle, Battler atk, Battler def, BattleMove move) {
        if (move == null || move.statusMove() || battle.pbs() == null) {
            return Dmg.ZERO;
        }
        boolean disguise = def.damageState.disguise;
        boolean iceface = def.damageState.iceface;
        boolean flameveil = def.damageState.flameveil;
        boolean critical = def.damageState.critical;
        int calcDamage = def.damageState.calcDamage;
        int typeMod = def.damageState.typeMod;
        try {
            int min = DamageCalc.compute(atk, def, move, battle.pbs(), new FixedRoll(0));
            int avg = DamageCalc.compute(atk, def, move, battle.pbs(), new FixedRoll(7));
            int max = DamageCalc.compute(atk, def, move, battle.pbs(), new FixedRoll(15));
            return new Dmg(min, avg, max);
        } finally {
            def.damageState.disguise = disguise;
            def.damageState.iceface = iceface;
            def.damageState.flameveil = flameveil;
            def.damageState.critical = critical;
            def.damageState.calcDamage = calcDamage;
            def.damageState.typeMod = typeMod;
        }
    }

    /** The move's priority as the attack phase computes it (:156-173 pbCalculatePriority). */
    static int priority(Battle battle, Battler user, BattleMove move) {
        int pri = move.priority();
        if (user.abilityActive()) {
            pri = BattleHandlers.triggerPriorityChangeAbility(user.ability, user, move, pri);
        }
        return pri;
    }

    /**
     * {@code MoveWouldHitFirst}: {@code move} acts before the foe's best reply.
     * Equal speed counts as NOT first (the safe assumption).
     */
    static boolean goesFirst(Battle battle, Battler user, Battler foe, BattleMove move) {
        int mine = priority(battle, user, move);
        BattleMove reply = strongestMove(battle, foe, user);
        int theirs = reply == null ? 0 : priority(battle, foe, reply);
        if (mine != theirs) {
            return mine > theirs;
        }
        return outspeeds(battle, user, foe);
    }

    /** Faster, honouring Trick Room. */
    static boolean outspeeds(Battle battle, Battler user, Battler foe) {
        boolean trickRoom = battle.field.effects.intVal(PBEffects.Field.TrickRoom) > 0;
        return trickRoom ? user.speed() < foe.speed() : user.speed() > foe.speed();
    }

    /** Usable damaging move slots only (PP left, not blocked). */
    static boolean usable(Battle battle, Battler user, int slot) {
        return user.moveSlot(slot) != null && battle.pbCanChooseMove(user.index, slot, false, false);
    }

    /** The move of {@code atk} that deals the most average damage to {@code def}, or null. */
    static BattleMove strongestMove(Battle battle, Battler atk, Battler def) {
        BattleMove best = null;
        int bestDmg = 0;
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m == null || m.statusMove()) {
                continue;
            }
            // The foe's own moves are all considered (CFRU's AI reads the player's
            // moveset too); only our side filters on PP / restrictions.
            if (!atk.foe && !usableByPlayer(atk, i)) {
                continue;
            }
            if (atk.foe && !usable(battle, atk, i)) {
                continue;
            }
            int d = damage(battle, atk, def, m).avg;
            if (d > bestDmg) {
                bestDmg = d;
                best = m;
            }
        }
        return best;
    }

    private static boolean usableByPlayer(Battler atk, int slot) {
        return atk.moveSlotPp(slot) > 0 || atk.moveSlotMaxPp(slot) == 0;
    }

    /** {@code CanKnockOut}: any of {@code atk}'s moves is expected to faint {@code def}. */
    static boolean canKnockOut(Battle battle, Battler atk, Battler def) {
        return bestDamage(battle, atk, def) >= def.hp;
    }

    /** {@code Can2HKO}: two average hits of the best move faint {@code def}. */
    static boolean can2HKO(Battle battle, Battler atk, Battler def) {
        return bestDamage(battle, atk, def) * 2 >= def.hp;
    }

    /** Highest average damage over {@code atk}'s damaging moves (0 when none). */
    static int bestDamage(Battle battle, Battler atk, Battler def) {
        BattleMove m = strongestMove(battle, atk, def);
        return m == null ? 0 : damage(battle, atk, def, m).avg;
    }

    /** Percentage of max HP left, 0..100. */
    static int hpPercent(Battler b) {
        int max = b.maxHp();
        return max <= 0 ? 0 : b.hp * 100 / max;
    }

    /** How many non-fainted battlers sit on {@code battler}'s bench. */
    static int benchAlive(Battle battle, Battler battler) {
        int n = 0;
        for (Battler b : battle.partyOf(battler.index)) {
            if (b != battler && !b.fainted() && b.pokemon != null && !b.pokemon.egg) {
                n++;
            }
        }
        return n;
    }
}
