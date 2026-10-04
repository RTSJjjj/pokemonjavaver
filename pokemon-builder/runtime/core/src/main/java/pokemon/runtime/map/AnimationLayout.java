package pokemon.runtime.map;

import pokemon.runtime.data.AnimationData;

/** Map animation geometry, independent of texture loading and the camera. */
final class AnimationLayout {
    static final int CELL_SIZE = 192;
    static final int COLUMNS = 5; // 0005.rb: pattern % 5, pattern / 5, even on cropped sheets
    // DustandGrass's last two leaf shapes extend two pixels below the authored
    // y=-8 centre. Keep their entire visible footprint inside the triggering tile.
    static final int GRASS_FOOT_INSET = 2;

    private AnimationLayout() { }

    static int sourceX(int pattern) { return pattern % COLUMNS * CELL_SIZE; }
    static int sourceY(int pattern) { return pattern / COLUMNS * CELL_SIZE; }

    static boolean hasCell(int pattern, int width, int height) {
        return pattern >= 0 && sourceX(pattern) + CELL_SIZE <= width
                && sourceY(pattern) + CELL_SIZE <= height;
    }

    static boolean grass(AnimationData.Animation animation) {
        return animation.id == 1 && "DustandGrass".equals(animation.graphic);
    }

    static boolean bubble(AnimationData.Animation animation) {
        return "029-Emotion01".equals(animation.graphic);
    }

    static boolean groundGrass(MapAnimations.Active active) {
        return active.tileX >= 0 && active.height == 1 && active.animation.position != 3
                && grass(active.animation);
    }

    static int grassDepth(int tileY) {
        // Just in front of a character standing on this tile (feet depth + 16),
        // but behind a character farther south, and behind overhead tiles.
        return (tileY + 1) * TilesetGeometry.TILE_SIZE + 17;
    }

    /** Grass is explicitly constrained to the foot of the tile it rustles. */
    static float tileAnchorY(AnimationData.Animation animation, int mapHeight, int tileY) {
        float bottom = (mapHeight - tileY - 1) * TilesetGeometry.TILE_SIZE;
        // Other addUserAnimation effects retain the source project's tile-top origin.
        return bottom + (grass(animation) ? GRASS_FOOT_INSET : TilesetGeometry.TILE_SIZE);
    }

    static float characterAnchorY(AnimationData.Animation animation, float feet, float spriteHeight) {
        if (grass(animation)) return feet + GRASS_FOOT_INSET;
        // This sheet contains bubbles above the cell centre. Keep them at the head,
        // including the position-2 exclamation, without changing all position-2 effects.
        // For bubbles the caller supplies the visible head height, excluding
        // transparent charset padding. The sheet's lowest tip at y=6 stays clear.
        if (bubble(animation)) return feet + spriteHeight + 10;
        // 0005.rb: sprite centre +/- one quarter of its actual frame height.
        if (animation.position == 0) return feet + spriteHeight * 0.75f;
        if (animation.position == 2) return feet + spriteHeight * 0.25f;
        return feet + spriteHeight * 0.5f;
    }
}
