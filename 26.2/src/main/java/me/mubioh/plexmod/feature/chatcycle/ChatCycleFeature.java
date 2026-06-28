package me.mubioh.plexmod.feature.chatcycle;

import me.mubioh.plexmod.core.chat.ChatChannel;
import me.mubioh.plexmod.core.event.EventBus;
import me.mubioh.plexmod.core.event.PartyJoinEvent;
import me.mubioh.plexmod.core.event.PartyLeaveEvent;
import me.mubioh.plexmod.core.feature.PlexFeature;
import me.mubioh.plexmod.core.util.GameDetectorUtil;
import me.mubioh.plexmod.core.util.MineplexHelper;
import me.mubioh.plexmod.feature.community.CommunityChannel;
import me.mubioh.plexmod.feature.community.CommunityService;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class ChatCycleFeature implements PlexFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlexMod/ChatCycleFeature");
    private static final int MAX_DM_TABS = 4;

    private static ChatCycleFeature instance;
    public static ChatCycleFeature getInstance() { return instance; }

    private ChatChannel  currentChannel   = ChatChannel.ALL;
    private Object       pinnedTab        = null;
    private DmChannel    currentDmChannel = null;
    private boolean      inParty          = false;
    private boolean      inCommunity      = false;
    private boolean      repopulating     = false;
    private boolean      hasFetchedThisSession = false;
    private boolean      lastTeamGameState = false;

    private CommunityChannel communityChannel = null;

    private final List<DmChannel>                    dmChannels = new ArrayList<>();
    private final Map<ChatChannel, ChatChannelState> states     = new EnumMap<>(ChatChannel.class);

    private Consumer<PartyJoinEvent>  partyJoinListener;
    private Consumer<PartyLeaveEvent> partyLeaveListener;
    private Runnable onChannelChanged;

    @Override public String getId()          { return "chat_cycle"; }
    @Override public String getDisplayName() { return "Chat Cycle"; }
    @Override public String getTooltip()     { return "Press Tab while chat is open to switch between channels."; }
    @Override public boolean isToggleable()  { return true; }

    @Override
    public void onEnable() {
        instance = this;
        for (ChatChannel channel : ChatChannel.values()) {
            states.put(channel, new ChatChannelState(channel));
        }

        partyJoinListener  = event -> inParty = true;
        partyLeaveListener = event -> {
            inParty = false;
            if (pinnedTab == ChatChannel.PARTY) pinnedTab = null;
            if (currentChannel == ChatChannel.PARTY) switchToChannel(ChatChannel.ALL);
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
        states.values().forEach(ChatChannelState::clear);
        dmChannels.forEach(DmChannel::clear);
        dmChannels.clear();
        instance = null;
    }

    public void onServerJoin(String uuid) {
        if (uuid == null) return;

        if (MineplexHelper.isOnMineplex()) {
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
        this.hasFetchedThisSession = false;
        this.inCommunity           = false;
        this.communityChannel      = null;
        if (pinnedTab == ChatChannel.COMMUNITY) pinnedTab = null;
        if (currentChannel == ChatChannel.COMMUNITY) {
            switchToChannel(ChatChannel.ALL);
        } else if (onChannelChanged != null) {
            onChannelChanged.run();
        }
    }

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

    private void executeApiFetch(String uuid) {
        CommunityService.fetchForPlayer(
                uuid,
                channel -> {
                    communityChannel = channel;
                    inCommunity      = true;
                    if (onChannelChanged != null) onChannelChanged.run();
                },
                () -> {
                    communityChannel = null;
                    inCommunity      = false;
                    if (currentChannel == ChatChannel.COMMUNITY) switchToChannel(ChatChannel.ALL);
                }
        );
    }

    public void handleCommunityJoinedChat(String communityName) {
        if (communityChannel == null) {
            communityChannel = new CommunityChannel(communityName);
        }
        inCommunity = true;
        if (onChannelChanged != null) onChannelChanged.run();

        CommunityService.fetchByName(
                communityName,
                populatedChannel -> {
                    this.communityChannel = populatedChannel;
                    this.inCommunity      = true;
                    if (onChannelChanged != null) onChannelChanged.run();
                },
                () -> LOGGER.warn("[PlexMod] Community styling query failed for: {}", communityName)
        );
    }

    public void handleCommunityLeftChat() {
        inCommunity      = false;
        communityChannel = null;
        if (pinnedTab == ChatChannel.COMMUNITY) pinnedTab = null;
        if (currentChannel == ChatChannel.COMMUNITY) {
            switchToChannel(ChatChannel.ALL);
        } else if (onChannelChanged != null) {
            onChannelChanged.run();
        }
    }

    public void resetSessionFetch() {
        this.hasFetchedThisSession = false;
    }

    public void onChatMessage(Component message) {
        String plainText = message.getString();

        for (ChatChannelState state : states.values()) {
            state.accept(message, plainText);
        }

        if (communityChannel != null) {
            communityChannel.accept(message, plainText);
        }

        Minecraft mc = Minecraft.getInstance();
        String localName = mc.player != null ? mc.player.getName().getString() : null;
        if (localName != null) {
            String other = DmChannel.parseOtherPlayer(plainText, localName);
            if (other != null) {
                getOrCreateDmChannel(other).accept(message, localName);
            }
        }
    }

    public boolean isInTeamGame() {
        return GameDetectorUtil.isInTeamGame();
    }

    private DmChannel getOrCreateDmChannel(String playerName) {
        for (DmChannel dm : dmChannels) {
            if (dm.getPlayerName().equalsIgnoreCase(playerName)) return dm;
        }

        if (dmChannels.size() >= MAX_DM_TABS) {
            DmChannel oldest = dmChannels.stream()
                    .min((a, b) -> Long.compare(a.getLastMessageTime(), b.getLastMessageTime()))
                    .orElse(null);
            if (oldest != null) {
                if (currentDmChannel == oldest) currentDmChannel = null;
                if (pinnedTab == oldest) pinnedTab = null;
                dmChannels.remove(oldest);
            }
        }

        DmChannel dm = new DmChannel(playerName);
        dmChannels.add(dm);
        return dm;
    }

    private void switchToChannel(ChatChannel channel) {
        currentChannel   = channel;
        currentDmChannel = null;
        if (onChannelChanged != null) onChannelChanged.run();
    }

    private void switchToDm(DmChannel dm) {
        currentDmChannel = dm;
        currentChannel   = ChatChannel.ALL;
        dm.markRead();
        if (onChannelChanged != null) onChannelChanged.run();
    }

    private void switchToCommunity() {
        currentChannel   = ChatChannel.COMMUNITY;
        currentDmChannel = null;
        if (communityChannel != null) communityChannel.markRead();
        if (onChannelChanged != null) onChannelChanged.run();
    }

    public void cycle() {
        boolean inTeamGame = isInTeamGame();

        List<Object> tabs = new ArrayList<>();
        for (ChatChannel ch : ChatChannel.values()) {
            if (ch == ChatChannel.COMMUNITY) {
                if (inCommunity) tabs.add(ch);
            } else if (ch.isAvailable(inParty, inTeamGame)) {
                tabs.add(ch);
            }
        }
        tabs.addAll(dmChannels);

        if (tabs.isEmpty()) return;

        Object current = currentDmChannel != null ? currentDmChannel : currentChannel;
        int idx = tabs.indexOf(current);
        Object next = tabs.get((idx + 1) % tabs.size());

        if (next instanceof ChatChannel ch) {
            if (ch == ChatChannel.COMMUNITY) switchToCommunity();
            else switchToChannel(ch);
        } else if (next instanceof DmChannel dm) {
            switchToDm(dm);
        }
    }

    public void handleTabClick(ChatChannel channel, boolean doubleClick) {
        if (channel == ChatChannel.COMMUNITY) {
            if (!inCommunity) return;
            if (doubleClick) {
                pinnedTab = (pinnedTab == channel) ? null : channel;
                if (currentChannel != channel || currentDmChannel != null) switchToCommunity();
                else if (onChannelChanged != null) onChannelChanged.run();
            } else {
                switchToCommunity();
            }
            return;
        }
        if (!channel.isAvailable(inParty, isInTeamGame())) return;
        if (doubleClick) {
            pinnedTab = (pinnedTab == channel) ? null : channel;
            if (currentChannel != channel || currentDmChannel != null) switchToChannel(channel);
            else if (onChannelChanged != null) onChannelChanged.run();
        } else {
            switchToChannel(channel);
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
        if (pinnedTab instanceof DmChannel dm) {
            currentDmChannel = dm;
            currentChannel   = ChatChannel.ALL;
        } else if (pinnedTab instanceof ChatChannel ch) {
            if (ch == ChatChannel.COMMUNITY && inCommunity) {
                currentDmChannel = null;
                currentChannel   = ch;
            } else if (ch.isAvailable(inParty, inTeamGame)) {
                currentDmChannel = null;
                currentChannel   = ch;
            } else {
                currentDmChannel = null;
                currentChannel   = ChatChannel.ALL;
            }
        } else {
            currentDmChannel = null;
            currentChannel   = ChatChannel.ALL;
        }
    }

    public boolean currentChannelMatches(String plainText) {
        if (currentDmChannel != null) {
            Minecraft mc = Minecraft.getInstance();
            String localName = mc.player != null ? mc.player.getName().getString() : "";
            return currentDmChannel.isMatch(plainText, localName);
        }
        if (currentChannel == ChatChannel.COMMUNITY) {
            return communityChannel != null && communityChannel.isMatch(plainText);
        }
        return states.get(currentChannel).isMatch(plainText);
    }

    public void setOnChannelChanged(Runnable callback) { this.onChannelChanged = callback; }
    public void setRepopulating(boolean r)             { this.repopulating = r; }
    public boolean isRepopulating()                    { return repopulating; }

    public ChatChannelState getState(ChatChannel channel) { return states.get(channel); }
    public ChatChannelState getCurrentState()             { return states.get(currentChannel); }
    public ChatChannel      getCurrentChannel()           { return currentChannel; }
    public ChatChannel      getPinnedChannel()            { return pinnedTab instanceof ChatChannel ch ? ch : null; }
    public DmChannel        getPinnedDmChannel()          { return pinnedTab instanceof DmChannel dm ? dm : null; }
    public DmChannel        getCurrentDmChannel()         { return currentDmChannel; }
    public List<DmChannel>  getDmChannels()               { return List.copyOf(dmChannels); }
    public CommunityChannel getCommunityChannel()         { return communityChannel; }
    public boolean          isInParty()                   { return inParty; }
    public boolean          isInCommunity()               { return inCommunity; }
    public boolean          isInTeamGamePublic()          { return isInTeamGame(); }
}