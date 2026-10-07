package pokemon.runtime.battle;

import pokemon.runtime.pokemon.PbsData;

/**
 * Stage 4 / L1': the {@code BattleHandlers_Abilities.rb} registrations assigned
 * to this file by {@code stage4-l1-handler-roster.md} §1.2 - <b>32 entries</b>
 * (…), plus the 2 {@code .copy} lines of roster §5 that land here.
 *
 * <p>Groups (roster §1.3): {@code MoveBaseTypeModifierAbility} 11,
 * {@code AccuracyCalcUserAbility} 8 + 1 copy, {@code AccuracyCalcUserAllyAbility}
 * 1, {@code AccuracyCalcTargetAbility} 8, {@code CriticalCalcUserAbility} 3,
 * {@code CriticalCalcTargetAbility} 1 + 1 copy. Every entry is 可转 (no
 * 降级/登记, roster §7.1).</p>
 *
 * <h2>Two documented translations</h2>
 * <ul>
 * <li>{@code isConst?(val,PBTypes,:X)} ({@code PSystem_Utilities:205-212}) compares
 *     the value against the compiled constant; this runtime's type identity IS
 *     the internal name, so it is written {@code "X".equals(val)} - the same
 *     comparison the plugin's compiled {@code PBTypes} class performs
 *     (Compiler_PBS:443-445).</li>
 * <li>{@code hasConst?(PBTypes,:X)} ({@code PSystem_Utilities:214-217}) asks
 *     whether the compiled class defines that constant, i.e. whether PBS/types.txt
 *     defines the type; here it is {@link #hasType} against
 *     {@code generated/pbs/types.json}.</li>
 * </ul>
 *
 * <p>The load order is fixed by {@link BattleHandlerRegistry} and must not be
 * changed here.</p>
 */
final class AbilitiesAccuracyCritType {

    private AbilitiesAccuracyCritType() {
    }

    /** {@code BattleHandlers_Abilities.rb}: 32 entries (roster §1.2, §2 main table). */
    static void register() {
        // ==================================================================
        // MoveBaseTypeModifierAbility (:754-811)
        // ==================================================================

        BattleHandlers.MoveBaseTypeModifierAbility.add("AERILATE", (ability, user, move, type) -> {
            // BattleHandlers_Abilities.rb:754-761
            if (!"NORMAL".equals(type) || !hasType(user, "FLYING")) {        // :756
                return null;
            }
            move.setPowerBoost(true);                                        // :757
            return "FLYING";                                                 // :758
        });

        BattleHandlers.MoveBaseTypeModifierAbility.add("GALVANIZE", (ability, user, move, type) -> {
            // BattleHandlers_Abilities.rb:762-769
            if (!"NORMAL".equals(type) || !hasType(user, "ELECTRIC")) {      // :764
                return null;
            }
            move.setPowerBoost(true);                                        // :765
            return "ELECTRIC";                                               // :766
        });

        BattleHandlers.MoveBaseTypeModifierAbility.add("LIQUIDVOICE", (ability, user, move, type) -> {
            // BattleHandlers_Abilities.rb:770-775
            if (hasType(user, "WATER") && move.flags().contains("k")) {      // :772 hasConst? && soundMove?
                return "WATER";
            }
            return null;
        });

        BattleHandlers.MoveBaseTypeModifierAbility.add("NORMALIZE", (ability, user, move, type) -> {
            // BattleHandlers_Abilities.rb:776-783
            if (!hasType(user, "NORMAL")) {                                  // :778
                return null;
            }
            if (Battle.NEWEST_BATTLE_MECHANICS) {                            // :779 NEWEST_BATTLE_MECHANICS (Settings:160)
                move.setPowerBoost(true);
            }
            return "NORMAL";                                                 // :780
        });

        BattleHandlers.MoveBaseTypeModifierAbility.add("PIXILATE", (ability, user, move, type) -> {
            // BattleHandlers_Abilities.rb:784-791
            if (!"NORMAL".equals(type) || !hasType(user, "FAIRY")) {         // :786
                return null;
            }
            move.setPowerBoost(true);                                        // :787
            return "FAIRY";                                                  // :788
        });

        BattleHandlers.MoveBaseTypeModifierAbility.add("REFRIGERATE", (ability, user, move, type) -> {
            // BattleHandlers_Abilities.rb:792-800
            if (!"NORMAL".equals(type) || !hasType(user, "ICE")) {           // :794
                return null;
            }
            move.setPowerBoost(true);                                        // :795
            return "ICE";                                                    // :796
        });

        BattleHandlers.MoveBaseTypeModifierAbility.add("DRAGONSKIN", (ability, user, move, type) -> {
            // BattleHandlers_Abilities.rb:801-811
            if (!"NORMAL".equals(type) || !hasType(user, "DRAGON")) {        // :803
                return null;
            }
            move.setPowerBoost(true);                                        // :804
            return "DRAGON";                                                 // :805
        });

        // ==================================================================
        // AccuracyCalcUserAbility (:812-851) + its copy (:3113)
        // ==================================================================

        BattleHandlers.AccuracyCalcUserAbility.add("COMPOUNDEYES", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:812-817
            mods[BattleHandlers.ACC_MULT] *= 1.3f;                           // :814
        });

        BattleHandlers.AccuracyCalcUserAbility.add("HUSTLE", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:818-823
            if (move.physical()) {                                           // :820 physicalMove? = @category==0 (Settings:159)
                mods[BattleHandlers.ACC_MULT] *= 0.8f;
            }
        });

        BattleHandlers.AccuracyCalcUserAbility.add("KEENEYE", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:824-829
            if (mods[BattleHandlers.EVA_STAGE] > 0 && Battle.NEWEST_BATTLE_MECHANICS) {   // :826
                mods[BattleHandlers.EVA_STAGE] = 0;
            }
        });

        BattleHandlers.AccuracyCalcUserAbility.add("ROSYAEGIS", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:830-834 (蔷薇庇护)
            if (mods[BattleHandlers.EVA_STAGE] > 0 && Battle.NEWEST_BATTLE_MECHANICS) {   // :832
                mods[BattleHandlers.EVA_STAGE] = 0;
            }
        });

        BattleHandlers.AccuracyCalcUserAbility.add("NOGUARD", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:835-840
            mods[BattleHandlers.BASE_ACC] = 0;                               // :837
        });

        BattleHandlers.AccuracyCalcUserAbility.add("UNAWARE", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:841-846
            if (!move.statusMove()) {                                        // :843 damagingMove? = @category!=2 (PokeBattle_Move.rb:90)
                mods[BattleHandlers.EVA_STAGE] = 0;
            }
        });

        BattleHandlers.AccuracyCalcUserAbility.add("VICTORYSTAR", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:847-856
            mods[BattleHandlers.ACC_MULT] *= 1.1f;                           // :849
        });

        // 心眼 (BattleHandlers_Abilities.rb:3111-3113)
        BattleHandlers.AccuracyCalcUserAbility.copy("KEENEYE", "MINDSEYE");   // :3113

        // ==================================================================
        // AccuracyCalcUserAllyAbility (:857-861)
        // ==================================================================

        BattleHandlers.AccuracyCalcUserAllyAbility.add("VICTORYSTAR", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:857-866
            mods[BattleHandlers.ACC_MULT] *= 1.1f;                           // :859
        });

        // ==================================================================
        // AccuracyCalcTargetAbility (:867-924)
        // ==================================================================

        BattleHandlers.AccuracyCalcTargetAbility.add("LIGHTNINGROD", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:867-872
            if ("ELECTRIC".equals(type)) {                                   // :869
                mods[BattleHandlers.BASE_ACC] = 0;
            }
        });

        BattleHandlers.AccuracyCalcTargetAbility.add("NOGUARD", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:873-878
            mods[BattleHandlers.BASE_ACC] = 0;                               // :875
        });

        BattleHandlers.AccuracyCalcTargetAbility.add("SANDVEIL", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:879-886
            if (target.battle.pbWeather() == PBWeather.Sandstorm) {          // :881
                mods[BattleHandlers.EVA_MULT] *= 1.25f;                      // :882
            }
        });

        BattleHandlers.AccuracyCalcTargetAbility.add("SNOWCLOAK", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:887-894
            if (target.battle.pbWeather() == PBWeather.Hail                      // :889
                    || target.battle.pbWeather() == PBWeather.Snow) {
                mods[BattleHandlers.EVA_MULT] *= 1.2f;                       // :890
            }
        });

        BattleHandlers.AccuracyCalcTargetAbility.add("STORMDRAIN", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:895-900
            if ("WATER".equals(type)) {                                      // :897
                mods[BattleHandlers.BASE_ACC] = 0;
            }
        });

        BattleHandlers.AccuracyCalcTargetAbility.add("TANGLEDFEET", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:901-906
            if (target.effects.intVal(PBEffects.Battler.Confusion) > 0) {    // :903
                mods[BattleHandlers.ACC_MULT] /= 2f;
            }
        });

        BattleHandlers.AccuracyCalcTargetAbility.add("UNAWARE", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:907-912
            if (!move.statusMove()) {                                        // :909 damagingMove? = @category!=2
                mods[BattleHandlers.ACC_STAGE] = 0;
            }
        });

        BattleHandlers.AccuracyCalcTargetAbility.add("WONDERSKIN", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:913-924
            if (move.statusMove() && user.opposes(target)) {                 // :915
                if (mods[BattleHandlers.BASE_ACC] > 50) {                    // :916
                    mods[BattleHandlers.BASE_ACC] = 0;
                }
            }
        });

        // ==================================================================
        // CriticalCalcUserAbility (:1405-1415, :3732-3738)
        // ==================================================================

        BattleHandlers.CriticalCalcUserAbility.add("MERCILESS", (ability, user, target, c) -> {
            // BattleHandlers_Abilities.rb:1405-1410
            if (target.poisoned()) {                                         // :1407
                return 99;
            }
            return null;
        });

        BattleHandlers.CriticalCalcUserAbility.add("SUPERLUCK", (ability, user, target, c) -> {
            // BattleHandlers_Abilities.rb:1411-1420
            return c + 1;                                                    // :1413
        });

        // ==================================================================
        // CriticalCalcTargetAbility (:1421-1427)
        // ==================================================================

        BattleHandlers.CriticalCalcTargetAbility.add("BATTLEARMOR", (ability, user, target, c) -> {
            // BattleHandlers_Abilities.rb:1421-1425
            return -1;                                                       // :1423
        });
        BattleHandlers.CriticalCalcTargetAbility.copy(                        // :1427
                "BATTLEARMOR", "SHELLARMOR", "NOBLESTRIKE", "RUYIBLADE");

        // ==================================================================
        // MoveBaseTypeModifierAbility, later entries (:3671-3692, :3924-3932, :4346-4354)
        // ==================================================================

        BattleHandlers.MoveBaseTypeModifierAbility.add("FAIRYSONG", (ability, user, move, type) -> {
            // BattleHandlers_Abilities.rb:3671-3676
            if (hasType(user, "FAIRY") && move.flags().contains("k")) {      // :3673 hasConst? && soundMove?
                return "FAIRY";
            }
            return null;
        });

        BattleHandlers.MoveBaseTypeModifierAbility.add("FAIRYDANCE", (ability, user, move, type) -> {
            // BattleHandlers_Abilities.rb:3685-3692
            if (user.hp <= user.maxHp() / 2                                     // :3687 user.hp<=user.totalhp/2
                    && hasType(user, "FAIRY")                                  // :3688
                    && move.flags().contains("a")) {                           // :3689 contactMove? = @flags[/a/]
                return "FAIRY";
            }
            return null;
        });

        BattleHandlers.CriticalCalcUserAbility.add("BEIMINGBLADE", (ability, user, target, c) -> {
            // BattleHandlers_Abilities.rb:3732-3738
            return c + 1;                                                    // :3734
        });

        BattleHandlers.MoveBaseTypeModifierAbility.add("MERMAIDSOUND", (ability, user, move, type) -> {
            // BattleHandlers_Abilities.rb:3924-3932
            if (!move.flags().contains("k")) {                               // :3926 soundMove?
                return null;
            }
            if (move.statusMove()) {                                         // :3927 pbDamagingMove? = damagingMove? (Move_Usage:38)
                return null;
            }
            if (!hasType(user, "WATER")) {                                   // :3928
                return null;
            }
            return "WATER";                                                  // :3929
        });

        // ==================================================================
        // AccuracyCalcUserAbility, later entry (:4287-4297)
        // ==================================================================

        BattleHandlers.AccuracyCalcUserAbility.add("SUPERSUN", (ability, mods, user, target, move, type) -> {
            // BattleHandlers_Abilities.rb:4287-4297
            if (user.battle.pbWeather() == PBWeather.Sun                          // :4289
                    || user.battle.pbWeather() == PBWeather.HarshSun) {
                return;
            }
            // :4291 isConst?(move.id,PBMoves,:THUNDER)
            if ("THUNDER".equals(move.internalName()) || "HURRICANE".equals(move.internalName())) {
                mods[BattleHandlers.BASE_ACC] = 50;                          // :4292
            }
        });

        // ==================================================================
        // MoveBaseTypeModifierAbility, last entry (:4346-4354)
        // ==================================================================

        BattleHandlers.MoveBaseTypeModifierAbility.add("SOARINGSTAGE", (ability, user, move, type) -> {
            // BattleHandlers_Abilities.rb:4346-4354
            if (!move.flags().contains("k")) {                               // :4348 soundMove?
                return null;
            }
            if (!hasType(user, "FLYING")) {                                  // :4349
                return null;
            }
            return "FLYING";                                                 // :4350
        });
    }

    /**
     * {@code hasConst?(mod,constant)} (PSystem_Utilities:214-217) for
     * {@code PBTypes}: the compiled {@code PBTypes} class defines one constant
     * per PBS/types.txt entry (Compiler_PBS:443-445), so "the constant exists" is
     * "generated/pbs/types.json carries that type" here.
     */
    private static boolean hasType(Battler user, String type) {
        PbsData pbs = user == null || user.battle == null ? null : user.battle.pbs();
        return pbs != null && pbs.types.containsKey(type);
    }
}
