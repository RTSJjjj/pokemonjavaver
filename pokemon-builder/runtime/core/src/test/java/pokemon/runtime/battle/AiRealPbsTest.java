package pokemon.runtime.battle;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.pokemon.PbsData;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The AI hard-codes Essentials function codes, flag letters and internal names; this checks them against the project's real exported
 * PBS ({@code plugin-src/pbs}) so a renumbered move or a renamed ability is caught here instead of silently skewing the scores.
 */
class AiRealPbsTest {

    private static PbsData real() {
        File root = new File("../../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("../plugin-src");
        if (!new File(root, "pbs/moves.json").isFile()) root = new File("plugin-src");
        Assumptions.assumeTrue(new File(root, "pbs/moves.json").isFile(), "plugin-src/pbs not found from " + new File(".").getAbsolutePath());
        return PbsData.parse(root);
    }

    private static void code(PbsData pbs, String move, String function) {
        assertNotNull(pbs.move(move), move + " exists");
        assertEquals(function, pbs.move(move).function, move);
    }

    @Test
    @DisplayName("function codes the scorer relies on belong to the moves it expects")
    void functionCodes() {
        PbsData p = real();
        String[][] table = {
            {"PROTECT", "0AA"}, {"DETECT", "0AA"}, {"KINGSSHIELD", "14B"}, {"SPIKYSHIELD", "14C"}, {"BANEFULBUNKER", "168"}, {"ENDURE", "0E8"},
            {"REFLECT", "0A2"}, {"LIGHTSCREEN", "0A3"}, {"AURORAVEIL", "167"}, {"SUBSTITUTE", "10C"}, {"EXPLOSION", "0E0"},
            {"UTURN", "0EE"}, {"VOLTSWITCH", "0EE"}, {"PARTINGSHOT", "151"}, {"BATONPASS", "0ED"}, {"KNOCKOFF", "0F0"},
            {"SPLASH", "001"}, {"SPORE", "003"}, {"YAWN", "004"}, {"TOXIC", "006"}, {"THUNDERWAVE", "007"}, {"WILLOWISP", "00A"},
            {"CONFUSERAY", "013"}, {"SWAGGER", "041"}, {"FLATTER", "040"}, {"ATTRACT", "016"}, {"SAFEGUARD", "01A"},
            {"SWORDSDANCE", "02E"}, {"DRAGONDANCE", "026"}, {"CALMMIND", "02C"}, {"BELLYDRUM", "03A"}, {"AGILITY", "030"},
            {"GROWL", "042"}, {"CHARM", "04B"}, {"HAZE", "051"}, {"ROAR", "0EB"}, {"DRAGONTAIL", "0EC"},
            {"MIMIC", "05C"}, {"SKETCH", "05D"}, {"DISABLE", "0B9"}, {"ENCORE", "0BC"}, {"SPITE", "10E"}, {"COPYCAT", "0AF"},
            {"COUNTER", "071"}, {"MIRRORCOAT", "072"}, {"METALBURST", "073"}, {"SLEEPTALK", "0B4"}, {"SNORE", "011"}, {"REST", "0D9"},
            {"RECOVER", "0D5"}, {"MORNINGSUN", "0D8"}, {"WISH", "0D7"}, {"SWALLOW", "114"}, {"LEECHSEED", "0DC"}, {"DESTINYBOND", "0E7"},
            {"SPIKES", "103"}, {"TOXICSPIKES", "104"}, {"STEALTHROCK", "105"}, {"STICKYWEB", "153"}, {"RAPIDSPIN", "110"}, {"DEFOG", "049"},
            {"SUCKERPUNCH", "116"}, {"PURSUIT", "088"}, {"FAKEOUT", "012"}, {"HEALBELL", "019"}, {"TRICK", "0F2"}, {"MAGICCOAT", "0B1"},
            {"POWDER", "148"}, {"IONDELUGE", "146"}, {"TAUNT", "0BA"}, {"TORMENT", "0B7"}, {"PERISHSONG", "0E5"}, {"HEALBLOCK", "0BB"},
        };
        for (String[] row : table) code(p, row[0], row[1]);
    }

    @Test
    @DisplayName("flag letters: contact a, protect b, magic coat c, thaw g, sound k, powder l, bomb n")
    void flagLetters() {
        PbsData p = real();
        assertTrue(p.move("TACKLE").flags.contains("a"));
        assertTrue(p.move("PROTECT").flags.indexOf('b') < 0);
        assertTrue(p.move("SPORE").flags.contains("c"));
        assertTrue(p.move("FLAMEWHEEL").flags.contains("g"));
        assertTrue(p.move("SCREECH").flags.contains("k"));
        assertTrue(p.move("SLEEPPOWDER").flags.contains("l"));
        assertTrue(p.move("SHADOWBALL").flags.contains("n"));
    }

    @Test
    @DisplayName("the ability and item names the scorer checks exist")
    void names() {
        PbsData p = real();
        for (String a : new String[] {"MULTISCALE", "SERENEGRACE", "SHEERFORCE", "STURDY", "MAGICGUARD", "POISONHEAL", "NATURALCURE", "REGENERATOR",
                "VOLTABSORB", "WATERABSORB", "FLASHFIRE", "SAPSIPPER", "DRYSKIN", "STORMDRAIN", "LIGHTNINGROD", "MOTORDRIVE", "UNAWARE", "CONTRARY",
                "MOLDBREAKER", "SPEEDBOOST", "MOODY", "GUTS", "QUICKFEET", "TRUANT", "ASONEGHOST", "ASONEICE", "GRIMNEIGH", "CHILLINGNEIGH"}) {
            assertTrue(p.abilities.containsKey(a), a);
        }
        for (String i : new String[] {"FOCUSSASH", "LEFTOVERS", "BLACKSLUDGE", "HEAVYDUTYBOOTS", "FULLRESTORE", "MAXPOTION", "BURNHEAL", "HYPERPOTION",
                "XATTACK", "TOXICORB", "FLAMEORB", "MENTALHERB", "CUSTAPBERRY", "IRONBALL", "STICKYBARB", "LAGGINGTAIL"}) {
            assertNotNull(p.item(i), i);
        }
    }

    @Test
    @DisplayName("a real trainer's Full Restore is used under a quarter HP and consumed; a vitamin is never used")
    void realTrainerItems() {
        PbsData p = real();
        Battle battle = new Battle(p, new Random(5), (user, target, moves) -> 0);
        battle.trainerBattle = true;
        battle.addPlayer(new pokemon.runtime.pokemon.Pokemon(p.species("BULBASAUR"), 50, p));
        battle.addFoe(new pokemon.runtime.pokemon.Pokemon(p.species("CHARMANDER"), 50, p));
        Battler foe = battle.foe();
        battle.foeItems.add("PROTEIN");
        battle.foeItems.add("FULLRESTORE");
        foe.setHp(1);
        String used = null;
        for (int seed = 0; seed < 40 && used == null; seed++) used = AiItems.choose(battle, foe, new Random(seed));
        assertEquals("FULLRESTORE", used);
        AiItems.register(battle, foe, used);
        AiItems.use(battle, foe, used);
        assertEquals(foe.maxHp(), foe.hp);
        assertEquals(java.util.List.of("PROTEIN"), battle.foeItems);
    }
}
