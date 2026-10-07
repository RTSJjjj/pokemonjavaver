package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T4: {@link BattleSprites#drawOrder()} caches the sorted draw list. The cache
 * must be indistinguishable from the sort it replaces, and it must notice every
 * change the sort would have seen - including a bare {@code sprite.z = ...},
 * which is how {@code BattleScreen} (:470/:476/:501/:588/:590/:597/:603/:628/
 * :650/:666/:678), {@code BattleEntryTransition} and {@code PictureEx.applyTo}
 * (:750) write it.
 */
class BattleSpritesDrawOrderTest {

    /**
     * The pre-T4 implementation, verbatim: a fresh list of the table, sorted by
     * {@code z} with a stable sort (ties keep creation order).
     */
    private static List<BattleSprite> referenceOrder(BattleSprites table) {
        List<BattleSprite> list = new ArrayList<>();
        for (BattleSprite sprite : table.all()) {
            list.add(sprite);
        }
        list.sort((a, b) -> Integer.compare(a.z, b.z));
        return list;
    }

    private static BattleSprites table(int count) {
        BattleSprites sprites = new BattleSprites();
        for (int i = 0; i < count; i++) {
            sprites.add("sprite_" + i, BattleSprite.Kind.IMAGE);
        }
        return sprites;
    }

    private static List<String> keys(List<BattleSprite> sprites) {
        List<String> out = new ArrayList<>(sprites.size());
        for (BattleSprite sprite : sprites) {
            out.add(sprite.key);
        }
        return out;
    }

    @Test
    @DisplayName("drawOrder sorts by z and keeps creation order for equal z")
    void sortedByZThenCreationOrder() {
        BattleSprites table = table(6);
        int[] z = { 5, 1, 5, 0, -3, 1 };
        int i = 0;
        for (BattleSprite sprite : table.all()) {
            sprite.z = z[i++];
        }
        List<BattleSprite> order = table.drawOrder();
        assertEquals(List.of("sprite_4", "sprite_3", "sprite_1", "sprite_5", "sprite_0", "sprite_2"),
                keys(order), "z = [-3, 0, 1, 1, 5, 5], ties by creation order");
        assertEquals(keys(referenceOrder(table)), keys(order));
    }

    @Test
    @DisplayName("the cached list is reused until a z changes")
    void cacheIsReusedUntilZChanges() {
        BattleSprites table = table(3);
        table.get("sprite_0").z = 2;
        table.get("sprite_1").z = 0;
        table.get("sprite_2").z = 1;
        BattleSprites.perfReset();

        List<BattleSprite> first = table.drawOrder();
        List<BattleSprite> second = table.drawOrder();
        assertSame(first, second, "nothing changed: the same list comes back");
        assertEquals(List.of("sprite_1", "sprite_2", "sprite_0"), keys(first));
        assertEquals(2L, BattleSprites.perfDrawOrderCalls());
        assertEquals(1L, BattleSprites.perfDrawOrderSorts(), "only the first call sorted");
        assertEquals(1L, BattleSprites.perfDrawOrderReuses());

        // BattleScreen writes the bare field, so the cache cannot rely on a setter.
        table.get("sprite_2").z = -10;
        List<BattleSprite> third = table.drawOrder();
        assertNotSame(first, third);
        assertEquals(List.of("sprite_2", "sprite_1", "sprite_0"), keys(third));
        assertEquals(2L, BattleSprites.perfDrawOrderSorts());
        assertEquals(3, BattleSprites.perfDrawListSize());
    }

    @Test
    @DisplayName("add / remove / put invalidate the cache")
    void structuralChangesInvalidate() {
        BattleSprites table = table(2);
        BattleSprite firstSprite = table.get("sprite_0");
        List<BattleSprite> before = table.drawOrder();

        table.add("sprite_2", BattleSprite.Kind.POKEMON);
        List<BattleSprite> afterAdd = table.drawOrder();
        assertNotSame(before, afterAdd);
        assertEquals(3, afterAdd.size());

        table.remove("sprite_0");
        List<BattleSprite> afterRemove = table.drawOrder();
        assertNotSame(afterAdd, afterRemove);
        // Every sprite still has z = 0, so the stable sort keeps creation order.
        assertEquals(List.of("sprite_1", "sprite_2"), keys(afterRemove), "sprite_0 is gone");
        assertFalse(afterRemove.contains(firstSprite));

        // Ruby's hash keeps the original position when a key is overwritten.
        BattleSprite replacement = new BattleSprite("sprite_1", BattleSprite.Kind.SHADOW);
        replacement.z = 99;
        table.put("sprite_1", replacement);
        assertSame(replacement, table.get("sprite_1"));
        List<BattleSprite> afterReplace = table.drawOrder();
        assertNotSame(afterRemove, afterReplace);
        assertEquals(List.of("sprite_2", "sprite_1"), keys(afterReplace),
                "z = 0 then 99, so the replacement is drawn last");
        assertEquals(99, afterReplace.get(1).z);
        assertEquals(2, table.size());
        assertEquals(keys(referenceOrder(table)), keys(afterReplace));
    }

    @Test
    @DisplayName("200 randomised z mutations always match the uncached sort")
    void alwaysMatchesUncachedReference() {
        BattleSprites table = table(12);
        Random random = new Random(20261007L);
        for (int round = 0; round < 200; round++) {
            // Structural changes first: the reference below must see exactly the
            // table the cached list was built from.
            if ((round % 4) == 0) {
                table.add("extra_" + round, BattleSprite.Kind.IMAGE);
            }
            if ((round % 7) == 0 && table.size() > 4) {
                table.remove("extra_" + (round - 4));
            }
            for (BattleSprite sprite : table.all()) {
                sprite.z = random.nextInt(7) - 3;               // duplicates on purpose
            }
            List<BattleSprite> cached = table.drawOrder();
            table.drawOrder();                                  // exercise the reuse path too
            assertEquals(keys(referenceOrder(table)), keys(cached), "round " + round);
        }
    }

    @Test
    @DisplayName("the shared list is read-only, so the cache cannot be corrupted")
    void cachedListIsReadOnly() {
        BattleSprites table = table(2);
        List<BattleSprite> order = table.drawOrder();
        assertThrows(UnsupportedOperationException.class, order::clear);
        assertSame(order, table.drawOrder(), "a failed mutation left the cache intact");
        assertEquals(2, order.size());
    }

    /**
     * T4 numbers. The steady state is the hot path ({@code BattleScreen} asks
     * for the draw list several times per rendered frame); the "every z changes"
     * worst case never reuses the cache, and is then one z-comparison pass more
     * expensive than the old sort. The assertion is deliberately loose (a real
     * regression here is an order of magnitude, not a few percent).
     */
    @Test
    @DisplayName("T4 numbers: cached drawOrder vs the uncached sort")
    void cacheCost() {
        BattleSprites table = table(24);
        for (int i = 0; i < 24; i++) {
            table.get("sprite_" + i).z = (i * 7) % 24;
        }
        int warmup = 20000;
        int rounds = 500000;
        for (int i = 0; i < warmup; i++) {
            table.drawOrder();
            referenceOrder(table);
        }
        long cachedNanos = 0L;
        long cachedChecksum = 0L;
        long start = System.nanoTime();
        for (int i = 0; i < rounds; i++) {
            cachedChecksum += table.drawOrder().get(0).z;
        }
        cachedNanos = System.nanoTime() - start;

        long referenceNanos;
        long referenceChecksum = 0L;
        start = System.nanoTime();
        for (int i = 0; i < rounds; i++) {
            referenceChecksum += referenceOrder(table).get(0).z;
        }
        referenceNanos = System.nanoTime() - start;

        assertEquals(referenceChecksum, cachedChecksum, "both orders must agree");
        System.out.printf(java.util.Locale.ROOT,
                "T4 drawOrder(24 sprites, %d rounds): cached %.1f ns/call, uncached %.1f ns/call, %.1fx%n",
                rounds, cachedNanos / (double) rounds, referenceNanos / (double) rounds,
                referenceNanos / (double) Math.max(1L, cachedNanos));
        assertTrue(cachedNanos <= referenceNanos,
                "the cached path must not be slower than the sort it replaces");
    }
}
