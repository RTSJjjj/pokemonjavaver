package pokemon.runtime.map;

import pokemon.runtime.data.MapData;
import pokemon.runtime.data.EventCommand;
import pokemon.runtime.state.GameState;

/**
 * RMXP event triggers (project3 section 79/18): which event page starts for a
 * tile. Trigger 0 = action button (the player faces the event), 1 = player
 * touch (the player walks into / onto it), 2 = event touch, 3 = autorun,
 * 4 = parallel process (the later two arrive with R6.4).
 */
public final class EventTriggers {

    public static final int ACTION = 0;
    public static final int PLAYER_TOUCH = 1;
    public static final int EVENT_TOUCH = 2;
    public static final int AUTORUN = 3;
    public static final int PARALLEL = 4;

    private EventTriggers() { }

    /**
     * Page that should run when the player stands on, or walks into,
     * {@code (x, y)}; null when no event there answers this trigger.
     */
    public static MapData.EventPageData pageAt(GameState state, MapData data, int x, int y, int trigger) {
        for (MapData.EventData event : data.events) {
            if (event.x != x || event.y != y) {
                continue;
            }
            MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
            if (page == null || page.trigger != trigger || page.commands.size == 0) {
                continue;
            }
            return page;
        }
        return null;
    }

    /** The matching event id, or -1 (used for self switches while running). */
    public static int eventIdAt(GameState state, MapData data, int x, int y, int trigger) {
        for (MapData.EventData event : data.events) {
            if (event.x != x || event.y != y) {
                continue;
            }
            MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
            if (page != null && page.trigger == trigger && page.commands.size > 0) {
                return event.id;
            }
        }
        return -1;
    }

    /**
     * First autorun page in event id order (RMXP runs exactly one autorun at a
     * time, and restarts it while its conditions still hold).
     */
    public static MapData.EventPageData autorunPage(GameState state, MapData data) {
        return firstPage(state, data, AUTORUN);
    }

    /**
     * Page of the event standing on the player's tile that answers the event
     * touch trigger, or null. {@code x}/{@code y} is the player's tile.
     */
    public static MapData.EventPageData eventTouchPage(GameState state, MapData data, int x, int y) {
        for (MapData.EventData event : data.events) {
            if (event.x != x || event.y != y) {
                continue;
            }
            MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
            if (page != null && page.trigger == EVENT_TOUCH && page.commands.size > 0) {
                return page;
            }
        }
        return null;
    }

    /** Event id of {@link #autorunPage}, or -1. */
    public static int autorunEventId(GameState state, MapData data) {
        for (MapData.EventData event : data.events) {
            MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
            if (page != null && page.trigger == AUTORUN && page.commands.size > 0) {
                return event.id;
            }
        }
        return -1;
    }

    /** Event id of {@link #eventTouchPage}, or -1. */
    public static int eventTouchEventId(GameState state, MapData data, int x, int y) {
        return eventIdAt(state, data, x, y, EVENT_TOUCH);
    }

    /** Parallel page (trigger 4) of one event, or null. */
    public static MapData.EventPageData parallelPage(GameState state, MapData data, MapData.EventData event) {
        MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
        return page != null && page.trigger == PARALLEL && page.commands.size > 0 ? page : null;
    }

    /**
     * R6.15: the Essentials door <em>arrival</em> pages are autorun pages whose
     * first command asks whether the player stands on the event
     * ({@code get_character(0).onEvent?}). That shape tells an arrival
     * animation apart from a normal autorun cutscene, so only those pages get
     * the "keep the hero hidden until the walk-out step" treatment.
     */
    public static boolean isArrivalDoorPage(MapData.EventPageData page) {
        if (page == null || page.trigger != AUTORUN || page.commands.size == 0) {
            return false;
        }
        EventCommand first = page.commands.first();
        if (first.code != 111 || first.parameters == null) {
            return false;
        }
        com.badlogic.gdx.utils.JsonValue script = first.parameters.get(1);
        return script != null && script.isString() && script.asString().contains("onEvent?");
    }

    private static MapData.EventPageData firstPage(GameState state, MapData data, int trigger) {
        for (MapData.EventData event : data.events) {
            MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
            if (page != null && page.trigger == trigger && page.commands.size > 0) {
                return page;
            }
        }
        return null;
    }
}
