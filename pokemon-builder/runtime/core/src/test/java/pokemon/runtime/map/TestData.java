package pokemon.runtime.map;

import pokemon.runtime.data.EventCommand;
import pokemon.runtime.data.MapData;

import java.io.File;

/**
 * R6.20: shared anchors for the tests that run against the real generated
 * project. The author edits the RMXP data while the port is in progress (R6.19
 * moved a script block from {@code cmd65} to {@code cmd71} by inserting
 * commands), so the tests must discover their anchors by <em>shape</em>
 * instead of hardcoding event ids, positions or command indices.
 */
public final class TestData {

    private TestData() { }

    /** generated/ sits next to the runtime checkout; also honour the property. */
    public static File runtimeDataRoot() {
        String configured = System.getProperty("pokemon.runtime.data");
        if (configured != null) {
            File file = new File(configured);
            if (file.isDirectory()) {
                return file;
            }
        }
        String[] candidates = { "../../generated", "../generated", "generated" };
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (new File(file, "project.json").isFile()) {
                return file;
            }
        }
        return null;
    }

    /** First event whose page with {@code trigger} holds a command of {@code code}. */
    public static MapData.EventData eventWithCommand(MapData data, int trigger, int code) {
        for (MapData.EventData event : data.events) {
            for (MapData.EventPageData page : event.pages) {
                if (page.trigger != trigger) {
                    continue;
                }
                for (int i = 0; i < page.commands.size; i++) {
                    EventCommand command = page.commands.get(i);
                    if (command != null && command.code == code) {
                        return event;
                    }
                }
            }
        }
        return null;
    }

    /**
     * First event whose page with {@code trigger} issues a Transfer Player (201)
     * with ({@code fading}) or without a fade - a real door between maps.
     */
    public static MapData.EventData eventWithTransfer(MapData data, int trigger, boolean fading) {
        for (MapData.EventData event : data.events) {
            for (MapData.EventPageData page : event.pages) {
                if (page.trigger != trigger) {
                    continue;
                }
                for (int i = 0; i < page.commands.size; i++) {
                    EventCommand command = page.commands.get(i);
                    if (command == null || command.code != 201) {
                        continue;
                    }
                    int fade = command.parameters != null && command.parameters.size > 5
                            ? command.parameters.getInt(5) : 0;
                    if (fading == (fade != 0)) {
                        return event;
                    }
                }
            }
        }
        return null;
    }

    /** First event that carries an arrival door page (trigger 3 + onEvent?). */
    public static MapData.EventData arrivalDoor(MapData data) {
        for (MapData.EventData event : data.events) {
            if (arrivalPage(event) != null) {
                return event;
            }
        }
        return null;
    }

    /**
     * First event whose page with {@code trigger} hides the hero (208) and then
     * transfers through a fade (201) - a scripted door with its walk-in / hide
     * animation, which the movement regressions assert on.
     */
    public static MapData.EventData eventWithDoorAnimation(MapData data, int trigger,
                                                           boolean fading) {
        for (MapData.EventData event : data.events) {
            for (MapData.EventPageData page : event.pages) {
                if (page.trigger != trigger) {
                    continue;
                }
                boolean hides = false;
                boolean fades = false;
                for (int i = 0; i < page.commands.size; i++) {
                    EventCommand command = page.commands.get(i);
                    if (command == null) {
                        continue;
                    }
                    if (command.code == 208 && command.parameters != null
                            && command.parameters.size > 0 && command.parameters.getInt(0) == 0) {
                        hides = true;
                    }
                    if (command.code == 201) {
                        int fade = command.parameters != null && command.parameters.size > 5
                                ? command.parameters.getInt(5) : 0;
                        if (fading == (fade != 0)) {
                            fades = true;
                        }
                    }
                }
                if (hides && fades) {
                    return event;
                }
            }
        }
        return null;
    }

    /** The arrival door page of an event, or null. */
    public static MapData.EventPageData arrivalPage(MapData.EventData event) {
        for (MapData.EventPageData page : event.pages) {
            if (EventTriggers.isArrivalDoorPage(page)) {
                return page;
            }
        }
        return null;
    }

    /** The event standing on a tile, or null. */
    public static MapData.EventData eventAt(MapData data, int x, int y) {
        for (MapData.EventData event : data.events) {
            if (event.x == x && event.y == y) {
                return event;
            }
        }
        return null;
    }

    /** [mapId, x, y] of the first Transfer Player (201) command of a page. */
    public static int[] transferTarget(MapData.EventData event) {
        for (MapData.EventPageData page : event.pages) {
            for (int i = 0; i < page.commands.size; i++) {
                EventCommand command = page.commands.get(i);
                if (command != null && command.code == 201 && command.parameters != null) {
                    return new int[] {command.parameters.getInt(1), command.parameters.getInt(2),
                            command.parameters.getInt(3)};
                }
            }
        }
        return null;
    }
}
