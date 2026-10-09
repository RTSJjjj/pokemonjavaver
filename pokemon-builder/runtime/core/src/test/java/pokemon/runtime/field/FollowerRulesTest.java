package pokemon.runtime.field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 296_Follower_Config's refresh handlers, change_sprite's file names and the talk handlers. */
class FollowerRulesTest {

    private static Pokemon pokemon(int id, String internalName, String... types) {
        PbsData.Species species = new PbsData.Species();
        species.id = id;
        species.internalName = internalName;
        for (String type : types) {
            species.types.add(type);
        }
        Pokemon pkmn = new Pokemon(species, 5, null);
        pkmn.name = internalName;
        pkmn.ability = "";
        return pkmn;
    }

    @Test
    @DisplayName("the bicycle hides the follower; a Water type stays away from the surf; a Flying type shows")
    void refresh() {
        FollowerRules.Context context = new FollowerRules.Context();
        context.outdoor = true;
        Pokemon rattata = pokemon(19, "RATTATA", "NORMAL");
        assertNull(FollowerRules.refresh(rattata, context), "nothing decides: -1, which still shows");
        context.bicycle = true;
        assertEquals(Boolean.FALSE, FollowerRules.refresh(rattata, context));
        context.bicycle = false;
        context.surfing = true;
        assertEquals(Boolean.FALSE, FollowerRules.refresh(rattata, context));
        assertEquals(Boolean.FALSE, FollowerRules.refresh(pokemon(7, "SQUIRTLE", "WATER"), context));
        context.surfing = false;
        assertEquals(Boolean.TRUE, FollowerRules.refresh(pokemon(16, "PIDGEY", "NORMAL", "FLYING"), context));
        context.diving = true;
        assertEquals(Boolean.TRUE, FollowerRules.refresh(pokemon(7, "SQUIRTLE", "WATER"), context));
        assertEquals(Boolean.FALSE, FollowerRules.refresh(rattata, context));
    }

    @Test
    @DisplayName("the graphic names: the number first, then the form / shiny / gender factors drop one by one")
    void spriteNames() {
        Pokemon pkmn = pokemon(3, "VENUSAUR", "GRASS", "POISON");
        pkmn.shiny = true;
        pkmn.gender = PokemonStats.FEMALE;
        List<String> candidates = FollowerRules.spriteCandidates(pkmn);
        assertEquals("Following/003fs", candidates.get(0));
        assertTrue(candidates.contains("Following/003s"));
        assertTrue(candidates.contains("Following/003f"));
        assertTrue(candidates.contains("Following/003"));
        assertEquals("000", candidates.get(candidates.size() - 1));
    }

    private static FollowerTalk.Env env(String map, int weather, boolean hold) {
        return new FollowerTalk.Env() {
            @Override
            public String mapName() {
                return map;
            }

            @Override
            public String trainerName() {
                return "Red";
            }

            @Override
            public int weather() {
                return weather;
            }

            @Override
            public boolean holdItem() {
                return hold;
            }

            @Override
            public String itemNameById(int id) {
                return "POTION";
            }
        };
    }

    @Test
    @DisplayName("a status answers first; the weather answers before the random groups")
    void talk() {
        Pokemon pkmn = pokemon(37, "VULPIX", "FIRE");
        pkmn.name = "Vulpix";
        pkmn.status = "POISON";
        FollowerTalk.Result result = FollowerTalk.choose(pkmn, env("a", 0, false), 0, new Random(1));
        assertEquals(FollowerRules.EMO_POISON, result.animation);
        assertEquals("Vulpix因为中毒而颤抖...", result.message);
        pkmn.status = "";
        result = FollowerTalk.choose(pkmn, env("a", pokemon.runtime.state.ScreenWeather.RAIN, false), 0, new Random(1));
        assertEquals(FollowerRules.EMO_HATE, result.animation, "a Fire type dislikes the rain");
        result = FollowerTalk.choose(pkmn, env("a", 0, false), 5, new Random(1));
        assertEquals(0, result.animation, "group 5 has no emote");
        assertNotNull(result.message);
        assertTrue(result.message.contains("Vulpix") || result.message.contains("{2}") || !result.message.isEmpty());
    }

    @Test
    @DisplayName("a follower that holds something hands it over once the hold flag is set")
    void foundItem() {
        Pokemon pkmn = pokemon(37, "VULPIX", "FIRE");
        pkmn.name = "Vulpix";
        FollowerTalk.Result result = FollowerTalk.choose(pkmn, env("a", 0, true), 0, new Random(1));
        assertTrue(result.foundItem);
        assertEquals("POTION", result.foundItemName);
        assertEquals("Vulpix似乎正在持有某物...", result.foundMessage);
    }
}
