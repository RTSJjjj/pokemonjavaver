package pokemon.runtime.field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.state.GameState;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 189_PItem_ItemEffects UseOnPokemon handlers and the 188_PItem_Items helpers they call. */
class ItemHandlersTest {

    /** Records what the handlers say and answers their questions from a queue. */
    private static final class FakeScene implements ItemScene {
        final List<String> log = new ArrayList<>();
        final ArrayDeque<Object> answers = new ArrayDeque<>();

        private Object next(Object fallback) {
            return answers.isEmpty() ? fallback : answers.poll();
        }

        @Override public void pbStartScene(String helpText, String[] annotations) {
            log.add("scene:" + helpText + (annotations == null ? "" : java.util.Arrays.toString(annotations)));
        }
        @Override public void pbSetHelpText(String text) { }
        @Override public void pbClearAnnotations() { }
        @Override public void pbRefreshAnnotations(java.util.function.Predicate<Pokemon> able) { log.add("annot"); }
        @Override public void pbDisplay(String text) { log.add(text); }
        @Override public boolean pbConfirm(String text) { log.add("?" + text); return (Boolean) next(Boolean.TRUE); }
        @Override public void pbRefresh() { }
        @Override public void pbHardRefresh() { }
        @Override public int pbChooseMove(Pokemon pkmn, String text) { log.add("?" + text); return (Integer) next(-1); }
        @Override public int pbChoosePokemon(String text) { log.add("?" + text); return (Integer) next(-1); }
        @Override public void pbMessage(String text) { log.add(text); }
        @Override public int pbMessage(String text, List<String> commands, int cmdIfCancel) {
            log.add("?" + text + commands);
            return (Integer) next(cmdIfCancel);
        }
        @Override public int pbShowCommands(String text, List<String> commands, int index) {
            log.add("?" + text);
            return (Integer) next(-1);
        }
        @Override public int pbMessageChooseNumber(String text, int max, int defaultValue, int cancelValue) {
            log.add("?" + text + "<=" + max);
            return (Integer) next(cancelValue);
        }
        @Override public void pbTopRightWindow(String text) { log.add("[" + text + "]"); }
        @Override public int pbForgetMove(Pokemon pkmn, PbsData.Move moveToLearn) { return (Integer) next(-1); }
        @Override public void pbSEPlay(String name) { log.add("se:" + name); }
        @Override public void pbEvolution(Pokemon pkmn, PbsData.Species species, String item) { log.add("evolve:" + species.internalName); }
    }

    @TempDir
    Path tempDir;

    private PbsData pbs;
    private GameState state;
    private ItemHandlers handlers;
    private FakeScene scene;
    private Pokemon pkmn;

    private void write(String name, String json) throws Exception {
        Path dir = tempDir.resolve("pbs");
        Files.createDirectories(dir);
        Files.write(dir.resolve(name), json.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        write("index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write("pokemon.json", "{\"total\":3,\"species\":{"
                + "\"PLAIN\":{\"id\":1,\"internalName\":\"PLAIN\",\"name\":\"Plain\",\"types\":[\"NORMAL\"],"
                + "\"baseStats\":[50,50,50,50,50,50],\"growthRate\":\"Medium\",\"baseExp\":60,\"happiness\":70,"
                + "\"abilities\":[\"ONE\",\"TWO\"],\"hiddenAbility\":\"HID\","
                + "\"moves\":[{\"level\":1,\"move\":\"TACKLE\"},{\"level\":5,\"move\":\"GROWL\"},{\"level\":6,\"move\":\"SCRATCH\"}],"
                + "\"evolutions\":[{\"species\":\"EVOLVED\",\"method\":\"Item\",\"parameter\":\"FIRESTONE\"}]},"
                + "\"EVOLVED\":{\"id\":2,\"internalName\":\"EVOLVED\",\"name\":\"Evolved\",\"types\":[\"FIRE\"],"
                + "\"baseStats\":[70,70,70,70,70,70],\"growthRate\":\"Medium\",\"abilities\":[\"ONE\"]},"
                + "\"KYUREM\":{\"id\":3,\"internalName\":\"KYUREM\",\"name\":\"Kyurem\",\"types\":[\"ICE\"],"
                + "\"baseStats\":[100,100,100,100,100,100],\"growthRate\":\"Slow\",\"abilities\":[\"ONE\"]},"
                + "\"RESHIRAM\":{\"id\":4,\"internalName\":\"RESHIRAM\",\"name\":\"Reshiram\",\"types\":[\"FIRE\"],"
                + "\"baseStats\":[100,100,100,100,100,100],\"growthRate\":\"Slow\",\"abilities\":[\"ONE\"]}}}");
        write("pokemonforms.json", "{\"total\":1,\"forms\":{\"KYUREM_1\":{\"species\":\"KYUREM\",\"form\":1,\"key\":\"KYUREM_1\"}}}");
        write("moves.json", "{\"total\":4,\"moves\":{"
                + "\"TACKLE\":{\"id\":1,\"internalName\":\"TACKLE\",\"name\":\"Tackle\",\"power\":40,\"type\":\"NORMAL\",\"pp\":35},"
                + "\"GROWL\":{\"id\":2,\"internalName\":\"GROWL\",\"name\":\"Growl\",\"power\":0,\"type\":\"NORMAL\",\"pp\":40},"
                + "\"SCRATCH\":{\"id\":3,\"internalName\":\"SCRATCH\",\"name\":\"Scratch\",\"power\":40,\"type\":\"NORMAL\",\"pp\":35},"
                + "\"SPLASH\":{\"id\":4,\"internalName\":\"SPLASH\",\"name\":\"Splash\",\"power\":0,\"type\":\"NORMAL\",\"pp\":1}}}");
        write("items.json", "{\"total\":6,\"items\":{"
                + "\"POTION\":{\"id\":1,\"internalName\":\"POTION\",\"name\":\"Potion\",\"fieldUse\":1},"
                + "\"RARECANDY\":{\"id\":2,\"internalName\":\"RARECANDY\",\"name\":\"Rare Candy\",\"fieldUse\":1},"
                + "\"FIRESTONE\":{\"id\":3,\"internalName\":\"FIRESTONE\",\"name\":\"Fire Stone\",\"fieldUse\":1,\"type\":7},"
                + "\"LONELYMINT\":{\"id\":4,\"internalName\":\"LONELYMINT\",\"name\":\"Lonely Mint\",\"fieldUse\":1},"
                + "\"DNASPLICERS\":{\"id\":5,\"internalName\":\"DNASPLICERS\",\"name\":\"DNA Splicers\",\"fieldUse\":1},"
                + "\"EXPCANDYM\":{\"id\":6,\"internalName\":\"EXPCANDYM\",\"name\":\"Exp. Candy M\",\"fieldUse\":1},"
                + "\"TM01\":{\"id\":7,\"internalName\":\"TM01\",\"name\":\"TM01\",\"fieldUse\":3,\"machine\":\"SPLASH\"}}}");
        write("tm.json", "{\"compatibility\":{\"SPLASH\":[\"PLAIN\"]}}");
        write("abilities.json", "{\"total\":3,\"abilities\":{"
                + "\"ONE\":{\"id\":1,\"internalName\":\"ONE\",\"name\":\"AbilityOne\"},"
                + "\"TWO\":{\"id\":2,\"internalName\":\"TWO\",\"name\":\"AbilityTwo\"},"
                + "\"HID\":{\"id\":3,\"internalName\":\"HID\",\"name\":\"HiddenAbility\"}}}");
        write("types.json", "{\"total\":1,\"types\":{\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\"}}}");
        write("natures.json", "{\"total\":4,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"},"
                + "{\"id\":1,\"internalName\":\"LONELY\",\"name\":\"Lonely\",\"statUp\":\"ATTACK\",\"statDown\":\"DEFENSE\"},"
                + "{\"id\":12,\"internalName\":\"SERIOUS\",\"name\":\"Serious\"},"
                + "{\"id\":3,\"internalName\":\"ADAMANT\",\"name\":\"Adamant\",\"statUp\":\"ATTACK\",\"statDown\":\"SPATK\"}]}");
        pbs = PbsData.parse(tempDir.toFile());
        state = new GameState();
        handlers = new ItemHandlers(pbs, state, () -> LocalTime.of(12, 0));
        scene = new FakeScene();
        pkmn = add("PLAIN", 30);
    }

    private Pokemon add(String species, int level) {
        Pokemon p = new Pokemon(pbs.species(species), level, pbs);
        p.nature = pbs.nature("HARDY");
        p.ability = "ONE";
        p.hp = p.maxHp();
        p.happiness = 100;
        p.ballused = 99;                       // not the Luxury Ball
        p.obtainMap = 77;                      // not met on the current map
        state.trainer().party.add(p);
        return p;
    }

    private boolean use(String item) {
        return handlers.useOnPokemon(item, pkmn, scene);
    }

    @Test
    @DisplayName("a Potion restores 20 HP; at full HP or fainted it does nothing (pbHPItem :575-584)")
    void potion() {
        pkmn.hp = pkmn.maxHp() - 50;
        assertTrue(use("POTION"));
        assertEquals(pkmn.maxHp() - 30, pkmn.hp);
        assertEquals("Plain的HP恢复了20点。", scene.log.get(0));
        pkmn.hp = pkmn.maxHp();
        assertFalse(use("POTION"));
        assertEquals("这没有任何效果…", scene.log.get(1));
        pkmn.hp = 0;
        assertFalse(use("POTION"));
        assertEquals(0, pkmn.hp);
    }

    @Test
    @DisplayName("Max Potion and the Sitrus Berry restore all / a quarter; the gain is clamped to the missing HP")
    void potionVariants() {
        pkmn.hp = 1;
        assertTrue(use("MAXPOTION"));
        assertEquals(pkmn.maxHp(), pkmn.hp);
        pkmn.hp = 1;
        assertTrue(use("SITRUSBERRY"));
        assertEquals(1 + pkmn.maxHp() / 4, pkmn.hp);
        pkmn.hp = pkmn.maxHp() - 3;
        assertTrue(use("HYPERPOTION"));
        assertEquals("Plain的HP恢复了3点。", scene.log.get(scene.log.size() - 1));
    }

    @Test
    @DisplayName("the status cures need their status on an able Pokemon and use the plugin's texts (:484-575)")
    void statusCures() {
        pkmn.status = "POISON";
        assertFalse(use("AWAKENING"));
        assertEquals("POISON", pkmn.status);
        assertTrue(use("ANTIDOTE"));
        assertEquals("", pkmn.status);
        assertEquals("Plain的毒被消去了！", scene.log.get(scene.log.size() - 1));
        pkmn.status = "FROZEN";
        assertTrue(use("ICEHEAL"));
        assertEquals("Plain不再被冰冻了", scene.log.get(scene.log.size() - 1));
        pkmn.status = "BURN";
        pkmn.hp = 0;
        assertFalse(use("BURNHEAL"), "a fainted Pokemon is not cured");
        pkmn.hp = 5;
        assertTrue(use("LUMBERRY"));
        assertEquals("Plain恢复健康了。", scene.log.get(scene.log.size() - 1));
    }

    @Test
    @DisplayName("Full Restore heals HP and status; it says 'healthy' when only the status needed it (:577-591)")
    void fullRestore() {
        pkmn.hp = pkmn.maxHp();
        pkmn.status = "SLEEP";
        assertTrue(use("FULLRESTORE"));
        assertEquals("Plain恢复健康了。", scene.log.get(0));
        pkmn.hp = 10;
        pkmn.status = "BURN";
        assertTrue(use("FULLRESTORE"));
        assertEquals(pkmn.maxHp(), pkmn.hp);
        assertEquals("", pkmn.status);
        assertFalse(use("FULLRESTORE"), "nothing to heal");
    }

    @Test
    @DisplayName("Revive gives half HP (at least 1), Max Revive all; neither works on a standing Pokemon (:593-616)")
    void revive() {
        assertFalse(use("REVIVE"));
        pkmn.hp = 0;
        pkmn.status = "BURN";
        assertTrue(use("REVIVE"));
        assertEquals(pkmn.maxHp() / 2, pkmn.hp);
        assertEquals("", pkmn.status);
        assertEquals("Plain的HP回复了。", scene.log.get(scene.log.size() - 1));
        pkmn.hp = 0;
        assertTrue(use("MAXREVIVE"));
        assertEquals(pkmn.maxHp(), pkmn.hp);
    }

    @Test
    @DisplayName("the herbal medicines lower happiness (:630-669)")
    void herbal() {
        pkmn.hp = 10;
        assertTrue(use("ENERGYPOWDER"));
        assertEquals(95, pkmn.happiness, "powder: -5 below 200");
        pkmn.hp = 10;
        assertTrue(use("ENERGYROOT"));
        assertEquals(85, pkmn.happiness);
        pkmn.hp = 0;
        assertTrue(use("REVIVALHERB"));
        assertEquals(70, pkmn.happiness);
        assertEquals(pkmn.maxHp(), pkmn.hp);
    }

    @Test
    @DisplayName("Ether restores 10 PP of the chosen move, Max Elixir all of them (:671-719)")
    void pp() {
        pkmn.moves.get(0).pp = 5;
        scene.answers.add(0);
        assertTrue(use("ETHER"));
        assertEquals(15, pkmn.moves.get(0).pp);
        assertEquals("PP恢复了。", scene.log.get(scene.log.size() - 1));
        pkmn.moves.get(0).pp = 35;
        scene.answers.add(0);
        assertFalse(use("ETHER"), "already full");
        scene.answers.add(-1);
        assertFalse(use("ETHER"), "cancelled");
        pkmn.moves.get(0).pp = 1;
        pkmn.moves.get(1).pp = 0;
        assertTrue(use("MAXELIXIR"));
        assertEquals(35, pkmn.moves.get(0).pp);
        assertEquals(40, pkmn.moves.get(1).pp);
    }

    @Test
    @DisplayName("PP Up adds a fifth of the move's PP, up to 3 times; PP Max sets 3 at once (:721-749)")
    void ppUp() {
        scene.answers.add(0);
        assertTrue(use("PPUP"));
        assertEquals(1, pkmn.moves.get(0).ppUp);
        assertEquals(42, pkmn.moves.get(0).totalPp());
        assertEquals(42, pkmn.moves.get(0).maxPp);
        assertEquals("Tackle的PP增加了。", scene.log.get(scene.log.size() - 1));
        scene.answers.add(0);
        assertTrue(use("PPMAX"));
        assertEquals(3, pkmn.moves.get(0).ppUp);
        assertEquals(56, pkmn.moves.get(0).maxPp);
        scene.answers.add(0);
        assertFalse(use("PPUP"), "already 3 PP Ups");
    }

    @Test
    @DisplayName("vitamins add 10 EVs up to 100 and recalculate the stats; the HP gap is kept (:751-760, pbRaiseEffortValues)")
    void vitamins() {
        pkmn.evs[PokemonStats.HP] = 90;
        int max = pkmn.maxHp();
        pkmn.hp = max - 10;
        assertTrue(use("HPUP"));
        assertEquals(100, pkmn.evs[PokemonStats.HP]);
        assertTrue(pkmn.maxHp() > max, "100 EVs add HP");
        assertEquals(pkmn.maxHp() - 10, pkmn.hp, "the missing HP stays 10");
        assertFalse(use("HPUP"), "vitamins stop at 100 EVs");
        assertEquals("Plain的HP增加了。", scene.log.get(0));
        assertTrue(use("HEALTHWING"), "wings have no 100 limit");
        assertEquals(101, pkmn.evs[PokemonStats.HP]);
    }

    @Test
    @DisplayName("the EV total stops at 510 (:647-649)")
    void evTotal() {
        pkmn.evs = new int[] {100, 100, 100, 100, 100, 5};
        assertFalse(use("PROTEIN"), "100 attack EVs: no effect");
        pkmn.evs = new int[] {0, 90, 100, 100, 100, 115};
        assertTrue(use("PROTEIN"));
        assertEquals(95, pkmn.evs[PokemonStats.ATTACK], "only 5 of the 10 fit under 510");
    }

    @Test
    @DisplayName("EV berries lower 10 EVs and raise happiness, with the message of the case (:663-681)")
    void evBerries() {
        pkmn.evs[PokemonStats.ATTACK] = 4;
        pkmn.happiness = 100;
        assertTrue(use("KELPSYBERRY"));
        assertEquals(0, pkmn.evs[PokemonStats.ATTACK]);
        assertEquals(105, pkmn.happiness, "evberry: +5 at 100..199");
        assertEquals("Plain更加喜欢你了。\n但是攻击降低了。", scene.log.get(0), "happiness and EVs both changed: the third text");
        pkmn.happiness = 255;
        assertFalse(use("KELPSYBERRY"));
        pkmn.evs[PokemonStats.HP] = 20;
        assertTrue(use("POMEGBERRY"));
        assertEquals("Plain十分喜欢你！\n基础HP降低了。", scene.log.get(scene.log.size() - 1), "only the EVs changed: the first text");
        pkmn.happiness = 100;
        pkmn.evs[PokemonStats.HP] = 0;
        assertTrue(use("POMEGBERRY"));
        assertEquals("Plain更加喜欢你了。\n基础HP不能再降低了。", scene.log.get(scene.log.size() - 1), "only the happiness changed: the second text");
        pkmn.evs[PokemonStats.SPEED] = 3;
        assertTrue(use("HOPOBERRY"));
        assertArrayEquals(new int[6], pkmn.evs);
        assertFalse(use("HOPOBERRY"));
    }

    @Test
    @DisplayName("Rare Candy raises the level by the number chosen, learns the new moves and keeps the missing HP (:874-925)")
    void rareCandy() {
        state.inventory().add("RARECANDY", 5);
        pkmn.level = 4;
        pkmn.exp = 64;
        pkmn.moves.clear();
        pkmn.moves.add(new Pokemon.MoveSlot(pbs.move("TACKLE")));
        pkmn.hp = pkmn.maxHp() - 3;
        scene.answers.add(2);                                    // how many: 2 -> level 6, learning GROWL and SCRATCH
        scene.answers.add(Boolean.TRUE);                         // learn them now
        assertTrue(use("RARECANDY"));
        assertEquals(6, pkmn.level);
        assertEquals(216, pkmn.exp);
        assertEquals(pkmn.maxHp() - 3, pkmn.hp);
        assertEquals(4, state.inventory().count("RARECANDY"), "the handler consumes qty-1; the bag loop the last one");
        assertEquals(3, pkmn.moves.size);
        assertEquals("Plain升到了6级！", scene.log.stream().filter(s -> s.contains("升到了")).findFirst().orElse(""));
        assertTrue(scene.log.stream().anyMatch(s -> s.startsWith("[最大HP<r>+")));
    }

    @Test
    @DisplayName("with the level lock on, a candy refuses above the cap of the next badge (:880-910)")
    void rareCandyLock() {
        state.switches().set(199, true);
        pkmn.level = 25;
        pkmn.exp = 15625;
        assertFalse(use("RARECANDY"));
        assertEquals("直到解锁16级限制前无法使用。", scene.log.get(0));
        state.trainer().badges.add(0);
        assertFalse(use("RARECANDY"), "badge 1 is held but the cap of badge 2 (16) is lower: the plugin's table order");
        pkmn.level = 1;
        pkmn.exp = 0;
        state.switches().set(199, false);
        state.inventory().add("RARECANDY", 1);
        assertTrue(use("RARECANDY"));
        assertEquals(2, pkmn.level);
    }

    @Test
    @DisplayName("Exp. Candy M gives 3000 exp and levels the Pokemon (:1209-1289)")
    void expCandy() {
        state.inventory().add("EXPCANDYM", 1);
        pkmn.level = 10;
        pkmn.exp = 1000;
        assertTrue(use("EXPCANDYM"));
        assertEquals(3375, pkmn.exp, "pbChangeLevel sets the experience to the start of the new level (188:479-480)");
        assertEquals(15, pkmn.level);
        assertEquals("你的宝可梦获得了3000点经验值！", scene.log.get(0));
        assertTrue(scene.log.contains("se:Pkmn level up"));
    }

    @Test
    @DisplayName("a mint sets the nature after asking; the same nature, or a neutral one for Serious, has no effect (:1327-1366)")
    void mint() {
        assertTrue(use("LONELYMINT"));
        assertEquals("LONELY", pkmn.nature.internalName);
        assertEquals("Plain的性格由于Lonely Mint而改变!", scene.log.get(scene.log.size() - 1));
        assertFalse(use("LONELYMINT"));
        scene.answers.add(Boolean.FALSE);
        pkmn.nature = pbs.nature("ADAMANT");
        assertFalse(use("LONELYMINT"));
        assertEquals("ADAMANT", pkmn.nature.internalName, "declined");
    }

    @Test
    @DisplayName("the Ability Capsule swaps the two normal abilities after asking; hidden abilities refuse (:1184-1206)")
    void abilityCapsule() {
        pkmn.ability = "ONE";
        assertTrue(use("ABILITYCAPSULE"));
        assertEquals("TWO", pkmn.ability);
        assertEquals("Plain的特性更改为AbilityTwo！", scene.log.get(scene.log.size() - 1));
        pkmn.ability = "HID";
        assertFalse(use("ABILITYCAPSULE"));
    }

    @Test
    @DisplayName("the Ability Patch moves to the hidden ability, and back with a choice (:1369-1414)")
    void abilityPatch() {
        pkmn.ability = "ONE";
        assertTrue(use("ABILITYPATCH"));
        assertEquals("HID", pkmn.ability);
        scene.answers.add(1);                                    // the list holds ONE and TWO: pick TWO
        assertTrue(use("ABILITYPATCH"));
        assertEquals("TWO", pkmn.ability);
    }

    @Test
    @DisplayName("an evolution stone evolves a matching Pokemon through the scene and refuses the others (:400-424)")
    void evolutionStone() {
        assertTrue(use("FIRESTONE"));
        assertTrue(scene.log.contains("evolve:EVOLVED"));
        Pokemon other = add("KYUREM", 30);
        assertFalse(handlers.useOnPokemon("FIRESTONE", other, scene));
        assertEquals("这没有任何效果…", scene.log.get(scene.log.size() - 1));
        assertNotNull(handlers.annotations("FIRESTONE"));
        assertEquals("可用", handlers.annotations("FIRESTONE")[0]);
        assertEquals("不可用", handlers.annotations("FIRESTONE")[1]);
        assertNull(handlers.annotations("POTION"));
    }

    @Test
    @DisplayName("DNA Splicers fuse a chosen Reshiram into Kyurem and take it out of the party, then separate (:1033-1084)")
    void dnaSplicers() {
        Pokemon kyurem = add("KYUREM", 50);
        Pokemon reshiram = add("RESHIRAM", 50);
        scene.answers.add(2);
        assertTrue(handlers.useOnPokemon("DNASPLICERS", kyurem, scene));
        assertSame(reshiram, kyurem.fused);
        assertEquals(1, kyurem.formIndex());
        assertEquals(2, state.trainer().party.size(), "Reshiram left the party");
        assertTrue(handlers.useOnPokemon("DNASPLICERS", kyurem, scene));
        assertNull(kyurem.fused);
        assertEquals(0, kyurem.formIndex());
        assertEquals(3, state.trainer().party.size());
        assertEquals("Kyurem改变了形态！", scene.log.get(scene.log.size() - 1));
        assertFalse(use("DNASPLICERS"), "only Kyurem");
        assertEquals("无法生效。", scene.log.get(scene.log.size() - 1));
    }

    @Test
    @DisplayName("pbUseItem on the party: choose, use, repeat until cancel; the last item is used up with a message (:867-916)")
    void useItemOnParty() {
        Pokemon second = add("PLAIN", 30);
        pkmn.hp = 10;
        second.hp = 10;
        state.inventory().add("POTION", 2);
        scene.answers.add(0);
        scene.answers.add(1);
        assertEquals(1, handlers.pbUseItemOnParty("POTION", 1, scene));
        assertEquals(30, pkmn.hp);
        assertEquals(30, second.hp);
        assertEquals(0, state.inventory().count("POTION"));
        assertEquals("最后的Potion被使用了。", scene.log.get(scene.log.size() - 1));
        // cancel at once
        state.inventory().add("POTION", 1);
        scene.answers.add(-1);
        assertEquals(0, handlers.pbUseItemOnParty("POTION", 1, scene));
        assertEquals(1, state.inventory().count("POTION"));
    }

    @Test
    @DisplayName("a handler that fails to heal does not use the item up")
    void failedUseKeepsItem() {
        state.inventory().add("POTION", 1);
        scene.answers.add(0);
        scene.answers.add(-1);
        handlers.pbUseItemOnParty("POTION", 1, scene);                // full HP: no effect
        assertEquals(1, state.inventory().count("POTION"));
    }

    @Test
    @DisplayName("a TM teaches its move to a compatible Pokemon through the learn flow and is never used up (:937-958)")
    void machine() {
        assertTrue(handlers.isMachine("TM01"));
        assertEquals("SPLASH", handlers.machineMove("TM01"));
        state.inventory().add("TM01", 1);
        assertTrue(handlers.pbUseItemOnPokemon("TM01", pkmn, scene));
        assertEquals("SPLASH", pkmn.moves.get(pkmn.moves.size - 1).move.internalName);
        assertEquals(1, state.inventory().count("TM01"), "INFINITE_TMS");
        // 4 moves known: the forget screen decides
        pkmn.moves.clear();
        for (String m : new String[] {"TACKLE", "GROWL", "SCRATCH"}) {
            pkmn.moves.add(new Pokemon.MoveSlot(pbs.move(m)));
        }
        pkmn.moves.add(new Pokemon.MoveSlot(pbs.move("TACKLE")));
        scene.answers.add(Boolean.TRUE);                 // teach it
        scene.answers.add(1);                            // forget GROWL
        assertTrue(handlers.pbUseItemOnPokemon("TM01", pkmn, scene));
        assertEquals("SPLASH", pkmn.moves.get(1).move.internalName);
        assertTrue(scene.log.stream().anyMatch(s -> s.startsWith("Plain遗忘了Growl")));
        Pokemon other = add("KYUREM", 30);
        assertFalse(handlers.pbUseItemOnPokemon("TM01", other, scene));
        assertEquals("Kyurem不能学习Splash。", scene.log.get(scene.log.size() - 1));
    }

    @Test
    @DisplayName("the move tutor annotations say 已学会 / 可用 / 不可用 and the tutor picks until a Pokemon learns (:955-1018)")
    void moveTutor() {
        Pokemon kyurem = add("KYUREM", 30);
        PbsData.Move splash = pbs.move("SPLASH");
        assertArrayEquals(new String[] {"可用", "不可用"}, handlers.moveTutorAnnotations(splash, null));
        scene.answers.add(1);                            // Kyurem: cannot learn
        scene.answers.add(0);                            // Plain learns it
        assertEquals(0, handlers.pbMoveTutorChoose(splash, null, true, scene));
        assertTrue(scene.log.contains("Kyurem不能学习Splash。"));
        assertEquals("已学会", handlers.moveTutorAnnotations(splash, null)[0]);
        assertNotNull(kyurem);
    }

    @Test
    @DisplayName("Give opens the party screen and hands the item over; a held item can be swapped (210:1049-1059, 188:1000-1052)")
    void giveItem() {
        state.inventory().add("POTION", 2);
        scene.answers.add(0);
        assertTrue(handlers.pbPokemonGiveScreen("POTION", scene));
        assertEquals("POTION", pkmn.item);
        assertEquals(1, state.inventory().count("POTION"));
        assertTrue(scene.log.contains("Plain现在携带着Potion。"));
        state.inventory().add("RARECANDY", 1);
        scene.answers.add(0);
        assertTrue(handlers.pbPokemonGiveScreen("RARECANDY", scene));
        assertEquals("RARECANDY", pkmn.item);
        assertEquals(2, state.inventory().count("POTION"), "the old item came back");
        scene.answers.add(-1);
        assertFalse(handlers.pbPokemonGiveScreen("POTION", scene));
    }

    @Test
    @DisplayName("the repels set the step counter and refuse while one is active; the infinite repel toggles (:124-160)")
    void repels() {
        assertEquals(3, handlers.useInField("SUPERREPEL", scene));
        assertEquals(200, state.fieldGlobals().repel);
        assertEquals(0, handlers.useInField("REPEL", scene));
        assertEquals("但喷雾剂仍然有效。", scene.log.get(scene.log.size() - 1));
        state.fieldGlobals().repel = 0;
        assertEquals(1, handlers.useInField("INFINITEREPEL", scene));
        assertTrue(state.fieldGlobals().infRepel);
        assertEquals(0, handlers.useInField("MAXREPEL", scene));
        assertEquals("无限喷雾的效果仍然存在。", scene.log.get(scene.log.size() - 1));
        handlers.useInField("INFINITEREPEL", scene);
        assertFalse(state.fieldGlobals().infRepel);
        assertEquals("已关闭无限喷雾。", scene.log.get(scene.log.size() - 1));
    }

    @Test
    @DisplayName("flutes, Exp. All and the Coin Case (:191-205, :377-392)")
    void fieldKeyItems() {
        handlers.useInField("BLACKFLUTE", scene);
        assertTrue(state.fieldGlobals().blackFluteUsed);
        handlers.useInField("WHITEFLUTE", scene);
        assertTrue(state.fieldGlobals().whiteFluteUsed);
        assertFalse(state.fieldGlobals().blackFluteUsed);
        state.fieldGlobals().coins = 12345;
        handlers.useInField("COINCASE", scene);
        assertEquals("代币:12,345", scene.log.get(scene.log.size() - 1));
        state.inventory().add("EXPALL", 1);
        handlers.useInField("EXPALL", scene);
        assertEquals(0, state.inventory().count("EXPALL"));
        assertEquals(1, state.inventory().count("EXPALLOFF"));
    }

    @Test
    @DisplayName("Sacred Ash revives every fainted Pokemon, and does nothing when none is (:271-304)")
    void sacredAsh() {
        assertEquals(0, handlers.sacredAsh(scene));
        assertEquals("这没有任何效果…", scene.log.get(scene.log.size() - 1));
        Pokemon second = add("PLAIN", 20);
        pkmn.hp = 0;
        second.hp = 0;
        assertEquals(3, handlers.sacredAsh(scene));
        assertEquals(pkmn.maxHp(), pkmn.hp);
        assertEquals(second.maxHp(), second.hp);
    }

    @Test
    @DisplayName("an item handler runs on its own thread and is answered from the screen side (ItemTask)")
    void itemTask() {
        pkmn.hp = 5;
        ItemTask task = ItemTask.start(s -> handlers.useOnPokemon("POTION", pkmn, s));
        List<TaskItemScene.Kind> kinds = new ArrayList<>();
        while (!task.resume()) {
            TaskItemScene.Request request = task.pending();
            kinds.add(request.kind);
            if (request.kind == TaskItemScene.Kind.DISPLAY) {
                assertEquals("Plain的HP恢复了20点。", request.text);
            }
            task.answer(null);
        }
        assertEquals(List.of(TaskItemScene.Kind.REFRESH, TaskItemScene.Kind.DISPLAY), kinds);
        assertEquals(25, pkmn.hp);
    }
}
