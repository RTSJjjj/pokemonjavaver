package pokemon.runtime.map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.18: the jump state machine of {@link MapCharacter}. RGSS moves the tile
 * position immediately and glides the sprite with a parabola; the durations
 * come from this project's engine (12.8 quarter-pixels per 40 fps frame,
 * REAL_RES_Y twice REAL_RES_X).
 */
class MapCharacterJumpTest {

    @Test
    @DisplayName("horizontal jumps take 10 frames, vertical ones 20 (RGSS speeds)")
    void durations() {
        MapCharacter character = new MapCharacter(2, 2, 8, 8, "npc");
        character.startJump(3, 2, 12f);
        character.update(0.24f);
        assertTrue(character.isJumping(), "0.25 s not reached yet");
        character.update(0.02f);
        assertFalse(character.isJumping(), "a horizontal tile lands after 0.25 s");

        MapCharacter vertical = new MapCharacter(2, 2, 8, 8, "npc");
        vertical.startJump(2, 3, 12f);
        vertical.update(0.4f);
        assertTrue(vertical.isJumping(), "a vertical tile takes 0.5 s");
        vertical.update(0.2f);
        assertFalse(vertical.isJumping());
    }

    @Test
    @DisplayName("an in-place jump lifts by the peak and lands back on the ground")
    void arc() {
        MapCharacter character = new MapCharacter(2, 2, 8, 8, "npc");
        float ground = character.pixelY();
        character.startJump(2, 2, 24f);
        assertEquals(ground, character.pixelY(), 0.01f, "the jump starts on the ground");
        character.update(0.125f);
        assertEquals(ground + 24f, character.pixelY(), 0.01f, "the peak is mid-jump");
        character.update(0.2f);
        assertFalse(character.isJumping());
        assertEquals(ground, character.pixelY(), 0.01f, "and it lands on the same tile");
    }

    @Test
    @DisplayName("the tile position changes immediately, the paint position glides")
    void glides() {
        MapCharacter character = new MapCharacter(2, 2, 8, 8, "npc");
        float startX = character.pixelX();
        character.startJump(5, 2, 12f);
        assertEquals(5, character.x(), "RMXP sets the landing tile right away");
        assertTrue(character.isMoving(), "jumping counts as moving");
        assertEquals(startX, character.pixelX(), 0.01f, "the sprite starts on the old tile");

        character.update(0.375f); // half of the 3 tile / 0.75 s jump
        assertEquals(startX + 3f * TilesetGeometry.TILE_SIZE / 2f, character.pixelX(), 0.5f,
                "half way across");
        character.update(0.4f);
        assertFalse(character.isJumping());
        assertEquals(startX + 3f * TilesetGeometry.TILE_SIZE, character.pixelX(), 0.01f,
                "and lands on the target tile");
    }
}
