package pokemon.runtime.state;

/**
 * Essentials {@code $PokemonTemp.battleRules} (PField_Battles:19-55): the rules
 * {@code setBattleRule} records for the next battle, consumed and cleared when
 * that battle starts (PField_Battles:497-498). Temp state - never saved.
 */
public final class BattleRules {

    /** "single"/"double"/... (PField_Battles:30-32). */
    public String size;
    public Boolean canLose;
    public Boolean canRun;
    public Boolean roamerFlees;
    public Boolean expGain;
    public Boolean moneyGain;
    public Boolean disablePokeBalls;
    public Boolean switchStyle;
    public Boolean battleAnims;
    /** Raw names/ids: the battle engine has no terrain/weather system yet. */
    public Object defaultTerrain;
    public Object defaultWeather;
    public Object environment;
    public String backdrop;
    public Object base;
    /** The game variable that receives the battle decision (default 1). */
    public Integer outcomeVar;
    public Boolean noPartner;

    /**
     * {@code recordBattleRule} (PField_Battles:27-55). Unknown rules are
     * reported by the caller (the plugin raises).
     *
     * @return false when the rule name is not part of the plugin's list
     */
    public boolean record(String rule, Object value) {
        String key = rule == null ? "" : rule.toLowerCase(java.util.Locale.ROOT);
        switch (key) {
            case "single": case "1v1": case "1v2": case "2v1":
            case "1v3": case "3v1": case "double": case "2v2":
            case "2v3": case "3v2": case "triple": case "3v3":
                size = key;
                return true;
            case "canlose": canLose = true; return true;
            case "cannotlose": canLose = false; return true;
            case "canrun": canRun = true; return true;
            case "cannotrun": canRun = false; return true;
            case "roamerflees": roamerFlees = true; return true;
            case "noexp": expGain = false; return true;
            case "nomoney": moneyGain = false; return true;
            case "disablepokeballs": disablePokeBalls = true; return true;
            case "switchstyle": switchStyle = true; return true;
            case "setstyle": switchStyle = false; return true;
            case "anims": battleAnims = true; return true;
            case "noanims": battleAnims = false; return true;
            case "terrain": defaultTerrain = value; return true;
            case "weather": defaultWeather = value; return true;
            case "environment": case "environ": environment = value; return true;
            case "backdrop": case "battleback": backdrop = value == null ? null : String.valueOf(value); return true;
            case "base": base = value; return true;
            case "outcome": case "outcomevar": outcomeVar = asInt(value); return true;
            case "nopartner": noPartner = true; return true;
            default:
                return false;
        }
    }

    private static Integer asInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    /** {@code $PokemonTemp.waitingTrainer} (PField_Metadata:213): {trainer data, event id}; not cleared with the rules. */
    public Object waitingTrainer;

    /** {@code clearBattleRules} (PField_Battles:23-25). */
    public void clear() {
        size = null;
        canLose = null;
        canRun = null;
        roamerFlees = null;
        expGain = null;
        moneyGain = null;
        disablePokeBalls = null;
        switchStyle = null;
        battleAnims = null;
        defaultTerrain = null;
        defaultWeather = null;
        environment = null;
        backdrop = null;
        base = null;
        outcomeVar = null;
        noPartner = null;
    }
}
