package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import pokemon.runtime.pokemon.PbsData;

import java.util.ArrayList;
import java.util.List;

/**
 * Battle_Phase_EndOfRound:5-858 ({@code pbEORCountDown*}, {@code pbEORWeather},
 * {@code pbEORTerrain}, {@code pbEndOfRoundPhase}, {@code pbCheckNeutralizingGas}),
 * PokeBattle_Battle:741-769 {@code pbStartTerrain} and the layers the 场地 section
 * puts on top of them (虫惑场地 :22-46, 冰冷场地 :261-306, the 捕获网 wrapper
 * :556-585, the terrain-seed batching :633-678), and PokeBattle_Clauses:39-48
 * (the sudden-death rule).
 *
 * <p>登记 (what this runtime plays elsewhere / does not model):
 * <ul>
 * <li>{@code @scene.pbDamageAnimation(b)} / {@code pbBeginEndOfRoundPhase}: scene calls.</li>
 * <li>:273-274 {@code @battle.pbCommonAnimation("SeaOfFire")}: {@code @battle} is nil inside
 *     {@code PokeBattle_Battle}, so the plugin raises NoMethodError when a Sea of Fire is up. The
 *     call is skipped here instead of crashing the battle.</li>
 * <li>:748 {@code pbEORShiftDistantBattlers}: only moves distant battlers when the battle is not a single battle.</li>
 * <li>:345-353 Hyper Mode damage and :706-715 Hyper Mode waking: Shadow Pokemon are not modelled
 *     ({@link Battler#inHyperMode()} is false).</li>
 * <li>:751-798 {@code lastAttacker}/{@code lastFoeAttacker}, the reset of the effects below.</li>
 * <li>:221-257 Future Sight: the user, when it left the field, is the party's {@link Battler}
 *     (this runtime keeps party members as Battlers) with its index set to the user's index while
 *     the move runs, in place of {@code PokeBattle_Battler.new + pbInitDummyPokemon}.</li>
 * </ul></p>
 */
public final class BattleEndOfRoundPhase {

    private BattleEndOfRoundPhase() {
    }

    // ------------------------------------------------------------------
    // :5-30 counters
    // ------------------------------------------------------------------

    /** {@code pbEORCountDownSideEffect(side,effect,msg)} (:13-18). */
    public static void pbEORCountDownSideEffect(Battle battle, int side, int effect, String msg) {
        BattleSide s = battle.field.sides[side];
        if (s.effects.intVal(effect) > 0) {                                           // :14
            s.effects.decrement(effect);                                              // :15
            if (s.effects.intVal(effect) == 0) battle.display(msg);                   // :16
        }
    }

    /** {@code pbEORCountDownFieldEffect(effect,msg)} (:20-30), with the {@code pbPriority(true)} loop of :26. */
    public static void pbEORCountDownFieldEffect(Battle battle, int effect, String msg) {
        if (battle.field.effects.intVal(effect) > 0) {                                // :21
            battle.field.effects.decrement(effect);                                   // :22
            if (battle.field.effects.intVal(effect) == 0) {                           // :23
                battle.display(msg);                                                  // :24
                if (effect == PBEffects.Field.MagicRoom) {                            // :25
                    for (Battler b : battle.pbPriority(true)) b.pbItemTerrainStatBoostCheck();   // :26
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // :35-110 weather
    // ------------------------------------------------------------------

    /** {@code pbEORWeather(priority)} (:35-110). */
    public static void pbEORWeather(Battle battle, Array<Battler> priority) {
        BattleField field = battle.field;
        // NOTE: Primordial weather doesn't need to be checked here
        // Count down weather duration
        if (field.weatherDuration > 0) field.weatherDuration -= 1;                    // :39
        // Weather wears off
        if (field.weatherDuration == 0) {                                             // :41
            switch (field.weather) {                                                  // :42
                case PBWeather.Sun: battle.display("阳光减弱了！"); break;               // :43-44
                case PBWeather.Rain: battle.display("雨停了！"); break;                  // :45-46
                case PBWeather.Sandstorm: battle.display("沙尘暴平息了！"); break;        // :47-48
                case PBWeather.Hail: battle.display("冰雹停止了。"); break;              // :49-50
                case PBWeather.Snow: battle.display("不再下雪了。"); break;              // :51-52
                case PBWeather.ShadowSky: battle.display("暗影天空消失了。"); break;      // :53-54
                default: break;
            }
            field.weather = PBWeather.None;                                           // :56
            // Check for form changes caused by the weather changing
            for (Battler b : battle.eachBattler()) b.pbCheckFormOnWeatherChange();    // :58
            // Start up the default weather
            if (field.defaultWeather != PBWeather.None) {                             // :60
                battle.pbStartWeather(null, field.defaultWeather);
            }
            if (field.weather == PBWeather.None) return;                              // :61
        }
        // Weather continues
        String anim = PBWeather.animationName(field.weather);
        if (anim != null) battle.commonAnimation(anim, null);                         // :64
        switch (field.weather) {                                                      // :65
            case PBWeather.Sandstorm: battle.display("沙暴正在肆虐！"); break;           // :68
            case PBWeather.Hail: battle.display("冰雹正在砸落！"); break;                // :69
            case PBWeather.ShadowSky: battle.display("天空依旧灰暗！"); break;           // :73
            default: break;
        }
        // Effects due to weather
        int curWeather = battle.pbWeather();                                          // :76
        for (Battler b : priority) {                                                  // :77
            // Weather-related abilities
            if (b.abilityActive()) {                                                  // :79
                BattleHandlers.triggerEORWeatherAbility(b.ability, curWeather, b, battle);   // :81
                if (b.fainted()) b.pbFaint();                                         // :82
            }
            // Weather damage
            switch (curWeather) {                                                     // :86
                case PBWeather.Sandstorm:                                             // :87
                    if (!b.takesSandstormDamage()) continue;                          // :88
                    weatherDamage(battle, b, "被沙暴伤害了！");                          // :89-93
                    break;
                case PBWeather.Hail:                                                  // :94
                    if (!b.takesHailDamage()) continue;                               // :95
                    weatherDamage(battle, b, "被冰雹伤害了！");                          // :96-100
                    break;
                case PBWeather.ShadowSky:                                             // :101
                    if (!b.takesShadowSkyDamage()) continue;                          // :102
                    weatherDamage(battle, b, "被黑暗气象伤害了！");                       // :103-107
                    break;
                default:
                    break;
            }
        }
    }

    private static void weatherDamage(Battle battle, Battler b, String text) {
        battle.display(b.pbThis() + text);                                            // :89
        // @scene.pbDamageAnimation(b)                                                // :90 (scene)
        b.pbReduceHP(b.maxHp() / 16, false, true, true);                              // :91
        b.pbItemHPHealCheck(0, false);                                                // :92
        if (b.fainted()) b.pbFaint();                                                 // :93
    }

    // ------------------------------------------------------------------
    // :115-146 terrain, with the 场地 section's wrappers
    // ------------------------------------------------------------------

    /** {@code pbEORTerrain} as the 场地 section leaves it: Cold (:274-305) around Bug Lure (:34-45) around :115-146. */
    public static void pbEORTerrain(Battle battle) {
        BattleField field = battle.field;
        // ---- 场地:274-305 (cold_terrain_pbEORTerrain) ----
        boolean wasCold = field.terrain == PBBattleTerrains.Cold;                     // 场地:275
        if (wasCold) {                                                                // 场地:278
            for (Battler b : battle.eachBattler()) {                                  // 场地:279
                if (b.fainted()) continue;                                            // 场地:280
                if (b.airborne()) continue;                                           // 场地:281
                if (b.pbHasType("ICE")) continue;                                     // 场地:282
                if (!b.takesIndirectDamage(false)) continue;                          // 场地:283
                battle.display(b.pbThis() + "受到了刺骨寒气的伤害！");                    // 场地:285
                int damage = b.maxHp() / 16;                                          // 场地:288
                if (damage < 1) damage = 1;                                           // 场地:289
                b.pbReduceHP(damage, false, true, true);                              // 场地:290
                b.pbItemHPHealCheck(0, false);                                        // 场地:291
                if (b.fainted()) b.pbFaint();                                         // 场地:292
            }
        }
        boolean endingCold = wasCold && field.terrainDuration == 1;                   // 场地:296
        if (endingCold) battle.display("场上的刺骨寒气消散了！");                       // 场地:297
        // ---- 场地:34-45 (bug_lure_pbEORTerrain) ----
        boolean wasBugLure = field.terrain == PBBattleTerrains.BugLure;               // 场地:35
        boolean endingBug = wasBugLure && field.terrainDuration == 1;                 // 场地:36
        if (endingBug) battle.display("笼罩场地的虫群消散了！");                        // 场地:38
        pbEORTerrainBase(battle);                                                     // 场地:39
        if (wasBugLure && !endingBug && field.terrain == PBBattleTerrains.BugLure) {  // 场地:41
            battle.display("虫群仍在场上飞舞！");                                       // 场地:43
        }
        if (wasCold && !endingCold && field.terrain == PBBattleTerrains.Cold) {       // 场地:301
            battle.display("刺骨的寒气仍笼罩着场地！");                                  // 场地:303
        }
    }

    /** {@code pbEORTerrain} (:115-147). */
    private static void pbEORTerrainBase(Battle battle) {
        BattleField field = battle.field;
        // Count down terrain duration
        if (field.terrainDuration > 0) field.terrainDuration -= 1;                    // :117
        // Terrain wears off
        if (field.terrain != PBBattleTerrains.None && field.terrainDuration == 0) {   // :119
            switch (field.terrain) {                                                  // :120
                case PBBattleTerrains.Electric: battle.display("场上的电流消失了！"); break;    // :121-122
                case PBBattleTerrains.Grassy: battle.display("四周的青草枯萎了！"); break;      // :123-124
                case PBBattleTerrains.Misty: battle.display("四周的薄雾消散了！"); break;       // :125-126
                case PBBattleTerrains.Psychic: battle.display("场地恢复原样了！"); break;       // :127-128
                default: break;
            }
            field.terrain = PBBattleTerrains.None;                                    // :130
            for (Battler b : battle.eachBattler()) b.pbAbilityOnTerrainChange(false);  // :131
            // Start up the default terrain
            if (field.defaultTerrain != PBBattleTerrains.None) {                      // :133
                pbStartTerrain(battle, null, field.defaultTerrain, false);            // :134
                for (Battler b : battle.eachBattler()) b.pbAbilityOnTerrainChange(false);   // :135
            }
            if (field.terrain == PBBattleTerrains.None) return;                       // :137
        }
        // Terrain continues
        String anim = PBBattleTerrains.animationName(field.terrain);
        if (anim != null) battle.commonAnimation(anim, null);                         // :140
        switch (field.terrain) {                                                      // :141
            case PBBattleTerrains.Electric: battle.display("电流在场上肆虐！"); break;      // :142
            case PBBattleTerrains.Grassy: battle.display("青草覆盖了四周！"); break;        // :143
            case PBBattleTerrains.Misty: battle.display("薄雾笼罩着四周！"); break;         // :144
            case PBBattleTerrains.Psychic: battle.display("周围变得极为瑰异！"); break;     // :145
            default: break;
        }
    }

    // ------------------------------------------------------------------
    // PokeBattle_Battle:741-769 pbStartTerrain and the 场地 wrappers
    // ------------------------------------------------------------------

    /** {@code pbStartTerrain(user,newTerrain,fixedDuration=true)} as the 场地 section leaves it (:646-677 around :263-271 around :24-31 around :741-769). */
    public static void pbStartTerrain(Battle battle, Battler user, int newTerrain, boolean fixedDuration) {
        // ---- 场地:646-677 (custom_seed_order_pbStartTerrain) ----
        boolean batchSeeds = battle.field.terrain != newTerrain                        // 场地:647-648
                && (newTerrain == PBBattleTerrains.BugLure || newTerrain == PBBattleTerrains.Cold);   // 场地:649-650
        if (!batchSeeds) {                                                            // 场地:653
            pbStartTerrainCold(battle, user, newTerrain, fixedDuration);              // 场地:654
            return;
        }
        battle.customTerrainSeedBatch = true;                                         // 场地:659
        battle.customTerrainSeedSymbiosisQueue.clear();                               // 场地:660
        pbStartTerrainCold(battle, user, newTerrain, fixedDuration);                  // 场地:662
        battle.customTerrainSeedBatch = false;                                        // 场地:666
        List<Battler> queue = new ArrayList<>(battle.customTerrainSeedSymbiosisQueue);   // 场地:667
        battle.customTerrainSeedSymbiosisQueue.clear();                               // 场地:668
        // 所有场地种子检查完成后，再依次触发共生
        for (Battler b : queue) {                                                     // 场地:671
            if (b == null || b.fainted()) continue;                                   // 场地:672
            b.pbSymbiosis();                                                          // 场地:673
        }
    }

    private static void pbStartTerrainCold(Battle battle, Battler user, int newTerrain, boolean fixedDuration) {
        int oldTerrain = battle.field.terrain;                                        // 场地:264
        pbStartTerrainBug(battle, user, newTerrain, fixedDuration);                   // 场地:265
        if (oldTerrain != newTerrain && battle.field.terrain == PBBattleTerrains.Cold) {   // 场地:267
            battle.display("刺骨的寒气笼罩了场地！");                                    // 场地:269
        }
    }

    private static void pbStartTerrainBug(Battle battle, Battler user, int newTerrain, boolean fixedDuration) {
        int oldTerrain = battle.field.terrain;                                        // 场地:25
        pbStartTerrainBase(battle, user, newTerrain, fixedDuration);                  // 场地:26
        if (oldTerrain != newTerrain && battle.field.terrain == PBBattleTerrains.BugLure) {   // 场地:27
            battle.display("无数虫群笼罩了场地！");                                       // 场地:29
        }
    }

    private static void pbStartTerrainBase(Battle battle, Battler user, int newTerrain, boolean fixedDuration) {
        BattleField field = battle.field;
        if (field.terrain == newTerrain) return;                                      // :742
        field.terrain = newTerrain;                                                   // :743
        int duration = fixedDuration ? 5 : -1;                                        // :744
        if (duration > 0 && user != null && user.itemActive()) {                      // :745
            duration = BattleHandlers.triggerTerrainExtenderItem(user.item, newTerrain, duration, user, battle);   // :746
        }
        field.terrainDuration = duration;                                             // :749
        String anim = PBBattleTerrains.animationName(field.terrain);
        if (anim != null) battle.commonAnimation(anim, null);                         // :750
        if (user != null) battle.hideAbilitySplash(user);                             // :751
        switch (field.terrain) {                                                      // :752
            case PBBattleTerrains.Electric: battle.display("电流在场上肆虐！"); break;      // :754
            case PBBattleTerrains.Grassy: battle.display("青草覆盖了四周！"); break;        // :756
            case PBBattleTerrains.Misty: battle.display("薄雾笼罩着四周！"); break;         // :758
            case PBBattleTerrains.Psychic: battle.display("周围变得极为瑰异！"); break;     // :760
            default: break;
        }
        battle.pbCalculatePriority(true, null);                                       // :762 if DYNAMIC_PRIORITY
        // Check for terrain seeds that boost stats in a terrain
        for (Battler b : battle.eachBattler()) {                                      // :764
            b.pbAbilityOnTerrainChange(false);                                        // :765
            b.pbCheckFormOnTerrainChange();                                           // :766
            b.pbItemTerrainStatBoostCheck();                                          // :767
        }
    }

    // ------------------------------------------------------------------
    // :211-827 pbEndOfRoundPhase
    // ------------------------------------------------------------------

    /** {@code pbEndOfRoundPhase} (:211-827) with the 捕获网 (场地:558-584) and sudden-death (Clauses:39-48) wrappers. */
    public static void pbEndOfRoundPhase(Battle battle) {
        pbEndOfRoundPhaseBase(battle);                                                // Clauses:40 / 场地:559
        // ---- 场地:560-583 capture net ----
        if (battle.decision != 0) return;                                             // 场地:560
        for (Battler b : battle.pbPriority(true)) {                                   // 场地:562
            if (b == null || b.fainted()) continue;                                   // 场地:563
            if (!b.effects.truthy(PBEffects.Battler.CaptureNet)) continue;            // 场地:564
            if (b.effects.intVal(PBEffects.Battler.MeanLook) < 0) continue;          // 场地:565
            int userIndex = b.effects.intVal(PBEffects.Battler.CaptureNetUser);       // 场地:567
            Battler netUser = battle.battlerAt(userIndex);                            // 场地:568
            // 捕获网的使用者离场后，束缚及降速效果结束
            if (netUser == null || netUser.fainted()
                    || b.effects.intVal(PBEffects.Battler.MeanLook) != userIndex) {   // 场地:571-572
                b.effects.set(PBEffects.Battler.CaptureNet, false);                   // 场地:573
                b.effects.set(PBEffects.Battler.CaptureNetUser, -1);                  // 场地:574
                continue;                                                             // 场地:575
            }
            if (b.pbCanLowerStatStage(PBStats.SPEED, netUser)) {                      // 场地:578
                b.pbLowerStatStage(PBStats.SPEED, 1, netUser, true, false, true);     // 场地:579-581
            }
        }
        // ---- PokeBattle_Clauses:41-47 ----
        if (battle.rules.get("suddendeath") != null && battle.decision == 0) {        // Clauses:41
            int p1able = battle.pbAbleCount(0);                                       // Clauses:42
            int p2able = battle.pbAbleCount(1);                                       // Clauses:43
            if (p1able > p2able) battle.decision = 1;                                 // Clauses:44
            else if (p1able < p2able) battle.decision = 2;                            // Clauses:45
        }
    }

    private static boolean chose(Battle battle, int idx, String action) {
        return action.equals(battle.choices(idx)[0]);
    }

    private static void pbEndOfRoundPhaseBase(Battle battle) {
        battle.endOfRound = true;                                                     // :214
        // @scene.pbBeginEndOfRoundPhase                                              // :215
        battle.pbCalculatePriority();                                                 // :216 recalculate speeds
        Array<Battler> priority = battle.pbPriority(true);                            // :217 in order of fastest -> slowest speeds only
        // Weather
        pbEORWeather(battle, priority);                                               // :219
        // Future Sight/Doom Desire
        for (int idxPos = 0; idxPos < battle.field.positions.length; idxPos++) {      // :221
            BattlePosition pos = battle.field.positions[idxPos];
            if (pos == null || pos.effects.intVal(PBEffects.Position.FutureSightCounter) == 0) continue;   // :222
            pos.effects.decrement(PBEffects.Position.FutureSightCounter);             // :223
            if (pos.effects.intVal(PBEffects.Position.FutureSightCounter) > 0) continue;   // :224
            Battler target = battle.battlerAt(idxPos);
            if (target == null || target.fainted()) continue;                         // :225 No target
            int userIdx = pos.effects.intVal(PBEffects.Position.FutureSightUserIndex);
            int userParty = pos.effects.intVal(PBEffects.Position.FutureSightUserPartyIndex);
            Battler moveUser = null;                                                  // :226
            for (Battler b : battle.eachBattler()) {                                  // :227
                if (b.opposes(userIdx)) continue;                                     // :228
                if (b.pokemonIndex != userParty) continue;                            // :229
                moveUser = b;                                                         // :230
                break;                                                                // :231
            }
            if (moveUser != null && moveUser.index == idxPos) continue;               // :233 Target is the user
            boolean dummy = false;
            int dummyOldIndex = 0;
            if (moveUser == null) {                                                   // :234 User isn't in battle, get it from the party
                Array<Battler> party = battle.partyOf(userIdx);                       // :235
                Battler pkmn = party != null && userParty >= 0 && userParty < party.size ? party.get(userParty) : null;   // :236
                if (pkmn != null && !pkmn.fainted() && !pkmn.pokemon.egg) {           // :237
                    moveUser = pkmn;                                                  // :238-239 (see class javadoc)
                    dummy = true;
                    dummyOldIndex = pkmn.index;
                    pkmn.index = userIdx;
                }
            }
            if (moveUser == null) continue;                                           // :242 User is fainted
            int moveId = pos.effects.intVal(PBEffects.Position.FutureSightMove);      // :243
            PbsData.Move moveData = battle.pbs().moveById(moveId);
            battle.display(target.pbThis() + "承受了" + (moveData == null ? "" : moveData.name) + "的攻击！");   // :244
            // NOTE: Future Sight failing against the target here doesn't count towards Stomping Tantrum.
            boolean userLastMoveFailed = moveUser.lastMoveFailed;                     // :247
            battle.futureSight = true;                                                // :248
            moveUser.pbUseMoveSimple(moveData == null ? null : moveData.internalName, idxPos);   // :249
            battle.futureSight = false;                                               // :250
            moveUser.lastMoveFailed = userLastMoveFailed;                             // :251
            if (dummy) moveUser.index = dummyOldIndex;
            if (target.fainted()) target.pbFaint();                                   // :252
            pos.effects.set(PBEffects.Position.FutureSightCounter, 0);                // :253
            pos.effects.set(PBEffects.Position.FutureSightMove, 0);                   // :254
            pos.effects.set(PBEffects.Position.FutureSightUserIndex, -1);             // :255
            pos.effects.set(PBEffects.Position.FutureSightUserPartyIndex, -1);        // :256
        }
        // Wish
        for (int idxPos = 0; idxPos < battle.field.positions.length; idxPos++) {      // :259
            BattlePosition pos = battle.field.positions[idxPos];
            if (pos == null || pos.effects.intVal(PBEffects.Position.Wish) == 0) continue;   // :260
            pos.effects.decrement(PBEffects.Position.Wish);                           // :261
            if (pos.effects.intVal(PBEffects.Position.Wish) > 0) continue;            // :262
            Battler b = battle.battlerAt(idxPos);
            if (b == null || !b.canHeal()) continue;                                  // :263
            String wishMaker = battle.pbThisEx(idxPos, pos.effects.intVal(PBEffects.Position.WishMaker));   // :264
            b.pbRecoverHP(pos.effects.intVal(PBEffects.Position.WishAmount));         // :265
            battle.display(wishMaker + "的祈愿成真了！");                                // :266
        }
        // Sea of Fire damage (Fire Pledge + Grass Pledge combination)
        int curWeather = battle.pbWeather();                                          // :269
        for (int side = 0; side < 2; side++) {                                        // :270
            if (battle.field.sides[side].effects.intVal(PBEffects.Side.SeaOfFire) == 0) continue;   // :271
            if (curWeather == PBWeather.Rain || curWeather == PBWeather.HeavyRain) continue;        // :272
            // :273-274 @battle.pbCommonAnimation("SeaOfFire"/"SeaOfFireOpp"): see class javadoc
            for (Battler b : priority) {                                              // :275
                if (b.opposes(side)) continue;                                        // :276
                if (!b.takesIndirectDamage(false) || b.pbHasType("FIRE")) continue;   // :277
                int oldHP = b.hp;                                                     // :278
                // @scene.pbDamageAnimation(b)                                        // :279
                b.pbReduceHP(b.maxHp() / 8, false, true, true);                       // :280
                battle.display(b.pbThis() + "被火海伤害了!");                            // :281
                b.pbItemHPHealCheck(0, false);                                        // :282
                b.pbAbilitiesOnDamageTaken(oldHP, -1);                                // :283
                if (b.fainted()) b.pbFaint();                                         // :284
            }
        }
        // Status-curing effects/abilities and HP-healing items
        BattleEndOfRound.pbEORHealing(battle, priority);                              // :287-326 (BattleEndOfRound)
        // Leech Seed
        for (Battler b : priority) {                                                  // :328
            if (b.effects.intVal(PBEffects.Battler.LeechSeed) < 0) continue;          // :329
            if (!b.takesIndirectDamage(false)) continue;                              // :330
            Battler recipient = battle.battlerAt(b.effects.intVal(PBEffects.Battler.LeechSeed));   // :331
            if (recipient == null || recipient.fainted()) continue;                   // :332
            int oldHP = b.hp;                                                         // :333
            int oldHPRecipient = recipient.hp;                                        // :334
            Array<Battler> to = new Array<>();
            to.add(b);
            battle.commonAnimation("LeechSeed", recipient, to);                       // :335
            int hpLoss = b.pbReduceHP(b.maxHp() / 8);                                 // :336
            recipient.pbRecoverHPFromDrain(hpLoss, b, b.pbThis() + "被寄生种子吸取了营养！");   // :337-338
            if (recipient.hp < oldHPRecipient) recipient.pbAbilitiesOnDamageTaken(oldHPRecipient, -1);   // :339
            b.pbItemHPHealCheck(0, false);                                            // :340
            b.pbAbilitiesOnDamageTaken(oldHP, -1);                                    // :341
            if (b.fainted()) b.pbFaint();                                             // :342
            if (recipient.fainted()) recipient.pbFaint();                             // :343
        }
        // :345-353 Damage from Hyper Mode (Shadow Pokemon): see class javadoc
        for (Battler b : priority) {
            if (!b.inHyperMode() || !chose(battle, b.index, ":UseMove")) continue;    // :347
            int hpLoss = Battle.NEWEST_BATTLE_MECHANICS ? b.maxHp() / 16 : b.maxHp() / 8;   // :348
            b.pbReduceHP(hpLoss, false, true, true);                                  // :350
            battle.display("因为处于暴走状态\n所以伤害了" + b.pbThis(true) + "！");        // :351
            if (b.fainted()) b.pbFaint();                                             // :352
        }
        // Pokemon Legends: Arceus - Stone Axe Splinters
        for (Battler b : priority) {                                                  // :356
            if (b.effects.intVal(PBEffects.Battler.StoneAxe) < 0) continue;           // :357
            b.effects.decrement(PBEffects.Battler.StoneAxe);                          // :358
            if (!b.takesIndirectDamage(false)) continue;                              // :359
            battle.commonAnimation("StoneAxe", b);                                    // :360
            int oldHP = b.hp;                                                         // :361
            b.pbReduceHP(b.pokemon.battleRank > 2 ? b.maxHp() / 40 : b.maxHp() / 8);   // :362-366
            battle.display(b.pbThis() + "因为锋利的碎片\n而损失了HP！");                    // :367
            b.pbItemHPHealCheck(0, false);                                            // :368
            b.pbAbilitiesOnDamageTaken(oldHP, -1);                                    // :369
            if (b.fainted()) b.pbFaint();                                             // :370
        }
        // Ceaseless Edge Splinters
        for (Battler b : priority) {                                                  // :373
            if (b.fainted()) continue;                                                // :374
            if (b.effects.intVal(PBEffects.Battler.CeaselessEdge) < 0) continue;      // :375
            b.effects.decrement(PBEffects.Battler.CeaselessEdge);                     // :376
            if (!b.takesIndirectDamage(false)) continue;                              // :377
            battle.commonAnimation("CeaselessEdge", b);                               // :378
            int oldHP = b.hp;                                                         // :379
            b.pbReduceHP(b.pokemon.battleRank > 2 ? b.maxHp() / 40 : b.maxHp() / 8);   // :380-384
            battle.display(b.pbThis() + "因为锋利的碎片\n而损失了HP！");                    // :385
            b.pbItemHPHealCheck(0, false);                                            // :386
            b.pbAbilitiesOnDamageTaken(oldHP, -1);                                    // :387
            if (b.fainted()) b.pbFaint();                                             // :388
        }
        // Damage from poisoning
        for (Battler b : priority) {                                                  // :391
            if (b.fainted()) continue;                                                // :392
            if (!"POISON".equals(b.status)) continue;                                 // :393
            if (b.statusCount > 0) {                                                  // :394
                b.effects.add(PBEffects.Battler.Toxic, 1);                            // :395
                if (b.effects.intVal(PBEffects.Battler.Toxic) > 15) b.effects.set(PBEffects.Battler.Toxic, 15);   // :396
            }
            if (b.hasActiveAbility("POISONHEAL")) {                                   // :398
                if (b.canHeal()) {                                                    // :399
                    battle.commonAnimation("Poison", b);                              // :400
                    battle.showAbilitySplash(b);                                      // :401
                    b.pbRecoverHP(b.maxHp() / 8);                                     // :402
                    if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {               // :403
                        battle.display(b.pbThis() + "的HP回复了。");                    // :404
                    } else {
                        battle.display(b.pbThis() + "的" + b.abilityName() + "回复了HP。");   // :406
                    }
                    battle.hideAbilitySplash(b);                                      // :408
                }
            } else if (b.takesIndirectDamage(false)) {                                // :410
                final int oldHP = b.hp;                                               // :411
                final int dmg;
                if (b.pokemon.battleRank > 2) {                                       // :412
                    dmg = (b.statusCount == 0) ? b.maxHp() / 40 : b.maxHp() * b.effects.intVal(PBEffects.Battler.Toxic) / 160;   // :413
                } else {
                    dmg = (b.statusCount == 0) ? b.maxHp() / 8 : b.maxHp() * b.effects.intVal(PBEffects.Battler.Toxic) / 16;     // :415
                }
                final Battler bb = b;
                b.pbContinueStatus(() -> bb.pbReduceHP(dmg, false, true, true));      // :417
                b.pbItemHPHealCheck(0, false);                                        // :418
                b.pbAbilitiesOnDamageTaken(oldHP, -1);                                // :419
                if (b.fainted()) b.pbFaint();                                         // :420
            }
        }
        // Damage from burn
        for (Battler b : priority) {                                                  // :424
            if (!"BURN".equals(b.status) || !b.takesIndirectDamage(false)) continue;   // :425
            final int oldHP = b.hp;                                                   // :426
            int dmg;
            if (b.pokemon.battleRank > 2) {                                           // :427
                dmg = b.maxHp() / 40;                                                 // :428
            } else {
                dmg = Battle.NEWEST_BATTLE_MECHANICS ? b.maxHp() / 16 : b.maxHp() / 8;   // :430
            }
            if (b.hasActiveAbility("HEATPROOF")) dmg = Math.round(dmg / 2.0f);        // :432
            final int fdmg = dmg;
            final Battler bb = b;
            b.pbContinueStatus(() -> bb.pbReduceHP(fdmg, false, true, true));         // :433
            b.pbItemHPHealCheck(0, false);                                            // :434
            b.pbAbilitiesOnDamageTaken(oldHP, -1);                                    // :435
            if (b.fainted()) b.pbFaint();                                             // :436
        }
        // Damage from frostbite (Pokemon Legends: Arceus)
        for (Battler b : priority) {                                                  // :439
            if (!"FROSTBITE".equals(b.status) || !b.takesIndirectDamage(false)) continue;   // :440
            final int oldHP = b.hp;                                                   // :441
            int dmg;
            if (b.pokemon.battleRank > 2) {                                           // :442
                dmg = b.maxHp() / 40;                                                 // :443
            } else {
                dmg = Battle.NEWEST_BATTLE_MECHANICS ? b.maxHp() / 16 : b.maxHp() / 8;   // :445
            }
            if (b.hasActiveAbility("MAGMAARMOR")) dmg = Math.round(dmg / 2.0f);       // :447
            final int fdmg = dmg;
            final Battler bb = b;
            b.pbContinueStatus(() -> bb.pbReduceHP(fdmg, false, true, true));         // :448
            b.pbItemHPHealCheck(0, false);                                            // :449
            b.pbAbilitiesOnDamageTaken(oldHP, -1);                                    // :450
            if (b.fainted()) b.pbFaint();                                             // :451
        }
        // Damage from sleep (Nightmare)
        for (Battler b : priority) {                                                  // :454
            if (!b.asleep()) b.effects.set(PBEffects.Battler.Nightmare, false);       // :455
            if (!b.effects.truthy(PBEffects.Battler.Nightmare) || !b.takesIndirectDamage(false)) continue;   // :456
            int oldHP = b.hp;                                                         // :457
            b.pbReduceHP(b.pokemon.battleRank > 2 ? b.maxHp() / 40 : b.maxHp() / 4);  // :458-462
            battle.display(b.pbThis() + "被囚禁在恶梦之中！");                            // :463
            b.pbItemHPHealCheck(0, false);                                            // :464
            b.pbAbilitiesOnDamageTaken(oldHP, -1);                                    // :465
            if (b.fainted()) b.pbFaint();                                             // :466
        }
        // Curse
        for (Battler b : priority) {                                                  // :469
            if (!b.effects.truthy(PBEffects.Battler.Curse) || !b.takesIndirectDamage(false)) continue;   // :470
            int oldHP = b.hp;                                                         // :471
            b.pbReduceHP(b.pokemon.battleRank > 2 ? b.maxHp() / 40 : b.maxHp() / 4);  // :472-476
            battle.display(b.pbThis() + "遭受着诅咒的折磨！");                           // :477
            b.pbItemHPHealCheck(0, false);                                            // :478
            b.pbAbilitiesOnDamageTaken(oldHP, -1);                                    // :479
            if (b.fainted()) b.pbFaint();                                             // :480
        }
        // Octolock
        for (Battler b : priority) {                                                  // :483
            if (b.fainted() || !b.effects.truthy(PBEffects.Battler.Octolock)) continue;   // :484
            Battler octouser = battle.battlerAt(b.effects.intVal(PBEffects.Battler.OctolockUser));   // :485
            if (b.pbCanLowerStatStage(PBStats.DEFENSE, octouser)) {                   // :486
                b.pbLowerStatStage(PBStats.DEFENSE, 1, octouser, true, false, true);  // :487
            }
            if (b.pbCanLowerStatStage(PBStats.SPDEF, octouser)) {                     // :489
                b.pbLowerStatStage(PBStats.SPDEF, 1, octouser, true, false, true);    // :490
            }
        }
        // Trapping attacks (Bind/Clamp/Fire Spin/Magma Storm/Sand Tomb/Whirlpool/Wrap)
        for (Battler b : priority) {                                                  // :494
            if (b.fainted() || b.effects.intVal(PBEffects.Battler.Trapping) == 0) continue;   // :495
            b.effects.decrement(PBEffects.Battler.Trapping);                          // :496
            PbsData.Move tm = battle.pbs().moveById(b.effects.intVal(PBEffects.Battler.TrappingMove));
            String moveName = tm == null ? "" : tm.name;                              // :497
            if (b.effects.intVal(PBEffects.Battler.Trapping) == 0) {                  // :498
                battle.display(b.pbThis() + "从" + moveName + "中脱身了！");              // :499
            } else {
                String trappingMove = tm == null ? "" : tm.internalName;              // :501
                String anim;
                switch (trappingMove) {                                               // :502-511
                    case "BIND": anim = "Bind"; break;
                    case "CLAMP": anim = "Clamp"; break;
                    case "FIRESPIN": anim = "FireSpin"; break;
                    case "MAGMASTORM": anim = "MagmaStorm"; break;
                    case "SANDTOMB": anim = "SandTomb"; break;
                    case "WRAP": anim = "Wrap"; break;
                    case "INFESTATION": anim = "Infestation"; break;
                    case "SNAPTRAP": anim = "SnapTrap"; break;
                    case "THUNDERCAGE": anim = "ThunderCage"; break;
                    default: anim = "Wrap"; break;
                }
                battle.commonAnimation(anim, b);
                if (b.takesIndirectDamage(false)) {                                   // :513
                    int hpLoss = Battle.NEWEST_BATTLE_MECHANICS ? b.maxHp() / 8 : b.maxHp() / 16;   // :514
                    Battler trapper = battle.battlerAt(b.effects.intVal(PBEffects.Battler.TrappingUser));
                    if (trapper != null && trapper.hasActiveItem("BINDINGBAND")) {    // :515
                        hpLoss = Battle.NEWEST_BATTLE_MECHANICS ? b.maxHp() / 6 : b.maxHp() / 8;   // :516
                    }
                    // @scene.pbDamageAnimation(b)                                    // :518
                    if (b.pokemon.battleRank > 2) hpLoss /= 5;                        // :519
                    b.pbReduceHP(hpLoss, false, true, true);                          // :520
                    battle.display(b.pbThis() + "受到了来自" + moveName + "的伤害！");       // :521
                    b.pbItemHPHealCheck(0, false);                                    // :522
                    // NOTE: No need to call pbAbilitiesOnDamageTaken as b can't switch out.
                    if (b.fainted()) b.pbFaint();                                     // :524
                }
            }
        }
        // Taunt
        BattleEndOfRound.pbEORCountDownBattlerEffect(priority, PBEffects.Battler.Taunt, battler ->   // :529
                battle.display(battler.pbThis() + "的挑衅无效了！"));                      // :530
        // Encore
        for (Battler b : priority) {                                                  // :533
            if (b.fainted() || b.effects.intVal(PBEffects.Battler.Encore) == 0) continue;   // :534
            int idxEncoreMove = b.pbEncoredMoveIndex();                               // :535
            if (idxEncoreMove >= 0) {                                                 // :536
                b.effects.decrement(PBEffects.Battler.Encore);                        // :537
                if (b.effects.intVal(PBEffects.Battler.Encore) == 0 || b.moveSlotPp(idxEncoreMove) == 0) {   // :538
                    b.effects.set(PBEffects.Battler.Encore, 0);                       // :539
                    battle.display(b.pbThis() + "的再来一次状态解除了！");                  // :540
                }
            } else {
                b.effects.set(PBEffects.Battler.Encore, 0);                           // :544
                b.effects.set(PBEffects.Battler.EncoreMove, 0);                       // :545
            }
        }
        // Disable/Cursed Body
        BattleEndOfRound.pbEORCountDownBattlerEffect(priority, PBEffects.Battler.Disable, battler -> {   // :549
            battler.effects.set(PBEffects.Battler.DisableMove, 0);                    // :550
            battle.display(battler.pbThis() + "不再被封印了！");                          // :551
        });
        // Magnet Rise
        BattleEndOfRound.pbEORCountDownBattlerEffect(priority, PBEffects.Battler.MagnetRise, battler ->   // :554
                battle.display(battler.pbThis() + "的电磁力消失了！"));                    // :555
        // Telekinesis
        BattleEndOfRound.pbEORCountDownBattlerEffect(priority, PBEffects.Battler.Telekinesis, battler ->   // :558
                battle.display(battler.pbThis() + "从念力中逃脱了！"));                    // :559
        // Heal Block
        BattleEndOfRound.pbEORCountDownBattlerEffect(priority, PBEffects.Battler.HealBlock, battler ->   // :562
                battle.display(battler.pbThis() + "的回复封印解除了！"));                  // :563
        // Embargo
        BattleEndOfRound.pbEORCountDownBattlerEffect(priority, PBEffects.Battler.Embargo, battler -> {   // :566
            battle.display(battler.pbThis() + "可以使用道具了！");                         // :567
            battler.pbItemTerrainStatBoostCheck();                                    // :568
        });
        // Yawn
        BattleEndOfRound.pbEORCountDownBattlerEffect(priority, PBEffects.Battler.Yawn, battler -> {   // :571
            if (battler.pbCanSleepYawn()) battler.pbSleep();                          // :572-574
        });
        // Perish Song
        List<Integer> perishSongUsers = new ArrayList<>();                            // :578
        for (Battler b : priority) {                                                  // :579
            if (b.fainted() || b.effects.intVal(PBEffects.Battler.PerishSong) == 0) continue;   // :580
            b.effects.decrement(PBEffects.Battler.PerishSong);                        // :581
            battle.display("距离" + b.pbThis() + "灭亡还剩下" + b.effects.intVal(PBEffects.Battler.PerishSong) + "回合！");   // :582
            if (b.effects.intVal(PBEffects.Battler.PerishSong) == 0) {                // :583
                perishSongUsers.add(b.effects.intVal(PBEffects.Battler.PerishSongUser));   // :584
                b.pbReduceHP(b.hp);                                                   // :585
            }
            b.pbItemHPHealCheck(0, false);                                            // :587
            if (b.fainted()) b.pbFaint();                                             // :588
        }
        if (perishSongUsers.size() > 0) {                                             // :590
            // If all remaining Pokemon fainted by a Perish Song triggered by a single side
            int opposing = 0;
            int allied = 0;
            for (int idx : perishSongUsers) {                                         // :592-593 opposes?(idxBattler) with idx0 = 0
                if (battle.battlerAt(0) != null && battle.battlerAt(0).opposes(idx)) opposing++; else allied++;
            }
            if (opposing == perishSongUsers.size() || allied == perishSongUsers.size()) {   // :592-593
                battle.pbJudgeCheckpoint(battle.battlerAt(perishSongUsers.get(0)), null);   // :594
            }
        }
        // 珠泪哀歌
        for (Battler b : priority) {                                                  // :598
            if (!b.takesIndirectDamage(false) || b.effects.truthy(PBEffects.Battler.Curse)
                    || !b.effects.truthy(PBEffects.Battler.Tearalament)) continue;    // :599-600
            int oldHP = b.hp;                                                         // :601
            b.pbReduceHP(b.pokemon.battleRank > 2 ? b.maxHp() / 80 : b.maxHp() / 8);  // :602-606
            battle.display(b.pbThis() + "被恐惧与哀伤折磨！");                            // :607
            b.pbItemHPHealCheck(0, false);                                            // :608
            b.pbAbilitiesOnDamageTaken(oldHP, -1);                                    // :609
            if (b.fainted()) b.pbFaint();                                             // :610
        }
        // 盐腌
        for (Battler battler : priority) {                                            // :613
            if (!battler.effects.truthy(PBEffects.Battler.SaltCure) || !battler.takesIndirectDamage(false)) continue;   // :614
            battle.commonAnimation("SaltCure", battler);                              // :615
            int fraction = (battler.pbHasType("STEEL") || battler.pbHasType("WATER")) ? 4 : 8;   // :616
            if (battler.pokemon.battleRank > 2) fraction *= 5;                        // :617
            final Battler bb = battler;
            battler.pbTakeEffectDamage(battler.maxHp() / fraction, hpLost ->          // :618
                    battle.display(bb.pbThis() + "被盐腌伤害了！"));                       // :619
        }
        // 咒钉
        for (Battler b : priority) {                                                  // :624
            if (b.fainted() || b.effects.intVal(PBEffects.Battler.CurseNail) == 0) continue;   // :625
            b.effects.decrement(PBEffects.Battler.CurseNail);                         // :626
            battle.display("距离" + b.pbThis() + "的咒钉消失\n还剩下" + b.effects.intVal(PBEffects.Battler.CurseNail) + "回合！");   // :627
        }
        // 地魔之剑/海魔之雨回合
        for (Battler b : priority) {                                                  // :631
            if (b.fainted() || b.effects.intVal(PBEffects.Battler.PoisonVulnerability) == 0) continue;   // :632
            b.effects.decrement(PBEffects.Battler.PoisonVulnerability);               // :633
        }
        for (Battler b : priority) {                                                  // :635
            if (b.fainted() || b.effects.intVal(PBEffects.Battler.IceVulnerability) == 0) continue;   // :636
            b.effects.decrement(PBEffects.Battler.IceVulnerability);                  // :637
        }
        // 复活
        for (Battler b : priority) {                                                  // :640
            if (b.fainted()) continue;                                                // :641
            b.setCanRebirth(false);                                                   // :642
        }
        // Check for end of battle
        if (battle.decision > 0) {                                                    // :645
            battle.pbGainExp();                                                       // :646
            return;                                                                   // :647
        }
        for (int side = 0; side < 2; side++) {                                        // :649
            Battler sb = battle.battlerAt(side);
            // Reflect
            pbEORCountDownSideEffect(battle, side, PBEffects.Side.Reflect, sb.pbTeam(false) + "的反射盾消失了！");   // :651-652
            // Light Screen
            pbEORCountDownSideEffect(battle, side, PBEffects.Side.LightScreen, sb.pbTeam(false) + "的光墙消失了！");   // :654-655
            // Safeguard
            pbEORCountDownSideEffect(battle, side, PBEffects.Side.Safeguard, sb.pbTeam(false) + "不再受神秘守护的保护了！");   // :657-658
            // Mist
            pbEORCountDownSideEffect(battle, side, PBEffects.Side.Mist, sb.pbTeam(false) + "不再受白雾的保护了！");   // :660-661
            // Tailwind
            pbEORCountDownSideEffect(battle, side, PBEffects.Side.Tailwind, sb.pbTeam(false) + "的顺风停止了！");   // :663-664
            // Lucky Chant
            pbEORCountDownSideEffect(battle, side, PBEffects.Side.LuckyChant, sb.pbTeam(false) + "的幸运咒语消失了！");   // :666-667
            // Pledge Rainbow
            pbEORCountDownSideEffect(battle, side, PBEffects.Side.Rainbow, sb.pbTeam(true) + "一方的彩虹消失了！");   // :669-670
            // Pledge Sea of Fire
            pbEORCountDownSideEffect(battle, side, PBEffects.Side.SeaOfFire, sb.pbTeam(true) + "周围的火海消失了！");   // :672-673
            // Pledge Swamp
            pbEORCountDownSideEffect(battle, side, PBEffects.Side.Swamp, sb.pbTeam(true) + "周围的沼泽消失了！");   // :675-676
            // Aurora Veil
            pbEORCountDownSideEffect(battle, side, PBEffects.Side.AuroraVeil, sb.pbTeam(true) + "的极光幕消失了！");   // :678-679
            // 妄之歌
            pbEORCountDownSideEffect(battle, side, PBEffects.Side.DelusionSong, "妖精的歌声停止了！");   // :681-682
        }
        // Trick Room .. Magic Room
        BattleEndOfRound.pbEORFieldCountdowns(battle);                                // :684-701 (the six field countdowns, in order)
        // End of terrains
        pbEORTerrain(battle);                                                         // :703
        for (Battler b : priority) {                                                  // :704
            if (b.fainted()) continue;                                                // :705
            // :706-715 Hyper Mode (Shadow Pokemon): see class javadoc
            if (b.inHyperMode()) {                                                    // :707
                battle.display(b.pbThis() + "处于暴走状态！");                            // :713 (the 10% wake-up branch needs Pokemon#hypermode)
            }
            // Uproar
            if (b.effects.intVal(PBEffects.Battler.Uproar) > 0) {                     // :717
                b.effects.decrement(PBEffects.Battler.Uproar);                        // :718
                if (b.effects.intVal(PBEffects.Battler.Uproar) == 0) {                // :719
                    battle.display(b.pbThis() + "冷静下来了。");                         // :720
                } else {
                    battle.display(b.pbThis() + "正在制造噪音！");                       // :722
                }
            }
            // Slow Start's end message
            if (b.effects.intVal(PBEffects.Battler.SlowStart) > 0) {                  // :726
                b.effects.decrement(PBEffects.Battler.SlowStart);                     // :727
                if (b.effects.intVal(PBEffects.Battler.SlowStart) == 0) {             // :728
                    battle.display(b.pbThis() + "聚集了所有的力量！");                    // :729
                }
            }
            // Bad Dreams, Moody, Speed Boost
            if (b.abilityActive()) BattleHandlers.triggerEOREffectAbility(b.ability, b, battle);   // :733
            // Flame Orb, Sticky Barb, Toxic Orb
            if (b.itemActive()) BattleHandlers.triggerEOREffectItem(b.item, b, battle);            // :735
            // Harvest, Pickup
            if (b.abilityActive()) BattleHandlers.triggerEORGainItemAbility(b.ability, b, battle);   // :737
        }
        battle.pbGainExp();                                                           // :739
        if (battle.decision > 0) return;                                              // :740
        // Form checks
        for (Battler b : priority) b.pbCheckForm(true);                               // :742
        battle.pbEORSwitch(false);                                                    // :744
        if (battle.decision > 0) return;                                              // :745
        battle.pbEORShiftDistantBattlers();                                           // :748
        // Try to make Trace work, check for end of primordial weather
        for (Battler b : priority) b.pbContinualAbilityChecks();                      // :750
        // Reset/count down battler-specific effects (no messages)
        for (Battler b : battle.eachBattler()) {                                      // :752
            EffectMap e = b.effects;
            e.set(PBEffects.Battler.BanefulBunker, false);                            // :753
            if (e.intVal(PBEffects.Battler.Charge) > 0) e.decrement(PBEffects.Battler.Charge);   // :754
            e.set(PBEffects.Battler.Counter, -1);                                     // :755
            e.set(PBEffects.Battler.CounterTarget, -1);                               // :756
            e.set(PBEffects.Battler.Electrify, false);                                // :757
            e.set(PBEffects.Battler.Endure, false);                                   // :758
            e.set(PBEffects.Battler.FirstPledge, 0);                                  // :759
            e.set(PBEffects.Battler.Flinch, false);                                   // :760
            e.set(PBEffects.Battler.FocusPunch, false);                               // :761
            e.set(PBEffects.Battler.FollowMe, 0);                                     // :762
            e.set(PBEffects.Battler.HelpingHand, false);                              // :763
            if (e.intVal(PBEffects.Battler.HyperBeam) > 0) e.decrement(PBEffects.Battler.HyperBeam);   // :764
            e.set(PBEffects.Battler.KingsShield, false);                              // :765
            if (e.intVal(PBEffects.Battler.LaserFocus) > 0) e.decrement(PBEffects.Battler.LaserFocus);   // :766
            if (e.intVal(PBEffects.Battler.LockOn) > 0) {                             // :767 Also Mind Reader
                e.decrement(PBEffects.Battler.LockOn);                                // :768
                if (e.intVal(PBEffects.Battler.LockOn) == 0) e.set(PBEffects.Battler.LockOnPos, -1);   // :769
            }
            e.set(PBEffects.Battler.MagicBounce, false);                              // :771
            e.set(PBEffects.Battler.MagicCoat, false);                                // :772
            e.set(PBEffects.Battler.MirrorCoat, -1);                                  // :773
            e.set(PBEffects.Battler.MirrorCoatTarget, -1);                            // :774
            e.set(PBEffects.Battler.Powder, false);                                   // :775
            e.set(PBEffects.Battler.Prankster, false);                                // :776
            e.set(PBEffects.Battler.PriorityAbility, false);                          // :777
            e.set(PBEffects.Battler.PriorityItem, false);                             // :778
            e.set(PBEffects.Battler.Protect, false);                                  // :779
            e.set(PBEffects.Battler.RagePowder, false);                               // :780
            e.set(PBEffects.Battler.Roost, false);                                    // :781
            e.set(PBEffects.Battler.Snatch, 0);                                       // :782
            e.set(PBEffects.Battler.SpikyShield, false);                              // :783
            e.set(PBEffects.Battler.Spotlight, 0);                                    // :784
            if (e.intVal(PBEffects.Battler.ThroatChop) > 0) e.decrement(PBEffects.Battler.ThroatChop);   // :785
            e.set(PBEffects.Battler.BurningJealousy, false);                          // :786
            e.set(PBEffects.Battler.LashOut, false);                                  // :787
            e.set(PBEffects.Battler.Obstruct, false);                                 // :788
            b.lastHPLost = 0;                                                         // :789
            b.lastHPLostFromFoe = 0;                                                  // :790
            b.tookDamage = false;                                                     // :791
            b.tookPhysicalHit = false;                                                // :792
            b.statsRaisedThisRound = false;                                           // :793
            b.statsLoweredThisRound = false;                                          // :794
            b.lastRoundMoveFailed = b.lastMoveFailed;                                 // :795
            b.lastAttacker.clear();                                                   // :796
            b.lastFoeAttacker.clear();                                                // :797
        }
        // Reset/count down side-specific effects (no messages)
        for (int side = 0; side < 2; side++) {                                        // :800
            EffectMap e = battle.field.sides[side].effects;
            e.set(PBEffects.Side.CraftyShield, false);                                // :801
            if (!e.truthy(PBEffects.Side.EchoedVoiceUsed)) {                          // :802
                e.set(PBEffects.Side.EchoedVoiceCounter, 0);                          // :803
            }
            e.set(PBEffects.Side.EchoedVoiceUsed, false);                             // :805
            e.set(PBEffects.Side.MatBlock, false);                                    // :806
            e.set(PBEffects.Side.QuickGuard, false);                                  // :807
            e.set(PBEffects.Side.Round, false);                                       // :808
            e.set(PBEffects.Side.WideGuard, false);                                   // :809
        }
        // Reset/count down field-specific effects (no messages)
        EffectMap fe = battle.field.effects;
        fe.set(PBEffects.Field.IonDeluge, false);                                     // :812
        if (fe.intVal(PBEffects.Field.FairyLock) > 0) fe.decrement(PBEffects.Field.FairyLock);   // :813
        fe.set(PBEffects.Field.FusionBolt, false);                                    // :814
        fe.set(PBEffects.Field.FusionFlare, false);                                   // :815
        fe.set(PBEffects.Battler.BurningBulwark, false);                                // :816 (the plugin uses the one PBEffects::BurningBulwark index for the field too)
        for (Battler battler : battle.allBattlers()) {                                // :818
            battler.effects.set(PBEffects.Battler.BurningBulwark, false);             // :819
            if (battler.effects.intVal(PBEffects.Battler.Charge) > 0) battler.effects.add(PBEffects.Battler.Charge, 1);   // :820
            if (battler.effects.intVal(PBEffects.Battler.GlaiveRush) > 0) battler.effects.decrement(PBEffects.Battler.GlaiveRush);   // :821
        }
        // Neutralizing Gas
        pbCheckNeutralizingGas(battle, null);                                         // :824
        battle.endOfRound = false;                                                    // :826
    }

    /** {@code pbCheckNeutralizingGas(battler=nil)} (:830-857). */
    public static void pbCheckNeutralizingGas(Battle battle, Battler battler) {
        if (!battle.field.effects.truthy(PBEffects.Field.NeutralizingGas)) return;    // :834
        // :835-838 as written: the three conditions are joined with ||, so for any real
        // ability at least one of them holds and `return` happens whenever battler is given.
        if (battler != null && (!"NEUTRALIZINGGAS".equals(battler.ability)
                || !"STEELDYNASTY".equals(battler.ability)
                || battler.effects.truthy(PBEffects.Battler.GastroAcid))) return;      // :835-838
        boolean hasabil = false;                                                      // :839
        for (Battler b : battle.eachBattler()) {                                      // :840
            if (b == null || b.fainted()) continue;                                   // :841
            if (battler != null && b.index == battler.index) continue;                // :842
            // neutralizing gas can be blocked with gastro acid, ending the effect.
            if (("NEUTRALIZINGGAS".equals(b.ability) || "STEELDYNASTY".equals(b.ability))
                    && !b.effects.truthy(PBEffects.Battler.GastroAcid)) {             // :845-847
                hasabil = true;                                                       // :848
                break;
            }
        }
        if (!hasabil) {                                                               // :851
            battle.field.effects.set(PBEffects.Field.NeutralizingGas, false);         // :852
            for (Battler b : battle.pbPriority(true)) {                               // :853
                if (battler != null && b.index == battler.index) continue;            // :854
                b.pbEffectsOnSwitchIn(false);                                         // :855
            }
        }
    }
}
