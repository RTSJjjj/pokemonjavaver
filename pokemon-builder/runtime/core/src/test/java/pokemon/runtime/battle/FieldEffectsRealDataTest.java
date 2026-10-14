package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Terrains, weathers and the effects tied to them, run with the project's real moves and species:
 * the damage changes, the second Bug Lure hit, status protection, accuracy / charging / healing
 * changes and the end-of-round damage and healing. Skipped without generated/pbs.
 */
class FieldEffectsRealDataTest {

    private static PbsData data;

    private static PbsData data() {
        if (data != null) return data;
        for (File d = new File("").getAbsoluteFile(); d != null; d = d.getParentFile()) {
            for (String candidate : new String[] {"generated", "pokemon-builder/generated"}) {
                File root = new File(d, candidate);
                if (new File(root, "pbs").isDirectory()) return data = PbsData.parse(root);
            }
        }
        assumeTrue(false, "generated/pbs not found");
        return null;
    }

    /** The top damage roll every time and never a critical hit (a critical needs nextInt(c) == 0). */
    private static final class TopRoll extends Random {
        @Override public int nextInt(int bound) { return bound - 1; }
        @Override public float nextFloat() { return 0.99f; }
        @Override public double nextDouble() { return 0.99; }
    }

    /** Level-100 user against a level-100 target, both without ability or item. */
    private static Battle battle(String user, String target) {
        PbsData d = data();
        Battle b = new Battle(d, new TopRoll(), (u, t, moves) -> 0)
                .addPlayer(new Pokemon(d.species(user), 100, d))
                .addFoe(new Pokemon(d.species(target), 100, d));
        for (Battler x : new Battler[] {b.player(), b.foe()}) {
            x.ability = "";
            x.item = "";
        }
        return b;
    }

    private static BattleMove move(String id) {
        PbsData.Move m = data().moves.get(id);
        assertNotNull(m, id);
        return new BattleMove(m);
    }

    private static MoveEffect fx(BattleMove m) { return MoveEffectRegistry.of(m.function()); }

    /** One hit of {@code moveId} through the full pbCalcDamage path. */
    private static int damage(Battle b, String moveId) {
        BattleMove m = move(moveId);
        b.foe().damageState.reset();
        fx(m).pbCalcDamage(m, b.player(), b.foe(), 1);
        return b.foe().damageState.calcDamage;
    }

    private static void weather(Battle b, int w) { b.field.weather = w; b.field.weatherDuration = 5; }
    private static void terrain(Battle b, int t) { b.field.terrain = t; b.field.terrainDuration = 5; }

    private static void assertRatio(double expected, int with, int without, String what) {
        double ratio = with / (double) without;
        assertEquals(expected, ratio, 0.05, what + ": " + with + " vs " + without);
    }

    // ------------------------------------------------------------------ terrains

    @Test
    @DisplayName("Electric / Grassy / Psychic Terrain raise their type by 30%; Misty halves Dragon")
    void terrainDamage() {
        Battle b = battle("SNORLAX", "SNORLAX");
        int elec = damage(b, "THUNDERSHOCK"), grass = damage(b, "VINEWHIP"), psy = damage(b, "CONFUSION");
        int dragon = damage(b, "DRAGONBREATH");
        terrain(b, PBBattleTerrains.Electric);
        assertRatio(1.3, damage(b, "THUNDERSHOCK"), elec, "Electric Terrain");
        terrain(b, PBBattleTerrains.Grassy);
        assertRatio(1.3, damage(b, "VINEWHIP"), grass, "Grassy Terrain");
        terrain(b, PBBattleTerrains.Psychic);
        assertRatio(1.3, damage(b, "CONFUSION"), psy, "Psychic Terrain");
        terrain(b, PBBattleTerrains.Misty);
        assertRatio(0.5, damage(b, "DRAGONBREATH"), dragon, "Misty Terrain");
    }

    @Test
    @DisplayName("Bug Lure Terrain: a grounded user's Bug move hits twice, x1.3, the second hit at 1/4")
    void bugLure() {
        Battle b = battle("SNORLAX", "SNORLAX");
        BattleMove m = move("XSCISSOR");
        Array<Battler> targets = new Array<>();
        targets.add(b.foe());
        int plain = damage(b, "XSCISSOR");
        assertEquals(1, fx(m).pbNumHits(m, b.player(), targets));
        terrain(b, PBBattleTerrains.BugLure);
        assertEquals(2, fx(m).pbNumHits(m, b.player(), targets), "two hits on Bug Lure Terrain");
        assertEquals(3, b.player().effects.intVal(PBEffects.Battler.ParentalBond), "the Parental Bond counter is borrowed");
        b.player().effects.set(PBEffects.Battler.ParentalBond, 2);       // first hit (pbProcessMoveHit counts down)
        assertRatio(1.3, damage(b, "XSCISSOR"), plain, "first hit");
        b.player().effects.set(PBEffects.Battler.ParentalBond, 1);       // second hit
        assertRatio(1.3 / 4, damage(b, "XSCISSOR"), plain, "second hit");
        b.player().effects.set(PBEffects.Battler.ParentalBond, 0);
        assertEquals(1, fx(move("TACKLE")).pbNumHits(move("TACKLE"), b.player(), targets), "non-Bug moves hit once");
        b.player().ability = "LEVITATE";
        assertEquals(1, fx(m).pbNumHits(m, b.player(), targets), "an airborne user is not affected");
    }

    @Test
    @DisplayName("Cold Terrain: grounded Ice x1.3, Fighting x0.5, non-Ice battlers lose 1/16 at the end of round")
    void coldTerrain() {
        Battle b = battle("SNORLAX", "SNORLAX");
        int ice = damage(b, "ICEBEAM"), fight = damage(b, "KARATECHOP");
        terrain(b, PBBattleTerrains.Cold);
        assertRatio(1.3, damage(b, "ICEBEAM"), ice, "Ice on Cold Terrain");
        assertRatio(0.5, damage(b, "KARATECHOP"), fight, "Fighting on Cold Terrain");
        b.pbCalculatePriority(true, null);
        int before = b.foe().hp;
        b.pbEndOfRoundPhase();
        assertEquals(before - b.foe().maxHp() / 16, b.foe().hp, "the biting cold");
    }

    @Test
    @DisplayName("Electric Terrain blocks sleep, Misty Terrain blocks status, Grassy Terrain heals 1/16")
    void terrainProtectionAndHealing() {
        Battle b = battle("SNORLAX", "SNORLAX");
        assertTrue(b.foe().pbCanSleep(b.player(), false, move("SPORE"), false));
        terrain(b, PBBattleTerrains.Electric);
        assertFalse(b.foe().pbCanSleep(b.player(), false, move("SPORE"), false), "Electric Terrain");
        terrain(b, PBBattleTerrains.Misty);
        assertFalse(b.foe().pbCanParalyze(b.player(), false, move("THUNDERWAVE")), "Misty Terrain");
        terrain(b, PBBattleTerrains.Grassy);
        b.foe().hp = b.foe().maxHp() / 2;
        int before = b.foe().hp;
        b.pbCalculatePriority(true, null);
        b.pbEndOfRoundPhase();
        assertEquals(before + b.foe().maxHp() / 16, b.foe().hp, "Grassy Terrain healing");
    }

    // ------------------------------------------------------------------ weather

    @Test
    @DisplayName("Sun: Fire x1.5, Water x0.5; Rain: Water x1.5, Fire x0.5")
    void weatherDamage() {
        Battle b = battle("SNORLAX", "SNORLAX");
        int fire = damage(b, "EMBER"), water = damage(b, "WATERGUN");
        weather(b, PBWeather.Sun);
        assertRatio(1.5, damage(b, "EMBER"), fire, "Fire in sun");
        assertRatio(0.5, damage(b, "WATERGUN"), water, "Water in sun");
        weather(b, PBWeather.Rain);
        assertRatio(1.5, damage(b, "WATERGUN"), water, "Water in rain");
        assertRatio(0.5, damage(b, "EMBER"), fire, "Fire in rain");
    }

    @Test
    @DisplayName("Sandstorm: Rock types take 2/3 from special moves; Snow: Ice types take 2/3")
    void weatherDefense() {
        Battle b = battle("SNORLAX", "GOLEM");
        int water = damage(b, "WATERGUN");
        weather(b, PBWeather.Sandstorm);
        assertRatio(1 / 1.5, damage(b, "WATERGUN"), water, "Rock in a sandstorm");
        Battle c = battle("SNORLAX", "GLALIE");
        int psy = damage(c, "CONFUSION");
        weather(c, PBWeather.Snow);
        assertRatio(1 / 1.5, damage(c, "CONFUSION"), psy, "Ice in snow");
    }

    @Test
    @DisplayName("Sandstorm and hail chip 1/16 at the end of round")
    void weatherChip() {
        for (int w : new int[] {PBWeather.Sandstorm, PBWeather.Hail}) {
            Battle b = battle("SNORLAX", "SNORLAX");
            weather(b, w);
            b.pbCalculatePriority(true, null);
            int before = b.foe().hp;
            b.pbEndOfRoundPhase();
            assertEquals(before - b.foe().maxHp() / 16, b.foe().hp, "weather " + w);
        }
    }

    @Test
    @DisplayName("Thunder / Hurricane never miss in rain and drop to 50 in sun; Blizzard never misses in hail")
    void weatherAccuracy() {
        Battle b = battle("SNORLAX", "SNORLAX");
        BattleMove thunder = move("THUNDER"), hurricane = move("HURRICANE"), blizzard = move("BLIZZARD");
        weather(b, PBWeather.Rain);
        assertEquals(0, fx(thunder).pbBaseAccuracy(thunder, b.player(), b.foe()), "Thunder in rain");
        assertEquals(0, fx(hurricane).pbBaseAccuracy(hurricane, b.player(), b.foe()), "Hurricane in rain");
        weather(b, PBWeather.Sun);
        assertEquals(50, fx(thunder).pbBaseAccuracy(thunder, b.player(), b.foe()), "Thunder in sun");
        weather(b, PBWeather.Hail);
        assertEquals(0, fx(blizzard).pbBaseAccuracy(blizzard, b.player(), b.foe()), "Blizzard in hail");
    }

    @Test
    @DisplayName("Solar Beam skips its charge in sun; Weather Ball turns Fire/100 in sun; Synthesis heals 2/3 in sun")
    void weatherMoves() {
        Battle b = battle("SNORLAX", "SNORLAX");
        BattleMove solar = move("SOLARBEAM"), ball = move("WEATHERBALL"), synth = move("SYNTHESIS");
        weather(b, PBWeather.Sun);
        assertFalse(fx(solar).pbIsChargingTurn(solar, b.player()), "Solar Beam in sun");
        assertEquals("FIRE", fx(ball).pbBaseType(ball, b.player()), "Weather Ball type in sun");
        assertEquals(100, fx(ball).pbBaseDamage(ball, 50, b.player(), b.foe()), "Weather Ball power in sun");
        fx(synth).pbOnStartUse(synth, b.player(), new Array<>());                  // sets @healAmount from the weather
        assertEquals(Math.round(b.player().maxHp() * 2 / 3.0), fx(synth).pbHealAmount(synth, b.player()), 1, "Synthesis in sun");
        weather(b, PBWeather.Rain);
        assertEquals("WATER", fx(ball).pbBaseType(ball, b.player()), "Weather Ball type in rain");
    }

    @Test
    @DisplayName("Swift Swim / Chlorophyll / Sand Rush / Slush Rush double Speed in their weather")
    void weatherSpeed() {
        Object[][] cases = {
                {"SWIFTSWIM", PBWeather.Rain}, {"CHLOROPHYLL", PBWeather.Sun},
                {"SANDRUSH", PBWeather.Sandstorm}, {"SLUSHRUSH", PBWeather.Hail}};
        for (Object[] c : cases) {
            Battle b = battle("SNORLAX", "SNORLAX");
            b.player().ability = (String) c[0];
            int plain = b.player().pbSpeed();
            weather(b, (Integer) c[1]);
            assertEquals(plain * 2, b.player().pbSpeed(), 1, (String) c[0]);
        }
    }

    @Test
    @DisplayName("Rain Dish / Ice Body heal 1/16, Dry Skin heals 1/8 in rain, Solar Power costs 1/8 in sun")
    void weatherAbilitiesAtEndOfRound() {
        Object[][] cases = {
                {"RAINDISH", PBWeather.Rain, 16}, {"ICEBODY", PBWeather.Hail, 16},
                {"DRYSKIN", PBWeather.Rain, 8}, {"SOLARPOWER", PBWeather.Sun, -8}};
        for (Object[] c : cases) {
            Battle b = battle("SNORLAX", "SNORLAX");
            b.player().ability = (String) c[0];
            b.player().hp = b.player().maxHp() / 2;
            weather(b, (Integer) c[1]);
            b.pbCalculatePriority(true, null);
            int before = b.player().hp;
            b.pbEndOfRoundPhase();
            int div = (Integer) c[2];
            int expected = before + (div > 0 ? b.player().maxHp() / div : -b.player().maxHp() / -div);
            assertEquals(expected, b.player().hp, (String) c[0]);
        }
    }

    // ------------------------------------------------------------------ damage abilities

    /** Damage with the user's / the target's ability set, relative to the same hit without it. */
    private static double abilityRatio(Battle b, String side, String ability, String moveId) {
        Battler holder = "user".equals(side) ? b.player() : b.foe();
        String old = holder.ability;
        holder.ability = "";
        int without = damage(b, moveId);
        holder.ability = ability;
        int with = damage(b, moveId);
        holder.ability = old;
        return with / (double) without;
    }

    /** A plain damaging move of {@code type} (function 000) from the project's move list. */
    private static String plainMoveOfType(String type) {
        String best = null;
        boolean bestPlain = false;
        for (PbsData.Move m : data().moves.values()) {
            if (!type.equals(m.type) || m.power < 40 || "Status".equals(m.category)) continue;
            // a plain hit, or one whose only extra is a status / flinch chance (003-013, 00F)
            boolean plain = "000".equals(m.function);
            boolean chance = m.function != null && m.function.matches("00[3-9A-F]|01[0-3]");
            if (!plain && !chance) continue;
            if (best == null || (plain && !bestPlain) || (plain == bestPlain && m.internalName.compareTo(best) < 0)) {
                best = m.internalName;
                bestPlain = plain;
            }
        }
        return best;
    }

    @Test
    @DisplayName("every ability whose only condition is the move type boosts by its multiplier (extracted from the plugin)")
    void typeOnlyAbilities() {
        // {side, ability, multiplier slot, factor, type} - extracted from 103_BattleHandlers_Abilities.rb
        String[][] cases = {
            {"user", "STEELWORKER", "ATK_MULT", "1.5", "STEEL"},
            {"user", "WATERBUBBLE", "ATK_MULT", "2.0", "WATER"},
            {"user", "STEELYSPIRIT", "BASE_DMG_MULT", "1.5", "STEEL"},
            {"user", "DRAGONSMAW", "ATK_MULT", "1.5", "DRAGON"},
            {"user", "TRANSISTOR", "ATK_MULT", "1.5", "ELECTRIC"},
            {"target", "DRYSKIN", "BASE_DMG_MULT", "1.25", "FIRE"},
            {"user", "ROCKYPAYLOAD", "ATK_MULT", "1.5", "ROCK"},
            {"user", "EERIEBODY", "ATK_MULT", "1.3", "DARK"},
            {"user", "ZEROSUMBODY", "ATK_MULT", "1.3", "LIGHT"},
            {"user", "STARFISSURE", "ATK_MULT", "1.3", "VOID"},
            {"user", "SOUNDSTRIDE", "ATK_MULT", "1.3", "BUG"},
            {"user", "ENERGYACCU", "BASE_DMG_MULT", "1.3", "STEEL"},
            {"user", "CONFLUENCE", "BASE_DMG_MULT", "1.3", "WATER"},
            {"user", "FIREMANE", "ATK_MULT", "1.5", "FIRE"},
        };
        StringBuilder wrong = new StringBuilder();
        for (String[] c : cases) {
            // VOID has no plain hit; Void Smash (10A, Brick Break) has no damage effect of its own
            String moveId = "VOID".equals(c[4]) ? "VOIDSMASH" : plainMoveOfType(c[4]);
            if (moveId == null) { wrong.append(c[1]).append(": no plain ").append(c[4]).append(" move; "); continue; }
            double ratio = abilityRatio(battle("SNORLAX", "SNORLAX"), c[0], c[1], moveId);
            if (Math.abs(ratio - Double.parseDouble(c[3])) > 0.05) {
                wrong.append(String.format("%s x%.3f with %s (expected x%s); ", c[1], ratio, moveId, c[3]));
            }
        }
        assertEquals("", wrong.toString(), "abilities not giving their boost");
    }

    @Test
    @DisplayName("the project's own conditional damage abilities give their boost (Thunder Cloud, Rose Garden, Wanxiang ...)")
    void projectAbilities() {
        Battle b = battle("SNORLAX", "SNORLAX");
        weather(b, PBWeather.Rain);
        assertEquals(1.5, abilityRatio(b, "user", "THUNDERCLOUD", "THUNDERSHOCK"), 0.05, "Thunder Cloud, Electric in rain");
        weather(b, PBWeather.None);
        assertEquals(1.0, abilityRatio(b, "user", "THUNDERCLOUD", "THUNDERSHOCK"), 0.05, "Thunder Cloud without rain");

        terrain(b, PBBattleTerrains.Grassy);
        assertEquals(1.5, abilityRatio(b, "user", "ROSEGARDEN", "EMBER"), 0.05, "Rose Garden, Fire on Grassy Terrain");
        assertEquals(1.0, abilityRatio(b, "user", "ROSEGARDEN", "ICEBEAM"), 0.05, "Rose Garden does not boost Ice");
        assertEquals(1 / 1.5, abilityRatio(b, "target", "VERDANTWARD", "TACKLE"), 0.05, "Verdant Ward on Grassy Terrain");
        terrain(b, PBBattleTerrains.None);

        assertEquals(2.0, abilityRatio(b, "user", "WANXIANG", "TACKLE"), 0.05, "Wanxiang, physical at full HP");
        b.player().hp = b.player().maxHp() / 2;
        assertEquals(1.0, abilityRatio(b, "user", "WANXIANG", "TACKLE"), 0.05, "Wanxiang, physical at half HP");
        assertEquals(0.5, abilityRatio(b, "user", "WANXIANG", "ICEBEAM"), 0.05, "Wanxiang, special at half HP");
        b.player().hp = b.player().maxHp();

        // Fairy Song also turns sound moves Fairy-type, so the user must not have a Normal STAB to lose
        assertEquals(1.2, abilityRatio(battle("GOLEM", "SNORLAX"), "user", "FAIRYSONG", "HYPERVOICE"), 0.05, "Fairy Song, sound move");
        assertEquals(1.5, abilityRatio(b, "user", "GHASTLYWAIL", "HYPERVOICE"), 0.05, "Ghastly Wail, sound move");
        assertEquals(1.3, abilityRatio(b, "user", "PUNKROCK", "HYPERVOICE"), 0.05, "Punk Rock, sound move");
        assertEquals(0.5, abilityRatio(b, "target", "PUNKROCK", "HYPERVOICE"), 0.05, "Punk Rock on the target");
        assertEquals(2.0, abilityRatio(b, "user", "DEMONKILLER", "TACKLE"), 0.05, "Demon Killer, contact move");
        assertEquals(1.3, abilityRatio(b, "user", "SOARINGSTAGE", "TACKLE"), 0.05, "Soaring Stage");
        assertEquals(1.5, abilityRatio(b, "user", "GORILLATACTICS", "TACKLE"), 0.05, "Gorilla Tactics");
        assertEquals(2.0, abilityRatio(b, "user", "BROKENBREATH", "ICEBEAM"), 0.05, "Broken Breath, user");
        assertEquals(2.0, abilityRatio(b, "target", "BROKENBREATH", "ICEBEAM"), 0.05, "Broken Breath halves the target's Sp. Def");
        weather(b, PBWeather.Sun);
        assertEquals(1.5, abilityRatio(b, "user", "FLOWERGIFT", "TACKLE"), 0.05, "Flower Gift, physical in sun");
        weather(b, PBWeather.None);

        Battle rock = battle("SNORLAX", "GOLEM");
        assertEquals(2.0, abilityRatio(rock, "user", "RUYIBLADE", "TACKLE"), 0.05, "Ruyi Blade against a resisted hit");
    }

    @Test
    @DisplayName("Ruin-type abilities: Turboblaze lowers the others' Sp. Def, Teravolt their Defense (per their descriptions)")
    void ruinAbilities() {
        Battle b = battle("SNORLAX", "SNORLAX");
        assertEquals(1 / 0.75, abilityRatio(b, "user", "TURBOBLAZE", "ICEBEAM"), 0.05, "Turboblaze, special hit");
        assertEquals(1.0, abilityRatio(b, "user", "TURBOBLAZE", "TACKLE"), 0.05, "Turboblaze leaves Defense alone");
        assertEquals(1 / 0.75, abilityRatio(b, "user", "TERAVOLT", "TACKLE"), 0.05, "Teravolt, physical hit");
        assertEquals(1.0, abilityRatio(b, "user", "TERAVOLT", "ICEBEAM"), 0.05, "Teravolt leaves Sp. Def alone");
        assertEquals(1 / 0.75, abilityRatio(b, "user", "SWORDOFRUIN", "TACKLE"), 0.05, "Sword of Ruin");
    }

    @Test
    @DisplayName("Clear Heart and Eternal Star each heal 1/8 at the end of round")
    void eorHealingAbilities() {
        for (String ability : new String[] {"CLEARHEART", "ETERNALSTAR"}) {
            Battle b = battle("SNORLAX", "SNORLAX");
            b.player().ability = ability;
            b.player().hp = b.player().maxHp() / 2;
            b.pbCalculatePriority(true, null);
            int before = b.player().hp;
            b.pbEndOfRoundPhase();
            assertEquals(before + b.player().maxHp() / 8, b.player().hp, ability);
        }
    }

    @Test
    @DisplayName("Solar Beam is halved in rain and sandstorm (the plugin's rounding had made it x1)")
    void solarBeamHalved() {
        Battle b = battle("SNORLAX", "SNORLAX");
        BattleMove solar = move("SOLARBEAM");
        weather(b, PBWeather.Rain);
        assertEquals(0.5f, fx(solar).pbBaseDamageMultiplier(solar, 1f, b.player(), b.foe()), 0.001f, "rain");
        weather(b, PBWeather.Sandstorm);
        assertEquals(0.5f, fx(solar).pbBaseDamageMultiplier(solar, 1f, b.player(), b.foe()), 0.001f, "sandstorm");
        weather(b, PBWeather.Sun);
        assertEquals(1f, fx(solar).pbBaseDamageMultiplier(solar, 1f, b.player(), b.foe()), 0.001f, "sun");
    }

    @Test
    @DisplayName("Cloud Nine / Air Lock cancel the weather (pbWeather used to ignore them)")
    void cloudNine() {
        for (String ability : new String[] {"CLOUDNINE", "AIRLOCK"}) {
            Battle b = battle("SNORLAX", "SNORLAX");
            weather(b, PBWeather.Sun);
            assertEquals(PBWeather.Sun, b.pbWeather());
            b.foe().ability = ability;
            assertEquals(PBWeather.None, b.pbWeather(), ability);
            // the sun's x1.5 on Fire is gone with the ability on the field
            assertEquals(1 / 1.5, abilityRatio(b, "target", ability, "EMBER"), 0.05, ability + " removes the sun boost");
        }
    }

    @Test
    @DisplayName("pbCanRun: Mean Look / Ingrain / Shadow Tag trap, Run Away and Smoke Ball always escape")
    void canRun() {
        Battle b = battle("SNORLAX", "SNORLAX");
        assertTrue(b.pbCanRun(b.player().index));
        b.player().effects.set(PBEffects.Battler.MeanLook, b.foe().index);
        assertFalse(b.pbCanRun(b.player().index), "Mean Look");
        b.player().ability = "RUNAWAY";
        assertTrue(b.pbCanRun(b.player().index), "Run Away escapes anyway");
        b.player().ability = "";
        b.player().effects.set(PBEffects.Battler.MeanLook, -1);
        b.player().effects.set(PBEffects.Battler.Ingrain, true);
        assertFalse(b.pbCanRun(b.player().index), "Ingrain");
        b.player().effects.set(PBEffects.Battler.Ingrain, false);
        b.foe().ability = "SHADOWTAG";
        assertFalse(b.pbCanRun(b.player().index), "Shadow Tag");
        b.player().item = "SMOKEBALL";
        assertTrue(b.pbCanRun(b.player().index), "Smoke Ball escapes anyway");
    }

    @Test
    @DisplayName("changeEVandNature: 252/252 in the two best stats (HP excluded), 6 HP EVs, a fitting nature")
    void changeEvAndNature() {
        PbsData d = data();
        Pokemon garchomp = new Pokemon(d.species("GARCHOMP"), 50, d);   // base 108/130/95/102/80/85: Atk, then Spe
        java.util.Arrays.fill(garchomp.evs, 85);
        assertTrue(ChangeEvAndNature.hasPlaceholderEvs(garchomp));
        ChangeEvAndNature.apply(garchomp, d, false);
        assertArrayEquals(new int[] {6, 252, 0, 252, 0, 0}, garchomp.evs);
        assertEquals("ADAMANT", garchomp.nature.internalName, "Atk first, Spe second (not SpA): :23-24 Adamant");
        assertFalse(ChangeEvAndNature.hasPlaceholderEvs(garchomp), "a tuned spread is left alone next time");

        Pokemon tuned = new Pokemon(d.species("GARCHOMP"), 50, d);
        tuned.evs = new int[] {4, 252, 0, 0, 0, 252};
        assertFalse(ChangeEvAndNature.hasPlaceholderEvs(tuned), "a hand-set spread is kept");
        Pokemon lev = new Pokemon(d.species("GARCHOMP"), 51, d);
        java.util.Arrays.fill(lev.evs, 77);                              // (51 * 1.5).round = 77 (76.5 rounds up)
        assertTrue(ChangeEvAndNature.hasPlaceholderEvs(lev));
    }

    // ------------------------------------------------------------------ whole move use (pbUseMove)

    /** The user knows only {@code moveId}; uses it once against the foe and returns the hits the log reports. */
    private static int hitsWhenUsing(String userAbility, int terrain, String moveId) {
        PbsData d = data();
        Pokemon mine = new Pokemon(d.species("SNORLAX"), 100, d);
        mine.moves.clear();
        mine.moves.add(new Pokemon.MoveSlot(d.moves.get(moveId)));
        Battle b = new Battle(d, new Random(3), (u, t, moves) -> 0)
                .addPlayer(mine).addFoe(new Pokemon(d.species("SNORLAX"), 100, d));
        b.badges = new java.util.HashSet<>();
        b.player().initEffects(false);
        b.foe().initEffects(false);
        b.player().ability = userAbility;
        b.foe().ability = "";
        b.player().item = "";
        b.foe().item = "";
        terrain(b, terrain);
        int hpBefore = b.foe().hp;
        b.player().pbUseMove(new Object[] {":UseMove", 0, b.player().moveSlot(0), -1}, false);
        assertTrue(b.foe().hp < hpBefore, moveId + " did damage");
        for (String m : b.roundMessages) {
            java.util.regex.Matcher x = java.util.regex.Pattern.compile("击中了(\\d+)次").matcher(m);
            if (x.find()) return Integer.parseInt(x.group(1));
        }
        return 1;                                                       // a single hit prints no count
    }

    @Test
    @DisplayName("using a move for real: Parental Bond hits twice; a Bug move hits twice on Bug Lure Terrain")
    void doubleHitsEndToEnd() {
        assertEquals(1, hitsWhenUsing("", PBBattleTerrains.None, "TACKLE"), "plain Tackle");
        assertEquals(2, hitsWhenUsing("PARENTALBOND", PBBattleTerrains.None, "TACKLE"), "Parental Bond");
        assertEquals(1, hitsWhenUsing("", PBBattleTerrains.None, "XSCISSOR"), "X-Scissor without terrain");
        assertEquals(2, hitsWhenUsing("", PBBattleTerrains.BugLure, "XSCISSOR"), "X-Scissor on Bug Lure Terrain");
        assertEquals(1, hitsWhenUsing("", PBBattleTerrains.BugLure, "TACKLE"), "a non-Bug move on Bug Lure Terrain");
        assertEquals(2, hitsWhenUsing("PARENTALBOND", PBBattleTerrains.BugLure, "XSCISSOR"),
                "Parental Bond on Bug Lure Terrain stays at two hits (the terrain only adds to single-hit moves)");
    }

    @Test
    @DisplayName("round events remember the terrain, so the screen keeps a terrain that ends this round until its message")
    void terrainSnapshotInRoundEvents() {
        Battle b = battle("SNORLAX", "SNORLAX");
        b.field.terrain = PBBattleTerrains.BugLure;
        b.field.terrainDuration = 1;                                      // ends at this end of round
        b.pbCalculatePriority(true, null);
        b.roundEvents.clear();
        b.pbEndOfRoundPhase();
        assertEquals(PBBattleTerrains.None, b.field.terrain, "the engine already ended it");
        assertEquals(PBBattleTerrains.BugLure, b.roundBaseTerrain, "the round started with it");
        int ending = -1;
        for (int i = 0; i < b.roundEvents.size; i++) {
            Battle.RoundEvent e = b.roundEvents.get(i);
            if (e.text != null && e.text.contains("笼罩场地的虫群消散了")) { ending = i; break; }
        }
        assertTrue(ending >= 0, "the ending message is in the round");
        for (int i = 0; i <= ending; i++) {
            assertEquals(PBBattleTerrains.BugLure, b.roundEvents.get(i).terrain, "event " + i + " still has the terrain");
        }
        for (int i = ending + 1; i < b.roundEvents.size; i++) {
            assertEquals(PBBattleTerrains.None, b.roundEvents.get(i).terrain, "event " + i + " after the message");
        }
    }

    @Test
    @DisplayName("Solar Power raises Sp. Atk 1.5x in sun (not 2x)")
    void solarPower() {
        Battle b = battle("SNORLAX", "SNORLAX");
        b.player().ability = "SOLARPOWER";
        int plain = damage(b, "WATERGUN");
        weather(b, PBWeather.Sun);
        // Water is halved in sun, Solar Power gives x1.5: x0.75 overall
        assertRatio(0.75, damage(b, "WATERGUN"), plain, "Solar Power");
    }
}
