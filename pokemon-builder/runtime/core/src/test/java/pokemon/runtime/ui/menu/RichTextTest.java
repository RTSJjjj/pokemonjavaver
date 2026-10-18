package pokemon.runtime.ui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RichTextTest {
    @Test void plainDropsColourAndIconCodes() {
        assertEquals("你将糖果放进了道具口袋。", RichText.plain("你将糖果放进了<icon=bagPocket1>\\c[1]道具口袋\\c[0]。"));
        assertEquals("X减免了溢出的伤害！", RichText.plain("<c3=FFEE88,FF6600>X减免了溢出的伤害！</c3>"));
        assertEquals("a<b", RichText.plain("a<b"));
    }

    @Test void unfinishedCodesAtTheEndOfARevealedPrefixAreHidden() {
        assertEquals("X", RichText.plain("X<c3=FFEE"));
        assertEquals("X", RichText.plain("X<ic"));
        assertEquals("X", RichText.plain("X\\c[1"));
        assertEquals("X", RichText.plain("X</c3"));
    }

    @Test void revealSkipsCodes() {
        String text = "<c3=FFEE88,FF6600>AB</c3>";
        int first = RichText.advance(text, 0, 1);
        assertEquals("A", RichText.plain(text.substring(0, first)));
        assertEquals(text.length(), RichText.advance(text, first, 1));
    }
}
