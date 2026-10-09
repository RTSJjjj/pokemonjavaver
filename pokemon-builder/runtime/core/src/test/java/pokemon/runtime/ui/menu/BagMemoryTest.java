package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.BagMemory;
import pokemon.runtime.state.GameState;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 195_PItem_Bag lastpocket / getChoice / setChoice and 305_BW_Bag:160-197, :429, :459. */
class BagMemoryTest {
    @TempDir Path temp;
    private PbsData data;
    private GameState state;
    private int nextId;

    @BeforeEach void setup() {
        data = PbsData.parse(temp.toFile());
        state = new GameState();
        for (String id : new String[] {"A", "B", "C"}) item(id, 2);
        item("BALL", 3);
    }

    private void item(String id, int pocket) {
        PbsData.Item item = new PbsData.Item();
        item.internalName = id;
        item.name = id;
        item.pocket = pocket;
        item.id = ++nextId;
        data.items.put(id, item);
        state.inventory().add(id, 1);
    }

    @Test @DisplayName("the bag reopens on the last pocket and the item the cursor was left on")
    void reopensWhereLeft() {
        BagModel bag = new BagModel(state.inventory(), data);
        bag.changePocket(1);                       // pocket 2
        bag.cursor.select(2);
        bag.remember();
        bag.changePocket(1);                       // pocket 3
        bag.remember();

        BagModel again = new BagModel(state.inventory(), data);
        assertEquals(3, again.pocket());
        again.changePocket(-1);                    // back to pocket 2: its own cursor comes back
        assertEquals(2, again.cursor.index());
        assertEquals("C", again.selected());
    }

    @Test @DisplayName("a battle bag keeps its own memory and leaves the field bag alone")
    void battleMemoryIsSeparate() {
        BagModel bag = new BagModel(state.inventory(), data);
        BagMemory battle = state.inventory().bagMemory().copy();
        bag.useMemory(battle);
        bag.changePocket(1);
        bag.cursor.select(1);
        bag.remember();
        assertEquals(1, state.inventory().bagMemory().lastPocket);
        assertEquals(0, state.inventory().bagMemory().choice(2));
        bag.useMemory(battle);                     // the next round reopens on the battle memory
        assertEquals(2, bag.pocket());
        assertEquals(1, bag.cursor.index());
    }
}
