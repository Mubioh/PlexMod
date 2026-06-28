package me.mubioh.plexmod.feature.discord;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import me.mubioh.plexmod.core.feature.PlexFeature;
import me.mubioh.plexmod.core.util.GameDetectorUtil;
import me.mubioh.plexmod.core.util.MineplexHelper;
import net.minecraft.SharedConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class DiscordRPCFeature implements PlexFeature {

    private static final Logger LOGGER    = LoggerFactory.getLogger("PlexMod/DiscordRPC");
    private static final long   CLIENT_ID = 1501655277743771678L;

    private static final boolean DEV_MODE = false;

    private Core   core;
    private Thread rpcThread;
    private volatile boolean running = false;

    private volatile String  lastDetails  = null;
    private volatile String  lastState    = null;
    private volatile Instant sessionStart = null;

    @Override public String getId()          { return "discord_rpc"; }
    @Override public String getDisplayName() { return "Discord RPC"; }
    @Override public String getTooltip()     { return "Shows your current Mineplex game in Discord Rich Presence."; }
    @Override public boolean isToggleable()  { return true; }

    @Override
    public void onEnable() {
        running      = true;
        sessionStart = Instant.now();
        lastDetails  = null;
        lastState    = null;

        rpcThread = new Thread(() -> {
            try (CreateParams params = new CreateParams()) {
                params.setClientID(CLIENT_ID);
                params.setFlags(CreateParams.getDefaultFlags());

                try (Core core = new Core(params)) {
                    this.core = core;
                    LOGGER.info("Discord Game SDK initialised.");

                    updateActivity(core);
                    core.runCallbacks();

                    while (running) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        updateActivity(core);
                        core.runCallbacks();
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Discord RPC error: {}", e.getMessage(), e);
            } finally {
                this.core = null;
            }
        }, "PlexMod-DiscordRPC");

        rpcThread.setDaemon(true);
        rpcThread.start();
    }

    @Override
    public void onDisable() {
        running = false;

        if (rpcThread != null) {
            rpcThread.interrupt();
            rpcThread = null;
        }

        core = null;
    }

    private void updateActivity(Core core) {
        if (!DEV_MODE && !MineplexHelper.isOnMineplex()) {
            clearActivityIfNeeded(core);
            return;
        }

        GameDetectorUtil.Detection detection = GameDetectorUtil.detect();

        String details = detection.isLobby()
                ? "In Lobby"
                : "Playing " + detection.gameName();

        if (details.equals(lastDetails)) return;

        sessionStart = Instant.now();
        lastDetails  = details;

        try (Activity activity = new Activity()) {
            activity.setDetails(details);
            activity.setState("On Minecraft " + SharedConstants.getCurrentVersion().name());
            activity.assets().setLargeImage("mineplex");
            activity.timestamps().setStart(sessionStart);
            core.activityManager().updateActivity(activity);
            LOGGER.debug("Discord RPC updated — details: '{}'", details);
        } catch (Exception e) {
            LOGGER.warn("Failed to update Discord activity: {}", e.getMessage());
        }
    }

    private void clearActivityIfNeeded(Core core) {
        if (lastDetails == null && lastState == null) return;

        lastDetails = null;
        lastState   = null;

        try {
            core.activityManager().clearActivity();
        } catch (Exception e) {
            LOGGER.warn("Failed to clear Discord activity: {}", e.getMessage());
        }
    }
}
