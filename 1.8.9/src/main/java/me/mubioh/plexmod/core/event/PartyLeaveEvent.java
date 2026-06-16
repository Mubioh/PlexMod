package me.mubioh.plexmod.core.event;
public class PartyLeaveEvent extends Event {
    public enum Reason { LEFT, DISBANDED, KICKED }
    private final Reason reason;
    public PartyLeaveEvent(Reason reason) { this.reason = reason; }
    public Reason getReason() { return reason; }
}
