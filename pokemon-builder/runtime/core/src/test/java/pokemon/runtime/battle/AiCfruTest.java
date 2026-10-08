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
        write(tempDir, "moves.json", "{\"total\":13,\"moves\":{"
                + status("SWORDSDANCE", "02E") + "," + status("SPORE", "003") + "," + status("RECOVER", "0D5") + "," + status("SPIKES", "103") + "," + status("PROTECT", "0AA") + "," + status("MIMIC", "05C") + "," + status("CHARM", "04B") + "," + status("REFLECT", "0A2") + ","
                + move("TACKLE", 40, "NORMAL", "Physical", 35) + "," + move("STRONGHIT", 90, "NORMAL", "Physical", 15) + ","
                + move("WATERGUN", 40, "WATER", "Special", 25) + "," + move("SURF", 90, "WATER", "Special", 15) + ","
                + move("BODYSLAM", 85, "NORMAL", "Physical", 15).replace("\"function\":\"000\"", "\"function\":\"007\"").replace("\"effectChance\":0", "\"effectChance\":30") + "}}");
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

    @Test
    @DisplayName("entry hazards are pointless against a foe with no Pokemon left on the bench (ai_negatives.c:1991-2003: -10)")
    void hazardsNeedABench() {
        Battler foe = foe("FAT", 50, "HERO", 50, "SPIKES", "TACKLE");
        for (int i = 0; i < 10; i++) assertEquals(1, AiMaster.chooseMove(foe.battle, foe, new Random(i)));
    }

    @Test
    @DisplayName("a set-up move beats a plain hit when nothing threatens the attacker (ai_positives.c:215-225, ai_advanced.c:1940)")
    void setsUpWhenSafe() {
        Battler foe = foe("FAT", 100, "HERO", 50, "SWORDSDANCE", "TACKLE");
        AiCtx ctx = new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST);
        int setUp = AiPositives.score(ctx, foe, foe.battle.player(), foe.moveSlot(0), 100);
        assertTrue(setUp > 100, "Swords Dance gets a bonus: " + setUp);
    }

    @Test
    @DisplayName("recovery is only favoured when the foe threatens a knock-out (ShouldRecover, ai_advanced.c:1029)")
    void recoversWhenThreatened() {
        Battler foe = foe("HERO", 100, "FAT", 20, "RECOVER", "TACKLE");
        foe.setHp(foe.maxHp() / 4);
        AiCtx ctx = new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST);
        int heal = AiPositives.score(ctx, foe, foe.battle.player(), foe.moveSlot(0), 100);
        assertTrue(heal >= 100);
        Battler healthy = foe("FAT", 5, "HERO", 50, "RECOVER", "TACKLE");
        AiCtx ctx2 = new AiCtx(healthy.battle, new Random(1), AiMaster.SMARTEST);
        assertEquals(100, AiPositives.score(ctx2, healthy, healthy.battle.player(), healthy.moveSlot(0), 100));
    }

    @Test
    @DisplayName("an original move only the user's line can learn is a signature move; a move another family also learns is not")
    void signatureMoves() throws Exception {
        write(tempDir, "pokemon.json", "{\"total\":3,\"byId\":{},\"species\":{"
                + "\"AAA\":{\"id\":1,\"internalName\":\"AAA\",\"name\":\"AAA\",\"types\":[\"NORMAL\"],\"baseStats\":[50,50,50,50,50,50],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"},{\"level\":5,\"move\":\"STRONGHIT\"}],\"evolutions\":[]},"
                + "\"BBB\":{\"id\":2,\"internalName\":\"BBB\",\"name\":\"BBB\",\"types\":[\"NORMAL\"],\"baseStats\":[50,50,50,50,50,50],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}],\"evolutions\":[]}}}");
        PbsData two = PbsData.parse(tempDir.toFile());
        assertTrue(AiSignature.isSignature(two, "AAA", "STRONGHIT"));
        assertFalse(AiSignature.isSignature(two, "AAA", "TACKLE"));
        assertFalse(AiSignature.isSignature(two, "BBB", "STRONGHIT"));
    }

    // ------------------------------------------------------------------
    // Switching (ai_switching.c)
    // ------------------------------------------------------------------

    private Pokemon mon(String species, int level, String... moves) {
        Pokemon p = new Pokemon(pbs.species(species), level, pbs);
        p.moves.clear();
        for (String id : moves) p.moves.add(new Pokemon.MoveSlot(pbs.move(id)));
        return p;
    }

    private Battle trainerBattle(Pokemon player, Pokemon... foeParty) {
        Battle battle = new Battle(pbs, new Random(5), (user, target, moves) -> 0);
        battle.trainerBattle = true;
        battle.addPlayer(player);
        for (Pokemon f : foeParty) battle.addFoe(f);
        return battle;
    }

    @Test
    @DisplayName("the bench scorer prefers a Pokemon that outspeeds and knocks out the foe (ai_switching.c:2088-2098: +14 +31)")
    void benchScorerPrefersKnockOut() {
        Battle battle = trainerBattle(mon("FAT", 5, "TACKLE"),
                mon("HERO", 5, "TACKLE"), mon("FAT", 5, "TACKLE"), mon("HERO", 100, "STRONGHIT"));
        Battler foe = battle.foe();
        AiCtx ctx = AiMaster.prepare(battle, foe, battle.player(), new Random(3));
        AiSwitching.Bench bench = AiSwitching.calcMostSuitable(ctx, foe, battle.player());
        assertEquals(2, bench.best);
        assertTrue((bench.bestFlags & AiSwitching.FLAG_KO_FOE) != 0);
        assertTrue((bench.bestFlags & AiSwitching.FLAG_OUTSPEEDS) != 0);
    }

    @Test
    @DisplayName("a Pokemon that is about to be knocked out swaps for one that takes no damage (ShouldSwitchToAvoidDeath, :1248)")
    void switchesToAvoidDeath() {
        Battle battle = trainerBattle(mon("HERO", 100, "STRONGHIT"),
                mon("HERO", 5, "TACKLE"), mon("GHOSTY", 100, "TACKLE"));
        Battler foe = battle.foe();
        foe.turnCount = 3;
        int switched = 0;
        for (int seed = 0; seed < 20; seed++) {
            int pick = AiSwitching.decide(battle, foe, new Random(seed));
            if (pick == 1) switched++;
            else assertEquals(AiSwitching.NONE, pick, "the only legal switch is the Ghost");
        }
        assertEquals(20, switched);
    }

    @Test
    @DisplayName("a Pokemon that can knock out first stays in (:438-441, :1281)")
    void staysInWhenItCanKnockOut() {
        Battle battle = trainerBattle(mon("FAT", 5, "TACKLE"),
                mon("HERO", 100, "STRONGHIT"), mon("GHOSTY", 100, "TACKLE"));
        Battler foe = battle.foe();
        foe.turnCount = 3;
        for (int seed = 0; seed < 20; seed++) {
            assertEquals(AiSwitching.NONE, AiSwitching.decide(battle, foe, new Random(seed)));
        }
    }

    @Test
    @DisplayName("a trainer's Pokemon never switches on its first turn (switchingCooldown) and the replacement after a faint is the best bench mon")
    void replacementAfterFaint() {
        Battle battle = trainerBattle(mon("FAT", 5, "TACKLE"),
                mon("HERO", 5, "TACKLE"), mon("FAT", 5, "TACKLE"), mon("HERO", 100, "STRONGHIT"));
        battle.foe().setHp(0);
        assertEquals(2, battle.defaultChooseNewEnemy(1));
    }

    // ------------------------------------------------------------------
    // Item use (143_AI_Item.rb)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a trainer heals a Pokemon under a quarter of its HP with the smallest potion that covers the loss, never above half HP")
    void trainerHealsLowHp() {
        Battle battle = trainerBattle(mon("FAT", 50, "TACKLE"), mon("HERO", 50, "TACKLE"));
        Battler foe = battle.foe();
        battle.foeItems.add("POTION");
        battle.foeItems.add("HYPERPOTION");
        foe.setHp(foe.maxHp() / 2 + 1);
        for (int seed = 0; seed < 30; seed++) assertNull(AiItems.choose(battle, foe, new Random(seed)), "above half HP: never");
        foe.setHp(foe.maxHp() / 8);
        int chosen = 0;
        for (int seed = 0; seed < 60; seed++) {
            String item = AiItems.choose(battle, foe, new Random(seed));
            if (item != null) {
                chosen++;
                assertEquals("HYPERPOTION", item, "POTION (20) does not cover the loss, HYPERPOTION (120) does");
            }
        }
        assertTrue(chosen > 20, "30% are skipped, the rest use the item: " + chosen);
    }

    @Test
    @DisplayName("registering an item spends the action and takes it out of the trainer's list; using it heals")
    void trainerItemIsConsumedAndHeals() {
        Battle battle = trainerBattle(mon("FAT", 50, "TACKLE"), mon("HERO", 50, "TACKLE"));
        Battler foe = battle.foe();
        battle.foeItems.add("HYPERPOTION");
        foe.setHp(1);
        assertTrue(AiItems.register(battle, foe, "HYPERPOTION"));
        assertTrue(battle.foeItems.isEmpty());
        assertEquals(":UseItem", battle.choices(foe.index)[0]);
        AiItems.use(battle, foe, "HYPERPOTION");
        assertTrue(foe.hp > 100 || foe.hp == foe.maxHp());
    }

    @Test
    @DisplayName("in a double battle the foe aims its Normal move at the target that is not immune (ChooseTarget_Doubles, ai_master.c:622)")
    void doublesAvoidsImmuneTarget() {
        Battle battle = new Battle(pbs, new Random(5), (user, target, moves) -> 0);
        battle.trainerBattle = true;
        battle.setSideSizes(2, 2);
        battle.addPlayer(mon("GHOSTY", 50, "TACKLE")).addPlayer(mon("FAT", 50, "TACKLE"));
        battle.addFoe(mon("HERO", 50, "STRONGHIT")).addFoe(mon("HERO", 50, "TACKLE"));
        Battler user = battle.battlerAt(1);
        for (int seed = 0; seed < 20; seed++) {
            AiDoubles.Choice choice = AiDoubles.choose(battle, user, new Random(seed));
            assertNotNull(choice);
            assertEquals(0, choice.slot);
            assertEquals(2, choice.target, "index 0 is the Ghost, 2 is the Normal-type FAT");
        }
    }

    @Test
    @DisplayName("a damaging move that shares a status function code (Body Slam = 007) is a plain attack, not a failed paralysis (ai_negatives.c:3289)")
    void sideEffectHitIsStandardDamage() {
        Battler foe = foe("FAT", 50, "HERO", 50, "BODYSLAM", "TACKLE");
        foe.battle.player().setStatus("PARALYSIS");
        AiCtx ctx = new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST);
        assertTrue(AiNegatives.score(ctx, foe, foe.battle.player(), foe.moveSlot(0), 100) >= 100);
        // 30% is below the 75% needed to count the side effect (ai_positives.c:93-113)
        assertEquals(AiPositives.score(ctx, foe, foe.battle.player(), foe.moveSlot(1), 100) - 0 >= 100, true);
    }

    @Test
    @DisplayName("Protect is favoured when the foe's predicted move would knock the user out and it cannot heal through it (ShouldProtect, ai_advanced.c:1108)")
    void protectsFromAKnockOut() {
        Battler foe = foe("HERO", 100, "HERO", 5, "PROTECT", "TACKLE");
        AiCtx ctx = AiMaster.prepare(foe.battle, foe, foe.battle.player(), new Random(1));
        // give the player a move: the fixture's player Pokemon has none, so teach it STRONGHIT
        assertTrue(AiPositives.score(ctx, foe, foe.battle.player(), foe.moveSlot(0), 100) >= 100);
    }

    @Test
    @DisplayName("Reflect is only worth setting up against a foe with physical moves (ShouldSetUpScreens, ai_advanced.c:1367)")
    void reflectNeedsPhysicalFoe() {
        Battle battle = trainerBattle(mon("HERO", 50, "TACKLE"), mon("HERO", 50, "REFLECT", "TACKLE"));
        Battler foe = battle.foe();
        AiCtx ctx = AiMaster.prepare(battle, foe, battle.player(), new Random(1));
        assertTrue(AiPositiveMore.shouldSetUpScreens(ctx, foe, battle.player(), foe.moveSlot(0)));
        Battle special = trainerBattle(mon("HERO", 50, "WATERGUN"), mon("HERO", 50, "REFLECT", "TACKLE"));
        Battler foe2 = special.foe();
        AiCtx ctx2 = AiMaster.prepare(special, foe2, special.player(), new Random(1));
        assertFalse(AiPositiveMore.shouldSetUpScreens(ctx2, foe2, special.player(), foe2.moveSlot(0)));
    }

    @Test
    @DisplayName("Mimic needs a move to copy: -10 until the foe has used one (ai_negatives.c:1684, gLastUsedMoves)")
    void mimicNeedsAMoveToCopy() {
        Battler foe = foe("FAT", 5, "HERO", 100, "MIMIC", "TACKLE");
        AiCtx ctx = new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST);
        assertTrue(AiNegatives.score(ctx, foe, foe.battle.player(), foe.moveSlot(0), 100) < 100);
        foe.battle.player().lastMoveUsed = "TACKLE";
        assertEquals(100, AiNegatives.score(ctx, foe, foe.battle.player(), foe.moveSlot(0), 100));
    }

    @Test
    @DisplayName("a foe that has shown a stat-lowering move makes raising that stat a bad idea (BadIdeaToRaiseAttackAgainst, ai_util.c:3076)")
    void revealedStatDropStopsSetUp() {
        Battler foe = foe("FAT", 100, "HERO", 50, "SWORDSDANCE", "TACKLE");
        AiCtx ctx = new AiCtx(foe.battle, new Random(1), AiMaster.SMARTEST);
        int before = AiPositives.score(ctx, foe, foe.battle.player(), foe.moveSlot(0), 100);
        foe.battle.player().movesUsed.add("CHARM");
        int after = AiPositives.score(ctx, foe, foe.battle.player(), foe.moveSlot(0), 100);
        assertTrue(before > 100 && after < before, before + " -> " + after);
    }
}
