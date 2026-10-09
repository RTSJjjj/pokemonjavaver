package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;

/**
 * Battle_Action_Switching:122-332 ({@code pbPartyScreen}, {@code pbSwitchInBetween}, {@code pbEORSwitch},
 * {@code pbGetReplacementPokemonIndex}, {@code pbRecallAndReplace}, {@code pbMessageOnRecall},
 * {@code pbMessagesOnReplace}, {@code pbReplace}, {@code pbSendOut}) and Battle_Action_Running:36-157
 * ({@code pbRun}), transcribed line by line.
 *
 * <p>Every {@code @scene.*} call goes through {@link Battle#scene}: the battle screen plays it (the engine
 * waits for it, see {@link EngineCoroutine}); a headless battle answers it itself.</p>
 *
 * <p>登记:
 * <ul>
 * <li>:315-316 {@code partyOrder} swap: this runtime keeps no party order, so {@code pbLastInTeam} is the last
 *     able Pokemon of the party.</li>
 * <li>:325 {@code @peer.pbOnEnteringBattle}, :328 {@code @scene.pbResetMoveIndex}, :330
 *     {@code @usedInBattle}: the peer, the scene's move-cursor memory and the participants list are not modelled.</li>
 * <li>:47-50, :68-73 Battle_Action_Running {@code $DEBUG && Input.press?(Input::CTRL)}: debug-only.</li>
 * <li>Battle_Action_Running:60, :69, :80, :90, :99, :150 {@code pbSEPlay("Battle flee")}: the engine has no
 *     audio; the screen plays the escape sound when the battle ends in an escape.</li>
 * </ul></p>
 */
public final class BattleSwitchAction {

    private static final String NL = String.valueOf((char) 10);

    private BattleSwitchAction() {
    }

    /** {@code opposes?(idxBattler,idxBattler2=0)} (PokeBattle_Battle). */
    private static boolean opposes(int idxBattler) {
        return (idxBattler & 1) != 0;
    }

    /** {@code pbPartyScreen(idxBattler,checkLaxOnly=false,canCancel=false,shouldRegister=false)} (:136-151). */
    public static int pbPartyScreen(Battle battle, int idxBattler, boolean checkLaxOnly, boolean canCancel,
            boolean shouldRegister) {
        int[] ret = {-1};                                                              // :137
        battle.scene.pbPartyScreen(idxBattler, canCancel, idxParty -> {                // :138
            String refusal = checkLaxOnly
                    ? battle.canSwitchLax(idxBattler, idxParty)                        // :140
                    : battle.canSwitch(idxBattler, idxParty);                          // :142
            if (refusal != null) {
                return refusal;                                                        // next false
            }
            if (shouldRegister) {                                                      // :144
                if (idxParty < 0 || !battle.registerSwitch(idxBattler, idxParty)) {    // :145
                    return "";                                                         // next false
                }
            }
            ret[0] = idxParty;                                                         // :147
            return null;                                                               // :148 next true
        });
        return ret[0];                                                                 // :150
    }

    /** {@code pbSwitchInBetween(idxBattler,checkLaxOnly=false,canCancel=false)} (:155-158). */
    public static int pbSwitchInBetween(Battle battle, int idxBattler, boolean checkLaxOnly, boolean canCancel) {
        if (battle.pbOwnedByPlayer(idxBattler)) {                                      // :156
            return pbPartyScreen(battle, idxBattler, checkLaxOnly, canCancel, false);
        }
        return battle.defaultChooseNewEnemy(idxBattler);                               // :157 @battleAI.pbDefaultChooseNewEnemy
    }

    /** {@code pbEORSwitch(favorDraws=false)} (:165-239). */
    public static void pbEORSwitch(Battle battle, boolean favorDraws) {
        if (battle.decision > 0 && !favorDraws) {                                      // :166
            return;
        }
        if (battle.decision == 5 && favorDraws) {                                      // :167
            return;
        }
        battle.pbJudge();                                                              // :168
        if (battle.decision > 0) {                                                     // :169
            return;
        }
        // Check through each fainted battler to see if that spot can be filled.
        List<Integer> switched = new ArrayList<>();                                    // :171
        while (true) {                                                                 // :172
            switched.clear();                                                          // :173
            for (int idxBattler = 0; idxBattler <= battle.maxBattlerIndex(); idxBattler++) {   // :174 @battlers.each
                Battler b = battle.battlerAt(idxBattler);
                if (b == null || !b.fainted()) {                                       // :175
                    continue;
                }
                if (!battle.pbCanChooseNonActive(idxBattler)) {                        // :177
                    continue;
                }
                if (!battle.pbOwnedByPlayer(idxBattler)) {                             // :178 Opponent/ally is switching in
                    if (battle.wildBattle() && opposes(idxBattler)) {                  // :179 Wild Pokemon can't switch
                        continue;
                    }
                    int idxPartyNew = pbSwitchInBetween(battle, idxBattler, false, false);   // :180
                    String opponentFullname = battle.pbGetOwnerName(idxBattler);       // :181 opponent.fullname
                    // NOTE: The player is only offered the chance to switch their own Pokemon when an
                    //       opponent replaces a fainted Pokemon in single battles.
                    Battler first = battle.battlerAt(0);
                    if (battle.internalBattle && battle.switchStyle && battle.trainerBattle      // :185
                            && battle.pbSideSize(0) == 1 && opposes(idxBattler)
                            && first != null && !first.fainted() && battle.pbCanChooseNonActive(0)   // :186
                            && first.effects.intVal(PBEffects.Battler.Outrage) == 0) {              // :187
                        int idxPartyForName = idxPartyNew;                             // :188
                        Array<Battler> enemyParty = battle.partyOf(idxBattler);        // :189
                        if ("ILLUSION".equals(enemyParty.get(idxPartyNew).ability)) {  // :190
                            idxPartyForName = pbLastInTeam(battle, idxBattler);        // :191
                        }
                        if (battle.pbDisplayConfirm(opponentFullname + "将要派出"
                                + enemyParty.get(idxPartyForName).name() + "。" + NL + "要更换宝可梦吗？")) {   // :193-194
                            int idxPlayerPartyNew = pbSwitchInBetween(battle, 0, false, true);   // :195
                            if (idxPlayerPartyNew >= 0) {                              // :196
                                pbMessageOnRecall(battle, battle.battlerAt(0));        // :197
                                pbRecallAndReplace(battle, 0, idxPlayerPartyNew, false, false);   // :198
                                switched.add(0);                                       // :199
                            }
                        }
                    }
                    pbRecallAndReplace(battle, idxBattler, idxPartyNew, false, false); // :203
                    switched.add(idxBattler);                                          // :204
                } else if (battle.trainerBattle) {                                     // :205 Player switches in in a trainer battle
                    int idxPlayerPartyNew = pbGetReplacementPokemonIndex(battle, idxBattler, false);   // :206 Owner chooses
                    pbRecallAndReplace(battle, idxBattler, idxPlayerPartyNew, false, false);   // :207
                    switched.add(idxBattler);                                          // :208
                } else {                                                               // :209 Player's Pokemon has fainted in a wild battle
                    boolean bossBattle = false;                                        // :210
                    for (Battler t : battle.eachOtherSideBattler(idxBattler)) {        // :211 eachOpposing
                        if (t.pokemon.battleRank > 1) {                                // :212
                            bossBattle = true;                                         // :213
                            break;                                                     // :214
                        }
                    }
                    boolean doSwitch = false;                                          // :217
                    if (bossBattle) {                                                  // :218
                        doSwitch = true;                                               // :219
                    } else {
                        if (!battle.pbDisplayConfirm("要更换宝可梦吗？")) {              // :221
                            doSwitch = pbRun(battle, idxBattler, true) <= 0;           // :222
                        } else {
                            doSwitch = true;                                           // :224
                        }
                    }
                    if (doSwitch) {                                                    // :227
                        int idxPlayerPartyNew = pbGetReplacementPokemonIndex(battle, idxBattler, false);   // :228 Owner chooses
                        pbRecallAndReplace(battle, idxBattler, idxPlayerPartyNew, false, false);   // :229
                        switched.add(idxBattler);                                      // :230
                    }
                }
            }
            if (switched.isEmpty()) {                                                  // :234
                break;
            }
            for (Battler b : battle.pbPriority(true)) {                                // :235
                if (switched.contains(b.index)) {
                    b.pbEffectsOnSwitchIn(true);                                       // :236
                }
            }
        }
    }

    /** {@code pbGetReplacementPokemonIndex(idxBattler,random=false)} (:241-253). */
    public static int pbGetReplacementPokemonIndex(Battle battle, int idxBattler, boolean random) {
        if (random) {                                                                  // :242
            if (!battle.pbCanSwitch(idxBattler)) {                                     // :243 Can battler switch out?
                return -1;
            }
            List<Integer> choices = new ArrayList<>();                                 // :244 Find all Pokemon that can switch in
            Array<Battler> party = battle.partyOf(idxBattler);
            int[] team = battle.pbTeamIndexRangeFromBattlerIndex(idxBattler);
            for (int i = team[0]; i < team[1]; i++) {                                  // :245 eachInTeamFromBattlerIndex
                if (party.get(i) == null) {
                    continue;
                }
                if (battle.canSwitchLax(idxBattler, i) == null) {                      // :246
                    choices.add(i);
                }
            }
            if (choices.isEmpty()) {                                                   // :248
                return -1;
            }
            return choices.get(battle.pbRandom(choices.size()));                       // :249
        }
        return pbSwitchInBetween(battle, idxBattler, true, false);                     // :251
    }

    /** {@code pbRecallAndReplace(idxBattler,idxParty,randomReplacement=false,batonPass=false)} (:256-262). */
    public static void pbRecallAndReplace(Battle battle, int idxBattler, int idxParty,
            boolean randomReplacement, boolean batonPass) {
        if (!battle.battlerAt(idxBattler).fainted()) {                                 // :257
            battle.scene.pbRecall(idxBattler);
        }
        Battler leaving = battle.battlerAt(idxBattler);
        leaving.pbAbilitiesOnSwitchOut();                                              // :258 Inc. primordial weather check
        if (battle.pbSideSize(idxBattler) == 1) {                                      // :259
            battle.scene.pbShowPartyLineup(idxBattler & 1);
        }
        if (!randomReplacement) {                                                      // :260
            pbMessagesOnReplace(battle, idxBattler, idxParty);
        }
        pbReplace(battle, idxBattler, idxParty, batonPass);                            // :261
        if (leaving != battle.battlerAt(idxBattler)) leaving.restoreAfterSwitchOut();  // the old Pokemon is back in the party as it was
    }

    /** {@code pbMessageOnRecall(battler)} (:264-281). */
    public static void pbMessageOnRecall(Battle battle, Battler battler) {
        if (battler.pbOwnedByPlayer()) {                                               // :265
            if (battler.hp <= battler.maxHp() / 4) {                                   // :266
                battle.displayBrief("做的很好，" + battler.name() + "！" + NL + "回来吧！");        // :267
            } else if (battler.hp <= battler.maxHp() / 2) {                            // :268
                battle.displayBrief("做的相当好了，" + battler.name() + "！" + NL + "回来吧！");    // :269
            } else if (battler.turnCount >= 5) {                                       // :270
                battle.displayBrief(battler.name() + "，你做的已经很棒了！" + NL + "回来吧！");     // :271
            } else if (battler.turnCount >= 2) {                                       // :272
                battle.displayBrief(battler.name() + "，回来吧！");                      // :273
            } else {
                battle.displayBrief("你已经尽力了，" + battler.name() + "！" + NL + "回来吧！");    // :275
            }
        } else {
            String owner = battle.pbGetOwnerName(battler.index);                       // :278
            battle.displayBrief(battler.name() + "回到了" + owner + "身边！");             // :279
        }
    }

    /** {@code pbMessagesOnReplace(idxBattler,idxParty)} (:284-305). */
    public static void pbMessagesOnReplace(Battle battle, int idxBattler, int idxParty) {
        Array<Battler> party = battle.partyOf(idxBattler);                             // :285
        String newPkmnName = party.get(idxParty).name();                               // :286
        if ("ILLUSION".equals(party.get(idxParty).ability)) {                          // :287
            newPkmnName = party.get(pbLastInTeam(battle, idxBattler)).name();          // :288
        }
        if (battle.pbOwnedByPlayer(idxBattler)) {                                      // :290
            Battler opposing = battle.battlerAt(idxBattler).pbDirectOpposing(false);   // :291
            if (opposing.fainted() || opposing.hp == opposing.maxHp()) {               // :292
                battle.displayBrief("加油啊！" + NL + newPkmnName);                      // :293
            } else if (opposing.hp >= opposing.maxHp() / 2) {                          // :294
                battle.displayBrief("去吧！" + NL + newPkmnName);                        // :295
            } else if (opposing.hp >= opposing.maxHp() / 4) {                          // :296
                battle.displayBrief("不要输给他！" + NL + "加油，" + newPkmnName);          // :297
            } else {
                battle.displayBrief("对手十分虚弱！" + NL + "抓住机会，" + newPkmnName + "！");   // :299
            }
        } else {
            String owner = battle.pbGetOwnerName(idxBattler);                          // :302 owner.fullname
            battle.displayBrief(owner + "派出了" + NL + newPkmnName + "！");              // :303
        }
    }

    /** {@code pbReplace(idxBattler,idxParty,batonPass=false)} (:309-320). */
    public static void pbReplace(Battle battle, int idxBattler, int idxParty, boolean batonPass) {
        // :311 idxPartyOld = @battlers[idxBattler].pokemonIndex is only read by the party-order swap (:315-316), see class javadoc.
        // :313 pbInitialize(party[idxParty],idxParty,batonPass); :319 pbCalculatePriority(false,[idxBattler]) is
        // part of Battle#replace (it does not depend on the send-out animation, which only draws).
        boolean replacingFainted = battle.battlerAt(idxBattler) != null && battle.battlerAt(idxBattler).fainted();
        battle.replace(idxBattler, idxParty, batonPass);
        Battler incoming = battle.battlerAt(idxBattler);
        if (incoming != null) {                                                        // CFRU switchingCooldown (end_turn.c:2171, battle_script_util.c:2416)
            incoming.aiSwitchCooldown = 0;
            if (replacingFainted) {
                incoming.aiSwitchCooldown = 1;                                         // AI shouldn't switch out again until the next end turn
            } else {
                if (battle.aiSideSwitchedTurn != battle.turns()) {
                    battle.aiSideSwitchedTurn = battle.turns();
                    battle.aiSideSwitchedMask = 0;
                }
                battle.aiSideSwitchedMask |= 1 << (idxBattler & 1);
                if ((battle.aiSideSwitchedMask & (1 << ((idxBattler & 1) ^ 1))) == 0) incoming.aiSwitchCooldown = 2;   // no change on the other side
            }
        }
        battle.aiPivotTo[(idxBattler ^ 1) % 6] = -1;                                   // switching.c:595 old pivot target is stale
        battle.aiPivotTo[((idxBattler ^ 1) ^ 2) % 6] = -1;                             // :597
        pbSendOut(battle, new int[] {idxBattler}, false);                              // :318
    }

    /** {@code pbSendOut(sendOuts,startBattle=false)} (:324-332). */
    public static void pbSendOut(Battle battle, int[] idxBattlers, boolean startBattle) {
        // :325 sendOuts.each { |b| @peer.pbOnEnteringBattle(self,b[1]) }: see class javadoc
        battle.scene.pbSendOutBattlers(idxBattlers, startBattle);                      // :326
        for (int idx : idxBattlers) {                                                  // :327
            // :328 @scene.pbResetMoveIndex(b[0]): see class javadoc
            battle.pbSetSeen(battle.battlerAt(idx));                                   // :329
            // :330 @usedInBattle[b[0]&1][b[0]/2] = true: see class javadoc
        }
    }

    /** {@code pbLastInTeam(idxBattler)} (PokeBattle_Battle:412-423). */
    public static int pbLastInTeam(Battle battle, int idxBattler) {
        Array<Battler> party = battle.partyOf(idxBattler);                             // :413
        int[] team = battle.pbTeamIndexRangeFromBattlerIndex(idxBattler);              // :415
        int ret = -1;                                                                  // :416
        for (int i = team[0]; i < team[1]; i++) {                                      // :417-418 (the owner's team only)
            Battler pkmn = party.get(i);
            if (pkmn == null || pkmn.fainted() || pkmn.pokemon.egg) {                  // :419 !pkmn.able?
                continue;
            }
            ret = i;                                                                   // :420 (no party order: the later index wins)
        }
        return ret;                                                                    // :422
    }

    // =====================================================================
    // Battle_Action_Running:36-157 pbRun
    // =====================================================================

    /**
     * {@code pbRun(idxBattler,duringBattle=false)} (Battle_Action_Running:36-157).
     *
     * @return -1 failed fleeing, 0 not possible, 1 succeeded (the battle ends)
     */
    public static int pbRun(Battle battle, int idxBattler, boolean duringBattle) {
        Battler battler = battle.battlerAt(idxBattler);                                // :37
        if (battler.opposes(0)) {                                                      // :38
            if (battle.trainerBattle) {                                                // :39
                return 0;
            }
            Object[] choice = battle.choices(idxBattler);
            choice[0] = ":Run";                                                        // :40
            choice[1] = 0;                                                             // :41
            choice[2] = null;                                                          // :42
            return -1;                                                                 // :43
        }
        // Fleeing from trainer battles
        if (battle.trainerBattle) {                                                    // :46
            // :47-56 $DEBUG && Input.press?(Input::CTRL): see class javadoc
            if (battle.internalBattle) {                                               // :57
                battle.displayPaused("不行！" + NL + "绝不能临阵脱逃！");                   // :58
            } else if (battle.pbDisplayConfirm("要放弃战斗直接投降吗？")) {               // :59
                battle.display(battle.playerName + "放弃战斗了！");                       // :61
                battle.decision = 3;                                                   // :62
                return 1;                                                              // :63
            }
            return 0;                                                                  // :65
        }
        // Fleeing from wild battles
        // :68-73 $DEBUG && Input.press?(Input::CTRL): see class javadoc
        if (!battle.canRun()) {                                                        // :74
            battle.displayPaused("逃跑失败了！");                                        // :75
            return 0;                                                                  // :76
        }
        if (!duringBattle) {                                                           // :78
            if (battler.pbHasType("GHOST") && Battle.NEWEST_BATTLE_MECHANICS) {        // :79
                battle.displayPaused("安全地逃跑了！");                                   // :81
                battle.decision = 3;                                                   // :82
                return 1;                                                              // :83
            }
            // Abilities that guarantee escape
            if (battler.abilityActive()) {                                             // :86
                if (BattleHandlers.triggerRunFromBattleAbility(battler.ability, battler)) {   // :87
                    battle.showAbilitySplash(battler);                                 // :88 (delay=true)
                    battle.hideAbilitySplash(battler);                                 // :89
                    battle.displayPaused("安全地逃跑了！");                               // :91
                    battle.decision = 3;                                               // :92
                    return 1;                                                          // :93
                }
            }
            // Held items that guarantee escape
            if (battler.itemActive()) {                                                // :97
                if (BattleHandlers.triggerRunFromBattleItem(battler.item, battler)) {  // :98
                    battle.displayPaused(battler.pbThis() + "使用" + battler.itemName() + "逃跑了！");   // :100-101
                    battle.decision = 3;                                               // :102
                    return 1;                                                          // :103
                }
            }
            // Other certain trapping effects
            if (battler.effects.intVal(PBEffects.Battler.Trapping) > 0                 // :107
                    || battler.effects.intVal(PBEffects.Battler.MeanLook) >= 0         // :108
                    || battler.effects.truthy(PBEffects.Battler.Ingrain)               // :109
                    || battler.effects.intVal(PBEffects.Battler.OctolockUser) >= 0     // :110
                    || battler.effects.truthy(PBEffects.Battler.NoRetreat)             // :111
                    || battle.field.effects.intVal(PBEffects.Field.FairyLock) > 0) {   // :112
                battle.displayPaused("逃跑失败了！");                                    // :113
                return 0;                                                              // :114
            }
            // Trapping abilities/items
            for (Battler b : battle.eachOtherSideBattler(idxBattler)) {                // :117
                if (!b.abilityActive()) {                                              // :118
                    continue;
                }
                if (BattleHandlers.triggerTrappingTargetAbility(b.ability, battler, b, battle)) {   // :119
                    battle.displayPaused(b.pbThis() + "的" + b.abilityName() + "阻止了逃跑！");       // :120
                    return 0;                                                          // :121
                }
            }
            for (Battler b : battle.eachOtherSideBattler(idxBattler)) {                // :124
                if (!b.itemActive()) {                                                 // :125
                    continue;
                }
                if (BattleHandlers.triggerTrappingTargetItem(b.item, battler, b, battle)) {   // :126
                    battle.displayPaused(b.pbThis() + "的" + b.itemName() + "阻止了逃跑！");         // :127
                    return 0;                                                          // :128
                }
            }
        }
        // Fleeing calculation
        // Get the speeds of the Pokemon fleeing and the fastest opponent
        // NOTE: Not pbSpeed, because using unmodified Speed.
        if (!duringBattle) {
            battle.runCommand += 1;                                                    // :135 Make it easier to flee next time
        }
        int speedPlayer = battle.rawSpeed(battler);                                    // :136
        int speedEnemy = 1;                                                            // :137
        for (Battler b : battle.eachOtherSideBattler(idxBattler)) {                    // :138
            int speed = battle.rawSpeed(b);                                            // :139
            if (speedEnemy < speed) {                                                  // :140
                speedEnemy = speed;
            }
        }
        // Compare speeds and perform fleeing calculation
        int rate;
        if (speedPlayer > speedEnemy) {                                                // :143
            rate = 256;                                                                // :144
        } else {
            rate = (speedPlayer * 128) / speedEnemy;                                   // :146
            rate += battle.runCommand * 30;                                            // :147
        }
        if (rate >= 256 || battle.pbRandom(256) < rate) {                              // :149 @battleAI.pbAIRandom(256)
            battle.displayPaused("安全地逃跑了！");                                       // :151
            battle.decision = 3;                                                       // :152
            return 1;                                                                  // :153
        }
        battle.displayPaused("无法逃跑！");                                              // :155
        return -1;                                                                     // :156
    }
}
