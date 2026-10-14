package pokemon.runtime.pokemon;

import java.util.Random;

/**
 * {@code MultipleForms.call("getFormOnCreation",self)} (198_Pokemon_Forms:39-48 PokeBattle_Pokemon#initialize): the form a
 * newly made Pokemon of these species starts in, by the registrations of 198_Pokemon_Forms. Until this existed every wild
 * Unown, Basculin, Flabebe, Oricorio, Minior, Tauros... appeared in its default form.
 */
public final class FormsOnCreation {
    private FormsOnCreation() {
    }

    /** What the procs read off the world. */
    public interface World {
        /** {@code pbGetEnvironment} (174_PField_Battles:194-217), as a {@code PBEnvironment} id. */
        int environment();

        /** {@code pbGetCurrentRegion}: the region of the map the player is on, -1 when unknown. */
        int region();

        /** {@code PBDayNight.isEvening?} (hours 19 to 21). */
        boolean evening();

        /** {@code PBDayNight.isNight?} (hours 18 to 23 and 0 to 5). */
        boolean night();

        /** {@code $game_switches[id]} (176_PField_EncounterModifiers). */
        boolean switchOn(int id);
    }

    // PBEnvironment ids (PBEnvironment.rb:3-12) the Burmy procs compare against
    private static final int ENV_NONE = 0;
    private static final int ENV_CAVE = 7;
    private static final int ENV_ROCK = 8;
    private static final int ENV_SAND = 9;

    /** The TOXEL natures that give the Low Key form (198:778-786): LONELY, ADAMANT, NAUGHTY, BRAVE, BOLD, ... */
    private static final int[] LOW_KEY_NATURES = {1, 5, 7, 10, 12, 15, 16, 17, 18, 20, 21, 23};

    /**
     * The form the species' {@code getFormOnCreation} proc gives, or -1 when the species registers none. {@code pkmn}
     * already has its personal ID (the gender and the nature are read from it, as the Ruby does lazily).
     */
    public static int formOnCreation(Pokemon pkmn, TrainerState trainer, World world, Random random) {
        if (pkmn == null || pkmn.species == null) return -1;
        switch (pkmn.species.internalName) {
            case "UNOWN":                                                       // :268-270
                return random.nextInt(28);
            case "BURMY":                                                       // :286-296
            case "WORMADAM": {                                                  // :310-320
                int env = world == null ? ENV_NONE : world.environment();
                if (env == ENV_ROCK || env == ENV_SAND || env == ENV_CAVE) return 1;   // Sandy Cloak
                if (env == ENV_NONE) return 2;                                  // Trash Cloak
                return 0;                                                       // Plant Cloak
            }
            case "BASCULIN":                                                    // :435-437
                return random.nextInt(2);
            case "SCATTERBUG":                                                  // :534-538 $Trainer.secretID%18
            case "SPEWPA":
            case "VIVILLON":
                return trainer == null ? 0 : (trainer.id >>> 16) % 18;
            case "FLABEBE":                                                     // :542-546
            case "FLOETTE":
            case "FLORGES":
                return random.nextInt(5);
            case "ESPURR":                                                      // :551-558 pkmn.gender
            case "MEOWSTIC":
            case "INDEEDEE":
            case "LECHONK":                                                     // :1186-1193
            case "OINKOLOGNE":
                return pkmn.effectiveGender();
            case "PUMPKABOO":                                                   // :570-579
            case "GOURGEIST": {
                int r = random.nextInt(100);
                if (r < 5) return 3;                                            // Super Size (5%)
                if (r < 20) return 2;                                           // Large (15%)
                if (r < 65) return 1;                                           // Average (45%)
                return 0;                                                       // Small (35%)
            }
            case "ORICORIO":                                                    // :633-635
                return random.nextInt(4);
            case "LYCANROC": {                                                  // :647-652
                if (world != null && world.evening()) return 2;                 // Dusk
                if (world != null && world.night()) return 1;                   // Midnight
                return 0;                                                       // Midday
            }
            case "MINIOR":                                                      // :723-725 the Core forms 7-13
                return 7 + random.nextInt(7);
            case "TOXEL":                                                       // :778-786
            case "TOXTRICITY": {
                int nature = Math.floorMod(pkmn.personalID, 25);                // the nature of the personal ID
                for (int n : LOW_KEY_NATURES) if (n == nature) return 1;
                return 0;
            }
            case "TAUROS":                                                      // :1167-1173
            case "WOOPER": {                                                    // :1175-1181
                // The plugin writes `ret = 0 if pbGetCurrentRegion > 0` and then overwrites ret with the roll on the next
                // line, so the region check never applied. Its intent is kept: outside region 0 only the plain form.
                int region = world == null ? 0 : world.region();
                if (region > 0) return 0;
                return random.nextInt("TAUROS".equals(pkmn.species.internalName) ? 4 : 2);
            }
            case "TANDEMAUS":                                                   // :1194-1199
            case "MAUSHOLD":
            case "DUNSPARCE":                                                   // :1219-1224
            case "DUDUNSPARCE":
                return Math.floorMod(pkmn.personalID, 100) == 0 ? 1 : 0;
            case "SQUAWKABILLY":                                                // :1201-1205
                return random.nextInt(4);
            case "TATSUGIRI":                                                   // :1213-1217
                return random.nextInt(3);
            case "GIMMIGHOUL":                                                  // :1226-1230
                return Math.floorMod(pkmn.personalID, 1000) == 0 ? 0 : 1;
            case "POLTCHAGEIST":                                                // :1232-1239
            case "SINISTCHA":
                return random.nextInt(100) < 10 ? 1 : 0;                        // Antique / Phony
            default:
                return -1;
        }
    }

    /**
     * PokeBattle_Pokemon#initialize's tail (198:43-47): {@code if @form==0; f = getFormOnCreation; if f; self.form = f;
     * self.resetMoves}. The HP follows the new form's stats (form= calls calcStats on a fresh, full-HP Pokemon).
     */
    public static void apply(Pokemon pkmn, PbsData data, TrainerState trainer, World world, Random random) {
        if (pkmn == null || pkmn.formIndex() != 0) return;
        int form = formOnCreation(pkmn, trainer, world, random);
        if (form < 0) return;
        pkmn.setForm(data, form);
        pkmn.resetMoves(data);
        pkmn.hp = pkmn.maxHp();
    }
}
