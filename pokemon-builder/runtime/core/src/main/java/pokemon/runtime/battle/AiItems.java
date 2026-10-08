package pokemon.runtime.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pokemon.runtime.pokemon.ItemUse;

/**
 * The trainer AI's item use, transcribed from the plugin's {@code PokeBattle_AI#pbEnemyShouldUseItem?} and
 * {@code #pbEnemyItemToUse} (143_AI_Item.rb), and the opposing side of {@code pbAttackPhaseItems}
 * (139_Battle_Phase_Attack.rb:72-93 + 135_Battle_Action_UseItem.rb:83-115).
 *
 * <p>Why not CFRU's {@code ShouldAIUseItem}: it reads CFRU's item-effect table, which this project's items do not
 * have; the plugin's own item AI works on item names and is what {@code pbDefaultChooseEnemyCommand}
 * (142_PokeBattle_AI.rb:168-170) calls.</p>
 *
 * <p>登记: the handlers {@code ItemHandlers.triggerCanUseInBattle} / {@code BattleUseOnBattler}
 * (PItem_BattleItemEffects) are not in the plugin source available here. "Can use" is read as: HP items need missing HP,
 * status cures need that status, X items need the stat below +6; the effect itself is {@link ItemUse}'s amount for
 * healing, the status cured, or the stat/stages of the plugin's own {@code xItems} table. Their per-item messages
 * are not shown (only {@code pbUseItemMessage}).</p>
 */
final class AiItems {

    private static final String[][] ONE_STATUS = {
        {"SLEEP", "AWAKENING", "CHESTOBERRY", "BLUEFLUTE"},
        {"POISON", "ANTIDOTE", "PECHABERRY"},
        {"BURN", "BURNHEAL", "RAWSTBERRY"},
        {"PARALYSIS", "PARALYZEHEAL", "PARLYZHEAL", "CHERIBERRY"},
        {"FREEZE", "ICEHEAL", "ASPEARBERRY"},
    };
    private static final String[] ALL_STATUS = {
        "FULLHEAL", "LAVACOOKIE", "OLDGATEAU", "CASTELIACONE", "LUMIOSEGALETTE", "SHALOURSABLE", "BIGMALASADA", "LUMBERRY", "HEALPOWDER",
        "RAGECANDYBAR",                                            // NEWEST_BATTLE_MECHANICS
    };

    private AiItems() {
    }

    /** One {@code [item, priority, power]} candidate. */
    private static final class Candidate {
        final String item;
        final int a;
        final int b;

        Candidate(String item, int a, int b) {
            this.item = item;
            this.a = a;
            this.b = b;
        }
    }

    private static int hpItemPower(String id, Battler battler) {
        switch (id) {
            case "POTION": case "BERRYJUICE": return 20;
            case "SUPERPOTION": return 60;
            case "HYPERPOTION": return 120;
            case "MAXPOTION": return battler.maxHp();
            case "SWEETHEART": return 80;
            case "FRESHWATER": return 30;
            case "SODAPOP": return 50;
            case "LEMONADE": return 70;
            case "MOOMOOMILK": return 100;
            case "ORANBERRY": return 10;
            case "SITRUSBERRY": return battler.maxHp() / 4;
            case "ENERGYPOWDER": return 50;
            case "ENERGYROOT": return 200;
            default: return -1;
        }
    }

    /** {@code xItems}: stat and stages, or null. NEWEST_BATTLE_MECHANICS: the plain X item is +2. */
    private static int[] xItem(String id) {
        String base;
        int stages = 2;
        if (id.length() > 1 && Character.isDigit(id.charAt(id.length() - 1))) {
            stages = id.charAt(id.length() - 1) - '0';
            base = id.substring(0, id.length() - 1);
        } else {
            base = id;
        }
        if (stages != 2 && stages != 3 && stages != 6) return null;
        switch (base) {
            case "XATTACK": return new int[]{PBStats.ATTACK, stages};
            case "XDEFENSE": case "XDEFEND": return new int[]{PBStats.DEFENSE, stages};
            case "XSPATK": case "XSPECIAL": return new int[]{PBStats.SPATK, stages};
            case "XSPDEF": return new int[]{PBStats.SPDEF, stages};
            case "XSPEED": return new int[]{PBStats.SPEED, stages};
            case "XACCURACY": return new int[]{PBStats.ACCURACY, stages};
            default: return null;
        }
    }

    private static boolean oneStatusMatches(String id, Battler b) {
        for (String[] row : ONE_STATUS) {
            if (!b.hasStatus(row[0])) continue;
            for (int i = 1; i < row.length; i++) if (row[i].equals(id)) return true;
        }
        return false;
    }

    private static boolean isOneStatus(String id) {
        for (String[] row : ONE_STATUS) for (int i = 1; i < row.length; i++) if (row[i].equals(id)) return true;
        return false;
    }

    private static boolean contains(String[] list, String id) {
        for (String s : list) if (s.equals(id)) return true;
        return false;
    }

    private static boolean hasStatus(Battler b) {
        return b.status != null && !b.status.isEmpty() && !"NONE".equals(b.status);
    }

    /** {@code pbEnemyShouldUseItem?(idxBattler)} (:5-34): the item to use, or null. */
    static String choose(Battle battle, Battler user, Random rng) {
        if (user.hp >= user.maxHp() / 2) {                                                   // :8
            return null;
        } else if (user.hp >= user.maxHp() / 4) {                                            // :10
            if (rng.nextInt(100) < 70) return null;                                          // :11
        } else {
            if (rng.nextInt(100) < 30) return null;                                          // :13
        }
        int total = 0;                                                                       // :15
        for (int s = PBStats.ATTACK; s <= PBStats.EVASION; s++) total += user.stage(s);
        boolean batonPass = AiCalc.moveFunctionInMoveset(user, "0ED");                        // :16-21
        if (total > 0 && batonPass) return null;                                             // :22
        return itemToUse(battle, user, rng);
    }

    /** {@code pbEnemyItemToUse(idxBattler)} (:39-198). */
    private static String itemToUse(Battle battle, Battler user, Random rng) {
        if (!battle.internalBattle) return null;                                             // :40
        List<String> items = battle.foeItems;                                                // :41 pbGetOwnerItems
        if (items == null || items.isEmpty()) return null;                                   // :42
        int lostHp = user.maxHp() - user.hp;                                                 // :146
        boolean preferFullRestore = user.hp <= user.maxHp() * 2 / 3
                && (hasStatus(user) || user.effects.intVal(PBEffects.Battler.Confusion) > 0);   // :147
        List<Candidate> hp = new ArrayList<>();
        List<Candidate> status = new ArrayList<>();
        List<Candidate> x = new ArrayList<>();
        for (String i : items) {                                                             // :153
            if (i == null) continue;
            if (user.pokemon.egg || user.effects.intVal(PBEffects.Battler.Embargo) > 0) continue;   // :155 pbCanUseItemOnPokemon?
            if (lostHp > 0) {                                                                // :158
                int power = hpItemPower(i, user);
                if (power >= 0) {
                    hp.add(new Candidate(i, 5, power));
                    continue;
                }
            }
            if (lostHp > 0 || hasStatus(user)) {                                             // :172
                if (i.equals("FULLRESTORE")) {
                    hp.add(new Candidate(i, preferFullRestore ? 3 : 7, 999));
                    status.add(new Candidate(i, preferFullRestore ? 3 : 9, 0));
                    continue;
                }
            }
            if (hasStatus(user)) {                                                           // :182
                if (oneStatusMatches(i, user)) {
                    status.add(new Candidate(i, 5, 0));
                    continue;
                }
                if (isOneStatus(i)) continue;
                if (contains(ALL_STATUS, i)) {
                    status.add(new Candidate(i, 7, 0));
                    continue;
                }
            }
            int[] xi = xItem(i);                                                             // :199
            if (xi != null && user.stage(xi[0]) < 6) {
                x.add(new Candidate(i, user.stage(xi[0]), xi[1]));
            }
        }
        // Prioritise using a HP restoration item
        if (!hp.isEmpty() && (user.hp <= user.maxHp() / 4 || (user.hp <= user.maxHp() / 2 && rng.nextInt(100) < 30))) {   // :186
            hp.sort((p, q) -> p.a == q.a ? Integer.compare(p.b, q.b) : Integer.compare(p.a, q.a));
            Candidate prev = null;
            for (Candidate c : hp) {
                if (c.b >= lostHp) return c.item;                                            // :192
                prev = c;
            }
            return prev.item;                                                                // :195
        }
        // Next prioritise using a status-curing item
        if (!status.isEmpty() && rng.nextInt(100) < 40) {                                    // :198
            status.sort((p, q) -> Integer.compare(p.a, q.a));
            return status.get(0).item;
        }
        // Next try using an X item
        if (!x.isEmpty() && rng.nextInt(100) < 30) {                                         // :203
            x.sort((p, q) -> p.a == q.a ? Integer.compare(p.b, q.b) : Integer.compare(p.a, q.a));
            Candidate prev = null;
            for (Candidate c : x) {
                if (prev != null && c.a > prev.a) break;                                     // :207
                if (c.a + c.b >= 6) return c.item;                                           // :208
                prev = c;
            }
            return prev.item;                                                                // :211
        }
        return null;
    }

    /** {@code pbRegisterItem} (135:27-37) for the opposing trainer: the item leaves the trainer's list. */
    static boolean register(Battle battle, Battler user, String item) {
        Object[] c = battle.choices(user.index);
        c[0] = ":UseItem";                                                                   // :29
        c[1] = item;                                                                         // :30
        c[2] = battle.partyOf(user.index).indexOf(user, true);                               // :31 party index of the target
        c[3] = -1;                                                                           // :32
        battle.foeItems.remove(item);                                                        // :51-55 pbConsumeItemInBag
        return true;
    }

    /** One opposing {@code :UseItem} of {@code pbAttackPhaseItems} (139:74-90) + {@code pbUseItemOnPokemon} (135:83-97). */
    static void use(Battle battle, Battler user, String item) {
        user.lastMoveFailed = false;                                                         // 139:75
        pokemon.runtime.pokemon.PbsData.Item data = battle.pbs() == null ? null : battle.pbs().item(item);
        String name = data == null ? item : data.name;
        battle.displayBrief(battle.pbGetOwnerName(user.index) + "使用了" + name + "。");           // 135:77-80 pbUseItemMessage
        int amount = ItemUse.healValue(item, user.pokemon);
        int[] xi = xItem(item);
        if (xi != null) {
            user.pbRaiseStatStage(xi[0], xi[1], user);
        } else {
            if (amount > 0 && user.hp < user.maxHp()) user.pbRecoverHP(amount);
            if (hasStatus(user) && (item.equals("FULLRESTORE") || contains(ALL_STATUS, item) || oneStatusMatches(item, user))) {
                user.pbCureStatus();
            }
        }
        battle.choices(user.index)[1] = "";                                                  // 135:91 ch[1]=0
    }
}
