package pokemon.runtime.battle;

/**
 * Stage 4 / M0: the temporary stub layer for every {@code Battler} / {@code
 * Battle} method that {@link BattleHandlers} and {@link BattleHandlerHelpers}
 * call but that this runtime has not landed yet.
 *
 * <h2>Temporary by design</h2>
 * The two target files ({@code Battler.java}, {@code Battle.java}) are being
 * edited by another agent during M0, so their missing methods cannot be added
 * here. Every method below is a <b>throwing stub</b> - deliberately not a
 * default value, because returning one would silently invent plugin behaviour.
 * When the real methods land, each call site becomes a direct call and this
 * class is deleted.
 *
 * <h2>Naming</h2>
 * Method names follow Ruby. A plugin <i>method</i> keeps its name
 * ({@code pbDisplay}, {@code pbThis}, ...); a plugin <i>module function</i>
 * ({@code PBItems.getName}, {@code PBNatures.getStatRaised},
 * {@code PBTypes.resistant?}) cannot keep the dot in Java, so it is written with
 * an underscore ({@code PBItems_getName}). Each javadoc names the Ruby source.
 *
 * <h2>Documented deviations behind these signatures</h2>
 * <ul>
 * <li>{@code ability} / {@code item} are internal-name Strings
 *     ({@code HandlerHash}'s documented deviation; {@code Pokemon.item} is a
 *     String).</li>
 * <li>{@code nature} is the numeric nature id, as in Ruby
 *     ({@code PokeBattle_Battler:137}).</li>
 * <li>{@code status} / {@code stat} / {@code weather} stay numeric.</li>
 * </ul>
 */
public final class PendingApi {

    private PendingApi() {
    }

    /** {@code Battler#ability} (PokeBattle_Battler:11 {@code attr_accessor :ability}). */
    public static String ability(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:11 attr_accessor :ability");
    }

    /** {@code Battler#abilityActive?} (PokeBattle_Battler:379, default {@code ignoreFainted=false}). */
    public static boolean abilityActive(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:379 abilityActive?");
    }

    /** {@code Battler#abilityName} (PokeBattle_Battler:211 {@code PBAbilities.getName(@ability)}). */
    public static String abilityName(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:211 abilityName");
    }

    /** {@code Battler#battle} (PokeBattle_Battler:3 {@code attr_reader :battle}). */
    public static Battle battle(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:3 attr_reader :battle");
    }

    /** {@code Battler#canHeal?} (PokeBattle_Battler:684). */
    public static boolean canHeal(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:684 canHeal?");
    }

    /** {@code Battler#damageState.typeMod} (PokeBattle_Battler:42 {@code attr_accessor :damageState}). */
    public static int damageStateTypeMod(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:42 damageState.typeMod");
    }

    /** {@code Battler#effects} (PokeBattle_Battler:23 {@code attr_accessor :effects}). */
    public static EffectMap effects(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:23 attr_accessor :effects");
    }

    /** {@code Battle#field.weather} (PokeBattle_ActiveField:3 {@code attr_accessor :effects}; weather lives on {@code PokeBattle_Battle#field}). */
    public static int fieldWeather(Battle battle) {
        throw new UnsupportedOperationException("M0 待接线: battle.field.weather (PokeBattle_ActiveField)");
    }

    /** {@code Battler#hasActiveAbility?} (PokeBattle_Battler:387, default {@code ignoreFainted=false}). */
    public static boolean hasActiveAbility(Battler battler, String ability) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:387 hasActiveAbility?");
    }

    /**
     * {@code isConst?(val,mod,constant)} (PSystem_Utilities:205) for the two
     * {@code PBTypes} comparisons the helpers make
     * ({@code isConst?(moveType,PBTypes,immuneType)}). Both arguments are type
     * internal names in this runtime.
     */
    public static boolean isConst(String val, String typeConstant) {
        throw new UnsupportedOperationException("M0 待接线: PSystem_Utilities:205 isConst? / PBTypes");
    }

    /**
     * {@code move.is_a?(PokeBattle_PledgeMove)} (BattleHandlers:635 - the only
     * call site in the project).
     *
     * <p>Lead-verified mapping: the base class {@code PokeBattle_PledgeMove} is
     * declared at {@code Move_Effects_Generic.rb:708}, and the ONLY three
     * subclasses are {@code PokeBattle_Move_106/107/108}
     * ({@code Move_Effects_100-17F.rb:106/122/138}), i.e. Grass/Fire/Water
     * Pledge. The plugin picks a move's class from its function code
     * ({@code PokeBattle_Move.pbFromPBMove}), so
     * {@code move.is_a?(PokeBattle_PledgeMove)} &equiv;
     * {@code move.function()} is one of {@code "106"}, {@code "107"},
     * {@code "108"}.</p>
     *
     * <p>The real implementation belongs on {@link BattleMove} (a
     * {@code isPledgeMove()} predicate) and this stub then disappears.</p>
     */
    public static boolean isPledgeMove(BattleMove move) {
        // Wiring: BattleMove.isPledgeMove() == "106"/"107"/"108".equals(function()).
        throw new UnsupportedOperationException("M0 待接线: BattleHandlers:635 is_a?(PokeBattle_PledgeMove) = function code 106/107/108 (Move_Effects_Generic:708, Move_Effects_100-17F:106/122/138)");
    }

    /** {@code Battler#item} (PokeBattle_Battler:69 {@code attr_reader :item}). */
    public static String item(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:69 attr_reader :item");
    }

    /** {@code Battler#nature} (PokeBattle_Battler:137 {@code @pokemon ? @pokemon.nature : 0}). */
    public static int nature(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:137 nature");
    }

    /** {@code PBItems.getName(item)} (Compiler_PBS:446 generates {@code def self.getName(id)}). */
    public static String PBItems_getName(String item) {
        throw new UnsupportedOperationException("M0 待接线: PBItems.getName (Compiler_PBS:446)");
    }

    /** {@code PBNatures.getStatRaised(id)} (PBNatures:63-67). */
    public static int PBNatures_getStatRaised(int nature) {
        throw new UnsupportedOperationException("M0 待接线: PBNatures:63 getStatRaised");
    }

    /** {@code PBNatures.getStatLowered(id)} (PBNatures:69-73). */
    public static int PBNatures_getStatLowered(int nature) {
        throw new UnsupportedOperationException("M0 待接线: PBNatures:69 getStatLowered");
    }

    /** {@code PBTypes.resistant?(attackType)} (PBTypes_Extra:73, single-argument form). */
    public static boolean PBTypes_resistant(int attackType) {
        throw new UnsupportedOperationException("M0 待接线: PBTypes_Extra:73 resistant?");
    }

    /** {@code Battler#pbCanConfuseSelf?} (Battler_Statuses:527). */
    public static boolean pbCanConfuseSelf(Battler battler, boolean showMessages) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:527 pbCanConfuseSelf?");
    }

    /**
     * {@code Battler#pbCanConsumeBerry?} (Battler_AbilityAndItem:152,
     * {@code alwaysCheckGluttony=true} by default).
     */
    public static boolean pbCanConsumeBerry(Battler battler, String item, boolean alwaysCheckGluttony) {
        throw new UnsupportedOperationException("M0 待接线: Battler_AbilityAndItem:152 pbCanConsumeBerry?");
    }

    /** {@code Battler#pbCanRaiseStatStage?(stat,user)} (Battler_StatStages:9). */
    public static boolean pbCanRaiseStatStage(Battler battler, int stat, Battler user) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:9 pbCanRaiseStatStage?");
    }

    /** {@code Battle#pbCommonAnimation(name,user)} (PokeBattle_Battle:797). */
    public static void pbCommonAnimation(Battle battle, String name, Battler user) {
        battle.commonAnimation(name, user);                     // PokeBattle_Battle:797-799
    }

    /** {@code Battler#pbConfuse} (Battler_Statuses:531, {@code msg=nil}). */
    public static void pbConfuse(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: Battler_Statuses:531 pbConfuse");
    }

    /** {@code Battle#pbDisplay} (PokeBattle_Battle:773). */
    public static void pbDisplay(Battle battle, String message) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:773 pbDisplay");
    }

    /** {@code Battle#pbHideAbilitySplash} (PokeBattle_Battle:810). */
    public static void pbHideAbilitySplash(Battle battle, Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:810 pbHideAbilitySplash");
    }

    /** {@code Battler#pbRaiseStatStage(stat,increment,user)} (Battler_StatStages:47). */
    public static boolean pbRaiseStatStage(Battler battler, int stat, int increment, Battler user) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:47 pbRaiseStatStage");
    }

    /** {@code Battler#pbRaiseStatStageByCause(stat,increment,user,cause)} (Battler_StatStages:74). */
    public static boolean pbRaiseStatStageByCause(Battler battler, int stat, int increment, Battler user, String cause) {
        throw new UnsupportedOperationException("M0 待接线: Battler_StatStages:74 pbRaiseStatStageByCause");
    }

    /** {@code Battler#pbRecoverHP(amt)} (Battler_ChangeSelf:19). */
    public static int pbRecoverHP(Battler battler, int amount) {
        throw new UnsupportedOperationException("M0 待接线: Battler_ChangeSelf:19 pbRecoverHP");
    }

    /** {@code Battle#pbShowAbilitySplash(battler)} (PokeBattle_Battle:801). */
    public static void pbShowAbilitySplash(Battle battle, Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:801 pbShowAbilitySplash");
    }

    /** {@code Battle#pbStartWeather(user,newWeather,fixedDuration)} (PokeBattle_Battle:679). */
    public static void pbStartWeather(Battle battle, Battler user, int newWeather, boolean fixedDuration) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:679 pbStartWeather");
    }

    /** {@code Battler#pbThis} (PokeBattle_Battler:214, default {@code lowerCase=false}). */
    public static String pbThis(Battler battler) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:214 pbThis");
    }

    /** {@code Battler#pbThis(true)} (PokeBattle_Battler:214). */
    public static String pbThis(Battler battler, boolean lowerCase) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battler:214 pbThis(lowerCase)");
    }

    /** {@code Battler#damageState.berryWeakened = true} (BattleHandlers:653). */
    public static void setDamageStateBerryWeakened(Battler battler, boolean value) {
        throw new UnsupportedOperationException("M0 待接线: BattleHandlers:653 damageState.berryWeakened=");
    }

    /**
     * {@code target.pbWeather} (BattleHandlers_Abilities:4581) - a PLUGIN
     * DEFECT rather than missing wiring: {@code pbWeather} is defined on
     * {@code PokeBattle_Battle} (:674) but NOT on {@code PokeBattle_Battler}
     * (the battler only has {@code effectiveWeather}), so this line raises
     * {@code NoMethodError} in Ruby. The stub is deliberate - "throws when
     * called" is the faithful mapping, and inventing a working implementation
     * (e.g. {@code target.battle.pbWeather}) would add behaviour the plugin
     * does not have. Registered as defect #380 in the L1' roster.
     */
    public static int pbWeather(Battler battler) {
        throw new UnsupportedOperationException(
                "插件缺陷: BattleHandlers_Abilities:4581 target.pbWeather - Battler 无此方法, Ruby 会 NoMethodError");
    }

    /** {@code Battle#pbStartTerrain(user,newTerrain,fixedDuration=true)} (PokeBattle_Battle:741). */
    public static void pbStartTerrain(Battle battle, Battler user, int terrain) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Battle:741 pbStartTerrain");
    }

    /** {@code Battler#pbCanFrostbiteSynchronize?(target)} (Arceus:846). */
    public static boolean pbCanFrostbiteSynchronize(Battler battler, Battler target) {
        throw new UnsupportedOperationException("M0 待接线: Arceus:846 pbCanFrostbiteSynchronize?");
    }

    /** {@code Battler#pbFrostbite(user,msg)} (Arceus:852). */
    public static void pbFrostbite(Battler battler, String msg) {
        throw new UnsupportedOperationException("M0 待接线: Arceus:852 pbFrostbite");
    }

    /** {@code Battler#pbCanDrowseSynchronize?(target)} (Arceus:856). */
    public static boolean pbCanDrowseSynchronize(Battler battler, Battler target) {
        throw new UnsupportedOperationException("M0 待接线: Arceus:856 pbCanDrowseSynchronize?");
    }

    /** {@code Battler#pbDrowse(user,msg)} (Arceus:863). */
    public static void pbDrowse(Battler battler, String msg) {
        throw new UnsupportedOperationException("M0 待接线: Arceus:863 pbDrowse");
    }
}
