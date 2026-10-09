package pokemon.runtime.field;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

/**
 * 179_PField_FieldMoves: the questions an event asks when the player faces a tree ({@code pbCut}), a rock
 * ({@code pbRockSmash}) or a boulder ({@code pbStrength}), and the hidden move helpers they use
 * ({@code pbCheckHiddenMoveBadge}, {@code pbCheckMove}).
 *
 * <p>登记: {@code $DEBUG} is false; the hidden move animation ({@code pbHiddenMoveAnimation}) and the
 * party-menu entry of the moves are roadmap stage 7.1.</p>
 */
public final class FieldMoves {
    /** 000_Settings:129-137. */
    public static final boolean FIELD_MOVES_COUNT_BADGES = true;
    public static final int BADGE_FOR_CUT = 1;
    public static final int BADGE_FOR_FLASH = 0;
    public static final int BADGE_FOR_ROCKSMASH = 2;
    public static final int BADGE_FOR_SURF = 6;
    public static final int BADGE_FOR_FLY = 5;
    public static final int BADGE_FOR_STRENGTH = 2;
    public static final int BADGE_FOR_DIVE = 7;
    public static final int BADGE_FOR_WATERFALL = 8;

    private final PbsData pbs;
    private final GameState state;

    public FieldMoves(PbsData pbs, GameState state) {
        this.pbs = pbs;
        this.state = state;
    }

    private String moveName(String move) {
        PbsData.Move data = pbs == null ? null : pbs.move(move);
        return data == null || data.name == null ? move : data.name;
    }

    /** {@code pbCheckHiddenMoveBadge(badge, showmsg)} (:63-72): {@code $Trainer.numbadges >= badge} while the badges count. */
    public boolean pbCheckHiddenMoveBadge(int badge, boolean showmsg, FieldScene scene) {
        if (badge < 0) {
            return true;                                                        // :64
        }
        boolean ok = FIELD_MOVES_COUNT_BADGES ? state.trainer().badges.size() >= badge
                : state.trainer().badges.contains(badge);                       // :66
        if (ok) {
            return true;
        }
        if (showmsg) {
            scene.pbMessage("对不起，\n这需要拥有对应的徽章。");                       // :70
        }
        return false;
    }

    /** {@code pbCheckMove(move)} (252_PSystem_PokemonUtilities:371-380): the first party Pokemon that knows the move. */
    public Pokemon pbCheckMove(String move) {
        for (Pokemon pkmn : state.trainer().party.members()) {
            if (pkmn.egg) {
                continue;                                                       // pokemonParty has no eggs
            }
            for (Pokemon.MoveSlot slot : pkmn.moves) {
                if (slot.move != null && move.equals(slot.move.internalName)) {
                    return pkmn;
                }
            }
        }
        return null;
    }

    private boolean hasItem(String item) {
        return state.inventory().has(item);
    }

    /** {@code pbCut} (:194-208). */
    public boolean pbCut(FieldScene scene) {
        Pokemon movefinder = pbCheckMove("CUT");                                // :196
        boolean hasHm = hasItem("HM01");
        if (!pbCheckHiddenMoveBadge(BADGE_FOR_CUT, false, scene) || (movefinder == null && !hasHm)) {   // :197
            scene.pbMessage("这棵树看起来似乎可以被砍倒。");                          // :198
            return false;
        }
        scene.pbMessage("这棵树看起来似乎可以被砍倒！");                            // :201
        if (scene.pbConfirmMessage("想要砍倒它吗？")) {                            // :202
            String speciesname = movefinder != null ? movefinder.name : state.trainer().name;   // :203
            scene.pbMessage(speciesname + "使用了" + moveName("CUT") + "！");       // :204
            return true;
        }
        return false;
    }

    /** {@code pbRockSmash} (:610-623). */
    public boolean pbRockSmash(FieldScene scene) {
        Pokemon movefinder = pbCheckMove("ROCKSMASH");                          // :612
        boolean hasHm = hasItem("HM07");
        if (!pbCheckHiddenMoveBadge(BADGE_FOR_ROCKSMASH, false, scene) || (movefinder == null && !hasHm)) {   // :613
            scene.pbMessage("这是一块岩石，\n也许宝可梦能破坏它。");                    // :614
            return false;
        }
        if (scene.pbConfirmMessage("这是一块岩石，要使用岩石粉碎吗？")) {            // :618
            String speciesname = movefinder != null ? movefinder.name : state.trainer().name;   // :619
            scene.pbMessage(speciesname + "使用了" + moveName("ROCKSMASH") + "！");   // :620
            return true;
        }
        return false;
    }

    /** {@code pbStrength} (:652-672). */
    public boolean pbStrength(FieldScene scene) {
        if (state.pokemonMapStrengthUsed()) {                                   // :653 $PokemonMap.strengthUsed
            scene.pbMessage("现在可以移动石头了。");                                // :654
            return false;
        }
        if (!pbCheckHiddenMoveBadge(BADGE_FOR_STRENGTH, false, scene)) {        // :659
            scene.pbMessage("这是一块石头，\n但也许宝可梦能移动它。");                  // :660
            return false;
        }
        scene.pbMessage("这是一块石头，\n但也许宝可梦能移动它。\u0001");               // :663
        if (scene.pbConfirmMessage("要使用怪力吗？")) {                            // :664
            String speciesname = state.trainer().name;                          // :665
            scene.pbMessage(speciesname + "使用" + moveName("STRENGTH") + "！");   // :666
            scene.pbMessage(speciesname + "的怪力\n现在可以移动石头了。");            // :667
            state.pokemonMapStrengthUsed(true);                                 // :668
            return true;
        }
        return false;
    }
}
