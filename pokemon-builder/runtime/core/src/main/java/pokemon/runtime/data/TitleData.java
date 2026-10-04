package pokemon.runtime.data;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/**
 * L1/L14: the project's title screen configuration ({@code generated/title.json},
 * extracted by the Builder from the Modular Title Screen plugin section):
 * splash slides, their duration, the title BGM and the L14 visual recipe
 * (modifiers/footer/messages/fade). A project without the plugin has no
 * title.json and gets {@link #empty()} (skip straight to the start prompt).
 */
public final class TitleData {

    public String kind = "title";
    public String source = "";
    public Array<String> splashImages = new Array<>();
    /** Seconds each splash slide stays on screen (0 = skip the sequence). */
    public float secondsPerSplash = 0f;
    public String bgm = "";
    public int bgmVolume = 100;
    /** L14: the active MODIFIERS entries (background/overlay/logo/effects...). */
    public Array<String> modifiers = new Array<>();
    /** L14: footer bar texts (version name / copyright). */
    public String footerLeft = "";
    public String footerRight = "";
    /** L14: the rotating title messages (chosen randomly like the original). */
    public Array<String> splashMessages = new Array<>();
    /** L14: splash fade length in ticks (fade seconds = ticks / 20). */
    public int fadeTicks = 8;

    public static TitleData parse(JsonValue root) {
        if (root == null) {
            return empty();
        }
        TitleData data = new TitleData();
        data.kind = root.getString("kind", "title");
        data.source = root.getString("source", "");
        data.secondsPerSplash = root.getFloat("secondsPerSplash", 0f);
        data.bgm = root.getString("bgm", "");
        data.bgmVolume = root.getInt("bgmVolume", 100);
        JsonValue images = root.get("splashImages");
        if (images != null && images.isArray()) {
            for (JsonValue image = images.child; image != null; image = image.next) {
                String name = image.asString();
                if (name != null && !name.isEmpty()) {
                    data.splashImages.add(name);
                }
            }
        }
        JsonValue modifiers = root.get("modifiers");
        if (modifiers != null && modifiers.isArray()) {
            for (JsonValue modifier = modifiers.child; modifier != null; modifier = modifier.next) {
                String token = modifier.asString();
                if (token != null && !token.isEmpty()) {
                    data.modifiers.add(token);
                }
            }
        }
        data.footerLeft = root.getString("footerLeft", "");
        data.footerRight = root.getString("footerRight", "");
        JsonValue messages = root.get("splashMessages");
        if (messages != null && messages.isArray()) {
            for (JsonValue message = messages.child; message != null; message = message.next) {
                String text = message.asString();
                if (text != null && !text.isEmpty()) {
                    data.splashMessages.add(text);
                }
            }
        }
        data.fadeTicks = root.getInt("fadeTicks", 8);
        return data;
    }

    public static TitleData empty() {
        return new TitleData();
    }

    public boolean hasSplash() {
        return splashImages.size > 0 && secondsPerSplash > 0f;
    }

    /** L14: the splash fade duration in seconds (ticks / 20, RGSS timing). */
    public float fadeSeconds() {
        return Math.max(0f, fadeTicks / 20f);
    }
}
