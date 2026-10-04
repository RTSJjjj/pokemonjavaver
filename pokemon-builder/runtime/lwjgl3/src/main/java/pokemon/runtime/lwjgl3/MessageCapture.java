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
import pokemon.runtime.event.EventInterpreter;
import pokemon.runtime.event.MessageService;
import pokemon.runtime.event.PictureService;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.map.EventPages;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.map.MapCamera;
import pokemon.runtime.map.MapRenderer;
import pokemon.runtime.map.TextureRepository;
import pokemon.runtime.map.TileMap;
import pokemon.runtime.state.GameState;
import pokemon.runtime.ui.MessageWindow;
import pokemon.runtime.ui.PictureLayer;

import java.io.File;

/**
 * R6.2 evidence: runs one real map event through the production interpreter,
 * map renderer and message window in a hidden window, then writes a PNG.
 * Arguments: dataRoot mapId eventId output.png [confirms]
 *
 * <p>{@code confirms} taps the confirm button that many times before the
 * capture, so a later page (for example a choice window) can be photographed.</p>
 */
public final class MessageCapture extends ApplicationAdapter {

    private final String[] args;
    private Throwable failure;

    private MessageCapture(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            throw new IllegalArgumentException("dataRoot mapId eventId output.png [confirms]");
        }
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setInitialVisible(false);
        config.setWindowedMode(640, 480);
        config.disableAudio(true);
        MessageCapture capture = new MessageCapture(args);
        new Lwjgl3Application(capture, config);
        if (capture.failure != null) {
            throw new RuntimeException("message capture failed", capture.failure);
        }
    }

    @Override
    public void create() {
        GameDatabase database = null;
        TextureRepository textures = null;
        SpriteBatch batch = null;
        FrameBuffer buffer = null;
        MessageWindow window = null;
        try {
            int width = pokemon.runtime.app.ScreenMetrics.LOGICAL_WIDTH;
            int height = pokemon.runtime.app.ScreenMetrics.LOGICAL_HEIGHT;
            database = GameDatabase.load(args[0], new File("."));
            MapData data = database.map(Integer.parseInt(args[1]));
            int eventId = Integer.parseInt(args[2]);
            TileMap map = new TileMap(data, database.tileset(data.tilesetId));
            GameState state = new GameState();
            state.enterMap(data.mapId, database.startX(), database.startY());
            GraphicsLocator locator = new GraphicsLocator(database);
            textures = new TextureRepository();
            MapRenderer renderer = new MapRenderer(map, textures, locator, state);

            MessageService messages = new MessageService();
            PictureService pictures = new PictureService();
            InputManager input = new InputManager();
            EventInterpreter interpreter = new EventInterpreter(state, messages, input,
                    null, id -> null, null, pictures, message -> System.out.println("[interpreter] " + message));
            MapData.EventData event = null;
            for (MapData.EventData candidate : data.events) {
                if (candidate.id == eventId) {
                    event = candidate;
                }
            }
            if (event == null) {
                throw new IllegalArgumentException("event " + eventId + " is not on map " + data.mapId);
            }
            MapData.EventPageData page = EventPages.resolve(state, data.mapId, event);
            if (page == null || page.commands.size == 0) {
                throw new IllegalStateException("event " + eventId + " has no active page to run");
            }
            interpreter.start(page.commands, data.mapId, event.id);
            interpreter.update(0f); // first frame shows the first message page
            int confirms = args.length > 4 ? Integer.parseInt(args[4]) : 0;
            for (int i = 0; i < confirms; i++) {
                input.beginFrame();
                input.press(pokemon.runtime.input.GameAction.CONFIRM);
                interpreter.update(0f);
                input.endFrame();
                input.release(pokemon.runtime.input.GameAction.CONFIRM);
            }
            interpreter.update(1f);

            MapCamera camera = new MapCamera(map.width(), map.height(), width, height);
            camera.centerOn(event.x, event.y);
            OrthographicCamera projection = new OrthographicCamera(width, height);
            projection.position.set(camera.originX() + width / 2f, camera.originY() + height / 2f, 0f);
            projection.update();

            batch = new SpriteBatch();
            batch.setProjectionMatrix(projection.combined);
            buffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
            buffer.begin();
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            renderer.render(batch, camera);

            PictureLayer pictureLayer = new PictureLayer(pictures, textures, locator);
            batch.begin();
            pictureLayer.render(batch, camera.originX(), camera.originY(), height);
            batch.end();

            window = new MessageWindow(messages, locator.font("FusionPixelMonoPatched.ttf"),
                    textures, locator);
            if (!window.ready()) {
                throw new IllegalStateException("message window could not load the project font");
            }
            batch.begin();
            window.render(batch, camera.originX(), camera.originY(), width, height);
            batch.end();

            int error = Gdx.gl.glGetError();
            if (error != GL20.GL_NO_ERROR) {
                throw new IllegalStateException("OpenGL error: " + error);
            }
            Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, width, height);
            try {
                PixmapIO.writePNG(Gdx.files.absolute(new File(args[3]).getAbsolutePath()), pixels, -1, true);
            } finally {
                pixels.dispose();
            }
            buffer.end();
            System.out.println("MESSAGE OK map=" + data.mapId + " event=" + eventId
                    + " speaker=" + messages.speaker() + " lines=" + messages.lines().size
                    + " pictures=" + pictures.size()
                    + " output=" + args[3]);
        } catch (Throwable error) {
            failure = error;
            error.printStackTrace();
        } finally {
            if (window != null) {
                window.dispose();
            }
            if (buffer != null) {
                buffer.dispose();
            }
            if (batch != null) {
                batch.dispose();
            }
            if (textures != null) {
                textures.dispose();
            }
            if (database != null) {
                database.dispose();
            }
            Gdx.app.exit();
        }
    }
}
