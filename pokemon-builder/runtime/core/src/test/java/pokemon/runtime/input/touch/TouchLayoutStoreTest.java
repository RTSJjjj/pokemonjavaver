package pokemon.runtime.input.touch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.app.StoragePort;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TouchLayoutStoreTest {
    private static final int W = 2400, H = 1080;

    private static TouchButton key(List<TouchButton> keys, String id) {
        return keys.stream().filter(b -> b.id.equals(id)).findFirst().orElseThrow();
    }

    @Test
    void theDefaultLayoutCarriesTheGearAndTheCheatKey() {
        List<TouchButton> keys = TouchLayout.create(W, H, 1f);
        TouchButton gear = key(keys, TouchLayout.GEAR);
        TouchButton cheat = key(keys, TouchLayout.CHEAT);
        TouchButton l = key(keys, TouchLayout.SHOULDER_L);
        TouchButton r = key(keys, TouchLayout.SHOULDER_R);
        assertTrue(gear.x >= l.x + l.w, "the gear sits right of L");
        assertTrue(cheat.x + cheat.w <= r.x, "the cheat key sits left of R");
        for (TouchButton a : keys) {
            for (TouchButton b : keys) {
                if (a == b || a.shape == TouchButton.Shape.CROSS || b.shape == TouchButton.Shape.CROSS) continue;
                boolean overlap = a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + b.h && b.y < a.y + a.h;
                assertFalse(overlap, a.id + " overlaps " + b.id);
            }
        }
    }

    @Test
    void aSavedArrangementMovesAndScalesAKeyButNeverTheGear() {
        TouchLayoutStore store = new TouchLayoutStore();
        store.keys.put(TouchLayout.CONFIRM, new float[] {0.5f, 0.5f, 2f});
        store.keys.put(TouchLayout.GEAR, new float[] {0.9f, 0.9f, 2f});
        List<TouchButton> plain = TouchLayout.create(W, H, 1f);
        List<TouchButton> moved = TouchLayout.create(W, H, 1f, store);
        TouchButton confirm = key(moved, TouchLayout.CONFIRM);
        assertEquals(W * 0.5f, confirm.centerX(), 0.5f);
        assertEquals(H * 0.5f, confirm.centerY(), 0.5f);
        assertEquals(key(plain, TouchLayout.CONFIRM).w * 2f, confirm.w, 0.5f);
        assertEquals(key(plain, TouchLayout.GEAR).x, key(moved, TouchLayout.GEAR).x, 0.01f);
    }

    @Test
    void aKeyDraggedOffTheScreenIsPulledBackIn() {
        TouchLayoutStore store = new TouchLayoutStore();
        store.keys.put(TouchLayout.RUN, new float[] {1f, 1f, 1f});
        TouchButton run = key(TouchLayout.create(W, H, 1f, store), TouchLayout.RUN);
        assertTrue(run.x + run.w <= W + 0.01f && run.y + run.h <= H + 0.01f);
    }

    @Test
    void theEditorBarIsSixKeysInsideTheScreen() {
        List<TouchButton> bar = TouchLayout.editorBar(W, H);
        assertEquals(6, bar.size());
        for (TouchButton item : bar) {
            assertTrue(item.x >= 0 && item.x + item.w <= W);
        }
        assertEquals(TouchLayout.EDIT_DONE, bar.get(5).id);
    }

    @Test
    void theArrangementRoundTripsThroughTheFile(@TempDir Path dir) {
        System.setProperty(StoragePort.USER_DIR_PROPERTY, dir.toString());
        try {
            StoragePort storage = new StoragePort();
            TouchLayoutStore store = new TouchLayoutStore();
            store.opacity = 0.6f;
            store.keys.put(TouchLayout.DPAD, new float[] {0.2f, 0.7f, 1.3f});
            store.save(storage);
            TouchLayoutStore back = TouchLayoutStore.load(storage);
            assertEquals(0.6f, back.opacity, 0.001f);
            assertArrayEquals(new float[] {0.2f, 0.7f, 1.3f}, back.keys.get(TouchLayout.DPAD), 0.001f);
        } finally {
            System.clearProperty(StoragePort.USER_DIR_PROPERTY);
        }
    }

    @Test
    void aMissingFileIsTheDefault(@TempDir Path dir) {
        System.setProperty(StoragePort.USER_DIR_PROPERTY, dir.toString());
        try {
            TouchLayoutStore store = TouchLayoutStore.load(new StoragePort());
            assertTrue(store.keys.isEmpty());
            assertEquals(1f, store.opacity, 0.001f);
        } finally {
            System.clearProperty(StoragePort.USER_DIR_PROPERTY);
        }
    }
}
