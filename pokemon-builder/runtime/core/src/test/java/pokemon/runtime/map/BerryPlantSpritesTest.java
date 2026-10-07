package pokemon.runtime.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.MapData;
import pokemon.runtime.field.BerryPlants;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * P3 display: {@code BerryPlantSprite} applies the event variable's stage to
 * the event's sheet / facing and {@code BerryPlantMoistureSprite} shows the
 * berrytreeDry/Damp/Wet patch (PField_BerryPlants:45-309).
 */
class BerryPlantSpritesTest {

    private GameState state;
    private MapData map;
    private EventCharacters events;
    private PbsData pbs;
    private final List<String> sparkles = new ArrayList<>();
    private final Set<String> sheets = new HashSet<>();

    @BeforeEach
    void setUp() {
        File dataRoot = TestData.runtimeDataRoot();
        assumeTrue(dataRoot != null, "runtime data (generated/) not available");
        pbs = PbsData.parse(dataRoot);
        assumeTrue(pbs.item("ORANBERRY") != null, "generated items.json not available");

        map = new MapData();
        map.mapId = 30;
        map.width = 8;
        map.height = 8;
        addEvent(10, "BerryPlant", 2, 3);
        addEvent(11, "BerryPlant", 4, 1);
        addEvent(12, "NPC", 0, 0);

        state = new GameState();
        state.enterMap(map.mapId, 0, 0);
        events = new EventCharacters(map, new TileMap(map, null), state);
        sparkles.clear();
        sheets.clear();
        sheets.add("berrytreeORANBERRY");
        sheets.add("berrytreeplanted");
        sheets.add("berrytree395");
    }

    private void addEvent(int id, String name, int x, int y) {
        MapData.EventData event = new MapData.EventData();
        event.id = id;
        event.name = name;
        event.x = x;
        event.y = y;
        MapData.EventPageData page = new MapData.EventPageData();
        page.trigger = 0;
        page.graphic = new MapData.EventGraphic();
        page.graphic.characterName = "berrytreeCHERIBERRY"; // the map's preset sheets
        page.graphic.direction = 8;
        event.pages.add(page);
        map.events.add(event);
    }

    private BerryPlantSprites sprites() {
        return new BerryPlantSprites(map, events, state, pbs,
                name -> sheets.contains(name),
                (animationId, x, y, height) -> sparkles.add(animationId + ":" + x + "," + y + ":" + height),
                null, null);
    }

    private int oran() {
        return pbs.item("ORANBERRY").id;
    }

    private void plant(int eventId, int stage, int dampness) {
        state.eventVars().set(map.mapId, eventId,
                new int[] { stage, oran(), 0, (int) BerryPlants.now(), dampness, 0, 0, 0 });
    }

    @Test
    @DisplayName("an event without a variable keeps its page graphic (line 119-120)")
    void noVariableKeepsThePageGraphic() {
        BerryPlantSprites sprites = sprites();
        assertEquals(2, sprites.plantCount(), "only the berryplant events are tracked");

        assertFalse(sprites.update(), "no sheet change");
        assertEquals("berrytreeCHERIBERRY", events.character(10).characterName);
        assertFalse(events.character(10).sheetOverridden, "the page graphic still owns the sheet");
        assertEquals(8, events.character(10).direction());
        assertEquals(-1, sprites.moistureLevel(10));
        assertTrue(sparkles.isEmpty());
    }

    @Test
    @DisplayName("a planted plot shows its sheet, faces down and shows the moisture patch")
    void plantedPlotAppliesSheetAndMoisture() {
        plant(10, 2, 60);
        BerryPlantSprites sprites = sprites();

        assertTrue(sprites.update(), "the preset sheet is replaced by the berry's own");
        assertEquals("berrytreeORANBERRY", events.character(10).characterName);
        assertEquals(2, events.character(10).direction(), "sprouted plots face down");
        assertEquals(2, sprites.moistureLevel(10), "dampness 60 is the wet patch");

        plant(10, 2, 30);
        assertFalse(sprites.update());
        assertEquals(1, sprites.moistureLevel(10), "dampness 30 is damp");

        plant(10, 2, 0);
        sprites.update();
        assertEquals(0, sprites.moistureLevel(10), "an empty tank is the dry patch");
    }

    @Test
    @DisplayName("stage 1 uses berrytreeplanted, stage 5 faces up, stage 0 hides (284-299)")
    void stageGraphics() {
        plant(10, 1, 100);
        BerryPlantSprites sprites = sprites();
        assertTrue(sprites.update());
        assertEquals("berrytreeplanted", events.character(10).characterName);
        assertEquals(2, events.character(10).direction());

        plant(10, 5, 100);
        assertTrue(sprites.update(), "a ripe plot uses the berry's own sheet");
        assertEquals("berrytreeORANBERRY", events.character(10).characterName);
        assertEquals(8, events.character(10).direction(), "a ripe plot faces up");

        state.eventVars().set(map.mapId, 10, new int[8]);
        assertTrue(sprites.update());
        assertNull(events.character(10).characterName, "an empty plot draws nothing");
        assertTrue(events.character(10).sheetOverridden,
                "the plant owns the sheet, so the page graphic must not come back");
        assertEquals(-1, sprites.moistureLevel(10));
    }

    @Test
    @DisplayName("a stage change plays the sparkle animation, never on first sight (303-305)")
    void stageChangeSparkles() {
        plant(10, 2, 100);
        BerryPlantSprites sprites = sprites();
        sprites.update();
        assertTrue(sparkles.isEmpty(), "loading an existing plant must not sparkle");

        plant(10, 3, 100);
        sprites.update();
        assertEquals(List.of("7:2,3:1"), sparkles, "PLANT_SPARKLE_ANIMATION_ID at the tile");
        assertEquals(4, events.character(10).direction(), "a taller plant faces left");

        state.eventVars().set(map.mapId, 10, new int[8]);
        sprites.update();
        assertEquals(1, sparkles.size(), "resetting to the empty plot has no sparkle");
    }

    @Test
    @DisplayName("the sheet falls back to berrytreeNNN then Object ball (290-301)")
    void sheetFallback() {
        sheets.clear();
        sheets.add("berrytree395");
        plant(11, 5, 100);
        BerryPlantSprites sprites = sprites();
        assertTrue(sprites.update());
        assertEquals("berrytree395", events.character(11).characterName,
                "the numeric fallback wins when berrytreeORANBERRY is absent");

        sheets.clear();
        BerryPlantSprites bare = sprites();
        assertTrue(bare.update());
        assertEquals("Object ball", events.character(11).characterName,
                "with no sheet at all the plugin uses Object ball");
    }
}
