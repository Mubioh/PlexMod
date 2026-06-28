package me.mubioh.plexmod.core.chat;

import me.mubioh.plexmod.core.event.*;
import me.mubioh.plexmod.core.util.PlayerDataService;
import me.mubioh.plexmod.feature.chatcycle.ChatCycleFeature;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatPatternEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlexMod/ChatPatterns");

    private static final long GAME_EVENT_COOLDOWN_MS = 5000;
    private static long lastGameEndTime   = 0;
    private static long lastGameStartTime = 0;

    private enum PendingEvent { NONE, GAME_START, GAME_END }
    private static PendingEvent pending = PendingEvent.NONE;

    private static final Pattern GAME_HEADER     = Pattern.compile("Game - .+");
    private static final Pattern PLACEMENT_LINE  = Pattern.compile("^\\s*1st Place .+");
    private static final Pattern TEAM_WIN_LINE   = Pattern.compile("^\\s*.+ won the game!.*");
    private static final Pattern PORTAL_TRAVEL   = Pattern.compile("^Portal> Traveling from .+ to .+$");
    private static final Pattern CHAT_UNSILENCED = Pattern.compile("^Chat> Chat is no longer silenced\\.$");
    private static final Pattern FRIEND_REQUEST  = Pattern.compile(
            "^Friends> ([A-Za-z0-9_]{1,16}) sent you a friend request! ACCEPT DENY!$");
    private static final Pattern PARTY_JOIN          = Pattern.compile("^Party> You have joined .+'s party\\.$");
    private static final Pattern PARTY_MEMBER_JOINED = Pattern.compile("^Party> .+ has joined the party\\.$");
    private static final Pattern PARTY_LEFT          = Pattern.compile("^Party> You have left the party\\.$");
    private static final Pattern PARTY_DISBANDED     = Pattern.compile("^Party> The party has been disbanded\\.$");
    private static final Pattern PARTY_KICKED        = Pattern.compile("^Party> You have been kicked from the party\\.$");
    private static final Pattern COMM_JOIN  = Pattern.compile("^Communities> You are now a member of (.+)\\.$");
    private static final Pattern COMM_LEAVE = Pattern.compile("^Communities> You have left the community\\.$");
    private static final Pattern COMM_KICK  = Pattern.compile("^Communities> You have been kicked from the community\\.$");

    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleMessage(stripColourCodes(message.getString()));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            pending = PendingEvent.NONE;
            EventBus.publish(new PartyLeaveEvent(PartyLeaveEvent.Reason.LEFT));
        });

        LOGGER.info("[PlexMod] ChatPatternEngine registered.");
    }

    private static void handleMessage(String plain) {
        if (PORTAL_TRAVEL.matcher(plain).matches()) {
            pending = PendingEvent.NONE;
            return;
        }

        if (GAME_HEADER.matcher(plain).find()) {
            if (pending == PendingEvent.NONE) {
                pending = PendingEvent.GAME_START;
                if (PlayerDataService.getInstance().isLocalPlayerStaff()) {
                    long now = System.currentTimeMillis();
                    if (now - lastGameStartTime >= GAME_EVENT_COOLDOWN_MS) {
                        lastGameStartTime = now;
                        EventBus.publish(new GameStartEvent());
                    }
                    pending = PendingEvent.NONE;
                }
            }
            return;
        }

        if (PLACEMENT_LINE.matcher(plain).matches() || TEAM_WIN_LINE.matcher(plain).matches()) {
            if (pending == PendingEvent.GAME_START) {
                pending = PendingEvent.GAME_END;
                if (PlayerDataService.getInstance().isLocalPlayerStaff()) {
                    long now = System.currentTimeMillis();
                    if (now - lastGameEndTime >= GAME_EVENT_COOLDOWN_MS) {
                        lastGameEndTime = now;
                        EventBus.publish(new GameEndEvent(GameEndEvent.Result.UNKNOWN));
                    }
                    pending = PendingEvent.NONE;
                }
            }
            return;
        }

        if (CHAT_UNSILENCED.matcher(plain).matches()) {
            switch (pending) {
                case GAME_END -> {
                    long now = System.currentTimeMillis();
                    if (now - lastGameEndTime >= GAME_EVENT_COOLDOWN_MS) {
                        lastGameEndTime = now;
                        EventBus.publish(new GameEndEvent(GameEndEvent.Result.UNKNOWN));
                    }
                }
                case GAME_START -> {
                    long now = System.currentTimeMillis();
                    if (now - lastGameStartTime >= GAME_EVENT_COOLDOWN_MS) {
                        lastGameStartTime = now;
                        EventBus.publish(new GameStartEvent());
                    }
                }
                default -> {}
            }
            pending = PendingEvent.NONE;
            return;
        }

        Matcher friendMatcher = FRIEND_REQUEST.matcher(plain);
        if (friendMatcher.matches()) {
            EventBus.publish(new FriendRequestEvent(friendMatcher.group(1)));
            return;
        }

        if (PARTY_JOIN.matcher(plain).matches() || PARTY_MEMBER_JOINED.matcher(plain).matches()) {
            EventBus.publish(new PartyJoinEvent());
            return;
        } else if (PARTY_DISBANDED.matcher(plain).matches()) {
            EventBus.publish(new PartyLeaveEvent(PartyLeaveEvent.Reason.DISBANDED));
            return;
        } else if (PARTY_KICKED.matcher(plain).matches()) {
            EventBus.publish(new PartyLeaveEvent(PartyLeaveEvent.Reason.KICKED));
            return;
        } else if (PARTY_LEFT.matcher(plain).matches()) {
            EventBus.publish(new PartyLeaveEvent(PartyLeaveEvent.Reason.LEFT));
            return;
        }

        Matcher commJoinMatcher = COMM_JOIN.matcher(plain);
        if (commJoinMatcher.matches()) {
            ChatCycleFeature feature = ChatCycleFeature.getInstance();
            if (feature != null) feature.handleCommunityJoinedChat(commJoinMatcher.group(1));
            return;
        }

        if (COMM_LEAVE.matcher(plain).matches() || COMM_KICK.matcher(plain).matches()) {
            ChatCycleFeature feature = ChatCycleFeature.getInstance();
            if (feature != null) feature.handleCommunityLeftChat();
        }
    }

    public static String stripColourCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }
}