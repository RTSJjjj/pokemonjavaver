package pokemon.runtime.legacy;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Test helper: writes the Marshal 4.8 subset a Pokémon save uses (no symbol / object links, which are only an optimisation). */
final class MarshalWriter {

    /** An object to write: class name and ivars (without "@"). */
    static final class Obj {
        final String className;
        final Map<String, Object> vars = new LinkedHashMap<>();

        Obj(String className) {
            this.className = className;
        }

        Obj set(String name, Object value) {
            vars.put(name, value);
            return this;
        }
    }

    /** A Ruby Time-like user dump. */
    static final class User {
        final String className;
        final byte[] data;

        User(String className, byte[] data) {
            this.className = className;
            this.data = data;
        }
    }

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    static byte[] dump(Object... roots) {
        MarshalWriter w = new MarshalWriter();
        for (Object root : roots) {
            w.out.write(4);
            w.out.write(8);
            w.write(root);
        }
        return w.out.toByteArray();
    }

    private void symbol(String name) {
        out.write(':');
        bytes(name.getBytes(StandardCharsets.UTF_8));
    }

    private void bytes(byte[] b) {
        number(b.length);
        out.write(b, 0, b.length);
    }

    private void number(long n) {
        if (n == 0) {
            out.write(0);
        } else if (0 < n && n < 123) {
            out.write((int) n + 5);
        } else if (-124 < n && n < 0) {
            out.write(((int) n - 5) & 0xff);
        } else {
            byte[] buf = new byte[9];
            long x = n;
            int i = 1;
            for (; i <= 8; i++) {
                buf[i] = (byte) (x & 0xff);
                x >>= 8;
                if (x == 0) {
                    buf[0] = (byte) i;
                    break;
                }
                if (x == -1) {
                    buf[0] = (byte) -i;
                    break;
                }
            }
            out.write(buf, 0, i + 1);
        }
    }

    private void write(Object o) {
        if (o == null) {
            out.write('0');
        } else if (o instanceof Boolean) {
            out.write((Boolean) o ? 'T' : 'F');
        } else if (o instanceof Integer || o instanceof Long) {
            long n = ((Number) o).longValue();
            if (n >= -(1L << 30) && n < (1L << 30)) {
                out.write('i');
                number(n);
            } else {                                                   // a bignum: sign, length in shorts, little-endian magnitude
                boolean neg = n < 0;
                long mag = Math.abs(n);
                byte[] b = new byte[8];
                for (int i = 0; i < 8; i++) b[i] = (byte) (mag >> (8 * i));
                out.write('l');
                out.write(neg ? '-' : '+');
                number(4);
                out.write(b, 0, 8);
            }
        } else if (o instanceof String) {
            out.write('I');
            out.write('"');
            bytes(((String) o).getBytes(StandardCharsets.UTF_8));
            number(1);
            symbol("E");
            out.write('T');
        } else if (o instanceof List) {
            List<?> list = (List<?>) o;
            out.write('[');
            number(list.size());
            for (Object item : list) write(item);
        } else if (o instanceof Obj) {
            Obj obj = (Obj) o;
            out.write('o');
            symbol(obj.className);
            number(obj.vars.size());
            for (Map.Entry<String, Object> e : obj.vars.entrySet()) {
                symbol("@" + e.getKey());
                write(e.getValue());
            }
        } else if (o instanceof User) {
            User u = (User) o;
            out.write('u');
            symbol(u.className);
            bytes(u.data);
        } else {
            throw new IllegalArgumentException("cannot write " + o.getClass());
        }
    }
}
