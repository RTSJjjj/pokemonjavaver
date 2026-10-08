package pokemon.runtime.battle;

import pokemon.runtime.battle.movefx.MoveEffectRegistry;

/**
 * Stage 4 / L1': the {@code BattleHandlers_Abilities.rb} registrations assigned
 * to this file by {@code stage4-l1-handler-roster.md} §1.2 - <b>72 entries</b>
 * ({@code DamageCalcUserAbility} 67, {@code DamageCalcUserAllyAbility} 5; roster
 * §1.3/§6) plus the 5 {@code .copy} lines of roster §5 C12/C13/C14/C15/C36
 * (= 9 further registered symbols, roster §6: 72 + 9 = 81). Every entry is 可转
 * (roster §7.1: 0 登记 / 0 降级).
 *
 * <p>The entries are NOT a contiguous Ruby range: they live at
 * {@code BattleHandlers_Abilities.rb:925-1242} and {@code :2914-4558}, because
 * the plugin interleaves this trigger group with every other group. The section
 * headers below follow Ruby order, and each entry carries its own
 * {@code :起-止行} comment.</p>
 *
 * <h2>Documented translations (Lead-approved 2026-10-07)</h2>
 * <ul>
 * <li>{@code isConst?(val,PBTypes,:X)} ({@code PSystem_Utilities:205-212})
 *     compares against the compiled constant; this runtime's type identity IS
 *     the internal name, so it is written {@code "X".equals(val)} - the same
 *     comparison the plugin's compiled {@code PBTypes} class performs
 *     (Compiler_PBS:443-445), and the same one {@code AbilitiesAccuracyCritType}
 *     uses.</li>
 * <li>{@code move.physicalMove?} ({@code PokeBattle_Move.rb:74-79}) is
 *     {@code move.physical()}: {@code Settings:159 MOVE_CATEGORY_PER_MOVE = true},
 *     so the plugin returns {@code @category==0}, i.e. {@code "Physical"}
 *     (MoveEffectBase:78-79). Same for {@code move.statusMove?}.</li>
 * <li>{@code move.specialMove?} ({@code PokeBattle_Move.rb:83-88}) is written
 *     {@code "Special".equals(move.category())} - the same expression
 *     MoveEffectBase:94-95 uses for {@code @category==1}. {@code BattleMove} has
 *     no {@code special()} accessor.</li>
 * <li>The flag predicates are read straight off the PBS flag string, exactly as
 *     {@code PokeBattle_Move.rb:113-129} does: {@code contactMove? = flags "a"},
 *     {@code bitingMove? = "i"}, {@code punchingMove? = "j"},
 *     {@code soundMove? = "k"}, {@code pulseMove? = "m"},
 *     {@code slicingMove? = "p"}.</li>
 * <li>{@code move.recoilMove?} ({@code PokeBattle_Move.rb:96}) has <b>no flag
 *     letter</b>: the base body returns false and only
 *     {@code PokeBattle_RecoilMove} ({@code Move_Effects_Generic.rb:592-593})
 *     overrides it, i.e. it is decided by the function code. It is therefore
 *     asked through the ported strategy table:
 *     {@code MoveEffectRegistry.of(move.function()).recoilMove(move)} - false
 *     today (no function code is registered yet) means "not modelled", never an
 *     invented answer.</li>
 * <li>{@code move.addlEffect} ({@code PokeBattle_Move.rb:13/39}) is
 *     {@code move.additionalChance()} (MoveEffectBase:919-924).</li>
 * <li>{@code target.movedThisRound?} ({@code PokeBattle_Battler:704}) has no
 *     {@code Battler} method yet and is called through the existing stub
 *     {@code MoveFxPendingApi.movedThisRound} (Lead decision: one stub layer
 *     fewer, not a second copy).</li>
 * <li>{@code user.totalhp} is {@code user.maxHp()} ({@code PokeBattle_Battler}'s
 *     {@code totalhp} is the computed maximum); {@code user.stages[s]} with
 *     {@code s} a {@code PBStats} id is {@code user.stage(s)}
 *     ({@code Battler.stage(int)} exists because the runtime array is a 5-element
 *     shifted layout); {@code type<0} (no type) is {@code type == null}, the
 *     runtime's documented "-1" for a type (see {@code BattleMove.calcType}).</li>
 * <li>Ruby {@code (x).round} is {@code Math.round(x)}; the plugin's two entries
 *     that omit the {@code .round} ({@code DRAGONSMAW:1196},
 *     {@code TRANSISTOR:1202}) keep omitting it - the float is assigned
 *     verbatim, as in Ruby.</li>
 * </ul>
 *
 * <p>The load order is fixed by {@link BattleHandlerRegistry} and must not be
 * changed here.</p>
 */
final class AbilitiesDamageUser {

    private AbilitiesDamageUser() {
    }

    /** {@code BattleHandlers_Abilities.rb}: 72 entries (roster §1.2, §2 main table). */
    static void register() {
        // ==================================================================
        // DamageCalcUserAbility (:925-1204) + copies :931/:999/:1012/:1031
        // ==================================================================

        BattleHandlers.DamageCalcUserAbility.add("AERILATE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:925-932 (roster §1.2 span; body :925-929, copy :931)
            if (move.powerBoost()) {                                         // :927
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;                 // :927
            }
        });
        BattleHandlers.DamageCalcUserAbility.copy(                            // :931
                "AERILATE", "PIXILATE", "REFRIGERATE", "GALVANIZE", "DRAGONSKIN");

        BattleHandlers.DamageCalcUserAbility.add("ANALYTIC", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:933-942 (roster §1.2 span; body :933-941)
            Object action = target.battle.choices(target.index)[0];           // :935-936 @choices[..][0]
            if ((!":UseMove".equals(action) && !":Shift".equals(action))      // :935-936
                    || target.movedThisRound()) {             // :937
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.3f;                  // :938
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("BLAZE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:943-950
            if (user.hp <= user.maxHp() / 3 && "FIRE".equals(type)) {         // :945 user.hp<=user.totalhp/3 && isConst?(type,PBTypes,:FIRE)
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :946
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("DEFEATIST", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:951-958
            if (user.hp <= user.maxHp() / 2) {                                // :953 user.hp<=user.totalhp/2
                mults[BattleHandlers.ATK_MULT] /= 2f;                         // :953
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("FLAREBOOST", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:959-966
            if (user.burned() && "Special".equals(move.category())) {         // :961 move.specialMove?
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;                  // :962
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("FLASHFIRE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:967-974
            if (user.effects.truthy(PBEffects.Battler.FlashFire)              // :969 user.effects[PBEffects::FlashFire]
                    && "FIRE".equals(type)) {                                 // :969 isConst?(type,PBTypes,:FIRE)
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :970
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("FLOWERGIFT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:975-984
            int w = user.battle.pbWeather();                                  // :977
            if (move.physical() && (w == PBWeather.Sun || w == PBWeather.HarshSun)   // :978
                    && !target.hasUtilityUmbrella()) {                        // :979
                mults[BattleHandlers.ATK_MULT] = Math.round(mults[BattleHandlers.ATK_MULT] * 1.5f);  // :980 (…*1.5).round
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("GUTS", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:985-992
            if (user.pbHasAnyStatus() && move.physical()) {                   // :987
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :988
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("HUGEPOWER", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:993-1000
            if (move.physical()) {                                            // :995
                mults[BattleHandlers.ATK_MULT] *= 2f;                         // :995
            }
        });
        BattleHandlers.DamageCalcUserAbility.copy(                            // :999
                "HUGEPOWER", "PUREPOWER", "SAVAGECEREMONY");

        BattleHandlers.DamageCalcUserAbility.add("HUSTLE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1001-1006
            if (move.physical()) {                                            // :1003
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :1003
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("IRONFIST", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1007-1013
            if (move.flags().contains("j")) {                                 // :1009 punchingMove? = @flags[/j/]
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;                  // :1009
            }
        });
        BattleHandlers.DamageCalcUserAbility.copy("IRONFIST", "SHATTERFIST");  // :1012

        BattleHandlers.DamageCalcUserAbility.add("MEGALAUNCHER", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1014-1019
            if (move.flags().contains("m")) {                                 // :1016 pulseMove? = @flags[/m/]
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;                  // :1016
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("MINUS", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1020-1032
            if (!"Special".equals(move.category())) {                         // :1022 next if !move.specialMove?
                return;
            }
            for (Battler b : user.allAllies()) {                              // :1023 user.eachAlly do |b| (same set, Battler:1193-1211)
                if (!b.hasActiveAbility(new String[] {"MINUS", "PLUS"})) {    // :1024 b.hasActiveAbility?([:MINUS,:PLUS])
                    continue;
                }
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :1025
                break;                                                        // :1026
            }
        });
        BattleHandlers.DamageCalcUserAbility.copy("MINUS", "PLUS");            // :1031

        BattleHandlers.DamageCalcUserAbility.add("NEUROFORCE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1033-1040
            if (PBTypes.superEffective(target.damageState.typeMod)) {         // :1035 PBTypes.superEffective?(target.damageState.typeMod)
                mults[BattleHandlers.FINAL_DMG_MULT] *= 1.25f;                // :1036
            }
        });

        BattleHandlers.DamageCalcUserAbility.copy("NEUROFORCE", "COMBATMACHINE");   // project addition (see AbilitiesDamageTarget)

        BattleHandlers.DamageCalcUserAbility.add("OVERGROW", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1041-1048
            if (user.hp <= user.maxHp() / 3 && "GRASS".equals(type)) {        // :1043
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :1044
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("RECKLESS", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1049-1054
            if (MoveEffectRegistry.of(move.function()).recoilMove(move)) {    // :1051 move.recoilMove? (function code, no flag)
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;                  // :1051
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("RIVALRY", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1055-1066
            if (user.gender() != 2 && target.gender() != 2) {                 // :1057
                if (user.gender() == target.gender()) {                       // :1058
                    mults[BattleHandlers.BASE_DMG_MULT] *= 1.25f;             // :1059
                } else {
                    mults[BattleHandlers.BASE_DMG_MULT] *= 0.75f;             // :1061
                }
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("SANDFORCE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1067-1077
            if (user.battle.pbWeather() == PBWeather.Sandstorm                    // :1069
                    && ("ROCK".equals(type) || "GROUND".equals(type)              // :1070-1071
                    || "STEEL".equals(type))) {                                   // :1072
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.3f;                  // :1073
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("SHEERFORCE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1078-1083
            if (move.additionalChance() > 0) {                                // :1080 move.addlEffect>0
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.3f;                  // :1080
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("SLOWSTART", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1084-1089
            if (user.effects.intVal(PBEffects.Battler.SlowStart) > 0              // :1086 user.effects[PBEffects::SlowStart]>0
                    && move.physical()) {                                     // :1086 && move.physicalMove?
                mults[BattleHandlers.ATK_MULT] /= 2f;                         // :1086
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("SOLARPOWER", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1090-1099
            int w = user.battle.pbWeather();                                  // :1092
            if ("Special".equals(move.category())                             // :1093 move.specialMove?
                    && (w == PBWeather.Sun || w == PBWeather.HarshSun)        // :1093
                    && !target.hasUtilityUmbrella()) {                        // :1094
                mults[BattleHandlers.ATK_MULT] = Math.round(mults[BattleHandlers.ATK_MULT] * 1.5f);  // :1095 (…*1.5).round
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("SNIPER", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1100-1107
            if (target.damageState.critical) {                                // :1102
                mults[BattleHandlers.FINAL_DMG_MULT] *= 1.5f;                 // :1103
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("STAKEOUT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1108-1113
            if (":SwitchOut".equals(target.battle.choices(target.index)[0])) { // :1110 @choices[target.index][0]==:SwitchOut
                mults[BattleHandlers.ATK_MULT] *= 2f;                         // :1110
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("STEELWORKER", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1114-1119
            if ("STEEL".equals(type)) {                                       // :1116
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :1116
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("STRONGJAW", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1120-1125
            if (move.flags().contains("i")) {                                 // :1122 bitingMove? = @flags[/i/]
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;                  // :1122
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("SWARM", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1126-1133
            if (user.hp <= user.maxHp() / 3 && "BUG".equals(type)) {          // :1128
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :1129
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("TECHNICIAN", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1134-1141
            if (user.index != target.index && move.id() > 0                     // :1136
                    && baseDmg * mults[BattleHandlers.BASE_DMG_MULT] <= 60) {  // :1136
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;                  // :1137
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("TINTEDLENS", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1142-1147
            if (PBTypes.resistant(target.damageState.typeMod)) {              // :1144 PBTypes.resistant?(target.damageState.typeMod)
                mults[BattleHandlers.FINAL_DMG_MULT] *= 2f;                   // :1144
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("TORRENT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1148-1155
            if (user.hp <= user.maxHp() / 3 && "WATER".equals(type)) {        // :1150
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :1151
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("TOUGHCLAWS", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1156-1161
            if (move.flags().contains("a")) {                                 // :1158 contactMove? = @flags[/a/]
                mults[BattleHandlers.BASE_DMG_MULT] *= 4 / 3.0f;              // :1158
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("TOXICBOOST", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1162-1169
            if (user.poisoned() && move.physical()) {                         // :1164
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;                  // :1165
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("WATERBUBBLE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1170-1175
            if ("WATER".equals(type)) {                                       // :1172
                mults[BattleHandlers.ATK_MULT] *= 2f;                         // :1172
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("GORILLATACTICS", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1176-1181
            if (move.physical()) {                                            // :1178
                mults[BattleHandlers.ATK_MULT] = Math.round(mults[BattleHandlers.ATK_MULT] * 1.5f);  // :1178 (…*1.5).round
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("PUNKROCK", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1182-1187
            if (move.flags().contains("k")) {                                 // :1184 soundMove? = @flags[/k/]
                mults[BattleHandlers.BASE_DMG_MULT] =
                        Math.round(mults[BattleHandlers.BASE_DMG_MULT] * 1.3f);   // :1184 (…*1.3).round
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("STEELYSPIRIT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1188-1193
            if ("STEEL".equals(type)) {                                       // :1190
                mults[BattleHandlers.BASE_DMG_MULT] =
                        Math.round(mults[BattleHandlers.BASE_DMG_MULT] * 1.5f);   // :1190 (…*1.5).round
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("DRAGONSMAW", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1194-1199
            if ("DRAGON".equals(type)) {                                      // :1196
                // :1196 has no .round (unlike its siblings) - the float is assigned verbatim, as in Ruby
                mults[BattleHandlers.ATK_MULT] = mults[BattleHandlers.ATK_MULT] * 1.5f;
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("TRANSISTOR", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1200-1209
            if ("ELECTRIC".equals(type)) {                                    // :1202
                // :1202 has no .round (unlike its siblings) - the float is assigned verbatim, as in Ruby
                mults[BattleHandlers.ATK_MULT] = mults[BattleHandlers.ATK_MULT] * 1.5f;
            }
        });

        // ==================================================================
        // DamageCalcUserAllyAbility (:1210-1238)
        // ==================================================================

        BattleHandlers.DamageCalcUserAllyAbility.add("BATTERY", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1210-1216
            if (!"Special".equals(move.category())) {                         // :1212 next if !move.specialMove?
                return;
            }
            mults[BattleHandlers.FINAL_DMG_MULT] *= 1.3f;                     // :1213
        });

        BattleHandlers.DamageCalcUserAllyAbility.add("FLOWERGIFT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1217-1226
            int w = user.battle.pbWeather();                                  // :1219
            if (move.physical() && (w == PBWeather.Sun || w == PBWeather.HarshSun)   // :1220
                    && !target.hasUtilityUmbrella()) {                        // :1221
                mults[BattleHandlers.ATK_MULT] = Math.round(mults[BattleHandlers.ATK_MULT] * 1.5f);  // :1222 (…*1.5).round
            }
        });

        BattleHandlers.DamageCalcUserAllyAbility.add("POWERSPOT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1227-1232
            mults[BattleHandlers.FINAL_DMG_MULT] =
                    Math.round(mults[BattleHandlers.FINAL_DMG_MULT] * 1.3f);  // :1229 (…*1.3).round
        });

        BattleHandlers.DamageCalcUserAllyAbility.add("STEELYSPIRIT", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:1233-1242
            if ("STEEL".equals(type)) {                                       // :1235
                mults[BattleHandlers.BASE_DMG_MULT] =
                        Math.round(mults[BattleHandlers.BASE_DMG_MULT] * 1.5f);   // :1235 (…*1.5).round
            }
        });

        // ==================================================================
        // DamageCalcUserAbility, later entries (:2914-4558, Ruby order)
        // ==================================================================

        BattleHandlers.DamageCalcUserAbility.add("SHARPNESS", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:2914-2922 (锋锐)
            if (move.flags().contains("p")) {                                 // :2916 slicingMove? = @flags[/p/]
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;                  // :2916
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("ROCKYPAYLOAD", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:2938-2944 (搬岩)
            if ("ROCK".equals(type)) {                                        // :2940
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :2940
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("SUPREMEOVERLORD", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3116-3123 (大将)
            int bonus = user.effects.intVal(PBEffects.Battler.SupremeOverlord);   // :3118
            if (bonus <= 0) {                                                 // :3119
                return;
            }
            mults[BattleHandlers.BASE_DMG_MULT] *= (1 + (0.1f * bonus));      // :3120
        });

        BattleHandlers.DamageCalcUserAbility.add("ORICHALCUMPULSE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3337-3345
            if (move.physical()                                             // :3339
                    && (user.effectiveWeather() == PBWeather.Sun              // :3339 [Sun, HarshSun].include?(user.effectiveWeather)
                    || user.effectiveWeather() == PBWeather.HarshSun)) {
                mults[BattleHandlers.ATK_MULT] *= 4 / 3.0f;                   // :3340
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("HADRONENGINE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3358-3367 (强子引擎)
            if ("Special".equals(move.category())                           // :3360 move.specialMove?
                    && (user.effectiveWeather() == PBWeather.Sun              // :3360 [Sun, HarshSun].include?(user.effectiveWeather)
                    || user.effectiveWeather() == PBWeather.HarshSun)) {
                mults[BattleHandlers.ATK_MULT] *= 4 / 3.0f;                   // :3361
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("PROTOSYNTHESIS", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3536-3546 (roster §1.2 span; body :3536-3543, copy :3544)
            if (user.effects.truthy(PBEffects.Battler.Transform)) {           // :3538
                return;
            }
            int stat = user.effects.intVal(PBEffects.Battler.ParadoxStat);    // :3539
            if (move.physical() && stat == PBStats.ATTACK) {                  // :3540
                mults[BattleHandlers.ATK_MULT] *= 1.3f;                       // :3540
            }
            if ("Special".equals(move.category()) && stat == PBStats.SPATK) { // :3541
                mults[BattleHandlers.ATK_MULT] *= 1.3f;                       // :3541
            }
        });
        BattleHandlers.DamageCalcUserAbility.copy("PROTOSYNTHESIS", "QUARKDRIVE");   // :3544

        BattleHandlers.DamageCalcUserAbility.add("BROKENBREATH", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3578-3584
            if ("Special".equals(move.category())) {                          // :3580
                mults[BattleHandlers.ATK_MULT] *= 2f;                         // :3581
            }
        });

        BattleHandlers.DamageCalcUserAllyAbility.add("BROKENBREATH", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3585-3591
            if ("Special".equals(move.category())) {                          // :3587
                mults[BattleHandlers.ATK_MULT] *= 2f;                         // :3588
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("THUNDERCLOUD", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3615-3622 (雷云)
            if (user.battle.pbWeather() == PBWeather.Rain && "ELECTRIC".equals(type)) {   // :3617-3618
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;                  // :3619
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("EERIEBODY", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3633-3639 (灵异躯体)
            if ("DARK".equals(type) || "PSYCHIC".equals(type)) {              // :3635
                // :3635 has no .round - the float is assigned verbatim, as in Ruby
                mults[BattleHandlers.ATK_MULT] = mults[BattleHandlers.ATK_MULT] * 1.3f;
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("ZEROSUMBODY", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3640-3645 (零和躯体)
            if ("LIGHT".equals(type) || "PSYCHIC".equals(type)) {             // :3642
                // :3642 has no .round - the float is assigned verbatim, as in Ruby
                mults[BattleHandlers.ATK_MULT] = mults[BattleHandlers.ATK_MULT] * 1.3f;
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("FAIRYSONG", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3677-3684 (妖醒之歌)
            if (move.flags().contains("k")) {                                 // :3679 soundMove? = @flags[/k/]
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;                  // :3679
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("STARFISSURE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3701-3706 (星界裂隙)
            if ("VOID".equals(type)) {                                        // :3703
                // :3703 has no .round - the float is assigned verbatim, as in Ruby
                mults[BattleHandlers.ATK_MULT] = mults[BattleHandlers.ATK_MULT] * 1.3f;
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("DEMONKILLER", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3767-3777 (皆杀恶魔)
            if (move.flags().contains("a")) {                                 // :3769 contactMove? = @flags[/a/]
                mults[BattleHandlers.BASE_DMG_MULT] *= 2f;                    // :3769
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("ELIMINATEEVIL", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3813-3822 (除恶)
            if (!target.pbHasType("GHOST") && !target.pbHasType("DARK")) {    // :3815 next unless target.pbHasType?(:GHOST) || ...(:DARK)
                return;
            }
            if (move.statusMove()) {                                          // :3816
                return;
            }
            // :3817 type<0 || !PBTypes.superEffective?(target.damageState.typeMod);
            //        "type<0" is this runtime's type==null (BattleMove.calcType's documented -1)
            if (type == null || !PBTypes.superEffective(target.damageState.typeMod)) {
                return;
            }
            mults[BattleHandlers.FINAL_DMG_MULT] *= 2f;                       // :3818
        });

        BattleHandlers.DamageCalcUserAbility.add("SOUNDSTRIDE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3823-3827 (音跃节拍)
            if ("BUG".equals(type)) {                                         // :3825
                // :3825 has no .round - the float is assigned verbatim, as in Ruby
                mults[BattleHandlers.ATK_MULT] = mults[BattleHandlers.ATK_MULT] * 1.3f;
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("RUYIBLADE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3835-3840 (如意神兵)
            if (PBTypes.resistant(target.damageState.typeMod)) {              // :3837
                mults[BattleHandlers.FINAL_DMG_MULT] *= 2f;                   // :3837
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("ROSEGARDEN", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3980-3988 (蔷薇花园)
            if (user.battle.field.terrain == PBBattleTerrains.Grassy             // :3982
                    && "FIRE".equals(type)) {                                 // :3983
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;                  // :3984
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("ROSEGARDEN", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:3997-4007 (蔷薇箱庭) - the plugin registers
            // ROSEGARDEN twice (:3980 and :3997); the second add overwrites the first
            if (user.battle.field.terrain == PBBattleTerrains.Grassy             // :3999
                    && "ICE".equals(type)) {                                  // :4000
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;                  // :4001
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("ANGERFLAME", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4084-4099 (怒炎)
            if (!move.physical()) {                                           // :4086
                return;
            }
            boolean lossStages = false;                                       // :4087
            for (int s : PBStats.EACH_MAIN_BATTLE_STAT) {                     // :4088 PBStats.eachMainBattleStat { |s|
                if (user.stage(s) < 0) {                                      // :4089 user.stages[s] < 0
                    lossStages = true;                                        // :4090
                    break;                                                    // :4091
                }
            }
            if (user.pbHasAnyStatus() || lossStages) {                        // :4094
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :4094
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("ENERGYACCU", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4126-4136 (聚能)
            if ("STEEL".equals(type) || "GRASS".equals(type) || "ROCK".equals(type)) {   // :4128-4130
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.3f;                  // :4131
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("CONFLUENCE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4137-4147 (汇流)
            if ("WATER".equals(type) || "FIRE".equals(type) || "ELECTRIC".equals(type)) {   // :4139-4141
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.3f;                  // :4142
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("GHASTLYWAIL", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4256-4266
            if (move.flags().contains("k")) {                                 // :4258 soundMove? = @flags[/k/]
                mults[BattleHandlers.BASE_DMG_MULT] =
                        Math.round(mults[BattleHandlers.BASE_DMG_MULT] * 1.5f);   // :4258 (…*1.5).round
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("SUPERSUN", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4275-4286 (超级太阳)
            if (user.battle.pbWeather() == PBWeather.Sun                         // :4278
                    || user.battle.pbWeather() == PBWeather.HarshSun) {
                return;
            }
            if ("FIRE".equals(type)) {                                        // :4280
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :4281
            } else if ("WATER".equals(type)) {                                // :4282
                mults[BattleHandlers.ATK_MULT] /= 2f;                         // :4283
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("RAGEFIREBLAST", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4322-4328 (怒炎焚击)
            if (move.flags().contains("a")) {                                 // :4324 contactMove? = @flags[/a/]
                mults[BattleHandlers.BASE_DMG_MULT] *= 4 / 3.0f;              // :4324
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("SOARINGSTAGE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4355-4367 (高歌舞台)
            mults[BattleHandlers.FINAL_DMG_MULT] =
                    Math.round(mults[BattleHandlers.FINAL_DMG_MULT] * 1.3f);  // :4357 (…*1.3).round
        });

        BattleHandlers.DamageCalcUserAbility.add("PIERCINGDRILL", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4479-4486
            if (!move.flags().contains("a")) {                                // :4481 next if !move.contactMove? = @flags[/a/]
                return;
            }
            mults[BattleHandlers.FINAL_DMG_MULT] /= 4.0f;                     // :4482
        });

        BattleHandlers.DamageCalcUserAbility.add("FIREMANE", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4487-4493 (火焰鬃毛)
            if ("FIRE".equals(type)) {                                        // :4489
                mults[BattleHandlers.ATK_MULT] *= 1.5f;                       // :4489
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("PLAYFULHEART", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4538-4544
            if (move.flags().contains("a")) {                                 // :4540 contactMove? = @flags[/a/]
                mults[BattleHandlers.BASE_DMG_MULT] *= 4 / 3.0f;              // :4540
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("WANXIANG", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4545-4549 (万相乖离) - the plugin registers
            // WANXIANG twice (:4545 and :4550); the second add overwrites the first
            if (user.hp <= user.maxHp() / 2) {                                // :4547 user.hp<=user.totalhp/2
                mults[BattleHandlers.ATK_MULT] /= 2f;                         // :4547
            }
        });

        BattleHandlers.DamageCalcUserAbility.add("WANXIANG", (ability, user, target, move, mults, baseDmg, type) -> {
            // BattleHandlers_Abilities.rb:4550-4558 (万相乖离)
            if (move.physical()) {                                            // :4552
                mults[BattleHandlers.ATK_MULT] *= 2f;                         // :4552
            }
        });
    }
}
