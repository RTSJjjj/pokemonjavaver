package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.pokemon.WildGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Robustness sweep of the trainer AI on the project's real data: every trainer of {@code generated/pbs} fights a random team in
 * a headless battle, with some of the Pokemon damaged the way bad data can be (blank move slots, no moves at all, no item,
 * no ability). Any exception that escapes the battle is a crash waiting in the game; they are grouped by where they are thrown.
 * Off by default (it takes a minute): run with {@code -Dpokemon.fuzz=true} (or {@code POKEMON_FUZZ=1}); {@code -Dpokemon.fuzz.rounds=N}
 * sets the battles per trainer, {@code -XX:-OmitStackTraceInFastThrow} keeps every stack trace. Skipped when the generated data
 * is not there.
 */
class AiFuzzRealDataTest {

    private static File generatedRoot() {
        String property = System.getProperty("pokemon.generated");
        if (property != null) return new File(property);
        for (File dir = new File("").getAbsoluteFile(); dir != null; dir = dir.getParentFile()) {
            File candidate = new File(dir, "generated");
            if (new File(candidate, "pbs").isDirectory()) return candidate;
            File nested = new File(dir, "pokemon-builder/generated");
            if (new File(nested, "pbs").isDirectory()) return nested;
        }
        return null;
    }

    /** Blank move slots, a Pokemon with no moves, no item / ability: what incomplete trainer data can produce. */
    private static void damage(Pokemon p, Random random) {
        int roll = random.nextInt(100);
        if (roll < 20 && p.moves.size > 1) {
            p.moves.removeIndex(random.nextInt(p.moves.size));        // a gap: the slots after it shift, the last one is blank
        } else if (roll < 26) {
            p.moves.clear();                                          // no moves at all
        } else if (roll < 36) {
            p.item = null;
        } else if (roll < 44) {
            p.ability = null;
        }
    }

    @Test
    @DisplayName("no exception escapes a trainer battle on the real data (CFRU AI sweep)")
    void sweep() {
        assumeTrue(Boolean.getBoolean("pokemon.fuzz") || System.getenv("POKEMON_FUZZ") != null, "set -Dpokemon.fuzz=true to run the sweep");
        File root = generatedRoot();
        assumeTrue(root != null, "generated/pbs not found");
        PbsData data = PbsData.parse(root);
        assumeTrue(data.trainers.size > 0 && data.species.size > 0, "no trainers in the PBS");

        List<PbsData.Species> pool = new ArrayList<>();
        for (PbsData.Species species : data.species.values()) {
            if (species.baseStats != null && species.moves.size > 0) pool.add(species);
        }
        Random random = new Random(20261009L);
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> samples = new LinkedHashMap<>();
        int battles = 0;
        int rounds = Integer.getInteger("pokemon.fuzz.rounds", 6);
        for (PbsData.TrainerData trainerData : data.trainers.values()) {
            if (trainerData.party.size == 0) continue;
            for (int round = 0; round < rounds; round++) {
                long seed = random.nextLong();
                try {
                    Random rng = new Random(seed);
                    Battle battle = new Battle(data, rng, null);
                    battle.trainerBattle = true;
                    boolean doubles = rng.nextInt(3) == 0 && trainerData.party.size >= 2;     // a third of the battles are doubles
                    if (doubles) battle.setSideSizes(2, 2);
                    TrainerState player = new TrainerState();
                    int teamSize = doubles ? 2 + rng.nextInt(2) : 1 + rng.nextInt(3);
                    for (int i = 0; i < teamSize; i++) {
                        PbsData.Species species = pool.get(rng.nextInt(pool.size()));
                        Pokemon mine = WildGenerator.pbNewPkmn(data, species, 20 + rng.nextInt(60), player, 1, rng);
                        damage(mine, rng);
                        battle.addPlayer(mine);
                    }
                    HeadlessBattlePort factory = new HeadlessBattlePort(player, () -> data, null, rng);
                    for (PbsData.TrainerPokemon member : trainerData.party) {
                        Pokemon foe = factory.trainerPokemon(trainerData, member, data);
                        if (foe == null) continue;
                        damage(foe, rng);
                        battle.addFoe(foe);
                    }
                    battles++;
                    battle.run(40);
                } catch (Throwable error) {
                    StringBuilder where = new StringBuilder();
                    int shown = 0;
                    for (StackTraceElement frame : error.getStackTrace()) {
                        if (frame.getClassName().startsWith("pokemon.runtime.battle.")) {
                            where.append(shown == 0 ? "" : " <- ").append(frame.getClassName().substring("pokemon.runtime.battle.".length()))
                                    .append('.').append(frame.getMethodName()).append(':').append(frame.getLineNumber());
                            if (++shown == 3) break;
                        }
                    }
                    String key = error.getClass().getSimpleName() + " at " + where;
                    counts.merge(key, 1, Integer::sum);
                    samples.putIfAbsent(key, trainerData.key + " seed=" + seed + " " + error.getMessage());
                }
            }
        }
        StringBuilder report = new StringBuilder("AI sweep: ").append(battles).append(" battles, ")
                .append(counts.size()).append(" distinct crash site(s)\n");
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            report.append("  ").append(entry.getValue()).append("x ").append(entry.getKey()).append("   e.g. ")
                    .append(samples.get(entry.getKey())).append('\n');
        }
        System.out.println(report);
        // The AI's own crash sites fail the test; engine ones (outside Ai*) are only listed, they are the engine's to decide.
        List<String> aiSites = new ArrayList<>();
        for (String key : counts.keySet()) {
            if (key.contains("Ai")) aiSites.add(key);
        }
        assertTrue(aiSites.isEmpty(), report.toString());
    }
}
