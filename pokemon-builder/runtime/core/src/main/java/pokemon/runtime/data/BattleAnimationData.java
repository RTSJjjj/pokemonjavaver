package pokemon.runtime.data;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code Data/PkmnAnimations.rxdata} ({@code PBAnimations}) and
 * {@code Data/move2anim.dat} as exported by the Builder
 * ({@code generated/battle-animations/}): the data behind
 * {@code PBAnimationPlayerX} (PokeBattle_AnimationPlayer:704-877).
 *
 * <p>The index ({@code index.json}) is read up front; each animation is its own
 * file and is parsed the first time it is played, because the whole set is
 * ~11 MB (1830 animations, ~250k cels).</p>
 */
public final class BattleAnimationData {

    /**
     * One cel: the slots of the plugin's {@code AnimFrame} array that
     * {@code pbSpriteSetAnimFrame} reads (PokeBattle_AnimationPlayer:624-697).
     */
    public static final class Cel {
        /** {@code AnimFrame::PATTERN}: sheet cell, -1 = user's sprite, -2 = target's sprite. */
        public int pattern;
        /** {@code AnimFrame::X}/{@code Y} are Floats in the data; RGSS truncates when assigning. */
        public float x;
        public float y;
        public int zoomX = 100;
        public int zoomY = 100;
        public int angle;
        public int mirror;
        public int opacity = 255;
        /** 0 normal, 1 additive, 2 subtractive. */
        public int blend;
        /** 0 back, 1 front, 2 behind focus, 3 before focus (pbResetCel:109). */
        public int priority = 1;
        /** 1 target, 2 user, 3 user and target, 4 screen (pbCreateCel:82). */
        public int focus = 4;
        public int visible = 1;
        /** {@code COLORRED..COLORALPHA}, {@code TONERED..TONEGRAY}. */
        public final int[] color = new int[4];
        public final int[] tone = new int[4];
    }

    /** {@code PBAnimTiming} (PokeBattle_AnimationPlayer:241-276). */
    public static final class Timing {
        public int frame;
        /** 0 = play SE, 1 = set bg, 2 = bg mod, 3 = set fg, 4 = fg mod. */
        public int type;
        public String name = "";
        public int volume = 80;
        public int pitch = 100;
        public Integer bgX;
        public Integer bgY;
        public Integer opacity;
        public Integer colorRed;
        public Integer colorGreen;
        public Integer colorBlue;
        public Integer colorAlpha;
        public int duration = 5;
    }

    /** {@code PBAnimation} (PokeBattle_AnimationPlayer:419-498). */
    public static final class Animation {
        public int id;
        public String name = "";
        public String graphic = "";
        public int hue;
        public int position = 4;
        /** {@code @array}: frames of cels, null = nil cel. */
        public Cel[][] frames = new Cel[0][];
        public Timing[] timings = new Timing[0];

        /** {@code PBAnimation#length}. */
        public int length() {
            return frames.length;
        }
    }

    private final File dir;
    private final int[] moveToAnim;
    private final int[] oppMoveToAnim;
    private final int[] ids;
    private final String[] names;
    private final String[] files;
    private final Map<Integer, Animation> loaded = new HashMap<>();
    private final Map<Integer, Integer> indexById = new HashMap<>();

    private BattleAnimationData(File dir, JsonValue index) {
        this.dir = dir;
        this.moveToAnim = intTable(index == null ? null : index.get("moveToAnim"));
        this.oppMoveToAnim = intTable(index == null ? null : index.get("oppMoveToAnim"));
        JsonValue list = index == null ? null : index.get("animations");
        int count = 0;
        if (list != null && list.isArray()) {
            count = list.size;
        }
        ids = new int[count];
        names = new String[count];
        files = new String[count];
        int i = 0;
        if (list != null && list.isArray()) {
            for (JsonValue entry = list.child; entry != null; entry = entry.next, i++) {
                ids[i] = entry.getInt("id", -1);
                names[i] = entry.getString("name", "");
                files[i] = entry.getString("file", "");
                indexById.put(ids[i], i);
            }
        }
    }

    /** -1 for {@code nil} entries (the plugin's holes in the move table). */
    private static int[] intTable(JsonValue table) {
        if (table == null || !table.isArray()) {
            return new int[0];
        }
        int[] out = new int[table.size];
        int i = 0;
        for (JsonValue entry = table.child; entry != null; entry = entry.next, i++) {
            out[i] = entry.isNumber() ? entry.asInt() : -1;
        }
        return out;
    }

    /**
     * Opens {@code <dataRoot>/battle-animations}; returns an empty set when the
     * project has none (no animation plays, like {@code pbLoadBattleAnimations}
     * returning nil, Scene_Animations:515-516).
     */
    public static BattleAnimationData load(File dataRoot) {
        File dir = new File(dataRoot, "battle-animations");
        File index = new File(dir, "index.json");
        if (!index.isFile()) {
            // Stage 4: a packaged data root that predates battle-animations (or a
            // build that skipped it) used to fail SILENTLY here - every common
            // animation (stat changes, status animations) and every move animation
            // simply did not play, with nothing in the log to say why.
            System.err.println("[battle-animations] missing " + index
                    + " - no battle animation will play; re-run the builder (build-data/build-pc)");
            return new BattleAnimationData(dir, null);
        }
        try {
            String text = pokemon.runtime.data.ResourceCrypto.readString(index);
            return new BattleAnimationData(dir, new JsonReader().parse(text));
        } catch (IOException | RuntimeException error) {
            System.err.println("[battle-animations] cannot read " + index + ": " + error);
            return new BattleAnimationData(dir, null);
        }
    }

    /** True when {@code pbLoadBattleAnimations} would return something. */
    public boolean available() {
        return ids.length > 0;
    }

    /**
     * {@code move2anim[0][moveID]} / {@code move2anim[1][moveID]}; -1 = nil. Ruby
     * arrays take a negative index from the end (Struggle's id is -1), and an
     * index past the end gives nil.
     */
    public int moveAnim(int moveId, boolean opposing) {
        int[] table = opposing ? oppMoveToAnim : moveToAnim;
        int index = moveId < 0 ? moveId + table.length : moveId;
        return index >= 0 && index < table.length ? table[index] : -1;
    }

    /**
     * {@code animations[anim]} (Scene_Animations:519/521): the animation whose
     * position in {@code PBAnimations} is {@code id}, or null.
     */
    public Animation animation(int id) {
        Integer index = indexById.get(id);
        if (index == null) {
            return null;
        }
        Animation cached = loaded.get(id);
        if (cached != null) {
            return cached;
        }
        File file = new File(dir, files[index]);
        try {
            String text = pokemon.runtime.data.ResourceCrypto.readString(file);
            Animation animation = parse(new JsonReader().parse(text));
            loaded.put(id, animation);
            return animation;
        } catch (IOException | RuntimeException error) {
            return null;
        }
    }

    /**
     * {@code pbCommonAnimation}'s search (Scene_Animations:535-539): the first
     * animation named {@code "Common:"+animName}, in array order.
     */
    public Animation common(String animName) {
        String wanted = "Common:" + animName;
        for (int i = 0; i < ids.length; i++) {
            if (wanted.equals(names[i])) {
                return animation(ids[i]);
            }
        }
        return null;
    }

    static Animation parse(JsonValue root) {
        Animation animation = new Animation();
        animation.id = root.getInt("id", -1);
        animation.name = root.getString("name", "");
        animation.graphic = root.getString("graphic", "");
        animation.hue = root.getInt("hue", 0);
        animation.position = root.getInt("position", 4);
        JsonValue frames = root.get("frames");
        int frameCount = frames == null ? 0 : frames.size;
        animation.frames = new Cel[frameCount][];
        int f = 0;
        for (JsonValue frame = frames == null ? null : frames.child; frame != null; frame = frame.next, f++) {
            Cel[] cels = new Cel[frame.size];
            int c = 0;
            for (JsonValue cel = frame.child; cel != null; cel = cel.next, c++) {
                cels[c] = cel.isNull() ? null : parseCel(cel);
            }
            animation.frames[f] = cels;
        }
        JsonValue timings = root.get("timings");
        int timingCount = timings == null ? 0 : timings.size;
        animation.timings = new Timing[timingCount];
        int t = 0;
        for (JsonValue timing = timings == null ? null : timings.child; timing != null; timing = timing.next, t++) {
            animation.timings[t] = parseTiming(timing);
        }
        return animation;
    }

    /** Positional layout documented in {@code tools/data-converter/battle-animations.js}. */
    private static Cel parseCel(JsonValue values) {
        Cel cel = new Cel();
        JsonValue v = values.child;
        cel.pattern = v.asInt(); v = v.next;
        cel.x = v.asFloat(); v = v.next;
        cel.y = v.asFloat(); v = v.next;
        cel.zoomX = v.asInt(); v = v.next;
        cel.zoomY = v.asInt(); v = v.next;
        cel.angle = v.asInt(); v = v.next;
        cel.mirror = v.asInt(); v = v.next;
        cel.opacity = v.asInt(); v = v.next;
        cel.blend = v.asInt(); v = v.next;
        cel.priority = v.asInt(); v = v.next;
        cel.focus = v.asInt(); v = v.next;
        cel.visible = v.asInt(); v = v.next;
        if (v != null) {
            for (int i = 0; i < 4; i++, v = v.next) {
                cel.color[i] = v.asInt();
            }
            for (int i = 0; i < 4; i++, v = v.next) {
                cel.tone[i] = v.asInt();
            }
        }
        return cel;
    }

    private static Integer optInt(JsonValue parent, String name) {
        JsonValue value = parent.get(name);
        return value == null || value.isNull() ? null : Integer.valueOf(value.asInt());
    }

    private static Timing parseTiming(JsonValue json) {
        Timing timing = new Timing();
        timing.frame = json.getInt("frame", 0);
        timing.type = json.getInt("type", 0);
        timing.name = json.getString("name", "");
        timing.volume = json.getInt("volume", 80);
        timing.pitch = json.getInt("pitch", 100);
        timing.bgX = optInt(json, "bgX");
        timing.bgY = optInt(json, "bgY");
        timing.opacity = optInt(json, "opacity");
        timing.colorRed = optInt(json, "colorRed");
        timing.colorGreen = optInt(json, "colorGreen");
        timing.colorBlue = optInt(json, "colorBlue");
        timing.colorAlpha = optInt(json, "colorAlpha");
        timing.duration = json.getInt("duration", 5);
        return timing;
    }
}
