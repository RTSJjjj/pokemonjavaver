package pokemon.runtime.battle;

/**
 * The Negatives cases that read the used-move history (ai_negatives.c): Copycat (:849), Spite (:1672), Mimic (:1684), Disable (:1714),
 * Encore (:1755), Conversion 2 (:1831), Sketch (:1861). The history is {@code Battler.movesUsed} / {@code lastMoveUsed}
 * (BATTLE_HISTORY->usedMoves, gLastUsedMoves) and {@code Battle.lastMoveUsed} (gNewBS->LastUsedMove).
 *
 * <p>登记: the Copycat/Mimic banned-move flags ({@code gCopycatBannedMoves}, {@code gMimicBannedMoves}) are approximated by the move's own
 * function code; {@code CanLastMoveNotBeEncored}; Z/Max moves; Mirror Move ({@code lastTakenMoveFrom}); the Counter
 * "tried the same move last turn" branch ({@code previousMovePredictions}).</p>
 */
final class AiNegativeHistory {

    private AiNegativeHistory() {
    }

    /** @return true when the function code had a case here. */
    static boolean apply(AiCtx ctx, Battler atk, Battler def, BattleMove move, AiNegativeEffects.Result r) {
        Battle battle = ctx.battle;
        String f = move.function();
        BattleMove predicted = ctx.prediction(def);
        BattleMove lastDef = AiCalc.lastUsedMove(battle, def);
        switch (f) {
            case "0AF": {                                                                          // MOVE_COPYCAT (:849)
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def) || predicted == null) {
                    BattleMove last = AiCalc.globalLastUsedMove(battle);
                    if (last == null || banned(last, "0AF") || inMoveset(atk, last)) {
                        r.viability -= 10;
                    } else {
                        r.viability = AiNegatives.score(ctx, atk, def, last, r.viability);
                    }
                } else if (banned(predicted, "0AF") || inMoveset(atk, predicted)) {
                    r.viability -= 10;
                } else {
                    r.viability = AiNegatives.score(ctx, atk, def, predicted, r.viability);
                }
                return true;
            }
            case "10E": {                                                                          // EFFECT_SPITE (:1672)
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
                    if (lastDef == null) r.viability -= 10;
                } else if (predicted == null) {
                    r.viability -= 10;
                } else {
                    AiNegativeEffects.substituteCheck(move, atk, def, r);
                }
                return true;
            }
            case "05C": {                                                                          // EFFECT_MIMIC (:1684)
                if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
                    if (lastDef == null || banned(lastDef, "05C")) r.viability -= 10;
                } else if (predicted == null) {
                    r.viability -= 10;
                } else {
                    AiNegativeEffects.substituteCheck(move, atk, def, r);
                }
                return true;
            }
            case "0B9": {                                                                          // EFFECT_DISABLE (:1714)
                if (def.effects.intVal(PBEffects.Battler.Disable) == 0 && !def.hasActiveItem("MENTALHERB")) {
                    if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
                        if (lastDef == null) r.viability -= 10;
                    } else if (predicted == null) {
                        r.viability -= 10;
                    }
                } else {
                    r.viability -= 10;
                }
                return true;
            }
            case "0BC": {                                                                          // EFFECT_ENCORE (:1755)
                if (def.effects.intVal(PBEffects.Battler.Encore) == 0 && !def.hasActiveItem("MENTALHERB")) {
                    if (AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
                        if (lastDef == null || banned(lastDef, "0BC")) r.viability -= 10;         // CanLastMoveNotBeEncored (approximated)
                    } else if (predicted == null) {
                        r.viability -= 10;
                    }
                } else {
                    r.viability -= 10;
                }
                return true;
            }
            case "05F": {                                                                          // EFFECT_CONVERSION_2 (:1831)
                if (def.lastMoveUsedType == null) r.viability -= 10;
                return true;
            }
            case "05D": {                                                                          // EFFECT_SKETCH (:1861)
                if (lastDef == null) r.viability -= 10;
                else AiNegativeEffects.substituteCheck(move, atk, def, r);
                return true;
            }
            case "063": case "064": case "065": case "066": case "067": case "068": {             // EFFECT_SKILL_SWAP family (:2560)
                if (abilityMoveFails(f, atk, def) || (!f.equals("067") && !f.equals("065") && AiCalc.blockedBySubstitute(move, atk, def))) {
                    r.viability -= 10;
                } else {
                    AiNegativeEffects.substituteCheck(move, atk, def, r);
                }
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * The failure rules of the ability-changing moves. CFRU keeps them in {@code gSpecialAbilityFlags}; this project's plugin has its own
     * (Move_Effects_000-07F.rb:2082-2355, mirrored in {@code PokeBattle_Move_063..068}), which is what actually decides whether the move works.
     */
    private static boolean abilityMoveFails(String f, Battler atk, Battler def) {
        String a = atk.ability == null ? "" : atk.ability;
        String d = def.ability == null ? "" : def.ability;
        if (def.hasActiveItem("ABILITYSHIELD") && !f.equals("065")) return true;
        switch (f) {
            case "063": return def.unstoppableAbility(null) || d.equals("TRUANT") || d.equals("SIMPLE");
            case "064": return def.unstoppableAbility(null) || d.equals("TRUANT") || d.equals("INSOMNIA");
            case "068": return def.unstoppableAbility(null) || def.effects.truthy(PBEffects.Battler.GastroAcid);
            case "066": return a.isEmpty() || atk.ungainableAbility(null) || a.equals("POWEROFALCHEMY") || a.equals("RECEIVER") || a.equals("TRACE")
                    || def.unstoppableAbility(null) || d.equals("TRUANT");
            case "065": return atk.unstoppableAbility(null) || d.isEmpty() || a.equals(d) || def.ungainableAbility(null)
                    || d.equals("POWEROFALCHEMY") || d.equals("RECEIVER") || d.equals("TRACE") || d.equals("WONDERGUARD");
            default:                                                                                // 067 Skill Swap
                return a.isEmpty() || d.isEmpty() || atk.unstoppableAbility(null) || atk.ungainableAbility(null) || a.equals("WONDERGUARD")
                        || def.unstoppableAbility(null) || def.ungainableAbility(null) || d.equals("WONDERGUARD");
        }
    }

    private static boolean banned(BattleMove m, String selfCode) {
        return m.function().equals(selfCode) || m.function().equals("0B0");                       // the copying move itself, Me First
    }

    private static boolean inMoveset(Battler atk, BattleMove m) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove own = atk.moveSlot(i);
            if (own != null && own.internalName().equals(m.internalName())) return true;
        }
        return false;
    }
}
