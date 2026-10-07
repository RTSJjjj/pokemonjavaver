package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Mega Evolution system: Pokemon_MegaEvolution (forms, Rayquaza's MegaMove, Slowbro's specific form),
 * Battle_Action_Other:65-127 (ring, can/register), the "Mega evolution" section's pbMegaEvolve,
 * Battle_Phase_Attack:95-103 and the ZA模式 energy rules.
 */
class MegaEvolutionTest {

    private static Battle battle(PbsData data, Pokemon hero, Pokemon foe) {
        Battle battle = new Battle(data, new Random(4), (user, target, moves) -> 0)
                .addPlayer(hero).addFoe(foe);
        battle.bagHasItem = id -> "MEGARING".equals(id);
        battle.playerName = "小智";
        return battle;
    }

    private static boolean said(Battle b, String part) {
        for (String m : b.roundMessages) if (m.contains(part)) return true;
        return false;
    }

    @Test
    @DisplayName("a held Mega Stone unlocks the Mega form and its stats (getMegaForm :6-33)")
    void megaForm(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon mon = new Pokemon(data.species("MEGAMON"), 50, data);
        assertFalse(mon.hasMegaForm(data), "no stone yet");
        mon.item = "MEGASTONE";
        assertTrue(mon.hasMegaForm(data));
        assertEquals(1, mon.getMegaForm(data, false));
        int before = mon.attack();
        mon.makeMega(data);
        assertTrue(mon.isMega());
        assertEquals("超级变形兽", mon.megaName());
        assertTrue(mon.attack() > before, "the Mega form's Attack is higher");
        mon.makeUnmega(data);
        assertFalse(mon.isMega());
        assertEquals(before, mon.attack());
    }

    @Test
    @DisplayName("Rayquaza Mega Evolves by knowing Dragon Ascent, not by an item (:24-29), with its own message (:73-75)")
    void rayquaza(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon ray = new Pokemon(data.species("RAYQUAZA"), 50, data);
        assertFalse(ray.hasMegaForm(data));
        ray.moves.add(new Pokemon.MoveSlot(data.move("DRAGONASCENT")));
        assertTrue(ray.hasMegaForm(data));
        assertEquals(1, ray.getMegaForm(data, false));
        assertEquals(0, ray.getMegaForm(data, true), "checkItemOnly ignores the move (:24)");
        assertEquals(1, ray.megaMessage(data));
    }

    @Test
    @DisplayName("Slowbro's specific Mega form (:154-163)")
    void slowbro(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon slow = new Pokemon(data.species("SLOWBRO"), 50, data);
        slow.item = "SLOWBRONITE";
        assertEquals(2, slow.getMegaForm(data, false));
        slow.makeMega(data);
        assertEquals(2, slow.form.form);
        assertEquals(0, slow.getUnmegaForm(data));
        slow.makeUnmega(data);
        assertNull(slow.form);
    }

    @Test
    @DisplayName("Primal Reversion needs the orb (:85-106)")
    void primal(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon kyogre = new Pokemon(data.species("KYOGRE"), 50, data);
        assertFalse(kyogre.hasPrimalForm());
        kyogre.item = "BLUEORB";
        assertTrue(kyogre.hasPrimalForm());
        kyogre.makePrimal(data);
        assertTrue(kyogre.isPrimal());
        kyogre.item = null;
        assertFalse(kyogre.isPrimal(), "primal? is false without the orb");
    }

    @Test
    @DisplayName("classic: registering is not evolving; the attack phase does it before the moves (:95-103)")
    void classicRegisterThenAttackPhase(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("MEGAMON"), 50, data);
        hero.item = "MEGASTONE";
        Battle b = battle(data, hero, new Pokemon(data.species("TANK"), 100, data));
        assertTrue(b.pbCanMegaEvolve(0));
        assertFalse(b.pbRegisteredMegaEvolution(0));
        b.pbToggleRegisteredMegaEvolution(0);
        assertTrue(b.pbRegisteredMegaEvolution(0));
        assertFalse(b.pbCanMegaEvolve(0), "a registered Mega leaves the slot taken (:98)");
        assertFalse(hero.isMega(), "nothing happens until the attack phase");
        b.step();
        assertTrue(hero.isMega());
        assertEquals(-2, b.megaEvolution[0][0], "used up (:443)");
        assertFalse(b.pbCanMegaEvolve(0));
        assertTrue(said(b, "发生了反应"), b.roundMessages.toString());
        assertTrue(said(b, "超级进化为"), b.roundMessages.toString());
        boolean scene = false;
        for (Battle.RoundEvent e : b.roundEvents) if (e.kind == Battle.RoundEvent.Kind.MEGA_SCENE) scene = true;
        assertTrue(scene, "the full Mega scene was requested");
    }

    @Test
    @DisplayName("the compact animation uses the two common animations around the form change (:420-431)")
    void compactAnimation(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("MEGAMON"), 50, data);
        hero.item = "MEGASTONE";
        Battle b = battle(data, hero, new Pokemon(data.species("TANK"), 100, data));
        b.fullMegaAnimation = false;
        b.pbRegisterMegaEvolution(0);
        b.step();
        StringBuilder kinds = new StringBuilder();
        for (Battle.RoundEvent e : b.roundEvents) kinds.append(e).append(' ');
        String all = kinds.toString();
        int first = all.indexOf("common:MegaEvolution/");
        int change = all.indexOf("CHANGE_POKEMON:0");
        int second = all.indexOf("common:MegaEvolution2/");
        assertTrue(first >= 0 && change > first && second > change, all);
    }

    @Test
    @DisplayName("no Mega Ring in the bag, no Mega Evolution (:65-71)")
    void needsARing(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("MEGAMON"), 50, data);
        hero.item = "MEGASTONE";
        Battle b = battle(data, hero, new Pokemon(data.species("TANK"), 100, data));
        b.bagHasItem = id -> false;
        assertFalse(b.pbCanMegaEvolve(0));
    }

    @Test
    @DisplayName("Rayquaza's message when a trainer Mega Evolves it (:388-390)")
    void rayquazaMessage(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon ray = new Pokemon(data.species("RAYQUAZA"), 50, data);
        ray.moves.add(new Pokemon.MoveSlot(data.move("DRAGONASCENT")));
        Battle b = battle(data, ray, new Pokemon(data.species("TANK"), 100, data));
        b.playerName = "小智";
        b.pbMegaEvolve(0);
        assertTrue(said(b, "小智衷心的祈愿传达给"), b.roundMessages.toString());
        assertTrue(ray.isMega());
    }

    @Test
    @DisplayName("ZA mode: the Mega lasts three rounds of energy, the energy refills afterwards (ZA:234-271)")
    void zaMode(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("MEGAMON"), 50, data);
        hero.item = "MEGASTONE";
        Battle b = battle(data, hero, new Pokemon(data.species("TANK"), 100, data));
        b.zaMode = true;
        assertTrue(b.pbRegisterMegaEvolution(0));
        assertTrue(b.pbRegisteredMegaEvolution(0));
        b.step();
        assertTrue(hero.isMega());
        assertTrue(b.zaMegaActive[0]);
        assertTrue(said(b, "的超级进化开始了！"));
        assertEquals(2, b.zaEnergy[0]);
        b.step();
        assertEquals(1, b.zaEnergy[0]);
        b.step();
        assertEquals(0, b.zaEnergy[0]);
        assertFalse(hero.isMega(), "energy ran out (ZA:257-259)");
        assertFalse(b.zaMegaActive[0]);
        b.step();
        assertEquals(1, b.zaEnergy[0], "energy comes back one per round (ZA:264-266)");
    }

    @Test
    @DisplayName("ZA mode: only one request per side at a time (ZA:48-53)")
    void zaOneRequestPerSide(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("MEGAMON"), 50, data);
        hero.item = "MEGASTONE";
        Battle b = battle(data, hero, new Pokemon(data.species("TANK"), 100, data));
        b.zaMode = true;
        b.zaMegaRequests[0].add(7);                 // another battler of this side asked first
        assertFalse(b.pbCanMegaEvolve(0));
    }

    @Test
    @DisplayName("ZA mode: the Mega is reverted when the battle ends (ZA:207-222; classic mode reverts in pbAfterBattle:622)")
    void megaRevertsAfterBattle(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Pokemon hero = new Pokemon(data.species("MEGAMON"), 50, data);
        hero.item = "MEGASTONE";
        Pokemon foe = new Pokemon(data.species("WEAK"), 2, data);
        Battle b = battle(data, hero, foe);
        b.zaMode = true;
        b.pbMegaEvolve(0);
        assertTrue(hero.isMega());
        b.run(20);
        assertFalse(hero.isMega(), "reverted when the battle ended");
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    private static String species(int id, String internal, String name, String types, String stats, String moves) {
        return "\"" + internal + "\":{\"id\":" + id + ",\"internalName\":\"" + internal + "\",\"name\":\"" + name + "\","
                + "\"types\":[" + types + "],\"baseStats\":[" + stats + "],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[" + moves + "]}";
    }

    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        String slash = "{\"level\":1,\"move\":\"SLASH\"}";
        write(root, "pokemon.json", "{\"total\":7,\"species\":{"
                + species(1, "MEGAMON", "变形兽", "\"NORMAL\"", "70,70,70,70,70,70", slash) + ","
                + species(2, "WEAK", "Weak", "\"NORMAL\"", "1,1,1,1,1,1", slash) + ","
                + species(3, "TANK", "Tank", "\"NORMAL\"", "255,5,255,5,5,5", slash) + ","
                + species(4, "RAYQUAZA", "烈空坐", "\"NORMAL\"", "105,150,90,95,150,90", slash) + ","
                + species(5, "SLOWBRO", "呆壳兽", "\"NORMAL\"", "95,75,110,30,100,80", slash) + ","
                + species(6, "KYOGRE", "盖欧卡", "\"NORMAL\"", "100,100,90,90,150,140", slash) + "}}");
        write(root, "moves.json", "{\"total\":2,\"moves\":{"
                + "\"SLASH\":{\"id\":1,\"internalName\":\"SLASH\",\"name\":\"Slash\","
                + "\"function\":\"000\",\"power\":70,\"type\":\"NORMAL\",\"category\":\"Physical\",\"accuracy\":100,\"pp\":20,\"target\":\"NearOther\"},"
                + "\"DRAGONASCENT\":{\"id\":2,\"internalName\":\"DRAGONASCENT\",\"name\":\"Dragon Ascent\","
                + "\"function\":\"000\",\"power\":120,\"type\":\"NORMAL\",\"category\":\"Physical\",\"accuracy\":100,\"pp\":5,\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(root, "types.json", "{\"total\":1,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":3,\"items\":{"
                + "\"MEGASTONE\":{\"id\":1,\"internalName\":\"MEGASTONE\",\"name\":\"Mega Stone\"},"
                + "\"MEGARING\":{\"id\":2,\"internalName\":\"MEGARING\",\"name\":\"超级环\"},"
                + "\"SLOWBRONITE\":{\"id\":3,\"internalName\":\"SLOWBRONITE\",\"name\":\"呆壳兽进化石\"}}}");
        write(root, "pokemonforms.json", "{\"total\":4,\"forms\":{"
                + "\"MEGAMON_1\":{\"species\":\"MEGAMON\",\"form\":1,\"key\":\"MEGAMON_1\","
                + "\"formName\":\"超级变形兽\",\"baseStats\":[70,120,90,120,90,100],"
                + "\"megaStone\":\"MEGASTONE\",\"unmegaForm\":0},"
                + "\"RAYQUAZA_1\":{\"species\":\"RAYQUAZA\",\"form\":1,\"key\":\"RAYQUAZA_1\","
                + "\"formName\":\"超级烈空坐\",\"baseStats\":[105,180,100,115,180,100],"
                + "\"megaMove\":\"DRAGONASCENT\",\"megaMessage\":1,\"unmegaForm\":0},"
                + "\"SLOWBRO_2\":{\"species\":\"SLOWBRO\",\"form\":2,\"key\":\"SLOWBRO_2\","
                + "\"formName\":\"超级呆壳兽\",\"baseStats\":[95,75,180,30,130,80],"
                + "\"megaStone\":\"SLOWBRONITE\",\"unmegaForm\":0},"
                + "\"KYOGRE_1\":{\"species\":\"KYOGRE\",\"form\":1,\"key\":\"KYOGRE_1\","
                + "\"formName\":\"原始盖欧卡\",\"baseStats\":[100,150,90,90,180,160],\"unmegaForm\":0}}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }
}
