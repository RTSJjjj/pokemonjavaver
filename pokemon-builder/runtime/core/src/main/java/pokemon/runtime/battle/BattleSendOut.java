package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.Pokemon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * {@code pbStartBattleSendOut} (Battle_StartAndEnd:194-275), the straight-line
 * sequence that opens a battle: the "wants to battle" line, then one brief line
 * plus one send-out per side.
 *
 * <p>Ruby runs it as blocking calls ({@code pbDisplayPaused} waits for a
 * confirm, {@code pbDisplayBrief} does not, {@code pbSendOut} waits for the
 * animation). The runtime's battle screen is a frame-driven state machine, so
 * the method is transcribed into a {@link Step} list that the screen walks in
 * order - one step per original call, in the original order.</p>
 */
final class BattleSendOut {

    /** One {@code pbDisplay*} / {@code pbSendOut} call of the Ruby body. */
    static final class Step {
        enum Kind {
            /** {@code pbDisplayPaused(msg)}: waits for the player. */
            DISPLAY_PAUSED,
            /** {@code pbDisplayBrief(msg)}: lingers while the next call runs. */
            DISPLAY_BRIEF,
            /** {@code pbSendOut(animSendOuts,true)}. */
            SEND_OUT
        }

        final Kind kind;
        final String message;
        /** For {@link Kind#SEND_OUT}: the {@code idxBattler}s, in order. */
        final int[] battlers;

        private Step(Kind kind, String message, int[] battlers) {
            this.kind = kind;
            this.message = message;
            this.battlers = battlers;
        }
    }

    private BattleSendOut() {
    }

    /**
     * Builds the step list. {@code sendOuts[side][trainer]} mirrors
     * {@code pbSetUpSides} (Battle_StartAndEnd:116-189): the player's side has
     * one trainer with its first able battler, a trainer battle's opposing side
     * likewise, and a wild battle leaves side 1 empty exactly like
     * {@code pbSetUpSides:120-129} does (which is why :231 skips it).
     *
     * @param switches {@code $game_switches}, read at :202.
     */
    static List<Step> plan(Battle battle, boolean trainerBattle, String trainerFullname,
            int opponentCount, IntPredicate switches) {
        List<Step> steps = new ArrayList<>();
        // ---- "Want to battle" messages (Battle_StartAndEnd:195-228) ----
        if (!trainerBattle) {                                    // :196
            Array<Battler> foeParty = battle.foeParty();          // :197 pbParty(1)
            switch (foeParty.size) {                             // :198
                case 1: {
                    Pokemon foe = foeParty.first().pokemon;
                    int rank = foe == null ? 0 : foe.battleRank;
                    if (rank > 1) {                              // :200
                        String prefix = rank > 2 ? "强大的" : "特殊的";   // :201
                        if (switches != null && switches.test(196)) {     // :202
                            steps.add(paused("哦！" + prefix + foe.name
                                    + "出现了！\n无论如何都是无法捕捉的！"));       // :203
                        } else {
                            steps.add(paused("哦！" + prefix + foe.name
                                    + "出现了！\n看来不打倒它是无法捕捉的！"));      // :205
                        }
                    } else {
                        steps.add(paused("哦！一只野生的" + foe.name + "出现了！"));   // :208
                    }
                    break;
                }
                case 2:
                    steps.add(paused("野生的" + name(foeParty, 0) + "和" + name(foeParty, 1)
                            + "\n出现了！"));                     // :211-212
                    break;
                case 3:
                    steps.add(paused("野生的" + name(foeParty, 0) + "、" + name(foeParty, 1)
                            + "、" + name(foeParty, 2) + "\n出现了！"));   // :214-215
                    break;
                default:
                    break;
            }
        } else {   // Trainer battle (:217)
            switch (opponentCount) {                             // :218
                case 1:
                    steps.add(paused(trainerFullname + "\n向你发起挑战！"));   // :220
                    break;
                case 2:
                    steps.add(paused(trainerFullname + "、" + trainerFullname
                            + "\n向你发起挑战！"));                   // :222-223
                    break;
                case 3:
                    // The plugin's own text ends with a half-width "!" here (:225).
                    steps.add(paused(trainerFullname + "、" + trainerFullname + "、" + trainerFullname
                            + "\n向你发起挑战!"));                    // :225-226
                    break;
                default:
                    break;
            }
        }
        // ---- Send out Pokemon (opposing trainers first) (:229-274) ----
        for (int side : new int[] { 1, 0 }) {                    // :230
            if (side == 1 && !trainerBattle) {                   // :231 next if side==1 && wildBattle?
                continue;
            }
            StringBuilder msg = new StringBuilder();             // :232
            List<Integer> toSendOut = new ArrayList<>();         // :233
            int trainerCount = side == 0 ? 1 : Math.max(1, opponentCount);   // :234 @player / @opponent
            for (int i = 0; i < trainerCount; i++) {             // :236 each_with_index
                if (side == 0 && i == 0) {                       // :237 The player's message is shown last
                    continue;
                }
                if (msg.length() > 0) {
                    msg.append("\r\n");                          // :238
                }
                int[] sent = sentBattlers(battle, side);          // :239 sendOuts[side][i]
                switch (sent.length) {                           // :240
                    case 1:
                        msg.append(trainerFullname).append("派出了\n")
                                .append(name(battle, sent[0])).append("！");       // :242
                        break;
                    case 2:
                        msg.append(trainerFullname).append("派出了\n")
                                .append(name(battle, sent[0])).append("和")
                                .append(name(battle, sent[1])).append("！");       // :244-245
                        break;
                    case 3:
                        msg.append(trainerFullname).append("派出了\n")
                                .append(name(battle, sent[0])).append("、 ")
                                .append(name(battle, sent[1])).append("和")
                                .append(name(battle, sent[2])).append("！");       // :247-248
                        break;
                    default:
                        break;
                }
                for (int idx : sent) {
                    toSendOut.add(idx);                          // :250
                }
            }
            // The player's message about sending out Pokemon (:252-266)
            if (side == 0) {                                     // :253
                if (msg.length() > 0) {
                    msg.append("\r\n");                          // :254
                }
                int[] sent = sentBattlers(battle, side);          // :255 sendOuts[side][0]
                switch (sent.length) {                           // :256
                    case 1:
                        msg.append("去吧！\n").append(name(battle, sent[0])).append("！");   // :258
                        break;
                    case 2:
                        msg.append("去吧！\n").append(name(battle, sent[0])).append("和")
                                .append(name(battle, sent[1])).append("！");       // :260
                        break;
                    case 3:
                        msg.append("去吧！\n").append(name(battle, sent[0])).append("、")
                                .append(name(battle, sent[1])).append("和")
                                .append(name(battle, sent[2])).append("！");       // :262-263
                        break;
                    default:
                        break;
                }
                for (int idx : sent) {
                    toSendOut.add(idx);                          // :265
                }
            }
            if (msg.length() > 0) {
                steps.add(brief(msg.toString()));                // :267 pbDisplayBrief(msg)
            }
            // The actual sending out of Pokemon (:268-273)
            int[] animSendOuts = new int[toSendOut.size()];
            for (int i = 0; i < animSendOuts.length; i++) {
                animSendOuts[i] = toSendOut.get(i);              // :271
            }
            steps.add(new Step(Step.Kind.SEND_OUT, null, animSendOuts));   // :273 pbSendOut(...,true)
        }
        return steps;
    }

    /**
     * {@code sendOuts[side]} (Battle_StartAndEnd:177-185): the field slots that
     * side sends out. A wild battle never fills side 1 (:120-129 returns before
     * the trainer loop).
     */
    private static int[] sentBattlers(Battle battle, int side) {
        if (side == 0) {
            Battler player = battle.player();
            return player == null || player.index < 0 ? new int[0] : new int[] { player.index };
        }
        Battler foe = battle.foe();
        return foe == null || foe.index < 0 ? new int[0] : new int[] { foe.index };
    }

    /** {@code @battlers[idxBattler].name} (:242/258). */
    private static String name(Battle battle, int idxBattler) {
        Battler battler = battler(battle, idxBattler);
        return battler == null ? "" : battler.name();
    }

    /** {@code pbParty(1)[i].name} (:211-215). */
    private static String name(Array<Battler> party, int index) {
        if (index >= party.size) {
            return "";
        }
        Pokemon pokemon = party.get(index).pokemon;
        return pokemon == null ? "" : party.get(index).name();
    }

    /** {@code @battlers[idx]}: slot 0 is the player's side, slot 1 the opposing one. */
    static Battler battler(Battle battle, int idxBattler) {
        if ((idxBattler & 1) == 0) {
            for (Battler battler : battle.playerParty()) {
                if (battler.index == idxBattler) {
                    return battler;
                }
            }
            return null;
        }
        for (Battler battler : battle.foeParty()) {
            if (battler.index == idxBattler) {
                return battler;
            }
        }
        return null;
    }

    private static Step paused(String message) {
        return new Step(Step.Kind.DISPLAY_PAUSED, message, null);
    }

    private static Step brief(String message) {
        return new Step(Step.Kind.DISPLAY_BRIEF, message, null);
    }
}
