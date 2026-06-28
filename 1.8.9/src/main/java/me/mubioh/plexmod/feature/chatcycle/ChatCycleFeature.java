package me.mubioh.plexmod.feature.chatcycle;

import me.mubioh.plexmod.core.chat.ChatChannel;
import me.mubioh.plexmod.core.event.EventBus;
import me.mubioh.plexmod.core.event.PartyJoinEvent;
import me.mubioh.plexmod.core.event.PartyLeaveEvent;
import me.mubioh.plexmod.core.feature.PlexFeature;
import me.mubioh.plexmod.core.util.GameDetectorUtil;
import me.mubioh.plexmod.feature.community.CommunityChannel;
import me.mubioh.plexmod.feature.community.CommunityService;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Consumer;

public class ChatCycleFeature implements PlexFeature {

    private static final Logger LOGGER      = LogManager.getLogger("PlexMod/ChatCycleFeature");
    private static final int    MAX_DM_TABS = 4;

    private static ChatCycleFeature instance;
    public static ChatCycleFeature getInstance() { return instance; }

    private ChatChannel  currentChannel        = ChatChannel.ALL;
    private Object       pinnedTab             = null;
    private DmChannel    currentDmChannel      = null;
    private boolean      inParty               = false;
    private boolean      inCommunity           = false;
    private boolean      repopulating          = false;
    private boolean      hasFetchedThisSession = false;
    private boolean      lastTeamGameState     = false;

    private CommunityChannel communityChannel = null;

    private final List<DmChannel>                    dmChannels = new ArrayList<>();
    private final Map<ChatChannel, ChatChannelState> states     = new EnumMap<>(ChatChannel.class);

    private Consumer<PartyJoinEvent>  partyJoinListener;
    private Consumer<PartyLeaveEvent> partyLeaveListener;
    private Runnable onChannelChanged;

    @Override public String  getId()          { return "chat_cycle"; }
    @Override public String  getDisplayName() { return "Chat Cycle"; }
    @Override public String  getTooltip()     { return "Press Tab while chat is open to switch between channels."; }
    @Override public boolean isToggleable()   { return true; }

    @Override
    public void onEnable() {
        instance = this;
        for (ChatChannel ch : ChatChannel.values()) states.put(ch, new ChatChannelState(ch));

        partyJoinListener  = new Consumer<PartyJoinEvent>() {
            public void accept(PartyJoinEvent e) { inParty = true; }
        };
        partyLeaveListener = new Consumer<PartyLeaveEvent>() {
            public void accept(PartyLeaveEvent e) {
                inParty = false;
                if (pinnedTab    == ChatChannel.PARTY) pinnedTab = null;
                if (currentChannel == ChatChannel.PARTY) switchToChannel(ChatChannel.ALL);
            }
        };
        EventBus.subscribe(PartyJoinEvent.class,  partyJoinListener);
        EventBus.subscribe(PartyLeaveEvent.class, partyLeaveListener);
    }

    @Override
    public void onDisable() {
        if (partyJoinListener  != null) EventBus.unsubscribe(PartyJoinEvent.class,  partyJoinListener);
        if (partyLeaveListener != null) EventBus.unsubscribe(PartyLeaveEvent.class, partyLeaveListener);
        partyJoinListener     = null;
        partyLeaveListener    = null;
        currentChannel        = ChatChannel.ALL;
        pinnedTab             = null;
        currentDmChannel      = null;
        inParty               = false;
        inCommunity           = false;
        repopulating          = false;
        hasFetchedThisSession = false;
        lastTeamGameState     = false;
        onChannelChanged      = null;
        communityChannel      = null;
        for (ChatChannelState s : states.values()) s.clear();
        for (DmChannel dm : dmChannels) dm.clear();
        dmChannels.clear();
        instance = null;
    }

    // Called each client tick (via KeybindManager) to detect team game transitions
    public void onClientTick() {
        boolean current = GameDetectorUtil.isInTeamGame();
        if (current != lastTeamGameState) {
            lastTeamGameState = current;
            if (!current && this.currentChannel == ChatChannel.TEAM) {
                switchToChannel(ChatChannel.ALL);
            } else if (onChannelChanged != null) {
                onChannelChanged.run();
            }
        }
    }

    public void onServerJoin(String uuid) {
        if (uuid == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        String serverIp = "";
        if (mc.getCurrentServerData() != null) {
            serverIp = mc.getCurrentServerData().serverIP.toLowerCase(Locale.ROOT);
        }
        boolean isMineplex = serverIp.contains("mineplex.com") || serverIp.contains("127.0.0.1");
        if (isMineplex) {
            if (!hasFetchedThisSession) {
                hasFetchedThisSession = true;
                LOGGER.info("[PlexMod] Querying Mineplex API for player community data...");
                executeApiFetch(uuid);
            } else if (communityChannel != null) {
                communityChannel.clear();
            }
        } else {
            purgeCommunityState();
        }
    }

    public void purgeCommunityState() {
        hasFetchedThisSession = false;
        inCommunity           = false;
        communityChannel      = null;
        if (pinnedTab    == ChatChannel.COMMUNITY) pinnedTab    = null;
        if (currentChannel == ChatChannel.COMMUNITY) switchToChannel(ChatChannel.ALL);
        else if (onChannelChanged != null) onChannelChanged.run();
    }

    private void executeApiFetch(final String uuid) {
        CommunityService.fetchForPlayer(uuid,
                new Consumer<CommunityChannel>() {
                    public void accept(CommunityChannel channel) {
                        communityChannel = channel;
                        inCommunity      = true;
                        if (onChannelChanged != null) onChannelChanged.run();
                    }
                },
                new Runnable() {
                    public void run() {
                        communityChannel = null;
                        inCommunity      = false;
                        if (currentChannel == ChatChannel.COMMUNITY) switchToChannel(ChatChannel.ALL);
                    }
                }
        );
    }

    public void handleCommunityJoinedChat(String communityName) {
        if (communityChannel == null) communityChannel = new CommunityChannel(communityName);
        inCommunity = true;
        if (onChannelChanged != null) onChannelChanged.run();

        CommunityService.fetchByName(communityName,
                new Consumer<CommunityChannel>() {
                    public void accept(CommunityChannel ch) {
                        communityChannel = ch;
                        inCommunity      = true;
                        if (onChannelChanged != null) onChannelChanged.run();
                    }
                },
                new Runnable() {
                    public void run() { LOGGER.warn("[PlexMod] Community styling query failed"); }
                }
        );
    }

    public void handleCommunityLeftChat() {
        inCommunity      = false;
        communityChannel = null;
        if (pinnedTab    == ChatChannel.COMMUNITY) pinnedTab = null;
        if (currentChannel == ChatChannel.COMMUNITY) switchToChannel(ChatChannel.ALL);
        else if (onChannelChanged != null) onChannelChanged.run();
    }

    public void resetSessionFetch() { hasFetchedThisSession = false; }

    public void onChatMessage(IChatComponent message) {
        String plain = message.getUnformattedText();
        for (ChatChannelState s : states.values()) s.accept(message, plain);

        if (communityChannel != null) communityChannel.accept(message, plain);

        Minecraft mc = Minecraft.getMinecraft();
        String localName = mc.thePlayer != null ? mc.thePlayer.getName() : null;
        if (localName != null) {
            String other = DmChannel.parseOtherPlayer(plain, localName);
            if (other != null) getOrCreateDmChannel(other).accept(message, localName);
        }
    }

    public boolean isInTeamGame() { return GameDetectorUtil.isInTeamGame(); }

    private DmChannel getOrCreateDmChannel(String playerName) {
        for (DmChannel dm : dmChannels)
            if (dm.getPlayerName().equalsIgnoreCase(playerName)) return dm;

        if (dmChannels.size() >= MAX_DM_TABS) {
            DmChannel oldest = null;
            for (DmChannel dm : dmChannels)
                if (oldest == null || dm.getLastMessageTime() < oldest.getLastMessageTime()) oldest = dm;
            if (oldest != null) {
                if (currentDmChannel == oldest) currentDmChannel = null;
                if (pinnedTab        == oldest) pinnedTab        = null;
                dmChannels.remove(oldest);
            }
        }
        DmChannel dm = new DmChannel(playerName);
        dmChannels.add(dm);
        return dm;
    }

    private void switchToChannel(ChatChannel ch) {
        currentChannel   = ch;
        currentDmChannel = null;
        if (onChannelChanged != null) onChannelChanged.run();
    }

    private void switchToCommunity() {
        currentChannel   = ChatChannel.COMMUNITY;
        currentDmChannel = null;
        if (communityChannel != null) communityChannel.markRead();
        if (onChannelChanged != null) onChannelChanged.run();
    }

    private void switchToDm(DmChannel dm) {
        currentDmChannel = dm;
        currentChannel   = ChatChannel.ALL;
        dm.markRead();
        if (onChannelChanged != null) onChannelChanged.run();
    }

    public void cycle() {
        boolean inTeamGame = isInTeamGame();
        List<Object> tabs = new ArrayList<>();
        for (ChatChannel ch : ChatChannel.values()) {
            if (ch == ChatChannel.COMMUNITY) { if (inCommunity) tabs.add(ch); }
            else if (ch.isAvailable(inParty, inTeamGame)) tabs.add(ch);
        }
        tabs.addAll(dmChannels);
        if (tabs.isEmpty()) return;

        Object current = currentDmChannel != null ? currentDmChannel : currentChannel;
        int idx  = tabs.indexOf(current);
        Object next = tabs.get((idx + 1) % tabs.size());

        if (next instanceof ChatChannel) {
            if (next == ChatChannel.COMMUNITY) switchToCommunity();
            else switchToChannel((ChatChannel) next);
        } else if (next instanceof DmChannel) {
            switchToDm((DmChannel) next);
        }
    }

    public void handleTabClick(ChatChannel ch, boolean doubleClick) {
        if (ch == ChatChannel.COMMUNITY) {
            if (!inCommunity) return;
            if (doubleClick) {
                pinnedTab = (pinnedTab == ch) ? null : ch;
                if (currentChannel != ch || currentDmChannel != null) switchToCommunity();
                else if (onChannelChanged != null) onChannelChanged.run();
            } else {
                switchToCommunity();
            }
            return;
        }
        if (!ch.isAvailable(inParty, isInTeamGame())) return;
        if (doubleClick) {
            pinnedTab = (pinnedTab == ch) ? null : ch;
            if (currentChannel != ch || currentDmChannel != null) switchToChannel(ch);
            else if (onChannelChanged != null) onChannelChanged.run();
        } else {
            switchToChannel(ch);
        }
    }

    public void handleDmTabClick(DmChannel dm, boolean doubleClick) {
        if (doubleClick) {
            pinnedTab = (pinnedTab == dm) ? null : dm;
            if (currentDmChannel != dm) switchToDm(dm);
            else if (onChannelChanged != null) onChannelChanged.run();
        } else {
            switchToDm(dm);
        }
    }

    public void resetChannel() {
        boolean inTeamGame = isInTeamGame();
        if (pinnedTab instanceof DmChannel) {
            currentDmChannel = (DmChannel) pinnedTab;
            currentChannel   = ChatChannel.ALL;
        } else if (pinnedTab == ChatChannel.COMMUNITY && inCommunity) {
            currentDmChannel = null;
            currentChannel   = ChatChannel.COMMUNITY;
        } else if (pinnedTab instanceof ChatChannel
                && ((ChatChannel) pinnedTab).isAvailable(inParty, inTeamGame)) {
            currentDmChannel = null;
            currentChannel   = (ChatChannel) pinnedTab;
        } else {
            currentDmChannel = null;
            currentChannel   = ChatChannel.ALL;
        }
    }

    public boolean currentChannelMatches(String plain) {
        if (currentChannel == ChatChannel.COMMUNITY) {
            return communityChannel != null && communityChannel.isMatch(plain);
        }
        if (currentDmChannel != null) {
            Minecraft mc = Minecraft.getMinecraft();
            String name = mc.thePlayer != null ? mc.thePlayer.getName() : "";
            return currentDmChannel.isMatch(plain, name);
        }
        return states.get(currentChannel).isMatch(plain);
    }

    public void setOnChannelChanged(Runnable r) { onChannelChanged = r; }
    public void setRepopulating(boolean r)      { repopulating = r; }
    public boolean isRepopulating()             { return repopulating; }

    public CommunityChannel getCommunityChannel()  { return communityChannel; }
    public ChatChannel getPinnedChannel()          { return pinnedTab instanceof ChatChannel  ? (ChatChannel) pinnedTab  : null; }
    public DmChannel   getPinnedDmChannel()        { return pinnedTab instanceof DmChannel    ? (DmChannel)   pinnedTab  : null; }
    public ChatChannel getCurrentChannel()         { return currentChannel; }
    public ChatChannelState getCurrentState()      { return states.get(currentChannel); }
    public DmChannel   getCurrentDmChannel()       { return currentDmChannel; }
    public List<DmChannel> getDmChannels()         { return Collections.unmodifiableList(dmChannels); }
    public boolean isInParty()                     { return inParty; }
    public boolean isInCommunity()                 { return inCommunity; }
    public boolean isInTeamGamePublic()            { return isInTeamGame(); }
}
