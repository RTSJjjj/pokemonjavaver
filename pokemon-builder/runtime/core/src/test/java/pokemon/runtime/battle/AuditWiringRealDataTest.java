package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The 2026-10-14 plugin audit: calls that used to be stubs ("provably false", "not landed") although the method
 * they needed existed, run with the project's real moves and species. Skipped without generated/pbs.
 */
class AuditWiringRealDataTest {

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

    /** The top damage roll every time and never a critical hit. */
    private static final class TopRoll extends Random {
        @Override public int nextInt(int bound) { return bound - 1; }
        @Override public float nextFloat() { return 0.99f; }
        @Override public double nextDouble() { return 0.99; }
    }

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

    private static int damage(Battle b, String moveId) {
        BattleMove m = move(moveId);
        b.foe().damageState.reset();
        fx(m).pbCalcDamage(m, b.player(), b.foe(), 1);
        return b.foe().damageState.calcDamage;
    }

    private static void charging(Battler b, String moveId) {
        b.effects.set(PBEffects.Battler.TwoTurnAttack, move(moveId).id());
    }

    @Test
    @DisplayName("Mold Breaker: Clear Body stops a stat drop only while battle.moldBreaker is off")
    void moldBreakerReachesBattlerChecks() {
        Battle b = battle("PIKACHU", "METAGROSS");
        b.foe().ability = "CLEARBODY";
        b.moldBreaker = false;
        assertFalse(b.foe().pbCanLowerStatStage(PBStats.ATTACK, b.player(), null, false, false));
        b.moldBreaker = true;
        assertTrue(b.foe().pbCanLowerStatStage(PBStats.ATTACK, b.player(), null, false, false));
    }

    @Test
    @DisplayName("Gust doubles against Fly, Earthquake against Dig")
    void twoTurnTargets() {
        Battle b = battle("PIDGEOT", "CHARIZARD");
        int gust = damage(b, "GUST");
        charging(b.foe(), "FLY");
        assertEquals(2.0, damage(b, "GUST") / (double) gust, 0.05);

        Battle e = battle("GARCHOMP", "DUGTRIO");
        int quake = damage(e, "EARTHQUAKE");
        charging(e.foe(), "DIG");
        assertEquals(2.0, damage(e, "EARTHQUAKE") / (double) quake, 0.05);
    }

    @Test
    @DisplayName("Gravity pulls a Fly user down; Smack Down cancels Fly")
    void gravityAndSmackDown() {
        Battle b = battle("CLEFABLE", "CHARIZARD");
        charging(b.foe(), "FLY");
        BattleMove gravity = move("GRAVITY");
        fx(gravity).pbEffectGeneral(gravity, b.player());
        assertEquals(0, b.foe().effects.intVal(PBEffects.Battler.TwoTurnAttack));

        Battle s = battle("TYRANITAR", "CHARIZARD");
        charging(s.foe(), "FLY");
        BattleMove smack = move("SMACKDOWN");
        s.foe().damageState.reset();
        fx(smack).pbEffectAfterAllHits(smack, s.player(), s.foe());
        assertTrue(s.foe().effects.truthy(PBEffects.Battler.SmackDown));
        assertEquals(0, s.foe().effects.intVal(PBEffects.Battler.TwoTurnAttack));
    }

    @Test
    @DisplayName("Future Sight deals no damage on its set-up turn, only on the delayed hit")
    void futureSightSetUpTurn() {
        Battle b = battle("ALAKAZAM", "MACHAMP");
        BattleMove fs = move("FUTURESIGHT");
        fx(fs).pbIsChargingTurn(fs, b.player());
        assertFalse(fx(fs).pbDamagingMove(fs));
        b.futureSight = true;
        assertTrue(fx(fs).pbDamagingMove(fs));
        b.futureSight = false;
    }

    @Test
    @DisplayName("Damp Rock makes a rain set by a move last 8 turns; Castform follows the weather")
    void weatherExtenderAndForecast() {
        Battle b = battle("CASTFORM", "MACHAMP");
        b.player().item = "DAMPROCK";
        b.pbStartWeather(b.player(), PBWeather.Rain, true, false);
        assertEquals(8, b.field.weatherDuration);

        Battle c = battle("CASTFORM", "MACHAMP");
        c.player().ability = "FORECAST";
        c.pbStartWeather(null, PBWeather.Sun, true, false);
        assertEquals(1, c.player().form());
    }

    @Test
    @DisplayName("Frostbite halves special damage; a drowsy target takes 1.25x")
    void frostbiteAndDrowsy() {
        Battle b = battle("CHARIZARD", "MACHAMP");
        int base = damage(b, "FLAMETHROWER");
        b.player().status = "FROSTBITE";
        assertEquals(0.5, damage(b, "FLAMETHROWER") / (double) base, 0.05);

        Battle d = battle("CHARIZARD", "MACHAMP");
        int normal = damage(d, "FLAMETHROWER");
        d.foe().status = "DROWSY";
        assertEquals(1.25, damage(d, "FLAMETHROWER") / (double) normal, 0.05);
    }

    @Test
    @DisplayName("Held items at the end: a used-up berry comes back (357), an item won in battle is kept")
    void heldItemsAfterBattle() {
        Battle b = battle("PIKACHU", "MACHAMP");
        b.player().item = "SITRUSBERRY";
        b.player().initialItem = "SITRUSBERRY";
        b.player().pokemon.item = "SITRUSBERRY";
        b.player().pbConsumeItem();
        assertEquals("", b.player().item);
        b.pbRestoreHeldItems();
        assertEquals("SITRUSBERRY", b.player().pokemon.item);

        Battle p = battle("MEOWTH", "MACHAMP");
        p.player().pokemon.item = null;
        p.player().item = "POTION";                                           // Pickup found one
        p.player().setInitialItem("POTION");                                  // BattleHandlers_Abilities:2298
        p.pbRestoreHeldItems();
        assertEquals("POTION", p.player().pokemon.item);

        Battle k = battle("PIKACHU", "MACHAMP");
        k.player().pokemon.item = "LIGHTBALL";
        k.player().initialItem = "LIGHTBALL";
        k.player().item = "LIGHTBALL";
        k.player().pbRemoveItem(false);                                       // Knock Off: not permanent
        k.pbRestoreHeldItems();
        assertEquals("LIGHTBALL", k.player().pokemon.item);
    }

    @Test
    @DisplayName("A Pokemon that faints in battle loses happiness")
    void faintHappiness() {
        Battle b = battle("PIKACHU", "MACHAMP");
        Pokemon pkmn = b.player().pokemon;
        pkmn.happiness = 150;
        b.player().pbReduceHP(b.player().hp);
        b.player().pbFaint(false);
        assertEquals(149, pkmn.happiness);                                    // "faint": -1 (197_PokeBattle_Pokemon)
    }
    @Test
    @DisplayName("Attract reads the gender derived from the personal id, not the raw @genderflag")
    void attractUsesTheRealGender() {
        Battle b = battle("PIKACHU", "PIKACHU");
        Pokemon a = b.player().pokemon, f = b.foe().pokemon;
        a.gender = PokemonStats.MALE;
        f.gender = PokemonStats.GENDERLESS;                  // no flag: the personal id decides
        f.personalID = 0;                                     // low byte 0 < the 50% threshold: female
        assertEquals(PokemonStats.FEMALE, b.foe().gender());
        assertTrue(b.foe().pbCanAttract(b.player(), false));
    }
}
