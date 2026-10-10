package pokemon.runtime.field;

import pokemon.runtime.pokemon.BallTypes;
import pokemon.runtime.pokemon.ItemUse;
import pokemon.runtime.pokemon.PBExperience;
import pokemon.runtime.pokemon.PBEvolution;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonGrowth;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.state.GameState;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The items used on a Pokemon: {@code ItemHandlers::UseOnPokemon} (189_PItem_ItemEffects:398-1819) with the helpers
 * it calls in 188_PItem_Items ({@code pbHPItem}, {@code pbRestorePP}, {@code pbRaiseEffortValues},
 * {@code pbChangeLevel}, {@code pbLearnMove} ...) and the bag's {@code pbUseItem} loop for them (:867-916).
 *
 * <p>The handlers are transcribed as the plugin's blocking Ruby: they call the {@link ItemScene} and return when the
 * player is done. Texts are the plugin's {@code _INTL} strings.</p>
 *
 * <p>登记: ①Pokemon#ability is stored as the ability's name here, so {@code abilityIndex} is derived from it (a Pokemon
 * whose two normal abilities are the same reads as index 0); ②the form lists ({@code getMoveList} of a form) and
 * {@code MultipleForms onSetForm} are not modelled; ③{@code pbCheckEvolution} / {@code PokemonEvolutionScene} are the
 * stage 8 evolution rewrite: stones use the checks of {@link ItemUse#evolution}; ④the summary screen's forget mode
 * ({@code pbForgetMove}) is the scene's; ⑤shadow Pokemon and mail are not modelled.</p>
 */
public final class ItemHandlers {
    static final String NO_EFFECT = "这没有任何效果…";
    private static final char PAUSE = '\u0001';
    /** 197_PokeBattle_Pokemon:59-60. */
    private static final int EV_LIMIT = 510;
    private static final int EV_STAT_LIMIT = 252;

    private static final Set<String> FULLHEAL_GROUP = new HashSet<>(Arrays.asList(
            "FULLHEAL", "LAVACOOKIE", "OLDGATEAU", "CASTELIACONE", "LUMIOSEGALETTE", "SHALOURSABLE",
            "BIGMALASADA", "LUMBERRY", "XIANGSHAWLPILL"));
    private static final Set<String> MINTS = new HashSet<>(Arrays.asList(
            "LONELYMINT", "ADAMANTMINT", "NAUGHTYMINT", "BRAVEMINT", "BOLDMINT", "IMPISHMINT", "LAXMINT",
            "RELAXEDMINT", "MODESTMINT", "MILDMINT", "RASHMINT", "QUIETMINT", "CALMMINT", "GENTLEMINT",
            "CAREFULMINT", "SASSYMINT", "TIMIDMINT", "HASTYMINT", "JOLLYMINT", "NAIVEMINT", "SERIOUSMINT"));
    private static final Set<String> NEUTRAL_NATURES = new HashSet<>(Arrays.asList(
            "HARDY", "DOCILE", "BASHFUL", "QUIRKY"));                   // nature ids 0, 6, 18, 24
    private static final Set<String> USE_ON_POKEMON = new HashSet<>();

    static {
        USE_ON_POKEMON.addAll(Arrays.asList(
                "POTION", "BERRYJUICE", "SWEETHEART", "SUPERPOTION", "HYPERPOTION", "MAXPOTION", "FRESHWATER",
                "SODAPOP", "LEMONADE", "MOOMOOMILK", "ORANBERRY", "SITRUSBERRY", "CIDER", "MIXEDBEVERAGES", "WWINE",
                "AWAKENING", "CHESTOBERRY", "BLUEFLUTE", "POKEFLUTE", "ANTIDOTE", "PECHABERRY", "BURNHEAL",
                "RAWSTBERRY", "PARLYZHEAL", "PARALYZEHEAL", "CHERIBERRY", "ICEHEAL", "ASPEARBERRY", "FULLRESTORE",
                "REVIVE", "MAXREVIVE", "EVEBURGER", "ENERGYPOWDER", "ENERGYROOT", "HEALPOWDER", "REVIVALHERB",
                "ETHER", "LEPPABERRY", "MAXETHER", "ELIXIR", "MAXELIXIR", "PPUP", "PPMAX", "HPUP", "PROTEIN", "IRON",
                "CALCIUM", "ZINC", "CARBOS", "HEALTHWING", "MUSCLEWING", "RESISTWING", "GENIUSWING", "CLEVERWING",
                "SWIFTWING", "RARECANDY", "HOPOBERRY", "POMEGBERRY", "KELPSYBERRY", "QUALOTBERRY", "HONDEWBERRY",
                "GREPABERRY", "TAMATOBERRY", "GRACIDEA", "REVEALGLASS", "PRISONBOTTLE", "DNASPLICERS", "NSOLARIZER",
                "NLUNARIZER", "ABILITYCAPSULE", "EXPCANDYXS", "EXPCANDYS", "EXPCANDYM", "EXPCANDYL", "EXPCANDYXL",
                "ROTOMCATALOG", "ABILITYPATCH", "REINSOFUNITY", "ZYGARDECUBE", "SCROLLOFWATERS", "SCROLLOFDARKNESS",
                "EXPPOT"));
        USE_ON_POKEMON.addAll(FULLHEAL_GROUP);
        USE_ON_POKEMON.addAll(MINTS);
    }

    private final PbsData pbs;
    private final GameState state;
    private final Supplier<LocalTime> clock;

    public ItemHandlers(PbsData pbs, GameState state, Supplier<LocalTime> clock) {
        this.pbs = pbs;
        this.state = state;
        this.clock = clock;
    }

    // =====================================================================
    // registration
    // =====================================================================

    /** {@code ItemHandlers.hasUseOnPokemon(item)} (188_PItem_Items:394-396), the evolution-stone predicate included. */
    public boolean hasUseOnPokemon(String item) {
        if (item == null) {
            return false;
        }
        if (USE_ON_POKEMON.contains(item)) {
            return true;
        }
        PbsData.Item data = pbs == null ? null : pbs.item(item);
        return data != null && data.type == 7;                                  // pbIsEvolutionStone? (:109-112)
    }

    /** The item's display name ({@code PBItems.getName}). */
    public String itemName(String item) {
        PbsData.Item data = pbs == null ? null : pbs.item(item);
        return data == null || data.name == null ? item : data.name;
    }

    private String moveName(PbsData.Move move) {
        return move == null ? "" : move.name != null ? move.name : move.internalName;
    }

    private String abilityName(String ability) {
        PbsData.Ability data = pbs == null || ability == null ? null : pbs.ability(ability);
        return data == null || data.name == null ? ability : data.name;
    }

    private static String fmt(String text, Object... args) {
        String result = text;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + (i + 1) + "}", String.valueOf(args[i]));
        }
        return result;
    }

    // =====================================================================
    // the bag's pbUseItem for items used on a Pokemon (188_PItem_Items:867-916)
    // =====================================================================

    /** The party screen's annotations while an evolution stone is chosen (:874-880): "可用" / "不可用" per Pokemon. */
    public String[] annotations(String item) {
        PbsData.Item data = pbs == null ? null : pbs.item(item);
        if (data == null || data.type != 7) {
            return null;                                                        // :874 pbIsEvolutionStone?
        }
        String[] annot = new String[state.trainer().party.size()];
        for (int i = 0; i < annot.length; i++) {
            Pokemon pkmn = state.trainer().party.get(i);
            boolean elig = pkmn != null && PBEvolution.checkEvolution(pkmn, item, world()) != null;   // :877
            annot[i] = elig ? "可用" : "不可用";                                  // :878
        }
        return annot;
    }

    private PBEvolution.Env world() {
        return new EvolutionWorld(state, pbs, null, clock);
    }

    private boolean isDay() {
        return PBDayNight.isDay(clock.get());
    }

    /** The line {@code pbUseItem} shows before any screen opens when the player has no Pokemon (:868-870, :848-851). */
    public String noPokemonMessage() {
        return state.trainer().pokemonCount() == 0 ? "没有宝可梦。" : null;
    }

    /**
     * The party-screen part of {@code pbUseItem} for useType 1 / 5 (:872-916): the screen opens with the annotations of
     * an evolution stone and the player picks a Pokemon until the item is used up or the screen is cancelled.
     *
     * @return 1 when the last use worked, else 0 (:916)
     */
    public int pbUseItemOnParty(String item, int useType, ItemScene scene) {
        boolean ret = false;
        scene.pbStartScene("要对哪只宝可梦使用？", annotations(item));               // :884
        while (true) {                                                          // :885
            scene.pbSetHelpText("要对哪只宝可梦使用？");                            // :886
            int chosen = scene.pbChoosePokemon("要对哪只宝可梦使用？");              // :887
            if (chosen < 0) {
                ret = false;                                                    // :888-890
                break;
            }
            Pokemon pkmn = state.trainer().party.get(chosen);                   // :892
            if (pkmn == null || pkmn.egg) {
                continue;                                                       // :893 pbCheckUseOnPokemon
            }
            // :894-901 UseOnPokemonMaximum has no handler in this plugin: one at a time.
            ret = useOnPokemon(item, pkmn, scene);                              // :903
            if (ret && useType == 1) {                                          // :904
                state.inventory().remove(item, 1);                              // :905
                if (!state.inventory().has(item)) {                             // :906
                    scene.pbMessage(fmt("最后的{1}被使用了。", itemName(item)));    // :907
                    break;
                }
            }
        }
        return ret ? 1 : 0;
    }

    /**
     * {@code pbUseItemOnPokemon(item, pkmn, scene)} for the party screen's item menu (:935-972).
     */
    public boolean pbUseItemOnPokemon(String item, Pokemon pkmn, ItemScene scene) {
        if (isMachine(item)) {                                                  // :937
            String machine = machineMove(item);                                 // :938
            PbsData.Move move = machine == null ? null : pbs.move(machine);
            if (move == null) {
                return false;                                                   // :939
            }
            String movename = moveName(move);                                   // :940
            if (!compatibleWithMove(pkmn, move)) {                              // :943
                scene.pbMessage(fmt("{1}不能学习{2}。", pkmn.name, movename));      // :944
            } else {
                scene.pbMessage(fmt("\\se[PC access]启动了{1}", itemName(item)) + PAUSE);   // :946
                if (scene.pbConfirm(fmt("想教{1}给宝可梦吗？", movename))) {         // :947 (the plugin passes only the move name)
                    if (pbLearnMove(pkmn, move, false, true, scene)) {          // :948
                        // :949 INFINITE_TMS is true: a TM is never used up.
                        PbsData.Item machineItem = pbs.item(item);
                        if (machineItem != null && machineItem.fieldUse == 6) {          // :950 pbIsTechnicalRecord?
                            state.inventory().remove(item, 1);                          // :951
                            pkmn.trMoves.add(machine);                                  // :952 pkmn.trmoves.push(machine)
                        }
                        return true;
                    }
                }
            }
            return false;                                                       // :958
        }
        boolean ret = useOnPokemon(item, pkmn, scene);                          // :961
        scene.pbClearAnnotations();                                             // :962
        scene.pbHardRefresh();                                                  // :963
        PbsData.Item data = pbs == null ? null : pbs.item(item);
        int useType = data == null ? 0 : data.fieldUse;                         // :964
        if (ret && useType == 1) {                                              // :965
            state.inventory().remove(item, 1);                                  // :966
            if (!state.inventory().has(item)) {                                 // :967
                scene.pbMessage(fmt("最后的{1}被使用了。", itemName(item)));        // :968
            }
        }
        return ret;
    }

    // =====================================================================
    // machines (188_PItem_Items:54-77, 253_PSystem_Utilities:955-1018)
    // =====================================================================

    /** {@code pbIsMachine?} (:69-72): TM, HM or TR. */
    public boolean isMachine(String item) {
        PbsData.Item data = pbs == null || item == null ? null : pbs.item(item);
        return data != null && (data.fieldUse == 3 || data.fieldUse == 4 || data.fieldUse == 6);
    }

    /** {@code pbGetMachine(item)} (:54-57): the move a machine teaches. */
    public String machineMove(String item) {
        PbsData.Item data = pbs == null || item == null ? null : pbs.item(item);
        if (data == null) {
            return null;
        }
        if (data.machine != null && !data.machine.isEmpty()) {
            return data.machine;
        }
        PbsData.Move move = ItemUse.machineMove(data, pbs);
        return move == null ? null : move.internalName;
    }

    /** {@code pokemon.compatibleWithMove?(move)} (197:543-545): the species is on the move's TM list. */
    public boolean compatibleWithMove(Pokemon pkmn, PbsData.Move move) {
        if (pkmn == null || pkmn.species == null || move == null) {
            return false;
        }
        // pbSpeciesCompatible?(self.fSpecies, move) (188_PItem_Items:869-874): the form's own entry (JIGGLYPUFF_1), not the base species
        com.badlogic.gdx.utils.Array<String> list = pbs.tmCompatibility.get(move.internalName);
        String fspecies = pkmn.internalName == null || pkmn.internalName.isEmpty() ? pkmn.species.internalName : pkmn.internalName;
        return list != null && list.contains(fspecies, false);
    }

    /**
     * The party-screen part of {@code pbUseItem} for a TM / HM / TR (188_PItem_Items:858-865): {@code pbMoveTutorChoose},
     * and a used-up TM (never, {@code INFINITE_TMS} is true); a technical record is used up and remembered (:859-863).
     */
    public int pbUseMachine(String item, PbsData.Move move, ItemScene scene) {
        int mon = pbMoveTutorChoose(move, null, true, scene);                  // :858
        if (mon < 0) return 0;
        PbsData.Item data = pbs.item(item);
        if (data != null && data.fieldUse == 6) {                              // :860 pbIsTechnicalRecord?
            state.inventory().remove(item, 1);                                 // :861
            state.trainer().party.get(mon).trMoves.add(move.internalName);     // :862
        }
        return 1;
    }

    /** {@code pbPokemonGiveScreen(item)} (210_PScreen_Party:1049-1059): the party screen asks who holds the item. */
    public boolean pbPokemonGiveScreen(String item, ItemScene scene) {
        scene.pbStartScene("给哪个宝可梦？", null);                                // :1050
        int pkmnid = scene.pbChoosePokemon("给哪个宝可梦？");                      // :1051
        boolean ret = false;
        if (pkmnid >= 0) {                                                      // :1053
            ret = pbGiveItemToPokemon(item, state.trainer().party.get(pkmnid), scene);   // :1054
        }
        scene.pbRefresh();                                                      // :1056
        return ret;
    }

    /** {@code pbGiveItemToPokemon(item, pkmn, scene)} (188_PItem_Items:1000-1052); 登记: mail is not modelled. */
    public boolean pbGiveItemToPokemon(String item, Pokemon pkmn, ItemScene scene) {
        String newitemname = itemName(item);                                    // :1001
        if (pkmn.egg) {
            scene.pbDisplay("宝可梦蛋不能携带物品。");                              // :1003
            return false;
        }
        if (pkmn.item != null && !pkmn.item.isEmpty()) {                        // :1009 hasItem?
            String olditemname = itemName(pkmn.item);                           // :1010
            scene.pbDisplay(fmt("{1}已经携带{2}了。", pkmn.name, olditemname) + PAUSE);   // :1012-1016 (all branches read the same)
            if (scene.pbConfirm("想要交换这两个道具吗？")) {                          // :1018
                state.inventory().remove(item, 1);                              // :1019
                state.inventory().add(pkmn.item, 1);                            // :1020 pbStoreItem (the bag has no capacity here)
                pkmn.item = item;                                               // :1037
                scene.pbDisplay(fmt("从{2}身上拿回{1}了，\n给予了{3}。", olditemname, pkmn.name, newitemname));   // :1038
                return true;
            }
        } else {
            state.inventory().remove(item, 1);                                  // :1045
            pkmn.item = item;                                                   // :1046
            scene.pbDisplay(fmt("{1}现在携带着{2}。", pkmn.name, newitemname));       // :1047
            return true;
        }
        return false;
    }

    /** {@code pbMoveTutorAnnotations(move, movelist)} (:955-980). */
    public String[] moveTutorAnnotations(PbsData.Move move, List<String> movelist) {
        String[] ret = new String[state.trainer().party.size()];
        for (int i = 0; i < ret.length; i++) {
            Pokemon pkmn = state.trainer().party.get(i);
            boolean found = false;
            for (Pokemon.MoveSlot known : pkmn.moves) {
                if (!pkmn.egg && known.move != null && known.move.internalName.equals(move.internalName)) {
                    ret[i] = "已学会";
                    found = true;
                }
            }
            if (found) {
                continue;
            }
            String species = pkmn.species == null ? null : pkmn.species.internalName;
            if (!pkmn.egg && movelist != null && movelist.contains(species)) {
                ret[i] = "可用";
            } else if (!pkmn.egg && compatibleWithMove(pkmn, move)) {
                ret[i] = "可用";
            } else {
                ret[i] = "不可用";
            }
        }
        return ret;
    }

    /**
     * {@code pbMoveTutorChoose(move, movelist, bymachine)} (253_PSystem_Utilities:982-1018) on a party screen.
     *
     * @return the party index of the Pokemon that learned the move, or -1
     */
    public int pbMoveTutorChoose(PbsData.Move move, List<String> movelist, boolean byMachine, ItemScene scene) {
        int ret = -1;
        String movename = moveName(move);                                       // :991
        String[] annot = moveTutorAnnotations(move, movelist);                  // :992
        scene.pbStartScene("教给哪个宝可梦？", annot);                              // :995
        while (true) {                                                          // :996
            int chosen = scene.pbChoosePokemon("教给哪个宝可梦？");                  // :997
            if (chosen < 0) {
                break;                                                          // :998
            }
            Pokemon pokemon = state.trainer().party.get(chosen);                // :999
            if (pokemon.egg) {
                scene.pbMessage("蛋不能学会招式。");                                // :1001
            } else if (movelist != null && !movelist.contains(pokemon.species.internalName)) {   // :1004
                scene.pbMessage(fmt("{1}不能学习{2}。", pokemon.name, movename));
            } else if (!compatibleWithMove(pokemon, move)) {                    // :1006
                scene.pbMessage(fmt("{1}不能学习{2}。", pokemon.name, movename));
            } else if (pbLearnMove(pokemon, move, false, byMachine, scene)) {   // :1009
                ret = chosen;
                break;
            }
        }
        return ret;
    }

    // =====================================================================
    // ItemHandlers::UseFromBag / UseInField that only change state (189_PItem_ItemEffects:21-392)
    // =====================================================================

    private static final Set<String> BAG_FIELD_ITEMS = new HashSet<>(Arrays.asList(
            "REPEL", "SUPERREPEL", "MAXREPEL", "INFINITEREPEL", "BLACKFLUTE", "WHITEFLUTE", "EXPALL", "EXPALLOFF",
            "COINCASE", "BICYCLE", "MACHBIKE", "ACROBIKE", "TOWNMAP"));

    /**
     * {@code ItemHandlers.hasUseInFieldHandler(item)} (188_PItem_Items:373-375): the items that have a {@code UseInField} handler in
     * the plugin (189_PItem_ItemEffects, 192_PItem_PokeRadar:246, 323_Egg_Hatcher:349) - the ones the bag lets the player register.
     * The bicycles other than {@code BICYCLE} are copies of UseText / UseFromBag only.
     */
    public static boolean hasUseInFieldHandler(String item) {
        return item != null && USE_IN_FIELD.contains(item);
    }

    private static final Set<String> USE_IN_FIELD = new HashSet<>(Arrays.asList(
            "INFINITEREPEL", "REPEL", "SUPERREPEL", "MAXREPEL", "BLACKFLUTE", "WHITEFLUTE", "HONEY", "ESCAPEROPE", "INFINITEROPE",
            "SACREDASH", "BICYCLE", "SUPERROD", "ITEMFINDER", "DOWSINGMCHN", "DOWSINGMACHINE", "TOWNMAP", "COINCASE", "EXPALL",
            "EXPALLOFF", "LANTERN", "EONFLUTE", "ETHEREALNEXUS", "HEAVENCRYSEAL", "POKERADAR", "EGGHATCHER"));

    /** Whether {@link #useInField} handles the item (SACREDASH needs the party screen: {@link #sacredAsh}). */
    public boolean hasBagFieldHandler(String item) {
        return item != null && (BAG_FIELD_ITEMS.contains(item) || "SACREDASH".equals(item) || isMapItem(item));
    }

    /**
     * The items whose {@code UseFromBag} answers 2 (used, close the screens) and whose {@code UseInField} body acts on the
     * map: it runs after the bag and the pause menu have closed ({@code pbUseKeyItemInField}).
     */
    public static boolean isMapItem(String item) {
        return "SUPERROD".equals(item) || "ESCAPEROPE".equals(item) || "INFINITEROPE".equals(item) || "LANTERN".equals(item)
                || "EONFLUTE".equals(item) || "ETHEREALNEXUS".equals(item);
    }

    /** {@code UseFromBag} answered 4 (189:25-35 {@code :ESCAPEROPE}): the bag deletes the item before the screens end. */
    public static boolean consumedInBag(String item) {
        return "ESCAPEROPE".equals(item);
    }

    /** 189:1522-1620: the maps the Eon Flute / Ethereal Nexus / Heaven Seal refuse ({@code ESMM_Config::BAN_MAPS}, 335_001_ESMM_Config:10-). */
    public static boolean banMap(int mapId) {
        return BAN_MAPS.contains(mapId);
    }

    private static final Set<Integer> BAN_MAPS = new HashSet<>(Arrays.asList(
            1, 119, 224, 292, 293, 294, 295, 296, 347, 60, 209, 210, 228, 297, 321, 371, 140, 441, 442, 444,
            417, 440, 443, 445, 446, 447, 448, 450, 451, 478, 485, 388, 462));

    /**
     * {@code UseFromBag :SUPERROD} (189_PItem_ItemEffects:55-63) and the check of {@code UseInField} (:319-323): water in
     * front, and no cliff edge unless surfing.
     */
    public static boolean canFish(boolean facingWater, boolean facingPassable, boolean surfing) {
        return facingWater && (facingPassable || surfing);
    }

    /** {@code pbUseItemMessage(item)} (188_PItem_Items:984-991). */
    private void pbUseItemMessage(String item, ItemScene scene) {
        scene.pbMessage(fmt("使用了{1}。", itemName(item)));
    }

    /** {@code pbRepel(item, steps)} (:124-136). */
    private int pbRepel(String item, int steps, ItemScene scene) {
        pokemon.runtime.state.FieldGlobals g = state.fieldGlobals();
        if (g.infRepel) {
            scene.pbMessage("无限喷雾的效果仍然存在。");                           // :126
            return 0;
        }
        if (g.repel > 0) {
            scene.pbMessage("但喷雾剂仍然有效。");                                 // :130
            return 0;
        }
        pbUseItemMessage(item, scene);                                          // :133
        g.repel = steps;                                                        // :134
        return 3;
    }

    /**
     * {@code ItemHandlers.triggerUseFromBag(item)} for the handlers that only change state (:138-392); the codes are
     * 0 not used, 1 used, 3 used and consumed. 登记: the handlers that move the player or open a scene of the map
     * (escape rope, Itemfinder, bicycle, rods, Lantern, Honey, flutes of the Eon, Town Map) are roadmap stages 5B / 7.
     */
    public int useInField(String item, ItemScene scene) {
        pokemon.runtime.state.FieldGlobals g = state.fieldGlobals();
        switch (item) {
            case "INFINITEREPEL": {                                             // :138-148
                g.repel = 0;
                boolean infinite = g.infRepel;
                g.infRepel = !infinite;
                scene.pbMessage(infinite ? "已关闭无限喷雾。" : "已开启无限喷雾。");
                return 1;
            }
            case "REPEL":
                return pbRepel(item, 100, scene);                               // :150
            case "SUPERREPEL":
                return pbRepel(item, 200, scene);                               // :154
            case "MAXREPEL":
                return pbRepel(item, 250, scene);                               // :158
            case "BLACKFLUTE":                                                  // :191-197
                pbUseItemMessage(item, scene);
                scene.pbMessage("野生的宝可梦将被驱散。");
                g.blackFluteUsed = true;
                g.whiteFluteUsed = false;
                return 1;
            case "WHITEFLUTE":                                                  // :199-205
                pbUseItemMessage(item, scene);
                scene.pbMessage("野生的宝可梦将被吸引。");
                g.blackFluteUsed = false;
                g.whiteFluteUsed = true;
                return 1;
            case "BICYCLE":                                                     // :306-316 (MACHBIKE / ACROBIKE are copies)
            case "MACHBIKE":
            case "ACROBIKE": {
                Vehicles vehicles = new Vehicles(pbs, state);
                if (vehicles.bikeCheck(state.currentMapId(), scene::pbMessage)) {
                    if (g.bicycle) {
                        vehicles.dismountBike();
                    } else {
                        vehicles.mountBike();
                    }
                    return 1;
                }
                return 0;
            }
            case "COINCASE":                                                    // :377-380
                scene.pbMessage(fmt("代币:{1}", String.format("%,d", g.coins)));
                return 1;
            case "EXPALL":                                                      // :382-386
                pbChangeItem("EXPALL", "EXPALLOFF");
                scene.pbMessage("经验共享已关闭。");
                return 1;
            case "EXPALLOFF":                                                   // :388-392
                pbChangeItem("EXPALLOFF", "EXPALL");
                scene.pbMessage("经验共享已打开。");
                return 1;
            default:
                return 0;
        }
    }

    /** {@code $PokemonBag.pbChangeItem(old, new)} (195_PItem_Bag:141-): the stack turns into the other item. */
    private void pbChangeItem(String olditem, String newitem) {
        int count = state.inventory().count(olditem);
        if (count > 0) {
            state.inventory().remove(olditem, count);
            state.inventory().add(newitem, count);
        }
    }

    /** {@code ItemHandlers::UseInField :SACREDASH} (:271-304) on a party screen; returns 3 when someone was revived. */
    public int sacredAsh(ItemScene scene) {
        if (state.trainer().pokemonCount() == 0) {
            scene.pbMessage("没有宝可梦。");                                       // :272-275
            return 0;
        }
        boolean canrevive = false;
        for (Pokemon i : state.trainer().party.members()) {                     // :277-280
            if (!i.egg && i.hp <= 0) {
                canrevive = true;
                break;
            }
        }
        if (!canrevive) {
            scene.pbMessage("这没有任何效果…");                                    // :282
            return 0;
        }
        int revived = 0;
        scene.pbStartScene("使用道具……", null);                                   // :289
        for (int i = 0; i < state.trainer().party.size(); i++) {                // :290
            Pokemon p = state.trainer().party.get(i);
            if (!p.egg && p.hp <= 0) {
                revived += 1;
                p.hp = p.maxHp();                                               // heal: HP, status, PP
                p.status = "";
                p.statusCount = 0;
                for (Pokemon.MoveSlot m : p.moves) {
                    m.pp = m.totalPp();
                }
                scene.pbRefresh();                                              // :294
                scene.pbDisplay(fmt("{1}的HP回复了。", p.name));                   // :295
            }
        }
        if (revived == 0) {
            scene.pbDisplay("这没有任何效果…");                                    // :299
        }
        return revived == 0 ? 0 : 3;
    }

    // =====================================================================
    // 188_PItem_Items helpers
    // =====================================================================

    private static boolean able(Pokemon pkmn) {
        return !pkmn.egg && pkmn.hp > 0;                                        // PokeBattle_Pokemon:735
    }

    /** {@code pbItemRestoreHP} (:567-573). */
    static int pbItemRestoreHP(Pokemon pkmn, int restoreHP) {
        int newHP = pkmn.hp + restoreHP;
        if (newHP > pkmn.maxHp()) {
            newHP = pkmn.maxHp();
        }
        int hpGain = newHP - pkmn.hp;
        pkmn.hp = newHP;
        return hpGain;
    }

    /** {@code pbHPItem(pkmn, restoreHP, scene)} (:575-584). */
    private boolean pbHPItem(Pokemon pkmn, int restoreHP, ItemScene scene) {
        if (!able(pkmn) || pkmn.hp == pkmn.maxHp()) {
            scene.pbDisplay(NO_EFFECT);
            return false;
        }
        int hpGain = pbItemRestoreHP(pkmn, restoreHP);
        scene.pbRefresh();
        scene.pbDisplay(fmt("{1}的HP恢复了{2}点。", pkmn.name, hpGain));
        return true;
    }

    /** {@code pbRestorePP(pkmn, idxMove, pp)} (:602-610). */
    static int pbRestorePP(Pokemon pkmn, int idxMove, int pp) {
        if (idxMove < 0 || idxMove >= pkmn.moves.size || pkmn.moves.get(idxMove).move == null) {
            return 0;                                                           // :603
        }
        Pokemon.MoveSlot slot = pkmn.moves.get(idxMove);
        if (slot.totalPp() <= 0) {
            return 0;                                                           // :604
        }
        int oldpp = slot.pp;
        int newpp = slot.pp + pp;
        if (newpp > slot.totalPp()) {
            newpp = slot.totalPp();
        }
        slot.pp = newpp;
        return newpp - oldpp;
    }

    /** {@code pbRaiseEffortValues(pkmn, ev, evgain, evlimit)} (:641-661). */
    static int pbRaiseEffortValues(Pokemon pkmn, int ev, int evgain, boolean evlimit) {
        if (evlimit && pkmn.evs[ev] >= 100) {
            return 0;                                                           // :642
        }
        int totalev = 0;
        for (int i = 0; i < 6; i++) {
            totalev += pkmn.evs[i];                                             // :643-646
        }
        if (totalev + evgain > EV_LIMIT) {
            evgain = EV_LIMIT - totalev;                                        // :647-649
        }
        if (pkmn.evs[ev] + evgain > EV_STAT_LIMIT) {
            evgain = EV_STAT_LIMIT - pkmn.evs[ev];                              // :650-652
        }
        if (evlimit && pkmn.evs[ev] + evgain > EV_STAT_LIMIT) {
            evgain = EV_STAT_LIMIT - pkmn.evs[ev];                              // :653-655
        }
        if (evgain > 0) {
            final int gain = evgain;
            pkmn.recalculatingStats(() -> pkmn.evs[ev] += gain);                // :657-658
        }
        return evgain;
    }

    /** {@code pbRaiseHappinessAndLowerEV} (:663-681). */
    private boolean pbRaiseHappinessAndLowerEV(Pokemon pkmn, ItemScene scene, int ev, String[] messages) {
        boolean h = pkmn.happiness < 255;
        boolean e = pkmn.evs[ev] > 0;
        if (!h && !e) {
            scene.pbDisplay(NO_EFFECT);
            return false;
        }
        if (h) {
            changeHappiness(pkmn, "evberry");
        }
        if (e) {
            pkmn.recalculatingStats(() -> pkmn.evs[ev] = Math.max(0, pkmn.evs[ev] - 10));   // :674-677
        }
        scene.pbRefresh();
        scene.pbDisplay(messages[2 - (h ? 0 : 2) - (e ? 0 : 1)]);
        return true;
    }

    /** {@code pbEmptyAllEV} (:683-700). */
    private boolean pbEmptyAllEV(Pokemon pkmn, ItemScene scene, String message) {
        boolean noEV = true;
        for (int ev : pkmn.evs) {
            if (ev > 0) {
                noEV = false;
                break;
            }
        }
        if (noEV) {
            scene.pbDisplay("没有任何效果。");
            return false;
        }
        pkmn.recalculatingStats(() -> Arrays.fill(pkmn.evs, 0));
        scene.pbRefresh();
        scene.pbDisplay(message);
        return true;
    }

    private void changeHappiness(Pokemon pkmn, String method) {
        int luxury = pbs == null ? -1 : BallTypes.ballType(pbs, "LUXURYBALL");
        pkmn.changeHappiness(method, state.currentMapId(), luxury);
    }

    private static int[] statSnapshot(Pokemon pkmn) {
        return new int[] {pkmn.attack(), pkmn.defense(), pkmn.speed(), pkmn.spAtk(), pkmn.spDef(), pkmn.maxHp()};
    }

    /** {@code pbChangeLevel(pkmn, newlevel, scene, should_learn)} (:465-545). */
    public void pbChangeLevel(Pokemon pkmn, int newlevel, ItemScene scene, boolean shouldLearn) {
        if (newlevel < 1) {
            newlevel = 1;                                                       // :466
        }
        int mLevel = PBExperience.maxLevel();                                   // :467
        if (newlevel > mLevel) {
            newlevel = mLevel;                                                  // :468
        }
        int oldLevel = pkmn.level;                                              // :469
        if (pkmn.level == newlevel) {
            scene.pbMessage(fmt("{1}的等级没有变化。", pkmn.name));                // :470-471
            return;
        }
        int[] before = statSnapshot(pkmn);
        final int target = newlevel;
        if (pkmn.level > newlevel) {                                            // :472
            setLevel(pkmn, target);                                             // :479-480
            scene.pbRefresh();                                                  // :481
            scene.pbMessage(fmt("{1}下降到Lv.{2}！", pkmn.name, pkmn.level));      // :482
            scene.pbTopRightWindow(statDiffWindow(pkmn, before));               // :489-490
            scene.pbTopRightWindow(statWindow(pkmn));                           // :491-492
            return;
        }
        setLevel(pkmn, target);                                                 // :500
        changeHappiness(pkmn, "vitamin");                                       // :501
        scene.pbRefresh();                                                      // :503
        scene.pbDisplay(fmt("{1}升到了{2}级！", pkmn.name, pkmn.level));           // :504-508 (the scene is the party screen)
        scene.pbTopRightWindow(statDiffWindow(pkmn, before));                   // :515-516
        scene.pbTopRightWindow(statWindow(pkmn));                               // :517-518
        if (shouldLearn) {                                                      // :519
            List<PbsData.LearnMove> realList = new ArrayList<>();               // :521
            if (pkmn.species != null) {
                for (PbsData.LearnMove m : pkmn.species.moves) {                // :522-526 getMoveList
                    if (m.level <= oldLevel || m.level > pkmn.level) {
                        continue;
                    }
                    realList.add(m);
                }
            }
            if (realList.size() == 1 || realList.size() > 1
                    && scene.pbConfirm(fmt("要{1}立即学习招式吗？", pkmn.name))) {   // :527-528
                for (PbsData.LearnMove m : realList) {
                    pbLearnMove(pkmn, pbs.move(m.move), true, false, scene);    // :529
                }
            }
        }
        // :532-543 the evolution check after a level-up is commented out in the plugin.
    }

    /** {@code pkmn.level = value; pkmn.calcStats}: the level and its start experience (:121-127), HP keeping its distance. */
    private void setLevel(Pokemon pkmn, int level) {
        pkmn.recalculatingStats(() -> {
            pkmn.level = level;
            pkmn.exp = PBExperience.pbGetStartExperience(level, pkmn.growthRate());
        });
    }

    private static String statDiffWindow(Pokemon pkmn, int[] before) {
        int[] now = statSnapshot(pkmn);
        return fmt("最大HP<r>+{1}\r\n攻击<r>+{2}\r\n防御<r>+{3}\r\n特攻<r>+{4}\r\n特防<r>+{5}\r\n速度<r>+{6}",
                now[5] - before[5], now[0] - before[0], now[1] - before[1], now[3] - before[3], now[4] - before[4],
                now[2] - before[2]);
    }

    private static String statWindow(Pokemon pkmn) {
        return fmt("最大HP<r>{1}\r\n攻击<r>{2}\r\n防御<r>{3}\r\n特攻<r>{4}\r\n特防<r>{5}\r\n速度<r>{6}",
                pkmn.maxHp(), pkmn.attack(), pkmn.defense(), pkmn.spAtk(), pkmn.spDef(), pkmn.speed());
    }

    /** {@code pbLearnMove(pkmn, move, ignoreifknown, bymachine)} (:779-821). */
    public boolean pbLearnMove(Pokemon pkmn, PbsData.Move move, boolean ignoreIfKnown, boolean byMachine,
                               ItemScene scene) {
        if (pkmn == null || move == null) {
            return false;                                                       // :780
        }
        String movename = moveName(move);                                       // :781
        if (pkmn.egg) {
            scene.pbMessage("蛋不能学会招式。");                                   // :782-785
            return false;
        }
        String pkmnname = pkmn.name;                                            // :790
        for (Pokemon.MoveSlot known : pkmn.moves) {                             // :791 hasMove?
            if (known.move != null && known.move.internalName.equals(move.internalName)) {
                if (!ignoreIfKnown) {
                    scene.pbMessage(fmt("{1}已经学会了{2}。", pkmnname, movename));   // :792
                }
                return false;
            }
        }
        if (pkmn.moves.size < 4) {                                              // :795
            pkmn.learnMoveSilently(move);                                       // :796
            scene.pbMessage(fmt("\\se[]{1}学会了{2}！\\se[Pkmn move learnt]", pkmnname, movename));   // :797
            return true;
        }
        while (true) {                                                          // :800
            if (!byMachine) {
                scene.pbMessage(fmt("{1}想要学会{2}\n可是它已经学会四个招式了。", pkmnname, movename) + PAUSE);   // :801
            }
            scene.pbMessage(fmt("请选择将被{1}替换的招式。", movename));              // :802
            int forgetmove = scene.pbForgetMove(pkmn, move);                    // :803
            if (forgetmove >= 0) {                                              // :804
                String oldmovename = moveName(pkmn.moves.get(forgetmove).move); // :805
                pkmn.moves.set(forgetmove, new Pokemon.MoveSlot(move));         // :807 PBMove.new(move): full PP
                // :808-810 NEWEST_BATTLE_MECHANICS is true: the new move keeps its full PP.
                scene.pbMessage("1,\\wt[16] 2,\\wt[16]...\\wt[16] ...\\wt[16] ... 当当！\\se[Battle ball drop]" + PAUSE);   // :811
                scene.pbMessage(fmt("{1}遗忘了{2}。\n以及……", pkmnname, oldmovename) + PAUSE);   // :812
                scene.pbMessage(fmt("\\se[]{1}学会了{2}！\\se[Pkmn move learnt]", pkmnname, movename));   // :813
                if (byMachine) {
                    changeHappiness(pkmn, "machine");                           // :814
                }
                return true;
            }
            scene.pbMessage(fmt("{1}没有学习{2}。", pkmnname, movename));          // :817
            return false;
        }
    }

    // =====================================================================
    // the level lock shared by the candies (RARECANDY :876-910, EXPCANDY :1211-1255, EXPPOT :1751-1790)
    // =====================================================================

    /** The cap index: {@code -1}, or the first badge short of the player's progress (when the level lock is on). */
    private int levelLockIndex(boolean leaguePass) {
        int index = -1;
        if (leaguePass) {
            for (int i = 8; i <= 15; i++) {
                if (!state.trainer().badges.contains(i)) {
                    index = i + 1;
                    break;
                }
            }
        } else {
            for (int i = 0; i <= 7; i++) {
                if (!state.trainer().badges.contains(i)) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    /** What the candies say when the level lock stops them; true when the use is over. */
    private boolean levelLockMessage(Pokemon pkmn, int index, ItemScene scene) {
        if (index == -1) {
            scene.pbDisplay("没有任何效果。");
        } else {
            scene.pbDisplay(fmt("直到解锁{1}级限制前无法使用。", PBExperience.maxLevelAt(index + 1)));
        }
        return true;
    }

    private boolean levelLockOn() {
        return state.switches().get(199);
    }

    private boolean leaguePass() {
        return state.switches().get(12);
    }

    private boolean rareCandy(String item, Pokemon pkmn, ItemScene scene) {
        int index = -1;
        if (pkmn.level >= PBExperience.maxLevelAt(index)) {                     // :880
            scene.pbDisplay("没有任何效果。");
            return false;
        }
        if (levelLockOn()) {                                                    // :885
            index = levelLockIndex(leaguePass());                               // :886-900
            if (pkmn.level >= PBExperience.maxLevelAt(index)) {                 // :902
                levelLockMessage(pkmn, index, scene);                           // :903-907
                return false;
            }
        }
        int qty = 1;                                                            // :911
        int quantity = Math.min(PBExperience.maxLevelAt(-1) - pkmn.level, state.inventory().count(item));   // :912
        if (quantity > 1) {                                                     // :913
            qty = scene.pbMessageChooseNumber(
                    fmt("需要使用多少个{1}？\n(当前最大只能使用{2}个)", itemName(item), quantity), quantity, 1, 0);   // :914-918
        }
        if (qty == 0) {
            return false;                                                       // :920
        }
        pbChangeLevel(pkmn, pkmn.level + qty, scene, true);                     // :921
        state.inventory().remove(item, qty - 1);                                // :922
        scene.pbHardRefresh();                                                  // :923
        return true;
    }

    private static int expCandyAmount(String item) {
        switch (item) {
            case "EXPCANDYXS": return 100;                                      // :1219
            case "EXPCANDYS": return 800;                                       // :1220
            case "EXPCANDYM": return 3000;                                      // :1221
            case "EXPCANDYL": return 10000;                                     // :1222
            default: return 30000;                                              // :1223 EXPCANDYXL
        }
    }

    private boolean expCandy(String item, Pokemon pkmn, ItemScene scene) {
        int index = -1;
        if (pkmn.level >= PBExperience.maxLevelAt(index)) {                     // :1215
            scene.pbDisplay("没有任何效果。");
            return false;
        }
        int experience = expCandyAmount(item);
        String growth = pkmn.growthRate();
        int experienceGain = PBExperience.pbGetStartExperience(PBExperience.maxLevelAt(index), growth) - pkmn.exp;   // :1224
        if (levelLockOn()) {                                                    // :1226
            index = levelLockIndex(leaguePass());                               // :1227-1241
            if (pkmn.level >= PBExperience.maxLevelAt(index)) {                 // :1243
                if (PBEvolution.checkEvolution(pkmn, null, world()) == null) {    // :1244-1245 pbCheckEvolution(pkmn,0)<=0
                    levelLockMessage(pkmn, index, scene);                       // :1246-1250
                    return false;
                }
            }
            experienceGain = PBExperience.pbGetStartExperience(PBExperience.maxLevelAt(index), growth) - pkmn.exp;   // :1254
        }
        int qty = 1;                                                            // :1256
        int quantity = Math.min((int) Math.ceil(experienceGain * 1.0 / experience), state.inventory().count(item));   // :1257
        if (quantity > 1) {                                                     // :1258
            qty = scene.pbMessageChooseNumber(
                    fmt("需要使用多少个{1}？\n(当前最大只能使用{2}个)", itemName(item), quantity), quantity, 1, 0);   // :1259-1263
            if (qty == 0) {
                return false;                                                   // :1264
            }
        }
        if (experienceGain >= qty * experience) {
            experienceGain = qty * experience;                                  // :1266
        }
        gainExperience(pkmn, experienceGain, scene, false);                     // :1267-1286
        state.inventory().remove(item, qty - 1);                                // :1287
        return true;
    }

    /** The shared tail of the experience candies and the Exp Pot (:1267-1286, :1798-1817). */
    private void gainExperience(Pokemon pkmn, int expGain, ItemScene scene, boolean learnInChangeLevel) {
        String growth = pkmn.growthRate();
        int newexp = PBExperience.pbAddExperience(pkmn.exp, expGain, growth);                 // :1267
        int newlevel = PBExperience.pbGetLevelFromExperience(newexp, growth);                 // :1268
        int curlevel = pkmn.level;                                                            // :1269
        int leveldif = newlevel - curlevel;                                                   // :1270
        scene.pbDisplay(fmt("你的宝可梦获得了{1}点经验值！", expGain));                         // :1271
        if (newlevel == curlevel) {                                                           // :1272
            pkmn.recalculatingStats(() -> pkmn.exp = newexp);                                 // :1273-1274
            scene.pbRefresh();                                                                // :1275
            return;
        }
        scene.pbSEPlay("Pkmn level up");                                                      // :1277
        int oldLevel = pkmn.level;                                                            // :1278
        pbChangeLevel(pkmn, pkmn.level + leveldif, scene, learnInChangeLevel);                // :1279
        if (pkmn.species != null) {
            for (PbsData.LearnMove m : pkmn.species.moves) {                                  // :1280-1284
                if (m.level <= oldLevel || m.level > pkmn.level) {
                    continue;
                }
                pbLearnMove(pkmn, pbs.move(m.move), true, false, scene);
            }
        }
        scene.pbHardRefresh();                                                                // :1285
    }

    private boolean expPot(Pokemon pkmn, ItemScene scene) {
        if (state.trainer().expPot == 0) {                                      // :1745-1749
            scene.pbDisplay("可供灌注的经验值不足。");
            return false;
        }
        int index = -1;
        if (pkmn.level >= PBExperience.maxLevelAt(index)) {                     // :1755
            scene.pbDisplay("没有任何效果。");
            return false;
        }
        String growth = pkmn.growthRate();
        int expMax = PBExperience.pbGetStartExperience(PBExperience.maxLevelAt(index), growth) - pkmn.exp;   // :1759
        if (levelLockOn()) {                                                    // :1761
            index = levelLockIndex(leaguePass());                               // :1762-1776
            if (pkmn.level >= PBExperience.maxLevelAt(index)) {                 // :1778
                if (PBEvolution.checkEvolution(pkmn, null, world()) == null) {    // :1779-1780
                    levelLockMessage(pkmn, index, scene);                       // :1781-1785
                    return false;
                }
            }
            expMax = PBExperience.pbGetStartExperience(PBExperience.maxLevelAt(index), growth) - pkmn.exp;   // :1789
        }
        expMax = Math.min(state.trainer().expPot, expMax);                      // :1791
        int expGain = scene.pbMessageChooseNumber(
                fmt("需要灌注多少经验值？\n(当前最大只能灌注{1})", expMax), expMax, 1, 0);   // :1792-1796
        if (expGain == 0) {
            return false;                                                       // :1797
        }
        gainExperience(pkmn, expGain, scene, true);                             // :1798-1817 (pbChangeLevel keeps should_learn)
        state.trainer().expPot -= expGain;                                      // :1818
        return true;
    }

    // =====================================================================
    // abilities
    // =====================================================================

    /** [name, index] pairs: the normal abilities at 0, 1, then the hidden one at 2 ({@code getAbilityList}, :265-280). */
    private List<Object[]> abilityList(Pokemon pkmn) {
        List<Object[]> ret = new ArrayList<>();
        List<String> natural = naturalAbilities(pkmn);
        for (int i = 0; i < natural.size(); i++) {
            if (natural.get(i) != null && !natural.get(i).isEmpty()) {
                ret.add(new Object[] {natural.get(i), i});
            }
        }
        String hidden = hiddenAbility(pkmn);
        if (hidden != null && !hidden.isEmpty()) {
            ret.add(new Object[] {hidden, 2});
        }
        return ret;
    }

    private static List<String> naturalAbilities(Pokemon pkmn) {
        List<String> list = new ArrayList<>();
        if (pkmn.form != null && pkmn.form.abilities != null && pkmn.form.abilities.size > 0) {
            for (String a : pkmn.form.abilities) {
                list.add(a);
            }
        } else if (pkmn.species != null) {
            for (String a : pkmn.species.abilities) {
                list.add(a);
            }
        }
        return list;
    }

    private static String hiddenAbility(Pokemon pkmn) {
        if (pkmn.form != null && pkmn.form.hiddenAbility != null && !pkmn.form.hiddenAbility.isEmpty()) {
            return pkmn.form.hiddenAbility;
        }
        return pkmn.species == null ? null : pkmn.species.hiddenAbility;
    }

    /** {@code abilityIndex} (:219-221) read back from the stored ability name. */
    int abilityIndex(Pokemon pkmn) {
        String hidden = hiddenAbility(pkmn);
        List<String> natural = naturalAbilities(pkmn);
        if (pkmn.ability != null && hidden != null && !hidden.isEmpty() && pkmn.ability.equals(hidden)
                && !natural.contains(pkmn.ability)) {
            return 2;
        }
        for (int i = 0; i < natural.size(); i++) {
            if (pkmn.ability != null && pkmn.ability.equals(natural.get(i))) {
                return i;
            }
        }
        return pkmn.personalID & 1;                                             // :220 (@personalID&1)
    }

    /** {@code setAbility(index)} (:255-257) followed by the {@code ability} lookup of :224-245. */
    private void setAbility(Pokemon pkmn, int index) {
        int abilIndex = index;
        String hidden = hiddenAbility(pkmn);
        if (abilIndex >= 2) {
            if (hidden != null && !hidden.isEmpty() && abilIndex == 2) {
                pkmn.ability = hidden;                                          // :233
                return;
            }
            abilIndex = pkmn.personalID & 1;                                    // :235
        }
        List<String> natural = naturalAbilities(pkmn);
        String ret = abilIndex < natural.size() ? natural.get(abilIndex) : null;
        if (ret == null || ret.isEmpty()) {
            int other = (abilIndex + 1) % 2;                                    // :241
            ret = other < natural.size() ? natural.get(other) : null;
        }
        pkmn.ability = ret;
    }

    private boolean abilityCapsule(Pokemon pkmn, ItemScene scene) {
        List<Object[]> abils = abilityList(pkmn);                               // :1185
        String abil1 = null;
        String abil2 = null;
        for (Object[] i : abils) {                                              // :1187-1190
            if ((int) i[1] == 0) abil1 = (String) i[0];
            if ((int) i[1] == 1) abil2 = (String) i[0];
        }
        boolean hasHidden = abilityIndex(pkmn) >= 2;                            // :259-262
        if (abil1 == null || abil2 == null || hasHidden || pkmn.isSpecies("ZYGARDE")) {   // :1191
            scene.pbDisplay(NO_EFFECT);
            return false;
        }
        int newabil = (abilityIndex(pkmn) + 1) % 2;                             // :1195
        String newabilname = abilityName(newabil == 0 ? abil1 : abil2);         // :1196
        if (scene.pbConfirm(fmt("是否要将{1}的特性更改为{2}？", pkmn.name, newabilname))) {   // :1197
            setAbility(pkmn, newabil);                                          // :1199
            scene.pbRefresh();
            scene.pbDisplay(fmt("{1}的特性更改为{2}！", pkmn.name, abilityName(pkmn.ability)));   // :1201
            return true;
        }
        return false;
    }

    private boolean abilityPatch(Pokemon pkmn, ItemScene scene) {
        List<Object[]> abils = abilityList(pkmn);                               // :1370
        if (abils.size() <= 1 || pkmn.isSpecies("ZYGARDE")) {                   // :1372
            scene.pbDisplay("没有任何效果。");
            return false;
        }
        int current = abilityIndex(pkmn);
        int newIndex = -1;
        String newabilname = null;
        if (current < 2) {                                                      // :1377 currently a normal ability
            for (Object[] a : abils) {                                          // :1378-1384
                if ((int) a[1] > 1) {
                    newIndex = (int) a[1];
                    newabilname = abilityName((String) a[0]);
                    break;
                }
            }
            if (newabilname == null) {
                scene.pbDisplay("没有任何效果。");                                 // :1385-1388
                return false;
            }
        } else {                                                                // :1389 currently the hidden ability
            List<Object[]> abilArr = new ArrayList<>();                         // :1390-1397
            List<String> commands = new ArrayList<>();
            for (Object[] a : abils) {
                if (current != (int) a[1]) {
                    abilArr.add(a);
                    commands.add(abilityName((String) a[0]));
                }
            }
            int cmd;
            if (abilArr.size() > 1) {                                           // :1398
                cmd = scene.pbMessage("请选择要修改的特性。", commands, -1);          // :1399
                if (cmd == -1) {
                    return false;                                               // :1400
                }
            } else {
                cmd = 0;                                                        // :1402
            }
            newIndex = (int) abilArr.get(cmd)[1];                               // :1404
            newabilname = commands.get(cmd);                                    // :1405
        }
        if (scene.pbConfirm(fmt("你想要将{1}的特性改变为\n{2}吗？", pkmn.name, newabilname))) {   // :1407
            setAbility(pkmn, newIndex);                                         // :1408
            scene.pbRefresh();
            scene.pbDisplay(fmt("{1}的特性变为{2}了！", pkmn.name, newabilname));   // :1410
            return true;
        }
        return false;
    }

    // =====================================================================
    // forms and fusion
    // =====================================================================

    /** {@code pkmn.setForm(value) { block }} (198_Pokemon_Forms:17-24): the form is set, the block runs, the stats follow. */
    private void setForm(Pokemon pkmn, int value, Runnable block) {
        pkmn.recalculatingStats(() -> {
            pkmn.setForm(pbs, value);
            if (block != null) {
                block.run();
            }
        });
    }

    private boolean formChangeItem(String item, Pokemon pkmn, ItemScene scene) {
        Runnable changed = () -> {
            scene.pbRefresh();
            scene.pbDisplay(fmt("{1}改变了形态！", pkmn.name));
        };
        switch (item) {
            case "GRACIDEA":                                                    // :979-994
                if (!pkmn.isSpecies("SHAYMIN") || pkmn.formIndex() != 0 || "FROZEN".equals(pkmn.status)
                        || PBDayNight.isNight(clock.get())) {
                    scene.pbDisplay("无法生效。");
                    return false;
                }
                if (pkmn.hp <= 0) {                                             // :985 fainted?
                    scene.pbDisplay("不能用在晕倒的宝可梦上。");
                    return false;
                }
                setForm(pkmn, 1, changed);
                return true;
            case "REVEALGLASS":                                                 // :997-1015
                if (!pkmn.isSpecies("TORNADUS") && !pkmn.isSpecies("THUNDURUS") && !pkmn.isSpecies("LANDORUS")
                        && !pkmn.isSpecies("ENAMORUS")) {
                    scene.pbDisplay("无法生效。");
                    return false;
                }
                if (pkmn.hp <= 0) {
                    scene.pbDisplay("不能用在晕倒的宝可梦上。");
                    return false;
                }
                setForm(pkmn, pkmn.formIndex() == 0 ? 1 : 0, changed);
                return true;
            case "PRISONBOTTLE":                                                // :1017-1031
                if (!pkmn.isSpecies("HOOPA")) {
                    scene.pbDisplay("无法生效。");
                    return false;
                }
                if (pkmn.hp <= 0) {
                    scene.pbDisplay("不能用在晕倒的宝可梦上。");                    // :1023 (no `next false`: the form still changes)
                }
                setForm(pkmn, pkmn.formIndex() == 0 ? 1 : 0, changed);
                return true;
            default:
                return false;
        }
    }

    /**
     * The fusion items (DNASPLICERS :1033-1084, NSOLARIZER :1086-1133, NLUNARIZER :1135-1182, REINSOFUNITY
     * :1416-1467): they share one flow.
     *
     * @param blockedForm the form of the item's own species on which the item has no effect (-1 for none)
     * @param partners the species that can be fused in; the form to take for each is {@code forms}
     */
    private boolean fusionItem(Pokemon pkmn, ItemScene scene, String own, int blockedForm,
                               String[] partners, int[] forms) {
        if (!pkmn.isSpecies(own) || (blockedForm >= 0 && pkmn.formIndex() == blockedForm)) {
            scene.pbDisplay("无法生效。");
            return false;
        }
        if (pkmn.hp <= 0) {
            scene.pbDisplay("不能用在晕倒的宝可梦上。");
            return false;
        }
        if (pkmn.fused == null) {                                               // fusing
            int chosen = scene.pbChoosePokemon("与哪个宝可梦融合？");
            if (chosen < 0) {
                return false;
            }
            Pokemon poke2 = state.trainer().party.get(chosen);
            if (pkmn == poke2) {
                scene.pbDisplay("不能与自身融合。");
                return false;
            } else if (poke2.egg) {
                scene.pbDisplay("不能与蛋融合。");
                return false;
            } else if (poke2.hp <= 0) {
                scene.pbDisplay("不能与晕倒的宝可梦融合。");
                return false;
            }
            int newForm = -1;
            for (int i = 0; i < partners.length; i++) {
                if (poke2.isSpecies(partners[i])) {
                    newForm = forms[i];
                }
            }
            if (newForm < 0) {
                scene.pbDisplay("不能与宝可梦融合。");
                return false;
            }
            setForm(pkmn, newForm, () -> {
                pkmn.fused = poke2;
                state.trainer().party.remove(chosen);                           // pbRemovePokemonAt(chosen)
                scene.pbHardRefresh();
                scene.pbDisplay(fmt("{1}改变了形态！", pkmn.name));
            });
            return true;
        }
        if (state.trainer().party.size() >= 6) {                                // unfusing
            scene.pbDisplay("没有空间可供分离。");
            return false;
        }
        setForm(pkmn, 0, () -> {
            state.trainer().party.add(pkmn.fused);
            pkmn.fused = null;
            scene.pbHardRefresh();
            scene.pbDisplay(fmt("{1}改变了形态！", pkmn.name));
        });
        return true;
    }

    private boolean rotomCatalog(Pokemon pkmn, ItemScene scene) {
        if (!pkmn.isSpecies("ROTOM")) {                                         // :1293
            scene.pbDisplay("无法生效。");                                        // :1320
            return false;
        }
        if (pkmn.hp > 0) {                                                      // :1294
            scene.pbDisplay(fmt("目录中包含了 {1} 可以拥有的家电清单！", pkmn.name));   // :1295
            List<String> commands = Arrays.asList("灯泡", "微波炉", "洗衣机", "冰箱", "电风扇", "割草机", "取消");
            int cmd = scene.pbShowCommands("您想订购哪种家电？", commands, 0);      // :1297-1305
            if (cmd >= 0 && cmd < 6) {                                          // :1306
                scene.pbDisplay(fmt("{1}变身了！", pkmn.name));                    // :1307
                scene.pbRefresh();
                pkmn.recalculatingStats(() -> pkmn.setForm(pbs, cmd));          // :1309 pkmn.form = cmd
                scene.pbRefresh();
            } else {
                scene.pbDisplay("没有订购任何家电");                                // :1312
            }
            scene.pbRefresh();                                                  // :1314
            return true;                                                        // :1315
        }
        scene.pbDisplay("不能用在晕倒的宝可梦上。");                                 // :1317 (the proc then returns nil)
        return false;
    }

    private boolean zygardeCube(Pokemon pkmn, ItemScene scene) {
        if (!pkmn.isSpecies("ZYGARDE")) {                                       // :1470
            scene.pbDisplay("没有任何效果。");
            return false;
        }
        if (pkmn.hp <= 0) {                                                     // :1474
            scene.pbDisplay("无法对濒死的宝可梦使用。");
            return false;
        }
        int form = pkmn.formIndex();
        if (form == 4 || form == 5 || form == 6 || form == 7 || form == 8) {    // :1480-1484
            scene.pbDisplay("这个形态无法使用基格尔德多面体。");
            return false;
        }
        int cmd = scene.pbMessage("你想要做什么？", Arrays.asList("教学招式", "改变形态", "取消"), -1);   // :1486
        if (cmd == 0) {                                                         // :1490
            String[] choices = {"EXTREMESPEED", "THOUSANDARROWS", "DRAGONDANCE", "THOUSANDWAVES", "COREENFORCER"};
            List<String> displayChoices = new ArrayList<>();                    // :1492-1495
            for (String choice : choices) {
                PbsData.Move move = pbs.move(choice);
                displayChoices.add(move == null ? choice : moveName(move));
            }
            displayChoices.add("取消");
            if (displayChoices.size() > 1) {                                    // :1496
                int cmd2 = scene.pbMessage("想要让基格尔德学习哪一个招式？", displayChoices, 0);   // :1497
                if (cmd2 < displayChoices.size() - 1) {                         // :1498
                    pbLearnMove(pkmn, pbs.move(choices[cmd2]), false, false, scene);   // :1499
                    return true;                                                // :1500
                }
            } else {
                scene.pbDisplay("未发现任何核心");                                 // :1503
                return false;
            }
            return false;
        }
        if (cmd == 1) {                                                         // :1506
            int oldForm = form;                                                 // :1507
            List<String> forms = Arrays.asList("50%气场破坏", "10%气场破坏", "50%群聚变形", "10%群聚变形", "取消");
            int cmd2 = scene.pbMessage("想要变成哪一个形态？", forms, -1);          // :1510
            if (cmd2 == -1 || cmd2 == 4) {
                return false;                                                   // :1511
            }
            setForm(pkmn, cmd2, null);                                          // :1512 pkmn.form = cmd2
            if (pkmn.formIndex() != oldForm) {                                  // :1513
                scene.pbDisplay(fmt("{1}改变了形态！", pkmn.name));
                return true;
            }
            scene.pbDisplay(fmt("它已经是{1}形态了！", forms.get(cmd2)));          // :1517
            return false;
        }
        return false;                                                           // -1 / cancel
    }

    private boolean scroll(String item, Pokemon pkmn, ItemScene scene) {
        if (!pkmn.isSpecies("KUBFU")) {                                         // :1707, :1726
            scene.pbDisplay("没有任何效果。");
            return false;
        }
        PbsData.Species urshifu = pbs.species("URSHIFU");                       // :1711
        setForm(pkmn, "SCROLLOFWATERS".equals(item) ? 1 : 0, null);             // :1712, :1731
        if (urshifu != null) {
            scene.pbEvolution(pkmn, urshifu, null);                                   // :1713-1721
        }
        return true;
    }

    // =====================================================================
    // ItemHandlers::UseOnPokemon.trigger
    // =====================================================================

    private static int hpAmount(String item) {
        switch (item) {
            case "POTION": case "BERRYJUICE": return 20;                        // :426-431
            case "SWEETHEART": return 80;                                       // :433
            case "SUPERPOTION": return 60;                                      // :436
            case "HYPERPOTION": return 120;                                     // :440
            case "FRESHWATER": return 50;                                       // :448
            case "SODAPOP": return 60;                                          // :452
            case "LEMONADE": return 80;                                         // :456
            case "MOOMOOMILK": return 100;                                      // :460
            case "ORANBERRY": return 10;                                        // :464
            case "CIDER": return 50;                                            // :472
            case "MIXEDBEVERAGES": return 100;                                  // :476
            case "WWINE": return 150;                                           // :480
            default: return -1;
        }
    }

    private boolean statusCure(Pokemon pkmn, String status, String message, ItemScene scene) {
        if (!able(pkmn) || !statusIs(pkmn, status)) {
            scene.pbDisplay(NO_EFFECT);
            return false;
        }
        healStatus(pkmn);
        scene.pbRefresh();
        scene.pbDisplay(fmt(message, pkmn.name));
        return true;
    }

    private static boolean statusIs(Pokemon pkmn, String status) {
        String current = pkmn.status == null ? "" : pkmn.status.toUpperCase(Locale.ROOT);
        return current.equals(status) || ("FROZEN".equals(status) && current.equals("FREEZE"));
    }

    private static boolean hasStatus(Pokemon pkmn) {
        return pkmn.status != null && !pkmn.status.isEmpty();
    }

    /** {@code healStatus} (:752-756). */
    private static void healStatus(Pokemon pkmn) {
        if (pkmn.egg) {
            return;
        }
        pkmn.status = "";
        pkmn.statusCount = 0;
    }

    private boolean vitamin(Pokemon pkmn, int stat, int gain, boolean limit, String name, String method,
                            boolean refresh, ItemScene scene) {
        if (pbRaiseEffortValues(pkmn, stat, gain, limit) == 0) {
            scene.pbDisplay(NO_EFFECT);
            return false;
        }
        if (refresh) {
            scene.pbRefresh();
        }
        scene.pbDisplay(fmt("{1}的" + name + "增加了。", pkmn.name));
        changeHappiness(pkmn, method);
        return true;
    }

    private boolean ppRestore(Pokemon pkmn, ItemScene scene, boolean choose, boolean max, boolean all) {
        if (all) {                                                              // ELIXIR :695-706, MAXELIXIR :708-719
            int pprestored = 0;
            for (int i = 0; i < pkmn.moves.size; i++) {
                Pokemon.MoveSlot m = pkmn.moves.get(i);
                pprestored += pbRestorePP(pkmn, i, max ? m.totalPp() - m.pp : 10);
            }
            if (pprestored == 0) {
                scene.pbDisplay(NO_EFFECT);
                return false;
            }
            scene.pbDisplay("PP恢复了。");
            return true;
        }
        int move = scene.pbChooseMove(pkmn, "要回复哪个招式？");                  // :672, :685
        if (move < 0) {
            return false;
        }
        Pokemon.MoveSlot m = pkmn.moves.get(move);
        if (pbRestorePP(pkmn, move, max ? m.totalPp() - m.pp : 10) == 0) {
            scene.pbDisplay(NO_EFFECT);
            return false;
        }
        scene.pbDisplay("PP恢复了。");
        return true;
    }

    private boolean ppUp(Pokemon pkmn, ItemScene scene, boolean max) {
        int move = scene.pbChooseMove(pkmn, "增加哪招的PP？");                     // :722, :737
        if (move >= 0) {
            Pokemon.MoveSlot m = pkmn.moves.get(move);
            if (m.totalPp() <= 1 || m.ppUp >= 3) {
                scene.pbDisplay(NO_EFFECT);
                return false;
            }
            m.setPpUp(max ? 3 : m.ppUp + 1);                                    // :728, :743
            scene.pbDisplay(fmt("{1}的PP增加了。", moveName(m.move)));
            return true;
        }
        return false;
    }

    private boolean mint(String item, Pokemon pkmn, ItemScene scene) {
        String a = item.substring(0, item.length() - "MINT".length());          // :1328-1348 (LONELYMINT -> LONELY)
        PbsData.Nature target = pbs.nature(a);
        PbsData.Nature current = pkmn.nature;
        String currentName = current == null ? "" : current.internalName;
        String b = NEUTRAL_NATURES.contains(currentName) ? "SERIOUS" : currentName;      // :1349-1350
        if (b.equals(a)) {                                                      // :1351 (natureOverride is never set)
            scene.pbDisplay(NO_EFFECT);
            return false;
        }
        if (scene.pbConfirm(fmt("这可能会影响{1}性格，\n确定要使用它吗？", pkmn.name))) {   // :1355
            scene.pbDisplay(fmt("{1}的性格由于{2}而改变!", pkmn.name, itemName(item)));   // :1356
            pkmn.recalculatingStats(() -> pkmn.nature = target);                // :1358-1359 setNature; calcStats
            return true;
        }
        return false;
    }

    /** {@code ItemHandlers.triggerUseOnPokemon(item, pkmn, scene)} (188_PItem_Items:412-416). */
    public boolean useOnPokemon(String item, Pokemon pkmn, ItemScene scene) {
        if (!hasUseOnPokemon(item)) {
            return false;                                                       // :413 return false if !UseOnPokemon[item]
        }
        PbsData.Item data = pbs == null ? null : pbs.item(item);
        if (data != null && data.type == 7 && !USE_ON_POKEMON.contains(item)) {   // :400 the evolution stones
            String evolved = PBEvolution.checkEvolution(pkmn, item, world());   // :406 pbCheckEvolution
            PbsData.Species newspecies = evolved == null ? null : pbs.species(evolved);
            if (newspecies == null) {                                           // :407-409 newspecies<=0
                scene.pbDisplay(NO_EFFECT);
                return false;
            }
            scene.pbEvolution(pkmn, newspecies, item);                                // :411-415 (the scene applies it)
            scene.pbRefreshAnnotations(p -> PBEvolution.checkEvolution(p, item, world()) != null);   // :417
            scene.pbRefresh();                                                  // :418
            return true;
        }
        switch (item) {
            // ---- HP
            case "POTION": case "BERRYJUICE": case "SWEETHEART": case "SUPERPOTION": case "HYPERPOTION":
            case "FRESHWATER": case "SODAPOP": case "LEMONADE": case "MOOMOOMILK": case "ORANBERRY":
            case "CIDER": case "MIXEDBEVERAGES": case "WWINE":
                return pbHPItem(pkmn, hpAmount(item), scene);
            case "MAXPOTION":                                                   // :444-446
                return pbHPItem(pkmn, pkmn.maxHp() - pkmn.hp, scene);
            case "SITRUSBERRY":                                                 // :468-470
                return pbHPItem(pkmn, pkmn.maxHp() / 4, scene);
            // ---- status
            case "AWAKENING": case "CHESTOBERRY": case "BLUEFLUTE": case "POKEFLUTE":                  // :484-495
                return statusCure(pkmn, "SLEEP", "{1}醒来了！", scene);
            case "ANTIDOTE": case "PECHABERRY":                                                       // :497-508
                return statusCure(pkmn, "POISON", "{1}的毒被消去了！", scene);
            case "BURNHEAL": case "RAWSTBERRY":                                                       // :510-521
                return statusCure(pkmn, "BURN", "{1}的灼伤被治愈了！", scene);
            case "PARLYZHEAL": case "PARALYZEHEAL": case "CHERIBERRY":                                // :523-534
                return statusCure(pkmn, "PARALYSIS", "{1}的麻痹被解除了！", scene);
            case "ICEHEAL": case "ASPEARBERRY":                                                       // :536-547
                return statusCure(pkmn, "FROZEN", "{1}不再被冰冻了", scene);
            case "FULLHEAL": case "LAVACOOKIE": case "OLDGATEAU": case "CASTELIACONE": case "LUMIOSEGALETTE":
            case "SHALOURSABLE": case "BIGMALASADA": case "LUMBERRY": case "XIANGSHAWLPILL":           // :549-575
                if (!able(pkmn) || !hasStatus(pkmn)) {
                    scene.pbDisplay(NO_EFFECT);
                    return false;
                }
                healStatus(pkmn);
                scene.pbRefresh();
                scene.pbDisplay(fmt("{1}恢复健康了。", pkmn.name));
                return true;
            case "FULLRESTORE": {                                                                     // :577-591
                if (!able(pkmn) || (pkmn.hp == pkmn.maxHp() && !hasStatus(pkmn))) {
                    scene.pbDisplay(NO_EFFECT);
                    return false;
                }
                int hpgain = pbItemRestoreHP(pkmn, pkmn.maxHp() - pkmn.hp);
                healStatus(pkmn);
                scene.pbRefresh();
                if (hpgain > 0) {
                    scene.pbDisplay(fmt("{1}的HP恢复了{2}点。", pkmn.name, hpgain));
                } else {
                    scene.pbDisplay(fmt("{1}恢复健康了。", pkmn.name));
                }
                return true;
            }
            // ---- revive
            case "REVIVE": case "MAXREVIVE": case "EVEBURGER": case "REVIVALHERB":                    // :593-669
                if (pkmn.egg || pkmn.hp > 0) {                                                        // !fainted?
                    scene.pbDisplay(NO_EFFECT);
                    return false;
                }
                if ("REVIVE".equals(item)) {
                    pkmn.hp = pkmn.maxHp() / 2;                                                       // :598
                    if (pkmn.hp <= 0) {
                        pkmn.hp = 1;                                                                  // :599
                    }
                } else {
                    pkmn.hp = pkmn.maxHp();                                                           // healHP
                }
                healStatus(pkmn);
                if ("REVIVALHERB".equals(item)) {
                    changeHappiness(pkmn, "revivalherb");                                             // :665
                }
                scene.pbRefresh();
                scene.pbDisplay(fmt("{1}的HP回复了。", pkmn.name));
                return true;
            case "ENERGYPOWDER":                                                                      // :630-636
                if (pbHPItem(pkmn, 60, scene)) {
                    changeHappiness(pkmn, "powder");
                    return true;
                }
                return false;
            case "ENERGYROOT":                                                                        // :638-644
                if (pbHPItem(pkmn, 200, scene)) {
                    changeHappiness(pkmn, "energyroot");
                    return true;
                }
                return false;
            case "HEALPOWDER":                                                                        // :646-656
                if (!able(pkmn) || !hasStatus(pkmn)) {
                    scene.pbDisplay(NO_EFFECT);
                    return false;
                }
                healStatus(pkmn);
                changeHappiness(pkmn, "powder");
                scene.pbRefresh();
                scene.pbDisplay(fmt("{1}恢复健康了。", pkmn.name));
                return true;
            // ---- PP
            case "ETHER": case "LEPPABERRY":
                return ppRestore(pkmn, scene, true, false, false);                                    // :671-682
            case "MAXETHER":
                return ppRestore(pkmn, scene, true, true, false);                                     // :684-693
            case "ELIXIR":
                return ppRestore(pkmn, scene, false, false, true);                                    // :695-706
            case "MAXELIXIR":
                return ppRestore(pkmn, scene, false, true, true);                                     // :708-719
            case "PPUP":
                return ppUp(pkmn, scene, false);                                                      // :721-734
            case "PPMAX":
                return ppUp(pkmn, scene, true);                                                       // :736-749
            // ---- vitamins and wings
            case "HPUP":
                return vitamin(pkmn, PokemonStats.HP, 10, true, "HP", "vitamin", true, scene);        // :751-760
            case "PROTEIN":
                return vitamin(pkmn, PokemonStats.ATTACK, 10, true, "攻击", "vitamin", false, scene);   // :762
            case "IRON":
                return vitamin(pkmn, PokemonStats.DEFENSE, 10, true, "防御", "vitamin", false, scene);  // :772
            case "CALCIUM":
                return vitamin(pkmn, PokemonStats.SPATK, 10, true, "特攻", "vitamin", false, scene);    // :782
            case "ZINC":
                return vitamin(pkmn, PokemonStats.SPDEF, 10, true, "特防", "vitamin", false, scene);    // :792
            case "CARBOS":
                return vitamin(pkmn, PokemonStats.SPEED, 10, true, "速度", "vitamin", false, scene);    // :802
            case "HEALTHWING":
                return vitamin(pkmn, PokemonStats.HP, 1, false, "HP", "wing", true, scene);           // :812-821
            case "MUSCLEWING":
                return vitamin(pkmn, PokemonStats.ATTACK, 1, false, "攻击", "wing", false, scene);     // :823
            case "RESISTWING":
                return vitamin(pkmn, PokemonStats.DEFENSE, 1, false, "防御", "wing", false, scene);    // :833
            case "GENIUSWING":
                return vitamin(pkmn, PokemonStats.SPATK, 1, false, "特攻", "wing", false, scene);      // :843
            case "CLEVERWING":
                return vitamin(pkmn, PokemonStats.SPDEF, 1, false, "特防", "wing", false, scene);      // :853
            case "SWIFTWING":
                return vitamin(pkmn, PokemonStats.SPEED, 1, false, "速度", "wing", false, scene);      // :863
            // ---- levels and experience
            case "RARECANDY":
                return rareCandy(item, pkmn, scene);                                                  // :874-925
            case "EXPCANDYXS": case "EXPCANDYS": case "EXPCANDYM": case "EXPCANDYL": case "EXPCANDYXL":
                return expCandy(item, pkmn, scene);                                                   // :1209-1289
            case "EXPPOT":
                return expPot(pkmn, scene);                                                           // :1744-1819
            // ---- EV berries
            case "HOPOBERRY":
                return pbEmptyAllEV(pkmn, scene, fmt("{1}的所有努力值都清空了！", pkmn.name));            // :927
            case "POMEGBERRY":
                return pbRaiseHappinessAndLowerEV(pkmn, scene, PokemonStats.HP, new String[] {
                        fmt("{1}十分喜欢你！\n基础HP降低了。", pkmn.name),
                        fmt("{1}更加喜欢你了。\n基础HP不能再降低了。", pkmn.name),
                        fmt("{1}更加喜欢你了。\n但是基础HP降低了。", pkmn.name)});                       // :931-937
            case "KELPSYBERRY":
                return evBerry(pkmn, scene, PokemonStats.ATTACK, "攻击");                              // :939
            case "QUALOTBERRY":
                return evBerry(pkmn, scene, PokemonStats.DEFENSE, "防御");                             // :947
            case "HONDEWBERRY":
                return evBerry(pkmn, scene, PokemonStats.SPATK, "特攻");                               // :955
            case "GREPABERRY":
                return evBerry(pkmn, scene, PokemonStats.SPDEF, "特防");                               // :963
            case "TAMATOBERRY":
                return evBerry(pkmn, scene, PokemonStats.SPEED, "速度");                               // :971
            // ---- forms, abilities, natures
            case "GRACIDEA": case "REVEALGLASS": case "PRISONBOTTLE":
                return formChangeItem(item, pkmn, scene);                                             // :979-1031
            case "DNASPLICERS":                                                                       // :1033-1084
                return fusionItem(pkmn, scene, "KYUREM", -1, new String[] {"RESHIRAM", "ZEKROM"}, new int[] {1, 2});
            case "NSOLARIZER":                                                                        // :1086-1133
                return fusionItem(pkmn, scene, "NECROZMA", 2, new String[] {"SOLGALEO"}, new int[] {1});
            case "NLUNARIZER":                                                                        // :1135-1182
                return fusionItem(pkmn, scene, "NECROZMA", 1, new String[] {"LUNALA"}, new int[] {2});
            case "REINSOFUNITY":                                                                      // :1416-1467
                return fusionItem(pkmn, scene, "CALYREX", -1, new String[] {"GLASTRIER", "SPECTRIER"}, new int[] {1, 2});
            case "ABILITYCAPSULE":
                return abilityCapsule(pkmn, scene);                                                   // :1184-1206
            case "ABILITYPATCH":
                return abilityPatch(pkmn, scene);                                                     // :1369-1414
            case "ROTOMCATALOG":
                return rotomCatalog(pkmn, scene);                                                     // :1292-1323
            case "ZYGARDECUBE":
                return zygardeCube(pkmn, scene);                                                      // :1469-1521
            case "SCROLLOFWATERS": case "SCROLLOFDARKNESS":
                return scroll(item, pkmn, scene);                                                     // :1706-1742
            default:
                break;
        }
        if (MINTS.contains(item)) {
            return mint(item, pkmn, scene);                                                           // :1327-1366
        }
        return false;
    }

    private boolean evBerry(Pokemon pkmn, ItemScene scene, int stat, String name) {
        return pbRaiseHappinessAndLowerEV(pkmn, scene, stat, new String[] {
                fmt("{1}十分喜欢你！\n" + name + "降低了。", pkmn.name),
                fmt("{1}更加喜欢你了。\n" + name + "不能再降低了。", pkmn.name),
                fmt("{1}更加喜欢你了。\n但是" + name + "降低了。", pkmn.name)});
    }
}
