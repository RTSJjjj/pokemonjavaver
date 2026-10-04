package pokemon.runtime.map;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * R6.17: an RGSS style screen {@code Tone} (the thing behind
 * {@code Tone.new(r,g,b,gray)} and {@code pbDayNightTint}) as a fragment
 * shader for the map layer.
 *
 * <p>RGSS shifts each channel by the tone's red/green/blue (negative darkens,
 * positive brightens) and then pulls the colour towards its luminance by
 * {@code gray/255}. The sprite batch's own tinting can only multiply, which is
 * why the old implementation could not show the project's night tones
 * ({@code (-90,-90,30,55)}: darken red/green while *adding* blue and
 * desaturating). The shader keeps the default SpriteBatch vertex shader, so
 * the batch's colour/alpha handling (fades, character opacity) is untouched.</p>
 */
public final class ToneShader {

    private static final String VERTEX =
        "attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "void main()\n" +
        "{\n" +
        "   v_color = a_color;\n" +
        "   v_color.a = v_color.a * (255.0/254.0);\n" +
        "   v_texCoords = a_texCoord0;\n" +
        "   gl_Position =  u_projTrans * a_position;\n" +
        "}\n";

    private static final String FRAGMENT =
        "#ifdef GL_ES\n" +
        "#define LOWP lowp\n" +
        "precision mediump float;\n" +
        "#else\n" +
        "#define LOWP\n" +
        "#endif\n" +
        "varying LOWP vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform vec4 u_tone;\n" +
        "void main()\n" +
        "{\n" +
        "  vec4 base = v_color * texture2D(u_texture, v_texCoords);\n" +
        "  vec3 shifted = clamp(base.rgb + u_tone.rgb, 0.0, 1.0);\n" +
        "  float luma = dot(shifted, vec3(0.299, 0.587, 0.114));\n" +
        "  vec3 rgb = mix(shifted, vec3(luma), u_tone.a);\n" +
        "  gl_FragColor = vec4(rgb, base.a);\n" +
        "}\n";

    private final ShaderProgram program = new ShaderProgram(VERTEX, FRAGMENT);

    public boolean isCompiled() {
        return program.isCompiled();
    }

    public String log() {
        return program.getLog();
    }

    public ShaderProgram program() {
        return program;
    }

    /**
     * RGSS tone values: channel shifts in -255..255, gray in 0..255.
     * Must be called while the program is in use (or it binds it itself).
     */
    public void setTone(float red, float green, float blue, float gray) {
        program.bind();
        program.setUniformf("u_tone", red / 255f, green / 255f, blue / 255f,
                Math.max(0f, Math.min(1f, gray / 255f)));
    }

    public void dispose() {
        program.dispose();
    }
}
