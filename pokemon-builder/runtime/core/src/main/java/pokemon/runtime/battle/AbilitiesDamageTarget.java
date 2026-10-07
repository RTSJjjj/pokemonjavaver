package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;

import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 4 / L1': the {@code BattleHandlers_Abilities.rb} registrations assigned
 * to this file by {@code stage4-l1-handler-roster.md} §1.2 - <b>28 entries</b>,
 * plus the 6 {@code .copy} lines of roster §5 that land here (§6: 28 + 8 copy
 * targets = 36 registered symbols).
 *
 * <p>Groups (roster §1.3): {@code DamageCalcTargetAbility} 23,
 * {@code DamageCalcTargetAbilityNonIgnorable} 2, {@code DamageCalcTargetAllyAbility}
 * 3. Every entry is 可转 (roster §7.1: no 降级/登记).</p>
 *
 * <h2>Translations and registered defects</h2>
 * <ul>
 * <li>{@code isConst?(val,PBTypes,:X)} = {@code "X".equals(val)}: type identity is
 *     the internal name (see {@link PBTypes}).</li>
 * <li>{@code PBTypes.superEffective?(target.damageState.typeMod)} uses the
 *     one-argument {@code typeMod} form ({@code PBTypes.superEffective(int)}).</li>
 * <li><b>登记 (plugin defect, roster §4 #380):</b> {@code RAINBOWARCH} calls
 *     {@code target.pbWeather} ({@code BattleHandlers_Abilities:4581}), a method
 *     that only exists on {@code Battle}, never on {@code Battler} - in Ruby that
 *     line is a guaranteed {@code NoMethodError}. The call shape is copied as
 *     {@code PendingApi.pbWeather(target)} and NOT corrected to
 *     {@code target.battle.pbWeather()}, and no default value is invented.</li>
 * <li><b>登记 (plugin defect, found while transcribing):</b> three
 *     {@code DamageCalcTargetAbility.copy} calls copy from symbols that this
 *     group never registers anywhere in the project ({@code MOLDBREAKER} :2653,
 *     {@code PRESSURE} :2662, {@code SUPREMEOVERLORD} :3134). {@code HandlerHash#copy}
 *     (Event_Handlers.rb:115-122) does nothing when the source has no handler, so
 *     {@code SHATTERFIST}/{@code CALAMITYAERIAL}/{@code TMOVERLORD}/{@code SPOVERLORD}/{@code DSOVERLORD}
 *     end up with NO handler in this group. The calls are copied verbatim and not
 *     "fixed"; {@link HandlerHash#copy} has the same no-op semantics
 *     (HandlerHash.java:90-98).</li>
 * </ul>
 *
 * <p>The load order is fixed by {@link BattleHandlerRegistry} and must not be
 * changed here.</p>
 */
final class AbilitiesDamageTarget {

    private AbilitiesDamageTarget() {
    }

    /** {@code BattleHandlers_Abilities.rb}: 28 entries (roster §1.2, §2 main table). */
    static void register() {
        // ==================================================================
        // DamageCalcTargetAbility (:1243-1364)
        // ==================================================================

        BattleHandlers.DamageCalcTargetAbility.add("DRYSKIN", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1243-1250
            if ("FIRE".equals(type)) {                                       // :1245
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.25f;                // :1246
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("FILTER", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1251-1259
            if (PBTypes.superEffective(target.damageState.typeMod)) {        // :1253
                mults[BattleHandlers.FINAL_DMG_MULT] *= 0.75f;               // :1254
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("ADAPTARMOR", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1260-1267 (适应装甲)
            String type3 = target.effects.stringVal(PBEffects.Battler.Type3);      // :1262 (哨兵 <0 读出 null)
            float mod = PBTypes.getCombinedEffectiveness(pbs(target), type,
                    type1(target), type2(target), type3) / 8.0f;                // :1263
            if (mod > 1.0f) {                                                    // :1264 mod>1.0
                mults[BattleHandlers.FINAL_DMG_MULT] /= mod;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("ETRTNALIGHT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1268-1274 (无限之光)
            String type3 = target.effects.stringVal(PBEffects.Battler.Type3);      // :1270
            float mod = PBTypes.getCombinedEffectiveness(pbs(target), type,
                    type1(target), type2(target), type3) / 8.0f;                // :1271
            if (mod > 1.0f) {                                                    // :1272
                mults[BattleHandlers.FINAL_DMG_MULT] /= mod;
            }
        });
        BattleHandlers.DamageCalcTargetAbility.copy("FILTER", "SOLIDROCK");      // :1277

        BattleHandlers.DamageCalcTargetAbility.add("FLOWERGIFT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1279-1288
            int weather = user.battle.pbWeather();                           // :1281
            if (specialMove(move)                                            // :1282 move.specialMove?
                    && (weather == PBWeather.Sun || weather == PBWeather.HarshSun)
                    && !target.hasUtilityUmbrella()) {                       // :1283
                mults[BattleHandlers.DEF_MULT] =
                        Math.round(mults[BattleHandlers.DEF_MULT] * 1.5f);   // :1284
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("FLUFFY", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1289-1295
            if ("FIRE".equals(move.calcType())) {                            // :1291 isConst?(move.calcType,…)
                mults[BattleHandlers.FINAL_DMG_MULT] *= 2f;
            }
            if (move.flags().contains("a")) {                                // :1292 contactMove? = @flags[/a/]
                mults[BattleHandlers.FINAL_DMG_MULT] /= 2f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("FURCOAT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1296-1302
            if (move.physical() || "122".equals(move.function())) {          // :1298 (Psyshock)
                mults[BattleHandlers.DEF_MULT] *= 2f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("ICESCALES", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1303-1308
            if (specialMove(move)) {                                         // :1305
                mults[BattleHandlers.FINAL_DMG_MULT] /= 2f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("GRASSPELT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1309-1316
            if (user.battle.field.terrain == PBBattleTerrains.Grassy) {      // :1311
                mults[BattleHandlers.DEF_MULT] *= 1.5f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("HEATPROOF", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1317-1322
            if ("FIRE".equals(type)) {                                       // :1319
                mults[BattleHandlers.BASE_DMG_MULT] /= 2f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("MARVELSCALE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1323-1330
            if (target.pbHasAnyStatus() && move.physical()) {                // :1325
                mults[BattleHandlers.DEF_MULT] *= 1.5f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("MULTISCALE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1331-1338
            if (target.hp == target.maxHp()) {                               // :1333 target.hp==target.totalhp
                mults[BattleHandlers.FINAL_DMG_MULT] /= 2f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("THICKFAT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1339-1346
            if ("FIRE".equals(type) || "ICE".equals(type)) {                 // :1341
                mults[BattleHandlers.BASE_DMG_MULT] /= 2f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("WATERBUBBLE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1347-1354
            if ("FIRE".equals(type)) {                                       // :1349
                mults[BattleHandlers.FINAL_DMG_MULT] /= 2f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("PUNKROCK", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1355-1364
            if (move.flags().contains("k")) {                                // :1357 soundMove?
                mults[BattleHandlers.FINAL_DMG_MULT] /= 2f;
            }
        });

        // ==================================================================
        // DamageCalcTargetAbilityNonIgnorable (:1365-1384)
        // ==================================================================

        BattleHandlers.DamageCalcTargetAbilityNonIgnorable.add("PRISMARMOR", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1365-1372
            if (PBTypes.superEffective(target.damageState.typeMod)) {        // :1367
                mults[BattleHandlers.FINAL_DMG_MULT] *= 0.75f;
            }
        });

        BattleHandlers.DamageCalcTargetAbilityNonIgnorable.add("SHADOWSHIELD", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1373-1384
            if (target.hp == target.maxHp()) {                               // :1375
                mults[BattleHandlers.FINAL_DMG_MULT] /= 2f;
            }
        });

        // ==================================================================
        // DamageCalcTargetAllyAbility (:1385-1404, :3599-3605)
        // ==================================================================

        BattleHandlers.DamageCalcTargetAllyAbility.add("FLOWERGIFT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1385-1394
            int weather = user.battle.pbWeather();                           // :1387
            if (specialMove(move)                                            // :1388
                    && (weather == PBWeather.Sun || weather == PBWeather.HarshSun)
                    && !target.hasUtilityUmbrella()) {                       // :1389
                mults[BattleHandlers.DEF_MULT] =
                        Math.round(mults[BattleHandlers.DEF_MULT] * 1.5f);   // :1390
            }
        });

        BattleHandlers.DamageCalcTargetAllyAbility.add("FRIENDGUARD", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1395-1404
            mults[BattleHandlers.FINAL_DMG_MULT] *= 0.75f;                   // :1397
        });

        // ==================================================================
        // Registered no-op copies (:2653, :2662, :3134) - see class javadoc
        // ==================================================================

        // 登记: 插件缺陷 —— 本组从未注册 MOLDBREAKER，copy 源缺失 ⇒ 不复制
        BattleHandlers.DamageCalcTargetAbility.copy("MOLDBREAKER", "SHATTERFIST");   // :2653

        // 登记: 插件缺陷 —— 本组从未注册 PRESSURE，copy 源缺失 ⇒ 不复制
        BattleHandlers.DamageCalcTargetAbility.copy("PRESSURE", "CALAMITYAERIAL");   // :2662

        // 登记: 插件缺陷 —— 本组从未注册 SUPREMEOVERLORD，copy 源缺失 ⇒ 不复制
        BattleHandlers.DamageCalcTargetAbility.copy(                                 // :3134
                "SUPREMEOVERLORD", "TMOVERLORD", "SPOVERLORD", "DSOVERLORD");

        // ==================================================================
        // DamageCalcTargetAbility, later entries (:3250-3255, :3547-3557, :3721)
        // ==================================================================

        BattleHandlers.DamageCalcTargetAbility.add("PURIFYINGSALT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3250-3255
            if ("GHOST".equals(type)) {                                      // :3252
                mults[BattleHandlers.ATK_MULT] /= 2f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("PROTOSYNTHESIS", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3547-3557
            if (target.effects.truthy(PBEffects.Battler.Transform)) {        // :3549
                return;
            }
            int stat = target.effects.intVal(PBEffects.Battler.ParadoxStat); // :3550
            if (move.physical() && stat == PBStats.DEFENSE) {                // :3551
                mults[BattleHandlers.DEF_MULT] *= 1.3f;
            }
            if (specialMove(move) && stat == PBStats.SPDEF) {                // :3552
                mults[BattleHandlers.DEF_MULT] *= 1.3f;
            }
        });
        BattleHandlers.DamageCalcTargetAbility.copy("PROTOSYNTHESIS", "QUARKDRIVE");  // :3555

        BattleHandlers.DamageCalcTargetAbility.add("BROKENBREATH", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3592-3598
            if (specialMove(move)) {                                         // :3594
                mults[BattleHandlers.DEF_MULT] =
                        Math.round(mults[BattleHandlers.DEF_MULT] * 0.5f);   // :3595
            }
        });

        BattleHandlers.DamageCalcTargetAllyAbility.add("BROKENBREATH", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3599-3605
            if (specialMove(move)) {                                         // :3601
                mults[BattleHandlers.DEF_MULT] =
                        Math.round(mults[BattleHandlers.DEF_MULT] * 0.5f);   // :3602
            }
        });

        // 虹霓之穹
        BattleHandlers.DamageCalcTargetAbility.copy("ETRTNALIGHT", "STARFISSURE");   // :3721

        // ==================================================================
        // DamageCalcTargetAbility, last entries (:4017-4585)
        // ==================================================================

        BattleHandlers.DamageCalcTargetAbility.add("VERDANTWARD", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4017-4025
            if (user.battle.field.terrain == PBBattleTerrains.Grassy) {      // :4019
                mults[BattleHandlers.DEF_MULT] *= 1.5f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("RAINCURTAIN", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4100-4116
            int weather = target.battle.pbWeather();                         // :4102
            if ((weather == PBWeather.Rain || weather == PBWeather.HeavyRain) // :4103
                    && !target.hasUtilityUmbrella()) {
                int ret = 0;                                                 // :4104
                // :4105 pbParty(user.index) —— 本运行时每个队伍成员都是一个 Battler
                // (Battle.playerParty/foeParty)，故按 Battler 迭代、用 Battler.hasType
                // 代替 Ruby 的 pkmn.hasType?。
                for (Battler pkmn : user.battle.partyOf(user.index)) {
                    if (pkmn == null || pkmn.fainted()) {                    // :4106
                        continue;
                    }
                    if (!pkmn.hasType("WATER") && !pkmn.hasType("POISON")) { // :4107
                        continue;
                    }
                    ret += 1;                                                // :4108
                }
                ret = Math.min(ret, 6);                                      // :4110 [ret,6].min
                float mult = 1.24f + ret * 0.06f;                            // :4111
                mults[BattleHandlers.DEF_MULT] =
                        Math.round(mults[BattleHandlers.DEF_MULT] * mult);   // :4112
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("FLUFFYCOAT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4148-4156
            if (!move.statusMove()) {                                        // :4150 damagingMove? = @category!=2
                mults[BattleHandlers.FINAL_DMG_MULT] /= 2f;
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("STORMEYE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4564-4572
            if (user.isSpecies("LUGIA") && "ELECTRIC".equals(type)) {        // :4566
                mults[BattleHandlers.BASE_DMG_MULT] *= 0.25f;                // :4567
            }
        });

        BattleHandlers.DamageCalcTargetAbility.add("RAINBOWARCH", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4578-4585
            if (user.isSpecies("HOOH") && "WATER".equals(type)) {            // :4580
                // 登记: 插件缺陷 (roster §4 #380) —— :4581 调用 target.pbWeather，
                // 而 pbWeather 只定义在 Battle 上（Battler 没有），Ruby 里该行必然
                // NoMethodError。照抄调用形状（PendingApi 桩），不修正成
                // target.battle.pbWeather、不补默认值。
                int weather = PendingApi.pbWeather(target);
                float mult = (weather == PBWeather.Sun || weather == PBWeather.HarshSun)
                        ? 0.5f : 0.25f;                                      // :4581
                mults[BattleHandlers.BASE_DMG_MULT] *= mult;                 // :4582
            }
        });
    }

    /**
     * {@code move.specialMove?} (PokeBattle_Move.rb:83-88) with
     * {@code MOVE_CATEGORY_PER_MOVE = true} (Settings:159), i.e. {@code @category==1}
     * - the PBS "Special" category.
     */
    private static boolean specialMove(BattleMove move) {
        return move != null && "Special".equalsIgnoreCase(move.category());
    }

    /** {@code target.type1} (the raw first type; null when the battler has none). */
    private static String type1(Battler target) {
        Array<String> types = target == null ? null : target.types();
        return types == null || types.size < 1 ? null : types.get(0);
    }

    /** {@code target.type2} (the raw second type; null when there is none). */
    private static String type2(Battler target) {
        Array<String> types = target == null ? null : target.types();
        return types == null || types.size < 2 ? null : types.get(1);
    }

    /**
     * The one PBS table the plugin's global {@code PBTypes} reads (see
     * {@link PBTypes}); taken from the battle the handler is running in.
     */
    private static PbsData pbs(Battler battler) {
        return battler == null || battler.battle == null ? null : battler.battle.pbs();
    }
}
