package pokemon.runtime.map;

import pokemon.runtime.input.InputManager;
import pokemon.runtime.input.GameAction;

/**
 * Continuous tile movement. Carries leftover frame time to the next step;
 * holding a direction never inserts a pause at every tile boundary.
 */
public final class MovementController {

    private int lastDirection;

    /**
     * @param input     frame input snapshot
     * @param delta     seconds since the previous frame
     * @param character the character to move
     * @param mover     attempts one step; returns true when it was taken
     */
    public void update(InputManager input, float delta, MapCharacter character, StepMover mover) {
        if (!Float.isFinite(delta) || delta <= 0f) return;
        character.running(input.isDown(GameAction.RUN));
        int direction = heldDirection(input);
        lastDirection = direction;
        float remaining = delta;
        while (remaining > 0f) {
            if (!character.isMoving()) {
                if (direction == 0) return;
                character.face(direction);
                if (!mover.tryStep(character, direction) || !character.isMoving()) return;
            }
            remaining = character.advance(remaining);
        }
    }

    private int heldDirection(InputManager input) {
        // Prefer a newly pressed direction, then retain the currently held one.
        if (input.wasPressed(GameAction.DOWN) && input.isDown(GameAction.DOWN)) return 2;
        if (input.wasPressed(GameAction.LEFT) && input.isDown(GameAction.LEFT)) return 4;
        if (input.wasPressed(GameAction.RIGHT) && input.isDown(GameAction.RIGHT)) return 6;
        if (input.wasPressed(GameAction.UP) && input.isDown(GameAction.UP)) return 8;
        if (lastDirection != 0 && input.isDown(directionAction(lastDirection))) return lastDirection;
        if (input.isDown(pokemon.runtime.input.GameAction.DOWN)) return 2;
        if (input.isDown(pokemon.runtime.input.GameAction.LEFT)) return 4;
        if (input.isDown(pokemon.runtime.input.GameAction.RIGHT)) return 6;
        if (input.isDown(pokemon.runtime.input.GameAction.UP)) return 8;
        return 0;
    }

    private static pokemon.runtime.input.GameAction directionAction(int direction) {
        switch (direction) {
            case 2: return pokemon.runtime.input.GameAction.DOWN;
            case 4: return pokemon.runtime.input.GameAction.LEFT;
            case 6: return pokemon.runtime.input.GameAction.RIGHT;
            default: return pokemon.runtime.input.GameAction.UP;
        }
    }

    /** One attempted step; returns true when the character started moving. */
    public interface StepMover {
        boolean tryStep(MapCharacter character, int direction);
    }
}
