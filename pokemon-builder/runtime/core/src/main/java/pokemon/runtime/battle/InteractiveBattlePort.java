package pokemon.runtime.battle;

import pokemon.runtime.pokemon.*;
import pokemon.runtime.state.Inventory;
import com.badlogic.gdx.utils.Array;
import java.util.Random;
import java.util.function.Supplier;

/** Asynchronous desktop/Android battle port. No GL dependencies in the session. */
public final class InteractiveBattlePort implements BattlePort {
    private final TrainerState trainer;
    private final Inventory inventory;
    private final Supplier<PbsData> data;
    private final Runnable whiteout;
    private final Random random;
    private Session session;
    private boolean zaMode;
    /** PField_Battles:510-516: the interpreter reads this when it resumes. */
    private BattleResult lastResult;
    /** setBattleRule("canLose"): suppress the white-out for the next battle. */
    private boolean canLose;
    /** setBattleRule("canRun") / pbWildBattle's canRun: the next battle only. */
    private boolean canRun = true;
    /** {@code setBattleRule("disablePokeballs")} (PField_Battles:109). */
    private boolean disablePokeBalls;
    /**
     * {@code battle.environment} (PField_Battles:146-151): the map's
     * MetadataEnvironment. -1 = the map declares none.
     */
    private int battleEnvironment = -1;
    /**
     * {@code battle.time} (PField_Battles:181-188): 2 = night or cave (Dusk
     * Balls), 1 = evening, 0 = day. Read when the battle starts.
     */
    private int battleTime;
    /** {@code $game_switches}: the interpreter binds the game state. */
    private java.util.function.IntPredicate gameSwitches;
    /** $game_switches[199] / [12] for pbGainExpOne's level lock. */
    private boolean levelLockOn;
    private java.util.function.IntUnaryOperator gameVariables;
    private boolean leaguePass;
    public InteractiveBattlePort(TrainerState trainer, Inventory inventory, Supplier<PbsData> data, Runnable whiteout, Random random) {
        this.trainer = trainer; this.inventory = inventory; this.data = data; this.whiteout = whiteout; this.random = random;
    }
    /** $PokemonSystem.battle_rule: false = classic Mega, true = ZA mode. */
    public void zaMode(boolean value) { this.zaMode = value; }
    /** {@code $PokemonSystem.mega_animation} (Mega evolution:366-369): 0 = the full scene. */
    private int megaAnimation;
    public void megaAnimation(int value) { this.megaAnimation = value; }
    /**
     * {@code $PokemonSystem.battlestyle} (PField_Battles:111): 0 = Switch (the
     * game asks before an opponent sends out a new Pokemon), 1 = Set.
     */
    private int battlestyle;
    public void battleStyle(int value) { this.battlestyle = value; }
    /** {@code setBattleRule("switchstyle")} / {@code ("setstyle")} (PField_Battles:112). */
    private Boolean switchStyleRule;
    public void setSwitchStyle(boolean value) { this.switchStyleRule = value; }
    /** {@code $PokemonSystem.battlescene} (PField_Battles:114): 0 = Battle Effects on. */
    private int battlescene;
    public void battleScene(int value) { this.battlescene = value; }
    /** {@code setBattleRule("anims")} / {@code ("noanims")} (PField_Battles:43-44). */
    private Boolean battleAnimsRule;
    public void setBattleAnims(boolean value) { this.battleAnimsRule = value; }
    private boolean switchStyle() {
        return switchStyleRule == null ? battlestyle == 0 : switchStyleRule;
    }
    public Session session() { return session; }
    public boolean pending() { return session != null; }
    public BattleResult wildBattle(String species, int level) {
        PbsData pbs = data.get();
        if (pbs != null && pbs.species(species) != null) freeWildBattle(factory().wildPokemon(pbs.species(species), level, pbs));
        return null;
    }
    @Override
    public BattleResult doubleWildBattle(String species1, int level1, String species2, int level2) {
        PbsData pbs = data.get();
        if (pbs == null || pbs.species(species1) == null || pbs.species(species2) == null) {
            return wildBattle(species1, level1);
        }
        java.util.List<Pokemon> foes = new java.util.ArrayList<>();
        foes.add(factory().wildPokemon(pbs.species(species1), level1, pbs));
        foes.add(factory().wildPokemon(pbs.species(species2), level2, pbs));
        return freeWildBattle(foes);
    }
    public BattleResult freeWildBattle(Pokemon foe) {
        if (foe != null) start(java.util.Collections.singletonList(Array.with(foe)), false, null);
        return null;
    }
    public BattleResult freeWildBattle(java.util.List<Pokemon> foes) {
        if (foes.isEmpty()) return null;
        // pbWildBattleCore:280-297: every wild Pokemon is one foe party entry of the one (absent) opposing trainer
        Array<Pokemon> team = new Array<>();
        for (Pokemon foe : foes) if (foe != null) team.add(foe);
        start(java.util.Collections.singletonList(team), false, null);
        return null;
    }
    public BattleResult trainerBattle(PbsData.TrainerData opponent) {
        return trainerBattle(java.util.Collections.singletonList(opponent));
    }
    public BattleResult trainerBattle(java.util.List<PbsData.TrainerData> opponents) {
        if (opponents.isEmpty() || data.get() == null) return null;
        java.util.List<Array<Pokemon>> teams = new java.util.ArrayList<>();
        for (PbsData.TrainerData opponent : opponents) {
            if (opponent == null) return null;
            Array<Pokemon> foes = new Array<>();
            for (PbsData.TrainerPokemon member : opponent.party) {
                Pokemon p = factory().trainerPokemon(member, data.get()); if (p != null) foes.add(p);
            }
            teams.add(foes);
        }
        start(teams, true, opponents); return null;
    }
    /** {@code setBattleRule("single"/"double"/...)} for the next battle. */
    private String battleSize;
    public void setBattleSize(String size) { this.battleSize = size; }
    /** {@code $PokemonGlobal.partner} for the next battle. */
    private String partnerType;
    private String partnerName;
    private Array<Pokemon> partnerParty;
    public void setPartner(String trainerType, String name, Iterable<Pokemon> party) {
        if (name == null) { partnerParty = null; return; }
        partnerType = trainerType;
        partnerName = name;
        partnerParty = new Array<>();
        for (Pokemon pokemon : party) partnerParty.add(pokemon);
    }
    private boolean noPartner;
    public void setNoPartner(boolean value) { this.noPartner = value; }
    private HeadlessBattlePort factory() {
        HeadlessBattlePort factory = new HeadlessBattlePort(trainer, data, null, random);
        // pbGenerateWildPokemon reads the bag (Shiny Charm, :436-442), the map
        // (@obtainMap, :951) and the fateful-encounter switch (:954-955).
        factory.setBag(inventory);
        factory.setObtainMap(obtainMap);
        factory.setFatefulEncounter(fatefulEncounter);
        return factory;
    }

    /** {@code $game_map.map_id} for a battle started by an event. */
    private int obtainMap;

    public void setObtainMap(int value) {
        this.obtainMap = value;
    }

    /** {@code $game_switches[FATEFUL_ENCOUNTER_SWITCH]} (Settings:32). */
    private boolean fatefulEncounter;

    public void setFatefulEncounter(boolean value) {
        this.fatefulEncounter = value;
    }
    /** {@code pbCryFile} + {@code pbSEPlay} for the engine's own lines. */
    private Battle.CryPlayer cryPlayer;
    public void setCryPlayer(Battle.CryPlayer player) { this.cryPlayer = player; }

    private void start(java.util.List<Array<Pokemon>> teams, boolean trainerBattle, java.util.List<PbsData.TrainerData> opponents) {
        if (pending() || teams.isEmpty() || teams.get(0).isEmpty() || trainer.party.firstAble() == null) return;
        lastResult = null;
        session = new Session(teams, trainerBattle, opponents);
        battleSize = null;                       // the recorded rules belong to this battle only (PField_Battles:498)
        partnerParty = null;
        noPartner = false;
    }
    public BattleResult lastResult() { return lastResult; }
    public void setCanLose(boolean value) { this.canLose = value; }
    /** {@code setBattleRule("noExp")}: {@code battle.expGain} (PField_Battles:105) of the next battle. */
    public void setExpGain(boolean value) { this.expGain = value; }
    private boolean expGain = true;                 // PokeBattle_Battle:145 @expGain = true
    /** {@code setBattleRule("canRun")} / pbWildBattle's {@code canRun}. */
    public void setCanRun(boolean value) { this.canRun = value; }
    /** {@code setBattleRule("disablePokeballs")} applies to the next battle. */
    public void setDisablePokeBalls(boolean value) { this.disablePokeBalls = value; }
    /** {@code $game_switches} reads switch 60 ("不可捕捉的野外对战"). */
    public void setSwitchSource(java.util.function.IntPredicate switches) {
        this.gameSwitches = switches;
    }
    /** {@code $game_variables}: variable 100 is the project's level-follow mode (PField_Battles:468, Battle_StartAndEnd:142). */
    public void setVariableSource(java.util.function.IntUnaryOperator variables) {
        this.gameVariables = variables;
    }
    /**
     * PField_Battles:146-151 + :181-188: the map's MetadataEnvironment decides
     * {@code battle.environment} and, for a cave, {@code battle.time = 2};
     * otherwise the time comes from the day/night hour.
     */
    public void setBattleEnvironment(int environment) {
        this.battleEnvironment = environment;
        if (environment == CaptureCalculator.ENVIRONMENT_CAVE) {
            this.battleTime = 2;                            // :181-183
        } else if (pokemon.runtime.map.DayNightTone.test("isNight?")) {
            this.battleTime = 2;                            // :185-186
        } else if (pokemon.runtime.map.DayNightTone.test("isEvening?")) {
            this.battleTime = 1;                            // :187
        } else {
            this.battleTime = 0;
        }
    }
    @Override public void setLevelLock(boolean on, boolean leaguePass) {
        this.levelLockOn = on;
        this.leaguePass = leaguePass;
    }
    public void finish() {
        if (session == null || session.result == null) return;
        BattleResult result = session.result;
        boolean caughtStored = session.caughtStored;       // the screen already ran storeCaught()
        session = null;
        lastResult = result;
        // pbRecordAndStoreCaughtPokemon (PokeBattle_BattleCommon:55-63): a Pokemon the engine caught (pbThrowPokeBall)
        // joins the party; the port's own ball() path has already added its own.
        // 登记: the box messages of pbStorePokemon (:11-38) and the Pokedex entry page (:48-52) are not modelled.
        if (!caughtStored && result.caught != null && !trainer.party.members().contains(result.caught, true)) trainer.addToParty(result.caught);
        for (Pokemon p : trainer.party.members()) trainer.registerOwned(p);
        // PField_Battles:619-658 pbAfterBattle runs before Events.onEndBattle
        // (:656), which is where the white-out lives (:701-706).
        BattleAftermath.pbAfterBattle(trainer, data.get(), result, canLose);
        boolean whiteOut = BattleAftermath.onEndBattle(trainer, data.get(), random, result, canLose);
        if (whiteOut && whiteout != null) whiteout.run();
        canLose = false;
        expGain = true;
        canRun = true;
        switchStyleRule = null;
        battleAnimsRule = null;
    }
    /**
     * Separator between two {@code pbDisplay} messages inside one
     * {@code Session.message} string. A {@code "\n"} is a line break *within*
     * one message window (Battle_StartAndEnd:220), so the two are not the same.
     */
    public static final String MESSAGE_BREAK = "\f";
    /** Settings:28 {@code EXP_POT_MAX}. */
    private static final int EXP_POT_MAX = 99999999;

    /** One thing {@code pbRecordAndStoreCaughtPokemon} shows: a line, or the Pokedex entry page of a new species. */
    public static final class CaughtStep {
        public final String text;
        public final PbsData.Species dex;

        CaughtStep(String text, PbsData.Species dex) {
            this.text = text;
            this.dex = dex;
        }
    }

    public final class Session {
        public final Battle battle;
        public final boolean trainerBattle;
        /** The trainers.txt row for a trainer battle (null for a wild battle). */
        public final PbsData.TrainerData trainerData;
        /** The second opposing trainer's row, or null. */
        public final PbsData.TrainerData trainerData2;
        /** The third opposing trainer's row (a triple trainer battle), or null. */
        public final PbsData.TrainerData trainerData3;
        /** The third opposing trainer's {@code fullname}, or null. */
        public String trainerFullname3;
        /** The third opponent's LoseText. */
        public String endSpeech3;
        /** The second opposing trainer's {@code fullname}, or null. */
        public String trainerFullname2;
        /** The second opponent's LoseText. */
        public String endSpeech2;
        /** The partner trainer's {@code fullname}, or null when none fights with the player. */
        public String partnerFullname;
        /** The partner trainer's trainer type (its back sprite), or null. */
        public String partnerTrainerType;
        public BattleResult result;
        public String message;
        /** pbGainExp:58-61: the "经验储罐" line played after the awards. */
        public String expPotMessage;
        /** Battle_StartAndEnd:456: "你打败了\n{fullname}！". */
        public String trainerFullname;
        /** Battle_StartAndEnd:466: the opponent's LoseText ("..." when empty). */
        public String endSpeech;
        /** Battle_StartAndEnd:394-409: the prize money of a won trainer battle. */
        public int prizeMoney;
        private int move;
        private int escapes;
        /**
         * The round's lines, in the order the plugin printed them: whatever the
         * player's action displayed first, then the engine's own lines
         * ({@code Battler_UseMove:290} for both sides, {@code Battler_ChangeSelf:71}).
         * {@link #endMessage()} joins it with MESSAGE_BREAK, which is how
         * {@code BattleScreen} shows one {@code pbDisplay} call per line.
         */
        private final Array<String> log = new Array<>();
        /**
         * The engine's events for this action ({@link Battle#roundEvents}: the
         * moves' and the end of round's, in the plugin's order), copied when the
         * round has run.
         */
        private final Array<Battle.RoundEvent> engineEvents = new Array<>();
        /**
         * pbThrowPokeBall's own scene calls (:93-164) in order: its lines and
         * the throw animation. Played before the engine's events - the
         * opponent's move that follows a failed throw.
         */
        private final Array<Battle.RoundEvent> ballEvents = new Array<>();
        /**
         * The whole action as the scene must play it: this port's own lines
         * ({@link #log}), then {@link #engineEvents}, then the closing line
         * ({@link #message} as set by {@link #endMessage()}). Built by
         * {@code endMessage}; {@link #takeEvents()} hands it to the screen once.
         */
        private final Array<Battle.RoundEvent> events = new Array<>();
        Session(java.util.List<Array<Pokemon>> teams, boolean trainerBattle, java.util.List<PbsData.TrainerData> opponents) {
            this.trainerBattle = trainerBattle;
            this.trainerData = opponents == null || opponents.isEmpty() ? null : opponents.get(0);
            this.trainerData2 = opponents == null || opponents.size() < 2 ? null : opponents.get(1);
            this.trainerData3 = opponents == null || opponents.size() < 3 ? null : opponents.get(2);
            battle = new Battle(data.get(), random, (user, foe, moves) -> move);
            battle.zaMode = zaMode;
            battle.fullMegaAnimation = megaAnimation == 0;       // Mega evolution:366-369 za_full_mega_animation?
            battle.bagHasItem = id -> inventory != null && inventory.has(id);   // $PokemonBag.pbHasItem?
            battle.setCanRun(canRun);       // PField_Battles:101 / :360
            battle.switchStyle = switchStyle();   // PField_Battles:111-112
            // PField_Battles:114-115 battle.showAnims = ($PokemonSystem.battlescene==0),
            // then the "anims"/"noanims" rule.
            battle.showAnims = battleAnimsRule != null ? battleAnimsRule : battlescene == 0;
            battle.trainerBattle = trainerBattle;
            if (trainerBattle && trainerData != null) for (String it : trainerData.items) battle.foeItems.add(it);   // @items (PField_Battles:pbTrainerBattleCore items)
            battle.playerName = trainer.name;
            battle.expGain = expGain;       // PField_Battles:105
            battle.levelLockOn = levelLockOn;
            battle.leaguePass = leaguePass;
            if (gameSwitches != null) battle.gameSwitches = gameSwitches;   // $game_switches (Battler_UseMove_SuccessChecks:157,165)
            battle.badges = trainer.badges;
            battle.setCryPlayer(cryPlayer);       // Battler_UseMove_SuccessChecks:318-319
            battle.scene = new CoroutineScene();   // @scene
            battle.captureHooks = new BallHooks(); // pbThrowPokeBall's outside world (PokeBattle_BattleCommon:68-232)
            int foeCount = 0;
            for (Array<Pokemon> team : teams) foeCount += team.size;
            // PField_Battles:303-318 / :459-487: the partner trainer joins when there is room for one.
            String size = battleSize;
            boolean roomForPartner = foeCount > 1;
            if (!roomForPartner && size != null && !isSingleSize(size)) roomForPartner = true;
            boolean withPartner = partnerParty != null && !noPartner && roomForPartner;
            int levelFollow = gameVariables == null ? 0 : gameVariables.applyAsInt(100);
            if (trainerBattle && withPartner && levelFollow > 0) {                   // PField_Battles:468-480
                int maxLevel = 0;
                for (Pokemon p : trainer.party.members()) {
                    if (p == null || p.egg || p.level <= maxLevel) continue;
                    maxLevel = p.level;
                }
                for (Pokemon p : partnerParty) setLevelKeepingHp(p, maxLevel);
            }
            for (Pokemon p : trainer.party.members()) battle.addPlayer(p);
            if (withPartner) {
                for (Pokemon p : partnerParty) battle.addPartner(p);
                PbsData pbs = data.get();
                PbsData.TrainerType type = pbs == null || partnerType == null ? null : pbs.trainerTypes.get(partnerType);
                partnerFullname = (type == null || type.name == null ? "" : type.name + " ") + partnerName;   // Trainer#fullname
                battle.partnerName = partnerFullname;
                partnerTrainerType = partnerType;
                if (size == null) size = "double";                                   // :317 / :486 setBattleRule("double") if !size
            }
            if (trainerBattle && levelFollow > 0) {                                  // Battle_StartAndEnd:140-161 "动态等级"
                int level = 1;
                java.util.List<Pokemon> mine = new java.util.ArrayList<>();
                for (Pokemon p : trainer.party.members()) mine.add(p);
                if (withPartner) for (Pokemon p : partnerParty) mine.add(p);
                if (levelFollow == 2) {                                              // :145 average level
                    int sum = 0;
                    for (Pokemon p : mine) sum += p.level;
                    level = Math.max(1, mine.isEmpty() ? 0 : sum / mine.size());
                } else if (levelFollow == 1) {                                       // :150 highest level
                    for (Pokemon p : mine) if (level < p.level) level = p.level;
                }
                for (Array<Pokemon> team : teams) {
                    for (Pokemon p : team) {
                        if (p.level == 1) continue;                                  // :157
                        if (p.level < level) setLevelKeepingHp(p, level);            // :158-159
                    }
                }
            }
            for (int owner = 0; owner < teams.size(); owner++) {
                for (Pokemon p : teams.get(owner)) {
                    if (owner == 0) battle.addFoe(p);
                    else if (owner == 1) battle.addFoeSecondTrainer(p);
                    else battle.addFoeThirdTrainer(p);
                    trainer.registerSeen(p);
                }
            }
            // pbPrepareBattle:88-97: the forced-double switch, else the recorded size
            if (gameSwitches != null && gameSwitches.test(41)
                    && (ableCount(trainer.party.members()) >= 2 || withPartner)) {
                size = "double";                                                     // :88-90 (the "not enough Pokemon" message of :92 is an event-side line)
            }
            if (size != null) battle.setBattleMode(size);                            // :96
            battle.pbEnsureParticipants();                                           // Battle_StartAndEnd:301
            // Battle_StartAndEnd:194-228: the "wants to battle" line. It is
            // produced by the transcribed pbStartBattleSendOut
            // (BattleSendOut.plan), which branches on the wild party size, the
            // battle rank and $game_switches[196] - so nothing is built here.
            if (trainerBattle && trainerData != null) {
                PbsData pbs = data.get();
                PbsData.TrainerType type = pbs == null || trainerData.type == null ? null
                        : pbs.trainerTypes.get(trainerData.type);
                // Trainer#fullname (PokeBattle_Trainer:31-33).
                trainerFullname = (type == null || type.name == null ? "" : type.name + " ") + trainerData.name;
                battle.opponentName = trainerFullname;                 // PokeBattle_Battle:268
                // Battle_StartAndEnd:466: the opponent's LoseText, "..." when empty.
                endSpeech = trainerData.loseText == null || trainerData.loseText.isEmpty()
                        ? "..." : trainerData.loseText;
            }
            if (trainerBattle && trainerData3 != null) {
                PbsData pbs = data.get();
                PbsData.TrainerType type = pbs == null || trainerData3.type == null ? null
                        : pbs.trainerTypes.get(trainerData3.type);
                trainerFullname3 = (type == null || type.name == null ? "" : type.name + " ") + trainerData3.name;
                battle.opponentName3 = trainerFullname3;
                endSpeech3 = trainerData3.loseText == null || trainerData3.loseText.isEmpty()
                        ? "..." : trainerData3.loseText;
            }
            if (trainerBattle && trainerData2 != null) {
                PbsData pbs = data.get();
                PbsData.TrainerType type = pbs == null || trainerData2.type == null ? null
                        : pbs.trainerTypes.get(trainerData2.type);
                trainerFullname2 = (type == null || type.name == null ? "" : type.name + " ") + trainerData2.name;
                battle.opponentName2 = trainerFullname2;
                endSpeech2 = trainerData2.loseText == null || trainerData2.loseText.isEmpty()
                        ? "..." : trainerData2.loseText;
            }
        }

        /** {@code pkmn.level = level; pkmn.calcStats}: exp follows the level, a full-HP Pokemon stays full. */
        private void setLevelKeepingHp(Pokemon p, int level) {
            if (p == null || p.level == level) return;
            boolean full = p.hp >= p.maxHp();
            p.level = level;
            p.exp = pokemon.runtime.pokemon.PokemonStats.experienceForLevel(p.growthRate(), level);
            p.hp = full ? p.maxHp() : Math.min(p.hp, p.maxHp());
        }

        /** {@code @opponent[i]}'s trainers.txt row, or null. */
        public PbsData.TrainerData trainerData(int i) {
            return i == 0 ? trainerData : (i == 1 ? trainerData2 : (i == 2 ? trainerData3 : null));
        }

        /** {@code @opponent[i].fullname}, or null. */
        public String trainerFullname(int i) {
            return i == 0 ? trainerFullname : (i == 1 ? trainerFullname2 : (i == 2 ? trainerFullname3 : null));
        }

        /** {@code @endSpeeches[i]}: the opponent's LoseText ("..." when empty), or null. */
        public String endSpeech(int i) {
            return i == 0 ? endSpeech : (i == 1 ? endSpeech2 : (i == 2 ? endSpeech3 : null));
        }

        /** {@code ["single","1v1","1v2","1v3"].include?(size)} (PField_Battles:305, :461). */
        private boolean isSingleSize(String size) {
            return "single".equals(size) || "1v1".equals(size) || "1v2".equals(size) || "1v3".equals(size);
        }

        /** {@code $Trainer.ablePokemonCount}. */
        private int ableCount(Iterable<Pokemon> party) {
            int count = 0;
            for (Pokemon p : party) if (p != null && !p.egg && p.hp > 0) count++;
            return count;
        }
        /**
         * {@code pbCanShowFightMenu?} (Battle_Phase_Command:43-55): {@code false}
         * when no slot can be chosen, which makes {@code pbFightMenu:68} take the
         * {@code pbAutoChooseMove} path instead of showing the menu. The Encore
         * branch (:46) needs PBEffects.
         */
        public boolean canShowFightMenu() {
            return battle.player() != null && canShowFightMenu(battle.player().index);
        }

        public boolean canShowFightMenu(int idxBattler) {
            return battle.battlerAt(idxBattler) != null && battle.pbCanShowFightMenu(idxBattler);   // :43-55
        }

        /**
         * {@code pbCommandPhaseLoop(true)} (Battle_Phase_Command:197-263) for the player: the battlers the player gives
         * commands to this round, in battler order - the ones that are not forced into an action (:207-208).
         */
        public int[] commandBattlers() {
            java.util.List<Integer> out = new java.util.ArrayList<>();
            for (int idx = 0; idx <= battle.maxBattlerIndex(); idx++) {
                if (battle.battlerAt(idx) == null || !battle.pbOwnedByPlayer(idx)) continue;   // :206
                if (!":None".equals(battle.choices(idx)[0])) continue;                          // :207
                if (!battle.pbCanShowCommands(idx)) continue;                                  // :208
                out.add(idx);
            }
            int[] ret = new int[out.size()];
            for (int i = 0; i < ret.length; i++) ret[i] = out.get(i);
            return ret;
        }

        /**
         * {@code pbRegisterMove(idxBattler,slot)} for one battler of a double battle's command phase
         * (Battle_Phase_Command:84-86): the round does not start yet.
         *
         * @return null when the move was registered, else the refusal line ("" = nothing to show)
         */
        public String registerMove(int idxBattler, int slot) {
            if (result != null || battle.battlerAt(idxBattler) == null) return "";
            String refusal = battle.canChooseMove(idxBattler, slot);
            if (refusal != null) return refusal;
            return battle.registerMove(idxBattler, slot) ? null : "";
        }

        /** {@code move.pbTarget(battler)} of a slot: the target menu's mode. */
        public int targetType(int idxBattler, int slot) {
            return battle.pbMoveTargetType(idxBattler, slot);
        }

        /** {@code pbChooseTarget} (Battle_Phase_Command:98-104): a double battle asks for a target (not a single battle, :87). */
        public boolean needsTargetChoice() {
            return !battle.singleBattle();
        }

        /** {@code pbCreateTargetTexts} (Scene_Commands:371-391). */
        public String[] targetTexts(int idxBattler, int targetType) {
            return battle.pbCreateTargetTexts(idxBattler, targetType);
        }

        /** {@code pbFirstTarget} (Scene_Commands:395-417). */
        public int firstTarget(int idxBattler, int targetType) {
            return battle.pbFirstTarget(idxBattler, targetType);
        }

        /** {@code pbRegisterTarget(idxBattler,idxTarget)} (Battle_Phase_Command:102). */
        public void registerTarget(int idxBattler, int idxTarget) {
            battle.pbRegisterTarget(idxBattler, idxTarget);
        }

        /** {@code pbCancelChoice(idxBattler)} (Battle_Phase_Command:13-23): going back to an earlier battler's command. */
        public void cancelChoice(int idxBattler) {
            battle.cancelChoice(idxBattler);
        }

        /** {@code pbCanShowCommands?(idxBattler)} (Battle_Phase_Command:35-41). */
        public boolean canShowCommands() {
            return battle.player() != null && battle.pbCanShowCommands(battle.player().index);
        }

        public boolean canShowCommands(int idxBattler) {
            return battle.battlerAt(idxBattler) != null && battle.pbCanShowCommands(idxBattler);
        }

        // -----------------------------------------------------------------
        // The engine runs as a coroutine (EngineCoroutine): a scene call that has to wait for the player or an
        // animation (the party screen of pbSwitchInBetween, a yes/no question, the recall and send-out of
        // pbRecallAndReplace) stops it, the battle screen plays the events so far and the call, and the engine
        // carries on from the same place (Battle_Action_Switching:136-332).
        // -----------------------------------------------------------------

        /** The engine call that is stopped on a scene call, or null. */
        private EngineCoroutine engine;
        /** What the action that started {@link #engine} does with the engine's events once it has stopped or ended. */
        private Runnable engineTail;
        /** The scene call {@link #engine} is waiting on. */
        private Battle.SceneCall request;

        /**
         * Runs {@code body} as the engine until it ends or stops on a scene call, then {@code tail}.
         */
        private void drive(Runnable body, Runnable tail) {
            if (engine != null) {
                throw new IllegalStateException("the engine is still waiting on " + request.kind);
            }
            engine = new EngineCoroutine(body);
            engineTail = tail;
            continueEngine();
        }

        private void continueEngine() {
            EngineCoroutine running = engine;
            boolean finished = running.resume();
            engineEvents.addAll(battle.roundEvents);
            battle.roundEvents.clear();
            if (finished) {
                engine = null;
                request = null;
            } else {
                request = running.pending();
            }
            engineTail.run();
        }

        /** True while the engine waits on a scene call ({@link #request()}). */
        public boolean suspended() {
            return engine != null;
        }

        /** The scene call the engine waits on. */
        public Battle.SceneCall request() {
            return request;
        }

        /**
         * The screen has played {@link #request()}; the engine carries on. Its events from here on are taken
         * with {@link #takeEvents()}.
         */
        public void answer(Object value) {
            request.result = value;
            continueEngine();
        }

        /** The screen is closing: lets a waiting engine thread end. */
        public void abortEngine() {
            if (engine != null) {
                engine.abort();
                engine = null;
            }
        }

        /** What most engine calls do with their events once they stop or end. */
        private void roundTail() {
            applyExpPot();
            message = null;
            endMessage();
        }

        /** What {@link Battle#pbThrowPokeBall} asks of the player's side: name, ball names, the Bag and {@code pbCaptureCalc}. */
        private final class BallHooks implements Battle.CaptureHooks {
            @Override public String playerName() {
                return trainer.name;                                                    // pbPlayer.name
            }

            @Override public String itemName(String ball) {
                PbsData.Item item = data.get().item(ball);
                return item == null ? ball : item.name;                                 // PBItems.getName(ball)
            }

            @Override public int ballType(String ball) {
                return BallTypes.ballType(data.get(), ball);                            // pbGetBallType
            }

            @Override public void deleteItem(String ball) {
                inventory.remove(ball, 1);                                              // $PokemonBag.pbDeleteItem(ball,1)
            }

            @Override public int pbCaptureCalc(Battler target, String ball, int rareness) {
                CaptureCalculator.Context capture = captureContext(data.get(), target);
                if (rareness >= 0) capture.rareness = rareness;                         // PokeBattle_BattleCommon:174 `if !rareness`
                return CaptureCalculator.shakes(capture, ball);
            }

            @Override public void refreshChain(String species) {
                trainer.chainCatching.refresh(species);                                 // 343_ChainCatching:97
            }

            @Override public void onCatch(String ball, Pokemon pkmn) {
                CaptureCalculator.onCatch(data.get(), ball, pkmn);                      // BallHandlers.onCatch
            }
        }

        /** {@code @scene}: the battle screen plays what the engine waits on. */
        private final class CoroutineScene implements Battle.Scene {
            private final Battle.HeadlessScene headless = new Battle.HeadlessScene();

            private boolean onEngine() {
                return engine != null && engine.onEngineThread();
            }

            @Override public void pbPartyScreen(int idxBattler, boolean canCancel, java.util.function.IntFunction<String> block) {
                if (!onEngine()) {
                    headless.pbPartyScreen(idxBattler, canCancel, block);
                    return;
                }
                engine.call(new Battle.SceneCall(Battle.SceneCall.Kind.PARTY_SCREEN, idxBattler, canCancel, block,
                        null, null, false));
            }

            @Override public boolean pbDisplayConfirmMessage(String msg) {
                if (!onEngine()) {
                    return headless.pbDisplayConfirmMessage(msg);
                }
                Object answer = engine.call(new Battle.SceneCall(Battle.SceneCall.Kind.CONFIRM, -1, false, null,
                        msg, null, false));
                return Boolean.TRUE.equals(answer);
            }

            @Override public String pbChooseBallFromBag() {
                if (!onEngine()) {
                    return headless.pbChooseBallFromBag();
                }
                Object answer = engine.call(new Battle.SceneCall(Battle.SceneCall.Kind.CHOOSE_BALL, -1, false, null,
                        null, null, false));
                return answer instanceof String ? (String) answer : null;
            }

            @Override public void pbRecall(int idxBattler) {
                if (onEngine()) {
                    engine.call(new Battle.SceneCall(Battle.SceneCall.Kind.RECALL, idxBattler, false, null,
                            null, null, false));
                }
            }

            @Override public void pbShowPartyLineup(int side) {
                if (onEngine()) {
                    engine.call(new Battle.SceneCall(Battle.SceneCall.Kind.SHOW_PARTY_LINEUP, side, false, null,
                            null, null, false));
                }
            }

            @Override public void pbSendOutBattlers(int[] idxBattlers, boolean startBattle) {
                if (onEngine()) {
                    engine.call(new Battle.SceneCall(Battle.SceneCall.Kind.SEND_OUT, -1, false, null,
                            null, idxBattlers, startBattle));
                }
            }
        }

        /**
         * {@code pbBossBuffPhase} (PokeBattle_BOSS:37-90) at the start of a round. The events are taken
         * with {@link #takeEvents()}.
         *
         * @return true when something has to be played
         */
        public boolean bossBuffPhase() {
            if (result != null) {
                return false;
            }
            log.clear();
            engineEvents.clear();
            drive(battle::pbBossBuffPhase, this::roundTail);
            return events.size > 0 || engine != null;
        }

        /**
         * A round whose commands cannot be shown (Battle_Phase_Command:208: a multi-turn attack in progress):
         * the forced choice stays and the round simply runs.
         */
        public boolean forcedRound() {
            if (result != null || battle.player() == null) {
                return false;
            }
            log.clear();
            engineEvents.clear();
            drive(() -> result = battle.step(), this::roundTail);
            return true;
        }

        /**
         * {@code pbAutoChooseMove} (Battle_Action_AttacksPriority:36-67): the move the player uses when the
         * fight menu cannot be shown ({@code pbFightMenu:68}) - Encore's move, or Struggle.
         */
        /**
         * {@code pbAutoChooseMove(idxBattler)} for one battler of a double battle's command phase: it registers
         * Encore's move / Struggle and the round does not start. Its lines are taken with {@link #takeEvents()}.
         */
        public boolean autoChooseMoveOnly(int idxBattler) {
            if (result != null || battle.battlerAt(idxBattler) == null) return false;
            if (battle.battlerAt(idxBattler).fainted()) {          // :38-41
                battle.clearChoice(idxBattler);
                return true;
            }
            if (battle.pbCanShowFightMenu(idxBattler)) return false;
            log.clear();
            engineEvents.clear();
            battle.roundMessages.clear();
            battle.roundEvents.clear();
            battle.pbAutoChooseMove(idxBattler, true);             // :43-67
            engineEvents.addAll(battle.roundEvents);
            battle.roundEvents.clear();
            roundTail();
            return true;
        }

        public boolean autoChooseMove() {
            if (result != null || battle.player() == null) {
                return false;
            }
            Battler user = battle.player();
            if (user.fainted()) {                                  // :38-41
                battle.clearChoice(user.index);
                return true;
            }
            if (battle.pbCanShowFightMenu(user.index)) {
                return false;                                      // pbCanShowFightMenu? was true
            }
            log.clear();
            engineEvents.clear();
            drive(() -> {
                battle.roundMessages.clear();
                battle.roundEvents.clear();
                battle.pbAutoChooseMove(user.index, true);         // :43-67 (its lines are in roundEvents)
                engineEvents.addAll(battle.roundEvents);
                battle.roundEvents.clear();
                result = battle.step();
            }, this::roundTail);
            return true;
        }

        /**
         * {@code pbEffectsOnSwitchIn(true)} of the battlers that were just sent out
         * (Battle_Phase_Attack:69, Battle_Action_Switching:235-237): entry hazards, Healing Wish, abilities and
         * items. The events are taken with {@link #takeEvents()}.
         *
         * @return true when something happened that has to be played
         */
        public boolean switchInEffects(int[] idxBattlers) {
            if (result != null) {
                return false;
            }
            log.clear();
            engineEvents.clear();
            drive(() -> {
                battle.pbSwitchInEffects(idxBattlers);
                result = battle.result();
            }, this::roundTail);
            return events.size > 0 || engine != null || result != null;
        }

        /** {@code pbOnActiveAll} (Battle_StartAndEnd:354): abilities upon entering battle. */
        public boolean onActiveAll() {
            if (result != null) {
                return false;
            }
            log.clear();
            engineEvents.clear();
            drive(() -> {
                battle.pbOnActiveAllRound();
                result = battle.result();
            }, () -> {
                message = null;
                endMessage();
            });
            return events.size > 0 || engine != null || result != null;
        }

        /**
         * {@code pbAttackPhaseSwitch} (Battle_Phase_Attack:57-59): after the recall line, a
         * Pursuit aimed at the switching Pokemon hits before it leaves. The events are
         * taken with {@link #takeEvents()}.
         *
         * @return true when something happened that has to be played first
         */
        public boolean pursuitOnSwitch() {
            if (result != null || battle.player() == null) {
                return false;
            }
            return pursuitOnSwitch(battle.player().index);
        }

        /**
         * {@code pbAttackPhaseSwitch}'s start for a double battle (Battle_Phase_Attack:171-184): the turn counts and the
         * priority order is known, so the screen can play the player's switches in {@code pbPriority} order. The
         * events (a Quick Claw line, ...) are taken with {@link #takeEvents()}.
         *
         * @return true when something has to be played first
         */
        public boolean beginSwitchPhase() {
            if (result != null) {
                return false;
            }
            log.clear();
            engineEvents.clear();
            drive(battle::pbBeginSwitchPhase, this::roundTail);
            return events.size > 0 || engine != null;
        }

        /** The player's battlers that switch this round, in {@code pbPriority} order (Battle_Phase_Attack:51-52). */
        public int[] switchOrder() {
            return battle.playerSwitchersInOrder();
        }

        /** {@code @choices[idxBattler][1]} of a registered switch: the party entry coming in. */
        public int switchParty(int idxBattler) {
            return battle.choiceSwitchParty(idxBattler);
        }

        /** True when {@code idxBattler} has registered a switch this round. */
        public boolean switching(int idxBattler) {
            return battle.choiceIsSwitch(idxBattler);
        }

        /**
         * The rest of a double battle's round once every player battler has its command and none switches: the
         * moves of {@code pbAttackPhase} and the end of the round (the command phase of the opponent and the
         * partner has already stored their choices).
         */
        public boolean startRound() {
            if (result != null) {
                return false;
            }
            log.clear();
            engineEvents.clear();
            drive(() -> result = battle.step(), this::roundTail);
            return true;
        }

        public boolean pursuitOnSwitch(int idxBattler) {
            if (result != null) {
                return false;
            }
            log.clear();
            engineEvents.clear();
            drive(() -> {
                battle.pbPursuitOnSwitch(idxBattler);
                result = battle.result();
            }, this::roundTail);
            return events.size > 0 || engine != null || result != null;
        }

        /**
         * The rest of the round after the player's action:
         * {@code pbAttackPhaseMoves} / {@code pbEndOfRoundPhase}. Whatever this
         * action already printed ({@code message}) comes first, then the lines the
         * opponent's move produced ({@code Battler_UseMove:290}).
         */
        public BattleResult foeTurn() {
            if (result != null) {
                return result;
            }
            String printed = message;
            log.clear();
            engineEvents.clear();
            addLog(printed);
            drive(() -> result = battle.foeTurn(), () -> {
                message = null;
                endMessage();
            });
            return result;
        }

        /**
         * The events of the action that just ran, in order, handed over once
         * (the next call returns an empty list until another action runs).
         */
        public Array<Battle.RoundEvent> takeEvents() {
            Array<Battle.RoundEvent> out = new Array<>(events);
            events.clear();
            return out;
        }

        /** Appends one already-formatted line to the round's log. */
        private void addLog(String line) {
            if (line != null && !line.isEmpty()) {
                log.add(line);
            }
        }

        /** {@code pbAttackPhaseSwitch} (:50-71) is played by the battle screen. */

        /**
         * {@code pbRegisterMove} (Battle_Action_AttacksPriority:70-79) for one
         * menu slot, then the round. {@code pbFightMenu}'s menu index is the SLOT
         * (Scene_Commands:136), so a slot whose move is out of PP reports
         * "技能已经没有PP了！" (:10) and the menu stays open.
         */
        public boolean chooseMove(int slot) {
            if (result != null || battle.player() == null) return false;
            Battler user = battle.player();
            String refusal = battle.canChooseMove(user.index, slot);
            if (refusal != null) {
                message = refusal.isEmpty() ? null : refusal;
                return false;
            }
            if (!battle.registerMove(user.index, slot)) return false;
            log.clear();
            engineEvents.clear();
            // Battler_UseMove:305 / Battler_ChangeSelf:71 and the rest of the round's events, in order
            // (the opponent's included); Battle_ExpAndMoveLearning:298-307: the exp pot grows with the gain.
            drive(() -> result = battle.step(), this::roundTail);
            return true;
        }

        /**
         * Battle_ExpAndMoveLearning:298-307 (per participant) and :58-61 (the
         * aggregated line): the exp pot grows by max(1, gain/8), capped at
         * {@code EXP_POT_MAX} (Settings:28), and the line is only shown when the
         * player actually holds an EXPPOT.
         */
        private void applyExpPot() {
            expPotMessage = null;
            PbsData pbsData = data.get();
            if (inventory == null || pbsData == null || pbsData.item("EXPPOT") == null
                    || inventory.count("EXPPOT") <= 0) {
                return;
            }
            int before = Math.max(0, trainer.expPot);
            for (Battle.ExpAward award : battle.lastExpAwards) {
                int add = award.potGain > 0 ? award.potGain : Math.max(1, award.expGained / 8);
                if (trainer.expPot + add > EXP_POT_MAX) {
                    add = EXP_POT_MAX - trainer.expPot;
                }
                trainer.expPot = Math.max(0, trainer.expPot + add);
            }
            int gained = trainer.expPot - before;
            if (gained > 0) {
                expPotMessage = "经验储罐累积的经验值增加了" + gained + "点。";
            }
        }

        /**
         * pbGainMoney (Battle_StartAndEnd:394-409): the prize for a won trainer
         * battle = the opponent party's maximum level times moneyEarned
         * (PokeBattle_Trainer:77-81: the trainer type's baseMoney, 30 unset).
         */
        private void awardPrizeMoney() {
            PbsData pbs = data.get();
            int total = 0;
            for (int owner = 0; owner < 3; owner++) {                          // :399 @opponent.each_with_index
                PbsData.TrainerData td = trainerData(owner);
                if (td == null) continue;
                PbsData.TrainerType type = pbs == null || td.type == null ? null : pbs.trainerTypes.get(td.type);
                int moneyEarned = type == null || type.baseMoney <= 0 ? 30 : type.baseMoney;
                int maxLevel = 1;                                              // :400 pbMaxLevelInTeam(1,i)
                for (Battler battler : battle.foeParty()) {
                    if (battler != null && battler.pokemon != null && battler.ownerIndex == owner && battler.level() > maxLevel) {
                        maxLevel = battler.level();
                    }
                }
                total += maxLevel * moneyEarned;
            }
            prizeMoney = total;
            trainer.money += prizeMoney;
        }

        /**
         * {@code pbCanMegaEvolve?(idxBattler)} (Battle_Action_Other:87-99): whether the fight menu
         * offers the Mega Evolution button ({@code @scene.pbFightMenu(idxBattler,pbCanMegaEvolve?)},
         * Battle_Phase_Command:73).
         */
        public boolean canMega() {
            return battle.player() != null && canMega(battle.player().index);
        }

        public boolean canMega(int idxBattler) {
            return result == null && battle.battlerAt(idxBattler) != null && battle.pbCanMegaEvolve(idxBattler);
        }

        /** {@code pbRegisteredMegaEvolution?} (Scene_Commands:110): the button is pressed. */
        public boolean megaRegistered() {
            return battle.player() != null && megaRegistered(battle.player().index);
        }

        public boolean megaRegistered(int idxBattler) {
            return battle.battlerAt(idxBattler) != null && battle.pbRegisteredMegaEvolution(idxBattler);
        }

        /** {@code pbToggleRegisteredMegaEvolution} (Battle_Phase_Command:77). */
        public void toggleMega() {
            if (battle.player() != null) toggleMega(battle.player().index);
        }

        public void toggleMega(int idxBattler) {
            if (battle.battlerAt(idxBattler) != null) battle.pbToggleRegisteredMegaEvolution(idxBattler);
        }

        /** {@code pbUnregisterMegaEvolution} (Battle_Phase_Command:80): the fight menu was cancelled. */
        public void unregisterMega() {
            if (battle.player() != null) unregisterMega(battle.player().index);
        }

        public void unregisterMega(int idxBattler) {
            if (battle.battlerAt(idxBattler) != null) battle.pbUnregisterMegaEvolution(idxBattler);
        }
        /**
         * {@code pbRecallAndReplace}'s round (:256-262) as {@code pbAttackPhaseSwitch}
         * (:50-71) runs it: the recall line, the lineup, the replace line and the
         * send-out. {@code BattleScreen} plays those steps; this only registers
         * the action, exactly like {@code pbRegisterSwitch}
         * (Battle_Action_Switching:122-128) - so a second switch in the same
         * round is impossible ({@code @choices[idxBattler][0]} is no longer
         * {@code :None}, Battle_Phase_Command:207).
         *
         * @return null when the switch was registered, else the refusal line.
         */
        public String registerSwitch(int partyIndex) {
            Battler active = battle.player();
            return registerSwitch(active == null ? 0 : active.index, partyIndex);
        }

        /** {@code pbRegisterSwitch(idxBattler,idxParty)} for one battler of the player. */
        public String registerSwitch(int idxBattler, int partyIndex) {
            if (result != null) {
                return "";
            }
            String refusal = battle.canSwitch(idxBattler, partyIndex);
            if (refusal != null) {
                return refusal;                                    // :123
            }
            if (!battle.registerSwitch(idxBattler, partyIndex)) {
                return "";
            }
            return null;
        }

        /** {@code pbMessageOnRecall} for the battler that is leaving (:264-281). */
        public String recallMessage(int idxBattler) {
            return recallMessage(battle.battlerAt(idxBattler));
        }

        /**
         * {@code pbMessageOnRecall} (Battle_Action_Switching:264-281). The
         * opponent-side branch ({@code {2}回到了{1}身边！}) is used when a
         * trainer recalls a Pokemon without replacing it.
         */
        public String recallMessage(Battler battler) {
            if (battler == null) {
                return "";
            }
            String name = battler.name();
            if (!battle.pbOwnedByPlayer(battler.index)) {          // :265/:277-280 an opponent's or the partner's
                return name + "回到了" + ownerName(battler.index) + "身边！";
            }
            if (battler.hp <= battler.maxHp() / 4) {
                return "做的很好，" + name + "！\n回来吧！";                 // :267
            }
            if (battler.hp <= battler.maxHp() / 2) {
                return "做的相当好了，" + name + "！\n回来吧！";             // :269
            }
            if (battler.turnCount >= 5) {
                return name + "，你做的已经很棒了！\n回来吧！";              // :271
            }
            if (battler.turnCount >= 2) {
                return name + "，回来吧！";                                 // :273
            }
            return "你已经尽力了，" + name + "！\n回来吧！";                 // :275
        }

        /** {@code pbGetOwnerName} (PokeBattle_Battle:266-271). */
        public String ownerName(int idxBattler) {
            String name = battle.pbGetOwnerName(idxBattler);       // :267-270 (the partner and the second opponent included)
            if (name != null) return name;
            if ((idxBattler & 1) == 1) {
                return trainerFullname == null ? "" : trainerFullname;
            }
            return trainer.name;
        }

        /** {@code pbParty(idxBattler)[idxParty].name} (:194 / :287). */
        public String partyName(int idxBattler, int idxParty) {
            Array<Battler> party = battle.partyOf(idxBattler);
            if (party == null || idxParty < 0 || idxParty >= party.size) {
                return "";
            }
            Battler battler = party.get(idxParty);
            return battler == null ? "" : battler.name();
        }

        /**
         * {@code pbMessagesOnReplace} (Battle_Action_Switching:284-305): the
         * line the incoming Pokemon gets, chosen by how healthy the opponent is.
         *
         * <p>The ILLUSION check (:287-289) needs abilities, and the opponent-side
         * branch (:301-303) is {@code "{owner.fullname}派出了\n{X}！"} - used
         * when a trainer replaces a fainted Pokemon.</p>
         */
        public String replaceMessage(int idxBattler, int idxParty) {
            Array<Battler> party = battle.partyOf(idxBattler);
            if (idxParty < 0 || idxParty >= party.size) {
                return "";
            }
            String name = party.get(idxParty).name();              // :286
            if (!battle.pbOwnedByPlayer(idxBattler)) {             // :290/:301 an opponent's or the partner's
                return ownerName(idxBattler) + "派出了\n" + name + "！";
            }
            Battler switcher = battle.battlerAt(idxBattler);
            Battler opposing = switcher == null ? battle.battlerAt(1) : switcher.pbDirectOpposing(false);   // :291 pbDirectOpposing
            if (opposing == null || opposing.fainted() || opposing.hp == opposing.maxHp()) {
                return "加油啊！\n" + name;                                  // :293
            }
            if (opposing.hp >= opposing.maxHp() / 2) {
                return "去吧！\n" + name;                                    // :295
            }
            if (opposing.hp >= opposing.maxHp() / 4) {
                return "不要输给他！\n加油，" + name;                        // :297
            }
            return "对手十分虚弱！\n抓住机会，" + name + "！";               // :299
        }

        /** The old single-argument form for the player's side. */
        String replaceMessage() {
            Battler active = battle.player();                      // :292 pbDirectOpposing
            Battler opposing = battle.foe();
            if (active == null || opposing == null) {
                return "";
            }
            String name = active.name();
            if (opposing.fainted() || opposing.hp == opposing.maxHp()) {
                return "加油啊！\n" + name;                                  // :293
            }
            if (opposing.hp >= opposing.maxHp() / 2) {
                return "去吧！\n" + name;                                    // :295
            }
            if (opposing.hp >= opposing.maxHp() / 4) {
                return "不要输给他！\n加油，" + name;                        // :297
            }
            return "对手十分虚弱！\n抓住机会，" + name + "！";               // :299
        }
        /**
         * {@code pbRun(idxBattler,false)} (Battle_Action_Running:36-157) as the
         * Run command uses it.
         *
         * @return -1 failed / 0 not possible, keep choosing / 1 escaped (:30-35)
         */
        public int escape() {
            return run(false);
        }

        /**
         * {@code pbRun} (Battle_Action_Running:36-157). {@code duringBattle} is
         * true when {@code pbEORSwitch} replaces a fainted Pokemon at the end of
         * a round (:34-35, called from Battle_Action_Switching:222).
         *
         * @return -1 failed fleeing, 0 fleeing was not possible, 1 escaped (:30-35)
         */
        public int run(boolean duringBattle) {
            if (result != null) {
                return 0;
            }
            Battler battler = battle.player();
            if (battler == null) {
                return 0;
            }
            // :38-44 a wild Pokemon's own attempt: the runtime never runs for
            // the opponent's side.
            // :46-66 a trainer battle only offers to surrender in Debug; the
            // project's answer is the refusal line.
            if (trainerBattle) {
                message = "不行！\n绝不能临阵脱逃！";                        // :58
                return 0;                                               // :65
            }
            // :74-77 setBattleRule("cannotRun") refuses without spending the turn.
            if (!battle.canRun()) {
                message = "逃跑失败了！";                                    // :75
                return 0;                                               // :76
            }
            if (!duringBattle) {                                        // :78
                // :79-84 a Ghost type always gets away (NEWEST_BATTLE_MECHANICS).
                if (isGhost(battler)) {
                    message = "安全地逃跑了！";                              // :81
                    result = new BattleResult(BattleResult.Outcome.ESCAPE, battle.turns(), null);
                    endMessage();
                    return 1;                                           // :83
                }
                // :86-105 abilities/items that guarantee escape: not modelled.
                // :107-130 trapping effects and abilities/items: not modelled.
            }
            if (!duringBattle) {
                escapes++;                                              // :135
            }
            // :143-148 unmodified Speed, and a faster runner gets rate 256.
            int playerSpeed = battle.rawSpeed(battler);
            int foeSpeed = 0;
            for (Battler opposing : battle.foeParty()) {                 // :138-141
                if (opposing != null && opposing.pokemon != null) {
                    foeSpeed = Math.max(foeSpeed, battle.rawSpeed(opposing));
                }
            }
            foeSpeed = Math.max(1, foeSpeed);
            int rate = playerSpeed > foeSpeed ? 256
                    : (playerSpeed * 128) / foeSpeed + 30 * escapes;      // :146-147
            if (rate >= 256 || random.nextInt(256) < rate) {              // :149
                message = "安全地逃跑了！";                                  // :151
                result = new BattleResult(BattleResult.Outcome.ESCAPE, battle.turns(), null);
                endMessage();
                return 1;
            }
            message = "无法逃跑！";                                          // :155
            return -1;                                                    // :156
        }

        /** {@code battler.pbHasType?(:GHOST)} (Battle_Action_Running:79). */
        private boolean isGhost(Battler battler) {
            if (battler == null || battler.pokemon == null) return false;
            for (String type : battler.pokemon.types()) {
                if ("GHOST".equals(type)) return true;
            }
            return false;
        }
        public boolean item(String id, int target) {
            Battler user = battle.player();
            return useItem(id, target, user == null ? 0 : user.index, battle.foe(), true);
        }

        /**
         * An item used by one battler of a double battle's command phase ({@code pbItemMenu}, Battle_Phase_Command:106-149).
         * It is applied now (this runtime uses items at command time) and spends the battler's action; the round
         * does not run yet - except for a Poke Ball, which takes all the actions (Battle_Action_UseItem:19-25) and
         * ends the round like in a single battle.
         *
         * @param idxTarget the opposing battler a Poke Ball is thrown at
         */
        public boolean commandItem(String id, int target, int idxBattler, int idxTarget) {
            return useItem(id, target, idxBattler, battle.battlerAt(idxTarget), false);
        }

        private boolean useItem(String id, int target, int idxBattler, Battler ballTarget, boolean endsRound) {
            if (result != null || !inventory.has(id)) return false;
            // Every Poke Ball (items of pocket type 3/4), not only the first four.
            PbsData ballData = data.get();
            PbsData.Item ballItem = ballData == null ? null : ballData.item(id);
            if (ballItem != null && ballItem.isPokeBall()) return ball(id, ballTarget);
            if (target < 0 || target >= battle.playerParty().size) return false;
            Battler battler = battle.playerParty().get(target);
            PbsData.Item item = data.get().item(id);
            if (item == null || item.battleUse <= 0) return false;
            // Source PItem_BattleItemEffects uses 50/200 for these two medicines.
            if ("SUPERPOTION".equals(id) || "HYPERPOTION".equals(id)) {
                if (battler.fainted() || battler.hp >= battler.maxHp()) return false;
                battler.hp = Math.min(battler.maxHp(), battler.hp + ("SUPERPOTION".equals(id) ? 50 : 200));
                battler.syncHp(); inventory.remove(id, 1);
            } else {
                // Do not allow field evolution stones or machines during battle.
                if (item.pocket != 2 && item.pocket != 5) return false;
                if (ItemUse.use(id, battler.pokemon, trainer, inventory, data.get(), -1, true) != ItemUse.Result.USED) return false;
                battler.hp = battler.pokemon.hp;
            }
            // Battle_Action_UseItem:77-80 pbUseItemMessage: "{1}使用了{2}。" with
            // the owner's name (the trainer), not the Pokemon's. The rest of the
            // round (the opponent's move) follows it.
            message = trainer.name + "使用了" + item.name + "。";
            if (endsRound) {
                foeTurn();
            } else {
                battle.markItemUsed(idxBattler);                    // Battle_Action_UseItem:29: the battler's action is spent
                log.clear();
                engineEvents.clear();
                endMessage();
            }
            return true;
        }
        /**
         * Throwing a Poké Ball: {@code PItem_BattleItemEffects:24-53}
         * (CanUseInBattle) and {@code PokeBattle_BattleCommon:68-164}
         * (pbThrowPokeBall). Every line below is one of those two.
         */
        private boolean ball(String id, Battler target) {
            PbsData pbs = data.get();
            PbsData.Item item = pbs == null ? null : pbs.item(id);
            if (item == null || !item.isPokeBall()) {
                return false;
            }
            // :26-29
            if (trainer.party.isFull()
                    && trainer.currentStorage().count() >= Storage.BOXES * Storage.SLOTS) {
                message = "电脑里已经没有空间了！";
                return false;
            }
            // :30-33: a rule, or a wild Boss (battleRank > 1).
            if (disablePokeBalls || (target != null && target.pokemon != null
                    && target.pokemon.battleRank > 1)) {
                message = "无法扔出精灵球！";
                return false;
            }
            // :84-92: a target that is already down has nothing to aim at.
            if (target == null || target.pokemon == null || target.fainted()) {
                message = trainer.name + "扔出了" + item.name + "！" + MESSAGE_BREAK + "但是没有目标……";
                return true;
            }
            String throwLine = trainer.name + "扔出了" + item.name + "！";        // :93-97
            int ballType = BallTypes.ballType(pbs, item.internalName);        // pbGetBallType
            // :100-103: a trainer's Pokemon bats the ball away.
            if (trainerBattle) {
                ballEvents.add(Battle.RoundEvent.message(throwLine, true));     // :94-97 pbDisplayBrief
                ballEvents.add(Battle.RoundEvent.ball(Battle.RoundEvent.Kind.BALL_DEFLECT,
                        ballType, 0, false, target.index));                      // :101 pbThrowAndDeflect
                message = "训练家打飞了球\n不要做小偷！";
                return true;
            }
            // :104-107: $game_switches[60], "不可捕捉的野外对战".
            if (uncatchableSwitch()) {
                message = throwLine + MESSAGE_BREAK + "精灵球被破坏了！\n看来只能战胜它了！";
                return true;
            }
            inventory.remove(id, 1);                                          // Battle_Action_UseItem:35
            CaptureCalculator.Context capture = captureContext(pbs, target);
            int shakes = CaptureCalculator.shakes(capture, item.internalName);  // :111-114
            // :94-97 pbDisplayBrief, then :114 @scene.pbThrow(ball,numShakes,...).
            ballEvents.add(Battle.RoundEvent.message(throwLine, true));
            ballEvents.add(Battle.RoundEvent.ball(Battle.RoundEvent.Kind.BALL_THROW,
                    ballType, shakes, false, target.index));
            if (shakes == 4) {                                                // :129-163
                CaptureCalculator.onCatch(pbs, item.internalName, target.pokemon);
                target.pokemon.ballused = ballType;
                target.pokemon.makeUnmega(pbs);
                target.pokemon.recordFirstMoves();
                // :130-131 pbDisplayBrief, then @scene.pbThrowSuccess; the ball is
                // hidden once the Pokemon is stored (:161).
                ballEvents.add(Battle.RoundEvent.message(
                        "太好了！\n捉到了" + target.pokemon.name + "！", true));
                ballEvents.add(Battle.RoundEvent.ball(Battle.RoundEvent.Kind.BALL_SUCCESS,
                        ballType, 4, false, target.index));
                // :134-138: a capture still awards exp (GAIN_EXP_FOR_CAPTURE,
                // Settings:165).
                battle.awardCaptureExperience();
                // :163 @caughtPokemon: stored (party/box, Pokedex page, lines) by storeCaught() at the end of the battle
                battle.caughtPokemon.add(target.pokemon);
                result = new BattleResult(BattleResult.Outcome.CAUGHT, battle.turns(), target.pokemon);
            } else {
                message = shakeMessage(shakes);                                 // :116-128
                // The outcome line comes before the opponent's move.
                ballEvents.add(Battle.RoundEvent.portMessage(message).asPaused());
                message = null;
                // The opponent still acts this round (Battle_Phase_Attack:105-193).
                foeTurn();
                return true;
            }
            endMessage();
            return true;
        }

        /** PokeBattle_BattleCommon:36-39 pbStorePokemon: the party is full and so is the PC. */
        private boolean uncatchableSwitch() {
            return gameSwitches != null && gameSwitches.test(60);
        }

        /** PokeBattle_BattleCommon:116-128: one line per shake count. */
        private static String shakeMessage(int shakes) {
            switch (shakes) {
                case 0: return "哦不！\n宝可梦逃出来了！";
                case 1: return "啊！\n还以为能抓住呢……";
                case 2: return "好可惜...\n差一点就能成功了";
                default: return "真可惜…\n明明只差一点点。";
            }
        }

        /** Everything pbCaptureCalc (:170-232) asks the battle for. */
        private CaptureCalculator.Context captureContext(PbsData pbs, Battler target) {
            CaptureCalculator.Context capture = new CaptureCalculator.Context();
            capture.pbs = pbs;
            capture.trainer = trainer;
            capture.target = target.pokemon;
            capture.targetHp = target.hp;
            capture.targetMaxHp = target.maxHp();
            capture.targetStatus = target.status == null ? "" : target.status;
            capture.rareness = target.pokemon.species == null
                    ? 0 : target.pokemon.species.rareness;
            capture.turnCount = battle.turns();
            capture.time = battleTime;                    // PField_Battles:181-188
            capture.environment = battleEnvironment;
            capture.safari = false;                       // pbInSafari? - no Safari system yet
            capture.playerMaxLevel = maxPlayerLevel();
            capture.sameSideParty = playerPartyArray();
            capture.random = random;
            return capture;
        }

        /** {@code pbMaxLevelInTeam(0,0)}: the Level Ball's reference level (:162-172). */
        private int maxPlayerLevel() {
            int max = 0;
            for (Battler battler : battle.playerParty()) {
                if (battler != null && battler.pokemon != null && battler.level() > max) {
                    max = battler.level();
                }
            }
            return max;
        }

        private Pokemon[] playerPartyArray() {
            Pokemon[] party = new Pokemon[battle.playerParty().size];
            for (int i = 0; i < party.length; i++) {
                Battler battler = battle.playerParty().get(i);
                party[i] = battler == null ? null : battler.pokemon;
            }
            return party;
        }

        private boolean caughtStored;

        /**
         * {@code pbRecordAndStoreCaughtPokemon} (PokeBattle_BattleCommon:43-63) with {@code pbStorePokemon} (:5-40) and
         * {@code PokeBattle_RealBattlePeer#pbStorePokemon} (166:25-40): the caught Pokemon are recorded in the Pokedex
         * and put in the party, or in the PC box when the party is full. Returns the lines and entry pages the scene
         * shows, in order. The nickname question of :6-12 is commented out in the plugin, so there is none.
         * 登记: {@code pbShadowPokemon} bookkeeping (:54-58), {@code @initialItems} (:18), the storage creator's name
         * ({@code seenStorageCreator}, :22/:33).
         */
        public java.util.List<CaughtStep> storeCaught() {
            java.util.List<CaughtStep> steps = new java.util.ArrayList<>();
            if (caughtStored) return steps;
            caughtStored = true;
            PbsData pbs = data.get();
            for (Pokemon pkmn : battle.caughtPokemon) {
                trainer.registerSeen(pkmn);                                          // :45 pbSeenForm
                String species = pkmn.species == null ? null : pkmn.species.internalName;
                if (species != null && !trainer.owned.contains(species)) {           // :47 hasOwned?
                    trainer.owned.add(species);                                      // :48 setOwned
                    if (trainer.pokedex) {                                           // :49
                        steps.add(new CaughtStep(pkmn.name + "的数据被记录在图鉴里了。", null));   // :50
                        steps.add(new CaughtStep(null, pkmn.species));               // :51 pbShowPokedex
                    }
                }
                if (trainer.party.size() < 6) {                                      // 166:26
                    trainer.party.add(pkmn);                                         // 166:27
                    steps.add(new CaughtStep(pkmn.name + "加入了队伍。", null));          // 129:17
                    continue;
                }
                Storage.heal(pkmn);                                                  // 166:30 pkmn.heal
                Storage storage = trainer.currentStorage();
                int currentBox = storage.currentBox;                                 // :14
                int stored = storage.pbStoreCaught(pkmn);                            // 166:32
                if (stored < 0) {
                    steps.add(new CaughtStep("Can't catch any more...", null));       // 166:36
                    continue;
                }
                String currentName = storage.box(currentBox).name;                   // :23
                String boxName = storage.box(stored).name;                           // :24
                if (stored != currentBox) {                                          // :25
                    steps.add(new CaughtStep("寄存系统的盒子" + currentName + "已经满了。", null));   // :29
                    steps.add(new CaughtStep(pkmn.name + "被传送到盒子\"" + boxName + "了。\".", null));   // :31
                } else {
                    steps.add(new CaughtStep(pkmn.name + "被传送到寄存系统里。", null));    // :36
                    steps.add(new CaughtStep("存储到" + boxName + "了。", null));          // :38
                }
            }
            battle.caughtPokemon.clear();                                            // :62
            return steps;
        }

        private final java.util.List<String> lossLines = new java.util.ArrayList<>();

        /**
         * {@code pbLoseMoney} (Battle_StartAndEnd:424-442): the player's highest level times the badge multiplier,
         * at most what the player holds; null when nothing is lost ({@code NO_MONEY_LOSS} = switch 33).
         */
        private String loseMoney() {
            if (gameSwitches != null && gameSwitches.test(33)) return null;            // :426
            // The plugin takes pbMaxLevelInTeam(0,0) * [8,16,24,36,48,64,80,100,120][badges] (131_Battle_StartAndEnd:427-431).
            // The project asked for a gentler loss instead: 5 % of the money held (never more than it holds).
            int money = trainer.money * 5 / 100;
            trainer.money -= money;                                                    // :433
            if (money <= 0) return null;                                               // :435
            String text = String.format(java.util.Locale.ROOT, "%,d", money);
            return trainerBattle ? "你给了获胜者$" + text + "……" : "你不小心掉了$" + text + "……";   // :436-440
        }

        private void endMessage() {
            if (result != null) {
                switch (result.outcome) {
                    case WIN:
                        // Battle_StartAndEnd:452-469: a won trainer battle pays
                        // pbMaxLevelInTeam(1,i) * moneyEarned (:394-409). The
                        // victory lines themselves are played by the battle screen.
                        if (trainerBattle) awardPrizeMoney();
                        break;
                    case LOSS:
                        // Battle_StartAndEnd:479-495: every line is a pbDisplayPaused.
                        lossLines.add("所有的宝可梦都倒下了……");                        // :480
                        if (trainerBattle) {                                           // :481-491
                            String[] names = {trainerFullname, trainerFullname2, trainerFullname3};
                            java.util.List<String> foes = new java.util.ArrayList<>();
                            for (String name : names) if (name != null) foes.add(name);
                            if (foes.size() == 1) lossLines.add("你输给了\n" + foes.get(0) + "！");
                            else if (foes.size() == 2) lossLines.add("你输给了\n" + foes.get(0) + "和" + foes.get(1) + "！");
                            else if (foes.size() == 3) lossLines.add("你输给了\n" + foes.get(0) + "、" + foes.get(1) + "和" + foes.get(2) + "！");
                        }
                        String lost = loseMoney();                                     // :494 pbLoseMoney
                        if (lost != null) lossLines.add(lost);
                        if (!canLose) lossLines.add("你眼前一黑！");                    // :495
                        break;
                    // The capture line is pbThrowPokeBall's own (:130) and is set
                    // by ball() before this runs.
                    case CAUGHT: break;
                    // The escape lines are pbRun's own (:151/:155) and are set by
                    // escape() before this runs.
                    case ESCAPE: break;
                    default: break;
                }
            }
            // The action in the order the plugin shows it: the player's action
            // lines, the engine's events (the moves' - Battler_UseMove:305 and
            // the rest of pbProcessMoveHit - then pbEndOfRoundPhase's, which runs
            // before pbBattleLoop breaks, Battle_StartAndEnd:381-386) and last
            // whatever this call set (the "所有的宝可梦都倒下了……" line of
            // Battle_StartAndEnd:480 in pbEndOfBattle, the capture line of
            // PokeBattle_BattleCommon:130).
            events.clear();
            for (String line : log) {
                events.add(Battle.RoundEvent.portMessage(line));
            }
            events.addAll(ballEvents);
            events.addAll(engineEvents);
            if (message != null && !message.isEmpty()) {
                events.add(Battle.RoundEvent.portMessage(message));
            }
            for (String line : lossLines) {
                events.add(Battle.RoundEvent.portMessage(line).asPaused());
            }
            lossLines.clear();
            // message keeps every line's text (MESSAGE_BREAK-joined) for callers
            // that only read text.
            StringBuilder out = new StringBuilder();
            for (Battle.RoundEvent event : events) {
                if (event.kind != Battle.RoundEvent.Kind.MESSAGE) continue;
                if (out.length() > 0) out.append(MESSAGE_BREAK);
                out.append(event.text);
            }
            log.clear();
            engineEvents.clear();
            ballEvents.clear();
            message = out.toString();
            // The screen's earlier contract kept: when this port's own line
            // (log / closing message) is the last thing shown, it waits for the
            // player like before. The engine's lines keep their own timing.
            if (events.size > 0 && events.peek().kind == Battle.RoundEvent.Kind.MESSAGE
                    && events.peek().fromPort) {
                events.set(events.size - 1, events.peek().asPaused());
            }
        }
    }
}
