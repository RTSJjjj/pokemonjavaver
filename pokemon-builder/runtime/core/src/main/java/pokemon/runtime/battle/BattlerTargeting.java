package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 5 / 2d: {@code Battler_UseMove_Targeting:1-272} - who a move's user is
 * and which battlers it targets. Each method is the body of the
 * {@code PokeBattle_Battler} method of the same name with {@code self} as the
 * first argument; {@link Battler} delegates to them.
 *
 * <p>{@code choice[3]} (the pre-chosen target index) is an {@code Integer}
 * inside the shared {@code Object[] choice}.</p>
 *
 * <h2>Registered</h2>
 * <ul>
 * <li>{@code :171/:187} {@code @battle.choices[user.index][4]>0} (the priority
 *     {@code pbCalculatePriority} saved - docs/stage4-wiring-queue.md C6) does not
 *     exist yet; {@link #quickGuardAppliesTo(Battler)} is false until it does.
 *     Same registration as {@link BattleSuccessChecks}.</li>
 * <li>{@code :113} {@code next if !@battle.choices[b.index][3] == targets} parses
 *     as {@code (!choice) == targets}, which is always false, so it never skips.
 *     Transcribed as written (no skip).</li>
 * <li>{@code :255}/{@code :266} pass {@code nearOnly} in the {@code move}
 *     parameter of {@code pbAddTarget}; the plugin's own call shape is kept
 *     ({@code move} is {@code null} here, {@code nearOnly} is always true).</li>
 * </ul>
 */
public final class BattlerTargeting {

    private BattlerTargeting() {
    }

    private static Battler battlerAt(Battle battle, int index) {
        return index >= 0 ? battle.battlerAt(index) : null;
    }

    /** {@code move.target} as the plugin's {@code PBTargets} constant. */
    private static int targetOf(BattleMove move) {
        return move.target() == null ? PBTargets.NearOther : PBTargets.fromName(move.target());
    }

    private static boolean quickGuardAppliesTo(Battler user) {
        return false;                                       // see class javadoc: @battle.choices[..][4]
    }

    /** {@code b.type1} / {@code b.type2} (PokeBattle_Battler @type1/@type2): the first and second type, {@code type2 == type1} for a single-type battler. */
    private static String t1(Battler b) {
        if (b.type1 != null) return b.type1;                                         // changed by pbChangeTypes
        Array<String> t = b.pokemon.types();
        return t.size > 0 ? t.get(0) : null;
    }

    private static String t2(Battler b) {
        if (b.type1 != null) return b.type2;
        Array<String> t = b.pokemon.types();
        return t.size > 1 ? t.get(1) : t1(b);
    }

    /** {@code pbGetMoveData(id,MOVE_FUNCTION_CODE)} of an internal name. */
    private static String functionOf(Battle battle, String internalName) {
        PbsData.Move data = battle.pbs().move(internalName);
        return data == null ? null : data.function;
    }

    // ------------------------------------------------------------------
    // Get move's user (:5-30)
    // ------------------------------------------------------------------

    /** {@code pbFindUser(_choice,_move)} (:5-7). */
    public static Battler pbFindUser(Battler self, Object[] choice, BattleMove move) {
        return self;                                                                 // :6
    }

    /** {@code pbChangeUser(choice,move,user)} (:9-30): Snatch. */
    public static Battler pbChangeUser(Battler self, Object[] choice, BattleMove move, Battler user) {
        Battle battle = self.battle;
        MoveEffect fx = MoveEffectRegistry.of(move.function());
        move.setSnatched(false);                                                     // :11
        if (fx.canSnatch(move)) {                                                    // :12
            Battler newUser = null;                                                  // :13
            int strength = 100;
            for (Battler b : battle.eachBattler()) {                                 // :14
                if (b.effects.intVal(PBEffects.Battler.Snatch) == 0
                        || b.effects.intVal(PBEffects.Battler.Snatch) >= strength) continue;   // :15-16
                if (b.effects.intVal(PBEffects.Battler.SkyDrop) >= 0) continue;      // :17
                newUser = b;                                                         // :18
                strength = b.effects.intVal(PBEffects.Battler.Snatch);               // :19
            }
            if (newUser != null) {                                                   // :21
                user = newUser;                                                      // :22
                user.effects.set(PBEffects.Battler.Snatch, 0);                       // :23
                move.setSnatched(true);                                              // :24
                battle.moldBreaker = user.hasMoldBreaker();                          // :25
                choice[3] = -1;                                                      // :26 Clear pre-chosen target
            }
        }
        return user;                                                                 // :29
    }

    // ------------------------------------------------------------------
    // Get move's default target(s) (:35-96)
    // ------------------------------------------------------------------

    /** {@code pbFindTargets(choice,move,user)} (:35-96). */
    public static Array<Battler> pbFindTargets(Battler self, Object[] choice, BattleMove move, Battler user) {
        Battle battle = self.battle;
        MoveEffect fx = MoveEffectRegistry.of(move.function());
        int preTarget = (Integer) choice[3];                                         // :36 A target that was already chosen
        Array<Battler> targets = new Array<>();                                      // :37
        // Get list of targets
        int targeting = fx.pbTarget(move, user);                                     // :39
        // Expanding Force
        if ("192".equals(move.function())
                && battle.field.terrain == PBBattleTerrains.Psychic && !user.airborne()) {   // :41-42
            for (Battler b : battle.eachOtherSideBattler(user.index)) {              // :43
                pbAddTarget(self, targets, user, b, move, false, false);
            }
        }
        switch (targeting) {                                                         // :45 Curse can change its target type
            case PBTargets.NearAlly: {                                               // :46
                Battler targetBattler = battlerAt(battle, preTarget);                // :47
                if (!pbAddTarget(self, targets, user, targetBattler, move, true, false)) {       // :48
                    pbAddTargetRandomAlly(self, targets, user, move, true);          // :49
                }
                break;
            }
            case PBTargets.UserOrNearAlly: {                                         // :51
                Battler targetBattler = battlerAt(battle, preTarget);                // :52
                if (!pbAddTarget(self, targets, user, targetBattler, move, true, true)) {        // :53
                    pbAddTarget(self, targets, user, user, move, true, true);        // :54
                }
                break;
            }
            case PBTargets.NearFoe:                                                  // :56
            case PBTargets.NearOther: {
                Battler targetBattler = battlerAt(battle, preTarget);                // :57
                if (!pbAddTarget(self, targets, user, targetBattler, move, true, false)) {       // :58
                    if (preTarget >= 0 && !user.opposes(preTarget)) {                // :59
                        pbAddTargetRandomAlly(self, targets, user, move, true);      // :60
                    } else {
                        pbAddTargetRandomFoe(self, targets, user, move, true);       // :62
                    }
                }
                break;
            }
            case PBTargets.AllNearFoes:                                              // :65
                for (Battler b : battle.eachOtherSideBattler(user.index)) {          // :66
                    pbAddTarget(self, targets, user, b, move, true, false);
                }
                break;
            case PBTargets.RandomNearFoe:                                            // :67
                pbAddTargetRandomFoe(self, targets, user, move, true);               // :68
                break;
            case PBTargets.AllNearOthers:                                            // :69
                for (Battler b : battle.eachBattler()) {                             // :70
                    pbAddTarget(self, targets, user, b, move, true, false);
                }
                break;
            case PBTargets.Other: {                                                  // :71
                Battler targetBattler = battlerAt(battle, preTarget);                // :72
                if (!pbAddTarget(self, targets, user, targetBattler, move, false, false)) {      // :73
                    if (preTarget >= 0 && !user.opposes(preTarget)) {                // :74
                        pbAddTargetRandomAlly(self, targets, user, move, false);     // :75
                    } else {
                        pbAddTargetRandomFoe(self, targets, user, move, false);      // :77
                    }
                }
                break;
            }
            case PBTargets.UserAndAllies:                                            // :80
                pbAddTarget(self, targets, user, user, move, true, true);            // :81
                for (Battler b : battle.eachSameSideBattler(user.index)) {           // :82
                    pbAddTarget(self, targets, user, b, move, false, true);
                }
                break;
            case PBTargets.AllFoes:                                                  // :83
                for (Battler b : battle.eachOtherSideBattler(user.index)) {          // :84
                    pbAddTarget(self, targets, user, b, move, false, false);
                }
                break;
            case PBTargets.AllBattlers:                                              // :85
                for (Battler b : battle.eachBattler()) {                             // :86
                    pbAddTarget(self, targets, user, b, move, false, true);
                }
                break;
            default:
                // Used by Counter/Mirror Coat/Metal Burst/Bide
                fx.pbAddTarget(move, targets, user);                                 // :89 Move-specific pbAddTarget
                break;
        }
        if (targets.size > 0) {                                                      // :91
            if (!battle.moldBreaker) {                                               // :92
                battle.moldBreaker = user.hasMoldBreaker()
                        || (fx.statusMove(move) && user.hasActiveAbility("MYCELIUMMIGHT"));
            }
            if (targets.get(0).hasActiveItem("ABILITYSHIELD")) battle.moldBreaker = false;   // :93
        }
        return targets;                                                              // :95
    }

    // ------------------------------------------------------------------
    // Redirect attack to another target (:101-238)
    // ------------------------------------------------------------------

    /** {@code pbChangeTargets(move,user,targets,dragondarts=-1)} (:101-217). */
    public static Array<Battler> pbChangeTargets(Battler self, BattleMove move, Battler user,
                                                 Array<Battler> targets, int dragondarts) {
        Battle battle = self.battle;
        MoveEffect fx = MoveEffectRegistry.of(move.function());
        int targetType = fx.pbTarget(move, user);                                    // :102
        if (battle.switching) return targets;                                        // :103 For Pursuit interrupting a switch
        if (fx.cannotRedirect(move)) return targets;                                 // :104
        if (!"17C".equals(move.function())
                && (!PBTargets.canChooseOneFoeTarget(targetType) || targets.size != 1)) return targets;   // :105
        // Stalwart / Propeller Tail
        boolean allySwitched = false;                                                // :107
        int ally = -1;                                                               // :108
        Array<Battler> opposing = new Array<>();
        user.eachOpposing(opposing::add);
        for (Battler b : opposing) {                                                 // :109
            if (!"120".equals(functionOf(battle, b.lastMoveUsed))) continue;         // :110
            if (!PBTargets.oneTarget(fx.pbTarget(move, user))) continue;             // :111
            if (!self.hasActiveAbility("STALWART") && !self.hasActiveAbility("PROPELLERTAIL")
                    && !"182".equals(move.function())) continue;                     // :112
            // :113 next if !@battle.choices[b.index][3] == targets  - always false (see class javadoc)
            if (b.effects.intVal(PBEffects.Battler.SwitchedAlly) == -1) continue;    // :114
            allySwitched = !allySwitched;                                            // :115
            ally = b.effects.intVal(PBEffects.Battler.SwitchedAlly);                 // :116
            b.effects.set(PBEffects.Battler.SwitchedAlly, -1);                       // :117
        }
        if (allySwitched && ally >= 0) {                                             // :119
            targets = new Array<>();                                                 // :120
            pbAddTarget(self, targets, user, battle.battlerAt(ally), move,
                    !PBTargets.canChooseDistantTarget(targetOf(move)), false);       // :121
            return targets;                                                          // :122
        }
        if (user.hasActiveAbility("STALWART") || user.hasActiveAbility("PROPELLERTAIL")) return targets;   // :124
        if ("182".equals(move.function())) return targets;                           // :125
        Array<Battler> priority = battle.wiringFieldedBySpeed();                     // :126 pbPriority(true)
        boolean nearOnly = !PBTargets.canChooseDistantTarget(targetOf(move));        // :127
        // Spotlight (takes priority over Follow Me/Rage Powder/Lightning Rod/Storm Drain)
        Battler newTarget = null;                                                    // :129
        int strength = 100;                                                          // Lower strength takes priority
        for (Battler b : priority) {                                                 // :130
            if (b.fainted() || b.effects.intVal(PBEffects.Battler.SkyDrop) >= 0) continue;           // :131
            if (b.effects.intVal(PBEffects.Battler.Spotlight) == 0
                    || b.effects.intVal(PBEffects.Battler.Spotlight) >= strength) continue;          // :132-133
            if (!b.opposes(user)) continue;                                          // :134
            if (nearOnly && !b.near(user)) continue;                                 // :135
            newTarget = b;                                                           // :136
            strength = b.effects.intVal(PBEffects.Battler.Spotlight);                // :137
        }
        if (newTarget != null) {                                                     // :139
            targets = new Array<>();                                                 // :141
            pbAddTarget(self, targets, user, newTarget, move, nearOnly, false);      // :142
            return targets;                                                          // :143
        }
        // Follow Me/Rage Powder (takes priority over Lightning Rod/Storm Drain)
        newTarget = null;                                                            // :146
        strength = 100;
        for (Battler b : priority) {                                                 // :147
            if (b.fainted() || b.effects.intVal(PBEffects.Battler.SkyDrop) >= 0) continue;           // :148
            if (b.effects.truthy(PBEffects.Battler.RagePowder) && !user.affectedByPowder(false)) continue;   // :149
            if (b.effects.intVal(PBEffects.Battler.FollowMe) == 0
                    || b.effects.intVal(PBEffects.Battler.FollowMe) >= strength) continue;           // :150-151
            if (!b.opposes(user)) continue;                                          // :152
            if (nearOnly && !b.near(user)) continue;                                 // :153
            newTarget = b;                                                           // :154
            strength = b.effects.intVal(PBEffects.Battler.FollowMe);                 // :155
        }
        if (newTarget != null) {                                                     // :157
            targets = new Array<>();                                                 // :159
            pbAddTarget(self, targets, user, newTarget, move, nearOnly, false);      // :160
            return targets;                                                          // :161
        }
        // Dragon Darts redirection
        if (dragondarts >= 0) {                                                      // :164
            Array<Battler> newTargets = new Array<>();                               // :165
            boolean neednewtarget = false;                                           // :166
            // Check if first use has to be redirected
            if (dragondarts == 0) {                                                  // :168
                for (Battler b : targets) {                                          // :169
                    if (!b.effects.truthy(PBEffects.Battler.Protect)
                            && !(b.effects.truthy(PBEffects.Side.QuickGuard) && quickGuardAppliesTo(user))
                            && !b.effects.truthy(PBEffects.Battler.SpikyShield)
                            && !b.effects.truthy(PBEffects.Battler.BanefulBunker)
                            && !b.effects.truthy(PBEffects.Battler.Obstruct)
                            && b.effects.intVal(PBEffects.Battler.TwoTurnAttack) <= 0
                            && !fx.pbImmunityByAbility(move, user, b)
                            && !PBTypes.ineffective(battle.pbs(), move.type(), t1(b), t2(b), null)
                            && fx.pbAccuracyCheck(move, user, b)) {
                        continue;                                                    // :170-178 next if (nothing blocks the hit)
                    }
                    neednewtarget = true;                                            // :179 next neednewtarget=true
                }
            }
            // Redirect first use if necessary or get another target on each consecutive use
            if (neednewtarget || dragondarts == 1) {                                 // :183
                Array<Battler> allies = new Array<>();
                targets.get(0).eachAlly(allies::add);                                // :184
                for (Battler b : allies) {
                    if (b.index == user.index && dragondarts == 1) continue;         // :185 Don't attack yourself on the second hit.
                    if (b.effects.truthy(PBEffects.Battler.Protect)
                            || (b.effects.truthy(PBEffects.Side.QuickGuard) && quickGuardAppliesTo(user))
                            || b.effects.truthy(PBEffects.Battler.SpikyShield)
                            || b.effects.truthy(PBEffects.Battler.BanefulBunker)
                            || b.effects.truthy(PBEffects.Battler.Obstruct)
                            || b.effects.intVal(PBEffects.Battler.TwoTurnAttack) > 0
                            || fx.pbImmunityByAbility(move, user, b)
                            || PBTypes.ineffective(battle.pbs(), move.type(), t1(b), t2(b), null)
                            || !fx.pbAccuracyCheck(move, user, b)) {                 // :186-194
                        continue;                                                    // :186 next if ...
                    }
                    newTargets.add(b);                                               // :195
                    b.damageState.unaffected = false;                                // :196
                    break;                                                           // :198
                }
            }
            // Final target
            if (newTargets.size != 0) targets = newTargets;                          // :202
            // Reduce PP if the new target has Pressure
            if (targets.get(0).hasActiveAbility(new String[] {"PRESSURE", "CALAMITYABYSSAL", "CALAMITYINFERNAL"})) {   // :204
                user.pbReducePP(move);                                               // :205
            }
        }
        // Lightning Rod
        targets = pbChangeTargetByAbility(self, "LIGHTNINGROD", "ELECTRIC", move, user, targets, priority, nearOnly);   // :209
        // Storm Drain
        targets = pbChangeTargetByAbility(self, "STORMDRAIN", "WATER", move, user, targets, priority, nearOnly);        // :211
        if (targets.size > 0) {                                                      // :212
            if (!battle.moldBreaker) {                                               // :213
                battle.moldBreaker = user.hasMoldBreaker()
                        || (fx.statusMove(move) && user.hasActiveAbility("MYCELIUMMIGHT"));
            }
            if (targets.get(0).hasActiveItem("ABILITYSHIELD")) battle.moldBreaker = false;   // :214
        }
        return targets;                                                              // :216
    }

    /** {@code pbChangeTargets(move,user,targets)} with the default {@code dragondarts=-1}. */
    public static Array<Battler> pbChangeTargets(Battler self, BattleMove move, Battler user, Array<Battler> targets) {
        return pbChangeTargets(self, move, user, targets, -1);
    }

    /** {@code pbChangeTargetByAbility(...)} (:219-238). */
    public static Array<Battler> pbChangeTargetByAbility(Battler self, String drawingAbility, String drawnType,
                                                         BattleMove move, Battler user, Array<Battler> targets,
                                                         Array<Battler> priority, boolean nearOnly) {
        Battle battle = self.battle;
        if (!drawnType.equals(move.calcType())) return targets;                      // :220
        if (targets.get(0).hasActiveAbility(drawingAbility)) return targets;         // :221
        for (Battler b : priority) {                                                 // :222
            if (b.index == user.index || b.index == targets.get(0).index) continue;  // :223
            if (!b.hasActiveAbility(drawingAbility)) continue;                       // :224
            if (nearOnly && !b.near(user)) continue;                                 // :225
            battle.showAbilitySplash(b);                                             // :226
            targets.clear();                                                         // :227
            pbAddTarget(self, targets, user, b, move, nearOnly, false);              // :228
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                      // :229
                battle.display(b.pbThis() + "吸收了攻击！");                           // :230
            } else {
                battle.display(b.pbThis() + "的" + b.abilityName() + "吸收了攻击！");   // :232
            }
            battle.hideAbilitySplash(b);                                             // :234
            break;                                                                   // :235
        }
        return targets;                                                              // :237
    }

    // ------------------------------------------------------------------
    // Register target (:243-271)
    // ------------------------------------------------------------------

    /** {@code pbAddTarget(targets,user,target,move,nearOnly=true,allowUser=false)} (:243-249). */
    public static boolean pbAddTarget(Battler self, Array<Battler> targets, Battler user, Battler target,
                                      BattleMove move, boolean nearOnly, boolean allowUser) {
        if (target == null) return false;                                            // :244
        if (target.fainted() && (move == null || !MoveEffectRegistry.of(move.function()).cannotRedirect(move))) {
            return false;                                                            // :244
        }
        if (!(allowUser && user == target) && nearOnly && !user.near(target)) return false;   // :245
        for (Battler b : targets) {                                                  // :246
            if (b.index == target.index) return true;                                // :246 Already added
        }
        targets.add(target);                                                         // :247
        return true;                                                                 // :248
    }

    /** {@code pbAddTargetRandomAlly(targets,user,_move,nearOnly=true)} (:251-260). */
    public static void pbAddTargetRandomAlly(Battler self, Array<Battler> targets, Battler user,
                                             BattleMove move, boolean nearOnly) {
        Battle battle = self.battle;
        Array<Battler> choices = new Array<>();                                      // :252
        Array<Battler> allies = new Array<>();
        user.eachAlly(allies::add);
        for (Battler b : allies) {                                                   // :253
            if (nearOnly && !user.near(b)) continue;                                 // :254
            pbAddTarget(self, choices, user, b, null, true, false);                  // :255 (move slot carries nearOnly in the plugin)
        }
        if (choices.size > 0) {                                                      // :257
            pbAddTarget(self, targets, user, choices.get(battle.pbRandom(choices.size)), null, true, false);   // :258
        }
    }

    /** {@code pbAddTargetRandomFoe(targets,user,_move,nearOnly=true)} (:262-271). */
    public static void pbAddTargetRandomFoe(Battler self, Array<Battler> targets, Battler user,
                                            BattleMove move, boolean nearOnly) {
        Battle battle = self.battle;
        Array<Battler> choices = new Array<>();                                      // :263
        Array<Battler> foes = new Array<>();
        user.eachOpposing(foes::add);
        for (Battler b : foes) {                                                     // :264
            if (nearOnly && !user.near(b)) continue;                                 // :265
            pbAddTarget(self, choices, user, b, null, true, false);                  // :266
        }
        if (choices.size > 0) {                                                      // :268
            pbAddTarget(self, targets, user, choices.get(battle.pbRandom(choices.size)), null, true, false);   // :269
        }
    }
}
