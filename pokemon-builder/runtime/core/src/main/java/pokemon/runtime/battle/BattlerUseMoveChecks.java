package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 5 / 2c: {@code Battler_UseMove_SuccessChecks:1-247} and
 * {@code :600-661} - the checks made before a move is actually used - plus
 * their small status helpers. ({@code :383-598}, the target-side checks, are
 * {@link BattleSuccessChecks}.)
 *
 * <p>Ruby's {@code choice} array is an {@code Object[]}:
 * {@code [action(":UseMove"..), slot(Integer), BattleMove, target(Integer)]} -
 * the same shape {@link Battle#choices(int)} emits. Each method is the body of
 * the {@code PokeBattle_Battler} method of the same name, with {@code self}
 * as the first argument; {@link Battler} delegates to them.</p>
 *
 * <h2>Registered deviations</h2>
 * <ul>
 * <li>{@code @pokemon.foreign?(@battle.pbPlayer)} (:181) compares
 *     {@code @trainerID} and {@code @ot}; this runtime's {@code Pokemon} has only
 *     the original trainer's name, so only the name is compared (the same test
 *     {@code pbGainExpOne}'s outsider check already uses).</li>
 * <li>{@code pbHyperModeObedience} (:185, Pokemon_ShadowPokemon:409) -
 *     {@code inHyperMode?} is {@code false} here (Shadow Pokemon are not
 *     modelled), which makes it return true at its first line.</li>
 * </ul>
 */
public final class BattlerUseMoveChecks {

    private BattlerUseMoveChecks() {
    }

    /** {@code (commandPhase) ? @battle.pbDisplayPaused(msg) : @battle.pbDisplay(msg)}. */
    private static void show(Battler self, boolean commandPhase, String msg) {
        if (commandPhase) {
            self.battle.displayPaused(msg);
        } else {
            self.battle.display(msg);
        }
    }

    /** {@code PBMoves.getName(id)}. */
    private static String moveName(Battler self, int id) {
        PbsData.Move data = self.battle.pbs().moveById(id);
        return data == null ? "" : data.name;
    }

    private static boolean isBerry(Battler self, String item) {
        PbsData.Item data = self.battle.pbs().item(item);
        return data != null && data.isBerry();                                   // PItem_Items:99-102
    }

    // ------------------------------------------------------------------
    // pbCanChooseMove? (:10-129)
    // ------------------------------------------------------------------

    /** {@code pbCanChooseMove?(move,commandPhase,showMessages=true,specialUsage=false)} (:10-129). */
    public static boolean pbCanChooseMove(Battler self, BattleMove move, boolean commandPhase,
                                          boolean showMessages, boolean specialUsage) {
        Battle battle = self.battle;
        MoveEffect fx = MoveEffectRegistry.of(move.function());
        // Disable
        if (self.effects.intVal(PBEffects.Battler.DisableMove) == move.id() && !specialUsage) {   // :12
            if (showMessages) {
                show(self, commandPhase, self.pbThis() + "的" + move.name() + "被定住了！");        // :14-15
            }
            return false;                                                        // :17
        }
        // Stuff Cheeks
        if ("183".equals(move.function())
                && (self.item != null && !self.item.isEmpty() && !isBerry(self, self.item))) {   // :20
            if (showMessages) {
                show(self, commandPhase, self.pbThis()
                        + " can't use that move because it doesn't have any berry!");            // :22-23
            }
            return false;                                                        // :25
        }
        // Heal Block
        if (self.effects.intVal(PBEffects.Battler.HealBlock) > 0 && fx.healingMove(move)) {      // :28
            if (showMessages) {
                show(self, commandPhase, self.pbThis() + "因回复封锁而无法使用" + move.name() + "！");   // :30-31
            }
            return false;                                                        // :33
        }
        // Gravity
        if (battle.field.effects.intVal(PBEffects.Field.Gravity) > 0 && fx.unusableInGravity(move)) {   // :36
            if (showMessages) {
                show(self, commandPhase, self.pbThis() + "因重力而无法使用" + move.name() + "！");     // :38-39
            }
            return false;                                                        // :41
        }
        // Throat Chop
        if (self.effects.intVal(PBEffects.Battler.ThroatChop) > 0 && fx.soundMove(move)) {       // :44
            if (showMessages) {
                show(self, commandPhase, self.pbThis() + "因极端痛苦而无法使用" + move.name() + "！");   // :46-47
            }
            return false;                                                        // :49
        }
        // Choice Band
        if (self.effects.intVal(PBEffects.Battler.ChoiceBand) >= 0) {            // :52
            if (self.hasActiveItem(new String[] {"CHOICEBAND", "CHOICESPECS", "CHOICESCARF"})
                    && self.pbHasMove(self.effects.intVal(PBEffects.Battler.ChoiceBand))) {      // :53-54
                if (move.id() != self.effects.intVal(PBEffects.Battler.ChoiceBand)) {            // :55
                    if (showMessages) {
                        show(self, commandPhase, self.itemName() + "使"
                                + moveName(self, self.effects.intVal(PBEffects.Battler.ChoiceBand))
                                + "变为唯一可用的招式！");                          // :57-59
                    }
                    return false;                                                // :61
                }
            } else {
                self.effects.set(PBEffects.Battler.ChoiceBand, -1);              // :64
            }
        }
        // Gorilla Tactics
        if (self.effects.intVal(PBEffects.Battler.GorillaTactics) >= 0) {        // :68
            if (self.hasActiveAbility("GORILLATACTICS")) {                       // :69
                if (move.id() != self.effects.intVal(PBEffects.Battler.GorillaTactics)) {        // :70
                    if (showMessages) {
                        show(self, commandPhase, self.abilityName() + " allows the use of only "
                                + moveName(self, self.effects.intVal(PBEffects.Battler.GorillaTactics)) + " !");   // :72-73
                    }
                    return false;                                                // :75
                }
            } else {
                self.effects.set(PBEffects.Battler.GorillaTactics, -1);          // :78
            }
        }
        // Taunt
        if (self.effects.intVal(PBEffects.Battler.Taunt) > 0 && fx.statusMove(move)) {           // :82
            if (showMessages) {
                show(self, commandPhase, self.pbThis() + "因为被挑衅了，\n所以无法使用" + move.name() + "！");   // :84-85
            }
            return false;                                                        // :87
        }
        // Torment
        boolean torment = self.effects.truthy(PBEffects.Battler.Torment);
        boolean instructed = self.effects.truthy(PBEffects.Battler.Instructed);
        if (torment && !instructed && move.internalName() != null
                && move.internalName().equals(self.lastMoveUsed)
                && move.id() != self.struggle(battle.pbs()).id()) {              // :90-91 move.id==@lastMoveUsed && !=struggle.id
            if (showMessages) {
                show(self, commandPhase, self.pbThis() + "因无理取闹而不能使用相同的招式！");     // :93-94
            }
            return false;                                                        // :96
        }
        // 巨力锤
        if (!torment && !instructed
                && self.effects.intVal(PBEffects.Battler.SuccessiveMove) == move.id()) {         // :99-100
            if (showMessages) {
                show(self, commandPhase, move.name() + "不能被连续使用2次！");        // :102-103
            }
            return false;                                                        // :105
        }
        // Imprison
        for (Battler b : battle.eachOtherSideBattler(self.index)) {              // :108
            if (!b.effects.truthy(PBEffects.Battler.Imprison) || !b.pbHasMove(move.id())) continue;   // :109
            if (showMessages) {
                show(self, commandPhase, self.pbThis() + "不能使用被封印的" + move.name() + "！");   // :111-112
            }
            return false;                                                        // :114
        }
        // Assault Vest (prevents choosing status moves but doesn't prevent executing them)
        if (self.hasActiveItem("ASSAULTVEST") && fx.statusMove(move) && commandPhase) {          // :118
            if (showMessages) {
                show(self, commandPhase, self.itemName() + "使得变化招式无法使用！");       // :120-122
            }
            return false;                                                        // :124
        }
        // Belch
        if (!fx.pbCanChooseMove(move, self, commandPhase, showMessages)) return false;           // :127
        return true;                                                             // :128
    }

    // ------------------------------------------------------------------
    // Obedience check (:134-239)
    // ------------------------------------------------------------------

    /** {@code pbObedienceCheck?(choice)} (:136-189). */
    public static boolean pbObedienceCheck(Battler self, Object[] choice) {
        Battle battle = self.battle;
        if (self.usingMultiTurnAttack()) return true;                            // :137
        if (!":UseMove".equals(choice[0])) return true;                          // :138
        if (!battle.internalBattle) return true;                                 // :139
        if (!self.pbOwnedByPlayer()) return true;                                // :140 @battle.pbOwnedByPlayer?(@index)
        // 海地之魔物不听话控制
        if (self.isSpecies("SEAMONSTER") && !self.hasActiveItem("EOSINORB")) {   // :142
            return pbDisobey(self, choice, 1);                                   // :143
        }
        if (self.isSpecies("GROUNDMONSTER") && !self.hasActiveItem("ULTRAMARINEORB")) {          // :145
            return pbDisobey(self, choice, 1);                                   // :146
        }
        if (self.isSpecies("SKYMONSTER") && !self.hasActiveItem("BLACKGREENORB")) {              // :148
            return pbDisobey(self, choice, 1);                                   // :149
        }
        if (self.isSpecies("HAXORUS") && !self.hasActiveItem("TYRANTCREST") && self.form() == 2) {   // :151
            return pbDisobey(self, choice, 1);                                   // :152
        }
        // 雷吉奇卡斯携带道具不听话
        if (self.isSpecies("REGIGIGAS") && self.hasActiveItem("REGISPELL")) {    // :155
            if (!battle.gameSwitches.test(198)) {                                // :157
                return pbDisobey(self, choice, 1);                               // :158
            }
        }
        boolean disobedient = false;                                             // :161
        int index = -1;                                                          // :164
        if (battle.gameSwitches.test(12)) {                                      // :165
            for (int i = 8; i <= 15; i++) {                                      // :166
                if (!battle.badges.contains(i)) {                                // :167
                    index = i + 1;                                               // :168
                    break;                                                       // :169
                }
            }
        } else {
            for (int i = 0; i <= 7; i++) {                                       // :173
                if (!battle.badges.contains(i)) {                                // :174
                    index = i;                                                   // :175
                    break;                                                       // :176
                }
            }
        }
        int badgeLevel = Battle.maxLevel(index);                                 // :180
        if (self.isForeign() && self.level() > badgeLevel) {                     // :181
            int a = (self.level() + badgeLevel) * battle.pbRandom(256) / 256;    // :182
            disobedient |= (a >= badgeLevel);                                    // :183
        }
        disobedient |= !self.pbHyperModeObedience((BattleMove) choice[2]);       // :185
        if (!disobedient) return true;                                           // :186
        return pbDisobey(self, choice, badgeLevel);                              // :188
    }

    /** {@code pbDisobey(choice,badgeLevel)} (:192-239). */
    public static boolean pbDisobey(Battler self, Object[] choice, int badgeLevel) {
        Battle battle = self.battle;
        BattleMove move = (BattleMove) choice[2];                                // :193
        self.effects.set(PBEffects.Battler.Rage, false);                         // :195
        // Do nothing if using Snore/Sleep Talk
        if ("SLEEP".equals(self.status)
                && MoveEffectRegistry.of(move.function()).usableWhenAsleep(move)) {              // :197
            battle.display(self.pbThis() + "装作没听见并决定继续睡觉……");             // :198
            return false;                                                        // :199
        }
        int b = (self.level() + badgeLevel) * battle.pbRandom(256) / 256;        // :201
        // Use another move
        if (b < badgeLevel) {                                                    // :203
            battle.display(self.pbThis() + "装作没听见……");                        // :204
            if (!battle.pbCanShowFightMenu(self.index)) return false;            // :205
            Array<Integer> otherMoves = new Array<>();                           // :206
            for (int i = 0; i < self.pokemon.moves.size; i++) {                  // :207 eachMoveWithIndex
                if (i == (Integer) choice[1]) continue;                          // :208
                if (battle.pbCanChooseMove(self.index, i, false, false)) otherMoves.add(i);      // :209
            }
            if (otherMoves.size == 0) return false;                              // :211 No other move to use; do nothing
            int newChoice = otherMoves.get(battle.pbRandom(otherMoves.size));    // :212
            choice[1] = newChoice;                                               // :213
            choice[2] = self.moveSlot(newChoice);                                // :214
            choice[3] = -1;                                                      // :215
            return true;                                                         // :216
        }
        int c = self.level() - badgeLevel;                                       // :218
        int r = battle.pbRandom(256);                                            // :219
        // Fall asleep
        if (r < c && self.pbCanSleep(self, false)) {                             // :221
            self.pbSleepSelf(self.pbThis() + "开始睡觉了……");                       // :222
            return false;                                                        // :223
        }
        // Hurt self in confusion
        r -= c;                                                                  // :226
        if (r < c && !"SLEEP".equals(self.status)) {                             // :227
            self.pbConfusionDamage(self.pbThis() + "不肯听从命令！\n在混乱中伤害了自己！");    // :228
            return false;                                                        // :229
        }
        // Show refusal message and do nothing
        switch (battle.pbRandom(4)) {                                            // :232
            case 0: battle.display(self.pbThis() + "不听从命令！"); break;           // :233
            case 1: battle.display(self.pbThis() + "看向其他地方……"); break;         // :234
            case 2: battle.display(self.pbThis() + "正在闲逛……"); break;            // :235
            case 3: battle.display(self.pbThis() + "假装没听到……"); break;          // :236
            default: break;
        }
        return false;                                                            // :238
    }

    // ------------------------------------------------------------------
    // pbTryUseMove (:246-377)
    // ------------------------------------------------------------------

    /** {@code pbTryUseMove(choice,move,specialUsage,skipAccuracyCheck)} (:246-377). */
    public static boolean pbTryUseMove(Battler self, Object[] choice, BattleMove move,
                                       boolean specialUsage, boolean skipAccuracyCheck) {
        Battle battle = self.battle;
        MoveEffect fx = MoveEffectRegistry.of(move.function());
        // Check whether it's possible for self to use the given move
        if (!self.pbCanChooseMove(move, false, true, specialUsage)) {            // :250
            self.lastMoveFailed = true;                                          // :251
            return false;                                                        // :252
        }
        // Check whether it's possible for self to do anything at all
        if (self.effects.intVal(PBEffects.Battler.SkyDrop) >= 0) {               // :255 Intentionally no message here
            return false;                                                        // :257
        }
        if (self.effects.intVal(PBEffects.Battler.HyperBeam) > 0) {              // :259 Intentionally before Truant
            battle.display(self.pbThis() + "必须休息一下！");                       // :260
            return false;                                                        // :261
        }
        if ((Integer) choice[1] == -2) {                                         // :263 Battle Palace
            battle.display(self.pbThis() + "似乎无法使用力量！");                    // :264
            return false;                                                        // :265
        }
        // Skip checking all applied effects that could make self fail doing something
        if (skipAccuracyCheck) return true;                                      // :268
        // Check status problems and continue their effects/cure them
        if ("SLEEP".equals(self.status)) {                                       // :271
            self.setStatusCount(self.statusCount - 1);                                               // :272
            if (self.statusCount <= 0) {                                         // :273
                self.pbCureStatus();                                             // :274
            } else {
                self.pbContinueStatus();                                         // :276
                if (!fx.usableWhenAsleep(move)) {                                // :277 Snore/Sleep Talk
                    self.lastMoveFailed = true;                                  // :278
                    return false;                                                // :279
                }
            }
        } else if ("FROZEN".equals(self.status)) {                               // :282
            if (!fx.thawsUser(move)) {                                           // :283
                if (battle.pbRandom(100) < 20) {                                 // :284
                    self.pbCureStatus();                                         // :285
                } else {
                    self.pbContinueStatus();                                     // :287
                    self.lastMoveFailed = true;                                  // :288
                    return false;                                                // :289
                }
            }
        }
        // Obedience check
        if (!pbObedienceCheck(self, choice)) return false;                       // :294
        // 断刃鏖杀 (:295-302 is commented out in the plugin)
        // Truant
        if (self.hasActiveAbility("TRUANT")) {                                   // :304
            self.effects.set(PBEffects.Battler.Truant, !self.effects.truthy(PBEffects.Battler.Truant));   // :305
            if (!self.effects.truthy(PBEffects.Battler.Truant)) {                // :306 True means loafing, but was just inverted
                battle.showAbilitySplash(self);                                  // :307
                battle.display(self.pbThis() + "正在闲逛……");                      // :308
                self.lastMoveFailed = true;                                      // :309
                battle.hideAbilitySplash(self);                                  // :310
                return false;                                                    // :311
            }
        }
        // Flinching
        if (self.effects.truthy(PBEffects.Battler.Flinch)) {                     // :315
            if (self.pokemon.battleRank > 2) {                                   // :316
                self.effects.set(PBEffects.Battler.Flinch, false);               // :317
                battle.playCry(self.pokemon);                                    // :318-319 pbCryFile/pbSEPlay
                battle.display("<c3=FFEE88,FF6600>" + self.pbThis() + "凭借它的力量\n而无所畏惧！</c3>");   // :320
                return true;                                                     // :321
            }
            battle.display(self.pbThis() + "畏缩了，无法行动！");                    // :323
            if (self.abilityActive()) {                                          // :324
                BattleHandlers.triggerAbilityOnFlinch(self.ability, self, battle);   // :325
            }
            self.lastMoveFailed = true;                                          // :327
            return false;                                                        // :328
        }
        // Confusion
        if (self.effects.intVal(PBEffects.Battler.Confusion) > 0) {              // :331
            self.effects.add(PBEffects.Battler.Confusion, -1);                   // :332
            if (self.effects.intVal(PBEffects.Battler.Confusion) <= 0) {         // :333
                self.pbCureConfusion();                                          // :334
                battle.display(self.pbThis() + "解除了混乱！");                     // :335
            } else {
                battle.commonAnimation("Confusion", self);                       // :337
                battle.display(self.pbThis() + "混乱了！");                        // :338
                int threshold = Battle.NEWEST_BATTLE_MECHANICS ? 33 : 50;        // :339
                if (battle.pbRandom(100) < threshold) {                          // :340
                    self.pbConfusionDamage("混乱中伤害了自己！");                    // :341
                    self.lastMoveFailed = true;                                  // :342
                    return false;                                                // :343
                }
            }
        }
        // Paralysis
        if ("PARALYSIS".equals(self.status)) {                                   // :348
            if (battle.pbRandom(100) < 25) {                                     // :349
                self.pbContinueStatus();                                         // :350
                self.lastMoveFailed = true;                                      // :351
                return false;                                                    // :352
            }
        }
        // Drowsy
        if ("DROWSY".equals(self.status)) {                                      // :356
            self.setStatusCount(self.statusCount - 1);                                               // :357
            int range = (battle.field.weather == PBWeather.Snow) ? 50 : 25;      // :358
            if (battle.pbRandom(100) < range) {                                  // :359
                self.pbContinueStatus();                                         // :360
                self.lastMoveFailed = true;                                      // :361
                return false;                                                    // :362
            }
        }
        // Infatuation
        if (self.effects.intVal(PBEffects.Battler.Attract) >= 0) {               // :366
            battle.commonAnimation("Attract", self);                             // :367
            battle.display(self.pbThis() + "爱上了"
                    + battle.battlerAt(self.effects.intVal(PBEffects.Battler.Attract)).pbThis(true) + "！");   // :368-369
            if (battle.pbRandom(100) < 50) {                                     // :370
                battle.display(self.pbThis() + "坠入了爱河！");                     // :371
                self.lastMoveFailed = true;                                      // :372
                return false;                                                    // :373
            }
        }
        return true;                                                             // :376
    }

    // ------------------------------------------------------------------
    // Per-hit success check (:604-661)
    // ------------------------------------------------------------------

    /** {@code pbSuccessCheckPerHit(move,user,target,skipAccuracyCheck)} (:604-647). */
    public static boolean pbSuccessCheckPerHit(Battle battle, BattleMove move, Battler user, Battler target,
                                               boolean skipAccuracyCheck) {
        MoveEffect fx = MoveEffectRegistry.of(move.function());
        // Two-turn attacks can't fail here in the charging turn
        if (user.effects.intVal(PBEffects.Battler.TwoTurnAttack) > 0) return true;       // :606
        // Lock-On
        if (user.effects.intVal(PBEffects.Battler.LockOn) > 0
                && user.effects.intVal(PBEffects.Battler.LockOnPos) == target.index) return true;   // :608-609
        // Toxic
        if (fx.pbOverrideSuccessCheckPerHit(move, user, target)) return true;    // :611
        boolean miss = false;                                                    // :612
        boolean hitsInvul = false;
        // No Guard
        if (user.hasActiveAbility("NOGUARD") || target.hasActiveAbility("NOGUARD")) hitsInvul = true;   // :614-615
        // Future Sight
        if (battle.futureSight) hitsInvul = true;                                // :617
        // Helping Hand
        if ("09C".equals(move.function())) hitsInvul = true;                     // :619
        if (!hitsInvul) {                                                        // :620
            // Semi-invulnerable moves
            if (target.effects.intVal(PBEffects.Battler.TwoTurnAttack) > 0) {    // :622
                if (target.inTwoTurnAttack("0C9", "0CC", "0CE")) {               // :623 Fly, Bounce, Sky Drop
                    if (!fx.hitsFlyingTargets(move)) miss = true;                // :624
                } else if (target.inTwoTurnAttack("0CA")) {                      // :625 Dig
                    if (!fx.hitsDiggingTargets(move)) miss = true;               // :626
                } else if (target.inTwoTurnAttack("0CB")) {                      // :627 Dive
                    if (!fx.hitsDivingTargets(move)) miss = true;                // :628
                } else if (target.inTwoTurnAttack("0CD", "14D")) {               // :629 Shadow Force, Phantom Force
                    miss = true;                                                 // :630
                }
            }
            if (target.effects.intVal(PBEffects.Battler.SkyDrop) >= 0
                    && target.effects.intVal(PBEffects.Battler.SkyDrop) != user.index) {          // :633-634
                if (!fx.hitsFlyingTargets(move)) miss = true;                    // :635
            }
        }
        if (!miss) {                                                             // :638
            // Called by another move
            if (skipAccuracyCheck) return true;                                  // :640
            // Accuracy check
            if (fx.pbAccuracyCheck(move, user, target)) return true;             // :642 Includes Counter/Mirror Coat
        }
        // Missed
        return false;                                                            // :646
    }

    /** {@code pbMissMessage(move,user,target)} (:652-661). */
    public static void pbMissMessage(Battle battle, BattleMove move, Battler user, Battler target) {
        MoveEffect fx = MoveEffectRegistry.of(move.function());
        int tar = fx.pbTarget(move, user);                                       // :653
        if (PBTargets.multipleTargets(tar)) {                                    // :654
            battle.display(target.pbThis() + "避开了攻击!");                       // :655
        } else if (target.effects.intVal(PBEffects.Battler.TwoTurnAttack) > 0) { // :656
            battle.display(target.pbThis() + "避开了攻击!");                       // :657
        } else if (!fx.pbMissMessage(move, user, target)) {                      // :658
            battle.display(user.pbThis() + "的攻击没有命中！");                     // :659
        }
    }
}
