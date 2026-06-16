package me.mubioh.plexmod.feature.chatcycle;

import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class DmChannel {

    private static final int MAX_HISTORY = 100;

    private final String playerName;
    private final Deque<Component> history = new ArrayDeque<>();
    private long lastMessageTime;
    private boolean hasUnread = false;

    public DmChannel(String playerName) {
        this.playerName      = playerName;
        this.lastMessageTime = System.currentTimeMillis();
    }

    public static String parseOtherPlayer(String plainText, String localPlayerName) {
        String[] parts = plainText.split(" > ", 2);
        if (parts.length != 2) return null;

        String sender = parts[0].trim();
        if (!sender.matches("[A-Za-z0-9_]{1,16}")) return null;

        String[] rightParts = parts[1].split(" ", 2);
        if (rightParts.length == 0) return null;
        String recipient = rightParts[0].trim();
        if (!recipient.matches("[A-Za-z0-9_]{1,16}")) return null;

        if (sender.equalsIgnoreCase(localPlayerName)) return recipient;
        if (recipient.equalsIgnoreCase(localPlayerName)) return sender;
        return null;
    }

    public boolean isMatch(String plainText, String localPlayerName) {
        String[] parts = plainText.split(" > ", 2);
        if (parts.length != 2) return false;

        String sender = parts[0].trim();
        if (!sender.matches("[A-Za-z0-9_]{1,16}")) return false;

        String[] rightParts = parts[1].split(" ", 2);
        if (rightParts.length == 0) return false;
        String recipient = rightParts[0].trim();
        if (!recipient.matches("[A-Za-z0-9_]{1,16}")) return false;

        return (sender.equalsIgnoreCase(playerName) && recipient.equalsIgnoreCase(localPlayerName))
                || (sender.equalsIgnoreCase(localPlayerName) && recipient.equalsIgnoreCase(playerName));
    }

    public void accept(Component message, String localPlayerName) {
        history.addFirst(message);
        lastMessageTime = System.currentTimeMillis();

        String plain = message.getString();
        String[] parts = plain.split(" > ", 2);
        if (parts.length == 2) {
            String sender = parts[0].trim();
            if (!sender.equalsIgnoreCase(localPlayerName)) {
                hasUnread = true;
            }
        }

        while (history.size() > MAX_HISTORY) history.removeLast();
    }

    public void markRead()           { hasUnread = false; }
    public boolean hasUnread()       { return hasUnread; }

    public List<Component> getHistory()  { return List.copyOf(history); }
    public String getPlayerName()        { return playerName; }
    public long getLastMessageTime()     { return lastMessageTime; }
    public String getDisplayName()       { return playerName; }
    public void clear()                  { history.clear(); hasUnread = false; }
}