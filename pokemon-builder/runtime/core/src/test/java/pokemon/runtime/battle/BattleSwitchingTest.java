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

    /** {@code @scene}: records every call and answers from a script. */
    static final class ScriptedScene implements Battle.Scene {
        final java.util.List<String> log = new java.util.ArrayList<>();
        final java.util.ArrayDeque<Integer> parties = new java.util.ArrayDeque<>();
        final java.util.ArrayDeque<Boolean> confirms = new java.util.ArrayDeque<>();

        @Override public void pbPartyScreen(int idxBattler, boolean canCancel, java.util.function.IntFunction<String> block) {
            log.add("party:" + idxBattler + (canCancel ? ":cancel" : ""));
            int pick = parties.isEmpty() ? -1 : parties.poll();
            if (pick >= 0) {
                assertNull(block.apply(pick), "the party screen accepted a Pokemon the block refuses");
            }
        }

        @Override public boolean pbDisplayConfirmMessage(String msg) {
            log.add("confirm:" + msg);
            return confirms.isEmpty() || confirms.poll();
        }

        @Override public void pbRecall(int idxBattler) {
            log.add("recall:" + idxBattler);
        }

        @Override public void pbShowPartyLineup(int side) {
            log.add("lineup:" + side);
        }

        @Override public void pbSendOutBattlers(int[] idxBattlers, boolean startBattle) {
            log.add("sendout:" + idxBattlers[0]);
        }
    }

    private static final String NL = String.valueOf((char) 10);

    @Test
    @DisplayName("pbEORSwitch: the player's fainted Pokemon in a wild battle - question, party screen, lineup, line, send-out (:209-231, :256-262)")
    void eorSwitchWild(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.addPlayer(pokemon(data, "HERO", 20));
        Battler second = battle.playerParty().get(1);
        battle.player().hp = 0;
        ScriptedScene scene = new ScriptedScene();
        scene.parties.add(1);
        battle.scene = scene;

        battle.pbEORSwitch(false);

        // :221 the question, :228 pbGetReplacementPokemonIndex (lax), :257 no recall of a fainted one, :259, :326
        assertEquals(java.util.Arrays.asList("confirm:要更换宝可梦吗？", "party:0", "lineup:0", "sendout:0"), scene.log);
        assertSame(second, battle.player());
        // :260 pbMessagesOnReplace: the opposing Pokemon is at full HP
        assertEquals("加油啊！" + NL + second.name(), battle.roundMessages.peek());
    }

    @Test
    @DisplayName("pbEORSwitch: answering no tries to run, and a failed run still switches (:221-224)")
    void eorSwitchWildRefusedRun(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.addPlayer(pokemon(data, "HERO", 20));
        battle.setCanRun(false);                                   // Battle_Action_Running:74-76 returns 0
        battle.player().hp = 0;
        ScriptedScene scene = new ScriptedScene();
        scene.confirms.add(false);
        scene.parties.add(1);
        battle.scene = scene;

        battle.pbEORSwitch(false);

        assertEquals(1, battle.playerFieldIndex(), ":222 pbRun(...)<=0 -> :227 switch");
        assertTrue(battle.roundMessages.contains("逃跑失败了！", false));
        assertEquals(0, battle.decision);
    }

    @Test
    @DisplayName("pbEORSwitch: answering no and getting away ends the battle as an escape (Battle_Action_Running:143-153)")
    void eorSwitchWildRun(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.addPlayer(pokemon(data, "HERO", 20));
        battle.player().hp = 0;
        // make the fastest opponent slower than the runner so the rate is 256 (:143-145)
        battle.foe().pokemon.level = 1;
        battle.player().pokemon.level = 100;
        ScriptedScene scene = new ScriptedScene();
        scene.confirms.add(false);
        battle.scene = scene;

        battle.pbEORSwitch(false);

        assertEquals(3, battle.decision, ":152 @decision = 3");
        assertEquals(BattleResult.Outcome.ESCAPE, battle.result().outcome);
        assertEquals(0, battle.playerFieldIndex(), "nothing was sent out");
    }

    @Test
    @DisplayName("pbEORSwitch: a wild Boss (battleRank>1) is not asked about, it just switches (:218-220)")
    void eorSwitchBoss(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon foe = pokemon(data, "FOE", 20);
        foe.battleRank = 3;
        Battle battle = new Battle(data, new Random(1), null)
                .addPlayer(pokemon(data, "HERO", 20))
                .addPlayer(pokemon(data, "HERO", 20))
                .addFoe(foe);
        battle.player().hp = 0;
        ScriptedScene scene = new ScriptedScene();
        scene.parties.add(1);
        battle.scene = scene;

        battle.pbEORSwitch(false);

        assertEquals(java.util.Arrays.asList("party:0", "lineup:0", "sendout:0"), scene.log);
    }

    @Test
    @DisplayName("pbEORSwitch: a wild foe is never replaced, a trainer's is by the AI (:179-180, :203)")
    void eorSwitchFoeSide(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle wild = battle(data, "HERO", "FOE");
        wild.addFoe(pokemon(data, "FOE", 20));
        wild.foe().hp = 0;
        ScriptedScene wildScene = new ScriptedScene();
        wild.scene = wildScene;
        wild.pbEORSwitch(false);
        assertEquals(0, wild.foeFieldIndex(), ":179 a wild Pokemon cannot switch out");
        assertTrue(wildScene.log.isEmpty());

        Battle trainer = battle(data, "HERO", "FOE");
        trainer.trainerBattle = true;
        trainer.switchStyle = false;
        trainer.opponentName = "Rival";
        trainer.addFoe(pokemon(data, "FOE", 20));
        Battler next = trainer.foeParty().get(1);
        trainer.foe().hp = 0;
        ScriptedScene scene = new ScriptedScene();
        trainer.scene = scene;
        trainer.pbEORSwitch(false);
        assertSame(next, trainer.foe());
        assertEquals(java.util.Arrays.asList("lineup:1", "sendout:1"), scene.log, ":180 the AI picks, no question without the Switch style");
        assertEquals("Rival派出了" + NL + next.name() + "！", trainer.roundMessages.peek());
    }

    @Test
    @DisplayName("pbEORSwitch: the Switch style offers the player a change before the opponent's next Pokemon (:185-202)")
    void eorSwitchOffer(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.trainerBattle = true;
        battle.switchStyle = true;
        battle.opponentName = "Rival";
        battle.addPlayer(pokemon(data, "HERO", 20));
        battle.addFoe(pokemon(data, "FOE", 20));
        Battler mine = battle.playerParty().get(1);
        Battler theirs = battle.foeParty().get(1);
        battle.foe().hp = 0;
        ScriptedScene scene = new ScriptedScene();
        scene.confirms.add(true);
        scene.parties.add(1);
        battle.scene = scene;

        battle.pbEORSwitch(false);

        assertEquals("confirm:Rival将要派出" + theirs.name() + "。" + NL + "要更换宝可梦吗？", scene.log.get(0));
        assertEquals("party:0:cancel", scene.log.get(1), ":195 pbSwitchInBetween(0,false,true)");
        // :198 the player's Pokemon is recalled and replaced first, then the opponent's (:203)
        assertEquals(java.util.Arrays.asList("recall:0", "lineup:0", "sendout:0", "lineup:1", "sendout:1"),
                scene.log.subList(2, scene.log.size()));
        assertSame(mine, battle.player());
        assertSame(theirs, battle.foe());
    }

    @Test
    @DisplayName("pbEORSwitch: cancelling the offered change leaves the player's Pokemon (:196)")
    void eorSwitchOfferCancelled(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.trainerBattle = true;
        battle.switchStyle = true;
        battle.addPlayer(pokemon(data, "HERO", 20));
        battle.addFoe(pokemon(data, "FOE", 20));
        Battler lead = battle.player();
        battle.foe().hp = 0;
        ScriptedScene scene = new ScriptedScene();
        scene.confirms.add(true);
        scene.parties.add(-1);
        battle.scene = scene;

        battle.pbEORSwitch(false);

        assertSame(lead, battle.player());
        assertEquals(1, battle.foeFieldIndex());
    }

    @Test
    @DisplayName("pbEORSwitch: a trainer battle makes the player pick its replacement without a question (:205-208)")
    void eorSwitchPlayerInTrainerBattle(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.trainerBattle = true;
        battle.addPlayer(pokemon(data, "HERO", 20));
        battle.player().hp = 0;
        ScriptedScene scene = new ScriptedScene();
        scene.parties.add(1);
        battle.scene = scene;

        battle.pbEORSwitch(false);

        assertEquals(java.util.Arrays.asList("party:0", "lineup:0", "sendout:0"), scene.log);
        assertEquals(1, battle.playerFieldIndex());
    }

    @Test
    @DisplayName("pbEORSwitch: a decided battle has no end-of-round switch (:168-169)")
    void eorSwitchStopsWhenDecided(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.player().hp = 0;
        battle.foe().hp = 0;
        ScriptedScene scene = new ScriptedScene();
        battle.scene = scene;
        battle.pbEORSwitch(false);
        assertTrue(scene.log.isEmpty());
        assertEquals(5, battle.decision);
    }

    @Test
    @DisplayName("pbCanSwitch?: trapping effects and trapping abilities refuse the switch (:69-107)")
    void trappedCannotSwitch(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.addPlayer(pokemon(data, "HERO", 20));
        assertTrue(battle.pbCanSwitch(0, 1));
        battle.player().effects.set(PBEffects.Battler.MeanLook, 1);
        assertEquals(battle.player().pbThis() + "无法离开战斗！", battle.canSwitch(0, 1));   // :82-88
        battle.player().effects.set(PBEffects.Battler.MeanLook, -1);
        battle.player().effects.set(PBEffects.Battler.Ingrain, true);
        assertFalse(battle.pbCanSwitch(0, 1));                                              // :83
        battle.player().hp = 0;
        assertTrue(battle.pbCanSwitch(0, 1), ":54 a fainted battler can always be replaced");
    }

    @Test
    @DisplayName("pbReplace with Baton Pass keeps the effects and stat stages (Battler_Initialize:66-70, :113-124)")
    void batonPassKeepsEffects(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.addPlayer(pokemon(data, "HERO", 20));
        battle.player().stages[0] = 2;
        battle.player().effects.set(PBEffects.Battler.Ingrain, true);
        battle.player().effects.set(PBEffects.Battler.LockOn, 5);
        Battler incoming = battle.playerParty().get(1);

        battle.pbRecallAndReplace(0, 1, false, true);

        assertSame(incoming, battle.player());
        assertEquals(2, incoming.stages[0]);
        assertTrue(incoming.effects.truthy(PBEffects.Battler.Ingrain));
        assertEquals(2, incoming.effects.intVal(PBEffects.Battler.LockOn), ":117 LockOn is reapplied as 2");
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
    @DisplayName("B2: pbSwitchInBetween sends the player to the party screen and a trainer's Pokemon to the AI (:155-158)")
    void switchInBetween(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.trainerBattle = true;
        battle.addPlayer(pokemon(data, "HERO", 20));
        battle.addFoe(pokemon(data, "FOE", 20));
        battle.player().hp = 0;
        battle.foe().hp = 0;
        ScriptedScene scene = new ScriptedScene();
        scene.parties.add(1);
        battle.scene = scene;

        // :156 the player chooses through the scene's party screen
        assertEquals(1, battle.pbSwitchInBetween(0, true, false));
        assertEquals(java.util.Arrays.asList("party:0"), scene.log);
        // :157 a trainer's Pokemon goes straight to the AI
        assertEquals(1, battle.pbSwitchInBetween(1, false, false));
        assertEquals(1, scene.log.size());
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
