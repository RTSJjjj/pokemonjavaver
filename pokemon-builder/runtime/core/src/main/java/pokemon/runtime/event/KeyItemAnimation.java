package pokemon.runtime.event;

import java.util.ArrayList;
import java.util.List;

/**
 * 302_BW_Get_Key_Item:112-190 {@code GetKeyItemScene#pbUpdate}: the white flash, the glowing background and the item
 * icon that spins in, shakes and fades out. The Ruby changes the sprites between two {@code Graphics.update} calls, so
 * the frames are recorded the same way: each {@link #tick()} stores what that update shows, at 40 frames a second.
 * Sprite opacity is an integer in RGSS, so every opacity step truncates.
 */
public final class KeyItemAnimation {
    /** One shown frame. */
    public static final class Frame {
        public final int whiteOpacity;
        public final float bgZoomX, bgZoomY;
        public final int bgOpacity;
        public final float itemAngle, itemZoomX, itemZoomY;
        public final int itemOpacity;
        /** The jingle starts with this frame (:136 {@code pbMEPlay("Key item get") if frametome==7}). */
        public final boolean playJingle;

        Frame(int whiteOpacity, float bgZoomX, float bgZoomY, int bgOpacity, float itemAngle, float itemZoomX,
              float itemZoomY, int itemOpacity, boolean playJingle) {
            this.whiteOpacity = whiteOpacity;
            this.bgZoomX = bgZoomX;
            this.bgZoomY = bgZoomY;
            this.bgOpacity = bgOpacity;
            this.itemAngle = itemAngle;
            this.itemZoomX = itemZoomX;
            this.itemZoomY = itemZoomY;
            this.itemOpacity = itemOpacity;
            this.playJingle = playJingle;
        }
    }

    /** RGSS frame rate. */
    public static final float FPS = 40f;

    /** The icon: the item id for a PBS item, null for a "fake" item named by an image file. */
    public final String item;
    /** :105-107 a String item is the file name in Graphics/Icons and shows no "Get Item" message. */
    public final String fakeIcon;
    private final List<Frame> frames = new ArrayList<>();
    private float elapsed;
    private boolean jinglePlayed;

    // :48-80 pbStartScene
    private int white;
    private float bgZx, bgZy;
    private int bgOp;
    private float angle = 180;
    private float izx, izy;
    private int iop;
    private boolean jingleNow;

    public KeyItemAnimation(String item, String fakeIcon) {
        this.item = item;
        this.fakeIcon = fakeIcon;
        script();
    }

    private static int clamp(float value) {
        return Math.max(0, Math.min(255, (int) value));
    }

    private void tick() {                                                    // Graphics.update
        frames.add(new Frame(white, bgZx, bgZy, bgOp, angle, izx, izy, iop, jingleNow));
        jingleNow = false;
    }

    private void spin() {                                                    // angle -= 15/2 if angle != 0; angle = 0 if < 0
        if (angle != 0) angle -= 7;                                          // 15/2 is Integer division: 7
        if (angle < 0) angle = 0;
    }

    private void shake() {                                                   // :90-104 shakeItem
        for (int i = 0; i < 3; i++) { tick(); angle += 2; }
        for (int i = 0; i < 3; i++) { tick(); angle -= 2; }
        for (int i = 0; i < 3; i++) { tick(); angle -= 2; }
        for (int i = 0; i < 3; i++) { tick(); angle += 2; }
    }

    private void script() {
        tick();                                                              // :113 pbWait(1)
        for (int i = 0; i < 10; i++) { tick(); white = clamp(white + 255 / 10); }       // :114-117 255/10 = 25
        for (int i = 0; i < 10; i++) { tick(); white = clamp(white - 255 / 10); }       // :118-121
        for (int frame = 1; frame <= 18; frame++) {                          // :123-135
            tick();
            spin();
            if (frame == 7) jingleNow = true;                                // :132 (after the update: the next frame)
            if (bgZy < 1.75f) bgZy += 0.1f / 2;
            if (bgZx < 1.75f) bgZx += 0.1f / 2;
            bgOp = clamp(bgOp + 14.16f);
            izy += 0.17f / 2;
            izx += 0.17f / 2;
            iop = clamp(iop + 14.16f);
        }
        for (int i = 0; i < 12; i++) {                                       // :136-151
            tick();
            spin();
            if (bgZy > 1) bgZy += 0.1f / 2;
            if (bgZx > 1) bgZx += 0.1f / 2;
            if (bgZx < 1) bgZx = 1;
            if (bgZy < 1) bgZy = 1;
            if (izy > 1) izy -= 0.17f / 2;
            if (izx > 1) izx -= 0.17f / 2;
            if (izx < 1) izx = 1;
            if (izy < 1) izy = 1;
        }
        angle = 0;                                                           // :152-156
        izy = 1;
        izx = 1;
        bgZy = 1;
        bgZx = 1;
        shake();
        shake();
        for (int i = 0; i < 6; i++) tick();                                  // :159 pbWait(6)
        for (int i = 0; i < 18; i++) {                                       // :160-167
            tick();
            bgZy -= 0.15f / 2;
            bgZx += 0.1f / 2;
            bgOp = clamp(bgOp - 14.16f);
            izy -= 0.17f / 2;
            izx -= 0.17f / 2;
            iop = clamp(iop - 14.16f);
        }
    }

    public void update(float delta) {
        elapsed += Math.max(0f, delta);
    }

    public int frameCount() {
        return frames.size();
    }

    public float duration() {
        return frames.size() / FPS;
    }

    public boolean finished() {
        return elapsed >= duration();
    }

    /** The frame the screen shows now. */
    public Frame current() {
        int index = Math.min(frames.size() - 1, (int) (elapsed * FPS));
        return frames.get(index);
    }

    /** True once, the first time the jingle's frame has been reached. */
    public boolean takeJingle() {
        if (jinglePlayed) {
            return false;
        }
        for (int i = 0; i <= Math.min(frames.size() - 1, (int) (elapsed * FPS)); i++) {
            if (frames.get(i).playJingle) {
                jinglePlayed = true;
                return true;
            }
        }
        return false;
    }
}
