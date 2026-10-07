package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R17: {@code pbCommandMenu}'s labels (Scene_Commands:8-14). The fourth entry
 * is dynamic, and this project ships no translation for the two English
 * literals it uses.
 */
class CommandMenuLabelTest {

    @Test
    @DisplayName("the four entries are 战斗/背包/宝可梦 and the dynamic fourth")
    void labels() {
        assertArrayEquals(new String[] { "战斗", "背包", "宝可梦", "Run" },
                BattleScreen.commandLabels(false, true));
        assertArrayEquals(new String[] { "战斗", "背包", "宝可梦", "Cancel" },
                BattleScreen.commandLabels(false, false),
                "a later action of the round shows Cancel (:17)");
        assertArrayEquals(new String[] { "战斗", "背包", "宝可梦", "呼唤" },
                BattleScreen.commandLabels(true, true),
                "a Shadow trainer battle shows 呼唤 (:13, :16)");
        assertArrayEquals(new String[] { "战斗", "背包", "宝可梦", "呼唤" },
                BattleScreen.commandLabels(true, false),
                "and 呼唤 wins over Cancel in a Shadow battle");
    }

    @Test
    @DisplayName("the labels are the plugin's literals, not the Safari Zone's 逃跑")
    void runLabelIsThePluginLiteral() {
        String[] labels = BattleScreen.commandLabels(false, true);
        assertEquals("Run", labels[3]);
        assertNotEquals("逃跑", labels[3],
                "逃跑 belongs to the Safari Zone menu (PokeBattle_SafariZone:259); "
                        + "Scene_Commands:13 uses the English literal and Data/intl.dat "
                        + "has no translation for it");
    }
}
