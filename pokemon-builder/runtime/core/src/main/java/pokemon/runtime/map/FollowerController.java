package pokemon.runtime.map;

import pokemon.runtime.data.MapData;
import pokemon.runtime.field.FollowerRules;
import pokemon.runtime.field.PBTerrain;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.Dependent;
import pokemon.runtime.state.FieldGlobals;
import pokemon.runtime.state.GameState;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 182_PField_DependentEvents + 297_Follower_Main: the events that walk behind the player, above all the following
 * Pokemon. The entries live in {@link FieldGlobals#dependents} (the plugin's {@code $PokemonGlobal.dependentEvents});
 * this class owns their {@link MapCharacter}s on the map being shown and moves them the way
 * {@code pbMoveDependentEvents} / {@code pbFollowEventAcrossMaps} do.
 *
 * <p>登记: followers on a map connected to the current one (the plugin keeps the event on the neighbour map; here it
 * always stands on the map the player is on), shadows and reflections of the follower sprite, the stair tiles of the
 * plugin's modified passage test, and the second step of a two-tile catch-up (it jumps instead).</p>
 */
public final class FollowerController {

    /** What the map screen offers the controller. */
    public interface Host {
        /** {@code $scene.spriteset.addUserAnimation(id, x, y)} (the come-out / come-in animations). */
        void playAnimation(int animationId, int x, int y);

        /** {@code $game_map.name}. */
        String mapName();

        /** {@code pbGetMetadata($game_map.map_id, MetadataOutdoor) == true}. */
        boolean outdoor();

        /** {@code $PokemonEncounters.isEncounterPossibleHere?}. */
        boolean encounterPossible();

        /** {@code pbGetTerrainTag} of the tile the player stands on. */
        int playerTerrainTag();

        /** The graphic file exists below Graphics/Characters. */
        boolean characterExists(String path);

        /** Erases the event the dependent was taken from. */
        void eraseEvent(int eventId);

        /** The graphic of an event of the map (for {@code addEvent}); null when unknown. */
        String eventGraphic(int eventId);

        /** x, y, direction of an event of the map; null when unknown. */
        int[] eventPosition(int eventId);
    }

    private final GameState state;
    private final Host host;
    private TileMap map;
    private MapData data;
    private final MapCharacter player;
    private final Map<Dependent, MapCharacter> characters = new IdentityHashMap<>();
    private int lastTileX;
    private int lastTileY;
    private int lastDirection;
    private boolean turnPending;
    private float frames;

    public FollowerController(GameState state, TileMap map, MapData data, MapCharacter player, Host host) {
        this.state = state;
        this.map = map;
        this.data = data;
        this.player = player;
        this.host = host;
        lastTileX = player.logicalX();
        lastTileY = player.logicalY();
        lastDirection = player.direction();
    }

    private FieldGlobals globals() {
        return state.fieldGlobals();
    }

    private List<Dependent> entries() {
        return globals().dependents;
    }

    /** Rebinds to the map the player now stands on and puts every entry behind the player. */
    public void bind(TileMap newMap, MapData newData, boolean instant) {
        this.map = newMap;
        this.data = newData;
        characters.clear();
        for (Dependent entry : entries()) {
            entry.currentMap = data.mapId;
            characters.put(entry, createCharacter(entry));
        }
        if (data != null) {
            for (Dependent entry : entries()) {
                if (entry.originalMap == data.mapId) {
                    host.eraseEvent(entry.eventId);                                // DependentEventSprites#refresh
                }
            }
        }
        lastTileX = player.logicalX();
        lastTileY = player.logicalY();
        lastDirection = player.direction();
        followAll(true);
        refreshGraphics(false);
    }

    private MapCharacter createCharacter(Dependent entry) {
        MapCharacter character = new MapCharacter(entry.x, entry.y, map.width(), map.height(),
                "nil".equals(entry.characterName) || entry.characterName == null || entry.characterName.isEmpty()
                        ? null : entry.characterName);
        character.face(entry.direction == 0 ? 2 : entry.direction);
        character.sheetOverridden = true;
        character.moveSpeedLevel = player.moveSpeedLevel;
        character.speed(player.speed());
        return character;
    }

    /** The characters to draw, in the order of the entries. */
    public List<MapCharacter> characters() {
        List<MapCharacter> list = new ArrayList<>();
        for (Dependent entry : entries()) {
            MapCharacter character = characters.get(entry);
            if (character != null) {
                list.add(character);
            }
        }
        return list;
    }

    // ------------------------------------------------------------------
    // The entries (182_PField_DependentEvents:12-35, :428-498)
    // ------------------------------------------------------------------

    public boolean hasDependents() {
        return !entries().isEmpty();
    }

    /** {@code pbGetDependency("FollowerPkmn")}. */
    public Dependent follower() {
        for (Dependent entry : entries()) {
            if (entry.isFollower()) {
                return entry;
            }
        }
        return null;
    }

    public MapCharacter followerCharacter() {
        Dependent follower = follower();
        return follower == null ? null : characters.get(follower);
    }

    /** {@code pbHasDependentEvents?} with 297_Follower_Main:964: the follower alone does not count. */
    public boolean hasDependentEvents() {
        return follower() == null && !entries().isEmpty();
    }

    /** {@code pbRemoveDependencies} (182:12-15): every entry goes; {@code pbDeregisterPartner} is the caller's. */
    public void removeAll() {
        entries().clear();
        characters.clear();
    }

    /** {@code pbRemoveDependenciesExceptFollower} (297:160-171). */
    public void removeAllButFollower() {
        for (int i = entries().size() - 1; i >= 0; i--) {
            Dependent entry = entries().get(i);
            if (!entry.isFollower()) {
                entries().remove(i);
                characters.remove(entry);
            }
        }
    }

    public void removeByName(String name) {
        for (int i = entries().size() - 1; i >= 0; i--) {
            Dependent entry = entries().get(i);
            if (name.equals(entry.name)) {
                entries().remove(i);
                characters.remove(entry);
            }
        }
    }

    /** {@code pbAddDependency2(eventID, name, commonEvent)} (182:25-27, :474-498). */
    public Dependent addEvent(int eventId, String name, int commonEvent) {
        int[] position = host.eventPosition(eventId);
        if (position == null) {
            return null;
        }
        for (Dependent entry : entries()) {
            if (entry.originalMap == data.mapId && entry.eventId == eventId) {
                return entry;                                                      // :479 already exists
            }
        }
        Dependent entry = new Dependent();
        entry.originalMap = data.mapId;
        entry.eventId = eventId;
        entry.currentMap = data.mapId;
        entry.x = position[0];
        entry.y = position[1];
        entry.direction = position[2];
        String graphic = host.eventGraphic(eventId);
        entry.characterName = graphic == null ? "" : graphic;
        entry.name = name;
        entry.commonEvent = commonEvent;
        entries().add(entry);
        characters.put(entry, createCharacter(entry));
        host.eraseEvent(eventId);                                                  // :497 event.erase
        return entry;
    }

    // ------------------------------------------------------------------
    // The following Pokemon (297_Follower_Main:26-63, :145-322)
    // ------------------------------------------------------------------

    private Pokemon firstPokemon() {
        return state.trainer().party.firstAble();
    }

    /** {@code pbPokemonFollow(x)} (:48-63). */
    public boolean startFollowing(int eventId) {
        if (firstPokemon() == null) {
            return false;
        }
        if (follower() != null) {
            removeByName(Dependent.FOLLOWER_NAME);                                 // :50
        }
        Dependent entry = addEvent(eventId, Dependent.FOLLOWER_NAME, FollowerRules.FOLLOWER_COMMON_EVENT);   // :51
        state.followerToggled(true);                                               // :52
        if (entry == null) {
            return false;
        }
        followAll(true);                                                           // :54 instant, not the true leader
        comeBack(true);                                                            // :55
        return true;
    }

    /** {@code pbToggleFollowingPokemon(forced, anim)} (:26-43). */
    public void toggle(String forced, boolean anim) {
        if (follower() == null || firstPokemon() == null) {
            return;                                                                // :27-28
        }
        if (forced != null && !forced.isEmpty()) {
            if ("on".equalsIgnoreCase(forced)) state.followerToggled(false);       // :30
            if ("off".equalsIgnoreCase(forced)) state.followerToggled(true);       // :31
        }
        if (state.followerToggled()) {
            state.followerToggled(false);                                          // :34
            Boolean refresh = trigger(firstPokemon());                             // :35
            removeSprite(refresh == null || refresh);                              // :36
        } else {
            state.followerToggled(true);                                           // :39
            comeBack(anim);                                                        // :40
        }
    }

    private Boolean trigger(Pokemon pkmn) {
        FollowerRules.Context context = new FollowerRules.Context();
        FieldGlobals g = globals();
        context.bicycle = g.bicycle;
        context.surfing = g.surfing;
        context.diving = g.diving;
        context.outdoor = host.outdoor();
        context.encounterPossible = host.encounterPossible();
        context.mapName = host.mapName();
        return FollowerRules.refresh(pkmn, context);
    }

    /**
     * {@code refresh_sprite(anim, check)} (:174-193): whether the follower shows now. The plugin's -1 ("no handler
     * decided") is truthy and counts as showing.
     */
    public boolean refreshSprite(boolean anim) {
        Dependent entry = follower();
        if (entry == null || !state.followerToggled()) {
            return false;                                                          // :175-176
        }
        Pokemon pkmn = firstPokemon();
        if (pkmn == null) {
            return false;                                                          // :178
        }
        Boolean refresh = trigger(pkmn);                                           // :179
        boolean shows = refresh == null || refresh;
        if (shows && anim) {
            MapCharacter character = characters.get(entry);
            if (character != null) {
                host.playAnimation(FollowerRules.ANIMATION_COME_OUT, character.logicalX(), character.logicalY());   // :185
            }
        }
        return shows;
    }

    /** {@code remove_sprite(anim)} (:273-298): hides the sprite, the follower stays switched on. */
    public void removeSprite(boolean anim) {
        Dependent entry = follower();
        if (entry == null) {
            return;
        }
        entry.characterName = "nil";                                               // :277
        MapCharacter character = characters.get(entry);
        if (character != null) {
            character.characterName = null;
            if (anim) {
                host.playAnimation(FollowerRules.ANIMATION_COME_IN, character.logicalX(), character.logicalY());   // :280
            }
        }
        globals().timeTaken = 0;                                                   // :295
    }

    /** {@code come_back(anim)} (:301-311). */
    public boolean comeBack(boolean anim) {
        if (!state.followerToggled()) {
            return false;                                                          // :302
        }
        Pokemon first = firstPokemon();
        if (first == null) {
            return false;                                                          // :304
        }
        removeSprite(false);                                                       // :305
        boolean shows = refreshSprite(anim);                                       // :306
        if (shows) {
            changeSprite(first);                                                   // :307
        }
        return shows;
    }

    /** {@code change_sprite} (:196-252): the first graphic that exists for the Pokemon. */
    private void changeSprite(Pokemon pkmn) {
        Dependent entry = follower();
        if (entry == null) {
            return;
        }
        for (String path : FollowerRules.spriteCandidates(pkmn)) {
            if (host.characterExists(path)) {
                entry.characterName = path;
                MapCharacter character = characters.get(entry);
                if (character != null) {
                    character.characterName = path;
                }
                return;
            }
        }
    }

    /** Called on map entry: shows the follower again without the come-out animation. */
    private void refreshGraphics(boolean anim) {
        if (follower() != null && state.followerToggled()) {
            comeBack(anim);
        }
    }

    /** True while the following Pokemon is on screen ({@code refresh_sprite(false, true)}). */
    public boolean followerShows() {
        return refreshSprite(false);
    }

    // ------------------------------------------------------------------
    // Movement (182:194-313 with the 297:1215-1362 overrides)
    // ------------------------------------------------------------------

    private static int rightOf(int d) {
        return new int[] {0, 0, 4, 0, 8, 0, 2, 0, 6}[d];
    }

    private static int leftOf(int d) {
        return new int[] {0, 0, 6, 0, 2, 0, 8, 0, 4}[d];
    }

    private boolean passableTile(MapCharacter follower, int x, int y) {
        if (!map.valid(x, y)) {
            return false;
        }
        boolean through = follower.through;
        follower.through = false;
        boolean passable = Collision.canLand(state, map, data, follower, x, y);
        follower.through = through;
        if (!passable && state.bridge() > 0) {
            passable = PBTerrain.isBridge(map.terrainTag(x, y, false, state.bridge()));   // 297:1260-1261 a bridge tile above the water
        } else if (passable && !globals().surfing && state.bridge() == 0) {
            passable = !PBTerrain.isWater(map.terrainTag(x, y, false, state.bridge()));   // 297:1262-1264
        }
        return passable;
    }

    private void followAll(boolean instant) {
        MapCharacter leader = player;
        boolean trueLeader = true;
        for (Dependent entry : entries()) {
            MapCharacter follower = characters.get(entry);
            if (follower == null) {
                continue;
            }
            followEvent(leader, follower, instant, trueLeader);
            entry.x = follower.logicalX();
            entry.y = follower.logicalY();
            entry.direction = follower.direction();
            leader = follower;
            trueLeader = false;
        }
    }

    /** {@code pbFollowEventAcrossMaps(leader, follower, instant, leaderIsTrueLeader)} for one map. */
    private void followEvent(MapCharacter leader, MapCharacter follower, boolean instant, boolean trueLeader) {
        int d = leader.direction();
        int leaderX = leader.logicalX();
        int leaderY = leader.logicalY();
        int facing = 10 - d;                                                      // :1220
        if (!trueLeader) {                                                        // :1221-1232
            int dx = follower.logicalX() - leaderX;
            int dy = follower.logicalY() - leaderY;
            if (dy == 0 && dx == 2) facing = 6;
            else if (dy == 0 && dx == -2) facing = 4;
            else if (dy == -2 && dx == 0) facing = 8;
            else if (dy == 2 && dx == 0) facing = 2;
        }
        List<Integer> facings = new ArrayList<>();
        facings.add(facing);                                                      // :1233 from behind
        facings.add(rightOf(d));                                                  // :1234
        facings.add(leftOf(d));                                                   // :1235
        if (!trueLeader) {
            facings.add(d);                                                       // :1237 forward
        }
        int bestX = -1;
        int bestY = -1;
        double best = -1;
        for (int i = 0; i < facings.size(); i++) {
            int direction = facings.get(i);
            int tx = Collision.targetX(leaderX, direction);
            int ty = Collision.targetY(leaderY, direction);
            boolean passable = passableTile(follower, tx, ty);
            if (i == 0 && !passable && map.valid(tx, ty) && PBTerrain.isLedge(map.terrainTag(tx, ty, false, state.bridge()))) {
                tx = Collision.targetX(tx, direction);                           // :1265-1274 from further behind the ledge
                ty = Collision.targetY(ty, direction);
                passable = passableTile(follower, tx, ty);
            }
            if (passable) {
                double distance = Math.sqrt((double) (tx - follower.logicalX()) * (tx - follower.logicalX())
                        + (double) (ty - follower.logicalY()) * (ty - follower.logicalY()));   // :1276-1278
                if (best < 0 || best > distance) {
                    best = distance;
                    bestX = tx;
                    bestY = ty;
                }
                if (i == 0 && distance <= 1) {
                    break;                                                        // :1283 prefer behind
                }
            }
        }
        if (best < 0) {
            follower.teleport(leaderX, leaderY);                                  // :1342-1348 no free tile: the leader's tile
            turnToward(follower, leader);
            return;
        }
        int newX = bestX;
        int newY = bestY;
        int posX = newX + (d == 6 ? -1 : d == 4 ? 1 : 0);                         // :1310-1313
        int posY = newY + (d == 2 ? -1 : d == 8 ? 1 : 0);
        follower.moveSpeedLevel = leader.moveSpeedLevel;                          // :1314 sync movespeed
        follower.speed(leader.speed());
        int fx = follower.logicalX();
        int fy = follower.logicalY();
        int distance = Math.abs(fx - newX) + Math.abs(fy - newY);
        boolean straight = fx == newX || fy == newY;
        if (distance == 1 || (distance == 2 && straight)) {                        // :1315-1332
            if (instant) {
                follower.teleport(newX, newY);
            } else {
                fancyMoveTo(follower, newX, newY);
            }
        } else if (fx != posX || fy != posY) {                                    // :1333
            follower.teleport(newX, newY);                                        // far away: it appears at once (pbFancyMoveTo's moveto)
        }
    }

    /** {@code pbFancyMoveTo} (182:120-140) on the same map: one step through everything, a jump for two. */
    private void fancyMoveTo(MapCharacter follower, int newX, int newY) {
        if (follower.hasStep()) {
            follower.teleport(follower.logicalX(), follower.logicalY());           // the earlier step lands first
        }
        int x = follower.x();
        int y = follower.y();
        boolean through = follower.through;
        follower.through = true;
        if (Math.abs(x - newX) + Math.abs(y - newY) == 1) {
            int direction = newX > x ? 6 : newX < x ? 4 : newY > y ? 2 : 8;
            follower.startMove(newX, newY, direction);                            // moveFancy
        } else if (Math.abs(x - newX) + Math.abs(y - newY) == 2 && (x == newX || y == newY)) {
            int direction = newX > x ? 6 : newX < x ? 4 : newY > y ? 2 : 8;
            follower.face(direction);
            follower.startJump(newX, newY, TilesetGeometry.TILE_SIZE * 3f / 8f * 2f);   // jumpFancy's jump
        } else {
            follower.teleport(newX, newY);
        }
        follower.through = through;
    }

    /** {@code pbTurnTowardEvent(event, other)} (170_PField_Field:1172-1186). */
    private void turnToward(MapCharacter event, MapCharacter other) {
        int sx = event.logicalX() - other.logicalX();
        int sy = event.logicalY() - other.logicalY();
        if (sx == 0 && sy == 0) {
            return;
        }
        if (Math.abs(sx) > Math.abs(sy)) {
            event.turn(sx > 0 ? 4 : 6);
        } else {
            event.turn(sy > 0 ? 8 : 2);
        }
    }

    // ------------------------------------------------------------------
    // Per frame
    // ------------------------------------------------------------------

    /** Before the player moves this frame: the followers' steps advance (their step ends when the player's does). */
    public void advance(float delta) {
        for (MapCharacter character : characters.values()) {
            character.advance(delta);
        }
    }

    /**
     * After the player moved this frame ({@code Game_Player#move_generic}:81 {@code pbMoveDependentEvents}, :338
     * {@code updateDependentEvents}, :400 {@code pbTurnDependentEvents}).
     */
    public void afterPlayer(float delta) {
        int tx = player.logicalX();
        int ty = player.logicalY();
        if (tx != lastTileX || ty != lastTileY) {
            boolean far = Math.abs(tx - lastTileX) + Math.abs(ty - lastTileY) > 2;   // a transfer: Game_Player#moveto
            lastTileX = tx;
            lastTileY = ty;
            followAll(far);
            turnPending = true;
        }
        lastDirection = player.direction();
        if (turnPending && !player.isMoving()) {
            turnPending = false;
            MapCharacter leader = player;
            for (Dependent entry : entries()) {
                MapCharacter follower = characters.get(entry);
                if (follower == null) {
                    continue;
                }
                if (!follower.directionFix && !follower.isMoving()) {
                    turnToward(follower, leader);
                }
                entry.direction = follower.direction();
                leader = follower;
            }
        }
        Dependent followerEntry = follower();
        boolean shows = followerEntry != null && state.followerToggled() && refreshSprite(false);
        for (Dependent entry : entries()) {
            MapCharacter character = characters.get(entry);
            if (character == null) {
                continue;
            }
            character.moveSpeedLevel = player.moveSpeedLevel;
            character.speed(player.speed());
            if (entry.isFollower() && shows) {
                // update_stepping (:255-265): ALWAYS_ANIMATE, except on ice
                character.stepAnime = !PBTerrain.isIce(host.playerTerrainTag());
            }
            character.updateAnimation(delta);
            entry.x = character.logicalX();
            entry.y = character.logicalY();
        }
        if (shows) {                                                               // 297:973 add_following_time (per frame)
            frames += delta * 40f;
            while (frames >= 1f) {
                frames -= 1f;
                globals().timeTaken++;
                Pokemon first = firstPokemon();
                if (first != null && globals().timeTaken % 5000 == 0) {
                    first.happiness = Math.min(255, first.happiness + 1);           // :156
                }
                if (globals().timeTaken > 15000) {
                    globals().followerHoldItem = true;                              // :157
                }
            }
        }
    }

    /** The dependent standing on a tile, or null. */
    public Dependent at(int x, int y) {
        for (Dependent entry : entries()) {
            MapCharacter character = characters.get(entry);
            if (character != null && character.characterName != null && !character.characterName.isEmpty()
                    && character.logicalX() == x && character.logicalY() == y) {
                return entry;
            }
        }
        return null;
    }

    public MapCharacter characterOf(Dependent entry) {
        return characters.get(entry);
    }
}
