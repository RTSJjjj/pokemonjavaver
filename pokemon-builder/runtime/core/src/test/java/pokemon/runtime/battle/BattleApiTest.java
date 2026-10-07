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
 * M0 接线 B: the {@code Battle} surface appended for the handler and
 * move-effect bodies (stage4-m0b-battler-api-notes.md §B).
 *
 * <p>Each test names the Ruby line it pins. The methods marked 登记 in
 * {@code Battle.java} are asserted for their documented placeholder behaviour
 * (never for plugin behaviour), so that a later wiring pass has a failing test
 * the moment it changes them.</p>
 */
class BattleApiTest {

    private static Pokemon pokemon(PbsData data, String species, int level) {
        return new Pokemon(data.species(species), level, data);
    }

    private static Battle battle(PbsData data, String player, String foe) {
        return new Battle(data, new Random(3), null)
                .addPlayer(pokemon(data, player, 20))
                .addFoe(pokemon(data, foe, 20));
    }

    // ------------------------------------------------------------------
    // A. fields + B. accessors
    // ------------------------------------------------------------------

    @Test
    @DisplayName("M0: @field and the initialize defaults (PokeBattle_Battle:112-182)")
    void fieldAndDefaults(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");

        // :112 @field = PokeBattle_ActiveField.new
        assertNotNull(battle.field);
        assertEquals(PBWeather.None, battle.field.weather);          // PokeBattle_ActiveField:27
        assertEquals(PBBattleTerrains.None, battle.field.terrain);   // PokeBattle_ActiveField:30

        assertEquals(0, battle.time);                                // :120
        assertEquals(0, battle.time());
        assertEquals(PBEnvironment.None, battle.environment);        // :121
        assertEquals(0, battle.environment());
        assertEquals(0, battle.decision);                            // :123
        assertFalse(battle.futureSight);                             // :171
        assertFalse(battle.endOfRound);                              // :172
        assertFalse(battle.moldBreaker);                             // :173

        // :182 @sideStatUps = [{}, {}]
        assertEquals(2, battle.sideStatUps.length);
        assertTrue(battle.sideStatUps[0].isEmpty());
        assertTrue(battle.sideStatUps[1].isEmpty());

        // :41 @scene - 登记 placeholder, and the injected tables.
        assertNull(battle.scene);
        assertSame(data, battle.pbs());

        // :52 @turnCount (Java's turns()).
        assertEquals(0, battle.turnCount());
        assertEquals(battle.turns(), battle.turnCount());
    }

    @Test
    @DisplayName("M0: weather()/terrain() read @field; pbWeather is the registered effective-weather stub (:673-676)")
    void weatherAndTerrainAccessors(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");

        battle.field.weather = PBWeather.Rain;
        battle.field.terrain = PBBattleTerrains.Electric;
        assertEquals(PBWeather.Rain, battle.weather());               // :675 @field.weather
        assertEquals(PBBattleTerrains.Electric, battle.terrain());
        // 登记: :674 needs Battler#hasActiveAbility?, so only :675 is transcribed.
        assertEquals(PBWeather.Rain, battle.pbWeather());
    }

    @Test
    @DisplayName("M0: pbRandom(x) is rand(x) - 0..x-1 - on the battle's own Random (:98)")
    void pbRandomReusesTheBattlesRandom(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle first = battle(data, "HERO", "FOE");
        Battle second = battle(data, "HERO", "FOE");
        // Both battles were built with new Random(3), and so was this reference.
        Random reference = new Random(3);
        for (int i = 0; i < 64; i++) {
            int expected = reference.nextInt(100);
            assertEquals(expected, first.pbRandom(100));
            assertEquals(expected, second.pbRandom(100));
        }
    }

    @Test
    @DisplayName("M0: pbRandom stays inside 0..x-1 for every x > 0 (:98 rand(x))")
    void pbRandomBounds(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = new Battle(data, new Random(11), null);
        for (int x = 1; x <= 16; x++) {
            for (int i = 0; i < 50; i++) {
                int value = battle.pbRandom(x);
                assertTrue(value >= 0 && value < x, "pbRandom(" + x + ") = " + value);
            }
        }
    }

    // ------------------------------------------------------------------
    // C. display
    // ------------------------------------------------------------------

    @Test
    @DisplayName("M0: display/displayBrief/displayPaused mirror B8's private pbDisplay/pbDisplayBrief (:773-783)")
    void displayMirrorsThePrivateMethods(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");

        battle.display("普通消息");                                    // :773-775
        battle.displayBrief("简报消息");                               // :777-779
        battle.displayPaused("等待按键");                              // :781-783

        assertEquals(3, battle.roundMessages.size);
        assertEquals("普通消息", battle.roundMessages.get(0));
        assertEquals("简报消息", battle.roundMessages.get(1));
        assertEquals("等待按键", battle.roundMessages.get(2));

        assertEquals(3, battle.roundEvents.size);
        Battle.RoundEvent normal = battle.roundEvents.get(0);
        assertEquals(Battle.RoundEvent.Kind.MESSAGE, normal.kind);
        assertFalse(normal.brief);
        assertFalse(normal.paused);
        assertFalse(normal.fromPort);

        Battle.RoundEvent brief = battle.roundEvents.get(1);
        assertEquals("简报消息", brief.text);
        assertTrue(brief.brief);

        Battle.RoundEvent paused = battle.roundEvents.get(2);
        assertEquals("等待按键", paused.text);
        assertTrue(paused.paused);
    }

    @Test
    @DisplayName("M0: the registered splashes/Pokedex calls stay silent; the wired animations queue ANIMATION events (:793-818, :652-656)")
    void registeredNoOpsDoNotThrowOrPrint(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        Battler player = battle.player();

        int messages = battle.roundMessages.size;
        int events = battle.roundEvents.size;
        battle.showAbilitySplash(player);                             // 登记: :801-808
        battle.hideAbilitySplash(player);                             // 登记: :810-813
        battle.replaceAbilitySplash(player);                          // 登记: :815-818
        battle.pbSetSeen(battle.foe());                               // 登记: :652-656

        // The four registered calls neither print nor queue anything.
        assertEquals(messages, battle.roundMessages.size);
        assertEquals(events, battle.roundEvents.size);

        // :797-799 pbCommonAnimation and :793-795 pbAnimation are WIRED now: each
        // queues one ANIMATION event for the scene to play (`if @showAnims`,
        // :794/:798). They still print nothing themselves.
        battle.commonAnimation("StatUp", player);                     // :797-799 targets=nil
        battle.commonAnimation("StatUp", player, battle.allBattlers());
        battle.animation(player.moveSlot(0), player, battle.allBattlers());      // :793 hitNum=0
        battle.animation(player.moveSlot(0), player, battle.allBattlers(), 1);

        assertEquals(messages, battle.roundMessages.size);
        assertEquals(events + 4, battle.roundEvents.size);
        for (int i = events; i < events + 4; i++) {
            Battle.RoundEvent event = battle.roundEvents.get(i);
            assertEquals(Battle.RoundEvent.Kind.ANIMATION, event.kind);
            assertNotNull(event.anim);
        }

        Battle.AnimationCall first = battle.roundEvents.get(events).anim;
        assertTrue(first.common);                                     // pbCommonAnimation
        assertEquals("StatUp", first.name);
        assertEquals(player.index, first.idxUser);
        assertEquals(-1, first.idxTarget);                            // targets=nil
        assertEquals(0, first.hitNum);

        Battle.AnimationCall second = battle.roundEvents.get(events + 1).anim;
        assertTrue(second.common);
        assertEquals("StatUp", second.name);
        assertEquals(player.index, second.idxUser);
        assertEquals(player.index, second.idxTarget);                 // Scene_Animations:529 targets[0]

        Battle.AnimationCall third = battle.roundEvents.get(events + 2).anim;
        assertFalse(third.common);                                    // pbAnimation
        assertEquals(player.moveSlot(0).id(), third.moveId);
        assertEquals(player.index, third.idxUser);
        assertEquals(player.index, third.idxTarget);
        assertEquals(0, third.hitNum);                                // :793 hitNum=0

        Battle.AnimationCall fourth = battle.roundEvents.get(events + 3).anim;
        assertFalse(fourth.common);
        assertEquals(player.moveSlot(0).id(), fourth.moveId);
        assertEquals(1, fourth.hitNum);

        // :794/:798 `if @showAnims`: the "Battle Effects" option silences both.
        battle.showAnims = false;
        battle.roundEvents.clear();
        battle.commonAnimation("StatUp", player);
        battle.animation(player.moveSlot(0), player, battle.allBattlers(), 0);
        assertEquals(0, battle.roundEvents.size);
    }

    @Test
    @DisplayName("M0: pbPlayer/pbCheckGlobalAbility/pbCheckOpposingAbility are registered nulls (:226, :478-489)")
    void registeredLookupsReturnNull(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");

        assertNull(battle.pbPlayer());                                // 登记: :226
        assertNull(battle.pbCheckGlobalAbility("CHLOROPHYLL"));       // 登记: :478-481
        assertNull(battle.pbCheckOpposingAbility("CHLOROPHYLL", 0, false));   // 登记: :483-489
    }

    @Test
    @DisplayName("M0: pbGetOwnerName returns the player's name; the opposing branch is registered (:266-271)")
    void ownerName(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.playerName = "小智";

        assertEquals("小智", battle.pbGetOwnerName(0));                // :270
        assertNull(battle.pbGetOwnerName(1));                          // 登记: :268
    }

    // ------------------------------------------------------------------
    // D. iteration / party / switching
    // ------------------------------------------------------------------

    @Test
    @DisplayName("M0: allBattlers/eachBattler drop the fainted (:437-442)")
    void allBattlersDropFainted(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");

        assertEquals(2, battle.allBattlers().size);
        assertEquals(2, battle.eachBattler().size);
        assertEquals(battle.player(), battle.allBattlers().get(0));
        assertEquals(battle.foe(), battle.allBattlers().get(1));

        battle.foe().hp = 0;
        assertEquals(1, battle.allBattlers().size);
        assertEquals(battle.player(), battle.allBattlers().get(0));
    }

    @Test
    @DisplayName("M0: the same/other side iterations use index parity (:444-463, AI_Move_EffectScores:3867-3870)")
    void sideIteration(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "SPOOK");

        assertEquals(1, battle.eachSameSideBattler(0).size);
        assertEquals(battle.player(), battle.eachSameSideBattler(0).get(0));
        assertEquals(battle.player(), battle.allSameSideBattlers(0).get(0));
        assertEquals(1, battle.eachOtherSideBattler(0).size);
        assertEquals(battle.foe(), battle.eachOtherSideBattler(0).get(0));
        assertEquals(battle.foe(), battle.allOtherSideBattlers(0).get(0));
        // idxBattler 1 is the foe's side, so the two flip over.
        assertEquals(battle.foe(), battle.eachSameSideBattler(1).get(0));
        assertEquals(battle.player(), battle.eachOtherSideBattler(1).get(0));

        assertEquals(1, battle.pbSideBattlerCount(0));                // :459-463
        assertEquals(1, battle.pbSideBattlerCount(1));

        battle.player().hp = 0;
        assertEquals(0, battle.pbSideBattlerCount(0));
        assertEquals(0, battle.eachSameSideBattler(0).size);
    }

    @Test
    @DisplayName("M0: pbAllFainted is the party able-count zero test (:353-355)")
    void allFainted(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = new Battle(data, new Random(1), null)
                .addPlayer(pokemon(data, "HERO", 20))
                .addFoe(pokemon(data, "FOE", 5));

        assertFalse(battle.pbAllFainted(0));
        assertFalse(battle.pbAllFainted(1));
        battle.foe().hp = 0;
        assertTrue(battle.pbAllFainted(1));                           // :354
        assertFalse(battle.pbAllFainted(0));
    }

    @Test
    @DisplayName("M0: pbCanSwitch/pbCanChooseNonActive delegate to the existing checks (:41-120)")
    void canSwitchDelegates(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = new Battle(data, new Random(1), null)
                .addPlayer(pokemon(data, "HERO", 20))
                .addPlayer(pokemon(data, "HERO", 20))
                .addFoe(pokemon(data, "FOE", 5));

        assertTrue(battle.pbCanSwitch(0));                            // :41 idxParty=-1
        assertTrue(battle.pbCanSwitch(0, 1));
        assertFalse(battle.pbCanSwitch(0, 0));                        // the active Pokemon itself
        assertTrue(battle.pbCanChooseNonActive(0));                   // :115-120
    }

    @Test
    @DisplayName("M0: pbCanChooseNonActive is false with no healthy bench (:115-120)")
    void canChooseNonActiveWithoutBench(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        assertFalse(battle.pbCanChooseNonActive(0));
    }

    @Test
    @DisplayName("M0: pbGetReplacementPokemonIndex(idx,true) picks among the legal replacements (:241-249)")
    void replacementIndexRandom(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = new Battle(data, new Random(1), null)
                .addPlayer(pokemon(data, "HERO", 20))
                .addPlayer(pokemon(data, "HERO", 20))
                .addFoe(pokemon(data, "FOE", 5));

        // :244-247 only party slot 1 can come in (slot 0 is already out).
        assertEquals(1, battle.pbGetReplacementPokemonIndex(0, true));

        // :248 nothing can come in at all.
        Battle alone = battle(data, "HERO", "FOE");
        assertEquals(-1, alone.pbGetReplacementPokemonIndex(0, true));

        // random=false opens the party screen (the screen owns it).
        assertEquals(Battle.OWNER_CHOOSES, battle.pbGetReplacementPokemonIndex(0));
    }

    @Test
    @DisplayName("M0: pbRecallAndReplace performs the swap (:256-262)")
    void recallAndReplace(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = new Battle(data, new Random(1), null)
                .addPlayer(pokemon(data, "HERO", 20))
                .addPlayer(pokemon(data, "HERO", 20))
                .addFoe(pokemon(data, "FOE", 5));

        assertEquals(0, battle.playerFieldIndex());
        battle.pbRecallAndReplace(0, 1);                              // :261 pbReplace
        assertEquals(1, battle.playerFieldIndex());
    }

    @Test
    @DisplayName("M0: pbCanRun follows Battle_Action_Running:5-28 for the transcribed branches")
    void canRun(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle wild = battle(data, "HERO", "SPOOK");
        assertTrue(wild.pbCanRun(0));                                 // :27

        // :9 a Ghost type always gets away with NEWEST_BATTLE_MECHANICS.
        assertTrue(wild.pbCanRun(1));

        // :8 the cannotRun rule only stops the player's own side.
        wild.setCanRun(false);
        assertFalse(wild.pbCanRun(0));
        assertTrue(wild.pbCanRun(1));

        // :6 a trainer battle can never be fled.
        Battle trainer = battle(data, "HERO", "FOE");
        trainer.trainerBattle = true;
        assertFalse(trainer.pbCanRun(0));

        // :20 @field.effects[PBEffects::FairyLock] > 0 traps the battler.
        Battle trapped = battle(data, "HERO", "FOE");
        trapped.field.effects.set(PBEffects.Field.FairyLock, 3);
        assertFalse(trapped.pbCanRun(0));
    }

    @Test
    @DisplayName("M0: @choices view reads the runtime's existing slots (:72, Battle_Phase_Command:5-11)")
    void choicesView(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = new Battle(data, new Random(1), null)
                .addPlayer(pokemon(data, "HERO", 20))
                .addPlayer(pokemon(data, "HERO", 20))
                .addFoe(pokemon(data, "FOE", 5));

        // :7-10 nothing chosen.
        Object[] none = battle.choices(0);
        assertEquals(":None", none[0]);
        assertEquals(0, none[1]);
        assertNull(none[2]);
        assertEquals(-1, none[3]);

        // Battle_Action_AttacksPriority:74-77 a registered move.
        assertTrue(battle.registerMove(0, 0));
        Object[] move = battle.choices(0);
        assertEquals(":UseMove", move[0]);
        assertEquals(0, move[1]);
        assertSame(battle.chosenMove(0), move[2]);
        assertEquals(-1, move[3]);

        battle.pbClearChoice(0);                                      // Battle_Phase_Command:5-11
        assertEquals(":None", battle.choices(0)[0]);

        // Battle_Action_Switching:124-125 a registered switch.
        assertTrue(battle.registerSwitch(0, 1));
        Object[] switching = battle.choices(0);
        assertEquals(":SwitchOut", switching[0]);
        assertEquals(1, switching[1]);
        assertNull(switching[2]);
        assertEquals(-1, switching[3]);
    }

    // ------------------------------------------------------------------
    // C (continued). weather
    // ------------------------------------------------------------------

    @Test
    @DisplayName("M0: pbStartWeather sets the field, the duration and the message (:679-705)")
    void startWeather(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");

        battle.pbStartWeather(null, PBWeather.Rain, true);            // :682 fixedDuration -> 5
        assertEquals(PBWeather.Rain, battle.field.weather);           // :681
        assertEquals(5, battle.field.weatherDuration);                // :687
        assertEquals(1, battle.roundMessages.size);
        assertEquals("开始下雨了！", battle.roundMessages.get(0));     // :692

        // :680 same weather again is a no-op.
        battle.pbStartWeather(null, PBWeather.Rain, true);
        assertEquals(1, battle.roundMessages.size);

        battle.pbStartWeather(null, PBWeather.Sun);                   // :679 both defaults
        assertEquals(PBWeather.Sun, battle.field.weather);
        assertEquals(-1, battle.field.weatherDuration);               // :682 !fixedDuration -> -1
        assertEquals("阳光变得刺眼！", battle.roundMessages.get(1));   // :691
    }

    @Test
    @DisplayName("M0: pbStartWeather ends the primordial weather and starts the default (:704-731)")
    void startWeatherEndsPrimordial(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        battle.field.defaultWeather = PBWeather.Rain;                 // :731

        battle.pbStartWeather(null, PBWeather.HarshSun, false, false);   // showAnim=false
        // :696 the Harsh Sun line, then :714 the primordial end, then :731 the
        // default weather comes back (:692).
        assertEquals(3, battle.roundMessages.size);
        assertEquals("阳光变得非常刺眼！", battle.roundMessages.get(0));
        assertEquals("强烈的阳光消散了！", battle.roundMessages.get(1));
        assertEquals("开始下雨了！", battle.roundMessages.get(2));
        assertEquals(PBWeather.Rain, battle.field.weather);
    }

    @Test
    @DisplayName("M0: pbEndPrimordialWeather resets HarshSun/HeavyRain/StrongWinds (:707-733)")
    void endPrimordialWeather(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));

        Battle harsh = battle(data, "HERO", "FOE");
        harsh.field.weather = PBWeather.HarshSun;
        harsh.pbEndPrimordialWeather();                               // :711-715
        assertEquals(PBWeather.None, harsh.field.weather);
        assertEquals("强烈的阳光消散了！", harsh.roundMessages.get(0));

        Battle heavy = battle(data, "HERO", "FOE");
        heavy.field.weather = PBWeather.HeavyRain;
        heavy.pbEndPrimordialWeather();                               // :716-720
        assertEquals(PBWeather.None, heavy.field.weather);
        assertEquals("暴雨停歇了！", heavy.roundMessages.get(0));

        Battle winds = battle(data, "HERO", "FOE");
        winds.field.weather = PBWeather.StrongWinds;
        winds.pbEndPrimordialWeather();                               // :721-725
        assertEquals(PBWeather.None, winds.field.weather);
        assertEquals("神秘的气流消散了！", winds.roundMessages.get(0));

        // A normal weather is untouched (:710-726 has no branch for it, and
        // :727's guard sees no change).
        Battle sunny = battle(data, "HERO", "FOE");
        sunny.field.weather = PBWeather.Sun;
        sunny.pbEndPrimordialWeather();
        assertEquals(PBWeather.Sun, sunny.field.weather);
        assertEquals(0, sunny.roundMessages.size);
    }

    @Test
    @DisplayName("M0: turnCount() tracks the engine's turn counter (:52)")
    void turnCount(@TempDir Path tempDir) throws Exception {
        PbsData data = PbsData.parse(syntheticPbs(tempDir));
        Battle battle = battle(data, "HERO", "FOE");
        assertEquals(0, battle.turnCount());
        battle.step();
        assertEquals(1, battle.turns());
        assertEquals(1, battle.turnCount());
    }

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    /** HERO (strong, NORMAL), FOE (weak, NORMAL), SPOOK (GHOST) - as in BattleSwitchingTest. */
    private static File syntheticPbs(Path root) throws Exception {
        write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\"}");
        write(root, "pokemon.json", "{\"total\":3,\"species\":{"
                + "\"HERO\":{\"id\":1,\"internalName\":\"HERO\",\"name\":\"Hero\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[100,100,100,100,100,100],"
                + "\"growthRate\":\"Medium\",\"baseExp\":200,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"FOE\":{\"id\":2,\"internalName\":\"FOE\",\"name\":\"Foe\","
                + "\"types\":[\"NORMAL\"],\"baseStats\":[1,1,1,1,1,1],"
                + "\"growthRate\":\"Medium\",\"baseExp\":20,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]},"
                + "\"SPOOK\":{\"id\":3,\"internalName\":\"SPOOK\",\"name\":\"Spook\","
                + "\"types\":[\"GHOST\"],\"baseStats\":[50,50,50,50,50,50],"
                + "\"growthRate\":\"Medium\",\"baseExp\":50,\"abilities\":[],"
                + "\"moves\":[{\"level\":1,\"move\":\"SLASH\"}]}}}");
        write(root, "moves.json", "{\"total\":1,\"moves\":{"
                + "\"SLASH\":{\"id\":1,\"internalName\":\"SLASH\",\"name\":\"Slash\","
                + "\"function\":\"000\",\"power\":70,\"type\":\"NORMAL\",\"category\":\"Physical\",\"accuracy\":100,\"pp\":20,\"target\":\"NearOther\"}}}");
        write(root, "abilities.json", "{\"total\":0,\"abilities\":{}}");
        write(root, "types.json", "{\"total\":2,\"types\":{"
                + "\"NORMAL\":{\"id\":0,\"internalName\":\"NORMAL\",\"name\":\"Normal\"},"
                + "\"GHOST\":{\"id\":7,\"internalName\":\"GHOST\",\"name\":\"Ghost\",\"immunities\":[\"NORMAL\"]}}}");
        write(root, "natures.json", "{\"total\":1,\"natures\":["
                + "{\"id\":0,\"internalName\":\"HARDY\",\"name\":\"Hardy\"}]}");
        write(root, "items.json", "{\"total\":0,\"items\":{}}");
        write(root, "pokemonforms.json", "{\"total\":0,\"forms\":{}}");
        write(root, "trainertypes.json", "{\"total\":0,\"trainerTypes\":{}}");
        write(root, "tm.json", "{\"total\":0,\"compatibility\":{}}");
        return root.toFile();
    }
}
