package pokemon.runtime.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.app.ScreenMetrics;
import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.PBBattleTerrains;
import pokemon.runtime.battle.PBEffects;
import pokemon.runtime.battle.PBStats;
import pokemon.runtime.battle.PBTypes;
import pokemon.runtime.battle.PBWeather;
import pokemon.runtime.battle.PendingApi;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.util.ArrayList;
import java.util.List;

/**
 * 353_ES_s_Battle_Info_Display {@code Battle_Info_Display}: the battlers' names, types, held items, abilities and HP, the
 * selected battler's stat stages with the rough stats, the type chart of its types, the turn counts, the weather, the terrain
 * and the rooms. F5 (R on a touch screen) or B closes it; with more than one battler the arrows pick which one is shown
 * (UP/DOWN between the sides, LEFT/RIGHT between positions).
 *
 * <p>登记: a fainted battler's icon is dimmed instead of drawn in the RGSS gray tone;
 * {@code pbSpeed} leaves out Swamp.</p>
 */
public final class BattleInfoView {

    /** 353:34 {@code @oys}: the arrow's bobbing offsets, one per 40fps frame. */
    private static final int[] OYS = {0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 3, 3, 3, 2, 2, 2, 1, 1, 1};
    private static final Color FONT = rgb(248, 248, 248);
    private static final Color SHADOW = rgb(64, 64, 64);
    private static final Color SHADOW_ALLY = rgb(0, 128, 248);
    private static final Color SHADOW_FOE = rgb(248, 128, 96);
    private static final Color SHADOW_RAISE = rgb(0, 216, 24);
    private static final Color SHADOW_LOSS = rgb(248, 48, 48);

    private static final String[] WEATHER = {"无", "大晴天", "下雨", "沙暴", "冰雹", "大日照", "大雨", "乱流", "暗影", "起雾", "下雪"};
    private static final String[] TERRAIN = {"无", "电气场地", "青草场地", "薄雾场地", "精神场地", "虫惑场地", "冰冷场地"};

    private final RuntimeContext context;
    private final Battle battle;
    private final boolean female;
    private final MenuClock clock = new MenuClock();
    private final int length;               // @battle.battlers.length
    private int allies, foes;
    private int index;
    private int bgScroll;
    private int bob;
    private Battler statsBattler;           // the one whose stages are listed (null: a fainted one, the list stays empty)
    private Battler chartBattler;           // the one whose type chart is drawn (a fainted battler leaves the old chart)
    private boolean closed;
    private int iconTick;
    private int iconFrame;
    private final float[] arrowX = new float[6];
    private final float[] arrowY = new float[6];
    /** 353:37 puts the arrow 16 above its slot; the first move (:281) puts it 12 above. */
    private float arrowLift = 16f;
    private String[] typeNames;

    public BattleInfoView(RuntimeContext context, Battle battle, boolean female) {
        this.context = context;
        this.battle = battle;
        this.female = female;
        this.length = battle.maxBattlerIndex() + 1;
        for (int i = 0; i < length; i++) {                                     // :72-79
            if (battle.battlerAt(i) == null) continue;
            if (i % 2 == 0) allies++; else foes++;
        }
        this.statsBattler = battle.battlerAt(0);                               // :186-190 battlers[0]
        this.chartBattler = battle.battlerAt(0);
        float w = ScreenMetrics.logicalWidth(), h = ScreenMetrics.logicalHeight();
        for (int k = 0; k < 6; k += 2) {                                       // :67-74 arrow_x / arrow_y
            arrowX[k] = w / Math.max(1, allies * 2) * (k + 1);
            arrowX[k + 1] = w / Math.max(1, foes * 2) * (k + 1);
            arrowY[k] = h - 164f;
            arrowY[k + 1] = 40f;
        }
        PbsData pbs = context.pbsData();
        if (pbs != null) {                                                     // type id -> internal name, once
            typeNames = new String[PBTypes.maxValue(pbs) + 2];
            for (PbsData.TypeInfo info : pbs.types.values()) {
                if (info.id >= 0 && info.id < typeNames.length) typeNames[info.id] = info.internalName;
            }
        }
    }

    private static Color rgb(int r, int g, int b) {
        return new Color(r / 255f, g / 255f, b / 255f, 1f);
    }

    private boolean switch197() {
        return context.gameState().switches().get(197);
    }

    /** @return true once the screen is closed (F5 or B) */
    public boolean update(InputManager input) {
        if (closed) return true;
        int ticks = clock.advance();
        for (int t = 0; t < ticks; t++) {
            bgScroll++;                                                        // :251 ox += 1, back to 0 when > 32
            if (bgScroll > 32) bgScroll = 0;
            if (length > 1) {
                bob = (bob + 1) % OYS.length;                                  // :253-255
            }
            iconTick++;
            if (iconTick >= 10) {                                              // PokemonIconSprite: the icon frames alternate
                iconTick = 0;
                iconFrame++;
            }
        }
        if (input.wasPressed(GameAction.F5) || input.wasPressed(GameAction.CANCEL)
                || (context.touchBuild() && input.wasPressed(GameAction.SHOULDER_RIGHT))) {
            closed = true;                                                     // :248 Input::F5 or Input::B
            return true;
        }
        if (length > 1) {
            int old = index;
            if (input.wasPressed(GameAction.RIGHT)) {                          // :257-259
                if (index < length - 2) index += 2;
            } else if (input.wasPressed(GameAction.LEFT)) {                    // :260-262
                if (index > 1) index -= 2;
            } else if (input.wasPressed(GameAction.UP)) {                      // :263-266
                if (index < length - 1 && index % 2 == 0) index += 1;
                if (foes == 1) index = 1;
            } else if (input.wasPressed(GameAction.DOWN)) {                    // :267-268
                if (index % 2 != 0) index -= 1;
            }
            if (index < 0 || index >= length || battle.battlerAt(index) == null) index = old;   // :270
            if (old != index) {
                if (context.audioManager() != null) {
                    context.audioManager().playSe("GUI sel decision", 80, 100);   // :272
                }
                arrowLift = 12f;                                               // :281 y = arrow_y[index] - 12
                Battler picked = battle.battlerAt(index);
                statsBattler = picked.fainted() ? null : picked;               // :275-276 a fainted one only clears the list
                if (!picked.fainted()) chartBattler = picked;                  // :311 show_type_calc(...) is after the `next`
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- drawing

    private float h;
    private float w;

    private void text(SpriteBatch b, MenuFont f, String text, float x, float y, int align, Color base, Color shadow) {
        if (align == 1) {
            f.drawRight(b, text, x, h - y, base, shadow);                      // pbDrawTextPositions: 1 = right aligned
        } else if (align == 2) {
            f.drawCentered(b, text, x, h - y, base, shadow);                   // 2 = centred
        } else {
            f.draw(b, text, x, h - y, base, shadow);
        }
    }

    private void image(SpriteBatch b, Texture t, float x, float y) {
        if (t != null) {
            b.draw(t, x, h - y - t.getHeight());
        }
    }

    public void render(SpriteBatch b, MenuAssets a, MenuFont smallFont) {
        w = ScreenMetrics.logicalWidth();
        h = ScreenMetrics.logicalHeight();
        b.setColor(Color.WHITE);
        Texture bg = a.graphic("Pictures/Battle", female ? "bg_esbid_f" : "bg_esbid_m");
        if (bg != null) {
            b.draw(bg, -bgScroll, h - bg.getHeight());                         // :12-17 scrolls left, 1 px per frame
        }
        drawBattlers(b, a, smallFont);
        drawStats(b, a, smallFont);
        drawField(b, smallFont);
        drawTypeChart(b, a);
        Texture arrow = a.graphic("Pictures/Battle", "Arrow");
        if (arrow != null) {                                                   // :36-37, :253-254: only the first frame, bobbing up
            int i = Math.min(index, 5);
            float ay = arrowY[i] - arrowLift - (length > 1 ? OYS[bob] : 0);
            b.draw(arrow, arrowX[i] - 10f, h - ay - 12f, 0, 0, 20, 12, 1f, 1f, 0f, 0, 0, 20, 12, false, false);
        }
        b.setColor(Color.WHITE);
    }

    private void drawBattlers(SpriteBatch b, MenuAssets a, MenuFont f) {
        PbsData pbs = context.pbsData();
        float offsetSize = 20f * 0.55f;                                        // :115 font.size * 0.55 (the small font is 20)
        for (int i = 0; i < length; i++) {
            Battler battler = battle.battlerAt(i);
            if (battler == null) continue;
            Color shadow;
            float x, y;
            if (i % 2 == 0) {                                                  // :87-94
                shadow = SHADOW_ALLY;
                x = w / (allies * 2) * (i + 1);
                y = h - 164f;
            } else {
                shadow = SHADOW_FOE;
                x = w / (foes * 2) * i;
                y = 40f;
            }
            if (battler.fainted()) shadow = SHADOW;                            // :95
            float nameX = x;
            boolean typesShown = switch197() || owned(battler);
            if (typesShown) {
                nameX -= offsetSize;                                           // :116
            }
            text(b, f, battler.name(), nameX, y, 2, FONT, shadow);             // :96-98
            // the Pokemon icon (:100-113)
            Pokemon pkmn = battler.pokemon;
            PbsData.Species iconSpecies = displaySpecies(battler);
            if (!(battler.effects.raw(PBEffects.Battler.Illusion) instanceof Pokemon)
                    && battler.effects.truthy(PBEffects.Battler.Transform) && battler.transformLook != null
                    && battler.transformLook.species != null) {
                iconSpecies = battler.transformLook.species;                   // :107-108 pkmn.species = TransformSpecies
            }
            boolean itemShown = switch197() || (owned(battler) && battle.wildBattle());
            boolean hasItemIcon = itemShown && pkmn != null && pkmn.item != null && !pkmn.item.isEmpty();
            float iconX = x - 16f - (hasItemIcon ? 8f : 0f);
            Texture icon = iconTexture(a, iconSpecies, pkmn != null && pkmn.shiny);
            if (icon != null) {
                int size = icon.getHeight();
                int frames = Math.max(1, icon.getWidth() / Math.max(1, size));
                int frame = battler.fainted() ? 0 : iconFrame % frames;
                if (battler.fainted()) {
                    b.setColor(0.35f, 0.35f, 0.35f, 1f);                       // :111 Tone(0,0,0,255): fully gray (登记)
                }
                b.draw(icon, iconX, h - (y + 20f) - size * 0.5f, size * 0.5f, size * 0.5f,
                        frame * size, 0, size, size, false, false);            // zoom 0.5
                b.setColor(Color.WHITE);
            }
            if (typesShown) {                                                  // :115-136 the type icons
                float offsetX = (battler.name().length() / 3.0f - 1f) * offsetSize;
                List<String> types = new ArrayList<>();
                for (String t : battler.pbTypes()) types.add(t);
                Object illusion = battler.effects.raw(PBEffects.Battler.Illusion);
                if (illusion instanceof Pokemon) {
                    Pokemon ils = (Pokemon) illusion;
                    types.clear();
                    types.add(ils.types().size > 0 ? ils.types().get(0) : null);
                    if (ils.types().size > 1 && !ils.types().get(1).equals(ils.types().get(0))) types.add(ils.types().get(1));
                }
                Texture t1 = types.isEmpty() ? null : typeIcon(a, pbs, types.get(0));
                image(b, t1, x + offsetX, y + 3f);
                if (types.size() > 1) {
                    image(b, typeIcon(a, pbs, types.get(1)), x + offsetX + 16f, y + 3f);
                }
            }
            if (hasItemIcon) {                                                 // :138-145 HeldItemIconSprite at (x+8, y+28), zoom 0.5
                Texture item = heldItemIcon(a, pbs, pkmn.item);
                if (item != null) {
                    b.draw(item, x + 8f, h - (y + 28f) - item.getHeight() * 0.5f, item.getWidth() * 0.5f, item.getHeight() * 0.5f);
                }
            }
            if (switch197() || battler.pbOwnedByPlayer()) {                     // :146-160 ability and HP
                String ability = battler.ability == null || battler.ability.isEmpty() ? ""
                        : (pbs != null && pbs.ability(battler.ability) != null ? pbs.ability(battler.ability).name : battler.ability);
                text(b, f, ability, x - 2f, y + 50f, 1, FONT, shadow);
                if (battler.fainted()) {
                    text(b, f, "濒死", x + 2f, y + 50f, 0, FONT, SHADOW);
                } else {
                    text(b, f, battler.hp + "/" + battler.maxHp(), x + 2f, y + 50f, 0, FONT, shadow);
                }
            }
        }
    }

    /** The species the box shows: the Illusion's, else its own ({@code pkmn.species = illusion.species}, :104-106). */
    private PbsData.Species displaySpecies(Battler battler) {
        Object illusion = battler.effects.raw(PBEffects.Battler.Illusion);
        if (illusion instanceof Pokemon && ((Pokemon) illusion).species != null) {
            return ((Pokemon) illusion).species;
        }
        return battler.pokemon == null ? null : battler.pokemon.species;
    }

    /** {@code battler.ownedEx?} (353:625-628): {@code $Trainer.owned[displaySpecies]}, the trainer ignored. */
    private boolean owned(Battler battler) {
        PbsData.Species species = displaySpecies(battler);
        return species != null && context.gameState().trainer().owned.contains(species.internalName);
    }

    private Texture iconTexture(MenuAssets a, PbsData.Species species, boolean shiny) {
        if (species == null) return null;
        Texture icon = PokemonIcons.of(a, species, false, shiny, false, 0, false, false);
        return icon;
    }

    private Texture typeIcon(MenuAssets a, PbsData pbs, String type) {
        PbsData.TypeInfo info = pbs == null || type == null ? null : pbs.type(type);
        return info == null ? null : a.graphic("Pictures/Battle/icon_type", "type" + info.id);
    }

    /** 353:597-617 {@code pbHeldItemIconFile}: item%03d, else the party's item icon. */
    private Texture heldItemIcon(MenuAssets a, PbsData pbs, String id) {
        PbsData.Item item = pbs == null ? null : pbs.item(id);
        Texture icon = item == null ? null : a.icon(String.format("item%03d", item.id));
        if (icon == null) icon = a.graphic("Pictures/Party", "icon_item");
        return icon;
    }

    private void drawStats(SpriteBatch b, MenuAssets a, MenuFont f) {
        Battler battler = statsBattler;
        if (battler == null) return;                                           // a fainted battler: the list was cleared
        float[] xs = {0, 32, 32, w / 2 + 16, 32, 32, w / 2 + 16, w / 2 + 16, w / 2 + 16};
        float[] ys = {0, 120, 142, 120, 164, 186, 140, 164, 186};
        boolean show = switch197() || battler.pbOwnedByPlayer();
        for (int i = 1; i < 8; i++) {                                          // :186-215 for i in 1...stages.length
            String statNum = show ? roughStat(battler, i) : "";
            int stat = battler.stage(i);
            String label = PBStats.getName(i) + "：" + statNum;
            if (stat > 0) {
                text(b, f, label, xs[i], ys[i], 0, FONT, SHADOW_RAISE);
                text(b, f, repeat("▲", stat), xs[i] + 104f, ys[i], 0, FONT, SHADOW_RAISE);
            } else if (stat < 0) {
                text(b, f, label, xs[i], ys[i], 0, FONT, SHADOW_LOSS);
                text(b, f, repeat("▼", -stat), xs[i] + 104f, ys[i], 0, FONT, SHADOW_LOSS);
            } else {
                text(b, f, label, xs[i], ys[i], 0, FONT, SHADOW);
            }
        }
        int focus = battler.effects.intVal(PBEffects.Battler.FocusEnergy);
        if (focus > 0) {                                                       // :216-226
            text(b, f, "会心：", xs[8], ys[8], 0, FONT, SHADOW_RAISE);
            text(b, f, repeat("▲", focus), xs[8] + 96f, ys[8], 0, FONT, SHADOW_RAISE);
        } else {
            text(b, f, "会心：", xs[8], ys[8], 0, FONT, SHADOW);
        }
        text(b, f, "精灵回合数：" + (battler.turnCount + 1), w / 2 + 16, h - 80f, 0, FONT, SHADOW);   // :227-229
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    private void drawField(SpriteBatch b, MenuFont f) {
        text(b, f, "战斗回合数：" + (battle.turnCount() + 1), 32f, h - 80f, 0, FONT, SHADOW);             // :235-237
        int weather = battle.pbWeather();
        String weatherEffect = weather == 0 ? "" : "(有效)";                                          // :241 pbWeather==weather is always true
        text(b, f, "天气：" + WEATHER[Math.max(0, Math.min(WEATHER.length - 1, weather))] + weatherEffect, 32f, h - 56f, 0, FONT, SHADOW);
        if (weather != PBWeather.None) {                                                              // :246-258
            text(b, f, battle.field.weatherDuration > 0 ? battle.field.weatherDuration + "回合" : "持续存在",
                    w / 2 - 16, h - 56f, 1, FONT, SHADOW);
        }
        int terrain = battle.field.terrain;
        text(b, f, "场地：" + (terrain >= 0 && terrain < TERRAIN.length ? TERRAIN[terrain] : ""), w / 2 + 16, h - 56f, 0, FONT, SHADOW);   // :260-266
        if (terrain != PBBattleTerrains.None) {                                                       // :267-278
            text(b, f, battle.field.terrainDuration > 0 ? battle.field.terrainDuration + "回合" : "持续存在",
                    w - 32f, h - 56f, 1, FONT, SHADOW);
        }
        text(b, f, "戏法空间：", 32f, h - 32f, 0, FONT, SHADOW);                                       // :279-281
        int trick = battle.field.effects.intVal(PBEffects.Field.TrickRoom);
        text(b, f, trick > 0 ? trick + "回合" : "不存在", w / 2 - 16, h - 32f, 1, FONT, SHADOW);        // :282-290
        text(b, f, "奇妙空间：", w / 2 + 16, h - 32f, 0, FONT, SHADOW);                                // :291-293
        int wonder = battle.field.effects.intVal(PBEffects.Field.WonderRoom);
        text(b, f, wonder > 0 ? wonder + "回合" : "不存在", w - 32f, h - 32f, 1, FONT, SHADOW);         // :294-302
    }

    /** {@code show_type_calc(battler)} (:322-373): the type icons of the battler and the effectiveness of every type against it. */
    private void drawTypeChart(SpriteBatch b, MenuAssets a) {
        Battler battler = chartBattler;
        PbsData pbs = context.pbsData();
        if (battler == null || pbs == null) return;
        int maxValue = PBTypes.maxValue(pbs);
        float xStart = w / 2 - 16f * (maxValue - 1) / 2f;
        float yStart = h - 234f;
        List<String> types = new ArrayList<>();
        for (String t : battler.pbTypes(true)) types.add(t);
        String type1 = types.size() > 0 ? types.get(0) : null;
        String type2 = types.size() > 1 ? types.get(1) : type1;
        String type3 = types.size() > 2 ? types.get(2) : null;
        if (type3 != null) {                                                   // :336-348
            if (java.util.Objects.equals(type1, type2)) {
                image(b, typeIcon(a, pbs, type1), xStart - 16f * 2, yStart + 8f);
                image(b, typeIcon(a, pbs, type3), xStart - 16f, yStart + 8f);
            } else {
                image(b, typeIcon(a, pbs, type1), xStart - 16f * 3, yStart + 8f);
                image(b, typeIcon(a, pbs, type2), xStart - 16f * 2, yStart + 8f);
                image(b, typeIcon(a, pbs, type3), xStart - 16f, yStart + 8f);
            }
        } else if (java.util.Objects.equals(type1, type2)) {
            image(b, typeIcon(a, pbs, type1), xStart - 16f, yStart + 8f);
        } else {
            image(b, typeIcon(a, pbs, type1), xStart - 16f * 2, yStart + 8f);
            image(b, typeIcon(a, pbs, type2), xStart - 16f, yStart + 8f);
        }
        PbsData.TypeInfo stellar = pbs.type("STELLAR");
        int stellarId = stellar == null ? -1 : stellar.id;
        for (int atkType = 0; atkType < maxValue; atkType++) {                 // :350 PBTypes.maxValue.times
            if (atkType == stellarId) continue;
            int xPos = atkType;
            if (stellarId >= 0 && atkType > stellarId) xPos -= 1;
            String attackName = typeNames == null || atkType >= typeNames.length ? null : typeNames[atkType];
            image(b, a.graphic("Pictures/Battle/icon_type", "type" + atkType), xStart + (xPos + 1) * 16f, yStart);
            int mult = attackName == null ? 8 : PBTypes.getCombinedEffectiveness(pbs, attackName, type1, type2, type3);
            String picture;
            switch (mult) {                                                    // :357-366
                case 8 * 8: picture = "effective_8"; break;
                case 8 * 4: picture = "effective_4"; break;
                case 8 * 2: picture = "effective_2"; break;
                case 8 / 2: picture = "effective_1_2"; break;
                case 8 / 4: picture = "effective_1_4"; break;
                case 8 / 8: picture = "effective_1_8"; break;
                case 0: picture = "effective_0"; break;
                default: picture = "effective_1"; break;
            }
            image(b, a.graphic("Pictures/Battle", picture), xStart + (xPos + 1) * 16f, yStart + 16f);
        }
    }

    // ---------------------------------------------------------------- pbRoughStat (353:379-593)

    /** {@code pbRoughStat(battler, stat)}: the stat with the held item, the ability, the weather and the badges, then its stage. */
    private String roughStat(Battler battler, int stat) {
        if (stat == PBStats.SPEED) {
            return String.valueOf(battler.pbSpeed(numBadges()));               // :380 return battler.pbSpeed
        }
        int[] mul = {2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8};
        int[] div = {8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2};
        int stage = battler.stage(stat) + 6;
        boolean playerowned = battler.battle != null && battler.battle.internalBattle && battler.pbOwnedByPlayer();
        int badges = numBadges();
        double value;
        switch (stat) {
            case PBStats.ATTACK:
                value = battler.baseAttack();
                if (battler.hasActiveItem("CHOICEBAND")) {
                    value *= 1.5;
                } else if (battler.hasActiveItem("LIGHTBALL") && battler.isSpecies("PIKACHU")) {
                    value *= 2;
                } else if (battler.hasActiveItem("THICKCLUB") && (battler.isSpecies("CUBONE") || battler.isSpecies("MAROWAK"))) {
                    value *= 2;
                } else if (battler.hasActiveItem("ABYSSSWORD") && battler.isSpecies("SUJINRAKU")) {
                    value *= 1.2;
                }
                if (battler.hasActiveAbility(new String[] {"HUGEPOWER", "PUREPOWER"})) {
                    value *= 2;
                } else if (battler.hasActiveAbility("FLOWERGIFT") && battler.isSpecies("CHERRIM")) {
                    value *= 1.5;
                } else if (battler.hasActiveAbility("GUTS") && battler.pbHasAnyStatus()) {
                    value *= 1.5;
                } else if (battler.hasActiveAbility("TOXICBOOST") && battler.poisoned()) {
                    value *= 1.5;
                } else if (battler.hasActiveAbility(new String[] {"HUSTLE", "GORILLATACTICS"})) {
                    value *= 1.5;
                } else if (battler.hasActiveAbility("ORICHALCUMPULSE")
                        && (battler.effectiveWeather() == PBWeather.Sun || battler.effectiveWeather() == PBWeather.HarshSun)) {
                    value *= 4 / 3.0;
                } else if (battler.hasActiveAbility("ANGERFLAME")) {
                    boolean lossStages = false;
                    for (int s : PBStats.EACH_MAIN_BATTLE_STAT) {
                        if (battler.stage(s) < 0) {
                            lossStages = true;
                            break;
                        }
                    }
                    if (lossStages || battler.pbHasAnyStatus()) value *= 1.5;
                } else if (battler.hasActiveAbility("DEFEATIST") && battler.hp <= battler.maxHp() / 2) {   // plugin writes user.hp: battler is meant
                    value *= 0.5;
                } else if (battler.hasActiveAbility("WANXIANG") && battler.hp <= battler.maxHp() / 2) {
                    value *= 0.5;
                } else if (battler.hasActiveAbility("SLOWSTART") && battler.effects.intVal(PBEffects.Battler.SlowStart) > 0) {
                    value *= 0.5;
                } else if (battler.burned() && !battler.hasActiveAbility(new String[] {"GUTS", "ANGERFLAME"})) {
                    value *= 0.5;
                }
                if (playerowned && badges >= Battler.NUM_BADGES_BOOST_ATTACK) value *= 1.1;
                break;
            case PBStats.DEFENSE:
                value = battler.baseDefense();
                if (battler.hasActiveItem("METALPOWDER") && battler.isSpecies("DITTO")
                        && !battler.effects.truthy(PBEffects.Battler.Transform)) {
                    value *= 1.5;
                } else if (battler.hasActiveItem("EVIOLITE")) {
                    if (!PendingApi.pbGetEvolvedFormData(battler, true).isEmpty()) value *= 1.5;
                }
                if (battler.hasActiveAbility("MARVELSCALE") && battler.pbHasAnyStatus()) {
                    value *= 1.5;
                } else if (battler.hasActiveAbility("GRASSPELT") && battle.field.terrain == PBBattleTerrains.Grassy) {
                    value *= 1.5;
                }
                if (battler.pbHasType("ICE") && battler.effectiveWeather() == PBWeather.Snow) value *= 1.5;
                if (playerowned && badges >= Battler.NUM_BADGES_BOOST_DEFENSE) value *= 1.1;
                break;
            case PBStats.SPATK:
                value = battler.baseSpAtk();
                if (battler.hasActiveItem("CHOICESPECS")) {
                    value *= 1.5;
                } else if (battler.hasActiveItem("LIGHTBALL") && battler.isSpecies("PIKACHU")) {
                    value *= 2;
                } else if (battler.hasActiveItem("DEEPSEATOOTH") && battler.isSpecies("CLAMPERL")) {
                    value *= 2;
                } else if (battler.hasActiveItem("TEMPLESCEPTER") && battler.isSpecies("SUGARDEVOIR")) {
                    value *= 1.2;
                }
                if (battler.hasActiveAbility("SOLARPOWER")) {
                    value *= 1.5;
                } else if (battler.hasActiveAbility("FLAREBOOST") && battler.burned()) {
                    value *= 1.5;
                } else if (battler.hasActiveAbility("HADRONENGINE") && battle.field.terrain == PBBattleTerrains.Electric) {
                    value *= 4 / 3.0;
                } else if (battler.hasActiveAbility("DEFEATIST") && battler.hp <= battler.maxHp() / 2) {
                    value *= 0.5;
                } else if (battler.hasActiveAbility("WANXIANG") && battler.hp <= battler.maxHp() / 2) {
                    value *= 0.5;
                }
                if (playerowned && badges >= Battler.NUM_BADGES_BOOST_SPATK) value *= 1.1;
                break;
            case PBStats.SPDEF:
                value = battler.baseSpDef();
                if (battler.hasActiveItem("ASSAULTVEST")) {
                    value *= 1.5;
                } else if (battler.hasActiveItem("EVIOLITE")) {
                    if (!PendingApi.pbGetEvolvedFormData(battler, true).isEmpty()) value *= 1.5;
                } else if (battler.hasActiveItem("DEEPSEASCALE") && battler.isSpecies("CLAMPERL")) {
                    value *= 2;
                }
                if (battler.hasActiveAbility("FLOWERGIFT") && battler.isSpecies("CHERRIM")) value *= 1.5;
                if (battler.pbHasType("ROCK") && battler.effectiveWeather() == PBWeather.Sandstorm) value *= 1.5;
                if (playerowned && badges >= Battler.NUM_BADGES_BOOST_SPDEF) value *= 1.1;
                break;
            default:
                return "";                                                     // :571 return ""
        }
        if (battler.effects.intVal(PBEffects.Battler.ParadoxStat) == stat) {   // :574-576 古代活性/夸克充能
            value *= stat == PBStats.ATTACK ? 1.5 : 1.3;
        }
        return String.valueOf((long) Math.floor(value * mul[stage] / div[stage]));
    }

    /** {@code @battle.pbPlayer.numbadges}. */
    private int numBadges() {
        return context.gameState().trainer().badges.size();
    }
}
