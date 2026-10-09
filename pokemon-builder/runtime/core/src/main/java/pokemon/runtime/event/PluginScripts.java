package pokemon.runtime.event;

import pokemon.runtime.battle.PBStats;
import pokemon.runtime.pokemon.BallTypes;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.pokemon.WildGenerator;
import pokemon.runtime.state.GameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * The project's own NPC scripts, each a blocking Ruby method run on a {@link pokemon.runtime.field.BlockingTask} whose
 * requests (messages, choices, the party screen, fades) the interpreter answers: {@code pbCrystalWarp}
 * (360_pbCrystalWarp), {@code pbMrHyper} (359_MrHyper), {@code changeBalls} (365_changeBalls), {@code resurrection2}
 * (351_resurrection) and {@code pbGiveAllMemories} (373_GiveAllMemories).
 *
 * <p>登记: {@code $DEBUG} is off, so the Heaven Crystal Seal check always applies; {@code pbShadowWarp}'s own viewport
 * fade is the map port's fade-transfer (the same one the Teleport move uses).</p>
 */
final class PluginScripts {
    private final GameState state;
    private final PbsData pbs;
    private final TaskFieldScene scene;
    private final MapPort port;

    PluginScripts(GameState state, PbsData pbs, TaskFieldScene scene, MapPort port) {
        this.state = state;
        this.pbs = pbs;
        this.scene = scene;
        this.port = port;
    }

    /** {@code pbChooseNonEggPokemon(1, 3)} + {@code var = $game_variables[1]}. */
    private int chooseNonEgg() {
        int var = scene.pbChooseNonEggPokemon();
        state.variables().set(1, var);
        state.variables().setText(3, var >= 0 ? state.trainer().party.get(var).name : "");
        return var;
    }

    private String itemName(String item) {
        PbsData.Item data = pbs == null ? null : pbs.item(item);
        return data == null || data.name == null ? item : data.name;
    }

    // =====================================================================
    // 360_pbCrystalWarp:1-41
    // =====================================================================

    private static final int[][] CRYSTAL_MAPS = {
        {201, 68, 19, 6},   // 天空城祭坛
        {241, 26, 13, 2},   // 茉克岛
        {242, 35, 14, 2},   // 厄季斯岛
        {247, 24, 13, 2},   // 灵诺森岛
        {243, 14, 13, 2},   // 绯焰岛
        {244, 41, 16, 2},   // 虹兰岛
        {264, 13, 44, 2},   // 落英岛
        {269, 15, 12, 2},   // 规盈岛
        {250, 34, 32, 2},   // 狱怜岛
        {142, 47, 45, 2},   // 幻谕岛
        {411, 26, 28, 2},   // 翼霄岛
        {408, 28, 31, 2},   // 影宿岛
        {409, 21, 16, 2},   // 磷火岛
    };

    void pbCrystalWarp() {
        if (!state.inventory().has("HEAVENCRYSEAL")) return;                        // :2 return if !$DEBUG && !pbHasItem?
        List<String> choices = new ArrayList<>();
        for (int i = 0; i <= state.variables().get(62); i++) {                      // :19 for i in 0..$game_variables[62]
            if (i >= CRYSTAL_MAPS.length) break;                                    // :20 break if !maps[i]
            choices.add(port.mapName(CRYSTAL_MAPS[i][0]));                          // :21-22
        }
        scene.pbMessage("好的，那么你要前往何地？");                                      // :24
        while (true) {                                                              // :25 loop do
            int c = scene.pbMessage("请选择你要前往的岛", choices, -1);                 // :26
            if (c == -1) {                                                          // :27
                scene.pbMessage("好的，要前往居民区随时来找我。");                         // :28
                return;
            }
            int[] target = CRYSTAL_MAPS[c];                                         // :31-34
            if (target[0] != state.currentMapId()) {                                // :35
                state.fieldGlobals().escapePoint = new int[0];                      // :44 pbEraseEscapePoint
                scene.runAction(() -> {                                             // :45-61 pbShadowWarp: fade, transfer, fade
                    port.transferThroughFade(target[0], target[1], target[2], target[3], false);
                    return 0f;
                });
                return;                                                             // :37
            }
            scene.pbMessage("已经在这里了。");                                          // :39
        }
    }

    // =====================================================================
    // 359_MrHyper:1-129
    // =====================================================================

    void pbMrHyper() {
        scene.pbMessage("\\G需要让哪只宝可梦进行特训呢？");                                // :2
        int var = chooseNonEgg();                                                   // :4-5
        if (var < 0 || var > 5) {                                                   // :7
            scene.pbMessage("\\G欢迎下次光临。");                                      // :8
            return;
        }
        Pokemon pkmn = state.trainer().party.get(var);                              // :11
        if (pkmn.level < 50) {                                                      // :15
            scene.pbMessage("\\G抱歉，需要达到50级后才可以进行特训。");                      // :16
            return;
        }
        List<Integer> indexList = new ArrayList<>();                                // :22-33
        List<String> choiceList = new ArrayList<>();
        for (int i = 0; i < pkmn.ivs.length; i++) {
            if (pkmn.ivs[i] < 31) {
                indexList.add(i);
                choiceList.add(PBStats.getName(i));
            }
        }
        if (choiceList.isEmpty()) {                                                 // :36
            scene.pbMessage("\\G" + pkmn.name + "不需要再锻炼了！");                      // :37
            return;
        }
        boolean hasSilverCap = state.inventory().has("BOTTLECAP");                  // :42
        if (!hasSilverCap) {
            choiceList.clear();                                                     // :44
        }
        if (state.inventory().has("GOLDBOTTLECAP")) {                               // :48
            choiceList.add("全部");                                                   // :49
        }
        if (choiceList.isEmpty()) {                                                 // :53
            scene.pbMessage("\\G抱歉，您的银色王冠和金色王冠不足。");                       // :54
            return;
        }
        if (choiceList.size() == 1 && choiceList.get(0).equals("全部")) {            // :59
            if (scene.pbConfirmMessage("\\G需要" + pkmn.name + "锻炼所有能力吗？")) {   // :61
                Arrays.fill(pkmn.ivs, 31);                                          // :63
                pkmn.hp = Math.min(pkmn.maxHp(), pkmn.hp);                          // :65 calcStats
                state.inventory().remove("GOLDBOTTLECAP", 1);                       // :67
                scene.pbMessage("\\me[Pkmn get]\\G" + pkmn.name + "的所有能力都锻炼了！\\wtnp[20]");   // :68
            }
            return;                                                                 // :71
        }
        while (true) {                                                              // :75 loop do
            int choice = scene.pbMessage("\\G需要" + pkmn.name + "锻炼哪一项能力？", choiceList, -1);   // :77
            if (choice == -1) {                                                     // :79
                scene.pbMessage("\\G欢迎下次光临。");                                  // :80
                return;
            }
            String ivName = choiceList.get(choice);                                 // :84
            if (ivName.equals("全部")) {                                             // :87
                if (!state.inventory().has("GOLDBOTTLECAP")) {                      // :89
                    scene.pbMessage("\\G抱歉，您的金色王冠不足。");                       // :90
                    continue;
                }
                Arrays.fill(pkmn.ivs, 31);                                          // :94
                pkmn.hp = Math.min(pkmn.maxHp(), pkmn.hp);                          // :96
                state.inventory().remove("GOLDBOTTLECAP", 1);                       // :98
                scene.pbMessage("\\me[Pkmn get]\\G" + pkmn.name + "的所有能力都锻炼了！\\wtnp[30]");   // :99
                return;                                                             // :101
            }
            if (!state.inventory().has("BOTTLECAP")) {                              // :106
                scene.pbMessage("\\G抱歉，您的银色王冠不足。");                           // :107
                continue;
            }
            int index = indexList.get(choice);                                      // :111
            if (pkmn.ivs[index] == 31) {                                            // :113
                scene.pbMessage("\\G" + pkmn.name + "的" + ivName + "能力不需要锻炼了！");   // :114
                continue;
            }
            pkmn.ivs[index] = 31;                                                   // :118
            pkmn.hp = Math.min(pkmn.maxHp(), pkmn.hp);                              // :120 calcStats
            state.inventory().remove("BOTTLECAP", 1);                               // :122
            scene.pbMessage("\\me[Pkmn get]\\G" + pkmn.name + "的" + ivName + "能力锻炼了！\\wtnp[30]");   // :123
            choiceList.remove(choice);                                              // :125
            indexList.remove(choice);                                               // :127
            if (choiceList.isEmpty() || choiceList.size() == 1 && choiceList.get(0).equals("全部")) {   // :129
                return;
            }
        }
    }

    // =====================================================================
    // 365_changeBalls:1-70
    // =====================================================================

    void changeBalls() {
        int payMoney = 1200;                                                        // :4
        if (state.trainer().money < payMoney) {                                     // :5
            scene.pbMessage("\\G对不起，您的金钱不足" + payMoney + "。");                  // :6
            return;
        }
        int var = chooseNonEgg();                                                   // :11-12
        if (var < 0 || var > 5) {                                                   // :13
            scene.pbMessage("\\G欢迎下次光临。");                                      // :14
            return;
        }
        Pokemon pkmn = state.trainer().party.get(var);                              // :17
        PbsData.Item oldItem = BallTypes.item(pbs, pkmn.ballused);                  // :18 pbBallTypeToItem
        String oldball = oldItem == null ? "POKEBALL" : oldItem.internalName;
        String oldballname = itemName(oldball);                                     // :19
        List<String> bannedBalls = Arrays.asList("MASTERBALL", "PETBALL");          // :22-25
        if (bannedBalls.contains(oldball)) {                                        // :26
            scene.pbMessage("\\G" + pkmn.name + "的" + oldballname + "是不能被换下的！");   // :27
            return;
        }
        List<String> ballTypes = new ArrayList<>();                                 // :32-39
        List<String> ballNames = new ArrayList<>();
        for (int i = 0; i < BallTypes.count(); i++) {
            PbsData.Item item = BallTypes.item(pbs, i);                             // :35 pbBallTypeToItem(i)
            String ball = item == null ? "POKEBALL" : item.internalName;
            if (bannedBalls.contains(ball)) continue;                               // :36
            ballTypes.add(ball);
            ballNames.add(itemName(ball));
        }
        while (true) {                                                              // :43 loop do
            int c = scene.pbMessage("\\G请选择新的球种", ballNames, -1);                // :44
            if (c == -1) {                                                          // :46
                scene.pbMessage("\\G欢迎下次光临。");                                  // :47
                return;
            }
            String newball = ballTypes.get(c);                                      // :50
            String newballname = itemName(newball);                                 // :51
            if (oldball.equals(newball)) {                                          // :53
                scene.pbMessage("\\G你选择的新球种和旧球种一样，\n无需更换哦。");          // :54
                continue;
            }
            state.trainer().money -= payMoney;                                      // :59
            pkmn.ballused = BallTypes.ballType(pbs, newball);                       // :63
            scene.pbMessage("\\me[Pkmn get]\\G" + pkmn.name + "的球种更换为" + newballname + "了！\\wtnp[30]");   // :64
            state.inventory().add(oldball, 1);                                      // :66 pbStoreItem
            scene.pbMessage(state.trainer().name + "将换下来的" + oldballname + "放进了背包。");   // :67
            break;                                                                  // :69
        }
    }

    // =====================================================================
    // 351_resurrection:1-40
    // =====================================================================

    private static final String[][] FOSSILS = {
        {"FOSSILIZEDBIRD", "FOSSILIZEDDRAKE", "DRACOZOLT"},   // 雷鸟龙
        {"FOSSILIZEDBIRD", "FOSSILIZEDDINO", "ARCTOZOLT"},    // 雷鸟海兽
        {"FOSSILIZEDFISH", "FOSSILIZEDDRAKE", "DRACOVISH"},   // 鳃鱼龙
        {"FOSSILIZEDFISH", "FOSSILIZEDDINO", "ARCTOVISH"},    // 鳃鱼海兽
    };

    void resurrection2(Random random, Consumer<Pokemon> pbAddPokemon) {
        List<String[]> items = new ArrayList<>();
        List<String> species = new ArrayList<>();
        List<String> choices = new ArrayList<>();
        for (String[] fossil : FOSSILS) {                                           // :10-16
            if (state.inventory().has(fossil[0]) && state.inventory().has(fossil[1])) {
                items.add(new String[] {fossil[0], fossil[1]});
                species.add(fossil[2]);
                PbsData.Species data = pbs.species(fossil[2]);
                choices.add(data == null ? fossil[2] : data.name);                  // :15 PBSpecies.getName
            }
        }
        if (choices.size() == 0) {                                                  // :17
            scene.pbMessage("你还没有任何可以拼接复活的化石啊，\n如果有的话，我可以帮你拼接并复活\n其中的宝可梦。");   // :18
            return;
        }
        scene.pbMessage("你有可以拼接复活的化石啊。");                                    // :21
        int i = scene.pbMessage("需要我帮你拼接复活哪个化石中的\n宝可梦呢？", choices, -1);   // :22
        if (i == -1) {                                                              // :23
            scene.pbMessage("如果你想要拼接复活化石中的\n宝可梦，随时都可以来找我。");          // :24
            return;
        }
        scene.pbMessage("好的，请稍等。\\wtnp[20]");                                    // :27
        scene.pbMessage("机器正在运行中\\wtnp[10]... \\wtnp[10]... \\wtnp[10]...\n\\wtnp[10]... \\wtnp[10]... \\wtnp[10]... \\wtnp[10]...");   // :28
        scene.pbMessage("化石复活完成！");                                              // :29
        state.inventory().remove(items.get(i)[0], 1);                               // :30
        state.inventory().remove(items.get(i)[1], 1);                               // :31
        Pokemon pkmn = WildGenerator.pbNewPkmn(pbs, pbs.species(species.get(i)), 1, state.trainer(),
                state.currentMapId(), random);                                      // :32 pbGenPkmn(species[i], 1)
        if (pkmn == null) return;
        if (state.variables().get(25) == 0) {                                       // :33
            Arrays.fill(pkmn.ivs, 31);                                              // :34
        } else if (state.variables().get(25) == 3 && state.switches().get(99)) {    // :35
            Arrays.fill(pkmn.ivs, 0);                                               // :36
        }
        pkmn.hp = pkmn.maxHp();                                                     // :38 calcStats
        pbAddPokemon.accept(pkmn);                                                  // :39
    }

    // =====================================================================
    // 373_GiveAllMemories:1-14
    // =====================================================================

    void pbGiveAllMemories() {
        String[] memories = {
            "FIREMEMORY", "WATERMEMORY", "ELECTRICMEMORY", "GRASSMEMORY",
            "ICEMEMORY", "FIGHTINGMEMORY", "POISONMEMORY", "GROUNDMEMORY",
            "FLYINGMEMORY", "PSYCHICMEMORY", "BUGMEMORY", "ROCKMEMORY",
            "GHOSTMEMORY", "DRAGONMEMORY", "DARKMEMORY", "STEELMEMORY",
            "FAIRYMEMORY",
        };
        for (String item : memories) state.inventory().add(item, 1);                // :9 pbStoreItem(item, 1)
        scene.pbMessage("\\me[Item get]获得了存储碟套装！");                              // :11
        scene.pbMessage("把存储碟放在了背包里面！。");                                    // :12
    }
}
