package pokemon.runtime.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import pokemon.runtime.data.GameDatabase;
import pokemon.runtime.data.MapData;
import pokemon.runtime.map.*;
import pokemon.runtime.state.GameState;
import java.io.File;

/** Reproducible real-OpenGL capture using the production MapRenderer, without a visible window.
 * Arguments: dataRoot mapId output.png [elapsedSeconds] [viewport]. */
public final class MapRenderCapture extends ApplicationAdapter {
    private final String[] args;
    private Throwable failure;
    private MapRenderCapture(String[] args) { this.args = args; }
    public static void main(String[] args) {
        if (args.length < 3) throw new IllegalArgumentException("dataRoot mapId output.png [seconds] [viewport]");
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(640, 480);
        config.disableAudio(true);
        MapRenderCapture capture = new MapRenderCapture(args);
        new Lwjgl3Application(capture, config);
        if (capture.failure != null) throw new RuntimeException("Map capture failed", capture.failure);
    }
    @Override public void create() {
        GameDatabase database = null;
        TextureRepository textures = null;
        SpriteBatch batch = null;
        FrameBuffer buffer = null;
        try {
            database = GameDatabase.load(args[0], new File("."));
            MapData data = database.map(Integer.parseInt(args[1]));
            TileMap map = new TileMap(data, database.tileset(data.tilesetId));
            boolean viewport = args.length > 4 && args[4].equals("viewport");
            int width = viewport ? pokemon.runtime.app.ScreenMetrics.LOGICAL_WIDTH : map.width() * 32;
            int height = viewport ? pokemon.runtime.app.ScreenMetrics.LOGICAL_HEIGHT : map.height() * 32;
            textures = new TextureRepository();
            // R5: the production renderer needs live state; a capture is a fresh
            // game on this map (all switches off, all variables 0).
            GameState state = new GameState();
            state.enterMap(data.mapId, database.startX(), database.startY());
            MapRenderer renderer = new MapRenderer(map, textures, new GraphicsLocator(database), state);
            renderer.update(args.length > 3 ? Float.parseFloat(args[3]) : 0);
            MapCamera camera = new MapCamera(map.width(), map.height(), width, height);
            camera.centerOn(database.startX(), database.startY());
            OrthographicCamera projection = new OrthographicCamera(width, height);
            projection.position.set(camera.originX() + width / 2f, camera.originY() + height / 2f, 0);
            projection.update();
            batch = new SpriteBatch();
            batch.setProjectionMatrix(projection.combined);
            buffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
            buffer.begin();
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            renderer.render(batch, camera);
            int error = Gdx.gl.glGetError();
            if (error != GL20.GL_NO_ERROR) throw new IllegalStateException("OpenGL error: " + error);
            Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, width, height);
            try { PixmapIO.writePNG(Gdx.files.absolute(new File(args[2]).getAbsolutePath()), pixels, -1, true); }
            finally { pixels.dispose(); }
            buffer.end();
            System.out.println("CAPTURE OK map=" + data.mapId + " size=" + width + "x" + height + " output=" + args[2]);
        } catch (Throwable error) { failure = error; error.printStackTrace(); }
        finally {
            if (buffer != null) buffer.dispose();
            if (batch != null) batch.dispose();
            if (textures != null) textures.dispose();
            if (database != null) database.dispose();
            Gdx.app.exit();
        }
    }
}
