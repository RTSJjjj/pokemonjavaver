package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;

/**
 * Stage 3 / P2: the live state of one Pokemon in a battle (singles). Holds the
 * current HP and stat stages; the Pokemon itself stays the source of truth for
 * level, IVs and moves, so the party / save keep working outside a battle.
 */
public final class Battler {

    /** Essentials stat stages: -6..6, index 0 (neutral) maps to 2/2. */
    private static final int[] STAGE_MUL = {2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] STAGE_DIV = {8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2};

    public final Pokemon pokemon;
    public final boolean foe;
    public int hp;
    /** Battle stat stages for ATTACK, DEFENSE, SPEED, SPATK, SPDEF (indices 0..4). */
    public final int[] stages = new int[5];

    public Battler(Pokemon pokemon, boolean foe) {
        this.pokemon = pokemon;
        this.foe = foe;
        this.hp = Math.max(0, pokemon.hp);
    }

    public int level() {
        return pokemon.level;
    }

    public int maxHp() {
        return pokemon.maxHp();
    }

    public boolean fainted() {
        return hp <= 0;
    }

    public String name() {
        return pokemon.name != null && !pokemon.name.isEmpty()
                ? pokemon.name : pokemon.species.name;
    }

    public Array<String> types() {
        return pokemon.types();
    }

    public boolean hasType(String type) {
        return type != null && pokemon.types().contains(type, false);
    }

    public static float stageMultiplier(int stage) {
        int index = Math.max(-6, Math.min(6, stage)) + 6;
        return (float) STAGE_MUL[index] / STAGE_DIV[index];
    }

    public int attack() {
        return scale(pokemon.attack(), stages[0]);
    }

    public int defense() {
        return scale(pokemon.defense(), stages[1]);
    }

    public int speed() {
        return scale(pokemon.speed(), stages[2]);
    }

    public int spAtk() {
        return scale(pokemon.spAtk(), stages[3]);
    }

    public int spDef() {
        return scale(pokemon.spDef(), stages[4]);
    }

    private static int scale(int base, int stage) {
        return (int) Math.floor(base * stageMultiplier(stage));
    }

    /** The usable moves of this battler; empty when it knows none. */
    public Array<BattleMove> moves() {
        Array<BattleMove> out = new Array<>();
        for (int i = 0; i < pokemon.moves.size; i++) {
            Pokemon.MoveSlot slot = pokemon.moves.get(i);
            if (slot.move != null) {
                out.add(new BattleMove(slot.move));
            }
        }
        return out;
    }

    /** Copies the battle HP back onto the Pokemon (fainted -> 0). */
    public void syncHp() {
        pokemon.hp = Math.max(0, hp);
    }

    /** Number of moves with PP left is not tracked yet (P2 keeps PP infinite). */
    public boolean canFight() {
        return !fainted() && moves().size > 0;
    }
}
