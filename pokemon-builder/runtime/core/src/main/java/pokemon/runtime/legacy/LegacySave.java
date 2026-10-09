package pokemon.runtime.legacy;

import pokemon.runtime.legacy.RubyMarshal.RObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The parts of an Essentials {@code Game.rxdata} the transfer needs, read in the order the plugin's {@code transportOldPokemon}
 * does (350_Pokemon_Transporter:49-66): the trainer, eleven other dumps, {@code PokemonGlobalMetadata} (its {@code currentVersion}),
 * two more, then {@code PokemonStorage}.
 */
public final class LegacySave {

    public final RObject trainer;
    public final RObject global;
    public final RObject storage;

    private LegacySave(RObject trainer, RObject global, RObject storage) {
        this.trainer = trainer;
        this.global = global;
        this.storage = storage;
    }

    /** {@code Marshal.load} x15; throws {@link RubyMarshal.MarshalException} when the file is not such a save. */
    public static LegacySave read(byte[] data) {
        RubyMarshal m = new RubyMarshal(data);
        Object trainer = m.load();                                                  // old_trainer = Marshal.load(f)
        Object global = null, storage = null;
        for (int i = 1; i <= 14; i++) {                                             // :50-64
            Object o = m.load();
            if (i == 11) global = o;                                                // :60 old_pokemon_global
            if (i == 14) storage = o;                                               // :64 old_storage
        }
        if (!(trainer instanceof RObject) || !(global instanceof RObject) || !(storage instanceof RObject)) {
            throw new RubyMarshal.MarshalException("not an Essentials save");
        }
        return new LegacySave((RObject) trainer, (RObject) global, (RObject) storage);
    }

    /** {@code old_pokemon_global.currentVersion} (:61). */
    public long version() {
        return RubyMarshal.num(global.get("currentVersion"), -1);
    }

    /** {@code old_trainer.id} (:90). */
    public long trainerId() {
        return RubyMarshal.num(trainer.get("id"), 0);
    }

    public String trainerName() {
        return RubyMarshal.str(trainer.get("name"));
    }

    /** The party as saved (null slots dropped). */
    public List<RObject> party() {
        return pokemonOf(RubyMarshal.list(trainer.get("party")));
    }

    public int boxCount() {
        List<Object> boxes = RubyMarshal.list(storage.get("boxes"));
        return boxes == null ? 0 : boxes.size();
    }

    /** {@code old_storage[index]} (zero based): the Pokémon of that box in slot order, null slots kept as null. */
    public List<RObject> box(int index) {
        List<Object> boxes = RubyMarshal.list(storage.get("boxes"));
        if (boxes == null || index < 0 || index >= boxes.size() || !(boxes.get(index) instanceof RObject)) {
            return new ArrayList<>();
        }
        List<Object> slots = RubyMarshal.list(((RObject) boxes.get(index)).get("pokemon"));
        List<RObject> out = new ArrayList<>();
        if (slots != null) {
            for (Object o : slots) out.add(o instanceof RObject ? (RObject) o : null);
        }
        return out;
    }

    private static List<RObject> pokemonOf(List<Object> list) {
        List<RObject> out = new ArrayList<>();
        if (list != null) {
            for (Object o : list) if (o instanceof RObject) out.add((RObject) o);
        }
        return out;
    }
}
