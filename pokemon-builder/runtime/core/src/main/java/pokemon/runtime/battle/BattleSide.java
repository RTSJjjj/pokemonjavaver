package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code PokeBattle_ActiveSide} (PokeBattle_ActiveField.rb:37-67) -
 * the effects attached to one side of the field (the player's side or the
 * opposing side), not to one battler. Scripts read them as
 * {@code @battle.sides[side].effects[...]}.
 *
 * <p>The indices come from the side group of {@code PBEffects}
 * (PBEffects.rb:191-213); they start again at 0 and collide with the battler and
 * position groups on purpose, so each side keeps its own {@link EffectMap} (see
 * the class comment of {@link PBEffects}).</p>
 *
 * <h2>Plugin quirk reproduced deliberately</h2>
 * <p>The last write of {@code initialize} is
 * {@code @effects[PBEffects::DelusionSong] = 0} (PokeBattle_ActiveField.rb:65).
 * {@code DelusionSong} is a BATTLER-group constant (172), so this writes the
 * side's effect array at index <b>172</b>, far outside the side-group range
 * 0..22 - the plugin's index spaces are mixed here.
 * {@link PBEffects.Side#DelusionSong} keeps 172 for exactly that reason and
 * this class's initialiser writes it at 172.</p>
 *
 * <p>Note the counted shape of the Ruby initialiser: it writes <b>24</b>
 * effect slots (PokeBattle_ActiveField.rb:42-65: the 23 side-group names at
 * :42-64 plus the battler-group {@code DelusionSong} at :65).</p>
 */
public final class BattleSide {

    /** {@code @effects = []} (PokeBattle_ActiveField.rb:41): the side-group effect indices. */
    public final EffectMap effects = new EffectMap();

    public BattleSide() {
        // PokeBattle_ActiveField.rb:42-65, one Ruby line per statement below.
        effects.set(PBEffects.Side.AuroraVeil, 0);            // :42
        effects.set(PBEffects.Side.CraftyShield, false);      // :43
        effects.set(PBEffects.Side.EchoedVoiceCounter, 0);    // :44
        effects.set(PBEffects.Side.EchoedVoiceUsed, false);   // :45
        effects.set(PBEffects.Side.LastRoundFainted, -1);     // :46
        effects.set(PBEffects.Side.LightScreen, 0);           // :47
        effects.set(PBEffects.Side.LuckyChant, 0);            // :48
        effects.set(PBEffects.Side.MatBlock, false);          // :49
        effects.set(PBEffects.Side.Mist, 0);                  // :50
        effects.set(PBEffects.Side.QuickGuard, false);        // :51
        effects.set(PBEffects.Side.Rainbow, 0);               // :52
        effects.set(PBEffects.Side.Reflect, 0);               // :53
        effects.set(PBEffects.Side.Round, false);             // :54
        effects.set(PBEffects.Side.Safeguard, 0);             // :55
        effects.set(PBEffects.Side.SeaOfFire, 0);             // :56
        effects.set(PBEffects.Side.Spikes, 0);                // :57
        effects.set(PBEffects.Side.StealthRock, false);       // :58
        effects.set(PBEffects.Side.StickyWeb, false);         // :59
        effects.set(PBEffects.Side.Swamp, 0);                 // :60
        effects.set(PBEffects.Side.Tailwind, 0);              // :61
        effects.set(PBEffects.Side.ToxicSpikes, 0);           // :62
        effects.set(PBEffects.Side.WideGuard, false);         // :63
        effects.set(PBEffects.Side.StickyWebUser, -1);        // :64
        effects.set(PBEffects.Side.DelusionSong, 0);          // :65 (battler-group 172, see class comment)
    }
}
