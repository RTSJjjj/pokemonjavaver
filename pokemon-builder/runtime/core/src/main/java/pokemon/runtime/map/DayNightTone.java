package pokemon.runtime.map;

/**
 * Ambient day/night colour tone of the player's real clock.
 *
 * <p>R6.17: the tones are the project's own - {@code PBDayNight::HourlyTones}
 * in {@code PField_Time} (script section 181) - one entry per hour, linearly
 * interpolated into the next hour by the current minute exactly like
 * {@code PBDayNight.getToneInternal} does. The project overrides
 * {@code pbGetTimeNow} with {@code Time.now}, so the original already follows
 * the player's system clock; this runtime reads {@link java.time.LocalTime}
 * the same way.</p>
 *
 * <p>RGSS tones have two parts: the red/green/blue channel shifts (-255..255,
 * negative darkens, positive brightens) and {@code gray} (0..255, pulls the
 * colours towards their luminance). Both are rendered by {@link ToneShader};
 * {@code -Dpokemon.daynight=false} disables the effect.</p>
 */
public final class DayNightTone {

    /**
     * PBDayNight::HourlyTones, hour by hour (00:00 .. 23:00):
     * red, green, blue, gray.
     */
    private static final float[][] HOURLY_TONES = {
        {-90f, -90f,  30f, 55f},  // 00:00 night
        {-90f, -90f,  30f, 55f},
        {-90f, -90f,  30f, 55f},
        {-90f, -90f,  30f, 55f},
        {-60f, -70f,  -5f, 50f},  // 04:00
        {-40f, -50f, -35f, 50f},  // 05:00 day/morning
        {-40f, -50f, -35f, 50f},
        {-40f, -50f, -35f, 50f},  // 07:00
        {-40f, -50f, -35f, 50f},
        {-20f, -25f, -15f, 20f},  // 09:00
        {  0f,   0f,   0f,  0f},  // 10:00 day
        {  0f,   0f,   0f,  0f},
        {  0f,   0f,   0f,  0f},  // 12:00 noon
        {  0f,   0f,   0f,  0f},
        {  0f,  -5f,  -5f,  0f},  // 14:00
        {  5f, -15f, -10f,  5f},  // 15:00
        { 10f, -25f, -15f, 10f},  // 16:00
        {  5f, -35f, -20f, 15f},  // 17:00
        { -5f, -30f, -20f,  0f},  // 18:00 (原傍晚)
        {-15f, -60f, -10f, 20f},  // 19:00 day/evening
        {-15f, -60f, -10f, 20f},
        {-70f, -90f,  15f, 55f},  // 21:00
        {-90f, -90f,  30f, 55f},  // 22:00 night
        {-90f, -90f,  30f, 55f},
    };

    /** Frames a change takes when the minute rolls over (1 s at 20 fps). */
    public static final int TRANSITION_FRAMES = 20;

    private DayNightTone() { }

    public static boolean enabled() {
        String property = System.getProperty("pokemon.daynight");
        return property == null || Boolean.parseBoolean(property);
    }

    /**
     * R6.16: whether a map gets the ambient tone. The project's
     * {@code pbDayNightTint} shades a map only when its PBS/metadata.txt says
     * {@code Outdoor = true} (indoor maps have the tone zeroed), so the runtime
     * follows the same flag. {@code null} means the generated data predates the
     * flag: keep the previous "shade everywhere" behaviour and let the map
     * screen ask for a data rebuild.
     */
    public static boolean shades(Boolean outdoor) {
        return outdoor == null || outdoor;
    }

    /** The tone for the player's current local time. */
    public static float[] now() {
        java.time.LocalTime time = java.time.LocalTime.now();
        return at(time.getHour(), time.getMinute());
    }

    /**
     * The tone for a wall-clock moment: {@code PBDayNight.getToneInternal}
     * interpolates the current hour's entry into the next hour's entry by the
     * minute ({@code 23:xx} wraps to {@code 00:00}).
     */
    public static float[] at(int hour, int minute) {
        int h = Math.floorMod(hour, 24);
        float t = Math.max(0, Math.min(59, minute)) / 60f;
        float[] from = HOURLY_TONES[h];
        float[] to = HOURLY_TONES[(h + 1) % 24];
        return new float[] {
            blend(from[0], to[0], t),
            blend(from[1], to[1], t),
            blend(from[2], to[2], t),
            blend(from[3], to[3], t),
        };
    }

    /** The raw table entry of an hour (tests / tools). */
    public static float[] hourlyTone(int hour) {
        return HOURLY_TONES[Math.floorMod(hour, 24)].clone();
    }

    private static float blend(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
