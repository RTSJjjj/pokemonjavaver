package pokemon.runtime.battle;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import pokemon.runtime.pokemon.PbsData;

/**
 * Finds the original / signature moves that only one Pokemon (line) can learn,
 * so the trainer AI can favour them: a move counts as a signature move of a
 * species when every species that can learn it by level-up, egg move, TM/tutor
 * ({@code tmCompatibility}) or a form-specific list belongs to the user's own
 * evolution family. Moves nobody can learn are NOT signature moves.
 */
final class AiSignature {

    /** Per-PBS cache: move internal name -> learner species (internal names). */
    private static final Map<PbsData, Map<String, Set<String>>> LEARNERS = new WeakHashMap<>();
    /** Per-PBS cache: species internal name -> evolution family id. */
    private static final Map<PbsData, Map<String, String>> FAMILY = new WeakHashMap<>();

    private AiSignature() {
    }

    /** Whether {@code moveName} is a signature move of {@code speciesName}'s family. */
    static synchronized boolean isSignature(PbsData pbs, String speciesName, String moveName) {
        if (pbs == null || speciesName == null || moveName == null) {
            return false;
        }
        Set<String> learners = learners(pbs).get(moveName);
        if (learners == null || learners.isEmpty() || !learners.contains(speciesName)) {
            return false;
        }
        Map<String, String> family = family(pbs);
        String mine = family.getOrDefault(speciesName, speciesName);
        for (String learner : learners) {
            if (!mine.equals(family.getOrDefault(learner, learner))) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Set<String>> learners(PbsData pbs) {
        Map<String, Set<String>> cached = LEARNERS.get(pbs);
        if (cached != null) {
            return cached;
        }
        Map<String, Set<String>> out = new HashMap<>();
        for (ObjectMap.Entry<String, PbsData.Species> e : pbs.species) {
            String name = e.key;
            PbsData.Species sp = e.value;
            if (sp.moves != null) {
                for (PbsData.LearnMove lm : sp.moves) {
                    add(out, lm.move, name);
                }
            }
            if (sp.eggMoves != null) {
                for (String m : sp.eggMoves) {
                    add(out, m, name);
                }
            }
        }
        for (ObjectMap.Entry<String, Array<String>> e : pbs.tmCompatibility) {
            for (String sp : e.value) {
                add(out, e.key, sp);
            }
        }
        for (ObjectMap.Entry<String, PbsData.SpeciesForm> e : pbs.forms) {
            PbsData.SpeciesForm form = e.value;
            if (form.moves != null && form.species != null) {
                for (PbsData.LearnMove lm : form.moves) {
                    add(out, lm.move, form.species);
                }
            }
        }
        LEARNERS.put(pbs, out);
        return out;
    }

    private static void add(Map<String, Set<String>> map, String move, String species) {
        if (move == null || species == null) {
            return;
        }
        map.computeIfAbsent(move, k -> new HashSet<>()).add(species);
    }

    /** Union-find over the evolution graph: every species gets its family's root. */
    private static Map<String, String> family(PbsData pbs) {
        Map<String, String> cached = FAMILY.get(pbs);
        if (cached != null) {
            return cached;
        }
        Map<String, String> parent = new HashMap<>();
        for (ObjectMap.Entry<String, PbsData.Species> e : pbs.species) {
            parent.putIfAbsent(e.key, e.key);
            if (e.value.evolutions == null) {
                continue;
            }
            for (PbsData.Evolution evo : e.value.evolutions) {
                if (evo.species != null) {
                    parent.putIfAbsent(evo.species, evo.species);
                    union(parent, e.key, evo.species);
                }
            }
        }
        Map<String, String> out = new HashMap<>();
        for (String k : parent.keySet()) {
            out.put(k, find(parent, k));
        }
        FAMILY.put(pbs, out);
        return out;
    }

    private static String find(Map<String, String> parent, String x) {
        String root = x;
        while (!parent.get(root).equals(root)) {
            root = parent.get(root);
        }
        return root;
    }

    private static void union(Map<String, String> parent, String a, String b) {
        String ra = find(parent, a);
        String rb = find(parent, b);
        if (!ra.equals(rb)) {
            parent.put(ra, rb);
        }
    }
}
