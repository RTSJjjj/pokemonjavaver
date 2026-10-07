package pokemon.runtime.map;

import pokemon.runtime.data.MoveRoute;

/**
 * One walking character: the player or an NPC (13). Tile position plus a
 * smooth pixel interpolation between tiles, RMXP facing (2/4/6/8) and a walk
 * animation driven by movement progress. Pure data: rendering reads it,
 * input drives it.
 */
public final class MapCharacter {

    /** Tiles per second; RMXP default walking pace is 4 tiles per second. */
    public static final float DEFAULT_SPEED = 4f;
    public static final float RUN_SPEED = 8f;

    private int mapWidth;
    private int mapHeight;

    public String characterName;
    /**
     * P3: a runtime system (the berry plant sprites) owns this event's sheet.
     * An empty {@link #characterName} then hides the event instead of falling
     * back to the page graphic, mirroring RMXP's {@code character_name = ""}.
     */
    public boolean sheetOverridden;
    public String runningCharacterName;
    private boolean running;
    public boolean through;
    /** Move route code 39/40: drawn above every other character. */
    public boolean alwaysOnTop;
    /** Move route code 42: 1 = opaque, 0 = invisible. */
    public float opacity = 1f;
    /**
     * Standing column of the sheet (RMXP page "pattern"). Doors and other fixed
     * events store their image in a specific column, so idle characters must use
     * it instead of column 0; walking still cycles 1..3 around it.
     */
    public int basePattern;
    /** Move route code 35/36: a walking character keeps its facing. */
    public boolean directionFix;
    /** Move route codes 31-34: walk / step animation switches. */
    public boolean walkAnime = true;
    /** RGSS default: a character only marches in place when its page asks for it. */
    public boolean stepAnime;
    /** Move route code 30: 1..6, used by autonomous movement only. */
    public int moveFrequency = 4;
    /** Move route code 43: 0 normal, 1 additive, 2 subtractive. */
    public int blendType;
    /** RGSS autonomous move type: 0 fixed, 1 random, 2 toward the player, 3 custom. */
    public int moveType;
    /** The page's custom route, played by autonomous movement (type 3). */
    public MoveRoute customRoute;
    /** RGSS move speed level 1..6; {@link #speed()} keeps the tiles/second value. */
    public int moveSpeedLevel = 3;
    /** Frames stopped; autonomous movement fires once this passes the frequency. */
    public float stopCount;

    private int x;
    private int y;
    private int direction = 2;

    private int fromX;
    private int fromY;
    private int toX;
    private int toY;
    private float progress;
    private float speed = DEFAULT_SPEED;
    private int walkedTiles;
    /** RGSS animation column; {@link #basePattern} is the standing column. */
    private int pattern;
    private float animeCount;
    private boolean movedLastFrame;
    // R6.18: move route code 14 (jump). RMXP puts the character on the landing
    // tile immediately and glides @real_x/@real_y there on a parabola.
    private int jumpFromX;
    private int jumpFromY;
    private int jumpToX;
    private int jumpToY;
    private float jumpElapsed;
    private float jumpDuration;
    private float jumpPeak;

    public MapCharacter(int x, int y, int mapWidth, int mapHeight, String characterName) {
        this.x = x;
        this.y = y;
        this.fromX = x;
        this.fromY = y;
        this.toX = x;
        this.toY = y;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.characterName = characterName;
    }

    public int x() { return x; }
    public int y() { return y; }

    /**
     * RMXP {@code Game_Character#x} / {@code #y} <em>during a step</em>: the
     * source sets {@code @x}/{@code @y} to the destination when the step starts
     * and glides {@code @real_x}/{@code @real_y} there, so every reader of the
     * player's tile sees the destination for the whole step. This port keeps
     * {@link #x()} on the source tile until the step lands, so the destination
     * is exposed here for the readers that depend on the RMXP behaviour (the
     * {@code float_plate} toggle reads {@code $game_player.x}). A jump already
     * stores its landing tile immediately, so it needs no special case.
     */
    public int logicalX() { return hasStep() ? toX : x; }

    /** RMXP {@code $game_player.y} during a step; see {@link #logicalX()}. */
    public int logicalY() { return hasStep() ? toY : y; }

    /**
     * True for {@code $game_player}. {@code Game_Map#passable?} hands only the
     * player to {@code playerPassable?} (Game_Map:162), which is the one method
     * that consults {@code $PokemonGlobal.bridge}.
     */
    public boolean isPlayer;

    public int direction() { return direction; }
    public float speed() { return speed; }
    public void speed(float tilesPerSecond) { this.speed = Math.max(0.1f, tilesPerSecond); }

    /** RGSS {@code move_speed=}: 1 => 1 tile/s … 4 => running 8, 6 => 20 tiles/s. */
    public void moveSpeed(int level) {
        moveSpeedLevel = Math.max(1, Math.min(6, level));
        speed(SPEEDS[moveSpeedLevel]);
    }

    /** RGSS move_speed 1..6 -> tiles per second. */
    public static final float[] SPEEDS = { 0.5f, 1f, 2f, 4f, 8f, 10f, 20f };

    /** Frames RGSS waits between two autonomous movement actions. */
    public static float moveFrequencyFrames(int frequency) {
        int level = Math.max(1, Math.min(6, frequency));
        return (40 - level * 2) * (6 - level);
    }
    public void running(boolean value) {
        running = value;
        moveSpeedLevel = value ? 4 : 3;
        speed(value ? RUN_SPEED : DEFAULT_SPEED);
    }

    /** Page graphic change: the standing column of the walk animation. */
    public void basePattern(int column) {
        basePattern = Math.floorMod(column, 4);
        pattern = basePattern;
        animeCount = 0f;
    }
    public String graphicName() {
        return running && isMoving() && runningCharacterName != null ? runningCharacterName : characterName;
    }

    /** Faces a direction without moving (RMXP allows turning in place). */
    public void face(int direction) {
        this.direction = direction;
    }

    /**
     * R6.32: RGSS {@code turn_generic} - move route turn commands (16-26) are
     * ignored while the direction is fixed.
     */
    public void turn(int direction) {
        if (!directionFix) {
            face(direction);
        }
    }

    /** Set Event Location / transfer: jump to a tile without walking there. */
    public void teleport(int x, int y) {
        this.x = x;
        this.y = y;
        this.fromX = x;
        this.fromY = y;
        this.toX = x;
        this.toY = y;
        this.progress = 0f;
        cancelJump();
    }

    /**
     * R6.23: map connections rebind the character to the map it walks into.
     * {@link #pixelY()} converts rows against this height, so the teleport and
     * the rebind have to happen together to stay pixel-continuous.
     */
    public void setMapBounds(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }

    /**
     * R6.18: move route code 14 - RGSS {@code Game_Character#jump}. The tile
     * position changes immediately, the sprite glides to the landing tile with
     * a parabola whose peak is {@code max(1, sqrt(dx²+dy²)) * TILE * 3/8}, and
     * a jump in place only bobs. Speed follows this project's RGSS engine
     * (12.8 sub-pixels per 40 fps frame; {@code Game_Map:34-37} makes
     * {@code REAL_RES_X} and {@code REAL_RES_Y} both 128): one tile takes
     * 10 frames (0.25 s) in every direction.
     */
    public void startJump(int targetX, int targetY, float peakPixels) {
        jumpFromX = x;
        jumpFromY = y;
        jumpToX = targetX;
        jumpToY = targetY;
        jumpPeak = Math.max(0f, peakPixels);
        jumpElapsed = 0f;
        int dx = Math.abs(targetX - x);
        int dy = Math.abs(targetY - y);
        int units = Math.max(dx, dy);
        jumpDuration = (units <= 0 ? 1 : units) * 10f / 40f;
        x = targetX;
        y = targetY;
        fromX = x;
        fromY = y;
        toX = x;
        toY = y;
        progress = 0f;
        stopCount = 0f;
    }

    public boolean isJumping() {
        return jumpDuration > 0f;
    }

    /** 0..1 progress of the current jump (1 when not jumping). */
    public float jumpProgress() {
        if (!isJumping()) {
            return 1f;
        }
        return Math.min(1f, jumpElapsed / Math.max(0.0001f, jumpDuration));
    }

    /** Sprite lift above the ground, in pixels (y-up world). */
    private float jumpLift() {
        if (!isJumping()) {
            return 0f;
        }
        float f = Math.abs(jumpProgress() - 0.5f); // 0.5 -> 0 -> 0.5
        return jumpPeak * (1f - 4f * f * f);        // 0 -> peak -> 0
    }

    private void cancelJump() {
        jumpElapsed = 0f;
        jumpDuration = 0f;
        jumpPeak = 0f;
    }

    /**
     * R6.18: move route code 41 - change the character sheet (and optionally
     * the facing / standing column) like RGSS
     * {@code @character_name = params[0]} and friends. The map screen rebinds
     * its sprites when this runs.
     */
    public void setGraphic(String name, int hue, int direction, int pattern) {
        if (name != null && !name.isEmpty()) {
            characterName = name;
        }
        if (direction == 2 || direction == 4 || direction == 6 || direction == 8) {
            face(direction);
        }
        basePattern(Math.floorMod(pattern, 4));
    }

    /**
     * Starts a step to an adjacent tile. Returns false when the target is not
     * exactly one tile away (the caller checks passability first).
     */
    public boolean startMove(int targetX, int targetY, int direction) {
        return startMove(targetX, targetY, direction, false);
    }

    /**
     * R6.23: {@code allowOutside} lets a map-connection step leave the map
     * rectangle; the caller maps it into the neighbour once the step lands.
     */
    public boolean startMove(int targetX, int targetY, int direction, boolean allowOutside) {
        if (isMoving()
                || (!allowOutside && (targetX < 0 || targetY < 0
                        || targetX >= mapWidth || targetY >= mapHeight))
                || Collision.directionBit(direction) == 0
                || targetX != Collision.targetX(x, direction) || targetY != Collision.targetY(y, direction)) {
            return false;
        }
        return beginMove(targetX, targetY, direction);
    }

    /**
     * R6.32: move route codes 5-8 - one diagonal step. RGSS sets the facing
     * first (the route player applies {@code move_upper_left}'s rule) and then
     * moves both axes in parallel; the step takes as long as a straight one
     * (1 / speed).
     */
    public boolean startDiagonalMove(int targetX, int targetY) {
        if (isMoving()
                || targetX < 0 || targetY < 0
                || targetX >= mapWidth || targetY >= mapHeight
                || Math.abs(targetX - x) != 1 || Math.abs(targetY - y) != 1) {
            return false;
        }
        this.fromX = x;
        this.fromY = y;
        this.toX = targetX;
        this.toY = targetY;
        this.progress = 0f;
        this.stopCount = 0f;
        return true;
    }

    private boolean beginMove(int targetX, int targetY, int direction) {
        if (!directionFix) {
            this.direction = direction;
        }
        this.fromX = x;
        this.fromY = y;
        this.toX = targetX;
        this.toY = targetY;
        this.progress = 0f;
        this.stopCount = 0f;
        return true;
    }

    /** True from startMove until the step completes. */
    public boolean hasStep() {
        return fromX != toX || fromY != toY;
    }

    public boolean isMoving() {
        return hasStep() || isJumping();
    }

    /** Advances the current step; delta time drives the pace (13). */
    public void update(float delta) {
        advance(delta);
    }

    /** Returns time left after reaching the destination, so held movement is frame-rate independent. */
    public float advance(float delta) {
        delta = Math.max(0f, delta);
        if (isJumping()) {
            jumpElapsed += delta;
            if (jumpElapsed >= jumpDuration) {
                cancelJump();
            }
            return 0f;
        }
        if (!hasStep()) {
            return delta;
        }
        float needed = (1f - progress) / speed;
        if (delta >= needed) {
            x = toX;
            y = toY;
            fromX = x;
            fromY = y;
            progress = 0f;
            walkedTiles++;
            return Math.max(0f, delta - needed);
        }
        progress += delta * speed;
        return 0f;
    }

    /** Current pixel X (top-left of the tile cell). */
    public float pixelX() {
        if (isJumping()) {
            float t = jumpProgress();
            return (jumpFromX + (jumpToX - jumpFromX) * t) * TilesetGeometry.TILE_SIZE;
        }
        return (fromX + (toX - fromX) * progress) * TilesetGeometry.TILE_SIZE;
    }

    /** Current pixel Y in world coordinates (y-up, origin bottom-left). */
    public float pixelY() {
        if (isJumping()) {
            float t = jumpProgress();
            float row = jumpFromY + (jumpToY - jumpFromY) * t;
            return (mapHeight - row - 1) * TilesetGeometry.TILE_SIZE + jumpLift();
        }
        float row = fromY + (toY - fromY) * progress;
        return (mapHeight - row - 1) * TilesetGeometry.TILE_SIZE;
    }

    /**
     * Walk animation column 0..3, driven by {@link #updateAnimation} the same
     * way RGSS drives {@code @pattern} (walk anime, step anime, direction fix).
     */
    public int pattern() {
        return Math.floorMod(pattern, 4);
    }

    /**
     * RGSS {@code Game_Character#update_pattern}: advance the animation column
     * while walking (walk anime), while standing (step anime - the "march in
     * place" Pokemon bob) and return to the page's column when the step ends.
     *
     * @param delta seconds since the previous frame
     */
    public void updateAnimation(float delta) {
        boolean moving = isMoving();
        float frames = Math.max(0f, delta) * 40f; // RGSS counts frames at 40 fps
        if (movedLastFrame && !moving && !stepAnime) {
            pattern = basePattern;
            animeCount = 0f;
            movedLastFrame = false;
            return;
        }
        if (!movedLastFrame && moving && !stepAnime) {
            if (walkAnime) {
                pattern = (pattern + 1) % 4;
            }
            animeCount = 0f;
            movedLastFrame = true;
            return;
        }
        if ((moving && walkAnime) || (!moving && stepAnime)) {
            animeCount += frames;
        }
        // Half a tile per pattern, doubled for the cycling speed (RGSS). A jump
        // animates at jump_speed_real (12.8 quarter-pixels per 40 fps frame).
        float realSpeed = isJumping() ? 12.8f : speed * 3.2f;
        float framesPerPattern = 128f / (realSpeed * 2f);
        if (moveSpeedLevel == 6) {
            framesPerPattern *= 2f;
        }
        if (framesPerPattern > 0f && animeCount >= framesPerPattern) {
            pattern = (pattern + 1) % 4;
            animeCount -= framesPerPattern;
        }
        movedLastFrame = moving;
    }

    public int mapWidth() { return mapWidth; }
    public int mapHeight() { return mapHeight; }
}
