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
 * <p>The item effects are transcribed from 190_PItem_BattleItemEffects.rb ({@code CanUseInBattle}, {@code BattleUseOnPokemon},
 * {@code BattleUseOnBattler}). 登记: its helpers {@code pbBattleHPItem}, {@code pbBattleItemCanCureStatus?} and
 * {@code pbBattleItemCanRaiseStat?} are defined in a section that is not exported; they are read in their standard Essentials
 * meaning (HP restored through {@code pbRecoverHP}, "status equals the cured status", "{@code pbCanRaiseStatStage?}").</p>
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
            if (!canUseInBattle(i, user)) continue;                                          // :156 ItemHandlers.triggerCanUseInBattle
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
            if (xi != null) {
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

    private static boolean isHpItem(String id) {
        return battleHeal(id, null) != 0 || id.equals("MAXPOTION");
    }

    /** The amount each {@code BattleUseOnPokemon} HP handler restores (190:262-314); MAXPOTION restores everything, SITRUSBERRY a quarter. */
    private static int battleHeal(String id, Battler b) {
        switch (id) {
            case "POTION": case "BERRYJUICE": return 20;
            case "SWEETHEART": return 80;
            case "SUPERPOTION": return 50;
            case "HYPERPOTION": return 200;
            case "MAXPOTION": return b == null ? 1 : b.maxHp() - b.hp;
            case "FRESHWATER": return 50;
            case "SODAPOP": return 60;
            case "LEMONADE": return 80;
            case "MOOMOOMILK": return 100;
            case "ORANBERRY": return 10;
            case "SITRUSBERRY": return b == null ? 1 : b.maxHp() / 4;
            case "CIDER": return 50;
            case "MIXEDBEVERAGES": return 100;
            case "WWINE": return 150;
            case "ENERGYPOWDER": return 50;
            case "ENERGYROOT": return 200;
            default: return 0;
        }
    }

    private static String cureStatusOf(String id) {
        for (String[] row : ONE_STATUS) for (int i = 1; i < row.length; i++) if (row[i].equals(id)) return row[0];
        return null;
    }

    private static boolean isFullHealType(String id) {
        return id.equals("FULLHEAL") || contains(ALL_STATUS, id);
    }

    /** {@code ItemHandlers.triggerCanUseInBattle} (190:1-236) for the items the AI knows. */
    static boolean canUseInBattle(String id, Battler b) {
        boolean able = !b.fainted();
        boolean confused = b.effects.intVal(PBEffects.Battler.Confusion) > 0;
        if (isHpItem(id)) return able && b.hp != b.maxHp();                                 // :70-81
        String cure = cureStatusOf(id);
        if (cure != null) {
            if (id.equals("BLUEFLUTE") && b.hasActiveAbility("SOUNDPROOF")) return false;   // :80-86
            return able && b.hasStatus(cure);                                                // pbBattleItemCanCureStatus?
        }
        if (isFullHealType(id)) return able && (hasStatus(b) || confused);                   // :113-126
        if (id.equals("FULLRESTORE")) return able && !(b.hp == b.maxHp() && !hasStatus(b) && !confused);   // :130-138
        int[] xi = xItem(id);
        if (xi != null) return b.pbCanRaiseStatStage(xi[0], b);                              // pbBattleItemCanRaiseStat?
        return false;
    }

    /** One opposing {@code :UseItem} of {@code pbAttackPhaseItems} (139:74-90) + {@code pbUseItemOnPokemon} (135:83-97). */
    static void use(Battle battle, Battler user, String item) {
        user.lastMoveFailed = false;                                                         // 139:75
        pokemon.runtime.pokemon.PbsData.Item data = battle.pbs() == null ? null : battle.pbs().item(item);
        String name = data == null ? item : data.name;
        battle.displayBrief(battle.pbGetOwnerName(user.index) + "使用了" + name + "。");           // 135:77-80 pbUseItemMessage
        if (!canUseInBattle(item, user)) {                                                   // 135:89 triggerCanUseInBattle with showMessages
            battle.display("这没有任何效果…");
            battle.foeItems.add(item);                                                       // 135:95 pbReturnUnusedItemToBag
            battle.choices(user.index)[1] = "";
            return;
        }
        int[] xi = xItem(item);
        String cure = cureStatusOf(item);
        if (xi != null) {                                                                    // 190:480-: BattleUseOnBattler X items
            user.pbRaiseStatStage(xi[0], xi[1], user);
        } else if (cure != null) {                                                           // 190:316-: single-status cures
            user.pbCureStatus(false);
            String[] msg = {"SLEEP", "{1}醒来了！", "POISON", "{1}的毒被消去了！", "BURN", "{1}的灼伤被治愈了！",
                    "PARALYSIS", "{1}的麻痹被解除了！", "FREEZE", "{1}不再被冰冻了"};
            for (int i = 0; i < msg.length; i += 2) if (msg[i].equals(cure)) battle.display(msg[i + 1].replace("{1}", user.pbThis()));
        } else if (isFullHealType(item)) {                                                   // 190:354-: Full Heal family
            user.pbCureStatus(false);
            user.pbCureConfusion();
            battle.display(user.pbThis() + "恢复健康了。");
        } else if (item.equals("FULLRESTORE")) {                                             // 190:376-
            user.pbCureStatus(false);
            user.pbCureConfusion();
            if (user.hp < user.maxHp()) user.pbRecoverHP(user.maxHp());                      // pbBattleHPItem(pokemon,battler,totalhp)
            else battle.display(user.pbThis() + "恢复健康了。");
        } else {
            user.pbRecoverHP(battleHeal(item, user));                                        // pbBattleHPItem
        }
        battle.choices(user.index)[1] = "";                                                  // 135:91 ch[1]=0
    }
}
