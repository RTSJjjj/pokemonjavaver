package pokemon.runtime.battle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** The project's own moves that used to be shells (not registered, or missing the state they read). Skipped without generated/pbs. */
class CustomMovesRealDataTest {

    private static PbsData data() {
        for (File d = new File("").getAbsoluteFile(); d != null; d = d.getParentFile()) {
            for (String candidate : new String[] {"generated", "pokemon-builder/generated"}) {
                File root = new File(d, candidate);
                if (new File(root, "pbs").isDirectory()) return PbsData.parse(root);
            }
        }
        assumeTrue(false, "generated/pbs not found");
        return null;
    }

    @Test
    @DisplayName("every function code a move of the project uses has an implementation (GRANDEUR's 0BQ included)")
    void everyUsedFunctionCodeIsRegistered() {
        PbsData data = data();
        StringBuilder missing = new StringBuilder();
        for (PbsData.Move move : data.moves.values()) {
            if (move.function == null || "000".equals(move.function)) continue;
            if (MoveEffectRegistry.of(move.function).getClass().getSimpleName().contains("Unimplemented")) {
                missing.append(move.internalName).append('/').append(move.function).append(' ');
            }
        }
        assertEquals("", missing.toString().trim(), "moves running as PokeBattle_UnimplementedMove");
    }

    @Test
    @DisplayName("Rage Fist counts the hits its user took (num_times_hit)")
    void rageHitsAreCounted() {
        PbsData data = data();
        Pokemon mine = new Pokemon(data.species("PRIMEAPE"), 50, data);
        Pokemon foe = new Pokemon(data.species("BIDOOF"), 5, data);
        Battle b = new Battle(data, new Random(1), (user, target, moves) -> 0).addPlayer(mine).addFoe(foe);
        assertEquals(0, b.player().numTimesHit());
        b.pbAddRageHit(b.player(), 1);
        b.pbAddRageHit(b.player(), 1);
        assertEquals(2, b.player().numTimesHit());
        assertEquals(0, b.foe().numTimesHit(), "the other side keeps its own count");
    }

    @Test
    @DisplayName("the AI revives its fainted attacker with the highest Attack / Sp. Atk")
    void aiRevivesItsBestAttacker() {
        PbsData data = data();
        Pokemon mine = new Pokemon(data.species("PRIMEAPE"), 50, data);
        Pokemon lead = new Pokemon(data.species("BIDOOF"), 30, data);
        Pokemon weak = new Pokemon(data.species("SHUCKLE"), 30, data);
        Pokemon strong = new Pokemon(data.species("GARCHOMP"), 30, data);
        Battle b = new Battle(data, new Random(1), (user, target, moves) -> 0)
                .addPlayer(mine).addFoe(lead).addFoe(weak).addFoe(strong);
        b.foeParty().get(1).hp = 0;
        b.foeParty().get(1).faintedFlag = true;
        b.foeParty().get(2).hp = 0;
        b.foeParty().get(2).faintedFlag = true;
        b.pbReviveInParty(1, false);
        assertTrue(b.foeParty().get(2).hp > 0, "the strong attacker came back");
        assertEquals(0, b.foeParty().get(1).hp, "the weak one stays down");
    }
}
