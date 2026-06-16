package me.mubioh.plexmod.feature.chatcycle;

import me.mubioh.plexmod.core.chat.ChatChannel;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ChatChannelState {

    private static final int MAX_HISTORY = 200;

    private final ChatChannel channel;
    private final Deque<Component> history = new ArrayDeque<>();

    public ChatChannelState(ChatChannel channel) {
        this.channel = channel;
    }

    public void accept(Component message, String plainText) {
        if (isMatch(plainText)) {
            history.addFirst(message);
            while (history.size() > MAX_HISTORY) history.removeLast();
        }
    }

    public boolean isMatch(String plainText) {
        return switch (channel) {
            case ALL   -> true;
            case PARTY -> isPartyMessage(plainText);
            case TEAM  -> isTeamMessage(plainText);
        };
    }

    private static boolean isPartyMessage(String text) {
        return text.startsWith("PARTY ") || text.startsWith("Party>");
    }

    private static boolean isTeamMessage(String text) {
        return text.startsWith("TEAM ");
    }

    public List<Component> getHistory()  { return List.copyOf(history); }
    public void clear()                  { history.clear(); }
    public ChatChannel getChannel()      { return channel; }
}