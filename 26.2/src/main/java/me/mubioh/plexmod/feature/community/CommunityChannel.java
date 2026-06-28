package me.mubioh.plexmod.feature.community;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class CommunityChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlexMod/CommunityChannel");
    private static final int MAX_HISTORY = 200;

    private final String communityName;
    private ChatFormatting fmtPrefixColor;
    private ChatFormatting fmtPlayerColor;
    private ChatFormatting fmtMessageColor;

    private final Deque<Component> history = new ArrayDeque<>();
    private boolean hasUnread = false;

    public CommunityChannel(String communityName) {
        this(communityName, ChatFormatting.BLUE, ChatFormatting.RED, ChatFormatting.GREEN);
    }

    public CommunityChannel(String communityName,
                            ChatFormatting fmtPrefix,
                            ChatFormatting fmtPlayerName,
                            ChatFormatting fmtMessage) {
        this.communityName   = communityName != null ? communityName.trim() : "";
        this.fmtPrefixColor  = fmtPrefix     != null ? fmtPrefix     : ChatFormatting.BLUE;
        this.fmtPlayerColor  = fmtPlayerName != null ? fmtPlayerName : ChatFormatting.RED;
        this.fmtMessageColor = fmtMessage    != null ? fmtMessage    : ChatFormatting.GREEN;
    }

    public void setFormattingStyles(ChatFormatting prefix, ChatFormatting name, ChatFormatting message) {
        this.fmtPrefixColor  = prefix  != null ? prefix  : ChatFormatting.BLUE;
        this.fmtPlayerColor  = name    != null ? name    : ChatFormatting.RED;
        this.fmtMessageColor = message != null ? message : ChatFormatting.GREEN;
    }

    /**
     * Matches against the Component sibling tree.
     *
     * Wire format: [name:prefixColor] [' ':null] [player:nameColor] [' ':null] [msg:msgColor]
     * So prefix is sibling 0, player name is sibling 2.
     */
    public boolean isMatch(Component message) {
        if (message == null || communityName.isEmpty()) return false;

        List<Component> siblings = message.getSiblings();
        // Need at least 3 siblings: name, space, player
        if (siblings.size() < 3) return false;

        try {
            Component prefixComponent = siblings.get(0);
            Component playerComponent = siblings.get(2);

            String prefixText = prefixComponent.getString().trim();
            if (!prefixText.equalsIgnoreCase(communityName)) return false;

            Style prefixStyle = prefixComponent.getStyle();
            Style playerStyle = playerComponent.getStyle();

            if (prefixStyle.getColor() == null || playerStyle.getColor() == null) return false;

            int expectedPrefixValue = TextColor.fromLegacyFormat(fmtPrefixColor).getValue();
            int expectedPlayerValue = TextColor.fromLegacyFormat(fmtPlayerColor).getValue();

            return prefixStyle.getColor().getValue() == expectedPrefixValue
                    && playerStyle.getColor().getValue() == expectedPlayerValue;
        } catch (Exception e) {
            LOGGER.error("[PlexMod] isMatch error", e);
            return false;
        }
    }

    public boolean isMatch(String plainText) {
        if (plainText == null || plainText.isBlank() || communityName.isEmpty()) return false;
        return plainText.startsWith(communityName + " ");
    }

    public void accept(Component message, String plainText) {
        if (!isMatch(message) && !isMatch(plainText)) return;

        history.addFirst(message);
        while (history.size() > MAX_HISTORY) history.removeLast();

        hasUnread = true;
    }

    public void markRead()           { hasUnread = false; }
    public boolean hasUnread()       { return hasUnread; }
    public String getCommunityName() { return communityName; }
    public String getDisplayName()   { return communityName; }
    public List<Component> getHistory() { return List.copyOf(history); }
    public void clear()              { history.clear(); hasUnread = false; }
}