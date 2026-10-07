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
    private boolean leaguePass;
    public InteractiveBattlePort(TrainerState trainer, Inventory inventory, Supplier<PbsData> data, Runnable whiteout, Random random) {
        this.trainer = trainer; this.inventory = inventory; this.data = data; this.whiteout = whiteout; this.random = random;
    }
    /** $PokemonSystem.battle_rule: false = classic Mega, true = ZA mode. */
    public void zaMode(boolean value) { this.zaMode = value; }
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
    public BattleResult freeWildBattle(Pokemon foe) {
        if (foe != null) start(Array.with(foe), false, null);
        return null;
    }
    public BattleResult trainerBattle(PbsData.TrainerData opponent) {
        if (opponent == null || data.get() == null) return null;
        Array<Pokemon> foes = new Array<>();
        for (PbsData.TrainerPokemon member : opponent.party) {
            Pokemon p = factory().trainerPokemon(member, data.get()); if (p != null) foes.add(p);
        }
        start(foes, true, opponent); return null;
    }
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

    private void start(Array<Pokemon> foes, boolean trainerBattle, PbsData.TrainerData opponent) {
        if (pending() || foes.isEmpty() || trainer.party.firstAble() == null) return;
        lastResult = null;
        session = new Session(foes, trainerBattle, opponent);
    }
    public BattleResult lastResult() { return lastResult; }
    public void setCanLose(boolean value) { this.canLose = value; }
    /** {@code setBattleRule("canRun")} / pbWildBattle's {@code canRun}. */
    public void setCanRun(boolean value) { this.canRun = value; }
    /** {@code setBattleRule("disablePokeballs")} applies to the next battle. */
    public void setDisablePokeBalls(boolean value) { this.disablePokeBalls = value; }
    /** {@code $game_switches} reads switch 60 ("不可捕捉的野外对战"). */
    public void setSwitchSource(java.util.function.IntPredicate switches) {
        this.gameSwitches = switches;
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
        BattleResult result = session.result; session = null;
        lastResult = result;
        for (Pokemon p : trainer.party.members()) trainer.registerOwned(p);
        // PField_Battles:619-658 pbAfterBattle runs before Events.onEndBattle
        // (:656), which is where the white-out lives (:701-706).
        BattleAftermath.pbAfterBattle(trainer, data.get(), result, canLose);
        boolean whiteOut = BattleAftermath.onEndBattle(trainer, data.get(), random, result, canLose);
        if (whiteOut && whiteout != null) whiteout.run();
        canLose = false;
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

    public final class Session {
        public final Battle battle;
        public final boolean trainerBattle;
        /** The trainers.txt row for a trainer battle (null for a wild battle). */
        public final PbsData.TrainerData trainerData;
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
        Session(Array<Pokemon> foes, boolean trainerBattle, PbsData.TrainerData trainerData) {
            this.trainerBattle = trainerBattle;
            this.trainerData = trainerData;
            battle = new Battle(data.get(), random, (user, foe, moves) -> move);
            battle.zaMode = zaMode;
            battle.setCanRun(canRun);       // PField_Battles:101 / :360
            battle.switchStyle = switchStyle();   // PField_Battles:111-112
            // PField_Battles:114-115 battle.showAnims = ($PokemonSystem.battlescene==0),
            // then the "anims"/"noanims" rule.
            battle.showAnims = battleAnimsRule != null ? battleAnimsRule : battlescene == 0;
            battle.trainerBattle = trainerBattle;
            battle.playerName = trainer.name;
            battle.levelLockOn = levelLockOn;
            battle.leaguePass = leaguePass;
            if (gameSwitches != null) battle.gameSwitches = gameSwitches;   // $game_switches (Battler_UseMove_SuccessChecks:157,165)
            battle.badges = trainer.badges;
            battle.setCryPlayer(cryPlayer);       // Battler_UseMove_SuccessChecks:318-319
            for (Pokemon p : trainer.party.members()) battle.addPlayer(p);
            for (Pokemon p : foes) { battle.addFoe(p); trainer.registerSeen(p); }
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
                // Battle_StartAndEnd:466: the opponent's LoseText, "..." when empty.
                endSpeech = trainerData.loseText == null || trainerData.loseText.isEmpty()
                        ? "..." : trainerData.loseText;
            }
        }
        /**
         * {@code pbCanShowFightMenu?} (Battle_Phase_Command:43-55): {@code false}
         * when no slot can be chosen, which makes {@code pbFightMenu:68} take the
         * {@code pbAutoChooseMove} path instead of showing the menu. The Encore
         * branch (:46) needs PBEffects.
         */
        public boolean canShowFightMenu() {
            Battler user = battle.player();
            return user != null && user.hasUsableMove();
        }

        /**
         * {@code pbAutoChooseMove} (Battle_Action_AttacksPriority:36-67): the move
         * the player uses when the fight menu cannot be shown, i.e. Struggle
         * ({@code pbFightMenu:68} calls this when {@code pbCanShowFightMenu?} is
         * false because no slot can be chosen).
         */
        public boolean autoChooseMove() {
            if (result != null || battle.player() == null) {
                return false;
            }
            Battler user = battle.player();
            if (user.fainted()) {                                  // :38-41
                battle.clearChoice(user.index);
                return true;
            }
            // :43-58 the Encore branch needs PBEffects[Encore] and
            // pbEncoredMoveIndex; this runtime has neither.
            if (user.hasUsableMove()) {
                return false;                                      // pbCanShowFightMenu? was true
            }
            // :60-62 pbDisplayPaused("{1}没有招式可使用了！")
            log.clear();
            engineEvents.clear();
            addLog(user.name() + "没有招式可使用了！");
            // :63-66 @choices = [:UseMove, -1, @struggle, -1]; the engine's
            // pickMove already falls back to Struggle for such a battler.
            result = battle.step();
            engineEvents.addAll(battle.roundEvents);
            applyExpPot();
            message = null;
            endMessage();
            return true;
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
            log.clear();
            engineEvents.clear();
            battle.pbPursuitOnSwitch(battle.player().index);
            engineEvents.addAll(battle.roundEvents);
            result = battle.result();
            applyExpPot();
            message = null;
            if (result != null) {
                endMessage();
            }
            return engineEvents.size > 0 || result != null;
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
            result = battle.foeTurn();
            engineEvents.addAll(battle.roundEvents);
            message = null;
            endMessage();
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
            result = battle.step();
            // Battler_UseMove:305 / Battler_ChangeSelf:71 and the rest of the
            // round's events, in order (the opponent's included).
            engineEvents.addAll(battle.roundEvents);
            // Battle_ExpAndMoveLearning:298-307: the exp pot grows with the gain.
            applyExpPot();
            message = null;
            endMessage();
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
            PbsData.TrainerType type = pbs == null || trainerData == null || trainerData.type == null
                    ? null : pbs.trainerTypes.get(trainerData.type);
            int moneyEarned = type == null || type.baseMoney <= 0 ? 30 : type.baseMoney;
            int maxLevel = 1;
            for (Battler battler : battle.foeParty()) {
                if (battler != null && battler.pokemon != null && battler.level() > maxLevel) {
                    maxLevel = battler.level();
                }
            }
            prizeMoney = maxLevel * moneyEarned;
            trainer.money += prizeMoney;
        }

        /** Mega Evolution / Primal Reversion, before choosing the move. */
        public boolean mega() {
            if (result != null || battle.player() == null) return false;
            if (!battle.megaEvolve(battle.player())) {
                message = "现在无法超级进化。";
                return false;
            }
            message = battle.player().name() + "超级进化为 " + battle.player().pokemon.megaName() + "！";
            return true;
        }

        /** Whether the player can currently Mega Evolve (for the menu). */
        public boolean canMega() {
            return result == null && battle.canMegaEvolve(battle.player());
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
            if (result != null) {
                return "";
            }
            Battler active = battle.player();
            int idxBattler = active == null ? 0 : active.index;
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
            if (battler.foe) {                                     // :277-280
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
            if ((idxBattler & 1) == 1) {                           // :301
                return ownerName(idxBattler) + "派出了\n" + name + "！";
            }
            Battler opposing = battle.battlerAt(1);                // :291 pbDirectOpposing
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
            if (result != null || !inventory.has(id)) return false;
            // Every Poke Ball (items of pocket type 3/4), not only the first four.
            PbsData ballData = data.get();
            PbsData.Item ballItem = ballData == null ? null : ballData.item(id);
            if (ballItem != null && ballItem.isPokeBall()) return ball(id);
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
            foeTurn();
            return true;
        }
        /**
         * Throwing a Poké Ball: {@code PItem_BattleItemEffects:24-53}
         * (CanUseInBattle) and {@code PokeBattle_BattleCommon:68-164}
         * (pbThrowPokeBall). Every line below is one of those two.
         */
        private boolean ball(String id) {
            PbsData pbs = data.get();
            PbsData.Item item = pbs == null ? null : pbs.item(id);
            if (item == null || !item.isPokeBall()) {
                return false;
            }
            Battler target = battle.foe();
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
                trainer.addToParty(target.pokemon);
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
                        // Battle_StartAndEnd:480 "所有的宝可梦都倒下了……".
                        message = "所有的宝可梦都倒下了……";
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
            if (battle.zaMode) {
                // ZA模式 pbEndOfRoundPhase: show the player's super energy.
                String suffix = "  超级能量：" + battle.zaEnergy(0) + "/" + Battle.ZA_MAX_ENERGY;
                message += suffix;
                // The same suffix on the last line the screen shows.
                for (int i = events.size - 1; i >= 0; i--) {
                    Battle.RoundEvent last = events.get(i);
                    if (last.kind == Battle.RoundEvent.Kind.MESSAGE) {
                        events.set(i, last.withText(last.text + suffix));
                        break;
                    }
                }
            }
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
