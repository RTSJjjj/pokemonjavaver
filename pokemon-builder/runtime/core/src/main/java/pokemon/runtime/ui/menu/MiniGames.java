package pokemon.runtime.ui.menu;

import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.event.MenuService;

/** The mini-games an event can open ({@link MenuService.Kind#MINIGAME}), by name. */
final class MiniGames {
    private MiniGames() {
    }

    /** @return the screen, or null for a name the runtime does not know */
    static MiniGame create(RuntimeContext context, String name, MenuService.Request request) {
        if (name == null) return null;
        switch (name) {
            case "mining": return new MiningView(context);                  // 239_PMinigame_Mining:610 pbMiningGame
            case "voltorbflip": return new VoltorbFlipView(context);        // 237_PMinigame_VoltorbFlip pbVoltorbFlip
            case "triadbuy": return new TriadShopView(context, true);       // 235_PMinigame_TripleTriad:1065 pbBuyTriads
            case "triadsell": return new TriadShopView(context, false);     // :1159 pbSellTriads
            default: return null;
        }
    }

    /** The project's message font at another size ({@code pbSetSystemFont} + {@code bitmap.font.size}). */
    static MenuFont font(RuntimeContext context, int size) {
        String name = context.database().project().runtime.messageFont;
        java.io.File file = name == null ? null : new pokemon.runtime.map.GraphicsLocator(context.database()).font(name);
        return new MenuFont(file, size);
    }
}
