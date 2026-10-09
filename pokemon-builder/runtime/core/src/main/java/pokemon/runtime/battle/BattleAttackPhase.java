package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Battle_Action_AttacksPriority:100-267 (target/priority calculation) and
 * Battle_Phase_Attack:5-194 (the attack phase), transcribed line by line.
 *
 * <p>登记 (what this runtime plays elsewhere): {@code pbAttackPhaseCall} (:16-22,
 * {@code :Call} is never a choice here), {@code pbAttackPhaseSwitch} (:50-71) and
 * {@code pbAttackPhaseItems} (:72-93) are performed by the battle screen
 * (the player's switch / item happens before {@code foeTurn}); the Mega Evolutions of
 * {@code pbAttackPhaseMegaEvolution} (:95-103) and {@code pbPursuit} (:35-39) are {@link BattleMega}'s.</p>
 */
public final class BattleAttackPhase {

    private BattleAttackPhase() {
    }

    // ------------------------------------------------------------------
    // Battle_Action_AttacksPriority:81-131
    // ------------------------------------------------------------------

    /** {@code pbChoseMoveFunctionCode?(idxBattler,code)} (:91-98). */
    public static boolean pbChoseMoveFunctionCode(Battle battle, int idxBattler, String code) {
        Battler b = battle.battlerAt(idxBattler);
        if (b == null || b.fainted()) return false;                                   // :92
        Object[] choice = battle.choices(idxBattler);
        int idxMove = choice[1] instanceof Integer ? (Integer) choice[1] : -1;                                            // :93
        if (":UseMove".equals(choice[0]) && idxMove >= 0) {                           // :94
            BattleMove move = b.moveSlot(idxMove);
            return move != null && code.equals(move.function());                      // :95
        }
        return false;                                                                 // :97
    }

    /** {@code pbChoseMove?(idxBattler,moveID)} (:81-89); the move is its internal name. */
    public static boolean pbChoseMove(Battle battle, int idxBattler, String moveName) {
        Battler b = battle.battlerAt(idxBattler);
        if (b == null || b.fainted()) return false;                                   // :82
        Object[] choice = battle.choices(idxBattler);
        int idxMove = choice[1] instanceof Integer ? (Integer) choice[1] : -1;                                            // :83
        if (":UseMove".equals(choice[0]) && idxMove >= 0) {                           // :84
            BattleMove move = b.moveSlot(idxMove);
            return move != null && moveName.equals(move.internalName());              // :85-86
        }
        return false;                                                                 // :88
    }

    /** {@code pbMoveCanTarget?(idxUser,idxTarget,targetType)} (:106-131). */
    public static boolean pbMoveCanTarget(Battle battle, int idxUser, int idxTarget, int targetType) {
        if (PBTargets.noTargets(targetType)) return false;                            // :107
        Battler user = battle.battlerAt(idxUser);
        boolean opposes = user.opposes(idxTarget);
        boolean near = user.near(idxTarget);
        switch (targetType) {                                                         // :108
            case PBTargets.NearAlly:                                                  // :109
                if (opposes) return false;                                            // :110
                if (!near) return false;                                              // :111
                break;
            case PBTargets.UserOrNearAlly:                                            // :112
                if (idxUser == idxTarget) return true;                                // :113
                if (opposes) return false;                                            // :114
                if (!near) return false;                                              // :115
                break;
            case PBTargets.NearFoe:                                                   // :116
            case PBTargets.AllNearFoes:
            case PBTargets.RandomNearFoe:
                if (!opposes) return false;                                           // :117
                if (!near) return false;                                              // :118
                break;
            case PBTargets.Foe:                                                       // :119
                if (!opposes) return false;                                           // :120
                break;
            case PBTargets.NearOther:                                                 // :121
            case PBTargets.AllNearOthers:
                if (!near) return false;                                              // :122
                break;
            case PBTargets.Other:                                                     // :123
                if (idxUser == idxTarget) return false;                               // :124
                break;
            case PBTargets.UserAndAllies:                                             // :125
                if (opposes) return false;                                            // :126
                break;
            case PBTargets.AllFoes:                                                   // :127
                if (!opposes) return false;                                           // :128
                break;
            default:
                break;
        }
        return true;                                                                  // :130
    }

    // ------------------------------------------------------------------
    // Battle_Action_AttacksPriority:136-248 pbCalculatePriority
    // ------------------------------------------------------------------

    /** {@code pbCalculatePriority(fullCalc=false,indexArray=nil)}. */
    public static void pbCalculatePriority(Battle battle, boolean fullCalc, int[] indexArray) {
        boolean needRearranging = false;                                              // :137
        if (fullCalc) {                                                               // :138
            battle.priorityTrickRoom = battle.field.effects.intVal(PBEffects.Field.TrickRoom) > 0;   // :139
            // Recalculate everything from scratch
            int n = battle.maxBattlerIndex() + 1;
            int[] randomOrder = new int[n];                                           // :141
            for (int i = 0; i < n; i++) randomOrder[i] = i;
            for (int i = 0; i < n - 1; i++) {                                         // :142 (randomOrder.length-1).times
                int r = i + battle.pbRandom(n - i);                                   // :143
                int tmp = randomOrder[i];                                             // :144
                randomOrder[i] = randomOrder[r];
                randomOrder[r] = tmp;
            }
            battle.priority.clear();                                                  // :146
            for (int i = 0; i <= battle.maxBattlerIndex(); i++) {                     // :147
                Battler b = battle.battlerAt(i);                                      // :148
                if (b == null) continue;                                              // :149
                // [battler, speed, sub-priority, priority, tie-breaker order]
                Object[] bArray = {b, b.speed(), 0, 0, randomOrder[i]};               // :151
                Object[] choice = battle.choices(b.index);
                if (":UseMove".equals(choice[0]) || ":Shift".equals(choice[0])) {     // :152
                    // Calculate move's priority
                    if (":UseMove".equals(choice[0])) {                               // :154
                        BattleMove move = (BattleMove) choice[2];                     // :155
                        int pri = move.priority();                                    // :156
                        if (b.abilityActive()) {                                      // :157
                            pri = BattleHandlers.triggerPriorityChangeAbility(b.ability, b, move, pri);   // :158
                        }
                        bArray[3] = pri;                                              // :160
                        // Grassy Glide
                        if ("18C".equals(move.function())                             // :162
                                && battle.field.terrain == PBBattleTerrains.Grassy
                                && !b.airborne()) {
                            pri += 1;                                                 // :165
                        }
                        // 古龙雷枪
                        if ("19E".equals(move.function())                             // :168
                                && (battle.field.weather == PBWeather.HeavyRain
                                || battle.field.weather == PBWeather.Rain)) {
                            pri += 1;                                                 // :171
                        }
                        bArray[3] = pri;                                              // :173
                        choice[4] = pri;                                              // :174
                    }
                    // Calculate sub-priority (first/last within priority bracket)
                    int subPri = 0;                                                   // :181
                    // Abilities (Stall)
                    if (b.abilityActive()) {                                          // :183
                        int newSubPri = BattleHandlers.triggerPriorityBracketChangeAbility(b.ability, b, subPri, battle);   // :184
                        if (subPri != newSubPri) {                                    // :186
                            subPri = newSubPri;                                       // :187
                            b.effects.set(PBEffects.Battler.PriorityAbility, true);   // :188
                            b.effects.set(PBEffects.Battler.PriorityItem, false);     // :189
                        }
                    }
                    // Items (Quick Claw, Custap Berry, Lagging Tail, Full Incense)
                    if (b.itemActive()) {                                             // :193
                        int newSubPri = BattleHandlers.triggerPriorityBracketChangeItem(b.item, b, subPri, battle);   // :194
                        if (subPri != newSubPri) {                                    // :196
                            subPri = newSubPri;                                       // :197
                            b.effects.set(PBEffects.Battler.PriorityAbility, false);  // :198
                            b.effects.set(PBEffects.Battler.PriorityItem, true);      // :199
                        }
                    }
                    bArray[2] = subPri;                                               // :202
                }
                battle.priority.add(bArray);                                          // :204
            }
            needRearranging = true;                                                   // :206
        } else {
            if ((battle.field.effects.intVal(PBEffects.Field.TrickRoom) > 0) != battle.priorityTrickRoom) {   // :208
                needRearranging = true;                                               // :209
                battle.priorityTrickRoom = battle.field.effects.intVal(PBEffects.Field.TrickRoom) > 0;   // :210
            }
            // Just recheck all battler speeds
            for (Object[] orderArray : battle.priority) {                             // :213
                if (orderArray == null) continue;                                     // :214
                Battler ob = (Battler) orderArray[0];
                if (indexArray != null && !contains(indexArray, ob.index)) continue;  // :215
                int oldSpeed = (Integer) orderArray[1];                               // :216
                orderArray[1] = ob.speed();                                           // :217
                if ((Integer) orderArray[1] != oldSpeed) needRearranging = true;      // :218
            }
        }
        // Reorder the priority array
        if (needRearranging) {                                                        // :222
            final boolean trickRoom = battle.priorityTrickRoom;
            battle.priority.sort((a, b) -> {                                          // :223
                int a3 = (Integer) a[3], b3 = (Integer) b[3];
                int a2 = (Integer) a[2], b2 = (Integer) b[2];
                int a1 = (Integer) a[1], b1 = (Integer) b[1];
                int a4 = (Integer) a[4], b4 = (Integer) b[4];
                if (a3 != b3) {
                    return Integer.compare(b3, a3);                                   // :226 highest priority first
                } else if (a2 != b2) {
                    return Integer.compare(b2, a2);                                   // :229 highest sub-priority first
                } else if (trickRoom) {                                               // :230
                    return a1 == b1 ? Integer.compare(b4, a4) : Integer.compare(a1, b1);   // :232 lowest speed first
                } else {
                    return a1 == b1 ? Integer.compare(b4, a4) : Integer.compare(b1, a1);   // :235 highest speed first
                }
            });
        }
    }

    private static boolean contains(int[] arr, int v) {
        for (int x : arr) if (x == v) return true;
        return false;
    }

    /** {@code pbPriority(onlySpeedSort=false)} (:253-267). */
    public static Array<Battler> pbPriority(Battle battle, boolean onlySpeedSort) {
        Array<Battler> ret = new Array<>();                                           // :254
        if (onlySpeedSort) {                                                          // :255
            // Sort battlers by their speed stats and tie-breaker order only.
            List<Object[]> tempArray = new ArrayList<>();                             // :257
            for (Object[] pArray : battle.priority) {                                 // :258
                tempArray.add(new Object[]{pArray[0], pArray[1], pArray[4]});
            }
            tempArray.sort((a, b) -> {                                                // :259
                int a1 = (Integer) a[1], b1 = (Integer) b[1];
                int a2 = (Integer) a[2], b2 = (Integer) b[2];
                return a1 == b1 ? Integer.compare(b2, a2) : Integer.compare(b1, a1);
            });
            for (Object[] tArray : tempArray) ret.add((Battler) tArray[0]);           // :260
        } else {
            // Sort battlers by priority, sub-priority and their speed. Ties are
            // resolved in the same way each time this method is called in a round.
            for (Object[] pArray : battle.priority) {                                 // :264
                Battler b = (Battler) pArray[0];
                if (!b.fainted()) ret.add(b);
            }
        }
        return ret;                                                                   // :266
    }

    // ------------------------------------------------------------------
    // Battle_Phase_Attack
    // ------------------------------------------------------------------

    /** {@code pbAttackPhasePriorityChangeMessages} (:5-14): Quick Claw, Custap Berry's message. */
    public static void pbAttackPhasePriorityChangeMessages(Battle battle) {
        for (Battler b : pbPriority(battle, false)) {                                 // :7
            if (b.effects.truthy(PBEffects.Battler.PriorityAbility) && b.abilityActive()) {   // :8
                BattleHandlers.triggerPriorityBracketUseAbility(b.ability, b, battle);        // :9
            } else if (b.effects.truthy(PBEffects.Battler.PriorityItem) && b.itemActive()) {  // :10
                BattleHandlers.triggerPriorityBracketUseItem(b.item, b, battle);              // :11
            }
        }
    }

    /** {@code pbPursuit(idxSwitcher)} (:24-48). */
    public static void pbPursuit(Battle battle, int idxSwitcher) {
        battle.switching = true;                                                      // :25
        for (Battler b : pbPriority(battle, false)) {                                 // :26
            if (b.fainted() || !b.opposes(idxSwitcher)) continue;                     // :27 Shouldn't hit an ally
            if (b.movedThisRound() || !pbChoseMoveFunctionCode(battle, b.index, "088")) continue;   // :28 Pursuit
            // Check whether Pursuit can be used
            Object[] choice = battle.choices(b.index);
            BattleMove chosen = (BattleMove) choice[2];
            if (!pbMoveCanTarget(battle, b.index, idxSwitcher, PBTargets.fromName(chosen.target()))) continue;   // :30
            if (!battle.pbCanChooseMove(b.index, (Integer) choice[1], false, false)) continue;                   // :31
            if ("SLEEP".equals(b.status) || "FROZEN".equals(b.status)) continue;       // :32
            if (b.effects.intVal(PBEffects.Battler.SkyDrop) >= 0) continue;           // :33
            if (b.hasActiveAbility("TRUANT") && b.effects.truthy(PBEffects.Battler.Truant)) continue;   // :34
            BattleMega.pbPursuitMegaEvolve(battle, b);                                // :35-39 Mega Evolve
            // Use Pursuit
            choice[3] = idxSwitcher;                                                  // :41 Change Pursuit's target
            if (b.pbProcessTurn(choice, false)) {                                     // :42
                b.effects.set(PBEffects.Battler.Pursuit, true);                       // :43
            }
            if (battle.decision > 0 || battle.battlerAt(idxSwitcher).fainted()) break;   // :45
        }
        battle.switching = false;                                                     // :47
    }

    /** {@code pbAttackPhaseMoves} (:105-164). */
    public static void pbAttackPhaseMoves(Battle battle) {
        // Show charging messages (Focus Punch)
        for (Battler b : pbPriority(battle, false)) {                                 // :107
            Object[] choice = battle.choices(b.index);
            if (!(":UseMove".equals(choice[0]) && !b.fainted())) continue;           // :108
            if (b.movedThisRound()) continue;                                         // :109
            BattleMove move = (BattleMove) choice[2];
            MoveEffectRegistry.of(move.function()).pbDisplayChargeMessage(move, b);   // :110
        }
        // Main move processing loop
        while (true) {                                                                // :113
            Array<Battler> priority = pbPriority(battle, false);                      // :114
            // Forced to go next
            boolean advance = false;                                                  // :116
            for (Battler b : priority) {                                              // :117
                if (!(b.effects.truthy(PBEffects.Battler.MoveNext) && !b.fainted())) continue;   // :118
                Object[] choice = battle.choices(b.index);
                if (!(":UseMove".equals(choice[0]) || ":Shift".equals(choice[0]))) continue;     // :119
                if (b.movedThisRound()) continue;                                     // :120
                advance = b.pbProcessTurn(choice);                                    // :121
                if (advance) break;                                                   // :122
            }
            if (battle.decision > 0) return;                                          // :124
            if (advance) continue;                                                    // :125
            // Regular priority order
            for (Battler b : priority) {                                              // :127
                if (b.effects.intVal(PBEffects.Battler.Quash) > 0 || b.fainted()) continue;       // :128
                Object[] choice = battle.choices(b.index);
                if (!(":UseMove".equals(choice[0]) || ":Shift".equals(choice[0]))) continue;     // :129
                if (b.movedThisRound()) continue;                                     // :130
                advance = b.pbProcessTurn(choice);                                    // :131
                if (advance) break;                                                   // :132
            }
            if (battle.decision > 0) return;                                          // :134
            if (advance) continue;                                                    // :135
            // Quashed
            int quashLevel = 0;                                                       // :137
            while (true) {                                                            // :138
                quashLevel += 1;                                                      // :139
                boolean moreQuash = false;                                            // :140
                for (Battler b : priority) {                                          // :141
                    if (b.effects.intVal(PBEffects.Battler.Quash) > quashLevel) moreQuash = true;   // :142
                    if (!(b.effects.intVal(PBEffects.Battler.Quash) == quashLevel && !b.fainted())) continue;   // :143
                    Object[] choice = battle.choices(b.index);
                    if (!(":UseMove".equals(choice[0]) || ":Shift".equals(choice[0]))) continue;   // :144
                    if (b.movedThisRound()) continue;                                 // :145
                    advance = b.pbProcessTurn(choice);                                // :146
                    break;                                                            // :147
                }
                if (advance || !moreQuash) break;                                     // :149
            }
            if (battle.decision > 0) return;                                          // :151
            if (advance) continue;                                                    // :152
            // Check for all done
            for (Battler b : priority) {                                              // :154
                Object[] choice = battle.choices(b.index);
                if (!b.fainted() && !b.movedThisRound()) {                            // :155
                    if (":UseMove".equals(choice[0]) || ":Shift".equals(choice[0])) advance = true;   // :156
                }
                if (advance) break;                                                   // :158
            }
            if (advance) continue;                                                    // :160
            // All Pokémon have moved; end the loop
            break;                                                                    // :162
        }
    }

    /**
     * {@code pbAttackPhase} (:169-194), without the actions the battle screen plays (class javadoc).
     * When the screen asked for the switch round's {@code pbPursuit} first
     * ({@link Battle#pbPursuitOnSwitch}) the prologue (:171-184) has already run.
     */
    public static void pbAttackPhase(Battle battle) {
        if (battle.attackPhasePrepared) {
            battle.attackPhasePrepared = false;
        } else {
            pbAttackPhasePrologue(battle);
        }
        // Perform actions
        pbAttackPhasePriorityChangeMessages(battle);                                  // :186
        // :187-191 call / switch / items: see class javadoc
        BattleMega.pbAttackPhaseMegaEvolution(battle);                                // :192
        pbAttackPhaseMoves(battle);                                                   // :193
    }

    /** {@code pbAttackPhase} :170-184: the resets and the round's move order. */
    public static void pbAttackPhasePrologue(Battle battle) {
        // @scene.pbBeginAttackPhase - nothing to do without a scene
        // Reset certain effects
        for (int i = 0; i <= battle.maxBattlerIndex(); i++) {                         // :172
            Battler b = battle.battlerAt(i);
            if (b == null) continue;                                                  // :173
            Object[] choice = battle.choices(i);
            // 登记: :174 b.turnCount += 1 would be undone for a battler that switched out this
            // round (its replacement starts at 0, Battler_Initialize); the screen has already
            // replaced it, so the increment is skipped for a :SwitchOut choice.
            if (!b.fainted() && !":SwitchOut".equals(choice[0])) b.turnCount += 1;     // :174
            battle.successStates[i].clear();                                          // :175
            if (!":UseMove".equals(choice[0]) && !":Shift".equals(choice[0]) && !":SwitchOut".equals(choice[0])) {   // :176
                b.effects.set(PBEffects.Battler.DestinyBond, false);                  // :177
                b.effects.set(PBEffects.Battler.Grudge, false);                       // :178
            }
            if (!pbChoseMoveFunctionCode(battle, i, "093")) {                         // :180 Rage
                b.effects.set(PBEffects.Battler.Rage, false);
            }
        }
        // Calculate move order for this round
        pbCalculatePriority(battle, true, null);                                      // :184
    }
}
