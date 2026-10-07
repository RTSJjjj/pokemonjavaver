package pokemon.runtime.battle;

/**
 * Stage 4 / M0: the registration entry point that fixes Ruby's <b>load
 * order</b> for every {@code BattleHandlers::*} table.
 *
 * <h2>Why order is semantic</h2>
 * {@code HandlerHash#add} assigns straight into the table
 * ({@code Event_Handlers.rb:110-113}), so re-adding the same key replaces the
 * handler. The project loads, in this order
 * ({@code __sections.mjs --list}):
 * <ol>
 * <li>{@code BattleHandlers} (675 lines) - the declarations;</li>
 * <li>{@code BattleHandlers_Abilities} (4585);</li>
 * <li>{@code BattleHandlers_Items} (1804);</li>
 * <li>{@code Arceus} (1024);</li>
 * <li>{@code 场地} (762).</li>
 * </ol>
 * The last two register 16 more entries (roster §9): 10 of them overwrite a
 * main-table entry ({@code Arceus:775} INSOMNIA, {@code :782} SWEETVEIL,
 * {@code :790} MAGMAARMOR, {@code :798} INSOMNIA, {@code :810} SYNCHRONIZE,
 * {@code :871} HEALER, {@code :902} HYDRATION, {@code :934} SHEDSKIN,
 * {@code :970} CHESTOBERRY, {@code :983} LUMBERRY) and 6 add new ones
 * ({@code 场地:597} BUGLURESEED, {@code :614} COLDSEED, {@code :707}
 * FROZENARMOR, {@code :720} TRAPTRICK, {@code :735} BUGLURESURGE, {@code :751}
 * COLDSURGE). Java's static-initialiser order is not controllable, so the order
 * is written out here instead of relying on field order in the registration
 * files.
 *
 * <h2>Line ranges in the comments</h2>
 * Each {@code register()} is credited with the entry count and span subtotal of
 * {@code stage4-l1-handler-roster.md} §1.2, and the nominal cumulative range
 * those subtotals produce. The registration lines are <b>not</b> contiguous in
 * the Ruby files (the roster's §2 main table is ordered by suggested file, and
 * e.g. {@code AbilityOnSwitchIn} entries appear throughout
 * {@code BattleHandlers_Abilities}), so the ranges are nominal spans, not
 * literal Ruby ranges.
 *
 * <h2>Status</h2>
 * The 13 {@code register()} bodies are empty skeletons for the L1' pass; this
 * class exists so that the order is already frozen in code.
 */
final class BattleHandlerRegistry {

    private BattleHandlerRegistry() {
    }

    /** Runs every registration entry once, in Ruby load order. */
    static void init() {
        // BattleHandlers_Abilities.rb, 91 entries, span 1174 (roster §1.2) - nominal :1-1174
        AbilitiesSwitchIn.register();
        // BattleHandlers_Abilities.rb, 72 entries, span 1190 (roster §1.2) - nominal :1175-2364
        AbilitiesOnHit.register();
        // BattleHandlers_Abilities.rb, 72 entries, span 590 (roster §1.2) - nominal :2365-2954
        AbilitiesDamageUser.register();
        // BattleHandlers_Abilities.rb, 47 entries, span 623 (roster §1.2) - nominal :2955-3577
        AbilitiesSpeedEor.register();
        // BattleHandlers_Abilities.rb, 38 entries, span 510 (roster §1.2) - nominal :3578-4087
        AbilitiesStatus.register();
        // BattleHandlers_Abilities.rb, 32 entries, span 249 (roster §1.2) - nominal :4088-4336
        AbilitiesAccuracyCritType.register();
        // BattleHandlers_Abilities.rb, 28 entries, span 245 (roster §1.2) - nominal :4337-4581
        AbilitiesDamageTarget.register();
        // BattleHandlers_Items.rb, 61 entries, span 436 (roster §1.2) - nominal :1-436
        ItemsDamageUser.register();
        // BattleHandlers_Items.rb, 38 entries, span 509 (roster §1.2) - nominal :437-945
        ItemsHealStatus.register();
        // BattleHandlers_Items.rb, 31 entries, span 234 (roster §1.2) - nominal :946-1179
        ItemsDamageTarget.register();
        // BattleHandlers_Items.rb, 28 entries, span 283 (roster §1.2) - nominal :1180-1462
        ItemsFieldSpeed.register();
        // BattleHandlers_Items.rb, 21 entries, span 338 (roster §1.2) - nominal :1463-1800
        ItemsOnHit.register();
        // Arceus:775-983 (10 overrides) then 场地:597-751 (6 additions) - roster §9.
        // MUST run after the two main tables, or the overrides are lost.
        HandlerArceusOverrides.register();
    }
}
