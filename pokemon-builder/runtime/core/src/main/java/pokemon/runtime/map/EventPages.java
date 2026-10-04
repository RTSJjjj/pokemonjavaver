package pokemon.runtime.map;

import pokemon.runtime.data.MapData;
import pokemon.runtime.state.GameState;

/**
 * Page resolver shared by rendering, collision and (R6) the event interpreter
 * (project3 sections 12, 29, 30): RMXP activates the LAST page whose
 * conditions hold, evaluated against the live switches, variables and self
 * switches of {@link GameState}.
 *
 * <p>The resolved index is recorded in the temporary event state so renderer,
 * collision and interpreter read one shared answer; the page data itself must
 * not change once the map is loaded (the Builder output is immutable).</p>
 */
public final class EventPages {

    private EventPages() { }

    /**
     * @param state live game state (switches / variables / self switches)
     * @param mapId the map that owns the event; self switches are per map
     */
    public static MapData.EventPageData resolve(GameState state, int mapId, MapData.EventData event) {
        for (int i = event.pages.size - 1; i >= 0; i--) {
            MapData.EventPageData page = event.pages.get(i);
            if (conditionsHold(state, mapId, event, page)) {
                record(state, mapId, event, i);
                return page;
            }
        }
        record(state, mapId, event, -1);
        return null;
    }

    /**
     * RMXP {@code Game_Event#conditions_met?}: every valid condition must
     * hold. A variable page uses ">= condition value", so a page conditioned
     * on {@code variable 45 >= 0} is active before any variable is set.
     */
    public static boolean conditionsHold(GameState state, int mapId,
                                         MapData.EventData event, MapData.EventPageData page) {
        MapData.EventConditions conditions = page.conditions;
        if (conditions == null) {
            return true;
        }
        if (conditions.switch1Valid && !switchOn(state, mapId, event, conditions.switch1Id)) {
            return false;
        }
        if (conditions.switch2Valid && !switchOn(state, mapId, event, conditions.switch2Id)) {
            return false;
        }
        if (conditions.variableValid
                && state.variables().get(conditions.variableId) < conditions.variableValue) {
            return false;
        }
        if (conditions.selfSwitchValid
                && !state.selfSwitches().get(mapId, event.id, conditions.selfSwitchCh)) {
            return false;
        }
        return true;
    }

    public static boolean through(MapData.EventPageData page) {
        return page.movement != null && page.movement.through;
    }

    /** {@code tsOn?("A")} / {@code tsOff?("A")} - the Essentials temp switch. */
    private static final java.util.regex.Pattern TEMP_SWITCH =
            java.util.regex.Pattern.compile("ts(On|Off)\\?\\(\\s*\"([A-D])\"\\s*\\)");
    /** {@code isOn?("A")} / {@code isOff?("A")} - the event's self switch. */
    private static final java.util.regex.Pattern SELF_SWITCH =
            java.util.regex.Pattern.compile("is(On|Off)\\?\\(\\s*\"([A-D])\"\\s*\\)");
    /** {@code pbIsWeekday(-1,2,4,6)} - true when today is one of those days. */
    private static final java.util.regex.Pattern WEEKDAY =
            java.util.regex.Pattern.compile("!?pbIsWeekday\\((.*)\\)");

    /**
     * RMXP / Essentials switch test ({@code Game_Event#switchIsOn?}). A switch
     * whose System.rxdata name starts with {@code s:} is not a stored boolean
     * but a script expression evaluated for the event that asks. This project
     * uses {@code s:tsOff?("A")} / {@code s:tsOn?("A")} on 671 event pages (the
     * arrival doors), plus a handful of PBDayNight / weekday / cooldown tests;
     * unknown expressions are reported once and count as false.
     */
    public static boolean switchOn(GameState state, int mapId, MapData.EventData event, int switchId) {
        String name = state.switchName(switchId);
        if (name == null || !name.startsWith("s:")) {
            return state.switches().get(switchId);
        }
        String expression = name.substring(2).trim();
        java.util.regex.Matcher temp = TEMP_SWITCH.matcher(expression);
        if (temp.matches()) {
            boolean on = state.tempSwitches().get(mapId, event.id, temp.group(2));
            return "On".equals(temp.group(1)) == on;
        }
        java.util.regex.Matcher self = SELF_SWITCH.matcher(expression);
        if (self.matches()) {
            boolean on = state.selfSwitches().get(mapId, event.id, self.group(2));
            return "On".equals(self.group(1)) == on;
        }
        java.util.regex.Matcher weekday = WEEKDAY.matcher(expression);
        if (weekday.matches()) {
            boolean listed = weekdayListContainsToday(weekday.group(1));
            return expression.startsWith("!") != listed;
        }
        if (expression.startsWith("PBDayNight.")) {
            return dayNight(expression.substring("PBDayNight.".length()));
        }
        if (expression.startsWith("cooledDown")) {
            // No save/time bookkeeping yet (R10 has no timestamps): treat the
            // cooldown as over so daily events stay reachable.
            return true;
        }
        if (expression.startsWith("pbInSafari") || expression.startsWith("pbBugContest")
                || expression.startsWith("pbInChallenge") || expression.startsWith("pbNextMysteryGiftID")) {
            // Stage 3 domains (safari / bug contest / mystery gift) are not
            // implemented yet, so those switches stay off.
            return false;
        }
        state.reportSwitchWarning("switch expression \"" + name + "\" is not supported yet; treated as false");
        return false;
    }

    /** {@code pbIsWeekday(0,2,4)}: Ruby weekday numbers, 0 = Sunday. */
    private static boolean weekdayListContainsToday(String arguments) {
        int today = java.time.LocalDate.now().getDayOfWeek().getValue() % 7; // Sunday = 0
        for (String part : arguments.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                if (Integer.parseInt(trimmed) == today) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // "-1" and other placeholders simply do not match today.
            }
        }
        return false;
    }

    /**
     * PBDayNight time-of-day tests, using the real clock and the plugin's own
     * hour ranges (plugin batch 2). The values come straight from
     * {@code PBDayNight.isRealDay?/Night?/...} in the project's plugin:
     * day 6..18, morning 6..9, before noon 9..11, noon 11..13, afternoon
     * 13..18, dusk 18, evening 19..22, midnight 22..3, dawn 3..6.
     */
    static boolean dayNight(String test) {
        return dayNight(test, java.time.LocalTime.now().getHour());
    }

    static boolean dayNight(String test, int hour) {
        switch (test) {
            case "isDay?":
                return hour >= 6 && hour < 18;
            case "isNight?":
                return hour >= 18 || hour < 6;
            case "isMorning?":
                return hour >= 6 && hour < 9;
            case "isBeforeNoon?":
                return hour >= 9 && hour < 11;
            case "isAtNoon?":
                return hour >= 11 && hour < 13;
            case "isAfternoon?":
                return hour >= 13 && hour < 18;
            case "isDusk?":
                return hour == 18;
            case "isEvening?":
                return hour >= 19 && hour < 22;
            case "isMidnight?":
                return hour >= 22 || hour < 3;
            case "isDawn?":
                return hour >= 3 && hour < 6;
            default:
                return false;
        }
    }

    private static void record(GameState state, int mapId, MapData.EventData event, int pageIndex) {
        state.temporary().of(mapId, event.id).pageIndex(pageIndex);
    }
}
