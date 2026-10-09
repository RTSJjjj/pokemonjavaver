package pokemon.runtime.field;

import pokemon.runtime.pokemon.BallTypes;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.FieldGlobals;
import pokemon.runtime.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * What the field does when the player takes a step: {@code pbOnStepTaken} (170_PField_Field:356-377) with the
 * {@code Events.onStepTaken} handlers that matter for walking (walking happiness :262-269, the repel countdown
 * 189_PItem_ItemEffects:162-188) and {@code pbBattleOnStepTaken} (:488-516), plus the encounter roll when the
 * player turns on the spot ({@code Events.onChangeDirection}, :381-384).
 *
 * <p>The screen owns everything that is drawn or played; this class changes the state and answers what the screen
 * has to do next: the wild Pokemon to battle and the messages to show.</p>
 *
 * <p>登记: ①the poison damage handler (:274-312), the Day Care / Pokerus / Shadow Pokemon / Follower / auto-save step
 * handlers and the Alcremie spin tracker are separate roadmap items; ②when a repel runs out while another is in the
 * bag the plugin asks whether to use one (189_PItem_ItemEffects:170-186): that needs item use (roadmap stage 5), so
 * only the "effect wore off" message is shown for now; ③safari zone and follower auto-toggle are not part of the game.</p>
 */
public final class FieldSteps {
    /** 169_PBTerrain: Ice = 12, TallGrass = 10. */
    private static final int TERRAIN_ICE = 12;
    private static final int TERRAIN_TALL_GRASS = 10;

    /** What a step asks the screen to do. */
    public static final class Result {
        /** The wild Pokemon to battle: none, one, or two (a double battle). */
        public final List<PokemonEncounters.Encounter> wild = new ArrayList<>();
        /** Messages to show, in order ({@code pbMessage}). */
        public final List<String> messages = new ArrayList<>();

        public boolean isEmpty() {
            return wild.isEmpty() && messages.isEmpty();
        }
    }

    private final PbsData pbs;
    private final GameState state;
    private final PokemonEncounters encounters;
    private final Random random;
    /** {@code $PokemonTemp.forceSingleBattle}. */
    private boolean forceSingleBattle;

    public FieldSteps(PbsData pbs, GameState state, PokemonEncounters encounters, Random random) {
        this.pbs = pbs;
        this.state = state;
        this.encounters = encounters;
        this.random = random;
    }

    public void forceSingleBattle(boolean value) {
        this.forceSingleBattle = value;
    }

    private FieldGlobals globals() {
        return state.fieldGlobals();
    }

    /** {@code $Trainer.ablePokemonCount}: party members that are not eggs and have HP left. */
    public int ablePokemonCount() {
        int count = 0;
        for (Pokemon pokemon : state.trainer().party.members()) {
            if (pokemon != null && !pokemon.egg && pokemon.hp > 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * {@code pbOnStepTaken(eventTriggered)} (:356-377).
     *
     * @param eventTriggered an event started on this step (:372 {@code !eventTriggered})
     * @param forced a move route is forcing the player or the map interpreter runs: only field movement is handled
     *               ({@code Events.onStepTakenFieldMovement}) and the counters do not move (:357-360)
     * @param terrainTag {@code pbGetTerrainTag($game_player)}: the tag under the player, bridge not counted
     * @param inMenu {@code $game_temp.in_menu}
     */
    public Result onStepTaken(boolean eventTriggered, boolean forced, int terrainTag, boolean inMenu) {
        Result result = new Result();
        if (forced) {
            return result;                                                   // :357-360
        }
        FieldGlobals g = globals();
        g.stepcount += 1;                                                    // :365 (stepcount = 0 if nil)
        g.stepcount &= 0x7FFFFFFF;                                           // :366
        boolean repel = g.infRepel || g.repel > 0;                           // :367
        // Events.onStepTaken (:371): walking happiness, then the repel countdown.
        walkingHappiness();                                                  // :262-269
        repelCountdown(terrainTag, result);                                  // 189_PItem_ItemEffects:162-188
        if (!eventTriggered && !inMenu) {                                    // :376
            battleOnStepTaken(repel, terrainTag, result);
        }
        return result;
    }

    /** {@code Events.onChangeDirection} (:381-384): a wild encounter can start while turning on the spot. */
    public Result onChangeDirection(int terrainTag, boolean inMenu) {
        Result result = new Result();
        boolean repel = globals().infRepel || globals().repel > 0;           // :382
        if (!inMenu) {
            battleOnStepTaken(repel, terrainTag, result);                    // :383
        }
        return result;
    }

    /** :262-269 every 128 steps each able party Pokemon has a 1 in 2 chance of gaining walking happiness. */
    private void walkingHappiness() {
        FieldGlobals g = globals();
        g.happinessSteps += 1;                                               // :264
        if (g.happinessSteps >= 128) {                                       // :265
            int luxury = pbs == null ? -1 : BallTypes.ballType(pbs, "LUXURYBALL");
            for (Pokemon pkmn : state.trainer().party.members()) {           // :266 ablePokemonParty
                if (pkmn != null && !pkmn.egg && pkmn.hp > 0 && random.nextInt(2) == 0) {
                    pkmn.changeHappiness("walking", state.currentMapId(), luxury);
                }
            }
            g.happinessSteps = 0;                                            // :269
        }
    }

    /** 189_PItem_ItemEffects:162-188: the repel counts down, except on ice. */
    private void repelCountdown(int terrainTag, Result result) {
        FieldGlobals g = globals();
        if (g.repel > 0 && terrainTag != TERRAIN_ICE) {                      // :163-164
            g.repel -= 1;                                                    // :165
            if (g.repel <= 0) {                                              // :166
                // :167-186 with a spare repel in the bag the plugin asks to use one (登记, see the class comment);
                // without one (:187) it only says the effect wore off.
                result.messages.add("使用的喷雾剂失去效果了！");
            }
        }
    }

    /** {@code pbBattleOnStepTaken(repel)} (:488-516). */
    private void battleOnStepTaken(boolean repel, int terrainTag, Result result) {
        if (ablePokemonCount() == 0) {
            return;                                                          // :489
        }
        String encounterType = encounters.pbEncounterType();                 // :490
        if (encounterType == null) {
            return;                                                          // :491 return if encounterType < 0
        }
        if (!encounters.isEncounterPossibleHere(terrainTag)) {
            return;                                                          // :492
        }
        PokemonEncounters.Encounter encounter = encounters.pbGenerateEncounter(encounterType);   // :494
        // :495 EncounterModifier.trigger(encounter): no modifiers are registered (see PokemonEncounters).
        if (encounters.pbCanEncounter(encounter, repel)) {                   // :496
            boolean partner = state.partner() != null;
            if (!forceSingleBattle                                           // :497
                    && (partner                                               // $PokemonGlobal.partner
                    || (ablePokemonCount() > 1 && terrainTag == TERRAIN_TALL_GRASS    // isDoubleWildBattle?
                    && random.nextInt(100) < 30))) {
                PokemonEncounters.Encounter encounter2 = encounters.pbEncounteredPokemon(encounterType, 1);   // :500
                if (encounter2 != null) {
                    result.wild.add(encounter);                              // :502 pbDoubleWildBattle
                    result.wild.add(encounter2);
                } else {
                    result.wild.add(encounter);
                }
            } else {
                result.wild.add(encounter);                                  // :504 pbWildBattle
            }
        }
        forceSingleBattle = false;                                           // :509
    }
}
