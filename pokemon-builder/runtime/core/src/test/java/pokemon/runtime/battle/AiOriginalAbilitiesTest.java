package pokemon.runtime.battle;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The project's original compound abilities are read by the trainer AI like the standard abilities they are made of. */
class AiOriginalAbilitiesTest {

    private static PbsData real() {
        File root = new File("../../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("plugin-src");
        Assumptions.assumeTrue(new File(root, "pbs/moves.json").isFile(), "plugin-src/pbs not found");
        return PbsData.parse(root);
    }

    /** The trainer's Gengar (foe) against the player's Snorlax (target). */
    private Battler setup(PbsData p, String... moves) {
        Battle battle = new Battle(p, new Random(5), (user, target, m) -> 0);
        battle.trainerBattle = true;
        battle.addPlayer(new Pokemon(p.species("SNORLAX"), 40, p));
        Pokemon gengar = new Pokemon(p.species("GENGAR"), 40, p);
        gengar.moves.clear();
        for (String m : moves) gengar.moves.add(new Pokemon.MoveSlot(p.move(m)));
        battle.addFoe(gengar);
        return battle.foe();
    }

    private static int negatives(Battler foe, int slot) {
        AiCtx ctx = new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST);
        return AiNegatives.score(ctx, foe, foe.battle.player(), foe.moveSlot(slot), 100);
    }

    @Test
    @DisplayName("ANGRYBODY (狂暴身躯) absorbs Fairy moves like Sap Sipper absorbs Grass")
    void angryBody() {
        Battler foe = setup(real(), "MOONBLAST");
        int plain = negatives(foe, 0);
        foe.battle.player().ability = "ANGRYBODY";
        assertTrue(negatives(foe, 0) < plain);
    }

    @Test
    @DisplayName("status immunities: PLAYFULHEART, RAINCURTAIN in rain, DEEPSEAFASCINATION burn, DEMONKILLER confusion")
    void statusImmunities() {
        Battler foe = setup(real(), "WILLOWISP");
        Battle battle = foe.battle;
        Battler target = battle.player();
        assertTrue(AiCalc.canBeBurned(battle, target, foe));
        target.ability = "PLAYFULHEART";
        assertFalse(AiCalc.canBeBurned(battle, target, foe));
        assertFalse(AiCalc.canBePoisoned(battle, target, foe));
        assertFalse(AiCalc.canBePutToSleep(battle, target, foe));
        target.ability = "RAINCURTAIN";
        assertTrue(AiCalc.canBeParalyzed(battle, target, foe));
        battle.field.weather = PBWeather.Rain;
        assertFalse(AiCalc.canBeParalyzed(battle, target, foe));
        battle.field.weather = PBWeather.None;
        target.ability = "DEEPSEAFASCINATION";
        assertFalse(AiCalc.canBeBurned(battle, target, foe));
        assertFalse(target.pbCanBurn(foe, false, foe.moveSlot(0)), "the engine blocks the burn too");
        target.ability = "DEMONKILLER";
        assertFalse(AiCalc.canBeConfused(battle, target, foe));
    }

    @Test
    @DisplayName("stat-loss immunities and Moxie-style abilities")
    void statsAndMoxie() {
        Battler foe = setup(real(), "GROWL");
        Battler target = foe.battle.player();
        target.ability = "STEELDYNASTY";
        assertFalse(AiCalc.statCanBeLowered(target, foe, PBStats.ATTACK));
        target.ability = "SPOVERLORD";
        assertFalse(AiCalc.statCanBeLowered(target, foe, PBStats.ATTACK));
        target.ability = "ROSYAEGIS";
        assertFalse(AiCalc.statCanBeLowered(target, foe, PBStats.ACCURACY));
        assertTrue(AiCalc.statCanBeLowered(target, foe, PBStats.ATTACK));
        assertTrue(AiCalc.isMoxie("NETHERDRIVE"));
        assertTrue(AiCalc.isMoxie("GHASTLYWAIL"));
        assertTrue(AiAbilityRatings.of("CLEARHEART") > 0 && AiAbilityRatings.of("ANGRYBODY") > 0);
    }
}
