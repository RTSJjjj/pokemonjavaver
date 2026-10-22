package pokemon.runtime.battle;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** A trainer's Pokémon holding a Choice item kept choosing status moves (and stayed locked into them). */
class AiChoiceItemTest {

    private static final String[] ITEMS = {"CHOICEBAND", "CHOICESPECS", "CHOICESCARF"};

    private static PbsData real() {
        File root = new File("../../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("plugin-src");
        Assumptions.assumeTrue(new File(root, "pbs/moves.json").isFile(), "plugin-src/pbs not found");
        return PbsData.parse(root);
    }

    /** A trainer's Gengar holding {@code item} with {@code moves}, against the player's Tyranitar. */
    private Battler setup(PbsData p, String item, String... moves) {
        Battle battle = new Battle(p, new Random(5), (user, target, m) -> 0);
        battle.trainerBattle = true;
        battle.addPlayer(new Pokemon(p.species("TYRANITAR"), 40, p));
        Pokemon gengar = new Pokemon(p.species("GENGAR"), 40, p);
        gengar.moves.clear();
        for (String m : moves) gengar.moves.add(new Pokemon.MoveSlot(p.move(m)));
        battle.addFoe(gengar);
        battle.foe().item = item;
        return battle.foe();
    }

    private static int[] scores(Battler foe) {
        AiCtx ctx = AiMaster.prepare(foe.battle, foe, foe.battle.player(), new Random(1));
        return AiMaster.scoreMoves(ctx, foe.battle, foe, foe.battle.player());
    }

    @Test
    @DisplayName("a Choice item holder picks its attack even when the attack scores below the status moves")
    void choiceHolderAttacks() {
        PbsData p = real();
        // Focus Punch is penalised by the scripts (98) while the status moves sit at 100: without a Choice item they win.
        int[] plain = scores(setup(p, "", "WILLOWISP", "TAUNT", "CONFUSERAY", "FOCUSPUNCH"));
        assertTrue(Math.max(plain[0], Math.max(plain[1], plain[2])) > plain[3], Arrays.toString(plain));
        for (String item : ITEMS) {
            for (String attack : new String[] {"FOCUSPUNCH", "SHADOWBALL"}) {
                Battler foe = setup(p, item, "WILLOWISP", "TAUNT", "CONFUSERAY", attack);
                for (int seed = 0; seed < 40; seed++) {
                    int choice = AiMaster.chooseMove(foe.battle, foe, new Random(seed));
                    assertEquals(3, choice, item + "/" + attack + " seed " + seed + " chose " + foe.moveSlot(choice).internalName()
                            + " " + Arrays.toString(scores(foe)));
                }
            }
        }
    }

    @Test
    @DisplayName("Trick is not capped for a Choice item holder (it hands the item away)")
    void choiceHolderMayTrick() {
        PbsData p = real();
        Battler foe = setup(p, "CHOICESPECS", "TRICK", "TAUNT", "CONFUSERAY", "FOCUSPUNCH");
        int[] score = {120, 100, 100, 98};
        AiMaster.preferAttacksWhenChoiceLocked(foe, score);
        assertArrayEquals(new int[] {120, 97, 97, 98}, score);
    }

    @Test
    @DisplayName("MOERAE (莫伊莱) bounces status moves like Magic Bounce")
    void moeraeIsMagicBounce() {
        PbsData p = real();
        Battler foe = setup(p, "", "WILLOWISP", "TAUNT", "CONFUSERAY", "SHADOWBALL");
        Battler target = foe.battle.player();
        AiCtx ctx = new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST);
        int plain = AiNegatives.score(ctx, foe, target, foe.moveSlot(1), 100);
        target.ability = "MOERAE";
        int bounced = AiNegatives.score(ctx, foe, target, foe.moveSlot(1), 100);
        assertTrue(bounced < plain, "plain " + plain + " vs MOERAE " + bounced);
    }
}
