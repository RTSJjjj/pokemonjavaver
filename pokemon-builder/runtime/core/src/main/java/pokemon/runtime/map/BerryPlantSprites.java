package pokemon.runtime.map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import pokemon.runtime.data.MapData;
import pokemon.runtime.field.BerryPlants;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The two berry plant user sprites of PField_BerryPlants for one map screen:
 * {@code BerryPlantSprite} (lines 111-309) applies the event variable's stage
 * to the event's own character sheet and facing, and
 * {@code BerryPlantMoistureSprite} (lines 45-107) draws the
 * berrytreeDry/Damp/Wet patch behind it.
 *
 * <p>The plugin creates both sprites for every event named "berryplant"
 * (lines 31-41). An event without an event variable keeps its page graphic
 * (line 119 returns early); a planted one replaces it every frame while the
 * map is open.</p>
 *
 * <p>The plugin also writes the settled data back every frame; this port only
 * reads it for the display, because {@code eventVars.set} bumps the shared
 * state version and would refresh every event page every second. Every
 * interaction ({@link pokemon.runtime.event.BerryInteraction}) and every save
 * load re-settles the growth, so the visible stage and the stored stage never
 * disagree.</p>
 */
public final class BerryPlantSprites {

    /** {@code PLANT_SPARKLE_ANIMATION_ID} (Settings:355). */
    public static final int PLANT_SPARKLE_ANIMATION_ID = 7;

    /** {@code addUserAnimation} host (PField_BerryPlants:304). */
    public interface SparkleHost {
        void play(int animationId, int x, int y, int height);
    }

    private static final int TILE = TilesetGeometry.TILE_SIZE;
    private static final int NONE = -1;
    private static final String[] MOISTURE_NAMES = {
            "berrytreeDry", "berrytreeDamp", "berrytreeWet",
    };

    /** One "berryplant" event and the display state already applied to it. */
    private static final class Plant {
        final int eventIndex;
        final MapCharacter character;
        /** Last stage applied; Integer.MIN_VALUE = not observed yet. */
        int lastStage = Integer.MIN_VALUE;
        /** Current moisture patch: NONE, 0 dry, 1 damp, 2 wet (lines 81-84). */
        int moisture = NONE;

        Plant(int eventIndex, MapCharacter character) {
            this.eventIndex = eventIndex;
            this.character = character;
        }
    }

    private final MapData map;
    private final GameState state;
    private final PbsData pbs;
    private final java.util.function.Predicate<String> characterExists;
    private final SparkleHost sparkles;
    private final TextureRepository textures;
    private final GraphicsLocator locator;

    private final Array<Plant> plants = new Array<>();
    private final Map<Integer, String> sheetCache = new HashMap<>();
    private final Map<String, Boolean> existsCache = new HashMap<>();
    private final Set<String> missingMoisture = new HashSet<>();
    private final Texture[] moistureTextures = new Texture[3];

    /** Per-frame depth-sorted list of visible moisture patches. */
    private int[] moisturePlant = new int[0];
    private int[] moistureDepth = new int[0];
    private int moistureCount;
    private int moistureCursor;

    public BerryPlantSprites(MapData map, EventCharacters events, GameState state, PbsData pbs,
                             SparkleHost sparkles, TextureRepository textures, GraphicsLocator locator) {
        this(map, events, state, pbs,
                name -> locator != null && locator.find("Characters", name + ".png") != null,
                sparkles, textures, locator);
    }

    /** Test constructor: the asset lookup is injected instead of using the locator. */
    public BerryPlantSprites(MapData map, EventCharacters events, GameState state, PbsData pbs,
                             java.util.function.Predicate<String> characterExists,
                             SparkleHost sparkles, TextureRepository textures, GraphicsLocator locator) {
        this.map = map;
        this.state = state;
        this.pbs = pbs;
        this.characterExists = characterExists;
        this.sparkles = sparkles;
        this.textures = textures;
        this.locator = locator;
        for (int i = 0; i < map.events.size; i++) {
            MapData.EventData event = map.events.get(i);
            if (event.name != null && event.name.equalsIgnoreCase("berryplant")) {
                plants.add(new Plant(i, events.character(event.id)));
            }
        }
    }

    /** Number of "berryplant" events on this map (tests). */
    public int plantCount() {
        return plants.size;
    }

    /** The moisture patch level of an event right now, or -1 when hidden. */
    public int moistureLevel(int eventId) {
        for (Plant plant : plants) {
            if (map.events.get(plant.eventIndex).id == eventId) {
                return plant.moisture;
            }
        }
        return NONE;
    }

    /**
     * One frame of BerryPlantSprite#update (lines 138-145).
     *
     * @return true when a character sheet changed: the caller must rebind the
     *         renderer so the new sheet loads (MapRenderer caches per event)
     */
    public boolean update() {
        boolean graphicsChanged = false;
        long now = BerryPlants.now();
        moistureCount = 0;
        for (int i = 0; i < plants.size; i++) {
            Plant plant = plants.get(i);
            MapData.EventData event = map.events.get(plant.eventIndex);
            int[] stored = state.eventVars().get(map.mapId, event.id);
            MapCharacter character = plant.character;
            if (stored == null || stored.length <= 6) {
                // NEW_BERRY_PLANTS: without a Gen 4 variable the plugin leaves
                // the page graphic untouched (initialize returns at line 120).
                if (character != null) {
                    character.sheetOverridden = false;
                }
                continue;
            }
            // updatePlantDetails (lines 147-217) on a copy: the display must
            // not bump the shared state version every second.
            int[] data = BerryPlants.update(stored.clone(), now,
                    plantData(stored[1]), mulchName(stored[7]));
            int stage = data[0];
            int berryId = data[1];
            if (stage != plant.lastStage) {
                // setGraphic's sparkle (line 303-305): only stages 2+ reach the
                // else branch, and never on the first observation.
                if (plant.lastStage != Integer.MIN_VALUE && stage >= 2 && sparkles != null) {
                    sparkles.play(PLANT_SPARKLE_ANIMATION_ID, event.x, event.y, 1);
                }
                plant.lastStage = stage;
            }
            String sheet = sheetFor(stage, berryId);
            int direction = directionFor(stage);
            if (character != null) {
                if (!Objects.equals(character.characterName, sheet)) {
                    character.characterName = sheet;
                    graphicsChanged = true;
                }
                // The plant owns the sheet even when it is empty: an empty
                // plot must hide the page graphic, not fall back to it.
                character.sheetOverridden = true;
                if (direction != 0) {
                    character.face(direction);
                }
            }
            // Moisture (lines 78-88): only a planted plot shows a patch.
            if (berryId > 0) {
                plant.moisture = data[4] > 50 ? 2 : data[4] > 0 ? 1 : 0;
                addMoisture(i, MapRenderer.eventDepth(event.y));
            } else {
                plant.moisture = NONE;
            }
        }
        sortMoisture();
        moistureCursor = 0;
        return graphicsChanged;
    }

    /** Restarts the depth walk; call once before each {@code renderer.render} pass. */
    public void beginRender() {
        moistureCursor = 0;
    }

    /**
     * Draws the moisture patches of the events whose depth bucket is about to
     * be drawn, so each patch stays behind its plant (the plugin draws the
     * moisture sprite before the plant's own character sprite).
     */
    public void renderMoisture(SpriteBatch batch, int depth) {
        while (moistureCursor < moistureCount) {
            if (moistureDepth[moistureCursor] > depth) {
                break;
            }
            Plant plant = plants.get(moisturePlant[moistureCursor]);
            if (plant.moisture != NONE) {
                drawMoisture(batch, plant);
            }
            moistureCursor++;
        }
    }

    // ------------------------------------------------------------------
    // setGraphic (lines 281-308)
    // ------------------------------------------------------------------

    /** Filename of one stage; null hides the event (line 284-285). */
    private String sheetFor(int stage, int berryId) {
        if (stage <= 0) {
            return null; // "": the plot is empty / not planted
        }
        if (stage == 1) {
            return "berrytreeplanted"; // common to all berries (line 287)
        }
        String cached = sheetCache.get(berryId);
        if (cached != null) {
            return cached;
        }
        String sheet = null;
        PbsData.Item item = pbs == null ? null : pbs.itemById(berryId);
        if (item != null) {
            String named = "berrytree" + item.internalName;
            if (exists(named)) {
                sheet = named;
            }
        }
        if (sheet == null) {
            String numeric = String.format(Locale.ROOT, "berrytree%03d", berryId);
            sheet = exists(numeric) ? numeric : "Object ball"; // lines 290-301
        }
        sheetCache.put(berryId, sheet);
        return sheet;
    }

    /** {@code turn_down/left/right/up} by stage (lines 294-299). */
    private static int directionFor(int stage) {
        switch (stage) {
            case 1:
            case 2:
                return 2; // X planted / X sprouted
            case 3:
                return 4;
            case 4:
                return 6;
            case 5:
                return 8;
            default:
                return 0;
        }
    }

    private boolean exists(String name) {
        Boolean cached = existsCache.get(name);
        if (cached != null) {
            return cached;
        }
        boolean value = characterExists != null && characterExists.test(name);
        existsCache.put(name, value);
        return value;
    }

    private int[] plantData(int itemId) {
        return pbs == null ? BerryPlants.DEFAULT_PLANT_DATA : pbs.berryPlantData(itemId);
    }

    private String mulchName(int itemId) {
        PbsData.Item item = pbs == null ? null : pbs.itemById(itemId);
        return item == null ? null : item.internalName;
    }

    // ------------------------------------------------------------------
    // BerryPlantMoistureSprite (lines 45-107)
    // ------------------------------------------------------------------

    private void addMoisture(int plantIndex, int depth) {
        if (moisturePlant.length < plants.size) {
            moisturePlant = new int[plants.size];
            moistureDepth = new int[plants.size];
        }
        moisturePlant[moistureCount] = plantIndex;
        moistureDepth[moistureCount] = depth;
        moistureCount++;
    }

    /** Insertion sort by depth; the list only holds visible patches. */
    private void sortMoisture() {
        for (int i = 1; i < moistureCount; i++) {
            int plant = moisturePlant[i];
            int depth = moistureDepth[i];
            int j = i - 1;
            while (j >= 0 && moistureDepth[j] > depth) {
                moisturePlant[j + 1] = moisturePlant[j];
                moistureDepth[j + 1] = moistureDepth[j];
                j--;
            }
            moisturePlant[j + 1] = plant;
            moistureDepth[j + 1] = depth;
        }
    }

    /**
     * The patch sits at the event's feet: RMXP screen_x/screen_y with
     * {@code ox=16, oy=24} (lines 50-51, 95-96) map to the tile's left edge and
     * 8 px above the tile bottom in this y-up world.
     */
    private void drawMoisture(SpriteBatch batch, Plant plant) {
        Texture texture = moistureTexture(plant.moisture);
        if (texture == null) {
            return;
        }
        MapData.EventData event = map.events.get(plant.eventIndex);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(texture, event.x * TILE, (map.height - event.y - 1) * TILE - 8,
                TILE, TILE);
    }

    private Texture moistureTexture(int level) {
        if (level < 0 || level >= moistureTextures.length) {
            return null;
        }
        if (moistureTextures[level] != null) {
            return moistureTextures[level];
        }
        String name = MOISTURE_NAMES[level];
        File file = locator == null ? null : locator.find("Characters", name + ".png");
        if (file == null || textures == null) {
            if (missingMoisture.add(name) && com.badlogic.gdx.Gdx.app != null) {
                // The map stays playable: the plant simply has no patch.
                com.badlogic.gdx.Gdx.app.error("BerryPlantSprites",
                        "moisture graphic missing: " + name);
            }
            return null;
        }
        try {
            moistureTextures[level] = textures.load("character:" + name, file);
        } catch (RuntimeException error) {
            if (missingMoisture.add(name) && com.badlogic.gdx.Gdx.app != null) {
                com.badlogic.gdx.Gdx.app.error("BerryPlantSprites",
                        "moisture graphic unreadable: " + name + " (" + error.getMessage() + ")");
            }
        }
        return moistureTextures[level];
    }
}
