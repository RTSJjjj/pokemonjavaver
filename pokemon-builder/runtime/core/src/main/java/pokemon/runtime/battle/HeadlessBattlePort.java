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
        for (Pokemon member : trainer.party.members()) {
            battle.addPlayer(member);
        }
        for (Pokemon foe : foes) {
            battle.addFoe(foe);
        }
        BattleResult result = battle.run(MAX_TURNS);
        if (result.outcome == BattleResult.Outcome.LOSS && whiteout != null) {
            whiteout.whiteout();
        }
        return result;
    }

    /** Wild Pokemon: random IVs and nature, like Essentials' generator. */
    Pokemon wildPokemon(PbsData.Species species, int level, PbsData data) {
        Pokemon pokemon = new Pokemon(species, level, data);
        int[] ivs = new int[6];
        for (int i = 0; i < ivs.length; i++) {
            ivs[i] = random.nextInt(32);
        }
        pokemon.ivs = ivs;
        pokemon.hp = pokemon.maxHp();
        if (data.natures.size > 0) {
            pokemon.nature = data.natures.get(random.nextInt(data.natures.size));
        }
        return pokemon;
    }

    /** Trainer party member from trainers.txt. */
    Pokemon trainerPokemon(PbsData.TrainerPokemon member, PbsData data) {
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
