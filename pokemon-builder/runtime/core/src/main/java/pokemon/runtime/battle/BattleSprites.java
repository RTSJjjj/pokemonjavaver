package pokemon.runtime.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The battle scene's {@code @sprites} hash ({@code PokeBattle_Scene:5}), filled
 * by {@code Scene_Initialize:107-174 pbInitSprites}. The plugin's animation
 * classes address sprites by key ({@code @sprites["pokemon_#{i}"]}), so the
 * runtime needs the same table before any of them can be transcribed.
 *
 * <p>Drawing order is RMXP's: ascending {@code z}, ties broken by creation
 * order (a stable sort).</p>
 *
 * <p><b>{@link #drawOrder()} cache.</b> The battle screen asks for the draw list
 * on every frame ({@code renderSprites}, {@code updateSelectedBob},
 * {@code debugSprites}, the end-of-battle fade), and the list only changes when
 * the table changes or when a sprite's {@code z} changes. Both are detected
 * exactly, without giving up the plain {@code z} field the rest of the code
 * writes:</p>
 * <ul>
 *   <li>{@link #add}, {@link #put} and {@link #remove} are the only structural
 *       changes, and they drop the cache explicitly;</li>
 *   <li>{@code BattleSprite.z} stays a plain field written directly by
 *       {@code BattleScreen} (:477 pbInitSprites' party balls, :483, :508,
 *       :595-610 the backdrop/base/command bar, :635/:657 the trainers, :673
 *       the shadow, :685 the battler), {@code BattleEntryTransition}
 *       (:241-497) and {@code PictureEx.applyTo} (:750), so the cache keeps the
 *       {@code z} of every entry in creation order and re-sorts only when one
 *       of them differs ({@link #snapshotMatches()}).</li>
 * </ul>
 *
 * <p><b>Invariants.</b> {@link #creationOrder} mirrors {@code sprites}' insertion
 * order one-to-one, and the cached list is exactly {@code creationOrder} sorted
 * by ({@code z} asc, creation index asc). The cache is only ever replaced, never
 * mutated, and {@link #drawOrder()} hands out the <em>same</em> read-only list
 * until something invalidates it, so callers must not modify it.</p>
 *
 * <p><b>The class is {@code public} only for instrumentation.</b> The
 * {@code perf*} counters below exist so the hidden-window capture probe in
 * {@code lwjgl3/MenuCapture} can read them across the module boundary
 * ({@code -Dpokemon.capture.perf=1} / {@code POKEMON_CAPTURE_PERF=1}). They are
 * probe meters - read-only snapshots of how often {@link #drawOrder()} ran and
 * how often it had to rebuild - and <b>not game state</b>: nothing reads them to
 * make a decision, and no gameplay, rendering or save path depends on them. The
 * sprite table itself is still only touched from this package.</p>
 */
public final class BattleSprites {

    private final Map<String, BattleSprite> sprites = new LinkedHashMap<>();
    /** Insertion-order mirror of {@link #sprites}, for the sort's tie-breaker. */
    private final List<BattleSprite> creationOrder = new ArrayList<>();
    /** The cached draw list, or null while it has to be rebuilt. */
    private List<BattleSprite> cachedDrawOrder;
    /** {@code creationOrder.get(i).z} when {@link #cachedDrawOrder} was built. */
    private int[] cachedZ;

    // Probe meters, see the class comment: cheap long increments that no game
    // code reads. They are instrumentation, not game state.
    private static long perfDrawOrderCalls;
    private static long perfDrawOrderSorts;
    private static long perfDrawOrderReuses;
    private static int perfDrawListSize;

    BattleSprites() {
    }

    /** {@code drawOrder()} calls since the last {@link #perfReset()} (probe only). */
    public static long perfDrawOrderCalls() {
        return perfDrawOrderCalls;
    }

    /** How many of those had to rebuild and re-sort the list. */
    public static long perfDrawOrderSorts() {
        return perfDrawOrderSorts;
    }

    /** How many reused the cached list. */
    public static long perfDrawOrderReuses() {
        return perfDrawOrderReuses;
    }

    /** Entries in the current draw list (0 before the first {@link #drawOrder()}). */
    public static int perfDrawListSize() {
        return perfDrawListSize;
    }

    /** Zeroes the counters; the cached list itself is left alone. */
    public static void perfReset() {
        perfDrawOrderCalls = 0;
        perfDrawOrderSorts = 0;
        perfDrawOrderReuses = 0;
    }

    BattleSprite get(String key) {
        return sprites.get(key);
    }

    /** {@code pbAddSprite}(name,x,y,filename,viewport): creates and registers. */
    BattleSprite add(String key, BattleSprite.Kind kind) {
        BattleSprite sprite = new BattleSprite(key, kind);
        put(key, sprite);
        return sprite;
    }

    /**
     * Registers (or replaces) a sprite. Ruby's hash keeps the original insertion
     * position when a key is overwritten, and so does
     * {@link LinkedHashMap#put(Object,Object)} - the mirror follows it.
     */
    void put(String key, BattleSprite sprite) {
        BattleSprite previous = sprites.put(key, sprite);
        if (previous == null) {
            creationOrder.add(sprite);
        } else {
            int index = creationOrder.indexOf(previous);
            if (index < 0) {
                creationOrder.add(sprite);          // defensive: never lose the mirror
            } else {
                creationOrder.set(index, sprite);
            }
        }
        invalidateDrawOrder();
    }

    /** {@code @sprites.delete(key)} / {@code pbDisposeSprite}(hash,key). */
    void remove(String key) {
        BattleSprite removed = sprites.remove(key);
        if (removed != null) {
            creationOrder.remove(removed);
        }
        invalidateDrawOrder();
    }

    boolean has(String key) {
        return sprites.containsKey(key);
    }

    int size() {
        return sprites.size();
    }

    /** Every registered sprite, in creation order. */
    Iterable<BattleSprite> all() {
        return sprites.values();
    }

    /**
     * The draw list: creation order sorted by z ({@code Graphics.update} draws
     * each sprite's z in ascending order).
     *
     * <p>Returns the shared cache, so the caller must treat it as read-only; the
     * list is replaced (never edited) whenever the table or a {@code z}
     * changes.</p>
     */
    List<BattleSprite> drawOrder() {
        perfDrawOrderCalls++;
        if (cachedDrawOrder != null && snapshotMatches()) {
            perfDrawOrderReuses++;
            return cachedDrawOrder;
        }
        perfDrawOrderSorts++;
        List<BattleSprite> list = new ArrayList<>(creationOrder);
        list.sort((a, b) -> Integer.compare(a.z, b.z));     // stable: ties keep creation order
        cachedDrawOrder = Collections.unmodifiableList(list);
        cachedZ = new int[creationOrder.size()];
        for (int i = 0; i < cachedZ.length; i++) {
            cachedZ[i] = creationOrder.get(i).z;
        }
        perfDrawListSize = list.size();
        return cachedDrawOrder;
    }

    private void invalidateDrawOrder() {
        cachedDrawOrder = null;
        cachedZ = null;
    }

    /**
     * True while every entry's {@code z} still equals the value the cache was
     * built from, i.e. while the cached sort is still correct. A structural
     * change never reaches this check: it nulls the cache first.
     */
    private boolean snapshotMatches() {
        if (cachedZ == null || cachedZ.length != creationOrder.size()) {
            return false;
        }
        for (int i = 0; i < cachedZ.length; i++) {
            if (cachedZ[i] != creationOrder.get(i).z) {
                return false;
            }
        }
        return true;
    }
}
