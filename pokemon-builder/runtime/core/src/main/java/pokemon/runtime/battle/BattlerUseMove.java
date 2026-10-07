package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 5 / 2d: the master "use move" flow, {@code Battler_UseMove:152-809}
 * ({@code pbUseMoveSimple}, {@code pbUseMove}, {@code pbProcessMoveHit}), line
 * by line. {@code pbBeginTurn}/{@code pbCancelMoves}/{@code pbEndTurn} (:71-128)
 * are on {@link Battler} (2a); the checks it calls are
 * {@link BattlerUseMoveChecks}, {@link BattleSuccessChecks}, {@link BattlerTargeting}
 * and {@link MoveUsage}.
 *
 * <p>Each method is the body of the {@code PokeBattle_Battler} method of the same
 * name with {@code self} first. {@code choice} is the shared
 * {@code Object[] {action, slot, BattleMove, target}}.</p>
 *
 * <h2>Registered</h2>
 * <ul>
 * <li>{@code :403-409} Deoxys' form change reads {@code @effects[DeoxysForm]},
 *     which is only ever written by the battle command UI
 *     ({@code Scene_Commands:149-172}, not modelled); it is transcribed and runs
 *     when that effect is set.</li>
 * <li>{@code :399-401}/{@code :635-637} Dragon Darts redirection is transcribed
 *     but needs a side size above 1, which the singles field does not have
 *     ({@link Battle#pbSideSize}).</li>
 * <li>{@code :557 pbHyperMode} is {@code Battler#pbHyperMode}, a no-op for any
 *     non-Shadow Pokemon (Pokemon_ShadowPokemon:401).</li>
 * <li>{@code :555 successStates[..].updateSkill} / {@code :265/:484-489} Battle
 *     Arena state is kept on {@link Battle#successStates}.</li>
 * </ul>
 */
public final class BattlerUseMove {

    private BattlerUseMove() {
    }

    private static boolean isStruggle(Battler self, BattleMove move) {
        return move != null && "STRUGGLE".equals(move.internalName());               // @battle.struggle (Battler#struggle always returns STRUGGLE)
    }

    // ------------------------------------------------------------------
    // Turn processing (:5-66)
    // ------------------------------------------------------------------

    /**
     * {@code pbProcessTurn(choice,tryFlee=true)} (:5-66): one battler's action of the
     * round. Returns true when the battler acted ({@code advance} in
     * {@code pbAttackPhaseMoves}).
     *
     * <p>Registered: {@code :8-16} "Wild roaming Pokemon always flee"
     * reads {@code @battle.rules["alwaysflee"]} (battle rules are not modelled);
     * {@code :18-41} Shift only does something on a side of 2 or more
     * ({@link Battle#pbSideSize}); {@code :64} {@code pbCalculatePriority}
     * (DYNAMIC_PRIORITY, Settings:161) has no priority table to refresh - the
     * singles order is computed once per round by {@code Battle.runTurn}.</p>
     */
    public static boolean pbProcessTurn(Battler self, Object[] choice, boolean tryFlee) {
        Battle battle = self.battle;
        if (self.fainted()) return false;                                            // :6
        // Shift with the battler next to this one
        if (":Shift".equals(choice[0])) {                                            // :18
            // :19-36 idxOther stays -1 on a side of size 1: nothing is swapped, nothing is shown
            self.pbBeginTurn(choice);                                                // :37
            self.pbCancelMoves();                                                    // :38
            self.lastRoundMoved = battle.turnCount();                                // :39 Done something this round
            return true;                                                             // :40
        }
        // If this battler's action for this round wasn't "use a move"
        if (!":UseMove".equals(choice[0])) {                                         // :43
            // Clean up effects that end at battler's turn
            self.pbBeginTurn(choice);                                                // :45
            self.pbEndTurn(choice);                                                  // :46
            return false;                                                            // :47
        }
        // Turn is skipped if Pursuit was used during switch
        if (self.effects.truthy(PBEffects.Battler.Pursuit)) {                        // :50
            self.effects.set(PBEffects.Battler.Pursuit, false);                      // :51
            self.pbCancelMoves();                                                    // :52
            self.pbEndTurn(choice);                                                  // :53
            battle.pbJudge();                                                        // :54
            return false;                                                            // :55
        }
        // Use the move
        self.pbUseMove(choice, isStruggle(self, (BattleMove) choice[2]));            // :60
        battle.pbJudge();                                                            // :62
        battle.pbCalculatePriority();                                                // :64 if DYNAMIC_PRIORITY (Settings:161 = true)
        return true;                                                                 // :65
    }

    // ------------------------------------------------------------------
    // Simple "use move" method (:152-165)
    // ------------------------------------------------------------------

    /** {@code pbUseMoveSimple(moveID,target=-1,idxMove=-1,specialUsage=true)} (:152-165); the move is its internal name here. */
    public static void pbUseMoveSimple(Battler self, String moveName, int target, int idxMove, boolean specialUsage) {
        Object[] choice = new Object[4];                                             // :153
        choice[0] = ":UseMove";                                                      // :154 "Use move"
        choice[1] = idxMove;                                                         // :155 Index of move to be used in user's moveset
        if (idxMove >= 0) {                                                          // :156
            choice[2] = self.moveSlot(idxMove);                                      // :157
        } else {
            choice[2] = new BattleMove(self.battle.pbs().move(moveName));            // :159-160 PokeBattle_Move.pbFromPBMove; pp=-1 is "not in the moveset" (see Battler#pbReducePP)
        }
        choice[3] = target;                                                          // :162 Target (-1 means no target yet)
        pbUseMove(self, choice, specialUsage);                                       // :164
    }

    // ------------------------------------------------------------------
    // Master "use move" method (:170-622)
    // ------------------------------------------------------------------

    /** {@code pbUseMove(choice,specialUsage=false)} (:170-622). */
    public static void pbUseMove(Battler self, Object[] choice, boolean specialUsage) {
        Battle battle = self.battle;
        // NOTE: This is intentionally determined before a multi-turn attack can set specialUsage to true.
        boolean skipAccuracyCheck = (specialUsage && !isStruggle(self, (BattleMove) choice[2]));   // :173
        // Start using the move
        self.pbBeginTurn(choice);                                                    // :175
        // Force the use of certain moves if they're already being used
        if (self.usingMultiTurnAttack()) {                                           // :177
            choice[2] = new BattleMove(battle.pbs().moveById(self.currentMove));     // :178
            specialUsage = true;                                                     // :179
        } else if (self.effects.intVal(PBEffects.Battler.Encore) > 0 && (Integer) choice[1] >= 0
                && battle.pbCanShowCommands(self.index)) {                           // :180-181
            int idxEncoredMove = self.pbEncoredMoveIndex();                      // :182
            if (idxEncoredMove >= 0 && battle.pbCanChooseMove(self.index, idxEncoredMove, false, false)) {   // :183
                if ((Integer) choice[1] != idxEncoredMove) {                         // :184 Change move if battler was Encored mid-round
                    choice[1] = idxEncoredMove;                                      // :185
                    choice[2] = self.moveSlot(idxEncoredMove);                       // :186
                    choice[3] = -1;                                                  // :187 No target chosen
                }
            }
        }
        // Labels the move being used as "move"
        BattleMove move = (BattleMove) choice[2];                                    // :192
        if (move == null || move.id() == 0) return;                                  // :193 if move was not chosen somehow
        MoveEffect fx = MoveEffectRegistry.of(move.function());
        // Try to use the move (inc. disobedience)
        self.lastMoveFailed = false;                                                 // :195
        if (!self.pbTryUseMove(choice, move, specialUsage, skipAccuracyCheck)) {     // :196
            self.lastMoveUsed = null;                                                // :197
            self.lastMoveUsedType = null;                                            // :198
            if (!specialUsage) {                                                     // :199
                self.lastRegularMoveUsed = null;                                     // :200
                self.lastRegularMoveTarget = -1;                                     // :201
            }
            battle.pbGainExp();                                                      // :203 In case self is KO'd due to confusion
            self.pbCancelMoves();                                                    // :204
            self.pbEndTurn(choice);                                                  // :205
            return;                                                                  // :206
        }
        move = (BattleMove) choice[2];                                               // :208 In case disobedience changed the move to be used
        if (move == null || move.id() == 0) return;                                  // :209
        fx = MoveEffectRegistry.of(move.function());
        // Subtract PP
        if (!specialUsage && self.pokemon.battleRank < 2) {                          // :211
            if (!self.pbReducePP(move)) {                                            // :212
                battle.display(self.pbThis() + "使用" + move.name() + "！");           // :213
                battle.display("但招式已经没有PP了！");                                // :214
                self.lastMoveUsed = null;                                            // :215
                self.lastMoveUsedType = null;                                        // :216
                self.lastRegularMoveUsed = null;                                     // :217
                self.lastRegularMoveTarget = -1;                                     // :218
                self.lastMoveFailed = true;                                          // :219
                self.pbCancelMoves();                                                // :220
                self.pbEndTurn(choice);                                              // :221
                return;                                                              // :222
            }
        }
        // Stance Change
        if (self.isSpecies("AEGISLASH") && "STANCECHANGE".equals(self.ability)) {    // :226
            if (fx.damagingMove(move)) {                                             // :227
                self.pbChangeFormTransform(1, self.pbThis() + "变成了刀剑形态！");       // :228
            } else if ("KINGSSHIELD".equals(move.internalName())) {                  // :229
                self.pbChangeFormTransform(0, self.pbThis() + "变成了盾牌形态！");       // :230
            }
        }
        // Calculate the move's type during this usage
        move.setCalcType(fx.pbCalcType(move, self));                                 // :234
        // Start effect of Mold Breaker
        battle.moldBreaker = self.hasMoldBreaker();                                  // :236
        // Remember that user chose a two-turn move
        if (fx.pbIsChargingTurn(move, self)) {                                       // :238
            // Beginning the use of a two-turn attack
            self.effects.set(PBEffects.Battler.TwoTurnAttack, move.id());            // :240
            self.currentMove = move.id();                                            // :241
        } else {
            self.effects.set(PBEffects.Battler.TwoTurnAttack, 0);                    // :243 Cancel use of two-turn attack
        }
        // Add to counters for moves which increase them when used in succession
        fx.pbChangeUsageCounters(move, self, specialUsage);                          // :246
        // Charge up Metronome item
        if (self.hasActiveItem("METRONOME") && !fx.callsAnotherMove(move)) {         // :248
            if (move.internalName() != null && move.internalName().equals(self.lastMoveUsed)
                    && !self.lastMoveFailed) {                                       // :249
                if (!"BAMBOOSWORD".equals(move.internalName())) {                    // :250
                    self.effects.add(PBEffects.Battler.Metronome, 1);
                }
            } else {
                self.effects.set(PBEffects.Battler.Metronome, 0);                    // :252
            }
        }
        // Record move as having been used
        self.lastMoveUsed = move.internalName();                                     // :256
        self.lastMoveUsedType = move.calcType();                                     // :257 For Conversion 2
        if (!specialUsage) {                                                         // :258
            self.lastRegularMoveUsed = move.internalName();                          // :259 For Disable, Encore, Instruct, Mimic, Mirror Move, Sketch, Spite
            self.lastRegularMoveTarget = (Integer) choice[3];                        // :260 For Instruct
            if (!self.movesUsed.contains(move.internalName(), false)) {              // :261 For Last Resort
                self.movesUsed.add(move.internalName());
            }
        }
        battle.lastMoveUsed = move.id();                                             // :263 For Copycat
        battle.lastMoveUser = self.index;                                            // :264 For "self KO" battle clause to avoid draws
        battle.successStates[self.index].useState = 1;                               // :265 Battle Arena - assume failure
        // Find the default user (self or Snatcher) and target(s)
        Battler user = BattlerTargeting.pbFindUser(self, choice, move);              // :267
        user = BattlerTargeting.pbChangeUser(self, choice, move, user);              // :268
        Array<Battler> targets = BattlerTargeting.pbFindTargets(self, choice, move, user);   // :269
        targets = BattlerTargeting.pbChangeTargets(self, move, user, targets);       // :270
        // Pressure
        if (!specialUsage) {                                                         // :272
            for (Battler b : targets) {                                              // :273
                if (!(b.opposes(user) && b.hasActiveAbility(
                        new String[] {"PRESSURE", "CALAMITYABYSSAL", "CALAMITYINFERNAL"}))) continue;   // :274
                user.pbReducePP(move);                                               // :276
            }
            if (PBTargets.targetsFoeSide(fx.pbTarget(move, user))) {                 // :278
                for (Battler b : battle.eachOtherSideBattler(user.index)) {          // :279
                    if (!b.hasActiveAbility(
                            new String[] {"PRESSURE", "CALAMITYABYSSAL", "CALAMITYINFERNAL"})) continue;   // :280
                    user.pbReducePP(move);                                           // :282
                }
            }
        }
        // Dazzling/Queenly Majesty make the move fail here
        for (Battler b : battle.wiringFieldedBySpeed()) {                            // :287 pbPriority(true)
            if (b == null || !b.abilityActive()) continue;                           // :288
            if (BattleHandlers.triggerMoveBlockingAbility(b.ability, b, user, targets, move, battle)) {   // :289
                battle.displayBrief(user.pbThis() + "使用" + move.name() + "！");       // :290
                battle.showAbilitySplash(b);                                         // :291
                battle.display(user.pbThis() + "不能使用" + move.name() + "了！");       // :292
                battle.hideAbilitySplash(b);                                         // :293
                user.lastMoveFailed = true;                                          // :294
                self.pbCancelMoves();                                                // :295
                self.pbEndTurn(choice);                                              // :296
                return;                                                              // :297
            }
        }
        // "X used Y!" message
        // NOTE: This intentionally passes self rather than user (the original user even if Snatched).
        fx.pbDisplayUseMessage(move, self);                                          // :305
        // Snatch's message (user is the new user, self is the original user)
        if (move.snatched()) {                                                       // :307
            self.lastMoveFailed = true;                                              // :308 Intentionally applies to self, not user
            battle.display(user.pbThis() + "抢夺了" + self.pbThis(true) + "的招式效果！");   // :309
        }
        // "But it failed!" checks
        if (fx.pbMoveFailed(move, user, targets)) {                                  // :312
            user.lastMoveFailed = true;                                              // :314
            self.pbCancelMoves();                                                    // :315
            self.pbEndTurn(choice);                                                  // :316
            return;                                                                  // :317
        }
        // Perform set-up actions and display messages
        // Messages include Magnitude's number and Pledge moves' "it's a combo!"
        fx.pbOnStartUse(move, user, targets);                                        // :321
        // Self-thawing due to the move
        if ("FROZEN".equals(user.status) && fx.thawsUser(move)) {                    // :323
            user.pbCureStatus(false);                                                // :324
            battle.display(user.pbThis() + "融化了冰！");                              // :325
        }
        // Pokémon Legends Arceus: Cures Drowsy.
        if ("DROWSY".equals(user.status) && undrowsesUser(move)) {                   // :328
            user.pbCureStatus(false);                                                // :329
            battle.display(user.pbThis() + " is longer drowsy!");                    // :330
        }
        // Pokémon Legends Arceus: Cures Frostbite.
        if ("FROSTBITE".equals(user.status) && fx.thawsUser(move)) {                 // :333
            user.pbCureStatus(false);                                                // :334
            battle.display(user.pbThis() + " cures its frostbite!");                 // :335
        }
        // Powder
        if (user.effects.truthy(PBEffects.Battler.Powder) && "FIRE".equals(move.calcType())) {   // :338
            battle.commonAnimation("Powder", user);                                  // :339
            battle.display("火焰在接触粉尘时发生了爆炸！");                              // :340
            user.lastMoveFailed = true;                                              // :341
            int w = battle.pbWeather();                                              // :342
            if (w != PBWeather.Rain && w != PBWeather.HeavyRain && user.takesIndirectDamage(false)) {   // :343
                int oldHP = user.hp;                                                 // :344
                user.pbReduceHP((int) Math.round(user.maxHp() / 4.0), false, true, true);   // :345
                if (user.fainted()) user.pbFaint();                                  // :346
                battle.pbGainExp();                                                  // :347 In case user is KO'd by this
                user.pbItemHPHealCheck(0, false);                                    // :348
                if (user.pbAbilitiesOnDamageTaken(oldHP, -1)) {                      // :349
                    user.pbEffectsOnSwitchIn(true);                                  // :350
                }
            }
            self.pbCancelMoves();                                                    // :353
            self.pbEndTurn(choice);                                                  // :354
            return;                                                                  // :355
        }
        // Primordial Sea, Desolate Land
        if (fx.pbDamagingMove(move)) {                                               // :358 move.damagingMove?
            int weather = battle.pbWeather();                                        // :359
            if (weather == PBWeather.HeavyRain) {                                    // :360
                if ("FIRE".equals(move.calcType())) {                                // :361
                    battle.display("火属性攻击在大雨中消耗殆尽！");                        // :362
                    user.lastMoveFailed = true;                                      // :363
                    self.pbCancelMoves();                                            // :364
                    self.pbEndTurn(choice);                                          // :365
                    return;                                                          // :366
                }
            } else if (weather == PBWeather.HarshSun) {                              // :368
                if ("WATER".equals(move.calcType())) {                               // :369
                    battle.display("水属性攻击在阳光中蒸发殆尽！");                        // :370
                    user.lastMoveFailed = true;                                      // :371
                    self.pbCancelMoves();                                            // :372
                    self.pbEndTurn(choice);                                          // :373
                    return;                                                          // :374
                }
            }
        }
        // Protean / Libero
        if (user.hasActiveAbility("PROTEAN") || user.hasActiveAbility("LIBERO")
                || (user.hasActiveAbility("SAVAGECEREMONY") && !fx.callsAnotherMove(move) && !move.snatched())) {   // :379-381
            if (user.pbHasOtherType(move.calcType()) && !PBTypes.isPseudoType(battle.pbs(), move.calcType())) {   // :382
                battle.showAbilitySplash(user);                                      // :383
                user.pbChangeTypes(move.calcType());                                 // :384
                String typeName = PBTypes.getName(battle.pbs(), move.calcType());    // :385
                battle.display(user.pbThis() + "变为了" + typeName + "属性！");          // :386
                battle.hideAbilitySplash(user);                                      // :387
                // NOTE: Curse used by a non-Ghost user becoming Ghost-type chooses a random opponent instead.
                if ("10D".equals(move.function()) && targets.size == 0) {            // :392 Curse
                    choice[3] = -1;                                                  // :393
                    targets = BattlerTargeting.pbFindTargets(self, choice, move, user);   // :394
                }
            }
        }
        // Redirect Dragon Darts first hit if necessary
        if ("17C".equals(move.function()) && targets.size > 0
                && battle.pbSideSize(targets.get(0).index) > 1) {                    // :399
            targets = BattlerTargeting.pbChangeTargets(self, move, user, targets, 0);    // :400
        }
        // 代欧奇希斯变形态
        if (self.isSpecies("DEOXYS")
                && self.effects.intVal(PBEffects.Battler.DeoxysForm) != user.form()) {   // :404
            int newForm = self.effects.intVal(PBEffects.Battler.DeoxysForm);         // :405
            String[] formNames = {"普通", "攻击", "防御", "速度"};                      // :406
            self.pbChangeFormTransform(newForm, self.pbThis() + "变为了" + formNames[newForm] + "形态！");   // :407
            user.effects.set(PBEffects.Battler.DeoxysForm, user.form());             // :408
        }
        //---------------------------------------------------------------------------
        int magicCoater = -1;                                                        // :411
        int magicBouncer = -1;                                                       // :412
        int realNumHits = 0;                                                         // :457
        if (targets.size == 0 && !PBTargets.noTargets(fx.pbTarget(move, user))
                && !fx.worksWithNoTargets(move)) {                                   // :413-414
            // def pbFindTargets should have found a target(s), but it didn't because they were all fainted
            battle.display("但是没有目标……");                                         // :418
            user.lastMoveFailed = true;                                              // :419
        } else {   // We have targets, or move doesn't use targets
            // Reset whole damage state, perform various success checks (not accuracy)
            for (Battler b : battle.allBattlers()) {                                 // :422
                b.droppedBelowHalfHP = false;                                        // :423
                b.statsDropped = false;                                              // :424
            }
            user.initialHP = user.hp;                                                // :426
            for (Battler b : targets) {                                              // :427
                b.damageState.reset();                                               // :428
                b.damageState.initialHP = b.hp;                                      // :429
                if (!BattleSuccessChecks.pbSuccessCheckAgainstTarget(battle, move, user, b)) {   // :430
                    b.damageState.unaffected = true;                                 // :431
                }
            }
            // Magic Coat/Magic Bounce checks (for moves which don't target Pokémon)
            if (targets.size == 0 && fx.canMagicCoat(move)) {                        // :435
                for (Battler b : battle.wiringFieldedBySpeed()) {                    // :436 pbPriority(true)
                    if (b.fainted() || !b.opposes(user)) continue;                   // :437
                    if (b.semiInvulnerable()) continue;                              // :438
                    if (b.effects.truthy(PBEffects.Battler.MagicCoat)) {             // :439
                        magicCoater = b.index;                                       // :440
                        b.effects.set(PBEffects.Battler.MagicCoat, false);           // :441
                        break;                                                       // :442
                    } else if ((b.hasActiveAbility("MAGICBOUNCE") || b.hasActiveAbility("MOERAE")
                            || b.hasActiveAbility("CLEARHEART") || b.hasActiveAbility("RADIANTRULE"))
                            && !battle.moldBreaker
                            && !b.effects.truthy(PBEffects.Battler.MagicBounce)) {   // :443-447
                        magicBouncer = b.index;                                      // :448
                        b.effects.set(PBEffects.Battler.MagicBounce, true);          // :449
                        break;                                                       // :450
                    }
                }
            }
            // Get the number of hits
            int numHits = fx.pbNumHits(move, user, targets);                         // :455
            // Process each hit in turn
            for (int i = 0; i < numHits; i++) {                                      // :458
                if (magicCoater >= 0 || magicBouncer >= 0) break;                    // :459
                boolean success = pbProcessMoveHit(self, move, user, targets, i, skipAccuracyCheck);   // :460
                if (!success) {                                                      // :461
                    if (i == 0 && targets.size > 0) {                                // :462
                        boolean hasFailed = false;                                   // :463
                        for (Battler t : targets) {                                  // :464
                            if (t.damageState.protectedFlag) continue;               // :465
                            hasFailed = t.damageState.unaffected;                    // :466
                            if (!t.damageState.unaffected) break;                    // :467
                        }
                        user.lastMoveFailed = hasFailed;                             // :469
                    }
                    break;                                                           // :471
                }
                realNumHits += 1;                                                    // :473
                if (user.fainted()) break;                                           // :474
                if ("SLEEP".equals(user.status) || "FROZEN".equals(user.status)) break;   // :475
                // NOTE: If a multi-hit move becomes disabled partway through, the rest of the hits continue as normal.
                // All targets are fainted
                // Don't stop using the move if Dragon Darts could still hit something
                boolean anyUnfainted = false;
                for (Battler t : targets) if (!t.fainted()) anyUnfainted = true;
                boolean dartsContinue = "17C".equals(move.function()) && realNumHits < numHits
                        && !battle.pbAllFainted(user.idxOpposingSide());
                if (!anyUnfainted && !dartsContinue) break;                          // :481
            }
            // Battle Arena only - attack is successful
            battle.successStates[user.index].useState = 2;                           // :484
            if (targets.size > 0) {                                                  // :485
                battle.successStates[user.index].typeMod = 0;                        // :486
                for (Battler b : targets) {                                          // :487
                    if (b.damageState.unaffected) continue;                          // :488
                    battle.successStates[user.index].typeMod += b.damageState.typeMod;   // :489
                }
            }
            // Effectiveness message for multi-hit moves
            // NOTE: No move is both multi-hit and multi-target.
            if (numHits > 1) {                                                       // :495
                if (fx.pbDamagingMove(move)) {                                       // :496
                    for (Battler b : targets) {                                      // :497
                        if (b.damageState.unaffected || b.damageState.substitute) continue;   // :498
                        MoveUsage.pbEffectivenessMessage(user, b, targets.size);     // :499
                    }
                }
                if (realNumHits == 1) {                                              // :502
                    battle.display("击中了1次！");                                     // :503
                } else if (realNumHits > 1) {                                        // :504
                    battle.display("击中了" + realNumHits + "次！");                    // :505
                }
            }
            // Magic Coat's bouncing back (move has targets)
            for (Battler b : targets) {                                              // :509
                if (b.fainted()) continue;                                           // :510
                if (!b.damageState.magicCoat && !b.damageState.magicBounce) continue;    // :511
                if (b.damageState.magicBounce) battle.showAbilitySplash(b);          // :512
                battle.display(b.pbThis() + "弹回了" + move.name() + "！");             // :513
                if (b.damageState.magicBounce) battle.hideAbilitySplash(b);          // :514
                Object[] newChoice = choice.clone();                                 // :515
                newChoice[3] = user.index;                                           // :516
                Array<Battler> newTargets = BattlerTargeting.pbFindTargets(self, newChoice, move, b);   // :517
                newTargets = BattlerTargeting.pbChangeTargets(self, move, b, newTargets);                // :518
                boolean success = pbProcessMoveHit(self, move, b, newTargets, 0, false);                 // :519
                if (!success) b.lastMoveFailed = true;                               // :520
                for (Battler otherB : targets) {                                     // :521
                    if (otherB != null && otherB.fainted()) otherB.pbFaint();
                }
                if (user.fainted()) user.pbFaint();                                  // :522
            }
            // Magic Coat's bouncing back (move has no targets)
            if (magicCoater >= 0 || magicBouncer >= 0) {                             // :525
                Battler mc = battle.battlerAt(magicCoater >= 0 ? magicCoater : magicBouncer);   // :526
                if (!mc.fainted()) {                                                 // :527
                    user.lastMoveFailed = true;                                      // :528
                    if (magicBouncer >= 0) battle.showAbilitySplash(mc);             // :529
                    battle.display(mc.pbThis() + "弹回了" + move.name() + "！");        // :530
                    if (magicBouncer >= 0) battle.hideAbilitySplash(mc);             // :531
                    boolean success = pbProcessMoveHit(self, move, mc, new Array<Battler>(), 0, false);   // :532
                    if (!success) mc.lastMoveFailed = true;                          // :533
                    for (Battler b : targets) {                                      // :534
                        if (b != null && b.fainted()) b.pbFaint();
                    }
                    if (user.fainted()) user.pbFaint();                              // :535
                }
            }
            // Move-specific effects after all hits
            for (Battler b : targets) fx.pbEffectAfterAllHits(move, user, b);        // :539
            // Faint if 0 HP
            for (Battler b : targets) {                                              // :541
                if (b != null && b.fainted()) b.pbFaint();
            }
            if (user.fainted()) user.pbFaint();                                      // :542
            // External/general effects after all hits. Eject Button, Shell Bell, etc.
            BattlerHitEffects.pbEffectsAfterMove(battle, user, targets, move, realNumHits);   // :544
            for (Battler b : battle.allBattlers()) {                                 // :545
                b.droppedBelowHalfHP = false;                                        // :546
                b.statsDropped = false;                                              // :547
            }
        }
        // End effect of Mold Breaker
        battle.moldBreaker = false;                                                  // :551
        // Gain Exp
        battle.pbGainExp();                                                          // :553
        // Battle Arena only - update skills
        for (Battler b : battle.eachBattler()) battle.successStates[b.index].updateSkill();   // :555
        // Shadow Pokémon triggering Hyper Mode
        if (!":None".equals(battle.choices(self.index)[0])) self.pbHyperMode();      // :557 Not if self is replaced
        // End of move usage
        self.pbEndTurn(choice);                                                      // :559
        // Instruct
        for (Battler b : battle.eachBattler()) {                                     // :561
            if (!b.effects.truthy(PBEffects.Battler.Instruct)) continue;             // :562
            b.effects.set(PBEffects.Battler.Instruct, false);                        // :563
            int idxMove = -1;                                                        // :564
            for (int i = 0; i < b.pokemon.moves.size; i++) {                         // :565 eachMoveWithIndex
                BattleMove m = b.moveSlot(i);
                if (m != null && m.internalName() != null && m.internalName().equals(b.lastMoveUsed)) idxMove = i;
            }
            if (idxMove < 0) continue;                                               // :566
            int oldLastRoundMoved = b.lastRoundMoved;                                // :567
            battle.display(b.pbThis() + "使用了" + user.pbThis(true) + "指示的招式！");   // :568
            b.effects.set(PBEffects.Battler.Instructed, true);                       // :570
            b.pbUseMoveSimple(b.lastMoveUsed, b.lastRegularMoveTarget, idxMove, false);   // :571
            b.effects.set(PBEffects.Battler.Instructed, false);                      // :572
            b.lastRoundMoved = oldLastRoundMoved;                                    // :574
            battle.pbJudge();                                                        // :575
            if (battle.decision > 0) return;                                         // :576
        }
        // Dancer
        if (!self.effects.truthy(PBEffects.Battler.Dancer) && !user.lastMoveFailed && realNumHits > 0
                && !move.snatched() && magicCoater < 0 && battle.pbCheckGlobalAbility("DANCER") != null
                && fx.danceMove(move)) {                                             // :579-581
            Array<Battler> dancers = new Array<>();                                  // :582
            for (Battler b : battle.wiringFieldedBySpeed()) {                        // :583
                if (b.index != user.index && b.hasActiveAbility("DANCER")) dancers.add(b);   // :584
            }
            while (dancers.size > 0) {                                               // :586
                Battler nextUser = dancers.pop();                                    // :587
                int oldLastRoundMoved = nextUser.lastRoundMoved;                     // :588
                // NOTE: Petal Dance being used because of Dancer shouldn't lock the Dancer into using that move.
                int oldOutrage = nextUser.effects.intVal(PBEffects.Battler.Outrage); // :592
                if (nextUser.effects.intVal(PBEffects.Battler.Outrage) > 0) {        // :593
                    nextUser.effects.add(PBEffects.Battler.Outrage, 1);
                }
                int oldCurrentMove = nextUser.currentMove;                           // :594
                int preTarget = (Integer) choice[3];                                 // :595
                if (nextUser.opposes(user) || !nextUser.opposes(preTarget)) preTarget = user.index;   // :596
                battle.showAbilitySplash(nextUser);                                  // :597 (delay=true only pauses the scene)
                battle.hideAbilitySplash(nextUser);                                  // :598
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                 // :599
                    battle.display(nextUser.pbThis() + "与" + nextUser.abilityName() + "都跳起了舞！");   // :600-601
                }
                nextUser.effects.set(PBEffects.Battler.Dancer, true);                // :604
                nextUser.pbUseMoveSimple(move.internalName(), preTarget, -1, true);  // :605
                nextUser.effects.set(PBEffects.Battler.Dancer, false);               // :606
                nextUser.lastRoundMoved = oldLastRoundMoved;                         // :608
                nextUser.effects.set(PBEffects.Battler.Outrage, oldOutrage);         // :609
                nextUser.currentMove = oldCurrentMove;                               // :610
                battle.pbJudge();                                                    // :611
                if (battle.decision > 0) return;                                     // :612
            }
        }
        for (Battler b : battle.eachBattler()) {                                     // :615
            if (battle.field.effects.intVal(PBEffects.Field.TrickRoom) == 0) continue;   // :616
            if (!b.pbCanLowerStatStage(PBStats.SPEED, b)) continue;                  // :617
            if (!b.hasActiveItem("ROOMSERVICE")) continue;                           // :618
            b.pbLowerStatStageByCause(PBStats.SPEED, 1, b, b.itemName());            // :619
            b.pbConsumeItem();                                                       // :620
        }
    }

    /** {@code undrowsesUser?} (Arceus:218): Wild Charge / Spark / Volt Tackle. */
    private static boolean undrowsesUser(BattleMove move) {
        String n = move.internalName();
        return "WILDCHARGE".equals(n) || "SPARK".equals(n) || "VOLTTACKLE".equals(n);
    }

    // ------------------------------------------------------------------
    // Attack a single target (:627-809)
    // ------------------------------------------------------------------

    /** {@code pbProcessMoveHit(move,user,targets,hitNum,skipAccuracyCheck)} (:627-809). */
    public static boolean pbProcessMoveHit(Battler self, BattleMove move, Battler user, Array<Battler> targets,
                                           int hitNum, boolean skipAccuracyCheck) {
        Battle battle = self.battle;
        MoveEffect fx = MoveEffectRegistry.of(move.function());
        if (user.fainted()) return false;                                            // :628
        // For two-turn attacks being used in a single turn
        fx.pbInitialEffect(move, user, targets, hitNum);                             // :630
        int numTargets = 0;                                                          // :631 Number of targets that are affected by this hit
        // Count a hit for Parental Bond (if it applies)
        if (user.effects.intVal(PBEffects.Battler.ParentalBond) > 0) {               // :633
            user.effects.add(PBEffects.Battler.ParentalBond, -1);
        }
        // Redirect Dragon Darts other hits
        if ("17C".equals(move.function()) && battle.pbSideSize(targets.get(0).index) > 1 && hitNum > 0) {   // :635
            targets = BattlerTargeting.pbChangeTargets(self, move, user, targets, 1);                       // :636
        }
        for (Battler b : targets) b.damageState.resetPerHit();                       // :638
        // Accuracy check (accuracy/evasion calc)
        if (hitNum == 0 || fx.successCheckPerHit(move)) {                            // :640
            for (Battler b : targets) {                                              // :641
                if (b.damageState.unaffected) continue;                              // :642
                if (self.pbSuccessCheckPerHit(move, user, b, skipAccuracyCheck)) {   // :643
                    numTargets += 1;                                                 // :644
                } else {
                    b.damageState.missed = true;                                     // :646
                    b.damageState.unaffected = true;                                 // :647
                }
            }
            // If failed against all targets
            if (targets.size > 0 && numTargets == 0 && !fx.worksWithNoTargets(move)) {   // :651
                for (Battler b : targets) {                                          // :652
                    if (!b.damageState.missed || b.damageState.magicCoat) continue;  // :653
                    self.pbMissMessage(move, user, b);                               // :654
                }
                fx.pbCrashDamage(move, user);                                        // :656
                user.pbItemHPHealCheck(0, false);                                    // :657
                // Blunder Policy
                if (user.hasActiveItem("BLUNDERPOLICY") && user.effects.truthy(PBEffects.Battler.BlunderPolicy)
                        && targets.get(0).effects.intVal(PBEffects.Battler.TwoTurnAttack) == 0
                        && !"070".equals(move.function()) && hitNum == 0) {          // :659-660
                    if (user.pbCanRaiseStatStage(PBStats.SPEED, user, null)) {       // :661 (the plugin passes self where a move goes)
                        self.pbRaiseStatStageByCause(PBStats.SPEED, 2, user, self.itemName(), true, false);   // :662
                        user.pbConsumeItem();                                        // :663
                    }
                }
                self.pbCancelMoves();                                                // :666
                return false;                                                        // :667
            }
        }
        // If we get here, this hit will happen and do something
        //---------------------------------------------------------------------------
        // Calculate damage to deal
        if (fx.pbDamagingMove(move)) {                                               // :673
            for (Battler b : targets) {                                              // :674
                if (b.damageState.unaffected) continue;                              // :675
                // Check whether Substitute/Disguise will absorb the damage
                MoveUsage.pbCheckDamageAbsorption(fx, move, user, b);                // :677
                // Calculate the damage against b
                fx.pbCalcDamage(move, user, b, targets.size);                        // :682 Stored in damageState.calcDamage
                // Lessen damage dealt because of False Swipe/Endure/etc.
                MoveUsage.pbReduceDamage(fx, move, user, b);                         // :684 Stored in damageState.hpLost
            }
        }
        // Show move animation (for this hit)
        fx.pbShowAnimation(move, move.id(), user, targets, hitNum, true);            // :688
        // Type-boosting Gem consume animation/message
        if (user.effects.stringVal(PBEffects.Battler.GemConsumed) != null && hitNum == 0) {     // :690 GemConsumed>0 (an item name here; 0 = none)
            // NOTE: The consume animation and message for Gems are shown now, but the actual removal of the item happens in pbEffectsAfterMove.
            battle.commonAnimation("UseItem", user);                                 // :693
            PbsData.Item gem = battle.pbs().item(user.effects.stringVal(PBEffects.Battler.GemConsumed));
            battle.display((gem == null ? "" : gem.name) + "增强了" + move.name() + "的威力！");   // :694-695
        }
        // Messages about missed target(s) (relevant for multi-target moves only)
        for (Battler b : targets) {                                                  // :698
            if (!b.damageState.missed) continue;                                     // :699
            self.pbMissMessage(move, user, b);                                       // :700
            // Blunder Policy (also activates if only one target is missed)
            if (user.hasActiveItem("BLUNDERPOLICY") && user.effects.truthy(PBEffects.Battler.BlunderPolicy)
                    && b.effects.intVal(PBEffects.Battler.TwoTurnAttack) == 0
                    && !"070".equals(move.function()) && hitNum == 0) {              // :702-703
                if (user.pbCanRaiseStatStage(PBStats.SPEED, user, null)) {           // :704
                    self.pbRaiseStatStageByCause(PBStats.SPEED, 2, user, self.itemName(), true, false);   // :705
                    user.pbConsumeItem();                                            // :706
                }
            }
        }
        // Deal the damage (to all allies first simultaneously, then all foes simultaneously)
        if (fx.pbDamagingMove(move)) {                                               // :712
            // This just changes the HP amounts and does nothing else
            for (Battler b : targets) {                                              // :714
                if (b.damageState.unaffected) continue;                              // :715
                MoveUsage.pbInflictHPDamage(b);                                      // :716
            }
            // Animate the hit flashing and HP bar changes
            MoveUsage.pbAnimateHitAndHPLost(user, targets);                          // :719
        }
        // Self-Destruct/Explosion's damaging and fainting of user
        if (hitNum == 0) fx.pbSelfKO(move, user);                                    // :722
        if (user.fainted()) user.pbFaint();                                          // :723
        if (fx.pbDamagingMove(move)) {                                               // :724
            for (Battler b : targets) {                                              // :725
                if (b.damageState.unaffected) continue;                              // :726
                // NOTE: This method is also used for the OKHO special message.
                fx.pbHitEffectivenessMessages(move, user, b, targets.size);          // :728
                // Record data about the hit for various effects' purposes
                MoveUsage.pbRecordDamageLost(fx, move, user, b);                     // :730
            }
            // Close Combat/Superpower's stat-lowering, Flame Burst's splash damage, and Incinerate's berry destruction
            for (Battler b : targets) {                                              // :734
                if (b.damageState.unaffected) continue;                              // :735
                fx.pbEffectWhenDealingDamage(move, user, b);                         // :736
            }
            // Ability/item effects such as Static/Rocky Helmet, and Grudge, etc.
            for (Battler b : targets) {                                              // :739
                if (b.damageState.unaffected) continue;                              // :740
                BattlerHitEffects.pbEffectsOnMakingHit(battle, move, user, b);       // :741
            }
            // Disguise/Endure/Sturdy/Focus Sash/Focus Band messages
            for (Battler b : targets) {                                              // :744
                if (b.damageState.unaffected) continue;                              // :745
                MoveUsage.pbEndureKOMessage(b);                                      // :746
            }
            // HP-healing held items (checks all battlers rather than just targets because Flame Burst's splash damage affects non-targets)
            for (Battler b : battle.wiringFieldedBySpeed()) b.pbItemHPHealCheck(0, false);   // :750
            // Animate battlers fainting (checks all battlers rather than just targets)
            for (Battler b : battle.wiringFieldedBySpeed()) {                        // :753
                if (b != null && b.fainted()) b.pbFaint();
            }
        }
        battle.pbJudgeCheckpoint(user, move);                                        // :755
        // Main effect (recoil/drain, etc.)
        for (Battler b : targets) {                                                  // :757
            if (b.damageState.unaffected) continue;                                  // :758
            fx.pbEffectAgainstTarget(move, user, b);                                 // :759
        }
        fx.pbEffectGeneral(move, user);                                              // :761
        for (Battler b : targets) {                                                  // :762
            if (b != null && b.fainted()) b.pbFaint();
        }
        if (user.fainted()) user.pbFaint();                                          // :763
        // Additional effect
        if (!user.hasActiveAbility("SHEERFORCE")) {                                  // :765
            for (Battler b : targets) {                                              // :766
                if (b.damageState.calcDamage == 0) continue;                         // :767
                int chance = fx.pbAdditionalEffectChance(move, user, b, 0);          // :768
                if (chance <= 0) continue;                                           // :769
                if (battle.pbRandom(100) < chance) {                                 // :770
                    fx.pbAdditionalEffect(move, user, b);                            // :771
                }
            }
        }
        // ====== 地魔之剑 / 海魔之雨的易伤效果 (:776-784: no code, the effect is set in pbAdditionalEffect) ======
        // Make the target flinch (because of an item/ability)
        for (Battler b : targets) {                                                  // :786
            if (b.fainted()) continue;                                               // :787
            if (b.damageState.calcDamage == 0 || b.damageState.substitute) continue; // :788
            int chance = fx.pbFlinchChance(move, user, b);                           // :789
            if (chance <= 0) continue;                                               // :790
            if (battle.pbRandom(100) < chance) {                                     // :791
                b.pbFlinch(user);                                                    // :793
            }
        }
        // Message for and consuming of type-weakening berries
        // NOTE: The "consume held item" animation for type-weakening berries occurs during pbCalcDamage above, but the message about it only shows here.
        for (Battler b : targets) {                                                  // :800
            if (b.damageState.unaffected) continue;                                  // :801
            if (!b.damageState.berryWeakened) continue;                              // :802
            battle.display(b.itemName() + "削弱了" + b.pbThis(true) + "的威力！");        // :803
            b.pbConsumeItem();                                                       // :804
        }
        for (Battler b : targets) {                                                  // :806
            if (b != null && b.fainted()) b.pbFaint();
        }
        if (user.fainted()) user.pbFaint();                                          // :807
        return true;                                                                 // :808
    }
}
