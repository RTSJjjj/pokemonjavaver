package pokemon.runtime.battle;

/**
 * The per-effect {@code switch (moveEffect)} of {@code AIScript_Negatives} (ai_negatives.c:667-3244), part 1:
 * Sleep, Absorb, Explosion, Dream Eater, Splash, Teleport, stat raising (:853-1085), stat lowering (:1109-1217)
 * and Haze. CFRU's {@code EFFECT_*} ids are mapped to the Essentials function codes of the movefx classes
 * (each code below is checked against the class list of {@code MoveEffects_*.java}).
 *
 * <p>Cases that are not here yet fall into the default branch, which is {@code AI_STANDARD_DAMAGE}
 * (:3246) exactly like an unlisted effect in C.</p>
 */
final class AiNegativeEffects {

    private AiNegativeEffects() {
    }

    /** What {@link #apply} decided: the new viability, and whether the {@code AI_STANDARD_DAMAGE} check should still run. */
    static final class Result {
        int viability;
        boolean standardDamage;
    }

    private static final String[] ATTACK_UP = {"01C", "02E"};
    private static final String[] DEFENSE_UP = {"01D", "02F", "01E", "038"};
    private static final String[] SPEED_UP = {"01F", "030", "031"};
    private static final String[] SPATK_UP = {"020", "032", "039"};
    private static final String[] SPDEF_UP = {"033"};
    private static final String[] EVASION_UP = {"022", "034", "037"};
    private static final String[] ATK_DOWN = {"042", "04B"};
    private static final String[] DEF_DOWN = {"043", "04C"};
    private static final String[] SPEED_DOWN = {"044", "04D"};
    private static final String[] SPATK_DOWN = {"045", "04E"};
    private static final String[] SPDEF_DOWN = {"046", "04F"};

    private static boolean in(BattleMove move, String[] codes) {
        return AiCalc.oneOf(move, codes);
    }

    static Result apply(AiCtx ctx, Battler atk, Battler def, BattleMove move, int viability) {
        Battle battle = ctx.battle;
        Result r = new Result();
        r.viability = viability;
        String atkAbility = atk.ability == null ? "" : atk.ability;
        String defAbility = atk.hasMoldBreaker() || def.ability == null ? "" : def.ability;
        boolean contrary = "CONTRARY".equals(atkAbility);
        boolean locked = AiCalc.goodAiMoveLocked(ctx, atk);
        boolean tr = AiCalc.trickRoomNotEnding(battle);
        String f = move.function();

        if (f.equals("000")) {                                                                    // EFFECT_HIT
            r.standardDamage = true;
        } else if (f.equals("003")) {                                                             // EFFECT_SLEEP (:760)
            if (AiCalc.noEffect(battle, atk, def, move)) {
                r.viability -= 10;
            } else if (AiCalc.named(move, "DARKVOID") && !atk.isSpecies("DARKRAI")) {
                r.viability -= 10;
            } else if (!AiCalc.canBePutToSleep(battle, def, atk) || AiCalc.blockedBySubstitute(move, atk, def)) {
                r.viability -= 10;                                                                // AI_CHECK_SLEEP (:775)
            }
        } else if (f.equals("0DD")) {                                                             // EFFECT_ABSORB (:782)
            if ("LIQUIDOOZE".equals(defAbility) && !AiCalc.knocksOutXHits(ctx, move, atk, def, 1)) r.viability -= 6;
            r.standardDamage = true;
        } else if (f.equals("160")) {                                                             // Strength Sap (EFFECT_ABSORB, :788)
            if ("CONTRARY".equals(defAbility) || !AiCalc.statCanBeLowered(def, atk, PBStats.ATTACK)) r.viability -= 10;
        } else if (f.equals("0E0")) {                                                             // EFFECT_EXPLOSION (:800, OKAY_WITH_AI_SUICIDE)
            explosion(ctx, atk, def, move, r);
        } else if (f.equals("0DE")) {                                                             // EFFECT_DREAM_EATER (:849)
            if (!"COMATOSE".equals(defAbility) && !def.hasStatus("SLEEP")) r.viability -= 10;
            else r.standardDamage = true;
        } else if (f.equals("001")) {                                                             // EFFECT_SPLASH (:906): needs a Z-Crystal, which this runtime lacks
            r.viability -= 10;
        } else if (f.equals("0EA")) {                                                             // EFFECT_TELEPORT (:911)
            if (ctx.battle.trainerBattle) {
                if (AiUtil.benchAlive(battle, atk) == 0) r.viability -= 10;                       // !HasMonToSwitchTo
            } else if (atk.foe) {
                if (false /* IsTrapped(bankAtk,FALSE) - 登记 */) r.viability -= 10;
            } else if (AiUtil.benchAlive(battle, atk) == 0) {
                r.viability -= 10;
            }
        } else if (in(move, ATTACK_UP)) {                                                         // EFFECT_ATTACK_UP(_2) (:931)
            if (locked) r.viability -= 10;
            else if (contrary || !AiCalc.statCanRise(atk, PBStats.ATTACK) || !AiCalc.physicalMoveInMoveset(ctx, atk)) r.viability -= 10;
        } else if (in(move, DEFENSE_UP)) {                                                        // EFFECT_DEFENSE_UP(_2)/CURL (:952)
            if (locked || contrary || !AiCalc.statCanRise(atk, PBStats.DEFENSE)) r.viability -= 10;
        } else if (in(move, SPEED_UP)) {                                                          // EFFECT_SPEED_UP(_2) (:999)
            if (contrary || !AiCalc.statCanRise(atk, PBStats.SPEED) || locked || tr) r.viability -= 10;
        } else if (in(move, SPATK_UP)) {                                                          // EFFECT_SPECIAL_ATTACK_UP(_2) (:1007)
            if (contrary || !AiCalc.statCanRise(atk, PBStats.SPATK) || locked || !AiCalc.specialMoveInMoveset(ctx, atk)) r.viability -= 10;
        } else if (in(move, SPDEF_UP)) {                                                          // EFFECT_SPECIAL_DEFENSE_UP(_2) (:1015)
            if (locked || contrary || !AiCalc.statCanRise(atk, PBStats.SPDEF)) r.viability -= 10;
        } else if (in(move, EVASION_UP)) {                                                        // EFFECT_EVASION_UP(_2)/MINIMIZE (:1030)
            if (locked) r.viability -= 10;
            else if (f.equals("037")) {                                                           // MOVE_ACUPRESSURE
                if ("CONTRARY".equals(defAbility) || statsMaxed(def)) r.viability -= 10;
            } else if (contrary || !AiCalc.statCanRise(atk, PBStats.EVASION)) {
                r.viability -= 10;
            }
        } else if (f.equals("027") || f.equals("028") || f.equals("15C")) {                       // EFFECT_ATK_SPATK_UP: Work Up, Growth, Gear Up (:1049)
            if (locked) {
                r.viability -= 10;
            } else if (f.equals("15C") && !isPlusMinus(atkAbility)) {                             // MOVE_GEARUP (no partner in singles)
                r.viability -= 10;
            } else if (contrary
                    || ((!AiCalc.statCanRise(atk, PBStats.ATTACK) || !AiCalc.physicalMoveInMoveset(ctx, atk))
                    && (!AiCalc.statCanRise(atk, PBStats.SPATK) || !AiCalc.specialMoveInMoveset(ctx, atk)))) {
                r.viability -= 10;
            }
        } else if (f.equals("13E")) {                                                             // MOVE_ROTOTILLER (:1057)
            if (!atk.hasType("GRASS") || atk.airborne()) r.viability -= 10;
            else if (locked || contrary || ((!AiCalc.statCanRise(atk, PBStats.ATTACK) || !AiCalc.physicalMoveInMoveset(ctx, atk))
                    && (!AiCalc.statCanRise(atk, PBStats.SPATK) || !AiCalc.specialMoveInMoveset(ctx, atk)))) r.viability -= 10;
        } else if (f.equals("029")) {                                                             // EFFECT_ATK_ACC_UP: Hone Claws (:1082)
            if (contrary || locked) {
                r.viability -= 10;
            } else if (!AiCalc.statCanRise(atk, PBStats.ATTACK) || !AiCalc.physicalMoveInMoveset(ctx, atk)) {
                if (!AiCalc.statCanRise(atk, PBStats.ACCURACY)) r.viability -= 10;
                else if (ctx.goodAi() && allMovesAlwaysHit(ctx, atk, def)) r.viability -= 1;      // ACC_CHECK_2 (:1010)
            }
        } else if (f.equals("02A") || f.equals("137")) {                                          // EFFECT_COSMIC_POWER; Magnetic Flux (:1096)
            if (contrary || locked) r.viability -= 10;
            else if (f.equals("137") && !isPlusMinus(atkAbility)) r.viability -= 10;
            else if (!AiCalc.statCanRise(atk, PBStats.DEFENSE) && !AiCalc.statCanRise(atk, PBStats.SPDEF)) r.viability -= 10;
        } else if (f.equals("024") || f.equals("025")) {                                          // EFFECT_BULK_UP; Coil (:1115)
            if (contrary || locked) {
                r.viability -= 10;
            } else if (f.equals("025")) {
                if (!AiCalc.statCanRise(atk, PBStats.ACCURACY)
                        && (!AiCalc.statCanRise(atk, PBStats.ATTACK) || !AiCalc.physicalMoveInMoveset(ctx, atk))
                        && !AiCalc.statCanRise(atk, PBStats.DEFENSE)) r.viability -= 10;
            } else if ((!AiCalc.statCanRise(atk, PBStats.ATTACK) || !AiCalc.physicalMoveInMoveset(ctx, atk))
                    && !AiCalc.statCanRise(atk, PBStats.DEFENSE)) {
                r.viability -= 10;
            }
        } else if (f.equals("02C") || f.equals("02B") || f.equals("14E")) {                       // EFFECT_CALM_MIND; Quiver Dance, Geomancy (:1139)
            if (contrary || locked) {
                r.viability -= 10;
            } else if (!f.equals("02C")) {
                if (!AiCalc.statCanRise(atk, PBStats.SPEED)
                        && (!AiCalc.statCanRise(atk, PBStats.SPATK) || !AiCalc.specialMoveInMoveset(ctx, atk))
                        && !AiCalc.statCanRise(atk, PBStats.SPDEF)) r.viability -= 10;
                else if (tr) r.viability -= 10;
            } else if ((!AiCalc.statCanRise(atk, PBStats.SPATK) || !AiCalc.specialMoveInMoveset(ctx, atk))
                    && !AiCalc.statCanRise(atk, PBStats.SPDEF)) {
                r.viability -= 10;                                                                // (:1166 Take Heart exception: no such move here)
            }
        } else if (f.equals("026") || f.equals("035") || f.equals("036")) {                       // EFFECT_DRAGON_DANCE; Shell Smash, Shift Gear (:1173)
            if (contrary || locked) {
                r.viability -= 10;
            } else if (f.equals("035")) {
                if ((!AiCalc.statCanRise(atk, PBStats.SPATK) || !AiCalc.specialMoveInMoveset(ctx, atk))
                        && (!AiCalc.statCanRise(atk, PBStats.ATTACK) || !AiCalc.physicalMoveInMoveset(ctx, atk))
                        && !AiCalc.statCanRise(atk, PBStats.SPEED)) r.viability -= 10;
                else if (tr) r.viability -= 10;
            } else if (tr) {
                r.viability -= 10;
            } else if ((!AiCalc.statCanRise(atk, PBStats.ATTACK) || !AiCalc.physicalMoveInMoveset(ctx, atk))
                    && !AiCalc.statCanRise(atk, PBStats.SPEED)) {
                r.viability -= 10;
            }
        } else if (f.equals("179")) {                                                             // Clangorous Soul (EFFECT_EXTREME_EVOBOOST, :1213)
            if (mainStatsMaxed(atk) || locked || atk.hp <= atk.maxHp() / 3) r.viability -= 10;
        } else if (f.equals("021")) {                                                             // EFFECT_CHARGE (:1228)
            if (atk.effects.intVal(PBEffects.Battler.Charge) > 0 || locked) r.viability -= 10;
            else if (!AiCalc.damagingTypeInMoveset(ctx, atk, "ELECTRIC")) {                       // goto AI_SPDEF_UP
                if (contrary || !AiCalc.statCanRise(atk, PBStats.SPDEF)) r.viability -= 10;
            }
        } else if (in(move, ATK_DOWN)) {                                                          // EFFECT_ATTACK_DOWN(_2) (:1240)
            if (!AiCalc.statCanBeLowered(def, atk, PBStats.ATTACK) || !AiCalc.physicalMoveInMoveset(ctx, def)) r.viability -= 10;
            else substituteCheck(move, atk, def, r);
        } else if (f.equals("04A")) {                                                             // EFFECT_TICKLE (:1248)
            if (!AiCalc.statCanBeLowered(def, atk, PBStats.ATTACK)
                    && (!AiCalc.statCanBeLowered(def, atk, PBStats.DEFENSE) || !AiCalc.physicalMoveInMoveset(ctx, def))) r.viability -= 10;
            else substituteCheck(move, atk, def, r);
        } else if (f.equals("139")) {                                                             // EFFECT_PLAY_NICE (:1256)
            if ((!AiCalc.statCanBeLowered(def, atk, PBStats.SPATK) || !AiCalc.specialMoveInMoveset(ctx, def))
                    && (!AiCalc.statCanBeLowered(def, atk, PBStats.ATTACK) || !AiCalc.physicalMoveInMoveset(ctx, def))) r.viability -= 10;
            else substituteCheck(move, atk, def, r);
        } else if (f.equals("140")) {                                                             // EFFECT_VENOM_DRENCH (:1264)
            if (!(def.hasStatus("POISON"))
                    || (!AiCalc.statCanBeLowered(def, atk, PBStats.SPEED)
                    && (!AiCalc.statCanBeLowered(def, atk, PBStats.ATTACK) || !AiCalc.physicalMoveInMoveset(ctx, def))
                    && (!AiCalc.statCanBeLowered(def, atk, PBStats.SPATK) || !AiCalc.specialMoveInMoveset(ctx, def)))) r.viability -= 10;
            else substituteCheck(move, atk, def, r);
        } else if (in(move, DEF_DOWN)) {                                                          // EFFECT_DEFENSE_DOWN(_2) (:1277)
            if (!AiCalc.statCanBeLowered(def, atk, PBStats.DEFENSE)) r.viability -= 10; else substituteCheck(move, atk, def, r);
        } else if (in(move, SPEED_DOWN)) {                                                        // EFFECT_SPEED_DOWN(_2) (:1284)
            if (!AiCalc.statCanBeLowered(def, atk, PBStats.SPEED)) r.viability -= 10; else substituteCheck(move, atk, def, r);
        } else if (in(move, SPATK_DOWN)) {                                                        // EFFECT_SPECIAL_ATTACK_DOWN(_2) (:1291)
            if (f.equals("04E") /* Captivate */ && (atk.pokemon.gender == pokemon.runtime.pokemon.PokemonStats.GENDERLESS
                    || def.pokemon.gender == pokemon.runtime.pokemon.PokemonStats.GENDERLESS
                    || atk.pokemon.gender == def.pokemon.gender)) r.viability -= 10;
            else if (!AiCalc.statCanBeLowered(def, atk, PBStats.SPATK) || !AiCalc.specialMoveInMoveset(ctx, def)) r.viability -= 10;
            else substituteCheck(move, atk, def, r);
        } else if (in(move, SPDEF_DOWN)) {                                                        // EFFECT_SPECIAL_DEFENSE_DOWN(_2) (:1304)
            if (!AiCalc.statCanBeLowered(def, atk, PBStats.SPDEF)) r.viability -= 10; else substituteCheck(move, atk, def, r);
        } else if (f.equals("047")) {                                                             // EFFECT_ACCURACY_DOWN(_2) (:1311)
            if (!AiCalc.statCanBeLowered(def, atk, PBStats.ACCURACY)) r.viability -= 10; else substituteCheck(move, atk, def, r);
        } else if (f.equals("048")) {                                                             // EFFECT_EVASION_DOWN(_2) (:1318)
            if (!AiCalc.statCanBeLowered(def, atk, PBStats.EVASION)) r.viability -= 10; else substituteCheck(move, atk, def, r);
        } else if (f.equals("051")) {                                                             // EFFECT_HAZE (:1325): GOOD_AI branch needs CountUsefulBoosts/Debuffs - 登记
            // :1325-1339 only runs for AI flags <= SEMI_SMART; :1342 `if (GOOD_AI)` boost counting is 登记 (CountUsefulBoosts).
        } else if (!part2(ctx, atk, def, move, r)) {
            r.standardDamage = true;                                                              // default: AI_STANDARD_DAMAGE (:3246)
        }
        return r;
    }

    /**
     * Part 2 (ai_negatives.c:1397-1790): recovery, Rest, Poison/Toxic, Light Screen/Reflect, OHKO, recoil, Mist, Focus Energy,
     * Confuse, Transform, Paralyze, Substitute, Recharge, Leech Seed, Endeavor, Pain Split, Snore, Sleep Talk, Counter.
     * 登记: Conversion/Reflect Type (:1397-1445), Spite/Mimic/Disable/Encore/Conversion 2 and the Counter "tried the same last turn" branch
     * (:1700-1787) need the last-used-move history, which this runtime does not keep; those effects stay on the default branch.
     *
     * @return true when the function code has a case here (so it does not fall to AI_STANDARD_DAMAGE unless the case sets it)
     */
    private static boolean part2(AiCtx ctx, Battler atk, Battler def, BattleMove move, Result r) {
        Battle battle = ctx.battle;
        String f = move.function();
        String atkAbility = atk.ability == null ? "" : atk.ability;
        String defAbility = atk.hasMoldBreaker() || def.ability == null ? "" : def.ability;
        BattleMove predicted = ctx.prediction(def);
        int hpPct = AiCalc.healthPercent(atk);
        switch (f) {
            case "0D5": case "0D6": case "0D8": case "0D9": {                                     // EFFECT_RESTORE_HP / MORNING_SUN (:1457), EFFECT_REST (:1507)
                if (f.equals("0D9") && !AiCalc.canRest(battle, atk)) r.viability -= 10;           // EFFECT_REST: CanRest, then AI_RECOVERY
                recovery(ctx, atk, def, r);
                return true;
            }
            case "005": case "006": {                                                             // EFFECT_POISON / TOXIC (:1513)
                if (AiCalc.noEffect(battle, atk, def, move)) {
                    r.viability -= 10;
                } else if (!AiCalc.canBePoisoned(battle, def, atk) || AiCalc.blockedBySubstitute(move, atk, def)) {
                    r.viability -= 10;                                                            // AI_POISON_CHECK
                }
                return true;
            }
            case "0A3": {                                                                         // EFFECT_LIGHT_SCREEN (:1530)
                if (atk.pbOwnSide().effects.intVal(PBEffects.Side.LightScreen) > 0) r.viability -= 10;
                return true;                                                                      // 登记: HasUsedMoveWithEffect(Brick Break/Defog) history
            }
            case "070": {                                                                         // EFFECT_0HKO (:1538)
                if (AiCalc.noEffect(battle, atk, def, move) || (!atk.hasMoldBreaker() && "STURDY".equals(defAbility))
                        || atk.level() < def.level() || (AiCalc.named(move, "SHEERCOLD") && def.hasType("ICE"))) {
                    r.viability -= 10;
                }
                return true;
            }
            case "10B": {                                                                         // EFFECT_RECOIL_IF_MISS (:1549)
                if (!"MAGICGUARD".equals(atkAbility) && AiCalc.hitChance(battle, atk, def, move) < 75) r.viability -= 6;
                r.standardDamage = true;
                return true;
            }
            case "056": {                                                                         // EFFECT_MIST (:1556)
                if (atk.pbOwnSide().effects.intVal(PBEffects.Side.Mist) > 0) r.viability -= 10;
                return true;
            }
            case "023": {                                                                         // EFFECT_FOCUS_ENERGY (:1562)
                if (atk.effects.intVal(PBEffects.Battler.FocusEnergy) > 0) r.viability -= 10;
                return true;
            }
            case "0FA": case "0FB": case "0FC": case "0FD": case "0FE": case "170": {            // EFFECT_RECOIL (:1568); Mind Blown is half max HP
                if ("MAGICGUARD".equals(atkAbility) || "ROCKHEAD".equals(atkAbility)) {
                    r.standardDamage = true;
                    return true;
                }
                int recoil = AiCalc.finalDamage(ctx, move, atk, def, 1);
                if (f.equals("0FA")) recoil = Math.max(1, recoil / 4);
                else if (f.equals("0FB") || f.equals("0FD") || f.equals("0FE")) recoil = Math.max(1, recoil / 3);
                else if (f.equals("0FC")) recoil = Math.max(1, recoil / 2);
                else {
                    if (AiCalc.blockedBySubstitute(move, atk, def)) {
                        r.viability -= 9;
                        r.standardDamage = true;
                        return true;
                    }
                    recoil = Math.max(1, atk.maxHp() / 2);
                }
                if (recoil >= atk.hp && AiUtil.benchAlive(battle, def) + 1 > 1) {                  // Recoil kills attacker; foe has more than 1 target left
                    if (!AiCalc.knocksOutXHits(ctx, move, atk, def, 1) || canKnockOutWithoutMove(ctx, move, atk, def)) r.viability -= 4;
                }
                r.standardDamage = true;
                return true;
            }
            case "013": {                                                                         // EFFECT_CONFUSE (:1604)
                if (!AiCalc.canBeConfused(battle, def, atk) || AiCalc.blockedBySubstitute(move, atk, def)) r.viability -= 10;
                return true;
            }
            case "069": {                                                                         // EFFECT_TRANSFORM (:1616)
                if (atk.effects.truthy(PBEffects.Battler.Transform)
                        || def.effects.truthy(PBEffects.Battler.Transform) || def.effects.intVal(PBEffects.Battler.Substitute) > 0) r.viability -= 10;
                return true;
            }
            case "0A2": {                                                                         // EFFECT_REFLECT default (:1634)
                if (atk.pbOwnSide().effects.intVal(PBEffects.Side.Reflect) > 0) r.viability -= 10;
                return true;                                                                      // 登记: HasUsedMoveWithEffect(Brick Break/Defog) history
            }
            case "167": {                                                                         // MOVE_AURORAVEIL (:1625)
                if (atk.pbOwnSide().effects.intVal(PBEffects.Side.AuroraVeil) > 0
                        || (battle.pbWeather() != PBWeather.Hail && battle.pbWeather() != PBWeather.Snow)) r.viability -= 10;
                return true;
            }
            case "007": {                                                                         // EFFECT_PARALYZE (:1647)
                if (!AiCalc.canBeParalyzed(battle, def, atk) || AiCalc.blockedBySubstitute(move, atk, def)) {
                    r.viability -= 10;
                } else if (!AiCalc.named(move, "GLARE") && AiCalc.noEffect(battle, atk, def, move)) {
                    r.viability -= 10;
                }
                return true;
            }
            case "10C": {                                                                         // EFFECT_SUBSTITUTE (:1662)
                if (atk.effects.intVal(PBEffects.Battler.Substitute) > 0 || AiCalc.healthPercent(atk) <= 25) r.viability -= 10;
                else if (defAbility.equals("INFILTRATOR") || soundMoveInMoveset(ctx, def)) r.viability -= 8;
                return true;
            }
            case "0C2": {                                                                         // EFFECT_RECHARGE (:1674)
                if (!"TRUANT".equals(atkAbility) && AiCalc.knocksOutXHits(ctx, move, atk, def, 1)
                        && canKnockOutWithoutMove(ctx, move, atk, def)) r.viability -= 9;
                r.standardDamage = true;
                return true;
            }
            case "0DC": {                                                                         // EFFECT_LEECH_SEED (:1729)
                if (def.hasType("GRASS") || def.effects.intVal(PBEffects.Battler.LeechSeed) != -1
                        || "LIQUIDOOZE".equals(defAbility)) r.viability -= 10;
                else substituteCheck(move, atk, def, r);
                return true;
            }
            case "06E": {                                                                         // EFFECT_ENDEAVOR (:1786)
                if (atk.hp > (atk.hp + def.hp) / 2) r.viability -= 10;
                else r.standardDamage = true;
                return true;
            }
            case "05A": {                                                                         // EFFECT_PAIN_SPLIT (:1793)
                if (atk.hp > (atk.hp + def.hp) / 2) r.viability -= 10;
                return true;
            }
            case "011": {                                                                         // EFFECT_SNORE (:1798)
                boolean asleep = atk.hasStatus("SLEEP") && atk.statusCount > 1;
                if (!asleep && !"COMATOSE".equals(atkAbility)) r.viability -= 10;
                else r.standardDamage = true;
                return true;
            }
            case "0B4": {                                                                         // EFFECT_SLEEP_TALK (:1806)
                boolean asleep = atk.hasStatus("SLEEP") && atk.statusCount > 1;
                if (!asleep && !"COMATOSE".equals(atkAbility)) r.viability -= 10;
                return true;
            }
            case "071": case "072": case "073": {                                                 // EFFECT_COUNTER / MIRROR_COAT (:1707)
                if (predicted == null || predicted.statusMove() || AiCalc.blockedBySubstitute(predicted, def, atk)) r.viability -= 10;
                if (f.equals("073") && AiCalc.moveWouldHitFirst(ctx, move, atk, def)) r.viability -= 10;   // Metal Burst can go first and fail
                r.standardDamage = true;                                                          // 登记: the previousMovePredictions branch (:1717-1725)
                return true;
            }
            default:
                return part3(ctx, atk, def, move, r);
        }
    }

    /** {@code AI_RECOVERY:} / {@code DEFAULT_RECOVERY:} (:1457-1493), single battle. */
    private static void recovery(AiCtx ctx, Battler atk, Battler def, Result r) {
        int hpPct = AiCalc.healthPercent(atk);
        if (ctx.goodAi() && AiCalc.takingSecondaryDamage(ctx.battle, def)) {                      // very smart AI, IS_SINGLE_BATTLE
            if (hpPct == 100) r.viability -= 1;
            return;
        }
        if (hpPct == 100) r.viability -= 10;
        else if (hpPct >= 90) r.viability -= 9;
    }

    /** {@code ProtectUses}: how many Protects in a row the battler has used (Essentials keeps the rate that divides the odds). */
    private static int protectUses(Battler b) {
        int rate = b.effects.intVal(PBEffects.Battler.ProtectRate);
        return rate <= 1 ? 0 : rate < 4 ? 1 : 2;
    }

    private static boolean protectInMoveset(Battler b) {
        return AiCalc.moveFunctionInMoveset(b, AiCalc.PROTECT);
    }

    private static boolean anyWeather(Battle battle) {
        int w = battle.pbWeather();
        return w != PBWeather.None;
    }

    /**
     * Part 3 (ai_negatives.c:1800-2560): Lock-On, Destiny Bond, False Swipe, Heal Bell, Mean Look, Nightmare, Curse, Protect, hazards,
     * Foresight, Perish Song, weather, Swagger/Flatter, Attract, Safeguard, Burn Up, Baton Pass, Defog, Belly Drum, Future Sight, two-turn
     * attacks, Fake Out, Stockpile, Torment, Will-O-Wisp, Memento family, Focus Punch, Taunt, Follow Me, Trick, Role Play, Wish, Ingrain,
     * Magic Coat, Recycle, Yawn. 登记: Skip because they need data this runtime lacks: Sketch/Spite/Mimic/Disable/Encore (last-used move),
     * Assist, Magic Coat (MagicCoatableMovesInMoveset), Role Play ability tables, HazardClearingMoveInMovesetThatAffects,
     * Evaporate (:2194), Shadow Shield, Z-Crystal/Max moves.
     */
    private static boolean part3(AiCtx ctx, Battler atk, Battler def, BattleMove move, Result r) {
        Battle battle = ctx.battle;
        String f = move.function();
        String atkAbility = atk.ability == null ? "" : atk.ability;
        String defAbility = atk.hasMoldBreaker() || def.ability == null ? "" : def.ability;
        BattleMove predicted = ctx.prediction(def);
        int atkSpeed = AiCalc.speed(atk);
        int defSpeed = AiCalc.speed(def);
        switch (f) {
            case "0A6": case "15E": {                                                             // EFFECT_LOCK_ON (:1802)
                if (f.equals("15E")) {                                                            // MOVE_LASERFOCUS
                    if (atk.effects.intVal(PBEffects.Battler.LaserFocus) > 0) r.viability -= 10;
                    else if ("SHELLARMOR".equals(defAbility) || "BATTLEARMOR".equals(defAbility)) r.viability -= 8;
                } else if (atk.hasActiveAbility("NOGUARD") || def.hasActiveAbility("NOGUARD")
                        || (def.effects.intVal(PBEffects.Battler.LockOn) > 0)) {
                    r.viability -= 10;
                } else {
                    substituteCheck(move, atk, def, r);
                }
                return true;
            }
            case "0E7": {                                                                         // EFFECT_DESTINY_BOND (:1826)
                if (atk.effects.truthy(PBEffects.Battler.DestinyBond)) r.viability -= 10;
                return true;
            }
            case "0E9": {                                                                         // EFFECT_FALSE_SWIPE (:1832)
                if (AiCalc.knocksOutXHits(ctx, move, atk, def, 1) && canKnockOutWithoutMove(ctx, move, atk, def)) r.viability -= 10;
                else r.standardDamage = true;
                return true;
            }
            case "019": {                                                                         // EFFECT_HEAL_BELL (:1840)
                boolean sound = AiCalc.has(move, 'k');
                if (!partyMemberStatused(battle, atk, sound)) r.viability -= 10;
                return true;
            }
            case "0EF": {                                                                         // EFFECT_MEAN_LOOK (:1854)
                if (!move.statusMove()) {
                    r.standardDamage = true;
                } else if (def.effects.intVal(PBEffects.Battler.MeanLook) >= 0 || def.effects.intVal(PBEffects.Battler.Trapping) > 0
                        || def.hasType("GHOST")) {
                    r.viability -= 10;                                                            // IsTrapped(bankDef,TRUE)
                }
                return true;
            }
            case "10F": {                                                                         // EFFECT_NIGHTMARE (:1865)
                if (def.effects.truthy(PBEffects.Battler.Nightmare) || !(def.hasStatus("SLEEP") || "COMATOSE".equals(defAbility))) r.viability -= 10;
                return true;
            }
            case "10D": {                                                                         // EFFECT_CURSE (:1873)
                if (atk.hasType("GHOST")) {
                    if (def.effects.truthy(PBEffects.Battler.Curse)) r.viability -= 10;
                    else if (AiCalc.healthPercent(atk) <= 50) r.viability -= 6;
                } else if ("CONTRARY".equals(atkAbility)) {
                    if (atk.stage(PBStats.ATTACK) <= -6 && atk.stage(PBStats.DEFENSE) <= -6 && !AiCalc.statCanRise(atk, PBStats.SPEED)) r.viability -= 10;
                } else if (!AiCalc.statCanRise(atk, PBStats.ATTACK) && !AiCalc.statCanRise(atk, PBStats.DEFENSE)
                        && atk.stage(PBStats.SPEED) <= -6) {
                    r.viability -= 10;
                }
                return true;
            }
            case "0AA": case "14B": case "14C": case "168": case "0E8": case "0AB": case "0AC": case "14A": case "149": {   // EFFECT_PROTECT (:1904)
                boolean teamProtect = f.equals("0AB") || f.equals("0AC") || f.equals("14A");
                if (teamProtect) { r.viability -= 10; return true; }                              // !IS_DOUBLE_BATTLE
                if (f.equals("149") && !AiCalc.firstTurn(atk)) { r.viability -= 10; return true; }   // MOVE_MATBLOCK
                if (f.equals("0E8") && (atk.hp == 1 || AiCalc.takingSecondaryDamage(battle, atk))) { r.viability -= 10; return true; }   // MOVE_ENDURE
                if (def.effects.intVal(PBEffects.Battler.HyperBeam) > 0) { r.viability -= 10; return true; }   // STATUS2_RECHARGE
                int uses = protectUses(atk);
                if (uses > 0) {                                                                   // the previous move was also a Protect
                    if (AiCalc.willFaintFromSecondaryDamage(battle, atk) && !AiCalc.isMoxie(defAbility)) r.viability -= 10;
                    else if (uses >= 2) r.viability -= 10;
                    else if (f.equals("14B") && uses > 0) r.viability -= 9;                       // King's Shield
                    else if (uses == 1 && (ctx.simulatedRng[1] & 1) != 0) r.viability -= 6;       // IS_SINGLE_BATTLE
                }
                return true;
            }
            case "103": case "104": case "105": case "153": {                                     // EFFECT_SPIKES (:1991)
                if (AiUtil.benchAlive(battle, def) + 1 <= 1) { r.viability -= 10; return true; }
                BattleSide side = def.pbOwnSide();
                if (f.equals("105")) {
                    if (side.effects.intVal(PBEffects.Side.StealthRock) > 0) r.viability -= 10;
                } else if (f.equals("104")) {
                    if (side.effects.intVal(PBEffects.Side.ToxicSpikes) >= 2) r.viability -= 10;
                } else if (f.equals("153")) {
                    if (side.effects.intVal(PBEffects.Side.StickyWeb) > 0) r.viability -= 10;
                } else {
                    if (side.effects.intVal(PBEffects.Side.Spikes) >= 3) r.viability -= 10;
                }
                return true;
            }
            case "0A7": case "0A8": {                                                             // EFFECT_FORESIGHT / Miracle Eye (:2042)
                boolean miracle = f.equals("0A8");
                if (miracle) {
                    if (def.effects.truthy(PBEffects.Battler.MiracleEye)) r.viability -= 10;
                    if (def.stage(PBStats.EVASION) <= -2 || !def.hasType("DARK")) r.viability -= 9;
                } else {
                    if (def.effects.truthy(PBEffects.Battler.Foresight)) r.viability -= 10;
                    else if (def.stage(PBStats.EVASION) <= -2 || !def.hasType("GHOST")) r.viability -= 9;
                }
                return true;
            }
            case "0E5": {                                                                         // EFFECT_PERISH_SONG (:2074), single battle
                if (AiUtil.benchAlive(battle, atk) + 1 == 1 && !"SOUNDPROOF".equals(atkAbility) && AiUtil.benchAlive(battle, def) + 1 >= 2) r.viability -= 10;
                if (def.effects.intVal(PBEffects.Battler.PerishSong) > 0 || def.hasActiveAbility("SOUNDPROOF")) r.viability -= 10;
                return true;
            }
            case "101": case "100": case "102": case "0FF": case "1C0": {                         // EFFECT_SANDSTORM / RAIN_DANCE / HAIL / SUNNY_DAY (:2090,:2192,:2200,:2338)
                int w = battle.pbWeather();
                boolean primal = w == PBWeather.HarshSun || w == PBWeather.HeavyRain || w == PBWeather.StrongWinds;
                boolean same = (f.equals("101") && w == PBWeather.Sandstorm) || (f.equals("100") && w == PBWeather.Rain)
                        || (f.equals("102") && (w == PBWeather.Hail || w == PBWeather.Snow)) || (f.equals("0FF") && w == PBWeather.Sun)
                        || (f.equals("1C0") && (w == PBWeather.Snow || w == PBWeather.Hail));
                if (same || primal) r.viability -= 10;
                return true;
            }
            case "041": case "040": {                                                             // EFFECT_SWAGGER / FLATTER (:2098,:2329)
                if (!AiCalc.canBeConfused(battle, def, atk) || AiCalc.blockedBySubstitute(move, atk, def)) r.viability -= 10;
                return true;
            }
            case "016": {                                                                         // EFFECT_ATTRACT (:2104)
                if (def.effects.intVal(PBEffects.Battler.Attract) >= 0 || def.fainted() || !oppositeGenders(atk, def)
                        || (!atk.hasMoldBreaker() && def.hasActiveAbility(new String[] {"OBLIVIOUS", "AROMAVEIL"}))) r.viability -= 10;
                return true;
            }
            case "01A": {                                                                         // EFFECT_SAFEGUARD (:2114)
                if (atk.pbOwnSide().effects.intVal(PBEffects.Side.Safeguard) > 0) r.viability -= 10;
                return true;
            }
            case "162": {                                                                         // EFFECT_BURN_UP (:2124)
                if (!atk.hasType("FIRE")) r.viability -= 10; else r.standardDamage = true;
                return true;
            }
            case "0EE": {                                                                         // U-turn / Volt Switch (EFFECT_BATON_PASS, :2133)
                r.standardDamage = true;
                return true;
            }
            case "0ED": case "151": {                                                             // Baton Pass / Parting Shot
                if (AiUtil.benchAlive(battle, atk) == 0) { r.viability -= 10; return true; }
                if (f.equals("151")) {
                    if (!AiCalc.statCanBeLowered(def, atk, PBStats.ATTACK) && !AiCalc.statCanBeLowered(def, atk, PBStats.SPATK)) r.viability -= 10;
                } else {
                    boolean passable = atk.effects.intVal(PBEffects.Battler.Substitute) > 0 || atk.effects.truthy(PBEffects.Battler.Ingrain)
                            || atk.effects.truthy(PBEffects.Battler.AquaRing) || anyStatRaised(atk);
                    if (!passable) r.viability -= 6;
                }
                return true;
            }
            case "110": {                                                                         // EFFECT_RAPID_SPIN (:2171): Rapid Spin is a damaging move
                r.standardDamage = true;
                return true;
            }
            case "03A": {                                                                         // EFFECT_BELLY_DRUM (:2230)
                if ("CONTRARY".equals(atkAbility) || AiCalc.healthPercent(atk) <= 50) r.viability -= 10;
                return true;
            }
            case "111": {                                                                         // EFFECT_FUTURE_SIGHT (:2243)
                BattlePosition fsPos = battle.field.positions[def.index];          // gWishFutureKnock.futureSightCounter[bankDef]
                if (fsPos != null && fsPos.effects.intVal(PBEffects.Position.FutureSightCounter) != 0) r.viability -= 10;
                else r.standardDamage = true;
                return true;
            }
            case "0C3": case "0C4": case "0C7": case "0C8": case "0C9": case "0CA": case "0CB": case "0CC": case "0CD": case "0CE": case "14D": {   // two-turn attacks (:2251-2325)
                if (atk.hasActiveItem("POWERHERB")) { r.standardDamage = true; return true; }
                if (f.equals("0C4") && (battle.pbWeather() == PBWeather.Sun || battle.pbWeather() == PBWeather.HarshSun)) {
                    r.standardDamage = true;
                    return true;
                }
                if (!f.equals("0C3") && !f.equals("0C4") && !f.equals("0C7") && !f.equals("0C8") && predicted != null
                        && AiCalc.moveWouldHitFirst(ctx, move, atk, def) && AiCalc.oneOf(predicted, "0C9", "0CA", "0CB", "0CC", "0CD", "0CE", "14D")) {
                    r.viability -= 10;                                                            // don't Fly if the opponent is going to Fly after you
                }
                if (AiCalc.willFaintFromSecondaryDamage(battle, atk)) r.viability -= 10;          // attacker will faint while charging
                if (ctx.goodAi()) {
                    boolean immobilised = def.hasStatus("PARALYSIS") || def.hasStatus("FROZEN")
                            || def.effects.intVal(PBEffects.Battler.Attract) >= 0 || (def.hasStatus("SLEEP") && def.statusCount > 1);
                    if (!immobilised && def.effects.intVal(PBEffects.Battler.Confusion) < 3) {
                        if (protectInMoveset(def) && !AiCalc.willFaintFromSecondaryDamage(battle, def)) {
                            r.viability -= 8;
                        } else if (atkSpeed > defSpeed) {
                            if (AiCalc.canKnockOut(ctx, def, atk) && predicted != null) r.viability -= 4;
                        } else if (AiCalc.can2HKO(ctx, def, atk) && predicted != null) {
                            r.viability -= 8;
                        }
                    }
                }
                r.standardDamage = true;
                return true;
            }
            case "012": {                                                                         // EFFECT_FAKE_OUT (:2328)
                if (!AiCalc.firstTurn(atk)) {
                    r.viability -= 10;
                } else if (AiCalc.named(move, "FAKEOUT") && AiCalc.choiceLocked(atk)
                        && (AiUtil.benchAlive(battle, def) + 1 >= 2 || !AiCalc.knocksOutXHits(ctx, move, atk, def, 1))
                        && AiUtil.benchAlive(battle, atk) == 0) {
                    r.viability -= 10;                                                            // don't lock the attacker into Fake Out
                }
                r.standardDamage = true;
                return true;
            }
            case "112": {                                                                         // EFFECT_STOCKPILE (:2341)
                if (atk.effects.intVal(PBEffects.Battler.Stockpile) >= 3) r.viability -= 10;
                return true;
            }
            case "113": {                                                                         // EFFECT_SPIT_UP (:2346)
                if (atk.effects.intVal(PBEffects.Battler.Stockpile) == 0) r.viability -= 10; else r.standardDamage = true;
                return true;
            }
            case "114": {                                                                         // EFFECT_SWALLOW (:2354)
                if (atk.effects.intVal(PBEffects.Battler.Stockpile) == 0) r.viability -= 10; else recovery(ctx, atk, def, r);
                return true;
            }
            case "0B7": {                                                                         // EFFECT_TORMENT (:2370)
                if (def.effects.truthy(PBEffects.Battler.Torment) || def.fainted()) { r.viability -= 10; return true; }
                substituteCheck(move, atk, def, r);
                return true;
            }
            case "00A": {                                                                         // EFFECT_WILL_O_WISP (:2394)
                if (!AiCalc.canBeBurned(battle, def, atk) || AiCalc.blockedBySubstitute(move, atk, def)) r.viability -= 10;
                return true;
            }
            case "0E1": {                                                                         // Final Gambit (EFFECT_MEMENTO, :2403)
                if (AiUtil.benchAlive(battle, atk) == 0) r.viability -= 10; else r.standardDamage = true;
                return true;
            }
            case "0E2": case "0E3": case "0E4": {                                                 // Memento / Healing Wish / Lunar Dance
                if (AiUtil.benchAlive(battle, atk) == 0) { r.viability -= 10; return true; }
                if (f.equals("0E2")) {
                    if (AiCalc.blockedBySubstitute(move, atk, def)) r.viability -= 10;
                    else if (def.stage(PBStats.ATTACK) <= -6 && def.stage(PBStats.SPATK) <= -6) r.viability -= 10;
                    else substituteCheck(move, atk, def, r);
                }
                return true;
            }
            case "115": case "171": case "172": {                                                 // EFFECT_FOCUS_PUNCH (:2425): Focus Punch, Shell Trap, Beak Blast
                if (f.equals("172")) { r.standardDamage = true; return true; }
                if (f.equals("171")) {
                    if (ctx.goodAi() && (predicted == null || !AiCalc.has(predicted, 'a'))) r.viability -= 10; else r.standardDamage = true;
                    return true;
                }
                if (predicted != null && !AiCalc.blockedBySubstitute(predicted, def, atk) && !predicted.statusMove() && predicted.power() != 0) r.viability -= 10;
                else r.standardDamage = true;
                return true;
            }
            case "0BA": {                                                                         // EFFECT_TAUNT (:2453)
                if (def.effects.intVal(PBEffects.Battler.Taunt) > 0 || "OBLIVIOUS".equals(defAbility)) r.viability -= 10;
                return true;
            }
            case "117": case "09C": {                                                             // EFFECT_FOLLOW_ME / HELPING_HAND (:2460): needs an ally
                r.viability -= 10;
                return true;
            }
            case "0F2": case "0F3": {                                                             // EFFECT_TRICK (:2472): Trick/Switcheroo, Bestow
                if (f.equals("0F3")) {
                    if (atk.item == null || atk.item.isEmpty()) r.viability -= 10; else substituteCheck(move, atk, def, r);
                } else if (java.util.Objects.equals(atk.item, def.item) || "STICKYHOLD".equals(defAbility)) {
                    r.viability -= 10;
                } else {
                    substituteCheck(move, atk, def, r);
                }
                return true;
            }
            case "0D7": {                                                                         // EFFECT_WISH (:2501)
                BattlePosition wishPos = battle.field.positions[atk.index];       // gWishFutureKnock.wishCounter[bankAtk]
                if (wishPos != null && wishPos.effects.intVal(PBEffects.Position.Wish) != 0) r.viability -= 10;
                return true;
            }
            case "0DA": case "0DB": {                                                             // EFFECT_INGRAIN (:2531): Aqua Ring, Ingrain
                if (f.equals("0DA") ? atk.effects.truthy(PBEffects.Battler.AquaRing) : atk.effects.truthy(PBEffects.Battler.Ingrain)) r.viability -= 10;
                return true;
            }
            case "13B": {                                                                         // EFFECT_SUPERPOWER: Hyperspace Fury (:2546)
                if (!atk.isSpecies("HOOPA") || atk.form() != 1) r.viability -= 10; else r.standardDamage = true;
                return true;
            }
            case "004": {                                                                         // EFFECT_YAWN (:2541)
                if (def.effects.intVal(PBEffects.Battler.Yawn) > 0 || def.hasStatus("SLEEP")) r.viability -= 10;
                else if (!AiCalc.canBePutToSleep(battle, def, atk) || AiCalc.blockedBySubstitute(move, atk, def)) r.viability -= 10;
                return true;
            }
            default:
                return part4(ctx, atk, def, move, r);
        }
    }

    /**
     * Part 4 (ai_negatives.c:2560-3244): Refresh/Psycho Shift, Imprison, Mud/Water Sport, the stat swap/split moves, Natural Gift, the terrain
     * moves, the field-effect moves (Trick Room, Magic/Wonder Room, Gravity, Ion Deluge), Embargo/Powder/Telekinesis/Heal Block, the type
     * changers, Topsy-Turvy/Electrify, Fairy Lock/Happy Hour/Celebrate. 登记: Knock Off (:2535-2566, item tables), the Skill Swap family
     * (:2568-2630, ability ban tables), Fling (:2918), Instruct (:3085), Max-move/partner checks, Court Change (:2903, needs ShouldCourtChange).
     */
    private static boolean part4(AiCtx ctx, Battler atk, Battler def, BattleMove move, Result r) {
        Battle battle = ctx.battle;
        String f = move.function();
        String atkAbility = atk.ability == null ? "" : atk.ability;
        String defAbility = atk.hasMoldBreaker() || def.ability == null ? "" : def.ability;
        BattleMove predicted = ctx.prediction(def);
        boolean locked = AiCalc.goodAiMoveLocked(ctx, atk);
        switch (f) {
            case "018": case "01B": {                                                             // EFFECT_REFRESH (:2625): Refresh, Psycho Shift
                if (!(atk.hasStatus("POISON") || atk.hasStatus("BURN") || atk.hasStatus("PARALYSIS"))) { r.viability -= 10; return true; }
                if (f.equals("01B")) {
                    if (atk.hasStatus("POISON")) { if (!AiCalc.canBePoisoned(battle, def, atk) || AiCalc.blockedBySubstitute(move, atk, def)) r.viability -= 10; }
                    else if (atk.hasStatus("BURN")) { if (!AiCalc.canBeBurned(battle, def, atk) || AiCalc.blockedBySubstitute(move, atk, def)) r.viability -= 10; }
                    else if (!AiCalc.canBeParalyzed(battle, def, atk) || AiCalc.blockedBySubstitute(move, atk, def)) r.viability -= 10;
                }
                return true;
            }
            case "0B8": {                                                                         // EFFECT_IMPRISON (:2660)
                if (atk.effects.truthy(PBEffects.Battler.Imprison)) r.viability -= 10;
                return true;
            }
            case "09D": {                                                                         // EFFECT_MUD_SPORT (:2680)
                if (battle.field.effects.intVal(PBEffects.Field.MudSportField) > 0) r.viability -= 10;
                return true;
            }
            case "09E": {                                                                         // EFFECT_WATER_SPORT (:2686)
                if (battle.field.effects.intVal(PBEffects.Field.WaterSportField) > 0) r.viability -= 10;
                return true;
            }
            case "057": {                                                                         // MOVE_POWERTRICK (:2705)
                if (locked) r.viability -= 10;
                else if (atk.pokemon.attack() >= atk.pokemon.defense() || !AiCalc.physicalMoveInMoveset(ctx, atk)) r.viability -= 10;
                return true;
            }
            case "052": {                                                                         // MOVE_POWERSWAP (:2716)
                if (locked || (atk.stage(PBStats.ATTACK) >= def.stage(PBStats.ATTACK) && atk.stage(PBStats.SPATK) >= def.stage(PBStats.SPATK))) r.viability -= 10;
                else substituteCheck(move, atk, def, r);
                return true;
            }
            case "053": {                                                                         // MOVE_GUARDSWAP (:2724)
                if (locked || (atk.stage(PBStats.DEFENSE) >= def.stage(PBStats.DEFENSE) && atk.stage(PBStats.SPDEF) >= def.stage(PBStats.SPDEF))) r.viability -= 10;
                else substituteCheck(move, atk, def, r);
                return true;
            }
            case "161": {                                                                         // MOVE_SPEEDSWAP (:2732)
                if (locked) r.viability -= 10;
                else if (AiCalc.trickRoomNotEnding(battle) ? atk.speed() <= def.speed() : atk.speed() >= def.speed()) r.viability -= 10;
                else substituteCheck(move, atk, def, r);
                return true;
            }
            case "058": {                                                                         // MOVE_POWERSPLIT (:2765)
                if (locked || atk.pokemon.attack() + atk.pokemon.spAtk() >= def.pokemon.attack() + def.pokemon.spAtk()) r.viability -= 10;
                else substituteCheck(move, atk, def, r);
                return true;
            }
            case "059": {                                                                         // MOVE_GUARDSPLIT (:2776)
                if (locked || atk.pokemon.defense() + atk.pokemon.spDef() >= def.pokemon.defense() + def.pokemon.spDef()) r.viability -= 10;
                else substituteCheck(move, atk, def, r);
                return true;
            }
            case "096": {                                                                         // EFFECT_NATURAL_GIFT (:2808)
                if ("KLUTZ".equals(atkAbility) || battle.field.effects.intVal(PBEffects.Field.MagicRoom) > 0
                        || atk.item == null || !atk.item.endsWith("BERRY")) r.viability -= 10;
                else r.standardDamage = true;
                return true;
            }
            case "154": case "155": case "156": case "173": {                                     // EFFECT_SET_TERRAIN (:2818)
                int t = f.equals("154") ? PBBattleTerrains.Electric : f.equals("155") ? PBBattleTerrains.Grassy
                        : f.equals("156") ? PBBattleTerrains.Misty : PBBattleTerrains.Psychic;
                if (battle.terrain() == t) r.viability -= 10;
                return true;
            }
            case "11F": {                                                                         // MOVE_TRICKROOM (:2856)
                boolean slower = atk.speed() < def.speed();
                if (battle.field.effects.intVal(PBEffects.Field.TrickRoom) > 0) { if (slower) r.viability -= 10; }   // keep the Trick Room up
                else if (atk.speed() > def.speed()) r.viability -= 10;                           // keep the Trick Room down
                return true;
            }
            case "0F9": {                                                                         // MOVE_MAGICROOM (:2874)
                if (battle.field.effects.intVal(PBEffects.Field.MagicRoom) > 0) r.viability -= 10;
                return true;
            }
            case "124": {                                                                         // MOVE_WONDERROOM (:2880)
                if (battle.field.effects.intVal(PBEffects.Field.WonderRoom) > 0) r.viability -= 10;
                return true;
            }
            case "118": {                                                                         // MOVE_GRAVITY (:2886)
                boolean active = battle.field.effects.intVal(PBEffects.Field.Gravity) > 0;
                boolean floatsMagnet = atk.effects.intVal(PBEffects.Battler.MagnetRise) > 0;
                if (active && !atk.hasType("FLYING") && !floatsMagnet && !atk.hasActiveItem("AIRBALLOON")) r.viability -= 10;
                else if (!active && floatsMagnet) r.viability -= 10;
                return true;
            }
            case "146": {                                                                         // MOVE_IONDELUGE (:2898)
                if (battle.field.effects.intVal(PBEffects.Field.IonDeluge) > 0) r.viability -= 10;
                return true;
            }
            case "0F8": {                                                                         // MOVE_EMBARGO (:2947)
                if ("KLUTZ".equals(defAbility) || battle.field.effects.intVal(PBEffects.Field.MagicRoom) > 0
                        || def.effects.intVal(PBEffects.Battler.Embargo) > 0) r.viability -= 10;
                else substituteCheck(move, atk, def, r);
                return true;
            }
            case "148": {                                                                         // MOVE_POWDER (:2957)
                if (!AiCalc.damagingTypeInMoveset(ctx, def, "FIRE")) r.viability -= 10; else substituteCheck(move, atk, def, r);
                return true;
            }
            case "11A": {                                                                         // MOVE_TELEKINESIS (:2963)
                if (def.effects.intVal(PBEffects.Battler.Telekinesis) > 0 || def.effects.truthy(PBEffects.Battler.Ingrain)
                        || battle.field.effects.intVal(PBEffects.Field.Gravity) > 0 || def.hasActiveItem("IRONBALL")) r.viability -= 10;
                else substituteCheck(move, atk, def, r);
                return true;
            }
            case "0BB": {                                                                         // EFFECT_ATTACK_BLOCKERS default: Heal Block (:2985)
                if (def.effects.intVal(PBEffects.Battler.HealBlock) > 0) r.viability -= 10; else substituteCheck(move, atk, def, r);
                return true;
            }
            case "061": {                                                                         // MOVE_SOAK (:3000)
                if (def.types().size == 1 && def.hasType("WATER")) r.viability -= 10; else substituteCheck(move, atk, def, r);
                return true;
            }
            case "142": {                                                                         // MOVE_TRICKORTREAT (:3011)
                if (def.hasType("GHOST")) r.viability -= 10; else substituteCheck(move, atk, def, r);
                return true;
            }
            case "143": {                                                                         // MOVE_FORESTSCURSE (:3022)
                if (def.hasType("GRASS")) r.viability -= 10; else substituteCheck(move, atk, def, r);
                return true;
            }
            case "145": {                                                                         // MOVE_ELECTRIFY (:3059)
                if (!AiCalc.moveWouldHitFirst(ctx, move, atk, def)
                        || (predicted != null && "ELECTRIC".equals(AiCalc.fx(predicted).pbCalcType(predicted, def)))) r.viability -= 10;
                else substituteCheck(move, atk, def, r);
                return true;
            }
            case "141": {                                                                         // MOVE_TOPSYTURVY (:3069)
                int pos = 0;
                int neg = 0;
                for (int st = PBStats.ATTACK; st <= PBStats.EVASION; st++) {
                    if (def.stage(st) > 0) pos += def.stage(st);
                    else neg -= def.stage(st);
                }
                if (pos == 0) r.viability -= 10;
                else if (neg < pos) r.viability -= 5;
                else substituteCheck(move, atk, def, r);
                return true;
            }
            case "152": {                                                                         // MOVE_FAIRYLOCK (:3093)
                if (battle.field.effects.intVal(PBEffects.Field.FairyLock) > 0) r.viability -= 10;
                return true;
            }
            case "157": case "134": case "133": {                                                 // Happy Hour, Celebrate, Hold Hands (:3098-3120): all need a Z-Crystal
                if (f.equals("157") && !atk.foe) return true;                                     // a player's Happy Hour on a normal battle is only blocked after the first use (not tracked)
                r.viability -= 10;
                return true;
            }
            default:
                return false;
        }
    }

    private static boolean oppositeGenders(Battler a, Battler b) {
        int ga = a.pokemon.gender;
        int gb = b.pokemon.gender;
        return ga != pokemon.runtime.pokemon.PokemonStats.GENDERLESS && gb != pokemon.runtime.pokemon.PokemonStats.GENDERLESS && ga != gb;
    }

    private static boolean anyStatRaised(Battler b) {
        for (int s = PBStats.ATTACK; s <= PBStats.EVASION; s++) if (b.stage(s) > 0) return true;
        return false;
    }

    /** {@code PartyMemberStatused(bank,checkSoundproof)}: a team member (incl. the battler) has a status that Heal Bell/Aromatherapy cures. */
    private static boolean partyMemberStatused(Battle battle, Battler atk, boolean soundMove) {
        for (Battler b : battle.partyOf(atk.index)) {
            if (b == null || b.removedFromParty || b.fainted() || b.pokemon.egg) continue;
            if (b == atk && soundMove && atk.hasActiveAbility("SOUNDPROOF")) continue;
            if (b.statused()) return true;
        }
        return false;
    }

    /** {@code SoundMoveInMoveset(bank)}. */
    private static boolean soundMoveInMoveset(AiCtx ctx, Battler b) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = b.moveSlot(i);
            if (m != null && AiCalc.has(m, 'k')) return true;
        }
        return false;
    }

    /** {@code AI_SUBSTITUTE_CHECK:} (:1246). */
    private static void substituteCheck(BattleMove move, Battler atk, Battler def, Result r) {
        if (AiCalc.blockedBySubstitute(move, atk, def)) r.viability -= 10;
    }

    /** {@code OKAY_WITH_AI_SUICIDE} branch of EFFECT_EXPLOSION (:802-842), single battle. */
    private static void explosion(AiCtx ctx, Battler atk, Battler def, BattleMove move, Result r) {
        Battle battle = ctx.battle;
        if (!atk.hasMoldBreaker() && anyHasAbility(battle, "DAMP")) {
            r.viability -= 10;
        } else if (def.semiInvulnerable()
                && AiCalc.moveWouldHitFirst(ctx, move, atk, def)) {
            r.viability -= 10;                                                                    // don't explode while the target is semi-invulnerable
        } else if (AiUtil.benchAlive(battle, def) + 1 == 1 && AiCalc.knocksOutXHits(ctx, move, atk, def, 1)) {
            // Good to use move
        } else {                                                                                  // Single Battle (:825)
            if (AiCalc.knocksOutXHits(ctx, move, atk, def, 1)) {
                if (AiUtil.benchAlive(battle, def) + 1 >= 2 && canKnockOutWithoutMove(ctx, move, atk, def)) r.viability -= 4;
            } else {
                r.viability -= 4;
            }
        }
        r.standardDamage = true;                                                                  // goto AI_STANDARD_DAMAGE
    }

    /** {@code CanKnockOutWithoutMove(ignoredMove,bankAtk,bankDef,FALSE)} (ai_util.c:244). */
    static boolean canKnockOutWithoutMove(AiCtx ctx, BattleMove ignored, Battler atk, Battler def) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m == null) break;
            if (m == ignored || !AiCalc.usable(ctx, atk, i)) continue;
            if (AiCalc.knocksOutXHits(ctx, m, atk, def, 1)) return true;
        }
        return false;
    }

    private static boolean anyHasAbility(Battle battle, String ability) {
        for (Battler b : new Battler[] {battle.player(), battle.foe()}) {
            if (b != null && !b.fainted() && b.hasActiveAbility(ability)) return true;
        }
        return false;
    }

    private static boolean isPlusMinus(String ability) {
        return "PLUS".equals(ability) || "MINUS".equals(ability);
    }

    /** {@code StatsMaxed(bank)}: every stat stage at +6. */
    private static boolean statsMaxed(Battler b) {
        for (int s = PBStats.ATTACK; s <= PBStats.SPDEF; s++) if (b.stage(s) < 6) return false;
        return b.stage(PBStats.ACCURACY) >= 6 && b.stage(PBStats.EVASION) >= 6;
    }

    /** {@code MainStatsMaxed(bank)}: Attack, Defense, Speed, Sp. Atk and Sp. Def at +6. */
    private static boolean mainStatsMaxed(Battler b) {
        for (int s = PBStats.ATTACK; s <= PBStats.SPDEF; s++) if (b.stage(s) < 6) return false;
        return true;
    }

    /** {@code !MoveInMovesetWithAccuracyLessThan(bankAtk,bankDef,100,FALSE)}: every usable move hits 100% of the time. */
    private static boolean allMovesAlwaysHit(AiCtx ctx, Battler atk, Battler def) {
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove m = atk.moveSlot(i);
            if (m == null) break;
            if (AiCalc.usable(ctx, atk, i) && AiCalc.hitChance(ctx.battle, atk, def, m) < 100) return false;
        }
        return true;
    }
}
