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
        return battler.ability;
    }

    /** {@code Battler#abilityActive?} (PokeBattle_Battler:379, default {@code ignoreFainted=false}). */
    public static boolean abilityActive(Battler battler) {
        return battler.abilityActive();
    }

    /** {@code Battler#abilityName} (PokeBattle_Battler:211 {@code PBAbilities.getName(@ability)}). */
    public static String abilityName(Battler battler) {
        return battler.abilityName();
    }

    /** {@code Battler#battle} (PokeBattle_Battler:3 {@code attr_reader :battle}). */
    public static Battle battle(Battler battler) {
        return battler.battle;
    }

    /** {@code Battler#effects} (PokeBattle_Battler:23 {@code attr_accessor :effects}). */
    public static EffectMap effects(Battler battler) {
        return battler.effects;
    }

    /** {@code Battle#field.weather} (PokeBattle_ActiveField:3 {@code attr_accessor :effects}; weather lives on {@code PokeBattle_Battle#field}). */
    public static int fieldWeather(Battle battle) {
        return battle.field.weather;
    }

    /** {@code Battler#hasActiveAbility?} (PokeBattle_Battler:387, default {@code ignoreFainted=false}). */
    public static boolean hasActiveAbility(Battler battler, String ability) {
        return battler.hasActiveAbility(ability);
    }

    /**
     * {@code isConst?(val,mod,constant)} (PSystem_Utilities:205) for the two
     * {@code PBTypes} comparisons the helpers make
     * ({@code isConst?(moveType,PBTypes,immuneType)}). Both arguments are type
     * internal names in this runtime.
     */
    public static boolean isConst(String val, String typeConstant) {
        return typeConstant.equals(val);
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
        String f = move.function();                                    // PokeBattle_PledgeMove = function 106/107/108
        return "106".equals(f) || "107".equals(f) || "108".equals(f);
    }

    /** {@code PBItems.getName(item)} (Compiler_PBS:446 generates {@code def self.getName(id)}). */
    public static String PBItems_getName(String item) {
        throw new UnsupportedOperationException("M0 待接线: PBItems.getName (Compiler_PBS:446)");
    }

    /** {@code Battle#pbCommonAnimation(name,user)} (PokeBattle_Battle:797). */
    public static void pbCommonAnimation(Battle battle, String name, Battler user) {
        battle.commonAnimation(name, user);                     // PokeBattle_Battle:797-799
    }

    /** {@code Battler#pbConfuse} (Battler_Statuses:531, {@code msg=nil}). */
    public static void pbConfuse(Battler battler) {
        battler.pbConfuse();
    }

    /** {@code Battle#pbDisplay} (PokeBattle_Battle:773). */
    public static void pbDisplay(Battle battle, String message) {
        battle.display(message);
    }

    /** {@code Battle#pbHideAbilitySplash} (PokeBattle_Battle:810). */
    public static void pbHideAbilitySplash(Battle battle, Battler battler) {
        battle.hideAbilitySplash(battler);
    }

    /** {@code Battler#pbRecoverHP(amt)} (Battler_ChangeSelf:19). */
    public static int pbRecoverHP(Battler battler, int amount) {
        return battler.pbRecoverHP(amount);
    }

    /** {@code Battle#pbShowAbilitySplash(battler)} (PokeBattle_Battle:801). */
    public static void pbShowAbilitySplash(Battle battle, Battler battler) {
        battle.showAbilitySplash(battler);
    }

    /** {@code Battler#pbThis} (PokeBattle_Battler:214, default {@code lowerCase=false}). */
    public static String pbThis(Battler battler) {
        return battler.pbThis();
    }

    /** {@code Battler#pbThis(true)} (PokeBattle_Battler:214). */
    public static String pbThis(Battler battler, boolean lowerCase) {
        return battler.pbThis(lowerCase);
    }

    /**
     * {@code target.pbWeather} (BattleHandlers_Abilities:4581). 插件缺陷已修: pbWeather is only
     * defined on {@code Battle}, so the plugin line would raise NoMethodError; the battle's weather is meant.
     */
    public static int pbWeather(Battler battler) {
        return battler.battle.pbWeather();                  // 插件缺陷已修: the plugin calls target.pbWeather, which only Battle defines (BattleHandlers_Abilities:4581)
    }

    /** {@code Battle#pbStartTerrain(user,newTerrain,fixedDuration=true)} (PokeBattle_Battle:741). */
    public static void pbStartTerrain(Battle battle, Battler user, int terrain) {
        battle.pbStartTerrain(user, terrain);
    }

    /** {@code Battler#pbCanFrostbiteSynchronize?(target)} (Arceus:846). */
    public static boolean pbCanFrostbiteSynchronize(Battler battler, Battler target) {
        return battler.pbCanFrostbiteSynchronize(target);
    }

    /** {@code Battler#pbFrostbite(user,msg)} (Arceus:852). */
    public static void pbFrostbite(Battler battler, String msg) {
        battler.pbFrostbite(null, msg);
    }

    /** {@code Battler#pbCanDrowseSynchronize?(target)} (Arceus:856). */
    public static boolean pbCanDrowseSynchronize(Battler battler, Battler target) {
        return battler.pbCanDrowseSynchronize(target);
    }

    /** {@code Battler#pbDrowse(user,msg)} (Arceus:863). */
    public static void pbDrowse(Battler battler, String msg) {
        battler.pbDrowse(null, msg);
    }

    /**
     * {@code pbGetEvolvedFormData(target.pokemon.fSpecies,true)} (Pokemon_Evolution:138-148):
     * the {@code [[Method,parameter,species],...]} list of a species' evolutions.
     * Takes the Battler because this runtime's {@code Pokemon} has no
     * {@code fSpecies}. Only the length is read (EVIOLITE,
     * BattleHandlers_Items:910-911).
     */
    public static java.util.List<Object[]> pbGetEvolvedFormData(Battler target, boolean ignoreNone) {
        java.util.List<Object[]> out = new java.util.ArrayList<>();
        pokemon.runtime.pokemon.PbsData.Species sp = target.pokemon.species;
        if (sp != null && sp.evolutions != null) {
            for (pokemon.runtime.pokemon.PbsData.Evolution e : sp.evolutions) {
                if (ignoreNone && (e.method == null || e.method.equals("None"))) continue;   // :143 skips the "None" method
                out.add(new Object[] {e.method, e.parameter, e.species});
            }
        }
        return out;
    }

    /**
     * {@code battle.rules["souldewclause"]} (PokeBattle_Battle:71/:148, written by
     * {@code setRule}, PBattle_OrgBattleRules:986). This runtime has no battle
     * rules map yet; only SOULDEW reads it (BattleHandlers_Items:981).
     */
    public static boolean battleRules(Battle battle, String rule) {
        return battle.rules.get(rule) != null && !Boolean.FALSE.equals(battle.rules.get(rule));   // Ruby truthiness of @rules[rule]
    }

    /**
     * {@code b.pbCanConfuse?(battler,true,self)} (Arceus:3884) - a PLUGIN DEFECT
     * rather than missing wiring: {@code Battler#pbCanConfuse?} is
     * {@code (user=nil,showMessages=true,move=nil,selfInflicted=false)}
     * (Battler_Statuses:488) and the third argument here is {@code self}, i.e. the
     * handler proc, handed over where a move belongs. The 3-argument call shape is
     * kept; it is NOT rewritten as the runtime's 4-argument
     * {@code Battler.pbCanConfuse} with a substituted value, because that would
     * invent a {@code move}.
     */
    public static boolean pbCanConfuse(Battler battler, Battler user, boolean showMessages) {
        return battler.pbCanConfuse(user, showMessages, null, false);   // 插件缺陷已修: the plugin passes the handler proc (self) as the move (Arceus:3884)
    }

    /**
     * {@code b.pbCanSleep?(battler,true,self)} (Arceus:3896) - same plugin defect
     * as {@link #pbCanConfuse}: {@code Battler#pbCanSleep?} is
     * {@code (user,showMessages,move=nil,ignoreStatus=false)}
     * (Battler_Statuses:318) and the third argument is the handler proc.
     */
    public static boolean pbCanSleep(Battler battler, Battler user, boolean showMessages) {
        return battler.pbCanSleep(user, showMessages, null, false);     // 插件缺陷已修: same, Arceus:3896
    }

    /**
     * {@code battler.pbHeldItemTriggerCheck(battler.recycleItem,true)}
     * (BattleHandlers_Abilities:3317, CUDCHEW). Ruby passes an item <em>id</em>
     * there, while this runtime's {@code Battler.item}/{@code recycleItem} are
     * internal-name Strings; the landed
     * {@code Battler.pbHeldItemTriggerCheck(int,boolean)} turns a non-zero id into
     * {@code String.valueOf(id)} (a fake item name), so the name-based overload is
     * the only faithful spelling until that lands.
     */
    public static void pbHeldItemTriggerCheck(Battler battler, String forcedItemName, boolean fling) {
        battler.pbHeldItemTriggerCheck(forcedItemName, fling);
    }

    /**
     * {@code battler.form=(battler.form==0) ? 1 : 0} (BattleHandlers_Abilities:2312,
     * HUNGERSWITCH): this runtime's {@code Battler.form()} is read-only (it reads
     * {@code pokemon.form.form}), and changing the form is state, not presentation.
     */
    public static void setForm(Battler battler, int form) {
        battler.setForm(form);
    }

    /**
     * {@code battler.pbItemOpposingStatGainCheck(statUps)}
     * (Battler_AbilityAndItem:334), the held-item half of
     * {@code AbilityOnOpposingStatGain}: OPPORTUNIST (BattleHandlers_Abilities:3229)
     * and its Mirror Herb loop (:3236) both call it. It is a real
     * {@code Battler} method in the plugin, not a defect.
     */
    public static void pbItemOpposingStatGainCheck(Battler battler, java.util.List<int[]> statUps) {
        battler.pbItemOpposingStatGainCheck(statUps);
    }

    /**
     * {@code Battler#pbSleep(msg=nil)} (Battler_Statuses:354-356):
     * {@code pbInflictStatus(PBStatuses::SLEEP,pbSleepDuration,msg)}, where
     * {@code pbSleepDuration} (:362-366) is
     * {@code duration = 2+@battle.pbRandom(3) if duration<=0} then
     * {@code duration = (duration/2).floor if hasActiveAbility?(:EARLYBIRD)}.
     * Needed by EFFECTSPORE (BattleHandlers_Abilities:1551).
     */
    public static void pbSleep(Battler battler, String msg) {
        battler.pbSleep(msg);
    }

    /**
     * {@code Battler#pbChangeTypes(newType)} (Battler_ChangeSelf:285 - the section
     * defines it TWICE, :157 and :285, and the later one wins, see
     * stage4-m0b-battler-api-notes.md §F.5). Needed by COLORCHANGE
     * (BattleHandlers_Abilities:2034).
     */
    public static void pbChangeTypes(Battler battler, String newType) {
        battler.pbChangeTypes(newType);                         // Battler_ChangeSelf:306-311 (stage 5 / 2d)
    }

    /**
     * {@code Battler#isCommander?} (PokeBattle_Battler:866-869):
     * {@code commander = @effects[PBEffects::Commander]; return commander && commander.length == 1}
     * - true for the rider (a 1-element array) rather than the host (2 elements).
     * Needed by COMMANDER (BattleHandlers_Abilities:3000).
     */
    public static boolean isCommander(Battler battler) {
        return battler.isCommander();
    }

    /**
     * {@code PokeBattle_Move#pp} (PokeBattle_Move.rb:11 {@code attr_accessor :pp},
     * set from {@code move.pp} at :38): the move's CURRENT PP. This runtime keeps
     * the current PP on the owner's {@code Pokemon.MoveSlot.pp}, not on
     * {@link BattleMove}. Needed by CURSEDBODY (BattleHandlers_Abilities:1491).
     */
    public static int movePp(BattleMove move) {
        throw new UnsupportedOperationException("M0 待接线: PokeBattle_Move.rb:11/#pp (PP 存于 Pokemon.MoveSlot.pp)");
    }
}
