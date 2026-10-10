package pokemon.runtime.event;

import org.junit.jupiter.api.Test;
import pokemon.runtime.field.ItemHandlers;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 371_goldFinger on the project's real data (skipped when generated/ is missing). */
class GoldFingerTest {

    /** Answers the task's requests from a script; messages need none. */
    private static class Replay {
        final ArrayDeque<Object> answers = new ArrayDeque<>();
        final List<String> log = new ArrayList<>();

        Replay answers(Object... values) {
            answers.addAll(Arrays.asList(values));
            return this;
        }

        Object answer(TaskFieldScene.Request request) {
            log.add(request.kind + ":" + request.text);
            switch (request.kind) {
                case MESSAGE: case SE: case ME: case WAIT: case FLASH: case ACTION:
                    return null;                                   // nothing to decide
                default:
                    break;
            }
            Object next = answers.poll();
            assertNotNull(next, "no scripted answer for " + request.kind + " " + request.text + " after " + log);
            return next;
        }

        boolean said(String fragment) {
            return log.stream().anyMatch(line -> line.contains(fragment));
        }
    }

    private PbsData data;
    private GameState state;

    private GoldFinger start(Replay replay) throws Exception {
        File root = pokemon.runtime.map.TestData.runtimeDataRoot();
        if (root == null) {
            return null;
        }
        data = PbsData.parse(root);
        state = new GameState();
        state.trainer().party.add(new Pokemon(data.species("TORCHIC"), 5, data));
        return new GoldFinger(state, data, new TaskFieldScene(replay::answer),
                new ItemHandlers(data, state, java.time.LocalTime::now), 6);
    }

    @Test
    void withoutTheCheatSwitchesOnlyTheHabitatRefreshIsOffered() throws Exception {
        Replay replay = new Replay().answers(Boolean.TRUE);
        GoldFinger gold = start(replay);
        if (gold == null) return;
        gold.goldFinger();
        assertTrue(replay.log.get(0).startsWith("CONFIRM:确定要刷新野生宝可梦分布的导航数据吗？"), replay.log.toString());
        assertTrue(replay.said("已刷新分布导航"), replay.log.toString());
        assertTrue(replay.answers.isEmpty());
    }

    @Test
    void maxMoneyAndExpPot() throws Exception {
        Replay replay = new Replay().answers(5, 6, 1234, -1);       // 金钱最大, 经验储罐 -> 1234, back
        GoldFinger gold = start(replay);
        if (gold == null) return;
        state.switches().set(42, true);
        state.trainer().expPot = 50;
        gold.goldFinger();
        assertEquals(999_999_999, state.trainer().money);
        assertEquals(1234, state.trainer().expPot);
        assertTrue(replay.said("金钱已设为最大。"));
    }

    @Test
    void addsTheChosenItem() throws Exception {
        Replay replay = new Replay().answers(2, 0, 5, -1);          // 添加道具 -> 大师球 x5, back
        GoldFinger gold = start(replay);
        if (gold == null) return;
        state.switches().set(197, true);
        gold.goldFinger();
        assertEquals(5, state.inventory().count("MASTERBALL"));
        assertTrue(replay.said("已向背包添加5个"), replay.log.toString());
    }

    @Test
    void changesALevelAndANature() throws Exception {
        // 修改精灵 -> party slot 0 -> 修改等级 -> 12 -> 修改性格 -> nature 3 -> back -> back
        Replay replay = new Replay().answers(4, 0, 1, 12, 2, 3, -1, -1);
        GoldFinger gold = start(replay);
        if (gold == null) return;
        state.switches().set(42, true);
        Pokemon torchic = state.trainer().party.get(0);
        gold.goldFinger();
        assertEquals(12, torchic.level);
        assertEquals(data.natures.get(3), torchic.nature);
        assertTrue(replay.said("已被修改为12级。"), replay.log.toString());
        assertTrue(replay.said("的性格已被修改为" + data.natures.get(3).name), replay.log.toString());
    }

    @Test
    void theNatureListShowsTheRaisedAndLoweredStat() throws Exception {
        List<List<String>> lists = new ArrayList<>();
        Replay replay = new Replay() {
            @Override
            Object answer(TaskFieldScene.Request request) {
                if (request.kind == TaskFieldScene.Kind.CHOOSE && request.text.contains("性格")) {
                    lists.add(request.commands);
                }
                return super.answer(request);
            }
        }.answers(4, 0, 2, -1, -1, -1);
        GoldFinger gold = start(replay);
        if (gold == null) return;
        state.switches().set(42, true);
        gold.goldFinger();
        assertEquals(1, lists.size());
        assertEquals(data.natures.size, lists.get(0).size());
        assertTrue(lists.get(0).stream().anyMatch(t -> t.contains("(+") && t.contains(", -")), lists.get(0).toString());
        assertTrue(lists.get(0).stream().anyMatch(t -> t.endsWith("(---)")), lists.get(0).toString());
    }

    @Test
    void unlocksTheDexAndFillsEverySpecies() throws Exception {
        Replay replay = new Replay().answers(3, Boolean.TRUE, -1);
        GoldFinger gold = start(replay);
        if (gold == null) return;
        state.switches().set(42, true);
        gold.goldFinger();
        List<Boolean> unlocked = state.fieldGlobals().pokedexUnlocked;
        assertEquals(6, unlocked.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(unlocked.get(i), "list " + i);
        }
        assertFalse(unlocked.get(5), "the National Dex stays locked outside debug");
        assertTrue(state.trainer().seen.contains("TORCHIC"));
        assertTrue(state.trainer().owned.contains("BULBASAUR") || state.trainer().owned.size() > 100);
        assertTrue(replay.said("获得全部图鉴并填充信息成功。"));
    }

    @Test
    void teachesAMoveFromTheLegalList() throws Exception {
        // 修改精灵 -> slot 0 -> 教学招式 -> first entry of the list -> back -> back
        List<String> list = new ArrayList<>();
        Replay replay = new Replay() {
            @Override
            Object answer(TaskFieldScene.Request request) {
                if (request.kind == TaskFieldScene.Kind.CHOOSE_LIST) {
                    list.addAll(request.commands);
                }
                return super.answer(request);
            }
        }.answers(4, 0, 5, 0, -1, -1);
        GoldFinger gold = start(replay);
        if (gold == null) return;
        state.switches().set(42, true);
        Pokemon torchic = state.trainer().party.get(0);
        torchic.moves.clear();
        gold.goldFinger();
        assertTrue(list.size() > 10, "the legal move list was offered: " + replay.log);
        assertEquals(1, torchic.moves.size, "the first listed move was learned: " + replay.log);
        assertEquals(list.get(0), torchic.moves.get(0).move.name);
    }
}
