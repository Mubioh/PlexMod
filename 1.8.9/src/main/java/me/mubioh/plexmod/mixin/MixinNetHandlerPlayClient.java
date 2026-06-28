package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.event.EventBus;
import me.mubioh.plexmod.core.event.ServerSwitchEvent;
import me.mubioh.plexmod.core.util.PlayerDataService;
import me.mubioh.plexmod.feature.chatcycle.ChatCycleFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;

@Mixin(value = NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    private static final Logger LOGGER         = LogManager.getLogger("PlexMod/Mixin");
    private static final String BUNGEE_CHANNEL = "BungeeCord";

    @Inject(method = "handleJoinGame", at = @At("TAIL"))
    private void plexmod$onJoinGame(S01PacketJoinGame packet, CallbackInfo ci) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null) return;

                String uuid = mc.thePlayer.getUniqueID().toString();

                ChatCycleFeature chatCycle = ChatCycleFeature.getInstance();
                if (chatCycle != null) chatCycle.onServerJoin(uuid);

                PlayerDataService.getInstance().requestPlayer(uuid);
                PlayerDataService.getInstance().fetchLocalPlayerRank(uuid);
            }
        }, "PlexMod-JoinGameDelay");
        t.setDaemon(true);
        t.start();
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void onCustomPayload(S3FPacketCustomPayload packet, CallbackInfo ci) {
        try {
            if (!BUNGEE_CHANNEL.equals(packet.getChannelName())) return;
            byte[] data = packet.getBufferData().array();
            if (data.length < 4) return;
            String sub = readBCString(data, 0);
            if (!"Connect".equalsIgnoreCase(sub)) return;
            int offset = 2 + sub.getBytes(StandardCharsets.UTF_8).length;
            String server = readBCString(data, offset);
            EventBus.publish(new ServerSwitchEvent(server));

            ChatCycleFeature chatCycle = ChatCycleFeature.getInstance();
            if (chatCycle != null) {
                chatCycle.purgeCommunityState();
                chatCycle.resetSessionFetch();
            }
        } catch (Exception e) {
            LOGGER.debug("[PlexMod] Plugin-message parse error: {}", e.getMessage());
        }
    }

    private static String readBCString(byte[] data, int offset) {
        if (offset + 2 > data.length) return "";
        int len = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        if (offset + 2 + len > data.length) return "";
        return new String(data, offset + 2, len, StandardCharsets.UTF_8);
    }
}
