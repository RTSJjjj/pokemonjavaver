package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;

import java.util.Random;

/**
 * Stage 3 / P2: a headless, single-battle engine (singles, one active Pokemon
 * per side). It runs whole turns without any UI so the event layer can start a
 * battle and get a result; the battle scene (P2d) drives the same engine one
 * action at a time. Status moves, abilities, items, switching and catching are
 * intentionally out of the v1 scope (P2/P5 add them).
 */
public final class Battle {

    /** Chooses a move for one battler; the default is the simple AI. */
    public interface Controller {
        int chooseMove(Battler user, Battler foe, Array<BattleMove> moves);
    }

    private final PbsData pbs;
    private final Random random;
    private final Controller playerController;
    private final Array<Battler> playerParty = new Array<>();
    private final Array<Battler> foeParty = new Array<>();
    private int turns;

    /**
     * {@code @party1}/ {@code @party2}'s field slot: which party entry each side
     * currently has on the field. {@code pbSetUpSides}
     * (Battle_StartAndEnd:116-189) puts the first able Pokemon of each side in
     * its slot and {@code pbReplace} (:309-318) moves the slot to the
     * replacement. -1 = that side has nothing on the field.
     */
    private int playerField = -1;
    private int foeField = -1;

    // --- Mega Evolution / ZA mode (Mega evolution + ZA模式) ---
    /** ZA mode ($PokemonSystem.battle_rule == 1): a super-energy system. */
    public boolean zaMode;
    public static final int ZA_MAX_ENERGY = 3;
    private final int[] zaEnergy = {ZA_MAX_ENERGY, ZA_MAX_ENERGY};
    private final boolean[] zaMegaActive = {false, false};
    private final Battler[] zaMegaPkmn = {null, null};
    /** Classic mode: one Mega per side per battle (@megaEvolution side/owner). */
    private final boolean[] megaUsed = {false, false};

    public Battle(PbsData pbs, Random random, Controller playerController) {
        this.pbs = pbs;
        this.random = random;
        this.playerController = playerController;
    }

    /**
     * {@code @canRun} (PField_Battles:101, {@code setBattleRule("cannotRun")}):
     * a wild battle the player may not flee from. {@code pbCanRun?}
     * (Battle_Action_Running:5-8) also refuses when the rule is off.
     */
    private boolean canRun = true;

    public boolean canRun() {
        return canRun;
    }

    public Battle setCanRun(boolean value) {
        this.canRun = value;
        return this;
    }

    /**
     * The unmodified Speed stat of a battler. {@code pbRun} says so explicitly:
     * "NOTE: Not pbSpeed, because using unmodified Speed"
     * (Battle_Action_Running:134-141), so stat stages and paralysis do not
     * apply to the escape chance.
     */
    public int rawSpeed(Battler battler) {
        return battler == null || battler.pokemon == null ? 0
                : battler.pokemon.stat(PokemonStats.SPEED);
    }

    public Battle addPlayer(Pokemon pokemon) {
        if (pokemon != null) {
            Battler battler = new Battler(pokemon, false);
            battler.trainerBattle = trainerBattle;
            playerParty.add(battler);
            playerField = firstAble(playerParty);                 // pbSetUpSides:179
            refreshFieldIndices();
        }
        return this;
    }

    public Battle addFoe(Pokemon pokemon) {
        if (pokemon != null) {
            Battler battler = new Battler(pokemon, true);
            battler.trainerBattle = trainerBattle;
            foeParty.add(battler);
            foeField = firstAble(foeParty);                       // pbSetUpSides:179
            refreshFieldIndices();
        }
        return this;
    }

    /**
     * {@code eachInTeam} + {@code next if !pkmn.able?} (Battle_StartAndEnd:179):
     * the first party entry that can fight. {@code able?} is
     * {@code !egg? && hp>0} (PokeBattle_Pokemon:735-737).
     */
    private static int firstAble(Array<Battler> party) {
        for (int i = 0; i < party.size; i++) {
            Battler battler = party.get(i);
            if (battler != null && !battler.fainted() && !battler.pokemon.egg) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@code @battlers[i].index} (PokeBattle_Battler's {@code @index}): the field
     * slot of the battler currently on it. {@code pbSetUpSides}
     * (Battle_StartAndEnd:116-189) gives the player's side slot 0 and the
     * opposing side slot 1 in a single battle; every other party member is
     * benched. The runtime only fields one battler per side (multi battles are
     * still outstanding), so a wild battle's second Pokemon - which
     * {@code pbSetUpSides:121-127} would put in slot 3 - cannot occur here.
     */
    public void refreshFieldIndices() {
        for (Battler battler : playerParty) {
            battler.index = -1;
            battler.battle = this;                                // M0: @battle (PokeBattle_Battler:216)
            battler.field = field;                                // M0: battle.field (PokeBattle_ActiveField)
            battler.pbs = pbs;                                    // M0: PbsData for PBTypes/PBItems name lookups
        }
        for (Battler battler : foeParty) {
            battler.index = -1;
            battler.battle = this;
            battler.field = field;
            battler.pbs = pbs;
        }
        if (playerField >= 0) {
            playerParty.get(playerField).index = 0;               // :180 2*slot+side
        }
        if (foeField >= 0) {
            foeParty.get(foeField).index = 1;
        }
    }

    public int turns() {
        return turns;
    }

    /** Advance exactly one turn; null means both sides can still fight. */
    public BattleResult step() {
        BattleResult result = result();
        if (result != null) return result;
        turns++;
        roundMessages.clear();
        roundEvents.clear();
        runTurn(player(), foe());
        return result();
    }
    /**
     * {@code wildBattle?} (PokeBattle_Battle:190): {@code @opponent.nil?}. The
     * public {@link #trainerBattle} field is its negative, offered as a method so
     * read-only callers (the AI) do not touch the field.
     */
    public boolean wildBattle() {
        return !trainerBattle;
    }

    /** {@code @battlers[0]} via {@code @party1order} (PokeBattle_Battle:315-321). */
    public Battler player() {
        return playerField < 0 || playerField >= playerParty.size ? null : playerParty.get(playerField);
    }
    /** The player's party slot on the field ({@code @party1order[0]}). */
    public int playerFieldIndex() {
        return playerField;
    }
    /** The opposing side's party slot on the field. */
    public int foeFieldIndex() {
        return foeField;
    }
    /** {@code @battlers[1]}. */
    public Battler foe() {
        return foeField < 0 || foeField >= foeParty.size ? null : foeParty.get(foeField);
    }
    /** {@code @battlers[idxBattler]} for the two singles slots. */
    public Battler battlerAt(int idxBattler) {
        return (idxBattler & 1) == 0 ? player() : foe();
    }
    /** {@code pbParty(idxBattler)} (PokeBattle_Battle:307-309). */
    public Array<Battler> partyOf(int idxBattler) {
        return (idxBattler & 1) == 0 ? playerParty : foeParty;
    }
    /** {@code pbParty(side)}: side 0 = player, 1 = opposing. */
    public Array<Battler> partyBySide(int side) {
        return side == 0 ? playerParty : foeParty;
    }

    /**
     * {@code pbAllFainted?(idxBattler)} (PokeBattle_Battle:353-355) =
     * {@code pbAbleCount(idxBattler)==0}, counting {@code pkmn.able?} over the
     * whole party of that side (:333-338).
     */
    public boolean allFainted(int side) {
        for (Battler battler : partyBySide(side)) {
            if (battler != null && !battler.fainted() && !battler.pokemon.egg) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@code pbJudge} (Battle_StartAndEnd:587-594): the end-of-battle decision
     * from who still has an able Pokemon. 1 = win, 2 = loss, 5 = draw, 0 = the
     * battle continues.
     */
    public int judge() {
        boolean fainted1 = allFainted(0);
        boolean fainted2 = allFainted(1);
        if (fainted1 && fainted2) return 5;                        // :590
        if (fainted1) return 2;                                    // :591
        if (fainted2) return 1;                                    // :592
        return 0;
    }

    public BattleResult result() {
        int decision = judge();
        if (decision == 2) return finish(BattleResult.Outcome.LOSS);
        if (decision == 1) return finish(BattleResult.Outcome.WIN);
        if (decision == 5) return finish(BattleResult.Outcome.DRAW);
        return null;
    }
    /** Items, switching and failed escapes spend the player's action. */
    public BattleResult foeTurn() {
        if (result() != null) return result();
        turns++;
        roundMessages.clear();
        roundEvents.clear();
        Battler foe = foe();
        Battler player = player();
        if (foe != null && player != null) {
            execute(foe, player, pickMove(foe, player, null));
        }
        endOfTurn();
        refreshFieldIndices();
        return result();
    }
    /**
     * {@code pbReplace} (Battle_Action_Switching:309-320) without the send-out:
     * the incoming Pokemon takes the field slot.
     *
     * <p>{@code :313 pbInitialize(party[idxParty],idxParty,batonPass)} swaps the
     * Pokemon inside the slot's battler and resets its effects;
     * {@code :315-317} swaps the pair in {@code partyOrder} (the runtime keeps
     * no party order). The incoming battler's reset is
     * {@link Battler#resetForSwitchIn()}.</p>
     */
    public boolean replace(int idxBattler, int idxParty) {
        Array<Battler> party = partyOf(idxBattler);
        if (party == null || idxParty < 0 || idxParty >= party.size) {
            return false;
        }
        Battler incoming = party.get(idxParty);
        if (incoming == null) {
            return false;
        }
        incoming.resetForSwitchIn();                               // :313 pbInitialize
        if ((idxBattler & 1) == 0) {
            playerField = idxParty;
        } else {
            foeField = idxParty;
        }
        refreshFieldIndices();
        return true;
    }

    // ==================================================================
    // Battle_Action_Switching (209-332): choosing and replacing battlers
    // ==================================================================

    /**
     * {@code @choices[i]} (Battle_Phase_Command:6-11). Only the
     * {@code :None}/:SwitchOut distinction is modelled - the runtime executes a
     * chosen move immediately instead of storing it for the attack phase.
     */
    private final boolean[] switchChoice = new boolean[2];
    private final int[] switchChoiceParty = { -1, -1 };

    /** {@code @choices[idxBattler][0] == :SwitchOut} (:52/:176). */
    public boolean choiceIsSwitch(int idxBattler) {
        return idxBattler >= 0 && idxBattler < 2 && switchChoice[idxBattler];
    }

    /** {@code @choices[idxBattler][1]} (:53/:124-125). */
    public int choiceSwitchParty(int idxBattler) {
        return idxBattler >= 0 && idxBattler < 2 ? switchChoiceParty[idxBattler] : -1;
    }

    /** {@code pbClearChoice} (Battle_Phase_Command:5-11). */
    public void clearChoice(int idxBattler) {
        if (idxBattler < 0 || idxBattler >= 2) {
            return;
        }
        switchChoice[idxBattler] = false;
        switchChoiceParty[idxBattler] = -1;
        chosenSlot[idxBattler] = -1;                               // :8 @choices[idxBattler][1]
        chosenMove[idxBattler] = null;                             // :9 @choices[idxBattler][2] = nil
    }

    /** {@code pbCancelChoice} (Battle_Phase_Command:13-23): Mega is not modelled. */
    public void cancelChoice(int idxBattler) {
        clearChoice(idxBattler);
    }

    /**
     * {@code pbCanSwitchLax?} (Battle_Action_Switching:9-34).
     *
     * @return null when the party entry can come in; otherwise the
     *         {@code partyScene.pbDisplay} line the plugin would show, or ""
     *         for the branches whose display the plugin also guards with
     *         {@code if partyScene} (i.e. they show nothing outside the party
     *         screen).
     */
    public String canSwitchLax(int idxBattler, int idxParty) {
        if (idxParty < 0) {
            return null;                                          // :10
        }
        Array<Battler> party = partyOf(idxBattler);
        if (party == null || idxParty >= party.size) {
            return "";                                            // :12-13
        }
        Battler candidate = party.get(idxParty);
        if (candidate == null) {
            return "";                                            // :13
        }
        if (candidate.pokemon.egg) {
            return "蛋不能进行战斗！";                              // :14-17
        }
        // :18-23 !pbIsOwner?(idxBattler,idxParty): a single trainer owns the
        // whole side in this runtime (pbGetOwnerIndexFromPartyIndex always
        // returns 0), so the check cannot fail.
        if (candidate.fainted()) {
            return candidate.name() + "已经无法战斗了！";            // :24-28
        }
        if (candidate.index == idxBattler) {
            return candidate.name() + "已经参与战斗了！";            // :29-33 pbFindBattler
        }
        return null;
    }

    /**
     * {@code pbCanSwitch?} (Battle_Action_Switching:41-113). The
     * ability/item/trapping branches (:55-107) need the ability and PBEffects
     * systems, neither of which this runtime has; this project's data reaches
     * the Ghost branch (:67) and otherwise allows the switch.
     */
    public String canSwitch(int idxBattler, int idxParty) {
        String lax = canSwitchLax(idxBattler, idxParty);           // :43
        if (lax != null) {
            return lax;
        }
        // :46-51 another battler on the same side already chose this party
        // entry. Singles fields one battler per side, so there is no other
        // chooser.
        Battler battler = battlerAt(idxBattler);
        if (battler == null) {
            return "";
        }
        if (battler.fainted()) {
            return null;                                          // :54
        }
        // :56-65 triggerCertainSwitchingUserAbility/Item - no abilities/items.
        if (NEWEST_BATTLE_MECHANICS && battler.hasType("GHOST")) {
            return null;                                          // :67
        }
        // :69-107 Octolock / Jaw Lock / Trapping / Mean Look / Ingrain /
        // NoRetreat / CurseNail / FierceKilling / FairyLock / trapping
        // abilities and items: none of these effects exist in this runtime.
        // :108-111 Commander is a multi-battler mechanic.
        return null;
    }

    /** {@code pbCanChooseNonActive?} (Battle_Action_Switching:115-120). */
    public boolean canChooseNonActive(int idxBattler) {
        Array<Battler> party = partyOf(idxBattler);
        if (party == null) {
            return false;
        }
        for (int i = 0; i < party.size; i++) {                     // :116-118
            if (canSwitchLax(idxBattler, i) == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code pbRegisterSwitch} (Battle_Action_Switching:122-128): a switch is
     * the battler's action for the round, which is why a second switch cannot be
     * chosen in the same round.
     */
    public boolean registerSwitch(int idxBattler, int idxParty) {
        if (canSwitch(idxBattler, idxParty) != null) {
            return false;                                         // :123
        }
        if (idxBattler < 0 || idxBattler >= 2) {
            return false;
        }
        switchChoice[idxBattler] = true;                           // :124
        switchChoiceParty[idxBattler] = idxParty;                  // :125
        return true;
    }

    /**
     * {@code pbGetReplacementPokemonIndex(idxBattler,false)}
     * (Battle_Action_Switching:241-253): the owner chooses through
     * {@code pbSwitchInBetween}. For the player that is the party screen, which
     * this method cannot show - it returns {@link #OWNER_CHOOSES} and the battle
     * screen asks. A trainer's Pokemon goes straight to the AI
     * ({@code :157 pbDefaultChooseNewEnemy}).
     */
    public static final int OWNER_CHOOSES = -2;

    public int getReplacementPokemonIndex(int idxBattler) {
        if ((idxBattler & 1) == 0) {                               // :156 pbOwnedByPlayer?
            return OWNER_CHOOSES;                                  // pbPartyScreen
        }
        return defaultChooseNewEnemy(idxBattler);                  // :157
    }

    /**
     * {@code pbDefaultChooseNewEnemy} (AI_Switch:179-186) and
     * {@code pbChooseBestNewEnemy} (:188-227).
     *
     * @return the party index the AI sends out, or -1 when nothing can come in.
     */
    public int defaultChooseNewEnemy(int idxBattler) {
        Array<Battler> party = partyOf(idxBattler);
        Array<Integer> enemies = new Array<>();                    // :180-183
        for (int i = 0; i < party.size; i++) {
            if (canSwitchLax(idxBattler, i) == null) {
                enemies.add(i);
            }
        }
        if (enemies.size == 0) {
            return -1;                                             // :184
        }
        int best = -1;                                             // :190-191
        int bestSum = 0;
        for (int idx : enemies) {                                  // :193
            Battler candidate = party.get(idx);
            int sum = 0;
            for (Pokemon.MoveSlot slot : candidate.pokemon.moves) {  // :196-205
                PbsData.Move move = slot.move;
                if (move == null) {                                // :197 next if m.id==0
                    continue;
                }
                if (move.power <= 0) {                             // :199 MOVE_BASE_DAMAGE==0
                    continue;
                }
                for (Battler b : opposingBattlers(idxBattler)) {     // :200-204
                    sum += combinedEffectiveness(move.type, b);
                }
            }
            // :206-220 the "om.calcType" half needs PokeBattle_Move#pbCalcType
            // (the -ate abilities and type-changing effects) which this runtime
            // has no ability system for; reported to the user, so only the
            // raw-type part above is scored.
            if (best == -1 || sum > bestSum) {                      // :221-224
                best = idx;
                bestSum = sum;
            }
        }
        return best;
    }

    /** {@code battler.eachOpposing} for the two singles slots. */
    private Array<Battler> opposingBattlers(int idxBattler) {
        Array<Battler> out = new Array<>();
        Battler opposing = battlerAt(idxBattler ^ 1);
        if (opposing != null && !opposing.fainted()) {
            out.add(opposing);
        }
        return out;
    }

    /**
     * {@code PBTypes.getCombinedEffectiveness(type,type1,type2,type3)}
     * (AI_Switch:203): the plugin's integer scale, where
     * {@code NORMAL_EFFECTIVE} is 4.
     */
    private int combinedEffectiveness(String type, Battler target) {
        if (pbs == null || type == null) {
            return 4;
        }
        return Math.round(pbs.effectiveness(type, target.types()) * 4f);
    }

    /**
     * One fainted battler {@code pbEORSwitch} (Battle_Action_Switching:165-239)
     * has to replace, with the branch that decides who picks the replacement.
     */
    public static final class Replacement {
        public int idxBattler;
        /** {@code pbOwnedByPlayer?(idxBattler)}. */
        public boolean playerSide;
        /** Trainer battle: the owner picks through the party screen (:205-208). */
        public boolean ownerChooses;
        /** Wild battle: {@code pbDisplayConfirm("要更换宝可梦吗？")} decides (:221-226). */
        public boolean askConfirm;
        /** {@code :218-220} bossBattle (a foe with {@code battleRank>1}). */
        public boolean bossBattle;
    }

    /**
     * The replacement pass of {@code pbEORSwitch} (Battle_Action_Switching:165-239):
     * every fainted battler that still has a Pokemon to send out, in field-slot
     * order. The Ruby runs the whole replacement itself, including the party
     * screen and the confirmation prompts; this runtime's screen owns that UI,
     * so the engine only reports what has to happen.
     *
     * @param favorDraws {@code pbEORSwitch}'s argument (:166-167): false when
     *                   called from the end of a round.
     */
    public Array<Replacement> eorSwitchPlan(boolean favorDraws) {
        Array<Replacement> plan = new Array<>();
        int decision = judge();                                    // :168 pbJudge
        if (decision != 0 && !favorDraws) {
            return plan;                                           // :166
        }
        if (decision == 5 && favorDraws) {
            return plan;                                           // :167
        }
        if (decision != 0) {
            return plan;                                           // :169
        }
        for (int idxBattler = 0; idxBattler < 2; idxBattler++) {    // :174 @battlers.each
            Battler battler = battlerAt(idxBattler);
            if (battler == null || !battler.fainted()) {
                continue;                                          // :175-176
            }
            if (!canChooseNonActive(idxBattler)) {
                continue;                                          // :177
            }
            Replacement replacement = new Replacement();
            replacement.idxBattler = idxBattler;
            replacement.playerSide = (idxBattler & 1) == 0;
            if (!replacement.playerSide) {                          // :178 opponent
                if (!trainerBattle) {
                    continue;                                      // :179 wild Pokemon can't switch
                }
                replacement.ownerChooses = false;                   // :180 pbSwitchInBetween
            } else if (trainerBattle) {                             // :205
                replacement.ownerChooses = true;                    // :206-207
            } else {                                               // :209 wild battle
                for (Battler opposing : opposingBattlers(idxBattler)) {   // :211-216
                    if (opposing.pokemon != null && opposing.pokemon.battleRank > 1) {
                        replacement.bossBattle = true;
                        break;
                    }
                }
                replacement.askConfirm = !replacement.bossBattle;   // :218-226
            }
            plan.add(replacement);
        }
        return plan;
    }

    public Array<Battler> playerParty() {
        return playerParty;
    }

    public Array<Battler> foeParty() {
        return foeParty;
    }

    /**
     * Runs the battle until one side is out of able Pokemon or {@code maxTurns}
     * is reached; HP is written back onto the Pokemon either way. A fainted
     * battler is replaced by the next able one like {@code pbEORSwitch} does,
     * but without any of its prompts (there is no UI here).
     */
    public BattleResult run(int maxTurns) {
        while (turns < maxTurns) {
            BattleResult result = result();
            if (result != null) {
                return result;
            }
            turns++;
            roundEvents.clear();       // no scene plays them in a headless battle
            runTurn(player(), foe());
            headlessEorSwitch();
        }
        return finish(BattleResult.Outcome.ESCAPE);
    }

    /** {@code pbEORSwitch} (Battle_Action_Switching:165-239) with no UI. */
    private void headlessEorSwitch() {
        if (judge() != 0) {                                        // :168-169
            return;
        }
        for (int side = 0; side < 2; side++) {                     // :174 @battlers.each
            Array<Battler> party = partyBySide(side);
            int field = side == 0 ? playerField : foeField;
            if (field < 0 || !party.get(field).fainted()) {
                continue;                                          // :175-176
            }
            // :177 pbCanChooseNonActive? + :180/:206 pbGetReplacementPokemonIndex
            // (the AI picks; the player would be prompted).
            int next = firstAble(party);
            if (next < 0) {
                continue;
            }
            if (side == 0) {
                playerField = next;
            } else {
                // :179 a wild Pokemon never switches out, so a wild foe's slot
                // stays empty and the battle is judged as won.
                if (!trainerBattle) {
                    continue;
                }
                foeField = next;
            }
        }
        refreshFieldIndices();
    }

    private void runTurn(Battler player, Battler foe) {
        BattleMove playerMove = pickMove(player, foe, playerController);
        BattleMove foeMove = pickMove(foe, player, null);
        int playerPriority = playerMove == null ? 0 : playerMove.priority();
        int foePriority = foeMove == null ? 0 : foeMove.priority();
        boolean playerFirst;
        if (playerPriority != foePriority) {
            playerFirst = playerPriority > foePriority;
        } else {
            playerFirst = player.speed() > foe.speed()
                    || (player.speed() == foe.speed() && random.nextBoolean());
        }
        Battler first = playerFirst ? player : foe;
        Battler second = playerFirst ? foe : player;
        BattleMove firstMove = playerFirst ? playerMove : foeMove;
        BattleMove secondMove = playerFirst ? foeMove : playerMove;

        execute(first, second, firstMove);
        if (!first.fainted() && !second.fainted()) {
            execute(second, first, secondMove);
        }
        // Battle_StartAndEnd:381-386: pbBattleLoop breaks before
        // pbEndOfRoundPhase when a move decided the battle, so the end of round
        // (its damage and its counter) does not run.
        if (judge() == 0) {
            endOfTurn();
        } else {
            endOfRoundMessages.clear();
        }
    }

    /**
     * {@code pbIsCritical?} (Move_Usage_Calculations:194-229) with this
     * project's settings: {@code NEWEST_BATTLE_MECHANICS} picks the
     * {@code [24,8,2,1]} ratio table (1/24 normally), a high-critical-rate move
     * and Focus Energy each add a step, and ability / item / Lucky Chant
     * modifiers are not modelled yet.
     */
    public boolean isCritical(Battler attacker, Battler defender, BattleMove move,
                              MoveEffects.Effect fx) {        if (fx.alwaysCrit) {
            return true;                                    // pbCritialOverride == 1
        }
        int[] ratios = NEWEST_BATTLE_MECHANICS
                ? new int[] {24, 8, 2, 1} : new int[] {16, 8, 4, 3, 2};   // :198
        int c = 0;                                          // :199
        if (hasHighCriticalRate(move)) {
            c += 1;                                         // :223
        }
        if (attacker.focusEnergy) {
            c += 1;                                         // :224
        }
        c = Math.min(c, ratios.length - 1);                 // :226
        return random.nextInt(ratios[c]) == 0;              // :228
    }

    /** {@code highCriticalRate?} (PokeBattle_Move:120): the moves.txt "h" flag. */
    private static boolean hasHighCriticalRate(BattleMove move) {
        String flags = move.flags();
        return flags != null && flags.indexOf('h') >= 0;
    }

    /**
     * {@code pbFindTargets} (Battler_UseMove_Targeting:35-96) on the singles
     * field: the list a move's animation is shown for ({@code targets[0]} is the
     * animation's target, Scene_Animations:514). Only the target types that add
     * a battler are listed; every other type reaches the {@code else} branch
     * (:87-90, {@code move.pbAddTarget} adds nothing) and leaves the list empty.
     */
    private Array<Battler> animationTargets(BattleMove move, Battler user, Battler foe) {
        Array<Battler> targets = new Array<>();
        if (move.target() == null) {
            return targets;                                          // a synthetic move (Struggle) has no PBS target
        }
        int targeting = PBTargets.fromName(move.target());
        switch (targeting) {
            case PBTargets.NearFoe:                                  // :56
            case PBTargets.NearOther:
            case PBTargets.Other:                                    // :71 (the foe is the only other battler)
            case PBTargets.AllNearFoes:                              // :65
            case PBTargets.RandomNearFoe:                            // :67
            case PBTargets.AllNearOthers:                            // :69
            case PBTargets.AllFoes:                                  // :83
                targets.add(foe);
                break;
            case PBTargets.UserOrNearAlly:                           // :51 no ally -> :54 the user
            case PBTargets.UserAndAllies:                            // :80
                targets.add(user);
                break;
            case PBTargets.AllBattlers:                              // :85 eachBattler: index order
                if (user.index < foe.index) {
                    targets.add(user);
                    targets.add(foe);
                } else {
                    targets.add(foe);
                    targets.add(user);
                }
                break;
            default:
                break;
        }
        return targets;
    }

    /**
     * {@code pbUseMove} (Battler_UseMove:170-622) for the one-target singles
     * field, emitting the round's events in the plugin's order (see
     * {@link #roundEvents}): each hit is its own {@code pbProcessMoveHit}
     * (:458-482 -> :627-809).
     */
    private void execute(Battler attacker, Battler defender, BattleMove move) {        if (attacker.fainted() || defender.fainted() || move == null) {
            return;
        }
        if (!canAct(attacker)) {
            return;
        }
        consumePp(attacker, move);
        // Battler_UseMove:305 move.pbDisplayUseMessage(self) -> Move_Usage:24
        // pbDisplayBrief("{1}使用{2}！") for whichever side is acting.
        pbDisplayBrief(attacker.thisName() + "使用" + move.name() + "！");
        MoveEffects.Effect fx = MoveEffects.of(move.function());
        // Battler_UseMove_SuccessChecks:529-533: a damaging move whose type cannot
        // affect the target prints "这不能影响{1}……" and deals nothing
        // (damageState.unaffected = true, so Move_Usage:269 skips it entirely).
        float typeMod = pbs == null ? 1f : pbs.effectiveness(move.type(), defender.types());
        if (!move.statusMove() && typeMod <= 0f) {
            pbDisplay("这不能影响" + defender.thisName() + "……");                 // :531
            return;
        }
        // ↓↓↓ Stage 4 §4 Q1 wiring: Battler_UseMove_SuccessChecks:567-587
        //     "Immunity to powder-based moves" - a Grass-type (or Overcoat / Safety
        //     Goggles) target blocks powder moves such as Stun Spore. The helper had
        //     been transcribed but never called, so the rule did not exist in game.
        //     (insert-only; the rest of the success-check phase is queued in
        //     docs/stage4-wiring-queue.md Q1b/Q2)
        if (!BattleSuccessChecks.pbSuccessCheckAgainstTarget(this, move, attacker, defender)) {
            return;
        }
        // ↑↑↑ end of inserted wiring
        if (move.statusMove() || move.power() <= 0) {
            // :688 move.pbShowAnimation(move.id,user,targets,hitNum): pbProcessMoveHit
            // shows the animation for a status move as well, before its effect.
            animation(move, attacker, animationTargets(move, attacker, defender), 0);
            // A status move's effect applies directly (PokeBattle_Move#pbEffect...).
            if (!fx.nothing) {
                applyEffect(attacker, defender, move, fx, true);
            }
            if (fx.healHalf > 0) {
                attacker.hp = Math.min(attacker.maxHp(), attacker.hp + Math.max(1, attacker.maxHp() / 2));
                attacker.syncHp();
            }
            // ↓↓↓ Stage 4 §4 wiring: Battler_UseMove:543-544 also runs for a
            //     status move (the call sits outside the damaging branch in Ruby).
            //     (insert-only)
            BattlerHitEffects.pbEffectsAfterMove(this, attacker,
                    animationTargets(move, attacker, defender), move, 0);
            // ↑↑↑ end of inserted wiring
            return;
        }
        // Battler_UseMove:640-669: the accuracy check runs on hit 0 only
        // (successCheckPerHit? is false for every function code here).
        if (!accuracyCheck(attacker, defender, move, fx)) {
            // :654 pbMissMessage -> Battler_UseMove_SuccessChecks:659
            pbDisplay(attacker.thisName() + "的攻击没有命中！");
            return;
        }
        // Move_Usage_Calculations:273-277 resistant?/superEffective?(typeMod):
        // the damage animation's effectiveness, 1 resisted / 2 super / 0 normal.
        int effectiveness = typeMod < 1f ? 1 : (typeMod > 1f ? 2 : 0);
        // PokeBattle_Move:101 multiHitMove? - true for the multi-hit function
        // codes (0BD/0C0 here, Move_Effects_080-0FF:2018-2040).
        boolean multiHitMove = fx.multiMax > 1;
        int hits = rollHits(fx);                                   // :455 numHits = move.pbNumHits
        int total = 0;
        int realNumHits = 0;                                       // :457
        for (int i = 0; i < hits; i++) {                           // :458
            if (attacker.fainted()) {
                break;                                             // :628 return false -> :471 break
            }
            // Move_Usage_Calculations:263 damageState.critical = pbIsCritical?,
            // inside pbCalcDamage (:682), i.e. once per hit.
            boolean critical = isCritical(attacker, defender, move, fx);
            int damage = fx.ohko ? defender.hp
                    : DamageCalc.compute(attacker, defender, move, pbs, random, critical);
            if (damage <= 0) {
                break;
            }
            // :688 move.pbShowAnimation(move.id,user,targets,hitNum) -> Move_Usage:67-74
            // @battle.pbAnimation(id,user,targets,hitNum) (ParentalBond is not modelled).
            animation(move, attacker, animationTargets(move, attacker, defender), i);
            int hpBefore = defender.hp;
            // ↓↓↓ Stage 4 §4 wiring: fill damageState. The legacy path kept the
            //     damage on locals only, so every handler body reading
            //     damageState.calcDamage/hpLost/initialHP saw 0.
            //     (:263 damageState.critical inside pbCalcDamage; Move_Usage:252
            //      initialHP = the HP before the hit.) (insert-only)
            defender.damageState.calcDamage = damage;
            defender.damageState.critical = critical;
            if (i == 0) {
                defender.damageState.initialHP = hpBefore;
            }
            // ↑↑↑ end of inserted wiring
            defender.hp = Math.max(0, defender.hp - damage);       // :716 pbInflictHPDamage (Move_Usage:257)
            defender.syncHp();
            int hpLost = hpBefore - defender.hp;
            // ↓↓↓ Stage 4 §4 wiring: the two per-hit damage totals. (insert-only)
            defender.damageState.hpLost = hpLost;
            defender.damageState.totalHPLost += hpLost;
            // ↑↑↑ end of inserted wiring
            total += hpLost;
            // :719 move.pbAnimateHitAndHPLost(user,targets)
            pbAnimateHitAndHPLost(attacker, defender, hpLost, effectiveness);
            // :723 user.pbFaint if user.fainted?
            pbFaint(attacker);
            // :728 move.pbHitEffectivenessMessages(user,b,targets.length)
            pbHitEffectivenessMessages(attacker, defender, critical, effectiveness, multiHitMove);
            // :753 @battle.pbPriority(true).each { |b| b.pbFaint if b && b.fainted? }
            for (Battler b : fieldedBySpeed()) {
                pbFaint(b);
            }
            // :765-773 the additional effect, rolled for every hit that dealt damage.
            applyEffect(attacker, defender, move, fx, false);
            // ↓↓↓ Stage 4 §4 wiring: Battler_UseMove:739-742
            //     targets.each { |b| next if b.damageState.unaffected; pbEffectsOnMakingHit(move,user,b) }
            //     Ability/item effects such as Static/Rocky Helmet, and Grudge, plus
            //     Rage/Beak Blast/Shell Trap/Destiny Bond. (insert-only)
            if (!defender.damageState.unaffected) {
                BattlerHitEffects.pbEffectsOnMakingHit(this, move, attacker, defender);
            }
            // ↑↑↑ end of inserted wiring
            // :806-807 targets.each { |b| b.pbFaint ... }; user.pbFaint ...
            pbFaint(defender);
            pbFaint(attacker);
            realNumHits++;                                         // :473
            if (attacker.fainted()) break;                         // :474
            if ("SLEEP".equals(attacker.status) || "FROZEN".equals(attacker.status)) break;   // :475
            if (defender.fainted()) break;                         // :481 all targets fainted
        }
        // :495-507 Effectiveness message for multi-hit moves
        if (hits > 1) {                                            // :495 numHits>1
            if (realNumHits > 0) {                                 // :498 next if unaffected
                pbEffectivenessMessage(effectiveness);             // :499
            }
            if (realNumHits == 1) {
                pbDisplay("击中了1次！");                           // :503
            } else if (realNumHits > 1) {
                pbDisplay("击中了" + realNumHits + "次！");          // :505
            }
        }
        // :539 move.pbEffectAfterAllHits(user,b): recoil / drain as before
        // (their own lines and HP animations belong to the move-effects batch).
        if (fx.recoilMaxQuarter) {
            attacker.hp = Math.max(0, attacker.hp - Math.max(1, attacker.maxHp() / 4));
            attacker.syncHp();
        } else if (fx.recoilDen > 0 && total > 0) {
            attacker.hp = Math.max(0, attacker.hp - Math.max(1, total * fx.recoilNum / fx.recoilDen));
            attacker.syncHp();
        } else if (fx.drain && total > 0) {
            attacker.hp = Math.min(attacker.maxHp(), attacker.hp + Math.max(1, total / 2));
            attacker.syncHp();
        }
        // :541-542 Faint if 0 HP
        pbFaint(defender);
        pbFaint(attacker);
        // ↓↓↓ Stage 4 §4 wiring: Battler_UseMove:543-544
        //     pbEffectsAfterMove(user,targets,move,realNumHits) - "External/general
        //     effects after all hits. Eject Button, Shell Bell, etc." (insert-only)
        BattlerHitEffects.pbEffectsAfterMove(this, attacker,
                animationTargets(move, attacker, defender), move, realNumHits);
        // ↑↑↑ end of inserted wiring
        // :553 @battle.pbGainExp
        if (total > 0 && defender.fainted() && !attacker.foe) {
            awardExperience(defender, attacker);
        }
    }

    /**
     * {@code pbAnimateHitAndHPLost} (Move_Usage:264-292) for one target: the
     * flash and HP bar ({@code :279-281 pbHitAndHPLossAnimation}), allies
     * first then foes, then the "Battle low HP" BGM check.
     */
    private void pbAnimateHitAndHPLost(Battler user, Battler b, int hpLost, int effectiveness) {
        boolean opposes = b.foe != user.foe;                       // opposes?(user)
        for (int side = 0; side < 2; side++) {                     // :267
            if (hpLost == 0) continue;                             // :269
            if ((side == 0 && opposes) || (side == 1 && !opposes)) continue;   // :270
            int oldHP = b.hp + hpLost;                             // :271
            // :277 animArray.push([b,oldHP,effectiveness]); :279-281 one
            // pbHitAndHPLossAnimation per side.
            roundEvents.add(RoundEvent.hit(new HitEvent(b.index, oldHP, b.hp, effectiveness)));
        }
        // :284-285 return when the user has more than one ally / opponent on
        // the field - never true on this runtime's 1v1 field.
        // :286-291 the first target owned by the player that is still up but at
        // or below a quarter of its HP switches the BGM.
        if (!b.foe && !b.fainted() && b.hp <= b.maxHp() / 4) {   // :287 t.pbOwnedByPlayer?
            roundEvents.add(RoundEvent.bgm("Battle low HP"));       // :288 pbBGMPlay("Battle low HP")
        }
    }

    /** {@code pbHitEffectivenessMessages} (Move_Usage:322-345) for one target. */
    private void pbHitEffectivenessMessages(Battler user, Battler target, boolean critical,
                                            int effectiveness, boolean multiHitMove) {
        // :323-328 disguise / ice face / flame veil / substitute need the
        // ability and PBEffects subsystems (damageState is always clear here).
        if (critical) {                                            // :329
            pbDisplay("击中了要害！");                               // :333 (numTargets==1)
        }
        // :338 if !multiHitMove? && user.effects[PBEffects::ParentalBond]==0
        if (!multiHitMove) {
            pbEffectivenessMessage(effectiveness);                 // :339
        }
    }

    /** {@code pbEffectivenessMessage} (Move_Usage:297-320) for a single target. */
    private void pbEffectivenessMessage(int effectiveness) {
        // :298-305 disguise / ice face / flame veil and the three "normal damage"
        // abilities need the ability subsystem.
        if (effectiveness == 2) {                                  // :301 superEffective?
            pbDisplay("这非常有效！");                               // :310
        } else if (effectiveness == 1) {                           // :313 notVeryEffective?
            pbDisplay("这不是很有效……");                             // :317
        }
    }

    /**
     * {@code pbFaint} (Battler_ChangeSelf:61-99, wrapped by the paldea alias
     * :104-127): the brief "{1}倒下了！" line and {@code pbFaintBattler}.
     *
     * <p>Not translated: :62-65 (a battleRank>1 Boss is caught by
     * {@code pbCatchBossPokemon} instead of fainting - Boss_Battles subsystem),
     * :79-98 (happiness, form reset, LastRoundFainted, abilities on fainting,
     * primordial weather, fainted-ally count) and :106-126 (Commander).</p>
     */
    private void pbFaint(Battler b) {
        if (b == null || !b.fainted()) {                          // :66-69 (callers' "if b.fainted?")
            return;
        }
        if (b.faintedFlag) {                                       // :70 Has already fainted properly
            return;
        }
        pbDisplayBrief(b.thisName() + "倒下了！");                  // :71
        roundEvents.add(RoundEvent.faint(b.index));                // :73 @battle.scene.pbFaintBattler(self)
        b.faintedFlag = true;                                      // :74 pbInitEffects(false) -> Battler_Initialize:157
        b.cureStatus();                                            // :76-77 status = NONE, statusCount = 0
    }

    /**
     * {@code pbTryUseMove}'s status checks (Battler_UseMove_SuccessChecks:269-364):
     * sleep/freeze (:270-292), flinching (:314-329), confusion (:330-346) and
     * paralysis (:347-354), in that order. Each one prints its
     * {@code pbContinueStatus} line (Battler_Statuses:443-466) and, when the
     * status ends, its {@code pbCureStatus} line (:468-478).
     */
    private boolean canAct(Battler battler) {
        // :270-281 Sleep
        if ("SLEEP".equals(battler.status)) {
            battler.sleepTurns--;                                  // :272
            if (battler.sleepTurns <= 0) {
                battler.cureStatus();                              // :274 pbCureStatus
                pbDisplay(battler.thisName() + "醒来了！");   // :473
            } else {
                pbDisplay(battler.thisName() + "依旧在沉睡。");        // :447
                // :277-280 !move.usableWhenAsleep? (Snore / Sleep Talk); this
                // runtime has no such function codes, so it always fails.
                return false;
            }
        } else if ("FROZEN".equals(battler.status)) {               // :282-291
            if (random.nextInt(100) < 20) {                         // :284
                battler.cureStatus();                              // :285 pbCureStatus
                pbDisplay(battler.thisName() + "不再被冰冻了！");   // :477
            } else {
                pbDisplay(battler.thisName() + "被结实的冰冻着！");   // :456
                return false;                                      // :289
            }
        }
        // :314-329 Flinching
        if (battler.flinched) {
            battler.flinched = false;
            if (battler.pokemon != null && battler.pokemon.battleRank > 2) {
                // :316-321 a rank>2 Boss shrugs it off: its cry (:318-319
                // pbCryFile + pbSEPlay) and its own line.
                if (cryPlayer != null) {
                    cryPlayer.playCry(battler.pokemon);
                }
                pbDisplay(battler.thisName() + "凭借它的力量\n而无所畏惧！");
                return true;
            }
            pbDisplay(battler.thisName() + "畏缩了，无法行动！");      // :323
            return false;
        }
        // :330-346 Confusion
        if (battler.confusion > 0) {
            battler.confusion--;                                    // :332
            if (battler.confusion <= 0) {
                pbDisplay(battler.thisName() + "解除了混乱！");        // :335
            } else {
                pbDisplay(battler.thisName() + "混乱了！");            // :338
                int threshold = NEWEST_BATTLE_MECHANICS ? 33 : 50;   // :339
                if (random.nextInt(100) < threshold) {               // :340
                    // :341 pbConfusionDamage(_INTL("混乱中伤害了自己！")) -
                    // Battler_UseMove:130-145.
                    int defense = Math.max(1, battler.defense());
                    double base = Math.floor(Math.floor(
                            Math.floor((2.0 * battler.level() / 5 + 2) * 40 * battler.attack() / defense) / 50) + 2);
                    int hpBefore = battler.hp;
                    battler.hp = Math.max(0, battler.hp - (int) base);   // :139 self.hp -= hpLost
                    battler.syncHp();
                    // :140 confusionMove.pbAnimateHitAndHPLost(self,[self]); the
                    // typeless confusion move is normally effective (:135 typeMod 8).
                    pbAnimateHitAndHPLost(battler, battler, hpBefore - battler.hp, 0);
                    pbDisplay("混乱中伤害了自己！");                    // :141 @battle.pbDisplay(msg)
                    pbFaint(battler);                               // :144 pbFaint if fainted?
                    return false;                                   // :343
                }
            }
        }
        // :347-354 Paralysis
        if ("PARALYSIS".equals(battler.status) && random.nextInt(100) < 25) {   // :349
            pbDisplay(battler.thisName() + "麻痹了！\n无法行动！");      // :454
            return false;
        }
        return true;
    }

    /**
     * {@code pbCryFile} + {@code pbSEPlay} for the engine's own lines
     * ({@code Battler_UseMove_SuccessChecks:318-319}); the battle screen owns the
     * audio, and a headless battle leaves it null.
     */
    public interface CryPlayer {
        void playCry(Pokemon pokemon);
    }

    private CryPlayer cryPlayer;

    public Battle setCryPlayer(CryPlayer player) {
        this.cryPlayer = player;
        return this;
    }


    /**
     * {@code pbAccuracyCheck}'s calculation (Move_Usage_Calculations:127-142): the
     * accuracy stage and the evasion stage are turned into their own percentages
     * ({@code stageMul}/{@code stageDiv} at :132-133, both indexed by stage+6) and
     * then compared as {@code BASE_ACC * accuracy / evasion} - not as a single
     * combined stage.
     */
    boolean accuracyCheck(Battler user, Battler target, BattleMove move, MoveEffects.Effect fx) {
        if (fx.alwaysHit || move.accuracy() <= 0) {
            return true;                                          // :128 BASE_ACC==0
        }
        // :140 @battle.pbRandom(100) < modifiers[BASE_ACC] * accuracy / evasion
        return random.nextInt(100) < hitChance(user, target, move);
    }

    /**
     * The right-hand side of {@code Move_Usage_Calculations:140}: {@code
     * BASE_ACC * accuracy / evasion}, with the two stages turned into their own
     * percentages first (:130-138).
     */
    double hitChance(Battler user, Battler target, BattleMove move) {
        int accStage = clamp(user.accuracyStage(), -6, 6) + 6;     // :130
        int evaStage = clamp(target.evasionStage(), -6, 6) + 6;    // :131
        int[] stageMul = { 3, 3, 3, 3, 3, 3, 3, 4, 5, 6, 7, 8, 9 };   // :132
        int[] stageDiv = { 9, 8, 7, 6, 5, 4, 3, 3, 3, 3, 3, 3, 3 };   // :133
        double accuracy = 100.0 * stageMul[accStage] / stageDiv[accStage];   // :134
        double evasion = 100.0 * stageMul[evaStage] / stageDiv[evaStage];    // :135
        // :136-137 ACC_MULT / EVA_MULT are 1.0 (no ability or item modifiers exist).
        accuracy = Math.round(accuracy);
        evasion = Math.round(evasion);
        if (evasion < 1) {
            evasion = 1;                                          // :138
        }
        return move.accuracy() * accuracy / evasion;
    }

    /** PokeBattle_Move#pbNumHits: 2-5 hits are 35/35/15/15. */
    private int rollHits(MoveEffects.Effect fx) {
        if (fx.multiMax <= 1) {
            return 1;
        }
        if (fx.multiMin == fx.multiMax) {
            return fx.multiMax;
        }
        int roll = random.nextInt(100);
        if (roll < 35) return 2;
        if (roll < 70) return 3;
        if (roll < 85) return 4;
        return 5;
    }

    private void applyEffect(Battler user, Battler target, BattleMove move,
                             MoveEffects.Effect fx, boolean direct) {
        if (!direct) {
            int chance = move.additionalChance();
            if (chance <= 0 || random.nextInt(100) >= chance) {
                return;
            }
        }
        if (fx.status != null) {
            inflictStatus(target, fx.status);
        }
        if (fx.confuse && target.confusion <= 0 && !target.fainted()) {
            target.confusion = 2 + random.nextInt(4);
        }
        if (fx.flinch && !target.fainted()) {
            target.flinched = true;
        }
        applyStages(user, fx.statUp, 1);
        applyStages(user, fx.statDown, -1);
        applyStages(target, fx.targetStatDown, -1);
    }

    private void inflictStatus(Battler target, String status) {
        if (target.fainted() || target.statused()) {
            return;
        }
        switch (status) {
            case "SLEEP":
                if (!target.canSleep()) return;
                target.setStatus("SLEEP");
                target.sleepTurns = 2 + random.nextInt(3);
                break;
            case "POISON":
                if (!target.canPoison()) return;
                target.setStatus("POISON");
                break;
            case "TOXIC":
                if (!target.canPoison()) return;
                target.setStatus("POISON");
                target.toxic = 1;
                break;
            case "BURN":
                if (!target.canBurn()) return;
                target.setStatus("BURN");
                break;
            case "PARALYSIS":
                if (!target.canParalyze()) return;
                target.setStatus("PARALYSIS");
                break;
            case "FROZEN":
                if (!target.canFreeze()) return;
                target.setStatus("FROZEN");
                break;
            default:
                break;
        }
    }

    private void applyStages(Battler battler, int[] changes, int sign) {
        if (changes == null) {
            return;
        }
        for (int i = 0; i + 1 < changes.length; i += 2) {
            addStage(battler, changes[i], changes[i + 1] * sign);
        }
    }

    /**
     * {@code pbRaiseStatStageBasic}/{@code pbLowerStatStageBasic}
     * (Battler_StatStages:28-45/172-189) plus the line they print
     * ({@code :58-62} for a gain, {@code :224-228} for a loss). Both stop silently
     * when the stage cannot move any further ({@code increment <= 0}, :55/:221) -
     * the "{1}的{2}不能再提高了！" line only belongs to
     * {@code pbCanRaiseStatStage?}'s {@code showFailMsg} callers (:21-23).
     */
    private void addStage(Battler battler, int stat, int delta) {
        if (delta == 0 || battler.fainted()) {
            return;
        }
        int applied;
        if (stat >= 0 && stat <= MoveEffects.SPDEF) {              // :182/:40
            int before = battler.stages[stat];
            battler.stages[stat] = clamp(before + delta, -6, 6);
            applied = battler.stages[stat] - before;
        } else if (stat == MoveEffects.ACCURACY) {
            int before = battler.hitStages[0];
            battler.hitStages[0] = clamp(before + delta, -6, 6);
            applied = battler.hitStages[0] - before;
        } else if (stat == MoveEffects.EVASION) {
            int before = battler.hitStages[1];
            battler.hitStages[1] = clamp(before + delta, -6, 6);
            applied = battler.hitStages[1] - before;
        } else {
            return;
        }
        if (applied == 0) {
            return;                                                // :55/:221 return false
        }
        // :57 @battle.pbCommonAnimation("StatUp",self) if showAnim / :223 "StatDown" (showAnim is true here)
        commonAnimation(applied > 0 ? "StatUp" : "StatDown", battler);
        int magnitude = Math.min(Math.abs(applied), 3);            // [increment-1,2].min
        String verb = applied > 0
                ? (magnitude == 1 ? "提升了" : magnitude == 2 ? "大幅提升了" : "巨幅提升了")
                : (magnitude == 1 ? "降低了" : magnitude == 2 ? "大幅降低了" : "巨幅降低了");
        pbDisplay(battler.thisName() + "的" + statName(stat) + verb + "！");
    }

    /** {@code PBStats.getName} (PBStats:18-30) for this runtime's stat ids. */
    private static String statName(int stat) {
        switch (stat) {
            case MoveEffects.ATK: return "攻击";
            case MoveEffects.DEF: return "防御";
            case MoveEffects.SPEED: return "速度";
            case MoveEffects.SPATK: return "特攻";
            case MoveEffects.SPDEF: return "特防";
            case MoveEffects.ACCURACY: return "命中";
            case MoveEffects.EVASION: return "回避";
            default: return "";
        }
    }

    private static void consumePp(Battler attacker, BattleMove move) {
        for (Pokemon.MoveSlot slot : attacker.pokemon.moves) {
            if (slot.move == move.move) {
                slot.pp = Math.max(0, slot.pp - 1);
                break;
            }
        }
    }

    /** Settings:160 {@code NEWEST_BATTLE_MECHANICS} - true in this project. */
    public static final boolean NEWEST_BATTLE_MECHANICS = true;

    /**
     * {@code pbEndOfRoundPhase}'s status damage (Battle_Phase_EndOfRound:390-436).
     * The plugin iterates {@code priority} - {@code pbPriority(true)}, i.e. the
     * battlers on the field sorted by Speed, never the bench
     * (Battle_Action_AttacksPriority:253-260) - and runs the poison sweep before
     * the burn sweep.
     *
     * <p>The lines the sweeps display are collected here for the battle screen,
     * which plays them after the attack line (the engine owns no UI).</p>
     */
    /**
     * The lines this round's actions produced, in execution order (the text of
     * every {@link RoundEvent.Kind#MESSAGE} in {@link #roundEvents} that a move
     * or a status check printed). Cleared at the start of every action.
     */
    public final Array<String> roundMessages = new Array<>();

    /**
     * One battler of a {@code pbHitAndHPLossAnimation} call
     * (Scene_Animations:239-268): {@code [battler, old HP, effectiveness]}
     * (Move_Usage:277), effectiveness 0 normal / 1 resisted / 2 super (:273-276).
     */
    public static final class HitEvent {
        public final int idxBattler;
        public final int oldHp;
        /** {@code t[0].hp} when the animation runs (Scene_Animations:246 animateHP(t[1],t[0].hp,...)). */
        public final int newHp;
        public final int effectiveness;
        HitEvent(int idxBattler, int oldHp, int newHp, int effectiveness) {
            this.idxBattler = idxBattler;
            this.oldHp = oldHp;
            this.newHp = newHp;
            this.effectiveness = effectiveness;
        }
    }

    /**
     * One {@code @scene.pbAnimation(moveID,user,targets,hitNum)} /
     * {@code @scene.pbCommonAnimation(animName,user,targets)} call
     * (PokeBattle_Battle:793-799), resolved by the scene when it plays the event
     * (Scene_Animations:510-540 look the animation up at play time).
     */
    public static final class AnimationCall {
        /** True for {@code pbCommonAnimation}: {@link #name} is the animation name. */
        public final boolean common;
        public final int moveId;
        public final String name;
        /** {@code user.index}, or -1 for {@code user=nil}. */
        public final int idxUser;
        /** {@code targets[0].index}, or -1 for no target (an empty target list / {@code nil}). */
        public final int idxTarget;
        public final int hitNum;
        AnimationCall(boolean common, int moveId, String name, int idxUser, int idxTarget, int hitNum) {
            this.common = common;
            this.moveId = moveId;
            this.name = name;
            this.idxUser = idxUser;
            this.idxTarget = idxTarget;
            this.hitNum = hitNum;
        }
        @Override public String toString() {
            return (common ? "common:" + name : "move:" + moveId + "#" + hitNum)
                    + "/" + idxUser + ">" + idxTarget;
        }
    }

    /**
     * One thing the battle scene has to show, in the order the plugin's
     * {@code pbUseMove}/{@code pbEndOfRoundPhase} calls the scene:
     * <ul>
     * <li>{@code MESSAGE}: {@code pbDisplay} (brief=false, PokeBattle_Scene:115-154)
     *     or {@code pbDisplayBrief} (brief=true, :130-134 - it lingers);</li>
     * <li>{@code HIT}: one {@code pbHitAndHPLossAnimation} (Move_Usage:280);</li>
     * <li>{@code HP_CHANGE}: {@code pbHPChanged} (Scene_Animations:211-222) from
     *     {@code pbReduceHP} (Battler_ChangeSelf:14);</li>
     * <li>{@code FAINT}: {@code pbFaintBattler} (Battler_ChangeSelf:73);</li>
     * <li>{@code BGM}: {@code pbBGMPlay} (Move_Usage:288);</li>
     * <li>{@code ANIMATION}: {@code @scene.pbAnimation} / {@code @scene.pbCommonAnimation}
     *     (PokeBattle_Battle:793-799, played by Scene_Animations:510-540).</li>
     * </ul>
     */
    public static final class RoundEvent {
        public enum Kind { MESSAGE, HIT, HP_CHANGE, FAINT, BGM, ANIMATION,
            /** {@code @scene.pbThrow} (Scene_Animations:348-358). */
            BALL_THROW,
            /** {@code @scene.pbThrowAndDeflect} (Scene_Animations:389-399). */
            BALL_DEFLECT,
            /** {@code @scene.pbThrowSuccess} + {@code pbHideCaptureBall} (Scene_Animations:360-387). */
            BALL_SUCCESS }
        public final Kind kind;
        /** MESSAGE text, or the BGM name. */
        public final String text;
        public final boolean brief;
        /** HIT: the battlers animated together. */
        public final HitEvent[] hits;
        /** HP_CHANGE / FAINT. */
        public final int idxBattler;
        /** HP_CHANGE: the HP the bar starts from. */
        public final int oldHp;
        /** HP_CHANGE: {@code battler.hp} at that moment (Scene_Animations:218 animateHP(oldHP,battler.hp,...)). */
        public final int newHp;
        /**
         * MESSAGE: a line the battle port printed itself (the player's action,
         * the closing line), not one of the engine's.
         */
        public final boolean fromPort;
        /** MESSAGE: waits for the player like {@code pbDisplayPaused}. */
        public final boolean paused;
        /** ANIMATION: which animation to play, and on whom. */
        public final AnimationCall anim;
        /** BALL_THROW / BALL_DEFLECT / BALL_SUCCESS: the ball being thrown. */
        public final BallCall ball;

        /** The arguments of {@code pbThrow(ball,shakes,critical,targetBattler)}. */
        public static final class BallCall {
            public final int ballType;
            public final int shakes;
            public final boolean critical;
            public final int idxTarget;
            BallCall(int ballType, int shakes, boolean critical, int idxTarget) {
                this.ballType = ballType;
                this.shakes = shakes;
                this.critical = critical;
                this.idxTarget = idxTarget;
            }
        }

        private RoundEvent(Kind kind, String text, boolean brief, HitEvent[] hits,
                           int idxBattler, int oldHp, int newHp, boolean fromPort, boolean paused) {
            this(kind, text, brief, hits, idxBattler, oldHp, newHp, fromPort, paused, null);
        }
        private RoundEvent(Kind kind, String text, boolean brief, HitEvent[] hits,
                           int idxBattler, int oldHp, int newHp, boolean fromPort, boolean paused,
                           AnimationCall anim) {
            this(kind, text, brief, hits, idxBattler, oldHp, newHp, fromPort, paused, anim, null);
        }
        private RoundEvent(Kind kind, String text, boolean brief, HitEvent[] hits,
                           int idxBattler, int oldHp, int newHp, boolean fromPort, boolean paused,
                           AnimationCall anim, BallCall ball) {
            this.ball = ball;
            this.anim = anim;
            this.kind = kind;
            this.text = text;
            this.brief = brief;
            this.hits = hits;
            this.idxBattler = idxBattler;
            this.oldHp = oldHp;
            this.newHp = newHp;
            this.fromPort = fromPort;
            this.paused = paused;
        }
        private RoundEvent(Kind kind, String text, boolean brief, HitEvent[] hits,
                           int idxBattler, int oldHp) {
            this(kind, text, brief, hits, idxBattler, oldHp, -1, false, false);
        }
        public static RoundEvent message(String text, boolean brief) {
            return new RoundEvent(Kind.MESSAGE, text, brief, null, -1, -1);
        }
        static RoundEvent portMessage(String text) {
            return new RoundEvent(Kind.MESSAGE, text, false, null, -1, -1, -1, true, false);
        }
        RoundEvent withText(String newText) {
            return new RoundEvent(kind, newText, brief, hits, idxBattler, oldHp, newHp, fromPort, paused);
        }
        RoundEvent asPaused() {
            return new RoundEvent(kind, text, false, hits, idxBattler, oldHp, newHp, fromPort, true);
        }
        static RoundEvent hit(HitEvent... hits) {
            return new RoundEvent(Kind.HIT, null, false, hits, -1, -1);
        }
        static RoundEvent hpChanged(int idxBattler, int oldHp, int newHp) {
            return new RoundEvent(Kind.HP_CHANGE, null, false, null, idxBattler, oldHp, newHp, false, false);
        }
        static RoundEvent faint(int idxBattler) {
            return new RoundEvent(Kind.FAINT, null, false, null, idxBattler, -1);
        }
        static RoundEvent bgm(String name) {
            return new RoundEvent(Kind.BGM, name, false, null, -1, -1);
        }
        static RoundEvent ball(Kind kind, int ballType, int shakes, boolean critical, int idxTarget) {
            return new RoundEvent(kind, null, false, null, -1, -1, -1, false, false, null,
                    new BallCall(ballType, shakes, critical, idxTarget));
        }
        static RoundEvent animation(AnimationCall call) {
            return new RoundEvent(Kind.ANIMATION, null, false, null, -1, -1, -1, false, false, call);
        }
        @Override public String toString() {
            switch (kind) {
                case MESSAGE: return (brief ? "BRIEF:" : "MSG:") + text;
                case HIT: {
                    StringBuilder out = new StringBuilder("HIT");
                    for (HitEvent h : hits) out.append(':').append(h.idxBattler).append('/').append(h.oldHp)
                            .append('>').append(h.newHp).append('/').append(h.effectiveness);
                    return out.toString();
                }
                case HP_CHANGE: return "HP:" + idxBattler + "/" + oldHp + ">" + newHp;
                case FAINT: return "FAINT:" + idxBattler;
                case ANIMATION: return "ANIM:" + anim;
                case BALL_THROW: return "BALL_THROW:" + ball.ballType + "/" + ball.shakes;
                case BALL_DEFLECT: return "BALL_DEFLECT:" + ball.ballType;
                case BALL_SUCCESS: return "BALL_SUCCESS";
                default: return "BGM:" + text;
            }
        }
    }

    /**
     * Everything this round showed, in order: the moves' events, then the end
     * of round's. The battle screen plays it front to back (the engine owns no
     * UI); cleared at the start of every action.
     */
    public final Array<RoundEvent> roundEvents = new Array<>();

    /** {@code @battle.pbDisplay(msg)} (PokeBattle_Battle:773-775). */
    private void pbDisplay(String msg) {
        roundMessages.add(msg);
        roundEvents.add(RoundEvent.message(msg, false));
    }

    /** {@code @battle.pbDisplayBrief(msg)} (PokeBattle_Battle:777-779). */
    private void pbDisplayBrief(String msg) {
        roundMessages.add(msg);
        roundEvents.add(RoundEvent.message(msg, true));
    }

    /** {@code pbDisplay} from {@code pbEndOfRoundPhase} (its lines are also kept in {@link #endOfRoundMessages}). */
    private void eorDisplay(String msg) {
        endOfRoundMessages.add(msg);
        roundEvents.add(RoundEvent.message(msg, false));
    }

    public final Array<String> endOfRoundMessages = new Array<>();

    /** The battlers on the field, fastest first ({@code pbPriority(true)}). */
    private Array<Battler> fieldedBySpeed() {
        Array<Battler> fielded = new Array<>();
        Battler player = player();
        Battler foe = foe();
        if (player != null) {
            fielded.add(player);
        }
        if (foe != null) {
            fielded.add(foe);
        }
        fielded.sort((a, b) -> b.speed() - a.speed());
        return fielded;
    }

    /** End of round (pbEndOfRoundPhase): poison / burn damage, flinch clears. */
    private void endOfTurn() {
        endOfRoundMessages.clear();
        Array<Battler> fielded = fieldedBySpeed();
        // Battle_Phase_Attack:174: every battler that survived the round counts
        // one more turn on the field (@turnCount).
        for (Battler battler : fielded) {
            if (!battler.fainted()) {
                battler.turnCount++;
            }
        }
        // ↓↓↓ Stage 4 §4 B4 wiring: Battle_Phase_EndOfRound:287-326
        //     Grassy Terrain + Healer/Hydration/Shed Skin + Black Sludge/Leftovers,
        //     then Operation / Aqua Ring / Ingrain. The plugin runs the healing stage
        //     (:287) BEFORE the poison (:390) and burn (:423) stages, hence this
        //     position. (insert-only; the rest of the 60+ stage roster is in
        //     docs/stage4-eor-roster.md)
        BattleEndOfRound.pbEORHealing(this, fielded);
        // ↑↑↑ end of inserted wiring
        for (Battler battler : fielded) {
            poisonDamage(battler);
        }
        for (Battler battler : fielded) {
            burnDamage(battler);
        }
        // ↓↓↓ Stage 4 §4 B4 wiring: Battle_Phase_EndOfRound:685-701
        //     the field-effect countdowns (Trick Room / Gravity / Water Sport /
        //     Mud Sport / Wonder Room / Magic Room), each with its end message.
        //     (insert-only)
        BattleEndOfRound.pbEORFieldCountdowns(this);
        // ↑↑↑ end of inserted wiring
        // ↓↓↓ Stage 4 §4 B4 wiring: Battle_Phase_EndOfRound:725-738
        //     Slow Start's end message + Bad Dreams/Moody/Speed Boost +
        //     Flame Orb/Sticky Barb/Toxic Orb + Harvest/Pickup. (insert-only)
        BattleEndOfRound.pbEOREffect(this, fielded);
        // ↑↑↑ end of inserted wiring
        for (Battler battler : fielded) {
            battler.flinched = false;
        }
        endOfRoundZa();
    }

    /** Battle_Phase_EndOfRound:390-422. */
    private void poisonDamage(Battler battler) {
        if (battler.fainted() || !"POISON".equals(battler.status)) {
            return;
        }
        // :394-397: the counter only grows while the Pokemon is badly poisoned.
        if (battler.toxic > 0) {
            battler.toxic = Math.min(15, battler.toxic + 1);
        }
        int rank = battler.pokemon == null ? 0 : battler.pokemon.battleRank;
        int damage;
        if (rank > 2) {                                   // :412-413
            damage = battler.toxic == 0 ? battler.maxHp() / 40
                    : battler.maxHp() * battler.toxic / 160;
        } else {                                          // :414-415
            damage = battler.toxic == 0 ? battler.maxHp() / 8
                    : battler.maxHp() * battler.toxic / 16;
        }
        // :417 b.pbContinueStatus { b.pbReduceHP(dmg,false) } -> Battler_Statuses:443-466:
        // :462 pbCommonAnimation("Poison"/"Toxic") - NOT TRANSLATED (animation player);
        // :463 yield -> pbReduceHP (Battler_ChangeSelf:5-17), whose :14
        // pbHPChanged(self,oldHP,false) animates the data box's HP bar.
        int oldHP = battler.hp;
        battler.hp = Math.max(0, battler.hp - Math.max(1, damage));   // :8 amt = 1 if amt<1
        battler.syncHp();
        roundEvents.add(RoundEvent.hpChanged(battler.index, oldHP, battler.hp));   // Battler_ChangeSelf:14
        // :464 @battle.pbDisplay(msg) (:448-450).
        eorDisplay(battler.thisName() + "因为中毒受到了伤害！");
        faintFromStatus(battler);                                      // :420 b.pbFaint if b.fainted?
    }

    /**
     * {@code pbTakeEffectDamage} (Battler_ChangeSelf:50-58) ends with
     * {@code pbFaint if fainted?} (:56), so indirect damage that knocks the
     * Pokemon out shows its line and plays its faint animation too.
     */
    private void faintFromStatus(Battler battler) {
        if (!battler.fainted() || battler.faintedFlag) {           // :66-70
            return;
        }
        endOfRoundMessages.add(battler.thisName() + "倒下了！");       // :71 (end-of-round lines)
        roundEvents.add(RoundEvent.message(battler.thisName() + "倒下了！", true));   // :71 pbDisplayBrief
        roundEvents.add(RoundEvent.faint(battler.index));            // :73 pbFaintBattler
        battler.faintedFlag = true;                                  // :74 -> Battler_Initialize:157
        battler.cureStatus();                                        // :76-77
    }

    /** Battle_Phase_EndOfRound:423-436: NEWEST_BATTLE_MECHANICS halves the burn. */
    private void burnDamage(Battler battler) {
        if (battler.fainted() || !"BURN".equals(battler.status)) {
            return;
        }
        int rank = battler.pokemon == null ? 0 : battler.pokemon.battleRank;
        int damage = rank > 2 ? battler.maxHp() / 40                  // :427-428
                : (NEWEST_BATTLE_MECHANICS ? battler.maxHp() / 16 : battler.maxHp() / 8);
        // :433 b.pbContinueStatus { b.pbReduceHP(dmg,false) }: :462 the "Burn"
        // common animation is NOT TRANSLATED; :463 pbReduceHP -> :14 pbHPChanged.
        int oldHP = battler.hp;
        battler.hp = Math.max(0, battler.hp - Math.max(1, damage));
        battler.syncHp();
        roundEvents.add(RoundEvent.hpChanged(battler.index, oldHP, battler.hp));   // Battler_ChangeSelf:14
        eorDisplay(battler.thisName() + "因为灼伤受到了伤害！");        // Battler_Statuses:452/:464
        faintFromStatus(battler);                                      // :436 b.pbFaint if b.fainted?
    }

    private static int clamp(int value, int lo, int hi) {
        return Math.max(lo, Math.min(hi, value));
    }

    // ------------------------------------------------------------------
    // Mega Evolution / Primal Reversion / ZA mode
    // ------------------------------------------------------------------

    private static int sideOf(Battler battler) {
        return battler != null && battler.foe ? 1 : 0;
    }

    /** {@code pbCanMegaEvolve?}: a held Mega Stone, once per battle (ZA: full energy). */
    public boolean canMegaEvolve(Battler battler) {
        if (battler == null || battler.pokemon == null || battler.fainted()) {
            return false;
        }
        if (!battler.pokemon.hasMegaForm(pbs) || battler.pokemon.isMega()) {
            return false;
        }
        if (zaMode) {
            int side = sideOf(battler);
            if (zaMegaActive[side] || zaEnergy[side] < ZA_MAX_ENERGY) {
                return false;
            }
        } else if (megaUsed[sideOf(battler)]) {
            return false;
        }
        return true;
    }

    public boolean hasPrimal(Battler battler) {
        return battler != null && battler.pokemon != null && battler.pokemon.hasPrimalForm()
                && !battler.pokemon.isPrimal();
    }

    /** The ZA super energy of one side (0..ZA_MAX_ENERGY). */
    public int zaEnergy(int side) {
        return side == 1 ? zaEnergy[1] : zaEnergy[0];
    }

    /** {@code pbMegaEvolve}: switch to the Mega form (or Primal Reversion). */
    public boolean megaEvolve(Battler battler) {
        if (battler == null || battler.pokemon == null) {
            return false;
        }
        if (hasPrimal(battler)) {
            battler.pokemon.makePrimal(pbs);
            return true;
        }
        if (!canMegaEvolve(battler)) {
            return false;
        }
        if (zaMode && zaMegaActive[sideOf(battler)]) {
            return false;
        }
        if (!zaMode && megaUsed[sideOf(battler)]) {
            return false;
        }
        battler.pokemon.makeMega(pbs);
        if (zaMode) {
            int side = sideOf(battler);
            zaMegaActive[side] = true;
            zaMegaPkmn[side] = battler;
        } else {
            // Classic mode: the Mega does not revert mid-battle.
            megaUsed[sideOf(battler)] = true;
            zaMegaPkmn[sideOf(battler)] = battler;
        }
        return true;
    }

    /** ZA {@code pbEndOfRoundPhase}: deplete an active Mega, recharge otherwise. */
    private void endOfRoundZa() {
        if (!zaMode) {
            return;
        }
        for (int side = 0; side < 2; side++) {
            if (zaMegaActive[side]) {
                Battler active = zaMegaPkmn[side];
                if (active == null || active.fainted()) {
                    zaForceExitMega(side);
                    continue;
                }
                zaEnergy[side]--;
                if (zaEnergy[side] <= 0) {
                    zaEnergy[side] = 0;
                    revertMega(active);
                }
            } else if (zaEnergy[side] < ZA_MAX_ENERGY) {
                zaEnergy[side]++;
            }
        }
    }

    private void zaForceExitMega(int side) {
        Battler active = zaMegaPkmn[side];
        if (active != null) {
            revertMega(active);
        }
        zaEnergy[side] = 0;
        zaMegaActive[side] = false;
        zaMegaPkmn[side] = null;
    }

    private void revertMega(Battler battler) {
        if (battler != null && battler.pokemon != null && battler.pokemon.isMega()) {
            battler.pokemon.makeUnmega(pbs);
        }
    }

    /** {@code pbEndOfBattle}: a Mega never survives the battle. */
    private void revertAllMega() {
        for (int side = 0; side < 2; side++) {
            if (zaMegaPkmn[side] != null) {
                revertMega(zaMegaPkmn[side]);
            }
            zaMegaPkmn[side] = null;
            zaMegaActive[side] = false;
        }
    }

    /**
     * {@code @switchStyle} (PokeBattle_Battle:65): "Switch" asks the player
     * whether to send out a new Pokemon when an opponent's Pokemon faints
     * (Battle_Action_Switching:185-202). {@code pbPrepareBattle}
     * (PField_Battles:111-112) sets it from {@code $PokemonSystem.battlestyle}
     * and the {@code switchstyle}/{@code setstyle} rules.
     */
    public boolean switchStyle = true;

    /**
     * The exp the last {@link #awardExperience} call granted (for the battle log).
     */
    public int lastExpGain;

    /** One Pokemon's exp award (pbGainExpOne) for the battle screen to play out. */
    public static final class ExpAward {
        public Pokemon pokemon;
        public int expGained;
        public boolean outsider;
        public int oldLevel;
        /** Bar segments {@code {levelMinExp, levelMaxExp, fromExp, toExp}} per level. */
        public final Array<int[]> segments = new Array<>();
        /** The six stats before the level-ups (pbLevelUp's window). */
        public int[] oldStats = new int[6];
        /** pbLearnMove's moves of the new level(s), learned in the settlement. */
        public final Array<PbsData.Move> movesToLearn = new Array<>();
        /**
         * The exp pot credit when the level lock turned the gain into pot exp
         * (lines 170-175 / 181-184). 0 = use {@code max(1, expGained/8)}.
         */
        public int potGain;
    }

    /** The awards of the last faint, in party order. */
    public final Array<ExpAward> lastExpAwards = new Array<>();
    /** {@code trainerBattle?} scales the exp by 1.5 (Battle_ExpAndMoveLearning:132). */
    public boolean trainerBattle;
    /** The player's name, for the outsider check (lines 146-147). */
    public String playerName;
    /** {@code $game_switches[199]} / {@code [12]} (lines 167-186, 221-257). */
    public boolean levelLockOn;
    public boolean leaguePass;
    /** {@code $Trainer.badges} for the level lock (lines 179-180, 224-254). */
    public java.util.Set<Integer> badges;
    /** Settings:29 {@code MAX_LEVEL}. */
    private static final int[] MAX_LEVEL = {
            25, 16, 32, 36, 42, 48, 58, 63, 85, 100, 113, 125, 138, 150, 163, 175, 188, 200,
    };

    /**
     * PokeBattle_BattleCommon:134-138: a captured foe still awards exp, because
     * {@code GAIN_EXP_FOR_CAPTURE} is true (Settings:165). The plugin marks the
     * battler {@code captured} and calls {@code pbGainExp} before the battle
     * ends, so the awards are waiting for the battle screen's exp stage.
     */
    public void awardCaptureExperience() {
        Battler captured = foe();
        Battler receiver = player();
        if (captured != null && receiver != null) {
            awardExperience(captured, receiver);
        }
    }

    /**
     * pbGainExpOne (Battle_ExpAndMoveLearning:99-308) for one participant with
     * this project's settings: {@code SCALED_EXP_FORMULA = true} (Settings:162)
     * and {@code SPLIT_EXP_BETWEEN_GAINERS = false} (Settings:163).
     */
    private void awardExperience(Battler fainted, Battler receiver) {
        lastExpAwards.clear();
        lastExpGain = 0;
        if (receiver == null || receiver.pokemon == null || fainted.pokemon == null
                || fainted.pokemon.species == null) {
            return;
        }
        Pokemon pkmn = receiver.pokemon;
        int baseExp = fainted.pokemon.species.baseExp;
        if (baseExp <= 0) {
            return;
        }
        String growth = pkmn.growthRate();
        int maxExp = PokemonStats.experienceForLevel(growth, 100);
        if (pkmn.exp >= maxExp) {
            return; // Already at the maximum (lines 104-107)
        }
        int level = fainted.level();
        int exp = level * baseExp;                 // one participant, no split
        if (exp <= 0) {
            return;
        }
        if (trainerBattle) {
            exp = (int) Math.floor(exp * 1.5);     // line 132
        }
        // SCALED_EXP_FORMULA (lines 134-141).
        exp /= 5;
        double adjust = (2.0 * level + 10.0) / (pkmn.level + level + 10.0);
        adjust = Math.pow(adjust, 5);
        adjust = Math.sqrt(adjust);
        exp = (int) (exp * adjust);
        exp += 1;
        // Foreign Pokemon gain more (lines 146-154).
        boolean outsider = pkmn.originalTrainer != null && !pkmn.originalTrainer.isEmpty()
                && !pkmn.originalTrainer.equals(playerName);
        if (outsider) {
            exp = (int) Math.floor(exp * 1.5);
        }
        if (pkmn.happiness >= 180) {
            exp = (int) Math.floor(exp * 1.2);     // line 160
        }
        if (pkmn.superShiny) {
            exp = (int) Math.floor(exp * 1.2);     // line 161 (PokeBattle_Pokemon:335)
        }
        // The level lock (lines 167-186): a capped gain goes to the exp pot.
        int potGain = 0;
        if (levelLockOn && levelLockReached(pkmn)) {
            potGain = Math.max(1, exp / 8);
            exp = 0;
        }
        int expFinal = Math.min(maxExp, pkmn.exp + exp);
        int expGained = expFinal - pkmn.exp;
        if (expGained <= 0 && potGain == 0) {
            return;
        }
        ExpAward award = new ExpAward();
        award.pokemon = pkmn;
        award.expGained = expGained;
        award.outsider = outsider;
        award.potGain = potGain;
        award.oldLevel = pkmn.level;
        award.oldStats = new int[] { pkmn.maxHp(), pkmn.attack(), pkmn.defense(),
                pkmn.spAtk(), pkmn.spDef(), pkmn.speed() };
        // The bar plays one segment per level (lines 221-265).
        int curLevel = pkmn.level;
        int tempExp1 = pkmn.exp;
        while (tempExp1 < expFinal && curLevel < 100) {
            if (levelLockStop(curLevel)) {
                break;                             // lines 223-255
            }
            int levelMinExp = PokemonStats.experienceForLevel(growth, curLevel);
            int levelMaxExp = PokemonStats.experienceForLevel(growth, curLevel + 1);
            int tempExp2 = Math.min(levelMaxExp, expFinal);
            award.segments.add(new int[] { levelMinExp, levelMaxExp, tempExp1, tempExp2 });
            tempExp1 = tempExp2;
            curLevel++;
        }
        // Apply it (the plugin sets pkmn.exp per segment while animating).
        pkmn.hp = receiver.hp;
        if (expGained > 0 && pkmn.gainExperience(expGained)) {
            // The level's moves are learned in the settlement (pbLearnMove);
            // the evolution check stays here (P3).
            for (PbsData.LearnMove learn : pkmn.species.moves) {
                if (learn.level > award.oldLevel && learn.level <= pkmn.level) {
                    PbsData.Move move = pbs.move(learn.move);
                    if (move != null && !knowsMove(pkmn, move)) {
                        award.movesToLearn.add(move);
                    }
                }
            }
            pokemon.runtime.pokemon.PokemonGrowth.evolveOnCondition(pkmn, pbs);
        }
        receiver.hp = Math.min(pkmn.hp, receiver.maxHp());
        lastExpGain = expGained;
        lastExpAwards.add(award);
    }

    /** Battle_ExpAndMoveLearning:167-186: the level-lock cap for the exp gain. */
    private boolean levelLockReached(Pokemon pkmn) {
        if (leaguePass) {
            for (int i = 8; i <= 15; i++) {
                if (pkmn.level >= MAX_LEVEL[i + 1] && !badges.contains(i)) {
                    return true;
                }
            }
            return false;
        }
        for (int i = 0; i <= 7; i++) {
            if (pkmn.level >= MAX_LEVEL[i] && !badges.contains(i)) {
                return true;
            }
        }
        return pkmn.level >= MAX_LEVEL[8];
    }

    /** Battle_ExpAndMoveLearning:223-255: the per-level stop of the level-up loop. */
    private boolean levelLockStop(int curLevel) {
        if (!levelLockOn) {
            return false;
        }
        int[][] pairs = { {16, 1}, {25, 0}, {32, 2}, {36, 3}, {42, 4}, {48, 5},
                {58, 6}, {63, 7}, {113, 9}, {125, 10}, {138, 11}, {150, 12},
                {163, 13}, {175, 14}, {188, 15} };
        for (int[] pair : pairs) {
            if (curLevel >= pair[0] && !badges.contains(pair[1])) {
                return true;
            }
        }
        return curLevel >= 85 && !leaguePass;
    }

    private static boolean knowsMove(Pokemon pkmn, PbsData.Move move) {
        for (int i = 0; i < pkmn.moves.size; i++) {
            PbsData.Move known = pkmn.moves.get(i).move;
            if (known != null && move.internalName != null
                    && move.internalName.equals(known.internalName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code pbCanChooseMove?} (Battle_Action_AttacksPriority:5-18).
     *
     * @param slot the move SLOT (0..3), as every battle menu index is
     * @return null when the slot can be used; otherwise the
     *         {@code pbDisplayPaused} line the plugin would show, or "" for the
     *         refusals that show nothing
     */
    public String canChooseMove(int idxBattler, int slot) {
        Battler battler = battlerAt(idxBattler);
        BattleMove move = battler == null ? null : battler.moveSlot(slot);
        if (move == null) {
            return "";                                             // :8 !move
        }
        if (battler.moveSlotPp(slot) == 0 && battler.moveSlotMaxPp(slot) > 0) {
            return "技能已经没有PP了！";                              // :9-11
        }
        // :13-16 the Encore restriction and :17 battler.pbCanChooseMove?
        // (Battler_UseMove_SuccessChecks:10-...) need PBEffects (Disable, Taunt,
        // Torment, Imprison, Gravity, Heal Block, ...); none of them exist in
        // this runtime, so both pass.
        return null;
    }

    /** {@code pbRegisterMove} (Battle_Action_AttacksPriority:70-79). */
    public boolean registerMove(int idxBattler, int slot) {
        if (canChooseMove(idxBattler, slot) != null) {
            return false;                                          // :73
        }
        Battler battler = battlerAt(idxBattler);
        if (battler == null) {
            return false;
        }
        chosenMove[idxBattler & 1] = battler.moveSlot(slot);        // :76 @choices[..][2]
        chosenSlot[idxBattler & 1] = slot;                          // :75 @choices[..][1]
        return true;
    }

    /** {@code @choices[idxBattler][1]}: the registered slot, -1 when none. */
    public int chosenMoveSlot(int idxBattler) {
        return chosenSlot[idxBattler & 1];
    }

    /** {@code @choices[idxBattler][2]}: the registered move, null when none. */
    public BattleMove chosenMove(int idxBattler) {
        return chosenMove[idxBattler & 1];
    }

    private final BattleMove[] chosenMove = new BattleMove[2];
    private final int[] chosenSlot = { -1, -1 };

    private BattleMove pickMove(Battler user, Battler foe, Controller controller) {
        // pbFightMenu:68 -> pbAutoChooseMove: no slot can be used, so Struggle
        // (Battle_Action_AttacksPriority:59-67).
        if (!user.hasUsableMove()) {
            return user.struggle(pbs);
        }
        // Battle_Phase_Attack:146 b.pbProcessTurn(@choices[b.index]) ->
        // Battler_UseMove:192 move = choice[2]: the move pbRegisterMove stored
        // (Battle_Action_AttacksPriority:75-76) is the one used. The choice is
        // spent here; the plugin clears it at the next command phase
        // (Battle_Phase_Command:183 pbClearChoice).
        int side = user.index < 0 ? -1 : (user.index & 1);
        if (side >= 0 && chosenMove[side] != null) {
            BattleMove registered = chosenMove[side];
            chosenMove[side] = null;
            chosenSlot[side] = -1;
            return registered;
        }
        if (controller != null) {
            int slot = controller.chooseMove(user, foe, user.moveSlots());
            BattleMove chosen = user.moveSlot(slot);
            if (chosen == null) {
                slot = defaultAi(user, foe);
            }
            return user.moveSlot(slot);
        }
        // AI_Move:6-146 pbChooseMoves: the plugin's own AI owns a wild opponent
        // with battleRank < 2 (AI_Move:8/20-21 + :153-155 pbRegisterMoveWild).
        // Trainer battles and wild Bosses need pbRegisterMoveTrainer, which is
        // not transcribed yet, so the simple AI stays as their fallback.
        if (BattleAi.handles(this, user)) {
            int slot = BattleAi.chooseMove(this, user, random);
            if (slot == BattleAi.STRUGGLE) {
                return user.struggle(pbs);                         // AI_Move:120-122
            }
            BattleMove chosen = user.moveSlot(slot);
            if (chosen != null) {
                return chosen;                                     // AI_Move:129
            }
        }
        return user.moveSlot(defaultAi(user, foe));
    }

    /**
     * Simple AI: consider only damaging moves, never pick an immune one, and
     * maximise {@code power * typeMod * STAB} (uniform among ties by order).
     * Blank slots ({@code id==0}) are skipped like {@code pbCanChooseAnyMove?} does.
     */
    private int defaultAi(Battler user, Battler foe) {
        Array<BattleMove> moves = user.moveSlots();
        int best = -1;
        double bestScore = -1;
        for (int i = 0; i < moves.size; i++) {
            BattleMove move = moves.get(i);
            if (move == null || move.statusMove()) {
                continue;
            }
            float typeMod = pbs == null ? 1f : pbs.effectiveness(move.type(), foe.types());
            if (typeMod <= 0f) {
                continue;
            }
            double stab = user.hasType(move.type()) ? 1.5 : 1.0;
            double score = move.power() * typeMod * stab;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        if (best >= 0) {
            return best;
        }
        for (int i = 0; i < moves.size; i++) {
            BattleMove move = moves.get(i);
            if (move != null && !move.statusMove()) {
                return i;
            }
        }
        return 0;
    }

    private static Battler active(Array<Battler> party) {
        for (Battler battler : party) {
            if (!battler.fainted() && !battler.pokemon.egg) {
                return battler;
            }
        }
        return null;
    }

    private BattleResult finish(BattleResult.Outcome outcome) {
        for (Battler battler : playerParty) {
            battler.syncHp();
        }
        for (Battler battler : foeParty) {
            battler.syncHp();
        }
        revertAllMega();
        return new BattleResult(outcome, turns, null);
    }

    // ==================================================================
    // Stage 4 / M0: the Battle surface the handler and move-effect bodies
    // need (stage4-m0b-battler-api-notes.md §B, 41 entries).
    //
    // EVERYTHING BELOW IS APPENDED. No line above this point was changed,
    // removed, reordered or renamed - in particular the B8 round-event
    // ordering (RoundEvent/roundEvents and the private pbDisplay/
    // pbDisplayBrief at :1294/:1300) is untouched, which is why the display
    // methods below are new public names that mirror those two private
    // bodies instead of widening their visibility.
    //
    // Branches that depend on a subsystem this runtime does not model yet
    // (the battle scene / PokeBattle_AnimationPlayer, the Pokedex, the
    // trainer object) or on a Battler method that task-12 has not landed yet
    // are left as a documented `// 登记:` (empty / skipped) rather than a
    // substitute implementation, and carry their Ruby line number. Nothing
    // here throws on a normal path.
    // ==================================================================

    // ------------------------------------------------------------------
    // A. Fields (PokeBattle_Battle:41-94; initialised at :112-182)
    // ------------------------------------------------------------------

    /**
     * {@code @field} (PokeBattle_Battle:43, created at :112): the field-wide
     * effects plus the current weather/terrain. {@link BattleField} also parks
     * {@code @sides}/{@code @positions} for now - see its javadoc.
     */
    public final BattleField field = new BattleField();

    /** {@code @time} (PokeBattle_Battle:50, {@code :120 = 0}): 0=day, 1=eve, 2=night. */
    public int time;

    /** {@code @environment} (PokeBattle_Battle:51, {@code :121 = PBEnvironment::None}). */
    public int environment = PBEnvironment.None;

    /** {@code @decision} (PokeBattle_Battle:53, {@code :123 = 0}): 0=undecided, 1=win, 2=loss, 3=escaped, 4=caught. */
    public int decision;

    /** {@code @futureSight} (PokeBattle_Battle:83, {@code :171 = false}): Future Sight is hitting. */
    public boolean futureSight;

    /** {@code @endOfRound} (PokeBattle_Battle:84, {@code :172 = false}): in the end-of-round phase. */
    public boolean endOfRound;

    /** {@code @moldBreaker} (PokeBattle_Battle:85, {@code :173 = false}): Mold Breaker applies. */
    public boolean moldBreaker;

    /**
     * {@code @sideStatUps = [{}, {}]} (PokeBattle_Battle:94, {@code :182}): the
     * per-side tally of stat increases, which Opportunist and Mirror Herb read.
     *
     * <p>Ruby stores two Hashes ({@code stat => increment}); the frozen handler
     * interface uses one {@code List<int[]>} of {@code [stat, increment]} pairs
     * per side ({@link BattleHandlers.AbilityOnOpposingStatGain}), so the field
     * is one such list per side. The list order is the write order, which is
     * what a Ruby Hash iterates in.</p>
     */
    public final java.util.List<int[]>[] sideStatUps = newSideStatUps();

    /**
     * {@code @scene} (PokeBattle_Battle:41) - 登记: PokeBattle_Battle:41. The
     * scene ({@code PokeBattle_Scene} / {@code PokeBattle_AnimationPlayer} /
     * {@code Graphics}) is not modelled in this runtime, so this is a
     * placeholder that stays {@code null}; the 21 call sites are all registered
     * (see {@link #showAbilitySplash(Battler)} and friends).
     */
    public Object scene;

    /** {@code @sideStatUps = [{}, {}]} (PokeBattle_Battle:182): two empty sides. */
    @SuppressWarnings("unchecked")
    private static java.util.List<int[]>[] newSideStatUps() {
        java.util.List<int[]>[] out = new java.util.List[2];
        out[0] = new java.util.ArrayList<>();
        out[1] = new java.util.ArrayList<>();
        return out;
    }

    // ------------------------------------------------------------------
    // B. Read-only accessors
    // ------------------------------------------------------------------

    /** The PBS tables this battle was built with (the {@code private final pbs} field, unchanged). */
    public PbsData pbs() {
        return pbs;
    }

    /**
     * {@code pbRandom(x)} (PokeBattle_Battle:98, {@code def pbRandom(x); return rand(x); end}):
     * Ruby's {@code rand(x)} for a positive Integer, i.e. <b>0..x-1</b> - not
     * {@code rand(x)+1}.
     *
     * <p>It reuses the {@code Random} the battle was constructed with, so a
     * fixed seed reproduces a whole battle. Ruby's {@code rand(0)} would return
     * a Float and {@code rand(negative)} raises {@code ArgumentError};
     * {@link java.util.Random#nextInt(int)} raises
     * {@code IllegalArgumentException} for both, and no call site in the plugin
     * passes a non-positive value (22 {@code pbRandom} sites, all counts), so
     * that difference is documented rather than papered over.</p>
     */
    /** The battle's single {@code Random} ({@code pbRandom}/{@code rand}); DamageCalc's damage roll uses the same one. */
    public Random random() {
        return random;
    }

    public int pbRandom(int x) {
        return random.nextInt(x);
    }

    /** {@code @field.weather} (PokeBattle_Battle:675): the raw weather, without Cloud Nine/Air Lock. */
    public int weather() {
        return field.weather;
    }

    /** {@code @field.terrain} (PokeBattle_ActiveField:30): the current terrain. */
    public int terrain() {
        return field.terrain;
    }

    /** {@code @environment} (PokeBattle_Battle:51): read form of the field above. */
    public int environment() {
        return environment;
    }

    /** {@code @time} (PokeBattle_Battle:50): read form of the field above. */
    public int time() {
        return time;
    }

    /**
     * {@code @turnCount} (PokeBattle_Battle:52). This runtime's existing
     * accessor for the same counter is {@link #turns()}; this method mirrors the
     * Ruby name for the handler bodies.
     */
    public int turnCount() {
        return turns;
    }

    /**
     * {@code pbWeather} (PokeBattle_Battle:673-676): the effective weather,
     * which Cloud Nine / Air Lock suppress.
     *
     * <p>登记: PokeBattle_Battle:674 -
     * {@code eachBattler { |b| return PBWeather::None if b.hasActiveAbility?([:CLOUDNINE,:AIRLOCK]) }}
     * needs {@code Battler#hasActiveAbility?}, which task-12 has not landed yet;
     * only the {@code :675 return @field.weather} line is transcribed.</p>
     */
    public int pbWeather() {
        // 登记: PokeBattle_Battle:674 依赖 Battler#hasActiveAbility?（未落地）
        return field.weather;                                      // :675
    }

    // ------------------------------------------------------------------
    // C. Display (mirrors of B8's private methods + animation registrations)
    // ------------------------------------------------------------------

    /**
     * Mirrors B8's private {@code pbDisplay(String)} ({@code Battle.java:1294},
     * {@code PokeBattle_Battle:773-775}): the same two lines, under a new public
     * name, because the pure-append rule forbids widening the existing method's
     * visibility.
     */
    public void display(String msg) {
        roundMessages.add(msg);
        roundEvents.add(RoundEvent.message(msg, false));
    }

    /**
     * Mirrors B8's private {@code pbDisplayBrief(String)}
     * ({@code Battle.java:1300}, {@code PokeBattle_Battle:777-779}): the same
     * two lines, under a new public name.
     */
    public void displayBrief(String msg) {
        roundMessages.add(msg);
        roundEvents.add(RoundEvent.message(msg, true));
    }

    /**
     * {@code pbDisplayPaused(msg)} (PokeBattle_Battle:781-783): a message that
     * waits for a key press. The {@code paused} flag rides on
     * {@link RoundEvent#asPaused()} (B8).
     */
    public void displayPaused(String msg) {
        roundMessages.add(msg);
        roundEvents.add(RoundEvent.message(msg, false).asPaused());
    }

    /**
     * {@code pbShowAbilitySplash(battler,delay=false,logTrigger=true,ability=nil)}
     * (PokeBattle_Battle:801-808) - 登记: PokeBattle_Battle:801-808.
     *
     * <p>The body needs {@code @scene.pbShowAbilitySplash}
     * ({@code Scene_Animations:171}) and therefore {@code PokeBattle_AnimationPlayer}
     * + {@code Graphics/Animations/*}; {@code :802 PBDebug.log} is a debug log
     * with no runtime counterpart. Note {@code :803
     * return if !PokeBattle_SceneConstants::USE_ABILITY_SPLASH} - the constant is
     * true in this project, so the plugin always takes the scene branch.</p>
     */
    public void showAbilitySplash(Battler battler) {
        // 登记: PokeBattle_Battle:801-808 依赖 PokeBattle_Scene（未建模）
    }

    /**
     * {@code pbHideAbilitySplash(battler)} (PokeBattle_Battle:810-813) -
     * 登记: PokeBattle_Battle:810-813 (scene).
     */
    public void hideAbilitySplash(Battler battler) {
        // 登记: PokeBattle_Battle:810-813 依赖 PokeBattle_Scene（未建模）
    }

    /**
     * {@code pbReplaceAbilitySplash(battler)} (PokeBattle_Battle:815-818) -
     * 登记: PokeBattle_Battle:815-818 (scene).
     */
    public void replaceAbilitySplash(Battler battler) {
        // 登记: PokeBattle_Battle:815-818 依赖 PokeBattle_Scene（未建模）
    }

    /**
     * {@code attr_accessor :showAnims} (PokeBattle_Battle:66), "Battle Effects"
     * option; {@code @showAnims = true} (:143). PField_Battles:114-115 sets it
     * from {@code $PokemonSystem.battlescene==0} and the {@code noanims}/{@code anims}
     * battle rule.
     */
    public boolean showAnims = true;

    /** {@code pbCommonAnimation(name,user,targets=nil)} (PokeBattle_Battle:797-799) with the default {@code targets=nil}. */
    public void commonAnimation(String name, Battler user) {
        commonAnimation(name, user, null);                          // :797 targets=nil
    }

    /**
     * {@code pbCommonAnimation(name,user=nil,targets=nil)}
     * (PokeBattle_Battle:797-799): {@code @scene.pbCommonAnimation(name,user,targets)
     * if @showAnims}. The scene plays the event ({@code Scene_Animations:527-540}).
     *
     * <p>{@code Scene_Animations:528/530} {@code isCommander?} is not
     * translated: the runtime has no commander (Tatsugiri/Dondozo) state on
     * {@link Battler}.</p>
     */
    public void commonAnimation(String name, Battler user, Array<Battler> targets) {
        if (!showAnims) {
            return;                                                 // :798 if @showAnims
        }
        // Scene_Animations:529/532 target = target[0] if it is an Array
        Battler target = targets != null && targets.size > 0 ? targets.get(0) : null;
        roundEvents.add(RoundEvent.animation(new AnimationCall(true, -1, name,
                user == null ? -1 : user.index, target == null ? -1 : target.index, 0)));
    }

    /** {@code pbAnimation(move,user,targets,hitNum=0)} (PokeBattle_Battle:793-795) with the default {@code hitNum=0}. */
    public void animation(BattleMove move, Battler user, Array<Battler> targets) {
        animation(move, user, targets, 0);                          // :793 hitNum=0
    }

    /**
     * {@code pbAnimation(move,user,targets,hitNum=0)} (PokeBattle_Battle:793-795):
     * {@code @scene.pbAnimation(move,user,targets,hitNum) if @showAnims}. The
     * first parameter is a move id ({@code moveID}, Scene_Animations:510).
     */
    public void animation(BattleMove move, Battler user, Array<Battler> targets, int hitNum) {
        animation(move == null ? 0 : move.id(), user, targets, hitNum);
    }

    /** {@code pbAnimation(moveID,user,targets,hitNum)} for a bare move id (Move_Usage:72 passes {@code id}). */
    public void animation(int moveId, Battler user, Array<Battler> targets, int hitNum) {
        if (!showAnims) {
            return;                                                 // :794 if @showAnims
        }
        // Scene_Animations:514 target = (targets && targets.is_a?(Array)) ? targets[0] : targets
        Battler target = targets != null && targets.size > 0 ? targets.get(0) : null;
        roundEvents.add(RoundEvent.animation(new AnimationCall(false, moveId, null,
                user == null ? -1 : user.index, target == null ? -1 : target.index, hitNum)));
    }

    /** {@code pbStartWeather(user,newWeather)} (PokeBattle_Battle:679-705) with both Ruby defaults ({@code fixedDuration=false}, {@code showAnim=true}). */
    public void pbStartWeather(Battler user, int newWeather) {
        pbStartWeather(user, newWeather, false, true);              // :679 fixedDuration=false, showAnim=true
    }

    /** {@code pbStartWeather(user,newWeather,fixedDuration=false,showAnim=true)} (PokeBattle_Battle:679-705) with the default {@code showAnim=true}. */
    public void pbStartWeather(Battler user, int newWeather, boolean fixedDuration) {
        pbStartWeather(user, newWeather, fixedDuration, true);      // :679 showAnim=true
    }

    /**
     * {@code pbStartWeather} (PokeBattle_Battle:679-705), transcribed line by
     * line except the branches marked 登记.
     *
     * <p>登记: PokeBattle_Battle:683-686 - the Weather Extender branch needs
     * {@code user.itemActive?} (not landed); :688/:689 go through
     * {@link #commonAnimation(String, Battler)} / {@link #hideAbilitySplash(Battler)}
     * (scene, registered); :703 {@code b.pbCheckFormOnWeatherChange} needs a
     * Battler method that is not landed.</p>
     */
    public void pbStartWeather(Battler user, int newWeather, boolean fixedDuration, boolean showAnim) {
        if (field.weather == newWeather) {                          // :680
            return;
        }
        field.weather = newWeather;                                 // :681
        int duration = fixedDuration ? 5 : -1;                      // :682
        // 登记: PokeBattle_Battle:683-686 Weather Extender 分支依赖 Battler#itemActive?
        field.weatherDuration = duration;                           // :687
        if (showAnim) {                                             // :688
            commonAnimation(PBWeather.animationName(field.weather), null);
        }
        if (user != null) {                                         // :689
            hideAbilitySplash(user);
        }
        switch (field.weather) {                                    // :690-701
            case PBWeather.Sun:         display("阳光变得刺眼！"); break;                     // :691
            case PBWeather.Rain:        display("开始下雨了！"); break;                       // :692
            case PBWeather.Sandstorm:   display("沙暴开始肆虐！"); break;                     // :693
            case PBWeather.Hail:        display("开始下冰雹了！"); break;                     // :694
            case PBWeather.Snow:        display("开始下雪了！"); break;                       // :695
            case PBWeather.HarshSun:    display("阳光变得非常刺眼！"); break;                 // :696
            case PBWeather.HeavyRain:   display("开始下倾盆大雨了！"); break;                 // :697
            case PBWeather.StrongWinds: display("神秘的气流正在保护飞行属性宝可梦！"); break;   // :698
            case PBWeather.ShadowSky:   display("一片暗影天空出现了！"); break;               // :699
            case PBWeather.Fog:         display("场上云雾缭绕..."); break;                   // :700
            default: break;
        }
        // 登记: PokeBattle_Battle:703 eachBattler { b.pbCheckFormOnWeatherChange }
        pbEndPrimordialWeather();                                   // :704
    }

    /**
     * {@code pbEndPrimordialWeather} (PokeBattle_Battle:707-733), the method
     * {@link #pbStartWeather(Battler, int, boolean, boolean)} calls at {@code :704}.
     *
     * <p>登记: PokeBattle_Battle:712/717/722 - the guards are
     * {@code !pbCheckGlobalAbility(:DESOLATELAND)} etc., and
     * {@link #pbCheckGlobalAbility(String)} is itself registered (needs
     * {@code Battler#hasActiveAbility?}), so those branches currently read as
     * "no holder on the field" and reset the weather. 登记: :729
     * {@code b.pbCheckFormOnWeatherChange}.</p>
     */
    public void pbEndPrimordialWeather() {
        int oldWeather = field.weather;                             // :708
        switch (field.weather) {                                    // :710
            case PBWeather.HarshSun:                                // :711
                if (pbCheckGlobalAbility("DESOLATELAND") == null) { // :712
                    field.weather = PBWeather.None;                 // :713
                    display("强烈的阳光消散了！");                    // :714
                }
                break;
            case PBWeather.HeavyRain:                               // :716
                if (pbCheckGlobalAbility("PRIMORDIALSEA") == null) { // :717
                    field.weather = PBWeather.None;                 // :718
                    display("暴雨停歇了！");                          // :719
                }
                break;
            case PBWeather.StrongWinds:                             // :721
                if (pbCheckGlobalAbility("DELTASTREAM") == null) {  // :722
                    field.weather = PBWeather.None;                 // :723
                    display("神秘的气流消散了！");                    // :724
                }
                break;
            default: break;
        }
        if (field.weather != oldWeather) {                          // :727
            // 登记: PokeBattle_Battle:729 eachBattler { b.pbCheckFormOnWeatherChange }
            if (field.defaultWeather != PBWeather.None) {           // :731
                pbStartWeather(null, field.defaultWeather);
            }
        }
    }

    // ------------------------------------------------------------------
    // D. Battler iteration, party and switching
    // ------------------------------------------------------------------

    /**
     * {@code allBattlers} (PokeBattle_Battle:440-442):
     * {@code @battlers.select { |b| b && !b.fainted? }}.
     *
     * <p>Ruby's {@code @battlers} holds only the battlers ON THE FIELD (this
     * runtime fields one per side, slots 0 and 1 - {@link #refreshFieldIndices()}),
     * so this iterates {@link #battlerAt(int)} and never the whole party.</p>
     */
    public Array<Battler> allBattlers() {
        Array<Battler> out = new Array<>();
        for (int idx = 0; idx < 2; idx++) {                         // :438/:441
            Battler b = battlerAt(idx);
            if (b != null && !b.fainted()) {
                out.add(b);
            }
        }
        return out;
    }

    /**
     * {@code eachBattler { |b| ... } } (PokeBattle_Battle:437-439): Ruby yields
     * each non-fainted fielded battler; Java has no block form, so this returns
     * exactly the set {@link #allBattlers()} yields, for a for-each.
     */
    public Array<Battler> eachBattler() {
        return allBattlers();                                       // :438
    }

    /**
     * {@code eachSameSideBattler(idxBattler=0) { } } (PokeBattle_Battle:444-447):
     * the non-fainted battlers on {@code idxBattler}'s side.
     */
    public Array<Battler> eachSameSideBattler(int idxBattler) {
        return sideBattlers(idxBattler, true);                      // :446 !b.opposes?(idxBattler)
    }

    /**
     * {@code eachOtherSideBattler(idxBattler=0) { } } (PokeBattle_Battle:449-452):
     * the non-fainted battlers on the opposing side.
     */
    public Array<Battler> eachOtherSideBattler(int idxBattler) {
        return sideBattlers(idxBattler, false);                     // :451 b.opposes?(idxBattler)
    }

    /**
     * {@code allOtherSideBattlers(idxBattler=0)} (PokeBattle_Battle:454-457):
     * the same set {@link #eachOtherSideBattler(int)} yields.
     */
    public Array<Battler> allOtherSideBattlers(int idxBattler) {
        return sideBattlers(idxBattler, false);                     // :456
    }

    /**
     * {@code allSameSideBattlers(idxBattler=0)}
     * (AI_Move_EffectScores:3867-3870 - the method reopens
     * {@code PokeBattle_Battle}, it is not in PokeBattle_Battle itself):
     * {@code @battlers.select { |b| b && !b.fainted? && !b.opposes?(idxBattler) }}.
     *
     * <p>Again the fielded battlers only - {@link #partyBySide(int)} would also
     * return healthy benched Pokemon, which Ruby's {@code @battlers} never
     * contains.</p>
     */
    public Array<Battler> allSameSideBattlers(int idxBattler) {
        return sideBattlers(idxBattler, true);                      // :3869
    }

    /**
     * The shared body of the four iteration methods:
     * {@code opposes?(i) == ((@index&1) != (i&1))} (PokeBattle_Battler:784-787),
     * so "same side" is {@code (slot & 1) == (idxBattler & 1)}. Only the two
     * field slots are visited, and fainted battlers are dropped, exactly like
     * {@code @battlers.select}.
     */
    private Array<Battler> sideBattlers(int idxBattler, boolean sameSide) {
        Array<Battler> out = new Array<>();
        int side = idxBattler & 1;
        for (int idx = 0; idx < 2; idx++) {
            Battler b = battlerAt(idx);
            if (b != null && ((idx & 1) == side) == sameSide && !b.fainted()) {
                out.add(b);
            }
        }
        return out;
    }

    /** {@code pbSideBattlerCount(idxBattler=0)} (PokeBattle_Battle:459-463): how many of the side are up. */
    public int pbSideBattlerCount(int idxBattler) {
        return eachSameSideBattler(idxBattler).size;                // :461
    }

    /**
     * {@code pbAllFainted?(idxBattler=0)} (PokeBattle_Battle:353-355) =
     * {@code pbAbleCount(idxBattler)==0} ({@code :333-338} counts
     * {@code pkmn.able?} over the side's whole party). This runtime's existing
     * {@link #allFainted(int)} is that count's zero test.
     */
    public boolean pbAllFainted(int idxBattler) {
        return allFainted(idxBattler & 1);                          // :354
    }

    /**
     * {@code pbCanSwitch?(idxBattler,idxParty=-1,partyScene=nil)}
     * (Battle_Action_Switching:41-113). The message side-effect only happens
     * when {@code partyScene} is non-nil, so the boolean answer is the existing
     * {@link #canSwitch(int, int)} ({@code null} = allowed).
     */
    public boolean pbCanSwitch(int idxBattler) {
        return canSwitch(idxBattler, -1) == null;                   // :41 idxParty=-1
    }

    /** {@code pbCanSwitch?(idxBattler,idxParty)} (Battle_Action_Switching:41-113). */
    public boolean pbCanSwitch(int idxBattler, int idxParty) {
        return canSwitch(idxBattler, idxParty) == null;             // :43-112
    }

    /** {@code pbCanChooseNonActive?(idxBattler)} (Battle_Action_Switching:115-120). */
    public boolean pbCanChooseNonActive(int idxBattler) {
        return canChooseNonActive(idxBattler);                      // :116-118
    }

    /**
     * {@code pbCanRun?(idxBattler)} (Battle_Action_Running:5-28), transcribed
     * except the branches marked 登记.
     *
     * <p>登记: Battle_Action_Running:10-13 (Run Away / Smoke Ball through
     * {@code battler.abilityActive?}/{@code itemActive?} +
     * {@code BattleHandlers.triggerRunFromBattle*}), :14-19 (the six
     * {@code battler.effects[...]} trapping terms - {@code Battler#effects} is
     * not landed; the available {@code @field.effects[FairyLock]} term of :20 is
     * transcribed) and :21-26 (the opposing side's trapping abilities/items).</p>
     */
    public boolean pbCanRun(int idxBattler) {
        if (trainerBattle) {                                        // :6 trainerBattle?
            return false;
        }
        Battler battler = battlerAt(idxBattler);
        if (battler == null) {
            return false;
        }
        if (!canRun && !battler.foe) {                              // :8 !@canRun && !battler.opposes?
            return false;
        }
        if (battler.hasType("GHOST") && NEWEST_BATTLE_MECHANICS) {   // :9
            return true;
        }
        // 登记: Battle_Action_Running:10-13 依赖 Battler#abilityActive?/#itemActive?/#ability/#item（未落地）
        if (field.effects.intVal(PBEffects.Field.FairyLock) > 0) {   // :20 (the available term of :14-20)
            return false;
        }
        // 登记: Battle_Action_Running:14-19 其余六个 battler.effects[...] 项依赖 Battler#effects（未落地）
        // 登记: Battle_Action_Running:21-26 对面束缚特性/道具依赖 Battler#abilityActive?/#itemActive?
        return true;                                                // :27
    }

    /** {@code pbGetReplacementPokemonIndex(idxBattler,false)} (Battle_Action_Switching:241-253). */
    public int pbGetReplacementPokemonIndex(int idxBattler) {
        return getReplacementPokemonIndex(idxBattler);              // :251 pbSwitchInBetween
    }

    /**
     * {@code pbGetReplacementPokemonIndex(idxBattler,random=false)}
     * (Battle_Action_Switching:241-253).
     *
     * <p>The {@code random=true} branch is fully transcribed ({@code :242-249});
     * the {@code random=false} branch opens the party screen through
     * {@code pbSwitchInBetween} ({@code :251}), which the battle screen owns, so
     * it delegates to the existing {@link #getReplacementPokemonIndex(int)}.</p>
     */
    public int pbGetReplacementPokemonIndex(int idxBattler, boolean random) {
        if (random) {
            if (!pbCanSwitch(idxBattler)) {                         // :243 return -1 if !pbCanSwitch?
                return -1;
            }
            Array<Battler> party = partyOf(idxBattler);             // :244-247 eachInTeamFromBattlerIndex
            Array<Integer> choices = new Array<>();
            for (int i = 0; i < party.size; i++) {
                if (canSwitchLax(idxBattler, i) == null) {
                    choices.add(i);
                }
            }
            if (choices.size == 0) {                                // :248
                return -1;
            }
            return choices.get(pbRandom(choices.size));             // :249
        }
        return getReplacementPokemonIndex(idxBattler);              // :251
    }

    /**
     * {@code pbRecallAndReplace(idxBattler,idxParty,randomReplacement=false,batonPass=false)}
     * (Battle_Action_Switching:256-262).
     *
     * <p>登记: Battle_Action_Switching:257 ({@code @scene.pbRecall}), :258
     * ({@code battler.pbAbilitiesOnSwitchOut}), :259
     * ({@code @scene.pbShowPartyLineup}) and :260 ({@code pbMessagesOnReplace})
     * - scene/Battler sides not landed. The actual swap ({@code :261 pbReplace})
     * is the existing {@link #replace(int, int)}.</p>
     */
    public void pbRecallAndReplace(int idxBattler, int idxParty) {
        // 登记: Battle_Action_Switching:257 依赖 PokeBattle_Scene（未建模）
        // 登记: Battle_Action_Switching:258 依赖 Battler#pbAbilitiesOnSwitchOut（未落地）
        // 登记: Battle_Action_Switching:259 依赖 PokeBattle_Scene（未建模）
        // 登记: Battle_Action_Switching:260 依赖 pbMessagesOnReplace / PokeBattle_Scene（未建模）
        replace(idxBattler, idxParty);                              // :261 pbReplace
    }

    /** {@code pbClearChoice(idxBattler)} (Battle_Phase_Command:5-11): the existing {@link #clearChoice(int)}. */
    public void pbClearChoice(int idxBattler) {
        clearChoice(idxBattler);                                    // :6-10
    }

    /**
     * {@code @choices[idxBattler]} (PokeBattle_Battle:72) as a derived view:
     * {@code [action, arg1, arg2, arg3]}.
     *
     * <p>This runtime stores the four slots in dedicated fields
     * ({@code switchChoice}/{@code switchChoiceParty}/{@code chosenSlot}/
     * {@code chosenMove}) and exposes them through {@link #choiceIsSwitch(int)},
     * {@link #choiceSwitchParty(int)}, {@link #chosenMoveSlot(int)} and
     * {@link #chosenMove(int)}. Rather than keep a second, writable copy of the
     * same state, this method assembles the Ruby array on demand, so
     * {@code battle.choices(idx)[0]} reads exactly like the plugin's
     * {@code @choices[idxBattler][0]}.</p>
     *
     * <p>Slot 3 is the "target chosen yet" marker: {@code :10 -1} after a clear
     * and {@code -1} for every registered move
     * (Battle_Action_AttacksPriority:77, {@code # No target chosen yet}).</p>
     */
    public Object[] choices(int idxBattler) {
        String action;
        Object arg1;
        Object arg2;
        if (choiceIsSwitch(idxBattler)) {                           // Battle_Action_Switching:124
            action = ":SwitchOut";
            arg1 = choiceSwitchParty(idxBattler);                   // :125
            arg2 = null;
        } else if (chosenMoveSlot(idxBattler) >= 0) {                // Battle_Action_AttacksPriority:74-77
            action = ":UseMove";
            arg1 = chosenMoveSlot(idxBattler);                      // :75
            arg2 = chosenMove(idxBattler);                          // :76
        } else {                                                    // Battle_Phase_Command:7-10
            action = ":None";
            arg1 = 0;
            arg2 = null;
        }
        return new Object[] { action, arg1, arg2, -1 };              // :10 [3] = -1
    }

    /**
     * {@code pbGetOwnerName(idxBattler)} (PokeBattle_Battle:266-271).
     *
     * <p>登记: PokeBattle_Battle:266-271 - the opposing branch ({@code :268})
     * and the ally-trainer branch ({@code :269}) need
     * {@code PokeBattle_Trainer.fullname}, which the trainer/save subsystem has
     * not landed. The player branch ({@code :270 @player[idxTrainer].name}) is
     * this runtime's existing {@code playerName}.</p>
     */
    public String pbGetOwnerName(int idxBattler) {
        if ((idxBattler & 1) == 0) {                                // :270 the player's own name
            return playerName;
        }
        // 登记: PokeBattle_Battle:268 对面训练家的 fullname 依赖 PokeBattle_Trainer（未建模）
        return null;
    }

    // ------------------------------------------------------------------
    // E. Ability lookup, Pokedex and trainer
    // ------------------------------------------------------------------

    /**
     * {@code pbCheckGlobalAbility(abil)} (PokeBattle_Battle:478-481):
     * {@code eachBattler { |b| return b if b.hasActiveAbility?(abil) }; return nil}.
     *
     * <p>登记: PokeBattle_Battle:478-481 - {@code Battler#hasActiveAbility?} is
     * not landed (task-12), so this returns {@code null} ("no holder"), which is
     * also Ruby's value when nothing on the field has the ability.</p>
     */
    public Battler pbCheckGlobalAbility(String abil) {
        // 登记: PokeBattle_Battle:478-481 依赖 Battler#hasActiveAbility?（未落地）
        return null;
    }

    /**
     * {@code pbCheckOpposingAbility(abil,idxBattler=0,nearOnly=false)}
     * (PokeBattle_Battle:483-489).
     *
     * <p>登记: PokeBattle_Battle:483-489 - needs {@code Battler#hasActiveAbility?}
     * and {@code Battler#near?}; returns {@code null} like Ruby when no opposing
     * battler has the ability.</p>
     */
    public Battler pbCheckOpposingAbility(String abil, int idxBattler, boolean nearOnly) {
        // 登记: PokeBattle_Battle:483-489 依赖 Battler#hasActiveAbility? 与 Battler#near?（未落地）
        return null;
    }

    /**
     * {@code pbSetSeen(battler)} (PokeBattle_Battle:652-656) - 登记:
     * PokeBattle_Battle:652-656.
     *
     * <p>The body registers the species in the Pokedex
     * ({@code pbPlayer.seen[battler.displaySpecies] = true} and
     * {@code pbSeenForm}, PSystem_PokemonUtilities:203); the Pokedex/save
     * subsystem is not modelled.</p>
     */
    public void pbSetSeen(Battler battler) {
        // 登记: PokeBattle_Battle:652-656 依赖图鉴（pbPlayer.seen / pbSeenForm）（未建模）
    }

    /**
     * {@code pbPlayer} (PokeBattle_Battle:226): {@code return @player[0];}, the
     * player's {@code PokeBattle_Trainer}.
     *
     * <p>登记: PokeBattle_Battle:226 - the trainer object is not modelled, so
     * this returns {@code null}. Note this runtime's {@link #player()} is the
     * fielded BATTLER, a different thing (as stage4-m0b-battler-api-notes.md §B
     * points out).</p>
     */
    public Object pbPlayer() {
        // 登记: PokeBattle_Battle:226 @player[0] 依赖 PokeBattle_Trainer（未建模）
        return null;
    }

    // ==================================================================
    // Stage 4 §4 wiring - bridges for BattlerHitEffects (pure append; no
    // existing line above this block is changed). The plugin's
    // pbEffectsAfterMove2 iterates @battle.pbPriority(true), which is this
    // class's private fieldedBySpeed(); the helper class lives outside so it
    // needs the accessor.
    // ==================================================================

    /** {@code @battle.pbPriority(true)} - the battlers on the field, fastest first. */
    public Array<Battler> wiringFieldedBySpeed() {
        return fieldedBySpeed();
    }
}
