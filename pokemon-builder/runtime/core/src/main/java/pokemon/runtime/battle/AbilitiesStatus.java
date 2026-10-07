package pokemon.runtime.battle;

import java.util.List;

/**
 * Stage 4 / L1': the {@code BattleHandlers_Abilities.rb} registrations assigned
 * to this file by {@code stage4-l1-handler-roster.md} §1.2 - <b>38 entries</b>
 * plus <b>13 {@code copy} lines</b>, span subtotal 510.
 *
 * <h2>Order</h2>
 * <p>Every {@code add}/{@code copy} below appears in the SAME ORDER as its Ruby
 * source line, because {@code HandlerHash.copy} reads the source's handler at the
 * moment it runs (Event_Handlers.rb:115-122): grouping by handler type instead
 * would let a {@code copy} run before its source and silently copy nothing. The
 * three copies far outside the entry spans are placed at their real positions
 * (roster §5): {@code :2729}, {@code :2920}, {@code :2967}, {@code :3112},
 * {@code :3773}.</p>
 *
 * <h2>Faithful parts and the runtime's two bridges</h2>
 * <ul>
 * <li>A proc that falls off its end returns Ruby's {@code nil}; every
 *     {@code Boolean} handler here therefore {@code return null} on that path,
 *     and each {@code trigger*} wrapper turns {@code null} into its documented
 *     default (BattleHandlers.java:150-178).</li>
 * <li>{@code battler.status} is the internal-name String in this runtime
 *     (PBStatuses.java's class comment), so {@code @status!=PBStatuses::X}
 *     becomes {@code !battler.hasStatus("X")} / {@code battler.burned()}. The
 *     handlers' own {@code status} PARAMETER stays the plugin's integer
 *     (task-11 style: the trigger's {@code int status}).</li>
 * <li>{@code status.nil?} (BattleHandlers_Abilities.rb:130) has no int value;
 *     {@link PBStatuses#NONE} (0) is this runtime's encoding of "no status".</li>
 * </ul>
 *
 * <h2>Stubs used</h2>
 * <p>{@code OPPORTUNIST} (:3229/:3236) calls
 * {@code Battler#pbItemOpposingStatGainCheck}, which has not landed on
 * {@code Battler} yet; it goes through the {@code PendingApi} stub of the same
 * name (throws until the wiring step replaces it), keeping the Ruby call shape
 * instead of dropping the call.</p>
 */
final class AbilitiesStatus {

    private AbilitiesStatus() {
    }

    /** {@code BattleHandlers_Abilities.rb}: 38 entries + 13 copies (roster §1.2, §2, §5). */
    static void register() {
        // ==============================================================
        // StatusCheckAbilityNonIgnorable
        // ==============================================================

        // BattleHandlers_Abilities.rb:127-137  StatusCheckAbilityNonIgnorable COMATOSE
        BattleHandlers.StatusCheckAbilityNonIgnorable.add("COMATOSE", (ability, battler, status) -> {
            if (!battler.isSpecies("KOMALA")) {                          // :129 next false if !battler.isSpecies?(:KOMALA)
                return false;
            }
            // :130 next true if status.nil? || status==PBStatuses::SLEEP
            if (status == PBStatuses.NONE || status == PBStatuses.SLEEP) {
                return true;
            }
            return null;                                                 // :131 the proc falls through (Ruby nil)
        });

        // ==============================================================
        // StatusImmunityAbility
        // ==============================================================

        // BattleHandlers_Abilities.rb:138-143  StatusImmunityAbility FLOWERVEIL
        BattleHandlers.StatusImmunityAbility.add("FLOWERVEIL", (ability, battler, status) -> {
            if (battler.pbHasType("GRASS")) {                            // :140 next true if battler.pbHasType?(:GRASS)
                return true;
            }
            return null;                                                 // :141
        });

        // BattleHandlers_Abilities.rb:144-151  StatusImmunityAbility IMMUNITY
        BattleHandlers.StatusImmunityAbility.add("IMMUNITY", (ability, battler, status) -> {
            if (status == PBStatuses.POISON) {                           // :146 next true if status==PBStatuses::POISON
                return true;
            }
            return null;                                                 // :147
        });

        // :150 BattleHandlers::StatusImmunityAbility.copy(:IMMUNITY,:PASTELVEIL)
        BattleHandlers.StatusImmunityAbility.copy("IMMUNITY", "PASTELVEIL");

        // BattleHandlers_Abilities.rb:152-159  StatusImmunityAbility INSOMNIA
        BattleHandlers.StatusImmunityAbility.add("INSOMNIA", (ability, battler, status) -> {
            if (status == PBStatuses.SLEEP) {                            // :154 next true if status==PBStatuses::SLEEP
                return true;
            }
            return null;                                                 // :155
        });

        // :158 BattleHandlers::StatusImmunityAbility.copy(:INSOMNIA,:SWEETVEIL,:VITALSPIRIT)
        BattleHandlers.StatusImmunityAbility.copy("INSOMNIA", "SWEETVEIL", "VITALSPIRIT");

        // BattleHandlers_Abilities.rb:160-167  StatusImmunityAbility LEAFGUARD
        BattleHandlers.StatusImmunityAbility.add("LEAFGUARD", (ability, battler, status) -> {
            int w = battler.battle.pbWeather();                          // :162 w = battler.battle.pbWeather
            // :163-164 next true if (w==Sun || w==HarshSun) && !battler.hasUtilityUmbrella?
            if ((w == PBWeather.Sun || w == PBWeather.HarshSun) && !battler.hasUtilityUmbrella()) {
                return true;
            }
            return null;                                                 // :165
        });

        // BattleHandlers_Abilities.rb:168-173  StatusImmunityAbility LIMBER
        BattleHandlers.StatusImmunityAbility.add("LIMBER", (ability, battler, status) -> {
            if (status == PBStatuses.PARALYSIS) {                        // :170 next true if status==PBStatuses::PARALYSIS
                return true;
            }
            return null;                                                 // :171
        });

        // BattleHandlers_Abilities.rb:174-179  StatusImmunityAbility MAGMAARMOR
        BattleHandlers.StatusImmunityAbility.add("MAGMAARMOR", (ability, battler, status) -> {
            if (status == PBStatuses.FROZEN) {                           // :176 next true if status==PBStatuses::FROZEN
                return true;
            }
            return null;                                                 // :177
        });

        // BattleHandlers_Abilities.rb:180-191  StatusImmunityAbility WATERVEIL
        BattleHandlers.StatusImmunityAbility.add("WATERVEIL", (ability, battler, status) -> {
            if (status == PBStatuses.BURN) {                             // :182 next true if status==PBStatuses::BURN
                return true;
            }
            return null;                                                 // :183
        });

        // :186 BattleHandlers::StatusImmunityAbility.copy(:WATERVEIL,:WATERBUBBLE)
        BattleHandlers.StatusImmunityAbility.copy("WATERVEIL", "WATERBUBBLE");

        // ==============================================================
        // StatusImmunityAbilityNonIgnorable
        // ==============================================================

        // BattleHandlers_Abilities.rb:192-197  StatusImmunityAbilityNonIgnorable COMATOSE
        BattleHandlers.StatusImmunityAbilityNonIgnorable.add("COMATOSE", (ability, battler, status) -> {
            if (battler.isSpecies("KOMALA")) {                           // :194 next true if battler.isSpecies?(:KOMALA)
                return true;
            }
            return null;                                                 // :195
        });

        // BattleHandlers_Abilities.rb:198-207  StatusImmunityAbilityNonIgnorable SHIELDSDOWN
        BattleHandlers.StatusImmunityAbilityNonIgnorable.add("SHIELDSDOWN", (ability, battler, status) -> {
            // :200 next true if battler.isSpecies?(:MINIOR) && battler.form<7
            if (battler.isSpecies("MINIOR") && battler.form() < 7) {
                return true;
            }
            return null;                                                 // :201
        });

        // ==============================================================
        // StatusImmunityAllyAbility
        // ==============================================================

        // BattleHandlers_Abilities.rb:208-213  StatusImmunityAllyAbility FLOWERVEIL
        BattleHandlers.StatusImmunityAllyAbility.add("FLOWERVEIL", (ability, battler, status) -> {
            if (battler.pbHasType("GRASS")) {                            // :210 next true if battler.pbHasType?(:GRASS)
                return true;
            }
            return null;                                                 // :211
        });

        // BattleHandlers_Abilities.rb:214-219  StatusImmunityAllyAbility SWEETVEIL
        BattleHandlers.StatusImmunityAllyAbility.add("SWEETVEIL", (ability, battler, status) -> {
            if (status == PBStatuses.SLEEP) {                            // :216 next true if status==PBStatuses::SLEEP
                return true;
            }
            return null;                                                 // :217
        });

        // BattleHandlers_Abilities.rb:220-229  StatusImmunityAllyAbility PASTELVEIL
        BattleHandlers.StatusImmunityAllyAbility.add("PASTELVEIL", (ability, battler, status) -> {
            if (status == PBStatuses.POISON) {                           // :222 next true if status==PBStatuses::POISON
                return true;
            }
            return null;                                                 // :223
        });

        // ==============================================================
        // AbilityOnStatusInflicted
        // ==============================================================

        // BattleHandlers_Abilities.rb:230-272  AbilityOnStatusInflicted SYNCHRONIZE
        BattleHandlers.AbilityOnStatusInflicted.add("SYNCHRONIZE", (ability, battler, user, status) -> {
            if (user == null || user.index == battler.index) {           // :232 next if !user || user.index==battler.index
                return;
            }
            switch (status) {                                            // :233 case status
                case PBStatuses.POISON:                                  // :234 when PBStatuses::POISON
                    if (user.pbCanPoisonSynchronize(battler)) {          // :235
                        battler.battle.showAbilitySplash(battler);       // :236
                        String msg = null;                               // :237
                        if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {   // :238
                            // :239-240 _INTL("{1}的{2}使{3}中毒了！",...)
                            msg = battler.pbThis() + "的" + battler.abilityName() + "使"
                                    + user.pbThis(true) + "中毒了！";
                        }
                        user.pbPoison(null, msg, battler.statusCount > 0);   // :241 user.pbPoison(nil,msg,(statusCount>0))
                        battler.battle.hideAbilitySplash(battler);       // :242
                    }
                    break;
                case PBStatuses.BURN:                                    // :244 when PBStatuses::BURN
                    if (user.pbCanBurnSynchronize(battler)) {            // :245
                        battler.battle.showAbilitySplash(battler);       // :246
                        String msg = null;                               // :247
                        if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {   // :248
                            // :249-250 _INTL("{1}的{2}使{3}灼伤了！",...)
                            msg = battler.pbThis() + "的" + battler.abilityName() + "使"
                                    + user.pbThis(true) + "灼伤了！";
                        }
                        user.pbBurn(null, msg);                          // :251
                        battler.battle.hideAbilitySplash(battler);       // :252
                    }
                    break;
                case PBStatuses.PARALYSIS:                               // :254 when PBStatuses::PARALYSIS
                    if (user.pbCanParalyzeSynchronize(battler)) {        // :255
                        battler.battle.showAbilitySplash(battler);       // :256
                        String msg = null;                               // :257
                        if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {   // :258
                            // :259-260 _INTL("{1}的{2}使{3}麻痹了！\n{3}有可能无法行动！",...)
                            msg = battler.pbThis() + "的" + battler.abilityName() + "使"
                                    + user.pbThis(true) + "麻痹了！\n" + user.pbThis(true)
                                    + "有可能无法行动！";
                        }
                        user.pbParalyze(null, msg);                      // :262
                        battler.battle.hideAbilitySplash(battler);       // :263
                    }
                    break;
                default:                                                 // :265 end of the case
                    break;
            }
        });

        // ==============================================================
        // StatusCureAbility
        // ==============================================================

        // BattleHandlers_Abilities.rb:273-286  StatusCureAbility IMMUNITY
        BattleHandlers.StatusCureAbility.add("IMMUNITY", (ability, battler) -> {
            if (!battler.hasStatus("POISON")) {                          // :275 next if battler.status!=PBStatuses::POISON
                return null;
            }
            battler.battle.showAbilitySplash(battler);                   // :276
            battler.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :277
            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :278
                // :279 _INTL("{1}的{2}消去了毒！",battler.pbThis,battler.abilityName)
                battler.battle.display(battler.pbThis() + "的" + battler.abilityName() + "消去了毒！");
            }
            battler.battle.hideAbilitySplash(battler);                   // :281
            return null;                                                 // :282 (the proc's last call is nil)
        });

        // :285 BattleHandlers::StatusCureAbility.copy(:IMMUNITY,:PASTELVEIL)
        BattleHandlers.StatusCureAbility.copy("IMMUNITY", "PASTELVEIL");

        // BattleHandlers_Abilities.rb:287-300  StatusCureAbility INSOMNIA
        BattleHandlers.StatusCureAbility.add("INSOMNIA", (ability, battler) -> {
            if (!battler.hasStatus("SLEEP")) {                           // :289 next if battler.status!=PBStatuses::SLEEP
                return null;
            }
            battler.battle.showAbilitySplash(battler);                   // :290
            battler.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :291
            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :292
                // :293 _INTL("{1}的{2}使它醒来了！",battler.pbThis,battler.abilityName)
                battler.battle.display(battler.pbThis() + "的" + battler.abilityName() + "使它醒来了！");
            }
            battler.battle.hideAbilitySplash(battler);                   // :295
            return null;                                                 // :296
        });

        // :299 BattleHandlers::StatusCureAbility.copy(:INSOMNIA,:VITALSPIRIT)
        BattleHandlers.StatusCureAbility.copy("INSOMNIA", "VITALSPIRIT");

        // BattleHandlers_Abilities.rb:301-312  StatusCureAbility LIMBER
        BattleHandlers.StatusCureAbility.add("LIMBER", (ability, battler) -> {
            if (!battler.hasStatus("PARALYSIS")) {                       // :303 next if battler.status!=PBStatuses::PARALYSIS
                return null;
            }
            battler.battle.showAbilitySplash(battler);                   // :304
            battler.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :305
            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :306
                // :307 _INTL("{1}的{2}治愈了麻痹！",battler.pbThis,battler.abilityName)
                battler.battle.display(battler.pbThis() + "的" + battler.abilityName() + "治愈了麻痹！");
            }
            battler.battle.hideAbilitySplash(battler);                   // :309
            return null;                                                 // :310
        });

        // BattleHandlers_Abilities.rb:313-324  StatusCureAbility MAGMAARMOR
        BattleHandlers.StatusCureAbility.add("MAGMAARMOR", (ability, battler) -> {
            if (!battler.hasStatus("FROZEN")) {                          // :315 next if battler.status!=PBStatuses::FROZEN
                return null;
            }
            battler.battle.showAbilitySplash(battler);                   // :316
            battler.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :317
            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :318
                // :319 _INTL("{2}解冻了{1}！",battler.pbThis,battler.abilityName)
                battler.battle.display(battler.abilityName() + "解冻了" + battler.pbThis() + "！");
            }
            battler.battle.hideAbilitySplash(battler);                   // :321
            return null;                                                 // :322
        });

        // BattleHandlers_Abilities.rb:325-351  StatusCureAbility OBLIVIOUS
        BattleHandlers.StatusCureAbility.add("OBLIVIOUS", (ability, battler) -> {
            // :327-328 next if battler.effects[Attract]<0 && (battler.effects[Taunt]==0 || !NEWEST_BATTLE_MECHANICS)
            if (battler.effects.intVal(PBEffects.Battler.Attract) < 0
                    && (battler.effects.intVal(PBEffects.Battler.Taunt) == 0
                        || !Battle.NEWEST_BATTLE_MECHANICS)) {
                return null;
            }
            battler.battle.showAbilitySplash(battler);                   // :329
            if (battler.effects.intVal(PBEffects.Battler.Attract) >= 0) {   // :330
                battler.pbCureAttract();                                 // :331
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :332
                    // :333 _INTL("{1}不再迷恋对方了！",battler.pbThis)
                    battler.battle.display(battler.pbThis() + "不再迷恋对方了！");
                } else {
                    // :335-336 _INTL("{1}的{2}解除了着迷状态！",battler.pbThis,battler.abilityName)
                    battler.battle.display(battler.pbThis() + "的" + battler.abilityName() + "解除了着迷状态！");
                }
            }
            if (battler.effects.intVal(PBEffects.Battler.Taunt) > 0 && Battle.NEWEST_BATTLE_MECHANICS) {   // :339
                battler.effects.set(PBEffects.Battler.Taunt, 0);          // :340
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :341
                    // :342 _INTL("{1}的挑衅无效了！",battler.pbThis)
                    battler.battle.display(battler.pbThis() + "的挑衅无效了！");
                } else {
                    // :344-345 _INTL("{1}的{2}使挑衅无效了！",battler.pbThis,battler.abilityName)
                    battler.battle.display(battler.pbThis() + "的" + battler.abilityName() + "使挑衅无效了！");
                }
            }
            battler.battle.hideAbilitySplash(battler);                   // :348
            return null;                                                 // :349
        });

        // BattleHandlers_Abilities.rb:352-366  StatusCureAbility OWNTEMPO
        BattleHandlers.StatusCureAbility.add("OWNTEMPO", (ability, battler) -> {
            if (battler.effects.intVal(PBEffects.Battler.Confusion) == 0) {   // :354
                return null;
            }
            battler.battle.showAbilitySplash(battler);                   // :355
            battler.pbCureConfusion();                                   // :356
            if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {          // :357
                // :358 _INTL("{1}解除了混乱！",battler.pbThis)
                battler.battle.display(battler.pbThis() + "解除了混乱！");
            } else {
                // :360-361 _INTL("{1}的{2}解除了混乱！",battler.pbThis,battler.abilityName)
                battler.battle.display(battler.pbThis() + "的" + battler.abilityName() + "解除了混乱！");
            }
            battler.battle.hideAbilitySplash(battler);                   // :363
            return null;                                                 // :364
        });

        // BattleHandlers_Abilities.rb:367-384  StatusCureAbility WATERVEIL
        BattleHandlers.StatusCureAbility.add("WATERVEIL", (ability, battler) -> {
            if (!battler.hasStatus("BURN")) {                            // :369 next if battler.status!=PBStatuses::BURN
                return null;
            }
            battler.battle.showAbilitySplash(battler);                   // :370
            battler.pbCureStatus(PokeBattle_SceneConstants.USE_ABILITY_SPLASH);   // :371
            if (!PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {         // :372
                // :373 _INTL("{1}的{2}治愈了灼伤！",battler.pbThis,battler.abilityName)
                battler.battle.display(battler.pbThis() + "的" + battler.abilityName() + "治愈了灼伤！");
            }
            battler.battle.hideAbilitySplash(battler);                   // :375
            return null;                                                 // :376
        });

        // :379 BattleHandlers::StatusCureAbility.copy(:WATERVEIL,:WATERBUBBLE,:DEEPSEAFASCINATION)
        BattleHandlers.StatusCureAbility.copy("WATERVEIL", "WATERBUBBLE", "DEEPSEAFASCINATION");

        // ==============================================================
        // StatLossImmunityAbility
        // ==============================================================

        // BattleHandlers_Abilities.rb:385-401  StatLossImmunityAbility BIGPECKS
        BattleHandlers.StatLossImmunityAbility.add("BIGPECKS", (ability, battler, stat, battle, showMessages) -> {
            if (stat != PBStats.DEFENSE) {                               // :387 next false if stat!=PBStats::DEFENSE
                return false;
            }
            if (showMessages) {                                          // :388
                battle.showAbilitySplash(battler);                       // :389
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :390
                    // :391 _INTL("{1}的{2}不能被降低了！",battler.pbThis,PBStats.getName(stat))
                    battle.display(battler.pbThis() + "的" + PBStats.getName(stat) + "不能被降低了！");
                } else {
                    // :393-394 _INTL("{1}的{2}防止了{3}遗失！",...)
                    battle.display(battler.pbThis() + "的" + battler.abilityName() + "防止了"
                            + PBStats.getName(stat) + "遗失！");
                }
                battle.hideAbilitySplash(battler);                       // :396
            }
            return true;                                                 // :398 next true
        });

        // BattleHandlers_Abilities.rb:402-418  StatLossImmunityAbility CLEARBODY
        BattleHandlers.StatLossImmunityAbility.add("CLEARBODY", (ability, battler, stat, battle, showMessages) -> {
            if (showMessages) {                                          // :404
                battle.showAbilitySplash(battler);                       // :405
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :406
                    // :407 _INTL("{1}的能力值不能被降低了！",battler.pbThis)
                    battle.display(battler.pbThis() + "的能力值不能被降低了！");
                } else {
                    // :409 _INTL("{1}的{2}防止了能力值降低！",battler.pbThis,battler.abilityName)
                    battle.display(battler.pbThis() + "的" + battler.abilityName() + "防止了能力值降低！");
                }
                battle.hideAbilitySplash(battler);                       // :411
            }
            return true;                                                 // :413 next true
        });

        // :417 copy(:CLEARBODY,:WHITESMOKE,:NOBLESTRIKE,:ETERNALSTAR,:TRANSLUCENTGHOST)
        BattleHandlers.StatLossImmunityAbility.copy("CLEARBODY", "WHITESMOKE", "NOBLESTRIKE",
                "ETERNALSTAR", "TRANSLUCENTGHOST");

        // BattleHandlers_Abilities.rb:419-434  StatLossImmunityAbility FLOWERVEIL
        BattleHandlers.StatLossImmunityAbility.add("FLOWERVEIL", (ability, battler, stat, battle, showMessages) -> {
            if (!battler.pbHasType("GRASS")) {                           // :421 next false if !battler.pbHasType?(:GRASS)
                return false;
            }
            if (showMessages) {                                          // :422
                battle.showAbilitySplash(battler);                       // :423
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :424
                    // :425 _INTL("{1}的能力值不能被降低了！",battler.pbThis)
                    battle.display(battler.pbThis() + "的能力值不能被降低了！");
                } else {
                    // :427 _INTL("{1}的{2}防止了能力值降低！",battler.pbThis,battler.abilityName)
                    battle.display(battler.pbThis() + "的" + battler.abilityName() + "防止了能力值降低！");
                }
                battle.hideAbilitySplash(battler);                       // :429
            }
            return true;                                                 // :431 next true
        });

        // BattleHandlers_Abilities.rb:435-451  StatLossImmunityAbility HYPERCUTTER
        BattleHandlers.StatLossImmunityAbility.add("HYPERCUTTER", (ability, battler, stat, battle, showMessages) -> {
            if (stat != PBStats.ATTACK) {                                // :437 next false if stat!=PBStats::ATTACK
                return false;
            }
            if (showMessages) {                                          // :438
                battle.showAbilitySplash(battler);                       // :439
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :440
                    // :441 _INTL("{1}的{2}不能被降低了！",battler.pbThis,PBStats.getName(stat))
                    battle.display(battler.pbThis() + "的" + PBStats.getName(stat) + "不能被降低了！");
                } else {
                    // :443-444 _INTL("{1}的{2}防止了{3}遗失！",...)
                    battle.display(battler.pbThis() + "的" + battler.abilityName() + "防止了"
                            + PBStats.getName(stat) + "遗失！");
                }
                battle.hideAbilitySplash(battler);                       // :446
            }
            return true;                                                 // :448 next true
        });

        // BattleHandlers_Abilities.rb:452-473  StatLossImmunityAbility KEENEYE
        BattleHandlers.StatLossImmunityAbility.add("KEENEYE", (ability, battler, stat, battle, showMessages) -> {
            if (stat != PBStats.ACCURACY) {                              // :454 next false if stat!=PBStats::ACCURACY
                return false;
            }
            if (showMessages) {                                          // :455
                battle.showAbilitySplash(battler);                       // :456
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :457
                    // :458 _INTL("{1}的{2}不能被降低了！",battler.pbThis,PBStats.getName(stat))
                    battle.display(battler.pbThis() + "的" + PBStats.getName(stat) + "不能被降低了！");
                } else {
                    // :460-461 _INTL("{1}的{2}防止了{3}遗失！",...)
                    battle.display(battler.pbThis() + "的" + battler.abilityName() + "防止了"
                            + PBStats.getName(stat) + "遗失！");
                }
                battle.hideAbilitySplash(battler);                       // :463
            }
            return true;                                                 // :465 next true
        });

        // :468 BattleHandlers::StatLossImmunityAbility.copy(:KEENEYE,:ROSYAEGIS)
        BattleHandlers.StatLossImmunityAbility.copy("KEENEYE", "ROSYAEGIS");

        // ==============================================================
        // StatLossImmunityAbilityNonIgnorable
        // ==============================================================

        // BattleHandlers_Abilities.rb:474-492  StatLossImmunityAbilityNonIgnorable FULLMETALBODY
        BattleHandlers.StatLossImmunityAbilityNonIgnorable.add("FULLMETALBODY",
                (ability, battler, stat, battle, showMessages) -> {
            if (showMessages) {                                          // :476
                battle.showAbilitySplash(battler);                       // :477
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :478
                    // :479 _INTL("{1}的能力值不能被降低了！",battler.pbThis)
                    battle.display(battler.pbThis() + "的能力值不能被降低了！");
                } else {
                    // :481 _INTL("{1}的{2}防止了能力值降低！",battler.pbThis,battler.abilityName)
                    battle.display(battler.pbThis() + "的" + battler.abilityName() + "防止了能力值降低！");
                }
                battle.hideAbilitySplash(battler);                       // :483
            }
            return true;                                                 // :485 next true
        });

        // ==============================================================
        // StatLossImmunityAllyAbility
        // ==============================================================

        // BattleHandlers_Abilities.rb:493-519  StatLossImmunityAllyAbility FLOWERVEIL
        BattleHandlers.StatLossImmunityAllyAbility.add("FLOWERVEIL",
                (ability, bearer, battler, stat, battle, showMessages) -> {
            if (!battler.pbHasType("GRASS")) {                           // :495 next false if !battler.pbHasType?(:GRASS)
                return false;
            }
            if (showMessages) {                                          // :496
                battle.showAbilitySplash(bearer);                        // :497
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :498
                    // :499 _INTL("{1}的能力值不能被降低了！",battler.pbThis)
                    battle.display(battler.pbThis() + "的能力值不能被降低了！");
                } else {
                    // :501-502 _INTL("{1}的{2}防止了{3}的能力值降低！",bearer.pbThis,bearer.abilityName,battler.pbThis(true))
                    battle.display(bearer.pbThis() + "的" + bearer.abilityName() + "防止了"
                            + battler.pbThis(true) + "的能力值降低！");
                }
                battle.hideAbilitySplash(bearer);                        // :504
            }
            return true;                                                 // :506 next true
        });

        // ==============================================================
        // AbilityOnStatLoss
        // ==============================================================

        // BattleHandlers_Abilities.rb:520-526  AbilityOnStatLoss COMPETITIVE
        BattleHandlers.AbilityOnStatLoss.add("COMPETITIVE", (ability, battler, stat, user) -> {
            if (user != null && !user.opposes(battler)) {                // :522 next if user && !user.opposes?(battler)
                return;
            }
            battler.pbRaiseStatStageByAbility(PBStats.SPATK, 2, battler, true);   // :523
        });

        // BattleHandlers_Abilities.rb:527-537  AbilityOnStatLoss DEFIANT
        BattleHandlers.AbilityOnStatLoss.add("DEFIANT", (ability, battler, stat, user) -> {
            if (user != null && !user.opposes(battler)) {                // :529 next if user && !user.opposes?(battler)
                return;
            }
            battler.pbRaiseStatStageByAbility(PBStats.ATTACK, 2, battler, true);  // :530
        });

        // ==============================================================
        // The copies that sit far outside the entry spans (roster §5)
        // ==============================================================

        // :2729 BattleHandlers::StatusImmunityAbility.copy(:UNNERVE,:CONFESSIONLIST)
        // 登记: UNNERVE has no StatusImmunityAbility handler (this file registers
        //       none), so HandlerHash.copy installs nothing - transcribed verbatim.
        BattleHandlers.StatusImmunityAbility.copy("UNNERVE", "CONFESSIONLIST");

        // :2920 BattleHandlers::StatusImmunityAbility.copy(:SHARPNESS,:PSYCHICEDGE,:ENDLESSDARKN,:NETHERDRIVE)
        // 登记: SHARPNESS has no StatusImmunityAbility handler here -> copies nothing.
        BattleHandlers.StatusImmunityAbility.copy("SHARPNESS", "PSYCHICEDGE", "ENDLESSDARKN", "NETHERDRIVE");

        // :2967 BattleHandlers::StatusImmunityAbility.copy(:WATERVEIL,:WATERBUBBLE,:THERMALEXCHANGE)
        BattleHandlers.StatusImmunityAbility.copy("WATERVEIL", "WATERBUBBLE", "THERMALEXCHANGE");

        // :3112 BattleHandlers::StatLossImmunityAbility.copy(:KEENEYE, :MINDSEYE)
        BattleHandlers.StatLossImmunityAbility.copy("KEENEYE", "MINDSEYE");

        // ==============================================================
        // AbilityOnOpposingStatGain / StatusImmunityAbility / AbilityOnMoveSuccessCheck
        // / AbilityOnInflictingStatus
        // ==============================================================

        // BattleHandlers_Abilities.rb:3217-3244  AbilityOnOpposingStatGain OPPORTUNIST
        BattleHandlers.AbilityOnOpposingStatGain.add("OPPORTUNIST", (ability, battler, battle, statUps) -> {
            boolean showAnim = true;                                     // :3219
            battle.showAbilitySplash(battler);                           // :3220
            for (int[] statUp : statUps) {                               // :3221 statUps.each do |stat, increment|
                int stat = statUp[0];
                int increment = statUp[1];
                if (!battler.pbCanRaiseStatStage(stat, battler)) {       // :3222 next if !battler.pbCanRaiseStatStage?(stat,battler)
                    continue;
                }
                // Ruby's 4th argument is showAnim; the 5th (ignoreContrary) keeps its default false
                // (Battler_StatStages:47 pbRaiseStatStage(stat,increment,user,showAnim=true,ignoreContrary=false)).
                if (battler.pbRaiseStatStage(stat, increment, battler, showAnim, false)) {   // :3223
                    showAnim = false;                                    // :3224
                }
            }
            if (showAnim) {                                              // :3227
                battle.display(battler.pbThis() + "的能力不能再提高了！");
            }
            battle.hideAbilitySplash(battler);                           // :3228
            PendingApi.pbItemOpposingStatGainCheck(battler, statUps);    // :3229
            // :3230-3240 Mirror Herb can trigger off this ability.
            if (!showAnim) {                                             // :3231
                List<int[]> opposingStatUps = battle.sideStatUps[battler.idxOwnSide()];   // :3232
                for (Battler b : battle.allOtherSideBattlers(battler.index)) {            // :3233
                    if (b == null || b.fainted()) {                      // :3234
                        continue;
                    }
                    if (b.itemActive()) {                                // :3235
                        PendingApi.pbItemOpposingStatGainCheck(b, opposingStatUps);   // :3236
                    }
                }
                opposingStatUps.clear();                                 // :3239
            }
        });

        // BattleHandlers_Abilities.rb:3245-3249  StatusImmunityAbility PURIFYINGSALT
        BattleHandlers.StatusImmunityAbility.add("PURIFYINGSALT", (ability, battler, status) -> {
            return true;                                                 // :3247 next true
        });

        // BattleHandlers_Abilities.rb:3379-3388  AbilityOnMoveSuccessCheck TERASHELL
        BattleHandlers.AbilityOnMoveSuccessCheck.add("TERASHELL", (ability, user, target, move, battle) -> {
            if (!target.damageState.terashell) {                         // :3381 next if !target.damageState.terashell
                return;
            }
            battle.showAbilitySplash(target);                            // :3382
            // :3383 _INTL("{1}的{2}扭曲了\n属性相性！",target.pbThis,target.abilityName)
            battle.display(target.pbThis() + "的" + target.abilityName() + "扭曲了\n属性相性！");
            battle.hideAbilitySplash(target);                            // :3384
        });

        // BattleHandlers_Abilities.rb:3447-3458  AbilityOnInflictingStatus POISONPUPPETEER
        // NOTE: the Ruby proc names its parameters (ability,user,battler,status) while
        // the wrapper's declaration reads (ability,battler,user,status); the lambda
        // below keeps the PROC's names because the wrapper passes
        // (userAbility, user, battler, status) (BattleHandlers_Abilities.rb:166).
        BattleHandlers.AbilityOnInflictingStatus.add("POISONPUPPETEER", (ability, user, battler, status) -> {
            if (user == null || user.index == battler.index) {           // :3449 next if !user || user.index==battler.index
                return;
            }
            if (status != PBStatuses.POISON) {                           // :3450 next if status != PBStatuses::POISON
                return;
            }
            if (battler.effects.intVal(PBEffects.Battler.Confusion) > 0) {   // :3451
                return;
            }
            user.battle.showAbilitySplash(user);                         // :3452
            if (battler.pbCanConfuse(user, false, null, false)) {        // :3453 battler.pbConfuse if battler.pbCanConfuse?(user,false,nil)
                battler.pbConfuse();
            }
            user.battle.hideAbilitySplash(user);                         // :3454
        });

        // :3773 BattleHandlers::StatusCureAbility.copy(:OWNTEMPO,:DEMONKILLER)
        BattleHandlers.StatusCureAbility.copy("OWNTEMPO", "DEMONKILLER");

        // BattleHandlers_Abilities.rb:3841-3856  StatLossImmunityAbility STEELDYNASTY
        BattleHandlers.StatLossImmunityAbility.add("STEELDYNASTY", (ability, battler, stat, battle, showMessages) -> {
            // :3843 无视 NeutralizingGas，直接防止能力下降
            if (showMessages) {                                          // :3844
                battle.showAbilitySplash(battler);                       // :3845
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {      // :3846
                    // :3847 _INTL("{1}的能力值不能被降低了！",battler.pbThis)
                    battle.display(battler.pbThis() + "的能力值不能被降低了！");
                } else {
                    // :3849 _INTL("{1}的{2}防止了能力值降低！",battler.pbThis,battler.abilityName)
                    battle.display(battler.pbThis() + "的" + battler.abilityName() + "防止了能力值降低！");
                }
                battle.hideAbilitySplash(battler);                       // :3851
            }
            return true;                                                 // :3853 next true
        });

        // BattleHandlers_Abilities.rb:4117-4125  StatusImmunityAbility RAINCURTAIN
        BattleHandlers.StatusImmunityAbility.add("RAINCURTAIN", (ability, battler, status) -> {
            int w = battler.battle.pbWeather();                          // :4119 w = battler.battle.pbWeather
            // :4120-4121 next true if (w==Rain || w==HeavyRain) && !battler.hasUtilityUmbrella?
            if ((w == PBWeather.Rain || w == PBWeather.HeavyRain) && !battler.hasUtilityUmbrella()) {
                return true;
            }
            return null;                                                 // :4122
        });

        // BattleHandlers_Abilities.rb:4414-4427  StatusCureAbility BIYIHUANGYAN
        BattleHandlers.StatusCureAbility.add("BIYIHUANGYAN", (ability, battler) -> {
            int w = battler.battle.pbWeather();                          // :4416
            // :4418 if (w == Sun || w == HarshSun) && battler.burned?
            if ((w == PBWeather.Sun || w == PBWeather.HarshSun) && battler.burned()) {
                battler.battle.showAbilitySplash(battler);               // :4419
                // :4420 _INTL("在晴天的照耀下，{1}的灼伤无法被治愈！", battler.pbThis)
                battler.battle.display("在晴天的照耀下，" + battler.pbThis() + "的灼伤无法被治愈！");
                battler.battle.hideAbilitySplash(battler);                // :4421
                return true;                                             // :4422 next true 阻止治愈
            }
            return null;                                                 // :4424 falls through
        });

        // BattleHandlers_Abilities.rb:4533-4537  StatusImmunityAbility PLAYFULHEART
        BattleHandlers.StatusImmunityAbility.add("PLAYFULHEART", (ability, battler, status) -> {
            return true;                                                 // :4535 next true
        });
    }
}
