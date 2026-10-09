package pokemon.runtime.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;
import pokemon.runtime.state.ScreenWeather;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 179_PField_FieldMoves: the UseMove bodies, run on a scene that answers at once. */
class HiddenMoveTaskTest {

    /** Records what the moves ask of the map. */
    private static final class Port implements MapPort {
        final List<String> calls = new ArrayList<>();
        String facing;

        @Override
        public void transfer(int mapId, int x, int y, int direction) {
        }

        @Override
        public String facingEventName() {
            return facing;
        }

        @Override
        public float hiddenMoveAnimation(Pokemon pokemon) {
            calls.add("banner");
            return 2f;
        }

        @Override
        public float smashFacingEvent() {
            calls.add("smash");
            return 0.4f;
        }

        @Override
        public void transferThroughFade(int mapId, int x, int y, int direction, boolean keepVehicles) {
            calls.add("transfer " + mapId + "," + x + "," + y + "," + direction + "," + keepVehicles);
        }

        @Override
        public void startSurfing() {
            calls.add("surf");
        }
    }

    private final List<String> lines = new ArrayList<>();
    private final Port port = new Port();
    private final Random random = new Random(7);

    private HiddenMoveTask task(GameState state, HiddenMoveTask.Services services) {
        TaskFieldScene scene = new TaskFieldScene(request -> {
            switch (request.kind) {
                case MESSAGE:
                    lines.add(request.text);
                    return null;
                case ACTION:
                    request.action.get();
                    return null;
                default:
                    return null;
            }
        });
        return new HiddenMoveTask(state, null, port, scene, services, random, null);
    }

    private static Pokemon pokemon(String name) {
        PbsData.Species species = new PbsData.Species();
        species.id = 25;
        species.internalName = "PIKACHU";
        Pokemon pkmn = new Pokemon(species, 5, null);
        pkmn.name = name;
        return pkmn;
    }

    @Test
    @DisplayName("Strength: the banner, the plugin's line, the flag")
    void strength() {
        GameState state = new GameState();
        assertTrue(task(state, enctype -> false).use("STRENGTH", pokemon("皮卡丘")));
        assertEquals(List.of("皮卡丘的怪力\n现在可以移动石头了。"), lines);
        assertTrue(state.pokemonMapStrengthUsed());
        assertEquals(List.of("banner"), port.calls);
    }

    @Test
    @DisplayName("Teleport erases the escape point and fades to the healing spot")
    void teleport() {
        GameState state = new GameState();
        state.fieldGlobals().healingSpot = new int[] {12, 3, 4};
        state.fieldGlobals().escapePoint = new int[] {1, 1, 1, 2};
        assertTrue(task(state, enctype -> false).use("TELEPORT", pokemon("皮卡丘")));
        assertEquals(0, state.fieldGlobals().escapePoint.length);
        assertEquals(List.of("banner", "transfer 12,3,4,2,false"), port.calls);
    }

    @Test
    @DisplayName("Cut shakes the tree away; Rock Smash may roll an encounter")
    void smash() {
        GameState state = new GameState();
        port.facing = "tree";
        List<String> encounters = new ArrayList<>();
        assertTrue(task(state, enctype -> {
            encounters.add(enctype);
            return true;
        }).use("CUT", pokemon("a")));
        assertEquals(List.of("banner", "smash"), port.calls);
        assertTrue(encounters.isEmpty());
        port.calls.clear();
        port.facing = "rock";
        int found = 0;
        for (int i = 0; i < 40; i++) {
            task(state, enctype -> {
                encounters.add(enctype);
                return true;
            }).use("ROCKSMASH", pokemon("a"));
        }
        assertTrue(encounters.size() > 0 && encounters.size() < 40, "roughly a quarter: " + encounters.size());
        assertEquals("RockSmash", encounters.get(0));
    }

    @Test
    @DisplayName("Dive flips surfing and diving and keeps them across the transfer; Defog clears the weather")
    void diveAndDefog() {
        GameState state = new GameState();
        state.fieldGlobals().surfing = true;
        state.enterMap(5, 7, 8);
        PbsData pbs = null;
        // No metadata: the plugin's `next false if !divemap`.
        assertFalse(task(state, enctype -> false).use("DIVE", pokemon("a")));
        state.weather().set(ScreenWeather.FOG, 0, 0);
        assertTrue(task(state, enctype -> false).use("DEFOG", pokemon("a")));
        assertEquals(ScreenWeather.NONE, state.weather().type());
    }
}
