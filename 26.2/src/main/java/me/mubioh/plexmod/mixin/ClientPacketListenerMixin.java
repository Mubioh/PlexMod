package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.util.PlayerDataService;
import me.mubioh.plexmod.feature.chatcycle.ChatCycleFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.multiplayer.ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Redirect(method = "handleLogin(Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;addToast(Lnet/minecraft/client/gui/components/toasts/Toast;)V"))
    private void cancelAdding(final ToastManager instance, final Toast toast) {}

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void plexmod$onLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;

        String uuid = localPlayer.getStringUUID();

        ChatCycleFeature chatCycle = ChatCycleFeature.getInstance();
        if (chatCycle != null) chatCycle.onServerJoin(uuid);

        PlayerDataService.getInstance().requestPlayer(uuid);
        PlayerDataService.getInstance().fetchLocalPlayerRank(uuid);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void plexmod$onClose(CallbackInfo ci) {
        ChatCycleFeature chatCycle = ChatCycleFeature.getInstance();
        if (chatCycle != null) {
            chatCycle.purgeCommunityState();
            chatCycle.resetSessionFetch();
        }
    }
}