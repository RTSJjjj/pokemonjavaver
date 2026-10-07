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
 * The battle lines the engine itself produces: the accuracy calculation
 * ({@code Move_Usage_Calculations:127-142}), the status checks
 * ({@code Battler_UseMove_SuccessChecks:269-364}) and the hit/miss lines of
 * {@code Battle.roundMessages}.
 */
class BattleRoundMessagesTest {

    private static Pokemon pokemon(PbsData data, String species, int level) {
        return new Pokemon(data.species(species), level, data);
    }

    private static Battle battle(PbsData data, String player, String foe, Random random) {
        return new Battle(data, random, null)
                .addPlayer(pokemon(data, player, 20))
                .addFoe(pokemon(data, foe, 20));
    }

    @Test
    @DisplayName("accuracy: the two stages are converted separately (:130-138)")
    void accuracyStagesAreSeparate(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "HERO", new Random(1));
        BattleMove move = new BattleMove(data.move("SLASH"));       // accuracy 80
        Battler user = battle.player();
        Battler target = battle.foe();

        // Both stages neutral: 80 * 100 / 100.
        assertEquals(80.0, battle.hitChance(user, target, move), 1e-9);

        // accuracy +2 (:134 -> 100*5/3 rounded 167) against evasion +1
        // (:135 -> 100*4/3 rounded 133): 80*167/133.
        user.hitStages[0] = 2;
        target.hitStages[1] = 1;
        assertEquals(80 * 167.0 / 133.0, battle.hitChance(user, target, move), 1e-9);
        // A single combined stage would give 80*4/3 = 106.67 instead, so the two
        // formulations are not interchangeable here.
        assertNotEquals(80 * 4.0 / 3.0, battle.hitChance(user, target, move), 1e-6);

        // -6 accuracy is 3/9 and +6 evasion is 9/3: 80*33/300.
        user.hitStages[0] = -6;
        target.hitStages[1] = 6;
        assertEquals(80 * 33.0 / 300.0, battle.hitChance(user, target, move), 1e-9);
    }

    @Test
    @DisplayName("a damaging move that cannot affect the target says so (:529-533)")
    void typeImmunityLine(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "SPOOK", new Random(3));
        // SPOOK is GHOST: the synthetic chart makes NORMAL moves ineffective.
        battle.step();
        assertTrue(String.join("|", battle.roundMessages).contains("这不能影响"),
                battle.roundMessages.toString());
        // The target was untouched (damageState.unaffected: Move_Usage:269), so
        // the immune move contributed no hit event; the opponent's own move did.
        assertEquals(battle.foe().maxHp(), battle.foe().hp);
        for (Battle.RoundEvent event : battle.roundEvents) {
            if (event.kind != Battle.RoundEvent.Kind.HIT) continue;
            for (Battle.HitEvent hit : event.hits) {
                assertEquals(0, hit.idxBattler, "only the opponent's move landed");
            }
        }
    }

    @Test
    @DisplayName("a missed attack reports it (Battler_UseMove_SuccessChecks:659)")
    void missLine(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        data.move("SLASH").accuracy = 1;                            // 1% to hit
        Battle battle = battle(data, "HERO", "HERO", new Random(7));
        boolean missed = false;
        for (int i = 0; i < 20 && !missed; i++) {
            battle.foe().hp = battle.foe().maxHp();
            battle.step();
            for (String line : battle.roundMessages) {
                if (line.endsWith("的攻击没有命中！")) {
                    missed = true;
                }
            }
        }
        assertTrue(missed, "a 1% accurate move must miss within 20 tries");
    }

    @Test
    @DisplayName("sleep continues with its line, and waking up says so (:270-281)")
    void sleepLines(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "HERO", new Random(5));
        Battler user = battle.player();
        user.setStatus("SLEEP");
        user.sleepTurns = 3;
        battle.step();
        assertTrue(battle.roundMessages.contains(user.name() + "依旧在沉睡。", false),
                battle.roundMessages.toString());                   // :447

        battle.roundMessages.clear();
        user.sleepTurns = 1;
        battle.step();
        assertEquals("", user.status, "the counter ran out");
        assertTrue(battle.roundMessages.contains(user.name() + "醒来了！", false),
                battle.roundMessages.toString());                   // :473
    }

    @Test
    @DisplayName("a flinching battler says it cannot move (Battler_UseMove_SuccessChecks:323)")
    void flinchLine(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "HERO", new Random(11));
        Battler user = battle.player();
        user.flinched = true;
        battle.step();
        assertTrue(battle.roundMessages.contains(user.name() + "畏缩了，无法行动！", false),
                battle.roundMessages.toString());
    }

    @Test
    @DisplayName("a stat change announces itself (Battler_StatStages:58-62/224-228)")
    void statStageLines(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        // A status move grants its own stat change directly (PokeBattle_Move's
        // status branch), so the line is deterministic.
        data.move("SLASH").power = 0;
        data.move("SLASH").category = "Status";
        data.move("SLASH").function = "01C";                        // user's Attack +1
        Battle battle = battle(data, "HERO", "HERO", new Random(13));
        battle.step();
        assertTrue(String.join("|", battle.roundMessages).contains("的攻击提升了！"),
                battle.roundMessages.toString());
        // :57 pbCommonAnimation("StatUp",self) comes right before the :62 line.
        int animAt = -1;
        int lineAt = -1;
        for (int i = 0; i < battle.roundEvents.size; i++) {
            Battle.RoundEvent event = battle.roundEvents.get(i);
            if (event.kind == Battle.RoundEvent.Kind.ANIMATION && event.anim.common
                    && "StatUp".equals(event.anim.name) && animAt < 0) animAt = i;
            if (event.kind == Battle.RoundEvent.Kind.MESSAGE && event.text != null
                    && event.text.contains("的攻击提升了！") && lineAt < 0) lineAt = i;
        }
        assertTrue(animAt >= 0 && animAt < lineAt, battle.roundEvents.toString());

        // Clamped at +6: no further line (increment <= 0, :55).
        Battle clamped = battle(data, "HERO", "HERO", new Random(13));
        clamped.player().stages[MoveEffects.ATK] = 6;
        String playerLine = clamped.player().thisName() + "的攻击提升了！";
        clamped.step();
        assertFalse(clamped.roundMessages.contains(playerLine, false),
                clamped.roundMessages.toString());
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    /** HERO (NORMAL), SPOOK (GHOST, immune to NORMAL), one move with accuracy 80. */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":2,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[100,100,100,100,100,100],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"SPOOK\":{\"id\":2,\"internalName\":\"SPOOK\",\"name\":\"Spook\","
                + "\"types\":[\"GHOST\"],\"baseStats\":[50,50,50,50,50,50],"
                + "\"growthRate\":\"Medium\",\"baseExp\":50,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]}}}");
        write(root, "moves.json", "{\"total\":1,\"moves\":{"
                + "\"SLASH\":{\"id\":1,\"internalName\":\"SLASH\",\"name\":\"Slash\","
                + "\"power\":70,\"type\":\"NORMAL\",\"category\":\"Physical\",\"accuracy\":80,\"pp\":20}}}");
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
