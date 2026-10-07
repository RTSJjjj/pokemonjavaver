package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code PBStats} (PBStats.rb:1-62), transcribed one constant at
 * a time.
 *
 * <p><b>This numbering is the plugin's and must be used everywhere.</b> Note
 * that {@code HP = 0} is a real stat slot, so the battle stat stages are
 * {@code @stages[PBStats::ATTACK] == @stages[1]} and the array has eight
 * entries (index 0 is never used for stages but is the HP slot in the base-stat
 * / EV arrays). The pre-M0 runtime used a shifted numbering (ATTACK at 0,
 * SPDEF at 4) which this class replaces; see {@link Battler#stages}.</p>
 */
public final class PBStats {

    private PBStats() {
    }

    public static final int HP = 0;
    public static final int ATTACK = 1;
    public static final int DEFENSE = 2;
    public static final int SPEED = 3;
    public static final int SPATK = 4;
    public static final int SPDEF = 5;
    public static final int ACCURACY = 6;
    public static final int EVASION = 7;

    /** {@code PBStats.getName} (PBStats.rb:18-30). */
    public static String getName(int id) {
        String[] names = {"血量", "攻击", "防御", "速度", "特攻", "特防", "命中", "回避"};
        return id >= 0 && id < names.length ? names[id] : null;
    }

    /**
     * {@code PBStats.getNameBrief} (PBStats.rb:32-44): the plugin's table is
     * character-for-character identical to {@link #getName}, so both return the
     * same strings here.
     */
    public static String getNameBrief(int id) {
        return getName(id);
    }

    /** {@code PBStats.eachStat} (PBStats.rb:46-48): HP, ATTACK, DEFENSE, SPATK, SPDEF, SPEED. */
    public static final int[] EACH_STAT = {HP, ATTACK, DEFENSE, SPATK, SPDEF, SPEED};

    /** {@code PBStats.eachMainBattleStat} (PBStats.rb:50-52). */
    public static final int[] EACH_MAIN_BATTLE_STAT = {ATTACK, DEFENSE, SPATK, SPDEF, SPEED};

    /** {@code PBStats.eachBattleStat} (PBStats.rb:54-56). */
    public static final int[] EACH_BATTLE_STAT = {ATTACK, DEFENSE, SPATK, SPDEF, SPEED, ACCURACY, EVASION};

    /** {@code PBStats.validBattleStat?} (PBStats.rb:58-61). */
    public static boolean validBattleStat(int stat) {
        for (int s : EACH_BATTLE_STAT) {
            if (s == stat) {
                return true;
            }
        }
        return false;
    }
}
