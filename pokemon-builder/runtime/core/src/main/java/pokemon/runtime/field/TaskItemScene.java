package pokemon.runtime.field;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

import java.util.List;

/**
 * The {@link ItemScene} an item handler sees while it runs on a {@link BlockingTask}: each call becomes an
 * {@link Request} handed to the screen, and the screen's answer is the call's result.
 */
public final class TaskItemScene implements ItemScene {

    /** What the handler asks the screen to do. */
    public enum Kind {
        DISPLAY, CONFIRM, REFRESH, HARD_REFRESH, CHOOSE_MOVE, CHOOSE_POKEMON, MESSAGE, MESSAGE_COMMANDS,
        SHOW_COMMANDS, CHOOSE_NUMBER, TOP_RIGHT, FORGET_MOVE, SE, EVOLUTION, START_SCENE, HELP_TEXT, ANNOTATIONS, CLEAR_ANNOTATIONS
    }

    /** One call of the handler. */
    public static final class Request {
        public final Kind kind;
        public String text = "";
        public List<String> commands;
        /** DISPLAY etc. have none; SHOW_COMMANDS: the start index; MESSAGE_COMMANDS: cmdIfCancel; CHOOSE_NUMBER: the maximum. */
        public int number;
        public int defaultValue;
        public int cancelValue;
        public Pokemon pokemon;
        public PbsData.Move move;
        public PbsData.Species species;
        public String[] annotations;
        public String item;
        public java.util.function.Predicate<Pokemon> able;

        Request(Kind kind) {
            this.kind = kind;
        }
    }

    private final java.util.function.Function<Request, Object> caller;

    public TaskItemScene(java.util.function.Function<Request, Object> caller) {
        this.caller = caller;
    }

    private Object call(Request request) {
        return caller.apply(request);
    }

    private Request text(Kind kind, String text) {
        Request request = new Request(kind);
        request.text = text;
        return request;
    }

    @Override
    public void pbStartScene(String helpText, String[] annotations) {
        Request request = text(Kind.START_SCENE, helpText);
        request.annotations = annotations;
        call(request);
    }

    @Override
    public void pbSetHelpText(String text) {
        call(text(Kind.HELP_TEXT, text));
    }

    @Override
    public void pbRefreshAnnotations(java.util.function.Predicate<Pokemon> able) {
        Request request = new Request(Kind.ANNOTATIONS);
        request.able = able;
        call(request);
    }

    @Override
    public void pbClearAnnotations() {
        call(new Request(Kind.CLEAR_ANNOTATIONS));
    }

    @Override
    public void pbDisplay(String text) {
        call(text(Kind.DISPLAY, text));
    }

    @Override
    public boolean pbConfirm(String text) {
        return Boolean.TRUE.equals(call(text(Kind.CONFIRM, text)));
    }

    @Override
    public void pbRefresh() {
        call(new Request(Kind.REFRESH));
    }

    @Override
    public void pbHardRefresh() {
        call(new Request(Kind.HARD_REFRESH));
    }

    @Override
    public int pbChooseMove(Pokemon pkmn, String text) {
        Request request = text(Kind.CHOOSE_MOVE, text);
        request.pokemon = pkmn;
        return (Integer) call(request);
    }

    @Override
    public int pbChoosePokemon(String text) {
        return (Integer) call(text(Kind.CHOOSE_POKEMON, text));
    }

    @Override
    public void pbMessage(String text) {
        call(text(Kind.MESSAGE, text));
    }

    @Override
    public int pbMessage(String text, List<String> commands, int cmdIfCancel) {
        Request request = text(Kind.MESSAGE_COMMANDS, text);
        request.commands = commands;
        request.cancelValue = cmdIfCancel;
        return (Integer) call(request);
    }

    @Override
    public int pbShowCommands(String text, List<String> commands, int index) {
        Request request = text(Kind.SHOW_COMMANDS, text);
        request.commands = commands;
        request.number = index;
        return (Integer) call(request);
    }

    @Override
    public int pbMessageChooseNumber(String text, int max, int defaultValue, int cancelValue) {
        Request request = text(Kind.CHOOSE_NUMBER, text);
        request.number = max;
        request.defaultValue = defaultValue;
        request.cancelValue = cancelValue;
        return (Integer) call(request);
    }

    @Override
    public void pbTopRightWindow(String text) {
        call(text(Kind.TOP_RIGHT, text));
    }

    @Override
    public int pbForgetMove(Pokemon pkmn, PbsData.Move moveToLearn) {
        Request request = new Request(Kind.FORGET_MOVE);
        request.pokemon = pkmn;
        request.move = moveToLearn;
        return (Integer) call(request);
    }

    @Override
    public void pbSEPlay(String name) {
        call(text(Kind.SE, name));
    }

    @Override
    public void pbEvolution(Pokemon pkmn, PbsData.Species species, String item) {
        Request request = new Request(Kind.EVOLUTION);
        request.item = item;
        request.pokemon = pkmn;
        request.species = species;
        call(request);
    }
}
