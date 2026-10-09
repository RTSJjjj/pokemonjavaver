package pokemon.runtime.legacy;

import pokemon.runtime.legacy.RubyMarshal.RObject;
import pokemon.runtime.legacy.RubyMarshal.RString;
import pokemon.runtime.legacy.RubyMarshal.RUserData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.Ribbons;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a Ruby {@code PokeBattle_Pokemon} read from a {@code Game.rxdata} ({@link RubyMarshal}) into this runtime's {@link Pokemon}.
 * The save holds numeric ids (species, items, moves, ribbons) and the Essentials "flag" fields that override what the personal id
 * would give; they are resolved against the current PBS exactly like the Ruby getters do (197_PokeBattle_Pokemon:166-349):
 * {@code gender} (:166-177), {@code ability} (:224-245), {@code nature} (:286-288), {@code shiny?} (:314-321). Everything the
 * conversion could not place is added to {@link #notes} (an id the PBS does not know, a field this runtime does not keep).
 */
public final class LegacyPokemon {

    /** What the conversion could not carry over, for the probe and the transfer messages. */
    public final List<String> notes = new ArrayList<>();
    private final PbsData pbs;

    public LegacyPokemon(PbsData pbs) {
        this.pbs = pbs;
    }

    /** @return the converted Pokémon, or null when its species is not in the PBS (the note says which id) */
    public Pokemon convert(RObject raw) {
        long speciesId = RubyMarshal.num(raw.get("species"), 0);
        String internal = pbs.speciesById.get(String.valueOf(speciesId));
        PbsData.Species species = internal == null ? null : pbs.species(internal);
        if (species == null) {
            notes.add("unknown species id " + speciesId);
            return null;
        }
        Object nick = raw.get("name");
        int level = (int) Math.max(1, RubyMarshal.num(raw.get("level"), 1));
        Pokemon p = new Pokemon(species, level, pbs);
        p.moves.clear();
        p.name = nick instanceof RString && !nick.toString().isEmpty() ? nick.toString() : species.name;

        p.personalID = (int) RubyMarshal.num(raw.get("personalID"), 0);
        p.setTrainerID((int) RubyMarshal.num(raw.get("trainerID"), 0));
        int form = (int) RubyMarshal.num(raw.get("form"), 0);
        if (form > 0) p.setForm(pbs, form);

        p.ivs = sixInts(raw.get("iv"));
        p.evs = sixInts(raw.get("ev"));
        p.exp = (int) RubyMarshal.num(raw.get("exp"), p.exp);
        p.happiness = (int) RubyMarshal.num(raw.get("happiness"), p.happiness);

        // nature (:286-288): the flag, else personalID % 25
        Object natureFlag = raw.get("natureflag");
        int natureId = natureFlag instanceof Long ? (int) (long) (Long) natureFlag : (int) Long.remainderUnsigned(p.personalID & 0xFFFFFFFFL, 25);
        p.nature = pbs.nature(natureId);
        if (p.nature == null) notes.add("unknown nature id " + natureId);

        // ability (:224-245): abilityflag, else personalID & 1; 2 and up is the hidden ability
        Object abilityFlag = raw.get("abilityflag");
        p.setAbilitySlot(abilityFlag instanceof Long ? (int) (long) (Long) abilityFlag : p.personalID & 1);
        if (p.ability == null) p.ability = species.abilities.size > 0 ? species.abilities.get(0) : null;

        // gender (:166-177): the single-gender rates decide, then genderflag, else the personal id
        Object genderFlag = raw.get("genderflag");                                 // left unset (derived) unless the save forced one
        if (!PokemonStats.singleGender(p.genderRate()) && genderFlag instanceof Long
                && ((Long) genderFlag == 0L || (Long) genderFlag == 1L)) {
            p.gender = (int) (long) (Long) genderFlag;
        }

        // shininess (:314-349): the flag, else the personal id against the trainer id
        Object shinyFlag = raw.get("shinyflag");
        p.shiny = shinyFlag instanceof Boolean ? (Boolean) shinyFlag : Pokemon.isShiny(p.personalID, p.trainerID);
        p.superShiny = p.shiny && Boolean.TRUE.equals(raw.get("supershinyflag"));

        // moves: PBMove id / pp / ppup (085_PBMove)
        List<Object> moves = RubyMarshal.list(raw.get("moves"));
        if (moves != null) {
            for (Object m : moves) {
                if (!(m instanceof RObject)) continue;
                RObject mv = (RObject) m;
                long id = RubyMarshal.num(mv.get("id"), 0);
                if (id <= 0) continue;
                PbsData.Move move = pbs.moveById((int) id);
                if (move == null) {
                    notes.add("unknown move id " + id);
                    continue;
                }
                Pokemon.MoveSlot slot = new Pokemon.MoveSlot(move);
                slot.setPpUp((int) RubyMarshal.num(mv.get("ppup"), 0));
                slot.pp = (int) Math.max(0, Math.min(slot.maxPp, RubyMarshal.num(mv.get("pp"), slot.maxPp)));
                p.moves.add(slot);
            }
        }
        moveNames(raw.get("firstmoves"), p.firstMoves, "first move");
        moveNames(raw.get("trmoves"), p.trMoves, "TR move");

        // item
        long itemId = RubyMarshal.num(raw.get("item"), 0);
        if (itemId > 0) {
            PbsData.Item item = pbs.itemById((int) itemId);
            if (item == null) notes.add("unknown item id " + itemId);
            p.item = item == null ? null : item.internalName;
        }

        // ribbons
        List<Object> ribbons = RubyMarshal.list(raw.get("ribbons"));
        if (ribbons != null) {
            for (Object r : ribbons) {
                if (r instanceof Long) p.giveRibbon(String.valueOf(r));
            }
        }

        // egg (:eggsteps > 0), status
        long eggSteps = RubyMarshal.num(raw.get("eggsteps"), 0);
        p.egg = eggSteps > 0;
        p.stepsToHatch = (int) eggSteps;
        p.status = statusName(RubyMarshal.num(raw.get("status"), 0));
        p.statusCount = (int) RubyMarshal.num(raw.get("statusCount"), 0);

        // trainer / origin
        Object ot = raw.get("ot");
        p.originalTrainer = ot instanceof RString && !ot.toString().isEmpty() ? ot.toString() : null;
        p.otGender = (int) RubyMarshal.num(raw.get("otgender"), -1);
        p.ballused = (int) RubyMarshal.num(raw.get("ballused"), 0);
        p.markings = (int) RubyMarshal.num(raw.get("markings"), 0);
        p.obtainMap = (int) RubyMarshal.num(raw.get("obtainMap"), 0);
        p.obtainLevel = (int) RubyMarshal.num(raw.get("obtainLevel"), level);
        p.obtainMode = (int) RubyMarshal.num(raw.get("obtainMode"), 0);
        Object obtainText = raw.get("obtainText");
        p.obtainText = obtainText instanceof RString && !obtainText.toString().isEmpty() ? obtainText.toString() : null;
        p.hatchedMap = (int) RubyMarshal.num(raw.get("hatchedMap"), 0);
        p.timeEggHatched = epochSeconds(raw.get("timeEggHatched"));
        p.pokerus = (int) RubyMarshal.num(raw.get("pokerus"), 0);
        p.battleRank = (int) RubyMarshal.num(raw.get("battleRank"), 0);
        p.criticalHits = (int) RubyMarshal.num(raw.get("criticalHits"), 0);
        p.yamaskhp = (int) RubyMarshal.num(raw.get("yamaskhp"), 0);

        // current HP (the totals are derived from the stats above, the stored @totalhp is not kept)
        p.hp = (int) Math.max(0, Math.min(p.maxHp(), RubyMarshal.num(raw.get("hp"), p.maxHp())));

        Object fused = raw.get("fused");
        if (fused instanceof RObject) p.fused = convert((RObject) fused);
        if (raw.get("mail") instanceof RObject) notes.add("mail not carried over");
        return p;
    }

    private void moveNames(Object ids, com.badlogic.gdx.utils.Array<String> into, String what) {
        List<Object> list = RubyMarshal.list(ids);
        if (list == null) return;
        for (Object o : list) {
            long id = RubyMarshal.num(o, 0);
            if (id <= 0) continue;
            PbsData.Move move = pbs.moveById((int) id);
            if (move == null) {
                notes.add("unknown " + what + " id " + id);
            } else {
                into.add(move.internalName);
            }
        }
    }

    private static int[] sixInts(Object o) {
        int[] out = new int[6];
        List<Object> list = RubyMarshal.list(o);
        if (list != null) {
            for (int i = 0; i < 6 && i < list.size(); i++) out[i] = (int) RubyMarshal.num(list.get(i), 0);
        }
        return out;
    }

    /** {@code PBStatuses}: 0 none, 1 sleep, 2 poison, 3 burn, 4 paralysis, 5 frozen. */
    static String statusName(long id) {
        switch ((int) id) {
            case 1: return "SLEEP";
            case 2: return "POISON";
            case 3: return "BURN";
            case 4: return "PARALYSIS";
            case 5: return "FROZEN";
            default: return "";
        }
    }

    /** A Ruby {@code Time} (the 8-byte {@code _dump}) or an Integer of seconds, as epoch seconds; 0 when absent. */
    static long epochSeconds(Object o) {
        if (o instanceof Long) return (Long) o;
        if (!(o instanceof RUserData) || ((RUserData) o).data == null || ((RUserData) o).data.length < 8) return 0;
        byte[] d = ((RUserData) o).data;
        long p = (d[0] & 0xffL) | (d[1] & 0xffL) << 8 | (d[2] & 0xffL) << 16 | (d[3] & 0xffL) << 24;
        long s = (d[4] & 0xffL) | (d[5] & 0xffL) << 8 | (d[6] & 0xffL) << 16 | (d[7] & 0xffL) << 24;
        if ((p & (1L << 31)) == 0) return p;                                     // the old form: plain seconds
        int year = (int) (((p >> 14) & 0xffff) + 1900), mon = (int) ((p >> 10) & 0xf) + 1, day = (int) ((p >> 5) & 0x1f),
                hour = (int) (p & 0x1f), min = (int) ((s >> 26) & 0x3f), sec = (int) ((s >> 20) & 0x3f);
        try {
            return LocalDateTime.of(year, mon, day, hour, min, sec).toEpochSecond(ZoneOffset.UTC);
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
