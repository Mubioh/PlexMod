package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.config.PlexConfig;
import me.mubioh.plexmod.core.util.PlayerDataService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {

    /**
     * canRenderName controls whether a nametag is drawn above an entity.
     * We force it true for the local player when in third-person (own nametag feature),
     * and trigger a player-data request for level tags.
     */
    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z",
            at = @At("HEAD"), cancellable = true)
    private void plexmod$showOwnName(EntityLivingBase entity, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP local = mc.thePlayer;
        if (local == null) return;

        // Request level data for all players so level tags can render
        if (entity instanceof EntityPlayer) {
            String uuid = entity.getUniqueID().toString();
            PlayerDataService.getInstance().requestPlayer(uuid);
        }

        // Own nametag feature
        if (!PlexConfig.getInstance().isFeatureEnabled("nametag")) return;
        if (entity != local) return;

        boolean thirdPerson = mc.gameSettings.thirdPersonView != 0;
        boolean hudVisible  = !mc.gameSettings.hideGUI;
        boolean visible     = !entity.isInvisibleToPlayer(local);

        if (thirdPerson && hudVisible && visible && !entity.isRiding()) {
            cir.setReturnValue(true);
        }
    }
}
