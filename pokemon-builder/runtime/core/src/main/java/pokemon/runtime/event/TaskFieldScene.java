package pokemon.runtime.event;

import pokemon.runtime.field.FieldScene;

import java.util.List;
import java.util.function.Function;

/** The {@link FieldScene} of a field script running on a {@code BlockingTask}: each call is a request for the interpreter. */
final class TaskFieldScene implements FieldScene {
    enum Kind { MESSAGE, CONFIRM, CHOOSE, WAIT, SE, TUTOR, ACTION, CHOOSE_NON_EGG, FORGET, RELEARN, ME, CHOOSE_ITEM, FLASH, WAIT_CANCEL }

    static final class Request {
        final Kind kind;
        String text = "";
        List<String> commands;
        int number;
        int cancel;
        List<String> movelist;
        boolean flag;
        java.util.function.Supplier<Float> action;
        pokemon.runtime.pokemon.Pokemon pokemon;
        pokemon.runtime.pokemon.PbsData.Move move;
        java.util.function.Predicate<String> filter;

        Request(Kind kind) {
            this.kind = kind;
        }
    }

    private final Function<Request, Object> caller;

    TaskFieldScene(Function<Request, Object> caller) {
        this.caller = caller;
    }

    @Override
    public void pbMessage(String text) {
        Request r = new Request(Kind.MESSAGE);
        r.text = text;
        caller.apply(r);
    }

    /** {@code pbMessageDisplay(msgWindow, text, false)} (274_Name-box:359 {@code break if !letterbyletter}): the text shows and the script goes on. */
    public void flashMessage(String text) {
        Request r = new Request(Kind.FLASH);
        r.text = text;
        caller.apply(r);
    }

    /** A wait of {@code frames} (40 fps) that ends early on C or B ({@code pbWaitMessage}'s loop); true when it was cancelled. */
    public boolean waitCancelable(int frames) {
        Request r = new Request(Kind.WAIT_CANCEL);
        r.number = frames;
        return Boolean.TRUE.equals(caller.apply(r));
    }

    @Override
    public boolean pbConfirmMessage(String text) {
        Request r = new Request(Kind.CONFIRM);
        r.text = text;
        return Boolean.TRUE.equals(caller.apply(r));
    }

    /** {@code pbMessage(text, commands, cmdIfCancel, skin, defaultCmd)}. */
    public int pbMessage(String text, List<String> commands, int cmdIfCancel, int defaultCmd) {
        Request r = new Request(Kind.CHOOSE);
        r.text = text;
        r.commands = commands;
        r.cancel = cmdIfCancel;
        r.number = defaultCmd;
        return (Integer) caller.apply(r);
    }

    @Override
    public int pbMessage(String text, List<String> commands, int cmdIfCancel) {
        Request r = new Request(Kind.CHOOSE);
        r.text = text;
        r.commands = commands;
        r.cancel = cmdIfCancel;
        return (Integer) caller.apply(r);
    }

    @Override
    public int pbMoveTutorChoose(String move, List<String> movelist, boolean byMachine) {
        Request r = new Request(Kind.TUTOR);
        r.text = move;
        r.movelist = movelist;
        r.flag = byMachine;
        return (Integer) caller.apply(r);
    }

    @Override
    public int pbChooseNonEggPokemon() {
        return (Integer) caller.apply(new Request(Kind.CHOOSE_NON_EGG));
    }

    @Override
    public boolean pbRelearnMoveScreen(pokemon.runtime.pokemon.Pokemon pkmn) {
        Request r = new Request(Kind.RELEARN);
        r.pokemon = pkmn;
        return Boolean.TRUE.equals(caller.apply(r));
    }

    @Override
    public int pbForgetMove(pokemon.runtime.pokemon.Pokemon pkmn, pokemon.runtime.pokemon.PbsData.Move move) {
        Request r = new Request(Kind.FORGET);
        r.pokemon = pkmn;
        r.move = move;
        return (Integer) caller.apply(r);
    }

    /**
     * Runs {@code action} on the interpreter's thread (it touches the map or starts a battle) and waits the seconds it
     * returns before the task goes on.
     */
    @Override
    public void runAction(java.util.function.Supplier<Float> action) {
        Request r = new Request(Kind.ACTION);
        r.action = action;
        caller.apply(r);
    }

    @Override
    public void pbWait(int frames) {
        Request r = new Request(Kind.WAIT);
        r.number = frames;
        caller.apply(r);
    }

    @Override
    public String pbChooseItem(java.util.function.Predicate<String> filter) {
        Request r = new Request(Kind.CHOOSE_ITEM);
        r.filter = filter;
        Object answer = caller.apply(r);
        return answer == null || answer.toString().isEmpty() ? null : answer.toString();
    }

    @Override
    public void pbMEPlay(String name) {
        Request r = new Request(Kind.ME);
        r.text = name;
        caller.apply(r);
    }

    @Override
    public void pbSEPlay(String name, int volume) {
        Request r = new Request(Kind.SE);
        r.text = name;
        r.number = volume;
        caller.apply(r);
    }
}
