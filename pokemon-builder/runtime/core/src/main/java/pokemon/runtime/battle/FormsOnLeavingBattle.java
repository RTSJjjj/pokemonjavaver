package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

/**
 * {@code PokeBattle_RealBattlePeer#pbOnLeavingBattle} (198_Pokemon_Forms:63-68) with the {@code getFormOnLeavingBattle}
 * entries of {@code MultipleForms} (198_Pokemon_Forms, one per species): the form a Pokemon goes back to when it leaves
 * the field (switched out or fainted) or when the battle ends. It was never transcribed, so Castform, Aegislash,
 * Darmanitan, Palafin, Shiny Garchomp ... kept the battle form afterwards.
 */
final class FormsOnLeavingBattle {

    private FormsOnLeavingBattle() {
    }

    /** The form {@code pkmn} goes back to, or -1 for "no change" (Ruby's nil). */
    static int formOnLeaving(Pokemon pkmn, boolean fainted, boolean usedInBattle, boolean endBattle, int environment) {
        if (pkmn == null || pkmn.species == null) {
            return -1;
        }
        int form = pkmn.formIndex();
        boolean faintedOrEnd = fainted || endBattle;
        switch (pkmn.species.internalName) {
            case "CASTFORM": case "CHERRIM": case "MELOETTA": case "AEGISLASH": case "XERNEAS":
            case "WISHIWASHI": case "CRAMORANT": case "ETERNATUS":
                return 0;
            case "BURMY":
                if (!endBattle || !usedInBattle) return -1;
                if (environment == PBEnvironment.Rock || environment == PBEnvironment.Sand || environment == PBEnvironment.Cave) {
                    return 1;                                              // Sandy Cloak
                }
                return environment == PBEnvironment.None ? 2 : 0;          // Trash Cloak / Plant Cloak
            case "ARCEUS":
                return "LEGENDPLATE".equals(pkmn.item) ? 0 : -1;
            case "DARMANITAN":
                if (form == 2) return 0;
                return form == 3 ? 1 : -1;
            case "KYUREM":
                return form >= 3 ? form - 2 : -1;                          // fused forms stop glowing
            case "GRENINJA":
                return form == 2 && faintedOrEnd ? 1 : -1;
            case "ZYGARDE":
                return form >= 4 && form <= 5 && faintedOrEnd ? form - 2 : -1;
            case "MINIOR":
                return form < 7 ? form + 7 : -1;
            case "NECROZMA":
                return form >= 3 && faintedOrEnd ? form - 2 : -1;
            case "OGERPON":
                return form >= 4 ? form - 4 : -1;
            case "PALAFIN": case "SHINYGARCHOMP":
                return endBattle ? 0 : -1;
            case "TERAPAGOS":
                return form > 0 && endBattle ? 0 : -1;
            case "MIMIKYU": case "EISCUE": case "MORPEKO": case "ROSEDRAGON": case "KABLIT": case "FLAMBLOOM":
            case "BLAZEPHEX": case "SOLGALEO": case "LUNALA":
                return faintedOrEnd ? 0 : -1;
            default:
                return -1;
        }
    }

    /** {@code pbOnLeavingBattle(battle,pkmn,usedInBattle,endBattle)}: set the form, keep the HP inside the new maximum. */
    static void apply(PbsData pbs, Pokemon pkmn, boolean fainted, boolean usedInBattle, boolean endBattle, int environment) {
        int f = formOnLeaving(pkmn, fainted, usedInBattle, endBattle, environment);
        if (f >= 0 && pkmn.formIndex() != f && pbs != null) {
            pkmn.setForm(pbs, f);                                           // :66 pkmn.form = f if f && pkmn.form!=f
        }
        if (pkmn.hp > pkmn.maxHp()) {
            pkmn.hp = pkmn.maxHp();                                         // :67
        }
    }
}
