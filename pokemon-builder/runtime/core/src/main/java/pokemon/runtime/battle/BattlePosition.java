package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code PokeBattle_ActivePosition} (PokeBattle_ActiveField.rb:71-86) -
 * the effects attached to one battle <em>position</em> rather than to one
 * battler. The plugin keeps one of these per position in
 * {@code @positions} on {@code PokeBattle_Battle} and scripts read them as
 * {@code @battle.positions[i].effects[...]} (e.g. Wish, Future Sight).
 *
 * <p>The nine indices come from the position group of
 * {@code PBEffects} (PBEffects.rb:178-186); the numbers overlap the battler and
 * side groups on purpose, so the positions array must keep its own
 * {@link EffectMap} (see the class comment of {@link PBEffects}).</p>
 */
public final class BattlePosition {

    /** {@code @effects = []} (PokeBattle_ActiveField.rb:75): the position-group effect indices. */
    public final EffectMap effects = new EffectMap();

    public BattlePosition() {
        // PokeBattle_ActiveField.rb:76-84, one Ruby line per statement below.
        effects.set(PBEffects.Position.FutureSightCounter, 0);        // :76
        effects.set(PBEffects.Position.FutureSightMove, 0);           // :77
        effects.set(PBEffects.Position.FutureSightUserIndex, -1);     // :78
        effects.set(PBEffects.Position.FutureSightUserPartyIndex, -1); // :79
        effects.set(PBEffects.Position.HealingWish, false);           // :80
        effects.set(PBEffects.Position.LunarDance, false);            // :81
        effects.set(PBEffects.Position.Wish, 0);                      // :82
        effects.set(PBEffects.Position.WishAmount, 0);                // :83
        effects.set(PBEffects.Position.WishMaker, -1);                // :84
    }
}
