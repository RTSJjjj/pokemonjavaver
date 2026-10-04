package pokemon.runtime.map;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import java.io.File;

/** AssetManager owns all textures; large tilesets use tile-aligned texture pages. */
public final class TextureRepository implements Disposable {
    public static final int PAGE_HEIGHT = 2048;
    private final AssetManager assets = new AssetManager(new AbsoluteFileHandleResolver());
    /**
     * R6.24: page count per tileset file. Without it, loading a tileset that was
     * already in memory decoded the whole PNG again just to measure it.
     */
    private final com.badlogic.gdx.utils.ObjectMap<String, Integer> tilesetPages =
            new com.badlogic.gdx.utils.ObjectMap<>();
    public TextureRepository() {
        assets.setLoader(Texture.class, ".tilepage", new PageLoader());
    }
    public Texture load(String key, File file) {
        String path = file.getAbsolutePath();
        if (!assets.isLoaded(path, Texture.class)) {
            assets.load(path, Texture.class);
            assets.finishLoadingAsset(path);
        }
        return nearest(assets.get(path, Texture.class));
    }
    public Texture[] loadTileset(File file) {
        String path = file.getAbsolutePath();
        Integer known = tilesetPages.get(path);
        if (known != null && allPagesLoaded(path, known)) {
            Texture[] cached = new Texture[known];
            for (int i = 0; i < known; i++) {
                cached[i] = nearest(assets.get(path + "#" + i + ".tilepage", Texture.class));
            }
            return cached;
        }
        Pixmap source = new Pixmap(new FileHandle(file));
        int width = source.getWidth(), height = source.getHeight();
        if (width != 256 || height < TilesetGeometry.TILE_SIZE) {
            source.dispose();
            throw new IllegalArgumentException("Invalid RMXP tileset: " + file + " " + width + "x" + height);
        }
        // Only complete tile rows are addressable. Some source tilesets include
        // trailing pixels (Gen4 interior: 25000px = 781 rows + 8px).
        // Ignore that remainder instead of rejecting the entire map.
        height -= height % TilesetGeometry.TILE_SIZE;
        Texture[] pages = new Texture[(height + PAGE_HEIGHT - 1) / PAGE_HEIGHT];
        tilesetPages.put(path, pages.length);
        for (int i = 0; i < pages.length; i++) {
            String key = path + "#" + i + ".tilepage";
            if (!assets.isLoaded(key)) {
                PageParameters p = new PageParameters(file, i * PAGE_HEIGHT,
                        Math.min(PAGE_HEIGHT, height - i * PAGE_HEIGHT));
                // R6.24: every page used to decode the whole source PNG again;
                // hand the already decoded source over for this batch only.
                p.sharedSource = source;
                assets.load(key, Texture.class, p);
                assets.finishLoadingAsset(key);
                p.sharedSource = null; // GL-loss recovery re-reads the file
            }
            pages[i] = nearest(assets.get(key, Texture.class));
        }
        source.dispose();
        return pages;
    }

    private boolean allPagesLoaded(String path, int count) {
        for (int i = 0; i < count; i++) {
            if (!assets.isLoaded(path + "#" + i + ".tilepage", Texture.class)) {
                return false;
            }
        }
        return true;
    }
    private Texture nearest(Texture texture) {
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }
    @Override public void dispose() { assets.dispose(); }
    private static final class PageParameters extends AssetLoaderParameters<Texture> {
        final File file;
        final int y, height;
        /** Transient: only set while the loading batch still holds the source. */
        Pixmap sharedSource;
        PageParameters(File file, int y, int height) { this.file = file; this.y = y; this.height = height; }
    }
    private static final class PageLoader extends SynchronousAssetLoader<Texture, PageParameters> {
        PageLoader() { super(new AbsoluteFileHandleResolver()); }
        @Override public Texture load(AssetManager manager, String name, FileHandle file, PageParameters p) {
            return new Texture(new PageData(p));
        }
        @Override public Array<AssetDescriptor> getDependencies(String name, FileHandle file, PageParameters p) {
            return null;
        }
    }
    /** Reloads pixels on GL context loss; never retains the full source bitmap. */
    private static final class PageData implements TextureData {
        final PageParameters p;
        Pixmap pixels;
        PageData(PageParameters p) { this.p = p; }
        @Override public TextureDataType getType() { return TextureDataType.Pixmap; }
        @Override public boolean isPrepared() { return pixels != null; }
        @Override public void prepare() {
            if (isPrepared()) throw new IllegalStateException("Texture page already prepared");
            Pixmap source = p.sharedSource != null ? p.sharedSource
                    : new Pixmap(new FileHandle(p.file));
            try {
                pixels = new Pixmap(256, p.height, Pixmap.Format.RGBA8888);
                pixels.setBlending(Pixmap.Blending.None);
                pixels.drawPixmap(source, 0, 0, 0, p.y, 256, p.height);
            } finally {
                if (source != p.sharedSource) { source.dispose(); }
            }
        }
        @Override public Pixmap consumePixmap() {
            if (!isPrepared()) throw new IllegalStateException("Texture page not prepared");
            Pixmap result = pixels; pixels = null; return result;
        }
        @Override public boolean disposePixmap() { return true; }
        @Override public void consumeCustomData(int target) { throw new UnsupportedOperationException(); }
        @Override public int getWidth() { return 256; }
        @Override public int getHeight() { return p.height; }
        @Override public Pixmap.Format getFormat() { return Pixmap.Format.RGBA8888; }
        @Override public boolean useMipMaps() { return false; }
        @Override public boolean isManaged() { return true; }
    }
}
