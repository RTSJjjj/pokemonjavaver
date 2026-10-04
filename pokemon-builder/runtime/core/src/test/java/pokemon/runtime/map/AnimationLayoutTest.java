package pokemon.runtime.map;

import org.junit.jupiter.api.Test;
import pokemon.runtime.data.AnimationData;

import static org.junit.jupiter.api.Assertions.*;

class AnimationLayoutTest {
    @Test void grassIsInFrontOfItsOwnFeetButBehindTheNextRow() {
        assertTrue(AnimationLayout.grassDepth(23) > MapRenderer.eventDepth(23));
        assertTrue(AnimationLayout.grassDepth(23) < MapRenderer.eventDepth(24));
        assertTrue(AnimationLayout.grassDepth(23) < MapRenderer.tileDepth(23, 1));
        MapAnimations animations = new MapAnimations();
        AnimationData.Animation grass = animation(1, "DustandGrass", 1);
        grass.frameMax = 9;
        animations.startTile(grass, 12, 23, 1);
        assertTrue(AnimationLayout.groundGrass(animations.active().first()));
        animations.startTile(grass, 12, 24, 3);
        assertFalse(AnimationLayout.groundGrass(animations.active().peek()));
    }
    private AnimationData.Animation animation(int id, String sheet, int position) {
        AnimationData.Animation result = new AnimationData.Animation();
        result.id = id;
        result.graphic = sheet;
        result.position = position;
        return result;
    }

    @Test void croppedGrassSheetDoesNotRenumberCellsIntoSparkles() {
        assertEquals(576, AnimationLayout.sourceX(3));
        assertEquals(0, AnimationLayout.sourceY(3));
        assertEquals(0, AnimationLayout.sourceX(5));
        assertEquals(192, AnimationLayout.sourceY(5));
        assertEquals(192, AnimationLayout.sourceX(6));
        assertEquals(192, AnimationLayout.sourceY(6));
        for (int cell : new int[]{3, 5, 6}) assertTrue(AnimationLayout.hasCell(cell, 768, 384));
        assertFalse(AnimationLayout.hasCell(4, 768, 384));
        assertFalse(AnimationLayout.hasCell(9, 768, 384));
        assertFalse(AnimationLayout.hasCell(10, 768, 384));
        assertFalse(AnimationLayout.hasCell(-1, 768, 384));
    }

    @Test void grassIsAtFeetAndRemainsOnItsOwnTile() {
        AnimationData.Animation grass = animation(1, "DustandGrass", 1);
        assertEquals(226, AnimationLayout.tileAnchorY(grass, 30, 22));
        assertEquals(194, AnimationLayout.tileAnchorY(grass, 30, 23));
        assertEquals(226, AnimationLayout.characterAnchorY(grass, 224, 48));
        // Authored y=-8 plus a two-pixel inset keeps all three shapes in the tile.
        assertEquals(234, AnimationLayout.tileAnchorY(grass, 30, 22) - (-8));
        // Dust is authored with y=+16 and retains its tile-top origin.
        assertEquals(256, AnimationLayout.tileAnchorY(animation(2, "DustandGrass", 1), 30, 22));
    }

    @Test void bubblesHaveAnExplicitHeadRuleWhileOrdinaryDownEffectsStayDown() {
        assertEquals(158, AnimationLayout.characterAnchorY(animation(3, "029-Emotion01", 2), 100, 48));
        assertEquals(190, AnimationLayout.characterAnchorY(animation(3, "029-Emotion01", 2), 100, 80));
        assertEquals(136, AnimationLayout.characterAnchorY(animation(20, "light", 0), 100, 48));
        assertEquals(124, AnimationLayout.characterAnchorY(animation(20, "light", 1), 100, 48));
        assertEquals(112, AnimationLayout.characterAnchorY(animation(20, "light", 2), 100, 48));
    }
}
