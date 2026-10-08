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
     * {@code @sideSizes} (PokeBattle_Battle:47, :117): the number of battlers per side. This runtime supports 1 and 2
     * (single and double battles, including 1v2 / 2v1); triples are not modelled.
     */
    private final int[] sideSizes = {1, 1};

    /**
     * {@code @battlers}: for each battler index (2*position+side) the party entry that is on the field there.
     * {@code pbSetUpSides} (Battle_StartAndEnd:116-189) fills it and {@code pbReplace} (:309-318) changes a slot.
     * -1 = nothing on the field at that index.
     */
    private final int[] fieldParty = {-1, -1, -1, -1, -1, -1};   // one per battler index: 6 for a side of 3

    // --- Mega Evolution / ZA mode (Mega evolution + ZA模式) ---
    /** ZA mode ($PokemonSystem.battle_rule == 1): a super-energy system. */
    /** {@code $PokemonSystem.battle_rule == 1} (ZA模式:8-10): the per-side Mega energy rule. */
    public boolean zaMode;
    /** ZA模式:6 {@code ZA_MAX_ENERGY}. */
    public static final int ZA_MAX_ENERGY = 3;
    /** {@code @za_energy} (ZA:14). */
    public final int[] zaEnergy = {ZA_MAX_ENERGY, ZA_MAX_ENERGY};
    /** {@code @za_mega_active} (ZA:15). */
    public final boolean[] zaMegaActive = {false, false};
    /** {@code @za_mega_pkmn} (ZA:16): the Pokemon that is Mega Evolved, per side. */
    public final pokemon.runtime.pokemon.Pokemon[] zaMegaPkmn = {null, null};
    /** {@code @za_mega_requests} (ZA:17): the battler indices that asked to Mega Evolve, per side. */
    @SuppressWarnings("unchecked")
    public final java.util.List<Integer>[] zaMegaRequests = new java.util.List[]{new java.util.ArrayList<Integer>(), new java.util.ArrayList<Integer>()};
    /** {@code @megaEvolution} (PokeBattle_Battle:152-155): {@code [side][owner]} = -1 free, the battler index when registered, -2 used. */
    public final int[][] megaEvolution = {{-1, -1, -1}, {-1, -1, -1}};
    /** {@code za_full_mega_animation?} (Mega evolution:366-369): {@code $PokemonSystem.mega_animation == 0}. */
    public boolean fullMegaAnimation = true;
    /** {@code $PokemonBag.pbHasItem?(item)} for the player. */
    public java.util.function.Predicate<String> bagHasItem;

    /** {@code $PokemonBag.pbHasItem?(item)} (Battle_Action_Other:68, :77). */
    public boolean bagHas(String item) {
        return bagHasItem != null && bagHasItem.test(item);
    }

    /** {@code pbOwnedByPlayer?(idxBattler)} (PokeBattle_Battle:287-290). */
    public boolean pbOwnedByPlayer(int idxBattler) {
        if (opposes(idxBattler, 0)) return false;                                  // :288
        return pbGetOwnerIndexFromBattlerIndex(idxBattler) == 0;                   // :289
    }

    /** {@code pbGetOwnerFromPartyIndex(idxBattler,idxParty).name}-ish: the other trainer's name for the error line (:19-20). */
    private String pbGetOwnerNameFromPartyIndex(int idxBattler, int idxParty) {
        int idxTrainer = pbGetOwnerIndexFromPartyIndex(idxBattler, idxParty);
        if (opposes(idxBattler, 0)) return idxTrainer > 1 ? opponentName3 : (idxTrainer > 0 ? opponentName2 : opponentName);
        return idxTrainer > 0 ? partnerName : playerName;
    }

    /** {@code opposes?(idxBattler1,idxBattler2=0)} (PokeBattle_Battle:538-542). */
    public boolean opposes(int idxBattler1, int idxBattler2) {
        return (idxBattler1 & 1) != (idxBattler2 & 1);                             // :541
    }

    /**
     * {@code pbGetOwnerIndexFromBattlerIndex(idxBattler)} (PokeBattle_Battle:232-243): the trainer (within its
     * side) that controls that battler index.
     */
    public int pbGetOwnerIndexFromBattlerIndex(int idxBattler) {
        int side = idxBattler & 1;
        int length = pbTrainerCount(side);                                         // :233-234 (0 = no trainer: a wild side)
        switch (length) {                                                          // :235
            case 2: {
                int n = pbSideSize(side);                                          // :237
                if (n == 3) return new int[] {0, 0, 1}[idxBattler / 2];            // :238
                return idxBattler / 2;                                             // :239
            }
            case 3:
                return idxBattler / 2;                                             // :240
            default:
                return 0;                                                          // :242
        }
    }

    /** {@code pbGetOwnerIndexFromPartyIndex(idxBattler,idxParty)} (PokeBattle_Battle:250-257). */
    public int pbGetOwnerIndexFromPartyIndex(int idxBattler, int idxParty) {
        Array<Battler> party = partyOf(idxBattler);
        return idxParty < 0 || idxParty >= party.size || party.get(idxParty) == null ? 0 : party.get(idxParty).ownerIndex;
    }

    /** {@code pbIsOwner?(idxBattler,idxParty)} (PokeBattle_Battle:281-285). */
    public boolean pbIsOwner(int idxBattler, int idxParty) {
        return pbGetOwnerIndexFromBattlerIndex(idxBattler) == pbGetOwnerIndexFromPartyIndex(idxBattler, idxParty);
    }

    /** {@code pbNumPositions(side,idxTrainer)} (PokeBattle_Battle:294-302). */
    public int pbNumPositions(int side, int idxTrainer) {
        int ret = 0;
        for (int i = 0; i < pbSideSize(side); i++) {
            if (pbGetOwnerIndexFromBattlerIndex(i * 2 + side) != idxTrainer) continue;
            ret += 1;
        }
        return ret;
    }

    /** {@code pbTeamIndexRangeFromBattlerIndex(idxBattler)} (PokeBattle_Battle:381-387): [start,end) of the owner's team in the party. */
    public int[] pbTeamIndexRangeFromBattlerIndex(int idxBattler) {
        int owner = pbGetOwnerIndexFromBattlerIndex(idxBattler);
        Array<Battler> party = partyOf(idxBattler);
        int start = party.size;
        int end = 0;
        for (int i = 0; i < party.size; i++) {
            if (party.get(i) != null && party.get(i).ownerIndex == owner) {
                start = Math.min(start, i);
                end = Math.max(end, i + 1);
            }
        }
        return start > end ? new int[] {0, 0} : new int[] {start, end};
    }

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
        return addMember(pokemon, false, 0);
    }

    /** The partner trainer's Pokemon ({@code @player[1]}, party1starts[1]): added after all of the player's own. */
    public Battle addPartner(Pokemon pokemon) {
        return addMember(pokemon, false, 1);
    }

    public Battle addFoe(Pokemon pokemon) {
        return addMember(pokemon, true, 0);
    }

    /** The second opposing trainer's Pokemon ({@code @opponent[1]}): added after all of the first one's. */
    public Battle addFoeSecondTrainer(Pokemon pokemon) {
        return addMember(pokemon, true, 1);
    }

    /** The third opposing trainer's Pokemon ({@code @opponent[2]}, a triple trainer battle). */
    public Battle addFoeThirdTrainer(Pokemon pokemon) {
        return addMember(pokemon, true, 2);
    }

    private Battle addMember(Pokemon pokemon, boolean foe, int owner) {
        if (pokemon != null) {
            Battler battler = new Battler(pokemon, foe);
            battler.ownerIndex = owner;
            battler.trainerBattle = trainerBattle;
            (foe ? foeParty : playerParty).add(battler);
            pbSetUpSides();
            refreshFieldIndices();
        }
        return this;
    }

    /**
     * {@code pbSetUpSides} (Battle_StartAndEnd:116-189): puts each trainer's first able Pokemon on the field, as
     * many as the positions that trainer controls; a wild side puts every wild Pokemon on its own position.
     */
    public void pbSetUpSides() {
        for (int i = 0; i < fieldParty.length; i++) {
            fieldParty[i] = -1;
        }
        for (int side = 0; side < 2; side++) {                                        // :118
            Array<Battler> party = partyBySide(side);
            if (side == 1 && wildBattle()) {                                          // :120
                for (int i = 0; i < party.size && i < sideSizes[1]; i++) {            // :121-122 (a wild party is as big as the side)
                    fieldParty[2 * i + side] = i;
                }
                continue;                                                             // :128
            }
            int trainers = pbTrainerCount(side);
            int[] requireds = new int[Math.max(1, trainers)];                         // :132-138
            for (int i = 0; i < sideSizes[side]; i++) {
                requireds[pbGetOwnerIndexFromBattlerIndex(i * 2 + side)] += 1;
            }
            int battlerNumber = 0;                                                    // :175
            for (int idxTrainer = 0; idxTrainer < requireds.length; idxTrainer++) {   // :176
                int placed = 0;
                for (int i = 0; i < party.size; i++) {                                // :178 eachInTeam(side,idxTrainer)
                    Battler pkmn = party.get(i);
                    if (pkmn == null || pkmn.ownerIndex != idxTrainer) continue;
                    if (pkmn.fainted() || pkmn.pokemon.egg) continue;                 // :179 next if !pkmn.able?
                    fieldParty[2 * battlerNumber + side] = i;                         // :180-181
                    battlerNumber += 1;                                               // :183
                    placed += 1;
                    if (placed >= requireds[idxTrainer]) break;                       // :184
                }
            }
        }
    }

    /** {@code setBattleMode(mode)} (PokeBattle_Battle:196-209). */
    public Battle setBattleMode(String mode) {
        switch (mode == null ? "" : mode) {
            case "triple": case "3v3": return setSideSizes(3, 3);        // :199
            case "3v2": return setSideSizes(3, 2);                       // :200
            case "3v1": return setSideSizes(3, 1);                       // :201
            case "2v3": return setSideSizes(2, 3);                       // :202
            case "double": case "2v2": return setSideSizes(2, 2);        // :203
            case "2v1": return setSideSizes(2, 1);                       // :204
            case "1v3": return setSideSizes(1, 3);                       // :205
            case "1v2": return setSideSizes(1, 2);                       // :206
            default: return setSideSizes(1, 1);                          // :207 Single, 1v1 (default)
        }
    }

    /** {@code pbAbleTeamCounts(side)} (PokeBattle_Battle:359-375): the number of able Pokemon of each trainer's team. */
    public int[] pbAbleTeamCounts(int side) {
        int trainers = Math.max(1, pbTrainerCount(side));
        int[] ret = new int[trainers];
        for (Battler pkmn : partyBySide(side)) {
            if (pkmn == null || pkmn.fainted() || pkmn.pokemon.egg) continue;     // :370 next if !pkmn || !pkmn.able?
            ret[Math.min(pkmn.ownerIndex, trainers - 1)] += 1;                    // :371-372
        }
        return ret;
    }

    /**
     * {@code pbEnsureParticipants} (Battle_StartAndEnd:16-99): makes sure every position can be filled and shrinks
     * the battle when it cannot (wild sides take the size of the wild party).
     */
    public void pbEnsureParticipants() {
        // :25-28 Prevent battles larger than 2v2 if both sides have multiple trainers
        if (trainerBattle && (sideSizes[0] > 2 || sideSizes[1] > 2) && pbTrainerCount(0) > 1 && pbTrainerCount(1) > 1) {
            throw new IllegalStateException("不能在双方都有多名训练家的情况\n下进行大于2v2的战斗！");
        }
        int[] side1counts = pbAbleTeamCounts(0);                                  // :30
        int[] side2counts = pbAbleTeamCounts(1);                                  // :31
        // Change the size of the battle depending on how many wild Pokemon there are
        if (wildBattle() && side2counts[0] != sideSizes[1]) {                     // :33
            if (sideSizes[0] == sideSizes[1]) {                                   // :34 even number of battlers per side
                sideSizes[0] = side2counts[0];                                    // :36
                sideSizes[1] = side2counts[0];
            } else {
                sideSizes[1] = side2counts[0];                                    // :39
            }
        }
        // Check if battle is possible, including changing the number of battlers per side if necessary
        while (true) {                                                            // :44
            boolean needsChanging = false;                                        // :45
            for (int side = 0; side < 2; side++) {                                // :46
                if (side == 1 && wildBattle()) continue;                          // :47
                int[] sideCounts = side == 0 ? side1counts : side2counts;         // :48
                int[] requireds = new int[sideCounts.length];                     // :49
                for (int i = 0; i < sideSizes[side]; i++) {                       // :51
                    int idxTrainer = pbGetOwnerIndexFromBattlerIndex(i * 2 + side);   // :52
                    if (idxTrainer >= requireds.length) {
                        throw new IllegalStateException("错误：def pbGetOwnerIndexFromBattlerIndex 为战斗类型" + sideSizes[0] + "v" + sideSizes[1]
                                + "，训练家" + side1counts.length + "v" + side2counts.length + "提供了无效的所有者索引！");   // :57-60
                    }
                    requireds[idxTrainer] += 1;                                   // :54
                }
                for (int i = 0; i < sideCounts.length; i++) {                     // :61
                    if (requireds[i] == 0) {                                      // :62
                        throw new IllegalStateException((side == 0 ? "玩家一方" : "对手") + (i + 1) + "没有战斗位置宝可梦无法去尝试"
                                + sideSizes[0] + "V" + sideSizes[1] + "战斗！");  // :63-66
                    }
                    if (requireds[i] <= sideCounts[i]) continue;                  // :68
                    if (requireds[i] == 1) {                                      // :69
                        throw new IllegalStateException((side == 0 ? "玩家一方" : "对方") + (i + 1) + "没有可用的宝可梦！");   // :70-71
                    }
                    needsChanging = true;                                         // :74
                    break;                                                        // :75
                }
                if (needsChanging) break;                                         // :77
            }
            if (!needsChanging) break;                                            // :79
            // Reduce one or both side's sizes by 1 and try again
            int newSize = wildBattle() ? sideSizes[0] - 1 : Math.max(sideSizes[0], sideSizes[1]) - 1;   // :81-89
            if (newSize == 0) {                                                   // :90
                throw new IllegalStateException("不要再降低任何一方的规模不然战斗是无法发生的！");   // :91
            }
            for (int side = 0; side < 2; side++) {                                // :93
                if (side == 1 && wildBattle()) continue;                          // :94
                if (sideSizes[side] == 1 || newSize > sideSizes[side]) continue;  // :95
                sideSizes[side] = newSize;                                        // :96
            }
        }
        pbSetUpSides();
        refreshFieldIndices();
    }

    /** {@code @sideSizes = [a,b]} (PokeBattle_Battle:197-206 setBattleMode). */
    public Battle setSideSizes(int player, int opposing) {
        sideSizes[0] = player;
        sideSizes[1] = opposing;
        pbSetUpSides();
        refreshFieldIndices();
        return this;
    }

    /** The number of trainers on a side ({@code @player.length} / {@code @opponent.length}); 0 for a wild side. */
    public int pbTrainerCount(int side) {
        if (side == 1 && wildBattle()) return 0;
        int count = 1;
        for (Battler b : partyBySide(side)) {
            if (b != null) count = Math.max(count, b.ownerIndex + 1);
        }
        return count;
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
        for (int idx = 0; idx < fieldParty.length; idx++) {
            Battler b = battlerAt(idx);
            if (b != null) {
                b.index = idx;                                    // :180 2*slot+side
            }
        }
        // 登记: the plugin updates the participants in pbOnActiveOne (Battle_Action_Switching:399) when a Pokemon enters;
        // a battle assembled by addPlayer/addFoe never runs pbOnActiveOne, so the fielded battlers meet here.
        for (int idx = 0; idx < fieldParty.length; idx++) {
            Battler b = battlerAt(idx);
            if (b != null) b.pbUpdateParticipants();
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

    /** {@code @battlers[0]} (the player's first position). */
    public Battler player() {
        return battlerAt(0);
    }
    /** The player's party entry on battler index 0 ({@code @party1order[0]}). */
    public int playerFieldIndex() {
        return fieldParty[0];
    }
    /** The opposing side's party entry on battler index 1. */
    public int foeFieldIndex() {
        return fieldParty[1];
    }
    /** The party entry on battler index {@code idxBattler} (-1 = nothing). */
    public int fieldPartyIndex(int idxBattler) {
        return idxBattler < 0 || idxBattler >= fieldParty.length ? -1 : fieldParty[idxBattler];
    }
    /** {@code @battlers[1]}. */
    public Battler foe() {
        return battlerAt(1);
    }
    /** {@code @battlers[idxBattler]}: null when nothing stands there. */
    public Battler battlerAt(int idxBattler) {
        if (idxBattler < 0 || idxBattler >= fieldParty.length) return null;
        int idxParty = fieldParty[idxBattler];
        Array<Battler> party = partyOf(idxBattler);
        return idxParty < 0 || idxParty >= party.size ? null : party.get(idxParty);
    }
    /** {@code gNewBS->ai.didTypeAbsorbSwitchToMonBefore[side]}: party-index bitmask per side (AiSwitching). */
    final int[] aiAbsorbSwitched = new int[2];
    /** {@code gNewBS->ai.typeAbsorbSwitchingCooldown}: the round of the last type-absorb switch per side, null = never. */
    final Integer[] aiTypeAbsorbSwitchTurn = new Integer[2];

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
            if (battler != null && !battler.removedFromParty && !battler.fainted() && !battler.pokemon.egg) {
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
        if (this.decision == 3) return finish(BattleResult.Outcome.ESCAPE);   // @decision = 3 (Battle_Action_Running:152 etc.)
        if (this.decision == 4) return finish(BattleResult.Outcome.CAUGHT);   // @decision = 4 "Battle ended by capture" (PokeBattle_BattleCommon:141)
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
        if (foe() != null && player() != null) {
            pbChooseAll(false);                                    // Battle_Phase_Command (the player's own action was played by the screen)
            if (!pbAttackPhaseSwitchOpposing()) {                  // Battle_Phase_Attack:50-71 for the opposing side's registered switches
                return result();
            }
            BattleAttackPhase.pbAttackPhase(this);
        }
        endOfTurn();
        refreshFieldIndices();
        pbClearChoicesForNextRound();                              // Battle_Phase_Command:181-190
        BattleMega.pbCommandPhaseZa(this);                         // ZA模式:225-232 (the next command phase starts)
        return result();
    }
    /**
     * {@code pbAttackPhaseSwitch} (Battle_Phase_Attack:50-71) for the battlers the screen does not play: an opposing
     * trainer's voluntary switch (CFRU {@code AI_TrySwitchOrUseItem}, see {@link AiSwitching}).
     * 登记: the player's and the opponent's switches of one round run player first instead of together in speed order.
     *
     * @return false when the battle ended during the switch
     */
    private boolean pbAttackPhaseSwitchOpposing() {
        for (Battler b : pbPriority(false)) {                      // :51
            if (b.fainted() || pbOwnedByPlayer(b.index)) continue;
            Object[] c = choices(b.index);
            if (!":SwitchOut".equals(c[0])) continue;              // :52
            int idxNew = (Integer) c[1];                           // :53
            pbPursuit(b.index);                                    // :59
            if (b.fainted()) continue;                             // :60
            BattleSwitchAction.pbMessageOnRecall(this, b);         // :57
            pbRecallAndReplace(b.index, idxNew);                   // :68
            Battler incoming = battlerAt(b.index);
            if (incoming != null) incoming.pbEffectsOnSwitchIn(true);   // :69
            if (result() != null) return false;
        }
        return true;
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
        return replace(idxBattler, idxParty, false);
    }

    /** {@code pbReplace(idxBattler,idxParty,batonPass)}'s {@code pbInitialize} (Battle_Action_Switching:313). */
    public boolean replace(int idxBattler, int idxParty, boolean batonPass) {
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
        if (batonPass && outgoing != null && outgoing != incoming) {
            incoming.pbInheritBatonPass(outgoing);                 // Battler_Initialize:68 pbInitEffects(true) keeps the passed effects
        }
        fieldParty[idxBattler] = idxParty;
        refreshFieldIndices();
        if (outgoing != null && outgoing != incoming) {
            // Ruby's @battlers[idxBattler] is ONE object that now holds the new Pokemon; here every party member is
            // its own Battler, so the leaving one keeps naming the slot (until the next refreshFieldIndices) for the
            // effect code that goes on with its reference (user.index, user.pbEffectsOnSwitchIn(true), ...).
            outgoing.index = idxBattler;
        }
        if (batonPass && outgoing != null && outgoing != incoming) {
            incoming.initEffects(true);                            // Battler_Initialize:68 pbInitEffects(batonPass)
        }
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
        Object[][] c = new Object[6][];
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

    /** {@code maxBattlerIndex} (PokeBattle_Battle:219-221). */
    public int maxBattlerIndex() {
        return sideSizes[0] > sideSizes[1] ? (sideSizes[0] - 1) * 2 : sideSizes[1] * 2 - 1;
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
        // :15-18 UseItem: returning the item to the bag is done by the port
        pbUnregisterMegaEvolution(idxBattler);                     // :20
        clearChoice(idxBattler);                                   // :22
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
        if (!pbIsOwner(idxBattler, idxParty)) {                   // :18
            return "不能将宝可梦与" + pbGetOwnerNameFromPartyIndex(idxBattler, idxParty) + "的宝可梦替换！";   // :19-22
        }
        if (candidate.fainted()) {
            return candidate.name() + "已经无法战斗了！";            // :24-28
        }
        if (battlerAt(idxBattler) == candidate) {
            return candidate.name() + "已经参与战斗了！";            // :29-33 pbFindBattler
        }
        return null;
    }

    /**
     * {@code pbCanSwitch?(idxBattler,idxParty=-1,partyScene=nil)} (Battle_Action_Switching:41-113).
     *
     * @return null when the battler can switch out; otherwise the {@code partyScene.pbDisplay} line the plugin
     *         shows in the party screen ("" for the branches that show nothing)
     */
    public String canSwitch(int idxBattler, int idxParty) {
        // Check whether party Pokemon can switch in
        String lax = canSwitchLax(idxBattler, idxParty);           // :43
        if (lax != null) {
            return lax;
        }
        // Make sure another battler isn't already choosing to switch to the party Pokemon
        for (Battler b : eachSameSideBattler(idxBattler)) {        // :46
            Object[] c = choices(b.index);
            if (!":SwitchOut".equals(c[0]) || !Integer.valueOf(idxParty).equals(c[1])) {   // :47
                continue;
            }
            return partyOf(idxBattler).get(idxParty).name() + "已经被选择了！";          // :48-49
        }
        // Check whether battler can switch out
        Battler battler = battlerAt(idxBattler);                   // :53
        if (battler == null) {
            return "";
        }
        if (battler.fainted()) {                                   // :54
            return null;
        }
        // Ability/item effects that allow switching no matter what
        if (battler.abilityActive()) {                             // :56
            if (BattleHandlers.triggerCertainSwitchingUserAbility(battler.ability, battler, this)) {   // :57
                return null;                                       // :58
            }
        }
        if (battler.itemActive()) {                                // :61
            if (BattleHandlers.triggerCertainSwitchingUserItem(battler.item, battler, this)) {         // :62
                return null;                                       // :63
            }
        }
        // Other certain switching effects
        if (NEWEST_BATTLE_MECHANICS && battler.pbHasType("GHOST")) {   // :67
            return null;
        }
        // Other certain trapping effects
        if (battler.effects.intVal(PBEffects.Battler.OctolockUser) >= 0) {   // :69
            return battler.pbThis() + "无法离开战斗！";             // :70
        }
        if (battler.effects.truthy(PBEffects.Battler.JawLock)) {   // :73
            for (Battler b : allBattlersRaw()) {                   // :74 @battlers.each
                if (battler.effects.intVal(PBEffects.Battler.JawLockUser) == b.index && !b.fainted()) {   // :75
                    return battler.pbThis() + "无法离开战斗！";     // :76
                }
            }
        }
        if (battler.effects.intVal(PBEffects.Battler.Trapping) > 0                  // :81
                || battler.effects.intVal(PBEffects.Battler.MeanLook) >= 0          // :82
                || battler.effects.truthy(PBEffects.Battler.Ingrain)                // :83
                || battler.effects.truthy(PBEffects.Battler.NoRetreat)              // :84
                || battler.effects.intVal(PBEffects.Battler.CurseNail) > 0          // :85 咒钉
                || battler.effects.intVal(PBEffects.Battler.FierceKilling) > 0      // :86 断刃鏖杀
                || field.effects.intVal(PBEffects.Field.FairyLock) > 0) {           // :87
            return battler.pbThis() + "无法离开战斗！";             // :88
        }
        // Trapping abilities/items
        for (Battler b : eachOtherSideBattler(idxBattler)) {       // :92
            if (!b.abilityActive()) {                              // :93
                continue;
            }
            if (BattleHandlers.triggerTrappingTargetAbility(b.ability, battler, b, this)) {   // :94
                return b.pbThis() + "的" + b.abilityName() + "生效了" + NL + "对方无法离开战斗！";   // :95-96
            }
        }
        for (Battler b : eachOtherSideBattler(idxBattler)) {       // :100
            if (!b.itemActive()) {                                 // :101
                continue;
            }
            if (BattleHandlers.triggerTrappingTargetItem(b.item, battler, b, this)) {         // :102
                return b.pbThis() + "的" + b.itemName() + "生效了" + NL + "对方无法离开战斗！";      // :103-104
            }
        }
        if (battler.effects.truthy(PBEffects.Battler.Commander)) { // :108
            return battler.pbThis() + "无法被换上！";               // :109
        }
        return null;                                               // :112
    }

    private static final String NL = String.valueOf((char) 10);

    /** {@code @battlers} without the fainted filter (the Ruby's plain {@code @battlers.each}). */
    private Array<Battler> allBattlersRaw() {
        Array<Battler> out = new Array<>();
        for (int i = 0; i <= maxBattlerIndex(); i++) {
            Battler b = battlerAt(i);
            if (b != null) out.add(b);
        }
        return out;
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
        if (idxBattler < 0 || idxBattler >= choicesStore.length) {
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
     * {@code pbDefaultChooseNewEnemy} (AI_Switch:179-186) and
     * {@code pbChooseBestNewEnemy} (:188-227).
     *
     * @return the party index the AI sends out, or -1 when nothing can come in.
     */
    public int defaultChooseNewEnemy(int idxBattler) {
        if (trainerBattle && singleBattle() && (idxBattler & 1) == 1) {
            Battler fainted = battlerAt(idxBattler);
            if (fainted != null) {
                int pick = AiSwitching.replacement(this, fainted, random);   // CFRU GetMostSuitableMonToSwitchInto (ai_switching.c:1860)
                if (pick >= 0 && canSwitchLax(idxBattler, pick) == null) {
                    return pick;
                }
            }
        }
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
        return eachOtherSideBattler(idxBattler);                   // battler.eachOpposing
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

    public Array<Battler> playerParty() {
        return playerParty;
    }

    public Array<Battler> foeParty() {
        return foeParty;
    }

    /**
     * Runs the battle until one side is out of able Pokemon or {@code maxTurns}
     * is reached; HP is written back onto the Pokemon either way. A fainted
     * battler is replaced by {@code pbEORSwitch} (at the end of the round) with
     * the {@link HeadlessScene} answering its prompts (there is no UI here).
     */
    public BattleResult run(int maxTurns) {
        while (turns < maxTurns) {
            BattleResult result = result();
            if (result != null) {
                return result;
            }
            turns++;
            roundEvents.clear();       // no scene plays them in a headless battle
            lastExpAwards.clear();     // nor the exp awards
            runTurn(player(), foe());
        }
        return finish(BattleResult.Outcome.ESCAPE);
    }

    private void runTurn(Battler player, Battler foe) {
        // Battle_Phase_Command: every battler stores its choice (:183 pbClearChoice runs
        // after the round, below).
        pbChooseAll(true);
        BattleAttackPhase.pbAttackPhase(this);                     // Battle_StartAndEnd pbAttackPhase
        // Battle_StartAndEnd:381-386: pbBattleLoop breaks before
        // pbEndOfRoundPhase when a move decided the battle, so the end of round
        // (its damage and its counter) does not run.
        if (judge() == 0) {
            endOfTurn();
        } else {
            endOfRoundMessages.clear();
        }
        pbClearChoicesForNextRound();                              // Battle_Phase_Command:181-190
        BattleMega.pbCommandPhaseZa(this);                         // ZA模式:225-232 (the next command phase starts)
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
            BALL_SUCCESS,
            /** {@code @scene.pbChangePokemon} + {@code pbRefreshOne} (Mega evolution:426-427). */
            CHANGE_POKEMON,
            /** {@code @scene.pbSwapBattlerSprites(idxA,idxB)} (PokeBattle_Scene:266-279): {@code idxBattler}, the other index in {@code oldHp}. */
            SWAP_SPRITES,
            /**
             * The full Mega Evolution scene (Mega evolution:400-417): {@code idxBattler}, the scene kind in
             * {@code oldHp} (0 Mega, 1 Primal Groudon, 2 Primal Kyogre), the old form in {@code newHp}'s
             * low half and the new form in its high half.
             */
            MEGA_SCENE }
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
        /** {@code oldForm} is the form the sprite shows until the event plays. */
        static RoundEvent changePokemon(int idxBattler, int oldForm) {
            return new RoundEvent(Kind.CHANGE_POKEMON, null, false, null, idxBattler, oldForm, -1, false, false);
        }
        static RoundEvent swapSprites(int idxA, int idxB) {
            return new RoundEvent(Kind.SWAP_SPRITES, null, false, null, idxA, idxB, -1, false, false);
        }
        static RoundEvent megaScene(int idxBattler, int sceneKind, int oldForm, int newForm) {
            return new RoundEvent(Kind.MEGA_SCENE, null, false, null, idxBattler, sceneKind,
                    (newForm << 16) | (oldForm & 0xFFFF), false, false);
        }
        /** MEGA_SCENE: the form before the transformation. */
        public int megaOldForm() {
            return newHp & 0xFFFF;
        }
        /** MEGA_SCENE: the form after the transformation. */
        public int megaNewForm() {
            return newHp >>> 16;
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
                case CHANGE_POKEMON: return "CHANGE_POKEMON:" + idxBattler;
                case SWAP_SPRITES: return "SWAP_SPRITES:" + idxBattler + "/" + oldHp;
                case MEGA_SCENE: return "MEGA_SCENE:" + idxBattler + "/" + oldHp + "/" + megaOldForm() + ">" + megaNewForm();
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
        for (int idx = 0; idx <= maxBattlerIndex(); idx++) {
            Battler b = battlerAt(idx);
            if (b != null) {
                fielded.add(b);
            }
        }
        fielded.sort((a, b) -> b.speed() - a.speed());
        return fielded;
    }

    /** {@code pbEndOfRoundPhase} (Battle_Phase_EndOfRound:211-827), then the ZA mode's energy tick (ZA模式:236-266). */
    private void endOfTurn() {
        endOfRoundMessages.clear();
        BattleEndOfRoundPhase.pbEndOfRoundPhase(this);
        BattleMega.pbEndOfRoundZa(this);                           // ZA模式:234-271
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
        if (roundStarted) {
            roundMessages.clear();                                 // a second switcher of the same round: the round is already under way
            roundEvents.clear();
        } else {
            pbBeginSwitchPhase();
        }
        if (foe() == null || player() == null) {
            return;
        }
        pbPursuit(idxSwitcher);                                    // :59
    }

    /**
     * The start of the round's attack phase (Battle_Phase_Attack:171-184) up to the first {@code pbAttackPhaseSwitch}
     * entry, run once per round: the turn counts, every non-player battler stores its choice and the priority order
     * is calculated. Later switchers of the same round (a double battle) reuse it.
     */
    public void pbBeginSwitchPhase() {
        if (roundStarted) {
            return;
        }
        turns++;
        roundMessages.clear();
        roundEvents.clear();
        roundStarted = true;
        if (foe() == null || player() == null) {
            return;
        }
        pbChooseAll(false);                                        // Battle_Phase_Command
        BattleAttackPhase.pbAttackPhasePrologue(this);             // :171-184
        attackPhasePrepared = true;
    }

    /**
     * {@code pbAttackPhaseSwitch}'s order (Battle_Phase_Attack:51-52): the player's battlers that registered a
     * switch, in {@code pbPriority} order.
     */
    public int[] playerSwitchersInOrder() {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (Battler b : pbPriority(false)) {                      // :51
            if (b.fainted() || !pbOwnedByPlayer(b.index)) continue;
            if (":SwitchOut".equals(choices(b.index)[0])) out.add(b.index);   // :52
        }
        int[] ret = new int[out.size()];
        for (int i = 0; i < ret.length; i++) ret[i] = out.get(i);
        return ret;
    }

    /**
     * {@code pbRegisterItem(idxBattler,item,...)} (Battle_Action_UseItem:27-31) for an item the port has already
     * applied (this runtime uses items at command time): the battler's action is spent, so
     * {@link #pbChooseAll(boolean)} leaves it alone and the attack phase does nothing for it.
     */
    public void markItemUsed(int idxBattler) {
        if (idxBattler < 0 || idxBattler >= choicesStore.length) return;
        choicesStore[idxBattler][0] = ":UseItem";                  // :29
        choicesStore[idxBattler][1] = 0;                           // the item is already gone (:91 ch[1]=0)
    }

    /** {@code pbPartyStarts(idxBattler)} (PokeBattle_Battle:319-321): where each trainer's team starts in a side's party. */
    public int[] partyStarts(int side) {
        Array<Battler> party = partyBySide(side & 1);
        int owners = Math.max(1, pbTrainerCount(side & 1));
        int[] starts = new int[owners];
        for (int owner = 1; owner < owners; owner++) {
            starts[owner] = party.size;
            for (int i = 0; i < party.size; i++) {
                if (party.get(i) != null && party.get(i).ownerIndex == owner) {
                    starts[owner] = i;
                    break;
                }
            }
        }
        return starts;
    }

    /**
     * The start of {@code pbCommandPhase} (Battle_Phase_Command:181-190), run once the round is over (nothing
     * reads the choices in between): choices are reset only where commands can be shown, so a battler in the
     * middle of a multi-turn attack keeps its forced choice; a registered but unused Mega Evolution is dropped.
     */
    private void pbClearChoicesForNextRound() {
        for (int i = 0; i <= maxBattlerIndex(); i++) {                         // :181
            Battler b = battlerAt(i);
            if (b == null) continue;                                           // :182
            if (pbCanShowCommands(i)) pbClearChoice(i);                        // :183
        }
        for (int side = 0; side < 2; side++) {                                 // :186
            for (int i = 0; i < megaEvolution[side].length; i++) {            // :187
                if (megaEvolution[side][i] >= 0) megaEvolution[side][i] = -1;  // :188
            }
        }
    }

    /** {@code pbBossBuffPhase} (PokeBattle_BOSS:37-90): runs at the start of every round, before the command phase. */
    public void pbBossBuffPhase() {
        roundMessages.clear();
        roundEvents.clear();
        for (Battler b : eachBattler()) {                                      // :38
            if (b.pbOwnedByPlayer()) continue;                                 // :40
            // BOSS恢复异常状态和清强化
            int rank = b.pokemon.battleRank;                                   // :42
            if (rank > 2) {                                                    // :43
                if (turnCount() == 0) {                                        // :44
                    if (!bossRaiseRandomStat(b)) continue;                     // :45-55
                    continue;                                                  // :57
                }
                int ret = random.nextInt(9000);                                // :59
                int perish = b.effects.intVal(PBEffects.Battler.PerishSong);
                if (perish == 1 || perish == 2) ret = 3999;                    // :60
                if (ret < 4000) {                                              // :61
                    playCry(b.pokemon);                                        // :62-63 pbSEPlay(pbCryFile)
                    if (ret < 1000) {                                          // :64
                        if (!bossRaiseRandomStat(b)) continue;                 // :65-75
                    } else if (ret < 3000) {                                   // :76
                        for (Battler t : b.allOpposing()) {                    // :77 eachOpposing
                            for (int s : PBStats.EACH_BATTLE_STAT) {           // :78
                                if (t.stage(s) > 0) t.setStage(s, 0);
                            }
                            t.effects.set(PBEffects.Battler.FocusEnergy, 0);   // :79
                        }
                        display("<c3=FFEE88,FF6600>" + b.pbThis() + "凭借它的力量\n清除了对手的能力提升！</c3>");   // :81
                    } else if (b.hasAnyNegativeEffects()) {                    // :82
                        b.removeAllNegativeEffects(true);                      // :83
                        displayBrief("<c3=FFEE88,FF6600>" + b.pbThis() + "凭借它的力量\n移除了受到的不好效果！</c3>");   // :84
                    }
                    // :86 pbWait(80)
                }
            }
        }
    }

    /** :45-55 / :65-75: raise one random main stat by 1; false when none can be raised ({@code next}). */
    private boolean bossRaiseRandomStat(Battler b) {
        java.util.List<Integer> randomUp = new java.util.ArrayList<>();
        for (int s : PBStats.EACH_MAIN_BATTLE_STAT) {                          // :46
            if (b.pbCanRaiseStatStage(s, b)) randomUp.add(s);                  // :47
        }
        if (randomUp.isEmpty()) return false;                                  // :49
        int r = pbRandom(randomUp.size());                                     // :50
        int stat = randomUp.get(r);                                            // :51
        b.pbRaiseStatStageBasic(stat, 1, true);                                // :52
        commonAnimation("StatUp", b);                                          // :53
        displayBrief("<c3=FFEE88,FF6600>" + b.pbThis() + "凭借它的力量\n提高了" + PBStats.getName(stat) + "！</c3>");   // :54-55
        return true;
    }

    /**
     * {@code pbAutoChooseMove(idxBattler,showMessages=true)} (Battle_Action_AttacksPriority:36-68), singles:
     * Encore forces its move, otherwise Struggle.
     */
    public boolean pbAutoChooseMove(int idxBattler, boolean showMessages) {
        Battler battler = battlerAt(idxBattler);                               // :37
        if (battler.fainted()) {                                               // :38
            pbClearChoice(idxBattler);                                         // :39
            return true;                                                       // :40
        }
        // Encore
        int idxEncoredMove = battler.pbEncoredMoveIndex();                     // :43
        Object[] c = choicesStore[idxBattler];
        if (idxEncoredMove >= 0 && pbCanChooseMove(idxBattler, idxEncoredMove, false, false)) {   // :44
            BattleMove encoreMove = battler.moveSlot(idxEncoredMove);          // :45
            c[0] = ":UseMove";                                                 // :46
            c[1] = idxEncoredMove;                                             // :47
            c[2] = encoreMove;                                                 // :48
            c[3] = -1;                                                         // :49
            return true;                                                       // :50 singleBattle?
        }
        // Struggle
        if (pbOwnedByPlayer(idxBattler) && showMessages) {                     // :60
            displayPaused(battler.name() + "没有招式可使用了！");                  // :61
        }
        c[0] = ":UseMove";                                                     // :63
        c[1] = -1;                                                             // :64
        c[2] = battler.struggle(pbs);                                          // :65 @struggle
        c[3] = -1;                                                             // :66
        return true;                                                           // :67
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
    /** The Mega Evolution system lives in {@link BattleMega}. */
    public boolean pbCanMegaEvolve(int idxBattler) {
        return BattleMega.pbCanMegaEvolve(this, idxBattler);
    }

    public boolean pbRegisterMegaEvolution(int idxBattler) {
        return BattleMega.pbRegisterMegaEvolution(this, idxBattler);
    }

    public void pbUnregisterMegaEvolution(int idxBattler) {
        BattleMega.pbUnregisterMegaEvolution(this, idxBattler);
    }

    public void pbToggleRegisteredMegaEvolution(int idxBattler) {
        BattleMega.pbToggleRegisteredMegaEvolution(this, idxBattler);
    }

    public boolean pbRegisteredMegaEvolution(int idxBattler) {
        return BattleMega.pbRegisteredMegaEvolution(this, idxBattler);
    }

    public void pbMegaEvolve(int idxBattler) {
        BattleMega.pbMegaEvolve(this, idxBattler);
    }

    public boolean pbHasMegaRing(int idxBattler) {
        return BattleMega.pbHasMegaRing(this, idxBattler);
    }

    public String pbGetMegaRingName(int idxBattler) {
        return BattleMega.pbGetMegaRingName(this, idxBattler);
    }

    public void zaForceExitMega(int side) {
        BattleMega.zaForceExitMega(this, side);
    }

    public void pbZARevertMega(int idxBattler) {
        BattleMega.pbZARevertMega(this, idxBattler);
    }

    public void pbZARevertMegaByPkmn(pokemon.runtime.pokemon.Pokemon pkmn) {
        BattleMega.pbZARevertMegaByPkmn(this, pkmn);
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
    /** {@code @opponent[..].fullname} (PokeBattle_Battle:268): the opposing trainer's full name. */
    public String opponentName;
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
        awardCaptureExperience(foe());
    }

    /** {@code pbGainExp} for a captured battler ({@code b.captured}, Battle_ExpAndMoveLearning:16). */
    public void awardCaptureExperience(Battler captured) {
        if (captured == null) return;
        lastExpAwards.clear();
        awardParticipants(captured);
    }

    /**
     * pbGainExpOne (Battle_ExpAndMoveLearning:99-308) for one participant with
     * this project's settings: {@code SCALED_EXP_FORMULA = true} (Settings:162)
     * and {@code SPLIT_EXP_BETWEEN_GAINERS = false} (Settings:163).
     */
    private void awardExperience(Battler fainted, Battler receiver) {
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
        // pbCanChooseMove? (Battle_Action_AttacksPriority:5-18) with showMessages: the refusal line it would show is
        // the answer; the events it put in the round are not part of this round.
        int messages = roundMessages.size;
        int events = roundEvents.size;
        boolean ok = pbCanChooseMove(idxBattler, slot, true, false);
        String refusal = null;
        if (!ok) {
            refusal = roundMessages.size > messages ? roundMessages.peek() : "";
        }
        roundMessages.truncate(messages);
        roundEvents.truncate(events);
        return refusal;
    }

    // ------------------------------------------------------------------
    // Stage 5 / 2c: the faithful move-choice checks. The older
    // canChooseMove(...) above still serves the menu/AI until 2d replaces
    // it; these are the plugin bodies including Battler#pbCanChooseMove?.
    // ------------------------------------------------------------------

    /** {@code @internalBattle} (PokeBattle_Battle:61, initialised true at :138). */
    public boolean internalBattle = true;

    /** {@code @expGain} (PokeBattle_Battle:68/145): whether Pokemon can gain Exp/EVs; {@code setBattleRule("noExp")} turns it off. */
    public boolean expGain = true;

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
            new DamageState.SuccessState(), new DamageState.SuccessState(),
    };

    /** {@code pbSideSize(index)} (PokeBattle_Battle:215-217). */
    public int pbSideSize(int index) {
        return sideSizes[index & 1];
    }

    /**
     * {@code pbGetOpposingIndicesInOrder(idxBattler)} (PokeBattle_Battle:496-533): the opposing battler indices, the
     * most "opposite" first.
     */
    public int[] pbGetOpposingIndicesInOrder(int idxBattler) {
        switch (pbSideSize(0)) {                                                   // :497
            case 1:
                switch (pbSideSize(1)) {                                           // :499
                    case 1:                                                        // :500 1v1 single
                        if (opposes(idxBattler, 0)) return new int[] {0};          // :501
                        return new int[] {1};                                      // :502
                    case 2:                                                        // :503 1v2
                        if (opposes(idxBattler, 0)) return new int[] {0};          // :504
                        return new int[] {3, 1};                                   // :505
                    case 3:                                                        // :506 1v3
                        if (opposes(idxBattler, 0)) return new int[] {0};          // :507
                        return new int[] {3, 5, 1};                                // :508
                    default:
                        break;
                }
                break;
            case 2:
                switch (pbSideSize(1)) {                                           // :511
                    case 1:                                                        // :512 2v1
                        if (opposes(idxBattler, 0)) return new int[] {0, 2};       // :513
                        return new int[] {1};                                      // :514
                    case 2:                                                        // :515 2v2 double
                        return new int[][] {{3, 1}, {2, 0}, {1, 3}, {0, 2}}[idxBattler];   // :516
                    case 3:                                                        // :517 2v3
                        if (idxBattler < 3) return new int[][] {{5, 3, 1}, {2, 0}, {3, 1, 5}}[idxBattler];   // :518
                        return new int[] {0, 2};                                   // :519
                    default:
                        break;
                }
                break;
            case 3:
                switch (pbSideSize(1)) {                                           // :522
                    case 1:                                                        // :523 3v1
                        if (opposes(idxBattler, 0)) return new int[] {2, 0, 4};    // :524
                        return new int[] {1};                                      // :525
                    case 2:                                                        // :526 3v2
                        return new int[][] {{3, 1}, {2, 4, 0}, {3, 1}, {2, 0, 4}, {1, 3}}[idxBattler];   // :527
                    case 3:                                                        // :528 3v3 triple
                        return new int[][] {{5, 3, 1}, {4, 2, 0}, {3, 5, 1}, {2, 0, 4}, {1, 3, 5}, {0, 2, 4}}[idxBattler];   // :529
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return new int[] {idxBattler};                                             // :532
    }

    /** {@code nearBattlers?(idxBattler1,idxBattler2)} (PokeBattle_Battle:544-568). */
    public boolean nearBattlers(int idxBattler1, int idxBattler2) {
        if (idxBattler1 == idxBattler2) return false;                              // :545
        if (pbSideSize(0) <= 2 && pbSideSize(1) <= 2) return true;                 // :546
        // Get all pairs of battler positions that are not close to each other
        java.util.List<int[]> pairs = new java.util.ArrayList<>();
        pairs.add(new int[] {0, 4});                                               // :548 Covers 3v1 and 1v3
        pairs.add(new int[] {1, 5});
        switch (pbSideSize(0)) {                                                   // :549
            case 3:
                switch (pbSideSize(1)) {                                           // :551
                    case 3:                                                        // :552 3v3 (triple)
                        pairs.add(new int[] {0, 1});                               // :553
                        pairs.add(new int[] {4, 5});                               // :554
                        break;
                    case 2:                                                        // :555 3v2
                        pairs.add(new int[] {0, 1});                               // :556
                        pairs.add(new int[] {3, 4});                               // :557
                        break;
                    default:
                        break;
                }
                break;
            case 2:                                                                // :559 2v3
                pairs.add(new int[] {0, 1});                                       // :560
                pairs.add(new int[] {2, 5});                                       // :561
                break;
            default:
                break;
        }
        // See if any pair matches the two battlers being assessed
        for (int[] pair : pairs) {                                                 // :564
            boolean has1 = pair[0] == idxBattler1 || pair[1] == idxBattler1;
            boolean has2 = pair[0] == idxBattler2 || pair[1] == idxBattler2;
            if (has1 && has2) return false;                                        // :565
        }
        return true;                                                               // :567
    }

    /**
     * {@code pbSwapBattlers(idxA,idxB)} (PokeBattle_Battle:593-624): two battlers of the same trainer change
     * positions. 登记: :601 {@code @scene.pbSwapBattlerSprites} - the screen reads the battlers' slots.
     */
    public boolean pbSwapBattlers(int idxA, int idxB) {
        if (battlerAt(idxA) == null || battlerAt(idxB) == null) return false;      // :594
        // Can't swap if battlers aren't owned by the same trainer
        if (opposes(idxA, idxB)) return false;                                     // :596
        if (pbGetOwnerIndexFromBattlerIndex(idxA) != pbGetOwnerIndexFromBattlerIndex(idxB)) return false;   // :597
        int tmpParty = fieldParty[idxA];                                           // :598
        fieldParty[idxA] = fieldParty[idxB];
        fieldParty[idxB] = tmpParty;
        Object[] tmpChoice = choicesStore[idxA];                                   // :600
        choicesStore[idxA] = choicesStore[idxB];
        choicesStore[idxB] = tmpChoice;
        refreshFieldIndices();                                                     // :599 the battlers' @index
        roundEvents.add(RoundEvent.swapSprites(idxA, idxB));                       // :601 @scene.pbSwapBattlerSprites(idxA,idxB)
        // Swap the target of any battlers' effects that point at either of the swapped battlers
        // NOTE: LeechSeed is not swapped, because drained HP goes to whichever Pokemon is in the position.
        int[] effectsToSwap = {PBEffects.Battler.Attract, PBEffects.Battler.BideTarget, PBEffects.Battler.CounterTarget,
                PBEffects.Battler.LockOnPos, PBEffects.Battler.MeanLook, PBEffects.Battler.MirrorCoatTarget,
                PBEffects.Battler.SkyDrop, PBEffects.Battler.TrappingUser};         // :609-616
        for (Battler b : eachBattler()) {                                          // :617
            for (int i : effectsToSwap) {                                          // :618
                if (!b.effects.has(i)) continue;
                int value = b.effects.intVal(i);
                if (value != idxA && value != idxB) continue;                      // :619
                b.effects.set(i, value == idxA ? idxB : idxA);                     // :620
            }
        }
        return true;                                                               // :623
    }

    /** {@code pbCanShift?(idxBattler)} (Battle_Action_Other:5-21). */
    public boolean pbCanShift(int idxBattler) {
        if (pbSideSize(0) <= 2 && pbSideSize(1) <= 2) return false;                // :6 Double battle or smaller
        int idxOther = -1;                                                         // :7
        switch (pbSideSize(idxBattler)) {                                          // :8
            case 1:
                return false;                                                      // :10 Only one battler on that side
            case 2:
                idxOther = (idxBattler + 2) % 4;                                   // :12
                break;
            case 3:
                if (idxBattler == 2 || idxBattler == 3) return false;              // :14 In middle spot already
                idxOther = ((idxBattler % 2) == 0) ? 2 : 3;                        // :15
                break;
            default:
                break;
        }
        if (pbGetOwnerIndexFromBattlerIndex(idxBattler) != pbGetOwnerIndexFromBattlerIndex(idxOther)) return false;   // :17
        Battler a = battlerAt(idxBattler);
        Battler b = battlerAt(idxOther);
        if (a == null || b == null) return false;
        if (a.effects.truthy(PBEffects.Battler.Commander) || b.effects.truthy(PBEffects.Battler.Commander)) return false;   // :18-19
        return true;                                                               // :20
    }

    /** {@code pbRegisterShift(idxBattler)} (Battle_Action_Other:23-28). */
    public boolean pbRegisterShift(int idxBattler) {
        Object[] c = choicesStore[idxBattler];
        c[0] = ":Shift";                                                           // :24
        c[1] = 0;                                                                  // :25
        c[2] = null;                                                               // :26
        return true;                                                               // :27
    }

    /**
     * {@code pbEORShiftDistantBattlers} (Battle_Phase_EndOfRound:152-206): battlers that are not near any foe move to
     * the middle position. The plugin's own {@code for side} loop and its leftover {@code side} are kept.
     */
    public void pbEORShiftDistantBattlers() {
        if (singleBattle()) return;                                                // :156
        java.util.List<int[]> swaps = new java.util.ArrayList<>();                 // :157
        int side;
        for (side = 0; side < 2; side++) {                                         // :158
            if (pbSideSize(side) == 1) continue;                                   // :159
            boolean anyNear = false;                                               // :161
            for (Battler b : eachSameSideBattler(side)) {                          // :162
                for (Battler otherB : eachOtherSideBattler(b.index)) {             // :163
                    if (!nearBattlers(otherB.index, b.index)) continue;            // :164
                    anyNear = true;                                                // :165
                    break;                                                         // :166
                }
                if (anyNear) break;                                                // :168
            }
            if (anyNear) break;                                                    // :170
            for (Battler b : eachSameSideBattler(side)) {                          // :180
                int pos = -1;                                                      // :182
                switch (pbSideSize(side)) {                                        // :183
                    case 2: pos = new int[] {2, 3, 0, 1}[b.index]; break;          // :184
                    case 3: pos = side == 0 ? 2 : 3; break;                        // :185
                    default: break;
                }
                if (pos < 0) continue;                                             // :187
                int idxOwner = pbGetOwnerIndexFromBattlerIndex(b.index);           // :189
                if (pbGetOwnerIndexFromBattlerIndex(pos) != idxOwner) continue;    // :190
                swaps.add(new int[] {b.index, pos});                               // :191
            }
        }
        if (side > 1) side = 1;                                                    // Ruby's for variable keeps the last value
        for (int[] pair : swaps) {                                                 // :195
            if (pbSideSize(pair[0]) == 2 && swaps.size() > 1) continue;            // :196
            if (!pbSwapBattlers(pair[0], pair[1])) continue;                       // :197
            Battler moved = battlerAt(pair[1]);
            switch (pbSideSize(side)) {                                            // :198
                case 2: pbDisplay(moved.pbThis() + "移动到了另一边！"); break;          // :199-200
                case 3: pbDisplay(moved.pbThis() + "移动到了中心！"); break;            // :201-202
                default: break;
            }
        }
    }

    /** {@code singleBattle?} (PokeBattle_Battle:211-213). */
    public boolean singleBattle() {
        return pbSideSize(0) == 1 && pbSideSize(1) == 1;
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
        for (int idx = 0; idx <= maxBattlerIndex(); idx++) {                         // :13 @battlers.each
            Battler b = battlerAt(idx);
            if (b == null || !opposes(idx, 0)) continue;                             // :14 next unless b && b.opposes?
            if (b.participants.isEmpty()) continue;                                  // :15
            if (!b.fainted() || b.expAwarded) continue;                              // :16 next unless b.fainted? || b.captured
            awardParticipants(b);
        }
    }

    /**
     * Battle_ExpAndMoveLearning:17-65 for one defeated battler: every able participant of the player's own team
     * gains Exp ({@code SPLIT_EXP_BETWEEN_GAINERS} is false, so none is split), then the participants are cleared.
     * 登记: :11/:23-32/:45-56 Exp All and Exp Share are not modelled; :42 pbGainEVsOne is not here.
     */
    private void awardParticipants(Battler b) {
        if (!internalBattle || !expGain) return;                                     // Battle_ExpAndMoveLearning:8
        for (int partic : new java.util.ArrayList<>(b.participants)) {               // :19
            Array<Battler> p1 = partyOf(0);
            if (partic < 0 || partic >= p1.size) continue;
            Battler receiver = p1.get(partic);
            if (receiver == null || receiver.fainted() || receiver.pokemon.egg) continue;   // :20 able?
            if (!pbIsOwner(0, partic)) continue;                                     // :20
            awardExperience(b, receiver);                                            // :43
        }
        b.expAwarded = true;                                                         // :64 b.participants = []
        b.participants.clear();
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

    /** {@code pbChooseTarget}'s data for the screen's target menu (Scene_Commands:371-417). */
    public String[] pbCreateTargetTexts(int idxBattler, int targetType) {
        return BattleCommandPhase.pbCreateTargetTexts(this, idxBattler, targetType);
    }

    /** {@code pbFirstTarget(idxBattler,targetType)} (Scene_Commands:395-417). */
    public int pbFirstTarget(int idxBattler, int targetType) {
        return BattleCommandPhase.pbFirstTarget(this, idxBattler, targetType);
    }

    /** {@code move.pbTarget(battler)} (the target type of the move the battler would use). */
    public int pbMoveTargetType(int idxBattler, int slot) {
        Battler battler = battlerAt(idxBattler);
        BattleMove move = battler == null ? null : battler.moveSlot(slot);
        if (move == null) return PBTargets.None;
        return pokemon.runtime.battle.movefx.MoveEffectRegistry.of(move.function()).pbTarget(move, battler);
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
    /**
     * Battle_Phase_Command: every battler on the field stores its choice. A battler that already has an action
     * (a registered move, switch, item or run) keeps it. The player's own battlers are skipped when
     * {@code includePlayer} is false (the screen played their action).
     */
    private void pbChooseAll(boolean includePlayer) {
        for (int idx = 0; idx <= maxBattlerIndex(); idx++) {
            Battler b = battlerAt(idx);
            if (b == null) continue;
            boolean player = pbOwnedByPlayer(idx);
            if (player && !includePlayer) continue;
            chooseFor(b, b.pbDirectOpposing(false), player ? playerController : null);
        }
    }

    private void chooseFor(Battler user, Battler foe, Controller controller) {
        if (user == null || user.index < 0 || user.index >= choicesStore.length) {
            return;
        }
        Object[] c = choicesStore[user.index];
        if (":SwitchOut".equals(c[0]) || ":UseItem".equals(c[0]) || ":Shift".equals(c[0]) || ":Run".equals(c[0])) {
            return;                                                // registered by pbRegisterSwitch / item / shift / run
        }
        if (":UseMove".equals(c[0]) && c[2] != null && user.hasUsableMove()) {
            return;                                                // registered by pbRegisterMove
        }
        // CFRU AI_TrySwitchOrUseItem (ai_master.c:948): a trainer's Pokemon may switch instead of attacking
        if (controller == null && user.foe && trainerBattle && singleBattle() && !user.fainted()) {
            int idxSwitch = AiSwitching.decide(this, user, random);
            if (idxSwitch >= 0 && registerSwitch(user.index, idxSwitch)) {
                return;
            }
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
        // CFRU ai_master.c ChooseMoveOrAction_Singles: trainers (and wild Bosses) in a single battle use the move scorer;
        // the simple AI below stays as its fallback (doubles are phase 4).
        if (user.foe && singleBattle() && (trainerBattle || (user.pokemon != null && user.pokemon.battleRank >= 2))) {
            int slot = AiMaster.chooseMove(this, user, random);
            BattleMove chosen = user.moveSlot(slot);
            if (chosen != null) {
                return chosen;
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
            float typeMod = pbs == null || foe == null ? 1f : pbs.effectiveness(move.type(), foe.types());
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
            if (!battler.removedFromParty) battler.syncHp();      // PokeBattle_Battle:576 party[idxParty] = nil: a caught Pokemon is no longer the battler's
        }
        BattleMega.pbEndOfBattle(this);                            // ZA模式:207-222
        return new BattleResult(outcome, turns, caughtPokemon.isEmpty() ? null : caughtPokemon.get(0));
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
        for (int idx = 0; idx <= maxBattlerIndex(); idx++) {         // :438/:441
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
        for (int idx = 0; idx <= maxBattlerIndex(); idx++) {
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

    /** {@code pbGetReplacementPokemonIndex(idxBattler,random=false)} (Battle_Action_Switching:241-253). */
    public int pbGetReplacementPokemonIndex(int idxBattler) {
        return BattleSwitchAction.pbGetReplacementPokemonIndex(this, idxBattler, false);
    }

    /** {@code pbGetReplacementPokemonIndex(idxBattler,random)} (Battle_Action_Switching:241-253). */
    public int pbGetReplacementPokemonIndex(int idxBattler, boolean random) {
        return BattleSwitchAction.pbGetReplacementPokemonIndex(this, idxBattler, random);
    }

    /** {@code pbRecallAndReplace(idxBattler,idxParty)} (Battle_Action_Switching:256-262). */
    public void pbRecallAndReplace(int idxBattler, int idxParty) {
        BattleSwitchAction.pbRecallAndReplace(this, idxBattler, idxParty, false, false);
    }

    /** {@code pbRecallAndReplace(idxBattler,idxParty,randomReplacement,batonPass)} (Battle_Action_Switching:256-262). */
    public void pbRecallAndReplace(int idxBattler, int idxParty, boolean randomReplacement, boolean batonPass) {
        BattleSwitchAction.pbRecallAndReplace(this, idxBattler, idxParty, randomReplacement, batonPass);
    }

    /** {@code pbEORSwitch(favorDraws=false)} (Battle_Action_Switching:165-239). */
    public void pbEORSwitch(boolean favorDraws) {
        BattleSwitchAction.pbEORSwitch(this, favorDraws);
    }

    /** {@code pbSwitchInBetween(idxBattler,checkLaxOnly=false,canCancel=false)} (Battle_Action_Switching:155-158). */
    public int pbSwitchInBetween(int idxBattler, boolean checkLaxOnly, boolean canCancel) {
        return BattleSwitchAction.pbSwitchInBetween(this, idxBattler, checkLaxOnly, canCancel);
    }

    /** {@code pbMessageOnRecall(battler)} (Battle_Action_Switching:264-281). */
    public void pbMessageOnRecall(Battler battler) {
        BattleSwitchAction.pbMessageOnRecall(this, battler);
    }

    /** {@code pbRun(idxBattler,duringBattle=false)} (Battle_Action_Running:36-157). */
    public int pbRun(int idxBattler, boolean duringBattle) {
        return BattleSwitchAction.pbRun(this, idxBattler, duringBattle);
    }

    /** {@code @runCommand} (PokeBattle_Battle): how often the player tried to flee. */
    public int runCommand;

    /** {@code @caughtPokemon} (PokeBattle_BattleCommon:60-63): the Pokemon caught this battle, stored at the end. */
    public final java.util.List<Pokemon> caughtPokemon = new java.util.ArrayList<>();

    /**
     * What {@link #pbThrowPokeBall} needs from outside the engine: the player's name ({@code pbPlayer.name}), the
     * ball's names and type ({@code PBItems.getName}, {@code pbGetBallType}), the Bag ({@code $PokemonBag}) and
     * the capture formula's inputs ({@code pbCaptureCalc}, PokeBattle_BattleCommon:170-232). Null in a battle
     * without a player ({@link HeadlessScene} never picks a ball).
     */
    public interface CaptureHooks {
        String playerName();

        String itemName(String ball);

        int ballType(String ball);

        /** {@code $PokemonBag.pbDeleteItem(ball,1)}. */
        void deleteItem(String ball);

        /** {@code pbCaptureCalc(pkmn,battler,rareness,ball)}: the number of shakes (4 = capture). */
        int pbCaptureCalc(Battler target, String ball, int rareness);

        /** {@code BallHandlers.onCatch(ball,self,pkmn)} (:146). */
        void onCatch(String ball, Pokemon pkmn);
    }

    public CaptureHooks captureHooks;

    /**
     * {@code pbRemoveFromParty(idxBattler,idxParty)} (PokeBattle_Battle:573-591): {@code party[idxParty] = nil}.
     * 登记: this runtime's party is a list of {@link Battler}s that the iteration sites do not expect to be null,
     * so the entry stays and is flagged ({@link Battler#removedFromParty}); {@link #allFainted(int)} and the
     * HP write-back skip it. The party order bookkeeping (:579-590) has no counterpart here.
     */
    public void pbRemoveFromParty(int idxBattler, int idxParty) {
        Array<Battler> party = partyOf(idxBattler);                                // :574
        if (idxParty >= 0 && idxParty < party.size && party.get(idxParty) != null) {
            party.get(idxParty).removedFromParty = true;                           // :576
        }
    }

    /**
     * {@code pbThrowPokeBall(idxBattler,ball,rareness=nil,showPlayer=false)} (PokeBattle_BattleCommon:68-164).
     * {@code rareness < 0} stands for Ruby's {@code nil}. The ball's own scene calls ({@code pbThrow},
     * {@code pbThrowAndDeflect}, {@code pbThrowSuccess}/{@code pbHideCaptureBall}) are the BALL_* round events.
     * 登记: {@code pbIsSnagBall?} (:100, :146-149) - no Shadow Pokemon / Snag Ball in this runtime;
     * {@code BallHandlers.onFailCatch} (:119-128) - no ball in this project's list has one;
     * {@code @criticalCapture} (:110/:114) - the capture calculation here has no critical capture;
     * {@code @peer.pbOnLeavingBattle} (:159) and {@code forcedForm=nil} (:158) have no counterpart.
     */
    public void pbThrowPokeBall(int idxBattler, String ball, int rareness, boolean showPlayer) {
        CaptureHooks hooks = captureHooks;
        // Determine which Pokémon you're throwing the Poké Ball at
        Battler battler;
        if (opposes(idxBattler, 0)) {                                              // :71 opposes?(idxBattler)
            battler = battlerAt(idxBattler);                                       // :72
        } else {
            battler = battlerAt(idxBattler).pbDirectOpposing(true);                // :75
        }
        if (battler.fainted()) {                                                   // :77
            Battler[] ally = new Battler[1];
            battler.eachAlly(b -> { if (ally[0] == null) ally[0] = b; });          // :78-81 first ally
            if (ally[0] != null) battler = ally[0];
        }
        // Messages
        String itemName = hooks.itemName(ball);                                    // :84
        if (battler.fainted()) {                                                   // :85
            display(hooks.playerName() + "扔出了" + itemName + "！");               // :86-90
            display("但是没有目标……");                                              // :91
            return;                                                                // :92
        }
        displayBrief(hooks.playerName() + "扔出了" + itemName + "！");              // :93-97
        int ballType = hooks.ballType(ball);
        // Animation of opposing trainer blocking Poké Balls (unless it's a Snag Ball at a Shadow Pokémon)
        if (trainerBattle) {                                                       // :100
            roundEvents.add(RoundEvent.ball(RoundEvent.Kind.BALL_DEFLECT, ballType, 0, false, battler.index));   // :101 pbThrowAndDeflect(ball,1)
            display("训练家打飞了球\n不要做小偷！");                                    // :102
            return;                                                                // :103
        } else if (gameSwitches.test(60)) {                                        // :104 不可捕捉的野外对战
            display("精灵球被破坏了！\n看来只能战胜它了！");                            // :105
            return;                                                                // :106
        }
        // Calculate the number of shakes (4=capture)
        Pokemon pkmn = battler.pokemon;                                            // :109
        int numShakes = hooks.pbCaptureCalc(battler, ball, rareness);              // :111
        // Animation of Ball throw, absorb, shake and capture/burst out
        roundEvents.add(RoundEvent.ball(RoundEvent.Kind.BALL_THROW, ballType, numShakes, false, battler.index));   // :114 @scene.pbThrow
        // Outcome message
        switch (numShakes) {                                                       // :116
            case 0:
                display("哦不！\n宝可梦逃出来了！");                                  // :118
                break;
            case 1:
                display("啊！\n还以为能抓住呢……");                                   // :121
                break;
            case 2:
                display("好可惜...\n差一点就能成功了");                               // :124
                break;
            case 3:
                display("真可惜…\n明明只差一点点了。");                                // :127
                break;
            case 4:
                displayBrief("太好了！\n捉到了" + pkmn.name + "！");                  // :130
                roundEvents.add(RoundEvent.ball(RoundEvent.Kind.BALL_SUCCESS, ballType, 4, false, battler.index));   // :131 pbThrowSuccess (+ :161 pbHideCaptureBall)
                pbRemoveFromParty(battler.index, battler.pokemonIndex);            // :132
                // Gain Exp
                awardCaptureExperience(battler);                                   // :134-138 GAIN_EXP_FOR_CAPTURE: captured=true; pbGainExp
                battler.pbReset();                                                 // :139
                if (trainerBattle) {                                               // :140
                    if (pbAllFainted(battler.index)) decision = 1;                 // :141
                } else {
                    if (pbAllFainted(battler.index)) decision = 4;                 // :143 Battle ended by capture
                }
                // Modify the Pokémon's properties because of the capture
                hooks.onCatch(ball, pkmn);                                         // :150 BallHandlers.onCatch
                if (gameSwitches.test(60)) pkmn.level = 1;                         // :151
                pkmn.ballused = ballType;                                          // :152 pkmn.ballused = pbGetBallType(ball)
                pkmn.makeUnmega(pbs);                                              // :153
                pkmn.makeUnprimal(pbs);                                            // :154
                pkmn.recordFirstMoves();                                           // :156 pbRecordFirstMoves
                // Save the Pokémon for storage at the end of battle
                caughtPokemon.add(pkmn);                                           // :163
                break;
            default:
                break;
        }
    }

    /** {@code pbDisplayConfirm(msg)} (PokeBattle_Battle:785-787): {@code @scene.pbDisplayConfirmMessage(msg)}. */
    public boolean pbDisplayConfirm(String msg) {
        return scene.pbDisplayConfirmMessage(msg);
    }

    /**
     * {@code @scene}'s calls that wait for the player or an animation. The battle screen's implementation
     * hands each call to the screen and the engine waits for it ({@link EngineCoroutine}); the default answers
     * them without a screen.
     */
    public interface Scene {
        /** {@code @scene.pbPartyScreen(idxBattler,canCancel){ |idxParty,partyScene| ... }}: the block returns the refusal line or null. */
        void pbPartyScreen(int idxBattler, boolean canCancel, java.util.function.IntFunction<String> block);

        /** {@code @scene.pbDisplayConfirmMessage(msg)}. */
        boolean pbDisplayConfirmMessage(String msg);

        /** {@code @scene.pbRecall(idxBattler)} (Scene_Animations:148-166). */
        void pbRecall(int idxBattler);

        /** {@code @scene.pbShowPartyLineup(side)}. */
        void pbShowPartyLineup(int side);

        /** {@code @scene.pbSendOutBattlers(sendOuts,startBattle)} (Scene_Animations:85-143). */
        void pbSendOutBattlers(int[] idxBattlers, boolean startBattle);

        /**
         * {@code PokemonBagScreen#pbChooseItemScreen(Proc{|item| pbIsPokeBall?(item)})} (PokeBattle_BOSS:168-169): the
         * Bag opened to pick a Poke Ball. The internal name of the ball, or null when the player backed out
         * ({@code ball>0} is false, :170/:177).
         */
        default String pbChooseBallFromBag() {
            return null;
        }
    }

    /** One scene call that waits (what {@link Scene} hands over to the battle screen). */
    public static final class SceneCall {
        public enum Kind { PARTY_SCREEN, CONFIRM, RECALL, SHOW_PARTY_LINEUP, SEND_OUT, CHOOSE_BALL }

        public final Kind kind;
        public final int idxBattler;
        public final boolean canCancel;
        public final java.util.function.IntFunction<String> validator;
        public final String text;
        public final int[] idxBattlers;
        public final boolean startBattle;
        /** What the screen answers: the confirm's Boolean; nothing for the others. */
        public Object result;

        SceneCall(Kind kind, int idxBattler, boolean canCancel, java.util.function.IntFunction<String> validator,
                String text, int[] idxBattlers, boolean startBattle) {
            this.kind = kind;
            this.idxBattler = idxBattler;
            this.canCancel = canCancel;
            this.validator = validator;
            this.text = text;
            this.idxBattlers = idxBattlers;
            this.startBattle = startBattle;
        }
    }

    /** The scene without a screen: the first Pokemon the block accepts, "yes" to every question. */
    public static final class HeadlessScene implements Scene {
        @Override public void pbPartyScreen(int idxBattler, boolean canCancel, java.util.function.IntFunction<String> block) {
            int count = 6;
            for (int i = 0; i < count; i++) {
                if (block.apply(i) == null) {
                    return;
                }
            }
        }

        @Override public boolean pbDisplayConfirmMessage(String msg) {
            return true;
        }

        @Override public void pbRecall(int idxBattler) {
        }

        @Override public void pbShowPartyLineup(int side) {
        }

        @Override public void pbSendOutBattlers(int[] idxBattlers, boolean startBattle) {
        }
    }

    /** {@code @scene}. */
    public Scene scene = new HeadlessScene();

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
        java.util.List<Integer> inBattleIndices = new java.util.ArrayList<>();   // :342
        for (Battler b : eachSameSideBattler(idxBattler)) {        // :343
            inBattleIndices.add(b.pokemonIndex);
        }
        int count = 0;                                             // :344
        for (int idxParty = 0; idxParty < party.size; idxParty++) {   // :345
            Battler pkmn = party.get(idxParty);
            if (pkmn == null || pkmn.fainted() || pkmn.pokemon.egg) continue;   // :346 !pkmn.able?
            if (inBattleIndices.contains(idxParty)) continue;      // :347
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
        int idxTrainer = pbGetOwnerIndexFromBattlerIndex(idxBattler);   // :267
        if (opposes(idxBattler, 0)) {                               // :268 Opponent
            return idxTrainer > 1 ? opponentName3 : (idxTrainer > 0 ? opponentName2 : opponentName);   // Trainer#fullname (set by the battle port); null in a wild battle
        }
        if (idxTrainer > 0) return partnerName;                     // :269 Ally trainer
        return playerName;                                          // :270 Player
    }

    /** {@code @opponent[1].fullname} (the second opposing trainer). */
    public String opponentName2;
    /** {@code @opponent[2].fullname} (the third opposing trainer). */
    public String opponentName3;
    /** {@code @player[1].fullname} (the partner trainer). */
    public String partnerName;

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
