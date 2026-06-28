package me.mubioh.plexmod.feature.community;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class CommunityChannel {

    private static final Logger LOGGER      = LogManager.getLogger("PlexMod/CommunityChannel");
    private static final int    MAX_HISTORY = 200;

    private final String            communityName;
    private EnumChatFormatting      fmtPrefixColor;
    private EnumChatFormatting      fmtPlayerColor;
    private EnumChatFormatting      fmtMessageColor;

    private final Deque<IChatComponent> history = new ArrayDeque<>();
    private boolean hasUnread = false;

    public CommunityChannel(String communityName) {
        this(communityName, EnumChatFormatting.BLUE, EnumChatFormatting.RED, EnumChatFormatting.GREEN);
    }

    public CommunityChannel(String communityName,
                            EnumChatFormatting fmtPrefix,
                            EnumChatFormatting fmtPlayerName,
                            EnumChatFormatting fmtMessage) {
        this.communityName   = communityName != null ? communityName.trim() : "";
        this.fmtPrefixColor  = fmtPrefix     != null ? fmtPrefix     : EnumChatFormatting.BLUE;
        this.fmtPlayerColor  = fmtPlayerName != null ? fmtPlayerName : EnumChatFormatting.RED;
        this.fmtMessageColor = fmtMessage    != null ? fmtMessage    : EnumChatFormatting.GREEN;
    }

    public void setFormattingStyles(EnumChatFormatting prefix,
                                    EnumChatFormatting name,
                                    EnumChatFormatting message) {
        this.fmtPrefixColor  = prefix  != null ? prefix  : EnumChatFormatting.BLUE;
        this.fmtPlayerColor  = name    != null ? name    : EnumChatFormatting.RED;
        this.fmtMessageColor = message != null ? message : EnumChatFormatting.GREEN;
    }

    public boolean isMatch(IChatComponent message) {
        if (message == null || communityName.isEmpty()) return false;
        return isMatch(message.getUnformattedText());
    }

    public boolean isMatch(String plainText) {
        if (plainText == null || plainText.trim().isEmpty() || communityName.isEmpty()) return false;
        return plainText.startsWith(communityName + " ")
                || plainText.startsWith("[" + communityName + "]");
    }

    public void accept(IChatComponent message, String plainText) {
        if (!isMatch(message) && !isMatch(plainText)) return;
        history.addFirst(message);
        while (history.size() > MAX_HISTORY) history.removeLast();
        hasUnread = true;
    }

    public void markRead()                        { hasUnread = false; }
    public boolean hasUnread()                    { return hasUnread; }
    public String getCommunityName()              { return communityName; }
    public String getDisplayName()                { return communityName; }
    public List<IChatComponent> getHistory()      { return new ArrayList<>(history); }
    public void clear()                           { history.clear(); hasUnread = false; }
    public EnumChatFormatting getFmtPrefixColor() { return fmtPrefixColor; }
    public EnumChatFormatting getFmtPlayerColor() { return fmtPlayerColor; }
}
