package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.TrainerState;

import java.util.Random;

/**
 * What happens to the party once a battle is over: {@code PField_Battles:619-658
 * pbAfterBattle} and the non-white-out half of {@code PField_Battles:684-708
 * Events.onEndBattle}.
 *
 * <p>Both ports (the interactive and the headless one) run this when the battle
 * finishes, because the plugin runs it in {@code pbWildBattleCore} /
 * {@code pbTrainerBattleCore} after {@code battle.pbStartBattle} returns, not in
 * the scene.</p>
 */
public final class BattleAftermath {

    /** PField_Battles:736-757 pbPickup's common pool, in the plugin's order. */
    private static final String[] PICKUP_COMMON = {
        "POTION", "ANTIDOTE", "SUPERPOTION", "GREATBALL", "REPEL", "ESCAPEROPE",
        "FULLHEAL", "HYPERPOTION", "ULTRABALL", "REVIVE", "RARECANDY", "SUNSTONE",
        "MOONSTONE", "HEARTSCALE", "FULLRESTORE", "MAXREVIVE", "PPUP", "MAXELIXIR",
    };
    /** PField_Battles:760-772 pbPickup's rare pool. */
    private static final String[] PICKUP_RARE = {
        "HYPERPOTION", "NUGGET", "KINGSROCK", "FULLRESTORE", "ETHER", "IRONBALL",
        "DESTINYKNOT", "ELIXIR", "DESTINYKNOT", "LEFTOVERS", "DESTINYKNOT",
    };
    /** PField_Battles:787 the per-slot probabilities (11 numbers, sum 110). */
    private static final int[] PICKUP_CHANCES = {30, 10, 10, 10, 10, 10, 10, 4, 4, 1, 1};
    /** PField_Battles:737 pbPickup's 10 % chance of finding anything. */
    private static final int PICKUP_CHANCE = 10;

    private BattleAftermath() {
    }

    /**
     * {@code pbAfterBattle} (PField_Battles:619-658).
     *
     * @param result  the finished battle's result
     * @param canLose whether the battle allowed a loss without a white-out
     */
    public static void pbAfterBattle(TrainerState trainer, PbsData pbs, BattleResult result,
                                     boolean canLose) {
        if (trainer == null) {
            return;
        }
        for (Pokemon pkmn : trainer.party.members()) {
            if (pkmn == null) {
                continue;
            }
            // :621 {@code pkmn.statusCount = 0 if pkmn.status==PBStatuses::POISON}
            // ("bad poison becomes regular"): the runtime keeps the toxic counter
            // on the battler (Battler.toxic), which dies with the battle, so the
            // party's poison is already regular by the time we get here.
            pkmn.makeUnmega(pbs);                       // :622
            pkmn.makeUnprimal(pbs);                     // :623
            if (isZacian(pkmn)) {
                // :624-630. The plugin's condition is
                // "pkmn.isSpecies?(:ZACIAN) || pkmn.isSpecies?(:ZAMAZENTA) && @form == 1";
                // @form is nil in this top level method, so a Zamazenta never
                // matches and only a Zacian gets its Iron Head PP topped up.
                for (Pokemon.MoveSlot slot : pkmn.moves) {
                    if (slot == null || slot.move == null || slot.move.internalName == null) {
                        continue;
                    }
                    if ("IRONHEAD".equals(slot.move.internalName) && slot.pp < 5) {
                        slot.pp *= 3;
                    }
                }
            }
            // :631-640 {@code pbCheckEvolutionEx { afterBattleCheck }}: the only
            // afterBattleCheck methods in this project are CriticalHits and
            // DamageDone (Pokemon_Evolution:849-862) and no PBS species uses
            // either, so the block has nothing to do here (and there is no
            // evolution scene yet).
        }
        // :642-649 heals the partner trainer's party; partner battles (and the
        // partner party in battle) do not exist in the runtime yet.
        if (canLose && result != null
                && (result.outcome == BattleResult.Outcome.LOSS)) {   // :650-655
            trainer.healParty();
        }
    }

    /**
     * {@code Events.onEndBattle} (PField_Battles:684-708), minus the white-out:
     * the white-out is the caller's job, because it swaps the screen.
     *
     * @return true when the caller must white the player out (a loss without
     *         {@code canLose}), which is {@code :701-706}
     */
    public static boolean onEndBattle(TrainerState trainer, PbsData pbs, Random random,
                                      BattleResult result, boolean canLose) {
        if (result == null) {
            return false;
        }
        switch (result.outcome) {
            case WIN:
            case CAUGHT:
                // :696-700: every party Pokemon tries Pickup and Honey Gather.
                for (Pokemon pkmn : trainer.party.members()) {
                    pickup(pbs, pkmn, random);
                    honeyGather(pbs, pkmn, random);
                }
                return false;
            case LOSS:
                // :701-706: no white-out when the battle allowed a loss.
                return !canLose;
            default:
                return false;
        }
    }

    /**
     * {@code pbPickup} (PField_Battles:734-799): a 10 % chance after a won or
     * captured battle for a Pokemon with Pickup and no held item.
     */
    static boolean pickup(PbsData pbs, Pokemon pkmn, Random random) {
        if (pbs == null || pkmn == null || pkmn.egg) {
            return false;
        }
        if (!"PICKUP".equals(pkmn.ability)) {           // :736
            return false;
        }
        if (hasItem(pkmn)) {                            // :737
            return false;
        }
        if (random.nextInt(100) >= PICKUP_CHANCE) {     // :738
            return false;
        }
        Array<String> common = dynamicItemList(pbs, PICKUP_COMMON);   // :740-758
        Array<String> rare = dynamicItemList(pbs, PICKUP_RARE);       // :760-772
        if (common.size < 18 || rare.size < 11) {       // :773-774
            return false;
        }
        int level = Math.min(100, pkmn.level);          // :777
        int start = Math.max(0, (level - 1) / 10);      // :778-779
        Array<String> items = new Array<>();
        for (int i = 0; i < 9; i++) {                   // :780-782
            items.add(common.get(start + i));
        }
        for (int i = 0; i < 2; i++) {                   // :783-785
            items.add(rare.get(start + i));
        }
        int chanceSum = 0;                              // :787-789
        for (int chance : PICKUP_CHANCES) {
            chanceSum += chance;
        }
        int roll = random.nextInt(chanceSum);           // :791
        int cumulative = 0;
        for (int i = 0; i < PICKUP_CHANCES.length; i++) {   // :792-798
            cumulative += PICKUP_CHANCES[i];
            if (roll < cumulative) {
                pkmn.item = items.get(i);
                return true;
            }
        }
        return false;
    }

    /**
     * {@code pbHoneyGather} (PField_Battles:801-808):
     * {@code 5+((level-1)/10)*5} % for a Pokemon with Honey Gather.
     */
    static boolean honeyGather(PbsData pbs, Pokemon pkmn, Random random) {
        if (pbs == null || pkmn == null || pkmn.egg) {
            return false;
        }
        if (!"HONEYGATHER".equals(pkmn.ability)) {      // :803
            return false;
        }
        if (hasItem(pkmn)) {                            // :804
            return false;
        }
        if (pbs.item("HONEY") == null) {                // :805
            return false;
        }
        int chance = 5 + ((pkmn.level - 1) / 10) * 5;   // :806
        if (random.nextInt(100) >= chance) {            // :807
            return false;
        }
        pkmn.item = "HONEY";
        return true;
    }

    /** {@code pbDynamicItemList} (PField_Battles:724-731): only items that exist. */
    private static Array<String> dynamicItemList(PbsData pbs, String[] names) {
        Array<String> items = new Array<>();
        for (String name : names) {
            if (pbs.item(name) != null) {
                items.add(name);
            }
        }
        return items;
    }

    /** {@code pkmn.hasItem?} (PokeBattle_Pokemon:622): a non-zero held item. */
    private static boolean hasItem(Pokemon pkmn) {
        return pkmn.item != null && !pkmn.item.isEmpty();
    }

    private static boolean isZacian(Pokemon pkmn) {
        return pkmn.species != null && "ZACIAN".equals(pkmn.species.internalName);
    }
}
