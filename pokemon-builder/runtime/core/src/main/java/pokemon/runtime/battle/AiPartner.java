package pokemon.runtime.battle;

import java.util.List;

/**
 * CFRU {@code AIScript_Partner} ({@code ai_partner.c:37-659}): how the AI scores a move aimed at its own partner
 * (Helping Hand, Beat Up on an ally with Justified, Skill Swap away a bad Ability, ...). {@code AIScript_Positives} and
 * {@code AIScript_SemiSmart} hand over to it when {@code IS_DOUBLE_BATTLE && TARGETING_PARTNER}.
 *
 * <p>CFRU effect -> this project's function codes: Howl 01C, Aromatic Mist 138, Acupressure 037, Rototiller 13E,
 * Gear Up 15C, Magnetic Flux 137, Purify 15B, Life Dew 17E, Jungle Healing 189, Swagger 041, Flatter 040, Sandstorm 101,
 * Rain Dance 100, Sunny Day 0FF, Hail 102 / Snowscape 1C0, Beat Up 0C1, Helping Hand 09C / Decorate 17B / Coaching 18E,
 * Psych Up 055, Skill Swap family 063-068, Soak 061, Heal Pulse 0DF, Ion Deluge 146, Magnet Rise 119, After You 11D,
 * Instruct 16B.</p>
 *
 * <p>登记: Z-moves and Max moves (no such mechanic); {@code CanKnockOffItem} is "holds a transferable item" (no wild-item
 * rule); Ion Deluge's second foe reads {@code data->foe1} twice in CFRU ({@code :594}) and does here as well; the Mummy /
 * Lightning-Rod style checks use every living foe instead of exactly two. The move tables ({@code gStatLoweringMoveEffects},
 * {@code DoesProtectionMoveBlockMove}) are transcribed from the CFRU repository.</p>
 */
final class AiPartner {
    private AiPartner() {
    }

    /** {@code AIScript_Partner(bankAtk,bankAtkPartner,originalMove,originalViability,data)}. */
    static int score(AiCtx ctx, Battler atk, Battler partner, BattleMove move, int originalViability) {
        Battle battle = ctx.battle;
        int viability = originalViability;
        int cls = AiCalc.fightingStyle(ctx, atk);
        int partnerCls = AiCalc.fightingStyle(ctx, partner);
        String f = move.function();
        String type = AiCalc.fx(move).pbCalcType(move, atk);
        BattleMove partnerMove = AiPositiveHelpers.isIncapacitated(partner) ? null : battle.chosenMove(partner.index);
        String partnerAbility = partner.ability == null ? "" : partner.ability;
        String atkAbility = atk.ability == null ? "" : atk.ability;
        if (atk.hasMoldBreaker() && !partnerAbility.isEmpty()) partnerAbility = "";                  // IsTargetAbilityIgnored
        boolean physicalMove = !move.statusMove() && move.physical();
        boolean statusMove = move.statusMove();
        boolean partnerProtects = doesProtectionMoveBlockMove(battle, atk, partner, move, partnerMove);
        boolean onlyHitsBothFoes = AiCalc.fx(move).pbTarget(move, atk) == PBTargets.AllNearFoes;     // MOVE_TARGET_BOTH
        List<Battler> foes = AiDoublesScore.foes(battle, atk);
        boolean totalSupport = AiCalc.classDoublesTotalTeamSupport(partnerCls);

        if (!partnerProtects && !onlyHitsBothFoes) {
            switch (partnerAbility) {                                                                 // type-specific ability checks
                case "VOLTABSORB":
                    if (type.equals("ELECTRIC")) viability = AiDoublesScore.increaseHealPartner(ctx, viability, cls, partner);
                    break;
                case "MOTORDRIVE":
                    if (type.equals("ELECTRIC") && !totalSupport && goodIdeaToRaisePartnerSpeed(ctx, partner, foes, 1)) {
                        viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                    }
                    break;
                case "LIGHTNINGROD":
                    if (type.equals("ELECTRIC") && !totalSupport && AiCalc.specialMoveInMoveset(ctx, partner)
                            && AiCalc.statCanRise(partner, PBStats.SPATK)) {
                        viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                    }
                    break;
                case "WATERABSORB": case "DRYSKIN":
                    if (type.equals("WATER")) viability = AiDoublesScore.increaseHealPartner(ctx, viability, cls, partner);
                    break;
                case "EARTHEATER":                                                                    // project absorbers with the same heal hook (pbBattleMoveImmunityHealAbility)
                    if (type.equals("GROUND")) viability = AiDoublesScore.increaseHealPartner(ctx, viability, cls, partner);
                    break;
                case "ICEBSORB":
                    if (type.equals("ICE")) viability = AiDoublesScore.increaseHealPartner(ctx, viability, cls, partner);
                    break;
                case "STORMDRAIN":
                    if (type.equals("WATER") && !totalSupport && AiCalc.specialMoveInMoveset(ctx, partner)
                            && AiCalc.statCanRise(partner, PBStats.SPATK)) {
                        viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                    }
                    break;
                case "FLASHFIRE":
                    if (type.equals("FIRE") && !totalSupport && AiCalc.damagingTypeInMoveset(ctx, partner, "FIRE")
                            && !partner.effects.truthy(PBEffects.Battler.FlashFire)) {
                        viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                    }
                    break;
                case "SAPSIPPER":
                    if (type.equals("GRASS") && !totalSupport && AiCalc.physicalMoveInMoveset(ctx, partner)
                            && AiCalc.statCanRise(partner, PBStats.ATTACK)) {
                        viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                    }
                    break;
                case "JUSTIFIED":
                    if (type.equals("DARK") && !statusMove && !totalSupport
                            && (!f.equals("0F0") || partnerAbility.equals("STICKYHOLD") || !canKnockOffItem(partner))
                            && AiCalc.physicalMoveInMoveset(ctx, partner) && AiCalc.statCanRise(partner, PBStats.ATTACK)
                            && !AiCalc.knocksOutXHits(ctx, move, atk, partner, 1)) {
                        viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                    }
                    break;
                case "RATTLED":
                    if (!statusMove && !totalSupport && (type.equals("DARK") || type.equals("GHOST") || type.equals("BUG"))
                            && (!f.equals("0F0") || partnerAbility.equals("STICKYHOLD") || !canKnockOffItem(partner))
                            && (!f.equals("0F4") /* EFFECT_EAT_BERRY: Bug Bite / Pluck */ || partnerAbility.equals("STICKYHOLD") || !isBerry(partner))
                            && !AiCalc.trickRoom(battle) && goodIdeaToRaisePartnerSpeed(ctx, partner, foes, 1)
                            && !AiCalc.knocksOutXHits(ctx, move, atk, partner, 1)) {
                        viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                    }
                    break;
                case "STEAMENGINE":
                    if (!statusMove && !totalSupport && (type.equals("WATER") || type.equals("FIRE")) && !AiCalc.trickRoom(battle)
                            && goodIdeaToRaisePartnerSpeed(ctx, partner, foes, 6) && !AiCalc.knocksOutXHits(ctx, move, atk, partner, 1)) {
                        viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                    }
                    break;
                case "CONTRARY":
                    if (!totalSupport && statusMove && AiCalc.oneOf(move, STAT_LOWERING)) viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                    break;
                case "MUMMY":
                    viability = mummy(ctx, viability, cls, atk, partner, move, atkAbility, foes, partnerMove);
                    break;
                default:
                    break;
            }
        }

        switch (f) {
            case "01C":                                                                                // EFFECT_ATTACK_UP: Howl
                if (AiCalc.named(move, "HOWL") && !partnerAbility.equals("CONTRARY") && !partnerAbility.equals("SOUNDPROOF")
                        && AiCalc.statCanRise(partner, PBStats.ATTACK)) viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                break;
            case "138":                                                                                // Aromatic Mist
                if (!partnerProtects && !partnerAbility.equals("CONTRARY") && AiCalc.statCanRise(partner, PBStats.SPDEF)) {
                    viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                }
                break;
            case "037":                                                                                // Acupressure
                if (!partnerProtects && !partnerAbility.equals("CONTRARY") && !totalSupport) {
                    viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                }
                break;
            case "13E":                                                                                // Rototiller
                if (partner.pbHasType("GRASS") && !partnerAbility.equals("CONTRARY") && !partner.airborne()
                        && (AiCalc.statCanRise(partner, PBStats.ATTACK) || AiCalc.statCanRise(partner, PBStats.SPATK))
                        && !totalSupport && AiCalc.damagingMoveInMoveset(ctx, partner)) {
                    viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                }
                break;
            case "15C":                                                                                // Gear Up
                if (plusMinus(partnerAbility) && (AiCalc.statCanRise(partner, PBStats.ATTACK) || AiCalc.statCanRise(partner, PBStats.SPATK))
                        && !totalSupport && AiCalc.damagingMoveInMoveset(ctx, partner)) {
                    viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                }
                break;
            case "137":                                                                                // Magnetic Flux
                if (plusMinus(partnerAbility) && (AiCalc.statCanRise(partner, PBStats.DEFENSE) || AiCalc.statCanRise(partner, PBStats.SPDEF))) {
                    viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                }
                break;
            case "15B":                                                                                // Purify
                viability = purify(ctx, viability, cls, atk, partner, partnerMove, partnerProtects, move);
                break;
            case "17E": case "189":                                                                    // Life Dew, Jungle Healing
                if (!partnerProtects) viability = AiDoublesScore.increaseHealPartner(ctx, viability, cls, partner);
                break;
            case "041":                                                                                // Swagger
                if (partner.stage(PBStats.ATTACK) < 6 && !totalSupport
                        && (!AiCalc.canBeConfused(battle, partner, atk) || partner.hasActiveItem("PERSIMBERRY") || partner.hasActiveItem("LUMBERRY"))) {
                    viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                }
                break;
            case "040":                                                                                // Flatter
                if (partner.stage(PBStats.SPATK) < 6 && !totalSupport
                        && (!AiCalc.canBeConfused(battle, partner, atk) || partner.hasActiveItem("PERSIMBERRY") || partner.hasActiveItem("LUMBERRY"))) {
                    viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                }
                break;
            case "101":                                                                                // Sandstorm
                if (partnerAbility.equals("SANDVEIL") || partnerAbility.equals("SANDRUSH") || partnerAbility.equals("SANDFORCE")
                        || partnerAbility.equals("OVERCOAT") || partnerAbility.equals("MAGICGUARD") || partner.hasActiveItem("SAFETYGOGGLES")
                        || partner.pbHasType("ROCK") || partner.pbHasType("STEEL") || partner.pbHasType("GROUND")
                        || has(partner, "SHOREUP") || has(partner, "WEATHERBALL")) {
                    viability = weatherBoost(ctx, viability, cls, atk, partner);
                }
                break;
            case "100":                                                                                // Rain Dance
                if (!partner.hasActiveItem("UTILITYUMBRELLA")
                        && (thunderInMoveset(partner) || has(partner, "WEATHERBALL")
                        || partner.hasActiveItem("DAMPROCK") || partnerAbility.equals("SWIFTSWIM") || partnerAbility.equals("FORECAST")
                        || partnerAbility.equals("HYDRATION") || partnerAbility.equals("RAINDISH") || partnerAbility.equals("DRYSKIN")
                        || AiCalc.damagingTypeInMoveset(ctx, partner, "WATER"))) {
                    viability = weatherBoost(ctx, viability, cls, atk, partner);
                }
                break;
            case "0FF":                                                                                // Sunny Day
                if (!partner.hasActiveItem("UTILITYUMBRELLA")
                        && (partnerAbility.equals("CHLOROPHYLL") || partnerAbility.equals("FLOWERGIFT") || partnerAbility.equals("FORECAST")
                        || partnerAbility.equals("LEAFGUARD") || partnerAbility.equals("SOLARPOWER") || partnerAbility.equals("HARVEST")
                        || AiCalc.moveFunctionInMoveset(partner, "0C4" /* Solar Beam / Blade */, "0D8" /* Morning Sun, Synthesis, Moonlight */)
                        || has(partner, "WEATHERBALL") || has(partner, "GROWTH") || AiCalc.damagingTypeInMoveset(ctx, partner, "FIRE"))) {
                    viability = weatherBoost(ctx, viability, cls, atk, partner);
                }
                break;
            case "102": case "1C0":                                                                    // Hail / Snowscape
                if (partnerAbility.equals("SNOWCLOAK") || partnerAbility.equals("ICEBODY") || partnerAbility.equals("FORECAST")
                        || partnerAbility.equals("SLUSHRUSH") || partnerAbility.equals("MAGICGUARD") || partnerAbility.equals("OVERCOAT")
                        || has(partner, "BLIZZARD") || has(partner, "AURORAVEIL") || has(partner, "WEATHERBALL")) {
                    viability = weatherBoost(ctx, viability, cls, atk, partner);
                }
                break;
            case "0C1":                                                                                // Beat Up
                viability = beatUp(ctx, viability, cls, atk, partner, move, partnerAbility, physicalMove);
                break;
            case "09C": case "17B": case "18E":                                                        // Helping Hand, Decorate, Coaching
                viability = helpingHand(ctx, viability, cls, atk, partner, move, partnerMove, partnerAbility, partnerProtects, totalSupport);
                break;
            case "055":                                                                                // Psych Up
                viability = increasePsychUp(ctx, viability, cls, atk, partner);
                break;
            case "063": case "064": case "065": case "066": case "067": case "068":                    // EFFECT_SKILL_SWAP / ROLE_PLAY family
                viability = skillSwap(ctx, viability, cls, atk, partner, move, partnerAbility, partnerProtects, partnerMove);
                break;
            case "061":                                                                                // Soak
                if (!partnerProtects && partnerAbility.equals("WONDERGUARD")
                        && !(partner.types().size == 1 && partner.hasType("WATER"))) {
                    viability = AiDoublesScore.increaseHelpingHand(viability, cls);                    // make Wonder Guard water for fewer weaknesses
                }
                break;
            case "0DF":                                                                                // Heal Pulse
                if (!partnerProtects) viability = AiDoublesScore.increaseHealPartner(ctx, viability, cls, partner);
                break;
            case "146":                                                                                // Ion Deluge
                viability = ionDeluge(ctx, viability, atk, partner, partnerAbility, partnerMove, foes);
                break;
            case "119":                                                                                // Magnet Rise
                if (!atk.airborne() && damagingAllHitMoveType(ctx, partner, "GROUND")
                        && !resistsEarthquake(ctx, partner, atk)) {
                    viability = AiPositiveHelpers.incStatus(ctx, viability, cls, 2, atk, partner);
                }
                break;
            case "11D": case "11E": case "16B":                                                        // After You / Quash / Instruct
                if (!partnerProtects) viability = afterYouInstruct(ctx, viability, cls, atk, partner, move, partnerMove, foes);
                break;
            default:
                break;
        }
        return Math.min(viability, 255);
    }

    // ------------------------------------------------------------------
    // Pieces
    // ------------------------------------------------------------------

    /** {@code gStatLoweringMoveEffects} (assembly/data/move_effect_table.s:288) mapped to this project's function codes through battle_moves.c. */
    private static final String[] STAT_LOWERING = {"042", "043", "044", "045", "046", "047", "048", "04A", "04B", "04C", "04D", "04E",
            "04F", "139", "13A", "13C", "13D", "140", "186"};

    /** {@code CheckTableForMovesEffect(move,gStatLoweringMoveEffects)}: a status move that lowers a stat of its target. */
    static boolean statLowering(BattleMove move) {
        return move.statusMove() && AiCalc.oneOf(move, STAT_LOWERING);
    }

    private static boolean plusMinus(String ability) {
        return ability.equals("PLUS") || ability.equals("MINUS");
    }

    private static boolean has(Battler b, String move) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && m.internalName().equals(move)) return true;
        }
        return false;
    }

    private static boolean thunderInMoveset(Battler b) {
        return AiCalc.moveFunctionInMoveset(b, "008", "015" /* Thunder, Hurricane */);
    }

    private static boolean isBerry(Battler b) {
        return b.item != null && b.item.endsWith("BERRY");
    }

    /** {@code CanKnockOffItem(bank)} (battle_util.c:1428): holds an item that can be transferred. */
    private static boolean canKnockOffItem(Battler b) {
        return b.item != null && !b.item.isEmpty() && !b.unlosableItem(b.item);
    }

    /** {@code DamagingAllHitMoveTypeInMoveset(bank,type)} (ai_util.c:3948). */
    private static boolean damagingAllHitMoveType(AiCtx ctx, Battler b, String type) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && AiCalc.usable(ctx, b, i) && AiCalc.fx(m).pbCalcType(m, b).equals(type) && !m.statusMove()
                    && AiDoublesScore.hitsAll(ctx.battle, b, m)) return true;
        }
        return false;
    }

    /** {@code AI_SpecialTypeCalc(MOVE_EARTHQUAKE,partner,atk) & (DOESNT_AFFECT | NOT_VERY_EFFECTIVE)}. */
    private static boolean resistsEarthquake(AiCtx ctx, Battler partner, Battler atk) {
        BattleMove eq = AiCalc.moveByName(ctx.battle, "EARTHQUAKE");
        if (eq == null) return false;
        AiCalc.AiDmg d = AiCalc.calcDmg(ctx.battle, partner, atk, eq);
        return d.typeMod < 8;                                                                          // 0 = no effect, <8 = not very effective (8 = neutral)
    }

    /** {@code GOOD_IDEA_TO_RAISE_PARTNER_SPEED(amount)} (ai_partner.c:29). */
    private static boolean goodIdeaToRaisePartnerSpeed(AiCtx ctx, Battler partner, List<Battler> foes, int amount) {
        if (!AiCalc.statCanRise(partner, PBStats.SPEED)) return false;
        for (Battler foe : foes) if (AiPositiveHelpers.goodIdeaToRaiseSpeed(ctx, partner, foe, amount)) return true;
        return false;
    }

    /** {@code DoesProtectionMoveBlockMove(bankAtk,bankDef,atkMove,protectMove)} (accuracy_calc.c:286). */
    static boolean doesProtectionMoveBlockMove(Battle battle, Battler atk, Battler def, BattleMove atkMove, BattleMove protectMove) {
        if (protectMove == null || atkMove == null) return false;
        boolean protectFlag = AiCalc.has(atkMove, 'b');
        boolean status = atkMove.statusMove();
        if (AiCalc.named(atkMove, "FEINT", "HYPERSPACEFURY", "HYPERSPACEHOLE", "PHANTOMFORCE", "SHADOWFORCE")) return false;   // gMovesThatLiftProtectTable
        int target = AiCalc.fx(atkMove).pbTarget(atkMove, atk);
        switch (protectMove.function()) {
            case "0AA": case "14C": case "168": case "1CC": return protectFlag;                                 // Protect, Spiky Shield, Baneful Bunker
            case "14B": return protectFlag && !status;                                              // King's Shield (and Obstruct)
            case "149": return AiCalc.firstTurn(def) && protectFlag && !status;                    // Mat Block
            case "14A": return target != PBTargets.User && status;                                  // Crafty Shield
            case "0AB": return protectFlag && AiCalc.priorityCalc(battle, atk, atkMove) > 0;        // Quick Guard
            case "0AC": return protectFlag && (target == PBTargets.AllNearFoes || target == PBTargets.AllNearOthers);   // Wide Guard
            default: return false;
        }
    }

    private static int weatherBoost(AiCtx ctx, int viability, int cls, Battler atk, Battler partner) {
        if (AiCalc.classDoublesTeamSupport(cls)) return AiPositiveHelpers.inc(viability, 17);
        return AiPositiveHelpers.incStatus(ctx, viability, cls, 2, atk, partner);
    }

    private static int mummy(AiCtx ctx, int viability, int cls, Battler atk, Battler partner, BattleMove move, String atkAbility,
                             List<Battler> foes, BattleMove partnerMove) {
        int atkSpeed = AiCalc.speed(atk);
        for (Battler foe : foes) {
            if (AiCalc.speed(foe) < atkSpeed) {
                if (AiCalc.canKnockOut(ctx, foe, atk)) return viability;                              // the foe KOs before the benefit can be used
            } else if (AiCalc.can2HKO(ctx, foe, atk)) {
                return viability;
            }
        }
        if (atkAbility.equals("TRUANT") || atkAbility.equals("SLOWSTART") || atkAbility.equals("DEFEATIST")) {
            if (checkContact(move, atk)                                                                // Mummy will transfer
                    && !skillSwapAimedAtMe(ctx.battle, atk, partner, partnerMove)
                    && !AiCalc.noEffect(ctx.battle, atk, partner, move)
                    && isWeakestContactMoveWithBestAccuracy(ctx, move, atk, partner)) {
                viability = AiDoublesScore.increaseDamageToScore(ctx, viability, cls, AiDoublesScore.BEST_KO_SCORE, atk, partner);   // best move to use
            }
        }
        return viability;
    }

    private static boolean checkContact(BattleMove move, Battler atk) {
        return AiCalc.has(move, 'a') && !atk.hasActiveItem("PROTECTIVEPADS") && !atk.hasActiveAbility("LONGREACH");
    }

    /** {@code PARTNER_MOVE_EFFECT_IS_SKILL_SWAP} (ai_partner.c:25). */
    private static boolean skillSwapAimedAtMe(Battle battle, Battler atk, Battler partner, BattleMove partnerMove) {
        if (partnerMove == null) return false;
        Object[] pc = battle.choices(partner.index);
        return pc[3] instanceof Integer && (Integer) pc[3] == atk.index && partnerMove.function().equals("067");
    }

    /** {@code IsWeakestContactMoveWithBestAccuracy(move,bankAtk,bankDef)} (ai_util.c:434). */
    static boolean isWeakestContactMoveWithBestAccuracy(AiCtx ctx, BattleMove move, Battler atk, Battler def) {
        int bestIndex = -1;
        int bestAcc = 0;
        long bestDmg = Long.MAX_VALUE;
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m == null) break;
            if (!AiCalc.usable(ctx, atk, i) || !checkContact(m, atk)) continue;
            if (AiCalc.oneOf(m, "0C2", "071", "072", "012", "0C3", "0C4", "0C5", "0C6", "0C7", "0C8", "0C9", "0CA", "0CB", "0CC", "0CD",
                    "0CE", "111", "070", "162")) continue;                                             // recharge, counters, charge/hiding moves, Future Sight, OHKO
            int acc = AiCalc.moveWillHit(ctx.battle, atk, def, m) ? 101 : AiCalc.hitChance(ctx.battle, atk, def, m);
            long dmg = AiCalc.finalDamage(ctx, m, atk, def, 1);
            if (dmg < bestDmg && acc > bestAcc) {
                bestAcc = acc;
                bestDmg = dmg;
                bestIndex = i;
            }
        }
        return bestIndex >= 0 && atk.moveSlot(bestIndex) == move;
    }

    private static int purify(AiCtx ctx, int viability, int cls, Battler atk, Battler partner, BattleMove partnerMove, boolean partnerProtects,
                              BattleMove move) {
        String status = partner.status;
        if (status == null || status.isEmpty() || partnerProtects) return viability;
        boolean help;
        switch (status) {
            case "POISON":
                partner.status = null;
                help = !AiPositiveItems.goodIdeaToPoisonSelf(ctx, partner);
                partner.status = status;
                break;
            case "BURN":
                partner.status = null;
                help = !AiPositiveItems.goodIdeaToBurnSelf(ctx, partner);
                partner.status = status;
                break;
            case "PARALYSIS":
                partner.status = null;
                help = !goodIdeaToParalyzeSelf(ctx, partner);
                partner.status = status;
                break;
            case "FROZEN": case "FROSTBITE":
                help = true;
                break;
            case "SLEEP":
                help = (partnerMove == null || (!AiCalc.named(partnerMove, "SLEEPTALK") && !AiCalc.named(partnerMove, "SNORE")))
                        || !AiCalc.moveWouldHitFirst(ctx, move, atk, partner);                       // the partner can use a sleep move and then be awoken
                break;
            default:
                help = false;
        }
        return help ? AiDoublesScore.increaseHelpingHand(viability, cls) : viability;
    }

    /** {@code GoodIdeaToParalyzeSelf(bankAtk)} (ai_util.c:2939). */
    private static boolean goodIdeaToParalyzeSelf(AiCtx ctx, Battler a) {
        String ab = a.ability == null ? "" : a.ability;
        return AiCalc.canBeParalyzed(ctx.battle, a, a)
                && (ab.equals("MARVELSCALE") || ab.equals("QUICKFEET") || (ab.equals("GUTS") && AiCalc.physicalMoveInMoveset(ctx, a))
                || has(a, "FACADE") || AiCalc.moveFunctionInMoveset(a, "01B"));
    }

    private static int beatUp(AiCtx ctx, int viability, int cls, Battler atk, Battler partner, BattleMove move, String partnerAbility,
                              boolean physicalMove) {
        boolean statusSplit = move.statusMove();
        boolean noKo = !AiCalc.knocksOutXHits(ctx, move, atk, partner, 1);
        switch (partnerAbility) {
            case "JUSTIFIED":
                if (AiCalc.fx(move).pbCalcType(move, atk).equals("DARK") && !statusSplit && AiCalc.physicalMoveInMoveset(ctx, partner)
                        && AiCalc.statCanRise(partner, PBStats.ATTACK) && noKo) viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                break;
            case "STAMINA":
                if (!statusSplit && AiCalc.statCanRise(partner, PBStats.DEFENSE) && noKo) viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                break;
            case "COTTONDOWN":
                if (!statusSplit && noKo) {
                    for (Battler foe : AiDoublesScore.foes(ctx.battle, atk)) {
                        String fa = foe.ability == null ? "" : foe.ability;
                        if (fa.equals("CONTRARY") || fa.equals("MIRRORARMOR")) return viability;       // do not benefit the enemy
                    }
                    for (Battler foe : AiDoublesScore.foes(ctx.battle, atk)) {
                        if (AiCalc.statCanBeLowered(foe, atk, PBStats.SPEED)) return AiDoublesScore.increaseHelpingHand(viability, cls);
                    }
                }
                break;
            default:
                break;
        }
        return viability;
    }

    private static int helpingHand(AiCtx ctx, int viability, int cls, Battler atk, Battler partner, BattleMove move, BattleMove partnerMove,
                                   String partnerAbility, boolean partnerProtects, boolean totalSupport) {
        if (totalSupport) return viability;                                                           // do not help a partner meant to be doing the helping out
        if (AiCalc.named(move, "DECORATE")) {
            if (!partnerAbility.equals("CONTRARY") && !partnerProtects) {
                if (AiCalc.physicalMoveInMoveset(ctx, partner) && AiCalc.statCanRise(partner, PBStats.ATTACK)) {
                    viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                } else if (AiCalc.specialMoveInMoveset(ctx, partner) && AiCalc.statCanRise(partner, PBStats.SPATK)) {
                    viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                }
            }
        } else if (AiCalc.named(move, "COACHING") && !partnerProtects) {
            if (AiCalc.statCanRise(partner, PBStats.ATTACK) && !partnerAbility.equals("CONTRARY") && AiCalc.physicalMoveInMoveset(ctx, partner)) {
                viability = AiDoublesScore.increaseHelpingHand(viability, cls);
            }
        } else if (partnerMove != null && !partnerProtects && !partnerMove.statusMove()) {            // regular Helping Hand
            Battle battle = ctx.battle;
            BattleMove chosen = battle.chosenMove(partner.index);
            Object[] pc = battle.choices(partner.index);
            int partnerTarget = pc[3] instanceof Integer ? (Integer) pc[3] : -1;
            Battler target = partnerTarget >= 0 ? battle.battlerAt(partnerTarget) : null;
            if (chosen != null && target != null && target.foe != partner.foe                           // already chose a damaging move on a foe
                    && !AiDoublesScore.spread(battle, partner, chosen)                                  // and targeting a single foe
                    && AiCalc.knocksOutXHits(ctx, chosen, partner, target, 1)) {                       // but the partner can already KO
                return viability;                                                                      // do not waste this turn on Helping Hand
            }
            viability = AiDoublesScore.increaseHelpingHand(viability, cls);
        }
        return viability;
    }

    private static int increasePsychUp(AiCtx ctx, int viability, int cls, Battler atk, Battler partner) {
        BattleMove psychUp = AiCalc.moveByName(ctx.battle, "PSYCHUP");
        if (psychUp == null || !AiPositiveItems.goodIdeaToSwapStatStages(ctx, atk, partner)) return viability;
        int[] stats = {PBStats.ATTACK, PBStats.SPATK, PBStats.SPEED, PBStats.ACCURACY, PBStats.EVASION, PBStats.DEFENSE, PBStats.SPDEF};
        for (int stat : stats) {                                                                       // offensive stats first
            int diff = partner.stage(stat) - atk.stage(stat);
            if (diff <= 0) continue;
            if (stat == PBStats.ATTACK && AiCalc.physicalMoveInMoveset(ctx, atk)
                    || stat == PBStats.SPATK && AiCalc.specialMoveInMoveset(ctx, atk)
                    || stat == PBStats.ACCURACY || stat == PBStats.EVASION || stat == PBStats.SPEED
                    || (AiCalc.classStall(cls) && (stat == PBStats.DEFENSE || stat == PBStats.SPDEF))) {
                return AiPositiveHelpers.incStat(ctx, viability, cls, 8, atk, partner, psychUp, stat, diff);   // INCREASE_STAT_VIABILITY(stat, 8, diff)
            }
        }
        return viability;
    }

    private static int skillSwap(AiCtx ctx, int viability, int cls, Battler atk, Battler partner, BattleMove move, String partnerAbility,
                                 boolean partnerProtects, BattleMove partnerMove) {
        String realAtkAbility = atk.ability == null ? "" : atk.ability;                                // the actual abilities
        boolean attackerBad = bad(realAtkAbility);
        boolean partnerBad = bad(partnerAbility);
        switch (move.function()) {
            case "064": case "068": case "063":                                                        // Worry Seed, Gastro Acid, Simple Beam
                if (!partnerProtects && partnerBad) viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                break;
            case "066":                                                                                // Entrainment
                if (!partnerProtects && partnerBad && AiAbilityRatings.of(realAtkAbility) >= 0) viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                break;
            case "067":                                                                                // Skill Swap: give the ability and then switch
                if (!partnerProtects && !attackerBad && partnerBad) viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                break;
            case "065":                                                                                // Role Play
                if (!partnerProtects && attackerBad && !partnerBad) {
                    viability = AiDoublesScore.increaseDamageToScore(ctx, viability, cls, AiDoublesScore.BEST_KO_SCORE, atk, partner);
                }
                break;
            default:
                break;
        }
        return viability;
    }

    private static boolean bad(String ability) {
        return ability.equals("TRUANT") || ability.equals("SLOWSTART") || ability.equals("DEFEATIST");
    }

    private static int ionDeluge(AiCtx ctx, int viability, Battler atk, Battler partner, String partnerAbility, BattleMove partnerMove,
                                 List<Battler> foes) {
        if (!(partnerAbility.equals("VOLTABSORB") || partnerAbility.equals("MOTORDRIVE") || partnerAbility.equals("LIGHTNINGROD"))) return viability;
        if (foes.isEmpty()) return viability;
        Battler foe1 = foes.get(0);                                                                    // :593-594 both foe moves read data->foe1
        BattleMove foeMove = AiMaster.predictionOf(ctx, foe1, partner);
        if (foeMove != null && AiCalc.fx(foeMove).pbCalcType(foeMove, foe1).equals("NORMAL") && !doesProtectionMoveBlockMove(ctx.battle, foe1, partner, foeMove, partnerMove)) {
            return AiPositiveHelpers.incStatus(ctx, viability, AiCalc.fightingStyle(ctx, atk), 2, atk, partner);
        }
        return viability;
    }

    private static int afterYouInstruct(AiCtx ctx, int viability, int cls, Battler atk, Battler partner, BattleMove move, BattleMove partnerMove,
                                        List<Battler> foes) {
        Battle battle = ctx.battle;
        if (AiCalc.named(move, "AFTERYOU")) {
            if (partnerMove == null) return viability;
            boolean notFirst = false;
            for (Battler foe : foes) notFirst |= !AiCalc.wouldHitBefore(battle, partnerMove, partner, null, foe);
            if (notFirst) {
                if (AiCalc.oneOf(partnerMove, "071", "072")) return viability;                          // Counter / Mirror Coat need to go last
                viability = AiDoublesScore.increaseHelpingHand(viability, cls);
            }
        } else if (AiCalc.named(move, "INSTRUCT")) {
            BattleMove instructed = !AiCalc.wouldHitBefore(battle, move, atk, null, partner) ? partnerMove : AiCalc.lastUsedMove(battle, partner);
            if (instructed != null && !instructed.statusMove()) {
                int t = AiCalc.fx(instructed).pbTarget(instructed, partner);
                if (t == PBTargets.AllNearFoes || t == PBTargets.AllNearOthers || t == PBTargets.AllBattlers) {   // multi-target moves
                    viability = AiDoublesScore.increaseHelpingHand(viability, cls);
                } else {
                    viability = AiPositiveHelpers.inc(viability, 1);                                   // at least as a last resort option
                }
            }
        }
        return viability;
    }
}
