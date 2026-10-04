package pokemon.runtime.android;

import com.badlogic.gdx.Gdx;

import pokemon.runtime.input.KeyStateSource;

/**
 * Key state of the modern Android backend (project3 sections 14, 51): the same
 * libGDX-polling adapter as the desktop build, so a physical keyboard (or the
 * emulator) drives the game during the R13 stage. Touch controls are the
 * separate touch task (section 51); they will feed the same
 * {@code pokemon.runtime.input.InputManager} through the {@code GameAction}
 * abstraction instead of adding a second input path.
 */
public final class AndroidKeyStateSource implements KeyStateSource {

    @Override
    public boolean isDown(int keyCode) {
        return Gdx.input != null && Gdx.input.isKeyPressed(keyCode);
    }
}
