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
}
