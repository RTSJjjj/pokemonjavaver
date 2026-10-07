package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code PBWeather} (PBWeather.rb:1-30). Battle weather is an
 * integer, not a name - the handlers compare it against these constants
 * ({@code w==PBWeather::Sun}) and the project's own
 * {@code PBWeather::HarshSun/HeavyRain/StrongWinds} are the "primordial"
 * weathers the plugin refuses to overwrite
 * (BattleHandlers:658-661).
 */
public final class PBWeather {

    private PBWeather() {
    }

    public static final int None = 0;
    public static final int Sun = 1;
    public static final int Rain = 2;
    public static final int Sandstorm = 3;
    public static final int Hail = 4;
    public static final int HarshSun = 5;
    public static final int HeavyRain = 6;
    public static final int StrongWinds = 7;
    public static final int ShadowSky = 8;
    public static final int Fog = 9;
    public static final int Snow = 10;

    /** {@code PBWeather.animationName} (PBWeather.rb:15-29); {@code null} for {@code None}. */
    public static String animationName(int weather) {
        switch (weather) {
            case Sun: return "Sun";
            case Rain: return "Rain";
            case Sandstorm: return "Sandstorm";
            case Hail: return "Hail";
            case HarshSun: return "HarshSun";
            case HeavyRain: return "HeavyRain";
            case StrongWinds: return "StrongWinds";
            case ShadowSky: return "ShadowSky";
            case Fog: return "Fog";
            case Snow: return "Snow";
            default: return null;
        }
    }
}
