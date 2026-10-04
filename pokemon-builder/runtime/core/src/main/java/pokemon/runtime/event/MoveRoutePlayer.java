package pokemon.runtime.event;

import pokemon.runtime.data.MoveRoute;
import pokemon.runtime.map.MapCharacter;
import pokemon.runtime.map.TilesetGeometry;

import java.util.Random;

/**
 * Plays one RMXP Move Route on a {@link MapCharacter} (project3 sections 13,
 * 18). Headless on purpose: the map side supplies the step function (so
 * collision stays in one place) and receives the side effects.
 *
 * <p>Codes follow RPG::MoveCommand (checked against this project's own
 * Game_Character#move_type_custom): 0 end, 1-4 step down/left/right/up, 5-8
 * diagonal steps, 9 random, 10/11 toward/away from the player, 12/13
 * forward/backward, 14 jump, 15 wait, 16-19 turn, 20-23 relative turns, 24
 * random facing, 25/26 turn toward/away from the player, 27/28 switch, 29
 * speed, 30 move frequency, 31-34 walk/step animation, 35/36 direction fix,
 * 37/38 through, 39/40 always on top, 41 change graphic, 42 opacity, 43 blend
 * type, 44 play SE.</p>
 */
public final class MoveRoutePlayer {

    /** Map-side hooks: stepping, player position and the route side effects. */
    public interface Context {
        /** Tries to start one step; false when something blocks it. */
        boolean step(MapCharacter character, int direction);

        default int playerX() {
            return -1;
        }

        default int playerY() {
            return -1;
        }

        default void playSe(String name) {
        }

        default void setSwitch(int id, boolean value) {
        }

        /**
         * R6.18: move route code 14 - true when the jump's landing tile is
         * passable ({@code Collision.canLand}, RGSS {@code passable?(x,y,0)}).
         */
        default boolean canLand(MapCharacter character, int x, int y) {
            return false;
        }

        /**
         * R6.32: move route codes 5-8 - one diagonal step (horz 4/6, vert 2/8).
         * The map side checks the two L-shaped paths (RGSS
         * {@code move_upper_left} & co.) and starts the step.
         */
        default boolean stepDiagonal(MapCharacter character, int horz, int vert) {
            return false;
        }

        /** R6.18: move route code 41 - swap the character sheet. */
        default void changeGraphic(MapCharacter character, String name, int hue,
                                   int direction, int pattern) {
            character.setGraphic(name, hue, direction, pattern);
        }

        default void warn(String message) {
        }
    }

    private final MoveRoute route;
    private final Random random = new Random();
    private int index;
    private float waitTimer;
    /** How long a non-skippable command has been blocked (safety net). */
    private float blockedTimer;
    private boolean finished;
    /** A repeating route restarted at its End command; this frame is over. */
    private boolean cycleDone;

    /** A blocked, non-skippable step is abandoned after this many seconds. */
    private static final float BLOCKED_LIMIT = 3f;

    public MoveRoutePlayer(MoveRoute route) {
        this.route = route;
    }

    public boolean finished() {
        return finished || route.size() == 0;
    }

    /** True once on the frame a repeating route wrapped around to its start. */
    public boolean cycleDone() {
        return cycleDone;
    }

    /** Advances the route; the character walks one step at a time. */
    public void update(float delta, MapCharacter character, Context context) {
        if (finished()) {
            return;
        }
        cycleDone = false;
        if (waitTimer > 0f) {
            waitTimer -= Math.max(0f, delta);
            if (waitTimer > 0f) {
                return;
            }
        }
        if (character.isMoving()) {
            return; // let the started step land first
        }
        int guard = 0;
        while (!finished() && !cycleDone && waitTimer <= 0f && !character.isMoving()) {
            if (++guard > 256) {
                context.warn("move route stopped after 256 commands");
                finished = true;
                return;
            }
            run(route.commands.get(index), character, context);
        }
    }

    private void run(MoveRoute.Command command, MapCharacter character, Context context) {
        switch (command.code) {
            case 0: // End (a repeating route starts over)
                if (route.repeat) {
                    // RGSS Game_Character#move_type_custom returns here, so a
                    // repeating route runs one pass per frame. Looping inside
                    // one frame made turn-only routes spin like a top.
                    index = 0;
                    cycleDone = true;
                } else {
                    finished = true;
                }
                return;
            case 15: // Wait (1/20 s units)
                waitTimer = Math.max(0, command.intParam(0, 1)) / 20f;
                advance();
                return;
            case 1:
            case 2:
            case 3:
            case 4:
                step(character, directionOf(command.code), context);
                return;
            case 5:
            case 6:
            case 7:
            case 8: // diagonal steps (RGSS move_upper_left & co.)
                diagonal(character, command.code, context);
                return;
            case 14: // Jump (x_plus, y_plus)
                jump(character, command.intParam(0, 0), command.intParam(1, 0), context);
                return;
            case 9:
                step(character, DIRECTIONS[random.nextInt(DIRECTIONS.length)], context);
                return;
            case 10:
                stepToward(character, context, true);
                return;
            case 11:
                stepToward(character, context, false);
                return;
            case 12:
                step(character, character.direction(), context);
                return;
            case 13:
                // RGSS move_backward temporarily fixes the direction, so the
                // character walks backwards without turning around.
                boolean fix = character.directionFix;
                character.directionFix = true;
                step(character, 10 - character.direction(), context);
                character.directionFix = fix;
                return;
            case 16:
            case 17:
            case 18:
            case 19:
                character.turn(directionOf(command.code - 15));
                advance();
                return;
            case 20: // turn 90 right: 2 -> 4 -> 8 -> 6 -> 2 (RMXP order)
                character.turn(turnRight(character.direction()));
                advance();
                return;
            case 21:
                character.turn(turnLeft(character.direction()));
                advance();
                return;
            case 22:
                character.turn(10 - character.direction());
                advance();
                return;
            case 23: // turn right or left 90, picked at random
                character.turn(random.nextBoolean()
                        ? turnRight(character.direction()) : turnLeft(character.direction()));
                advance();
                return;
            case 24:
                character.turn(DIRECTIONS[random.nextInt(DIRECTIONS.length)]);
                advance();
                return;
            case 25:
                character.turn(toward(character, context, true));
                advance();
                return;
            case 26:
                character.turn(toward(character, context, false));
                advance();
                return;
            case 27:
                context.setSwitch(Math.max(1, command.intParam(0, 1)), true);
                advance();
                return;
            case 28:
                context.setSwitch(Math.max(1, command.intParam(0, 1)), false);
                advance();
                return;
            case 29: // move_speed = parameters[0] (RGSS levels 1..6)
                character.moveSpeed(command.intParam(0, character.moveSpeedLevel));
                advance();
                return;
            case 30: // move frequency: only autonomous movement reads it
                character.moveFrequency = Math.max(1, Math.min(6, command.intParam(0, 4)));
                advance();
                return;
            case 31:
                character.walkAnime = true;
                advance();
                return;
            case 32:
                character.walkAnime = false;
                advance();
                return;
            case 33:
                character.stepAnime = true;
                advance();
                return;
            case 34:
                character.stepAnime = false;
                advance();
                return;
            case 35:
                character.directionFix = true;
                advance();
                return;
            case 36:
                character.directionFix = false;
                advance();
                return;
            case 43: // blend type: normal / additive / subtractive
                character.blendType = command.intParam(0, 0);
                advance();
                return;
            case 37:
                character.through = true;
                advance();
                return;
            case 38:
                character.through = false;
                advance();
                return;
            case 39:
                character.alwaysOnTop = true;
                advance();
                return;
            case 40:
                character.alwaysOnTop = false;
                advance();
                return;
            case 42:
                character.opacity = Math.max(0f, Math.min(1f, command.intParam(0, 255) / 255f));
                advance();
                return;
            case 41: // Change Graphic (name, hue, direction, pattern)
                context.changeGraphic(character, command.stringParam(0, ""),
                        command.intParam(1, 0), command.intParam(2, 0), command.intParam(3, 0));
                advance();
                return;
            case 44:
                String se = command.audioName();
                if (se != null && !se.isEmpty()) {
                    context.playSe(se);
                }
                advance();
                return;
            default:
                context.warn("move command " + command.code + " is not implemented yet; skipped");
                advance();
        }
    }

    private void step(MapCharacter character, int direction, Context context) {
        if (context.step(character, direction)) {
            blockedTimer = 0f;
            advance(); // the step itself blocks until it lands
            return;
        }
        blockedStep(context);
    }

    /**
     * R6.32: move route codes 5-8 - RGSS {@code move_lower_left} & co. The
     * facing rule first, then the diagonal step; a blocked step follows the
     * same skippable / 3 s safety rules as a straight one.
     */
    private void diagonal(MapCharacter character, int code, Context context) {
        int horz = (code == 5 || code == 7) ? 4 : 6;
        int vert = (code == 5 || code == 6) ? 2 : 8;
        if (!character.directionFix) {
            int direction = character.direction();
            if ((code == 5 || code == 7) && direction == 6) {
                direction = 4;
            } else if ((code == 6 || code == 8) && direction == 4) {
                direction = 6;
            } else if ((code == 7 || code == 8) && direction == 2) {
                direction = 8;
            } else if ((code == 5 || code == 6) && direction == 8) {
                direction = 2;
            }
            character.face(direction);
        }
        if (context.stepDiagonal(character, horz, vert)) {
            blockedTimer = 0f;
            advance();
            return;
        }
        blockedStep(context);
    }

    /**
     * R6.32: codes 10/11 - RGSS {@code move_toward_player} tries the dominant
     * axis first and falls back to the other one when the way is blocked
     * (ties pick the axis at random).
     */
    private void stepToward(MapCharacter character, Context context, boolean forward) {
        int playerX = context.playerX();
        int playerY = context.playerY();
        if (playerX < 0 || playerY < 0) {
            advance();
            return;
        }
        int dx = playerX - character.x();
        int dy = playerY - character.y();
        if (dx == 0 && dy == 0) {
            advance();
            return;
        }
        for (int direction : towardOrder(dx, dy, forward)) {
            if (direction != 0 && context.step(character, direction)) {
                blockedTimer = 0f;
                advance();
                return;
            }
        }
        blockedStep(context);
    }

    /** Dominant axis first (random on ties), then the other one; 0 = no axis. */
    private int[] towardOrder(int dx, int dy, boolean forward) {
        boolean horizontalFirst;
        if (Math.abs(dx) > Math.abs(dy)) {
            horizontalFirst = true;
        } else if (Math.abs(dy) > Math.abs(dx)) {
            horizontalFirst = false;
        } else {
            horizontalFirst = random.nextBoolean();
        }
        int horizontal = dx == 0 ? 0 : forward ? (dx > 0 ? 6 : 4) : (dx > 0 ? 4 : 6);
        int vertical = dy == 0 ? 0 : forward ? (dy > 0 ? 2 : 8) : (dy > 0 ? 8 : 2);
        return horizontalFirst
                ? new int[] {horizontal, vertical}
                : new int[] {vertical, horizontal};
    }

    /** Skippable commands advance; others retry with the shared 3 s safety net. */
    private void blockedStep(Context context) {
        if (route.skippable) {
            advance();
            return;
        }
        waitTimer = 0.1f;
        blockedTimer += 0.1f;
        if (blockedTimer >= BLOCKED_LIMIT) {
            context.warn("move route blocked for " + (int) blockedTimer + " s at command "
                    + route.commands.get(index).code + "; skipped");
            blockedTimer = 0f;
            advance();
        }
    }

    /**
     * R6.18: move route code 14 - RGSS {@code Game_Character#jump}. A blocked
     * landing retries like a blocked step (with the same 3 s safety net); a
     * jump in place only bobs, which is how the Pokemon on the maps hop.
     */
    private void jump(MapCharacter character, int plusX, int plusY, Context context) {
        if (plusX != 0 || plusY != 0) {
            // RGSS turns towards the dominant axis before jumping (turn_*
            // respects the direction fix).
            if (Math.abs(plusX) > Math.abs(plusY)) {
                character.turn(plusX < 0 ? 4 : 6);
            } else {
                character.turn(plusY < 0 ? 8 : 2);
            }
        }
        int targetX = character.x() + plusX;
        int targetY = character.y() + plusY;
        boolean inPlace = plusX == 0 && plusY == 0;
        if (!inPlace && !context.canLand(character, targetX, targetY)) {
            if (route.skippable) {
                blockedTimer = 0f;
                advance();
            } else {
                waitTimer = 0.1f;
                blockedTimer += 0.1f;
                if (blockedTimer >= BLOCKED_LIMIT) {
                    context.warn("move route blocked for " + (int) blockedTimer + " s at command "
                            + route.commands.get(index).code + "; skipped");
                    blockedTimer = 0f;
                    advance();
                }
            }
            return;
        }
        float distance = (float) Math.sqrt((double) plusX * plusX + (double) plusY * plusY);
        float peak = Math.max(1f, distance) * TilesetGeometry.TILE_SIZE * 3f / 8f;
        character.startJump(targetX, targetY, peak);
        blockedTimer = 0f;
        advance(); // the jump itself blocks the route until it lands
    }

    private void advance() {
        index++;
        if (index >= route.size()) {
            if (route.repeat) {
                index = 0;
            } else {
                finished = true;
            }
        }
    }

    private static final int[] DIRECTIONS = { 2, 4, 6, 8 };

    private static int directionOf(int moveCode) {
        switch (moveCode) {
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 6;
            case 4:
                return 8;
            default:
                return 2;
        }
    }

    private static int turnRight(int direction) {
        return direction == 2 ? 4 : direction == 4 ? 8 : direction == 8 ? 6 : 2;
    }

    private static int turnLeft(int direction) {
        return direction == 2 ? 6 : direction == 6 ? 8 : direction == 8 ? 4 : 2;
    }

    private static int toward(MapCharacter character, Context context, boolean forward) {
        int playerX = context.playerX();
        int playerY = context.playerY();
        if (playerX < 0 || playerY < 0) {
            return character.direction();
        }
        int dx = playerX - character.x();
        int dy = playerY - character.y();
        int direction;
        if (Math.abs(dx) > Math.abs(dy)) {
            direction = dx > 0 ? 6 : 4;
        } else {
            direction = dy > 0 ? 2 : 8;
        }
        return forward ? direction : 10 - direction;
    }
}
