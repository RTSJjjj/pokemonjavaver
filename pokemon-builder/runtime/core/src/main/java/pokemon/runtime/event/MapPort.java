package pokemon.runtime.event;

import pokemon.runtime.data.MoveRoute;

import com.badlogic.gdx.utils.JsonValue;

/**
 * The map side of the interpreter (project3 section 18). Keeping it behind an
 * interface lets the interpreter stay headless-testable: the game binds the
 * real map screen, tests bind a recorder.
 */
public interface MapPort {

    /**
     * Transfer Player (command 201).
     *
     * @param direction RMXP facing (2/4/6/8) or 0 to keep the current facing
     */
    void transfer(int mapId, int x, int y, int direction);

    /**
     * Transfer Player with the command's fade parameter (0 = no fade, other
     * values fade through black). Defaults to the plain transfer.
     */
    default void transfer(int mapId, int x, int y, int direction, int fade) {
        transfer(mapId, x, y, direction);
    }

    /**
     * Set Move Route (209); {@code eventId} -1 addresses the player, otherwise
     * an event of the current map. The map side plays the route over the next
     * frames (R6.3b-2).
     */
    default void setMoveRoute(int eventId, MoveRoute route) {
    }

    /**
     * Wait for Move's Completion (210): true while that character still walks
     * its route. The default answers false, so a map without routes never
     * blocks an event.
     */
    default boolean isMoving(int eventId) {
        return false;
    }

    /**
     * {@code $game_player.pbTriggeredTrainerEvents([2],false)} (Game_Player:104-119): ids of the Trainer(N) events that
     * see the player right now (PField_Battles:539 asks it to find a second trainer for a double battle).
     */
    default int[] triggeredTrainerEvents() {
        return new int[0];
    }

    /**
     * Wait for Move's Completion (210). RMXP's command carries no target: it
     * waits for the player's forced route and every event's forced route, which
     * is what lets a door script finish walking the hero into the doorway first.
     * The default keeps single-character test ports working.
     */
    default boolean anyRouteActive() {
        return isMoving(-1);
    }

    /**
     * Erases an event (Essentials {@code pbSmashThisEvent}): its sprite and its
     * blocking disappear until the map is loaded again. The default does
     * nothing, so a headless run stays unaffected.
     */
    default void eraseEvent(int eventId) {
    }

    /**
     * Set Event Location (202); {@code eventId} -1 moves the player, 0 the
     * event that runs the command. {@code direction} 0 keeps the facing.
     */
    default void setEventLocation(int eventId, int x, int y, int direction) {
    }

    /** Change Transparent Flag (208): hidden and passable while true. */
    default void setTransparent(int eventId, boolean transparent) {
    }

    /**
     * Essentials {@code Interpreter#getVariable} (PField_Field:827): the event
     * variable of event N, or null when it has none. The berry plants keep
     * their whole growth state here.
     */
    default int[] getEventVariable(int eventId) {
        return null;
    }

    /** Essentials {@code Interpreter#setVariable} (PField_Field:836). */
    default void setEventVariable(int eventId, int[] value) {
    }

    /**
     * Essentials {@code Game_Character#turn_down/left/right/up}: the berry
     * plants encode their growth stage in the event's facing
     * (PField_BerryPlants:325-331 - down planted/sprouted, left taller,
     * right flowering, up berries). Directions are RMXP's 2/4/6/8.
     */
    default void turnEvent(int eventId, int direction) {
    }

    /**
     * Essentials {@code get_character(N).onEvent?} - true when the <em>player</em>
     * stands on character N's tile ({@code Game_Event#onEvent?} compares
     * {@code $game_player} with the receiver). 0 = the running event, -1 = the
     * player, anything else an event id. The map screen answers from the live
     * positions; the default keeps headless ports at "false".
     */
    default boolean playerOnCharacter(int characterId) {
        return false;
    }

    /**
     * Essentials {@code pbGlobalLock} / {@code pbGlobalUnlock} (Messages:223-234):
     * every map event stops its autonomous movement (forced routes keep
     * running) until the matching unlock - the trainer intro uses it around the
     * battle, so NPCs freeze while the intro plays.
     */
    default void lockEvents(boolean locked) {
    }

    /** Essentials {@code pbTrainerEnd} (PField_Field:801-805): erase the route. */
    default void eraseRoute(int eventId) {
    }

    /**
     * Change Map Settings (204): type 0 = panorama ({@code [type,name,hue]}),
     * type 1 = fog ({@code [type,name,hue,opacity,blend,zoom,sx,sy]}); an empty
     * name clears the layer. Types the runtime does not draw are reported by
     * the map screen.
     */
    default void changeMapSettings(int type, JsonValue parameters) {
    }

    /** Change Fog Opacity (206): 0..255. */
    default void changeFogOpacity(int opacity) {
    }

    /** Scroll Map (203): direction (2 down / 4 left / 6 right / 8 up), tiles, speed 1..6. */
    default void scrollMap(int direction, int distance, int speed) {
    }

    /**
     * R6.26: {@code pbCaveEntrance} / {@code pbCaveExit} - the nested-band
     * animation plus its screen tone. Returns how long the running event has to
     * wait, in seconds; a headless port answers 0 and the event continues.
     */
    default float caveEntrance(boolean exiting) {
        return 0f;
    }

    /**
     * R6.27: Show Animation (207). {@code characterId} -1 = the player, 0 has
     * already been resolved to the running event, anything else is an event id.
     * The animation plays once and does not block the event (RMXP's own
     * command_207 ignores its wait parameter).
     */
    default void showAnimation(int characterId, int animationId) {
    }

    /**
     * R6.28: Essentials' {@code $scene.spriteset.addUserAnimation(id, x, y,
     * tinting, height)} - an animation anchored at a map tile's top centre
     * (grass rustle, dust, {@code pbExclaim}). {@code height} is the RPG::Sprite
     * height (1 = low, 3 = high).
     */
    default void showTileAnimation(int animationId, int x, int y, int height) {
    }

    /**
     * R6.30: {@code pbExclaim(event)} - the exclamation bubble (animation 3 by
     * default) above a character, and the wait for it. {@code characterId}
     * uses the usual addressing (-1 player, 0 running event, N event id).
     * Returns how long the event has to wait, in seconds; 0 skips the wait.
     */
    default float exclaim(int characterId, int animationId) {
        return 0f;
    }

    /**
     * L6: {@code pbNoticePlayer(event)} - the event notices the player: an
     * exclamation when they are not already facing each other (Essentials'
     * {@code pbFacingEachOther}), then the player turns toward the event and
     * the event walks up to the player. Returns how long the running event has
     * to wait (exclaim + walk) in seconds; 0 skips the wait.
     */
    default float noticePlayer(int characterId) {
        return 0f;
    }

    /**
     * L6: {@code pbSave} - the project's quiet save (no UI). Returns true when
     * the save file was written.
     */
    default boolean saveGame() {
        return false;
    }

    /**
     * R6.30: {@code pbPushThisBoulder} - moves the running boulder event one
     * tile in the player's facing direction (Essentials checks the boulder's
     * own tile with {@code passableStrict?} first). Returns how long the push
     * step takes, in seconds; 0 means the boulder did not move.
     */
    default float pushBoulder(int eventId) {
        return 0f;
    }

    /**
     * R6.30: the project's {@code toggle_liefeng_switches} plugin - every
     * {@code float_plate} event turns its self switch A on when the player
     * stands on it and off otherwise.
     */
    default void togglePlateSwitches() {
    }

    /** {@code pbStartSurfing} (179_PField_FieldMoves:723-732): the player jumps onto the water in front and surfs. */
    default void startSurfing() {
    }

    /** What {@code pbTalkToFollower} makes the event do after the map side started the cry, the emote and the routes. */
    final class FollowerTalkPlan {
        /** {@code pbWait(n)} after the emote, in 40 fps frames. */
        public int waitFrames;
        /** The lines, shown one after another. */
        public final java.util.List<String> messages = new java.util.ArrayList<>();
        /** {@code pbPokemonFound}: the item the follower hands over (internal name), or null. */
        public String foundItem;
        public int foundQuantity = 1;
        public String foundMessage;
        /** The follower's Pokemon name, for the found-item lines. */
        public String pokemonName;
    }

    /** {@code pbToggleFollowingPokemon(forced)} (297_Follower_Main:26-43); false when the map side does not run followers. */
    default boolean toggleFollower(String forced) {
        return false;
    }

    /** {@code pbPokemonFollow(eventId)} (297_Follower_Main:48-63). */
    default boolean startFollowing(int eventId) {
        return false;
    }

    /** {@code pbRemoveDependencies} / {@code pbRemoveDependenciesExceptFollower}. */
    default void removeDependencies(boolean exceptFollower) {
    }

    /** {@code pbRemoveDependency2(eventName)} (182_PField_DependentEvents:34-36). */
    default void removeDependency(String name) {
    }

    /** {@code pbAddDependency2(eventId, name, commonEvent)} (182_PField_DependentEvents:25-27). */
    default boolean addDependency(int eventId, String name, int commonEvent) {
        return false;
    }

    /** {@code pbTalkToFollower} (297_Follower_Main:68-82); null when there is nothing to say. */
    default FollowerTalkPlan talkToFollower() {
        return null;
    }

    // ---- 179_PField_FieldMoves: the field side of the hidden moves ----

    /** {@code $game_player.pbFacingEvent.name.downcase}, or null when no event is in front. */
    default String facingEventName() {
        return null;
    }

    /** x, y of the event {@link #facingEventName()} found, or null. */
    default int[] facingEventPosition() {
        return null;
    }

    /** {@code pbFacingTerrainTag}. */
    default int facingTerrainTag() {
        return 0;
    }

    /** {@code $game_player.terrain_tag}. */
    default int playerTerrainTag() {
        return 0;
    }

    /** {@code $game_map.passable?(player.x, player.y, player.direction, player)}. */
    default boolean facingPassable() {
        return false;
    }

    /** {@code $game_player.pbHasDependentEvents?}. */
    /**
     * {@code $game_player.setDefaultCharName(fishSheet, pattern, true)} of pbFishingBegin / pbFishingEnd
     * (170_PField_Field:1232-1269): shows one frame of the player's fishing sheet ({@code pattern} 0..15 = direction row
     * x 4 + column, rows ordered down / left / right / up). False when the project has no sheet for this state.
     */
    default boolean fishingFrame(boolean surfing, int pattern) {
        return false;
    }

    /** {@code $game_player.setDefaultCharName(nil, oldpattern)} (:1294): the walking graphic and the old frame come back. */
    default void endFishingFrame(int oldPattern) {
    }

    /** {@code $game_player.fullPattern} (the direction row x 4 + the column). */
    default int playerFullPattern() {
        return 0;
    }

    default boolean hasDependentEvents() {
        return false;
    }

    /** {@code $MapFactory.getTerrainTag(mapId, $game_player.x, $game_player.y)}. */
    default int terrainTagOnMap(int mapId) {
        return 0;
    }

    /**
     * {@code pbSmashEvent(event)} (179:231-248) for the event in front: it turns left, right and up, then disappears.
     * Returns the seconds until it is gone (the plugin's {@code pbWait(40*4/10)}).
     */
    default float smashFacingEvent() {
        return 0f;
    }

    /** {@code pbHiddenMoveAnimation(pokemon)} (179:78-189): starts the banner; returns its length in seconds, 0 when there is none. */
    default float hiddenMoveAnimation(pokemon.runtime.pokemon.Pokemon pokemon) {
        return 0f;
    }

    /** {@code pbAscendWaterfall} (179:919-936): the player swims up the waterfall; returns the seconds it takes. */
    default float ascendWaterfall() {
        return 0f;
    }

    /** A transfer through a fade ({@code pbFadeOutIn { transfer_player(cancelVehicles) }}); {@code keepVehicles} = {@code transfer_player(false)}. */
    /** {@code pbGetMessage(MessageTypes::MapNames, mapId)}: the name of a map. */
    default String mapName(int mapId) {
        return "";
    }

    default void transferThroughFade(int mapId, int x, int y, int direction, boolean keepVehicles) {
    }

    /** {@code pbSweetScent}'s red flash (179:818-839); returns its length in seconds. */
    default float sweetScentFlash() {
        return 0f;
    }

    /**
     * {@code pbFlyAnimation(landing, ...)} (340_Fly_Animation): {@code departure} is the plugin's {@code landing = true}
     * call before the fade (the player turns left, the bird's SE, the player is hidden), false the one after the arrival.
     * Returns the seconds it takes.
     */
    default float flyAnimation(boolean departure) {
        return 0f;
    }

    /** {@code $PokemonTemp.darknessSprite} exists (the map is dark). */
    default boolean darknessActive() {
        return false;
    }

    /** Flash's growing light circle (179:486-493); returns the seconds it takes. */
    default float flashDarkness() {
        return 0f;
    }
}
