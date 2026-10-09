package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.pokemon.WildGenerator;

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
            Pokemon built = trainerPokemon(trainerData, member, data);
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
        battle.numBadges = trainer.badges.size();
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

    /** Trainer party member from trainers.txt, without its trainer (no trainer type: the neutral defaults). */
    public Pokemon trainerPokemon(PbsData.TrainerPokemon member, PbsData data) {
        return trainerPokemon(null, member, data);
    }

    /**
     * {@code pbLoadTrainer}'s party loop (186_PTrainer_NPCTrainers:80-127): a Pokemon from {@code pbNewPkmn} (random
     * personal id) whose fields trainers.txt leaves out take the plugin's defaults - ability index 0, the trainer's
     * gender, nature {@code (species + trainertype) % 25}, IVs {@code min(level/2, 31)}, EVs {@code min(level*3/2, 85)}.
     */
    public Pokemon trainerPokemon(PbsData.TrainerData trainerData, PbsData.TrainerPokemon member, PbsData data) {
        PbsData.Species species = data.species(member.species);
        if (species == null) {
            return null;
        }
        int level = Math.max(1, member.level);
        Pokemon pokemon = WildGenerator.pbNewPkmn(data, species, level, null, 0, random);   // :83 pbNewPkmn(species,level,opponent,false)
        PbsData.TrainerType type = trainerData == null || trainerData.type == null ? null : data.trainerTypes.get(trainerData.type);
        if (member.item != null) {                                                          // :88
            pokemon.item = member.item;
        }
        if (member.moves.size > 0) {                                                        // :89-95 pbLearnMove each, else resetMoves
            pokemon.moves.clear();
            for (String moveName : member.moves) {
                PbsData.Move move = data.move(moveName);
                if (move != null) {
                    pokemon.moves.add(new Pokemon.MoveSlot(move));
                }
            }
        } else {
            pokemon.resetMoves(data);
        }
        applyAbility(pokemon, member.ability == null ? "0" : member.ability, species);      // :96 setAbility(poke[TPABILITY] || 0)
        if (member.gender != null) {                                                        // :97-98 setGender
            pokemon.gender = "female".equalsIgnoreCase(member.gender) ? PokemonStats.FEMALE : PokemonStats.MALE;
        } else {
            pokemon.gender = trainerIsFemale(type) ? PokemonStats.FEMALE : PokemonStats.MALE;
        }
        pokemon.shiny = member.shiny;                                                       // :99 makeShiny / makeNotShiny
        PbsData.Nature nature = member.nature == null ? null : data.nature(member.nature);  // :100-101 setNature
        if (nature == null && data.natures.size > 0) {
            int trainerTypeId = type == null ? 0 : type.id;
            nature = data.natures.get(Math.floorMod(species.id + trainerTypeId, 25) % data.natures.size);
        }
        if (nature != null) {
            pokemon.nature = nature;
        }
        pokemon.ivs = new int[6];                                                           // :102-113
        pokemon.evs = new int[6];
        for (int i = 0; i < 6; i++) {
            if (member.ivs != null && member.ivs.length > 0) {
                pokemon.ivs[i] = i < member.ivs.length ? member.ivs[i] : member.ivs[0];
            } else {
                pokemon.ivs[i] = Math.min(level / 2, 31);
            }
            if (member.evs != null && member.evs.length > 0) {
                pokemon.evs[i] = i < member.evs.length ? member.evs[i] : member.evs[0];
            } else {
                pokemon.evs[i] = Math.min(level * 3 / 2, 510 / 6);
            }
        }
        if (member.happiness >= 0) {                                                        // :114
            pokemon.happiness = member.happiness;
        }
        pokemon.hp = pokemon.maxHp();                                                       // :125 calcStats
        return pokemon;
    }

    /** {@code opponent.female?} (185_PokeBattle_Trainer:107-116): the trainer type's gender column says Female. */
    private static boolean trainerIsFemale(PbsData.TrainerType type) {
        return type != null && type.fields != null && type.fields.size > 2 && "Female".equalsIgnoreCase(type.fields.get(2));
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
