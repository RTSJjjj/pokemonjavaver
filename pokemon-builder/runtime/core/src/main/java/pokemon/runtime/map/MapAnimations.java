package pokemon.runtime.map;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.data.AnimationData;

/**
 * R6.27: the Show Animation (207) effects playing on the map right now.
 *
 * <p>The project's 0005.rb advances one animation frame every two game frames at
 * 40 fps, so an animation lasts {@code frameMax * 2 / 40} seconds. Timings fire
 * on the animation frame they are assigned to; their SE plays and their flash
 * either tints the target (scope 1) or the whole screen (scope 2).</p>
 */
public final class MapAnimations {

    public static final int FRAMES_PER_ANIMATION_FRAME = 2;
    private static final float GAME_FPS = 40f;

    /**
     * R6.30: how long one animation plays, in seconds. Used by the event side
     * ({@code pbExclaim} waits until its bubble is disposed) so the wait and
     * the tile animation cannot disagree about the animation data.
     */
    public static float durationSeconds(AnimationData.Animation animation) {
        if (animation == null || animation.frameMax <= 0) {
            return 0f;
        }
        return animation.frameMax * (float) FRAMES_PER_ANIMATION_FRAME / GAME_FPS;
    }

    /** Where timings send their side effects (audio and screen flash). */
    public interface TimingSink {
        void playSe(String name, int volume, int pitch);

        void flash(float red, float green, float blue, float alpha, int durationFrames);
    }

    /** One running animation attached to a character (-1 = player) or screen. */
    public static final class Active {
        public final AnimationData.Animation animation;
        public final int targetId;
        /** Tile anchor for Essentials' addUserAnimation; -1 for characters. */
        public final int tileX;
        public final int tileY;
        /** RPG::Sprite height of a tile animation (1 low .. 3 high). */
        public final int height;
        private float elapsed;
        private final boolean[] fired;

        private Active(AnimationData.Animation animation, int targetId,
                       int tileX, int tileY, int height) {
            this.animation = animation;
            this.targetId = targetId;
            this.tileX = tileX;
            this.tileY = tileY;
            this.height = height;
            this.fired = new boolean[animation.timings.length];
        }

        /** Animation frame index right now, clamped to the animation's last frame. */
        public int frameIndex() {
            if (animation.frameMax <= 0) {
                return 0;
            }
            int index = (int) (elapsed * GAME_FPS) / FRAMES_PER_ANIMATION_FRAME;
            return Math.min(animation.frameMax - 1, Math.max(0, index));
        }

        public boolean finished() {
            return elapsed * GAME_FPS >= animation.frameMax * FRAMES_PER_ANIMATION_FRAME;
        }

        /** Seconds the animation has been running (tests). */
        public float elapsed() {
            return elapsed;
        }
    }

    /** A target flash requested by a timing (scope 1), fading out. */
    public static final class TargetFlash {
        public final int targetId;
        public final float red;
        public final float green;
        public final float blue;
        private final float startAlpha;
        private final float duration;
        private float elapsed;

        private TargetFlash(int targetId, float red, float green, float blue,
                            float startAlpha, float duration) {
            this.targetId = targetId;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.startAlpha = startAlpha;
            this.duration = duration;
        }

        /** 1 at the start, 0 when the flash is over. */
        public float alpha() {
            if (duration <= 0f) {
                return 0f;
            }
            return startAlpha * Math.max(0f, 1f - elapsed / duration);
        }
    }

    private final Array<Active> active = new Array<>();
    private final Array<TargetFlash> flashes = new Array<>();

    /** Starts one animation; a second call for the same target replaces it. */
    public void start(AnimationData.Animation animation, int targetId) {
        if (animation == null || animation.frameMax <= 0) {
            return;
        }
        for (int i = active.size - 1; i >= 0; i--) {
            if (active.get(i).targetId == targetId) {
                active.removeIndex(i);
            }
        }
        active.add(new Active(animation, targetId, -1, -1, 3));
    }

    /**
     * R6.28: starts an animation at a map tile, like Essentials'
     * {@code addUserAnimation}. Tile effects never replace each other, so a row
     * of rustling grass keeps its own sprites.
     */
    public void startTile(AnimationData.Animation animation, int x, int y, int height) {
        if (animation == null || animation.frameMax <= 0) {
            return;
        }
        active.add(new Active(animation, Integer.MIN_VALUE, x, y, height));
    }

    public void update(float delta, TimingSink sink) {
        float step = Math.max(0f, delta);
        for (int i = active.size - 1; i >= 0; i--) {
            Active current = active.get(i);
            current.elapsed += step;
            int frame = current.frameIndex();
            for (int t = 0; t < current.animation.timings.length; t++) {
                AnimationData.Timing timing = current.animation.timings[t];
                if (current.fired[t] || timing.frame > frame || timing.condition == 2) {
                    continue;
                }
                current.fired[t] = true;
                fireTiming(timing, current.targetId, sink);
            }
            if (current.finished()) {
                active.removeIndex(i);
            }
        }
        for (int i = flashes.size - 1; i >= 0; i--) {
            TargetFlash flash = flashes.get(i);
            flash.elapsed += step;
            if (flash.alpha() <= 0f) {
                flashes.removeIndex(i);
            }
        }
    }

    private void fireTiming(AnimationData.Timing timing, int targetId, TimingSink sink) {
        if (timing.seName != null && !timing.seName.isEmpty() && sink != null) {
            sink.playSe(timing.seName, timing.seVolume, timing.sePitch);
        }
        if (timing.flashDuration <= 0) {
            return;
        }
        float alpha = Math.max(0f, Math.min(1f, timing.alpha / 255f));
        float red = Math.max(0f, Math.min(1f, timing.red / 255f));
        float green = Math.max(0f, Math.min(1f, timing.green / 255f));
        float blue = Math.max(0f, Math.min(1f, timing.blue / 255f));
        if (timing.scope == 2) {
            if (sink != null) {
                sink.flash(red, green, blue, alpha,
                        timing.flashDuration * FRAMES_PER_ANIMATION_FRAME);
            }
        } else if (timing.scope == 1) {
            flashes.add(new TargetFlash(targetId, red, green, blue, alpha,
                    timing.flashDuration * FRAMES_PER_ANIMATION_FRAME / GAME_FPS));
        }
    }

    public Array<Active> active() {
        return active;
    }

    public Array<TargetFlash> flashes() {
        return flashes;
    }

    public boolean isEmpty() {
        return active.isEmpty() && flashes.isEmpty();
    }

    public void clear() {
        active.clear();
        flashes.clear();
    }
}
