package pokemon.runtime.field;

import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.map.DayNightTone;
import pokemon.runtime.pokemon.PBEvolution;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.state.GameState;

import java.time.LocalTime;
import java.util.Random;
import java.util.function.Supplier;

/**
 * The world as the evolution methods read it (201_Pokemon_Evolution): the clock, {@code $game_screen}'s weather,
 * {@code $PokemonGlobal}, the map metadata, the party and the bag.
 */
public final class EvolutionWorld implements PBEvolution.Env {
    private static final Random RANDOM = new Random();

    private final GameState state;
    private final PbsData pbs;
    private final GameDatabase database;
    private final Supplier<LocalTime> clock;

    public EvolutionWorld(RuntimeContext context) {
        this(context.gameState(), context.pbsData(), context.database(), null);
    }

    /** {@code clock} null = the real clock through {@link DayNightTone}; {@code database} null = no region data. */
    public EvolutionWorld(GameState state, PbsData pbs, GameDatabase database, Supplier<LocalTime> clock) {
        this.state = state;
        this.pbs = pbs;
        this.database = database;
        this.clock = clock;
    }

    private GameState state() {
        return state;
    }

    @Override public boolean isDay() { return clock == null ? DayNightTone.test("isDay?") : PBDayNight.isDay(clock.get()); }
    @Override public boolean isNight() { return clock == null ? DayNightTone.test("isNight?") : PBDayNight.isNight(clock.get()); }
    @Override public boolean isMorning() { return clock == null ? DayNightTone.test("isMorning?") : PBDayNight.isMorning(clock.get()); }
    @Override public boolean isAfternoon() { return clock == null ? DayNightTone.test("isAfternoon?") : PBDayNight.isAfternoon(clock.get()); }
    @Override public boolean isEvening() { return clock == null ? DayNightTone.test("isEvening?") : PBDayNight.isEvening(clock.get()); }
    @Override public int weatherType() { return state().weather().type(); }
    @Override public boolean bicycle() { return state().fieldGlobals().bicycle; }
    @Override public boolean surfing() { return state().fieldGlobals().surfing; }
    @Override public boolean diving() { return state().fieldGlobals().diving; }

    @Override
    public boolean darkMap() {
        PbsData.Metadata meta = pbs == null ? null : pbs.mapMetadata(state().currentMapId());
        return meta != null && meta.darkMap;
    }

    @Override public int mapId() { return state().currentMapId(); }

    @Override
    public int mapRegion() {
        MapData map = database == null ? null : database.map(state().currentMapId());
        return map == null || map.mapPosition == null || map.mapPosition.length == 0 ? -1 : map.mapPosition[0];
    }

    @Override
    public boolean partyHasType(String type) {
        for (Pokemon p : state().trainer().party.members()) {
            if (p != null && p.types().contains(type, false)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasSpecies(String species) {
        for (Pokemon p : state().trainer().party.members()) {
            if (p != null && !p.egg && p.species != null && species.equals(p.species.internalName)) {
                return true;
            }
        }
        return false;
    }

    @Override public int partySize() { return state().trainer().party.size(); }
    @Override public int bagQuantity(String item) { return state().inventory().count(item); }
    @Override public boolean bagHas(String item) { return state().inventory().has(item); }
    @Override public void bagDelete(String item, int amount) { state().inventory().remove(item, amount); }

    @Override
    public void addDuplicate(Pokemon duplicate) {
        TrainerState trainer = state().trainer();
        trainer.party.add(duplicate);
        trainer.registerOwned(duplicate);
    }

    @Override public int rand(int n) { return RANDOM.nextInt(n); }
    @Override public PbsData data() { return pbs; }
}
