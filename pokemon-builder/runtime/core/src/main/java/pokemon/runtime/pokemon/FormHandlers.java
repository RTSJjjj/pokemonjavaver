package pokemon.runtime.pokemon;

/**
 * The {@code MultipleForms} hooks of 198_Pokemon_Forms (and 314_Mega_repair) that decide a form while the game runs:
 * {@code getForm} (re-evaluated every time a Pokemon's form is read outside battle, 198:5-11 - held-item, season,
 * time, gender and region forms), {@code onSetForm} (what changes with the form: moves, names, Hoopa's timer) and
 * {@code getFormOnEnteringBattle} (198:57-60). The creation and leaving-battle hooks are {@link FormsOnCreation} and
 * {@code battle.FormsOnLeavingBattle}; Zygarde's {@code getFormOnCapture} (:593-595) is never called by the plugin's
 * capture code (it returns the current form anyway), so it has no counterpart.
 *
 * <p>Ruby recomputes {@code getForm} lazily in the {@code form} getter; this runtime calls {@link #refresh} where the
 * player's Pokemon are looked at or change: every frame on the map for the party, when a held item is given or taken,
 * before a battle builds its battlers, and in the storage screens. Opposing trainers' Pokemon get their PBS form
 * pinned ({@code forcedForm}, 186_PTrainer_NPCTrainers:84-86) and wild ones are not read before the battle starts,
 * so only the player's Pokemon go through it.</p>
 */
public final class FormHandlers {

    private FormHandlers() {
    }

    /** What an {@code onSetForm} proc shows and asks: {@code pbMessage} and {@code pbLearnMove(pkmn,move,true)}. */
    public interface Sink {
        void message(String text);

        /** {@code pbLearnMove(pkmn,move,ignoreifknown=true)} (188_PItem_Items:779-821), which may ask to forget a move. */
        void learnMove(Pokemon pkmn, PbsData.Move move);
    }

    private static volatile PbsData defaultData;

    public static void setDefaultData(PbsData data) {
        defaultData = data;
    }

    public static PbsData defaultData() {
        return defaultData;
    }

    // ------------------------------------------------------------------
    // getForm
    // ------------------------------------------------------------------

    private static final java.util.Map<String, Integer> PLATES = new java.util.LinkedHashMap<>();
    private static final java.util.Map<String, Integer> MEMORIES = new java.util.LinkedHashMap<>();

    static {
        // 314_Mega_repair:12-33 (the table that wins over 198:396-417): form 9 (???) has no plate.
        String[] plates = {"FISTPLATE", "SKYPLATE", "TOXICPLATE", "EARTHPLATE", "STONEPLATE", "INSECTPLATE",
                "SPOOKYPLATE", "IRONPLATE", null, "FLAMEPLATE", "SPLASHPLATE", "MEADOWPLATE", "ZAPPLATE", "MINDPLATE",
                "ICICLEPLATE", "DRACOPLATE", "DREADPLATE", "PIXIEPLATE", "LIGHTPLATE", "SHADOWPLATE", "LEGENDPLATE"};
        for (int i = 0; i < plates.length; i++) if (plates[i] != null) PLATES.put(plates[i], i + 1);
        String[] memories = {"FIGHTINGMEMORY", "FLYINGMEMORY", "POISONMEMORY", "GROUNDMEMORY", "ROCKMEMORY",
                "BUGMEMORY", "GHOSTMEMORY", "STEELMEMORY", null, "FIREMEMORY", "WATERMEMORY", "GRASSMEMORY",
                "ELECTRICMEMORY", "PSYCHICMEMORY", "ICEMEMORY", "DRAGONMEMORY", "DARKMEMORY", "FAIRYMEMORY"};
        for (int i = 0; i < memories.length; i++) if (memories[i] != null) MEMORIES.put(memories[i], i + 1);
    }

    private static boolean holds(Pokemon p, String item) {
        return item.equals(p.item);
    }

    private static boolean hasItem(Pokemon p) {
        return p.item != null && !p.item.isEmpty();
    }

    private static boolean knows(Pokemon p, String move) {
        for (Pokemon.MoveSlot slot : p.moves) {
            if (slot != null && slot.move != null && move.equals(slot.move.internalName)) return true;
        }
        return false;
    }

    /** {@code pbGetSeason} (183_PField_Time:287-295): 0 spring (Mar-May), 1 summer, 2 autumn, 3 winter. */
    static int season(java.time.LocalDate date) {
        int month = date.getMonthValue();
        if (month >= 3 && month <= 5) return 0;
        if (month >= 6 && month <= 8) return 1;
        if (month >= 9 && month <= 11) return 2;
        return 3;
    }

    /** The region form of 198:256-261 / :1094-1118: region 1 (Tiall) gives form 1; a set form 2+ stays. */
    private static int regionForm(Pokemon p, FormsOnCreation.World world) {
        if (p.formIndex() >= 2) return -1;                                       // next if pkmn.formSimple>=2
        return world != null && world.region() == 1 ? 1 : 0;                    // mapPos[0]==1
    }

    /**
     * {@code MultipleForms.call("getForm",pkmn)}: the form the Pokemon should have now, or -1 for nil (keep it).
     *
     * @param now the clock ({@code pbGetTimeNow}): seasons, Hoopa's three days
     */
    public static int getForm(Pokemon p, FormsOnCreation.World world, java.time.LocalDateTime now) {
        if (p == null || p.species == null || p.egg) return -1;
        int current = p.formIndex();
        switch (p.species.internalName) {
            case "PIKACHU":                                                     // 198:258-262
            case "EXEGGCUTE": case "CUBONE":                                    // :1094-1103
            case "KOFFING": case "MIMEJR":                                       // :1111-1120
                return regionForm(p, world);
            case "GIRATINA":                                                    // :376-384 maps 157 / 165
                int map = world == null ? 0 : world.mapId();
                return holds(p, "GRISEOUSORB") || map == 157 || map == 165 ? 1 : 0;
            case "SHAYMIN":                                                     // :386-391
                if (p.hp <= 0 || "FROZEN".equals(p.status) || (world != null && world.night())) return 0;
                return -1;
            case "ARCEUS": {                                                    // 314_Mega_repair:9-43
                if (!"MULTITYPE".equals(p.ability)) return -1;
                if (!hasItem(p)) return 0;
                Integer f = PLATES.get(p.item);
                return f == null ? 0 : f;
            }
            case "DEERLING": case "SAWSBUCK":                                   // :447-453 pbGetSeason
                return season(now.toLocalDate());
            case "KELDEO":                                                      // :504-509
                return knows(p, "SECRETSWORD") ? 1 : 0;
            case "GENESECT":                                                    // :517-525
                if (holds(p, "SHOCKDRIVE")) return 1;
                if (holds(p, "BURNDRIVE")) return 2;
                if (holds(p, "CHILLDRIVE")) return 3;
                if (holds(p, "DOUSEDRIVE")) return 4;
                return 0;
            case "ESPURR": case "MEOWSTIC": case "INDEEDEE":                    // :550-559, :796-800 pkmn.gender
            case "LECHONK": case "OINKOLOGNE":                                  // :1182-1191
                return p.effectiveGender();
            case "HOOPA": {                                                     // :621-630 three days
                long nowSec = now.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                if (p.formTime == 0 || nowSec > p.formTime + 60L * 60 * 24 * 3) return 0;
                return -1;
            }
            case "ROCKRUFF":                                                    // :638-643
                if (current >= 2) return -1;                                    // Own Tempo Rockruff
                return world != null && world.night() ? 1 : 0;
            case "DIALGA": return holds(p, "ADAMANTORB") ? 1 : 0;               // :660-665
            case "PALKIA": return holds(p, "LUSTROUSORB") ? 1 : 0;              // :667-672
            case "REGIGIGAS": return holds(p, "REGISPELL") ? 1 : 0;             // :674-679
            case "SILVALLY": {                                                  // :687-720
                if (!"RKSSYSTEM".equals(p.ability)) return -1;
                Integer f = p.item == null ? null : MEMORIES.get(p.item);
                return f == null ? 0 : f;
            }
            case "ZAMAZENTA": return holds(p, "RUSTEDSHIELD") ? 1 : 0;          // :802-807
            case "ZACIAN": return holds(p, "RUSTEDSWORD") ? 1 : 0;              // :809-814
            case "CHALLEN":                                                     // :817-831
                if (current == 3) return 3;
                if (holds(p, "CHARLENEITE")) return 1;
                if (holds(p, "ZEROSUMHEART")) return 2;
                return 0;
            case "VALKYRIE": return holds(p, "BRIGHTSTONE") ? 1 : 0;            // :846-851
            case "BLACKKNIGHT": return holds(p, "DAWNISTONE") ? 1 : 0;          // :853-858
            case "MEWTWO":                                                      // :873-881
                if (holds(p, "BINDARMOR")) return 1;
                if (holds(p, "RARMOR")) return 2;
                return 0;
            case "SUGARDEVOIR":                                                 // :930-949
                if (current == 6) return 6;
                if (holds(p, "RADIANTSHARD")) return 5;
                if (holds(p, "SANCTFEATHER")) return 4;
                if (holds(p, "SQHL")) return 3;
                if (holds(p, "WMDCX")) return 2;
                if (holds(p, "TEMPLESCEPTER")) return 1;
                return 0;
            case "SUJINRAKU":                                                   // :1032-1042
                if (current == 2) return 2;
                return holds(p, "ABYSSSWORD") ? 1 : 0;
            case "SIRFETCHD": return holds(p, "HOLYCREST") ? 2 : 0;             // :1058-1063
            case "OGERPON":                                                     // :1248-1258
                if (holds(p, "WELLSPRINGMASK")) return 1;
                if (holds(p, "HEARTHFLAMEMASK")) return 2;
                if (holds(p, "CORNERSTONEMASK")) return 3;
                return 0;
            default:
                return -1;
        }
    }

    /**
     * The {@code form} getter outside battle (198:5-11): {@code v = getForm; self.form = v if v!=nil && v!=@form}.
     *
     * @return true when the form changed
     */
    public static boolean refresh(Pokemon p, PbsData data, FormsOnCreation.World world, Sink sink) {
        if (p == null || data == null) return false;
        int v = getForm(p, world, java.time.LocalDateTime.now());
        if (v < 0 || v == p.formIndex()) return false;
        setForm(p, data, v, sink);
        return true;
    }

    /** {@link #refresh} for every Pokemon of a party with the default data and world. */
    public static void refreshAll(Iterable<Pokemon> pokemon, Sink sink) {
        PbsData data = defaultData;
        if (data == null || pokemon == null) return;
        FormsOnCreation.World world = FormsOnCreation.defaultWorld();
        for (Pokemon p : pokemon) refresh(p, data, world, sink);
    }

    /** {@link #refresh} for every Pokemon of a party, silently, with the default world. */
    public static void refresh(Iterable<Pokemon> pokemon, PbsData data) {
        if (data == null || pokemon == null) return;
        FormsOnCreation.World world = FormsOnCreation.defaultWorld();
        for (Pokemon p : pokemon) refresh(p, data, world, null);
    }

    /** {@link #refresh} with the default data and world. */
    public static boolean refresh(Pokemon p, Sink sink) {
        return refresh(p, defaultData, FormsOnCreation.defaultWorld(), sink);
    }

    /**
     * {@code setForm(value)} (198:17-24): the form, then {@code onSetForm}, then {@code calcStats} (which keeps the HP
     * lost).
     */
    public static void setForm(Pokemon p, PbsData data, int form, Sink sink) {
        int oldForm = p.formIndex();
        p.recalculatingStats(() -> p.setForm(data, form));
        onSetForm(p, data, form, oldForm, sink);
    }

    // ------------------------------------------------------------------
    // onSetForm
    // ------------------------------------------------------------------

    private static void say(Sink sink, String text) {
        if (sink != null) sink.message(text);
    }

    /** {@code pbLearnMove(pkmn,move,true)}; without a scene the move is learned only when there is room. */
    private static void learn(Pokemon p, PbsData data, String move, Sink sink) {
        PbsData.Move m = data.move(move);
        if (m == null || knows(p, move)) return;
        if (sink != null) {
            sink.learnMove(p, m);
        } else if (p.moves.size < 4) {
            p.moves.add(new Pokemon.MoveSlot(m));
        }
    }

    /** {@code move.id = X} (085_PBMove:64-68): the slot keeps its PP, capped at the new move's total. */
    private static void replace(Pokemon.MoveSlot slot, PbsData data, String move) {
        PbsData.Move m = data.move(move);
        if (m == null) return;                                                  // hasConst?(PBMoves,X)
        slot.move = m;
        slot.maxPp = slot.totalPp();
        slot.pp = Math.min(slot.pp, slot.maxPp);
    }

    private static void replaceAll(Pokemon p, PbsData data, String to, String... from) {
        for (Pokemon.MoveSlot slot : p.moves) {
            if (slot == null || slot.move == null) continue;
            for (String f : from) {
                if (f.equals(slot.move.internalName)) {
                    replace(slot, data, to);
                    break;
                }
            }
        }
    }

    private static int indexOfAny(Pokemon p, String... moves) {
        for (int i = 0; i < p.moves.size; i++) {
            Pokemon.MoveSlot slot = p.moves.get(i);
            if (slot == null || slot.move == null) continue;
            for (String m : moves) if (m.equals(slot.move.internalName)) return i;
        }
        return -1;
    }

    private static String moveName(Pokemon p, int index) {
        PbsData.Move m = p.moves.get(index).move;
        return m.name == null ? m.internalName : m.name;
    }

    /** {@code pkmn.pbDeleteMoveAtIndex(i)} + "{1} forgot {2}..." + {@code pbLearnMove(fallback) if numMoves==0}. */
    private static void forget(Pokemon p, PbsData data, int index, String fallback, Sink sink) {
        String name = moveName(p, index);
        p.moves.removeIndex(index);
        say(sink, p.name + "忘记了" + name + "……");
        if (p.moves.size == 0) p.learnMoveSilently(data.move(fallback));
    }

    /**
     * The swap of Rotom (198:328-374), Pikachu (:212-256) and Necrozma (:745-774): one special move per form, forgotten
     * on going back to the plain form, replaced or learned on taking a special one.
     */
    private static void swapFormMove(Pokemon p, PbsData data, String[] formMoves, int newMoveIndex, String fallback,
                                     boolean learnOnly, Sink sink) {
        int idx = indexOfAny(p, formMoves);
        if (newMoveIndex < 0) {
            if (idx >= 0) forget(p, data, idx, fallback, sink);
            return;
        }
        String newMove = formMoves[newMoveIndex];
        PbsData.Move m = data.move(newMove);
        if (learnOnly) {
            if (m != null) learn(p, data, newMove, sink);
            return;
        }
        if (idx >= 0) {
            String oldName = moveName(p, idx);
            if (m != null) {
                replace(p.moves.get(idx), data, newMove);
                say(sink, "1，2，和……然后……然后……当当！");
                say(sink, p.name + "忘记了" + oldName + "的使用方法。\n然后……");
                say(sink, p.name + "学会了" + (m.name == null ? newMove : m.name) + "！");
            } else {
                forget(p, data, idx, fallback, sink);
            }
        } else if (m != null) {
            learn(p, data, newMove, sink);
        }
    }

    /** {@code MultipleForms.call("onSetForm",pkmn,form,oldForm)}. */
    public static void onSetForm(Pokemon p, PbsData data, int form, int oldForm, Sink sink) {
        if (p == null || p.species == null || data == null) return;
        switch (p.species.internalName) {
            case "PIKACHU": {                                                   // :212-256
                // 插件缺陷已修: the plugin tests `[3..8].include?(form)` (an array holding one Range), which is never
                // true, so every form change - the region swap 0 <-> 1 included - made Pikachu forget Meteor Mash,
                // Electric Terrain, Draining Kiss ... The cosplay forms here are 3-8 (3 the plain Cosplay Pikachu),
                // so the swap runs only when a cosplay form is involved, and form 3 learns no special move.
                boolean cosplay = form >= 3 && form <= 8;
                if (!cosplay && !(oldForm >= 3 && oldForm <= 8)) return;
                if (cosplay) p.gender = PokemonStats.FEMALE;                    // :214 makeFemale
                String[] moves = {"ICICLECRASH", "FLYINGPRESS", "ELECTRICTERRAIN", "DRAININGKISS", "METEORMASH"};
                swapFormMove(p, data, moves, cosplay && form >= 4 ? form - 4 : -1, "THUNDERSHOCK", false, sink);
                return;
            }
            case "ROTOM": {                                                     // :328-374
                String[] moves = {"OVERHEAT", "HYDROPUMP", "BLIZZARD", "AIRSLASH", "LEAFSTORM"};
                swapFormMove(p, data, moves, form >= 1 && form <= 5 ? form - 1 : -1, "THUNDERSHOCK", false, sink);
                return;
            }
            case "KYUREM":                                                      // :462-501
                if (form == 0) {
                    replaceAll(p, data, "GLACIATE", "ICEBURN", "FREEZESHOCK");
                    replaceAll(p, data, "SCARYFACE", "FUSIONFLARE", "FUSIONBOLT");
                } else if (form == 1) {
                    replaceAll(p, data, "ICEBURN", "GLACIATE");
                    replaceAll(p, data, "FUSIONFLARE", "SCARYFACE");
                } else if (form == 2) {
                    replaceAll(p, data, "FREEZESHOCK", "GLACIATE");
                    replaceAll(p, data, "FUSIONBOLT", "SCARYFACE");
                }
                return;
            case "ZYGARDE":                                                     // :606-618
                if (oldForm <= 5 && form <= 5 && oldForm == 5 && form != 5) {
                    replaceAll(p, data, "COREENFORCER", "NIHILLIGHT");
                }
                return;
            case "HOOPA":                                                       // :627-629
                p.formTime = form > 0 ? java.time.Instant.now().getEpochSecond() : 0;
                return;
            case "NECROZMA": {                                                  // :745-774
                if (form > 2 || oldForm > 2) return;                            // Ultra form changes keep the moves
                String[] moves = {"SUNSTEELSTRIKE", "MOONGEISTBEAM"};
                swapFormMove(p, data, moves, form == 0 ? -1 : form - 1, "CONFUSION", true, sink);
                return;
            }
            case "MEWTWO": {                                                    // :882-920
                if (form == 0) replaceAll(p, data, "PSYSTRIKE", "ARMORRELEASE", "PSYCHOBLAST");
                else if (form == 1) replaceAll(p, data, "ARMORRELEASE", "PSYSTRIKE", "PSYCHOBLAST");
                else if (form == 2) replaceAll(p, data, "PSYCHOBLAST", "PSYSTRIKE", "ARMORRELEASE");
                PbsData.SpeciesForm f = form > 0 ? data.form(p.species.internalName, form) : null;
                p.name = f != null && f.formName != null && !f.formName.isEmpty() ? f.formName : p.species.name;   // :913-919
                return;
            }
            case "SUGARDEVOIR": {                                               // :950-1029
                if (form == oldForm) return;
                String[] signature = {"SUTACKLE", "TEMPLESHINE", "EVOBEAM", "TIDEARIA", "RADIANTVERDICT", "TRUTHCRASH"};
                String[] names = {"素纱奈", "圣殿女王", "文明续约", "潮心公主", "圣洁之翼", "真理之核", "厉鬼之凄厉"};
                if (form >= 0 && form <= 5) {
                    java.util.List<String> others = new java.util.ArrayList<>(java.util.Arrays.asList(signature));
                    others.remove(signature[form]);
                    replaceAll(p, data, signature[form], others.toArray(new String[0]));
                }
                if (form >= 0 && form < names.length) p.name = names[form];
                return;
            }
            case "SUJINRAKU":                                                   // :1043-1055
                if (form == oldForm) return;
                p.name = form == 1 ? "暗渊守卫" : form == 2 ? "冥焰之战骑" : "素艾路";
                return;
            case "CALYREX":                                                     // :1065-1086
                if (form == 0) {
                    String[] exclusive = {"TACKLE", "TAILWHIP", "DOUBLEKICK", "AVALANCHE", "HEX", "STOMP", "TORMENT",
                            "CONFUSERAY", "MIST", "HAZE", "ICICLECRASH", "SHADOWBALL", "TAKEDOWN", "IRONDEFENSE",
                            "AGILITY", "THRASH", "TAUNT", "DISABLE", "DOUBLEEDGE", "SWORDSDANCE", "NASTYPLOT",
                            "GLACIALLANCE", "ASTRALBARRAGE"};
                    // :1072-1078 deletes while iterating, so the move after a deleted one is skipped; walking
                    // backwards removes every exclusive move as intended.
                    for (int i = p.moves.size - 1; i >= 0; i--) {
                        Pokemon.MoveSlot slot = p.moves.get(i);
                        if (slot == null || slot.move == null) continue;
                        if (java.util.Arrays.asList(exclusive).contains(slot.move.internalName)) {
                            say(sink, p.name + "忘记了" + moveName(p, i) + "……");
                            p.moves.removeIndex(i);
                        }
                    }
                    if (p.moves.size == 0) p.learnMoveSilently(data.move("CONFUSION"));
                } else if (form == 1) {
                    learn(p, data, "GLACIALLANCE", sink);
                } else if (form == 2) {
                    learn(p, data, "ASTRALBARRAGE", sink);
                }
                return;
            case "RAPIDASH":                                                    // :1124-1134
                if (form == 0) replaceAll(p, data, "FLAREBLITZ", "FLAMEEXPLOSION");
                return;
            default:
        }
    }

    // ------------------------------------------------------------------
    // battle hooks
    // ------------------------------------------------------------------

    /** {@code getFormOnEnteringBattle(pkmn,wild)} (198:455-458, :582-585, :726-728, :1259-1264), or -1. */
    public static int formOnEnteringBattle(Pokemon p, boolean wild) {
        if (p == null || p.species == null) return -1;
        int form = p.formIndex();
        switch (p.species.internalName) {
            case "KYUREM": return form == 1 || form == 2 ? form + 2 : -1;
            case "XERNEAS": return 1;
            case "MINIOR": return form >= 7 && wild ? form - 7 : -1;            // wild Minior appear in Meteor form
            case "OGERPON": return form < 4 ? form + 4 : form;
            default: return -1;
        }
    }

    /**
     * {@code pbOnEnteringBattle} (198:57-60): {@code pkmn.form = f if f}. Not routed through {@link #onSetForm}'s move
     * swaps for these species (none of them registers moves for the entering forms).
     *
     * @return true when the form changed
     */
    public static boolean onEnteringBattle(Pokemon p, PbsData data, boolean wild) {
        int f = formOnEnteringBattle(p, wild);
        if (f < 0 || data == null || f == p.formIndex()) return false;
        setForm(p, data, f, null);
        return true;
    }
}
