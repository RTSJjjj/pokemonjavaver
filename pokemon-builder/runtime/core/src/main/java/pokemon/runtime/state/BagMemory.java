package pokemon.runtime.state;

/**
 * Where the bag was left: {@code PokemonBag#lastpocket} and the per-pocket cursor
 * {@code @choices} (195_PItem_Bag:5, :16-21, :63-78 getChoice/setChoice). The
 * field bag keeps one in the save; the battle bag keeps its own for the whole
 * battle (Scene_Commands:239-243 {@code @bagLastPocket} / {@code @bagChoices}).
 */
public final class BagMemory {
    /** Pockets are 1..9; index 0 is the blank pocket. */
    public static final int POCKETS = 10;

    public int lastPocket = 1;                       // :16 @lastpocket = 1
    private final int[] choices = new int[POCKETS];  // :21 @choices[i] = 0

    public int choice(int pocket) {
        return pocket < 0 || pocket >= POCKETS ? 0 : choices[pocket];
    }

    public void choice(int pocket, int value) {
        if (pocket >= 0 && pocket < POCKETS) {
            choices[pocket] = Math.max(0, value);
        }
    }

    public int[] choices() {
        return choices.clone();
    }

    public BagMemory copy() {
        BagMemory copy = new BagMemory();
        copy.lastPocket = lastPocket;
        System.arraycopy(choices, 0, copy.choices, 0, POCKETS);
        return copy;
    }
}
