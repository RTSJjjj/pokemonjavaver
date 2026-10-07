package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;

/**
 * Stage 3 / P2: the live state of one Pokemon in a battle (singles). Holds the
 * current HP and stat stages; the Pokemon itself stays the source of truth for
 * level, IVs and moves, so the party / save keep working outside a battle.
 */
public final class Battler {

    /** Essentials stat stages: -6..6, index 0 (neutral) maps to 2/2. */
    private static final int[] STAGE_MUL = {2, 2, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] STAGE_DIV = {8, 7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 2, 2};

    public final Pokemon pokemon;
    public final boolean foe;
    public int hp;
    /**
     * {@code @index} (PokeBattle_Battler): the field slot this battler occupies,
     * {@code 2*slot+side} as {@code pbSetUpSides} assigns it
     * (Battle_StartAndEnd:122/180). -1 while the battler is benched. The scene
     * keys its sprites on it ({@code @sprites["pokemon_#{battler.index}"]}).
     */
    public int index = -1;
    /** Battle stat stages for ATTACK, DEFENSE, SPEED, SPATK, SPDEF (indices 0..4). */
    public final int[] stages = new int[5];
    /** Accuracy (5) and evasion (6) stages, per PBStats::ACCURACY/EVASION. */
    public final int[] hitStages = new int[2];

    // --- Essentials status conditions (PokeBattle_Battler) ---
    /** Primary status: "" / SLEEP / POISON / BURN / PARALYSIS / FROZEN. */
    public String status;
    /** Toxic counter for badly poisoned (0 = regular poison). */
    public int toxic;
    /** Remaining sleep turns (SLEEP only). */
    public int sleepTurns;
    /** Confusion turns left and flinch (cleared at end of the turn). */
    public int confusion;
    public boolean flinched;
    /** Focus Energy raises the critical-hit rate. */
    public boolean focusEnergy;
    /**
     * {@code @battle.trainerBattle?} (PokeBattle_Battler:216): set by
     * {@link Battle} when the battler joins it, which {@link #thisName()} reads.
     */
    public boolean trainerBattle;
    /**
     * {@code @turnCount} (PokeBattle_Battler:25, Battler_Initialize:176): how
     * many attack phases this battler has been on the field for, incremented at
     * the end of each one (Battle_Phase_Attack:174). The recall lines read it
     * (Battle_Action_Switching:270-273).
     */
    public int turnCount;

    public Battler(Pokemon pokemon, boolean foe) {
        this.pokemon = pokemon;
        this.foe = foe;
        // ↓↓↓ Stage 4 §4 wiring: Battler_Initialize:83-84
        //     @ability = pkmn.ability / @item = pkmn.item.
        //     Both are attr_accessors that handlers rewrite during a battle (Mummy,
        //     Wandering Spirit, Recycle, ...), but their INITIAL value was never
        //     copied from the Pokemon, so `battler.ability`/`battler.item` stayed ""
        //     and every BattleHandlers lookup keyed on them
        //     (triggerAbilityOnSwitchIn(user.ability, ...), triggerTargetItemOnHit(
        //     target.item, ...)) found no handler at all. (insert-only)
        this.ability = pokemon.ability == null ? "" : pokemon.ability;
        this.item = pokemon.item == null ? "" : pokemon.item;
        // ↑↑↑ end of inserted wiring
        this.hp = Math.max(0, pokemon.hp);
        this.status = pokemon.status == null ? "" : pokemon.status;
        if ("SLEEP".equals(status)) {
            this.sleepTurns = 3;
        }
        this.faintedFlag = this.hp == 0;                         // Battler_Initialize:157 @fainted = (@hp==0)
    }

    /**
     * The plugin's {@code @fainted} ("has already fainted properly",
     * Battler_ChangeSelf:70) - not {@code fainted?} (PokeBattle_Battler:97,
     * {@code @hp<=0}). Only {@code pbInitEffects} writes it
     * (Battler_Initialize:157 {@code @fainted = (@hp==0)}), which
     * {@code pbFaint} calls at Battler_ChangeSelf:74, so a second
     * {@code pbFaint} on the same battler returns at :70.
     */
    public boolean faintedFlag;

    /** Battle status helpers (the Essentials pbCan* checks, without abilities). */
    public boolean hasStatus(String id) {
        return id != null && id.equals(status);
    }

    public boolean statused() {
        return status != null && !status.isEmpty();
    }

    public boolean canPoison() {
        return !flinched && !statused() && !"POISON".equals(status) && !hasType("POISON") && !hasType("STEEL");
    }

    public boolean canBurn() {
        return !statused() && !hasType("FIRE");
    }

    public boolean canParalyze() {
        return !statused() && !hasType("ELECTRIC");
    }

    public boolean canFreeze() {
        return !statused() && !hasType("ICE");
    }

    public boolean canSleep() {
        return !statused();
    }

    public void setStatus(String id) {
        status = id == null ? "" : id;
        pokemon.status = status;
        if ("POISON".equals(status)) {
            toxic = 0;
        }
    }

    public void cureStatus() {
        status = "";
        toxic = 0;
        sleepTurns = 0;
        pokemon.status = "";
    }

    public int level() {
        return pokemon.level;
    }

    public int maxHp() {
        return pokemon.maxHp();
    }

    public boolean fainted() {
        return hp <= 0;
    }

    public String name() {
        return pokemon.name != null && !pokemon.name.isEmpty()
                ? pokemon.name : pokemon.species.name;
    }

    /**
     * {@code pbThis} (PokeBattle_Battler:214-231): how the battle lines address
     * this battler. A wild opponent is "野生的{X}", a Boss is "特殊的{X}" or
     * "强大的{X}", and a trainer's Pokemon is "对手的{X}"; the player's own
     * Pokemon uses its nickname alone.
     *
     * <p>{@code @battle.trainerBattle?} is read from the battle this battler was
     * added to (the runtime's battlers learn it from {@link Battle#trainerBattle}).</p>
     */
    public String thisName() {
        if (!foe) {
            return name();
        }
        if (trainerBattle) {
            return "对手的" + name();
        }
        int rank = pokemon == null ? 0 : pokemon.battleRank;
        if (rank > 2) {
            return "强大的" + name();
        }
        if (rank > 1) {
            return "特殊的" + name();
        }
        return "野生的" + name();
    }

    public Array<String> types() {
        return pokemon.types();
    }

    public boolean hasType(String type) {
        return type != null && pokemon.types().contains(type, false);
    }

    public static float stageMultiplier(int stage) {
        int index = Math.max(-6, Math.min(6, stage)) + 6;
        return (float) STAGE_MUL[index] / STAGE_DIV[index];
    }

    public int attack() {
        return attack(false);
    }

    /**
     * {@code pbGetAttackStats} (Move_Usage_Calculations:266-271): on a critical
     * hit the attack stage is treated as 6, so the attacker's lowered Attack is
     * ignored.
     */
    public int attack(boolean critical) {
        int value = scale(pokemon.attack(), critical ? 6 : stages[0]);
        // Burn halves physical Attack (except with Guts, which is not modelled).
        if (hasStatus("BURN")) {
            value = Math.max(1, value / 2);
        }
        return value;
    }

    public int defense() {
        return defense(false);
    }

    /** {@code pbGetDefenseStats} (:272-277): a critical hit ignores raised Defense. */
    public int defense(boolean critical) {
        return scale(pokemon.defense(), critical ? 6 : stages[1]);
    }

    /**
     * {@code pbSpeed} (PokeBattle_Battler:268-278): paralysis halves Speed with
     * {@code NEWEST_BATTLE_MECHANICS} (Settings:160) and quarters it otherwise.
     */
    public int speed() {
        int value = scale(pokemon.speed(), stages[2]);
        if (hasStatus("PARALYSIS")) {
            value = Math.max(1, Battle.NEWEST_BATTLE_MECHANICS ? value / 2 : value / 4);
        }
        return value;
    }

    public int spAtk() {
        return spAtk(false);
    }

    public int spAtk(boolean critical) {
        return scale(pokemon.spAtk(), critical ? 6 : stages[3]);
    }

    public int spDef() {
        return spDef(false);
    }

    public int spDef(boolean critical) {
        return scale(pokemon.spDef(), critical ? 6 : stages[4]);
    }

    /** Accuracy / evasion stage multiplier (PBStats indices 6/7). */
    public int accuracyStage() {
        return hitStages[0];
    }

    public int evasionStage() {
        return hitStages[1];
    }

    public static float hitStageMultiplier(int stage) {
        return stageMultiplier(stage);
    }

    private static int scale(int base, int stage) {
        return (int) Math.floor(base * stageMultiplier(stage));
    }

    /** {@code PokeBattle_SceneMenus::MAX_MOVES} (:222). */
    public static final int MOVES_MAX = 4;

    /**
     * {@code @moves} (Battler_Initialize:98-101): {@code @moves[i] =
     * PokeBattle_Move.pbFromPBMove(@battle,pkmn.moves[i])}, filled up to
     * {@code MAX_MOVES}. The plugin's move list always has four entries - a blank
     * one is {@code PBMove.new(0)}, i.e. {@code id == 0}
     * (PokeBattle_Pokemon:498-521) - and every battle menu index is a SLOT index
     * (Scene_Commands:136 yields {@code cw.index};
     * Battle_Phase_Command:84-85 refuses {@code moves[cmd].id<=0}). This runtime's
     * {@code Pokemon.moves} only holds the real entries, so a slot without one is
     * null here. There is deliberately no PP filter: a 0 PP move still occupies
     * its slot (Battle_Action_AttacksPriority:9-11 is what refuses to use it).
     *
     * @return exactly four entries, in slot order; a blank slot is null
     */
    public Array<BattleMove> moveSlots() {
        Array<BattleMove> slots = new Array<>(MOVES_MAX);
        for (int i = 0; i < MOVES_MAX; i++) {
            slots.add(moveSlot(i));
        }
        return slots;
    }

    /** The move in one slot, or null for a blank slot ({@code id==0}). */
    public BattleMove moveSlot(int slot) {
        if (slot < 0 || slot >= MOVES_MAX || slot >= pokemon.moves.size) {
            return null;
        }
        Pokemon.MoveSlot entry = pokemon.moves.get(slot);
        if (entry == null || entry.move == null) {
            moveCache[slot] = null;
            moveCacheId[slot] = null;
            return null;
        }
        // BattleMove is immutable (only a final PbsData.Move plus derived getters),
        // so the wrapper is reused until the slot's move is replaced - the battle
        // menu asks for it every frame. A replaced move goes through a new
        // MoveSlot (ItemUse / PokemonGrowth), so the identity check catches it.
        if (moveCacheId[slot] != entry.move) {
            moveCacheId[slot] = entry.move;
            moveCache[slot] = new BattleMove(entry.move);
        }
        return moveCache[slot];
    }

    private final BattleMove[] moveCache = new BattleMove[MOVES_MAX];
    private final pokemon.runtime.pokemon.PbsData.Move[] moveCacheId =
            new pokemon.runtime.pokemon.PbsData.Move[MOVES_MAX];

    /** The remaining PP of one slot, 0 for a blank slot. */
    public int moveSlotPp(int slot) {
        if (slot < 0 || slot >= MOVES_MAX || slot >= pokemon.moves.size) {
            return 0;
        }
        Pokemon.MoveSlot entry = pokemon.moves.get(slot);
        return entry == null ? 0 : Math.max(0, entry.pp);
    }

    /** {@code move.totalpp}; 0 for a blank slot. */
    public int moveSlotMaxPp(int slot) {
        if (slot < 0 || slot >= MOVES_MAX || slot >= pokemon.moves.size) {
            return 0;
        }
        Pokemon.MoveSlot entry = pokemon.moves.get(slot);
        return entry == null ? 0 : Math.max(0, entry.maxPp);
    }

    /**
     * {@code pbCanChooseAnyMove?} (Battle_Action_AttacksPriority:20-32): whether
     * any slot can be used. {@code pbFightMenu:68} uses it to decide between the
     * move menu and Struggle ({@code pbAutoChooseMove}).
     */
    public boolean hasUsableMove() {
        for (int i = 0; i < MOVES_MAX; i++) {
            BattleMove move = moveSlot(i);
            if (move == null) {
                continue;                                          // :22-23 id<=0
            }
            if (moveSlotPp(i) == 0 && moveSlotMaxPp(i) > 0) {
                continue;                                          // :23
            }
            return true;                                           // :29
        }
        return false;
    }

    /**
     * {@code @struggle} (Battler_Initialize): the move {@code pbAutoChooseMove}
     * registers when no slot can be used (Battle_Action_AttacksPriority:59-67,
     * {@code @choices[idxBattler][2] = @struggle}). The project's own moves.txt
     * entry is used when it has one.
     */
    public BattleMove struggle(pokemon.runtime.pokemon.PbsData pbs) {
        if (pbs != null) {
            pokemon.runtime.pokemon.PbsData.Move data = pbs.move("STRUGGLE");
            if (data != null) {
                return new BattleMove(data);
            }
        }
        pokemon.runtime.pokemon.PbsData.Move fallback = new pokemon.runtime.pokemon.PbsData.Move();
        fallback.internalName = "STRUGGLE"; fallback.name = "挣扎"; fallback.type = "NORMAL";
        fallback.power = 50; fallback.category = "Physical"; fallback.function = "002";
        return new BattleMove(fallback);
    }

    /**
     * {@code pbInitEffects(false)} (Battler_Initialize:112-156 + :157-260), the
     * part this runtime models. {@code pbReplace} runs it through
     * {@code pbInitialize} (Battler_Initialize:66-70) for every battler that
     * comes onto the field, so a Pokemon that is switched out and back in does
     * not keep its stat stages. The status itself is re-read from the Pokemon
     * ({@code pbInitPokemon:91-92}) and therefore survives.
     */
    public void resetForSwitchIn() {
        type1 = null;                                            // Battler_Initialize:47-48/81-82 types come from the Pokemon again
        type2 = null;
        for (int i = 0; i < stages.length; i++) {
            stages[i] = 0;                                       // :127-131
        }
        hitStages[0] = 0;                                        // :133 Accuracy
        hitStages[1] = 0;                                        // :132 Evasion
        confusion = 0;                                           // :135
        focusEnergy = false;                                     // :139
        flinched = false;                                        // :204
        turnCount = 0;                                           // :176
        faintedFlag = hp == 0;                                   // :157 @fainted = (@hp==0)
    }

    /** Copies the battle HP and status back onto the Pokemon. */
    public void syncHp() {
        pokemon.hp = Math.max(0, hp);
        pokemon.status = status == null ? "" : status;
    }

    /** {@code pbCanChooseAnyMove?}: whether the battler has any usable move. */
    public boolean canFight() {
        return !fainted() && hasUsableMove();
    }

    // =========================================================================
    // Stage 4 / M0 —— 以下全部为**纯追加**（本行以上的代码一行未改）
    //
    // 逐行依据：pokemon-builder/docs/stage4-m0b-battler-api-notes.md（§A 143 个方法，
    // 每个都带 `段:行号`）与 stage4-m0-interface-freeze.md §1/§2。
    // 每处追加的注释都写 Ruby 的 `段:行号`。
    //
    // 本运行时缺口与处置（详见各处 javadoc）：
    //  * `@battle.<method>` 缺失 → 走 PendingApi 桩（M0 既定约定，抛错不写假值）；
    //  * `@battle.scene.*` → 空实现 + `// 登记: <段:行号>`；
    //  * `@battle.field` / `@battle.sides` → 见 {@link #field}；
    //    **接线状态：已完成** —— Lead 在 `Battle.refreshFieldIndices()` 的两个循环里各加了
    //    `battler.battle = this; battler.field = field; battler.pbs = pbs;`（纯追加）。
    //    下面 {@link #field} / {@link #pbs} 的 javadoc 仍写着「必须接线」，是落地当时的措辞，
    //    保留以记录原因（本区不回头改，避免破坏「相对 prewire 快照 0 删除」的自查）。
    //  * `@battle.eachBattler` / `allBattlers` / `battlers` → 见 {@link #fieldBattlers()}；
    //  * `@battle.moldBreaker` → 见 {@link #moldBreaker()}（当前可证为 false）；
    //  * `@battle.sideStatUps` → 登记空实现（只影响 Opportunist / 模仿香草）。
    //
    // 故意**没有**追加的三个方法（插件自身缺陷，用户裁决：不新增方法，保持「照抄调用形状
    // + 抛异常桩」）。理由：Ruby 里这三处被调用时会直接 NoMethodError，所以「调用即抛异常」
    // 才是最忠实的映射；给它们一个「合理实现」才是自造行为。调用点在各 handler 体里：
    //  * `dynamax?`         —— **插件未定义该方法**（全工程只有
    //                          `BattleHandlers_Items:1317` 的无保护调用与
    //                          `BattleHandlers_Abilities:2973/2980` 的 `defined?` 保护调用）。
    //                          Ruby 会 NoMethodError。
    //  * `pbRecoverHP?`     —— **插件未定义该方法**（`BattleHandlers_Items:186` 多写了一个 `?`，
    //                          本意是 `pbRecoverHP`）。Ruby 会 NoMethodError。
    //  * `Battler#pbWeather` —— **插件未定义该方法**（`BattleHandlers_Abilities:4581` 的
    //                          `target.pbWeather`；Battle 才有 `pbWeather`，Battler 只有
    //                          `effectiveWeather`）。Ruby 会 NoMethodError。
    // 另外 5 处同类缺陷已按「照抄 + 注释登记」落在本区方法体内，用 `// 登记:` 搜索可见。
    // =========================================================================

    /**
     * {@code @effects} (PokeBattle_Battler:23 {@code attr_accessor :effects}) —— 最高频成员
     * （handler 体读 109 次）。初值由 {@link #initEffects(boolean)} 逐行照
     * {@code pbInitEffects} 写；键是 {@link PBEffects.Battler}。
     *
     * <p>读法必须区分 {@link EffectMap#truthy(int)} 与 {@link EffectMap#intVal(int)}：
     * Ruby 里 {@code 0} 是**真**，{@code if effects[X]} 与 {@code effects[X]>0} 是两个问题。</p>
     */
    public EffectMap effects = new EffectMap();

    /** {@code @damageState} (PokeBattle_Battler:42)，39 次。{@code pbInitialize:69} 会 reset。 */
    public DamageState damageState = new DamageState();

    /**
     * {@code @battle} (PokeBattle_Battler:3)，72 次。由 {@code Battle.addPlayer/addFoe} 赋值
     * （infra2 接线；本批不许改 Battle.java，所以这里只留字段）。
     */
    public Battle battle;

    /**
     * {@code @item} (PokeBattle_Battler:69)，40 次。本运行时惯例：内部名字符串，
     * Ruby 的 {@code 0}（无道具）↔ {@code ""}。
     */
    public String item = "";

    /** {@code @ability} (PokeBattle_Battler:11)，12 次。内部名字符串。 */
    public String ability = "";

    /**
     * {@code @statusCount} (PokeBattle_Battler:111)：SLEEP 的剩余回合，或 POISON 的剧毒计数
     * （{@code 1} = 剧毒）。本运行时早先把这一字段拆成了 {@link #sleepTurns} 与 {@link #toxic}
     * （Battle.java 与既有测试在写它们），所以三者必须一致 —— 唯一写入口
     * {@link #setStatusCount(int)} 会按当前状态同步对应的旧字段。
     */
    public int statusCount;

    /**
     * {@code initialItem} (PokeBattle_Battler:740-742)。
     *
     * <p>⚠️ <b>已登记的行为偏差</b>：插件读的是
     * {@code @battle.initialItems[@index&1][@pokemonIndex]}（按 <b>阵营+队伍槽位</b>存储，
     * 换人后仍保留）；本运行时 {@code Battle} 未暴露 {@code initialItems} 且本批不许改
     * Battle.java，所以暂存为 battler 字段（按<b>场上槽位</b>）。{@code Battle.initialItems}
     * 落地后必须改成查表，否则换人后数据会丢。</p>
     */
    public String initialItem = "";

    /** {@code recycleItem} (PokeBattle_Battler:748-750)。偏差同 {@link #initialItem}。 */
    public String recycleItem = "";

    /**
     * {@code @mirrorHerbUsed} (Battler_Initialize:356)。
     *
     * <p>⚠️ <b>插件自身缺陷</b>：插件只在 {@code Battler_Initialize:356} 初始化它、
     * 在 {@code Battler_StatStages:68/102} 读它，却**没有 {@code attr_accessor}**，
     * 而 {@code BattleHandlers_Items:1783/1790} 直接写 {@code battler.mirrorHerbUsed = ...}
     * → Ruby 里必然 {@code NoMethodError}。照抄为 public 字段（用户裁决：照抄 + 登记）。</p>
     */
    public boolean mirrorHerbUsed;

    /** {@code @pokemonIndex} (Battler_Initialize:96)：队伍槽位，换人/经验要用。 */
    public int pokemonIndex = -1;

    /** {@code @droppedBelowHalfHP} (Battler_Initialize:163)。 */
    public boolean droppedBelowHalfHP;

    /** {@code @statsDropped} (Battler_AbilityAndItem:9)。 */
    public boolean statsDropped;

    /** {@code @statsRaisedThisRound} (Battler_Initialize:166)。 */
    public boolean statsRaisedThisRound;

    /** {@code @statsLoweredThisRound} (Battler_Initialize:167)。 */
    public boolean statsLoweredThisRound;

    /** {@code @dummy} (PokeBattle_Battler:22 {@code attr_reader :dummy})。 */
    public boolean dummy;

    /**
     * {@code @fainted} (Battler_Initialize:157 {@code @fainted = (@hp==0)})。
     *
     * <p><b>本类已有等价字段 {@link #faintedFlag}</b>（Battler.java:69-77，javadoc 已写明它就是
     * {@code @fainted}），且 {@code Battle.java:873/878/1394} 与
     * {@code BattleRoundEventsTest:143} 都在用它。任务清单里列的
     * {@code public boolean fainted;} 因此**没有**再加 —— 两个字段同义会变成两个真相来源。
     * 本 M0 区一律读写 {@code faintedFlag}。</p>
     */
    public int yamaskhp;

    /** {@code @initialHP} (PokeBattle_Battler:43)，{@code pbInitEffects:158 @initialHP = 0}。 */
    public int initialHP;

    // --- PokeBattle_Battler:26-39 的“本场做过什么”字段（pbInitEffects:159-175 逐行要写） ---
    /** {@code @lastAttacker} (PokeBattle_Battler:27)。 */
    public Array<Battler> lastAttacker = new Array<>();
    /** {@code @lastFoeAttacker} (PokeBattle_Battler:28)。 */
    public Array<Battler> lastFoeAttacker = new Array<>();
    /** {@code @lastHPLost} (PokeBattle_Battler:29)。 */
    public int lastHPLost;
    /** {@code @lastHPLostFromFoe} (PokeBattle_Battler:30)。 */
    public int lastHPLostFromFoe;
    /** {@code @tookDamage} (PokeBattle_Battler:40)。 */
    public boolean tookDamage;
    /** {@code @tookPhysicalHit} (PokeBattle_Battler:41)。 */
    public boolean tookPhysicalHit;
    /** {@code @lastMoveUsed} (PokeBattle_Battler:31)。内部名；Ruby 的 {@code -1} 哨兵 ↔ {@code null}。 */
    public String lastMoveUsed;
    /** {@code @lastMoveUsedType} (PokeBattle_Battler:32)。类型内部名；{@code -1} ↔ {@code null}。 */
    public String lastMoveUsedType;
    /** {@code @lastRegularMoveUsed} (PokeBattle_Battler:33)。内部名；{@code -1} ↔ {@code null}。 */
    public String lastRegularMoveUsed;
    /** {@code @lastRegularMoveTarget} (PokeBattle_Battler:34)。 */
    public int lastRegularMoveTarget;
    /** {@code @lastRoundMoved} (PokeBattle_Battler:35)。 */
    public int lastRoundMoved;
    /** {@code @lastMoveFailed} (PokeBattle_Battler:36)。 */
    public boolean lastMoveFailed;
    /** {@code @lastRoundMoveFailed} (PokeBattle_Battler:37)。 */
    public boolean lastRoundMoveFailed;
    /** {@code @movesUsed} (PokeBattle_Battler:38)。内部名列表。 */
    public Array<String> movesUsed = new Array<>();

    /**
     * {@code @battle.field} (PokeBattle_ActiveField) 的桥接引用。
     *
     * <p>⚠️ <b>必须接线</b>：本运行时 {@code Battle} 还没有 {@code field} 字段
     * （{@code BattleField} 已由 infra3 建好但 Battle 未持有），而本批不许改 Battle.java。
     * 这里给一个独立实例，其初值与 {@code PokeBattle_ActiveField#initialize} 的默认值一致
     * （weather/terrain = None、所有 effects 未写 = nil/false/0），所以**在 Battle 接线之前**
     * 读到的是「刚开局、没人写过任何 field 键」的状态，与插件一致，不是自造值。</p>
     *
     * <p>Battle 接线后应改成 {@code battler.field = battle.field}；否则
     * NeutralizingGas / MagicRoom / Gravity / terrain 会静默停在初值。</p>
     */
    public BattleField field = new BattleField();

    /**
     * {@code PBAbilities.getName(@ability)} / {@code PBItems.getName(@item)} 需要的显示名表。
     *
     * <p>⚠️ <b>必须接线</b>：{@code Battle.pbs} 是 private 且无访问器，本批不许改 Battle.java，
     * 所以 {@link #abilityName()} / {@link #itemName()} 暂用一个可选引用；为 {@code null}
     * 时退化为内部名（已登记，见两个方法的 javadoc）。</p>
     */
    public pokemon.runtime.pokemon.PbsData pbs;

    // ---------------------------------------------------------------------
    // 能力等级访问器（解决 PBStats 原编号 与 现有 5 元数组 的错位）
    // ---------------------------------------------------------------------

    /**
     * {@code @stages[PBStats::X]} 的访问器（{@code PokeBattle_Battler} 的 {@code @stages} 是
     * <b>8 元</b>、用插件原编号：{@code HP=0,ATTACK=1,DEFENSE=2,SPEED=3,SPATK=4,SPDEF=5,
     * ACCURACY=6,EVASION=7}，见 {@code PBStats.rb:9-16}）。
     *
     * <p>⚠️ <b>临时映射</b>：本类现有的 {@code stages} 是 5 元<b>错位</b>布局
     * （Battler.java:28-31，ATTACK 在 0 … SPDEF 在 4），命中/回避另存 {@code hitStages}；
     * {@code PBStats} 是插件原编号，二者<b>差 1</b>。所以本访问器做映射：
     * ATTACK(1)→{@code stages[0]} … SPDEF(5)→{@code stages[4]}、
     * ACCURACY(6)→{@code hitStages[0]}、EVASION(7)→{@code hitStages[1]}，其余（HP=0 与越界）返回 0。
     * 后续把 {@code stages} 统一成 8 元时，本访问器退化为直通。
     * {@code stages}/{@code hitStages} 本体保持原样。</p>
     */
    public int stage(int pbStat) {
        switch (pbStat) {
            case PBStats.ATTACK:   return stages[0];
            case PBStats.DEFENSE:  return stages[1];
            case PBStats.SPEED:    return stages[2];
            case PBStats.SPATK:    return stages[3];
            case PBStats.SPDEF:    return stages[4];
            case PBStats.ACCURACY: return hitStages[0];
            case PBStats.EVASION:  return hitStages[1];
            default:               return 0;   // HP(0) 是基础值槽，插件从不用它做能力等级
        }
    }

    /** {@code @stages[PBStats::X] = value}；映射规则同 {@link #stage(int)}。 */
    public void setStage(int pbStat, int value) {
        switch (pbStat) {
            case PBStats.ATTACK:   stages[0] = value; break;
            case PBStats.DEFENSE:  stages[1] = value; break;
            case PBStats.SPEED:    stages[2] = value; break;
            case PBStats.SPATK:    stages[3] = value; break;
            case PBStats.SPDEF:    stages[4] = value; break;
            case PBStats.ACCURACY: hitStages[0] = value; break;
            case PBStats.EVASION:  hitStages[1] = value; break;
            default: break;                       // HP(0) 与越界：插件不会写
        }
    }

    // ---------------------------------------------------------------------
    // pbInitEffects —— Battler_Initialize:112-357 逐行转译
    // ---------------------------------------------------------------------

    /**
     * {@code pbInitEffects(batonPass)} (Battler_Initialize:112-357)，逐行转译。
     *
     * <p>与既有的 {@link #resetForSwitchIn()}（Battler.java:358-377，M0 前只转了插件的一部分）
     * 的关系：本方法**完整覆盖**它 —— {@code resetForSwitchIn} 转的是
     * {@code pbInitEffects(false)} 里的 stages/confusion/focusEnergy/flinched/turnCount/fainted
     * 那几行。谁调谁由 Lead 接线决定；本方法不调用它，也不改它。</p>
     *
     * @param batonPass {@code true} = 接棒传递（:113-124 分支）；{@code false} = 正常上场
     */
    public void initEffects(boolean batonPass) {
        if (batonPass) {
            // :116 @effects[LaserFocus] = (@effects[LaserFocus]>0) ? 2 : 0
            effects.set(PBEffects.Battler.LaserFocus,
                    effects.intVal(PBEffects.Battler.LaserFocus) > 0 ? 2 : 0);
            // :117 @effects[LockOn] = (@effects[LockOn]>0) ? 2 : 0
            effects.set(PBEffects.Battler.LockOn,
                    effects.intVal(PBEffects.Battler.LockOn) > 0 ? 2 : 0);
            if (effects.truthy(PBEffects.Battler.PowerTrick)) {          // :118
                int tmp = stages[0];                                     // :119 @attack,@defense = @defense,@attack
                stages[0] = stages[1];
                stages[1] = tmp;
            }
            // :123 @effects[Telekinesis] = 0 if isSpecies?(:GENGAR) && mega?
            if (isSpecies("GENGAR") && isMega()) {
                effects.set(PBEffects.Battler.Telekinesis, 0);
            }
            // :124 @effects[GastroAcid] = false if unstoppableAbility?
            if (unstoppableAbility(null)) {
                effects.set(PBEffects.Battler.GastroAcid, false);
            }
        } else {
            // :127-133 @stages[...] = 0（插件原编号 → 走映射访问器）
            setStage(PBStats.ATTACK, 0);
            setStage(PBStats.DEFENSE, 0);
            setStage(PBStats.SPEED, 0);
            setStage(PBStats.SPATK, 0);
            setStage(PBStats.SPDEF, 0);
            setStage(PBStats.EVASION, 0);
            setStage(PBStats.ACCURACY, 0);
            effects.set(PBEffects.Battler.AquaRing, false);            // :134
            effects.set(PBEffects.Battler.Confusion, 0);               // :135
            effects.set(PBEffects.Battler.Curse, false);               // :136
            effects.set(PBEffects.Battler.Tearalament, false);         // :137
            effects.set(PBEffects.Battler.Embargo, 0);                 // :138
            effects.set(PBEffects.Battler.FocusEnergy, 0);             // :139
            effects.set(PBEffects.Battler.GastroAcid, false);          // :140
            effects.set(PBEffects.Battler.HealBlock, 0);               // :141
            effects.set(PBEffects.Battler.Ingrain, false);             // :142
            effects.set(PBEffects.Battler.LaserFocus, 0);              // :143
            effects.set(PBEffects.Battler.LeechSeed, -1);              // :144
            effects.set(PBEffects.Battler.LockOn, 0);                  // :145
            effects.set(PBEffects.Battler.LockOnPos, -1);              // :146
            effects.set(PBEffects.Battler.MagnetRise, 0);              // :147
            effects.set(PBEffects.Battler.PerishSong, 0);              // :148
            effects.set(PBEffects.Battler.PerishSongUser, -1);         // :149
            effects.set(PBEffects.Battler.PowerTrick, false);          // :150
            effects.set(PBEffects.Battler.Substitute, 0);              // :151
            effects.set(PBEffects.Battler.Telekinesis, 0);             // :152
            effects.set(PBEffects.Battler.JawLock, false);             // :153
            effects.set(PBEffects.Battler.JawLockUser, -1);            // :154
            effects.set(PBEffects.Battler.NoRetreat, false);           // :155
        }
        faintedFlag = (hp == 0);                                       // :157 @fainted = (@hp==0)
        initialHP = 0;                                                 // :158
        lastAttacker.clear();                                          // :159 @lastAttacker = []
        lastFoeAttacker.clear();                                       // :160
        lastHPLost = 0;                                                // :161
        lastHPLostFromFoe = 0;                                         // :162
        droppedBelowHalfHP = false;                                    // :163
        tookDamage = false;                                            // :164
        tookPhysicalHit = false;                                       // :165
        statsRaisedThisRound = false;                                  // :166
        statsLoweredThisRound = false;                                 // :167
        lastMoveUsed = null;                                           // :168 @lastMoveUsed = -1
        lastMoveUsedType = null;                                       // :169 -1
        lastRegularMoveUsed = null;                                    // :170 -1
        lastRegularMoveTarget = -1;                                    // :171
        lastRoundMoved = -1;                                           // :172
        lastMoveFailed = false;                                        // :173
        lastRoundMoveFailed = false;                                   // :174
        movesUsed.clear();                                             // :175 @movesUsed = []
        turnCount = 0;                                                 // :176
        effects.set(PBEffects.Battler.Attract, -1);                    // :177
        for (Battler b : fieldBattlers()) {                            // :178 @battle.eachBattler
            if (b.effects.intVal(PBEffects.Battler.Attract) == index) { // :179
                b.effects.set(PBEffects.Battler.Attract, -1);
            }
        }
        effects.set(PBEffects.Battler.BanefulBunker, false);           // :181
        effects.set(PBEffects.Battler.BeakBlast, false);               // :182
        effects.set(PBEffects.Battler.Bide, 0);                        // :183
        effects.set(PBEffects.Battler.BideDamage, 0);                  // :184
        effects.set(PBEffects.Battler.BideTarget, -1);                 // :185
        effects.set(PBEffects.Battler.BurnUp, false);                  // :186
        effects.set(PBEffects.Battler.Charge, 0);                      // :187
        effects.set(PBEffects.Battler.ChoiceBand, -1);                 // :188
        effects.set(PBEffects.Battler.Counter, -1);                    // :189
        effects.set(PBEffects.Battler.CounterTarget, -1);              // :190
        effects.set(PBEffects.Battler.Dancer, false);                  // :191
        effects.set(PBEffects.Battler.DefenseCurl, false);             // :192
        effects.set(PBEffects.Battler.DestinyBond, false);             // :193
        effects.set(PBEffects.Battler.DestinyBondPrevious, false);     // :194
        effects.set(PBEffects.Battler.DestinyBondTarget, -1);          // :195
        effects.set(PBEffects.Battler.Disable, 0);                     // :196
        effects.set(PBEffects.Battler.DisableMove, 0);                 // :197
        effects.set(PBEffects.Battler.Electrify, false);               // :198
        effects.set(PBEffects.Battler.Encore, 0);                      // :199
        effects.set(PBEffects.Battler.EncoreMove, 0);                  // :200
        effects.set(PBEffects.Battler.Endure, false);                  // :201
        effects.set(PBEffects.Battler.FirstPledge, 0);                 // :202
        effects.set(PBEffects.Battler.FlashFire, false);               // :203
        effects.set(PBEffects.Battler.Flinch, false);                  // :204
        effects.set(PBEffects.Battler.FocusPunch, false);              // :205
        effects.set(PBEffects.Battler.FollowMe, 0);                    // :206
        effects.set(PBEffects.Battler.Foresight, false);               // :207
        effects.set(PBEffects.Battler.FuryCutter, 0);                  // :208
        effects.set(PBEffects.Battler.GemConsumed, 0);                 // :209
        effects.set(PBEffects.Battler.Grudge, false);                  // :210
        effects.set(PBEffects.Battler.HelpingHand, false);             // :211
        effects.set(PBEffects.Battler.HyperBeam, 0);                   // :212
        effects.set(PBEffects.Battler.Illusion, (Object) null);        // :213 @effects[Illusion] = nil
        // :214-219 —— 幻觉：需要 @battle.pbLastInTeam(:216) 与 @battle.pbParty(:218)
        // 登记: Battler_Initialize:214-219 依赖 Battle#pbLastInTeam / #pbParty（Battle 未暴露，
        //       本批不许改 Battle.java）→ 照 Ruby 的 if 形状保留分支，但不写 Illusion。
        if (hasActiveAbility("ILLUSION")) {                            // :214
            // idxLastParty = @battle.pbLastInTeam(@index)             // :215
            // if idxLastParty != @pokemonIndex                        // :216
            //   @effects[Illusion] = @battle.pbParty(@index)[idxLastParty]   // :217-218
        }
        effects.set(PBEffects.Battler.Imprison, false);                // :220
        effects.set(PBEffects.Battler.Instruct, false);                // :221
        effects.set(PBEffects.Battler.Instructed, false);              // :222
        effects.set(PBEffects.Battler.KingsShield, false);             // :223
        for (Battler b : fieldBattlers()) {                            // :224 @battle.eachBattler
            if (b.effects.intVal(PBEffects.Battler.LockOn) == 0) {      // :225 next if ==0
                continue;
            }
            if (b.effects.intVal(PBEffects.Battler.LockOnPos) != index) { // :226
                continue;
            }
            b.effects.set(PBEffects.Battler.LockOn, 0);                // :227
            b.effects.set(PBEffects.Battler.LockOnPos, -1);            // :228
        }
        effects.set(PBEffects.Battler.Octolock, false);                // :230
        effects.set(PBEffects.Battler.OctolockUser, -1);               // :231
        for (Battler b : fieldBattlers()) {                            // :232 @battle.eachBattler
            if (!b.effects.truthy(PBEffects.Battler.Octolock)) {        // :233 next if !...
                continue;
            }
            if (b.effects.intVal(PBEffects.Battler.OctolockUser) != index) { // :234
                continue;
            }
            b.effects.set(PBEffects.Battler.Octolock, false);          // :235
            b.effects.set(PBEffects.Battler.OctolockUser, -1);         // :236
        }
        for (Battler b : fieldBattlers()) {                            // :238 @battle.eachBattler
            if (!b.effects.truthy(PBEffects.Battler.JawLock)) {         // :239
                continue;
            }
            if (b.effects.intVal(PBEffects.Battler.JawLockUser) != index) { // :240
                continue;
            }
            b.effects.set(PBEffects.Battler.JawLock, false);           // :241
            b.effects.set(PBEffects.Battler.JawLockUser, -1);          // :242
        }
        effects.set(PBEffects.Battler.MagicBounce, false);             // :244
        effects.set(PBEffects.Battler.MagicCoat, false);               // :245
        effects.set(PBEffects.Battler.MeanLook, -1);                   // :246
        effects.set(PBEffects.Battler.FierceKilling, -1);              // :248 断刃鏖杀
        effects.set(PBEffects.Battler.FierceKilling2, false);          // :249
        for (Battler b : fieldBattlers()) {                            // :250 @battle.eachBattler
            if (b.effects.intVal(PBEffects.Battler.MeanLook) == index) { // :251
                b.effects.set(PBEffects.Battler.MeanLook, -1);
            }
            if (b.effects.intVal(PBEffects.Battler.FierceKilling) == index) { // :252
                b.effects.set(PBEffects.Battler.FierceKilling, -1);
            }
        }
        effects.set(PBEffects.Battler.MeFirst, false);                 // :254
        effects.set(PBEffects.Battler.Metronome, 0);                   // :255
        effects.set(PBEffects.Battler.MicleBerry, false);              // :256
        effects.set(PBEffects.Battler.Minimize, false);                // :257
        effects.set(PBEffects.Battler.MiracleEye, false);              // :258
        effects.set(PBEffects.Battler.MirrorCoat, -1);                 // :259
        effects.set(PBEffects.Battler.MirrorCoatTarget, -1);           // :260
        effects.set(PBEffects.Battler.MoveNext, false);                // :261
        effects.set(PBEffects.Battler.MudSport, false);                // :262
        effects.set(PBEffects.Battler.Nightmare, false);               // :263
        effects.set(PBEffects.Battler.Outrage, 0);                     // :264
        effects.set(PBEffects.Battler.ParentalBond, 0);                // :265
        effects.set(PBEffects.Battler.PickupItem, 0);                  // :266
        effects.set(PBEffects.Battler.PickupUse, 0);                   // :267
        effects.set(PBEffects.Battler.Pinch, false);                   // :268
        effects.set(PBEffects.Battler.Powder, false);                  // :269
        effects.set(PBEffects.Battler.Prankster, false);               // :270
        effects.set(PBEffects.Battler.PriorityAbility, false);         // :271
        effects.set(PBEffects.Battler.PriorityItem, false);            // :272
        effects.set(PBEffects.Battler.Protect, false);                 // :273
        effects.set(PBEffects.Battler.ProtectRate, 1);                 // :274
        effects.set(PBEffects.Battler.Pursuit, false);                 // :275
        effects.set(PBEffects.Battler.Quash, 0);                       // :276
        effects.set(PBEffects.Battler.Rage, false);                    // :277
        effects.set(PBEffects.Battler.RagePowder, false);              // :278
        effects.set(PBEffects.Battler.Revenge, 0);                     // :279
        effects.set(PBEffects.Battler.Rollout, 0);                     // :280
        effects.set(PBEffects.Battler.Roost, false);                   // :281
        effects.set(PBEffects.Battler.SkyDrop, -1);                    // :282
        for (Battler b : fieldBattlers()) {                            // :283 @battle.eachBattler
            if (b.effects.intVal(PBEffects.Battler.SkyDrop) == index) { // :284
                b.effects.set(PBEffects.Battler.SkyDrop, -1);
            }
        }
        effects.set(PBEffects.Battler.SlowStart, 0);                   // :286
        effects.set(PBEffects.Battler.SmackDown, false);               // :287
        effects.set(PBEffects.Battler.Snatch, 0);                      // :288
        effects.set(PBEffects.Battler.SpikyShield, false);             // :289
        effects.set(PBEffects.Battler.Spotlight, 0);                   // :290
        effects.set(PBEffects.Battler.Stockpile, 0);                   // :291
        effects.set(PBEffects.Battler.StockpileDef, 0);                // :292
        effects.set(PBEffects.Battler.StockpileSpDef, 0);              // :293
        effects.set(PBEffects.Battler.Taunt, 0);                       // :294
        effects.set(PBEffects.Battler.ThroatChop, 0);                  // :295
        effects.set(PBEffects.Battler.Torment, false);                 // :296
        effects.set(PBEffects.Battler.Toxic, 0);                       // :297
        effects.set(PBEffects.Battler.Transform, false);               // :298
        effects.set(PBEffects.Battler.TransformSpecies, 0);            // :299
        effects.set(PBEffects.Battler.Trapping, 0);                    // :300
        effects.set(PBEffects.Battler.TrappingMove, 0);                // :301
        effects.set(PBEffects.Battler.TrappingUser, -1);               // :302
        for (Battler b : fieldBattlers()) {                            // :303 @battle.eachBattler
            if (b.effects.intVal(PBEffects.Battler.TrappingUser) != index) { // :304
                continue;
            }
            b.effects.set(PBEffects.Battler.Trapping, 0);              // :305
            b.effects.set(PBEffects.Battler.TrappingUser, -1);         // :306
        }
        effects.set(PBEffects.Battler.Truant, false);                  // :308
        effects.set(PBEffects.Battler.TwoTurnAttack, 0);               // :309
        effects.set(PBEffects.Battler.Type3, -1);                      // :310
        effects.set(PBEffects.Battler.Unburden, false);                // :311
        effects.set(PBEffects.Battler.Uproar, 0);                      // :312
        effects.set(PBEffects.Battler.WaterSport, false);              // :313
        effects.set(PBEffects.Battler.WeightChange, 0);                // :314
        effects.set(PBEffects.Battler.Yawn, 0);                        // :315
        effects.set(PBEffects.Battler.GorillaTactics, -1);             // :316
        effects.set(PBEffects.Battler.BallFetch, 0);                   // :317
        effects.set(PBEffects.Battler.LashOut, false);                 // :318
        effects.set(PBEffects.Battler.BurningJealousy, false);         // :319
        effects.set(PBEffects.Battler.Obstruct, false);                // :320
        effects.set(PBEffects.Battler.TarShot, false);                 // :321
        effects.set(PBEffects.Battler.BlunderPolicy, false);           // :322
        effects.set(PBEffects.Battler.SwitchedAlly, -1);               // :323
        effects.set(PBEffects.Battler.CurseNail, 0);                   // :324
        effects.set(PBEffects.Battler.UsedRapidSpin, false);           // :325
        effects.set(PBEffects.Battler.UsedCoil, false);                // :326
        effects.set(PBEffects.Battler.Operation, false);               // :327
        effects.set(PBEffects.Battler.CudChew, 0);                     // :328
        effects.set(PBEffects.Battler.LoseGrassType, false);           // :329
        effects.set(PBEffects.Battler.LoseFireType, false);            // :330
        effects.set(PBEffects.Battler.LoseWaterType, false);           // :331
        effects.set(PBEffects.Battler.DoubleShock, false);             // :332
        effects.set(PBEffects.Battler.BambooSword, 0);                 // :333
        effects.set(PBEffects.Battler.BoosterEnergy, false);           // :334
        effects.set(PBEffects.Battler.ParadoxStat, (Object) null);     // :335 = nil
        effects.set(PBEffects.Battler.BurningBulwark, false);          // :336
        effects.set(PBEffects.Battler.Commander, (Object) null);       // :337 = nil
        effects.set(PBEffects.Battler.CudChew, 0);                     // :338（插件重复赋值，照抄）
        effects.set(PBEffects.Battler.DoubleShock, false);             // :339（重复，照抄）
        effects.set(PBEffects.Battler.GlaiveRush, 0);                  // :340
        effects.set(PBEffects.Battler.ParadoxStat, (Object) null);     // :341（重复，照抄）
        effects.set(PBEffects.Battler.SaltCure, false);                // :342
        effects.set(PBEffects.Battler.Syrupy, 0);                      // :343
        effects.set(PBEffects.Battler.SyrupyUser, -1);                 // :344
        effects.set(PBEffects.Battler.GlaiveRush, 0);                  // :345（重复，照抄）
        effects.set(PBEffects.Battler.SuccessiveMove, -1);             // :346
        effects.set(PBEffects.Battler.SupremeOverlord, 0);             // :347
        effects.set(PBEffects.Battler.PoisonVulnerability, 0);         // :348 地魔之剑
        effects.set(PBEffects.Battler.IceVulnerability, 0);            // :349 海魔之雨
        effects.set(PBEffects.Battler.DeoxysForm, 0);                  // :350
        for (Battler b : fieldBattlers()) {                            // :351 @battle.allBattlers.each
            if (b.effects.intVal(PBEffects.Battler.SyrupyUser) != index) { // :352
                continue;
            }
            b.effects.set(PBEffects.Battler.Syrupy, 0);                // :353
            b.effects.set(PBEffects.Battler.SyrupyUser, -1);           // :354
        }
        mirrorHerbUsed = false;                                        // :356
    }

    // ---------------------------------------------------------------------
    // 本运行时缺口的小桥接（每个都写清 Ruby 依据与接线要求）
    // ---------------------------------------------------------------------

    /**
     * {@code @battle.eachBattler} / {@code allBattlers} (PokeBattle_Battle:437-442) 的等价物：
     * 插件里 {@code @battlers} 是 6 槽位数组，两个方法都过滤 {@code nil} 与 {@code fainted?}。
     * 本运行时 {@code Battle} 只暴露 {@code player()}/{@code foe()}（1v1），所以这里遍历这两个
     * 槽位并做同样的过滤。
     */
    private Array<Battler> fieldBattlers() {
        Array<Battler> out = new Array<>();
        if (battle == null) {
            return out;
        }
        Battler p = battle.player();
        Battler f = battle.foe();
        if (p != null && !p.fainted()) {                              // :438 `b && !b.fainted?`
            out.add(p);
        }
        if (f != null && !f.fainted()) {
            out.add(f);
        }
        return out;
    }

    /** 场上某槽位的 battler（= {@code @battle.battlers[idx]}）；越界/空位返回 {@code null}。 */
    private Battler fieldBattlerAt(int idx) {
        if (battle == null) {
            return null;
        }
        return (idx & 1) == 0 ? battle.player() : battle.foe();
    }

    /**
     * {@code @battle.moldBreaker} (PokeBattle_Battle:85)。
     *
     * <p>{@code Battle} 未暴露该字段且本批不许改 Battle.java。返回 {@code false} 是
     * <b>本运行时当前的真实值</b>：{@code @moldBreaker} 只在破格类特性（Mold Breaker /
     * 涡轮火焰 / 垓级电压 …，见 {@code PokeBattle_Battler:573-575 hasMoldBreaker?}）行动时被置
     * true，而本运行时尚未建模任何特性触发 → 当前恒为 false。{@code Battle.moldBreaker}
     * 落地后本方法改为直读。</p>
     */
    private boolean moldBreaker() {
        return false;
    }

    /** {@code @effects[idx]} 的 field 组读法（Ruby truthy）。{@link #field} 见字段 javadoc。 */
    private boolean fieldTruthy(int idx) {
        return field != null && field.effects.truthy(idx);
    }

    /** {@code @effects[idx]} 的 field 组读法（Ruby 数值）。 */
    private int fieldInt(int idx) {
        return field == null ? 0 : field.effects.intVal(idx);
    }

    /** {@code @battle.pbDisplay(msg)} (PokeBattle_Battle:773-775)。Battle 未暴露 → 走桩。 */
    private void display(String message) {
        PendingApi.pbDisplay(battle, message);
    }

    /**
     * {@code gender} (PokeBattle_Battler:13 {@code attr_accessor :gender})：0=雄 1=雌 2=无性别。
     *
     * <p>本类没有 {@code gender} 字段（M0 前也没人需要），以 {@code Pokemon.gender} 为真相来源
     * （Ruby 的 {@code @gender} 就是 {@code Battler_Initialize:80} 从 {@code pkmn.gender} 同步的）。
     * {@code pokemon == null}（假人）按无性别 2 处理。</p>
     */
    public int gender() {
        return pokemon == null ? 2 : pokemon.gender;
    }

    /**
     * {@code move.ignoresSubstitute?(user)} (PokeBattle_Move:135-142)。
     *
     * <p>{@code BattleMove} 目前没有这个方法（Lead 的文件，本批不许改），所以按 Ruby 定义内联：
     * {@code NEWEST_BATTLE_MECHANICS} 下声音招式（标志 {@code k}，见 PokeBattle_Move:123
     * {@code soundMove?}）或使用者有 INFILTRATOR / TRANSLUCENTGHOST 时无视替身。</p>
     */
    private static boolean ignoresSubstitute(BattleMove move, Battler user) {
        if (move == null) {                                              // :135
            return false;
        }
        if (Battle.NEWEST_BATTLE_MECHANICS) {                            // :136
            String flags = move.flags();
            if (flags != null && flags.contains("k")) {                  // :137 soundMove?
                return true;
            }
            if (user != null && user.hasActiveAbility("INFILTRATOR")) {  // :138
                return true;
            }
            if (user != null && user.hasActiveAbility("TRANSLUCENTGHOST")) { // :139
                return true;
            }
        }
        return false;                                                    // :141
    }

    // =========================================================================
    // 标识与查询（stage4-m0b-battler-api-notes.md §A）
    // =========================================================================

    /** {@code pbThis} (PokeBattle_Battler:214-231)，默认 {@code lowerCase=false}；308 次，最高频。 */
    public String pbThis() {
        return pbThis(false);
    }

    /**
     * {@code pbThis(lowerCase=false)} (PokeBattle_Battler:214-231)。
     *
     * <p>本类已有的 {@link #thisName()} 是 M0 前的手写版（只按 {@code foe}/{@code trainerBattle}/
     * {@code battleRank} 分支）；本方法按 Ruby 的 {@code opposes?} 形状重写一遍，
     * 玩家侧走 {@code !pbOwnedByPlayer?} → "队友的{X}"，其余返回名字本身。
     * {@code lowerCase} 在插件里三分支返回同一字符串（作者留的坑，照抄）。</p>
     */
    public String pbThis(boolean lowerCase) {
        if (opposes(0)) {                                              // :215
            if (trainerBattle) {                                       // :216 @battle.trainerBattle?
                return "对手的" + name();                               // :217
            }
            if (pokemon != null && pokemon.battleRank > 1) {            // :218
                if (pokemon.battleRank > 2) {                          // :219
                    return "强大的" + name();                           // :220
                }
                return "特殊的" + name();                               // :222
            }
            return "野生的" + name();                                   // :225
        }
        if (!pbOwnedByPlayer()) {                                       // :227
            return "队友的" + name();                                   // :228
        }
        return name();                                                 // :230
    }

    /**
     * {@code abilityName} (PokeBattle_Battler:211) = {@code PBAbilities.getName(@ability)}，86 次。
     *
     * <p>⚠️ <b>已登记的退化</b>：显示名在 {@code PbsData.abilities} 里，而 {@code Battle.pbs}
     * 是 private 且无访问器（本批不许改 Battle.java）→ 走 {@link #pbs} 这个可选引用；
     * 为 {@code null} 时返回内部名（插件不会这么做，接线后消失）。</p>
     */
    public String abilityName() {
        if (pbs != null && ability != null && !ability.isEmpty()) {
            pokemon.runtime.pokemon.PbsData.Ability data = pbs.ability(ability);
            if (data != null && data.name != null && !data.name.isEmpty()) {
                return data.name;
            }
        }
        return ability == null ? "" : ability;
    }

    /** {@code itemName} (PokeBattle_Battler:212) = {@code PBItems.getName(@item)}，34 次。退化同 {@link #abilityName()}。 */
    public String itemName() {
        if (pbs != null && item != null && !item.isEmpty()) {
            pokemon.runtime.pokemon.PbsData.Item data = pbs.item(item);
            if (data != null && data.name != null && !data.name.isEmpty()) {
                return data.name;
            }
        }
        return item == null ? "" : item;
    }

    /** {@code isSpecies?} (PokeBattle_Battler:307-309)，45 次。 */
    public boolean isSpecies(String species) {
        return pokemon != null && pokemon.species != null && species != null
                && species.equals(pokemon.species.internalName);
    }

    /** {@code mega?} (PokeBattle_Battler:148)。运行时以 {@code Pokemon.isMega()} 为准。 */
    public boolean isMega() {
        return pokemon != null && pokemon.isMega();
    }

    /** {@code hasMega?} (PokeBattle_Battler:143-146)：变身中不算。 */
    public boolean hasMega() {
        if (effects.truthy(PBEffects.Battler.Transform)) {              // :144
            return false;
        }
        return pokemon != null && pokemon.hasMegaForm(pbs);             // :145
    }

    /** {@code primal?} (PokeBattle_Battler:156)。 */
    public boolean isPrimal() {
        return pokemon != null && pokemon.isPrimal();
    }

    /** {@code hasPrimal?} (PokeBattle_Battler:151-154)。 */
    public boolean hasPrimal() {
        if (effects.truthy(PBEffects.Battler.Transform)) {              // :152
            return false;
        }
        return pokemon != null && pokemon.hasPrimalForm();              // :153
    }

    /**
     * {@code form} (PokeBattle_Battler:62 {@code attr_reader :form})，15 次。
     *
     * <p>Ruby 的 {@code @form} 是形态编号整数（{@code Battler_Initialize:78} 从
     * {@code pkmn.form} 同步）；本运行时 {@code Pokemon.form} 是 {@code PbsData.SpeciesForm}，
     * 其 {@code form} 字段就是那个编号 → 直接返回它。</p>
     */
    public int form() {
        return pokemon == null || pokemon.form == null ? 0 : pokemon.form.form;
    }

    /** {@code opposes?} (PokeBattle_Battler:784-787)：{@code (@index&1)!=(i&1)}。 */
    public boolean opposes(int i) {
        return (index & 1) != (i & 1);                                  // :786
    }

    /** {@code opposes?(battler)}：Ruby 的 {@code i = i.index if i.respond_to?("index")} (:785)。 */
    public boolean opposes(Battler other) {
        return other != null && opposes(other.index);
    }

    /** {@code idxOwnSide} (PokeBattle_Battler:802-804)。 */
    public int idxOwnSide() {
        return index & 1;                                               // :803
    }

    /** {@code idxOpposingSide} (PokeBattle_Battler:808-810)。 */
    public int idxOpposingSide() {
        return (index & 1) ^ 1;                                         // :809
    }

    /** {@code pbOwnSide} (PokeBattle_Battler:813-815)：{@code @battle.sides[idxOwnSide]}。 */
    public BattleSide pbOwnSide() {
        return field.sides[idxOwnSide()];
    }

    /** {@code pbOpposingSide} (PokeBattle_Battler:818-820)。 */
    public BattleSide pbOpposingSide() {
        return field.sides[idxOpposingSide()];
    }

    /** {@code pbTeam} (PokeBattle_Battler:233-238)。 */
    public String pbTeam(boolean lowerCase) {
        return opposes(0) ? "对方队伍" : "我方队伍";                     // :235 / :237
    }

    /** {@code pbOpposingTeam} (PokeBattle_Battler:240-245)：**注意语义与 {@code pbTeam} 相反**。 */
    public String pbOpposingTeam(boolean lowerCase) {
        return opposes(0) ? "我方队伍" : "对方队伍";                     // :242 / :244
    }

    /**
     * {@code pbOwnedByPlayer?} (PokeBattle_Battler:796-798) →
     * {@code Battle#pbOwnedByPlayer?} (PokeBattle_Battle:287-290)：
     * 不在对面且 {@code pbGetOwnerIndexFromBattlerIndex==0}；本运行时 1v1 下等价于「自己是玩家侧」。
     */
    public boolean pbOwnedByPlayer() {
        return !opposes(0);                                             // :288 false if opposes?
    }

    /**
     * {@code near?} (PokeBattle_Battler:790-793) → {@code Battle#nearBattlers?}
     * (PokeBattle_Battle:544-568)。该函数在 {@code sideSizes} 均 ≤2 时直接
     * {@code return true if pbSideSize(0)<=2 && pbSideSize(1)<=2}（:546），只排除同一位置（:545）。
     * 本运行时是 1v1（{@code Battle} 只有 player()/foe()）→ 等价于「不是同一个位置」。
     */
    public boolean near(int i) {
        return index != i;                                              // :545-546
    }

    /** {@code near?(battler)}：{@code i = i.index if i.respond_to?("index")} (:791)。 */
    public boolean near(Battler other) {
        return other != null && near(other.index);
    }

    /**
     * {@code pbDirectOpposing(unfaintedOnly=false)} (PokeBattle_Battler:842-854)。
     * 用 {@code pbGetOpposingIndicesInOrder} (PokeBattle_Battle:496-533) 在 1v1 下的结果
     * （{@code [0]} 或 {@code [1]}）实现。
     */
    public Battler pbDirectOpposing(boolean unfaintedOnly) {
        Battler other = fieldBattlerAt(index ^ 1);                      // :843-847
        if (other == null) {
            return null;                                                // :853 @battle.battlers[@index^1]
        }
        if (!unfaintedOnly || !other.fainted()) {                       // :845 break if unfaintedOnly && fainted?
            return other;
        }
        return other;                                                   // :850-852 兜底也返回它
    }

    /** {@code eachAlly} (PokeBattle_Battler:823-827)：遍历未倒下的同伴（**排除自己**）。 */
    public void eachAlly(java.util.function.Consumer<Battler> action) {
        for (Battler b : fieldBattlers()) {                             // :824
            if (!b.opposes(index) && b.index != index) {                // :825
                action.accept(b);
            }
        }
    }

    /** {@code allAllies} (PokeBattle_Battler:829-831)。 */
    public Array<Battler> allAllies() {
        Array<Battler> out = new Array<>();
        for (Battler b : fieldBattlers()) {
            if (!b.opposes(index) && b.index != index) {                // :830 reject { index == @index }
                out.add(b);
            }
        }
        return out;
    }

    /** {@code allOpposing} (PokeBattle_Battler:837-839)。 */
    public Array<Battler> allOpposing() {
        return battle.allOtherSideBattlers(index);                      // :838
    }

    /** {@code eachOpposing} (PokeBattle_Battler:833-835)。 */
    public void eachOpposing(java.util.function.Consumer<Battler> action) {
        for (Battler b : fieldBattlers()) {                             // :834
            if (b.opposes(index)) {
                action.accept(b);
            }
        }
    }

    // =========================================================================
    // 特性与道具
    // =========================================================================

    /** {@code abilityActive?} (PokeBattle_Battler:379-385)，默认 {@code ignoreFainted=false}。 */
    public boolean abilityActive() {
        return abilityActive(false);
    }

    /** {@code abilityActive?(ignoreFainted)} (PokeBattle_Battler:379-385)。 */
    public boolean abilityActive(boolean ignoreFainted) {
        if (fainted() && !ignoreFainted) {                              // :380
            return false;
        }
        if (fieldTruthy(PBEffects.Field.NeutralizingGas) && !activeAbilityShield()) { // :381-382
            return false;
        }
        if (effects.truthy(PBEffects.Battler.GastroAcid)) {             // :383
            return false;
        }
        return true;                                                    // :384
    }

    /** {@code activeAbilityShield?} (PokeBattle_Battler:370-377)。 */
    public boolean activeAbilityShield() {
        if (fainted()) {                                                // :371
            return false;
        }
        if (!"ABILITYSHIELD".equals(item)) {                            // :372
            return false;
        }
        if (effects.intVal(PBEffects.Battler.Embargo) > 0) {            // :373
            return false;
        }
        if (fieldInt(PBEffects.Field.MagicRoom) > 0) {                  // :374
            return false;
        }
        if ("KLUTZ".equals(ability)) {                                  // :375
            return false;
        }
        return true;                                                    // :376
    }

    /** {@code hasActiveAbility?} (PokeBattle_Battler:387-399)，默认 {@code ignoreFainted=false}。 */
    public boolean hasActiveAbility(String abil) {
        return hasActiveAbility(abil, false);
    }

    /** {@code hasActiveAbility?(ability,ignoreFainted)} (PokeBattle_Battler:387-399)。 */
    public boolean hasActiveAbility(String abil, boolean ignoreFainted) {
        if (!abilityActive(ignoreFainted)) {                            // :388
            return false;
        }
        return abil != null && !abil.isEmpty() && abil.equals(ability); // :396-397 getID + ==@ability
    }

    /** {@code hasActiveAbility?([A,B])}：Ruby 的 {@code ability.is_a?(Array)} 分支 (:389-395)。 */
    public boolean hasActiveAbility(String[] abils) {
        return hasActiveAbility(abils, false);
    }

    /** {@code hasActiveAbility?([A,B])}：Ruby 的 {@code ability.is_a?(Array)} 分支 (:389-395)。 */
    public boolean hasActiveAbility(String[] abils, boolean ignoreFainted) {
        if (!abilityActive(ignoreFainted)) {                            // :388
            return false;
        }
        if (abils == null) {
            return false;
        }
        for (String a : abils) {                                        // :390-393
            if (a != null && !a.isEmpty() && a.equals(ability)) {
                return true;
            }
        }
        return false;                                                   // :394
    }

    /** {@code unstoppableAbility?} (PokeBattle_Battler:403-442)；{@code abil=null} 时取 {@code @ability} (:404)。 */
    public boolean unstoppableAbility(String abil) {
        String check = abil != null ? abil : ability;                   // :404
        if (check == null) {
            return false;
        }
        for (String a : UNSTOPPABLE_ABILITIES) {                        // :438-440
            if (a.equals(check)) {
                return true;
            }
        }
        return false;                                                   // :441
    }

    /** {@code ungainableAbility?} (PokeBattle_Battler:445-488)。 */
    public boolean ungainableAbility(String abil) {
        String check = abil != null ? abil : ability;                   // :446
        if (check == null) {
            return false;
        }
        for (String a : UNGAINABLE_ABILITIES) {                         // :484-486
            if (a.equals(check)) {
                return true;
            }
        }
        return false;                                                   // :487
    }

    /**
     * {@code uncopyableAbility?} (PokeBattle_Battler:490-503)。
     *
     * <p>⚠️ <b>插件自身缺陷（照抄）</b>：{@code :491 abil = @ability_id if !abil} ——
     * {@code @ability_id} 全工程从未定义，所以无参调用时 {@code abil} 恒为 {@code nil}，
     * {@code :492 return false if !abil} 让本方法**恒返回 false**。Java 里 {@code null} 就是那个
     * {@code nil}，照抄不修。</p>
     */
    public boolean uncopyableAbility(String abil) {
        if (abil == null) {                                             // :491-492（@ability_id 未定义）
            return false;
        }
        if (ungainableAbility(abil)) {                                  // :493
            return true;
        }
        for (String a : UNCOPYABLE_ABILITIES) {                         // :500-501
            if (a.equals(abil)) {
                return true;
            }
        }
        return false;                                                   // :502
    }

    /** {@code itemActive?} (PokeBattle_Battler:506-512)。 */
    public boolean itemActive() {
        return itemActive(false);
    }

    /** {@code itemActive?(ignoreFainted)} (PokeBattle_Battler:506-512)。 */
    public boolean itemActive(boolean ignoreFainted) {
        if (fainted() && !ignoreFainted) {                              // :507
            return false;
        }
        if (effects.intVal(PBEffects.Battler.Embargo) > 0) {            // :508
            return false;
        }
        if (fieldInt(PBEffects.Field.MagicRoom) > 0) {                  // :509
            return false;
        }
        if (hasActiveAbility("KLUTZ", ignoreFainted)) {                 // :510
            return false;
        }
        return true;                                                    // :511
    }

    /** {@code hasActiveItem?} (PokeBattle_Battler:519-530)。 */
    public boolean hasActiveItem(String itm) {
        return hasActiveItem(itm, false);
    }

    /** {@code hasActiveItem?(item,ignoreFainted)} (PokeBattle_Battler:519-530)。 */
    public boolean hasActiveItem(String itm, boolean ignoreFainted) {
        if (!itemActive(ignoreFainted)) {                               // :520
            return false;
        }
        return itm != null && !itm.isEmpty() && itm.equals(item);       // :528-529
    }

    /** {@code hasActiveItem?([A,B])}：Ruby 的 {@code item.is_a?(Array)} 分支 (:521-527)。 */
    public boolean hasActiveItem(String[] items) {
        return hasActiveItem(items, false);
    }

    /** {@code hasActiveItem?([A,B])}：Ruby 的 {@code item.is_a?(Array)} 分支 (:521-527)。 */
    public boolean hasActiveItem(String[] items, boolean ignoreFainted) {
        if (!itemActive(ignoreFainted)) {                               // :520
            return false;
        }
        if (items == null) {
            return false;
        }
        for (String i : items) {                                        // :522-525
            if (i != null && !i.isEmpty() && i.equals(item)) {
                return true;
            }
        }
        return false;                                                   // :526
    }

    /**
     * {@code unlosableItem?} (PokeBattle_Battler:534-541)。
     *
     * <p>{@code pbIsMail?} / {@code pbIsUnlosableItem?} 不在本批（它们要道具表），
     * 按 Ruby 的短路顺序保留：{@code :536 return true if pbIsMail?} 与
     * {@code :541 return pbIsUnlosableItem?} 两处留 登记 注释。</p>
     */
    public boolean unlosableItem(String checkItem) {
        if (checkItem == null || checkItem.isEmpty()) {                 // :535 return false if check_item <= 0
            return false;
        }
        // 登记: PokeBattle_Battler:536 pbIsMail?(check_item) 依赖道具邮件标记（本批未建模）
        if (effects.truthy(PBEffects.Battler.Transform)) {              // :537
            return false;
        }
        if (pokemon != null && pokemon.megaFormIndex(pbs) > 0) {        // :539 getMegaForm(true) > 0
            return true;
        }
        // 登记: PokeBattle_Battler:541 pbIsUnlosableItem?(check_item,@species,@ability)（本批未建模）
        return false;
    }

    /** {@code hasUtilityUmbrella?} (PokeBattle_Battler:610-613)。 */
    public boolean hasUtilityUmbrella() {
        return hasActiveItem("UTILITYUMBRELLA");                        // :611
    }

    /** {@code isUnnerved?} (PokeBattle_Battler:577-583)：内联 {@code Battle#pbCheckOpposingAbility}（:483-489）。 */
    public boolean isUnnerved() {
        return checkOpposingAbility("UNNERVE")                          // :578
                || checkOpposingAbility("ASONEICE")                     // :579
                || checkOpposingAbility("CONFESSIONLIST")               // :580
                || checkOpposingAbility("ASONEGHOST");                  // :581
    }

    /**
     * {@code Battle#pbCheckOpposingAbility} (PokeBattle_Battle:483-489) 的内联版：
     * 遍历对面未倒下的 battler 找该特性（{@code nearOnly=false}）。
     * {@code Battle} 尚未暴露此方法（interface-freeze §4 归 infra2），接线后改为直调。
     */
    private boolean checkOpposingAbility(String abil) {
        for (Battler b : fieldBattlers()) {                             // :484 eachOtherSideBattler
            if (b.opposes(index) && b.hasActiveAbility(abil)) {          // :486
                return true;
            }
        }
        return false;
    }

    /** {@code pbCanConsumeBerry?} (Battler_AbilityAndItem:152-159)。 */
    public boolean pbCanConsumeBerry(String berryItem, boolean alwaysCheckGluttony) {
        if (isUnnerved()) {                                             // :153
            return false;
        }
        if (hp <= maxHp() / 4) {                                        // :154 @hp<=@totalhp/4
            return true;
        }
        if (alwaysCheckGluttony || Battle.NEWEST_BATTLE_MECHANICS) {    // :155
            if (hp <= maxHp() / 2 && hasActiveAbility("GLUTTONY")) {    // :156
                return true;
            }
        }
        return false;                                                   // :158
    }

    /** {@code initialItem} (PokeBattle_Battler:740-742)。偏差见 {@link #initialItem} 字段 javadoc。 */
    public String initialItem() {
        return initialItem == null ? "" : initialItem;
    }

    /** {@code setInitialItem} (PokeBattle_Battler:744-746)。 */
    public void setInitialItem(String newItem) {
        initialItem = newItem == null ? "" : newItem;
    }

    /** {@code recycleItem} (PokeBattle_Battler:748-750)。 */
    public String recycleItem() {
        return recycleItem == null ? "" : recycleItem;
    }

    /** {@code setRecycleItem} (PokeBattle_Battler:752-754)。 */
    public void setRecycleItem(String newItem) {
        recycleItem = newItem == null ? "" : newItem;
    }

    /** {@code pbRemoveItem} (Battler_AbilityAndItem:163-168)。 */
    public void pbRemoveItem(boolean permanent) {
        effects.set(PBEffects.Battler.ChoiceBand, -1);                  // :164
        if (item != null && !item.isEmpty()) {                          // :165 @item>0
            effects.set(PBEffects.Battler.Unburden, true);
        }
        if (initialItem().equals(item) && permanent) {                  // :166
            setInitialItem("");
        }
        item = "";                                                      // :167 self.item = 0
    }

    /** {@code pbConsumeItem} (Battler_AbilityAndItem:170-180)。 */
    public void pbConsumeItem() {
        pbConsumeItem(true, true, true);
    }

    /** {@code pbConsumeItem(recoverable,symbiosis,belch)} (Battler_AbilityAndItem:170-180)。 */
    public void pbConsumeItem(boolean recoverable, boolean symbiosis, boolean belch) {
        if (recoverable) {                                              // :172
            setRecycleItem(item);                                       // :173
            effects.set(PBEffects.Battler.PickupItem, item == null ? 0 : item); // :174
            effects.set(PBEffects.Battler.PickupUse, 0);                // :175 @battle.nextPickupUse
        }
        if (belch && isBerry(item)) {                                   // :177 pbIsBerry?(@item)
            effects.set(PBEffects.Battler.Unburden, true);              // :177 setBelched → 见下
            setBelchedFlag();
        }
        pbRemoveItem(true);                                             // :178
        if (symbiosis) {                                                // :179
            pbSymbiosis();
        }
    }

    /** {@code setBelched} (PokeBattle_Battler:760-762)：运行时用 {@code Unburden} 兼作标记（登记见调用点）。 */
    private void setBelchedFlag() {
        // 登记: PokeBattle_Battler:760-762 @battle.belch[side][pokemonIndex]（Battle 未暴露 belch 表）
    }

    /** {@code pbIsBerry?} 的最小等价：按内部名以 BERRY 结尾判定（PItem_Items:99 是道具表查询）。 */
    private static boolean isBerry(String itm) {
        // 登记: PItem_Items:99 pbIsBerry?(item) 依赖道具表的 isBerry 标记（本批未建模）
        return itm != null && itm.endsWith("BERRY");
    }

    /** {@code pbSymbiosis} (Battler_AbilityAndItem:182-205)。 */
    public void pbSymbiosis() {
        if (fainted()) {                                                // :183
            return;
        }
        if (item != null && !item.isEmpty()) {                          // :184 return if @item!=0
            return;
        }
        for (Battler b : fieldBattlers()) {                             // :185 @battle.pbPriority(true)
            if (b.opposes(index)) {                                     // :186 next if b.opposes?
                continue;
            }
            if (!b.hasActiveAbility("SYMBIOSIS")) {                     // :187
                continue;
            }
            if (b.item == null || b.item.isEmpty() || b.unlosableItem(b.item)) { // :188
                continue;
            }
            if (unlosableItem(b.item)) {                                // :189
                continue;
            }
            PendingApi.pbShowAbilitySplash(battle, b);                  // :190
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :191
                display(b.pbThis() + "与" + pbThis(true) + "平分了" + b.itemName() + "！"); // :192-193
            } else {
                display(b.pbThis() + "的" + b.abilityName() + "让" + b.pbThis() + "与"
                        + pbThis(true) + "平分了" + b.itemName() + "！"); // :195-197
            }
            item = b.item;                                              // :198
            b.item = "";                                                // :199
            b.effects.set(PBEffects.Battler.Unburden, true);            // :200
            PendingApi.pbHideAbilitySplash(battle, b);                  // :201
            pbHeldItemTriggerCheck(0, false);                           // :202
            break;                                                      // :203
        }
    }

    /** {@code pbHeldItemTriggered} (Battler_AbilityAndItem:207-225)。 */
    public void pbHeldItemTriggered(String thisItem, int forcedItem, boolean fling) {
        if (hasActiveAbility("CHEEKPOUCH") && isBerry(thisItem) && canHeal()) { // :209
            PendingApi.pbShowAbilitySplash(battle, this);               // :210
            pbRecoverHP(maxHp() / 3);                                   // :211
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :212
                display(pbThis() + "的HP回复了。");                      // :213
            } else {
                display(pbThis() + "的" + abilityName() + "回复了HP。"); // :215
            }
            PendingApi.pbHideAbilitySplash(battle, this);               // :217
        }
        // :218-220 —— ⚠️ 插件自身缺陷：`item_to_use` 在本方法内从未定义（参数名是 `thisItem`），
        // 拥有反刍(CUDCHEW)特性的宝可梦走到这行必然 NoMethodError。照抄形状 + 登记，不自造替代：
        //   if hasActiveAbility?(:CUDCHEW) && pbIsBerry?(item_to_use) && fling
        //     setRecycleItem(item_to_use)
        //   end
        // 登记: Battler_AbilityAndItem:218-220 item_to_use 未定义（插件缺陷，等用户拍板）
        if (forcedItem <= 0) {                                          // :222
            pbConsumeItem();
        }
        if (forcedItem > 0 && !fling) {                                 // :223 Bug Bite/Pluck 触发共生
            pbSymbiosis();
        }
    }

    /** {@code pbHeldItemTriggerCheck} (Battler_AbilityAndItem:235-249)。 */
    public void pbHeldItemTriggerCheck(int forcedItem, boolean fling) {
        if (fainted()) {                                                // :236
            return;
        }
        if (forcedItem == 0 && !itemActive()) {                         // :237
            return;
        }
        pbItemHPHealCheck(forcedItem, fling);                           // :238
        pbItemStatusCureCheck(forcedItem, fling);                       // :239
        pbItemEndOfMoveCheck(forcedItem, fling);                        // :240
        if (forcedItem != 0) {                                          // :243
            String itm = forcedItem > 0 ? String.valueOf(forcedItem) : item; // :244
            if (BattleHandlers.triggerTargetItemOnHitPositiveBerry(itm, this, battle, true)) { // :245
                pbHeldItemTriggered(itm, forcedItem, fling);            // :246
            }
        }
    }

    /** {@code pbItemHPHealCheck} (Battler_AbilityAndItem:253-261)。 */
    public void pbItemHPHealCheck(int forcedItem, boolean fling) {
        if (forcedItem == 0 && !itemActive()) {                         // :254
            return;
        }
        String itm = forcedItem > 0 ? String.valueOf(forcedItem) : item; // :255
        if (BattleHandlers.triggerHPHealItem(itm, this, battle, forcedItem != 0)) { // :256
            pbHeldItemTriggered(itm, forcedItem, fling);                // :257
        } else if (forcedItem == 0) {                                   // :258
            pbItemTerrainStatBoostCheck();                              // :259
        }
    }

    /** {@code pbItemStatusCureCheck} (Battler_AbilityAndItem:267-274)。 */
    public void pbItemStatusCureCheck(int forcedItem, boolean fling) {
        if (fainted()) {                                                // :268
            return;
        }
        if (forcedItem == 0 && !itemActive()) {                         // :269
            return;
        }
        String itm = forcedItem > 0 ? String.valueOf(forcedItem) : item; // :270
        if (BattleHandlers.triggerStatusCureItem(itm, this, battle, forcedItem != 0)) { // :271
            pbHeldItemTriggered(itm, forcedItem, fling);                // :272
        }
    }

    /** {@code pbItemEndOfMoveCheck} (Battler_AbilityAndItem:279-288)。 */
    public void pbItemEndOfMoveCheck(int forcedItem, boolean fling) {
        if (fainted()) {                                                // :280
            return;
        }
        if (forcedItem == 0 && !itemActive()) {                         // :281
            return;
        }
        String itm = forcedItem > 0 ? String.valueOf(forcedItem) : item; // :282
        if (BattleHandlers.triggerEndOfMoveItem(itm, this, battle, forcedItem != 0)) { // :283
            pbHeldItemTriggered(itm, forcedItem, fling);                // :284
        } else if (BattleHandlers.triggerEndOfMoveStatRestoreItem(itm, this, battle, forcedItem != 0)) { // :285
            pbHeldItemTriggered(itm, forcedItem, fling);                // :286
        }
    }

    /** {@code pbItemStatRestoreCheck} (Battler_AbilityAndItem:295-302)。 */
    public void pbItemStatRestoreCheck(int forcedItem, boolean fling) {
        if (fainted()) {                                                // :296
            return;
        }
        if (forcedItem == 0 && !itemActive()) {                         // :297
            return;
        }
        String itm = forcedItem > 0 ? String.valueOf(forcedItem) : item; // :298
        if (BattleHandlers.triggerEndOfMoveStatRestoreItem(itm, this, battle, forcedItem != 0)) { // :299
            pbHeldItemTriggered(itm, forcedItem, fling);                // :300
        }
    }

    /** {@code pbItemTerrainStatBoostCheck} (Battler_AbilityAndItem:307-312)。 */
    public void pbItemTerrainStatBoostCheck() {
        if (!itemActive()) {                                            // :308
            return;
        }
        if (BattleHandlers.triggerTerrainStatBoostItem(item, this, battle)) { // :309
            pbHeldItemTriggered(item, 0, false);                        // :310
        }
    }

    /** {@code pbItemOnIntimidatedCheck} (Battler_AbilityAndItem:318-323)。 */
    public void pbItemOnIntimidatedCheck() {
        if (!itemActive()) {                                            // :319
            return;
        }
        if (BattleHandlers.triggerItemOnIntimidated(item, this, battle)) { // :320
            pbHeldItemTriggered(item, 0, false);                        // :321
        }
    }

    /** {@code pbAbilityStatusCureCheck} (Battler_AbilityAndItem:123-127)。 */
    public void pbAbilityStatusCureCheck() {
        if (abilityActive()) {                                          // :124
            BattleHandlers.triggerStatusCureAbility(ability, this);      // :125
        }
    }

    /** {@code pbOnAbilityChanged} (Battler_AbilityAndItem:132-147)。 */
    public void pbOnAbilityChanged(String oldAbil) {
        if (effects.truthy(PBEffects.Battler.Illusion) && "ILLUSION".equals(oldAbil)) { // :133
            effects.set(PBEffects.Battler.Illusion, (Object) null);      // :134
            if (!effects.truthy(PBEffects.Battler.Transform)) {          // :135
                // 登记: Battler_AbilityAndItem:136 @battle.scene.pbChangePokemon(self,@pokemon)
                //       依赖 PokeBattle_Scene（本批未建模）→ 空实现
                display(pbThis() + "的" + abilityName() + "消失了！");    // :137
                // 登记: Battler_AbilityAndItem:138 @battle.pbSetSeen(self) 依赖图鉴/存档
            }
        }
        if (unstoppableAbility(null)) {                                  // :141
            effects.set(PBEffects.Battler.GastroAcid, false);
        }
        if (!"SLOWSTART".equals(ability)) {                              // :142
            effects.set(PBEffects.Battler.SlowStart, 0);
        }
        pbCheckFormOnWeatherChange();                                    // :144
        // :146 @battle.pbEndPrimordialWeather —— 登记: Battle 未暴露（本批不许改 Battle.java）
    }

    /** {@code pbAbilityOnTerrainChange} (Battler_AbilityAndItem:75-78)。 */
    public void pbAbilityOnTerrainChange(boolean abilityChanged) {
        if (!abilityActive()) {                                          // :76
            return;
        }
        BattleHandlers.triggerAbilityOnTerrainChange(ability, this, battle, abilityChanged); // :77
    }

    /** {@code pbAbilitiesOnDamageTaken} (Battler_AbilityAndItem:67-73)：返回是否被换下。 */
    public boolean pbAbilitiesOnDamageTaken(int oldHP, int newHP) {
        if (!abilityActive()) {                                          // :68
            return false;
        }
        int newHp = newHP < 0 ? hp : newHP;                              // :69
        if (oldHP < maxHp() / 2 || newHp >= maxHp() / 2) {               // :70 没跌破半血
            return false;
        }
        return BattleHandlers.triggerAbilityOnHPDroppedBelowHalf(ability, this, battle); // :71
    }

    /** {@code pbAbilitiesOnSwitchOut} (Battler_AbilityAndItem:41-52)。 */
    public void pbAbilitiesOnSwitchOut() {
        if (abilityActive()) {                                           // :42
            BattleHandlers.triggerAbilityOnSwitchOut(ability, this, false); // :43
        }
        // 登记: Battler_AbilityAndItem:46 @battle.peer.pbOnLeavingBattle(...) 依赖 BattlePeer/存档
        hp = 0;                                                          // :48
        faintedFlag = true;                                              // :49 @fainted = true
        // :51 @battle.pbEndPrimordialWeather —— 登记: Battle 未暴露
    }

    // =========================================================================
    // HP 与治疗
    // =========================================================================

    /** {@code pbReduceHP} (Battler_ChangeSelf:5-17)。 */
    public int pbReduceHP(int amount) {
        return pbReduceHP(amount, true, true, true);
    }

    /** {@code pbReduceHP(amt,anim,registerDamage,anyAnim)} (Battler_ChangeSelf:5-17)。 */
    public int pbReduceHP(int amount, boolean anim, boolean registerDamage, boolean anyAnim) {
        int amt = Math.round((float) amount);                            // :6 amt = amt.round
        if (amt > hp) {                                                  // :7
            amt = hp;
        }
        if (amt < 1 && !fainted()) {                                     // :8
            amt = 1;
        }
        int oldHP = hp;                                                  // :9
        hp -= amt;                                                       // :10 self.hp -= amt
        if (hp < 0) {                                                    // :12 raise _INTL("HP小于0")
            throw new IllegalStateException("HP小于0");
        }
        if (hp > maxHp()) {                                              // :13 raise _INTL("HP大于最大HP")
            throw new IllegalStateException("HP大于最大HP");
        }
        if (anyAnim && amt > 0) {                                        // :14
            // 登记: Battler_ChangeSelf:14 @battle.scene.pbHPChanged(self,oldHP,anim)
            //       依赖 PokeBattle_Scene（本批未建模）→ 空实现
        }
        if (amt > 0 && registerDamage) {                                 // :15
            tookDamage = true;
        }
        return amt;                                                      // :16
    }

    /** {@code pbRecoverHP} (Battler_ChangeSelf:19-31)。 */
    public int pbRecoverHP(int amount) {
        return pbRecoverHP(amount, true, true);
    }

    /** {@code pbRecoverHP(amt,anim,anyAnim)} (Battler_ChangeSelf:19-31)。 */
    public int pbRecoverHP(int amount, boolean anim, boolean anyAnim) {
        int amt = Math.round((float) amount);                            // :20
        if (amt > maxHp() - hp) {                                        // :21
            amt = maxHp() - hp;
        }
        if (amt < 1 && hp < maxHp()) {                                   // :22
            amt = 1;
        }
        int oldHP = hp;                                                  // :23
        hp += amt;                                                       // :24
        if (hp < 0) {                                                    // :26
            throw new IllegalStateException("HP小于0");
        }
        if (hp > maxHp()) {                                              // :27
            throw new IllegalStateException("HP大于最大HP");
        }
        if (anyAnim && amt > 0) {                                        // :28
            // 登记: Battler_ChangeSelf:28 @battle.scene.pbHPChanged(self,oldHP,anim)
            //       依赖 PokeBattle_Scene（本批未建模）→ 空实现
        }
        yamaskhp = 0;                                                    // :29 self.yamaskhp = 0
        return amt;                                                      // :30
    }

    /** {@code canHeal?} (PokeBattle_Battler:684-688)，16 次。 */
    public boolean canHeal() {
        if (fainted() || hp >= maxHp()) {                                // :685
            return false;
        }
        return effects.intVal(PBEffects.Battler.HealBlock) <= 0;         // :686
    }

    /** {@code takesIndirectDamage?} (PokeBattle_Battler:615-630)。 */
    public boolean takesIndirectDamage(boolean showMsg) {
        if (fainted()) {                                                 // :616
            return false;
        }
        if (hasActiveAbility("MAGICGUARD")) {                            // :617
            if (showMsg) {                                               // :618
                PendingApi.pbShowAbilitySplash(battle, this);            // :619
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :620
                    display(pbThis() + "没有受到影响！");                // :621
                } else {
                    display(pbThis() + "因" + abilityName() + "而不受影响！"); // :623
                }
                PendingApi.pbHideAbilitySplash(battle, this);            // :625
            }
            return false;                                                // :627
        }
        return true;                                                     // :629
    }

    /** {@code affectedByContactEffect?} (PokeBattle_Battler:695-702)。 */
    public boolean affectedByContactEffect(boolean showMsg) {
        if (fainted()) {                                                 // :696
            return false;
        }
        if (hasActiveItem("PROTECTIVEPADS")) {                           // :697
            if (showMsg) {                                               // :698
                display(pbThis() + "用" + itemName() + "保护了自己！");
            }
            return false;                                                // :699
        }
        return true;                                                     // :701
    }

    /** {@code affectedByPowder?} (PokeBattle_Battler:656-682)。 */
    public boolean affectedByPowder(boolean showMsg) {
        if (fainted()) {                                                 // :657
            return false;
        }
        if (!Battle.NEWEST_BATTLE_MECHANICS) {                           // :658
            return true;
        }
        if (pbHasType("GRASS")) {                                        // :659
            if (showMsg) {                                               // :660
                display(pbThis() + "没有受到影响！");
            }
            return false;                                                // :661
        }
        if (hasActiveAbility(new String[] {"OVERCOAT", "DIVINEPACT"}) && !moldBreaker()) { // :663
            if (showMsg) {                                               // :664
                PendingApi.pbShowAbilitySplash(battle, this);            // :665
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :666
                    display(pbThis() + "没有受到影响！");                // :667
                } else {
                    display(pbThis() + "因" + abilityName() + "而不受影响！"); // :669
                }
                PendingApi.pbHideAbilitySplash(battle, this);            // :671
            }
            return false;                                                // :673
        }
        if (hasActiveItem("SAFETYGOGGLES")) {                            // :675
            if (showMsg) {                                               // :676
                display(pbThis() + "因" + itemName() + "而不受影响！");   // :677
            }
            return false;                                                // :679
        }
        return true;                                                     // :681
    }

    /** {@code pbFaint} (Battler_ChangeSelf:105-127，包住 :61-99 的第一定义)。 */
    public void pbFaint() {
        pbFaint(true);
    }

    /**
     * {@code pbFaint(showMessage=true)} (Battler_ChangeSelf:61-99 + :105-127)。
     *
     * <p>两个 Ruby 定义：{@code :105-127} 用 {@code alias paldea_pbFaint pbFaint} 包住
     * {@code :61-99}。Java 只能有一个方法名，所以这里按「先跑指挥官前置段，再跑主体」的顺序
     * 合并（等价于 Ruby 的调用链）。</p>
     */
    public void pbFaint(boolean showMessage) {
        // :106-119 指挥官配对的前置段
        String commanderMsg = null;
        if (effects.truthy(PBEffects.Battler.Commander)) {                // :107
            // 登记: Battler_ChangeSelf:108-118 依赖 @effects[Commander] 的 Array 形态与
            //       @battle.battlers[...] / @battle.scene.sprites[...]（本批未建模）
        }
        boolean wasFainted = faintedFlag;                                 // :120
        pbFaintCore(showMessage);                                         // :121 paldea_pbFaint(showMessage)
        // :122 @battle.pbAddFaintedAlly(self) if !isFainted && @fainted
        //   登记: Battle 未暴露 pbAddFaintedAlly（本批不许改 Battle.java）
        if (commanderMsg != null) {                                       // :123-126
            display(commanderMsg);
            // 登记: Battler_ChangeSelf:125 batSprite.visible = true（依赖 PokeBattle_Scene）
        }
    }

    /** {@code pbFaint} 的主体（Battler_ChangeSelf:61-99）。 */
    private void pbFaintCore(boolean showMessage) {
        if (pokemon != null && pokemon.battleRank > 1) {                  // :62
            // :63 pbCatchBossPokemon(self) if @battle.decision == 0
            //   登记: PokeBattle_BOSS:157 pbCatchBossPokemon 依赖 BOSS 子系统
            return;                                                       // :64
        }
        if (!fainted()) {                                                 // :66
            return;                                                       // :68
        }
        if (faintedFlag) {                                                // :70 已经倒下过
            return;
        }
        if (showMessage) {                                                // :71
            display(pbThis() + "倒下了！");
        }
        // 登记: Battler_ChangeSelf:73 @battle.scene.pbFaintBattler(self) 依赖 PokeBattle_Scene
        initEffects(false);                                               // :74 pbInitEffects(false)
        status = "";                                                      // :76 self.status = PBStatuses::NONE
        setStatusCount(0);                                                // :77
        if (pokemon != null && battle != null) {                          // :79 @battle.internalBattle
            boolean badLoss = false;                                      // :80
            for (Battler b : fieldBattlers()) {                           // :81 @battle.eachOtherSideBattler
                if (b.opposes(index) && b.level() >= level() + 30) {      // :82
                    badLoss = true;
                }
            }
            // 登记: Battler_ChangeSelf:84 @pokemon.changeHappiness(...)（PokeBattle_Pokemon:777-838）
            //       依赖亲密度方法表（本运行时 Pokemon.happiness 只是裸字段，无 changeHappiness）
            //       → 本批不写假行为，badLoss 只用于选择 "faintbad"/"faint" 分支名
        }
        // 登记: Battler_ChangeSelf:87 @battle.peer.pbOnLeavingBattle(...) 依赖 BattlePeer/存档
        if (isMega()) {                                                   // :88
            pokemon.makeUnmega(pbs);
        }
        if (isPrimal()) {                                                 // :89
            pokemon.makeUnprimal(pbs);
        }
        yamaskhp = 0;                                                     // :90 Yamask（@pokemon.yamaskhp；本运行时 Pokemon 无此字段 → 只写 battler 侧）
        if (battle != null) {                                             // :92 @battle.pbClearChoice(@index)
            battle.clearChoice(index);
        }
        pbOwnSide().effects.set(PBEffects.Side.LastRoundFainted,
                battle == null ? 0 : battle.turns());                      // :93
        pbAbilitiesOnFainting();                                          // :95
        // :97 @battle.pbEndPrimordialWeather —— 登记: Battle 未暴露
        // :98 @battle.pbAddFaintedAlly(self) —— 登记: Battle 未暴露
    }

    /** {@code pbAbilitiesOnFainting} (Battler_AbilityAndItem:54-64)。 */
    public void pbAbilitiesOnFainting() {
        for (Battler b : fieldBattlers()) {                               // :56 @battle.pbPriority(true)
            if (!b.abilityActive()) {                                     // :57
                continue;
            }
            BattleHandlers.triggerAbilityChangeOnBattlerFainting(b.ability, b, this, battle); // :58
        }
        for (Battler b : fieldBattlers()) {                               // :60
            if (!b.abilityActive()) {                                     // :61
                continue;
            }
            BattleHandlers.triggerAbilityOnBattlerFainting(b.ability, b, this, battle); // :62
        }
    }

    // =========================================================================
    // 状态异常（Battler_Statuses）
    // =========================================================================

    /** {@code pbHasStatus?} (Battler_Statuses:11-16)。 */
    public boolean pbHasStatus(int checkStatus) {
        if (BattleHandlers.triggerStatusCheckAbilityNonIgnorable(ability, this, checkStatus)) { // :12
            return true;                                                 // :13
        }
        return PBStatuses.idOf(status) == checkStatus;                   // :15 @status==checkStatus
    }

    /** {@code pbHasAnyStatus?} (Battler_Statuses:18-23)。 */
    public boolean pbHasAnyStatus() {
        if (BattleHandlers.triggerStatusCheckAbilityNonIgnorable(ability, this, PBStatuses.NONE)) { // :19
            return true;                                                 // :20
        }
        return statused();                                               // :22 @status!=PBStatuses::NONE
    }

    /** {@code asleep?} (Battler_Statuses:314-316)。 */
    public boolean asleep() {
        return pbHasStatus(PBStatuses.SLEEP);
    }

    /** {@code poisoned?} (Battler_Statuses:371-373)。 */
    public boolean poisoned() {
        return pbHasStatus(PBStatuses.POISON);
    }

    /** {@code burned?} (Battler_Statuses:390-392)。 */
    public boolean burned() {
        return pbHasStatus(PBStatuses.BURN);
    }

    /** {@code paralyzed?} (Battler_Statuses:409-411)。 */
    public boolean paralyzed() {
        return pbHasStatus(PBStatuses.PARALYSIS);
    }

    /** {@code frozen?} (Battler_Statuses:428-430)。 */
    public boolean frozen() {
        return pbHasStatus(PBStatuses.FROZEN);
    }

    /**
     * {@code statusCount=} (PokeBattle_Battler:113-117)。
     *
     * <p>本运行时把 {@code @statusCount} 的两用拆成了 {@link #sleepTurns}（SLEEP 剩余回合）
     * 与 {@link #toxic}（POISON 剧毒计数），三者必须一致 → 本方法是唯一写入口，按当前状态同步。</p>
     */
    public void setStatusCount(int value) {
        statusCount = value;                                             // :114
        if ("SLEEP".equals(status)) {                                    // Battler_Statuses:363-365
            sleepTurns = value;
        } else if ("POISON".equals(status)) {                            // Battler_Statuses:383-385
            toxic = value;
        }
        // 登记: PokeBattle_Battler:116 @battle.scene.pbRefreshOne(@index) 依赖 PokeBattle_Scene
        // 登记: PokeBattle_Battler:115 @pokemon.statusCount = value（本运行时 Pokemon 无此字段）
    }

    /** {@code pbCanPoison?} (Battler_Statuses:375-377)。 */
    public boolean pbCanPoison(Battler user, boolean showMessages, BattleMove move) {
        return pbCanInflictStatus(PBStatuses.POISON, user, showMessages, move, false); // :376
    }

    /** {@code pbCanBurn?} (Battler_Statuses:394-396)。 */
    public boolean pbCanBurn(Battler user, boolean showMessages, BattleMove move) {
        return pbCanInflictStatus(PBStatuses.BURN, user, showMessages, move, false); // :395
    }

    /** {@code pbCanParalyze?} (Battler_Statuses:413-415)。 */
    public boolean pbCanParalyze(Battler user, boolean showMessages, BattleMove move) {
        return pbCanInflictStatus(PBStatuses.PARALYSIS, user, showMessages, move, false); // :414
    }

    /** {@code pbCanSleep?} (Battler_Statuses:318-320)。 */
    public boolean pbCanSleep(Battler user, boolean showMessages, BattleMove move, boolean ignoreStatus) {
        return pbCanInflictStatus(PBStatuses.SLEEP, user, showMessages, move, ignoreStatus); // :319
    }

    /** {@code pbPoison} (Battler_Statuses:383-385)。 */
    public void pbPoison(Battler user, String msg, boolean toxic) {
        pbInflictStatus(PBStatuses.POISON, toxic ? 1 : 0, msg, user);     // :384
    }

    /** {@code pbBurn} (Battler_Statuses:402-404)。 */
    public void pbBurn(Battler user, String msg) {
        pbInflictStatus(PBStatuses.BURN, 0, msg, user);                  // :403
    }

    /** {@code pbParalyze} (Battler_Statuses:421-423)。 */
    public void pbParalyze(Battler user, String msg) {
        pbInflictStatus(PBStatuses.PARALYSIS, 0, msg, user);             // :422
    }

    /**
     * {@code pbCanInflictStatus?} (Battler_Statuses:25-200)，逐段转译。
     *
     * <p>{@code newStatus} 用 {@link PBStatuses} 的整数编号（照 Ruby 的 {@code PBStatuses::X}）。</p>
     */
    public boolean pbCanInflictStatus(int newStatus, Battler user, boolean showMessages,
                                      BattleMove move, boolean ignoreStatus) {
        if (fainted()) {                                                 // :26
            return false;
        }
        boolean selfInflicted = user != null && user.index == index;     // :27
        if (PBStatuses.idOf(status) == newStatus && !ignoreStatus) {     // :29
            if (showMessages) {                                          // :30-41
                String msg = "";
                switch (status) {
                    case "SLEEP":     msg = pbThis() + "已经睡着了！"; break;      // :33
                    case "POISON":    msg = pbThis() + "已经中毒了！"; break;      // :34
                    case "BURN":      msg = pbThis() + "已经灼伤了！"; break;      // :35
                    case "PARALYSIS": msg = pbThis() + "已经被麻痹了！"; break;    // :36
                    case "FROZEN":    msg = pbThis() + "已经被冻住了！"; break;    // :37
                    case "FROSTBITE": msg = pbThis() + "已经被冻伤了！"; break;    // :38
                    case "DROWSY":    msg = pbThis() + " is already drowsy!"; break; // :39
                    default: break;
                }
                display(msg);                                            // :41
            }
            return false;                                                // :43
        }
        if (statused() && !ignoreStatus && !selfInflicted) {             // :46
            if (showMessages) {                                          // :47
                display("这不能影响" + pbThis(true) + "……");
            }
            return false;                                                // :48
        }
        if (effects.intVal(PBEffects.Battler.Substitute) > 0
                && !(ignoresSubstitute(move, user)) && !selfInflicted) { // :51-52
            if (showMessages) {                                          // :53
                display("这不能影响" + pbThis(true) + "……");
            }
            return false;                                                // :54
        }
        // :57-61 天气免疫（冰冻）
        if (newStatus == PBStatuses.FROZEN) {
            int w = PendingApi.fieldWeather(battle);                     // :58 @battle.pbWeather
            if ((w == PBWeather.Sun || w == PBWeather.HarshSun) && !hasUtilityUmbrella()) {
                if (showMessages) {
                    display("这不能影响" + pbThis(true) + "……");
                }
                return false;
            }
        }
        // :63-75 场地免疫
        if (affectedByTerrain()) {
            int terrain = field == null ? PBBattleTerrains.None : field.terrain; // :64 @battle.field.terrain
            if (terrain == PBBattleTerrains.Electric && newStatus == PBStatuses.SLEEP) { // :65-66
                if (showMessages) {                                      // :67-68
                    display(pbThis(true) + "的周围电光飞闪！");
                }
                return false;                                            // :69
            }
            if (terrain == PBBattleTerrains.Misty) {                     // :71
                if (showMessages) {                                      // :72
                    display(pbThis(true) + "的周围雾气缭绕！");
                }
                return false;                                            // :73
            }
        }
        // :77-84 吵闹免疫（睡眠）
        if (newStatus == PBStatuses.SLEEP && !(hasActiveAbility("SOUNDPROOF") && !moldBreaker())) {
            for (Battler b : fieldBattlers()) {                          // :79 @battle.eachBattler
                if (b.effects.intVal(PBEffects.Battler.Uproar) == 0) {   // :80
                    continue;
                }
                if (showMessages) {                                      // :81
                    display("但是吵闹使得" + pbThis(true) + "醒来了！");
                }
                return false;                                            // :82
            }
        }
        // :86-108 属性免疫
        boolean hasImmuneType = false;
        switch (newStatus) {
            case PBStatuses.SLEEP:
                break;                                                   // :89 没有属性免疫睡眠
            case PBStatuses.POISON:                                      // :90-94
                if (!(user != null && user.hasActiveAbility("CORROSION"))) {
                    hasImmuneType |= pbHasType("POISON");
                    hasImmuneType |= pbHasType("STEEL");
                }
                break;
            case PBStatuses.BURN:                                        // :95-96
                hasImmuneType |= pbHasType("FIRE");
                break;
            case PBStatuses.PARALYSIS:                                   // :97-98
                hasImmuneType |= pbHasType("ELECTRIC") && Battle.NEWEST_BATTLE_MECHANICS;
                break;
            case PBStatuses.FROZEN:                                      // :99-100
                hasImmuneType |= pbHasType("ICE");
                break;
            case PBStatuses.FROSTBITE:                                   // :101-103
                hasImmuneType |= pbHasType("ICE");
                hasImmuneType |= pbHasType("FIRE");
                break;
            default:
                break;
        }
        if (hasImmuneType) {                                             // :105
            if (showMessages) {                                          // :106
                display("这不能影响" + pbThis(true) + "……");
            }
            return false;                                                // :107
        }
        // :110-179 特性免疫
        boolean immuneByAbility = false;
        Battler immAlly = null;
        if (BattleHandlers.triggerStatusImmunityAbilityNonIgnorable(ability, this, newStatus)) { // :111
            immuneByAbility = true;                                      // :112
        } else if (selfInflicted || !moldBreaker()) {                    // :113
            if (abilityActive() && BattleHandlers.triggerStatusImmunityAbility(ability, this, newStatus)) { // :114
                immuneByAbility = true;                                  // :115
            } else {
                for (Battler b : fieldBattlers()) {                       // :117 eachAlly
                    if (!b.abilityActive() || !b.opposes(index) && b.index == index) {
                        continue;
                    }
                    if (b.opposes(index)) {
                        continue;                                        // eachAlly 只走同伴
                    }
                    if (!BattleHandlers.triggerStatusImmunityAllyAbility(b.ability, this, newStatus)) { // :119
                        continue;
                    }
                    immuneByAbility = true;                              // :120
                    immAlly = b;                                         // :121
                    break;                                               // :122
                }
            }
        }
        if (immuneByAbility) {                                           // :126
            if (showMessages) {                                          // :127
                Battler splashOn = immAlly != null ? immAlly : this;
                PendingApi.pbShowAbilitySplash(battle, splashOn);         // :128
                String msg = "";
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {       // :130
                    switch (newStatus) {                                  // :131-138
                        case PBStatuses.SLEEP:     msg = pbThis() + "依旧保持清醒！"; break;
                        case PBStatuses.POISON:    msg = pbThis() + "不能陷入中毒状态!"; break;
                        case PBStatuses.BURN:      msg = pbThis() + "不能陷入灼伤状态！"; break;
                        case PBStatuses.PARALYSIS: msg = pbThis() + "不能陷入麻痹状态！"; break;
                        case PBStatuses.FROZEN:    msg = pbThis() + "不能陷入冰冻状态！"; break;
                        case PBStatuses.FROSTBITE: msg = pbThis() + "不能陷入冻伤状态！"; break;
                        case PBStatuses.DROWSY:    msg = pbThis() + " resists drowsiness!"; break;
                        default: break;
                    }
                } else if (immAlly != null) {                             // :140-163
                    msg = pbThis() + "因为" + immAlly.pbThis(true) + "的"
                            + immAlly.abilityName() + "而没有受到影响！";
                } else {                                                  // :164-174
                    msg = abilityName() + "防止了" + pbThis() + "陷入异常状态！";
                }
                display(msg);                                             // :175
                PendingApi.pbHideAbilitySplash(battle, splashOn);         // :176
            }
            return false;                                                // :178
        }
        // :181-186 神秘守护
        if (pbOwnSide().effects.intVal(PBEffects.Side.Safeguard) > 0 && !selfInflicted && move != null
                && !(user != null && (user.hasActiveAbility("INFILTRATOR")
                                      || user.hasActiveAbility("TRANSLUCENTGHOST")))) {
            if (showMessages) {                                          // :184
                display(pbThis() + "的队伍受到了神秘守护的保护！");
            }
            return false;                                                // :185
        }
        // :188-198 Boss 免疫
        if (pokemon != null && pokemon.battleRank > 2) {                 // :188
            if (newStatus == PBStatuses.BURN || newStatus == PBStatuses.FROSTBITE
                    || newStatus == PBStatuses.POISON) {                 // :189
                String statusName = newStatus == PBStatuses.BURN ? "烧伤"
                        : newStatus == PBStatuses.FROSTBITE ? "冻伤" : "中毒"; // :191-193
                if (showMessages) {                                      // :195
                    PendingApi.pbDisplay(battle,
                            "<c3=FFEE88,FF6600>" + pbThis() + "凭借它的力量防止了" + statusName + "！");
                }
                return false;                                            // :196
            }
        }
        return true;                                                     // :199
    }

    /** {@code pbCanSynchronizeStatus?} (Battler_Statuses:202-250)。 */
    public boolean pbCanSynchronizeStatus(int newStatus, Battler target) {
        if (fainted()) {                                                 // :203
            return false;
        }
        if (statused()) {                                                // :205
            return false;
        }
        int terrain = field == null ? PBBattleTerrains.None : field.terrain;
        if (terrain == PBBattleTerrains.Misty && affectedByTerrain()) {   // :207
            return false;
        }
        boolean hasImmuneType = false;                                    // :209-224
        switch (newStatus) {
            case PBStatuses.POISON:
                if (!(target != null && target.hasActiveAbility("CORROSION"))) {
                    hasImmuneType |= pbHasType("POISON");
                    hasImmuneType |= pbHasType("STEEL");
                }
                break;
            case PBStatuses.BURN:
                hasImmuneType |= pbHasType("FIRE");
                break;
            case PBStatuses.PARALYSIS:
                hasImmuneType |= pbHasType("ELECTRIC") && Battle.NEWEST_BATTLE_MECHANICS;
                break;
            case PBStatuses.FROSTBITE:
                hasImmuneType |= pbHasType("ICE");
                hasImmuneType |= pbHasType("FIRE");
                break;
            default:
                break;
        }
        if (hasImmuneType) {                                             // :225
            return false;
        }
        if (BattleHandlers.triggerStatusImmunityAbilityNonIgnorable(ability, this, newStatus)) { // :227
            return false;
        }
        if (abilityActive() && BattleHandlers.triggerStatusImmunityAbility(ability, this, newStatus)) { // :230
            return false;
        }
        for (Battler b : fieldBattlers()) {                               // :233 eachAlly
            if (b.opposes(index)) {
                continue;
            }
            if (!b.abilityActive()) {                                     // :234
                continue;
            }
            if (!BattleHandlers.triggerStatusImmunityAllyAbility(b.ability, this, newStatus)) { // :235
                continue;
            }
            return false;                                                 // :236
        }
        // :239-243 神秘守护
        // ⚠️ 插件自身缺陷：:240 用了**未定义的局部变量 `user`**（方法签名只有 newStatus,target），
        // 所以这一行在 Ruby 里走到就 NameError。照抄形状 + 登记，不自造替代：
        //   if pbOwnSide.effects[PBEffects::Safeguard] > 0 && !(user && (...))
        // 登记: Battler_Statuses:239-243 未定义的 `user`（插件缺陷，等用户拍板）
        if (pbOwnSide().effects.intVal(PBEffects.Side.Safeguard) > 0) {
            // 分支体在 Ruby 里是 return false（:242），但前置条件必然抛错；
            // 这里按「条件为真即 false」转译，并把缺陷写在上面的登记里。
            return false;
        }
        if ((newStatus == PBStatuses.BURN || newStatus == PBStatuses.POISON)
                && pokemon != null && pokemon.battleRank > 2) {            // :245-246
            return false;
        }
        return true;                                                       // :249
    }

    /** {@code pbCanPoisonSynchronize?} (Battler_Statuses:379-381)。 */
    public boolean pbCanPoisonSynchronize(Battler target) {
        return pbCanSynchronizeStatus(PBStatuses.POISON, target);
    }

    /** {@code pbCanBurnSynchronize?} (Battler_Statuses:398-400)。 */
    public boolean pbCanBurnSynchronize(Battler target) {
        return pbCanSynchronizeStatus(PBStatuses.BURN, target);
    }

    /** {@code pbCanParalyzeSynchronize?} (Battler_Statuses:417-419)。 */
    public boolean pbCanParalyzeSynchronize(Battler target) {
        return pbCanSynchronizeStatus(PBStatuses.PARALYSIS, target);
    }

    /** {@code pbInflictStatus} (Battler_Statuses:255-309)。 */
    public void pbInflictStatus(int newStatus, int newStatusCount, String msg, Battler user) {
        status = PBStatuses.nameOf(newStatus);                            // :257 self.status = newStatus
        setStatusCount(newStatusCount);                                   // :258
        effects.set(PBEffects.Battler.Toxic, 0);                          // :259
        String message = msg;
        switch (newStatus) {                                              // :261-288
            case PBStatuses.SLEEP:
                PendingApi.pbCommonAnimation(battle, "Sleep", this);       // :263
                if (message == null || message.isEmpty()) {
                    message = pbThis() + "睡着了！";                        // :264
                }
                break;
            case PBStatuses.POISON:
                if (newStatusCount > 0) {                                  // :266
                    PendingApi.pbCommonAnimation(battle, "Toxic", this);   // :267
                    if (message == null || message.isEmpty()) {
                        message = pbThis() + "中了剧毒！";                  // :268
                    }
                } else {
                    PendingApi.pbCommonAnimation(battle, "Poison", this);  // :270
                    if (message == null || message.isEmpty()) {
                        message = pbThis() + "中毒了！";                    // :271
                    }
                }
                break;
            case PBStatuses.BURN:
                PendingApi.pbCommonAnimation(battle, "Burn", this);        // :274
                if (message == null || message.isEmpty()) {
                    message = pbThis() + "被灼伤了！";                      // :275
                }
                break;
            case PBStatuses.PARALYSIS:
                PendingApi.pbCommonAnimation(battle, "Paralysis", this);   // :277
                if (message == null || message.isEmpty()) {
                    message = pbThis() + "麻痹了！\n有可能无法行动！";       // :278
                }
                break;
            case PBStatuses.FROZEN:
                PendingApi.pbCommonAnimation(battle, "Frozen", this);      // :280
                if (message == null || message.isEmpty()) {
                    message = pbThis() + "被冰冻了！";                      // :281
                }
                break;
            case PBStatuses.FROSTBITE:
                PendingApi.pbCommonAnimation(battle, "Frostbitten", this); // :283
                if (message == null || message.isEmpty()) {
                    message = pbThis() + "被冻伤了！";                      // :284
                }
                break;
            case PBStatuses.DROWSY:
                PendingApi.pbCommonAnimation(battle, "Drowsy", this);      // :286
                if (message == null || message.isEmpty()) {
                    message = pbThis() + " is drowsy!";                     // :287
                }
                break;
            default:
                break;
        }
        if (message != null && !message.isEmpty()) {                       // :290
            display(message);
        }
        pbCheckFormOnStatusChange();                                       // :292
        if (abilityActive()) {                                             // :294
            BattleHandlers.triggerAbilityOnStatusInflicted(ability, this, user, newStatus); // :295
        }
        pbItemStatusCureCheck(0, false);                                   // :298
        pbAbilityStatusCureCheck();                                        // :299
        if ("SLEEP".equals(status) && effects.intVal(PBEffects.Battler.Outrage) > 0) { // :305
            effects.set(PBEffects.Battler.Outrage, 0);                     // :306
            currentMove = 0;                                               // :307 @currentMove = 0 (was mis-mapped to lastMoveUsed = null before stage 5)
        }
    }

    /** {@code pbCureStatus} (Battler_Statuses:468-483)。 */
    public void pbCureStatus() {
        pbCureStatus(true);
    }

    /** {@code pbCureStatus(showMessages=true)} (Battler_Statuses:468-483)。 */
    public void pbCureStatus(boolean showMessages) {
        int oldStatus = PBStatuses.idOf(status);                           // :469 oldStatus = status
        cureStatus();                                                      // :470 self.status = PBStatuses::NONE
        if (showMessages) {                                                // :471
            switch (oldStatus) {                                           // :472-480
                case PBStatuses.SLEEP:     display(pbThis() + "醒来了！"); break;
                case PBStatuses.POISON:    display(pbThis() + "的毒被消去了！"); break;
                case PBStatuses.BURN:      display(pbThis() + "的灼伤被治愈了！"); break;
                case PBStatuses.PARALYSIS: display(pbThis() + "的麻痹被解除了！"); break;
                case PBStatuses.FROZEN:    display(pbThis() + "不再被冰冻了！"); break;
                case PBStatuses.FROSTBITE: display(pbThis() + "'s frostbite was healed!"); break;
                case PBStatuses.DROWSY:    display(pbThis() + " is no longer drowsy!"); break;
                default: break;
            }
        }
    }

    /** {@code pbCureConfusion} (Battler_Statuses:547-549)。 */
    public void pbCureConfusion() {
        effects.set(PBEffects.Battler.Confusion, 0);                       // :548
    }

    /** {@code pbCanConfuse?} (Battler_Statuses:488-525)。 */
    public boolean pbCanConfuse(Battler user, boolean showMessages, BattleMove move, boolean selfInflicted) {
        if (fainted()) {                                                   // :489
            return false;
        }
        if (effects.intVal(PBEffects.Battler.Confusion) > 0) {              // :490
            if (showMessages) {                                            // :491
                display(pbThis() + "已经混乱了！");
            }
            return false;                                                  // :492
        }
        if (effects.intVal(PBEffects.Battler.Substitute) > 0
                && !(ignoresSubstitute(move, user)) && !selfInflicted) { // :494-495
            if (showMessages) {                                            // :496
                display("但是失败了！");
            }
            return false;                                                  // :497
        }
        int terrain = field == null ? PBBattleTerrains.None : field.terrain;
        if (affectedByTerrain() && terrain == PBBattleTerrains.Misty) {     // :500
            if (showMessages) {                                            // :501
                display(pbThis(true) + "的周围雾气缭绕！");
            }
            return false;                                                  // :502
        }
        if (selfInflicted || !moldBreaker()) {                             // :504
            if (hasActiveAbility(new String[] {"OWNTEMPO", "DEMONKILLER"})) { // :505
                if (showMessages) {                                        // :506
                    PendingApi.pbShowAbilitySplash(battle, this);          // :507
                    if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {    // :508
                        display(pbThis() + "并没有混乱！");                 // :509
                    } else {
                        display(pbThis() + "的" + abilityName() + "防止了混乱！"); // :511
                    }
                    PendingApi.pbHideAbilitySplash(battle, this);          // :513
                }
                return false;                                              // :515
            }
        }
        if (pbOwnSide().effects.intVal(PBEffects.Side.Safeguard) > 0 && !selfInflicted
                && !(user != null && (user.hasActiveAbility("INFILTRATOR")
                                      || user.hasActiveAbility("TRANSLUCENTGHOST")))) { // :518-520
            if (showMessages) {                                            // :521
                display(pbThis() + "的队伍受到了神秘守护的保护！");
            }
            return false;                                                  // :522
        }
        return true;                                                       // :524
    }

    /** {@code pbCureAttract} (Battler_Statuses:612-614)。 */
    public void pbCureAttract() {
        effects.set(PBEffects.Battler.Attract, -1);                        // :613
    }

    /** {@code pbCanAttract?} (Battler_Statuses:554-596)。 */
    public boolean pbCanAttract(Battler user, boolean showMessages) {
        if (fainted()) {                                                   // :555
            return false;
        }
        if (user == null || user.fainted()) {                              // :556
            return false;
        }
        if (effects.intVal(PBEffects.Battler.Attract) >= 0) {               // :557
            if (showMessages) {                                            // :558
                display(pbThis() + "没有受到影响！");
            }
            return false;                                                  // :559
        }
        int agender = user.gender();                                         // :561
        int ogender = gender();                                              // :562
        if (agender == 2 || ogender == 2 || agender == ogender) {           // :563
            if (showMessages) {                                            // :564
                display(pbThis() + "没有受到影响！");
            }
            return false;                                                  // :565
        }
        if (!moldBreaker()) {                                              // :567
            if (hasActiveAbility(new String[] {"AROMAVEIL", "OBLIVIOUS"})) { // :568
                if (showMessages) {                                        // :569
                    PendingApi.pbShowAbilitySplash(battle, this);          // :570
                    if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {    // :571
                        display(pbThis() + "没有受到影响！");               // :572
                    } else {
                        display(abilityName() + "防止了" + pbThis() + "着迷！"); // :574
                    }
                    PendingApi.pbHideAbilitySplash(battle, this);          // :576
                }
                return false;                                              // :578
            }
            for (Battler b : fieldBattlers()) {                            // :580 eachAlly
                if (b.opposes(index) || !b.hasActiveAbility("AROMAVEIL")) { // :581
                    continue;
                }
                if (showMessages) {                                        // :582
                    PendingApi.pbShowAbilitySplash(battle, this);          // :583
                    if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {    // :584
                        display(pbThis() + "没有受到影响！");               // :585
                    } else {
                        display(b.abilityName() + "防止了" + pbThis() + "着迷！"); // :587
                    }
                    PendingApi.pbHideAbilitySplash(battle, this);          // :589
                }
                return true;                                               // :591（插件如此，照抄）
            }
        }
        return true;                                                       // :595
    }

    /** {@code pbAttract} (Battler_Statuses:598-610)。 */
    public void pbAttract(Battler user, String msg) {
        effects.set(PBEffects.Battler.Attract, user == null ? -1 : user.index); // :599
        PendingApi.pbCommonAnimation(battle, "Attract", this);             // :600
        String message = msg;
        if (message == null || message.isEmpty()) {                        // :601
            message = pbThis() + "坠入爱河了！";
        }
        display(message);                                                  // :602
        if (hasActiveItem("DESTINYKNOT") && user != null && user.pbCanAttract(this, false)) { // :604
            user.pbAttract(this, user.pbThis(true) + "爱上了" + itemName() + "！"); // :605
        }
        pbItemStatusCureCheck(0, false);                                   // :608
        pbAbilityStatusCureCheck();                                        // :609
    }

    /** {@code pbCheckFormOnStatusChange} (Battler_ChangeSelf:197-215)。 */
    public void pbCheckFormOnStatusChange() {
        if (fainted() || effects.truthy(PBEffects.Battler.Transform)) {     // :198
            return;
        }
        if (isSpecies("SHAYMIN") && frozen()) {                            // :200
            pbChangeFormTransform(0, pbThis() + "变身了！");                // :201
        } else if (isSpecies("ROSEDRAGON") && hasActiveAbility("SLEEPSOUNDLY")) { // :202
            if (asleep()) {                                                // :203
                pbChangeFormTransform(1, pbThis() + "的身体变得坚硬！");     // :204
            } else {
                pbChangeFormTransform(0, pbThis() + "变得具有攻击性！");     // :206
                if (pbCanRaiseStatStage(PBStats.ATTACK)) {                  // :207
                    pbRaiseStatStageByAbility(PBStats.ATTACK, 1, this, false); // :208
                }
                if (pbCanRaiseStatStage(PBStats.SPEED)) {                   // :210
                    pbRaiseStatStageByAbility(PBStats.SPEED, 1, this, false); // :211
                }
            }
        }
    }

    /** {@code pbCheckFormOnWeatherChange} (Battler_ChangeSelf:227-280)。 */
    public void pbCheckFormOnWeatherChange() {
        if (fainted() || effects.truthy(PBEffects.Battler.Transform)) {     // :228
            return;
        }
        if (hasUtilityUmbrella()) {                                        // :229
            return;
        }
        int w = PendingApi.fieldWeather(battle);                           // :234 @battle.pbWeather
        if (isSpecies("CASTFORM")) {                                       // :231
            if (hasActiveAbility("FORECAST")) {                            // :232
                int newForm = 0;                                           // :233
                if (w == PBWeather.Sun || w == PBWeather.HarshSun) {        // :235-236
                    newForm = 1;
                } else if (w == PBWeather.Rain || w == PBWeather.HeavyRain) { // :237-238
                    newForm = 2;
                } else if (w == PBWeather.Hail) {                           // :239-240
                    newForm = 3;
                }
                if (form() != newForm) {                                   // :242
                    PendingApi.pbShowAbilitySplash(battle, this);           // :243
                    PendingApi.pbHideAbilitySplash(battle, this);           // :244
                    pbChangeFormTransform(newForm, pbThis() + "变身了！");   // :245
                }
            } else {
                pbChangeFormTransform(0, pbThis() + "变身了！");            // :248
            }
        }
        if (isSpecies("CHERRIM")) {                                        // :252
            if (hasActiveAbility("FLOWERGIFT")) {                          // :253
                int newForm = 0;                                           // :254
                if (w == PBWeather.Sun || w == PBWeather.HarshSun) {        // :256
                    newForm = 1;
                }
                if (form() != newForm) {                                   // :258
                    PendingApi.pbShowAbilitySplash(battle, this);           // :259
                    PendingApi.pbHideAbilitySplash(battle, this);           // :260
                    pbChangeFormTransform(newForm, pbThis() + "变身了！");   // :261
                }
            } else {
                pbChangeFormTransform(0, pbThis() + "变身了！");            // :264
            }
        }
        if (isSpecies("EISCUE") && hasActiveAbility("ICEFACE")                 // :268-269
                && (w == PBWeather.Hail || w == PBWeather.Snow)) {
            if (form() == 1) {                                             // :270
                PendingApi.pbShowAbilitySplash(battle, this);               // :271
                PendingApi.pbHideAbilitySplash(battle, this);               // :272
                pbChangeFormTransform(0, pbThis() + "改变了形态！");         // :273
            }
        }
        if (hasActiveAbility("PROTOSYNTHESIS")) {                          // :277 古代活性
            BattleHandlers.triggerAbilityOnSwitchIn(ability, this, battle); // :278
        }
    }

    /** {@code pbChangeFormTransform} (Battler_ChangeSelf:182-195)。 */
    public void pbChangeFormTransform(int newForm, String msg) {
        if (fainted() || effects.truthy(PBEffects.Battler.Transform) || form() == newForm) { // :183
            return;
        }
        int oldForm = form();                                              // :184
        int oldDmg = maxHp() - hp;                                         // :185
        // :186 self.form = newForm（本运行时 form 落在 Pokemon.form 上；Battler 无 setter → 见下）
        // 登记: Battler_ChangeSelf:186-191 self.form= / pbUpdate(true) / @hp=@totalhp-oldDmg
        //       / @battle.scene.pbChangePokemonTransform / @battle.scene.pbRefreshOne
        //       依赖 Pokemon.setForm(PbsData) 与 PokeBattle_Scene（本批未建模）
        if (pbs != null && pokemon != null) {
            pokemon.setForm(pbs, newForm);
        }
        hp = maxHp() - oldDmg;                                             // :188 @hp = @totalhp-oldDmg
        if (Battle.NEWEST_BATTLE_MECHANICS) {                              // :189
            effects.set(PBEffects.Battler.WeightChange, 0);
        }
        if (msg != null && !msg.isEmpty()) {                               // :192
            display(msg);
        }
        // 登记: Battler_ChangeSelf:194 @battle.pbSetSeen(self) 依赖图鉴/存档
    }

    // =========================================================================
    // 能力等级（Battler_StatStages）
    // =========================================================================

    /** {@code statStageAtMax?} (Battler_StatStages:5-7)。 */
    public boolean statStageAtMax(int stat) {
        return stage(stat) >= 6;                                           // :6
    }

    /** {@code statStageAtMin?} (Battler_StatStages:126-128)。 */
    public boolean statStageAtMin(int stat) {
        return stage(stat) <= -6;                                          // :127
    }

    /** {@code pbCanRaiseStatStage?(stat)} (Battler_StatStages:9-26)，其余参数取默认值。 */
    public boolean pbCanRaiseStatStage(int stat) {
        return pbCanRaiseStatStage(stat, null, null, false, false);
    }

    /** {@code pbCanRaiseStatStage?(stat,user)} (Battler_StatStages:9-26)。 */
    public boolean pbCanRaiseStatStage(int stat, Battler user) {
        return pbCanRaiseStatStage(stat, user, null, false, false);
    }

    /** {@code pbCanRaiseStatStage?(stat,user,move,showFailMsg)} (Battler_StatStages:9-26)。 */
    public boolean pbCanRaiseStatStage(int stat, Battler user, BattleMove move, boolean showFailMsg) {
        return pbCanRaiseStatStage(stat, user, move, showFailMsg, false);
    }

    /** {@code pbCanRaiseStatStage?} (Battler_StatStages:9-26)。40 次。 */
    public boolean pbCanRaiseStatStage(int stat, Battler user, BattleMove move,
                                       boolean showFailMsg, boolean ignoreContrary) {
        if (fainted()) {                                                   // :10
            return false;
        }
        if (hasActiveAbility("CONTRARY") && !ignoreContrary && !moldBreaker()) { // :12
            return pbCanLowerStatStage(stat, user, move, showFailMsg, true); // :13
        }
        if (abilityActive()) {                                             // :15
            if (!moldBreaker() && BattleHandlers.triggerStatGainImmunityAbility(
                    ability, this, stat, battle, showFailMsg)) {            // :16-17
                return false;
            }
        }
        if (statStageAtMax(stat)) {                                        // :20
            if (showFailMsg) {                                             // :21-22
                display(pbThis() + "的" + PBStats.getName(stat) + "不能再提高了！");
            }
            return false;                                                  // :23
        }
        return true;                                                       // :25
    }

    /** {@code pbRaiseStatStageBasic} (Battler_StatStages:28-45)：返回实际增量。 */
    public int pbRaiseStatStageBasic(int stat, int increment, boolean ignoreContrary) {
        int inc = increment;
        if (!moldBreaker()) {                                              // :29
            if (hasActiveAbility("CONTRARY") && !ignoreContrary) {          // :31
                return pbLowerStatStageBasic(stat, inc, true);              // :32
            }
            if (hasActiveAbility("SIMPLE")) {                               // :35
                inc *= 2;
            }
        }
        inc = Math.min(inc, 6 - stage(stat));                               // :38
        if (inc > 0) {                                                      // :39
            setStage(stat, stage(stat) + inc);                              // :42
        }
        return inc;                                                         // :44
    }

    /** {@code pbRaiseStatStage} (Battler_StatStages:47-72)。 */
    public boolean pbRaiseStatStage(int stat, int increment, Battler user) {
        return pbRaiseStatStage(stat, increment, user, true, false);
    }

    /** {@code pbRaiseStatStage(stat,increment,user,showAnim,ignoreContrary)} (Battler_StatStages:47-72)。 */
    public boolean pbRaiseStatStage(int stat, int increment, Battler user,
                                    boolean showAnim, boolean ignoreContrary) {
        if (!PBStats.validBattleStat(stat)) {                               // :48
            return false;
        }
        if (hasActiveAbility("CONTRARY") && !ignoreContrary && !moldBreaker()) { // :50
            return pbLowerStatStage(stat, increment, user, showAnim, true, false); // :51
        }
        int inc = pbRaiseStatStageBasic(stat, increment, ignoreContrary);   // :54
        if (inc <= 0) {                                                     // :55
            return false;
        }
        if (showAnim) {                                                     // :57
            PendingApi.pbCommonAnimation(battle, "StatUp", this);
        }
        String[] texts = {                                                  // :58-61
            pbThis() + "的" + PBStats.getName(stat) + "提升了！",
            pbThis() + "的" + PBStats.getName(stat) + "大幅提升了！",
            pbThis() + "的" + PBStats.getName(stat) + "巨幅提升了！"
        };
        display(texts[Math.min(inc - 1, 2)]);                               // :62
        if (abilityActive()) {                                              // :64
            BattleHandlers.triggerAbilityOnStatGain(ability, this, stat, user); // :65
        }
        effects.set(PBEffects.Battler.BurningJealousy, true);               // :67
        if (!mirrorHerbUsed && !(hasActiveAbility("CONTRARY") && !ignoreContrary && !moldBreaker())) { // :68
            addSideStatUps(stat, inc);                                      // :69
        }
        return true;                                                        // :71
    }

    /** {@code pbRaiseStatStageByCause} (Battler_StatStages:74-106)。 */
    public boolean pbRaiseStatStageByCause(int stat, int increment, Battler user, String cause) {
        return pbRaiseStatStageByCause(stat, increment, user, cause, true, false);
    }

    /** {@code pbRaiseStatStageByCause(...)} (Battler_StatStages:74-106)。17 次。 */
    public boolean pbRaiseStatStageByCause(int stat, int increment, Battler user, String cause,
                                           boolean showAnim, boolean ignoreContrary) {
        if (!PBStats.validBattleStat(stat)) {                               // :75
            return false;
        }
        if (hasActiveAbility("CONTRARY") && !ignoreContrary && !moldBreaker()) { // :77
            return pbLowerStatStageByCause(stat, increment, user, cause, showAnim, true, false); // :78
        }
        int inc = pbRaiseStatStageBasic(stat, increment, ignoreContrary);   // :81
        if (inc <= 0) {                                                     // :82
            return false;
        }
        if (showAnim) {                                                     // :84
            PendingApi.pbCommonAnimation(battle, "StatUp", this);
        }
        String[] texts;
        if (user != null && user.index == index) {                          // :85
            texts = new String[] {                                          // :86-89
                pbThis() + "的" + cause + "提升了" + PBStats.getName(stat) + "！",
                pbThis() + "的" + cause + "大幅提升了" + PBStats.getName(stat) + "！",
                pbThis() + "的" + cause + "巨幅提升了" + PBStats.getName(stat) + "！"
            };
        } else {
            String userName = user == null ? "" : user.pbThis();
            texts = new String[] {                                          // :91-94
                userName + "的" + cause + "提升了" + pbThis(true) + "的" + PBStats.getName(stat) + "！",
                userName + "的" + cause + "大幅提升了" + pbThis(true) + "的" + PBStats.getName(stat) + "！",
                userName + "的" + cause + "巨幅提升了" + pbThis(true) + "的" + PBStats.getName(stat) + "！"
            };
        }
        display(texts[Math.min(inc - 1, 2)]);                               // :96
        if (abilityActive()) {                                              // :98
            BattleHandlers.triggerAbilityOnStatGain(ability, this, stat, user); // :99
        }
        effects.set(PBEffects.Battler.BurningJealousy, true);               // :101
        if (!mirrorHerbUsed && !(hasActiveAbility("CONTRARY") && !ignoreContrary && !moldBreaker())) { // :102
            addSideStatUps(stat, inc);                                      // :103
        }
        return true;                                                        // :105
    }

    /** {@code pbRaiseStatStageByAbility} (Battler_StatStages:108-122)。32 次。 */
    public boolean pbRaiseStatStageByAbility(int stat, int increment, Battler user, boolean splashAnim) {
        if (fainted()) {                                                    // :109
            return false;
        }
        boolean ret = false;                                                // :110
        if (splashAnim) {                                                   // :111
            PendingApi.pbShowAbilitySplash(battle, user);
        }
        if (pbCanRaiseStatStage(stat, user, null, PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) { // :112
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {             // :113
                ret = pbRaiseStatStage(stat, increment, user);              // :114
            } else {
                ret = pbRaiseStatStageByCause(stat, increment, user,
                        user == null ? "" : user.abilityName());            // :116
            }
        }
        if (splashAnim) {                                                   // :119
            PendingApi.pbHideAbilitySplash(battle, user);
        }
        pbMirrorStatUpsOpposing();                                          // :120
        return ret;                                                         // :121
    }

    /** {@code pbCanLowerStatStage?} (Battler_StatStages:130-170)。 */
    public boolean pbCanLowerStatStage(int stat) {
        return pbCanLowerStatStage(stat, null, null, false, false);
    }

    /** {@code pbCanLowerStatStage?} (Battler_StatStages:130-170)。 */
    public boolean pbCanLowerStatStage(int stat, Battler user) {
        return pbCanLowerStatStage(stat, user, null, false, false);
    }

    /** {@code pbCanLowerStatStage?(stat,user,move,showFailMsg,ignoreContrary)} (Battler_StatStages:130-170)。 */
    public boolean pbCanLowerStatStage(int stat, Battler user, BattleMove move,
                                       boolean showFailMsg, boolean ignoreContrary) {
        if (fainted()) {                                                    // :131
            return false;
        }
        if (hasActiveAbility("CONTRARY") && !ignoreContrary && !moldBreaker()) { // :133
            return pbCanRaiseStatStage(stat, user, move, showFailMsg, true); // :134
        }
        if (user == null || user.index != index) {                          // :136 非自我施加
            if (itemActive()) {                                             // :137
                // 注意：triggerStatLossImmunityItem 返回 **装箱 Boolean**（BattleHandlers:228 的
                // `handler != null ? ... : null`），没命中 handler 时是 null → 必须用 TRUE.equals 判。
                if (Boolean.TRUE.equals(BattleHandlers.triggerStatLossImmunityItem(
                        item, this, stat, battle, showFailMsg))) {          // :138
                    return false;
                }
            }
            if (effects.intVal(PBEffects.Battler.Substitute) > 0
                    && !(ignoresSubstitute(move, user))) {    // :140
                if (showFailMsg) {                                          // :141
                    display(pbThis() + "被替身保护了！");
                }
                return false;                                               // :142
            }
            if (pbOwnSide().effects.intVal(PBEffects.Side.Mist) > 0
                    && !(user != null && (user.hasActiveAbility("INFILTRATOR")
                                          || user.hasActiveAbility("TRANSLUCENTGHOST")))) { // :144-146
                if (showFailMsg) {                                          // :147
                    display(pbThis() + " 被白雾保护了！");
                }
                return false;                                               // :148
            }
            if (abilityActive()) {                                          // :150
                if (!moldBreaker() && BattleHandlers.triggerStatLossImmunityAbility(
                        ability, this, stat, battle, showFailMsg)) {         // :151-152
                    return false;
                }
                if (BattleHandlers.triggerStatLossImmunityAbilityNonIgnorable(
                        ability, this, stat, battle, showFailMsg)) {         // :153-154
                    return false;
                }
            }
            if (!moldBreaker()) {                                           // :156
                for (Battler b : fieldBattlers()) {                          // :157 eachAlly
                    if (b.opposes(index)) {
                        continue;
                    }
                    if (!b.abilityActive()) {                                // :158
                        continue;
                    }
                    if (BattleHandlers.triggerStatLossImmunityAllyAbility(
                            b.ability, b, this, stat, battle, showFailMsg)) { // :159-160
                        return false;
                    }
                }
            }
        }
        if (statStageAtMin(stat)) {                                         // :165
            if (showFailMsg) {                                              // :166
                display(pbThis() + "的" + PBStats.getName(stat) + "不能再降低了！");
            }
            return false;                                                   // :167
        }
        return true;                                                        // :169
    }

    /** {@code pbLowerStatStageBasic} (Battler_StatStages:172-189)：返回实际增量。 */
    public int pbLowerStatStageBasic(int stat, int increment, boolean ignoreContrary) {
        int inc = increment;
        if (!moldBreaker()) {                                               // :173
            if (hasActiveAbility("CONTRARY") && !ignoreContrary) {           // :175
                return pbRaiseStatStageBasic(stat, inc, true);               // :176
            }
            if (hasActiveAbility("SIMPLE")) {                                // :179
                inc *= 2;
            }
        }
        inc = Math.min(inc, 6 + stage(stat));                                // :182
        if (inc > 0) {                                                       // :183
            setStage(stat, stage(stat) - inc);                               // :186
        }
        return inc;                                                          // :188
    }

    /** {@code pbLowerStatStage} (Battler_StatStages:191-235)。 */
    public boolean pbLowerStatStage(int stat, int increment, Battler user) {
        return pbLowerStatStage(stat, increment, user, true, false, false);
    }

    /** {@code pbLowerStatStage(stat,increment,user,showAnim,ignoreContrary,ignoreMirrorArmor)} (Battler_StatStages:191-235)。 */
    public boolean pbLowerStatStage(int stat, int increment, Battler user, boolean showAnim,
                                    boolean ignoreContrary, boolean ignoreMirrorArmor) {
        if (!PBStats.validBattleStat(stat)) {                                // :192
            return false;
        }
        // :194-214 Mirror Armor / SPOVERLORD
        if (!ignoreMirrorArmor && (hasActiveAbility("MIRRORARMOR") || hasActiveAbility("SPOVERLORD"))
                && (user == null || user.index != index) && !moldBreaker()
                && pbCanLowerStatStage(stat)) {
            PendingApi.pbShowAbilitySplash(battle, this);                    // :198
            display(pbThis() + "的" + abilityName() + "发动了！");            // :199
            if (user == null) {                                              // :200
                PendingApi.pbHideAbilitySplash(battle, this);                 // :201
                return false;                                                // :202
            }
            if ((!user.hasActiveAbility("MIRRORARMOR") || !user.hasActiveAbility("SPOVERLORD"))
                    && user.pbCanLowerStatStage(stat, null, null, true, false)) { // :204-205
                user.pbLowerStatStageByAbility(stat, increment, user, false, false); // :206
                if (user.abilityActive()) {                                  // :208
                    BattleHandlers.triggerAbilityOnStatLoss(user.ability, user, stat, this); // :209
                }
            }
            PendingApi.pbHideAbilitySplash(battle, this);                    // :212
            return false;                                                    // :213
        }
        if (hasActiveAbility("CONTRARY") && !ignoreContrary && !moldBreaker()) { // :216
            return pbRaiseStatStage(stat, increment, user, showAnim, true);   // :217
        }
        int inc = pbLowerStatStageBasic(stat, increment, ignoreContrary);      // :220
        if (inc <= 0) {                                                       // :221
            return false;
        }
        if (showAnim) {                                                       // :223
            PendingApi.pbCommonAnimation(battle, "StatDown", this);
        }
        String[] texts = {                                                    // :224-227
            pbThis() + "的" + PBStats.getName(stat) + "降低了！",
            pbThis() + "的" + PBStats.getName(stat) + "大幅降低了！",
            pbThis() + "的" + PBStats.getName(stat) + "巨幅降低了！"
        };
        display(texts[Math.min(inc - 1, 2)]);                                 // :228
        if (abilityActive()) {                                                // :230
            BattleHandlers.triggerAbilityOnStatLoss(ability, this, stat, user); // :231
        }
        effects.set(PBEffects.Battler.LashOut, true);                         // :233
        return true;                                                          // :234
    }

    /** {@code pbLowerStatStageByCause} (Battler_StatStages:237-287)。 */
    public boolean pbLowerStatStageByCause(int stat, int increment, Battler user, String cause) {
        return pbLowerStatStageByCause(stat, increment, user, cause, true, false, false);
    }

    /** {@code pbLowerStatStageByCause(...)} (Battler_StatStages:237-287)。 */
    public boolean pbLowerStatStageByCause(int stat, int increment, Battler user, String cause,
                                           boolean showAnim, boolean ignoreContrary, boolean ignoreMirrorArmor) {
        if (!PBStats.validBattleStat(stat)) {                                 // :238
            return false;
        }
        // :240-259 Mirror Armor（与 pbLowerStatStage 同形，Ruby 的括号写法略有不同，照抄）
        if ((!ignoreMirrorArmor && hasActiveAbility("MIRRORARMOR"))
                || (!ignoreMirrorArmor && hasActiveAbility("SPOVERLORD"))) {
            if ((user == null || user.index != index) && !moldBreaker() && pbCanLowerStatStage(stat)) {
                PendingApi.pbShowAbilitySplash(battle, this);                 // :243
                display(pbThis() + "的" + abilityName() + "发动了！");         // :244
                if (user == null) {                                           // :245
                    PendingApi.pbHideAbilitySplash(battle, this);              // :246
                    return false;                                             // :247
                }
                if ((!user.hasActiveAbility("MIRRORARMOR") || !user.hasActiveAbility("SPOVERLORD"))
                        && user.pbCanLowerStatStage(stat, null, null, true, false)) { // :249-250
                    user.pbLowerStatStageByAbility(stat, increment, user, false, false); // :251
                    if (user.abilityActive()) {                               // :253
                        BattleHandlers.triggerAbilityOnStatLoss(user.ability, user, stat, this); // :254
                    }
                }
                PendingApi.pbHideAbilitySplash(battle, this);                 // :257
                return false;                                                 // :258
            }
        }
        if (hasActiveAbility("CONTRARY") && !ignoreContrary && !moldBreaker()) { // :261
            return pbRaiseStatStageByCause(stat, increment, user, cause, showAnim, true); // :262
        }
        int inc = pbLowerStatStageBasic(stat, increment, ignoreContrary);       // :265
        if (inc <= 0) {                                                        // :266
            return false;
        }
        if (showAnim) {                                                        // :268
            PendingApi.pbCommonAnimation(battle, "StatDown", this);
        }
        String[] texts;
        if (user != null && user.index == index) {                             // :269
            texts = new String[] {                                             // :270-273
                pbThis() + "的" + cause + "降低了" + PBStats.getName(stat) + "！",
                pbThis() + "的" + cause + "大幅降低了" + PBStats.getName(stat) + "！",
                pbThis() + "的" + cause + "巨幅降低了" + PBStats.getName(stat) + "！"
            };
        } else {
            String userName = user == null ? "" : user.pbThis();
            texts = new String[] {                                             // :275-278
                userName + "的" + cause + "降低了" + pbThis(true) + "的" + PBStats.getName(stat) + "！",
                userName + "的" + cause + "大幅降低了" + pbThis(true) + "的" + PBStats.getName(stat) + "！",
                userName + "的" + cause + "巨幅降低了" + pbThis(true) + "的" + PBStats.getName(stat) + "！"
            };
        }
        display(texts[Math.min(inc - 1, 2)]);                                  // :280
        if (abilityActive()) {                                                 // :282
            BattleHandlers.triggerAbilityOnStatLoss(ability, this, stat, user); // :283
        }
        effects.set(PBEffects.Battler.LashOut, true);                          // :285
        return true;                                                           // :286
    }

    /** {@code pbLowerStatStageByAbility} (Battler_StatStages:289-306)。 */
    public boolean pbLowerStatStageByAbility(int stat, int increment, Battler user,
                                             boolean splashAnim, boolean checkContact) {
        if (hasActiveAbility(new String[] {"WATCHDOGEYE", "GUARDDOG"})
                && user != null && "INTIMIDATE".equals(user.ability)) {         // :290-291
            return pbRaiseStatStageByAbility(stat, increment, this, true);      // :292
        }
        boolean ret = false;                                                    // :294
        if (splashAnim) {                                                       // :295
            PendingApi.pbShowAbilitySplash(battle, user);
        }
        if (pbCanLowerStatStage(stat, user, null, PokeBattle_SceneConstants.USE_ABILITY_SPLASH, false)
                && (!checkContact || affectedByContactEffect(PokeBattle_SceneConstants.USE_ABILITY_SPLASH))) { // :296-297
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {                  // :298
                ret = pbLowerStatStage(stat, increment, user);                   // :299
            } else {
                ret = pbLowerStatStageByCause(stat, increment, user,
                        user == null ? "" : user.abilityName());                 // :301
            }
        }
        if (splashAnim) {                                                        // :304
            PendingApi.pbHideAbilitySplash(battle, user);
        }
        return ret;                                                              // :305
    }

    /** {@code hasAlteredStatStages?} (Battler_StatStages:378-381)。 */
    public boolean hasAlteredStatStages() {
        for (int s : PBStats.EACH_BATTLE_STAT) {                                 // :379
            if (stage(s) != 0) {
                return true;
            }
        }
        return false;                                                            // :380
    }

    /** {@code pbResetStatStages} (Battler_StatStages:393-403)。 */
    public void pbResetStatStages() {
        for (int s : PBStats.EACH_BATTLE_STAT) {                                 // :394
            if (stage(s) > 0) {                                                  // :395
                statsLoweredThisRound = true;                                    // :396
                statsDropped = true;                                             // :397
            } else if (stage(s) < 0) {                                           // :398
                statsRaisedThisRound = true;                                     // :399
            }
            setStage(s, 0);                                                      // :401
        }
    }

    /** {@code plainStats} (PokeBattle_Battler:297-305)：攻/防/特攻/特防/速。 */
    public int[] plainStats() {
        int[] ret = new int[8];                                                  // :298
        ret[PBStats.ATTACK] = attack();                                          // :299
        ret[PBStats.DEFENSE] = defense();                                        // :300
        ret[PBStats.SPATK] = spAtk();                                            // :301
        ret[PBStats.SPDEF] = spDef();                                            // :302
        ret[PBStats.SPEED] = speed();                                            // :303
        return ret;                                                              // :304
    }

    /**
     * {@code addSideStatUps} (PokeBattle_Battler:892-896)。
     *
     * <p>登记: {@code @battle.sideStatUps[self.idxOwnSide]} 未建模（{@code Battle} 未暴露
     * {@code sideStatUps}，interface-freeze §4 归 infra2）→ 空实现。它只喂
     * Opportunist（跟风）与模仿香草，不影响能力等级本身。</p>
     */
    private void addSideStatUps(int stat, int increment) {
        // 登记: PokeBattle_Battler:892-896 @battle.sideStatUps（Battle 未暴露）
    }

    /** {@code pbMirrorStatUpsOpposing} (Battler_AbilityAndItem:344-356)。登记同 {@link #addSideStatUps}。 */
    public void pbMirrorStatUpsOpposing() {
        // 登记: Battler_AbilityAndItem:344-356 依赖 @battle.sideStatUps /
        //       @battle.allOtherSideBattlers / triggerAbilityOnOpposingStatGain（本批未建模）
    }

    // =========================================================================
    // 属性、天气、浮空
    // =========================================================================

    /**
     * {@code pbTypes} (PokeBattle_Battler:314-348)。
     *
     * <p>Ruby 读 {@code @type1}/{@code @type2} 两个 battler 字段；本运行时以
     * {@code Pokemon.types()} 为准（{@code Pokemon.types()} 已合并形态属性），
     * 其余 7 个去属性/加属性效果逐行照转。{@code @effects[Type3]} 在 Ruby 里存类型编号，
     * 本运行时存类型<b>内部名</b>（{@code -1} 仍是整数哨兵，见 {@link #initEffects(boolean)} :310）。</p>
     */
    public Array<String> pbTypes() {
        return pbTypes(false);
    }

    /** {@code pbTypes(withType3=false)} (PokeBattle_Battler:314-348)。 */
    public Array<String> pbTypes(boolean withType3) {
        Array<String> ret = new Array<>();                                       // :315
        Array<String> base = pokemon == null ? new Array<String>() : pokemon.types();
        if (type1 != null) {                                                     // @type1/@type2 changed by pbChangeTypes
            base = new Array<>();
            base.add(type1);
            if (type2 != null) base.add(type2);
        }
        if (base.size > 0) {
            ret.add(base.get(0));                                                // :315 ret = [@type1]
        }
        if (base.size > 1 && !base.get(1).equals(base.get(0))) {
            ret.add(base.get(1));                                                // :316
        }
        if (effects.truthy(PBEffects.Battler.LoseGrassType)) {                    // :318 势如破竹
            removeType(ret, "GRASS");
        }
        if (effects.truthy(PBEffects.Battler.LoseFireType)) {                     // :322 爆焰突进
            removeType(ret, "FIRE");
        }
        if (effects.truthy(PBEffects.Battler.LoseWaterType)) {                    // :326 追本溯源
            removeType(ret, "WATER");
        }
        if (effects.truthy(PBEffects.Battler.DoubleShock)) {                      // :330 电光双击
            removeType(ret, "ELECTRIC");
        }
        if (effects.truthy(PBEffects.Battler.BurnUp)) {                           // :334
            removeType(ret, "FIRE");
        }
        if (effects.truthy(PBEffects.Battler.Roost)) {                            // :339
            removeType(ret, "FLYING");
            if (ret.size == 0) {                                                  // :341
                ret.add("NORMAL");                                               // getConst(PBTypes,:NORMAL) || 0
            }
        }
        if (withType3) {                                                          // :344
            String type3 = effects.stringVal(PBEffects.Battler.Type3);
            if (type3 != null && !ret.contains(type3, false)) {                   // :345
                ret.add(type3);
            }
        }
        return ret;                                                               // :347
    }

    private static void removeType(Array<String> types, String type) {
        for (int i = types.size - 1; i >= 0; i--) {
            if (type.equals(types.get(i))) {
                types.removeIndex(i);
            }
        }
    }

    /** {@code pbHasType?} (PokeBattle_Battler:350-355)，12 次。 */
    public boolean pbHasType(String type) {
        if (type == null || type.isEmpty()) {                                     // :352 return false if !type || type<0
            return false;
        }
        Array<String> activeTypes = pbTypes(true);                                // :353
        return activeTypes.contains(type, false);                                 // :354
    }

    /** {@code pbHasOtherType?} (PokeBattle_Battler:357-363)。 */
    public boolean pbHasOtherType(String type) {
        if (type == null || type.isEmpty()) {                                     // :359
            return false;
        }
        Array<String> activeTypes = pbTypes(true);                                // :360
        removeType(activeTypes, type);                                            // :361 reject! { t==type }
        return activeTypes.size > 0;                                              // :362
    }

    /** {@code airborne?} (PokeBattle_Battler:591-602)。 */
    public boolean airborne() {
        if (hasActiveItem("IRONBALL")) {                                          // :592
            return false;
        }
        if (effects.truthy(PBEffects.Battler.Ingrain)) {                          // :593
            return false;
        }
        if (effects.truthy(PBEffects.Battler.SmackDown)) {                        // :594
            return false;
        }
        if (fieldInt(PBEffects.Field.Gravity) > 0) {                              // :595
            return false;
        }
        if (pbHasType("FLYING")) {                                                // :596
            return true;
        }
        if (hasActiveAbility("LEVITATE") && !moldBreaker()) {                     // :597
            return true;
        }
        if (hasActiveItem("AIRBALLOON")) {                                        // :598
            return true;
        }
        if (effects.intVal(PBEffects.Battler.MagnetRise) > 0) {                   // :599
            return true;
        }
        return effects.intVal(PBEffects.Battler.Telekinesis) > 0;                 // :600
    }

    /** {@code affectedByTerrain?} (PokeBattle_Battler:604-608)。 */
    public boolean affectedByTerrain() {
        if (airborne()) {                                                         // :605
            return false;
        }
        if (semiInvulnerable()) {                                                 // :606
            return false;
        }
        return true;                                                              // :607
    }

    /**
     * {@code semiInvulnerable?} (PokeBattle_Battler:725-727) =
     * {@code inTwoTurnAttack?("0C9","0CA","0CB","0CC","0CD","0CE","14D")}。
     *
     * <p>Stage 5 / 2c: {@link #inTwoTurnAttack(String...)} now maps the stored move id to its
     * function code through {@code PbsData.moveById}; nothing writes {@code TwoTurnAttack}
     * yet (that is {@code pbUseMove}, :240), so this is still false in practice.</p>
     */
    public boolean semiInvulnerable() {
        return inTwoTurnAttack("0C9", "0CA", "0CB", "0CC", "0CD", "0CE", "14D");   // :726
    }

    /** {@code effectiveWeather} (PokeBattle_Battler:856-861)。 */
    public int effectiveWeather() {
        int ret = PendingApi.fieldWeather(battle);                                // :857 @battle.pbWeather
        boolean umbrellaWeather = ret == PBWeather.Sun || ret == PBWeather.Rain
                || ret == PBWeather.HarshSun || ret == PBWeather.HeavyRain;       // :858
        if (umbrellaWeather && hasActiveItem("UTILITYUMBRELLA")) {                 // :859
            ret = PBWeather.None;
        }
        return ret;                                                               // :860
    }

    // =========================================================================
    // 静态表（逐条照 PokeBattle_Battler 的 abilityBlacklist）
    // =========================================================================

    /** {@code unstoppableAbility?} 的黑名单（PokeBattle_Battler:405-437，被注释掉的 FLOWERGIFT/FORECAST 不算）。 */
    private static final String[] UNSTOPPABLE_ABILITIES = {
        "BATTLEBOND", "DISGUISE", "FLAMEVEIL", "MULTITYPE", "POWERCONSTRUCT", "SCHOOLING",
        "SHIELDSDOWN", "STANCECHANGE", "ZENMODE", "ICEFACE", "COMATOSE", "RKSSYSTEM",
        "ASONEICE", "ASONEGHOST", "NEUTRALIZINGGAS", "WONDERGUARD", "HUNGERSWITCH",
        "RIGHTTOFIGHT", "COMMANDER", "QUARKDRIVE", "PROTOSYNTHESIS", "ZEROTOHERO",
        "TERASHIFT", "TERASHELL", "TERAFORMZERO", "POISONPUPPETEER"
    };

    /** {@code ungainableAbility?} 的黑名单（PokeBattle_Battler:447-483）。 */
    private static final String[] UNGAINABLE_ABILITIES = {
        "BATTLEBOND", "DISGUISE", "FLAMEVEIL", "FLOWERGIFT", "FORECAST", "MULTITYPE",
        "POWERCONSTRUCT", "SCHOOLING", "SHIELDSDOWN", "STANCECHANGE", "ZENMODE", "ILLUSION",
        "IMPOSTER", "COMATOSE", "RKSSYSTEM", "ASONEICE", "ASONEGHOST", "NEUTRALIZINGGAS",
        "WONDERGUARD", "HUNGERSWITCH", "RIGHTTOFIGHT", "COMMANDER", "QUARKDRIVE", "TERASHIFT",
        "TERASHELL", "TERAFORMZERO", "PROTOSYNTHESIS", "SACREDREBORN", "ABYSSREBORN",
        "STORMEYE", "RAINBOWARCH"
    };

    /** {@code uncopyableAbility?} 追加的黑名单（PokeBattle_Battler:494-498）。 */
    private static final String[] UNCOPYABLE_ABILITIES = {"POWEROFALCHEMY", "RECEIVER", "TRACE"};

    // =========================================================================
    // Stage 4 / L1' —— **纯追加**：Ruby 默认参数的「中间档」重载
    //
    // 背景：Ruby 的 `def m(a,b=x,c=y)` 允许任意实参个数，L1' 的 handler 体是 1:1
    // 照抄调用形状的，而本类原先只提供了首档与末档 → 中间档全部「找不到合适的方法」。
    // 这些重载**只做转发**，每个默认值都取自插件原文（行号见各处 javadoc），
    // 没有一个默认值是凭直觉填的。本行以上的代码一行未改。
    //
    // 已存在的重载（无需再补）：`hasActiveAbility(String[])` / `hasActiveItem(String[])`
    // 单参形式在 M0（task-12）就已落地（PokeBattle_Battler:387 / :519 的
    // `ignoreFainted=false` 档），本次只补下面这些。
    // =========================================================================

    /**
     * {@code pbCanRaiseStatStage?(stat,user,move)} —— Ruby 默认参数档（3 个实参）。
     *
     * <p>原文 {@code Battler_StatStages:9}
     * {@code def pbCanRaiseStatStage?(stat,user=nil,move=nil,showFailMsg=false,ignoreContrary=false)}
     * → 本档补 {@code showFailMsg=false}、{@code ignoreContrary=false}，
     * 转发到 5 档 {@link #pbCanRaiseStatStage(int, Battler, BattleMove, boolean, boolean)}。</p>
     */
    public boolean pbCanRaiseStatStage(int stat, Battler user, BattleMove move) {
        return pbCanRaiseStatStage(stat, user, move, false, false);
    }

    /**
     * {@code pbCanLowerStatStage?(stat,user,move)} —— Ruby 默认参数档（3 个实参）。
     *
     * <p>原文 {@code Battler_StatStages:130}
     * {@code def pbCanLowerStatStage?(stat,user=nil,move=nil,showFailMsg=false,ignoreContrary=false)}
     * → 本档补 {@code showFailMsg=false}、{@code ignoreContrary=false}，
     * 转发到 5 档 {@link #pbCanLowerStatStage(int, Battler, BattleMove, boolean, boolean)}。</p>
     */
    public boolean pbCanLowerStatStage(int stat, Battler user, BattleMove move) {
        return pbCanLowerStatStage(stat, user, move, false, false);
    }

    /**
     * {@code pbCanLowerStatStage?(stat,user,move,showFailMsg)} —— Ruby 默认参数档（4 个实参）。
     *
     * <p>原文 {@code Battler_StatStages:130} → 本档补 {@code ignoreContrary=false}，
     * 转发到 5 档 {@link #pbCanLowerStatStage(int, Battler, BattleMove, boolean, boolean)}。</p>
     */
    public boolean pbCanLowerStatStage(int stat, Battler user, BattleMove move, boolean showFailMsg) {
        return pbCanLowerStatStage(stat, user, move, showFailMsg, false);
    }

    /**
     * {@code pbRaiseStatStage(stat,increment,user,showAnim)} —— Ruby 默认参数档（4 个实参）。
     *
     * <p>原文 {@code Battler_StatStages:47}
     * {@code def pbRaiseStatStage(stat,increment,user,showAnim=true,ignoreContrary=false)}
     * → 本档补 {@code ignoreContrary=false}，
     * 转发到 5 档 {@link #pbRaiseStatStage(int, int, Battler, boolean, boolean)}。</p>
     */
    public boolean pbRaiseStatStage(int stat, int increment, Battler user, boolean showAnim) {
        return pbRaiseStatStage(stat, increment, user, showAnim, false);
    }

    /**
     * {@code pbRaiseStatStageByCause(stat,increment,user,cause,showAnim)} —— Ruby 默认参数档（5 个实参）。
     *
     * <p>原文 {@code Battler_StatStages:74}
     * {@code def pbRaiseStatStageByCause(stat,increment,user,cause,showAnim=true,ignoreContrary=false)}
     * → 本档补 {@code ignoreContrary=false}，
     * 转发到 6 档 {@link #pbRaiseStatStageByCause(int, int, Battler, String, boolean, boolean)}。</p>
     */
    public boolean pbRaiseStatStageByCause(int stat, int increment, Battler user, String cause,
                                           boolean showAnim) {
        return pbRaiseStatStageByCause(stat, increment, user, cause, showAnim, false);
    }

    /**
     * {@code pbLowerStatStage(stat,increment,user,showAnim)} —— Ruby 默认参数档（4 个实参）。
     *
     * <p>原文 {@code Battler_StatStages:191}
     * {@code def pbLowerStatStage(stat,increment,user,showAnim=true,ignoreContrary=false,
     * ignoreMirrorArmor=false)} → 本档补 {@code ignoreContrary=false}、{@code ignoreMirrorArmor=false}，
     * 转发到 6 档 {@link #pbLowerStatStage(int, int, Battler, boolean, boolean, boolean)}。</p>
     */
    public boolean pbLowerStatStage(int stat, int increment, Battler user, boolean showAnim) {
        return pbLowerStatStage(stat, increment, user, showAnim, false, false);
    }

    /**
     * {@code pbLowerStatStage(stat,increment,user,showAnim,ignoreContrary)} —— Ruby 默认参数档（5 个实参）。
     *
     * <p>原文 {@code Battler_StatStages:191} → 本档补 {@code ignoreMirrorArmor=false}，
     * 转发到 6 档 {@link #pbLowerStatStage(int, int, Battler, boolean, boolean, boolean)}。</p>
     */
    public boolean pbLowerStatStage(int stat, int increment, Battler user, boolean showAnim,
                                    boolean ignoreContrary) {
        return pbLowerStatStage(stat, increment, user, showAnim, ignoreContrary, false);
    }

    /**
     * {@code pbLowerStatStageByCause(stat,increment,user,cause,showAnim)} —— Ruby 默认参数档（5 个实参）。
     *
     * <p>原文 {@code Battler_StatStages:237}
     * {@code def pbLowerStatStageByCause(stat,increment,user,cause,showAnim=true,
     * ignoreContrary=false, ignoreMirrorArmor=false)} → 本档补 {@code ignoreContrary=false}、
     * {@code ignoreMirrorArmor=false}，
     * 转发到 7 档 {@link #pbLowerStatStageByCause(int, int, Battler, String, boolean, boolean, boolean)}。</p>
     */
    public boolean pbLowerStatStageByCause(int stat, int increment, Battler user, String cause,
                                           boolean showAnim) {
        return pbLowerStatStageByCause(stat, increment, user, cause, showAnim, false, false);
    }

    /**
     * {@code pbLowerStatStageByCause(stat,increment,user,cause,showAnim,ignoreContrary)} —— Ruby 默认参数档（6 个实参）。
     *
     * <p>原文 {@code Battler_StatStages:237} → 本档补 {@code ignoreMirrorArmor=false}，
     * 转发到 7 档 {@link #pbLowerStatStageByCause(int, int, Battler, String, boolean, boolean, boolean)}。</p>
     */
    public boolean pbLowerStatStageByCause(int stat, int increment, Battler user, String cause,
                                           boolean showAnim, boolean ignoreContrary) {
        return pbLowerStatStageByCause(stat, increment, user, cause, showAnim, ignoreContrary, false);
    }

    /**
     * {@code pbCanPoison?(user,showMessages)} —— Ruby 默认参数档（2 个实参）。
     *
     * <p>原文 {@code Battler_Statuses:375} {@code def pbCanPoison?(user,showMessages,move=nil)}
     * —— 注意 {@code user} 与 {@code showMessages} <b>没有默认值</b>，只有 {@code move} 有
     * → 本档补 {@code move=null}，转发到 3 档 {@link #pbCanPoison(Battler, boolean, BattleMove)}。</p>
     */
    public boolean pbCanPoison(Battler user, boolean showMessages) {
        return pbCanPoison(user, showMessages, null);
    }

    /**
     * {@code pbCanBurn?(user,showMessages)} —— Ruby 默认参数档（2 个实参）。
     *
     * <p>原文 {@code Battler_Statuses:394} {@code def pbCanBurn?(user,showMessages,move=nil)}
     * → 本档补 {@code move=null}，转发到 3 档 {@link #pbCanBurn(Battler, boolean, BattleMove)}。</p>
     */
    public boolean pbCanBurn(Battler user, boolean showMessages) {
        return pbCanBurn(user, showMessages, null);
    }

    /**
     * {@code pbCanParalyze?(user,showMessages)} —— Ruby 默认参数档（2 个实参）。
     *
     * <p>原文 {@code Battler_Statuses:413} {@code def pbCanParalyze?(user,showMessages,move=nil)}
     * → 本档补 {@code move=null}，转发到 3 档 {@link #pbCanParalyze(Battler, boolean, BattleMove)}。</p>
     */
    public boolean pbCanParalyze(Battler user, boolean showMessages) {
        return pbCanParalyze(user, showMessages, null);
    }

    /**
     * {@code pbCanSleep?(user,showMessages)} —— Ruby 默认参数档（2 个实参）。
     *
     * <p>原文 {@code Battler_Statuses:318}
     * {@code def pbCanSleep?(user,showMessages,move=nil,ignoreStatus=false)}
     * → 本档补 {@code move=null}、{@code ignoreStatus=false}，
     * 转发到 4 档 {@link #pbCanSleep(Battler, boolean, BattleMove, boolean)}。</p>
     */
    public boolean pbCanSleep(Battler user, boolean showMessages) {
        return pbCanSleep(user, showMessages, null, false);
    }

    /**
     * {@code pbCanSleep?(user,showMessages,move)} —— Ruby 默认参数档（3 个实参）。
     *
     * <p>原文 {@code Battler_Statuses:318} → 本档补 {@code ignoreStatus=false}，
     * 转发到 4 档 {@link #pbCanSleep(Battler, boolean, BattleMove, boolean)}。</p>
     */
    public boolean pbCanSleep(Battler user, boolean showMessages, BattleMove move) {
        return pbCanSleep(user, showMessages, move, false);
    }

    /**
     * {@code pbCanConfuse?(user)} —— Ruby 默认参数档（1 个实参）。
     *
     * <p>原文 {@code Battler_Statuses:488}
     * {@code def pbCanConfuse?(user=nil,showMessages=true,move=nil,selfInflicted=false)}
     * → 本档补 {@code showMessages=true}、{@code move=null}、{@code selfInflicted=false}，
     * 转发到 4 档 {@link #pbCanConfuse(Battler, boolean, BattleMove, boolean)}。</p>
     */
    public boolean pbCanConfuse(Battler user) {
        return pbCanConfuse(user, true, null, false);
    }

    /**
     * {@code pbCanConfuse?(user,showMessages)} —— Ruby 默认参数档（2 个实参）。
     *
     * <p>原文 {@code Battler_Statuses:488} → 本档补 {@code move=null}、{@code selfInflicted=false}，
     * 转发到 4 档 {@link #pbCanConfuse(Battler, boolean, BattleMove, boolean)}。</p>
     */
    public boolean pbCanConfuse(Battler user, boolean showMessages) {
        return pbCanConfuse(user, showMessages, null, false);
    }

    /**
     * {@code pbCanConfuse?(user,showMessages,move)} —— Ruby 默认参数档（3 个实参）。
     *
     * <p>原文 {@code Battler_Statuses:488} → 本档补 {@code selfInflicted=false}，
     * 转发到 4 档 {@link #pbCanConfuse(Battler, boolean, BattleMove, boolean)}。</p>
     */
    public boolean pbCanConfuse(Battler user, boolean showMessages, BattleMove move) {
        return pbCanConfuse(user, showMessages, move, false);
    }

    /**
     * {@code pbCanConfuse?} —— Ruby **全默认档**（0 个实参）。
     *
     * <p>原文 {@code Battler_Statuses:488}
     * {@code def pbCanConfuse?(user=nil,showMessages=true,move=nil,selfInflicted=false)}
     * —— 四个参数**全部有默认值**，所以 Ruby 里可以无参调用。本档把四个默认值全部补齐，
     * 转发到 4 档 {@link #pbCanConfuse(Battler, boolean, BattleMove, boolean)}。
     * 这样 {@code pbCanConfuse} 的 arity 表就是 0..4 的完整五档，后续作者不必再数参数。</p>
     */
    public boolean pbCanConfuse() {
        return pbCanConfuse(null, true, null, false);
    }

    // =========================================================================
    // Stage 4 / L2 —— **纯追加**：L2 招式效果层需要的三个桥
    //
    // 背景：Ruby 的 `forcedItem` / `move.pp` 在插件里是「招式/道具对象上的字段」，
    // 而本运行时把道具身份改成内部名 String、把 PP 挂在 Pokemon.MoveSlot 上，
    // 所以这三个必须加一个 String / slot 形状的入口。**它们不是新行为，是同一段
    // Ruby 的另一种承载**，行号见各处 javadoc。本行以上的代码一行未改。
    // =========================================================================

    /**
     * {@code pbHeldItemTriggerCheck(forcedItem,fling)} (Battler_AbilityAndItem:235-249) 的
     * **内部名档**：Ruby 的 {@code forcedItem} 是道具 id，本运行时道具身份是内部名，
     * 而 {@code BattleHandlers_Abilities/Items} 里大量调用是
     * {@code battler.pbHeldItemTriggerCheck(battler.item,false)}
     * （例如 {@code Move_Effects_180-1FF.rb:60/94} 的 Stuff Cheeks / Tea Time），
     * 传的正是道具本身 → 需要这一档才能照抄调用形状。
     */
    public void pbHeldItemTriggerCheck(String forcedItemName, boolean fling) {
        if (fainted()) {                                                 // :236
            return;
        }
        boolean forced = forcedItemName != null && !forcedItemName.isEmpty();
        if (!forced && !itemActive()) {                                  // :237 forcedItem==0 && !itemActive?
            return;
        }
        pbItemHPHealCheck(forcedItemName, fling);                        // :238
        pbItemStatusCureCheck(forcedItemName, fling);                    // :239
        pbItemEndOfMoveCheck(forcedItemName, fling);                     // :240
        if (forced) {                                                    // :243
            if (BattleHandlers.triggerTargetItemOnHitPositiveBerry(forcedItemName, this, battle, true)) { // :245
                pbHeldItemTriggered(forcedItemName, 1, fling);           // :246
            }
        }
    }

    /** {@code pbItemHPHealCheck(forcedItem,fling)} (Battler_AbilityAndItem:253-261) 的内部名档。 */
    public void pbItemHPHealCheck(String forcedItemName, boolean fling) {
        boolean forced = forcedItemName != null && !forcedItemName.isEmpty();
        if (!forced && !itemActive()) {                                  // :254
            return;
        }
        String itm = forced ? forcedItemName : item;                     // :255
        if (BattleHandlers.triggerHPHealItem(itm, this, battle, forced)) { // :256
            pbHeldItemTriggered(itm, forced ? 1 : 0, fling);             // :257
        } else if (!forced) {                                            // :258
            pbItemTerrainStatBoostCheck();                               // :259
        }
    }

    /** {@code pbItemStatusCureCheck(forcedItem,fling)} (Battler_AbilityAndItem:267-274) 的内部名档。 */
    public void pbItemStatusCureCheck(String forcedItemName, boolean fling) {
        if (fainted()) {                                                 // :268
            return;
        }
        boolean forced = forcedItemName != null && !forcedItemName.isEmpty();
        if (!forced && !itemActive()) {                                  // :269
            return;
        }
        String itm = forced ? forcedItemName : item;                     // :270
        if (BattleHandlers.triggerStatusCureItem(itm, this, battle, forced)) { // :271
            pbHeldItemTriggered(itm, forced ? 1 : 0, fling);             // :272
        }
    }

    /** {@code pbItemEndOfMoveCheck(forcedItem,fling)} (Battler_AbilityAndItem:279-288) 的内部名档。 */
    public void pbItemEndOfMoveCheck(String forcedItemName, boolean fling) {
        if (fainted()) {                                                 // :280
            return;
        }
        boolean forced = forcedItemName != null && !forcedItemName.isEmpty();
        if (!forced && !itemActive()) {                                  // :281
            return;
        }
        String itm = forced ? forcedItemName : item;                     // :282
        if (BattleHandlers.triggerEndOfMoveItem(itm, this, battle, forced)) { // :283
            pbHeldItemTriggered(itm, forced ? 1 : 0, fling);             // :284
        } else if (BattleHandlers.triggerEndOfMoveStatRestoreItem(itm, this, battle, forced)) { // :285
            pbHeldItemTriggered(itm, forced ? 1 : 0, fling);             // :286
        }
    }

    /**
     * {@code pbSetPP(move,pp)} (Battler_ChangeSelf:132-139) 的**本运行时桥形**。
     *
     * <p>Ruby 写的是招式对象自己的 {@code move.pp}，并且当
     * {@code move.realMove && move.id==move.realMove.id && !@effects[Transform]} 时
     * 也写 {@code move.realMove.pp}（:136-138）。本运行时的 {@link BattleMove} 没有
     * {@code pp} 字段 —— PP 唯一存放处是 {@code Pokemon.MoveSlot.pp}
     * （即 Ruby 的 {@code move.realMove.pp}），{@link #moveSlotPp(int)} 读的就是它。
     * 所以这里按**招式内部名**定位槽位并写回，两个分支在本运行时是同一处。</p>
     *
     * @param internalName 招式的内部名（本运行时的招式身份）
     * @param pp           新的 PP
     * @return 是否找到了该招式槽位
     */
    public boolean pbSetPP(String internalName, int pp) {
        if (pokemon == null || internalName == null) {                   // :133 move.pp = pp
            return false;
        }
        for (int i = 0; i < pokemon.moves.size; i++) {
            Pokemon.MoveSlot slot = pokemon.moves.get(i);
            if (slot != null && slot.move != null
                    && internalName.equals(slot.move.internalName)) {    // :136 move.id==move.realMove.id
                slot.pp = pp;                                            // :133/:137
                return true;
            }
        }
        return false;
    }

    // ==================================================================
    // Stage 5 / 2a: turn bookkeeping (Battler_UseMove:71-128) and its helpers
    // ==================================================================

    /**
     * {@code @currentMove} (Battler_UseMove:104/178/241, Battler_Statuses:307):
     * the PBS move id of the multi-turn attack in progress. {@code 0} = none
     * (Ruby leaves it {@code nil} until first written; {@code usingMultiTurnAttack?}
     * guards every read, so {@code 0} is equivalent).
     */
    public int currentMove;

    /** {@code @criticalHits} (PokeBattle_Battler:119-124; the Pokemon-side mirror {@code @pokemon.criticalHits=} has no field in this runtime's Pokemon). */
    public int criticalHits;

    /** {@code hp=(value)} (PokeBattle_Battler:92-95): writes the battler and its Pokemon. */
    public void setHp(int value) {
        hp = value;                                                              // :93
        if (pokemon != null) pokemon.hp = value;                                 // :94
    }

    /**
     * {@code @type1} / {@code @type2} when a move or ability changed them
     * (Battler_ChangeSelf:285-314 {@code pbChangeTypes}); {@code null} = "the
     * Pokemon's own types" (Battler_Initialize:47-48/81-82). Cleared on switch-in.
     * 登记: {@code pbUpdate}'s type reset (Battler_Initialize:378-379) is not modelled.
     */
    public String type1, type2;

    /** {@code pbChangeTypes(newType)} with a single type name (Battler_ChangeSelf:306-311). */
    public void pbChangeTypes(String newType) {
        type1 = newType;                                                         // :308
        type2 = newType;                                                         // :309
        effects.set(PBEffects.Battler.Type3, -1);                                // :310
        effects.set(PBEffects.Battler.BurnUp, false);                            // :312
        effects.set(PBEffects.Battler.Roost, false);                             // :313
    }

    /** {@code pbChangeTypes(newType)} with a type list (Battler_ChangeSelf:294-305). */
    public void pbChangeTypes(Array<String> newType) {
        Object newType3 = newType.size > 2 ? newType.get(2) : (Object) (-1);     // :302 newType[2] || -1
        type1 = newType.get(0);                                                  // :303
        type2 = newType.size > 1 ? newType.get(1) : newType.get(0);              // :304
        effects.set(PBEffects.Battler.Type3, newType3);                          // :305
        effects.set(PBEffects.Battler.BurnUp, false);                            // :312
        effects.set(PBEffects.Battler.Roost, false);                             // :313
    }

    /** {@code pbChangeTypes(battler)}: copy another battler's types (Battler_ChangeSelf:286-293). */
    public void pbChangeTypes(Battler newType) {
        Array<String> newTypes = newType.pbTypes();                              // :287
        if (newTypes.size == 0) newTypes.add("NORMAL");                          // :288
        Object newType3 = newType.effects.stringVal(PBEffects.Battler.Type3);    // :289
        if (newType3 == null) newType3 = -1;
        if (newType3 instanceof String && newTypes.contains((String) newType3, false)) newType3 = -1;   // :290
        type1 = newTypes.get(0);                                                 // :291
        type2 = newTypes.size == 1 ? newTypes.get(0) : newTypes.get(1);          // :292
        effects.set(PBEffects.Battler.Type3, newType3);                          // :293
        effects.set(PBEffects.Battler.BurnUp, false);                            // :312
        effects.set(PBEffects.Battler.Roost, false);                             // :313
    }

    /** {@code usingMultiTurnAttack?} (PokeBattle_Battler:708-716). */
    public boolean usingMultiTurnAttack() {
        if (effects.intVal(PBEffects.Battler.TwoTurnAttack) > 0) return true;   // :709
        if (effects.intVal(PBEffects.Battler.HyperBeam) > 0) return true;       // :710
        if (effects.intVal(PBEffects.Battler.Rollout) > 0) return true;         // :711
        if (effects.intVal(PBEffects.Battler.Outrage) > 0) return true;         // :712
        if (effects.intVal(PBEffects.Battler.Uproar) > 0) return true;          // :713
        if (effects.intVal(PBEffects.Battler.Bide) > 0) return true;            // :714
        return false;                                                            // :715
    }

    /**
     * {@code pbHasMove?(move_id)} (PokeBattle_Battler:551-556). {@code eachMove}
     * is this runtime's {@code pokemon.moves} (blank slots are not stored).
     */
    public boolean pbHasMove(int moveId) {
        if (moveId <= 0) return false;                                           // :553
        for (Pokemon.MoveSlot slot : pokemon.moves) {                            // :554 eachMove
            if (slot != null && slot.move != null && slot.move.id == moveId) return true;
        }
        return false;                                                            // :555
    }

    /** {@code getID(PBMoves,:X)} for the PBS id of a move's internal name; {@code -1} when absent (the {@code -1} sentinel of Ruby's {@code @lastMoveUsed}). */
    public int moveIdOf(String internalName) {
        pokemon.runtime.pokemon.PbsData pbsData = battle == null ? null : battle.pbs();
        pokemon.runtime.pokemon.PbsData.Move data =
                pbsData == null || internalName == null ? null : pbsData.move(internalName);
        return data == null ? -1 : data.id;
    }

    /** {@code pbEncoredMoveIndex} (PokeBattle_Battler:729-738). */
    public int pbEncoredMoveIndex() {
        if (effects.intVal(PBEffects.Battler.Encore) == 0
                || effects.intVal(PBEffects.Battler.EncoreMove) == 0) {          // :730
            return -1;
        }
        int ret = -1;                                                            // :731
        for (int i = 0; i < pokemon.moves.size; i++) {                           // :732 eachMoveWithIndex
            Pokemon.MoveSlot slot = pokemon.moves.get(i);
            if (slot == null || slot.move == null
                    || slot.move.id != effects.intVal(PBEffects.Battler.EncoreMove)) {   // :733
                continue;
            }
            ret = i;                                                             // :734
            break;                                                               // :735
        }
        return ret;                                                              // :737
    }

    /** {@code hasMoldBreaker?} (PokeBattle_Battler:573-575). */
    public boolean hasMoldBreaker() {
        return hasActiveAbility(new String[] {"MOLDBREAKER", "TERAVOLT", "TURBOBLAZE", "SHATTERFIST",
                "ETERNALFLAME", "ETRTNALIGHT", "ENDLESSDARKN"});                 // :574
    }

    /** {@code pbCanConfuseSelf?(showMessages)} (Battler_Statuses:527-529). */
    public boolean pbCanConfuseSelf(boolean showMessages) {
        return pbCanConfuse(null, showMessages, null, true);                     // :528
    }

    /** {@code pbContinualAbilityChecks(onSwitchIn=false)} (Battler_AbilityAndItem:82-117). */
    public void pbContinualAbilityChecks() {
        pbContinualAbilityChecks(false);
    }

    /** {@code pbContinualAbilityChecks(onSwitchIn)} (Battler_AbilityAndItem:82-117). */
    public void pbContinualAbilityChecks(boolean onSwitchIn) {
        battle.pbEndPrimordialWeather();                                         // :84
        if (hasActiveAbility("COMMANDER")) {                                     // :85
            BattleHandlers.triggerAbilityOnSwitchIn(ability, this, battle);      // :86
        }
        if (hasActiveAbility("TRACE")) {                                         // :89
            if (hasActiveItem("ABILITYSHIELD")) {                                // :90
                if (onSwitchIn) {                                                // :91
                    battle.showAbilitySplash(this);                              // :92
                    battle.display(pbThis() + "的特性\n被特性护具的效果保护了！");  // :93
                    battle.hideAbilitySplash(this);                              // :94
                }
            } else {
                Array<Battler> choices = new Array<>();                          // :97
                for (Battler b : battle.eachOtherSideBattler(index)) {           // :98
                    if (b.ungainableAbility(b.ability)
                            || "POWEROFALCHEMY".equals(b.ability)
                            || "RECEIVER".equals(b.ability)
                            || "TRACE".equals(b.ability)) {                      // :99-102
                        continue;
                    }
                    choices.add(b);                                              // :103
                }
                if (choices.size > 0) {                                          // :105
                    Battler choice = choices.get(battle.pbRandom(choices.size)); // :106
                    battle.showAbilitySplash(this);                              // :107
                    ability = choice.ability;                                    // :108
                    battle.display(pbThis() + "复制了" + choice.pbThis(true) + "的" + choice.abilityName() + "！");   // :109
                    battle.hideAbilitySplash(this);                              // :110
                    if (!onSwitchIn && (unstoppableAbility(ability) || abilityActive())) {   // :111
                        BattleHandlers.triggerAbilityOnSwitchIn(ability, this, battle);      // :112
                    }
                }
            }
        }
    }

    /**
     * {@code pbConfusionDamage(msg)} (Battler_UseMove:130-146): the pseudomove
     * {@code PokeBattle_Confusion} hits the confused battler. Critical hits are
     * off: {@code pbCritialOverride} is {@code -1} (Move_Effects_Generic:44;
     * Move_Usage_Calculations:216-219), so {@code pbIsCritical?} is always false.
     */
    public void pbConfusionDamage(String msg) {
        MoveEffect fx = MoveEffectRegistry.confusion();
        damageState.reset();                                                     // :131
        damageState.initialHP = hp;                                              // :132
        BattleMove confusionMove = MoveEffectRegistry.confusionMove();   // :133
        confusionMove.setCalcType(fx.pbCalcType(confusionMove, this));           // :134 -1
        damageState.typeMod = fx.pbCalcTypeMod(confusionMove, confusionMove.calcType(), this, this);   // :135 8
        Array<Battler> self = new Array<>();
        self.add(this);
        MoveUsage.pbCheckDamageAbsorption(fx, confusionMove, this, this);        // :136
        DamageCalc.compute(this, this, confusionMove, battle.pbs(), battle.random(), false, 1);   // :137
        MoveUsage.pbReduceDamage(fx, confusionMove, this, this);                 // :138
        setHp(hp - damageState.hpLost);                                          // :139
        MoveUsage.pbAnimateHitAndHPLost(this, self);                             // :140
        battle.display(msg);                                                     // :141 "It hurt itself in its confusion!"
        MoveUsage.pbRecordDamageLost(fx, confusionMove, this, this);             // :142
        MoveUsage.pbEndureKOMessage(this);                                       // :143
        if (fainted()) pbFaint();                                                // :144
        pbItemHPHealCheck(0, false);                                             // :145
    }

    // ==================================================================
    // Stage 5 / 2c: success checks before a move is used
    // (bodies in BattlerUseMoveChecks)
    // ==================================================================

    /** {@code pbCanChooseMove?(move,commandPhase,showMessages=true,specialUsage=false)} (Battler_UseMove_SuccessChecks:10-129). */
    public boolean pbCanChooseMove(BattleMove move, boolean commandPhase, boolean showMessages, boolean specialUsage) {
        return BattlerUseMoveChecks.pbCanChooseMove(this, move, commandPhase, showMessages, specialUsage);
    }

    /** {@code pbObedienceCheck?(choice)} (Battler_UseMove_SuccessChecks:136-189). */
    public boolean pbObedienceCheck(Object[] choice) {
        return BattlerUseMoveChecks.pbObedienceCheck(this, choice);
    }

    /** {@code pbDisobey(choice,badgeLevel)} (Battler_UseMove_SuccessChecks:192-239). */
    public boolean pbDisobey(Object[] choice, int badgeLevel) {
        return BattlerUseMoveChecks.pbDisobey(this, choice, badgeLevel);
    }

    /** {@code pbTryUseMove(choice,move,specialUsage,skipAccuracyCheck)} (Battler_UseMove_SuccessChecks:246-377). */
    public boolean pbTryUseMove(Object[] choice, BattleMove move, boolean specialUsage, boolean skipAccuracyCheck) {
        return BattlerUseMoveChecks.pbTryUseMove(this, choice, move, specialUsage, skipAccuracyCheck);
    }

    /** {@code pbSuccessCheckPerHit(move,user,target,skipAccuracyCheck)} (Battler_UseMove_SuccessChecks:604-647). */
    public boolean pbSuccessCheckPerHit(BattleMove move, Battler user, Battler target, boolean skipAccuracyCheck) {
        return BattlerUseMoveChecks.pbSuccessCheckPerHit(battle, move, user, target, skipAccuracyCheck);
    }

    /** {@code pbMissMessage(move,user,target)} (Battler_UseMove_SuccessChecks:652-661). */
    public void pbMissMessage(BattleMove move, Battler user, Battler target) {
        BattlerUseMoveChecks.pbMissMessage(battle, move, user, target);
    }

    /** {@code isCommander?} (PokeBattle_Battler:866-869): the effect holds a 1-element list. */
    public boolean isCommander() {
        Object commander = effects.raw(PBEffects.Battler.Commander);
        return commander instanceof java.util.List && ((java.util.List<?>) commander).size() == 1;
    }

    /**
     * {@code inHyperMode?} (Pokemon_ShadowPokemon:394-398): {@code p.hypermode},
     * which only Shadow Pokemon ever set (Pokemon_ShadowPokemon:259). 登记: Shadow
     * Pokemon are not modelled in this runtime, so no Pokemon is ever in
     * Hyper Mode.
     */
    public boolean inHyperMode() {
        return false;
    }

    /** {@code pbHyperModeObedience(move)} (Pokemon_ShadowPokemon:409-413). */
    public boolean pbHyperModeObedience(BattleMove move) {
        if (!inHyperMode()) return true;                                         // :410
        if (move == null || "SHADOW".equals(move.type())) return true;           // :411
        return battle.pbRandom(100) < 20;                                        // :412
    }

    /**
     * {@code @pokemon.foreign?(@battle.pbPlayer)} (PokeBattle_Pokemon:72-74).
     * Only the original trainer's name is modelled (see {@link BattlerUseMoveChecks}).
     */
    public boolean isForeign() {
        return pokemon.originalTrainer != null && !pokemon.originalTrainer.isEmpty()
                && !pokemon.originalTrainer.equals(battle.playerName);
    }

    /** {@code pbSleepSelf(msg=nil,duration=-1)} (Battler_Statuses:358-360; PLA_STATUS_MODE_DROWSY is 0, Arceus:15, so the original runs). */
    public void pbSleepSelf(String msg) {
        pbSleepSelf(msg, -1);
    }

    public void pbSleepSelf(String msg, int duration) {
        pbInflictStatus(PBStatuses.SLEEP, pbSleepDuration(duration), msg, null);   // :359
    }

    /** {@code pbContinueStatus} (Battler_Statuses:443-466). */
    public void pbContinueStatus() {
        pbContinueStatus(null);
    }

    /** {@code pbContinueStatus { block }} - {@code yield if block_given?} (:463) runs between the animation and the message. */
    public void pbContinueStatus(Runnable block) {
        String anim = "";                                                        // :444
        String msg = "";
        String st = status == null ? "" : status;
        switch (st) {                                                            // :445
            case "SLEEP":                                                        // :446
                anim = "Sleep";
                msg = pbThis() + "依旧在沉睡。";                                   // :447
                break;
            case "POISON":                                                       // :448
                anim = (statusCount > 0) ? "Toxic" : "Poison";                   // :449
                msg = pbThis() + "因为中毒受到了伤害！";                            // :450
                break;
            case "BURN":                                                         // :451
                anim = "Burn";
                msg = pbThis() + "因为灼伤受到了伤害！";                            // :452
                break;
            case "PARALYSIS":                                                    // :453
                anim = "Paralysis";
                msg = pbThis() + "麻痹了！\n无法行动！";                            // :454
                break;
            case "FROZEN":                                                       // :455
                anim = "Frozen";
                msg = pbThis() + "被结实的冰冻着！";                                // :456
                break;
            case "FROSTBITE":                                                    // :457
                anim = "Frostbitten";
                msg = pbThis() + "陷入了冻伤！";                                   // :458
                break;
            case "DROWSY":                                                       // :459
                anim = "Drowsy";
                msg = pbThis() + " is drowsy.";                                  // :460
                break;
            default:
                break;
        }
        if (!anim.isEmpty()) battle.commonAnimation(anim, this);                 // :462
        if (block != null) block.run();                                          // :463
        if (!msg.isEmpty()) battle.display(msg);                                 // :464
    }

    /**
     * {@code inTwoTurnAttack?(*functionCodes)} (PokeBattle_Battler:718-723):
     * the move stored in {@code @effects[TwoTurnAttack]} has one of the function codes.
     */
    public boolean inTwoTurnAttack(String... functionCodes) {
        int moveId = effects.intVal(PBEffects.Battler.TwoTurnAttack);
        if (moveId <= 0) return false;                                           // :719 return false if @effects[TwoTurnAttack]==0
        PbsData.Move data = battle.pbs().moveById(moveId);                       // :720 pbGetMoveData(@effects[TwoTurnAttack],MOVE_FUNCTION_CODE)
        if (data == null) return false;
        for (String code : functionCodes) {                                      // :721
            if (code.equals(data.function)) return true;
        }
        return false;                                                            // :722
    }

    // ==================================================================
    // Stage 5 / 2d: the master "use move" flow (bodies in BattlerUseMove /
    // BattlerTargeting)
    // ==================================================================

    /** {@code pbUseMove(choice,specialUsage=false)} (Battler_UseMove:170-622). */
    public void pbUseMove(Object[] choice, boolean specialUsage) {
        BattlerUseMove.pbUseMove(this, choice, specialUsage);
    }

    /** {@code pbUseMoveSimple(moveID,target=-1,idxMove=-1,specialUsage=true)} (Battler_UseMove:152-165); the move is its internal name. */
    public void pbUseMoveSimple(String moveName, int target, int idxMove, boolean specialUsage) {
        BattlerUseMove.pbUseMoveSimple(this, moveName, target, idxMove, specialUsage);
    }

    /** {@code pbUseMoveSimple(moveID,target)} with the default {@code idxMove=-1, specialUsage=true}. */
    public void pbUseMoveSimple(String moveName, int target) {
        pbUseMoveSimple(moveName, target, -1, true);
    }

    /** {@code pbProcessMoveHit(move,user,targets,hitNum,skipAccuracyCheck)} (Battler_UseMove:627-809). */
    public boolean pbProcessMoveHit(BattleMove move, Battler user, Array<Battler> targets, int hitNum,
                                    boolean skipAccuracyCheck) {
        return BattlerUseMove.pbProcessMoveHit(this, move, user, targets, hitNum, skipAccuracyCheck);
    }

    /** {@code pbFindUser(choice,move)} (Battler_UseMove_Targeting:5-7). */
    public Battler pbFindUser(Object[] choice, BattleMove move) {
        return BattlerTargeting.pbFindUser(this, choice, move);
    }

    /** {@code pbChangeUser(choice,move,user)} (Battler_UseMove_Targeting:9-30). */
    public Battler pbChangeUser(Object[] choice, BattleMove move, Battler user) {
        return BattlerTargeting.pbChangeUser(this, choice, move, user);
    }

    /** {@code pbFindTargets(choice,move,user)} (Battler_UseMove_Targeting:35-96). */
    public Array<Battler> pbFindTargets(Object[] choice, BattleMove move, Battler user) {
        return BattlerTargeting.pbFindTargets(this, choice, move, user);
    }

    /** {@code pbChangeTargets(move,user,targets,dragondarts=-1)} (Battler_UseMove_Targeting:101-217). */
    public Array<Battler> pbChangeTargets(BattleMove move, Battler user, Array<Battler> targets) {
        return BattlerTargeting.pbChangeTargets(this, move, user, targets);
    }

    /** Stand-in for {@code participants} (see {@link Battle#pbGainExp}): set once this battler's Exp was given out. */
    public boolean expAwarded;

    /** {@code pbHyperMode} (Pokemon_ShadowPokemon:400-407): returns at once for any non-Shadow Pokemon, which is all this runtime has. */
    public void pbHyperMode() {
        // :401 return if fainted? || !shadowPokemon? || inHyperMode?
    }

    /** {@code pbEffectsOnSwitchIn(switchIn=false)} (Battler_AbilityAndItem:5-35). */
    public void pbEffectsOnSwitchIn() {
        pbEffectsOnSwitchIn(false);
    }

    public void pbEffectsOnSwitchIn(boolean switchIn) {
        if (!switchIn) {                                                         // :6
            for (Battler b : battle.allBattlers()) {                             // :7
                b.droppedBelowHalfHP = false;                                    // :8
                b.statsDropped = false;                                          // :9
            }
        }
        // Healing Wish/Lunar Dance/entry hazards
        if (switchIn) battle.pbOnActiveOne(this);                                // :13
        // Primal Revert upon entering battle
        if (!fainted()) battle.pbPrimalReversion(index);                         // :15
        // Ending primordial weather, checking Trace
        pbContinualAbilityChecks(true);                                          // :17
        // Abilities that trigger upon switching in
        if ((!fainted() && unstoppableAbility(ability)) || abilityActive()) {    // :19
            BattleHandlers.triggerAbilityOnSwitchIn(ability, this, battle);      // :20
        }
        // Check for end of primordial weather
        battle.pbEndPrimordialWeather();                                         // :23
        // Items that trigger upon switching in (Air Balloon message)
        if (switchIn && itemActive()) {                                          // :25
            BattleHandlers.triggerItemOnSwitchIn(item, this, battle);            // :26
        }
        // Berry check, status-curing ability check
        if (switchIn) pbHeldItemTriggerCheck(0, false);                          // :29
        pbAbilityStatusCureCheck();                                              // :30
        for (Battler b : battle.allBattlers()) {                                 // :31
            b.droppedBelowHalfHP = false;                                        // :32
            b.statsDropped = false;                                              // :33
        }
    }

    /** {@code pbBeginTurn(_choice)} (Battler_UseMove:71-85). */
    public void pbBeginTurn(Object[] choice) {
        effects.set(PBEffects.Battler.BeakBlast, false);                         // :73
        effects.set(PBEffects.Battler.DestinyBondPrevious,
                effects.truthy(PBEffects.Battler.DestinyBond));                  // :74
        effects.set(PBEffects.Battler.DestinyBond, false);                       // :75
        effects.set(PBEffects.Battler.Grudge, false);                            // :76
        effects.set(PBEffects.Battler.MoveNext, false);                          // :77
        effects.set(PBEffects.Battler.Quash, 0);                                 // :78
        effects.set(PBEffects.Battler.ShellTrap, false);                         // :79
        if (effects.intVal(PBEffects.Battler.Encore) > 0 && pbEncoredMoveIndex() < 0) {   // :81
            effects.set(PBEffects.Battler.Encore, 0);                            // :82
            effects.set(PBEffects.Battler.EncoreMove, 0);                        // :83
        }
    }

    /**
     * {@code pbCancelMoves} (Battler_UseMove:92-109): called when a multi-turn
     * move is disrupted. Hyper Beam's effect is NOT cancelled (:90).
     */
    public void pbCancelMoves() {
        if (effects.intVal(PBEffects.Battler.Outrage) == 1 && pbCanConfuseSelf(false)) {   // :95
            pbConfuse(pbThis() + "因为过于疲劳而混乱了！");                        // :96
        }
        effects.set(PBEffects.Battler.TwoTurnAttack, 0);                         // :99
        effects.set(PBEffects.Battler.Rollout, 0);                               // :100
        effects.set(PBEffects.Battler.Outrage, 0);                               // :101
        effects.set(PBEffects.Battler.Uproar, 0);                                // :102
        effects.set(PBEffects.Battler.Bide, 0);                                  // :103
        currentMove = 0;                                                         // :104
        effects.set(PBEffects.Battler.FuryCutter, 0);                            // :106
        effects.set(PBEffects.Battler.BambooSword, 0);                           // :107
        effects.set(PBEffects.Battler.SuccessiveMove, -1);                       // :108
    }

    /** {@code pbEndTurn(_choice)} (Battler_UseMove:111-128). */
    public void pbEndTurn(Object[] choice) {
        lastRoundMoved = battle.turnCount();                                     // :112 Done something this round
        if (effects.intVal(PBEffects.Battler.GorillaTactics) < 0 && lastMoveUsed != null
                && hasActiveAbility("GORILLATACTICS")) {                         // :114
            effects.set(PBEffects.Battler.GorillaTactics, moveIdOf(lastMoveUsed));   // :115
        }
        if (effects.intVal(PBEffects.Battler.ChoiceBand) < 0
                && hasActiveItem(new String[] {"CHOICEBAND", "CHOICESPECS", "CHOICESCARF"})) {   // :117-118
            if (lastMoveUsed != null && pbHasMove(moveIdOf(lastMoveUsed))) {    // :119
                effects.set(PBEffects.Battler.ChoiceBand, moveIdOf(lastMoveUsed));          // :120
            } else if (lastRegularMoveUsed != null && pbHasMove(moveIdOf(lastRegularMoveUsed))) {   // :121
                effects.set(PBEffects.Battler.ChoiceBand, moveIdOf(lastRegularMoveUsed));   // :122
            }
        }
        if (effects.intVal(PBEffects.Battler.Charge) == 1) {                     // :125
            effects.set(PBEffects.Battler.Charge, 0);
        }
        effects.set(PBEffects.Battler.GemConsumed, 0);                           // :126
        for (Battler b : battle.eachBattler()) {                                 // :127
            b.pbContinualAbilityChecks();                                        // :127 Trace, end primordial weathers
        }
    }

    /**
     * The {@code Pokemon.MoveSlot} behind a {@code BattleMove}, i.e. Ruby's
     * {@code move.realMove} (Battler_ChangeSelf:136). {@code null} when the move
     * is not in the moveset - Ruby's {@code pp<0} object (Struggle,
     * Move_Effects_Generic:57; {@code pbUseMoveSimple} Battler_UseMove:160).
     * Bridge: this runtime's {@link BattleMove} carries no {@code pp} field.
     */
    private Pokemon.MoveSlot slotOf(BattleMove move) {
        if (move == null || move.internalName() == null) return null;
        for (Pokemon.MoveSlot slot : pokemon.moves) {
            if (slot != null && slot.move != null && move.internalName().equals(slot.move.internalName)) {
                return slot;
            }
        }
        return null;
    }

    /** {@code pbReducePP(move)} (Battler_ChangeSelf:141-148). */
    public boolean pbReducePP(BattleMove move) {
        if (usingMultiTurnAttack()) return true;                                 // :142
        Pokemon.MoveSlot slot = slotOf(move);
        if (slot == null) return true;                                           // :143 move.pp<0 (see slotOf)
        if (slot.maxPp <= 0) return true;                                        // :144 move.totalpp<=0
        if (slot.pp == 0) return false;                                          // :145
        if (slot.pp > 0) pbSetPP(move.internalName(), slot.pp - 1);              // :146
        return true;                                                             // :147
    }

    /** {@code pbReducePPOther(move)} (Battler_ChangeSelf:150-152). */
    public void pbReducePPOther(BattleMove move) {
        Pokemon.MoveSlot slot = slotOf(move);
        if (slot != null && slot.pp > 0) pbSetPP(move.internalName(), slot.pp - 1);   // :151
    }

    /**
     * {@code pbConfusionDuration(duration=-1)} (Battler_Statuses:542-545)。
     *
     * @param duration {@code <=0} 时按 Ruby 掷 {@code 2 + @battle.pbRandom(4)}
     */
    public int pbConfusionDuration(int duration) {
        int d = duration;
        if (d <= 0) {                                                    // :543
            d = 2 + battle.pbRandom(4);                                  // :543
        }
        return d;                                                        // :544
    }

    /** {@code pbConfusionDuration} 的默认档（{@code duration=-1}，Battler_Statuses:542）。 */
    public int pbConfusionDuration() {
        return pbConfusionDuration(-1);
    }

    /** {@code pbConfuse(msg=nil)} (Battler_Statuses:531-540)。 */
    public void pbConfuse() {
        pbConfuse(null);
    }

    /**
     * {@code pbConfuse(msg=nil)} (Battler_Statuses:531-540)。
     *
     * <p>{@code @battle.pbCommonAnimation("Confusion",self)} 走真实的
     * {@link Battle#commonAnimation}；{@code PBDebug.log}（:536）按 M0b 裁决登记为空实现。</p>
     */
    public void pbConfuse(String msg) {
        effects.set(PBEffects.Battler.Confusion, pbConfusionDuration()); // :532
        battle.commonAnimation("Confusion", this);                       // :533
        String message = msg;                                            // :534
        if (message == null || message.isEmpty()) {
            message = pbThis() + "混乱了！";                              // :534
        }
        display(message);                                                // :535
        // 登记: Battler_Statuses:536 PBDebug.log("[Lingering effect] ...")（PBDebug 未建模，静音）
        pbItemStatusCureCheck(0, false);                                 // :538
        pbAbilityStatusCureCheck();                                      // :539
    }

    // =========================================================================
    // Stage 4 / L2 —— **纯追加**：L2 招式效果层正在用、而本类还没移植的 5 个真方法
    //
    // 这 5 个不是「无替代」而是「没移植」：`movebase` 的 pre-wire 迁移 pass 把 37 处桩
    // 换成真实方法后，剩下 44 处里有 32 处卡在这 5 个上。全部按 Ruby 原文逐行转译，
    // 默认参数档一并补齐（与补 17 个中间档时同一标准）。本行以上的代码一行未改。
    // =========================================================================

    /**
     * {@code pbSleep(msg=nil)} (Battler_Statuses:354-356) 的默认档。
     *
     * <p>原文 {@code def pbSleep(msg=nil)} —— {@code msg} 有默认值，所以 Ruby 里可以无参调用。
     * 本档把默认值补齐，转发到 1 档 {@link #pbSleep(String)}。</p>
     */
    public void pbSleep() {
        pbSleep(null);
    }

    /**
     * {@code pbSleep(msg=nil)} (Battler_Statuses:354-356)。
     *
     * <p>{@code :355 pbInflictStatus(PBStatuses::SLEEP,pbSleepDuration,msg)} —— 原文只传 3 个实参，
     * 第 4 个 {@code user} 走 Ruby 默认值 {@code nil}（本类 4 档签名的第 4 参传 {@code null}）；
     * {@code pbSleepDuration} 不带括号即调用 0 档（见 {@link #pbSleepDuration()}）。</p>
     */
    public void pbSleep(String msg) {
        pbInflictStatus(PBStatuses.SLEEP, pbSleepDuration(), msg, null);  // :355
    }

    /** {@code pbSleepDuration(duration=-1)} (Battler_Statuses:362-366) 的默认档。 */
    public int pbSleepDuration() {
        return pbSleepDuration(-1);
    }

    /**
     * {@code pbSleepDuration(duration=-1)} (Battler_Statuses:362-366)。
     *
     * <p>{@code :364 duration = (duration/2).floor if hasActiveAbility?(:EARLYBIRD)} —— Ruby 的
     * Integer 除法本身就是向下取整，{@code .floor} 是恒等操作，所以这里是整数除法
     * {@code d / 2}（**不是** {@code Math.floor}）。</p>
     */
    public int pbSleepDuration(int duration) {
        int d = duration;
        if (d <= 0) {                                                    // :363
            d = 2 + battle.pbRandom(3);                                  // :363
        }
        if (hasActiveAbility("EARLYBIRD")) {                             // :364
            d = d / 2;                                                   // :364 (duration/2).floor
        }
        return d;                                                        // :365
    }

    /**
     * {@code pbCanFreeze?(user,showMessages,move=nil)} (Battler_Statuses:432-434) 的默认档。
     *
     * <p>原文 {@code move=nil} 有默认值 ⇒ 补齐 2 档，转发到 3 档。</p>
     */
    public boolean pbCanFreeze(Battler user, boolean showMessages) {
        return pbCanFreeze(user, showMessages, null);
    }

    /**
     * {@code pbCanFreeze?(user,showMessages,move=nil)} (Battler_Statuses:432-434)。
     *
     * <p>{@code :433 return pbCanInflictStatus?(PBStatuses::FROZEN,user,showMessages,move)} ——
     * 原文只传 4 个实参，本类的 {@code pbCanInflictStatus} 是 5 档
     * （{@code (int,Battler,boolean,BattleMove,boolean)}），第 5 个 {@code ignoreStatus}
     * 走 Ruby 默认值 {@code false}。</p>
     */
    public boolean pbCanFreeze(Battler user, boolean showMessages, BattleMove move) {
        return pbCanInflictStatus(PBStatuses.FROZEN, user, showMessages, move, false); // :433
    }

    /** {@code pbFreeze(msg=nil)} (Battler_Statuses:436-438) 的默认档。 */
    public void pbFreeze() {
        pbFreeze(null);
    }

    /**
     * {@code pbFreeze(msg=nil)} (Battler_Statuses:436-438)。
     *
     * <p>{@code :437 pbInflictStatus(PBStatuses::FROZEN,0,msg)} —— 第 2 个实参是字面量
     * {@code 0}（不是 {@code pbSleepDuration} 那样的调用）；第 4 个 {@code user} 走默认
     * {@code nil}。</p>
     */
    public void pbFreeze(String msg) {
        pbInflictStatus(PBStatuses.FROZEN, 0, msg, null);                // :437
    }

    /** {@code pbFlinch(_user=nil)} (Battler_Statuses:619-621) 的默认档。 */
    public void pbFlinch() {
        pbFlinch(null);
    }

    /**
     * {@code pbFlinch(_user=nil)} (Battler_Statuses:619-621)。
     *
     * <p>原文形参名带下划线前缀 {@code _user}（Ruby 的「声明但不用」约定），方法体里确实
     * 没用它 —— 本档保留形参只为 arity 对齐。{@code :620} 的 {@code @battle.moldBreaker}
     * 走本类已有的私有访问器 {@link #moldBreaker()}（其 javadoc 说明当前可证为 false）。</p>
     */
    public void pbFlinch(Battler user) {
        if (hasActiveAbility("INNERFOCUS") && !moldBreaker()) {          // :620
            return;
        }
        effects.set(PBEffects.Battler.Flinch, true);                     // :621
    }

    /**
     * {@code movedThisRound?} (PokeBattle_Battler:704-706)。
     *
     * <p>{@code :705 return @lastRoundMoved && @lastRoundMoved==@battle.turnCount} —— 前半段是对
     * Integer 的<b>真值</b>判断：Ruby 里 Integer 恒为真，只有 {@code nil} 才假；本运行时的
     * {@link #lastRoundMoved} 是 {@code int} 且 {@code Battler_Initialize:172} 初始化为
     * {@code -1}，永不为 {@code nil} ⇒ 该守卫恒真，故 Java 侧只留相等比较。这一点写在这里
     * 是为了说明「不是漏抄了守卫」，而不是简化。</p>
     */
    public boolean movedThisRound() {
        return lastRoundMoved == battle.turnCount();                     // :705
    }
}
