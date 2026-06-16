package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.util.GameDetectorUtil;
import me.mubioh.plexmod.feature.betterlobbies.BetterLobbiesFeature;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public class BossBarMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void onExtractRenderState(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (BetterLobbiesFeature.getInstance() == null) return;
        if (GameDetectorUtil.isInLobby()) ci.cancel();
    }
}