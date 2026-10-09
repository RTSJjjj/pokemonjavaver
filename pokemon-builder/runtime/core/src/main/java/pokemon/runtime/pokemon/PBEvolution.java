package pokemon.runtime.pokemon;

import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;

/**
 * 201_Pokemon_Evolution + 366_PBEvolution: the evolution methods and the helper functions around them.
 *
 * <p>The plugin registers a hash of procs per method ({@code levelUpCheck}, {@code itemCheck}, {@code tradeCheck},
 * {@code afterBattleCheck}, {@code afterEvolution}); here each is a method of this class that switches on the method name
 * as the PBS writes it. The state a proc reads from the world ({@code PBDayNight}, {@code $game_screen},
 * {@code $PokemonGlobal}, the bag, the party ...) comes through {@link Env}.</p>
 *
 * <p>The checks mutate the Pokemon exactly where the plugin does ({@code pkmn.form = ...} inside {@code Level} for Goomy,
 * {@code Item} for Petilil ... and the special-form items), so a check is not free of side effects.</p>
 */
public final class PBEvolution {

    private PBEvolution() {
    }

    /** What the procs read from the world. */
    public interface Env {
        boolean isDay();                        // PBDayNight.isDay?
        boolean isNight();
        boolean isMorning();
        boolean isAfternoon();
        boolean isEvening();
        /** {@code $game_screen.weather_type} ({@link pokemon.runtime.state.ScreenWeather}). */
        int weatherType();
        boolean bicycle();                      // $PokemonGlobal.bicycle
        boolean surfing();
        boolean diving();
        /** {@code pbGetMetadata($game_map.map_id, MetadataDarkMap)}. */
        boolean darkMap();
        /** {@code $game_map.map_id}. */
        int mapId();
        /** {@code pbGetMetadata(map, MetadataMapPosition)}: the region number is the first entry, or -1 when unset. */
        int mapRegion();
        /** {@code $Trainer.pokemonParty.any? { |p| p && p.hasType?(type) }}. */
        boolean partyHasType(String type);
        /** {@code pbHasSpecies?(species)}. */
        boolean hasSpecies(String species);
        /** {@code $Trainer.party.length}. */
        int partySize();
        int bagQuantity(String item);
        boolean bagHas(String item);
        void bagDelete(String item, int amount);
        /** {@code $Trainer.party.push}, {@code $Trainer.seen/owned} (pbDuplicatePokemon). */
        void addDuplicate(Pokemon duplicate);
        /** {@code rand(n)}. */
        int rand(int n);
        PbsData data();
    }

    /** One entry of {@code pbGetEvolvedFormData}: {@code [method, parameter, species]}. */
    public static final class Evo {
        public final String method;
        public final String parameter;
        public final String species;

        Evo(String method, String parameter, String species) {
            this.method = method;
            this.parameter = parameter;
            this.species = species;
        }
    }

    // ------------------------------------------------------------------
    // Method table (201:2-72 constants and the registered hashes)
    // ------------------------------------------------------------------

    private static boolean in(String method, String... names) {
        for (String name : names) {
            if (name.equals(method)) {
                return true;
            }
        }
        return false;
    }

    /** {@code PBEvolution.hasFunction?(method, "levelUpCheck")}. */
    public static boolean hasLevelUpCheck(String method) {
        return in(method, "Level", "LevelMale", "LevelFemale", "LevelDay", "LevelNight", "LevelMorning", "LevelAfternoon",
                "LevelEvening", "LevelNoWeather", "LevelSun", "LevelRain", "LevelSnow", "LevelSandstorm", "LevelCycling",
                "LevelSurfing", "LevelDiving", "LevelDarkness", "LevelDarkInParty", "AttackGreater", "AtkDefEqual",
                "DefenseGreater", "Silcoon", "Cascoon", "Ninjask", "Happiness", "HappinessMale", "HappinessFemale",
                "HappinessDay", "HappinessNight", "HappinessMove", "HappinessMoveType", "HappinessHoldItem",
                "MaxHappiness", "Beauty", "HoldItem", "HoldItemMale", "HoldItemFemale", "DayHoldItem", "NightHoldItem",
                "HoldItemHappiness", "HasMove", "HasMoveType", "HasInParty", "Location", "Region", "SweetItem",
                "CollectItems", "ItemHappiness");     // ItemHappiness registers "levelUpCheck" (201:782-787), not "itemCheck"
    }

    /** {@code PBEvolution.hasFunction?(method, "itemCheck")}. */
    public static boolean hasItemCheck(String method) {
        return in(method, "Item", "SpecialItem", "PhantomItem", "MiloticmItem", "TinkatonItem", "ItemMale", "ItemFemale",
                "ItemDay", "ItemNight");
    }

    /** {@code PBEvolution.hasFunction?(method, "tradeCheck")}. */
    public static boolean hasTradeCheck(String method) {
        return in(method, "Trade", "TradeMale", "TradeFemale", "TradeDay", "TradeNight", "TradeItem", "TradeSpecies");
    }

    /** The {@code "minimumLevel" => 1} entries (201): "needs any level up". */
    private static boolean minimumLevelOne(String method) {
        return in(method, "Happiness", "HappinessMale", "HappinessFemale", "HappinessDay", "HappinessNight",
                "HappinessMove", "HappinessMoveType", "HappinessHoldItem", "MaxHappiness", "Beauty", "HoldItem",
                "HoldItemMale", "HoldItemFemale", "DayHoldItem", "NightHoldItem", "HoldItemHappiness", "HasMove",
                "HasMoveType", "HasInParty", "Location", "Region", "SweetItem", "CollectItems");
    }

    private static boolean hasAfterEvolution(String method) {
        return in(method, "Shedinja", "HappinessHoldItem", "HoldItem", "HoldItemMale", "HoldItemFemale", "DayHoldItem",
                "NightHoldItem", "HoldItemHappiness", "TradeItem", "CollectItems");
    }

    private static int number(String parameter) {
        try {
            return parameter == null ? 0 : Integer.parseInt(parameter.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean male(Pokemon p) {
        return p.gender == PokemonStats.MALE;
    }

    private static boolean female(Pokemon p) {
        return p.gender == PokemonStats.FEMALE;
    }

    private static boolean isSpecies(Pokemon p, String name) {
        return p.species != null && name.equals(p.species.internalName);
    }

    private static boolean holds(Pokemon p, String item) {
        return item != null && p.item != null && item.equals(p.item);
    }

    private static boolean knowsMove(Pokemon p, String move) {
        for (Pokemon.MoveSlot slot : p.moves) {
            if (slot != null && slot.move != null && slot.move.internalName.equals(move)) {
                return true;
            }
        }
        return false;
    }

    private static boolean knowsMoveOfType(Pokemon p, String type) {
        for (Pokemon.MoveSlot slot : p.moves) {
            if (slot != null && slot.move != null && type.equals(slot.move.type)) {
                return true;
            }
        }
        return false;
    }

    private static void setForm(Env env, Pokemon p, int index) {
        p.recalculatingStats(() -> p.setForm(env.data(), index));
    }

    // ------------------------------------------------------------------
    // levelUpCheck (201:295-671, 864-908)
    // ------------------------------------------------------------------

    /** {@code PBEvolution.call("levelUpCheck", method, pkmn, parameter)}: false when the method has none. */
    public static boolean levelUpCheck(String method, Pokemon pkmn, String parameter, Env env) {
        int level = number(parameter);
        switch (method) {
            case "Level":                                                                         // :295-304
                if (isSpecies(pkmn, "GOOMY") || isSpecies(pkmn, "BERGMITE") || isSpecies(pkmn, "RUFFLET")) {
                    setForm(env, pkmn, 0);                                                        // :300
                }
                return pkmn.level >= level;                                                       // :302
            case "LevelMale": return pkmn.level >= level && male(pkmn);                           // :306-310
            case "LevelFemale": return pkmn.level >= level && female(pkmn);
            case "LevelDay": return pkmn.level >= level && env.isDay();
            case "LevelNight": return pkmn.level >= level && env.isNight();
            case "LevelMorning": return pkmn.level >= level && env.isMorning();
            case "LevelAfternoon": return pkmn.level >= level && env.isAfternoon();
            case "LevelEvening": return pkmn.level >= level && env.isEvening();
            case "LevelNoWeather": return pkmn.level >= level && env.weatherType() == pokemon.runtime.state.ScreenWeather.NONE;   // :348-354
            case "LevelSun": return pkmn.level >= level && env.weatherType() == pokemon.runtime.state.ScreenWeather.SUN;
            case "LevelRain": {                                                                   // :364-371 Rain, HeavyRain, Storm, Fog
                int w = env.weatherType();
                return pkmn.level >= level && (w == pokemon.runtime.state.ScreenWeather.RAIN
                        || w == pokemon.runtime.state.ScreenWeather.HEAVY_RAIN
                        || w == pokemon.runtime.state.ScreenWeather.STORM || w == pokemon.runtime.state.ScreenWeather.FOG);
            }
            case "LevelSnow": {                                                                   // :373-379
                int w = env.weatherType();
                return pkmn.level >= level && (w == pokemon.runtime.state.ScreenWeather.SNOW
                        || w == pokemon.runtime.state.ScreenWeather.BLIZZARD);
            }
            case "LevelSandstorm": return pkmn.level >= level && env.weatherType() == pokemon.runtime.state.ScreenWeather.SANDSTORM;
            case "LevelCycling": return pkmn.level >= level && env.bicycle();                     // :389-393
            case "LevelSurfing": return pkmn.level >= level && env.surfing();
            case "LevelDiving": return pkmn.level >= level && env.diving();
            case "LevelDarkness": return pkmn.level >= level && env.darkMap();                    // :407-411
            case "LevelDarkInParty": return pkmn.level >= level && env.partyHasType("DARK");      // :413-419
            case "AttackGreater": return pkmn.level >= level && pkmn.stat(PokemonStats.ATTACK) > pkmn.stat(PokemonStats.DEFENSE);   // :421-425
            case "AtkDefEqual": return pkmn.level >= level && pkmn.stat(PokemonStats.ATTACK) == pkmn.stat(PokemonStats.DEFENSE);
            case "DefenseGreater": return pkmn.level >= level && pkmn.stat(PokemonStats.ATTACK) < pkmn.stat(PokemonStats.DEFENSE);
            case "Silcoon": return pkmn.level >= level && (((pkmn.personalID >> 16) & 0xFFFF) % 10) < 5;      // :439-443
            case "Cascoon": return pkmn.level >= level && (((pkmn.personalID >> 16) & 0xFFFF) % 10) >= 5;
            case "Ninjask": return pkmn.level >= level;                                           // :451-455
            case "Happiness": return pkmn.happiness >= 220;                                       // :468-474
            case "HappinessMale": return pkmn.happiness >= 220 && male(pkmn);
            case "HappinessFemale": return pkmn.happiness >= 220 && female(pkmn);
            case "HappinessDay": return pkmn.happiness >= 220 && env.isDay();
            case "HappinessNight": return pkmn.happiness >= 220 && env.isNight();
            case "HappinessMove": return pkmn.happiness >= 220 && knowsMove(pkmn, parameter);     // :508-516
            case "HappinessMoveType": return pkmn.happiness >= 220 && knowsMoveOfType(pkmn, parameter);   // :518-526
            case "HappinessHoldItem": return holds(pkmn, parameter) && pkmn.happiness >= 220;    // :528-533
            case "MaxHappiness": return pkmn.happiness == 255;                                    // :541-547
            case "Beauty": return pkmn.beauty >= level;                                           // :549-554
            case "HoldItem": return holds(pkmn, parameter);                                       // :556-561
            case "HoldItemMale": return holds(pkmn, parameter) && male(pkmn);
            case "HoldItemFemale": return holds(pkmn, parameter) && female(pkmn);
            case "DayHoldItem": return holds(pkmn, parameter) && env.isDay();
            case "NightHoldItem": return holds(pkmn, parameter) && env.isNight();
            case "HoldItemHappiness": return holds(pkmn, parameter) && pkmn.happiness >= 220;
            case "HasMove": return knowsMove(pkmn, parameter);                                    // :634-640
            case "HasMoveType": return knowsMoveOfType(pkmn, parameter);
            case "HasInParty": return env.hasSpecies(parameter);                                  // :650-656
            case "Location": return env.mapId() == number(parameter);                            // :658-663
            case "Region": {                                                                      // :665-671
                int region = env.mapRegion();
                return region >= 0 && region == number(parameter);
            }
            case "SweetItem": {                                                                   // :864-896
                int sweet = -1;
                if (holds(pkmn, "STRAWBERRYSWEET")) sweet = 0;
                else if (holds(pkmn, "BERRYSWEET")) sweet = 1;
                else if (holds(pkmn, "LOVESWEET")) sweet = 2;
                else if (holds(pkmn, "STARSWEET")) sweet = 3;
                else if (holds(pkmn, "CLOVERSWEET")) sweet = 4;
                else if (holds(pkmn, "FLOWERSWEET")) sweet = 5;
                else if (holds(pkmn, "RIBBONSWEET")) sweet = 6;
                if (sweet != -1) {
                    int cream = env.rand(9);                                                       // :889 rand(9)
                    setForm(env, pkmn, cream * 7 + sweet);                                         // :890
                    return true;
                }
                return false;
            }
            case "CollectItems": return env.bagQuantity(parameter) >= 999;                        // :898-903
            default: return false;
        }
    }

    // ------------------------------------------------------------------
    // itemCheck (201:676-787)
    // ------------------------------------------------------------------

    /** {@code PBEvolution.call("itemCheck", method, pkmn, parameter, item)}. */
    public static boolean itemCheck(String method, Pokemon pkmn, String parameter, String item, Env env) {
        switch (method) {
            case "Item": {                                                                        // :676-697
                if ("HISUISTONE".equals(item)) {                                                  // :679
                    if (isSpecies(pkmn, "PETILIL") || isSpecies(pkmn, "GOOMY") || isSpecies(pkmn, "BERGMITE")) {
                        setForm(env, pkmn, 1);                                                    // :682
                    }
                } else if ("SUNSTONE".equals(item)) {                                             // :684
                    if (isSpecies(pkmn, "PETILIL")) setForm(env, pkmn, 0);                        // :685
                }
                if (isSpecies(pkmn, "PETILIL") || isSpecies(pkmn, "GOOMY") || isSpecies(pkmn, "BERGMITE")
                        || isSpecies(pkmn, "RUFFLET")) {                                          // :687-694
                    setForm(env, pkmn, "HISUISTONE".equals(item) ? 1 : 0);
                }
                return item.equals(parameter);                                                    // :695
            }
            case "SpecialItem":                                                                   // :700-709
                if (isSpecies(pkmn, "MAGIKARP") && "THUNDERSTONE".equals(item)) {
                    setForm(env, pkmn, 2);
                    return true;
                }
                return false;
            case "PhantomItem":                                                                   // :712-721
                if (isSpecies(pkmn, "FRAXURE") && "DUSKSTONE".equals(item)) {
                    setForm(env, pkmn, 1);
                    return true;
                }
                return false;
            case "MiloticmItem":                                                                  // :724-733
                if (isSpecies(pkmn, "FEEBAS") && "DRAGONSCALE".equals(item)) {
                    setForm(env, pkmn, 2);
                    return true;
                }
                return false;
            case "TinkatonItem":                                                                  // :736-745
                if (isSpecies(pkmn, "TINKATUFF") && "RAZORCLAW".equals(item)) {
                    setForm(env, pkmn, 1);
                    return true;
                }
                return false;
            case "ItemMale": return item.equals(parameter) && male(pkmn);                         // :754-759
            case "ItemFemale": return item.equals(parameter) && female(pkmn);
            case "ItemDay": return item.equals(parameter) && env.isDay();
            case "ItemNight": return item.equals(parameter) && env.isNight();
            default: return false;
        }
    }

    // ------------------------------------------------------------------
    // tradeCheck / afterBattleCheck (201:792-862)
    // ------------------------------------------------------------------

    /** {@code PBEvolution.call("tradeCheck", method, pkmn, parameter, other_pkmn)}. */
    public static boolean tradeCheck(String method, Pokemon pkmn, String parameter, Pokemon other, Env env) {
        switch (method) {
            case "Trade": return true;                                                            // :792-797
            case "TradeMale": return male(pkmn);                                                  // :799-804
            case "TradeFemale": return female(pkmn);
            case "TradeDay": return env.isDay();
            case "TradeNight": return env.isNight();
            case "TradeItem": return holds(pkmn, parameter);                                      // :827-831
            case "TradeSpecies":                                                                  // :839-844
                return pkmn.species != null && other != null && other.species != null
                        && pkmn.species.internalName.equals(parameter) && !"EVERSTONE".equals(other.item);
            default: return false;
        }
    }

    /** {@code PBEvolution.call("afterBattleCheck", method, pkmn, parameter)} (CriticalHits, DamageDone). */
    public static boolean afterBattleCheck(String method, Pokemon pkmn, String parameter) {
        switch (method) {
            case "CriticalHits": return pkmn.criticalHits >= number(parameter);                   // :849-853
            case "DamageDone": return pkmn.yamaskhp >= number(parameter);                         // :858-862
            default: return false;
        }
    }

    // ------------------------------------------------------------------
    // afterEvolution (201:459-466, 534-539, 562-632, 832-837, 904-908)
    // ------------------------------------------------------------------

    /**
     * {@code PBEvolution.call("afterEvolution", method, pkmn, new_species, parameter, evo_species)}: whether the method
     * consumed something (the held item, a Poke Ball for Shedinja, 999 items).
     */
    public static boolean afterEvolution(String method, Pokemon pkmn, String newSpecies, String parameter,
                                         String evoSpecies, Env env) {
        if (!hasAfterEvolution(method)) {
            return false;
        }
        if ("Shedinja".equals(method)) {                                                          // :459-465
            if (env.partySize() >= 6) return false;
            if (!env.bagHas("POKEBALL")) return false;
            duplicatePokemon(pkmn, env.data().species(newSpecies), env);                          // :462
            env.bagDelete("POKEBALL", 1);                                                         // :463
            return true;
        }
        if ("CollectItems".equals(method)) {                                                      // :904-908
            if (!evoSpecies.equals(newSpecies) || env.bagQuantity(parameter) < 999) return false;
            env.bagDelete(parameter, 999);
            return true;
        }
        // HappinessHoldItem, HoldItem*, DayHoldItem, NightHoldItem, HoldItemHappiness, TradeItem: the item is consumed
        if (!evoSpecies.equals(newSpecies) || !holds(pkmn, parameter)) return false;
        pkmn.item = null;                                                                         // pkmn.setItem(0)
        return true;
    }

    /** {@code PokemonEvolutionScene.pbDuplicatePokemon} (226:632-648). */
    public static void duplicatePokemon(Pokemon pkmn, PbsData.Species newSpecies, Env env) {
        Pokemon copy = pkmn.copy();
        copy.changeSpecies(env.data(), newSpecies);                                               // :634-635 species + name
        copy.name = newSpecies.name;
        copy.markings = 0;                                                                        // :636
        copy.ballused = 0;                                                                        // :637
        copy.item = null;                                                                         // :638
        copy.ribbons.clear();                                                                     // :639
        copy.hp = copy.maxHp();                                                                   // :640-641 calcStats, heal
        copy.status = "";
        copy.statusCount = 0;
        for (Pokemon.MoveSlot slot : copy.moves) {
            slot.pp = slot.maxPp;
        }
        env.addDuplicate(copy);                                                                   // :643-647
    }

    // ------------------------------------------------------------------
    // Evolution data helpers (201:138-248)
    // ------------------------------------------------------------------

    private static Array<PbsData.Evolution> evolutionsOf(Pokemon pkmn) {
        if (pkmn.form != null && pkmn.form.evolutions != null) {
            return pkmn.form.evolutions;
        }
        return pkmn.species.evolutions;
    }

    private static boolean isNone(String method) {
        return method == null || method.isEmpty() || "None".equals(method);
    }

    /** {@code pbGetEvolvedFormData(species, ignoreNone)} (:138-148). */
    public static List<Evo> evolvedFormData(PbsData.Species species, boolean ignoreNone) {
        List<Evo> ret = new ArrayList<>();
        if (species == null) {
            return ret;
        }
        for (PbsData.Evolution evo : species.evolutions) {
            if (isNone(evo.method) && ignoreNone) {
                continue;                                                                         // :144
            }
            ret.add(new Evo(evo.method, evo.parameter, evo.species));
        }
        return ret;
    }

    /** {@code pbGetPreviousForm(species)} (:150-157): the species that evolves into it, else itself. */
    public static PbsData.Species previousForm(PbsData data, PbsData.Species species) {
        PbsData.Species previous = previousOf(data, species);
        return previous == null ? species : previous;
    }

    private static PbsData.Species previousOf(PbsData data, PbsData.Species species) {
        if (species == null) {
            return null;
        }
        for (String name : data.speciesById.values()) {
            PbsData.Species candidate = data.species(name);
            if (candidate == null || candidate == species) {
                continue;
            }
            for (PbsData.Evolution evo : candidate.evolutions) {
                if (species.internalName.equals(evo.species)) {
                    return candidate;                                                             // evo[3] "is the prevolution"
                }
            }
        }
        return null;
    }

    /**
     * {@code pbGetBabySpecies(species)} (:159-175): the first species of the family. 登记: the incense variant
     * ({@code item1/item2}, {@code SpeciesIncense}) is not modelled; the plain form is.
     */
    public static PbsData.Species babySpecies(PbsData data, PbsData.Species species) {
        PbsData.Species ret = species;
        PbsData.Species previous = previousOf(data, species);
        if (previous != null) {
            ret = previous;                                                                       // :169
        }
        return ret != species ? babySpecies(data, ret) : ret;                                     // :173
    }

    /**
     * {@code pbGetBabySpecies(species, item1, item2)} (:159-175) with the incense check (:165-167): the prevolution is taken
     * when its species needs no incense or a parent holds it.
     */
    public static PbsData.Species babySpecies(PbsData data, PbsData.Species species, String item1, String item2) {
        PbsData.Species ret = species;
        PbsData.Species previous = previousOf(data, species);
        if (previous != null) {
            String incense = previous.incense;                                                    // :166 SpeciesIncense
            boolean none = incense == null || incense.isEmpty();
            if (none || incense.equalsIgnoreCase(item1) || incense.equalsIgnoreCase(item2)) {
                ret = previous;                                                                   // :167
            }
        }
        return ret != species ? babySpecies(data, ret, item1, item2) : ret;                       // :173
    }

    /**
     * {@code pbGetMinimumLevel(species)} (:177-190): the level at which the species can first appear, derived from the
     * prevolution's method.
     */
    public static int minimumLevel(PbsData data, PbsData.Species species) {
        PbsData.Species previous = previousOf(data, species);
        if (previous == null) {
            return 1;                                                                             // :179
        }
        int ret = -1;
        for (PbsData.Evolution evo : previous.evolutions) {
            if (!species.internalName.equals(evo.species)) {
                continue;
            }
            if (hasLevelUpCheck(evo.method)) {                                                    // :183
                ret = minimumLevelOne(evo.method) ? ret : number(evo.parameter);                  // :185 !min_level || min_level != 1
            }
            break;                                                                                // :187
        }
        return ret == -1 ? 1 : ret;
    }

    /** One row of {@code pbGetEvolutionFamilyData}: {@code [species, method, parameter, evolved species]}. */
    public static final class FamilyEntry {
        public final String species;
        public final String method;
        public final String parameter;
        public final String evolved;

        FamilyEntry(String species, String method, String parameter, String evolved) {
            this.species = species;
            this.method = method;
            this.parameter = parameter;
            this.evolved = evolved;
        }
    }

    /** {@code pbGetEvolutionFamilyData(species)} (:192-202). */
    public static List<FamilyEntry> familyData(PbsData data, PbsData.Species species) {
        List<FamilyEntry> ret = new ArrayList<>();
        for (Evo evo : evolvedFormData(species, true)) {
            ret.add(new FamilyEntry(species.internalName, evo.method, evo.parameter, evo.species));
            List<FamilyEntry> below = familyData(data, data.species(evo.species));
            ret.addAll(below);
        }
        return ret;
    }

    /** {@code pbCheckEvolutionFamilyForMethod(species, method, param)} (:206-220); a null method means "any" (-1). */
    public static boolean checkFamilyForMethod(PbsData data, PbsData.Species species, String[] methods, String param) {
        List<FamilyEntry> evos = familyData(data, babySpecies(data, species));
        for (FamilyEntry evo : evos) {
            if (methods != null && !in(evo.method, methods)) {
                continue;                                                                         // :212/214
            }
            if (param != null && !param.equals(evo.parameter)) {
                continue;                                                                         // :216
            }
            return true;
        }
        return false;
    }

    /** {@code pbCheckEvolutionFamilyForItemMethodItem(species, param)} (:224-234). */
    public static boolean checkFamilyForItemMethodItem(PbsData data, PbsData.Species species, String param) {
        List<FamilyEntry> evos = familyData(data, babySpecies(data, species));
        for (FamilyEntry evo : evos) {
            if (!hasItemCheck(evo.method)) {
                continue;                                                                         // :229
            }
            if (param != null && !param.equals(evo.parameter)) {
                continue;                                                                         // :230
            }
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Evolution checks (201:253-290)
    // ------------------------------------------------------------------

    /** The block {@code pbCheckEvolutionEx} yields to: the species it answers for, or null for -1. */
    public interface Check {
        String test(Pokemon pkmn, String method, String parameter, String newSpecies);
    }

    /** {@code pbCheckEvolutionEx(pokemon) { ... }} (:266-276): the first species the block answers for, or null. */
    public static String checkEvolutionEx(Pokemon pokemon, Check block) {
        if (pokemon == null || pokemon.species == null || pokemon.egg) {
            return null;                                                                          // :267 (no shadow Pokemon in this runtime)
        }
        if (holds(pokemon, "EVERSTONE")) {
            return null;                                                                          // :268
        }
        if ("BATTLEBOND".equals(pokemon.ability)) {
            return null;                                                                          // :269
        }
        for (PbsData.Evolution evo : evolutionsOf(pokemon)) {
            if (isNone(evo.method)) {
                continue;                                                                         // pbGetEvolvedFormData(..., true)
            }
            String ret = block.test(pokemon, evo.method, evo.parameter, evo.species);              // :272
            if (ret != null) {
                return ret;                                                                       // :273 break if ret>0
            }
        }
        return null;
    }

    /** {@code pbMiniCheckEvolution} (:253-256). */
    private static String miniCheck(Pokemon pkmn, String method, String parameter, String newSpecies, Env env) {
        return hasLevelUpCheck(method) && levelUpCheck(method, pkmn, parameter, env) ? newSpecies : null;
    }

    /** {@code pbMiniCheckEvolutionItem} (:258-261). */
    private static String miniCheckItem(Pokemon pkmn, String method, String parameter, String newSpecies, String item,
                                        Env env) {
        if (hasItemCheck(method)) {
            return itemCheck(method, pkmn, parameter, item, env) ? newSpecies : null;
        }
        // 201:782 ItemHappiness registers "levelUpCheck" with an extra item argument, so itemCheck finds nothing for it
        return null;
    }

    /**
     * {@code pbCheckEvolution(pokemon, item)} (:280-290): the internal name of the species the Pokemon can evolve into
     * now (by level-up methods, or by the item), or null.
     */
    public static String checkEvolution(Pokemon pokemon, String item, Env env) {
        if (item == null) {
            return checkEvolutionEx(pokemon, (p, method, parameter, species) -> miniCheck(p, method, parameter, species, env));
        }
        return checkEvolutionEx(pokemon, (p, method, parameter, species) -> miniCheckItem(p, method, parameter, species, item, env));
    }

    /** {@code pbCheckEvolution(pokemon)} for the after-battle block of pbAfterBattle (174:631-634). */
    public static String checkEvolutionAfterBattle(Pokemon pokemon) {
        return checkEvolutionEx(pokemon, (p, method, parameter, species) ->
                afterBattleCheck(method, p, parameter) ? species : null);
    }

    /** {@code pbCheckEvolutionEx} with the {@code afterEvolution} block (226:625-630). */
    public static void methodAfterEvolution(Pokemon pokemon, String newSpecies, Env env) {
        checkEvolutionEx(pokemon, (p, method, parameter, species) ->
                afterEvolution(method, p, species, parameter, newSpecies, env) ? species : null);
    }
}
