package pokemon.runtime.battle;

/**
 * The target menu's data ({@code Scene_Commands:364-417}) and {@code pbChooseTarget} (Battle_Phase_Command:98-104) for
 * non-single battles. The screen shows the menu; these are the parts that read the battle.
 */
public final class BattleCommandPhase {

    private BattleCommandPhase() {
    }

    /**
     * {@code pbCreateTargetTexts(idxBattler,targetType)} (Scene_Commands:371-391): for each battler index, null when
     * that position cannot be chosen, "" when it can but nobody is there (a fainted battler), else the battler's name.
     */
    public static String[] pbCreateTargetTexts(Battle battle, int idxBattler, int targetType) {
        String[] texts = new String[battle.maxBattlerIndex() + 1];                         // :372
        for (int i = 0; i < texts.length; i++) {
            Battler b = battle.battlerAt(i);
            if (b == null) continue;                                                       // :373
            boolean showName;                                                              // :374
            switch (targetType) {                                                          // :375
                case PBTargets.None:
                case PBTargets.User:
                case PBTargets.RandomNearFoe:
                    showName = i == idxBattler;                                            // :377
                    break;
                case PBTargets.UserSide:
                case PBTargets.UserAndAllies:
                    showName = !battle.opposes(i, idxBattler);                             // :379
                    break;
                case PBTargets.FoeSide:
                case PBTargets.AllFoes:
                    showName = battle.opposes(i, idxBattler);                              // :381
                    break;
                case PBTargets.BothSides:
                case PBTargets.AllBattlers:
                    showName = true;                                                       // :383
                    break;
                default:
                    showName = battle.pbMoveCanTarget(i, idxBattler, targetType);          // :385
                    break;
            }
            if (!showName) continue;                                                       // :387
            texts[i] = b.fainted() ? "" : b.name();                                        // :388
        }
        return texts;
    }

    /** {@code pbFirstTarget(idxBattler,targetType)} (Scene_Commands:395-417): where the cursor starts. */
    public static int pbFirstTarget(Battle battle, int idxBattler, int targetType) {
        switch (targetType) {                                                              // :396
            case PBTargets.NearAlly: {                                                     // :397
                for (Battler b : battle.eachSameSideBattler(idxBattler)) {                 // :398
                    if (b.index == idxBattler || !battle.nearBattlers(b.index, idxBattler)) continue;   // :399
                    if (b.fainted()) continue;                                             // :400
                    return b.index;                                                        // :401
                }
                for (Battler b : battle.eachSameSideBattler(idxBattler)) {                 // :403
                    if (b.index == idxBattler || !battle.nearBattlers(b.index, idxBattler)) continue;   // :404
                    return b.index;                                                        // :405
                }
                break;
            }
            case PBTargets.NearFoe:
            case PBTargets.NearOther: {                                                    // :407
                int[] indices = battle.pbGetOpposingIndicesInOrder(idxBattler);            // :408
                for (int i : indices) {                                                    // :409
                    if (battle.nearBattlers(i, idxBattler) && !battle.battlerAt(i).fainted()) return i;
                }
                for (int i : indices) {                                                    // :410
                    if (battle.nearBattlers(i, idxBattler)) return i;
                }
                break;
            }
            case PBTargets.Foe:
            case PBTargets.Other: {                                                        // :411
                int[] indices = battle.pbGetOpposingIndicesInOrder(idxBattler);            // :412
                for (int i : indices) {                                                    // :413
                    if (!battle.battlerAt(i).fainted()) return i;
                }
                for (int i : indices) {                                                    // :414
                    return i;
                }
                break;
            }
            default:
                break;
        }
        return idxBattler;                                                                 // :416
    }
}
