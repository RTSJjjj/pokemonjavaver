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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage 4 / &sect;4 wiring: {@link BattlerHitEffects} actually runs.
 *
 * <p>Before this pass the eight handler tables it calls had <b>no call site
 * anywhere in the project</b> - the handlers were transcribed and registered,
 * but nothing ever invoked them, so Rocky Helmet, Poison Touch and friends were
 * dead code. These tests drive a real {@code battle.step()} and assert the
 * handler bodies' observable effect, so a future edit that drops the call sites
 * fails here instead of silently going quiet.</p>
 *
 * <p>Each test also depends on {@code battle.execute} now filling
 * {@code damageState.calcDamage / hpLost / initialHP}: the plugin's
 * {@code pbEffectsOnMakingHit} gates its whole ability/item block on
 * {@code target.damageState.calcDamage>0} (Battler_UseMove_TriggerEffects:6), so
 * without that wiring these handlers could never fire.</p>
 */
class HitEffectsWiringTest {

    /** HERO (bulky physical attacker, POISONTOUCH) vs FOE (bulky), both with TACKLE. */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":2,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[250,60,150,60,60,60],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"}]}}}");
        // TACKLE carries the /a/ contact flag, which is what both handlers check.
        write(root, "moves.json", "{\"total\":1,\"moves\":{"
                + "\"TACKLE\":{\"id\":1,\"internalName\":\"TACKLE\",\"name\":\"Tackle\","
                + "\"function\":\"000\",\"power\":40,\"type\":\"NORMAL\",\"category\":\"Physical\","
                + "\"accuracy\":100,\"pp\":35,\"flags\":\"a\"}}}");
        write(root, "abilities.json", "{\"total\":1,\"abilities\":{"
                + "\"POISONTOUCH\":{\"id\":1,\"internalName\":\"POISONTOUCH\",\"name\":\"Poison Touch\"}}}");
        write(root, "types.json", "{\"total\":1,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":1,\"items\":{"
                + "\"ROCKYHELMET\":{\"id\":1,\"internalName\":\"ROCKYHELMET\",\"name\":\"Rocky Helmet\"}}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }

    /** {@code PbsData.parse} reads {@code <root>/pbs/*.json} (PbsData.java:684 region). */
    private static void write(Path root, String name, String content) throws Exception {
        Path pbs = root.resolve("pbs");
        Files.createDirectories(pbs);
        Files.write(pbs.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    private static Battle battle(PbsData data, Random random) {
        assertTrue(data.species("HERO") != null,
                "fixture did not parse: species keys = " + data.species.keys().toArray());
        assertTrue(data.move("TACKLE") != null,
                "fixture did not parse: move keys = " + data.moves.keys().toArray());
        Pokemon hero = new Pokemon(data.species("HERO"), 20, data);
        // The fixture's species lists no abilities: PbsData.parse runs readSpecies
        // (PbsData.java:684) before readAbilities (:688), so a name there would be
        // resolved against an empty table. Set it on the Pokemon instead, which is
        // the same field the constructor would have filled.
        hero.ability = "POISONTOUCH";
        return new Battle(data, random, (user, target, moves) -> 0)   // always use slot 0 (TACKLE)
                .addPlayer(hero)
                .addFoe(new Pokemon(data.species("FOE"), 20, data));
    }

    @Test
    @DisplayName("§4: Rocky Helmet (TargetItemOnHit) hurts the attacker on a contact hit")
    void rockyHelmetHurtsTheAttacker(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, new Random(7));
        // BattleHandlers_Items.rb:1134-1146 reads the *target's* held item.
        battle.foe().item = "ROCKYHELMET";
        int attackerMax = battle.player().maxHp();

        battle.step();

        assertTrue(battle.player().hp < attackerMax,
                "Rocky Helmet did not fire: attacker " + battle.player().hp + "/" + attackerMax);
    }

    @Test
    @DisplayName("§4: Poison Touch (UserAbilityOnHit) poisons the target on a contact hit")
    void poisonTouchPoisonsOnContact(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, new Random(11));

        // The handler is looked up by the Battler's ability string, so make sure the
        // Pokemon's ability actually reached the Battler.
        assertEquals("POISONTOUCH", battle.player().ability,
                "ability did not propagate from the Pokemon to the Battler");
        // The handler only fires on 30% of contact hits (BattleHandlers_Abilities:1872
        // `battle.pbRandom(100) >= 30`), so give it several turns rather than pin one seed.
        for (int turn = 0; turn < 12 && !"POISON".equals(battle.foe().status); turn++) {
            battle.step();
        }

        assertEquals("POISON", battle.foe().status,
                "Poison Touch did not fire in 12 turns: foe status is " + battle.foe().status);
    }

    @Test
    @DisplayName("§4: damageState is filled, so the on-hit gate (calcDamage>0) can open")
    void damageStateIsFilled(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, new Random(13));

        battle.step();

        // Battler_UseMove_TriggerEffects:6 gates on these; Move_Usage:252 sets initialHP.
        assertTrue(battle.foe().damageState.calcDamage > 0,
                "damageState.calcDamage should hold the computed damage");
        assertTrue(battle.foe().damageState.hpLost > 0, "damageState.hpLost should hold the HP lost");
        assertTrue(battle.foe().damageState.initialHP > 0, "damageState.initialHP should be the pre-hit HP");
    }
}
