package pokemon.runtime.map;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.data.MapData;
import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.event.MoveRoutePlayer;
import pokemon.runtime.state.GameState;

/**
 * Runtime characters for map events (R6.3b-2): position, facing, step
 * animation and route playback live here instead of in the immutable
 * {@link MapData}. This is what Set Event Location (202), Change Transparent
 * Flag (208) and Set Move Route (209) need.
 *
 * <p>Headless: the renderer and collision can read it, but nothing here needs
 * OpenGL, so the movement rules stay unit-testable.</p>
 */
public final class EventCharacters {

    private final MapData data;
    private final GameState state;
    private final Array<MapCharacter> characters = new Array<>();
    private final MoveRoutePlayer[] players;
    private final boolean[] transparent;
    private final MapData.EventPageData[] activePages;
    private final int[] savedOpacity;
    private final boolean[] savedThrough;
    /** One autonomous route player per event that uses move type 3 (custom). */
    private final MoveRoutePlayer[] autonomous;
    private final java.util.Random random = new java.util.Random();

    public EventCharacters(MapData data, TileMap map, GameState state) {
        this.data = data;
        this.state = state;
        players = new MoveRoutePlayer[data.events.size];
        transparent = new boolean[data.events.size];
        activePages = new MapData.EventPageData[data.events.size];
        savedOpacity = new int[data.events.size];
        savedThrough = new boolean[data.events.size];
        autonomous = new MoveRoutePlayer[data.events.size];
        for (int i = 0; i < data.events.size; i++) {
            MapData.EventData event = data.events.get(i);
            MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
            activePages[i] = page;
            MapData.EventGraphic graphic = page == null ? null : page.graphic;
            // R6.26 (all events own a character, like RGSS's Game_Event): a
            // blank or tile page has no sheet and stays on the static drawing
            // path, but Set Move Route, 202/208 and code 41 can address it and
            // the drawn tile follows the character's live position.
            MapCharacter character = new MapCharacter(event.x, event.y, map.width(), map.height(),
                    graphic != null && graphic.tileId == 0 ? graphic.characterName : null);
            if (graphic != null) {
                character.face(graphic.direction);
                character.basePattern(graphic.pattern);
                applyPageMovement(character, page);
            }
            characters.add(character);
        }
    }

    /** Runtime character per event index; null when the event has no sprite. */
    public Array<MapCharacter> characters() {
        return characters;
    }

    public MapCharacter character(int eventId) {
        int index = indexOf(eventId);
        return index < 0 ? null : characters.get(index);
    }

    public boolean transparent(int eventId) {
        int index = indexOf(eventId);
        return index >= 0 && transparent[index];
    }

    /** Change Transparent Flag (208): hides the sprite and disables blocking. */
    public void setTransparent(int eventId, boolean value) {
        int index = indexOf(eventId);
        if (index < 0) {
            return;
        }
        transparent[index] = value;
        MapData.EventPageData page = activePages[index];
        if (page == null || page.graphic == null) {
            return;
        }
        if (value) {
            // RMXP hides the sprite and stops it from blocking. The runtime
            // represents that on the page: opacity 0 plus through.
            savedOpacity[index] = page.graphic.opacity;
            savedThrough[index] = page.movement.through;
            page.graphic.opacity = 0;
            page.movement.through = true;
        } else {
            page.graphic.opacity = savedOpacity[index];
            page.movement.through = savedThrough[index];
        }
    }

    /** Set Event Location (202): teleports a character (eventId -1 = player). */
    public void setLocation(int eventId, int x, int y, int direction) {
        int index = indexOf(eventId);
        if (index < 0) {
            return;
        }
        MapCharacter character = characters.get(index);
        if (character == null) {
            return;
        }
        character.teleport(x, y);
        if (direction != 0) {
            character.face(direction);
        }
        players[index] = null; // RMXP stops a route when the location changes
        sync(index);
    }

    /** Set Move Route (209). */
    public void setMoveRoute(int eventId, MoveRoute route) {
        int index = indexOf(eventId);
        if (index >= 0 && characters.get(index) != null) {
            players[index] = new MoveRoutePlayer(route);
        }
    }

    /** Wait for Move's Completion (210) / MapPort.isMoving. */
    public boolean isMoving(int eventId) {
        int index = indexOf(eventId);
        return index >= 0 && players[index] != null && !players[index].finished();
    }

    /** Wait for Move's Completion (210) looks at every event's forced route. */
    public boolean anyRouteActive() {
        for (int i = 0; i < players.length; i++) {
            if (players[i] != null && !players[i].finished()) {
                return true;
            }
        }
        return false;
    }

    /** Advances every running route; the map side supplies the step context. */
    public void update(float delta, MoveRoutePlayer.Context context) {
        for (int i = 0; i < characters.size; i++) {
            MapCharacter character = characters.get(i);
            if (character == null) {
                continue;
            }
            MoveRoutePlayer player = players[i];
            if (player != null) {
                player.update(delta, character, context);
                if (player.finished()) {
                    players[i] = null;
                }
            } else {
                updateAutonomous(i, character, delta, context);
            }
            // RMXP advances every character each frame, so a step an event
            // route started has to land here too - otherwise NPC routes stall
            // on their first step for good (reported: "events never move").
            character.advance(delta);
            character.updateAnimation(delta);
            sync(i);
        }
    }

    /** Copies the page's movement options onto a runtime character (R6.10). */
    public void applyPageMovement(MapCharacter character, MapData.EventPageData page) {
        if (character == null || page == null) {
            return;
        }
        MapData.EventMovement movement = page.movement;
        character.moveType = movement.moveType;
        character.moveSpeed(movement.moveSpeed);
        character.moveFrequency = movement.moveFrequency;
        character.customRoute = movement.moveRoute;
        character.through = movement.through;
        character.alwaysOnTop = movement.alwaysOnTop;
        character.walkAnime = movement.walkAnime;
        character.stepAnime = movement.stepAnime;
        character.directionFix = movement.directionFix;
    }

    /**
     * RGSS autonomous movement ({@code Game_Character#update_command_new}):
     * every {@code move_frequency} frames a stopped event takes one action -
     * wander (type 1), walk toward the player (type 2) or continue its own
     * move route (type 3).
     */
    private void updateAutonomous(int index, MapCharacter character, float delta,
                                  MoveRoutePlayer.Context context) {
        if (character.moveType == 0 || character.isMoving()) {
            return;
        }
        character.stopCount += Math.max(0f, delta) * 40f;
        if (character.stopCount < MapCharacter.moveFrequencyFrames(character.moveFrequency)) {
            return;
        }
        switch (character.moveType) {
            case 1: // random: mostly wander, sometimes forward, sometimes pause
                character.stopCount = 0f; // one action per frequency interval
                switch (random.nextInt(6)) {
                    case 0: case 1: case 2: case 3:
                        stepRandom(character, context);
                        break;
                    case 4:
                        context.step(character, character.direction());
                        break;
                    default:
                        break;
                }
                break;
            case 2: // toward the player (RMXP keeps its distance beyond 20 tiles)
                character.stopCount = 0f;
                int dx = Math.abs(character.x() - context.playerX());
                int dy = Math.abs(character.y() - context.playerY());
                if (context.playerX() < 0 || dx + dy >= 20) {
                    stepRandom(character, context);
                    break;
                }
                switch (random.nextInt(6)) {
                    case 0: case 1: case 2: case 3:
                        stepTowardPlayer(character, context, true);
                        break;
                    case 4:
                        stepRandom(character, context);
                        break;
                    default:
                        context.step(character, character.direction());
                        break;
                }
                break;
            case 3: // custom route from the page, repeated while it says so
                MoveRoute route = character.customRoute;
                if (route == null || route.size() == 0) {
                    return;
                }
                if (autonomous[index] == null || autonomous[index].finished()) {
                    autonomous[index] = new MoveRoutePlayer(route);
                }
                autonomous[index].update(delta, character, context);
                // A route may need several frames (it walks tile by tile), so
                // only the finished cycle starts a new frequency wait.
                if (autonomous[index].finished() || autonomous[index].cycleDone()) {
                    character.stopCount = 0f;
                }
                break;
            default:
                break;
        }
    }

    private void stepRandom(MapCharacter character, MoveRoutePlayer.Context context) {
        int[] directions = { 2, 4, 6, 8 };
        context.step(character, directions[random.nextInt(directions.length)]);
    }

    /** RGSS {@code move_toward_player} / {@code turn_toward_player} geometry. */
    private void stepTowardPlayer(MapCharacter character, MoveRoutePlayer.Context context,
                                  boolean move) {
        int dx = context.playerX() - character.x();
        int dy = context.playerY() - character.y();
        if (dx == 0 && dy == 0) {
            return;
        }
        int direction;
        if (Math.abs(dx) > Math.abs(dy)) {
            direction = dx > 0 ? 6 : 4;
        } else {
            direction = dy > 0 ? 2 : 8;
        }
        if (move) {
            context.step(character, direction);
        } else {
            character.face(direction);
        }
    }

    /**
     * Copies the runtime character back into the immutable-looking map data.
     * MapData is a per-screen runtime object (the source project is never
     * written), so this is what makes moved/turned events show up in the
     * renderer and in collision without duplicating either of them.
     */
    private void sync(int index) {
        MapCharacter character = characters.get(index);
        MapData.EventData event = data.events.get(index);
        event.x = character.x();
        event.y = character.y();
        MapData.EventPageData page = activePages[index];
        if (page != null && page.graphic != null) {
            page.graphic.direction = character.direction();
        }
    }

    /**
     * Follows page changes: when a switch flips an event to another page with a
     * different character sheet, the runtime character takes over that sheet
     * (position and route stay untouched).
     */
    public void refreshGraphics() {
        for (int i = 0; i < data.events.size; i++) {
            MapCharacter character = characters.get(i);
            if (character == null) {
                continue;
            }
            MapData.EventPageData page = EventPages.resolve(state, data.mapId, data.events.get(i));
            MapData.EventGraphic graphic = page == null ? null : page.graphic;
            if (graphic != null && graphic.tileId == 0 && graphic.characterName != null
                    && !graphic.characterName.isEmpty()) {
                character.characterName = graphic.characterName;
                character.face(graphic.direction);
                character.basePattern(graphic.pattern);
            } else {
                // R6.21: the new page has no character sprite (a blank "done"
                // page such as a taken item ball, or a tile graphic). The old
                // sheet must go, otherwise the event looks alive, blocks
                // nothing and cannot be talked to any more (reported).
                character.characterName = null;
                character.basePattern(0);
            }
            if (page == null) {
                // R6.26: no active page means the event does not exist in
                // RMXP; its character must not keep wandering invisibly.
                character.moveType = 0;
                character.customRoute = null;
                players[i] = null;
                autonomous[i] = null;
            } else {
                // The new page brings its own movement options (R6.10).
                applyPageMovement(character, page);
            }
            activePages[i] = page;
        }
    }

    private int indexOf(int eventId) {
        for (int i = 0; i < data.events.size; i++) {
            if (data.events.get(i).id == eventId) {
                return i;
            }
        }
        return -1;
    }
}
