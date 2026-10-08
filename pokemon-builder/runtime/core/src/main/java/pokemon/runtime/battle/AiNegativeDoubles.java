package pokemon.runtime.battle;

/**
 * ai_negatives.c: the partner-aware conditions that the per-effect cases OR into their "this move cannot work" test
 * ({@code PARTNER_MOVE_EFFECT_IS_SAME}, {@code _SAME_NO_TARGET}, {@code _STATUS_SAME_TARGET}, {@code _IS_WEATHER},
 * {@code _IS_TERRAIN}, {@code _IS_MISTY_TERRAIN}, {@code _SLEEP_BLOCKING_TERRAIN}, {@code PARTNER_MOVE_IS_TAILWIND_TRICKROOM},
 * ai_negatives.c:36-95, {@code PartnerMoveEffectIsStatusSameTarget} :3269, {@code IsCurrentWeatherPartnersWeather}
 * ai_util.c:2002). Every case that uses them ends in {@code DECREASE_VIABILITY(10)}.
 *
 * <p>登记: the Max-move variants of the macros (no Dynamax), Foresight / Miracle Eye ({@code DECREASE_VIABILITY(9)} in C
 * and only when the evasion check fails), Rapid Spin / Defog, Pledge, Perish Song's second foe, and the "Helping Hand while
 * the partner switches out" clause.</p>
 */
final class AiNegativeDoubles {
    private AiNegativeDoubles() {
    }

    private static final String[] SAME_TARGET = {"0EB", "0DC", "0B9", "0BC", "0A6", "0EF", "10F", "0E5", "016", "0B7", "0E2", "0BA"};
    private static final String[] SAME_NO_TARGET = {"051", "056", "0A2", "0A3", "167", "019", "01A", "0B2", "09D", "09E", "09C", "117"};
    private static final String[] INFLICT = {"003", "004", "005", "006", "007", "00A"};
    private static final String[] TERRAIN = {"154", "155", "156", "173"};

    /** @return true when a partner-related condition makes {@code move} on {@code def} a move to avoid (the case's {@code DECREASE_VIABILITY(10)}). */
    static boolean blocks(AiCtx ctx, Battler atk, Battler def, BattleMove move) {
        Battle battle = ctx.battle;
        if (!AiDoublesScore.isDouble(battle, atk)) return false;
        Battler partner = AiDoublesScore.partner(battle, atk);
        if (partner == null) return false;
        BattleMove pm = battle.chosenMove(partner.index);                                          // gChosenMovesByBanks[bankAtkPartner] != MOVE_NONE
        String f = move.function();
        Object[] pc = battle.choices(partner.index);
        boolean sameTargetAsMine = pc[3] instanceof Integer && (Integer) pc[3] == def.index;
        boolean grounded = !def.airborne();

        // weather moves also avoid overriding the partner's weather
        if (AiCalc.oneOf(move, "0FF", "100", "101", "102", "1C0")) {
            if (pm != null && AiCalc.oneOf(pm, "0FF", "100", "101", "102", "1C0")) return true;
            return currentWeatherIsPartners(battle, partner);
        }
        if (pm == null) return false;
        if (AiCalc.oneOf(move, TERRAIN)) return AiCalc.oneOf(pm, TERRAIN);
        if (f.equals("11F")) return AiCalc.oneOf(pm, "05B", "11F");                                // PARTNER_MOVE_IS_TAILWIND_TRICKROOM
        if (AiCalc.oneOf(move, SAME_TARGET)) {
            if (f.equals("10D") && !ownsGhostCurse(atk)) return false;
            return f.equals(pm.function()) && sameTargetAsMine;
        }
        if (AiCalc.oneOf(move, SAME_NO_TARGET)) return f.equals(pm.function());
        if (AiCalc.oneOf(move, "003", "004", "005", "006", "007", "00A", "013", "018")) {
            if (statusSameTarget(ctx, partner, pm, move, def, sameTargetAsMine)) return true;
            boolean misty = AiCalc.named(pm, "MISTYTERRAIN") && grounded;                           // PARTNER_MOVE_EFFECT_IS_MISTY_TERRAIN
            if (misty) return true;
            return AiCalc.oneOf(move, "003", "004")
                    && (AiCalc.named(pm, "ELECTRICTERRAIN") && grounded);                           // _SLEEP_BLOCKING_TERRAIN (Electric)
        }
        return false;
    }

    private static boolean ownsGhostCurse(Battler atk) {
        return atk.hasType("GHOST");
    }

    /** {@code PartnerMoveEffectIsStatusSameTarget(data,move,bankDef)} (ai_negatives.c:3269). */
    private static boolean statusSameTarget(AiCtx ctx, Battler partner, BattleMove pm, BattleMove move, Battler def, boolean sameTarget) {
        if (!sameTarget) return false;
        if (AiCalc.oneOf(pm, INFLICT)) return true;
        if (AiCalc.isSideEffectHit(pm) && AiCalc.oneOf(pm, "005", "006", "007", "00A", "00C")) {
            return AiCalc.secondaryEffectChance(pm, partner) >= 75 && !AiCalc.blockedBySubstitute(move, partner, def);
        }
        return false;
    }

    /** Weather-setting abilities: the CFRU ones plus the project's / Gen 9 ones, which share the engine's weather handler. */
    private static final java.util.Set<String> SUN_SETTERS = new java.util.HashSet<>(java.util.Arrays.asList(
            "DROUGHT", "RAINBOWARCH", "ORICHALCUMPULSE", "ETERNALFLAME", "CALAMITYINFERNAL"));
    private static final java.util.Set<String> RAIN_SETTERS = new java.util.HashSet<>(java.util.Arrays.asList(
            "DRIZZLE", "BESTOWEDRAIN", "STORMEYE", "CALAMITYABYSSAL"));

    /** {@code IsCurrentWeatherPartnersWeather(partner,partnerAbility)} (ai_util.c:2002). */
    private static boolean currentWeatherIsPartners(Battle battle, Battler partner) {
        String a = partner.ability == null ? "" : partner.ability;
        int w = battle.pbWeather();
        if (w == PBWeather.Sun || w == PBWeather.HarshSun) return SUN_SETTERS.contains(a) || AiCalc.moveFunctionInMoveset(partner, "0FF");
        if (w == PBWeather.Rain || w == PBWeather.HeavyRain) return RAIN_SETTERS.contains(a) || AiCalc.moveFunctionInMoveset(partner, "100");
        if (w == PBWeather.Sandstorm) return a.equals("SANDSTREAM") || AiCalc.moveFunctionInMoveset(partner, "101");
        if (w == PBWeather.Hail || w == PBWeather.Snow) return a.equals("SNOWWARNING") || AiCalc.moveFunctionInMoveset(partner, "102", "1C0");
        return false;
    }
}
