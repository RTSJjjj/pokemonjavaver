package pokemon.runtime.battle;

import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;

import java.util.Random;

/**
 * The singles-relevant helpers of CFRU's {@code ai_util.c} / {@code ai_advanced.c} that the Negatives and
 * Positives scripts call. Each method names its C function and line.
 *
 * <p>Function codes are the standard Essentials hex codes of {@code moves.json}; CFRU's {@code EFFECT_*} ids
 * are mapped to them where a helper switches on a move effect.</p>
 *
 * <p>登记 (not transcribed in this batch): Z-moves / Dynamax / Raid (CFRU-only), Parental Bond multi-hit scaling,
 * {@code BracketCalc} (Quick Claw), Focus Sash / Sturdy damage clamps ({@code ai_util.c:1344-1347}), the
 * contact / recoil / choice-lock tie-breaks of {@code CalcStrongestMoveIgnoringMove} (:1573-1640), and
 * {@code GetBankFightingStyle}'s doubles branch.</p>
 */
final class AiCalc {

    private AiCalc() {
    }

    /** {@code FIGHT_CLASS_*} ({@code ai_advanced.h}), singles ones. */
    static final int CLASS_NONE = 0, CLASS_SWEEPER_KILL = 1, CLASS_SWEEPER_SETUP_STATS = 2,
            CLASS_SWEEPER_SETUP_STATUS = 3, CLASS_SWEEPER_SETUP_SCREENS = 4, CLASS_STALL = 5,
            CLASS_BATON_PASS = 6, CLASS_CLERIC = 7, CLASS_SCREENS = 8, CLASS_PHAZING = 9, CLASS_ENTRY_HAZARDS = 10;

    /** A damage prediction: {@code dmg} is {@code AI_CalcDmg}'s value, {@code typeMod} the type result (0 = no effect). */
    static final class AiDmg {
        final int dmg;
        final int typeMod;

        AiDmg(int dmg, int typeMod) {
            this.dmg = dmg;
            this.typeMod = typeMod;
        }
    }

    /** A {@link Random} that always rolls the top of the damage range. */
    private static final class MaxRoll extends Random {
        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }
    }

    static MoveEffect fx(BattleMove move) {
        return MoveEffectRegistry.of(move.function());
    }

    static boolean isStatus(BattleMove move) {
        return move.statusMove();
    }

    /** The function code as a number (hex), for the range checks the Essentials numbering allows. */
    static int code(BattleMove move) {
        try {
            return Integer.parseInt(move.function(), 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static boolean has(BattleMove move, char flag) {
        return move.flags() != null && move.flags().indexOf(flag) >= 0;
    }

    static boolean oneOf(BattleMove move, String... codes) {
        String f = move.function();
        for (String c : codes) {
            if (c.equals(f)) return true;
        }
        return false;
    }

    static boolean named(BattleMove move, String... names) {
        String n = move.internalName();
        for (String c : names) {
            if (c.equals(n)) return true;
        }
        return false;
    }

    /** {@code IsPlayerInControl(bank)}: the battler is the player's. */
    static boolean playerControlled(Battler b) {
        return !b.foe;
    }

    /**
     * {@code AI_CalcDmg(bankAtk,bankDef,move,NULL)} (damage_calc.c:463-): the fixed-damage effects, then the damage
     * calculation at "93%, about halfway between min & max" (:524). 登记: critical chance (:508-512) and multi-hit
     * (:535-) are not applied.
     */
    static AiDmg calcDmg(Battle battle, Battler atk, Battler def, BattleMove move) {
        if (move == null || battle.pbs() == null) return new AiDmg(0, 8);
        boolean disguise = def.damageState.disguise;
        boolean iceface = def.damageState.iceface;
        boolean flameveil = def.damageState.flameveil;
        boolean critical = def.damageState.critical;
        int calcDamage = def.damageState.calcDamage;
        int typeMod = def.damageState.typeMod;
        try {
            // AI_SpecialTypeCalc: only the type result is needed here, so run the damage calc once at the top roll.
            int max = DamageCalc.compute(atk, def, move, battle.pbs(), new MaxRoll());
            int tm = def.damageState.typeMod;
            if (move.statusMove() && !oneOf(move, "06E")) return new AiDmg(0, tm);
            boolean noEffect = tm == 0;
            // damage_calc.c:473-503 fixed-damage effects
            switch (move.function()) {
                case "06A": return new AiDmg(noEffect ? 0 : 20, tm);                          // EFFECT_SONICBOOM
                case "06B": return new AiDmg(noEffect ? 0 : 40, tm);                          // EFFECT_DRAGON_RAGE
                case "06C": return new AiDmg(noEffect ? 0 : def.hp / 2, tm);                  // EFFECT_SUPER_FANG
                case "06D": return new AiDmg(noEffect ? 0 : atk.level(), tm);                 // EFFECT_LEVEL_DAMAGE
                case "06E": {                                                                 // EFFECT_ENDEAVOR
                    int d = def.hp - atk.hp;
                    return new AiDmg(d <= 0 ? 0 : d, tm);
                }
                case "06F": return new AiDmg(noEffect ? 0 : atk.level() * 3 / 2, tm);        // EFFECT_PSYWAVE: random 50 -> level*(50+100)/100
                default: break;
            }
            if (noEffect) return new AiDmg(0, tm);                                            // :469 MOVE_RESULT_NO_EFFECT
            if (move.power() == 0 && !oneOf(move, "070")) return new AiDmg(0, tm);
            return new AiDmg(max * 93 / 100, tm);                                             // :524 roll 93%
        } finally {
            def.damageState.disguise = disguise;
            def.damageState.iceface = iceface;
            def.damageState.flameveil = flameveil;
            def.damageState.critical = critical;
            def.damageState.calcDamage = calcDamage;
            def.damageState.typeMod = typeMod;
        }
    }

    // ------------------------------------------------------------------
    // Used-move history (BATTLE_HISTORY->usedMoves, gLastUsedMoves, gNewBS->LastUsedMove)
    // ------------------------------------------------------------------

    /** A {@link BattleMove} for a move's internal name, or null. */
    static BattleMove moveByName(Battle battle, String name) {
        if (name == null || battle.pbs() == null) return null;
        pokemon.runtime.pokemon.PbsData.Move data = battle.pbs().move(name);
        return data == null ? null : new BattleMove(data);
    }

    /** {@code gLastUsedMoves[bank]}: the last move the battler used, or null (MOVE_NONE). */
    static BattleMove lastUsedMove(Battle battle, Battler b) {
        return moveByName(battle, b.lastMoveUsed);
    }

    /** {@code gNewBS->LastUsedMove}: the last move anyone used (Copycat). */
    static BattleMove globalLastUsedMove(Battle battle) {
        if (battle.lastMoveUsed < 0 || battle.pbs() == null) return null;
        for (pokemon.runtime.pokemon.PbsData.Move m : battle.pbs().moves.values()) {
            if (m.id == battle.lastMoveUsed) return new BattleMove(m);
        }
        return null;
    }

    /** {@code HasUsedMove(bank,move)}. */
    static boolean hasUsedMove(Battler b, String internalName) {
        return b.movesUsed.contains(internalName, false);
    }

    /** {@code HasUsedMoveWithEffect(bank,effect)}: a used status move with one of these function codes. */
    static boolean hasUsedStatusFunction(Battle battle, Battler b, String... codes) {
        for (String name : b.movesUsed) {
            BattleMove m = moveByName(battle, name);
            if (m != null && m.statusMove() && oneOf(m, codes)) return true;
        }
        return false;
    }

    /** {@code HasUsedMoveWithEffectHigherThanChance(bank,effect,chance)}: a used damaging move of that side-effect family. */
    static boolean hasUsedHitFunction(Battle battle, Battler b, int chance, String... codes) {
        for (String name : b.movesUsed) {
            BattleMove m = moveByName(battle, name);
            if (m != null && !m.statusMove() && oneOf(m, codes) && secondaryEffectChance(m, b) >= chance) return true;
        }
        return false;
    }

    /** {@code HasUsedMoveWithEffect(bank,effect)} for a function code shared by status and damaging moves (any used move). */
    static boolean hasUsedFunction(Battle battle, Battler b, String... codes) {
        for (String name : b.movesUsed) {
            BattleMove m = moveByName(battle, name);
            if (m != null && oneOf(m, codes)) return true;
        }
        return false;
    }

    /** Function codes Essentials shares between a status move and its damaging "hit with a side effect" variants (CFRU's separate *_HIT effects). */
    static final String[] SIDE_EFFECT_CODES = {"003", "005", "006", "007", "00A", "00C", "013", "042", "043", "044", "045", "046", "047",
            "04B", "04C", "04D", "04E", "04F",
            "01C", "01D", "01F", "020", "022", "179",      // self stat-raising hits (Power-Up Punch, Steel Wing, Flame Charge, Charge Beam, ...)
            "0A7", "0EF"};                                  // Foresight / trapping hits (Target Beam, Spirit Shackle): plain damage

    /** A damaging move whose function code is a status family: CFRU's EFFECT_*_HIT. */
    static boolean isSideEffectHit(BattleMove move) {
        return !move.statusMove() && move.power() > 0 && oneOf(move, SIDE_EFFECT_CODES);
    }

    /** {@code CalcSecondaryEffectChance(bank,move,ability)} (ai_util.c:2728). 登记: Sheer Force boosted-move table (any move with a chance), Rainbow, flinch table. */
    static int secondaryEffectChance(BattleMove move, Battler atk) {
        int chance = move.additionalChance();
        if (chance > 0 && atk.hasActiveAbility("SHEERFORCE")) return 0;
        if (atk.hasActiveAbility("SERENEGRACE")) chance *= 2;
        return chance;
    }

    /** {@code AI_SpecialTypeCalc(...) & MOVE_RESULT_NO_EFFECT}. */
    static boolean noEffect(Battle battle, Battler atk, Battler def, BattleMove move) {
        return calcDmg(battle, atk, def, move).typeMod == 0;
    }

    /** {@code CalcAIAccuracy} / {@code AccuracyCalc} (accuracy_calc.c): the hit chance 0..100, ai_util math of {@code pbAccuracyCheck} (:117-140). */
    static int hitChance(Battle battle, Battler atk, Battler def, BattleMove move) {
        MoveEffect fx = fx(move);
        if (def.effects.intVal(PBEffects.Battler.Telekinesis) > 0) return 100;           // :115
        int baseAcc = fx.pbBaseAccuracy(move, atk, def);                                  // :117
        if (baseAcc == 0) return 100;                                                     // :118
        float[] m = new float[5];
        m[BattleHandlers.BASE_ACC] = baseAcc;
        m[BattleHandlers.ACC_STAGE] = atk.stage(PBStats.ACCURACY);
        m[BattleHandlers.EVA_STAGE] = def.stage(PBStats.EVASION);
        m[BattleHandlers.ACC_MULT] = 1.0f;
        m[BattleHandlers.EVA_MULT] = 1.0f;
        fx.pbCalcAccuracyModifiers(move, atk, def, m);                                    // :126
        if (m[BattleHandlers.BASE_ACC] == 0) return 100;                                  // :128
        int accStage = Math.min(Math.max((int) m[BattleHandlers.ACC_STAGE], -6), 6) + 6;
        int evaStage = Math.min(Math.max((int) m[BattleHandlers.EVA_STAGE], -6), 6) + 6;
        int[] mul = {3, 3, 3, 3, 3, 3, 3, 4, 5, 6, 7, 8, 9};
        int[] div = {9, 8, 7, 6, 5, 4, 3, 3, 3, 3, 3, 3, 3};
        int accuracy = (int) Math.round(100.0 * mul[accStage] / div[accStage] * m[BattleHandlers.ACC_MULT]);
        int evasion = (int) Math.round(100.0 * mul[evaStage] / div[evaStage] * m[BattleHandlers.EVA_MULT]);
        if (evasion < 1) evasion = 1;
        return Math.min(100, (int) m[BattleHandlers.BASE_ACC] * accuracy / evasion);
    }

    /** {@code MoveWillHit}: a sure-hit move (the chance is 100 and nothing can miss it). */
    static boolean moveWillHit(Battle battle, Battler atk, Battler def, BattleMove move) {
        return hitChance(battle, atk, def, move) >= 100 && fx(move).pbBaseAccuracy(move, atk, def) == 0
                || def.effects.intVal(PBEffects.Battler.Telekinesis) > 0;
    }

    /** {@code PriorityCalc(bank,ACTION_USE_MOVE,move)} (ai_util / battle_util): the move's priority incl. abilities. */
    static int priorityCalc(Battle battle, Battler user, BattleMove move) {
        return AiUtil.priority(battle, user, move);
    }

    /** {@code SpeedCalc(bank)}. */
    static int speed(Battler b) {
        return b.speed();
    }

    /** {@code IsTrickRoomActive()}. */
    static boolean trickRoom(Battle battle) {
        return battle.field.effects.intVal(PBEffects.Field.TrickRoom) > 0;
    }

    /** {@code MoveWouldHitBeforeOtherMove(moveAtk,bankAtk,moveDef,bankDef)} (ai_util.c:1853-1893). 登记: BracketCalc. */
    static boolean wouldHitBefore(Battle battle, BattleMove moveAtk, Battler atk, BattleMove moveDef, Battler def) {
        if (moveDef == null) {
            if (priorityCalc(battle, atk, moveAtk) > 0) return true;                      // :1859
        } else {
            int atkPriority = priorityCalc(battle, atk, moveAtk);
            int defPriority = priorityCalc(battle, def, moveDef);
            if (atkPriority > defPriority) return true;
            if (defPriority > atkPriority) return false;
        }
        int atkSpeed = speed(atk);
        int defSpeed = speed(def);
        if (trickRoom(battle)) {                                                          // :1881
            int t = defSpeed;
            defSpeed = atkSpeed;
            atkSpeed = t;
        }
        return atkSpeed > defSpeed;                                                       // :1888
    }

    /** {@code MoveWouldHitFirst(move,bankAtk,bankDef)} (ai_util.c:1847): uses the foe's predicted move. */
    static boolean moveWouldHitFirst(AiCtx ctx, BattleMove move, Battler atk, Battler def) {
        return wouldHitBefore(ctx.battle, move, atk, ctx.prediction(def), def);
    }

    /** {@code CheckMoveLimitations(bank,0,...)}: the slot can be chosen. */
    static boolean usable(AiCtx ctx, Battler user, int slot) {
        BattleMove m = user.moveSlot(slot);
        if (m == null) return false;
        if (user.foe) return AiUtil.usable(ctx.battle, user, slot);
        return user.moveSlotPp(slot) > 0 || user.moveSlotMaxPp(slot) == 0;
    }

    // ------------------------------------------------------------------
    // Secondary damage (ai_util.c:2594-2645)
    // ------------------------------------------------------------------

    /**
     * {@code GetSecondaryEffectDamage(bank)} (ai_util.c:2628) = {@code CalcSecondaryEffectDamage} (:2594): the damage
     * the battler takes at the end of the turn. 登记: Sea of Fire, Bad Dreams, Splinters, Bad Thoughts, G-Max
     * residuals (CFRU only); weather duration "about to end" (:2603).
     */
    static int secondaryDamage(Battle battle, Battler b) {
        if (b.hasActiveAbility("MAGICGUARD")) return 0;                                   // :2599
        int max = b.maxHp();
        int dmg = 0;
        int weather = battle.pbWeather();
        if (weather == PBWeather.Sandstorm && !b.hasType("ROCK") && !b.hasType("GROUND") && !b.hasType("STEEL")
                && !b.hasActiveAbility(new String[] {"SANDVEIL", "SANDRUSH", "SANDFORCE", "OVERCOAT"})) {
            dmg += Math.max(1, max / 16);                                                 // GetSandstormDamage
        }
        if (weather == PBWeather.Hail && !b.hasType("ICE")
                && !b.hasActiveAbility(new String[] {"ICEBODY", "SNOWCLOAK", "OVERCOAT"})) {
            dmg += Math.max(1, max / 16);                                                 // GetHailDamage
        }
        if (b.hasStatus("SLEEP") && b.effects.truthy(PBEffects.Battler.Nightmare)) dmg += Math.max(1, max / 4);   // GetNightmareDamage
        if (b.effects.intVal(PBEffects.Battler.Trapping) > 0) dmg += Math.max(1, max / 8);                        // GetTrapDamage
        if (b.effects.intVal(PBEffects.Battler.LeechSeed) != -1) {
            dmg += Math.max(1, max / 8);                                                  // GetLeechSeedDamage
        }
        if (b.hasStatus("POISON") && !b.hasActiveAbility("POISONHEAL")) {
            dmg += b.toxic > 0 ? Math.max(1, max * Math.min(b.toxic + 1, 15) / 16) : Math.max(1, max / 8);   // GetPoisonDamage
        }
        if (b.hasStatus("BURN") && !b.hasActiveAbility("HEATPROOF")) dmg += Math.max(1, max / 16);                // GetBurnDamage (NEWEST_BATTLE_MECHANICS)
        if (b.effects.truthy(PBEffects.Battler.Curse)) dmg += Math.max(1, max / 4);       // GetCurseDamage
        return dmg;
    }

    /** {@code IsTakingSecondaryDamage(bank,FALSE)} (ai_util.c:2637). */
    static boolean takingSecondaryDamage(Battle battle, Battler b) {
        return secondaryDamage(battle, b) > 0;
    }

    /** {@code WillFaintFromSecondaryDamage(bank)} (ai_util.c:2642). 登记: Perish Song (:2644), passive recovery (:2647). */
    static boolean willFaintFromSecondaryDamage(Battle battle, Battler b) {
        return secondaryDamage(battle, b) >= b.hp;
    }

    /** {@code GetContactDamage(move,bankAtk,bankDef)} (ai_util.c:2681-2695): Rough Skin / Iron Barbs / Rocky Helmet. */
    static int contactDamage(BattleMove move, Battler atk, Battler def) {
        if (!has(move, 'a')) return 0;
        if (atk.hasActiveAbility("MAGICGUARD")) return 0;
        int dmg = 0;
        if (def.hasActiveAbility(new String[] {"ROUGHSKIN", "IRONBARBS"})) dmg += atk.maxHp() / 8;
        if (def.hasActiveItem("ROCKYHELMET")) dmg += atk.maxHp() / 6;
        return dmg;
    }

    /** {@code WillFaintFromContactDamage(bankAtk,bankDef,move)} (ai_util.c:2697). */
    static boolean willFaintFromContactDamage(Battler atk, Battler def, BattleMove predicted) {
        return predicted != null && contactDamage(predicted, atk, def) >= atk.hp;
    }

    /** {@code HighChanceOfBeingImmobilized(bank)} (ai_util.c:2702-): odds of landing an attack below 75%? 登记: confusion branch. */
    static boolean highChanceOfBeingImmobilized(Battler b) {
        int odds = 100;
        if (b.hasStatus("PARALYSIS")) odds = odds * 75 / 100;
        else if (b.hasStatus("FROZEN")) odds = odds * 20 / 100;
        if (b.effects.intVal(PBEffects.Battler.Attract) >= 0) odds = odds * 50 / 100;
        return odds < 75;
    }

    // ------------------------------------------------------------------
    // Knock-out maths (ai_util.c:1201-1330)
    // ------------------------------------------------------------------

    /** {@code AttacksThisTurn(bank,move) == 1}: a charging first turn (Essentials 0C3-0CE). Solar Beam in sun does not charge. */
    private static boolean chargesFirst(Battle battle, BattleMove move) {
        int c = code(move);
        if (c < 0xC3 || c > 0xCE) return false;
        if (c == 0xC4 && (battle.pbWeather() == PBWeather.Sun || battle.pbWeather() == PBWeather.HarshSun)) return false;
        return true;
    }

    /** {@code MoveKnocksOutXHits(move,bankAtk,bankDef,numHits)} (ai_util.c:1201-1269). 登记: raid shields, Disguise on a fresh Pokemon. */
    static boolean knocksOutXHits(AiCtx ctx, BattleMove move, Battler atk, Battler def, int numHits) {
        Battle battle = ctx.battle;
        if (move == null) return false;
        if (def.effects.intVal(PBEffects.Battler.Substitute) > 0 && numHits > 0) numHits -= 1;     // :1206 MoveBlockedBySubstitute
        else if (def.isSpecies("MIMIKYU") && def.form() == 0 && def.hasActiveAbility("DISGUISE")
                && !atk.hasMoldBreaker() && !move.statusMove() && numHits > 0) numHits -= 1;       // :1207 IsAffectedByDisguse
        if (chargesFirst(battle, move) && numHits > 0) numHits -= 1;                                // :1213
        if (move.function().equals("0E0")) {                                                         // :1219 EFFECT_EXPLOSION
            if (numHits > 0) numHits = 1;
        } else if (move.function().equals("0C2")) {                                                  // :1224 EFFECT_RECHARGE
            if (numHits > 0) numHits -= 1;
        }
        if (numHits <= 0) return false;                                                              // :1237
        if (move.function().equals("111") && numHits >= 1) return false;                            // Future Sight: really always 3 hits
        return finalDamage(ctx, move, atk, def, numHits) >= def.hp;
    }

    /** {@code CalcFinalAIMoveDamage(move,bankAtk,bankDef,numHits,NULL)} (ai_util.c:1326-1366), capped at the target's HP. */
    static int finalDamage(AiCtx ctx, BattleMove move, Battler atk, Battler def, int numHits) {
        if (move == null || numHits == 0) return 0;
        if (move.power() == 0 && !oneOf(move, "06A", "06B", "06C", "06D", "06E", "06F", "070")) return 0;
        if (move.function().equals("070")) {                                                         // EFFECT_0HKO
            if (playerControlled(atk) == false || moveWillHit(ctx.battle, atk, def, move)) return def.hp;
            return 1;
        }
        int dmg = calcDmg(ctx.battle, atk, def, move).dmg;
        if (dmg >= def.hp) return def.hp;
        if (numHits >= 2) {                                                                          // AdjustFinalAIDamageForNumHits (:1296-1308)
            int first = dmg;
            int endTurn = secondaryDamage(ctx.battle, def) * (numHits - 1);
            dmg = first + dmg * (numHits - 1) + endTurn;
        } else {
            dmg *= numHits;
        }
        return Math.min(dmg, def.hp);
    }

    /** {@code CanKnockOut(bankAtk,bankDef)}: any usable move of {@code atk} knocks {@code def} out in one hit. */
    static boolean canKnockOut(AiCtx ctx, Battler atk, Battler def) {
        return knocksOutXHits(ctx, strongestMove(ctx, atk, def), atk, def, 1);                       // ai_util.c:UpdateStrongestMoves :1257
    }

    /** {@code Can2HKO(bankAtk,bankDef)}: {@code canKnockOut || strongest move knocks out in 2 hits}. */
    static boolean can2HKO(AiCtx ctx, Battler atk, Battler def) {
        BattleMove strongest = strongestMove(ctx, atk, def);
        return knocksOutXHits(ctx, strongest, atk, def, 1) || knocksOutXHits(ctx, strongest, atk, def, 2);   // :1258-1259
    }

    // ------------------------------------------------------------------
    // Strongest move (ai_util.c:1489-1650)
    // ------------------------------------------------------------------

    /** {@code GetStrongestMove(bankAtk,bankDef)} / {@code IsStrongestMove}'s source (ai_util.c:1699). */
    static BattleMove strongestMove(AiCtx ctx, Battler atk, Battler def) {
        if (!ctx.hasCachedStrongest(atk)) ctx.cacheStrongest(atk, calcStrongestMove(ctx, atk, def));
        return ctx.cachedStrongest(atk);
    }

    /** {@code IsStrongestMove(currentMove,bankAtk,bankDef)} (ai_util.c:1691). */
    static boolean isStrongestMove(AiCtx ctx, BattleMove move, Battler atk, Battler def) {
        return strongestMove(ctx, atk, def) == move;
    }

    /** {@code CalcStrongestMoveIgnoringMove(bankAtk,bankDef,FALSE,0)} (ai_util.c:1489-1650), without the contact/recoil/choice tie-breaks. */
    static BattleMove calcStrongestMove(AiCtx ctx, Battler atk, Battler def) {
        if (def.hp == 0) return null;                                                                // :1494
        BattleMove strongest = atk.moveSlot(0);                                                      // :1498
        int highest = 0;
        int bestAcc = 0;
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove move = atk.moveSlot(i);
            if (move == null) break;                                                                 // :1520
            if (!usable(ctx, atk, i)) continue;                                                      // :1527
            if (move.power() == 0 && !oneOf(move, "06A", "06B", "06C", "06D", "06E", "06F", "070")) continue;   // :1529
            if (move.function().equals("070")) {                                                     // :1535 EFFECT_0HKO
                if (atk.level() < def.level() || (named(move, "SHEERCOLD") && def.hasType("ICE"))
                        || def.hasActiveAbility("STURDY") || !moveWillHit(ctx.battle, atk, def, move)) continue;
            }
            int predicted = finalDamage(ctx, move, atk, def, 1);                                     // :1544
            if (predicted > highest) {
                strongest = move;
                highest = predicted;
                bestAcc = 0;
            } else if (predicted == highest && !oneOf(move, "071", "072", "073", "111")) {                                    // :1553-1556 counter, mirror coat, metal burst, future sight
                int thisPriority = priorityCalc(ctx.battle, atk, move);
                int bestPriority = strongest == null ? 0 : priorityCalc(ctx.battle, atk, strongest);
                if (strongest != null && strongest.function().equals("111")
                        || (thisPriority > bestPriority && !move.function().equals("111"))) {         // :1569-1572
                    strongest = move;
                    bestAcc = 0;
                } else if (thisPriority == bestPriority) {                                            // :1580
                    int currAcc = moveWillHit(ctx.battle, atk, def, move) ? 100 : hitChance(ctx.battle, atk, def, move);
                    if (bestAcc == 0 && strongest != null) {
                        bestAcc = moveWillHit(ctx.battle, atk, def, strongest) ? 100 : hitChance(ctx.battle, atk, def, strongest);
                    }
                    if (currAcc > bestAcc && bestAcc < 100) {                                         // :1590
                        strongest = move;
                        bestAcc = currAcc;
                    } else if (currAcc == bestAcc || currAcc >= 100) {                                // :1595
                        if ((ctx.random() & 1) != 0) strongest = move;                                // :1637 finally pick a move at random
                    }
                }
            }
        }
        return strongest;
    }

    /** {@code StrongestMoveGoesFirst(currentMove,bankAtk,bankDef)} (ai_util.c:742): {@code CalcStrongestMoveGoesFirst} (:710-740) picks the strongest of the moves that hit first. */
    static boolean strongestMoveGoesFirst(AiCtx ctx, BattleMove current, Battler atk, Battler def) {
        BattleMove best = null;
        int bestDmg = -1;
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m == null) break;
            if (!usable(ctx, atk, i) || (m.power() == 0 && !oneOf(m, "06A", "06B", "06C", "06D", "06E", "06F", "070"))) continue;
            if (!moveWouldHitFirst(ctx, m, atk, def)) continue;
            int d = finalDamage(ctx, m, atk, def, 1);
            if (d > bestDmg) {
                bestDmg = d;
                best = m;
            }
        }
        return best != null && best == current;
    }

    /**
     * {@code MoveKnocksOutPossiblyGoesFirstWithBestAccuracy(checkMove,bankAtk,bankDef,checkGoingFirst)}
     * (ai_util.c:301-413), without the contact preferences (:339-340, :395-410: BadIdeaToMakeContactWith needs the
     * contact-ability table) and Pursuit (:313).
     */
    static boolean knocksOutPossiblyGoesFirstWithBestAccuracy(AiCtx ctx, BattleMove checkMove, Battler atk, Battler def,
                                                              boolean checkGoingFirst) {
        int bestAcc = 0;
        int bestPriority = 0;
        int perfectMoves = 0;
        int goodMoves = 0;
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove cur = atk.moveSlot(i);
            if (cur == null) break;
            if (!usable(ctx, atk, i)) continue;
            if (!knocksOutXHits(ctx, cur, atk, def, 1)) continue;
            if (checkGoingFirst && !(moveWouldHitFirst(ctx, cur, atk, def) && !cur.function().equals("111"))) continue;
            if (moveWillHit(ctx.battle, atk, def, cur)) {                                             // :329
                perfectMoves |= 1 << i;
            } else if (perfectMoves == 0) {
                int currAcc = hitChance(ctx.battle, atk, def, cur);
                int currPriority = priorityCalc(ctx.battle, atk, cur);
                if (goodMoves == 0 || (currAcc > bestAcc && bestAcc < 100)) {
                    bestAcc = currAcc;                                                                // REPLACE_ALL_MOVES
                    bestPriority = currPriority;
                    goodMoves = 1 << i;
                } else if (currAcc == bestAcc || currAcc >= 100) {
                    if (priorityCalc(ctx.battle, atk, cur) > bestPriority) {
                        bestAcc = currAcc;
                        bestPriority = currPriority;
                        goodMoves = 1 << i;
                    } else {
                        goodMoves |= 1 << i;
                    }
                }
            }
        }
        int movesToCheck = perfectMoves == 0 ? goodMoves : perfectMoves;                              // :380
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            if ((movesToCheck & (1 << i)) != 0 && atk.moveSlot(i) == checkMove) return true;          // :384-405
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Small predicates
    // ------------------------------------------------------------------

    /** {@code MoveEffectInMoveset(effect,bank)} for a set of function codes. */
    static boolean moveFunctionInMoveset(Battler b, String... codes) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && oneOf(m, codes)) return true;
        }
        return false;
    }

    /** {@code EFFECT_PROTECT} function codes, from the movefx classes (Endure 0E8 is a different effect). */
    static final String[] PROTECT = {"0AA", "14B", "14C", "168"};   // Protect/Detect, King's Shield, Spiky Shield, Baneful Bunker (movefx table)

    /** {@code MoveThatCanHelpAttacksHitInMoveset(bank)} (ai_util.c:4207): accuracy-up / evasion-down moves and Lock-On. */
    static boolean moveThatCanHelpAttacksHitInMoveset(AiCtx ctx, Battler b) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m == null) break;
            if (!usable(ctx, b, i)) continue;
            if (oneOf(m, "0A6")) return true;                                                           // EFFECT_LOCK_ON (Laser Focus is 15E)
            if (m.statusMove() && oneOf(m, "048")) return true;                                         // EVASION_DOWN (Sweet Scent); no pure accuracy-up move exists (Hone Claws 029 is ATK_ACC_UP)
        }
        return false;
    }

    /** {@code IsMovePredictionHealingMove(bankAtk,bankDef)} (ai_util.c:3471): restore HP, morning sun, swallow, wish. */
    static boolean movePredictionIsHealing(AiCtx ctx, Battler atk) {
        BattleMove m = ctx.prediction(atk);
        return m != null && oneOf(m, "0D5", "0D6", "0D8", "0D7", "114");
    }

    /** {@code IsMoxieAbility(ability)}. */
    static boolean isMoxie(String ability) {
        return "MOXIE".equals(ability) || "BEASTBOOST".equals(ability) || "CHILLINGNEIGH".equals(ability)
                || "GRIMNEIGH".equals(ability) || "ASONEGHOST".equals(ability);
    }

    /** {@code CanHealFirstToPreventKnockOut(bankAtk,foe)} (ai_util.c:2478): a healing move that goes first and keeps the foe from a KO. */
    static boolean canHealFirstToPreventKnockOut(AiCtx ctx, Battler atk, Battler foe) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m == null) break;
            if (!usable(ctx, atk, i)) continue;
            if (oneOf(m, "0D5", "0D6", "0D8", "0DD", "0DE")) {                                        // restore hp, morning sun, absorb, dream eater
                int heal = atk.maxHp() / 2;
                if (moveWouldHitFirst(ctx, m, atk, foe) && !canKnockOutAfterHealing(ctx, foe, atk, heal)) return true;
            }
        }
        return false;
    }

    /** {@code CanKnockOutAfterHealing(bankAtk,bankDef,healAmount,numHits,checkAlwaysHits)}. */
    static boolean canKnockOutAfterHealing(AiCtx ctx, Battler atk, Battler def, int healAmount) {
        int hp = Math.min(def.hp + healAmount, def.maxHp());
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m == null) break;
            if (!usable(ctx, atk, i)) continue;
            if (finalDamage(ctx, m, atk, def, 1) >= hp) return true;
        }
        return false;
    }

    /** {@code IsChoiceItemEffectOrAbility}. */
    static boolean choiceLocked(Battler b) {
        return b.hasActiveItem(new String[] {"CHOICEBAND", "CHOICESPECS", "CHOICESCARF"}) || b.hasActiveAbility("GORILLATACTICS");
    }

    /** {@code ShouldUseFakeOut(bankAtk,bankDef,defAbility)} (ai_advanced.c:1430-1475), the single-battle branch. */
    static boolean shouldUseFakeOut(AiCtx ctx, Battler atk, Battler def) {
        if (!AiCalc.firstTurn(atk) || def.hasActiveAbility(new String[] {"INNERFOCUS", "SHIELDDUST", "STEADFAST"})
                || def.effects.intVal(PBEffects.Battler.Substitute) > 0) return false;                  // isFirstTurn / CanBeFlinched
        if (choiceLocked(atk) && AiUtil.benchAlive(ctx.battle, atk) <= 0) {
            if (AiUtil.benchAlive(ctx.battle, def) == 0) {
                BattleMove fakeOut = null;
                for (int i = 0; i < Battler.MOVES_MAX; i++) {
                    BattleMove m = atk.moveSlot(i);
                    if (m != null && named(m, "FAKEOUT")) fakeOut = m;
                }
                return fakeOut != null && knocksOutXHits(ctx, fakeOut, atk, def, 1);
            }
            return false;
        }
        return true;
    }

    /** {@code gDisableStructs[bank].isFirstTurn}: the battler has been on the field for 0 attack phases. */
    static boolean firstTurn(Battler b) {
        return b.turnCount == 0;
    }

    // ------------------------------------------------------------------
    // Fighting style (ai_advanced.c:340-560, singles)
    // ------------------------------------------------------------------

    /** {@code PredictBankFightingStyle(bank)} -> {@code PredictFightingStyle} singles branch (ai_advanced.c:380-560). */
    static int fightingStyle(AiCtx ctx, Battler bank) {
        boolean leechSeed = false;
        boolean protectionMove = false;
        boolean boostingMove = false;
        boolean healingMove = false;
        boolean auroraVeil = false;
        boolean phazingMove = false;
        int attackMoveNum = 0;
        int entryHazardNum = 0;
        int reflectionNum = 0;
        int statusMoveNum = 0;
        int cls = CLASS_NONE;
        Battler foe = ctx.foeOf(bank);
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove move = bank.moveSlot(i);
            if (move == null) continue;                                                              // :414
            boolean batonPassClass = false;
            if (named(move, "BATONPASS") && AiUtil.benchAlive(ctx.battle, bank) > 0) {              // :416-428 ViableMonCountFromBankLoadPartyRange > 1
                cls = CLASS_BATON_PASS;
                batonPassClass = true;
            }
            if (!batonPassClass) {
                String f = move.function();
                if (f.equals("0EB")) {                                                               // EFFECT_ROAR
                    phazingMove = true;
                } else if (f.equals("051")) {                                                        // EFFECT_HAZE
                    cls = CLASS_PHAZING;
                } else if (f.equals("0D7") || f.equals("019")) {                                     // EFFECT_WISH / EFFECT_HEAL_BELL
                    cls = CLASS_CLERIC;
                } else if (f.equals("167")) {                                                        // EFFECT_REFLECT / MOVE_AURORAVEIL
                    if (bank.pbOwnSide().effects.intVal(PBEffects.Side.AuroraVeil) == 0) auroraVeil = true;
                } else if (f.equals("0A2")) {                                                        // EFFECT_REFLECT
                    reflectionNum++;
                } else if (f.equals("0A3")) {                                                        // EFFECT_LIGHT_SCREEN
                    reflectionNum++;
                } else if (f.equals("0DC")) {                                                        // EFFECT_LEECH_SEED
                    leechSeed = true;
                } else if (oneOf(move, PROTECT)) {                                                   // EFFECT_PROTECT
                    protectionMove = true;
                } else if (f.equals("0EF") || f.equals("0CF")) {                                     // EFFECT_MEAN_LOOK / EFFECT_TRAP
                    cls = CLASS_STALL;
                } else {
                    if (choiceLocked(bank) || bank.hasActiveItem("ASSAULTVEST")) {
                        cls = CLASS_SWEEPER_KILL;                                                    // :454
                    } else if (f.equals("0D5") || f.equals("0D6") || f.equals("0D8") || f.equals("114")) {
                        healingMove = true;                                                          // :458 restore hp / morning sun / swallow
                    }
                }
            }
            if (cls != CLASS_NONE) break;                                                            // :465
            if (!move.statusMove()) attackMoveNum++;                                                 // :468
            int c = code(move);
            String f = move.function();
            if (move.statusMove() && c >= 0x1C && c <= 0x3B) {                                       // the stat-boosting effects :474-492
                boostingMove = true;
            } else if (f.equals("103") || f.equals("104") || f.equals("105") || f.equals("153")) {   // EFFECT_SPIKES
                BattleSide foeSide = foe == null ? null : foe.pbOwnSide();
                if (named(move, "STEALTHROCK")) {
                    if (foeSide != null && foeSide.effects.intVal(PBEffects.Side.StealthRock) == 0) entryHazardNum++;
                } else if (named(move, "TOXICSPIKES")) {
                    if (foeSide != null && foeSide.effects.intVal(PBEffects.Side.ToxicSpikes) < 2) entryHazardNum++;
                } else if (named(move, "STICKYWEB")) {
                    if (foeSide != null && foeSide.effects.intVal(PBEffects.Side.StickyWeb) == 0) entryHazardNum++;
                } else {
                    if (foeSide != null && foeSide.effects.intVal(PBEffects.Side.Spikes) < 3) entryHazardNum++;
                }
            } else if (move.statusMove()) {
                statusMoveNum++;                                                                     // :540 default
            }
        }
        if (cls == CLASS_NONE) {                                                                     // :547
            if (reflectionNum >= 2 || auroraVeil) {
                cls = attackMoveNum >= 2 ? CLASS_SWEEPER_SETUP_SCREENS : CLASS_SCREENS;
            } else if (entryHazardNum >= 1) {
                cls = phazingMove ? CLASS_PHAZING : CLASS_ENTRY_HAZARDS;
            } else if (attackMoveNum >= 3) {
                if (boostingMove) cls = CLASS_SWEEPER_SETUP_STATS;
                else if (statusMoveNum > 0) cls = CLASS_SWEEPER_SETUP_STATUS;
                else if (phazingMove) cls = CLASS_SWEEPER_SETUP_STATUS;
                else cls = CLASS_SWEEPER_KILL;
            } else if (leechSeed && protectionMove) {
                cls = CLASS_STALL;
            } else if (attackMoveNum >= 2 && (boostingMove || statusMoveNum > 0 || phazingMove)) {
                cls = boostingMove ? CLASS_SWEEPER_SETUP_STATS : CLASS_SWEEPER_SETUP_STATUS;
            } else {
                cls = CLASS_STALL;                                                                   // healingMove or the default
            }
        }
        return cls;
    }

    static boolean classSweeper(int c) {
        return c == CLASS_SWEEPER_KILL || c == CLASS_SWEEPER_SETUP_STATS || c == CLASS_SWEEPER_SETUP_STATUS;
    }

    /** {@code IsClassDamager}: singles - a sweeper. */
    static boolean classDamager(int c) {
        return classSweeper(c);
    }

    static boolean classStall(int c) {
        return c == CLASS_STALL;
    }

    // ------------------------------------------------------------------
    // Stat / moveset predicates used by the per-effect cases
    // ------------------------------------------------------------------

    /** {@code AI_STAT_CAN_RISE(bank,stat)}: the stage is below +6. */
    static boolean statCanRise(Battler b, int stat) {
        return b.stage(stat) < 6;
    }

    /**
     * {@code CanStatBeLowered(stat,bankDef,bankAtk,defAbility)} (ai_util.c): not at -6, not protected by Mist or a stat-protecting Ability
     * (Mold Breaker of {@code atk} ignores the Ability). 登记: Shield Dust-like secondary-only cases.
     */
    static boolean statCanBeLowered(Battler def, Battler atk, int stat) {
        if (def.stage(stat) <= -6) return false;
        if (def.pbOwnSide().effects.intVal(PBEffects.Side.Mist) > 0) return false;
        if (!atk.hasMoldBreaker()) {
            if (def.hasActiveAbility(new String[] {"CLEARBODY", "WHITESMOKE", "FULLMETALBODY", "MIRRORARMOR"})) return false;
            if (stat == PBStats.ATTACK && def.hasActiveAbility("HYPERCUTTER")) return false;
            if (stat == PBStats.DEFENSE && def.hasActiveAbility("BIGPECKS")) return false;
            if (stat == PBStats.ACCURACY && def.hasActiveAbility("KEENEYE")) return false;
        }
        return true;
    }

    /** {@code RealPhysicalMoveInMoveset(bank)}: a usable damaging Physical move. */
    static boolean physicalMoveInMoveset(AiCtx ctx, Battler b) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && m.power() > 0 && !m.statusMove() && m.physical() && usable(ctx, b, i)) return true;
        }
        return false;
    }

    /** {@code SpecialMoveInMoveset(bank)}: a usable damaging Special move. */
    static boolean specialMoveInMoveset(AiCtx ctx, Battler b) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && m.power() > 0 && !m.statusMove() && !m.physical() && usable(ctx, b, i)) return true;
        }
        return false;
    }

    /** {@code DamagingMoveInMoveset(bank)}. */
    static boolean damagingMoveInMoveset(AiCtx ctx, Battler b) {
        return physicalMoveInMoveset(ctx, b) || specialMoveInMoveset(ctx, b);
    }

    /** {@code GOOD_AI_MOVE_LOCKED} (ai_negatives.c:70): a choice lock or Encore is a reason not to set up. */
    static boolean goodAiMoveLocked(AiCtx ctx, Battler atk) {
        return ctx.goodAi() && (choiceLocked(atk) || atk.effects.intVal(PBEffects.Battler.Encore) > 0);
    }

    /** {@code IsTrickRoomActive() && !IsTrickRoomOnLastTurn()}. */
    static boolean trickRoomNotEnding(Battle battle) {
        return battle.field.effects.intVal(PBEffects.Field.TrickRoom) > 1;
    }

    /** {@code CanBePutToSleep(bankDef,bankAtk,TRUE)}. 登记: Sweet Veil's ally check, Flower Veil. */
    static boolean canBePutToSleep(Battle battle, Battler def, Battler atk) {
        if (def.statused()) return false;
        if (def.fainted()) return false;
        if (!atk.hasMoldBreaker() && def.hasActiveAbility(new String[] {"INSOMNIA", "VITALSPIRIT", "SWEETVEIL", "COMATOSE"})) return false;
        if (def.pbOwnSide().effects.intVal(PBEffects.Side.Safeguard) > 0 && !atk.hasActiveAbility("INFILTRATOR")) return false;
        int terrain = battle.terrain();
        if (!def.airborne() && (terrain == PBBattleTerrains.Electric || terrain == PBBattleTerrains.Misty)) return false;
        return true;
    }

    /** {@code MoveBlockedBySubstitute(move,bankAtk,bankDef)}. */
    static boolean blockedBySubstitute(BattleMove move, Battler atk, Battler def) {
        return def.effects.intVal(PBEffects.Battler.Substitute) > 0 && !has(move, 'k') && !atk.hasActiveAbility("INFILTRATOR");
    }

    /** {@code DamagingMoveTypeInMoveset(bank,type)}. */
    static boolean damagingTypeInMoveset(AiCtx ctx, Battler b, String type) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && !m.statusMove() && type.equals(fx(m).pbCalcType(m, b)) && usable(ctx, b, i)) return true;
        }
        return false;
    }

    /** {@code GetHealthPercentage(bank)} (0..100). */
    static int healthPercent(Battler b) {
        return AiUtil.hpPercent(b);
    }

    private static boolean statusBlockedByTerrainOrSafeguard(Battle battle, Battler def, Battler atk) {
        if (def.pbOwnSide().effects.intVal(PBEffects.Side.Safeguard) > 0 && !atk.hasActiveAbility("INFILTRATOR")) return true;
        return battle.terrain() == PBBattleTerrains.Misty && !def.airborne();
    }

    /** {@code CanBePoisoned(bankDef,bankAtk,TRUE)}. 登记: Leaf Guard / Flower Veil, Pastel Veil. */
    static boolean canBePoisoned(Battle battle, Battler def, Battler atk) {
        if (def.statused() || def.fainted()) return false;
        if (!atk.hasActiveAbility("CORROSION") && (def.hasType("POISON") || def.hasType("STEEL"))) return false;
        if (!atk.hasMoldBreaker() && def.hasActiveAbility(new String[] {"IMMUNITY", "COMATOSE", "PASTELVEIL"})) return false;
        return !statusBlockedByTerrainOrSafeguard(battle, def, atk);
    }

    /** {@code CanBeParalyzed(bankDef,bankAtk,TRUE)}. */
    static boolean canBeParalyzed(Battle battle, Battler def, Battler atk) {
        if (def.statused() || def.fainted() || def.hasType("ELECTRIC")) return false;
        if (!atk.hasMoldBreaker() && def.hasActiveAbility(new String[] {"LIMBER", "COMATOSE"})) return false;
        return !statusBlockedByTerrainOrSafeguard(battle, def, atk);
    }

    /** {@code CanBeBurned(bankDef,bankAtk,TRUE)}. */
    static boolean canBeBurned(Battle battle, Battler def, Battler atk) {
        if (def.statused() || def.fainted() || def.hasType("FIRE")) return false;
        if (!atk.hasMoldBreaker() && def.hasActiveAbility(new String[] {"WATERVEIL", "WATERBUBBLE", "COMATOSE", "THERMALEXCHANGE"})) return false;
        return !statusBlockedByTerrainOrSafeguard(battle, def, atk);
    }

    /** {@code CanBeConfused(bankDef,bankAtk,TRUE)}. */
    static boolean canBeConfused(Battle battle, Battler def, Battler atk) {
        if (def.fainted() || def.effects.intVal(PBEffects.Battler.Confusion) > 0) return false;
        if (!atk.hasMoldBreaker() && def.hasActiveAbility("OWNTEMPO")) return false;
        return !statusBlockedByTerrainOrSafeguard(battle, def, atk);
    }

    /** {@code CanRest(bank)}: below full HP, not asleep or Insomniac, terrain permitting. */
    static boolean canRest(Battle battle, Battler b) {
        if (b.hp >= b.maxHp() || b.hasStatus("SLEEP")) return false;
        if (b.hasActiveAbility(new String[] {"INSOMNIA", "VITALSPIRIT", "COMATOSE"})) return false;
        int terrain = battle.terrain();
        return !( (terrain == PBBattleTerrains.Electric || terrain == PBBattleTerrains.Misty) && !b.airborne());
    }
}
