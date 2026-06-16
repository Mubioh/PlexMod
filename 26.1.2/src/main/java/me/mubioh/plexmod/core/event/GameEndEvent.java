package me.mubioh.plexmod.core.event;

public class GameEndEvent extends Event {

    public enum Result { WIN, LOSS, UNKNOWN }

    private final Result result;

    public GameEndEvent(Result result) {
        this.result = result;
    }

    public Result getResult() {
        return result;
    }
}
