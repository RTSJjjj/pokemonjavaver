package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;

/**
 * Stage 4 / &sect;4 (wiring): the per-hit and end-of-move effect handlers of
 * {@code Battler_UseMove_TriggerEffects}, translated line by line.
 *
 * <h2>Why a separate class</h2>
 * The plugin puts these on {@code PokeBattle_Battler} ({@code pbEffectsOnMakingHit},
 * {@code pbEffectsAfterMove}, {@code pbEffectsAfterMove2}) but calls them from the
 * move flow ({@code Battler_UseMove:741} inside the per-hit loop and
 * {@code Battler_UseMove:544} once the move is over). This runtime keeps the
 * already-ported {@link Battler} untouched and hangs these three methods on a
 * static helper instead, so the battle flow can call them from
 * {@code Battle#execute} without editing that class's existing lines.
 *
 * <h2>The class is reopened in the plugin</h2>
 * {@code Arceus:181-210} reopens the class and overrides {@code pbEffectsAfterMove}
 * with {@code alias __pla__pbEffectsAfterMove pbEffectsAfterMove} - the same
 * "later section wins, layered through the alias" pattern as the move-effect
 * classes. The effective method is the {@code Arceus} body (un-drowse /
 * de-frostbite) which then calls the aliased base body; {@link #pbEffectsAfterMove}
 * below is that whole chain, in order.
 *
 * <h2>What this wires</h2>
 * Eight handler tables that had no call site at all before:
 * {@code TargetAbilityOnHit}, {@code UserAbilityOnHit}, {@code TargetItemOnHit},
 * {@code UserAbilityEndOfMove}, {@code TargetItemAfterMoveUse},
 * {@code ItemOnStatLoss}, {@code UserItemAfterMoveUse},
 * {@code TargetAbilityAfterMoveUse}.
 *
 * <h2>Registered gaps (see the {@code // 登记:} notes inline)</h2>
 * <ul>
 * <li>{@code @battle.battleBond} (Greninja's Battle Bond) - the table is not modelled.</li>
 * <li>{@code @battle.pbJudgeCheckpoint} - no judge/checkpoint subsystem.</li>
 * <li>{@code pbEffectsOnSwitchIn} on the battler - not ported onto {@link Battler} yet.</li>
 * </ul>
 */
public final class BattlerHitEffects {

    private BattlerHitEffects() {
    }

    /** The strategy for this move's function code (never null; unknown codes get the unimplemented effect). */
    private static MoveEffect effect(BattleMove move) {
        return MoveEffectRegistry.of(move.function());
    }

    /**
     * {@code pbEffectsOnMakingHit(move,user,target)}
     * (Battler_UseMove_TriggerEffects:5-65), called once per hit that dealt
     * damage ({@code Battler_UseMove:741}).
     */
    public static void pbEffectsOnMakingHit(Battle battle, BattleMove move, Battler user, Battler target) {
        // :6 if target.damageState.calcDamage>0 && !target.damageState.substitute
        if (target.damageState.calcDamage > 0 && !target.damageState.substitute) {
            // :8-12 Target's ability (Poison Touch, ...) - can hurt the user, hence the HP check.
            if (target.abilityActive(true)) {
                int oldHP = user.hp;                                              // :9
                BattleHandlers.triggerTargetAbilityOnHit(target.ability, user, target, move, battle);   // :10
                if (user.hp < oldHP) {                                            // :11
                    user.pbItemHPHealCheck(0, false);                             // :11 (Ruby defaults)
                }
            }
            // :14-17 User's ability (e.g. Poison Point on the user's side).
            if (user.abilityActive(true)) {
                BattleHandlers.triggerUserAbilityOnHit(user.ability, user, target, move, battle);      // :15
                user.pbItemHPHealCheck(0, false);                                 // :16
            }
            // :19-26 Target's item.
            if (target.itemActive(true)) {
                int oldHP = user.hp;                                              // :20
                // :21-23 Burning Jealousy (function 18B) burns a target that has the effect.
                if ("18B".equals(move.function()) && target.pbCanBurn(user, false, move)
                        && target.effects.truthy(PBEffects.Battler.BurningJealousy)
                        && !target.damageState.substitute) {
                    target.pbBurn(user, null);                                    // :22
                }
                BattleHandlers.triggerTargetItemOnHit(target.item, user, target, move, battle);        // :24
                if (user.hp < oldHP) {                                            // :25
                    user.pbItemHPHealCheck(0, false);                             // :25
                }
            }
        }
        // :28 if target.opposes?(user)
        if (target.opposes(user)) {
            // :30-35 Rage
            if (target.effects.truthy(PBEffects.Battler.Rage) && !target.fainted()) {
                if (target.pbCanRaiseStatStage(PBStats.ATTACK, target)) {         // :31
                    battle.display(target.thisName() + "的怒火正在积聚！");        // :32
                    target.pbRaiseStatStage(PBStats.ATTACK, 1, target);           // :33
                }
            }
            // :37-42 Beak Blast: a contact hit burns the attacker.
            if (target.effects.truthy(PBEffects.Battler.BeakBlast)) {
                // :38 PBDebug.log("[Lingering effect] ...") - debug log, not modelled.
                if (effect(move).pbContactMove(move, user) && user.affectedByContactEffect(false)) {
                    if (target.pbCanBurn(user, false, move)) {                    // :40
                        target.pbBurn(user, null);                                // :40
                    }
                }
            }
            // :44-51 Shell Trap: make the trapper move next when the trap triggered.
            if (target.effects.truthy(PBEffects.Battler.ShellTrap)
                    && ":UseMove".equals(battle.choices(target.index)[0])         // :45
                    && !target.movedThisRound()) {                                // :45
                if (target.damageState.hpLost > 0 && !target.damageState.substitute
                        && move.physical()) {                                     // :46
                    target.tookPhysicalHit = true;                                // :47
                    target.effects.set(PBEffects.Battler.MoveNext, true);         // :48
                    target.effects.set(PBEffects.Battler.Quash, 0);               // :49
                }
            }
            // :53-57 Grudge: the target took the move down with it, so the user's
            // move loses all its PP. The plugin writes `move.pp = 0`; this runtime
            // keeps current PP in the Pokemon's move slot, so the same value is
            // written through the pbSetPP bridge.
            if (target.effects.truthy(PBEffects.Battler.Grudge) && target.fainted()) {
                user.pbSetPP(move.internalName(), 0);                             // :54
                battle.display(user.thisName() + "的" + move.name() + "因为怨恨失去了所有的PP！");   // :55-56
            }
            // :59-63 Destiny Bond: record that it should apply.
            if (target.effects.truthy(PBEffects.Battler.DestinyBond) && target.fainted()) {
                if (user.effects.intVal(PBEffects.Battler.DestinyBondTarget, -1) < 0) {
                    user.effects.set(PBEffects.Battler.DestinyBondTarget, target.index);   // :61
                }
            }
        }
    }

    /**
     * {@code pbEffectsAfterMove(user,targets,move,numHits)}
     * (Battler_UseMove:544), called once the move is over.
     *
     * <p>Effective body = {@code Arceus:182-210} (the reopening) followed by the
     * aliased base {@code Battler_UseMove_TriggerEffects:70-145}.</p>
     */
    public static void pbEffectsAfterMove(Battle battle, Battler user, Array<Battler> targets,
                                          BattleMove move, int numHits) {
        // ---------------- Arceus:184-195 un-drowse ----------------
        if (effect(move).damagingMove(move)) {
            for (Battler b : targets) {
                if (b.damageState.unaffected || b.damageState.substitute) continue;   // :186
                if (!"DROWSY".equals(b.status)) continue;                              // :187
                // :190-191 non-canon: the plugin's Arceus section makes Electric
                // moves un-drowse. undrowsesUser? (Arceus:218) is
                // [WILDCHARGE,SPARK,VOLTACKLE].include?(@id) under
                // NEWEST_BATTLE_MECHANICS (true, Battle.java:1209).
                if ("ELECTRIC".equals(move.calcType()) || undrowsesUser(move)) {
                    b.pbCureStatus();                                                  // :192
                }
            }
        }
        // ---------------- Arceus:197-208 de-frostbite ----------------
        if (effect(move).damagingMove(move)) {
            for (Battler b : targets) {
                if (b.damageState.unaffected || b.damageState.substitute) continue;   // :199
                if (!"FROSTBITE".equals(b.status)) continue;                           // :200
                // :203-204 non-canon: Fire-type moves un-frostbite.
                if ("FIRE".equals(move.calcType()) || (Battle.NEWEST_BATTLE_MECHANICS && thawsUser(move))) {
                    b.pbCureStatus();                                                  // :205
                }
            }
        }
        // ---------------- base :71-83 defrost ----------------
        if (effect(move).damagingMove(move)) {
            for (Battler b : targets) {
                if (b.damageState.unaffected || b.damageState.substitute) continue;   // :74
                if (!"FROZEN".equals(b.status)) continue;                              // :75
                if ("FIRE".equals(move.calcType())
                        || (Battle.NEWEST_BATTLE_MECHANICS && thawsUser(move))) {      // :78-79
                    b.pbCureStatus();                                                  // :80
                }
            }
        }
        // ---------------- base :85-101 Destiny Bond ----------------
        if (user.effects.intVal(PBEffects.Battler.DestinyBondTarget, -1) >= 0 && !user.fainted()) {
            int rank = user.pokemon == null ? 0 : user.pokemon.battleRank;             // :89
            String dbName = battle.battlerAt(user.effects.intVal(PBEffects.Battler.DestinyBondTarget)).thisName();   // :90
            if (rank <= 2) {
                battle.display(dbName + "携带着对手一同倒下！");                        // :91
                user.pbReduceHP(user.hp, false, true, true);                           // :92
                user.pbItemHPHealCheck(0, false);                                      // :93
                user.pbFaint();                                                        // :94
                // :95 @battle.pbJudgeCheckpoint(user)
                // 登记: no judge / checkpoint subsystem in this runtime.
            } else {
                battle.displayPaused(dbName + "想要携带着对手一同倒下！");               // :98
                battle.display("但是" + user.thisName() + "免疫了\n" + dbName + "的同命！");   // :99
            }
        }
        // ---------------- base :103-105 user's ability ----------------
        if (user.abilityActive()) {
            BattleHandlers.triggerUserAbilityEndOfMove(user.ability, user, targets, move, battle);   // :104
        }
        // ---------------- base :107-122 Greninja Battle Bond ----------------
        // 登记: @battle.battleBond (a per-side/per-slot boolean table) is not
        // modelled, so this whole block is skipped rather than approximated.
        // ---------------- base :124-128 consume the user's Gem ----------------
        if (user.effects.truthy(PBEffects.Battler.GemConsumed)) {                      // :125
            user.pbConsumeItem();                                                      // :127
        }
        // ---------------- base :130-131 Roar / Whirlwind style switching ----------------
        // The plugin keeps ONE mutable switchedBattlers array that both the
        // MoveEffect hooks (:131/:140, raw battler indices) and the handler
        // wrappers (:160/:192, boxed) push into. This runtime has two container
        // types for those two surfaces, so both are kept live and merged after
        // every call that may push.
        IntArray switchedIx = new IntArray();                                          // :130
        Array<Integer> switched = new Array<>();                                       // :130 (mirror)
        effect(move).pbSwitchOutTargetsEffect(move, user, targets, numHits, switchedIx);   // :131
        mergeIndices(switchedIx, switched);
        // ---------------- base :133-135 all negated by Sheer Force ----------------
        if (effect(move).addlEffect(move) == 0 || !user.hasActiveAbility("SHEERFORCE")) {        // :133
            pbEffectsAfterMove2(battle, user, targets, move, numHits, switched, switchedIx);      // :134
        }
        // ---------------- base :139-141 U-turn / Baton Pass / Fling style effects ----------------
        if (!switched.contains(user.index, true)) {                                    // :140
            effect(move).pbEndOfMoveUsageEffect(move, user, targets, numHits, switchedIx);  // :140
            mergeIndices(switchedIx, switched);
        }
        // ---------------- base :142-144 end-of-move item checks ----------------
        if (numHits > 0) {                                                             // :142
            for (Battler b : battle.eachBattler()) {                                   // :143
                b.pbItemEndOfMoveCheck(0, false);
            }
        }
    }

    /** Copies any index the {@code MoveEffect} hooks pushed into the mirror list the wrappers use. */
    private static void mergeIndices(IntArray from, Array<Integer> to) {
        for (int i = 0; i < from.size; i++) {
            int value = from.get(i);
            if (!hasIndex(to, value)) {
                to.add(value);
            }
        }
    }

    /** {@code switchedBattlers.include?(index)} over the boxed mirror list. */
    private static boolean hasIndex(Array<Integer> list, int index) {
        for (int i = 0; i < list.size; i++) {
            if (list.get(i) == index) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code pbEffectsAfterMove2(user,targets,move,numHits,switchedBattlers)}
     * (Battler_UseMove_TriggerEffects:148-214). Everything in here is negated by
     * Sheer Force, which is why the caller gates the whole method.
     */
    private static void pbEffectsAfterMove2(Battle battle, Battler user, Array<Battler> targets,
                                            BattleMove move, int numHits, Array<Integer> switchedBattlers,
                                            IntArray switchedIx) {
        int hpNow = user.hp;                                                           // :149
        // :151-165 Target's held item (Eject Button, Red Card, Eject Pack).
        Array<Integer> switchByItem = new Array<>();                                   // :151
        for (Battler b : battle.wiringFieldedBySpeed()) {                              // :152
            if (hasIndex(switchedBattlers, b.index)) continue;                          // :153
            if (!b.itemActive()) continue;                                            // :154
            if (switchByItem.size > 0) continue;                                       // :155
            if (switchedBattlers.size > 0) continue;                                   // :156
            if (targets.contains(b, true)) {                                           // :158
                if (!b.damageState.unaffected && b.damageState.calcDamage != 0) {       // :159
                    BattleHandlers.triggerTargetItemAfterMoveUse(b.item, b, user, move, switchByItem, battle);   // :160
                }
            }
            if (b.effects.truthy(PBEffects.Battler.LashOut)) {                          // :163 Eject Pack
                BattleHandlers.triggerItemOnStatLoss(b.item, b, user, move, switchByItem, battle);          // :164
            }
        }
        if (hasIndex(switchByItem, user.index)) {                                  // :177
            battle.moldBreaker = false;
        }
        for (Battler b : battle.wiringFieldedBySpeed()) {                               // :178
            if (hasIndex(switchByItem, b.index)) {                                 // :179
                // :179 b.pbEffectsOnSwitchIn(true)
                // 登记: Battler#pbEffectsOnSwitchIn is not ported (Battler_AbilityAndItem:5-35).
                effectsOnSwitchIn(b);
            }
        }
        for (int idxB : switchByItem) {                                                 // :181
            switchedBattlers.add(idxB);
            switchedIx.add(idxB);
        }
        // :183-185 User's held item (Life Orb, Shell Bell).
        if (!hasIndex(switchedBattlers, user.index) && user.itemActive()) {              // :184
            BattleHandlers.triggerUserItemAfterMoveUse(user.item, user, targets, move, numHits, battle);   // :184
        }
        // :187-198 Target's ability (Berserk, Color Change, Emergency Exit, Pickpocket, Wimp Out).
        Array<Integer> switchWimpOut = new Array<>();                                   // :188
        for (Battler b : battle.wiringFieldedBySpeed()) {                               // :189
            if (!targets.contains(b, true)) continue;                                   // :189
            if (b.damageState.unaffected || hasIndex(switchedBattlers, b.index)) continue;   // :190
            if (!b.abilityActive()) continue;                                           // :191
            BattleHandlers.triggerTargetAbilityAfterMoveUse(b.ability, b, user, move, switchedBattlers, battle);   // :192
            if (!hasIndex(switchedBattlers, b.index) && effect(move).damagingMove(move)) {   // :193
                if (b.pbAbilitiesOnDamageTaken(b.damageState.initialHP, b.hp)) {        // :194 Emergency Exit / Wimp Out
                    switchWimpOut.add(b.index);                                         // :195
                }
            }
        }
        if (hasIndex(switchWimpOut, user.index)) {                                 // :199
            battle.moldBreaker = false;
        }
        for (Battler b : battle.wiringFieldedBySpeed()) {                               // :200
            if (b.index == user.index) continue;                                        // :201
            if (hasIndex(switchWimpOut, b.index)) {                                // :202
                effectsOnSwitchIn(b);                                                   // :202 (see the 登记 above)
            }
        }
        for (int idxB : switchWimpOut) {                                                // :204
            switchedBattlers.add(idxB);
            switchedIx.add(idxB);
        }
        // :206-213 User's ability (Emergency Exit, Wimp Out).
        if (!hasIndex(switchedBattlers, user.index) && effect(move).damagingMove(move)) {   // :206
            if (user.hp < hpNow) {                                                       // :207
                hpNow = user.hp;                                                         // :207 in case Life Orb hurt the user
            }
            if (user.pbAbilitiesOnDamageTaken(user.initialHP, hpNow)) {                   // :208
                battle.moldBreaker = false;                                              // :209
                effectsOnSwitchIn(user);                                                 // :210
                switchedBattlers.add(user.index);                                        // :211
                switchedIx.add(user.index);
            }
        }
    }

    /**
     * {@code Arceus:218 undrowsesUser?}: {@code
     * [PBMoves::WILDCHARGE,:SPARK,:VOLTACKLE].include?(@id)}, under the plugin's
     * {@code NEWEST_BATTLE_MECHANICS}. The hook was never added to the
     * {@code MoveEffect} surface, so it is evaluated here on the move's internal
     * name (this runtime's move identity).
     */
    private static boolean undrowsesUser(BattleMove move) {
        if (!Battle.NEWEST_BATTLE_MECHANICS) {
            return false;
        }
        String name = move.internalName();
        return "WILDCHARGE".equals(name) || "SPARK".equals(name) || "VOLTACKLE".equals(name);
    }

    /**
     * {@code PokeBattle_Move#thawsUser?} - the plugin's {@code /g/} flag
     * ({@code PokeBattle_Move.rb:119}). {@code BattleMove} exposes the raw flag
     * string, so the flag character is read from it.
     */
    private static boolean thawsUser(BattleMove move) {
        String flags = move.flags();
        return flags != null && flags.contains("g");
    }

    /**
     * {@code Battler#pbEffectsOnSwitchIn(showAnim)} (Battler_AbilityAndItem:5-35).
     *
     * <p><b>登记:</b> not ported onto {@link Battler} yet, and the plugin calls it
     * here only for a battler that just switched in because of an Eject
     * Button/Red Card/Eject Pack. Skipping it means such a battler does not run
     * its own switch-in ability that turn; the switch itself still happens.</p>
     */
    private static void effectsOnSwitchIn(Battler battler) {
        // Intentionally empty - see the 登记 note above.
    }
}
