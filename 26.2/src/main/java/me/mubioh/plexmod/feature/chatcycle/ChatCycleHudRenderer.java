package me.mubioh.plexmod.feature.chatcycle;

import me.mubioh.plexmod.core.chat.ChatChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class ChatCycleHudRenderer {

    public static final int TAB_HEIGHT     = 14;
    public static final int TAB_GAP        = 2;
    private static final int TAB_PADDING_X = 4;

    private static final int COLOUR_ACTIVE_BG    = 0xAA000000;
    private static final int COLOUR_INACTIVE_BG  = 0x88000000;
    private static final int COLOUR_HOVER_BG     = 0xAA000000;

    private static final int COLOUR_ACTIVE_TEXT   = 0xFFFFFFFF;
    private static final int COLOUR_INACTIVE_TEXT = 0xFFAAAAAA;
    private static final int COLOUR_PINNED_TEXT   = 0xFFFFAA00;

    private static final int COLOUR_DM_ACTIVE_TEXT   = 0xFFFFFFFF;
    private static final int COLOUR_DM_INACTIVE_TEXT = 0xFFAAAAAA;
    private static final int COLOUR_DM_UNREAD_TEXT   = 0xFF55FFFF;
    private static final int COLOUR_DM_PINNED_TEXT   = 0xFFFFAA00;
    private static final int COLOUR_DM_PINNED_UNREAD = 0xFF00AAAA;

    private ChatCycleHudRenderer() {}

    public static void renderTabs(GuiGraphicsExtractor graphics, int x, int y,
                                  ChatChannel current, ChatChannel pinnedChannel,
                                  DmChannel currentDm, DmChannel pinnedDm,
                                  List<DmChannel> dmChannels,
                                  boolean inParty, boolean inTeamGame,
                                  int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int curX = x - 2;

        for (ChatChannel channel : ChatChannel.values()) {
            if (!channel.isAvailable(inParty, inTeamGame)) continue;

            String label  = channel.displayName;
            int tabWidth  = mc.font.width(label) + TAB_PADDING_X * 2;

            boolean isActive = currentDm == null && channel == current;
            boolean isPinned = channel == pinnedChannel;
            boolean isHover  = isHovered(mouseX, mouseY, curX, y, tabWidth);

            int bg = isActive ? COLOUR_ACTIVE_BG : (isHover ? COLOUR_HOVER_BG : COLOUR_INACTIVE_BG);
            graphics.fill(curX, y, curX + tabWidth, y + TAB_HEIGHT, bg);

            int textColour = isPinned ? COLOUR_PINNED_TEXT
                    : isActive        ? COLOUR_ACTIVE_TEXT
                    :                   COLOUR_INACTIVE_TEXT;

            graphics.text(mc.font, label, curX + TAB_PADDING_X, y + (TAB_HEIGHT - 8) / 2, textColour);
            curX += tabWidth + TAB_GAP;
        }

        for (DmChannel dm : dmChannels) {
            String label  = dm.getDisplayName();
            int tabWidth  = mc.font.width(label) + TAB_PADDING_X * 2;

            boolean isActive = dm == currentDm;
            boolean isPinned = dm == pinnedDm;
            boolean isHover  = isHovered(mouseX, mouseY, curX, y, tabWidth);

            int bg = isActive ? COLOUR_ACTIVE_BG : (isHover ? COLOUR_HOVER_BG : COLOUR_INACTIVE_BG);
            graphics.fill(curX, y, curX + tabWidth, y + TAB_HEIGHT, bg);

            int textColour;
            if (isPinned && dm.hasUnread()) {
                textColour = COLOUR_DM_PINNED_UNREAD;
            } else if (isPinned) {
                textColour = COLOUR_DM_PINNED_TEXT;
            } else if (isActive) {
                textColour = COLOUR_DM_ACTIVE_TEXT;
            } else if (dm.hasUnread()) {
                textColour = COLOUR_DM_UNREAD_TEXT;
            } else {
                textColour = COLOUR_DM_INACTIVE_TEXT;
            }

            graphics.text(mc.font, label, curX + TAB_PADDING_X, y + (TAB_HEIGHT - 8) / 2, textColour);
            curX += tabWidth + TAB_GAP;
        }
    }

    public static @Nullable ChatChannel getClickedFixedTab(int mouseX, int mouseY,
                                                           int x, int y,
                                                           boolean inParty, boolean inTeamGame) {
        if (mouseY < y || mouseY >= y + TAB_HEIGHT) return null;

        Minecraft mc = Minecraft.getInstance();
        int curX = x - 2;

        for (ChatChannel channel : ChatChannel.values()) {
            if (!channel.isAvailable(inParty, inTeamGame)) continue;
            int tabWidth = mc.font.width(channel.displayName) + TAB_PADDING_X * 2;
            if (mouseX >= curX && mouseX < curX + tabWidth) return channel;
            curX += tabWidth + TAB_GAP;
        }

        return null;
    }

    public static @Nullable DmChannel getClickedDmTab(int mouseX, int mouseY,
                                                      int x, int y,
                                                      boolean inParty, boolean inTeamGame,
                                                      List<DmChannel> dmChannels) {
        if (mouseY < y || mouseY >= y + TAB_HEIGHT) return null;

        Minecraft mc = Minecraft.getInstance();
        int curX = x - 2;

        for (ChatChannel channel : ChatChannel.values()) {
            if (!channel.isAvailable(inParty, inTeamGame)) continue;
            curX += mc.font.width(channel.displayName) + TAB_PADDING_X * 2 + TAB_GAP;
        }

        for (DmChannel dm : dmChannels) {
            int tabWidth = mc.font.width(dm.getDisplayName()) + TAB_PADDING_X * 2;
            if (mouseX >= curX && mouseX < curX + tabWidth) return dm;
            curX += tabWidth + TAB_GAP;
        }

        return null;
    }

    private static boolean isHovered(int mouseX, int mouseY, int x, int y, int width) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + TAB_HEIGHT;
    }
}