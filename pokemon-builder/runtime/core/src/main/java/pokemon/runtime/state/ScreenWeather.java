package pokemon.runtime.state;

/**
 * The weather half of {@code Game_Screen} (019_Game_Screen): the field weather
 * the Set Weather Effects command (236) and the plugin's
 * {@code $game_screen.weather(type, power, duration)} write. Rendering the
 * weather sprites is a separate layer (roadmap stage 10); this class owns only
 * the state machine, exactly as the plugin keeps it.
 *
 * <p>Types are {@code PBFieldWeather} (172_PField_Weather:3-11): 0 none, 1 rain,
 * 2 storm, 3 snow, 4 blizzard, 5 sandstorm, 6 heavy rain, 7 sun, 8 fog.</p>
 */
public final class ScreenWeather {

    public static final int NONE = 0;
    public static final int RAIN = 1;
    public static final int STORM = 2;
    public static final int SNOW = 3;
    public static final int BLIZZARD = 4;
    public static final int SANDSTORM = 5;
    public static final int HEAVY_RAIN = 6;
    public static final int SUN = 7;
    public static final int FOG = 8;

    /** Game_Screen runs at Graphics.frame_rate = 40. */
    private static final float FRAMES_PER_SECOND = 40f;

    private int type;                 // @weather_type
    private float max;                // @weather_max
    private int typeTarget;           // @weather_type_target
    private float maxTarget;          // @weather_max_target
    private int duration;             // @weather_duration
    private float clock;

    /** {@code attr_reader :weather_type}. */
    public int type() {
        return type;
    }

    /** {@code attr_reader :weather_max}: how many weather sprites are drawn. */
    public float max() {
        return max;
    }

    /**
     * {@code weather(type, power, duration)} (019_Game_Screen:78-93).
     * {@code duration} is in frames.
     */
    public void set(int newType, int power, int newDuration) {
        typeTarget = newType;                                      // :79
        if (typeTarget != 0) {                                     // :80
            type = typeTarget;                                     // :81
        }
        if (typeTarget == 0) {                                     // :83
            maxTarget = 0.0f;                                      // :84
        } else {
            maxTarget = (power + 1) * 4.0f;                        // :86
        }
        duration = newDuration;                                    // :88
        clock = 0f;
        if (duration == 0) {                                       // :89
            type = typeTarget;                                     // :90
            max = maxTarget;                                       // :91
        }
    }

    /** The weather part of {@code Game_Screen#update} (019_Game_Screen:140-146), run once per frame. */
    private void frame() {
        if (duration >= 1) {                                       // :140 (weather block)
            float d = duration;
            max = (max * (d - 1f) + maxTarget) / d;
            duration -= 1;
            if (duration == 0) {
                type = typeTarget;
            }
        }
    }

    /** Advances by {@code delta} seconds at 40 frames a second. */
    public void update(float delta) {
        if (duration < 1) {
            clock = 0f;
            return;
        }
        clock += Math.max(0f, delta) * FRAMES_PER_SECOND;
        while (clock >= 1f && duration >= 1) {
            clock -= 1f;
            frame();
        }
        if (duration < 1) {
            clock = 0f;                  // the ramp is over: leftover time must not shorten the next one
        }
    }

    /** A new game: {@code Game_Screen#initialize} (:35-38). */
    public void reset() {
        type = 0;
        max = 0.0f;
        typeTarget = 0;
        maxTarget = 0.0f;
        duration = 0;
        clock = 0f;
    }
}
