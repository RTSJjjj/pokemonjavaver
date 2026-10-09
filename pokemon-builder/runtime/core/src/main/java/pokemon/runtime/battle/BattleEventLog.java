package pokemon.runtime.battle;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Diagnostics: appends what the engine produced for each round (the events the screen will play, and both sides'
 * stat stages) to {@code ~/pokemon-runtime-battle.log}, so a report like "the Attack boost never showed" can be
 * compared with what the engine actually emitted. Stops writing once the file passes 2 MB.
 */
final class BattleEventLog {
    private static final long LIMIT = 2L * 1024 * 1024;

    private BattleEventLog() {
    }

    static void round(Battle battle, com.badlogic.gdx.utils.Array<Battle.RoundEvent> events) {
        try {
            File file = new File(System.getProperty("user.home", "."), "pokemon-runtime-battle.log");
            if (file.length() > LIMIT) {
                return;
            }
            try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
                out.println("=== " + new Date() + " turn " + battle.turns());
                for (int i = 0; i < 2; i++) {
                    Battler b = battle.battlerAt(i);
                    if (b == null || b.pokemon == null) continue;
                    out.println("  battler " + i + " " + b.pokemon.name + " ability=" + b.ability + " hp=" + b.hp
                            + " stages atk=" + b.stage(PBStats.ATTACK) + " def=" + b.stage(PBStats.DEFENSE)
                            + " spe=" + b.stage(PBStats.SPEED));
                }
                for (Battle.RoundEvent event : events) {
                    out.println("  " + event.kind + (event.text == null ? "" : " | " + event.text.replace('\n', ' ')));
                }
            }
        } catch (Exception ignored) {
            // diagnostics only
        }
    }
}
