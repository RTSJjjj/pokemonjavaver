package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.function.Predicate;

/**
 * Stage 4 / M0: {@code HandlerHash} (Event_Handlers.rb:68-146) - the registry
 * every {@code BattleHandlers::*} table is built from, plus the
 * {@code AbilityHandlerHash}/{@code ItemHandlerHash} subclasses
 * (Event_Handlers.rb:158-170) which only differ in the module they resolve
 * symbols against.
 *
 * <h2>Faithful parts</h2>
 * <ul>
 * <li>{@link #copy} reproduces {@code HandlerHash#copy} (:115-122): the source's
 *     handler is installed for every destination, and a source with no handler
 *     copies nothing. The project uses this 75 times to alias one ability/item
 *     onto another ({@code AbilityOnHPDroppedBelowHalf.copy(:EMERGENCYEXIT,:WIMPOUT)}
 *     BattleHandlers_Abilities:121).</li>
 * <li>{@link #addIf} reproduces the fallback list (:98-103, consulted by
 *     {@code []} at :130-135 when no exact key matches).</li>
 * <li>{@link #get} returns {@code null} for "no handler", which is what Ruby's
 *     {@code nil} means and what every {@code trigger*} wrapper turns into its
 *     documented default.</li>
 * </ul>
 *
 * <h2>Documented deviation: keys are internal names, not numeric PBS ids</h2>
 * Ruby's {@code HandlerHash} resolves a {@code :SYMBOL} to the numeric PBS id
 * through {@code Object.const_get(@mod).const_get(sym)} ({@code fromSymbol},
 * :76-81) and keeps BOTH the id and the symbol as keys (:109-113); {@code
 * trigger} then hands the handler the numeric id (:140). This runtime has keyed
 * every ability/item by its internal name String since stage 3 (see
 * {@code PbsData.Ability#internalName}), so:
 * <ul>
 * <li>keys are the upper-cased internal name ({@code "CHLOROPHYLL"});</li>
 * <li>a handler's first parameter is that same String, not an Integer;</li>
 * <li>{@link #addIf}'s predicate therefore receives the name, not the numeric id
 *     - and nothing in this project registers an {@code addIf} for abilities or
 *     items (0 occurrences in {@code BattleHandlers_Abilities}/{@code _Items}),
 *     so the difference is currently unobservable. If an {@code addIf} is ever
 *     added, this is the place to revisit.</li>
 * </ul>
 *
 * @param <H> the functional interface of one handler group
 */
public final class HandlerHash<H> {

    /** One {@code addIf} entry: {@code [conditionProc, handler]} (Event_Handlers.rb:102-103). */
    private static final class AddIf<H> {
        final Predicate<String> condition;
        final H handler;

        AddIf(Predicate<String> condition, H handler) {
            this.condition = condition;
            this.handler = handler;
        }
    }

    private final ObjectMap<String, H> byName = new ObjectMap<>();
    private final Array<AddIf<H>> addIfs = new Array<>();

    /** {@code add(sym, handler)} (Event_Handlers.rb:105-113). */
    public HandlerHash<H> add(String sym, H handler) {
        if (sym == null) {
            throw new IllegalArgumentException("HandlerHash.add was given no symbol");
        }
        if (handler == null) {
            throw new IllegalArgumentException("HandlerHash for " + sym + " has no valid handler");
        }
        byName.put(sym.toUpperCase(java.util.Locale.ROOT), handler);
        return this;
    }

    /** {@code addIf(conditionProc, handler)} (Event_Handlers.rb:98-103). */
    public HandlerHash<H> addIf(Predicate<String> condition, H handler) {
        if (condition == null || handler == null) {
            throw new IllegalArgumentException("addIf call has no valid handler");
        }
        addIfs.add(new AddIf<>(condition, handler));
        return this;
    }

    /**
     * {@code copy(src, *dests)} (Event_Handlers.rb:115-122): install the
     * source's handler for every destination; do nothing when the source has
     * none.
     */
    public HandlerHash<H> copy(String src, String... dests) {
        H handler = get(src);
        if (handler != null) {
            for (String dest : dests) {
                add(dest, handler);
            }
        }
        return this;
    }

    /**
     * {@code HandlerHash#[]} (Event_Handlers.rb:124-136): the handler for a
     * symbol, or the first matching {@code addIf} handler, or {@code null}.
     */
    public H get(String sym) {
        if (sym == null) {
            return null;
        }
        String key = sym.toUpperCase(java.util.Locale.ROOT);
        H handler = byName.get(key);
        if (handler != null) {
            return handler;
        }
        for (AddIf<H> addIf : addIfs) {
            if (addIf.condition.test(key)) {
                return addIf.handler;
            }
        }
        return null;
    }

    /** {@code HandlerHash#[]} with the project's own name; kept for readability at call sites. */
    public H of(String sym) {
        return get(sym);
    }

    public boolean has(String sym) {
        return get(sym) != null;
    }

    /** {@code clear} (Event_Handlers.rb:143-145). */
    public void clear() {
        byName.clear();
        addIfs.clear();
    }

    public int size() {
        return byName.size;
    }
}
