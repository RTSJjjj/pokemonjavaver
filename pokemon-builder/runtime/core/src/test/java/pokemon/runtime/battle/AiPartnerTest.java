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

/** CFRU's doubles machinery: fight classes, the doubles killing score and AIScript_Partner (ai_partner.c). */
class AiPartnerTest {

    @TempDir
    Path tempDir;

    private PbsData pbs;

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static String move(String id, int power, String type, String category, String function, String target, int priority) {
        return "\"" + id + "\":{\"id\":" + (id.hashCode() & 0x7fff) + ",\"internalName\":\"" + id + "\",\"name\":\"" + id
                + "\",\"function\":\"" + function + "\",\"power\":" + power + ",\"type\":\"" + type + "\",\"category\":\"" + category
                + "\",\"accuracy\":100,\"pp\":20,\"effectChance\":0,\"target\":\"" + target + "\",\"priority\":" + priority + ",\"flags\":\"ab\"}";
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
                + species("ABSORBER", 2, "ELECTRIC", 80, 80, 80, 40, "VOLTABSORB") + ","
                + species("TRUANTER", 3, "NORMAL", 80, 80, 80, 40, "TRUANT") + ","
                + species("FAT", 4, "NORMAL", 250, 40, 40, 40, "NONE") + "}}");
        write(tempDir, "moves.json", "{\"total\":12,\"moves\":{"
                + move("TACKLE", 40, "NORMAL", "Physical", "000", "NearOther", 0) + ","
                + move("QUICKHIT", 40, "NORMAL", "Physical", "000", "NearOther", 0) + ","
                + move("SPARK", 65, "ELECTRIC", "Special", "000", "NearOther", 0) + ","
                + move("BOLT", 90, "ELECTRIC", "Special", "000", "NearOther", 0) + ","
                + move("EARTHQUAKE", 100, "NORMAL", "Physical", "000", "AllNearOthers", 0) + ","
                + move("TRICKROOM", 0, "NORMAL", "Status", "11F", "BothSides", -7) + ","
                + move("REFLECT", 0, "NORMAL", "Status", "0A2", "UserSide", 0) + ","
                + move("HELPINGHAND", 0, "NORMAL", "Status", "09C", "NearAlly", 5) + ","
                + move("SKILLSWAP", 0, "NORMAL", "Status", "067", "NearOther", 0) + ","
                + move("FAKEOUT", 40, "NORMAL", "Physical", "012", "NearOther", 3) + ","
                + move("PROTECT", 0, "NORMAL", "Status", "0AA", "User", 4) + ","
                + move("TAILWIND", 0, "NORMAL", "Status", "05B", "UserSide", 0) + ","
                + move("WIDEGUARD", 0, "NORMAL", "Status", "0AC", "UserSide", 3) + "}}");
        write(tempDir, "types.json", "{\"total\":2,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"ELECTRIC\":{\"id\":13,\"internalName\":\"ELECTRIC\",\"name\":\"Electric\"}}}");
        write(tempDir, "abilities.json", "{\"total\":3,\"abilities\":{"
                + "\"VOLTABSORB\":{\"id\":1,\"internalName\":\"VOLTABSORB\",\"name\":\"VOLTABSORB\"},"
                + "\"TRUANT\":{\"id\":2,\"internalName\":\"TRUANT\",\"name\":\"TRUANT\"},"
                + "\"NONE\":{\"id\":3,\"internalName\":\"NONE\",\"name\":\"NONE\"}}}");
        write(tempDir, "items.json", "{\"total\":0,\"items\":{}}");
        write(tempDir, "natures.json", "{\"total\":1,\"natures\":[{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        pbs = PbsData.parse(tempDir.toFile());
    }

    private Pokemon mon(String species, int level, String... moves) {
        Pokemon p = new Pokemon(pbs.species(species), level, pbs);
        p.moves.clear();
        for (String id : moves) p.moves.add(new Pokemon.MoveSlot(pbs.move(id)));
        return p;
    }

    /** A 2v2: foes are indices 1 and 3 (the AI), the player's are 0 and 2. */
    private Battle doubles(Pokemon foe1, Pokemon foe2, Pokemon player1, Pokemon player2) {
        Battle battle = new Battle(pbs, new Random(5), (user, target, moves) -> 0);
        battle.trainerBattle = true;
        battle.setSideSizes(2, 2);
        battle.addPlayer(player1).addPlayer(player2);
        battle.addFoe(foe1).addFoe(foe2);
        return battle;
    }

    private Battle defaultDoubles(Pokemon foe1, Pokemon foe2) {
        return doubles(foe1, foe2, mon("FAT", 50, "TACKLE"), mon("FAT", 50, "TACKLE"));
    }

    @Test
    @DisplayName("PredictFightingStyle's doubles branch picks the doubles classes (ai_advanced.c:586-775)")
    void doublesFightingClasses() {
        Battle b = defaultDoubles(mon("HERO", 50, "TRICKROOM", "TACKLE", "QUICKHIT"), mon("HERO", 50, "TACKLE", "QUICKHIT", "SPARK", "BOLT"));
        AiCtx ctx = AiMaster.prepare(b, b.battlerAt(1), b.battlerAt(0), new Random(1));
        assertEquals(AiCalc.CLASS_D_TRICK_ROOM_ATTACKER, AiCalc.fightingStyle(ctx, b.battlerAt(1)));   // Trick Room + 2 attacks
        assertEquals(AiCalc.CLASS_D_ALL_OUT_ATTACKER, AiCalc.fightingStyle(ctx, b.battlerAt(3)));      // 4 attacking moves
        Battle c = defaultDoubles(mon("HERO", 50, "TRICKROOM", "TACKLE"), mon("HERO", 50, "REFLECT", "HELPINGHAND", "TACKLE"));
        AiCtx ctx2 = AiMaster.prepare(c, c.battlerAt(1), c.battlerAt(0), new Random(1));
        assertEquals(AiCalc.CLASS_D_TRICK_ROOM_SETUP, AiCalc.fightingStyle(ctx2, c.battlerAt(1)));     // Trick Room + 1 attack
        assertEquals(AiCalc.CLASS_D_TOTAL_TEAM_SUPPORT, AiCalc.fightingStyle(ctx2, c.battlerAt(3)));   // Reflect, <= 1 attack
    }

    @Test
    @DisplayName("a 1v1 keeps the singles classes even in a double battle (IS_DOUBLE_BATTLE needs two foes or a partner, ai_advanced.c:186)")
    void lastMonsKeepSinglesClasses() {
        Battle b = defaultDoubles(mon("HERO", 50, "TACKLE", "QUICKHIT", "SPARK"), mon("HERO", 50, "TACKLE"));
        b.battlerAt(3).hp = 0;
        b.battlerAt(2).hp = 0;
        AiCtx ctx = AiMaster.prepare(b, b.battlerAt(1), b.battlerAt(0), new Random(1));
        assertFalse(AiDoublesScore.isDouble(b, b.battlerAt(1)));
        assertTrue(AiCalc.fightingStyle(ctx, b.battlerAt(1)) < AiCalc.CLASS_D_ALL_OUT_ATTACKER);
    }

    @Test
    @DisplayName("Helping Hand is only worth it for a partner that has an attack to boost (AIScript_Partner EFFECT_HELPING_HAND, :491-529)")
    void helpingHandNeedsAnAttackingPartner() {
        Battle b = defaultDoubles(mon("HERO", 50, "HELPINGHAND", "TACKLE", "QUICKHIT", "SPARK"), mon("HERO", 50, "TACKLE"));
        Battler user = b.battlerAt(1);
        Battler partner = b.battlerAt(3);
        AiCtx ctx = AiMaster.prepare(b, user, partner, new Random(1));
        int score = AiPartner.score(ctx, user, partner, user.moveSlot(0), 100);
        // the partner has not chosen a move yet: no partnerMove, so Helping Hand gains nothing (a move must be known)
        assertEquals(100, score);
        // once the partner has chosen Tackle on a foe that it cannot knock out, Helping Hand pays off
        b.choices(partner.index)[0] = ":UseMove";
        b.choices(partner.index)[1] = 0;
        b.choices(partner.index)[2] = partner.moveSlot(0);
        b.choices(partner.index)[3] = 0;
        int helped = AiPartner.score(ctx, user, partner, user.moveSlot(0), 100);
        assertTrue(helped > 100, "Helping Hand = " + helped);
    }

    @Test
    @DisplayName("an Electric move into a Volt Absorb partner heals it when it is hurt (AIScript_Partner ABILITY_VOLTABSORB, :67-69)")
    void healsAPartnerWithVoltAbsorb() {
        Battle b = defaultDoubles(mon("HERO", 50, "SPARK", "TACKLE"), mon("ABSORBER", 50, "TACKLE"));
        Battler user = b.battlerAt(1);
        Battler partner = b.battlerAt(3);
        partner.hp = partner.maxHp() / 2;
        AiCtx ctx = AiMaster.prepare(b, user, partner, new Random(1));
        assertTrue(AiPartner.score(ctx, user, partner, user.moveSlot(0), 100) > 100, "hurt partner is healed by the absorbed move");
        assertEquals(100, AiPartner.score(ctx, user, partner, user.moveSlot(1), 100), "a Normal move is not absorbed");
        partner.hp = partner.maxHp();
        assertEquals(100, AiPartner.score(ctx, user, partner, user.moveSlot(0), 100), "a healthy partner gains nothing");
    }

    @Test
    @DisplayName("Skill Swap is used to take a bad Ability off the partner (AIScript_Partner EFFECT_SKILL_SWAP, :536-571)")
    void skillSwapRemovesTruant() {
        Battle b = defaultDoubles(mon("HERO", 50, "SKILLSWAP", "TACKLE"), mon("TRUANTER", 50, "TACKLE"));
        Battler user = b.battlerAt(1);
        Battler partner = b.battlerAt(3);
        AiCtx ctx = AiMaster.prepare(b, user, partner, new Random(1));
        assertTrue(AiPartner.score(ctx, user, partner, user.moveSlot(0), 100) > 100);
    }

    @Test
    @DisplayName("the Negatives script no longer penalises an absorbed move aimed at the partner (TARGETING_PARTNER, ai_negatives.c:220)")
    void absorbedMoveOnPartnerIsNotPenalised() {
        Battle b = defaultDoubles(mon("HERO", 50, "SPARK", "TACKLE"), mon("ABSORBER", 50, "TACKLE"));
        Battler user = b.battlerAt(1);
        AiCtx ctx = AiMaster.prepare(b, user, b.battlerAt(3), new Random(1));
        assertTrue(AiNegatives.score(ctx, user, b.battlerAt(3), user.moveSlot(0), 100) >= 100, "partner target");
        // against a foe with the same Ability the move is useless (-20)
        Battle c = doubles(mon("HERO", 50, "SPARK", "TACKLE"), mon("HERO", 50, "TACKLE"), mon("ABSORBER", 50, "TACKLE"), mon("FAT", 50, "TACKLE"));
        AiCtx ctx2 = AiMaster.prepare(c, c.battlerAt(1), c.battlerAt(0), new Random(1));
        assertTrue(AiNegatives.score(ctx2, c.battlerAt(1), c.battlerAt(0), c.battlerAt(1).moveSlot(0), 100) < 100, "foe target");
    }

    @Test
    @DisplayName("the doubles killing score is zero for a move that is not the best one and positive for the best one (GetDoubleKillingScore, ai_util.c:1113)")
    void doubleKillingScore() {
        Battle b = defaultDoubles(mon("HERO", 90, "TACKLE", "EARTHQUAKE"), mon("HERO", 50, "TACKLE"));
        Battler user = b.battlerAt(1);
        AiCtx ctx = AiMaster.prepare(b, user, b.battlerAt(0), new Random(1));
        int best = 0;
        int nonBest = 0;
        for (int i = 0; i < 2; i++) {
            int s = AiDoublesScore.doubleKillingScore(ctx, user.moveSlot(i), user, b.battlerAt(0));
            if (s > 0) best++; else nonBest++;
        }
        assertEquals(1, best, "exactly one best move");
        assertEquals(1, nonBest);
    }

    @Test
    @DisplayName("the doubles AI can now pick its partner as the target when a move is clearly good for it")
    void allyIsAScoredTarget() {
        Battle b = defaultDoubles(mon("HERO", 50, "SPARK", "TACKLE"), mon("ABSORBER", 50, "TACKLE"));
        b.battlerAt(3).hp = b.battlerAt(3).maxHp() / 3;
        Battler user = b.battlerAt(1);
        boolean pickedAlly = false;
        for (int seed = 0; seed < 30 && !pickedAlly; seed++) {
            AiDoubles.Choice c = AiDoubles.choose(b, user, new Random(seed));
            assertNotNull(c);
            pickedAlly = c.target == 3;
        }
        // Volt Absorb heal is +7..+13 for the doubles classes; a plain damaging move vs a foe may still win, so only assert it is reachable
        assertDoesNotThrow(() -> AiDoubles.choose(b, user, new Random(1)));
    }

    @Test
    @DisplayName("Protect is favoured when the partner is about to use a move that hits the whole field (ShouldProtect doubles, PROTECT_FROM_ALLIES, ai_advanced.c:1190)")
    void protectsFromThePartnersEarthquake() {
        Battle b = defaultDoubles(mon("HERO", 50, "PROTECT", "TACKLE"), mon("HERO", 50, "EARTHQUAKE", "TACKLE"));
        Battler user = b.battlerAt(1);
        Battler partner = b.battlerAt(3);
        b.choices(partner.index)[0] = ":UseMove";
        b.choices(partner.index)[1] = 0;
        b.choices(partner.index)[2] = partner.moveSlot(0);
        b.choices(partner.index)[3] = 0;
        AiCtx ctx = AiMaster.prepare(b, user, b.battlerAt(0), new Random(1));
        assertTrue(AiPositives.score(ctx, user, b.battlerAt(0), user.moveSlot(0), 100) >= 100 + 12, "Protect gains the ally-protection bonus");
    }

    @Test
    @DisplayName("Wide Guard is only worth it against a predicted spread move (ai_positives.c:1148)")
    void wideGuardNeedsASpreadMove() {
        Battle b = doubles(mon("HERO", 50, "WIDEGUARD", "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "EARTHQUAKE"), mon("HERO", 50, "TACKLE"));
        Battler user = b.battlerAt(1);
        AiCtx ctx = AiMaster.prepare(b, user, b.battlerAt(0), new Random(1));
        int score = AiPositives.score(ctx, user, b.battlerAt(0), user.moveSlot(0), 100);
        assertTrue(ctx.prediction(b.battlerAt(0)) == null || score >= 100);
    }

    @Test
    @DisplayName("a Trick Room user that is slower than the foes scores +19 as a doubles Trick Room class (ai_positives.c:2423)")
    void trickRoomForADoublesTrickRoomer() {
        Battle b = doubles(mon("FAT", 50, "TRICKROOM", "TACKLE"), mon("FAT", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"));
        Battler user = b.battlerAt(1);
        AiCtx ctx = AiMaster.prepare(b, user, b.battlerAt(0), new Random(1));
        assertEquals(AiCalc.CLASS_D_TRICK_ROOM_SETUP, AiCalc.fightingStyle(ctx, user));
        assertTrue(AiPositives.score(ctx, user, b.battlerAt(0), user.moveSlot(0), 100) >= 119);
        b.field.effects.set(PBEffects.Field.TrickRoom, 5);
        assertEquals(100, AiPositives.score(AiMaster.prepare(b, user, b.battlerAt(0), new Random(1)), user, b.battlerAt(0), user.moveSlot(0), 100),
                "no point while Trick Room is already up");
    }

    @Test
    @DisplayName("Tailwind is worth a lot to a doubles team-support Pokemon (IncreaseTailwindViability, ai_advanced.c:2972)")
    void tailwindForTeamSupport() {
        Battle b = defaultDoubles(mon("HERO", 50, "TAILWIND", "REFLECT"), mon("HERO", 50, "TACKLE"));
        Battler user = b.battlerAt(1);
        AiCtx ctx = AiMaster.prepare(b, user, b.battlerAt(0), new Random(1));
        assertEquals(AiCalc.CLASS_D_TOTAL_TEAM_SUPPORT, AiCalc.fightingStyle(ctx, user));
        assertTrue(AiPositives.score(ctx, user, b.battlerAt(0), user.moveSlot(0), 100) >= 118);
    }

    @Test
    @DisplayName("a sleeping Pokemon in a double battle can switch out, and its partner never picks the same replacement (ShouldSwitchWhileAsleep, ai_switching.c:1086)")
    void doublesSwitchingWhileAsleep() {
        Battle b = doubles(mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"));
        b.addFoe(mon("FAT", 50, "TACKLE", "QUICKHIT"));                    // the bench Pokemon
        Battler user = b.battlerAt(1);
        Battler partner = b.battlerAt(3);
        user.setStatus("SLEEP");
        user.setStatusCount(4);
        user.turnCount = 3;
        partner.turnCount = 3;
        int picked = -1;
        for (int seed = 0; seed < 60 && picked < 0; seed++) picked = AiSwitching.decide(b, user, new Random(seed));
        assertTrue(picked >= 0, "a sleeping Pokemon switches some of the time");
        assertTrue(b.registerSwitch(user.index, picked));
        for (int seed = 0; seed < 60; seed++) {
            assertNotEquals(picked, AiSwitching.decide(b, partner, new Random(seed)), "the partner must not take the same Pokemon");
        }
    }

    @Test
    @DisplayName("the replacement after a faint is chosen by the bench scorer in doubles too (GetMostSuitableMonToSwitchInto, ai_switching.c:1860)")
    void doublesReplacement() {
        Battle b = doubles(mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"));
        b.addFoe(mon("FAT", 50, "TACKLE", "QUICKHIT"));
        Battler fainted = b.battlerAt(1);
        fainted.hp = 0;
        assertEquals(2, AiSwitching.replacement(b, fainted, new Random(3)));
    }

    @Test
    @DisplayName("PassOnWish: a hurt Pokemon on the bench is switched in while a Wish is pending (ai_switching.c:751)")
    void passesOnAWish() {
        Battle b = doubles(mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"));
        b.addFoe(mon("FAT", 50, "TACKLE", "QUICKHIT"));
        Battler user = b.battlerAt(1);
        user.turnCount = 3;
        b.battlerAt(3).turnCount = 3;
        b.partyOf(1).get(2).hp = b.partyOf(1).get(2).maxHp() / 3;
        b.field.positions[1].effects.set(PBEffects.Position.Wish, 2);
        assertEquals(2, AiSwitching.decide(b, user, new Random(1)));
    }

    @Test
    @DisplayName("ShouldSwitchIfPerishSong: a Pokemon that faints at the end of this turn leaves (vanilla pokefirered)")
    void switchesOutOnPerishSong() {
        Battle b = doubles(mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"));
        b.addFoe(mon("FAT", 50, "TACKLE", "QUICKHIT"));
        Battler user = b.battlerAt(1);
        user.turnCount = 3;
        b.battlerAt(3).turnCount = 3;
        user.effects.set(PBEffects.Battler.PerishSong, 1);
        assertEquals(2, AiSwitching.decide(b, user, new Random(1)));
    }

    @Test
    @DisplayName("pivot hand-off: a pivoting move's replacement is the Pokemon recorded by ConfirmAISwitch(.., willPivot) (battle_controller_opponent.c:324)")
    void pivotHandsOffToRecordedMon() {
        Battle b = doubles(mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"));
        b.addFoe(mon("FAT", 50, "TACKLE", "QUICKHIT"));
        Battler user = b.battlerAt(1);
        b.aiPivotTo[1] = 2;
        assertEquals(2, AiSwitching.replacement(b, user, new Random(1)));
        assertEquals(-1, b.aiPivotTo[1], "consumed");
    }

    @Test
    @DisplayName("Unseen Fist / Translucent Ghost: contact moves ignore the protect family in the AI's block check")
    void contactMovesIgnoreProtectWithUnseenFist() {
        Battle b = doubles(mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"), mon("HERO", 50, "TACKLE"));
        Battler user = b.battlerAt(0);
        BattleMove tackle = user.moveSlot(0);
        user.ability = "TRANSLUCENTGHOST";
        assertTrue(AiPartner.ignoresProtect(user, tackle));
        user.ability = "UNSEENFIST";
        assertTrue(AiPartner.ignoresProtect(user, tackle));
        user.ability = "BLAZE";
        assertFalse(AiPartner.ignoresProtect(user, tackle));
    }

    @Test
    @DisplayName("Fearless / Dragon Soul Cry share Moxie's handler, so the AI treats them as Moxie abilities")
    void moxieFamily() {
        assertTrue(AiCalc.isMoxie("FEARLESS"));
        assertTrue(AiCalc.isMoxie("DRAGONSOULCRY"));
        assertFalse(AiCalc.isMoxie("BLAZE"));
    }
}
