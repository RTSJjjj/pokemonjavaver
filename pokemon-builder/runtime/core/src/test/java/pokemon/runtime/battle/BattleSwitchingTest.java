package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B2: {@code Battle_Action_Switching} (9-332) - which Pokemon may come in, what
 * a switch does to the field, and who {@code pbEORSwitch} sends out.
 */
class BattleSwitchingTest {

    private static Pokemon pokemon(PbsData data, String species, int level) {
        return new Pokemon(data.species(species), level, data);
    }

    private static Battle battle(PbsData data, String player, String foe) {
        return new Battle(data, new Random(3), null)
                .addPlayer(pokemon(data, player, 20))
                .addFoe(pokemon(data, foe, 20));
    }

    @Test
    @DisplayName("B2: pbCanSwitchLax? refuses the active Pokemon and the fainted ones")
    void canSwitchLaxRefusals(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon lead = pokemon(data, "HERO", 20);
        Pokemon second = pokemon(data, "HERO", 20);
        Pokemon down = pokemon(data, "HERO", 20);
        down.hp = 0;
        Pokemon egg = pokemon(data, "HERO", 20);
        egg.egg = true;
        Battle battle = new Battle(data, new Random(1), null)
                .addPlayer(lead).addPlayer(second).addPlayer(down).addPlayer(egg)
                .addFoe(pokemon(data, "FOE", 5));

        // :29-33 the battler that is already on the field.
        assertEquals(lead.name + "已经参与战斗了！", battle.canSwitchLax(0, 0));
        // :24-28 a fainted party member.
        assertEquals(down.name + "已经无法战斗了！", battle.canSwitchLax(0, 2));
        // :14-17 an egg.
        assertEquals("蛋不能进行战斗！", battle.canSwitchLax(0, 3));
        // :10/:12 out of range and negative indices are refused silently.
        assertEquals("", battle.canSwitchLax(0, 9));
        assertNull(battle.canSwitchLax(0, -1));
        assertNull(battle.canSwitchLax(0, 1));
    }

    @Test
    @DisplayName("B2: a switch is the round's action, so it can only be registered once")
    void registerSwitchTakesTheRound(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = new Battle(data, new Random(1), null)
                .addPlayer(pokemon(data, "HERO", 20))
                .addPlayer(pokemon(data, "HERO", 20))
                .addFoe(pokemon(data, "FOE", 5));

        // :123 the active battler cannot be chosen as its own replacement.
        assertFalse(battle.registerSwitch(0, 0));
        assertFalse(battle.choiceIsSwitch(0));
        assertTrue(battle.registerSwitch(0, 1));
        // :124-125 @choices[0] = [:SwitchOut, 1, nil]
        assertTrue(battle.choiceIsSwitch(0));
        assertEquals(1, battle.choiceSwitchParty(0));
        // Battle_Phase_Command:207 skips a battler whose choice is no longer
        // :None, which is what stops a second switch in the same round.
        battle.clearChoice(0);                                     // Battle_Phase_Command:5-11
        assertFalse(battle.choiceIsSwitch(0));
    }

    @Test
    @DisplayName("B2: pbReplace moves the field slot and resets the incoming battler")
    void replaceMovesTheSlot(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.addPlayer(pokemon(data, "HERO", 20));
        Battler incoming = battle.playerParty().get(1);
        incoming.stages[0] = 3;
        incoming.effects.set(PBEffects.Battler.Confusion, 4);
        incoming.turnCount = 7;
        assertEquals(0, battle.player().index);

        assertTrue(battle.replace(0, 1));                          // :313 pbInitialize

        assertSame(incoming, battle.player());
        assertEquals(1, battle.playerFieldIndex());
        assertEquals(0, incoming.index);                           // refreshFieldIndices
        // pbInitEffects(false) (:127-176): stages, confusion and turnCount reset.
        assertEquals(0, incoming.stages[0]);
        assertEquals(0, incoming.effects.intVal(PBEffects.Battler.Confusion));
        assertEquals(0, incoming.turnCount);
        assertFalse(battle.replace(0, 9), "an out-of-range party slot is refused");
    }

    @Test
    @DisplayName("B2: pbAllFainted? counts able Pokemon, so an egg is not able")
    void allFaintedCountsAblePokemon(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon egg = pokemon(data, "HERO", 20);
        egg.egg = true;
        Battle battle = new Battle(data, new Random(1), null)
                .addPlayer(egg)
                .addFoe(pokemon(data, "FOE", 5));
        // PokeBattle_Battle:333-355: pbAbleCount counts pkmn.able? = !egg? && hp>0.
        assertTrue(battle.allFainted(0));
        assertFalse(battle.allFainted(1));
        assertEquals(2, battle.judge(), "an all-egg party is a loss");
    }

    @Test
    @DisplayName("B2: pbJudge returns 5 (draw) when both sides are out")
    void judgeDraw(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon player = pokemon(data, "HERO", 20);
        Pokemon foe = pokemon(data, "FOE", 20);
        Battle battle = new Battle(data, new Random(1), null).addPlayer(player).addFoe(foe);
        battle.player().hp = 0;
        battle.foe().hp = 0;
        assertEquals(5, battle.judge());                            // :590
        assertEquals(BattleResult.Outcome.DRAW, battle.result().outcome);
    }

    @Test
    @DisplayName("B2: pbEORSwitch plans the player's replacement in a wild battle")
    void eorSwitchPlanWild(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.addPlayer(pokemon(data, "HERO", 20));
        battle.player().hp = 0;

        com.badlogic.gdx.utils.Array<Battle.Replacement> plan = battle.eorSwitchPlan(false);
        assertEquals(1, plan.size, "only the player's side has a replacement");
        Battle.Replacement replacement = plan.first();
        assertEquals(0, replacement.idxBattler);
        assertTrue(replacement.playerSide);
        assertFalse(replacement.ownerChooses);
        // :221 the wild-battle form asks "要更换宝可梦吗？".
        assertTrue(replacement.askConfirm);
        assertFalse(replacement.bossBattle);
    }

    @Test
    @DisplayName("B2: a wild Boss (battleRank>1) is not asked about, it just switches")
    void eorSwitchPlanBoss(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon foe = pokemon(data, "FOE", 20);
        foe.battleRank = 3;
        Battle battle = new Battle(data, new Random(1), null)
                .addPlayer(pokemon(data, "HERO", 20))
                .addPlayer(pokemon(data, "HERO", 20))
                .addFoe(foe);
        battle.player().hp = 0;

        Battle.Replacement replacement = battle.eorSwitchPlan(false).first();
        // :211-219 bossBattle is true, so :218-220 sets switch = true without a
        // question and without the run attempt.
        assertTrue(replacement.bossBattle);
        assertFalse(replacement.askConfirm);
    }

    @Test
    @DisplayName("B2: a wild foe is never replaced, a trainer's is")
    void eorSwitchPlanFoeSide(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle wild = battle(data, "HERO", "FOE");
        wild.foe().hp = 0;
        assertEquals(0, wild.eorSwitchPlan(false).size,
                ":179 a wild Pokemon cannot switch out");

        Battle trainer = battle(data, "HERO", "FOE");
        trainer.trainerBattle = true;
        trainer.addFoe(pokemon(data, "FOE", 20));
        trainer.foe().hp = 0;
        com.badlogic.gdx.utils.Array<Battle.Replacement> plan = trainer.eorSwitchPlan(false);
        assertEquals(1, plan.size);
        assertFalse(plan.first().playerSide);
        assertFalse(plan.first().ownerChooses, ":180 the AI picks");
    }

    @Test
    @DisplayName("B2: a trainer battle makes the player pick its replacement (:205-208)")
    void eorSwitchPlanPlayerInTrainerBattle(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.trainerBattle = true;
        battle.addPlayer(pokemon(data, "HERO", 20));
        battle.player().hp = 0;

        Battle.Replacement replacement = battle.eorSwitchPlan(false).first();
        assertTrue(replacement.playerSide);
        assertTrue(replacement.ownerChooses, ":206 pbGetReplacementPokemonIndex");
        assertFalse(replacement.askConfirm);
    }

    @Test
    @DisplayName("B2: a decided battle has no end-of-round switch (:168-169)")
    void eorSwitchPlanStopsWhenDecided(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.player().hp = 0;
        battle.foe().hp = 0;
        assertEquals(0, battle.eorSwitchPlan(false).size);
    }

    @Test
    @DisplayName("B2: pbDefaultChooseNewEnemy scores moves by effectiveness (:188-227)")
    void defaultChooseNewEnemy(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        // SPOOK is GHOST, so SLASH (NORMAL) cannot hurt it; HERO's SLASH scores.
        Battle battle = new Battle(data, new Random(1), null)
                .addPlayer(pokemon(data, "HERO", 20))
                .addFoe(pokemon(data, "SPOOK", 20));
        battle.trainerBattle = true;
        battle.addPlayer(pokemon(data, "SPOOK", 20));
        battle.player().hp = 0;

        // :182 pbCanSwitchLax? filters the party, then :188-227 scores each.
        assertEquals(1, battle.defaultChooseNewEnemy(0));
    }

    @Test
    @DisplayName("B2: the getter routes a trainer's replacement to the AI (:241-253)")
    void getReplacementPokemonIndex(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.trainerBattle = true;
        battle.addPlayer(pokemon(data, "HERO", 20));
        battle.addFoe(pokemon(data, "FOE", 20));
        battle.player().hp = 0;
        battle.foe().hp = 0;

        // :156 the player chooses through the party screen, which the screen owns.
        assertEquals(Battle.OWNER_CHOOSES, battle.getReplacementPokemonIndex(0));
        // :157 a trainer's Pokemon goes straight to the AI.
        assertEquals(1, battle.getReplacementPokemonIndex(1));
    }

    @Test
    @DisplayName("B2: the engine replaces fainted battlers in a headless run")
    void headlessRunReplacesFainted(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = new Battle(data, new Random(9), null)
                .addPlayer(pokemon(data, "FOE", 2))
                .addPlayer(pokemon(data, "HERO", 100))
                .addFoe(pokemon(data, "HERO", 5))
                .addFoe(pokemon(data, "HERO", 5));
        battle.trainerBattle = true;
        BattleResult result = battle.run(200);
        // The weak lead faints, the strong one takes over and wins.
        assertEquals(BattleResult.Outcome.WIN, result.outcome);
        assertEquals(1, battle.playerFieldIndex());
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    /** HERO (strong, NORMAL), FOE (weak, NORMAL), SPOOK (GHOST, immune to NORMAL). */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":3,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[100,100,100,100,100,100],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[1,1,1,1,1,1],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"SPOOK\":{\"id\":3,\"internalName\":\"SPOOK\",\"name\":\"Spook\","
                + "\"types\":[\"GHOST\"],\"baseStats\":[50,50,50,50,50,50],"
                + "\"growthRate\":\"Medium\",\"baseExp\":50,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]}}}");
        write(root, "moves.json", "{\"total\":1,\"moves\":{"
                + "\"SLASH\":{\"id\":1,\"internalName\":\"SLASH\",\"name\":\"Slash\","
                + "\"function\":\"000\",\"power\":70,\"type\":\"NORMAL\",\"category\":\"Physical\",\"accuracy\":100,\"pp\":20,\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(root, "types.json", "{\"total\":2,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"GHOST\":{\"id\":7,\"internalName\":\"GHOST\",\"name\":\"Ghost\",\"immunities\":[\"NORMAL\"]}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }
}
