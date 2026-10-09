package pokemon.runtime.legacy;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reader for Ruby's {@code Marshal} format 4.8 (what RPG Maker XP / Essentials write into {@code Game.rxdata}). It builds a plain tree:
 * nil = null, true/false = Boolean, Fixnum/Bignum = Long/BigInteger, Float = Double, String = {@link RString}, Symbol = {@link RSymbol},
 * Array = List, Hash = Map, an object = {@link RObject} (class name + instance variables without the "@"), a {@code _dump}ed object
 * = {@link RUserData}, a Struct = {@link RObject} with its members as variables. Nothing is executed and no class has to exist, so a
 * save of any game version can be read. A file made of several consecutive dumps is read one {@link #load()} after the other.
 */
public final class RubyMarshal {

    public static final class RSymbol {
        public final String name;

        RSymbol(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof RSymbol && ((RSymbol) o).name.equals(name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 31 + 7;
        }

        @Override
        public String toString() {
            return ":" + name;
        }
    }

    public static final class RString {
        public final byte[] bytes;
        public Charset charset = StandardCharsets.UTF_8;

        RString(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof RString && java.util.Arrays.equals(((RString) o).bytes, bytes);
        }

        @Override
        public int hashCode() {
            return java.util.Arrays.hashCode(bytes);
        }

        @Override
        public String toString() {
            return new String(bytes, charset);
        }
    }

    public static final class RObject {
        public final String className;
        public final Map<String, Object> vars = new LinkedHashMap<>();

        RObject(String className) {
            this.className = className;
        }

        public Object get(String name) {
            return vars.get(name);
        }

        @Override
        public String toString() {
            return "#<" + className + ">";
        }
    }

    /** An object written by its own {@code _dump} (Table, Color, Tone ...) or by {@code marshal_dump}. */
    public static final class RUserData {
        public final String className;
        public byte[] data;
        public Object marshalled;

        RUserData(String className) {
            this.className = className;
        }
    }

    /** A class or module reference ({@code c}, {@code m}). */
    public static final class RClass {
        public final String name;

        RClass(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class MarshalException extends RuntimeException {
        public MarshalException(String message) {
            super(message);
        }
    }

    private final byte[] buf;
    private int pos;
    private List<RSymbol> symbols;
    private List<Object> objects;

    public RubyMarshal(byte[] data) {
        this.buf = Objects.requireNonNull(data);
    }

    public boolean eof() {
        return pos >= buf.length;
    }

    public int position() {
        return pos;
    }

    /** {@code Marshal.load(f)}: the next dump of the stream. */
    public Object load() {
        symbols = new ArrayList<>();
        objects = new ArrayList<>();
        int major = next(), minor = next();
        if (major != 4 || minor > 8) {
            throw new MarshalException("unsupported Marshal version " + major + "." + minor);
        }
        return read();
    }

    private int next() {
        if (pos >= buf.length) {
            throw new MarshalException("unexpected end of data");
        }
        return buf[pos++] & 0xff;
    }

    private long readLong() {
        int c = (byte) next();
        if (c == 0) return 0;
        if (c > 0) {
            if (4 < c && c < 128) return c - 5;
            long x = 0;
            for (int i = 0; i < c; i++) x |= (long) next() << (8 * i);
            return x;
        }
        if (-129 < c && c < -4) return c + 5;
        c = -c;
        long x = -1;
        for (int i = 0; i < c; i++) {
            x &= ~(0xffL << (8 * i));
            x |= (long) next() << (8 * i);
        }
        return x;
    }

    private byte[] readBytes() {
        int len = (int) readLong();
        if (len < 0 || pos + len > buf.length) {
            throw new MarshalException("bad length " + len);
        }
        byte[] out = new byte[len];
        System.arraycopy(buf, pos, out, 0, len);
        pos += len;
        return out;
    }

    private RSymbol readSymbolBody() {
        RSymbol symbol = new RSymbol(new String(readBytes(), StandardCharsets.UTF_8));
        symbols.add(symbol);
        return symbol;
    }

    private String readSymbol() {
        int type = next();
        if (type == ':') return readSymbolBody().name;
        if (type == ';') return symbols.get((int) readLong()).name;
        throw new MarshalException("symbol expected, got " + (char) type);
    }

    private <T> T register(T value) {
        objects.add(value);
        return value;
    }

    private Object read() {
        return read(null);
    }

    /** @param ivarsHolder when reading under an {@code I} prefix, receives the object whose variables follow */
    private Object read(Object[] ivarsHolder) {
        int type = next();
        switch (type) {
            case '0': return null;
            case 'T': return Boolean.TRUE;
            case 'F': return Boolean.FALSE;
            case 'i': return readLong();
            case ':': return readSymbolBody();
            case ';': return symbols.get((int) readLong());
            case '@': return objects.get((int) readLong());
            case 'I': {
                Object[] holder = new Object[1];
                Object value = read(holder);
                int count = (int) readLong();
                for (int i = 0; i < count; i++) {
                    String name = readSymbol();
                    Object v = read();
                    applyVar(value, name, v);
                }
                return value;
            }
            case '"': return register(new RString(readBytes()));
            case 'f': {
                String s = new String(readBytes(), StandardCharsets.ISO_8859_1);
                int nul = s.indexOf('\0');                                      // old dumps append the raw mantissa after a NUL
                if (nul >= 0) s = s.substring(0, nul);
                double d = s.equals("inf") ? Double.POSITIVE_INFINITY : s.equals("-inf") ? Double.NEGATIVE_INFINITY
                        : s.equals("nan") ? Double.NaN : Double.parseDouble(s);
                return register(d);
            }
            case 'l': {
                int sign = next();
                int len = (int) readLong() * 2;
                byte[] mag = new byte[len];
                for (int i = 0; i < len; i++) mag[len - 1 - i] = (byte) next();
                BigInteger big = new BigInteger(1, mag);
                if (sign == '-') big = big.negate();
                Object v = big.bitLength() < 63 ? (Object) big.longValue() : big;
                return register(v);
            }
            case '[': {
                int n = (int) readLong();
                List<Object> list = register(new ArrayList<>(Math.min(n, 1 << 16)));
                for (int i = 0; i < n; i++) list.add(read());
                return list;
            }
            case '{':
            case '}': {
                int n = (int) readLong();
                Map<Object, Object> map = register(new LinkedHashMap<>());
                for (int i = 0; i < n; i++) {
                    Object key = read();
                    map.put(key, read());
                }
                if (type == '}') read();                                       // the default value
                return map;
            }
            case 'o': {
                String className = readSymbol();
                RObject obj = register(new RObject(className));
                int n = (int) readLong();
                for (int i = 0; i < n; i++) {
                    String name = readSymbol();
                    obj.vars.put(stripAt(name), read());
                }
                return obj;
            }
            case 'S': {
                String className = readSymbol();
                RObject obj = register(new RObject(className));
                int n = (int) readLong();
                for (int i = 0; i < n; i++) {
                    String name = readSymbol();
                    obj.vars.put(name, read());
                }
                return obj;
            }
            case 'u': {
                String className = readSymbol();
                RUserData data = new RUserData(className);
                data.data = readBytes();
                return register(data);
            }
            case 'U': {
                String className = readSymbol();
                RUserData data = register(new RUserData(className));
                data.marshalled = read();
                return data;
            }
            case 'e': {                                                         // extended with a module: the module is irrelevant
                readSymbol();
                return read();
            }
            case 'C': {                                                         // a subclass of String / Array / Hash
                readSymbol();
                return read();
            }
            case 'c':
            case 'm':
            case 'M': return register(new RClass(new String(readBytes(), StandardCharsets.UTF_8)));
            case '/': {
                byte[] source = readBytes();
                next();                                                         // option flags
                return register(new RString(source));
            }
            case 'd': {
                String className = readSymbol();
                RUserData data = register(new RUserData(className));
                data.marshalled = read();
                return data;
            }
            default:
                throw new MarshalException("unknown type '" + (char) type + "' at " + (pos - 1));
        }
    }

    private static String stripAt(String name) {
        return name.startsWith("@") ? name.substring(1) : name;
    }

    /** An {@code I}-wrapped value carries encoding variables ({@code E}, {@code encoding}) or, for objects, extra variables. */
    private static void applyVar(Object value, String name, Object v) {
        if (value instanceof RString) {
            RString s = (RString) value;
            if (name.equals("E")) {
                s.charset = Boolean.TRUE.equals(v) ? StandardCharsets.UTF_8 : StandardCharsets.US_ASCII;
            } else if (name.equals("encoding") && v instanceof RString) {
                try {
                    s.charset = Charset.forName(v.toString());
                } catch (RuntimeException ignored) {
                    // an encoding name Java does not know: keep UTF-8
                }
            }
        } else if (value instanceof RObject) {
            ((RObject) value).vars.put(stripAt(name), v);
        }
    }

    // ---------------------------------------------------------------- reading helpers

    public static String str(Object o) {
        return o == null ? null : o.toString();
    }

    public static long num(Object o, long fallback) {
        return o instanceof Long ? (Long) o : o instanceof Double ? (long) (double) (Double) o : fallback;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Object o) {
        return o instanceof List ? (List<Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    public static Map<Object, Object> map(Object o) {
        return o instanceof Map ? (Map<Object, Object>) o : null;
    }

    /** Indented dump for looking at a save (depth-limited, long lists shortened). */
    public static String dump(Object o, int depth, int maxItems) {
        StringBuilder sb = new StringBuilder();
        dump(sb, o, 0, depth, maxItems);
        return sb.toString();
    }

    private static void dump(StringBuilder sb, Object o, int indent, int depth, int maxItems) {
        String pad = "  ".repeat(indent);
        if (o == null || o instanceof Boolean || o instanceof Number) {
            sb.append(o).append('\n');
        } else if (o instanceof RString) {
            String s = o.toString();
            sb.append('"').append(s.length() > 120 ? s.substring(0, 120) + "..." : s).append("\"\n");
        } else if (o instanceof RSymbol || o instanceof RClass) {
            sb.append(o).append('\n');
        } else if (o instanceof RUserData) {
            RUserData u = (RUserData) o;
            sb.append("<user ").append(u.className).append(u.data != null ? " " + u.data.length + " bytes" : "").append(">\n");
            if (u.marshalled != null && depth > 0) {
                sb.append(pad).append("  ");
                dump(sb, u.marshalled, indent + 1, depth - 1, maxItems);
            }
        } else if (o instanceof List) {
            List<?> l = (List<?>) o;
            sb.append("[").append(l.size()).append("]");
            if (depth <= 0) {
                sb.append(" ...\n");
                return;
            }
            sb.append('\n');
            int shown = 0;
            for (int i = 0; i < l.size() && shown < maxItems; i++) {
                if (l.get(i) == null && l.size() > maxItems) continue;      // skip empty slots of the big arrays
                sb.append(pad).append("  [").append(i).append("] ");
                dump(sb, l.get(i), indent + 1, depth - 1, maxItems);
                shown++;
            }
        } else if (o instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) o;
            sb.append("{").append(m.size()).append("}");
            if (depth <= 0) {
                sb.append(" ...\n");
                return;
            }
            sb.append('\n');
            int shown = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (shown++ >= maxItems) break;
                sb.append(pad).append("  ").append(e.getKey()).append(" => ");
                dump(sb, e.getValue(), indent + 1, depth - 1, maxItems);
            }
        } else if (o instanceof RObject) {
            RObject r = (RObject) o;
            sb.append(r.className).append('\n');
            if (depth <= 0) return;
            for (Map.Entry<String, Object> e : r.vars.entrySet()) {
                sb.append(pad).append("  @").append(e.getKey()).append(" = ");
                dump(sb, e.getValue(), indent + 1, depth - 1, maxItems);
            }
        } else {
            sb.append(o).append('\n');
        }
    }
}
