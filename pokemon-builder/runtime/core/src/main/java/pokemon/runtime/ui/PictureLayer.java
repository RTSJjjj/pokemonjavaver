package pokemon.runtime.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ObjectSet;
import pokemon.runtime.event.PictureService;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.map.TextureRepository;
import pokemon.runtime.map.ToneShader;

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
    private final com.badlogic.gdx.utils.Array<PictureService.Picture> sorted = new com.badlogic.gdx.utils.Array<>();
    /** Built on first use (needs a GL context): a picture's own RGSS tone. */
    private ToneShader toneShader;
    private boolean toneShaderFailed;

    public PictureLayer(PictureService pictures, TextureRepository textures, GraphicsLocator locator) {
        this.pictures = pictures;
        this.textures = textures;
        this.locator = locator;
    }

    public void render(SpriteBatch batch, float originX, float originY, float viewHeight) {
        if (pictures.isEmpty()) {
            return;
        }
        // RMXP draws the pictures by number, the higher ones on top (Spriteset_Map: @picture_sprites[n].z = 100 + n);
        // the map of ids has no order, so a picture shown with a higher number could end up under the lower ones.
        sorted.clear();
        for (PictureService.Picture picture : pictures.values()) {
            sorted.add(picture);
        }
        sorted.sort((a, b) -> Integer.compare(a.id, b.id));
        for (PictureService.Picture picture : sorted) {
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
            boolean toned = hasTone(picture) && toneShader() != null;
            if (toned) {
                batch.setShader(toneShader.program());
                toneShader.setTone(picture.toneRed, picture.toneGreen, picture.toneBlue, picture.toneGray);
            }
            batch.draw(texture, x, y, width, height);
            if (toned) {
                batch.setShader(null);
            }
            batch.setColor(1f, 1f, 1f, 1f);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    private static boolean hasTone(PictureService.Picture picture) {
        return picture.toneRed != 0f || picture.toneGreen != 0f || picture.toneBlue != 0f
                || picture.toneGray != 0f;
    }

    private ToneShader toneShader() {
        if (toneShader == null && !toneShaderFailed) {
            ToneShader shader = new ToneShader();
            if (shader.isCompiled()) {
                toneShader = shader;
            } else {
                toneShaderFailed = true;
                Gdx.app.error("PictureLayer", "picture tone shader failed to compile: " + shader.log());
            }
        }
        return toneShader;
    }
}
