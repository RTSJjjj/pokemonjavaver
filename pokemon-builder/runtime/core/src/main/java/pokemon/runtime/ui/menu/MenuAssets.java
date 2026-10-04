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
            Pixmap pixmap = new Pixmap(new FileHandle(file));
            Texture texture = new Texture(new FileHandle(file));
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
            Texture texture = new Texture(new FileHandle(found));
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
