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
    private String revealKey = "";

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
        if (messages.choiceMode() || (messages.speaker() != null && !messages.speaker().isEmpty())) {
            skin(CHOICE_SKIN);
            ui("selarrow", "Pictures/selarrow.png");
        }
        if (messages.waiting() && !messages.choiceMode()) {
            ui("pause", "Pictures/pause.png");
        }
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

        if (!messages.lines().isEmpty()) {
            if (skin != null) {
                skin.draw(batch, windowX, windowY, windowWidth, windowHeight);
            }
            drawLines(batch, skin, palette, windowX, windowY, windowHeight, windowWidth);
        }
        if (messages.speaker() != null && !messages.speaker().isEmpty()) {
            drawSpeaker(batch, windowX, windowY + windowHeight);
        }
        if (messages.choiceMode()) {
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
        float contentX = windowX + (skin != null ? skin.geometry.trimStartX : 16f);
        float contentWidth = windowWidth - (skin != null ? skin.geometry.borderX : 32f);
        float contentTop = windowY + windowHeight - (skin != null ? skin.geometry.trimStartY : 16f);
        Array<Run> runs = new Array<>();
        // <ac> persists across line breaks (the source keeps an alignment
        // stack until </ac>), which is how the notice pages centre every line.
        boolean centered = false;
        int remaining = revealedChars;
        for (int i = 0; i < messages.lines().size; i++) {
            String line = messages.lines().get(i);
            String lower = line.toLowerCase();
            if (lower.contains("<ac>")) {
                centered = true;
            }
            runs.clear();
            collectRuns(line, palette, runs);
            float lineWidth = 0f;
            for (Run run : runs) {
                layout.setText(font, run.text);
                lineWidth += layout.width;
            }
            float x = centered ? contentX + Math.max(0f, (contentWidth - lineWidth) / 2f) : contentX;
            float y = contentTop - i * LINE_HEIGHT;
            for (Run run : runs) {
                String visible = run.text;
                if (remaining < visible.length()) {
                    visible = remaining <= 0 ? "" : visible.substring(0, remaining);
                }
                remaining -= run.text.length();
                if (!visible.isEmpty()) {
                    drawRun(batch, new Run(visible, run.base, run.shadow), x, y);
                }
                layout.setText(font, run.text);
                x += layout.width;
            }
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
        StringBuilder key = new StringBuilder();
        for (String line : messages.lines()) {
            key.append(line).append('\n');
        }
        key.append('|').append(messages.speaker());
        String current = key.toString();
        if (!current.equals(revealKey)) {
            revealKey = current;
            revealedChars = 0;
        }
        int total = 0;
        for (String line : messages.lines()) {
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

        Run(String text, int base, int shadow) {
            this.text = text;
            this.base = base;
            this.shadow = shadow;
        }
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
        if (font != null) {
            font.dispose();
        }
        if (generator != null) {
            generator.dispose();
        }
    }
}
