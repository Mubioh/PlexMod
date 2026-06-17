package me.mubioh.plexmod.feature.chatcycle;

import me.mubioh.plexmod.core.chat.ChatChannel;
import me.mubioh.plexmod.core.event.EventBus;
import me.mubioh.plexmod.core.event.PartyJoinEvent;
import me.mubioh.plexmod.core.event.PartyLeaveEvent;
import me.mubioh.plexmod.core.feature.PlexFeature;
import me.mubioh.plexmod.core.util.GameDetectorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ChatCycleFeature implements PlexFeature {

    private static final int MAX_DM_TABS = 4;

    private static final Set<String> TEAM_GAME_KEYWORDS = Set.of(
            "The Bridges",
            "Champions",
            "Cake Wars",
            "Minestrike",
            "Turf Wars",
            "Super Paintball",
            "Micro Battle",
            "Block Hunt",
            "Sheep Quest",
            "Bomb Lobbers"
    );

    private static ChatCycleFeature instance;
    public static ChatCycleFeature getInstance() { return instance; }

    private ChatChannel currentChannel   = ChatChannel.ALL;
    private Object      pinnedTab        = null;
    private DmChannel   currentDmChannel = null;
    private boolean inParty              = false;
    private boolean repopulating         = false;

    private final List<DmChannel>                    dmChannels = new ArrayList<>();
    private final Map<ChatChannel, ChatChannelState> states     = new EnumMap<>(ChatChannel.class);

    private Consumer<PartyJoinEvent>  partyJoinListener;
    private Consumer<PartyLeaveEvent> partyLeaveListener;
    private Runnable onChannelChanged;

    @Override public String getId()          { return "chat_cycle"; }
    @Override public String getDisplayName() { return "Chat Cycle"; }
    @Override public String getTooltip()     { return "Press Tab while chat is open to switch between channels."; }
    @Override public boolean isToggleable()  { return false; }

    @Override
    public void onEnable() {
        instance = this;
        for (ChatChannel channel : ChatChannel.values()) {
            states.put(channel, new ChatChannelState(channel));
        }

        partyJoinListener = event -> inParty = true;

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
        EventBus.unsubscribe(PartyJoinEvent.class,  partyJoinListener);
        EventBus.unsubscribe(PartyLeaveEvent.class, partyLeaveListener);
        partyJoinListener  = null;
        partyLeaveListener = null;
        currentChannel     = ChatChannel.ALL;
        pinnedTab          = null;
        currentDmChannel   = null;
        inParty            = false;
        repopulating       = false;
        onChannelChanged   = null;
        states.values().forEach(ChatChannelState::clear);
        dmChannels.forEach(DmChannel::clear);
        dmChannels.clear();
    }

    public void onChatMessage(Component message) {
        String plainText = message.getString();

        for (ChatChannelState state : states.values()) {
            state.accept(message, plainText);
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
        String gameName = GameDetectorUtil.getCurrentGameName();
        if (gameName == null) return false;
        String lower = gameName.toLowerCase(java.util.Locale.ROOT);
        for (String keyword : TEAM_GAME_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase(java.util.Locale.ROOT))) return true;
        }
        return false;
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

    public void cycle() {
        boolean inTeamGame = isInTeamGame();

        List<Object> tabs = new ArrayList<>();
        for (ChatChannel ch : ChatChannel.values()) {
            if (ch.isAvailable(inParty, inTeamGame)) tabs.add(ch);
        }
        tabs.addAll(dmChannels);

        if (tabs.isEmpty()) return;

        Object current = currentDmChannel != null ? currentDmChannel : currentChannel;
        int idx = tabs.indexOf(current);

        Object next = tabs.get((idx + 1) % tabs.size());

        if (next instanceof ChatChannel ch) {
            switchToChannel(ch);
        } else if (next instanceof DmChannel dm) {
            switchToDm(dm);
        }
    }

    public void handleTabClick(ChatChannel channel, boolean doubleClick) {
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
        } else if (pinnedTab instanceof ChatChannel ch && ch.isAvailable(inParty, inTeamGame)) {
            currentDmChannel = null;
            currentChannel   = ch;
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
        return states.get(currentChannel).isMatch(plainText);
    }

    public void setOnChannelChanged(Runnable callback) { this.onChannelChanged = callback; }
    public void setRepopulating(boolean r)             { this.repopulating = r; }
    public boolean isRepopulating()                    { return repopulating; }

    public ChatChannelState getState(ChatChannel channel)  { return states.get(channel); }
    public ChatChannelState getCurrentState()              { return states.get(currentChannel); }
    public ChatChannel getCurrentChannel()                 { return currentChannel; }
    public ChatChannel getPinnedChannel()                  { return pinnedTab instanceof ChatChannel ch ? ch : null; }
    public DmChannel getPinnedDmChannel()                  { return pinnedTab instanceof DmChannel dm ? dm : null; }
    public DmChannel getCurrentDmChannel()                 { return currentDmChannel; }
    public List<DmChannel> getDmChannels()                 { return List.copyOf(dmChannels); }
    public boolean isInParty()                             { return inParty; }
    public boolean isInTeamGamePublic()                    { return isInTeamGame(); }
}