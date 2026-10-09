package pokemon.runtime.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import pokemon.runtime.event.MessageService;
import pokemon.runtime.map.GraphicsLocator;
import pokemon.runtime.map.TextureRepository;

import java.io.File;

/**
 * R6.31: the Essentials message window with the project's real graphics.
 *
 * <p>Behaviour (paging, waiting, choices) stays in the headless
 * {@link MessageService}; this class only draws:</p>
 * <ul>
 *   <li>the speech frame from {@code Graphics/Windowskins/speech bw 1} drawn
 *       as the source 9-slice (80px side caps, 16px top/bottom);</li>
 *   <li>two 32px text lines with the 22px pixel font, base colour plus the
 *       2px shadow the source renderer uses ({@code \c[n]} / {@code <c3>});</li>
 *   <li>the speaker name box from the project's name-box plugin (choice
 *       skin, centred text);</li>
 *   <li>the choice window and its arrow ({@code selarrow});</li>
 *   <li>the pause cursor ({@code Graphics/Pictures/pause}) bottom right
 *       (MessageConfig::CURSORMODE = 1).</li>
 * </ul>
 */
public final class MessageWindow implements Disposable {

    /** Project defaults from MessageConfig / the name-box plugin. */
    public static final String DEFAULT_SKIN = "speech bw 1";
    public static final String CHOICE_SKIN = "choice 1";

    private static final int LINE_HEIGHT = 32;
    private static final int FONT_SIZE = 22;

    private final MessageService messages;
    private final TextureRepository textures;
    private final GraphicsLocator locator;
    private final ObjectMap<String, WindowSkin> skins = new ObjectMap<>();
    private final ObjectMap<String, Boolean> missingSkins = new ObjectMap<>();
    private final ObjectMap<String, Texture> ui = new ObjectMap<>();
    private final ObjectMap<String, Boolean> missingUi = new ObjectMap<>();

    /** Incremental fonts keep generating glyphs, so the generator must live on. */
    private FreeTypeFontGenerator generator;
    private BitmapFont font;
    private boolean ready;
    /** Last measured line width (GlyphLayout is reused by BitmapFont#draw). */
    private final GlyphLayout layout = new GlyphLayout();

    /** 文字速度 (PScreen_Options): 0=slow, 1=normal, 2=fast. */
    public int textSpeed = 1;
    /** The char-by-char reveal (MessageConfig::pbGetSystemTextSpeed). */
    private int revealedChars;
    private String[] revealLines = new String[0];
    private String revealSpeaker;

    public MessageWindow(MessageService messages, File fontFile,
                         TextureRepository textures, GraphicsLocator locator) {
        this.messages = messages;
        this.textures = textures;
        this.locator = locator;
        if (fontFile == null || !fontFile.isFile()) {
            Gdx.app.error("MessageWindow", "message font not found; the window stays hidden");
            return;
        }
        generator = new FreeTypeFontGenerator(Gdx.files.absolute(fontFile.getAbsolutePath()));
        try {
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = FONT_SIZE;
            parameter.incremental = true; // CJK glyphs are added on demand
            parameter.minFilter = Texture.TextureFilter.Nearest;
            parameter.magFilter = Texture.TextureFilter.Nearest;
            font = generator.generateFont(parameter);
        } catch (RuntimeException error) {
            generator.dispose();
            generator = null;
            throw error;
        }
        ready = true;
    }

    public boolean ready() {
        return ready;
    }

    /** Fixed text of the item / gift messages (309_Item_Find) and the common UI wording, rendered ahead of time. */
    private static final String COMMON_TEXT =
            "你发现了获得一些个将放进口袋。但是背包满了...！？，、：“”（）《》0123456789"
            + "道具回复精灵球招式学习机树果超级石对战重要特殊零花钱是否好的不"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz×$%,.:;!?'\"-+/ ";

    /**
     * Generates the glyphs of the incremental font and loads the skins / icons the item messages use, so the first
     * message does not stall on FreeType and disk reads. {@code extraTexts}: item and move names of the PBS data.
     */
    public void warmUp(Iterable<String> extraTexts) {
        if (!ready) {
            return;
        }
        java.util.LinkedHashSet<Integer> codes = new java.util.LinkedHashSet<>();
        addCodePoints(codes, COMMON_TEXT);
        if (extraTexts != null) {
            for (String text : extraTexts) {
                addCodePoints(codes, text);
            }
        }
        StringBuilder all = new StringBuilder(codes.size() * 2);
        for (int code : codes) {
            all.appendCodePoint(code);
            if (all.length() >= 64) {                       // short layouts keep each glyph-page update small
                layout.setText(font, all);
                all.setLength(0);
            }
        }
        if (all.length() > 0) {
            layout.setText(font, all);
        }
        skin(DEFAULT_SKIN);
        skin(CHOICE_SKIN);
        ui("pause", "Pictures/pause.png");
        ui("selarrow", "Pictures/selarrow.png");
        for (int pocket = 1; pocket <= 9; pocket++) {
            iconTexture("bagPocket" + pocket);
        }
    }

    private static void addCodePoints(java.util.Set<Integer> codes, String text) {
        if (text == null) {
            return;
        }
        text.codePoints().forEach(codes::add);
    }

    /**
     * Loads the textures the next frame needs. Called outside the batch so a
     * first-time skin load never happens in the middle of drawing.
     */
    public void prepare() {
        if (!ready || !messages.visible()) {
            return;
        }
        String skinName = messages.skin();
        if (skinName == null) {
            skinName = DEFAULT_SKIN;
        }
        if (!skinName.isEmpty()) {
            skin(skinName);
        }
        if (messages.numberMode() || messages.choiceMode()
                || (messages.speaker() != null && !messages.speaker().isEmpty())) {
            skin(CHOICE_SKIN);
            ui("selarrow", "Pictures/selarrow.png");
        }
        if (messages.waiting() && !messages.choiceMode() && !messages.numberMode()) {
            ui("pause", "Pictures/pause.png");
        }
    }

    /**
     * {@code pbDrawTextPositions}' text with the three-copy shadow (+2,0), (0,+2), (+2,+2) (MenuFont#draw); {@code y} is the
     * top of the text line.
     */
    public void drawShadowText(SpriteBatch batch, String text, float x, float y, com.badlogic.gdx.graphics.Color main,
                               com.badlogic.gdx.graphics.Color shadow) {
        if (!ready) {
            return;
        }
        font.setColor(shadow);
        font.draw(batch, text, x + 2f, y);
        font.draw(batch, text, x, y - 2f);
        font.draw(batch, text, x + 2f, y - 2f);
        font.setColor(main);
        font.draw(batch, text, x, y);
        font.setColor(1f, 1f, 1f, 1f);
    }

    /** {@code pbDrawTextPositions} with alignment 2 (centred on {@code centerX}) and the one-shadow style 0 of Headtop_Name. */
    public void drawCenteredShadowText(SpriteBatch batch, String text, float centerX, float y,
                                       com.badlogic.gdx.graphics.Color main, com.badlogic.gdx.graphics.Color shadow, float alpha) {
        if (!ready) {
            return;
        }
        layout.setText(font, text);
        float x = centerX - layout.width / 2f;
        font.setColor(shadow.r, shadow.g, shadow.b, alpha);
        font.draw(batch, text, x + 2f, y);
        font.draw(batch, text, x, y - 2f);
        font.draw(batch, text, x + 2f, y - 2f);
        font.setColor(main.r, main.g, main.b, alpha);
        font.draw(batch, text, x, y);
        font.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Small text with a shadow for the boxes of the map (308_ItemFindSimple_Scene:42-58: white on dark grey);
     * {@code x} is the left edge, or the right edge when {@code right}; {@code y} is the top of the text line.
     */
    public void drawToastText(SpriteBatch batch, String text, float x, float y, boolean right) {
        if (!ready) {
            return;
        }
        layout.setText(font, text);
        float left = right ? x - layout.width : x;
        font.setColor(64 / 255f, 64 / 255f, 64 / 255f, 1f);
        font.draw(batch, text, left + 1f, y - 1f);
        font.setColor(248 / 255f, 248 / 255f, 248 / 255f, 1f);
        font.draw(batch, text, left, y);
        font.setColor(1f, 1f, 1f, 1f);
    }

    /**
     * Draws the current message inside the screen rectangle whose bottom-left
     * corner is {@code (originX, originY)}.
     */
    public void render(SpriteBatch batch, float originX, float originY,
                       float viewWidth, float viewHeight) {
        if (!ready || !messages.visible()) {
            return;
        }
        String skinName = messages.skin();
        if (skinName == null) {
            skinName = DEFAULT_SKIN;
        }
        // \w[] hides the window; the source then keeps white text
        // (isDarkWindowskin(nil) is true). A failed load behaves the same.
        WindowSkin skin = skinName.isEmpty() ? null : skin(skinName);
        MessagePalette palette = new MessagePalette(skin == null || skin.dark);

        int linesPerPage = Math.max(1, messages.linesPerPage());
        float windowHeight = linesPerPage * LINE_HEIGHT + (skin != null ? skin.geometry.borderY : 0);
        float windowX = originX;
        float windowY = originY;
        float windowWidth = viewWidth;
        // \wm / \wu (071_Messages:1198-1217): the window in the middle or at the top of the screen.
        if (messages.position() == 1) {
            windowY = originY + (viewHeight - windowHeight) / 2f;
        } else if (messages.position() == 2) {
            windowY = originY + viewHeight - windowHeight;
        }

        if (!messages.lines().isEmpty()) {
            if (skin != null) {
                skin.draw(batch, windowX, windowY, windowWidth, windowHeight);
            }
            drawLines(batch, skin, palette, windowX, windowY, windowHeight, windowWidth);
        }
        if (messages.speaker() != null && !messages.speaker().isEmpty()) {
            drawSpeaker(batch, windowX, windowY + windowHeight);
        }
        if (messages.gold()) {
            drawGold(batch, originX, originY, viewHeight, messages.position() == 2);
        }
        if (messages.numberMode()) {
            float messageTop = messages.lines().isEmpty() ? originY : windowY + windowHeight;
            drawNumberInput(batch, originX, originY, viewWidth, viewHeight, messageTop);
        } else if (messages.choiceMode()) {
            float messageTop = messages.lines().isEmpty()
                    ? originY : windowY + windowHeight;
            drawChoices(batch, originX, originY, viewWidth, viewHeight, messageTop);
        } else if (messages.waiting()) {
            drawPauseCursor(batch, windowX, windowY, windowWidth);
        }
    }

    // ------------------------------------------------------------------
    // Text
    // ------------------------------------------------------------------

    private void drawLines(SpriteBatch batch, WindowSkin skin, MessagePalette palette,
                           float windowX, float windowY, float windowHeight,
                           float windowWidth) {
        updateReveal();
        ensureLayoutCache(palette);
        float contentX = windowX + (skin != null ? skin.geometry.trimStartX : 16f);
        float contentWidth = windowWidth - (skin != null ? skin.geometry.borderX : 32f);
        float contentTop = windowY + windowHeight - (skin != null ? skin.geometry.trimStartY : 16f);
        int remaining = revealedChars;
        for (int i = 0; i < cachedRuns.size; i++) {
            Array<Run> runs = cachedRuns.get(i);
            float x = cachedCentered[i] ? contentX + Math.max(0f, (contentWidth - cachedWidths[i]) / 2f) : contentX;
            float y = contentTop - i * LINE_HEIGHT;
            for (int r = 0; r < runs.size; r++) {
                Run run = runs.get(r);
                int length = run.text.length();
                if (remaining > 0) {
                    if (run.icon != null) {
                        Texture icon = iconTexture(run.icon);
                        if (icon != null) {
                            batch.draw(icon, x, y - icon.getHeight() + 6f);
                        }
                    } else if (remaining >= length) {
                        drawRun(batch, run, x, y);                       // fully revealed: no copy
                    } else {
                        drawRun(batch, new Run(run.text.substring(0, remaining), run.base, run.shadow), x, y);
                    }
                }
                remaining -= length;
                x += run.width;
            }
        }
    }

    // The parsed message, rebuilt only when its text/speaker/palette changes (not every frame).
    private final Array<Array<Run>> cachedRuns = new Array<>();
    private String[] cachedSource = new String[0];
    private float[] cachedWidths = new float[0];
    private boolean[] cachedCentered = new boolean[0];
    private int cachedTotalChars;
    private int cachedBase = Integer.MIN_VALUE;
    private int cachedShadow;

    private boolean layoutCacheValid(Array<String> lines, MessagePalette palette) {
        if (cachedSource.length != lines.size || cachedBase != palette.base || cachedShadow != palette.shadow) {
            return false;
        }
        for (int i = 0; i < cachedSource.length; i++) {
            String line = lines.get(i);
            if (line != cachedSource[i] && !line.equals(cachedSource[i])) {      // same instance each frame: O(1)
                return false;
            }
        }
        return true;
    }

    private void ensureLayoutCache(MessagePalette palette) {
        Array<String> lines = messages.lines();
        if (layoutCacheValid(lines, palette)) {
            return;
        }
        int count = lines.size;
        cachedSource = new String[count];
        cachedWidths = new float[count];
        cachedCentered = new boolean[count];
        cachedRuns.clear();
        cachedBase = palette.base;
        cachedShadow = palette.shadow;
        cachedTotalChars = 0;
        // <ac> persists across line breaks (the source keeps an alignment
        // stack until </ac>), which is how the notice pages centre every line.
        boolean centered = false;
        for (int i = 0; i < count; i++) {
            String line = lines.get(i);
            cachedSource[i] = line;
            String lower = line.toLowerCase();
            if (lower.contains("<ac>")) {
                centered = true;
            }
            Array<Run> runs = new Array<>();
            collectRuns(line, palette, runs);
            float width = 0f;
            for (Run run : runs) {
                run.width = runWidth(run);
                width += run.width;
            }
            cachedRuns.add(runs);
            cachedWidths[i] = width;
            cachedCentered[i] = centered;
            cachedTotalChars += line.length();
            if (lower.contains("</ac>")) {
                centered = false;
            }
        }
    }

    /**
     * MessageConfig::pbGetSystemTextSpeed: reveals the text a few characters per
     * frame (slow waits a frame between characters, fast shows three at a time).
     */
    private void updateReveal() {
        Array<String> lines = messages.lines();
        String speaker = messages.speaker();
        boolean changed = revealLines.length != lines.size || !java.util.Objects.equals(speaker, revealSpeaker);
        for (int i = 0; !changed && i < lines.size; i++) {
            String line = lines.get(i);
            changed = line != revealLines[i] && !line.equals(revealLines[i]);
        }
        if (changed) {
            revealLines = lines.toArray(String.class);
            revealSpeaker = speaker;
            revealedChars = 0;
        }
        int total = 0;
        for (String line : revealLines) {
            total += line.length();
        }
        if (revealedChars >= total) {
            return;
        }
        int step;
        switch (textSpeed) {
            case 0: step = (Gdx.graphics.getFrameId() % 3 == 0) ? 1 : 0; break;
            case 2: step = 3; break;
            default: step = 1; break;
        }
        revealedChars = Math.min(total, revealedChars + Math.max(1, step));
    }

    private void drawRun(SpriteBatch batch, Run run, float x, float y) {
        font.setColor((run.shadow >>> 16 & 0xff) / 255f, (run.shadow >>> 8 & 0xff) / 255f,
                (run.shadow & 0xff) / 255f, 1f);
        font.draw(batch, run.text, x + 2f, y);
        font.draw(batch, run.text, x, y - 2f);
        font.draw(batch, run.text, x + 2f, y - 2f);
        font.setColor((run.base >>> 16 & 0xff) / 255f, (run.base >>> 8 & 0xff) / 255f,
                (run.base & 0xff) / 255f, 1f);
        font.draw(batch, run.text, x, y);
    }

    /** One styled piece of a line. */
    private static final class Run {
        final String text;
        final int base;
        final int shadow;
        /** {@code <icon=X>} (070_DrawText:604-610): a picture of Graphics/Icons drawn in the line, or null. */
        final String icon;
        /** Measured once when the message is parsed. */
        float width;

        Run(String text, int base, int shadow) {
            this(text, base, shadow, null);
        }

        Run(String text, int base, int shadow, String icon) {
            this.text = text;
            this.base = base;
            this.shadow = shadow;
            this.icon = icon;
        }
    }

    /** The icon placeholder: one revealed character, drawn as the picture. */
    private static final String ICON_CHAR = "\uFFFC";
    private final ObjectMap<String, Texture> iconTextures = new ObjectMap<>();

    private Texture iconTexture(String name) {
        if (iconTextures.containsKey(name)) {
            return iconTextures.get(name);
        }
        File file = locator == null ? null : locator.find("Icons", name + ".png");
        Texture texture = file == null ? null : textures.load("icon:" + name, file);
        iconTextures.put(name, texture);
        return texture;
    }

    private float runWidth(Run run) {
        if (run.icon != null) {
            Texture icon = iconTexture(run.icon);
            return icon == null ? 0f : icon.getWidth();
        }
        layout.setText(font, run.text);
        return layout.width;
    }

    /**
     * Splits a line into coloured runs. Understands {@code \c[n]},
     * {@code <c3=B,S>} / {@code </c3>} and {@code <c2=...>} / {@code </c2>};
     * {@code <ac>} asks for centring.
     *
     * @return true when the line is centred
     */
    private boolean collectRuns(String line, MessagePalette palette, Array<Run> runs) {
        int base = palette.base;
        int shadow = palette.shadow;
        boolean centered = false;
        StringBuilder text = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            char ch = line.charAt(i);
            if (ch == '\\' && i + 2 < line.length()
                    && (line.charAt(i + 1) == 'c' || line.charAt(i + 1) == 'C')
                    && line.charAt(i + 2) == '[') {
                int close = line.indexOf(']', i);
                if (close > 0) {
                    if (text.length() > 0) {
                        runs.add(new Run(text.toString(), base, shadow));
                        text.setLength(0);
                    }
                    int index;
                    try {
                        index = Integer.parseInt(line.substring(i + 3, close));
                    } catch (NumberFormatException error) {
                        index = 0;
                    }
                    int[] color = palette.color(index);
                    base = color[0];
                    shadow = color[1];
                    i = close + 1;
                    continue;
                }
            }
            if (ch == '<') {
                int close = line.indexOf('>', i);
                if (close > 0) {
                    String tag = line.substring(i, close + 1);
                    String lower = tag.toLowerCase();
                    if (lower.startsWith("<icon=")) {                       // 070_DrawText:604-610
                        if (text.length() > 0) {
                            runs.add(new Run(text.toString(), base, shadow));
                            text.setLength(0);
                        }
                        runs.add(new Run(ICON_CHAR, base, shadow, tag.substring(6, tag.length() - 1).trim()));
                        i = close + 1;
                        continue;
                    }
                    if (lower.equals("<ac>")) {
                        centered = true;
                        i = close + 1;
                        continue;
                    }
                    if (lower.equals("</ac>")) {
                        i = close + 1;
                        continue;
                    }
                    if (lower.startsWith("<c3") || lower.startsWith("<c2")
                            || lower.equals("</c3>") || lower.equals("</c2>")) {
                        if (text.length() > 0) {
                            runs.add(new Run(text.toString(), base, shadow));
                            text.setLength(0);
                        }
                        if (lower.startsWith("<c3")) {
                            String body = tag.substring(tag.indexOf('=') + 1,
                                    tag.length() - 1);
                            String[] parts = body.split(",", -1);
                            base = MessagePalette.parseRgb32(parts.length > 0 ? parts[0] : null,
                                    base);
                            shadow = MessagePalette.parseRgb32(parts.length > 1 ? parts[1] : null,
                                    shadow);
                        } else if (lower.startsWith("<c2")) {
                            String body = tag.substring(tag.indexOf('=') + 1,
                                    tag.length() - 1);
                            int[] color = MessagePalette.parseRgb16Pair(body, base, shadow);
                            base = color[0];
                            shadow = color[1];
                        } else {
                            base = palette.base;
                            shadow = palette.shadow;
                        }
                        i = close + 1;
                        continue;
                    }
                }
            }
            text.append(ch);
            i++;
        }
        if (text.length() > 0) {
            runs.add(new Run(text.toString(), base, shadow));
        }
        return centered;
    }

    // ------------------------------------------------------------------
    // Speaker, choices, cursor
    // ------------------------------------------------------------------

    /** Provides the player's money for {@link #drawGold}. */
    public java.util.function.IntSupplier money = () -> 0;

    /**
     * {@code pbDisplayGoldWindow} (071_Messages:926-940): the "零花钱" box with the money right-aligned; it sits at the top of
     * the screen unless the message is there ({@code msgwindow.y == 0}), then at the bottom.
     */
    private void drawGold(SpriteBatch batch, float originX, float originY, float viewHeight, boolean messageAtTop) {
        WindowSkin skin = skin("goldskin");
        if (skin == null) {
            skin = skin(CHOICE_SKIN);
        }
        if (skin == null) {
            return;
        }
        String label = "零花钱：";
        String moneyText = "$" + String.format(java.util.Locale.ROOT, "%,d", money.getAsInt());
        layout.setText(font, label);
        float labelWidth = layout.width;
        layout.setText(font, moneyText);
        float width = Math.max(160f, Math.max(labelWidth, layout.width) + skin.geometry.borderX + 4f);
        float height = 2 * LINE_HEIGHT + skin.geometry.borderY;
        float y = messageAtTop ? originY : originY + viewHeight - height;
        skin.draw(batch, originX, y, width, height);
        MessagePalette palette = new MessagePalette(skin.dark);
        float top = y + height - skin.geometry.trimStartY;
        drawRun(batch, new Run(label, palette.base, palette.shadow), originX + skin.geometry.trimStartX, top);
        layout.setText(font, moneyText);
        drawRun(batch, new Run(moneyText, palette.base, palette.shadow),
                originX + width - skin.geometry.trimStartX - layout.width, top - LINE_HEIGHT);
    }

    private void drawSpeaker(SpriteBatch batch, float x, float messageTop) {
        String speaker = messages.speaker();
        WindowSkin skin = skin(CHOICE_SKIN);
        if (skin == null) {
            return;
        }
        layout.setText(font, speaker);
        float width = Math.max(180f, layout.width + skin.geometry.borderX + 4f);
        float height = LINE_HEIGHT + skin.geometry.borderY;
        skin.draw(batch, x, messageTop, width, height);
        MessagePalette palette = new MessagePalette(skin.dark);
        Run run = new Run(speaker, palette.base, palette.shadow);
        drawRun(batch, run, x + (width - layout.width) / 2f,
                messageTop + height - skin.geometry.trimStartY);
    }

    private void drawChoices(SpriteBatch batch, float originX, float originY,
                             float viewWidth, float viewHeight, float messageTop) {
        WindowSkin skin = skin(CHOICE_SKIN);
        if (skin == null) {
            return;
        }
        MessagePalette palette = new MessagePalette(skin.dark);
        float maxText = 0f;
        for (String choice : messages.choices()) {
            layout.setText(font, choice);
            maxText = Math.max(maxText, layout.width);
        }
        float width = maxText + skin.geometry.borderX + 36f;
        float height = messages.choices().size * LINE_HEIGHT + skin.geometry.borderY;
        float x = originX + viewWidth - width;
        float y = messageTop;
        if (y + height > originY + viewHeight) {
            y = Math.max(originY, originY + viewHeight - height);
        }
        skin.draw(batch, x, y, width, height);

        for (int i = 0; i < messages.choices().size; i++) {
            float rowTop = y + height - skin.geometry.trimStartY - i * LINE_HEIGHT;
            if (i == messages.cursor()) {
                Texture arrow = ui("selarrow", "Pictures/selarrow.png");
                if (arrow != null) {
                    batch.setColor(1f, 1f, 1f, 1f);
                    batch.draw(arrow, x + skin.geometry.trimStartX, rowTop - 30f,
                            arrow.getWidth(), arrow.getHeight());
                }
            }
            Array<Run> runs = new Array<>();
            collectRuns(messages.choices().get(i), palette, runs);
            float textX = x + skin.geometry.trimStartX + 16f;
            for (Run run : runs) {
                drawRun(batch, run, textX, rowTop);
                layout.setText(font, run.text);
                textX += layout.width;
            }
        }
    }

    /**
     * Window_InputNumberPokemon (065_SpriteWindow_text:612-): one 24px cell per
     * digit, {@code width = digits*24 + 8 + borderX}, {@code height = 32 + borderY},
     * the digit under the cursor underlined and blinking ({@code @frame/15 == 0} of a
     * 30 frame cycle at 40 fps). 071_Messages {@code pbPositionNearMsgWindow(...,:right)}
     * puts it at the right edge of the screen, just above or below the message window.
     */
    private void drawNumberInput(SpriteBatch batch, float originX, float originY,
                                 float viewWidth, float viewHeight, float messageTop) {
        WindowSkin skin = skin(CHOICE_SKIN);
        if (skin == null) {
            return;
        }
        MessagePalette palette = new MessagePalette(skin.dark);
        int digits = messages.numberDigits();
        float width = digits * 24f + 8f + skin.geometry.borderX;
        float height = 32f + skin.geometry.borderY;
        float x = originX + viewWidth - width;
        float y = messageTop;
        if (y + height > originY + viewHeight) {
            y = Math.max(originY, originY + viewHeight - height);
        }
        skin.draw(batch, x, y, width, height);
        String text = String.format("%0" + digits + "d", messages.number());
        float rowTop = y + height - skin.geometry.trimStartY;
        float left = x + skin.geometry.trimStartX;
        boolean blinkOn = (System.nanoTime() / 375_000_000L) % 2L == 0L;      // 15 of 30 frames at 40 fps
        for (int i = 0; i < digits; i++) {
            String digit = text.substring(i, i + 1);
            layout.setText(font, digit);
            float cellX = left + i * 24f + (12f - layout.width / 2f);
            drawRun(batch, new Run(digit, palette.base, palette.shadow), cellX, rowTop);
            if (i == messages.numberIndex() && blinkOn) {
                batch.setColor((palette.base >>> 16 & 0xff) / 255f, (palette.base >>> 8 & 0xff) / 255f,
                        (palette.base & 0xff) / 255f, 1f);
                batch.draw(underline(), cellX, rowTop - 30f, Math.max(1f, layout.width), 2f);
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }
    }

    private Texture underlineTexture;

    private Texture underline() {
        if (underlineTexture == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(1f, 1f, 1f, 1f);
            pixmap.fill();
            underlineTexture = new Texture(pixmap);
            pixmap.dispose();
        }
        return underlineTexture;
    }

    private void drawPauseCursor(SpriteBatch batch, float windowX, float windowY,
                                 float windowWidth) {
        Texture pause = ui("pause", "Pictures/pause.png");
        if (pause == null) {
            return;
        }
        int frames = Math.max(1, pause.getWidth() / 20);
        int frame = frames <= 1 ? 0
                : (int) ((Gdx.graphics.getFrameId() / 3) % frames);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(pause, windowX + windowWidth - 30f, windowY + 18f,
                20f, 28f, frame * 20, 0, 20, 28, false, false);
    }

    // ------------------------------------------------------------------
    // Assets
    // ------------------------------------------------------------------

    private WindowSkin skin(String name) {
        WindowSkin cached = skins.get(name);
        if (cached != null) {
            return cached;
        }
        if (missingSkins.containsKey(name)) {
            return null;
        }
        File file = locator == null ? null : locator.find("Windowskins", name + ".png");
        if (file == null) {
            missingSkins.put(name, true);
            Gdx.app.log("MessageWindow", "windowskin not found: " + name);
            return null;
        }
        Texture texture = textures.load("windowskin:" + name, file);
        Pixmap pixmap = new Pixmap(new FileHandle(file));
        WindowSkin created = new WindowSkin(name, texture, pixmap);
        pixmap.dispose();
        skins.put(name, created);
        return created;
    }

    private Texture ui(String key, String relative) {
        Texture cached = ui.get(key);
        if (cached != null) {
            return cached;
        }
        if (missingUi.containsKey(key)) {
            return null;
        }
        File file = locator == null ? null : locator.find("Pictures",
                relative.substring(relative.indexOf('/') + 1));
        if (file == null) {
            missingUi.put(key, true);
            Gdx.app.log("MessageWindow", "ui graphic not found: " + relative);
            return null;
        }
        Texture texture = textures.load("ui:" + key, file);
        ui.put(key, texture);
        return texture;
    }

    @Override
    public void dispose() {
        if (underlineTexture != null) {
            underlineTexture.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (generator != null) {
            generator.dispose();
        }
    }
}
