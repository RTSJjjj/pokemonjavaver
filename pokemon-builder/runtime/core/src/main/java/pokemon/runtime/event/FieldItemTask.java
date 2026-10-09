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
    private final MapPort port;
    private final TaskFieldScene scene;
    private final Services services;
    private final Random random;

    FieldItemTask(GameState state, MapPort port, TaskFieldScene scene, Services services, Random random) {
        this.state = state;
        this.port = port;
        this.scene = scene;
        this.services = services;
        this.random = random;
    }

    /** {@code ItemHandlers.triggerUseInField(item)}: 0 not used, 1 used. */
    int use(String item) {
        switch (item) {
            case "SUPERROD":
                return superRod();
            default:
                return 0;
        }
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
