package pokemon.runtime.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/** {@code pbSafariState.decision} in the reception counter's autorun (map 28, event 2 page 2). */
class SafariConditionTest {
    private static boolean eval(GameState state, String text) {
        ConditionEnv env = new ConditionEnv(state, null, () -> 28, () -> 2, new java.util.HashMap<>(),
                java.time.LocalTime::now, null);
        return ScriptCondition.truthy(ScriptCondition.parse(text).eval(env));
    }

    @Test @DisplayName("pbSafariState.decision is read from the visit")
    void decision() {
        GameState state = new GameState();
        state.fieldGlobals().safari.begin(28, 14, 6, 8, 30);
        assertTrue(eval(state, "pbSafariState.decision==0"));
        assertFalse(eval(state, "pbSafariState.decision==1"));
        state.fieldGlobals().safari.decision = 1;
        assertFalse(eval(state, "pbSafariState.decision==0"));
        assertTrue(eval(state, "pbSafariState.decision==1"));
    }
}
