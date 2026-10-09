package pokemon.runtime.battle;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 227_PScreen_Trading pbScene1 / pbScene2 timelines (PictureEx, 40 fps ticks). */
class TradeAnimationTest {

    private static int run(TradeAnimation animation) {
        int ticks = 0;
        while (animation.tick() && ticks < 1000) {
            ticks++;
        }
        return ticks + 1;
    }

    @Test
    void sceneOneRecallsThePokemonIntoItsBall() {
        List<String> played = new ArrayList<>();
        TradeAnimation animation = new TradeAnimation(0, 0, "001Cry", (name, volume, pitch) -> played.add(name));
        animation.startScene1();
        int ticks = run(animation);
        assertEquals(List.of("Battle recall", "Battle jump to ball"), played);
        assertFalse(animation.pokemon1Visible(), "setVisible(delay+8,false) hides the recalled Pokemon");
        assertTrue(ticks >= 60 && ticks <= 66, "31 units of 1/20 s = about 62 ticks, was " + ticks);
    }

    @Test
    void sceneTwoDropsTheBallAndOpensIt() {
        List<String> played = new ArrayList<>();
        TradeAnimation animation = new TradeAnimation(0, 0, "001Cry", (name, volume, pitch) -> played.add(name));
        animation.startScene2();
        run(animation);
        assertEquals(List.of("Battle ball drop", "Battle ball drop", "Battle ball drop", "Battle ball drop",
                "Battle recall", "001Cry"), played);
        assertTrue(animation.pokemon2Visible());
    }
}
