package pokemon.runtime.field;

import java.util.List;

/**
 * What a field script (a hidden move's {@code pbCut}, an item used on the map ...) asks of the screen: the global
 * {@code pbMessage} family and the waits. The scripts are blocking Ruby, so every call returns when it is done; the
 * event interpreter runs them on a {@link BlockingTask} and plays each request.
 */
public interface FieldScene {
    /** {@code pbMessage(text)}. */
    void pbMessage(String text);

    /** {@code pbConfirmMessage(text)}: true for Yes. */
    boolean pbConfirmMessage(String text);

    /** {@code pbMessage(text, commands, cmdIfCancel)}: the index chosen (B answers cmdIfCancel-1, or -1 when it is -1). */
    int pbMessage(String text, List<String> commands, int cmdIfCancel);

    /** {@code pbWait(frames)} at 40 fps. */
    void pbWait(int frames);

    /**
     * {@code pbMoveTutorChoose(move, movelist, bymachine)}: the party screen of the tutor; the party index of the
     * Pokemon that learned the move, or -1.
     */
    int pbMoveTutorChoose(String move, List<String> movelist, boolean byMachine);

    /** {@code pbChooseNonEggPokemon(1, 3)}: the party index chosen, or -1. */
    default int pbChooseNonEggPokemon() {
        return -1;
    }

    /** {@code pbRelearnMoveScreen(pokemon)} (228_PScreen_MoveRelearner:221-228): true when a move was taught. */
    default boolean pbRelearnMoveScreen(pokemon.runtime.pokemon.Pokemon pkmn) {
        return false;
    }

    /** {@code pbForgetMove(pkmn, move)}: the summary screen's forget mode; the index to forget, or -1. */
    default int pbForgetMove(pokemon.runtime.pokemon.Pokemon pkmn, pokemon.runtime.pokemon.PbsData.Move move) {
        return -1;
    }

    /** {@code pbChooseItemScreen(proc)}: the internal name of the item picked in the bag, or null. */
    default String pbChooseItem(java.util.function.Predicate<String> filter) {
        return null;
    }

    /** {@code pbMEPlay(name)}. */
    default void pbMEPlay(String name) {
    }

    /** {@code pbSEPlay(name, volume)}. */
    void pbSEPlay(String name, int volume);

    /**
     * Runs {@code action} on the interpreter's thread (it touches the map, plays a banner, starts a transfer) and
     * waits the seconds it returns. The default (a test scene) runs it at once.
     */
    default void runAction(java.util.function.Supplier<Float> action) {
        action.get();
    }
}
