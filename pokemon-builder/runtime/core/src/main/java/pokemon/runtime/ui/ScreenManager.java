package pokemon.runtime.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import pokemon.runtime.app.RuntimeContext;

/**
 * Screen manager (project3 section 8): the placeholder screen the runtime
 * shows until the map runtime (R3) provides a real MapScreen.
 *
 * <p>Screen switching itself (ScreenManager as a dispatcher) arrives with the
 * event interpreter (R6); this class only proves the render loop works
 * end to end.</p>
 */
public final class ScreenManager extends ScreenAdapter {

    private final RuntimeContext context;

    public ScreenManager(RuntimeContext context) {
        this.context = context;
    }

    @Override
    public void render(float delta) {
        // Deliberately empty: no per frame allocations, no texture creation
        // (project3 section 81).
    }

    @Override
    public void resize(int width, int height) {
        // TODO(R11): FitViewport handling lands with the desktop runtime.
    }

    @Override
    public void dispose() {
        Gdx.app.log("ScreenManager", "placeholder screen disposed");
    }

    public RuntimeContext context() {
        return context;
    }
}