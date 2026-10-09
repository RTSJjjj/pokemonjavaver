package pokemon.runtime.legacy;

import java.nio.file.Files;
import java.nio.file.Paths;

/** Developer probe: {@code SaveDump <Game.rxdata> [depth] [maxItems]} prints every Marshal dump of a Ruby save as a tree. */
public final class SaveDump {
    public static void main(String[] args) throws Exception {
        RubyMarshal m = new RubyMarshal(Files.readAllBytes(Paths.get(args[0])));
        int depth = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        int max = args.length > 2 ? Integer.parseInt(args[2]) : 8;
        int n = 0;
        if (args.length > 3 && args[3].startsWith("box:")) {                 // box:<number>:<count> prints that many Pokémon of a box
            String[] parts = args[3].split(":");
            for (int i = 0; i <= 14; i++) {
                Object o = m.load();
                if (i != 14) continue;
                RubyMarshal.RObject storage = (RubyMarshal.RObject) o;
                Object box = RubyMarshal.list(storage.get("boxes")).get(Integer.parseInt(parts[1]) - 1);
                int want = Integer.parseInt(parts[2]), shown = 0;
                for (Object p : RubyMarshal.list(((RubyMarshal.RObject) box).get("pokemon"))) {
                    if (p == null || shown++ >= want) continue;
                    System.out.print(RubyMarshal.dump(p, depth, max));
                    System.out.println("-----");
                }
            }
            return;
        }
        while (!m.eof()) {
            int at = m.position();
            try {
                Object o = m.load();
                if (args.length > 3 && !args[3].isEmpty() && Integer.parseInt(args[3]) != n) {
                    n++;
                    continue;
                }
                System.out.println("=== dump " + n + " @" + at + " ===");
                System.out.print(RubyMarshal.dump(o, depth, max));
            } catch (RuntimeException e) {
                System.out.println("=== dump " + n + " @" + at + " FAILED: " + e);
                break;
            }
            n++;
        }
    }
}
