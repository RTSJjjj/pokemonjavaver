package pokemon.runtime.battle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.BallTypes;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.TrainerState;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R13: the Poké Ball capture calculation (PokeBattle_BattleCommon:170-232) and
 * the ball handlers (PokeBall_CatchEffects:57-257).
 */
class CaptureCalculatorTest {

    @TempDir
    Path tempDir;

    private PbsData pbs;
    private TrainerState trainer;

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        write(tempDir, "pokemon.json", "{\"total\":4,\"byId\":{},"
                + "\"species\":{"
                // rareness 45 (the classic Caterpie-like value), weight 2.9 kg.
                + "\"CATERPIE\":{\"id\":10,\"internalName\":\"CATERPIE\",\"name\":\"绿毛虫\","
                + "\"types\":[\"BUG\"],\"baseStats\":[45,30,35,45,20,20],\"rareness\":45,"
                + "\"weight\":2.9,\"genderRate\":\"Female50Percent\",\"evolutions\":[]},"
                + "\"PIKACHU\":{\"id\":25,\"internalName\":\"PIKACHU\",\"name\":\"皮卡丘\","
                + "\"types\":[\"ELECTRIC\"],\"baseStats\":[35,55,40,90,50,50],\"rareness\":190,"
                + "\"weight\":6.0,\"genderRate\":\"Female50Percent\",\"evolutions\":[]},"
                + "\"SNORLAX\":{\"id\":143,\"internalName\":\"SNORLAX\",\"name\":\"卡比兽\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[160,110,65,30,65,110],\"rareness\":25,"
                + "\"weight\":460.0,\"genderRate\":\"Female12Percent\",\"evolutions\":[]},"
                // A Moon Stone family member, for the Moon Ball.
                + "\"NIDORAN_F\":{\"id\":29,\"internalName\":\"NIDORAN_F\",\"name\":\"尼多兰\","
                + "\"types\":[\"POISON\"],\"baseStats\":[55,47,52,41,40,40],\"rareness\":235,"
                + "\"weight\":7.0,\"genderRate\":\"FemaleOnly\","
                + "\"evolutions\":[{\"species\":\"NIDORINA\",\"method\":\"Item\","
                + "\"parameter\":\"MOONSTONE\"}]}}}");
        write(tempDir, "items.json", "{\"total\":6,\"items\":{"
                + "\"POKEBALL\":{\"id\":1,\"internalName\":\"POKEBALL\",\"name\":\"精灵球\","
                + "\"pocket\":3,\"price\":200,\"type\":3},"
                + "\"GREATBALL\":{\"id\":2,\"internalName\":\"GREATBALL\",\"name\":\"超级球\","
                + "\"pocket\":3,\"price\":600,\"type\":3},"
                + "\"MASTERBALL\":{\"id\":4,\"internalName\":\"MASTERBALL\",\"name\":\"大师球\","
                + "\"pocket\":3,\"price\":0,\"type\":3},"
                + "\"NETBALL\":{\"id\":6,\"internalName\":\"NETBALL\",\"name\":\"捕网球\","
                + "\"pocket\":3,\"price\":1000,\"type\":3},"
                + "\"REPEATBALL\":{\"id\":9,\"internalName\":\"REPEATBALL\",\"name\":\"重复球\","
                + "\"pocket\":3,\"price\":1000,\"type\":3},"
                + "\"POTION\":{\"id\":20,\"internalName\":\"POTION\",\"name\":\"伤药\","
                + "\"pocket\":1,\"price\":300,\"type\":0}}}");
        pbs = PbsData.parse(tempDir.toFile());
        trainer = new TrainerState();
    }

    private CaptureCalculator.Context context(String species, int hp, int maxHp) {
        PbsData.Species data = pbs.species(species);
        Pokemon target = new Pokemon(data, 10, pbs);
        CaptureCalculator.Context context = new CaptureCalculator.Context();
        context.pbs = pbs;
        context.trainer = trainer;
        context.target = target;
        context.targetHp = hp;
        context.targetMaxHp = maxHp;
        context.rareness = data.rareness;
        context.random = new Random(1234);
        return context;
    }

    @Test
    @DisplayName("items.txt ITEM_TYPE identifies Poke Balls (PItem_Items:89-92)")
    void identifiesPokeBalls() {
        assertTrue(pbs.item("POKEBALL").isPokeBall());
        assertTrue(pbs.item("NETBALL").isPokeBall());
        assertFalse(pbs.item("POTION").isPokeBall());
        assertEquals(3, pbs.item("POKEBALL").type);
    }

    @Test
    @DisplayName("$BallTypes maps every ball type to its item (PokeBall_CatchEffects:1-42)")
    void ballTypes() {
        assertEquals(28, BallTypes.count());
        assertEquals(0, BallTypes.ballType(pbs, "POKEBALL"));
        assertEquals(4, BallTypes.ballType(pbs, "MASTERBALL"));
        assertEquals(5, BallTypes.ballType(pbs, "NETBALL"));
        assertEquals(8, BallTypes.ballType(pbs, "REPEATBALL"));
        assertEquals("POKEBALL", BallTypes.itemName(0));
        assertEquals("BEASTBALL", BallTypes.itemName(25));
        assertEquals("POKEBALL", BallTypes.item(pbs, 0).internalName);
    }

    @Test
    @DisplayName("the Master Ball is unconditional (:91-93), so it always shakes four times")
    void masterBallIsUnconditional() {
        CaptureCalculator.Context context = context("SNORLAX", 200, 200);
        assertTrue(CaptureCalculator.isUnconditional(context, "MASTERBALL"));
        assertEquals(4, CaptureCalculator.shakes(context, "MASTERBALL"));
    }

    @Test
    @DisplayName("x is the HP factor times the modified rareness (:190-200)")
    void captureRateFormula() {
        // Full health halves the factor: (3a-2a)/3a = 1/3, so a rareness-190
        // Pikachu at full HP has x = 63.
        CaptureCalculator.Context context = context("PIKACHU", 10, 10);
        assertEquals(63, CaptureCalculator.captureRate(context, "POKEBALL"));
        // Sleep multiplies by 2.5 (:194-195).
        context.targetStatus = "SLEEP";
        assertEquals(158, CaptureCalculator.captureRate(context, "POKEBALL"));
        // Any other status multiplies by 1.5 (:196-197).
        context.targetStatus = "POISON";
        assertEquals(95, CaptureCalculator.captureRate(context, "POKEBALL"));
        // Down to a sliver of HP the factor approaches the rareness itself.
        context.targetStatus = "";
        context.targetHp = 1;
        assertEquals(177, CaptureCalculator.captureRate(context, "POKEBALL"));
        // x is never below 1 (:199-200).
        CaptureCalculator.Context weak = context("CATERPIE", 100, 100);
        weak.rareness = 1;
        assertEquals(1, CaptureCalculator.captureRate(weak, "POKEBALL"));
    }

    @Test
    @DisplayName("x >= 255 is a definite capture without random checks (:201-202)")
    void definiteCapture() {
        // The factor only reaches the rareness itself at 0 HP, so a rareness-255
        // foe on the floor is the x >= 255 case.
        CaptureCalculator.Context context = context("CATERPIE", 0, 100);
        context.rareness = 255;
        assertEquals(255, CaptureCalculator.captureRate(context, "POKEBALL"));
        assertEquals(4, CaptureCalculator.shakes(context, "POKEBALL"));
    }

    @Test
    @DisplayName("the badge and Pokedex bonuses are added before the ball multiplier (:70-71)")
    void badgeAndDexBonus() {
        CaptureCalculator.Context context = context("CATERPIE", 10, 10);
        assertEquals(45, CaptureCalculator.modifyCatchRate(context, "POKEBALL", 45));
        trainer.badges.add(1);
        trainer.badges.add(2);
        assertEquals(49, CaptureCalculator.modifyCatchRate(context, "POKEBALL", 45));
        // 32 owned species add exactly 1 (pokedexOwned / 32).
        for (int i = 0; i < 32; i++) {
            trainer.owned.add("SPECIES" + i);
        }
        assertEquals(50, CaptureCalculator.modifyCatchRate(context, "POKEBALL", 45));
        // ...and the ball's own multiplier applies afterwards.
        assertEquals(100, CaptureCalculator.modifyCatchRate(context, "ULTRABALL", 45));
    }

    @Test
    @DisplayName("ball handlers: Net Ball's types, Nest Ball's level ramp, Timer/Quick Ball turns")
    void ballHandlers() {
        CaptureCalculator.Context bug = context("CATERPIE", 10, 10);
        assertEquals(157, CaptureCalculator.modifyCatchRate(bug, "NETBALL", 45),
                "Bug type gets the 3.5x Net Ball multiplier (:113-117)");
        CaptureCalculator.Context electric = context("PIKACHU", 10, 10);
        assertEquals(190, CaptureCalculator.modifyCatchRate(electric, "NETBALL", 190),
                "a non-Bug/Water type keeps the plain rate");

        // Nest Ball: level 10 -> max((41-10)/10, 1) = 3.1 (:124-129).
        assertEquals(139, CaptureCalculator.modifyCatchRate(bug, "NESTBALL", 45));

        // Timer Ball: min(1 + 0.3*turnCount, 4) (:137-141).
        bug.turnCount = 5;
        assertEquals(112, CaptureCalculator.modifyCatchRate(bug, "TIMERBALL", 45));
        bug.turnCount = 20;
        assertEquals(180, CaptureCalculator.modifyCatchRate(bug, "TIMERBALL", 45));

        // Quick Ball only on the first round (:149-153).
        bug.turnCount = 0;
        assertEquals(180, CaptureCalculator.modifyCatchRate(bug, "QUICKBALL", 45));
        bug.turnCount = 1;
        assertEquals(45, CaptureCalculator.modifyCatchRate(bug, "QUICKBALL", 45));
    }

    @Test
    @DisplayName("Dusk Ball reads battle.time, Repeat Ball the Pokedex (:131-147)")
    void duskAndRepeatBall() {
        CaptureCalculator.Context context = context("CATERPIE", 10, 10);
        assertEquals(45, CaptureCalculator.modifyCatchRate(context, "DUSKBALL", 45),
                "daytime: no Dusk Ball bonus");
        context.time = 2;
        assertEquals(135, CaptureCalculator.modifyCatchRate(context, "DUSKBALL", 45),
                "night/cave: 3x with NEWEST_BATTLE_MECHANICS (:143-147)");

        assertEquals(45, CaptureCalculator.modifyCatchRate(context, "REPEATBALL", 45));
        trainer.owned.add("CATERPIE");
        assertEquals(157, CaptureCalculator.modifyCatchRate(context, "REPEATBALL", 45));
    }

    @Test
    @DisplayName("Heavy Ball takes this project's <1000 kg branch, Moon Ball the family (:182-220)")
    void heavyAndMoonBall() {
        // pbWeight is the species weight in kg, so every species is < 1000 and
        // the Heavy Ball always subtracts 20 (:182-199, ported literally).
        CaptureCalculator.Context light = context("CATERPIE", 10, 10);
        assertEquals(25, CaptureCalculator.modifyCatchRate(light, "HEAVYBALL", 45));
        CaptureCalculator.Context heavy = context("SNORLAX", 10, 10);
        assertEquals(5, CaptureCalculator.modifyCatchRate(heavy, "HEAVYBALL", 25));
        assertEquals(1, CaptureCalculator.modifyCatchRate(heavy, "HEAVYBALL", 5),
                "the result never drops below 1 (:197-198)");

        CaptureCalculator.Context nidoran = context("NIDORAN_F", 10, 10);
        assertEquals(255, CaptureCalculator.modifyCatchRate(nidoran, "MOONBALL", 235),
                "the family evolves with a Moon Stone, clamped to 255 (:211-220)");
        assertEquals(45, CaptureCalculator.modifyCatchRate(light, "MOONBALL", 45));
    }

    @Test
    @DisplayName("Beast Ball and Dream Ball (:226-238)")
    void beastAndDreamBall() {
        CaptureCalculator.Context context = context("CATERPIE", 10, 10);
        context.targetStatus = "SLEEP";
        assertEquals(180, CaptureCalculator.modifyCatchRate(context, "DREAMBALL", 45));
        context.targetStatus = "";
        assertEquals(45, CaptureCalculator.modifyCatchRate(context, "DREAMBALL", 45));
        // battleRank < 2 (a normal wild Pokemon): the Beast Ball divides by 10.
        context.target.battleRank = 0;
        assertEquals(4, CaptureCalculator.modifyCatchRate(context, "BEASTBALL", 45));
        context.target.battleRank = 2;
        assertEquals(45, CaptureCalculator.modifyCatchRate(context, "BEASTBALL", 45));
    }

    @Test
    @DisplayName("OnCatch: the Heal Ball heals, the Friend Ball sets happiness (:251-257)")
    void onCatchHandlers() {
        Pokemon caught = new Pokemon(pbs.species("CATERPIE"), 10, pbs);
        caught.hp = 1;
        caught.status = "POISON";
        CaptureCalculator.onCatch(pbs, "HEALBALL", caught);
        assertEquals(caught.maxHp(), caught.hp);
        assertEquals("", caught.status);

        caught.happiness = 70;
        CaptureCalculator.onCatch(pbs, "FRIENDBALL", caught);
        assertEquals(200, caught.happiness);
    }

    @Test
    @DisplayName("the shake roll never exceeds four, and a low rate rarely reaches it")
    void shakeRoll() {
        // A full-health Snorlax (rareness 25) on a plain ball is very hard.
        CaptureCalculator.Context hard = context("SNORLAX", 500, 500);
        int catches = 0;
        for (int i = 0; i < 500; i++) {
            hard.random = new Random(i);
            if (CaptureCalculator.shakes(hard, "POKEBALL") == 4) {
                catches++;
            }
        }
        assertTrue(catches < 50, "a 25-rareness full-HP foe is rarely caught: " + catches);

        // The same foe asleep at 1 HP is a very different story.
        CaptureCalculator.Context easy = context("CATERPIE", 1, 100);
        easy.targetStatus = "SLEEP";
        int easyCatches = 0;
        for (int i = 0; i < 500; i++) {
            easy.random = new Random(i);
            if (CaptureCalculator.shakes(easy, "POKEBALL") == 4) {
                easyCatches++;
            }
        }
        assertTrue(easyCatches > catches * 3,
                "1 HP + sleep must be far better than full HP: " + easyCatches + " vs " + catches);
        assertTrue(easyCatches > 200, "and a likely catch in absolute terms: " + easyCatches);
    }

    @Test
    @DisplayName("PBEnvironment names map to the plugin's ids (PBEnvironment:3-21)")
    void environmentIds() {
        assertEquals(CaptureCalculator.ENVIRONMENT_CAVE, CaptureCalculator.environmentId("Cave"));
        assertEquals(6, CaptureCalculator.environmentId("Underwater"));
        assertEquals(-1, CaptureCalculator.environmentId(null));
        assertEquals(-1, CaptureCalculator.environmentId("Nowhere"));
    }
}
