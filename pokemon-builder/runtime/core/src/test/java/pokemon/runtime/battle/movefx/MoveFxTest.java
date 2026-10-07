package pokemon.runtime.battle.movefx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.PBTypeEffectiveness;
import pokemon.runtime.pokemon.PbsData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage 4 / L2: the observable, non-throwing surface of the move-effect block.
 *
 * <p>Everything the strategy classes do at battle time goes through the
 * {@code PendingApi} / {@link MoveFxPendingApi} stub layers, which throw by
 * design, so this test pins down exactly the three things that ARE real in this
 * batch:</p>
 * <ol>
 * <li>the two synthesized move records ({@code PokeBattle_Confusion#initialize}
 *     Move_Effects_Generic.rb:22-40 and {@code PokeBattle_Struggle#initialize}
 *     :54-72), field for field;</li>
 * <li>the function-code lookup's default
 *     ({@code PokeBattle_Move.pbFromPBMove}:58 falls back to
 *     {@code PokeBattle_UnimplementedMove}) and the deliberately separate
 *     {@code confusion()}/{@code struggle()} factories (task-11 decision 13);</li>
 * <li>the "honest stub" contract: every method this batch does not transcribe
 *     throws, and its message names the Ruby source it is waiting for.</li>
 * </ol>
 */
class MoveFxTest {

    // ==================================================================
    // 1) The synthesized Confusion / Struggle move records
    // ==================================================================

    @Test
    @DisplayName("L2: PokeBattle_Confusion's synthetic move matches Move_Effects_Generic.rb:22-40")
    void confusionMoveIsSynthesizedFieldForField() {
        BattleMove move = MoveEffectRegistry.confusionMove();

        assertEquals(0, move.id());                      // :25 @id         = 0
        assertEquals("", move.name());                   // :26 @name       = ""
        assertEquals("000", move.function());            // :27 @function   = "000"
        assertEquals(40, move.power());                  // :28 @baseDamage = 40
        assertNull(move.type());                         // :29 @type       = -1  (null is the plugin's -1)
        assertEquals("Physical", move.category());       // :30 @category   = 0
        assertEquals(100, move.accuracy());              // :31 @accuracy   = 100
        assertEquals(-1, move.move.pp);                  // :32 @pp         = -1
        assertEquals("NearOther", move.target());        // :33 @target     = 0   (PBTargets::NearOther == 0)
        assertEquals(0, move.priority());                // :34 @priority   = 0
        assertEquals("", move.flags());                  // :35 @flags      = ""
        assertEquals(0, move.additionalChance());        // :36 @addlEffect = 0
    }

    @Test
    @DisplayName("L2: PokeBattle_Struggle's synthetic move matches Move_Effects_Generic.rb:54-72 (move == null)")
    void struggleMoveWithoutANamedMove() {
        BattleMove move = MoveEffectRegistry.struggleMove(null);

        assertEquals(-1, move.id());                     // :57 @id         = (move) ? move.id : -1
        assertEquals("挣扎", move.name());                 // :58 @name       = _INTL("挣扎")
        assertEquals("002", move.function());            // :59 @function   = "002"
        assertEquals(50, move.power());                  // :60 @baseDamage = 50
        assertNull(move.type());                         // :61 @type       = -1
        assertEquals("Physical", move.category());       // :62 @category   = 0
        assertEquals(0, move.accuracy());                // :63 @accuracy   = 0
        assertEquals(-1, move.move.pp);                  // :64 @pp         = -1
        assertEquals("NearOther", move.target());        // :65 @target     = 0
        assertEquals(0, move.priority());                // :66 @priority   = 0
        assertEquals("", move.flags());                  // :67 @flags      = ""
        assertEquals(0, move.additionalChance());        // :68 @addlEffect = 0
    }

    @Test
    @DisplayName("L2: PokeBattle_Struggle keeps the named move's id and name (Move_Effects_Generic.rb:57-58)")
    void struggleMoveWithANamedMove() {
        PbsData.Move data = new PbsData.Move();
        data.id = 165;
        data.name = "Struggle";
        data.function = "002";
        BattleMove named = new BattleMove(data);

        BattleMove move = MoveEffectRegistry.struggleMove(named);

        assertEquals(165, move.id());                    // :57 (move) ? move.id : -1
        assertEquals("Struggle", move.name());           // :58 (move) ? PBMoves.getName(@id) : ...
        assertEquals("002", move.function());            // :59 the synthesized record still wins for the rest
        assertEquals(50, move.power());                  // :60
    }

    // ==================================================================
    // 2) The function-code lookup and the pseudomove factories
    // ==================================================================

    @Test
    @DisplayName("L2: an unregistered function code gets PokeBattle_UnimplementedMove (PokeBattle_Move.rb:58)")
    void unknownFunctionFallsBackToUnimplementedMove() {
        MoveEffect fallback = MoveEffectsGeneric.PokeBattle_UnimplementedMove.INSTANCE;

        // "7FF" is not a plugin function code at all; null/"" cannot be looked up.
        assertSame(fallback, MoveEffectRegistry.of("7FF"),
                "PokeBattle_Move.rb:58 returns PokeBattle_UnimplementedMove");
        assertSame(fallback, MoveEffectRegistry.of(null),
                "a null code cannot be looked up either");
        assertSame(fallback, MoveEffectRegistry.of(""),
                "an empty code is not registered");

        // "20F" is a GAP: the Arceus section defines 200-209 and 210-215, so
        // 20A-20F have no class (__l2register.mjs's class inventory).
        assertSame(fallback, MoveEffectRegistry.of("20F"),
                "20A-20F are gaps in the plugin's class inventory");
    }

    @Test
    @DisplayName("L2: every PokeBattle_Move_XXX code is registered (PokeBattle_Move.rb:51-59)")
    void functionCodesAreRegistered() {
        assertEquals(526, MoveEffectRegistry.size(), "526 classes across the 7 movefx files");

        assertTrue(MoveEffectRegistry.of("000") instanceof MoveEffects_000_07F.PokeBattle_Move_000);
        assertTrue(MoveEffectRegistry.of("0DD") instanceof MoveEffects_0D5_0FF.PokeBattle_Move_0DD);
        assertTrue(MoveEffectRegistry.of("0F7") instanceof MoveEffects_0D5_0FF.PokeBattle_Move_0F7);
        // "1C8" is declared twice in the plugin (two reopenings) and is one class here.
        assertTrue(MoveEffectRegistry.of("1C8") instanceof MoveEffects_180_1FF.PokeBattle_Move_1C8);
        // The out-of-bounds sections (Arceus / 场地 / Pokemon_ShadowPokemon).
        assertTrue(MoveEffectRegistry.of("200") instanceof MoveEffects_Extra.PokeBattle_Move_200);
        assertTrue(MoveEffectRegistry.of("215") instanceof MoveEffects_Extra.PokeBattle_Move_215);
        assertTrue(MoveEffectRegistry.of("126") instanceof MoveEffects_Extra.PokeBattle_Move_126);
        assertTrue(MoveEffectRegistry.of("219") instanceof MoveEffects_Extra.PokeBattle_Move_219);
    }

    @Test
    @DisplayName("L2: confusion()/struggle() are singletons and do NOT shadow their function codes (decision 13)")
    void pseudomoveFactoriesAreSingletonsAndStayOutOfTheTable() {
        assertSame(MoveEffectRegistry.confusion(), MoveEffectRegistry.confusion());
        assertSame(MoveEffectRegistry.struggle(), MoveEffectRegistry.struggle());
        assertNotSame(MoveEffectRegistry.confusion(), MoveEffectRegistry.struggle());

        // PokeBattle_Confusion carries @function = "000" and PokeBattle_Struggle
        // "002" (Move_Effects_Generic.rb:27/59) but neither is constructed through
        // PokeBattle_Move.pbFromPBMove, so of("000")/of("002") are the REAL move
        // classes and the pseudomoves are only reachable through the factories.
        assertNotSame(MoveEffectRegistry.confusion(), MoveEffectRegistry.of("000"));
        assertNotSame(MoveEffectRegistry.struggle(), MoveEffectRegistry.of("002"));
        assertTrue(MoveEffectRegistry.of("000") instanceof MoveEffects_000_07F.PokeBattle_Move_000);
        assertTrue(MoveEffectRegistry.of("002") instanceof MoveEffects_000_07F.PokeBattle_Move_002);
    }

    // ==================================================================
    // 3) The honest-stub contract
    // ==================================================================

    private static void assertPending(String what, Runnable call) {
        UnsupportedOperationException error =
                assertThrows(UnsupportedOperationException.class, call::run, what);
        assertTrue(error.getMessage() != null && error.getMessage().contains("M0 待接线"),
                what + " must be an honest M0 stub, was: " + error.getMessage());
    }

    @Test
    @DisplayName("L2: the hooks with no base body throw '插件无默认实现' (task-11 decision 8)")
    void hooksWithoutABaseBodyThrow() {
        MoveEffectBase effect = new MoveEffectBase();

        for (Runnable call : new Runnable[] {
                () -> effect.pbFixedDamage(null, null, null),        // Move_Effects_Generic.rb:474
                () -> effect.pbHealAmount(null, null),               // :570
                () -> effect.pbRecoilDamage(null, null, null),       // :594
                () -> effect.pbChargingTurnMessage(null, null, null),// :536
                () -> effect.pbAttackingTurnMessage(null, null, null),// :540
                () -> effect.pbChargingTurnEffect(null, null, null), // :543
                () -> effect.pbAttackingTurnEffect(null, null, null),// :548
                () -> effect.pbProtectMessage(null, null),           // :661
        }) {
            UnsupportedOperationException error =
                    assertThrows(UnsupportedOperationException.class, call::run);
            assertTrue(error.getMessage() != null
                            && error.getMessage().contains("插件无默认实现"),
                    "message must name the plugin's missing default, was: " + error.getMessage());
        }
    }

    @Test
    @DisplayName("L2: the heavy default bodies throw 'M0 待接线' with their Ruby line range (decision 10)")
    void heavyDefaultBodiesAreHonestStubs() {
        MoveEffectBase effect = new MoveEffectBase();

        // §4 wiring: the three type-calculation bodies are transcribed now
        // (Move_Usage_Calculations.rb:14-29 / 34-71 / 73-103) - see
        // typeBodiesAreTranscribed below - so they are no longer in this list.
        // Stage 5 / 2c2: pbAccuracyCheck (:112-143), pbCalcAccuracyModifiers
        // (:145-183) and pbIsCritical? (:196-229) are transcribed now - see
        // MoveCalculationsTest - so they are no longer in this list.
        assertPending("pbCalcDamage (:252-295)",
                () -> effect.pbCalcDamage(null, null, null, 1));
        assertPending("pbCalcDamageMultipliers (:296-546)",
                () -> effect.pbCalcDamageMultipliers(null, null, null, 1, null, 0, null));
    }

    @Test
    @DisplayName("§4: pbCalcType/pbCalcTypeMod were transcribed, not left as stubs")
    void typeBodiesAreTranscribed() {
        MoveEffectBase effect = new MoveEffectBase();
        // :74 return PBTypeEffectiveness::NORMAL_EFFECTIVE if moveType<0
        // (null is the runtime's -1), which is the one path that needs no battle.
        assertEquals(PBTypeEffectiveness.NORMAL_EFFECTIVE,
                effect.pbCalcTypeMod(null, null, null, null),
                "Move_Usage_Calculations.rb:74");
    }

    @Test
    @DisplayName("L2: pbCalcAccuracyMultipliers is the plugin's dead (and broken) hook - empty body (decision 5)")
    void deadAccuracyHookDoesNothing() {
        // No call site exists (pbAccuracyCheck:126 calls pbCalcAccuracyModifiers:145)
        // and every override reads an undefined `modifiers`, so the base body is
        // deliberately empty rather than a transcription of broken code.
        new MoveEffectBase().pbCalcAccuracyMultipliers(null, null, null, new float[5]);
    }
}