package me.mubioh.plexmod.core.event;

public abstract class Event {

    private boolean cancelled = false;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        if (!isCancellable()) {
            throw new UnsupportedOperationException("Event " + getClass().getSimpleName() + " is not cancellable");
        }
        this.cancelled = cancelled;
    }

    public boolean isCancellable() {
        return false;
    }
}
