package pokemon.runtime.map;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.data.AnimationData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Show Animation playback. The project's 0005.rb uses two game
 * frames at 40 fps and fires each timing on its own animation frame.
 */
class MapAnimationsTest {

    private static AnimationData.Animation animation(int frameMax) {
        StringBuilder frames = new StringBuilder();
        for (int i = 0; i < frameMax; i++) {
            frames.append(i == 0 ? "" : ",").append(
                    "[{\"cell\":").append(i).append(",\"x\":8,\"y\":5,\"zoom\":100,")
                    .append("\"angle\":0,\"flip\":false,\"opacity\":255,\"blend\":0}]");
        }
        AnimationData data = AnimationData.parse(new JsonReader().parse(
                "{\"animations\":[{\"id\":1,\"name\":\"Test\",\"graphic\":\"sheet\","
                        + "\"hue\":0,\"position\":1,\"frameMax\":" + frameMax + ",\"frames\":["
                        + frames + "],\"timings\":[{\"frame\":2,\"condition\":1,\"scope\":2,"
                        + "\"flashDuration\":5,\"color\":[255,255,255,255],"
                        + "\"se\":{\"name\":\"Exclaim\",\"volume\":80,\"pitch\":100}},"
                        + "{\"frame\":2,\"condition\":0,\"scope\":1,\"flashDuration\":5,"
                        + "\"color\":[255,0,0,255],\"se\":null}]}]}"));
        return data.animation(1);
    }

    @Test
    @DisplayName("one animation frame lasts 2 game frames and the timing fires once")
    void playback() {
        MapAnimations animations = new MapAnimations();
        List<String> calls = new ArrayList<>();
        MapAnimations.TimingSink sink = new MapAnimations.TimingSink() {
            @Override
            public void playSe(String name, int volume, int pitch) {
                calls.add("se:" + name + ":" + volume);
            }

            @Override
            public void flash(float red, float green, float blue, float alpha, int durationFrames) {
                calls.add("flash:" + durationFrames);
            }
        };
        animations.start(animation(4), -1);
        assertEquals(1, animations.active().size);

        animations.update(0.05f, sink); // 2 game frames -> animation frame 1
        assertEquals(1, animations.active().first().frameIndex());
        assertTrue(calls.isEmpty(), "the timing sits on animation frame 2");

        animations.update(0.05f, sink); // animation frame 2
        assertEquals(2, animations.active().first().frameIndex());
        assertEquals(List.of("se:Exclaim:80", "flash:10"), calls);
        assertEquals(1, animations.flashes().size, "scope 1 adds a target flash");

        animations.update(0.05f, sink); // frame 3
        assertEquals(List.of("se:Exclaim:80", "flash:10"), calls, "timings fire once");

        animations.update(0.1f, sink); // 4 frames * 0.05 s = 0.2 s total
        assertTrue(animations.active().isEmpty(), "the animation is over");

        animations.update(0.15f, sink); // the target flash started at 0.1 s
        assertTrue(animations.flashes().isEmpty(), "the target flash faded out");
    }

    @Test
    @DisplayName("a new animation on the same target replaces the previous one")
    void replacesTarget() {
        MapAnimations animations = new MapAnimations();
        animations.start(animation(4), 7);
        animations.start(animation(4), 7);
        assertEquals(1, animations.active().size);
        animations.start(animation(4), -1);
        assertEquals(2, animations.active().size);
        animations.clear();
        assertTrue(animations.isEmpty());
    }

    @Test
    @DisplayName("tile animations keep their own sprite and can coexist (R6.28)")
    void tileAnimations() {
        MapAnimations animations = new MapAnimations();
        animations.startTile(animation(4), 12, 23, 1);
        animations.startTile(animation(4), 12, 24, 1);
        animations.start(animation(4), -1);

        assertEquals(3, animations.active().size, "two tiles plus the player");
        MapAnimations.Active first = animations.active().first();
        assertEquals(12, first.tileX);
        assertEquals(23, first.tileY);
        assertEquals(1, first.height);
        MapAnimations.Active player = animations.active().peek();
        assertEquals(-1, player.targetId);
        assertEquals(-1, player.tileX, "character animations are not tile-anchored");
    }
}
