package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.ui.WindowSkin;

/** A full-screen mini-game the pause overlay hosts for an event ({@code MenuService.Kind.MINIGAME}). */
public interface MiniGame {
    /** @return true when the whole screen is over (its fades included) */
    boolean update(InputManager input);

    void render(SpriteBatch batch, MenuAssets assets, MenuFont font, WindowSkin skin);

    default void dispose() {
    }

    /** True when the game is drawn over the frozen map (no pause-menu backdrop), like a shop's windows. */
    default boolean overMap() {
        return false;
    }

    /** How far the frozen map has scrolled to the right ({@code pbScrollMap}), in pixels. */
    default float mapShift() {
        return 0f;
    }
}
