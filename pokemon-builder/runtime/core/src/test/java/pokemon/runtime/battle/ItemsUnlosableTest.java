package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemsUnlosableTest {
    @Test
    @DisplayName("pbIsUnlosableItem?: species-bound items stay on their holder (188_PItem_Items.rb:172)")
    void speciesBoundItems() {
        assertTrue(ItemsUnlosable.isUnlosable("GRISEOUSORB", "GIRATINA", "PRESSURE"));
        assertFalse(ItemsUnlosable.isUnlosable("GRISEOUSORB", "PIKACHU", "STATIC"));
        assertFalse(ItemsUnlosable.isUnlosable("FISTPLATE", "ARCEUS", "PRESSURE"), "Arceus without Multitype loses its plate");
        assertTrue(ItemsUnlosable.isUnlosable("FISTPLATE", "ARCEUS", "MULTITYPE"));
    }

    @Test
    @DisplayName("project addition: Regigigas' REGISPELL (changes its form) cannot be knocked off")
    void regispellIsUnlosable() {
        assertTrue(ItemsUnlosable.isUnlosable("REGISPELL", "REGIGIGAS", "SLOWSTART"));
        assertFalse(ItemsUnlosable.isUnlosable("REGISPELL", "PIKACHU", "STATIC"));
    }

    @Test
    @DisplayName("project addition: Samurott's CRAFTMIND stays on it in every form (species-keyed)")
    void craftmindIsUnlosable() {
        assertTrue(ItemsUnlosable.isUnlosable("CRAFTMIND", "SAMUROTT", "TORRENT"));
        assertFalse(ItemsUnlosable.isUnlosable("CRAFTMIND", "OSHAWOTT", "TORRENT"));
    }
}
