package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.event.EventBus;
import me.mubioh.plexmod.core.event.ServerSwitchEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;

@Mixin(ClientPacketListener.class)
public class ClientConnectionMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlexMod/Mixin");
    private static final String BUNGEE_CHANNEL = "bungeecord:main";

    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void onCustomPayload(CustomPacketPayload payload, CallbackInfo ci) {
        try {
            if (payload == null) return;

            String channelId = payload.type().id().toString();
            if (!channelId.equals(BUNGEE_CHANNEL)) return;

            byte[] data = extractBytes(payload);
            if (data == null || data.length < 4) return;

            String subCommand = readBCString(data, 0);
            if (!"Connect".equalsIgnoreCase(subCommand)) return;

            int offset = 2 + subCommand.getBytes(StandardCharsets.UTF_8).length;
            String serverName = readBCString(data, offset);

            EventBus.publish(new ServerSwitchEvent(serverName));

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

    private static byte[] extractBytes(CustomPacketPayload payload) {
        try {
            var field = payload.getClass().getDeclaredField("data");
            field.setAccessible(true);
            Object buf = field.get(payload);
            if (buf instanceof io.netty.buffer.ByteBuf bb) {
                byte[] bytes = new byte[bb.readableBytes()];
                bb.getBytes(bb.readerIndex(), bytes);
                return bytes;
            }
        } catch (Exception ignored) {}
        return null;
    }
}