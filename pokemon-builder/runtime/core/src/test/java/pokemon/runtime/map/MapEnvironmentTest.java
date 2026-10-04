package pokemon.runtime.map;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.22: Change Map Settings (204) / Change Fog Opacity (206) state - the
 * panorama and fog layers of one map, straight from this project's Game_Map.
 */
class MapEnvironmentTest {

    private static MapEnvironment environment() {
        return new MapEnvironment();
    }

    @Test
    @DisplayName("a fog command carries name, opacity, blend, zoom and drift")
    void fogCommand() {
        MapEnvironment environment = environment();
        assertTrue(environment.apply(1,
                new JsonReader().parse("[1,\"clouds2\",0,30,0,100,2,2]")));

        MapEnvironment.Layer fog = environment.fog();
        assertEquals("clouds2", fog.name);
        assertEquals(30f, fog.opacity, 0.01f);
        assertEquals(0, fog.blendType);
        assertEquals(100, fog.zoom);
        assertEquals(2, fog.sx);
        assertEquals(2, fog.sy);
        assertTrue(fog.visible());
    }

    @Test
    @DisplayName("a panorama command carries just its name and hue")
    void panoramaCommand() {
        MapEnvironment environment = environment();
        assertTrue(environment.apply(0, new JsonReader().parse("[0,\"CloudySky2\",0]")));
        assertEquals("CloudySky2", environment.panorama().name);
        assertTrue(environment.panorama().visible());
    }

    @Test
    @DisplayName("an empty name clears the layer (RMXP erase)")
    void emptyNameClears() {
        MapEnvironment environment = environment();
        environment.apply(1, new JsonReader().parse("[1,\"clouds2\",0,30,0,100,2,2]"));
        environment.apply(1, new JsonReader().parse("[1,\"\",0,0,0,100,0,0]"));
        assertFalse(environment.fog().visible());
    }

    @Test
    @DisplayName("fog opacity can be changed on its own (206) and clamps")
    void fogOpacityCommand() {
        MapEnvironment environment = environment();
        environment.apply(1, new JsonReader().parse("[1,\"clouds2\",0,30,0,100,2,2]"));
        environment.changeFogOpacity(10);
        assertEquals(10f, environment.fog().opacity, 0.01f);
        environment.changeFogOpacity(999);
        assertEquals(255f, environment.fog().opacity, 0.01f);
    }

    @Test
    @DisplayName("the fog drifts by sx/sy at 40 fps (@fog_ox -= @fog_sx/8.0)")
    void fogDrift() {
        MapEnvironment environment = environment();
        environment.apply(1, new JsonReader().parse("[1,\"clouds2\",0,30,0,100,2,2]"));
        environment.update(1f / 40f);
        assertEquals(-0.25f, environment.fog().ox, 0.001f);
        assertEquals(-0.25f, environment.fog().oy, 0.001f);
        environment.update(1f);
        assertEquals(-10.25f, environment.fog().ox, 0.001f);
    }

    @Test
    @DisplayName("other 204 types are left to the caller")
    void otherTypes() {
        MapEnvironment environment = environment();
        assertFalse(environment.apply(2, new JsonReader().parse("[2,\"field\"]")));
        assertFalse(environment.apply(0, null));
    }

    @Test
    @DisplayName("planePhase pins the pattern to the world (never to the screen)")
    void planePhaseWorldAnchored() {
        float size = 256f;
        // One world pixel of camera travel moves the pattern one pixel the
        // other way - the clouds are anchored to the map, not the window.
        assertEquals(-64f, MapEnvironment.planePhase(64f, 1f, 0f, size), 0.001f);
        // Half-speed parallax panorama (Spriteset_Map: @panorama.ox = display_x/2).
        assertEquals(-32f, MapEnvironment.planePhase(64f, 0.5f, 0f, size), 0.001f);
        // Drift still slides the fog (@fog_ox -= @fog_sx/8.0).
        assertEquals(-54f, MapEnvironment.planePhase(64f, 1f, -10f, size), 0.001f);
        assertEquals(12.5f - size,
                MapEnvironment.planePhase(0f, 1f, -12.5f, size), 0.001f);
        for (float origin : new float[]{0f, 37f, 255f, 256f, 257f, 1023f, 4001f}) {
            float fog = MapEnvironment.planePhase(origin, 1f, -12.5f, size);
            assertTrue(fog <= 0f && fog > -size);
            assertEquals(0f, mod(origin - 12.5f + fog, size), 0.01f);
            float panorama = MapEnvironment.planePhase(origin, 0.5f, 0f, size);
            assertTrue(panorama <= 0f && panorama > -size);
            assertEquals(0f, mod(origin * 0.5f + panorama, size), 0.01f);
        }
    }

    private static float mod(float value, float size) {
        float result = value % size;
        return result < 0f ? result + size : result;
    }
}
