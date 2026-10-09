package pokemon.runtime.pokemon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** 228_PScreen_MoveRelearner:1-34, the moves a Pokemon may be taught again. 登记: shadow Pokemon are not modelled. */
public final class MoveRelearner {
    private MoveRelearner() {
    }

    /** {@code pbHasRelearnableMove?} (:7-9). */
    public static boolean hasRelearnableMove(Pokemon pokemon, PbsData data) {
        return pbGetRelearnableMoves(pokemon, data).size() > 0;
    }

    /** {@code pbGetRelearnableMoves} (:11-34): first moves, then technical records, then the natural moves. */
    public static List<String> pbGetRelearnableMoves(Pokemon pokemon, PbsData data) {
        List<String> none = new ArrayList<>();
        if (pokemon == null || pokemon.egg) return none;                              // :12 (shadowPokemon? is false)
        List<String> moves = new ArrayList<>();
        if (pokemon.species != null) {
            for (PbsData.LearnMove learn : pokemon.getMoveList(data)) {                // :2-5 pbEachNaturalMove
                if (learn.level <= pokemon.level && !knows(pokemon, learn.move)) {     // :15
                    if (!moves.contains(learn.move)) moves.add(learn.move);            // :16
                }
            }
        }
        List<String> tmoves = new ArrayList<>();
        for (String move : pokemon.firstMoves) {                                       // :19-22
            if (!knows(pokemon, move) && !moves.contains(move)) tmoves.add(move);
        }
        List<String> trmoves = new ArrayList<>();
        for (String move : pokemon.trMoves) {                                          // :25-28
            if (!knows(pokemon, move) && !moves.contains(move)) trmoves.add(move);
        }
        List<String> all = new ArrayList<>(tmoves);                                    // :30
        all.addAll(trmoves);
        all.addAll(moves);
        if (pokemon.species != null && "KYUREM".equals(pokemon.species.internalName) && pokemon.formIndex() != 0) {
            all.remove("GLACIATE");                                                    // :31-33
        }
        return new ArrayList<>(new LinkedHashSet<>(all));                              // :34 moves|[]
    }

    private static boolean knows(Pokemon pokemon, String move) {
        for (Pokemon.MoveSlot slot : pokemon.moves) {
            if (slot != null && slot.move != null && move.equals(slot.move.internalName)) return true;
        }
        return false;
    }
}
