package pokemon.runtime.map;

import pokemon.runtime.audio.AudioManager;
import pokemon.runtime.data.MapData;
import pokemon.runtime.event.MoveRoutePlayer;
import pokemon.runtime.state.GameState;

import java.util.function.Consumer;

/**
 * R6.32: the map-side {@link MoveRoutePlayer.Context} the real screen uses.
 *
 * <p>It used to be an anonymous class inside MapScreen that never overrode
 * {@code playerX()/playerY()}. The interface defaults answer -1, so "move/turn
 * toward/away from the player" (codes 10/11 and 25/26) silently did nothing
 * and autonomous "move toward the player" fell back to random - the same
 * "port not wired" trap as R6.14, one layer down. Putting the context in its
 * own class keeps the wiring visible and headless-testable.</p>
 */
public final class MapRouteContext implements MoveRoutePlayer.Context {

    /** Player steps go through MapScreen (map connections / held-direction rules). */
    public interface PlayerStepper {
        boolean step(MapCharacter character, int direction);
    }

    private final GameState state;
    private final TileMap tileMap;
    private final MapData mapData;
    private final MapCharacter player;
    private final PlayerStepper playerStepper;
    private final AudioManager audio;
    private final Consumer<String> log;
    /** Code 41 changed a character sheet; the screen rebinds and clears this. */
    private boolean graphicsDirty;

    public MapRouteContext(GameState state, TileMap tileMap, MapData mapData,
                           MapCharacter player, PlayerStepper playerStepper,
                           AudioManager audio, Consumer<String> log) {
        this.state = state;
        this.tileMap = tileMap;
        this.mapData = mapData;
        this.player = player;
        this.playerStepper = playerStepper;
        this.audio = audio;
        this.log = log;
    }

    /** True once move route code 41 swapped a sheet; reading it resets it. */
    public boolean takeGraphicsDirty() {
        boolean dirty = graphicsDirty;
        graphicsDirty = false;
        return dirty;
    }

    /** The live player tile: codes 10/11/25/26 and autonomous type 2 read it. */
    @Override
    public int playerX() {
        return player.x();
    }

    @Override
    public int playerY() {
        return player.y();
    }

    @Override
    public boolean step(MapCharacter character, int direction) {
        if (character == player) {
            // R6.23: a forced route may cross a connection like a held
            // direction does.
            return playerStepper.step(character, direction);
        }
        int targetX = Collision.targetX(character.x(), direction);
        int targetY = Collision.targetY(character.y(), direction);
        if (playerBlocks(character, targetX, targetY)) {
            return false;
        }
        return Collision.canStep(state, tileMap, mapData, character, direction)
                && character.startMove(targetX, targetY, direction);
    }

    /**
     * L6c: RGSS {@code Game_Character#passableEx?} - the player is not
     * passable for a character that has a graphic, so a trainer walks up to
     * the hero and stops on the tile next to them instead of stepping onto
     * (through) them.
     */
    private boolean playerBlocks(MapCharacter character, int x, int y) {
        return x == player.x() && y == player.y()
                && !player.through
                && character.characterName != null && !character.characterName.isEmpty();
    }

    /** R6.32: codes 5-8 - one diagonal step (the route applied the facing rule). */
    @Override
    public boolean stepDiagonal(MapCharacter character, int horz, int vert) {
        int targetX = character.x() + (horz == 4 ? -1 : 1);
        int targetY = character.y() + (vert == 8 ? -1 : 1);
        if (playerBlocks(character, targetX, targetY)) {
            return false;
        }
        return Collision.canStepDiagonal(state, tileMap, mapData, character, horz, vert)
                && character.startDiagonalMove(targetX, targetY);
    }

    /** R6.18: code 14 - the landing tile must be passable (RGSS d=0). */
    @Override
    public boolean canLand(MapCharacter character, int x, int y) {
        return !playerBlocks(character, x, y)
                && Collision.canLand(state, tileMap, mapData, character, x, y);
    }

    /** R6.18: code 41 - the sheet swap needs its sprites rebound. */
    @Override
    public void changeGraphic(MapCharacter character, String name, int hue,
                              int direction, int pattern) {
        MoveRoutePlayer.Context.super.changeGraphic(character, name, hue, direction, pattern);
        graphicsDirty = true;
    }

    @Override
    public void playSe(String name) {
        if (audio != null) {
            audio.playSe(name);
        }
    }

    @Override
    public void setSwitch(int id, boolean value) {
        state.switches().set(id, value);
    }

    @Override
    public void warn(String message) {
        if (log != null) {
            log.accept(message);
        }
    }
}
