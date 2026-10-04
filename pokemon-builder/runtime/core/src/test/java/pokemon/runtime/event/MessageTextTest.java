package pokemon.runtime.event;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.state.GameState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R6.31: the message markup keeps the styling the skinned window draws
 * (colours, centring), extracts the per-message skin and line count, and
 * wraps by half-width units with colour carry-over.
 */
class MessageTextTest {

    private static Array<String> lines(String... raw) {
        Array<String> list = new Array<>();
        for (String line : raw) {
            list.add(line);
        }
        return list;
    }

    private static GameState state() {
        GameState state = new GameState();
        state.enterMap(1, 0, 0);
        return state;
    }

    @Test
    @DisplayName("colours and centring survive the clean-up; other tags go")
    void keepsDrawableStyling() {
        MessageText.Parsed parsed = MessageText.parse(lines(
                "\\c[2]红色<b>粗体</b><ac>居中</ac><c3=112233,445566>绿</c3>"), state());
        String line = parsed.lines.first();
        assertTrue(line.contains("\\c[2]"), line);
        assertTrue(line.contains("<ac>"), line);
        assertTrue(line.contains("<c3=112233,445566>"), line);
        assertTrue(line.contains("</c3>"), line);
        assertFalse(line.contains("<b>"), line);
        assertTrue(line.startsWith("\\c[2]红色粗体"), line);
    }

    @Test
    @DisplayName("\\w[skin] / \\sign[skin] and \\l[n] are extracted")
    void extractsSkinAndLineCount() {
        MessageText.Parsed parsed = MessageText.parse(
                lines("\\w[sign bw town]\\l[5]你好"), state());
        assertEquals("sign bw town", parsed.skin);
        assertEquals(5, parsed.lineCount);
        assertEquals("你好", parsed.lines.first());

        MessageText.Parsed sign = MessageText.parse(lines("\\sign[signskin]牌子"), state());
        assertEquals("signskin", sign.skin);
        assertEquals("牌子", sign.lines.first());

        // \w[] means "no window" - an empty skin name, not null.
        MessageText.Parsed none = MessageText.parse(lines("\\w[]透明"), state());
        assertEquals("", none.skin);
    }

    @Test
    @DisplayName("\\b / \\r use the name-box plugin's colours")
    void blueAndRed() {
        MessageText.Parsed parsed = MessageText.parse(lines("\\b蓝\\r红"), state());
        assertEquals("<c2=6546675A>蓝<c2=043C675A>红", parsed.lines.first());
    }

    @Test
    @DisplayName("\\n splits into real lines")
    void lineBreakSplits() {
        MessageText.Parsed parsed = MessageText.parse(lines("第一行\\n第二行"), state());
        assertEquals(2, parsed.lines.size);
        assertEquals("第一行", parsed.lines.first());
        assertEquals("第二行", parsed.lines.get(1));
    }

    @Test
    @DisplayName("wrapping counts half-width units and keeps colours on both halves")
    void wrapByUnits() {
        Array<String> plain = lines("一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十");
        MessageText.wrap(plain, 54);
        assertEquals(2, plain.size);
        assertEquals(27, plain.first().length());
        assertEquals(3, plain.peek().length());

        Array<String> coloured = lines("\\c[2]一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十");
        MessageText.wrap(coloured, 54);
        assertEquals(2, coloured.size);
        assertTrue(coloured.first().startsWith("\\c[2]"), coloured.first());
        assertTrue(coloured.peek().startsWith("\\c[2]"), coloured.peek());
        assertEquals(5 + 27, coloured.first().length());
        assertEquals(5 + 3, coloured.peek().length());
    }
}
