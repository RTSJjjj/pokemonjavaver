package pokemon.runtime.event;

/**
 * Screen effects of the event commands (project3 section 18, R6.4b): Fade Out
 * /In (221/222), Tint (223), Flash (224) and Shake (225). Headless on purpose:
 * the map screen renders whatever this holds, so the timing stays testable.
 *
 * <p>Durations are RMXP frames (1/20 s). Fade 0 = clear, 255 = black; flash
 * carries the command's colour and decays to nothing; shake exposes a decaying
 * sine offset the camera adds to its position.</p>
 */
public final class ScreenEffects {

    private float fade;
    private float fadeFrom;
    private float fadeTo;
    private float fadeLeft;
    private float fadeTotal;

    private float flashLeft;
    private float flashTotal;
    private float flashAlpha;
    private float flashRed = 1f;
    private float flashGreen = 1f;
    private float flashBlue = 1f;

    private float shakeLeft;
    private float shakeTotal;
    private float shakePower;
    private float shakeSpeed = 1f;
    private float shakeTime;
    private float shakeX;
    private float shakeY;
    private float toneRed;
    private float toneGreen;
    private float toneBlue;
    private float toneGray;
    private float toneFromRed;
    private float toneFromGreen;
    private float toneFromBlue;
    private float toneFromGray;
    private float toneToRed;
    private float toneToGreen;
    private float toneToBlue;
    private float toneToGray;
    private float toneLeft;
    private float toneTotal;

    /** Fade Out (221) / Fade In (222): duration in frames. */
    public void fade(int durationFrames, boolean out) {
        fadeFrom = fade;
        fadeTo = out ? 255f : 0f;
        fadeTotal = Math.max(0f, durationFrames / 20f);
        fadeLeft = fadeTotal;
        if (fadeTotal <= 0f) {
            fade = fadeTo;
        }
    }

    /** Flash Screen (224): colour, strength and duration in frames. */
    public void flash(float red, float green, float blue, float power, int durationFrames) {
        flashRed = red;
        flashGreen = green;
        flashBlue = blue;
        flashAlpha = Math.max(0f, Math.min(1f, power / 255f));
        flashTotal = Math.max(0f, durationFrames / 20f);
        flashLeft = flashTotal;
    }

    /** Shake Screen (225): strength, speed (1..9) and duration in frames. */
    public void shake(float power, float speed, int durationFrames) {
        shakePower = power;
        shakeSpeed = Math.max(1f, speed);
        shakeTotal = Math.max(0f, durationFrames / 20f);
        shakeLeft = shakeTotal;
    }

    /**
     * Change Screen Color Tone (223). RMXP shifts every channel by the tone:
     * negative values darken that channel (-255 leaves nothing, the black
     * screen doors use), positive values brighten it and {@code gray} pulls the
     * colours towards grey.
     */
    public void tint(float red, float green, float blue, float gray, int durationFrames) {
        tint(red, green, blue, gray, durationFrames, true);
    }

    /**
     * The runtime's own ambient tone (day/night). It changes the same tone
     * channels but does not claim the tone for a script, so a later 223 still
     * wins and a map transfer resets to the ambient value.
     */
    public void ambientTint(float red, float green, float blue, float gray, int durationFrames) {
        tint(red, green, blue, gray, durationFrames, false);
    }

    private void tint(float red, float green, float blue, float gray, int durationFrames,
                      boolean fromScript) {
        if (fromScript) {
            toneFromScript = true;
        }
        setTone(red, green, blue, gray, durationFrames);
    }

    /** True while a script (223) owns the tone; the ambient effect then yields. */
    public boolean toneFromScript() {
        return toneFromScript;
    }

    private void setTone(float red, float green, float blue, float gray, int durationFrames) {
        toneFromRed = toneRed;
        toneFromGreen = toneGreen;
        toneFromBlue = toneBlue;
        toneFromGray = toneGray;
        toneToRed = red;
        toneToGreen = green;
        toneToBlue = blue;
        toneToGray = gray;
        toneTotal = Math.max(0f, durationFrames / 20f);
        toneLeft = toneTotal;
        if (toneTotal <= 0f) {
            landTone();
        }
    }

    private boolean toneFromScript;

    public float toneRed() {
        return toneRed;
    }

    public float toneGreen() {
        return toneGreen;
    }

    public float toneBlue() {
        return toneBlue;
    }

    public float toneGray() {
        return toneGray;
    }

    /** Called once per frame by the map screen. */
    public void update(float delta) {
        float step = Math.max(0f, delta);
        if (fadeLeft > 0f) {
            fadeLeft -= step;
            float t = 1f - Math.max(0f, fadeLeft) / fadeTotal;
            fade = fadeFrom + (fadeTo - fadeFrom) * Math.max(0f, Math.min(1f, t));
            if (fadeLeft <= 0f) {
                fade = fadeTo;
            }
        }
        if (flashLeft > 0f) {
            flashLeft -= step;
            if (flashLeft <= 0f) {
                flashLeft = 0f;
            }
        }
        if (shakeLeft > 0f) {
            shakeLeft -= step;
            shakeTime += step;
            float decay = shakeTotal <= 0f ? 0f : Math.max(0f, shakeLeft) / shakeTotal;
            shakeX = (float) Math.sin(shakeTime * shakeSpeed * 40f) * shakePower * decay;
            shakeY = (float) Math.cos(shakeTime * shakeSpeed * 40f) * shakePower * decay;
            if (shakeLeft <= 0f) {
                shakeX = 0f;
                shakeY = 0f;
            }
        }
        if (toneLeft > 0f) {
            toneLeft -= step;
            float t = toneTotal <= 0f ? 1f : 1f - Math.max(0f, toneLeft) / toneTotal;
            t = Math.max(0f, Math.min(1f, t));
            toneRed = toneFromRed + (toneToRed - toneFromRed) * t;
            toneGreen = toneFromGreen + (toneToGreen - toneFromGreen) * t;
            toneBlue = toneFromBlue + (toneToBlue - toneFromBlue) * t;
            toneGray = toneFromGray + (toneToGray - toneFromGray) * t;
            if (toneLeft <= 0f) {
                landTone();
            }
        }
    }

    private void landTone() {
        toneRed = toneToRed;
        toneGreen = toneToGreen;
        toneBlue = toneToBlue;
        toneGray = toneToGray;
    }

    /**
     * Drops the screen tone / flash / shake but keeps the fade. RMXP recreates
     * the screen when a map is set up, which is what clears the black tone a
     * door applied before the transfer (the trailing "tone back to 0" command
     * never runs, because the transfer ends the event).
     */
    public void clearTone() {
        toneRed = 0f;
        toneGreen = 0f;
        toneBlue = 0f;
        toneGray = 0f;
        toneFromRed = 0f;
        toneFromGreen = 0f;
        toneFromBlue = 0f;
        toneFromGray = 0f;
        toneToRed = 0f;
        toneToGreen = 0f;
        toneToBlue = 0f;
        toneToGray = 0f;
        toneLeft = 0f;
        toneTotal = 0f;
        flashLeft = 0f;
        flashTotal = 0f;
        shakeLeft = 0f;
        shakeTotal = 0f;
        shakeX = 0f;
        shakeY = 0f;
        toneFromScript = false;
    }

    /** 0 = clear screen, 255 = fully black. */
    public float fade() {
        return fade;
    }

    public boolean flashing() {
        return flashLeft > 0f && flashAlpha > 0f;
    }

    public float flashAlpha() {
        return flashAlpha * (flashTotal <= 0f ? 0f : Math.max(0f, flashLeft) / flashTotal);
    }

    public float flashRed() {
        return flashRed;
    }

    public float flashGreen() {
        return flashGreen;
    }

    public float flashBlue() {
        return flashBlue;
    }

    public float shakeX() {
        return shakeX;
    }

    public float shakeY() {
        return shakeY;
    }

    /** Cancels every effect (map change). */
    public void clear() {
        fade = 0f;
        fadeLeft = 0f;
        fadeTotal = 0f;
        flashLeft = 0f;
        flashTotal = 0f;
        shakeLeft = 0f;
        shakeTotal = 0f;
        shakeX = 0f;
        shakeY = 0f;
        toneRed = 0f;
        toneGreen = 0f;
        toneBlue = 0f;
        toneGray = 0f;
        toneLeft = 0f;
        toneTotal = 0f;
    }
}
