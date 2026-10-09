package pokemon.runtime.ui.menu;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.*;
import pokemon.runtime.save.SaveManager;
import pokemon.runtime.state.*;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class P4MenuTest {
    @TempDir Path temp;
    private PbsData data;
    private GameState state;
    @BeforeEach void setup() {
        data = PbsData.parse(temp.toFile()); state = new GameState(); state.enterMap(1, 0, 0);
        species("A", 1); species("B", 2);
    }
    private PbsData.Species species(String id, int number) {
        PbsData.Species s = new PbsData.Species(); s.id = number; s.internalName = id; s.name = id;
        s.baseStats = new int[] {80, 80, 80, 80, 80, 80}; data.species.put(id, s); return s;
    }
    private Pokemon pokemon() { return new Pokemon(data.species("A"), 20, data); }
    private PbsData.Item item(String id, int pocket) {
        PbsData.Item item = new PbsData.Item(); item.internalName = id; item.name = id; item.pocket = pocket;
        data.items.put(id, item); state.inventory().add(id, 2); return item;
    }
    @Test void emptyListWrapScrollAndShrink() {
        MenuListModel cursor = new MenuListModel(3); cursor.move(-1); assertEquals(0, cursor.index());
        cursor.size(10); cursor.move(-1); assertEquals(9, cursor.index()); assertEquals(7, cursor.first());
        cursor.move(1); assertEquals(0, cursor.first()); cursor.select(6); assertEquals(4, cursor.first());
        cursor.size(2); assertEquals(1, cursor.index()); assertEquals(0, cursor.first());
    }
    @Test void pocketsHideZeroAndKeepKeyItemsSeparate() {
        item("POTION", 2); item("KEY", 8); item("STONE", 1);
        BagModel bag = new BagModel(state.inventory(), data); assertEquals("STONE", bag.selected());
        bag.changePocket(1); assertEquals("POTION", bag.selected());
        state.inventory().remove("POTION", 2); bag.refresh(); assertNull(bag.selected());
        bag.changePocket(6); assertEquals("KEY", bag.selected());
    }
    @Test void healingOnlyConsumesOnEffectAndCannotRevive() {
        Pokemon p = pokemon(); item("POTION", 2);
        assertEquals(ItemUse.Result.NO_EFFECT, ItemUse.use("POTION", p, state.trainer(), state.inventory(), data, -1, true));
        assertEquals(2, state.inventory().count("POTION"));
        p.hp -= 21; int before = p.hp;
        assertEquals(ItemUse.Result.USED, ItemUse.use("POTION", p, state.trainer(), state.inventory(), data, -1, true));
        assertEquals(before + 20, p.hp); assertEquals(1, state.inventory().count("POTION"));
        p.hp = 0; ItemUse.use("POTION", p, state.trainer(), state.inventory(), data, -1, true);
        assertEquals(0, p.hp); assertEquals(1, state.inventory().count("POTION"));
    }
    @Test void evolutionChecksGenderAndRegistersDex() {
        Pokemon p = pokemon(); item("STONE", 1);
        PbsData.Evolution evo = new PbsData.Evolution(); evo.method = "ItemFemale"; evo.parameter = "STONE"; evo.species = "B";
        p.species.evolutions.add(evo); p.gender = PokemonStats.MALE;
        assertNull(ItemUse.evolution(p, "STONE", data, true));
        p.gender = PokemonStats.FEMALE;
        assertEquals(ItemUse.Result.USED, ItemUse.use("STONE", p, state.trainer(), state.inventory(), data, -1, true));
        assertEquals("B", p.internalName); assertTrue(state.trainer().owned.contains("B")); assertEquals(1, state.inventory().count("STONE"));
    }
    @Test void machineCancelDoesNotForgetAndReplacementHasPp() {
        Pokemon p = pokemon(); PbsData.Item tm = item("TM01", 4); tm.fieldUse = 3; tm.extra.add("NEW");
        PbsData.Move move = new PbsData.Move(); move.internalName = "NEW"; move.pp = 15; data.moves.put("NEW", move);
        data.tmCompatibility.put("NEW", Array.with("A"));
        PbsData.Move old = new PbsData.Move(); old.internalName = "OLD";
        for (int i = 0; i < 4; i++) p.moves.add(new Pokemon.MoveSlot(old));
        assertEquals(ItemUse.Result.REPLACE_MOVE, ItemUse.use("TM01", p, state.trainer(), state.inventory(), data, -1, true));
        assertSame(old, p.moves.get(2).move); assertEquals(2, state.inventory().count("TM01"));
        assertEquals(ItemUse.Result.USED, ItemUse.use("TM01", p, state.trainer(), state.inventory(), data, 2, true));
        assertEquals(15, p.moves.get(2).pp); assertEquals(4, p.moves.size);
    }
    @Test void heldItemsAreExchangedWithoutLossAndSwapCanCancel() {
        Pokemon p = pokemon(), second = pokemon(); state.trainer().party.add(p); state.trainer().party.add(second);
        item("OLD", 1); item("NEW", 1); p.item = "OLD";
        assertTrue(PartyModel.giveItem(p, "NEW", state.inventory(), data));
        assertEquals(3, state.inventory().count("OLD")); assertEquals(1, state.inventory().count("NEW"));
        PartyModel party = new PartyModel(state.trainer().party, data);
        party.swap(); party.cursor.move(1); party.cancelSwap(); assertSame(p, state.trainer().first());
        party.swap(); party.cursor.move(-1); party.swap(); assertSame(second, state.trainer().first());
    }
    @Test void storageSlotsNamesAndWallpapersSurviveTheSave() {
        Pokemon a = pokemon(), b = pokemon();
        Storage pc = state.trainer().storage;
        pc.set(0, 5, a); pc.set(0, 7, b); pc.box(0).name = "对战"; pc.box(0).background = 3;
        pc.currentBox = 2;
        SaveManager saves = new SaveManager(); saves.attachPbs(data); GameState loaded = new GameState();
        assertTrue(saves.fromJson(saves.toJson(state), loaded));
        Storage restored = loaded.trainer().storage;
        assertNotNull(restored.get(0, 5)); assertNull(restored.get(0, 6)); assertNotNull(restored.get(0, 7));
        assertEquals("对战", restored.box(0).name); assertEquals(3, restored.box(0).background);
        assertEquals(2, restored.currentBox);
    }
    @Test void legacyCompactStorageIsMigratedIntoSlots() {
        Pokemon a = pokemon(), b = pokemon();
        Storage pc = state.trainer().storage;
        pc.set(1, 0, a); pc.set(1, 9, b);
        SaveManager saves = new SaveManager(); saves.attachPbs(data);
        com.badlogic.gdx.utils.JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(saves.toJson(state));
        com.badlogic.gdx.utils.JsonValue trainer = root.get("trainer");
        com.badlogic.gdx.utils.JsonValue boxes = trainer.get("storage").get("boxes");
        // the old format: an array of boxes, each an array of Pokemon, no slot numbers
        com.badlogic.gdx.utils.JsonValue legacy = new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.array);
        legacy.addChild(new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.array));
        com.badlogic.gdx.utils.JsonValue legacyBox = new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.array);
        for (com.badlogic.gdx.utils.JsonValue entry = boxes.child.get("slots").child; entry != null; ) {
            com.badlogic.gdx.utils.JsonValue next = entry.next;
            entry.next = null; entry.prev = null; entry.parent = null;
            legacyBox.addChild(entry);
            entry = next;
        }
        legacy.addChild(legacyBox);
        trainer.remove("storage");
        trainer.addChild("storage", legacy);
        GameState loaded = new GameState();
        assertTrue(saves.fromJson(root.toJson(com.badlogic.gdx.utils.JsonWriter.OutputType.json), loaded));
        Storage restored = loaded.trainer().storage;
        assertNull(restored.get(0, 0));
        assertNotNull(restored.get(1, 0)); assertNotNull(restored.get(1, 1), "filled from the first slot in saved order");
        assertNull(restored.get(1, 9));
    }
    @Test void optionalSaveFieldsRoundTripAndLegacyBackfillsDex() {
        state.trainer().addToParty(pokemon()); state.trainer().region = 4; state.trainer().currentStorage().store(pokemon());
        state.trainer().seen.add("B"); state.trainer().badges.add(2); state.trainer().playSeconds = 3661;
        state.variables().set(1, 5); state.variables().setText(2, "名字$\\");
        assertArrayEquals(new int[] {1, 2}, state.variables().ids());
        SaveManager saves = new SaveManager(); saves.attachPbs(data); GameState loaded = new GameState();
        assertTrue(saves.fromJson(saves.toJson(state), loaded));
        assertEquals(1, loaded.trainer().storageForRegion(4).count()); assertEquals(0, loaded.trainer().storage.count());
        assertEquals(state.trainer().owned, loaded.trainer().owned); assertTrue(loaded.trainer().seen.contains("B"));
        assertEquals(3661, loaded.trainer().playSeconds); assertEquals("名字$\\", loaded.variables().text(2));
        loaded.reset(); assertEquals(0, loaded.trainer().partyCount()); assertTrue(loaded.trainer().owned.isEmpty());
        assertTrue(saves.fromJson("{\"saveVersion\":1,\"map\":{\"id\":1,\"x\":0,\"y\":0}}", loaded));
        assertEquals(0, loaded.trainer().playSeconds);
    }
    @Test void tradePreparesTheIncomingPokemonLikePbStartTrade() {
        state.trainer().party.add(pokemon()); Pokemon offered = pokemon(); offered.shiny = true; offered.ivs[0] = 31;
        Pokemon yours = TradeModel.prepare(state.trainer(), state.trainer().first(), offered, "礼物", "交换者", data, new java.util.Random(1));
        assertSame(offered, yours); assertEquals("礼物", yours.name); assertEquals("交换者", yours.originalTrainer);
        assertEquals(2, yours.obtainMode); assertEquals(31, yours.ivs[0]); assertTrue(yours.shiny);
        assertTrue(state.trainer().owned.contains(yours.species.internalName));
        assertNull(TradeModel.prepare(state.trainer(), state.trainer().first(), "NOSUCHSPECIES", "", "", data, new java.util.Random(1)));
    }
}
