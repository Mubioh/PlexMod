package me.mubioh.plexmod.core.chat;

import me.mubioh.plexmod.core.event.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatPatternEngine {

    private static final Logger LOGGER = LogManager.getLogger("PlexMod/ChatPatterns");

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

    private static boolean registered = false;

    public static final ChatPatternEngine INSTANCE = new ChatPatternEngine();

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        LOGGER.info("[PlexMod] ChatPatternEngine registered.");
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event.type == 2) return;
        handleMessage(stripColourCodes(event.message.getUnformattedText()));
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        LOGGER.debug("[PlexMod] Disconnected — clearing pending game event.");
        pending = PendingEvent.NONE;
        EventBus.publish(new PartyLeaveEvent(PartyLeaveEvent.Reason.LEFT));
    }

    private static void handleMessage(String plain) {

        if (PORTAL_TRAVEL.matcher(plain).matches()) {
            LOGGER.debug("[PlexMod] Portal travel — clearing pending.");
            pending = PendingEvent.NONE;
            return;
        }

        if (GAME_HEADER.matcher(plain).find()) {
            if (pending == PendingEvent.NONE) {
                pending = PendingEvent.GAME_START;
                LOGGER.debug("[PlexMod] Game header — pending: GAME_START");
                EventBus.publish(new GameReadyEvent());
            }
            return;
        }

        if (PLACEMENT_LINE.matcher(plain).matches() || TEAM_WIN_LINE.matcher(plain).matches()) {
            if (pending == PendingEvent.GAME_START) {
                pending = PendingEvent.GAME_END;
                LOGGER.debug("[PlexMod] Win/placement — pending: GAME_END");
            }
            return;
        }

        if (CHAT_UNSILENCED.matcher(plain).matches()) {
            long now = System.currentTimeMillis();
            switch (pending) {
                case GAME_END:
                    if (now - lastGameEndTime >= GAME_EVENT_COOLDOWN_MS) {
                        lastGameEndTime = now;
                        LOGGER.debug("[PlexMod] Firing: GameEndEvent");
                        EventBus.publish(new GameEndEvent(GameEndEvent.Result.UNKNOWN));
                    }
                    break;
                case GAME_START:
                    if (now - lastGameStartTime >= GAME_EVENT_COOLDOWN_MS) {
                        lastGameStartTime = now;
                        LOGGER.debug("[PlexMod] Firing: GameStartEvent");
                        EventBus.publish(new GameStartEvent());
                    }
                    break;
                default:
                    break;
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
        } else if (PARTY_DISBANDED.matcher(plain).matches()) {
            EventBus.publish(new PartyLeaveEvent(PartyLeaveEvent.Reason.DISBANDED));
        } else if (PARTY_KICKED.matcher(plain).matches()) {
            EventBus.publish(new PartyLeaveEvent(PartyLeaveEvent.Reason.KICKED));
        } else if (PARTY_LEFT.matcher(plain).matches()) {
            EventBus.publish(new PartyLeaveEvent(PartyLeaveEvent.Reason.LEFT));
        }
    }

    public static String stripColourCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }
}
