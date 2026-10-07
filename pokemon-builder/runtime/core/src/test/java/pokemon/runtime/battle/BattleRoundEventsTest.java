package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.TrainerState;
import pokemon.runtime.state.Inventory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B8: the round's events come out in the plugin's order ({@code Battle.roundEvents}).
 * <ul>
 * <li>{@code Battler_UseMove:305} brief "使用" -> {@code :719} hit flash ->
 *     {@code :728} critical / effectiveness lines -> {@code :753} pbFaint
 *     (brief "倒下了" then pbFaintBattler);</li>
 * <li>each side's move is its own {@code pbHitAndHPLossAnimation};</li>
 * <li>a multi-hit move flashes once per hit ({@code :458-482}), its
 *     effectiveness and "击中了N次" lines come after the last hit (:495-506);</li>
 * <li>{@code pbEndOfRoundPhase}'s poison: pbHPChanged, then the line, then pbFaint
 *     (Battle_Phase_EndOfRound:417-420, Battler_Statuses:462-464).</li>
 * </ul>
 */
class BattleRoundEventsTest {
    @TempDir Path temp;
    private PbsData data;

    @BeforeEach void setup() {
        data = PbsData.parse(temp.toFile());
        species("NORM", "NORMAL", 100);
        species("ROCKY", "ROCK", 100);
        type("NORMAL");
        PbsData.TypeInfo rock = type("ROCK");
        rock.weaknesses.add("FIGHTING");
        type("FIGHTING");
        move("TACKLE", "NORMAL", 40, "Physical", null);
        move("BIGHIT", "NORMAL", 250, "Physical", null);
        move("CHOP", "FIGHTING", 40, "Physical", null);
        move("DOUBLE", "NORMAL", 10, "Physical", "0BD");           // two hits
        move("SPLASH", "NORMAL", 0, "Status", "001");              // does nothing
    }

    private void species(String id, String type, int base) {
        PbsData.Species s = new PbsData.Species();
        s.internalName = id; s.name = id;
        s.types.add(type);
        s.baseStats = new int[] {base, base, base, base, base, base};
        data.species.put(id, s);
    }
    private PbsData.TypeInfo type(String id) {
        PbsData.TypeInfo t = new PbsData.TypeInfo();
        t.internalName = id; t.name = id;
        data.types.put(id, t);
        return t;
    }
    private void move(String id, String type, int power, String category, String function) {
        PbsData.Move m = new PbsData.Move();
        m.internalName = id; m.name = id; m.type = type; m.power = power;
        m.category = category; m.pp = 20; m.accuracy = 0;             // 0 = never misses (:128)
        m.function = function;
        data.moves.put(id, m);
    }
    private Pokemon mon(String species, int level, String moveId) {
        Pokemon p = new Pokemon(data.species(species), level, data);
        p.moves.clear();
        p.moves.add(new Pokemon.MoveSlot(data.move(moveId)));
        return p;
    }
    private Battle battle(Pokemon player, Pokemon foe, long seed) {
        return new Battle(data, new Random(seed), null).addPlayer(player).addFoe(foe);
    }
    private static List<String> trace(Battle battle) {
        List<String> out = new ArrayList<>();
        for (Battle.RoundEvent e : battle.roundEvents) out.add(e.toString());
        return out;
    }
    private static int indexOf(List<String> trace, String prefix, int from) {
        for (int i = Math.max(0, from); i < trace.size(); i++) {
            if (trace.get(i).startsWith(prefix)) return i;
        }
        return -1;
    }
    private static int count(List<String> trace, String prefix) {
        int n = 0;
        for (String s : trace) if (s.startsWith(prefix)) n++;
        return n;
    }

    @Test
    @DisplayName("each side's move: brief 使用, then its own hit flash (not merged)")
    void bothSidesFlashSeparately() {
        Battle battle = battle(mon("NORM", 50, "TACKLE"), mon("NORM", 50, "TACKLE"), 11);
        battle.step();
        List<String> t = trace(battle);
        assertEquals(2, count(t, "HIT"), t.toString());
        int use1 = indexOf(t, "BRIEF:", 0);
        int hit1 = indexOf(t, "HIT", 0);
        int use2 = indexOf(t, "BRIEF:", use1 + 1);
        int hit2 = indexOf(t, "HIT", hit1 + 1);
        assertTrue(t.get(use1).endsWith("使用TACKLE！"), t.toString());
        assertTrue(use1 < hit1 && hit1 < use2 && use2 < hit2, t.toString());
        Battle.HitEvent a = battle.roundEvents.get(hit1).hits[0];
        Battle.HitEvent b = battle.roundEvents.get(hit2).hits[0];
        assertNotEquals(a.idxBattler, b.idxBattler, "one flash per target: " + t);
        assertTrue(a.oldHp > a.newHp && b.oldHp > b.newHp, t.toString());
    }

    @Test
    @DisplayName("the effectiveness line comes after the flash (:719 -> :728)")
    void effectivenessAfterFlash() {
        Battle battle = battle(mon("NORM", 50, "CHOP"), mon("ROCKY", 50, "SPLASH"), 5);
        battle.step();
        List<String> t = trace(battle);
        int hit = indexOf(t, "HIT:1/", 0);
        int line = t.indexOf("MSG:这非常有效！");
        assertTrue(hit >= 0 && line > hit, t.toString());
        assertEquals(2, battle.roundEvents.get(hit).hits[0].effectiveness, "Move_Usage:275 superEffective -> 2");
    }

    @Test
    @DisplayName("a KO: flash, lines, brief 倒下了, pbFaintBattler - once (Battler_ChangeSelf:70)")
    void faintComesAfterTheHitAndOnlyOnce() {
        Battle battle = battle(mon("NORM", 100, "BIGHIT"), mon("NORM", 2, "TACKLE"), 3);
        battle.step();
        List<String> t = trace(battle);
        int hit = indexOf(t, "HIT:1/", 0);
        int fainted = indexOf(t, "BRIEF:", hit);
        int faint = t.indexOf("FAINT:1");
        assertTrue(hit >= 0 && fainted > hit && faint == fainted + 1, t.toString());
        assertTrue(t.get(fainted).endsWith("倒下了！"), t.toString());
        assertEquals(1, count(t, "FAINT:1"), "pbFaint returns at :70 once @fainted is set: " + t);
        long uses = t.stream().filter(x -> x.startsWith("BRIEF:") && x.contains("使用")).count();
        assertEquals(1, uses, "the fainted foe never moves: " + t);
        assertTrue(battle.foe().faintedFlag);
    }

    @Test
    @DisplayName("a two-hit move flashes per hit and reports the hits afterwards (:458-506)")
    void multiHitFlashesPerHit() {
        Battle battle = battle(mon("NORM", 50, "DOUBLE"), mon("NORM", 100, "SPLASH"), 9);
        battle.step();
        List<String> t = trace(battle);
        List<Battle.HitEvent> hits = new ArrayList<>();
        for (Battle.RoundEvent e : battle.roundEvents) {
            if (e.kind == Battle.RoundEvent.Kind.HIT && e.hits[0].idxBattler == 1) hits.add(e.hits[0]);
        }
        assertEquals(2, hits.size(), t.toString());
        assertEquals(hits.get(0).newHp, hits.get(1).oldHp, "the second hit starts where the first ended: " + t);
        int secondHit = indexOf(t, "HIT:1/", indexOf(t, "HIT:1/", 0) + 1);
        int times = t.indexOf("MSG:击中了2次！");
        assertTrue(times > secondHit, t.toString());
    }

    @Test
    @DisplayName("poison at the end of round: HP bar, line, then the faint (EndOfRound:417-420)")
    void poisonOrder() {
        Battle battle = battle(mon("NORM", 50, "SPLASH"), mon("NORM", 50, "SPLASH"), 1);
        Battler foe = battle.foe();
        foe.setStatus("POISON");
        foe.hp = 1;
        foe.syncHp();
        battle.step();
        List<String> t = trace(battle);
        int hp = indexOf(t, "HP:1/1>0", 0);
        int line = indexOf(t, "MSG:", hp);
        int fainted = indexOf(t, "BRIEF:", line);
        assertTrue(hp >= 0 && line > hp && t.get(line).endsWith("因为中毒受到了伤害！"), t.toString());
        assertTrue(fainted > line && t.get(fainted).endsWith("倒下了！") && t.get(fainted + 1).equals("FAINT:1"),
                t.toString());
        assertEquals("", foe.status, "Battler_ChangeSelf:76 status = NONE");
    }

    @Test
    @DisplayName("our Pokemon at or below 1/4 HP after a hit: pbBGMPlay(\"Battle low HP\") (Move_Usage:286-291)")
    void lowHpBgm() {
        Battle battle = battle(mon("NORM", 50, "SPLASH"), mon("NORM", 5, "TACKLE"), 2);
        Battler player = battle.player();
        player.hp = player.maxHp() / 4 + 1;
        player.syncHp();
        battle.step();
        List<String> t = trace(battle);
        int hit = indexOf(t, "HIT:0/", 0);
        assertTrue(hit >= 0, t.toString());
        assertEquals("BGM:Battle low HP", t.get(hit + 1), t.toString());
        // A hit on the opponent never switches the BGM (:287 t.pbOwnedByPlayer?).
        Battle other = battle(mon("NORM", 50, "TACKLE"), mon("NORM", 50, "SPLASH"), 2);
        other.foe().hp = other.foe().maxHp() / 4 + 1;
        other.step();
        assertEquals(0, count(trace(other), "BGM"), trace(other).toString());
    }

    @Test
    @DisplayName("confusion self-hit: 混乱了, flash on itself, then its line (Battler_UseMove:139-141)")
    void confusionSelfHit() {
        boolean seen = false;
        for (long seed = 0; seed < 60 && !seen; seed++) {
            Battle battle = battle(mon("NORM", 50, "SPLASH"), mon("NORM", 50, "SPLASH"), seed);
            battle.player().confusion = 5;
            battle.step();
            List<String> t = trace(battle);
            int line = t.indexOf("MSG:混乱中伤害了自己！");
            if (line < 0) continue;
            seen = true;
            assertTrue(t.get(line - 1).startsWith("HIT:0/"), t.toString());
            assertTrue(t.get(line - 2).endsWith("混乱了！"), t.toString());
        }
        assertTrue(seen, "a 33% self-hit shows up within 60 seeds");
    }

    @Test
    @DisplayName("the port hands the screen: its own lines, the engine's events, the closing line")
    void portEventStream() {
        TrainerState trainer = new TrainerState();
        trainer.party.add(mon("NORM", 50, "TACKLE"));
        InteractiveBattlePort port = new InteractiveBattlePort(trainer, new Inventory(), () -> data,
                () -> { }, new Random(4));
        port.freeWildBattle(mon("NORM", 50, "TACKLE"));
        assertTrue(port.session().chooseMove(0));
        Array<Battle.RoundEvent> events = port.session().takeEvents();
        List<String> t = new ArrayList<>();
        for (Battle.RoundEvent e : events) t.add(e.toString());
        assertEquals(2, count(t, "HIT"), t.toString());
        assertEquals(Battle.RoundEvent.Kind.MESSAGE, events.first().kind);
        assertTrue(events.first().brief, "the round opens with the brief 使用 line: " + t);
        assertFalse(events.peek().kind == Battle.RoundEvent.Kind.MESSAGE && events.peek().paused,
                "an engine line keeps pbDisplay's own timing: " + t);
        assertEquals(0, port.session().takeEvents().size, "handed over once");
        // The text view still carries every line.
        assertEquals(2, port.session().message.split("使用", -1).length - 1, port.session().message);
    }

    @Test
    @DisplayName("the move the player picks is the one used (Battle_Phase_Attack:146 -> Battler_UseMove:192 choice[2])")
    void chosenSlotIsUsed() {
        TrainerState trainer = new TrainerState();
        Pokemon mine = mon("NORM", 50, "TACKLE");
        mine.moves.add(new Pokemon.MoveSlot(data.move("CHOP")));
        trainer.party.add(mine);
        InteractiveBattlePort port = new InteractiveBattlePort(trainer, new Inventory(), () -> data,
                () -> { }, new Random(4));
        port.freeWildBattle(mon("NORM", 50, "SPLASH"));
        int tacklePp = mine.moves.get(0).pp;
        int chopPp = mine.moves.get(1).pp;
        assertTrue(port.session().chooseMove(1));
        assertEquals(tacklePp, mine.moves.get(0).pp, "slot 0 untouched");
        assertEquals(chopPp - 1, mine.moves.get(1).pp, "slot 1 spent its PP");
        assertTrue(port.session().message.contains("使用CHOP！"), port.session().message);
        // The choice is spent: the next round uses what is picked then (Battle_Phase_Command:183).
        assertTrue(port.session().chooseMove(0));
        assertEquals(tacklePp - 1, mine.moves.get(0).pp);
    }
}
