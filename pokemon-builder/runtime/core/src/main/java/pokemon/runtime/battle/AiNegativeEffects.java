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
                if (ctx.goodAi() && AiCalc.takingSecondaryDamage(battle, def)) {                  // DEFAULT_RECOVERY, very smart AI, single battle
                    if (hpPct == 100) r.viability -= 1;
                    return true;
                }
                if (hpPct == 100) r.viability -= 10;
                else if (hpPct >= 90) r.viability -= 9;
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
                return false;
        }
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
