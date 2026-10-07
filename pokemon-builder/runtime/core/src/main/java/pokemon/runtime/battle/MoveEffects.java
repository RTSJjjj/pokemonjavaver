package pokemon.runtime.battle;

import java.util.Locale;

/**
 * Stage 3 / P2: the move function-code effects, transcribed from the project's
 * {@code PokeBattle_Move_XXX} classes. Covers the function codes the project's
 * moves use most often (status infliction, stat stages, flinch/confusion,
 * recoil/drain, multi-hit, OHKO); unknown codes behave like
 * {@code PokeBattle_UnimplementedMove} (plain damage, status moves fail).
 */
public final class MoveEffects {

    /** PBStats battle stat indices used by the effect tables. */
    public static final int ATK = 0;
    public static final int DEF = 1;
    public static final int SPEED = 2;
    public static final int SPATK = 3;
    public static final int SPDEF = 4;
    public static final int ACCURACY = 5;
    public static final int EVASION = 6;

    /** One move's behaviour. */
    public static final class Effect {
        public String status;         // SLEEP/POISON/TOXIC/BURN/PARALYSIS/FROZEN
        public int[] statUp;          // user stat changes, pairs [stat, stages]
        public int[] statDown;        // user stat drops
        public int[] targetStatDown;  // target stat drops
        public boolean flinch;
        public boolean confuse;
        public boolean nothing;       // "但什么也没有发生！" (Splash)
        public boolean alwaysCrit;    // 0A0
        public boolean alwaysHit;     // 0A5
        public boolean ohko;          // 070
        public int multiMin, multiMax; // 0BD/0C0
        public int recoilNum, recoilDen; // recoil from the damage dealt
        public boolean recoilMaxQuarter; // Struggle: 1/4 max HP
        public boolean drain;         // heal half the damage dealt (0DD)
        public int healHalf;          // heal 1/2 max HP (Recover 0D5/D5)
    }

    private MoveEffects() {
    }

    /** The effect for a function code (hex string like "000"/"0DD"). */
    public static Effect of(String function) {
        Effect e = new Effect();
        if (function == null) {
            return e;
        }
        switch (function.toUpperCase(Locale.ROOT)) {
            case "000": break;
            case "001": e.nothing = true; break;
            case "002": e.recoilMaxQuarter = true; break;
            case "003": e.status = "SLEEP"; break;
            case "005": e.status = "POISON"; break;
            case "006": e.status = "TOXIC"; break;
            case "007": case "008": e.status = "PARALYSIS"; break;
            case "009": e.status = "PARALYSIS"; e.flinch = true; break;
            case "00A": e.status = "BURN"; break;
            case "00B": e.status = "BURN"; e.flinch = true; break;
            case "00C": case "00D": e.status = "FROZEN"; break;
            case "00E": e.status = "FROZEN"; e.flinch = true; break;
            case "00F": case "010": case "011": case "012": e.flinch = true; break;
            case "013": case "014": case "015": e.confuse = true; break;
            case "01C": e.statUp = new int[] {ATK, 1}; break;
            case "01D": case "01E": e.statUp = new int[] {DEF, 1}; break;
            case "01F": e.statUp = new int[] {SPEED, 1}; break;
            case "020": e.statUp = new int[] {SPATK, 1}; break;
            case "021": e.statUp = new int[] {SPDEF, 1}; break;
            case "022": e.statUp = new int[] {EVASION, 1}; break;
            case "023": e.statUp = new int[] {ATK, 0}; break; // Focus Energy (crit)
            case "024": e.statUp = new int[] {ATK, 1, DEF, 1}; break;
            case "025": e.statUp = new int[] {ATK, 1, DEF, 1, ACCURACY, 1}; break;
            case "026": e.statUp = new int[] {ATK, 1, SPEED, 1}; break;
            case "027": case "028": e.statUp = new int[] {ATK, 1, SPATK, 1}; break;
            case "029": e.statUp = new int[] {ATK, 1, ACCURACY, 1}; break;
            case "02A": e.statUp = new int[] {DEF, 1, SPDEF, 1}; break;
            case "02B": e.statUp = new int[] {SPATK, 1, SPDEF, 1, SPEED, 1}; break;
            case "02C": e.statUp = new int[] {SPATK, 1, SPDEF, 1}; break;
            case "02D": e.statUp = new int[] {ATK, 1, DEF, 1, SPATK, 1, SPDEF, 1, SPEED, 1}; break;
            case "02E": e.statUp = new int[] {ATK, 2}; break;
            case "02F": e.statUp = new int[] {DEF, 2}; break;
            case "030": case "031": e.statUp = new int[] {SPEED, 2}; break;
            case "032": e.statUp = new int[] {SPATK, 2}; break;
            case "033": e.statUp = new int[] {SPDEF, 2}; break;
            case "034": e.statUp = new int[] {EVASION, 2}; break;
            case "035": e.statUp = new int[] {ATK, 2, SPATK, 2, SPEED, 2}; e.statDown = new int[] {DEF, 1, SPDEF, 1}; break;
            case "036": e.statUp = new int[] {SPEED, 2, ATK, 1}; break;
            case "038": e.statUp = new int[] {DEF, 3}; break;
            case "039": e.statUp = new int[] {SPATK, 3}; break;
            case "03B": e.statDown = new int[] {ATK, 1, DEF, 1}; break;
            case "03C": e.statDown = new int[] {DEF, 1, SPDEF, 1}; break;
            case "03D": e.statDown = new int[] {SPEED, 1, DEF, 1, SPDEF, 1}; break;
            case "03E": e.statDown = new int[] {SPEED, 1}; break;
            case "03F": e.statDown = new int[] {SPATK, 2}; break;
            case "042": e.targetStatDown = new int[] {ATK, 1}; break;
            case "043": e.targetStatDown = new int[] {DEF, 1}; break;
            case "044": e.targetStatDown = new int[] {SPEED, 1}; break;
            case "045": e.targetStatDown = new int[] {SPATK, 1}; break;
            case "046": e.targetStatDown = new int[] {SPDEF, 1}; break;
            case "047": e.targetStatDown = new int[] {ACCURACY, 1}; break;
            case "048": case "049": case "04B": case "04C":
                e.targetStatDown = new int[] {EVASION, 1}; break;
            case "04D": e.targetStatDown = new int[] {SPEED, 2}; break;
            case "070": e.ohko = true; e.alwaysHit = true; break;
            case "0A0": e.alwaysCrit = true; break;
            case "0A5": e.alwaysHit = true; break;
            case "0BD": e.multiMin = 2; e.multiMax = 2; break;
            case "0C0": e.multiMin = 2; e.multiMax = 5; break;
            case "0D5": e.healHalf = 1; break;
            case "0DD": e.drain = true; break;
            case "0FA": e.recoilNum = 1; e.recoilDen = 4; break;
            case "0FC": e.recoilNum = 1; e.recoilDen = 2; break;
            default: break;
        }
        return e;
    }
}
