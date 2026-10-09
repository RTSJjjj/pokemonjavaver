package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;

import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Battle#aiGuard: a trainer-AI entry point that throws is logged once and skipped for the rest of the battle. */
class AiGuardTest {

    @Test
    @DisplayName("the first failure is caught and logged, later calls do not run the entry point again")
    void failureSwitchesTheEntryOff(@TempDir Path dir) {
        Battle battle = new Battle(PbsData.parse(dir.toFile()), new Random(1), null);
        AtomicInteger calls = new AtomicInteger();
        int first = battle.aiGuard("AiTest", () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("boom");
        }, 7);
        assertEquals(7, first, "the fallback answers");
        assertEquals(1, battle.aiFailures().size());
        assertTrue(battle.aiFailures().get(0).contains("AiTest failed") && battle.aiFailures().get(0).contains("boom"));
        assertTrue(battle.disabledAi().contains("AiTest"));

        int second = battle.aiGuard("AiTest", () -> {
            calls.incrementAndGet();
            return 99;
        }, 7);
        assertEquals(7, second, "a switched-off entry point is not tried again");
        assertEquals(1, calls.get());
        assertEquals(1, battle.aiFailures().size(), "no second log line");
    }

    @Test
    @DisplayName("a healthy entry point is untouched, and entry points are switched off one by one")
    void healthyEntryRuns(@TempDir Path dir) {
        Battle battle = new Battle(PbsData.parse(dir.toFile()), new Random(1), null);
        assertEquals(5, battle.aiGuard("AiA", () -> 5, 0));
        battle.aiGuard("AiB", () -> {
            throw new NullPointerException("x");
        }, 0);
        assertEquals(6, battle.aiGuard("AiA", () -> 6, 0), "AiA keeps working after AiB failed");
        assertTrue(battle.disabledAi().contains("AiB") && !battle.disabledAi().contains("AiA"));
    }
}
