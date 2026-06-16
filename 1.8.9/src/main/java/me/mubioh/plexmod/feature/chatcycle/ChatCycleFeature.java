package me.mubioh.plexmod.feature.chatcycle;

import me.mubioh.plexmod.core.chat.ChatChannel;
import me.mubioh.plexmod.core.event.EventBus;
import me.mubioh.plexmod.core.event.PartyJoinEvent;
import me.mubioh.plexmod.core.event.PartyLeaveEvent;
import me.mubioh.plexmod.core.feature.PlexFeature;
import me.mubioh.plexmod.core.util.GameDetectorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;

import java.util.*;
import java.util.function.Consumer;

public class ChatCycleFeature implements PlexFeature {

    private static final int MAX_DM_TABS = 4;

    private static final Set<String> TEAM_GAME_KEYWORDS = new HashSet<>(Arrays.asList(
            "The Bridges", "Champions", "Cake Wars", "Minestrike", "Turf Wars",
            "Super Paintball", "Micro Battle", "Block Hunt", "Sheep Quest", "Bomb Lobbers"
    ));

    private static ChatCycleFeature instance;
    public static ChatCycleFeature getInstance() { return instance; }

    private ChatChannel currentChannel   = ChatChannel.ALL;
    private Object      pinnedTab        = null;
    private DmChannel   currentDmChannel = null;
    private boolean     inParty          = false;
    private boolean     repopulating     = false;

    private final List<DmChannel>                    dmChannels = new ArrayList<>();
    private final Map<ChatChannel, ChatChannelState> states     = new EnumMap<>(ChatChannel.class);

    private Consumer<PartyJoinEvent> partyJoinListener;
    private Consumer<PartyLeaveEvent> partyLeaveListener;
    private Runnable onChannelChanged;

    @Override public String  getId()          { return "chat_cycle"; }
    @Override public String  getDisplayName() { return "Chat Cycle"; }
    @Override public String  getTooltip()     { return "Press Tab while chat is open to switch between channels."; }
    @Override public boolean isToggleable()   { return false; }

    @Override
    public void onEnable() {
        instance = this;
        for (ChatChannel ch : ChatChannel.values()) states.put(ch, new ChatChannelState(ch));

        partyJoinListener  = e -> inParty = true;
        partyLeaveListener = e -> {
            inParty = false;
            if (pinnedTab    == ChatChannel.PARTY) pinnedTab = null;
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
        currentChannel    = ChatChannel.ALL;
        pinnedTab         = null;
        currentDmChannel  = null;
        inParty = repopulating = false;
        onChannelChanged  = null;
        for (ChatChannelState s : states.values()) s.clear();
        for (DmChannel dm : dmChannels) dm.clear();
        dmChannels.clear();
        instance = null;
    }

    public void onChatMessage(IChatComponent message) {
        String plain = message.getUnformattedText();
        for (ChatChannelState s : states.values()) s.accept(message, plain);

        Minecraft mc = Minecraft.getMinecraft();
        String localName = mc.thePlayer != null ? mc.thePlayer.getName() : null;
        if (localName != null) {
            String other = DmChannel.parseOtherPlayer(plain, localName);
            if (other != null) getOrCreateDmChannel(other).accept(message, localName);
        }
    }

    public boolean isInTeamGame() {
        String name = GameDetectorUtil.getCurrentGameName();
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        for (String kw : TEAM_GAME_KEYWORDS)
            if (lower.contains(kw.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

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

    private void switchToDm(DmChannel dm) {
        currentDmChannel = dm;
        currentChannel   = ChatChannel.ALL;
        dm.markRead();
        if (onChannelChanged != null) onChannelChanged.run();
    }

    public void cycle() {
        boolean inTeamGame = isInTeamGame();
        List<Object> tabs = new ArrayList<>();
        for (ChatChannel ch : ChatChannel.values())
            if (ch.isAvailable(inParty, inTeamGame)) tabs.add(ch);
        tabs.addAll(dmChannels);
        if (tabs.isEmpty()) return;

        Object current = currentDmChannel != null ? currentDmChannel : currentChannel;
        int idx  = tabs.indexOf(current);
        Object next = tabs.get((idx + 1) % tabs.size());

        if (next instanceof ChatChannel) switchToChannel((ChatChannel) next);
        else if (next instanceof DmChannel) switchToDm((DmChannel) next);
    }

    public void handleTabClick(ChatChannel ch, boolean doubleClick) {
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

    public ChatChannel getPinnedChannel()  { return pinnedTab instanceof ChatChannel  ? (ChatChannel) pinnedTab  : null; }
    public DmChannel   getPinnedDmChannel(){ return pinnedTab instanceof DmChannel    ? (DmChannel)   pinnedTab  : null; }
    public ChatChannel getCurrentChannel() { return currentChannel; }
    public ChatChannelState getCurrentState() { return states.get(currentChannel); }
    public DmChannel   getCurrentDmChannel()      { return currentDmChannel; }
    public List<DmChannel> getDmChannels()        { return Collections.unmodifiableList(dmChannels); }
    public boolean isInParty()                     { return inParty; }
}
