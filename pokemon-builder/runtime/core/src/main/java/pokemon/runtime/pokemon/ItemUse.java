package pokemon.runtime.pokemon;

import pokemon.runtime.state.Inventory;

/** Field item effects. Failed/cancelled operations never consume inventory. */
public final class ItemUse {
    public enum Result { USED, NO_EFFECT, REPLACE_MOVE, UNAVAILABLE }
    /** PItem_Items: the text the last call displayed (scene.pbDisplay). */
    public static String lastMessage = "";
    /** Panels / bag rows call this line: {@code _INTL("这没有任何效果。")}. */
    public static final String NO_EFFECT_TEXT = "这没有任何效果。";
    private ItemUse() { }

    public static PbsData.Move machineMove(PbsData.Item item, PbsData data) {
        if (item == null || data == null || (item.fieldUse != 3 && item.fieldUse != 4)) return null;
        for (String extra : item.extra) {
            PbsData.Move move = data.move(extra);
            if (move != null) return move;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // PItem_ItemEffects: ItemHandlers::UseOnPokemon
    // Every entry below is transcribed from the plugin's registration; the
    // amounts are the literals in its proc (no guessed values).
    // ------------------------------------------------------------------

    /**
     * {@code pbHPItem(pkmn, amount)}: the amount each healing item restores
     * (PItem_ItemEffects). {@code -1} means "totalhp - hp" (MAXPOTION) and
     * {@code -2} means "totalhp/4" (SITRUSBERRY).
     */
    private static int healAmount(String id) {
        switch (id) {
            case "POTION":                                   // pbHPItem(pkmn,20)
            case "BERRYJUICE":                               // copy(:POTION)
            case "RAGECANDYBAR":                             // copy(:POTION)
                return 20;
            case "SWEETHEART": return 80;                    // pbHPItem(pkmn,80)
            case "SUPERPOTION": return 60;                   // pbHPItem(pkmn,60)
            case "HYPERPOTION": return 120;                  // pbHPItem(pkmn,120)
            case "MAXPOTION": return -1;                     // pkmn.totalhp-pkmn.hp
            case "FRESHWATER": return 50;
            case "SODAPOP": return 60;
            case "LEMONADE": return 80;
            case "MOOMOOMILK": return 100;
            case "ORANBERRY": return 10;
            case "SITRUSBERRY": return -2;                   // pkmn.totalhp/4
            case "CIDER": return 50;
            case "MIXEDBEVERAGES": return 100;
            case "WWINE": return 150;
            case "FULLRESTORE": return -1;                   // totalhp-hp + healStatus
            case "ENERGYPOWDER": return 60;                  // pbHPItem(pkmn,60)
            case "ENERGYROOT": return 200;                   // pbHPItem(pkmn,200)
            default: return 0;
        }
    }

    public static int healValue(String id, Pokemon p) {
        int amount = healAmount(id);
        if (amount == -1) return Math.max(0, p.maxHp() - p.hp);
        if (amount == -2) return Math.max(1, p.maxHp() / 4);
        return amount;
    }

    /**
     * The status each curing item heals, from the plugin's
     * {@code pkmn.status!=PBStatuses::X} guards. {@code "*"} heals any status
     * (FULLHEAL / FULLRESTORE / HEALPOWDER / LUMBERRY).
     */
    private static String cureStatus(String id) {
        switch (id) {
            case "AWAKENING": case "CHESTOBERRY": return "SLEEP";
            case "ANTIDOTE": case "PECHABERRY": return "POISON";
            case "BURNHEAL": case "RAWSTBERRY": return "BURN";
            case "PARLYZHEAL": case "PARALYZEHEAL": case "CHERIBERRY": return "PARALYSIS";
            case "ICEHEAL": case "ASPEARBERRY": return "FROZEN";
            case "FULLHEAL": case "XIANGSHAWLPILL": case "HEALPOWDER":
            case "FULLRESTORE": case "LUMBERRY": return "*";
            default: return null;
        }
    }

    private static boolean cures(String id, String status) {
        String cure = cureStatus(id);
        if (cure == null) return false;
        if ("*".equals(cure)) return true;
        return cure.equalsIgnoreCase(status)
                || ("FROZEN".equals(cure) && "FREEZE".equalsIgnoreCase(status));
    }

    /** {@code pbItemRestoreHP}: always clamps to the maximum HP. */
    private static int restoreHp(Pokemon p, int amount) {
        int before = p.hp;
        p.hp = Math.min(p.maxHp(), p.hp + Math.max(0, amount));
        return p.hp - before;
    }

    public static Result use(String id, Pokemon p, TrainerState trainer, Inventory bag,
                             PbsData data, int replaceSlot, boolean day) {
        if (p == null || p.egg || data == null || !bag.has(id)) return Result.UNAVAILABLE;
        PbsData.Item item = data.item(id);
        if (item == null) return Result.UNAVAILABLE;
        PbsData.Move move = machineMove(item, data);
        if (move != null) {
            com.badlogic.gdx.utils.Array<String> compatible = data.tmCompatibility.get(move.internalName);
            if (p.species == null || compatible == null || !compatible.contains(p.species.internalName, false)) return Result.NO_EFFECT;
            for (Pokemon.MoveSlot known : p.moves)
                if (known.move != null && move.internalName.equals(known.move.internalName)) return Result.NO_EFFECT;
            if (p.moves.size >= 4) {
                if (replaceSlot < 0 || replaceSlot >= p.moves.size) return Result.REPLACE_MOVE;
                p.moves.set(replaceSlot, new Pokemon.MoveSlot(move));
            } else p.moves.add(new Pokemon.MoveSlot(move));
            // Source project's TM/HM are reusable.
            return Result.USED;
        }
        PbsData.Species evolved = evolution(p, id, data, day);
        if (evolved != null) {
            applyItemEvolution(p, evolved, id, data);
            trainer.registerOwned(p);
            bag.remove(id, 1);
            return Result.USED;
        }
        // REVIVE / MAXREVIVE: "if !pkmn.fainted?" -> no effect (PItem_ItemEffects).
        if ("REVIVE".equals(id) || "MAXREVIVE".equals(id)) {
            if (!p.fainted()) { lastMessage = NO_EFFECT_TEXT; return Result.NO_EFFECT; }
            p.hp = "REVIVE".equals(id) ? Math.max(1, p.maxHp() / 2) : p.maxHp();
            p.status = "";
            bag.remove(id, 1);
            // PItem_ItemEffects: scene.pbDisplay(_INTL("{1}的HP回复了。",pkmn.name))
            lastMessage = p.name + "的HP回复了。";
            return Result.USED;
        }
        int heal = healAmount(id);
        String cure = cureStatus(id);
        if (heal == 0 && cure == null) { lastMessage = NO_EFFECT_TEXT; return Result.UNAVAILABLE; }
        boolean changed = false;
        int hpGain = 0;
        // pbHPItem: a fainted Pokemon or one at full HP is "no effect".
        if (heal != 0 && !p.fainted() && p.hp < p.maxHp()) {
            hpGain = restoreHp(p, healValue(id, p));
            if (hpGain > 0) changed = true;
        }
        // pbStatusItem: "if pkmn.fainted? || pkmn.status==NONE" -> no effect.
        boolean cured = false;
        if (cure != null && !p.fainted() && p.status != null && !p.status.isEmpty()
                && cures(id, p.status)) {
            p.status = "";
            cured = true; changed = true;
        }
        if (!changed) { lastMessage = NO_EFFECT_TEXT; return Result.NO_EFFECT; }
        bag.remove(id, 1);
        // PItem_ItemEffects: the per-branch pbDisplay text.
        if (!cured) {
            lastMessage = p.name + "的HP恢复了" + hpGain + "点。";
        } else {
            lastMessage = statusMessage(id, cure, p.name);
        }
        return Result.USED;
    }

    /** The {@code pbDisplay} line each curing item prints (PItem_ItemEffects). */
    private static String statusMessage(String id, String cure, String name) {
        switch (id) {
            case "FULLHEAL": case "XIANGSHAWLPILL": case "HEALPOWDER": return name + "恢复健康了。";
            default: break;
        }
        if ("*".equals(cure)) return name + "恢复健康了。";
        switch (cure) {
            case "SLEEP": return name + "醒来了！";
            case "POISON": return name + "的毒被消去了。";
            case "BURN": return name + "的灼伤被治愈了！";
            case "PARALYSIS": return name + "的麻痹被解除了！";
            case "FROZEN": return name + "不再被冰冻了";
            default: return name + "恢复健康了。";
        }
    }
    public static PbsData.Species evolution(Pokemon p, String id, PbsData data, boolean day) {
        if (p == null || p.egg || p.species == null) return null;
        for (PbsData.Evolution e : p.species.evolutions) {
            if (!id.equals(e.parameter)) continue;
            boolean matches;
            switch (e.method) {
                case "Item": matches = true; break;
                case "ItemMale": matches = p.gender == PokemonStats.MALE; break;
                case "ItemFemale": matches = p.gender == PokemonStats.FEMALE; break;
                case "ItemDay": matches = day; break;
                case "ItemNight": matches = !day; break;
                case "ItemHappiness": matches = p.happiness >= 220; break;
                case "SpecialItem": matches = "MAGIKARP".equals(p.species.internalName) && "THUNDERSTONE".equals(id); break;
                case "PhantomItem": matches = "FRAXURE".equals(p.species.internalName) && "DUSKSTONE".equals(id); break;
                case "MiloticmItem": matches = "FEEBAS".equals(p.species.internalName) && "DRAGONSCALE".equals(id); break;
                case "TinkatonItem": matches = "TINKATUFF".equals(p.species.internalName) && "RAZORCLAW".equals(id); break;
                default: matches = false;
            }
            if (matches) return data.species(e.species);
        }
        return null;
    }
    /** The species change of an evolution stone, with the special forms the project's stones give. */
    public static void applyItemEvolution(Pokemon p, PbsData.Species evolved, String id, PbsData data) {
        int form = specialForm(p, id);
        PokemonGrowth.evolve(p, evolved, data);
        if (form > 0 && data.form(evolved.internalName, form) != null) {
            p.form = data.form(evolved.internalName, form);
            p.internalName = p.form.key;
            p.hp = Math.min(p.hp, p.maxHp());
        }
    }

    private static int specialForm(Pokemon p, String id) {
        String s = p.species.internalName;
        if (("MAGIKARP".equals(s) && "THUNDERSTONE".equals(id)) || ("FEEBAS".equals(s) && "DRAGONSCALE".equals(id))) return 2;
        if (("FRAXURE".equals(s) && "DUSKSTONE".equals(id)) || ("TINKATUFF".equals(s) && "RAZORCLAW".equals(id))) return 1;
        return 0;
    }
}
