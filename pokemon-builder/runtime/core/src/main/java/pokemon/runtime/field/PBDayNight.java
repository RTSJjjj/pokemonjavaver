package pokemon.runtime.field;

import java.time.LocalTime;

/**
 * {@code PBDayNight} (372_PBDayNight): the project's time-of-day rules. Both the {@code isReal*} variants and the ones
 * the game logic uses read the clock's own hour: day 6..17, night 18..23 and 0..5 (the screen tone follows the same hour).
 */
public final class PBDayNight {
    private PBDayNight() {
    }

    /** {@code time.hour}: the project reads the real clock's hour for every time-of-day test. */
    static int gameHour(LocalTime time) {
        return time.getHour();
    }

    /** :87-92 isDay?: hours 6 to 17. */
    public static boolean isDay(LocalTime time) {
        int hour = gameHour(time);
        return hour >= 6 && hour < 18;
    }

    /** :94-99 isNight?: hours 18 to 23 and 0 to 5. */
    public static boolean isNight(LocalTime time) {
        int hour = gameHour(time);
        return hour >= 18 || hour < 6;
    }

    /** :101-106 isDawn?: hours 3 to 5. */
    public static boolean isDawn(LocalTime time) {
        int hour = gameHour(time);
        return hour >= 3 && hour < 6;
    }

    /** :108-113 isMorning?: hours 6 to 8. */
    public static boolean isMorning(LocalTime time) {
        int hour = gameHour(time);
        return hour >= 6 && hour < 9;
    }

    /** :115-120 isBeforeNoon?: hours 9 and 10. */
    public static boolean isBeforeNoon(LocalTime time) {
        int hour = gameHour(time);
        return hour >= 9 && hour < 11;
    }

    /** :122-127 isAtNoon?: hours 11 and 12. */
    public static boolean isAtNoon(LocalTime time) {
        int hour = gameHour(time);
        return hour >= 11 && hour < 13;
    }

    /** :129-134 isAfternoon?: hours 13 to 17. */
    public static boolean isAfternoon(LocalTime time) {
        int hour = gameHour(time);
        return hour >= 13 && hour < 18;
    }

    /** :136-141 isDusk?: hour 18. */
    public static boolean isDusk(LocalTime time) {
        return gameHour(time) == 18;
    }

    /** :143-148 isEvening?: hours 19 to 21. */
    public static boolean isEvening(LocalTime time) {
        int hour = gameHour(time);
        return hour >= 19 && hour < 22;
    }

    /** :169-179 pbGetDayNightName: the name of the part of the day. */
    public static String name(LocalTime time) {
        if (isDawn(time)) return "凌晨";
        if (isMorning(time)) return "早晨";
        if (isBeforeNoon(time)) return "上午";
        if (isAtNoon(time)) return "中午";
        if (isAfternoon(time)) return "下午";
        if (isDusk(time)) return "黄昏";
        if (isEvening(time)) return "夜晚";
        if (isMidnight(time)) return "午夜";
        return "";
    }

    /** :150-155 isMidnight?: hours 22 to 24 and 0 to 2. */
    public static boolean isMidnight(LocalTime time) {
        int hour = gameHour(time);
        return hour >= 22 || hour < 3;
    }
}
