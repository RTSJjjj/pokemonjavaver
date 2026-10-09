package pokemon.runtime.event;

import pokemon.runtime.field.PBTerrain;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.util.Random;

/**
 * 189_PItem_ItemEffects {@code ItemHandlers::UseInField} bodies of the items that act on the map (fishing rod ...), run as
 * blocking Ruby on a {@link pokemon.runtime.field.BlockingTask} after the bag and the pause menu have closed
 * ({@code pbUseKeyItemInField}).
 */
final class FieldItemTask {

    /** What the interpreter does for the task besides messages: encounters. */
    interface Services {
        /** {@code $PokemonEncounters.hasEncounter?(enctype)}: the map has a table for the method. */
        boolean hasEncounter(String enctype);

        /** {@code pbEncounter(enctype)} (175_PField_Encounters:484-499): true when a battle started. */
        boolean pbEncounter(String enctype);
    }

    /** 189:331 {@code pbFishing(encounter, 3)}. */
    private static final int SUPER_ROD_TYPE = 3;
    /** 000_Settings:351 {@code EXCLAMATION_ANIMATION_ID}. */
    private static final int EXCLAMATION_ANIMATION_ID = 3;
    private static final String[] ROD_TYPES = {"OldRod", "GoodRod", "SuperRod"};

    private final GameState state;
    private final pokemon.runtime.pokemon.PbsData pbs;
    private final MapPort port;
    private final TaskFieldScene scene;
    private final Services services;
    private final Random random;

    FieldItemTask(GameState state, pokemon.runtime.pokemon.PbsData pbs, MapPort port, TaskFieldScene scene, Services services,
                  Random random) {
        this.state = state;
        this.pbs = pbs;
        this.port = port;
        this.scene = scene;
        this.services = services;
        this.random = random;
    }

    /** {@code ItemHandlers.triggerUseInField(item)}: 0 not used, 1 used, 3 used and consumed. */
    int use(String item) {
        switch (item) {
            case "SUPERROD":
                return superRod();
            case "ESCAPEROPE":
                return escapeRope(item, 3);                                            // :213-240 next 3
            case "INFINITEROPE":
                return escapeRope(item, 1);                                            // :242-268 next 1
            case "LANTERN":
                return lantern();
            case "EONFLUTE":
                return eonFlute();
            case "ETHEREALNEXUS":
                return etherealNexus();
            default:
                return 0;
        }
    }

    /** {@code pbUseItemMessage(item)} (188_PItem_Items:984-991). */
    private void useItemMessage(String item) {
        pokemon.runtime.pokemon.PbsData.Item data = pbs == null ? null : pbs.item(item);
        scene.pbMessage("使用了" + (data == null || data.name == null ? item : data.name) + "。");
    }

    /** 189_PItem_ItemEffects:213-268: the maps the ropes refuse, and the way back to the cave mouth. */
    private static final int[] ROPE_BANNED_MAPS = {60, 226, 207, 321, 322, 209, 210};

    /** {@code UseInField :ESCAPEROPE / :INFINITEROPE}: back to the escape point set by the cave entrance. */
    private int escapeRope(String item, int consumed) {
        int[] escape = state.fieldGlobals().escapePoint;
        if (escape == null || escape.length < 4) {
            scene.pbMessage("这里不能使用。");                                            // :215-218
            return 0;
        }
        for (int map : ROPE_BANNED_MAPS) {
            if (state.currentMapId() == map) {
                scene.pbMessage("这里不能使用。");                                        // :219-222
                return 0;
            }
        }
        boolean[] partnered = {false};
        scene.runAction(() -> {
            partnered[0] = port.hasDependentEvents();
            return 0f;
        });
        if (partnered[0]) {
            scene.pbMessage("与他人同行时不能使用。");                                      // :223-226
            return 0;
        }
        useItemMessage(item);                                                          // :227
        final int[] target = escape.clone();
        scene.runAction(() -> {
            new pokemon.runtime.field.Vehicles(pbs, state).cancelVehicles(null);       // :234 pbCancelVehicles
            port.transferThroughFade(target[0], target[1], target[2], target[3], false);   // :229-237 pbFadeOutIn { transfer_player }
            return 0f;
        });
        state.fieldGlobals().escapePoint = new int[0];                                 // :239 pbEraseEscapePoint
        return consumed;
    }

    /** {@code UseInField :EONFLUTE} (189:1602-1646): the flute calls Latios, Latias or a flying Groudon to carry the player to a visited place. */
    private int eonFlute() {
        boolean[] flags = {false, false};                                              // {partnered, outdoor}
        scene.runAction(() -> {
            flags[0] = port.hasDependentEvents();
            flags[1] = port.currentMapOutdoor();
            return 0f;
        });
        if (flags[0]) {
            scene.pbMessage("与他人同行时不能使用。");                                      // :1603-1606
            return 0;
        }
        scene.pbMessage("\\me[无限之笛]" + state.trainer().name + "吹响了无限之笛。\\wtnp[10]");   // :1607
        if (!flags[1] || pokemon.runtime.field.ItemHandlers.banMap(state.currentMapId())) {
            scene.pbMessage("似乎没有宝可梦听到笛声。");                                    // :1608-1611
            return 0;
        }
        scene.pbMessage("宝可梦听到笛声了！");                                             // :1612
        int[] destination = scene.chooseFlyDestination();                              // :1613-1616 pbStartFlyScreen
        if (destination == null || destination.length < 3) {
            return 0;                                                                  // :1617 next false if !ret
        }
        int roll = random.nextInt(100);                                                // :1620-1630 10 % Groudon, 45 % Latios, 45 % Latias
        String species = roll < 10 ? "GROUDON" : roll < 55 ? "LATIOS" : "LATIAS";
        Pokemon bird = pbs == null || pbs.species(species) == null ? null
                : pokemon.runtime.pokemon.WildGenerator.pbNewPkmn(pbs, pbs.species(species), 50, state.trainer(),
                        state.currentMapId(), random);
        if (bird != null && "GROUDON".equals(species)) {
            bird.setForm(pbs, 2);                                                      // :1623 the flying Groudon
        }
        boolean[] banner = {false};
        scene.runAction(() -> {
            float duration = bird == null ? 0f : port.hiddenMoveAnimation(bird);       // :1632 pbHiddenMoveAnimation(pkmn)
            banner[0] = duration > 0f;
            return duration;
        });
        if (!banner[0]) {
            scene.pbMessage(state.trainer().name + "召唤的宝可梦使用了飞翔！");              // :1633-1635
        }
        scene.runAction(() -> port.flyAnimation(true, species));                       // :1636 pbFlyAnimation(true, nil, :EONFLUTE, species)
        state.fieldGlobals().escapePoint = new int[0];                                 // :1646 pbEraseEscapePoint (before the swap: the screen is rebuilt)
        final int[] to = destination.clone();
        scene.runAction(() -> {
            state.flyArrival(true);                                                    // :1645 pbFlyAnimation(false, ...) after the fade
            state.flyArrivalBird(species);
            port.transferThroughFade(to[0], to[1], to[2], 2, false);                   // :1637-1644 direction 2
            return 0f;
        });
        return 1;
    }

    /** {@code UseInField :ETHEREALNEXUS} (189:1662-1673): the list of places, then the warp (the plugin's own black fade). */
    private int etherealNexus() {
        boolean[] partnered = {false};
        scene.runAction(() -> {
            partnered[0] = port.hasDependentEvents();
            return 0f;
        });
        if (partnered[0]) {
            scene.pbMessage("与他人同行时不能使用。");                                      // :1663-1666
            return 0;
        }
        if (pokemon.runtime.field.ItemHandlers.banMap(state.currentMapId())) {
            scene.pbMessage("无法在这里使用");                                            // :1668-1671
            return 0;
        }
        int[] place = scene.chooseDimensionWarp();                                     // :1672 dimensionality_warp
        if (place == null || place.length < 3) {
            return 1;
        }
        final int[] to = place.clone();
        scene.runAction(() -> {
            port.transferThroughFade(to[0], to[1], to[2], 2, false);                   // 363:355-399 pbWarpWithFade(map, x, y, 2)
            return 0f;
        });
        return 1;
    }

    /** {@code UseInField :LANTERN} (:1528-1552): lights a dark cave like Flash does. */
    private int lantern() {
        pokemon.runtime.field.HiddenMoves.Check badge = pokemon.runtime.field.HiddenMoves.badgeCheck(state,
                pokemon.runtime.field.FieldMoves.BADGE_FOR_FLASH);
        if (!badge.ok) {
            if (badge.message != null) scene.pbMessage(badge.message);                  // :1529 pbCheckHiddenMoveBadge(..., true)
            return 0;
        }
        boolean[] dark = {false};
        scene.runAction(() -> {
            dark[0] = port.darknessActive();
            return 0f;
        });
        if (!dark[0]) {
            scene.pbMessage("不能在这里使用。");                                          // :1530-1533 the map is not a dark map
            return 0;
        }
        if (state.fieldGlobals().flashUsed) {
            scene.pbMessage("这里已经被照亮了。");                                        // :1534-1537
            return 0;
        }
        scene.pbMessage("拿出了提灯！");                                                 // :1540
        state.fieldGlobals().flashUsed = true;                                         // :1541
        scene.runAction(port::flashDarkness);                                          // :1542-1550 the light grows to its full radius
        return 1;
    }

    /** {@code UseInField :SUPERROD} (:318-337). */
    private int superRod() {
        boolean[] ok = {false};
        scene.runAction(() -> {
            ok[0] = pokemon.runtime.field.ItemHandlers.canFish(PBTerrain.isWater(port.facingTerrainTag()), port.facingPassable(), state.fieldGlobals().surfing);
            return 0f;
        });
        if (!ok[0]) {
            scene.pbMessage("这里不能使用。");                                         // :322
            return 0;
        }
        String chosenRod = ROD_TYPES[random.nextInt(ROD_TYPES.length)];                // :326-327 rodTypes.sample
        boolean[] has = {false};
        scene.runAction(() -> {
            has[0] = services.hasEncounter(chosenRod);                                 // :328
            return 0f;
        });
        if (pbFishing(has[0], SUPER_ROD_TYPE)) {                                       // :329
            scene.runAction(() -> {
                services.pbEncounter(chosenRod);                                       // :330
                return 0f;
            });
        }
        return 1;
    }

    /** {@code pbFishingBegin} / {@code pbFishingEnd}: the player's four fishing frames, two 40 fps frames each (:1232-1269). */
    private void fishingFrames(boolean ending) {
        boolean surfing = state.fieldGlobals().surfing;
        int[] direction = {2};
        scene.runAction(() -> {
            direction[0] = new int[] {2, 4, 6, 8}[Math.max(0, Math.min(3, port.playerFullPattern() / 4))];
            return 0f;
        });
        int patternb = ending ? 2 * (direction[0] - 2) : 2 * direction[0] - 1;         // :1255 / :1236
        for (int k = 0; k < 4; k++) {
            final int pattern = ending ? patternb + k : patternb - k;
            boolean[] shown = {false};
            scene.runAction(() -> {
                shown[0] = port.fishingFrame(surfing, pattern);
                return shown[0] ? 2f / 40f : 0f;                                       // (40/20).times { Graphics.update ... }
            });
            if (!shown[0]) {
                return;                                                                // meta[num] missing: no animation at all
            }
        }
    }

    /** {@code pbFishing(hasEncounter, rodType)} (:1271-1311): true when a Pokemon bites. */
    private boolean pbFishing(boolean hasEncounter, int rodType) {
        Pokemon first = state.trainer().party.first();
        boolean speedup = first != null && ("STICKYHOLD".equals(first.ability) || "SUCTIONCUPS".equals(first.ability));   // :1272-1274
        double biteChance = 20 + 25 * rodType;                                         // :1275 45, 70, 95
        if (speedup) biteChance *= 1.5;                                                // :1276
        int[] oldPattern = {0};
        scene.runAction(() -> {
            oldPattern[0] = port.playerFullPattern();                                  // :1277 oldpattern = fullPattern
            return 0f;
        });
        fishingFrames(false);                                                          // :1278 pbFishingBegin
        int time = 2 + random.nextInt(4);                                              // :1284
        if (speedup) time = Math.min(time, 2 + random.nextInt(4));                     // :1285
        boolean bite = false;
        if (waitMessage(time)) {                                                       // :1291-1296 cancelled
            finish(oldPattern[0]);
            scene.pbMessage("没有任何动静……");
        } else if (hasEncounter && random.nextInt(100) < biteChance) {                 // :1298
            int[] at = new int[2];
            scene.runAction(() -> {
                at[0] = state.playerX();
                at[1] = state.playerY();
                port.showTileAnimation(EXCLAMATION_ANIMATION_ID, at[0], at[1], 3);     // :1299 addUserAnimation
                return 0f;
            });
            finish(oldPattern[0]);
            scene.pbMessage("宝可梦上钩了！");                                           // :1302
            bite = true;
        } else {
            finish(oldPattern[0]);
            scene.pbMessage("没有任何动静……");                                           // :1306
        }
        return bite;
    }

    private void finish(int oldPattern) {
        fishingFrames(true);                                                           // pbFishingEnd
        scene.runAction(() -> {
            port.endFishingFrame(oldPattern);                                          // :1294 setDefaultCharName(nil, oldpattern)
            return 0f;
        });
    }

    /** {@code pbWaitMessage(msgWindow, time)} (:1315-1331): dots every 0.4 s; true when C or B ended the wait. */
    private boolean waitMessage(int time) {
        StringBuilder message = new StringBuilder();
        int periodTime = 40 * 4 / 10;                                                  // 16 frames
        for (int i = 0; i <= time; i++) {
            if (i > 0) message.append(".   ");
            scene.flashMessage(message.toString());
            if (scene.waitCancelable(periodTime)) {
                return true;
            }
        }
        return false;
    }
}
