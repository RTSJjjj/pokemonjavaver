package pokemon.runtime.event;

import pokemon.runtime.pokemon.Pokemon;
import java.util.ArrayDeque;

/** Headless request queue. Each interpreter waits for its own request only. */
public final class MenuService {
    public enum Kind { STORAGE, CHOOSE_TRADE, TRADE, GENDER, CHOOSE_ITEM, SHOW_MAP, MART, STARTER, TUTOR,
        /** {@code pbChooseNonEggPokemon}: the party screen, eggs refused (252_PSystem_PokemonUtilities:268-270). */
        CHOOSE_NON_EGG,
        /** {@code pbChoosePokemon(variable, nameVariable, proc, allowIneligible)}: the party screen with an able proc (252_PSystem_PokemonUtilities:246-266). */
        CHOOSE_ABLE,
        /** {@code pbRelearnMoveScreen(pokemon)} (228_PScreen_MoveRelearner:221-228); the result is 1 when a move was taught. */
        RELEARN,
        /** {@code pbHatchAnimation(pokemon)} (225_PScreen_EggHatching:182-189). */
        HATCH,
        /** {@code pbHallOfFameEntry} (232_PScreen_HallOfFame:524-528). */
        HALL_OF_FAME,
        /** {@code $scene = Scene_Credits.new} (080_Scene_Credits). */
        CREDITS,
        /** {@code pbTrainerPC} (PScreen_PC:205-209). */
        TRAINER_PC,
        /** {@code PokemonTrainerCardScreen#pbStartBadgeScreen} (300_B2W2_Trainer_Card:1084-1096). */
        TRAINER_CARD_BADGES,
        /** {@code pbSlotMachine(difficulty)} (236_PMinigame_SlotMachine:384-402); {@code index} is the difficulty. */
        SLOT_MACHINE,
        /** {@code pbForgetMove(pkmn, move)}: the summary screen's forget mode (188_PItem_Items:823-830). */
        FORGET_MOVE }
    public static final class Request {
        public final Kind kind;
        public String wanted, nickname, trainerName;
        public Pokemon offered;
        /** CHOOSE_ABLE: which proc ({@code relearnable}: {@code pbHasRelearnableMove?(p)}) and {@code allowIneligible}. */
        public String ableProc;
        public boolean allowIneligible;
        /** FORGET_MOVE: the Pokemon and the move it is going to learn. */
        public Pokemon pokemon;
        public pokemon.runtime.pokemon.PbsData.Move learnMove;
        public int index = -1, variable, nameVariable;
        /**
         * SHOW_MAP: pbShowMap(region, wallmap) (PScreen_RegionMap:431-437);
         * region -1 means "the player's region".
         */
        public int region = -1;
        public boolean wallmap = true;
        /**
         * CHOOSE_ITEM: only items the predicate accepts can be picked, mirroring
         * {@code pbChooseItemScreen(proc { |item| ... })} (PField_BerryPlants:353).
         */
        public java.util.function.Predicate<String> filter;
        /**
         * MART: {@code pbPokemonMart(stock, speech=nil, cantsell=false)}
         * (230_PScreen_Mart:807-846); the shop's items, its greeting and whether selling is off.
         */
        /** STARTER: {@code DiegoWTsStarterSelection.new(a,b,c)}: the dex numbers of the three starters. */
        public int[] dex;
        public java.util.List<String> items;
        public String speech;
        public boolean cantSell;
        /** TUTOR: {@code pbMoveTutorChoose(move, movelist, bymachine)} (253_PSystem_Utilities:982-1018). */
        public String move;
        public java.util.List<String> movelist;
        public boolean byMachine;
        public int result = -1;
        public String text = "";
        public boolean done;
        public Request(Kind kind) { this.kind = kind; }
        public void complete(int result, String text) { this.result = result; this.text = text; done = true; }
    }
    private final ArrayDeque<Request> requests = new ArrayDeque<>();
    public Request submit(Request request) { requests.add(request); return request; }
    public Request pending() {
        while (!requests.isEmpty() && requests.peek().done) requests.remove();
        return requests.peek();
    }
    public void clear() { for (Request request : requests) request.complete(-1, ""); requests.clear(); }
}
