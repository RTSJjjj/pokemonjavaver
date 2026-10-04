package pokemon.runtime.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * R6.31: one Essentials / RGSS windowskin drawn as the 9-slice frame the
 * project's {@code SpriteWindow} uses.
 *
 * <p>The project ships three kinds of skins:</p>
 * <ul>
 *   <li>custom Essentials skins (96x48 speech, 48x48 choice, 80x48 signs,
 *       80x80 signs) - {@code loadSkinFile} fixes the body tile at (32,16) for
 *       80/96x48, (32,32) for 80x80 and at the bitmap centre for the rest;</li>
 *   <li>RPG XP windowskins (192x128): the left 128x128 is the tiled back, the
 *       right 64px column carries the corners, edges and cursor;</li>
 *   <li>RPG VX windowskins (128x128).</li>
 * </ul>
 *
 * <p>{@link Geometry} is pure math (headless-testable); {@link WindowSkin}
 * adds the texture, the dark/light decision and the tiled draw.</p>
 */
public final class WindowSkin {

    /** Fixed 9-slice geometry of a windowskin bitmap (SpriteWindow rules). */
    public static final class Geometry {
        public final int width;
        public final int height;
        /** XP / VX windowskin (SpriteWindow's skinformat 0). */
        public final boolean classic;
        public final boolean vx;
        public final int startX;
        public final int startY;
        public final int endX;
        public final int endY;
        /** The tiled centre tile. */
        public final int backX;
        public final int backY;
        public final int backW;
        public final int backH;
        public final int[] cornerX = new int[4];
        public final int[] cornerY = new int[4];
        public final int[] cornerW = new int[4];
        public final int[] cornerH = new int[4];
        /** Top / left / right / bottom edge source rectangles. */
        public final int[] sideX = new int[4];
        public final int[] sideY = new int[4];
        public final int[] sideW = new int[4];
        public final int[] sideH = new int[4];
        /** Content origin relative to the window's top-left corner. */
        public final int trimStartX;
        public final int trimStartY;
        /** How much the frame takes off the window size. */
        public final int borderX;
        public final int borderY;

        private Geometry(int width, int height, boolean classic, boolean vx,
                         int startX, int startY, int endX, int endY,
                         int backX, int backY, int backW, int backH,
                         int trimStartX, int trimStartY, int borderX, int borderY) {
            this.width = width;
            this.height = height;
            this.classic = classic;
            this.vx = vx;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.backX = backX;
            this.backY = backY;
            this.backW = backW;
            this.backH = backH;
            this.trimStartX = trimStartX;
            this.trimStartY = trimStartY;
            this.borderX = borderX;
            this.borderY = borderY;
        }

        /** Computes the geometry for a bitmap (matches SpriteWindow#privRefresh). */
        public static Geometry of(int width, int height) {
            if (width == 192 && height == 128) { // RPG XP
                Geometry g = new Geometry(width, height, true, false,
                        16, 16, 16, 16, 0, 0, 128, 128, 16, 16, 32, 32);
                g.setCorners(128, 0, 16, 16, 128 + 48, 0, 16, 16, 128, 48, 16, 16, 128 + 48, 48, 16, 16);
                g.setSides(128 + 16, 0, 32, 16, 128, 16, 16, 32,
                        128 + 48, 16, 16, 32, 128 + 16, 48, 32, 16);
                return g;
            }
            if (width == 128 && height == 128) { // RPG VX
                Geometry g = new Geometry(width, height, true, true,
                        16, 16, 16, 16, 0, 0, 64, 64, 16, 16, 32, 32);
                g.setCorners(64, 0, 16, 16, 112, 0, 16, 16, 64, 48, 16, 16, 112, 48, 16, 16);
                g.setSides(80, 0, 32, 16, 64, 16, 16, 32, 112, 16, 16, 32, 80, 48, 32, 16);
                return g;
            }
            // Custom Essentials skins (SpriteWindow skinformat 1).
            int bodyX;
            int bodyY;
            int trimX;
            int trimY;
            int trimW;
            int trimH;
            if ((width == 80 || width == 96) && height == 48) {
                bodyX = 32;
                bodyY = 16;
                trimX = 32;
                trimY = 16;
                trimW = 16;
                trimH = 16;
            } else if (width == 80 && height == 80) {
                bodyX = 32;
                bodyY = 32;
                trimX = 32;
                trimY = 16;
                trimW = 16;
                trimH = 48;
            } else {
                bodyX = (width - 16) / 2;
                bodyY = (height - 16) / 2;
                trimX = bodyX;
                trimY = bodyY;
                trimW = bodyX;
                trimH = bodyY;
            }
            int bodyW = 16;
            int bodyH = 16;
            int startX = bodyX;
            int startY = bodyY;
            int cx = bodyX + bodyW;
            int cy = bodyY + bodyH;
            int endX = width - cx;
            int endY = height - cy;
            int borderX = trimX + (width - trimW - trimX);
            int borderY = trimY + (height - trimH - trimY);
            Geometry g = new Geometry(width, height, false, false,
                    startX, startY, endX, endY, bodyX, bodyY, bodyW, bodyH,
                    trimX, trimY, borderX, borderY);
            g.setCorners(0, 0, startX, startY, cx, 0, endX, startY,
                    0, cy, startX, endY, cx, cy, endX, endY);
            g.setSides(startX, 0, bodyW, startY, 0, startY, startX, bodyH,
                    cx, startY, endX, bodyH, startX, cy, bodyW, endY);
            return g;
        }

        private void setCorners(int x0, int y0, int w0, int h0,
                                int x1, int y1, int w1, int h1,
                                int x2, int y2, int w2, int h2,
                                int x3, int y3, int w3, int h3) {
            cornerX[0] = x0; cornerY[0] = y0; cornerW[0] = w0; cornerH[0] = h0;
            cornerX[1] = x1; cornerY[1] = y1; cornerW[1] = w1; cornerH[1] = h1;
            cornerX[2] = x2; cornerY[2] = y2; cornerW[2] = w2; cornerH[2] = h2;
            cornerX[3] = x3; cornerY[3] = y3; cornerW[3] = w3; cornerH[3] = h3;
        }

        private void setSides(int topX, int topY, int topW, int topH,
                              int leftX, int leftY, int leftW, int leftH,
                              int rightX, int rightY, int rightW, int rightH,
                              int bottomX, int bottomY, int bottomW, int bottomH) {
            sideX[0] = topX; sideY[0] = topY; sideW[0] = topW; sideH[0] = topH;
            sideX[1] = leftX; sideY[1] = leftY; sideW[1] = leftW; sideH[1] = leftH;
            sideX[2] = rightX; sideY[2] = rightY; sideW[2] = rightW; sideH[2] = rightH;
            sideX[3] = bottomX; sideY[3] = bottomY; sideW[3] = bottomW; sideH[3] = bottomH;
        }
    }

    public final String name;
    public final int width;
    public final int height;
    public final boolean dark;
    public final Geometry geometry;
    private final Texture texture;

    public WindowSkin(String name, Texture texture, Pixmap pixels) {
        this.name = name;
        this.texture = texture;
        this.width = pixels.getWidth();
        this.height = pixels.getHeight();
        this.geometry = Geometry.of(width, height);
        this.dark = isDarkBackground(width, height,
                index -> pixels.getPixel(index % width, index / width), geometry);
    }

    /**
     * {@code isDarkWindowskin}: 192x128 samples the inner 128x128, VX the inner
     * 64x64, 96x48 the body tile and everything else its centre pixel. The
     * sample grid mirrors the source {@code isDarkBackground} (16x16 points).
     * Takes a pixel accessor so the rule stays testable without GL natives.
     */
    static boolean isDarkBackground(int width, int height,
                                    java.util.function.IntUnaryOperator pixel,
                                    Geometry geometry) {
        int x;
        int y;
        int w;
        int h;
        if (geometry.classic) {
            if (geometry.vx) {
                x = 0; y = 0; w = 64; h = 64;
            } else {
                x = 0; y = 0; w = 128; h = 128;
            }
        } else if (geometry.width == 96 && geometry.height == 48) {
            x = geometry.backX; y = geometry.backY;
            w = geometry.backW; h = geometry.backH;
        } else {
            int centre = pixel.applyAsInt(geometry.height / 2 * width + geometry.width / 2);
            return luminance(centre) < 160;
        }
        long r = 0;
        long g = 0;
        long b = 0;
        int count = 0;
        for (int gy = 0; gy < 16; gy++) {
            for (int gx = 0; gx < 16; gx++) {
                int px = x + gx * w / 16 + Math.max(1, w / 32);
                int py = y + gy * h / 16 + Math.max(1, h / 32);
                if (px >= width || py >= height) {
                    continue;
                }
                int colour = pixel.applyAsInt(py * width + px);
                if ((colour & 0xff) == 0) {
                    continue;
                }
                r += (colour >>> 24) & 0xff;
                g += (colour >>> 16) & 0xff;
                b += (colour >>> 8) & 0xff;
                count++;
            }
        }
        if (count == 0) {
            return true;
        }
        return (r * 0.299 + g * 0.587 + b * 0.114) / count < 160;
    }

    private static double luminance(int rgba8888) {
        int r = (rgba8888 >>> 24) & 0xff;
        int g = (rgba8888 >>> 16) & 0xff;
        int b = (rgba8888 >>> 8) & 0xff;
        return r * 0.299 + g * 0.587 + b * 0.114;
    }

    public Texture texture() {
        return texture;
    }

    /**
     * Draws the nine slices into {@code (x, y, width, height)} (y-up window
     * bottom-left). Edges and the body tile; partial tiles are clipped like
     * RGSS's sprite src_rect clipping.
     */
    public void draw(SpriteBatch batch, float x, float y, float width, float height) {
        Geometry g = geometry;
        // Corners (top-left, top-right, bottom-left, bottom-right).
        batch.draw(texture, x, y + height - g.startY, g.cornerW[0], g.cornerH[0],
                g.cornerX[0], g.cornerY[0], g.cornerW[0], g.cornerH[0], false, false);
        batch.draw(texture, x + width - g.endX, y + height - g.startY, g.cornerW[1], g.cornerH[1],
                g.cornerX[1], g.cornerY[1], g.cornerW[1], g.cornerH[1], false, false);
        batch.draw(texture, x, y, g.cornerW[2], g.cornerH[2],
                g.cornerX[2], g.cornerY[2], g.cornerW[2], g.cornerH[2], false, false);
        batch.draw(texture, x + width - g.endX, y, g.cornerW[3], g.cornerH[3],
                g.cornerX[3], g.cornerY[3], g.cornerW[3], g.cornerH[3], false, false);
        // Top / bottom edges.
        tileX(batch, x + g.startX, y + height - g.startY,
                width - g.startX - g.endX, g.sideY[0], g.sideH[0], g.sideX[0], g.sideW[0]);
        tileX(batch, x + g.startX, y,
                width - g.startX - g.endX, g.sideY[3], g.sideH[3], g.sideX[3], g.sideW[3]);
        // Left / right edges.
        tileY(batch, x, y + g.endY, height - g.startY - g.endY,
                g.sideX[1], g.sideW[1], g.sideY[1], g.sideH[1]);
        tileY(batch, x + width - g.endX, y + g.endY, height - g.startY - g.endY,
                g.sideX[2], g.sideW[2], g.sideY[2], g.sideH[2]);
        // Centre.
        tile(batch, x + g.startX, y + g.endY,
                width - g.startX - g.endX, height - g.startY - g.endY,
                g.backX, g.backY, g.backW, g.backH);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void tileX(SpriteBatch batch, float x, float y, float width,
                       int srcY, int srcH, int srcX, int srcW) {
        float remaining = width;
        float cursor = x;
        while (remaining > 0f) {
            float part = Math.min(srcW, remaining);
            batch.draw(texture, cursor, y, part, srcH, srcX, srcY, (int) part, srcH, false, false);
            cursor += part;
            remaining -= part;
        }
    }

    private void tileY(SpriteBatch batch, float x, float y, float height,
                       int srcX, int srcW, int srcY, int srcH) {
        float remaining = height;
        float cursor = y;
        while (remaining > 0f) {
            float part = Math.min(srcH, remaining);
            batch.draw(texture, x, cursor, srcW, part, srcX, srcY, srcW, (int) part, false, false);
            cursor += part;
            remaining -= part;
        }
    }

    private void tile(SpriteBatch batch, float x, float y, float width, float height,
                      int srcX, int srcY, int srcW, int srcH) {
        float remainingY = height;
        float cursorY = y;
        while (remainingY > 0f) {
            float partY = Math.min(srcH, remainingY);
            float remainingX = width;
            float cursorX = x;
            while (remainingX > 0f) {
                float partX = Math.min(srcW, remainingX);
                batch.draw(texture, cursorX, cursorY, partX, partY, srcX, srcY,
                        (int) partX, (int) partY, false, false);
                cursorX += partX;
                remainingX -= partX;
            }
            cursorY += partY;
            remainingY -= partY;
        }
    }
}
