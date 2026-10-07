package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;

import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.PBEffects;
import pokemon.runtime.battle.PBStats;
import pokemon.runtime.battle.PBStatuses;
import pokemon.runtime.battle.PBBattleTerrains;
import pokemon.runtime.battle.PBWeather;
import pokemon.runtime.battle.PendingApi;

/**
 * Stage 4 / L2: the function-code classes that live OUTSIDE the four
 * {@code Move_Effects_*} sections - {@code Arceus} (net-new 200-215),
 * {@code 场地} (net-new 216-219) and {@code Pokemon_ShadowPokemon}
 * (126-132) - plus the three reusable base classes those sections declare
 * ({@code PokeBattle_StatusAndBonusDamageMove}, {@code PokeBattle_FrostbiteMove},
 * {@code PokeBattle_DrowsyMove}).
 *
 * <h2>The 15 duplicate declarations: verified, and where they went</h2>
 * <p>Three sections re-declare 11 codes (15 declarations) that the
 * {@code Move_Effects_*} sections already define. Every one was read line by
 * line; <b>none is a harmless repeat</b>: Ruby reopens the class (a second
 * {@code class X < PokeBattle_Move} is legal when the superclass matches) and the
 * later-loaded section's methods <b>override</b> the earlier ones - all 15 bodies
 * differ from the original. They are therefore NOT transcribed here; they are
 * merged into the file that already holds the class:</p>
 * <ul>
 * <li>{@code 0A4} - original {@code Move_Effects_080-0FF:965}; reopened by
 *     {@code 场地:154} and {@code 场地:373} (both {@code class PokeBattle_Move_0A4},
 *     no superclass) via {@code alias bug_lure_secret_pbOnStartUse} /
 *     {@code alias cold_terrain_secret_power_pbOnStartUse} → merged into
 *     {@code MoveEffects_080_0AF}.</li>
 * <li>{@code 0B3} (original {@code 080-0FF:1406}; {@code 场地:141}/{@code :359}) and
 *     {@code 0D1} (original {@code 080-0FF:2473}; {@code Arceus:351}) → the classes
 *     now live in {@code MoveEffects_0B0_0D4.java}, so their merge belongs to that
 *     file's owner.</li>
 * <li>{@code 018}/{@code 019}/{@code 01B}/{@code 07D} (originals {@code 000-07F:384/421/532/2816})
 *     and {@code 060} (original {@code 000-07F:1955}; reopened twice by
 *     {@code 场地:189}/{@code :409}) → {@code MoveEffects_000_07F.java} (infra3).</li>
 * <li>{@code 110} (original {@code 100-17F:378}; {@code Arceus:370}) →
 *     {@code MoveEffects_100_17F.java} (buildertypes).</li>
 * <li>{@code 15A} (original {@code 100-17F:1921}; {@code Arceus:418}) and
 *     {@code 18A} (original {@code 180-1FF:220}; reopened by {@code 场地:238}/{@code :458}) →
 *     {@code MoveEffects_180_1FF.java} (battlerapi).</li>
 * </ul>
 * <p>No superclass mismatch ({@code TypeError}) exists among the 15.</p>
 *
 * <h2>Private helpers instead of {@code MoveFxPendingApi}</h2>
 * <p>This batch adds no stub to {@code MoveFxPendingApi}: every plugin method that
 * has no real counterpart is a private throwing helper at the bottom of this
 * file, so the missing surface is local and greppable.</p>
 *
 * <h2>Plugin quirks reproduced verbatim</h2>
 * <ul>
 * <li>{@code Move_Effects_100-17F.rb:593} is a real bug kept as-is
 *     ({@code b.pbReduceHP(i.hp/2,false)} - {@code i} is undefined); see
 *     {@code PokeBattle_Move_12E}.</li>
 * <li>{@code PBEffects} is reopened by later sections: {@code Arceus:30-33}
 *     redefines {@code StoneAxe}/{@code CeaselessEdge}/{@code VictoryDance} as
 *     301/302/303 and adds {@code PowerShift = 300}; {@code 场地:476-477} adds
 *     {@code CaptureNet = 304}, {@code CaptureNetUser = 305}. {@code PBEffects.java}
 *     now carries all six at those effective values (Ruby's later assignment wins),
 *     so this file uses {@code PBEffects.Battler.*} directly - no local copies.</li>
 * </ul>
 */
public final class MoveEffects_Extra {

    private MoveEffects_Extra() {
    }

    // ==================================================================
    // Reusable base classes declared outside Move_Effects_Generic.rb
    // ==================================================================

    /**
     * {@code class PokeBattle_StatusAndBonusDamageMove < PokeBattle_Move}
     * (Arceus:621-639): double damage against a statused target, then try to
     * inflict its own status.
     */
    public static class PokeBattle_StatusAndBonusDamageMove extends MoveEffectBase {

        /** {@code @status = PBStatuses::NONE} (:624, {@code initialize}). */
        protected int status = PBStatuses.NONE;

        /** {@code pbBaseDamage(baseDmg,user,target)} (:627-633). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int ret = baseDmg;
            // :628-629 if target.pbHasAnyStatus? && (Substitute==0 || ignoresSubstitute?(user))
            if (target.pbHasAnyStatus()
                    && (target.effects.intVal(PBEffects.Battler.Substitute) == 0
                        || ignoresSubstitute(move, user))) {
                ret *= 2;                                                // :630
            }
            return ret;                                                  // :632
        }

        /** {@code pbAdditionalEffect(user,target)} (:635-638). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                         // :636
                return;
            }
            // :637 target.pbInflictStatus(@status,0,nil,user) if target.pbCanInflictStatus?(@status,user,false,self)
            if (target.pbCanInflictStatus(status, user, false, move, false)) {
                target.pbInflictStatus(status, 0, null, user);
            }
        }
    }

    /** {@code class PokeBattle_FrostbiteMove < PokeBattle_Move} (Arceus:686-701). */
    public static class PokeBattle_FrostbiteMove extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:687-690). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {                                    // :688
                return false;
            }
            return !canFrostbite(target, user, true, move);               // :689
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:692-695). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {                                    // :693
                return;
            }
            frostbite(target, user, null);                                // :694
        }

        /** {@code pbAdditionalEffect(user,target)} (:697-700). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                          // :698
                return;
            }
            if (canFrostbite(target, user, false, move)) {                // :699
                frostbite(target, user, null);
            }
        }
    }

    /** {@code class PokeBattle_DrowsyMove < PokeBattle_Move} (Arceus:706-721). */
    public static class PokeBattle_DrowsyMove extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:707-710). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {                                    // :708
                return false;
            }
            return !canDrowse(target, user, true, move);                  // :709
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:712-715). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (damagingMove(move)) {                                    // :713
                return;
            }
            drowse(target, user, null);                                   // :714
        }

        /** {@code pbAdditionalEffect(user,target)} (:717-720). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                          // :718
                return;
            }
            if (canDrowse(target, user, false, move)) {                   // :719
                drowse(target, user, null);
            }
        }
    }

    // ==================================================================
    // Arceus 200-205 (Move_Effects_Extra: Arceus section)
    // ==================================================================

    /** {@code class PokeBattle_Move_200 < PokeBattle_Move} (Arceus:438-447). */
    public static class PokeBattle_Move_200 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:439-446). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                          // :440
                return;
            }
            switch (user.battle.pbRandom(3)) {                            // :441 case @battle.pbRandom(3)
                case 0:                                                  // :442 when 0
                    if (canDrowse(target, user, false, move)) {
                        drowse(target, user, null);
                    }
                    break;
                case 1:                                                  // :443 when 1
                    if (target.pbCanPoison(user, false, move)) {
                        target.pbPoison(user, null, false);
                    }
                    break;
                case 2:                                                  // :444 when 2
                    if (target.pbCanParalyze(user, false, move)) {
                        target.pbParalyze(user, null);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /** {@code class PokeBattle_Move_201 < PokeBattle_Move} (Arceus:454-461). */
    public static class PokeBattle_Move_201 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:455-460). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            // :456-457 user.attack,user.defense = user.defense,user.attack; spatk/spdef likewise
            swapOffensiveDefensiveStats(user);
            user.effects.set(PBEffects.Battler.PowerShift,
                    !user.effects.truthy(PBEffects.Battler.PowerShift));           // :458 = !@effects[PowerShift]
            user.battle.display(user.pbThis() + "交换了它的攻击和防御属性！");   // :459
        }
    }

    /** {@code class PokeBattle_Move_202 < PokeBattle_Move} (Arceus:466-472). */
    public static class PokeBattle_Move_202 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:467-471). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.StoneAxe) > -1) {   // :468
                return;
            }
            // :469 target.effects[PBEffects::StoneAxe] = 3+rand(3)
            // PBEffects.Battler.StoneAxe is 301 - the value the Arceus reopening
            // (Arceus:31) leaves in place, which PBEffects.java now carries.
            target.effects.set(PBEffects.Battler.StoneAxe, 3 + user.battle.pbRandom(3));
            user.battle.display(target.pbThis(true) + " 周围的碎片四散飞溅！");   // :470
        }
    }

    /** {@code class PokeBattle_Move_203 < PokeBattle_Move} (Arceus:474-480). */
    public static class PokeBattle_Move_203 extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:475-479). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.CeaselessEdge) > -1) {   // :476
                return;
            }
            // :477 target.effects[PBEffects::CeaselessEdge] = 3+rand(3)
            // PBEffects.Battler.CeaselessEdge is 302 - the Arceus:32 value.
            target.effects.set(PBEffects.Battler.CeaselessEdge, 3 + user.battle.pbRandom(3));
            user.battle.display(target.pbThis(true) + " 周围的碎片四散飞溅！");   // :478
        }
    }

    /** {@code class PokeBattle_Move_204 < PokeBattle_Move} (Arceus:486-516). */
    public static class PokeBattle_Move_204 extends MoveEffectBase {

        /** {@code @statUp = [PBStats::ATTACK,1,PBStats::SPATK,1]} (:489). */
        private final int[] statUp = { PBStats.ATTACK, 1, PBStats.SPATK, 1 };
        /** {@code @statDown = [PBStats::DEFENSE,1,PBStats::SPDEF,1]} (:490). */
        private final int[] statDown = { PBStats.DEFENSE, 1, PBStats.SPDEF, 1 };

        /** {@code pbAdditionalEffect(user,target)} (:493-515). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            // :494 return if user.species != PBSpecies::ENAMORUS # DEBUG  (commented out in the plugin)
            boolean showAnim = true;                                      // :495
            if (user.form() == 1) {                                       // :497
                // :499 return if target.damageState.substitute
                if (target.damageState.substitute) {
                    return;
                }
                for (int i = 0; i < statDown.length / 2; i++) {           // :500
                    if (!target.pbCanLowerStatStage(statDown[i * 2], user, move)) {   // :501
                        continue;
                    }
                    if (target.pbLowerStatStage(statDown[i * 2], statDown[i * 2 + 1], user, showAnim)) {   // :502
                        showAnim = false;                                 // :503
                    }
                }
            } else {                                                      // :506
                for (int i = 0; i < statUp.length / 2; i++) {             // :508
                    if (!user.pbCanRaiseStatStage(statUp[i * 2], user, move)) {   // :509
                        continue;
                    }
                    if (user.pbRaiseStatStage(statUp[i * 2], statUp[i * 2 + 1], user, showAnim)) {   // :510
                        showAnim = false;                                 // :511
                    }
                }
            }
        }
    }

    /** {@code class PokeBattle_Move_205 < PokeBattle_Move} (Arceus:522-540). */
    public static class PokeBattle_Move_205 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:523-539). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            boolean showAnim = true;                                      // :524
            int[] statUp;                                                 // :525 statUp = []
            // :526 if user.attack + user.spatk >= user.defense + user.spdef
            if (rawStat(user, PBStats.ATTACK) + rawStat(user, PBStats.SPATK)
                    >= rawStat(user, PBStats.DEFENSE) + rawStat(user, PBStats.SPDEF)) {
                statUp = new int[] { PBStats.ATTACK, 1, PBStats.SPATK, 1 };   // :528
            } else {
                statUp = new int[] { PBStats.DEFENSE, 1, PBStats.SPDEF, 1 };  // :530
            }
            for (int i = 0; i < statUp.length / 2; i++) {                 // :533
                if (!user.pbCanRaiseStatStage(statUp[i * 2], user, move)) {   // :534
                    continue;
                }
                if (user.pbRaiseStatStage(statUp[i * 2], statUp[i * 2 + 1], user, showAnim)) {   // :535
                    showAnim = false;                                     // :536
                }
            }
        }
    }

    // ==================================================================
    // Arceus 206-215
    // ==================================================================

    /** {@code class PokeBattle_Move_206 < PokeBattle_StatUpMove} (Arceus:545-567). */
    public static class PokeBattle_Move_206 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code @statUp = [PBStats::SPEED,1]} (Arceus:548, {@code initialize}). */
        public PokeBattle_Move_206() {
            this.statUp = new int[] { PBStats.SPEED, 1 };
        }
    }

    /** {@code class PokeBattle_Move_207 < PokeBattle_StatDownMove} (Arceus:572-594). */
    public static class PokeBattle_Move_207 extends MoveEffectsGeneric.PokeBattle_StatDownMove {

        /** {@code @statDown = [PBStats::SPEED,1]} (:575). */
        public PokeBattle_Move_207() {
            this.statDown = new int[] { PBStats.SPEED, 1 };
        }

        /** {@code recoilMove?; return true; end} (:578). */
        @Override
        public boolean recoilMove(BattleMove move) {
            return true;
        }

        /** {@code pbRecoilDamage(user,target)} (:580-582). */
        @Override
        public int pbRecoilDamage(BattleMove move, Battler user, Battler target) {
            return Math.round(target.damageState.totalHPLost / 2.0f);     // :581
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:584-593). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (target.damageState.unaffected) {                          // :585
                return;
            }
            if (!user.takesIndirectDamage(false)) {                       // :586
                return;
            }
            if (user.hasActiveAbility("ROCKHEAD")) {                      // :587
                return;
            }
            int amt = pbRecoilDamage(move, user, target);                 // :588
            if (amt < 1) {                                                // :589
                amt = 1;
            }
            user.pbReduceHP(amt, false, true, true);                      // :590
            user.battle.display(user.pbThis() + "受到了反作用力的伤害！");     // :591
            user.pbItemHPHealCheck(0, false);                             // :592
        }
    }

    /** {@code class PokeBattle_Move_208 < PokeBattle_MultiStatUpMove} (Arceus:599-614). */
    public static class PokeBattle_Move_208 extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code @statUp = [ATTACK,1,DEFENSE,1,SPATK,1,SPDEF,1]} (:602-603). */
        public PokeBattle_Move_208() {
            this.statUp = new int[] { PBStats.ATTACK, 1, PBStats.DEFENSE, 1,
                                      PBStats.SPATK, 1, PBStats.SPDEF, 1 };
        }

        /** {@code pbAdditionalEffect(user,target)} (:606-613). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            super.pbAdditionalEffect(move, user, target);                 // :607 super
            if (!user.effects.truthy(PBEffects.Battler.VictoryDance)) {    // :609
                user.effects.set(PBEffects.Battler.VictoryDance, true);    // :610
                user.battle.display(user.pbThis() + " dances in victory!");   // :611
            }
        }
    }

    /** {@code class PokeBattle_Move_209 < PokeBattle_StatusAndBonusDamageMove} (Arceus:642-647). */
    public static class PokeBattle_Move_209 extends PokeBattle_StatusAndBonusDamageMove {
        /** {@code @status = PBStatuses::POISON} (:645). */
        public PokeBattle_Move_209() {
            this.status = PBStatuses.POISON;
        }
    }

    /** {@code class PokeBattle_Move_210 < PokeBattle_StatusAndBonusDamageMove} (Arceus:650-655). */
    public static class PokeBattle_Move_210 extends PokeBattle_StatusAndBonusDamageMove {
        /** {@code @status = PBStatuses::FROSTBITE} (:653). */
        public PokeBattle_Move_210() {
            this.status = PBStatuses.FROSTBITE;
        }
    }

    /** {@code class PokeBattle_Move_211 < PokeBattle_StatusAndBonusDamageMove} (Arceus:658-663). */
    public static class PokeBattle_Move_211 extends PokeBattle_StatusAndBonusDamageMove {
        /** {@code @status = PBStatuses::BURN} (:661). */
        public PokeBattle_Move_211() {
            this.status = PBStatuses.BURN;
        }
    }

    /** {@code class PokeBattle_Move_212 < PokeBattle_TargetStatDownMove} (Arceus:669-680). */
    public static class PokeBattle_Move_212 extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code @statDown = [PBStats::DEFENSE, 1]} (:672). */
        public PokeBattle_Move_212() {
            this.statDown = new int[] { PBStats.DEFENSE, 1 };
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:675-679). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (user.effects.intVal(PBEffects.Battler.FocusEnergy) > 2) {   // :676
                return;
            }
            user.effects.set(PBEffects.Battler.FocusEnergy, 2);            // :677
            user.battle.display(user.pbThis() + "变得兴奋了！");             // :678
        }
    }

    /** {@code class PokeBattle_Move_213 < PokeBattle_FrostbiteMove} (Arceus:726-727). */
    public static class PokeBattle_Move_213 extends PokeBattle_FrostbiteMove {
        // :726-727 empty body - the base class does the work.
    }

    /** {@code class PokeBattle_Move_214 < PokeBattle_HealingMove} (Arceus:732-744). */
    public static class PokeBattle_Move_214 extends MoveEffectsGeneric.PokeBattle_HealingMove {

        /** {@code pbHealAmount(user)} (:733-735). */
        @Override
        public int pbHealAmount(BattleMove move, Battler user) {
            return Math.round(user.maxHp() / 3.0f);                       // :734
        }

        /** {@code pbMoveFailed?(user,targets)} (:736-739). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbHasAnyStatus()) {                                  // :737
                return false;
            }
            return super.pbMoveFailed(move, user, targets);                // :738 return super
        }

        /** {@code pbEffectGeneral(user)} (:740-743). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.pbCureStatus();                                          // :741
            super.pbEffectGeneral(move, user);                            // :742 super
        }
    }

    /** {@code class PokeBattle_Move_215 < PokeBattle_MultiStatUpMove} (Arceus:749-765). */
    public static class PokeBattle_Move_215 extends MoveEffectsGeneric.PokeBattle_MultiStatUpMove {

        /** {@code @statUp = [ATTACK,1,DEFENSE,1,SPATK,1,SPDEF,1]} (:752-753). */
        public PokeBattle_Move_215() {
            this.statUp = new int[] { PBStats.ATTACK, 1, PBStats.DEFENSE, 1,
                                      PBStats.SPATK, 1, PBStats.SPDEF, 1 };
        }

        /** {@code pbMoveFailed?(user,targets)} (:756-759). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.pbHasAnyStatus()) {                                  // :757
                return false;
            }
            return super.pbMoveFailed(move, user, targets);                // :758 return super
        }

        /** {@code pbEffectGeneral(user)} (:761-764). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (user.pbHasAnyStatus()) {                                  // :762
                user.pbCureStatus();
            }
            super.pbEffectGeneral(move, user);                            // :763 super
        }
    }

    // ==================================================================
    // 场地 216-219
    // ==================================================================

    /** {@code class PokeBattle_Move_216 < PokeBattle_Move} (场地:124-136). */
    public static class PokeBattle_Move_216 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:125-131). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.battle.terrain() == PBBattleTerrains.BugLure) {       // :126
                user.battle.display("但是失败了！");                        // :127
                return true;                                              // :128
            }
            return false;                                                 // :130
        }

        /** {@code pbEffectGeneral(user)} (:133-135). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            startTerrain(user, PBBattleTerrains.BugLure);                 // :134
        }
    }

    /** {@code class PokeBattle_Move_217 < PokeBattle_Move} (场地:342-354). */
    public static class PokeBattle_Move_217 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:343-349). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.battle.terrain() == PBBattleTerrains.Cold) {          // :344
                user.battle.display("但是失败了！");                        // :345
                return true;                                              // :346
            }
            return false;                                                 // :348
        }

        /** {@code pbEffectGeneral(user)} (:351-353). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            startTerrain(user, PBBattleTerrains.Cold);                    // :352
        }
    }

    /** {@code class PokeBattle_Move_218 < PokeBattle_StatUpMove} (场地:496-509). */
    public static class PokeBattle_Move_218 extends MoveEffectsGeneric.PokeBattle_StatUpMove {

        /** {@code @statUp = [PBStats::ATTACK,1]} (:499). */
        public PokeBattle_Move_218() {
            this.statUp = new int[] { PBStats.ATTACK, 1 };
        }

        /** {@code pbOnStartUse(user,targets)} (:502-508). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            int increment = 1;                                            // :503
            if (user.battle.terrain() == PBBattleTerrains.Cold) {          // :504
                increment = 2;                                            // :505
            }
            this.statUp[1] = increment;                                   // :507 @statUp[1] = increment
        }
    }

    /** {@code class PokeBattle_Move_219 < PokeBattle_Move} (场地:516-551). */
    public static class PokeBattle_Move_219 extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:517-531). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.MeanLook) >= 0) {   // :518
                user.battle.display("但是失败了！");                        // :519
                return true;                                              // :520
            }
            // :523 if NEWEST_BATTLE_MECHANICS && target.pbHasType?(:GHOST)
            if (Battle.NEWEST_BATTLE_MECHANICS && target.pbHasType("GHOST")) {
                // :524-526 _INTL("这不能影响{1}……",target.pbThis(true))
                user.battle.display("这不能影响" + target.pbThis(true) + "……");
                return true;                                              // :527
            }
            return false;                                                 // :530
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:533-550). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            // :535 使用黑色目光的原版效果阻止目标交换和逃走
            target.effects.set(PBEffects.Battler.MeanLook, user.index);
            // :538-539 if @battle.field.terrain==BugLure && target.affectedByTerrain?
            if (user.battle.terrain() == PBBattleTerrains.BugLure && target.affectedByTerrain()) {
                target.effects.set(PBEffects.Battler.CaptureNet, true);            // :540
                target.effects.set(PBEffects.Battler.CaptureNetUser, user.index);  // :541
                // :542-544 _INTL("{1}被虫惑黏网牢牢缠住了！",target.pbThis)
                user.battle.display(target.pbThis() + "被虫惑黏网牢牢缠住了！");
            } else {                                                      // :545
                target.effects.set(PBEffects.Battler.CaptureNet, false);           // :546
                target.effects.set(PBEffects.Battler.CaptureNetUser, -1);          // :547
                user.battle.display(target.pbThis() + "不能逃脱！");         // :548
            }
        }
    }

    // ==================================================================
    // Pokemon_ShadowPokemon 126-132
    // ==================================================================

    /** {@code class PokeBattle_Move_126 < PokeBattle_Move_000} (Pokemon_ShadowPokemon:506-507). */
    public static class PokeBattle_Move_126 extends MoveEffects_000_07F.PokeBattle_Move_000 {
        // :506-507 empty body.
    }

    /** {@code class PokeBattle_Move_127 < PokeBattle_Move_007} (Pokemon_ShadowPokemon:514-515). */
    public static class PokeBattle_Move_127 extends MoveEffects_000_07F.PokeBattle_Move_007 {
        // :514-515 empty body.
    }

    /** {@code class PokeBattle_Move_128 < PokeBattle_Move_00A} (Pokemon_ShadowPokemon:522-523). */
    public static class PokeBattle_Move_128 extends MoveEffects_000_07F.PokeBattle_Move_00A {
        // :522-523 empty body.
    }

    /** {@code class PokeBattle_Move_129 < PokeBattle_Move_00C} (Pokemon_ShadowPokemon:530-531). */
    public static class PokeBattle_Move_129 extends MoveEffects_000_07F.PokeBattle_Move_00C {
        // :530-531 empty body.
    }

    /** {@code class PokeBattle_Move_12A < PokeBattle_Move_013} (Pokemon_ShadowPokemon:538-539). */
    public static class PokeBattle_Move_12A extends MoveEffects_000_07F.PokeBattle_Move_013 {
        // :538-539 empty body.
    }

    /** {@code class PokeBattle_Move_12B < PokeBattle_Move_04C} (Pokemon_ShadowPokemon:546-547). */
    public static class PokeBattle_Move_12B extends MoveEffects_000_07F.PokeBattle_Move_04C {
        // :546-547 empty body.
    }

    /** {@code class PokeBattle_Move_12C < PokeBattle_TargetStatDownMove} (Pokemon_ShadowPokemon:554-559). */
    public static class PokeBattle_Move_12C extends MoveEffectsGeneric.PokeBattle_TargetStatDownMove {

        /** {@code @statDown = [PBStats::EVASION,2]} (:557). */
        public PokeBattle_Move_12C() {
            this.statDown = new int[] { PBStats.EVASION, 2 };
        }
    }

    /** {@code class PokeBattle_Move_12D < PokeBattle_Move_075} (Pokemon_ShadowPokemon:566-567). */
    public static class PokeBattle_Move_12D extends MoveEffects_000_07F.PokeBattle_Move_075 {
        // :566-567 empty body. NOTE: PokeBattle_Move_075 is not written yet (its
        // range owner still owes it), so this extends a not-yet-existing class -
        // the joint compile gate reports "找不到符号" until it lands, which the
        // task explicitly accepts; the inheritance is NOT to be changed.
    }

    /** {@code class PokeBattle_Move_12E < PokeBattle_Move} (Pokemon_ShadowPokemon:575-600). */
    public static class PokeBattle_Move_12E extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:576-588). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            boolean failed = true;                                        // :577
            for (Battler b : user.battle.eachBattler()) {                  // :578 @battle.eachBattler
                if (b.hp == 1) {                                          // :579 next if b.hp==1
                    continue;
                }
                failed = false;                                           // :580
                break;                                                    // :581
            }
            if (failed) {                                                 // :583
                user.battle.display("但是失败了！");                        // :584
                return true;                                              // :585
            }
            return false;                                                 // :587
        }

        /** {@code pbEffectGeneral(user)} (:590-599). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            for (Battler b : user.battle.eachBattler()) {                  // :591
                if (b.hp == 1) {                                          // :592
                    continue;
                }
                // :593 b.pbReduceHP(i.hp/2,false)
                // 登记: PLUGIN DEFECT kept verbatim - the Ruby passes `i`, which is
                //       never defined in this method, so the line raises NameError.
                //       No method is added and the intended `b.hp/2` is NOT guessed;
                //       the call shape is recorded here for the defect register.
                reduceHalfHp(b);
            }
            user.battle.display("所有宝可梦的HP减半！");                    // :595
            for (Battler b : user.battle.eachBattler()) {                  // :596
                b.pbItemHPHealCheck(0, false);
            }
            user.effects.set(PBEffects.Battler.HyperBeam, 2);              // :597
            setCurrentMove(user, move.id());                               // :598 user.currentMove = @id
        }
    }

    /** {@code class PokeBattle_Move_12F < PokeBattle_Move_0EF} (Pokemon_ShadowPokemon:608-609). */
    public static class PokeBattle_Move_12F extends MoveEffects_0D5_0FF.PokeBattle_Move_0EF {
        // :608-609 empty body.
    }

    /** {@code class PokeBattle_Move_130 < PokeBattle_RecoilMove} (Pokemon_ShadowPokemon:616-630). */
    public static class PokeBattle_Move_130 extends MoveEffectsGeneric.PokeBattle_RecoilMove {

        /** {@code pbRecoilDamage(user,target)} (:617-619). */
        @Override
        public int pbRecoilDamage(BattleMove move, Battler user, Battler target) {
            return Math.round(target.damageState.totalHPLost / 2.0f);     // :618
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:621-629). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            // :622 return if user.fainted? || target.damageState.unaffected
            if (user.fainted() || target.damageState.unaffected) {
                return;
            }
            // :623 NOTE: This move's recoil is not prevented by Rock Head/Magic Guard.
            int amt = pbRecoilDamage(move, user, target);                 // :624
            if (amt < 1) {                                                // :625
                amt = 1;
            }
            user.pbReduceHP(amt, false, true, true);                      // :626
            user.battle.display(user.pbThis() + "受到了反作用力的伤害！");     // :627
            user.pbItemHPHealCheck(0, false);                             // :628
        }
    }

    /** {@code class PokeBattle_Move_131 < PokeBattle_WeatherMove} (Pokemon_ShadowPokemon:637-642). */
    public static class PokeBattle_Move_131 extends MoveEffectsGeneric.PokeBattle_WeatherMove {

        /** {@code @weatherType = PBWeather::ShadowSky} (:640). */
        public PokeBattle_Move_131() {
            this.weatherType = PBWeather.ShadowSky;
        }
    }

    /** {@code class PokeBattle_Move_132 < PokeBattle_Move} (Pokemon_ShadowPokemon:650-660). */
    public static class PokeBattle_Move_132 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:651-659). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            for (int i = 0; i < user.battle.field.sides.length; i++) {    // :652 for i in @battle.sides
                user.battle.field.sides[i].effects.set(PBEffects.Side.AuroraVeil, 0);    // :653
                user.battle.field.sides[i].effects.set(PBEffects.Side.Reflect, 0);       // :654
                user.battle.field.sides[i].effects.set(PBEffects.Side.LightScreen, 0);   // :655
                user.battle.field.sides[i].effects.set(PBEffects.Side.Safeguard, 0);     // :656
            }
            user.battle.display("它打破了所有的障碍！");                    // :658
        }
    }

    // ==================================================================
    // Private helpers for plugin methods this runtime does not have.
    // Each throws: a silent default would invent behaviour.
    // ==================================================================

    /** {@code Battler#pbCanFrostbite?(user,showMessages,move)} (Arceus:91-104). */
    private static boolean canFrostbite(Battler target, Battler user, boolean showMessages, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Arceus:91 pbCanFrostbite?");
    }

    /** {@code Battler#pbFrostbite(user=nil,msg=nil)} (Arceus:105-116). */
    private static void frostbite(Battler target, Battler user, String msg) {
        throw new UnsupportedOperationException("M0 待接线: Arceus:105 pbFrostbite");
    }

    /** {@code Battler#pbCanDrowse?(user,showMessages,move)} (Arceus:154-167). */
    private static boolean canDrowse(Battler target, Battler user, boolean showMessages, BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: Arceus:154 pbCanDrowse?");
    }

    /** {@code Battler#pbDrowse(user=nil,msg=nil)} (Arceus:168-180). */
    private static void drowse(Battler target, Battler user, String msg) {
        throw new UnsupportedOperationException("M0 待接线: Arceus:168 pbDrowse");
    }

    /**
     * {@code user.attack,user.defense = user.defense,user.attack} (Arceus:456).
     * This runtime's {@code Battler.attack()}/{@code defense()} are the
     * stage-scaled readers, so a swap has no target; see the same note on
     * {@code MoveFxPendingApi.attack}.
     */
    private static void swapOffensiveDefensiveStats(Battler user) {
        throw new UnsupportedOperationException("M0 待接线: Arceus:456 user.attack,user.defense = ...");
    }

    /** The plugin's raw stat ({@code user.attack} etc.) - no faithful accessor yet (Arceus:526). */
    private static int rawStat(Battler battler, int pbStat) {
        throw new UnsupportedOperationException("M0 待接线: Battler 原始能力值 " + PBStats.getName(pbStat));
    }

    /** {@code @battle.pbStartTerrain(user,terrain)} (场地:134/:352; PokeBattle_Battle:741). */
    private static void startTerrain(Battler user, int terrain) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:741 pbStartTerrain");
    }

    /** {@code b.pbReduceHP(i.hp/2,false)} (Pokemon_ShadowPokemon:593) - the plugin's NameError line. */
    private static void reduceHalfHp(Battler target) {
        throw new UnsupportedOperationException(
                "插件缺陷: Pokemon_ShadowPokemon:593 b.pbReduceHP(i.hp/2,false) - `i` 未定义, Ruby 会 NameError");
    }

    /** {@code user.currentMove = @id} (Pokemon_ShadowPokemon:598). */
    private static void setCurrentMove(Battler user, int moveId) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler currentMove=");
    }

    /**
     * {@code PokeBattle_Move.pbFromPBMove} (PokeBattle_Move.rb:51-59) for the
     * 33 function-code classes this file holds: the Ruby builds
     * {@code "PokeBattle_Move_#{function}"} and instantiates it on demand, so each
     * code is bound to its no-argument constructor here and
     * {@link MoveEffectRegistry#of} calls it lazily (some constructors still touch
     * not-yet-wired stubs, which must not break the whole table).
     *
     * <p>Generated by {@code __l2register.mjs} - re-run it with {@code --write}
     * after the class set changes.</p>
     */
    static void register() {
        MoveEffectRegistry.register("200", PokeBattle_Move_200::new);
        MoveEffectRegistry.register("201", PokeBattle_Move_201::new);
        MoveEffectRegistry.register("202", PokeBattle_Move_202::new);
        MoveEffectRegistry.register("203", PokeBattle_Move_203::new);
        MoveEffectRegistry.register("204", PokeBattle_Move_204::new);
        MoveEffectRegistry.register("205", PokeBattle_Move_205::new);
        MoveEffectRegistry.register("206", PokeBattle_Move_206::new);
        MoveEffectRegistry.register("207", PokeBattle_Move_207::new);
        MoveEffectRegistry.register("208", PokeBattle_Move_208::new);
        MoveEffectRegistry.register("209", PokeBattle_Move_209::new);
        MoveEffectRegistry.register("210", PokeBattle_Move_210::new);
        MoveEffectRegistry.register("211", PokeBattle_Move_211::new);
        MoveEffectRegistry.register("212", PokeBattle_Move_212::new);
        MoveEffectRegistry.register("213", PokeBattle_Move_213::new);
        MoveEffectRegistry.register("214", PokeBattle_Move_214::new);
        MoveEffectRegistry.register("215", PokeBattle_Move_215::new);
        MoveEffectRegistry.register("216", PokeBattle_Move_216::new);
        MoveEffectRegistry.register("217", PokeBattle_Move_217::new);
        MoveEffectRegistry.register("218", PokeBattle_Move_218::new);
        MoveEffectRegistry.register("219", PokeBattle_Move_219::new);
        MoveEffectRegistry.register("126", PokeBattle_Move_126::new);
        MoveEffectRegistry.register("127", PokeBattle_Move_127::new);
        MoveEffectRegistry.register("128", PokeBattle_Move_128::new);
        MoveEffectRegistry.register("129", PokeBattle_Move_129::new);
        MoveEffectRegistry.register("12A", PokeBattle_Move_12A::new);
        MoveEffectRegistry.register("12B", PokeBattle_Move_12B::new);
        MoveEffectRegistry.register("12C", PokeBattle_Move_12C::new);
        MoveEffectRegistry.register("12D", PokeBattle_Move_12D::new);
        MoveEffectRegistry.register("12E", PokeBattle_Move_12E::new);
        MoveEffectRegistry.register("12F", PokeBattle_Move_12F::new);
        MoveEffectRegistry.register("130", PokeBattle_Move_130::new);
        MoveEffectRegistry.register("131", PokeBattle_Move_131::new);
        MoveEffectRegistry.register("132", PokeBattle_Move_132::new);
    }

}
