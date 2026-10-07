package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code PokeBattle_ActiveField} (PokeBattle_ActiveField.rb:1-33) -
 * the whole battle field: the field-wide effects
 * ({@code @battle.field.effects[...]}), the current weather and terrain, and
 * (in this runtime, see below) the two sides and the two positions.
 *
 * <p>The effect indices come from the field group of {@code PBEffects}
 * (PBEffects.rb:219-232). They start at 0 like the battler/side/position groups,
 * so the field keeps its own {@link EffectMap} (see the class comment of
 * {@link PBEffects}).</p>
 *
 * <h2>Plugin quirk reproduced deliberately</h2>
 * <p>{@code PBEffects::NeutralizingGas} is index 13 (PBEffects.rb:232) but the
 * Ruby initialiser stops at {@code WonderRoom} (index 12,
 * PokeBattle_ActiveField.rb:25), so index 13 is <b>never written</b> and reads
 * back as Ruby {@code nil} until something sets it. This class therefore
 * deliberately does not initialise it either - do not "fix" that by adding a
 * {@code false} (truthiness of {@code nil} and of {@code false} happen to agree,
 * but {@link EffectMap#has(int)} would start lying).</p>
 *
 * <h2>Where the sides and positions live in this runtime</h2>
 * <p>In the plugin {@code @sides} and {@code @positions} are members of
 * {@code PokeBattle_Battle}, not of {@code PokeBattle_ActiveField}; scripts read
 * {@code @battle.sides[i]} / {@code @battle.positions[i]}. {@code Battle.java}
 * is owned by another agent in this round and must not be touched, so the two
 * arrays are parked here for now. When {@code Battle} is wired up, decide
 * whether they should move (or be exposed as views) - the effect maps themselves
 * are unchanged either way.</p>
 */
public final class BattleField {

    /** {@code @effects = []} (PokeBattle_ActiveField.rb:12): the field-group effect indices. */
    public final EffectMap effects = new EffectMap();

    // --- Weather / terrain, PokeBattle_ActiveField.rb:26-31 ---

    /** {@code @defaultWeather} (PokeBattle_ActiveField.rb:26). */
    public int defaultWeather = PBWeather.None;
    /** {@code @weather} (PokeBattle_ActiveField.rb:27). */
    public int weather = PBWeather.None;
    /** {@code @weatherDuration} (PokeBattle_ActiveField.rb:28). */
    public int weatherDuration = 0;
    /** {@code @defaultTerrain} (PokeBattle_ActiveField.rb:29). */
    public int defaultTerrain = PBBattleTerrains.None;
    /** {@code @terrain} (PokeBattle_ActiveField.rb:30). */
    public int terrain = PBBattleTerrains.None;
    /** {@code @terrainDuration} (PokeBattle_ActiveField.rb:31). */
    public int terrainDuration = 0;

    // --- Parked here from PokeBattle_Battle, see the class comment above ---

    /**
     * {@code @battle.sides[i]} in the plugin ({@code PokeBattle_Battle}); parked
     * on the field here so {@code Battle.java} stays untouched this round.
     */
    public final BattleSide[] sides = {new BattleSide(), new BattleSide()};

    /**
     * {@code @battle.positions[i]} in the plugin ({@code PokeBattle_Battle});
     * parked on the field here so {@code Battle.java} stays untouched this round.
     */
    public final BattlePosition[] positions = {new BattlePosition(), new BattlePosition()};

    public BattleField() {
        // PokeBattle_ActiveField.rb:13-25, one Ruby line per statement below.
        effects.set(PBEffects.Field.AmuletCoin, false);       // :13
        effects.set(PBEffects.Field.FairyLock, 0);            // :14
        effects.set(PBEffects.Field.FusionBolt, false);       // :15
        effects.set(PBEffects.Field.FusionFlare, false);      // :16
        effects.set(PBEffects.Field.Gravity, 0);              // :17
        effects.set(PBEffects.Field.HappyHour, false);        // :18
        effects.set(PBEffects.Field.IonDeluge, false);        // :19
        effects.set(PBEffects.Field.MagicRoom, 0);            // :20
        effects.set(PBEffects.Field.MudSportField, 0);        // :21
        effects.set(PBEffects.Field.PayDay, 0);               // :22
        effects.set(PBEffects.Field.TrickRoom, 0);            // :23
        effects.set(PBEffects.Field.WaterSportField, 0);      // :24
        effects.set(PBEffects.Field.WonderRoom, 0);           // :25
        // PBEffects.Field.NeutralizingGas (13) is NOT initialised - the Ruby
        // initialiser never writes it. See the class comment.
    }
}
