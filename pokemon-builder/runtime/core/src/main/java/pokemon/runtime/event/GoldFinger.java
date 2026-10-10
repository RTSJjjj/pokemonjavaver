package pokemon.runtime.event;

import pokemon.runtime.field.ItemHandlers;
import pokemon.runtime.pokemon.PBEvolution;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.PokemonStats;
import pokemon.runtime.pokemon.Storage;
import pokemon.runtime.state.GameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

/**
 * 371_goldFinger: the cheat menu of {@code Scene_Map} (049_Scene_Map:200-201, the F8 key). The Ruby is blocking script text;
 * it runs on a {@link pokemon.runtime.field.BlockingTask} whose requests (messages, choices, number windows, the party
 * screen) the interpreter answers, the same way {@link EggMoveTutor} does.
 *
 * <p>Outside the cheat switches (42 and 197; {@code $DEBUG} is never set in a built game) the menu is skipped and
 * {@code select = 1} runs: the confirmation to refresh the habitat list.</p>
 *
 * <p>登记: {@code pbGetLegalMoves(species)} is not in the plugin source (it is Essentials' own debug helper); its result
 * is modelled as the level-up moves, the machine moves and the egg moves of the baby species. {@code gfAddItem}'s
 * evolution stones that the project's items do not define are skipped (Ruby would raise on the missing constant).</p>
 */
final class GoldFinger {
    /** 000_Settings:96 {@code MAX_MONEY}. */
    private static final int MAX_MONEY = 999_999_999;
    /** 000_Settings:28 {@code EXP_POT_MAX}. */
    private static final int EXP_POT_MAX = 99999999;
    private static final String[] STAT_NAMES = {"血量", "攻击", "防御", "速度", "特攻", "特防"};   // 088_PBStats:15-44
    private static final int[] MAX_LEVELS = {16, 25, 32, 36, 42, 48, 58, 63, 85};            // :156
    private static final int EV_LIMIT = 510;                                                   // PokeBattle_Pokemon::EV_LIMIT
    private static final int EV_STAT_LIMIT = 252;                                              // PokeBattle_Pokemon::EV_STAT_LIMIT

    private final GameState state;
    private final PbsData pbs;
    private final TaskFieldScene scene;
    private final ItemHandlers handlers;
    private final int dexCount;

    /**
     * @param dexCount {@code pbDexNames.length}: the regional Dex lists plus the National Dex
     */
    GoldFinger(GameState state, PbsData pbs, TaskFieldScene scene, ItemHandlers handlers, int dexCount) {
        this.state = state;
        this.pbs = pbs;
        this.scene = scene;
        this.handlers = handlers;
        this.dexCount = dexCount;
    }

    /** {@code def goldFinger} (:1-54): the menu, which comes back after each entry (:53 {@code goldFinger}). */
    void goldFinger() {
        while (true) {
            List<String> cheatList = new ArrayList<>(Arrays.asList(
                    "治疗全队", "刷新导航", "添加道具", "全开图鉴", "修改精灵", "金钱最大"));   // :2-4
            cheatList.add("经验储罐");                                                    // :5
            int select;
            if (state.switches().get(42) || state.switches().get(197)) {                // :6 $DEBUG || switches
                select = scene.pbMessage("请选择需要进行的操作。", cheatList, -1);            // :7
            } else {
                select = 1;                                                              // :9
            }
            switch (select) {
                case -1:                                                                 // :12-13
                    return;
                case 0:                                                                  // :14-16
                    for (Pokemon pkmn : state.trainer().party.members()) {
                        Storage.heal(pkmn);                                              // :15 pbHealAll
                    }
                    scene.pbMessage("已治疗全队精灵。");
                    break;
                case 1:                                                                  // :17-22
                    if (scene.pbConfirmMessage("确定要刷新野生宝可梦分布的导航数据吗？")) {
                        state.trainer().habitats.setup(pbs);                             // :19 Habitats.setup
                        scene.pbMessage("已刷新分布导航，可能需要重新进入地图。");
                    }
                    return;
                case 2:                                                                  // :23-24
                    gfAddItem();
                    break;
                case 3:                                                                  // :25-39
                    unlockPokedex();
                    break;
                case 4:                                                                  // :40-41
                    alterPokemon();
                    break;
                case 5:                                                                  // :42-44
                    state.trainer().money = MAX_MONEY;
                    scene.pbMessage("金钱已设为最大。");
                    break;
                case 6: {                                                                // :45-51
                    int pot = Math.max(0, state.trainer().expPot);                       // :46 exp_pot = 0 if !exp_pot
                    state.trainer().expPot = scene.pbMessageChooseNumber(
                            "设置经验储罐剩余量。", 0, EXP_POT_MAX, pot, 0);                  // :47-51
                    break;
                }
                default:
                    break;
            }
        }
    }

    /** :26-38 the "全开图鉴" entry. */
    private void unlockPokedex() {
        if (!scene.pbConfirmMessage("确定要获得全部图鉴并填充信息吗？\n(这不会获得宝可梦)")) {      // :26
            return;
        }
        java.util.List<Boolean> unlocked = state.fieldGlobals().pokedexUnlocked;
        // :27 pbLockDex: locks the National Dex (the last list), :28-32 then every list but that one is unlocked
        while (unlocked.size() < dexCount) {
            unlocked.add(unlocked.isEmpty());                                            // 173:92-99 only the first starts unlocked
        }
        unlocked.set(unlocked.size() - 1, false);                                        // :27 pbLockDex
        int dexLength = dexCount - 1;                                                    // :29 $DEBUG ? d.length : d.length - 1
        for (int i = 0; i < dexLength; i++) {                                            // :30-32
            unlocked.set(i, true);
        }
        List<String> everySpecies = new ArrayList<>();
        for (String name : pbs.speciesById.values()) {
            everySpecies.add(name);
        }
        for (String species : everySpecies) {                                            // :33-37 sp in 1..maxValue
            state.trainer().setSeen(species);                                            // :34
            state.trainer().setOwned(species);                                           // :35
            state.trainer().habitats.updateForSpecies(species);                          // :36 Habitats.updateHabitatsForSpecies
        }
        scene.pbMessage("获得全部图鉴并填充信息成功。");                                       // :38
    }

    /** {@code def gfAddItem} (:67-114). */
    private void gfAddItem() {
        List<String> nameList = Arrays.asList(
                "大师球", "神奇糖果", "全复药", "活力块", "PP极限提升剂",
                "进化道具", "无限喷雾");                                                   // :68-71
        int i = scene.pbMessage("请选择需要添加的道具。", nameList, -1);                     // :72
        String[] items = {"MASTERBALL", "RARECANDY", "FULLRESTORE", "MAXREVIVE", "PPMAX",
                null, "INFINITEREPEL"};                                                  // :73-76
        if (i <= -1) {
            return;                                                                      // :77
        }
        String item = items[i];
        if (item != null) {                                                              // :79
            PbsData.Item data = pbs.item(item);
            String itemName = data == null ? item : data.name;                           // :80 PBItems.getName
            int qty = 1;                                                                 // :85
            if (i <= 4) {
                qty = scene.pbMessageChooseNumber(
                        "请选择要添加的" + itemName + "的\n数量(1~99)", 1, 99, 1, 0);        // :86-87 setRange(1,99), cancel 0
            }
            if (qty == 0) {
                return;                                                                  // :88
            }
            if (canStore(item, qty)) {                                                   // :89
                state.inventory().add(item, qty);                                        // :90 pbStoreItem
                scene.pbMessage("已向背包添加" + qty + "个" + itemName + "。");              // :91
            } else {
                scene.pbMessage("背包已经满了。");                                          // :93
            }
        } else {
            String[] evoStone = {
                    "FIRESTONE", "THUNDERSTONE", "WATERSTONE", "LEAFSTONE",
                    "MOONSTONE", "SUNSTONE", "DUSKSTONE", "DAWNSTONE",
                    "SHINYSTONE", "ICESTONE", "PRISMSCALE", "OVALSTONE",
                    "BLACKAUGURITE", "PEATBLOCK", "HISUISTONE"};                         // :97-101
            for (String stone : evoStone) {                                              // :102
                if (pbs.item(stone) == null) {
                    continue;                                                            // 登记: not defined by the project's items
                }
                if (canStore(stone, 6)) {                                                // :103
                    state.inventory().add(stone, 6);                                     // :104
                } else {
                    scene.pbMessage("背包已经满了，无法继续添加" + pbs.item(stone).name + "。"); // :106-107
                    break;                                                               // :108
                }
            }
            scene.pbMessage("已向背包添加进化道具各6个。");                                    // :111
        }
    }

    /** {@code $PokemonBag.pbCanStore?}: the pockets are unbounded (195_PItem_Bag:90-, BAG_MAX_POCKET_SIZE = -1). */
    private boolean canStore(String item, int qty) {
        return qty > 0;
    }

    /** {@code def alterPokemon} (:116-131). */
    private void alterPokemon() {
        if (state.trainer().party.size() == 0) {                                         // :117
            scene.pbMessage("请领取御三家后使用。");                                          // :118
            return;
        }
        int pkmnid = scene.pbChoosePokemon();                                            // :121 pbChoosePokemon(1,3)
        state.variables().set(1, pkmnid);
        state.variables().setText(3, pkmnid >= 0 && state.trainer().party.get(pkmnid) != null
                ? state.trainer().party.get(pkmnid).name : "");
        if (pkmnid >= 0 && pkmnid <= 5) {                                                // :123
            Pokemon pkmn = state.trainer().party.get(pkmnid);                            // :124
            if (pkmn == null) {
                return;
            }
            if (pkmn.egg) {                                                              // :125
                alterPokemonEgg(pkmn);                                                   // :126
            } else {
                alterPokemonOption(pkmn);                                                // :128
            }
        }
    }

    /** {@code def alterPokemonEgg} (:133-141). */
    private void alterPokemonEgg(Pokemon pkmn) {
        if (scene.pbConfirmMessage("\\l[1]要快速孵化这只精灵蛋吗？")) {                        // :134
            pkmn.name = pkmn.species == null ? pkmn.name : pkmn.species.name;            // :135 PBSpecies.getName
            pkmn.stepsToHatch = 0;                                                       // :136 eggsteps = 0
            pkmn.egg = false;                                                            // egg? is eggsteps > 0
            pkmn.hatchedMap = 0;                                                         // :137
            pkmn.obtainMode = 0;                                                         // :138
            scene.pbMessage("\\l[1]" + pkmn.name + "从蛋中孵化了出来。");                      // :139
        }
    }

    /** {@code def alterPokemonOption} (:143-238): it calls itself again after each change (:237). */
    private void alterPokemonOption(Pokemon pkmn) {
        List<String> optionList = Arrays.asList(
                "治愈精灵", "修改等级", "修改性格", "修改特性", "修改异色",
                "教学招式", "修改亲密", "修改个体", "修改努力");                              // :144-147
        while (true) {
            int option = scene.pbMessage("\\l[1]请选择要进行的修改。", optionList, -1);        // :148
            switch (option) {
                case -1:                                                                 // :150-151
                    return;
                case 0:                                                                  // :152-154
                    Storage.heal(pkmn);                                                  // :153 pkmn.heal
                    scene.pbMessage("\\l[1]" + pkmn.name + "恢复了健康。");                  // :154
                    break;
                case 1:                                                                  // :155-167
                    alterLevel(pkmn);
                    break;
                case 2:                                                                  // :168-187
                    alterNature(pkmn);
                    break;
                case 3:                                                                  // :188-199
                    alterAbility(pkmn);
                    break;
                case 4:                                                                  // :200-215
                    if (!alterShiny(pkmn)) {
                        return;                                                          // :203 return if shiny == -1
                    }
                    break;
                case 5:                                                                  // :216-222
                    teachMove(pkmn);
                    break;
                case 6: {                                                                // :223-231
                    int happiness = scene.pbMessageChooseNumber(
                            "\\l[1]修改精灵亲密度(0~255)。", 0, 255, pkmn.happiness, pkmn.happiness);   // :224-227 setDefaultValue
                    if (happiness != pkmn.happiness) {                                   // :228
                        pkmn.happiness = happiness;                                      // :229
                        scene.pbMessage("\\l[1]" + pkmn.name + "的亲密度已被修改为" + pkmn.happiness + "。");   // :230
                    }
                    break;
                }
                case 7:                                                                  // :232-233
                    alterPokemonIV(pkmn);
                    break;
                case 8:                                                                  // :234-235
                    alterPokemonEV(pkmn);
                    break;
                default:
                    break;
            }
        }
    }

    /** :155-167 "修改等级". */
    private void alterLevel(Pokemon pkmn) {
        int i = Math.min(state.trainer().badges.size(), 8);                              // :157 [$Trainer.numbadges, 8].min
        int max = state.switches().get(12) ? 100 : MAX_LEVELS[i];                        // :158
        int level = scene.pbMessageChooseNumber(
                "\\l[1]修改精灵等级(最大" + max + ")。", 1, max, pkmn.level, pkmn.level);   // :159-162 setRange(1,max), default
        if (level != pkmn.level) {                                                       // :163
            final int chosen = level;
            pkmn.recalculatingStats(() -> pkmn.setLevelAndExp(chosen));                  // :164-165 level=, calcStats
            scene.pbMessage("\\l[1]" + pkmn.name + "已被修改为" + pkmn.level + "级。");        // :166
        }
    }

    /** :168-187 "修改性格". */
    private void alterNature(Pokemon pkmn) {
        List<String> natureList = new ArrayList<>();
        for (PbsData.Nature nature : pbs.natures) {                                      // :170 PBNatures.getCount.times
            int up = PokemonStats.statIndexOf(nature.statUp);                            // :171 getStatRaised
            int down = PokemonStats.statIndexOf(nature.statDown);                        // :172 getStatLowered
            if (up != down) {                                                            // :173
                natureList.add(nature.name + " (+" + statName(up) + ", -" + statName(down) + ")");   // :174-175
            } else {
                natureList.add(nature.name + " (---)");                                  // :177
            }
        }
        int newNature = scene.pbMessage("\\l[1]请选择要修改的性格。", natureList, -1);        // :182
        if (newNature >= 0 && newNature < natureList.size()) {                           // :183
            final PbsData.Nature chosen = pbs.natures.get(newNature);
            pkmn.recalculatingStats(() -> pkmn.nature = chosen);                         // :184 setNature (= calcStats)
            scene.pbMessage("\\l[1]" + pkmn.name + "的性格已被修改为" + chosen.name + "。");    // :186
        }
    }

    private static String statName(int index) {
        return index >= 0 && index < STAT_NAMES.length ? STAT_NAMES[index] : "";
    }

    /** :188-199 "修改特性"; {@code getAbilityList} (197_PokeBattle_Pokemon:265-280). */
    private void alterAbility(Pokemon pkmn) {
        List<String> names = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        com.badlogic.gdx.utils.Array<String> natural = pkmn.form != null && pkmn.form.abilities != null
                ? pkmn.form.abilities : pkmn.species == null ? null : pkmn.species.abilities;
        String hidden = pkmn.form != null && pkmn.form.hiddenAbility != null ? pkmn.form.hiddenAbility
                : pkmn.species == null ? null : pkmn.species.hiddenAbility;
        if (natural != null) {
            for (int i = 0; i < natural.size; i++) {                                     // :269
                String ability = natural.get(i);
                if (ability != null && !ability.isEmpty()) {
                    names.add(abilityName(ability));                                     // :192 (i[1] < 2): no prefix
                    slots.add(i);
                }
            }
        }
        if (hidden != null && !hidden.isEmpty()) {                                       // :277
            names.add("(隐藏)" + abilityName(hidden));                                    // :192
            slots.add(2);
        }
        int cmd = scene.pbMessage("\\l[1]请选择要修改的特性。", names, -1);                  // :195
        if (cmd >= 0 && cmd < slots.size()) {                                            // :196
            pkmn.setAbilitySlot(slots.get(cmd));                                         // :197 setAbility
            scene.pbMessage("\\l[1]" + pkmn.name + "的特性已被修改为" + abilityName(pkmn.ability) + "。");   // :198
        }
    }

    private String abilityName(String ability) {
        PbsData.Ability data = ability == null ? null : pbs.ability(ability);
        return data == null || data.name == null ? String.valueOf(ability) : data.name;
    }

    /** :200-215 "修改异色"; false when the player backed out (:203). */
    private boolean alterShiny(Pokemon pkmn) {
        List<String> shinyChoice = Arrays.asList("普通", "异色", "超闪");                    // :201
        int shiny = scene.pbMessage("\\l[1]修改" + pkmn.name + "是否异色。", shinyChoice, -1);   // :202
        if (shiny == -1) {
            return false;                                                                // :203
        }
        switch (shiny) {
            case 0:                                                                      // :205-207
                pkmn.shiny = false;                                                      // makeNotShiny
                pkmn.superShiny = false;                                                 // makeNotSuperShiny
                break;
            case 1:                                                                      // :208-210
                pkmn.shiny = true;                                                       // makeShiny
                pkmn.superShiny = false;
                break;
            case 2:                                                                      // :211-214
                pkmn.shiny = true;
                pkmn.superShiny = true;                                                  // makeSuperShiny
                break;
            default:
                break;
        }
        scene.pbMessage("\\l[1]" + pkmn.name + "已被修改为" + shinyChoice.get(shiny) + "。");   // :215
        return true;
    }

    /** :216-222 "教学招式" with {@code pbChooseValidMoveListForSpecies} (:303-324). */
    private void teachMove(Pokemon pkmn) {
        PbsData.Move move = chooseValidMoveListForSpecies(pkmn);                         // :217-218
        if (move != null) {                                                              // :219 move != 0
            handlers.pbLearnMove(pkmn, move, false, false,
                    new EggMoveTutor(state, pbs, scene, handlers).itemSceneFor());       // :221 pbLearnMove(pkmn, move)
        }
    }

    /** :303-324: the sorted list of the species' legal moves in a list window; null when cancelled. */
    private PbsData.Move chooseValidMoveListForSpecies(Pokemon pkmn) {
        List<PbsData.Move> legal = legalMoves(pkmn);                                     // :307 pbGetLegalMoves
        legal.sort((a, b) -> Integer.compare(a.id, b.id));                               // :311 sort by move id
        List<String> realcommands = new ArrayList<>();
        for (PbsData.Move move : legal) {                                                // :317-320
            realcommands.add(move.name);
        }
        int ret = scene.pbChooseFromList(realcommands, 0);                               // :321 pbCommands2(.., -1, moveDefault, true)
        return ret >= 0 && ret < legal.size() ? legal.get(ret) : null;                   // :323
    }

    /** 登记: Essentials' {@code pbGetLegalMoves(species)}: level-up moves, machine moves and the baby species' egg moves. */
    private List<PbsData.Move> legalMoves(Pokemon pkmn) {
        TreeMap<Integer, PbsData.Move> byId = new TreeMap<>();
        PbsData.Species species = pkmn.species;
        if (species == null) {
            return new ArrayList<>();
        }
        List<PbsData.LearnMove> learnset = pkmn.form != null && pkmn.form.moves != null && pkmn.form.moves.size > 0
                ? toList(pkmn.form.moves) : toList(species.moves);
        for (PbsData.LearnMove learn : learnset) {
            add(byId, learn.move);
        }
        for (com.badlogic.gdx.utils.ObjectMap.Entry<String, com.badlogic.gdx.utils.Array<String>> machine : pbs.tmCompatibility) {
            if (machine.value.contains(species.internalName, false)) {
                add(byId, machine.key);
            }
        }
        PbsData.Species baby = PBEvolution.babySpecies(pbs, species);
        for (String egg : (baby == null ? species : baby).eggMoves) {
            add(byId, egg);
        }
        return new ArrayList<>(byId.values());
    }

    private static List<PbsData.LearnMove> toList(com.badlogic.gdx.utils.Array<PbsData.LearnMove> array) {
        List<PbsData.LearnMove> list = new ArrayList<>();
        for (PbsData.LearnMove learn : array) {
            list.add(learn);
        }
        return list;
    }

    private void add(TreeMap<Integer, PbsData.Move> byId, String internalName) {
        PbsData.Move move = pbs.move(internalName);
        if (move != null) {
            byId.put(move.id, move);
        }
    }

    /** {@code def alterPokemonIV} (:240-266): repeats while a value is changed (:263). */
    private void alterPokemonIV(Pokemon pkmn) {
        while (true) {
            int numstats = 6;                                                            // :241
            int totaliv = 0;
            List<String> ivcommands = new ArrayList<>();
            for (int i = 0; i < numstats; i++) {                                         // :245-248
                ivcommands.add(statName(i) + " (" + pkmn.ivs[i] + ")");
                totaliv += pkmn.ivs[i];
            }
            String msg = "修改哪一项个体值？\n总计：" + totaliv + "/" + (numstats * 31)
                    + " (" + (100 * totaliv / (numstats * 31)) + "%)";                   // :249-250
            int cmd = scene.pbMessage(msg, ivcommands, -1);                              // :251
            if (cmd < 0 || cmd >= ivcommands.size()) {                                   // :252
                return;
            }
            int f = scene.pbMessageChooseNumber(
                    "\\l[1]修改" + statName(cmd) + "个体值(最大31)。", 0, 31, pkmn.ivs[cmd], pkmn.ivs[cmd]);   // :253-258
            if (f == pkmn.ivs[cmd]) {                                                    // :259
                return;
            }
            final int stat = cmd;
            final int value = f;
            pkmn.recalculatingStats(() -> pkmn.ivs[stat] = value);                       // :260-261 iv[cmd] = f; calcStats
            scene.pbMessage("\\l[1]已修改" + pkmn.name + "的" + statName(cmd) + "个体值为" + f + "。");   // :262
        }
    }

    /** {@code def alterPokemonEV} (:268-301): repeats while a value is changed (:298). */
    private void alterPokemonEV(Pokemon pkmn) {
        while (true) {
            int numstats = 6;                                                            // :269
            int totalev = 0;
            List<String> evcommands = new ArrayList<>();
            for (int i = 0; i < numstats; i++) {                                         // :272-275
                evcommands.add(statName(i) + " (" + pkmn.evs[i] + ")");
                totalev += pkmn.evs[i];
            }
            int cmd = scene.pbMessage("修改哪一项努力值？\n总计：" + totalev + "/" + EV_LIMIT
                    + " (" + (100 * totalev / EV_LIMIT) + "%)", evcommands, -1);         // :278-279
            if (cmd < 0 || cmd >= numstats) {                                            // :280
                return;
            }
            int upperLimit = 0;
            for (int i = 0; i < numstats; i++) {                                         // :283-285
                if (i != cmd) {
                    upperLimit += pkmn.evs[i];
                }
            }
            upperLimit = EV_LIMIT - upperLimit;                                          // :286
            upperLimit = Math.min(upperLimit, EV_STAT_LIMIT);                            // :287
            int thisValue = Math.min(pkmn.evs[cmd], upperLimit);                         // :288
            int f = scene.pbMessageChooseNumber(
                    "\\l[1]修改" + statName(cmd) + "努力值(最大" + upperLimit + ")。",
                    0, upperLimit, thisValue, thisValue);                                // :289-293
            if (f == pkmn.evs[cmd]) {                                                    // :294
                return;
            }
            final int stat = cmd;
            final int value = f;
            pkmn.recalculatingStats(() -> pkmn.evs[stat] = value);                       // :295-296
            scene.pbMessage("\\l[1]已修改" + pkmn.name + "的" + statName(cmd) + "努力值为" + f + "。");   // :297
        }
    }
}
