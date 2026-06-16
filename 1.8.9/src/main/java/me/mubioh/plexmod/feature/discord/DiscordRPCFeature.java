package me.mubioh.plexmod.feature.discord;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import me.mubioh.plexmod.core.feature.PlexFeature;
import me.mubioh.plexmod.core.util.GameDetectorUtil;
import me.mubioh.plexmod.core.util.MineplexHelper;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DiscordRPCFeature implements PlexFeature {

    private static final Logger LOGGER    = LogManager.getLogger("PlexMod/DiscordRPC");
    private static final long   CLIENT_ID = 1501655277743771678L;

    private IPCClient ipcClient;
    private final AtomicBoolean ready               = new AtomicBoolean(false);
    private final AtomicBoolean running             = new AtomicBoolean(false);
    private final AtomicLong    lastConnectionAttempt = new AtomicLong(0L);

    private Thread connectionThread;
    private Thread updateThread;

    private volatile String lastDetails = null;
    private OffsetDateTime sessionStart = null;

    @Override public String  getId()          { return "discord_rpc"; }
    @Override public String  getDisplayName() { return "Discord RPC"; }
    @Override public String  getTooltip()     { return "Shows your current Mineplex game in Discord Rich Presence."; }
    @Override public boolean isToggleable()   { return false; }

    @Override
    public void onEnable() {
        running.set(true);

        updateThread = new Thread(new Runnable() {
            public void run() {
                while (running.get()) {
                    try {
                        manageConnection();
                        if (ready.get() && MineplexHelper.isOnMineplex()) {
                            updatePresence();
                        }
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        LOGGER.warn("[PlexMod] Discord loop error: {}", e.getMessage());
                    }
                }
            }
        }, "PlexMod-DiscordRPC");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    @Override
    public void onDisable() {
        running.set(false);
        ready.set(false);
        if (updateThread != null) { updateThread.interrupt(); updateThread = null; }
        if (connectionThread != null) { connectionThread.interrupt(); connectionThread = null; }
        closeConnection();
    }

    private void manageConnection() {
        if (!MineplexHelper.isOnMineplex()) {
            if (isConnected()) {
                closeConnection();
            }
            return;
        }

        if (isConnected()) return;
        ready.set(false);

        long now = Minecraft.getSystemTime();
        if (connectionThread != null) {
            if (now > lastConnectionAttempt.get() + 8000L) {
                connectionThread.interrupt();
                connectionThread = null;
            }
            return;
        }
        if (now < lastConnectionAttempt.get() + 8000L) return;

        lastConnectionAttempt.set(now);

        ipcClient = new IPCClient(CLIENT_ID);
        ipcClient.setListener(new IPCListener() {
            @Override
            public void onReady(IPCClient client) {
                LOGGER.info("[PlexMod] Discord IPC connected.");
                ready.set(true);
                sessionStart = OffsetDateTime.now();
                lastConnectionAttempt.set(0L);
            }
        });

        connectionThread = new Thread(new Runnable() {
            public void run() {
                try {
                    ipcClient.connect(DiscordBuild.ANY);
                } catch (Exception e) {
                    LOGGER.debug("[PlexMod] Discord IPC connect failed: {}", e.getMessage());
                } finally {
                    connectionThread = null;
                }
            }
        }, "PlexMod-DiscordConnect");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    private void updatePresence() {
        if (!ready.get() || ipcClient == null) return;
        try {
            GameDetectorUtil.Detection det = GameDetectorUtil.detect();
            String details = det.isLobby() ? "In Lobby" : "Playing " + det.gameName();

            if (details.equals(lastDetails)) return;

            sessionStart = OffsetDateTime.now();
            lastDetails  = details;

            RichPresence.Builder presence = new RichPresence.Builder();
            presence.setDetails(details);
            presence.setState("On Minecraft 1.8.9");
            presence.setLargeImage("mineplex", "mineplex.com");
            presence.setStartTimestamp(sessionStart);

            ipcClient.sendRichPresence(presence.build());
            LOGGER.debug("[PlexMod] Discord RPC updated: {}", details);
        } catch (Exception e) {
            LOGGER.warn("[PlexMod] Failed to update Discord presence: {}", e.getMessage());
            ready.set(false);
        }
    }

    private void closeConnection() {
        try {
            if (ipcClient != null) {
                ipcClient.close();
            }
        } catch (Exception e) {
            LOGGER.debug("[PlexMod] Error closing IPC: {}", e.getMessage());
        } finally {
            ipcClient = null;
            ready.set(false);
            lastDetails = null;
            lastConnectionAttempt.set(0L);
        }
    }

    private boolean isConnected() {
        return ipcClient != null && ipcClient.getStatus() == PipeStatus.CONNECTED;
    }
}