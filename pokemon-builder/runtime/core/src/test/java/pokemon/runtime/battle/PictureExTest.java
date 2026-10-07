package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B1: the {@code PictureEx} engine (PictureEx:1-520). The battle animations are
 * only faithful if this engine is, so its delays, durations and interpolation
 * rules are pinned here.
 */
class PictureExTest {

    private final List<String> played = new ArrayList<>();
    private final BattleSprite sprite = new BattleSprite("test", BattleSprite.Kind.IMAGE);

    private PictureEx picture() {
        return PictureEx.forSprite(sprite, PictureEx.Origin.TOP_LEFT,
                (name, volume, pitch) -> played.add(name + ":" + volume + ":" + pitch));
    }

    /**
     * One iteration of {@code PokeBattle_Animation#update} (PokeBattle_Animation
     * :54-55): the picture steps, then {@code setPictureIconSprite} copies it
     * onto the sprite.
     */
    private static void step(PictureEx p) {
        p.update();
        p.applyTo(p.sprite());
    }

    @Test
    @DisplayName("durations are in 1/20 s and become frames (PictureEx:145-153)")
    void durationsAreDoubled() {
        PictureEx p = picture();
        // moveDelta(delay=0, duration=4, ...) -> duration 4*40/20 = 8 frames.
        p.moveDelta(0, 4, 80, 0);
        for (int i = 0; i < 7; i++) {
            step(p);
        }
        assertTrue(p.running(), "the 8-frame process must still be running after 7 updates");
        assertNotEquals(80f, sprite.x, "and must not have reached its target yet");
        step(p);
        step(p);
        assertEquals(80f, sprite.x, 1e-3f, "after 9 updates the target is reached");
    }

    @Test
    @DisplayName("an XY process starts at the value it had when it was queued (PictureEx:346-347)")
    void moveStartsFromTheCurrentValue() {
        sprite.x = 10f;
        PictureEx p = picture();       // addSprite copies x/y out of the sprite (:23-24)
        p.moveXY(0, 4, 50f, 0f);
        step(p);                       // frame 1: fra = process[3] = 0
        assertEquals(10f, sprite.x, 1e-3f);
        step(p);                       // frame 2: fra = 1 -> 10 + 1*(50-10)/8
        assertEquals(15f, sprite.x, 1e-3f);
        step(p);                       // frame 3: fra = 2
        assertEquals(20f, sprite.x, 1e-3f);
    }

    @Test
    @DisplayName("a zero-duration set lands on the first update (PictureEx:380-381)")
    void zeroDurationIsImmediate() {
        PictureEx p = picture();
        p.setXY(0, 12f, 34f);
        p.setVisible(0, false);
        step(p);
        assertEquals(12f, sprite.x, 1e-3f);
        assertEquals(34f, sprite.y, 1e-3f);
        assertFalse(sprite.visible);
        assertFalse(p.running(), "a zero-duration process ends on the frame it runs");
    }

    @Test
    @DisplayName("a delay is in 1/20 s too, so delay 3 waits 6 updates (PictureEx:145-153)")
    void delayCountsBeforeTheFirstStep() {
        PictureEx p = picture();
        p.setXY(3, 99f, 0f);
        for (int i = 0; i < 6; i++) {
            step(p);
        }
        assertEquals(0f, sprite.x, 1e-3f, "still waiting after 6 updates");
        step(p);
        assertEquals(99f, sprite.x, 1e-3f, "the 7th update applies it");
    }

    @Test
    @DisplayName("setDelta adds to the live value at the frame it starts (PictureEx:349-352)")
    void deltaAccumulates() {
        sprite.x = 100f;
        PictureEx p = picture();
        p.setDelta(0, 5f, 0f);
        step(p);
        assertEquals(105f, sprite.x, 1e-3f, "a duration-0 delta lands on its first frame");
    }

    @Test
    @DisplayName("totalDuration converts frames back to 1/20 s (PictureEx:135-143)")
    void totalDurationHalves() {
        PictureEx p = picture();
        p.moveDelta(0, 16, 10f, 0f);
        assertEquals(16, p.totalDuration(), "16 -> 32 frames -> back to 16");
        // setVisible(7,true) is delay 7 in 1/20 s = 14 frames; the halving in
        // totalDuration turns it back into 7 - the "# 0 or 7" the send-out
        // animation's own comment mentions (Follower_Main:776).
        PictureEx q = picture();
        q.setVisible(7, true);
        assertEquals(7, q.totalDuration());
    }

    @Test
    @DisplayName("a callback fires after its delay, with the picture as its argument (PictureEx:126-129/432)")
    void callbackFiresWithThePicture() {
        sprite.x = 7f;
        PictureEx p = picture();
        List<Float> seen = new ArrayList<>();
        p.setCallback(2, pic -> seen.add(pic.x()));
        for (int i = 0; i < 4; i++) {      // delay 2 -> 4 frames
            step(p);
        }
        assertTrue(seen.isEmpty());
        step(p);
        assertEquals(1, seen.size());
        assertEquals(7f, seen.get(0), 1e-3f);
    }

    @Test
    @DisplayName("opacity, visible and SE steps follow the plugin (PictureEx:284-307/408-414)")
    void opacityVisibleAndSe() {
        PictureEx p = picture();
        p.moveOpacity(0, 2, 0f);           // duration 2 -> 4 frames
        p.setSE(0, "Battle recall", null, null);
        step(p);
        assertEquals("Battle recall:100:100", played.get(0));
        assertEquals(255f, sprite.opacity, 1e-3f, "fra = 0 on the first frame");
        step(p);
        // RMXP's opacity is an Integer property, so the interpolation truncates
        // (PictureEx:477 -> setPictureSprite:477).
        assertEquals(191f, sprite.opacity, 1e-3f);
        step(p);
        step(p);
        step(p);
        assertEquals(0f, sprite.opacity, 1e-3f);
    }

    @Test
    @DisplayName("zoom and angle are carried onto the sprite, angle keeps its sign (PictureEx:391-394/464-467)")
    void zoomAndAngle() {
        PictureEx p = picture();
        p.setZoom(0, 0f);
        p.moveAngle(0, 4, 1080f);
        step(p);
        assertEquals(0f, sprite.zoomX, 1e-3f);
        step(p);
        step(p);
        assertEquals(1080f * 2f / 8f, sprite.angle, 1e-3f, "frame 3 of an 8-frame turn");
    }

    @Test
    @DisplayName("the origin decides ox/oy from the source rect (PictureEx:483-498)")
    void originSetsOxOy() {
        BattleSprite box = new BattleSprite("box", BattleSprite.Kind.IMAGE);
        box.bitmapWidth = 160;
        box.bitmapHeight = 160;
        PictureEx p = PictureEx.forSprite(box, PictureEx.Origin.BOTTOM,
                (name, volume, pitch) -> { });
        step(p);
        assertEquals(80f, box.ox, 1e-3f);
        assertEquals(160f, box.oy, 1e-3f);
        PictureEx centred = PictureEx.forSprite(box, PictureEx.Origin.CENTER,
                (name, volume, pitch) -> { });
        step(centred);
        assertEquals(80f, box.ox, 1e-3f);
        assertEquals(80f, box.oy, 1e-3f);
    }

    @Test
    @DisplayName("an origin of nil leaves ox/oy alone (PokeBattle_SceneAnimations:50-57)")
    void nilOriginKeepsOxOy() {
        BattleSprite bg = new BattleSprite("battle_bg", BattleSprite.Kind.IMAGE);
        bg.bitmapWidth = 672;
        bg.bitmapHeight = 448;
        bg.ox = 3f;
        bg.oy = 4f;
        PictureEx p = PictureEx.forSprite(bg, PictureEx.Origin.NONE, (name, volume, pitch) -> { });
        step(p);
        assertEquals(3f, bg.ox, 1e-3f);
        assertEquals(4f, bg.oy, 1e-3f);
    }

    @Test
    @DisplayName("cubic interpolation matches getCubicPoint2 (PictureEx:40-69)")
    void cubicPoint() {
        float[] p = PictureEx.cubicPoint(new float[] { 0, 0, 0, 100, 100, 100, 100, 0 }, 0.5f);
        assertEquals(50f, p[0], 1e-3f);
        assertEquals(75f, p[1], 1e-3f);
    }
}
