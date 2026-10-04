package pokemon.runtime.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.event.ScreenEffects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.6 regression: Transfer Player (201) with the fade option. RMXP blacks the
 * old map out, swaps it and fades the new one in. The first version consumed
 * the request once and never came back for it, so every door ended on a black
 * window (the real Map002 door asks for fade 1).
 */
class RuntimeTransferTest {

    private static RuntimeContext context() {
        return new RuntimeContext("test", null, ".");
    }

    @Test
    @DisplayName("a transfer without the fade option runs at once")
    void immediateTransfer() {
        RuntimeContext context = context();
        context.requestTransfer(3, 14, 15, 8, 0);
        assertTrue(context.transferPending());
        assertEquals(0, context.pendingTransferFade());

        RuntimeContext.Transfer ready = context.takeReadyTransfer();
        assertNotNull(ready);
        assertEquals(3, ready.mapId);
        assertEquals(14, ready.x);
        assertEquals(15, ready.y);
        assertEquals(8, ready.direction);
        assertFalse(context.transferPending(), "the request is consumed");
        assertNull(context.takeReadyTransfer());
    }

    @Test
    @DisplayName("a fading transfer waits for the black screen instead of hanging")
    void fadingTransferWaitsForBlack() {
        RuntimeContext context = context();
        ScreenEffects effects = context.screenEffects();
        context.requestTransfer(3, 14, 15, 8, 1);
        effects.fade(12, true); // what the map screen starts when the request lands

        assertNull(context.takeReadyTransfer(), "the fade has not started yet");
        assertTrue(context.transferPending());
        effects.update(6f / 20f); // half of the 12 frames
        assertEquals(127.5f, effects.fade(), 0.5f);
        assertNull(context.takeReadyTransfer(), "still grey, not black");

        effects.update(1f); // the fade lands on black
        assertEquals(255f, effects.fade());
        RuntimeContext.Transfer ready = context.takeReadyTransfer();
        assertNotNull(ready, "the swap must arrive once the screen is black");
        assertEquals(3, ready.mapId);
        assertEquals(8, ready.direction);
        assertFalse(context.transferPending());
    }

    @Test
    @DisplayName("a newer transfer replaces the queued one")
    void newestRequestWins() {
        RuntimeContext context = context();
        context.requestTransfer(2, 1, 2, 0, 1);
        context.requestTransfer(3, 14, 15, 8, 0);

        RuntimeContext.Transfer ready = context.takeReadyTransfer();
        assertNotNull(ready);
        assertEquals(3, ready.mapId, "only one transfer can be in flight");
        assertFalse(context.transferPending());
    }
}
