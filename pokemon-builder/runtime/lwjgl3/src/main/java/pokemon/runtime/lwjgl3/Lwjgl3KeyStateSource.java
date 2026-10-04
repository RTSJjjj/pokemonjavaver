package pokemon.runtime.lwjgl3;

import com.badlogic.gdx.Gdx;
import pokemon.runtime.input.KeyStateSource;

/**
 * Desktop key state: libGDX polls the physical keys, the runtime never sees a
 * raw key outside the input package (project3 sections 14-15).
 */
public final class Lwjgl3KeyStateSource implements KeyStateSource {

    @Override
    public boolean isDown(int keyCode) {
        return Gdx.input != null && Gdx.input.isKeyPressed(keyCode);
    }
}