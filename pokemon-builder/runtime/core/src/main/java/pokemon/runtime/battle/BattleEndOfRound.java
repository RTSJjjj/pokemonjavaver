package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;

import java.util.function.Consumer;

/**
 * Stage 4 / &sect;4 B4 (wiring): the end-of-round phase,
 * {@code Battle_Phase_EndOfRound} (861 lines).
 *
 * <h2>Why this class exists</h2>
 * {@code Battle#endOfTurn} was a 20-line approximation (turn counter, poison, burn,
 * flinch reset). The plugin's {@code pbEndOfRoundPhase} (:211-829) runs <b>60+ ordered
 * stages</b>; the roster and the per-stage coverage are tabulated in
 * {@code docs/stage4-eor-roster.md}. This class carries the stages as they are
 * transcribed, so the battle flow only needs insert-only call sites.
 *
 * <h2>Transcribed in this pass (EOR-1 + EOR-3)</h2>
 * <ul>
 * <li>the counter helpers {@code pbEORCountDownBattlerEffect} (:5-11) and
 *     {@code pbEORCountDownFieldEffect} (:20-30) - the side variant (:13-18) needs a
 *     by-index sides accessor this runtime does not expose yet</li>
 * <li>the healing stage (:287-326): Grassy Terrain, <b>{@code triggerEORHealingAbility}
 *     (:297)</b> and <b>{@code triggerEORHealingItem} (:298)</b> - Leftovers, Black
 *     Sludge, Healer, Hydration, Shed Skin - then Operation (:301), Aqua Ring (:309)
 *     and Ingrain (:318), both with Big Root's x1.3</li>
 * </ul>
 *
 * <h2>Still to transcribe (see the roster)</h2>
 * Leech Seed (:327) · weather ({@code pbEORWeather} :35-111) and terrain (:115-148) ·
 * the remaining damage stages (:345-527) · the countdown stages (:528-702) · the
 * effect/ability stage (:706-829, the three remaining EOR triggers) · the side/field
 * countdown groups (:799-822) · Neutralizing Gas (:823).
 */
public final class BattleEndOfRound {

    private BattleEndOfRound() {
    }

    /**
     * {@code pbEORCountDownBattlerEffect(priority,effect)} (:5-11). Decrements the
     * effect counter of every healthy battler and runs {@code onZero} on the battler
     * whose counter just reached 0 (Ruby's {@code yield b if ...}).
     */
    public static void pbEORCountDownBattlerEffect(Array<Battler> priority, int effect,
                                                   Consumer<Battler> onZero) {
        for (Battler b : priority) {
            if (b.fainted() || b.effects.intVal(effect) == 0) {          // :7
                continue;
            }
            b.effects.decrement(effect);                                 // :8
            if (onZero != null && b.effects.intVal(effect) == 0) {       // :9
                onZero.accept(b);
            }
        }
    }

    /**
     * {@code pbEORCountDownFieldEffect(effect,msg)} (:20-30). The plugin's
     * {@code pbItemTerrainStatBoostCheck} re-check when Magic Room ends (:26) is
     * registered: the runtime has no such helper.
     */
    public static void pbEORCountDownFieldEffect(Battle battle, int effect, String msg) {
        BattleEndOfRoundPhase.pbEORCountDownFieldEffect(battle, effect, msg);
    }

    /**
     * The healing stage of {@code pbEndOfRoundPhase} (:287-326), called once per
     * end of round with the fielded battlers in speed order.
     */
    public static void pbEORHealing(Battle battle, Array<Battler> priority) {
        // :288-300 status-curing effects/abilities and HP-healing items
        for (Battler b : priority) {
            if (b.fainted()) {                                           // :289
                continue;
            }
            // :291-295 Grassy Terrain heals 1/16 for a grounded battler
            if (battle.field.terrain == PBBattleTerrains.Grassy
                    && b.affectedByTerrain() && b.canHeal()) {
                b.pbRecoverHP(b.maxHp() / 16);                           // :293
                battle.display(b.pbThis() + "的HP回复了。");             // :294
            }
            // :297 Healer, Hydration, Shed Skin
            if (b.abilityActive()) {
                BattleHandlers.triggerEORHealingAbility(b.ability, b, battle);
            }
            // :299 Black Sludge, Leftovers
            if (b.itemActive()) {
                BattleHandlers.triggerEORHealingItem(b.item, b, battle);
            }
        }
        // :301-308 手术
        for (Battler b : priority) {
            if (!b.effects.truthy(PBEffects.Battler.Operation)) {        // :303
                continue;
            }
            if (!b.canHeal()) {                                          // :304
                continue;
            }
            b.pbRecoverHP(b.maxHp() / 16);                               // :306
            battle.display("手术恢复了" + b.pbThis(true) + "的HP！");    // :307
        }
        // :309-317 Aqua Ring
        for (Battler b : priority) {
            if (!b.effects.truthy(PBEffects.Battler.AquaRing)) {          // :311
                continue;
            }
            if (!b.canHeal()) {                                          // :312
                continue;
            }
            int hpGain = b.maxHp() / 16;                                 // :313
            if (b.hasActiveItem("BIGROOT")) {                            // :314
                hpGain = (int) Math.floor(hpGain * 1.3f);
            }
            b.pbRecoverHP(hpGain);                                       // :315
            battle.display("水流环恢复了" + b.pbThis(true) + "的HP！");  // :316
        }
        // :318-326 Ingrain
        for (Battler b : priority) {
            if (!b.effects.truthy(PBEffects.Battler.Ingrain)) {           // :320
                continue;
            }
            if (!b.canHeal()) {                                          // :321
                continue;
            }
            int hpGain = b.maxHp() / 16;                                 // :322
            if (b.hasActiveItem("BIGROOT")) {                            // :323
                hpGain = (int) Math.floor(hpGain * 1.3f);
            }
            b.pbRecoverHP(hpGain);                                       // :324
            battle.display(b.pbThis() + "通过根吸取了营养！");           // :325
        }
    }

    /**
     * The field-effect countdowns of {@code pbEndOfRoundPhase} (:685-701), in the
     * plugin's order. Each ends with its own message; Magic Room additionally re-checks
     * the terrain-boost items, which is registered inside
     * {@link #pbEORCountDownFieldEffect}.
     */
    public static void pbEORFieldCountdowns(Battle battle) {
        pbEORCountDownFieldEffect(battle, PBEffects.Field.TrickRoom,       // :685-686
                "扭曲的时空恢复正常了！");
        pbEORCountDownFieldEffect(battle, PBEffects.Field.Gravity,         // :687-688
                "重力恢复正常了！");
        pbEORCountDownFieldEffect(battle, PBEffects.Field.WaterSportField,  // :690-691
                "玩水的效果消失了。");
        pbEORCountDownFieldEffect(battle, PBEffects.Field.MudSportField,    // :693-694
                "玩泥巴的效果消失了。");
        pbEORCountDownFieldEffect(battle, PBEffects.Field.WonderRoom,       // :696-697
                "奇妙空间消失了！\n防御与特防恢复正常了！");
        pbEORCountDownFieldEffect(battle, PBEffects.Field.MagicRoom,        // :699-700
                "魔法空间消失了！\n携带道具的效果恢复正常了！");
        // :702-703 end of terrains (pbEORTerrain) - queued separately (EOR-2)
    }

    /**
     * The per-battler effect stage of {@code pbEndOfRoundPhase} (:725-738):
     * Slow Start's end message, then the three remaining EOR handler tables.
     *
     * <p>Call it after the damage stages (poison :390 / burn :423) - the plugin's own
     * position - and before {@code endOfRoundZa}.</p>
     */
    public static void pbEOREffect(Battle battle, Array<Battler> priority) {
        for (Battler b : priority) {
            // :726-731 Slow Start's end message
            if (b.effects.intVal(PBEffects.Battler.SlowStart) > 0) {
                b.effects.decrement(PBEffects.Battler.SlowStart);        // :727
                if (b.effects.intVal(PBEffects.Battler.SlowStart) == 0) {  // :728
                    battle.display(b.pbThis() + "聚集了所有的力量！");     // :729
                }
            }
            // :732-733 Bad Dreams, Moody, Speed Boost
            if (b.abilityActive()) {
                BattleHandlers.triggerEOREffectAbility(b.ability, b, battle);
            }
            // :734-735 Flame Orb, Sticky Barb, Toxic Orb
            if (b.itemActive()) {
                BattleHandlers.triggerEOREffectItem(b.item, b, battle);
            }
            // :736-737 Harvest, Pickup
            if (b.abilityActive()) {
                BattleHandlers.triggerEORGainItemAbility(b.ability, b, battle);
            }
        }
    }
}
