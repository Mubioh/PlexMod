package me.mubioh.plexmod.core.event;
public class FriendRequestEvent extends Event {
    private final String senderName;
    public FriendRequestEvent(String senderName) { this.senderName = senderName; }
    public String getSenderName() { return senderName; }
}
