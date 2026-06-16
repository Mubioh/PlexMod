package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.event.EventBus;
import me.mubioh.plexmod.core.event.ServerSwitchEvent;
import net.minecraft.client.network.NetHandlerPlayClient;
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

    private static final Logger LOGGER = LogManager.getLogger("PlexMod/Mixin");
    private static final String BUNGEE_CHANNEL = "BungeeCord";

    /**
     * Intercepts S3F custom payload packets to detect BungeeCord server-switch messages.
     * In 1.8.9 these arrive as S3FPacketCustomPayload with channel "BungeeCord" and
     * a payload whose first UTF-8 short-string is "Connect".
     */
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
