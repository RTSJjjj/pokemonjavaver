package pokemon.runtime.battle;

import pokemon.runtime.data.BattleAnimationData;
import pokemon.runtime.pokemon.PbsData;

/**
 * {@code pbFindMoveAnimDetails} (Scene_Animations:427-438) and
 * {@code pbFindMoveAnimation} (:440-504): which animation a move plays.
 *
 * <p>The result is {@code [anim, noFlip]} like the Ruby array ({@code anim} the
 * position in {@code PBAnimations}, {@code noFlip} 1 when an opposing user
 * found an {@code OppMove} animation), or null for Ruby's {@code nil}.</p>
 */
final class MoveAnimationFinder {

    private MoveAnimationFinder() {
    }

    /** {@code typeDefaultAnim} (:461-480): [one target physical, one target special, user status, multiple physical, multiple special, non-user status]. */
    private static final String[][][] TYPE_DEFAULT_ANIM = {
        { {"NORMAL"},   {"TACKLE", "SONICBOOM", "DEFENSECURL", "EXPLOSION", "SWIFT", "TAILWHIP"} },
        { {"FIGHTING"}, {"MACHPUNCH", "AURASPHERE", "DETECT", null, null, null} },
        { {"FLYING"},   {"WINGATTACK", "GUST", "ROOST", null, "AIRCUTTER", "FEATHERDANCE"} },
        { {"POISON"},   {"POISONSTING", "SLUDGE", "ACIDARMOR", null, "ACID", "POISONPOWDER"} },
        { {"GROUND"},   {"SANDTOMB", "MUDSLAP", null, "EARTHQUAKE", "EARTHPOWER", "MUDSPORT"} },
        { {"ROCK"},     {"ROCKTHROW", "POWERGEM", "ROCKPOLISH", "ROCKSLIDE", null, "SANDSTORM"} },
        { {"BUG"},      {"TWINEEDLE", "BUGBUZZ", "QUIVERDANCE", null, "STRUGGLEBUG", "STRINGSHOT"} },
        { {"GHOST"},    {"LICK", "SHADOWBALL", "GRUDGE", null, null, "CONFUSERAY"} },
        { {"STEEL"},    {"IRONHEAD", "MIRRORSHOT", "IRONDEFENSE", null, null, "METALSOUND"} },
        { {"FIRE"},     {"FIREPUNCH", "EMBER", "SUNNYDAY", null, "INCINERATE", "WILLOWISP"} },
        { {"WATER"},    {"CRABHAMMER", "WATERGUN", "AQUARING", null, "SURF", "WATERSPORT"} },
        { {"GRASS"},    {"VINEWHIP", "MEGADRAIN", "COTTONGUARD", "RAZORLEAF", null, "SPORE"} },
        { {"ELECTRIC"}, {"THUNDERPUNCH", "THUNDERSHOCK", "CHARGE", null, "DISCHARGE", "THUNDERWAVE"} },
        { {"PSYCHIC"},  {"ZENHEADBUTT", "CONFUSION", "CALMMIND", null, "SYNCHRONOISE", "MIRACLEEYE"} },
        { {"ICE"},      {"ICEPUNCH", "ICEBEAM", "MIST", null, "POWDERSNOW", "HAIL"} },
        { {"DRAGON"},   {"DRAGONCLAW", "DRAGONRAGE", "DRAGONDANCE", null, "TWISTER", null} },
        { {"DARK"},     {"PURSUIT", "DARKPULSE", "HONECLAWS", null, "SNARL", "EMBARGO"} },
        { {"FAIRY"},    {"TACKLE", "FAIRYWIND", "MOONLIGHT", null, "SWIFT", "SWEETKISS"} },
    };

    /** {@code pbFindMoveAnimDetails(move2anim,moveID,idxUser,hitNum=0)} (:427-438). */
    static int[] findMoveAnimDetails(BattleAnimationData data, int moveId, int idxUser, int hitNum) {
        boolean noFlip = false;                                      // :428
        int anim;
        if ((idxUser & 1) == 0) {                                    // :429 On player's side
            anim = data.moveAnim(moveId, false);                     // :430
        } else {                                                     // :431 On opposing side
            anim = data.moveAnim(moveId, true);                      // :432
            if (anim >= 0) {
                noFlip = true;                                       // :433
            }
            if (anim < 0) {
                anim = data.moveAnim(moveId, false);                 // :434
            }
        }
        if (anim >= 0) {
            return new int[] { anim + hitNum, noFlip ? 1 : 0 };      // :436
        }
        return null;                                                 // :437
    }

    /** {@code hasConst?(PBMoves,:NAME)}: the move exists in the PBS. */
    private static PbsData.Move moveConst(PbsData pbs, String internalName) {
        return pbs == null ? null : pbs.move(internalName);
    }

    /** {@code pbGetMoveData(moveID)}: the PBS move with that id, or null. */
    private static PbsData.Move moveData(PbsData pbs, int moveId) {
        if (pbs == null) {
            return null;
        }
        for (PbsData.Move move : pbs.moves.values()) {
            if (move.id == moveId) {
                return move;
            }
        }
        return null;
    }

    /**
     * {@code pbFindMoveAnimation(moveID,idxUser,hitNum)} (:444-504). The Ruby
     * wraps everything in {@code begin ... rescue end}, so a move missing from
     * the data returns nil.
     */
    static int[] findMoveAnimation(BattleAnimationData data, PbsData pbs, int moveId, int idxUser, int hitNum) {
        // Find actual animation requested (an opponent using the animation first
        // looks for an OppMove version then a Move version)
        int[] anim = findMoveAnimDetails(data, moveId, idxUser, hitNum);    // :449
        if (anim != null) {
            return anim;                                             // :450
        }
        // Actual animation not found, get the default animation for the move's type
        PbsData.Move moveData = moveData(pbs, moveId);               // :452
        if (moveData == null) {
            return null;                                             // :501 rescue
        }
        String moveType = moveData.type;                             // :453
        int moveKind = categoryId(moveData.category);                // :454
        int target = PBTargets.fromName(moveData.target);
        if (PBTargets.multipleTargets(target) || PBTargets.targetsFoeSide(target)) {   // :455-456
            moveKind += 3;
        }
        if (moveKind == 2 && target != PBTargets.User && target != PBTargets.UserSide) {   // :457-458
            moveKind += 3;
        }
        for (String[][] entry : TYPE_DEFAULT_ANIM) {                 // :481
            if (!entry[0][0].equalsIgnoreCase(moveType)) {           // :482 next if !isConst?(moveType,PBTypes,type)
                continue;
            }
            String[] anims = entry[1];
            if (anims[moveKind] != null && moveConst(pbs, anims[moveKind]) != null) {   // :483
                anim = findMoveAnimDetails(data, moveConst(pbs, anims[moveKind]).id, idxUser, 0);   // :484
            }
            if (anim != null) {
                break;                                               // :486
            }
            if (moveKind >= 3 && anims[moveKind - 3] != null
                    && moveConst(pbs, anims[moveKind - 3]) != null) {            // :487
                anim = findMoveAnimDetails(data, moveConst(pbs, anims[moveKind - 3]).id, idxUser, 0);   // :488
            }
            if (anim != null) {
                break;                                               // :490
            }
            if (anims[2] != null && moveConst(pbs, anims[2]) != null) {           // :491
                anim = findMoveAnimDetails(data, moveConst(pbs, anims[2]).id, idxUser, 0);   // :492
            }
            break;                                                   // :494
        }
        if (anim != null) {
            return anim;                                             // :496
        }
        // Default animation for the move's type not found, use Tackle's animation
        PbsData.Move tackle = moveConst(pbs, "TACKLE");              // :498
        if (tackle != null) {
            return findMoveAnimDetails(data, tackle.id, idxUser, 0); // :499
        }
        return null;                                                 // :503
    }

    /** {@code MOVE_CATEGORY}: 0 Physical, 1 Special, 2 Status. */
    private static int categoryId(String category) {
        if ("Physical".equalsIgnoreCase(category)) {
            return 0;
        }
        if ("Special".equalsIgnoreCase(category)) {
            return 1;
        }
        return 2;
    }
}
