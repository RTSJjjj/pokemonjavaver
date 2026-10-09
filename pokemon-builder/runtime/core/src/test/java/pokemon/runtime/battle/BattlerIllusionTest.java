package pokemon.runtime.battle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(tempDir, "pokemon.json", "{\"total\":3,\"byId\":{},\"species\":{"
                + "\"ZOROARK\":{\"id\":571,\"internalName\":\"ZOROARK\",\"name\":\"索罗亚克\",\"types\":[\"DARK\"],"
                + "\"baseStats\":[60,105,60,105,120,60],\"rareness\":45,\"genderRate\":\"Female50Percent\","
                + "\"abilities\":[\"ILLUSION\"],\"evolutions\":[]},"
                + "\"BULBASAUR\":{\"id\":1,\"internalName\":\"BULBASAUR\",\"name\":\"妙蛙种子\",\"types\":[\"GRASS\"],"
                + "\"baseStats\":[45,49,49,45,65,65],\"rareness\":45,\"genderRate\":\"Female50Percent\","
                + "\"abilities\":[\"OVERGROW\"],\"evolutions\":[]},"
                + "\"PIKACHU\":{\"id\":25,\"internalName\":\"PIKACHU\",\"name\":\"皮卡丘\",\"types\":[\"ELECTRIC\"],"
                + "\"baseStats\":[35,55,40,90,50,50],\"rareness\":45,\"genderRate\":\"Female50Percent\","
                + "\"abilities\":[\"STATIC\"],\"evolutions\":[]}}}");
        write(tempDir, "moves.json", "{\"total\":1,\"moves\":{\"TACKLE\":{\"id\":33,\"internalName\":\"TACKLE\","
                + "\"name\":\"撞击\",\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"effectChance\":0,\"target\":\"NearOther\",\"priority\":0,\"flags\":\"\"}}}");
        pbs = PbsData.parse(tempDir.toFile());
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
