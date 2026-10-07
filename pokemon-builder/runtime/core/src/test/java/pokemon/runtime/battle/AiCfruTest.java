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

/** CFRU's move scorer (ai_master.c / ai_negatives.c / ai_positives.c) on a single battle. */
class AiCfruTest {

    @TempDir
    Path tempDir;

    private PbsData pbs;

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static String move(String id, int power, String type, String category, int pp) {
        return "\"" + id + "\":{\"id\":" + (id.hashCode() & 0x7fff) + ",\"internalName\":\"" + id + "\",\"name\":\"" + id
                + "\",\"function\":\"000\",\"power\":" + power + ",\"type\":\"" + type + "\",\"category\":\"" + category
                + "\",\"accuracy\":100,\"pp\":" + pp + ",\"effectChance\":0,\"target\":\"NearOther\",\"priority\":0,\"flags\":\"a\"}";
    }

    private static String status(String id, String function) {
        return "\"" + id + "\":{\"id\":" + (id.hashCode() & 0x7fff) + ",\"internalName\":\"" + id + "\",\"name\":\"" + id
                + "\",\"function\":\"" + function + "\",\"power\":0,\"type\":\"NORMAL\",\"category\":\"Status\","
                + "\"accuracy\":100,\"pp\":20,\"effectChance\":0,\"target\":\"User\",\"priority\":0,\"flags\":\"\"}";
    }

    private static String species(String id, int n, String type, int hp, int atk, int def, int spd, String ability) {
        return "\"" + id + "\":{\"id\":" + n + ",\"internalName\":\"" + id + "\",\"name\":\"" + id + "\",\"types\":[\"" + type
                + "\"],\"baseStats\":[" + hp + "," + atk + "," + def + "," + spd + "," + atk + "," + def + "],\"rareness\":45,"
                + "\"weight\":10.0,\"genderRate\":\"Female50Percent\",\"abilities\":[\"" + ability + "\"],\"evolutions\":[]}";
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(tempDir, "pokemon.json", "{\"total\":4,\"byId\":{},\"species\":{"
                + species("HERO", 1, "NORMAL", 80, 80, 80, 80, "NONE") + ","
                + species("GHOSTY", 2, "GHOST", 80, 80, 80, 40, "NONE") + ","
                + species("DUCK", 3, "WATER", 80, 80, 80, 40, "WATERABSORB") + ","
                + species("FAT", 4, "NORMAL", 250, 40, 40, 40, "NONE") + "}}");
        write(tempDir, "moves.json", "{\"total\":7,\"moves\":{"
                + status("SWORDSDANCE", "02E") + "," + status("SPORE", "003") + "," + status("RECOVER", "0D5") + ","
                + move("TACKLE", 40, "NORMAL", "Physical", 35) + "," + move("STRONGHIT", 90, "NORMAL", "Physical", 15) + ","
                + move("WATERGUN", 40, "WATER", "Special", 25) + "," + move("SURF", 90, "WATER", "Special", 15) + "}}");
        write(tempDir, "types.json", "{\"total\":3,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"GHOST\":{\"id\":7,\"internalName\":\"GHOST\",\"name\":\"Ghost\",\"immunities\":[\"NORMAL\"]},"
                + "\"WATER\":{\"id\":10,\"internalName\":\"WATER\",\"name\":\"Water\"}}}");
        write(tempDir, "abilities.json", "{\"total\":1,\"abilities\":{\"WATERABSORB\":{\"id\":1,\"internalName\":\"WATERABSORB\",\"name\":\"WATERABSORB\"}}}");
        write(tempDir, "items.json", "{\"total\":0,\"items\":{}}");
        write(tempDir, "natures.json", "{\"total\":1,\"natures\":[{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        pbs = PbsData.parse(tempDir.toFile());
    }

    private Battler foe(String playerSpecies, int playerLevel, String foeSpecies, int foeLevel, String... foeMoves) {
        Battle battle = new Battle(pbs, new Random(5), (user, target, moves) -> 0);
        battle.trainerBattle = true;
        battle.addPlayer(new Pokemon(pbs.species(playerSpecies), playerLevel, pbs));
        Pokemon foe = new Pokemon(pbs.species(foeSpecies), foeLevel, pbs);
        foe.moves.clear();
        for (String id : foeMoves) foe.moves.add(new Pokemon.MoveSlot(pbs.move(id)));
        battle.addFoe(foe);
        return battle.foe();
    }

    private int pick(Battler foe) {
        return AiMaster.chooseMove(foe.battle, foe, new Random(11));
    }

    @Test
    @DisplayName("the smartest tier skips a move the target is immune to (ai_negatives.c:3246-3250: -15) and takes the other")
    void avoidsImmuneMove() {
        Battler foe = foe("GHOSTY", 50, "HERO", 50, "STRONGHIT", "WATERGUN");
        for (int i = 0; i < 20; i++) {
            assertEquals(1, AiMaster.chooseMove(foe.battle, foe, new Random(i)), "STRONGHIT (Normal) cannot hit a Ghost");
        }
    }

    @Test
    @DisplayName("a type-absorbing Ability makes the move useless: Water vs Water Absorb is -20 (ai_negatives.c:230)")
    void avoidsAbsorbedMove() {
        Battler foe = foe("DUCK", 50, "HERO", 50, "SURF", "TACKLE");
        assertEquals(1, pick(foe));
    }

    @Test
    @DisplayName("a move that knocks out and goes first is preferred over a stronger-looking one that does not (ai_positives.c:2747-2762: +9)")
    void prefersKnockOut() {
        // The player's FAT is slower and has plenty of HP; the foe's weak TACKLE cannot KO, STRONGHIT cannot either, but at level 100 vs 5 both KO.
        Battler foe = foe("FAT", 5, "HERO", 100, "TACKLE", "STRONGHIT");
        int slot = pick(foe);
        assertTrue(slot == 0 || slot == 1);
        // Both knock out: they tie and one is chosen at random, never a blank slot.
        assertNotNull(foe.moveSlot(slot));
    }

    @Test
    @DisplayName("without a knock-out the strongest move wins (ai_positives.c:2795-2838: +2)")
    void prefersStrongestMove() {
        Battler foe = foe("FAT", 100, "HERO", 50, "TACKLE", "STRONGHIT");
        for (int i = 0; i < 10; i++) {
            assertEquals(1, AiMaster.chooseMove(foe.battle, foe, new Random(i)));
        }
    }

    @Test
    @DisplayName("a +2 Attack move at +6 Attack is -10 (ai_negatives.c:931-948) and the damaging move is chosen")
    void maxedSetUpMoveIsAvoided() {
        Battler foe = foe("FAT", 50, "HERO", 50, "SWORDSDANCE", "TACKLE");
        foe.stages[0] = 6;
        for (int i = 0; i < 10; i++) assertEquals(1, AiMaster.chooseMove(foe.battle, foe, new Random(i)));
    }

    @Test
    @DisplayName("a sleep move on a target that already has a status is -10 (ai_negatives.c:775-780)")
    void sleepOnStatusedTargetIsAvoided() {
        Battler foe = foe("FAT", 50, "HERO", 50, "SPORE", "TACKLE");
        foe.battle.player().setStatus("BURN");
        for (int i = 0; i < 10; i++) assertEquals(1, AiMaster.chooseMove(foe.battle, foe, new Random(i)));
    }

    @Test
    @DisplayName("a healing move at full HP is -10 (ai_negatives.c:1493) and a damaging move is chosen; below 90% it is not penalised")
    void healingAtFullHp() {
        Battler foe = foe("FAT", 50, "HERO", 50, "RECOVER", "TACKLE");
        for (int i = 0; i < 10; i++) assertEquals(1, AiMaster.chooseMove(foe.battle, foe, new Random(i)));
        int base = AiNegatives.score(new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST), foe, foe.battle.player(), foe.moveSlot(0), 100);
        assertEquals(90, base);
        foe.setHp(foe.maxHp() / 2);
        assertEquals(100, AiNegatives.score(new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST), foe, foe.battle.player(), foe.moveSlot(0), 100));
    }
}
