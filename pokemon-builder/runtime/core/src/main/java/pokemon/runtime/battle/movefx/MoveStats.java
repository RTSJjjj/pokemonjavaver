package pokemon.runtime.battle.movefx;

/**
 * One pair of return values of the plugin's two multi-value move hooks.
 *
 * <p>Ruby has no multi-value return; it has "return a, b" sugar, which the two
 * stat hooks use:</p>
 * <ul>
 * <li>{@code pbGetAttackStats} - {@code Move_Usage_Calculations.rb:238-243}
 *     <pre>{@code
 * def pbGetAttackStats(user,target)
 *   if specialMove?
 *     return user.spatk, user.stages[PBStats::SPATK]+6
 *   end
 *   return user.attack, user.stages[PBStats::ATTACK]+6
 * end}</pre></li>
 * <li>{@code pbGetDefenseStats} - {@code Move_Usage_Calculations.rb:245-250}
 *     <pre>{@code
 * def pbGetDefenseStats(user,target)
 *   if specialMove?
 *     return target.spdef, target.stages[PBStats::SPDEF]+6
 *   end
 *   return target.defense, target.stages[PBStats::DEFENSE]+6
 * end}</pre></li>
 * </ul>
 *
 * <p>The overrides also destructure the pair, which pins the order down
 * ({@code Move_Effects_080-0FF:1140-1143}: {@code ret1, _ret2 = super} then
 * {@code return ret1, 6}), so element 1 is the stat and element 2 is the
 * {@code +6}-shifted stat stage. Task-11 decision 1: a small value object
 * instead of an {@code int[]}.</p>
 */
public final class MoveStats {

    /** The stat itself ({@code user.spatk}/{@code user.attack}/... in Ruby). */
    public final int value;

    /** The stat stage shifted by {@code +6} (Ruby's {@code stages[...]+6}, 0..12). */
    public final int stage;

    public MoveStats(int value, int stage) {
        this.value = value;
        this.stage = stage;
    }
}
