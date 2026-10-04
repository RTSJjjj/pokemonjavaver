package pokemon.runtime.map;

import com.badlogic.gdx.utils.JsonValue;

/**
 * R6.22: the map's panorama and fog overlays (Change Map Settings 204, Change
 * Fog Opacity 206), modelled after this project's own {@code Game_Map}: the
 * panorama is a half-speed parallax background, the fog a world-anchored
 * overlay that drifts by {@code fog_sx}/{@code fog_sy} ({@code @fog_ox -=
 * @fog_sx/8.0} per 40 fps frame). Spriteset_Map positions them with
 * {@code @panorama.ox = display_x/2} and {@code @fog.ox = display_x + fog_ox}.
 *
 * <p>Pure state: the map screen loads the textures and draws the layers, this
 * class only answers "what does the command mean" and advances the drift.</p>
 */
public final class MapEnvironment {

    /**
     * The scroll phase of one tiled plane in world coordinates, so a texture
     * drawn at {@code origin + phase} shows bitmap pixel {@code screenX + ox}
     * like RGSS {@code Plane#ox}. With {@code ox = origin * parallax + drift}
     * (this project: 1/2 for the panorama, 1 plus {@code fog_ox} for the fog)
     * the phase is {@code -(origin * parallax + drift)}, normalized to
     * {@code (-size, 0]} so tiling from the camera origin always covers the
     * viewport. The pattern therefore stays pinned to the world while the
     * camera moves; only the drift term slides it.
     */
    public static float planePhase(float origin, float parallax, float drift, float size) {
        if (size <= 0f) {
            return 0f;
        }
        float phase = -(origin * parallax + drift) % size;
        return phase > 0f ? phase - size : phase;
    }

    /** One panorama or fog layer. */
    public static final class Layer {
        public String name = "";
        public int hue;
        /** 0..255; the panorama is always opaque. */
        public float opacity = 255f;
        /** 0 normal, 1 additive, 2 subtractive. */
        public int blendType;
        /** Percent, 100 = 1:1. */
        public int zoom = 100;
        public int sx;
        public int sy;
        /** Drift in pixels (RGSS {@code @fog_ox}/{@code @fog_oy}). */
        public float ox;
        public float oy;

        public boolean visible() {
            return name != null && !name.isEmpty() && opacity > 0.5f;
        }
    }

    private final Layer panorama = new Layer();
    private final Layer fog = new Layer();

    public Layer panorama() {
        return panorama;
    }

    public Layer fog() {
        return fog;
    }

    /**
     * Change Map Settings (204).
     *
     * <p>Type 0 = panorama {@code [type, name, hue]}; type 1 = fog
     * {@code [type, name, hue, opacity, blend, zoom, sx, sy]}. An empty name
     * clears the layer, like RMXP. Other types (battleback, start position) are
     * reported by the caller.</p>
     *
     * @return true when the command was a panorama / fog change
     */
    public boolean apply(int type, JsonValue parameters) {
        if (parameters == null || !parameters.isArray()) {
            return false;
        }
        if (type == 0) {
            panorama.name = stringParam(parameters, 1);
            panorama.hue = intParam(parameters, 2, 0);
            return true;
        }
        if (type == 1) {
            fog.name = stringParam(parameters, 1);
            fog.hue = intParam(parameters, 2, 0);
            fog.opacity = clamp(intParam(parameters, 3, 255), 0, 255);
            fog.blendType = intParam(parameters, 4, 0);
            fog.zoom = Math.max(1, intParam(parameters, 5, 100));
            fog.sx = intParam(parameters, 6, 0);
            fog.sy = intParam(parameters, 7, 0);
            return true;
        }
        return false;
    }

    /** Change Fog Opacity (206). */
    public void changeFogOpacity(int opacity) {
        fog.opacity = clamp(opacity, 0, 255);
    }

    /**
     * R6.23: a map-connection crossing starts the neighbour map fresh, like
     * {@code Game_Map#setup} (the project's tilesets carry no panorama / fog,
     * so every 204 state has to be re-issued by that map's own events).
     */
    public void reset() {
        panorama.name = "";
        panorama.hue = 0;
        panorama.ox = 0;
        panorama.oy = 0;
        fog.name = "";
        fog.hue = 0;
        fog.opacity = 255f;
        fog.blendType = 0;
        fog.zoom = 100;
        fog.sx = 0;
        fog.sy = 0;
        fog.ox = 0;
        fog.oy = 0;
    }

    /** Advances the fog drift; RGSS: {@code @fog_ox -= @fog_sx / 8.0} per frame. */
    public void update(float delta) {
        if (!fog.visible()) {
            return;
        }
        float frames = Math.max(0f, delta) * 40f;
        fog.ox -= fog.sx / 8f * frames;
        fog.oy -= fog.sy / 8f * frames;
    }

    private static int intParam(JsonValue parameters, int index, int fallback) {
        JsonValue value = parameters.get(index);
        return value == null || !value.isNumber() ? fallback : value.asInt();
    }

    private static String stringParam(JsonValue parameters, int index) {
        JsonValue value = parameters.get(index);
        return value == null || !value.isString() ? "" : value.asString();
    }

    private static float clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
