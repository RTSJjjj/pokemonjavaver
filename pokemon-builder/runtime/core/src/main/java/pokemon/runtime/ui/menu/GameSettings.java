package pokemon.runtime.ui.menu;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import pokemon.runtime.app.StoragePort;

/**
 * L1/P4: the project's {@code PokemonSystem} options (PScreen_Options) plus the
 * audio volumes. Persisted as {@code settings.json} through the storage port.
 *
 * <p>{@link Option} mirrors the plugin's {@code @PokemonOptions} list in order
 * (the last entry is the "退出" row).</p>
 */
public final class GameSettings {

    /** The project's {@code $SpeechFrames} (MessageConfig + PScreen_Options). */
    public static final String[] SPEECH_FRAMES = {
            "speech bw 1", "speech hgss 2", "speech hgss 3", "speech hgss 4",
            "speech hgss 5", "speech hgss 6", "speech hgss 7", "speech hgss 8",
            "speech hgss 9", "speech hgss 10", "speech hgss 11", "speech hgss 12",
            "speech hgss 13", "speech hgss 14", "speech hgss 15", "speech hgss 16",
            "speech hgss 17", "speech hgss 18", "speech hgss 19", "speech hgss 20",
            "speech pl 18", "speech bw 1"
    };

    /** The project's {@code $TextFrames} (choice 1..choice 29). */
    public static final String[] TEXT_FRAMES = {
            "choice 1", "choice 2", "choice 3", "choice 4", "choice 5", "choice 6",
            "choice 7", "choice 8", "choice 9", "choice 10", "choice 11", "choice 12",
            "choice 13", "choice 14", "choice 15", "choice 16", "choice 17", "choice 18",
            "choice 19", "choice 20", "choice 21", "choice 22", "choice 23", "choice 24",
            "choice 25", "choice 26", "choice 27", "choice 28", "choice 29"
    };

    /** The option rows, in the plugin's order, plus the trailing 退出. */
    public enum Option {
        BGM("游戏音量", Kind.SLIDER, 0, 100, 5),
        SE("音效音量", Kind.SLIDER, 0, 100, 5),
        HEADTOP("头顶名称", Kind.ENUM, 0, 0, 0),
        AUTO_FOLLOW("自动跟随", Kind.ENUM, 0, 0, 0),
        TEXT_SPEED("文字速度", Kind.ENUM, 0, 0, 0),
        MEGA_ANIM("Mega动画", Kind.ENUM, 0, 0, 0),
        BATTLE_RULE("超级进化规则", Kind.ENUM, 0, 0, 0),
        BATTLE_SCENE("对战动画", Kind.ENUM, 0, 0, 0),
        BATTLE_STYLE("对战规则", Kind.ENUM, 0, 0, 0),
        RUN_STYLE("跑步键", Kind.ENUM, 0, 0, 0),
        TEXT_SKIN("对话栏", Kind.NUMBER, 1, SPEECH_FRAMES.length, 1),
        MENU_FRAME("菜单栏", Kind.NUMBER, 1, TEXT_FRAMES.length, 1),
        FONT("字体样式", Kind.ENUM, 0, 0, 0),
        SCREEN_SIZE("荧幕大小", Kind.ENUM, 0, 0, 0),
        EXIT("退出", Kind.EXIT, 0, 0, 0);

        public enum Kind { ENUM, NUMBER, SLIDER, EXIT }

        public final String label;
        public final Kind kind;
        public final int start;
        public final int end;
        public final int interval;

        Option(String label, Kind kind, int start, int end, int interval) {
            this.label = label;
            this.kind = kind;
            this.start = start;
            this.end = end;
            this.interval = interval;
        }

        /** The enum values (only for Kind.ENUM). */
        public String[] enumValues() {
            switch (this) {
                case HEADTOP: return new String[] {"隐藏", "主角", "NPC", "全开"};
                case AUTO_FOLLOW: return new String[] {"开", "关"};
                case TEXT_SPEED: return new String[] {"慢", "中", "快"};
                case MEGA_ANIM: return new String[] {"完整", "简洁"};
                case BATTLE_RULE: return new String[] {"传统模式", "ZA模式"};
                case BATTLE_SCENE: return new String[] {"开", "关"};
                case BATTLE_STYLE: return new String[] {"交换", "连战"};
                case RUN_STYLE: return new String[] {"按住", "切换"};
                case FONT: return new String[] {"字体1", "字体2", "字体3"};
                case SCREEN_SIZE: return new String[] {"小", "中", "大", "全屏"};
                default: return new String[0];
            }
        }
    }

    private static final String FILE = "settings.json";

    public int bgmVolume = 100;
    public int seVolume = 100;
    /** Internal: the project has no BGS option row, so this stays at 100. */
    public int bgsVolume = 100;
    public int headtopname = 0;
    public int autoFollow = 0;
    public int textspeed = 1;
    public int megaAnimation = 0;
    /** 0 = classic Mega Evolution, 1 = ZA mode. */
    public int battleRule = 0;
    public int battlescene = 0;
    public int battlestyle = 0;
    public int runstyle = 0;
    public int textskin = 0;
    public int frame = 0;
    public int font = 0;
    public int screensize = 1;

    /** Loads the file, falling back to the defaults when it is missing/broken. */
    public static GameSettings load(StoragePort storage) {
        GameSettings settings = new GameSettings();
        if (storage == null) {
            return settings;
        }
        try {
            String json = new String(storage.readUtf8(FILE), java.nio.charset.StandardCharsets.UTF_8);
            JsonValue root = new JsonReader().parse(json);
            settings.bgmVolume = clamp(root.getInt("bgmVolume", 100));
            settings.seVolume = clamp(root.getInt("seVolume", 100));
            settings.bgsVolume = clamp(root.getInt("bgsVolume", 100));
            settings.headtopname = root.getInt("headtopname", 0);
            settings.autoFollow = root.getInt("autoFollow", 0);
            settings.textspeed = root.getInt("textspeed", 1);
            settings.megaAnimation = root.getInt("megaAnimation", 0);
            settings.battleRule = root.getInt("battleRule", 0);
            settings.battlescene = root.getInt("battlescene", 0);
            settings.battlestyle = root.getInt("battlestyle", 0);
            settings.runstyle = root.getInt("runstyle", 0);
            settings.textskin = root.getInt("textskin", 0);
            settings.frame = root.getInt("frame", 0);
            settings.font = root.getInt("font", 0);
            settings.screensize = root.getInt("screensize", 1);
        } catch (RuntimeException error) {
            // missing or unreadable: the defaults are a valid state
        }
        return settings;
    }

    public void save(StoragePort storage) {
        if (storage == null) {
            return;
        }
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"bgmVolume\": ").append(bgmVolume).append(",\n");
        json.append("  \"seVolume\": ").append(seVolume).append(",\n");
        json.append("  \"bgsVolume\": ").append(bgsVolume).append(",\n");
        json.append("  \"headtopname\": ").append(headtopname).append(",\n");
        json.append("  \"autoFollow\": ").append(autoFollow).append(",\n");
        json.append("  \"textspeed\": ").append(textspeed).append(",\n");
        json.append("  \"megaAnimation\": ").append(megaAnimation).append(",\n");
        json.append("  \"battleRule\": ").append(battleRule).append(",\n");
        json.append("  \"battlescene\": ").append(battlescene).append(",\n");
        json.append("  \"battlestyle\": ").append(battlestyle).append(",\n");
        json.append("  \"runstyle\": ").append(runstyle).append(",\n");
        json.append("  \"textskin\": ").append(textskin).append(",\n");
        json.append("  \"frame\": ").append(frame).append(",\n");
        json.append("  \"font\": ").append(font).append(",\n");
        json.append("  \"screensize\": ").append(screensize).append("\n");
        json.append("}\n");
        try {
            storage.writeUtf8(FILE, json.toString());
        } catch (RuntimeException error) {
            // A read-only profile must not crash the game.
        }
    }

    /** The value index of one option (the enum index / number / slider value). */
    public int value(Option option) {
        switch (option) {
            case BGM: return bgmVolume;
            case SE: return seVolume;
            case HEADTOP: return headtopname;
            case AUTO_FOLLOW: return autoFollow;
            case TEXT_SPEED: return textspeed;
            case MEGA_ANIM: return megaAnimation;
            case BATTLE_RULE: return battleRule;
            case BATTLE_SCENE: return battlescene;
            case BATTLE_STYLE: return battlestyle;
            case RUN_STYLE: return runstyle;
            case TEXT_SKIN: return textskin;
            case MENU_FRAME: return frame;
            case FONT: return font;
            case SCREEN_SIZE: return Math.min(screensize, 3);
            default: return 0;
        }
    }

    /** Applies one option's value index. */
    public void setValue(Option option, int value) {
        switch (option) {
            case BGM: bgmVolume = clamp(value); break;
            case SE: seVolume = clamp(value); break;
            case HEADTOP: headtopname = value; break;
            case AUTO_FOLLOW: autoFollow = value; break;
            case TEXT_SPEED: textspeed = value; break;
            case MEGA_ANIM: megaAnimation = value; break;
            case BATTLE_RULE: battleRule = value; break;
            case BATTLE_SCENE: battlescene = value; break;
            case BATTLE_STYLE: battlestyle = value; break;
            case RUN_STYLE: runstyle = value; break;
            case TEXT_SKIN: textskin = value; break;
            case MENU_FRAME: frame = value; break;
            case FONT: font = value; break;
            case SCREEN_SIZE: screensize = value; break;
            default: break;
        }
    }

    /** LEFT/RIGHT on one option row (EnumOption/SliderOption/NumberOption). */
    public int step(Option option, int current, int delta) {
        switch (option.kind) {
            case ENUM: {
                int index = current + delta;
                return Math.max(0, Math.min(index, option.enumValues().length - 1));
            }
            case NUMBER: {
                // NumberOption: wraps around the range.
                int value = current + delta;
                if (value > option.end - option.start) value = 0;
                if (value < 0) value = option.end - option.start;
                return value;
            }
            case SLIDER: {
                // SliderOption: clamps at the ends.
                int value = option.start + current + delta * option.interval;
                value = Math.max(option.start, Math.min(option.end, value));
                return value - option.start;
            }
            default:
                return current;
        }
    }

    /** The display value of one option (PokeBattle_Scene/PokemonOption#drawItem). */
    public String display(Option option) {
        switch (option.kind) {
            case SLIDER:
                return String.valueOf(option.start + value(option));
            case NUMBER:
                return "Type " + (option.start + value(option)) + "/"
                        + (option.end - option.start + 1);
            case ENUM: {
                String[] values = option.enumValues();
                int index = value(option);
                return index >= 0 && index < values.length ? values[index] : "";
            }
            default:
                return "";
        }
    }

    public float bgmFactor() {
        return bgmVolume / 100f;
    }

    /** 荧幕大小 3 (全屏) drives the desktop fullscreen toggle. */
    public boolean fullscreen() {
        return screensize >= 3;
    }

    public float seFactor() {
        return seVolume / 100f;
    }

    public float bgsFactor() {
        return bgsVolume / 100f;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
