package pokemon.runtime.battle;

import com.badlogic.gdx.utils.IntMap;

/**
 * Stage 4 / M0: the storage behind every {@code @effects} hash in the plugin -
 * {@code battler.effects}, {@code battle.field.effects},
 * {@code battle.sides[i].effects} and {@code battle.field.positions[i].effects}.
 *
 * <h2>Why this is an {@code Object} map and not an {@code int[]}</h2>
 * The Ruby hashes hold mixed types and the plugin relies on all of them:
 * booleans ({@code @effects[PBEffects::AquaRing] = false}), counters
 * ({@code = 0}), sentinels ({@code = -1}), and even objects
 * ({@code @effects[PBEffects::Illusion] = <a Pokemon>},
 * Battler_Initialize:213). An {@code int[]} cannot hold the Pokemon, and it
 * cannot reproduce Ruby's truthiness, which is the single biggest trap in this
 * port:
 *
 * <ul>
 * <li>Ruby {@code nil} and {@code false} are falsy - an unset key (an array
 *     slot never written) reads back as {@code nil} and is therefore falsy.</li>
 * <li>Ruby <b>{@code 0} is TRUTHY</b>. {@code if battler.effects[X]} is true
 *     even when {@code effects[X] == 0}. So "is it set?" and "is it non-zero?"
 *     are DIFFERENT questions and the plugin asks both.</li>
 * </ul>
 *
 * <p>So the rule at every call site - and the reason both accessors exist:</p>
 * <table border="1">
 * <caption>Reading a Ruby effect</caption>
 * <tr><th>Ruby</th><th>Java</th></tr>
 * <tr><td>{@code if b.effects[X]} / {@code unless b.effects[X]} / {@code &&} / {@code ||}</td>
 *     <td>{@link #truthy(int)}</td></tr>
 * <tr><td>{@code b.effects[X] > 0}, {@code == 0}, {@code != 0}, {@code + 1}, {@code - 1}</td>
 *     <td>{@link #intVal(int)}</td></tr>
 * <tr><td>{@code b.effects[X] = true} / {@code = false}</td>
 *     <td>{@code set(X, true)} / {@code set(X, false)}</td></tr>
 * <tr><td>{@code b.effects[X] = 3} / {@code = -1}</td><td>{@code set(X, 3)}</td></tr>
 * <tr><td>{@code b.effects[X] = nil}</td><td>{@code set(X, null)}</td></tr>
 * </table>
 *
 * <p>{@link #truthy(int)} therefore treats {@code 0} as true, exactly like
 * Ruby; do not "simplify" it to {@code intVal(idx) != 0}.</p>
 */
public final class EffectMap {

    private final IntMap<Object> map = new IntMap<>();

    /** The raw Ruby value; {@code null} means the key was never written ({@code nil}). */
    public Object raw(int idx) {
        return map.get(idx);
    }

    public boolean has(int idx) {
        return map.containsKey(idx);
    }

    /** {@code @effects[idx] = value}; a {@code null} value writes Ruby's {@code nil}. */
    public void set(int idx, Object value) {
        if (value == null) {
            map.remove(idx);
        } else {
            map.put(idx, value);
        }
    }

    public void set(int idx, boolean value) {
        map.put(idx, value);
    }

    public void set(int idx, int value) {
        map.put(idx, value);
    }

    public void clear() {
        map.clear();
    }

    /**
     * Ruby truthiness of {@code @effects[idx]}: {@code nil} and {@code false}
     * are falsy, <b>everything else including {@code 0} and {@code ""} is
     * truthy</b>.
     */
    public boolean truthy(int idx) {
        return truthyValue(map.get(idx));
    }

    /** Ruby truthiness of an arbitrary value (shared with code that holds the value already). */
    public static boolean truthyValue(Object value) {
        return value != null && !Boolean.FALSE.equals(value);
    }

    /** {@code @effects[idx]} read as an Integer; {@code nil} and non-numbers read as 0. */
    public int intVal(int idx) {
        Object value = map.get(idx);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        return 0;
    }

    /** {@code @effects[idx]} read as an Integer, with an explicit fallback for {@code nil}. */
    public int intVal(int idx, int fallback) {
        Object value = map.get(idx);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        return fallback;
    }

    /** {@code @effects[idx] += delta}, treating an unset key as 0. */
    public void add(int idx, int delta) {
        map.put(idx, intVal(idx) + delta);
    }

    /** {@code @effects[idx] -= 1}. */
    public void decrement(int idx) {
        add(idx, -1);
    }

    /** {@code @effects[idx] += 1}. */
    public void increment(int idx) {
        add(idx, 1);
    }

    /** {@code @effects[idx]} read as a String; {@code nil} reads as {@code null}. */
    public String stringVal(int idx) {
        Object value = map.get(idx);
        return value instanceof String ? (String) value : null;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
