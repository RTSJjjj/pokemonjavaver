package pokemon.runtime.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ObjectSet;
import pokemon.runtime.event.PictureService;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.map.TextureRepository;

import java.io.File;

/**
 * Draws the pictures of {@link PictureService} (project3 section 18, R6.4).
 * Screen-space coordinates: {@code (originX, originY)} is the bottom-left
 * corner of the logical view, and RMXP's picture y grows downwards.
 */
public final class PictureLayer {

    private final PictureService pictures;
    private final TextureRepository textures;
    private final GraphicsLocator locator;
    private final ObjectSet<String> missing = new ObjectSet<>();

    public PictureLayer(PictureService pictures, TextureRepository textures, GraphicsLocator locator) {
        this.pictures = pictures;
        this.textures = textures;
        this.locator = locator;
    }

    public void render(SpriteBatch batch, float originX, float originY, float viewHeight) {
        if (pictures.isEmpty()) {
            return;
        }
        for (PictureService.Picture picture : pictures.values()) {
            if (picture.name == null || picture.name.isEmpty()) {
                continue;
            }
            File file = locator.find("Pictures", picture.name + ".png");
            if (file == null) {
                if (missing.add(picture.name)) {
                    Gdx.app.error("PictureLayer", "missing picture: Graphics/Pictures/"
                            + picture.name + ".png");
                }
                continue;
            }
            Texture texture = textures.load("picture:" + picture.name, file);
            float width = texture.getWidth() * picture.zoomX / 100f;
            float height = texture.getHeight() * picture.zoomY / 100f;
            boolean centered = picture.origin == 1;
            float topDown = centered ? picture.y - height / 2f : picture.y;
            float x = originX + picture.x - (centered ? width / 2f : 0f);
            float y = originY + viewHeight - topDown - height;

            batch.setColor(1f, 1f, 1f, Math.max(0f, Math.min(1f, picture.opacity / 255f)));
            if (picture.blendType == 1) {
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            }
            batch.draw(texture, x, y, width, height);
            batch.setColor(1f, 1f, 1f, 1f);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }
}
