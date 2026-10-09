package pokemon.runtime.battle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Illusion: 110_Battler_Initialize:213-219, 109_PokeBattle_Battler:166-196 (display-only properties), BattleHandlers_Abilities:1603-1613. */
class BattlerIllusionTest {

    @TempDir
    Path tempDir;
    private PbsData pbs;
    private Battle battle;
    private Pokemon zoroark;
    private Pokemon bulbasaur;

    @BeforeEach
    void setUp() throws Exception {
        pbs = new BattleFixture(tempDir)
                .species("ZOROARK", 571, "索罗亚克", "DARK", new int[] {60, 105, 60, 105, 120, 60}, "ILLUSION")
                .species("BULBASAUR", 1, "妙蛙种子", "GRASS", new int[] {45, 49, 49, 45, 65, 65}, "OVERGROW")
                .species("PIKACHU", 25, "皮卡丘", "ELECTRIC", new int[] {35, 55, 40, 90, 50, 50}, "STATIC")
                .move("TACKLE", 33, "NORMAL", 35)
                .build();
        battle = new Battle(pbs, new Random(7), (user, foe, moves) -> 0);
        zoroark = new Pokemon(pbs.species("ZOROARK"), 50, pbs);
        zoroark.name = "小狐";
        bulbasaur = new Pokemon(pbs.species("BULBASAUR"), 50, pbs);
        bulbasaur.name = "小绿";
    }

    @Test
    @DisplayName("a Zoroark in front of a healthy Pokemon looks like the last Pokemon of the team")
    void looksLikeTheLastOfTheTeam() {
        battle.addPlayer(zoroark);
        battle.addPlayer(bulbasaur);
        battle.addFoe(new Pokemon(pbs.species("PIKACHU"), 50, pbs));
        battle.pbSetUpIllusions();                                           // what the ports do once both parties are complete
        Battler lead = battle.battlerAt(0);
        assertSame(bulbasaur, lead.illusion());
        assertEquals("小绿", lead.name());
        assertEquals("BULBASAUR", lead.displaySpecies().internalName);
        assertSame(bulbasaur, lead.visiblePokemon());
        assertSame(zoroark, lead.pokemon);                                   // its own data is untouched
        assertTrue(lead.pbThis().endsWith("小绿"));
    }

    @Test
    @DisplayName("alone, or when the last Pokemon is itself, there is no Illusion")
    void noIllusionWithoutAnotherPokemon() {
        battle.addPlayer(zoroark);
        battle.addFoe(new Pokemon(pbs.species("PIKACHU"), 50, pbs));
        battle.pbSetUpIllusions();                                           // what the ports do once both parties are complete
        Battler lead = battle.battlerAt(0);
        assertNull(lead.illusion());
        assertEquals("小狐", lead.name());
        assertSame(zoroark, lead.visiblePokemon());
    }

    @Test
    @DisplayName("a hit breaks the Illusion: the sprite change is queued and the line is shown")
    void aHitBreaksIt() {
        battle.addPlayer(zoroark);
        battle.addPlayer(bulbasaur);
        Pokemon pika = new Pokemon(pbs.species("PIKACHU"), 50, pbs);
        battle.addFoe(pika);
        battle.pbSetUpIllusions();                                           // what the ports do once both parties are complete
        Battler lead = battle.battlerAt(0);
        Battler foe = battle.battlerAt(1);
        int before = battle.roundEvents.size;
        BattleHandlers.triggerTargetAbilityOnHit("ILLUSION", foe, lead, new BattleMove(pbs.move("TACKLE")), battle);
        assertNull(lead.illusion());
        assertEquals("小狐", lead.name());
        boolean looked = false;
        for (int i = before; i < battle.roundEvents.size; i++) {
            Battle.RoundEvent event = battle.roundEvents.get(i);
            if (event.kind == Battle.RoundEvent.Kind.CHANGE_LOOK) {
                looked = true;
                assertSame(bulbasaur, event.oldLook);
                assertEquals(0, event.idxBattler);
            }
        }
        assertTrue(looked, "the sprite change is queued");
    }

    @Test
    @DisplayName("losing the ability (Skill Swap, Gastro Acid ...) breaks it too")
    void losingTheAbilityBreaksIt() {
        battle.addPlayer(zoroark);
        battle.addPlayer(bulbasaur);
        battle.addFoe(new Pokemon(pbs.species("PIKACHU"), 50, pbs));
        battle.pbSetUpIllusions();                                           // what the ports do once both parties are complete
        Battler lead = battle.battlerAt(0);
        lead.ability = "BLAZE";
        lead.pbOnAbilityChanged("ILLUSION");
        assertNull(lead.illusion());
        assertEquals("小狐", lead.name());
    }
}
