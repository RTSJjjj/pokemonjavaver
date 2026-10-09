package pokemon.runtime.ui.menu;

import pokemon.runtime.audio.UiSounds;
import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.input.GameAction;
import pokemon.runtime.input.InputManager;
import pokemon.runtime.ui.WindowSkin;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 他段: Messages #71 的 {@code pbMessage(message,commands,cmdIfCancel,skin,defaultCmd)}
 * (:1304-1318) / {@code pbMessageDisplay}(:1007-1297) / {@code pbShowCommands}(:1355-1389)。
 * PScreen_PC 与 B2W2 PC 的整句对话(「登录了精灵寄存服务！」「要登录哪一个服务？」「确定提交…」)用它。
 *
 * <p>只覆盖本段文案用到的控制符: {@code \se[] \me[] \wtnp[] \wt[] \. \| \G \1 \n};
 * 其余控制符(\c[] 取色、\f 头像、\l 行数等)按原文语义被识别后<b>忽略</b>，
 * 登记: 取色(\c[1])未画，消息窗固定 2 行。</p>
 *
 * <p>状态机: 每帧 {@link #update} 一次；{@code ticks} 是 40fps 的 tick 数（{@link MenuClock}）。</p>
 */
final class PbMessage {
    private static final int ROW = 32;
    private static final int BORDER = 32;
    private static final char PAUSE = '\u0001';
    private static final String[] POCKET_NAMES = {"", "道具", "回复道具", "精灵球", "招式学习机", "树果", "超级石", "对战道具", "重要道具", "特殊道具"};

    private final RuntimeContext context;
    private boolean active;
    private String text = "";
    private final List<String[]> controls = new ArrayList<>();   // {type, param, position}
    private int shown;
    private int stepCounter;
    private boolean pausing;
    private int waitcount;
    private boolean autoresume;
    private boolean goldWindow;
    private List<String> commands;
    private int cmdIfCancel;
    private int cmdIndex;
    private boolean cmdPhase;
    private IntConsumer done;
    private int result;

    PbMessage(RuntimeContext context) {
        this.context = context;
    }

    boolean active() {
        return active;
    }

    /** :1304 pbMessage; {@code commands} null = plain message (returns 0). */
    void start(String message, List<String> commands, int cmdIfCancel, int defaultCmd, IntConsumer done) {
        this.commands = commands;
        this.cmdIfCancel = cmdIfCancel;
        this.cmdIndex = defaultCmd;                              // :1362 cmdwindow.index=defaultCmd
        this.done = done;
        this.cmdPhase = false;
        this.pausing = false;
        this.autoresume = false;
        this.goldWindow = false;
        this.waitcount = 0;
        this.shown = 0;
        this.stepCounter = 0;
        this.result = 0;
        parse(message);
        active = true;
        // :1144-1155 a \se[] at position 0 replaces the decision sound
        boolean startSe = false;
        for (int i = 0; i < controls.size(); i++) {
            String[] control = controls.get(i);
            if (control != null && control[0].equals("se") && Integer.parseInt(control[2]) == 0) {
                playSe(control[1]);
                controls.set(i, null);
                startSe = true;
                break;
            }
        }
        if (!startSe) {
            MenuSe.decision(context.audioManager());             // :1154 pbPlayDecisionSE()
        }
    }

    /** :1020-1112 text replacement + control extraction (the subset above). */
    private void parse(String message) {
        controls.clear();
        String source = message == null ? "" : message;
        StringBuilder plain = new StringBuilder();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\\' && i + 1 < source.length()) {
                char next = source.charAt(i + 1);
                if (next == 'n' || next == 'N') {                // :1038 \n
                    plain.append('\n');
                    i += 2;
                    continue;
                }
                if (next == '1') {                               // :1028 \1
                    plain.append(PAUSE);
                    i += 2;
                    continue;
                }
                int j = i + 1;
                StringBuilder name = new StringBuilder();
                while (j < source.length() && Character.isLetter(source.charAt(j))) name.append(source.charAt(j++));
                String type = name.toString().toLowerCase();
                String param = "";
                if (j < source.length() && source.charAt(j) == '[') {
                    int close = source.indexOf(']', j);
                    if (close > 0) {
                        param = source.substring(j + 1, close);
                        j = close + 1;
                    }
                }
                if (type.equals("se") || type.equals("me") || type.equals("wtnp") || type.equals("wt")) {
                    controls.add(new String[] {type, param, String.valueOf(plain.length())});
                    if (type.equals("wt") || type.equals("wtnp")) plain.append('\u0002');   // :1105 textchunks[i] += "\2"
                    i = j;
                    continue;
                }
                if (type.equals("g")) {
                    controls.add(new String[] {"g", "", String.valueOf(plain.length())});
                    i = j;
                    continue;
                }
                if (next == '.' || next == '|') {
                    controls.add(new String[] {String.valueOf(next), "", String.valueOf(plain.length())});
                    plain.append('\u0002');
                    i += 2;
                    continue;
                }
                if (!type.isEmpty()) {                           // 其余控制符: 识别后忽略(登记)
                    i = j;
                    continue;
                }
            }
            plain.append(c);
            i++;
        }
        text = plain.toString();
    }

    private void playSe(String name) {
        if (context.audioManager() != null && name != null && !name.isEmpty()) {
            context.audioManager().playSe(name, 100, 100);
        }
    }

    private boolean busy() {
        return shown < text.length();
    }

    /** One rendered frame: {@code ticks} 40fps ticks of the text window + the input. */
    void update(InputManager input, int ticks) {
        if (!active) return;
        if (!cmdPhase) {
            for (int t = 0; t < ticks; t++) tick();
            boolean confirm = input.wasPressed(GameAction.CONFIRM);
            boolean cancel = input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU);
            if (autoresume && waitcount == 0) {                  // :1251-1254
                if (busy() && pausing) resume();
                if (!busy()) {
                    finishText();
                    return;
                }
            }
            if (confirm || cancel) {                             // :1255-1262
                if (busy()) {
                    if (pausing) {
                        MenuSe.decision(context.audioManager()); // :1257
                        resume();                                // :1258
                    }
                } else if (commands == null) {
                    finishText();
                    return;
                }
            }
            if (commands != null && !busy()) {                   // :1266 break once the text is finished
                finishText();
            }
            return;
        }
        // :1364-1384 pbShowCommands
        int n = commands.size();
        if (input.wasRepeated(GameAction.UP)) {
            cmdIndex = Math.floorMod(cmdIndex - 1, n);
            UiSounds.cursor(context.audioManager());
        } else if (input.wasRepeated(GameAction.DOWN)) {
            cmdIndex = Math.floorMod(cmdIndex + 1, n);
            UiSounds.cursor(context.audioManager());
        }
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) {    // :1370
            if (cmdIfCancel > 0) {
                result = cmdIfCancel - 1;
                close();
            } else if (cmdIfCancel < 0) {
                result = cmdIfCancel;
                close();
            }
        } else if (input.wasPressed(GameAction.CONFIRM)) {       // :1379
            result = cmdIndex;
            close();
        }
    }

    private void resume() {
        pausing = false;
        if (shown < text.length() && text.charAt(shown) == PAUSE) shown++;
    }

    private void finishText() {
        if (commands != null) {
            cmdPhase = true;
        } else {
            close();
        }
    }

    private void close() {
        active = false;
        IntConsumer callback = done;
        done = null;
        if (callback != null) callback.accept(result);
    }

    /** Window_AdvancedTextPokemon#update (letter-by-letter) + the control dispatch (:1181-1243). */
    private void tick() {
        if (waitcount > 0) {
            waitcount--;
            return;
        }
        for (int i = 0; i < controls.size(); i++) {
            String[] control = controls.get(i);
            if (control == null) continue;
            if (Integer.parseInt(control[2]) > shown || waitcount != 0) continue;   // :1183
            switch (control[0]) {
                case "g": goldWindow = true; break;                                  // :1199
                case ".": waitcount += 40 / 4; break;                                // :1224
                case "|": waitcount += 40; break;                                    // :1226
                case "wt": waitcount += parseInt(control[1]) * 40 / 20; break;       // :1228
                case "wtnp": waitcount = parseInt(control[1]) * 40 / 20; autoresume = true; break;   // :1231
                case "se": playSe(control[1]); break;                                // :1237
                case "me": playSe(control[1]); break;                                // 登记: ME 通道未区分，按 SE 播放
                default: break;
            }
            controls.set(i, null);
        }
        if (busy() && !pausing) {
            int speed = context.settings().textspeed;
            int step = speed == 0 ? ((stepCounter++ % 3 == 0) ? 1 : 0) : speed == 2 ? 3 : 1;
            for (int k = 0; k < step && busy(); k++) {
                char c = text.charAt(shown);
                if (c == PAUSE) {
                    pausing = true;
                    break;
                }
                shown++;
            }
        }
        // \2 (wt/wtnp placeholder) is never drawn; a wait with an unfinished window keeps busy() true
        // until the character is skipped:
        while (busy() && !pausing && text.charAt(shown) == '\u0002' && waitcount == 0 && controlsPending(shown) == false) {
            shown++;
        }
    }

    private boolean controlsPending(int position) {
        for (String[] control : controls) {
            if (control != null && Integer.parseInt(control[2]) <= position) return true;
        }
        return false;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }

    // =====================================================================
    // 绘制
    // =====================================================================

    /**
     * @param speech message windowskin (MessageConfig.pbGetSpeechFrame), {@code skin} the
     *               system frame used by the command window (Window_CommandPokemonEx)
     */
    void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech, float w, float h) {
        if (!active) return;
        renderWindow(b, a, f, skin, speech, w, h);
    }

    /**
     * A message window that is still on screen after its text finished
     * ({@code pbMessageDisplay} leaves the scene's own {@code msgwindow} showing; only
     * {@code pbMessage} disposes the window it created).
     */
    static void renderResting(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin speech,
                              String text, float w, float h) {
        float msgHeight = BORDER + 2 * ROW;                      // :989 pbBottomLeftLines(msgwindow,2)
        float msgTop = h - msgHeight;
        Color[] mc = MenuPanel.textColors(speech);
        MenuPanel.window(b, a, speech, 0f, h - msgTop - msgHeight, w, msgHeight);
        String[] lines = (text == null ? "" : text).split("\n", -1);
        for (int i = 0; i < lines.length && i < 2; i++) {
            float top = msgTop + 16f + i * ROW + (ROW - f.lineHeight()) / 2f;
            f.draw(b, lines[i], 16f, h - top, mc[0], mc[1]);
        }
    }

    private void renderWindow(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin, WindowSkin speech,
                              float w, float h) {
        float msgHeight = BORDER + 2 * ROW;                      // :988 pbBottomLeftLines(msgwindow,2)
        float msgTop = h - msgHeight;
        Color[] mc = MenuPanel.textColors(speech);
        MenuPanel.window(b, a, speech, 0f, h - msgTop - msgHeight, w, msgHeight);
        String visible = shown >= text.length() ? text : text.substring(0, shown);
        visible = visible.replace("\u0002", "").replace(String.valueOf(PAUSE), "");
        String[] lines = visible.split("\n", -1);
        for (int i = 0; i < lines.length && i < 2; i++) {
            float top = msgTop + 16f + i * ROW + (ROW - f.lineHeight()) / 2f;
            f.draw(b, lines[i], 16f, h - top, mc[0], mc[1]);
        }
        if (goldWindow) {                                        // :926-940 pbDisplayGoldWindow
            WindowSkin gold = a.skin("goldskin");
            WindowSkin use = gold != null ? gold : skin;
            Color[] gc = MenuPanel.textColors(use);
            String money = "$" + String.format("%,d", context.gameState().trainer().money);
            float gw = Math.max(160f, Math.max(f.width("零花钱："), f.width(money)) + BORDER + 4f);
            float gh = BORDER + 2 * ROW;
            float gtop = msgTop == 0f ? h - gh : 0f;
            MenuPanel.window(b, a, use, 0f, h - gtop - gh, gw, gh);
            f.draw(b, "零花钱：", 16f, h - (gtop + 16f + (ROW - f.lineHeight()) / 2f), gc[0], gc[1]);
            f.drawRight(b, money, gw - 16f, h - (gtop + 16f + ROW + (ROW - f.lineHeight()) / 2f), gc[0], gc[1]);
        }
        if (cmdPhase && commands != null) {                      // :1357-1361 Window_CommandPokemonEx near the message window
            Color[] cc = MenuPanel.textColors(skin);
            float width = 0f;
            for (String label : commands) width = Math.max(width, f.width(label));
            width += 16f + 16f + 4f + BORDER;
            float height = Math.min(BORDER + commands.size() * ROW, h - msgHeight);
            float x = w - width;
            float top = msgTop - height;                         // pbPositionNearMsgWindow :162
            MenuPanel.window(b, a, skin, x, h - top - height, width, height);
            for (int i = 0; i < commands.size(); i++) {
                float rowTop = top + 16f + i * ROW;
                f.draw(b, commands.get(i), x + 32f, h - (rowTop + (ROW - f.lineHeight()) / 2f), cc[0], cc[1]);
            }
            MenuPanel.cursor(b, a, skin, x, h - top - height, height, cmdIndex, cmdIndex, ROW);
        }
    }

    /** 他段: PField/Item Find:185-206 pbReceiveItem 的第二条提示用到的口袋名 (Settings:176 pbPocketNames)。 */
    static String pocketName(int pocket) {
        return pocket >= 0 && pocket < POCKET_NAMES.length ? POCKET_NAMES[pocket] : "";
    }
}
