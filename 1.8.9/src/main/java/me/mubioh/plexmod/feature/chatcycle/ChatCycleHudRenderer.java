package me.mubioh.plexmod.feature.chatcycle;

import me.mubioh.plexmod.core.chat.ChatChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import java.util.List;

public class ChatCycleHudRenderer {

    public static final  int TAB_HEIGHT     = 14;
    public static final  int TAB_GAP        = 2;
    private static final int TAB_PADDING_X  = 4;

    private static final int BG_ACTIVE   = 0xAA000000;
    private static final int BG_INACTIVE = 0x88000000;

    private static final int TEXT_ACTIVE   = 0xFFFFFF;
    private static final int TEXT_INACTIVE = 0xAAAAAA;
    private static final int TEXT_PINNED   = 0xFFAA00;
    private static final int TEXT_DM_UNREAD       = 0x55FFFF;
    private static final int TEXT_DM_PINNED_UNREAD = 0x00AAAA;

    private ChatCycleHudRenderer() {}

    public static void renderTabs(int x, int y,
                                  ChatChannel current, ChatChannel pinnedCh,
                                  DmChannel currentDm, DmChannel pinnedDm,
                                  List<DmChannel> dms,
                                  boolean inParty, boolean inTeamGame,
                                  int mx, int my) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int curX = x - 2;

        for (ChatChannel ch : ChatChannel.values()) {
            if (!ch.isAvailable(inParty, inTeamGame)) continue;
            String label   = ch.displayName;
            int    w       = fr.getStringWidth(label) + TAB_PADDING_X * 2;
            boolean active = currentDm == null && ch == current;
            boolean pinned = ch == pinnedCh;
            boolean hover  = hover(mx, my, curX, y, w);

            Gui.drawRect(curX, y, curX + w, y + TAB_HEIGHT, active || hover ? BG_ACTIVE : BG_INACTIVE);
            int col = pinned ? TEXT_PINNED : active ? TEXT_ACTIVE : TEXT_INACTIVE;
            fr.drawString(label, curX + TAB_PADDING_X, y + (TAB_HEIGHT - 8) / 2, col);
            curX += w + TAB_GAP;
        }

        for (DmChannel dm : dms) {
            String label   = dm.getDisplayName();
            int    w       = fr.getStringWidth(label) + TAB_PADDING_X * 2;
            boolean active = dm == currentDm;
            boolean pinned = dm == pinnedDm;
            boolean hover  = hover(mx, my, curX, y, w);

            Gui.drawRect(curX, y, curX + w, y + TAB_HEIGHT, active || hover ? BG_ACTIVE : BG_INACTIVE);

            int col;
            if      (pinned && dm.hasUnread()) col = TEXT_DM_PINNED_UNREAD;
            else if (pinned)                   col = TEXT_PINNED;
            else if (active)                   col = TEXT_ACTIVE;
            else if (dm.hasUnread())           col = TEXT_DM_UNREAD;
            else                               col = TEXT_INACTIVE;

            fr.drawString(label, curX + TAB_PADDING_X, y + (TAB_HEIGHT - 8) / 2, col);
            curX += w + TAB_GAP;
        }
    }

    public static ChatChannel getClickedFixedTab(int mx, int my, int x, int y,
                                                  boolean inParty, boolean inTeamGame) {
        if (my < y || my >= y + TAB_HEIGHT) return null;
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int curX = x - 2;
        for (ChatChannel ch : ChatChannel.values()) {
            if (!ch.isAvailable(inParty, inTeamGame)) continue;
            int w = fr.getStringWidth(ch.displayName) + TAB_PADDING_X * 2;
            if (mx >= curX && mx < curX + w) return ch;
            curX += w + TAB_GAP;
        }
        return null;
    }

    public static DmChannel getClickedDmTab(int mx, int my, int x, int y,
                                             boolean inParty, boolean inTeamGame,
                                             List<DmChannel> dms) {
        if (my < y || my >= y + TAB_HEIGHT) return null;
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int curX = x - 2;
        for (ChatChannel ch : ChatChannel.values()) {
            if (!ch.isAvailable(inParty, inTeamGame)) continue;
            curX += fr.getStringWidth(ch.displayName) + TAB_PADDING_X * 2 + TAB_GAP;
        }
        for (DmChannel dm : dms) {
            int w = fr.getStringWidth(dm.getDisplayName()) + TAB_PADDING_X * 2;
            if (mx >= curX && mx < curX + w) return dm;
            curX += w + TAB_GAP;
        }
        return null;
    }

    private static boolean hover(int mx, int my, int x, int y, int w) {
        return mx >= x && mx < x + w && my >= y && my < y + TAB_HEIGHT;
    }
}
