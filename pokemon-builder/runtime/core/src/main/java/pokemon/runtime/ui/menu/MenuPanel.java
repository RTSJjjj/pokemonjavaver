package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.ui.WindowSkin;
import java.util.List;

/** Shared list/detail windows. Uses only the existing project assets/font. */
public final class MenuPanel {
    private MenuPanel() { }
    public static void fill(SpriteBatch b, MenuAssets a, float x, float y, float w, float h,
                            float r, float g, float blue, float alpha) {
        b.setColor(r, g, blue, alpha);
        b.draw(a.pixel(), x, y, w, h);
        b.setColor(Color.WHITE);
    }
    public static void window(SpriteBatch b, MenuAssets a, WindowSkin skin,
                              float x, float y, float w, float h) {
        fill(b, a, x, y, w, h, .07f, .12f, .21f, .96f);
        if (skin != null) skin.draw(b, x, y, w, h);
    }

    /**
     * The text colours over {@link #window}: the project's
     * {@code getDefaultTextColors} picks light text for a dark windowskin and
     * dark text (DARKTEXTBASE/DARKTEXTSHADOW) for the light system frame
     * ({@code choice 1}).
     */
    public static Color[] textColors(WindowSkin skin) {
        if (skin != null && !skin.dark) {
            return new Color[] {
                    new Color(90f / 255f, 82f / 255f, 82f / 255f, 1f),
                    new Color(165f / 255f, 165f / 255f, 173f / 255f, 1f)};
        }
        return new Color[] {Color.WHITE, new Color(132f / 255f, 132f / 255f, 132f / 255f, 1f)};
    }

    /**
     * A command window row's text position (libGDX bottom-origin y). Mirrors
     * {@code Window_DrawableCommand#drawItem}: {@code drawCursor} returns
     * {@code rect.x+16} on top of the contents origin ({@code startX/Y} = 16),
     * and {@code pbDrawShadowText} centres the text vertically in the row.
     */
    public static float rowX(float windowX) {
        return windowX + 16f + 16f;
    }

    public static float rowY(float windowY, float windowHeight, int index,
                             float rowHeight, float lineHeight) {
        return windowY + windowHeight - 16f - (rowHeight - lineHeight) / 2f - index * rowHeight;
    }

    /**
     * The row cursor bitmap. {@code Window_DrawableCommand#drawCursor} copies
     * {@code selarrow} (light skin) or {@code selarrow_white} (dark) at
     * {@code rect.x} = the contents origin, i.e. windowX+startX.
     */
    public static void cursor(SpriteBatch b, MenuAssets a, WindowSkin skin,
                              float windowX, float windowY, float windowHeight,
                              int index, int selected, float rowHeight) {
        if (index != selected) {
            return;
        }
        Texture arrow = a.graphic("Pictures", skin != null && skin.dark ? "selarrow_white" : "selarrow");
        if (arrow == null) {
            return;
        }
        float x = windowX + 16f;
        float y = windowY + windowHeight - 16f - index * rowHeight - arrow.getHeight();
        b.draw(arrow, x, y);
    }
    public static void text(SpriteBatch b, MenuFont f, String text, float x, float y, float width) {
        if (text == null) return;
        String shown = text;
        while (shown.length() > 1 && f.width(shown) > width) shown = shown.substring(0, shown.length() - 1);
        if (!shown.equals(text) && shown.length() > 1) shown = shown.substring(0, shown.length() - 1) + "…";
        f.draw(b, shown, x, y);
    }
    public static void draw(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin,
                            String title, List<String> rows, MenuListModel cursor,
                            List<String> detail, String footer) {
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        float split = w * .40f;
        window(b, a, skin, 12, h - 60, w - 24, 48);
        text(b, f, title, 28, h - 28, w - 56);
        window(b, a, skin, 12, 62, split - 18, h - 130);
        window(b, a, skin, split, 62, w - split - 12, h - 130);
        if (rows.isEmpty()) f.draw(b, "（空）", 30, h - 98);
        float lineHeight = (h - 162) / cursor.visible();
        for (int i = cursor.first(); i < cursor.end(); i++) {
            float y = h - 96 - (i - cursor.first()) * lineHeight;
            if (i == cursor.index()) fill(b, a, 22, y - 24, split - 40, lineHeight, .22f, .42f, .60f, .9f);
            text(b, f, (i == cursor.index() ? "▶ " : "   ") + rows.get(i), 28, y, split - 62);
        }
        if (cursor.size() > cursor.visible()) {
            float track = h - 154;
            float knob = track * cursor.visible() / cursor.size();
            fill(b, a, split - 15, 76, 4, track, .25f, .3f, .4f, 1);
            float offset = (track - knob) * cursor.first() / (cursor.size() - cursor.visible());
            fill(b, a, split - 15, 76 + track - knob - offset, 4, knob, .8f, .9f, 1, 1);
        }
        float y = h - 98;
        for (String line : detail) {
            // Wrap descriptions, avoiding overflow into the footer.
            String remaining = line == null ? "" : line;
            do {
                int end = remaining.length();
                while (end > 1 && f.width(remaining.substring(0, end)) > w - split - 50) end--;
                if (y < 84) break;
                f.draw(b, remaining.substring(0, end), split + 20, y);
                remaining = remaining.substring(end);
                y -= 29;
            } while (!remaining.isEmpty());
            if (y < 84) break;
        }
        text(b, f, footer, 22, 37, w - 44);
    }
}
