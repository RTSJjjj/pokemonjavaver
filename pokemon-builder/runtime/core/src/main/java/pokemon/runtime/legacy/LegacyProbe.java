package pokemon.runtime.legacy;

import pokemon.runtime.legacy.RubyMarshal.RObject;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Developer probe: {@code LegacyProbe <generatedDataRoot> <Game.rxdata> <out.txt> [boxes]} writes the Pokémon of a Ruby save as read
 * from the file next to what this runtime makes of them, so the conversion can be checked against the original game.
 * {@code boxes} is a comma list of box numbers (1 based), {@code party}, or {@code all} (default {@code party,100,200}); with
 * {@code stats} only the totals per save are written (how many species ids / nicknames agree with the PBS).
 */
public final class LegacyProbe {

    public static void main(String[] args) throws Exception {
        PbsData pbs = PbsData.parse(new File(args[0]));
        String spec = args.length > 3 ? args[3] : "party,100,200";
        try (PrintStream out = new PrintStream(new File(args[2]), "UTF-8")) {
            for (String file : args[1].split(";")) {
                probe(pbs, file, spec, out);
            }
        }
    }

    private static void probe(PbsData pbs, String file, String spec, PrintStream out) throws Exception {
        if (file.endsWith(".json")) {
            probeJava(pbs, file, spec, out);
            return;
        }
        LegacySave save;
        try {
            save = LegacySave.read(Files.readAllBytes(Paths.get(file)));
        } catch (RuntimeException e) {
            out.println("##### " + file + " FAILED: " + e);
            return;
        }
        out.println("##### " + file);
        out.println("version=" + save.version() + " trainer=" + save.trainerName() + " id=" + save.trainerId()
                + " (id & 0xFFFF=" + (save.trainerId() & 0xFFFF) + ") boxes=" + save.boxCount());
        boolean stats = spec.equals("stats");
        int total = 0, converted = 0, nameSame = 0, unknown = 0, statsOff = 0;
        java.util.Map<String, Integer> problems = new java.util.TreeMap<>();
        for (int b = -1; b < save.boxCount(); b++) {
            String label = b < 0 ? "party" : "box " + (b + 1);
            boolean wanted = stats || spec.equals("all") || (b < 0 ? containsBox(spec, -1) : containsBox(spec, b + 1));
            if (!wanted) continue;
            List<RObject> list = b < 0 ? save.party() : save.box(b);
            for (int i = 0; i < list.size(); i++) {
                RObject raw = list.get(i);
                if (raw == null) continue;
                LegacyPokemon reader = new LegacyPokemon(pbs);
                Pokemon p = reader.convert(raw);
                if (!stats) describe(out, label, i + 1, raw, p, reader);
                total++;
                if (p == null) unknown++;
                else {
                    converted++;
                    if (raw.get("name") != null && raw.get("name").toString().equals(p.species.name)) nameSame++;
                    String mismatch = statMismatch(raw, p);              // the stored stats against the ones this runtime derives
                    if (mismatch != null) {
                        statsOff++;
                        problems.merge("stats differ: " + mismatch.split(" ")[0], 1, Integer::sum);
                        if (!stats || statsOff <= 6) out.println("     STATS DIFFER" + (stats ? " [" + label + " #" + (i + 1) + "] " + p.species.internalName : "") + ": " + mismatch);
                    }
                }
                for (String note : reader.notes) problems.merge(note, 1, Integer::sum);
            }
        }
        out.println("-- " + total + " Pokémon, " + converted + " converted, " + unknown + " unknown species, " + nameSame
                + " with the stored name equal to the species name, " + statsOff + " whose stored stats differ from the derived ones");
        for (java.util.Map.Entry<String, Integer> e : problems.entrySet()) {
            out.println("   note x" + e.getValue() + ": " + e.getKey());
        }
        out.println();
    }

    /** null when the saved {@code @totalhp/@attack/@defense/@speed/@spatk/@spdef} equal what the runtime derives for the Pokémon. */
    private static String statMismatch(RObject raw, Pokemon p) {
        String[] names = {"totalhp", "attack", "defense", "speed", "spatk", "spdef"};
        int[] derived = {p.maxHp(), p.attack(), p.defense(), p.speed(), p.spAtk(), p.spDef()};
        StringBuilder sb = null;
        for (int i = 0; i < names.length; i++) {
            Object stored = raw.get(names[i]);
            if (!(stored instanceof Long)) continue;
            if ((Long) stored != derived[i]) {
                if (sb == null) sb = new StringBuilder();
                sb.append(names[i]).append(" saved ").append(stored).append(" derived ").append(derived[i]).append("; ");
            }
        }
        return sb == null ? null : sb.toString();
    }

    private static boolean containsBox(String spec, int number) {
        for (String part : spec.split(",")) {
            String p = part.trim();
            if (number < 0 ? p.equals("party") : p.equals(String.valueOf(number))) return true;
        }
        return false;
    }

    private static String val(Object o) {
        return o == null ? "-" : o.toString();
    }

    private static void describe(PrintStream out, String where, int slot, RObject raw, Pokemon p, LegacyPokemon reader) {
        out.println("[" + where + " #" + slot + "]");
        out.println("  raw : species=" + val(raw.get("species")) + " name=" + val(raw.get("name")) + " form=" + val(raw.get("form"))
                + " level=" + val(raw.get("level")) + " exp=" + val(raw.get("exp")) + " item=" + val(raw.get("item"))
                + " abilityflag=" + val(raw.get("abilityflag")) + " natureflag=" + val(raw.get("natureflag"))
                + " genderflag=" + val(raw.get("genderflag")) + " shinyflag=" + val(raw.get("shinyflag"))
                + " supershinyflag=" + val(raw.get("supershinyflag")));
        if (p == null) {
            out.println("  -> NOT CONVERTED: " + reader.notes);
            return;
        }
        printConverted(out, p);
        if (!reader.notes.isEmpty()) out.println("     notes: " + reader.notes);
    }

    private static void printConverted(PrintStream out, Pokemon p) {
        StringBuilder moves = new StringBuilder();
        for (Pokemon.MoveSlot m : p.moves) {
            moves.append(m.move.internalName).append(' ').append(m.pp).append('/').append(m.maxPp).append("(+").append(m.ppUp).append(") ");
        }
        out.println("  -> " + p.species.internalName + " \"" + p.species.name + "\" nick=\"" + p.name + "\" form=" + p.formIndex()
                + (p.form == null ? "" : "(" + p.form.key + ")") + " Lv" + p.level + " exp=" + p.exp + " hp=" + p.hp + "/" + p.maxHp()
                + " item=" + p.item + " ability=" + p.ability + " nature=" + (p.nature == null ? "?" : p.nature.internalName)
                + " gender=" + p.effectiveGender() + " shiny=" + p.shiny + (p.superShiny ? "(super)" : ""));
        out.println("     moves: " + moves.toString().trim());
        out.println("     IV " + java.util.Arrays.toString(p.ivs) + " EV " + java.util.Arrays.toString(p.evs)
                + " happiness=" + p.happiness + " status=" + (p.status.isEmpty() ? "-" : p.status) + " egg=" + p.egg);
        out.println("     OT=" + p.originalTrainer + " otGender=" + p.otGender + " trainerID=" + (p.trainerID & 0xFFFFFFFFL)
                + " personalID=" + (p.personalID & 0xFFFFFFFFL) + " ball=" + p.ballused + " markings=" + p.markings);
        out.println("     met: map " + p.obtainMap + " Lv" + p.obtainLevel + " mode=" + p.obtainMode + " text=" + p.obtainText
                + " hatchedMap=" + p.hatchedMap + "  ribbons=" + p.ribbons + " firstMoves=" + p.firstMoves + " trMoves=" + p.trMoves
                + " pokerus=" + p.pokerus + " battleRank=" + p.battleRank);
    }

    /** A save of this runtime (JSON): the party and the boxes as {@link pokemon.runtime.save.SaveManager#readTrainer} reads them. */
    private static void probeJava(PbsData pbs, String file, String spec, PrintStream out) throws Exception {
        pokemon.runtime.save.SaveManager manager = new pokemon.runtime.save.SaveManager();
        manager.attachPbs(pbs);
        pokemon.runtime.pokemon.TrainerState trainer = manager.readTrainer(new String(Files.readAllBytes(Paths.get(file)), java.nio.charset.StandardCharsets.UTF_8));
        out.println("##### " + file);
        if (trainer == null) {
            out.println("not a save of this runtime");
            return;
        }
        out.println("trainer=" + trainer.name + " id=" + (trainer.id & 0xFFFFFFFFL) + " party=" + trainer.party.size());
        if (containsBox(spec, -1) || spec.equals("all")) {
            for (int i = 0; i < trainer.party.size(); i++) {
                out.println("[party #" + (i + 1) + "]");
                printConverted(out, trainer.party.get(i));
            }
        }
        for (int b = 0; b < pokemon.runtime.pokemon.Storage.BOXES; b++) {
            if (!spec.equals("all") && !containsBox(spec, b + 1)) continue;
            pokemon.runtime.pokemon.Storage.Box box = trainer.storage.boxIfCreated(b);
            if (box == null) continue;
            for (int i = 0; i < box.length(); i++) {
                if (box.get(i) == null) continue;
                out.println("[box " + (b + 1) + " #" + (i + 1) + "]");
                printConverted(out, box.get(i));
            }
        }
    }
}
