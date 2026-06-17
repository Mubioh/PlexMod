package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.config.PlexConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {
    @Inject(
            method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void plexmod$showOwnName(T entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (!PlexConfig.getInstance().isFeatureEnabled("nametag")) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null || entity != localPlayer) return;

        boolean thirdPerson = !mc.options.getCameraType().isFirstPerson();
        boolean hudVisible = !mc.gui.hud.isHidden();
        boolean visible = !entity.isInvisibleTo(localPlayer);

        if (thirdPerson && hudVisible && visible && !entity.isVehicle()) {
            cir.setReturnValue(true);
        }
    }
}