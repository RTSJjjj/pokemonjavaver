package pokemon.runtime.map;

import pokemon.runtime.input.InputManager;
import pokemon.runtime.input.GameAction;

/**
 * Continuous tile movement. Carries leftover frame time to the next step;
 * holding a direction never inserts a pause at every tile boundary.
 */
public final class MovementController {

    /** {@code Graphics.frame_rate / 20} frames at 40 fps, plus the frame the key went down: 3 frames. */
    private static final float TURN_DELAY = 3f / 40f;

    private int lastDirection;
    /** Seconds since the direction input last changed ({@code @lastdirframe}). */
    private float heldSeconds;
    /** {@code @moved_last_frame}: the character was walking at the end of the previous frame. */
    private boolean movedLastFrame;
    private boolean runToggle;
    /** 0 = hold to run, 1 = toggle auto-run (PScreen_Options 跑步键). */
    public int runStyle;
    /** 026_Game_Player_Visuals:56-66: the move speed level of the vehicle (5 cycling, 4 surfing), 0 = none. */
    public int vehicleSpeedLevel;

    /**
     * @param input     frame input snapshot
     * @param delta     seconds since the previous frame
     * @param character the character to move
     * @param mover     attempts one step; returns true when it was taken
     */
    public void update(InputManager input, float delta, MapCharacter character, StepMover mover) {
        if (!Float.isFinite(delta) || delta <= 0f) return;
        if (runStyle == 1) {
            if (input.wasPressed(GameAction.RUN)) {
                runToggle = !runToggle;
            }
            character.running(runToggle);
        } else {
            character.running(input.isDown(GameAction.RUN));
        }
        if (vehicleSpeedLevel > 0 && !character.isMoving()) {
            character.moveSpeed(vehicleSpeedLevel);
        }
        // 025_Game_Player:350-375 update_command_new: a direction that was just pressed only turns the player; the step
        // follows when it is still held more than frame_rate/20 = 2 frames later, and a step that follows one at once
        // (the player moved last frame) needs no such wait.
        int previous = lastDirection;
        int direction = heldDirection(input);
        if (direction != previous) {
            heldSeconds = 0f;                                                   // :373 @lastdirframe = Graphics.frame_count
        } else {
            heldSeconds += delta;
        }
        lastDirection = direction;                                              // :374 @lastdir = dir
        boolean chained = movedLastFrame;
        boolean moved = false;
        float remaining = delta;
        while (remaining > 0f) {
            if (!character.isMoving()) {
                if (direction == 0) break;
                boolean mayMove = chained || moved || (direction == previous && heldSeconds >= TURN_DELAY);   // :355-356
                character.face(direction);                                      // :363-369 turn_*
                if (!mayMove) break;
                if (!mover.tryStep(character, direction) || !character.isMoving()) break;
                moved = true;
            }
            remaining = character.advance(remaining);
        }
        movedLastFrame = moved || character.isMoving();
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
