package pokemon.runtime.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.ui.WindowSkin;

import java.io.File;

/**
 * L1: textures the menus need, loaded from the project's Graphics tree through
 * the same case-insensitive {@link GraphicsLocator} the map uses. Missing files
 * degrade to null (the views skip them) instead of failing the game.
 */
public final class MenuAssets implements Disposable {

    private final GraphicsLocator locator;
    private final ObjectMap<String, Texture> textures = new ObjectMap<>();
    private final ObjectMap<String, WindowSkin> skins = new ObjectMap<>();
    private final ObjectMap<String, Boolean> missing = new ObjectMap<>();

    public MenuAssets(GraphicsLocator locator) {
        this.locator = locator;
    }

    /** Graphics/Pictures/MPM/&lt;name&gt;.png (pause menu background, icons, ...). */
    public Texture mpm(String name) {
        return load("mpm:" + name, "Pictures/MPM", name + ".png");
    }
    public Texture graphic(String directory, String name) {
        return load(directory + ":" + name, directory, name + ".png");
    }

    /**
     * {@code BitmapCache.load_bitmap(path, hue)} (BitmapCache:399-413): a copy of
     * {@code Graphics/<directory>/<name>.png} with {@code Bitmap#hue_change(hue)}
     * applied; {@code hue == 0} is the plain cached bitmap.
     */
    public Texture graphicHue(String directory, String name, int hue) {
        if (hue == 0) {
            return graphic(directory, name);
        }
        String key = directory + ":" + name + ":hue" + hue;
        Texture cached = textures.get(key);
        if (cached != null) {
            return cached;
        }
        if (missing.containsKey(key)) {
            return null;
        }
        File found = locator == null ? null : locator.find(directory, name + ".png");
        if (found == null || !found.isFile()) {
            missing.put(key, true);
            return null;
        }
        try {
            Pixmap pixmap = new Pixmap(pokemon.runtime.data.ResourceCrypto.handle(found));
            HueShift.apply(pixmap, hue);
            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            textures.put(key, texture);
            return texture;
        } catch (RuntimeException error) {
            missing.put(key, true);
            return null;
        }
    }

    /**
     * A field-cache handle for one {@code Graphics/&lt;directory&gt;/&lt;name&gt;.png}:
     *
     * <pre>{@code
     * private final MenuAssets.TextureRef overlayHp = assets.ref("Pictures/Battle", "overlay_hp");
     * ...
     * Texture hpBar = overlayHp.texture();   // a field read after the first call
     * }</pre>
     *
     * <p>Perf pass: {@code BattleScreen} calls
     * {@code assets.graphic("Pictures/Battle", ...)} inside its per-frame render
     * methods (:2938 hp bar, :2953 exp bar, :2988 status icons, :3064 message
     * overlay, :3113 command overlay, :3136 fight overlay, :3210 icon numbers),
     * and every one of those builds a fresh {@code directory + ":" + name} key
     * string and hashes it in the {@code ObjectMap}. Holding a handle in a field
     * makes the steady state one field read.</p>
     *
     * <p>Resolution is lazy and goes through the same {@link #graphic} cache, so
     * the texture, the missing-file fallback ({@code null}) and {@link #dispose()}
     * behave exactly as before.</p>
     */
    public TextureRef ref(String directory, String name) {
        return new TextureRef(this, directory, name);
    }

    /** See {@link MenuAssets#ref(String, String)}. */
    public static final class TextureRef {
        private final MenuAssets assets;
        private final String directory;
        private final String name;
        private Texture texture;
        private boolean resolved;

        private TextureRef(MenuAssets assets, String directory, String name) {
            this.assets = assets;
            this.directory = directory;
            this.name = name;
        }

        /** The texture; resolved once, then a plain field read. */
        public Texture texture() {
            if (!resolved) {
                texture = assets.graphic(directory, name);
                resolved = true;
            }
            return texture;
        }

        /** Forgets the resolved value; the next {@link #texture()} looks it up again. */
        public void refresh() {
            resolved = false;
        }
    }

    /** Graphics/Characters/&lt;name&gt;.png (walking charsets / 004's trainer sprite). */
    public Texture character(String name) {
        return load("char:" + name, "Characters", name + ".png");
    }

    private final ObjectMap<String, Texture> itemIcons = new ObjectMap<>();

    /**
     * The resolved icon of an item ({@link ItemIcons}), remembered: the lookup builds several candidate names (a
     * {@code String.format} among them) and the bag asks for every visible row on every frame.
     */
    public Texture itemIcon(String id, java.util.function.Supplier<Texture> resolver) {
        if (itemIcons.containsKey(id)) {
            return itemIcons.get(id);
        }
        Texture texture = resolver.get();
        itemIcons.put(id, texture);
        return texture;
    }

    /** Graphics/Icons/&lt;name&gt;.png (PokemonIconSprite strips). */
    public Texture icon(String name) {
        return load("icon:" + name, "Icons", name + ".png");
    }

    /** Graphics/Titles/&lt;name&gt;.png (splash slides, start prompt). */
    public Texture title(String name) {
        return load("title:" + name, "Titles", name + ".png");
    }

    /**
     * Graphics/Titles/ModularTS/[folder/]&lt;name&gt;.png (L14 title visuals:
     * Backgrounds / Overlays / Particles and the root logo1..3/start).
     */
    public Texture modular(String folder, String name) {
        String directory = folder == null || folder.isEmpty() ? "Titles/ModularTS" : "Titles/ModularTS/" + folder;
        return load("modular:" + folder + ":" + name, directory, name + ".png");
    }

    /** 1x1 white pixel for full-screen fades (L14 intro). */
    public Texture pixel() {
        Texture cached = textures.get("pixel");
        if (cached != null) {
            return cached;
        }
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        textures.put("pixel", texture);
        return texture;
    }

    /** A windowskin by name (the System windowskin for menu windows). */
    public WindowSkin skin(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        WindowSkin cached = skins.get(name);
        if (cached != null) {
            return cached;
        }
        if (missing.containsKey("skin:" + name)) {
            return null;
        }
        File file = locator == null ? null : locator.find("Windowskins", name + ".png");
        if (file == null) {
            missing.put("skin:" + name, true);
            Gdx.app.log("MenuAssets", "windowskin not found: " + name);
            return null;
        }
        try {
            Pixmap pixmap = new Pixmap(pokemon.runtime.data.ResourceCrypto.handle(file));
            Texture texture = new Texture(pokemon.runtime.data.ResourceCrypto.handle(file));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            WindowSkin created = new WindowSkin(name, texture, pixmap);
            pixmap.dispose();
            skins.put(name, created);
            return created;
        } catch (RuntimeException error) {
            missing.put("skin:" + name, true);
            return null;
        }
    }

    private Texture load(String key, String directory, String file) {
        Texture cached = textures.get(key);
        if (cached != null) {
            return cached;
        }
        if (missing.containsKey(key)) {
            return null;
        }
        File found = locator == null ? null : locator.find(directory, file);
        if (found == null || !found.isFile()) {
            missing.put(key, true);
            Gdx.app.log("MenuAssets", "graphic not found: " + directory + "/" + file);
            return null;
        }
        try {
            Texture texture = new Texture(pokemon.runtime.data.ResourceCrypto.handle(found));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            textures.put(key, texture);
            return texture;
        } catch (RuntimeException error) {
            missing.put(key, true);
            return null;
        }
    }

    @Override
    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
        for (WindowSkin skin : skins.values()) {
            skin.texture().dispose();
        }
        skins.clear();
    }
}
