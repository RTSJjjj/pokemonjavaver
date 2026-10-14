package pokemon.runtime.pokemon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 198_Pokemon_Forms getForm / onSetForm / getFormOnEnteringBattle with the project's real data. */
class FormHandlersRealDataTest {

    private static PbsData data;

    private static PbsData data() {
        if (data != null) return data;
        for (File d = new File("").getAbsoluteFile(); d != null; d = d.getParentFile()) {
            for (String candidate : new String[] {"generated", "pokemon-builder/generated"}) {
                File root = new File(d, candidate);
                if (new File(root, "pbs").isDirectory()) return data = PbsData.parse(root);
            }
        }
        assumeTrue(false, "generated/pbs not found");
        return null;
    }

    private static Pokemon make(String species) {
        Pokemon p = new Pokemon(data().species(species), 50, data());
        p.item = null;
        return p;
    }

    private static FormsOnCreation.World world(int mapId, int region, boolean night) {
        return new FormsOnCreation.World() {
            @Override public int environment() { return 0; }
            @Override public int region() { return region; }
            @Override public boolean evening() { return false; }
            @Override public boolean night() { return night; }
            @Override public boolean switchOn(int id) { return false; }
            @Override public int mapId() { return mapId; }
        };
    }

    private static final FormsOnCreation.World DAY = world(1, 0, false);

    private static void teach(Pokemon p, String... moves) {
        p.moves.clear();
        for (String m : moves) p.moves.add(new Pokemon.MoveSlot(data().move(m)));
    }

    private static List<String> moves(Pokemon p) {
        List<String> out = new ArrayList<>();
        for (Pokemon.MoveSlot s : p.moves) out.add(s.move.internalName);
        return out;
    }

    @Test
    @DisplayName("Zacian takes the Crowned form with the Rusted Sword and goes back without it")
    void zacianRustedSword() {
        Pokemon z = make("ZACIAN");
        int hero = z.attack();
        z.item = "RUSTEDSWORD";
        assertTrue(FormHandlers.refresh(z, data(), DAY, null));
        assertEquals(1, z.formIndex());
        assertTrue(z.attack() > hero, "the Crowned form's Attack");
        z.item = null;
        FormHandlers.refresh(z, data(), DAY, null);
        assertEquals(0, z.formIndex());
    }

    @Test
    @DisplayName("Arceus follows its plate only with Multitype; Giratina's maps 157/165")
    void plateAndOrb() {
        Pokemon a = make("ARCEUS");
        a.ability = "MULTITYPE";
        a.item = "FLAMEPLATE";
        FormHandlers.refresh(a, data(), DAY, null);
        assertEquals(10, a.formIndex());
        Pokemon g = make("GIRATINA");
        FormHandlers.refresh(g, data(), world(157, 0, false), null);
        assertEquals(1, g.formIndex());
        FormHandlers.refresh(g, data(), DAY, null);
        assertEquals(0, g.formIndex());
    }

    @Test
    @DisplayName("Deerling's season, Hoopa's three days")
    void seasonAndHoopa() {
        Pokemon d = make("DEERLING");
        assertEquals(1, FormHandlers.getForm(d, DAY, LocalDateTime.of(2026, 7, 1, 12, 0)));
        assertEquals(3, FormHandlers.getForm(d, DAY, LocalDateTime.of(2026, 1, 1, 12, 0)));
        Pokemon h = make("HOOPA");
        FormHandlers.setForm(h, data(), 1, null);
        assertTrue(h.formTime > 0);
        assertEquals(-1, FormHandlers.getForm(h, DAY, LocalDateTime.now()));
        assertEquals(0, FormHandlers.getForm(h, DAY, LocalDateTime.now().plusDays(4)));
    }

    @Test
    @DisplayName("onSetForm: Rotom learns its appliance move, Kyurem swaps Glaciate for Ice Burn")
    void onSetFormMoves() {
        Pokemon r = make("ROTOM");
        teach(r, "THUNDERSHOCK");
        FormHandlers.setForm(r, data(), 1, null);
        assertTrue(moves(r).contains("OVERHEAT"), moves(r).toString());
        FormHandlers.setForm(r, data(), 0, null);
        assertFalse(moves(r).contains("OVERHEAT"), moves(r).toString());

        Pokemon k = make("KYUREM");
        teach(k, "GLACIATE", "SCARYFACE");
        FormHandlers.setForm(k, data(), 1, null);
        assertEquals(List.of("ICEBURN", "FUSIONFLARE"), moves(k));
    }

    @Test
    @DisplayName("A plain Pikachu moving between regions keeps Meteor Mash (the plugin's [3..8] never matched)")
    void pikachuKeepsMoves() {
        Pokemon p = make("PIKACHU");
        teach(p, "METEORMASH", "THUNDERSHOCK");
        FormHandlers.refresh(p, data(), world(1, 1, false), null);
        assertEquals(1, p.formIndex());
        FormHandlers.refresh(p, data(), DAY, null);
        assertEquals(0, p.formIndex());
        assertTrue(moves(p).contains("METEORMASH"));
    }

    @Test
    @DisplayName("getFormOnEnteringBattle: Ogerpon's Terastal form, a wild Minior's Meteor form")
    void enteringBattle() {
        Pokemon o = make("OGERPON");
        assertTrue(FormHandlers.onEnteringBattle(o, data(), false));
        assertEquals(4, o.formIndex());
        Pokemon m = make("MINIOR");
        FormHandlers.setForm(m, data(), 9, null);
        assertTrue(FormHandlers.onEnteringBattle(m, data(), true));
        assertEquals(2, m.formIndex());
        assertEquals(-1, FormHandlers.formOnEnteringBattle(make("PIKACHU"), false));
    }
    @Test
    @DisplayName("setAbility(1) on an original form reads the form's table (the trade script of CERULEDGE_1 / HATTERENE_1)")
    void scriptAbilityFollowsTheForm() {
        for (String[] c : new String[][] {{"CERULEDGE", "SEAMLESS"}, {"HATTERENE", "MOERAE"}}) {
            Pokemon p = make(c[0]);
            p.setForm(data(), 1);
            p.setAbilitySlot(1);
            assertEquals(c[1], p.ability, c[0] + "_1 has one ability: slot 1 falls back to slot 0 (197:241)");
        }
    }

    @Test
    @DisplayName("an old save's form Pokemon with the base species' ability is repaired on load")
    void savedBaseAbilityIsRepaired() {
        Pokemon p = make("CERULEDGE");
        p.setForm(data(), 1);
        p.ability = data().species("CERULEDGE").abilities.get(data().species("CERULEDGE").abilities.size - 1);
        p.repairFormAbility();
        assertEquals("SEAMLESS", p.ability);
        Pokemon base = make("CERULEDGE");
        String own = base.ability;
        base.repairFormAbility();
        assertEquals(own, base.ability, "a base-form Pokemon is left alone");
    }
}
