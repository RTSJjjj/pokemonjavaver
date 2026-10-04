package pokemon.runtime.android.legacy;

import com.badlogic.gdx.Gdx;

import pokemon.runtime.input.KeyStateSource;

/**
 * Key state of the legacy placeholder (project3 sections 14, 50-51): the same
 * libGDX-polling adapter the modern backend uses, so the shared
 * {@code InputManager} keeps working unchanged. The real legacy touch layer is
 * a later stage and will feed the same GameAction abstraction.
 */
public final class LegacyKeyStateSource implements KeyStateSource {

    @Override
    public boolean isDown(int keyCode) {
        return Gdx.input != null && Gdx.input.isKeyPressed(keyCode);
    }
}
