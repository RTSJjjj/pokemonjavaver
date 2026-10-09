package pokemon.runtime.battle;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * RGSS sprite {@code tone} and {@code color} as a fragment shader, for the
 * battle sprites the {@code PictureEx} animations drive.
 *
 * <p>An RMXP sprite is drawn in two stages, and {@code SpriteBatch}'s own
 * tinting can only multiply, so neither can be expressed with
 * {@code batch.setColor}:</p>
 * <ol>
 *   <li>{@code tone} ({@code Tone.new(r,g,b,gray)}, PictureEx:396-399 and
 *       Scene_Initialize:170): each channel is <em>shifted</em> by the tone
 *       (negative darkens, positive brightens) and then pulled towards its
 *       luminance by {@code gray/255}. This is the same operation
 *       {@link pokemon.runtime.map.ToneShader} performs for the map.</li>
 *   <li>{@code color} ({@code Color.new(r,g,b,a)}, PictureEx:401-404 and
 *       {@code battlerAppear}, PokeBattle_Animation:234-235): the sprite is
 *       blended <em>towards</em> that colour by {@code a/255} - at full alpha
 *       the sprite becomes a flat silhouette of it, which is how a Pokémon
 *       materialises out of its Poké Ball.</li>
 * </ol>
 *
 * <p>The vertex shader is the stock SpriteBatch one, so the batch's own
 * colour/alpha handling (sprite opacity, fades) is untouched.</p>
 */
public final class BattleSpriteShader {

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
        "uniform vec4 u_blend;\n" +
        "void main()\n" +
        "{\n" +
        "  vec4 base = v_color * texture2D(u_texture, v_texCoords);\n" +
        "  vec3 shifted = clamp(base.rgb + u_tone.rgb, 0.0, 1.0);\n" +
        "  float luma = dot(shifted, vec3(0.299, 0.587, 0.114));\n" +
        "  vec3 rgb = mix(shifted, vec3(luma), u_tone.a);\n" +
        "  rgb = mix(rgb, u_blend.rgb, u_blend.a);\n" +
        "  gl_FragColor = vec4(rgb, base.a);\n" +
        "}\n";

    private final ShaderProgram program = new ShaderProgram(VERTEX, FRAGMENT);

    public boolean isCompiled() {
        return program.isCompiled();
    }

    String log() {
        return program.getLog();
    }

    public ShaderProgram program() {
        return program;
    }

    /** {@code tone} = {red, green, blue, gray} in RGSS units (-255..255, 0..255). */
    public void setTone(float[] tone) {
        program.setUniformf("u_tone", tone[0] / 255f, tone[1] / 255f, tone[2] / 255f,
                Math.max(0f, Math.min(1f, tone[3] / 255f)));
    }

    /** {@code color} = {red, green, blue, alpha}; alpha is the blend amount (0..255). */
    public void setColor(float[] color) {
        program.setUniformf("u_blend", Math.max(0f, Math.min(1f, color[0] / 255f)),
                Math.max(0f, Math.min(1f, color[1] / 255f)),
                Math.max(0f, Math.min(1f, color[2] / 255f)),
                Math.max(0f, Math.min(1f, color[3] / 255f)));
    }

    public void dispose() {
        program.dispose();
    }
}
