package pokemon.runtime.battle;

import pokemon.runtime.battle.movefx.MoveEffect;
import pokemon.runtime.battle.movefx.MoveEffectRegistry;

/**
 * Stage 4 / L1': the {@code BattleHandlers_Items.rb} registrations assigned to
 * this file by {@code stage4-l1-handler-roster.md} §1.2 - the
 * <b>{@code DamageCalcUserItem} group, 61 entries + 17 {@code copy} lines</b>
 * (78 registrations, roster §2/§5).
 *
 * <h2>Mapping notes</h2>
 * <ul>
 * <li>{@code mults} is a {@code float[]} indexed by
 *     {@code BattleHandlers.BASE_DMG_MULT}/{@code ATK_MULT}/{@code DEF_MULT}/
 *     {@code FINAL_DMG_MULT} (BattleHandlers.rb:539-542), and the plugin's
 *     {@code *= 1.2} becomes {@code *= 1.2f} so nothing is truncated.</li>
 * <li>{@code isConst?(type,PBTypes,:X)} is the type-identity test; this runtime
 *     keys types by internal-name String, so it goes through
 *     {@code PendingApi.isConst(String,String)} exactly like
 *     {@code BattleHandlerHelpers.pbBattleGem} does
 *     (BattleHandlerHelpers.java:211).</li>
 * <li>{@code pbBattleGem(user,:TYPE,move,mults,type)} is the module-level helper
 *     (BattleHandlers.rb:633-643, already landed as
 *     {@link BattleHandlerHelpers#pbBattleGem}).</li>
 * <li>{@code move.physicalMove?} / {@code specialMove?} / {@code punchingMove?}
 *     are {@code PokeBattle_Move} hooks, i.e. the L2 strategy
 *     ({@link MoveEffectRegistry}) - the move object's own method in the plugin,
 *     not a category shortcut.</li>
 * <li>Order follows the Ruby line order, because {@code copy} reads its source's
 *     handler when it runs (Event_Handlers.rb:115-122).</li>
 * </ul>
 */
final class ItemsDamageUser {

    private ItemsDamageUser() {
    }

    /** The move's L2 effect; {@code PokeBattle_Move.pbFromPBMove}'s dispatch (PokeBattle_Move.rb:51-59). */
    private static MoveEffect effectOf(BattleMove move) {
        return MoveEffectRegistry.of(move.function());
    }

    /** {@code BattleHandlers_Items.rb}: 61 entries + 17 copies (roster §1.2, §2, §5). */
    static void register() {
        // ==============================================================
        // DamageCalcUserItem (BattleHandlers_Items.rb:468-853)
        // ==============================================================

        // :468-475 ADAMANTORB
        BattleHandlers.DamageCalcUserItem.add("ADAMANTORB", (item, user, target, move, mults, baseDmg, type) -> {
            // :470-472 if user.isSpecies?(:DIALGA) && (isConst?(type,PBTypes,:DRAGON) || ...:STEEL)
            if (user.isSpecies("DIALGA")
                    && ("DRAGON".equals(type) || "STEEL".equals(type))) {
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;             // :473
            }
        });

        // :477-481 BLACKBELT
        BattleHandlers.DamageCalcUserItem.add("BLACKBELT", (item, user, target, move, mults, baseDmg, type) -> {
            if ("FIGHTING".equals(type)) {                  // :479
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :483 DamageCalcUserItem.copy(:BLACKBELT,:FISTPLATE)
        BattleHandlers.DamageCalcUserItem.copy("BLACKBELT", "FISTPLATE");

        // :485-489 BLACKGLASSES
        BattleHandlers.DamageCalcUserItem.add("BLACKGLASSES", (item, user, target, move, mults, baseDmg, type) -> {
            if ("DARK".equals(type)) {                      // :487
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :491 DamageCalcUserItem.copy(:BLACKGLASSES,:DREADPLATE)
        BattleHandlers.DamageCalcUserItem.copy("BLACKGLASSES", "DREADPLATE");

        // :493-497 BUGGEM
        BattleHandlers.DamageCalcUserItem.add("BUGGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "BUG", move, mults, type);   // :495
        });

        // :499-503 CHARCOAL
        BattleHandlers.DamageCalcUserItem.add("CHARCOAL", (item, user, target, move, mults, baseDmg, type) -> {
            if ("FIRE".equals(type)) {                      // :501
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :505 DamageCalcUserItem.copy(:CHARCOAL,:FLAMEPLATE)
        BattleHandlers.DamageCalcUserItem.copy("CHARCOAL", "FLAMEPLATE");

        // :507-511 CHOICEBAND
        BattleHandlers.DamageCalcUserItem.add("CHOICEBAND", (item, user, target, move, mults, baseDmg, type) -> {
            if (effectOf(move).physicalMove(move, null)) {               // :509 move.physicalMove?
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;
            }
        });

        // :513-517 CHOICESPECS
        BattleHandlers.DamageCalcUserItem.add("CHOICESPECS", (item, user, target, move, mults, baseDmg, type) -> {
            if (effectOf(move).specialMove(move, null)) {                // :515 move.specialMove?
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.5f;
            }
        });

        // :519-523 DARKGEM
        BattleHandlers.DamageCalcUserItem.add("DARKGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "DARK", move, mults, type);   // :521
        });

        // :525-531 DEEPSEATOOTH
        BattleHandlers.DamageCalcUserItem.add("DEEPSEATOOTH", (item, user, target, move, mults, baseDmg, type) -> {
            // :527 if user.isSpecies?(:CLAMPERL) && move.specialMove?
            if (user.isSpecies("CLAMPERL") && effectOf(move).specialMove(move, null)) {
                mults[BattleHandlers.ATK_MULT] *= 2;                     // :528
            }
        });

        // :533-537 DRAGONFANG
        BattleHandlers.DamageCalcUserItem.add("DRAGONFANG", (item, user, target, move, mults, baseDmg, type) -> {
            if ("DRAGON".equals(type)) {                    // :535
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :539 DamageCalcUserItem.copy(:DRAGONFANG,:DRACOPLATE)
        BattleHandlers.DamageCalcUserItem.copy("DRAGONFANG", "DRACOPLATE");

        // :541-545 DRAGONGEM
        BattleHandlers.DamageCalcUserItem.add("DRAGONGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "DRAGON", move, mults, type);   // :543
        });

        // :547-551 ELECTRICGEM
        BattleHandlers.DamageCalcUserItem.add("ELECTRICGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "ELECTRIC", move, mults, type);   // :549
        });

        // :553-559 EXPERTBELT
        BattleHandlers.DamageCalcUserItem.add("EXPERTBELT", (item, user, target, move, mults, baseDmg, type) -> {
            if (PBTypes.superEffective(target.damageState.typeMod)) {    // :555 PBTypes.superEffective?(target.damageState.typeMod)
                mults[BattleHandlers.FINAL_DMG_MULT] *= 1.2f;            // :556
            }
        });

        // :561-565 FAIRYGEM
        BattleHandlers.DamageCalcUserItem.add("FAIRYGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "FAIRY", move, mults, type);   // :563
        });

        // :567-571 FIGHTINGGEM
        BattleHandlers.DamageCalcUserItem.add("FIGHTINGGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "FIGHTING", move, mults, type);   // :569
        });

        // :573-577 FIREGEM
        BattleHandlers.DamageCalcUserItem.add("FIREGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "FIRE", move, mults, type);   // :575
        });

        // :579-583 FLYINGGEM
        BattleHandlers.DamageCalcUserItem.add("FLYINGGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "FLYING", move, mults, type);   // :581
        });

        // :585-589 GHOSTGEM
        BattleHandlers.DamageCalcUserItem.add("GHOSTGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "GHOST", move, mults, type);   // :587
        });

        // :591-595 GRASSGEM
        BattleHandlers.DamageCalcUserItem.add("GRASSGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "GRASS", move, mults, type);   // :593
        });

        // :597-604 GRISEOUSORB
        BattleHandlers.DamageCalcUserItem.add("GRISEOUSORB", (item, user, target, move, mults, baseDmg, type) -> {
            // :599-600 if user.isSpecies?(:GIRATINA) && (isConst?(type,PBTypes,:DRAGON) || ...:GHOST)
            if (user.isSpecies("GIRATINA")
                    && ("DRAGON".equals(type) || "GHOST".equals(type))) {
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;             // :601
            }
        });

        // :606-610 GROUNDGEM
        BattleHandlers.DamageCalcUserItem.add("GROUNDGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "GROUND", move, mults, type);   // :608
        });

        // :612-616 HARDSTONE
        BattleHandlers.DamageCalcUserItem.add("HARDSTONE", (item, user, target, move, mults, baseDmg, type) -> {
            if ("ROCK".equals(type)) {                      // :614
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :618 DamageCalcUserItem.copy(:HARDSTONE,:STONEPLATE,:ROCKINCENSE)
        BattleHandlers.DamageCalcUserItem.copy("HARDSTONE", "STONEPLATE", "ROCKINCENSE");

        // :620-624 ICEGEM
        BattleHandlers.DamageCalcUserItem.add("ICEGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "ICE", move, mults, type);   // :622
        });

        // :626-632 LIFEORB
        BattleHandlers.DamageCalcUserItem.add("LIFEORB", (item, user, target, move, mults, baseDmg, type) -> {
            // :628 if !move.is_a?(PokeBattle_Confusion) - the strategy's identity IS
            //       the function-code class, so PokeBattle_Confusion is the registry's
            //       pseudomove instance.
            if (effectOf(move) != MoveEffectRegistry.confusion()) {
                mults[BattleHandlers.FINAL_DMG_MULT] *= 1.3f;            // :629
            }
        });

        // :634-640 LIGHTBALL
        BattleHandlers.DamageCalcUserItem.add("LIGHTBALL", (item, user, target, move, mults, baseDmg, type) -> {
            if (user.isSpecies("PIKACHU")) {                             // :636
                mults[BattleHandlers.ATK_MULT] *= 2;                     // :637
            }
        });

        // :642-649 LUSTROUSORB
        BattleHandlers.DamageCalcUserItem.add("LUSTROUSORB", (item, user, target, move, mults, baseDmg, type) -> {
            // :644-645 if user.isSpecies?(:PALKIA) && (isConst?(type,PBTypes,:DRAGON) || ...:WATER)
            if (user.isSpecies("PALKIA")
                    && ("DRAGON".equals(type) || "WATER".equals(type))) {
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;             // :646
            }
        });

        // :651-655 MAGNET
        BattleHandlers.DamageCalcUserItem.add("MAGNET", (item, user, target, move, mults, baseDmg, type) -> {
            if ("ELECTRIC".equals(type)) {                  // :653
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :657 DamageCalcUserItem.copy(:MAGNET,:ZAPPLATE)
        BattleHandlers.DamageCalcUserItem.copy("MAGNET", "ZAPPLATE");

        // :659-663 METALCOAT
        BattleHandlers.DamageCalcUserItem.add("METALCOAT", (item, user, target, move, mults, baseDmg, type) -> {
            if ("STEEL".equals(type)) {                     // :661
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :665 DamageCalcUserItem.copy(:METALCOAT,:IRONPLATE)
        BattleHandlers.DamageCalcUserItem.copy("METALCOAT", "IRONPLATE");

        // :667-672 METRONOME
        BattleHandlers.DamageCalcUserItem.add("METRONOME", (item, user, target, move, mults, baseDmg, type) -> {
            // :669 met = 1+0.2*[user.effects[PBEffects::Metronome],5].min
            float met = 1 + 0.2f * Math.min(user.effects.intVal(PBEffects.Battler.Metronome), 5);
            mults[BattleHandlers.FINAL_DMG_MULT] *= met;                 // :670
        });

        // :674-678 MIRACLESEED
        BattleHandlers.DamageCalcUserItem.add("MIRACLESEED", (item, user, target, move, mults, baseDmg, type) -> {
            if ("GRASS".equals(type)) {                     // :676
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :680 DamageCalcUserItem.copy(:MIRACLESEED,:MEADOWPLATE,:ROSEINCENSE)
        BattleHandlers.DamageCalcUserItem.copy("MIRACLESEED", "MEADOWPLATE", "ROSEINCENSE");

        // :682-686 MUSCLEBAND
        BattleHandlers.DamageCalcUserItem.add("MUSCLEBAND", (item, user, target, move, mults, baseDmg, type) -> {
            if (effectOf(move).physicalMove(move, null)) {               // :684 move.physicalMove?
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.1f;
            }
        });

        // :688-692 MYSTICWATER
        BattleHandlers.DamageCalcUserItem.add("MYSTICWATER", (item, user, target, move, mults, baseDmg, type) -> {
            if ("WATER".equals(type)) {                     // :690
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :694 DamageCalcUserItem.copy(:MYSTICWATER,:SPLASHPLATE,:SEAINCENSE,:WAVEINCENSE)
        BattleHandlers.DamageCalcUserItem.copy("MYSTICWATER", "SPLASHPLATE", "SEAINCENSE", "WAVEINCENSE");

        // :696-700 NEVERMELTICE
        BattleHandlers.DamageCalcUserItem.add("NEVERMELTICE", (item, user, target, move, mults, baseDmg, type) -> {
            if ("ICE".equals(type)) {                       // :698
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :702 DamageCalcUserItem.copy(:NEVERMELTICE,:ICICLEPLATE)
        BattleHandlers.DamageCalcUserItem.copy("NEVERMELTICE", "ICICLEPLATE");

        // :704-708 NORMALGEM
        BattleHandlers.DamageCalcUserItem.add("NORMALGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "NORMAL", move, mults, type);   // :706
        });

        // :710-714 PIXIEPLATE
        BattleHandlers.DamageCalcUserItem.add("PIXIEPLATE", (item, user, target, move, mults, baseDmg, type) -> {
            if ("FAIRY".equals(type)) {                     // :712
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :716-720 POISONBARB
        BattleHandlers.DamageCalcUserItem.add("POISONBARB", (item, user, target, move, mults, baseDmg, type) -> {
            if ("POISON".equals(type)) {                    // :718
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :722 DamageCalcUserItem.copy(:POISONBARB,:TOXICPLATE)
        BattleHandlers.DamageCalcUserItem.copy("POISONBARB", "TOXICPLATE");

        // :724-728 POISONGEM
        BattleHandlers.DamageCalcUserItem.add("POISONGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "POISON", move, mults, type);   // :726
        });

        // :730-734 PSYCHICGEM
        BattleHandlers.DamageCalcUserItem.add("PSYCHICGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "PSYCHIC", move, mults, type);   // :732
        });

        // :736-740 ROCKGEM
        BattleHandlers.DamageCalcUserItem.add("ROCKGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "ROCK", move, mults, type);   // :738
        });

        // :742-746 SHARPBEAK
        BattleHandlers.DamageCalcUserItem.add("SHARPBEAK", (item, user, target, move, mults, baseDmg, type) -> {
            if ("FLYING".equals(type)) {                    // :744
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :748 DamageCalcUserItem.copy(:SHARPBEAK,:SKYPLATE)
        BattleHandlers.DamageCalcUserItem.copy("SHARPBEAK", "SKYPLATE");

        // :750-754 SILKSCARF
        BattleHandlers.DamageCalcUserItem.add("SILKSCARF", (item, user, target, move, mults, baseDmg, type) -> {
            if ("NORMAL".equals(type)) {                    // :752
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :755 DamageCalcUserItem.copy(:SILKSCARF,:BLANKPLATE)
        BattleHandlers.DamageCalcUserItem.copy("SILKSCARF", "BLANKPLATE");

        // :758-762 SILVERPOWDER
        BattleHandlers.DamageCalcUserItem.add("SILVERPOWDER", (item, user, target, move, mults, baseDmg, type) -> {
            if ("BUG".equals(type)) {                       // :760
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :764 DamageCalcUserItem.copy(:SILVERPOWDER,:INSECTPLATE)
        BattleHandlers.DamageCalcUserItem.copy("SILVERPOWDER", "INSECTPLATE");

        // :766-770 SOFTSAND
        BattleHandlers.DamageCalcUserItem.add("SOFTSAND", (item, user, target, move, mults, baseDmg, type) -> {
            if ("GROUND".equals(type)) {                    // :768
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :772 DamageCalcUserItem.copy(:SOFTSAND,:EARTHPLATE)
        BattleHandlers.DamageCalcUserItem.copy("SOFTSAND", "EARTHPLATE");

        // :774-787 SOULDEW
        BattleHandlers.DamageCalcUserItem.add("SOULDEW", (item, user, target, move, mults, baseDmg, type) -> {
            // :776 next if !user.isSpecies?(:LATIAS) && !user.isSpecies?(:LATIOS)
            if (!user.isSpecies("LATIAS") && !user.isSpecies("LATIOS")) {
                return;
            }
            if (Battle.NEWEST_BATTLE_MECHANICS) {                        // :777
                if ("PSYCHIC".equals(type) || "DRAGON".equals(type)) {   // :778
                    mults[BattleHandlers.FINAL_DMG_MULT] *= 1.2f;        // :779
                }
            }
            // :781-785 else: if move.specialMove? && !user.battle.rules["souldewclause"]
            //                then mults[ATK_MULT] *= 1.5
            // 登记: Battle#rules ("souldewclause") is not modelled in this runtime and
            //       NEWEST_BATTLE_MECHANICS is true (Settings:160), which makes the
            //       branch unreachable; the Ruby lines are recorded here verbatim:
            //         :782 if move.specialMove? && !user.battle.rules["souldewclause"]
            //         :783   mults[ATK_MULT] *= 1.5
        });

        // :789-793 SPELLTAG
        BattleHandlers.DamageCalcUserItem.add("SPELLTAG", (item, user, target, move, mults, baseDmg, type) -> {
            if ("GHOST".equals(type)) {                     // :791
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :795 DamageCalcUserItem.copy(:SPELLTAG,:SPOOKYPLATE)
        BattleHandlers.DamageCalcUserItem.copy("SPELLTAG", "SPOOKYPLATE");

        // :797-801 STEELGEM
        BattleHandlers.DamageCalcUserItem.add("STEELGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "STEEL", move, mults, type);   // :799
        });

        // :803-809 THICKCLUB
        BattleHandlers.DamageCalcUserItem.add("THICKCLUB", (item, user, target, move, mults, baseDmg, type) -> {
            // :805 if (user.isSpecies?(:CUBONE) || user.isSpecies?(:MAROWAK)) && move.physicalMove?
            if ((user.isSpecies("CUBONE") || user.isSpecies("MAROWAK"))
                    && effectOf(move).physicalMove(move, null)) {
                mults[BattleHandlers.ATK_MULT] *= 2;                     // :806
            }
        });

        // :811-815 TWISTEDSPOON
        BattleHandlers.DamageCalcUserItem.add("TWISTEDSPOON", (item, user, target, move, mults, baseDmg, type) -> {
            if ("PSYCHIC".equals(type)) {                   // :813
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :817 DamageCalcUserItem.copy(:TWISTEDSPOON,:MINDPLATE,:ODDINCENSE)
        BattleHandlers.DamageCalcUserItem.copy("TWISTEDSPOON", "MINDPLATE", "ODDINCENSE");

        // :819-823 WATERGEM
        BattleHandlers.DamageCalcUserItem.add("WATERGEM", (item, user, target, move, mults, baseDmg, type) -> {
            BattleHandlerHelpers.pbBattleGem(user, "WATER", move, mults, type);   // :821
        });

        // :825-829 WISEGLASSES
        BattleHandlers.DamageCalcUserItem.add("WISEGLASSES", (item, user, target, move, mults, baseDmg, type) -> {
            if (effectOf(move).specialMove(move, null)) {                // :827 move.specialMove?
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.1f;
            }
        });

        // :832-836 WELLSPRINGMASK
        BattleHandlers.DamageCalcUserItem.add("WELLSPRINGMASK", (item, user, target, move, mults, baseDmg, type) -> {
            if (user.isSpecies("OGERPON")) {                             // :834
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :837-841 HEARTHFLAMEMASK
        BattleHandlers.DamageCalcUserItem.add("HEARTHFLAMEMASK", (item, user, target, move, mults, baseDmg, type) -> {
            if (user.isSpecies("OGERPON")) {                             // :839
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :842-846 CORNERSTONEMASK
        BattleHandlers.DamageCalcUserItem.add("CORNERSTONEMASK", (item, user, target, move, mults, baseDmg, type) -> {
            if (user.isSpecies("OGERPON")) {                             // :844
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // ==============================================================
        // DamageCalcUserItem, continued (BattleHandlers_Items.rb:1719-1804)
        // ==============================================================

        // :1719-1723 ABYSSSWORD
        BattleHandlers.DamageCalcUserItem.add("ABYSSSWORD", (item, user, target, move, mults, baseDmg, type) -> {
            // :1721 if user.isSpecies?(:SUJINRAKU) && move.physicalMove?
            if (user.isSpecies("SUJINRAKU") && effectOf(move).physicalMove(move, null)) {
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :1725-1729 TEMPLESCEPTER
        BattleHandlers.DamageCalcUserItem.add("TEMPLESCEPTER", (item, user, target, move, mults, baseDmg, type) -> {
            // :1727 if user.isSpecies?(:SUGARDEVOIR) && move.specialMove?
            if (user.isSpecies("SUGARDEVOIR") && effectOf(move).specialMove(move, null)) {
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :1731-1735 WMDCX
        BattleHandlers.DamageCalcUserItem.add("WMDCX", (item, user, target, move, mults, baseDmg, type) -> {
            // :1733 if user.isSpecies?(:SUGARDEVOIR) && move.specialMove?
            if (user.isSpecies("SUGARDEVOIR") && effectOf(move).specialMove(move, null)) {
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :1737-1741 SANCTFEATHER
        BattleHandlers.DamageCalcUserItem.add("SANCTFEATHER", (item, user, target, move, mults, baseDmg, type) -> {
            // :1739 if user.isSpecies?(:SUGARDEVOIR) && move.specialMove?
            if (user.isSpecies("SUGARDEVOIR") && effectOf(move).specialMove(move, null)) {
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :1743-1747 RADIANTSHARD
        BattleHandlers.DamageCalcUserItem.add("RADIANTSHARD", (item, user, target, move, mults, baseDmg, type) -> {
            // :1745 if user.isSpecies?(:SUGARDEVOIR) && move.specialMove?
            if (user.isSpecies("SUGARDEVOIR") && effectOf(move).specialMove(move, null)) {
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.2f;
            }
        });

        // :1757-1761 HOLYCREST
        BattleHandlers.DamageCalcUserItem.add("HOLYCREST", (item, user, target, move, mults, baseDmg, type) -> {
            // :1759 if user.isSpecies?(:SIRFETCHD) && move.specialMove?
            if (user.isSpecies("SIRFETCHD") && effectOf(move).specialMove(move, null)) {
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.1f;
            }
        });

        // :1764-1768 CRAFTMIND  (#匠心律印)
        BattleHandlers.DamageCalcUserItem.add("CRAFTMIND", (item, user, target, move, mults, baseDmg, type) -> {
            // :1766 if user.isSpecies?(:SAMUROTT) && move.physicalMove?
            if (user.isSpecies("SAMUROTT") && effectOf(move).physicalMove(move, null)) {
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.1f;
            }
        });

        // :1799-1803 PUNCHINGGLOVE
        BattleHandlers.DamageCalcUserItem.add("PUNCHINGGLOVE", (item, user, target, move, mults, baseDmg, type) -> {
            if (effectOf(move).punchingMove(move)) {                     // :1801 move.punchingMove?
                mults[BattleHandlers.BASE_DMG_MULT] *= 1.1f;
            }
        });
    }
}
