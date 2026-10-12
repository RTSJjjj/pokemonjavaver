package pokemon.runtime.ui.menu;

import org.junit.jupiter.api.Test;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.field.PBDayNight;
import pokemon.runtime.map.TestData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.lang.reflect.Method;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** The party screen's 进化 command for a Gligar holding a Razor Fang follows the clock (the plugin has no automatic evolution). */
class PartyEvolutionCommandTest {

    @Test
    void gligarWithARazorFangCanEvolveExactlyAtNight() throws Exception {
        File root = TestData.runtimeDataRoot();
        assumeTrue(root != null, "runtime data not available");
        RuntimeContext context = new RuntimeContext("test", null, root.getAbsolutePath());
        assumeTrue(context.pbsData() != null && context.pbsData().species("GLIGAR") != null, "no GLIGAR in the data");
        Pokemon gligar = new Pokemon(context.pbsData().species("GLIGAR"), 30, context.pbsData());
        gligar.item = "RAZORFANG";
        context.gameState().trainer().party.add(gligar);
        PartyView view = new PartyView(context);
        Method target = PartyView.class.getDeclaredMethod("evolutionTarget", Pokemon.class);
        target.setAccessible(true);
        Object result = target.invoke(view, gligar);
        boolean night = PBDayNight.isNight(LocalTime.now());
        if (night) {
            assertEquals("GLISCOR", ((pokemon.runtime.pokemon.PbsData.Species) result).internalName);
        } else {
            assertNull(result);
        }
        gligar.item = null;
        assertNull(target.invoke(view, gligar), "no Razor Fang: never");
    }
}
