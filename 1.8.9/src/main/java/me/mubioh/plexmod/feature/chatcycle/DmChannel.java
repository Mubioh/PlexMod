package me.mubioh.plexmod.feature.chatcycle;

import net.minecraft.util.IChatComponent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class DmChannel {

    private static final int MAX_HISTORY = 100;

    private final String playerName;
    private final Deque<IChatComponent> history = new ArrayDeque<>();
    private long    lastMessageTime;
    private boolean hasUnread = false;

    public DmChannel(String playerName) {
        this.playerName      = playerName;
        this.lastMessageTime = System.currentTimeMillis();
    }

    public static String parseOtherPlayer(String plainText, String localPlayerName) {
        String[] parts = plainText.split(" > ", 2);
        if (parts.length != 2) return null;
        String sender    = parts[0].trim();
        if (!sender.matches("[A-Za-z0-9_]{1,16}")) return null;
        String[] right   = parts[1].split(" ", 2);
        if (right.length == 0) return null;
        String recipient = right[0].trim();
        if (!recipient.matches("[A-Za-z0-9_]{1,16}")) return null;
        if (sender.equalsIgnoreCase(localPlayerName))    return recipient;
        if (recipient.equalsIgnoreCase(localPlayerName)) return sender;
        return null;
    }

    public boolean isMatch(String plainText, String localPlayerName) {
        String other = parseOtherPlayer(plainText, localPlayerName);
        return other != null && other.equalsIgnoreCase(playerName);
    }

    public void accept(IChatComponent message, String localPlayerName) {
        history.addFirst(message);
        lastMessageTime = System.currentTimeMillis();
        String plain = message.getUnformattedText();
        String[] parts = plain.split(" > ", 2);
        if (parts.length == 2 && !parts[0].trim().equalsIgnoreCase(localPlayerName))
            hasUnread = true;
        while (history.size() > MAX_HISTORY) history.removeLast();
    }

    public void markRead()               { hasUnread = false; }
    public boolean hasUnread()           { return hasUnread; }
    public List<IChatComponent> getHistory() { return new ArrayList<>(history); }
    public String getPlayerName()        { return playerName; }
    public long   getLastMessageTime()   { return lastMessageTime; }
    public String getDisplayName()       { return playerName; }
    public void   clear()                { history.clear(); hasUnread = false; }
}
