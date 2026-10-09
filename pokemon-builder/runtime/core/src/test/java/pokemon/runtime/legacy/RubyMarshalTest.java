package pokemon.runtime.legacy;

import org.junit.jupiter.api.Test;
import pokemon.runtime.legacy.RubyMarshal.RObject;
import pokemon.runtime.legacy.RubyMarshal.RString;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RubyMarshalTest {

    private static byte[] hex(String text) {
        String[] parts = text.trim().split("\\s+");
        byte[] out = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = (byte) Integer.parseInt(parts[i], 16);
        return out;
    }

    private static Object load(String hex) {
        return new RubyMarshal(hex(hex)).load();
    }

    @Test
    void singletonsAndIntegers() {
        assertNull(load("04 08 30"));
        assertEquals(Boolean.TRUE, load("04 08 54"));
        assertEquals(Boolean.FALSE, load("04 08 46"));
        assertEquals(0L, load("04 08 69 00"));
        assertEquals(5L, load("04 08 69 0a"));
        assertEquals(-1L, load("04 08 69 fa"));
        assertEquals(122L, load("04 08 69 7f"));
        assertEquals(-123L, load("04 08 69 80"));
        assertEquals(300L, load("04 08 69 02 2c 01"));
        assertEquals(-300L, load("04 08 69 fe d4 fe"));
        assertEquals(1L << 40, load("04 08 6c 2b 08 00 00 00 00 00 01"));       // a bignum: sign, 3 shorts
    }

    @Test
    void stringsKeepTheirBytesAndEncoding() {
        Object s = load("04 08 49 22 07 61 62 06 3a 06 45 54");                  // I"ab" with E = true
        assertTrue(s instanceof RString);
        assertEquals("ab", s.toString());
        Object cjk = new RubyMarshal(MarshalWriter.dump("沐桐")).load();
        assertEquals("沐桐", cjk.toString());
    }

    @Test
    void floatsIgnoreTheRawTail() {
        assertEquals(1.5, (Double) load("04 08 66 08 31 2e 35"), 0.0);
        String body = "12.800000000000001\0ª»";                           // older dumps append the mantissa after a NUL
        byte[] raw = body.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        byte[] dump = new byte[4 + raw.length];
        dump[0] = 4;
        dump[1] = 8;
        dump[2] = 'f';
        dump[3] = (byte) (raw.length + 5);
        System.arraycopy(raw, 0, dump, 4, raw.length);
        assertEquals(12.800000000000001, (Double) new RubyMarshal(dump).load(), 0.0);
    }

    @Test
    void arraysHashesAndLinks() {
        List<Object> a = RubyMarshal.list(load("04 08 5b 08 69 06 3a 06 61 3b 00"));          // [1, :a, :a]
        assertEquals(3, a.size());
        assertEquals(1L, a.get(0));
        assertEquals(a.get(1), a.get(2));
        List<Object> same = RubyMarshal.list(load("04 08 5b 07 49 22 06 78 06 3a 06 45 54 40 06"));   // ["x", <same object>]
        assertSame(same.get(0), same.get(1));
        Map<Object, Object> h = RubyMarshal.map(load("04 08 7b 06 69 06 69 07"));
        assertEquals(2L, h.get(1L));
    }

    @Test
    void objectsCarryTheirInstanceVariables() {
        Object o = load("04 08 6f 3a 08 46 6f 6f 06 3a 07 40 78 69 0a");
        assertTrue(o instanceof RObject);
        assertEquals("Foo", ((RObject) o).className);
        assertEquals(5L, ((RObject) o).get("x"));
    }

    @Test
    void severalDumpsInOneFileAreReadOneAfterTheOther() {
        RubyMarshal m = new RubyMarshal(hex("04 08 69 06 04 08 69 07 04 08 30"));
        assertEquals(1L, m.load());
        assertEquals(2L, m.load());
        assertNull(m.load());
        assertTrue(m.eof());
    }

    @Test
    void roundTripsWhatTheTestWriterMakes() {
        MarshalWriter.Obj obj = new MarshalWriter.Obj("PokeBattle_Pokemon").set("species", 131).set("name", "拉普拉斯")
                .set("personalID", 4027637648L).set("iv", List.of(31, 0, 5, 6, 7, 8)).set("mail", null).set("flag", true);
        RObject back = (RObject) new RubyMarshal(MarshalWriter.dump(obj)).load();
        assertEquals(131L, back.get("species"));
        assertEquals("拉普拉斯", back.get("name").toString());
        assertEquals(4027637648L, back.get("personalID"));
        assertEquals(6, RubyMarshal.list(back.get("iv")).size());
        assertNull(back.get("mail"));
        assertEquals(Boolean.TRUE, back.get("flag"));
    }

    @Test
    void garbageIsRejectedNotMisread() {
        assertThrows(RubyMarshal.MarshalException.class, () -> new RubyMarshal(hex("03 08 30")).load());
        assertThrows(RubyMarshal.MarshalException.class, () -> new RubyMarshal(hex("04 08 5b 08 69")).load());
        assertThrows(RubyMarshal.MarshalException.class, () -> new RubyMarshal(hex("04 08 7a")).load());
    }
}
