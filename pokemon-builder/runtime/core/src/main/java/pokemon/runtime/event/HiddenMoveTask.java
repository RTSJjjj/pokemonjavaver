package pokemon.runtime.event;

import pokemon.runtime.field.HiddenMoves;
import pokemon.runtime.field.PBTerrain;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;
import pokemon.runtime.state.ScreenWeather;

import java.util.Random;

/**
 * 179_PField_FieldMoves: the {@code HiddenMoveHandlers::UseMove} bodies, run as blocking Ruby on a
 * {@link pokemon.runtime.field.BlockingTask} (they show lines, play the banner, wait for animations and fades).
 * Every step that touches the map or starts a battle is an action the interpreter runs on its own thread.
 */
final class HiddenMoveTask {

    /** What the interpreter does for the task besides messages: encounters. */
    interface Services {
        /** {@code pbEncounter(enctype)} (175_PField_Encounters:484-499): true when a battle started. */
        boolean pbEncounter(String enctype);
    }

    private final GameState state;
    private final PbsData pbs;
    private final MapPort port;
    private final TaskFieldScene scene;
    private final Services services;
    private final Random random;
    private final pokemon.runtime.audio.AudioManager audio;

    HiddenMoveTask(GameState state, PbsData pbs, MapPort port, TaskFieldScene scene, Services services, Random random,
                   pokemon.runtime.audio.AudioManager audio) {
        this.state = state;
        this.pbs = pbs;
        this.port = port;
        this.scene = scene;
        this.services = services;
        this.random = random;
        this.audio = audio;
    }

    private String moveName(String move) {
        PbsData.Move data = pbs == null ? null : pbs.move(move);
        return data == null || data.name == null ? move : data.name;
    }

    /** {@code pbHiddenMoveAnimation(pokemon)}: true when the banner played (no "used" line then). */
    private boolean banner(Pokemon pkmn) {
        float[] duration = {0f};
        scene.runAction(() -> {
            duration[0] = port.hiddenMoveAnimation(pkmn);
            return duration[0];
        });
        return duration[0] > 0f;
    }

    /** The "{1}使用{2}！" line the plugin shows when there was no banner. */
    private void usedLine(Pokemon pkmn, String move) {
        scene.pbMessage(pkmn.name + "使用" + moveName(move) + "！");
    }

    /** {@code pbEncounter(enctype)} on the interpreter's thread. */
    private boolean encounter(String enctype) {
        boolean[] started = {false};
        scene.runAction(() -> {
            started[0] = services.pbEncounter(enctype);
            return 0f;
        });
        return started[0];
    }

    /** {@code HiddenMoveHandlers.triggerUseMove(move, pokemon)}: whether the move was used. */
    boolean use(String move, Pokemon pkmn) {
        switch (move) {
            case "CUT": {                                                           // :220-229
                if (!banner(pkmn)) usedLine(pkmn, move);
                smash(true);
                return true;
            }
            case "ROCKSMASH": {                                                     // :635-645
                if (!banner(pkmn)) usedLine(pkmn, move);
                if (port.facingEventName() != null) {
                    smash(false);
                    if (random.nextInt(100) < 25) {                                 // :604-608 pbRockSmashRandomEncounter
                        encounter("RockSmash");
                    }
                }
                return true;
            }
            case "STRENGTH": {                                                      // :688-695
                if (!banner(pkmn)) scene.pbMessage(pkmn.name + "使用" + moveName(move) + "！\u0001");
                scene.pbMessage(pkmn.name + "的怪力\n现在可以移动石头了。");
                state.pokemonMapStrengthUsed(true);
                return true;
            }
            case "SURF": {                                                          // :796-806
                new pokemon.runtime.field.Vehicles(pbs, state).cancelVehicles(null);   // :798 pbCancelVehicles
                if (!banner(pkmn)) usedLine(pkmn, move);
                scene.runAction(() -> {
                    new pokemon.runtime.field.Vehicles(pbs, state).cancelVehicles(null);   // :804 pbStartSurfing -> pbCancelVehicles
                    port.startSurfing();
                    return 0f;
                });
                return true;
            }
            case "WATERFALL": {                                                     // :992-998
                if (!banner(pkmn)) usedLine(pkmn, move);
                scene.runAction(port::ascendWaterfall);
                return true;
            }
            case "DEFOG": {                                                         // :1012-1018
                if (!banner(pkmn)) usedLine(pkmn, move);
                state.weather().set(ScreenWeather.NONE, 0, 0);                      // :1004 $game_screen.weather(0,0,0)
                return true;
            }
            case "SWEETSCENT": {                                                    // :851-857
                if (!banner(pkmn)) usedLine(pkmn, move);
                sweetScent();
                return true;
            }
            case "HEADBUTT": {                                                      // :591-597
                if (!banner(pkmn)) usedLine(pkmn, move);
                headbuttEffect();
                return false;                                                       // the plugin's UseMove returns pbHeadbuttEffect's value (nil)
            }
            case "TELEPORT": {                                                      // :894-912
                int[] healing = HiddenMoves.healingSpot(pbs, state);
                if (healing == null) return false;
                if (!banner(pkmn)) usedLine(pkmn, move);
                state.fieldGlobals().escapePoint = new int[0];                      // :910 pbEraseEscapePoint
                int[] target = {healing[0], healing[1], healing[2]};
                scene.runAction(() -> {
                    port.transferThroughFade(target[0], target[1], target[2], 2, false);
                    return 0f;
                });
                return true;
            }
            case "DIG": {                                                           // :275-294
                int[] escape = state.fieldGlobals().escapePoint;
                if (escape.length == 0) return false;
                if (!banner(pkmn)) usedLine(pkmn, move);
                state.fieldGlobals().escapePoint = new int[0];                      // :290 pbEraseEscapePoint
                scene.runAction(() -> {
                    port.transferThroughFade(escape[0], escape[1], escape[2], escape[3], false);
                    return 0f;
                });
                return true;
            }
            case "DIVE": {                                                          // :431-461
                boolean wasDiving = state.fieldGlobals().diving;
                int divemap;
                if (wasDiving) {
                    divemap = HiddenMoves.divemapFor(pbs, state.currentMapId());     // :434-440
                } else {
                    PbsData.Metadata meta = pbs == null ? null : pbs.mapMetadata(state.currentMapId());
                    divemap = meta == null ? -1 : meta.diveMap;                      // :442
                }
                if (divemap < 0) return false;                                      // :444
                if (!banner(pkmn)) usedLine(pkmn, move);
                scene.runAction(() -> {
                    state.fieldGlobals().surfing = wasDiving;                       // :453
                    state.fieldGlobals().diving = !wasDiving;                       // :454
                    port.transferThroughFade(divemap, state.playerX(), state.playerY(), state.playerDirection(), true);
                    return 0f;
                });
                return true;
            }
            case "FLY": {                                                           // :515-538
                int[] fly = state.flyData();
                if (fly == null || contains(HiddenMoves.BAN_MAPS, state.currentMapId())) {
                    scene.pbMessage("不能在这里使用。");                              // :518 (the plugin's `showmsg` is nil here: it raises)
                    return false;
                }
                if (!banner(pkmn)) usedLine(pkmn, move);
                scene.runAction(() -> port.flyAnimation(true));                     // :524 pbFlyAnimation
                state.fieldGlobals().escapePoint = new int[0];                      // :536 pbEraseEscapePoint (before the swap: the screen is rebuilt)
                state.flyData(null);                                                // :530
                scene.runAction(() -> {
                    state.flyArrival(true);                                         // :535 pbFlyAnimation(false) after the fade
                    port.transferThroughFade(fly[0], fly[1], fly[2], 2, false);     // :526-531 direction 2
                    return 0f;
                });
                return true;
            }
            case "CHATTER": {                                                       // 202_Pokemon_Chatter:35-38
                scene.runAction(() -> {
                    if (audio != null && pkmn.species != null) {
                        audio.playCry(pkmn.species.id);                             // pbPlayCry(pokemon, 90, 100)
                    }
                    return 40f / 40f;                                               // 40.times { Graphics.update }
                });
                return true;
            }
            case "FLASH": {                                                         // :479-495
                if (!port.darknessActive()) return false;                           // :481 next false if !darkness
                if (!banner(pkmn)) usedLine(pkmn, move);
                state.fieldGlobals().flashUsed = true;                              // :485
                scene.runAction(port::flashDarkness);
                return true;
            }
            default:
                return false;
        }
    }

    private static boolean contains(int[] list, int value) {
        for (int entry : list) {
            if (entry == value) {
                return true;
            }
        }
        return false;
    }

    /** {@code pbSmashEvent(facingEvent)} (:231-248): the SE, then the shake and the removal. */
    private void smash(boolean cut) {
        if (port.facingEventName() == null) {
            return;
        }
        scene.pbSEPlay(cut ? "Cut" : "Rock Smash", 80);                             // :233-234
        scene.runAction(port::smashFacingEvent);
    }

    /** {@code pbSweetScent} (:813-845). */
    private void sweetScent() {
        if (state.weather().type() != ScreenWeather.NONE) {
            scene.pbMessage("因为某些原因，香气散去了……");                              // :814-816
            return;
        }
        scene.runAction(port::sweetScentFlash);                                      // :818-839
        boolean[] started = {false};
        scene.runAction(() -> {
            started[0] = services.pbEncounter("*");                                 // pbEncounterType + isEncounterPossibleHere?
            return 0f;
        });
        if (!started[0]) {
            scene.pbMessage("这里好像什么都没有……");                                    // :843
        }
    }

    /** {@code pbHeadbuttEffect} (:545-563). */
    private void headbuttEffect() {
        int[] position = port.facingEventPosition();
        if (position == null) {
            return;
        }
        int x = position[0];
        int y = position[1];
        int a = (x + (x / 24) + 1) * (y + (y / 24) + 1);                            // :547
        a = (a * 2 / 5) % 10;                                                       // :548
        int b = state.trainer().publicID() % 10;                                    // :549
        int chance = 1;                                                             // :550
        if (a == b) {
            chance = 8;                                                             // :551
        } else if (a > b && Math.abs(a - b) < 5) {
            chance = 5;                                                             // :552
        } else if (a < b && Math.abs(a - b) > 5) {
            chance = 5;                                                             // :553
        }
        if (random.nextInt(10) >= chance) {
            scene.pbMessage("不。\n什么都没有……");                                      // :556
        } else {
            String enctype = chance == 1 ? "HeadbuttLow" : "HeadbuttHigh";         // :558
            if (!encounter(enctype)) {
                scene.pbMessage("不。\n什么都没有……");                                  // :560
            }
        }
    }
}
