package pokemon.runtime.event;

import pokemon.runtime.pokemon.Pokemon;
import java.util.ArrayDeque;

/** Headless request queue. Each interpreter waits for its own request only. */
public final class MenuService {
    public enum Kind { STORAGE, CHOOSE_TRADE, TRADE, GENDER, CHOOSE_ITEM, SHOW_MAP }
    public static final class Request {
        public final Kind kind;
        public String wanted, nickname, trainerName;
        public Pokemon offered;
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
