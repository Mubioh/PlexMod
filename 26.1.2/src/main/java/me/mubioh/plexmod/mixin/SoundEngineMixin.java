package me.mubioh.plexmod.mixin;

import me.mubioh.plexmod.core.util.GameDetectorUtil;
import me.mubioh.plexmod.feature.betterlobbies.BetterLobbiesFeature;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void onPlay(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (BetterLobbiesFeature.getInstance() == null) return;
        if (!GameDetectorUtil.isInLobby()) return;

        var identifier = instance.getIdentifier();
        if (identifier == null) return;

        String path = identifier.getPath();
        if (path.contains("portal")
                || path.contains("fire")
                || path.contains("cat.")
                || path.contains("zombie.")
                || path.contains("ender_dragon.flap")
                || path.contains("ender_dragon.growl")
                || path.contains("wither.spawn")
                || path.contains("thunder")) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}