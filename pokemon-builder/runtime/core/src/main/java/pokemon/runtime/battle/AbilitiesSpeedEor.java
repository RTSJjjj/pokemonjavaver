package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;

import pokemon.runtime.battle.movefx.MoveEffectRegistry;
import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 4 / L1': the {@code BattleHandlers_Abilities.rb} registrations assigned
 * to this file by {@code stage4-l1-handler-roster.md} §1.2 - <b>47 entries</b>,
 * plus the 1 {@code .copy} line of roster §5 that lands here (§6: 47 + 1 copy
 * target = 48 registered symbols).
 *
 * <p>Groups (roster §1.3): {@code SpeedCalcAbility} 10 + 1 copy,
 * {@code WeightCalcAbility} 3, {@code PriorityChangeAbility} 5,
 * {@code PriorityBracketChangeAbility} 3, {@code PriorityBracketUseAbility} 1,
 * {@code RunFromBattleAbility} 1, {@code EORWeatherAbility} 4,
 * {@code EORHealingAbility} 3, {@code EOREffectAbility} 15,
 * {@code EORGainItemAbility} 2. Verdicts (roster §7.1): 27 可转 / 19 降级 /
 * 1 登记.</p>
 *
 * <h2>Translations used in this file</h2>
 * <ul>
 * <li><b>Items are internal names, not ids.</b> Ruby's {@code battler.item},
 *     {@code initialItem}, {@code recycleItem} are PBS ids and its
 *     {@code effects[PBEffects::BallFetch]}/{@code [PickupItem]} hold ids or 0.
 *     This runtime stores item identity as the internal name String
 *     ({@code Battler.item} etc., see {@code HandlerHash}'s documented
 *     deviation), so {@code ==0}/{@code <=0}/{@code >0} become
 *     {@link #noItem}/{@link #hasItem} and those two effect slots are read with
 *     {@code stringVal} ({@code ""} = Ruby's 0).</li>
 * <li>{@code pbIsBerry?(item)} (PItem_Items:99-102) is {@link #isBerry}, i.e.
 *     {@code PbsData.Item.isBerry()} ({@code ITEM_TYPE==5}).</li>
 * <li>{@code battle.pbDisplay/pbShowAbilitySplash/pbHideAbilitySplash} use the
 *     landed {@code Battle.display/showAbilitySplash/hideAbilitySplash}
 *     (task-13); {@code _INTL} strings are concatenated at the call site
 *     ({@code BattleHandlerHelpers.java:36/51}).</li>
 * <li>{@code move.healingMove?} (TRIAGE :556) goes through the L2 layer:
 *     {@code MoveEffectRegistry.of(move.function()).healingMove(move)}
 *     ({@code PokeBattle_Move.rb:95}).</li>
 * <li>{@code battler.pbReduceHP(amt,false)} passes the plugin's documented
 *     defaults explicitly ({@code registerDamage=true,anyAnim=true},
 *     Battler_ChangeSelf:5); the item-trigger/refresh helpers likewise pass
 *     {@code (0,false)} (Battler_AbilityAndItem:235/253/295) and
 *     {@code pbAbilitiesOnDamageTaken(oldHP,-1)} (Battler_AbilityAndItem:67).</li>
 * <li>{@code battle.scene.*} (DRYSKIN :2085, SOLARPOWER :2138, HUNGERSWITCH
 *     :2314) and {@code battler.pbUpdate} (:2313) are scene/refresh calls with no
 *     counterpart here - each is a {@code 登记} comment, per the roster's
 *     "降级 = 演出调用做空实现 + 登记" rule.</li>
 * <li>{@code HUNGERSWITCH}'s {@code battler.form=} goes through
 *     {@code PendingApi.setForm} ({@code Battler.form()} is read-only here).</li>
 * <li>{@code ETERNALSTAR} is registered TWICE in this group (:4298 and :4329,
 *     roster §3) - the second {@code add} overwrites the first, exactly as in the
 *     plugin, so both calls are transcribed in order.</li>
 * <li>{@code PBDebug.log} (:2299) is kept as a {@code 登记} comment.</li>
 * </ul>
 *
 * <p>The load order is fixed by {@link BattleHandlerRegistry} and must not be
 * changed here.</p>
 */
final class AbilitiesSpeedEor {

    private AbilitiesSpeedEor() {
    }

    /** {@code BattleHandlers_Abilities.rb}: 47 entries (roster §1.2, §2 main table). */
    static void register() {
        // ==================================================================
        // SpeedCalcAbility (:5-60, :3558-3570, :4314-4321)
        // ==================================================================

        BattleHandlers.SpeedCalcAbility.add("CHLOROPHYLL", (ability, battler, mult) -> {
            // BattleHandlers_Abilities.rb:5-11
            int weather = battler.battle.pbWeather();                        // :7
            // :8 next mult*2 if (Sun||HarshSun) if !hasUtilityUmbrella?
            if (!battler.hasUtilityUmbrella()
                    && (weather == PBWeather.Sun || weather == PBWeather.HarshSun)) {
                return mult * 2f;
            }
            return null;
        });

        BattleHandlers.SpeedCalcAbility.add("QUICKFEET", (ability, battler, mult) -> {
            // BattleHandlers_Abilities.rb:12-17
            if (battler.pbHasAnyStatus()) {                                  // :14
                return mult * 1.5f;
            }
            return null;
        });

        BattleHandlers.SpeedCalcAbility.add("SANDRUSH", (ability, battler, mult) -> {
            // BattleHandlers_Abilities.rb:18-24
            int weather = battler.battle.pbWeather();                        // :20
            if (weather == PBWeather.Sandstorm) {                            // :21
                return mult * 2f;
            }
            return null;
        });

        BattleHandlers.SpeedCalcAbility.add("SLOWSTART", (ability, battler, mult) -> {
            // BattleHandlers_Abilities.rb:25-30
            if (battler.effects.intVal(PBEffects.Battler.SlowStart) > 0) {   // :27
                return mult / 2f;
            }
            return null;
        });

        BattleHandlers.SpeedCalcAbility.add("SLUSHRUSH", (ability, battler, mult) -> {
            // BattleHandlers_Abilities.rb:31-37
            int weather = battler.battle.pbWeather();                        // :33
            if (weather == PBWeather.Hail || weather == PBWeather.Snow) {    // :34
                return mult * 2f;
            }
            return null;
        });

        BattleHandlers.SpeedCalcAbility.add("SURGESURFER", (ability, battler, mult) -> {
            // BattleHandlers_Abilities.rb:38-43
            if (battler.battle.field.terrain == PBBattleTerrains.Electric) { // :40
                return mult * 2f;
            }
            return null;
        });

        BattleHandlers.SpeedCalcAbility.add("SWIFTSWIM", (ability, battler, mult) -> {
            // BattleHandlers_Abilities.rb:44-50
            int weather = battler.battle.pbWeather();                        // :46
            // :47 next mult*2 if (Rain||HeavyRain) if !hasUtilityUmbrella?
            if (!battler.hasUtilityUmbrella()
                    && (weather == PBWeather.Rain || weather == PBWeather.HeavyRain)) {
                return mult * 2f;
            }
            return null;
        });

        BattleHandlers.SpeedCalcAbility.add("UNBURDEN", (ability, battler, mult) -> {
            // BattleHandlers_Abilities.rb:51-60
            if (battler.effects.truthy(PBEffects.Battler.Unburden)              // :53
                    && noItem(battler.item)) {
                return mult * 2f;
            }
            return null;
        });

        BattleHandlers.SpeedCalcAbility.add("PROTOSYNTHESIS", (ability, battler, mult) -> {
            // BattleHandlers_Abilities.rb:3558-3563
            if (battler.effects.truthy(PBEffects.Battler.Transform)) {       // :3560
                return mult;
            }
            if (battler.effects.intVal(PBEffects.Battler.ParadoxStat) == PBStats.SPEED) {   // :3561
                return mult * 1.5f;
            }
            return null;
        });
        BattleHandlers.SpeedCalcAbility.copy("PROTOSYNTHESIS", "QUARKDRIVE");   // :3564

        BattleHandlers.SpeedCalcAbility.add("BESTOWEDRAIN", (ability, battler, mult) -> {
            // BattleHandlers_Abilities.rb:4314-4321
            int weather = battler.battle.pbWeather();                        // :4316
            // :4317 next mult*2 if (Rain||HeavyRain) if !hasUtilityUmbrella?
            if (!battler.hasUtilityUmbrella()
                    && (weather == PBWeather.Rain || weather == PBWeather.HeavyRain)) {
                return mult * 2f;
            }
            return null;
        });

        // ==================================================================
        // WeightCalcAbility (:61-82)
        // ==================================================================

        BattleHandlers.WeightCalcAbility.add("HEAVYMETAL", (ability, battler, w) -> {
            // BattleHandlers_Abilities.rb:61-66
            return w * 2f;                                                   // :63
        });

        BattleHandlers.WeightCalcAbility.add("LIGHTMETAL", (ability, battler, w) -> {
            // BattleHandlers_Abilities.rb:67-72
            return Math.max(w / 2f, 1f);                                     // :69 [w/2,1].max
        });

        BattleHandlers.WeightCalcAbility.add("ADAPTARMOR", (ability, battler, w) -> {
            // BattleHandlers_Abilities.rb:73-82
            return w * 2f;                                                   // :75
        });

        // ==================================================================
        // PriorityChangeAbility (:538-563, :3693-3700, :3828-3834)
        // ==================================================================

        BattleHandlers.PriorityChangeAbility.add("GALEWINGS", (ability, battler, move, pri) -> {
            // BattleHandlers_Abilities.rb:538-543
            if (battler.hp == battler.maxHp() && "FLYING".equals(move.type())) {   // :540
                return pri + 1;
            }
            return null;
        });

        BattleHandlers.PriorityChangeAbility.add("PRANKSTER", (ability, battler, move, pri) -> {
            // BattleHandlers_Abilities.rb:544-553
            if (move.statusMove()) {                                         // :546
                battler.effects.set(PBEffects.Battler.Prankster, true);      // :547
                return pri + 1;                                              // :548
            }
            return null;
        });

        BattleHandlers.PriorityChangeAbility.add("TRIAGE", (ability, battler, move, pri) -> {
            // BattleHandlers_Abilities.rb:554-563
            // :556 move.healingMove? —— 走 L2 招式层（PokeBattle_Move.rb:95）
            if (MoveEffectRegistry.of(move.function()).healingMove(move)) {
                return pri + 3;
            }
            return null;
        });

        BattleHandlers.PriorityChangeAbility.add("FAIRYDANCE", (ability, battler, move, pri) -> {
            // BattleHandlers_Abilities.rb:3693-3700
            if (battler.hp <= battler.maxHp() / 2                               // :3695
                    && move.flags().contains("a")) {                         // contactMove? = @flags[/a/]
                return pri + 1;
            }
            return null;
        });

        BattleHandlers.PriorityChangeAbility.add("SOUNDSTRIDE", (ability, battler, move, pri) -> {
            // BattleHandlers_Abilities.rb:3828-3834
            if ("BUG".equals(move.type())) {                                 // :3830
                return pri + 1;
            }
            return null;
        });

        // ==================================================================
        // PriorityBracketChangeAbility (:564-579, :3208-3216)
        // ==================================================================

        BattleHandlers.PriorityBracketChangeAbility.add("STALL", (ability, battler, subPri, battle) -> {
            // BattleHandlers_Abilities.rb:564-569
            if (subPri == 0) {                                               // :566
                return -1;
            }
            return null;
        });

        BattleHandlers.PriorityBracketChangeAbility.add("QUICKDRAW", (ability, battler, subPri, battle) -> {
            // BattleHandlers_Abilities.rb:570-579
            if (subPri < 1 && battle.pbRandom(10) < 3) {                     // :572
                return 1;
            }
            return null;
        });

        BattleHandlers.PriorityBracketChangeAbility.add("MYCELIUMMIGHT", (ability, battler, subPri, battle) -> {
            // BattleHandlers_Abilities.rb:3208-3216
            Object[] choices = battle.choices(battler.index);                // :3210
            if (":UseMove".equals(choices[0])) {                             // :3211
                BattleMove chosen = (BattleMove) choices[2];                 // :3212 choices[2]
                if (chosen != null && chosen.statusMove()) {
                    return -1;
                }
            }
            return null;
        });

        // ==================================================================
        // PriorityBracketUseAbility (:580-589)
        // ==================================================================

        BattleHandlers.PriorityBracketUseAbility.add("QUICKDRAW", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:580-589
            battle.display(battler.abilityName() + "使" + battler.pbThis()
                    + "可以先使用技能！");                                    // :582
        });

        // ==================================================================
        // RunFromBattleAbility (:2907-2913)
        // ==================================================================

        BattleHandlers.RunFromBattleAbility.add("RUNAWAY", (ability, battler) -> {
            // BattleHandlers_Abilities.rb:2907-2913
            return true;                                                     // :2909
        });

        // ==================================================================
        // EORWeatherAbility (:2080-2149)
        // ==================================================================

        BattleHandlers.EORWeatherAbility.add("DRYSKIN", (ability, weather, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2080-2103
            if (weather == PBWeather.Sun || weather == PBWeather.HarshSun) { // :2083
                battle.showAbilitySplash(battler);                           // :2084
                // 登记: :2085 battle.scene.pbDamageAnimation(battler) —— 场景未建模
                battler.pbReduceHP(battler.maxHp() / 8, false, true, true);  // :2086 pbReduceHP(÷8,false)
                battle.display(battler.pbThis() + "被强烈的阳光晒伤了！");   // :2087
                battle.hideAbilitySplash(battler);                           // :2088
                battler.pbItemHPHealCheck(0, false);                         // :2089
            } else if (weather == PBWeather.Rain || weather == PBWeather.HeavyRain) {   // :2090
                if (!battler.canHeal()) {                                    // :2091 next if !canHeal?
                    return;
                }
                battle.showAbilitySplash(battler);                           // :2092
                battler.pbRecoverHP(battler.maxHp() / 8);                    // :2093
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {          // :2094
                    battle.display(battler.pbThis() + "的HP回复了。");         // :2095
                } else {
                    battle.display(battler.pbThis() + "的" + battler.abilityName()
                            + "回复了HP。");                                 // :2097
                }
                battle.hideAbilitySplash(battler);                           // :2099
            }
        });

        BattleHandlers.EORWeatherAbility.add("ICEBODY", (ability, weather, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2104-2118
            if (!(weather == PBWeather.Hail || weather == PBWeather.Snow)) {  // :2106
                return;
            }
            if (!battler.canHeal()) {                                        // :2107
                return;
            }
            battle.showAbilitySplash(battler);                               // :2108
            battler.pbRecoverHP(battler.maxHp() / 16);                       // :2109
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {              // :2110
                battle.display(battler.pbThis() + "的HP回复了。");             // :2111
            } else {
                battle.display(battler.pbThis() + "的" + battler.abilityName()
                        + "回复了它的HP。");                                 // :2113
            }
            battle.hideAbilitySplash(battler);                               // :2115
        });

        BattleHandlers.EORWeatherAbility.add("RAINDISH", (ability, weather, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2119-2133
            if (!(weather == PBWeather.Rain || weather == PBWeather.HeavyRain)) {   // :2121
                return;
            }
            if (!battler.canHeal()) {                                        // :2122
                return;
            }
            battle.showAbilitySplash(battler);                               // :2123
            battler.pbRecoverHP(battler.maxHp() / 16);                       // :2124
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {              // :2125
                battle.display(battler.pbThis() + "的HP回复了。");             // :2126
            } else {
                battle.display(battler.pbThis() + "的" + battler.abilityName()
                        + "回复了HP。");                                     // :2128
            }
            battle.hideAbilitySplash(battler);                               // :2130
        });

        BattleHandlers.EORWeatherAbility.add("SOLARPOWER", (ability, weather, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2134-2149
            if (!(weather == PBWeather.Sun || weather == PBWeather.HarshSun)) {   // :2136
                return;
            }
            battle.showAbilitySplash(battler);                               // :2137
            // 登记: :2138 battle.scene.pbDamageAnimation(battler) —— 场景未建模
            battler.pbReduceHP(battler.maxHp() / 8, false, true, true);      // :2139
            battle.display(battler.pbThis() + "被强烈的阳光晒伤了！");       // :2140
            battle.hideAbilitySplash(battler);                               // :2141
            battler.pbItemHPHealCheck(0, false);                             // :2142
        });

        // ==================================================================
        // EORHealingAbility (:2150-2233) - overwritten later by
        // HandlerArceusOverrides (roster §9), both are transcribed
        // ==================================================================

        BattleHandlers.EORHealingAbility.add("HEALER", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2150-2176
            if (!(battle.pbRandom(100) < 30)) {                              // :2152 next unless …<30
                return;
            }
            battler.eachAlly(ally -> {                                       // :2153
                if (PBStatuses.idOf(ally.status) == PBStatuses.NONE) {       // :2154
                    return;
                }
                battle.showAbilitySplash(battler);                           // :2155
                int oldStatus = PBStatuses.idOf(ally.status);                // :2156
                ally.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :2157
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :2158
                    switch (oldStatus) {                                     // :2159
                        case PBStatuses.SLEEP:                               // :2160
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "把伙伴吵醒了！");                        // :2161
                            break;
                        case PBStatuses.POISON:                              // :2162
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了伙伴的毒！");                      // :2163
                            break;
                        case PBStatuses.BURN:                                // :2164
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了伙伴的灼伤！");                    // :2165
                            break;
                        case PBStatuses.PARALYSIS:                           // :2166
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了伙伴的麻痹！");                    // :2167
                            break;
                        case PBStatuses.FROZEN:                              // :2168
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "解冻了伙伴！");                          // :2169
                            break;
                        default:
                            break;
                    }
                }
                battle.hideAbilitySplash(battler);                           // :2172
            });
        });

        BattleHandlers.EORHealingAbility.add("HYDRATION", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2177-2204
            if (!battler.hasUtilityUmbrella()) {                             // :2179
                if (PBStatuses.idOf(battler.status) == PBStatuses.NONE) {    // :2180
                    return;
                }
                int curWeather = battle.pbWeather();                         // :2181
                if (curWeather != PBWeather.Rain && curWeather != PBWeather.HeavyRain) {   // :2182
                    return;
                }
                battle.showAbilitySplash(battler);                           // :2183
                int oldStatus = PBStatuses.idOf(battler.status);             // :2184
                battler.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :2185
                if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :2186
                    switch (oldStatus) {                                     // :2187
                        case PBStatuses.SLEEP:                               // :2188
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "使它醒来了！");                          // :2189
                            break;
                        case PBStatuses.POISON:                              // :2190
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了毒！");                            // :2191
                            break;
                        case PBStatuses.BURN:                                // :2192
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了灼伤！");                          // :2193
                            break;
                        case PBStatuses.PARALYSIS:                           // :2194
                            battle.display(battler.pbThis() + "的" + battler.abilityName()
                                    + "治愈了麻痹！");                          // :2195
                            break;
                        case PBStatuses.FROZEN:                              // :2196
                            battle.display(battler.abilityName() + "解冻了" + battler.pbThis()
                                    + "！");                                    // :2197
                            break;
                        default:
                            break;
                    }
                }
                battle.hideAbilitySplash(battler);                           // :2200
            }
        });

        BattleHandlers.EORHealingAbility.add("SHEDSKIN", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2205-2233
            if (PBStatuses.idOf(battler.status) == PBStatuses.NONE) {        // :2207
                return;
            }
            if (!(battle.pbRandom(100) < 30)) {                              // :2208 next unless …<30
                return;
            }
            battle.showAbilitySplash(battler);                               // :2209
            int oldStatus = PBStatuses.idOf(battler.status);                 // :2210
            battler.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :2211
            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {             // :2212
                switch (oldStatus) {                                         // :2213
                    case PBStatuses.SLEEP:                                   // :2214
                        battle.display(battler.pbThis() + "的" + battler.abilityName()
                                + "使它醒来了！");                              // :2215
                        break;
                    case PBStatuses.POISON:                                  // :2216
                        battle.display(battler.pbThis() + "的" + battler.abilityName()
                                + "治愈了毒！");                                // :2217
                        break;
                    case PBStatuses.BURN:                                    // :2218
                        battle.display(battler.pbThis() + "的" + battler.abilityName()
                                + "治愈了灼伤！");                              // :2219
                        break;
                    case PBStatuses.PARALYSIS:                               // :2220
                        battle.display(battler.pbThis() + "的" + battler.abilityName()
                                + "治愈了麻痹！");                              // :2221
                        break;
                    case PBStatuses.FROZEN:                                  // :2222
                        battle.display(battler.abilityName() + "解冻了" + battler.pbThis()
                                + "！");                                        // :2223
                        break;
                    default:
                        break;
                }
            }
            battle.hideAbilitySplash(battler);                               // :2226
        });

        // ==================================================================
        // EOREffectAbility (:2234-2319, :3306-3324, :3724-3757, :3879-3903,
        //                   :4157-4169, :4181-4191, :4298-4345, :4396-4413)
        // ==================================================================

        BattleHandlers.EOREffectAbility.add("BADDREAMS", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2234-2259
            // （roster 把它标为「登记 BOSS」；运行时的 Pokemon.battleRank 是普通 int
            //   字段（Pokemon.java:43-44），所以本条可完整转译，无需桩。）
            for (Battler b : battle.eachOtherSideBattler(battler.index)) {   // :2236
                if (!b.near(battler) || !b.asleep()) {                       // :2237
                    continue;
                }
                battle.showAbilitySplash(battler);                           // :2238
                if (!b.takesIndirectDamage(PokeBattle_SceneConstants.USE_ABILITY_SPLASH)) {   // :2239
                    continue;
                }
                int oldHP = b.hp;                                            // :2240
                if (b.pokemon.battleRank > 2) {                              // :2241
                    b.pbReduceHP(b.maxHp() / 40, true, true, true);          // :2242
                } else {
                    b.pbReduceHP(b.maxHp() / 8, true, true, true);           // :2244
                }
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {          // :2246
                    battle.display(b.pbThis() + "备受折磨！");                // :2247
                } else {
                    battle.display(b.pbThis() + "被" + battler.pbThis(true) + "的"
                            + battler.abilityName() + "折磨着！");            // :2249-2250
                }
                battle.hideAbilitySplash(battler);                           // :2252
                b.pbItemHPHealCheck(0, false);                               // :2253
                b.pbAbilitiesOnDamageTaken(oldHP, -1);                       // :2254
                if (b.fainted()) {                                           // :2255
                    b.pbFaint();
                }
            }
        });

        BattleHandlers.EOREffectAbility.add("MOODY", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2260-2282
            Array<Integer> randomUp = new Array<>();                         // :2262
            Array<Integer> randomDown = new Array<>();
            for (int s : PBStats.EACH_MAIN_BATTLE_STAT) {                    // :2263
                if (battler.pbCanRaiseStatStage(s, battler)) {               // :2264
                    randomUp.add(s);
                }
                if (battler.pbCanLowerStatStage(s, battler)) {               // :2265
                    randomDown.add(s);
                }
            }
            if (randomUp.size == 0 && randomDown.size == 0) {                // :2267
                return;
            }
            battle.showAbilitySplash(battler);                               // :2268
            if (randomUp.size > 0) {                                         // :2269
                int r = battle.pbRandom(randomUp.size);                      // :2270
                battler.pbRaiseStatStageByAbility(randomUp.get(r), 2, battler, false);   // :2271
                randomDown.removeValue(randomUp.get(r), false);              // :2272 randomDown.delete(...)
            }
            if (randomDown.size > 0) {                                       // :2274
                int r = battle.pbRandom(randomDown.size);                    // :2275
                battler.pbLowerStatStageByAbility(randomDown.get(r), 1, battler, false, false);   // :2276
            }
            battle.hideAbilitySplash(battler);                               // :2278
            if (randomDown.size > 0) {                                       // :2279
                battler.pbItemStatRestoreCheck(0, false);
            }
        });

        BattleHandlers.EOREffectAbility.add("SPEEDBOOST", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2283-2292
            // A Pokémon's turnCount is 0 if it became active after the beginning of
            // a round (:2285-2286)
            if (battler.turnCount > 0 && battler.pbCanRaiseStatStage(PBStats.SPEED, battler)) {   // :2287
                battler.pbRaiseStatStageByAbility(PBStats.SPEED, 1, battler, true);   // :2288 splashAnim=true
            }
        });

        BattleHandlers.EOREffectAbility.add("BALLFETCH", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2293-2307
            // 运行时的道具身份是内部名字符串，故 BallFetch 槽按「名字字符串、"" 为空」读写
            // （Ruby 用 0 表示空）。
            String ball = battler.effects.stringVal(PBEffects.Battler.BallFetch);   // :2295 !=0
            if (ball != null && !ball.isEmpty() && noItem(battler.item)) {
                battler.item = ball;                                         // :2297
                battler.setInitialItem(battler.item);                        // :2298
                // 登记: :2299 PBDebug.log("[Ability triggered] …") —— PBDebug 未建模
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {          // :2300
                    battle.showAbilitySplash(battler);
                }
                battle.display(battler.pbThis() + "回收了" + itemName(battle, ball)
                        + "！");                                             // :2301
                battler.effects.set(PBEffects.Battler.BallFetch, "");        // :2302
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {          // :2303
                    battle.hideAbilitySplash(battler);
                }
            }
        });

        BattleHandlers.EOREffectAbility.add("HUNGERSWITCH", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2308-2324
            if (battler.isSpecies("MORPEKO")) {                              // :2310
                battle.showAbilitySplash(battler);                           // :2311
                battler.setFormShowing(battler.form() == 0 ? 1 : 0);          // :2312 + :2314 pbChangePokemon (queued with the splash in order)
                // 登记: :2313 battler.pbUpdate(true) —— 刷新调用，无对应实现
                battle.display(battler.pbThis() + "变身了！");                // :2315
                battle.hideAbilitySplash(battler);                           // :2316
            }
        });

        BattleHandlers.EOREffectAbility.add("CUDCHEW", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3306-3324
            if (hasItem(battler.item)) {                                     // :3308
                return;
            }
            if (noItem(battler.recycleItem) || !isBerry(battle, battler.recycleItem)) {   // :3309
                return;
            }
            if (battler.effects.intVal(PBEffects.Battler.CudChew) == 0) {    // :3310 when 0
                battler.effects.increment(PBEffects.Battler.CudChew);        // :3312 +=1
            } else {                                                         // :3313 else
                battler.effects.set(PBEffects.Battler.CudChew, 0);           // :3314
                // 登记: :3315 pbShowAbilitySplash(battler,true) 的 delay=true 是纯演出延迟，
                // 运行时只有 1 参 showAbilitySplash（PokeBattle_Battle:801）。
                battle.showAbilitySplash(battler);
                battle.hideAbilitySplash(battler);                           // :3316
                battler.pbHeldItemTriggerCheck(battler.recycleItem, true);   // :3317
                battler.setRecycleItem(null);                                // :3318
            }
        });

        BattleHandlers.EOREffectAbility.add("SEAMLESS", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3724-3731
            if (battler.turnCount > 0 && battler.pbCanRaiseStatStage(PBStats.SPEED, battler)) {   // :3726
                battler.pbRaiseStatStageByAbility(PBStats.SPEED, 1, battler, true);   // :3727
            }
        });

        BattleHandlers.EOREffectAbility.add("MOERAE", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3739-3757
            Array<Integer> randomUp = new Array<>();                         // :3741
            Array<Integer> randomDown = new Array<>();
            for (int s : PBStats.EACH_MAIN_BATTLE_STAT) {                    // :3742
                if (battler.pbCanRaiseStatStage(s, battler)) {               // :3743
                    randomUp.add(s);
                }
                if (battler.pbCanLowerStatStage(s, battler)) {               // :3744
                    randomDown.add(s);
                }
            }
            if (randomUp.size == 0 && randomDown.size == 0) {                // :3746
                return;
            }
            battle.showAbilitySplash(battler);                               // :3747
            if (randomUp.size > 0) {                                         // :3748
                int r = battle.pbRandom(randomUp.size);                      // :3749
                battler.pbRaiseStatStageByAbility(randomUp.get(r), 1, battler, false);   // :3750
                randomDown.removeValue(randomUp.get(r), false);              // :3751
            }
            battle.hideAbilitySplash(battler);                               // :3753
            if (randomDown.size > 0) {                                       // :3754
                battler.pbItemStatRestoreCheck(0, false);
            }
        });

        BattleHandlers.EOREffectAbility.add("MAGICQUEEN", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3879-3890
            if (battler.turnCount == 0) {                                    // :3881
                return;
            }
            battle.showAbilitySplash(battler);                               // :3882
            battler.eachOpposing(b -> {                                      // :3883
                // :3884 插件缺陷已修：第三参误传 self（proc），按无招式处理（见 PendingApi）
                if (!PendingApi.pbCanConfuse(b, battler, true)) {
                    return;
                }
                b.pbConfuse();                                               // :3885
            });
            battle.hideAbilitySplash(battler);                               // :3887
        });

        BattleHandlers.EOREffectAbility.add("DEMONCLOAK", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:3891-3903
            battle.showAbilitySplash(battler);                               // :3893
            battler.eachOpposing(b -> {                                      // :3894
                if (b.effects.intVal(PBEffects.Battler.Yawn) > 0) {          // :3895
                    return;
                }
                // :3896 插件缺陷已修：同 :3884
                if (!PendingApi.pbCanSleep(b, battler, true)) {
                    return;
                }
                b.effects.set(PBEffects.Battler.Yawn, 2);                    // :3897
                battle.display(battler.pbThis() + "让" + b.pbThis(true)
                        + "昏昏欲睡！");                                     // :3898
            });
            battle.hideAbilitySplash(battler);                               // :3900
        });

        BattleHandlers.EOREffectAbility.add("FLUFFYCOAT", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4157-4169
            if (battler.turnCount > 0 && battler.hp > 0) {                   // :4159
                // 使用 pbReduceHP 代替 pbRecoilDamage (:4160)
                battler.pbReduceHP(battler.maxHp() / 8, false, true, true);  // :4161
                battle.display(battler.pbThis() + "因毛棉棉而受到了伤害！");  // :4162
                battler.pbItemHPHealCheck(0, false);                         // :4163
                if (battler.fainted()) {                                     // :4164
                    battler.pbFaint();
                }
            }
        });

        BattleHandlers.EOREffectAbility.add("TMOVERLORD", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4181-4191
            // A Pokémon's turnCount is 0 if it became active after the beginning of
            // a round (:4183-4184)
            if (battler.turnCount > 0 && battler.pbCanRaiseStatStage(PBStats.SPEED, battler)) {   // :4185
                battler.pbRaiseStatStageByAbility(PBStats.SPEED, 1, battler, true);   // :4186
            }
        });

        BattleHandlers.EOREffectAbility.add("ETERNALSTAR", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4298-4308
            if (!battler.canHeal()) {                                        // :4300
                return;
            }
            battle.showAbilitySplash(battler);                               // :4301
            battler.pbRecoverHP(battler.maxHp() / 8);                        // :4302
            battle.display(battler.pbThis() + "的伤口通过星火愈合了！");     // :4303
            battle.hideAbilitySplash(battler);                               // :4304
        });

        // ETERNALSTAR 在插件里被注册两次（:4298 与 :4329，roster §3）：第二次 add 覆盖
        // 第一次，行为与插件一致，两条都照抄。
        BattleHandlers.EOREffectAbility.add("ETERNALSTAR", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4329-4345
            if (!battler.canHeal()) {                                        // :4331
                return;
            }
            battle.showAbilitySplash(battler);                               // :4332
            battler.pbRecoverHP(battler.maxHp() / 8);                        // :4333
            battle.display(battler.pbThis() + "的伤口愈合了！");             // :4334
            battle.hideAbilitySplash(battler);                               // :4335
        });

        BattleHandlers.EOREffectAbility.add("BIYIHUANGYAN", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:4396-4413
            if (!battler.burned()) {                                         // :4398
                return;
            }
            // 回复灼伤伤害量（最大HP的1/16）(:4399)
            if (!battler.canHeal()) {                                        // :4400
                return;
            }
            battle.showAbilitySplash(battler);                               // :4401
            int healAmount = battler.maxHp() / 16;                           // :4402 heal_amt
            battler.pbRecoverHP(healAmount);                                 // :4403
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {              // :4404
                battle.display(battler.pbThis()
                        + "将灼伤的痛楚转化为了力量，HP回复了！");            // :4405
            } else {
                battle.display(battler.pbThis() + "的" + battler.abilityName()
                        + "将灼伤转化为了HP！");                             // :4407
            }
            battle.hideAbilitySplash(battler);                               // :4409
        });

        // ==================================================================
        // EORGainItemAbility (:2325-2379)
        // ==================================================================

        BattleHandlers.EORGainItemAbility.add("HARVEST", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2325-2342
            if (hasItem(battler.item)) {                                     // :2327
                return;
            }
            // :2328 next if recycleItem<=0 || (!pbIsBerry? && !hasUtilityUmbrella?)
            if (noItem(battler.recycleItem)
                    || (!isBerry(battle, battler.recycleItem) && !battler.hasUtilityUmbrella())) {
                return;
            }
            int curWeather = battle.pbWeather();                             // :2329
            if (curWeather != PBWeather.Sun && curWeather != PBWeather.HarshSun) {   // :2330
                if (!(battle.pbRandom(100) < 50)) {                          // :2331 next unless …<50
                    return;
                }
            }
            battle.showAbilitySplash(battler);                               // :2333
            battler.item = battler.recycleItem;                              // :2334
            battler.setRecycleItem("");                                      // :2335 setRecycleItem(0)
            if (noItem(battler.initialItem)) {                               // :2336
                battler.setInitialItem(battler.item);
            }
            battle.display(battler.pbThis() + "收获了" + battler.itemName()
                    + "！");                                                 // :2337
            battle.hideAbilitySplash(battler);                               // :2338
            battler.pbHeldItemTriggerCheck(0, false);                        // :2339
        });

        BattleHandlers.EORGainItemAbility.add("PICKUP", (ability, battler, battle) -> {
            // BattleHandlers_Abilities.rb:2343-2379
            if (hasItem(battler.item)) {                                     // :2345
                return;
            }
            String foundItem = "";                                           // :2346 foundItem = 0
            Battler fromBattler = null;                                      // :2346 fromBattler = nil
            int use = 0;                                                     // :2346 use = 0
            for (Battler b : battle.eachBattler()) {                         // :2347
                if (b.index == battler.index) {                              // :2348
                    continue;
                }
                if (b.effects.intVal(PBEffects.Battler.PickupUse) <= use) {  // :2349
                    continue;
                }
                String picked = b.effects.stringVal(PBEffects.Battler.PickupItem);   // :2350
                foundItem = picked == null ? "" : picked;
                fromBattler = b;                                             // :2351
                use = b.effects.intVal(PBEffects.Battler.PickupUse);         // :2352
            }
            if (noItem(foundItem)) {                                         // :2354 foundItem<=0
                return;
            }
            battle.showAbilitySplash(battler);                               // :2355
            battler.item = foundItem;                                        // :2356
            fromBattler.effects.set(PBEffects.Battler.PickupItem, "");        // :2357 = 0
            fromBattler.effects.set(PBEffects.Battler.PickupUse, 0);         // :2358
            if (foundItem.equals(fromBattler.recycleItem())) {               // :2359
                fromBattler.setRecycleItem("");
            }
            if (battle.wildBattle() && noItem(battler.initialItem)           // :2360
                    && foundItem.equals(fromBattler.initialItem)) {
                battler.setInitialItem(foundItem);                           // :2361
                fromBattler.setInitialItem("");                              // :2362
            }
            battle.display(battler.pbThis() + "回收了" + battler.itemName()
                    + "！");                                                 // :2364
            battle.hideAbilitySplash(battler);                               // :2365
            battler.pbHeldItemTriggerCheck(0, false);                        // :2366
        });
    }

    /**
     * {@code battler.item > 0} / {@code foundItem > 0} in the plugin: the runtime
     * stores item identity as the internal name ({@code ""} = none).
     */
    private static boolean hasItem(String item) {
        return item != null && !item.isEmpty();
    }

    /** {@code battler.item == 0} / {@code recycleItem <= 0}: no item at all. */
    private static boolean noItem(String item) {
        return item == null || item.isEmpty();
    }

    /**
     * {@code pbIsBerry?(item)} (PItem_Items:99-102): {@code ITEM_TYPE==5}. The
     * plugin takes the id, this runtime the internal name, so the item is looked
     * up in the same {@code PbsData.Items} table the handlers get their names from.
     */
    private static boolean isBerry(Battle battle, String item) {
        PbsData pbs = battle == null ? null : battle.pbs();
        PbsData.Item entry = pbs == null || item == null ? null : pbs.item(item);
        return entry != null && entry.isBerry();
    }

    /**
     * {@code PBItems.getName(item)} (Compiler_PBS:446-448): the display name, or
     * {@code ""} for an unknown item ({@code pbGetMessage} returns {@code ""},
     * Intl_Messages:555-561).
     */
    private static String itemName(Battle battle, String item) {
        PbsData pbs = battle == null ? null : battle.pbs();
        PbsData.Item entry = pbs == null ? null : pbs.item(item);
        return entry == null || entry.name == null ? "" : entry.name;
    }
}
