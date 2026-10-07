package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

/**
 * Stage 3 / P2: how the event layer runs a battle. The runtime binds a headless
 * implementation (auto battle) now; the battle scene (P2d) swaps in an
 * interactive one behind the same interface.
 */
public interface BattlePort {
    /** An interactive port keeps events suspended until the result is acknowledged. */
    default boolean pending() { return false; }

    /** A wild battle against a freshly generated Pokemon. */
    BattleResult wildBattle(String species, int level);

    /** A wild battle against a specific Pokemon (pbFreeWildBattle). */
    BattleResult freeWildBattle(Pokemon foe);

    /** A trainer battle from a trainers.txt entry. */
    BattleResult trainerBattle(PbsData.TrainerData trainer);

    /** Several wild Pokemon at once (pbWildBattleCore with a foe party of 2, PField_Battles:262-345). */
    default BattleResult freeWildBattle(java.util.List<Pokemon> foes) {
        return foes.isEmpty() ? null : freeWildBattle(foes.get(0));
    }

    /** Several opposing trainers (pbTrainerBattleCore with 2 arguments, PField_Battles:399-518). */
    default BattleResult trainerBattle(java.util.List<PbsData.TrainerData> trainers) {
        return trainers.isEmpty() ? null : trainerBattle(trainers.get(0));
    }

    /**
     * {@code setBattleRule("single"/"double"/...)} (PField_Battles:30-32, applied by
     * {@code pbPrepareBattle:96} {@code battle.setBattleMode}): the size of the next battle; null = the default.
     */
    default void setBattleSize(String size) {
    }

    /**
     * {@code $PokemonGlobal.partner} (PField_Field:1399-1416) for the next battle: the partner's trainer type, name
     * and party, or null when none is registered. Whether the partner joins is decided by
     * {@code pbWildBattleCore:303-318} / {@code pbTrainerBattleCore:459-487}.
     */
    default void setPartner(String trainerType, String name, Iterable<Pokemon> party) {
    }

    /** {@code setBattleRule("noPartner")} (PField_Battles:51). */
    default void setNoPartner(boolean value) {
    }

    /**
     * The result of the battle that just finished, or null. The interpreter
     * reads it when it resumes to write the outcome variable
     * (PField_Battles:510-516); an interactive port keeps it until the next
     * battle starts.
     */
    default BattleResult lastResult() {
        return null;
    }

    /**
     * {@code setBattleRule("canLose")} (PField_Battles:98): a loss then skips
     * the white-out. The port applies it to the next battle only.
     */
    default void setCanLose(boolean value) {
    }

    /**
     * {@code setBattleRule("noExp")} (PField_Battles:38, applied by {@code pbPrepareBattle:105}
     * {@code battle.expGain = battleRules["expGain"]}): whether the next battle gives Exp.
     */
    default void setExpGain(boolean value) {
    }

    /**
     * {@code setBattleRule("disablePokeballs")} (PField_Battles:109): no Poke
     * Ball may be thrown in the next battle.
     */
    default void setDisablePokeBalls(boolean value) {
    }

    /**
     * {@code setBattleRule("canRun")} / {@code pbWildBattle}'s {@code canRun}
     * (PField_Battles:101, Battle_Action_Running:8): a wild battle the player
     * may not flee from.
     */
    default void setCanRun(boolean value) {
    }

    /**
     * {@code $game_switches}: switch 60 is the project's "不可捕捉的野外对战"
     * (PokeBattle_BattleCommon:104-107).
     */
    default void setSwitchSource(java.util.function.IntPredicate switches) {
    }

    /** {@code $game_variables}: variable 100 is the level-follow mode of trainer battles. */
    default void setVariableSource(java.util.function.IntUnaryOperator variables) {
    }

    /**
     * PField_Battles:146-151 + :181-188: the map's MetadataEnvironment as a
     * PBEnvironment id (-1 when it declares none), which decides
     * {@code battle.environment} and {@code battle.time}.
     */
    default void setBattleEnvironment(int environment) {
    }

    /**
     * {@code $game_map.map_id} when the battle starts: {@code pbGenerateWildPokemon}
     * stores it as {@code @obtainMap} (PField_Encounters:951) and
     * {@code Settings:32}'s fateful-encounter switch as {@code @obtainMode}
     * (:954-955).
     */
    default void setObtainMap(int mapId) {
    }

    default void setFatefulEncounter(boolean value) {
    }

    /**
     * {@code setBattleRule("switchstyle")} / {@code ("setstyle")}
     * (PField_Battles:112): whether the game asks the player to send out a new
     * Pokemon when an opponent's Pokemon faints (Battle_Action_Switching:185-202).
     */
    default void setSwitchStyle(boolean value) {
    }

    /**
     * {@code setBattleRule("anims")} / {@code ("noanims")} (PField_Battles:43-44,
     * applied at :115): overrides the "Battle Effects" option for the next battle.
     */
    default void setBattleAnims(boolean value) {
    }

    /**
     * Battle_ExpAndMoveLearning:167-186: the level lock reads
     * {@code $game_switches[199]} (the lock is on) and {@code [12]} (the league
     * pass / second playthrough). The event layer sets them before a battle.
     */
    default void setLevelLock(boolean on, boolean leaguePass) {
    }
}
