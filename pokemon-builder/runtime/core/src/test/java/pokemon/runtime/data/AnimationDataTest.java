package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** R6.27: generated/animations.json (Show Animation, 207) parsing. */
class AnimationDataTest {

    @Test
    @DisplayName("animations keep their frames, cells and timings")
    void parses() {
        AnimationData data = AnimationData.parse(new JsonReader().parse(
                "{\"total\":1,\"animations\":[{\"id\":3,\"name\":\"Exclaim bubble\","
                        + "\"graphic\":\"029-Emotion01\",\"hue\":0,\"position\":2,"
                        + "\"frameMax\":2,\"frames\":["
                        + "[{\"cell\":4,\"x\":8,\"y\":5,\"zoom\":100,\"angle\":0,"
                        + "\"flip\":false,\"opacity\":255,\"blend\":0}],"
                        + "[{\"cell\":5,\"x\":8,\"y\":5,\"zoom\":100,\"angle\":0,"
                        + "\"flip\":true,\"opacity\":200,\"blend\":1}]],"
                        + "\"timings\":[{\"frame\":1,\"condition\":1,\"scope\":2,"
                        + "\"flashDuration\":5,\"color\":[255,128,0,200],"
                        + "\"se\":{\"name\":\"Exclaim\",\"volume\":80,\"pitch\":100}}]}]}"));

        assertEquals(1, data.size());
        AnimationData.Animation animation = data.animation(3);
        assertNotNull(animation);
        assertEquals("Exclaim bubble", animation.name);
        assertEquals("029-Emotion01", animation.graphic);
        assertEquals(2, animation.position);
        assertEquals(2, animation.frames.length);
        assertEquals(4, animation.frames[0][0].cell);
        assertTrue(animation.frames[1][0].flip);
        assertEquals(200, animation.frames[1][0].opacity);
        assertEquals(1, animation.frames[1][0].blend);

        AnimationData.Timing timing = animation.timings[0];
        assertEquals(1, timing.frame);
        assertEquals(2, timing.scope);
        assertEquals(5, timing.flashDuration);
        assertEquals(200f, timing.alpha, 0.01f);
        assertEquals("Exclaim", timing.seName);
        assertEquals(80, timing.seVolume);
    }

    @Test
    @DisplayName("ids outside the data answer null instead of throwing")
    void missingIds() {
        AnimationData data = AnimationData.parse(null);
        assertEquals(0, data.size());
        assertNull(data.animation(1));
    }
}
