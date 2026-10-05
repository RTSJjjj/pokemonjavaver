package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.util.Random;

/**
 * Stage 3 / P2: a headless, single-battle engine (singles, one active Pokemon
 * per side). It runs whole turns without any UI so the event layer can start a
 * battle and get a result; the battle scene (P2d) drives the same engine one
 * action at a time. Status moves, abilities, items, switching and catching are
 * intentionally out of the v1 scope (P2/P5 add them).
 */
public final class Battle {

    /** Chooses a move for one battler; the default is the simple AI. */
    public interface Controller {
        int chooseMove(Battler user, Battler foe, Array<BattleMove> moves);
    }

    private final PbsData pbs;
    private final Random random;
    private final Controller playerController;
    private final Array<Battler> playerParty = new Array<>();
    private final Array<Battler> foeParty = new Array<>();
    private int turns;

    public Battle(PbsData pbs, Random random, Controller playerController) {
        this.pbs = pbs;
        this.random = random;
        this.playerController = playerController;
    }

    public Battle addPlayer(Pokemon pokemon) {
        if (pokemon != null) {
            playerParty.add(new Battler(pokemon, false));
        }
        return this;
    }

    public Battle addFoe(Pokemon pokemon) {
        if (pokemon != null) {
            foeParty.add(new Battler(pokemon, true));
        }
        return this;
    }

    public int turns() {
        return turns;
    }

    public Array<Battler> playerParty() {
        return playerParty;
    }

    public Array<Battler> foeParty() {
        return foeParty;
    }

    /**
     * Runs the battle until one side is out of able Pokemon or {@code maxTurns}
     * is reached; HP is written back onto the Pokemon either way.
     */
    public BattleResult run(int maxTurns) {
        while (turns < maxTurns) {
            Battler player = active(playerParty);
            Battler foe = active(foeParty);
            if (player == null) {
                return finish(BattleResult.Outcome.LOSS);
            }
            if (foe == null) {
                return finish(BattleResult.Outcome.WIN);
            }
            turns++;
            runTurn(player, foe);
        }
        return finish(BattleResult.Outcome.ESCAPE);
    }

    private void runTurn(Battler player, Battler foe) {
        BattleMove playerMove = pickMove(player, foe, playerController);
        BattleMove foeMove = pickMove(foe, player, null);
        boolean playerFirst = player.speed() > foe.speed()
                || (player.speed() == foe.speed() && random.nextBoolean());
        Battler first = playerFirst ? player : foe;
        Battler second = playerFirst ? foe : player;
        BattleMove firstMove = playerFirst ? playerMove : foeMove;
        BattleMove secondMove = playerFirst ? foeMove : playerMove;

        execute(first, second, firstMove);
        if (active(playerParty) == null || active(foeParty) == null) {
            return;
        }
        if (!second.fainted()) {
            execute(second, first, secondMove);
        }
    }

    private void execute(Battler attacker, Battler defender, BattleMove move) {
        if (attacker.fainted() || defender.fainted() || move == null) {
            return;
        }
        if (move.accuracy() > 0 && random.nextInt(100) >= move.accuracy()) {
            return; // miss
        }
        int damage = DamageCalc.compute(attacker, defender, move, pbs, random);
        if (damage <= 0) {
            return;
        }
        defender.hp = Math.max(0, defender.hp - damage);
        defender.syncHp();
        if (defender.fainted() && !attacker.foe) {
            awardExperience(defender);
        }
    }

    /** Gen 1-4 experience: floor(foe baseExp * foe level / 7), split evenly. */
    private void awardExperience(Battler fainted) {
        int baseExp = fainted.pokemon.species == null ? 0 : fainted.pokemon.species.baseExp;
        if (baseExp <= 0) {
            return;
        }
        int total = (int) Math.floor((double) baseExp * fainted.level() / 7.0);
        Array<Battler> receivers = new Array<>();
        for (Battler battler : playerParty) {
            if (!battler.fainted()) {
                receivers.add(battler);
            }
        }
        if (receivers.size == 0) {
            return;
        }
        int share = Math.max(1, total / receivers.size);
        for (Battler battler : receivers) {
            // The battler's HP is authoritative during the battle; sync it first
            // so a level-up adds only the maximum-HP increase.
            battler.pokemon.hp = battler.hp;
            int oldLevel = battler.pokemon.level;
            boolean leveled = battler.pokemon.gainExperience(share);
            if (leveled) {
                // P3: learn the level's moves and evolve on a reached condition.
                pokemon.runtime.pokemon.PokemonGrowth.afterLevelUp(battler.pokemon, pbs, oldLevel);
            }
            battler.hp = Math.min(battler.pokemon.hp, battler.maxHp());
        }
    }

    private BattleMove pickMove(Battler user, Battler foe, Controller controller) {
        Array<BattleMove> moves = user.moves();
        if (moves.size == 0) {
            return null;
        }
        int index = controller != null
                ? controller.chooseMove(user, foe, moves)
                : defaultAi(user, foe, moves);
        return moves.get(Math.max(0, Math.min(index, moves.size - 1)));
    }

    /**
     * Simple AI: consider only damaging moves, never pick an immune one, and
     * maximise {@code power * typeMod * STAB} (uniform among ties by order).
     */
    private int defaultAi(Battler user, Battler foe, Array<BattleMove> moves) {
        int best = -1;
        double bestScore = -1;
        for (int i = 0; i < moves.size; i++) {
            BattleMove move = moves.get(i);
            if (move.statusMove()) {
                continue;
            }
            float typeMod = pbs == null ? 1f : pbs.effectiveness(move.type(), foe.types());
            if (typeMod <= 0f) {
                continue;
            }
            double stab = user.hasType(move.type()) ? 1.5 : 1.0;
            double score = move.power() * typeMod * stab;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        if (best >= 0) {
            return best;
        }
        for (int i = 0; i < moves.size; i++) {
            if (!moves.get(i).statusMove()) {
                return i;
            }
        }
        return 0;
    }

    private static Battler active(Array<Battler> party) {
        for (Battler battler : party) {
            if (!battler.fainted()) {
                return battler;
            }
        }
        return null;
    }

    private BattleResult finish(BattleResult.Outcome outcome) {
        for (Battler battler : playerParty) {
            battler.syncHp();
        }
        for (Battler battler : foeParty) {
            battler.syncHp();
        }
        return new BattleResult(outcome, turns, null);
    }
}
