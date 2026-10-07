package pokemon.runtime.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * B8-A: the WILD Pokemon's move choice, transcribed from
 * {@code PokeBattle_AI#pbChooseMoves} ({@code AI_Move:6-146}) and its wild
 * branch {@code pbRegisterMoveWild} ({@code AI_Move:153-155}).
 *
 * <p>The plugin's wild AI scores every usable move with the same 100
 * ({@code :154}), so the weighted pick at {@code :124-132} degenerates to a
 * uniform choice among the usable moves - but the weighting itself is
 * transcribed as written, because {@code :27/125-131} is what runs.</p>
 *
 * <h3>Scope (task-5)</h3>
 * <p>Only the wild branch is implemented: {@code :8} {@code wildBattler} and
 * {@code :20} {@code battleRank < 2}. A battler that fails that condition takes
 * the trainer branch ({@code :22-24 pbRegisterMoveTrainer}), which is a later
 * batch, so {@link #chooseMove} answers {@link #NOT_HANDLED} instead of
 * silently producing a different decision.</p>
 *
 * <h3>Deliberately not transcribed</h3>
 * <ul>
 *   <li>{@code :92-105} - it sits after the trainer-only {@code if} at
 *       {@code :30-91} and therefore DOES run for a wild battler, but its only
 *       effect is writing {@code $nextTarget}/{@code $nextMove}/{@code $nextQue}
 *       ({@code :100-102}). Those three globals are written in this plugin and
 *       never read anywhere (the only other mentions are their initialisation at
 *       {@code PokeBattle_AI:46-48} and three more writes at
 *       {@code PokeBattle_AI:139-141}), and {@code pbRoughType}
 *       ({@code AI_Move_Utilities:144-150}) does not cache anything on the move
 *       either. Transcribing it would mean first transcribing
 *       {@code pbMoveBaseDamage} ({@code AI_Move_Utilities:171-264}) and
 *       {@code pbRoughDamage} ({@code :266-...}), i.e. hundreds of lines of
 *       ability/item code, to compute three values nobody reads.</li>
 *   <li>{@code :133-145} - {@code PBDebug.log} plus the Deoxys form branch. The
 *       branch assigns {@code user.effects[PBEffects::DeoxysForm]} ({@code :142}),
 *       which the plugin then READS to actually change form in battle
 *       ({@code Battler_UseMove:404-408}) and which the T-key form menu writes
 *       ({@code Scene_Commands:152-171}). Neither {@code PBEffects::DeoxysForm}
 *       nor that menu exists in this runtime, so the branch is not dead code but
 *       a dependency on an unmodelled subsystem.</li>
 *   <li>{@code :11-15} ({@code opposing}) - kept out because every reader of it
 *       ({@code :41-47}, {@code :97-104}) is inside the not-transcribed regions
 *       above.</li>
 * </ul>
 */
public final class BattleAi {

    private BattleAi() {
    }

    /**
     * {@code AI_Move:120-122}: no slot can be chosen at all, so
     * {@code @battle.pbAutoChooseMove(user.index)} runs and the plugin registers
     * Struggle ({@code Battle_Action_AttacksPriority:59-67}). This runtime's
     * {@code Battle.pickMove} already answers that case with
     * {@code user.struggle(pbs)} before asking any AI, so the value is defensive.
     */
    public static final int STRUGGLE = -1;

    /**
     * {@code AI_Move:22-24}: the battler takes {@code pbRegisterMoveTrainer},
     * which this batch does not implement (it needs {@code pbGetMoveScore},
     * {@code AI_Move_EffectScores} and {@code AI_Move_Utilities}). The caller
     * must fall back to whatever it used before until that batch lands.
     */
    public static final int NOT_HANDLED = -2;

    /**
     * {@code AI_Move:8} + {@code :20}: {@code wildBattler &&
     * user.pokemon.battleRank < 2} - whether {@link #chooseMove} owns this
     * battler's decision.
     */
    public static boolean handles(Battle battle, Battler user) {
        return isWildBattler(battle, user) && battleRank(user) < 2;
    }

    /**
     * {@code AI_Move:8 wildBattler = (@battle.wildBattle? && @battle.opposes?(idxBattler))}.
     *
     * <p>{@code wildBattle?} is {@code @opponent.nil?} ({@code PokeBattle_Battle:190}),
     * i.e. this runtime's {@code trainerBattle} flag; {@code opposes?(idxBattler)}
     * with its default second argument is
     * {@code (idxBattler & 1) != (0 & 1)} ({@code PokeBattle_Battle:538-542}).</p>
     */
    public static boolean isWildBattler(Battle battle, Battler user) {
        if (battle == null || user == null) {
            return false;
        }
        return battle.wildBattle() && (user.index & 1) != 0;
    }

    /**
     * The wild Pokemon's move, transcribed from {@code pbChooseMoves}
     * ({@code AI_Move:6-146}).
     *
     * @return the chosen move SLOT (0..3), {@link #STRUGGLE} when the plugin
     *         would run {@code pbAutoChooseMove} ({@code :120-122}), or
     *         {@link #NOT_HANDLED} when the battler is not on the wild path
     */
    public static int chooseMove(Battle battle, Battler user, Random random) {
        if (!handles(battle, user)) {
            return NOT_HANDLED;
        }
        // :8-9. skill (:9) is only passed to pbRegisterMoveTrainer (:23), so the
        // wild path never needs getSkill (PokeBattle_AI:67-77).
        boolean wildBattler = isWildBattler(battle, user);
        int battleRank = battleRank(user);

        // :17 choices = []
        List<Choice> choices = new ArrayList<>();

        // :18 user.eachMoveWithIndex do |m,i| - PokeBattle_Battler:548 walks the
        //     four SLOTS and yields each one that holds a move (m && m.id != 0).
        for (int i = 0; i < Battler.MOVES_MAX; i++) {
            BattleMove move = user.moveSlot(i);
            if (move == null) {
                continue;                                      // :18 "if m && m.id != 0"
            }
            // :19 next if !@battle.pbCanChooseMove?(idxBattler,i,false)
            //     canChooseMove returns null when the slot may be used.
            if (battle.canChooseMove(user.index, i) != null) {
                continue;
            }
            // :20 if wildBattler && user.pokemon.battleRank < 2
            if (!(wildBattler && battleRank < 2)) {
                return NOT_HANDLED;                            // :22-24 trainer branch
            }
            // :21 -> :153-155 pbRegisterMoveWild: choices.push([idxMove,100,-1])
            choices.add(new Choice(i, 100, -1));
        }

        // :27 totalScore = choices.inject(0) { |sum, c| sum + c[1] }
        // :28 maxScore is read only by the trainer block (:30-91), so it is not
        //     computed here.
        int totalScore = 0;
        for (Choice choice : choices) {
            totalScore += choice.score;
        }

        // :92-105 not transcribed - see the class comment.

        // :106-107 "# If there are no calculated choices, pick one at random"
        if (choices.isEmpty()) {
            // :108-114 is skipped: pbEnemyShouldWithdrawEx? answers false at once
            //          in a wild battle (AI_Switch:107 "return false if
            //          @battle.wildBattle?"), so the wild path never switches here.
            // :116-119 the re-scan. It uses the very predicate :19 used, so for a
            //          wild battler it cannot add anything; it is kept because it
            //          is part of the transcribed region and because the trainer
            //          path (where choices can be empty while slots are usable)
            //          reaches it in the next batch.
            for (int i = 0; i < Battler.MOVES_MAX; i++) {
                BattleMove move = user.moveSlot(i);            // :116 eachMoveWithIndex
                if (move == null) {
                    continue;
                }
                if (battle.canChooseMove(user.index, i) != null) {
                    continue;                                  // :117
                }
                choices.add(new Choice(i, 100, -1));           // :118
            }
            // :120-122 if choices.length == 0 -> @battle.pbAutoChooseMove(user.index)
            if (choices.isEmpty()) {
                // This early return is also what keeps Java's Random out of the
                // plugin's rand(0) case: totalScore is 0 here (:27 injects an empty
                // choices to 0) and :125 would call pbAIRandom(0). Ruby returns a
                // Float for that, while java.util.Random.nextInt(0) throws, so the
                // empty list must never reach the weighted pick below.
                return STRUGGLE;
            }
            // totalScore is NOT recomputed here - :27 already ran, exactly like the
            // plugin. The stale 0 makes :125 use pbAIRandom(0), whose Float keeps
            // the loop below from ever reaching >= 0, i.e. the FIRST choice wins.
        }

        // :124-132 "Randomly choose a move from the choices and register it"
        // A non-empty wild choice list always sums to choices.size()*100 (:154),
        // so totalScore is never 0 here and nextInt is never asked for 0.
        double randNum = aiRandom(totalScore, random);         // :125
        for (Choice choice : choices) {                        // :126
            randNum -= choice.score;                           // :127
            if (randNum >= 0) {                                // :128 next if randNum>=0
                continue;
            }
            // :129 @battle.pbRegisterMove(idxBattler,c[0],false) - the caller
            //      registers the returned slot.
            // :130 @battle.pbRegisterTarget(idxBattler,c[2]) if c[2]>=0 - every wild
            //      choice carries target -1 (:154), so nothing is registered.
            return choice.idxMove;
        }
        // Ruby falls out of the loop with nothing registered. A wild choice list
        // always sums to choices.size()*100 (:154) and pbAIRandom returns
        // 0...totalScore, so the last entry always goes negative (:127-128) and
        // this line is unreachable on the wild path; it maps Ruby's "registered
        // nothing" at the same value as :121's Struggle so the caller has a
        // defined answer.
        return STRUGGLE;
    }

    /**
     * {@code pbAIRandom} ({@code PokeBattle_AI:83}):
     * {@code def pbAIRandom(x); return rand(x); end}.
     *
     * <p>Ruby's {@code Kernel#rand(Integer)} returns an Integer in
     * <b>{@code 0...x}</b> - 0 inclusive, {@code x} EXCLUSIVE - and {@code rand(0)}
     * returns a <b>Float</b> in {@code [0.0, 1.0)} instead of an Integer. The
     * {@code double} return keeps both cases (integers are exact), which is what
     * {@code randNum} at {@code AI_Move:125-127} relies on.</p>
     *
     * <p>The {@code x == 0} branch is a Java trap, not a style choice:
     * {@code Random.nextInt(0)} throws {@code IllegalArgumentException} where
     * Ruby's {@code rand(0)} quietly returns a Float. {@link #chooseMove} never
     * reaches it from an empty choice list (that goes to {@code :121}'s Struggle
     * first), but the helper must not turn the plugin's {@code rand(0)} into an
     * exception.</p>
     *
     * @throws IllegalArgumentException for a negative argument, which Ruby's
     *         {@code rand} rejects as well; no plugin caller produces one
     */
    static double aiRandom(int x, Random random) {
        if (x > 0) {
            return random.nextInt(x);                           // 0...x
        }
        if (x == 0) {
            return random.nextDouble();                         // rand(0) -> [0.0,1.0)
        }
        throw new IllegalArgumentException("pbAIRandom(" + x + ")");
    }

    /** {@code user.pokemon.battleRank} ({@code PokeBattle_BOSS:16-18}, default 1). */
    private static int battleRank(Battler user) {
        return user.pokemon == null ? 1 : user.pokemon.battleRank;
    }

    /** {@code choices}' entries: {@code [idxMove, score, target]} ({@code AI_Move:118/154}). */
    private static final class Choice {
        final int idxMove;
        final int score;
        final int target;

        Choice(int idxMove, int score, int target) {
            this.idxMove = idxMove;
            this.score = score;
            this.target = target;
        }
    }
}
