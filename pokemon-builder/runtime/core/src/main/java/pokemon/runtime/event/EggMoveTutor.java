package pokemon.runtime.event;

import pokemon.runtime.field.FieldScene;
import pokemon.runtime.field.ItemHandlers;
import pokemon.runtime.field.ItemScene;
import pokemon.runtime.pokemon.PBEvolution;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.GameState;

import java.util.ArrayList;
import java.util.List;

/**
 * 362_changeShiny {@code teachEggMoves}: the NPC who teaches a party Pokemon one of its egg moves for 5000. The Ruby is
 * blocking script text; it runs on a {@link pokemon.runtime.field.BlockingTask} whose requests (messages, the choice list,
 * the party screen, the summary's forget mode) the interpreter answers.
 *
 * <p>登记: {@code pbGetSpeciesEggMoves(babyspecies, form)} reads the baby species' egg moves; the egg moves of a form are not
 * modelled.</p>
 */
final class EggMoveTutor {
    private final GameState state;
    private final PbsData pbs;
    private final FieldScene scene;
    private final ItemHandlers handlers;

    EggMoveTutor(GameState state, PbsData pbs, FieldScene scene, ItemHandlers handlers) {
        this.state = state;
        this.pbs = pbs;
        this.scene = scene;
        this.handlers = handlers;
    }

    /** The scene {@code pbLearnMove} (188_PItem_Items:779-821) talks to: its messages and the forget screen. */
    private ItemScene itemScene() {
        return new ItemScene() {
            public void pbStartScene(String helpText, String[] annotations) { }
            public void pbSetHelpText(String text) { }
            public void pbRefreshAnnotations(java.util.function.Predicate<Pokemon> able) { }
            public void pbClearAnnotations() { }
            public void pbDisplay(String text) { scene.pbMessage(text); }
            public boolean pbConfirm(String text) { return scene.pbConfirmMessage(text); }
            public void pbRefresh() { }
            public void pbHardRefresh() { }
            public int pbChooseMove(Pokemon pkmn, String text) { return -1; }
            public int pbChoosePokemon(String text) { return -1; }
            public void pbMessage(String text) { scene.pbMessage(text); }
            public int pbMessage(String text, List<String> commands, int cmdIfCancel) { return scene.pbMessage(text, commands, cmdIfCancel); }
            public int pbShowCommands(String text, List<String> commands, int index) { return scene.pbMessage(text, commands, -1); }
            public int pbMessageChooseNumber(String text, int max, int defaultValue, int cancelValue) { return cancelValue; }
            public void pbTopRightWindow(String text) { }
            public int pbForgetMove(Pokemon pkmn, PbsData.Move moveToLearn) { return scene.pbForgetMove(pkmn, moveToLearn); }
            public void pbSEPlay(String name) { scene.pbSEPlay(name, 100); }
            public void pbEvolution(Pokemon pkmn, PbsData.Species species, String item) { }
        };
    }

    /** {@code pbChangeShinyByNPC} (362_changeShiny:1-36). */
    void changeShinyByNPC() {
        int moneyCost = 250000;                                                      // :2
        scene.pbMessage("\\G想改变哪只宝可梦的颜色呢？");                                 // :3
        int var = scene.pbChooseNonEggPokemon();                                     // :4 pbChooseNonEggPokemon(1, 3)
        state.variables().set(1, var);
        state.variables().setText(3, var >= 0 ? state.trainer().party.get(var).name : "");
        if (var < 0 || var > 5) {                                                    // :6
            scene.pbMessage("\\G欢迎下次再来。");                                      // :7
            return;
        }
        Pokemon pkmn = state.trainer().party.get(var);                               // :11
        if (pkmn.shiny) {                                                            // :13
            if (scene.pbConfirmMessage("这已经是一只异色宝可梦了。\n要将其变回普通颜色吗？（免费）")) {   // :14-15
                pkmn.shiny = false;                                                  // :16 makeNotShiny
                scene.pbMessage("\\me[Pkmn get]\\G" + pkmn.name + "已恢复为普通颜色！");   // :17
            } else {
                scene.pbMessage("\\G没有进行任何更改。");                                // :19
            }
        } else {
            if (state.trainer().money < moneyCost) {                                 // :22
                scene.pbMessage("\\G抱歉，您的金钱不足（需要" + moneyCost + "）。");        // :23
                return;
            }
            if (scene.pbConfirmMessage("是否花费" + moneyCost + "将" + pkmn.name + "变为异色？")) {   // :26-27
                pkmn.shiny = true;                                                   // :28 makeShiny
                state.trainer().money -= moneyCost;                                  // :29
                scene.pbMEPlay("Pkmn get");                                          // :30
                scene.pbMessage("\\me[Pkmn get]\\G" + pkmn.name + "现在变为异色宝可梦了！");   // :31
            } else {
                scene.pbMessage("\\G没有进行任何更改。");                                // :33
            }
        }
    }

    /** {@code teachEggMoves} (362_changeShiny:38-85). */
    void teachEggMoves() {
        int price = 5000;                                                            // :39 价格
        if (state.trainer().money < price) {                                         // :40
            scene.pbMessage("\\G对不起，您的金钱不足。");                                // :41
            return;
        }
        if (scene.pbConfirmMessage("\\G确定要支付" + price + "金钱教给宝可梦一个\n蛋招式吗？")) {   // :44
            int var = scene.pbChooseNonEggPokemon();                                 // :45 pbChooseNonEggPokemon(1,3)
            state.variables().set(1, var);
            state.variables().setText(3, var >= 0 ? state.trainer().party.get(var).name : "");
            if (var < 0 || var > 5) {                                                // :47
                scene.pbMessage("\\G欢迎下次光临。");                                    // :48
                return;
            }
            Pokemon pkmn = state.trainer().party.get(var);                           // :51 pbGetPokemon(1)
            PbsData.Species baby = PBEvolution.babySpecies(pbs, pkmn.species);       // :52
            List<String> oEggMoves = new ArrayList<>();                              // :53
            for (String move : baby.eggMoves) {
                oEggMoves.add(move);
            }
            while (true) {                                                           // :54 loop do
                List<String> eggMoves = new ArrayList<>();                           // :55-60 clone, nil the known ones, compact
                for (String move : oEggMoves) {
                    if (!knows(pkmn, move)) eggMoves.add(move);
                }
                if (eggMoves.isEmpty()) {                                            // :61
                    scene.pbMessage("\\G" + pkmn.name + "没有可以学习的蛋招式。");          // :62
                    return;
                }
                List<String> moveNames = new ArrayList<>();                          // :65-68
                for (String move : eggMoves) {
                    PbsData.Move data = pbs.move(move);
                    moveNames.add(data == null ? move : data.name);
                }
                int choice = scene.pbMessage("\\G要教给" + pkmn.name + "哪一个蛋招式？", moveNames, -1);   // :69
                if (choice == -1) {                                                  // :70
                    scene.pbMessage("\\G欢迎下次光临。");                                // :71
                    break;
                }
                if (handlers.pbLearnMove(pkmn, pbs.move(eggMoves.get(choice)), false, true, itemScene())) {   // :74
                    eggMoves.remove(choice);                                         // :75
                    state.trainer().money -= price;                                  // :76
                    scene.pbMessage("\\G\\se[Mart buy item]你支付了" + price + "金钱。\\wtnp[30]");   // :77
                    if (eggMoves.isEmpty() || state.trainer().money < price) {       // :78
                        break;
                    }
                }
            }
        } else {
            scene.pbMessage("\\G欢迎下次光临。");                                        // :82
        }
    }

    private static boolean knows(Pokemon pkmn, String move) {
        for (Pokemon.MoveSlot slot : pkmn.moves) {
            if (slot.move != null && slot.move.internalName.equals(move)) return true;
        }
        return false;
    }
}
