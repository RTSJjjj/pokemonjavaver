package pokemon.runtime.battle;

import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;

/**
 * Stage 4 / L1': the {@code BattleHandlers_Items.rb} registrations assigned to
 * this file by {@code stage4-l1-handler-roster.md} §1.2 - <b>31 entries</b>,
 * plus the 2 {@code .copy} lines of roster §5 that land here (§6: 31 + 2 copy
 * targets = 33 registered symbols).
 *
 * <p>Groups (roster §1.3): {@code DamageCalcTargetItem} 24,
 * {@code AccuracyCalcUserItem} 3, {@code CriticalCalcUserItem} 3,
 * {@code AccuracyCalcTargetItem} 1. All 31 are 可转 (roster §7.1: none 降级, none
 * 登记). The entries are grouped here by group, not by line range: the roster
 * assigns a group to a file as a whole (§1.2), and the source file interleaves
 * the groups (e.g. {@code AccuracyCalcUserItem} is at {@code :427-450} while
 * {@code DamageCalcTargetItem} is at {@code :854-1008} plus {@code :1749-1756}).</p>
 *
 * <h2>Translations</h2>
 * <ul>
 * <li>{@code isConst?(val,PBTypes,:X)} = {@code "X".equals(val)}: this runtime
 *     identifies a type by its internal name ({@link BattleHandlers}' javadoc).</li>
 * <li>Move predicates go through the movefx strategy layer
 *     ({@code MoveEffectRegistry.of(move.function())}), the L1' convention: the
 *     un-registered function codes fall back to
 *     {@code PokeBattle_UnimplementedMove}, whose base class carries the
 *     un-overridden {@code PokeBattle_Move} bodies. {@link #fx} is the single
 *     lookup point.</li>
 * <li>{@code pbBattleTypeWeakingBerry(type,moveType,target,mults)} is the helper
 *     of the same name in {@link BattleHandlerHelpers}.</li>
 * <li>{@code mults} is {@code float[]} ({@link BattleHandlers}' javadoc), so the
 *     plugin's {@code *= 1.5} is {@code *= 1.5f} and nothing is truncated.</li>
 * <li>{@code target.effects[PBEffects::Transform]} is read with
 *     {@link EffectMap#truthy(int)} (the slot is a boolean;
 *     {@code EffectMap}'s javadoc).</li>
 * </ul>
 *
 * <h2>Registered plugin defect (found while transcribing, Lead-confirmed)</h2>
 * <p>{@code AccuracyCalcUserItem/CRAFTMIND} ({@code :433-441}) declares
 * {@code |item,user,target,move,type,baseacc|}, one parameter MORE than the
 * trigger passes ({@code (item,mods,user,target,move,type)}), so every local is
 * shifted by one: the local {@code move} receives the Battler target and the
 * local {@code baseacc} the String type. {@code :435 move.id == :WARTESTRIKE}
 * (no {@code id} on a Battler) and {@code :436 baseacc * 1.2} (String * Float)
 * both raise at runtime. The proc contains only {@code next <value>} statements
 * and has no side effect at all, while
 * {@link BattleHandlers.AccuracyCalcUserItem} is {@code void} - the return value
 * is discarded - so an empty body is exactly what the plugin's own behaviour
 * amounts to. The call shape is registered rather than "fixed": the parameter
 * list is not corrected and no default value is invented.</p>
 *
 * <p>The load order is fixed by {@link BattleHandlerRegistry} and must not be
 * changed here.</p>
 */
final class ItemsDamageTarget {

    private ItemsDamageTarget() {
    }

    /** {@code BattleHandlers_Items.rb}: 31 entries (roster §1.2, §2 main table). */
    static void register() {
        // ==================================================================
        // AccuracyCalcUserItem (:427-450)
        // ==================================================================

        BattleHandlers.AccuracyCalcUserItem.add("WIDELENS", (item, mods, user, target, move, type) -> {
            // BattleHandlers_Items.rb:427-432
            mods[BattleHandlers.ACC_MULT] *= 1.1f;                           // :429
        });

        BattleHandlers.AccuracyCalcUserItem.add("CRAFTMIND", (item, mods, user, target, move, type) -> {
            // BattleHandlers_Items.rb:433-441
            // 登记: 插件缺陷（roster §4 之外，Lead 已确认）—— :434 的 proc 形参表
            // |item,user,target,move,type,baseacc| 比 trigger 实参
            // (item,mods,user,target,move,type) 多一位、整体错位：局部 move 收到 Battler
            // target、局部 baseacc 收到 String type。:435 `move.id == :WARTESTRIKE`（Battler
            // 无 id）与 :436 `baseacc * 1.2`（String * Float）两处都会运行时崩。
            // 该 proc 只有 `next 值`、无任何副作用，而 AccuracyCalcUserItem 接口是 void
            // （返回值被丢弃）⇒ 空体即逐行等价。不修正形参、不造替代实现。
        });

        BattleHandlers.AccuracyCalcUserItem.add("ZOOMLENS", (item, mods, user, target, move, type) -> {
            // BattleHandlers_Items.rb:442-455
            // @battle.choices[i][0] is :UseMove/:Shift (Battle#choices assembles the
            // Ruby array on demand; Battle.java:2590).
            String action = (String) target.battle.choices(target.index)[0]; // :444-445
            if ((!":UseMove".equals(action) && !":Shift".equals(action))     // :444-445
                    || target.movedThisRound()) {            // :446 target.movedThisRound?
                mods[BattleHandlers.ACC_MULT] *= 1.2f;                       // :447
            }
        });

        // ==================================================================
        // AccuracyCalcTargetItem (:456-462)
        // ==================================================================

        BattleHandlers.AccuracyCalcTargetItem.add("BRIGHTPOWDER", (item, mods, user, target, move, type) -> {
            // BattleHandlers_Items.rb:456-461
            mods[BattleHandlers.ACC_MULT] *= 0.9f;                           // :458
        });
        BattleHandlers.AccuracyCalcTargetItem.copy("BRIGHTPOWDER", "LAXINCENSE"); // :462

        // ==================================================================
        // DamageCalcTargetItem (:854-1008, :1749-1756)
        // ==================================================================

        BattleHandlers.DamageCalcTargetItem.add("ASSAULTVEST", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:854-859
            if (fx(move).specialMove(move, null)) {                          // :856 move.specialMove?
                mults[BattleHandlers.DEF_MULT] *= 1.5f;                      // :856
            }
        });

        BattleHandlers.DamageCalcTargetItem.add("BABIRIBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:860-865
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("STEEL", type, target, mults);   // :862
        });

        BattleHandlers.DamageCalcTargetItem.add("CHARTIBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:866-871
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("ROCK", type, target, mults);    // :868
        });

        BattleHandlers.DamageCalcTargetItem.add("CHILANBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:872-877
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("NORMAL", type, target, mults);  // :874
        });

        BattleHandlers.DamageCalcTargetItem.add("CHOPLEBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:878-883
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("FIGHTING", type, target, mults); // :880
        });

        BattleHandlers.DamageCalcTargetItem.add("COBABERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:884-889
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("FLYING", type, target, mults);  // :886
        });

        BattleHandlers.DamageCalcTargetItem.add("COLBURBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:890-895
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("DARK", type, target, mults);    // :892
        });

        BattleHandlers.DamageCalcTargetItem.add("DEEPSEASCALE", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:896-903
            if (target.isSpecies("CLAMPERL") && fx(move).specialMove(move, null)) { // :898
                mults[BattleHandlers.DEF_MULT] *= 2f;                        // :899
            }
        });

        BattleHandlers.DamageCalcTargetItem.add("EVIOLITE", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:904-914
            // :906-909 NOTE (copied): Eviolite cares about whether the Pokemon itself
            //         can evolve, which means it also cares about the Pokemon's form.
            //         Some forms cannot evolve even if the species generally can, and
            //         such forms are not affected by Eviolite.
            java.util.List<Object[]> evos = PendingApi.pbGetEvolvedFormData(target, true); // :910
            if (evos != null && evos.size() > 0) {                           // :911 evos && evos.length>0
                mults[BattleHandlers.DEF_MULT] *= 1.5f;                      // :911
            }
        });

        BattleHandlers.DamageCalcTargetItem.add("HABANBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:915-920
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("DRAGON", type, target, mults);  // :917
        });

        BattleHandlers.DamageCalcTargetItem.add("KASIBBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:921-926
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("GHOST", type, target, mults);   // :923
        });

        BattleHandlers.DamageCalcTargetItem.add("KEBIABERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:927-932
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("POISON", type, target, mults);  // :929
        });

        BattleHandlers.DamageCalcTargetItem.add("METALPOWDER", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:933-940
            if (target.isSpecies("DITTO")
                    && !target.effects.truthy(PBEffects.Battler.Transform)) { // :935
                mults[BattleHandlers.DEF_MULT] *= 1.5f;                      // :936
            }
        });

        BattleHandlers.DamageCalcTargetItem.add("OCCABERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:941-946
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("FIRE", type, target, mults);    // :943
        });

        BattleHandlers.DamageCalcTargetItem.add("PASSHOBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:947-952
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("WATER", type, target, mults);   // :949
        });

        BattleHandlers.DamageCalcTargetItem.add("PAYAPABERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:953-958
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("PSYCHIC", type, target, mults); // :955
        });

        BattleHandlers.DamageCalcTargetItem.add("RINDOBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:959-964
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("GRASS", type, target, mults);   // :961
        });

        BattleHandlers.DamageCalcTargetItem.add("ROSELIBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:965-970
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("FAIRY", type, target, mults);   // :967
        });

        BattleHandlers.DamageCalcTargetItem.add("SHUCABERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:971-976
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("GROUND", type, target, mults);  // :973
        });

        BattleHandlers.DamageCalcTargetItem.add("SOULDEW", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:977-986
            if (Battle.NEWEST_BATTLE_MECHANICS) {
                return;                                                      // :979 next if NEWEST_BATTLE_MECHANICS
            }
            if (!target.isSpecies("LATIAS") && !target.isSpecies("LATIOS")) {
                return;                                                      // :980
            }
            if (fx(move).specialMove(move, null)                             // :981 move.specialMove?
                    && !PendingApi.battleRules(user.battle, "souldewclause")) { // :981 battle.rules["souldewclause"]
                mults[BattleHandlers.DEF_MULT] *= 1.5f;                      // :982
            }
        });

        BattleHandlers.DamageCalcTargetItem.add("TANGABERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:987-992
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("BUG", type, target, mults);     // :989
        });

        BattleHandlers.DamageCalcTargetItem.add("WACANBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:993-998
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("ELECTRIC", type, target, mults); // :995
        });

        BattleHandlers.DamageCalcTargetItem.add("YACHEBERRY", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:999-1008
            BattleHandlerHelpers.pbBattleTypeWeakingBerry("ICE", type, target, mults);     // :1001
        });

        BattleHandlers.DamageCalcTargetItem.add("SQHL", (item, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Items.rb:1749-1756 (圣灵纹章)
            if (target.isSpecies("SUGARDEVOIR")
                    && !target.effects.truthy(PBEffects.Battler.Transform)) { // :1751
                mults[BattleHandlers.DEF_MULT] *= 1.5f;                      // :1752
            }
        });

        // ==================================================================
        // CriticalCalcUserItem (:1009-1038)
        // ==================================================================

        BattleHandlers.CriticalCalcUserItem.add("LUCKYPUNCH", (item, user, target, c) -> {
            // BattleHandlers_Items.rb:1009-1014
            if (user.isSpecies("CHANSEY")) {                                 // :1011
                return c + 2;                                                // :1011
            }
            return null;                                                     // :1011 no `next` -> Ruby nil
        });

        BattleHandlers.CriticalCalcUserItem.add("RAZORCLAW", (item, user, target, c) -> {
            // BattleHandlers_Items.rb:1015-1022
            return c + 1;                                                    // :1017
        });
        BattleHandlers.CriticalCalcUserItem.copy("RAZORCLAW", "SCOPELENS");  // :1021

        BattleHandlers.CriticalCalcUserItem.add("LEEK", (item, user, target, c) -> {
            // BattleHandlers_Items.rb:1023-1038
            if (user.isSpecies("FARFETCHD") || user.isSpecies("SIRFETCHD")) { // :1025
                return c + 2;                                                // :1025
            }
            return null;                                                     // :1025 no `next` -> Ruby nil
        });
    }

    /**
     * The move's effect strategy ({@code PokeBattle_Move.pbFromPBMove}); the L1'
     * convention is to ask the strategy for every move predicate, so an
     * un-registered function code falls back to
     * {@code PokeBattle_UnimplementedMove} and inherits the
     * {@link pokemon.runtime.battle.movefx.MoveEffectBase} bodies of
     * {@code PokeBattle_Move}.
     */
    private static MoveEffect fx(BattleMove move) {
        return MoveEffectRegistry.of(move.function());
    }
}
