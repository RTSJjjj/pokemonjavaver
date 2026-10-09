package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.PBEffects;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;

import java.util.ArrayList;
import java.util.List;

/**
 * 354_ES_s_Move_Info_Display {@code Move_Info_Display}: the fight menu's F5 (R on a touch screen) screen for the highlighted
 * move: name, type, category, power, accuracy, critical stage, priority, how effective it is against the first target, the
 * move's flags and its description. F5 or B closes it.
 *
 * <p>登记: the plugin's {@code damage = pbModifyDamage(...)} is computed and never read, so it is left out.</p>
 */
public final class MoveInfoView {

    private static final Color BASE = new Color(248f / 255f, 248f / 255f, 248f / 255f, 1f);
    private static final Color SHADOW = new Color(64f / 255f, 64f / 255f, 64f / 255f, 1f);
    private static final Color BASE_GOOD = new Color(200f / 255f, 248f / 255f, 200f / 255f, 1f);
    private static final Color SHADOW_GOOD = new Color(0f, 200f / 255f, 64f / 255f, 1f);
    private static final Color BASE_BAD = new Color(248f / 255f, 192f / 255f, 192f / 255f, 1f);
    private static final Color SHADOW_BAD = new Color(200f / 255f, 0f, 0f, 1f);
    private static final Color BASE_NONE = new Color(200f / 255f, 200f / 255f, 200f / 255f, 1f);
    private static final Color MOVE_BASE = new Color(200f / 255f, 248f / 255f, 200f / 255f, 1f);
    private static final Color MOVE_SHADOW = new Color(0f, 200f / 255f, 40f / 255f, 1f);
    /** 354:112 the flag letters in the order they are listed. */
    private static final String[] FLAGS = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "r"};

    private final RuntimeContext context;
    private final boolean female;
    private final MenuClock clock = new MenuClock();
    private int bgScroll;
    private boolean closed;

    // what drawMoveInfo works out once
    private final String moveName;
    private final String moveType;
    private final int typeIndex;
    private final int category;
    private final int baseDamage;
    private final boolean stab;
    private final String flags;
    private final String effectiveness;      // null: no line
    private final Color effectBase, effectShadow;
    private final int accuracy;
    private final int critical;
    private final int priority;
    private final String description;

    public MoveInfoView(RuntimeContext context, Battle battle, Battler battler, BattleMove move, boolean female) {
        this.context = context;
        this.female = female;
        PbsData pbs = context.pbsData();
        MoveEffect effect = MoveEffectRegistry.of(move.function());
        Object[] choice = {":UseMove", -1, battler.struggle(pbs), -1};   // :34 [:UseMove,-1,struggle,-1]
        Battler target = null;
        Array<Battler> targets = battler.pbFindTargets(choice, move, battler);                  // :35
        for (int i = 0; i < targets.size && target == null; i++) {                                // :37-41 the first one standing
            Battler t = targets.get(i);
            if (t != null && !t.fainted()) target = t;
        }
        String type = effect.pbCalcType(move, battler);                                          // :52
        this.moveType = type;
        PbsData.TypeInfo info = pbs == null || type == null ? null : pbs.type(type);
        this.typeIndex = info == null ? 0 : info.id;
        int base = move.move == null ? 0 : move.move.power;                                      // :53 MOVE_BASE_DAMAGE
        if (target != null) {
            base = effect.pbBaseDamage(move, base, battler, target);                             // :56
        }
        this.baseDamage = base;
        int cat = "Physical".equalsIgnoreCase(move.category()) ? 0 : "Special".equalsIgnoreCase(move.category()) ? 1 : 2;   // :59 @move.category
        String name = move.internalName();
        if ("PHOTONGEYSER".equals(name) || "ETERNALFLAME".equals(name) || "STELLARBLAST".equals(name)
                || "TERASTARSTORM".equals(name)) {                                               // :80-88
            cat = battler.baseAttack() > battler.baseSpAtk() ? 0 : 1;
        }
        this.category = cat;
        this.moveName = move.name();
        this.stab = type != null && battler.pbHasType(type);                                     // :105
        this.flags = move.flags() == null ? "" : move.flags();                                   // :63
        int acc = move.move == null ? 0 : move.move.accuracy;                                    // :60
        if (target != null) {
            acc = effect.pbBaseAccuracy(move, battler, target);                                  // :61
        }
        this.accuracy = acc;
        int crit = effect.highCriticalRate(move) ? 1 : 0;                                        // :65
        this.critical = crit + battler.effects.intVal(PBEffects.Battler.FocusEnergy);            // :159
        int prio = move.move == null ? 0 : move.move.priority;                                   // :64
        if (battler.hasActiveItem(new String[] {"LAGGINGTAIL", "FULLINCENSE"})) prio = -1;       // :165
        this.priority = prio;
        String text = null;
        Color tb = BASE, ts = SHADOW;
        if (target != null && base > 0 && (context.gameState().switches().get(197) || ownedEx(target))) {   // :132-135
            int mult = effect.pbCalcTypeMod(move, type, battler, target);                        // :136
            if (("JUDGMENT".equals(name) || "GODJUDGMENT".equals(name)) && battler.isSpecies("ARCEUS")
                    && (battler.hasActiveItem("LEGENDPLATE") || battler.hasActiveItem("LEGENDPLATEFREE"))) {   // :137-139
                List<String> t = new ArrayList<>();
                for (String s : target.pbTypes()) t.add(s);
                mult = t.size() > 1 && !t.get(0).equals(t.get(1)) ? 32 : 16;                    // :140 target.type1 != target.type2
            }
            if (mult == 8 * 4) {                                                                 // :149-165
                text = "效果绝佳";
                tb = BASE_GOOD;
                ts = SHADOW_GOOD;
            } else if (mult == 8 * 2) {
                text = "效果拔群";
                tb = BASE_GOOD;
                ts = SHADOW_GOOD;
            } else if (mult == 8) {
                text = "效果正常";
            } else if (mult == 8 / 2) {
                text = "效果不好";
                tb = BASE_BAD;
                ts = SHADOW_BAD;
            } else if (mult == 8 / 4) {
                text = "收效甚微";
                tb = BASE_BAD;
                ts = SHADOW_BAD;
            } else if (mult == 0) {
                text = "没有效果";
                tb = BASE_NONE;
                ts = SHADOW;
            }
        }
        this.effectiveness = text;
        this.effectBase = tb;
        this.effectShadow = ts;
        this.description = move.move == null || move.move.description == null ? "" : move.move.description;   // :170
    }

    /** {@code target.ownedEx?} (353:625-628). */
    private boolean ownedEx(Battler target) {
        Object illusion = target.effects.raw(PBEffects.Battler.Illusion);
        PbsData.Species species = illusion instanceof pokemon.runtime.pokemon.Pokemon && ((pokemon.runtime.pokemon.Pokemon) illusion).species != null
                ? ((pokemon.runtime.pokemon.Pokemon) illusion).species : target.pokemon.species;
        return species != null && context.gameState().trainer().owned.contains(species.internalName);
    }

    /** @return true once the screen is closed (F5 or B) */
    public boolean update(InputManager input) {
        if (closed) return true;
        int ticks = clock.advance();
        for (int t = 0; t < ticks; t++) {
            bgScroll++;                                                        // :27-28 ox += 1, back to 0 when > 32
            if (bgScroll > 32) bgScroll = 0;
        }
        if (input.wasPressed(GameAction.F5) || input.wasPressed(GameAction.CANCEL)
                || (context.touchBuild() && input.wasPressed(GameAction.SHOULDER_RIGHT))) {
            closed = true;                                                     // :29 Input::F5 or Input::B
        }
        return closed;
    }

    private float h;

    /** Text of the overlay sprite: {@code y} counts from the overlay's top, which is 48 px down the screen. */
    private void text(SpriteBatch b, MenuFont f, String text, float x, float y, int align, Color base, Color shadow) {
        float top = h - (48f + y);
        if (align == 2) {
            f.drawCentered(b, text, x, top, base, shadow);
        } else {
            f.draw(b, text, x, top, base, shadow);
        }
    }

    private void strip(SpriteBatch b, Texture t, float x, float y, int srcY, int srcW, int srcH) {
        if (t != null && srcY + srcH <= t.getHeight()) {
            b.draw(t, x, h - (48f + y) - srcH, srcW, srcH, 0, srcY, srcW, srcH, false, false);
        }
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont f) {
        float w = ScreenMetrics.logicalWidth();
        h = ScreenMetrics.logicalHeight();
        b.setColor(Color.WHITE);
        Texture bg = a.graphic("Pictures/Battle", female ? "bg_moveinfo_f" : "bg_moveinfo_m");
        if (bg != null) {
            b.draw(bg, -bgScroll, h - 48f - bg.getHeight());                  // :11-17 in the viewport (0, 48, width, height-192)
        }
        float expWidth = f.width("类别：");                                      // :48
        text(b, f, moveName, w / 4, 10f, 2, BASE, SHADOW);                      // :71
        float baseX = w / 2;
        text(b, f, "属性：", baseX, 10f, 0, BASE, SHADOW);                       // :74
        strip(b, a.graphic("Pictures", "types"), baseX + expWidth, 6f, typeIndex * 28, 64, 28);       // :75-76
        float cateX = baseX + 64 + expWidth + 28;
        text(b, f, "类别：", cateX, 10f, 0, BASE, SHADOW);                       // :79
        strip(b, a.graphic("Pictures", "category"), cateX + expWidth, 6f, category * 28, 64, 28);     // :90-91
        text(b, f, "威力：", baseX, 36f, 0, BASE, SHADOW);                       // :94
        if (baseDamage == 0) {                                                  // :99-104
            text(b, f, "---", baseX + expWidth, 36f, 0, BASE, SHADOW);
        } else if (baseDamage == 1) {
            text(b, f, "???", baseX + expWidth, 36f, 0, BASE, SHADOW);
        } else {
            text(b, f, String.valueOf(baseDamage), baseX + expWidth, 36f, 0, stab ? MOVE_BASE : BASE, stab ? MOVE_SHADOW : SHADOW);
        }
        text(b, f, "标签：", 8f, 36f, 0, BASE, SHADOW);                         // :106
        float flagsX = 60f;
        for (String flag : FLAGS) {                                             // :109-124
            boolean show = flags.contains(flag);
            if ("b".equals(flag) || "e".equals(flag)) show = !show;             // Protect / Mirror Move: shown when the move does NOT have it
            if (!show) continue;
            strip(b, a.graphic("Pictures/Move Flags", flag), flagsX, 34f, 0, 26, 28);
            flagsX += 26f;
        }
        if (effectiveness != null) {                                            // :126-166
            text(b, f, effectiveness, w / 4, 62f, 2, effectBase, effectShadow);
        }
        text(b, f, accuracy == 0 ? "命中：---" : "命中：" + accuracy + "%", cateX, 36f, 0, BASE, SHADOW);   // :168-172
        text(b, f, "会心：" + critical, baseX, 62f, 0, BASE, SHADOW);            // :174-175
        text(b, f, "先制：" + priority, cateX, 62f, 0, BASE, SHADOW);            // :177-178
        text(b, f, "描述：", 8f, 88f, 0, BASE, SHADOW);                          // :180
        drawDescription(b, f, w);
        b.setColor(Color.WHITE);
    }

    /** 354:182-203: the description without spaces, broken after the characters that fit, three lines of 26 px from y = 114. */
    private void drawDescription(SpriteBatch b, MenuFont f, float w) {
        String desc = description.replace(" ", "");
        float charWidth = Math.max(1f, f.width("字"));
        int length = desc.length();
        for (int i = 0; i < desc.length(); i++) {
            if (charWidth * i >= w - 16f) {
                length = i;
                break;
            }
        }
        length = Math.max(1, length);
        for (int line = 0; line < 3 && line * length < desc.length(); line++) {
            String part = desc.substring(line * length, Math.min(desc.length(), (line + 1) * length));
            text(b, f, part, 8f, 114f + 26f * line, 0, BASE, SHADOW);
        }
    }
}
