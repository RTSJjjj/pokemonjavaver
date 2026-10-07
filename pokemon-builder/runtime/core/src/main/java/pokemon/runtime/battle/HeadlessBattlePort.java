package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.TrainerState;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Stage 3 / P2: the headless {@link BattlePort}. It builds the foe side, runs
 * the engine to completion against the player's party and reports the outcome;
 * a loss triggers the white-out callback (the runtime heals and warps to the
 * PokeCenter). The battle scene (P2d) replaces this with an interactive port.
 */
public final class HeadlessBattlePort implements BattlePort {

    /** Called after a loss: heal the party and warp to the PokeCenter. */
    public interface WhiteoutHandler {
        void whiteout();
    }

    private static final int MAX_TURNS = 1000;

    private final TrainerState trainer;
    private final Supplier<PbsData> pbs;
    private final WhiteoutHandler whiteout;
    private final Random random;
    /** The last finished battle (the interpreter's outcome variable). */
    private BattleResult lastResult;
    /** setBattleRule("canLose"): suppress the white-out for the next battle. */
    private boolean canLose;
    /** setBattleRule("canRun") / pbWildBattle's canRun: the next battle only. */
    private boolean canRun = true;

    public HeadlessBattlePort(TrainerState trainer, Supplier<PbsData> pbs,
                              WhiteoutHandler whiteout, Random random) {
        this.trainer = trainer;
        this.pbs = pbs;
        this.whiteout = whiteout;
        this.random = random == null ? new Random() : random;
    }

    @Override
    public BattleResult wildBattle(String species, int level) {
        PbsData data = pbs.get();
        PbsData.Species found = data == null ? null : data.species(species);
        if (found == null) {
            return null;
        }
        Pokemon foe = wildPokemon(found, Math.max(1, level), data);
        Array<Pokemon> foes = new Array<>();
        foes.add(foe);
        return run(foes, data);
    }

    @Override
    public BattleResult freeWildBattle(Pokemon foe) {
        if (foe == null) {
            return null;
        }
        Array<Pokemon> foes = new Array<>();
        foes.add(foe);
        return run(foes, pbs.get());
    }

    @Override
    public BattleResult trainerBattle(PbsData.TrainerData trainerData) {
        PbsData data = pbs.get();
        if (trainerData == null || data == null) {
            return null;
        }
        Array<Pokemon> foes = new Array<>();
        for (PbsData.TrainerPokemon member : trainerData.party) {
            Pokemon built = trainerPokemon(member, data);
            if (built != null) {
                foes.add(built);
            }
        }
        if (foes.size == 0) {
            return null;
        }
        return run(foes, data);
    }

    private BattleResult run(Array<Pokemon> foes, PbsData data) {
        if (trainer.party.size() == 0) {
            return null; // no Pokemon to send out
        }
        Battle battle = new Battle(data, random, null);
        battle.setCanRun(canRun);
        for (Pokemon member : trainer.party.members()) {
            battle.addPlayer(member);
        }
        for (Pokemon foe : foes) {
            trainer.registerSeen(foe);
            battle.addFoe(foe);
        }
        BattleResult result = battle.run(MAX_TURNS);
        lastResult = result;
        for (Pokemon member : trainer.party.members()) trainer.registerOwned(member);
        // PField_Battles:619-658 pbAfterBattle, then Events.onEndBattle
        // (:684-708) - the same order as the interactive port.
        BattleAftermath.pbAfterBattle(trainer, data, result, canLose);
        boolean whiteOut = BattleAftermath.onEndBattle(trainer, data, random, result, canLose);
        if (whiteOut && whiteout != null) {
            whiteout.whiteout();
        }
        canLose = false;
        canRun = true;
        return result;
    }

    @Override
    public BattleResult lastResult() {
        return lastResult;
    }

    @Override
    public void setCanLose(boolean value) {
        this.canLose = value;
    }

    @Override
    public void setCanRun(boolean value) {
        this.canRun = value;
    }

    /**
     * Wild Pokemon: {@code pbGenerateWildPokemon} (PField_Encounters:417-463) -
     * personality value, IVs, ability slot, nature, gender, held item, Pokérus
     * and shininess all come from the plugin's rolls.
     */
    public Pokemon wildPokemon(PbsData.Species species, int level, PbsData data) {
        return pokemon.runtime.pokemon.WildGenerator.generate(data, species, level, trainer,
                bag, obtainMap, fatefulEncounter, random);
    }

    /** The bag, for the Shiny Charm's extra shiny rolls (":436-442"); may be null. */
    private pokemon.runtime.state.Inventory bag;

    public void setBag(pokemon.runtime.state.Inventory value) {
        this.bag = value;
    }

    /** {@code $game_map.map_id} for {@code @obtainMap} (:951). */
    private int obtainMap;

    public void setObtainMap(int value) {
        this.obtainMap = value;
    }

    /**
     * {@code $game_switches[FATEFUL_ENCOUNTER_SWITCH]} (Settings:32) makes the
     * encounter "fateful" (:954-955).
     */
    private boolean fatefulEncounter;

    public void setFatefulEncounter(boolean value) {
        this.fatefulEncounter = value;
    }

    /** Trainer party member from trainers.txt. */
    public Pokemon trainerPokemon(PbsData.TrainerPokemon member, PbsData data) {
        PbsData.Species species = data.species(member.species);
        if (species == null) {
            return null;
        }
        Pokemon pokemon = new Pokemon(species, Math.max(1, member.level), data);
        if (member.ivs != null && member.ivs.length == 6) {
            pokemon.ivs = member.ivs.clone();
        }
        if (member.evs != null && member.evs.length == 6) {
            pokemon.evs = member.evs.clone();
        }
        if (member.nature != null) {
            PbsData.Nature nature = data.nature(member.nature);
            if (nature != null) {
                pokemon.nature = nature;
            }
        }
        applyAbility(pokemon, member.ability, species);
        if (member.item != null) {
            pokemon.item = member.item;
        }
        if (member.gender != null) {
            pokemon.gender = "female".equalsIgnoreCase(member.gender)
                    ? PokemonStats.FEMALE : PokemonStats.MALE;
        }
        pokemon.shiny = member.shiny;
        if (member.moves.size > 0) {
            pokemon.moves.clear();
            for (String moveName : member.moves) {
                PbsData.Move move = data.move(moveName);
                if (move != null) {
                    pokemon.moves.add(new Pokemon.MoveSlot(move));
                }
            }
        }
        pokemon.hp = pokemon.maxHp();
        return pokemon;
    }

    private static void applyAbility(Pokemon pokemon, String ability, PbsData.Species species) {
        if (ability == null) {
            return;
        }
        try {
            int index = Integer.parseInt(ability.trim());
            if (index >= 0 && index < species.abilities.size) {
                pokemon.ability = species.abilities.get(index);
            } else if (index == 2) {
                pokemon.ability = species.hiddenAbility;
            }
        } catch (NumberFormatException notAnIndex) {
            pokemon.ability = ability;
        }
    }
}
