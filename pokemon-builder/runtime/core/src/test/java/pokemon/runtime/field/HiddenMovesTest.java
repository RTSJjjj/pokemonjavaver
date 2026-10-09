package pokemon.runtime.field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.state.GameState;
import pokemon.runtime.state.ScreenWeather;

import static org.junit.jupiter.api.Assertions.*;

/** 179_PField_FieldMoves: the CanUseMove / ConfirmUseMove handlers and the handler list. */
class HiddenMovesTest {

    private static final class World implements HiddenMoves.World {
        String facingEvent;
        int facingTag;
        int mapId = 5;
        boolean passable = true;
        boolean dependents;

        @Override
        public String facingEventName() {
            return facingEvent;
        }

        @Override
        public int facingTerrainTag() {
            return facingTag;
        }

        @Override
        public int playerTerrainTag() {
            return 0;
        }

        @Override
        public boolean facingPassable() {
            return passable;
        }

        @Override
        public boolean hasDependentEvents() {
            return dependents;
        }

        @Override
        public int mapId() {
            return mapId;
        }

        @Override
        public boolean outdoor() {
            return true;
        }

        @Override
        public int terrainTagOn(int map) {
            return 0;
        }

        @Override
        public String mapName(int id) {
            return "Town" + id;
        }
    }

    @Test
    @DisplayName("the handler list is the plugin's")
    void handlers() {
        assertTrue(HiddenMoves.hasHandler("SURF"));
        assertTrue(HiddenMoves.hasHandler("CHATTER"));
        assertFalse(HiddenMoves.hasHandler("TACKLE"));
        assertFalse(HiddenMoves.hasHandler(null));
    }

    @Test
    @DisplayName("Cut needs the badge and a tree in front; the lines are the plugin's")
    void cut() {
        GameState state = new GameState();
        World world = new World();
        HiddenMoves.Check check = HiddenMoves.canUse("CUT", null, state, null, world);
        assertFalse(check.ok);
        assertEquals("对不起，\n这需要拥有对应的徽章。", check.message);
        state.trainer().badges.add(0);
        state.trainer().badges.add(1);
        check = HiddenMoves.canUse("CUT", null, state, null, world);
        assertEquals("不能在这里使用。", check.message);
        world.facingEvent = "tree";
        assertTrue(HiddenMoves.canUse("CUT", null, state, null, world).ok);
    }

    @Test
    @DisplayName("Strength says so when it is already in use; Defog needs fog; Waterfall needs the falls in front")
    void others() {
        GameState state = new GameState();
        for (int i = 0; i < 8; i++) state.trainer().badges.add(i);
        World world = new World();
        assertTrue(HiddenMoves.canUse("STRENGTH", null, state, null, world).ok);
        state.pokemonMapStrengthUsed(true);
        assertEquals("已经使用怪力了。", HiddenMoves.canUse("STRENGTH", null, state, null, world).message);
        assertFalse(HiddenMoves.canUse("DEFOG", null, state, null, world).ok);
        state.weather().set(ScreenWeather.FOG, 0, 0);
        assertTrue(HiddenMoves.canUse("DEFOG", null, state, null, world).ok);
        assertEquals("不能在这里使用。", HiddenMoves.canUse("WATERFALL", null, state, null, world).message);
        world.facingTag = PBTerrain.WATERFALL;
        assertTrue(HiddenMoves.canUse("WATERFALL", null, state, null, world).ok);
    }

    @Test
    @DisplayName("Teleport asks for the healing spot's map; Dig needs an escape point")
    void teleportAndDig() {
        GameState state = new GameState();
        World world = new World();
        assertEquals("不能在这里使用。", HiddenMoves.canUse("TELEPORT", null, state, null, world).message);
        state.fieldGlobals().healingSpot = new int[] {12, 3, 4};
        assertTrue(HiddenMoves.canUse("TELEPORT", null, state, null, world).ok);
        assertEquals("想要回到Town12吗？", HiddenMoves.confirmQuestion("TELEPORT", state, null, world));
        world.mapId = 60;
        HiddenMoves.Check banned = HiddenMoves.canUse("TELEPORT", null, state, null, world);
        assertFalse(banned.ok);
        assertTrue(banned.always);
        assertEquals("这里不能使用。", banned.message);
        world.mapId = 5;
        assertFalse(HiddenMoves.canUse("DIG", null, state, null, world).ok);
        state.fieldGlobals().escapePoint = new int[] {7, 1, 1, 2};
        assertTrue(HiddenMoves.canUse("DIG", null, state, null, world).ok);
        assertEquals("想从这里出去回到Town7吗？", HiddenMoves.confirmQuestion("DIG", state, null, world));
        assertNull(HiddenMoves.confirmQuestion("CUT", state, null, world));
    }
}
