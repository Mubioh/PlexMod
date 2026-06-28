package me.mubioh.plexmod.feature.chatcycle;

import me.mubioh.plexmod.core.chat.ChatChannel;
import net.minecraft.util.IChatComponent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ChatChannelState {

    private static final int MAX_HISTORY = 200;

    private final ChatChannel channel;
    private final Deque<IChatComponent> history = new ArrayDeque<>();

    public ChatChannelState(ChatChannel channel) { this.channel = channel; }

    public void accept(IChatComponent message, String plainText) {
        if (isMatch(plainText)) {
            history.addFirst(message);
            while (history.size() > MAX_HISTORY) history.removeLast();
        }
    }

    public boolean isMatch(String plain) {
        switch (channel) {
            case ALL:       return true;
            case PARTY:     return plain.startsWith("PARTY ") || plain.startsWith("Party>");
            case TEAM:      return plain.startsWith("TEAM ");
            case COMMUNITY: return false; // Community matching handled by CommunityChannel
            default:        return false;
        }
    }

    public List<IChatComponent> getHistory() { return new ArrayList<>(history); }
    public void clear()                       { history.clear(); }
    public ChatChannel getChannel()           { return channel; }
}
