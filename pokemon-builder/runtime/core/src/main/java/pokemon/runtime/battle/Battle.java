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
        for (int i = 0; i < playerParty.size; i++) playerParty.get(i).pokemonIndex = i;   // Battler_Initialize:96 @pokemonIndex = idxParty
        for (int i = 0; i < foeParty.size; i++) foeParty.get(i).pokemonIndex = i;
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
        // Battler_Initialize:74 pbInitEffects(false) for every battler that has just taken a slot.
        for (Battler battler : playerParty) {
            if (battler.index >= 0 && !battler.effectsInitialized) {
                battler.effectsInitialized = true;
                battler.initEffects(false);
            }
        }
        for (Battler battler : foeParty) {
            if (battler.index >= 0 && !battler.effectsInitialized) {
                battler.effectsInitialized = true;
                battler.initEffects(false);
            }
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
        if (!roundStarted) turns++;                                // pbPursuitOnSwitch already advanced it
        roundStarted = false;
        roundMessages.clear();
        roundEvents.clear();
        Battler foe = foe();
        Battler player = player();
        if (foe != null && player != null) {
            chooseFor(foe, player, null);                          // Battle_Phase_Command
            BattleAttackPhase.pbAttackPhase(this);                 // the player's switch/item was played by the screen
        }
        endOfTurn();
        refreshFieldIndices();
        for (int i = 0; i < choicesStore.length; i++) clearChoice(i);   // Battle_Phase_Command:183
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
        Battler outgoing = battlerAt(idxBattler);
        incoming.resetForSwitchIn();                               // :313 pbInitialize
        if ((idxBattler & 1) == 0) {
            playerField = idxParty;
        } else {
            foeField = idxParty;
        }
        refreshFieldIndices();
        // @battlers[idxBattler] stays the same object in the plugin; here the slot's battler is a
        // different object, so the move-order table has to point at it (:319).
        for (Object[] entry : priority) {
            if (entry[0] == outgoing) entry[0] = incoming;
        }
        pbCalculatePriority(false, new int[]{idxBattler});         // :319 if DYNAMIC_PRIORITY
        return true;
    }

    // ==================================================================
    // Battle_Action_Switching (209-332): choosing and replacing battlers
    // ==================================================================

    /**
     * {@code @choices[idxBattler]} (PokeBattle_Battle:72): {@code [action, arg1, arg2,
     * target, priority]}; {@code [4]} is written by {@code pbCalculatePriority}
     * (Battle_Action_AttacksPriority:174). One entry per battler index.
     */
    private final Object[][] choicesStore = newChoices();

    private static Object[][] newChoices() {
        Object[][] c = new Object[4][];
        for (int i = 0; i < c.length; i++) {
            c[i] = new Object[]{":None", 0, null, -1, 0};                  // pbClearChoice's initial state
        }
        return c;
    }

    /** {@code @priority} (PokeBattle_Battle): {@code [battler, speed, sub-priority, priority, tie-breaker]}. */
    public final java.util.List<Object[]> priority = new java.util.ArrayList<>();
    /** {@code @priorityTrickRoom}. */
    public boolean priorityTrickRoom;
    /** {@code @rules} (PokeBattle_Battle:148 {@code = {}}): battle rules; empty unless a setup fills it. */
    public final java.util.Map<String, Object> rules = new java.util.HashMap<>();

    /** {@code @can_rebirth} / {@code @rebirth} (PokeBattle_Battle:164-165): one flag per party slot per side. */
    public final boolean[][] canRebirth = new boolean[2][6];
    public final boolean[][] rebirth = new boolean[2][6];

    /** {@code @customTerrainSeedBatch} / {@code @customTerrainSeedSymbiosisQueue} (场地:633-643). */
    public boolean customTerrainSeedBatch;
    public final java.util.List<Battler> customTerrainSeedSymbiosisQueue = new java.util.ArrayList<>();

    /** {@code pbQueueCustomTerrainSeedSymbiosis(battler)} (场地:638-643). */
    public void pbQueueCustomTerrainSeedSymbiosis(Battler battler) {
        if (!customTerrainSeedSymbiosisQueue.contains(battler)) {  // 场地:640
            customTerrainSeedSymbiosisQueue.add(battler);          // 场地:641
        }
    }

    /** {@code pbStartTerrain(user,newTerrain,fixedDuration=true)} (PokeBattle_Battle:741-769 + the 场地 wrappers). */
    public void pbStartTerrain(Battler user, int newTerrain) {
        BattleEndOfRoundPhase.pbStartTerrain(this, user, newTerrain, true);
    }

    public void pbStartTerrain(Battler user, int newTerrain, boolean fixedDuration) {
        BattleEndOfRoundPhase.pbStartTerrain(this, user, newTerrain, fixedDuration);
    }

    /**
     * {@code pbEffectsOnSwitchIn(true)} for the battlers that were just sent out
     * (Battle_Phase_Attack:69 for the player's switch, Battle_Action_Switching:235-237 for
     * {@code pbEORSwitch}); the events are in {@link #roundEvents}.
     */
    public void pbSwitchInEffects(int[] idxBattlers) {
        roundMessages.clear();
        roundEvents.clear();
        if (priority.isEmpty()) pbCalculatePriority(true, null);
        for (Battler b : pbPriority(true)) {                       // :235
            for (int idx : idxBattlers) {
                if (b.index == idx) b.pbEffectsOnSwitchIn(true);   // :236
            }
        }
    }

    /** {@code pbOnActiveAll} (Battle_StartAndEnd:354) with its events collected in {@link #roundEvents}. */
    public void pbOnActiveAllRound() {
        roundMessages.clear();
        roundEvents.clear();
        pbOnActiveAll();
    }

    /** {@code pbOnActiveAll} (Battle_Action_Switching:338-347). */
    public void pbOnActiveAll() {
        BattleSwitching.pbOnActiveAll(this);
    }

    /** {@code pbActivateHealingWish(battler)} (Battle_Action_Switching:363-382). */
    public void pbActivateHealingWish(Battler battler) {
        BattleSwitching.pbActivateHealingWish(this, battler);
    }

    /**
     * {@code pbFindBattler(idxParty,idxBattlerOther=0)} (PokeBattle_Battle:631-634): the battler that is
     * the party entry {@code idxParty}, on the side of {@code idxBattlerOther}.
     */
    public Battler pbFindBattler(int idxParty, int idxBattlerOther) {
        for (Battler b : eachSameSideBattler(idxBattlerOther)) {   // :632
            if (b.pokemonIndex == idxParty) return b;              // :632
        }
        return null;                                               // :633
    }

    /** {@code pbAbleCount(idxBattler=0)} (PokeBattle_Battle:327-338): able Pokemon in the party of {@code idxBattler}. */
    public int pbAbleCount(int idxBattler) {
        int count = 0;                                             // :328
        for (Battler pkmn : partyOf(idxBattler)) {                 // :329
            if (pkmn != null && !pkmn.fainted() && !pkmn.pokemon.egg) count += 1;   // :330 pkmn.able?
        }
        return count;                                              // :336
    }

    /** {@code pbThisEx(idxBattler,idxParty)} (PokeBattle_Battle:637-651). */
    public String pbThisEx(int idxBattler, int idxParty) {
        Array<Battler> party = partyOf(idxBattler);                // :638
        Battler pkmn = party.get(idxParty);
        if ((idxBattler & 1) != 0) {                               // :639 opposes?(idxBattler)
            if (trainerBattle) return "对手的" + pkmn.name();          // :640
            int rank = pkmn.pokemon.battleRank;                    // :641
            if (rank > 1) {                                        // :642
                if (rank > 2) return "强大的" + pkmn.name();           // :643
                return "特殊的" + pkmn.name();                         // :644
            }
            return "野生的" + pkmn.name();                           // :646
        }
        return pkmn.name();                                        // :649 (the player owns the whole side)
    }

    /** {@code maxBattlerIndex} (PokeBattle_Battle): singles has battlers 0 and 1. */
    public int maxBattlerIndex() {
        return 1;
    }

    /** {@code @choices[idxBattler][0] == :SwitchOut} (:52/:176). */
    public boolean choiceIsSwitch(int idxBattler) {
        return ":SwitchOut".equals(choices(idxBattler)[0]);
    }

    /** {@code @choices[idxBattler][1]} for a switch (:53/:124-125); -1 when none. */
    public int choiceSwitchParty(int idxBattler) {
        Object[] c = choices(idxBattler);
        return ":SwitchOut".equals(c[0]) ? (Integer) c[1] : -1;
    }

    /** {@code pbClearChoice} (Battle_Phase_Command:5-11). */
    public void clearChoice(int idxBattler) {
        if (idxBattler < 0 || idxBattler >= choicesStore.length) {
            return;
        }
        Object[] c = choicesStore[idxBattler];
        c[0] = ":None";                                            // :7
        c[1] = 0;                                                  // :8
        c[2] = null;                                               // :9
        c[3] = -1;                                                 // :10
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
        Object[] c = choicesStore[idxBattler];
        c[0] = ":SwitchOut";                                       // :124
        c[1] = idxParty;                                           // :125
        c[2] = null;                                               // :126
        c[3] = -1;                                                 // :127
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
        // Battle_Phase_Command: every battler stores its choice (:183 pbClearChoice runs
        // after the round, below).
        chooseFor(player, foe, playerController);
        chooseFor(foe, player, null);
        BattleAttackPhase.pbAttackPhase(this);                     // Battle_StartAndEnd pbAttackPhase
        // Battle_StartAndEnd:381-386: pbBattleLoop breaks before
        // pbEndOfRoundPhase when a move decided the battle, so the end of round
        // (its damage and its counter) does not run.
        if (judge() == 0) {
            endOfTurn();
        } else {
            endOfRoundMessages.clear();
        }
        for (int i = 0; i < choicesStore.length; i++) clearChoice(i);   // Battle_Phase_Command:183
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

    /** {@code pbEndOfRoundPhase} (Battle_Phase_EndOfRound:211-827), then the ZA mode's energy tick (ZA模式:236-266). */
    private void endOfTurn() {
        endOfRoundMessages.clear();
        BattleEndOfRoundPhase.pbEndOfRoundPhase(this);
        endOfRoundZa();
    }

    /** The round's prologue (turn counter, resets, move order) already ran in {@link #pbPursuitOnSwitch}. */
    public boolean attackPhasePrepared;
    /** {@code turns} was already advanced for this round by {@link #pbPursuitOnSwitch}. */
    private boolean roundStarted;

    /**
     * {@code pbAttackPhase} up to {@code pbAttackPhaseSwitch}'s {@code pbPursuit(b.index)}
     * (Battle_Phase_Attack:169-186, :50-59) for a round whose player action is a switch:
     * the opposing battler has chosen its move, the order is calculated, and a Pursuit
     * aimed at the switcher hits before it leaves. The caller shows the recall line first
     * (:57) and plays {@link #roundEvents} before the recall animation (:68).
     * 登记: :186 {@code pbAttackPhasePriorityChangeMessages} then plays after the switch, not before it.
     */
    public void pbPursuitOnSwitch(int idxSwitcher) {
        turns++;
        roundMessages.clear();
        roundEvents.clear();
        roundStarted = true;
        Battler foe = foe();
        Battler player = player();
        if (foe == null || player == null) {
            return;
        }
        chooseFor(foe, player, null);                              // Battle_Phase_Command
        BattleAttackPhase.pbAttackPhasePrologue(this);             // :171-184
        attackPhasePrepared = true;
        pbPursuit(idxSwitcher);                                    // :59
    }

    /** {@code pbEndOfRoundPhase}. */
    public void pbEndOfRoundPhase() {
        BattleEndOfRoundPhase.pbEndOfRoundPhase(this);
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
    public java.util.Set<Integer> badges = new java.util.HashSet<>();
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

    // ------------------------------------------------------------------
    // Stage 5 / 2c: the faithful move-choice checks. The older
    // canChooseMove(...) above still serves the menu/AI until 2d replaces
    // it; these are the plugin bodies including Battler#pbCanChooseMove?.
    // ------------------------------------------------------------------

    /** {@code @internalBattle} (PokeBattle_Battle:61, initialised true at :138). */
    public boolean internalBattle = true;

    /** {@code $game_switches[id]}: bound by the port (see {@code setSwitchSource}); all off when unbound. */
    public java.util.function.IntPredicate gameSwitches = id -> false;

    /** {@code pbCryFile(pokemon)} + {@code pbSEPlay}: the screen owns the audio, headless battles have none. */
    void playCry(Pokemon pokemon) {
        if (cryPlayer != null) {
            cryPlayer.playCry(pokemon);
        }
    }

    /** {@code MAX_LEVEL[index]} (Settings:29) with Ruby's negative index ({@code -1} = the last entry). */
    static int maxLevel(int index) {
        return MAX_LEVEL[index < 0 ? MAX_LEVEL.length + index : index];
    }

    /** {@code pbCanChooseMove?(idxBattler,idxMove,showMessages,sleepTalk=false)} (Battle_Action_AttacksPriority:5-18). */
    public boolean pbCanChooseMove(int idxBattler, int idxMove, boolean showMessages, boolean sleepTalk) {
        Battler battler = battlerAt(idxBattler);                                     // :6
        BattleMove move = battler.moveSlot(idxMove);                                 // :7
        if (move == null || move.id() <= 0) return false;                            // :8
        if (battler.moveSlotPp(idxMove) == 0 && battler.moveSlotMaxPp(idxMove) > 0 && !sleepTalk) {   // :9
            if (showMessages) displayPaused("技能已经没有PP了！");                     // :10
            return false;                                                            // :11
        }
        if (battler.effects.intVal(PBEffects.Battler.Encore) > 0) {                  // :13
            int idxEncoredMove = battler.pbEncoredMoveIndex();                       // :14
            if (idxEncoredMove >= 0 && idxMove != idxEncoredMove) return false;      // :15
        }
        return battler.pbCanChooseMove(move, true, showMessages, sleepTalk);         // :17
    }

    /** {@code pbCanChooseAnyMove?(idxBattler,sleepTalk=false)} (Battle_Action_AttacksPriority:20-32). */
    public boolean pbCanChooseAnyMove(int idxBattler, boolean sleepTalk) {
        Battler battler = battlerAt(idxBattler);                                     // :21
        for (int i = 0; i < battler.pokemon.moves.size; i++) {                       // :22 eachMoveWithIndex
            BattleMove m = battler.moveSlot(i);
            if (m == null) continue;
            if (battler.moveSlotPp(i) == 0 && battler.moveSlotMaxPp(i) > 0 && !sleepTalk) continue;   // :23
            if (battler.effects.intVal(PBEffects.Battler.Encore) > 0) {              // :24
                int idxEncoredMove = battler.pbEncoredMoveIndex();                   // :25
                if (idxEncoredMove >= 0 && i != idxEncoredMove) continue;            // :26
            }
            if (!battler.pbCanChooseMove(m, true, false, sleepTalk)) continue;       // :28
            return true;                                                             // :29
        }
        return false;                                                                // :31
    }

    /** {@code pbCanShowCommands?(idxBattler)} (Battle_Phase_Command:35-41). */
    public boolean pbCanShowCommands(int idxBattler) {
        Battler battler = battlerAt(idxBattler);                                     // :36
        if (battler == null || battler.fainted()) return false;                      // :37
        if (battler.isCommander()) return false;                                     // :38
        if (battler.usingMultiTurnAttack()) return false;                            // :39
        return true;                                                                 // :40
    }

    /** {@code pbCanShowFightMenu?(idxBattler)} (Battle_Phase_Command:43-55). */
    public boolean pbCanShowFightMenu(int idxBattler) {
        Battler battler = battlerAt(idxBattler);                                     // :44
        if (battler.effects.intVal(PBEffects.Battler.Encore) > 0) return false;      // :46
        boolean usable = false;                                                      // :48
        for (int i = 0; i < battler.pokemon.moves.size; i++) {                       // :49 eachMoveWithIndex
            if (!pbCanChooseMove(idxBattler, i, false, false)) continue;             // :50
            usable = true;                                                           // :51
            break;                                                                   // :52
        }
        return usable;                                                               // :54
    }

    // ------------------------------------------------------------------
    // Stage 5 / 2d: what pbUseMove / pbProcessMoveHit read on the battle.
    // ------------------------------------------------------------------

    /** {@code @lastMoveUsed} (PokeBattle_Battle, read by Copycat): the PBS id of the last move used; {@code -1} = none (:169 initialises the sibling fields to -1). */
    public int lastMoveUsed = -1;

    /** {@code @lastMoveUser} (PokeBattle_Battle:81/169): index of the last move's user. */
    public int lastMoveUser = -1;

    /** {@code @switching} (set while a battler is being switched out; read by pbChangeTargets for Pursuit). */
    public boolean switching;

    /** {@code @successStates} (Battle Arena only; Battler_UseMove:265/484-489/555). */
    public final DamageState.SuccessState[] successStates = {
            new DamageState.SuccessState(), new DamageState.SuccessState(),
            new DamageState.SuccessState(), new DamageState.SuccessState(),
    };

    /**
     * {@code pbSideSize(index)} (PokeBattle_Battle:215-217). This runtime only
     * has the singles field (sideSize is fixed at 1; doubles are the last batch
     * of the rewrite).
     */
    public int pbSideSize(int index) {
        return 1;
    }

    /** {@code pbJudge} (Battle_StartAndEnd:587-594): writes {@code @decision}. */
    public void pbJudge() {
        int d = judge();
        if (d != 0) decision = d;
    }

    /** {@code pbJudgeCheckpoint(user,move=nil)} (PokeBattle_Clauses:25-37). */
    public void pbJudgeCheckpoint(Battler user, BattleMove move) {
        if (pbAllFainted(0) && pbAllFainted(1)) {                                    // :26
            if (rules.get("drawclause") != null) {                                   // :27
                if (!(move != null && "0DD".equals(move.function()))) {              // :28 Not a draw if fainting occurred due to Liquid Ooze
                    decision = user.opposes(0) ? 1 : 2;                              // :29 win / loss
                }
            } else if (rules.get("modifiedselfdestructclause") != null) {            // :31
                if (move != null && "0E0".equals(move.function())) {                 // :32 Self-Destruct
                    decision = user.opposes(0) ? 1 : 2;                              // :33
                }
            }
        }
    }

    /**
     * {@code pbGainExp} (Battle_ExpAndMoveLearning:5-66) bridge: this runtime
     * has no {@code participants} list, so the award that {@code execute} gave
     * after a hit ({@code awardExperience}, :13-65 for one fainted foe) is
     * given here once per fainted foe.
     */
    public void pbGainExp() {
        for (Battler b : foeParty) {                                                 // :13 @battlers.each, next unless b.opposes?
            if (b != foe() || !b.fainted() || b.expAwarded) continue;                // :16 next unless b.fainted?
            Battler receiver = player();
            if (receiver == null || receiver.fainted()) continue;                    // :20 only able participants
            b.expAwarded = true;                                                     // :64 b.participants = []
            awardExperience(b, receiver);
        }
    }

    /**
     * {@code pbOnActiveOne(battler)} (Battle_Action_Switching:386-): entry
     * effects and entry hazards. 待接线: belongs to the switching batch.
     */
    public boolean pbOnActiveOne(Battler battler) {
        return BattleSwitching.pbOnActiveOne(this, battler);
    }

    /** {@code pbPrimalReversion(idxBattler)} (Battle_Action_Other:175). 待接线: Mega/Primal batch. */
    public void pbPrimalReversion(int idxBattler) {
        BattleSwitching.pbPrimalReversion(this, idxBattler);
    }

    /** {@code pbRegisterMove} (Battle_Action_AttacksPriority:70-79). */
    public boolean registerMove(int idxBattler, int slot) {
        if (canChooseMove(idxBattler, slot) != null) {
            return false;                                          // :73
        }
        Battler battler = battlerAt(idxBattler);
        if (battler == null || idxBattler < 0 || idxBattler >= choicesStore.length) {
            return false;
        }
        Object[] c = choicesStore[idxBattler];
        c[0] = ":UseMove";                                         // :74 "Use move"
        c[1] = slot;                                               // :75 Index of move to be used
        c[2] = battler.moveSlot(slot);                             // :76 PokeBattle_Move object
        c[3] = -1;                                                 // :77 No target chosen yet
        return true;
    }

    /** {@code pbRegisterTarget(idxBattler,idxTarget)} (Battle_Action_AttacksPriority:100-102). */
    public void pbRegisterTarget(int idxBattler, int idxTarget) {
        choicesStore[idxBattler][3] = idxTarget;                   // :101 Set target of move
    }

    /** {@code @choices[idxBattler][1]} of a registered move: the slot, -1 when none. */
    public int chosenMoveSlot(int idxBattler) {
        Object[] c = choices(idxBattler);
        return ":UseMove".equals(c[0]) ? (Integer) c[1] : -1;
    }

    /** {@code @choices[idxBattler][2]} of a registered move: null when none. */
    public BattleMove chosenMove(int idxBattler) {
        Object[] c = choices(idxBattler);
        return ":UseMove".equals(c[0]) ? (BattleMove) c[2] : null;
    }

    /**
     * Battle_Phase_Command: stores the battler's move choice for the round. The
     * player's was stored by {@code pbRegisterMove} (the menu), the rest are
     * picked here (the controller / the plugin's AI, or Struggle via
     * {@code pbAutoChooseMove} :63-66).
     */
    private void chooseFor(Battler user, Battler foe, Controller controller) {
        if (user == null || user.index < 0 || user.index >= choicesStore.length) {
            return;
        }
        Object[] c = choicesStore[user.index];
        if (":UseMove".equals(c[0]) && c[2] != null && user.hasUsableMove()) {
            return;                                                // registered by pbRegisterMove
        }
        BattleMove move = pickMove(user, foe, controller);
        if (move == null) {
            return;
        }
        c[0] = ":UseMove";                                         // :63 / :74
        c[1] = user.moveSlotIndex(move);                           // :64 -1 for Struggle / :75
        c[2] = move;                                               // :65 @struggle / :76
        c[3] = -1;                                                 // :66 / :77
    }

    private BattleMove pickMove(Battler user, Battler foe, Controller controller) {
        // pbFightMenu:68 -> pbAutoChooseMove: no slot can be used, so Struggle
        // (Battle_Action_AttacksPriority:59-67).
        if (!user.hasUsableMove()) {
            return user.struggle(pbs);
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
        if (endOfRound) endOfRoundMessages.add(msg);               // the lines of pbEndOfRoundPhase
    }

    /**
     * Mirrors B8's private {@code pbDisplayBrief(String)}
     * ({@code Battle.java:1300}, {@code PokeBattle_Battle:777-779}): the same
     * two lines, under a new public name.
     */
    public void displayBrief(String msg) {
        roundMessages.add(msg);
        roundEvents.add(RoundEvent.message(msg, true));
        if (endOfRound) endOfRoundMessages.add(msg);               // the lines of pbEndOfRoundPhase
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

    /** {@code @choices[idxBattler]} (PokeBattle_Battle:72). */
    public Object[] choices(int idxBattler) {
        if (idxBattler < 0 || idxBattler >= choicesStore.length) {
            return new Object[]{":None", 0, null, -1, 0};
        }
        return choicesStore[idxBattler];
    }

    // ------------------------------------------------------------------
    // Battle_Action_AttacksPriority / Battle_Phase_Attack (BattleAttackPhase)
    // ------------------------------------------------------------------

    /** {@code pbCalculatePriority(fullCalc=false,indexArray=nil)} (Battle_Action_AttacksPriority:136-248). */
    public void pbCalculatePriority() {
        BattleAttackPhase.pbCalculatePriority(this, false, null);
    }

    public void pbCalculatePriority(boolean fullCalc, int[] indexArray) {
        BattleAttackPhase.pbCalculatePriority(this, fullCalc, indexArray);
    }

    /** {@code pbPriority(onlySpeedSort=false)} (Battle_Action_AttacksPriority:253-267). */
    public Array<Battler> pbPriority(boolean onlySpeedSort) {
        return BattleAttackPhase.pbPriority(this, onlySpeedSort);
    }

    /** {@code pbPursuit(idxSwitcher)} (Battle_Phase_Attack:24-48). */
    public void pbPursuit(int idxSwitcher) {
        BattleAttackPhase.pbPursuit(this, idxSwitcher);
    }

    /** {@code pbChoseMoveFunctionCode?(idxBattler,code)} (Battle_Action_AttacksPriority:91-98). */
    public boolean pbChoseMoveFunctionCode(int idxBattler, String code) {
        return BattleAttackPhase.pbChoseMoveFunctionCode(this, idxBattler, code);
    }

    /** {@code pbMoveCanTarget?(idxUser,idxTarget,targetType)} (Battle_Action_AttacksPriority:106-131). */
    public boolean pbMoveCanTarget(int idxUser, int idxTarget, int targetType) {
        return BattleAttackPhase.pbMoveCanTarget(this, idxUser, idxTarget, targetType);
    }

    /** {@code pbAbleNonActiveCount(idxBattler=0)} (PokeBattle_Battle:340-352). */
    public int pbAbleNonActiveCount(int idxBattler) {
        Array<Battler> party = partyOf(idxBattler);                // :341
        Battler active = battlerAt(idxBattler);                    // :342-343 inBattleIndices (one battler per side)
        int count = 0;                                             // :344
        for (Battler pkmn : party) {                               // :345
            if (pkmn == null || pkmn.fainted() || pkmn.pokemon.egg) continue;   // :346 !pkmn.able?
            if (pkmn == active) continue;                          // :347
            count += 1;                                            // :348
        }
        return count;                                              // :351
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
