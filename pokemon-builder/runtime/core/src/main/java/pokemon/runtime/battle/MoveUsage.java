package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.battle.movefx.MoveEffect;

/**
 * Stage 5 / 2b: the damage-dealing half of {@code class PokeBattle_Move}
 * ({@code Move_Usage.rb:166-436}), transcribed line by line.
 *
 * <p>These methods are instance methods of the plugin's move object. In this
 * runtime a move is the pair {@link BattleMove} (its data) + {@link MoveEffect}
 * (its function-code strategy), so each method takes both; nothing in
 * {@code Move_Usage:166-436} is overridden by any {@code PokeBattle_Move_XXX}
 * class except {@code pbHitEffectivenessMessages} (Move_Effects_000-07F:2518,
 * which stays in its own strategy). Pass the strategy that owns the move:
 * {@code MoveEffectRegistry.of(move.function())}, or
 * {@code MoveEffectRegistry.confusion()} for confusion damage.</p>
 *
 * <p>Scene calls ({@code @battle.scene.pbHitAndHPLossAnimation}, {@code pbBGMPlay})
 * become {@link Battle.RoundEvent}s, the event stream the battle screen plays.</p>
 */
public final class MoveUsage {

    private MoveUsage() {
    }

    // ------------------------------------------------------------------
    // Weaken the damage dealt (doesn't actually change a battler's HP)
    // ------------------------------------------------------------------

    /** {@code pbCheckDamageAbsorption(user,target)} (Move_Usage:166-194). */
    public static void pbCheckDamageAbsorption(MoveEffect fx, BattleMove move, Battler user, Battler target) {
        Battle battle = target.battle;
        // Substitute will take the damage
        if (target.effects.intVal(PBEffects.Battler.Substitute) > 0 && !fx.ignoresSubstitute(move, user)
                && (user == null || user.index != target.index)) {                       // :168-169
            target.damageState.substitute = true;                                        // :170
            return;                                                                      // :171
        }
        // Disguise will take the damage
        if (!battle.moldBreaker && target.isSpecies("MIMIKYU")
                && target.form() == 0 && "DISGUISE".equals(target.ability)) {            // :174-175
            target.damageState.disguise = true;                                          // :176
            return;                                                                      // :177
        }
        // Ice Face will take the damage
        if (!battle.moldBreaker && target.isSpecies("EISCUE")
                && target.form() == 0 && "ICEFACE".equals(target.ability)
                && fx.physicalMove(move, null)) {                                        // :181-182
            target.damageState.iceface = true;                                           // :183
            return;                                                                      // :184
        }
        if (!battle.moldBreaker && (target.isSpecies("KABLIT")
                || target.isSpecies("FLAMBLOOM")
                || target.isSpecies("BLAZEPHEX"))
                && target.form() == 0 && "FLAMEVEIL".equals(target.ability)) {          // :186-190
            target.damageState.flameveil = true;                                         // :191
            return;                                                                      // :192
        }
    }

    /** {@code pbReduceDamage(user,target)} (Move_Usage:196-248). */
    public static void pbReduceDamage(MoveEffect fx, BattleMove move, Battler user, Battler target) {
        Battle battle = target.battle;
        int damage = target.damageState.calcDamage;                                      // :197
        // Substitute takes the damage
        if (target.damageState.substitute) {                                             // :199
            if (damage > target.effects.intVal(PBEffects.Battler.Substitute)) {          // :200
                damage = target.effects.intVal(PBEffects.Battler.Substitute);
            }
            target.damageState.hpLost = damage;                                          // :201
            target.damageState.totalHPLost += damage;                                    // :202
            return;                                                                      // :203
        }
        // Disguise takes the damage
        if (target.damageState.disguise) return;                                         // :206
        // Ice Face takes the damage
        if (target.damageState.iceface) return;                                          // :208
        // 火焰外壳
        if (target.damageState.flameveil) return;                                        // :210
        // Target takes the damage
        if (target.damageState.bossHP == 0) {                                            // :212
            int rank = target.pokemon.battleRank;                                        // :213
            if (rank > 1 && target.hp > 0) {                                             // :214
                int partHp = target.maxHp() / 2;                                         // :215
                int overflow = target.hp % partHp;                                       // :216
                if (overflow == 0) overflow = partHp;                                    // :217
                if (damage >= overflow) {                                                // :218
                    damage = overflow;                                                   // :219
                    target.damageState.bossHP += 1;                                      // :220
                }
            }
        }
        if (damage >= target.hp) {                                                       // :224
            damage = target.hp;                                                          // :225
            // Survive a lethal hit with 1 HP effects
            if (fx.nonLethal(move, user, target)) {                                      // :227
                damage -= 1;                                                             // :228
            } else if (target.effects.truthy(PBEffects.Battler.Endure)) {                // :229
                target.damageState.endured = true;                                       // :230
                damage -= 1;                                                             // :231
            } else if (damage == target.maxHp()) {                                       // :232
                if (target.hasActiveAbility("STURDY") && !battle.moldBreaker) {          // :233
                    target.damageState.sturdy = true;                                    // :234
                    damage -= 1;                                                         // :235
                } else if (target.hasActiveItem("FOCUSSASH") && target.hp == target.maxHp()) {   // :236
                    target.damageState.focusSash = true;                                 // :237
                    damage -= 1;                                                         // :238
                } else if (target.hasActiveItem("FOCUSBAND") && battle.pbRandom(100) < 10) {     // :239
                    target.damageState.focusBand = true;                                 // :240
                    damage -= 1;                                                         // :241
                }
            }
        }
        if (damage < 0) damage = 0;                                                      // :245
        target.damageState.hpLost = damage;                                              // :246
        target.damageState.totalHPLost += damage;                                        // :247
    }

    // ------------------------------------------------------------------
    // Change the target's HP by the amount calculated above
    // ------------------------------------------------------------------

    /** {@code pbInflictHPDamage(target)} (Move_Usage:253-259). */
    public static void pbInflictHPDamage(Battler target) {
        if (target.damageState.substitute) {                                             // :254
            target.effects.add(PBEffects.Battler.Substitute, -target.damageState.hpLost);   // :255
        } else {
            target.setHp(target.hp - target.damageState.hpLost);                         // :257 target.hp -= hpLost
        }
    }

    // ------------------------------------------------------------------
    // Animate the damage dealt, including lowering the HP
    // ------------------------------------------------------------------

    /**
     * {@code pbAnimateHitAndHPLost(user,targets)} (Move_Usage:264-292). The scene
     * call {@code pbHitAndHPLossAnimation(animArray)} (:280) is one
     * {@code HIT} round event per side, carrying every target of that side.
     */
    public static void pbAnimateHitAndHPLost(Battler user, Array<Battler> targets) {
        Battle battle = user.battle;
        // Animate allies first, then foes
        Array<Battle.HitEvent> animArray = new Array<>();                                // :266
        for (int side = 0; side < 2; side++) {                                           // :267 allies first, then foes
            for (Battler b : targets) {                                                  // :268
                if (b.damageState.unaffected || b.damageState.hpLost == 0) continue;     // :269
                if ((side == 0 && b.opposes(user)) || (side == 1 && !b.opposes(user))) continue;   // :270
                int oldHP = b.hp + b.damageState.hpLost;                                 // :271
                int effectiveness = 0;                                                   // :273
                if (PBTypes.resistant(b.damageState.typeMod)) {                          // :274
                    effectiveness = 1;
                } else if (PBTypes.superEffective(b.damageState.typeMod)) {              // :275
                    effectiveness = 2;
                }
                animArray.add(new Battle.HitEvent(b.index, oldHP, b.hp, effectiveness)); // :277
            }
            if (animArray.size > 0) {                                                    // :279
                battle.roundEvents.add(Battle.RoundEvent.hit(animArray.toArray(Battle.HitEvent.class)));   // :280
                animArray.clear();                                                       // :281
            }
        }
        if (user.pbOwnedByPlayer() && user.allAllies().size > 1) return;                 // :284
        if (!user.pbOwnedByPlayer() && user.allOpposing().size > 1) return;              // :285
        for (Battler t : targets) {                                                      // :286
            if (t.pbOwnedByPlayer() && !t.fainted() && t.hp <= t.maxHp() / 4) {          // :287
                battle.roundEvents.add(Battle.RoundEvent.bgm("Battle low HP"));          // :288 pbBGMPlay
                break;                                                                   // :289
            }
        }
    }

    // ------------------------------------------------------------------
    // Messages upon being hit
    // ------------------------------------------------------------------

    /** {@code pbEffectivenessMessage(user,target,numTargets=1)} (Move_Usage:297-320). */
    public static void pbEffectivenessMessage(Battler user, Battler target, int numTargets) {
        Battle battle = target.battle;
        if (target.damageState.disguise) return;                                         // :298
        if (target.damageState.iceface) return;                                          // :299
        if (target.damageState.flameveil) return;                                        // :300
        if (PBTypes.superEffective(target.damageState.typeMod)) {                        // :301
            if ("ADAPTARMOR".equals(target.ability)
                    || "ETRTNALIGHT".equals(target.ability)
                    || "STARFISSURE".equals(target.ability)) {                           // :302-304
                battle.display(target.name() + "因为" + target.abilityName() + "而受到常规伤害！");   // :305
            } else {
                if (numTargets > 1) {                                                    // :307
                    battle.display("这对" + target.pbThis(true) + "非常有效！");           // :308
                } else {
                    battle.display("这非常有效！");                                        // :310
                }
            }
        } else if (PBTypes.notVeryEffective(target.damageState.typeMod)) {               // :313
            if (numTargets > 1) {                                                        // :314
                battle.display("这对" + target.pbThis(true) + "不是很有效……");            // :315
            } else {
                battle.display("这不是很有效……");                                         // :317
            }
        }
    }

    /** {@code pbHitEffectivenessMessages(user,target,numTargets=1)} (Move_Usage:322-345). */
    public static void pbHitEffectivenessMessages(MoveEffect fx, BattleMove move, Battler user, Battler target,
                                                  int numTargets) {
        Battle battle = target.battle;
        if (target.damageState.disguise) return;                                         // :323
        if (target.damageState.iceface) return;                                          // :324
        if (target.damageState.flameveil) return;                                        // :325
        if (target.damageState.substitute) {                                             // :326
            battle.display("替身承受伤害保护了" + target.pbThis(true) + "！");              // :327
        }
        if (target.damageState.critical) {                                               // :329
            if (numTargets > 1) {                                                        // :330
                battle.display("击中了" + target.pbThis(true) + "的要害！");               // :331
            } else {
                battle.display("击中了要害！");                                            // :333
            }
            user.criticalHits += 1;                                                      // :335
        }
        // Effectiveness message, for moves with 1 hit
        if (!fx.multiHitMove(move) && user.effects.intVal(PBEffects.Battler.ParentalBond) == 0) {   // :338
            pbEffectivenessMessage(user, target, numTargets);                            // :339
        }
        if (target.damageState.substitute
                && target.effects.intVal(PBEffects.Battler.Substitute) == 0) {           // :341
            target.effects.set(PBEffects.Battler.Substitute, 0);                         // :342
            battle.display(target.pbThis() + "的替身消失了！");                            // :343
        }
    }

    /** {@code pbEndureKOMessage(target)} (Move_Usage:347-405). */
    public static void pbEndureKOMessage(Battler target) {
        Battle battle = target.battle;
        if (target.damageState.disguise) {                                               // :348
            battle.showAbilitySplash(target);                                            // :349
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                          // :350
                battle.display("画皮挡住了攻击！");                                        // :351
            } else {
                battle.display(target.pbThis() + "的画皮挡住了攻击！");                     // :353
            }
            battle.hideAbilitySplash(target);                                            // :355
            target.pbChangeFormTransform(1, target.pbThis() + "的画皮被破坏了！");          // :356
            target.pbReduceHP(target.maxHp() / 8);                                       // :357
        } else if (target.damageState.iceface) {                                         // :359
            battle.showAbilitySplash(target);                                            // :360
            target.pbChangeFormTransform(1, target.pbThis() + "变身了！");                 // :361
            battle.hideAbilitySplash(target);                                            // :362
        } else if (target.damageState.flameveil) {                                       // :364
            battle.showAbilitySplash(target);                                            // :365
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                          // :366
                battle.display("火焰外壳挡住了攻击！");                                     // :367
            } else {
                battle.display(target.pbThis() + "的火焰外壳挡住了攻击！");                  // :369
            }
            battle.hideAbilitySplash(target);                                            // :371
            target.pbChangeFormTransform(1, target.pbThis() + "的火焰外壳被破坏了！");       // :372
            // 焚火面纱破坏后的效果
            battle.showAbilitySplash(target);                                            // :374
            if (target.pbCanRaiseStatStage(PBStats.SPEED, target)) {                     // :375
                target.pbRaiseStatStage(PBStats.SPEED, 2, target);                       // :376
            }
            if (target.pbCanRaiseStatStage(PBStats.ATTACK, target)) {                    // :378
                target.pbRaiseStatStage(PBStats.ATTACK, 1, target);                      // :379
            }
            battle.hideAbilitySplash(target);                                            // :381
        } else if (target.damageState.endured) {                                         // :384
            battle.display(target.pbThis() + "挺住了！");                                  // :385
        } else if (target.damageState.sturdy) {                                          // :386
            battle.showAbilitySplash(target);                                            // :387
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                          // :388
                battle.display(target.pbThis() + "挺住了！");                              // :389
            } else {
                battle.display(target.pbThis() + "靠着结实挺住了！");                       // :391
            }
            battle.hideAbilitySplash(target);                                            // :393
        } else if (target.damageState.focusSash) {                                       // :394
            battle.commonAnimation("UseItem", target);                                   // :395
            battle.display(target.pbThis() + "靠着气势披带撑住了！");                       // :396
            target.pbConsumeItem();                                                      // :397
        } else if (target.damageState.focusBand) {                                       // :398
            battle.commonAnimation("UseItem", target);                                   // :399
            battle.display(target.pbThis() + "靠着气势头带撑住了！");                       // :400
        } else if (target.damageState.bossHP == 1) {                                     // :401
            battle.display("<c3=FFEE88,FF6600>" + target.pbThis() + "减免了溢出的伤害！</c3>");   // :402
            target.damageState.bossHP += 1;                                              // :403
        }
    }

    /**
     * {@code pbRecordDamageLost(user,target)} (Move_Usage:408-436): used by
     * Counter/Mirror Coat/Metal Burst/Revenge/Focus Punch/Bide/Assurance.
     */
    public static void pbRecordDamageLost(MoveEffect fx, BattleMove move, Battler user, Battler target) {
        int damage = target.damageState.hpLost;                                          // :409
        target.yamaskhp += damage;                                                       // :410
        // NOTE (:411-414): Hidden Power is countered by Counter in Gen 3.
        String moveType = null;                                                          // :415
        if ("090".equals(move.function())) moveType = "NORMAL";                          // :416 Hidden Power
        if (fx.physicalMove(move, moveType)) {                                           // :417
            target.effects.set(PBEffects.Battler.Counter, damage);                       // :418
            target.effects.set(PBEffects.Battler.CounterTarget, user.index);             // :419
        } else if (fx.specialMove(move, moveType)) {                                     // :420
            target.effects.set(PBEffects.Battler.MirrorCoat, damage);                    // :421
            target.effects.set(PBEffects.Battler.MirrorCoatTarget, user.index);          // :422
        }
        if (target.effects.intVal(PBEffects.Battler.Bide) > 0) {                         // :424
            target.effects.add(PBEffects.Battler.BideDamage, damage);                    // :425
            target.effects.set(PBEffects.Battler.BideTarget, user.index);                // :426
        }
        if (target.fainted()) target.damageState.fainted = true;                         // :428
        target.lastHPLost = damage;                                                      // :429 For Focus Punch
        if (damage > 0) target.tookDamage = true;                                        // :430 For Assurance
        target.lastAttacker.add(user);                                                   // :431 For Revenge (this runtime keeps the Battler, not its index)
        if (target.opposes(user)) {                                                      // :432
            target.lastHPLostFromFoe = damage;                                           // :433 For Metal Burst
            target.lastFoeAttacker.add(user);                                            // :434
        }
    }
}
