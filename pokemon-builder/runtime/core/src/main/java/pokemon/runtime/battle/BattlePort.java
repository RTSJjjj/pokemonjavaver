package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

/**
 * Stage 3 / P2: how the event layer runs a battle. The runtime binds a headless
 * implementation (auto battle) now; the battle scene (P2d) swaps in an
 * interactive one behind the same interface.
 */
public interface BattlePort {

    /** A wild battle against a freshly generated Pokemon. */
    BattleResult wildBattle(String species, int level);

    /** A wild battle against a specific Pokemon (pbFreeWildBattle). */
    BattleResult freeWildBattle(Pokemon foe);

    /** A trainer battle from a trainers.txt entry. */
    BattleResult trainerBattle(PbsData.TrainerData trainer);
}
