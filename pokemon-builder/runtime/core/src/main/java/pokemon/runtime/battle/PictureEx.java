package pokemon.runtime.battle;

import java.util.ArrayList;
import java.util.List;

/**
 * The plugin's scripted-sprite animation engine, transcribed line by line from
 * the {@code PictureEx} section (520 lines). Every battle animation class in
 * {@code PokeBattle_SceneAnimations} / {@code Follower_Main} is a list of
 * {@code setX}/{@code moveX} calls on these objects, so the battle scene's intro
 * and send-out animations are only reproducible once this engine exists.
 *
 * <p>Mapping to the Ruby source:</p>
 * <ul>
 *   <li>{@code PictureOrigin} - {@code PictureEx:1-13}</li>
 *   <li>{@code Processes} - {@code PictureEx:17-36}</li>
 *   <li>{@code getCubicPoint2} - {@code PictureEx:40-69}</li>
 *   <li>{@code PictureEx} - {@code PictureEx:76-447}</li>
 *   <li>{@code setPictureSprite} origin/visible/... application -
 *       {@code PictureEx:454-519} (folded into
 *       {@link #applyTo(BattleSprite)}).</li>
 * </ul>
 *
 * <p>Ruby's {@code process[i]} array is a {@link Process} whose fields are named
 * after the indices in a comment, so the transcription can be checked against
 * the source one line at a time.</p>
 */
final class PictureEx {

    /** {@code PictureOrigin} (PictureEx:1-13). */
    static final class Origin {
        /**
         * Not a plugin value: {@code addSprite(s, origin = PictureOrigin::TopLeft)}
         * still accepts an explicit {@code nil} (PokeBattle_SceneAnimations:50-57
         * passes none), which leaves {@code ox}/{@code oy} untouched because no
         * {@code when} branch of {@code setPictureSprite:483-498} matches.
         */
        static final int NONE = -1;
        static final int TOP_LEFT = 0;
        static final int CENTER = 1;
        static final int TOP_RIGHT = 2;
        static final int BOTTOM_LEFT = 3;
        static final int LOWER_LEFT = 3;
        static final int BOTTOM_RIGHT = 4;
        static final int LOWER_RIGHT = 4;
        static final int TOP = 5;
        static final int BOTTOM = 6;
        static final int LEFT = 7;
        static final int RIGHT = 8;

        private Origin() {
        }
    }

    /** {@code Processes} (PictureEx:17-36). */
    static final class Processes {
        static final int XY = 0;
        static final int DELTA_XY = 1;
        static final int Z = 2;
        static final int CURVE = 3;
        static final int ZOOM = 4;
        static final int ANGLE = 5;
        static final int TONE = 6;
        static final int COLOR = 7;
        static final int HUE = 8;
        static final int OPACITY = 9;
        static final int VISIBLE = 10;
        static final int BLEND_TYPE = 11;
        static final int SE = 12;
        static final int NAME = 13;
        static final int ORIGIN = 14;
        static final int SRC = 15;
        static final int SRC_SIZE = 16;
        static final int CROP_BOTTOM = 17;
        /** {@code process[0] == nil} for a pure callback entry (PictureEx:128). */
        static final int CALLBACK = -1;

        private Processes() {
        }
    }

    /** {@code pbSEPlay(param,volume=nil,pitch=nil)} (Audio_Play:200-217). */
    interface SePlayer {
        void playSe(String name, int volume, int pitch);
    }

    /** {@code cb[0].method(cb[1]).call(self)} (PictureEx:119-124). */
    interface Callback {
        void call(PictureEx picture);
    }

    /** One entry of {@code @processes} (PictureEx:96). */
    private static final class Process {
        /** {@code process[0]} - a {@code Processes::*} value or {@link Processes#CALLBACK}. */
        int type = Processes.CALLBACK;
        /** {@code process[1]} - the countdown before the process starts. */
        int delay;
        /** {@code process[2]} - the total duration in frames (0 = instant). */
        int duration;
        /** {@code process[3]} - the frame counter while running. */
        int frame;
        /** {@code process[4]}. */
        Callback callback;
        /** {@code process[5]} / {@code process[6]}. */
        float v0;
        float v1;
        /** {@code process[7]} / {@code process[8]}. */
        float v2;
        float v3;
        /** {@code process[5..8]} for a Curve process (PictureEx:206). */
        float[] curve;
        /** {@code process[5]} / {@code process[6]} for Tone and Color (clones). */
        float[] from;
        float[] to;
        /** {@code process[5]} for Src / SrcSize. */
        int i0;
        int i1;
        /** {@code process[5]} for Name / SE and the SE's volume/pitch. */
        String text;
        float seVolume;
        float sePitch;
        /** {@code process[5]} for Visible. */
        boolean flag;
    }

    // PictureEx:95-117
    private final List<Process> processes = new ArrayList<>();
    private float x;
    private float y;
    private int z;
    private float zoomX = 100f;
    private float zoomY = 100f;
    private float angle;
    /** {@code @rotate_speed} (PictureEx:104/164-168). */
    private float rotateSpeed;
    private final float[] tone = { 0f, 0f, 0f, 0f };
    private final float[] color = { 0f, 0f, 0f, 0f };
    private float hue;
    private float opacity = 255f;
    private boolean visible = true;
    private int blendType;
    private String name = "";
    private int origin = Origin.TOP_LEFT;
    /** {@code @src_rect} - width/height -1 means "the whole bitmap" (PictureEx:114). */
    private int srcX;
    private int srcY;
    private int srcWidth = -1;
    private int srcHeight = -1;
    /** {@code @cropBottom} (PictureEx:115). */
    private int cropBottom = -1;
    private final SePlayer sePlayer;
    /** The sprite this picture drives ({@code @pictureSprites[num]}). */
    private BattleSprite sprite;

    PictureEx(int initialZ, SePlayer sePlayer) {
        this.z = initialZ;                                     // PictureEx:100
        this.sePlayer = sePlayer;
    }

    // ------------------------------------------------------------------
    // Accessors used by the animation classes' createProcesses
    // ------------------------------------------------------------------

    float x() {
        return x;
    }

    float y() {
        return y;
    }

    int z() {
        return z;
    }

    float zoomX() {
        return zoomX;
    }

    float zoomY() {
        return zoomY;
    }

    float angle() {
        return angle;
    }

    float opacity() {
        return opacity;
    }

    boolean visible() {
        return visible;
    }

    String name() {
        return name;
    }

    int origin() {
        return origin;
    }

    int srcX() {
        return srcX;
    }

    int srcY() {
        return srcY;
    }

    int srcWidth() {
        return srcWidth;
    }

    int srcHeight() {
        return srcHeight;
    }

    int cropBottom() {
        return cropBottom;
    }

    float[] tone() {
        return tone;
    }

    float[] color() {
        return color;
    }

    BattleSprite sprite() {
        return sprite;
    }

    void setSprite(BattleSprite value) {
        this.sprite = value;
    }

    /** PictureEx:131-133. */
    boolean running() {
        return !processes.isEmpty();
    }

    /** {@code @animDone}: every process has finished (PokeBattle_Animation:57). */
    boolean done() {
        return processes.isEmpty();
    }

    /**
     * {@code totalDuration} (PictureEx:135-143). Note that it converts frames
     * back into the 1/20-second units the animation classes pass to
     * {@code ensureDelayAndDuration}, i.e. it halves and truncates.
     */
    int totalDuration() {
        int ret = 0;
        for (Process process : processes) {
            int dur = process.delay + process.duration;         // :138
            if (dur > ret) {
                ret = dur;                                      // :139
            }
        }
        ret = (int) (ret * 20.0 / 40);                          // :141
        return ret;                                            // :142
    }

    /** {@code ensureDelayAndDuration} (PictureEx:145-153). */
    private int ensureDelay(int delay) {
        if (delay < 0) {
            delay = totalDuration();                            // :146
        }
        return (int) (delay * 40 / 20.0);                       // :147
    }

    private int[] ensureDelayAndDuration(int delay, int duration) {
        int d = ensureDelay(delay);
        return new int[] { d, (int) (duration * 40 / 20.0) };   // :149-150
    }

    private static Process base(int type, int delay, int duration) {
        Process process = new Process();
        process.type = type;
        process.delay = delay;
        process.duration = duration;
        return process;
    }

    // ------------------------------------------------------------------
    // The set*/move* builders (PictureEx:126-333)
    // ------------------------------------------------------------------

    /** {@code setCallback} (PictureEx:126-129). */
    void setCallback(int delay, Callback callback) {
        Process process = base(Processes.CALLBACK, ensureDelay(delay), 0);   // :127-128
        process.callback = callback;
        processes.add(process);
    }

    /** {@code rotate} (PictureEx:164-168). The runtime's frame rate is 40. */
    void rotate(float speed) {
        rotateSpeed = speed * 20f / Graphics.FRAME_RATE;        // :165
        while (rotateSpeed < 0) {
            rotateSpeed += 360;                                 // :166
        }
        rotateSpeed %= 360;                                     // :167
    }

    /**
     * {@code move} (PictureEx:188-193). Transcribed as written, including its
     * {@code setOrigin(delay,duration,origin)} call - Ruby binds those three
     * arguments to {@code (delay, origin, cb)}, so the middle argument lands in
     * {@code origin}. No battle animation calls {@code move} (they use the
     * individual setters), so the quirk never shows.
     */
    void move(int delay, int duration, int origin, float nx, float ny, float nzoomX, float nzoomY,
            float nopacity) {
        setOrigin(delay, duration);                             // :189
        moveXY(delay, duration, nx, ny);                        // :190
        moveZoomXY(delay, duration, nzoomX, nzoomY);            // :191
        moveOpacity(delay, duration, nopacity);                 // :192
    }

    /** {@code moveXY} (PictureEx:195-198). */
    void moveXY(int delay, int duration, float nx, float ny) {
        int[] d = ensureDelayAndDuration(delay, duration);      // :196
        Process process = base(Processes.XY, d[0], d[1]);       // :197
        process.v0 = x;                                         // @x
        process.v1 = y;                                         // @y
        process.v2 = nx;
        process.v3 = ny;
        processes.add(process);
    }

    void setXY(int delay, float nx, float ny) {
        moveXY(delay, 0, nx, ny);                               // :200-202
    }

    /** {@code moveCurve} (PictureEx:204-207). */
    void moveCurve(int delay, int duration, float x1, float y1, float x2, float y2, float x3,
            float y3) {
        int[] d = ensureDelayAndDuration(delay, duration);
        Process process = base(Processes.CURVE, d[0], d[1]);
        process.curve = new float[] { x, y, x1, y1, x2, y2, x3, y3 };
        processes.add(process);
    }

    /** {@code moveDelta} (PictureEx:209-212). */
    void moveDelta(int delay, int duration, float dx, float dy) {
        int[] d = ensureDelayAndDuration(delay, duration);      // :210
        Process process = base(Processes.DELTA_XY, d[0], d[1]);
        process.v0 = x;                                         // :211 (@x)
        process.v1 = y;                                         //      (@y)
        process.v2 = dx;
        process.v3 = dy;
        processes.add(process);
    }

    void setDelta(int delay, float dx, float dy) {
        moveDelta(delay, 0, dx, dy);                            // :214-216
    }

    /** {@code moveZ} (PictureEx:218-221). */
    void moveZ(int delay, int duration, float nz) {
        int[] d = ensureDelayAndDuration(delay, duration);
        Process process = base(Processes.Z, d[0], d[1]);
        process.v0 = z;
        process.v1 = nz;
        processes.add(process);
    }

    void setZ(int delay, float nz) {
        moveZ(delay, 0, nz);                                    // :223-225
    }

    /** {@code moveZoomXY} (PictureEx:227-230). */
    void moveZoomXY(int delay, int duration, float nzoomX, float nzoomY) {
        moveZoomXY(delay, duration, nzoomX, nzoomY, null);
    }

    void moveZoomXY(int delay, int duration, float nzoomX, float nzoomY, Callback callback) {
        int[] d = ensureDelayAndDuration(delay, duration);
        Process process = base(Processes.ZOOM, d[0], d[1]);
        process.v0 = zoomX;
        process.v1 = zoomY;
        process.v2 = nzoomX;
        process.v3 = nzoomY;
        process.callback = callback;
        processes.add(process);
    }

    void setZoomXY(int delay, float nzoomX, float nzoomY) {
        moveZoomXY(delay, 0, nzoomX, nzoomY);                   // :232-234
    }

    void moveZoom(int delay, int duration, float zoom) {
        moveZoomXY(delay, duration, zoom, zoom);                // :236-238
    }

    /** {@code moveZoom(delay,duration,zoom,cb)} (PictureEx:236-238). */
    void moveZoom(int delay, int duration, float zoom, Callback callback) {
        moveZoomXY(delay, duration, zoom, zoom, callback);
    }

    void setZoom(int delay, float zoom) {
        moveZoomXY(delay, 0, zoom, zoom);                       // :240-242
    }

    /** {@code moveAngle} (PictureEx:244-247). */
    void moveAngle(int delay, int duration, float nangle) {
        int[] d = ensureDelayAndDuration(delay, duration);
        Process process = base(Processes.ANGLE, d[0], d[1]);
        process.v0 = angle;
        process.v1 = nangle;
        processes.add(process);
    }

    void setAngle(int delay, float nangle) {
        moveAngle(delay, 0, nangle);                            // :249-251
    }

    /** {@code moveTone} (PictureEx:253-257). */
    void moveTone(int delay, int duration, float[] target) {
        int[] d = ensureDelayAndDuration(delay, duration);
        Process process = base(Processes.TONE, d[0], d[1]);
        process.from = tone.clone();
        process.to = target == null ? new float[] { 0, 0, 0, 0 } : target.clone();   // :255
        processes.add(process);
    }

    void setTone(int delay, float[] target) {
        moveTone(delay, 0, target);                             // :259-261
    }

    /** {@code moveColor} (PictureEx:263-267). */
    void moveColor(int delay, int duration, float[] target) {
        int[] d = ensureDelayAndDuration(delay, duration);
        Process process = base(Processes.COLOR, d[0], d[1]);
        process.from = color.clone();                           // :266
        process.to = target == null ? new float[] { 0, 0, 0, 0 } : target.clone();
        processes.add(process);
    }

    void setColor(int delay, float[] target) {
        moveColor(delay, 0, target);                            // :269-271
    }

    /** {@code moveOpacity} (PictureEx:284-287). */
    void moveOpacity(int delay, int duration, float nopacity) {
        int[] d = ensureDelayAndDuration(delay, duration);
        Process process = base(Processes.OPACITY, d[0], d[1]);
        process.v0 = opacity;
        process.v1 = nopacity;
        processes.add(process);
    }

    void setOpacity(int delay, float nopacity) {
        moveOpacity(delay, 0, nopacity);                        // :289-291
    }

    /** {@code setVisible} (PictureEx:293-296). */
    void setVisible(int delay, boolean nvisible) {
        Process process = base(Processes.VISIBLE, ensureDelay(delay), 0);    // :294-295
        process.flag = nvisible;
        processes.add(process);
    }

    /** {@code setBlendType} (PictureEx:298-302): 0 normal, 1 additive, 2 subtractive. */
    void setBlendType(int delay, int blend) {
        Process process = base(Processes.BLEND_TYPE, ensureDelay(delay), 0);   // :300-301
        process.v0 = blend;
        processes.add(process);
    }

    /** {@code setSE} (PictureEx:304-307). */
    void setSE(int delay, String seFile) {
        setSE(delay, seFile, null, null);
    }

    void setSE(int delay, String seFile, Integer volume, Integer pitch) {
        Process process = base(Processes.SE, ensureDelay(delay), 0);         // :305-306
        process.text = seFile;
        process.seVolume = volume == null ? 100f : volume;
        process.sePitch = pitch == null ? 100f : pitch;
        processes.add(process);
    }

    /** {@code setName} (PictureEx:309-312). */
    void setName(int delay, String nname) {
        Process process = base(Processes.NAME, ensureDelay(delay), 0);        // :310-311
        process.text = nname;
        processes.add(process);
    }

    /** {@code setOrigin} (PictureEx:314-317). */
    void setOrigin(int delay, int norigin) {
        Process process = base(Processes.ORIGIN, ensureDelay(delay), 0);      // :315-316
        process.i0 = norigin;
        processes.add(process);
    }

    /** {@code setSrc} (PictureEx:319-322). */
    void setSrc(int delay, int sx, int sy) {
        Process process = base(Processes.SRC, ensureDelay(delay), 0);         // :320-321
        process.i0 = sx;
        process.i1 = sy;
        processes.add(process);
    }

    /** {@code setSrcSize} (PictureEx:324-327). */
    void setSrcSize(int delay, int sw, int sh) {
        Process process = base(Processes.SRC_SIZE, ensureDelay(delay), 0);    // :325-326
        process.i0 = sw;
        process.i1 = sh;
        processes.add(process);
    }

    /** {@code setCropBottom} (PictureEx:330-333). */
    void setCropBottom(int delay, int ny) {
        Process process = base(Processes.CROP_BOTTOM, ensureDelay(delay), 0); // :331-332
        process.i0 = ny;
        processes.add(process);
    }

    /** {@code empty?} (PokeBattle_Animation:17). */
    boolean emptyProcesses() {
        return processes.isEmpty();
    }

    // ------------------------------------------------------------------
    // update (PictureEx:335-446)
    // ------------------------------------------------------------------

    private final List<Integer> frameUpdates = new ArrayList<>();

    /** The {@code Processes::*} values that changed during the last update. */
    List<Integer> frameUpdates() {
        return frameUpdates;
    }

    void update() {
        boolean procEnded = false;                              // :336
        frameUpdates.clear();                                   // :337
        List<Process> kept = new ArrayList<>(processes.size());
        for (Process process : processes) {                     // :338
            boolean ended = false;
            if (process.delay >= 0) {                           // :341
                if (process.delay == 0) {                       // :343
                    switch (process.type) {                     // :344-371
                        case Processes.XY:
                            process.v0 = x;                     // :346
                            process.v1 = y;                     // :347
                            break;
                        case Processes.DELTA_XY:
                            process.v0 = x;                     // :349
                            process.v1 = y;                     // :350
                            process.v2 += x;                    // :351
                            process.v3 += y;                    // :352
                            break;
                        case Processes.CURVE:
                            process.curve[0] = x;               // :354
                            process.curve[1] = y;               // :355
                            break;
                        case Processes.Z:
                            process.v0 = z;                     // :357
                            break;
                        case Processes.ZOOM:
                            process.v0 = zoomX;                 // :359
                            process.v1 = zoomY;                 // :360
                            break;
                        case Processes.ANGLE:
                            process.v0 = angle;                 // :362
                            break;
                        case Processes.TONE:
                            process.from = tone.clone();        // :364
                            break;
                        case Processes.COLOR:
                            process.from = color.clone();       // :366
                            break;
                        case Processes.HUE:
                            process.v0 = hue;                   // :368
                            break;
                        case Processes.OPACITY:
                            process.v0 = opacity;               // :370
                            break;
                        default:
                            break;
                    }
                }
                process.delay -= 1;                             // :374
                if (process.delay >= 0) {                       // :376
                    kept.add(process);
                    continue;
                }
            }
            if (!frameUpdates.contains(process.type)) {         // :379
                frameUpdates.add(process.type);
            }
            int fra = process.duration == 0 ? 1 : process.frame;    // :380
            int dur = process.duration == 0 ? 1 : process.duration; // :381
            switch (process.type) {                             // :382-427
                case Processes.XY:
                case Processes.DELTA_XY:
                    x = process.v0 + fra * (process.v2 - process.v0) / dur;   // :384
                    y = process.v1 + fra * (process.v3 - process.v1) / dur;   // :385
                    break;
                case Processes.CURVE: {
                    float[] p = cubicPoint(process.curve, (float) fra / dur); // :387
                    x = p[0];
                    y = p[1];
                    break;
                }
                case Processes.Z:
                    z = (int) (process.v0 + fra * (process.v1 - process.v0) / dur);   // :389
                    break;
                case Processes.ZOOM:
                    zoomX = process.v0 + fra * (process.v2 - process.v0) / dur;       // :391
                    zoomY = process.v1 + fra * (process.v3 - process.v1) / dur;       // :392
                    break;
                case Processes.ANGLE:
                    angle = process.v0 + fra * (process.v1 - process.v0) / dur;       // :394
                    break;
                case Processes.TONE:
                    for (int c = 0; c < 4; c++) {                                     // :396-399
                        tone[c] = process.from[c] + fra * (process.to[c] - process.from[c]) / dur;
                    }
                    break;
                case Processes.COLOR:
                    for (int c = 0; c < 4; c++) {                                     // :401-404
                        color[c] = process.from[c] + fra * (process.to[c] - process.from[c]) / dur;
                    }
                    break;
                case Processes.HUE:
                    // PictureEx:406 stores the rate rather than the hue; the
                    // plugin's own comments (:273/279) say hue changes do
                    // nothing anyway. No battle animation queues one.
                    hue = (process.v1 - process.v0) / dur;                            // :406
                    break;
                case Processes.OPACITY:
                    opacity = process.v0 + fra * (process.v1 - process.v0) / dur;     // :408
                    break;
                case Processes.VISIBLE:
                    visible = process.flag;                                           // :410
                    break;
                case Processes.BLEND_TYPE:
                    blendType = (int) process.v0;                                     // :412
                    break;
                case Processes.SE:
                    if (sePlayer != null) {                                           // :414
                        sePlayer.playSe(process.text, (int) process.seVolume, (int) process.sePitch);
                    }
                    break;
                case Processes.NAME:
                    name = process.text;                                             // :416
                    break;
                case Processes.ORIGIN:
                    origin = process.i0;                                             // :418
                    break;
                case Processes.SRC:
                    srcX = process.i0;                                               // :420-421
                    srcY = process.i1;
                    break;
                case Processes.SRC_SIZE:
                    srcWidth = process.i0;                                           // :423-424
                    srcHeight = process.i1;
                    break;
                case Processes.CROP_BOTTOM:
                    cropBottom = process.i0;                                         // :426
                    break;
                default:
                    break;
            }
            process.frame += 1;                                 // :429
            if (process.frame > process.duration) {             // :430
                if (process.callback != null) {                 // :432
                    process.callback.call(this);
                }
                ended = true;                                   // :433
                procEnded = true;
            }
            if (!ended) {
                kept.add(process);
            }
        }
        if (procEnded) {                                        // :438
            processes.clear();
            processes.addAll(kept);
        }
        if (rotateSpeed != 0) {                                 // :440
            if (!frameUpdates.contains(Processes.ANGLE)) {      // :441
                frameUpdates.add(Processes.ANGLE);
            }
            angle += rotateSpeed;                               // :442
            while (angle < 0) {
                angle += 360;                                   // :443
            }
            angle %= 360;                                       // :444
        }
    }

    /** {@code getCubicPoint2} (PictureEx:40-69). */
    static float[] cubicPoint(float[] src, float t) {
        float x0 = src[0];
        float y0 = src[1];
        float cx0 = src[2];
        float cy0 = src[3];
        float cx1 = src[4];
        float cy1 = src[5];
        float x1 = src[6];
        float y1 = src[7];

        x1 = cx1 + (x1 - cx1) * t;                              // :46
        x0 = x0 + (cx0 - x0) * t;                               // :47
        cx0 = cx0 + (cx1 - cx0) * t;                            // :48
        cx1 = cx0 + (x1 - cx0) * t;                             // :49
        cx0 = x0 + (cx0 - x0) * t;                              // :50
        float cx = cx0 + (cx1 - cx0) * t;                       // :51

        y1 = cy1 + (y1 - cy1) * t;                              // :57
        y0 = y0 + (cy0 - y0) * t;                               // :58
        cy0 = cy0 + (cy1 - cy0) * t;                            // :59
        cy1 = cy0 + (y1 - cy0) * t;                             // :60
        cy0 = y0 + (cy0 - y0) * t;                              // :61
        float cy = cy0 + (cy1 - cy0) * t;                       // :62
        return new float[] { cx, cy };
    }

    // ------------------------------------------------------------------
    // setPictureSprite (PictureEx:454-519)
    // ------------------------------------------------------------------

    /**
     * {@code setPictureIconSprite} (PictureEx:517-519) - writes the picture's
     * current state onto its sprite. Like {@code setPictureSprite} (:454-515) it
     * only touches the fields whose process ran during the last update
     * ({@code picture.frameUpdates}), which is why a sprite keeps its own bitmap
     * {@code name}, source rectangle and origin until an animation changes them.
     */
    void applyTo(BattleSprite target) {
        applyTo(target, null);
    }

    /**
     * {@link #applyTo(BattleSprite)} with the bitmap sizes, so a {@code Name}
     * process can swap the bitmap like {@code IconSprite#name=} does.
     */
    void applyTo(BattleSprite target, BattleAnimation.BitmapSize sizes) {
        if (target == null) {
            return;
        }
        for (int type : frameUpdates) {
            switch (type) {
                case Processes.XY:
                case Processes.DELTA_XY:
                    target.x = Math.round(x);                   // :459-460
                    target.y = Math.round(y);
                    break;
                case Processes.Z:
                    target.z = z;                               // :462
                    break;
                case Processes.ZOOM:
                    target.zoomX = zoomX / 100f;                // :464-465
                    target.zoomY = zoomY / 100f;
                    break;
                case Processes.ANGLE:
                    target.angle = angle;                       // :467
                    break;
                case Processes.TONE:
                    target.tone = tone.clone();                 // :469
                    break;
                case Processes.COLOR:
                    target.color = color.clone();               // :471
                    break;
                case Processes.HUE:
                    break;                                      // :473 "doesn't do anything"
                case Processes.BLEND_TYPE:
                    target.blendType = blendType;               // :475
                    break;
                case Processes.OPACITY:
                    target.opacity = (float) (int) opacity;     // :477 (an Integer property in RMXP)
                    break;
                case Processes.VISIBLE:
                    target.visible = visible;                   // :479
                    break;
                case Processes.NAME:
                    if (!name.equals(target.name)) {            // :481
                        target.name = name;
                        // IconSprite#name= -> setBitmap (SpriteWrapper:273-291):
                        // the new bitmap is loaded and src_rect becomes its full
                        // rectangle (:286-287); ox/oy are kept.
                        if (sizes != null) {
                            int[] size = sizes.size(name);
                            target.bitmapWidth = size == null ? -1 : size[0];
                            target.bitmapHeight = size == null ? -1 : size[1];
                            target.srcX = 0;
                            target.srcY = 0;
                            target.srcWidth = -1;
                            target.srcHeight = -1;
                        }
                    }
                    break;
                case Processes.ORIGIN:
                    // :483-498 recomputes ox/oy from the sprite's source rect.
                    target.origin = origin;
                    target.updateOrigin();
                    break;
                case Processes.SRC:
                    target.srcX = srcX;                         // :501-502
                    target.srcY = srcY;
                    break;
                case Processes.SRC_SIZE:
                    target.srcWidth = srcWidth;                 // :505-506
                    target.srcHeight = srcHeight;
                    // RMXP keeps ox/oy, so an origin process that already ran
                    // this frame used the previous width - the plugin sets
                    // src_rect directly first when that matters
                    // (addBallSprite:92).
                    break;
                case Processes.CROP_BOTTOM:
                    target.cropBottom = cropBottom;             // :509-513
                    break;
                default:
                    break;
            }
        }
    }

    /** The plugin's combat frame rate ({@code Graphics.frame_rate}). */
    static final class Graphics {
        static final int FRAME_RATE = 40;
        static final int WIDTH = 672;
        static final int HEIGHT = 448;

        private Graphics() {
        }
    }

    // ------------------------------------------------------------------
    // PokeBattle_Animation's two sprite factories (PokeBattle_Animation:20-45)
    // ------------------------------------------------------------------

    /**
     * {@code PokeBattle_Animation#addSprite} (PokeBattle_Animation:20-31):
     * {@code PictureEx.new(s.z)} with x/y/visible/tone copied out of the sprite
     * (:23-26) and the origin set at delay 0 (:27).
     */
    static PictureEx forSprite(BattleSprite sprite, int origin, SePlayer sePlayer) {
        PictureEx picture = new PictureEx(sprite.z, sePlayer);   // :22
        picture.x = sprite.x;                                   // :23
        picture.y = sprite.y;                                   // :24
        picture.visible = sprite.visible;                        // :25
        System.arraycopy(sprite.tone, 0, picture.tone, 0, 4);    // :26
        picture.setOrigin(0, origin);                            // :27
        picture.sprite = sprite;                                 // :29
        return picture;
    }

    /**
     * {@code PokeBattle_Animation#addNewSprite} (PokeBattle_Animation:33-45):
     * a temporary IconSprite that exists only for this animation. The caller
     * supplies the already-created {@link BattleSprite} (the runtime keeps it in
     * the scene's sprite table so the viewport draws it, like RMXP does).
     */
    static PictureEx forNewSprite(BattleSprite sprite, float nx, float ny, String bitmapName,
            int origin, int zIndex, SePlayer sePlayer) {
        PictureEx picture = new PictureEx(zIndex, sePlayer);     // :35
        picture.setXY(0, nx, ny);                                // :36
        picture.setName(0, bitmapName);                          // :37
        picture.setOrigin(0, origin);                            // :38
        picture.sprite = sprite;                                 // :42
        return picture;
    }
}
