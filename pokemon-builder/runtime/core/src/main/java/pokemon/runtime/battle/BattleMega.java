package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;


/**
 * The Mega Evolution system, line by line:
 * <ul>
 * <li>Battle_Action_Other:65-127 ({@code pbHasMegaRing?} ... {@code pbRegisteredMegaEvolution?});</li>
 * <li>"Mega evolution" section :364-460 - the refactored {@code pbMegaEvolve} that replaces
 *     Battle_Action_Other:132-170 and the Gengar/Rapidash/Zygarde special cases;</li>
 * <li>"ZA模式" :5-223, :234-271 - the per-side Mega energy, the wrappers around every method above;</li>
 * <li>Battle_Phase_Attack:35-39 and :95-103 - Mega Evolving inside the attack phase.</li>
 * </ul>
 *
 * <p>登记:
 * <ul>
 * <li>{@code $DEBUG && Input.press?(Input::CTRL)} (Battle_Action_Other:93, ZA:58): no debug mode.</li>
 * <li>{@code BattleHandlers.triggerTargetAbilityOnHit(ability,nil,battler,nil,self)} for Illusion
 *     (:138-140): called as written; Illusion's handler needs the user/move it is handed nil.</li>
 * <li>{@code battler.pbUpdate(true)}: stats are read from the Pokemon, so the new form is already in effect.</li>
 * <li>The scene calls ({@code @scene.pbChangePokemon}, {@code pbRefreshOne}, the Mega scene): they
 *     are the round events {@code MEGA_SCENE} / {@code CHANGE_POKEMON} the battle screen plays.</li>
 * <li>AI: {@code pbEnemyShouldMegaEvolve?} (PokeBattle_AI:108-163) with {@code MEGAEVOMETHOD = 2} answers
 *     {@code pbCanMegaEvolve?} (the damage / type checks only ever set "should" to true again), so
 *     {@code Battle.chooseFor} registers it for every AI battler (142_PokeBattle_AI:172).</li>
 * </ul></p>
 */
public final class BattleMega {

    /** Settings:166 {@code MEGA_RINGS}. */
    public static final String[] MEGA_RINGS = {"MEGARING", "MEGABRACELET", "MEGACUFF", "MEGACHARM", "KEYSTONE"};
    /** Settings:322 {@code NO_MEGA_EVOLUTION}. */
    public static final int NO_MEGA_EVOLUTION = 34;

    private BattleMega() {
    }

    private static boolean noMegaSwitch(Battle battle) {
        return battle.gameSwitches != null && battle.gameSwitches.test(NO_MEGA_EVOLUTION);   // $game_switches[NO_MEGA_EVOLUTION]
    }

    private static boolean opposes(int idxBattler) {
        return (idxBattler & 1) != 0;                                                // Battle#opposes?(idxBattler) with idxOther = 0
    }

    // ------------------------------------------------------------------
    // Battle_Action_Other:65-127 and the ZA wrappers
    // ------------------------------------------------------------------

    /** {@code pbHasMegaRing?(idxBattler)} (:65-71). */
    public static boolean pbHasMegaRing(Battle battle, int idxBattler) {
        if (!battle.pbOwnedByPlayer(idxBattler)) return true;                        // :66 Assume AI trainer have a ring
        for (String item : MEGA_RINGS) {                                             // :67
            if (battle.pbs().item(item) != null && battle.bagHas(item)) return true;  // :68
        }
        return false;                                                                // :70
    }

    /** {@code pbGetMegaRingName(idxBattler)} (:73-85). */
    public static String pbGetMegaRingName(Battle battle, int idxBattler) {
        if (battle.pbOwnedByPlayer(idxBattler)) {                                    // :74
            for (String i : MEGA_RINGS) {                                            // :75
                PbsData.Item data = battle.pbs().item(i);
                if (data == null) continue;                                          // :76
                if (battle.bagHas(i)) return data.name;                              // :77
            }
        }
        return "超级环";                                                              // :84
    }

    /** {@code pbCanMegaEvolve?(idxBattler)} as the ZA模式 section leaves it (ZA:33-63 around Battle_Action_Other:87-99). */
    public static boolean pbCanMegaEvolve(Battle battle, int idxBattler) {
        Battler battler = battle.battlerAt(idxBattler);
        if (!battle.zaMode) {                                                        // ZA:36
            return pbCanMegaEvolveBase(battle, idxBattler);
        }
        if (noMegaSwitch(battle)) return false;                                      // ZA:38
        if (battler == null) return false;                                           // ZA:40
        if (!battler.hasMega()) return false;                                        // ZA:41
        if (battler.isMega()) return false;                                          // ZA:42
        int side = battler.idxOwnSide();                                             // ZA:44
        if (battle.zaMegaActive[side]) return false;                                 // ZA:45
        if (battle.zaEnergy[side] < Battle.ZA_MAX_ENERGY) return false;              // ZA:46
        if (!battle.zaMegaRequests[side].isEmpty()) {                                // ZA:48
            for (int reqIdx : battle.zaMegaRequests[side]) {                         // ZA:49
                if (reqIdx == idxBattler) continue;                                  // ZA:50
                return false;                                                        // ZA:51
            }
        }
        int rank = battler.pokemon.battleRank;                                       // ZA:55
        if (battle.wildBattle() && opposes(idxBattler)) return rank > 2;             // ZA:56
        if (battler.effects.intVal(PBEffects.Battler.SkyDrop) >= 0) return false;    // ZA:59
        if (!pbHasMegaRing(battle, idxBattler)) return false;                        // ZA:60
        return true;                                                                 // ZA:61
    }

    /** {@code pbCanMegaEvolve?} (Battle_Action_Other:87-99). */
    private static boolean pbCanMegaEvolveBase(Battle battle, int idxBattler) {
        if (noMegaSwitch(battle)) return false;                                      // :88
        Battler battler = battle.battlerAt(idxBattler);
        if (battler == null || !battler.hasMega()) return false;                     // :89
        boolean isBoss = battler.pokemon.battleRank > 2;                             // :90
        if (battle.wildBattle() && opposes(idxBattler)) return isBoss;               // :91
        if (battler.effects.intVal(PBEffects.Battler.SkyDrop) >= 0) return false;    // :94
        if (!pbHasMegaRing(battle, idxBattler)) return false;                        // :95
        int side = battler.idxOwnSide();                                             // :96
        return battle.megaEvolution[side][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] == -1;                                  // :97-98 (owner)
    }

    /** {@code pbRegisterMegaEvolution(idxBattler)} (:101-105) under the ZA wrapper (ZA:65-79). */
    public static boolean pbRegisterMegaEvolution(Battle battle, int idxBattler) {
        Battler battler = battle.battlerAt(idxBattler);
        if (!battle.zaMode) {                                                        // ZA:68
            int side = battler.idxOwnSide();                                         // :102
            battle.megaEvolution[side][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] = idxBattler;                              // :104
            return true;                                                             // (Ruby returns the assigned index; truthy)
        }
        if (!pbCanMegaEvolve(battle, idxBattler)) return false;                      // ZA:70
        if (battler == null) return false;                                           // ZA:72
        int side = battler.idxOwnSide();                                             // ZA:73
        battle.megaEvolution[side][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] = idxBattler;                                  // ZA:75
        if (!battle.zaMegaRequests[side].contains(idxBattler)) battle.zaMegaRequests[side].add(idxBattler);   // ZA:76
        return true;                                                                 // ZA:77
    }

    /** {@code pbUnregisterMegaEvolution(idxBattler)} (:107-111) under the ZA wrapper (ZA:81-94). */
    public static void pbUnregisterMegaEvolution(Battle battle, int idxBattler) {
        Battler battler = battle.battlerAt(idxBattler);
        if (!battle.zaMode) {                                                        // ZA:84
            int side = battler.idxOwnSide();                                         // :108
            if (battle.megaEvolution[side][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] == idxBattler) battle.megaEvolution[side][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] = -1;   // :110
            return;
        }
        if (battler != null) {                                                       // ZA:87
            int side = battler.idxOwnSide();                                         // ZA:88
            if (battle.megaEvolution[side][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] == idxBattler) battle.megaEvolution[side][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] = -1;   // ZA:90
            battle.zaMegaRequests[side].remove(Integer.valueOf(idxBattler));         // ZA:91
        }
    }

    /** {@code pbToggleRegisteredMegaEvolution(idxBattler)} (:113-121) under the ZA wrapper (ZA:96-110). */
    public static void pbToggleRegisteredMegaEvolution(Battle battle, int idxBattler) {
        Battler battler = battle.battlerAt(idxBattler);
        if (!battle.zaMode) {                                                        // ZA:99
            int side = battler.idxOwnSide();                                         // :114
            if (battle.megaEvolution[side][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] == idxBattler) {                       // :116
                battle.megaEvolution[side][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] = -1;                                  // :117
            } else {
                battle.megaEvolution[side][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] = idxBattler;                          // :119
            }
            return;
        }
        if (battler == null) return;                                                 // ZA:102
        int side = battler.idxOwnSide();                                             // ZA:103
        if (battle.zaMegaRequests[side].contains(idxBattler)) {                      // ZA:104
            pbUnregisterMegaEvolution(battle, idxBattler);                           // ZA:105
        } else {
            pbRegisterMegaEvolution(battle, idxBattler);                             // ZA:107
        }
    }

    /** {@code pbRegisteredMegaEvolution?(idxBattler)} (:123-127) under the ZA wrapper (ZA:112-121). */
    public static boolean pbRegisteredMegaEvolution(Battle battle, int idxBattler) {
        Battler battler = battle.battlerAt(idxBattler);
        if (!battle.zaMode) {                                                        // ZA:115
            return battle.megaEvolution[battler.idxOwnSide()][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] == idxBattler;      // :124-126
        }
        if (battler == null) return false;                                           // ZA:118
        return battle.zaMegaRequests[battler.idxOwnSide()].contains(idxBattler);     // ZA:119
    }

    // ------------------------------------------------------------------
    // "Mega evolution" :371-460 pbMegaEvolve, then the ZA wrapper (ZA:123-145)
    // ------------------------------------------------------------------

    /** {@code pbMegaEvolve(idxBattler)} with the ZA模式 wrapper. */
    public static void pbMegaEvolve(Battle battle, int idxBattler) {
        Battler battler = battle.battlerAt(idxBattler);
        if (battler == null) return;                                                 // ZA:127
        int side = battler.idxOwnSide();                                             // ZA:128
        if (battle.zaMode && battle.zaMegaActive[side]) {                            // ZA:131
            return;                                                                  // ZA:132
        }
        pbMegaEvolveBase(battle, idxBattler);                                        // ZA:135
        if (!battle.zaMode) return;                                                  // ZA:137
        if (battler.pokemon == null) return;                                         // ZA:138
        battle.zaMegaRequests[side].remove(Integer.valueOf(idxBattler));             // ZA:140
        battle.zaMegaActive[side] = true;                                            // ZA:141
        battle.zaMegaPkmn[side] = battler.pokemon;                                   // ZA:142
        battle.display(battler.pbThis() + "的超级进化开始了！");                       // ZA:143
    }

    /** "Mega evolution" :371-460. */
    private static void pbMegaEvolveBase(Battle battle, int idxBattler) {
        Battler battler = battle.battlerAt(idxBattler);                              // :372
        if (battler == null || battler.pokemon == null) return;                      // :373
        if (!battler.hasMega() || battler.isMega()) return;                          // :374
        PbsData pbs = battle.pbs();
        String trainerName = null;                                                   // :376
        if (!battle.wildBattle() || !opposes(idxBattler)) {                          // :377
            trainerName = battle.pbGetOwnerName(idxBattler);                         // :378
        }
        // 打破幻影
        if (battler.hasActiveAbility("ILLUSION")) {                                  // :382
            BattleHandlers.triggerTargetAbilityOnHit(battler.ability, null, battler, null, battle);   // :383
        }
        // Mega 进化信息
        if (trainerName != null) {                                                   // :387
            if (battler.pokemon.megaMessage(pbs) == 1) {                             // :388-389 Rayquaza
                battle.display(trainerName + "衷心的祈愿传达给" + battler.pbThis() + "了！");   // :390
            } else {
                battle.display(battler.pbThis() + "的" + battler.itemName() + "与\n" + trainerName + "的"
                        + pbGetMegaRingName(battle, idxBattler) + "发生了反应！");     // :392-393
            }
        } else {
            battle.display(battler.pbThis() + "的力量暴走了！");                       // :396
        }
        // ───────── 根据设置选择完整 / 简洁动画 ─────────
        int oldForm = battler.form();
        if (battle.fullMegaAnimation) {                                              // :400 za_full_mega_animation?
            // 完整版：使用 megascene (SceneMegaEvolution / ScenePrimalblue / ScenePrimalred)
            int sceneKind = battler.isSpecies("KYOGRE") ? 2 : battler.isSpecies("GROUDON") ? 1 : 0;   // :402-408
            battler.pokemon.makeMega(pbs);                                           // :412
            battle.roundEvents.add(Battle.RoundEvent.megaScene(idxBattler, sceneKind, oldForm, battler.form()));   // :410,:415-417
        } else {
            // 简洁版：压缩版动画
            battle.commonAnimation("MegaEvolution", battler);                        // :420
            // pbWait(30)                                                            // :421
            battler.pokemon.makeMega(pbs);                                           // :423
            battle.roundEvents.add(Battle.RoundEvent.changePokemon(idxBattler, oldForm));   // :426-427 pbChangePokemon + pbRefreshOne
            // pbWait(20)                                                            // :428
            battle.commonAnimation("MegaEvolution2", battler);                       // :430
            // pbWait(30)                                                            // :431
        }
        String megaName = battler.pokemon.megaName();                                // :435
        if (megaName == null || megaName.isEmpty()) {                                // :436
            megaName = "超级" + battler.pokemon.species.name;                         // :437
        }
        battle.display(battler.pbThis() + "超级进化为\n" + megaName + "！");            // :439
        int side = battler.idxOwnSide();                                             // :441
        battle.megaEvolution[side][battle.pbGetOwnerIndexFromBattlerIndex(idxBattler)] = -2;                                          // :443
        // 特殊效果
        if (battler.isSpecies("GENGAR") && battler.isMega()) {                       // :446
            battler.effects.set(PBEffects.Battler.Telekinesis, 0);                   // :447
        }
        if (battler.isSpecies("RAPIDASH") && battler.isMega()) {                     // :450
            replaceMove(battle, battler, "FLAREBLITZ", "FLAMEEXPLOSION", "爆焰角袭");   // :451
        }
        if (battler.isSpecies("ZYGARDE") && battler.isMega()) {                      // :454
            replaceMove(battle, battler, "COREENFORCER", "NIHILLIGHT", "归无之光");     // :455
        }
        if (Battle.NEWEST_BATTLE_MECHANICS) {                                        // :458
            battle.pbCalculatePriority(false, new int[]{idxBattler});
        }
        battler.pbEffectsOnSwitchIn(false);                                          // :459
    }

    /** {@code replace_move(battler, old_move, new_move, display_name)} (:510-521). */
    private static void replaceMove(Battle battle, Battler battler, String oldMove, String newMove, String displayName) {
        PbsData pbs = battle.pbs();
        for (int i = 0; i < battler.pokemon.moves.size; i++) {                       // :511
            Pokemon.MoveSlot slot = battler.pokemon.moves.get(i);
            if (slot == null || slot.move == null) continue;                         // :512
            if (oldMove.equals(slot.move.internalName)) {                            // :513
                PbsData.Move replacement = pbs.move(newMove);
                if (replacement == null) continue;                                   // (no such move in the data)
                String oldName = slot.move.name;
                slot.move = replacement;                                             // :514 pokemon.moves[i].id = ...
                if (slot.pp > 0) slot.pp -= 1;                                       // :515
                // :516 battler.moves[i] = PokeBattle_Move.pbFromPBMove(...): the battler's move cache follows slot.move
                battle.displayPaused(battler.pbThis() + "的" + oldName + "变成了" + displayName + "！");   // :517
            }
        }
    }

    // ------------------------------------------------------------------
    // ZA模式 :147-222, :234-271
    // ------------------------------------------------------------------

    /** {@code zaForceExitMega(side)} (ZA:147-158). */
    public static void zaForceExitMega(Battle battle, int side) {
        Pokemon pkmn = battle.zaMegaPkmn[side];                                      // ZA:150
        if (pkmn != null && pkmn.isMega()) {                                         // ZA:151
            pkmn.makeUnmega(battle.pbs());                                           // ZA:152
        }
        battle.zaEnergy[side] = 0;                                                   // ZA:154
        battle.zaMegaActive[side] = false;                                           // ZA:155
        battle.zaMegaPkmn[side] = null;                                              // ZA:156
    }

    /** {@code pbZARevertMegaByPkmn(pkmn)} (ZA:168-205). */
    public static void pbZARevertMegaByPkmn(Battle battle, Pokemon pkmn) {
        if (pkmn == null) return;                                                    // ZA:170
        int side = -1;                                                               // ZA:173
        for (int i = 0; i < battle.zaMegaPkmn.length; i++) {                         // ZA:174
            if (battle.zaMegaPkmn[i] == pkmn) {                                      // ZA:175
                side = i;                                                            // ZA:176
                break;                                                               // ZA:177
            }
        }
        if (side >= 0) {                                                             // ZA:181
            battle.zaMegaPkmn[side] = null;                                          // ZA:182
            battle.zaMegaActive[side] = false;                                       // ZA:183
        }
        if (!pkmn.isMega()) return;                                                  // ZA:186
        Battler onField = null;                                                      // ZA:189
        for (int i = 0; i <= battle.maxBattlerIndex(); i++) {                        // ZA:190
            Battler b = battle.battlerAt(i);
            if (b == null) continue;                                                 // ZA:191
            if (b.pokemon != null && b.pokemon == pkmn) {                            // ZA:192
                onField = b;                                                         // ZA:193
                break;                                                               // ZA:194
            }
        }
        // Battler_ChangeSelf:185 oldDmg = @totalhp-@hp is taken from the battler's Mega-form totalhp, so it is
        // read before the Pokemon's form changes (the plugin's battler keeps its old @form until :201).
        int oldDmg = onField == null ? 0 : onField.maxHp() - onField.hp;
        int oldForm = onField == null ? 0 : onField.form();
        pkmn.makeUnmega(battle.pbs());                                               // ZA:187
        if (onField != null) {                                                       // ZA:198
            // ZA:200-202 pbChangeFormTransform(newForm,...): the form was already applied by makeUnmega
            onField.pbChangeFormTransformApplied(oldDmg, oldForm, onField.pbThis() + "的超级进化结束了，变回了原来的样子！");
        }
    }

    /** {@code pbZARevertMega(idxBattler)} (ZA:160-166). */
    public static void pbZARevertMega(Battle battle, int idxBattler) {
        Battler battler = battle.battlerAt(idxBattler);
        if (battler == null) return;                                                 // ZA:163
        pbZARevertMegaByPkmn(battle, battler.pokemon);                               // ZA:164
    }

    /** The energy step of {@code pbEndOfRoundPhase} (ZA:234-271). */
    public static void pbEndOfRoundZa(Battle battle) {
        if (!battle.zaMode) return;                                                  // ZA:238
        for (int side = 0; side < 2; side++) {                                       // ZA:241
            if (battle.zaMegaActive[side]) {                                         // ZA:242
                Pokemon pkmn = battle.zaMegaPkmn[side];                              // ZA:243
                if (pkmn == null) continue;                                          // ZA:244
                boolean onField = false;                                             // ZA:246
                for (int i = 0; i <= battle.maxBattlerIndex(); i++) {
                    Battler b = battle.battlerAt(i);
                    if (b != null && !b.fainted() && b.pokemon == pkmn) onField = true;
                }
                if (!onField) {                                                      // ZA:250
                    zaForceExitMega(battle, side);                                   // ZA:251
                    if (side == 0) battle.display("超级能量归零！");                    // ZA:252
                    continue;                                                        // ZA:253
                }
                battle.zaEnergy[side] -= 1;                                          // ZA:256
                if (battle.zaEnergy[side] <= 0) {                                    // ZA:257
                    battle.zaEnergy[side] = 0;                                       // ZA:258
                    pbZARevertMegaByPkmn(battle, pkmn);                              // ZA:259
                } else if (side == 0) {                                              // ZA:261
                    battle.display("超级能量：" + battle.zaEnergy[side] + "/" + Battle.ZA_MAX_ENERGY);
                }
            } else if (battle.zaEnergy[side] < Battle.ZA_MAX_ENERGY) {               // ZA:264
                battle.zaEnergy[side] += 1;                                          // ZA:265
                if (side == 0) battle.display("超级能量：" + battle.zaEnergy[side] + "/" + Battle.ZA_MAX_ENERGY);   // ZA:266
            }
        }
    }

    /** {@code pbEndOfBattle} wrapper (ZA:207-222): reverts the Mega Evolutions and clears the energy. */
    public static void pbEndOfBattle(Battle battle) {
        if (battle.zaMode) {                                                         // ZA:211
            for (int side = 0; side < 2; side++) {                                   // ZA:212
                if (battle.zaMegaPkmn[side] != null) {                               // ZA:213
                    pbZARevertMegaByPkmn(battle, battle.zaMegaPkmn[side]);           // ZA:214
                }
            }
        }
        battle.zaEnergy[0] = 0;                                                      // ZA:218
        battle.zaEnergy[1] = 0;
        battle.zaMegaActive[0] = false;                                              // ZA:219
        battle.zaMegaActive[1] = false;
        battle.zaMegaPkmn[0] = null;                                                 // ZA:220
        battle.zaMegaPkmn[1] = null;
    }

    /** {@code pbCommandPhase} wrapper (ZA:225-232): the ZA requests are cleared at the start of each command phase. */
    public static void pbCommandPhaseZa(Battle battle) {
        if (battle.zaMode) {                                                         // ZA:229
            battle.zaMegaRequests[0].clear();
            battle.zaMegaRequests[1].clear();
        }
    }

    // ------------------------------------------------------------------
    // Battle_Phase_Attack:95-103
    // ------------------------------------------------------------------

    /** {@code pbAttackPhaseMegaEvolution} (:95-103). */
    public static void pbAttackPhaseMegaEvolution(Battle battle) {
        for (Battler b : battle.pbPriority(false)) {                                 // :96
            if (battle.wildBattle() && b.opposes(0) && b.pokemon.battleRank <= 2) continue;   // :97
            if (!(":UseMove".equals(battle.choices(b.index)[0]) && !b.fainted())) continue;  // :98
            if (battle.megaEvolution[b.idxOwnSide()][battle.pbGetOwnerIndexFromBattlerIndex(b.index)] != b.index) continue;        // :99-100 (owner)
            pbMegaEvolve(battle, b.index);                                           // :101
        }
    }

    /** The Mega Evolution inside {@code pbPursuit} (Battle_Phase_Attack:35-39). */
    public static void pbPursuitMegaEvolve(Battle battle, Battler b) {
        if (!battle.wildBattle() || !b.opposes(0)) {                                 // :36
            if (battle.megaEvolution[b.idxOwnSide()][battle.pbGetOwnerIndexFromBattlerIndex(b.index)] == b.index) {                // :38
                pbMegaEvolve(battle, b.index);
            }
        }
    }
}
