package pokemon.runtime.battle;

/**
 * The helpers behind {@code AIScript_Positives}'s per-effect cases ({@code ai_advanced.c} / {@code ai_util.c}), single battle.
 * Stat limits are given as stages relative to neutral (C's {@code 6 + n} becomes {@code n}).
 *
 * <p>登记: every {@code HasUsedMoveWithEffect*} / {@code HasUsedPhazingMoveThatAffects} test (a per-battler used-move history this runtime
 * does not keep) is treated as false; {@code IsMovePredictionPhazingMove}'s "the player is assumed to have Haze" shortcut and
 * {@code gNewBS->UnburdenBoosts} / Flash Fire flags are not tracked.</p>
 */
final class AiPositiveHelpers {

    private AiPositiveHelpers() {
    }

    /** {@code stat == 0xFE}: every stat. */
    static final int ALL_STATS = -2;

    // ------------------------------------------------------------------
    // Viability increases (ai_advanced.c:1726-2098)
    // ------------------------------------------------------------------

    /** {@code INCREASE_VIABILITY(x)}: {@code min(viability + x, 255)} (ai_util.c:5715). */
    static int inc(int viability, int amount) {
        return Math.min(viability + amount, 255);
    }

    /** {@code IncreaseStatusViability(&viability,class,boost,bankAtk,bankDef)} (ai_advanced.c:1726-1790), single-battle classes. */
    static int incStatus(AiCtx ctx, int viability, int cls, int boost, Battler atk, Battler def) {
        switch (cls) {
            case AiCalc.CLASS_SWEEPER_KILL:
                break;
            case AiCalc.CLASS_SWEEPER_SETUP_STATS:
                if (!AiCalc.can2HKO(ctx, def, atk)) viability = inc(viability, 3);
                break;
            case AiCalc.CLASS_SWEEPER_SETUP_STATUS:
                viability = inc(viability, 4 + boost);
                break;
            case AiCalc.CLASS_STALL:
                viability = inc(viability, 3 + boost);
                break;
            case AiCalc.CLASS_BATON_PASS:
                if (boost >= 3) viability = inc(viability, 1);
                break;
            case AiCalc.CLASS_CLERIC:
                viability = inc(viability, 3 + boost);
                break;
            case AiCalc.CLASS_SCREENS:
            case AiCalc.CLASS_SWEEPER_SETUP_SCREENS:
                viability = inc(viability, 2 + boost);
                break;
            case AiCalc.CLASS_PHAZING:
                viability = inc(viability, 4 + boost);
                break;
            case AiCalc.CLASS_ENTRY_HAZARDS:
                viability = inc(viability, 3);
                break;
            default:
                break;
        }
        return Math.min(viability, 255);
    }

    /** {@code IncreaseStatViability(&viability,class,boost,bankAtk,bankDef,move,stat,statLimit)} (ai_advanced.c:1940-2098), single-battle classes. */
    static int incStat(AiCtx ctx, int viability, int cls, int boost, Battler atk, Battler def, BattleMove move, int stat, int statLimit) {
        if (statLimit < 6 && shouldMaxStatBuffLimit(ctx, atk, def, stat)) statLimit = 6;
        boolean setUp = shouldTryToSetUpStat(ctx, atk, def, move, stat, statLimit);
        switch (cls) {
            case AiCalc.CLASS_SWEEPER_KILL:
                if (setUp) viability = inc(viability, 3 + boost);
                else if (!move.statusMove() && (stat == PBStats.ATTACK || stat == PBStats.SPATK)) viability = inc(viability, 1);
                break;
            case AiCalc.CLASS_SWEEPER_SETUP_STATS:
                if (setUp) viability = inc(viability, 3 + boost);
                break;
            case AiCalc.CLASS_SWEEPER_SETUP_STATUS:
            case AiCalc.CLASS_STALL:
            case AiCalc.CLASS_CLERIC:
            case AiCalc.CLASS_SCREENS:
            case AiCalc.CLASS_SWEEPER_SETUP_SCREENS:
            case AiCalc.CLASS_ENTRY_HAZARDS:
                if (setUp) viability = incStatus(ctx, viability, cls, 1, atk, def);                // treat like a low-priority status move
                break;
            case AiCalc.CLASS_PHAZING:
                if (!AiCalc.moveFunctionInMoveset(atk, "051") && setUp) viability = incStatus(ctx, viability, cls, 1, atk, def);
                break;
            case AiCalc.CLASS_BATON_PASS:
                switch (stat) {
                    case PBStats.ATTACK: case PBStats.SPATK: case PBStats.SPEED: case PBStats.ACCURACY:
                        if (shouldTryToSetUpStat(ctx, atk, def, move, stat, 4)) viability = inc(viability, 5);
                        break;
                    case PBStats.DEFENSE: case PBStats.SPDEF:
                        if (shouldTryToSetUpStat(ctx, atk, def, move, stat, 4)) viability = inc(viability, 4);
                        break;
                    case PBStats.EVASION:
                        if (shouldTryToSetUpStat(ctx, atk, def, move, stat, 6)) viability = inc(viability, 6);
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return Math.min(viability, 255);
    }

    /** {@code ShouldMaxStatBuffLimit(bankAtk,bankDef,move,stat)} (ai_advanced.c:1911-1938), single battle. 登记: gLastUsedMoves history. */
    static boolean shouldMaxStatBuffLimit(AiCtx ctx, Battler atk, Battler def, int stat) {
        int defStyle = AiCalc.fightingStyle(ctx, def);
        if (AiCalc.classStall(defStyle) || defStyle == AiCalc.CLASS_BATON_PASS || defStyle == AiCalc.CLASS_CLERIC
                || defStyle == AiCalc.CLASS_SCREENS || defStyle == AiCalc.CLASS_PHAZING) {            // IsClassTeamSupport
            // the foe "didn't try to damage last turn" is not tracked; only the predicted-switch part is applied below
        }
        return ctx.predictedToSwitch(def);
    }

    /** {@code ShouldTryToSetUpStat(bankAtk,bankDef,move,stat,statLimit)} (ai_advanced.c:1801-1909), single battle. */
    static boolean shouldTryToSetUpStat(AiCtx ctx, Battler atk, Battler def, BattleMove move, int stat, int statLimit) {
        Battle battle = ctx.battle;
        if (def.hasActiveAbility("UNAWARE") && !usableNamed(ctx, atk, "STOREDPOWER", "POWERTRIP")
                && !(stat == PBStats.DEFENSE && usableNamed(ctx, atk, "BODYPRESS"))) return false;
        if (AiCalc.willFaintFromSecondaryDamage(battle, atk)) return false;
        if (stat == ALL_STATS) return true;
        if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
            if (canKnockOutWithChipDamage(ctx, def, atk)) return false;
            if (def.semiInvulnerable()) return true;
        } else {
            BattleMove foe = ctx.prediction(def);
            if (foe != null && def.semiInvulnerable() == false && AiCalc.oneOf(foe, "0C9", "0CA", "0CB", "0CC", "0CD", "0CE", "14D")) return true;
            if (stat == PBStats.SPEED && atk.stage(stat) < statLimit && !AiCalc.knocksOutXHits(ctx, foe, def, atk, 1)
                    && willBeFasterAfterSpeedBuff(atk, def, move)) return true;
            if (AiCalc.can2HKO(ctx, def, atk)) {
                if (!AiCalc.canKnockOut(ctx, def, atk)) {
                    BattleMove strongest = AiCalc.strongestMove(ctx, def, atk);
                    if (stat == PBStats.DEFENSE && strongest != null && strongest.physical()) return true;
                    if (stat == PBStats.SPDEF && strongest != null && !strongest.physical() && !strongest.statusMove()) return true;
                }
                return false;
            }
        }
        if (atk.stage(stat) >= statLimit) return false;
        if ((stat == PBStats.ATTACK || stat == PBStats.SPATK) && enduresAHitFromFullHealth(def, atk)
                && !isIncapacitated(def)) return false;                                               // 登记: MultiHitMoveWithSplitInMovesetThatAffects
        return true;
    }

    /** {@code CanKnockOutWithChipDamage(bankAtk,bankDef)} (ai_util.c:279). */
    static boolean canKnockOutWithChipDamage(AiCtx ctx, Battler atk, Battler def) {
        if (AiCalc.canKnockOut(ctx, atk, def)) return true;
        BattleMove strongest = AiCalc.strongestMove(ctx, atk, def);
        return AiCalc.finalDamage(ctx, strongest, atk, def, 1) + AiCalc.secondaryDamage(ctx.battle, def) >= def.hp;
    }

    /** {@code WillBeFasterAfterMoveSpeedBuff(bankAtk,bankDef,move)} (ai_util.c:2115): emulate one (or two) speed stages. */
    static boolean willBeFasterAfterSpeedBuff(Battler atk, Battler def, BattleMove move) {
        int increaseBy = AiCalc.oneOf(move, "030", "031", "035") || AiCalc.named(move, "GEOMANCY") ? 2 : 1;
        if (atk.hasActiveAbility("SIMPLE")) increaseBy *= 2;
        int old = atk.stages[2];
        atk.stages[2] = Math.min(old + increaseBy, 6);
        try {
            return atk.speed() >= def.speed();
        } finally {
            atk.stages[2] = old;
        }
    }

    /** {@code EnduresAHitFromFullHealth(bankDef,defAbility,atkAbility)} (ai_advanced / ai_util). */
    static boolean enduresAHitFromFullHealth(Battler def, Battler atk) {
        return def.hp == def.maxHp() && (def.hasActiveItem("FOCUSSASH")
                || (def.hasActiveAbility("STURDY") && !atk.hasMoldBreaker()));
    }

    /** {@code IsBankIncapacitated(bank)} (ai_util.c). */
    static boolean isIncapacitated(Battler b) {
        return (b.hasStatus("SLEEP") && b.statusCount > 1) || b.hasStatus("FROZEN") || b.effects.intVal(PBEffects.Battler.HyperBeam) > 0
                || (b.hasActiveAbility("TRUANT") && b.effects.truthy(PBEffects.Battler.Truant));
    }

    private static boolean usableNamed(AiCtx ctx, Battler b, String... names) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && AiCalc.named(m, names) && AiCalc.usable(ctx, b, i)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // GoodIdea / BadIdea (ai_util.c:3069-3405)
    // ------------------------------------------------------------------

    /** {@code BadIdeaToRaiseStatAgainst(bankAtk,bankDef,checkDefAbility)} (ai_util.c:3069). */
    static boolean badIdeaToRaiseStat(AiCtx ctx, Battler atk, Battler def, boolean checkDefAbility) {
        BattleMove foe = ctx.prediction(def);
        if (foe != null && AiCalc.oneOf(foe, "051", "0EB")) return true;                              // IsMovePredictionPhazingMove (Haze / Roar)
        if (foe != null && AiCalc.oneOf(foe, "003", "004") && AiCalc.hitChance(ctx.battle, def, atk, foe) >= 80
                && def.speed() < atk.speed()) return true;                                           // high-accuracy sleeping move
        for (String name : def.movesUsed) {                                                          // HasUsedPhazingMoveThatAffects
            BattleMove used = AiCalc.moveByName(ctx.battle, name);
            if (used != null && AiCalc.oneOf(used, "0EB", "0EC", "051") && AiCalc.moveFunctionInMoveset(def, used.function())) return true;
        }
        return checkDefAbility && def.hasActiveAbility("UNAWARE") && !AiCalc.willFaintFromSecondaryDamage(ctx.battle, def);
    }

    /**
     * {@code BadIdeaToRaise<Stat>Against(bankAtk,bankDef,amount,checkPartner)} (ai_util.c:3076-3250): the common check plus the foe having
     * revealed a move that lowers that stat. Function codes: Growl 042 / Charm 04B, Tail Whip 043 / 04C, String Shot 044 / Cotton Spore 04D,
     * Confide 045 / Captivate 04E, Metal Sound 046 / 04F, Sand Attack 047, Sweet Scent 048, Tickle 04A (Atk+Def), Play Nice 139, Venom Drench 140.
     */
    static boolean badIdeaToRaise(AiCtx ctx, Battler atk, Battler def, int stat, int amount) {
        if (stat == PBStats.SPEED && AiCalc.trickRoomNotEnding(ctx.battle)) return true;
        if (badIdeaToRaiseStat(ctx, atk, def, true)) return true;
        Battle b = ctx.battle;
        boolean poisoned = atk.hasStatus("POISON");
        switch (stat) {
            case PBStats.ATTACK:
                if (AiCalc.hasUsedStatusFunction(b, def, "04B")) return true;
                if (amount <= 1 && (AiCalc.hasUsedStatusFunction(b, def, "042", "04A", "139") || (poisoned && AiCalc.hasUsedStatusFunction(b, def, "140"))
                        || AiCalc.hasUsedHitFunction(b, def, 75, "042"))) return true;
                return false;
            case PBStats.DEFENSE:
                if (AiCalc.hasUsedStatusFunction(b, def, "04C")) return true;
                return amount <= 1 && (AiCalc.hasUsedStatusFunction(b, def, "043", "04A") || AiCalc.hasUsedHitFunction(b, def, 75, "043"));
            case PBStats.SPATK:
                if (AiCalc.hasUsedStatusFunction(b, def, "04E")) return true;
                return amount <= 1 && (AiCalc.hasUsedStatusFunction(b, def, "045", "139") || (poisoned && AiCalc.hasUsedStatusFunction(b, def, "140"))
                        || AiCalc.hasUsedHitFunction(b, def, 75, "045"));
            case PBStats.SPDEF:
                if (AiCalc.hasUsedStatusFunction(b, def, "04F") || AiCalc.hasUsedHitFunction(b, def, 75, "04F")) return true;
                return amount <= 1 && (AiCalc.hasUsedStatusFunction(b, def, "046") || AiCalc.hasUsedHitFunction(b, def, 75, "046"));
            case PBStats.SPEED:
                if (AiCalc.hasUsedStatusFunction(b, def, "04D")) return true;
                return amount <= 1 && (AiCalc.hasUsedStatusFunction(b, def, "044") || (poisoned && AiCalc.hasUsedStatusFunction(b, def, "140"))
                        || AiCalc.hasUsedHitFunction(b, def, 75, "044"));
            case PBStats.ACCURACY:
                return amount <= 1 && (AiCalc.hasUsedStatusFunction(b, def, "047") || AiCalc.hasUsedHitFunction(b, def, 75, "047"));
            case PBStats.EVASION:
                return amount <= 1 && (AiCalc.hasUsedStatusFunction(b, def, "048") || AiCalc.hasUsedHitFunction(b, def, 75, "048"));
            default:
                return false;
        }
    }

    static boolean goodIdeaToRaiseAttack(AiCtx ctx, Battler atk, Battler def, int amount) {
        return !badIdeaToRaise(ctx, atk, def, PBStats.ATTACK, amount) && AiCalc.physicalMoveInMoveset(ctx, atk);
    }

    /** {@code GoodIdeaToRaiseDefenseAgainst}: 1 = the foe likely uses a physical move, 2 = Body Press, 0 = no. */
    static int goodIdeaToRaiseDefense(AiCtx ctx, Battler atk, Battler def, int amount) {
        if (!badIdeaToRaise(ctx, atk, def, PBStats.DEFENSE, amount)) {
            if (likelyToUseMoveSplit(ctx, def) == 1) return 1;
            if (AiCalc.moveFunctionInMoveset(atk, "177")) return 2;                                   // Body Press
        }
        return 0;
    }

    static boolean goodIdeaToRaiseSpAttack(AiCtx ctx, Battler atk, Battler def, int amount) {
        return !badIdeaToRaise(ctx, atk, def, PBStats.SPATK, amount) && AiCalc.specialMoveInMoveset(ctx, atk);
    }

    static boolean goodIdeaToRaiseSpDefense(AiCtx ctx, Battler atk, Battler def, int amount) {
        return !badIdeaToRaise(ctx, atk, def, PBStats.SPDEF, amount) && likelyToUseMoveSplit(ctx, def) == 2;
    }

    /** {@code GoodIdeaToRaiseSpeedAgainst(...)} (ai_util.c:3281): not in Trick Room, and not already faster than the foe's whole team. */
    static boolean goodIdeaToRaiseSpeed(AiCtx ctx, Battler atk, Battler def, int amount) {
        if (def.fainted()) return false;
        if (badIdeaToRaise(ctx, atk, def, PBStats.SPEED, amount)) return false;                     // BadIdeaToRaiseSpeedAgainst
        return atk.speed() <= teamMaxSpeed(ctx.battle, def);                                         // !FasterThanEntireTeam
    }

    static boolean goodIdeaToRaiseAccuracy(AiCtx ctx, Battler atk, Battler def, int amount) {
        if (badIdeaToRaise(ctx, atk, def, PBStats.ACCURACY, amount)) return false;
        for (int i = 0; i < Battler.MOVES_MAX; i++) {                                                // MoveInMovesetWithAccuracyLessThan(...,90,TRUE)
            BattleMove m = atk.moveSlot(i);
            if (m != null && !m.statusMove() && AiCalc.usable(ctx, atk, i) && AiCalc.hitChance(ctx.battle, atk, def, m) < 90) return true;
        }
        return false;
    }

    static boolean goodIdeaToRaiseEvasion(AiCtx ctx, Battler atk, Battler def, int amount) {
        return !badIdeaToRaise(ctx, atk, def, PBStats.EVASION, amount)
                && !(def.hasActiveAbility("KEENEYE") || atk.effects.truthy(PBEffects.Battler.Foresight) || atk.effects.truthy(PBEffects.Battler.MiracleEye));
    }

    /** {@code GetTeamMaxSpeed(bank)}: the fastest able Pokemon on the battler's side (the party's own speed). */
    static int teamMaxSpeed(Battle battle, Battler b) {
        int max = 0;
        for (Battler m : battle.partyOf(b.index)) {
            if (m == null || m.removedFromParty || m.fainted() || m.pokemon.egg) continue;
            max = Math.max(max, m.pokemon.speed());
        }
        return Math.max(max, b.speed());
    }

    /** {@code BankLikelyToUseMoveSplit(bank,class)} (ai_advanced.c:1704): 1 physical, 2 special, 0 status. */
    static int likelyToUseMoveSplit(AiCtx ctx, Battler b) {
        int cls = AiCalc.fightingStyle(ctx, b);
        boolean physical = hasDamaging(ctx, b, true);
        boolean special = hasDamaging(ctx, b, false);
        if (physical) {
            if (AiCalc.classSweeper(cls) && b.pokemon.attack() > b.pokemon.spAtk()) return 1;
        } else if (special) {
            if (AiCalc.classSweeper(cls) && b.pokemon.spAtk() > b.pokemon.attack()) return 2;
        }
        return 0;
    }

    private static boolean hasDamaging(AiCtx ctx, Battler b, boolean physical) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && !m.statusMove() && m.power() > 0 && m.physical() == physical) return true;
        }
        return false;
    }

    private static boolean abilityBlocksLowering(Battler def, int stat) {
        return def.hasActiveAbility(new String[] {"CLEARBODY", "WHITESMOKE", "FULLMETALBODY", "CONTRARY", "DEFIANT", "COMPETITIVE", "MIRRORARMOR"})
                || (stat == PBStats.ATTACK && def.hasActiveAbility("HYPERCUTTER"))
                || (stat == PBStats.DEFENSE && def.hasActiveAbility("BIGPECKS"))
                || (stat == PBStats.ACCURACY && def.hasActiveAbility("KEENEYE"));
    }

    private static boolean lowerNotWorthIt(AiCtx ctx, Battler def, Battler atk, BattleMove move) {
        return !AiCalc.moveWouldHitFirst(ctx, move, atk, def) && AiCalc.canKnockOut(ctx, atk, def);
    }

    /** {@code GoodIdeaToLowerAttack(bankDef,bankAtk,move)} (ai_util.c:3308). */
    static boolean goodIdeaToLowerAttack(AiCtx ctx, Battler def, Battler atk, BattleMove move) {
        if (lowerNotWorthIt(ctx, def, atk, move) || AiCalc.willFaintFromSecondaryDamage(ctx.battle, def)) return false;
        return def.stage(PBStats.ATTACK) > -2 && AiCalc.physicalMoveInMoveset(ctx, def) && !abilityBlocksLowering(def, PBStats.ATTACK);
    }

    static boolean goodIdeaToLowerDefense(AiCtx ctx, Battler def, Battler atk, BattleMove move) {
        if (lowerNotWorthIt(ctx, def, atk, move)) return false;
        return def.stage(PBStats.DEFENSE) > -2 && AiCalc.physicalMoveInMoveset(ctx, atk) && !abilityBlocksLowering(def, PBStats.DEFENSE);
    }

    static boolean goodIdeaToLowerSpAtk(AiCtx ctx, Battler def, Battler atk, BattleMove move) {
        if (lowerNotWorthIt(ctx, def, atk, move)) return false;
        return def.stage(PBStats.SPATK) > -2 && AiCalc.specialMoveInMoveset(ctx, def) && !abilityBlocksLowering(def, PBStats.SPATK);
    }

    static boolean goodIdeaToLowerSpDef(AiCtx ctx, Battler def, Battler atk, BattleMove move) {
        if (lowerNotWorthIt(ctx, def, atk, move)) return false;
        return def.stage(PBStats.SPDEF) > -2 && AiCalc.specialMoveInMoveset(ctx, atk) && !abilityBlocksLowering(def, PBStats.SPDEF);
    }

    static boolean goodIdeaToLowerSpeed(AiCtx ctx, Battler def, Battler atk, BattleMove move, int reduceBy) {
        if (lowerNotWorthIt(ctx, def, atk, move)) return false;
        return atk.speed() <= def.speed() && !abilityBlocksLowering(def, PBStats.SPEED);
    }

    static boolean goodIdeaToLowerAccuracy(AiCtx ctx, Battler def, Battler atk, BattleMove move) {
        return !lowerNotWorthIt(ctx, def, atk, move) && !abilityBlocksLowering(def, PBStats.ACCURACY);
    }

    /** {@code GoodIdeaToLowerEvasion} (ai_util.c:3394). */
    static boolean goodIdeaToLowerEvasion(AiCtx ctx, Battler def, Battler atk) {
        if (abilityBlocksLowering(def, PBStats.EVASION)) return false;
        if (def.stage(PBStats.EVASION) > 0) return true;
        boolean hasInaccurate = false;
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m != null && !m.statusMove() && AiCalc.usable(ctx, atk, i) && AiCalc.hitChance(ctx.battle, atk, def, m) < 90) hasInaccurate = true;
        }
        if (!hasInaccurate) return false;
        if (atk.speed() >= def.speed()) return !AiCalc.canKnockOut(ctx, def, atk);
        return !AiCalc.can2HKO(ctx, def, atk);
    }

    // ------------------------------------------------------------------
    // Recovery / phazing / sleep
    // ------------------------------------------------------------------

    /** {@code GetAmountToRecoverBy(bankAtk,bankDef,move)} (ai_advanced.c:857-1027), the move-driven cases. */
    static int amountToRecover(AiCtx ctx, Battler atk, Battler def, BattleMove move) {
        if (atk.effects.intVal(PBEffects.Battler.HealBlock) > 0) return 0;
        int max = atk.maxHp();
        int amount = 0;
        String f = move.function();
        switch (f) {
            case "0D5": case "0D6": amount = Math.max(1, max / 2); break;
            case "0D8": {
                int w = ctx.battle.pbWeather();
                if (w == PBWeather.None) amount = Math.max(1, max / 2);
                else if (w == PBWeather.Sun || w == PBWeather.HarshSun) amount = max;
                else amount = Math.max(1, max / 4);
                break;
            }
            case "114": {
                int s = atk.effects.intVal(PBEffects.Battler.Stockpile);
                amount = s == 1 ? Math.max(1, max / 4) : s == 2 ? Math.max(1, max / 2) : s >= 3 ? max : 0;
                break;
            }
            case "0D9": amount = max; break;
            case "0DE":
                if (!def.hasStatus("SLEEP") && !def.hasActiveAbility("COMATOSE")) break;
                // fallthrough
            case "0DD": {
                int dmg = AiCalc.finalDamage(ctx, move, atk, def, 1);
                amount = Math.max(1, dmg / 2);
                if (atk.hasActiveItem("BIGROOT")) amount = 13 * dmg / 10;
                break;
            }
            case "05A": {
                int finalHp = Math.max(1, (atk.hp + def.hp) / 2);
                if (finalHp > atk.hp) amount = finalHp - atk.hp;
                break;
            }
            default: break;
        }
        return Math.min(amount, max - 1);
    }

    /** {@code CanKnockOutAfterHealing(bankAtk,bankDef,healAmount,numHits,checkAlwaysHits)} for the strongest move. */
    static boolean canKnockOutAfterHealing(AiCtx ctx, Battler atk, Battler def, int heal, int numHits) {
        int hp = Math.min(def.hp + heal, def.maxHp());
        BattleMove strongest = AiCalc.strongestMove(ctx, atk, def);
        return strongest != null && AiCalc.finalDamage(ctx, strongest, atk, def, numHits) >= hp;
    }

    /** {@code ShouldRecover(bankAtk,bankDef,move)} (ai_advanced.c:1029-1062). */
    static boolean shouldRecover(AiCtx ctx, Battler atk, Battler def, BattleMove move) {
        int heal = amountToRecover(ctx, atk, def, move);
        if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
            if (AiCalc.canKnockOut(ctx, def, atk) && !canKnockOutAfterHealing(ctx, def, atk, heal, 1)) return true;
            if (AiCalc.can2HKO(ctx, def, atk) && !isIncapacitated(def) && heal > atk.hp && (ctx.simulatedRng[1] & 1) == 0) return true;
        } else if (!AiCalc.canKnockOut(ctx, def, atk)) {
            if (!isIncapacitated(def) && AiCalc.can2HKO(ctx, def, atk) && !canKnockOutAfterHealing(ctx, def, atk, heal, 1)) return true;
        }
        return false;
    }

    /** {@code AnyUsefulStatIsRaised(bank)} (ai_util.c:5216) via {@code CountUsefulStatChanges}. */
    static boolean anyUsefulStatIsRaised(AiCtx ctx, Battler b, Battler foe) {
        return countUsefulStatChanges(ctx, b, b, foe, false) > 0;
    }

    /** {@code CountUsefulBoosts / CountUsefulDebuffs} (ai_util.c:5130-5199). */
    static int countUsefulStatChanges(AiCtx ctx, Battler withBuffs, Battler toGet, Battler foe, boolean debuff) {
        boolean storedPower = !debuff && usableNamed(ctx, toGet, "STOREDPOWER", "POWERTRIP");
        int buffs = 0;
        for (int stat = PBStats.ATTACK; stat <= PBStats.EVASION; stat++) {
            int v = withBuffs.stage(stat);
            if (!(debuff ? v < 0 : v > 0)) continue;
            boolean add = storedPower;
            if (!add) {
                switch (stat) {
                    case PBStats.ATTACK: add = AiCalc.physicalMoveInMoveset(ctx, toGet); break;
                    case PBStats.DEFENSE: add = hasDamaging(ctx, foe, true) || AiCalc.moveFunctionInMoveset(toGet, "177"); break;
                    case PBStats.SPATK: add = AiCalc.specialMoveInMoveset(ctx, toGet); break;
                    case PBStats.SPDEF: add = hasDamaging(ctx, foe, false); break;
                    default: add = true; break;
                }
            }
            if (add) buffs += debuff ? -v : v;
        }
        return buffs;
    }

    /** {@code ShouldPhaze(bankAtk,bankDef,move,class)} (ai_advanced.c:1198-1262), single battle. */
    static boolean shouldPhaze(AiCtx ctx, Battler atk, Battler def, BattleMove move, int cls) {
        if (AiNegativeEffects.canKnockOutWithoutMove(ctx, move, atk, def)) return false;
        if (AiCalc.canKnockOut(ctx, atk, def)) return false;
        int perish = def.effects.intVal(PBEffects.Battler.PerishSong);
        if (perish > 0) {
            if (perish == 1) return false;
            if (perish == 2 && !AiCalc.moveWouldHitFirst(ctx, move, atk, def)) return false;
        }
        if (move.function().equals("0EB") && cls == AiCalc.CLASS_PHAZING) {
            BattleSide side = def.pbOwnSide();
            if (side.effects.intVal(PBEffects.Side.StealthRock) > 0 || side.effects.intVal(PBEffects.Side.ToxicSpikes) >= 1
                    || side.effects.intVal(PBEffects.Side.Spikes) >= 1) return true;
        }
        return anyUsefulStatIsRaised(ctx, def, atk);
    }

    /** {@code BadIdeaToPutToSleep(bankDef,bankAtk)} (ai_util.c:2862). */
    static boolean badIdeaToPutToSleep(AiCtx ctx, Battler def, Battler atk) {
        return !AiCalc.canBePutToSleep(ctx.battle, def, atk) || def.effects.intVal(PBEffects.Battler.Yawn) > 0
                || def.hasActiveAbility(new String[] {"EARLYBIRD", "SHEDSKIN"});
    }

    /** {@code IncreaseSleepViability(&viability,class,bankAtk,bankDef,move)} (ai_advanced.c:2124-), single battle. */
    static int incSleep(AiCtx ctx, int viability, int cls, Battler atk, Battler def, BattleMove move) {
        if (badIdeaToPutToSleep(ctx, def, atk)) return viability;
        if (move.function().equals("004")) {                                                         // Yawn
            if (AiCalc.canKnockOut(ctx, atk, def) && !AiCalc.movePredictionIsHealing(ctx, def) && !ctx.predictedToSwitch(def)) return viability;
            if (!def.hasActiveAbility("TRUANT") && AiUtil.benchAlive(ctx.battle, atk) == 0 && !AiCalc.moveFunctionInMoveset(atk, AiCalc.PROTECT)) {
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
                    if (AiCalc.canKnockOut(ctx, def, atk)) return viability;
                    if (AiCalc.can2HKO(ctx, def, atk) && !AiCalc.moveFunctionInMoveset(atk, "0D5", "0D6", "0D8", "114")) return viability;
                } else if (AiCalc.can2HKO(ctx, def, atk)) {
                    return viability;
                }
            }
        }
        boolean decent = AiCalc.hitChance(ctx.battle, atk, def, move) >= 80;
        switch (cls) {
            case AiCalc.CLASS_SWEEPER_KILL: break;
            case AiCalc.CLASS_SWEEPER_SETUP_STATS: viability = decent ? inc(viability, 9) : incStatus(ctx, viability, cls, 3, atk, def); break;
            case AiCalc.CLASS_SWEEPER_SETUP_STATUS: viability = decent ? inc(viability, 8) : incStatus(ctx, viability, cls, 3, atk, def); break;
            case AiCalc.CLASS_STALL: viability = incStatus(ctx, viability, cls, 3, atk, def); break;
            case AiCalc.CLASS_BATON_PASS: viability = decent ? inc(viability, 9) : incStatus(ctx, viability, cls, 3, atk, def); break;
            case AiCalc.CLASS_CLERIC: viability = decent ? inc(viability, 7) : incStatus(ctx, viability, cls, 3, atk, def); break;
            case AiCalc.CLASS_SCREENS: case AiCalc.CLASS_SWEEPER_SETUP_SCREENS: viability = decent ? inc(viability, 8) : incStatus(ctx, viability, cls, 3, atk, def); break;
            case AiCalc.CLASS_PHAZING: viability = decent ? inc(viability, 9) : incStatus(ctx, viability, cls, 3, atk, def); break;
            case AiCalc.CLASS_ENTRY_HAZARDS: viability = decent ? inc(viability, 8) : incStatus(ctx, viability, cls, 3, atk, def); break;
            default: break;
        }
        return viability;
    }
}
