package pokemon.runtime.field;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.util.List;

/**
 * What an item handler asks of the screen it runs on: the {@code scene} argument of
 * {@code ItemHandlers::UseOnPokemon} (a {@code PokemonPartyScreen}) plus the global {@code pbMessage} family the
 * handlers also call. The plugin's handlers are plain blocking Ruby, so every method returns when the player is done;
 * the party screen runs the handler on a {@link BlockingTask} and answers each call from its own state machine.
 */
public interface ItemScene {
    /** {@code screen.pbStartScene(helptext, false, annot)}: opens the party screen with its help text and annotations (or null). */
    void pbStartScene(String helpText, String[] annotations);

    /** {@code scene.pbSetHelpText(text)}. */
    void pbSetHelpText(String text);

    /** {@code scene.pbRefreshAnnotations(ableProc)} (210_PScreen_Party:1143-1151): "可以使用" / "无效" for each Pokemon. */
    void pbRefreshAnnotations(java.util.function.Predicate<Pokemon> able);

    /** {@code scene.pbClearAnnotations} (210_PScreen_Party:1152-1154). */
    void pbClearAnnotations();

    /** {@code scene.pbDisplay(text)}: a message in the party screen's own message window. */
    void pbDisplay(String text);

    /** {@code scene.pbConfirm(text)} (= pbDisplayConfirm): a yes/no question. */
    boolean pbConfirm(String text);

    /** {@code scene.pbRefresh}. */
    void pbRefresh();

    /** {@code scene.pbHardRefresh}. */
    void pbHardRefresh();

    /** {@code scene.pbChooseMove(pkmn, text)}: the index of the move picked, or -1. */
    int pbChooseMove(Pokemon pkmn, String text);

    /** {@code scene.pbChoosePokemon(text)}: the party index picked, or -1. */
    int pbChoosePokemon(String text);

    /** {@code pbMessage(text)}: the global message window. */
    void pbMessage(String text);

    /** {@code pbMessage(text, commands, cmdIfCancel)}: a message with a choice list; the index chosen, or the cancel value. */
    int pbMessage(String text, List<String> commands, int cmdIfCancel);

    /** {@code scene.pbShowCommands(text, commands, index)}: the index chosen or -1. */
    int pbShowCommands(String text, List<String> commands, int index);

    /** {@code pbMessageChooseNumber(text, params)}: a number in 1..max (starting at {@code defaultValue}), or the cancel value. */
    int pbMessageChooseNumber(String text, int max, int defaultValue, int cancelValue);

    /** {@code pbTopRightWindow(text, scene)}: a small window at the top right, closed with C. */
    void pbTopRightWindow(String text);

    /** {@code pbForgetMove(pkmn, move)}: the summary screen's forget mode; the index of the move to forget, or -1. */
    int pbForgetMove(Pokemon pkmn, PbsData.Move moveToLearn);

    /** {@code pbSEPlay(name)}. */
    void pbSEPlay(String name);

    /**
     * {@code pbFadeOutInWithMusic { evo = PokemonEvolutionScene ... }}: plays the evolution of {@code pkmn} into
     * {@code species}; {@code item} is the stone used (null for the scrolls, which keep the form they set).
     */
    void pbEvolution(Pokemon pkmn, PbsData.Species species, String item);
}
